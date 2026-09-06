package ais.action.servlet;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Servlet proksi keluar (outbound forwarder) untuk integrasi Virtual Account BTN, dipetakan
 * ke {@code /BtnForwarder} pada {@code web.xml} tanpa {@code security-constraint} khusus
 * sehingga tunduk pada aturan tangkapan-semua {@code IS_AUTHENTICATED_ANONYMOUSLY} di
 * {@code applicationContext-security.xml} — endpoint ini dapat dipanggil TANPA login.
 *
 * <p>Tujuannya adalah meneruskan permintaan pembuatan/inquiry Virtual Account ke host BTN
 * ({@link #HOST_BTN_DIIZINKAN}) sambil menambahkan header {@code id}/{@code key}/
 * {@code signature} yang menjadi kredensial API BTN. Kredensial BTN yang sebelumnya
 * di-hardcode pada berkas ini sudah ditambal pada revisi terdahulu (lihat
 * {@code Bankaltimtara.java} untuk pola sejenis) sehingga kini datang dari parameter
 * request, bukan konstanta pada source.</p>
 *
 * <h2>FIX (SSRF) &mdash; {@code strURL} kini divalidasi terhadap daftar putih host BTN</h2>
 * <p><b>Sebelumnya</b> {@link #doGet} membaca URL tujuan langsung dari parameter request
 * {@code strURL} dan memakainya apa adanya sebagai argumen {@link HttpPost} tanpa validasi
 * atau daftar putih host apa pun. Karena endpoint ini tidak punya gerbang otentikasi (lihat
 * di atas), akibatnya siapa pun yang dapat menjangkau endpoint ini dapat menyuruh server AIS
 * mengirim HTTP POST ke host/port mana pun yang terjangkau dari jaringan server (termasuk
 * alamat jaringan internal yang seharusnya tidak dapat diakses dari luar) dengan body dan
 * header (id/key/signature) yang juga sepenuhnya dikendalikan pemanggil, lalu membaca
 * kembali responsnya &mdash; pola Server-Side Request Forgery (SSRF) klasik.</p>
 * <p><b>Sekarang</b> {@link #doGet} menolak permintaan (HTTP 403, tanpa memanggil
 * {@link HttpPost} sama sekali) kecuali {@code strURL} diuraikan oleh {@link #isUrlDiizinkan}
 * sebagai URL berskema {@code https} dengan host yang <b>sama persis</b> (case-insensitive)
 * dengan {@link #HOST_BTN_DIIZINKAN}. Perbandingan host memakai {@link java.net.URI#getHost()}
 * (bukan pencocokan string/{@code contains}/{@code endsWith} yang mudah dilewati lewat
 * userinfo palsu seperti {@code https://vabtn.btn.co.id@evil.com/} atau subdomain jebakan
 * seperti {@code https://vabtn.btn.co.id.evil.com/}), sehingga hanya host BTN yang sah yang
 * bisa menjadi tujuan proksi ini apa pun path/port yang diminta pemanggil. Header
 * {@code id}/{@code key}/{@code signature} tetap berasal dari parameter request seperti
 * sebelumnya &mdash; itu di luar cakupan perbaikan ini karena hanya memengaruhi kredensial
 * yang dikirim ke host BTN yang sudah tervalidasi, bukan tujuan koneksi itu sendiri.</p>
 */
public class BtnForwarder extends HttpServlet {
	/** Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} servlet ini. */
	private static final long serialVersionUID = 1L;

	/**
	 * Satu-satunya host tujuan yang boleh dituju oleh proksi ini. Nilai ini adalah konstanta
	 * tetap pada source, TIDAK PERNAH diambil dari parameter/header request, sehingga
	 * pemanggil tidak dapat mengarahkan proksi ke host lain walau parameter {@code strURL}
	 * sepenuhnya dikendalikannya. Lihat catatan keamanan SSRF pada javadoc kelas dan pada
	 * {@link #doGet}.
	 */
	private static final String HOST_BTN_DIIZINKAN = "vabtn.btn.co.id";

	/**
	 * Konstruktor default tanpa argumen, hanya meneruskan ke {@link HttpServlet#HttpServlet()}.
	 * Tidak ada state khusus yang diinisialisasi di sini.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public BtnForwarder() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Membaca seluruh body request sebagai JSON mentah, lalu meneruskannya sebagai HTTP POST
	 * ke URL yang diberikan klien pada parameter {@code strURL} — setelah divalidasi oleh
	 * {@link #isUrlDiizinkan} terhadap daftar putih host BTN, lihat catatan keamanan di bawah
	 * — dengan header {@code id} (dari parameter {@code prefix}), {@code key} (dari parameter
	 * {@code postfix}), dan {@code signature} (dari parameter {@code signature}) — nilai
	 * header-header itu tetap mentah dari request, tanpa validasi. Respons dari host tujuan
	 * dikembalikan apa adanya ke pemanggil dengan {@code Content-Type: application/json}.
	 *
	 * <p><b>Keamanan — gerbang daftar putih host (fix SSRF):</b> endpoint ini tetap tidak
	 * memeriksa sesi atau peran (lihat javadoc kelas ihwal {@code IS_AUTHENTICATED_ANONYMOUSLY}),
	 * tetapi nilai {@code strURL} kini WAJIB lolos {@link #isUrlDiizinkan} sebelum dipakai
	 * sebagai argumen {@link HttpPost}. Bila tidak lolos (null/kosong, tidak bisa diuraikan,
	 * berskema selain {@code https}, atau host-nya bukan persis {@link #HOST_BTN_DIIZINKAN}),
	 * method ini langsung menulis respons HTTP 403 dan <b>tidak pernah</b> memanggil
	 * {@link CloseableHttpClient#execute} — sehingga pemanggil anonim tidak lagi dapat memaksa
	 * server AIS membuka koneksi ke host sembarang (termasuk layanan jaringan internal).
	 * Header otentikasi (id/key/signature) tetap sepenuhnya ditentukan pemanggil seperti
	 * sebelumnya; itu di luar cakupan perbaikan ini karena hanya memengaruhi kredensial yang
	 * dikirim ke host BTN yang sudah tervalidasi, bukan tujuan koneksi.</p>
	 *
	 * @param request  request HTTP masuk; body-nya dibaca utuh sebagai JSON yang diteruskan,
	 *                 dan parameter {@code strURL}/{@code prefix}/{@code postfix}/
	 *                 {@code signature} menentukan tujuan serta header proksi
	 * @param response response HTTP keluar; diisi header {@code Content-Type: application/json}
	 *                 dan badan berupa respons mentah dari host tujuan, atau HTTP 403 dengan
	 *                 badan JSON penolakan bila {@code strURL} tidak lolos {@link #isUrlDiizinkan}
	 * @throws ServletException bila terjadi kegagalan pada lapisan servlet
	 * @throws IOException      bila gagal membaca body request atau menulis respons, atau bila
	 *                          {@link CloseableHttpClient#execute} gagal terhubung ke host tujuan
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String postData = buffer.toString();

//		String postData = "{\"layanan\":\"STMIK Palangkaraya\",\"flag\":\"F\",\"angkatan\":\"2021\",\"jenisbayar\":\"siswa Baru - Heregistrasi (Daftar Ulang)\",\"kodelayanan\":\"113066\",\"description\":\"Jl. Batu Suli No.35\",\"va\":\"946160016691629533\",\"noid\":\"202100149\",\"ref\":\"9D8DF6D5DC\",\"tagihan\":\"5705000\",\"expired\":\"\",\"nama\":\"ANNISA ANGGRAINI\",\"reserve\":\"943\",\"kodejenisbyr\":\"103\"}";
		System.out.println("==> BtnForwarder request => " + postData);

		String strURL = request.getParameter("strURL");
		System.out.println("strURL => " + strURL);

		if (!isUrlDiizinkan(strURL)) {
			System.out.println("BtnForwarder: strURL ditolak, bukan host BTN yang diizinkan => " + strURL);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setHeader("Content-Type", "application/json");
			PrintWriter tolakWriter = response.getWriter();
			tolakWriter.write("{\"status\":\"03\",\"description\":\"URL tujuan tidak diizinkan\"}");
			return;
		}

//		String strURL = "https://vabtn.btn.co.id:9022/v1/stimikpr/createVA";
		String hasil = "";
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {

			HttpPost httpPost = new HttpPost(strURL);

			String prefix = request.getParameter("prefix");// "BSTIMPR";
			String postfix = request.getParameter("postfix"); // "OLVWnHmtrOVnCKzKvnLN0JdIp8uFOtWu";
			String signature = request.getParameter("signature");// "5f8aad4fcca3fb5cefab13e2f57b0591da3d6accca96b266b5c45d14a5460c89";

			StringEntity entity = new StringEntity(postData);
			httpPost.setEntity(entity);
			// httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");
			httpPost.setHeader("id", prefix);
			httpPost.setHeader("key", postfix);

			System.out.println("id => " + prefix);
			System.out.println("key => " + postfix);
			System.out.println("signature => " + signature);

			httpPost.setHeader("signature", signature);

			CloseableHttpResponse r = httpclient.execute(httpPost);

			hasil = EntityUtils.toString(r.getEntity());

		} finally {
			httpclient.close();
		}

		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();
		writer.write(hasil);
	}

	/**
	 * Menangani POST dengan perilaku identik seperti GET: langsung mendelegasikan ke
	 * {@link #doGet(HttpServletRequest, HttpServletResponse)} tanpa logika tambahan, karena
	 * seluruh input (body serta parameter {@code strURL}/{@code prefix}/{@code postfix}/
	 * {@code signature}) diproses dengan cara yang sama pada kedua verb HTTP.
	 *
	 * @param request  request HTTP masuk; diteruskan apa adanya ke {@link #doGet}
	 * @param response response HTTP keluar; diteruskan apa adanya ke {@link #doGet}
	 * @throws ServletException bila {@link #doGet} melempar {@code ServletException}
	 * @throws IOException      bila {@link #doGet} melempar {@code IOException}
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

	/**
	 * Menentukan apakah {@code strURL} boleh dipakai sebagai tujuan {@link HttpPost} oleh
	 * {@link #doGet} — bagian inti dari perbaikan SSRF pada kelas ini.
	 *
	 * <p>Mengembalikan {@code true} hanya bila {@code strURL} tidak kosong, dapat diuraikan
	 * sebagai {@link java.net.URI} yang sah, berskema {@code https} (case-insensitive), dan
	 * {@link java.net.URI#getHost()}-nya sama persis (case-insensitive, tanpa
	 * {@code contains}/{@code endsWith}) dengan {@link #HOST_BTN_DIIZINKAN}. Memakai
	 * {@code getHost()} bawaan {@link java.net.URI} (bukan pencocokan string mentah) penting
	 * agar trik userinfo (mis. {@code https://vabtn.btn.co.id@evil.com/}, yang oleh
	 * {@code URI} diuraikan dengan host {@code evil.com}) dan trik subdomain jebakan (mis.
	 * {@code https://vabtn.btn.co.id.evil.com/}) tetap ditolak.</p>
	 *
	 * @param strURL nilai mentah parameter request {@code strURL}; boleh {@code null}
	 * @return {@code true} bila dan hanya bila {@code strURL} menuju host BTN yang sah lewat
	 *         {@code https}
	 */
	private static boolean isUrlDiizinkan(String strURL) {
		if (strURL == null || strURL.trim().isEmpty()) {
			return false;
		}
		try {
			java.net.URI uri = new java.net.URI(strURL);
			String skema = uri.getScheme();
			String host = uri.getHost();
			return "https".equalsIgnoreCase(skema) && host != null && host.equalsIgnoreCase(HOST_BTN_DIIZINKAN);
		} catch (java.net.URISyntaxException e) {
			return false;
		}
	}

}
