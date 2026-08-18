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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.JenisBiaya;
import ais.ui.util.MyTextbox;

public class AmbilDataJenisBiayaBanyak extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<JenisBiaya> jenisBiayas;
	private List<JenisBiaya> jenisBiayasHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();
	private String tipe;

	public AmbilDataJenisBiayaBanyak(List<JenisBiaya> jenisBiayas, String tipe) {
		super();
		this.jenisBiayas = jenisBiayas;
		this.tipe = tipe;
		display();

		onSearchDefault(null);
	}

	public AmbilDataJenisBiayaBanyak(List<JenisBiaya> jenisBiayas,
			List<JenisBiaya> jenisBiayasHanyaDitampilkan, String tipe) {
		super();
		this.jenisBiayas = jenisBiayas;
		this.tipe = tipe;
		this.jenisBiayasHanyaDitampilkan = jenisBiayasHanyaDitampilkan;
		display();

		onSearchDefault(null);
	}

	private MyTextbox nama;

	class JenisBiayaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final JenisBiaya jenisBiaya = (JenisBiaya) arg1;
			arg0.setAttribute("jenisBiaya", jenisBiaya);
			final Checkbox checkbox = new Checkbox();
			checkbox.setParent(arg0);
			for (JenisBiaya myJenisBiaya : jenisBiayas) {
				if (myJenisBiaya != null && myJenisBiaya.getId() != null
						&& myJenisBiaya.getId().equals(jenisBiaya.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(jenisBiaya.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(jenisBiaya.getId());
					} else {
						ids.remove(jenisBiaya.getId());
					}
				}
			});

			new Label(jenisBiaya.getNama()).setParent(arg0);
			new Label(jenisBiaya.getAkun().toString()).setParent(arg0);
			new Label(jenisBiaya.getKeterangan()).setParent(arg0);
		}

	}

	public void display() {

		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Jenis Biaya");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Jenis Biaya")));
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
		column.setLabel("Nama");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Akun");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

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
				AmbilDataJenisBiayaBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null
						&& grid.getRows().getChildren() != null) {
					List<JenisBiaya> jenisBiayas = new ArrayList<JenisBiaya>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						Checkbox checkbox = (Checkbox) row.getChildren().get(0);
						if (checkbox.isChecked() && !checkbox.isDisabled()) {
							JenisBiaya myJenisBiaya = (JenisBiaya) row
									.getAttribute("jenisBiaya");
							jenisBiayas.add(myJenisBiaya);
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(),
							jenisBiayas);
					eventListener.onEvent(myEvent);
				}
				AmbilDataJenisBiayaBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Semua Data", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Session session = HibernateUtil.currentSession();

				List<JenisBiaya> myJenisBiaya = session
						.createCriteria(JenisBiaya.class)
						.add(ids.size() == 0 ? Restrictions
								.sqlRestriction("1=1") : Restrictions
								.not(Restrictions.in("id", ids)))
						.add(Restrictions.eq("tipe", tipe))
						.addOrder(Order.asc("nama"))
						.add(Restrictions.ilike("nama", nama.getValue().trim(),
								MatchMode.ANYWHERE)).list();
				Event myEvent = new Event("myEvent", event.getTarget(),
						myJenisBiaya);
				eventListener.onEvent(myEvent);

				AmbilDataJenisBiayaBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (jenisBiayasHanyaDitampilkan != null) {
			for (JenisBiaya jenisBiaya : jenisBiayasHanyaDitampilkan) {
				values.add(jenisBiaya.getId());
			}
		}

		List<JenisBiaya> jenisBiaya = session
				.createCriteria(JenisBiaya.class)
				.add(Restrictions.eq("tipe", tipe))

				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1")
						: Restrictions.in("id", ids)).list();

		List<JenisBiaya> myJenisBiaya = session
				.createCriteria(JenisBiaya.class)
				.add(Restrictions.eq("tipe", tipe))

				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(jenisBiayasHanyaDitampilkan == null || values.size() == 0 ? Restrictions
						.sqlRestriction("1=1") : Restrictions.in("id", values))

				.add(Restrictions.ilike("nama", nama.getValue().trim(),
						MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT).list();

		jenisBiaya.addAll(myJenisBiaya);

		ListModel strset = new SimpleListModel(jenisBiaya);
		grid.setRowRenderer(new JenisBiayaRenderer());
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
