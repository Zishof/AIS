package ais.action.master.koperasi;

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
import ais.database.model.koperasi.JenisIdentitasAnggotaKoperasi;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JenisIdentitasAnggotaKoperasiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisIdentitasAnggotaKoperasi jenisIdentitasAnggotaKoperasi;
	private MyToolbarbuttonConfig add;
	private Textbox kode;

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
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisIdentitasAnggotaKoperasi.class, this,
				contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisIdentitasAnggotaKoperasi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class JenisIdentitasAnggotaKoperasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisIdentitasAnggotaKoperasi jenisIdentitasAnggotaKoperasi = (JenisIdentitasAnggotaKoperasi) arg1;
			new Label(jenisIdentitasAnggotaKoperasi.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(JenisIdentitasAnggotaKoperasi.class, jenisIdentitasAnggotaKoperasi,
					jenisIdentitasAnggotaKoperasi.getNama()).setParent(arg0);
			new Label(jenisIdentitasAnggotaKoperasi.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisIdentitasAnggotaKoperasi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisIdentitasAnggotaKoperasi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisIdentitasAnggotaKoperasi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisIdentitasAnggotaKoperasi,
					JenisIdentitasAnggotaKoperasiAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisIdentitasAnggotaKoperasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisIdentitasAnggotaKoperasi = (JenisIdentitasAnggotaKoperasi) obj;
		init(jenisIdentitasAnggotaKoperasi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisIdentitasAnggotaKoperasi jenisIdentitasAnggotaKoperasi) {
		this.jenisIdentitasAnggotaKoperasi = jenisIdentitasAnggotaKoperasi;
		addWindow.setTitle(jenisIdentitasAnggotaKoperasi.getId() == null ? "Tambah Jenis Identitas Koperasi" : "Ubah Jenis Identitas Koperasi");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Jenis Identitas"));
		row.appendChild(kode = new Textbox(jenisIdentitasAnggotaKoperasi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Identitas *"));
		row.appendChild(nama = new Textbox(jenisIdentitasAnggotaKoperasi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisIdentitasAnggotaKoperasi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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
			MyMessageboxConfig.show("Mohon maaf, nama jenis identitas koperasi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Jenis Identitas; (2) gunakan nama yang deskriptif (contoh: KTP, SIM, Paspor); (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaJenisIdentitasAnggotaKoperasi();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, nama jenis identitas koperasi sudah ada di database. Langkah yang dapat dilakukan: (1) gunakan nama lain yang belum terdaftar; (2) cari jenis identitas yang sudah ada di daftar; (3) ulangi penyimpanan.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisIdentitasAnggotaKoperasi.getId() != null) {
			jenisIdentitasAnggotaKoperasi = (JenisIdentitasAnggotaKoperasi) session
					.load(JenisIdentitasAnggotaKoperasi.class, jenisIdentitasAnggotaKoperasi.getId());
		}

		jenisIdentitasAnggotaKoperasi.setKode(kode.getValue());
		jenisIdentitasAnggotaKoperasi.setNama(nama.getValue());
		jenisIdentitasAnggotaKoperasi.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisIdentitasAnggotaKoperasi);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisIdentitasAnggotaKoperasi.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisIdentitasAnggotaKoperasi> jenisIdentitasAnggotaKoperasi = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisIdentitasAnggotaKoperasi);
		grid.setRowRenderer(new JenisIdentitasAnggotaKoperasiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaJenisIdentitasAnggotaKoperasi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisIdentitasAnggotaKoperasi.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.jenisIdentitasAnggotaKoperasi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisIdentitasAnggotaKoperasi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
