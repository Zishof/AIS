package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet tampilan murni yang meneruskan (forward) permintaan ke halaman
 * {@code /WEB-INF/u/gagal.jsp} untuk menampilkan notifikasi "pembayaran gagal" kepada pengguna.
 *
 * <p>
 * Servlet ini TIDAK melakukan pembacaan maupun penulisan apa pun ke database — tidak ada
 * validasi terhadap transaksi pembayaran yang sesungguhnya, tidak ada perubahan status
 * pembayaran, dan tidak ada gerbang otentikasi/otorisasi. Ia murni sebuah "halaman tujuan"
 * (landing page) yang biasanya dituju oleh redirect dari gateway pembayaran setelah transaksi
 * gagal; JSP tujuan sendiri yang bertanggung jawab menampilkan pesan berdasarkan parameter
 * request (bila ada), bukan servlet ini.
 * </p>
 *
 * @see PembayaranSukses
 */
public class PembayaranGagal extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Konstruktor baku servlet, tanpa inisialisasi tambahan. */
	public PembayaranGagal() {
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
	 * Meneruskan (forward) permintaan apa adanya ke {@code /WEB-INF/u/gagal.jsp} tanpa
	 * menyertakan attribute tambahan maupun melakukan operasi database.
	 *
	 * @param request permintaan HTTP masuk
	 * @param response respons HTTP yang akan di-forward ke JSP
	 * @throws Exception bila terjadi galat saat forward ke JSP
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		request.getRequestDispatcher("/WEB-INF/u/gagal.jsp").forward(request, response);

	}

}
