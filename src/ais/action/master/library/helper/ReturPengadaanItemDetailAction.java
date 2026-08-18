package ais.action.master.library.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
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
import ais.ui.util.MyToolbarbuttonConfig;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.ReturPengadaanItem;
import ais.database.model.library.ReturPengadaanItemDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

public class ReturPengadaanItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private ReturPengadaanItem returPengadaanItem;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	public ReturPengadaanItemDetailAction(ReturPengadaanItem returPengadaanItem) {
		super();
		this.returPengadaanItem = returPengadaanItem;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(ReturPengadaanItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class ReturPengadaanItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public ReturPengadaanItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final ReturPengadaanItemDetail returPengadaanItemDetail = (ReturPengadaanItemDetail) data;

			Image image = LibraryUtil.generateImage(returPengadaanItemDetail.getItem());
			image.setWidth("100%");
			image.setParent(row);

			final Label jumlah = new Label(returPengadaanItemDetail.getJumlah() == null ? "0.0"
					: Common.numberFormat.get().format(returPengadaanItemDetail.getJumlah()));

			final MyDoublebox dikembalikan = new MyDoublebox(returPengadaanItemDetail.getDikembalikan() == null ? 0.0
					: returPengadaanItemDetail.getDikembalikan());

			final Label sisa = new Label(
					returPengadaanItemDetail.getJumlah() == null || returPengadaanItemDetail.getDikembalikan() == null
							? ""
							: Common.numberFormat.get().format(
									returPengadaanItemDetail.getJumlah() - returPengadaanItemDetail.getDikembalikan()));

			new Label(returPengadaanItemDetail.getItem() == null ? ""
					: returPengadaanItemDetail.getItem().getIsbn() + " " + returPengadaanItemDetail.getItem().getIssn())
							.setParent(row);

			RevisiHelper.createNewRevisi(ReturPengadaanItemDetail.class, returPengadaanItemDetail,
					returPengadaanItemDetail.getItem() == null ? "" : returPengadaanItemDetail.getItem().getNama())
					.setParent(row);

			(jumlah).setParent(row);

			(dikembalikan).setParent(row);
			dikembalikan
					.setDisabled(returPengadaanItemDetail.getReturPengadaanItem().getDisetujuiOleh() != null || !edit);
			dikembalikan.setStyle("text-align:right");
			dikembalikan.setWidth("90%");
			dikembalikan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					returPengadaanItemDetail.setDikembalikan(dikembalikan.getValue());

					Common.refreshUpdate(returPengadaanItemDetail);

					String mysisa = returPengadaanItemDetail.getJumlah() == null
							|| returPengadaanItemDetail.getDikembalikan() == null ? ""
									: Common.numberFormat.get().format(returPengadaanItemDetail.getJumlah()
											- returPengadaanItemDetail.getDikembalikan());
					sisa.setValue(mysisa);
				}
			});

			(sisa).setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					returPengadaanItemDetail.getKeterangan() == null ? "" : returPengadaanItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan
					.setDisabled(returPengadaanItemDetail.getReturPengadaanItem().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					returPengadaanItemDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (returPengadaanItemDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(returPengadaanItemDetail.getReturPengadaanItem().getDisetujuiOleh() != null || !delete);
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

									Common.refreshDelete(returPengadaanItemDetail);

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
		List<ReturPengadaanItemDetail> returPengadaanItemDetails = session
				.createCriteria(ReturPengadaanItemDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("returPengadaanItem", returPengadaanItem)).list();

		ListModel strset = new SimpleListModel(returPengadaanItemDetails);
		grid.setRowRenderer(new ReturPengadaanItemDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Item Retur Pengadaan Item");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		// Toolbar toolbar = new Toolbar();
		// // toolbar.setHeight("25px");
		// toolbar.setParent(panel);
		// MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data
		// Item",
		// "/img/add_item.png");
		// button.setDisabled(returPengadaanItem.getDisetujuiOleh() != null);
		// button.addEventListener("onClick", new EventListener() {
		//
		// @SuppressWarnings("unchecked")
		// @Override
		// public void onEvent(Event event) throws Exception {
		// Session session = HibernateUtil.currentSession();
		//
		// List<Item> items = session
		// .createCriteria(ReturPengadaanItemDetail.class)
		// .setProjection(Projections.groupProperty("item"))
		// .add(Restrictions.eq("returPengadaanItem",
		// returPengadaanItem)).list();
		//
		// AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(
		// items);
		// ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
		// .appendChild(ambilDataItemBanyak);
		// ambilDataItemBanyak.setEventListener(new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// List<Item> items = (List<Item>) arg0.getData();
		// ReturPengadaanItemDetailDao returPengadaanItemDetailDao = DaoFactory
		// .getInstance().getReturPengadaanItemDetailDao();
		// for (Item item : items) {
		// ReturPengadaanItemDetail returPengadaanItemDetail = new
		// ReturPengadaanItemDetail();
		// returPengadaanItemDetail.setItem(item);
		// returPengadaanItemDetail.setJumlah(0.0);
		// returPengadaanItemDetail.setDikembalikan(0.0);
		// returPengadaanItemDetail.setKeterangan("");
		// returPengadaanItemDetail
		// .setReturPengadaanItem(returPengadaanItem);
		// returPengadaanItemDetailDao
		// .save(returPengadaanItemDetail);
		// }
		//
		// loadData(null);
		// }
		// });
		// ambilDataItemBanyak.setWidth("97%");
		// ambilDataItemBanyak.setHeight("97%");
		// ambilDataItemBanyak.setVisible(true);
		// ambilDataItemBanyak.onModal();
		// }
		//
		// });
		// button.setParent(toolbar);

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
		column.setLabel("Nama/Judul");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dikembalikan");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Selisih");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

}
