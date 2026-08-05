package ais.action.master.recruitment;

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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.recruitment.ParameterVerifikasiCalonPegawai;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class ParameterVerifikasiCalonPegawaiAction extends GenericAutowireComposer
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

	private ParameterVerifikasiCalonPegawai parameterVerifikasiCalonPegawai;
	private MyToolbarbuttonConfig add;

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
		
		
		
		Session session = HibernateUtil.currentSession();

		int count = ((Number) session.createCriteria(ParameterVerifikasiCalonPegawai.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {

			String[] parameterVerifikasiCalonPegawaies = new String[] {
					"Prestasi Tingkat Sekolah/Daerah/Kapubaten",
					"Prestasi Tingkat Nasional",
					"Prestasi Tingkat Internasional" };
			for (String k : parameterVerifikasiCalonPegawaies) {
				if (k != null) {
					ParameterVerifikasiCalonPegawai parameterVerifikasiCalonPegawai = new ParameterVerifikasiCalonPegawai();
					parameterVerifikasiCalonPegawai.setNama(k.toString().trim());
					parameterVerifikasiCalonPegawai.setKeterangan("" + k.toString().trim());
					session.save(parameterVerifikasiCalonPegawai);
				}
			}

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

		String[] contents = new String[] { "id", "nama", "aktif", "nomorUrut", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ParameterVerifikasiCalonPegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class ParameterVerifikasiCalonPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final ParameterVerifikasiCalonPegawai parameterVerifikasiCalonPegawai = (ParameterVerifikasiCalonPegawai) arg1;

			RevisiHelper.createNewRevisi(ParameterVerifikasiCalonPegawai.class, parameterVerifikasiCalonPegawai,
					parameterVerifikasiCalonPegawai.getNama()).setParent(arg0);

			final Intbox nomorUrut = new Intbox(parameterVerifikasiCalonPegawai.getNomorUrut());
			nomorUrut.setParent(arg0);
			nomorUrut.setWidth("90%");

			nomorUrut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterVerifikasiCalonPegawai.setNomorUrut(nomorUrut.getValue());
					Common.refreshUpdate(parameterVerifikasiCalonPegawai);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(parameterVerifikasiCalonPegawai.getAktif());
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterVerifikasiCalonPegawai.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(parameterVerifikasiCalonPegawai);
				}
			});

			new Label(parameterVerifikasiCalonPegawai.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, parameterVerifikasiCalonPegawai,
					ParameterVerifikasiCalonPegawaiAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new ParameterVerifikasiCalonPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		parameterVerifikasiCalonPegawai = (ParameterVerifikasiCalonPegawai) obj;
		init(parameterVerifikasiCalonPegawai);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(ParameterVerifikasiCalonPegawai parameterVerifikasiCalonPegawai) {
		this.parameterVerifikasiCalonPegawai = parameterVerifikasiCalonPegawai;
		addWindow.setTitle(parameterVerifikasiCalonPegawai.getId() == null ? "Tambah Parameter Verifikasi Calon Pegawai" : "Ubah Parameter Verifikasi Calon Pegawai");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Parameter Verifikasi"));
		row.appendChild(nama = new Textbox(parameterVerifikasiCalonPegawai.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(parameterVerifikasiCalonPegawai.getKeterangan()));
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
			MyMessageboxConfig.show("Nama Parameter Verifikasi harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaParameterVerifikasiCalonPegawai();
		if (i) {
			MyMessageboxConfig.show("Nama Parameter Verifikasi sudah ada di database", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (parameterVerifikasiCalonPegawai.getId() != null) {
			parameterVerifikasiCalonPegawai = (ParameterVerifikasiCalonPegawai) session
					.load(ParameterVerifikasiCalonPegawai.class, parameterVerifikasiCalonPegawai.getId());

		}

		parameterVerifikasiCalonPegawai.setNama(nama.getValue().trim());
		parameterVerifikasiCalonPegawai.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, parameterVerifikasiCalonPegawai);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ParameterVerifikasiCalonPegawai.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ParameterVerifikasiCalonPegawai> parameterVerifikasiCalonPegawai = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(parameterVerifikasiCalonPegawai);
		grid.setRowRenderer(new ParameterVerifikasiCalonPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaParameterVerifikasiCalonPegawai() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(ParameterVerifikasiCalonPegawai.class)
				.setProjection(Projections.rowCount()).add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.EXACT))
				.add(this.parameterVerifikasiCalonPegawai.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.parameterVerifikasiCalonPegawai.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
