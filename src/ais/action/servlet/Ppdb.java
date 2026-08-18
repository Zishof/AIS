package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;
import ais.database.model.Konfigurasi;

/**
 * Servlet implementation class CheckISBN
 */
public class Ppdb extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Ppdb() {
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
		String dispatcherPath = "/WEB-INF/z/x/y/psb.zul";
		
		Konfigurasi config = Common.getKonfigurasi("default_ppdb_gunakan_versi_baru", Konfigurasi.TIDAK_AKTIF);

		// PERBAIKAN POTENSI ERROR:
		// config bisa saja bernilai null jika data tidak ditemukan di database.
		// Kita harus pastikan config != null sebelum memanggil .getNilai() (Mencegah
		// NullPointerException)
		boolean isVersiBaruAktif = config != null && Konfigurasi.AKTIF.equalsIgnoreCase(config.getNilai());
		boolean isRequestVersiBaruNull = request.getParameter("baru") != null;
		boolean hanya_tampil_jsp = request.getParameter("hanya_tampil_jsp") != null
				&& request.getParameter("hanya_tampil_jsp").trim().equalsIgnoreCase("true");
		if (isRequestVersiBaruNull || isVersiBaruAktif || hanya_tampil_jsp) {
			dispatcherPath = "/WEB-INF/baru/ppdb.jsp";
		}
		
		request.getRequestDispatcher(dispatcherPath).forward(request, response);

	}

}
