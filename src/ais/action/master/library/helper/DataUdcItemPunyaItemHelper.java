package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
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
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DataUdcItem;
import ais.database.model.library.DataUdcItemDetail;
import ais.database.model.library.Item;
import ais.ui.util.MyTextbox;

public class DataUdcItemPunyaItemHelper {

	private MyGrid gridItem;
	private boolean add = false;
	private boolean edit = false;
	private boolean delete = false;

	public DataUdcItemPunyaItemHelper(MyGrid gridItem) {
		this.gridItem = gridItem;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(final DataUdcItem dataUdcItem) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Item", "/img/new.gif");
		add.setVisible(DataUdcItemPunyaItemHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Item> items = new ArrayList<Item>();
				List<Row> myrows = gridItem.getRows().getChildren();
				for (Row row : myrows) {
					items.add(((DataUdcItemDetail) row.getAttribute("dataUdcItemDetail")).getItem());
				}
				AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items, false, true);
				ambilDataItemBanyak.setHeight("95%");
				ambilDataItemBanyak.setWidth("90%");
				ambilDataItemBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						for (Item item : items) {
							DataUdcItemDetail dataUdcItemDetail = new DataUdcItemDetail();
							dataUdcItemDetail.setItem(item);
							dataUdcItemDetail.setKeterangan("");
							dataUdcItemDetail.setDataUdcItem(dataUdcItem);

							if (dataUdcItem.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.save(dataUdcItemDetail);

								item.setUdcItem(dataUdcItem.getUdcItem());
								session.update(item);
							}

							Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
							rows.setParent(gridItem);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, dataUdcItemDetail);
						}
					}
				});

				ambilDataItemBanyak.onModal();

			}
		});

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(dataUdcItem);
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
		column.setLabel("Kode/ISBN/ISSN");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama/Judul");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadDataDetail(dataUdcItem);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final DataUdcItem dataUdcItem) throws Exception {

		List<DataUdcItemDetail> dataUdcItemDetails = dataUdcItem == null || dataUdcItem.getId() == null
				? new ArrayList<DataUdcItemDetail>()
				: HibernateUtil.currentSession().createCriteria(DataUdcItemDetail.class)
						.add(Restrictions.eq("dataUdcItem", dataUdcItem)).list();

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);

		for (DataUdcItemDetail dataUdcItemDetail : dataUdcItemDetails) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, dataUdcItemDetail);
		}
	}

	public void initRow(final Row row, final DataUdcItemDetail dataUdcItemDetail) throws Exception {
		row.setValign("top");row.setAttribute("dataUdcItemDetail", dataUdcItemDetail);

		new Label(dataUdcItemDetail.getItem() == null ? ""
				: dataUdcItemDetail.getItem().getIsbn() + " " + dataUdcItemDetail.getItem().getIssn()).setParent(row);

		RevisiHelper
				.createNewRevisi(DataUdcItemDetail.class, dataUdcItemDetail,
						dataUdcItemDetail.getItem() == null ? "" : dataUdcItemDetail.getItem().getNama())
				.setParent(row);

		final MyTextbox keterangan = new MyTextbox(
				dataUdcItemDetail.getKeterangan() == null ? "" : dataUdcItemDetail.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");
		keterangan.setParent(row);
		keterangan.setDisabled(!edit);
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				dataUdcItemDetail.setKeterangan(keterangan.getValue());
				row.setValign("top");row.setAttribute("dataUdcItemDetail", dataUdcItemDetail);
				if (dataUdcItemDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (dataUdcItemDetail));
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
							if (dataUdcItemDetail.getId() != null) {
								Session session = HibernateUtil.currentSession();

								Item item = dataUdcItemDetail.getItem();
								item.setUdcItem(null);
								session.update(item);

								session.delete(dataUdcItemDetail);
							}
							row.setVisible(false);
						}

					}
				});

			}
		});
	}

}
