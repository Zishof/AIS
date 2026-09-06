package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet titik masuk antarmuka "baru" (New UI, {@code /WEB-INF/new/index.jsp}): menginisialisasi
 * variabel global {@link Common} yang bergantung pada konteks request pertama kali diterima
 * (path fisik aplikasi {@code REAL_PATH}/{@code REAL_PATH_REPORT_TEMP}, context path
 * {@code ROOT}, dan URL dasar aplikasi {@code CURRENT_URL}/{@code CURRENT_URL_SIMPLE}), lalu
 * memvalidasi/menyiapkan sesi user lewat {@link Main#checkAndSetUserSession} sebelum meneruskan ke
 * JSP indeks antarmuka baru.
 */
public class New extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/** Konstruktor default tanpa inisialisasi khusus. */
	public New() {
		super();
	}

	/** Menangani permintaan GET dengan mendelegasikan ke {@link #process}; kegagalan ditangkap dan dilaporkan, tidak dilempar ke container. */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Menangani permintaan POST dengan perilaku identik {@link #doGet}. */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Implementasi kanonik: menyetel variabel global {@link Common} terkait path/URL dari
	 * {@code request} saat ini, memvalidasi sesi user (tanpa memaksa login, {@code paksa=false}),
	 * lalu meneruskan ke {@code /WEB-INF/new/index.jsp} bila response belum di-commit (mis. belum
	 * di-redirect oleh pemeriksaan sesi).
	 */
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Common.REAL_PATH = getServletContext().getRealPath("/");
		Common.REAL_PATH_REPORT_TEMP = getServletContext().getRealPath("/report");
		Common.ROOT = request.getContextPath();
		if (Common.sanitizedRequestHostForCurrentUrl(request) != null) {
			Common.CURRENT_URL_SIMPLE = (request.isSecure() ? "https://" : "http://") + request.getServerName()
					+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? ""
							: ":" + request.getServerPort());
			Common.CURRENT_URL = (Common.isSecure(request) ? "https://" : "http://") + request.getServerName()
					+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort())
					+ request.getContextPath();
		}

		Main.checkAndSetUserSession(request, false);
		if (!response.isCommitted()) {
			request.getRequestDispatcher("/WEB-INF/new/index.jsp").forward(request, response);
		}
	}
}
