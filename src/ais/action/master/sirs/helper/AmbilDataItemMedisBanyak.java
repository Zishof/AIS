package ais.action.master.sirs.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.JenisItemMedis;
import ais.ui.util.MyTextbox;

public class AmbilDataItemMedisBanyak extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<ItemMedis> items;
	private List<ItemMedis> itemsHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();
	private JenisItemMedis myjenisItemMedis;

	public AmbilDataItemMedisBanyak(List<ItemMedis> items) {
		super();
		this.items = items;
		this.myjenisItemMedis = null;
		display();
		onSearchDefault(null);
	}

	public AmbilDataItemMedisBanyak(List<ItemMedis> items, JenisItemMedis jenisItemMedis) {
		super();
		this.items = items;
		this.myjenisItemMedis = jenisItemMedis;
		display();
		onSearchDefault(null);
	}

	// public AmbilDataItemMedisBanyak(JenisItemMedis jenisItemMedis) {
	// super();
	// this.myjenisItemMedis = jenisItemMedis;
	// this.items = new ArrayList<ItemMedis>();
	// display();
	//
	// onSearchDefault(null);
	// }

	public AmbilDataItemMedisBanyak(List<ItemMedis> items, List<ItemMedis> itemsHanyaDitampilkan) {
		super();
		this.items = items;
		this.itemsHanyaDitampilkan = itemsHanyaDitampilkan;
		display();

		onSearchDefault(null);

	}

	private MyTextbox kodeItemMedisan;
	private MyTextbox nama;
	private Combobox jenisItemMedis;

	class ItemMedisRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final ItemMedis item = (ItemMedis) arg1;
			arg0.setAttribute("item", item);
			final Checkbox checkbox = new Checkbox();
			checkbox.setParent(arg0);
			for (ItemMedis myItemMedis : items) {
				if (myItemMedis != null && myItemMedis.getId() != null && myItemMedis.getId().equals(item.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(item.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(item.getId());
					} else {
						ids.remove(item.getId());
					}
				}
			});

			new Label(item.getKode()).setParent(arg0);
			new Label(item.getNama()).setParent(arg0);
			new Label(item.getKandungan()).setParent(arg0);
			new Label(item.getJenisItem() == null ? "" : item.getJenisItem().getNama()).setParent(arg0);
			new Label(item.getKelompokItem() == null ? "" : item.getKelompokItem().getNama()).setParent(arg0);
			new Label(item.getKelasItem() == null ? "" : item.getKelasItem().getNama()).setParent(arg0);
			new Label(item.getGenerikItem() == null ? "" : item.getGenerikItem().getNama()).setParent(arg0);
			new Label(item.getSatuanItem() == null ? "" : item.getSatuanItem().getNama()).setParent(arg0);

		}

	}

	public void display() {

		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar ItemMedis");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.Grid gridUtama = new org.zkoss.zul.Grid();
		gridUtama.setWidth("100%");
		ais.ui.util.ZkCompat.setFlex(gridUtama, true);
		gridUtama.setParent(center);
		Rows rowsUtama = new Rows();
		rowsUtama.setParent(gridUtama);

		Row rowUtama = new Row();
		rowUtama.setParent(rowsUtama);

		Grid searchgrid = new Grid();
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode ItemMedis")));
		row.appendChild(kodeItemMedisan = new MyTextbox());
		kodeItemMedisan.setWidth("90%");
		kodeItemMedisan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama ItemMedis")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis ItemMedis")));
		row.appendChild(jenisItemMedis = new Combobox());
		Common.insertCombo(jenisItemMedis, "nama", JenisItemMedis.class);
		if (myjenisItemMedis != null) {
			Common.selectComboItem(jenisItemMedis, myjenisItemMedis);
			// jenisItemMedis.setDisabled(true);
		}
		jenisItemMedis.setWidth("90%");
		jenisItemMedis.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Cari", "/img/search.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		grid = new Grid();
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("8%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kandungan");
		column.setWidth("8%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kelompok");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kelas");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Generik");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("10%");

		// onSearchDefault(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(south);

		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataItemMedisBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<ItemMedis> items = new ArrayList<ItemMedis>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						Checkbox checkbox = (Checkbox) row.getChildren().get(0);
						if (checkbox.isChecked() && !checkbox.isDisabled()) {
							ItemMedis myItemMedis = (ItemMedis) row.getAttribute("item");
							items.add(myItemMedis);
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), items);
					eventListener.onEvent(myEvent);
				}
				AmbilDataItemMedisBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Semua Data", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				JenisItemMedis jenisItemMedis = (JenisItemMedis) (AmbilDataItemMedisBanyak.this.jenisItemMedis
						.getSelectedItem() == null ? null
								: AmbilDataItemMedisBanyak.this.jenisItemMedis.getSelectedItem().getValue());
				List<ItemMedis> myItemMedis = HibernateUtil.currentSession().createCriteria(ItemMedis.class)
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.not(Restrictions.in("id", ids)))
						.addOrder(Order.asc("nama"))
						.add(jenisItemMedis == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisItemMedis", jenisItemMedis))
						.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
						.add(Restrictions.ilike("kode", kodeItemMedisan.getValue().trim(), MatchMode.ANYWHERE)).list();
				Event myEvent = new Event("myEvent", event.getTarget(), myItemMedis);
				eventListener.onEvent(myEvent);

				AmbilDataItemMedisBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		JenisItemMedis jenisItemMedis = (JenisItemMedis) (this.jenisItemMedis.getSelectedItem() == null ? null
				: this.jenisItemMedis.getSelectedItem().getValue());

		List<Long> values = new ArrayList<Long>();
		if (itemsHanyaDitampilkan != null) {
			for (ItemMedis item : itemsHanyaDitampilkan) {
				values.add(item.getId());
			}
		}

		List<ItemMedis> item = ConstantValues.simpleList(
				session.createCriteria(ItemMedis.class).addOrder(Order.asc("nama"))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				ItemMedis.class);

		List<ItemMedis> myItemMedis = ConstantValues
				.simpleList(session.createCriteria(ItemMedis.class).addOrder(Order.asc("nama"))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.not(Restrictions.in("id", ids)))
						.add(itemsHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.in("id", values))
						.add(jenisItemMedis == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisItemMedis", jenisItemMedis))
						.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
						.add(Restrictions.ilike("kode", kodeItemMedisan.getValue().trim(), MatchMode.ANYWHERE))
						.setMaxResults(Common.MAX_RESULT), ItemMedis.class);

		item.addAll(myItemMedis);

		ListModel strset = new SimpleListModel(item);
		grid.setRowRenderer(new ItemMedisRenderer());
		grid.setModel(strset);

		grid.renderAll();
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
