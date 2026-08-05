package ais.action.master.kursus;

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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
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
import ais.database.model.kursus.KomponenProdukKursus;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KomponenProdukKursusAction extends GenericAutowireComposer
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

	private Combobox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KomponenProdukKursus komponenProdukKursus;
	private MyToolbarbuttonConfig add;
	private MyDoublebox harga;

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

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Session session = HibernateUtil.currentSession();
		int i = ((Number) session.createCriteria(KomponenProdukKursus.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (i == 0) {

			String[] s = new String[] { "001;" + KomponenProdukKursus.VIDEO + ";5000",
					"002;" + KomponenProdukKursus.BUKU + ";130000", "003;" + KomponenProdukKursus.EBOOK + ";10000",
					"004;" + KomponenProdukKursus.LATIHAN_SOAL + ";10000",
					"005;" + KomponenProdukKursus.UJIAN + ";10000",
					"006;" + KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA + ";60000",
					"007;" + KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH + ";60000",
					"008;" + KomponenProdukKursus.EKSTRA_KURIKULER + ";60000" };
			for (String ss : s) {
				KomponenProdukKursus komponenProdukKursus = new KomponenProdukKursus();
				komponenProdukKursus.setKode(ss.split(";")[0]);
				komponenProdukKursus.setNama(ss.split(";")[1]);
				komponenProdukKursus.setHarga(Double.parseDouble(ss.split(";")[2]));
				session.save(komponenProdukKursus);
			}
			session.flush();
		}

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif", "harga" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KomponenProdukKursus.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KomponenProdukKursus.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class KomponenProdukKursusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KomponenProdukKursus komponenProdukKursus = (KomponenProdukKursus) arg1;

			RevisiHelper
					.createNewRevisi(KomponenProdukKursus.class, komponenProdukKursus, komponenProdukKursus.getNama())
					.setParent(arg0);
			new Label(Common.numberFormat.get().format(komponenProdukKursus.getHarga())).setParent(arg0);
			new Label(komponenProdukKursus.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(komponenProdukKursus.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenProdukKursus.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(komponenProdukKursus);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, komponenProdukKursus, KomponenProdukKursusAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KomponenProdukKursus());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		komponenProdukKursus = (KomponenProdukKursus) obj;
		init(komponenProdukKursus);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KomponenProdukKursus komponenProdukKursus) {
		this.komponenProdukKursus = komponenProdukKursus;
		addWindow.setTitle(komponenProdukKursus.getId() == null ? "Tambah Komponen Produk" : "Ubah Komponen Produk");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Komponen Produk *"));
		row.appendChild(nama = new Combobox());
		nama.setWidth("90%");

		for (String ss : KomponenProdukKursus.s) {
			Comboitem comboitem = new Comboitem(ss);
			comboitem.setValue(ss);
			nama.appendChild(comboitem);
		}
		nama.setReadonly(true);
		Common.selectComboItem(nama, komponenProdukKursus.getNama());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Harga Default"));
		row.appendChild(harga = new MyDoublebox(komponenProdukKursus.getHarga()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(komponenProdukKursus.getKeterangan()));
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
		if (nama.getSelectedItem() == null || nama.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Komponen Produk harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (komponenProdukKursus.getId() != null) {
			komponenProdukKursus = (KomponenProdukKursus) session.load(KomponenProdukKursus.class,
					komponenProdukKursus.getId());

		}

		komponenProdukKursus.setNama(nama.getValue());
		komponenProdukKursus.setHarga(harga.getValue());
		komponenProdukKursus.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, komponenProdukKursus);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KomponenProdukKursus.class)
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

		List<KomponenProdukKursus> komponenProdukKursus = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(komponenProdukKursus);
		grid.setRowRenderer(new KomponenProdukKursusRenderer());
		grid.setModelCheckMobile(strset);

	}

}
