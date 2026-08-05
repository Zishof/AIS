package ais.action.master.penelitiandanpengabdian;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.DetailPengumumanPenelitianHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DiskusiPengumumanPenelitian;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.PengumumanPenelitian;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PengumumanPenelitianAction extends GenericAutowireComposer {
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyWindow addWindowAttachment;
	private MyGrid grid;

	private Textbox searchjudul;

	private Textbox judul;
	private MyCkEditor catatan;
	private MyDatebox tanggal;
	private MyDatebox sampai;

	private MyCheckboxConfig aktif;
	private MyCheckboxConfig bolehDiberiKomentar;

	private PengumumanPenelitian pengumumanPenelitian;

	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;
	private Textbox korespondensi;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class PengumumanPenelitianRenderer extends ais.ui.util.MyRowRenderer {
		private DetailPengumumanPenelitianHelper detailPengumumanPenelitianHelper = new DetailPengumumanPenelitianHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PengumumanPenelitian pengumumanPenelitian = (PengumumanPenelitian) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(detail);
						tabbox.setHeight("100%");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						MyTabConfig tab1 = new MyTabConfig("Diskusi");
						tab1.setParent(tabs);

						MyTabConfig tab2 = new MyTabConfig("Lampiran");
						tab2.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
						tabpanel1.setParent(tabpanels);

						detailPengumumanPenelitianHelper.displayDetailPengumuman(pengumumanPenelitian, tabpanel1);

						final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
						tabpanel2.setParent(tabpanels);
						detailPengumumanPenelitianHelper.displayAttachment(pengumumanPenelitian, tabpanel2,
								addWindowAttachment);

					}

				}
			});

			new Label(pengumumanPenelitian.getTanggal() == null ? ""
					: Common.dateFormat2.get().format(pengumumanPenelitian.getTanggal())).setParent(arg0);
			new Label(pengumumanPenelitian.getSampai() == null ? ""
					: Common.dateFormat2.get().format(pengumumanPenelitian.getSampai())).setParent(arg0);
			new Label(pengumumanPenelitian.getJudul()).setParent(arg0);
			new Label(pengumumanPenelitian.getKorespondensi()).setParent(arg0);

			long diff = 0L;
			if (pengumumanPenelitian.getTanggal() != null && pengumumanPenelitian.getSampai() != null) {
				diff = pengumumanPenelitian.getSampai().getTime() - pengumumanPenelitian.getTanggal().getTime();
				diff = (diff / (1000 * 60 * 60 * 24));
			}
			new Label(diff + " hari").setParent(arg0);

			new Label(pengumumanPenelitian.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			new Label(pengumumanPenelitian.getBolehDiberiKomentar() ? "Ya" : "Tidak").setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pengumumanPenelitian);
					addWindow.setVisible(true);
					addWindow.onModal();
				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(pengumumanPenelitian);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show("Data ini tidak dapat dihapus");
										}
									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new PengumumanPenelitian());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PengumumanPenelitian pengumumanPenelitian) {
		this.pengumumanPenelitian = pengumumanPenelitian;
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setWidth("85%");
		columns.appendChild(column);
		grid.appendChild(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		row.appendChild(
				tanggal = new MyDatebox(pengumumanPenelitian.getTanggal() == null ? ais.ui.util.WaktuUtil.getDate()
						: pengumumanPenelitian.getTanggal()));
		tanggal.setWidth("90%");
//		tanggal.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Selesai"));
		row.appendChild(
				sampai = new MyDatebox(pengumumanPenelitian.getSampai() == null ? ais.ui.util.WaktuUtil.getDate()
						: pengumumanPenelitian.getSampai()));
		sampai.setWidth("90%");
//		sampai.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
		row.appendChild(
				judul = new Textbox(pengumumanPenelitian.getJudul() == null ? "" : pengumumanPenelitian.getJudul()));
		judul.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Isi Pengumuman"));
		catatan = new MyCkEditor();
		catatan.setValue(pengumumanPenelitian.getCatatan() == null ? "" : pengumumanPenelitian.getCatatan());
		catatan.setWidth("90%");
		catatan.setHeight("180px");
		row.appendChild(catatan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(aktif = new MyCheckboxConfig());
		aktif.setChecked(pengumumanPenelitian.getAktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Boleh Diberi Komentar"));
		row.appendChild(bolehDiberiKomentar = new MyCheckboxConfig());
		bolehDiberiKomentar.setChecked(pengumumanPenelitian.getBolehDiberiKomentar());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Koresponden"));
		row.appendChild(korespondensi = new Textbox(pengumumanPenelitian.getKorespondensi()));
		korespondensi.setWidth("90%");
		korespondensi.setRows(3);

		if (korespondensi.getValue().trim().isEmpty()) {
			korespondensi.setValue(Common.getCurrentUser().getUserId());
		}

		Common.initKeterangan(rows,
				"Untuk memasukkan banyak Koresponden, masukkan username masing-masing pengguna dengan pemisah tanda koma (,)");

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Koresponden", "/img/user_male_add.png");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tambah Koresponden"));
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								korespondensi.setValue(korespondensi.getValue()
										+ (korespondensi.getValue().isEmpty() ? tbmuser.getUserId()
												: "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (judul.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Judul harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pengumumanPenelitian.getId() != null) {
			pengumumanPenelitian = (PengumumanPenelitian) session.load(PengumumanPenelitian.class,
					pengumumanPenelitian.getId());
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		pengumumanPenelitian.setSampai(sampai.getValue());
		pengumumanPenelitian.setTanggal(tanggal.getValue());
		pengumumanPenelitian.setJudul(judul.getValue());
		pengumumanPenelitian.setOleh(tbmuser.getUserId() + " (" + tbmuser.hakAkses().getRoleName() + ")");
		pengumumanPenelitian.setCatatan(catatan.getValue());
		pengumumanPenelitian.setAktif(aktif.isChecked());
		pengumumanPenelitian.setBolehDiberiKomentar(bolehDiberiKomentar.isChecked());
		pengumumanPenelitian.setKorespondensi(
				korespondensi.getValue().trim().isEmpty() ? tbmuser.getUserId() : korespondensi.getValue().trim());

		Common.refreshSaveOrUpdate(session, pengumumanPenelitian);

		PengumumanPenelitianAction.kirimEmailKeKorespondensi(pengumumanPenelitian);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PengumumanPenelitian.class);
		if (order)
			criteria.addOrder(Order.asc("tanggal"));
		criteria.add(searchjudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.ilike("judul", searchjudul.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("catatan", searchjudul.getValue().trim(), MatchMode.ANYWHERE)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PengumumanPenelitian> pengumumanPenelitian = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pengumumanPenelitian);
		grid.setRowRenderer(new PengumumanPenelitianRenderer());
		grid.setModelCheckMobile(strset);

	}

	public static void kirimEmail(final DiskusiPengumumanPenelitian diskusiPengumumanPenelitian) {
		if (!diskusiPengumumanPenelitian.getCatatan().trim().isEmpty()) {
			Common.createDefaultTimer(new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Tbmuser tbmuser = Common.getCurrentUser();
					String emailUser = "";

					if (tbmuser != null && tbmuser.getEmail() != null
							&& Common.isValidEmailAddress(tbmuser.getEmail())) {
						emailUser += emailUser.trim().isEmpty() ? tbmuser.getEmail().trim()
								: "," + tbmuser.getEmail().trim();
					}

					JSONArray userIds = new JSONArray();
					for (String email : diskusiPengumumanPenelitian.getPengumumanPenelitian().getKorespondensi().trim()
							.split(",")) {
						if (!email.trim().isEmpty()) {
							userIds.put(email);
						}
					}

					List<String> emails = diskusiPengumumanPenelitian.getPengumumanPenelitian().getKorespondensi()
							.trim().isEmpty()
									? new ArrayList<String>()
									: HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
											.add(Restrictions.in("userId",
													diskusiPengumumanPenelitian.getPengumumanPenelitian()
															.getKorespondensi().trim().split(",")))
											.setProjection(Projections.groupProperty("email")).list();
					for (String email : emails) {
						if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
							emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
						}
					}

					emails = HibernateUtil.currentSession().createCriteria(DiskusiPengumumanPenelitian.class)
							.add(Restrictions.eq("pengumumanPenelitian",
									diskusiPengumumanPenelitian.getPengumumanPenelitian()))
							.createAlias("mahasiswa", "mahasiswa")
							.setProjection(Projections.groupProperty("mahasiswa.email")).list();
					for (String email : emails) {
						if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
							emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
						}
					}

					emails = HibernateUtil.currentSession().createCriteria(DiskusiPengumumanPenelitian.class)
							.add(Restrictions.eq("pengumumanPenelitian",
									diskusiPengumumanPenelitian.getPengumumanPenelitian()))
							.createAlias("tbmuser", "tbmuser").setProjection(Projections.groupProperty("tbmuser.email"))

							.list();
					for (String email : emails) {
						if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
							emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
						}
					}

					tbmuser = diskusiPengumumanPenelitian.getTbmuser();
					Mahasiswa mahasiswa = diskusiPengumumanPenelitian.getMahasiswa();

					// System.out.println("emailUser = " + emailUser);

					if (!emailUser.trim().isEmpty()) {
						String subject = "Komentar pengumuman penelitian => "
								+ diskusiPengumumanPenelitian.getPengumumanPenelitian().getJudul();

						String body = "Komentar dari "
								+ (tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")"
										: (mahasiswa == null ? "" : mahasiswa.getNim() + " " + mahasiswa.getNama()));

						body += diskusiPengumumanPenelitian.getCatatan() + "<br><br>Isi pengumuman<hr>"
								+ diskusiPengumumanPenelitian.getPengumumanPenelitian().getCatatan();

						body += "<br><br>Komentar Lainnya<hr>";

						List<DiskusiPengumumanPenelitian> komentars = HibernateUtil.currentSession()
								.createCriteria(DiskusiPengumumanPenelitian.class)
								.add(Restrictions.eq("pengumumanPenelitian",
										diskusiPengumumanPenelitian.getPengumumanPenelitian()))
								.list();
						body += "<ul>";
						for (DiskusiPengumumanPenelitian komentar : komentars) {
							tbmuser = komentar.getTbmuser();
							mahasiswa = komentar.getMahasiswa();
							body += "<li>" + (tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")"
									: (mahasiswa == null ? "" : mahasiswa.getNim() + " " + mahasiswa.getNama()));
							body += " : " + komentar.getCatatan() + " "
									+ Common.dateFormat.get().format(komentar.getTanggal());
							body += "</li>";
						}
						body += "</ul>";

						String url = Common
								.getKonfigurasi("alamat_url_sistem_penelitian_dan_pengabdian",
										"http://simlitabmas.ecampus.id")
								.getNilai() + "/pengumuman/index/"
								+ diskusiPengumumanPenelitian.getPengumumanPenelitian().getId();

						body += "<br><br><hr>Untuk informasi lebih lanjut bisa dilihat di " + url
								+ "<br><br>Terima Kasih";

						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						MailSender.sendMail(userIds, subject, body, sender, emailUser, diskusiPengumumanPenelitian);
					}
				}
			});
		}
	}

	public static void kirimEmailKeKorespondensi(final PengumumanPenelitian pengumumanPenelitian) {
		if (!pengumumanPenelitian.getCatatan().trim().isEmpty() && !pengumumanPenelitian.getKorespondensi().isEmpty()) {
			Common.createDefaultTimer(new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					String emailUser = "";

					List<String> emails = HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.in("userId", pengumumanPenelitian.getKorespondensi().trim().split(",")))
							.setProjection(Projections.groupProperty("email")).list();
					for (String email : emails) {
						if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
							emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
						}
					}

					JSONArray userIds = new JSONArray();
					for (String email : pengumumanPenelitian.getKorespondensi().trim().split(",")) {
						if (!email.trim().isEmpty()) {
							userIds.put(email);
						}
					}

					// System.out.println("emailUser = " + emailUser);

					if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
						String subject = "Korespondensi pengumuman penelitian => " + pengumumanPenelitian.getJudul();
						Tbmuser tbmuser = Common.getCurrentUser();
						String body = "Anda ditugaskan sebagai koresponsi pada pengumuman penelitian \""
								+ pengumumanPenelitian.getJudul() + "\" oleh "
								+ (tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : "");

						body += "<br>Isi pengumuman<hr>" + pengumumanPenelitian.getCatatan();

						body += "<br><br>Komentar<hr>";

						List<DiskusiPengumumanPenelitian> komentars = HibernateUtil.currentSession()
								.createCriteria(DiskusiPengumumanPenelitian.class)
								.add(Restrictions.eq("pengumumanPenelitian", pengumumanPenelitian)).list();
						body += "<ul>";
						for (DiskusiPengumumanPenelitian komentar : komentars) {
							tbmuser = komentar.getTbmuser();
							Mahasiswa mahasiswa = komentar.getMahasiswa();
							body += "<li>" + (tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")"
									: (mahasiswa == null ? "" : mahasiswa.getNim() + " " + mahasiswa.getNama()));
							body += " : " + komentar.getCatatan() + " "
									+ Common.dateFormat.get().format(komentar.getTanggal());
							body += "</li>";
						}
						body += "</ul>";

						String url = Common
								.getKonfigurasi("alamat_url_sistem_penelitian_dan_pengabdian",
										"http://simlitabmas.ecampus.id")
								.getNilai() + "/pengumuman/index/" + pengumumanPenelitian.getId();

						body += "<br><br><hr>Untuk informasi lebih lanjut bisa dilihat di " + url
								+ "<br><br>Terima Kasih";

						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						MailSender.sendMail(userIds, subject, body, sender, emailUser, pengumumanPenelitian);
					}
				}
			});
		}
	}

}
