package ais.action.master.sirs.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
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
import ais.database.model.sirs.JenisAlatMedis;
import ais.database.model.sirs.AlatMedis;
import ais.ui.util.MyTextbox;

public class AmbilDataAlatMedisBanyak extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<AlatMedis> alatMediss;
	private List<AlatMedis> alatMedissHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	public Boolean getAlatMedisLab() {
		return alatMedisLab;
	}

	public void setAlatMedisLab(Boolean alatMedisLab) {
		this.alatMedisLab = alatMedisLab;
	}

	private Boolean alatMedisLab = null;
	private Boolean alatMedisOperasi = null;
	private Boolean alatMedisRadiologi = null;
	private Boolean alatMedisVk = null;
	private Boolean alatMedisRenalUnit = null;
	private Boolean alatMedisGizi = null;
	private String jenis = AlatMedis.JENIS_UMUM;

	public AmbilDataAlatMedisBanyak(List<AlatMedis> alatMediss, String jenis,
			Boolean alatMedisLab, Boolean alatMedisOperasi,
			Boolean alatMedisRadiologi, Boolean alatMedisVk,
			Boolean alatMedisRenalUnit, Boolean alatMedisGizi) {
		super();
		this.alatMediss = alatMediss;
		this.alatMedisLab = alatMedisLab;
		this.alatMedisOperasi = alatMedisOperasi;
		this.alatMedisRadiologi = alatMedisRadiologi;
		this.alatMedisVk = alatMedisVk;
		this.alatMedisRenalUnit = alatMedisRenalUnit;
		this.alatMedisGizi = alatMedisGizi;
		this.jenis = jenis;
		display();

		onSearchDefault(null);
	}

	public AmbilDataAlatMedisBanyak(List<AlatMedis> alatMediss, String jenis) {
		super();
		this.alatMediss = alatMediss;
		this.jenis = jenis;
		display();

		onSearchDefault(null);
	}

	public AmbilDataAlatMedisBanyak(List<AlatMedis> alatMediss, String jenis,
			List<AlatMedis> alatMedissHanyaDitampilkan) {
		super();
		this.alatMediss = alatMediss;
		this.alatMedissHanyaDitampilkan = alatMedissHanyaDitampilkan;
		this.jenis = jenis;
		display();

		onSearchDefault(null);
	}

	private MyTextbox kodeAlatMedisan;
	private MyTextbox nama;
	private Combobox jenisAlatMedis;

	private Checkbox alatMedisLabCheck = null;
	private Checkbox alatMedisOperasiCheck = null;
	private Checkbox alatMedisRadiologiCheck = null;
	private Checkbox alatMedisVkCheck = null;
	private Checkbox alatMedisRenalUnitCheck = null;
	private Checkbox alatMedisGiziCheck = null;

	class AlatMedisRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final AlatMedis alatMedis = (AlatMedis) arg1;
			arg0.setAttribute("alatMedis", alatMedis);
			final Checkbox checkbox = new Checkbox();
			checkbox.setParent(arg0);
			for (AlatMedis myAlatMedis : alatMediss) {
				if (myAlatMedis != null && myAlatMedis.getId() != null
						&& myAlatMedis.getId().equals(alatMedis.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(alatMedis.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(alatMedis.getId());
					} else {
						ids.remove(alatMedis.getId());
					}
				}
			});

			new Label(alatMedis.getKode()).setParent(arg0);
			new Label(alatMedis.getNama()).setParent(arg0);
			new Label(alatMedis.getJenisAlatMedis() == null ? "" : alatMedis
					.getJenisAlatMedis().getNama()).setParent(arg0);
			new Html(alatMedis.getKeteranganLayanan()).setParent(arg0);
		}

	}

	@SuppressWarnings("deprecation")
	public void display() {

		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar AlatMedis");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode AlatMedis")));
		row.appendChild(kodeAlatMedisan = new MyTextbox());
		kodeAlatMedisan.setWidth("90%");
		kodeAlatMedisan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama AlatMedis")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis AlatMedis")));
		row.appendChild(jenisAlatMedis = new Combobox());
		Common.insertCombo(jenisAlatMedis, "nama", JenisAlatMedis.class);
		jenisAlatMedis.setWidth("90%");
		jenisAlatMedis.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new Row();
		ais.ui.util.ZkCompat.setSpans(row, "6");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.setVisible(alatMedisLab == null && alatMedisOperasi == null
				&& alatMedisRadiologi == null && alatMedisVk == null
				&& alatMedisRenalUnit == null && alatMedisGizi == null);
		row.appendChild(new Hbox(new Component[] {
				alatMedisLabCheck = new Checkbox("Lab."),
				alatMedisOperasiCheck = new Checkbox("Operasi"),
				alatMedisRadiologiCheck = new Checkbox("Radiologi"),
				alatMedisVkCheck = new Checkbox("VK"),
				alatMedisRenalUnitCheck = new Checkbox("Renal Unit"),
				alatMedisGiziCheck = new Checkbox("Gizi") }));

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
		column.setLabel("Kode");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Layanan");

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
				AmbilDataAlatMedisBanyak.this.detach();
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
					List<AlatMedis> alatMediss = new ArrayList<AlatMedis>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						Checkbox checkbox = (Checkbox) row.getChildren().get(0);
						if (checkbox.isChecked() && !checkbox.isDisabled()) {
							AlatMedis myAlatMedis = (AlatMedis) row
									.getAttribute("alatMedis");
							alatMediss.add(myAlatMedis);
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(),
							alatMediss);
					eventListener.onEvent(myEvent);
				}
				AmbilDataAlatMedisBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Semua Data", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Session session = HibernateUtil.currentSession();

				Criterion criterion = Restrictions.sqlRestriction("1!=1");
				boolean adaOr = false;
				if (alatMedisLab != null) {
					criterion = Restrictions.or(criterion,
							Restrictions.eq("alatMedisLab", alatMedisLab));
					adaOr = true;
				}
				if (alatMedisGizi != null) {
					criterion = Restrictions.or(criterion,
							Restrictions.eq("alatMedisGizi", alatMedisGizi));
					adaOr = true;
				}
				if (alatMedisOperasi != null) {
					criterion = Restrictions.or(criterion, Restrictions.eq(
							"alatMedisOperasi", alatMedisOperasi));
					adaOr = true;
				}
				if (alatMedisRadiologi != null) {
					criterion = Restrictions.or(criterion, Restrictions.eq(
							"alatMedisRadiologi", alatMedisRadiologi));
					adaOr = true;
				}
				if (alatMedisRenalUnit != null) {
					criterion = Restrictions.or(criterion, Restrictions.eq(
							"alatMedisRenalUnit", alatMedisRenalUnit));
					adaOr = true;
				}
				if (alatMedisVk != null) {
					criterion = Restrictions.or(criterion,
							Restrictions.eq("alatMedisVk", alatMedisVk));
					adaOr = true;
				}

				if (alatMedisLabCheck.isChecked()) {
					criterion = Restrictions.or(criterion,
							Restrictions.eq("alatMedisLab", true));
					adaOr = true;
				}
				if (alatMedisGiziCheck.isChecked()) {
					criterion = Restrictions.or(criterion,
							Restrictions.eq("alatMedisGizi", true));
					adaOr = true;
				}
				if (alatMedisOperasiCheck.isChecked()) {
					criterion = Restrictions.or(criterion,
							Restrictions.eq("alatMedisOperasi", true));
					adaOr = true;
				}
				if (alatMedisRadiologiCheck.isChecked()) {
					criterion = Restrictions.or(criterion,
							Restrictions.eq("alatMedisRadiologi", true));
					adaOr = true;
				}
				if (alatMedisRenalUnitCheck.isChecked()) {
					criterion = Restrictions.or(criterion,
							Restrictions.eq("alatMedisRenalUnit", true));
					adaOr = true;
				}
				if (alatMedisVkCheck.isChecked()) {
					criterion = Restrictions.or(criterion,
							Restrictions.eq("alatMedisVk", true));
					adaOr = true;
				}

				JenisAlatMedis jenisAlatMedis = (JenisAlatMedis) (AmbilDataAlatMedisBanyak.this.jenisAlatMedis
						.getSelectedItem() == null ? null
						: AmbilDataAlatMedisBanyak.this.jenisAlatMedis
								.getSelectedItem().getValue());
				List<AlatMedis> myAlatMedis = session
						.createCriteria(AlatMedis.class)
						.add(ids.size() == 0 ? Restrictions
								.sqlRestriction("1=1") : Restrictions
								.not(Restrictions.in("id", ids)))
						.add(adaOr ? criterion : Restrictions
								.sqlRestriction("1=1"))
						.addOrder(Order.asc("nama"))
						.add(jenisAlatMedis == null ? Restrictions
								.sqlRestriction("1=1") : Restrictions.eq(
								"jenisAlatMedis", jenisAlatMedis))
						.add(Restrictions.ilike("nama", nama.getValue().trim(),
								MatchMode.ANYWHERE))
						.add(Restrictions.ilike("kode", kodeAlatMedisan
								.getValue().trim(), MatchMode.ANYWHERE)).list();
				Event myEvent = new Event("myEvent", event.getTarget(),
						myAlatMedis);
				eventListener.onEvent(myEvent);

				AmbilDataAlatMedisBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criterion criterion = Restrictions.sqlRestriction("1!=1");
		boolean adaOr = false;
		if (alatMedisLab != null) {
			criterion = Restrictions.or(criterion,
					Restrictions.eq("alatMedisLab", alatMedisLab));
			adaOr = true;
		}
		if (alatMedisGizi != null) {
			criterion = Restrictions.or(criterion,
					Restrictions.eq("alatMedisGizi", alatMedisGizi));
			adaOr = true;
		}
		if (alatMedisOperasi != null) {
			criterion = Restrictions.or(criterion,
					Restrictions.eq("alatMedisOperasi", alatMedisOperasi));
			adaOr = true;
		}
		if (alatMedisRadiologi != null) {
			criterion = Restrictions.or(criterion,
					Restrictions.eq("alatMedisRadiologi", alatMedisRadiologi));
			adaOr = true;
		}
		if (alatMedisRenalUnit != null) {
			criterion = Restrictions.or(criterion,
					Restrictions.eq("alatMedisRenalUnit", alatMedisRenalUnit));
			adaOr = true;
		}
		if (alatMedisVk != null) {
			criterion = Restrictions.or(criterion,
					Restrictions.eq("alatMedisVk", alatMedisVk));
			adaOr = true;
		}

		if (alatMedisLabCheck.isChecked()) {
			criterion = Restrictions.or(criterion,
					Restrictions.eq("alatMedisLab", true));
			adaOr = true;
		}
		if (alatMedisGiziCheck.isChecked()) {
			criterion = Restrictions.or(criterion,
					Restrictions.eq("alatMedisGizi", true));
			adaOr = true;
		}
		if (alatMedisOperasiCheck.isChecked()) {
			criterion = Restrictions.or(criterion,
					Restrictions.eq("alatMedisOperasi", true));
			adaOr = true;
		}
		if (alatMedisRadiologiCheck.isChecked()) {
			criterion = Restrictions.or(criterion,
					Restrictions.eq("alatMedisRadiologi", true));
			adaOr = true;
		}
		if (alatMedisRenalUnitCheck.isChecked()) {
			criterion = Restrictions.or(criterion,
					Restrictions.eq("alatMedisRenalUnit", true));
			adaOr = true;
		}
		if (alatMedisVkCheck.isChecked()) {
			criterion = Restrictions.or(criterion,
					Restrictions.eq("alatMedisVk", true));
			adaOr = true;
		}

		JenisAlatMedis jenisAlatMedis = (JenisAlatMedis) (this.jenisAlatMedis
				.getSelectedItem() == null ? null : this.jenisAlatMedis
				.getSelectedItem().getValue());

		List<Long> values = new ArrayList<Long>();
		if (alatMedissHanyaDitampilkan != null) {
			for (AlatMedis alatMedis : alatMedissHanyaDitampilkan) {
				values.add(alatMedis.getId());
			}
		}

		List<AlatMedis> alatMedis = session
				.createCriteria(AlatMedis.class)
				.add(adaOr ? criterion : Restrictions.sqlRestriction("1=1"))

				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1")
						: Restrictions.in("id", ids)).list();

		List<AlatMedis> myAlatMedis = session
				.createCriteria(AlatMedis.class)
				.add(adaOr ? criterion : Restrictions.sqlRestriction("1=1"))

				.add(Restrictions.eq("jenis", jenis))

				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(alatMedissHanyaDitampilkan == null || values.size() == 0 ? Restrictions
						.sqlRestriction("1=1") : Restrictions.in("id", values))
				.add(jenisAlatMedis == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq(
						"jenisAlatMedis", jenisAlatMedis))
				.add(Restrictions.ilike("nama", nama.getValue().trim(),
						MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodeAlatMedisan.getValue()
						.trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();

		alatMedis.addAll(myAlatMedis);

		ListModel strset = new SimpleListModel(alatMedis);
		grid.setRowRenderer(new AlatMedisRenderer());
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
