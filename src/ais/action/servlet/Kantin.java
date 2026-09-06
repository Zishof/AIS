package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet tampilan murni yang meneruskan (forward) permintaan ke halaman landing modul Kantin
 * ({@code /WEB-INF/baru/modul/kantin/landing_page.jsp}). Tidak melakukan pembacaan maupun
 * penulisan apa pun ke database dan tidak menerapkan gerbang otentikasi/otorisasi sendiri;
 * kontrol akses (bila ada) sepenuhnya menjadi tanggung jawab konfigurasi keamanan servlet
 * (mis. {@code intercept-url}) atau JSP tujuan.
 */
public class Kantin extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Konstruktor baku servlet, tanpa inisialisasi tambahan. */
	public Kantin() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan GET dengan mendelegasikan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}; galat ditangani oleh
	 * {@link Common#tampilErrorJikaAdmin(Exception)} agar detail teknis hanya tampil untuk admin.
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
	 * Menangani permintaan POST dengan mendelegasikan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}; galat ditangani oleh
	 * {@link Common#tampilErrorJikaAdmin(Exception)} agar detail teknis hanya tampil untuk admin.
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Meneruskan (forward) permintaan apa adanya ke halaman landing modul Kantin tanpa
	 * menyertakan attribute tambahan maupun melakukan operasi database.
	 *
	 * @param request permintaan HTTP masuk
	 * @param response respons HTTP yang akan di-forward ke JSP
	 * @throws Exception bila terjadi galat saat forward ke JSP
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		request.getRequestDispatcher("/WEB-INF/baru/modul/kantin/landing_page.jsp").forward(request, response);

	}

}
