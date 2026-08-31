package ais.action.master.helper.generic;

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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
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
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BuktiPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ambil data bukti pembayaran banyak. Kelas ini memberi nama dan batas tanggung
 * jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code List
 * buktiPembayarans}, {@code List buktiPembayaransHanyaDitampilkan}, {@code Set ids}, {@code Mahasiswa
 * mahasiswa}, {@code JenisKegiatan jenisKegiatan}; pembacaan/pencarian ({@code tampilkan()}, {@code
 * tampilkan()}, {@code onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}); operasi
 * domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataBuktiPembayaranBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<BuktiPembayaran> buktiPembayarans;
	private List<BuktiPembayaran> buktiPembayaransHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();
	private Mahasiswa mahasiswa;
	private JenisKegiatan jenisKegiatan;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private Integer semester;

	public AmbilDataBuktiPembayaranBanyak(List<BuktiPembayaran> buktiPembayarans, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan) {
		this(buktiPembayarans, mahasiswa, biodataCalonMahasiswa, jenisKegiatan, null);
	}

	public AmbilDataBuktiPembayaranBanyak(List<BuktiPembayaran> buktiPembayarans, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, Integer semester) {
		super();
		this.buktiPembayarans = buktiPembayarans;
		this.mahasiswa = mahasiswa;
		this.jenisKegiatan = jenisKegiatan;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		this.semester = semester;
		display();
		onSearchDefault(null);
	}

	public AmbilDataBuktiPembayaranBanyak(List<BuktiPembayaran> buktiPembayarans,
			List<BuktiPembayaran> buktiPembayaransHanyaDitampilkan, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan) {
		this(buktiPembayarans, buktiPembayaransHanyaDitampilkan, mahasiswa, biodataCalonMahasiswa, jenisKegiatan,
				null);
	}

	public AmbilDataBuktiPembayaranBanyak(List<BuktiPembayaran> buktiPembayarans,
			List<BuktiPembayaran> buktiPembayaransHanyaDitampilkan, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, Integer semester) {
		super();
		this.buktiPembayarans = buktiPembayarans;
		this.buktiPembayaransHanyaDitampilkan = buktiPembayaransHanyaDitampilkan;
		this.mahasiswa = mahasiswa;
		this.jenisKegiatan = jenisKegiatan;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		this.semester = semester;
		display();

		onSearchDefault(null);
	}

	public static AmbilDataBuktiPembayaranBanyak tampilkan(List<BuktiPembayaran> buktiPembayarans,
			Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan,
			Integer semester, EventListener eventListener) throws InterruptedException {
		AmbilDataBuktiPembayaranBanyak window = new AmbilDataBuktiPembayaranBanyak(buktiPembayarans, mahasiswa,
				biodataCalonMahasiswa, jenisKegiatan, semester);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setEventListener(eventListener);
		window.setWidth("97%");
		window.setHeight("97%");
		window.setVisible(true);
		window.onModal();
		return window;
	}

	public static AmbilDataBuktiPembayaranBanyak tampilkan(List<BuktiPembayaran> buktiPembayarans,
			List<BuktiPembayaran> buktiPembayaransHanyaDitampilkan, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, Integer semester,
			EventListener eventListener) throws InterruptedException {
		AmbilDataBuktiPembayaranBanyak window = new AmbilDataBuktiPembayaranBanyak(buktiPembayarans,
				buktiPembayaransHanyaDitampilkan, mahasiswa, biodataCalonMahasiswa, jenisKegiatan, semester);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setEventListener(eventListener);
		window.setWidth("97%");
		window.setHeight("97%");
		window.setVisible(true);
		window.onModal();
		return window;
	}

	private MyTextbox nama;

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataBuktiPembayaranBanyak}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataBuktiPembayaranBanyak} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataBuktiPembayaranBanyak
	 */
	class BuktiPembayaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final BuktiPembayaran buktiPembayaran = (BuktiPembayaran) arg1;
			arg0.setAttribute("buktiPembayaran", buktiPembayaran);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (BuktiPembayaran myBuktiPembayaran : buktiPembayarans) {
				if (myBuktiPembayaran != null && myBuktiPembayaran.getId() != null
						&& myBuktiPembayaran.getId().equals(buktiPembayaran.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(buktiPembayaran.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(buktiPembayaran.getId());
					} else {
						ids.remove(buktiPembayaran.getId());
					}
				}
			});

			new Label(buktiPembayaran.getMahasiswa() == null ? (buktiPembayaran.getBiodataCalonMahasiswa().toString())
					: buktiPembayaran.getMahasiswa().getNim() + "-" + buktiPembayaran.getMahasiswa().getNama())
							.setParent(arg0);

			Vbox vbox = RevisiHelper.createNewRevisi(BuktiPembayaran.class, buktiPembayaran, buktiPembayaran.getNama());
			vbox.setParent(arg0);

			Vbox myvbox = new Vbox();
			myvbox.setParent(vbox);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, buktiPembayaran.getIdLampiran(),
					BuktiPembayaran.class.getName(), "Bukti Pembayaran", true, null, null, false, false, true, false);

			myvbox = new Vbox();
			myvbox.setParent(vbox);

			new Label(Common.dateFormat6.get().format(buktiPembayaran.getTanggal())).setParent(arg0);

			new Label(buktiPembayaran.getItemBiaya() == null ? "" : buktiPembayaran.getItemBiaya().getNama())
					.setParent(arg0);
			new Label(buktiPembayaran.getSemester() + "").setParent(arg0);
			new Label(Common.numberFormat.get().format(buktiPembayaran.getNilai())).setParent(arg0);
			new Label(buktiPembayaran.getKeterangan()).setParent(arg0);

		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Bukti Pembayaran");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Bukti"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
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
		column.setLabel("Mahasiswa");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Bukti");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("14%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Item Biaya");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setWidth("7%");
		column.setAlign("right");

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
				AmbilDataBuktiPembayaranBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<BuktiPembayaran> buktiPembayarans = new ArrayList<BuktiPembayaran>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						if (checkbox.isChecked() && !checkbox.isDisabled()) {
							BuktiPembayaran myBuktiPembayaran = (BuktiPembayaran) row.getAttribute("buktiPembayaran");
							buktiPembayarans.add(myBuktiPembayaran);
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), buktiPembayarans);
					eventListener.onEvent(myEvent);
				}
				AmbilDataBuktiPembayaranBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (buktiPembayaransHanyaDitampilkan != null) {
			for (BuktiPembayaran buktiPembayaran : buktiPembayaransHanyaDitampilkan) {
				values.add(buktiPembayaran.getId());
			}
		}

		List<BuktiPembayaran> buktiPembayaran = session.createCriteria(BuktiPembayaran.class)

				.add(mahasiswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa", mahasiswa))
				.add(biodataCalonMahasiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))

				.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
				.add(semester == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semester", semester))
				.add(Restrictions.isNull("cicilanPembayaran"))

				.addOrder(Order.desc("tanggal"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)).list();

		List<BuktiPembayaran> myBuktiPembayaran = session.createCriteria(BuktiPembayaran.class)
				.add(mahasiswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa", mahasiswa))
				.add(biodataCalonMahasiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))

				.add(Restrictions.eq("jenisKegiatan", jenisKegiatan)).addOrder(Order.desc("tanggal"))
				.add(semester == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semester", semester))
				.add(Restrictions.isNull("cicilanPembayaran"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(buktiPembayaransHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();

		buktiPembayaran.addAll(myBuktiPembayaran);

		ListModel strset = new SimpleListModel(buktiPembayaran);
		grid.setRowRenderer(new BuktiPembayaranRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
