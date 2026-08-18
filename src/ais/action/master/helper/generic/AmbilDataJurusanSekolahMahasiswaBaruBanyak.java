package ais.action.master.helper.generic;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
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
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataJurusanSekolahMahasiswaBaruBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<JurusanSekolahMahasiswaBaru> jurusanSekolahMahasiswaBarus;
	private List<JurusanSekolahMahasiswaBaru> jurusanSekolahMahasiswaBarusHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataJurusanSekolahMahasiswaBaruBanyak(List<JurusanSekolahMahasiswaBaru> jurusanSekolahMahasiswaBarus) {
		super();
		this.jurusanSekolahMahasiswaBarus = jurusanSekolahMahasiswaBarus;
		display();
		onSearchDefault(null);
	}

	public AmbilDataJurusanSekolahMahasiswaBaruBanyak(List<JurusanSekolahMahasiswaBaru> jurusanSekolahMahasiswaBarus,
			List<JurusanSekolahMahasiswaBaru> jurusanSekolahMahasiswaBarusHanyaDitampilkan) {
		super();
		this.jurusanSekolahMahasiswaBarus = jurusanSekolahMahasiswaBarus;
		this.jurusanSekolahMahasiswaBarusHanyaDitampilkan = jurusanSekolahMahasiswaBarusHanyaDitampilkan;

		display();

		onSearchDefault(null);
	}

	private MyTextbox nama;

	class JurusanSekolahMahasiswaBaruRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru = (JurusanSekolahMahasiswaBaru) arg1;
			arg0.setAttribute("jurusanSekolahMahasiswaBaru", jurusanSekolahMahasiswaBaru);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (JurusanSekolahMahasiswaBaru myJurusanSekolahMahasiswaBaru : jurusanSekolahMahasiswaBarus) {
				if (myJurusanSekolahMahasiswaBaru.getId().equals(jurusanSekolahMahasiswaBaru.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(jurusanSekolahMahasiswaBaru.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(jurusanSekolahMahasiswaBaru.getId());
					} else {
						ids.remove(jurusanSekolahMahasiswaBaru.getId());
					}
				}
			});

			new Label(jurusanSekolahMahasiswaBaru.getJenisSekolahMahasiswaBaru().getNama()).setParent(arg0);
			new Label(jurusanSekolahMahasiswaBaru.getNama()).setParent(arg0);
			new Label(jurusanSekolahMahasiswaBaru.getKeterangan()).setParent(arg0);
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
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama ");

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
				AmbilDataJurusanSekolahMahasiswaBaruBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<JurusanSekolahMahasiswaBaru> jurusanSekolahMahasiswaBarus = new ArrayList<JurusanSekolahMahasiswaBaru>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								JurusanSekolahMahasiswaBaru myJurusanSekolahMahasiswaBaru = (JurusanSekolahMahasiswaBaru) row
										.getAttribute("jurusanSekolahMahasiswaBaru");
								jurusanSekolahMahasiswaBarus.add(myJurusanSekolahMahasiswaBaru);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataJurusanSekolahMahasiswaBaruBanyak.java:230");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), jurusanSekolahMahasiswaBarus);
					eventListener.onEvent(myEvent);
				}
				AmbilDataJurusanSekolahMahasiswaBaruBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (jurusanSekolahMahasiswaBarusHanyaDitampilkan != null) {
			for (JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru : jurusanSekolahMahasiswaBarusHanyaDitampilkan) {
				values.add(jurusanSekolahMahasiswaBaru.getId());
			}
		}

		List<JurusanSekolahMahasiswaBaru> jurusanSekolahMahasiswaBaru = session
				.createCriteria(JurusanSekolahMahasiswaBaru.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)).list();

		List<JurusanSekolahMahasiswaBaru> myJurusanSekolahMahasiswaBaru = session
				.createCriteria(JurusanSekolahMahasiswaBaru.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(jurusanSekolahMahasiswaBarusHanyaDitampilkan == null || values.size() == 0
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();

		jurusanSekolahMahasiswaBaru.addAll(myJurusanSekolahMahasiswaBaru);

		ListModel strset = new SimpleListModel(jurusanSekolahMahasiswaBaru);
		grid.setRowRenderer(new JurusanSekolahMahasiswaBaruRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
