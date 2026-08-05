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
import ais.action.master.library.PengarangAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaPengarang;
import ais.database.model.library.Pengarang;

public class ItemPunyaPengarangHelper {

	private MyGrid gridPengarang;
	private boolean add = false;
	// private boolean edit = false;
	private boolean delete = false;

	public ItemPunyaPengarangHelper(MyGrid gridPengarang) {
		this.gridPengarang = gridPengarang;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
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

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Ambil Nama Pengarang", "/img/new.gif");
		add.setVisible(ItemPunyaPengarangHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Pengarang> pengarangs = new ArrayList<Pengarang>();
				List<Row> myrows = gridPengarang.getRows().getChildren();
				for (Row row : myrows) {
					pengarangs.add(((ItemPunyaPengarang) row.getAttribute("itemPunyaPengarang")).getPengarang());
				}
				AmbilDataPengarangBanyak ambilDataPengarangBanyak = new AmbilDataPengarangBanyak(pengarangs);
				ambilDataPengarangBanyak.setHeight("95%");
				ambilDataPengarangBanyak.setWidth("250px");
				ambilDataPengarangBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataPengarangBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Pengarang> pengarangs = (List<Pengarang>) arg0.getData();
						for (Pengarang pengarang : pengarangs) {
							ItemPunyaPengarang itemPunyaPengarang = new ItemPunyaPengarang();
							itemPunyaPengarang.setItem(item);
							itemPunyaPengarang.setPengarang(pengarang);

							if (item.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.save(itemPunyaPengarang);
							}

							Rows rows = gridPengarang.getRows() == null ? new Rows() : gridPengarang.getRows();
							rows.setParent(gridPengarang);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, itemPunyaPengarang);
						}
					}
				});

				ambilDataPengarangBanyak.onModal();

			}
		});

		add = new MyToolbarbuttonConfig("Tambah Nama Pengarang Baru", "/img/new.gif");
		add.setVisible(ItemPunyaPengarangHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				PengarangAction.onAddExternal(event, new EventListener() {
					
					@Override
					public void onEvent(Event arg0) throws Exception {
						ItemPunyaPengarang itemPunyaPengarang = new ItemPunyaPengarang();
						itemPunyaPengarang.setItem(item);
						itemPunyaPengarang.setPengarang((Pengarang) arg0.getData());

						if (item.getId() != null) {
							Session session = HibernateUtil.currentSession();
							session.save(itemPunyaPengarang);
						}

						Rows rows = gridPengarang.getRows() == null ? new Rows() : gridPengarang.getRows();
						rows.setParent(gridPengarang);
						Row row = new Row();row.setValign("top");
						row.setParent(rows);
						initRow(row, itemPunyaPengarang);
					}
				}, new Pengarang());

			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridPengarang);
		gridPengarang.setParent(center);
		gridPengarang.setWidth("100%");
		gridPengarang.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridPengarang);

		MyColumnConfig column = new MyColumnConfig("Pengarang");
		column.setParent(columns);

		column = new MyColumnConfig("Status");
		column.setWidth("20%");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("20%");

		loadDataDetail(item);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Item item) {

		List<ItemPunyaPengarang> itemPunyaPengarangs = item == null || item.getId() == null
				? new ArrayList<ItemPunyaPengarang>()
				: HibernateUtil.currentSession().createCriteria(ItemPunyaPengarang.class)
						.add(Restrictions.eq("item", item)).list();

		Rows rows = gridPengarang.getRows() == null ? new Rows() : gridPengarang.getRows();
		rows.setParent(gridPengarang);

		for (ItemPunyaPengarang itemPunyaPengarang : itemPunyaPengarangs) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, itemPunyaPengarang);
		}
	}

	public void initRow(final Row row, final ItemPunyaPengarang itemPunyaPengarang) {
		row.setValign("top");row.setAttribute("itemPunyaPengarang", itemPunyaPengarang);

		new Label(itemPunyaPengarang.getPengarang() == null ? "" : itemPunyaPengarang.getPengarang().getNama())
				.setParent(row);

		new Label(itemPunyaPengarang.getPengarang() == null ? ""
				: itemPunyaPengarang.getPengarang().getAktif() ? "Aktif" : "Tidak Aktif").setParent(row);

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
									if (itemPunyaPengarang.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(itemPunyaPengarang);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
