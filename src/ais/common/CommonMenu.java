package ais.common;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.action.master.helper.MainHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.DynamicReport;
import ais.database.model.Konfigurasi;
import ais.database.model.LogLogin;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;

/**
 * Helper menu dan privilege untuk common menu. Tipe ini membentuk navigasi berdasarkan hak
 * pengguna dan menjadi satu sumber pemeriksaan tampilan menu agar action tidak menyusun kebijakan
 * sendiri.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code loadTreeDynamicReport()}, {@code
 * loadTree()}); operasi domain lain ({@code generateMenuHtml()}, {@code child()}, {@code buildMenuItem()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 */
public class CommonMenu {

	@SuppressWarnings("unchecked")
	public static String loadTreeDynamicReport(Tbmuser tbmuser, HttpServletRequest request, String labelMenu)
			throws Exception {
		String out = "";

		// Cegah NullPointerException jika labelMenu kosong
		if (labelMenu == null || labelMenu.isEmpty())
			return out;

		String label = labelMenu.substring(0, 1).toUpperCase() + labelMenu.substring(1).replace("_", " ");
		String reportId = request.getParameter("reportId");
		String s = (request.getParameter("s") == null) ? "" : request.getParameter("s").trim();

		// Pisahkan status aktif parent dan child agar tidak menimpa satu sama lain
		boolean isParentActive = s.equalsIgnoreCase(labelMenu);
		// Menu collapse hanya terbuka ("show") jika parent-nya aktif DAN ada report
		// yang dipilih
		boolean isAnyChildSelected = isParentActive && reportId != null;

		Session session = HibernateUtil.currentNativeSession();
		try {
			List<DynamicReport> dynamicReports = session.createCriteria(DynamicReport.class)
					.add(Restrictions.eq("aktif", true)).addOrder(Order.asc("kode")).list();

			if (!dynamicReports.isEmpty()) {
				String targetId = "laporan_" + labelMenu;
				StringBuilder sb = new StringBuilder();

				// --- 1. PARENT MENU ---
				sb.append("  <li class=\"nav-item\">\n");
				sb.append("    <a class=\"nav-link ").append(isParentActive ? "active" : "")
						.append(" dropdown-indicator\" ").append("href=\"#").append(targetId)
						.append("\" role=\"button\" ").append("data-bs-toggle=\"collapse\" aria-expanded=\"")
						.append(isAnyChildSelected).append("\" ").append("aria-controls=\"").append(targetId)
						.append("\">\n");
				sb.append("      <div class=\"d-flex align-items-center\">\n");
				sb.append("        <span class=\"nav-link-text ps-1\">").append(label).append("</span>\n");
				sb.append("      </div>\n");
				sb.append("    </a>\n");

				// --- 2. CHILD MENU (COLLAPSE) ---
				sb.append("    <ul class=\"nav collapse ").append(isAnyChildSelected ? "show" : "").append("\" id=\"")
						.append(targetId).append("\">\n");

				for (DynamicReport dynamicReport : dynamicReports) {
					String url = Common.ROOT + "/baru?p=kantin&s=" + URLEncoder.encode(labelMenu, "UTF-8")
							+ "&reportId=" + dynamicReport.getId();

					boolean isChildActive = isParentActive && reportId != null
							&& reportId.equals(dynamicReport.getId().toString());

					// PERBAIKAN: Bungkus a nav-link dengan li nav-item
					sb.append("      <li class=\"nav-item\">\n");
					sb.append("        <a class=\"nav-link ").append(isChildActive ? "active" : "").append("\" href=\"")
							.append(url).append("\" role=\"button\">\n");
					sb.append("          <div class=\"d-flex align-items-center\">\n");
					sb.append("            <span class=\"nav-link-text ps-1\">").append(dynamicReport.getKode())
							.append("</span>\n");
					sb.append("          </div>\n");
					sb.append("        </a>\n");
					sb.append("      </li>\n");
				}

				sb.append("    </ul>\n");
				sb.append("  </li>\n");

				out = sb.toString();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonMenu.java:98");
		} finally {
			try {
				if (session != null)
					session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonMenu.java:103");
				// Abaikan
			}
			try {
				if (session != null)
					session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonMenu.java:109");
				// Abaikan
			}
			HibernateUtil.closeSession();
		}

		return out;
	}

	@SuppressWarnings("unchecked")
	public static String loadTree(Tbmuser tbmuser, HttpServletRequest request) throws Exception {
		String out = "";
		Tbmrole tbmrole = tbmuser.hakAkses();
		if (tbmuser == null || tbmrole == null)
			return out;
		Sekolah sekolah = SekolahUtil.getSekolah(request);
		if (tbmuser != null && tbmuser.getSekolah() != null && tbmuser.getSekolah().getId() != null) {
			sekolah = tbmuser.getSekolah();
		}
		LogLogin login = (LogLogin) request.getSession().getAttribute("login");
		Long detailLogLoginId = login == null || login.getId() == null ? null : MainHelper.logins.get(login.getId());

		if (detailLogLoginId == null && login != null) {

			// Pencatatan DetailLogLogin best-effort: kegagalan (mis. sequence
			// detail_log_login_id_seq belum ada) WAJIB di-rollback + tutup native session
			// di finally agar tx aborted tidak meracuni refresh(tbmrole) berikutnya.
			org.hibernate.Session session1 = null;
			try {
				DetailLogLogin detailLogLogin = new DetailLogLogin();
				detailLogLogin.setKeterangan("Login");
				detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
				detailLogLogin.setLogLogin(login);

				session1 = HibernateUtil.currentNativeSession();
				session1.getTransaction().begin();
				session1.save(detailLogLogin);
				session1.getTransaction().commit();

				MainHelper.logins.put(login.getId(), detailLogLogin.getId());
			} catch (Exception e) {
//				e.printStackTrace();
				try {
					if (session1 != null && session1.getTransaction() != null
							&& session1.getTransaction().isActive()) {
						session1.getTransaction().rollback();
					}
				} catch (Exception eRoll) {
					eRoll.printStackTrace(); ais.common.ErrorAuditUtil.record(eRoll, "auto-audit src/ais/common/CommonMenu.java:157");
				}
			} finally {
				try {
					if (session1 != null) {
						session1.disconnect();
					}
				} catch (Exception eDis) { ais.common.ErrorAuditUtil.record(eDis, "auto-audit(empty-catch) src/ais/common/CommonMenu.java:164");
					// abaikan
				}
				try {
					if (session1 != null) {
						session1.close();
					}
				} catch (Exception eClose) { ais.common.ErrorAuditUtil.record(eClose, "auto-audit(empty-catch) src/ais/common/CommonMenu.java:171");
					// abaikan
				}
				try {
					HibernateUtil.closeSession();
				} catch (Exception eCs) { ais.common.ErrorAuditUtil.record(eCs, "auto-audit(empty-catch) src/ais/common/CommonMenu.java:176");
					// abaikan
				}
			}
		}

		List<Menu> menus = (List<Menu>) request.getSession().getAttribute("current_menus");

		if (menus == null) {
			Session session = HibernateUtil.currentNativeSession();
			try {
				session.refresh(tbmrole);
				menus = new ArrayList<Menu>(tbmrole.getMenus());
				Collections.sort(menus);
				request.getSession().setAttribute("current_menus", menus);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonMenu.java:192");
			} finally {
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonMenu.java:196");
					// TODO: handle exception
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonMenu.java:201");
					// TODO: handle exception
				}
				HibernateUtil.closeSession();
			}

		}

		Menu menuData = (Menu) request.getSession().getAttribute("current_menu");

		HashMap<Long, Long> parents = null;
		if (menuData != null) {
			parents = MainHelper.parents(menuData.getRoot(), menuData, menus);
		}

		out += generateMenuHtml(menus, parents, menuData, sekolah);

		return out;
	}

	/**
	 * Method utama untuk men-generate keseluruhan tag HTML dari struktur Menu.
	 * Biasanya dipanggil setelah melakukan fetch List<Menu> dari database via
	 * Hibernate.
	 * 
	 * @throws Exception
	 */
	public static String generateMenuHtml(List<Menu> menuList, HashMap<Long, Long> parents, Menu menuData,
			Sekolah sekolah) throws Exception {
		if (menuList == null || menuList.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		// Membuka tag UL utama untuk sidebar
		sb.append("<ul class=\"navbar-nav flex-column mb-3\" id=\"navbarVerticalNav\">\n");

		boolean aktifkanFIlterPerSekolah = Common.bolehKonfigurasi("aktifkan_filter_per_sekolah", Konfigurasi.TIDAK_AKTIF);

		for (Menu menu : menuList) {

			if (menu.getRoot().equals(0L)) {
				if (aktifkanFIlterPerSekolah) {
					if (((sekolah == null || sekolah.getId() == null)
							&& !menu.getLabel().equalsIgnoreCase("Sistem Sekolah"))
							|| (sekolah != null && sekolah.getId() != null
									&& !menu.getLabel().equalsIgnoreCase("Sistem Informasi Akademik"))) {
						buildMenuItem(sb, menu, 0, menuList, parents, menuData);
					}
				} else {
					buildMenuItem(sb, menu, 0, menuList, parents, menuData);
				}
			}

		}

		sb.append("</ul>\n");
		return sb.toString();
	}

	public static List<Menu> child(Long root, List<Menu> menus) {
		List<Menu> listChild = new ArrayList<Menu>();
		for (Menu menu : menus) {
			if (menu.getRoot().equals(root)) {
				listChild.add(menu);
			}
		}
		return listChild;
	}

	/**
	 * Method rekursif untuk menyusun node root dan children. * @param sb
	 * StringBuilder penampung HTML
	 * 
	 * @param menu  Object Menu saat ini
	 * @param level Tingkat kedalaman menu (0 = root)
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private static void buildMenuItem(StringBuilder sb, Menu menu, int level, List<Menu> menuList,
			HashMap<Long, Long> parents, Menu menuData) throws Exception {
		if (menu.getAktif() != null && !menu.getAktif()) {
			return;
		}

		boolean aktif = menuData != null && menuData.getId().equals(menu.getId());

		boolean pilih = false;
		if (parents != null) {
			pilih = parents.keySet().contains(menu.getId());
		}
		// Asumsi: Class Menu memiliki relasi struktur tree (self-referencing
		// @OneToMany)
		// Jika list masih flat, perlu di-convert menjadi hirarki / tree list terlebih
		// dahulu.
		List<Menu> children = child(menu.getChild(), menuList); // Ganti dengan getter child list yang sesuai di entity
																// Anda
		boolean hasChildren = children != null && !children.isEmpty();
		String m = "";

		try {
			m = menu.getUrl().replaceAll("\\p{Punct}", "");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonMenu.java:303");
			// TODO: handle exception
		}
		String label = menu.getLabel() != null ? menu.getLabel() : "Menu";
		String url = Common.ROOT + "/baru?p=" + URLEncoder.encode(m, "UTF-8") + "&menu=" + menu.getId();
		// String icon = menu.getBigIcon(); // contoh: "fas fa-chart-pie"
		String icon = null;
		// Membuat ID unik yang aman (hanya alphanumeric) untuk target collapse
		// Bootstrap
		// Menambahkan ID database untuk memastikan tidak ada ID duplikat di HTML
		String targetId = "menu_" + menu.getId();

		// --- Skenario Standar ---
		sb.append("  <li class=\"nav-item\">\n");

		if (hasChildren) {
			// Tampilan untuk menu dropdown (Parent yang memiliki inner pages)
			sb.append("    \n");
			sb.append("    <a class=\"nav-link " + (aktif ? "active" : "") + " dropdown-indicator\" href=\"#")
					.append(targetId).append("\" role=\"button\" data-bs-toggle=\"collapse\" aria-expanded=\"" + pilih
							+ "\" aria-controls=\"")
					.append(targetId).append("\">\n");

			sb.append("      <div class=\"d-flex align-items-center\">\n");

			// Hanya men-generate tag icon jika menu berada di level teratas dan icon-nya
			// ada
			if (level == 0 && icon != null && !icon.isEmpty()) {
				sb.append("        <span class=\"nav-link-icon\"><span class=\"").append(icon)
						.append("\"></span></span>\n");
			}
			sb.append("        <span class=\"nav-link-text ps-1\">").append(label).append("</span>\n");
			sb.append("      </div>\n");
			sb.append("    </a>\n");

			// Membuka ul collapse untuk inner child
			sb.append("    <ul class=\"nav collapse" + (pilih ? " show" : "") + "\" id=\"").append(targetId)
					.append("\">\n");

			// Rekursi untuk inner pages
			for (Menu child : children) {
				buildMenuItem(sb, child, level + 1, menuList, parents, menuData);
			}

			sb.append("    </ul>\n");

		} else {
			// Tampilan untuk link statis tunggal (tanpa dropdown)
			sb.append("    <a class=\"nav-link " + (aktif ? "active" : "") + "\" href=\"").append(url)
					.append("\" role=\"button\">\n");
			sb.append("      <div class=\"d-flex align-items-center\">\n");

			if (level == 0 && icon != null && !icon.isEmpty()) {
				sb.append("        <span class=\"nav-link-icon\"><span class=\"").append(icon)
						.append("\"></span></span>\n");
			}
			sb.append("        <span class=\"nav-link-text ps-1\">").append(label).append("</span>\n");
			sb.append("      </div>\n");
			sb.append("    </a>\n");
		}

		sb.append("  </li>\n");
	}
}