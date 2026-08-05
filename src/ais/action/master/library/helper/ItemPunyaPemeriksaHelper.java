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

import ais.action.master.helper.generic.AmbilDataPemeriksaBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaPemeriksa;

public class ItemPunyaPemeriksaHelper {

	private MyGrid gridPemeriksa;
	private boolean add = false;
	// private boolean edit = false;
	private boolean delete = false;

	public ItemPunyaPemeriksaHelper(MyGrid gridPemeriksa) {
		this.gridPemeriksa = gridPemeriksa;
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

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Pemeriksa",
				"/img/new.gif");
		add.setVisible(ItemPunyaPemeriksaHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Tbmuser> pemeriksas = new ArrayList<Tbmuser>();
				List<Row> myrows = gridPemeriksa.getRows().getChildren();
				for (Row row : myrows) {
					pemeriksas.add(((ItemPunyaPemeriksa) row
							.getAttribute("itemPunyaPemeriksa")).getPemeriksa());
				}
				AmbilDataPemeriksaBanyak ambilDataPemeriksaBanyak = new AmbilDataPemeriksaBanyak(
						pemeriksas);
				ambilDataPemeriksaBanyak.setHeight("95%");
				ambilDataPemeriksaBanyak.setWidth("90%");
				ambilDataPemeriksaBanyak.setParent(ExecutionsCtrl
						.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataPemeriksaBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Tbmuser> pemeriksas = (List<Tbmuser>) arg0
								.getData();
						for (Tbmuser pemeriksa : pemeriksas) {
							ItemPunyaPemeriksa itemPunyaPemeriksa = new ItemPunyaPemeriksa();
							itemPunyaPemeriksa.setItem(item);
							itemPunyaPemeriksa.setPemeriksa(pemeriksa);

							if (item.getId() != null) {
								Session session = HibernateUtil
										.currentSession();
								session.save(itemPunyaPemeriksa);
							}

							Rows rows = gridPemeriksa.getRows() == null ? new Rows()
									: gridPemeriksa.getRows();
							rows.setParent(gridPemeriksa);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, itemPunyaPemeriksa);
						}
					}
				});

				ambilDataPemeriksaBanyak.onModal();

			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridPemeriksa);
		gridPemeriksa.setParent(center);
		gridPemeriksa.setWidth("100%");
		gridPemeriksa.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridPemeriksa);

		MyColumnConfig column = new MyColumnConfig("Pemeriksa");
		column.setParent(columns);

		column = new MyColumnConfig("Status");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("20%");

		loadDataDetail(item);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Item item) {

		List<ItemPunyaPemeriksa> itemPunyaPemeriksas = item == null
				|| item.getId() == null ? new ArrayList<ItemPunyaPemeriksa>()
				: HibernateUtil.currentSession()
						.createCriteria(ItemPunyaPemeriksa.class)
						.add(Restrictions.eq("item", item)).list();

		Rows rows = gridPemeriksa.getRows() == null ? new Rows()
				: gridPemeriksa.getRows();
		rows.setParent(gridPemeriksa);

		for (ItemPunyaPemeriksa itemPunyaPemeriksa : itemPunyaPemeriksas) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, itemPunyaPemeriksa);
		}
	}

	public void initRow(final Row row,
			final ItemPunyaPemeriksa itemPunyaPemeriksa) {
		row.setValign("top");row.setAttribute("itemPunyaPemeriksa", itemPunyaPemeriksa);

		new Label(itemPunyaPemeriksa.getPemeriksa() == null ? ""
				: itemPunyaPemeriksa.getPemeriksa().toString()).setParent(row);
		new Label(itemPunyaPemeriksa.getStatus()).setParent(row);

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
									if (itemPunyaPemeriksa.getId() != null) {
										Session session = HibernateUtil
												.currentSession();
										session.delete(itemPunyaPemeriksa);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
