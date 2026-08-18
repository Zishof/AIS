package ais.action.master.helper;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.zkoss.zk.ui.Sessions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.LogLogin;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

public class MainBaruMenuHelper {

	public static String loadTree(Tbmuser tbmuser, HttpServletRequest request) throws Exception {
		String out = "";
		if (tbmuser == null || tbmuser.hakAkses() == null || tbmuser.hakAkses().getMenus() == null)
			return out;

		LogLogin login = (LogLogin) request.getSession().getAttribute("login");
		Long detailLogLoginId = login == null || login.getId() == null ? null : MainHelper.logins.get(login.getId());

		if (detailLogLoginId == null) {

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
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainBaruMenuHelper.java:50");
				try {
					if (session1 != null && session1.getTransaction() != null
							&& session1.getTransaction().isActive()) {
						session1.getTransaction().rollback();
					}
				} catch (Exception eRoll) {
					eRoll.printStackTrace(); ais.common.ErrorAuditUtil.record(eRoll, "auto-audit src/ais/action/master/helper/MainBaruMenuHelper.java:57");
				}
			} finally {
				try {
					if (session1 != null) {
						session1.disconnect();
					}
				} catch (Exception eDis) { ais.common.ErrorAuditUtil.record(eDis, "auto-audit(empty-catch) src/ais/action/master/helper/MainBaruMenuHelper.java:64");
					// abaikan
				}
				try {
					if (session1 != null) {
						session1.close();
					}
				} catch (Exception eClose) { ais.common.ErrorAuditUtil.record(eClose, "auto-audit(empty-catch) src/ais/action/master/helper/MainBaruMenuHelper.java:71");
					// abaikan
				}
				try {
					HibernateUtil.closeSession();
				} catch (Exception eCs) { ais.common.ErrorAuditUtil.record(eCs, "auto-audit(empty-catch) src/ais/action/master/helper/MainBaruMenuHelper.java:76");
					// abaikan
				}
			}
		}

		Tbmrole tbmrole = tbmuser.hakAkses();
		
		List<Menu> menus = (List<Menu>) Sessions.getCurrent().getAttribute("current_menus");

		if (menus == null) {
			Session session = HibernateUtil.currentNativeSession();
			try {
				session.refresh(tbmrole);
				menus = new ArrayList<Menu>(tbmrole.getMenus());
				Collections.sort(menus);
				Sessions.getCurrent().setAttribute("current_menus", menus);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainBaruMenuHelper.java:94");
			} finally {
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MainBaruMenuHelper.java:98");
					// TODO: handle exception
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MainBaruMenuHelper.java:103");
					// TODO: handle exception
				}
				HibernateUtil.closeSession();
			}

		}
		
		
		request.getSession().setAttribute("users", tbmuser);

		out += createTreeMenu(tbmrole, menus, tbmuser, login, request);

		return out;
	}

	public static String createTreeMenu(Tbmrole job, List<Menu> menus, Tbmuser tbmuser, LogLogin login, HttpServletRequest request)
			throws Exception {
		String out = "";

		request.getSession().setAttribute("currentMenus", menus);

		Menu menuData = (Menu) request.getSession().getAttribute("current_menu");
		HashMap<Long, Long> parents = null;
		if (menuData != null) {
			parents = MainHelper.parents(menuData.getRoot(), menuData, menus);
		}
//		System.out.println("parents -> " + parents);

		for (final Menu menu : menus) {
			if (menu.getAktif() != null && !menu.getAktif()) {
				continue;
			}
			if (menu.getRoot().equals(0L)) {
				String m = "";

				try {
					m = menu.getUrl().replaceAll("\\p{Punct}", "");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MainBaruMenuHelper.java:141");
					// TODO: handle exception
				}

				Boolean ada = MainHelper.hasChild(menu.getChild(), menus);
				if (ada) {

					boolean pilih = false;
					if (parents != null) {
						pilih = parents.keySet().contains(menu.getId());
					}

					out += "<a class=\"nav-link dropdown-indicator\" href=\"#multi-level-" + menu.getId()
							+ "\" role=\"button\" data-bs-toggle=\"collapse\" aria-expanded=\"" + pilih
							+ "\" aria-controls=\"multi-level\">\r\n"
							+ "                    <div class=\"d-flex align-items-center\"><span class=\"nav-link-icon\"><span class=\"fas fa-layer-group\"></span></span><span class=\"nav-link-text ps-1\">"
							+ ais.common.Common.getBahasaConfig(menu.getLabel()) + "</span>\r\n" + "                    </div>\r\n"
							+ "                  </a>";

					out += "<ul class=\"nav collapse " + (pilih ? "show" : "") + "\" id=\"multi-level-" + menu.getId()
							+ "\">";

					out += createRootSubMenu(menu.getChild(), menus, tbmuser, login, request);

					out += "</ul>";
				} else {
					out += "<li class=\"nav-item\"><a class=\"nav-link "
							+ (menuData != null && menuData.getId().equals(menu.getId()) ? "active" : "") + "\" href=\""
							+ request.getContextPath() + "/baru?p=" + URLEncoder.encode(m, "UTF-8") + "&menu="
							+ menu.getId() + "\">\r\n"
							+ "                            <div class=\"d-flex align-items-center\"><span class=\"nav-link-text ps-1\">"
							+ ais.common.Common.getBahasaConfig(menu.getLabel()) + "</span>\r\n" + "                            </div>\r\n"
							+ "                          </a>\r\n" + "                        </li>";
					
					
					out += "<a class=\"nav-link\" href=\"pages/starter.html\" role=\"button\">\r\n"
							+ "                    <div class=\"d-flex align-items-center\"><span class=\"nav-link-icon\"><span class=\"fas fa-flag\"></span></span><span class=\"nav-link-text ps-1\">Starter</span>\r\n"
							+ "                    </div>\r\n"
							+ "                  </a>";
				}

			}
		}

		return out;
	}

	private static String createRootSubMenu(Long root, List<Menu> menus, Tbmuser tbmuser, LogLogin login,
			HttpServletRequest request) throws Exception {
		String out = "";
		Menu menuData = (Menu) request.getSession().getAttribute("current_menu");

		HashMap<Long, Long> parents = null;
		if (menuData != null) {
			parents = MainHelper.parents(menuData.getRoot(), menuData, menus);
		}
//		System.out.println("parents -> " + parents);
		for (final Menu menu : menus) {
			if (menu.getAktif() != null && !menu.getAktif()) {
				continue;
			}
			if (menu.getRoot().equals(root)) {
				String m = "";

				try {
					m = menu.getUrl().replaceAll("\\p{Punct}", "");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MainBaruMenuHelper.java:207");
					// TODO: handle exception
				}

				Boolean ada = MainHelper.hasChild(menu.getChild(), menus);
				if (ada) {

					boolean pilih = false;
					if (parents != null) {
						pilih = parents.keySet().contains(menu.getId());
					}

					out += "<li class=\"nav-item\">" + "<a class=\"nav-link dropdown-indicator\" href=\"#level-two-"
							+ menu.getId() + "\" data-bs-toggle=\"collapse\" aria-expanded=\"" + pilih
							+ "\" aria-controls=\"multi-level\">\r\n"
							+ "                        <div class=\"d-flex align-items-center\"><span class=\"nav-link-text ps-1\">"
							+ ais.common.Common.getBahasaConfig(menu.getLabel()) + "</span>\r\n" + "                        </div>\r\n"
							+ "                      </a>";

					out += "<ul class=\"nav collapse " + (pilih ? "show" : "") + "\" id=\"level-two-" + menu.getId()
							+ "\">";

					out += createRootSubMenu(menu.getChild(), menus, tbmuser, login, request);

					out += "</ul>\n</li>";
				} else {
					out += "<li class=\"nav-item\"><a class=\"nav-link "
							+ (menuData != null && menuData.getId().equals(menu.getId()) ? "active" : "") + "\" href=\""
							+ request.getContextPath() + "/baru?p=" + URLEncoder.encode(m, "UTF-8") + "&menu="
							+ menu.getId() + "\">\r\n"
							+ "                            <div class=\"d-flex align-items-center\"><span class=\"nav-link-text ps-1\">"
							+ ais.common.Common.getBahasaConfig(menu.getLabel()) + "</span>\r\n" + "                            </div>\r\n"
							+ "                          </a>\r\n"
							+ "                          <!-- more inner pages-->\r\n"
							+ "                        </li>";
				}

			}
		}

		return out;
	}

}
