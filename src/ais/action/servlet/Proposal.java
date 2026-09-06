package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet tampilan yang meneruskan (forward) permintaan ke salah satu JSP proposal, dipilih
 * berdasarkan parameter {@code modul}: {@code "kesehatan"} → proposal modul kesehatan,
 * {@code "gudang"} → proposal modul gudang, nilai lain/tidak ada → proposal umum Enterprise
 * Education (default). Pemilihan JSP memakai daftar putih (whitelist) nilai literal yang
 * dibandingkan dengan {@code equalsIgnoreCase}, sehingga nilai parameter {@code modul} TIDAK
 * pernah dipakai langsung sebagai bagian path JSP — mencegah path traversal lewat parameter
 * tersebut. Tidak melakukan pembacaan maupun penulisan apa pun ke database.
 */
public class Proposal extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Konstruktor baku servlet, tanpa inisialisasi tambahan. */
	public Proposal() {
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
	 * Menentukan JSP proposal tujuan berdasarkan parameter {@code modul} (whitelist
	 * {@code "kesehatan"}/{@code "gudang"}, default proposal umum) lalu meneruskan (forward)
	 * permintaan ke JSP tersebut.
	 *
	 * @param request permintaan HTTP masuk, membawa parameter opsional {@code modul}
	 * @param response respons HTTP yang akan di-forward ke JSP
	 * @throws Exception bila terjadi galat saat forward ke JSP
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Pilih varian proposal via parameter "modul" (whitelist, cegah path traversal).
		// Default tetap proposal umum Enterprise Education seperti semula.
		String modul = request.getParameter("modul");
		String target = "/WEB-INF/baru/proposal.jsp";
		if (modul != null && modul.trim().equalsIgnoreCase("kesehatan")) {
			target = "/WEB-INF/baru/proposal_kesehatan.jsp";
		} else if (modul != null && modul.trim().equalsIgnoreCase("gudang")) {
			target = "/WEB-INF/baru/proposal_gudang.jsp";
		}

		request.getRequestDispatcher(target).forward(request, response);

	}

}
