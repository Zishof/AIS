package ais.action.master.helper;

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
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Detailperkuliahan}
 * — lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback). {@code Detailperkuliahan} adalah baris
 * KRS/transkrip: satu mata kuliah yang telah/sedang ditempuh seorang {@link Mahasiswa} tertentu,
 * merujuk ke {@link Matakuliah} lewat {@code perkuliahan} (kelas/tawaran perkuliahan reguler) ATAU
 * lewat {@code matakuliahKonversi} (mata kuliah hasil konversi/pengakuan kredit dari luar, mis.
 * transfer/RPL).
 * <p>
 * Komponen ini SELALU dibatasi ke satu mahasiswa tertentu — diberikan wajib lewat constructor
 * {@link #AmbilDataDetailPerkuliahanBanbox(Mahasiswa)}, bukan dipilih user. Popup menampilkan grid
 * pilih-tunggal (via {@link Radiogroup}) dengan filter "Kode" dan "Nama" (keduanya ILIKE ANYWHERE,
 * dicocokkan ke kode/nama {@link Matakuliah}), dibatasi ke detail perkuliahan berstatus
 * {@link Detailperkuliahan#DISETUJUI}. Hasil merupakan GABUNGAN dua query terpisah (mata kuliah
 * reguler + mata kuliah konversi) yang digabung jadi satu list Java setelah query, BUKAN satu
 * query UNION di database — masing-masing diurutkan sendiri berdasarkan semester lalu nama mata
 * kuliah sebelum digabung, sehingga urutan gabungan tidak sepenuhnya terjaga menaik.
 *
 * @see Bandbox
 */
public class AmbilDataDetailPerkuliahanBanbox extends Bandbox implements GetEventListener {

	/**
	 * Serial version UID standar untuk kompatibilitas serialisasi komponen ZK.
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private Mahasiswa selectedMahasiswa = null;

	/**
	 * Membangun komponen dibatasi ke satu mahasiswa: memasang mode read-only standar dan
	 * listener {@code onOpen} yang membangun popup ({@link #display()}) hanya pada pembukaan
	 * pertama, mengikuti kerangka umum di {@link ais.ui.util.GetEventListener}.
	 *
	 * @param mahasiswa mahasiswa pemilik detail perkuliahan yang akan dicari; disimpan ke
	 *                  {@link #selectedMahasiswa} dan dipakai sebagai filter wajib di setiap
	 *                  pencarian
	 */
	public AmbilDataDetailPerkuliahanBanbox(Mahasiswa mahasiswa) {
		super();
		this.selectedMahasiswa = mahasiswa;
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

	private Textbox kodeMatakuliahan;
	private Textbox nama;

	/**
	 * Merender satu baris grid: radio pilih, kode+id mata kuliah, nama, SKS, semester, dan tahun
	 * akademik — mata kuliah dibaca dari {@code perkuliahan.matakuliah} bila ada, atau dari
	 * {@code matakuliahKonversi} untuk baris hasil konversi. Memilih baris menutup popup,
	 * menyimpan entity {@link Detailperkuliahan} terpilih ke attribute
	 * {@code "detailperkuliahan"} pada Bandbox, mengisi teks tampilan dengan "kode - nama", lalu
	 * memicu {@link #eventListener} bila terpasang — mengikuti kerangka callback standar di
	 * {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataDetailPerkuliahanBanbox
	 */
	class MatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) arg1;
			final Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() != null
					? detailperkuliahan.getPerkuliahan().getMatakuliah() : (detailperkuliahan.getMatakuliahKonversi());
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataDetailPerkuliahanBanbox.this.setOpen(false);
					AmbilDataDetailPerkuliahanBanbox.this.setAttribute("detailperkuliahan", detailperkuliahan);
					AmbilDataDetailPerkuliahanBanbox.this.setValue(matakuliah.getKode() + " - " + matakuliah.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(matakuliah.getKode() + " (" + matakuliah.getId() + ") ").setParent(arg0);
			new Label(matakuliah.getNama()).setParent(arg0);
			new Label(matakuliah.getSks() + "").setParent(arg0);
			new Label(detailperkuliahan.getSemester() == null ? "" : detailperkuliahan.getSemester().toString())
					.setParent(arg0);
			new Label(detailperkuliahan.getTahunAkademik()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (dipanggil sekali saat pertama dibuka): form filter Kode/Nama,
	 * grid hasil bermold "paging", lalu memuat data awal lewat {@link #onSearchDefault(Event)}.
	 */
	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("700px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Matakuliah");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kodeMatakuliahan = new Textbox());
		kodeMatakuliahan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
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
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Akademik");
		column.setWidth("20%");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan DUA kriteria pencarian {@link Detailperkuliahan} terpisah untuk
	 * {@link #selectedMahasiswa} berstatus {@link Detailperkuliahan#DISETUJUI}, masing-masing
	 * cocok filter Kode/Nama (ILIKE ANYWHERE): satu untuk mata kuliah reguler (via alias
	 * {@code perkuliahan.matakuliah}), satu lagi untuk mata kuliah konversi (via alias
	 * {@code matakuliahKonversi}); hasil keduanya digabung jadi satu list (bukan UNION SQL).
	 * Mengisi ulang grid dengan hasilnya beserta {@link MatakuliahRenderer}.
	 *
	 * @param event tidak dipakai, hanya mengikuti signature standar listener pencarian
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<Detailperkuliahan> matakuliah;

		matakuliah = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
				.createAlias("perkuliahan", "perkuliahan").createAlias("perkuliahan.matakuliah", "matakuliah")
				.add(Restrictions.eq("mahasiswa", selectedMahasiswa)).addOrder(Order.asc("semester"))
				.add(kodeMatakuliahan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("matakuliah.kode", kodeMatakuliahan.getValue().trim(), MatchMode.ANYWHERE))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("matakuliah.nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.addOrder(Order.asc("matakuliah.nama")).list();

		List<Detailperkuliahan> konversis = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
				.createAlias("matakuliahKonversi", "matakuliahKonversi")
				.add(Restrictions.eq("mahasiswa", selectedMahasiswa)).addOrder(Order.asc("semester"))

		.add(kodeMatakuliahan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("matakuliahKonversi.kode", kodeMatakuliahan.getValue().trim(), MatchMode.ANYWHERE))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("matakuliahKonversi.nama", nama.getValue().trim(), MatchMode.ANYWHERE))

		.addOrder(Order.asc("matakuliahKonversi.nama")).list();
		matakuliah.addAll(konversis);

		ListModel strset = new SimpleListModel(matakuliah);
		grid.setRowRenderer(new MatakuliahRenderer());
		grid.setModelCheckMobile(strset);
	}

	/** @param eventListener dipanggil setiap kali user memilih satu detail perkuliahan */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan detail perkuliahan yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}
}

