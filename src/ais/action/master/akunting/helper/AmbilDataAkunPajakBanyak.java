package ais.action.master.akunting.helper;

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
import ais.database.model.akunting.AkunPajak;
import ais.database.model.akunting.Transaksi;
import ais.ui.util.MyTextbox;

public class AmbilDataAkunPajakBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<AkunPajak> akunPajaks;
	private List<AkunPajak> akunPajaksHanyaDitampilkan;
	private Double nilai = 0.0;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataAkunPajakBanyak(List<AkunPajak> akunPajaks,
			List<Transaksi> transaksis) {
		super();
		this.akunPajaks = akunPajaks;
		nilai = 0.0;
		for (Transaksi transaksi : transaksis) {
			nilai += transaksi.getDebet() == null ? 0.0 : transaksi.getDebet();
		}

		display();
		onSearchDefault(null);
	}

	public AmbilDataAkunPajakBanyak(List<AkunPajak> akunPajaks,
			List<AkunPajak> akunPajaksHanyaDitampilkan,
			List<Transaksi> transaksis) {
		super();
		this.akunPajaks = akunPajaks;
		this.akunPajaksHanyaDitampilkan = akunPajaksHanyaDitampilkan;
		nilai = 0.0;
		for (Transaksi transaksi : transaksis) {
			nilai += transaksi.getDebet() == null ? 0.0 : transaksi.getDebet();
		}

		display();

		onSearchDefault(null);

	}

	public AmbilDataAkunPajakBanyak(List<AkunPajak> akunPajaks, Double nilai) {
		super();
		this.akunPajaks = akunPajaks;
		this.nilai = nilai;

		display();
		onSearchDefault(null);
	}

	public AmbilDataAkunPajakBanyak(List<AkunPajak> akunPajaks,
			List<AkunPajak> akunPajaksHanyaDitampilkan, Double nilai) {
		super();
		this.akunPajaks = akunPajaks;
		this.akunPajaksHanyaDitampilkan = akunPajaksHanyaDitampilkan;
		this.nilai = nilai;

		display();

		onSearchDefault(null);

	}

	private MyTextbox nama;

	class AkunPajakRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final AkunPajak akunPajak = (AkunPajak) arg1;
			arg0.setAttribute("akunPajak", akunPajak);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			for (AkunPajak myAkunPajak : akunPajaks) {
				if (myAkunPajak.getId().equals(akunPajak.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(akunPajak.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(akunPajak.getId());
					} else {
						ids.remove(akunPajak.getId());
					}
				}
			});

			new Label(akunPajak.getAkun() == null ? "" : akunPajak.getAkun()
					.toString()).setParent(arg0);
			new Label(akunPajak.getNama()).setParent(arg0);
			new Label(akunPajak.getPersen() == null ? "0 %"
					: Common.numberFormat.get().format(akunPajak.getPersen()) + " %")
					.setParent(arg0);

			new Label(akunPajak.getPersen() == null
					|| akunPajak.getPersen() < 0.000001 ? "0"
					: Common.numberFormat.get().format(nilai
							* (akunPajak.getPersen() / 100.0))).setParent(arg0);
		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Akun Pajak");
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
		column.setLabel("Akun");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persentase");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah Pajak");
		column.setWidth("10%");
		column.setAlign("right");

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
				AmbilDataAkunPajakBanyak.this.detach();
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
					List<AkunPajak> akunPajaks = new ArrayList<AkunPajak>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						if (checkbox.isChecked() && !checkbox.isDisabled()) {
							AkunPajak myAkunPajak = (AkunPajak) row
									.getAttribute("akunPajak");
							akunPajaks.add(myAkunPajak);
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(),
							akunPajaks);
					eventListener.onEvent(myEvent);
				}
				AmbilDataAkunPajakBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (akunPajaksHanyaDitampilkan != null) {
			for (AkunPajak akunPajak : akunPajaksHanyaDitampilkan) {
				values.add(akunPajak.getId());
			}
		}

		List<AkunPajak> akunPajak = session
				.createCriteria(AkunPajak.class)
				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1")
						: Restrictions.in("id", ids)).list();

		List<AkunPajak> myAkunPajak = session
				.createCriteria(AkunPajak.class)
				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(akunPajaksHanyaDitampilkan == null || values.size() == 0 ? Restrictions
						.sqlRestriction("1=1") : Restrictions.in("id", values))
				.add(Restrictions.ilike("nama", nama.getValue().trim(),
						MatchMode.ANYWHERE)).setMaxResults(Common.MAX_RESULT)
				.list();

		akunPajak.addAll(myAkunPajak);

		ListModel strset = new SimpleListModel(akunPajak);
		grid.setRowRenderer(new AkunPajakRenderer());
		grid.setModelCheckMobile(strset);

		
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
