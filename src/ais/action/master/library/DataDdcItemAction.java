package ais.action.master.library;

import java.util.List;
import java.util.Map;

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
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
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
import ais.action.master.library.helper.AmbilDataDdcItemBanbox;
import ais.action.master.library.helper.DataDdcItemDetailAction;
import ais.action.master.library.helper.DataDdcItemPunyaItemHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.DataDdcItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DataDdcItem;
import ais.database.model.library.DataDdcItemDetail;
import ais.database.model.library.DdcItem;
import ais.database.model.library.Item;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DataDdcItemAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;
	private Paging paging;

	// private AmbilDataDdcItemBanbox searchddc;
	private Textbox searchddc;

	private MyTextbox keterangan;
	private AmbilDataDdcItemBanbox ddcItem;

	private boolean edit = false;
	private boolean delete = false;

	private DataDdcItem dataDdcItem;
	private MyToolbarbuttonConfig add;
	private MyGrid gridItem;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		// searchddc.setEventListener(new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// onSearchDefault(null);
		// }
		// });

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

	// public void onCetak(Event event) throws Exception {
	// LaporanDataDdcItem laporan = new LaporanDataDdcItem();
	// laporan.setTitle("Cetak Laporan");
	// page.getFirstRoot().appendChild(laporan);
	// laporan.setHeight("95%");
	// laporan.setWidth("90%");
	// laporan.setClosable(true);
	// laporan.onModal();
	// }

	class DataDdcItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DataDdcItem dataDdcItem = (DataDdcItem) arg1;

			(new DataDdcItemDetailAction(dataDdcItem)).setParent(arg0);

			RevisiHelper.createNewRevisi(DataDdcItem.class, dataDdcItem,
					dataDdcItem.getDdcItem() == null ? "" : dataDdcItem.getDdcItem().getKode()).setParent(arg0);

			new Label(dataDdcItem.getDdcItem() == null ? "" : dataDdcItem.getDdcItem().getNama()).setParent(arg0);

			new Label(dataDdcItem.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak ");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {

					final Map parameters = ais.common.HashMapGenerator.getRand();
					parameters.put("id", dataDdcItem.getId());
					parameters.put("perpustakaan",
							Common.getCurrentPerpustakaan() == null ? "" : Common.getCurrentPerpustakaan().getNama());
					Report.generatePDFReport(Report.PDF, parameters, "library/ddc", dataDdcItem.getTanggal_dirubah());
				}

			});
			button.setParent(toolbar);

			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(dataDdcItem);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete);
			hapus.addEventListener("onClick", new EventListener() {
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

											Common.refreshDelete(dataDdcItem);

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
			hapus.setParent(toolbar);
			toolbar.setParent(arg0);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new DataDdcItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(final DataDdcItem dataDdcItem, Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabDipinjam = new MyTabConfig("Item");
		tabDipinjam.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelDipinjam = new ais.ui.util.MyTabpanel();
		tabpanelDipinjam.setParent(tabpanels);
		tabpanelDipinjam.setWidth("100%");

		tabpanelDipinjam.appendChild(new DataDdcItemPunyaItemHelper(gridItem = new MyGrid()).initDetail(dataDdcItem));

	}

	private void init(DataDdcItem dataDdcItem) throws Exception {
		this.dataDdcItem = dataDdcItem;
		addWindow.setTitle("Pendataan ");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("70%");

		initDetail(dataDdcItem, east);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Klasifikasi"));
		row.appendChild(ddcItem = new AmbilDataDdcItemBanbox());
		ddcItem.setAttribute("ddcItem", dataDdcItem.getDdcItem());
		ddcItem.setValue(dataDdcItem.getDdcItem() == null ? "" : dataDdcItem.getDdcItem().toString());
		ddcItem.setWidth("90%");
		ddcItem.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new MyTextbox(dataDdcItem.getKeterangan() == null ? "" : dataDdcItem.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(4);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
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
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (ddcItem.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Klasifikasi harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		DdcItem ddc = (DdcItem) ddcItem.getAttribute("ddcItem");
		if (ddc == null) {
			MyMessageboxConfig.show("Kode DDC tidak ditemukan, Anda harus input dulu kode DDC di menu Setup",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Integer count = ((Number) HibernateUtil.currentSession().createCriteria(DataDdcItem.class)
				.add(Restrictions.eq("ddcItem", ddc))
				.add(dataDdcItem.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", dataDdcItem.getId()))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (!count.equals(0)) {
			MyMessageboxConfig.show("Klasifikasi sudah didata", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		List<Row> rowsItem = gridItem.getRows().getChildren();
		for (Row row : rowsItem) {
			DataDdcItemDetail dataDdcItemDetail = (DataDdcItemDetail) row.getAttribute("dataDdcItemDetail");
			if (dataDdcItemDetail.getItem() == null) {
				MyMessageboxConfig.show("Item harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		DataDdcItemDao dataDdcItemDao = DaoFactory.getInstance().getDataDdcItemDao();
		if (dataDdcItem.getId() != null) {
			dataDdcItem = dataDdcItemDao.load(dataDdcItem.getId());
		}

		dataDdcItem.setDdcItem((DdcItem) ddc);
		dataDdcItem.setKeterangan(keterangan.getValue());

		if (dataDdcItem.getId() != null) {
			dataDdcItemDao.update(dataDdcItem);
		} else {
			dataDdcItemDao.save(dataDdcItem);
		}

		Session session = dataDdcItemDao.getCurrentSession();
		for (Row row : rowsItem) {
			DataDdcItemDetail dataDdcItemDetail = (DataDdcItemDetail) row.getAttribute("dataDdcItemDetail");
			dataDdcItemDetail.setDataDdcItem(dataDdcItem);
			session.saveOrUpdate(dataDdcItemDetail);

			Item item = dataDdcItemDetail.getItem();
			item.setDdcItem(dataDdcItem.getDdcItem());
			session.update(item);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DataDdcItem.class).createAlias("ddcItem", "ddcItem")
				.add(searchddc.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("ddcItem.kode", searchddc.getValue().trim(), MatchMode.ANYWHERE));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<DataDdcItem> dataDdcItem = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(dataDdcItem);
		grid.setRowRenderer(new DataDdcItemRenderer());
		grid.setModelCheckMobile(strset);

	}

}
