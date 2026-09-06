package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet pengalih statis ke halaman login lama &mdash; dipetakan ke <code>/login2</code> pada
 * {@code web.xml}.
 *
 * <p><b>Perilaku.</b> {@link #process(HttpServletRequest, HttpServletResponse)} tidak membaca
 * parameter permintaan sama sekali dan langsung meneruskan (<i>forward</i>) ke berkas JSP tetap
 * {@code /WEB-INF/baru/login.jsp}, tanpa syarat maupun percabangan apa pun. Tidak ada logika
 * otentikasi, tidak ada pembacaan {@code username}/{@code password}, dan tidak ada pemeriksaan
 * status login &mdash; kelas ini murni pengalih statis satu baris.</p>
 *
 * <h3>Perbandingan dengan {@link Login}</h3>
 * <p>Berbeda jauh dari {@link Login} (dipetakan ke <code>/login</code>), yang berperan ganda:
 * endpoint otentikasi AJAX <code>action=ajax_login</code> yang mendelegasikan ke
 * {@code SecurityFilter.doAutoLogin}, sekaligus penyaji halaman dengan rantai cadangan menurut
 * konfigurasi ({@code default_login_versi_baru} dan sejenisnya). Kelas ini <b>tidak memiliki
 * satu pun</b> dari kedua peran tersebut: tidak ada endpoint AJAX, tidak ada query basis data,
 * dan tidak ada pembacaan kredensial. Akibatnya, pola pencarian akun yang relevan dengan
 * perbaikan {@code MatchMode.EXACT} (potensi injeksi pola LIKE lewat
 * {@code Restrictions.ilike}) pada {@code Login.cekStatusAwalAjaxLogin} <b>tidak berlaku sama
 * sekali di sini</b> &mdash; tidak ada satu pun pencarian akun yang dijalankan oleh kelas ini.</p>
 *
 * <h3>Catatan penamaan yang berpotensi membingungkan</h3>
 * <p>Nama kelas {@code Login2} mirip dengan berkas {@code /WEB-INF/baru/login2.jsp} yang dituju
 * oleh {@link Login#process(HttpServletRequest, HttpServletResponse)} maupun servlet lain ketika
 * konfigurasi {@code default_login_versi_baru}/{@code default_home_login_versi_baru} aktif &mdash;
 * tetapi keduanya tidak berkaitan: kelas ini meneruskan ke {@code login.jsp} (tanpa angka), bukan
 * {@code login2.jsp}. Jangan menyimpulkan hubungan apa pun antara keduanya hanya dari kemiripan
 * nama.</p>
 *
 * @see Login
 */
public class Login2 extends HttpServlet {
	/** Nomor versi serialisasi bawaan {@link HttpServlet}; kelas ini tanpa state instance. */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diperlukan kontainer servlet; tidak melakukan inisialisasi
	 * tambahan selain memanggil konstruktor {@link HttpServlet}.
	 */
	public Login2() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menerima permintaan HTTP GET dan meneruskannya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request  permintaan dari peramban; tidak ada parameter yang dibaca
	 * @param response respons yang akan diisi halaman {@code login.jsp}
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
	 * @param response respons yang akan diisi halaman {@code login.jsp}
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
	 * Meneruskan (<i>forward</i>) permintaan ke halaman login lama, tanpa syarat dan tanpa
	 * membaca parameter apa pun.
	 *
	 * <p>Jalur tujuan {@code /WEB-INF/baru/login.jsp} adalah konstanta di kode; tidak ada bagian
	 * yang berasal dari masukan pengguna, sehingga tidak ada risiko open-redirect atau path
	 * traversal pada method ini.</p>
	 *
	 * @param request  permintaan yang sedang dilayani; tidak ada parameter yang dibaca
	 * @param response respons yang akan diisi halaman {@code login.jsp}
	 * @throws Exception bila penerusan gagal
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String dispacher = "/WEB-INF/baru/login.jsp";
		request.getRequestDispatcher(dispacher).forward(request, response);

	}

}
