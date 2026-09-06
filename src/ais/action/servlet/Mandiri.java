package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.Tagihan;

/**
 * Servlet endpoint <b>Host-to-Host (H2H) Bank Mandiri</b> untuk Virtual Account
 * (VA) pembayaran: menerima request <i>inquiry</i> (tampilkan rincian tagihan)
 * dan <i>payment</i> (catat pelunasan) yang dikirim langsung oleh sistem Bank
 * Mandiri ke server AIS.
 *
 * <h3>Protokol</h3>
 * Bank mengirim HTTP request (GET/POST) berisi body JSON dengan salah satu dari
 * dua amplop di tingkat akar:
 * <ul>
 * <li>{@code InquiryRequest} &rarr; dijawab dengan {@code InquiryResponse}
 * (berisi identitas pembayar, {@code billInfo1}..{@code billInfo8}, dan rincian
 * {@code billDetails.BillDetail});</li>
 * <li>{@code paymentRequest} &rarr; dijawab dengan {@code paymentResponse}
 * (tanpa rincian tagihan, hanya status).</li>
 * </ul>
 * Nomor VA dibaca dari {@code billKey1}, nominal dari {@code billKey2}
 * (inquiry) atau {@code paymentAmount} (payment), dan waktu transaksi dari
 * {@code trxDateTime}. Setiap respons selalu memuat objek {@code status} dengan
 * {@code isError}/{@code errorCode}/{@code statusDescription}; kode error yang
 * dipakai: {@code 00} sukses, {@code B5} tagihan tidak ditemukan/nominal tidak
 * cocok, {@code B8} tagihan sudah lunas, {@code C0} tagihan kedaluwarsa,
 * {@code 87} masalah database provider, {@code 89} timeout.
 *
 * <h3>PENTING &mdash; model autentikasi endpoint ini</h3>
 * Kelas ini <b>tidak melakukan verifikasi tanda tangan digital, MAC, maupun
 * token</b> atas payload bank. Satu-satunya pengenalan pemanggil adalah
 * pencocokan alamat IP pemanggil terhadap tabel {@link BankHost} lewat
 * {@link PembayaranUtil#getBankHost(String, String)} di {@link #process}, dan
 * hasilnya (objek {@code bankHost}) <b>tidak dipakai sebagai gerbang</b>: alur
 * {@link #doProcess} sengaja tetap berjalan walau {@code bankHost} bernilai
 * {@code null} supaya seluruh lalu lintas tetap tercatat pada Log
 * Host-to-Host. Konsekuensinya, kendali akses efektif endpoint ini bertumpu
 * sepenuhnya pada pembatasan jaringan/firewall di depan aplikasi. Lihat catatan
 * rinci pada {@link #process}.
 *
 * <h3>Hubungan dengan Log Host-to-Host</h3>
 * Pada blok {@code finally} di {@link #doProcess}, seluruh request &mdash;
 * sukses maupun gagal &mdash; diteruskan ke
 * {@code PembayaranGatewayHelper.catatLogHostToHost(...)} bersama
 * <b>payload JSON mentah</b> apa adanya. Kelas ini karenanya merupakan salah
 * satu penyumbang utama isi tabel {@code LogHostToHost}, termasuk data pribadi
 * pembayar (nama, NIM/nomor induk, nominal) yang ikut tersimpan permanen.
 *
 * @see Bankaltimtara
 * @see Bniresponse
 * @see VirtualAccountBank
 * @see LogHostToHost
 */
public class Mandiri extends HttpServlet {
	/** Versi serialisasi standar {@link HttpServlet}; tidak dipakai secara fungsional. */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton utilitas pembayaran; dipakai di sini untuk memetakan alamat IP
	 * pemanggil ke baris {@link BankHost} dan untuk menghitung total/denda
	 * cicilan sebuah {@link Kegiatan}.
	 */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * Formatter {@code yyyyMMddHHmmss} untuk field {@code trxDateTime} dari bank.
	 * Dibungkus {@link ThreadLocal} karena {@link SimpleDateFormat} tidak aman
	 * dipakai bersama antar-thread, sementara servlet ini dilayani banyak thread
	 * Tomcat sekaligus.
	 *
	 * <p>
	 * Catatan: bank hanya mengirim 10 digit ({@code MMddHHmmss}); tahun disisipkan
	 * dari jam server saat parsing di {@link #doProcess}.
	 */
	private static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
		/**
		 * Membuat instance formatter baru untuk setiap thread yang pertama kali
		 * mengakses {@link #dateFormat}.
		 *
		 * @return formatter pola {@code yyyyMMddHHmmss} milik thread pemanggil
		 */
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMddHHmmss");
		}
	};

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan kontainer servlet untuk
	 * meng-instansiasi endpoint ini.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public Mandiri() {
		super();
	}

	/**
	 * Menangani request HTTP GET dari Bank Mandiri dengan mendelegasikan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p>
	 * Seluruh exception ditelan di sini (dicatat ke Error Log) supaya kegagalan
	 * internal tidak pernah membuat koneksi ke bank putus tanpa respons; badan
	 * respons error yang wajar sudah disusun di lapisan {@link #doProses}.
	 *
	 * @param request  request masuk dari gateway bank
	 * @param response respons yang akan diisi badan JSON
	 * @throws ServletException bila kontainer servlet gagal memproses request
	 * @throws IOException      bila terjadi kegagalan I/O saat menulis respons
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			ais.common.ErrorAuditUtil.record(e, "Mandiri H2H doGet gagal", request);
		}
	}

	/**
	 * Menangani request HTTP POST dari Bank Mandiri &mdash; jalur normal
	 * inquiry/payment H2H &mdash; dengan mendelegasikan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p>
	 * Sama seperti {@link #doGet}, exception ditelan dan dicatat ke Error Log agar
	 * bank selalu menerima balasan.
	 *
	 * @param request  request masuk dari gateway bank; badan request dibaca sebagai
	 *                 JSON mentah
	 * @param response respons yang akan diisi badan JSON
	 * @throws ServletException bila kontainer servlet gagal memproses request
	 * @throws IOException      bila terjadi kegagalan I/O saat menulis respons
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			ais.common.ErrorAuditUtil.record(e, "Mandiri H2H doPost gagal", request);
		}
	}

	/**
	 * Inti pemrosesan H2H Mandiri: mencari Virtual Account, memvalidasinya, lalu
	 * menyusun objek JSON respons untuk bank &mdash; sekaligus (pada mode
	 * <i>payment</i>) benar-benar membukukan pembayaran ke database.
	 *
	 * <h3>Urutan validasi</h3>
	 * <ol>
	 * <li><b>VA tidak kosong</b> &mdash; bila {@code va} null/blank dijawab
	 * {@code B5 Invalid Virtual Account}.</li>
	 * <li><b>Aturan prefix</b> &mdash; konfigurasi
	 * {@code prefix_wajib_diterima_pembayaran_mandiri} dan
	 * {@code bukan_prefix_wajib_diterima_pembayaran_mandiri} menyaring nomor VA
	 * yang bukan milik instalasi ini; gagal dijawab {@code B5 Bill not found}.</li>
	 * <li><b>Pencarian VA</b> lewat
	 * {@link VirtualAccountBank#ambilVa(String, double, BankHost)}.</li>
	 * <li><b>Kedaluwarsa</b> &mdash; dilewati bila {@code chekLagi} true; gagal
	 * dijawab {@code C0 Bill suspend}.</li>
	 * <li><b>Sudah terbayar</b> &mdash; dilewati bila {@code chek} true; gagal
	 * dijawab {@code B8 Bill has been Already Paid}.</li>
	 * <li><b>Kecocokan nominal</b> &mdash; <i>hanya pada mode payment</i>
	 * ({@code inquery == false}) nilai {@code nominalP} dibandingkan dengan
	 * {@code virtualAccountBank.totalBiaya()}; selisih apa pun dijawab
	 * {@code B5 Bill not found}. Pada mode inquiry nominal memang belum
	 * diverifikasi karena bank baru menanyakan besaran tagihan.</li>
	 * </ol>
	 *
	 * <h3>Dua jalur entitas</h3>
	 * Setelah VA valid, alur bercabang sesuai pemilik tagihan:
	 * <ul>
	 * <li><b>Siswa/calon siswa</b> &mdash; rincian tagihan diambil lewat
	 * {@link VirtualAccountBank#bayarSiswa} dan dipetakan menjadi entri
	 * {@code BillDetail} (item biaya dan dendanya terpisah, prefix {@code D} untuk
	 * denda);</li>
	 * <li><b>Mahasiswa/calon mahasiswa</b> &mdash; sebuah {@link Kegiatan}
	 * dibuat/diperbarui, lalu token pada kolom {@code cicilan} milik VA diurai satu
	 * per satu menjadi baris {@link CicilanPembayaran} (format token: numerik
	 * murni, {@code Bulanan-}, {@code Item-}, atau {@code Keranjang-}); penutupan
	 * VA dilakukan {@link VirtualAccountBank#updateVa}.</li>
	 * </ul>
	 * Bila VA menyimpan nilai {@code tabungan}, entri tagihan tidak dikirim satu per
	 * satu melainkan diakumulasi dan diringkas menjadi satu baris {@code TBG01}.
	 *
	 * <h3>Jejak audit</h3>
	 * Setiap tahap dicatat ke daftar {@code jejakLangkah} di memori. Jejak ini hanya
	 * dicetak penuh ke konsol dan disimpan ke kolom {@code stackTrace} Log
	 * Host-to-Host bila status akhir berupa error, supaya lalu lintas sukses yang
	 * bervolume tinggi tidak membanjiri konsol/DB. Blok {@code finally} menjamin
	 * {@code catatLogHostToHost(...)} <b>selalu</b> dipanggil &mdash; itulah alasan
	 * seluruh badan method dibungkus blok tanpa syarat {@code bankHost}, sehingga
	 * request dari IP yang tidak dikenal pun tetap terekam.
	 *
	 * @param nominalP  nominal yang dikirim bank; pada mode payment wajib sama
	 *                  persis dengan total biaya VA
	 * @param tanggalP  waktu transaksi dari bank berformat {@code MMddHHmmss};
	 *                  tahun ditambahkan dari jam server, dan bila gagal diurai
	 *                  dipakai waktu server sebagai cadangan
	 * @param va        nomor Virtual Account yang ditagih ({@code billKey1})
	 * @param bank      nama bank pembayar; disimpan sebagai {@code validator} pada
	 *                  {@link Kegiatan}/{@link CicilanPembayaran}
	 * @param bankHost  baris {@link BankHost} hasil pencocokan IP pemanggil; boleh
	 *                  {@code null} &mdash; alur tetap berjalan, dan jenis
	 *                  pembayaran jatuh ke {@code ConstantValues.TUNAI}
	 * @param request   request HTTP asli; boleh {@code null} bila dipanggil dari cek
	 *                  ulang manual menu Log Host-to-Host
	 * @param data      payload JSON mentah dari bank; disimpan apa adanya ke Log
	 *                  Host-to-Host
	 * @param chekLagi  {@code true} melewati pemeriksaan kedaluwarsa VA (dipakai
	 *                  jalur cek ulang/rekonsiliasi)
	 * @param inquery   {@code true} untuk inquiry (menyusun rincian tagihan tanpa
	 *                  membukukan), {@code false} untuk payment (membukukan)
	 * @param chek      {@code true} bila dipicu admin dari menu Log Host-to-Host
	 *                  &mdash; melewati pemeriksaan "sudah terbayar" dan mengaktifkan
	 *                  dump konsol {@link #logDetailCekUlang}
	 * @return objek JSON siap kirim berisi {@code InquiryResponse} atau
	 *         {@code paymentResponse}; tidak pernah {@code null}
	 * @throws Exception bila terjadi kegagalan di luar yang sudah ditangani internal
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject doProcess(double nominalP, String tanggalP, String va, String bank, BankHost bankHost,
			HttpServletRequest request, String data, boolean chekLagi, boolean inquery, boolean chek) throws Exception {

		JSONObject response = new JSONObject();

		JSONObject jsonObjectResponse = new JSONObject();
		if (inquery) {
			jsonObjectResponse.put("InquiryResponse", response);
		} else {
			jsonObjectResponse.put("paymentResponse", response);
		}
		response.put("billInfo1", "");
		response.put("billInfo2", "");
		response.put("billInfo3", "");
		JSONArray BillDetail = new JSONArray();

		if (inquery) {
			JSONObject billDetails = new JSONObject();
			response.put("billDetails", billDetails);
			billDetails.put("BillDetail", BillDetail);
			response.put("currency", "360");
		}

		JSONObject status = new JSONObject();
		response.put("status", status);
		status.put("isError", "true");
		status.put("errorCode", "87");
		status.put("statusDescription", "Provider Database Problem");

		{ // TANPA syarat bankHost: request bank apa pun WAJIB tercatat ke log H2H (lihat finally)
			String nim = "";
			String nama = "";
			JSONArray rincian = new JSONArray();
			// Jejak stack trace bila inquiry/pembayaran error; disimpan ke kolom log H2H.
			String h2hStackTrace = null;

			// JEJAK LANGKAH-DEMI-LANGKAH: setiap tahap penting alur inquiry/pembayaran
			// dicatat di sini (murah â€” sekadar List<String> di memori). SELALU diisi,
			// tapi HANYA dicetak penuh ke konsol + disimpan ke kolom stackTrace pada
			// Log Host-to-Host ketika status akhir berupa ERROR (lihat blok finally) â€”
			// supaya lalu-lintas H2H yang sukses (volume tinggi) tidak membanjiri
			// konsol/DB, sementara SETIAP kegagalan langsung punya jejak lengkap dari
			// request masuk sampai response keluar, tanpa perlu reproduksi manual.
			final List<String> jejakLangkah = new ArrayList<String>();
			jejakLangkah.add("1. REQUEST DITERIMA: bank=" + bank + ", va=" + va + ", nominal=" + nominalP
					+ ", tanggal=" + tanggalP + ", inquiry=" + inquery + ", chekLagi=" + chekLagi + ", chek=" + chek
					+ ", bankHost=" + (bankHost == null ? "(null)" : bankHost.getId()));
			if (data != null) {
				jejakLangkah.add("   payload mentah: " + data);
			}

			VirtualAccountBank virtualAccountBankNtt = null;
			try {

				// 1. Pastikan VA tidak null untuk mencegah NPE saat memanggil va.startsWith()
				if (va == null || va.trim().isEmpty()) {
					status.put("isError", "true");
					status.put("errorCode", "B5"); // Atau kode error khusus invalid VA
					status.put("statusDescription", "Invalid Virtual Account");
					jejakLangkah.add("2. VALIDASI VA: GAGAL â€” parameter va kosong/null.");

				} else {
					// 2. Ambil konfigurasi dengan aman (Safe check)
					String prefixWajib = "";
					String prefixTerlarang = "";

					// Asumsi getKonfigurasi bisa null, atau getNilai bisa null
					Konfigurasi configWajib = Common.getKonfigurasi("prefix_wajib_diterima_pembayaran_mandiri", "");
					if (configWajib != null && configWajib.getNilai() != null) {
						prefixWajib = configWajib.getNilai().trim();
					}

					Konfigurasi configTerlarang = Common
							.getKonfigurasi("bukan_prefix_wajib_diterima_pembayaran_mandiri", "");
					if (configTerlarang != null && configTerlarang.getNilai() != null) {
						prefixTerlarang = configTerlarang.getNilai().trim();
					}

					// 3. Logika pengecekan dengan tanda kurung yang memperjelas scope
					boolean isMissingRequiredPrefix = !prefixWajib.isEmpty() && !va.startsWith(prefixWajib);
					boolean hasForbiddenPrefix = !prefixTerlarang.isEmpty() && va.startsWith(prefixTerlarang);
					jejakLangkah.add("2. VALIDASI PREFIX VA: prefixWajib='" + prefixWajib + "', prefixTerlarang='"
							+ prefixTerlarang + "', isMissingRequiredPrefix=" + isMissingRequiredPrefix
							+ ", hasForbiddenPrefix=" + hasForbiddenPrefix);

					if (isMissingRequiredPrefix || hasForbiddenPrefix) {
						status.put("isError", "true");
						status.put("errorCode", "B5");
						status.put("statusDescription", "Bill not found");
						jejakLangkah.add("   â†’ DITOLAK: VA '" + va + "' tidak memenuhi aturan prefix konfigurasi.");
					} else {
						virtualAccountBankNtt = VirtualAccountBank.ambilVa(va, nominalP, bankHost);
						try {
							jejakLangkah.add("3. PENCARIAN VA (VirtualAccountBank.ambilVa): ditemukan="
									+ (virtualAccountBankNtt != null)
									+ (virtualAccountBankNtt == null ? ""
											: ", id=" + virtualAccountBankNtt.getId() + ", total="
													+ virtualAccountBankNtt.getTotal() + ", biayaAdmin="
													+ virtualAccountBankNtt.getBiayaAdmin() + ", kadaluarsa="
													+ virtualAccountBankNtt.getKadaluarsa()));
						} catch (Exception eJejak) {
							jejakLangkah.add("3. PENCARIAN VA: ditemukan=" + (virtualAccountBankNtt != null)
									+ " (gagal baca detail: " + eJejak + ")");
						}

						if (virtualAccountBankNtt != null && virtualAccountBankNtt.getKadaluarsa() != null) {
							System.out.println("Kadaluara "
									+ Common.databaseDateFormat.get().format(virtualAccountBankNtt.getKadaluarsa()));
						}
						Date tanggal = ais.ui.util.WaktuUtil.getDate();
						try {
							tanggal = dateFormat.get().parse(Calendar.getInstance().get(Calendar.YEAR) + tanggalP);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Mandiri.java:172");
							ais.common.ErrorAuditUtil.record(e, "Mandiri H2H: gagal parse tanggal transaksi '" + tanggalP + "'; VA=" + va);
							jejakLangkah.add("   PERINGATAN: gagal parse tanggal transaksi '" + tanggalP
									+ "', fallback ke waktu server: " + e);
						}

						Date msk = virtualAccountBankNtt == null ? null : virtualAccountBankNtt.getKadaluarsaWaktu();
						if (!chekLagi && (tanggal != null && msk != null
								&& Double.parseDouble(Common.dateFormat84.get().format(msk)) < Double
										.parseDouble(Common.dateFormat84.get().format(tanggal)))) {
							status.put("isError", "true");
							status.put("errorCode", "C0");
							status.put("statusDescription", "Bill suspend");
							jejakLangkah.add("4. CEK KADALUARSA: DITOLAK â€” VA sudah lewat batas waktu bayar (msk="
									+ msk + ", tanggalTrx=" + tanggal + ").");
						} else {
							jejakLangkah.add("4. CEK KADALUARSA: OK (msk=" + msk + ", tanggalTrx=" + tanggal + ").");

							if (!chek && virtualAccountBankNtt != null
									&& VirtualAccountBank.isSudahTerbayar(virtualAccountBankNtt)) {
								status.put("isError", "true");
								status.put("errorCode", "B8");
								status.put("statusDescription", "Bill has been Already Paid");
								jejakLangkah.add("5. CEK SUDAH TERBAYAR: DITOLAK â€” VA " + va + " sudah lunas.");
							}

							else if (!chek && virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1
									&& (virtualAccountBankNtt.getKegiatan() != null
											|| virtualAccountBankNtt.getPembayaran() != null)) {
								status.put("isError", "true");
								status.put("errorCode", "B8");
								status.put("statusDescription", "Bill has been Already Paid");
								jejakLangkah.add("5. CEK SUDAH TERBAYAR: DITOLAK â€” VA " + va
										+ " sudah tertaut kegiatan/pembayaran (kegiatan="
										+ virtualAccountBankNtt.getKegiatan() + ", pembayaran="
										+ virtualAccountBankNtt.getPembayaran() + ").");
							}

							else if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {
								jejakLangkah.add("5. CEK SUDAH TERBAYAR: OK (belum lunas, lanjut proses).");
								Session session = HibernateUtil.getSessionFactory().openSession();
								try {

									Double nominal = nominalP;

									if (!inquery && nominal.intValue() != virtualAccountBankNtt.totalBiaya()) {
										status.put("isError", "true");
										status.put("errorCode", "B5");
										status.put("statusDescription", "Bill not found");
										jejakLangkah.add("6. VALIDASI NOMINAL (payment): DITOLAK â€” nominal dari bank="
												+ nominal.intValue() + " != totalBiaya seharusnya="
												+ virtualAccountBankNtt.totalBiaya());
									} else {
										jejakLangkah.add("6. VALIDASI NOMINAL: OK (nominal=" + nominal
												+ ", inquiry=" + inquery + ").");
										int totalTabungan = 0;
										if (inquery) {
											if (virtualAccountBankNtt.getBiayaAdmin() > 0.1) {
												JSONObject bill = new JSONObject();
												bill.put("billCode", "0010");
												bill.put("billName", "Biaya admin");
												bill.put("billShortName", "0010");
												bill.put("billAmount",
														virtualAccountBankNtt.getBiayaAdmin().intValue() + ".00");

												bill.put("reference1", "");
												bill.put("reference2", "");
												bill.put("reference3", "");

												if (virtualAccountBankNtt.getTabungan() > 0.1) {
													totalTabungan += virtualAccountBankNtt.getBiayaAdmin().intValue();
												} else {
													BillDetail.put(bill);
												}

											}

										}

										if (virtualAccountBankNtt.getSiswa() != null
												|| virtualAccountBankNtt.getCalonSiswa() != null) {
											jejakLangkah.add("7. JALUR ENTITAS: SISWA/CALON SISWA (siswa="
													+ virtualAccountBankNtt.getSiswa() + ", calonSiswa="
													+ virtualAccountBankNtt.getCalonSiswa() + ").");

											if (virtualAccountBankNtt.getSiswa() != null) {
												nim = virtualAccountBankNtt.getSiswa().getNomorInduk();
												nama = virtualAccountBankNtt.getSiswa().getNama();
											} else if (virtualAccountBankNtt.getCalonSiswa() != null) {
												nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk();
												nama = virtualAccountBankNtt.getCalonSiswa().getNama();
											}

											response.put("billInfo1",
													virtualAccountBankNtt.getAkunPembayaranSiswa() == null ? ""
															: virtualAccountBankNtt.getAkunPembayaranSiswa().getNama()
																	+ " " + virtualAccountBankNtt.getSemester());
											response.put("billInfo2", nama);
											response.put("billInfo3", nim);
											response.put("billInfo4", virtualAccountBankNtt.getTahunAkademik());
											response.put("billInfo5",
													virtualAccountBankNtt.getAkunPembayaranSiswa() == null ? ""
															: virtualAccountBankNtt.getAkunPembayaranSiswa().getNama());

											Map<String, List<Tagihan>> map = VirtualAccountBank.bayarSiswa(
													virtualAccountBankNtt, session, tanggal, bank, inquery, data,
													false);
											jejakLangkah.add("8. PROSES bayarSiswa(): jumlah kelompok tagihan="
													+ (map == null ? 0 : map.size()));

											for (List<Tagihan> tagihans : map.values()) {

												for (Tagihan tagihan : tagihans) {

													if (inquery) {
														JSONObject bill = new JSONObject();
														bill.put("billCode", tagihan.getItemBiayaSekolah().getKode());
														bill.put("billName", tagihan.getItemBiayaSekolah().getNama());
														bill.put("billShortName",
																tagihan.getItemBiayaSekolah().getKode());
														bill.put("billAmount", tagihan.getNominal().intValue() + ".00");

														bill.put("reference1", "");
														bill.put("reference2", "");
														bill.put("reference3", "");

														if (virtualAccountBankNtt.getTabungan() > 0.1) {
															totalTabungan += tagihan.getNominal().intValue();
														} else {
															BillDetail.put(bill);
														}

														if (tagihan.getDenda() > 0.1) {
															bill = new JSONObject();
															bill.put("billCode",
																	"D" + tagihan.getItemBiayaSekolah().getKode());
															bill.put("billName",
																	"Denda " + tagihan.getItemBiayaSekolah().getNama());
															bill.put("billShortName",
																	"D" + tagihan.getItemBiayaSekolah().getKode());
															bill.put("billAmount",
																	tagihan.getDenda().intValue() + ".00");

															bill.put("reference1", "");
															bill.put("reference2", "");
															bill.put("reference3", "");

															if (virtualAccountBankNtt.getTabungan() > 0.1) {
																totalTabungan += tagihan.getDenda().intValue();
															} else {
																BillDetail.put(bill);
															}
														}

													}
												}

											}
											if (inquery) {
												if (virtualAccountBankNtt.getTabungan() > 0.1) {
													JSONObject bill = new JSONObject();
													bill.put("billCode", "TBG01");
													bill.put("billName", "Total Item Biaya Dipotong Tabungan");
													bill.put("billShortName", "Tabungan");
													bill.put("billAmount",
															(totalTabungan
																	- virtualAccountBankNtt.getTabungan().intValue())
																	+ ".00");

													bill.put("reference1", "");
													bill.put("reference2", "");
													bill.put("reference3", "");

													BillDetail.put(bill);
												}
											}

											if (inquery) {
												status.put("isError", "false");
												status.put("errorCode", "00");
												status.put("statusDescription", "Inquiry success");
											} else {

												status.put("isError", "false");
												status.put("errorCode", "00");
												status.put("statusDescription", "Payment success");
											}

										} else {
											jejakLangkah.add("7. JALUR ENTITAS: MAHASISWA/CALON MAHASISWA (bukan siswa).");

											JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
											Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
											BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt
													.getBiodataCalonMahasiswa();

											Integer semester = virtualAccountBankNtt.getSemester();

											// FIX NPE: VA yang tidak tertaut ke entitas manapun (siswa/
											// calonSiswa/mahasiswa/biodataCalonMahasiswa semua null --
											// skenario "vaDitemukan=false"/errorCode 87) membuat
											// biodataCalonMahasiswa ikut null di jalur ini, padahal
											// sebelumnya diakses langsung tanpa null-check.
											nim = mahasiswa != null ? mahasiswa.getNim()
													: (biodataCalonMahasiswa == null ? ""
															: biodataCalonMahasiswa.getNoRegistrasi());
											nama = mahasiswa != null ? mahasiswa.getNama()
													: (biodataCalonMahasiswa == null ? ""
															: biodataCalonMahasiswa.getNama());

											Jurusan jurusan = mahasiswa == null ? null : mahasiswa.getJurusan();
											if (biodataCalonMahasiswa != null
													&& biodataCalonMahasiswa.getProdiLulus() != null) {
												jurusan = biodataCalonMahasiswa.getProdiLulus();
											} else if (biodataCalonMahasiswa != null
													&& biodataCalonMahasiswa.getProdi1() != null) {
												jurusan = biodataCalonMahasiswa.getProdi1();
											}

											response.put("billInfo1", nama + " " + virtualAccountBankNtt.getSemester());
											response.put("billInfo2", nama);
											response.put("billInfo3", nim);
											response.put("billInfo4", virtualAccountBankNtt.getTahunAkademik());
											// FIX NPE: jenisKegiatan bisa null pada jalur VA yg tak
											// tertaut sempurna (sama spt kasus nim/nama di atas).
											response.put("billInfo5",
													jenisKegiatan == null ? "" : jenisKegiatan.getNamaKegiatan());
											response.put("billInfo6", jurusan == null ? "" : jurusan.getNama());
											// FIX NPE laten: jurusan.getFakultas() sendiri bisa null
											// (guard sebelumnya cuma cek jurusan != null).
											response.put("billInfo7",
													jurusan == null || jurusan.getFakultas() == null ? ""
															: jurusan.getFakultas().getNama());
											response.put("billInfo8", jurusan == null || jurusan.getFakultas() == null
													|| jurusan.getFakultas().getPerguruanTinggi() == null ? ""
															: jurusan.getFakultas().getPerguruanTinggi().getNama());

											Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null
													? null
													: session.createCriteria(Kegiatan.class)
															.add(Restrictions.idEq(virtualAccountBankNtt.getKegiatan()))
															.uniqueResult());

											if (kegiatan == null || kegiatan.getId() == null) {

												kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class)

														.addOrder(Order.asc("id"))

														.add(biodataCalonMahasiswa != null
																? Restrictions.eq("calonMahasiswa",
																		biodataCalonMahasiswa)
																: Restrictions.eq("mahasiswa", mahasiswa))
														.add(Restrictions.eq("jenisKegiatan",
																virtualAccountBankNtt.getJenisKegiatan()))
														.add(Restrictions.eq("semster", semester))

														.setMaxResults(1).uniqueResult();
											}

											if (kegiatan == null || kegiatan.getId() == null) {
												kegiatan = new Kegiatan();
											}

											kegiatan.setKodeUnikLain(virtualAccountBankNtt.getKodeUnikLain());
											kegiatan.setJadwalPembayaran(virtualAccountBankNtt.getJadwalPembayaran());
											kegiatan.setNama(nama);
											kegiatan.setUploadVirtualAccount(null);
											kegiatan.setAmount(virtualAccountBankNtt.getTotal());
											kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
											kegiatan.setMahasiswa(mahasiswa);
											kegiatan.setTahunAkademik(virtualAccountBankNtt.getTahunAkademik());
											kegiatan.setSemster(semester);
											kegiatan.setJenisKegiatan(jenisKegiatan);
											kegiatan.setTanggal(tanggal);
											kegiatan.setValidated(1);
											kegiatan.setValidator(bank);

											session.getTransaction().begin();
											Common.refreshSaveOrUpdate(session, kegiatan);
											session.getTransaction().commit();
											jejakLangkah.add("9. KEGIATAN disimpan/diperbarui: id=" + kegiatan.getId()
													+ ", nim=" + nim + ", jenisKegiatan="
													+ (jenisKegiatan == null ? "(null)" : jenisKegiatan.getNamaKegiatan())
													+ ", semester=" + semester);

											List<Long> detailBiayasId = new ArrayList<Long>();
											for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(),
													",")) {
												try {
													detailBiayasId.add(Long.parseLong(id.trim()));
												} catch (Exception e) {
													ais.common.ErrorAuditUtil.record(e, "Mandiri H2H: gagal parse id detailbiaya '" + id + "' dari VA " + va);
												}
											}

											Collection<DetailBiaya> detailBiayas = session
													.createCriteria(DetailBiaya.class)
													.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
															: Restrictions.in("id", detailBiayasId))
													.list();

											Double nilaiBiayaHarusDiBayars = 0.0;
											for (DetailBiaya detailBiaya : detailBiayas) {
												Double biaya = detailBiaya.hitungTotalKegiatan(kegiatan, session);

												nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);

											}

											Double[] totalCicilan = kegiatan.hitungTotalDanDendaFromCicilan();
											Double total = totalCicilan[0];
											Double totalTagihan = kegiatan.getAmount() + kegiatan.getAmountTerhutang();
											System.out.println("cicilanPembayaran total -> " + total + " totalTagihan "
													+ totalTagihan);
											jejakLangkah.add("10. DETAIL BIAYA: jumlah item=" + detailBiayas.size()
													+ ", nilaiBiayaHarusDiBayars=" + nilaiBiayaHarusDiBayars
													+ ", totalCicilanSaatIni=" + total + ", totalTagihan=" + totalTagihan);

											if (virtualAccountBankNtt.getCicilan() != null
													&& !virtualAccountBankNtt.getCicilan().isEmpty()) {
												String[] tokenCicilan = StringUtils
														.split(virtualAccountBankNtt.getCicilan(), ",");
												jejakLangkah.add("11. PROSES TOKEN CICILAN: jumlah token="
														+ tokenCicilan.length + " -> " + virtualAccountBankNtt.getCicilan());
												for (String idPemBul : tokenCicilan) {
													if (Common.isNumber(idPemBul)) {
														PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
																.createCriteria(PengaturanPembayaranBulanan.class)
																.add(Restrictions.idEq(Long.parseLong(idPemBul)))
																.uniqueResult();

														if (pengaturanPembayaranBulanan != null) {
															String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul
																	+ "-" + virtualAccountBankNtt.getId();

															ItemBiaya itemBiaya = pengaturanPembayaranBulanan
																	.getDetailBiaya().getItemBiaya();

															Double subtotal = 0.0;
															try {
																String[] spl = idPemBul.split("-");
																subtotal = Double.parseDouble(spl[spl.length - 1]);
															} catch (Exception e) {
																ais.common.ErrorAuditUtil.record(e, "Mandiri H2H: gagal parse subtotal token cicilan '" + idPemBul + "'; VA=" + va);
															}

															if (itemBiaya.getPenghitungan()
																	.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
																subtotal = 0.0 - subtotal;
															}

															if (inquery) {
																JSONObject bill = new JSONObject();
																bill.put("billCode", itemBiaya.getKode());
																bill.put("billName", itemBiaya.getNama());
																bill.put("billShortName", itemBiaya.getKode());
																bill.put("billAmount", subtotal.intValue() + ".00");

																bill.put("reference1", "");
																bill.put("reference2", "");
																bill.put("reference3", "");

																BillDetail.put(bill);

															} else {

																CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																		.createCriteria(CicilanPembayaran.class)
																		.add(Restrictions.eq("ref", ref))
																		.setMaxResults(1).uniqueResult();

																if (cicilanPembayaran == null) {
																	cicilanPembayaran = new CicilanPembayaran(
																			pengaturanPembayaranBulanan
																					.getDetailBiaya());
																}
																cicilanPembayaran.setRef(ref);
																cicilanPembayaran.setValidator(bank);
																cicilanPembayaran.setKegiatan(kegiatan);
																cicilanPembayaran
																		.setItemBiaya(pengaturanPembayaranBulanan
																				.getDetailBiaya().getItemBiaya());
																cicilanPembayaran.setPengaturanPembayaranBulanan(
																		pengaturanPembayaranBulanan);
																cicilanPembayaran
																		.setRefVa(virtualAccountBankNtt.getId());
																cicilanPembayaran.setNilai(subtotal);
																cicilanPembayaran
																		.setNilaiAsli(cicilanPembayaran.getNilai());
																cicilanPembayaran.setTanggal(tanggal);
																cicilanPembayaran.setJenisPembayaran(bankHost == null
																		|| bankHost.getJenisPembayaran() == null
																				? ConstantValues.TUNAI
																				: bankHost.getJenisPembayaran());
																cicilanPembayaran.setDenda(0.0);
																cicilanPembayaran
																		.setNilaiAsli(cicilanPembayaran.getNilai());

																session.getTransaction().begin();
																if (cicilanPembayaran.getId() == null)
																	session.save(cicilanPembayaran);
																else
																	Common.refreshUpdate(session, cicilanPembayaran);
																session.getTransaction().commit();

																JSONObject jsonObjectRinci = new JSONObject();
																jsonObjectRinci.put("nama", pengaturanPembayaranBulanan
																		.getDetailBiaya().getItemBiaya().getNama());
																jsonObjectRinci.put("bulan",
																		pengaturanPembayaranBulanan.getNamaBulan());
																jsonObjectRinci.put("nominal",
																		pengaturanPembayaranBulanan
																				.ambilNominalModifikasi(mahasiswa,
																						semester));

																rincian.put(jsonObjectRinci);
															}

														}
													} else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
														PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
																.createCriteria(PengaturanPembayaranBulanan.class)
																.add(Restrictions
																		.idEq(Long.parseLong(idPemBul.split("-")[1])))
																.uniqueResult();

														if (pengaturanPembayaranBulanan != null) {
															String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul
																	+ "-" + virtualAccountBankNtt.getId();

															ItemBiaya itemBiaya = pengaturanPembayaranBulanan
																	.getDetailBiaya().getItemBiaya();
															Double subtotal = 0.0;
															try {
																String[] spl = idPemBul.split("-");
																subtotal = Double.parseDouble(spl[spl.length - 1]);
															} catch (Exception e) {
																ais.common.ErrorAuditUtil.record(e, "Mandiri H2H: gagal parse subtotal token cicilan '" + idPemBul + "'; VA=" + va);
															}

															if (itemBiaya.getPenghitungan()
																	.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
																subtotal = 0.0 - subtotal;
															}

															if (inquery) {
																JSONObject bill = new JSONObject();
																bill.put("billCode", itemBiaya.getKode());
																bill.put("billName", itemBiaya.getNama());
																bill.put("billShortName", itemBiaya.getKode());
																bill.put("billAmount", subtotal.intValue() + ".00");

																bill.put("reference1", "");
																bill.put("reference2", "");
																bill.put("reference3", "");

																BillDetail.put(bill);

															} else {
																CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																		.createCriteria(CicilanPembayaran.class)
																		.add(Restrictions.eq("ref", ref))
																		.setMaxResults(1).uniqueResult();

																if (cicilanPembayaran == null) {
																	cicilanPembayaran = new CicilanPembayaran(
																			pengaturanPembayaranBulanan
																					.getDetailBiaya());
																}
																cicilanPembayaran.setRef(ref);
																cicilanPembayaran.setValidator(bank);
																cicilanPembayaran.setKegiatan(kegiatan);
																cicilanPembayaran.setItemBiaya(itemBiaya);
																cicilanPembayaran.setPengaturanPembayaranBulanan(
																		pengaturanPembayaranBulanan);
																cicilanPembayaran
																		.setRefVa(virtualAccountBankNtt.getId());

																cicilanPembayaran.setNilai(subtotal);
																cicilanPembayaran
																		.setNilaiAsli(cicilanPembayaran.getNilai());
																cicilanPembayaran.setTanggal(tanggal);
																cicilanPembayaran.setJenisPembayaran(bankHost == null
																		|| bankHost.getJenisPembayaran() == null
																				? ConstantValues.TUNAI
																				: bankHost.getJenisPembayaran());
																cicilanPembayaran.setDenda(0.0);
																cicilanPembayaran
																		.setNilaiAsli(cicilanPembayaran.getNilai());
																session.getTransaction().begin();
																if (cicilanPembayaran.getId() == null)
																	session.save(cicilanPembayaran);
																else
																	Common.refreshUpdate(session, cicilanPembayaran);
																session.getTransaction().commit();

																JSONObject jsonObjectRinci = new JSONObject();
																jsonObjectRinci.put("nama", pengaturanPembayaranBulanan
																		.getDetailBiaya().getItemBiaya().getNama());
																jsonObjectRinci.put("bulan",
																		pengaturanPembayaranBulanan.getNamaBulan());
																jsonObjectRinci.put("nominal", subtotal);

																rincian.put(jsonObjectRinci);
															}

														}
													} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
														ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
																.simpleObject(
																		session.createCriteria(ItemBiaya.class)
																				.add(Restrictions.idEq(Long.parseLong(
																						idPemBul.split("-")[1]))),
																		ItemBiaya.class);

														if (itemBiaya != null) {
															String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul
																	+ "-" + virtualAccountBankNtt.getId();
															Double subtotal = 0.0;
															try {
																String[] spl = idPemBul.split("-");
																subtotal = Double.parseDouble(spl[2]);
															} catch (Exception e) {
																ais.common.ErrorAuditUtil.record(e, "Mandiri H2H: gagal parse subtotal token cicilan '" + idPemBul + "'; VA=" + va);
															}

															Long detailBiayaId = null;
															try {
																String[] spl = idPemBul.split("-");
																detailBiayaId = Long.parseLong(spl[4]);
																System.out.println("detailBiayaId -> " + detailBiayaId);
															} catch (Exception e) {
																ais.common.ErrorAuditUtil.record(e, "Mandiri H2H: gagal parse detailBiayaId dari token cicilan '" + idPemBul + "'; VA=" + va);
															}

															if (itemBiaya.getPenghitungan()
																	.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
																subtotal = 0.0 - subtotal;
															}

															if (inquery) {
																JSONObject bill = new JSONObject();
																bill.put("billCode", itemBiaya.getKode());
																bill.put("billName", itemBiaya.getNama());
																bill.put("billShortName", itemBiaya.getKode());
																bill.put("billAmount", subtotal.intValue() + ".00");

																bill.put("reference1", "");
																bill.put("reference2", "");
																bill.put("reference3", "");

																BillDetail.put(bill);

															} else {

																CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																		.createCriteria(CicilanPembayaran.class)
																		.add(Restrictions.eq("ref", ref))
																		.setMaxResults(1).uniqueResult();

																System.out.println("ref -> " + ref
																		+ " cicilanPembayaran " + cicilanPembayaran);

																if (cicilanPembayaran == null) {
																	cicilanPembayaran = new CicilanPembayaran(
																			DetailBiaya.muatRefAman(session, detailBiayaId));
																}
																cicilanPembayaran
																		.setDetailBiaya(DetailBiaya.muatRefAman(session, detailBiayaId));
																cicilanPembayaran.setRef(ref);
																cicilanPembayaran.setValidator(bank);
																cicilanPembayaran.setKegiatan(kegiatan);
																cicilanPembayaran.setItemBiaya(itemBiaya);
																cicilanPembayaran.setPengaturanPembayaranBulanan(null);
																cicilanPembayaran
																		.setRefVa(virtualAccountBankNtt.getId());

																cicilanPembayaran.setNilai(subtotal);
																cicilanPembayaran
																		.setNilaiAsli(cicilanPembayaran.getNilai());
																cicilanPembayaran.setTanggal(tanggal);
																cicilanPembayaran.setJenisPembayaran(bankHost == null
																		|| bankHost.getJenisPembayaran() == null
																				? ConstantValues.TUNAI
																				: bankHost.getJenisPembayaran());
																cicilanPembayaran.setDenda(0.0);
																cicilanPembayaran
																		.setNilaiAsli(cicilanPembayaran.getNilai());
																session.getTransaction().begin();
																if (cicilanPembayaran.getId() == null)
																	session.save(cicilanPembayaran);
																else
																	Common.refreshUpdate(session, cicilanPembayaran);
																session.getTransaction().commit();

																JSONObject jsonObjectRinci = new JSONObject();
																jsonObjectRinci.put("nama", itemBiaya.getNama());
																jsonObjectRinci.put("bulan", "");
																jsonObjectRinci.put("nominal", subtotal);

																rincian.put(jsonObjectRinci);
															}

														}
													} else if (idPemBul != null && idPemBul.startsWith("Keranjang-")) {
														// Pembayaran Keranjang Belanja (multi jenis / KegiatanTemporary): konversi draf
														// menjadi Kegiatan+Cicilan nyata â€” pemroses terpusat yang sama dengan Esmartlink.
														jejakLangkah.add("   token '" + idPemBul + "': jalur Keranjang Belanja (prosesSatuTokenKeranjang).");
														ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
																virtualAccountBankNtt, inquery, bank, bankHost, tanggal, data, null);
													} else {
														jejakLangkah.add("   token '" + idPemBul + "': format token TIDAK DIKENALI (tidak masuk kategori manapun â€” numerik/Bulanan-/Item-/Keranjang-).");
													}
												}
											}

											jejakLangkah.add("12. STATUS AKHIR: " + (inquery ? "INQUIRY" : "PAYMENT") + " diproses, menuju status sukses.");
											if (inquery) {
												status.put("isError", "false");
												status.put("errorCode", "00");
												status.put("statusDescription", "Inquiry success");
											} else {
												Double[] d = PembayaranUtil.getInstance()
														.getTotalDanDendaFromCicilan(session, kegiatan);
												Double jumlah = d[0];
												Double denda = d[1];
												kegiatan.setDenda(denda.doubleValue());
												kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars
														- (jumlah.doubleValue() - denda.doubleValue()));

												kegiatan.setAmount(jumlah.doubleValue() > 0.1 ? jumlah.doubleValue()
														: virtualAccountBankNtt.getTotal());
												kegiatan.setValidator(bank);

												session.getTransaction().begin();
												Common.refreshUpdate(session, kegiatan);
												session.getTransaction().commit();

												VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan,
														data, bank);

												status.put("isError", "false");
												status.put("errorCode", "00");
												status.put("statusDescription", "Payment success");
											}

										}
									}

								} finally {
									if (session != null) {
										// FIX "TransactionException: Transaction not successfully started" /
										// "current transaction is aborted": bila salah satu begin()+commit()
										// di atas gagal (mis. koneksi c3p0 rusak di tengah transaksi), transaksi
										// bisa tertinggal aktif TANPA di-rollback -- HibernateUtil.rollbackTransaction()
										// di blok catch luar HANYA menyentuh session ThreadLocal (MAP.get()),
										// BUKAN `session` lokal H2H ini. Rollback eksplisit di sini SEBELUM
										// clear/disconnect/close supaya koneksi dikembalikan ke pool c3p0 dalam
										// keadaan bersih (bukan "idle in transaction"/aborted).
										try {
											if (session.isOpen()) {
												Transaction txSisa = session.getTransaction();
												if (txSisa != null && txSisa.isActive()) {
													txSisa.rollback();
												}
											}
										} catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "Mandiri H2H: gagal rollback sisa transaksi sebelum tutup session"); }
										try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "Mandiri H2H: gagal clear session"); }
										try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "Mandiri H2H: gagal disconnect session"); }
										try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "Mandiri H2H: gagal close session"); }
									}
								}

							}
						}
					}
				}
			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();
				status.put("isError", "true");
				status.put("errorCode", "87");
				status.put("statusDescription", "Provider Database Problem");
				h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
				jejakLangkah.add("!! EXCEPTION TERTANGKAP: " + e.getClass().getName() + " - " + e.getMessage());
				Common.tampilErrorJikaAdmin(e);
				// Catat juga ke tabel ErrorLog (menu Error Log) agar penyebab errorCode=87
				// mudah diaudit tanpa harus membuka log H2H/console server.
				try {
					ais.common.ErrorAuditUtil.record(e, "Mandiri H2H doProcess gagal (respons 87); va=" + va
							+ ", nominal=" + nominalP + ", inquiry=" + inquery + ", bank=" + bank, request);
				} catch (Exception exAudit) { ais.common.ErrorAuditUtil.record(exAudit, "auto-audit(empty-catch) src/ais/action/servlet/Mandiri.java:797"); /* jangan ganggu respons ke bank */ }
			} finally {
				// DIAGNOSA 87 SENYAP: errorCode masih "87" TANPA exception berarti alur tidak
				// pernah mencapai cabang sukses/kode spesifik (mis. VA tidak ditemukan oleh
				// kriteria, tipe token cicilan tak tertangani, atau nominal tak cocok).
				// Catat jejaknya ke ErrorLog supaya kasus seperti ini tidak buntu diagnosa.
				try {
					if (h2hStackTrace == null && status != null && "87".equals(status.optString("errorCode"))) {
						ais.common.ErrorAuditUtil.record(new Exception(
								"Mandiri H2H berakhir errorCode=87 (Provider Database Problem) TANPA exception"
										+ " â€” alur tidak mencapai cabang sukses. va=" + va + ", nominal=" + nominalP
										+ ", inquiry=" + inquery + ", vaDitemukan=" + (virtualAccountBankNtt != null)
										+ ", data=" + data),
								"Mandiri H2H silent-87", request);
					}
				} catch (Exception exAudit) { ais.common.ErrorAuditUtil.record(exAudit, "auto-audit(empty-catch) src/ais/action/servlet/Mandiri.java:812"); /* jangan ganggu respons ke bank */ }

				// PERMINTAAN USER: tampilkan SEMUA â€” mulai request sampai response â€” bila
				// terdapat error, plus jejak detail tiap langkah alur. Dieksekusi SELALU
				// (bukan hanya saat chek==true) karena tujuannya justru menangkap kegagalan
				// pada transaksi H2H SUNGGUHAN yang masuk dari bank, bukan hanya saat admin
				// mengecek ulang manual. Transaksi SUKSES sengaja tidak di-dump penuh agar
				// konsol/DB tidak dibanjiri (lihat komentar deklarasi jejakLangkah di atas).
				try {
					boolean adaError = status != null && "true".equalsIgnoreCase(status.optString("isError"));
					if (adaError) {
						StringBuilder dump = new StringBuilder();
						dump.append("\n========== MANDIRI H2H GAGAL â€” JEJAK LENGKAP REQUEST s.d. RESPONSE ==========\n");
						dump.append("Waktu   : ").append(new Date()).append("\n");
						dump.append("Bank    : ").append(bank).append("\n");
						dump.append("VA      : ").append(va).append("\n");
						dump.append("Nominal : ").append(nominalP).append("\n");
						dump.append("Inquiry : ").append(inquery).append("\n");
						dump.append("---- Langkah demi langkah ----\n");
						for (String langkah : jejakLangkah) {
							dump.append(langkah).append("\n");
						}
						dump.append("---- Status akhir ----\n");
						dump.append("errorCode        : ").append(status.optString("errorCode")).append("\n");
						dump.append("statusDescription: ").append(status.optString("statusDescription")).append("\n");
						if (h2hStackTrace != null) {
							dump.append("---- Stack trace exception ----\n").append(h2hStackTrace).append("\n");
						}
						dump.append("---- Response body dikirim ke bank ----\n")
								.append(jsonObjectResponse == null ? "(null)" : jsonObjectResponse.toString())
								.append("\n");
						dump.append("================================================================================\n");

						String dumpStr = dump.toString();
						// 1) SELALU cetak penuh ke konsol Tomcat â€” tak dibatasi chek==true, karena
						// ini KHUSUS kasus error (permintaan eksplisit: "tampilkan semua jika error").
						System.out.println(dumpStr);
						// 2) Simpan jejak lengkap sebagai stackTrace pada Log Host-to-Host (menu
						// admin), menggantikan h2hStackTrace polos agar langkah-langkahnya ikut
						// terlihat tanpa perlu membuka console server.
						h2hStackTrace = dumpStr;
					}
				} catch (Exception exDump) {
					ais.common.ErrorAuditUtil.record(exDump, "Mandiri H2H: gagal menyusun/menampilkan jejak lengkap error");
				}

				// LOG DETAIL KONSOL TOMCAT â€” HANYA untuk CEK ULANG manual dari menu Log
				// Host-to-Host (chek==true, request==null/dipicu admin), BUKAN dari
				// request pembayaran asli via URL bank (chek==false) â€” supaya konsol
				// tidak dibanjiri log tiap kali ada transaksi H2H sungguhan masuk.
				if (chek) {
					try {
						logDetailCekUlang(va, nominalP, tanggalP, bank, bankHost, inquery, data,
								virtualAccountBankNtt, status, h2hStackTrace, jsonObjectResponse, nim, nama,
								jejakLangkah);
					} catch (Exception exLog) { ais.common.ErrorAuditUtil.record(exLog, "auto-audit(empty-catch) src/ais/action/servlet/Mandiri.java:logDetailCekUlang"); /* jangan ganggu respons ke bank */ }
				}

				// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
				// (helper: session terdedikasi + commit + retry, tak pernah gagal).
				ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
						jsonObjectResponse.toString(), nominalP, rincian.toString(), h2hStackTrace);
			}
		}
		return jsonObjectResponse;
	}

	/**
	 * Cetak log SELENGKAP-LENGKAPNYA ke konsol Tomcat (System.out) khusus untuk
	 * CEK ULANG pembayaran Mandiri H2H (dipicu manual oleh admin lewat menu Log
	 * Host-to-Host, {@code chek==true}). Method ini SENGAJA tidak dipanggil dari
	 * jalur request pembayaran asli (webhook bank via URL, {@code chek==false})
	 * supaya log konsol tidak penuh oleh transaksi H2H sungguhan yang volumenya
	 * jauh lebih tinggi daripada cek ulang manual.
	 *
	 * @param jejakLangkah jejak langkah-demi-langkah alur {@code doProcess} (boleh
	 *                     null/kosong) â€” dicetak setelahnya agar cek ulang manual
	 *                     selalu punya rincian tiap tahap, bukan hanya ringkasan.
	 */
	@SuppressWarnings("unchecked")
	private static void logDetailCekUlang(String va, double nominalP, String tanggalP, String bank,
			BankHost bankHost, boolean inquery, String data, VirtualAccountBank virtualAccountBankNtt,
			JSONObject status, String h2hStackTrace, JSONObject jsonObjectResponse, String nim, String nama,
			List<String> jejakLangkah) {

		StringBuilder log = new StringBuilder();
		log.append("\n================ MANDIRI H2H CEK ULANG (manual, menu Log Host-to-Host) ================\n");
		log.append("Waktu proses     : ").append(new Date()).append("\n");
		log.append("Bank             : ").append(bank).append("\n");
		log.append("BankHost id      : ").append(bankHost == null ? "(null)" : String.valueOf(bankHost.getId()))
				.append("\n");
		log.append("Jenis request    : ").append(inquery ? "Inquiry" : "Payment").append("\n");
		log.append("VA               : ").append(va).append("\n");
		log.append("Nominal          : ").append(nominalP).append("\n");
		log.append("Tanggal transaksi: ").append(tanggalP).append("\n");
		log.append("Payload data     : ").append(data).append("\n");
		log.append("---- Hasil pencarian VA (VirtualAccountBank.ambilVa) ----\n");
		if (virtualAccountBankNtt == null) {
			log.append("vaDitemukan      : false (tidak ada baris virtual_account_bank yg cocok, & fallback ambilByNisAja juga gagal)\n");
		} else {
			log.append("vaDitemukan      : true\n");
			log.append("  id             : ").append(virtualAccountBankNtt.getId()).append("\n");
			log.append("  kode           : ").append(virtualAccountBankNtt.getKode()).append("\n");
			try {
				log.append("  total          : ").append(virtualAccountBankNtt.getTotal()).append("\n");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Mandiri.java:logDetailCekUlang.total"); }
			try {
				log.append("  biayaAdmin     : ").append(virtualAccountBankNtt.getBiayaAdmin()).append("\n");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Mandiri.java:logDetailCekUlang.biayaAdmin"); }
			try {
				log.append("  kadaluarsa     : ").append(virtualAccountBankNtt.getKadaluarsa()).append("\n");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Mandiri.java:logDetailCekUlang.kadaluarsa"); }
			try {
				log.append("  sudahTerbayar  : ").append(VirtualAccountBank.isSudahTerbayar(virtualAccountBankNtt))
						.append("\n");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Mandiri.java:logDetailCekUlang.sudahTerbayar"); }
			try {
				log.append("  waktuBayar     : ").append(virtualAccountBankNtt.getWaktuBayar()).append("\n");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Mandiri.java:logDetailCekUlang.waktuBayar"); }
			try {
				log.append("  siswa          : ")
						.append(virtualAccountBankNtt.getSiswa() == null ? "(null)"
								: virtualAccountBankNtt.getSiswa().getNomorInduk() + " - "
										+ virtualAccountBankNtt.getSiswa().getNama())
						.append("\n");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Mandiri.java:logDetailCekUlang.siswa"); }
			try {
				log.append("  calonSiswa     : ").append(
						virtualAccountBankNtt.getCalonSiswa() == null ? "(null)"
								: virtualAccountBankNtt.getCalonSiswa().getNomorInduk() + " - "
										+ virtualAccountBankNtt.getCalonSiswa().getNama())
						.append("\n");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Mandiri.java:logDetailCekUlang.calonSiswa"); }
			try {
				log.append("  kegiatan       : ").append(virtualAccountBankNtt.getKegiatan()).append("\n");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Mandiri.java:logDetailCekUlang.kegiatan"); }
			try {
				log.append("  pembayaran     : ").append(virtualAccountBankNtt.getPembayaran()).append("\n");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Mandiri.java:logDetailCekUlang.pembayaran"); }
		}
		log.append("---- Hasil identifikasi nama/nim ----\n");
		log.append("nim              : ").append(nim).append("\n");
		log.append("nama             : ").append(nama).append("\n");
		log.append("---- Jejak langkah-demi-langkah ----\n");
		if (jejakLangkah == null || jejakLangkah.isEmpty()) {
			log.append("(tidak ada jejak â€” kemungkinan gagal sebelum jejak sempat tercatat)\n");
		} else {
			for (String langkah : jejakLangkah) {
				log.append(langkah).append("\n");
			}
		}
		log.append("---- Status respons akhir ----\n");
		log.append("isError          : ").append(status == null ? "(null)" : status.optString("isError")).append("\n");
		log.append("errorCode        : ").append(status == null ? "(null)" : status.optString("errorCode")).append("\n");
		log.append("statusDescription: ").append(status == null ? "(null)" : status.optString("statusDescription"))
				.append("\n");
		log.append("h2hStackTrace    : ").append(h2hStackTrace == null ? "(tidak ada exception nyata)" : h2hStackTrace)
				.append("\n");
		log.append("Response body    : ").append(jsonObjectResponse == null ? "(null)" : jsonObjectResponse.toString())
				.append("\n");
		log.append("========================================================================================\n");

		System.out.println(log.toString());
	}

	/**
	 * Mengurai payload JSON mentah dari Bank Mandiri menjadi parameter terpisah,
	 * memanggil {@link #doProcess}, lalu mengembalikan badan respons siap tulis.
	 *
	 * <h3>Pemetaan field</h3>
	 * <ul>
	 * <li>Amplop {@code InquiryRequest} atau {@code paymentRequest} menentukan mode;
	 * bila keduanya ada, {@code paymentRequest} menang untuk pembacaan
	 * VA/nominal/tanggal, tetapi penentuan mode respons memakai
	 * {@code InquiryRequest != null}.</li>
	 * <li>Nomor VA dari {@code billKey1}, dengan cadangan parameter query bernama
	 * sama bila field JSON kosong.</li>
	 * <li>Nominal dari {@code billKey2} (inquiry) atau {@code paymentAmount}
	 * (payment). Keduanya dilindungi: nilai null/kosong/blank tidak lagi melempar
	 * {@code NumberFormatException} melainkan jatuh ke {@code 0.0}.</li>
	 * <li>Waktu transaksi dari {@code trxDateTime}, cadangan jam server.</li>
	 * </ul>
	 *
	 * <h3>Penanganan kegagalan</h3>
	 * Exception dari {@link #doProcess} ditangkap, dicatat ke Error Log, dan diganti
	 * respons {@link #errorDb(boolean)} ({@code errorCode 87}) supaya bank selalu
	 * menerima JSON yang sah. Bila konfigurasi {@code mandiri_va_sleep} aktif,
	 * method sengaja tidur 3 detik lalu mengembalikan {@link #timeoutDb(boolean)}
	 * ({@code errorCode 89}) &mdash; sakelar uji untuk mensimulasikan timeout dari
	 * sisi bank.
	 *
	 * @param data     payload JSON mentah; boleh {@code null} (seluruh field jatuh
	 *                 ke nilai cadangan)
	 * @param request  request HTTP asli; boleh {@code null} pada cek ulang manual
	 * @param bankHost baris {@link BankHost} hasil pencocokan IP; boleh {@code null}
	 * @param bank     nama bank pembayar, biasanya {@code "Mandiri"}
	 * @param chek     {@code true} bila dipicu admin dari menu Log Host-to-Host
	 * @return badan respons JSON sebagai {@link String}, tidak pernah {@code null}
	 * @throws Exception bila penguraian JSON tingkat akar gagal atau tidur
	 *                   simulasi timeout diinterupsi
	 */
	public static String doProses(String data, HttpServletRequest request, BankHost bankHost, String bank, boolean chek)
			throws Exception {
		JSONObject req = data == null ? null : new JSONObject(data);

		JSONObject InquiryRequest = req == null || req.isNull("InquiryRequest") ? null
				: req.getJSONObject("InquiryRequest");

		JSONObject paymentRequest = req == null || req.isNull("paymentRequest") ? null
				: req.getJSONObject("paymentRequest");

		String va = InquiryRequest == null || InquiryRequest.isNull("billKey1")
				? (request == null ? "" : request.getParameter("billKey1"))
				: InquiryRequest.getString("billKey1");

		if (paymentRequest != null) {
			va = paymentRequest == null || paymentRequest.isNull("billKey1")
					? (request == null ? "" : request.getParameter("billKey1"))
					: paymentRequest.getString("billKey1");
		}

		double nominalP = 0.0;
		try {
			// Root cause fix: validasi dulu string-nya (null/kosong/blank) SEBELUM
			// panggil Double.parseDouble -- payload H2H dari bank kadang mengirim
			// billKey2 sbg string kosong "", yg selama ini langsung melempar
			// NumberFormatException("empty String"). Kosong/blank dianggap 0.0
			// (default aman utk inquiry) tanpa perlu melempar exception sama sekali.
			String billKey2 = InquiryRequest == null || InquiryRequest.isNull("billKey2") ? null
					: InquiryRequest.getString("billKey2");
			if (billKey2 == null || billKey2.trim().length() == 0) {
				if (InquiryRequest != null) {
					System.out.println("Mandiri.doProses: billKey2 kosong/null dari gateway, pakai default 0.0");
				}
				nominalP = 0.0;
			} else {
				nominalP = Double.parseDouble(billKey2.trim());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "Mandiri.doProses: gagal parse billKey2 dari InquiryRequest, fallback 0.0");
			nominalP = 0.0;
		}

		if (paymentRequest != null) {
			try {
				// FIX crash NYATA (bukan sekadar noise): berbeda dgn cabang
				// InquiryRequest/billKey2 di atas yg SUDAH dilindungi try/catch,
				// cabang paymentRequest/paymentAmount ini SEBELUMNYA tanpa proteksi
				// -- payload paymentAmount kosong/rusak dari bank akan melempar
				// NumberFormatException TAK TERTANGKAP keluar dari doProses(),
				// gagal total merespons bank alih-alih fallback errorDb() yg ada
				// di try/catch method ini.
				//
				// Root cause fix: validasi null/kosong/blank SEBELUM Double.parseDouble
				// -- payload paymentAmount kosong "" dari bank tidak lagi melempar
				// NumberFormatException("empty String"), langsung fallback 0.0.
				String paymentAmount = paymentRequest.isNull("paymentAmount") ? null
						: paymentRequest.getString("paymentAmount");
				if (paymentAmount == null || paymentAmount.trim().length() == 0) {
					System.out.println("Mandiri.doProses: paymentAmount kosong/null dari gateway, pakai default 0.0");
					nominalP = 0.0;
				} else {
					nominalP = Double.parseDouble(paymentAmount.trim());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "Mandiri.doProses: gagal parse paymentAmount dari paymentRequest, fallback 0.0");
				nominalP = 0.0;
			}
		}

		String tanggalP = InquiryRequest == null || InquiryRequest.isNull("trxDateTime") ? dateFormat.get().format(new Date())
				: InquiryRequest.getString("trxDateTime");

		if (paymentRequest != null) {
			tanggalP = paymentRequest == null || paymentRequest.isNull("trxDateTime") ? dateFormat.get().format(new Date())
					: paymentRequest.getString("trxDateTime");
		}
		String body;
		try {
			// 05, request tidak diizinkan
			body = Mandiri.doProcess(nominalP, tanggalP, va, bank, bankHost, request, data, true,
					InquiryRequest != null, chek).toString();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Mandiri.java:869");
			// Catat ke tabel ErrorLog (menu Error Log) â€” penyebab respons errorCode=87
			// "Provider Database Problem" selama ini hanya tercetak di console sehingga
			// sulit diaudit. Kegagalan pencatatan tidak boleh mengganggu respons ke bank.
			try {
				ais.common.ErrorAuditUtil.record(e, "Mandiri H2H doProses gagal (respons 87); va=" + va
						+ ", nominal=" + nominalP + ", inquiry=" + (InquiryRequest != null) + ", data=" + data,
						request);
			} catch (Exception exAudit) { ais.common.ErrorAuditUtil.record(exAudit, "auto-audit(empty-catch) src/ais/action/servlet/Mandiri.java:877"); /* jangan ganggu respons ke bank */ }
			body = errorDb(InquiryRequest != null);

		}

		if (Common.bolehKonfigurasi("mandiri_va_sleep", Konfigurasi.TIDAK_AKTIF)) {
			Thread.sleep(3 * 1000);
			body = timeoutDb(InquiryRequest != null);
		}

		System.out.println("response->" + body);

		return body;
	}

	/**
	 * Titik masuk bersama {@link #doGet} dan {@link #doPost}: membaca badan request
	 * sebagai teks, mengenali pemanggil, memanggil {@link #doProses}, lalu menulis
	 * hasilnya ke respons sebagai {@code application/json}.
	 *
	 * <h3>Pengenalan pemanggil &mdash; catatan penting</h3>
	 * Pemanggil dikenali <b>hanya</b> lewat
	 * {@link PembayaranUtil#getBankHost(String, String)} dengan alamat
	 * {@link HttpServletRequest#getRemoteAddr()}. Beberapa hal yang perlu disadari
	 * pembaca kode:
	 * <ul>
	 * <li><b>Tidak ada verifikasi kriptografis.</b> Berbeda dengan
	 * {@link Bniresponse} yang mendekode payload memakai kunci bersama lewat
	 * {@code BNIHash.parseData(...)}, di sini tidak ada tanda tangan, MAC, maupun
	 * token yang diperiksa &mdash; baik pada cabang inquiry maupun pada cabang
	 * payment yang benar-benar membukukan uang.</li>
	 * <li><b>Hasil pencocokan IP tidak menjadi gerbang.</b> {@code bankHost} yang
	 * bernilai {@code null} tetap diteruskan ke {@link #doProses}; alur pembayaran
	 * berjalan penuh dan hanya jenis pembayarannya yang jatuh ke nilai bawaan.</li>
	 * <li>Varian {@link PembayaranUtil#getBankHost(String, String)} yang dipakai di
	 * sini sengaja memakai alamat soket langsung, <b>bukan</b> header
	 * {@code X-Forwarded-For}/{@code CF-Connecting-IP} seperti varian
	 * {@code getBankHost(HttpServletRequest)}, sehingga IP tidak dapat dipalsukan
	 * lewat header oleh pemanggil.</li>
	 * <li>Di sisi {@link PembayaranUtil} sendiri masih ada dua pelonggaran yang
	 * berlaku umum: pembuatan otomatis baris {@link BankHost} bila konfigurasi
	 * {@code apabila_bank_host_tidak_ditemukan_buat_data_bank_otomatis} aktif, dan
	 * cadangan ke baris ber-IP {@code 0.0.0.0} yang cocok untuk alamat mana pun.</li>
	 * </ul>
	 * Dengan kata lain, perlindungan endpoint ini bergantung pada pembatasan
	 * jaringan di depan aplikasi, bukan pada pemeriksaan di dalam kode.
	 *
	 * @param request  request HTTP dari gateway bank
	 * @param response respons yang akan diisi badan JSON beserta header
	 *                 {@code length} dan {@code Content-Type}
	 * @throws Exception bila pembacaan badan request atau penulisan respons gagal
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Read from request

		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String data = buffer.toString();

		String querystring = request.getQueryString();
		System.out.println("==> VA data => " + data);
		System.out.println("==> VA querystring => " + querystring);

		String bank = "Mandiri";
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

		String body = doProses(data, request, bankHost, bank, false);

		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();

		writer.write(body);

	}

	/**
	 * Menyusun badan respons baku bertanda <b>timeout</b> ({@code errorCode 89}),
	 * dipakai hanya oleh sakelar uji {@code mandiri_va_sleep} pada
	 * {@link #doProses} untuk mensimulasikan server lambat dari sisi bank.
	 *
	 * @param inquery {@code true} membungkus respons sebagai
	 *                {@code InquiryResponse} beserta kerangka
	 *                {@code billDetails}/{@code currency}; {@code false} sebagai
	 *                {@code paymentResponse}
	 * @return badan JSON siap kirim berisi status {@code 89 Timeout}
	 * @throws Exception bila penyusunan objek JSON gagal
	 */
	private static String timeoutDb(boolean inquery) throws Exception {
		JSONObject response = new JSONObject();

		JSONObject jsonObjectResponse = new JSONObject();
		if (inquery) {
			jsonObjectResponse.put("InquiryResponse", response);
		} else {
			jsonObjectResponse.put("paymentResponse", response);
		}
		response.put("billInfo1", "");
		response.put("billInfo2", "");
		response.put("billInfo3", "");
		JSONArray BillDetail = new JSONArray();

		if (inquery) {
			JSONObject billDetails = new JSONObject();
			response.put("billDetails", billDetails);
			billDetails.put("BillDetail", BillDetail);
			response.put("currency", "360");
		}

		JSONObject status = new JSONObject();
		response.put("status", status);

		status.put("isError", "true");
		status.put("errorCode", "89");
		status.put("statusDescription", "Timeout");
		return jsonObjectResponse.toString();
	}

	/**
	 * Menyusun badan respons baku bertanda <b>gagal database</b>
	 * ({@code errorCode 87 Provider Database Problem}), dipakai {@link #doProses}
	 * sebagai jaring pengaman ketika {@link #doProcess} melempar exception.
	 *
	 * <p>
	 * Kode 87 yang sama juga dapat muncul tanpa exception apa pun &mdash; yaitu
	 * ketika alur {@link #doProcess} tidak pernah mencapai cabang sukses; kasus
	 * "87 senyap" itu sengaja dicatat terpisah ke Error Log agar tetap bisa
	 * didiagnosis.
	 *
	 * @param inquery {@code true} membungkus respons sebagai
	 *                {@code InquiryResponse} beserta kerangka
	 *                {@code billDetails}/{@code currency}; {@code false} sebagai
	 *                {@code paymentResponse}
	 * @return badan JSON siap kirim berisi status {@code 87 Provider Database
	 *         Problem}
	 * @throws Exception bila penyusunan objek JSON gagal
	 */
	private static String errorDb(boolean inquery) throws Exception {
		JSONObject response = new JSONObject();

		JSONObject jsonObjectResponse = new JSONObject();
		if (inquery) {
			jsonObjectResponse.put("InquiryResponse", response);
		} else {
			jsonObjectResponse.put("paymentResponse", response);
		}
		response.put("billInfo1", "");
		response.put("billInfo2", "");
		response.put("billInfo3", "");
		JSONArray BillDetail = new JSONArray();

		if (inquery) {
			JSONObject billDetails = new JSONObject();
			response.put("billDetails", billDetails);
			billDetails.put("BillDetail", BillDetail);
			response.put("currency", "360");
		}

		JSONObject status = new JSONObject();
		response.put("status", status);

		status.put("isError", "true");
		status.put("errorCode", "87");
		status.put("statusDescription", "Provider Database Problem");
		return jsonObjectResponse.toString();
	}
}

