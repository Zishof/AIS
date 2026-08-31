package ais.action.master.chat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.East;
import org.zkoss.zul.Group;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.West;

import ais.action.maintenance.MainAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.OnlineUsers;
import ais.database.model.Perkuliahan;
import ais.database.model.Pesan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk chat users. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit
 * pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Rows rows},
 * {@code Rows rowsOffline}, {@code Tabs tabs}, {@code Tabpanels tabpanels}, {@code Tbmuser currentUser}, {@code
 * List onlineUsers}, {@code List chatWindows}; inisialisasi/lifecycle ({@code init()}); pembacaan/pencarian
 * ({@code loadData()}, {@code loadDataSemua()}, {@code getPesanDari()}); validasi/perhitungan ({@code
 * checkPesan()}, {@code checkPesan()}); mutasi data ({@code prosess()}); operasi domain lain ({@code
 * onCreate()}, {@code createOnlineUsers()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class ChatUsers extends MyWindow {

	// private static final Log log = Log.lookup(ChatUsers.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 5826165925891023866L;

	private Center center;

	private Rows rows;
	private Rows rowsOffline;

	private Tabs tabs;

	private Tabpanels tabpanels;

	private Tbmuser currentUser = Common.getCurrentUser();

	private List<OnlineUsers> onlineUsers;
	private List<ChatWindow> chatWindows;

	// private MyWindow chatWindow;
	private Button chat;

	private Tbmuser selectedFriend;

	private Page page;

	private Tab tabChat;

	private Textbox cari;
	private Textbox cariOffline;

	private Perkuliahan perkuliahan;

	public ChatUsers() {
		super();

	}

	public ChatUsers(Perkuliahan perkuliahan, Tab tabChat) {
		super();
		this.tabChat = tabChat;
		this.perkuliahan = perkuliahan;
	}

	public ChatUsers(String title, String border, boolean closable) {
		super(title, border, closable);

	}

	public void onCreate() {
		init();
	}

	private Desktop _desktop;

	public void init() {
		chatWindows = new LinkedList<ChatWindow>();

		_desktop = Executions.getCurrent().getDesktop();
		_desktop.enableServerPush(true);

		page = ExecutionsCtrl.getCurrentCtrl().getCurrentPage();

		// chatWindow = (MyWindow)
		// Sessions.getCurrent().getAttribute("chatWindow");
		chat = (Button) Sessions.getCurrent().getAttribute("chat");

		if (perkuliahan == null) {
			tabChat = (Tab) Sessions.getCurrent().getAttribute("tabChat");
		}

		currentUser = Common.getCurrentUser();
		if (currentUser.getMahasiswa() != null) {
			currentUser.setUserId(currentUser.getMahasiswa().getNim());
		}

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		center = new Center();
		center.setTitle("Percakapan");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		tabs = new Tabs();
		tabs.setParent(tabbox);

		tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		if (true) {
			West west = new West();
			west.setTitle("Pengguna Online");
			west.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(west, true);
			west.setWidth("250px");
			west.setBorder("none");

			Borderlayout subBorderlayout = new ais.ui.util.MyBorderlayout();
			subBorderlayout.setParent(west);

			North subNorth = new North();
			subNorth.setParent(subBorderlayout);
			subNorth.setHeight("25px");
			subNorth.setBorder("none");

			Borderlayout subSubBorderlayout = new ais.ui.util.MyBorderlayout();
			subSubBorderlayout.setParent(subNorth);

			West subSubwest = new West();
			subSubwest.setParent(subSubBorderlayout);
			subSubwest.setWidth("80%");
			subSubwest.setBorder("none");

			cari = new Textbox();
			cari.setWidth("90%");
			cari.setParent(subSubwest);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.setWidth("90%");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(cari.getValue().trim());
				}
			});

			Center subsubcenter = new Center();
			subsubcenter.setParent(subSubBorderlayout);
			ais.ui.util.ZkCompat.setFlex(subsubcenter, true);
			button.setParent(subsubcenter);
			subsubcenter.setBorder("none");

			cari.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(cari.getValue().trim());
				}
			});

			Center subcenter = new Center();
			subcenter.setParent(subBorderlayout);
			ais.ui.util.ZkCompat.setFlex(subcenter, true);
			subcenter.setBorder("none");

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(subcenter);
			grid.setWidth("100%");
			grid.setHeight("100%");

			rows = new Rows();
			rows.setParent(grid);
		}

		if (true) {
			East east = new East();
			east.setTitle("Pengguna Pernah Online");
			east.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(east, true);
			east.setWidth("250px");
			east.setBorder("none");

			Borderlayout subBorderlayout = new ais.ui.util.MyBorderlayout();
			subBorderlayout.setParent(east);

			North subNorth = new North();
			subNorth.setParent(subBorderlayout);
			subNorth.setHeight("25px");
			subNorth.setBorder("none");

			Borderlayout subSubBorderlayout = new ais.ui.util.MyBorderlayout();
			subSubBorderlayout.setParent(subNorth);

			West subSubwest = new West();
			subSubwest.setParent(subSubBorderlayout);
			subSubwest.setWidth("80%");
			subSubwest.setBorder("none");

			cariOffline = new Textbox();
			cariOffline.setWidth("90%");
			cariOffline.setParent(subSubwest);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.setWidth("90%");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					loadDataSemua(cariOffline.getValue().trim());
				}
			});

			Center subsubcenter = new Center();
			subsubcenter.setParent(subSubBorderlayout);
			ais.ui.util.ZkCompat.setFlex(subsubcenter, true);
			button.setParent(subsubcenter);
			subsubcenter.setBorder("none");

			cariOffline.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadDataSemua(cariOffline.getValue().trim());
				}
			});

			Center subcenter = new Center();
			subcenter.setParent(subBorderlayout);
			ais.ui.util.ZkCompat.setFlex(subcenter, true);
			subcenter.setBorder("none");

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(subcenter);
			grid.setWidth("100%");
			grid.setHeight("100%");

			rowsOffline = new Rows();
			rowsOffline.setParent(grid);

		}

		MainAction.mapChat.get(currentUser.getUserId()).chatUsers.add(this);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(cari.getValue().trim());
				loadDataSemua(cariOffline.getValue().trim());

				prosess(null);
			}
		});
	}

	public void loadData(String keyword) {

		if (onlineUsers == null) {
			return;
		}

		Common.clear(rows);

		TreeMap<String, List<OnlineUsers>> maps = new TreeMap<String, List<OnlineUsers>>();
		for (OnlineUsers objects : onlineUsers) {
			Mahasiswa mahasiswa = objects.getMahasiswa();
			Dosen dosen = objects.getDosen();
			Tbmuser a = objects.getTbmuser();
			String roleName = mahasiswa != null ? Common.getBahasa("label_mahasiswa")
					: dosen != null ? Common.getBahasa("label_dosen")
							: a == null || a.hakAkses() == null ? "" : a.hakAkses().getRoleName();
			if (roleName == null) {
				continue;
			}

			final Tbmuser tbmuser;
			if (a == null) {
				tbmuser = new Tbmuser();
			} else {
				tbmuser = a;
			}

			if (mahasiswa != null) {
				tbmuser.setUserId(mahasiswa.getNim());
				tbmuser.setMahasiswa(mahasiswa);
			}

			tbmuser.setDosen(dosen);

			if (currentUser.getUserId() != null && tbmuser.getUserId() != null
					&& currentUser.getUserId().equals(tbmuser.getUserId())) {
				continue;
			}

			if (currentUser.getMahasiswa() != null && tbmuser.getMahasiswa() != null
					&& currentUser.getMahasiswa().getId().equals(tbmuser.getMahasiswa().getId())) {
				continue;
			}

			boolean adaMahasiswa = false;
			if (mahasiswa != null) {
				adaMahasiswa = (mahasiswa.getNim() != null
						&& mahasiswa.getNim().toLowerCase().contains(keyword.toLowerCase()))
						|| (mahasiswa.getNama() != null
								&& mahasiswa.getNama().toLowerCase().contains(keyword.toLowerCase()));
			}

			boolean adaDosen = false;
			if (dosen != null) {
				adaDosen = dosen.getNama() != null && dosen.getNama().toLowerCase().contains(keyword.toLowerCase());
			}

			boolean adaAdmin = false;

			adaAdmin = (tbmuser.getUserId() != null
					&& tbmuser.getUserId().toLowerCase().contains(keyword.toLowerCase()))
					|| (tbmuser.getUserNama() != null
							&& tbmuser.getUserNama().toLowerCase().contains(keyword.toLowerCase()));

			if (!adaMahasiswa && !adaDosen && !adaAdmin) {
				continue;
			}

			if (maps.containsKey(roleName)) {
				maps.get(roleName).add(objects);
			} else {
				maps.put(roleName, new ArrayList<OnlineUsers>());
				maps.get(roleName).add(objects);
			}
		}

		for (String roleName : maps.keySet()) {

			Group group = new ais.ui.util.MyGroupConfig();
			group.setLabel(roleName);
			group.setParent(rows);

			for (OnlineUsers objects : maps.get(roleName)) {
				Mahasiswa mahasiswa = objects.getMahasiswa();
				Dosen dosen = objects.getDosen();
				Tbmuser a = objects.getTbmuser();

				final Tbmuser tbmuser;
				if (a == null) {
					tbmuser = new Tbmuser();
				} else {
					tbmuser = a;
				}

				if (mahasiswa != null) {
					tbmuser.setUserId(mahasiswa.getNim());
					tbmuser.setMahasiswa(mahasiswa);
				}

				tbmuser.setDosen(dosen);

				if (currentUser.getUserId() != null && tbmuser.getUserId() != null
						&& currentUser.getUserId().equals(tbmuser.getUserId())) {
					continue;
				}

				if (currentUser.getMahasiswa() != null && tbmuser.getMahasiswa() != null
						&& currentUser.getMahasiswa().getId().equals(tbmuser.getMahasiswa().getId())) {
					continue;
				}

				final MyFormRow row = new MyFormRow();row.setValign("top");
				row.setValign("top");row.setAttribute("tbmuser", tbmuser);

				if (selectedFriend != null && selectedFriend.getUserId().equals(tbmuser.getUserId())) {
					row.setStyle("border:0px;background: yellow;");
					Clients.scrollIntoView(row);
				}

				row.setParent(rows);

				final A toolbarbutton = new A(tbmuser.toString());

				row.appendChild(toolbarbutton);
				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						for (Object myRow : rows.getChildren()) {
							Row myyRow = (Row) myRow;
							myyRow.setStyle("border:0px;background: transparent;");
						}
						selectedFriend = tbmuser;
						row.setStyle("border:0px;background: yellow;");
						Clients.scrollIntoView(row);
						prosess(tbmuser);
					}
				});

			}
		}

		maps = null;
	}

	@SuppressWarnings("unchecked")
	public void loadDataSemua(final String keyword) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsOffline);

				TreeMap<String, List<Object[]>> maps = new TreeMap<String, List<Object[]>>();
				Session session = HibernateUtil.currentNativeSession();
				String idMhs = "";
				String idDosen = "";
				if (perkuliahan != null) {
					List<Long> ids = session.createCriteria(Detailperkuliahan.class)
							.setProjection(Projections.groupProperty("mahasiswa.id"))
							.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
							.add(Restrictions.eq("perkuliahan", perkuliahan)).list();
					for (Long id : ids) {
						idMhs += idMhs.isEmpty() ? id + "" : "," + id;
					}

					ids = new ArrayList<Long>();
					for (Dosen dosen : perkuliahan.populateDosen().values()) {
						idDosen += idDosen.isEmpty() ? dosen.getId() + "" : "," + dosen.getId();
					}
				}

				String all = "";
				if (!idMhs.isEmpty() && !idDosen.isEmpty()) {
					all = " (a.mahasiswa in (" + idMhs + ") or a.dosen in (" + idDosen + ")) and ";
				} else if (!idMhs.isEmpty()) {
					all = " a.mahasiswa in (" + idMhs + ") and ";
				} else if (!idDosen.isEmpty()) {
					all = " a.dosen in (" + idDosen + ") and ";
				}

				String sql = "select *\nfrom (\n	(\n		select \n"
						+ "		tbmuser,max(b.usernama) nama,max(c.rolename) as rolename,max(a.id) as id\n"
						+ "		from log_login a\n		inner join tbmuser b on (b.userid=a.tbmuser)\n"
						+ "		inner join tbmrole c on (b.userrole=c.roleid)\n" + "		where " + all
						+ " tbmuser is not null and a.dosen is null and a.mahasiswa is null\n"
						+ "		and (tbmuser ilike '%" + keyword + "%' or b.usernama ilike '%" + keyword + "%')\n"
						+ "		group by tbmuser\n		order by max(a.id) desc\n		limit 10\n	)\n"
						+ "	union all\n	(\n		select \n"
						+ "		a.dosen||'' as tbmuser,max(b.nama) nama,'Dosen' as rolename,max(a.id) as id\n"
						+ "		from log_login a\n		inner join dosen b on (b.id=a.dosen)\n" + "		where " + all
						+ " a.dosen is not null and a.mahasiswa is null\n		and (b.nama ilike '%" + keyword
						+ "%')\n		group by a.dosen\n		order by max(a.id) desc\n		limit 10\n	)\n"
						+ "	union all\n	(\n		select \n"
						+ "		a.mahasiswa||'' as tbmuser,max(b.nama) nama,'Mahasiswa' as rolename,max(a.id) as id\n"
						+ "		from log_login a\n		inner join mahasiswa b on (b.id=a.mahasiswa)\n" + "		where "
						+ all + " a.mahasiswa is not null\n		and (b.nim ilike '%" + keyword + "%' or b.nama ilike '%"
						+ keyword + "%')\n		group by a.mahasiswa\n"
						+ "		order by max(a.id) desc\n		limit 10\n	)\n) a";

				List<Object[]> logLogins = session.createSQLQuery(sql).list();
				for (Object[] objects : logLogins) {
					String roleName = objects[2] == null ? null : objects[2].toString();
					if (roleName == null) {
						continue;
					}

					if (maps.containsKey(roleName)) {
						maps.get(roleName).add(objects);
					} else {
						maps.put(roleName, new ArrayList<Object[]>());
						maps.get(roleName).add(objects);
					}
				}

				for (String roleName : maps.keySet()) {

					Group group = new ais.ui.util.MyGroupConfig();
					group.setLabel(roleName);
					group.setParent(rowsOffline);

					for (Object[] objects : maps.get(roleName)) {
						Mahasiswa mahasiswa = null;
						Dosen dosen = null;
						Tbmuser a = null;

						if (roleName.equalsIgnoreCase("Mahasiswa") && Common.isNumber(objects[0].toString())) {
							mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.idEq(Long.parseLong(objects[0].toString()))).uniqueResult();
						} else if (roleName.equalsIgnoreCase("Dosen") && Common.isNumber(objects[0].toString())) {
							dosen = (Dosen) session.createCriteria(Dosen.class)
									.add(Restrictions.idEq(Long.parseLong(objects[0].toString()))).uniqueResult();
							if (dosen != null) {
								a = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
										.add(Restrictions.eq("dosen", dosen)).uniqueResult();
							}
						} else {
							a = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.idEq(objects[0].toString())).uniqueResult();
						}

						final Tbmuser tbmuser;
						if (a == null) {
							tbmuser = new Tbmuser();
						} else {
							tbmuser = a;
						}

						if (mahasiswa != null) {
							tbmuser.setUserId(mahasiswa.getNim());
							tbmuser.setMahasiswa(mahasiswa);
						}

						tbmuser.setDosen(dosen);

						if (currentUser.getUserId() != null && tbmuser.getUserId() != null
								&& currentUser.getUserId().equals(tbmuser.getUserId())) {
							continue;
						}

						if (currentUser.getMahasiswa() != null && tbmuser.getMahasiswa() != null
								&& currentUser.getMahasiswa().getId().equals(tbmuser.getMahasiswa().getId())) {
							continue;
						}

						final MyFormRow row = new MyFormRow();row.setValign("top");
						row.setValign("top");row.setAttribute("tbmuser", tbmuser);

						if (selectedFriend != null && selectedFriend.getUserId().equals(tbmuser.getUserId())) {
							row.setStyle("border:0px;background: yellow;");
							Clients.scrollIntoView(row);
						}

						row.setParent(rowsOffline);

						final A toolbarbutton = new A(tbmuser.toString());

						row.appendChild(toolbarbutton);
						toolbarbutton.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								for (Object myRow : rowsOffline.getChildren()) {
									Row myyRow = (Row) myRow;
									myyRow.setStyle("border:0px;background: transparent;");
								}
								selectedFriend = tbmuser;
								row.setStyle("border:0px;background: yellow;");
								Clients.scrollIntoView(row);
								prosess(tbmuser);
							}
						});

					}
				}

				HibernateUtil.closeSession();

				maps = null;
			}
		});

	}

	@SuppressWarnings("unchecked")
	private void prosess(final Tbmuser tbmuser) {
		List<Tabpanel> tabpanels = ChatUsers.this.tabpanels.getChildren();
		synchronized (tabpanels) {
			for (Tabpanel myTabpanel : tabpanels) {

				if (tbmuser == null && myTabpanel.getAttribute("semua") != null) {
					myTabpanel.getLinkedTab().setSelected(true);
					return;
				}

				if (tbmuser != null) {
					if (myTabpanel.getAttribute("tbmuser") == null) {
						continue;
					}

					Tbmuser myTbmuser = (Tbmuser) myTabpanel.getAttribute("tbmuser");

					if (myTbmuser.getUserId().equals(tbmuser.getUserId())) {
						myTabpanel.getLinkedTab().setSelected(true);
						return;
					}
				}

			}

			final MyTabConfig tab = new MyTabConfig(tbmuser == null ? "Forum Komunikasi" : tbmuser.getUserId());
			final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			// tabs.setHeight("0px");
			// tab.setHeight("0px");
			tab.setClosable(false);
			// tabs.setVisible(false);
			tab.setParent(tabs);
			// tab.addEventListener("onClick", new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// tab.setLabel("");
			// }
			// });

			tabpanel.setParent(ChatUsers.this.tabpanels);
			if (tbmuser != null) {
				tabpanel.setAttribute("tbmuser", tbmuser);
			} else {
				tabpanel.setAttribute("semua", true);
			}

			final Textbox msg = new Textbox();

			final ChatWindow chatWindow = new ChatWindow(page, msg, chat, currentUser,
					(tbmuser == null ? currentUser : tbmuser), perkuliahan, tbmuser == null, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							tabChat.setVisible(true);
							tabChat.setSelected(true);
							tabChat.getLinkedPanel().setVisible(true);

							try {

								if (chat != null && perkuliahan == null) {
									chat.setLabel("Ada pesan masuk");
									if (!StringUtils.contains(chat.getStyle(), ";background: transparent;")) {
										chat.setStyle(chat.getStyle() + ";background: transparent;");
									}

									chat.setStyle(org.apache.commons.lang3.StringUtils.replace(chat.getStyle(), ";background: transparent;",
											";background: yellow;"));
								}

								page.setTitle("PESAN MASUK - " + page.getTitle().replaceAll("PESAN MASUK - ", ""));

							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/chat/ChatUsers.java:687");

							}

							msg.focus();
							tab.setSelected(true);
							if (tbmuser != null) {
								for (Object myRow : rows.getChildren()) {
									Row myyRow = (Row) myRow;
									Tbmuser myTbmuser = (Tbmuser) myyRow.getAttribute("tbmuser");
									if (myTbmuser != null) {
										if (!myTbmuser.getUserId().equals(tbmuser.getUserId())) {
											myyRow.setStyle("border:0px;background: transparent;");
										} else {
											selectedFriend = tbmuser;
											myyRow.setStyle("border:0px;background: yellow;");
											Clients.scrollIntoView(myyRow);
										}
									}
								}
							}
						}
					});

			tab.addEventListener("onClose", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					System.out
							.println("============================ Closing tab " + tbmuser + " in user " + currentUser);
					chatWindow.onExit();
				}
			});

			chatWindow.setWidth("100%");
			chatWindow.setHeight("100%");
			chatWindow.setParent(tabpanel);

			chatWindows.add(chatWindow);
			// tab.setAttribute("chatWindow", chatWindow);

			tab.setSelected(true);
			msg.focus();
		}

	}

	private Boolean checkPesan(Session session, Boolean semua) {

		Criterion and = null;
		if (!semua) {
			Criterion or = Restrictions.sqlRestriction("1!=1");

			if (currentUser.getMahasiswa() == null && currentUser.getUserId() != null) {
				or = Restrictions.or(or, Restrictions.eq("tbmuserf", currentUser));
			}

			if (currentUser.getMahasiswa() != null) {
				or = Restrictions.or(or, Restrictions.eq("mahasiswaf", currentUser.getMahasiswa()));
			}

			if (currentUser.getDosen() != null) {
				or = Restrictions.or(or, Restrictions.eq("dosenf", currentUser.getDosen()));
			}

			if (currentUser.getPegawai() != null) {
				or = Restrictions.or(or, Restrictions.eq("pegawaif", currentUser.getPegawai()));
			}

			and = Restrictions.and(or, Restrictions.eq("aktif", true));
		} else {

			and = Restrictions.eq("semua", true);
		}

		String tambahan = "[" + currentUser.getUserId() + "]";
		and = Restrictions.and(and, Restrictions.not(Restrictions.ilike("diterimaOleh", tambahan, MatchMode.ANYWHERE)));

		Integer count = ((Number) session.createCriteria(Pesan.class)
				.add(perkuliahan == null ? Restrictions.isNull("perkuliahan")
						: Restrictions.eq("perkuliahan", perkuliahan))
				.add(and).setProjection(Projections.rowCount()).uniqueResult()).intValue();

		return !count.equals(0);
	}

	@SuppressWarnings("unchecked")
	private List<Tbmuser> getPesanDari() {

		Criterion and = null;

		Criterion or = Restrictions.sqlRestriction("1!=1");

		if (currentUser.getMahasiswa() == null && currentUser.getUserId() != null) {
			or = Restrictions.or(or, Restrictions.eq("tbmuserf", currentUser));
		}

		if (currentUser.getMahasiswa() != null) {
			or = Restrictions.or(or, Restrictions.eq("mahasiswaf", currentUser.getMahasiswa()));
		}

		if (currentUser.getDosen() != null) {
			or = Restrictions.or(or, Restrictions.eq("dosenf", currentUser.getDosen()));
		}

		if (currentUser.getPegawai() != null) {
			or = Restrictions.or(or, Restrictions.eq("pegawaif", currentUser.getPegawai()));
		}
		and = Restrictions.and(or, Restrictions.eq("aktif", true));

		String tambahan = "[" + currentUser.getUserId() + "]";
		and = Restrictions.and(and, Restrictions.not(Restrictions.ilike("diterimaOleh", tambahan, MatchMode.ANYWHERE)));

		List<Tbmuser> tbmusers = new ArrayList<Tbmuser>();

		Session session = HibernateUtil.currentNativeSession();
		List<String> count = session.createCriteria(Pesan.class)
				.add(perkuliahan == null ? Restrictions.isNull("perkuliahan")
						: Restrictions.eq("perkuliahan", perkuliahan))
				.add(and).createAlias("tbmuser", "tbmuser").setProjection(Projections.groupProperty("tbmuser.userId"))
				.list();

		if (count.size() != 0) {
			tbmusers = session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.in("userId", count)).list();
		}

		List<Long> mhs = session.createCriteria(Pesan.class)
				.add(perkuliahan == null ? Restrictions.isNull("perkuliahan")
						: Restrictions.eq("perkuliahan", perkuliahan))
				.add(and).createAlias("mahasiswa", "mahasiswa").setProjection(Projections.groupProperty("mahasiswa.id"))
				.list();

		if (mhs.size() != 0) {
			Tbmrole tbmrole = ConstantValues.tbmroleMahasiswa;

			List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.in("id", mhs)).list();

			for (Mahasiswa mahasiswa : mahasiswas) {
				Tbmuser users = new Tbmuser();
				users.setDosen(null);
				users.setMahasiswa(mahasiswa);
				users.setUserNama(mahasiswa.getNim());
				users.setUserId(mahasiswa.getNim());
				users.setUserRole(tbmrole);
				users.setFakultas(mahasiswa.getJurusan().getFakultas());
				users.setJurusan(mahasiswa.getJurusan());
				tbmusers.add(users);
			}
		}

		HibernateUtil.closeSession();

		return tbmusers;
	}

	@SuppressWarnings("unchecked")
	public Criteria createOnlineUsers(Session session1) {
		Criteria criteria = session1.createCriteria(OnlineUsers.class);
		if (perkuliahan != null) {
			Map<String, Dosen> map = perkuliahan.populateDosen();
			List<Mahasiswa> mahasiswas = session1.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
					.setProjection(Projections.groupProperty("mahasiswa"))
					.add(Restrictions.eq("perkuliahan", perkuliahan)).list();

			Criterion curr = currentUser.getMahasiswa() == null ? Restrictions.eq("tbmuser", currentUser)
					: Restrictions.eq("mahasiswa", currentUser.getMahasiswa());

			if (map.isEmpty() && mahasiswas.isEmpty()) {
				criteria.add(curr);
			} else if (!map.isEmpty() && !mahasiswas.isEmpty()) {
				criteria.add(Restrictions.or(curr, Restrictions.or(Restrictions.in("mahasiswa", mahasiswas),
						Restrictions.in("dosen", map.values()))));
			} else if (!map.isEmpty()) {
				criteria.add(Restrictions.or(curr, Restrictions.in("dosen", map.values())));
			} else if (!mahasiswas.isEmpty()) {
				criteria.add(Restrictions.or(curr, Restrictions.in("mahasiswa", mahasiswas)));
			}
		}
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void checkPesan() throws Exception {

		Session session2 = HibernateUtil.currentNativeSession();
		try {
			int count = ((Number) createOnlineUsers(session2).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();

			int size = onlineUsers == null ? 0 : onlineUsers.size();

			if (size != count) {

				onlineUsers = null;
				onlineUsers = createOnlineUsers(session2).list();
				Executions.activate(_desktop);
				try {
					loadData(cari == null ? "" : cari.getValue().trim());
				} finally {
					Executions.deactivate(_desktop);
				}

			}

			if (checkPesan(session2, true)) {
				if (!_desktop.isServerPushEnabled()) {
					_desktop.enableServerPush(true);
				}
				Executions.activate(_desktop);

				try {

					System.out.println("================================ " + currentUser
							+ " Ada pesan masuk forum =================================");

					tabChat.setVisible(true);
					tabChat.setSelected(true);
					tabChat.getLinkedPanel().setVisible(true);

					if (chat != null && perkuliahan == null) {
						chat.setLabel("Ada pesan masuk");

						if (!StringUtils.contains(chat.getStyle(), ";background: transparent;")) {
							chat.setStyle(chat.getStyle() + ";background: transparent;");
						}

						chat.setStyle(org.apache.commons.lang3.StringUtils.replace(chat.getStyle(), ";background: transparent;",
								";background: yellow;"));
					}
					try {
						page.setTitle("PESAN MASUK - " + page.getTitle().replaceAll("PESAN MASUK - ", ""));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/chat/ChatUsers.java:919");

					}

					prosess(null);

				} finally {
					Executions.deactivate(_desktop);
				}
			}

			else if (checkPesan(session2, false)) {
				if (!_desktop.isServerPushEnabled()) {
					_desktop.enableServerPush(true);
				}
				Executions.activate(_desktop);
				try {

					System.out.println("================================ " + currentUser
							+ " Ada pesan masuk pengguna =================================");

					tabChat.setVisible(true);
					tabChat.setSelected(true);
					tabChat.getLinkedPanel().setVisible(true);

					if (chat != null && perkuliahan == null) {
						chat.setLabel("Ada pesan masuk");

						if (!StringUtils.contains(chat.getStyle(), ";background: transparent;")) {
							chat.setStyle(chat.getStyle() + ";background: transparent;");
						}

						chat.setStyle(org.apache.commons.lang3.StringUtils.replace(chat.getStyle(), ";background: transparent;",
								";background: yellow;"));
					}
					try {
						page.setTitle("PESAN MASUK - " + page.getTitle().replaceAll("PESAN MASUK - ", ""));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/chat/ChatUsers.java:956");

					}

					List<Tbmuser> tbmusers = getPesanDari();
					for (Tbmuser myTbmuser : tbmusers) {
						prosess(myTbmuser);
					}

				} finally {
					Executions.deactivate(_desktop);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			MainAction.mapChat.get(currentUser.getUserId()).chatUsers.remove(this);
			if (_desktop.isServerPushEnabled())
				_desktop.enableServerPush(false);
		}

		HibernateUtil.closeSession();
	}

}
