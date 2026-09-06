package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet pembuka layar pemeliharaan grup pengguna (job) &mdash; dipetakan ke
 * <code>/grupPengguna</code>.
 *
 * <p><b>Tujuan.</b> Servlet ini hanya meneruskan (<i>forward</i>) permintaan apa pun ke satu ZUL
 * tetap, <code>/WEB-INF/z/x/y/pages/maintenance/job/list.zul</code> (daftar "job"/grup pengguna
 * pada modul pemeliharaan), tanpa membaca parameter permintaan apa pun dan tanpa menyentuh sesi
 * HTTP. Pengisian daftar dan pemeriksaan hak akses atas layar tersebut sepenuhnya menjadi
 * tanggung jawab komponen ZK di dalam ZUL itu sendiri.</p>
 *
 * <p><b>Nama kelas menyesatkan.</b> Komentar bawaan generator Eclipse semula berbunyi "Servlet
 * implementation class CheckISBN" &mdash; sisa salin-tempel dari servlet perpustakaan, tidak ada
 * hubungannya dengan fungsi kelas ini.</p>
 */
public class GrupPengguna extends HttpServlet {

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
	public GrupPengguna() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET &mdash; bentuk pemanggilan lazim untuk membuka layar grup
	 * pengguna.
	 *
	 * <p><b>Cara kerja.</b> Melimpahkan seluruh pekerjaan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)} dan menangkap setiap
	 * {@code Exception} untuk diteruskan ke {@link Common#tampilErrorJikaAdmin(Exception)} &mdash;
	 * pesan galat hanya tampil bila pengguna aktif berperan admin, selain itu ditelan diam-diam.</p>
	 *
	 * @param request permintaan HTTP; tidak ada parameter yang dibaca
	 * @param response tanggapan HTTP; dipakai sebagai sasaran <i>forward</i> ke ZUL daftar job
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
	 * @param request permintaan HTTP; tidak ada parameter yang dibaca
	 * @param response tanggapan HTTP; dipakai sebagai sasaran <i>forward</i> ke ZUL daftar job
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
	 * Inti servlet: meneruskan permintaan ke ZUL daftar grup pengguna (job) pada modul
	 * pemeliharaan.
	 *
	 * <p>Satu-satunya pekerjaan method ini adalah
	 * <code>request.getRequestDispatcher("/WEB-INF/z/x/y/pages/maintenance/job/list.zul").forward(
	 * request, response)</code>. Tidak ada parameter yang dibaca, tidak ada atribut sesi yang
	 * ditulis, dan tidak ada logika bersyarat apa pun &mdash; setiap pemanggilan menghasilkan
	 * <i>forward</i> yang sama persis.</p>
	 *
	 * @param request permintaan HTTP yang diteruskan apa adanya ke ZUL tujuan
	 * @param response tanggapan HTTP yang diteruskan apa adanya ke ZUL tujuan
	 * @throws Exception bila <i>forward</i> gagal; ditangkap oleh pemanggil
	 *         ({@code doGet}/{@code doPost})
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		request.getRequestDispatcher("/WEB-INF/z/x/y/pages/maintenance/job/list.zul").forward(request, response);
	}

}