package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.Konfigurasi;
import ais.database.model.LogLogin;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;

public class MainTreeMenuHelper {

	@SuppressWarnings("unchecked")
	public static void loadTree(Tbmuser tbmuser, final LogLogin login, Tree menubar, EventListener clickEventListener,
			Tabbox iframe, West navigasi, MyToolbarbuttonConfig menuService, boolean pt, boolean ya) {

		if (tbmuser == null || tbmuser.hakAkses() == null || tbmuser.hakAkses().getMenus() == null)
			return;

		Long detailLogLoginId = login == null || login.getId() == null ? null : MainHelper.logins.get(login.getId());

		if (detailLogLoginId == null && login != null) {

			org.hibernate.Session session1 = null;
			try {
				DetailLogLogin detailLogLogin = new DetailLogLogin();
				detailLogLogin.setKeterangan("Login");
				detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
				detailLogLogin.setLogLogin(login);

				session1 = HibernateUtil.openSession();
				session1.beginTransaction();
				/* DetailLogLogin mempunyai FK ke LogLogin. Pada login yang baru dibuat,
				 * objek induk kadang belum di-flush sehingga masih transient. Persist dan
				 * flush induk lebih dulu dalam transaksi terisolasi. */
				if (login.getId() == null) {
					session1.save(login);
					session1.flush();
				}
				session1.save(detailLogLogin);
				session1.getTransaction().commit();

				MainHelper.logins.put(login.getId(), detailLogLogin.getId());
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainTreeMenuHelper.java:55");
				// PENTING: kegagalan pencatatan DetailLogLogin (mis. sequence DB
				// "detail_log_login_id_seq" belum ter-wire sebagai default kolom id) TIDAK
				// boleh meninggalkan transaksi dalam keadaan aborted. currentNativeSession()
				// berbagi sesi thread-bound; bila transaksi tak di-rollback & sesi tak
				// dilepas, refresh tbmrole di bawah ikut gagal ("current transaction is
				// aborted, commands ignored...") sehingga menu tidak termuat & halaman utama
				// rusak. Rollback + tutup sesi agar pemanggilan berikutnya dapat sesi bersih.
				try {
					if (session1 != null && session1.getTransaction() != null
							&& session1.getTransaction().isActive()) {
						session1.getTransaction().rollback();
					}
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/MainTreeMenuHelper.java:68");
				}
			} finally {
				HibernateUtil.closeSessionQuietly(session1);
			}
		}

		Tbmrole tbmrole = tbmuser.hakAkses();

		List<Menu> menus = (List<Menu>) Sessions.getCurrent().getAttribute("current_menus");

		if (menus == null) {
			/* KE-FIX (SessionException "Session is closed!" di PersistentSet.toArray <-
			 * new ArrayList<Menu>(tbmrole.getMenus()) <- MainAction.initPencarianMenuJikaKosong):
			 * tbmrole berasal dari atribut ZK session, jadi objeknya DETACHED dan koleksi
			 * lazy `menus` miliknya masih terikat ke Session Hibernate LAMA yang sudah ditutup.
			 * Membungkusnya dengan new ArrayList(...) memaksa inisialisasi koleksi itu memakai
			 * session yang sudah mati -> exception -> `menus` tetap null -> MENU UTAMA KOSONG
			 * saat halaman dimuat. session.refresh(tbmrole) di sini tidak dapat diandalkan
			 * memasang ulang wrapper koleksinya pada instance detached tersebut.
			 *
			 * Sekarang daftar menu diambil EKSPLISIT lewat query pada session yang hidup, jadi
			 * tidak ada koleksi lazy yang perlu diinisialisasi belakangan. Hasilnya List<Menu>
			 * biasa (bukan PersistentSet) sehingga tetap aman dipakai setelah session ditutup,
			 * persis seperti nilai yang di-cache di atribut ZK "current_menus". Bila query pun
			 * gagal, menus di-fallback ke list KOSONG yang sudah terinisialisasi supaya kerangka
			 * menu & item "Bantuan"/"Keluar Aplikasi" tetap ter-render (halaman tidak rusak);
			 * fallback ini sengaja TIDAK di-cache ke ZK session supaya pemuatan berikutnya
			 * mencoba lagi. Session dibuka manual (openSession) sehingga WAJIB ditutup di
			 * finally lewat closeSessionQuietly (clear + disconnect + close). */
			Session session = HibernateUtil.openSession();
			try {
				List<Menu> dimuat = null;
				String roleId = tbmrole.getRoleId();
				if (roleId != null) {
					try {
						dimuat = session
								.createQuery("select distinct m from Tbmrole r join r.menus m where r.roleId = :roleId")
								.setString("roleId", roleId).list();
					} catch (Exception eQuery) {
						// Cadangan: muat ulang role lewat session yang HIDUP, lalu inisialisasi
						// koleksinya SELAGI session masih terbuka. Instance hasil get() terikat ke
						// session ini (bukan ke session lama yang sudah ditutup), jadi aman.
						ais.common.ErrorAuditUtil.record(eQuery,
								"auto-audit src/ais/action/master/helper/MainTreeMenuHelper.java:menu-query");
						Tbmrole segar = (Tbmrole) session.get(Tbmrole.class, roleId);
						dimuat = segar == null || segar.getMenus() == null ? null
								: new ArrayList<Menu>(segar.getMenus());
					}
				}
				menus = dimuat == null ? new ArrayList<Menu>() : new ArrayList<Menu>(dimuat);
				Collections.sort(menus);
				Sessions.getCurrent().setAttribute("current_menus", menus);
			} catch (Exception e) {
				// Menu utama TIDAK BOLEH menggagalkan halaman: pakai daftar kosong yang sudah
				// terinisialisasi (bukan null / bukan koleksi lazy) agar tree tetap terbentuk.
				menus = new ArrayList<Menu>();
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainTreeMenuHelper.java:95");
			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}

		}

		Sessions.getCurrent().setAttribute("users", tbmuser);
		menubar.setSclass("menu_item");
		Treechildren tc1 = createTreeMenu(menubar, tbmrole, menus, tbmuser, login, clickEventListener, iframe, navigasi,
				menuService, pt, ya);

		if (Common.bolehKonfigurasi("tampilkan_menu_bantuan")) {
			MyTreeitemConfig menuitem = new MyTreeitemConfig(
					tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleId(), "Bantuan",
					"bantuan");
			menuitem.setImage("/img/help.gif");

			menuitem.setOpen(false);
			menuitem.setParent(tc1);
			menuitem.addEventListener("onClick", new EventListener() {
				public void onEvent(Event event) throws Exception {
					MainHelper.onKatalogBantuan(login);
				}
			});
		}

		MyTreeitemConfig menuitem = new MyTreeitemConfig(
				tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleId(), "Keluar Aplikasi",
				"");
		menuitem.setOpen(false);
		menuitem.setImage("/img/shutdown.PNG");
		menuitem.setParent(tc1);
		menuitem.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) throws Exception {
				MainHelper.onKeluar(login);
			}
		});

	}

	public static Treechildren createTreeMenu(Tree menubar, Tbmrole job, List<Menu> menus, Tbmuser tbmuser,
			final LogLogin login, final EventListener clickEventListener, final Tabbox iframe, final West navigasi,
			final MyToolbarbuttonConfig menuService, boolean pt, boolean ya) {

		Sessions.getCurrent().setAttribute("currentMenus", menus, true);
		Treechildren tc1 = new Treechildren();

		tc1.setParent(menubar);
		if (menus == null) {
			return tc1;
		}
		for (final Menu menu : menus) {
			if ((!pt && !ya) || (pt && menu.getTampilDiPt()) || (ya && menu.getTampilDiSekolah())) {
				if (menu.getAktif() != null && !menu.getAktif()) {
					continue;
				}
				if (menu.getRoot().equals(0L)) {

					Boolean ada = MainHelper.hasChild(menu.getChild(), menus);
					if (ada) {
						final MyTreeitemConfig treeitem = new MyTreeitemConfig(job.getRoleId(), menu.getLabel(),
								menu.getUrl());
						treeitem.setParent(tc1);
						treeitem.setImage("/img/svg/folder-open-thin.svg");
						treeitem.setOpen(false);
						treeitem.addEventListener("onClick", new EventListener() {
							public void onEvent(Event event) throws Exception {
								treeitem.setOpen(treeitem.isOpen() ? false : true);
								Sessions.getCurrent().setAttribute("treeitem", treeitem);
							}
						});
						createRootSubMenu(menu.getChild(), menus, treeitem, tbmuser, login, clickEventListener, iframe,
								navigasi, menuService, pt, ya);
					} else {
						final MyTreeitemConfig treeitem = new MyTreeitemConfig(job.getRoleId(), menu.getLabel(),
								menu.getUrl());
						treeitem.setParent(tc1);
						treeitem.setImage("/img/svg/chevron-right.svg");
						treeitem.setOpen(false);
						treeitem.addEventListener("onClick", new EventListener() {
							public void onEvent(Event event) throws Exception {

								clickEventListener.onEvent(event);

								try {

									Sessions.getCurrent().setAttribute("currentMenu", menu);

									DetailLogLogin detailLogLogin = new DetailLogLogin();
									detailLogLogin.setKeterangan(menu.getLabel());
									detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
									detailLogLogin.setLogLogin(login);
									Session session = HibernateUtil.openSession();
									org.hibernate.Transaction tx = null;
									try {
										tx = session.beginTransaction();
										session.save(detailLogLogin);
										tx.commit();
										Sessions.getCurrent().setAttribute("detailLogLogin", detailLogLogin);
									} catch (Exception e) {
										try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainTreeMenuHelper.java:210");
									} finally {
										HibernateUtil.closeSessionQuietly(session);
									}

									Sessions.getCurrent().setAttribute("treeitem", treeitem);
									Common.insertToTab(navigasi, menuService, iframe, menu, login);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									try {
										PesanFormalHelper.tampilkanGagalException("Membuka menu", "Menu yang Bapak/Ibu pilih gagal dimuat ke dalam frame aplikasi, kemungkinan karena sesi/koneksi ke server sempat terputus sesaat atau terjadi kendala saat memuat komponen menu tersebut.", e, new String[]{"Segarkan (refresh) halaman browser Anda, lalu buka kembali menu ini.", "Pastikan koneksi internet Anda stabil sebelum mencoba kembali.", "Jika menu ini tetap tidak dapat dibuka setelah beberapa kali percobaan, hubungi Administrator."});
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/MainTreeMenuHelper.java:225");
									}

								}

							}
						});
					}
				}
			}
		}
		return tc1;
	}

	private static void createRootSubMenu(Long root, List<Menu> menus, MyTreeitemConfig parenttreeitem, Tbmuser tbmuser,
			final LogLogin login, final EventListener clickEventListener, final Tabbox iframe, final West navigasi,
			final MyToolbarbuttonConfig menuService, boolean pt, boolean ya) {
		Treechildren tc1 = new Treechildren();

		tc1.setParent(parenttreeitem);
		for (final Menu menu : menus) {
			if ((!pt && !ya) || (pt && menu.getTampilDiPt()) || (ya && menu.getTampilDiSekolah())) {
				if (menu.getAktif() != null && !menu.getAktif()) {
					continue;
				}
				if (menu.getRoot().equals(root)) {
					Boolean ada = MainHelper.hasChild(menu.getChild(), menus);
					if (ada) {
						final MyTreeitemConfig treeitem = new MyTreeitemConfig(
								tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleId(),
								menu.getLabel(), menu.getUrl());
						treeitem.setOpen(false);
						treeitem.setParent(tc1);
						treeitem.setImage("/img/svg/folder-open-thin.svg");
						treeitem.addEventListener("onClick", new EventListener() {
							public void onEvent(Event event) throws Exception {
								treeitem.setOpen(treeitem.isOpen() ? false : true);
								Sessions.getCurrent().setAttribute("treeitem", treeitem);
							}
						});
						createRootSubMenu(menu.getChild(), menus, treeitem, tbmuser, login, clickEventListener, iframe,
								navigasi, menuService, pt, ya);
					} else {
						final MyTreeitemConfig treeitem = new MyTreeitemConfig(
								tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleId(),
								menu.getLabel(), menu.getUrl());
						treeitem.setOpen(false);
						treeitem.setParent(tc1);
						treeitem.setImage("/img/svg/chevron-right.svg");
						treeitem.addEventListener("onClick", new EventListener() {
							@SuppressWarnings({})
							public void onEvent(Event event) throws Exception {

								clickEventListener.onEvent(event);

								Sessions.getCurrent().setAttribute("currentMenu", menu);

								DetailLogLogin detailLogLogin = new DetailLogLogin();
								detailLogLogin.setKeterangan(menu.getLabel());
								detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
								detailLogLogin.setLogLogin(login);
								detailLogLogin.setHalaman(menu.getUrl());

								Session session = HibernateUtil.openSession();
								org.hibernate.Transaction tx = null;
								try {
									tx = session.beginTransaction();
									session.save(detailLogLogin);
									tx.commit();
									Sessions.getCurrent().setAttribute("detailLogLogin", detailLogLogin);
								} catch (Exception e) {
									try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainTreeMenuHelper.java:300");
								} finally {
									HibernateUtil.closeSessionQuietly(session);
								}

								Common.launchMenu(navigasi, menuService, iframe, menu, login);

								Sessions.getCurrent().setAttribute("treeitem", treeitem);
							}
						});
					}
				}
			}
		}

	}

}
