package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;
import ais.common.home.HomePortalService;
import ais.database.model.Konfigurasi;

public class Web extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	private void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Common.REAL_PATH = getServletContext().getRealPath("/");
		Common.REAL_PATH_REPORT_TEMP = getServletContext().getRealPath("/report");
		Common.ROOT = request.getContextPath();
		Common.CURRENT_URL_SIMPLE = (request.isSecure() ? "https://" : "http://") + request.getServerName()
				+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? ""
						: ":" + request.getServerPort());
		Common.CURRENT_URL = (Common.isSecure(request) ? "https://" : "http://") + request.getServerName()
				+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort())
				+ request.getContextPath();
		try {
			Konfigurasi aktif = Common.getKonfigurasi("website_kampus_aktif", Konfigurasi.AKTIF);
			if (aktif != null && aktif.getNilai() != null
					&& Konfigurasi.TIDAK_AKTIF.equalsIgnoreCase(aktif.getNilai().trim())) {
				response.sendRedirect(request.getContextPath() + "/index");
				return;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		if (!isEnabled("website_ui_v4", true)) {
			request.getRequestDispatcher("/WEB-INF/baru/website.jsp").forward(request, response);
			return;
		}

		try {
			request.setAttribute("website", new HomePortalService().build(request));
			request.getRequestDispatcher("/WEB-INF/baru/website/home.jsp").forward(request, response);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "Web website V4 fallback");
			if (!response.isCommitted()) {
				request.getRequestDispatcher("/WEB-INF/baru/website.jsp").forward(request, response);
			}
		}
	}

	private boolean isEnabled(String key, boolean fallback) {
		try {
			Konfigurasi config = Common.getKonfigurasi(key,
					fallback ? Konfigurasi.AKTIF : Konfigurasi.TIDAK_AKTIF);
			if (config == null || config.getNilai() == null) {
				return fallback;
			}
			String value = config.getNilai().trim();
			return Konfigurasi.AKTIF.equalsIgnoreCase(value) || "true".equalsIgnoreCase(value)
					|| "yes".equalsIgnoreCase(value);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "Web feature flag " + key);
			return fallback;
		}
	}
}
