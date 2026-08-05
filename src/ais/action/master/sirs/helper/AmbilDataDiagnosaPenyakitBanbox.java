package ais.action.master.sirs.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.MyMessageboxConfig;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Pasien;
import ais.ui.util.MyTextbox;

public class AmbilDataDiagnosaPenyakitBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataDiagnosaPenyakitBanbox() {
		super();
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("diagnosaPenyakit", null);
					setValue("");
					return;
				}

				DiagnosaPenyakit diagnosaPenyakit = (DiagnosaPenyakit) HibernateUtil
						.currentSession()
						.createCriteria(DiagnosaPenyakit.class)
						.add(Restrictions.ilike("kode",
								AmbilDataDiagnosaPenyakitBanbox.this.getValue()
										.trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (diagnosaPenyakit == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data Diagnosa Penyakit dengan kode \"{V1}\" tidak ditemukan. Langkah yang dapat dilakukan: (1) periksa kembali penulisan kode diagnosa penyakit; (2) gunakan tombol pencarian untuk memilih dari daftar yang tersedia; (3) pastikan data diagnosa penyakit telah terdaftar di dalam sistem.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							AmbilDataDiagnosaPenyakitBanbox.this.getValue().trim());
					return;
				}
				AmbilDataDiagnosaPenyakitBanbox.this.setOpen(false);
				AmbilDataDiagnosaPenyakitBanbox.this.setAttribute(
						"diagnosaPenyakit", diagnosaPenyakit);
				AmbilDataDiagnosaPenyakitBanbox.this.setValue(diagnosaPenyakit
						.getKode());
				if (eventListener != null) {
					eventListener.onEvent(arg0);
				}
			}
		});

		display();

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (grid == null || grid.getRows() == null
						|| grid.getRows().getChildren() == null
						|| grid.getRows().getChildren().size() == 0) {
					onSearchDefault(null);
				}
			}
		});
	}

	private MyTextbox kode;
	private MyTextbox mr;
	private MyTextbox nama;
	private MyTextbox telp;

	class DiagnosaPenyakitRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final DiagnosaPenyakit diagnosaPenyakit = (DiagnosaPenyakit) arg1;
			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataDiagnosaPenyakitBanbox.this.setOpen(false);
					AmbilDataDiagnosaPenyakitBanbox.this.setAttribute(
							"diagnosaPenyakit", diagnosaPenyakit);
					AmbilDataDiagnosaPenyakitBanbox.this
							.setValue(diagnosaPenyakit.getKode());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			Pasien pasien = diagnosaPenyakit.getPasien();

			new Label(diagnosaPenyakit.getKode()).setParent(arg0);

			new Label(pasien == null ? "" : pasien.getKode()).setParent(arg0);
			new Label(pasien == null ? "" : pasien.getNama()).setParent(arg0);
			new Label(pasien == null ? "" : pasien.getNoTelp() + "/"
					+ pasien.getNoHp()).setParent(arg0);
			new Label(pasien == null ? "" : pasien.getAlamatLengkap()).setParent(arg0);

			new Label(diagnosaPenyakit.getTanggal() == null ? ""
					: Common.dateFormat3.get().format(diagnosaPenyakit.getTanggal()))
					.setParent(arg0);
			new Label(diagnosaPenyakit.getKeluhanPasien() + " "
					+ diagnosaPenyakit.getKeluhanDiagnosa()).setParent(arg0);
			new Label(diagnosaPenyakit.getDiagnosaAwal1() == null ? ""
					: diagnosaPenyakit.getDiagnosaAwal1().getKode()
							+ " - "
							+ diagnosaPenyakit.getDiagnosaAwal1()
									.getNama_english()).setParent(arg0);
		}

	}

	public void display() {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("95%");
		bandpopup.setHeight("600px");

		Panel panel = new Panel();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Diagnosa Penyakit");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Diagnosis")));
		row.appendChild(kode = new MyTextbox());
		kode.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Pasien")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("MR Pasien")));
		row.appendChild(mr = new MyTextbox());
		mr.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Telp. Pasien")));
		row.appendChild(telp = new MyTextbox());
		telp.addEventListener(Events.ON_OK, new EventListener() {
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
		button.setParent(toolbar);toolbar.appendChild(Common.createCleanButton(this, new EventListener() {@Override public void onEvent(Event event) throws Exception {if(eventListener != null){try {eventListener.onEvent(null);} catch (Exception e) {e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/AmbilDataDiagnosaPenyakitBanbox.java:250");}}onSearchDefault(event);}}));

		// final Radiogroup radiogroup = new Radiogroup();
		// radiogroup.setWidth("100%");
		// radiogroup.setHeight("100%");
		// radiogroup.setParent(center);

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
		column.setLabel("Kode");

		column = new Column();
		column.setParent(columns);
		column.setLabel("MR");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Telp");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Alamat");
		column.setWidth("40%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Waktu Dgs.");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keluhan");

		column = new Column();
		column.setParent(columns);
		column.setLabel("ICD");

		// onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<DiagnosaPenyakit> diagnosaPenyakit = session
				.createCriteria(DiagnosaPenyakit.class)
				.createAlias("pasien", "pasien", Criteria.LEFT_JOIN)
				.addOrder(Order.desc("id"))
				.add(kode.getValue().trim().equals("") ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ilike("kode",
						kode.getValue().trim(), MatchMode.ANYWHERE))
				.add(mr.getValue().trim().equals("") ? Restrictions
						.sqlRestriction("1=1") : Restrictions
						.ilike("pasien.kode", mr.getValue().trim(),
								MatchMode.ANYWHERE))
				.add(nama.getValue().trim().equals("") ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ilike(
						"pasien.nama", nama.getValue().trim(),
						MatchMode.ANYWHERE))
				.add(Restrictions.or(
						telp.getValue().trim().equals("") ? Restrictions
								.sqlRestriction("1=1") : Restrictions.ilike(
								"pasien.noTelp", telp.getValue().trim(),
								MatchMode.ANYWHERE),
						telp.getValue().trim().equals("") ? Restrictions
								.sqlRestriction("1=1") : Restrictions.ilike(
								"pasien.noHp", telp.getValue().trim(),
								MatchMode.ANYWHERE)))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(diagnosaPenyakit);
		ListModel strset = new SimpleListModel(diagnosaPenyakit);
		grid.setRowRenderer(new DiagnosaPenyakitRenderer());
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
