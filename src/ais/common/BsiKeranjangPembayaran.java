package ais.common;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.hibernate.Session;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import com.bni.encrypt.BNIHash;

import ais.action.master.bsi.BsiRequestAction;
import ais.action.report.Report;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.KegiatanTemporary;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.bsi.BsiRequest;
import ais.ui.util.MyMessageboxConfig;

/**
 * Integrasi pembayaran mahasiswa lewat gateway <b>BSI e-Collection</b> (Bank Syariah Indonesia,
 * memakai pustaka enkripsi bersama {@code com.bni.encrypt.BNIHash}) pada keranjang pembayaran
 * AIS: membangun nomor Virtual Account, menandatangani/mengenkripsi payload permintaan billing
 * lewat {@code BNIHash}, mengirimkannya ke gateway BSI (langsung ke {@code bsi_gateway_url} atau
 * lewat forwarder {@code bsi_ip_client} bila dikonfigurasi), menyimpan hasilnya sebagai entitas
 * {@link BsiRequest}, lalu menampilkan kode pembayaran ke mahasiswa/calon mahasiswa beserta bukti
 * PDF yang dikirim via email ({@link CommonEmail#infoBayarViaBsi}).
 *
 * <p>
 * Kelas ini mengikuti pola serupa integrasi payment gateway lain di paket {@code ais.common}
 * (bandingkan {@link JatelindoKeranjangPembayaran} untuk Jatelindo/Mandiri dan {@link BJBUtil}
 * untuk BJB): nomor VA dibentuk dari kombinasi prefix konfigurasi, {@code merchant_id}, dan
 * digit acak/NIM (diatur lewat saklar {@code angka_va_bsi_menggunakan_nim}); permintaan
 * dieksekusi via {@code org.apache.commons.httpclient} (Apache Commons HttpClient versi lama,
 * ditandai {@code @SuppressWarnings("deprecation")} pada {@link #sendRequest}); dan kegagalan
 * dicatat lewat mekanisme bersama {@link InfoTeknisPembayaran} serta {@code BsiCommon} agar pesan
 * error konsisten dengan kanal pembayaran lain.
 * </p>
 *
 * <h2>Peringatan keamanan — kredensial BSI tertanam sebagai nilai default</h2>
 * <p>
 * <b>Konfigurasi {@code bsi_password} pada {@link #onPilihBsi} memiliki nilai default yang
 * merupakan kunci/password enkripsi nyata dalam bentuk teks polos:
 * {@code "685dedd9f045787873794ead6276f8bf"}.</b> Nilai ini dipakai langsung sebagai parameter
 * {@code key} pada {@code BNIHash.hashData}/{@code BNIHash.parseData} untuk mengenkripsi dan
 * mendekripsi payload permintaan/respons BSI — bila konfigurasi runtime
 * {@code bsi_password} tidak diisi eksplisit, sistem otomatis memakai kredensial bawaan ini.
 * Karena berkas ini berada pada working copy SVN dan berpotensi sudah ter-commit ke riwayat
 * repositori, kredensial ini WAJIB dianggap berpotensi bocor. Dokumentasi ini TIDAK mengubah
 * maupun menghapus nilai tersebut — pemilik integrasi BSI disarankan meninjau apakah kunci ini
 * masih aktif di sisi bank dan mempertimbangkan rotasi serta pemindahan penuh ke konfigurasi
 * rahasia runtime tanpa default tertanam di kode.
 * </p>
 */
public class BsiKeranjangPembayaran {

	/**
	 * Titik masuk validasi + eksekusi pembuatan Virtual Account BSI untuk satu transaksi
	 * keranjang pembayaran mahasiswa. Menolak permintaan dengan nominal kurang dari {@code 0.01},
	 * lalu mendelegasikan pembuatan VA dan tampilan kode pembayaran ke {@link #onPilihBsi}.
	 *
	 * @param amn                        nominal yang harus dibayar
	 * @param mahasiswa                  mahasiswa yang membayar, boleh {@code null} bila
	 *                                   pembayaran atas nama calon mahasiswa
	 * @param biodataCalonMahasiswa      biodata calon mahasiswa (jalur PMB), boleh {@code null}
	 * @param selectedKegiatanTemporary  kumpulan item kegiatan/tagihan sementara yang dipilih
	 *                                   untuk dibayar pada transaksi ini
	 * @param event                      event ZK pemicu aksi
	 * @return {@code true} bila permintaan diteruskan untuk diproses; {@code false} bila nominal
	 *         kurang dari {@code 0.01} dan permintaan ditolak lebih awal
	 * @throws Exception diteruskan dari {@link #onPilihBsi} bila terjadi kegagalan tak terduga di
	 *                    luar penanganan internalnya
	 */
	@SuppressWarnings({})
	public static boolean onSaveBsi(final Double amn, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final Set<KegiatanTemporary> selectedKegiatanTemporary,
			final Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		onPilihBsi(amn, mahasiswa, biodataCalonMahasiswa, selectedKegiatanTemporary, event);

		return true;
	}

	/**
	 * Membangun payload permintaan billing BSI (nomor VA, tanggal kedaluwarsa, data
	 * pembayar, nominal) lalu mengenkripsinya lewat {@code BNIHash.hashData}, mengirimkannya
	 * lewat {@link #sendRequest}, dan bila berhasil menampilkan kode pembayaran (VA) ke pengguna
	 * lewat dialog {@link MyMessageboxConfig}. Setelah pengguna menutup dialog, secara asinkron
	 * (lewat {@link Common#createDefaultTimer}) membuat bukti pembayaran PDF
	 * ({@code Bukti_Bsi_Mahasiswa}) dan mengirimkannya via email lewat
	 * {@link CommonEmail#infoBayarViaBsi}.
	 *
	 * <p>
	 * Nomor Virtual Account dibentuk dari prefix konfigurasi {@code angka_prefix_va_bsi} (default
	 * {@code "8"}) + {@code merchant_id} (konfigurasi {@code bsi_merchant_id}, default
	 * {@code "000"}) + baik NIM mahasiswa/nomor registrasi calon mahasiswa (bila
	 * {@code angka_va_bsi_menggunakan_nim} aktif) maupun digit acak (konfigurasi
	 * {@code generated_angka_digit_bsi}, default 8), lalu dipotong/di-pad ke panjang tetap sesuai
	 * konfigurasi {@code virtual_account_angka_digit_bsi} (default 16). Bila pembuatan request
	 * gagal (mengembalikan {@code null} atau VA kosong), menampilkan pesan gagal generik lewat
	 * {@link InfoTeknisPembayaran#pesanGagal()}.
	 * </p>
	 *
	 * @param amn                        nominal yang harus dibayar
	 * @param mahasiswa                  mahasiswa yang membayar, boleh {@code null}
	 * @param biodataCalonMahasiswa      biodata calon mahasiswa, boleh {@code null}
	 * @param selectedKegiatanTemporary  item kegiatan/tagihan sementara yang dipilih
	 * @param event                      event ZK pemicu aksi
	 * @return selalu {@code true} — status gagal disampaikan lewat dialog pesan, bukan lewat nilai
	 *         balik
	 * @throws Exception diteruskan dari kegagalan yang tidak tertangani secara internal (mis. saat
	 *                    menyusun payload sebelum enkripsi)
	 */
	@SuppressWarnings("unchecked")
	public static boolean onPilihBsi(final Double amn, Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			final Set<KegiatanTemporary> selectedKegiatanTemporary, Event event) throws Exception {

		String merchant_id = Common.getKonfigurasi("bsi_merchant_id", "000").getNilai().trim();
		String Password = Common.getKonfigurasi("bsi_password", "685dedd9f045787873794ead6276f8bf").getNilai().trim();

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

		String bill_no = Common.getGeneratedBarCode();

		Date tanggalExpired = null;
		String tanggal_terakhir_pembayaran = Common.getKonfigurasi("tanggal_terakhir_pembayaran", "").getNilai();
		if (tanggal_terakhir_pembayaran != null && !tanggal_terakhir_pembayaran.trim().isEmpty()) {
			try {
				tanggalExpired = Common.dateFormat1.get().parse(tanggal_terakhir_pembayaran);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BsiKeranjangPembayaran.java:62");

			}
		}

		String datetime_expired = tanggalExpired != null ? Common.databaseDateFormat1.get().format(tanggalExpired)
				: Common.databaseDateFormat1.get().format(calendar.getTime());

		String virtual_account = "";

		if (Common.bolehKonfigurasi("angka_va_bsi_menggunakan_nim")) {
			virtual_account = Common.getKonfigurasi("angka_prefix_va_bsi", "8").getNilai() + merchant_id
					+ (mahasiswa != null ? mahasiswa.getNim()
							: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNoRegistrasi() : ""));
		} else {
			int generatedAngkaDigit = 8;
			try {
				generatedAngkaDigit = Integer
						.parseInt(Common.getKonfigurasi("generated_angka_digit_bsi", "8").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BsiKeranjangPembayaran.java:81");

			}
			virtual_account = Common.getKonfigurasi("angka_prefix_va_bsi", "8").getNilai() + merchant_id
					+ Common.getGeneratedAngkaDigit(generatedAngkaDigit);
		}

		Double biayaAdministrasi = 0.0;
		try {
			biayaAdministrasi = Double.parseDouble(Common.getKonfigurasi("bsi_biaya_administrasi", "0.0").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BsiKeranjangPembayaran.java:91");

		}

		int generatedAngkaDigit = 16;
		try {
			generatedAngkaDigit = Integer
					.parseInt(Common.getKonfigurasi("virtual_account_angka_digit_bsi", "16").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BsiKeranjangPembayaran.java:99");

		}
		virtual_account = (virtual_account + "00000000000000000").substring(0, generatedAngkaDigit);

		// BNIHash hash = new BNIHash();
		String data = "{\"customer_email\":\""
				+ (mahasiswa != null ? mahasiswa.getEmail().split(",")[0].trim()
						: (biodataCalonMahasiswa == null ? "test@email.com"
								: biodataCalonMahasiswa.getEmail().split(",")[0].trim()))
				+ "\",\"trx_id\":\"" + bill_no + "\",\"datetime_expired\":\"" + datetime_expired + "\",\"client_id\":\""
				+ merchant_id + "\",\"customer_phone\":\""
				+ (mahasiswa != null ? mahasiswa.getTelp()
						: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getHp() : ""))
				+ "\",\"customer_name\":\""
				+ (mahasiswa != null ? mahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "")
						: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "") : ""))
				+ "\",\"type\":\"createbilling\",\"virtual_account\":\"" + virtual_account + "\",\"trx_amount\":\""
				+ (biayaAdministrasi.intValue() + amn.intValue()) + "\",\"billing_type\":\"c\"}";

		String cid = merchant_id; // from BNI
		String key = Password; // from BNI

		String parsedData = BNIHash.hashData(data, cid, key);
		String decodeData = BNIHash.parseData(parsedData, cid, key);

		System.out.println("parsedData = " + parsedData);
		System.out.println("decodeData = " + decodeData);

		String postData = "{ \"client_id\":\"" + merchant_id + "\", \"data\":\"" + parsedData + "\"}";
		final BsiRequest bsiRequest = BsiKeranjangPembayaran.sendRequest(postData, mahasiswa, biodataCalonMahasiswa,
				selectedKegiatanTemporary, amn, merchant_id, data, bill_no, key, true);
		if (bsiRequest != null && bsiRequest.getVa() != null && !bsiRequest.getVa().trim().isEmpty()) {

			MyMessageboxConfig.show("Kode pembayaran Anda adalah " + bsiRequest.getVa() + " dengan tagihan "
					+ Common.numberFormat.get().format(amn)
					+ (biayaAdministrasi > 0.1
							? "\nBiaya administrasi " + Common.numberFormat.get().format(biayaAdministrasi)
									+ "\nJadi total tagihan " + Common.numberFormat.get().format(amn + biayaAdministrasi)
									+ "\nTerbilang " + IndonesianNumberToWords.convert((long) (amn + biayaAdministrasi))
							: "")
					+ "\n\nAnda dapat membayar tagihan ini dengan memasukkan kode \"" + bsiRequest.getVa()
					+ "\" di semua channel BNI.", "Pemberitahuan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings({ "rawtypes" })
								@Override
								public void onEvent(Event arg0) throws Exception {

									Double biayaAdministrasi = 0.0;
									try {
										biayaAdministrasi = Double.parseDouble(
												Common.getKonfigurasi("bsi_biaya_administrasi", "0.0").getNilai());
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BsiKeranjangPembayaran.java:157");

									}

									String info = "Kode Pembayaran\t\t: " + bsiRequest.getVa() + "\n";
									info += "Kode invoice\t\t\t: " + bsiRequest.getBillNo() + "\n";
									info += "Tagihan \t\t\t: " + Common.numberFormat.get().format(amn) + "\n";
									if (biayaAdministrasi > 0.1) {
										info += "Biaya admin \t\t\t: " + Common.numberFormat.get().format(biayaAdministrasi)
												+ "\n";
										info += "Total tagihan \t\t: "
												+ Common.numberFormat.get().format(amn + biayaAdministrasi) + "\n";
									}
									info += "Terbilang \t\t\t: "
											+ IndonesianNumberToWords.convert((long) (amn + biayaAdministrasi)) + "\n";
									if (bsiRequest.getMahasiswa() != null) {
										info += "NIM \t\t\t\t: " + bsiRequest.getMahasiswa().getNim() + "\n";
										info += "Nama \t\t\t\t: " + bsiRequest.getMahasiswa().getNama() + "\n";
									} else if (bsiRequest.getBiodataCalonMahasiswa() != null) {
										info += "No. Reg \t\t\t: "
												+ bsiRequest.getBiodataCalonMahasiswa().getNoRegistrasi() + "\n";
										if (bsiRequest.getBiodataCalonMahasiswa().getNoUjian() != null) {
											info += "No. Ujian \t\t\t: "
													+ bsiRequest.getBiodataCalonMahasiswa().getNoUjian() + "\n";
										}
										info += "Nama \t\t\t\t: " + bsiRequest.getBiodataCalonMahasiswa().getNama()
												+ "\n";
									}

									Map parameters = ais.common.HashMapGenerator.getRand();
									parameters.put("tanggal", bsiRequest.getTanggal_dirubah());
									parameters.put("bsiRequest", bsiRequest.getId());
									parameters.put("info", info);
									Report.generatePDFReport(Report.PDF, parameters, "Bukti_Bsi_Mahasiswa",
											ais.ui.util.WaktuUtil.getDate());

									try {
										File file = Report.generateFileReport(Report.PDF, parameters,
												"Bukti_Bsi_Mahasiswa", ais.ui.util.WaktuUtil.getDate(),
												Common.locale);
										CommonEmail.infoBayarViaBsi(bsiRequest, file);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BsiKeranjangPembayaran.java:198");

									}

								}
							}, "Menyiapkan pembayaran via bsi..");

						}
					});

		} else {
			MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);

		}

		return true;
	}

	/**
	 * Mengirim payload billing yang sudah dienkripsi ke endpoint gateway BSI (langsung ke
	 * {@code bsi_gateway_url}, default {@code https://apibeta.bsi-ecollection.com/}, atau lewat
	 * forwarder internal {@code bsi_ip_client + "/BsiForwarder"} bila dikonfigurasi) memakai
	 * Apache Commons HttpClient (API lama, method ini ditandai {@code @SuppressWarnings
	 * ("deprecation")}), mem-parse dan mendekripsi respons lewat {@code BNIHash.parseData}, lalu
	 * menyimpan hasilnya sebagai entitas {@link BsiRequest} dalam transaksi Hibernate tersendiri.
	 * Sebelum memulai, memanggil {@link InfoTeknisPembayaran#bersihkan()} agar detail kegagalan
	 * transaksi sebelumnya tidak ikut tampil pada alert transaksi baru. Karakter {@code '&'} pada
	 * {@code postData} diganti dengan kata {@code "dan"} sebelum dikirim.
	 *
	 * <p>
	 * Respons BSI diperiksa lewat kode {@code status}: bila bukan {@code "000"} (kode sukses),
	 * permintaan dianggap ditolak gateway — pesan/kode status dicatat ke
	 * {@link InfoTeknisPembayaran#catat(String)} dan method mengembalikan {@code null} tanpa
	 * menyimpan apa pun ke database. Bila status sukses namun penyimpanan ke database gagal, flag
	 * lokal {@code gagalSimpan} diset {@code true} (menandakan VA sudah diterbitkan pihak BSI
	 * walau gagal tercatat di aplikasi) dan diteruskan ke
	 * {@code BsiCommon#catatKegagalanBsi(Exception, String, boolean)} pada blok catch terluar
	 * untuk membedakan pesan error dari kegagalan sebelum permintaan terkirim.
	 * </p>
	 *
	 * @param postData                  payload JSON permintaan yang sudah dienkripsi/berisi
	 *                                   {@code client_id} dan {@code data} terenkripsi
	 * @param mahasiswa                 mahasiswa yang membayar, boleh {@code null}
	 * @param biodataCalonMahasiswa     biodata calon mahasiswa, boleh {@code null}
	 * @param selectedKegiatanTemporary item kegiatan/tagihan sementara yang dipilih; diambil satu
	 *                                  elemen pertamanya untuk menentukan semester dan tahun
	 *                                  akademik
	 * @param amount                    nominal yang harus dibayar
	 * @param merchant_id               kode merchant/client id BSI
	 * @param signature                 payload asli sebelum dienkripsi (dipakai untuk audit/log,
	 *                                  bukan dikirim langsung ke gateway)
	 * @param bill_no                   nomor tagihan/invoice yang dibuat lokal sebelum permintaan
	 *                                  dikirim
	 * @param key                       kunci enkripsi/dekripsi BSI (nilai {@code bsi_password})
	 * @param hapusCicilanSebelumnya    tandai apakah request cicilan/VA sebelumnya perlu
	 *                                  dihapus/digantikan
	 * @return entitas {@link BsiRequest} tersimpan bila berhasil penuh; {@code null} bila gateway
	 *         menolak permintaan (status bukan {@code "000"}); objek {@link BsiRequest} yang belum
	 *         tentu tersimpan bila terjadi kegagalan lain yang ditangkap secara internal
	 * @throws Exception dilempar ulang khusus dari kegagalan penyimpanan ke database (lihat
	 *                    {@code gagalSimpan})
	 */
	@SuppressWarnings("deprecation")
	public static BsiRequest sendRequest(String postData, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, final Set<KegiatanTemporary> selectedKegiatanTemporary,
			Double amount, String merchant_id, String signature, String bill_no, String key,
			Boolean hapusCicilanSebelumnya) throws Exception {
		// Bersihkan "Informasi Teknis" lama agar kegagalan sebelumnya tidak bocor ke alert transaksi ini.
		InfoTeknisPembayaran.bersihkan();

		KegiatanTemporary kegiatanTemporary = selectedKegiatanTemporary.iterator().next();

		postData = postData.replaceAll("&", "dan");

		// curl_init and url
		String ipClient = (Common.getKonfigurasi("bsi_ip_client", "").getNilai());
		if (!ipClient.trim().isEmpty()) {
			ipClient = ipClient + "/BsiForwarder";
		}
		String strURL = !ipClient.trim().isEmpty() ? ipClient
				: (Common.getKonfigurasi("bsi_gateway_url", "https://apibeta.bsi-ecollection.com/").getNilai());

		BsiRequest bsiRequest = new BsiRequest();
		System.out.println("postData = " + postData);

		// Penanda: VA sudah diterima BSI namun gagal disimpan ke DB (dipakai di blok catch luar).
		boolean gagalSimpan = false;
		PostMethod post = new PostMethod(strURL);
		try {
			StringRequestEntity requestEntity = new StringRequestEntity(postData);
			post.setRequestEntity(requestEntity);
			post.setRequestHeader("Content-type", "application/json");
			HttpClient httpclient = new HttpClient();

			int result = httpclient.executeMethod(post);
			System.out.println("Response status code: " + result);
			System.out.println("Response body: ");

			String hasil = post.getResponseBodyAsString();

			System.out.println(hasil);

			JSONObject bsi = new JSONObject(hasil);
			System.out.println("jSONObject = " + bsi);

			String status = bsi.isNull("status") ? "" : bsi.getString("status");

			if (!status.trim().equals("000")) {
				// BSI menolak permintaan — catat kode status + pesan server agar alert tidak generik.
				String pesanBsi = bsi.optString("message", "");
				if (pesanBsi == null || pesanBsi.trim().isEmpty()) pesanBsi = bsi.optString("description", "");
				InfoTeknisPembayaran.catat("Server BSI menolak permintaan, kode status=" + status
						+ (pesanBsi == null || pesanBsi.trim().isEmpty() ? "" : ", pesan=" + pesanBsi.trim())
						+ ". Respons server: " + InfoTeknisPembayaran.potong(hasil, 300) + ". URL: " + strURL);
				return null;
			}

			String data = bsi.isNull("data") ? "" : bsi.getString("data");

			// String decodeData = "";
			String decodeData = BNIHash.parseData(data, merchant_id, key);

			JSONObject responseData = new JSONObject(decodeData);
			System.out.println("responseData = " + responseData);

			bsiRequest.setHapusCicilanSebelumnya(hapusCicilanSebelumnya);
			bsiRequest.setNama(responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));

			bsiRequest.setTrxId(responseData.isNull("trx_id") ? "" : responseData.getString("trx_id"));
			bsiRequest.setVa(responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));
			bsiRequest.setBillNo(bill_no);
			bsiRequest.setMerchant_id(merchant_id);
			bsiRequest.setData(responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));
			bsiRequest.setMerchant(merchant_id);
			bsiRequest.setResponse_code(status);
			bsiRequest.setResponse_desc(BsiRequestAction.statses.get(status));
			bsiRequest.setMahasiswa(mahasiswa);
			bsiRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			// bsiRequest.setJenisKegiatan(jenisKegiatan);
			// bsiRequest.setJadwalPembayaran(jadwalPembayaran);
			bsiRequest.setSemester(kegiatanTemporary.getSemster());
			bsiRequest.setTahunAkademik(kegiatanTemporary.getTahunAkademik());
			// bsiRequest.setKeterangan(keterangan);
			// bsiRequest.setPengurangan(pengurangan);
			bsiRequest.setNilaiBiayaHarusDiBayars(amount);
			bsiRequest.setAmount(amount);
			bsiRequest.setResponse(bsi.toString());
			bsiRequest.setRequest(postData);
			bsiRequest.setKegiatanTemporarys(selectedKegiatanTemporary);

			Session session = null;
			try {
				session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.save(bsiRequest);
				session.getTransaction().commit();
			} catch (Exception se) {
				// VA sudah diterima BSI; kegagalan di sini = gagal simpan DB (dicatat di catch luar).
				gagalSimpan = true;
				try {
					if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
				} catch (Exception re) { ais.common.ErrorAuditUtil.record(re, "auto-audit(empty-catch) src/ais/common/BsiKeranjangPembayaran.java:306");
				}
				throw se;
			} finally {
				Common.closeNativeSessionQuietly(session);
			}

		} catch (Exception e) {
			// Catat penyebab kegagalan untuk alert "Informasi Teknis" — pakai helper bersama BsiCommon.
			BsiCommon.catatKegagalanBsi(e, strURL, gagalSimpan);
			Common.tampilErrorJikaAdmin(e);
		} finally {
			post.releaseConnection();
		}

		return bsiRequest;
	}

}
