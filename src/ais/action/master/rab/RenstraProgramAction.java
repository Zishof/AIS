package ais.action.master.rab;

import java.util.List;

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
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.East;
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
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.RenstraProgramPunyaIndikatorHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.rab.RenstraProgramDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.RenstraProgram;
import ais.database.model.rab.RenstraProgramPunyaIndikator;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class RenstraProgramAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private Textbox nama;
	private Textbox sasaran;
	private Intbox tahun;

	private RenstraProgramPunyaIndikatorHelper renstraProgramPunyaIndikatorHelper;

	private boolean edit = false;
	private boolean delete = false;

	private RenstraProgram renstraProgram;
	private MyToolbarbuttonConfig add;
	private MyGrid gridParameter;

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
//		if (session.getAttribute("usersTemp") == null
//				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
//			session.removeAttribute("usersTemp");
//			Common.goLogoff();
//			return;
//		}

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
	}

	class RenstraProgramRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings({ "unchecked", "deprecation" })
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final RenstraProgram renstraProgram = (RenstraProgram) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						MyWindow window = new MyWindow("", "none", false);
						window.setHeight("450px");
						window.setWidth("100%");
						window.setParent(detail);
						initDetail(renstraProgram, window);
					}
				}
			});

			RevisiHelper
					.createNewRevisi(RenstraProgram.class, renstraProgram,
							renstraProgram.getSatuanKerja() == null ? "" : renstraProgram.getSatuanKerja().toString())
					.setParent(arg0);

			new Label(renstraProgram.getTahun() + "").setParent(arg0);

			new Label(renstraProgram.getNama()).setParent(arg0);

			new Label(renstraProgram.getSasaran()).setParent(arg0);

			List<RenstraProgramPunyaIndikator> renstraProgramPunyaIndikators = HibernateUtil.currentSession()
					.createCriteria(RenstraProgramPunyaIndikator.class)
					.add(Restrictions.eq("renstraProgram", renstraProgram)).list();

			MyGrid gridParameter = new MyGrid();
			gridParameter.setParent(arg0);
			gridParameter.setWidth("100%");
			gridParameter.setHeight("100%");
			ais.ui.util.ZkCompat.setFixedLayout(gridParameter, true);
			Columns columns = new Columns();
			columns.setParent(gridParameter);

			MyColumnConfig column = new MyColumnConfig("Indikator");
			column.setParent(columns);
			// column.setWidth("20%");

			column = new MyColumnConfig("Lokasi");
			column.setParent(columns);
			column.setWidth("30%");

			// column = new MyColumnConfig("Tahun 1");
			// column.setParent(columns);
			//
			// column = new MyColumnConfig("Tahun 2");
			// column.setParent(columns);
			//
			// column = new MyColumnConfig("Tahun 3");
			// column.setParent(columns);
			//
			// column = new MyColumnConfig("Tahun 4");
			// column.setParent(columns);
			//
			// column = new MyColumnConfig("Tahun 5");
			// column.setParent(columns);
			//
			// column = new MyColumnConfig("Anggaran 1");
			// column.setParent(columns);
			//
			// column = new MyColumnConfig("Anggaran 2");
			// column.setParent(columns);
			//
			// column = new MyColumnConfig("Anggaran 3");
			// column.setParent(columns);
			//
			// column = new MyColumnConfig("Anggaran 4");
			// column.setParent(columns);
			//
			// column = new MyColumnConfig("Anggaran 5");
			// column.setParent(columns);
			//
			// column = new MyColumnConfig("Total");
			// column.setParent(columns);

			Rows rows = gridParameter.getRows() == null ? new Rows() : gridParameter.getRows();
			rows.setParent(gridParameter);

			for (RenstraProgramPunyaIndikator renstraProgramPunyaIndikator : renstraProgramPunyaIndikators) {

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);

				row.appendChild(new ais.ui.util.MyLabelConfig(renstraProgramPunyaIndikator.getIndikator()));
				row.appendChild(new ais.ui.util.MyLabelConfig(renstraProgramPunyaIndikator.getLokasi()));
				// row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get()
				// .get().format(renstraProgramPunyaIndikator.getTarget1())));
				// row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get()
				// .get().format(renstraProgramPunyaIndikator.getTarget2())));
				// row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get()
				// .get().format(renstraProgramPunyaIndikator.getTarget3())));
				// row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get()
				// .get().format(renstraProgramPunyaIndikator.getTarget4())));
				// row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get()
				// .get().format(renstraProgramPunyaIndikator.getTarget5())));
				//
				// row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get()
				// .get().format(renstraProgramPunyaIndikator.getAnggaran1())));
				// row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get()
				// .get().format(renstraProgramPunyaIndikator.getAnggaran2())));
				// row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get()
				// .get().format(renstraProgramPunyaIndikator.getAnggaran3())));
				// row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get()
				// .get().format(renstraProgramPunyaIndikator.getAnggaran4())));
				// row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get()
				// .get().format(renstraProgramPunyaIndikator.getAnggaran5())));
				//
				// Double mytotal = renstraProgramPunyaIndikator.getAnggaran1()
				// + renstraProgramPunyaIndikator.getAnggaran2()
				// + renstraProgramPunyaIndikator.getAnggaran3()
				// + renstraProgramPunyaIndikator.getAnggaran4()
				// + renstraProgramPunyaIndikator.getAnggaran5();
				//
				// final Label total = new Label(
				// Common.numberFormat.get().format(mytotal));
				// total.setParent(row);

			}

			// new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">"
			// + renstraProgram.getContent() + "</font>").setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(renstraProgram);
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
											RenstraProgramDao renstraProgramDao = DaoFactory.getInstance()
													.getRenstraProgramDao();
											renstraProgramDao.delete((renstraProgram));
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
		init(new RenstraProgram());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(final RenstraProgram renstraProgram, Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabInformasi = new MyTabConfig("Indikator");
		tabInformasi.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelInformasi = new ais.ui.util.MyTabpanel();
		tabpanelInformasi.setParent(tabpanels);

		renstraProgramPunyaIndikatorHelper = new RenstraProgramPunyaIndikatorHelper(gridParameter = new MyGrid());
		tabpanelInformasi.appendChild(renstraProgramPunyaIndikatorHelper.initDetail(renstraProgram));

	}

	private void init(RenstraProgram renstraProgram) throws Exception {
		this.renstraProgram = renstraProgram;
		addWindow.setTitle(renstraProgram.getId() == null ? "Tambah Renstra Program" : "Ubah Renstra Program");
		Common.clear(addWindow);

		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setAttribute("satuanKerja", renstraProgram.getSatuanKerja());
		satuanKerja.setValue(renstraProgram.getSatuanKerja() == null ? "" : renstraProgram.getSatuanKerja().toString());

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		East east = new East();
		east.setWidth("70%");
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setParent(borderlayout);
		initDetail(renstraProgram, east);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahun = new Intbox(renstraProgram.getTahun()));
		tahun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kegiatan Prioritas"));
		row.appendChild(nama = new Textbox(renstraProgram.getNama()));
		nama.setWidth("90%");
		nama.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sasaran (Hasil Outcomes/ Output yang Diharapkan)"));
		row.appendChild(sasaran = new Textbox(renstraProgram.getSasaran()));
		sasaran.setWidth("90%");
		sasaran.setRows(5);

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

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Satuan kerja harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (tahun.getValue() == null) {
			MyMessageboxConfig.show("Tahun harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kegiatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (sasaran.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Sasaran harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		List<Row> rows = gridParameter.getRows().getChildren();
		for (Row row : rows) {
			RenstraProgramPunyaIndikator renstraProgramPunyaIndikator = (RenstraProgramPunyaIndikator) row
					.getAttribute("renstraProgramPunyaIndikator");
			if (renstraProgramPunyaIndikator.getIndikator().trim().equals("")) {
				MyMessageboxConfig.show("Indikator harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		RenstraProgramDao renstraProgramDao = DaoFactory.getInstance().getRenstraProgramDao();
		if (renstraProgram.getId() != null) {
			renstraProgram = renstraProgramDao.load(renstraProgram.getId());

		}

		renstraProgram.setNama(nama.getValue());
		renstraProgram.setSasaran(sasaran.getValue());
		renstraProgram.setTahun(tahun.getValue());
		renstraProgram.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		renstraProgram.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());

		if (renstraProgram.getId() != null) {
			renstraProgramDao.update(renstraProgram);
		} else {
			renstraProgramDao.save(renstraProgram);
		}

		Session session = renstraProgramDao.getCurrentSession();
		for (Row row : rows) {
			RenstraProgramPunyaIndikator renstraProgramPunyaIndikator = (RenstraProgramPunyaIndikator) row
					.getAttribute("renstraProgramPunyaIndikator");
			renstraProgramPunyaIndikator.setRenstraProgram(renstraProgram);
			session.saveOrUpdate(renstraProgramPunyaIndikator);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(RenstraProgram.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<RenstraProgram> renstraProgram = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(renstraProgram);
		grid.setRowRenderer(new RenstraProgramRenderer());
		grid.setModelCheckMobile(strset);

	}

}
