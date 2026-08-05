package ais.action.master.kpi;

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
import ais.ui.util.MyInclude;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataParameterTambahanBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.kpi.SatuanKpi;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class SatuanKpiAction extends GenericAutowireComposer
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

	private SatuanKpi satuanKpi;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private AmbilDataParameterTambahanBanbox tambahanBanbox;

	private Tabpanel manajemenParameter;

	public void onManajemenParameter(Event event) {
		if (manajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan.zul");
			iframe.setParent(window);
		}
	}

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

		String[] contents = new String[] { "id", "kode", "nama", "parameterTambahan", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(SatuanKpi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, SatuanKpi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class SatuanKpiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final SatuanKpi satuanKpi = (SatuanKpi) arg1;
			new Label(satuanKpi.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(SatuanKpi.class, satuanKpi, satuanKpi.getNama()).setParent(arg0);
			new Label(satuanKpi.getParameterTambahan() == null ? ""
					: satuanKpi.getParameterTambahan().getKode() + "-" + satuanKpi.getParameterTambahan().getNama())
							.setParent(arg0);
			new Label(satuanKpi.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(satuanKpi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					satuanKpi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(satuanKpi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, satuanKpi, SatuanKpiAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new SatuanKpi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		satuanKpi = (SatuanKpi) obj;
		init(satuanKpi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(SatuanKpi satuanKpi) {
		this.satuanKpi = satuanKpi;
		addWindow.setTitle(satuanKpi.getId() == null ? "Tambah Satuan KPI" : "Ubah Satuan KPI");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Satuan KPI *"));
		row.appendChild(kode = new Textbox(satuanKpi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Satuan KPI *"));
		row.appendChild(nama = new Textbox(satuanKpi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Parameter KPI"));
		tambahanBanbox = new AmbilDataParameterTambahanBanbox();
		tambahanBanbox.setAttribute("parameterTambahan", satuanKpi.getParameterTambahan());
		tambahanBanbox.setWidth("90%");
		tambahanBanbox.setValue(satuanKpi.getParameterTambahan() == null ? ""
				: (satuanKpi.getParameterTambahan().getKode() + "-" + satuanKpi.getParameterTambahan().getNama()));
		row.appendChild(tambahanBanbox);
		tambahanBanbox.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(satuanKpi.getKeterangan()));
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
		
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Satuan KPI belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Kode Satuan KPI dengan kode yang sesuai; (2) pastikan kolom tidak kosong; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Satuan KPI belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Satuan KPI dengan nama yang sesuai; (2) pastikan kolom tidak kosong; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaSatuanKpi();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Nama Satuan KPI yang dimasukkan sudah ada di database. Langkah yang dapat dilakukan: (1) gunakan nama lain yang belum terdaftar; (2) periksa daftar Satuan KPI yang sudah ada; (3) ulangi proses penyimpanan dengan nama berbeda. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (satuanKpi.getId() != null) {
			satuanKpi = (SatuanKpi) session.load(SatuanKpi.class, satuanKpi.getId());

		}

		satuanKpi.setKode(kode.getValue());
		satuanKpi.setNama(nama.getValue());
		ParameterTambahan parameterTambahan = (ParameterTambahan) tambahanBanbox.getAttribute("parameterTambahan");
		satuanKpi.setParameterTambahan(parameterTambahan);
		satuanKpi.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, satuanKpi);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(SatuanKpi.class)
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

		List<SatuanKpi> satuanKpi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(satuanKpi);
		grid.setRowRenderer(new SatuanKpiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaSatuanKpi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(SatuanKpi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.satuanKpi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.satuanKpi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
