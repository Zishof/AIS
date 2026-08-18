package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
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
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaKategoriItem;
import ais.database.model.library.KategoriItem;

public class ItemPunyaKategoriItemHelper {

	private MyGrid gridKategoriItem;
	private boolean add = false;
	// private boolean edit = false;
	private boolean delete = false;

	public ItemPunyaKategoriItemHelper(MyGrid gridKategoriItem) {
		this.gridKategoriItem = gridKategoriItem;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(final Item item) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Kategori", "/img/new.gif");
		add.setVisible(ItemPunyaKategoriItemHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<KategoriItem> kategoriItems = new ArrayList<KategoriItem>();
				List<Row> myrows = gridKategoriItem.getRows().getChildren();
				for (Row row : myrows) {
					ItemPunyaKategoriItem itemPunyaKategoriItem = (ItemPunyaKategoriItem) row
							.getAttribute("itemPunyaKategoriItem");
					// Lewati baris yang KategoriItem-nya sudah yatim/terhapus (FK orphan) --
					// sebelumnya null ikut dimasukkan lalu meledak NPE saat dirender di
					// AmbilDataKategoriItemBanyak.KategoriItemRenderer.
					if (itemPunyaKategoriItem != null && itemPunyaKategoriItem.getKategoriItem() != null) {
						kategoriItems.add(itemPunyaKategoriItem.getKategoriItem());
					}
				}
				AmbilDataKategoriItemBanyak ambilDataKategoriItemBanyak = new AmbilDataKategoriItemBanyak(
						kategoriItems);
				ambilDataKategoriItemBanyak.setHeight("95%");
				ambilDataKategoriItemBanyak.setWidth("90%");
				ambilDataKategoriItemBanyak.setParent(ExecutionsCtrl
						.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataKategoriItemBanyak
						.setEventListener(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								List<KategoriItem> kategoriItems = (List<KategoriItem>) arg0
										.getData();
								for (KategoriItem kategoriItem : kategoriItems) {
									ItemPunyaKategoriItem itemPunyaKategoriItem = new ItemPunyaKategoriItem();
									itemPunyaKategoriItem.setItem(item);
									itemPunyaKategoriItem
											.setKategoriItem(kategoriItem);

									if (item.getId() != null) {
										Session session = HibernateUtil
												.currentSession();
										session.save(itemPunyaKategoriItem);
									}

									Rows rows = gridKategoriItem.getRows() == null ? new Rows()
											: gridKategoriItem.getRows();
									rows.setParent(gridKategoriItem);
									Row row = new Row();row.setValign("top");
									row.setParent(rows);
									initRow(row, itemPunyaKategoriItem);
								}
							}
						});

				ambilDataKategoriItemBanyak.onModal();

			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridKategoriItem);
		gridKategoriItem.setParent(center);
		gridKategoriItem.setWidth("100%");
		gridKategoriItem.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridKategoriItem);

		MyColumnConfig column = new MyColumnConfig("Kategori");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("20%");

		loadDataDetail(item);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Item item) {

		List<ItemPunyaKategoriItem> itemPunyaKategoriItems = item == null
				|| item.getId() == null ? new ArrayList<ItemPunyaKategoriItem>()
				: HibernateUtil.currentSession()
						.createCriteria(ItemPunyaKategoriItem.class)
						.add(Restrictions.eq("item", item)).list();

		Rows rows = gridKategoriItem.getRows() == null ? new Rows()
				: gridKategoriItem.getRows();
		rows.setParent(gridKategoriItem);

		for (ItemPunyaKategoriItem itemPunyaKategoriItem : itemPunyaKategoriItems) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, itemPunyaKategoriItem);
		}
	}

	public void initRow(final Row row,
			final ItemPunyaKategoriItem itemPunyaKategoriItem) {
		row.setValign("top");row.setAttribute("itemPunyaKategoriItem", itemPunyaKategoriItem);

		new Label(itemPunyaKategoriItem.getKategoriItem() == null ? ""
				: itemPunyaKategoriItem.getKategoriItem().getNama())
				.setParent(row);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (itemPunyaKategoriItem.getId() != null) {
										Session session = HibernateUtil
												.currentSession();
										session.delete(itemPunyaKategoriItem);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
