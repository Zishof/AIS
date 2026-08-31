package ais.action.servlet;

import ais.common.Common; 
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet utilitas untuk memaksa logout bersih: menghapus seluruh cookie yang dikirim browser
 * (dengan menyetel ulang nilai kosong, umur 0, dan path {@code "/"} agar cocok dengan cookie asli
 * lalu mengirimkannya kembali via {@code Set-Cookie}), menghancurkan sesi HTTP di sisi server,
 * kemudian mengalihkan browser ke {@code /logoff}. Menerima permintaan GET maupun POST dengan
 * perilaku identik.
 */
public class ClearCookieServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/** Menangani GET dengan mendelegasikan ke {@link #prosesPenghapusan}. */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		prosesPenghapusan(request, response);
	}

	/** Menangani POST dengan mendelegasikan ke {@link #prosesPenghapusan}. */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		prosesPenghapusan(request, response);
	}

	/**
	 * Menghapus semua cookie permintaan (nilai dikosongkan, umur 0, path {@code "/"}),
	 * menginvalidasi sesi HTTP, lalu mengalihkan ke {@code <contextPath>/logoff}. Kegagalan pada
	 * setiap tahap ditangkap dan dicatat ke audit tanpa menghentikan tahap berikutnya, sehingga
	 * redirect ke halaman logoff tetap diusahakan terjadi walau penghapusan cookie/sesi gagal.
	 */
	private void prosesPenghapusan(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		try {
			Cookie[] cookies = request.getCookies();

			// Menghapus semua cookie yang ditemukan
			if (cookies != null) {
				for (int i = 0; i < cookies.length; i++) {
					Cookie cookie = cookies[i];
					
					// 1. Kosongkan nilainya (Gunakan string kosong "", bukan null untuk menghindari error di beberapa server)
					cookie.setValue("");
					
					// 2. Set umur menjadi 0 untuk menginstruksikan browser menghapusnya
					cookie.setMaxAge(0);
					
					// 3. KUNCI UTAMA: Path WAJIB di-set ke "/" (Sama seperti saat pembuatan via JS)
					cookie.setPath("/"); 
					
					response.addCookie(cookie);
				}
			}
			
			// Hancurkan sesi di sisi server
			request.getSession().invalidate();
			
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/ClearCookieServlet.java:55");
		}

		// Mengalihkan (Redirect) ke URL logoff setelah proses selesai
		try {
			// Menggunakan sendRedirect lebih disarankan untuk logout agar URL di browser ter-refresh dan riwayat post terputus.
			// Menggunakan request.getContextPath() lebih dinamis dan aman ketimbang Common.ROOT untuk urusan redirect.
			String targetLogoff = request.getContextPath() + "/logoff";
			response.sendRedirect(targetLogoff);
			
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/ClearCookieServlet.java:66");
		}
	}
}