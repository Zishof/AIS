package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.hibernate.TransactionException;
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
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.Tagihan;

/**
 * Servlet gateway <i>host-to-host</i> (H2H) Bank Syariah Indonesia untuk skema Virtual Account:
 * menerima permintaan <b>inquiry</b> (tanya tagihan) dan <b>payment</b> (konfirmasi setoran) dari
 * sisi bank, lalu menjawab dengan JSON berisi identitas pembayar, total tagihan, dan rinciannya.
 *
 * <p>Terpasang di {@code web.xml} sebagai servlet {@code BSI} pada URL {@code /BSI}. Seluruh
 * pekerjaan dilakukan lewat {@link #doProcess} yang dipanggil melalui {@link #doProses}.</p>
 *
 * <h4>Pembagian tanggung jawab dengan {@link Bsiresponse}</h4>
 * <p>Kelas ini dan {@link Bsiresponse} adalah <b>dua integrasi BSI yang berbeda</b>, bukan pasangan
 * request/response dari satu alur:</p>
 * <ul>
 *   <li><b>{@code BSI} (kelas ini)</b> — kanal <i>Virtual Account tagihan</i>. Bank menghubungi
 *       eCampus. Payload JSON <b>polos</b> ({@code nomorPembayaran}, {@code totalNominal},
 *       {@code kodeBiller}, {@code action}). Sumber data tagihan adalah tabel
 *       {@link VirtualAccountBank}. Jawaban memakai kode {@code rc} berupa teks
 *       ({@code OK}, {@code ERR-NOT-FOUND}, {@code ERR-ALREADY-PAID},
 *       {@code ERR-PAYMENT-WRONG-AMOUNT}, {@code ERR-DB}).</li>
 *   <li><b>{@link Bsiresponse}</b> — kanal <i>callback pembayaran</i> gaya BNI eCollection,
 *       dipasang di URL {@code /BsiResponse}. Payload <b>terenkripsi</b> dan dibuka dengan
 *       {@code BNIHash.parseData(data, client_id, password)}. Sumber datanya tabel
 *       {@code BsiRequest}/{@code BsiResponse}, bukan {@code VirtualAccountBank}. Jawabannya
 *       {@code {"status":"000"}}.</li>
 * </ul>
 * <p>Keduanya sama sekali tidak saling memanggil dan tidak berbagi tabel; satu-satunya kesamaan
 * adalah nama bank dan pemakaian {@link PembayaranUtil#getBankHost(String, String)}.</p>
 *
 * <h4>Model autentikasi (PENTING — tidak ada tanda tangan digital)</h4>
 * <p>Berbeda dari {@link Bsiresponse} yang membuka payload dengan kunci rahasia bersama, kelas ini
 * <b>tidak memverifikasi tanda tangan, HMAC, token, maupun kata sandi apa pun</b>. Satu-satunya
 * pengenal pemanggil adalah pencocokan alamat IP lewat
 * {@link PembayaranUtil#getBankHost(String, String)} di {@link #process}, dan hasil pencocokan itu
 * <b>tidak dipakai sebagai gerbang</b>: bila IP tidak dikenal, {@code bankHost} bernilai
 * {@code null} namun pemrosesan tetap berlanjut (lihat komentar di dalam {@link #doProcess}).
 * Karena {@link VirtualAccountBank#ambilVa(String, Double, BankHost)} mencocokkan baris dengan
 * syarat <i>{@code bankHost} kosong ATAU sama dengan pemanggil</i>, VA "netral" (yang kolom
 * {@code bankHost}-nya {@code null}) tetap ditemukan dan tetap dapat dilunasi walau permintaan
 * datang dari IP mana pun. Sifat ini didokumentasikan apa adanya di sini; penilaian dan
 * penanganannya berada di luar cakupan berkas ini.</p>
 * <p>Sisi baiknya, IP yang dibaca berasal dari {@link HttpServletRequest#getRemoteAddr()} —
 * alamat TCP sebenarnya — <b>bukan</b> dari header {@code X-Forwarded-For}/{@code X-Real-IP} yang
 * bisa dipalsukan klien, sehingga kanal ini tidak ikut terdampak peracunan header seperti pada
 * varian {@link PembayaranUtil#getBankHost(HttpServletRequest)}.</p>
 *
 * <h4>Pemisahan inquiry dan payment</h4>
 * <p>Parameter {@code action} bernilai {@code "inquiry"} membuat seluruh alur bersifat
 * <b>baca saja</b>: tidak ada {@code save}/{@code update} yang di-commit, sehingga pengecekan
 * tagihan oleh bank tidak pernah terlihat sebagai pembayaran. Nilai selain itu diperlakukan
 * sebagai pelunasan dan wajib lolos pencocokan nominal.</p>
 *
 * @see Bsiresponse
 * @see VirtualAccountBank
 * @see ais.action.ws.util.PembayaranGatewayHelper
 */
public class BSI extends HttpServlet {
	/** Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai untuk logika apa pun. */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton utilitas pembayaran; di kelas ini hanya dipakai untuk memetakan alamat IP
	 * pemanggil menjadi {@link BankHost} lewat
	 * {@link PembayaranUtil#getBankHost(String, String)}.
	 */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * Pemformat tanggal {@code yyyyMMddHHmmss} untuk field {@code tanggalTransaksi} yang dikirim
	 * bank.
	 *
	 * <p>Dibungkus {@link ThreadLocal} karena {@link SimpleDateFormat} <b>tidak aman untuk
	 * dipakai bersama antar-thread</b>, sedangkan servlet dilayani banyak thread sekaligus;
	 * satu instance bersama akan menghasilkan tanggal yang kacau atau
	 * {@code NumberFormatException} acak di bawah beban.</p>
	 */
	private static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
		/** Membuat pemformat baru untuk setiap thread yang pertama kali memakainya. */
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMddHHmmss");
		}
	};

	/**
	 * Konstruktor bawaan yang dipanggil kontainer servlet saat memuat kelas ini; tidak ada
	 * inisialisasi khusus karena seluruh keadaan bersifat statis.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public BSI() {
		super();
	}

	/**
	 * Commit transaksi HANYA jika transaksi tersebut masih benar-benar aktif.
	 *
	 * <p><b>Root cause yang dijaga di sini.</b> {@code Common.refreshSaveOrUpdate}/{@code refreshUpdate}
	 * bisa menelan exception (mis. pelanggaran constraint NOT NULL yang bukan "unique constraint") lalu
	 * diam-diam melakukan rollback transaksi tanpa melempar ulang exception-nya (lihat
	 * {@code CommonHibernateHelper}). Jika kode pemanggil di sini langsung memanggil
	 * {@code session.getTransaction().commit()} setelah itu, Hibernate melempar
	 * {@code TransactionException: Transaction not successfully started} yang membingungkan --
	 * gejala jauh dari akar masalah aslinya (pola sama seperti bug kodeunik null di Kegiatan/Mandiri).</p>
	 *
	 * <p>Dengan guard ini: bila transaksi ternyata sudah tidak aktif saat hendak commit, JANGAN commit
	 * (mencegah TransactionException generic), catat detail lokasi ke {@code ErrorAuditUtil}, lalu
	 * lempar ulang RuntimeException yang jelas supaya ditangkap oleh catch umum method pemanggil
	 * (yang sudah mengisi rc=ERR-DB & mencatat log H2H) -- bukan silently melanjutkan seolah sukses.</p>
	 *
	 * @param session sesi Hibernate yang transaksinya hendak di-commit; {@code null} diperlakukan
	 *                sama seperti transaksi tidak aktif
	 * @param lokasi  penanda baris/blok pemanggil, ikut ditulis ke pesan audit agar akar masalah
	 *                mudah dilacak
	 * @throws RuntimeException bila transaksi sudah tidak aktif saat hendak di-commit
	 */
	private static void commitTransaksiAman(Session session, String lokasi) {
		Transaction tx = session == null ? null : session.getTransaction();
		if (tx != null && tx.isActive()) {
			tx.commit();
		} else {
			String pesan = "Transaksi tidak aktif saat hendak commit di BSI.java:" + lokasi
					+ " -- kemungkinan sudah di-rollback akibat error sebelumnya (mis. pelanggaran constraint) yang tertelan sebelum commit.";
			ais.common.ErrorAuditUtil.record(new TransactionException(pesan),
					"auto-audit src/ais/action/servlet/BSI.java:" + lokasi);
			throw new RuntimeException(pesan);
		}
	}

	/**
	 * Menangani permintaan HTTP GET dengan meneruskannya apa adanya ke {@link #process}.
	 *
	 * <p>GET dan POST diperlakukan identik; bank boleh memakai keduanya karena parameter dapat
	 * dikirim sebagai query string maupun sebagai badan JSON (lihat {@link #doProses}).</p>
	 *
	 * <p>Setiap {@link Exception} ditelan di sini dan hanya diteruskan ke
	 * {@code Common.tampilErrorJikaAdmin}. Akibatnya kegagalan tak terduga <b>tidak</b> menjadi
	 * HTTP 500: bank menerima balasan kosong bertatus 200. Jalur kegagalan yang normal sudah
	 * ditangani lebih dulu di dalam {@link #process} yang menjawab dengan
	 * {@link #errorDB(String)}.</p>
	 *
	 * @param request  permintaan dari sisi bank
	 * @param response tujuan penulisan balasan JSON
	 * @throws ServletException diwariskan dari kontrak {@link HttpServlet}; tidak dilempar sendiri
	 * @throws IOException      bila penulisan balasan ke {@code response} gagal
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menangani permintaan HTTP POST — jalur yang sebenarnya dipakai bank — dengan meneruskannya
	 * ke {@link #process}; perilakunya sama persis dengan {@link #doGet}.
	 *
	 * @param request  permintaan dari sisi bank, badan JSON-nya dibaca di {@link #process}
	 * @param response tujuan penulisan balasan JSON
	 * @throws ServletException diwariskan dari kontrak {@link HttpServlet}; tidak dilempar sendiri
	 * @throws IOException      bila penulisan balasan ke {@code response} gagal
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Inti gateway: mencari VA, menyusun balasan JSON, dan — hanya bila <b>bukan</b> inquiry —
	 * mencatat pelunasan ke basis data.
	 *
	 * <h4>Urutan pemeriksaan</h4>
	 * <ol>
	 *   <li>{@link VirtualAccountBank#ambilVa(String, Double, BankHost)} mencari VA; bila tidak
	 *       ada, {@code rc=ERR-NOT-FOUND};</li>
	 *   <li>{@code VirtualAccountBank.isSudahTerbayarUntukPayment} mencegah pembayaran ganda;
	 *       bila sudah lunas, {@code rc=ERR-ALREADY-PAID};</li>
	 *   <li><b>pencocokan nominal</b> — pada mode pembayaran, {@code nominalP} harus sama persis
	 *       (dibandingkan sebagai {@code int}) dengan total tagihan; bila tidak,
	 *       {@code rc=ERR-PAYMENT-WRONG-AMOUNT}. Total yang dipakai bergantung pada konfigurasi
	 *       {@code biaya_admin_jangan_dimasukkan_saat_simpan_va}: bila aktif dipakai
	 *       {@code getTotal()} saja, bila tidak dipakai {@code totalBiaya()} yang sudah termasuk
	 *       biaya admin;</li>
	 *   <li>untuk VA berjenis kegiatan mahasiswa, {@code kodeBiller} yang dikirim bank juga
	 *       dicocokkan dengan {@code prefixKodePembayaran} milik jenis kegiatan — bila berbeda
	 *       VA dianggap tidak ditemukan.</li>
	 * </ol>
	 *
	 * <h4>Dua cabang pembayar</h4>
	 * <p>VA milik siswa/calon siswa diproses oleh
	 * {@link VirtualAccountBank#bayarSiswa}; VA milik mahasiswa/calon mahasiswa diproses di
	 * tempat, dengan membuat atau memperbarui {@link Kegiatan} beserta baris
	 * {@link CicilanPembayaran} untuk setiap komponen pada kolom {@code cicilan}. Komponen itu
	 * dikenali dari awalannya: angka murni dan {@code "Bulanan-"} merujuk
	 * {@link PengaturanPembayaranBulanan}, {@code "Item-"} merujuk {@link ItemBiaya}, sedangkan
	 * {@code "Keranjang-"} didelegasikan ke
	 * {@code PembayaranGatewayHelper.prosesSatuTokenKeranjang}.</p>
	 *
	 * <h4>Mode inquiry bersifat baca-saja</h4>
	 * <p>Bila {@code inquery} bernilai {@code true}, seluruh blok penyimpanan dilewati sehingga
	 * pengecekan tagihan oleh bank tidak pernah tercatat sebagai pembayaran, dan balasan diberi
	 * {@code PaymentFlagStatus=00}.</p>
	 *
	 * <h4>Jaminan pencatatan log H2H</h4>
	 * <p>Blok {@code finally} <b>selalu</b> memanggil
	 * {@code PembayaranGatewayHelper.catatLogHostToHost} — tanpa syarat {@code bankHost} — supaya
	 * permintaan dari IP yang tidak dikenal pun tetap meninggalkan jejak audit. Ini adalah
	 * keputusan arsitektur yang disengaja, bukan kelalaian pemeriksaan: pencatatan log memang
	 * dibuat tidak bergantung pada dikenalinya pemanggil.</p>
	 *
	 * <p><b>Catatan.</b> Nilai {@code bankHost} {@code null} (IP tak dikenal) tidak menghentikan
	 * pemrosesan; lihat pembahasan model autentikasi pada javadoc {@link BSI kelas ini}.</p>
	 *
	 * @param nominalP    nominal setoran yang dikirim bank; diabaikan saat {@code inquery}
	 * @param tanggalP    waktu transaksi berformat {@code yyyyMMddHHmmss}; bila gagal diurai,
	 *                    dipakai waktu server saat ini
	 * @param va          nomor Virtual Account yang dibayar
	 * @param companyCode kode biller pengirim, dicocokkan dengan prefix kode pembayaran jenis
	 *                    kegiatan; boleh kosong untuk melewati pencocokan
	 * @param RequestID   id transaksi dari bank; hanya diteruskan, tidak dipakai untuk logika
	 * @param bank        label bank yang disimpan sebagai {@code validator} pada Kegiatan dan
	 *                    CicilanPembayaran; bernilai {@code "BSI"}
	 * @param bankHost    host bank hasil pencocokan IP; boleh {@code null} dan tidak menjadi
	 *                    gerbang
	 * @param request     permintaan asli, dipakai untuk pencatatan log H2H
	 * @param data        badan permintaan mentah, disimpan apa adanya ke log H2H
	 * @param chekLagi    penanda dari pemanggil; pada alur servlet selalu {@code true}
	 * @param inquery     {@code true} untuk mode tanya tagihan (baca saja), {@code false} untuk
	 *                    pelunasan
	 * @param chek        mode pemeriksaan internal; melonggarkan pemeriksaan "sudah terbayar"
	 * @return objek JSON balasan lengkap dengan {@code rc}, {@code msg}, identitas pembayar,
	 *         {@code totalNominal}, {@code informasi}, dan {@code rincian}
	 * @throws Exception bila penyusunan JSON gagal; kegagalan basis data sendiri sudah ditangkap
	 *                   di dalam dan diubah menjadi {@code rc=ERR-DB}
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject doProcess(double nominalP, String tanggalP, String va, String companyCode,
			String RequestID, String bank, BankHost bankHost, HttpServletRequest request, String data, boolean chekLagi,
			boolean inquery, boolean chek) throws Exception {

		JSONObject response = new JSONObject();

		response.put("rc", "OK");
		response.put("msg", inquery ? "Inquiry Succeeded" : "Payment Succeeded");
		response.put("nomorPembayaran", va);
		response.put("idPelanggan", va);

		response.put("nama", "");
		response.put("totalNominal", 0.0);

		JSONArray arr_informasi = new JSONArray();
		response.put("informasi", arr_informasi);

		JSONArray rincian = new JSONArray();
		response.put("rincian", rincian);

		String idTagihan = Common.getGeneratedBarCode();

		response.put("idTagihan", idTagihan);

		{ // TANPA syarat bankHost: request bank apa pun WAJIB tercatat ke log H2H (lihat finally)
			String nim = "";
			String nama = "";
			// Jejak stack trace bila inquiry/pembayaran error; disimpan ke kolom log H2H.
			String h2hStackTrace = null;

			VirtualAccountBank virtualAccountBankNtt = null;
			Session session = null;
			try {

				virtualAccountBankNtt = VirtualAccountBank.ambilVa(va, nominalP, bankHost);

				if (virtualAccountBankNtt == null) {
					response.put("rc", "ERR-NOT-FOUND");
					response.put("msg", "Error nomor VA tidak ditemukan");
				}

				else if (VirtualAccountBank.isSudahTerbayarUntukPayment(virtualAccountBankNtt, inquery, false, chek)) {

					response.put("rc", "ERR-ALREADY-PAID");
					response.put("msg", "Sudah Terbayar");

				}

				else if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {
					session = HibernateUtil.openSession();
					{

						int mytotal = virtualAccountBankNtt.totalBiaya();

						boolean biaya_admin_jangan_dimasukkan_saat_simpan_va = Common.bolehKonfigurasi("biaya_admin_jangan_dimasukkan_saat_simpan_va", Konfigurasi.TIDAK_AKTIF);

						if (biaya_admin_jangan_dimasukkan_saat_simpan_va) {
							mytotal = virtualAccountBankNtt.getTotal().intValue();
						}

						response.put("totalNominal", mytotal);

						Double nominal = nominalP;

						if (!inquery && nominal.intValue() != mytotal) {
							response.put("rc", "ERR-PAYMENT-WRONG-AMOUNT");
							response.put("msg", "Terdapat kesalahan nilai pembayaran " + nominal.intValue()
									+ " tidak sama dengan tagihan " + mytotal);
						} else {

							Date tanggal = ais.ui.util.WaktuUtil.getDate();
							try {
								tanggal = dateFormat.get().parse(tanggalP);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/BSI.java:161");
							}

							if (virtualAccountBankNtt.getSiswa() != null
									|| virtualAccountBankNtt.getCalonSiswa() != null) {

								if (!biaya_admin_jangan_dimasukkan_saat_simpan_va) {
									if (virtualAccountBankNtt.getBiayaAdmin() > 0.1) {
										JSONObject bill = new JSONObject();
										bill.put("kode_rincian", "Biaya admin");
										bill.put("deskripsi", "Biaya admin");
										bill.put("nominal", virtualAccountBankNtt.getBiayaAdmin().intValue());
										rincian.put(bill);

									}
								}

								String hp = "";
								String email = "";
								if (virtualAccountBankNtt.getSiswa() != null) {

									nim = virtualAccountBankNtt.getSiswa().getNomorInduk();
									nama = virtualAccountBankNtt.getSiswa().getNama();
									hp = virtualAccountBankNtt.getSiswa().getTeleponSiswa();
									email = virtualAccountBankNtt.getSiswa().getAlamatEmail();
								} else if (virtualAccountBankNtt.getCalonSiswa() != null) {

									nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk();
									nama = virtualAccountBankNtt.getCalonSiswa().getNama();
									hp = virtualAccountBankNtt.getCalonSiswa().getTeleponSiswa();
									email = virtualAccountBankNtt.getCalonSiswa().getAlamatEmail();
								}

								hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
								hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
								hp = !hp.startsWith("+") ? "+62" + hp : hp;

								response.put("nomorHP", hp);

								response.put("email", email);

								response.put("nama", nama + " " + nim);
								response.put("totalNominal", mytotal);

								Map<String, List<Tagihan>> map = VirtualAccountBank.bayarSiswa(virtualAccountBankNtt,
										session, tanggal, bank, inquery, data, false);

								int infoke = 1;
								for (List<Tagihan> tagihans : map.values()) {

									for (Tagihan tagihan : tagihans) {

										JSONObject jsonObjectInfo = new JSONObject();
										jsonObjectInfo.put("label_key", "INFO" + (infoke++));
										jsonObjectInfo.put("label_value",
												tagihan.getItemBiayaSekolah() + " "
														+ (tagihan.getBulan() != null && tagihan.getBulan() > 0
																? " bulan " + tagihan.getBulan()
																: "")
														+ (tagihan.getTahun() != null && tagihan.getTahun() > 0
																? " tahun " + tagihan.getTahun()
																: ""));
										arr_informasi.put(jsonObjectInfo);

										if (inquery) {
											JSONObject bill = new JSONObject();
											bill.put("kode_rincian", tagihan.getItemBiayaSekolah().getKode());
											bill.put("deskripsi", tagihan.getItemBiayaSekolah().getNama());
											bill.put("nominal", tagihan.getNominal().intValue());
											rincian.put(bill);

											if (tagihan.getDenda() > 0.1) {
												bill = new JSONObject();

												bill.put("kode_rincian", "D" + tagihan.getItemBiayaSekolah().getKode());
												bill.put("deskripsi",
														"Denda " + tagihan.getItemBiayaSekolah().getNama());
												bill.put("nominal", tagihan.getDenda().intValue());
												rincian.put(bill);

											}

										}
									}

								}

								if (inquery) {
									response.put("PaymentFlagStatus", "00");
								}

							} else {

								JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();

								if (jenisKegiatan != null && jenisKegiatan.getPrefixKodePembayaran() != null
										&& companyCode != null && !companyCode.trim().isEmpty()
										&& !jenisKegiatan.getPrefixKodePembayaran().equalsIgnoreCase(companyCode)) {
									response.put("rc", "ERR-NOT-FOUND");
									response.put("msg", "Error nomor VA tidak ditemukan");
								} else {

									Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
									BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt
											.getBiodataCalonMahasiswa();

									Integer semester = virtualAccountBankNtt.getSemester();

									nim = mahasiswa == null ? biodataCalonMahasiswa.getNoRegistrasi()
											: mahasiswa.getNim();
									nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();

									String hp = mahasiswa == null ? biodataCalonMahasiswa.getHp() : mahasiswa.getTelp();
									if (hp != null) {
										hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
										hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
										hp = !hp.startsWith("+") ? "+62" + hp : hp;
									}
									if (hp == null) {
										hp = "";
									}
									response.put("nomorHP", hp);

									String email = mahasiswa == null ? biodataCalonMahasiswa.getEmail()
											: mahasiswa.getEmail();
									response.put("email", email);

									response.put("nama", nama + " " + nim);
									response.put("totalNominal", mytotal);

									Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null ? null
											: session.createCriteria(Kegiatan.class)
													.add(Restrictions.idEq(virtualAccountBankNtt.getKegiatan()))
													.uniqueResult());

									if (kegiatan == null || kegiatan.getId() == null) {

										kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class)
												.addOrder(Order.asc("id"))

												.add(biodataCalonMahasiswa != null
														? Restrictions.eq("calonMahasiswa", biodataCalonMahasiswa)
														: Restrictions.eq("mahasiswa", mahasiswa))
												.add(Restrictions.eq("jenisKegiatan",
														virtualAccountBankNtt.getJenisKegiatan()))
												.add(Restrictions.eq("semster", semester))

												.setMaxResults(1).uniqueResult();
									}

									if (kegiatan == null || kegiatan.getId() == null) {
										kegiatan = new Kegiatan();
									}

									// Inquiry hanya boleh membaca tagihan. Sebelumnya blok ini ikut mengubah dan
									// menyimpan Kegiatan, sehingga pengecekan dari bank dapat terlihat seperti
									// pembayaran sebelum dana benar-benar didebit.
									if (!inquery || kegiatan.getId() == null) {
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
									}

									if (!inquery) {
										session.getTransaction().begin();
										Common.refreshSaveOrUpdate(session, kegiatan);
										commitTransaksiAman(session, "331");
									}

									List<Long> detailBiayasId = new ArrayList<Long>();
									for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(), ",")) {
										try {
											detailBiayasId.add(Long.parseLong(id.trim()));
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BSI.java:337");

										}
									}

									Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
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
									System.out.println(
											"cicilanPembayaran total -> " + total + " totalTagihan " + totalTagihan);

									if (!biaya_admin_jangan_dimasukkan_saat_simpan_va) {
										if (virtualAccountBankNtt.getBiayaAdmin() > 0.1) {
											JSONObject bill = new JSONObject();
											bill.put("kode_rincian", "Biaya admin");
											bill.put("deskripsi", "Biaya admin");
											bill.put("nominal", virtualAccountBankNtt.getBiayaAdmin().intValue());
											rincian.put(bill);

										}
									}

									int infoke = 1;
									if (virtualAccountBankNtt.getCicilan() != null
											&& !virtualAccountBankNtt.getCicilan().isEmpty()) {
										for (String idPemBul : StringUtils.split(virtualAccountBankNtt.getCicilan(),
												",")) {
											if (Common.isNumber(idPemBul)) {
												PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
														.createCriteria(PengaturanPembayaranBulanan.class)
														.add(Restrictions.idEq(Long.parseLong(idPemBul)))
														.uniqueResult();

												if (pengaturanPembayaranBulanan != null) {
													String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
															+ virtualAccountBankNtt.getId();

													ItemBiaya itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya()
															.getItemBiaya();
													Double subtotal = 0.0;
													try {
														String[] spl = idPemBul.split("-");
														subtotal = Double.parseDouble(spl[spl.length - 1]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BSI.java:393");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													JSONObject jsonObjectInfo = new JSONObject();
													jsonObjectInfo.put("label_key", "INFO" + (infoke++));
													jsonObjectInfo.put("label_value",
															itemBiaya.getNama() + " " + " bulan "
																	+ pengaturanPembayaranBulanan.getNamaBulan() + " "
																	+ pengaturanPembayaranBulanan.getDetailBiaya()
																			.getTahunAkademik());
													arr_informasi.put(jsonObjectInfo);

													JSONObject bill = new JSONObject();

													bill.put("kode_rincian", itemBiaya.getKode());
													bill.put("deskripsi", itemBiaya.getNama());
													bill.put("nominal", subtotal.intValue());

													rincian.put(bill);

													if (!inquery) {

														CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																.createCriteria(CicilanPembayaran.class)
																.add(Restrictions.eq("ref", ref)).setMaxResults(1)
																.uniqueResult();

														if (cicilanPembayaran == null) {
															cicilanPembayaran = new CicilanPembayaran(
																	pengaturanPembayaranBulanan.getDetailBiaya());
														}
														cicilanPembayaran.setRef(ref);
														cicilanPembayaran.setValidator(bank);
														cicilanPembayaran.setKegiatan(kegiatan);
														cicilanPembayaran.setItemBiaya(pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya());
														cicilanPembayaran.setPengaturanPembayaranBulanan(
																pengaturanPembayaranBulanan);
														cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());
														cicilanPembayaran.setNilai(subtotal);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														cicilanPembayaran.setTanggal(tanggal);
														cicilanPembayaran.setJenisPembayaran(bankHost == null
																|| bankHost.getJenisPembayaran() == null
																		? ConstantValues.TUNAI
																		: bankHost.getJenisPembayaran());
														cicilanPembayaran.setDenda(0.0);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());

														session.getTransaction().begin();
														if (cicilanPembayaran.getId() == null)
															session.save(cicilanPembayaran);
														else
															Common.refreshUpdate(session, cicilanPembayaran);
														commitTransaksiAman(session, "452");

														JSONObject jsonObjectRinci = new JSONObject();
														jsonObjectRinci.put("nama", pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya().getNama());
														jsonObjectRinci.put("bulan",
																pengaturanPembayaranBulanan.getNamaBulan());
														jsonObjectRinci.put("nominal", pengaturanPembayaranBulanan
																.ambilNominalModifikasi(mahasiswa, semester));

														rincian.put(jsonObjectRinci);
													}
//											}
												}
											} else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
												PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
														.createCriteria(PengaturanPembayaranBulanan.class)
														.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1])))
														.uniqueResult();

												if (pengaturanPembayaranBulanan != null) {
													String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
															+ virtualAccountBankNtt.getId();

													ItemBiaya itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya()
															.getItemBiaya();

													JSONObject jsonObjectInfo = new JSONObject();
													jsonObjectInfo.put("label_key", "INFO" + (infoke++));
													jsonObjectInfo.put("label_value",
															itemBiaya.getNama() + " " + " bulan "
																	+ pengaturanPembayaranBulanan.getNamaBulan() + " "
																	+ pengaturanPembayaranBulanan.getDetailBiaya()
																			.getTahunAkademik());
													arr_informasi.put(jsonObjectInfo);

													Double subtotal = 0.0;
													try {
														String[] spl = idPemBul.split("-");
														subtotal = Double.parseDouble(spl[spl.length - 1]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BSI.java:492");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													if (inquery) {
														JSONObject bill = new JSONObject();
														bill.put("kode_rincian", itemBiaya.getKode());
														bill.put("deskripsi", itemBiaya.getNama());
														bill.put("nominal", subtotal.intValue());

														rincian.put(bill);

													} else {
														CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																.createCriteria(CicilanPembayaran.class)
																.add(Restrictions.eq("ref", ref)).setMaxResults(1)
																.uniqueResult();

														if (cicilanPembayaran == null) {
															cicilanPembayaran = new CicilanPembayaran(
																	pengaturanPembayaranBulanan.getDetailBiaya());
														}
														cicilanPembayaran.setRef(ref);
														cicilanPembayaran.setValidator(bank);
														cicilanPembayaran.setKegiatan(kegiatan);
														cicilanPembayaran.setItemBiaya(itemBiaya);
														cicilanPembayaran.setPengaturanPembayaranBulanan(
																pengaturanPembayaranBulanan);
														cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

														cicilanPembayaran.setNilai(subtotal);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														cicilanPembayaran.setTanggal(tanggal);
														cicilanPembayaran.setJenisPembayaran(bankHost == null
																|| bankHost.getJenisPembayaran() == null
																		? ConstantValues.TUNAI
																		: bankHost.getJenisPembayaran());
														cicilanPembayaran.setDenda(0.0);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														session.getTransaction().begin();
														if (cicilanPembayaran.getId() == null)
															session.save(cicilanPembayaran);
														else
															Common.refreshUpdate(session, cicilanPembayaran);
														commitTransaksiAman(session, "540");

														JSONObject bill = new JSONObject();
														bill.put("kode_rincian", itemBiaya.getKode());
														bill.put("deskripsi", itemBiaya.getNama());
														bill.put("nominal", subtotal.intValue());

														rincian.put(bill);

														JSONObject jsonObjectRinci = new JSONObject();
														jsonObjectRinci.put("nama", pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya().getNama());
														jsonObjectRinci.put("bulan",
																pengaturanPembayaranBulanan.getNamaBulan());
														jsonObjectRinci.put("nominal", subtotal);

														rincian.put(jsonObjectRinci);
//												}
													}
												}
											} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
												ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
														.simpleObject(
																session.createCriteria(ItemBiaya.class)
																		.add(Restrictions.idEq(Long
																				.parseLong(idPemBul.split("-")[1]))),
																ItemBiaya.class);

												if (itemBiaya != null) {
													String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
															+ virtualAccountBankNtt.getId();

													JSONObject jsonObjectInfo = new JSONObject();
													jsonObjectInfo.put("label_key", "INFO" + (infoke++));
													jsonObjectInfo.put("label_value", itemBiaya.getNama() + " "
															+ virtualAccountBankNtt.getTahunAkademik());
													arr_informasi.put(jsonObjectInfo);

													Double subtotal = 0.0;
													try {
														String[] spl = idPemBul.split("-");
														subtotal = Double.parseDouble(spl[2]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BSI.java:582");
													}

													Long detailBiayaId = null;
													try {
														String[] spl = idPemBul.split("-");
														detailBiayaId = Long.parseLong(spl[4]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BSI.java:589");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													if (inquery) {
														JSONObject bill = new JSONObject();
														bill.put("kode_rincian", itemBiaya.getKode());
														bill.put("deskripsi", itemBiaya.getNama());
														bill.put("nominal", subtotal.intValue());

														rincian.put(bill);

													} else {

														CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																.createCriteria(CicilanPembayaran.class)
																.add(Restrictions.eq("ref", ref)).setMaxResults(1)
																.uniqueResult();

														if (cicilanPembayaran == null) {
															cicilanPembayaran = new CicilanPembayaran(
																	DetailBiaya.muatRefAman(session, detailBiayaId));
														}
														cicilanPembayaran.setDetailBiaya(DetailBiaya.muatRefAman(session, detailBiayaId));
														cicilanPembayaran.setRef(ref);
														cicilanPembayaran.setValidator(bank);
														cicilanPembayaran.setKegiatan(kegiatan);
														cicilanPembayaran.setItemBiaya(itemBiaya);
														cicilanPembayaran.setPengaturanPembayaranBulanan(null);
														cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

														cicilanPembayaran.setNilai(subtotal);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														cicilanPembayaran.setTanggal(tanggal);
														cicilanPembayaran.setJenisPembayaran(bankHost == null
																|| bankHost.getJenisPembayaran() == null
																		? ConstantValues.TUNAI
																		: bankHost.getJenisPembayaran());
														cicilanPembayaran.setDenda(0.0);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														session.getTransaction().begin();
														if (cicilanPembayaran.getId() == null)
															session.save(cicilanPembayaran);
														else
															Common.refreshUpdate(session, cicilanPembayaran);
														commitTransaksiAman(session, "638");

														JSONObject bill = new JSONObject();
														bill.put("kode_rincian", itemBiaya.getKode());
														bill.put("deskripsi", itemBiaya.getNama());
														bill.put("nominal", subtotal.intValue());

														rincian.put(bill);

														JSONObject jsonObjectRinci = new JSONObject();
														jsonObjectRinci.put("nama", itemBiaya.getNama());
														jsonObjectRinci.put("bulan", "");
														jsonObjectRinci.put("nominal", subtotal);

														rincian.put(jsonObjectRinci);

													}
												}
											} else if (idPemBul != null && idPemBul.startsWith("Keranjang-")) {
												// Pembayaran Keranjang Belanja (multi jenis / KegiatanTemporary): konversi draf
												// menjadi Kegiatan+Cicilan nyata - pemroses terpusat yang sama dengan Esmartlink.
												if (!inquery) {
													ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
															virtualAccountBankNtt, false, bank, bankHost, tanggal, data, null);
												}
											}
										}
									}

									if (inquery) {
										response.put("PaymentFlagStatus", "00");

									} else {
										Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session,
												kegiatan);
										Double jumlah = d[0];
										Double denda = d[1];
										kegiatan.setDenda(denda.doubleValue());
										kegiatan.setAmountTerhutang(
												nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));

										kegiatan.setAmount(jumlah.doubleValue() > 0.1 ? jumlah.doubleValue()
												: virtualAccountBankNtt.getTotal());
										kegiatan.setValidator(bank);

										session.getTransaction().begin();
										Common.refreshUpdate(session, kegiatan);
										commitTransaksiAman(session, "685");

										VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data,
												bank);

									}
								}
							}
						}

					}

				}

			} catch (Exception e) {
					try {
						if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
							session.getTransaction().rollback();
						}
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/BSI.java:704");
					}

					response.put("rc", "ERR-DB");
				response.put("msg", "Error saat Update Transaksi");

				h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
				Common.tampilErrorJikaAdmin(e);
			} finally {
				Common.closeNativeSessionQuietly(session);
				// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception, dan
				// TANPA syarat bankHost (request dari IP tak dikenal pun WAJIB tercatat).
				ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
						response.toString(), nominalP, rincian.toString(), h2hStackTrace);
			}

			}

		if (Common.bolehKonfigurasi("bsi_paksa_gagal", Konfigurasi.TIDAK_AKTIF)) {
			response.put("rc", Common.getKonfigurasi("bsi_paksa_gagal_rc", "ERR-DB").getNilai());
			response.put("msg", Common.getKonfigurasi("bsi_paksa_gagal_msg", "Error saat Update Transaksi").getNilai());
		}

		return response;
	}

	/**
	 * Membaca badan permintaan, menetapkan {@link BankHost} dari alamat IP pemanggil, memanggil
	 * {@link #doProses}, lalu menuliskan hasilnya sebagai JSON.
	 *
	 * <p>Badan permintaan dibaca baris demi baris lalu digabung <b>tanpa pemisah</b>, sehingga
	 * JSON yang dikirim bank dalam beberapa baris tetap menjadi satu dokumen utuh.</p>
	 *
	 * <p>Identifikasi pemanggil memakai
	 * {@link PembayaranUtil#getBankHost(String, String)} dengan
	 * {@link HttpServletRequest#getRemoteAddr()} — alamat TCP sebenarnya, <b>bukan</b> header
	 * {@code X-Forwarded-For} atau sejenisnya yang dapat dipalsukan klien. Nilai yang dihasilkan
	 * hanya dilekatkan pada log dan pencarian VA; ia <b>tidak</b> dipakai untuk menolak
	 * permintaan (lihat javadoc {@link BSI kelas ini}).</p>
	 *
	 * <p>Bila {@link #doProses} melempar exception, balasan diganti
	 * {@link #errorDB(String)} sehingga bank tetap menerima JSON berbentuk benar dan tidak
	 * menganggap tagihan lunas.</p>
	 *
	 * <p>Isi badan permintaan dan query string dicetak ke {@link System#out}. Untuk kanal ini
	 * payload memang tidak terenkripsi dan tidak memuat kredensial, sehingga pencetakan tersebut
	 * tidak membocorkan rahasia — berbeda dengan {@link Bsiresponse} yang payload-nya harus
	 * disamarkan.</p>
	 *
	 * @param request  permintaan dari sisi bank
	 * @param response tujuan penulisan balasan JSON
	 * @throws Exception bila pembacaan badan permintaan atau penulisan balasan gagal
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
		String bank = "BSI";
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

		String body;
		try {
			// 05, request tidak diizinkan
			body = BSI.doProses(data, request, bankHost, bank, false).toString();
		} catch (Exception e) {
			JSONObject req = data == null ? null : new JSONObject(data);

			String va = req == null || req.isNull("nomorPembayaran") ? request.getParameter("nomorPembayaran")
					: req.getString("nomorPembayaran");

			body = errorDB(va);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/BSI.java:760");

		}

		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();

		writer.write(body);

	}

	/**
	 * Menyusun balasan JSON berbentuk lengkap namun bertanda gagal, dipakai saat pemrosesan
	 * melempar exception sebelum sempat menghasilkan balasan.
	 *
	 * <p>Struktur balasan sengaja dibuat sama dengan balasan sukses ({@code nomorPembayaran},
	 * {@code idPelanggan}, {@code nama}, {@code totalNominal}, {@code informasi}, {@code rincian},
	 * {@code idTagihan}) agar pengurai di sisi bank tidak ikut gagal; yang membedakan hanyalah
	 * {@code rc} dan {@code msg}. Keduanya dapat diatur lewat konfigurasi
	 * {@code bsi_paksa_gagal_rc} dan {@code bsi_paksa_gagal_msg}, dengan bawaan {@code ERR-DB}
	 * dan {@code "Error saat Update Transaksi"}.</p>
	 *
	 * <p>Seluruh isi method dibungkus {@code try/catch} sehingga tetap mengembalikan JSON yang
	 * dapat diurai walaupun pembacaan konfigurasi gagal.</p>
	 *
	 * @param va nomor Virtual Account yang sedang diproses; diikutkan agar bank dapat
	 *           mencocokkan balasan dengan permintaannya
	 * @return teks JSON balasan gagal
	 */
	public static String errorDB(String va) {
		JSONObject response = new JSONObject();
		try {

			response.put("rc", Common.getKonfigurasi("bsi_paksa_gagal_rc", "ERR-DB").getNilai());
			response.put("msg", Common.getKonfigurasi("bsi_paksa_gagal_msg", "Error saat Update Transaksi").getNilai());
			response.put("nomorPembayaran", va);
			response.put("idPelanggan", va);

			response.put("nama", "");
			response.put("totalNominal", 0.0);

			JSONArray arr_informasi = new JSONArray();
			response.put("informasi", arr_informasi);

			JSONArray rincian = new JSONArray();
			response.put("rincian", rincian);

			String idTagihan = Common.getGeneratedBarCode();

			response.put("idTagihan", idTagihan);
		} catch (Exception e) {

			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/BSI.java:795");

		}
		return response.toString();
	}

	/**
	 * Mengurai payload permintaan bank menjadi parameter-parameter terpisah, lalu meneruskannya
	 * ke {@link #doProcess}.
	 *
	 * <p>Setiap nilai dicari lebih dulu di badan JSON; bila tidak ada di sana, dipakai parameter
	 * query dengan nama yang sama. Pemetaan namanya: {@code nomorPembayaran} menjadi nomor VA,
	 * {@code kodeBiller} menjadi kode biller, {@code idTransaksi} menjadi id permintaan,
	 * {@code totalNominal} menjadi nominal setoran, dan {@code tanggalTransaksi} menjadi waktu
	 * transaksi (bila kosong dipakai waktu server saat ini).</p>
	 *
	 * <p>Field {@code action} menentukan mode: nilai {@code "inquiry"} (tanpa membedakan
	 * besar-kecil huruf) berarti tanya tagihan yang bersifat baca-saja, nilai lain — termasuk
	 * {@code null} — diperlakukan sebagai <b>pelunasan</b>.</p>
	 *
	 * <p>Kegagalan penguraian {@code totalNominal} tidak menghentikan alur: nominal tetap
	 * {@code 0.0} dan dicatat lewat {@code ErrorAuditUtil}. Pada mode pembayaran nilai nol itu
	 * hampir pasti tidak sama dengan total tagihan, sehingga permintaan ditolak
	 * {@link #doProcess} dengan {@code ERR-PAYMENT-WRONG-AMOUNT}.</p>
	 *
	 * @param data     badan permintaan mentah; boleh {@code null}, saat itu seluruh nilai diambil
	 *                 dari parameter query
	 * @param request  permintaan asli, sumber cadangan parameter dan bahan pencatatan log H2H
	 * @param bankHost host bank hasil pencocokan IP; boleh {@code null}
	 * @param bank     label bank yang akan tercatat sebagai {@code validator}
	 * @param chek     mode pemeriksaan internal, diteruskan apa adanya ke {@link #doProcess}
	 * @return teks JSON balasan
	 * @throws Exception bila payload bukan JSON yang sah atau pemrosesan gagal
	 */
	public static String doProses(String data, HttpServletRequest request, BankHost bankHost, String bank, boolean chek)
			throws Exception {
		JSONObject req = data == null ? null : new JSONObject(data);

		String action = req == null || req.isNull("action") ? request.getParameter("action") : req.getString("action");

		String va = req == null || req.isNull("nomorPembayaran") ? request.getParameter("nomorPembayaran")
				: req.getString("nomorPembayaran");

		String companyCode = req == null || req.isNull("kodeBiller") ? request.getParameter("kodeBiller")
				: req.getString("kodeBiller");

		String requestID = req == null || req.isNull("idTransaksi") ? request.getParameter("idTransaksi")
				: req.getString("idTransaksi");

		double nominalP = 0.0;
		try {
			nominalP = req == null || req.isNull("totalNominal") ? 0.0
					: Double.parseDouble(req.get("totalNominal").toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BSI.java:820");
			// TODO: handle exception
		}

		String tanggalP = req == null || req.isNull("tanggalTransaksi") ? dateFormat.get().format(new Date())
				: req.getString("tanggalTransaksi");

		// 05, request tidak diizinkan

		System.out.println("action " + action);

		String body = BSI.doProcess(nominalP, tanggalP, va, companyCode, requestID, bank, bankHost, request, data, true,
				action != null && action.equalsIgnoreCase("inquiry"), chek).toString();
		return body;
	}

}

