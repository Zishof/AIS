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
public class Pks extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Pks() {
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

		// Pilih varian PKS via parameter "modul" (whitelist, cegah path traversal).
		// Default tetap draf PKS umum Enterprise Education seperti semula.
		String modul = request.getParameter("modul");
		String target = "/WEB-INF/baru/pks.jsp";
		if (modul != null && modul.trim().equalsIgnoreCase("kesehatan")) {
			target = "/WEB-INF/baru/pks_kesehatan.jsp";
		} else if (modul != null && modul.trim().equalsIgnoreCase("gudang")) {
			target = "/WEB-INF/baru/pks_gudang.jsp";
		}

		request.getRequestDispatcher(target).forward(request, response);

	}

}
