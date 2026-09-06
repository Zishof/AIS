package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet halaman "tautan/berkas tidak ditemukan" &mdash; dipetakan ke <code>/broken</code> DAN
 * didaftarkan sebagai <code>&lt;error-page&gt;</code> global untuk kode status 404 di
 * {@code web.xml}. Artinya kontainer servlet mem-<i>forward</i> ke sini secara otomatis untuk
 * <b>setiap</b> permintaan yang tidak menemukan sumber daya (URL salah, berkas terhapus, dsb.),
 * selain juga dapat dipanggil langsung lewat <code>/broken</code>.
 *
 * <p><b>Tujuan.</b> Meneruskan (<i>forward</i>) permintaan ke <code>/WEB-INF/u/broken.jsp</code>,
 * yang menampilkan pesan "Halaman Tidak Ditemukan" (atau "File Tidak Ditemukan" bila atribut
 * kontainer <code>javax.servlet.error.request_uri</code> berakhiran <code>pdf</code>) beserta ikon
 * yang sesuai.</p>
 *
 * <p><b>Nama kelas menyesatkan.</b> Komentar bawaan generator Eclipse semula berbunyi "Servlet
 * implementation class CheckISBN" &mdash; sisa salin-tempel dari servlet perpustakaan, tidak ada
 * hubungannya dengan fungsi kelas ini.</p>
 */
public class Broken extends HttpServlet {

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
	public Broken() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET &mdash; termasuk pemanggilan otomatis oleh kontainer saat
	 * <code>&lt;error-page&gt;</code> 404 dipicu.
	 *
	 * <p><b>Cara kerja.</b> Melimpahkan seluruh pekerjaan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)} dan menangkap setiap
	 * {@code Exception} untuk diteruskan ke {@link Common#tampilErrorJikaAdmin(Exception)} &mdash;
	 * pesan galat hanya tampil bila pengguna aktif berperan admin, selain itu ditelan diam-diam.</p>
	 *
	 * @param request permintaan HTTP; tidak ada parameter yang dibaca oleh method ini (atribut
	 *        kontainer <code>javax.servlet.error.request_uri</code> dibaca oleh
	 *        <code>broken.jsp</code> tujuan <i>forward</i>)
	 * @param response tanggapan HTTP; dipakai sebagai sasaran <i>forward</i> ke <code>broken.jsp</code>
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
	 * @param request permintaan HTTP; tidak ada parameter yang dibaca oleh method ini
	 * @param response tanggapan HTTP; dipakai sebagai sasaran <i>forward</i> ke <code>broken.jsp</code>
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
	 * Inti servlet: meneruskan permintaan ke JSP halaman "tidak ditemukan".
	 *
	 * <p>Satu-satunya pekerjaan method ini adalah
	 * <code>request.getRequestDispatcher("/WEB-INF/u/broken.jsp").forward(request, response)</code>.
	 * Tidak ada parameter yang dibaca dan tidak ada logika bersyarat di sini; pembedaan pesan
	 * "halaman" versus "berkas PDF" dilakukan sepenuhnya di dalam {@code broken.jsp} berdasarkan
	 * atribut kontainer <code>javax.servlet.error.request_uri</code>.</p>
	 *
	 * @param request permintaan HTTP yang diteruskan apa adanya ke <code>broken.jsp</code>
	 * @param response tanggapan HTTP yang diteruskan apa adanya ke <code>broken.jsp</code>
	 * @throws Exception bila <i>forward</i> gagal; ditangkap oleh pemanggil
	 *         ({@code doGet}/{@code doPost})
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		request.getRequestDispatcher("/WEB-INF/u/broken.jsp").forward(request, response);

	}

}
