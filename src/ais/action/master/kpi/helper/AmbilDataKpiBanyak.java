package ais.action.master.kpi.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.kpi.Kpi;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataKpiBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<Kpi> kpis;
	private List<Kpi> kpisHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataKpiBanyak(List<Kpi> kpis) {
		super();
		this.kpis = kpis;
		display();
		onSearchDefault(null);
	}

	public AmbilDataKpiBanyak(List<Kpi> kpis, List<Kpi> kpisHanyaDitampilkan) {
		super();
		this.kpis = kpis;
		this.kpisHanyaDitampilkan = kpisHanyaDitampilkan;

		display();

		onSearchDefault(null);
	}

	private MyTextbox nama;
	private MyTextbox kode;
	private MyCheckboxConfig all;

	class KpiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Kpi kpi = (Kpi) arg1;
			arg0.setAttribute("kpi", kpi);

			final Checkbox checkbox = new Checkbox(kpi.getKode() + " - " + kpi.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (Kpi myKpi : kpis) {
				if (myKpi.getId().equals(kpi.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(kpi.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(kpi.getId());
					} else {
						ids.remove(kpi.getId());
					}
				}
			});
			new Label(kpi.getKategoriKpi() == null ? "" : kpi.getKategoriKpi().getNama()).setParent(arg0);

			new Label(kpi.getSatuanKpi() == null ? "" : kpi.getSatuanKpi().getNama()).setParent(arg0);

			new Html(KpiUtil.ambilDeskripsi(kpi.getFormula())).setParent(arg0);

			new Label(kpi.getKeterangan()).setParent(arg0);
		}

	}

	public void display() {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
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

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new MyTextbox());
		kode.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
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

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);

		all = new MyCheckboxConfig("KPI");
		all.setChecked(!ids.isEmpty());
		all.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (all.isChecked()) {
					Session session = HibernateUtil.currentSession();
					List<Long> myKpi = session.createCriteria(Kpi.class).setProjection(Projections.property("id"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
							.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
									: Restrictions.not(Restrictions.in("id", ids)))

							.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
									: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

							.add(kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
									: Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE))

							.list();
					ids.addAll(myKpi);
				} else {
					ids.clear();
				}

				onSearchDefault(arg0);

			}
		});

		column.appendChild(all);
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kategori");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Formula");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataKpiBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<Kpi> kpis = new ArrayList<Kpi>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {

						try {
							Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								Kpi myKpi = (Kpi) row.getAttribute("kpi");
								kpis.add(myKpi);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/AmbilDataKpiBanyak.java:290");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), kpis);
					eventListener.onEvent(myEvent);
				}
				AmbilDataKpiBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (kpisHanyaDitampilkan != null) {
			for (Kpi kpi : kpisHanyaDitampilkan) {
				values.add(kpi.getId());
			}
		}

		List<Long> valuesNot = new ArrayList<Long>();
		if (kpis != null) {
			for (Kpi kpi : kpis) {
				valuesNot.add(kpi.getId());
			}
		}

		List<Kpi> kpi = session.createCriteria(Kpi.class)

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))

				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids))

				.add(valuesNot.size() == 0 ? Restrictions.sqlRestriction("true")
						: Restrictions.not(Restrictions.in("id", valuesNot)))

				.list();

		List<Kpi> myKpi = session.createCriteria(Kpi.class)

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(kpisHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE))

				.add(valuesNot.size() == 0 ? Restrictions.sqlRestriction("true")
						: Restrictions.not(Restrictions.in("id", valuesNot)))

				.setMaxResults(Common.MAX_RESULT_1000).list();

		kpi.addAll(myKpi);

		ListModel strset = new SimpleListModel(kpi);
		grid.setRowRenderer(new KpiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
