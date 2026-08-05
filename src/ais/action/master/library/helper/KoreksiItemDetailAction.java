package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.List;

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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.KoreksiItemDetailDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.KodeTransaksi;
import ais.database.model.library.KoreksiItem;
import ais.database.model.library.KoreksiItemDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

public class KoreksiItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private KoreksiItem koreksiItem;
	private MyGrid grid;

	private List<KodeTransaksi> kodeTransaksis = new ArrayList<KodeTransaksi>();

	private Textbox barcode;

	public KoreksiItemDetailAction(KoreksiItem koreksiItem) {
		super();
		this.koreksiItem = koreksiItem;
		kodeTransaksis.add(LibraryUtil.adjustmentPenambahan);
		kodeTransaksis.add(LibraryUtil.adjustmentPengurangan);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(KoreksiItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class KoreksiItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public KoreksiItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KoreksiItemDetail koreksiItemDetail = (KoreksiItemDetail) data;

			Image image = LibraryUtil.generateImage(koreksiItemDetail.getItem());
			image.setWidth("100%");
			image.setParent(row);

			new Label(koreksiItemDetail.getItem() == null ? ""
					: koreksiItemDetail.getItem().getIsbn() + " " + koreksiItemDetail.getItem().getIssn())
							.setParent(row);

			RevisiHelper
					.createNewRevisi(KoreksiItemDetail.class, koreksiItemDetail,
							koreksiItemDetail.getItem() == null ? "" : koreksiItemDetail.getItem().getNama())
					.setParent(row);

			final Label stok = new Label(Common.numberFormat.get()
					.format((koreksiItemDetail.getStok() == null ? 0.0 : koreksiItemDetail.getStok())));

			final Label stokMenjadi = new Label(Common.numberFormat.get()
					.format((koreksiItemDetail.getStokmenjadi() == null ? 0.0 : koreksiItemDetail.getStokmenjadi())));

			final MyDoublebox jumlah;
			jumlah = new MyDoublebox(koreksiItemDetail.getJumlah() == null ? 0.0 : koreksiItemDetail.getJumlah());

			final Combobox ajdusment = new Combobox();
			Common.insertComboItems(ajdusment, "nama", kodeTransaksis);
			Common.selectComboItem(ajdusment, koreksiItemDetail.getKodeTransaksi());

			final EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					KodeTransaksi kodeTransaksi = (KodeTransaksi) (ajdusment.getSelectedItem() == null ? null
							: ajdusment.getSelectedItem().getValue());
					if (kodeTransaksi == null) {
						MyMessageboxConfig.show("Pilih salah satu jenis koreksi", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						jumlah.setValue(0.0);
						return;
					}
					if (jumlah.getValue() == null) {
						jumlah.setValue(0.0);
					}
					Session session = HibernateUtil.currentSession();
					koreksiItemDetail.setKodeTransaksi(kodeTransaksi);
					koreksiItemDetail.setJumlah(Math.abs(jumlah.getValue()) * kodeTransaksi.getJenis().doubleValue());
					jumlah.setValue(koreksiItemDetail.getJumlah());

					Double menjadi = koreksiItemDetail.getJumlah() + koreksiItemDetail.getStok();
					stokMenjadi.setValue(Common.numberFormat.get().format(menjadi));
					koreksiItemDetail.setStokmenjadi(menjadi);

					Common.refreshUpdate(session, (koreksiItemDetail));

				}
			};

			ajdusment.setWidth("90%");
			ajdusment.setDisabled(koreksiItemDetail.getKoreksiItem().getDisetujuiOleh() != null);
			ajdusment.setParent(row);
			ajdusment.addEventListener("onChange", eventListener);

			jumlah.setParent(row);
			jumlah.setDisabled(koreksiItemDetail.getKoreksiItem().getDisetujuiOleh() != null);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, eventListener);

			stok.setParent(row);
			stokMenjadi.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					koreksiItemDetail.getKeterangan() == null ? "" : koreksiItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(koreksiItemDetail.getKoreksiItem().getDisetujuiOleh() != null);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					koreksiItemDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (koreksiItemDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(koreksiItemDetail.getKoreksiItem().getDisetujuiOleh() != null);
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

									Common.refreshDelete(koreksiItemDetail);

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
		Session session = HibernateUtil.currentSession();
		List<KoreksiItemDetail> koreksiItemDetails = session.createCriteria(KoreksiItemDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("koreksiItem", koreksiItem)).list();

		ListModel strset = new SimpleListModel(koreksiItemDetails);
		grid.setRowRenderer(new KoreksiItemDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Koreksi Item");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Item", "/img/add_item.png");
		button.setDisabled(koreksiItem.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Item> items = session.createCriteria(KoreksiItemDetail.class)
						.setProjection(Projections.groupProperty("item"))
						.add(Restrictions.eq("koreksiItem", koreksiItem)).list();

				AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						KoreksiItemDetailDao koreksiItemDetailDao = DaoFactory.getInstance().getKoreksiItemDetailDao();

						Session session = koreksiItemDetailDao.getCurrentSession();
						for (Item item : items) {

							String sql = "select sum((a.qty+a.qtybonus)*b.jenis) as stok from library.detail_transaksi a inner join library.kode_transaksi b on (a.kode_transaksi = b.id) where a.item = "
									+ item.getId() + " and a.perpustakaan = " + koreksiItem.getPerpustakaan().getId()
									+ ";";
							Number number = (Number) session.createSQLQuery(sql).uniqueResult();

							KoreksiItemDetail koreksiItemDetail = new KoreksiItemDetail();
							koreksiItemDetail.setItem(item);
							koreksiItemDetail.setJumlah(0.0);
							koreksiItemDetail.setStok(number == null ? 0.0 : number.doubleValue());
							koreksiItemDetail.setStokmenjadi(number == null ? 0.0 : number.doubleValue());
							koreksiItemDetail.setKeterangan("");
							koreksiItemDetail.setKoreksiItem(koreksiItem);
							koreksiItemDetailDao.save(koreksiItemDetail);
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

		button = new MyToolbarbuttonConfig("Ambil Data Item Bedasarkan Stok", "/img/add_item.png");
		button.setDisabled(koreksiItem.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Item> items = session.createCriteria(KoreksiItemDetail.class)
						.setProjection(Projections.groupProperty("item"))
						.add(Restrictions.eq("koreksiItem", koreksiItem)).list();

				AmbilDataItemBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemBanyakBerdasarkanStok(items,
						koreksiItem.getPerpustakaan(), false);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						KoreksiItemDetailDao koreksiItemDetailDao = DaoFactory.getInstance().getKoreksiItemDetailDao();

						Session session = koreksiItemDetailDao.getCurrentSession();
						for (Item item : items) {

							String sql = "select sum((a.qty+a.qtybonus)*b.jenis) as stok from library.detail_transaksi a inner join library.kode_transaksi b on (a.kode_transaksi = b.id) where a.item = "
									+ item.getId() + " and a.perpustakaan = " + koreksiItem.getPerpustakaan().getId()
									+ ";";
							Number number = (Number) session.createSQLQuery(sql).uniqueResult();

							KoreksiItemDetail koreksiItemDetail = new KoreksiItemDetail();
							koreksiItemDetail.setItem(item);
							koreksiItemDetail.setJumlah(0.0);
							koreksiItemDetail.setStok(number == null ? 0.0 : number.doubleValue());
							koreksiItemDetail.setStokmenjadi(number == null ? 0.0 : number.doubleValue());
							koreksiItemDetail.setKeterangan("");
							koreksiItemDetail.setKoreksiItem(koreksiItem);
							koreksiItemDetailDao.save(koreksiItemDetail);
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

		new Label("Barcode/ISBN/ISSN").setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setDisabled(koreksiItem.getDisetujuiOleh() != null);
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setParent(toolbar);
		cari.setDisabled(koreksiItem.getDisetujuiOleh() != null);
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
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
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
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Koreksi");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setAlign("right");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Stok");
		column.setAlign("right");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Menjadi");
		column.setAlign("right");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

	public void loadBarcode() throws Exception {
		String barcode = this.barcode.getText().trim();
		if (barcode.trim().equals("")) {
			MyMessageboxConfig.show("Barcode/ISBN/ISSN harus diisi", "Peringatan", MyMessageboxConfig.OK,
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

		String sql = "select sum((a.qty+a.qtybonus)*b.jenis) as stok from library.detail_transaksi a inner join library.kode_transaksi b on (a.kode_transaksi = b.id) where a.item = "
				+ item.getId() + " and a.perpustakaan = " + koreksiItem.getPerpustakaan().getId() + ";";
		Number number = (Number) session.createSQLQuery(sql).uniqueResult();

		KoreksiItemDetail koreksiItemDetail = new KoreksiItemDetail();
		koreksiItemDetail.setItem(item);
		koreksiItemDetail.setJumlah(0.0);
		koreksiItemDetail.setStok(number == null ? 0.0 : number.doubleValue());
		koreksiItemDetail.setStokmenjadi(number == null ? 0.0 : number.doubleValue());
		koreksiItemDetail.setKeterangan("");
		koreksiItemDetail.setKoreksiItem(koreksiItem);
		session.save(koreksiItemDetail);

		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}

}
