package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet tampilan yang meneruskan (forward) permintaan ke salah satu JSP deck presentasi,
 * dipilih berdasarkan parameter {@code modul}: {@code "keuangan"}/{@code "keuangan_mahasiswa"}/
 * {@code "keuangan-mahasiswa"} → presentasi keuangan mahasiswa, {@code "instansi"}/
 * {@code "yayasan"}/{@code "keuangan_instansi"}/{@code "keuangan-instansi"}/{@code "anggaran"} →
 * presentasi keuangan instansi, {@code "kesehatan"} → presentasi modul kesehatan,
 * {@code "gudang"} → presentasi modul gudang, nilai lain/tidak ada → presentasi umum Enterprise
 * Education (default). Pemilihan JSP memakai daftar putih (whitelist) nilai literal yang
 * dibandingkan setelah di-{@code trim()} dan di-lowercase-kan, sehingga nilai parameter
 * {@code modul} TIDAK pernah dipakai langsung sebagai bagian path JSP — mencegah path traversal
 * lewat parameter tersebut. Tidak melakukan pembacaan maupun penulisan apa pun ke database.
 */
public class Presentasi extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Konstruktor baku servlet, tanpa inisialisasi tambahan. */
	public Presentasi() {
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
	 * Menentukan JSP deck presentasi tujuan berdasarkan parameter {@code modul} (whitelist
	 * varian keuangan mahasiswa/keuangan instansi/kesehatan/gudang, default presentasi umum)
	 * lalu meneruskan (forward) permintaan ke JSP tersebut.
	 *
	 * @param request permintaan HTTP masuk, membawa parameter opsional {@code modul}
	 * @param response respons HTTP yang akan di-forward ke JSP
	 * @throws Exception bila terjadi galat saat forward ke JSP
	 */
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
