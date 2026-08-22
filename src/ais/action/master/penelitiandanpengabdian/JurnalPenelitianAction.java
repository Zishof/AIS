package ais.action.master.penelitiandanpengabdian;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.DetailArtikelHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JurnalPenelitianAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;

	private MyGrid grid;

	private Textbox searchjudul;

	private Textbox judul;
	private Textbox korespondensi;
	private MyCheckboxConfig aktif;

	private JurnalPenelitian jurnalPenelitian;

	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;
	private Textbox path;
	private Textbox korespondensiGrupPengguna;
	private EventListener eventListener = null;

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

		boolean terhubungKeOjs = Common.bolehKonfigurasi("terhubung_ke_ojs", Konfigurasi.TIDAK_AKTIF);

		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Import Jurnal dari OJS", "/img/corner.gif");
		Common.appendKeToolbar(exportKeOjs, add, comp);
		if (exportKeOjs != null) { exportKeOjs.setVisible(terhubungKeOjs); }
		exportKeOjs.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				redirectToIntegratedImporter();
			}
		});

		MyToolbarbuttonConfig exportArtikelKeOjs = new MyToolbarbuttonConfig("Import Artikel dari OJS",
				"/img/corner.gif");
		Common.appendKeToolbar(exportArtikelKeOjs, add, comp);
		if (exportArtikelKeOjs != null) { exportArtikelKeOjs.setVisible(terhubungKeOjs); }
		exportArtikelKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JurnalPenelitianAction.singkronkanArtikel(null, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});
			}
		});

		String[] contents = new String[] { "id", "judul", "path", "korespondensi", "korespondensiGrupPengguna",
				"aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JurnalPenelitian.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JurnalPenelitian.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	public static void singkronkanArtikel(final JurnalPenelitian jurnalPenelitianData,
			final EventListener eventListener) {
		redirectToIntegratedImporter();
	}
	private static void redirectToIntegratedImporter() {
		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		Executions.sendRedirect(request.getContextPath() + "/jurnal/admin/importOjs");
	}

	class JurnalPenelitianRenderer extends ais.ui.util.MyRowRenderer {

		private DetailArtikelHelper detailArtikelHelper = new DetailArtikelHelper(null);

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JurnalPenelitian jurnalPenelitian = (JurnalPenelitian) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						MyWindow addWindowPengajuan = new MyWindow();
						addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

						detailArtikelHelper.displayPengajuan(false, null, PengumumanAkademis.UNTUK_UMUM,
								jurnalPenelitian, detail, addWindowPengajuan, "500px");

					}

				}
			});

			RevisiHelper.createNewRevisi(JurnalPenelitian.class, jurnalPenelitian, jurnalPenelitian.getJudul())
					.setParent(arg0);

			new Label(jurnalPenelitian.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			new Label(jurnalPenelitian.getPath()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;
			Session session = HibernateUtil.currentSession();
			for (String username : StringUtils.split(jurnalPenelitian.getKorespondensi(), ",")) {
				System.out.println("username=>" + username);
				Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("userId", username)).uniqueResult();
				String oleh = username;
				if (tbmuser != null) {
					oleh = (tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")");
				} else {
					Mahasiswa anggota = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("nim", username)).setMaxResults(1).uniqueResult();
					if (anggota != null) {
						oleh = (anggota.getNim() + " " + anggota.getNama());
					}
				}

				new Label(i + ". " + oleh).setParent(vbox);

				i++;
			}

			new Label(jurnalPenelitian.getKorespondensiGrupPengguna()).setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(jurnalPenelitian);
					addWindow.setVisible(true);
					addWindow.onModal();
				}
			});
			aksiButtons.add(button);

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

											Common.refreshDelete(jurnalPenelitian);

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
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}
	}

	public static void onAddExternal(Event event, EventListener eventListener, JurnalPenelitian jurnalPenelitian)
			throws Exception {
		JurnalPenelitianAction jurnalPenelitianAction = new JurnalPenelitianAction();
		jurnalPenelitianAction.eventListener = eventListener;
		jurnalPenelitianAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(jurnalPenelitianAction.addWindow);
		jurnalPenelitianAction.addWindow.setHeight("200px");
		jurnalPenelitianAction.addWindow.setWidth("300px");

		jurnalPenelitianAction.init(jurnalPenelitian);

		jurnalPenelitianAction.addWindow.setVisible(true);
		jurnalPenelitianAction.addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new JurnalPenelitian());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JurnalPenelitian jurnalPenelitian) {
		this.jurnalPenelitian = jurnalPenelitian;
		addWindow.setTitle(jurnalPenelitian.getId() == null ? "Tambah Jurnal" : "Ubah Jurnal");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
		row.appendChild(judul = new Textbox(jurnalPenelitian.getJudul() == null ? "" : jurnalPenelitian.getJudul()));
		judul.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(eventListener == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Path / Sub URL"));
		row.appendChild(path = new Textbox(jurnalPenelitian.getPath()));
		path.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(eventListener == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(aktif = new MyCheckboxConfig());
		aktif.setChecked(jurnalPenelitian.getAktif());

		row = new MyFormRow();
		row.setVisible(eventListener == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Koresponden"));
		row.appendChild(korespondensi = new Textbox(jurnalPenelitian.getKorespondensi()));
		korespondensi.setWidth("90%");
		korespondensi.setRows(3);

		if (korespondensi.getValue().trim().isEmpty()) {
			korespondensi.setValue(Common.getCurrentUser().getUserId());
		}

		if (eventListener == null) {
			Common.initKeterangan(rows,
					"Untuk memasukkan banyak Koresponden, masukkan username masing-masing pengguna dengan pemisah tanda koma (,)");
		}
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Koresponden", "/img/user_male_add.png");

		row = new MyFormRow();
		row.setVisible(eventListener == null);
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

		row = new MyFormRow();
		row.setVisible(eventListener == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Koresponden Grup Pengguna"));
		row.appendChild(korespondensiGrupPengguna = new Textbox(jurnalPenelitian.getKorespondensiGrupPengguna()));
		korespondensiGrupPengguna.setWidth("90%");
		korespondensiGrupPengguna.setRows(3);

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

				if (eventListener != null) {
					eventListener.onEvent(new Event("", null, JurnalPenelitianAction.this.jurnalPenelitian));
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
//		if (path.getValue().trim().equals("")) {
//			MyMessageboxConfig.show("Path harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}

		Session session = HibernateUtil.currentSession();
		if (jurnalPenelitian.getId() != null) {
			jurnalPenelitian = (JurnalPenelitian) session.load(JurnalPenelitian.class, jurnalPenelitian.getId());
		} else {
			JurnalPenelitian j = (JurnalPenelitian) session.createCriteria(JurnalPenelitian.class)
					.add(Restrictions.ilike("judul", judul.getValue().trim(), MatchMode.EXACT)).setMaxResults(1)
					.uniqueResult();
			if (j != null) {
				jurnalPenelitian = j;
			}
		}

		jurnalPenelitian.setJudul(judul.getValue());
		jurnalPenelitian.setAktif(aktif.isChecked());
		jurnalPenelitian.setPath(path.getValue().isEmpty() ? judul.getValue() : path.getValue());
		jurnalPenelitian.setKorespondensiGrupPengguna(korespondensiGrupPengguna.getValue());
		Tbmuser tbmuser = Common.getCurrentUser();
		jurnalPenelitian.setKorespondensi(
				korespondensi.getValue().trim().isEmpty() ? tbmuser.getUserId() : korespondensi.getValue().trim());

		Common.refreshSaveOrUpdate(session, jurnalPenelitian);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JurnalPenelitian.class);
		if (order)
			criteria.addOrder(Order.asc("judul"));
		criteria.add(searchjudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("judul", searchjudul.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (searchjudul == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);

		List<JurnalPenelitian> jurnalPenelitian = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(jurnalPenelitian);
		grid.setRowRenderer(new JurnalPenelitianRenderer());
		grid.setModelCheckMobile(strset);

	}

}
