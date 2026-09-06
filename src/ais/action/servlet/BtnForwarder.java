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
 * <p>Tujuan aslinya adalah meneruskan permintaan pembuatan/([in]quiry Virtual Account ke
 * host BTN ({@code vabtn.btn.co.id}) sambil menambahkan header {@code id}/{@code key}/
 * {@code signature} yang menjadi kredensial API BTN. Namun implementasi {@link #doGet}
 * membaca URL tujuan langsung dari parameter request {@code strURL} tanpa validasi atau
 * daftar putih host, sehingga fungsinya secara efektif adalah proksi HTTP POST TERBUKA:
 * siapa pun yang dapat menjangkau endpoint ini dapat menyuruh server AIS mengirim POST ke
 * host mana pun (termasuk alamat jaringan internal) dengan body dan header (id/key/
 * signature) yang juga sepenuhnya dikendalikan pemanggil, lalu membaca kembali responsnya.
 * Ini adalah kerentanan Server-Side Request Forgery (SSRF); lihat catatan keamanan pada
 * {@link #doGet} untuk rincian. Kredensial BTN yang sebelumnya di-hardcode pada berkas ini
 * sudah ditambal pada revisi terdahulu (lihat {@code Bankaltimtara.java} untuk pola sejenis)
 * — perbaikan tersebut TIDAK menutup celah SSRF di atas, karena kredensial kini datang dari
 * parameter request, bukan dari konstanta pada source.</p>
 */
public class BtnForwarder extends HttpServlet {
	/** Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} servlet ini. */
	private static final long serialVersionUID = 1L;

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
	 * ke URL yang diberikan klien pada parameter {@code strURL}, dengan header {@code id}
	 * (dari parameter {@code prefix}), {@code key} (dari parameter {@code postfix}), dan
	 * {@code signature} (dari parameter {@code signature}) — semuanya nilai mentah dari
	 * request, tanpa validasi. Respons dari host tujuan dikembalikan apa adanya ke pemanggil
	 * dengan {@code Content-Type: application/json}.
	 *
	 * <p><b>Keamanan — SSRF tanpa gerbang otentikasi:</b> endpoint ini tidak memeriksa sesi,
	 * peran, atau daftar putih host tujuan. Nilai {@code strURL} dipakai langsung sebagai
	 * argumen {@link HttpPost}, sehingga pemanggil anonim dapat memaksa server AIS membuka
	 * koneksi HTTP POST ke host/port mana pun yang terjangkau dari jaringan server (termasuk
	 * layanan internal yang seharusnya tidak dapat diakses dari luar), dengan body dan header
	 * otentikasi (id/key/signature) yang juga sepenuhnya ditentukan pemanggil. Ini adalah pola
	 * SSRF klasik dan berbeda dari isu kredensial hardcode yang sudah ditambal sebelumnya.</p>
	 *
	 * @param request  request HTTP masuk; body-nya dibaca utuh sebagai JSON yang diteruskan,
	 *                 dan parameter {@code strURL}/{@code prefix}/{@code postfix}/
	 *                 {@code signature} menentukan tujuan serta header proksi
	 * @param response response HTTP keluar; diisi header {@code Content-Type: application/json}
	 *                 dan badan berupa respons mentah dari host tujuan
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

}
