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
import ais.ui.util.MyTextbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.JenisRacikan;
import ais.database.model.sirs.Racikan;

public class AmbilDataRacikanBanyak extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;
	private EventListener eventListener;
	private List<Racikan> racikans;
	private List<Racikan> racikansHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataRacikanBanyak(List<Racikan> racikans) {
		super();
		this.racikans = racikans;
		display();

		onSearchDefault(null);
	}

	public AmbilDataRacikanBanyak(List<Racikan> racikans, List<Racikan> racikansHanyaDitampilkan) {
		super();
		this.racikans = racikans;
		this.racikansHanyaDitampilkan = racikansHanyaDitampilkan;
		display();

		onSearchDefault(null);
	}

	private MyTextbox kodeRacikanan;
	private MyTextbox nama;
	private Combobox jenisRacikan;

	class RacikanRenderer extends ais.ui.util.MyRowRenderer {

		private Session session = HibernateUtil.currentSession();

		@SuppressWarnings("unchecked")
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Racikan racikan = (Racikan) arg1;
			arg0.setAttribute("racikan", racikan);
			final Checkbox checkbox = new Checkbox();
			checkbox.setParent(arg0);
			for (Racikan myRacikan : racikans) {
				if (myRacikan != null && myRacikan.getId() != null && myRacikan.getId().equals(racikan.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(racikan.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(racikan.getId());
					} else {
						ids.remove(racikan.getId());
					}
				}
			});

			new Label(racikan.getKode()).setParent(arg0);
			new Label(racikan.getNama()).setParent(arg0);
			new Label(racikan.getJenisRacikan() == null ? "" : racikan.getJenisRacikan().getNama()).setParent(arg0);

			List<String> strings = session.createSQLQuery(
					"select ('[' || b.nama || ', ' || a.jumlah || ']') as nama  from sirs.racikan_detail a left join sirs.item_medis b on (a.item = b.id)")
					.list();

			String keterangan = "";
			for (String s : strings) {
				keterangan += s;
			}

			new Label(keterangan).setParent(arg0);

		}

	}

	public void display() {

		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Racikan");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Racikan")));
		row.appendChild(kodeRacikanan = new MyTextbox());
		kodeRacikanan.setWidth("90%");
		kodeRacikanan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Racikan")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Racikan")));
		row.appendChild(jenisRacikan = new Combobox());
		Common.insertCombo(jenisRacikan, "nama", JenisRacikan.class);
		jenisRacikan.setWidth("90%");
		jenisRacikan.addEventListener(Events.ON_CHANGE, new EventListener() {
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
		grid.setMold("paging");
		grid.setPageSize(25);
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
		column.setLabel("Kode Racikan");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Racikan");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Item-Item");

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
				AmbilDataRacikanBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<Racikan> racikans = new ArrayList<Racikan>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						Checkbox checkbox = (Checkbox) row.getChildren().get(0);
						if (checkbox.isChecked() && !checkbox.isDisabled()) {
							Racikan myRacikan = (Racikan) row.getAttribute("racikan");
							racikans.add(myRacikan);
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), racikans);
					eventListener.onEvent(myEvent);
				}
				AmbilDataRacikanBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		JenisRacikan jenisRacikan = (JenisRacikan) (this.jenisRacikan.getSelectedItem() == null ? null
				: this.jenisRacikan.getSelectedItem().getValue());

		List<Long> values = new ArrayList<Long>();
		if (racikansHanyaDitampilkan != null) {
			for (Racikan racikan : racikansHanyaDitampilkan) {
				values.add(racikan.getId());
			}
		}

		List<Racikan> racikan = ConstantValues.simpleList(
				session.createCriteria(Racikan.class).addOrder(Order.asc("nama"))
						.add(Restrictions.isNull("variasiDari"))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				Racikan.class);

		List<Racikan> myRacikan = ConstantValues.simpleList(session.createCriteria(Racikan.class)
				.add(Restrictions.isNull("variasiDari")).addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(racikansHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))
				.add(jenisRacikan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisRacikan", jenisRacikan))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodeRacikanan.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT), Racikan.class);

		racikan.addAll(myRacikan);

		ListModel strset = new SimpleListModel(racikan);
		grid.setRowRenderer(new RacikanRenderer());
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
