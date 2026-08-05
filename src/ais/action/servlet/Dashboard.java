package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;

/**
 * Servlet implementation class CheckISBN
 */
public class Dashboard extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Dashboard() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
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
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {

			Common.REAL_PATH = getServletContext().getRealPath("/");
			Common.REAL_PATH_REPORT_TEMP = getServletContext().getRealPath("/report");
			Common.ROOT = request.getContextPath();
			Common.CURRENT_URL_SIMPLE = (request.isSecure() ? "https://" : "http://") + request.getServerName()
					+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? ""
							: ":" + request.getServerPort());
			Common.CURRENT_URL = (Common.isSecure(request) ? "https://" : "http://") + request.getServerName()
					+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? ""
							: ":" + request.getServerPort())
					+ request.getContextPath();
			Konfigurasi config = Common.getKonfigurasi("akses_ke_dashboard_tanpa_login_tidak_diizinkan",
					Konfigurasi.AKTIF);
			if (config.getNilai().equalsIgnoreCase(Konfigurasi.AKTIF)) {
				Tbmuser tbmuser = Common.getCurrentUser(request);
				if (tbmuser == null || tbmuser.getUserId() == null) {
					PrintWriter outWriter = response.getWriter();
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					JSONObject errAuth = new JSONObject();
					errAuth.put("status", "error");
					errAuth.put("message", Common.getBahasaConfig("Sesi Anda telah habis. Silakan login kembali."));
					outWriter.print(errAuth.toString());
					outWriter.flush();
				} else {
					request.getRequestDispatcher("/WEB-INF/baru/dashboard.jsp?tampilkan_header=false").forward(request,
							response);
				}
			} else {
				request.getRequestDispatcher("/WEB-INF/baru/dashboard.jsp?tampilkan_header=true").forward(request,
						response);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Dashboard.java:93");
		} finally {

		}
	}
}
