package ais.action.master.rab.helper;

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

import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Sasaran;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyTextbox;

public class AmbilDataSasaranBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<Sasaran> sasarans;
	private List<Sasaran> sasaransHanyaDitampilkan;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataSasaranBanyak(List<Sasaran> sasarans) {
		super();
		this.sasarans = sasarans;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		try {
			display();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
		onSearchDefault(null);
	}

	public AmbilDataSasaranBanyak(List<Sasaran> sasarans,
			List<Sasaran> sasaransHanyaDitampilkan) {
		super();
		this.sasarans = sasarans;
		this.sasaransHanyaDitampilkan = sasaransHanyaDitampilkan;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		try {
			display();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}

		onSearchDefault(null);
	}

	private MyTextbox kodeSasaranan;
	private MyTextbox nama;
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	class SasaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Sasaran sasaran = (Sasaran) arg1;
			arg0.setAttribute("sasaran", sasaran);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			for (Sasaran mySasaran : sasarans) {
				if (mySasaran.getId().equals(sasaran.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(sasaran.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(sasaran.getId());
					} else {
						ids.remove(sasaran.getId());
					}
				}
			});

			new Label(sasaran.getKode()).setParent(arg0);
			new Label(sasaran.getNama()).setParent(arg0);

		}

	}

	public void display() throws Exception {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Sasaran");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Sasaran"));
		row.appendChild(kodeSasaranan = new MyTextbox());
		kodeSasaranan.setWidth("90%");
		kodeSasaranan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Sasaran"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(this.satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setWidth("90%");
		satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
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
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Isi Sasaran");

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
				AmbilDataSasaranBanyak.this.detach();
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
					List<Sasaran> sasarans = new ArrayList<Sasaran>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						if (checkbox.isChecked() && !checkbox.isDisabled()) {
							Sasaran mySasaran = (Sasaran) row
									.getAttribute("sasaran");
							sasarans.add(mySasaran);
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(),
							sasarans);
					eventListener.onEvent(myEvent);
				}
				AmbilDataSasaranBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (sasaransHanyaDitampilkan != null) {
			for (Sasaran sasaran : sasaransHanyaDitampilkan) {
				values.add(sasaran.getId());
			}
		}

		SatuanKerja parent = (SatuanKerja) satuanKerja
				.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		List<Sasaran> sasaran = session
				.createCriteria(Sasaran.class)
				.add(satuanKerjas.size() == 0 ? Restrictions
						.sqlRestriction("1=1") : Restrictions.in("satuanKerja",
						satuanKerjas))
				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1")
						: Restrictions.in("id", ids)).list();

		List<Sasaran> mySasaran = session
				.createCriteria(Sasaran.class)
				.add(satuanKerjas.size() == 0 ? Restrictions
						.sqlRestriction("1=1") : Restrictions.in("satuanKerja",
						satuanKerjas))
				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(sasaransHanyaDitampilkan == null || values.size() == 0 ? Restrictions
						.sqlRestriction("1=1") : Restrictions.in("id", values))
				.add(Restrictions.ilike("nama", nama.getValue().trim(),
						MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode",
						kodeSasaranan.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();

		sasaran.addAll(mySasaran);

		ListModel strset = new SimpleListModel(sasaran);
		grid.setRowRenderer(new SasaranRenderer());
		grid.setModelCheckMobile(strset);
		
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
