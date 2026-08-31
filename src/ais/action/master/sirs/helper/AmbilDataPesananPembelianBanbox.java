package ais.action.master.sirs.helper;

import java.util.List;

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
import ais.database.model.sirs.PenerimaanOrder;
import ais.database.model.sirs.PesananPembelian;

/**
 * Tipe khusus untuk ambil data pesanan pembelian banbox. Kelas ini memberi nama dan batas tanggung
 * jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Grid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code MyTextbox
 * kodePesananPembelianan}, {@code MyTextbox keterangan}; pembacaan/pencarian ({@code onSearchDefault()}, {@code
 * setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code display()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataPesananPembelianBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataPesananPembelianBanbox() {
		super();
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("pesananPembelian", null);
					setValue("");
					return;
				}

				PesananPembelian pesananPembelian = (PesananPembelian) HibernateUtil
						.currentSession()
						.createCriteria(PesananPembelian.class)
						.add(Restrictions.ilike("kode",
								AmbilDataPesananPembelianBanbox.this.getValue()
										.trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (pesananPembelian == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data Permintaan Pembelian dengan kode \"{V1}\" tidak ditemukan di dalam sistem. Langkah yang dapat Bapak/Ibu lakukan: (1) periksa kembali ketepatan penulisan kode Permintaan Pembelian yang dimasukkan; (2) pastikan data Permintaan Pembelian tersebut telah terdaftar pada sistem; (3) gunakan fitur pencarian pada tabel untuk menelusuri data yang tersedia.",
							"Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION,
							AmbilDataPesananPembelianBanbox.this.getValue().trim());
					return;
				}
				AmbilDataPesananPembelianBanbox.this.setOpen(false);
				AmbilDataPesananPembelianBanbox.this.setAttribute(
						"pesananPembelian", pesananPembelian);
				AmbilDataPesananPembelianBanbox.this.setValue(pesananPembelian
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

	private MyTextbox kodePesananPembelianan;
	private MyTextbox keterangan;

	class PesananPembelianRenderer extends ais.ui.util.MyRowRenderer {

		private Session session = HibernateUtil.currentSession();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final PesananPembelian pesananPembelian = (PesananPembelian) arg1;

			Integer jml = ((Number) session
					.createCriteria(PenerimaanOrder.class)
					.add(Restrictions.eq("pesananPembelian", pesananPembelian))
					.setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
			if (!jml.equals(0) || pesananPembelian.getDisetujuiOleh() == null) {
				arg0.setStyle("background-color:red;");
			} else {
				arg0.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataPesananPembelianBanbox.this.setOpen(false);
						AmbilDataPesananPembelianBanbox.this.setAttribute(
								"pesananPembelian", pesananPembelian);
						AmbilDataPesananPembelianBanbox.this
								.setValue(pesananPembelian.getKode());
						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});
			}

			new Label(pesananPembelian.getKode()).setParent(arg0);
			new Label(pesananPembelian.getDibuatOleh() == null ? ""
					: pesananPembelian.getDibuatOleh().getUserNama())
					.setParent(arg0);
			new Label(pesananPembelian.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(pesananPembelian
							.getTanggalPembuatan())).setParent(arg0);
			new Label(pesananPembelian.getDisetujuiOleh() == null ? ""
					: pesananPembelian.getDisetujuiOleh().getPegawai()
							.getNama()).setParent(arg0);
			new Label(pesananPembelian.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(pesananPembelian
							.getTanggalPersetujuan())).setParent(arg0);
			new Label(pesananPembelian.getKeterangan()).setParent(arg0);

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
		panel.setTitle("Daftar Permintaan Pembelian");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Permintaan Pembelian")));
		row.appendChild(kodePesananPembelianan = new MyTextbox());
		kodePesananPembelianan.setWidth("90%");
		kodePesananPembelianan.addEventListener(Events.ON_OK,
				new EventListener() {
					public void onEvent(Event event) throws Exception {
						onSearchDefault(event);
					}
				});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox());
		keterangan.setWidth("90%");
		keterangan.addEventListener(Events.ON_OK, new EventListener() {
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
		button.setParent(toolbar);toolbar.appendChild(Common.createCleanButton(this, new EventListener() {@Override public void onEvent(Event event) throws Exception {if(eventListener != null){try {eventListener.onEvent(null);} catch (Exception e) {e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/AmbilDataPesananPembelianBanbox.java:238");}}onSearchDefault(event);}}));

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
		column.setLabel("Kode Purchase Order");
		column.setWidth("25%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Dibuat Oleh");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Waktu Dibuat");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Disetujui Oleh");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Waktu Disetujui");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

		// onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<PesananPembelian> pesananPembelian = session
				.createCriteria(PesananPembelian.class)
				.addOrder(Order.desc("id"))

				.add(Restrictions.ilike("keterangan", keterangan.getValue()
						.trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodePesananPembelianan
						.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.isNotNull("disetujuiOleh"))
				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(pesananPembelian);
		ListModel strset = new SimpleListModel(pesananPembelian);
		grid.setRowRenderer(new PesananPembelianRenderer());
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
