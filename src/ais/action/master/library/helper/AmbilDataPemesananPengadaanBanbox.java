package ais.action.master.library.helper;

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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.PemesananPengadaanItem;
import ais.ui.util.MyTextbox;

/**
 * Tipe khusus untuk ambil data pemesanan pengadaan banbox. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code MyTextbox
 * kodePemesananPengadaanIteman}, {@code MyTextbox nama}, {@code AmbilDataPerpustakaanBanbox searchperpustakaan};
 * pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()});
 * operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataPemesananPengadaanBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataPemesananPengadaanBanbox() throws Exception {
		super();

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

	private MyTextbox kodePemesananPengadaanIteman;
	private MyTextbox nama;
	private AmbilDataPerpustakaanBanbox searchperpustakaan;

	class PemesananPengadaanItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final PemesananPengadaanItem pemesananPengadaanItem = (PemesananPengadaanItem) arg1;

			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (pemesananPengadaanItem.getDisetujuiOleh() == null) {
						MyMessageboxConfig.show(
								"Pemesanan yang anda pilih belum disetujui",
								"Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						return;
					}

					AmbilDataPemesananPengadaanBanbox.this.setOpen(false);
					AmbilDataPemesananPengadaanBanbox.this.setAttribute(
							"pemesananPengadaanItem", pemesananPengadaanItem);
					AmbilDataPemesananPengadaanBanbox.this
							.setValue(pemesananPengadaanItem.toString());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(pemesananPengadaanItem.getKode()).setParent(arg0);
			new Label(pemesananPengadaanItem.getKeterangan()).setParent(arg0);
			new Label(pemesananPengadaanItem.getPenyedia() == null ? ""
					: pemesananPengadaanItem.getPenyedia().getNama())
					.setParent(arg0);
			new Label(
					pemesananPengadaanItem.getTanggalPersetujuan() == null ? ""
							: Common.dateFormat.get().format(pemesananPengadaanItem
									.getTanggalPersetujuan())).setParent(arg0);
			new Label(pemesananPengadaanItem.getDisetujuiOleh() == null ? ""
					: pemesananPengadaanItem.getDisetujuiOleh().getUserId())
					.setParent(arg0);

		}

	}

	public void display() throws Exception {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Pemesanan Pengadaan Item");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kodePemesananPengadaanIteman = new MyTextbox());
		kodePemesananPengadaanIteman.setWidth("90%");
		kodePemesananPengadaanIteman.addEventListener(Events.ON_OK,
				new EventListener() {
					public void onEvent(Event event) throws Exception {
						onSearchDefault(event);
					}
				});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(searchperpustakaan = new AmbilDataPerpustakaanBanbox());
		searchperpustakaan.setWidth("90%");
		searchperpustakaan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
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
		toolbar.appendChild(Common.createCleanButton(this, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					try {
						eventListener.onEvent(null);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e); 
					}
				}
				onSearchDefault(event);
			}
		}));

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
		column.setLabel("Kode");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterengan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penyedia");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Disetujui");
		column.setWidth("10%");

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<PemesananPengadaanItem> pemesananPengadaanItem = session
				.createCriteria(PemesananPengadaanItem.class)
				.add(searchperpustakaan.getAttribute("perpustakaan") == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq(
						"perpustakaan",
						searchperpustakaan.getAttribute("perpustakaan")))
				.addOrder(Order.desc("id"))
				.add(Restrictions.ilike("keterangan", nama.getValue().trim(),
						MatchMode.ANYWHERE))
				.add(kodePemesananPengadaanIteman.getValue().trim().equals("") ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ilike("kode",
						kodePemesananPengadaanIteman.getValue().trim(),
						MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(pemesananPengadaanItem);
		ListModel strset = new SimpleListModel(pemesananPengadaanItem);
		grid.setRowRenderer(new PemesananPengadaanItemRenderer());
		grid.setModelCheckMobile(strset);

		
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}

