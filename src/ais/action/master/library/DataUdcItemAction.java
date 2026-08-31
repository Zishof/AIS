package ais.action.master.library;

import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataUdcItemBanbox;
import ais.action.master.library.helper.DataUdcItemDetailAction;
import ais.action.master.library.helper.DataUdcItemPunyaItemHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.DataUdcItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DataUdcItem;
import ais.database.model.library.DataUdcItemDetail;
import ais.database.model.library.Item;
import ais.database.model.library.UdcItem;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk data udc item. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code MyGrid
 * grid}, {@code Paging paging}, {@code AmbilDataUdcItemBanbox searchudc}, {@code MyTextbox keterangan}, {@code
 * AmbilDataUdcItemBanbox udcItem}, {@code boolean edit}, {@code boolean delete}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code initDetail()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class DataUdcItemAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;
	private Paging paging;

	private AmbilDataUdcItemBanbox searchudc;

	private MyTextbox keterangan;
	private AmbilDataUdcItemBanbox udcItem;

	private boolean edit = false;
	private boolean delete = false;

	private DataUdcItem dataUdcItem;
	private MyToolbarbuttonConfig add;
	private MyGrid gridItem;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		searchudc.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		add.setVisible(CommonPrivilages
				.checkPrevilages(CommonPrivilages.CREATE));
		if (add != null) { add.setTooltiptext("Tambah"); }

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
	// LaporanDataUdcItem laporan = new LaporanDataUdcItem();
	// laporan.setTitle("Cetak Laporan");
	// page.getFirstRoot().appendChild(laporan);
	// laporan.setHeight("95%");
	// laporan.setWidth("90%");
	// laporan.setClosable(true);
	// laporan.onModal();
	// }

	class DataUdcItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final DataUdcItem dataUdcItem = (DataUdcItem) arg1;

			(new DataUdcItemDetailAction(dataUdcItem)).setParent(arg0);

			RevisiHelper.createNewRevisi(
					DataUdcItem.class,
					dataUdcItem,
					dataUdcItem.getUdcItem() == null ? "" : dataUdcItem
							.getUdcItem().getKode()).setParent(arg0);

			new Label(dataUdcItem.getUdcItem() == null ? "" : dataUdcItem
					.getUdcItem().getNama()).setParent(arg0);

			new Label(dataUdcItem.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak ");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {

					final Map parameters = ais.common.HashMapGenerator.getRand();
					parameters.put("id", dataUdcItem.getId());
					parameters.put("perpustakaan", Common
							.getCurrentPerpustakaan() == null ? "" : Common
							.getCurrentPerpustakaan().getNama());
					Report.generatePDFReport(Report.PDF, parameters,
							"library/udc", dataUdcItem.getTanggal_dirubah());
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
					init(dataUdcItem);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event)
										throws Exception {
									int i = new Integer(event.getData()
											.toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(dataUdcItem);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			hapus.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new DataUdcItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(final DataUdcItem dataUdcItem, Component component)
			throws Exception {
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

		tabpanelDipinjam.appendChild(new DataUdcItemPunyaItemHelper(
				gridItem = new MyGrid()).initDetail(dataUdcItem));

	}

	private void init(DataUdcItem dataUdcItem) throws Exception {
		this.dataUdcItem = dataUdcItem;
		addWindow.setTitle("Pendataan ");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("70%");

		initDetail(dataUdcItem, east);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("UdcItem"));
		row.appendChild(udcItem = new AmbilDataUdcItemBanbox());
		udcItem.setAttribute("udcItem", dataUdcItem.getUdcItem());
		udcItem.setValue(dataUdcItem.getUdcItem() == null ? "" : dataUdcItem
				.getUdcItem().toString());
		udcItem.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new MyTextbox(
				dataUdcItem.getKeterangan() == null ? "" : dataUdcItem
						.getKeterangan()));
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

		if (udcItem.getAttribute("udcItem") == null) {
			MyMessageboxConfig.show("DDC harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Integer count = ((Number) HibernateUtil
				.currentSession()
				.createCriteria(DataUdcItem.class)
				.add(Restrictions.eq("udcItem", udcItem.getAttribute("udcItem")))
				.add(dataUdcItem.getId() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ne("id",
						dataUdcItem.getId()))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		if (!count.equals(0)) {
			MyMessageboxConfig.show("UDC sudah didata", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		List<Row> rowsItem = gridItem.getRows().getChildren();
		for (Row row : rowsItem) {
			DataUdcItemDetail dataUdcItemDetail = (DataUdcItemDetail) row
					.getAttribute("dataUdcItemDetail");
			if (dataUdcItemDetail.getItem() == null) {
				MyMessageboxConfig.show("Item harus diisi", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		DataUdcItemDao dataUdcItemDao = DaoFactory.getInstance()
				.getDataUdcItemDao();
		if (dataUdcItem.getId() != null) {
			dataUdcItem = dataUdcItemDao.load(dataUdcItem.getId());
		}

		dataUdcItem.setUdcItem((UdcItem) udcItem.getAttribute("udcItem"));

		dataUdcItem.setKeterangan(keterangan.getValue());

		if (dataUdcItem.getId() != null) {
			dataUdcItemDao.update(dataUdcItem);
		} else {
			dataUdcItemDao.save(dataUdcItem);
		}

		Session session = dataUdcItemDao.getCurrentSession();
		for (Row row : rowsItem) {
			DataUdcItemDetail dataUdcItemDetail = (DataUdcItemDetail) row
					.getAttribute("dataUdcItemDetail");
			dataUdcItemDetail.setDataUdcItem(dataUdcItem);
			session.saveOrUpdate(dataUdcItemDetail);

			Item item = dataUdcItemDetail.getItem();
			item.setUdcItem(dataUdcItem.getUdcItem());
			session.update(item);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DataUdcItem.class).add((searchudc == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (
				searchudc.getAttribute("udcItem") == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq("udcItem",
						searchudc.getAttribute("udcItem"))));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<DataUdcItem> dataUdcItem = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(
						Common.ROWS_COUNT_ON_PAGE
								* (paging == null ? 0 : paging.getActivePage()))
				.list();
		ListModel strset = new SimpleListModel(dataUdcItem);
		grid.setRowRenderer(new DataUdcItemRenderer());
		grid.setModelCheckMobile(strset);
		

	}

}
