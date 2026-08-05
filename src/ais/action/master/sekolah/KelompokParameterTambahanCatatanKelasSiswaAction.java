package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zul.Hbox;
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
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanKelasSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class KelompokParameterTambahanCatatanKelasSiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KelompokParameterTambahanCatatanKelasSiswa kelompokParameterTambahanCatatanKelasSiswa;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Combobox sekolah;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);
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

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class KelompokParameterTambahanCatatanKelasSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelompokParameterTambahanCatatanKelasSiswa kelompokParameterTambahanCatatanKelasSiswa = (KelompokParameterTambahanCatatanKelasSiswa) arg1;

			RevisiHelper.createNewRevisi(KelompokParameterTambahanCatatanKelasSiswa.class,
					kelompokParameterTambahanCatatanKelasSiswa, kelompokParameterTambahanCatatanKelasSiswa.getNama())
					.setParent(arg0);

			new Label(kelompokParameterTambahanCatatanKelasSiswa.getYayasan() == null ? "Semua"
					: kelompokParameterTambahanCatatanKelasSiswa.getYayasan().getNama()).setParent(arg0);
			new Label(kelompokParameterTambahanCatatanKelasSiswa.getSekolah() == null ? "Semua"
					: kelompokParameterTambahanCatatanKelasSiswa.getSekolah().getNama()).setParent(arg0);

			new Label(kelompokParameterTambahanCatatanKelasSiswa.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kelompokParameterTambahanCatatanKelasSiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokParameterTambahanCatatanKelasSiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kelompokParameterTambahanCatatanKelasSiswa);
				}
			});

			final Intbox intbox = new Intbox(kelompokParameterTambahanCatatanKelasSiswa.getNomorUrut());
			intbox.setWidth("90%");
			intbox.setParent(arg0);
			intbox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokParameterTambahanCatatanKelasSiswa.setNomorUrut(intbox.getValue());
					Common.refreshSaveOrUpdate(kelompokParameterTambahanCatatanKelasSiswa);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kelompokParameterTambahanCatatanKelasSiswa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && !kelompokParameterTambahanCatatanKelasSiswa.getDefaultData());
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(kelompokParameterTambahanCatatanKelasSiswa);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
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
		init(new KelompokParameterTambahanCatatanKelasSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KelompokParameterTambahanCatatanKelasSiswa kelompokParameterTambahanCatatanKelasSiswa) {
		this.kelompokParameterTambahanCatatanKelasSiswa = kelompokParameterTambahanCatatanKelasSiswa;
		addWindow.setTitle(kelompokParameterTambahanCatatanKelasSiswa.getId() == null ? "Tambah Kelompok" : "Ubah Kelompok");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelompok"));
		row.appendChild(nama = new Textbox(kelompokParameterTambahanCatatanKelasSiswa.getNama() == null ? ""
				: kelompokParameterTambahanCatatanKelasSiswa.getNama()));
		nama.setWidth("90%");

		Tbmuser tbmuser1 = Common.getCurrentUser();
		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan,
				kelompokParameterTambahanCatatanKelasSiswa == null
						|| kelompokParameterTambahanCatatanKelasSiswa.getYayasan() == null ? tbmuser1.ambilYayasan()
								: kelompokParameterTambahanCatatanKelasSiswa.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				kelompokParameterTambahanCatatanKelasSiswa == null
						|| kelompokParameterTambahanCatatanKelasSiswa.getSekolah() == null ? tbmuser1.ambilSekolah()
								: kelompokParameterTambahanCatatanKelasSiswa.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kelompokParameterTambahanCatatanKelasSiswa.getKeterangan() == null ? ""
				: kelompokParameterTambahanCatatanKelasSiswa.getKeterangan()));
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
			MyMessageboxConfig.show("Nama Kelompok harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaKelompokParameterTambahanCatatanKelasSiswa();
		if (i) {
			MyMessageboxConfig.show("Nama Kelompok sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelompokParameterTambahanCatatanKelasSiswa.getId() != null) {
			kelompokParameterTambahanCatatanKelasSiswa = (KelompokParameterTambahanCatatanKelasSiswa) session
					.load(KelompokParameterTambahanCatatanKelasSiswa.class, kelompokParameterTambahanCatatanKelasSiswa.getId());

		}

		kelompokParameterTambahanCatatanKelasSiswa.setNama(nama.getValue());
		kelompokParameterTambahanCatatanKelasSiswa.setKeterangan(keterangan.getValue());

		kelompokParameterTambahanCatatanKelasSiswa.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		kelompokParameterTambahanCatatanKelasSiswa.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, kelompokParameterTambahanCatatanKelasSiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelompokParameterTambahanCatatanKelasSiswa.class)

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("sekolah"),
										CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false)))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("yayasan"),
										CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelompokParameterTambahanCatatanKelasSiswa> kelompokParameterTambahanCatatanKelasSiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelompokParameterTambahanCatatanKelasSiswa);
		grid.setRowRenderer(new KelompokParameterTambahanCatatanKelasSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaKelompokParameterTambahanCatatanKelasSiswa() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KelompokParameterTambahanCatatanKelasSiswa.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.kelompokParameterTambahanCatatanKelasSiswa.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kelompokParameterTambahanCatatanKelasSiswa.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
