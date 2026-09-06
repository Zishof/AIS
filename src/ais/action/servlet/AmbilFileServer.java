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
 * Servlet generik pengambil berkas dari disk, dipetakan ke URL {@code /AmbilFileServer}
 * (lihat {@code web.xml}). Berkas dikirim sebagai unduhan ({@code Content-Disposition:
 * attachment}), berbeda dengan {@link F} (path terenkripsi, tampilan {@code inline}).
 *
 * <h4>PERINGATAN KEAMANAN &mdash; path traversal / pembacaan berkas arbitrer, TANPA gerbang
 * otentikasi (diverifikasi dari kode berjalan, 2026-09-07)</h4>
 * <p>Parameter permintaan {@code file} dipakai LANGSUNG (tanpa dekripsi, tanpa normalisasi,
 * tanpa validasi direktori dasar/whitelist ekstensi) sebagai path ke {@link File} yang
 * dibaca dan dikirim ke klien pada {@link #process}: {@code new File(request.getParameter
 * ("file"))}. Tidak ada pemeriksaan bahwa path hasil resolusi berada di dalam direktori
 * tertentu (tidak ada pengecekan {@code ../} maupun pembatasan ke direktori upload/report),
 * dan tidak ada pemeriksaan sesi/login di {@link #doGet}/{@link #doPost}/{@link #process}
 * maupun {@code intercept-url} khusus untuk {@code /AmbilFileServer} pada
 * {@code applicationContext-security.xml} (jatuh ke katalog {@code /**} ber-akses
 * {@code IS_AUTHENTICATED_ANONYMOUSLY}, pola yang sama dengan endpoint anonim lain yang sudah
 * dicatat pada dokumentasi Javadoc menyeluruh). Akibatnya siapa pun tanpa login dapat meminta
 * {@code /AmbilFileServer?file=<path absolut atau relatif apa pun yang dapat dibaca proses
 * server>} untuk MEMBACA berkas APA PUN yang terjangkau izin filesystem proses aplikasi
 * (mis. berkas konfigurasi, kode sumber yang ter-deploy, atau berkas pengguna lain di luar
 * direktori upload), tidak terbatas pada berkas yang dimaksudkan untuk diunduh publik.
 * Ini adalah pola serupa (namun bukan identik &mdash; ini pembacaan file, bukan RCE via
 * upload) dengan risiko yang sudah dicatat pada perbaikan RCE upload JRXML dan direktori
 * {@code /tmp} webapp terbuka publik.
 *
 * @see F
 */
public class AmbilFileServer extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet; tidak melakukan inisialisasi
	 * khusus.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilFileServer() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan ke {@link #process}; kegagalan
	 * ditangkap dan hanya ditampilkan ke administrator lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}, tidak dilempar ke kontainer. Lihat
	 * dokumentasi kelas untuk peringatan keamanan (tidak ada gerbang otentikasi).
	 *
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
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
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Mengambil parameter {@code file} (path berkas MENTAH, tanpa validasi apa pun &mdash;
	 * lihat peringatan keamanan pada dokumentasi kelas) dan menyalin isinya ke {@code resp}
	 * dengan tipe MIME dari nama berkas ({@link ServletContext#getMimeType}) dan header
	 * {@code Content-Disposition: attachment}. Membalas
	 * {@link HttpServletResponse#SC_INTERNAL_SERVER_ERROR} bila parameter {@code file} kosong;
	 * kegagalan pembacaan berkas ditangkap dan diteruskan ke
	 * {@link Common#tampilErrorJikaAdmin(Exception)} tanpa membalas berkas.
	 */
	private void process(HttpServletRequest request, HttpServletResponse resp)
			throws Exception {

		String filename = request.getParameter("file");
		ServletContext sc = getServletContext();
		if (filename == null || filename.trim().equals("")) {
			sc.log("file harus diisi !");
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		try {

			String mimeType = sc==null?null:sc.getMimeType(filename);
			// Set content type
			resp.setContentType(mimeType);
			String headerKey = "Content-Disposition";
			String headerValue = String.format("attachment; filename=\"%s\"", filename);
			resp.setHeader(headerKey, headerValue);

			File file = new File(filename);
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
