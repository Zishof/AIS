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
	private static final long serialVersionUID = 1L;

	public Vendor() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			handleFatalError(request, response, e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			handleFatalError(request, response, e);
		}
	}

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

	private String trim(String s) {
		return s == null ? "" : s.trim();
	}
}
