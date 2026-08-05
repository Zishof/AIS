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
import ais.database.model.sirs.Dokter;
import ais.ui.util.MyTextbox;

public class AmbilDataDokterBanyak extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;
	private EventListener eventListener;
	private List<Dokter> dokters;
	private List<Dokter> doktersHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataDokterBanyak(List<Dokter> dokters) {
		super();
		this.dokters = dokters;

		display();

		onSearchDefault(null);
	}

	public AmbilDataDokterBanyak(List<Dokter> dokters, List<Dokter> doktersHanyaDitampilkan) {
		super();
		this.dokters = dokters;
		this.doktersHanyaDitampilkan = doktersHanyaDitampilkan;

		display();

		onSearchDefault(null);
	}

	private MyTextbox kodeDokteran;
	private MyTextbox nama;

	class DokterRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Dokter dokter = (Dokter) arg1;
			arg0.setAttribute("dokter", dokter);
			final Checkbox checkbox = new Checkbox();
			checkbox.setParent(arg0);
			for (Dokter myDokter : dokters) {
				if (myDokter.getId().equals(dokter.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(dokter.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(dokter.getId());
					} else {
						ids.remove(dokter.getId());
					}
				}
			});

			new Label(dokter.getKode()).setParent(arg0);
			new Label(dokter.getNama()).setParent(arg0);

		}

	}

	public void display() {

		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Dokter");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Dokter")));
		row.appendChild(kodeDokteran = new MyTextbox());
		kodeDokteran.setWidth("90%");
		kodeDokteran.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Dokter")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
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
		column.setLabel("Kode");
		column.setWidth("25%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");

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
				AmbilDataDokterBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<Dokter> dokters = new ArrayList<Dokter>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						Checkbox checkbox = (Checkbox) row.getChildren().get(0);
						if (checkbox.isChecked() && !checkbox.isDisabled()) {
							Dokter myDokter = (Dokter) row.getAttribute("dokter");
							dokters.add(myDokter);
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), dokters);
					eventListener.onEvent(myEvent);
				}
				AmbilDataDokterBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (doktersHanyaDitampilkan != null) {
			for (Dokter dokter : doktersHanyaDitampilkan) {
				values.add(dokter.getId());
			}
		}

		List<Dokter> dokter = ConstantValues.simpleList(
				session.createCriteria(Dokter.class).addOrder(Order.asc("nama"))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				Dokter.class);

		List<Dokter> myDokter = ConstantValues
				.simpleList(session.createCriteria(Dokter.class).addOrder(Order.asc("nama"))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.not(Restrictions.in("id", ids)))
						.add(doktersHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.in("id", values))
						.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
						.add(Restrictions.ilike("kode", kodeDokteran.getValue().trim(), MatchMode.ANYWHERE))
						.setMaxResults(Common.MAX_RESULT), Dokter.class);

		dokter.addAll(myDokter);

		ListModel strset = new SimpleListModel(dokter);
		grid.setRowRenderer(new DokterRenderer());
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
