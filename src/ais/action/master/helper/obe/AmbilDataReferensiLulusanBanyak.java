package ais.action.master.helper.obe;

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
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.obe.ReferensiLulusan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataReferensiLulusanBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<ReferensiLulusan> referensiLulusans;
	private List<ReferensiLulusan> referensiLulusansHanyaDitampilkan;

	private MyTextbox nama;

	private Set<Long> ids = new HashSet<Long>();
	private MyTextbox keterangan;

	public AmbilDataReferensiLulusanBanyak(List<ReferensiLulusan> referensiLulusans) { 
		super();
		this.referensiLulusans = referensiLulusans;
		display();
		onSearchDefault(null);
	}

	public AmbilDataReferensiLulusanBanyak(List<ReferensiLulusan> referensiLulusans,
			List<ReferensiLulusan> referensiLulusansHanyaDitampilkan) {
		super();
		this.referensiLulusans = referensiLulusans;
		this.referensiLulusansHanyaDitampilkan = referensiLulusansHanyaDitampilkan;

		display();

		onSearchDefault(null);
	}

	class ReferensiLulusanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ReferensiLulusan referensiLulusan = (ReferensiLulusan) arg1;
			arg0.setAttribute("referensiLulusan", referensiLulusan);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (ReferensiLulusan myReferensiLulusan : referensiLulusans) {
				if (myReferensiLulusan.getId().equals(referensiLulusan.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(referensiLulusan.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(referensiLulusan.getId());
					} else {
						ids.remove(referensiLulusan.getId());
					}
				}
			});
			new Label(referensiLulusan.getKode()).setParent(arg0);
			new Label(referensiLulusan.getNama()).setParent(arg0);
			new Label(referensiLulusan.getKeterangan()).setParent(arg0);
		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Capaian Lulusan");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Isi"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new MyTextbox());
		keterangan.addEventListener(Events.ON_OK, new EventListener() {
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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100. */
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
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Isi");
		column.setWidth("60%");

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
				AmbilDataReferensiLulusanBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<ReferensiLulusan> referensiLulusans = new ArrayList<ReferensiLulusan>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								ReferensiLulusan myReferensiLulusan = (ReferensiLulusan) row
										.getAttribute("referensiLulusan");
								referensiLulusans.add(myReferensiLulusan);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/obe/AmbilDataReferensiLulusanBanyak.java:258");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), referensiLulusans);
					eventListener.onEvent(myEvent);
				}
				AmbilDataReferensiLulusanBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (referensiLulusansHanyaDitampilkan != null) {
			for (ReferensiLulusan referensiLulusan : referensiLulusansHanyaDitampilkan) {
				values.add(referensiLulusan.getId());
			}
		}

		List<ReferensiLulusan> referensiLulusan = ConstantValues.simpleList(
				session.createCriteria(ReferensiLulusan.class).addOrder(Order.asc("kode"))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				ReferensiLulusan.class);

		List<Long> notIn = new ArrayList<Long>();
		if (referensiLulusans != null) {
			for (ReferensiLulusan u : referensiLulusans) {
				notIn.add(u.getId());
			}
		}

		List<ReferensiLulusan> myReferensiLulusan = session.createCriteria(ReferensiLulusan.class)

						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(notIn.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.not(Restrictions.in("id", notIn)))

						.addOrder(Order.desc("id"))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.not(Restrictions.in("id", ids)))
						.add(referensiLulusansHanyaDitampilkan == null || values.size() == 0
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.in("id", values))
						.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("kode", nama.getValue().trim(), MatchMode.ANYWHERE)))

						.add(keterangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("keterangan", keterangan.getValue().trim(), MatchMode.ANYWHERE))

						.setMaxResults(Common.MAX_RESULT).list();

		referensiLulusan.addAll(myReferensiLulusan);

		ListModel strset = new SimpleListModel(referensiLulusan);
		grid.setRowRenderer(new ReferensiLulusanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
