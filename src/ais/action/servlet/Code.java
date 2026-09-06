package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet penampung callback OAuth Google Drive alur "state" &mdash; dipetakan ke
 * <code>/code</code>.
 *
 * <p><b>Tujuan.</b> Meneruskan (<i>forward</i>) permintaan ke <code>/WEB-INF/u/code.jsp</code>.
 * JSP tersebut menampilkan kotak "Copy / salin kode" berisi parameter <code>code</code> untuk
 * disalin manual oleh pengguna &mdash; <b>kecuali</b> bila parameter <code>state</code> ikut
 * dikirim, dalam hal ini {@code code.jsp} langsung memanggil
 * <code>response.sendRedirect(request.getParameter("state") + "&amp;code=" +
 * request.getParameter("code"))</code> tanpa menampilkan apa pun.</p>
 *
 * <p><b>TEMUAN BARU &mdash; open redirect via parameter <code>state</code>.</b> Diverifikasi
 * langsung dari isi {@code code.jsp}: nilai <code>state</code> dipakai <b>mentah, tanpa validasi
 * atau daftar putih apa pun</b>, sebagai awal URL tujuan {@code sendRedirect}. Karena {@code state}
 * dan {@code code} sama-sama berasal dari <code>request.getParameter(...)</code>, siapa pun dapat
 * memanggil <code>/code?state=https://situs-jahat.contoh&amp;code=apa+saja</code> dan pengguna yang
 * mengeklik tautan itu akan menerima HTTP 302 menuju host sembarang &mdash; klasik <i>open
 * redirect</i>, dan berpotensi disalahgunakan sebagai halaman phishing "resmi" AIS (URL awal berada
 * di domain AIS, tetapi berakhir di situs penyerang) atau untuk membocorkan token OAuth
 * ({@code code}) ke domain pihak ketiga lewat query string tujuan redirect. Berkas {@link Redirect}
 * pada paket yang sama memiliki pola identik lewat parameter <code>authorizationUrl</code> pada
 * {@code redirect.jsp} &mdash; lihat catatan lengkap pada Javadoc kelas itu. Kerentanan ini berada
 * pada JSP tujuan (di luar berkas Java ini) dan <b>belum ditambal</b>; kelas {@code Code} sendiri
 * tidak diubah oleh dokumentasi ini.</p>
 *
 * <p><b>Nama kelas menyesatkan.</b> Komentar bawaan generator Eclipse semula berbunyi "Servlet
 * implementation class CheckISBN" &mdash; sisa salin-tempel dari servlet perpustakaan, tidak ada
 * hubungannya dengan fungsi kelas ini.</p>
 *
 * @see Redirect
 */
public class Code extends HttpServlet {

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
	public Code() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET &mdash; bentuk pemanggilan lazim untuk callback OAuth Google
	 * Drive (redirect dari halaman izin Google, membawa parameter <code>code</code> dan/atau
	 * <code>state</code>).
	 *
	 * <p><b>Cara kerja.</b> Melimpahkan seluruh pekerjaan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)} dan menangkap setiap
	 * {@code Exception} untuk diteruskan ke {@link Common#tampilErrorJikaAdmin(Exception)} &mdash;
	 * pesan galat hanya tampil bila pengguna aktif berperan admin, selain itu ditelan diam-diam.</p>
	 *
	 * @param request permintaan HTTP; parameter <code>code</code> dan <code>state</code> dibaca oleh
	 *        <code>code.jsp</code> tujuan <i>forward</i>, bukan oleh method ini (lihat catatan
	 *        <i>open redirect</i> pada Javadoc kelas)
	 * @param response tanggapan HTTP; dipakai sebagai sasaran <i>forward</i> ke <code>code.jsp</code>
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
	 * @param request permintaan HTTP; parameter <code>code</code> dan <code>state</code> dibaca oleh
	 *        <code>code.jsp</code> tujuan <i>forward</i>, bukan oleh method ini
	 * @param response tanggapan HTTP; dipakai sebagai sasaran <i>forward</i> ke <code>code.jsp</code>
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
	 * Inti servlet: meneruskan permintaan ke JSP penerima callback OAuth Google Drive.
	 *
	 * <p>Satu-satunya pekerjaan method ini adalah
	 * <code>request.getRequestDispatcher("/WEB-INF/u/code.jsp").forward(request, response)</code>.
	 * Tidak ada parameter yang dibaca dan tidak ada logika bersyarat di sini; percabangan
	 * "tampilkan kode untuk disalin" versus "redirect memakai <code>state</code>" (lihat catatan
	 * <i>open redirect</i> pada Javadoc kelas) sepenuhnya terjadi di dalam {@code code.jsp} yang
	 * dituju.</p>
	 *
	 * @param request permintaan HTTP yang diteruskan apa adanya ke <code>code.jsp</code>
	 * @param response tanggapan HTTP yang diteruskan apa adanya ke <code>code.jsp</code>
	 * @throws Exception bila <i>forward</i> gagal; ditangkap oleh pemanggil
	 *         ({@code doGet}/{@code doPost})
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		request.getRequestDispatcher("/WEB-INF/u/code.jsp").forward(request, response);

	}

}
