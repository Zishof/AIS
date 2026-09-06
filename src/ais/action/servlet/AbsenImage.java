package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

/**
 * Servlet endpoint yang dimaksudkan untuk melayani data terkait foto/gambar absensi pegawai atau
 * siswa (sesuai penamaan kelas), namun implementasinya saat ini adalah STUB: baik {@link #doGet}
 * maupun {@link #doPost} tidak pernah membaca parameter permintaan, tidak melakukan query
 * database, dan tidak pernah memuat berkas foto apa pun — keduanya selalu mengembalikan body JSON
 * kosong ({@code "{}"}) dengan header CORS terbuka penuh ({@code Access-Control-Allow-Origin: *}).
 *
 * <p>
 * <b>Catatan:</b> karena tidak ada logika yang benar-benar mengakses foto absensi, endpoint ini
 * pada kondisi saat ini TIDAK membocorkan PII (foto wajah/identitas) — ia murni kode "belum
 * diimplementasikan" (masih menyisakan komentar {@code TODO Auto-generated method stub} dari
 * template Eclipse) yang selalu menjawab objek kosong. Bila di kemudian hari method ini diisi
 * dengan logika pengambilan foto sungguhan, header CORS wildcard yang sudah terpasang perlu
 * ditinjau ulang agar tidak mengizinkan domain mana pun membaca foto absensi lintas-origin tanpa
 * otentikasi.
 * </p>
 *
 * @see HttpServlet
 */
public class AbsenImage extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Konstruktor baku servlet, tanpa inisialisasi tambahan. */
	public AbsenImage() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Selalu menuliskan body JSON kosong ({@code "{}"}) ke response, disertai header
	 * {@code length} (panjang body), {@code Content-Type: application/json}, dan
	 * {@code Access-Control-Allow-Origin: *} (CORS terbuka untuk semua origin). Tidak membaca
	 * parameter permintaan maupun mengakses database — lihat catatan stub pada javadoc kelas.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub

		JSONObject jsonObject = new JSONObject();
		String body = jsonObject.toString();
		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		PrintWriter writer = response.getWriter();

		writer.write(body);
	}

	/**
	 * Menangani permintaan POST dengan perilaku identik dengan GET, yaitu mendelegasikan
	 * langsung ke {@link #doGet(HttpServletRequest, HttpServletResponse)}.
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
