package ais.action.master.sirs.helper;

import java.util.List;

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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
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
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.ui.util.MyTextbox;

public class AmbilDataPendaftaranSemuaBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private Boolean lunas = null;

	public AmbilDataPendaftaranSemuaBanbox(Boolean lunas) {
		super();
		this.lunas = lunas;

		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("pendaftaran", null);
					setValue("");
					return;
				}

				Pendaftaran pendaftaran = (Pendaftaran) HibernateUtil
						.currentSession()
						.createCriteria(Pendaftaran.class)
						.add(Restrictions.ilike("kode",
								AmbilDataPendaftaranSemuaBanbox.this.getValue()
										.trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (pendaftaran == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data Pendaftaran dengan kode \"{V1}\" tidak ditemukan di dalam sistem. Langkah yang dapat Bapak/Ibu lakukan: (1) periksa kembali ketepatan penulisan kode Pendaftaran yang dimasukkan; (2) pastikan data Pendaftaran tersebut telah terdaftar pada sistem; (3) gunakan fitur pencarian pada tabel untuk menelusuri data yang tersedia.",
							"Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION,
							AmbilDataPendaftaranSemuaBanbox.this.getValue().trim());
					return;
				}
				AmbilDataPendaftaranSemuaBanbox.this.setOpen(false);
				AmbilDataPendaftaranSemuaBanbox.this.setAttribute(
						"pendaftaran", pendaftaran);
				AmbilDataPendaftaranSemuaBanbox.this.setValue(pendaftaran
						.getKode()
						+ " - "
						+ (pendaftaran.getPasien() == null ? "" : pendaftaran
								.getPasien().getNama()));
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

	private MyTextbox kodePendaftaranan;
	private MyTextbox mr;
	private MyTextbox nama;
	private Combobox rjOrRi;

	class PendaftaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Pendaftaran pendaftaran = (Pendaftaran) arg1;

			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPendaftaranSemuaBanbox.this.setOpen(false);
					AmbilDataPendaftaranSemuaBanbox.this.setAttribute(
							"pendaftaran", pendaftaran);
					AmbilDataPendaftaranSemuaBanbox.this.setValue(pendaftaran
							.getKode()
							+ " - "
							+ (pendaftaran.getPasien() == null ? ""
									: pendaftaran.getPasien().getNama()));
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			final Pasien pasien = pendaftaran.getPasien();

			new Label(pendaftaran.getKode()).setParent(arg0);
			new Label(pendaftaran.getJenis()).setParent(arg0);
			new Label(pasien == null ? "" : pasien.getKode()).setParent(arg0);
			new Label(pasien == null ? "" : pasien.getNama()).setParent(arg0);
			

			new Label(pasien.getAlamatLengkap()).setParent(arg0);
			new Label(pendaftaran.getTanggalPendaftaran() == null ? ""
					: Common.dateFormat3.get().format(pendaftaran
							.getTanggalPendaftaran())).setParent(arg0);
			new Label(pendaftaran.getPoly() == null ? "" : pendaftaran
					.getPoly().getNama()).setParent(arg0);
			new Label(pendaftaran.getDokter() == null ? "" : pendaftaran
					.getDokter().getNama()).setParent(arg0);
			new Label(pendaftaran.getNomorAntrian() == null ? ""
					: Common.numberFormat.get().format(pendaftaran.getNomorAntrian()))
					.setParent(arg0);

			String bed = (pendaftaran.getRuangPerawatan() == null ? ""
					: pendaftaran.getRuangPerawatan().getNama())
					+ " - "
					+ (pendaftaran.getKamarPerawatan() == null ? ""
							: pendaftaran.getKamarPerawatan().getNama())
					+ " - "
					+ (pendaftaran.getTempatTidur() == null ? "" : pendaftaran
							.getTempatTidur().getNama());

			new Label(bed).setParent(arg0);

		}

	}

	public void display() {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("950px");
		bandpopup.setHeight("600px");

		Panel panel = new Panel();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Pendaftar Pasien");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("No. Reg")));
		row.appendChild(kodePendaftaranan = new MyTextbox());
		kodePendaftaranan.setWidth("90%");
		kodePendaftaranan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("MR")));
		row.appendChild(mr = new MyTextbox());
		mr.setWidth("90%");
		mr.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Rajal/Ranap"));
		row.appendChild(rjOrRi = new Combobox());
		rjOrRi.setWidth("90%");

		Comboitem comboitem = new Comboitem(Pendaftaran.RAWAT_JALAN);
		comboitem.setValue(Pendaftaran.RAWAT_JALAN);
		rjOrRi.appendChild(comboitem);
		comboitem = new Comboitem(Pendaftaran.RAWAT_INAP);
		comboitem.setValue(Pendaftaran.RAWAT_INAP);
		rjOrRi.appendChild(comboitem);

		rjOrRi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

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
		toolbar.appendChild(Common.createCleanButton(this, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					try {
						eventListener.onEvent(null);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/AmbilDataPendaftaranSemuaBanbox.java:295");
					}
				}
				onSearchDefault(event);
			}
		}));

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
		column.setLabel("No. Reg");
		column.setWidth("7%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("MR");
		column.setWidth("5%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Alamat");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Wkt.");
		column.setWidth("7%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Poli");
		column.setWidth("8%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Dokter");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Antr");
		column.setWidth("5%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Bed");
		column.setWidth("10%");

		// onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Pendaftaran> pendaftaran = session
				.createCriteria(Pendaftaran.class)
				.add(lunas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("lunas", lunas))
				.createAlias("pasien", "pasien")
				.addOrder(Order.desc("id"))
				.add(rjOrRi.getSelectedItem() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq("jenis",
						rjOrRi.getSelectedItem().getValue()))
				.add(Restrictions.ilike("pasien.kode", mr.getValue().trim(),
						MatchMode.ANYWHERE))
				.add(Restrictions.ilike("pasien.nama", nama.getValue().trim(),
						MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodePendaftaranan.getValue()
						.trim(), MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(pendaftaran);
		ListModel strset = new SimpleListModel(pendaftaran);
		grid.setRowRenderer(new PendaftaranRenderer());
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
