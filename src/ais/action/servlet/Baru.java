package ais.action.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * Servlet implementation class Baru
 */
public class Baru extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final String PAGE_MAIN = "/WEB-INF/baru/index.jsp";
	private static final String PAGE_BARU_INDEX = "/WEB-INF/baru/index.jsp";
	private static final String PAGE_HAK_AKSES = "/WEB-INF/baru/modul/common/hak_akses.jsp";
	private static final String PAGE_MEMBER = "/WEB-INF/baru/modul/kantin/index.jsp";
	private static final String PAGE_APOTIK_SERVICE = "/WEB-INF/baru/modul/apotik/service.jsp";

	public Baru() {
		super();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		handleRequest(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		handleRequest(request, response);
	}

	private void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);

			if (!response.isCommitted()) {
				throw new ServletException(e);
			}
		}
	}

	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		initCommonUrl(request);

		/*
		 * User wajib dicek dulu sebelum dipakai.
		 * Pada kode lama, tbmuser dipakai sebelum dideklarasikan.
		 */
		Tbmuser tbmuser = Main.checkAndSetUserSession(request, true);

		if (response.isCommitted()) {
			return;
		}

		String dispatcher = resolveDispatcher(request, tbmuser);

		if (!response.isCommitted()) {
			request.getRequestDispatcher(dispatcher).forward(request, response);
		}
	}

	private void initCommonUrl(HttpServletRequest request) {
		Common.REAL_PATH = getServletContext().getRealPath("/");
		Common.REAL_PATH_REPORT_TEMP = getServletContext().getRealPath("/report");
		Common.ROOT = request.getContextPath();

		String protocol = Common.isSecure(request) ? "https://" : "http://";
		String serverName = request.getServerName();
		int serverPort = request.getServerPort();

		String port = "";
		if (serverPort != 80 && serverPort != 443) {
			port = ":" + serverPort;
		}

		Common.CURRENT_URL_SIMPLE = protocol + serverName + port;
		Common.CURRENT_URL = Common.CURRENT_URL_SIMPLE + request.getContextPath();
	}

	private String resolveDispatcher(HttpServletRequest request, Tbmuser tbmuser) throws Exception {
		if ("apotik".equalsIgnoreCase(request.getParameter("p"))
				&& "service".equalsIgnoreCase(request.getParameter("s"))) {
			return PAGE_APOTIK_SERVICE;
		}
		/*
		 * Prioritas 1:
		 * Jika user adalah anggota koperasi/member biasa,
		 * bukan pedagang, bukan admin lain, dan konfigurasi aktif,
		 * langsung arahkan ke halaman landing member.
		 */
		if (tbmuser != null && harusKeHalamanMember(tbmuser)) {
			return PAGE_MEMBER;
		}

		/*
		 * Prioritas 2:
		 * Jika bukan kondisi harusKeHalamanMember(),
		 * lalu terdapat parameter p=kantin,
		 * maka selalu arahkan ke /WEB-INF/baru/index.jsp.
		 *
		 * Contoh:
		 * /baru?p=kantin&s=ringkasan
		 */
		if (isParameterKantin(request)) {
			return PAGE_BARU_INDEX;
		}

		/*
		 * Prioritas 3:
		 * Jika request memang meminta halaman pilih hak akses.
		 */
		if (isRequestHakAkses(request)) {
			return PAGE_HAK_AKSES;
		}

		/*
		 * Prioritas 4:
		 * Jika user punya lebih dari 1 role dan belum pernah ditanya,
		 * arahkan ke halaman pilih hak akses.
		 */
		if (tbmuser != null && harusTanyaHakAkses(request, tbmuser)) {
			return PAGE_HAK_AKSES;
		}

		return PAGE_MAIN;
	}

	private boolean isParameterKantin(HttpServletRequest request) {
		return "kantin".equalsIgnoreCase(request.getParameter("p"));
	}

	private boolean isRequestHakAkses(HttpServletRequest request) {
		return "true".equalsIgnoreCase(request.getParameter("hak_akses"));
	}

	private boolean harusTanyaHakAkses(HttpServletRequest request, Tbmuser tbmuser) throws Exception {
		if (tbmuser == null) {
			return false;
		}

		if (request.getSession().getAttribute("udah_tanya") != null) {
			return false;
		}

		List<Tbmrole> tbmroles = tbmuser.ambilRoles();

		return tbmroles != null && tbmroles.size() > 1;
	}

	private boolean harusKeHalamanMember(Tbmuser tbmuser) throws Exception {
		if (tbmuser == null) {
			return false;
		}

		if (tbmuser.hakAkses() == null) {
			return false;
		}

		if (Tbmrole.KANTIN.equals(tbmuser.hakAkses().getRoleId())) {
			return false;
		}

		if (tbmuser.getAnggotaKoperasi() == null) {
			return false;
		}

		if (tbmuser.getPedagang() != null) {
			return false;
		}

		if (Common.getApakahAdminLain(tbmuser)) {
			return false;
		}

		Konfigurasi konfigurasi = Common.getKonfigurasi(
				"jika_login_sebagai_member_kecuali_admin_maka_langsung_ke_halaman_member",
				Konfigurasi.TIDAK_AKTIF);

		return konfigurasi != null && Konfigurasi.AKTIF.equals(konfigurasi.getNilai());
	}
}
