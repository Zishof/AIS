package ais.action.master.library.helper;

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
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.TransferPengadaanItemDetailDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.TransferPengadaanItem;
import ais.database.model.library.TransferPengadaanItemDetail;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class TransferPengadaanItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private TransferPengadaanItem transferPengadaanItem;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private Textbox barcode;

	public TransferPengadaanItemDetailAction(TransferPengadaanItem transferPengadaanItem) {
		super();
		this.transferPengadaanItem = transferPengadaanItem;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(TransferPengadaanItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class TransferPengadaanItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public TransferPengadaanItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final TransferPengadaanItemDetail transferPengadaanItemDetail = (TransferPengadaanItemDetail) data;

			Image image = LibraryUtil.generateImage(transferPengadaanItemDetail.getItem());
			image.setWidth("100%");
			image.setParent(row);

			// final MyDoublebox jumlah = new MyDoublebox(
			// transferPengadaanItemDetail.getJumlah() == null ? 0.0 :
			// transferPengadaanItemDetail.getJumlah());

			new Label(transferPengadaanItemDetail.getItemPunyaBarcode() == null ? ""
					: transferPengadaanItemDetail.getItemPunyaBarcode().getBarcode()).setParent(row);

			new Label(transferPengadaanItemDetail.getItem() == null ? ""
					: transferPengadaanItemDetail.getItem().getIsbn() + " "
							+ transferPengadaanItemDetail.getItem().getIssn()).setParent(row);

			RevisiHelper.createNewRevisi(TransferPengadaanItemDetail.class, transferPengadaanItemDetail,
					transferPengadaanItemDetail.getItem() == null ? ""
							: transferPengadaanItemDetail.getItem().getNama())
					.setParent(row);

			// (jumlah).setParent(row);
			// jumlah.setDisabled(
			// transferPengadaanItemDetail.getTransferPengadaanItem().getDisetujuiOleh() !=
			// null || !edit);
			// jumlah.setStyle("text-align:right");
			// jumlah.setWidth("90%");
			// jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// Session session = HibernateUtil.currentSession();
			// transferPengadaanItemDetail.setJumlah(jumlah.getValue());
			// Common.refreshUpdate(session, (transferPengadaanItemDetail));
			// }
			// });

			final MyTextbox keterangan = new MyTextbox(transferPengadaanItemDetail.getKeterangan() == null ? ""
					: transferPengadaanItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(
					transferPengadaanItemDetail.getTransferPengadaanItem().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					transferPengadaanItemDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (transferPengadaanItemDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(
					transferPengadaanItemDetail.getTransferPengadaanItem().getDisetujuiOleh() != null || !delete);
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

											Common.refreshDelete(transferPengadaanItemDetail);

											loadData(null);

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
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<TransferPengadaanItemDetail> transferPengadaanItemDetails = session
				.createCriteria(TransferPengadaanItemDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("transferPengadaanItem", transferPengadaanItem)).list();

		ListModel strset = new SimpleListModel(transferPengadaanItemDetails);
		grid.setRowRenderer(new TransferPengadaanItemDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Item Transfer Pengadaan Item");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Item", "/img/add_item.png");
		button.setDisabled(transferPengadaanItem.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemPunyaBarcode> itemPunyaBarcodes = session.createCriteria(TransferPengadaanItemDetail.class)
						.setProjection(Projections.groupProperty("itemPunyaBarcode"))
						.add(Restrictions.isNotNull("itemPunyaBarcode"))
						.add(Restrictions.eq("transferPengadaanItem", transferPengadaanItem)).list();

				AmbilDataBarcodePunyaItemBanyak ambilDataItemBanyak = new AmbilDataBarcodePunyaItemBanyak(
						itemPunyaBarcodes);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemPunyaBarcode> itemPunyaBarcodes = (List<ItemPunyaBarcode>) arg0.getData();
						TransferPengadaanItemDetailDao transferPengadaanItemDetailDao = DaoFactory.getInstance()
								.getTransferPengadaanItemDetailDao();
						for (ItemPunyaBarcode itemPunyaBarcode : itemPunyaBarcodes) {
							TransferPengadaanItemDetail transferPengadaanItemDetail = new TransferPengadaanItemDetail();
							transferPengadaanItemDetail.setItem(itemPunyaBarcode.getItem());
							transferPengadaanItemDetail.setItemPunyaBarcode(itemPunyaBarcode);
							transferPengadaanItemDetail.setKeterangan("");
							transferPengadaanItemDetail.setTransferPengadaanItem(transferPengadaanItem);
							transferPengadaanItemDetailDao.save(transferPengadaanItemDetail);
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

		new Label(ais.common.Common.getBahasaConfig("Barcode")).setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setDisabled(transferPengadaanItem.getDisetujuiOleh() != null);
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setParent(toolbar);
		cari.setDisabled(transferPengadaanItem.getDisetujuiOleh() != null);
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
		column.setLabel("Barcode");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("ISBN/ISSN");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama/Judul");
		column.setWidth("15%");

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
			MyMessageboxConfig.show("Barcode harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) session.createCriteria(ItemPunyaBarcode.class)
				.add(Restrictions.ilike("barcode", barcode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();

		// Item item = null;
		// if (itemPunyaBarcode != null) {
		// item = itemPunyaBarcode.getItem();
		// } else {
		// item = (Item) session.createCriteria(Item.class)
		// .add(Restrictions.or(Restrictions.ilike("isbn10", barcode, MatchMode.EXACT),
		// Restrictions.or(Restrictions.ilike("isbn", barcode, MatchMode.EXACT),
		// Restrictions.ilike("issn", barcode, MatchMode.EXACT))))
		// .setMaxResults(1).uniqueResult();
		// }

		if (itemPunyaBarcode == null) {
			MyMessageboxConfig.show("Barcode " + barcode + " tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		TransferPengadaanItemDetail transferPengadaanItemDetail = new TransferPengadaanItemDetail();
		transferPengadaanItemDetail.setItem(itemPunyaBarcode.getItem());
		transferPengadaanItemDetail.setItemPunyaBarcode(itemPunyaBarcode);
		// transferPengadaanItemDetail.setJumlah(1.0);
		transferPengadaanItemDetail.setKeterangan("");
		transferPengadaanItemDetail.setTransferPengadaanItem(transferPengadaanItem);
		session.save(transferPengadaanItemDetail);
		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}

}
