package ais.action.master.rab;

import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
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
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.rab.SumberDanaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class SumberDanaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private Checkbox searchaktif;

	private Intbox tahun;
	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;
	// private MyCheckboxConfig defaultItem;
	private MyDatebox tanggal;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private MyDoublebox pagu;

	private boolean edit = false;
	private boolean delete = false;

	private SumberDana sumberDana;
	private MyToolbarbuttonConfig add;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

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

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

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

		searchparent.setEventListener(new EventListener() {

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
	}

	class SumberDanaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final SumberDana sumberDana = (SumberDana) arg1;

			RevisiHelper.createNewRevisi(SumberDana.class, sumberDana, sumberDana.getKode()).setParent(arg0);
			new Label(sumberDana.getNama()).setParent(arg0);
			new Label(sumberDana.getTahun() == null ? "" : sumberDana.getTahun() + "").setParent(arg0);
			new Label(sumberDana.getTanggal() == null ? "" : Common.dateFormat2.get().format(sumberDana.getTanggal()))
					.setParent(arg0);
			new Label(sumberDana.getSatuanKerja() == null ? "" : sumberDana.getSatuanKerja().toString())
					.setParent(arg0);
			new Label(sumberDana.getPagu() == null ? "" : Common.numberFormat.get().format(sumberDana.getPagu()))
					.setParent(arg0);
			// new Label(sumberDana.getDefaultItem() != null
			// && sumberDana.getDefaultItem() ? "Ya" : "Tidak")
			// .setParent(arg0);
			new Label(sumberDana.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(sumberDana.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					sumberDana.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(sumberDana);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(sumberDana);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											SumberDanaDao sumberDanaDao = DaoFactory.getInstance().getSumberDanaDao();
											// sumberDanaDao.beginTransaction();
											sumberDanaDao.delete(sumberDanaDao.merge(sumberDana));
											// sumberDanaDao.commitTransaction();
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
		init(new SumberDana());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(SumberDana sumberDana) throws Exception {
		this.sumberDana = sumberDana;
		addWindow.setTitle(sumberDana.getId() == null ? "Tambah Sumber Dana" : "Ubah Sumber Dana");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Sumber Dana * "));
		row.appendChild(kode = new Textbox(sumberDana.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Sumber Dana *"));
		row.appendChild(nama = new Textbox(sumberDana.getNama() == null ? "" : sumberDana.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		row.appendChild(tahun = new Intbox(sumberDana.getTahun()));
		tahun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal *"));
		row.appendChild(tanggal = new MyDatebox(sumberDana.getTanggal()));
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
		satuanKerja.setValue(sumberDana.getSatuanKerja() == null
				? (Common.getCurrentUser().ambilSatuanKerja() == null ? ""
						: Common.getCurrentUser().ambilSatuanKerja().toString())
				: sumberDana.getSatuanKerja().toString());
		satuanKerja.setAttribute("satuanKerja",
				sumberDana.getSatuanKerja() == null ? Common.getCurrentUser().ambilSatuanKerja()
						: sumberDana.getSatuanKerja());
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pagu"));
		row.appendChild(pagu = new MyDoublebox(sumberDana.getPagu()));
		pagu.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("Default"));
		// row.appendChild(defaultItem = new MyCheckboxConfig());
		// defaultItem.setChecked(sumberDana.getDefaultItem() != null
		// && sumberDana.getDefaultItem());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(sumberDana.getKeterangan() == null ? "" : sumberDana.getKeterangan()));
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
			MyMessageboxConfig.show("Kode Sumber Dana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Sumber Dana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tahun.getValue() == null) {
			MyMessageboxConfig.show("Tahun harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Tanggal harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
//		if (satuanKerja.getAttribute("satuanKerja") == null) {
//			MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}

//		boolean i = checkKodeSumberDana();
//		if (i) {
//			MyMessageboxConfig.show("Kode Sumber Dana sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}
		SumberDanaDao sumberDanaDao = DaoFactory.getInstance().getSumberDanaDao();
		// if (defaultItem.isChecked()) {
		// sumberDanaDao
		// .getCurrentSession()
		// .createSQLQuery(
		// "update rab.sumber_dana set default_item = false;")
		// .executeUpdate();
		// }

		if (sumberDana.getId() != null) {
			sumberDana = sumberDanaDao.load(sumberDana.getId());
		}

		sumberDana.setPagu(pagu.getValue());
		sumberDana.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		sumberDana.setTahun(tahun.getValue());
		// sumberDana.setDefaultItem(defaultItem.isChecked());
		sumberDana.setKode(kode.getValue());
		sumberDana.setNama(nama.getValue());
		sumberDana.setKeterangan(keterangan.getValue());
		sumberDana.setTanggal(tanggal.getValue());

		if (sumberDana.getId() != null) {
			sumberDanaDao.update(sumberDana);
		} else {
			sumberDanaDao.save(sumberDana);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(SumberDana.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(
						parent == null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"),
						Restrictions.in("satuanKerja", satuanKerjas)))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<SumberDana> sumberDana = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(sumberDana);
		grid.setRowRenderer(new SumberDanaRenderer());
		grid.setModelCheckMobile(strset);

	}

//	public Boolean checkKodeSumberDana() {
//
//		Integer kotaCount = null;
//		Session session = HibernateUtil.currentSession();
//		kotaCount = ((Number) session.createCriteria(SumberDana.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
//				.add(Restrictions.eq("kode", kode.getValue().trim()))
//				.add(this.sumberDana.getId() == null ? Restrictions.sqlRestriction("1=1")
//						: Restrictions.ne("id", this.sumberDana.getId()))
//				.uniqueResult()).intValue();
//
//		return !kotaCount.equals(0);
//	}

}
