package ais.action.master.sirs.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.TransaksiRetur;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

/**
 * Tipe khusus untuk ambil data transaksi banbox. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Grid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code Boolean disableRetur}, {@code EventListener
 * eventListener}, {@code String sumber}, {@code String jenisTransaksi}, {@code Boolean yangBelumLunas}, {@code
 * Boolean tampilkanPaket}; pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()}, {@code
 * getEventListener()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataTransaksiBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Boolean disableRetur;

	private EventListener eventListener;
	private String sumber;
	private String jenisTransaksi;
	private Boolean yangBelumLunas;
	private Boolean tampilkanPaket;
	private Boolean pembeliBebas;

	public AmbilDataTransaksiBanbox(Boolean disableRetur, final String sumber,
			final String jenisTransaksi, final Boolean yangBelumLunas,
			final Boolean tampilkanPaket, final Boolean pembeliBebas) {
		super();
		this.pembeliBebas = pembeliBebas;
		this.sumber = sumber;
		this.jenisTransaksi = jenisTransaksi;
		this.disableRetur = disableRetur;
		this.yangBelumLunas = yangBelumLunas;
		this.tampilkanPaket = tampilkanPaket;
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("transaksi", null);
					setValue("");
					return;
				}

				TransaksiMedis transaksi = (TransaksiMedis) HibernateUtil
						.currentSession()
						.createCriteria(TransaksiMedis.class)

						.createAlias("pendaftaran", "pendaftaran",
								Criteria.LEFT_JOIN)
						.createAlias("pembayaran", "pembayaran",
								Criteria.LEFT_JOIN)

						.add(yangBelumLunas == null ? Restrictions
								.sqlRestriction("true")
								: !yangBelumLunas ? Restrictions.or(
										Restrictions.eq("pembayaran.lunas",
												false), Restrictions
												.isNull("pembayaran"))
										: Restrictions.eq("pembayaran.lunas",
												true))

						.add(sumber == null ? Restrictions
								.sqlRestriction("true") : Restrictions.eq(
								"sumber", sumber))
						.add(jenisTransaksi == null ? Restrictions
								.sqlRestriction("true") : Restrictions.eq(
								"jenisTransaksi", jenisTransaksi))

						.add(tampilkanPaket ? Restrictions
								.sqlRestriction("true") : Restrictions
								.isEmpty("pendaftaran.pakets"))

						.add(Restrictions
								.ilike("kode", AmbilDataTransaksiBanbox.this
										.getValue().trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (transaksi == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data transaksi dengan kode \"{V1}\" tidak dapat ditemukan di dalam sistem. Langkah yang dapat dilakukan: (1) periksa kembali ketepatan penulisan kode transaksi; (2) pastikan transaksi tersebut memang telah tercatat; (3) gunakan tombol pencarian untuk memilih transaksi langsung dari daftar.",
							"Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION,
							AmbilDataTransaksiBanbox.this.getValue().trim());
					return;
				}
				AmbilDataTransaksiBanbox.this.setOpen(false);
				AmbilDataTransaksiBanbox.this.setAttribute("transaksi",
						transaksi);
				AmbilDataTransaksiBanbox.this.setValue(transaksi.getKode());
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

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataTransaksiBanbox}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataTransaksiBanbox} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Session session}; operasi lokal:
	 * {@code render}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataTransaksiBanbox
	 */
	class TransaksiRenderer extends ais.ui.util.MyRowRenderer {

		private Session session = HibernateUtil.currentSession();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final TransaksiMedis transaksi = (TransaksiMedis) arg1;
			final Pasien pasien = transaksi.getPasien();

			Integer count = 0;
			if (disableRetur) {
				count = ((Number) session.createCriteria(TransaksiRetur.class)
						.add(Restrictions.eq("transaksi", transaksi))
						.setProjection(Projections.rowCount()).uniqueResult())
						.intValue();
			}

			if (!count.equals(0)) {
				arg0.setStyle("background-color:red;");
			} else {
				arg0.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataTransaksiBanbox.this.setOpen(false);
						AmbilDataTransaksiBanbox.this.setAttribute("transaksi",
								transaksi);
						AmbilDataTransaksiBanbox.this.setValue(transaksi
								.getKode());
						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});
			}

			new Label(transaksi.getKode()).setParent(arg0);
			new Label(pasien == null ? "" : pasien.getKode()).setParent(arg0);
			new Label(pasien == null ? transaksi.getNama() : pasien.getNama())
					.setParent(arg0);
			new Label(
					transaksi.getTanggalTransaksi() == null ? null
							: Common.dateFormat3.get().format(transaksi
									.getTanggalTransaksi())).setParent(arg0);
			new Label(count.equals(0) ? "Tidak" : "Ya").setParent(arg0);
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
		panel.setTitle("Daftar Transaksi");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Transaksi")));
		row.appendChild(kode = new MyTextbox());
		kode.setWidth("90%");
		kode.addEventListener(Events.ON_OK, new EventListener() {
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Pasien")));
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
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/AmbilDataTransaksiBanbox.java:297");
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
		column.setLabel("Kode Transaksi");

		column = new Column();
		column.setParent(columns);
		column.setLabel("MR");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Pasien");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Wkt Trns");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ada Retur");

		// onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<TransaksiMedis> transaksi = session
				.createCriteria(TransaksiMedis.class)

				.add(pembeliBebas != null ? Restrictions.eq("bebas",
						pembeliBebas) : Restrictions.sqlRestriction("true"))

				.createAlias("pendaftaran", "pendaftaran", Criteria.LEFT_JOIN)
				.createAlias("pembayaran", "pembayaran", Criteria.LEFT_JOIN)

				.add(yangBelumLunas == null ? Restrictions
						.sqlRestriction("true")
						: !yangBelumLunas ? Restrictions.or(
								Restrictions.eq("pembayaran.lunas", false),
								Restrictions.isNull("pembayaran"))
								: Restrictions.eq("pembayaran.lunas", true))

				.add(sumber == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("sumber", sumber))
				.add(jenisTransaksi == null ? Restrictions
						.sqlRestriction("true") : Restrictions.eq(
						"jenisTransaksi", jenisTransaksi))

				.add(tampilkanPaket ? Restrictions.sqlRestriction("true")
						: Restrictions.isEmpty("pendaftaran.pakets"))

				.createAlias("pasien", "pasien", Criteria.LEFT_JOIN)

				.addOrder(Order.desc("tanggalTransaksi"))
				.add(kode.getValue().trim().equals("") ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ilike("kode",
						kode.getValue().trim(), MatchMode.ANYWHERE))
				.add(mr.getValue().trim().equals("") ? Restrictions
						.sqlRestriction("1=1") : Restrictions
						.ilike("pasien.kode", mr.getValue().trim(),
								MatchMode.ANYWHERE))
				.add(nama.getValue().trim().equals("") ? Restrictions
						.sqlRestriction("1=1") : Restrictions.or(Restrictions
						.ilike("nama", nama.getValue().trim(),
								MatchMode.ANYWHERE), Restrictions.ilike(
						"pasien.nama", nama.getValue().trim(),
						MatchMode.ANYWHERE)))

				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(transaksi);
		grid.setRowRenderer(new TransaksiRenderer());
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
