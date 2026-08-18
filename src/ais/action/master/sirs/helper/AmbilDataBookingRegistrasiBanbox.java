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

import ais.ui.util.MyTextbox;

import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.MyMessageboxConfig;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.BookingRegistrasi;

public class AmbilDataBookingRegistrasiBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataBookingRegistrasiBanbox() {
		super();

		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("bookingRegistrasi", null);
					setValue("");
					return;
				}

				BookingRegistrasi bookingRegistrasi = (BookingRegistrasi) HibernateUtil
						.currentSession()
						.createCriteria(BookingRegistrasi.class)
						.add(Restrictions.ilike("kode",
								AmbilDataBookingRegistrasiBanbox.this
										.getValue().trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (bookingRegistrasi == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data Booking Registrasi dengan kode \"{V1}\" tidak ditemukan. Langkah yang dapat dilakukan: (1) periksa kembali penulisan kode booking registrasi; (2) gunakan tombol pencarian untuk memilih dari daftar yang tersedia; (3) pastikan data booking registrasi telah terdaftar di dalam sistem.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							AmbilDataBookingRegistrasiBanbox.this.getValue().trim());
					return;
				}
				AmbilDataBookingRegistrasiBanbox.this.setOpen(false);
				AmbilDataBookingRegistrasiBanbox.this.setAttribute(
						"bookingRegistrasi", bookingRegistrasi);
				AmbilDataBookingRegistrasiBanbox.this
						.setValue(bookingRegistrasi.getKode()
								+ " - "
								+ (bookingRegistrasi.getPasien() == null ? ""
										: bookingRegistrasi.getPasien()
												.getNama()));
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

	private MyTextbox kodeBookingRegistrasian;
	private MyTextbox mr;
	private MyTextbox nama;

	class BookingRegistrasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final BookingRegistrasi bookingRegistrasi = (BookingRegistrasi) arg1;

			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataBookingRegistrasiBanbox.this.setOpen(false);
					AmbilDataBookingRegistrasiBanbox.this.setAttribute(
							"bookingRegistrasi", bookingRegistrasi);
					AmbilDataBookingRegistrasiBanbox.this.setValue(bookingRegistrasi
							.getKode()
							+ " - "
							+ (bookingRegistrasi.getPasien() == null ? ""
									: bookingRegistrasi.getPasien().getNama()));
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			final Pasien pasien = bookingRegistrasi.getPasien();

			new Label(bookingRegistrasi.getKode()).setParent(arg0);
			new Label(pasien == null ? "" : pasien.getKode()).setParent(arg0);
			new Label(pasien == null ? "" : pasien.getNama()).setParent(arg0);
			new Label(pasien == null ? "" : pasien.getAlamatLengkap())
					.setParent(arg0);
			new Label(
					bookingRegistrasi.getTanggalBookingRegistrasi() == null ? ""
							: Common.dateFormat3.get().format(bookingRegistrasi
									.getTanggalBookingRegistrasi()))
					.setParent(arg0);
			new Label(
					bookingRegistrasi.getTanggalBookingRegistrasi() == null ? ""
							: Common.dateFormat2.get().format(bookingRegistrasi
									.getBookingUntukTanggal())).setParent(arg0);
			new Label(bookingRegistrasi.getPoly() == null ? ""
					: bookingRegistrasi.getPoly().getNama()).setParent(arg0);
			new Label(bookingRegistrasi.getDokter() == null ? ""
					: bookingRegistrasi.getDokter().getNama()).setParent(arg0);

		}

	}

	public void display() {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		Panel panel = new Panel();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Booking Pasien");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("No. Book.")));
		row.appendChild(kodeBookingRegistrasian = new MyTextbox());
		kodeBookingRegistrasian.setWidth("90%");
		kodeBookingRegistrasian.addEventListener(Events.ON_OK,
				new EventListener() {
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

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
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
		button.setParent(toolbar);
		toolbar.appendChild(Common.createCleanButton(this, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					try {
						eventListener.onEvent(null);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/AmbilDataBookingRegistrasiBanbox.java:261");
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
		column.setLabel("No. Book.");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("MR");
		column.setWidth("10%");

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
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Unt. Tgl.");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Poli");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Dokter");
		column.setWidth("10%");

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<BookingRegistrasi> bookingRegistrasi = session
				.createCriteria(BookingRegistrasi.class)
				.addOrder(Order.desc("tanggalBookingRegistrasi"))
				.add(Restrictions.isNull("pendaftaran"))
				.createAlias("pasien", "pasien")
				.add(Restrictions.ilike("pasien.kode", mr.getValue().trim(),
						MatchMode.ANYWHERE))
				.add(Restrictions.ilike("pasien.nama", nama.getValue().trim(),
						MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodeBookingRegistrasian
						.getValue().trim(), MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(bookingRegistrasi);
		ListModel strset = new SimpleListModel(bookingRegistrasi);
		grid.setRowRenderer(new BookingRegistrasiRenderer());
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
