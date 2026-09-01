package ais.action.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;

import ais.action.servlet.api.ApiUtil;
import ais.common.Common;
import ais.common.newui.NewUiUnggahRequest;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * Gerbang API native untuk halaman desktop Flutter.
 *
 * <p>Servlet ini tidak merender HTML, ZUL, WebView, maupun iframe. Setelah token,
 * role, relasi {@code job_has_menu}, status aktif dan status leaf diverifikasi,
 * request diteruskan secara internal ke service JSON New UI. Dengan demikian
 * renderer Flutter dapat memakai metadata/list/mutation Generic CRUD yang telah
 * mengikat lifecycle Action existing tanpa membawa presentasi ZK ke klien.</p>
 *
 * <p><b>Unggahan berkas.</b> Permintaan yang membawa berkas datang sebagai
 * {@code multipart/form-data} dan diurai lebih dahulu oleh
 * {@link NewUiUnggahRequest}, karena deskriptor Servlet 2.5 aplikasi ini tidak
 * menyediakan {@code getPart()} maupun pembacaan field multipart oleh
 * {@code getParameter()}. Setelah diurai, permintaan berperilaku seperti form
 * biasa sehingga otentikasi, penjaga, dan controller di hilir tidak berubah.</p>
 *
 * <p>Route yang belum mempunyai service native tetap fail-closed oleh resolver
 * New UI dengan respons {@code NOT_MAPPED}/{@code SERVICE_NOT_FOUND}. Hal ini
 * penting: menebak CRUD dari bentuk ZUL saja dapat melewatkan validasi, detail
 * transaksi, stok, persetujuan, cetak, dan efek bisnis Action.</p>
 */
public class DesktopNativeApi extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Gunakan POST untuk API desktop native.");
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Referrer-Policy", "no-referrer");
		response.setHeader("X-Content-Type-Options", "nosniff");

		// Permintaan yang membawa berkas datang sebagai multipart. Badannya diurai
		// di sini lalu dibungkus supaya seluruh lapisan di hilir -- otentikasi di
		// bawah, penjaga, index.jsp, controller -- tetap memanggil getParameter()
		// seperti biasa. Cabang ini hanya dimasuki bila tipe isinya memang
		// multipart, sehingga perilaku permintaan yang sudah ada tidak berubah.
		if (NewUiUnggahRequest.multipart(request)) {
			try {
				request = NewUiUnggahRequest.urai(request);
			} catch (IllegalArgumentException e) {
				writeError(response, HttpServletResponse.SC_BAD_REQUEST, "UPLOAD_INVALID",
						e.getMessage() == null ? "Berkas unggahan ditolak." : e.getMessage());
				return;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				writeError(response, HttpServletResponse.SC_BAD_REQUEST, "UPLOAD_INVALID",
						"Berkas unggahan tidak dapat dibaca. Pastikan ukurannya di bawah "
								+ (NewUiUnggahRequest.BATAS_UKURAN / (1024 * 1024)) + " MB.");
				return;
			}
		}

		Session db = null;
		try {
			String token = trim(request.getParameter("token"));
			Long menuId = parseLong(request.getParameter("menuId"));
			String roleId = trim(request.getParameter("roleId"));
			if (token.length() == 0 || menuId == null) {
				writeError(response, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_INVALID",
						"Token atau menu tidak lengkap.");
				return;
			}

			Tbmuser user = ApiUtil.currentUser(token);
			if (user == null || user.getUserId() == null) {
				writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_EXPIRED",
						"Sesi aplikasi sudah berakhir.");
				return;
			}

			Tbmrole selectedRole = findOwnedRole(user, roleId);
			if (selectedRole == null || selectedRole.getRoleId() == null) {
				writeError(response, HttpServletResponse.SC_FORBIDDEN, "ROLE_FORBIDDEN",
						"Role tidak tersedia untuk pengguna ini.");
				return;
			}

			db = HibernateUtil.getSessionFactory().openSession();
			Tbmrole roleDb = (Tbmrole) db.get(Tbmrole.class, selectedRole.getRoleId());
			if (roleDb == null) {
				writeError(response, HttpServletResponse.SC_FORBIDDEN, "ROLE_NOT_FOUND",
						"Role tidak ditemukan.");
				return;
			}

			List<Menu> menus = new ArrayList<Menu>();
			if (roleDb.getMenus() != null) menus.addAll(roleDb.getMenus());
			Collections.sort(menus);
			Menu requestedMenu = findAuthorizedLeaf(menus, menuId);
			if (requestedMenu == null) {
				writeError(response, HttpServletResponse.SC_FORBIDDEN, "MENU_FORBIDDEN",
						"Menu tidak tersedia atau bukan leaf menu untuk role aktif.");
				return;
			}

			// Samakan role aktif dengan HakAksesApi dan shell New UI.
			Tbmuser.getUserRoleYgDipakai.put(user.getUserId(), roleDb);
			HttpSession httpSession = request.getSession(true);
			httpSession.setAttribute("mytbmuser", user);
			httpSession.setAttribute("usersTemp", user);
			httpSession.setAttribute("user", user);
			httpSession.setAttribute("udah_tanya", Boolean.TRUE);
			httpSession.setAttribute("currentMenus", menus);
			httpSession.setAttribute("currentMenu", requestedMenu);
			httpSession.setAttribute("current_menus", menus);
			httpSession.setAttribute("current_menu", requestedMenu);

			// Query internal menambahkan service/menuId; parameter action, paging,
			// filter dan field form dari POST semula tetap diteruskan oleh container.
			request.getRequestDispatcher("/new?service=1&menuId=" + menuId).forward(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			if (!response.isCommitted()) {
				writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
						"Layanan native gagal memproses menu.");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(db);
			try { HibernateUtil.closeSession(); } catch (Exception ignored) { }
		}
	}

	private static Tbmrole findOwnedRole(Tbmuser user, String requestedRoleId) {
		List<Tbmrole> roles;
		try { roles = user.ambilRoles(); }
		catch (Exception ignored) { roles = new ArrayList<Tbmrole>(); }
		if (requestedRoleId.length() > 0) {
			for (Tbmrole role : roles) {
				if (role != null && role.getRoleId() != null
						&& requestedRoleId.equalsIgnoreCase(role.getRoleId())) return role;
			}
			return null;
		}
		try { return user.hakAkses(); }
		catch (Exception ignored) { return user.getUserRole(); }
	}

	private static Menu findAuthorizedLeaf(List<Menu> menus, Long menuId) {
		Menu target = null;
		for (Menu menu : menus) {
			if (isActive(menu) && menuId.equals(menu.getId())) { target = menu; break; }
		}
		if (target == null || !hasText(target.getUrl())) return null;
		for (Menu candidate : menus) {
			if (isActive(candidate) && target.getChild() != null
					&& target.getChild().equals(candidate.getRoot())) return null;
		}
		return target;
	}

	private static boolean isActive(Menu menu) {
		return menu != null && menu.getId() != null
				&& (menu.getAktif() == null || menu.getAktif().booleanValue());
	}
	private static boolean hasText(String value) { return value != null && value.trim().length() > 0; }
	private static String trim(String value) { return value == null ? "" : value.trim(); }
	private static Long parseLong(String value) {
		try { return Long.valueOf(trim(value)); } catch (Exception ignored) { return null; }
	}
	private static void writeError(HttpServletResponse response, int status, String code, String message)
			throws IOException {
		response.setStatus(status);
		response.setContentType("application/json; charset=UTF-8");
		response.getWriter().write("{\"success\":false,\"code\":\"" + json(code)
				+ "\",\"message\":\"" + json(message) + "\"}");
	}
	private static String json(String value) {
		return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")
				.replace("\r", "\\r").replace("\n", "\\n");
	}
}
