package ais.action.master.surat.helper;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
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

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataSuratMasukBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<SuratMasuk> suratMasuks;
	private List<SuratMasuk> suratMasuksHanyaDitampilkan;

	private MyTextbox nama;

	private Set<Long> ids = new HashSet<Long>();

	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private String tipe;
	private Boolean hanyaYangBolehDipinjam = false;
	private SatuanKerja satker = null;

	public AmbilDataSuratMasukBanyak(List<SuratMasuk> suratMasuks, String tipe) {
		this(suratMasuks, tipe, false);
	}

	public AmbilDataSuratMasukBanyak(List<SuratMasuk> suratMasuks, String tipe, Boolean hanyaYangBolehDipinjam) {
		super();
		this.suratMasuks = suratMasuks;
		this.tipe = tipe;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		this.hanyaYangBolehDipinjam = hanyaYangBolehDipinjam;
		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/AmbilDataSuratMasukBanyak.java:84");
		}
		onSearchDefault(null);
	}

	public AmbilDataSuratMasukBanyak(List<SuratMasuk> suratMasuks, String tipe, Boolean hanyaYangBolehDipinjam,
			SatuanKerja satker) {
		super();
		this.suratMasuks = suratMasuks;
		this.tipe = tipe;
		this.satker = satker;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		this.hanyaYangBolehDipinjam = hanyaYangBolehDipinjam;
		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/AmbilDataSuratMasukBanyak.java:101");
		}
		onSearchDefault(null);
	}

	public AmbilDataSuratMasukBanyak(List<SuratMasuk> suratMasuks, List<SuratMasuk> suratMasuksHanyaDitampilkan,
			String tipe) {
		super();
		this.suratMasuks = suratMasuks;
		this.suratMasuksHanyaDitampilkan = suratMasuksHanyaDitampilkan;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		this.tipe = tipe;
		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/AmbilDataSuratMasukBanyak.java:117");
		}

		onSearchDefault(null);
	}

	class SuratMasukRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final SuratMasuk suratMasuk = (SuratMasuk) arg1;
			arg0.setAttribute("suratMasuk", suratMasuk);

			final Checkbox checkbox = new Checkbox(suratMasuk.getNoSurat());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (SuratMasuk mySuratMasuk : suratMasuks) {
				if (mySuratMasuk.getId().equals(suratMasuk.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(suratMasuk.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(suratMasuk.getId());
					} else {
						ids.remove(suratMasuk.getId());
					}
				}
			});
			new Label(suratMasuk.getKode()).setParent(arg0);
			new Label(suratMasuk.getPerihal()).setParent(arg0);
			new Label(suratMasuk.getNama()).setParent(arg0);
		}

	}

	public void display() throws Exception {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Surat Masuk");
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

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor/Agenda/Judul"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true, false));
		satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (satker != null) {
			satuanKerja.setAttribute("satuanKerja", satker);
			satuanKerja.setAttribute("myValue", satker);
			satuanKerja.setValue(satker.getNama());
			satuanKerja.setDisabled(true);
		}

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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
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
		column.setLabel("Nomor Surat");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nomor Agenda");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Perihal");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Surat Masuk");

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
				AmbilDataSuratMasukBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<SuratMasuk> suratMasuks = new ArrayList<SuratMasuk>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								SuratMasuk mySuratMasuk = (SuratMasuk) row.getAttribute("suratMasuk");
								suratMasuks.add(mySuratMasuk);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/AmbilDataSuratMasukBanyak.java:310");
//							e.printStackTrace();
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), suratMasuks);
					eventListener.onEvent(myEvent);
				}
				AmbilDataSuratMasukBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		SatuanKerja parent = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (suratMasuksHanyaDitampilkan != null) {
			for (SuratMasuk suratMasuk : suratMasuksHanyaDitampilkan) {
				values.add(suratMasuk.getId());
			}
		}

		List<SuratMasuk> suratMasuk = session.createCriteria(SuratMasuk.class)

				.createAlias("klasifikasiSuratMasuk", "klasifikasiSuratMasuk")

				.add(hanyaYangBolehDipinjam
						? Restrictions.or(Restrictions.isNull("klasifikasiSuratMasuk.bolehDipinjam"),
								Restrictions.eq("klasifikasiSuratMasuk.bolehDipinjam", true))
						: Restrictions.sqlRestriction("true"))

				.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))
				.addOrder(Order.asc("nama"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)).list();

		List<Long> notIn = new ArrayList<Long>();
		if (suratMasuks != null) {
			for (SuratMasuk u : suratMasuks) {
				notIn.add(u.getId());
			}
		}

		List<SuratMasuk> mySuratMasuk = session.createCriteria(SuratMasuk.class)

				.createAlias("klasifikasiSuratMasuk", "klasifikasiSuratMasuk")

				.add(hanyaYangBolehDipinjam
						? Restrictions.or(Restrictions.isNull("klasifikasiSuratMasuk.bolehDipinjam"),
								Restrictions.eq("klasifikasiSuratMasuk.bolehDipinjam", true))
						: Restrictions.sqlRestriction("true"))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)))
				.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(notIn.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", notIn)))

				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(suratMasuksHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("noSurat", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("kode", nama.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE)))

				)

				.setMaxResults(Common.MAX_RESULT).list();

		suratMasuk.addAll(mySuratMasuk);

		ListModel strset = new SimpleListModel(suratMasuk);
		grid.setRowRenderer(new SuratMasukRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
