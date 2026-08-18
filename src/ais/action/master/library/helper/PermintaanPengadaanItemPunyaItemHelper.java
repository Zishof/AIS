package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.Penyedia;
import ais.database.model.library.PermintaanPengadaanItem;
import ais.database.model.library.PermintaanPengadaanItemDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

public class PermintaanPengadaanItemPunyaItemHelper {

	private MyGrid gridItem;
	private boolean add = false;
	private boolean edit = false;
	private boolean delete = false;
	private Textbox barcode;

	public PermintaanPengadaanItemPunyaItemHelper(MyGrid gridItem) {
		this.gridItem = gridItem;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(final PermintaanPengadaanItem permintaanPengadaanItem) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Item", "/img/new.gif");
		add.setVisible(PermintaanPengadaanItemPunyaItemHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Item> items = new ArrayList<Item>();
				List<Row> myrows = gridItem.getRows().getChildren();
				for (Row row : myrows) {
					items.add(((PermintaanPengadaanItemDetail) row.getAttribute("permintaanPengadaanItemDetail"))
							.getItem());
				}
				AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items);
				ambilDataItemBanyak.setHeight("95%");
				ambilDataItemBanyak.setWidth("90%");
				ambilDataItemBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						for (Item item : items) {
							PermintaanPengadaanItemDetail permintaanPengadaanItemDetail = new PermintaanPengadaanItemDetail();
							permintaanPengadaanItemDetail.setItem(item);
							permintaanPengadaanItemDetail.setJumlah(1.0);
							permintaanPengadaanItemDetail.setKeterangan("");
							permintaanPengadaanItemDetail.setPermintaanPengadaanItem(permintaanPengadaanItem);

							if (permintaanPengadaanItem.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.save(permintaanPengadaanItemDetail);
							}

							Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
							rows.setParent(gridItem);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, permintaanPengadaanItemDetail);
						}
					}
				});

				ambilDataItemBanyak.onModal();

			}
		});

		add = new MyToolbarbuttonConfig("Tambah Item berdasar stok", "/img/new.gif");
		add.setVisible(PermintaanPengadaanItemPunyaItemHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Item> items = new ArrayList<Item>();
				List<Row> myrows = gridItem.getRows().getChildren();
				for (Row row : myrows) {
					items.add(((PermintaanPengadaanItemDetail) row.getAttribute("permintaanPengadaanItemDetail"))
							.getItem());
				}
				AmbilDataItemBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemBanyakBerdasarkanStok(items,
						permintaanPengadaanItem.getPerpustakaan(), false);
				ambilDataItemBanyak.setHeight("95%");
				ambilDataItemBanyak.setWidth("90%");
				ambilDataItemBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						for (Item item : items) {
							PermintaanPengadaanItemDetail permintaanPengadaanItemDetail = new PermintaanPengadaanItemDetail();
							permintaanPengadaanItemDetail.setItem(item);
							permintaanPengadaanItemDetail.setJumlah(1.0);
							permintaanPengadaanItemDetail.setKeterangan("");
							permintaanPengadaanItemDetail.setPermintaanPengadaanItem(permintaanPengadaanItem);

							if (permintaanPengadaanItem.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.save(permintaanPengadaanItemDetail);
							}

							Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
							rows.setParent(gridItem);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, permintaanPengadaanItemDetail);
						}
					}
				});

				ambilDataItemBanyak.onModal();

			}
		});

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		new Label("Barcode/ISBN/ISSN").setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setDisabled(permintaanPengadaanItem.getDisetujuiOleh() != null);
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode(permintaanPengadaanItem);
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setParent(toolbar);
		cari.setDisabled(permintaanPengadaanItem.getDisetujuiOleh() != null);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode(permintaanPengadaanItem);
			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridItem);
		gridItem.setParent(center);
		gridItem.setWidth("100%");
		gridItem.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridItem);

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
		column.setLabel("Penyedia");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadDataDetail(permintaanPengadaanItem);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final PermintaanPengadaanItem permintaanPengadaanItem) throws Exception {

		List<PermintaanPengadaanItemDetail> permintaanPengadaanItemDetails = permintaanPengadaanItem == null
				|| permintaanPengadaanItem.getId() == null ? new ArrayList<PermintaanPengadaanItemDetail>()
						: HibernateUtil.currentSession().createCriteria(PermintaanPengadaanItemDetail.class)
								.add(Restrictions.eq("permintaanPengadaanItem", permintaanPengadaanItem)).list();

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);

		for (PermintaanPengadaanItemDetail permintaanPengadaanItemDetail : permintaanPengadaanItemDetails) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, permintaanPengadaanItemDetail);
		}
	}

	public void initRow(final Row row, final PermintaanPengadaanItemDetail permintaanPengadaanItemDetail)
			throws Exception {

		Image image = LibraryUtil.generateImage(permintaanPengadaanItemDetail.getItem());
		image.setWidth("100%");
		image.setParent(row);

		row.setValign("top");row.setAttribute("permintaanPengadaanItemDetail", permintaanPengadaanItemDetail);

		final MyDoublebox jumlah = new MyDoublebox(
				permintaanPengadaanItemDetail.getJumlah() == null ? 0.0 : permintaanPengadaanItemDetail.getJumlah());

		new Label(permintaanPengadaanItemDetail.getItem() == null ? ""
				: permintaanPengadaanItemDetail.getItem().getIsbn() + " "
						+ permintaanPengadaanItemDetail.getItem().getIssn()).setParent(row);

		RevisiHelper.createNewRevisi(PermintaanPengadaanItemDetail.class, permintaanPengadaanItemDetail,
				permintaanPengadaanItemDetail.getItem() == null ? ""
						: permintaanPengadaanItemDetail.getItem().getNama())
				.setParent(row);

		(jumlah).setParent(row);
		jumlah.setDisabled(
				permintaanPengadaanItemDetail.getPermintaanPengadaanItem().getDisetujuiOleh() != null || !edit);
		jumlah.setStyle("text-align:right");
		jumlah.setWidth("90%");
		jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				permintaanPengadaanItemDetail.setJumlah(jumlah.getValue());
				row.setValign("top");row.setAttribute("permintaanPengadaanItemDetail", permintaanPengadaanItemDetail);
				if (permintaanPengadaanItemDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (permintaanPengadaanItemDetail));
				}
			}
		});

		final AmbilDataPenyediaBanbox penyedia = new AmbilDataPenyediaBanbox();
		penyedia.setParent(row);
		penyedia.setWidth("90%");
		penyedia.setDisabled(
				permintaanPengadaanItemDetail.getPermintaanPengadaanItem().getDisetujuiOleh() != null || !edit);
		penyedia.setAttribute("penyedia", permintaanPengadaanItemDetail.getPenyedia());
		penyedia.setValue(permintaanPengadaanItemDetail.getPenyedia() == null ? ""
				: permintaanPengadaanItemDetail.getPenyedia().getNama());
		penyedia.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Penyedia myPenyedia = (Penyedia) penyedia.getAttribute("penyedia");
				permintaanPengadaanItemDetail.setPenyedia(myPenyedia);
				permintaanPengadaanItemDetail.getItem().setDefaultPenyedia(myPenyedia);

				row.setValign("top");row.setAttribute("permintaanPengadaanItemDetail", permintaanPengadaanItemDetail);
				if (permintaanPengadaanItemDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (permintaanPengadaanItemDetail));
					Common.refreshUpdate(session, (permintaanPengadaanItemDetail.getItem()));
				}
			}
		});

		final MyTextbox keterangan = new MyTextbox(permintaanPengadaanItemDetail.getKeterangan() == null ? ""
				: permintaanPengadaanItemDetail.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");
		keterangan.setParent(row);
		keterangan.setDisabled(
				permintaanPengadaanItemDetail.getPermintaanPengadaanItem().getDisetujuiOleh() != null || !edit);
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				permintaanPengadaanItemDetail.setKeterangan(keterangan.getValue());
				row.setValign("top");row.setAttribute("permintaanPengadaanItemDetail", permintaanPengadaanItemDetail);
				if (permintaanPengadaanItemDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (permintaanPengadaanItemDetail));
				}
			}
		});

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							if (permintaanPengadaanItemDetail.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.delete(permintaanPengadaanItemDetail);
							}
							row.setVisible(false);
						}

					}
				});

			}
		});
	}

	public void loadBarcode(PermintaanPengadaanItem permintaanPengadaanItem) throws Exception {
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

		PermintaanPengadaanItemDetail permintaanPengadaanItemDetail = new PermintaanPengadaanItemDetail();
		permintaanPengadaanItemDetail.setItem(item);
		permintaanPengadaanItemDetail.setJumlah(1.0);
		permintaanPengadaanItemDetail.setKeterangan("");
		permintaanPengadaanItemDetail.setPermintaanPengadaanItem(permintaanPengadaanItem);

		if (permintaanPengadaanItem.getId() != null) {
			session.save(permintaanPengadaanItemDetail);
		}

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);
		Row row = new Row();row.setValign("top");
		row.setParent(rows);
		initRow(row, permintaanPengadaanItemDetail);

		this.barcode.focus();
		this.barcode.select();
	}

}
