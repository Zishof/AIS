package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.ItemKomentar;

public class ItemKomentarHelper {

	private MyGrid gridKomentar;
	// private boolean add = false;
	// private boolean edit = false;
	private boolean delete = false;

	public ItemKomentarHelper(MyGrid gridKomentar) {
		this.gridKomentar = gridKomentar;
		// add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(final Item item) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridKomentar);
		gridKomentar.setParent(center);
		gridKomentar.setWidth("100%");
		gridKomentar.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridKomentar);

		MyColumnConfig column = new MyColumnConfig("Komentar");
		column.setParent(columns);

		column = new MyColumnConfig("Oleh");
		column.setParent(columns);

		column = new MyColumnConfig("Email");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("20%");

		loadDataDetail(item);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Item item) {

		List<ItemKomentar> itemKomentars = item == null || item.getId() == null ? new ArrayList<ItemKomentar>()
				: HibernateUtil.currentSession().createCriteria(ItemKomentar.class).add(Restrictions.eq("item", item))
						.list();

		Rows rows = gridKomentar.getRows() == null ? new Rows() : gridKomentar.getRows();
		rows.setParent(gridKomentar);

		for (ItemKomentar itemKomentar : itemKomentars) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, itemKomentar);
		}
	}

	public void initRow(final Row row, final ItemKomentar itemKomentar) {
		row.setValign("top");row.setAttribute("itemKomentar", itemKomentar);

		new Label(itemKomentar.getNama()).setParent(row);
		new Label(itemKomentar.getKontak()).setParent(row);
		new Label(itemKomentar.getEmail()).setParent(row);

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
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (itemKomentar.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(itemKomentar);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
