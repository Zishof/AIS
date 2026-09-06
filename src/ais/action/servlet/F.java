package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet generik pengambil berkas dari disk berdasarkan path TERENKRIPSI, dipetakan ke
 * URL {@code /F} (nama kelas sangat pendek — bukan alias/shortcut untuk kelas lain, melainkan
 * servlet mandiri; lihat {@code web.xml} untuk pemetaan URL-nya). Parameter permintaan
 * {@code p} WAJIB berupa path berkas yang sudah dienkripsi lewat {@link Common#desEncrypter}
 * (skema enkripsi simetris yang sama dipakai di berbagai tautan unduhan/tampil berkas pada
 * AIS, mis. tautan foto/lampiran yang dibangun sisi server) — path didekripsi di {@link #process}
 * sebelum dipakai membentuk {@link File}. Berbeda dengan {@link AmbilFileServer} (yang menerima
 * path MENTAH dari klien tanpa enkripsi), servlet ini hanya bisa menyasar berkas yang pathnya
 * memang sengaja dienkripsi dan diedarkan oleh aplikasi sendiri — namun tetap TIDAK ada
 * pemeriksaan sesi/login di {@link #doGet}/{@link #doPost}/{@link #process}, dan tidak ada
 * validasi bahwa path hasil dekripsi berada di dalam direktori yang diizinkan (tidak ada
 * pengecekan traversal {@code ../} pada path hasil dekripsi). Berkas yang ditemukan dikirim
 * dengan header {@code Content-Disposition: inline} (ditampilkan langsung di browser, bukan
 * diunduh sebagai attachment seperti {@link AmbilFileServer}).
 */
public class F extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet; tidak melakukan inisialisasi
	 * khusus.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public F() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan ke {@link #process}; kegagalan apa
	 * pun (termasuk kegagalan dekripsi path) ditangkap dan hanya ditampilkan ke administrator
	 * lewat {@link Common#tampilErrorJikaAdmin(Exception)}, tidak dilempar ke kontainer.
	 *
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
	 * Menangani permintaan HTTP POST dengan perilaku identik {@link #doGet}.
	 *
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
	 * Mengambil parameter {@code p} (path berkas terenkripsi), mendekripsinya lewat
	 * {@link Common#desEncrypter}, lalu menyalin isi berkas hasil dekripsi ke {@code resp}
	 * dengan tipe MIME yang ditentukan dari nama berkas ({@link ServletContext#getMimeType})
	 * dan header {@code Content-Disposition: inline}. Membalas
	 * {@link HttpServletResponse#SC_INTERNAL_SERVER_ERROR} bila parameter {@code p} kosong;
	 * kegagalan dekripsi/pembacaan berkas ditangkap dan diteruskan ke
	 * {@link Common#tampilErrorJikaAdmin(Exception)} tanpa membalas berkas.
	 */
	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {

		String filename = request.getParameter("p");
		ServletContext sc = getServletContext();
		if (filename == null || filename.trim().equals("")) {
			sc.log("file harus diisi !");
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		try {
			filename = Common.desEncrypter.get().decrypt(filename);
			File file = new File(filename);
			String mimeType = sc.getMimeType(file.getName());
			// Set content type
			resp.setContentType(mimeType);
			String headerKey = "Content-Disposition";
			String headerValue = String.format("inline; filename=\"%s\"", file.getName());
			resp.setHeader(headerKey, headerValue);

			resp.setContentLength((int) file.length());
			// Open the file and output streams
			FileInputStream in = new FileInputStream(file);
			OutputStream out = resp.getOutputStream();
			byte[] buf = new byte[1024];
			int count = 0;
			while ((count = in.read(buf)) >= 0) {
				out.write(buf, 0, count);
			}
			in.close();
			out.close();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

}
