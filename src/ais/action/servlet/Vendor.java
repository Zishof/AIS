package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ais.common.Common;

/**
 * Servlet Portal Vendor.
 *
 * FIX 2026-05-30:
 * - auth_action=logout ditangani di servlet sebelum forward ke JSP agar tidak blank putih.
 * - jika terjadi error forward/include, servlet menulis fallback HTML sederhana ke response.
 * - redirect memakai request.getContextPath(), bukan Common.ROOT, agar aman pada context /ecampus.
 */
public class Vendor extends HttpServlet {
	/** Versi serialisasi tetap 1L; servlet tidak pernah benar-benar diserialisasi ke stream. */
	private static final long serialVersionUID = 1L;

	/** Konstruktor bawaan tanpa argumen, hanya meneruskan ke {@link HttpServlet#HttpServlet()}. */
	public Vendor() {
		super();
	}

	/**
	 * Menangani permintaan GET portal vendor. Publik/anonim (tanpa gerbang login di servlet ini
	 * -- login vendor sendiri ditangani di dalam JSP/sesi portal vendor); servlet ini hanya
	 * routing dan tidak membaca/mengekspos data vendor (mis. kredensial) secara langsung. Galat
	 * tak terduga ditangkap dan dibalas halaman fallback HTML alih-alih membiarkan respons kosong.
	 *
	 * @param request permintaan HTTP masuk
	 * @param response respons HTTP keluar
	 * @throws ServletException tidak pernah dilempar, digantikan oleh {@link #handleFatalError}
	 * @throws IOException jika terjadi galat I/O saat menulis fallback HTML
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			handleFatalError(request, response, e);
		}
	}

	/**
	 * Menangani permintaan POST portal vendor dengan perilaku identik dengan
	 * {@link #doGet(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP masuk
	 * @param response respons HTTP keluar
	 * @throws ServletException tidak pernah dilempar, digantikan oleh {@link #handleFatalError}
	 * @throws IOException jika terjadi galat I/O saat menulis fallback HTML
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			handleFatalError(request, response, e);
		}
	}

	/**
	 * Logika inti routing portal vendor: menangani logout, memforward ke ZUL lama (mode
	 * {@code versilama=true}) atau ke JSP modern portal vendor. Header cache dinonaktifkan agar
	 * halaman login/status vendor selalu segar.
	 *
	 * @param request permintaan HTTP masuk; parameter {@code auth_action} dan {@code versilama}
	 *        dibaca di sini
	 * @param response respons HTTP keluar
	 * @throws Exception diteruskan apa adanya ke pemanggil untuk ditangani {@link #handleFatalError}
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		//response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);

		String authAction = trim(request.getParameter("auth_action"));
		if ("logout".equalsIgnoreCase(authAction)) {
			doLogout(request);
			redirectToVendorHome(request, response, "logout=1");
			return;
		}

		boolean isRequestVersiLamaNull = request.getParameter("versilama") == null;
		if (!isRequestVersiLamaNull && "true".equalsIgnoreCase(request.getParameter("versilama"))) {
			request.getRequestDispatcher("/WEB-INF/z/x/y/vendor.zul").forward(request, response);
			return;
		}

		String dispatcherPath = "/WEB-INF/baru/vendor.jsp";
		request.getRequestDispatcher(dispatcherPath).forward(request, response);
	}

	/**
	 * Menghapus seluruh atribut sesi terkait login portal vendor (JSP modern) maupun sisa
	 * atribut modul lama, sehingga pengguna benar-benar keluar dari sesi vendor setelah
	 * {@code auth_action=logout}. Galat saat membersihkan sesi diredam agar logout tetap
	 * berlanjut ke redirect (tidak boleh membuat halaman blank).
	 *
	 * @param request permintaan HTTP yang membawa sesi yang akan dibersihkan
	 */
	private void doLogout(HttpServletRequest request) {
		try {
			HttpSession session = request.getSession(false);
			if (session != null) {
				// Session khusus portal vendor JSP modern.
				session.removeAttribute("VENDOR_LOGGED_IN");
				session.removeAttribute("VENDOR_USER_LOGGED_IN");
				session.removeAttribute("VENDOR_LOGIN_ERROR");
				session.removeAttribute("VENDOR_LAST_MESSAGE");

				// Beberapa kemungkinan key lama agar kompatibel dengan modul sebelumnya.
				session.removeAttribute("penyediaAsset");
				session.removeAttribute("vendorLogged");
				session.removeAttribute("vendor");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Vendor.java:85");
			// Logout tidak boleh membuat halaman blank.
		}
	}

	/**
	 * Mengalihkan (redirect) ke halaman utama portal vendor, opsional dengan query string.
	 * Memakai {@code request.getContextPath()} (bukan {@code Common.ROOT}) agar tetap benar pada
	 * context path non-default (mis. {@code /ecampus}).
	 *
	 * @param request permintaan HTTP masuk, dipakai mengambil context path
	 * @param response respons HTTP keluar
	 * @param query query string tambahan (tanpa {@code ?}); boleh {@code null}/kosong
	 * @throws IOException jika terjadi galat I/O saat mengirim redirect
	 */
	private void redirectToVendorHome(HttpServletRequest request, HttpServletResponse response, String query)
			throws IOException {
		String contextPath = request.getContextPath();
		if (contextPath == null) {
			contextPath = "";
		}
		String url = contextPath + "/vendor";
		if (query != null && query.trim().length() > 0) {
			url += "?" + query;
		}
		response.sendRedirect(response.encodeRedirectURL(url));
	}

	/**
	 * Menangani galat fatal tak terduga dari {@link #process}: mencatat galat (jika pemakai
	 * admin, ditampilkan detailnya lewat {@link Common#tampilErrorJikaAdmin}), lalu menulis
	 * halaman fallback HTML sederhana (bukan membiarkan respons kosong/blank) berisi tautan
	 * kembali ke portal vendor. Tidak melakukan apa pun jika respons sudah terkirim sebagian
	 * ({@code isCommitted()}).
	 *
	 * @param request permintaan HTTP masuk, dipakai mengambil context path untuk tautan kembali
	 * @param response respons HTTP keluar; direset lalu diisi halaman fallback
	 * @param e galat yang terjadi
	 * @throws IOException jika terjadi galat I/O saat menulis halaman fallback
	 */
	private void handleFatalError(HttpServletRequest request, HttpServletResponse response, Exception e)
			throws IOException {
		try {
			Common.tampilErrorJikaAdmin(e);
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Vendor.java:107");
		}

		if (response.isCommitted()) {
			return; 
		}

		response.reset();
		//response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html; charset=UTF-8");
		String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
		response.getWriter().write("<!doctype html><html><head><meta charset='utf-8'>"
				+ "<meta name='viewport' content='width=device-width, initial-scale=1'>"
				+ "<title>Portal Vendor</title>"
				+ "<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css' rel='stylesheet'>"
				+ "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css'>"
				+ "</head><body class='bg-light'>"
				+ "<div class='container py-5'><div class='card border-0 shadow rounded-4'><div class='card-body p-4 p-lg-5'>"
				+ "<div class='d-flex align-items-center gap-3 mb-3'><div class='rounded-circle bg-warning-subtle text-warning p-3'>"
				+ "<i class='fas fa-triangle-exclamation fa-2x'></i></div><div><h3 class='fw-bold mb-1'>Portal Vendor belum dapat ditampilkan</h3>"
				+ "<p class='text-muted mb-0'>Terjadi kendala saat membuka halaman. Silakan kembali ke halaman vendor.</p></div></div>"
				+ "<a class='btn btn-primary rounded-pill px-4' href='" + contextPath + "/vendor'><i class='fas fa-home me-2'></i>Kembali ke Portal Vendor</a>"
				+ "</div></div></div></body></html>");
	}

	/**
	 * Merapikan nilai string dengan {@code trim()}, memperlakukan {@code null} sebagai string kosong.
	 *
	 * @param s nilai mentah; boleh {@code null}
	 * @return nilai yang sudah di-trim; string kosong jika {@code s} {@code null}
	 */
	private String trim(String s) {
		return s == null ? "" : s.trim();
	}
}
