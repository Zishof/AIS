package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet endpoint {@code /pos} yang menampilkan halaman Point of Sale (POS) klasik
 * ({@code /WEB-INF/baru/pos.jsp}), sekaligus menginisialisasi ulang beberapa variabel statis
 * global di {@link Common} (path fisik aplikasi dan URL dasar server) berdasarkan permintaan
 * yang sedang berjalan. Bukan sekadar forward pasif — lihat catatan efek samping pada javadoc
 * {@link #process}. Berkerabat dengan {@code PosApi} (API POS yang lebih lengkap,
 * didokumentasikan pada batch terpisah).
 */
public class Pos extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Konstruktor baku servlet, tanpa inisialisasi tambahan. */
	public Pos() {
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
	 * Menginisialisasi ulang variabel statis global {@link Common#REAL_PATH},
	 * {@link Common#REAL_PATH_REPORT_TEMP}, {@link Common#ROOT}, {@link Common#CURRENT_URL_SIMPLE},
	 * dan {@link Common#CURRENT_URL} berdasarkan {@code ServletContext}/{@code request} yang
	 * sedang diproses (efek samping bersifat global — mempengaruhi request lain yang berjalan
	 * bersamaan karena field-field tersebut statis, bukan per-request), lalu meneruskan (forward)
	 * permintaan ke {@code /WEB-INF/baru/pos.jsp} dengan parameter query {@code rnd} (barcode acak
	 * 7 digit dari {@link Common#getGeneratedBarCode(int)}) untuk mencegah cache browser. Galat
	 * yang terjadi dicatat via {@link ais.common.ErrorAuditUtil#record} dan TIDAK dilempar ulang
	 * (blok {@code catch} menelan exception), sehingga response bisa saja tidak pernah
	 * ter-forward tanpa pemberitahuan eksplisit ke pemanggil bila terjadi galat.
	 *
	 * @param request permintaan HTTP masuk
	 * @param response respons HTTP yang akan di-forward ke JSP
	 * @throws Exception dideklarasikan pada signature namun tidak pernah dilempar keluar method
	 *                    ini karena seluruh badan method dibungkus {@code try/catch} yang menelan
	 *                    exception
	 */
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
