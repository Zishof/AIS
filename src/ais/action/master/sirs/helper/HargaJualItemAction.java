package ais.action.master.sirs.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.JenisItemMedis;
import ais.database.model.sirs.KelasPerawatan;
import ais.ui.util.MyTextbox;

public class HargaJualItemAction extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private List<KelasPerawatan> kelasPerawatans;
	private MyTextbox kodeIteman;
	private MyTextbox nama;
	private Combobox jenisItem;

	private Grid grid;

	@SuppressWarnings("unchecked")
	public HargaJualItemAction() {
		super();
		kelasPerawatans = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(KelasPerawatan.class).addOrder(Order.asc("id")),
				KelasPerawatan.class);
		display();
	}

	@SuppressWarnings("unchecked")
	public HargaJualItemAction(String title, String border, boolean closable) {
		super(title, border, closable);
		kelasPerawatans = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(KelasPerawatan.class).addOrder(Order.asc("id")),
				KelasPerawatan.class);
		display();
	}

	class ItemRenderer extends ais.ui.util.MyRowRenderer {

		public ItemRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final ItemMedis item = (ItemMedis) data;
			new Label(item.getKode()).setParent(row);
			new Label(item.getNama()).setParent(row);

			Session session = HibernateUtil.currentSession();
			for (final KelasPerawatan kelasPerawatan : kelasPerawatans) {
				HargaJualItem hargaJualItem = (HargaJualItem) session.createCriteria(HargaJualItem.class)
						.add(Restrictions.eq("item", item)).add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
						.setMaxResults(1).uniqueResult();

				new Label(Common.numberFormat.get().format(hargaJualItem == null || hargaJualItem.getHargaJual() == null ? 0.0
						: hargaJualItem.getHargaJual())).setParent(row);

			}

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		JenisItemMedis jenisItem = (JenisItemMedis) (this.jenisItem.getSelectedItem() == null ? null
				: this.jenisItem.getSelectedItem().getValue());
		Session session = HibernateUtil.currentSession();
		List<ItemMedis> items = ConstantValues.simpleList(session.createCriteria(ItemMedis.class)
				.addOrder(Order.asc("nama"))
				.add(jenisItem == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jenisItem", jenisItem))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodeIteman.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT_50), ItemMedis.class);

		ListModel strset = new SimpleListModel(items);
		grid.setRowRenderer(new ItemRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	private void display() {

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		Grid searchgrid = new Grid();
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Barang")));
		row.appendChild(kodeIteman = new MyTextbox());
		kodeIteman.setWidth("90%");
		kodeIteman.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Barang")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Barang")));
		row.appendChild(jenisItem = new Combobox());
		Common.insertCombo(jenisItem, "nama", JenisItemMedis.class);
		jenisItem.setWidth("90%");
		jenisItem.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(div);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Cari", "/img/search.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});
		button.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new Grid();
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Kode Item");
		column.setWidth("100px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Item");
		column.setWidth("200px");

		for (KelasPerawatan kelasPerawatan : kelasPerawatans) {
			column = new Column();
			column.setParent(columns);
			column.setLabel(kelasPerawatan.getNama());
		}

		loadData(null);
	}

}
