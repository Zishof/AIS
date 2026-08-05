package ais.action.servlet;

import ais.common.Common; 
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ClearCookieServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		prosesPenghapusan(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		prosesPenghapusan(request, response);
	}

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