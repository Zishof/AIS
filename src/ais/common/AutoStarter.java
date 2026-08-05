package ais.common;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.North;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.West;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.Konfigurasi;
import ais.database.model.LogLogin;
import ais.database.model.Menu;
import ais.database.model.RolePrivilage;
import ais.database.model.Tbmuser;

public class AutoStarter {

	public static void autoStartManajemenKRS() {
		Konfigurasi dosen = Common.getKonfigurasi("dosen_langsung_ke_manajemen_krs", Konfigurasi.AKTIF);
		if (dosen.getNilai().equalsIgnoreCase(Konfigurasi.AKTIF)) {
			final Page page = ExecutionsCtrl.getCurrentCtrl().getCurrentPage();
			final Tbmuser tbmuser = Common.getCurrentUser();

			if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				final LogLogin login = (LogLogin) Sessions.getCurrent().getAttribute("login");

				Timer timer = new Timer();
				timer.setParent(page.getFirstRoot());
				timer.setDelay(500);

				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						try {
							Menu menu = ConstantValues.MENU_MANAJEMEN_KRS_DOSEN;
							Session session = HibernateUtil.currentSession();
							RolePrivilage rolePrivilage = (RolePrivilage) ConstantValues
									.simpleObject(
											session.createCriteria(RolePrivilage.class)
													.add(Restrictions.eq("role", tbmuser.hakAkses()))
													.add(Restrictions.eq("menu", menu)).setMaxResults(1),
											RolePrivilage.class);
							if (rolePrivilage == null) {
								rolePrivilage = new RolePrivilage();
								rolePrivilage.setRole(tbmuser.hakAkses());
							}
							rolePrivilage.setCreate(1);
							rolePrivilage.setDelete(1);
							rolePrivilage.setRead(1);
							rolePrivilage.setUpdate(1);
							session.saveOrUpdate(rolePrivilage);

							Sessions.getCurrent().setAttribute("currentMenu", menu);

							DetailLogLogin detailLogLogin = new DetailLogLogin();
							detailLogLogin.setKeterangan(menu.getLabel());
							detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
							detailLogLogin.setLogLogin(login);

							try {
								session = HibernateUtil.currentNativeSession();
								session.getTransaction().begin();
								session.save(detailLogLogin);
								session.getTransaction().commit();
								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
								HibernateUtil.closeSession();

								Sessions.getCurrent().setAttribute("detailLogLogin", detailLogLogin);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AutoStarter.java:81");
							}

							Tabbox iframe = (Tabbox) Sessions.getCurrent().getAttribute("iframe");
							Common.insertToTab(null, null, iframe, menu, login);

							West navigation = (West) Sessions.getCurrent().getAttribute("navigation");
							North mycenter = (North) Sessions.getCurrent().getAttribute("mycenter");

							if (navigation != null) {
								navigation.setWidth("250px");
							}

							if (mycenter != null) {
								mycenter.detach();
								Sessions.getCurrent().removeAttribute("mycenter");
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AutoStarter.java:98");
						}

					}
				});
				timer.start();

			}

		}
	}


}
