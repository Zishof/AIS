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
public class Presentasi extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Presentasi() {
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

		// Pilih deck presentasi via parameter "modul" (whitelist, cegah path traversal).
		// Default tetap presentasi umum Enterprise Education seperti semula.
		String modul = request.getParameter("modul");
		String target = "/WEB-INF/baru/presentasi.jsp";
		if (modul != null) {
			modul = modul.trim().toLowerCase();
			if (modul.equals("keuangan") || modul.equals("keuangan_mahasiswa")
					|| modul.equals("keuangan-mahasiswa")) {
				target = "/WEB-INF/baru/presentasi_keuangan_mahasiswa.jsp";
			} else if (modul.equals("instansi") || modul.equals("yayasan")
					|| modul.equals("keuangan_instansi") || modul.equals("keuangan-instansi")
					|| modul.equals("anggaran")) {
				target = "/WEB-INF/baru/presentasi_keuangan_instansi.jsp";
			} else if (modul.equals("kesehatan")) {
				target = "/WEB-INF/baru/presentasi_kesehatan.jsp";
			} else if (modul.equals("gudang")) {
				target = "/WEB-INF/baru/presentasi_gudang.jsp";
			}
		}

		request.getRequestDispatcher(target).forward(request, response);

	}

}
