package ais.action.master.kursus.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.kursus.KomponenProdukKursus;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.kursus.KomponenProdukKursus}
 * — lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback). Komponen produk kursus adalah item-item
 * pembentuk satu produk kursus di modul Kursus AIS (mis. biaya pendaftaran, materi, seragam), masing-masing
 * dengan kode, nama, harga, dan keterangan sendiri.
 *
 * <p>Pencarian memakai satu {@code Textbox nama} yang dicocokkan (ilike, tanpa memandang posisi substring)
 * ke kolom {@code kode} ATAU {@code nama} sekaligus, hanya terhadap komponen yang berstatus aktif
 * ({@code aktif} bernilai {@code null} atau {@code true}), diurutkan ascending berdasar nama. Hasil
 * ditampilkan dalam {@link Radiogroup} (pilih-tunggal via {@link Radio}) dengan grid client-side bermold
 * "paging" (page size 50, dibatasi {@code Common.MAX_RESULT_1000}); field {@code pagingHelper} dideklarasikan
 * namun tidak dipakai pada file ini. Kolom grid: Kode, Nama, Harga (rata kanan, diformat
 * {@code Common.numberFormat}), Keterangan. Tidak ada parameter constructor tambahan — konstruktor hanya
 * memasang pola lazy-open standar.</p>
 *
 * @see Bandbox
 */
public class AmbilDataKomponenProdukKursusBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/**
	 * Membangun Bandbox read-only dan memasang listener {@code onOpen} standar: popup dibangun lazy
	 * (sekali saja, dicek lewat {@code getChildren().isEmpty()}) via {@link #display()}, lalu dibuka
	 * lewat {@link Common#createDefaultTimer}. Tidak ada parameter filter dari entity induk.
	 */
	public AmbilDataKomponenProdukKursusBanbox() {
		super();
		setReadonly(true);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {

					display();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}
			}
		});

	}

	private Textbox nama;

	/**
	 * Merender satu baris grid hasil pencarian komponen produk kursus: label Nama, Harga (terformat),
	 * dan Keterangan, plus satu {@link Radio} pilihan di kolom pertama. Saat radio dicentang
	 * ({@code onCheck}), popup ditutup, entity {@link KomponenProdukKursus} terpilih disimpan lewat
	 * {@code setAttribute("komponenProdukKursus", ...)} dan teks tampilan Bandbox diisi
	 * {@code toString()}-nya, lalu {@link #eventListener} (bila terpasang) diberi tahu — mengikuti pola
	 * callback standar yang dijelaskan di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataKomponenProdukKursusBanbox
	 */
	class KomponenProdukKursusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KomponenProdukKursus komponenProdukKursus = (KomponenProdukKursus) arg1;
			Radio checkbox = new Radio(komponenProdukKursus.getKode());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKomponenProdukKursusBanbox.this.setOpen(false);
					AmbilDataKomponenProdukKursusBanbox.this.setAttribute("komponenProdukKursus", komponenProdukKursus);
					AmbilDataKomponenProdukKursusBanbox.this.setValue(komponenProdukKursus.toString());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(komponenProdukKursus.getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(komponenProdukKursus.getHarga())).setParent(arg0);
			new Label(komponenProdukKursus.getKeterangan()).setParent(arg0);

		}

	}

	/**
	 * Membangun isi {@link Bandpopup} sekali: panel judul "Daftar Komponen Produk" berisi form
	 * pencarian ({@code Textbox nama}) + tombol Cari/Bersihkan, dan grid hasil dibungkus
	 * {@link Radiogroup} (pilih-tunggal). Diakhiri memanggil {@link #onSearchDefault(Event)} dengan
	 * {@code null} agar grid langsung terisi saat popup pertama dibuka.
	 */
	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Komponen Produk");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

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

		toolbar.appendChild(Common.createCleanButton(this, this));

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
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
		column.setLabel("Kode");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Harga");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian {@link KomponenProdukKursus} yang aktif ({@code aktif} null atau
	 * {@code true}), difilter opsional lewat {@code Textbox nama} yang dicocokkan (ilike, kedua arah)
	 * ke kolom {@code kode} ATAU {@code nama} bila diisi, diurutkan ascending berdasar nama dan dibatasi
	 * {@code Common.MAX_RESULT_1000} baris. Hasil dipasang ke {@link #grid} lewat
	 * {@link KomponenProdukKursusRenderer} dan {@code SimpleListModel}.
	 *
	 * @param event event pemicu (boleh {@code null}, mis. saat dipanggil dari {@link #display()})
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<KomponenProdukKursus> komponenProdukKursus = session.createCriteria(KomponenProdukKursus.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("nama"))
						.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										Restrictions.ilike("kode", nama.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

						)

						.setMaxResults(Common.MAX_RESULT_1000)

						.list();

		System.out.println(komponenProdukKursus);
		ListModel strset = new SimpleListModel(komponenProdukKursus);
		grid.setRowRenderer(new KomponenProdukKursusRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Menetapkan listener yang dipanggil setelah baris komponen produk kursus dipilih.
	 *
	 * @param eventListener listener baru yang akan dipasang
	 */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * @return listener aktif saat ini, atau {@code null} bila belum diset
	 */
	public EventListener getEventListener() {
		return eventListener;
	}
}
