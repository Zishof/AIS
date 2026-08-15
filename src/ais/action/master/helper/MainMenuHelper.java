package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.Konfigurasi;
import ais.database.model.LogLogin;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMenu;
import ais.ui.util.MyMenuitem;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class MainMenuHelper {

	@SuppressWarnings("unchecked")
	public static void loadMenuCari(Tbmuser tbmuser, final LogLogin login, Row rowDicari,
			final EventListener clickEventListener, final Tabbox iframe, final West navigasi,
			final MyToolbarbuttonConfig menuService, String cari) {
		List<Menu> menus = (List<Menu>) Sessions.getCurrent().getAttribute("currentMenus");
		if (menus == null)
			return;

		Long detailLogLoginId = login == null || login.getId() == null ? null : MainHelper.logins.get(login.getId());

		if (detailLogLoginId == null && login != null && login.getId() != null) {

			// Pencatatan DetailLogLogin bersifat best-effort: kegagalannya TIDAK boleh
			// merusak pemuatan menu. Bila save gagal (mis. sequence detail_log_login_id_seq
			// belum ada di DB), WAJIB rollback + tutup native session di finally agar
			// transaksi yang aborted tidak tertinggal & meracuni operasi berikutnya
			// (mencegah cascade "current transaction is aborted ... menu tidak muncul").
			org.hibernate.Session session1 = null;
			try {
				session1 = HibernateUtil.currentNativeSession();
				LogLogin loginPersisten = (LogLogin) session1.get(LogLogin.class, login.getId());
				if (loginPersisten != null) {
					DetailLogLogin detailLogLogin = new DetailLogLogin();
					detailLogLogin.setKeterangan("Login");
					detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
					detailLogLogin.setLogLogin(loginPersisten);
					session1.getTransaction().begin();
					session1.save(detailLogLogin);
					session1.getTransaction().commit();

					MainHelper.logins.put(login.getId(), detailLogLogin.getId());
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainMenuHelper.java:67");
				try {
					if (session1 != null && session1.getTransaction() != null
							&& session1.getTransaction().isActive()) {
						session1.getTransaction().rollback();
					}
				} catch (Exception eRoll) {
					eRoll.printStackTrace(); ais.common.ErrorAuditUtil.record(eRoll, "auto-audit src/ais/action/master/helper/MainMenuHelper.java:74");
				}
			} finally {
				HibernateUtil.closeSessionQuietly(session1);
			}
		}

		Grid grid = new Grid();
		// "main-menu-cari-grid": penanda agar CSS memberi WARNA TEKS GELAP pada hasil pencarian
		// (di dalam popup putih), sebab toolbarbutton di area menu mewarisi color:#fff dari strip header
		// sehingga teks hasil sebelumnya "putih di atas putih" / tak terlihat.
		grid.setSclass("fgrid main-menu-cari-grid");
		grid.setParent(rowDicari);

		Rows rows = new Rows();
		rows.setParent(grid);

		final String cariLc = cari.trim().toLowerCase();

		// Kumpulkan menu DAUN (yang bisa dibuka) yang cocok. Pencocokan memakai JALUR LENGKAP
		// (label menu + semua label INDUK-nya), sehingga mencari nama KATEGORI (mis. "calon") tetap
		// menemukan sub-menu di bawah kategori itu walau label sub-menu itu sendiri tidak memuat kata
		// tersebut. (Sebelumnya HANYA label menu daun yang dicocokkan -> pencarian nama kategori sering
		// "tidak ketemu".)
		java.util.List<Menu> hasil = new java.util.ArrayList<Menu>();
		java.util.Map<Long, String> pathLabelMap = new java.util.HashMap<Long, String>();
		for (Menu mm : menus) {
			if (mm.getLabel() == null || mm.getAktif() == null || !mm.getAktif()) continue;
			if (MainHelper.hasChild(mm.getChild(), menus)) continue; // hanya menu daun yang dapat dibuka
			String pathLabel = buildMenuPathLabel(mm, menus);
			if (pathLabel.toLowerCase().contains(cariLc)) {
				hasil.add(mm);
				pathLabelMap.put(mm.getId(), pathLabel);
			}
		}

		if (hasil.isEmpty()) {
			Row rowKosong = new Row();
			rowKosong.setParent(rows);
			org.zkoss.zul.Label kosong = new org.zkoss.zul.Label(
					"Tidak ada menu yang cocok dengan \"" + cari.trim() + "\".");
			kosong.setStyle("font-size:11px;color:#64748b;padding:8px;display:block;");
			kosong.setParent(rowKosong);
			return;
		}

		Group group = new ais.ui.util.MyGroupConfig("Hasil pencarian menu (" + hasil.size() + ")");
		group.setParent(rows);

		for (final Menu menu : hasil) {
			{
				Row row = new Row();
				row.setValign("top");
				row.setParent(rows);
				String tampilLabel = pathLabelMap.get(menu.getId());
				if (tampilLabel == null || tampilLabel.trim().isEmpty()) tampilLabel = menu.getLabel();
				Toolbarbutton toolbarbuttonConfig = new ais.ui.util.MyToolbarbuttonConfig(tampilLabel);
					toolbarbuttonConfig.setTooltiptext(tampilLabel);
					toolbarbuttonConfig.setParent(row);
					toolbarbuttonConfig.setStyle("font-size:11px;");
//					toolbarbuttonConfig.setImage("/img/svg/chevron-right.svg");
					toolbarbuttonConfig.addEventListener("onClick", new EventListener() {
						public void onEvent(Event event) throws Exception {
							clickEventListener.onEvent(event);
							try {

								Sessions.getCurrent().setAttribute("currentMenu", menu);

								DetailLogLogin detailLogLogin = new DetailLogLogin();
								detailLogLogin.setKeterangan(menu.getLabel());
								detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
								detailLogLogin.setLogLogin(login);

								Session session = HibernateUtil.currentNativeSession();
								try {
									session.getTransaction().begin();
									session.save(detailLogLogin);
									session.getTransaction().commit();
									// session.disconnect();
									if (session.isOpen()) {
										session.disconnect();
										session.close();
									}
									Sessions.getCurrent().setAttribute("detailLogLogin", detailLogLogin);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainMenuHelper.java:177");
								}
								HibernateUtil.closeSession();

								Common.launchMenu(navigasi, menuService, iframe, menu, login);

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								try {
									PesanFormalHelper.tampilkanGagalException("Membuka menu", "Menu yang Bapak/Ibu pilih gagal dimuat ke dalam frame aplikasi, kemungkinan karena sesi/koneksi ke server sempat terputus sesaat atau terjadi kendala saat memuat komponen menu tersebut.", e, new String[]{"Segarkan (refresh) halaman browser Anda, lalu buka kembali menu ini.", "Pastikan koneksi internet Anda stabil sebelum mencoba kembali.", "Jika menu ini tetap tidak dapat dibuka setelah beberapa kali percobaan, hubungi Administrator."});
								} catch (Exception e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/MainMenuHelper.java:191");
								}

							}

						}
					});
				}
			}
	}

	/**
	 * Bangun label JALUR menu: gabungan label menu DAUN dengan semua label INDUK-nya
	 * (mis. "Calon Mahasiswa &gt; Pendaftaran"). Dipakai agar pencarian menu dapat mencocokkan
	 * nama KATEGORI, bukan hanya label daun. Penelusuran induk lewat relasi root/child (anak
	 * punya {@code root == induk.getChild()}), dengan pengaman anti-loop &amp; batas kedalaman.
	 */
	private static String buildMenuPathLabel(Menu daun, List<Menu> menus) {
		StringBuilder sb = new StringBuilder();
		Menu cur = daun;
		int guard = 0;
		java.util.Set<Long> seen = new java.util.HashSet<Long>();
		while (cur != null && guard < 25) {
			guard++;
			if (cur.getId() != null && !seen.add(cur.getId())) break;
			if (cur.getLabel() != null && cur.getLabel().trim().length() > 0) {
				if (sb.length() > 0) sb.insert(0, " > ");
				sb.insert(0, cur.getLabel().trim());
			}
			Long root = cur.getRoot();
			if (root == null || root.longValue() == 0L) break;
			Menu parent = null;
			for (Menu m : menus) {
				if (m.getChild() != null && m.getChild().equals(root)) { parent = m; break; }
			}
			if (parent == null || parent == cur) break;
			cur = parent;
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public static void loadTree(Tbmuser tbmuser, final LogLogin login, Component menubar,
			EventListener clickEventListener, Tabbox iframe, West navigasi, MyToolbarbuttonConfig menuService,
			boolean pt, boolean ya) {

		if (tbmuser == null || tbmuser.hakAkses() == null || tbmuser.hakAkses().getMenus() == null)
			return;

		Long detailLogLoginId = login == null || login.getId() == null ? null : MainHelper.logins.get(login.getId());

		if (detailLogLoginId == null && login != null && login.getId() != null) {

			// Pencatatan DetailLogLogin bersifat best-effort: kegagalannya TIDAK boleh
			// merusak pemuatan menu. Bila save gagal (mis. sequence detail_log_login_id_seq
			// belum ada di DB), WAJIB rollback + tutup native session di finally agar
			// transaksi yang aborted tidak tertinggal & meracuni operasi berikutnya
			// (mencegah cascade "current transaction is aborted ... menu tidak muncul").
			org.hibernate.Session session1 = null;
			try {
				session1 = HibernateUtil.currentNativeSession();
				LogLogin loginPersisten = (LogLogin) session1.get(LogLogin.class, login.getId());
				if (loginPersisten != null) {
					DetailLogLogin detailLogLogin = new DetailLogLogin();
					detailLogLogin.setKeterangan("Login");
					detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
					detailLogLogin.setLogLogin(loginPersisten);
					session1.getTransaction().begin();
					session1.save(detailLogLogin);
					session1.getTransaction().commit();

					MainHelper.logins.put(login.getId(), detailLogLogin.getId());
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainMenuHelper.java:263");
				try {
					if (session1 != null && session1.getTransaction() != null
							&& session1.getTransaction().isActive()) {
						session1.getTransaction().rollback();
					}
				} catch (Exception eRoll) {
					eRoll.printStackTrace(); ais.common.ErrorAuditUtil.record(eRoll, "auto-audit src/ais/action/master/helper/MainMenuHelper.java:270");
				}
			} finally {
				HibernateUtil.closeSessionQuietly(session1);
			}
		}

		Tbmrole tbmrole = tbmuser.hakAkses();

		List<Menu> menus = (List<Menu>) Sessions.getCurrent().getAttribute("current_menus");

		if (menus == null) {
			/*
			 * Muat daftar menu role secara TAHAN-ERROR sekaligus mengatasi
			 * "org.hibernate.SessionException: Session is closed!".
			 *
			 * Latar: 'tbmrole' (dari tbmuser.hakAkses() / ConstantValues.tbmrole*) adalah
			 * entitas DETACHED yang di-cache; koleksi lazy getMenus()-nya terikat session asal
			 * yang sudah ditutup (atau ditutup thread lain di tengah load → WARN "fail-safe
			 * cleanup (collections)").
			 *
			 * Strategi 2 lapis agar menu DIJAMIN tampil:
			 *  (1) UTAMA — session TERISOLASI, muat ulang role by id lalu inisialisasi koleksi
			 *      menus selagi session terbuka (bersih, tanpa menyentuh koleksi lazy bersama).
			 *  (2) CADANGAN — bila utama gagal/menghasilkan KOSONG, kembali ke cara lama (refresh
			 *      role yang di-cache lalu baca getMenus()) supaya menu tetap muncul.
			 * Hasil KOSONG TIDAK di-cache agar gangguan sesaat tidak "mengunci" menu jadi hilang.
			 */

			// (0) TERCEPAT & PALING ANDAL: koleksi menus dari peran yang DI-CACHE
			// (tbmuser.hakAkses() → getUserRoleYgDipakai). Bila koleksinya SUDAH ter-inisialisasi
			// (umumnya sejak login), baca LANGSUNG tanpa session — inilah perilaku lama yang
			// terbukti menampilkan menu. isInitialized() aman: tidak memicu lazy-load (jadi tak
			// melempar "Session is closed!"). Bila belum ter-inisialisasi/kosong → lanjut load by id.
			try {
				if (tbmrole != null && org.hibernate.Hibernate.isInitialized(tbmrole.getMenus())
						&& tbmrole.getMenus() != null && !tbmrole.getMenus().isEmpty()) {
					menus = new ArrayList<Menu>(tbmrole.getMenus());
				}
			} catch (Exception e) {
				menus = null;
			}

			// (1) UTAMA: session terisolasi, load by id (hanya bila (0) belum menghasilkan menu)
			if (menus == null || menus.isEmpty()) {
				Session sesiUtama = null;
				try {
					sesiUtama = HibernateUtil.getSessionFactory().openSession();
					Tbmrole tbmroleFresh = (tbmrole != null && tbmrole.getId() != null)
							? (Tbmrole) sesiUtama.get(Tbmrole.class, tbmrole.getId())
							: null;
					if (tbmroleFresh != null && tbmroleFresh.getMenus() != null) {
						menus = new ArrayList<Menu>(tbmroleFresh.getMenus());
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainMenuHelper.java:343");
				} finally {
					if (sesiUtama != null) {
						try { if (sesiUtama.isOpen()) sesiUtama.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MainMenuHelper.java:346"); }
						try { sesiUtama.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MainMenuHelper.java:347"); }
						try { if (sesiUtama.isOpen()) sesiUtama.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MainMenuHelper.java:348"); }
					}
				}
			}

			// (2) CADANGAN: pastikan menu TETAP tampil (perilaku lama)
			if (menus == null || menus.isEmpty()) {
				Session sesiCadangan = HibernateUtil.currentNativeSession();
				try {
					sesiCadangan.refresh(tbmrole);
					menus = new ArrayList<Menu>(tbmrole.getMenus());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainMenuHelper.java:360");
					if (menus == null) {
						menus = new ArrayList<Menu>();
					}
				} finally {
					try { sesiCadangan.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MainMenuHelper.java:365"); }
					try { sesiCadangan.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MainMenuHelper.java:366"); }
					HibernateUtil.closeSession();
				}
			}

			if (menus != null) {
				try {
					Collections.sort(menus);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainMenuHelper.java:375");
				}
				// Hanya cache bila ADA isinya → hindari menu "terkunci kosong".
				if (!menus.isEmpty()) {
					Sessions.getCurrent().setAttribute("current_menus", menus);
				}
			}

		}

		Sessions.getCurrent().setAttribute("users", tbmuser);
		createTreeMenu(menubar, tbmrole, menus, tbmuser, login, clickEventListener, iframe, navigasi, menuService, pt,
				ya);

		if (Common.bolehKonfigurasi("tampilkan_menu_bantuan")) {
			MyMenuitem menuitem = new MyMenuitem(
					tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleId(), "Bantuan",
					"bantuan");
			menuitem.setImage("/img/svg/question-square.svg");

			menuitem.setParent(menubar);
			menuitem.addEventListener("onClick", new EventListener() {
				public void onEvent(Event event) throws Exception {
					MainHelper.onKatalogBantuan(login);
				}
			});
		}

		MyMenuitem menuitem = new MyMenuitem(
				tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleId(), "Keluar Aplikasi",
				"");
		menuitem.setImage("/img/shutdown.PNG");
		menuitem.setParent(menubar);
		menuitem.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) throws Exception {
				MainHelper.onKeluar(login);
			}
		});

	}

	private static void createRootSubMenu(Long root, List<Menu> menus, MyMenu parenttreeitem, Tbmuser tbmuser,
			final LogLogin login, final EventListener clickEventListener, final Tabbox iframe, final West navigasi,
			final MyToolbarbuttonConfig menuService, int coba, boolean pt, boolean ya) {
		Menupopup tc1 = new Menupopup();
		tc1.setParent(parenttreeitem);
		for (final Menu menu : menus) {
			if ((!pt && !ya) || (pt && menu.getTampilDiPt()) || (ya && menu.getTampilDiSekolah())) {
				if (menu.getAktif() != null && !menu.getAktif()) {
					continue;
				}
				if (menu.getRoot().equals(root)) {
					Boolean ada = MainHelper.hasChild(menu.getChild(), menus);
					if (ada && coba < 500) {
						final MyMenu treeitem = new MyMenu(
								tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleId(),
								menu.getLabel(), menu.getUrl());
						treeitem.setParent(tc1);
						treeitem.setHoverImage("/img/svg/folder-symlink.svg");
						treeitem.setImage("/img/svg/folder2.svg");
						treeitem.addEventListener("onClick", new EventListener() {
							public void onEvent(Event event) throws Exception {
								Sessions.getCurrent().setAttribute("treeitem", treeitem);
							}
						});
						createRootSubMenu(menu.getChild(), menus, treeitem, tbmuser, login, clickEventListener, iframe,
								navigasi, menuService, ++coba, pt, ya);
					} else {
						final MyMenuitem treeitem = new MyMenuitem(
								tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleId(),
								menu.getLabel(), menu.getUrl());
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

								Session session = HibernateUtil.currentNativeSession();
								try {
									session.getTransaction().begin();
									session.save(detailLogLogin);
									session.getTransaction().commit();
									// session.disconnect();
									if (session.isOpen()) {
										session.disconnect();
										session.close();
									}
									Sessions.getCurrent().setAttribute("detailLogLogin", detailLogLogin);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainMenuHelper.java:474");
								}
								HibernateUtil.closeSession();

								Common.launchMenu(navigasi, menuService, iframe, menu, login);

								Sessions.getCurrent().setAttribute("treeitem", treeitem);
							}
						});
					}
				}
			}
		}

	}

	public static void createTreeMenu(Component menubar, Tbmrole job, List<Menu> menus, Tbmuser tbmuser,
			final LogLogin login, final EventListener clickEventListener, final Tabbox iframe, final West navigasi,
			final MyToolbarbuttonConfig menuService, boolean pt, boolean ya) {
		if (menus == null) {
			return;
		}
		Sessions.getCurrent().setAttribute("currentMenus", menus);

		for (final Menu menu : menus) {
			if ((!pt && !ya) || (pt && menu.getTampilDiPt()) || (ya && menu.getTampilDiSekolah())) {
				if (menu.getAktif() != null && !menu.getAktif()) {
					continue;
				}
				if (menu.getRoot().equals(0L)) {

					Boolean ada = MainHelper.hasChild(menu.getChild(), menus);
					if (ada) {
						final MyMenu treeitem = new MyMenu(job.getRoleId(), menu.getLabel(), menu.getUrl());
						treeitem.setParent(menubar);
						treeitem.setHoverImage("/img/svg/folder-symlink.svg");
						treeitem.setImage("/img/svg/folder2.svg");
						treeitem.addEventListener("onClick", new EventListener() {
							public void onEvent(Event event) throws Exception {
								Sessions.getCurrent().setAttribute("treeitem", treeitem);
							}
						});
						createRootSubMenu(menu.getChild(), menus, treeitem, tbmuser, login, clickEventListener, iframe,
								navigasi, menuService, 0, pt, ya);
					} else {
						final MyMenuitem treeitem = new MyMenuitem(job.getRoleId(), menu.getLabel(), menu.getUrl());
						treeitem.setParent(menubar);
						treeitem.setImage("/img/svg/chevron-right.svg");
						treeitem.addEventListener("onClick", new EventListener() {
							public void onEvent(Event event) throws Exception {

								clickEventListener.onEvent(event);

								try {

									Sessions.getCurrent().setAttribute("currentMenu", menu);

									DetailLogLogin detailLogLogin = new DetailLogLogin();
									detailLogLogin.setKeterangan(menu.getLabel());
									detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
									detailLogLogin.setLogLogin(login);
									Session session = HibernateUtil.currentNativeSession();
									try {
										session.getTransaction().begin();
										session.save(detailLogLogin);
										session.getTransaction().commit();
										// session.disconnect();
										if (session.isOpen()) {
											session.disconnect();
											session.close();
										}
										Sessions.getCurrent().setAttribute("detailLogLogin", detailLogLogin);
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainMenuHelper.java:547");
									}
									HibernateUtil.closeSession();

									Sessions.getCurrent().setAttribute("treeitem", treeitem);
									Common.launchMenu(navigasi, menuService, iframe, menu, login);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									try {
										PesanFormalHelper.tampilkanGagalException("Membuka menu", "Menu yang Bapak/Ibu pilih gagal dimuat ke dalam frame aplikasi, kemungkinan karena sesi/koneksi ke server sempat terputus sesaat atau terjadi kendala saat memuat komponen menu tersebut.", e, new String[]{"Segarkan (refresh) halaman browser Anda, lalu buka kembali menu ini.", "Pastikan koneksi internet Anda stabil sebelum mencoba kembali.", "Jika menu ini tetap tidak dapat dibuka setelah beberapa kali percobaan, hubungi Administrator."});
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/MainMenuHelper.java:562");
									}

								}

							}
						});
					}
				}
			}
		}
	}

}
