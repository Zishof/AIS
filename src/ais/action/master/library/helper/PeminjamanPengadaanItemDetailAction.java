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
import ais.database.dao.library.PeminjamanPengadaanItemDetailDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.PeminjamanPengadaanItem;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.database.model.library.PesananAnggota;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class PeminjamanPengadaanItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PeminjamanPengadaanItem peminjamanPengadaanItem;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private Textbox barcode;

	public PeminjamanPengadaanItemDetailAction(PeminjamanPengadaanItem peminjamanPengadaanItem) {
		super();
		this.peminjamanPengadaanItem = peminjamanPengadaanItem;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PeminjamanPengadaanItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class PeminjamanPengadaanItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PeminjamanPengadaanItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = (PeminjamanPengadaanItemDetail) data;

			Image image = LibraryUtil.generateImage(peminjamanPengadaanItemDetail.getItem());
			image.setWidth("100%");
			image.setParent(row);

			final MyDoublebox jumlah = new MyDoublebox(peminjamanPengadaanItemDetail.getJumlah() == null ? 0.0
					: peminjamanPengadaanItemDetail.getJumlah());

			new Label(peminjamanPengadaanItemDetail.getItem() == null ? ""
					: peminjamanPengadaanItemDetail.getItem().getIsbn() + " "
							+ peminjamanPengadaanItemDetail.getItem().getIssn()).setParent(row);

			new Label(peminjamanPengadaanItemDetail.getItemPunyaBarcode() == null ? ""
					: peminjamanPengadaanItemDetail.getItemPunyaBarcode().getBarcode()).setParent(row);

			RevisiHelper.createNewRevisi(PeminjamanPengadaanItemDetail.class, peminjamanPengadaanItemDetail,
					peminjamanPengadaanItemDetail.getItem() == null ? ""
							: peminjamanPengadaanItemDetail.getItem().getNama())
					.setParent(row);

			(jumlah).setParent(row);
			jumlah.setDisabled(
					peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem().getDisetujuiOleh() != null || !edit);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (jumlah.getValue() != null
							&& jumlah.getValue() > peminjamanPengadaanItem.getPerpustakaan().getMaxPinjam()) {
						MyMessageboxConfig.show(
								"Maksimal peminjaman " + peminjamanPengadaanItem.getPerpustakaan().getNama()
										+ " tidak boleh melebihi "
										+ peminjamanPengadaanItem.getPerpustakaan().getMaxPinjam(),
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						jumlah.setValue(1.0);
						return;
					}

					Session session = HibernateUtil.currentSession();
					peminjamanPengadaanItemDetail.setJumlah(jumlah.getValue());
					session.update(peminjamanPengadaanItemDetail);
				}
			});

			final MyTextbox keterangan = new MyTextbox(peminjamanPengadaanItemDetail.getKeterangan() == null ? ""
					: peminjamanPengadaanItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(
					peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					peminjamanPengadaanItemDetail.setKeterangan(keterangan.getValue());
					session.update(peminjamanPengadaanItemDetail);
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(
					peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem().getDisetujuiOleh() != null || !delete);
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

									Common.refreshDelete(peminjamanPengadaanItemDetail);

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
		List<PeminjamanPengadaanItemDetail> peminjamanPengadaanItemDetails = session
				.createCriteria(PeminjamanPengadaanItemDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("peminjamanPengadaanItem", peminjamanPengadaanItem)).list();

		ListModel strset = new SimpleListModel(peminjamanPengadaanItemDetails);
		grid.setRowRenderer(new PeminjamanPengadaanItemDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Item Peminjaman Item");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Item", "/img/add_item.png");
		button.setDisabled(peminjamanPengadaanItem.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Item> items = session.createCriteria(PeminjamanPengadaanItemDetail.class)
						.setProjection(Projections.groupProperty("item"))
						.add(Restrictions.eq("peminjamanPengadaanItem", peminjamanPengadaanItem)).list();

				AmbilDataItemBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemBanyakBerdasarkanStok(items,
						peminjamanPengadaanItem.getPerpustakaan(), true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						PeminjamanPengadaanItemDetailDao peminjamanPengadaanItemDetailDao = DaoFactory.getInstance()
								.getPeminjamanPengadaanItemDetailDao();
						Session session = peminjamanPengadaanItemDetailDao.getCurrentSession();
						for (Item item : items) {

							PesananAnggota pesananAnggota = null;

							pesananAnggota = (PesananAnggota) session.createCriteria(PesananAnggota.class)
									.add(Restrictions.eq("perpustakaan", peminjamanPengadaanItem.getPerpustakaan()))
									.add(Restrictions.eq("anggota", peminjamanPengadaanItem.getAnggota()))
									.add(Restrictions.eq("item", item))
									.add(Restrictions.sqlRestriction("date(kadaluarsa) > date('"+Common.databaseDateFormat1.get().format(WaktuUtil.getDate())+"')"))
									.add(Restrictions.eq("status", PesananAnggota.PESAN)).addOrder(Order.desc("id"))
									.setMaxResults(1).uniqueResult();

							PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = new PeminjamanPengadaanItemDetail();
							peminjamanPengadaanItemDetail.setItem(item);
							peminjamanPengadaanItemDetail.setJumlah(1.0);
							peminjamanPengadaanItemDetail.setKeterangan("");
							peminjamanPengadaanItemDetail.setPeminjamanPengadaanItem(peminjamanPengadaanItem);
							peminjamanPengadaanItemDetail.setPesananAnggota(pesananAnggota);
							peminjamanPengadaanItemDetailDao.save(peminjamanPengadaanItemDetail);
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
		barcode.setDisabled(peminjamanPengadaanItem.getDisetujuiOleh() != null);
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setParent(toolbar);
		cari.setDisabled(peminjamanPengadaanItem.getDisetujuiOleh() != null);
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
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Barcode");
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

		PesananAnggota pesananAnggota = null;
		if (itemPunyaBarcode == null) {
			pesananAnggota = (PesananAnggota) session.createCriteria(PesananAnggota.class)
					.add(Restrictions.sqlRestriction("date(kadaluarsa) > date('"+Common.databaseDateFormat1.get().format(WaktuUtil.getDate())+"')"))
					.add(Restrictions.eq("status", PesananAnggota.PESAN))
					.add(Restrictions.ilike("kode", barcode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
		}

		if (pesananAnggota != null && pesananAnggota.getPerpustakaan() != null && !pesananAnggota.getPerpustakaan()
				.getId().equals(peminjamanPengadaanItem.getPerpustakaan().getId())) {
			MyMessageboxConfig.show(
					"Pesanan " + pesananAnggota.getItem().getNama() + " (" + barcode + ") bukan untuk perpustakaan "
							+ peminjamanPengadaanItem.getPerpustakaan().getNama() + ", namun untuk perpustakaan "
							+ pesananAnggota.getPerpustakaan().getNama(),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Item item = null;
		if (itemPunyaBarcode != null) {
			item = itemPunyaBarcode.getItem();
		} else if (pesananAnggota != null) {
			item = pesananAnggota.getItem();
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

		PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = new PeminjamanPengadaanItemDetail();
		peminjamanPengadaanItemDetail.setItem(item);
		peminjamanPengadaanItemDetail.setJumlah(1.0);
		peminjamanPengadaanItemDetail.setKeterangan("");
		peminjamanPengadaanItemDetail.setPeminjamanPengadaanItem(peminjamanPengadaanItem);
		peminjamanPengadaanItemDetail.setItemPunyaBarcode(itemPunyaBarcode);
		peminjamanPengadaanItemDetail.setPesananAnggota(pesananAnggota);

		session.save(peminjamanPengadaanItemDetail);
		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}

}
