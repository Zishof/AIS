package ais.action.maintenance;

import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Center;
import org.zkoss.zul.Image;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.LogLogin;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

public class BlankAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1783885254736882086L;

	// private Image utama_image;
	// private Tabs mytabs;
	// private Tabpanels mytabpanels;

	private Center center;
	private LogLogin login;
	private Rows myRows;

	private Tbmuser tbmuser;
	@SuppressWarnings("unused")
	private Image utama_image;

	private TreeSet<Menu> menus;

	private Textbox cari;
	private North north;

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		try {
			Common.REAL_PATH = session.getWebApp().getRealPath("/");
			Common.initTemp();
			CommonMedia.getMediaDirectory();
			Common.REAL_PATH_REPORT_TEMP = session.getWebApp().getRealPath("/report");
			CommonMedia.getMediaDirectory();
			tbmuser = Common.getCurrentFromSpringUser();
			if (tbmuser == null || tbmuser.getUserId() == null) {
				return;
			}

			// Integer point = 100;

			// Konfigurasi konfigurasi = Common.checkKonfigurasiBigIcon();
			// if (konfigurasi.getInfo1().equalsIgnoreCase("true")) {
			// north.setVisible(true);
			// initBigIcon();
			// } else {
			// north.setVisible(false);
			// }

			north.setVisible(true);
			initBigIcon();

			HttpServletRequest request = (HttpServletRequest) execution.getNativeRequest();
			String req = request.getRequestURL().toString();
			req = req.replaceAll("http://", "");
			req = req.split(":")[0];
			req = req.split("/")[0];
			System.out.println("URL = " + req);

			// if (tbmuser != null &&
			// Common.getKonfigurasi("tampil_dashbord_di_panel_utama",
			// Konfigurasi.TIDAK_AKTIF)
			// .getNilai().equals(Konfigurasi.AKTIF)) {
			//
			// Tbmrole tbmrole = tbmuser.hakAkses();
			// List<RoleHasDashboard> roleHasDashboards =
			// HibernateUtil.currentSession()
			// .createCriteria(RoleHasDashboard.class).addOrder(Order.asc("name"))
			// .add(Restrictions.eq("userRole", tbmrole)).list();
			//
			// System.out.println("roleHasDashboards = " + roleHasDashboards);
			//
			// for (final RoleHasDashboard roleHasDashboard : roleHasDashboards)
			// {
			// final MyTabConfig tab;
			// mytabs.appendChild(tab = new
			// MyTabConfig(roleHasDashboard.getName()));
			// final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			// tabpanel.setHeight("100%");
			// tabpanel.setWidth("100%");
			// tabpanel.setParent(mytabpanels);
			// tab.addEventListener(Events.ON_CLICK, new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// if (tabpanel.getChildren().size() == 0) {
			// tabpanel.appendChild(
			// (Component)
			// Class.forName(roleHasDashboard.getDashboard()).newInstance());
			// }
			//
			// }
			// });
			// }
			//
			// }
		} catch (Exception e) {

			Common.tampilErrorJikaAdmin(e);
		}

		// AutoStarter.autoStartAktifitasPerkuliahan();
		// AutoStarter.autoStartManajemenKRS();
	}

	private void initBigIcon() {
		Common.clear(center);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("border:0px;background: transparent;");

		myRows = new Rows();
		myRows.setParent(grid);

		if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null) {
			Tbmrole tbmrole = tbmuser.hakAkses();
			Set<Menu> mainMenus = tbmrole.getMenus();
			menus = new TreeSet<Menu>(mainMenus);
		}

		utama_image = new Image("/img/background-white.jpg");

		onCari(null);
	}

	public void onCari(Event event) {
		if (menus != null) {
			Common.clear(myRows);
			int i = 0;
			MyFormRow row = new MyFormRow();row.setValign("top");
			myRows.appendChild(row);
			row.setHeight("100px");
			row.setWidth("100%");
			row.setAlign("center");
			for (final Menu menu : menus) {
				if (menu.getAktif() != null && !menu.getAktif()) {
					continue;
				}
				if (menu.getUrl() == null || menu.getUrl().trim().equals("") || menu.getBigIcon() == null
						|| menu.getBigIcon().trim().equals("")) {
					continue;
				}
				if (menu.getLabel() == null
						|| !menu.getLabel().trim().toLowerCase().contains(cari.getValue().trim().toLowerCase())) {
					continue;
				}

				if (i == 2) {
					i = 0;
					row = new MyFormRow();
					myRows.appendChild(row);
					row.setHeight("100px");
					row.setWidth("100%");
					row.setAlign("center");
				}
				MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(menu.getLabel(),
						menu.getBigIcon() == null || menu.getBigIcon().trim().equals("") ? "/img/none-icon.png"
								: menu.getBigIcon().trim());
				toolbarbutton.setOrient("vertical");
				toolbarbutton.setWidth("100%");
				row.appendChild(toolbarbutton);
				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Sessions.getCurrent().setAttribute("currentMenu", menu);

						// if (tampilWindow(menu)) {
						// return;
						// }

						DetailLogLogin detailLogLogin = new DetailLogLogin();
						detailLogLogin.setKeterangan(menu.getLabel());
						detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
						detailLogLogin.setLogLogin(login);

						try {
							Session session = HibernateUtil.currentNativeSession();
							session.getTransaction().begin();
							session.save(detailLogLogin);
							session.getTransaction().commit();
							// session.disconnect();
							if (session.isOpen()) {session.disconnect();session.close();}
							HibernateUtil.closeSession();

							Sessions.getCurrent().setAttribute("detailLogLogin", detailLogLogin);

							Common.launchMenu(null, null, menu, login);
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
							PesanFormalHelper.tampilkanGagalException("pembukaan menu \"" + menu.getLabel() + "\"", e,
									new String[] {
											"Muat ulang halaman utama (main), kemungkinan sesi/koneksi ke server sempat terputus.",
											"Coba klik kembali menu tersebut beberapa saat lagi.",
											"Jika menu ini tetap tidak dapat dibuka, hubungi Administrator Sistem." });
						}

					}
				});
				i++;
			}
		}
	}

	// private boolean tampilWindow(Menu menu) {
	// try {
	// login = (LogLogin) session.getAttribute("login");
	// // if (menu.getUrl().trim().startsWith("ais.action.report.window"))
	// // {
	// MyWindow window = (MyWindow)
	// Class.forName(menu.getUrl().trim()).newInstance();
	// // window.setTitle(menu.getLabel());
	// window.setVisible(true);
	// window.setHeight("100%");
	// // window.setClosable(true);
	// window.setWidth("100%");
	// // Common.insertToTab((Tabbox) session.getAttribute("iframe"),
	// // menu, window, login);
	// // } else {
	// // MyWindow window = (MyWindow) Class.forName(menu.getUrl().trim())
	// // .newInstance();
	// // ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
	// // .appendChild(window);
	// // window.setTitle(menu.getLabel());
	// window.setVisible(true);
	// // window.onModal();
	// Common.insertToTab((Tabbox) session.getAttribute("iframe"), menu, window,
	// login);
	// // }
	// return true;
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/BlankAction.java:261");
	// // Common.tampilErrorJikaAdmin(e);
	// return false;
	// }
	// }

}
