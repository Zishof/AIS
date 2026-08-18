package ais.action.master.library.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
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
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.RakDetailDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.Rak;
import ais.database.model.library.RakDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

public class RakDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Rak rak;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private Textbox barcode;

	private Textbox kode;
	private Textbox judul;

	public RakDetailAction(Rak rak, final Textbox kode, final Textbox judul) {
		super();
		this.rak = rak;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(RakDetailAction.this);
				if (isOpen()) {
					display();
					RakDetailAction.this.kode.setValue(kode.getValue());
					RakDetailAction.this.judul.setValue(judul.getValue());
					loadData(null);
				}
			}
		});
	}

	class RakDetailRenderer extends ais.ui.util.MyRowRenderer {

		public RakDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final RakDetail rakDetail = (RakDetail) data;

			final MyDoublebox jumlah = new MyDoublebox(rakDetail.getJumlah() == null ? 0.0 : rakDetail.getJumlah());

			new Label(rakDetail.getItem() == null ? ""
					: rakDetail.getItem().getIsbn() + " " + rakDetail.getItem().getIssn()).setParent(row);

			RevisiHelper.createNewRevisi(RakDetail.class, rakDetail,
					rakDetail.getItem() == null ? "" : rakDetail.getItem().getNama()).setParent(row);

			(jumlah).setParent(row);
			jumlah.setDisabled(!edit);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Double saldo = Math.abs(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
					jumlah.setValue(saldo);
					Session session = HibernateUtil.currentSession();
					rakDetail.setJumlah(saldo);
					Common.refreshUpdate(session, (rakDetail));
				}
			});

			final MyTextbox keterangan = new MyTextbox(
					rakDetail.getKeterangan() == null ? "" : rakDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(!edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					rakDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (rakDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(!delete);
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

									Common.refreshDelete(rakDetail);

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

		Criterion criterion = Restrictions.sqlRestriction("false");
		if (!kode.getValue().trim().isEmpty()) {
			criterion = Restrictions.or(criterion,
					Restrictions.ilike("item.isbn", kode.getValue().trim(), MatchMode.ANYWHERE));

			criterion = Restrictions.or(criterion,
					Restrictions.ilike("item.isbn10", kode.getValue().trim(), MatchMode.ANYWHERE));

			criterion = Restrictions.or(criterion,
					Restrictions.ilike("item.issn", kode.getValue().trim(), MatchMode.ANYWHERE));
		}

		List<RakDetail> rakDetails = session.createCriteria(RakDetail.class).createAlias("item", "item")
				.add(kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : criterion)
				.add(judul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("item.nama", judul.getValue(), MatchMode.ANYWHERE))
				.addOrder(Order.desc("id")).add(Restrictions.eq("rak", rak)).list();

		ListModel strset = new SimpleListModel(rakDetails);
		grid.setRowRenderer(new RakDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Rak");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Item", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Item> items = session.createCriteria(RakDetail.class)
						.setProjection(Projections.groupProperty("item")).add(Restrictions.eq("rak", rak)).list();

				AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						RakDetailDao rakDetailDao = DaoFactory.getInstance().getRakDetailDao();
						for (Item item : items) {
							RakDetail rakDetail = new RakDetail();
							rakDetail.setItem(item);
							rakDetail.setJumlah(0.0);
							rakDetail.setKeterangan("");
							rakDetail.setRak(rak);
							rakDetailDao.save(rakDetail);
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

		new Label(ais.common.Common.getBahasaConfig("Input berdasarkan ISBN")).setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		new Label(ais.common.Common.getBahasaConfig("Cari: ISBN ")).setParent(toolbar);
		new Space().setParent(toolbar);
		kode = new Textbox();
		kode.setParent(toolbar);
		kode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		new Label(ais.common.Common.getBahasaConfig(" Judul ")).setParent(toolbar);
		new Space().setParent(toolbar);
		judul = new Textbox();
		judul.setParent(toolbar);
		judul.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
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

	}

	public void loadBarcode() throws Exception {
		String barcode = this.barcode.getText().trim();
		if (barcode.trim().equals("")) {
			MyMessageboxConfig.show("Barcode/ISBN/ISSN harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		ItemPunyaBarcode itemPunyaBarcode = barcode == null || barcode.trim().equals("")
				|| barcode.trim().equals("null")
						? null
						: (ItemPunyaBarcode) session.createCriteria(ItemPunyaBarcode.class)
								.add(Restrictions.ilike("barcode", barcode, MatchMode.EXACT)).setMaxResults(1)
								.uniqueResult();

		Item item = null;
		if (itemPunyaBarcode != null) {
			item = itemPunyaBarcode.getItem();
		} else {
			item = (Item) session.createCriteria(Item.class)
					.add(Restrictions.or(Restrictions.ilike("isbn10", barcode, MatchMode.EXACT),
							Restrictions.or(Restrictions.ilike("isbn10", barcode, MatchMode.EXACT),
									Restrictions.or(Restrictions.ilike("isbn", barcode, MatchMode.EXACT),
											Restrictions.ilike("issn", barcode, MatchMode.EXACT)))))
					.setMaxResults(1).uniqueResult();
		}

		if (item == null) {
			MyMessageboxConfig.show("Barcode " + barcode + " tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		RakDetail rakDetail = new RakDetail();
		rakDetail.setItem(item);
		rakDetail.setJumlah(1.0);
		rakDetail.setKeterangan("");
		rakDetail.setRak(rak);
		session.save(rakDetail);
		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}

}
