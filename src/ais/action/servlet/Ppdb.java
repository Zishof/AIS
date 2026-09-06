package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;
import ais.database.model.Konfigurasi;

/**
 * Servlet endpoint {@code /ppdb} yang meneruskan (forward) permintaan ke salah satu dari dua
 * tampilan PPDB (Penerimaan Peserta Didik Baru): halaman ZK lama ({@code /WEB-INF/z/x/y/psb.zul},
 * default) atau halaman JSP baru ({@code /WEB-INF/baru/ppdb.jsp}). Pemilihan versi ditentukan
 * oleh (mana pun yang terpenuhi lebih dulu memicu versi baru): parameter {@code baru} hadir pada
 * permintaan, konfigurasi global {@code default_ppdb_gunakan_versi_baru} bernilai
 * {@link Konfigurasi#AKTIF}, atau parameter {@code hanya_tampil_jsp=true}. Tidak melakukan
 * penulisan apa pun ke database; hanya membaca satu baris {@link Konfigurasi}.
 */
public class Ppdb extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Konstruktor baku servlet, tanpa inisialisasi tambahan. */
	public Ppdb() {
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
	 * Menentukan halaman PPDB tujuan: default halaman ZK lama
	 * ({@code /WEB-INF/z/x/y/psb.zul}), beralih ke halaman JSP baru
	 * ({@code /WEB-INF/baru/ppdb.jsp}) bila parameter {@code baru} hadir pada permintaan, atau
	 * konfigurasi {@code default_ppdb_gunakan_versi_baru} aktif, atau parameter
	 * {@code hanya_tampil_jsp} bernilai {@code "true"} (case-insensitive) — lalu meneruskan
	 * (forward) permintaan ke halaman yang dipilih.
	 *
	 * @param request permintaan HTTP masuk, membawa parameter opsional {@code baru} dan
	 *                {@code hanya_tampil_jsp}
	 * @param response respons HTTP yang akan di-forward ke halaman tujuan
	 * @throws Exception bila terjadi galat saat forward ke halaman tujuan
	 */
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
