package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet pengalih statis ke halaman login alumni &mdash; dipetakan ke <code>/loginAlumni</code>
 * pada {@code web.xml}.
 *
 * <p><b>Perilaku.</b> {@link #process(HttpServletRequest, HttpServletResponse)} tidak membaca
 * parameter permintaan sama sekali dan langsung meneruskan (<i>forward</i>) ke halaman ZK tetap
 * {@code /WEB-INF/z/x/y/pages/master/login_alumni.zul}, tanpa syarat apa pun. Tidak ada logika
 * otentikasi di kelas ini; ZUL tujuan itulah yang menampilkan formulir dan memproses login alumni
 * sesungguhnya.</p>
 *
 * <p><b>Catatan hubungan dengan {@code MServet}.</b> Servlet {@code /m}
 * ({@code ais.action.servlet.MServet}, lihat {@code task_5a059324}) dapat mengalihkan pemanggil ke
 * URL <code>/loginAlumni?digunakanUntukPenggunaAlumni=false</code> atau
 * <code>...=true</code> setelah membentuk sesi lewat magic-link. Kelas ini sendiri <b>tidak
 * membaca</b> parameter {@code digunakanUntukPenggunaAlumni}; parameter itu murni konsumsi ZUL
 * tujuan setelah <i>forward</i>, bukan bagian dari logika servlet ini.</p>
 */
public class LoginAlumni extends HttpServlet {
	/** Nomor versi serialisasi bawaan {@link HttpServlet}; kelas ini tanpa state instance. */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diperlukan kontainer servlet; tidak melakukan inisialisasi
	 * tambahan selain memanggil konstruktor {@link HttpServlet}.
	 */
	public LoginAlumni() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menerima permintaan HTTP GET dan meneruskannya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request  permintaan dari peramban; tidak ada parameter yang dibaca
	 * @param response respons yang akan diisi halaman {@code login_alumni.zul}
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
	 * @param response respons yang akan diisi halaman {@code login_alumni.zul}
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
	 * Meneruskan (<i>forward</i>) permintaan ke halaman login alumni, tanpa syarat dan tanpa
	 * membaca parameter apa pun.
	 *
	 * <p>Jalur tujuan {@code /WEB-INF/z/x/y/pages/master/login_alumni.zul} adalah konstanta di
	 * kode; tidak ada bagian yang berasal dari masukan pengguna.</p>
	 *
	 * @param request  permintaan yang sedang dilayani; tidak ada parameter yang dibaca
	 * @param response respons yang akan diisi halaman {@code login_alumni.zul}
	 * @throws Exception bila penerusan gagal
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		request.getRequestDispatcher("/WEB-INF/z/x/y/pages/master/login_alumni.zul").forward(request, response);

	}

}
