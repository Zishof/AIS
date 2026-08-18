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
public class Desktop extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Desktop() {
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
		/*
		 * PAKSA tampilan DESKTOP. Mekanisme deteksi mobile (CommonCurrentSessionHelper.isMobile)
		 * membaca atribut sesi "is_mobile"; dengan menyetelnya FALSE di sini, dekstop.zul
		 * dan MainAction merender tampilan desktop PENUH (baris menu modul yang tombolnya
		 * bisa diklik), walaupun perangkatnya HP. Bersifat sticky selama sesi sampai
		 * pengguna memilih "Mode HP" kembali (?is_mobile=true).
		 * Tanpa langkah ini, /desktop di HP tetap dirender sebagai tampilan mobile.
		 */
		try {
			request.getSession(true).setAttribute("is_mobile", Boolean.FALSE);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Desktop.java:65");
			// abaikan; tetap lanjut forward
		}
		request.getRequestDispatcher("/WEB-INF/z/x/y/pages/main/dekstop.zul").forward(request, response);

	}

}
