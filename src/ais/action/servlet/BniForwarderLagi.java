package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet forwarder kembaran {@code BniForwarder}: meneruskan mentah-mentah (tanpa modifikasi)
 * body request masuk lewat proses {@code curl} eksternal ke sebuah host relay tetap
 * ({@code http://34.101.225.237/f/BniForwarder}) — bukan langsung ke API BNI eCollection —
 * lalu mengembalikan respons {@code curl} tersebut apa adanya ke pemanggil. Nama "Lagi"
 * ("lagi"/"lainnya") menandakan ini adalah forward TAMBAHAN/kedua: request yang sama diteruskan
 * lagi ke relay {@code 34.101.225.237} yang pada gilirannya (di luar kendali kode ini) diduga
 * meneruskannya sekali lagi ke BNI, sebagai jalur cadangan/duplikat dari {@code BniForwarder}.
 *
 * <p><b>Bandingkan dengan {@code BtnForwarder}:</b> nama kelas ini mirip {@code BtnForwarder}
 * (yang pernah mengandung kerentanan SSRF nyata karena URL tujuan forward diambil dari parameter
 * request {@code strURL} tanpa validasi apa pun, sudah ditambal). Kelas ini TIDAK memiliki pola
 * yang sama: {@code strURL} pada {@link #doProses} adalah literal string tetap
 * ({@code http://34.101.225.237/f/BniForwarder}), bukan nilai yang diambil dari
 * {@code request}/parameter mana pun — sehingga pemanggil tidak dapat mengarahkan forward ini ke
 * host lain. Lihat javadoc kembarannya, {@code BniForwarder} (target berbeda:
 * {@code https://api.bni-ecollection.com/}), untuk pola yang identik.</p>
 *
 * <p><b>Catatan lain (bukan SSRF, di luar cakupan pemeriksaan ini):</b> {@link #doProses}
 * memanggil biner {@code curl} eksternal lewat {@link ProcessBuilder} dengan array argumen
 * (bukan lewat shell), sehingga {@code postData} yang disisipkan sebagai argumen {@code --data}
 * TIDAK rentan command injection meski isinya sepenuhnya dikendalikan pemanggil; namun flag
 * {@code -k} (curl {@code --insecure}) menonaktifkan verifikasi sertifikat TLS, host relay tetap
 * ini dituju lewat {@code http://} biasa (bukan HTTPS) sehingga isi {@code postData} berjalan
 * tanpa enkripsi transport sampai ke relay, dan endpoint ini (seperti kebanyakan servlet
 * notifikasi/forwarder pembayaran di paket ini) tidak memiliki gerbang otentikasi/otorisasi
 * sendiri di level servlet.</p>
 *
 * <p>Javadoc kelas ini semula adalah stub generator Eclipse usang ("Servlet implementation
 * class CheckISBN") tersalin-tempel dari templat lama — lihat javadoc {@code CheckISBN} untuk
 * konfirmasi bahwa file aslinya sendiri sudah didokumentasikan penuh, bukan stub.</p>
 */
public class BniForwarderLagi extends HttpServlet {
	/** Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} servlet ini. */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor default tanpa argumen, hanya meneruskan ke {@link HttpServlet#HttpServlet()}.
	 * Tidak ada state khusus yang diinisialisasi di sini.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public BniForwarderLagi() {
		super();
	}

	/**
	 * Menangani GET dengan mendelegasikan ke {@link #doProses}; kegagalan apa pun ditelan dan
	 * hanya ditampilkan ke pengguna bila konteks saat ini adalah administrator, lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param request  request HTTP masuk; body-nya diteruskan mentah ke host relay oleh
	 *                 {@link #doProses}
	 * @param response response HTTP keluar; badan diisi respons {@code curl} oleh
	 *                 {@link #doProses}
	 * @throws ServletException tidak pernah dilempar keluar karena {@link #doProses} dibungkus
	 *                          try/catch di sini; dipertahankan hanya karena tanda tangan
	 *                          {@link HttpServlet#doGet}
	 * @throws IOException      idem, ditelan oleh blok catch
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			doProses(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menangani POST dengan perilaku identik seperti {@link #doGet}: mendelegasikan ke
	 * {@link #doProses} dan menelan kegagalan lewat {@link Common#tampilErrorJikaAdmin(Exception)}.
	 * Notifikasi/panggilan forwarder ini pada praktiknya dikirim sebagai POST, tetapi kedua verb
	 * didukung.
	 *
	 * @param request  request HTTP masuk; body-nya diteruskan mentah ke host relay oleh
	 *                 {@link #doProses}
	 * @param response response HTTP keluar; badan diisi respons {@code curl} oleh
	 *                 {@link #doProses}
	 * @throws ServletException tidak pernah dilempar keluar, lihat catatan pada {@link #doGet}
	 * @throws IOException      idem
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			doProses(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membaca seluruh body request apa adanya, meneruskannya sebagai body POST JSON ke host relay
	 * tetap {@code http://34.101.225.237/f/BniForwarder} lewat proses {@code curl} eksternal
	 * ({@code curl -k -H "Content-Type: application/json" -X POST <url> --data <postData>}),
	 * lalu menyalin stdout proses tersebut apa adanya sebagai respons {@code application/json}
	 * ke pemanggil.
	 *
	 * <p>{@code strURL} adalah literal tetap di dalam method ini (BUKAN diambil dari parameter
	 * request), sehingga tujuan forward tidak dapat diarahkan ulang oleh pemanggil endpoint ini
	 * — lihat catatan SSRF pada Javadoc kelas untuk perbandingan dengan {@code BtnForwarder}.
	 * {@code postData} disisipkan sebagai argumen array terpisah pada {@link ProcessBuilder}
	 * (bukan lewat shell), sehingga aman dari command injection walau isinya bebas dikendalikan
	 * pemanggil.</p>
	 *
	 * <p>Kegagalan menjalankan/membaca proses {@code curl} ditelan (stack trace dicetak dan
	 * dicatat ke {@link ais.common.ErrorAuditUtil}) dan menghasilkan {@code hasil} berupa string
	 * kosong yang tetap dibalas dengan status 200 — pemanggil tidak menerima kode error HTTP
	 * eksplisit saat forward ke relay gagal.</p>
	 *
	 * @param request  request HTTP masuk; seluruh body dibaca utuh dan diteruskan tanpa
	 *                 modifikasi sebagai {@code postData}
	 * @param response response HTTP keluar; diisi header {@code Content-Type: application/json}
	 *                 dan badan berupa stdout proses {@code curl} (atau string kosong bila proses
	 *                 gagal)
	 * @throws Exception bila pembacaan body request atau penulisan respons gagal (kegagalan
	 *                    proses {@code curl} sendiri ditelan secara internal, lihat di atas)
	 */
	public static void doProses(HttpServletRequest request, HttpServletResponse response) throws Exception {
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String postData = buffer.toString();
		System.out.println("==> BniForwarder request => " + postData);

		String strURL = "http://34.101.225.237/f/BniForwarder";

		String hasil = "";
		try {

			String[] command = { "curl", "-k", "-H", "Content-Type: application/json", "-X", "POST", strURL, "--data",
					postData.toString() };

			ProcessBuilder process = new ProcessBuilder(command);
			Process p;
			p = process.start();
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			hasil = builder.toString();

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/BniForwarderLagi.java:85");
		}

		System.out.println("==> BniForwarder response => " + hasil);

		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();
		writer.write(hasil);
	}

}
