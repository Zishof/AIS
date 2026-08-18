package ais.action.master.sekolah;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Intbox;
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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.sekolah.PenjurusanSekolah;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PenjurusanSekolahAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;
	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private PenjurusanSekolah penjurusanSekolah;
	private MyToolbarbuttonConfig add;
	private MyCheckboxConfig dibatasiUmur;
	private Intbox umurminimal;
	private Intbox umurmaksimal;
	private MyDatebox umurDihitungTanggal;

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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "nama", "dibatasiUmur", "umurminimal", "umurmaksimal", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PenjurusanSekolah.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class PenjurusanSekolahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenjurusanSekolah penjurusanSekolah = (PenjurusanSekolah) arg1;

			RevisiHelper.createNewRevisi(PenjurusanSekolah.class, penjurusanSekolah, penjurusanSekolah.getNama())
					.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(penjurusanSekolah.getDibatasiUmur() ? "Ya" : "Tidak").setParent(vbox);
			if (penjurusanSekolah.getDibatasiUmur()) {
				new Label("Minimal " + Common.numberFormat.get().format(penjurusanSekolah.getUmurminimal())).setParent(vbox);
				new Label("Maksimal " + Common.numberFormat.get().format(penjurusanSekolah.getUmurmaksimal()))
						.setParent(vbox);
			}

			new Label(penjurusanSekolah.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(penjurusanSekolah.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					penjurusanSekolah.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(penjurusanSekolah);
				}
			});

			final MyCheckboxConfig tampilkanDiPpdb = new MyCheckboxConfig("PPDB");
			tampilkanDiPpdb.setDisabled(!edit);
			tampilkanDiPpdb.setChecked(penjurusanSekolah.getTampilkanDiPpdb());
			tampilkanDiPpdb.setParent(arg0);
			tampilkanDiPpdb.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					penjurusanSekolah.setTampilkanDiPpdb(tampilkanDiPpdb.isChecked());
					Common.refreshSaveOrUpdate(penjurusanSekolah);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, penjurusanSekolah, PenjurusanSekolahAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PenjurusanSekolah());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		penjurusanSekolah = (PenjurusanSekolah) obj;
		init(penjurusanSekolah);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PenjurusanSekolah penjurusanSekolah) {
		this.penjurusanSekolah = penjurusanSekolah;
		addWindow.setTitle(penjurusanSekolah.getId() == null ? "Tambah Penjurusan Sekolah" : "Ubah Penjurusan Sekolah");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Penjurusan Sekolah *"));
		row.appendChild(nama = new Textbox(penjurusanSekolah.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(penjurusanSekolah.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(dibatasiUmur = new MyCheckboxConfig("Dibatasi Umur"));
		dibatasiUmur.setChecked(penjurusanSekolah.getDibatasiUmur());

		int umur = 27;
		try {
			umur = Integer.parseInt(Common.getKonfigurasi("nilai_umur_calon_siswa_dibatasi", "27").getNilai().trim());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		if (penjurusanSekolah.getUmurmaksimal() == null) {
			penjurusanSekolah.setUmurmaksimal(umur);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Umur Minimal"));
		row.appendChild(umurminimal = new Intbox(penjurusanSekolah.getUmurminimal()));
		umurminimal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Umur Maksimal"));
		row.appendChild(umurmaksimal = new Intbox(penjurusanSekolah.getUmurmaksimal()));
		umurmaksimal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Umur dihitung saat tanggal"));
		row.appendChild(umurDihitungTanggal = new MyDatebox(penjurusanSekolah.getUmurDihitungTanggal()));

		final Row aa = Common.initKeterangan(rows,
				"Kosongkan tanggal apabila umur dihitung saat melakukan pendaftaran");

		EventListener eventListenerUmur = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				umurminimal.getParent().setVisible(dibatasiUmur.isChecked());
				umurmaksimal.getParent().setVisible(dibatasiUmur.isChecked());
				umurDihitungTanggal.getParent().setVisible(dibatasiUmur.isChecked());
				aa.setVisible(dibatasiUmur.isChecked());
			}
		};

		try {
			eventListenerUmur.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		dibatasiUmur.addEventListener("onClick", eventListenerUmur);

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
			MyMessageboxConfig.show("Nama Penjurusan Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaPenjurusanSekolah();
		if (i) {
			MyMessageboxConfig.show("Nama Penjurusan Sekolah sudah ada di database", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (penjurusanSekolah.getId() != null) {
			penjurusanSekolah = (PenjurusanSekolah) session.load(PenjurusanSekolah.class, penjurusanSekolah.getId());

		}

		penjurusanSekolah.setNama(nama.getValue());
		penjurusanSekolah.setKeterangan(keterangan.getValue());
		penjurusanSekolah.setUmurminimal(umurminimal.getValue());
		penjurusanSekolah.setUmurmaksimal(umurmaksimal.getValue());
		penjurusanSekolah.setDibatasiUmur(dibatasiUmur.isChecked());
		penjurusanSekolah.setUmurDihitungTanggal(umurDihitungTanggal.getValue());
		Common.refreshSaveOrUpdate(session, penjurusanSekolah);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenjurusanSekolah.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenjurusanSekolah> penjurusanSekolah = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penjurusanSekolah);
		grid.setRowRenderer(new PenjurusanSekolahRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaPenjurusanSekolah() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(PenjurusanSekolah.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.penjurusanSekolah.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.penjurusanSekolah.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
