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
import org.zkoss.zul.Combobox;
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

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.kpi.NilaiDefaultKpi;
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

public class NilaiDefaultKpiAction extends GenericAutowireComposer
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

	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private NilaiDefaultKpi nilaiDefaultKpi;
	private MyToolbarbuttonConfig add;
	private Combobox ta;
	private AmbilDataPegawaiBanbox pegawai;
	private MyDoublebox nilai;

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

		String[] contents = new String[] { "id", "pegawai", "keterangan", "ta", "nilai", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(NilaiDefaultKpi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, NilaiDefaultKpi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class NilaiDefaultKpiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final NilaiDefaultKpi nilaiDefaultKpi = (NilaiDefaultKpi) arg1;
			new Label(nilaiDefaultKpi.getTa()).setParent(arg0);
			RevisiHelper.createNewRevisi(NilaiDefaultKpi.class, nilaiDefaultKpi, nilaiDefaultKpi.getNama())
					.setParent(arg0);
			new Label(Common.numberFormat.get().format(nilaiDefaultKpi.getNilai()) + " %").setParent(arg0);
			new Label(nilaiDefaultKpi.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(nilaiDefaultKpi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					nilaiDefaultKpi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(nilaiDefaultKpi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, nilaiDefaultKpi, NilaiDefaultKpiAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new NilaiDefaultKpi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		nilaiDefaultKpi = (NilaiDefaultKpi) obj;
		init(nilaiDefaultKpi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(NilaiDefaultKpi nilaiDefaultKpi) {
		this.nilaiDefaultKpi = nilaiDefaultKpi;
		addWindow.setTitle(nilaiDefaultKpi.getId() == null ? "Tambah Nilai Default Kpi" : "Ubah Nilai Default Kpi");
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

		Common.generateTahunAjaranJuniJuli(ta = new Combobox());

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		row.appendChild(ta);
		Common.selectComboItem(ta, nilaiDefaultKpi.getTa());
		ta.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox());
		pegawai.setAttribute("pegawai", nilaiDefaultKpi.getPegawai());
		pegawai.setValue(nilaiDefaultKpi.getPegawai() == null ? "" : nilaiDefaultKpi.getPegawai().getNama());
		pegawai.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Persen (%) *"));
		row.appendChild(nilai = new MyDoublebox(nilaiDefaultKpi.getNilai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(nilaiDefaultKpi.getKeterangan()));
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
		if (pegawai.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, data pegawai belum diisi. Langkah yang dapat dilakukan: (1) pilih pegawai terlebih dahulu melalui kolom pencarian; (2) pastikan data pegawai tersimpan dengan benar; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (ta.getSelectedItem() == null || ta.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, tahun akademik belum dipilih. Langkah yang dapat dilakukan: (1) pilih tahun akademik dari dropdown yang tersedia; (2) pastikan tahun akademik aktif sudah dikonfigurasi; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		boolean i = checkNamaNilaiDefaultKpi();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, nilai default untuk pegawai dan tahun ini sudah ada di database. Langkah yang dapat dilakukan: (1) periksa data nilai default yang sudah ada; (2) edit data yang sudah ada daripada membuat baru; (3) ulangi proses jika perlu. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (nilaiDefaultKpi.getId() != null) {
			nilaiDefaultKpi = (NilaiDefaultKpi) session.load(NilaiDefaultKpi.class, nilaiDefaultKpi.getId());

		}

		nilaiDefaultKpi.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		nilaiDefaultKpi.setNilai(nilai.getValue());
		nilaiDefaultKpi.setKeterangan(keterangan.getValue());
		nilaiDefaultKpi.setTa((String) ta.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, nilaiDefaultKpi);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(NilaiDefaultKpi.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.createAlias("pegawai", "pegawai").add(searchnama.getValue().trim().isEmpty()
				? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("pegawai.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.or(
								Restrictions.ilike("pegawai.mycode", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("pegawai.code", searchnama.getValue().trim(), MatchMode.ANYWHERE)))

		);
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<NilaiDefaultKpi> nilaiDefaultKpi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(nilaiDefaultKpi);
		grid.setRowRenderer(new NilaiDefaultKpiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaNilaiDefaultKpi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(NilaiDefaultKpi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("pegawai", pegawai.getAttribute("pegawai")))
				.add(Restrictions.eq("ta", ta.getSelectedItem().getValue()))
				.add(this.nilaiDefaultKpi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.nilaiDefaultKpi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
