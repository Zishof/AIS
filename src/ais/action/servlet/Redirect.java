package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet halaman pengalihan generik &mdash; dipetakan ke <code>/redirect</code>.
 *
 * <p><b>Tujuan.</b> Meneruskan (<i>forward</i>) permintaan ke
 * <code>/WEB-INF/u/redirect.jsp</code>, sebuah halaman perantara "Harap tunggu, sedang menyiapkan
 * data ...." yang memakai <i>meta refresh</i>
 * <code>&lt;meta http-equiv="refresh" content="0;URL='&lt;%=request.getParameter("authorizationUrl")
 * %&gt;'" /&gt;</code> untuk langsung mengarahkan peramban ke URL tujuan.</p>
 *
 * <p><b>TEMUAN BARU &mdash; open redirect (dan XSS reflektif) via parameter
 * <code>authorizationUrl</code>.</b> Diverifikasi langsung dari isi {@code redirect.jsp}: nilai
 * parameter disisipkan <b>mentah, tanpa {@code encodeURI}/escaping apa pun dan tanpa daftar putih
 * host</b>, langsung ke dalam atribut {@code content} tag {@code <meta>}. Dua konsekuensi:</p>
 * <ul>
 *   <li><b>Open redirect.</b> <code>/redirect?authorizationUrl=https://situs-jahat.contoh</code>
 *   membuat peramban korban dialihkan ke host sembarang di bawah kendali penyerang &mdash; berguna
 *   untuk phishing (URL awal tetap di domain AIS) atau untuk melewati filter yang hanya memeriksa
 *   domain awal tautan.</li>
 *   <li><b>XSS reflektif.</b> Karena nilai parameter disisipkan ke dalam atribut HTML tanpa
 *   escaping tanda kutip tunggal, nilai seperti <code>x'&gt;&lt;script&gt;...&lt;/script&gt;</code>
 *   dapat memutus atribut {@code content} dan menyisipkan markah/skrip sembarang ke halaman yang
 *   dirender di bawah origin AIS &mdash; bukan sekadar pengalihan, melainkan eksekusi skrip pada
 *   konteks domain AIS.</li>
 * </ul>
 * <p>Berkas {@link Code} pada paket yang sama memiliki pola serupa (open redirect, tanpa XSS)
 * lewat parameter <code>state</code> pada {@code code.jsp} &mdash; lihat Javadoc kelas itu.
 * Kerentanan ini berada pada JSP tujuan (di luar berkas Java ini) dan <b>belum ditambal</b>; kelas
 * {@code Redirect} sendiri hanya mem-<i>forward</i> tanpa membaca parameter apa pun, dan tidak
 * diubah oleh dokumentasi ini.</p>
 *
 * <p><b>Nama kelas menyesatkan.</b> Komentar bawaan generator Eclipse semula berbunyi "Servlet
 * implementation class CheckISBN" &mdash; sisa salin-tempel dari servlet perpustakaan, tidak ada
 * hubungannya dengan fungsi kelas ini.</p>
 *
 * @see Code
 */
public class Redirect extends HttpServlet {

	/**
	 * Nomor versi serialisasi bawaan {@link HttpServlet}.
	 *
	 * <p>Dibiarkan pada nilai {@code 1L} hasil wizard servlet Eclipse. Servlet ini tidak menyimpan
	 * state instance apa pun, sehingga serialisasi/deserialisasi kontainer tidak membawa data yang
	 * perlu dijaga kompatibilitasnya.</p>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diperlukan kontainer servlet.
	 *
	 * <p>Hanya memanggil konstruktor {@link HttpServlet}; tidak ada inisialisasi tambahan. Seluruh
	 * pekerjaan dilakukan per-request di {@link #process(HttpServletRequest, HttpServletResponse)},
	 * sehingga instance servlet tetap tanpa state dan aman dipakai bersama oleh banyak thread.</p>
	 */
	public Redirect() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET &mdash; bentuk pemanggilan lazim untuk halaman pengalihan.
	 *
	 * <p><b>Cara kerja.</b> Melimpahkan seluruh pekerjaan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)} dan menangkap setiap
	 * {@code Exception} untuk diteruskan ke {@link Common#tampilErrorJikaAdmin(Exception)} &mdash;
	 * pesan galat hanya tampil bila pengguna aktif berperan admin, selain itu ditelan diam-diam.</p>
	 *
	 * @param request permintaan HTTP; parameter <code>authorizationUrl</code> dibaca oleh
	 *        <code>redirect.jsp</code> tujuan <i>forward</i>, bukan oleh method ini (lihat catatan
	 *        <i>open redirect</i>/XSS pada Javadoc kelas)
	 * @param response tanggapan HTTP; dipakai sebagai sasaran <i>forward</i> ke
	 *        <code>redirect.jsp</code>
	 * @throws ServletException bila kontainer gagal saat <i>forward</i>
	 * @throws IOException bila terjadi kegagalan I/O pada response
	 * @see #process(HttpServletRequest, HttpServletResponse)
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
	 * Menangani permintaan HTTP POST &mdash; identik dengan
	 * {@link #doGet(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p><b>Cara kerja.</b> Sama persis dengan {@code doGet}: memanggil
	 * {@link #process(HttpServletRequest, HttpServletResponse)} lalu menelan galat lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}.</p>
	 *
	 * @param request permintaan HTTP; parameter <code>authorizationUrl</code> dibaca oleh
	 *        <code>redirect.jsp</code> tujuan <i>forward</i>, bukan oleh method ini
	 * @param response tanggapan HTTP; dipakai sebagai sasaran <i>forward</i> ke
	 *        <code>redirect.jsp</code>
	 * @throws ServletException bila kontainer gagal saat <i>forward</i>
	 * @throws IOException bila terjadi kegagalan I/O pada response
	 * @see #process(HttpServletRequest, HttpServletResponse)
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
	 * Inti servlet: meneruskan permintaan ke JSP halaman pengalihan.
	 *
	 * <p>Satu-satunya pekerjaan method ini adalah
	 * <code>request.getRequestDispatcher("/WEB-INF/u/redirect.jsp").forward(request,
	 * response)</code>. Tidak ada parameter yang dibaca dan tidak ada logika bersyarat di sini;
	 * penyisipan parameter <code>authorizationUrl</code> ke <i>meta refresh</i> (lihat catatan
	 * <i>open redirect</i>/XSS pada Javadoc kelas) sepenuhnya terjadi di dalam {@code redirect.jsp}
	 * yang dituju.</p>
	 *
	 * @param request permintaan HTTP yang diteruskan apa adanya ke <code>redirect.jsp</code>
	 * @param response tanggapan HTTP yang diteruskan apa adanya ke <code>redirect.jsp</code>
	 * @throws Exception bila <i>forward</i> gagal; ditangkap oleh pemanggil
	 *         ({@code doGet}/{@code doPost})
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		request.getRequestDispatcher("/WEB-INF/u/redirect.jsp").forward(request, response);

	}

}
