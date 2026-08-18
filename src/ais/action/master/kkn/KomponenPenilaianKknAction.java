package ais.action.master.kkn;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.kkn.KomponenPenilaianKkn;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KomponenPenilaianKknAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KomponenPenilaianKkn komponenPenilaianKkn;
	private MyToolbarbuttonConfig add;
	private MyIntbox nomorUrut;
	private Combobox parent;
	private MyDoublebox bobot;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(KomponenPenilaianKkn.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			KomponenPenilaianKkn komponenPenilaianKkn = new KomponenPenilaianKkn(1);
			komponenPenilaianKkn.setNama("Tes Materi pembekalan umum");
			komponenPenilaianKkn.setKeterangan(
					"Tes materi pembekalan umum dilakukan setelah mahasiswa mengikuti pembekalan umum KKN. Tes materi pembekalan umum dimaksudkan untuk mengukur kemampuan pemahaman mahasiswa terhadap materi pembekalan umum.");
			session.save(komponenPenilaianKkn);

			komponenPenilaianKkn = new KomponenPenilaianKkn(2);
			komponenPenilaianKkn.setNama("Laporan Rencana Kegiatan (LRK)");
			komponenPenilaianKkn.setKeterangan(
					"Laporan ini berisi rencana pelaksanaan kegiatan yang telah disosialisasikan dan didiskusikan dengan berbagai pihak.");
			session.save(komponenPenilaianKkn);

			KomponenPenilaianKkn kinerjaMahasiswa = new KomponenPenilaianKkn(3);
			kinerjaMahasiswa.setNama("Kinerja Mahasiswa");
			kinerjaMahasiswa.setKeterangan(
					"Penilaian kinerja mahasiswa KKN dibagi menjadi 4 kriteria penilaian yaitu disiplin, kerjasama, penghayatan dan pelaksanaan kegiatan.");
			session.save(kinerjaMahasiswa);

			komponenPenilaianKkn = new KomponenPenilaianKkn(1);
			komponenPenilaianKkn.setParent(kinerjaMahasiswa);
			komponenPenilaianKkn.setNama("Pelaksanaan kegiatan");
			komponenPenilaianKkn
					.setKeterangan("Penilaian kinerja mahasiswa KKN pada kriteria penilaian pelaksanaan kegiatan.");
			session.save(komponenPenilaianKkn);

			komponenPenilaianKkn = new KomponenPenilaianKkn(2);
			komponenPenilaianKkn.setParent(kinerjaMahasiswa);
			komponenPenilaianKkn.setNama("Kerjasama");
			komponenPenilaianKkn
					.setKeterangan("Penilaian kinerja mahasiswa KKN pada kriteria penilaian kerjasama tim.");
			session.save(komponenPenilaianKkn);

			komponenPenilaianKkn = new KomponenPenilaianKkn(3);
			komponenPenilaianKkn.setParent(kinerjaMahasiswa);
			komponenPenilaianKkn.setNama("Disiplin");
			komponenPenilaianKkn.setKeterangan("Penilaian kinerja mahasiswa KKN pada kriteria penilaian kedisiplinan.");
			session.save(komponenPenilaianKkn);

			komponenPenilaianKkn = new KomponenPenilaianKkn(4);
			komponenPenilaianKkn.setParent(kinerjaMahasiswa);
			komponenPenilaianKkn.setNama("Penghayatan");
			komponenPenilaianKkn.setKeterangan("Penilaian kinerja mahasiswa KKN pada kriteria penilaian penghayatan.");
			session.save(komponenPenilaianKkn);

			komponenPenilaianKkn = new KomponenPenilaianKkn(4);
			komponenPenilaianKkn.setNama("Laporan Pelaksanaan Kegiatan (LPK)");
			komponenPenilaianKkn.setKeterangan(
					"Laporan ini berisi pelaksanaan rencana kegiatan yang telah disusun dan analisis keberhasilan program yang meliputi peluang, kendala dan solusi.");
			session.save(komponenPenilaianKkn);

			komponenPenilaianKkn = new KomponenPenilaianKkn(5);
			komponenPenilaianKkn.setNama("Responsi");
			komponenPenilaianKkn.setKeterangan(
					"Responsi dilakukan untuk mengukur tingkat keberhasilan pelaksanaan kegiatan mahasiswa di lokasi KKN");
			session.save(komponenPenilaianKkn);
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

		String[] contents = new String[] { "id", "nama", "keterangan", "nomorUrut", "bobot", "parent" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KomponenPenilaianKkn.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class KomponenPenilaianKknRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KomponenPenilaianKkn komponenPenilaianKkn = (KomponenPenilaianKkn) arg1;

			RevisiHelper
					.createNewRevisi(KomponenPenilaianKkn.class, komponenPenilaianKkn, komponenPenilaianKkn.getNama())
					.setParent(arg0);
			new Label(komponenPenilaianKkn.getNomorUrut().toString()).setParent(arg0);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(komponenPenilaianKkn.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianKkn.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianKkn);
				}
			});
			new Label(komponenPenilaianKkn.getParent() == null ? "" : komponenPenilaianKkn.getParent().getNama())
					.setParent(arg0);
			new Label(Common.numberFormat.get().format(komponenPenilaianKkn.getBobot())).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			final MyCheckboxConfig dosen1 = new MyCheckboxConfig("Dsn1");
			dosen1.setChecked(komponenPenilaianKkn.getDosen1());
			dosen1.setParent(hbox);
			dosen1.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianKkn.setDosen1(dosen1.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianKkn);
				}
			});

			final MyCheckboxConfig dosen2 = new MyCheckboxConfig("Dsn2");
			dosen2.setChecked(komponenPenilaianKkn.getDosen2());
			dosen2.setParent(hbox);
			dosen2.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianKkn.setDosen2(dosen2.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianKkn);
				}
			});
			final MyCheckboxConfig dosen3 = new MyCheckboxConfig("Dsn3");
			dosen3.setChecked(komponenPenilaianKkn.getDosen3());
			dosen3.setParent(hbox);
			dosen3.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianKkn.setDosen3(dosen3.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianKkn);
				}
			});
			final MyCheckboxConfig dosen4 = new MyCheckboxConfig("Dsn4");
			dosen4.setChecked(komponenPenilaianKkn.getDosen4());
			dosen4.setParent(hbox);
			dosen4.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianKkn.setDosen4(dosen4.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianKkn);
				}
			});
			final MyCheckboxConfig dosen5 = new MyCheckboxConfig("Dsn5");
			dosen5.setChecked(komponenPenilaianKkn.getDosen5());
			dosen5.setParent(hbox);
			dosen5.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianKkn.setDosen5(dosen5.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianKkn);
				}
			});

			new Label(komponenPenilaianKkn.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, komponenPenilaianKkn, KomponenPenilaianKknAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KomponenPenilaianKkn());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		komponenPenilaianKkn = (KomponenPenilaianKkn) obj;
		init(komponenPenilaianKkn);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KomponenPenilaianKkn komponenPenilaianKkn) {
		this.komponenPenilaianKkn = komponenPenilaianKkn;
		addWindow.setTitle(komponenPenilaianKkn.getId() == null ? "Tambah Komponen Penilaian" : "Ubah Komponen Penilaian");
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
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Komponen Penilaian"));
		row.appendChild(nama = new Textbox(komponenPenilaianKkn.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Urut"));
		row.appendChild(nomorUrut = new MyIntbox(komponenPenilaianKkn.getNomorUrut()));
		nomorUrut.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Induk"));
		row.appendChild(parent = new Combobox());
		parent.setWidth("90%");
		Common.insertComboDanSemua(parent, "nama", KomponenPenilaianKkn.class);
		Common.selectComboItem(parent, komponenPenilaianKkn.getParent());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bobot"));
		row.appendChild(bobot = new MyDoublebox(komponenPenilaianKkn.getBobot()));
		bobot.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(komponenPenilaianKkn.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, nama Komponen Penilaian KKN belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Komponen Penilaian dengan nama yang sesuai; (2) pastikan kolom nama tidak dikosongkan; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaKomponenPenilaianKkn();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, nama Komponen Penilaian KKN yang dimasukkan sudah terdaftar di database. Langkah yang dapat dilakukan: (1) gunakan nama lain yang berbeda dan belum terdaftar; (2) periksa daftar komponen penilaian yang sudah ada untuk menghindari duplikasi; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (komponenPenilaianKkn.getId() != null) {
			komponenPenilaianKkn = (KomponenPenilaianKkn) session.load(KomponenPenilaianKkn.class,
					komponenPenilaianKkn.getId());

		}

		komponenPenilaianKkn.setNama(nama.getValue());
		komponenPenilaianKkn.setNomorUrut(nomorUrut.getValue());
		komponenPenilaianKkn.setBobot(bobot.getValue());
		komponenPenilaianKkn.setKeterangan(keterangan.getValue());
		komponenPenilaianKkn.setKeterangan(keterangan.getValue());
		komponenPenilaianKkn.setParent(
				(KomponenPenilaianKkn) (parent.getSelectedItem() == null ? null : parent.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, komponenPenilaianKkn);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KomponenPenilaianKkn.class).createAlias("parent", "parent",
				Criteria.LEFT_JOIN);

		if (order)
			criteria.addOrder(Order.asc("parent.nomorUrut")).addOrder(Order.asc("nomorUrut"))
					.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KomponenPenilaianKkn> komponenPenilaianKkn = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(komponenPenilaianKkn);
		grid.setRowRenderer(new KomponenPenilaianKknRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaKomponenPenilaianKkn() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KomponenPenilaianKkn.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.komponenPenilaianKkn.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.komponenPenilaianKkn.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
