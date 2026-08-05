package ais.action.master.employ.helper;

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
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.employ.JabatanFungsional;
import ais.ui.util.MyTextbox;

public class AmbilDataJabatanFungsionalBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<JabatanFungsional> jabatanFungsionals;
	private List<JabatanFungsional> jabatanFungsionalsHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataJabatanFungsionalBanyak(List<JabatanFungsional> jabatanFungsionals) {
		super();
		this.jabatanFungsionals = jabatanFungsionals;
		display();
		onSearchDefault(null);
	}

	public AmbilDataJabatanFungsionalBanyak(List<JabatanFungsional> jabatanFungsionals,
			List<JabatanFungsional> jabatanFungsionalsHanyaDitampilkan) {
		super();
		this.jabatanFungsionals = jabatanFungsionals;
		this.jabatanFungsionalsHanyaDitampilkan = jabatanFungsionalsHanyaDitampilkan;
		display();

		onSearchDefault(null);

	}

	private MyTextbox nama;

	class JabatanFungsionalRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final JabatanFungsional jabatanFungsional = (JabatanFungsional) arg1;
			arg0.setAttribute("jabatanFungsional", jabatanFungsional);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			for (JabatanFungsional myJabatanFungsional : jabatanFungsionals) {
				if (myJabatanFungsional.getId().equals(jabatanFungsional.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(jabatanFungsional.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(jabatanFungsional.getId());
					} else {
						ids.remove(jabatanFungsional.getId());
					}
				}
			});

			new Label(jabatanFungsional.getNama()).setParent(arg0);
			new Label(jabatanFungsional.getKeterangan()).setParent(arg0);

		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Jabatan Fungsional");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
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

		MyGrid searchgrid = new MyGrid();searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
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

		grid = new MyGrid();//grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* setPageSize legacy dihapus: grid bukan mold "paging" sehingga setPageSize melempar IllegalStateException ("Available only the paging mold") dan daftar tidak pernah tampil. Paging ditangani AmbilDataPagingHelper. */
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
		column.setLabel("Nama");

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
				AmbilDataJabatanFungsionalBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null
						&& grid.getRows().getChildren() != null) {
					List<JabatanFungsional> jabatanFungsionals = new ArrayList<JabatanFungsional>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						if (checkbox.isChecked() && !checkbox.isDisabled()) {
							JabatanFungsional myJabatanFungsional = (JabatanFungsional) row
									.getAttribute("jabatanFungsional");
							jabatanFungsionals.add(myJabatanFungsional);
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(),
							jabatanFungsionals);
					eventListener.onEvent(myEvent);
				}
				AmbilDataJabatanFungsionalBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (jabatanFungsionalsHanyaDitampilkan != null) {
			for (JabatanFungsional jabatanFungsional : jabatanFungsionalsHanyaDitampilkan) {
				values.add(jabatanFungsional.getId());
			}
		}

		List<JabatanFungsional> jabatanFungsional = session
				.createCriteria(JabatanFungsional.class)
				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1")
						: Restrictions.in("id", ids)).list();

		List<JabatanFungsional> myJabatanFungsional = session
				.createCriteria(JabatanFungsional.class)
				.add(Restrictions.eq("defaultItem", true))
				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(jabatanFungsionalsHanyaDitampilkan == null
						|| values.size() == 0 ? Restrictions
						.sqlRestriction("1=1") : Restrictions.in("id", values))
				.add(Restrictions.ilike("nama", nama.getValue().trim(),
						MatchMode.ANYWHERE)).setMaxResults(Common.MAX_RESULT)
				.list();

		jabatanFungsional.addAll(myJabatanFungsional);

		ListModel strset = new SimpleListModel(jabatanFungsional);
		grid.setRowRenderer(new JabatanFungsionalRenderer());
		grid.setModelCheckMobile(strset);

		
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
