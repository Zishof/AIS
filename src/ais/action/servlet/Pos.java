package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet implementation class CheckISBN
 */
public class Pos extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Pos() {
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
			request.getRequestDispatcher("/WEB-INF/baru/pos.jsp?rnd=" + Common.getGeneratedBarCode(7)).forward(request,
					response);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Pos.java:70");
		} finally {

		}
	}
}
