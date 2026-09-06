package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet pengalih statis ke halaman logout &mdash; dipetakan ke <code>/logout</code> pada
 * {@code web.xml}.
 *
 * <p><b>Perilaku.</b> {@link #process(HttpServletRequest, HttpServletResponse)} tidak membaca
 * parameter permintaan sama sekali dan langsung meneruskan (<i>forward</i>) ke berkas JSP tetap
 * {@code /WEB-INF/u/logout.jsp}, tanpa syarat apa pun. Invalidasi sesi dan pembersihan atribut
 * login sesungguhnya (bila ada) dilakukan oleh JSP tujuan itu, <b>bukan</b> oleh kelas ini &mdash;
 * kelas ini murni pengalih.</p>
 *
 * <p><b>Catatan.</b> Karena tidak ada pemeriksaan status login di sini, memanggil
 * <code>/logout</code> pada pengguna yang belum login pun tetap diteruskan ke
 * {@code logout.jsp} apa adanya; halaman itulah yang menentukan tampilan/pengalihan
 * selanjutnya.</p>
 */
public class Logout extends HttpServlet {
	/** Nomor versi serialisasi bawaan {@link HttpServlet}; kelas ini tanpa state instance. */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diperlukan kontainer servlet; tidak melakukan inisialisasi
	 * tambahan selain memanggil konstruktor {@link HttpServlet}.
	 */
	public Logout() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menerima permintaan HTTP GET dan meneruskannya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request  permintaan dari peramban; tidak ada parameter yang dibaca
	 * @param response respons yang akan diisi halaman {@code logout.jsp}
	 * @throws ServletException bila container melaporkan kegagalan servlet
	 * @throws IOException      bila penulisan respons gagal
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
	 * Menerima permintaan HTTP POST dan meneruskannya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p>Perilakunya identik dengan {@link #doGet(HttpServletRequest, HttpServletResponse)};
	 * kelas ini tidak membedakan metode HTTP.</p>
	 *
	 * @param request  permintaan dari peramban; tidak ada parameter yang dibaca
	 * @param response respons yang akan diisi halaman {@code logout.jsp}
	 * @throws ServletException bila container melaporkan kegagalan servlet
	 * @throws IOException      bila penulisan respons gagal
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
	 * Meneruskan (<i>forward</i>) permintaan ke halaman logout, tanpa syarat dan tanpa membaca
	 * parameter apa pun.
	 *
	 * <p>Jalur tujuan {@code /WEB-INF/u/logout.jsp} adalah konstanta di kode; tidak ada bagian
	 * yang berasal dari masukan pengguna.</p>
	 *
	 * @param request  permintaan yang sedang dilayani; tidak ada parameter yang dibaca
	 * @param response respons yang akan diisi halaman {@code logout.jsp}
	 * @throws Exception bila penerusan gagal
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		request.getRequestDispatcher("/WEB-INF/u/logout.jsp").forward(request, response);

	}

}
