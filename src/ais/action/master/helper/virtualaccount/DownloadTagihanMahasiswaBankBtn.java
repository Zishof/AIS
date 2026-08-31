package ais.action.master.helper.virtualaccount;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.servlet.Va;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.MyDoublebox;
import ais.ui.util.WaktuUtil;

/**
 * Integrasi payment gateway Virtual Account (VA) Bank BTN untuk pembuatan dan inquiry tagihan,
 * dipakai baik untuk tagihan mahasiswa Perguruan Tinggi maupun tagihan siswa/calon siswa Sekolah.
 * Kelas ini murni statis (kumpulan fungsi utilitas), tidak menyimpan state antar pemanggilan.
 *
 * <h2>Skema keamanan permintaan</h2>
 * Setiap permintaan ke gateway BTN ditandatangani dengan HMAC-SHA256 ({@link #encode(String, String)})
 * atas string {@code companyId:jsonData:key}, memakai kredensial dari konfigurasi
 * {@code btn_company_id}/{@code btn_key}/{@code btn_secret}, dan dikirim lewat header HTTP
 * {@code id}/{@code key}/{@code signature} ({@link #post(String, String, String)}). Endpoint dapat
 * diarahkan lewat proxy internal ({@code btn_forward_url}) bila konfigurasi
 * {@code btn_forward_url_aktif} aktif.
 *
 * <h2>Alur pembuatan VA</h2>
 * <ol>
 * <li>{@link #downloadData(Mahasiswa, Integer, JadwalPembayaran, Collection, Grid)} (untuk mahasiswa)
 * atau {@link #downloadData(Siswa, CalonSiswa, Collection, boolean, Double, BankHost,
 * AkunPembayaranSiswa, Sekolah)} (untuk siswa/calon siswa sekolah) menyusun rincian item tagihan
 * dari baris cicilan/tagihan yang dipilih pengguna, menghitung total, dan mencoba menemukan
 * {@link VirtualAccountBank} yang masih berlaku (belum kedaluwarsa, keterangan identik) sebelum
 * membuat permintaan baru ke gateway — mencegah pembuatan VA duplikat untuk tagihan yang sama.</li>
 * <li>Bila tidak ditemukan VA yang masih berlaku, nomor VA dan nomor referensi dibangkitkan lokal,
 * permintaan {@code createVA} dikirim ke {@code btn_gateway_url}, dan hasil sukses (kode respons
 * {@code "000"}) disimpan sebagai baris {@link VirtualAccountBank} baru.</li>
 * <li>{@link #inquiryBillingBTN(String, BankHost, VirtualAccountBank)} dipakai untuk mengecek status
 * pembayaran VA yang sudah ada (endpoint {@code inqVA}); bila field {@code terbayar} pada respons
 * bernilai positif, transaksi diproses lewat {@code Va#doProses} agar pembayaran tercatat di sistem.</li>
 * </ol>
 *
 * <p>
 * Waktu kedaluwarsa VA dihitung dari salah satu konfigurasi (dicek berurutan):
 * {@code tagihan_expired_akhir_hari} (kedaluwarsa jam 23:59:59 hari yang sama),
 * {@code tagihan_expired_jam} (offset jam dari sekarang), atau {@code tagihan_expired_day} (offset
 * hari dari sekarang); bila tidak ada yang diset, memakai {@code JadwalPembayaran#getEndDate()} atau
 * waktu saat ini.
 * </p>
 *
 * <p>
 * <b>Perhatian keamanan</b>: {@link #post(String, String, String)} memakai nilai default hardcode
 * untuk kredensial gateway ({@code btn_company_id="BSTIMPR"}, {@code btn_key} berupa string acak
 * panjang, dan {@code btn_secret}) sebagai fallback ketika baris {@link Konfigurasi} terkait belum
 * ada di database — lihat catatan temuan keamanan pada laporan tugas ini untuk detail lokasi baris.
 * </p>
 */
public class DownloadTagihanMahasiswaBankBtn {

	public static final ThreadLocal<SimpleDateFormat> expiredFormat = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyMMddHHmm");
		}
	};

	/**
	 * Menghitung tanda tangan HMAC-SHA256 (heksadesimal huruf kecil) atas {@code data} memakai
	 * {@code key} sebagai kunci rahasia. Dipakai untuk menandatangani permintaan ke gateway BTN.
	 *
	 * @param key  kunci rahasia (secret) HMAC
	 * @param data payload yang akan ditandatangani
	 * @return tanda tangan dalam bentuk string heksadesimal, atau {@code null} bila terjadi kegagalan
	 *         algoritma/enkoding (dicatat lewat {@code Common#tampilErrorJikaAdmin})
	 */
	public static String encode(String key, String data) {
		try {
			Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
			sha256_HMAC.init(secret_key);

			return new String(Hex.encodeHex(sha256_HMAC.doFinal(data.getBytes("UTF-8"))));

		} catch (NoSuchAlgorithmException e) {
			Common.tampilErrorJikaAdmin(e);
		} catch (InvalidKeyException e) {
			Common.tampilErrorJikaAdmin(e);
		} catch (UnsupportedEncodingException e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return null;
	}

	/**
	 * Mengirim permintaan HTTP POST bertanda tangan HMAC ke gateway Bank BTN. Timeout koneksi/baca
	 * dapat diatur lewat konfigurasi {@code btn_connect_timeout_ms}/{@code btn_read_timeout_ms}
	 * (default 15 dan 30 detik). Bila {@code btn_forward_url_aktif} aktif, permintaan diarahkan lebih
	 * dulu ke proxy internal {@code btn_forward_url} dengan {@code strURLParam} asli, prefix, postfix,
	 * dan signature disisipkan sebagai parameter query.
	 *
	 * @param postData     body permintaan yang benar-benar dikirim (biasanya sama dengan {@code jsonData})
	 * @param jsonData     payload JSON yang dipakai untuk menghitung signature (lihat dokumentasi kelas)
	 * @param strURLParam  URL tujuan gateway (atau tujuan asli bila diteruskan lewat proxy)
	 * @return body respons mentah dari gateway
	 * @throws Exception dilempar dengan pesan berbahasa Indonesia yang informatif bila koneksi
	 *                    ditolak, gagal terhubung, atau timeout menghubungi gateway
	 */
	public static String post(String postData, String jsonData, String strURLParam) throws Exception {
		int connectTimeout = 15000;
		int readTimeout = 30000;
		try {
			connectTimeout = Integer.parseInt(Common.getKonfigurasi("btn_connect_timeout_ms", "15000").getNilai().trim());
		} catch (Exception eIgnore) { ais.common.ErrorAuditUtil.record(eIgnore, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBtn.java:84");
		}
		try {
			readTimeout = Integer.parseInt(Common.getKonfigurasi("btn_read_timeout_ms", "30000").getNilai().trim());
		} catch (Exception eIgnore) { ais.common.ErrorAuditUtil.record(eIgnore, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBtn.java:88");
		}
		org.apache.http.client.config.RequestConfig requestConfig = org.apache.http.client.config.RequestConfig.custom()
				.setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectTimeout)
				.setSocketTimeout(readTimeout).build();
		CloseableHttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
		try {
			String prefix = Common.getKonfigurasi("btn_company_id", "BSTIMPR").getNilai();
			String postfix = Common.getKonfigurasi("btn_key", "zitAhzP3B6HrQhO7yUaiIAENnv3GX3N3").getNilai();
			String secret = Common.getKonfigurasi("btn_secret", "kNlRLG978n").getNilai();
			String message = prefix + ":" + jsonData + ":" + postfix;
			String signature = encode(secret, message);

			System.out.println("secret => " + secret);
			System.out.println("message => " + message);
			System.out.println("id => " + prefix);
			System.out.println("key => " + postfix);
			System.out.println("signature => " + signature);

			String strURL;
			if (Common.bolehKonfigurasi("btn_forward_url_aktif", Konfigurasi.TIDAK_AKTIF)) {
				strURL = Common.getKonfigurasi("btn_forward_url", strURLParam).getNilai() + "?strURL="
						+ URLEncoder.encode(strURLParam, "UTF-8") + "&prefix=" + URLEncoder.encode(prefix, "UTF-8")
						+ "&postfix=" + URLEncoder.encode(postfix, "UTF-8") + "&signature="
						+ URLEncoder.encode(signature, "UTF-8");
			} else {
				strURL = strURLParam;
			}

			HttpPost httpPost = new HttpPost(strURL);

			StringEntity entity = new StringEntity(postData);
			httpPost.setEntity(entity);
			httpPost.setHeader("Content-type", "application/json");
			httpPost.setHeader("id", prefix);
			httpPost.setHeader("key", postfix);
			httpPost.setHeader("signature", signature);

			CloseableHttpResponse response;
			try {
				response = httpclient.execute(httpPost);
			} catch (org.apache.http.conn.HttpHostConnectException eConn) {
				throw new Exception("Koneksi ke gateway Bank BTN ditolak/tidak dapat dihubungi (" + strURL
						+ "). Server bank kemungkinan sedang tidak aktif. Silakan coba beberapa saat lagi.", eConn);
			} catch (java.net.ConnectException eConn) {
				throw new Exception("Koneksi ke gateway Bank BTN gagal (" + strURL
						+ "). Server bank kemungkinan sedang tidak aktif. Silakan coba beberapa saat lagi.", eConn);
			} catch (java.net.SocketTimeoutException eTo) {
				throw new Exception("Timeout saat menghubungi gateway Bank BTN (" + strURL
						+ "). Silakan coba beberapa saat lagi.", eTo);
			}
			String hasil = EntityUtils.toString(response.getEntity());

			return hasil;
		} finally {
			if (httpclient != null) {
				try { httpclient.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBtn.java:144");}
			}
		}
	}

	/**
	 * Mengecek status tagihan/pembayaran satu VA BTN lewat endpoint {@code inqVA} (diturunkan dari
	 * {@code btn_gateway_url} dengan mengganti {@code createVA} menjadi {@code inqVA}). Bila field
	 * {@code terbayar} pada respons bernilai lebih dari 0.1, transaksi diteruskan ke {@code Va#doProses}
	 * agar pembayaran tercatat pada sistem.
	 *
	 * @param va                          nomor Virtual Account yang dicek
	 * @param bankHost                    konfigurasi host bank terkait, diteruskan ke {@code Va#doProses}
	 * @param virtualAccountBankReadOnly  baris {@link VirtualAccountBank} tersimpan; field
	 *                                    {@code request}-nya dipakai untuk mengambil kembali nomor
	 *                                    referensi ({@code ref}) permintaan awal
	 * @return objek JSON respons gateway (berisi status pembayaran)
	 * @throws Exception diteruskan dari kegagalan komunikasi HTTP atau parsing JSON
	 */
	public static JSONObject inquiryBillingBTN(String va, BankHost bankHost,
			VirtualAccountBank virtualAccountBankReadOnly) throws Exception {

		JSONObject refData = new JSONObject(virtualAccountBankReadOnly.getRequest());
		String ref = refData.getString("ref");

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("va", va);
		jsonObject.put("ref", ref);

		String strURL = (Common.getKonfigurasi("btn_gateway_url", "https://vabtn-dev.btn.co.id:9021/v1/bstimpr/createVA").getNilai());
		strURL = org.apache.commons.lang3.StringUtils.replace(strURL, "createVA", "inqVA");
		String postData = jsonObject.toString();

		System.out.println("Request body: " + strURL);
		System.out.println(postData);
		String hasil = DownloadTagihanMahasiswaBankBtn.post(postData, postData, strURL);
		System.out.println("Response body: ");
		System.out.println(hasil);

		JSONObject response = new JSONObject(hasil);

		if (!response.isNull("terbayar")) {
			Double nominalP = 0.0;
			try {
				nominalP = Double.parseDouble(response.get("terbayar") + "");
			} catch (Exception e) {
				nominalP = 0.0;
			}
			
			if (nominalP > 0.1) {
				response.put("action", "payment");
				String body = Va.doProses(response.toString(), null, bankHost, true);
				System.out.println("Hasil: " + body);
			}
		}

		return response;
	}

	/**
	 * Membuat (atau menggunakan kembali) VA BTN untuk tagihan seorang {@link Mahasiswa}. Menyusun
	 * daftar item dari baris {@code gridCicilan} yang diisi pengguna (nilai cicilan per baris via
	 * atribut komponen {@code jumlahCicilan}, dipetakan ke {@link PengaturanPembayaranBulanan} atau
	 * {@link ItemBiaya} tergantung baris), menghitung total tagihan, lalu mencari
	 * {@link VirtualAccountBank} yang masih berlaku dengan keterangan identik sebelum membuat
	 * permintaan {@code createVA} baru ke gateway. Menolak proses (melempar
	 * {@link IllegalArgumentException}) bila total tagihan nol/negatif dan konfigurasi
	 * {@code payment_gateway_tolak_total_nol_atau_minus} aktif. Transaksi database dibuka dan
	 * ditutup mandiri oleh method ini (commit bila sukses, rollback bila gagal).
	 *
	 * @param mahasiswa            mahasiswa pemilik tagihan
	 * @param smt                  nomor semester tagihan
	 * @param myjadwalPembayaran   jadwal pembayaran terkait (menentukan jenis kegiatan dan tenggat
	 *                             kedaluwarsa VA), boleh {@code null}
	 * @param detailBiayas         koleksi {@link DetailBiaya} yang menjadi rincian tagihan (untuk
	 *                             kolom {@code detailbiaya} pada {@link VirtualAccountBank})
	 * @param gridCicilan          grid ZK berisi baris cicilan yang dipilih pengguna beserta nilainya
	 * @return {@link VirtualAccountBank} yang sudah ada atau baru dibuat, atau {@code null} bila
	 *         terjadi kegagalan (dicatat lewat {@code Common#tampilErrorJikaAdmin})
	 * @throws Exception diteruskan dari kegagalan komunikasi gateway atau akses database
	 */
	@SuppressWarnings({ "rawtypes" })
	public static VirtualAccountBank downloadData(Mahasiswa mahasiswa, Integer smt, JadwalPembayaran myjadwalPembayaran,
			Collection detailBiayas, Grid gridCicilan) throws Exception {

		Session session = null;
		boolean isLocalTransaction = false;
		VirtualAccountBank virtualAccountBankNtt = null;

		try {
			// Optimasi RAM: Gunakan StringBuilder, hindari operasi String += dalam Loop
			StringBuilder sbDetailbiaya = new StringBuilder();
			if (detailBiayas != null) {
				for (Object o : detailBiayas) {
					if (o instanceof DetailBiaya) {
						DetailBiaya biaya = (DetailBiaya) o;
						if (sbDetailbiaya.length() > 0) sbDetailbiaya.append(",");
						sbDetailbiaya.append(biaya.getId());
					}
				}
			}

			StringBuilder sbPemb = new StringBuilder();
			StringBuilder sbCicilan = new StringBuilder();
			double totalVal = 0.0; // Primitif double menghemat beban Auto-Boxing

			JadwalPembayaran jdw = myjadwalPembayaran != null && myjadwalPembayaran.getKhususUntukNim() != null
					&& myjadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
							? myjadwalPembayaran
							: null;
							
			if (gridCicilan != null && gridCicilan.getRows() != null) {
				List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
				for (Row row : mycicilanrows) {
					MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");

					if (jumlahCicilan != null && jumlahCicilan.getValue() != null && jumlahCicilan.getValue().intValue() != 0) {
						CicilanPembayaran cicilanPembayaranSebelumnya = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
						
						if (cicilanPembayaranSebelumnya != null && cicilanPembayaranSebelumnya.getId() == null) {
							try {
								PengaturanPembayaranBulanan biaya = cicilanPembayaranSebelumnya.getPengaturanPembayaranBulanan();
								Double nilai = jumlahCicilan.getValue();

								if (biaya != null) {
									if (sbCicilan.length() > 0) sbCicilan.append(",");
									sbCicilan.append("Bulanan-").append(biaya.getId()).append("-").append(nilai);

									Double hasilDenda = biaya.checkDenda(nilai, ais.ui.util.WaktuUtil.getDate(), jdw,
											myjadwalPembayaran == null ? null : myjadwalPembayaran.getJenisKegiatan());

									String desc = biaya.getKeterangan();
									if (desc == null || desc.isEmpty()) {
										desc = (biaya.getDetailBiaya() != null && biaya.getDetailBiaya().getItemBiaya() != null) ? biaya.getDetailBiaya().getItemBiaya().getNama() : "";
									}
									
									desc += ", Rp. " + Common.numberFormat.get().format(nilai)
											+ (hasilDenda.intValue() > nilai.intValue() ? biaya.getInfoDenda() : "");

									String itemKode = (biaya.getDetailBiaya() != null && biaya.getDetailBiaya().getItemBiaya() != null) ? biaya.getDetailBiaya().getItemBiaya().getKode().trim() : "";
									sbPemb.append(itemKode).append(",").append(desc).append(";");
									totalVal += nilai;
									
								} else {
									Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
									ItemBiaya itemBiaya = null;
									DetailBiaya detailBiaya = (myItemBiaya != null && myItemBiaya.getSelectedItem() != null) 
											? (DetailBiaya) myItemBiaya.getSelectedItem().getValue() : null;
											
									if (cicilanPembayaranSebelumnya.getItemBiaya() != null && cicilanPembayaranSebelumnya.getItemBiaya().getId() != null) {
										itemBiaya = cicilanPembayaranSebelumnya.getItemBiaya();
									} else if (detailBiaya != null) {
										itemBiaya = detailBiaya.getItemBiaya();
									}
									
									if (itemBiaya != null && detailBiaya != null) {
										if (sbCicilan.length() > 0) sbCicilan.append(",");
										sbCicilan.append("Item-").append(itemBiaya.getId()).append("-").append(nilai).append("-").append(detailBiaya.getBayarKe()).append("-").append(detailBiaya.getId());

										String desc = itemBiaya.getNama() + ", Rp. " + Common.numberFormat.get().format(nilai);
										sbPemb.append(itemBiaya.getKode().trim()).append(",").append(desc).append(";");
										totalVal += nilai;
									}
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBtn.java:273");
							}
						}
					}
				}
			}

			if (Common.bolehKonfigurasi("payment_gateway_tolak_total_nol_atau_minus") && totalVal <= 0.0) {
				throw new IllegalArgumentException("Total tagihan bernilai nol atau minus. Item minus seperti bantuan, beasiswa, potongan, atau koreksi tidak dapat dibuatkan VA sendiri.");
			}

			// BUKA SESI SECARA EKSPLISIT DAN AMAN
			session = MahasiswaVirtualAccountHelper.openSession();
			MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
			isLocalTransaction = true;

			boolean tagihan_expired_akhir_hari = Common.getKonfigurasi("tagihan_expired_akhir_hari", Konfigurasi.TIDAK_AKTIF).getNilai().trim().equals(Konfigurasi.AKTIF);
			Date expired_date = (myjadwalPembayaran != null && myjadwalPembayaran.getEndDate() != null) ? myjadwalPembayaran.getEndDate() : WaktuUtil.getDate();
			
			if (tagihan_expired_akhir_hari) {
				try {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.HOUR_OF_DAY, 23);
					calendar.set(Calendar.MINUTE, 59);
					calendar.set(Calendar.SECOND, 59);
					expired_date = calendar.getTime();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBtn.java:299");}
			} else {
				String tagihan_expired_jam = Common.getKonfigurasi("tagihan_expired_jam", "").getNilai();
				if (!tagihan_expired_jam.isEmpty() && !tagihan_expired_jam.equalsIgnoreCase("0")) {
					try {
						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + Integer.parseInt(tagihan_expired_jam));
						expired_date = calendar.getTime();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBtn.java:307");}
				} else {
					String tagihan_expired_day = Common.getKonfigurasi("tagihan_expired_day", "0").getNilai();
					if (!tagihan_expired_day.isEmpty() && !tagihan_expired_day.equalsIgnoreCase("0")) {
						try {
							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + Integer.parseInt(tagihan_expired_day));
							expired_date = calendar.getTime();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBtn.java:315");}
					}
				}
			}

			virtualAccountBankNtt = (VirtualAccountBank) session.createCriteria(VirtualAccountBank.class)
					.add(Restrictions.eq("terjadiKendala", false))
					.add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate()))
					.add(Restrictions.eq("keterangan", sbPemb.toString()))
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("semester", smt))
					.add(Restrictions.eq("jenisKegiatan", myjadwalPembayaran == null ? null : myjadwalPembayaran.getJenisKegiatan()))
					.add(Restrictions.isNull("kegiatan"))
					.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
					
			if (virtualAccountBankNtt == null) {
				JSONObject jsonObject = new JSONObject();

				String ref = Common.getGeneratedBarCode(12);
				String kodeInstitusi = Common.getKonfigurasi("btn_kode_institusi", "4463").getNilai();
				String kodePayment = Common.getKonfigurasi("btn_kode_payment", "001").getNilai();

				int digitgenerated = 10;
				try {
					digitgenerated = Integer.parseInt(Common.getKonfigurasi("btn_generated_payment", "10").getNilai());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBtn.java:340");}
				
				String va = "9" + kodeInstitusi + kodePayment + Common.getGeneratedAngkaDigit(digitgenerated);

				// NPE Protection pada chain objek
				String namaLayanan = "";
				String kodeLayanan = "";
				if (mahasiswa != null && mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getFakultas() != null && mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null) {
					namaLayanan = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama();
					kodeLayanan = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getKodePerguruanTinggi();
				}
				
				String jenisBayarNama = (myjadwalPembayaran != null && myjadwalPembayaran.getJenisKegiatan() != null) ? myjadwalPembayaran.getJenisKegiatan().getNamaKegiatan() : "";
				String jenisBayarKode = (myjadwalPembayaran != null && myjadwalPembayaran.getJenisKegiatan() != null) ? myjadwalPembayaran.getJenisKegiatan().getKode() : "";

				jsonObject.put("ref", ref);
				jsonObject.put("va", va);
				jsonObject.put("nama", Common.maxPanjang(mahasiswa != null ? mahasiswa.getNama() : "", 40));
				jsonObject.put("layanan", Common.maxPanjang(namaLayanan, 40));
				jsonObject.put("kodelayanan", Common.maxPanjang(kodeLayanan, 40));
				jsonObject.put("jenisbayar", Common.maxPanjangAkhir(jenisBayarNama, 40));
				jsonObject.put("kodejenisbyr", jenisBayarKode);
				jsonObject.put("noid", mahasiswa != null ? mahasiswa.getNim() : "");
				jsonObject.put("tagihan", (int) totalVal + "");
				jsonObject.put("flag", "F");
				jsonObject.put("reserve", (mahasiswa != null ? mahasiswa.getId() : "") + "");
				jsonObject.put("angkatan", (mahasiswa != null ? mahasiswa.getTahunangkatan() : "") + "");
				jsonObject.put("expired", "");
				jsonObject.put("description", "");

				String postData = jsonObject.toString();

				System.out.println("Request body: ");
				System.out.println(postData);
				String strURL = (Common.getKonfigurasi("btn_gateway_url", "https://vabtn-dev.btn.co.id:9021/v1/bstimpr/createVA").getNilai());
				String hasil = DownloadTagihanMahasiswaBankBtn.post(postData, postData, strURL);
				System.out.println("Response body: ");
				System.out.println(hasil);

				JSONObject response = new JSONObject(hasil);

				if (response.has("rsp") && response.getString("rsp").equals("000")) {
					Long ptId = (PerguruanTinggiUtil.getPerguruanTinggi() != null) ? PerguruanTinggiUtil.getPerguruanTinggi().getId() : null;
					virtualAccountBankNtt = new VirtualAccountBank(ptId);
					virtualAccountBankNtt.setKadaluarsa(expired_date);
					virtualAccountBankNtt.setOtomatis(false);
					virtualAccountBankNtt.setKode(va);
					virtualAccountBankNtt.setRequest(postData);
					virtualAccountBankNtt.setResponse(response.toString());

					virtualAccountBankNtt.setCicilan(sbCicilan.toString());
					virtualAccountBankNtt.setJenisKegiatan(myjadwalPembayaran != null ? myjadwalPembayaran.getJenisKegiatan() : null);
					virtualAccountBankNtt.setKeterangan(sbPemb.toString());
					virtualAccountBankNtt.setTotal(totalVal);
					virtualAccountBankNtt.setBulanan("");
					virtualAccountBankNtt.setDetailbiaya(sbDetailbiaya.toString());

					virtualAccountBankNtt.setMahasiswa(mahasiswa);
					virtualAccountBankNtt.setJadwalPembayaran(myjadwalPembayaran);
					virtualAccountBankNtt.setSemester(smt);
					virtualAccountBankNtt.setTahunAkademik(myjadwalPembayaran != null ? myjadwalPembayaran.getTahunAkademik() : "");
					virtualAccountBankNtt.setBank("Bank BTN");

					session.saveOrUpdate(virtualAccountBankNtt);
				}
			}

			if (isLocalTransaction) {
				MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);
			}

			return virtualAccountBankNtt;

		} catch (Exception e) {
			if (isLocalTransaction) {
				MahasiswaVirtualAccountHelper.rollbackTransactionIfActive(session);
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			MahasiswaVirtualAccountHelper.closeSessionQuietly(session);
		}
		
		return null;
	}

	/**
	 * Varian {@link #downloadData(Mahasiswa, Integer, JadwalPembayaran, Collection, Grid)} untuk
	 * tagihan siswa/calon siswa Sekolah (koleksi {@link Tagihan}, bukan cicilan berbasis grid).
	 * Menyusun item dari setiap {@link Tagihan} (nominal + denda) beserta opsional biaya admin,
	 * mencari VA yang masih berlaku dengan keterangan dan flag QRIS identik, dan bila perlu membuat
	 * permintaan {@code createVA} baru. Setelah VA tersimpan, seluruh {@link Tagihan} terkait
	 * diperbarui dengan kode VA dan tanggal kedaluwarsa dalam transaksi yang sama.
	 *
	 * @param siswa                  siswa pemilik tagihan (mode siswa aktif), boleh {@code null} bila
	 *                               tagihan atas nama {@code calonSiswa}
	 * @param calonSiswa             calon siswa pemilik tagihan (mode pendaftaran), boleh {@code null}
	 * @param tag                    koleksi {@link Tagihan} yang menjadi rincian pembayaran
	 * @param qris                   {@code true} bila VA ini juga dipakai sebagai kanal QRIS
	 *                               (disisipkan ke kolom {@code keterangan} sebagai penanda)
	 * @param biayaAdmin             biaya admin tambahan yang ditambahkan ke total tagihan, boleh
	 *                               {@code null}/nol
	 * @param bankHost               host bank yang membedakan VA antar channel/cabang, boleh {@code null}
	 * @param akunPembayaranSiswa    akun pembayaran siswa terkait, disimpan ke {@link VirtualAccountBank}
	 * @param sekolah                sekolah terkait (tidak dipakai langsung untuk membangun payload)
	 * @return {@link VirtualAccountBank} yang sudah ada atau baru dibuat, atau {@code null} bila
	 *         terjadi kegagalan
	 * @throws Exception diteruskan dari kegagalan komunikasi gateway atau akses database
	 */
	@SuppressWarnings({})
	public static VirtualAccountBank downloadData(Siswa siswa, CalonSiswa calonSiswa, Collection<Tagihan> tag,
			boolean qris, Double biayaAdmin, BankHost bankHost, AkunPembayaranSiswa akunPembayaranSiswa,
			Sekolah sekolah) throws Exception {

		Session session = null;
		boolean isLocalTransaction = false;
		VirtualAccountBank virtualAccountBankOnline = null;

		try {
			StringBuilder sbCicilan = new StringBuilder();
			StringBuilder sbKeterangan = new StringBuilder();
			double totalVal = 0.0;

			JSONArray items = new JSONArray();

			if (biayaAdmin != null && biayaAdmin.intValue() > 0) {
				JSONObject jsonObjectitems = new JSONObject();
				jsonObjectitems.put("description", "Biaya Admin");
				jsonObjectitems.put("unitPrice", biayaAdmin.intValue());
				jsonObjectitems.put("qty", 1);
				jsonObjectitems.put("amount", biayaAdmin.intValue());
				items.put(jsonObjectitems);
			}
			
			JenisBiayaSekolah jenisBiayaSekolah = null;
			List<Tagihan> tagihans = null;
			
			if (tag != null) {
				tagihans = new ArrayList<Tagihan>(tag);
				Collections.sort(tagihans);

				for (Tagihan tagihan : tagihans) {
					if (tagihan.getNominalBiaya() != null && tagihan.getPengaturanBiaya() != null) {
						jenisBiayaSekolah = tagihan.getPengaturanBiaya().getJenisBiayaSekolah();
					}

					String itemName = (tagihan.getItemBiayaSekolah() != null) ? tagihan.getItemBiayaSekolah().getNama() : "";
					String desc = tagihan.getId() + "-" + itemName
							+ (tagihan.getNominalBiaya() != null && tagihan.getNominalBiaya().getDibayarSebayak() != null && tagihan.getNominalBiaya().getDibayarSebayak() > 1 ? " (ke " + tagihan.getBayarKe() + ")" : "")
							+ (tagihan.getBulan() == null ? "" : ", bulan " + tagihan.getBulan())
							+ (tagihan.getTahun() == null ? "" : ", tahun " + tagihan.getTahun()) + ", ";

					sbKeterangan.append(desc);

					Double nominalT = tagihan.getNominal() != null ? tagihan.getNominal() : 0.0;
					Double dendaT = tagihan.getDenda() != null ? tagihan.getDenda() : 0.0;
					Double nilai = nominalT + dendaT;
					
					totalVal += nilai;
					
					if (sbCicilan.length() > 0) sbCicilan.append(",");
					sbCicilan.append("Bulanan-").append(tagihan.getId()).append("-").append(nilai);

					JSONObject jsonObjectitems = new JSONObject();
					jsonObjectitems.put("description", desc);
					jsonObjectitems.put("unitPrice", nilai.intValue());
					jsonObjectitems.put("qty", 1);
					jsonObjectitems.put("amount", nilai.intValue());
					items.put(jsonObjectitems);
				}
			}

			if (Common.bolehKonfigurasi("payment_gateway_tolak_total_nol_atau_minus") && totalVal <= 0.0) {
				throw new IllegalArgumentException("Total tagihan bernilai nol atau minus. Item minus seperti bantuan, beasiswa, potongan, atau koreksi tidak dapat dibuatkan VA sendiri.");
			}

			// BUKA SESI SECARA EKSPLISIT DAN AMAN
			session = MahasiswaVirtualAccountHelper.openSession();
			MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
			isLocalTransaction = true;

			boolean tagihan_expired_akhir_hari = Common.getKonfigurasi("tagihan_expired_akhir_hari", Konfigurasi.TIDAK_AKTIF).getNilai().trim().equals(Konfigurasi.AKTIF);
			Date expired_date = WaktuUtil.getDate(); // Default value
			
			if (tagihan_expired_akhir_hari) {
				try {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.HOUR_OF_DAY, 23);
					calendar.set(Calendar.MINUTE, 59);
					calendar.set(Calendar.SECOND, 59);
					expired_date = calendar.getTime();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBtn.java:507");}
			} else {
				String tagihan_expired_jam = Common.getKonfigurasi("tagihan_expired_jam", "").getNilai();
				if (!tagihan_expired_jam.isEmpty() && !tagihan_expired_jam.equalsIgnoreCase("0")) {
					try {
						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + Integer.parseInt(tagihan_expired_jam));
						expired_date = calendar.getTime();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBtn.java:515");}
				} else {
					String tagihan_expired_day = Common.getKonfigurasi("tagihan_expired_day", "0").getNilai();
					if (!tagihan_expired_day.isEmpty() && !tagihan_expired_day.equalsIgnoreCase("0")) {
						try {
							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + Integer.parseInt(tagihan_expired_day));
							expired_date = calendar.getTime();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBtn.java:523");}
					}
				}
			}
			
			virtualAccountBankOnline = (VirtualAccountBank) session.createCriteria(VirtualAccountBank.class)
					.add(Restrictions.eq("terjadiKendala", false))
					.add(bankHost == null ? Restrictions.isNull("bankHost") : Restrictions.eq("bankHost", bankHost))
					.add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate()))
					.add(Restrictions.eq("keterangan", sbKeterangan.toString() + (qris ? "qris:true" : "")))
					.add(Restrictions.or(Restrictions.eq("calonSiswa", calonSiswa), Restrictions.eq("siswa", siswa)))
					.add(Restrictions.isNull("pembayaran"))
					.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();

			if (virtualAccountBankOnline == null) {
				JSONObject jsonObject = new JSONObject();

				String ref = Common.getGeneratedBarCode(12);
				String kodeInstitusi = Common.getKonfigurasi("btn_kode_institusi", "4463").getNilai();
				String kodePayment = Common.getKonfigurasi("btn_kode_payment", "001").getNilai();

				int digitgenerated = 10;
				try {
					digitgenerated = Integer.parseInt(Common.getKonfigurasi("btn_generated_payment", "10").getNilai());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBtn.java:547");}
				String va = "9" + kodeInstitusi + kodePayment + Common.getGeneratedAngkaDigit(digitgenerated);

				String namaSiswaStr = siswa != null ? siswa.getNama() : (calonSiswa != null ? calonSiswa.getNama() : "");
				
				// Proteksi NPE berlapis pada Relasi Sekolah
				String namaLayanan = "";
				String kodeLayanan = "";
				if (siswa != null && siswa.getSekolah() != null) {
					namaLayanan = siswa.getSekolah().getNama();
					kodeLayanan = siswa.getSekolah().getNpsn();
				} else if (calonSiswa != null && calonSiswa.getSekolah() != null) {
					namaLayanan = calonSiswa.getSekolah().getNama();
					kodeLayanan = calonSiswa.getSekolah().getNpsn();
				}

				String noIdStr = siswa != null ? siswa.getNomorInduk() : (calonSiswa != null ? calonSiswa.getNomorInduk() : "");
				String reserveStr = (siswa != null ? siswa.getId() : (calonSiswa != null ? calonSiswa.getId() : "")) + "";
				String angkatanStr = (siswa != null ? siswa.getTahunMasuk() : (calonSiswa != null ? calonSiswa.getTahunMasuk() : "")) + "";

				jsonObject.put("ref", ref);
				jsonObject.put("va", va);
				jsonObject.put("nama", Common.maxPanjang(namaSiswaStr, 40));
				jsonObject.put("layanan", Common.maxPanjang(namaLayanan, 40));
				jsonObject.put("kodelayanan", kodeLayanan != null ? kodeLayanan : "");
				jsonObject.put("jenisbayar", jenisBiayaSekolah == null ? "00" : Common.maxPanjangAkhir(jenisBiayaSekolah.getNama(), 40));
				jsonObject.put("kodejenisbyr", jenisBiayaSekolah == null ? "00" : jenisBiayaSekolah.getKode());
				jsonObject.put("noid", noIdStr != null ? noIdStr : "");
				
				int safeBiayaAdmin = biayaAdmin != null ? biayaAdmin.intValue() : 0;
				jsonObject.put("tagihan", (safeBiayaAdmin + (int) totalVal) + "");
				jsonObject.put("flag", "F");
				jsonObject.put("reserve", reserveStr);
				jsonObject.put("angkatan", angkatanStr);
				jsonObject.put("expired", "");
				jsonObject.put("description", "");

				String postData = jsonObject.toString();

				System.out.println("Request body: ");
				System.out.println(postData);
				String strURL = (Common.getKonfigurasi("btn_gateway_url", "https://vabtn-dev.btn.co.id:9021/v1/bstimpr/createVA").getNilai());
				String hasil = DownloadTagihanMahasiswaBankBtn.post(postData, postData, strURL);
				System.out.println("Response body: ");
				System.out.println(hasil);

				JSONObject response = new JSONObject(hasil);

				if (response.has("rsp") && response.getString("rsp").equals("000")) {
					Long ptId = (PerguruanTinggiUtil.getPerguruanTinggi() != null) ? PerguruanTinggiUtil.getPerguruanTinggi().getId() : null;
					virtualAccountBankOnline = new VirtualAccountBank(ptId);
					virtualAccountBankOnline.setKadaluarsa(expired_date);
					virtualAccountBankOnline.setOtomatis(false);
					virtualAccountBankOnline.setKode(va);
					virtualAccountBankOnline.setRequest(postData);
					virtualAccountBankOnline.setResponse(response.toString());

					virtualAccountBankOnline.setCicilan(sbCicilan.toString());
					virtualAccountBankOnline.setKeterangan(sbKeterangan.toString() + (qris ? "qris:true" : ""));
					virtualAccountBankOnline.setTotal(totalVal);
					virtualAccountBankOnline.setBulanan("");
					virtualAccountBankOnline.setBiayaAdmin(biayaAdmin);

					virtualAccountBankOnline.setSiswa(siswa);
					virtualAccountBankOnline.setCalonSiswa(calonSiswa);
					virtualAccountBankOnline.setBankHost(bankHost);
					virtualAccountBankOnline.setAkunPembayaranSiswa(akunPembayaranSiswa);
					virtualAccountBankOnline.setBank("Bank BTN");

					session.saveOrUpdate(virtualAccountBankOnline);
				}
			}

			// Optimasi Transaksi: Update semua Tagihan sekaligus dalam 1 transaksi
			if (tagihans != null && virtualAccountBankOnline != null && virtualAccountBankOnline.getKode() != null) {
				for (Tagihan tagihan : tagihans) {
					tagihan.setVa(virtualAccountBankOnline.getKode());
					tagihan.setExpired(expired_date);
					session.update(tagihan);
				}
			}

			if (isLocalTransaction) {
				MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);
			}

			return virtualAccountBankOnline;

		} catch (Exception e) {
			if (isLocalTransaction) {
				MahasiswaVirtualAccountHelper.rollbackTransactionIfActive(session);
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBtn.java:639");
		} finally {
			MahasiswaVirtualAccountHelper.closeSessionQuietly(session);
		}
		
		return null;
	}
}