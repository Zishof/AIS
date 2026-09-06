package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
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
import ais.database.model.DetailKegiatan;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.WaktuUtil;

/**
 * Servlet gateway <b>host-to-host (H2H)</b> Bank BRI (Briva) — pintu masuk callback bank untuk
 * inquiry tagihan, posting pembayaran, dan pembatalan (reversal) Virtual Account.
 *
 * <h3>Otentikasi H2H — FAKTA ARSITEKTUR YANG WAJIB DIPAHAMI</h3>
 * <p>Verifikasi tanda tangan RSA (SHA256withRSA atas {@code X-Client-Key|X-Timestamp}) hanya
 * dijalankan pada cabang penerbitan token di {@link #doProses(String, HttpServletRequest,
 * BankHost, String, boolean)}, yaitu ketika body memuat {@code grantType}. <b>Riwayat.</b> Sampai
 * dengan perbaikan ini, cabang transaksi — inquiry, payment, dan reversal, yakni jalur yang
 * benar-benar memutasi data keuangan — sama sekali tidak memeriksa tanda tangan maupun bearer
 * token: token yang diterbitkan hanya ditulis ke daftar statis kelas ini, tidak pernah dibaca
 * kembali. Sekarang cabang transaksi memeriksa header {@code Authorization: Bearer &lt;token&gt;}
 * terhadap token yang diterbitkan cabang penerbitan token (disimpan bersama-aman-thread dengan
 * masa berlaku di
 * {@link ais.action.ws.util.PembayaranGatewayHelper#terbitkanTokenH2h(String, String, long)}),
 * lewat
 * {@link ais.action.ws.util.PembayaranGatewayHelper#periksaGerbangTokenH2h(String, String, String)}.
 * <b>Gerbang ini berlaku BERTAHAP lewat konfigurasi</b> {@code gerbang_token_h2h_briva} (nilai
 * {@code nonaktif}/{@code log}/{@code aktif}, default {@code log}): pada mode {@code log}
 * permintaan tanpa token sah TETAP diproses seperti sebelumnya, hanya dicatat sebagai peringatan,
 * sehingga operator dapat mengukur dampak sebelum menyalakan penolakan sungguhan lewat
 * {@code aktif}. Timestamp {@code X-Timestamp} pada penerbitan token kini juga diperiksa
 * kesegarannya (toleransi {@code toleransi_menit_timestamp_h2h_briva} menit, default 5 menit)
 * lewat gerbang {@code gerbang_timestamp_h2h_briva} (default {@code log}).</p>
 * <p>Penyaring lain yang tersisa pada jalur transaksi: pemetaan IP penelepon ke {@link BankHost}
 * lewat {@link PembayaranUtil#getBankHost(String, String)} — nilainya boleh {@code null}, dan
 * {@link VirtualAccountBank#ambilVa(String, Double, BankHost)} menerima VA yang kolom
 * {@code bankHost}-nya kosong untuk host pemanggil mana pun. Sejak perbaikan ini, jalur POSTING
 * (payment/reversal — BUKAN inquiry, yang tetap harus berfungsi tanpa IP terdaftar untuk
 * kebutuhan operasional) dapat dibuat fail-closed terhadap {@code bankHost == null} lewat gerbang
 * {@code gerbang_bankhost_null_posting_briva}
 * ({@link ais.action.ws.util.PembayaranGatewayHelper#periksaGerbangBankHostPosting(String, String, BankHost)}),
 * default {@code nonaktif} karena {@code bankHost == null} bisa jadi kondisi normal hari ini —
 * menyalakannya tanpa koordinasi dapat menghentikan setelmen.</p>
 * <p>Ketiga gerbang di atas TIDAK mengubah bentuk/kode respons SNAP pada kasus sukses; mode
 * {@code aktif} hanya menambah kemungkinan respons "Unauthorized" baru pada kasus yang
 * sebelumnya diam-diam lolos. Catatan ini adalah dokumentasi keadaan terkini; jangan diubah tanpa
 * koordinasi dengan pihak bank karena perubahan kontrak H2H berdampak langsung ke setelmen.</p>
 *
 * @see ais.action.ws.util.PembayaranGatewayHelper
 * @see VirtualAccountBank
 * @see BankHost
 */
public class Briva extends HttpServlet {
	/**
	 * Versi serialisasi {@link java.io.Serializable} yang diwarisi dari {@link HttpServlet}.
	 *
	 * <p>Bernilai tetap {@code 1L}: servlet ini tidak pernah diserialisasi antarnode, sehingga nilai
	 * ini semata memenuhi kontrak {@code Serializable} dan meredam peringatan kompilator.</p>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton utilitas pembayaran; dipakai {@link #process(HttpServletRequest, HttpServletResponse)}
	 * untuk memetakan alamat IP penelepon menjadi {@link BankHost}, dan oleh
	 * {@link #doProcess doProcess} untuk menjumlahkan cicilan lewat
	 * {@code getTotalDanDendaFromCicilan}.
	 *
	 * <p><b>Perhatikan:</b> {@link BankHost} hasil pemetaan bersifat <i>informasional</i> pada
	 * jalur inquiry — nilai {@code null} tidak memblokir dan hanya membuat jenis pembayaran jatuh
	 * ke {@code ConstantValues.TUNAI}. Sejak gerbang {@code gerbang_bankhost_null_posting_briva}
	 * ditambahkan, khusus jalur POSTING (payment/reversal) keadaan {@code null} dapat dibuat
	 * gagal-tertutup; lihat Javadoc kelas untuk nilai default dan cara menyalakannya.</p>
	 */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * Pemformat tanggal per-thread untuk pola ISO tanpa zona waktu ({@code yyyy-MM-dd'T'HH:mm:ss}).
	 *
	 * <p>Dibungkus {@link ThreadLocal} karena {@link SimpleDateFormat} tidak aman-thread, sedangkan
	 * servlet melayani banyak permintaan secara paralel. Dipakai {@link #doProcess doProcess} untuk
	 * mengurai {@code trxDateTime} menjadi tanggal pembukuan; karena polanya tidak memuat offset
	 * zona, {@link #doProses} lebih dulu membuang sufiks {@code +07:00}.</p>
	 *
	 * <p>Bila penguraian gagal, {@link #doProcess doProcess} <b>tidak</b> menolak permintaan
	 * melainkan mundur ke waktu sekarang, sehingga tanggal pembukuan dapat berbeda dari tanggal
	 * transaksi yang sesungguhnya.</p>
	 */
	public static final ThreadLocal<DateFormat> dateFormat1 = new ThreadLocal<DateFormat>() {
		/**
		 * Membuat pemformat milik thread pemanggil saat {@code dateFormat1.get()} dipakai pertama
		 * kali pada thread itu.
		 *
		 * <p>Setiap thread memperoleh instance {@link SimpleDateFormat} tersendiri, sehingga
		 * ketiadaan keamanan-thread pada kelas tersebut tidak menjadi masalah. Pola
		 * {@code yyyy-MM-dd'T'HH:mm:ss} sengaja tanpa offset zona: {@link #doProses} membuang
		 * sufiks {@code +07:00} lebih dulu.</p>
		 *
		 * <p>Pemformat ini bersifat <i>lenient</i> (bawaan {@link SimpleDateFormat}), sehingga
		 * tanggal di luar jangkauan seperti bulan ke-13 akan digulung, bukan ditolak.</p>
		 *
		 * @return pemformat baru khusus untuk thread pemanggil
		 */
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		}
	};

	/**
	 * Membentuk instance servlet.
	 *
	 * <p>Konstruktor tanpa argumen yang dibutuhkan wadah servlet: Tomcat membuat satu instance
	 * {@link Briva} lalu memakainya kembali untuk kedua {@code url-pattern} yang dipetakan ke kelas
	 * ini. Tidak ada state yang disiapkan di sini — inisialisasi terjadi pada penginisialisasi field
	 * {@link #pembayaranUtil} dan {@link #dateFormat1}.</p>
	 *
	 * <p>Karena instance dipakai bersama oleh banyak thread permintaan, jangan menambahkan field
	 * instance yang dapat berubah dan bergantung pada satu permintaan; pakai variabel lokal atau
	 * {@link ThreadLocal}.</p>
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public Briva() {
		super();
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikannya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p>GET dan POST diperlakukan identik. Dalam praktik BRI selalu memakai POST — jalur GET
	 * disediakan agar endpoint mudah diuji manual dan agar permintaan yang salah metode tetap
	 * menghasilkan balasan JSON, bukan halaman galat wadah servlet.</p>
	 *
	 * <p><b>Penanganan galat:</b> setiap {@link Exception} ditangkap dan diserahkan ke
	 * {@code Common.tampilErrorJikaAdmin}. Konsekuensinya, bila kegagalan terjadi sebelum badan
	 * respons sempat ditulis, bank dapat menerima badan kosong dengan status 200 dan akan
	 * memperlakukannya sebagai kegagalan lalu mengirim ulang.</p>
	 *
	 * @param request  permintaan dari BRI
	 * @param response respons yang akan diisi JSON
	 * @throws ServletException bila wadah servlet melaporkan kegagalan
	 * @throws IOException      bila penulisan respons gagal
	 * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
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
	 * Menangani permintaan HTTP POST dengan mendelegasikannya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p>Inilah metode yang sesungguhnya dipakai BRI untuk kedua endpoint SNAP: penerbitan token
	 * dan notifikasi pembayaran. Isinya sama persis dengan {@link #doGet} — pemilihan cabang tidak
	 * ditentukan oleh metode HTTP maupun oleh URL, melainkan semata oleh ada-tidaknya
	 * {@code grantType} pada body (lihat {@link #doProses}).</p>
	 *
	 * <p><b>Penanganan galat:</b> sama dengan {@link #doGet} — pengecualian ditelan oleh
	 * {@code Common.tampilErrorJikaAdmin} sehingga tidak merambat ke wadah servlet.</p>
	 *
	 * @param request  permintaan dari BRI
	 * @param response respons yang akan diisi JSON
	 * @throws ServletException bila wadah servlet melaporkan kegagalan
	 * @throws IOException      bila penulisan respons gagal
	 * @see HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
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
	 * Inti pemroses transaksi BRIVA: memvalidasi permintaan, memposting atau membatalkan pembayaran
	 * di basis data, lalu merakit objek balasan.
	 *
	 * <p><b>Prasyarat keamanan.</b> Metode ini tidak melakukan otentikasi sendiri. Gerbang token H2H
	 * sudah dievaluasi pemanggilnya, {@link #doProses}, dan hasilnya diterima lewat
	 * {@code tolakTokenGerbang}. Gerbang {@code bankHost} untuk jalur posting dievaluasi di sini
	 * karena hanya berlaku pada payment/reversal, bukan inquiry. Lihat Javadoc kelas untuk sejarah
	 * dan mode bertahap kedua gerbang tersebut.</p>
	 *
	 * <h3>Mode operasi</h3>
	 * <p>Perilaku ditentukan oleh flag {@code inquery} dan {@code reversal}:</p>
	 * <ul>
	 *   <li><b>Inquiry</b> ({@code inquery=true}) — merakit rincian tagihan tanpa menyimpan
	 *       {@code CicilanPembayaran} dan tanpa memanggil {@code updateVa}.</li>
	 *   <li><b>Payment</b> ({@code inquery=false, reversal=false}) — memposting uang: menyimpan
	 *       {@code Kegiatan}, mengurai token cicilan, lalu menutup VA lewat {@code updateVa}.</li>
	 *   <li><b>Reversal</b> ({@code reversal=true}) — membatalkan pembayaran: melepas kaitan
	 *       {@code Kegiatan} dari VA, <b>menghapus seluruh baris {@code cicilan_pembayaran} milik
	 *       VA itu lewat SQL langsung</b>, menghitung ulang total, dan membalas
	 *       {@code errorCode "00"}.</li>
	 * </ul>
	 * <p><b>Catatan:</b> {@link #doProses} selalu meneruskan {@code reversal = false}, sehingga
	 * jalur reversal tidak terjangkau dari kedua {@code url-pattern} servlet ini. Kode tersebut
	 * tetap dipelihara untuk dipakai kembali bila endpoint pembatalan diaktifkan.</p>
	 *
	 * <h3>Rantai penjaga, berurutan</h3>
	 * <ol>
	 *   <li>Gerbang H2H ({@code tolakTokenGerbang}/{@code tolakBankHostGerbang}) bila mode
	 *       {@code aktif}.</li>
	 *   <li>VA tidak ditemukan &rarr; {@code 4003400} "Bad Request".</li>
	 *   <li>VA kadaluarsa &rarr; {@code 4003400}. <b>Penjaga ini dilumpuhkan oleh
	 *       {@code chek}</b> — lihat bagian di bawah.</li>
	 *   <li>Reversal atas VA yang belum pernah terbayar &rarr; {@code 4003400}.</li>
	 *   <li>Tagihan sudah lunas &rarr; {@code 4003400}. <b>Penjaga ini juga dilumpuhkan oleh
	 *       {@code chek}</b>.</li>
	 *   <li>Nominal tidak sama persis dengan {@code totalBiaya()} &rarr; {@code 4003400}.</li>
	 * </ol>
	 *
	 * <h3>PERINGATAN: {@code chek} melumpuhkan dua penjaga</h3>
	 * <p>Parameter {@code chek} muncul sebagai {@code !chek} pada dua tempat, sehingga nilai
	 * {@code true} membuat keduanya tidak pernah aktif:</p>
	 * <ul>
	 *   <li>penjaga <b>VA kadaluarsa</b>, yang bersyarat
	 *       {@code !reversal && !chek && getKadaluarsa().before(now)};</li>
	 *   <li>penjaga <b>tagihan sudah dibayar</b>, karena
	 *       {@link VirtualAccountBank#isSudahTerbayarUntukPayment(VirtualAccountBank, boolean, boolean, boolean)}
	 *       berbunyi {@code !inquery && !reversal && !chek && isSudahTerbayar(...)} sehingga selalu
	 *       mengembalikan {@code false} saat {@code chek} bernilai {@code true}.</li>
	 * </ul>
	 * <p>{@link #process(HttpServletRequest, HttpServletResponse)} memanggil {@link #doProses}
	 * dengan {@code chek = true}, sedangkan gateway sejenis ({@code Bankaltimtara}, {@code BMS},
	 * {@code BSI}, {@code Esmartlink}) semuanya memakai {@code false}. Akibatnya kedua penjaga di
	 * atas tidak aktif pada endpoint notifikasi pembayaran BRI. Keadaan ini telah dilaporkan
	 * sebagai temuan tersendiri untuk ditindaklanjuti; jangan menganggapnya perilaku yang
	 * disengaja.</p>
	 *
	 * <h3>Perlakuan nominal</h3>
	 * <p>Pada mode pembayaran, nominal wajib <b>sama persis</b> dengan
	 * {@code virtualAccountBankNtt.totalBiaya()} (perbandingan {@code intValue()}), sehingga
	 * pembayaran nol maupun negatif tertolak selama tagihannya bernilai. Perlu dicatat bahwa
	 * perbandingannya memakai {@code int} sehingga pecahan rupiah terpotong, dan seluruh blok
	 * pemostingan bersyarat {@code getTotal() > 0.1} — VA bernilai nol tidak masuk cabang mana pun
	 * dan tetap dibalas {@code 2003400 Successful} tanpa ada yang tercatat. Karena nilai tagihan
	 * bukan rahasia, pencocokan ini menghalangi kekeliruan, bukan penyerang.</p>
	 *
	 * <h3>Efek terhadap basis data</h3>
	 * <p>Untuk mahasiswa/calon mahasiswa: {@code Kegiatan} dicari atau dibuat lalu disimpan,
	 * kemudian token pada kolom {@code cicilan} milik VA diurai menjadi baris
	 * {@code CicilanPembayaran}. Empat bentuk token dikenali — id polos, {@code Bulanan-},
	 * {@code Item-}, dan {@code Keranjang-} (didelegasikan ke
	 * {@code PembayaranGatewayHelper.prosesSatuTokenKeranjang}). Setiap baris memakai kunci
	 * {@code ref} deterministik {@code "ntt-<kegiatanId>-<token>-<vaId>"} dan dicari ulang sebelum
	 * ditulis, sehingga <b>pengiriman ulang memperbarui baris yang sama alih-alih
	 * menggandakannya</b> — inilah pengaman utama terhadap pemostingan ganda, mengingat penjaga
	 * kelunasan sedang tidak aktif. Untuk siswa/calon siswa, pemostingan didelegasikan ke
	 * {@code VirtualAccountBank.bayarSiswa}.</p>
	 *
	 * <h3>Transaksi dan pencatatan</h3>
	 * <p>Metode ini membuka {@link Session} Hibernate sendiri dan memakai beberapa transaksi pendek
	 * berturut-turut, bukan satu transaksi menyeluruh — kegagalan di tengah dapat meninggalkan
	 * {@code Kegiatan} tersimpan tanpa seluruh {@code CicilanPembayaran}-nya. Blok {@code catch}
	 * membalas {@code errorCode "05"} untuk reversal atau {@code "91" Link Down} untuk selainnya,
	 * sambil merekam jejak tumpukan. Blok {@code finally} <b>selalu</b> menulis {@link LogHostToHost}
	 * lewat {@code PembayaranGatewayHelper.catatLogHostToHost}, bahkan ketika {@code bankHost}
	 * bernilai {@code null}; pencatatan tanpa syarat itu adalah keputusan arsitektur yang
	 * disengaja, bukan kelalaian.</p>
	 *
	 * @param nominalP           nominal yang dibayarkan dalam rupiah, dari
	 *                           {@code additionalInfo.paymentAmount}; {@code 0.0} bila tidak ada
	 * @param tanggalP           {@code trxDateTime} yang sufiks zona {@code +07:00}-nya sudah
	 *                           dibuang, siap diurai {@link #dateFormat1}
	 * @param va                 nomor VA yang dipakai mencari {@link VirtualAccountBank}
	 * @param bank               label validator yang disimpan pada baris pembayaran; selalu
	 *                           {@code "Briva"} dari {@link #process}
	 * @param bankHost           {@link BankHost} hasil pemetaan IP; boleh {@code null}
	 * @param request            permintaan asli, dipakai untuk pencatatan log H2H
	 * @param data               body JSON mentah, menjadi kerangka objek balasan sekaligus disimpan
	 *                           ke log
	 * @param chekLagi           penanda pemanggilan ulang; selalu {@code true} dari {@link #doProses}
	 * @param inquery            {@code true} untuk penanyaan tagihan, {@code false} untuk pembayaran
	 * @param reversal           {@code true} untuk pembatalan pembayaran; selalu {@code false} dari
	 *                           {@link #doProses}
	 * @param chek               bila {@code true}, penjaga VA kadaluarsa dan penjaga tagihan sudah
	 *                           dibayar <b>tidak aktif</b> — lihat peringatan di atas
	 * @param virtualAccountData objek yang sudah diisi {@link #doProses} dan disematkan ke balasan
	 * @param tolakTokenGerbang  alasan penolakan gerbang token H2H yang SUDAH dievaluasi pemanggil
	 *                           lewat {@link ais.action.ws.util.PembayaranGatewayHelper#periksaGerbangTokenH2h};
	 *                           {@code null} bila lolos (termasuk saat gerbang nonaktif/log).
	 * @return objek JSON balasan berisi {@code responseCode}/{@code errorCode} beserta
	 *         {@code virtualAccountData}
	 * @throws Exception bila perakitan JSON gagal di luar blok yang sudah tertangkap
	 * @see #doProses(String, HttpServletRequest, BankHost, String, boolean)
	 * @see VirtualAccountBank#isSudahTerbayarUntukPayment(VirtualAccountBank, boolean, boolean, boolean)
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject doProcess(double nominalP, String tanggalP, String va, String bank, BankHost bankHost,
			HttpServletRequest request, String data, boolean chekLagi, boolean inquery, boolean reversal, boolean chek,
			JSONObject virtualAccountData, String tolakTokenGerbang) throws Exception {

		System.out.println("chekLagi -> " + chekLagi + ", inquery -> " + inquery + ", reversal -> " + reversal
				+ ", chek -> " + chek);

		JSONObject response = new JSONObject(data);
		response.put("responseCode", "2003400");
		response.put("responseMessage", "Successful");

		response.put("virtualAccountData", virtualAccountData);

		{ // TANPA syarat bankHost: request bank apa pun WAJIB tercatat ke log H2H (lihat finally)
			String nim = "";
			String nama = "";
			JSONArray rincian = new JSONArray();
			// Jejak stack trace bila inquiry/pembayaran error; disimpan ke kolom log H2H.
			String h2hStackTrace = null;

			VirtualAccountBank virtualAccountBankNtt = null;

			// Gerbang akses H2H (lihat javadoc kelas): tolakTokenGerbang sudah dievaluasi
			// pemanggil (null bila lolos/gerbang nonaktif/log); tolakBankHostGerbang dievaluasi
			// di sini karena hanya berlaku pada jalur POSTING (payment/reversal), bukan inquiry.
			String tolakBankHostGerbang = !inquery
					? ais.action.ws.util.PembayaranGatewayHelper.periksaGerbangBankHostPosting(bank,
							"gerbang_bankhost_null_posting_briva", bankHost)
					: null;
			String tolakGerbang = tolakTokenGerbang != null ? tolakTokenGerbang : tolakBankHostGerbang;

			try {

				virtualAccountBankNtt = VirtualAccountBank.ambilVa(va,  nominalP, bankHost);

				if (virtualAccountBankNtt != null && virtualAccountBankNtt.getKadaluarsa() != null) {
					System.out.println(
							"Kadaluara " + Common.databaseDateFormat.get().format(virtualAccountBankNtt.getKadaluarsa()));
				}

				// Gerbang akses H2H: tolak SEBELUM mengungkap detail VA bila mode gerbang sudah
				// diaktifkan operator (lihat javadoc kelas).
				if (tolakGerbang != null) {
					response = new JSONObject();
					response.put("responseCode", "4013400");
					response.put("statusDescription", "Unauthorized. [" + tolakGerbang + "]");
				}

				else if (virtualAccountBankNtt == null) {
					response = new JSONObject();
					response.put("responseCode", "4003400");
					response.put("statusDescription", "Bad Request");
				}

				else if (!reversal && !chek && virtualAccountBankNtt != null
						&& virtualAccountBankNtt.getKadaluarsa().before(WaktuUtil.getDate())) {
					response = new JSONObject();
					response.put("responseCode", "4003400");
					response.put("statusDescription", "Bad Request");

				} else {

					if (reversal && virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1
							&& virtualAccountBankNtt.getKegiatan() == null
							&& virtualAccountBankNtt.getPembayaran() == null) {
						response = new JSONObject();
						response.put("responseCode", "4003400");
						response.put("statusDescription", "Bad Request");
					} else

					if (VirtualAccountBank.isSudahTerbayarUntukPayment(virtualAccountBankNtt, inquery, reversal, chek)) {
						response = new JSONObject();
						response.put("responseCode", "4003400");
						response.put("statusDescription", "Bad Request");
					}

					else if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {

						String TahunID = "";
						try {
							TahunID = virtualAccountBankNtt.getTahunAkademik().split("/")[0];
							response.put("TahunID", TahunID);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:158");
							// TODO: handle exception
						}

						Session session = HibernateUtil.getSessionFactory().openSession();
						try {

							Double nominal = nominalP;

							if (!inquery && nominal.intValue() != virtualAccountBankNtt.totalBiaya()) {
								response = new JSONObject();
								response.put("responseCode", "4003400");
								response.put("statusDescription", "Bad Request");
							} else {

								Date tanggal = ais.ui.util.WaktuUtil.getDate();
								try {
									tanggal = dateFormat1.get().parse(tanggalP);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Briva.java:177");
								}

								if (virtualAccountBankNtt.getSiswa() != null
										|| virtualAccountBankNtt.getCalonSiswa() != null) {

									if (virtualAccountBankNtt.getSiswa() != null) {
										nim = virtualAccountBankNtt.getSiswa().getNomorInduk();
										nama = virtualAccountBankNtt.getSiswa().getNama();

									} else if (virtualAccountBankNtt.getCalonSiswa() != null) {
										nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk();
										nama = virtualAccountBankNtt.getCalonSiswa().getNama();

									}

									VirtualAccountBank.bayarSiswa(virtualAccountBankNtt, session, tanggal, bank,
											inquery, data, false);

								} else {

									JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
									Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
									BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt
											.getBiodataCalonMahasiswa();

									Integer semester = virtualAccountBankNtt.getSemester();

									nim = mahasiswa == null ? biodataCalonMahasiswa.getNoRegistrasi()
											: mahasiswa.getNim();
									nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();

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

									if (reversal) {

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

										virtualAccountBankNtt.setKegiatan(null);

										List<Long> detailBiayasId = new ArrayList<Long>();
										for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(),
												",")) {
											try {
												detailBiayasId.add(Long.parseLong(id.trim()));
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:261");

											}
										}

										Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
												.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
														: Restrictions.in("id", detailBiayasId))
												.list();

										Double nilaiBiayaHarusDiBayars = 0.0;
										for (DetailBiaya detailBiaya : detailBiayas) {

											DetailKegiatan detailKegiatan = kegiatan
													.ambilSatuDetailKegiatan(detailBiaya, session);
											if (detailKegiatan == null) {
												detailKegiatan = new DetailKegiatan();
											}

											Double biaya = detailBiaya.hitungTotalKegiatan(kegiatan, session);

											detailKegiatan.setBiaya(biaya);

											detailKegiatan.setDetailBiaya(detailBiaya);
											detailKegiatan.setKeterangan(detailBiaya.getKeterangan());
											detailKegiatan.setKegiatan(kegiatan);

											nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);

										}

										session.getTransaction().begin();
										session.createSQLQuery("delete from cicilan_pembayaran where ref_va = "
												+ virtualAccountBankNtt.getId()).executeUpdate();
										session.getTransaction().commit();

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
										session.getTransaction().commit();

										VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, null, data, bank);
									} else {

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

										List<Long> detailBiayasId = new ArrayList<Long>();
										for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(),
												",")) {
											try {
												detailBiayasId.add(Long.parseLong(id.trim()));
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:339");

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
										System.out.println("cicilanPembayaran total -> " + total + " totalTagihan "
												+ totalTagihan);

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

														ItemBiaya itemBiaya = pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya();

														Double subtotal = 0.0;
														try {
															String[] spl = idPemBul.split("-");
															subtotal = Double.parseDouble(spl[spl.length - 1]);
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:384");
														}

														if (itemBiaya.getPenghitungan()
																.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
															subtotal = 0.0 - subtotal;
														}

														if (inquery) {

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
															cicilanPembayaran.setItemBiaya(pengaturanPembayaranBulanan
																	.getDetailBiaya().getItemBiaya());
															cicilanPembayaran.setPengaturanPembayaranBulanan(
																	pengaturanPembayaranBulanan);
															cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());
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
															jsonObjectRinci.put("nominal", pengaturanPembayaranBulanan
																	.ambilNominalModifikasi(mahasiswa, semester));

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
														String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
																+ virtualAccountBankNtt.getId();

														ItemBiaya itemBiaya = pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya();
														Double subtotal = 0.0;
														try {
															String[] spl = idPemBul.split("-");
															subtotal = Double.parseDouble(spl[spl.length - 1]);
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:461");
														}

														if (itemBiaya.getPenghitungan()
																.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
															subtotal = 0.0 - subtotal;
														}

														if (inquery) {

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
														String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
																+ virtualAccountBankNtt.getId();
														Double subtotal = 0.0;
														try {
															String[] spl = idPemBul.split("-");
															subtotal = Double.parseDouble(spl[2]);
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:533");
														}

														Long detailBiayaId = null;
														try {
															String[] spl = idPemBul.split("-");
															detailBiayaId = Long.parseLong(spl[4]);
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:540");
														}

														if (itemBiaya.getPenghitungan()
																.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
															subtotal = 0.0 - subtotal;
														}

														if (inquery) {

														} else {

															CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																	.createCriteria(CicilanPembayaran.class)
																	.add(Restrictions.eq("ref", ref)).setMaxResults(1)
																	.uniqueResult();

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
															cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

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
													// menjadi Kegiatan+Cicilan nyata - pemroses terpusat yang sama dengan Esmartlink.
													if (!inquery && !reversal) {
														ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
																virtualAccountBankNtt, false, bank, bankHost, tanggal, data, null);
													}
												}
											}
										}

										if (reversal) {
											response = new JSONObject();
											response.put("errorCode", "00");
											response.put("statusDescription", "Success");
										}

										else if (inquery) {

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

											VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data,
													bank);

										}

									}
								}
							}
						} finally {
							if (session != null) {
								try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:644");}
								try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:645");}
								try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:646");}
							}
						}
					}
				}
			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();

				if (reversal) {
					response = new JSONObject();
					response.put("errorCode", "05");
					response.put("statusDescription", "Reversal gagal");
				} else {

					response.put("errorCode", "91");
					response.put("statusDescription", "Link Down");
				}
				h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
				// (helper: session terdedikasi + commit + retry, tak pernah gagal).
				ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
						response.toString(), nominalP, rincian.toString(), h2hStackTrace);
			}
		}
		return response;
	}

	/**
	 * Pemilah cabang SNAP: menentukan apakah permintaan adalah penerbitan <i>access token</i> atau
	 * notifikasi transaksi, lalu menjalankan otentikasi yang sesuai untuk cabang itu.
	 *
	 * <p>Pemilahan dilakukan semata oleh <b>ada-tidaknya {@code grantType} pada body</b>, bukan
	 * oleh URL. Kedua {@code url-pattern} servlet ini bermuara di metode yang sama, sehingga
	 * permintaan bergrant-type yang dikirim ke URL notifikasi tetap diperlakukan sebagai
	 * penerbitan token, dan sebaliknya.</p>
	 *
	 * <h3>Cabang penerbitan token ({@code grantType != null})</h3>
	 * <ol>
	 *   <li>Tanda tangan {@code SHA256withRSA} atas {@code "<X-Client-Key>|<X-Timestamp>"}
	 *       diverifikasi terhadap kunci publik konfigurasi {@code strPublicKey_bri} (dengan kunci
	 *       bawaan tertanam di kode sebagai cadangan — aman, karena ini kunci <i>publik</i>). Gagal
	 *       berarti {@code 4017300 Unathorized. [Signature]}.</li>
	 *   <li>Kesegaran {@code X-Timestamp} diperiksa lewat gerbang
	 *       {@code gerbang_timestamp_h2h_briva} dengan toleransi
	 *       {@code toleransi_menit_timestamp_h2h_briva}; gagal berarti {@code 4017301}.</li>
	 *   <li>Sebuah {@link UUID} diterbitkan sebagai token, didaftarkan ke simpanan bersama
	 *       {@code PembayaranGatewayHelper.terbitkanTokenH2h} dengan masa berlaku internal dari
	 *       {@code masa_berlaku_token_h2h_briva_menit} (bawaan 15 menit).</li>
	 * </ol>
	 * <p><b>Catatan kontrak:</b> nilai {@code expiresIn} yang dikirim ke bank tetap {@code 1000*15}
	 * dan sengaja <b>tidak</b> disamakan dengan masa berlaku internal di atas, karena angka itu
	 * bagian kontrak SNAP yang sudah disepakati. Perlu disadari bahwa spesifikasi SNAP menyatakan
	 * {@code expiresIn} dalam detik, sehingga 15.000 terbaca sebagai lebih dari empat jam —
	 * ketidakselarasan yang dipertahankan demi kompatibilitas, bukan kekeliruan baru.</p>
	 *
	 * <h3>Cabang transaksi ({@code grantType == null})</h3>
	 * <p>Merakit {@code virtualAccountData} dari field body, membaca header {@code Authorization}
	 * (kedua ejaan huruf besar-kecil), mengevaluasi gerbang token H2H lewat
	 * {@code PembayaranGatewayHelper.periksaGerbangTokenH2h} dengan konfigurasi
	 * {@code gerbang_token_h2h_briva}, lalu meneruskan hasilnya ke {@link #doProcess doProcess}.
	 * Lihat Javadoc kelas untuk riwayat gerbang ini dan arti mode
	 * {@code nonaktif}/{@code log}/{@code aktif}.</p>
	 * <p>Perhatikan bahwa {@code virtualAccountData.paymentStatus} diisi {@code "Success"}
	 * <b>sebelum</b> pemrosesan berlangsung; {@link #doProcess doProcess} akan menimpa
	 * {@code responseCode} bila transaksi ditolak, tetapi field ini sendiri tidak ikut disesuaikan.</p>
	 *
	 * <h3>Perilaku lain</h3>
	 * <ul>
	 *   <li>{@code reversal} dipaku {@code false}, sehingga jalur pembatalan di
	 *       {@link #doProcess doProcess} tidak terjangkau dari servlet ini.</li>
	 *   <li>Nominal diambil dari {@code additionalInfo.paymentAmount}; kegagalan penguraian
	 *       menjadikannya {@code 0.0} secara diam-diam, yang lalu tertolak penjaga pencocokan
	 *       nominal di {@link #doProcess doProcess}.</li>
	 *   <li>Setiap {@link Exception} diterjemahkan menjadi {@link #errorDb} ({@code 91 Link Down}),
	 *       sehingga bank akan mengirim ulang.</li>
	 *   <li>Bila konfigurasi {@code otto_va_sleep} aktif, balasan apa pun <b>ditimpa</b>
	 *       {@link #timeoutDb} setelah jeda tiga detik — sakelar simulasi galat yang harus tetap
	 *       nonaktif di lingkungan produksi.</li>
	 *   <li>Body dan seluruh header dicetak ke {@code System.out}; keluarannya memuat data
	 *       transaksi nasabah sehingga berkas log server harus diperlakukan sebagai data sensitif.</li>
	 * </ul>
	 *
	 * @param data     body JSON mentah dari bank
	 * @param request  permintaan asli, dipakai membaca header dan mencatat log H2H
	 * @param bankHost {@link BankHost} hasil pemetaan IP penelepon; boleh {@code null}
	 * @param bank     label validator, selalu {@code "Briva"} dari {@link #process}
	 * @param chek     diteruskan apa adanya ke {@link #doProcess doProcess}; nilai {@code true}
	 *                 melumpuhkan penjaga VA kadaluarsa dan penjaga tagihan sudah dibayar — lihat
	 *                 peringatan pada {@link #doProcess doProcess}
	 * @return badan respons JSON siap kirim
	 * @throws Exception bila penguraian body atau jeda simulasi gagal
	 * @see #doProcess(double, String, String, String, BankHost, HttpServletRequest, String,
	 *      boolean, boolean, boolean, boolean, JSONObject, String)
	 */
	public static String doProses(String data, HttpServletRequest request, BankHost bankHost, String bank, boolean chek)
			throws Exception {
		JSONObject req = data == null ? null : new JSONObject(data);

		String va = req == null || req.isNull("virtualAccountNo")
				? (request == null ? "" : request.getParameter("virtualAccountNo"))
				: req.getString("virtualAccountNo").trim();

		double nominalP = 0.0;
		try {
			nominalP = req == null || req.isNull("additionalInfo") ? 0.0
					: Double.parseDouble(req.getJSONObject("additionalInfo").get("paymentAmount") + "");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:689");
			// TODO: handle exception
		}

		boolean reversal = false;

		String body;
		try {
			// 05, request tidak diizinkan
			String grantType = req == null || req.isNull("grantType") ? null : req.get("grantType").toString();

			Enumeration<String> header = request.getHeaderNames();
			JSONObject jsonObjectHeader = new JSONObject();
			while (header.hasMoreElements()) {
				String key = header.nextElement();
				jsonObjectHeader.put(key, request.getHeader(key));
			}

			System.out.println("==> VA data => " + data + " request.getRequestURI() " + request.getRequestURI());
			System.out.println("==> VA header => " + jsonObjectHeader);

			String strClientId = jsonObjectHeader.isNull("X-Client-Key") ? null
					: jsonObjectHeader.get("X-Client-Key").toString();

			if (!jsonObjectHeader.isNull("X-CLIENT-KEY")) {
				strClientId = jsonObjectHeader.get("X-CLIENT-KEY").toString();
			}

			if (grantType != null) {

				String strISOTimeStamp = jsonObjectHeader.isNull("X-Timestamp") ? null
						: jsonObjectHeader.get("X-Timestamp").toString();

				if (!jsonObjectHeader.isNull("X-TIMESTAMP")) {
					strISOTimeStamp = jsonObjectHeader.get("X-TIMESTAMP").toString();
				}

				String strToSign = strClientId + "|" + strISOTimeStamp;
				String strPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0zd5K3uxlMHUdsWYalyxwefDycD8DYdiRAGByLcd6Zlgudt1bA3HjuFoE9AeSTxx+ggN3ucr21XbfuVMV0QnuNr6/d/C1U2FRE3MS4Ma1ybT0dYOb5Y6ZkwlMtHJ+3CD7Z33INEQorcd+8pBB/p+0f3Z0lScpQebN+njiWcHG4SoN9B1Tdy3yJFFscAeBiSf8aNvjEoAQM50sM9OHAUz33LCUfu16mu22a+9TkH/1Y5B65kHsOQNMDdYVgdEij4Ayvue2TnUj7asaOPlLvDAIplJBzdLQND6nLuWgCOhDUPcClqY2I6BhsIp6eGpvvQEHGCDmgdzDZcVhVC3lUUyFwIDAQAB";

				strPublicKey = Common.getKonfigurasi("strPublicKey_bri", strPublicKey).getNilai();

				String strSignature = jsonObjectHeader.isNull("X-Signature") ? null
						: jsonObjectHeader.get("X-Signature").toString();

				if (!jsonObjectHeader.isNull("X-SIGNATURE")) {
					strSignature = jsonObjectHeader.get("X-SIGNATURE").toString();
				}

				byte[] signDecode = Base64.getDecoder().decode(strSignature);

				byte[] bPublicKey = Base64.getDecoder().decode(strPublicKey);
				X509EncodedKeySpec specPublic = new X509EncodedKeySpec(bPublicKey);
				KeyFactory kf = KeyFactory.getInstance("RSA");

				Signature privateSignature = Signature.getInstance("SHA256withRSA");
				privateSignature.initVerify(kf.generatePublic(specPublic));
				privateSignature.update(strToSign.getBytes("UTF8"));

				Boolean validateSignature = privateSignature.verify(signDecode);

				System.out.println("==> validateSignature " + validateSignature);

				if (validateSignature) {

					// Kesegaran X-Timestamp (cegah replay tanda tangan lama) -- lihat javadoc kelas;
					// bertahap lewat konfigurasi gerbang_timestamp_h2h_briva, default log-only.
					String tolakTimestamp = ais.action.ws.util.PembayaranGatewayHelper.periksaGerbangTimestampH2h(
							"gerbang_timestamp_h2h_briva", "toleransi_menit_timestamp_h2h_briva", strISOTimeStamp);

					if (tolakTimestamp != null) {

						JSONObject jsonObject = new JSONObject();
						jsonObject.put("responseCode", "4017301");
						jsonObject.put("responseMessage", "Unathorized. [" + tolakTimestamp + "]");

						body = jsonObject.toString();

					} else {

						int expiresIn = 1000 * 15;

						String token = UUID.randomUUID().toString();

						// Masa berlaku INTERNAL token untuk gerbang periksaGerbangTokenH2h -- sengaja
						// TERPISAH dari field "expiresIn" di atas (nilai itu bagian kontrak SNAP yang
						// sudah disepakati bank dan tidak diubah oleh perbaikan ini).
						long masaBerlakuTokenMillis;
						try {
							masaBerlakuTokenMillis = Long.parseLong(
									Common.getKonfigurasi("masa_berlaku_token_h2h_briva_menit", "15").getNilai().trim())
									* 60000L;
						} catch (Exception e) {
							masaBerlakuTokenMillis = 15 * 60000L;
						}
						ais.action.ws.util.PembayaranGatewayHelper.terbitkanTokenH2h("Briva", token,
								masaBerlakuTokenMillis);

						JSONObject jsonObject = new JSONObject();
						jsonObject.put("accessToken", token);
						jsonObject.put("tokenType", "BearerToken");
						jsonObject.put("expiresIn", expiresIn);

						body = jsonObject.toString();
					}
				} else {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("responseCode", "4017300");
					jsonObject.put("responseMessage", "Unathorized. [Signature]");

					body = jsonObject.toString();
				}
			} else {

				JSONObject virtualAccountData = new JSONObject();

				virtualAccountData.put("partnerServiceId", req.get("partnerServiceId"));
				virtualAccountData.put("customerNo", req.get("customerNo"));
				virtualAccountData.put("virtualAccountNo", req.get("virtualAccountNo"));
				virtualAccountData.put("paymentRequestId", req.get("paymentRequestId"));
				virtualAccountData.put("trxDateTime", req.get("trxDateTime"));
				virtualAccountData.put("paymentStatus", "Success");

				String tanggalP = (req.get("trxDateTime") + "").replaceAll("\\+07:00", "");

				System.out.println("==> tanggalP " + tanggalP);

				// Gerbang token H2H pada cabang TRANSAKSI (lihat javadoc kelas): sebelum perbaikan
				// ini, cabang ini tidak pernah memeriksa Authorization/accessTokens sama sekali.
				String authorization = jsonObjectHeader.isNull("Authorization") ? null
						: jsonObjectHeader.get("Authorization").toString();
				if (!jsonObjectHeader.isNull("authorization")) {
					authorization = jsonObjectHeader.get("authorization").toString();
				}
				String tolakTokenGerbang = ais.action.ws.util.PembayaranGatewayHelper.periksaGerbangTokenH2h(bank,
						"gerbang_token_h2h_briva", authorization);

				body = Briva.doProcess(nominalP, tanggalP, va, bank, bankHost, request, data, true, false, reversal,
						chek, virtualAccountData, tolakTokenGerbang).toString();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Briva.java:790");
			body = errorDb(req.isNull("amount"), data);

		}

		if (Common.bolehKonfigurasi("otto_va_sleep", Konfigurasi.TIDAK_AKTIF)) {
			Thread.sleep(3 * 1000);
			body = timeoutDb(req.isNull("amount"), data);
		}

		System.out.println("response->" + body);

		return body;
	}

	/**
	 * Titik masuk tunggal kedua endpoint SNAP BRI: membaca body, memetakan IP penelepon, lalu
	 * menyerahkan pemilahan cabang dan otentikasi ke {@link #doProses}.
	 *
	 * <p>Dipanggil {@link #doGet} maupun {@link #doPost}. Berbeda dengan {@link BCA} yang menegakkan
	 * rantai otentikasi di dalam metode ini sendiri, di sini seluruh keputusan otentikasi berada di
	 * {@link #doProses}; metode ini hanya menyiapkan masukan dan menuliskan hasilnya.</p>
	 *
	 * <h3>Pemetaan {@link BankHost}</h3>
	 * <p>{@link BankHost} dicari dari {@code request.getRemoteAddr()}. Perlu dicatat sebagai hal
	 * positif bahwa kelas ini <b>tidak</b> memercayai header {@code X-Forwarded-For} maupun
	 * {@code CF-Connecting-IP} yang dapat dipalsukan penelepon — berbeda dengan
	 * {@code PembayaranUtil.getBankHost(HttpServletRequest)} yang memang membaca header-header
	 * tersebut dan karenanya tidak dipakai di sini. Hasil {@code null} tidak menghentikan inquiry;
	 * khusus jalur posting, keadaan itu dapat dibuat gagal-tertutup lewat gerbang
	 * {@code gerbang_bankhost_null_posting_briva}.</p>
	 *
	 * <h3>PERINGATAN: argumen {@code chek} yang menyimpang</h3>
	 * <p>Metode ini memanggil {@link #doProses} dengan {@code chek = true}. Semua gateway sejenis
	 * ({@code Bankaltimtara}, {@code BMS}, {@code BSI}, {@code Esmartlink}) memanggil padanan
	 * metodenya dengan {@code false}. Karena {@code chek} muncul sebagai {@code !chek} pada dua
	 * penjaga di {@link #doProcess doProcess}, nilai {@code true} melumpuhkan <b>penjaga VA
	 * kadaluarsa</b> sekaligus <b>penjaga tagihan sudah dibayar</b> pada endpoint notifikasi
	 * pembayaran. Keadaan ini telah dilaporkan sebagai temuan tersendiri; jangan menyalin polanya
	 * ke gateway lain, dan jangan menganggapnya perilaku yang disengaja.</p>
	 *
	 * <h3>Penulisan respons</h3>
	 * <p>Respons dikirim dengan {@code Content-Type: application/json} dan header non-standar
	 * {@code length}. Status HTTP <b>selalu 200</b> — tidak seperti {@link BCA} yang menetapkan
	 * 400/401/404; keberhasilan atau kegagalan disampaikan sepenuhnya lewat {@code responseCode}
	 * atau {@code errorCode} di dalam badan JSON.</p>
	 *
	 * @param request  permintaan dari BRI
	 * @param response respons yang akan diisi JSON
	 * @throws Exception bila pembacaan body atau penulisan respons gagal
	 * @see #doProses(String, HttpServletRequest, BankHost, String, boolean)
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

		String bank = "Briva";
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

		String body = doProses(data, request, bankHost, bank, true);

		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();

		writer.write(body);

	}

	/**
	 * Merakit balasan simulasi <i>timeout</i> ({@code errorCode "68" Connection Timeout}).
	 *
	 * <p>Body permintaan digemakan kembali sebagai kerangka, lalu dua field status ditimpakan di
	 * atasnya. Dipakai <b>hanya</b> oleh {@link #doProses} ketika konfigurasi {@code otto_va_sleep}
	 * aktif: setelah jeda tiga detik, balasan apa pun — termasuk yang menandakan pembayaran
	 * berhasil diposting — akan <b>ditimpa</b> oleh nilai ini.</p>
	 *
	 * <p><b>Peringatan:</b> ini sakelar pengujian ketahanan yang harus tetap nonaktif di
	 * lingkungan produksi. Bila dinyalakan, pembayaran tetap tercatat di basis data sementara bank
	 * menerima jawaban gagal dan akan mengirim ulang, sehingga menimbulkan ketidaksesuaian
	 * setelmen.</p>
	 *
	 * @param inquery <b>tidak dipakai.</b> Parameter ini diabaikan sepenuhnya oleh badan metode;
	 *                ia tetap ada agar tanda tangan seragam dengan {@link #errorDb} dan dengan
	 *                gateway sejenis
	 * @param data    body JSON mentah yang dipakai sebagai kerangka balasan
	 * @return balasan JSON bertanda {@code errorCode "68"}
	 * @throws Exception bila {@code data} bukan JSON yang sah
	 * @see #errorDb(boolean, String)
	 */
	private static String timeoutDb(boolean inquery, String data) throws Exception {

		JSONObject jsonObjectResponse = new JSONObject(data);
		jsonObjectResponse.put("errorCode", "68");
		jsonObjectResponse.put("statusDescription", "Connection Timeout");
		return jsonObjectResponse.toString();
	}

	/**
	 * Merakit balasan galat umum ({@code errorCode "91" Link Down}).
	 *
	 * <p>Body permintaan digemakan kembali sebagai kerangka, lalu dua field status ditimpakan di
	 * atasnya. Dipakai {@link #doProses} sebagai penampung terakhir: setiap {@link Exception} yang
	 * lolos dari pemilahan cabang maupun dari {@link #doProcess doProcess} diterjemahkan menjadi
	 * balasan ini.</p>
	 *
	 * <p>Kode {@code 91} memberi tahu bank bahwa gangguan bersifat sementara, sehingga BRI akan
	 * mengirim ulang notifikasi yang sama. Pengulangan itu aman terhadap penggandaan karena
	 * {@code CicilanPembayaran} memakai kunci {@code ref} deterministik, tetapi perlu diingat bahwa
	 * kegagalan di tengah rangkaian transaksi pendek dapat menyisakan {@code Kegiatan} yang
	 * tersimpan sebagian.</p>
	 *
	 * @param inquery <b>tidak dipakai.</b> Parameter ini diabaikan sepenuhnya oleh badan metode;
	 *                ia tetap ada agar tanda tangan seragam dengan {@link #timeoutDb} dan dengan
	 *                gateway sejenis
	 * @param data    body JSON mentah yang dipakai sebagai kerangka balasan
	 * @return balasan JSON bertanda {@code errorCode "91"}
	 * @throws Exception bila {@code data} bukan JSON yang sah — dalam hal itu galat asli tertutupi
	 *                   oleh galat penguraian ini
	 * @see #timeoutDb(boolean, String)
	 */
	private static String errorDb(boolean inquery, String data) throws Exception {

		JSONObject jsonObjectResponse = new JSONObject(data);
		jsonObjectResponse.put("errorCode", "91");
		jsonObjectResponse.put("statusDescription", "Link Down");
		return jsonObjectResponse.toString();
	}
}

