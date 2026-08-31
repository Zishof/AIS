package ais.common;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.AccessedUsers;
import ais.database.model.LogLogin;
import ais.database.model.OnlineUsers;

/**
 * Listener lifecycle aplikasi/web untuk logout listener. Tipe ini bereaksi terhadap startup,
 * session, atau logout dan mengelola state global terkait tanpa mengambil alih aturan bisnis
 * request.
 *
 * <p><b>Batas tanggung jawab:</b> tipe ini mendeklarasikan kontrak {@link LogoutSuccessHandler}. Implementasi
 * konkret bertanggung jawab atas transaksi, resource, error handling, dan efek samping; pemanggil sebaiknya
 * bergantung pada kontrak ini agar tidak menggandakan integrasi.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah mutasi data ({@code setLogout()}); penghapusan/pembatalan ({@code
 * hapus()}); operasi domain lain ({@code onLogoutSuccess()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> callback berjalan pada lifecycle container dan dapat mengubah session, response,
 * penghitung global, atau resource aplikasi. Implementasi harus thread-safe dan tidak menyimpan data pengguna
 * request pada field singleton.</p>
 */
public class LogoutListener implements LogoutSuccessHandler {

	/**
	 * Melakukan proses logout di sisi server (Database & Static Map)
	 * Optimized for memory efficiency and session handling.
	 */
	public static void setLogout(HttpServletRequest request, HttpServletResponse response) {
		// Menggunakan getSession(false) agar tidak membuat session baru secara tidak sengaja
		HttpSession httpSession = request.getSession(false);
		if (httpSession == null) {
			return;
		}

		String sessionId = httpSession.getId();
		System.out.println("URI = " + request.getRequestURI()
				+ "  =============================================================== USER LOGOUT SUCCESS, SESSION ID = "
				+ sessionId + " ====================================================================== ");

		Session session = null;
		Transaction tx = null;

		try {
			// Menggunakan openSession untuk isolasi proses logout
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();

			OnlineUsers onlineUsers = SecurityFilter.dataOnline.remove(sessionId);
			if (onlineUsers != null) {

				// Hapus entitas terkait dalam satu transaksi
				hapus(onlineUsers, session);

				// Logika Single Device Policy (Hapus login lama jika user login kembali di perangkat lain)
				if (ConstantValues.satuperangkatipygbeda || ConstantValues.satuperangkat || ConstantValues.satuperangkat_mahasiswa) {
					List<String> keysToRemove = new ArrayList<String>();
					
					// Iterasi data online untuk menemukan duplikasi user
					for (java.util.Map.Entry<String, OnlineUsers> entry : SecurityFilter.dataOnline.entrySet()) {
						OnlineUsers online = entry.getValue();
						boolean isSameUser = false;

						if (online.getTbmuser() != null && onlineUsers.getTbmuser() != null
								&& onlineUsers.getTbmuser().getUserId().equals(online.getTbmuser().getUserId())) {
							isSameUser = true;
						} else if (online.getMahasiswa() != null && onlineUsers.getMahasiswa() != null
								&& online.getMahasiswa().getId().equals(onlineUsers.getMahasiswa().getId())) {
							isSameUser = true;
						} else if (online.getSiswa() != null && onlineUsers.getSiswa() != null
								&& online.getSiswa().getId().equals(onlineUsers.getSiswa().getId())) {
							isSameUser = true;
						}

						if (isSameUser) {
							if (online.getAccessedUsers() != null && online.getAccessedUsers().getNama() != null) {
								keysToRemove.add(entry.getKey());
							}
							hapus(online, session);
						}
					}

					// Pembersihan Map statis
					if (!keysToRemove.isEmpty()) {
						for (String key : keysToRemove) {
							SecurityFilter.dataOnline.remove(key);
						}
						keysToRemove.clear();
					}
					keysToRemove = null;
				}
			}

			if (tx != null && tx.isActive()) {
				tx.commit();
			}

		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/LogoutListener.java:100");}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/LogoutListener.java:102");
		} finally {
			// Pembersihan session sesuai standar manajemen memori
			if (session != null) {
				try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/LogoutListener.java:106");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/LogoutListener.java:107");}
				try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/LogoutListener.java:108");}
			}
			HibernateUtil.closeSession();
		}

		try {
			httpSession.removeAttribute("mytbmuser");
			httpSession.invalidate();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/LogoutListener.java:116");
			// Session mungkin sudah invalid
		}
		httpSession = null;
	}

	/**
	 * Internal helper untuk menghapus record login dan online users.
	 */
	private static void hapus(OnlineUsers onlineUsers, Session session) {
		try {
			LogLogin login = onlineUsers.getLogin();
			if (login != null) {
				login.setLogout(ais.ui.util.WaktuUtil.getDate());
				session.update(login); 
				SecurityFilter.dataLogin.remove(login.getNama());
			}
			
			AccessedUsers accessedUsers = onlineUsers.getAccessedUsers();
			session.delete(onlineUsers);

			if (accessedUsers != null) {
				session.delete(accessedUsers);
				System.out.println("Logout session destroyed: " + accessedUsers.getNama());
			}
		} catch (Exception e) {
			throw new RuntimeException(e); // Propagasi ke tx.rollback
		}
	}

	@Override
	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
			throws IOException, ServletException {

		String loginError = request.getParameter("login_error");
		String param = request.getParameter("param");
		String forwardUrl = request.getParameter("forward_url");

		// VALIDASI KHUSUS: Jika mengarah ke modul PMB atau PPDB, cukup bersihkan cookie login PMB.
		// Cookie lain tidak dihapus supaya sesi modul lain tidak terganggu.
		if (forwardUrl != null && (forwardUrl.equalsIgnoreCase("pmb") || forwardUrl.equalsIgnoreCase("ppdb"))) {
			Common.clearPmbLoginCookies(request, response);
		}

		// Eksekusi pembersihan session di server
		LogoutListener.setLogout(request, response);

		// Logika Pengalihan Halaman (Forward / Redirect)
		if (loginError != null && !loginError.isEmpty()) {
			response.sendRedirect("login?login_error=" + URLEncoder.encode(loginError, "UTF-8"));
		} else if (forwardUrl != null && !forwardUrl.isEmpty()) {
			// Melakukan FORWARD secara internal (Relative URL)
			request.getRequestDispatcher(forwardUrl).forward(request, response);
		} else if (param != null && !param.isEmpty()) {
			response.sendRedirect(param);
		} else {
			response.sendRedirect("index");
		}
	}
}