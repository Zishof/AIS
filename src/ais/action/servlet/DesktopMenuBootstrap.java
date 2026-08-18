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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * Jembatan aman dari aplikasi desktop ke shell ZK.
 *
 * <p>Token hanya diterima melalui POST dan tidak pernah ditempelkan pada URL.
 * Servlet memvalidasi ulang bahwa menu berasal dari relasi job_has_menu milik
 * role aktif, masih aktif, dan merupakan leaf. Setelah sesi HTTP/ZK terbentuk,
 * MainAction membuka menu dengan Common.launchMenu sehingga seluruh Action.java,
 * ZUL, privilege, popup, dan laporan memakai implementasi web yang sama.</p>
 */
public class DesktopMenuBootstrap extends HttpServlet {

	private static final long serialVersionUID = 1L;
	public static final String ATTR_PENDING_MENU_ID = "desktopPendingMenuId";
	public static final String ATTR_PENDING_ROLE_ID = "desktopPendingRoleId";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
				"Gunakan aplikasi desktop untuk membuka menu ini.");
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Referrer-Policy", "no-referrer");

		Session db = null;
		try {
			String token = trim(request.getParameter("token"));
			Long menuId = parseLong(request.getParameter("menuId"));
			String roleId = trim(request.getParameter("roleId"));
			if (token.length() == 0 || menuId == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Token atau menu tidak lengkap.");
				return;
			}

			// Token harus menjadi sumber identitas; jangan menerima user lama dari
			// cookie WebView bila aplikasi desktop baru saja berganti akun.
			Tbmuser user = ApiUtil.currentUser(token);
			if (user == null || user.getUserId() == null) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sesi aplikasi sudah berakhir.");
				return;
			}

			Tbmrole selectedRole = findOwnedRole(user, roleId);
			if (selectedRole == null || selectedRole.getRoleId() == null) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Role tidak tersedia untuk pengguna ini.");
				return;
			}

			db = HibernateUtil.getSessionFactory().openSession();
			Tbmrole roleDb = (Tbmrole) db.get(Tbmrole.class, selectedRole.getRoleId());
			if (roleDb == null) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Role tidak ditemukan.");
				return;
			}

			List<Menu> menus = new ArrayList<Menu>();
			if (roleDb.getMenus() != null) {
				menus.addAll(roleDb.getMenus());
			}
			Collections.sort(menus);
			Menu requestedMenu = findAuthorizedLeaf(menus, menuId);
			if (requestedMenu == null) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN,
						"Menu tidak tersedia atau bukan leaf menu untuk role aktif.");
				return;
			}

			// Samakan role aktif yang dipakai hakAkses() dengan role pada respons API.
			Tbmuser.getUserRoleYgDipakai.put(user.getUserId(), roleDb);

			HttpSession httpSession = request.getSession(true);
			httpSession.setAttribute("mytbmuser", user);
			httpSession.setAttribute("usersTemp", user);
			httpSession.setAttribute("user", user);
			httpSession.setAttribute("udah_tanya", Boolean.TRUE);
			httpSession.setAttribute("currentMenus", menus);
			httpSession.setAttribute("currentMenu", requestedMenu);
			httpSession.setAttribute(ATTR_PENDING_MENU_ID, requestedMenu.getId());
			httpSession.setAttribute(ATTR_PENDING_ROLE_ID, roleDb.getRoleId());

			// Forward internal melewati pemeriksaan principal Spring pada /main, tetapi
			// tetap memakai sesi yang baru diautentikasi dengan token di atas. Request
			// berikutnya (ZK AU/ZUL) membawa JSESSIONID yang sama.
			request.setAttribute("desktop", "true");
			request.setAttribute("versilama", "true");
			request.getRequestDispatcher("/main").forward(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			if (!response.isCommitted()) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Menu web gagal disiapkan. Silakan coba kembali.");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(db);
			try {
				HibernateUtil.closeSession();
			} catch (Exception e) {
				// Thread-local session boleh sudah ditutup oleh helper autentikasi.
			}
		}
	}

	private static Tbmrole findOwnedRole(Tbmuser user, String requestedRoleId) {
		List<Tbmrole> roles;
		try {
			roles = user.ambilRoles();
		} catch (Exception e) {
			roles = new ArrayList<Tbmrole>();
		}
		if (requestedRoleId.length() > 0) {
			for (Tbmrole role : roles) {
				if (role != null && role.getRoleId() != null
						&& requestedRoleId.equalsIgnoreCase(role.getRoleId())) {
					return role;
				}
			}
			return null;
		}
		try {
			return user.hakAkses();
		} catch (Exception e) {
			return user.getUserRole();
		}
	}

	private static Menu findAuthorizedLeaf(List<Menu> menus, Long menuId) {
		Menu target = null;
		for (Menu menu : menus) {
			if (isActive(menu) && menuId.equals(menu.getId())) {
				target = menu;
				break;
			}
		}
		if (target == null || !hasText(target.getUrl())) {
			return null;
		}
		for (Menu candidate : menus) {
			if (isActive(candidate) && candidate.getRoot() != null && target.getChild() != null
					&& candidate.getRoot().equals(target.getChild())) {
				return null;
			}
		}
		return target;
	}

	private static boolean isActive(Menu menu) {
		return menu != null && menu.getId() != null
				&& (menu.getAktif() == null || menu.getAktif().booleanValue());
	}

	private static boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}

	private static Long parseLong(String value) {
		try {
			return Long.valueOf(trim(value));
		} catch (Exception e) {
			return null;
		}
	}
}
