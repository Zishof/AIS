package ais.action.master.library.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.PemesananPengadaanItemDetailDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.PemesananPengadaanItem;
import ais.database.model.library.PemesananPengadaanItemDetail;
import ais.database.model.library.SaldoAwalDetail;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class PemesananPengadaanItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PemesananPengadaanItem pemesananPengadaanItem;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;
	

	private Paging paging;

	private Textbox barcode;

	private Textbox cari;

	public PemesananPengadaanItemDetailAction(PemesananPengadaanItem pemesananPengadaanItem) {
		super();
		this.pemesananPengadaanItem = pemesananPengadaanItem;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PemesananPengadaanItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
		
		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	class PemesananPengadaanItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PemesananPengadaanItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PemesananPengadaanItemDetail pemesananPengadaanItemDetail = (PemesananPengadaanItemDetail) data;

			Image image = LibraryUtil.generateImage(pemesananPengadaanItemDetail.getItem());
			image.setWidth("100%");
			image.setParent(row);

			final MyDoublebox jumlah = new MyDoublebox(
					pemesananPengadaanItemDetail.getJumlah() == null ? 0.0 : pemesananPengadaanItemDetail.getJumlah());

			new Label(pemesananPengadaanItemDetail.getItem() == null ? ""
					: pemesananPengadaanItemDetail.getItem().getIsbn() + " "
							+ pemesananPengadaanItemDetail.getItem().getIssn()).setParent(row);

			RevisiHelper.createNewRevisi(PemesananPengadaanItemDetail.class, pemesananPengadaanItemDetail,
					pemesananPengadaanItemDetail.getItem() == null ? ""
							: pemesananPengadaanItemDetail.getItem().getNama())
					.setParent(row);

			(jumlah).setParent(row);
			jumlah.setDisabled(
					pemesananPengadaanItemDetail.getPemesananPengadaanItem().getDisetujuiOleh() != null || !edit);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pemesananPengadaanItemDetail.setJumlah(jumlah.getValue());
					Common.refreshUpdate(session, (pemesananPengadaanItemDetail));
				}
			});

			final MyTextbox keterangan = new MyTextbox(pemesananPengadaanItemDetail.getKeterangan() == null ? ""
					: pemesananPengadaanItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(
					pemesananPengadaanItemDetail.getPemesananPengadaanItem().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pemesananPengadaanItemDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (pemesananPengadaanItemDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(
					pemesananPengadaanItemDetail.getPemesananPengadaanItem().getDisetujuiOleh() != null || !delete);
			button.setTooltiptext("Hapus Data");
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

									Common.refreshDelete(pemesananPengadaanItemDetail);

									loadData(null);

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
			button.setParent(toolbar);
			toolbar.setParent(row);

		}
	}
	
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging(initCriteria(false), paging);
		List<SaldoAwalDetail> saldoAwalDetails = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(saldoAwalDetails);
		grid.setRowRenderer(new PemesananPengadaanItemDetailRenderer());
		grid.setModelCheckMobile(strset);

	}
	

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PemesananPengadaanItemDetail.class).createAlias("item", "item")

				.add(cari == null || cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("item.isbn10", cari.getValue().trim()),
								Restrictions.or(Restrictions.ilike("item.isbn", cari.getValue().trim()),
										Restrictions.ilike("item.nama", cari.getValue().trim(), MatchMode.ANYWHERE))))

				.add(Restrictions.eq("pemesananPengadaanItem", pemesananPengadaanItem));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}


	public void display() {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Item Pemesanan Pengadaan Item");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);

		new Label(ais.common.Common.getBahasaConfig("Cari")).setParent(toolbar);
		cari = new Textbox(); 
		cari.setParent(toolbar);
		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig pencari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		pencari.setParent(toolbar);
		pencari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Item", "/img/add_item.png");
		button.setDisabled(pemesananPengadaanItem.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Item> items = session.createCriteria(PemesananPengadaanItemDetail.class)
						.setProjection(Projections.groupProperty("item"))
						.add(Restrictions.eq("pemesananPengadaanItem", pemesananPengadaanItem)).list();

				AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						PemesananPengadaanItemDetailDao pemesananPengadaanItemDetailDao = DaoFactory.getInstance()
								.getPemesananPengadaanItemDetailDao();
						for (Item item : items) {
							PemesananPengadaanItemDetail pemesananPengadaanItemDetail = new PemesananPengadaanItemDetail();
							pemesananPengadaanItemDetail.setItem(item);
							pemesananPengadaanItemDetail.setJumlah(0.0);
							pemesananPengadaanItemDetail.setKeterangan("");
							pemesananPengadaanItemDetail.setPemesananPengadaanItem(pemesananPengadaanItem);
							pemesananPengadaanItemDetailDao.save(pemesananPengadaanItemDetail);
						}

						loadData(null);
					}
				});
				ambilDataItemBanyak.setWidth("97%");
				ambilDataItemBanyak.setHeight("97%");
				ambilDataItemBanyak.setVisible(true);
				ambilDataItemBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		new Label("ISBN/ISSN").setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setDisabled(pemesananPengadaanItem.getDisetujuiOleh() != null);
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari ISBN/ISSN", "/img/svg/search.svg");
		cari.setDisabled(pemesananPengadaanItem.getDisetujuiOleh() != null);
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/ISBN/ISSN");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama/Judul");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
		
		South mySouth = new South();
		mySouth.setParent(borderlayout);

		paging.setParent(mySouth);
	}

	public void loadBarcode() throws Exception {
		String barcode = this.barcode.getText().trim();
		if (barcode.trim().equals("")) {
			MyMessageboxConfig.show("ISBN/ISSN harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) session.createCriteria(ItemPunyaBarcode.class)
				.add(Restrictions.ilike("barcode", barcode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();

		Item item = null;
		if (itemPunyaBarcode != null) {
			item = itemPunyaBarcode.getItem();
		} else {
			item = (Item) session.createCriteria(Item.class)
					.add(Restrictions.or(Restrictions.ilike("isbn10", barcode, MatchMode.EXACT),
							Restrictions.or(Restrictions.ilike("isbn", barcode, MatchMode.EXACT),
									Restrictions.ilike("issn", barcode, MatchMode.EXACT))))
					.setMaxResults(1).uniqueResult();
		}

		if (item == null) {
			MyMessageboxConfig.show("Barcode " + barcode + " tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		PemesananPengadaanItemDetail pemesananPengadaanItemDetail = new PemesananPengadaanItemDetail();
		pemesananPengadaanItemDetail.setItem(item);
		pemesananPengadaanItemDetail.setJumlah(1.0);
		pemesananPengadaanItemDetail.setKeterangan("");
		pemesananPengadaanItemDetail.setPemesananPengadaanItem(pemesananPengadaanItem);
		session.save(pemesananPengadaanItemDetail);
		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}

}
