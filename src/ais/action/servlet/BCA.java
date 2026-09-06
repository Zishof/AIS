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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
import ais.common.Minify;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;

/**
 * Servlet gateway <b>Host-to-Host (H2H) Bank BCA</b> dengan protokol <b>SNAP</b> (Standar Nasional
 * Open API Pembayaran) untuk kanal <i>Virtual Account</i> (VA).
 *
 * <p>Servlet ini adalah sisi <b>penerima</b>: BCA yang memanggil AIS, bukan sebaliknya. BCA
 * memberitahukan bahwa seorang mahasiswa/siswa membayar tagihan lewat teller/ATM/mobile banking,
 * dan AIS wajib membalas dengan kode respons SNAP serta memposting pembayaran itu ke tabel
 * {@code Kegiatan}/{@code CicilanPembayaran}. Karena balasan servlet inilah yang menentukan apakah
 * tagihan dianggap lunas, <b>seluruh permukaan kelas ini bernilai finansial</b>.</p>
 *
 * <h3>Peta endpoint (lihat {@code web.xml})</h3>
 * <table border="1" summary="Peta url-pattern ke cabang logika">
 *   <tr><th>url-pattern</th><th>Cabang di {@link #process(HttpServletRequest, HttpServletResponse)}</th><th>Arti</th></tr>
 *   <tr><td>{@code /v1.0/access-token/b2b}</td><td>{@code grantType != null}</td>
 *       <td>Penerbitan <i>access token</i> B2B; body memuat {@code grantType=client_credentials}.</td></tr>
 *   <tr><td>{@code /v1.0/transfer-va/inquiry}</td><td>{@code grantType == null}, {@code inquery=true}</td>
 *       <td>Penanyaan tagihan (body tanpa {@code paidAmount}).</td></tr>
 *   <tr><td>{@code /v1.0/transfer-va/payment}</td><td>{@code grantType == null}, {@code inquery=false}</td>
 *       <td>Notifikasi pembayaran (body memuat {@code paidAmount}).</td></tr>
 *   <tr><td>{@code /v1.0/transfer-va/status}</td><td>{@code grantType == null}, {@code status=true}</td>
 *       <td>Penanyaan status transaksi; hanya membaca, tidak memposting.</td></tr>
 * </table>
 *
 * <h3>Rantai otentikasi — lengkap di SEMUA cabang</h3>
 * <p><b>Fakta hasil audit (verifikasi presisi, bukan pembacaan sepintas):</b> berbeda dengan
 * gateway {@code OcbcNisp} dan {@link Briva}, kelas ini <b>benar-benar memverifikasi tanda tangan
 * pada cabang transaksi</b>, bukan hanya pada cabang penerbitan token. Urutannya:</p>
 * <ol>
 *   <li><b>Format {@code X-TIMESTAMP}</b> — {@link #process} baris ~1278; gagal parse membalas
 *       {@code 4007301}. (Catatan: hanya <i>format</i>, bukan kesegaran/skew — lihat batasan.)</li>
 *   <li><b>Cabang token</b>: {@code grantType} harus {@code client_credentials}, {@code X-CLIENT-KEY}
 *       harus sama dengan konfigurasi {@code strClientId_bca}, lalu {@link #sign(String, String)}
 *       memverifikasi tanda tangan <b>RSA SHA256withRSA</b> atas {@code clientId|timestamp}. Baru
 *       setelah itu token diterbitkan dan disimpan ke {@link #accessTokens}.</li>
 *   <li><b>Cabang transaksi</b> (inquiry/payment/status): {@code CHANNEL-ID} dicocokkan ke
 *       {@code channel_id_bca}, {@code X-PARTNER-ID} ke {@code partner_id_bca}, lalu bearer token
 *       dari header {@code Authorization} <b>dibaca kembali dari {@link #accessTokens}</b> (token
 *       tak dikenal = {@code 401 Invalid Token (B2B)}), dan terakhir tanda tangan
 *       <b>HMAC-SHA512</b> atas {@code METHOD:path:token:sha256(body):timestamp} dihitung ulang
 *       lalu dibandingkan dengan {@code X-SIGNATURE}. Tidak cocok = {@code 401 Unauthorized
 *       [Signature]}.</li>
 * </ol>
 * <p>Dengan kata lain {@link #accessTokens} di kelas ini <b>bukan field write-only</b>: ia ditulis di
 * cabang penerbitan dan <b>dibaca sebagai gerbang</b> di cabang transaksi. Inilah pembeda utama
 * terhadap {@link Briva}, yang pada endpoint pembayarannya sama sekali tidak membaca token maupun
 * memverifikasi tanda tangan.</p>
 *
 * <h3>Batasan yang tetap perlu diketahui</h3>
 * <ul>
 *   <li><b>Kesegaran timestamp tidak diperiksa.</b> {@code X-TIMESTAMP} hanya divalidasi formatnya;
 *       tidak ada jendela toleransi. Perlindungan ulang-kirim bertumpu pada {@link #unikId}
 *       (dedupe {@code X-EXTERNAL-ID}) yang hanya di memori dan hilang saat restart.</li>
 *   <li><b>Rahasia HMAC punya nilai bawaan di kode.</b> {@code strClientScret_bca} dibaca lewat
 *       {@code Common.getKonfigurasi} dengan literal bawaan; karena helper itu menuliskan nilai
 *       bawaan ke basis data saat kunci belum ada, instalasi yang tak pernah menggantinya memakai
 *       rahasia yang diketahui publik dari kode sumber.</li>
 *   <li><b>Penyaringan IP bersifat pencatatan, bukan gerbang.</b> {@link BankHost} dicari dari
 *       {@code request.getRemoteAddr()} (bukan header {@code X-Forwarded-For}/{@code CF-Connecting-IP}
 *       yang bisa dipalsukan — ini pilihan yang benar), tetapi {@code bankHost == null} <b>tidak</b>
 *       menghentikan pemrosesan; ia hanya membuat jenis pembayaran jatuh ke {@code TUNAI}.</li>
 *   <li><b>Perbandingan tanda tangan tidak <i>constant-time</i></b> ({@code equalsIgnoreCase}).</li>
 * </ul>
 *
 * <h3>Kontrak balasan</h3>
 * <p>Setiap jalur menghasilkan {@code responseCode} SNAP 7 digit dan {@code responseMessage}. Kode
 * berakhiran {@code 2400}/{@code 2401} dipakai untuk inquiry, {@code 2500}/{@code 2501} untuk
 * payment; pemilihannya dilakukan lewat ternary atas flag {@code inquery} di sepanjang
 * {@link #doProcess doProcess}.</p>
 *
 * @see Briva
 * @see PembayaranUtil
 * @see VirtualAccountBank
 * @see LogHostToHost
 */
public class BCA extends HttpServlet {
	/**
	 * Versi serialisasi {@link java.io.Serializable} yang diwarisi dari {@link HttpServlet}.
	 *
	 * <p>Bernilai tetap {@code 1L}: servlet ini tidak pernah diserialisasi antarnode, sehingga nilai
	 * ini semata memenuhi kontrak {@code Serializable} dan meredam peringatan kompilator.</p>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton utilitas pembayaran; dipakai untuk memetakan alamat IP pemanggil ke
	 * {@link BankHost} lewat {@code getBankHost(String, String)} dan untuk menjumlahkan
	 * cicilan lewat {@code getTotalDanDendaFromCicilan}.
	 *
	 * <p><b>Perhatikan:</b> {@link BankHost} yang dihasilkan bersifat <i>informasional</i> — nilai
	 * {@code null} tidak memblokir transaksi, hanya membuat jenis pembayaran jatuh ke
	 * {@code ConstantValues.TUNAI}. Gerbang keamanan yang sesungguhnya adalah verifikasi tanda
	 * tangan di {@link #process(HttpServletRequest, HttpServletResponse)}.</p>
	 */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * Kumpulan <i>access token</i> B2B yang sedang berlaku, dipetakan dari nilai token ke
	 * {@link Remover} yang mengatur masa hidupnya.
	 *
	 * <p><b>Berbeda dengan gateway sejenis, field ini dibaca sebagai gerbang otorisasi.</b> Alur
	 * pemakaiannya:</p>
	 * <ul>
	 *   <li><b>Tulis</b> — {@link #process} menyimpan token baru setelah verifikasi RSA berhasil.</li>
	 *   <li><b>Baca</b> — {@link #process} mengambil kembali token dari header {@code Authorization}
	 *       pada setiap permintaan inquiry/payment/status; bila tidak ditemukan, permintaan ditolak
	 *       dengan {@code 401 Invalid Token (B2B)}.</li>
	 *   <li><b>Hapus</b> — {@link Remover#run()} membuang token setelah masa berlakunya lewat.</li>
	 * </ul>
	 * <p>Karena bersifat {@code static} dan disimpan di memori proses, seluruh token <b>hangus saat
	 * aplikasi di-restart</b> dan tidak dibagikan antarnode pada penggelaran berklaster; BCA harus
	 * meminta token baru. {@link HashMap} yang dipakai <b>tidak sinkron</b>, sementara penulisan
	 * terjadi dari thread permintaan dan penghapusan dari thread {@link Remover} — sebuah balapan
	 * yang secara teori dapat merusak struktur peta di bawah beban tinggi.</p>
	 */
	private static Map<String, Remover> accessTokens = new HashMap<String, Remover>();

	/**
	 * Pemformat tanggal per-thread untuk pola ISO tanpa zona waktu ({@code yyyy-MM-dd'T'HH:mm:ss}).
	 *
	 * <p>Dibungkus {@link ThreadLocal} karena {@link SimpleDateFormat} tidak aman-thread, sedangkan
	 * servlet melayani banyak permintaan secara paralel. Dipakai untuk dua hal: memvalidasi format
	 * header {@code X-TIMESTAMP} dan mengurai {@code trxDateTime} menjadi tanggal pembukuan. Karena
	 * polanya tidak memuat offset zona, pemanggil lebih dulu membuang sufiks {@code +07:00}.</p>
	 */
	public static final ThreadLocal<DateFormat> dateFormat1 = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		}
	};

	/**
	 * Membentuk instance servlet.
	 *
	 * <p>Konstruktor tanpa argumen yang dibutuhkan wadah servlet: Tomcat membuat satu instance
	 * {@link BCA} lalu memakainya kembali untuk semua permintaan pada keempat {@code url-pattern}
	 * yang dipetakan ke kelas ini. Tidak ada state yang disiapkan di sini — inisialisasi terjadi
	 * pada penginisialisasi field ({@link #pembayaranUtil}, {@link #accessTokens},
	 * {@link #expiresInMinute}).</p>
	 *
	 * <p>Karena instance dipakai bersama oleh banyak thread permintaan, jangan menambahkan field
	 * instance yang dapat berubah dan bergantung pada satu permintaan; pakai variabel lokal atau
	 * {@link ThreadLocal} seperti {@link #dateFormat1}.</p>
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public BCA() {
		super();
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikannya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p>GET dan POST diperlakukan identik: keduanya memanggil pemroses yang sama. Dalam praktik
	 * BCA selalu memakai POST — jalur GET disediakan agar endpoint mudah diuji manual dan agar
	 * permintaan yang salah metode tetap menghasilkan balasan JSON SNAP, bukan halaman galat
	 * wadah servlet.</p>
	 *
	 * <p><b>Penanganan galat:</b> setiap {@link Exception} ditangkap dan diserahkan ke
	 * {@code Common.tampilErrorJikaAdmin}. Konsekuensinya, bila kegagalan terjadi sebelum badan
	 * respons sempat ditulis, klien dapat menerima badan kosong dengan status 200; bank akan
	 * memperlakukannya sebagai kegagalan dan mengirim ulang.</p>
	 *
	 * @param request  permintaan dari BCA
	 * @param response respons yang akan diisi JSON SNAP
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
	 * <p>Inilah metode yang sesungguhnya dipakai BCA untuk keempat endpoint SNAP: penerbitan
	 * token, inquiry, payment, dan status. Isinya sama persis dengan {@link #doGet} — pemilihan
	 * cabang tidak ditentukan oleh metode HTTP melainkan oleh isi body ({@code grantType},
	 * ada-tidaknya {@code paidAmount}) dan oleh akhiran {@code request.getRequestURI()}.</p>
	 *
	 * <p><b>Penanganan galat:</b> sama dengan {@link #doGet} — pengecualian ditelan oleh
	 * {@code Common.tampilErrorJikaAdmin} sehingga tidak merambat ke wadah servlet.</p>
	 *
	 * @param request  permintaan dari BCA
	 * @param response respons yang akan diisi JSON SNAP
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
	 * Pola pendeteksi karakter di luar himpunan aman untuk field identitas SNAP.
	 *
	 * <p>Mencocokkan satu karakter apa pun yang <b>bukan</b> huruf, angka, atau spasi (mode
	 * abai-huruf-besar-kecil). Dipakai di {@link #doProcess doProcess} dengan
	 * {@code p.matcher(x).find()} untuk menolak {@code partnerServiceId}, {@code virtualAccountNo},
	 * dan {@code customerNo} yang memuat karakter aneh; permintaan seperti itu dibalas
	 * {@code Invalid Field Format} ({@code 4002401}/{@code 4002501}).</p>
	 *
	 * <p><b>Catatan:</b> ini adalah penyaring <i>format</i> agar balasan sesuai spesifikasi SNAP,
	 * bukan pertahanan terhadap injeksi — nilai-nilai tersebut tidak pernah disusun menjadi SQL
	 * mentah. {@link Pattern} bersifat aman-thread sehingga boleh dibagikan sebagai field statik,
	 * meskipun {@code Matcher} hasil {@code matcher()} tidak.</p>
	 */
	private static Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);

	@SuppressWarnings("unchecked")
	public static JSONObject doProcess(Double nominalP, String paidAmountReq, String tanggalP, String va,
			String partnerServiceId, String customerNo, String virtualAccountNo, String virtualAccountName,
			String requestId, String xTernal, String trxDateTime, String referenceNo, String bank, BankHost bankHost,
			HttpServletRequest request, HttpServletResponse response, String data, boolean chekLagi, boolean inquery,
			boolean status) throws Exception {

		System.out.println("chekLagi -> " + chekLagi + ", inquery -> " + inquery + ", status -> " + status
				+ ", nominalP -> " + nominalP);

		JSONObject responseUtama = new JSONObject();
		responseUtama.put("responseCode", inquery ? "2002400" : "2002500");
		responseUtama.put("responseMessage", "Successful");

		JSONObject virtualAccountData = new JSONObject();

		if (!inquery) {
			JSONObject paidAmount = new JSONObject();
			paidAmount.put("value", (nominalP == null ? "0" : nominalP.intValue()) + ".00");
			paidAmount.put("currency", "IDR");
			virtualAccountData.put("paidAmount", paidAmount);
		}

		virtualAccountData.put("virtualAccountName", virtualAccountName);
//		virtualAccountData.put("virtualAccountEmail", "");
//		virtualAccountData.put("virtualAccountPhone", "");

		if (inquery) {
//			virtualAccountData.put("virtualAccountTrxType", "");
		}

		JSONObject reason = new JSONObject();

		reason.put("english", "Success");
		reason.put("indonesia", "Sukses");

		virtualAccountData.put("partnerServiceId", partnerServiceId);
		virtualAccountData.put("customerNo", customerNo);
		virtualAccountData.put("virtualAccountNo", virtualAccountNo);

		if (status) {
			virtualAccountData.put("paymentFlagStatus", "00");
			virtualAccountData.put("paymentFlagReason", reason);
		} else if (inquery) {
			virtualAccountData.put("inquiryStatus", "00");
			virtualAccountData.put("inquiryReason", reason);
			virtualAccountData.put("inquiryRequestId", requestId);
		} else {
			virtualAccountData.put("paymentRequestId", requestId);
//			virtualAccountData.put("paidBills", "");
			virtualAccountData.put("trxDateTime", trxDateTime);
//			virtualAccountData.put("journalNum", "");
//			virtualAccountData.put("paymentType", "");
//			virtualAccountData.put("flagAdvise", "N");
			virtualAccountData.put("referenceNo", referenceNo);

			virtualAccountData.put("paymentFlagStatus", "00");
			virtualAccountData.put("paymentFlagReason", reason);
		}

		JSONObject totalAmount = new JSONObject();
		totalAmount.put("value", nominalP.intValue() + ".00");
		totalAmount.put("currency", "IDR");

		virtualAccountData.put("totalAmount", totalAmount);

		if (inquery) {
			virtualAccountData.put("subCompany", "00000");
			responseUtama.put("additionalInfo", new JSONObject());

//			virtualAccountData.put("feeAmount", "null");
		} else {
			responseUtama.put("additionalInfo", new JSONObject());
		}

//		if (!inquery) {
//			virtualAccountData.put("trxId", "");
//		}

		JSONArray billDetailsBaru = new JSONArray();

		virtualAccountData.put("billDetails", billDetailsBaru);

		JSONArray freeTexts = new JSONArray();

		virtualAccountData.put("freeTexts", freeTexts);

		{ // TANPA syarat bankHost: request bank apa pun WAJIB tercatat ke log H2H (lihat finally)
			String nim = "";
			String nama = "";
			String email = "";
			String phone = "";
			String info1 = "";
			String info2 = "";
			Jurusan jurusan = null;
			JSONArray rincian = new JSONArray();
			// Jejak stack trace bila inquiry/pembayaran error; disimpan ke kolom log H2H.
			String h2hStackTrace = null;
			Session session = null;
			VirtualAccountBank virtualAccountBankNtt = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();

				virtualAccountBankNtt = VirtualAccountBank.ambilVa(va, nominalP, bankHost);
				String key = requestId + "_" + xTernal;

				if (virtualAccountBankNtt != null) {

					if (virtualAccountBankNtt.getMahasiswa() != null
							|| virtualAccountBankNtt.getBiodataCalonMahasiswa() != null) {
						Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
						BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt.getBiodataCalonMahasiswa();
						nim = mahasiswa == null ? biodataCalonMahasiswa.getNoRegistrasi() : mahasiswa.getNim();
						nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();
						email = mahasiswa == null ? biodataCalonMahasiswa.getEmail() : mahasiswa.getEmail();
						phone = mahasiswa == null ? biodataCalonMahasiswa.getHp() : mahasiswa.getTelp();

						jurusan = mahasiswa == null ? null : mahasiswa.getJurusan();
						if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getProdiLulus() != null) {
							jurusan = biodataCalonMahasiswa.getProdiLulus();
						} else if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getProdi1() != null) {
							jurusan = biodataCalonMahasiswa.getProdi1();
						}

						JSONObject FreeText = new JSONObject();
						FreeText.put("indonesia", nim);
						FreeText.put("english", nim);
						freeTexts.put(FreeText);

						FreeText = new JSONObject();
						FreeText.put("indonesia", nama);
						FreeText.put("english", nama);
						freeTexts.put(FreeText);

						FreeText = new JSONObject();
						FreeText.put("indonesia", jurusan == null ? "" : jurusan.getNama());
						FreeText.put("english", jurusan == null ? "" : jurusan.getNamaEn());
						freeTexts.put(FreeText);

						FreeText = new JSONObject();
						FreeText.put("indonesia", jurusan == null ? "" : jurusan.getFakultas().getNama());
						FreeText.put("english", jurusan == null ? "" : jurusan.getFakultas().getNamaEn());
						freeTexts.put(FreeText);
					} else {

						if (virtualAccountBankNtt.getSiswa() != null) {
							nim = virtualAccountBankNtt.getSiswa().getNomorInduk();
							nama = virtualAccountBankNtt.getSiswa().getNama();
							email = virtualAccountBankNtt.getSiswa().getAlamatEmail();
							phone = virtualAccountBankNtt.getSiswa().getTeleponSiswa();

							info1 = virtualAccountBankNtt.getSiswa().getSekolah().getNama();
							info2 = virtualAccountBankNtt.getSiswa().getYayasan().getNama();

						} else if (virtualAccountBankNtt.getCalonSiswa() != null) {
							nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk();
							nama = virtualAccountBankNtt.getCalonSiswa().getNama();
							email = virtualAccountBankNtt.getCalonSiswa().getAlamatEmail();
							phone = virtualAccountBankNtt.getCalonSiswa().getTeleponSiswa();

							info1 = virtualAccountBankNtt.getCalonSiswa().getSekolah().getNama();
							info2 = virtualAccountBankNtt.getCalonSiswa().getYayasan().getNama();
						}

						JSONObject FreeText = new JSONObject();
						FreeText.put("indonesia", nim);
						FreeText.put("english", nim);
						freeTexts.put(FreeText);

						FreeText = new JSONObject();
						FreeText.put("indonesia", nama);
						FreeText.put("english", nama);
						freeTexts.put(FreeText);

						FreeText = new JSONObject();
						FreeText.put("indonesia", info1);
						FreeText.put("english", info1);
						freeTexts.put(FreeText);

						FreeText = new JSONObject();
						FreeText.put("indonesia", info2);
						FreeText.put("english", info2);
						freeTexts.put(FreeText);
					}
				}

				if (virtualAccountBankNtt == null) {
					if (inquery) {
						virtualAccountData.put("inquiryStatus", "01");
					} else {
						virtualAccountData.put("paymentFlagStatus", "01");
					}

					reason.put("english", "Bill not found");
					reason.put("indonesia", "Tagihan Tidak Ditemukan");

					responseUtama.put("responseCode", inquery ? "4042412" : "4042512");
					responseUtama.put("responseMessage", "Invalid Bill/Virtual Account [Not Found]");
					response.setStatus(404);
				}

				else if (partnerServiceId != null && p.matcher(partnerServiceId).find()) {
					responseUtama.put("responseCode", !inquery ? "4002501" : "4002401");
					responseUtama.put("responseMessage", "Invalid Field Format partnerServiceId");

					reason.put("english", responseUtama.get("responseMessage"));
					reason.put("indonesia", responseUtama.get("responseMessage"));

					if (inquery) {
						virtualAccountData.put("inquiryStatus", "01");
					} else {
						virtualAccountData.put("paymentFlagStatus", "01");
					}

					response.setStatus(400);
				} else if (virtualAccountNo != null && p.matcher(virtualAccountNo).find()) {
					responseUtama.put("responseCode", !inquery ? "4002501" : "4002401");
					responseUtama.put("responseMessage", "Invalid Field Format virtualAccountNo");

					reason.put("english", responseUtama.get("responseMessage"));
					reason.put("indonesia", responseUtama.get("responseMessage"));

					if (inquery) {
						virtualAccountData.put("inquiryStatus", "01");
					} else {
						virtualAccountData.put("paymentFlagStatus", "01");
					}

					response.setStatus(400);
				} else if (customerNo != null && p.matcher(customerNo).find()) {
					responseUtama.put("responseCode", !inquery ? "4002501" : "4002401");
					responseUtama.put("responseMessage", "Invalid Field Format customerNo");

					reason.put("english", responseUtama.get("responseMessage"));
					reason.put("indonesia", responseUtama.get("responseMessage"));

					if (inquery) {
						virtualAccountData.put("inquiryStatus", "01");
					} else {
						virtualAccountData.put("paymentFlagStatus", "01");
					}

					response.setStatus(400);
				} else if (partnerServiceId == null || partnerServiceId.trim().isEmpty()) {
					responseUtama.put("responseCode", !inquery ? "4002502" : "4002402");
					responseUtama.put("responseMessage", "Invalid Mandatory Field partnerServiceId");

					reason.put("english", responseUtama.get("responseMessage"));
					reason.put("indonesia", responseUtama.get("responseMessage"));

					if (inquery) {
						virtualAccountData.put("inquiryStatus", "01");
					} else {
						virtualAccountData.put("paymentFlagStatus", "01");
					}

					response.setStatus(400);
				} else if (customerNo == null || customerNo.trim().isEmpty()) {
					responseUtama.put("responseCode", !inquery ? "4002502" : "4002402");
					responseUtama.put("responseMessage", "Invalid Mandatory Field customerNo");

					reason.put("english", responseUtama.get("responseMessage"));
					reason.put("indonesia", responseUtama.get("responseMessage"));

					if (inquery) {
						virtualAccountData.put("inquiryStatus", "01");
					} else {
						virtualAccountData.put("paymentFlagStatus", "01");
					}

					response.setStatus(400);
				} else if (virtualAccountNo == null || virtualAccountNo.trim().isEmpty()) {
					responseUtama.put("responseCode", !inquery ? "4002502" : "4002402");
					responseUtama.put("responseMessage", "Invalid Mandatory Field virtualAccountNo");

					reason.put("english", responseUtama.get("responseMessage"));
					reason.put("indonesia", responseUtama.get("responseMessage"));

					if (inquery) {
						virtualAccountData.put("inquiryStatus", "01");
					} else {
						virtualAccountData.put("paymentFlagStatus", "01");
					}

					response.setStatus(400);
				} else if (virtualAccountNo != null && !Common.isNumber(virtualAccountNo)) {
					responseUtama.put("responseCode", !inquery ? "4002501" : "4002401");
					responseUtama.put("responseMessage", "Invalid Field Format virtualAccountNo");
					reason.put("english", responseUtama.get("responseMessage"));
					reason.put("indonesia", responseUtama.get("responseMessage"));

					if (inquery) {
						virtualAccountData.put("inquiryStatus", "01");
					} else {
						virtualAccountData.put("paymentFlagStatus", "01");
					}

					response.setStatus(400);
				}

				else if (customerNo != null && !Common.isNumber(customerNo)) {
					responseUtama.put("responseCode", !inquery ? "4002501" : "4002401");
					responseUtama.put("responseMessage", "Invalid Field Format customerNo");
					reason.put("english", responseUtama.get("responseMessage"));
					reason.put("indonesia", responseUtama.get("responseMessage"));

					if (inquery) {
						virtualAccountData.put("inquiryStatus", "01");
					} else {
						virtualAccountData.put("paymentFlagStatus", "01");
					}

					response.setStatus(400);
				}

				else if (paidAmountReq != null && !paidAmountReq.endsWith(".00")) {
					responseUtama.put("responseCode", "4042513");
					responseUtama.put("responseMessage", "Invalid Amount");
					reason.put("english", responseUtama.get("responseMessage"));
					reason.put("indonesia", responseUtama.get("responseMessage"));

					if (inquery) {
						virtualAccountData.put("inquiryStatus", "01");
					} else {
						virtualAccountData.put("paymentFlagStatus", "01");
					}

					response.setStatus(404);
				}

				else if (virtualAccountNo != null && !Common.isNumber(virtualAccountNo)) {
					responseUtama.put("responseCode", !inquery ? "4002501" : "4002401");
					responseUtama.put("responseMessage", "Invalid Field Format virtualAccountNo");
					reason.put("english", responseUtama.get("responseMessage"));
					reason.put("indonesia", responseUtama.get("responseMessage"));

					if (inquery) {
						virtualAccountData.put("inquiryStatus", "01");
					} else {
						virtualAccountData.put("paymentFlagStatus", "01");
					}

					response.setStatus(400);
				}

				else if (!status && virtualAccountBankNtt != null
						&& VirtualAccountBank.isSudahTerbayar(virtualAccountBankNtt)) {
					reason.put("english", "Bill has been Already Paid");
					reason.put("indonesia", "Tagihan telah dibayar");
					if (inquery) {
						virtualAccountData.put("inquiryStatus", "01");
					} else {
						virtualAccountData.put("paymentFlagStatus", "01");
					}
					responseUtama.put("responseCode", inquery ? "4042414" : "4042514");
					responseUtama.put("responseMessage", "Paid Bill");
					response.setStatus(404);
				}

				else if (!status && virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1
						&& (virtualAccountBankNtt.getKegiatan() != null
								|| virtualAccountBankNtt.getPembayaran() != null)) {

					reason.put("english", "Bill has been Already Paid");
					reason.put("indonesia", "Tagihan telah dibayar");
					if (inquery) {
						virtualAccountData.put("inquiryStatus", "01");
					} else {
						virtualAccountData.put("paymentFlagStatus", "01");
					}
					responseUtama.put("responseCode", inquery ? "4042414" : "4042514");
					responseUtama.put("responseMessage", "Paid Bill");
					response.setStatus(404);

				}

				else if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {

					{

						Date tanggal = ais.ui.util.WaktuUtil.getDate();
						try {
							tanggal = dateFormat1.get().parse(tanggalP);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/BCA.java:483");
						}

						Date msk = virtualAccountBankNtt == null ? null : virtualAccountBankNtt.getKadaluarsaWaktu();
						if (!chekLagi && (tanggal != null && msk != null
								&& Double.parseDouble(Common.dateFormat84.get().format(msk)) < Double
										.parseDouble(Common.dateFormat84.get().format(tanggal)))) {

							responseUtama.put("responseCode", !inquery ? "4042519" : "4042419");
							responseUtama.put("responseMessage", "Invalid Bill/Virtual Account");
							reason.put("english", "Bill expired");
							reason.put("indonesia", "Tagihan kadaluarsa");

							if (inquery) {
								virtualAccountData.put("inquiryStatus", "01");
							} else {
								virtualAccountData.put("paymentFlagStatus", "01");
							}

							response.setStatus(404);

						} else {

							Double nominal = nominalP;

							if (!status && !inquery
									&& nominal.intValue() != virtualAccountBankNtt.totalBiaya()) {

								if (inquery) {
									virtualAccountData.put("inquiryStatus", "01");
								} else {
									virtualAccountData.put("paymentFlagStatus", "01");
								}

								reason.put("english", "Incorrect Billing Value");
								reason.put("indonesia", "Nilai Tagihan Tidak Sesuai");

								responseUtama.put("responseCode", "4042513");
								responseUtama.put("responseMessage", "Invalid Amount");
								response.setStatus(404);
							} else {
								totalAmount.put("value", (virtualAccountBankNtt.getTotal().intValue()
										+ virtualAccountBankNtt.getBiayaAdmin().intValue()) + ".00");

								if (status && virtualAccountBankNtt.getNotif() != null
										&& virtualAccountBankNtt.getNotif().isEmpty()) {
									try {
										JSONObject notif = new JSONObject(virtualAccountBankNtt.getNotif());
										virtualAccountData.put("transactionDate",
												!notif.isNull("trxDateTime") ? notif.get("trxDateTime") + "" : "");
										virtualAccountData.put("trxDateTime",
												!notif.isNull("trxDateTime") ? notif.get("trxDateTime") + "" : "");
										virtualAccountData.put("referenceNo",
												!notif.isNull("referenceNo") ? notif.get("referenceNo") + "" : "");
										virtualAccountData.put("paymentType",
												!notif.isNull("paymentType") ? notif.get("paymentType") + "" : "");
										virtualAccountData.put("flagAdvise",
												!notif.isNull("flagAdvise") ? notif.get("flagAdvise") + "" : "");
//									virtualAccountData.put("paidBills", "");
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/BCA.java:543");
									}
								}

								if (virtualAccountBankNtt.getSiswa() != null
										|| virtualAccountBankNtt.getCalonSiswa() != null) {

									virtualAccountData.put("virtualAccountName", nama);
//								virtualAccountData.put("virtualAccountEmail", email);
//								virtualAccountData.put("virtualAccountPhone", phone);

//								if (inquery) {
//									virtualAccountData.put("virtualAccountTrxType", "C");
//								}

									if (!status) {
										VirtualAccountBank.bayarSiswa(virtualAccountBankNtt, session, tanggal, bank,
												inquery, data, false);
									}
								} else {

									JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
									Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
									BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt
											.getBiodataCalonMahasiswa();

									Integer semester = virtualAccountBankNtt.getSemester();

									virtualAccountData.put("virtualAccountName", nama);
//								virtualAccountData.put("virtualAccountEmail", email);
//								virtualAccountData.put("virtualAccountPhone", phone);
//								if (inquery) {
//									virtualAccountData.put("virtualAccountTrxType", "C");
//								}

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
									for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(), ",")) {
										try {
											detailBiayasId.add(Long.parseLong(id.trim()));
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BCA.java:624");

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
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BCA.java:668");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													String kode = itemBiaya.getKode();
													if (kode != null && kode.length() > 2) {
														kode = kode.substring(kode.length() - 2);
													}

													String billName = itemBiaya.getNama();
													if (billName != null && billName.length() > 20) {
														billName = billName.substring(billName.length() - 20);
													}
													String billShortName = itemBiaya.getNama();
													if (billShortName != null && billShortName.length() > 10) {
														billShortName = billName.substring(billName.length() - 10);
													}

//												JSONObject bill = new JSONObject();
//												bill.put("billCode", kode);
//												bill.put("billNo", itemBiaya.getKode());
//												bill.put("billName", billName);
//												bill.put("billShortName", billShortName);
//
//												JSONObject billDescription = new JSONObject();
//												billDescription.put("Indonesian", itemBiaya.getNama());
//												billDescription.put("English", itemBiaya.getNama());
//
//												bill.put("billDescription", billDescription);
//
//												bill.put("billSubCompany", itemBiaya.getKode());
//
//												JSONObject billAmount = new JSONObject();
//												billAmount.put("value", subtotal.intValue() + ".00");
//												billAmount.put("currency", "IDR");
//
//												bill.put("billAmountLabel", "Total Tagihan");
//												bill.put("billAmountValue",
//														"Rp. " + Common.numberFormat.get().format(subtotal) + ",-");
//
//												bill.put("additionalInfo", new JSONObject());
//
//												billDetails.put(bill);

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
														.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1])))
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
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BCA.java:781");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													String kode = itemBiaya.getKode();
													if (kode != null && kode.length() > 2) {
														kode = kode.substring(kode.length() - 2);
													}

													String billName = itemBiaya.getNama();
													if (billName != null && billName.length() > 20) {
														billName = billName.substring(billName.length() - 20);
													}
													String billShortName = itemBiaya.getNama();
													if (billShortName != null && billShortName.length() > 10) {
														billShortName = billName.substring(billName.length() - 10);
													}

//												JSONObject bill = new JSONObject();
//												bill.put("billCode", kode);
//												bill.put("billNo", itemBiaya.getKode());
//												bill.put("billName", billName);
//												bill.put("billShortName", billShortName);
//
//												JSONObject billDescription = new JSONObject();
//												billDescription.put("Indonesian", itemBiaya.getNama());
//												billDescription.put("English", itemBiaya.getNama());
//
//												bill.put("billDescription", billDescription);
//
//												bill.put("billSubCompany", itemBiaya.getKode());
//
//												JSONObject billAmount = new JSONObject();
//												billAmount.put("value", subtotal.intValue() + ".00");
//												billAmount.put("currency", "IDR");
//
//												bill.put("billAmountLabel", "Total Tagihan");
//												bill.put("billAmountValue",
//														"Rp. " + Common.numberFormat.get().format(subtotal) + ",-");
//
//												bill.put("additionalInfo", new JSONObject());
//
//												billDetails.put(bill);

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
																		.add(Restrictions.idEq(Long
																				.parseLong(idPemBul.split("-")[1]))),
																ItemBiaya.class);

												if (itemBiaya != null) {
													String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
															+ virtualAccountBankNtt.getId();
													Double subtotal = 0.0;
													try {
														String[] spl = idPemBul.split("-");
														subtotal = Double.parseDouble(spl[2]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BCA.java:891");
													}

													Long detailBiayaId = null;
													try {
														String[] spl = idPemBul.split("-");
														detailBiayaId = Long.parseLong(spl[4]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BCA.java:898");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													String kode = itemBiaya.getKode();
													if (kode != null && kode.length() > 2) {
														kode = kode.substring(kode.length() - 2);
													}

													String billName = itemBiaya.getNama();
													if (billName != null && billName.length() > 20) {
														billName = billName.substring(billName.length() - 20);
													}
													String billShortName = itemBiaya.getNama();
													if (billShortName != null && billShortName.length() > 10) {
														billShortName = billName.substring(billName.length() - 10);
													}

//												JSONObject bill = new JSONObject();
//												bill.put("billCode", kode);
//												bill.put("billNo", itemBiaya.getKode());
//												bill.put("billName", billName);
//												bill.put("billShortName", billShortName);
//
//												JSONObject billDescription = new JSONObject();
//												billDescription.put("Indonesian", itemBiaya.getNama());
//												billDescription.put("English", itemBiaya.getNama());
//
//												bill.put("billDescription", billDescription);
//
//												bill.put("billSubCompany", itemBiaya.getKode());
//
//												JSONObject billAmount = new JSONObject();
//												billAmount.put("value", subtotal.intValue() + ".00");
//												billAmount.put("currency", "IDR");
//
//												bill.put("billAmountLabel", "Total Tagihan");
//												bill.put("billAmountValue",
//														"Rp. " + Common.numberFormat.get().format(subtotal) + ",-");
//
//												bill.put("additionalInfo", new JSONObject());
//
//												billDetails.put(bill);

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
												if (!inquery) {
													ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
															virtualAccountBankNtt, false, bank, bankHost, tanggal, data, null);
												}
											}
										}
									}

									if (inquery) {
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
										session.getTransaction().commit();
										if (!status) {
											VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data,
													bank);
										}
									}
								}
							}

						}

					}
				}

				if (xTernal != null && !xTernal.isEmpty() && unikId.containsKey(key)) {

					responseUtama.put("responseCode", !inquery ? "4042518" : "4042512");
					responseUtama.put("responseMessage", "Inconsistent Request");
					response.setStatus(404);

					JSONObject namaD = unikId.get(key);

					if (namaD != null) {
						virtualAccountData = namaD;
					}

				} else if (xTernal != null && !xTernal.isEmpty() && unikId.containsKey(xTernal)) {
					responseUtama.put("responseCode", !inquery ? "4092500" : "4092400");
					responseUtama.put("responseMessage", "Conflict");

					reason.put("english", "Cannot use the same X-EXTERNAL-ID");
					reason.put("indonesia", "Tidak bisa menggunakan X-EXTERNAL-ID yang sama");

					if (inquery) {
						virtualAccountData.put("inquiryStatus", "01");
					} else {
						virtualAccountData.put("paymentFlagStatus", "01");
					}

					response.setStatus(409);

				} else if (xTernal != null && !xTernal.isEmpty()) {
					unikId.put(key, virtualAccountData);
					unikId.put(xTernal, virtualAccountData);
				}

			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();

				if (inquery) {
					virtualAccountData.put("inquiryStatus", "01");
				} else {
					virtualAccountData.put("paymentFlagStatus", "01");
				}

				reason.put("english", "Provider Database Problem");
				reason.put("indonesia", "Ada masalah database");

				responseUtama.put("responseCode", "4002400");
				responseUtama.put("responseMessage", "Bad Request");
				response.setStatus(401);
				h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
				// (helper: session terdedikasi + commit + retry, tak pernah gagal).
				if (session != null) {
					try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/BCA.java:1086");}
					try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/BCA.java:1087");}
					try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/BCA.java:1088");}
				}
				ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
						responseUtama.toString(), nominalP, rincian.toString(), h2hStackTrace);
			}
		}
		responseUtama.put("virtualAccountData", virtualAccountData);
		return responseUtama;
	}

	private int expiresInMinute = 60 * 15;
	private int expiresIn = 1000 * expiresInMinute;

	/**
	 * Tipe implementasi bersarang {@link Remover} milik {@link BCA}. Kelas ini memberi nama pada state atau
	 * perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link BCA} dan dapat mengakses state kelas induk.
	 * Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code long startedTime}, {@code String
	 * token}; operasi lokal: {@code toString()}, {@code setStartedTime()}, {@code run}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see BCA
	 */
	class Remover implements Runnable {

		private long startedTime;
		private String token;

		public String toString() {
			return startedTime + "_" + token;
		}

		public void setStartedTime(long startedTime) {
			this.startedTime = startedTime;
		}

		public Remover(long startedTime, String token) {
			this.startedTime = startedTime;
			this.token = token;
		}

		@Override
		public void run() {
			// remove element from list 2 seconds after adding it
			while ((startedTime + expiresIn) >= System.currentTimeMillis()) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/BCA.java:1127");
				}
			}

			accessTokens.remove(token);
		}

	}

	private boolean sign(String strToSign, String strSignature) {
		boolean validateSignature = false;
		try {

			String strPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2LZkMU+yemb14+eKdwuQKUZMM1fK/XKKZAZDcYQ4dgN6q11IDUCr22txaP7gmnyHpFyJHcu4FEbVbnEJp9w8hCisiJTUsqT79aqzySYDK01j650NQUIDQWPBVBcoL4Cv1fGwd0rLoGaGrzQLekoqEUloXX9e0k2P+JeDzNnldX01+0V1gXq3c4wSYtrpovVbdMHqvj1toFUyZjVN2wUUJaUs2fC/IZvqpw5p5Bwak33J8e5soJ+i/ij7rttZoq+brHbgg5KwmGdIL7jUqLUx1uJZUPOVM+GwvKXau/7VZKCU1XWnYo7NCYyhoIdIXSRjotW3WFvXXQdXrsvQHxHFSwIDAQAB";

			strPublicKey = Common.getKonfigurasi("strPublicKey_bca", strPublicKey).getNilai();

			byte[] signDecode = Base64.getDecoder().decode(strSignature);

			byte[] bPublicKey = Base64.getDecoder().decode(strPublicKey);
			X509EncodedKeySpec specPublic = new X509EncodedKeySpec(bPublicKey);
			KeyFactory kf = KeyFactory.getInstance("RSA");

			Signature privateSignature = Signature.getInstance("SHA256withRSA");
			privateSignature.initVerify(kf.generatePublic(specPublic));
			privateSignature.update(strToSign.getBytes("UTF8"));

			validateSignature = privateSignature.verify(signDecode);

			System.out.println("==> validateSignature " + validateSignature);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/BCA.java:1159");

		}

		return validateSignature;
	}

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

		JSONObject req = data == null ? null : new JSONObject(data);

		String grantType = req == null || req.isNull("grantType") ? null : req.get("grantType").toString();
		boolean inquery = req != null && req.isNull("paidAmount");
		Enumeration<String> header = request.getHeaderNames();
		JSONObject jsonObjectHeader = new JSONObject();
		while (header.hasMoreElements()) {
			String key = header.nextElement();
			jsonObjectHeader.put(key, request.getHeader(key));
		}

		System.out.println("==> VA data => " + data + " request.getRequestURI() " + request.getRequestURI());
		System.out.println("==> VA header => " + jsonObjectHeader);

		String Authorization = jsonObjectHeader.isNull("Authorization") ? null
				: jsonObjectHeader.get("Authorization").toString();

		if (!jsonObjectHeader.isNull("authorization")) {
			Authorization = jsonObjectHeader.get("authorization").toString();
		}

		String strClientId = jsonObjectHeader.isNull("X-Client-Key") ? null
				: jsonObjectHeader.get("X-Client-Key").toString();

		if (!jsonObjectHeader.isNull("X-CLIENT-KEY")) {
			strClientId = jsonObjectHeader.get("X-CLIENT-KEY").toString();
		}

		String strISOTimeStamp = jsonObjectHeader.isNull("X-Timestamp") ? null
				: jsonObjectHeader.get("X-Timestamp").toString();

		if (!jsonObjectHeader.isNull("X-TIMESTAMP")) {
			strISOTimeStamp = jsonObjectHeader.get("X-TIMESTAMP").toString();
		}

		String strSignature = jsonObjectHeader.isNull("X-Signature") ? null
				: jsonObjectHeader.get("X-Signature").toString();

		if (!jsonObjectHeader.isNull("X-SIGNATURE")) {
			strSignature = jsonObjectHeader.get("X-SIGNATURE").toString();
		}

		String xPartner = jsonObjectHeader.isNull("X-PARTNER-ID") ? null
				: jsonObjectHeader.get("X-PARTNER-ID").toString();

		if (!jsonObjectHeader.isNull("X-Partner-Id")) {
			xPartner = jsonObjectHeader.get("X-Partner-Id").toString();
		}

		String partner_id_bca = Common.getKonfigurasi("partner_id_bca", "14275").getNilai();

		String xChannel = jsonObjectHeader.isNull("CHANNEL-ID") ? null : jsonObjectHeader.get("CHANNEL-ID").toString();

		if (!jsonObjectHeader.isNull("Channel-Id")) {
			xChannel = jsonObjectHeader.get("Channel-Id").toString();
		}

		String xTernal = jsonObjectHeader.isNull("X-EXTERNAL-ID") ? null
				: jsonObjectHeader.get("X-EXTERNAL-ID").toString().trim();

		if (!jsonObjectHeader.isNull("X-External-Id")) {
			xTernal = jsonObjectHeader.get("X-External-Id").toString().trim();
		}

		String channel_id_bca = Common.getKonfigurasi("channel_id_bca", "95231").getNilai();

		String body = "";

		String strClientId_bca = Common.getKonfigurasi("strClientId_bca", "68533592-83d5-462f-9335-9283d5a62f2d")
				.getNilai();
		String strClientScret_bca = Common
				.getKonfigurasi("strClientScret_bca", "YQegLaFKEzCU8c1kAefEGWm2scOsoA6LRO_EFZCjGvc").getNilai();

		boolean berhasil = false;
		try {
			dateFormat1.get().parse(strISOTimeStamp.replaceAll("\\+07:00", ""));
			berhasil = true;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BCA.java:1260");
		}

		if (!berhasil) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("responseCode", "4007301");
			jsonObject.put("responseMessage", "Invalid field format [X-TIMESTAMP]");
			response.setStatus(400);
			body = jsonObject.toString();
		} else {

			if (grantType != null) {

				if (!grantType.equalsIgnoreCase("client_credentials")) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("responseCode", "4007301");
					jsonObject.put("responseMessage", "Invalid field format [clientId/clientSecret/grantType]");
					response.setStatus(400);
					body = jsonObject.toString();
				} else {

					if (strClientId == null || !strClientId_bca.trim().equalsIgnoreCase(strClientId.trim())) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("responseCode", "4017300");
						jsonObject.put("responseMessage", "Unauthorized. [Unknown client]");
						response.setStatus(401);
						body = jsonObject.toString();
					} else {
						String strToSign = strClientId + "|" + strISOTimeStamp;
						boolean validateSignature = sign(strToSign, strSignature);
						if (!validateSignature) {
							JSONObject jsonObject = new JSONObject();
							jsonObject.put("responseCode", "4017300");
							jsonObject.put("responseMessage", "Unauthorized. [Signature]");
							response.setStatus(401);
							body = jsonObject.toString();
						} else {

							String token = UUID.randomUUID().toString().trim();

							long startTime = System.currentTimeMillis();

							Remover remover;
							Thread thread = new Thread(remover = new Remover(startTime, token));
							thread.start();

							accessTokens.put(token, remover);
							JSONObject jsonObject = new JSONObject();
							jsonObject.put("accessToken", token);
							jsonObject.put("tokenType", "Bearer");
							jsonObject.put("expiresIn", expiresInMinute);

							jsonObject.put("responseCode", "2007300");
							jsonObject.put("responseMessage", "Successful");

							body = jsonObject.toString();
						}
					}
				}
			} else {

				if (xChannel == null || channel_id_bca == null
						|| !channel_id_bca.trim().equalsIgnoreCase(xChannel.trim())) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("responseCode", inquery ? "4012400" : "4012500");
					jsonObject.put("responseMessage", "Unauthorized. [Unknown client]");
					response.setStatus(401);
					body = jsonObject.toString();
				} else {

					if (xPartner == null || partner_id_bca == null
							|| !partner_id_bca.trim().equalsIgnoreCase(xPartner.trim())) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("responseCode", inquery ? "4012400" : "4012500");
						jsonObject.put("responseMessage", "Unauthorized. [Unknown client]");
						response.setStatus(401);
						body = jsonObject.toString();
					} else {

						try {

							String tokenReq = Authorization == null ? "------------"
									: Authorization.split(" ")[1].trim();

							System.out.println("==> tokenReq " + (tokenReq == null || tokenReq.isEmpty() ? "(kosong)" : "(disamarkan)"));

							Remover remover = accessTokens.get(tokenReq);

							System.out.println("==> tokenReq " + (tokenReq == null || tokenReq.isEmpty() ? "(kosong)" : "(disamarkan)") + ", remover-> " + remover);

							if (remover == null) {
								JSONObject jsonObject = new JSONObject();
								jsonObject.put("responseCode", inquery ? "4012401" : "4012501");
								jsonObject.put("responseMessage", "Invalid Token (B2B)");
								response.setStatus(401);
								body = jsonObject.toString();
							} else {

								remover.setStartedTime(System.currentTimeMillis());

								System.out.println("remover after -> " + remover);

								String minifiedJson = new Minify().minify(data);
								System.out.println("minifiedJson -> " + minifiedJson);

								String sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(minifiedJson);
								sha256hex = sha256hex.toLowerCase();
								String requestPath = request.getRequestURI();
								String httpMethod = "POST";
								String payload = httpMethod + ":" + requestPath + ":" + tokenReq + ":" + sha256hex + ":"
										+ strISOTimeStamp;

								System.out.println("sebelum HAMAC SHA 512 " + payload);

//								String signature = Hashing.hmacSha512(strClientScret_bca.getBytes()).newHasher()
//										.putString(payload, StandardCharsets.UTF_8).hash().toString();

								Mac HmacSHA512 = Mac.getInstance("HmacSHA512");
								SecretKeySpec secret_key = new SecretKeySpec(strClientScret_bca.getBytes(),
										"HmacSHA512");
								HmacSHA512.init(secret_key);

								String signature = org.apache.commons.codec.binary.Base64
										.encodeBase64String(HmacSHA512.doFinal(payload.getBytes()));

								boolean validateSignature = strSignature.equalsIgnoreCase(signature);

								System.out.println("strSignature " + strSignature + " signature " + signature
										+ " validateSignature " + validateSignature);

								if (!validateSignature) {
									JSONObject jsonObject = new JSONObject();
									jsonObject.put("responseCode", inquery ? "4012400" : "4012500");
									jsonObject.put("responseMessage", "Unauthorized. [Signature]");
									response.setStatus(401);
									body = jsonObject.toString();
								} else {

									String va = req == null || req.isNull("virtualAccountNo")
											? (request == null ? "" : request.getParameter("virtualAccountNo"))
											: req.getString("virtualAccountNo").trim();

									double nominalP = 0.0;
									try {
										nominalP = req == null || req.isNull("paidAmount") ? 0.0
												: Double.parseDouble(req.getJSONObject("paidAmount").get("value") + "");
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BCA.java:1406");
										// TODO: handle exception
									}

									String partnerServiceId = req.get("partnerServiceId") + "";
									String customerNo = req.get("customerNo") + "";
									String virtualAccountNo = req.get("virtualAccountNo") + "";
									String virtualAccountName = req.isNull("virtualAccountName") ? ""
											: req.get("virtualAccountName") + "";
									String requestId = !req.isNull("inquiryRequestId")
											? req.get("inquiryRequestId") + ""
											: !req.isNull("paymentRequestId") ? req.get("paymentRequestId") + "" : "";

									String trxDateTime = !req.isNull("trxDateTime") ? req.get("trxDateTime") + ""
											: !req.isNull("trxDateInit") ? req.get("trxDateInit") + "" : "";

									String referenceNo = !req.isNull("referenceNo") ? req.get("referenceNo") + "" : "";

									String bank = "BCA";

									String tanggalP = trxDateTime.replaceAll("\\+07:00", "");

									System.out.println("==> tanggalP " + tanggalP + ", nominalP " + nominalP);

									String paidAmountReq = null;
									try {
										paidAmountReq = req == null || req.isNull("paidAmount") ? null
												: req.getJSONObject("paidAmount").get("value") + "";
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BCA.java:1434");
										// TODO: handle exception
									}

									// 05, request tidak diizinkan
									BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

									body = BCA.doProcess(nominalP, paidAmountReq, tanggalP, va, partnerServiceId,
											customerNo, virtualAccountNo, virtualAccountName, requestId, xTernal,
											trxDateTime, referenceNo, bank, bankHost, request, response, data, false,
											inquery, request.getRequestURI().endsWith("status")).toString();
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/BCA.java:1448");

							JSONObject jsonObject = new JSONObject();
							jsonObject.put("responseCode", inquery ? "4012401" : "4012501");
							jsonObject.put("responseMessage", "Invalid Token (B2B)");
							response.setStatus(401);
							body = jsonObject.toString();
						}
					}

				}
			}

		}

		body = org.apache.commons.lang3.StringUtils.replace(body, "\"null\"", "null");

		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();

		writer.write(body);

	}

	private static Map<String, JSONObject> unikId = new HashMap<String, JSONObject>();

}

