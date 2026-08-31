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
import ais.ui.util.MyTextbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pembayaran;
import ais.database.model.sirs.Pendaftaran;

/**
 * Tipe khusus untuk ambil data pembayaran banbox. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Grid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code MyTextbox
 * kodePembayaranan}, {@code MyTextbox mr}, {@code MyTextbox nama}, {@code Combobox rjOrRi}; pembacaan/pencarian
 * ({@code onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}); operasi domain lain
 * ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataPembayaranBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataPembayaranBanbox() {
		super();
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("pembayaran", null);
					setValue("");
					return;
				}

				Pembayaran pembayaran = (Pembayaran) HibernateUtil
						.currentSession()
						.createCriteria(Pembayaran.class)
						.add(Restrictions.ilike("kode",
								AmbilDataPembayaranBanbox.this.getValue()
										.trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (pembayaran == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data Pembayaran dengan kode \"{V1}\" tidak ditemukan di dalam sistem. Langkah yang dapat Bapak/Ibu lakukan: (1) periksa kembali ketepatan penulisan kode Pembayaran yang dimasukkan; (2) pastikan data Pembayaran tersebut telah terdaftar pada sistem; (3) gunakan fitur pencarian pada tabel untuk menelusuri data yang tersedia.",
							"Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION,
							AmbilDataPembayaranBanbox.this.getValue().trim());
					return;
				}
				AmbilDataPembayaranBanbox.this.setOpen(false);
				AmbilDataPembayaranBanbox.this.setAttribute("pembayaran",
						pembayaran);
				AmbilDataPembayaranBanbox.this.setValue(pembayaran.getKode()
						+ " - "
						+ (pembayaran.getPasien() == null ? "" : pembayaran
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

	private MyTextbox kodePembayaranan;
	private MyTextbox mr;
	private MyTextbox nama;
	private Combobox rjOrRi;

	class PembayaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Pembayaran pembayaran = (Pembayaran) arg1;
			final Pendaftaran pendaftaran = pembayaran.getPendaftaran();

			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPembayaranBanbox.this.setOpen(false);
					AmbilDataPembayaranBanbox.this.setAttribute("pembayaran",
							pembayaran);
					AmbilDataPembayaranBanbox.this.setValue(pembayaran
							.getKode()
							+ " - "
							+ (pembayaran.getPasien() == null ? "" : pembayaran
									.getPasien().getNama()));
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			final Pasien pasien = pembayaran.getPasien();

			new Label(pembayaran.getKode()).setParent(arg0);
			new Label(pendaftaran == null ? "" : pendaftaran.getJenis())
					.setParent(arg0);
			new Label(pasien == null ? "" : pasien.getKode()).setParent(arg0);
			new Label(pasien == null ? "" : pasien.getNama()).setParent(arg0);
			

			new Label(pasien.getAlamatLengkap()).setParent(arg0);
			new Label(pembayaran.getTanggalPembayaran() == null ? ""
					: Common.dateFormat3.get().format(pembayaran
							.getTanggalPembayaran())).setParent(arg0);

			String bed = pendaftaran == null ? "" : (pendaftaran
					.getRuangPerawatan() == null ? "" : pendaftaran
					.getRuangPerawatan().getNama())
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
		bandpopup.setWidth("95%");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("No. Pemb.")));
		row.appendChild(kodePembayaranan = new MyTextbox());
		kodePembayaranan.setWidth("90%");
		kodePembayaranan.addEventListener(Events.ON_OK, new EventListener() {
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
		button.setParent(toolbar);toolbar.appendChild(Common.createCleanButton(this, new EventListener() {@Override public void onEvent(Event event) throws Exception {if(eventListener != null){try {eventListener.onEvent(null);} catch (Exception e) {e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/AmbilDataPembayaranBanbox.java:281");}}onSearchDefault(event);}}));

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
		column.setLabel("No. Pemb");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Rajal/Ranap");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("MR");
		column.setWidth("7%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Alamat");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Wkt. Pemb.");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Bed");
		column.setWidth("10%");

		// onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Pembayaran> pembayaran = session
				.createCriteria(Pembayaran.class)
				.createAlias("pasien", "pasien", Criteria.LEFT_JOIN)
				.createAlias("pendaftaran", "pendaftaran", Criteria.LEFT_JOIN)
				.addOrder(Order.desc("id"))
				.add(rjOrRi.getSelectedItem() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq(
						"pendaftaran.jenis", rjOrRi.getSelectedItem()
								.getValue()))
				.add(Restrictions.ilike("pasien.kode", mr.getValue().trim(),
						MatchMode.ANYWHERE))
				.add(Restrictions.ilike("pasien.nama", nama.getValue().trim(),
						MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodePembayaranan.getValue()
						.trim(), MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(pembayaran);
		ListModel strset = new SimpleListModel(pembayaran);
		grid.setRowRenderer(new PembayaranRenderer());
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
