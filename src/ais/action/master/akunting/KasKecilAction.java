package ais.action.master.akunting;

import java.io.File;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.dashboard.akunting.DasboardKasKecil;
import ais.action.master.akunting.helper.MonitorKasKecilDashboard;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.akunting.LaporanKasKecil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.JenisKasKecil;
import ais.database.model.akunting.KasKecil;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.PenggantianKasKecil;
import ais.database.model.akunting.UangMuka;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCheckboxConfigPilih;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h3>KasKecilAction — Pengelola Pengeluaran Kas Kecil (Petty Cash)</h3>
 *
 * <p><b>Untuk apa:</b> Kelas ini merupakan controller ZK untuk modul Kas Kecil (petty cash)
 * dalam sistem keuangan eCampus. Modul ini mengelola dokumen pengeluaran kas kecil per transaksi:
 * staf mengajukan pengeluaran operasional kecil dengan merinci item biaya (akun, keterangan,
 * qty, harga, tanggal), kemudian penyetuju menyetujuinya, dan sistem secara otomatis membuat
 * {@link ais.database.model.akunting.PenggantianKasKecil} beserta
 * {@link ais.database.model.akunting.DaftarPengajuanTransfer} untuk penggantian dana.</p>
 *
 * <p><b>Cara kerja:</b> Kelas mengimplementasikan empat antarmuka: {@code DataCriteria},
 * {@code DataSearchDefault}, {@code DataInitDefault}, dan {@code FormSop}. Setiap pengeluaran
 * kas kecil berisi data: satuan kerja, jenis kas kecil (yang menentukan saldo tersedia dan akun
 * penerima), tanggal, daftar rincian biaya (disimpan dalam field {@code formula} sebagai JSONArray),
 * status (PENGAJUAN/DISETUJU/DITOLAK), dan flag penutupan kas kecil.</p>
 *
 * <p><b>Struktur formula biaya:</b> Setiap item biaya disimpan sebagai JSONObject dalam JSONArray
 * dengan field: {@code key} (identifier unik randLong), {@code akun} (id Akun biaya),
 * {@code workspace} (id Workspace/anggaran terkait), {@code nama} (keterangan), {@code qty},
 * {@code harga}, {@code jumlah} (qty*harga), {@code tanggal}, {@code nama_file}, {@code link},
 * dan {@code id_file} (jika lampiran sudah diupload).</p>
 *
 * <p><b>Alur proses:</b>
 * <ol>
 *   <li>Staf membuat pengeluaran (status=PENGAJUAN) dengan mengisi rincian biaya.
 *       Sistem memeriksa saldo kas kecil dan memastikan tidak ada kas kecil lain yang
 *       belum disetujui untuk jenis kas kecil yang sama.</li>
 *   <li>Penyetuju mengubah status ke DISETUJU. Sistem membuat {@code PenggantianKasKecil}
 *       dan {@code DaftarPengajuanTransfer} untuk penggantian kas.</li>
 *   <li>Setelah disetujui, pengeluaran tidak dapat diedit lagi (read-only).</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b> Semua operasi berjalan di UI thread ZK. Operasi "Hitung Ulang"
 * saldo massal dan cetak laporan pasca simpan dijalankan melalui
 * {@link Common#createDefaultTimer} (timer ZK sekali tembak) untuk menghindari
 * blocking UI thread langsung.</p>
 *
 * <p><b>Pemeliharaan:</b> Konfigurasi {@code kas_kecil_dimulai} menentukan prefix kode
 * akun yang ditampilkan di picker akun biaya. Jenis kas kecil menentukan saldo via
 * {@link JenisKasKecilAction#hitungSaldo} yang dipanggil secara sinkron di UI thread —
 * jika banyak data, pertimbangkan caching atau pembatasan maxResults.</p>
 *
 * @see ais.database.model.akunting.KasKecil
 * @see ais.database.model.akunting.JenisKasKecil
 * @see ais.database.model.akunting.PenggantianKasKecil
 * @see JenisKasKecilAction#hitungSaldo
 */
public class KasKecilAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 *
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox serachnama;
	private Textbox serachjenis;
	private Textbox serachkode;
	private Combobox searchstatus;
	private Checkbox searchaktif;
	private MyDatebox start;
	private MyDatebox end;
	private Textbox nama;
	private Label kode;
	private Textbox keterangan;

	public KasKecil kasKecil;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;

	private Combobox jenisKasKecil;

	private Double nilai;

	private MyDatebox tanggal;

	private DisposisiSop disposisiSop;

	private JSONArray array;

	private Row rowFormula;

	private boolean persetujuan = false;

	private Tbmuser tbmuser;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private Radiogroup status;

	private boolean setujui = false;
	protected LampiranLain lainMahasiswa;
	private Tabpanel jenisKasKecilTab;

	private boolean viewOnly = false;

	/**
	 * Memuat konten tab "Jenis Kas Kecil" secara lazy saat tab pertama kali diklik.
	 *
	 * <p><b>Tujuan:</b> Event handler untuk klik tab Jenis Kas Kecil. Menggunakan pola
	 * lazy loading: konten tab tidak dimuat saat halaman pertama kali dibuka, melainkan
	 * hanya saat pengguna mengklik tab tersebut. Hal ini mempercepat waktu muat awal
	 * halaman.</p>
	 *
	 * <p><b>Cara kerja:</b> Memeriksa apakah tab sudah memiliki anak komponen. Jika belum,
	 * membuat {@link MyWindow} sebagai wadah dan menambahkan {@link MyInclude} yang merujuk
	 * ke ZUL halaman {@code /pages/master/akunting/jenis_kas_kecil.zul}. Jika sudah ada anak
	 * (tab sudah pernah dibuka), tidak melakukan apa-apa.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Path ZUL hardcoded. Jika file ZUL dipindahkan atau diganti nama,
	 * perbarui string path di sini. Tab ini hanya tersedia pada halaman yang memiliki komponen
	 * {@code jenisKasKecilTab} di ZUL-nya.</p>
	 *
	 * @param event event ZK dari klik tab; nilai event tidak digunakan
	 */
	public void onKasKecil(Event event) {

		if (jenisKasKecilTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisKasKecilTab);
			MyInclude iframe = new MyInclude("/pages/master/akunting/jenis_kas_kecil.zul");
			iframe.setParent(window);
		}
	}

	protected Tabpanel statistik;
	/** Tabpanel tab "Monitor" — dashboard pemantauan & sirkulasi kas kecil (lazy). */
	protected Tabpanel monitor;

	private Row rowtanggal;

	private MyCheckboxConfig merupakanPenutupanKasKecil;

	private Label nilaiHarusDikembalikan;

	private Label saldo;

//	private MyCheckboxConfig tampilkanAnggaran;

	/**
	 * Memuat dashboard statistik kas kecil secara lazy saat tab Statistik pertama kali diklik.
	 *
	 * <p><b>Tujuan:</b> Event handler untuk klik tab Statistik/Analisis. Sama dengan
	 * {@link #onKasKecil(Event)}, menggunakan pola lazy loading untuk efisiensi: dashboard
	 * berat {@link DasboardKasKecil} hanya diinstansiasi ketika benar-benar dibutuhkan.</p>
	 *
	 * <p><b>Cara kerja:</b> Memeriksa apakah {@code statistik} Tabpanel sudah memiliki anak.
	 * Jika belum, membuat instance {@link DasboardKasKecil} (komponen dashboard analitik kas kecil)
	 * dan menambahkannya ke panel. Dashboard ini menampilkan grafik dan ringkasan pengeluaran
	 * kas kecil dari seluruh unit.</p>
	 *
	 * <p><b>Pemeliharaan:</b> {@link DasboardKasKecil} adalah komponen berat yang melakukan
	 * query database saat diinstansiasi. Pastikan lazy loading ini tidak diubah menjadi eager
	 * loading untuk menghindari perlambatan muat halaman utama.</p>
	 *
	 * @param event event ZK dari klik tab; nilai event tidak digunakan
	 */
	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DasboardKasKecil include = new DasboardKasKecil();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(statistik);
		}
	}

	/**
	 * Memuat dashboard "Monitor" (pemantauan & sirkulasi kas kecil) secara lazy saat tab pertama
	 * kali diklik. Menampilkan kartu ringkas, status penggantian, tren pemakaian per bulan,
	 * sirkulasi per satuan kerja, skor kesehatan (radar), aging belum-diganti, dan rincian.
	 */
	public void onMonitor(Event event) {
		if (monitor != null && monitor.getChildren().size() == 0) {
			MonitorKasKecilDashboard dashboard = new MonitorKasKecilDashboard();
			dashboard.setHeight("100%");
			dashboard.setWidth("100%");
			dashboard.setParent(monitor);
		}
	}

	/**
	 * Konstruktor default untuk mode pengajuan pengeluaran kas kecil.
	 *
	 * <p><b>Tujuan:</b> Membuat instance controller dalam mode pengajuan normal
	 * ({@code persetujuan=false}). Digunakan saat halaman diakses oleh staf
	 * yang mengajukan pengeluaran kas kecil.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link Common#getCurrentUser()} untuk mengambil
	 * pengguna aktif dari sesi HTTP. Pengguna ini dipakai sebagai {@code dibuatOleh}
	 * pada entitas {@link ais.database.model.akunting.KasKecil} yang baru dibuat.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Konstruktor ini tidak memanggil {@code super()} secara
	 * eksplisit; ZK memanggil konstruktor super secara internal sebelum wire komponen.</p>
	 */
	public KasKecilAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Konstruktor dengan mode persetujuan eksplisit.
	 *
	 * <p><b>Tujuan:</b> Membuat instance controller dengan mode persetujuan ({@code true})
	 * atau mode pengajuan ({@code false}). Perbedaan mode:
	 * <ul>
	 *   <li><b>Mode pengajuan (false):</b> Tombol Tambah terlihat, form dapat diisi penuh,
	 *       sistem memvalidasi saldo sebelum menyimpan.</li>
	 *   <li><b>Mode persetujuan (true):</b> Tombol Tambah tersembunyi, form hanya-baca
	 *       kecuali radio status persetujuan, checkbox aktif per baris tersedia.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Cara kerja:</b> Menetapkan field {@code this.persetujuan} sebelum ZK wire
	 * komponen. Nilai ini dapat juga di-override oleh parameter URL {@code ?persetujuan=true}
	 * yang diproses di {@link #doAfterCompose(Component)}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Konstruktor ini dipakai oleh modul SOP dan factory method
	 * eksternal. Pastikan logika mode konsisten antara konstruktor ini, parameter URL,
	 * dan {@link #setPersetujuan(boolean)}.</p>
	 *
	 * @param persetujuan {@code true} untuk mode persetujuan; {@code false} untuk pengajuan
	 */
	public KasKecilAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
	}

	public static String[] contents = new String[] { "id", "kode", "nama", "keterangan", "formula", "satuanKerja",
			"jenisKasKecil", "tanggal", "nilai", "penggantianKasKecil.kode", "penggantianKasKecil.nama",
			"penggantianKasKecil.status", "penggantianKasKecil.daftarPengajuanTransfer.prosesTransfer.kode",
			"penggantianKasKecil.daftarPengajuanTransfer.prosesTransfer.nama",
			"penggantianKasKecil.daftarPengajuanTransfer.prosesTransfer.tanggalPembuatan",
			"penggantianKasKecil.daftarPengajuanTransfer.prosesTransfer.disetujuiOleh",
			"penggantianKasKecil.daftarPengajuanTransfer.prosesTransfer.tanggalPersetujuan",
			"penggantianKasKecil.daftarPengajuanTransfer.prosesTransfer.realisasikanOleh",
			"penggantianKasKecil.daftarPengajuanTransfer.prosesTransfer.tanggalRealisasikan", "aktif" };

	/**
	 * Memeriksa keamanan sesi sebelum komponen ZUL mulai di-compose.
	 *
	 * <p><b>Tujuan:</b> Mencegah akses tidak sah ke halaman Kas Kecil dengan memvalidasi
	 * token keamanan sebelum ZK memulai proses composing komponen. Jika sesi tidak valid,
	 * pengguna di-redirect ke halaman login sebelum komponen apapun dirender.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link Common#doCheckSecurity()} yang memeriksa
	 * atribut sesi {@code usersTemp}. Kemudian memanggil implementasi super agar proses
	 * compose ZK tetap berjalan normal.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jangan tambahkan logika bisnis di sini. Gunakan
	 * {@link #doAfterCompose(Component)} untuk inisialisasi yang membutuhkan akses
	 * ke komponen ZK yang sudah ter-wire.</p>
	 *
	 * @param page     halaman ZK yang sedang dicompose
	 * @param parent   komponen induk (bisa null jika halaman root)
	 * @param compInfo metadata komponen dari ZUL parser
	 * @return {@code ComponentInfo} dari super class untuk dilanjutkan ke pipeline ZK
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Menginisialisasi seluruh komponen UI halaman Kas Kecil setelah ZK selesai wire.
	 *
	 * <p><b>Tujuan:</b> Mempersiapkan halaman Kas Kecil agar siap digunakan: validasi sesi,
	 * inisialisasi filter tanggal dan status, pengecekan hak akses CRUD, penambahan tombol
	 * ekspor/upload/hitung-ulang, serta memuat data awal via timer.</p>
	 *
	 * <p><b>Cara kerja — langkah-langkah inisialisasi:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk auto-wire field dari ZUL.</li>
	 *   <li>Memanggil {@link Common#initLaguage()} untuk set bahasa.</li>
	 *   <li>Memvalidasi sesi dan hak akses READ. Jika gagal, memanggil {@link Common#goLogoff()}.</li>
	 *   <li>Merefresh {@code tbmuser} dari sesi aktif (penting karena bisa ada multi-sesi).</li>
	 *   <li>Memasang listener pada {@code searchparent} untuk refresh grid saat satuan kerja
	 *       filter berubah.</li>
	 *   <li>Mengatur tanggal filter default: 6 bulan lalu hingga besok.</li>
	 *   <li>Mengisi combobox status filter dengan Semua/PENGAJUAN/DISETUJU/DITOLAK.</li>
	 *   <li>Memeriksa parameter URL {@code ?persetujuan=true} untuk override mode.</li>
	 *   <li>Mengatur visibilitas tombol Tambah berdasarkan CREATE privilege dan mode.</li>
	 *   <li>Menambahkan tombol ekspor/upload data ke toolbar.</li>
	 *   <li>Menambahkan tombol "Hitung Ulang" yang menghitung ulang saldo semua kas kecil
	 *       via {@link JenisKasKecilAction#hitungSaldo} dalam timer sekali tembak.</li>
	 *   <li>Memanggil {@link FilterLanjutHelper#setup} untuk fitur filter lanjutan.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Guard {@code if (searchparent == null) return;} melindungi dari
	 * ZUL yang tidak memiliki komponen filter (misalnya ZUL mode persetujuan yang disederhanakan).
	 * Tambahkan null-check untuk komponen ZUL baru yang mungkin tidak ada di semua varian ZUL.</p>
	 *
	 * @param comp komponen root ZUL yang telah di-compose oleh ZK framework
	 * @throws Exception jika inisialisasi gagal
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		tbmuser = Common.getCurrentUser();
		if (searchparent == null) return;
		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		if (searchstatus != null) {
			Comboitem comboitemSemua = new Comboitem("Semua");
			comboitemSemua.setValue(null);
			searchstatus.appendChild(comboitemSemua);

			Comboitem comboitem = new Comboitem(KasKecil.PENGAJUAN);
			comboitem.setValue(KasKecil.PENGAJUAN);
			searchstatus.appendChild(comboitem);
			comboitem = new Comboitem(KasKecil.DISETUJU);
			comboitem.setValue(KasKecil.DISETUJU);
			searchstatus.appendChild(comboitem);
			comboitem = new Comboitem(KasKecil.DITOLAK);
			comboitem.setValue(KasKecil.DITOLAK);
			searchstatus.appendChild(comboitem);

			searchstatus.setSelectedItem(comboitemSemua);
			searchstatus.setReadonly(true);
		}

		if (execution.getParameter("persetujuan") != null) {
			persetujuan = Boolean.parseBoolean(execution.getParameter("persetujuan"));
		}

		if (add != null) {
			add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE) && !persetujuan);
			add.setTooltiptext("Tambah");
		}
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KasKecil.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KasKecil.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hitung Ulang", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<KasKecil> kasKecils = initCriteria(false).addOrder(Order.asc("id")).setMaxResults(5000)
								.list();

						for (KasKecil kasKecil : kasKecils) {
							Double saldo = JenisKasKecilAction.hitungSaldo(kasKecil.getId(),
									kasKecil.getJenisKasKecil(), kasKecil.getTanggal());

							if (saldo.intValue() != kasKecil.getSaldo().intValue()) {
								kasKecil.setSaldo(saldo);
								Common.refreshUpdate(kasKecil);
							}

						}

						onSearchDefault(null);
					}
				});

			}

		});
		if (button != null && add != null && add.getParent() != null) { button.setParent(add.getParent()); }
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Membangun dan mengembalikan grid read-only yang menampilkan rincian biaya sebuah kas kecil.
	 *
	 * <p><b>Tujuan:</b> Metode statis utilitas yang dapat dipanggil dari modul lain (misalnya
	 * laporan atau popup detail) untuk menampilkan rincian biaya kas kecil tanpa perlu
	 * membuat instance controller penuh. Grid yang dihasilkan bersifat read-only.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code kasKecil} null, mengembalikan {@code Grid} kosong.</li>
	 *   <li>Membuat Grid baru dengan kolom: Akun/Keterangan, Qty, Harga, Jumlah.</li>
	 *   <li>Mem-parse {@code formula} JSONArray dari entitas kas kecil.</li>
	 *   <li>Untuk setiap item JSONObject yang valid, membuat baris dengan informasi:
	 *       nama akun, tanggal, keterangan, qty, harga, dan jumlah.</li>
	 *   <li>Menambahkan footer row dengan total nilai dari {@code kasKecil.getNilai()}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Grid ini tidak memiliki interaksi (read-only). Untuk versi
	 * interaktif dengan input field, gunakan {@link #reloadDataFormula}. Jika kolom baru
	 * ditambahkan ke formula, tambahkan juga di sini untuk konsistensi tampilan.</p>
	 *
	 * @param kasKecil entitas yang akan ditampilkan rincian biayanya; jika {@code null}
	 *                 mengembalikan Grid kosong tanpa error
	 * @return {@link Grid} ZK yang berisi rincian biaya; tidak pernah {@code null}
	 * @throws Exception jika terjadi error parsing JSONArray atau format tanggal tidak valid
	 */
	public static Grid tampilRinci(KasKecil kasKecil) throws Exception {
		if (kasKecil != null) {
			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("Akun/Keterangan");
			column.setParent(columns);

			column = new MyColumnConfig("Qty");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig("Harga");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("20%");

			column = new MyColumnConfig("Jumlah");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("20%");

			Rows rows = new Rows();
			rows.setParent(grid);
			JSONArray array = new JSONArray(kasKecil.getFormula());
			for (int i = 0; i < array.length(); i++) {

				JSONObject jsonObject = array.getJSONObject(i);

				Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
						: ConstantValues.ambil(Akun.class.getName(),
								ais.common.CommonJSONUtil.ambilLong(jsonObject, "akun")));

				String nama = "";

				if (!jsonObject.isNull("nama")) {
					nama = jsonObject.get("nama") + "";
				}

				Double qty = 0.0;
				if (!jsonObject.isNull("qty")) {
					qty = jsonObject.getDouble("qty");
				}

				Double harga = 0.0;
				if (!jsonObject.isNull("harga")) {
					harga = jsonObject.getDouble("harga");
				}

				Double jumlah = 0.0;
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}

				Date tanggal = WaktuUtil.getDate();
				if (!jsonObject.isNull("tanggal")) {
					tanggal = Common.dateFormat9.get().parse(jsonObject.getString("tanggal"));
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				Vbox vbox = new Vbox();
				row.appendChild(vbox);

				vbox.appendChild(new MyLabelAgakKecil(akunBiaya == null ? "" : akunBiaya.toString()));
				vbox.appendChild(new MyLabelAgakKecil(Common.dateFormat.get().format(tanggal)));
				vbox.appendChild(new MyLabelAgakKecil(nama));

				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(qty)));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(harga)));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(jumlah)));
			}
			Foot foot = new Foot();
			foot.setParent(grid);

			Footer footer = new Footer("Total");
			foot.appendChild(footer);

			footer = new Footer("");
			foot.appendChild(footer);

			footer = new Footer("");
			foot.appendChild(footer);

			Footer footerTotal = new Footer(Common.numberFormat.get().format(kasKecil.getNilai()));
			foot.appendChild(footerTotal);

			return grid;
		} else {
			return new Grid();
		}
	}

	/**
	 * Renderer baris grid untuk daftar {@link ais.database.model.akunting.KasKecil}.
	 *
	 * <p><b>Untuk apa:</b> Kelas inner ini bertanggung jawab atas rendering setiap baris
	 * pada grid daftar pengeluaran kas kecil. Setiap baris menampilkan kode, jenis kas kecil,
	 * saldo, nilai pengeluaran, sisa saldo, tanggal, pemohon, status persetujuan, keterangan,
	 * status penggantian, dan tombol aksi.</p>
	 *
	 * <p><b>Perhatian performa:</b> Metode {@code render} ini memanggil
	 * {@link JenisKasKecilAction#hitungSaldo} secara sinkron untuk setiap baris. Jika ada
	 * banyak baris di halaman, ini dapat menyebabkan banyak query database. Pertimbangkan
	 * caching saldo jika performa menjadi masalah.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Urutan penambahan komponen ke baris (Row) harus konsisten dengan
	 * jumlah kolom di ZUL. Perubahan kolom ZUL harus diikuti perubahan di sini.</p>
	 */
	class KasKecilRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data kas kecil ke dalam komponen ZK.
		 *
		 * <p><b>Tujuan:</b> Mengubah entitas {@link ais.database.model.akunting.KasKecil}
		 * menjadi baris UI ZK lengkap dengan komponen interaktif, termasuk widget upload
		 * bukti pengeluaran, link ke proses transfer terkait, dan pembuatan otomatis
		 * PenggantianKasKecil jika kondisi terpenuhi.</p>
		 *
		 * <p><b>Cara kerja:</b>
		 * <ol>
		 *   <li>Menetapkan {@code dibuatOleh} jika null (data lama).</li>
		 *   <li>Menghitung ulang saldo via {@link JenisKasKecilAction#hitungSaldo} dan
		 *       menyimpan ke entitas jika berubah.</li>
		 *   <li>Menghitung ulang total nilai dari JSONArray dan memperbarui entitas jika berbeda.</li>
		 *   <li>Menampilkan kode (dengan revisi history), link ke proses transfer kas besar
		 *       atau penggantian kas kecil (jika ada).</li>
		 *   <li>Menyediakan widget upload/unduh bukti pengeluaran per baris.</li>
		 *   <li>Menampilkan jenis kas kecil, saldo, nilai, dan sisa saldo.</li>
		 *   <li>Menampilkan tanggal, satuan kerja, dan pembuat.</li>
		 *   <li>Menampilkan status, penyetuju, dan tanggal persetujuan.</li>
		 *   <li>Menampilkan keterangan dan link SOP jika ada alur SOP.</li>
		 *   <li>Menampilkan status aktif atau checkbox aktif (khusus mode persetujuan).</li>
		 *   <li>Menampilkan status penggantian dana (kas besar / penggantian kas kecil).</li>
		 *   <li>Jika kas kecil sudah disetujui dan SOP selesai tetapi belum ada PenggantianKasKecil,
		 *       membuat PenggantianKasKecil baru dan DaftarPengajuanTransfer secara otomatis.</li>
		 *   <li>Menambahkan tombol Edit/Delete/Cetak.</li>
		 * </ol>
		 * </p>
		 *
		 * @param arg0 baris ZK (Row) yang akan diisi komponen
		 * @param arg1 objek {@link ais.database.model.akunting.KasKecil} yang dirender
		 * @throws Exception jika terjadi error database atau komponen ZK
		 */
		/**
		 * Rapikan kolom Kode/Nama: potong label tombol yang terlalu panjang (mis. nama berkas bukti)
		 * agar tidak melebar/berlipat; nama lengkap tetap tampil sebagai tooltip saat kursor diarahkan.
		 */
		private void rapikanLabelPanjang(org.zkoss.zk.ui.Component c) {
			if (c == null) {
				return;
			}
			if (c instanceof org.zkoss.zul.Toolbarbutton) {
				org.zkoss.zul.Toolbarbutton tb = (org.zkoss.zul.Toolbarbutton) c;
				String lbl = tb.getLabel();
				if (lbl != null && lbl.trim().length() > 24) {
					tb.setTooltiptext(lbl.trim());
					tb.setLabel(lbl.trim().substring(0, 22).trim() + "…");
				}
			}
			for (Object ch : c.getChildren()) {
				rapikanLabelPanjang((org.zkoss.zk.ui.Component) ch);
			}
		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final KasKecil kasKecil = (KasKecil) arg1;

			if (kasKecil.getDibuatOleh() == null) {
				kasKecil.setDibuatOleh(tbmuser);
			}

			Double saldo = JenisKasKecilAction.hitungSaldo(kasKecil.getId(), kasKecil.getJenisKasKecil(),
					kasKecil.getTanggal());

			if (saldo.intValue() != kasKecil.getSaldo().intValue()) {
				kasKecil.setSaldo(saldo);
				Common.refreshUpdate(kasKecil);
			}

			try {
				Double nilai = 0.0;
				JSONArray array = new JSONArray(kasKecil.getFormula());
				for (int i = 0; i < array.length(); i++) {
					Double jumlah = 0.0;
					JSONObject jsonObject = array.getJSONObject(i);
					Long key = null;
					if (!jsonObject.isNull("key")) {
						key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
					}

					if (key != null) {
						if (!jsonObject.isNull("jumlah")) {
							jumlah = jsonObject.getDouble("jumlah");
						}
						nilai += jumlah;
					}
				}

				if (nilai.intValue() != kasKecil.getNilai().intValue()) {
					Session session = HibernateUtil.currentSession();
					kasKecil.setNilai(nilai);
					session.update(kasKecil);
					session.flush();
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(KasKecil.class, kasKecil,
					kasKecil.getKode() == null ? "" : kasKecil.getKode().trim().toString())).setParent(arg0);

			if (kasKecil.getKasBesar() != null && kasKecil.getKasBesar().getDaftarPengajuanTransfer() != null
					&& kasKecil.getKasBesar().getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A aaa = new A(kasKecil.getKasBesar().getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				aaa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, kasKecil.getKasBesar().getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				aaa.setStyle("font-size:12px;");
				aaa.setParent(a);
			}

			else if (kasKecil.getPenggantianKasKecil() != null
					&& kasKecil.getPenggantianKasKecil().getDaftarPengajuanTransfer() != null
					&& kasKecil.getPenggantianKasKecil().getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A aaa = new A(
						kasKecil.getPenggantianKasKecil().getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				aaa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, kasKecil.getPenggantianKasKecil().getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				aaa.setStyle("font-size:12px;");
				aaa.setParent(a);
			}

			if (kasKecil.getPenggantianKasKecil() == null && kasKecil.getDisetujuiOleh() != null
					&& kasKecil.getDisposisiSop() != null && kasKecil.getDisposisiSop().getDisposisiEnd() != null
					&& kasKecil.getDisposisiSop().getDisposisiEnd() != null
					&& kasKecil.getDisposisiSop().getDisposisiEnd().getSelesai()) {
				PenggantianKasKecil penggantianKasKecil = new PenggantianKasKecil();
				penggantianKasKecil.setDisposisiSop(kasKecil.getDisposisiSop());
				penggantianKasKecil.setKasKecil(kasKecil);
				penggantianKasKecil.setDisetujuiOleh(kasKecil.getDisetujuiOleh());
				penggantianKasKecil.setTanggalPersetujuan(kasKecil.getTanggalPersetujuan());

				Session session = HibernateUtil.currentSession();
				session.save(penggantianKasKecil);
				session.flush();

				kasKecil.setPenggantianKasKecil(penggantianKasKecil);

				Common.refreshUpdate(session, kasKecil);
				session.flush();

				DaftarPengajuanTransfer.simpanPenggantianKasKecil(penggantianKasKecil);
			}

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			// Rapikan kolom Kode/Nama: bungkus tombol bukti dalam Div ber-CSS "kk-bukti"
			// (nama berkas panjang dipotong rapi dgn ellipsis, ikon tetap sebaris).
			org.zkoss.zul.Div boxBukti = new org.zkoss.zul.Div();
			boxBukti.setSclass("kk-bukti");
			boxBukti.setParent(myvbox);

			Hbox hbox = new Hbox();
			hbox.setSpacing("4px");
			hbox.setParent(boxBukti);
			LampiranLain.createDownloadUploadFileLain(hbox, kasKecil.getId(), KasKecil.class.getName(), "Bukti", false,
					null, null, false, false, false, kasKecil.getDisetujuiOleh() == null);
			rapikanLabelPanjang(boxBukti);

			new Label(kasKecil.getJenisKasKecil() == null ? ""
					: kasKecil.getJenisKasKecil().getKode() + "-" + kasKecil.getJenisKasKecil().getNama())
					.setParent(arg0);

			new Label(Common.numberFormat.get().format(kasKecil.getSaldo())).setParent(arg0);
			new Label(Common.numberFormat.get().format(kasKecil.getNilai())).setParent(arg0);
			new Label(kasKecil.getSaldo() <= 0.0 ? ""
					: Common.numberFormat.get().format(kasKecil.getSaldo() - kasKecil.getNilai())).setParent(arg0);

			a = new Vbox();
			a.setParent(arg0);
			new Label(kasKecil.getTanggal() == null ? "" : Common.dateFormat3.get().format(kasKecil.getTanggal()))
					.setParent(a);
			new Label(kasKecil.getSatuanKerja() == null ? "" : kasKecil.getSatuanKerja().getNama()).setParent(a);

			new MyLabelAgakKecil(kasKecil.getDibuatOleh() == null ? "" : kasKecil.getDibuatOleh().getUserNama())
					.setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label(kasKecil.getStatus()).setParent(a);
			(new MyLabelAgakKecil(kasKecil.getDisetujuiOleh() == null ? "" : kasKecil.getDisetujuiOleh().getUserNama()))
					.setParent(a);
			(new MyLabelAgakKecil(kasKecil.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(kasKecil.getTanggalPersetujuan()))).setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);

			new MyLabelAgakKecil(Common.simpleString(kasKecil.getKeterangan())).setParent(vbox1);
			if (kasKecil.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				UIClassHelper.applyReadMore(aa, "SOP " + kasKecil.getDisposisiSop().getKeterangan() + " ("
						+ kasKecil.getDisposisiSop().getSop().getNama() + ")");
				aa.setStyle("font-size:9px;");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(kasKecil.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			if (kasKecil.getDisposisiSop() != null && !kasKecil.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (persetujuan && !kasKecil.getStatus().equals(KasKecil.DISETUJU)) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(kasKecil.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kasKecil.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(kasKecil);
					}
				});
			} else {
				new Label(kasKecil.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			if (kasKecil.getKasBesar() != null) {
				vbox1 = new Vbox();
				vbox1.setParent(arg0);

				(new Label(kasKecil.getKasBesar().getDaftarPengajuanTransfer() == null
						|| kasKecil.getKasBesar().getDaftarPengajuanTransfer().getProsesTransfer() == null
								? "Belum Diganti Kas Besar"
								: "Telah diganti Kas Besar"))
						.setParent(vbox1);

				DaftarPengajuanTransfer.tampilStatus(kasKecil.getKasBesar().getDaftarPengajuanTransfer(), vbox1);
			} else if (kasKecil.getPenggantianKasKecil() != null
					&& kasKecil.getPenggantianKasKecil().getDisetujuiOleh() != null
					&& kasKecil.getPenggantianKasKecil().getDaftarPengajuanTransfer() != null) {
				vbox1 = new Vbox();
				vbox1.setParent(arg0);

				(new Label(kasKecil.getPenggantianKasKecil().getDaftarPengajuanTransfer().getProsesTransfer() == null
						? "Belum Diganti"
						: "Telah diganti")).setParent(vbox1);

				DaftarPengajuanTransfer.tampilStatus(kasKecil.getPenggantianKasKecil().getDaftarPengajuanTransfer(),
						vbox1);
			} else {
				(new Label(kasKecil.getPenggantianKasKecil() == null
						|| kasKecil.getPenggantianKasKecil().getDisetujuiOleh() == null
						|| kasKecil.getPenggantianKasKecil().getTanggalPersetujuan() == null ? "Belum Diganti"

								: "Telah disetujui penggantian \"" + kasKecil.getNama() + "\" pada "
										+ Common.dateFormat3.get()
												.format(kasKecil.getPenggantianKasKecil().getTanggalPersetujuan())))
						.setParent(arg0);
			}

			Hbox hbx;
			(hbx = Common.copyEditDeleteButtons(edit, !persetujuan && !kasKecil.getStatus().equals(KasKecil.DISETUJU),
					delete && !persetujuan && !kasKecil.getStatus().equals(KasKecil.DISETUJU), kasKecil,
					KasKecilAction.this)).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(kasKecil);
				}

			});
			button.setParent(hbx);
		}

	}

	/**
	 * Menghasilkan file PDF laporan pengeluaran kas kecil untuk keperluan ekspor/unduhan batch.
	 *
	 * <p><b>Tujuan:</b> Implementasi antarmuka yang menghasilkan {@link File} PDF sementara
	 * yang kemudian dikirim sebagai unduhan ke browser oleh {@code Common.cetakData}.
	 * Metode ini dipanggil ketika pengguna mengklik tombol cetak di toolbar (bukan per baris).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Meng-cast {@code generalValueObject} ke {@link ais.database.model.akunting.KasKecil}.</li>
	 *   <li>Membuat instance {@link LaporanKasKecil} yang merupakan generator laporan khusus
	 *       kas kecil menggunakan JasperReports.</li>
	 *   <li>Memanggil {@link ais.action.report.Report#generateFileReport} dengan template
	 *       {@code akunting/kasKecil} untuk menghasilkan file PDF.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Template JasperReports berada di path {@code akunting/kasKecil.jrxml}.
	 * Perubahan layout laporan dilakukan di file template tersebut.</p>
	 *
	 * @param generalValueObject entitas {@link ais.database.model.akunting.KasKecil} yang akan
	 *                           dicetak, dicast dari {@link GeneralValueObject}
	 * @return {@link File} PDF sementara berisi laporan kas kecil
	 * @throws Exception jika template JasperReports tidak ditemukan atau terjadi error generate
	 */
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		KasKecil kasKecil = (KasKecil) generalValueObject;
		LaporanKasKecil buktiPengeluaranKas = new LaporanKasKecil(kasKecil);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setVisible(false);
		File file = Report.generateFileReport(Report.PDF, buktiPengeluaranKas.generateParameter(), "akunting/kasKecil",
				ais.ui.util.WaktuUtil.getDate(), null, new Toolbar());
		return file;
	}

	/**
	 * Menampilkan laporan PDF kas kecil secara langsung di browser sebagai window modal.
	 *
	 * <p><b>Tujuan:</b> Membuka laporan PDF kas kecil dalam window modal ZK yang dapat
	 * ditutup pengguna. Berbeda dengan {@link #cetakData(GeneralValueObject)} yang menghasilkan
	 * file unduhan, metode ini menampilkan laporan langsung di UI tanpa mengunduh file.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat instance {@link LaporanKasKecil} dengan entitas yang diberikan.</li>
	 *   <li>Mengkonfigurasi window: judul "Laporan", closable, tinggi 90%, lebar 900px.</li>
	 *   <li>Menambahkan window ke root komponen halaman aktif via {@link ExecutionsCtrl}.</li>
	 *   <li>Memanggil {@code onModal()} untuk menampilkan sebagai dialog modal.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Bersifat statis agar dapat dipanggil dari renderer baris grid
	 * dan dari {@link #onSave(Event)} tanpa referensi instance. Harus dipanggil dari UI thread.
	 * Saat ini dipanggil dengan delay 2500ms via timer setelah simpan (via {@link #onSave}).</p>
	 *
	 * @param kasKecil entitas kas kecil yang akan dicetak; tidak boleh {@code null}
	 * @throws Exception jika terjadi error pada JasperReports atau komponen ZK
	 */
	public static void cetak(KasKecil kasKecil) throws Exception {
		LaporanKasKecil buktiPengeluaranKas = new LaporanKasKecil(kasKecil);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		buktiPengeluaranKas.onModal();
	}

	/**
	 * Membuka form edit/lihat kas kecil yang sudah ada dari external trigger atau grid.
	 *
	 * <p><b>Tujuan:</b> Implementasi antarmuka {@link DataInitDefault} untuk menerima entitas
	 * kas kecil yang sudah ada (dari klik tombol Edit di baris grid atau dari framework SOP)
	 * dan menampilkannya dalam window modal yang dapat diedit atau hanya dilihat.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Meng-cast {@code obj} ke {@link ais.database.model.akunting.KasKecil}.</li>
	 *   <li>Memanggil {@link #init(KasKecil)} (private overload) untuk membangun window.</li>
	 *   <li>Menampilkan dan mengaktifkan modal pada {@code addWindow}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Berbeda dari {@link #onAdd(Event)} yang membuat entitas baru.
	 * Mode edit atau view-only ditentukan oleh status entitas (DISETUJU = view-only).</p>
	 *
	 * @param obj entitas {@link ais.database.model.akunting.KasKecil} yang akan ditampilkan;
	 *            tidak boleh {@code null}
	 * @throws Exception jika terjadi error pada inisialisasi form atau komponen ZK
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kasKecil = (KasKecil) obj;
		init(kasKecil);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membuka form untuk membuat pengeluaran kas kecil baru.
	 *
	 * <p><b>Tujuan:</b> Event handler untuk tombol "Tambah" di toolbar. Membuat entitas
	 * {@link ais.database.model.akunting.KasKecil} baru, menginisialisasi form kosong,
	 * dan membuka window modal untuk diisi pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mereset flag {@code viewOnly} ke {@code false} agar form dapat diedit.</li>
	 *   <li>Memanggil {@link #init(KasKecil)} dengan entitas baru yang belum memiliki ID.</li>
	 *   <li>Menampilkan dan mengaktifkan modal pada {@code addWindow}.</li>
	 *   <li>Menangkap exception onModal() dan menampilkan error jika pengguna adalah admin.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Tombol ini hanya terlihat jika {@code CommonPrivilages.CREATE}
	 * aktif dan mode bukan persetujuan (dikonfigurasi di {@link #doAfterCompose(Component)}).</p>
	 *
	 * @param event event ZK dari klik tombol Tambah; nilai tidak digunakan
	 * @throws Exception jika terjadi error pada inisialisasi form
	 */
	public void onAdd(Event event) throws Exception {
		viewOnly = false;
		init(new KasKecil());
		addWindow.setVisible(true);
		try {
			addWindow.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun dan mengembalikan panel form pengeluaran kas kecil sebagai {@link MyGrid}.
	 *
	 * <p><b>Tujuan:</b> Implementasi antarmuka {@link FormSop} yang membangun seluruh UI form
	 * pengisian pengeluaran kas kecil. Form ini mendukung tiga mode: pengajuan baru, edit
	 * pengajuan yang ada, dan tampilan persetujuan/read-only.</p>
	 *
	 * <p><b>Cara kerja — elemen form:</b>
	 * <ol>
	 *   <li><b>Satuan Kerja:</b> Picker dengan lazy load daftar kas kecil berdasarkan satuan kerja.</li>
	 *   <li><b>Kode:</b> Auto-generate via {@link #generateCode(boolean)} untuk entitas baru.</li>
	 *   <li><b>Judul:</b> Textbox nama pengeluaran (wajib isi).</li>
	 *   <li><b>Jenis Kas Kecil:</b> Combobox yang diisi berdasarkan satuan kerja aktif dan
	 *       satuan kerja pengguna (menggunakan {@code eventListenerKas}).</li>
	 *   <li><b>Info dinamis:</b> Unit, Saldo Kas Kecil, Akun Penerima — diperbarui otomatis
	 *       saat jenis kas kecil berubah.</li>
	 *   <li><b>Tanggal Pengajuan:</b> Datebox yang memicu reload rincian biaya.</li>
	 *   <li><b>Bukti Pengeluaran:</b> Upload file PDF/ZIP (satu file; zip jika banyak).</li>
	 *   <li><b>Keterangan:</b> Textarea 2 baris.</li>
	 *   <li><b>Rincian Biaya:</b> Grid interaktif via {@link #reloadFormula} dengan kolom
	 *       Anggaran, Akun, Tanggal, Qty, Harga, Jumlah, dan tombol hapus.</li>
	 *   <li><b>Sisa Saldo:</b> Kalkulasi otomatis saldo - total biaya.</li>
	 *   <li><b>Penutupan Kas Kecil:</b> Checkbox (admin atau non-persetujuan).</li>
	 *   <li><b>Status:</b> Radio group PENGAJUAN/DISETUJU/DITOLAK (hanya mode persetujuan).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Mode tampilan:</b>
	 * <ul>
	 *   <li><b>Normal:</b> Semua field dapat diedit, tombol save "Simpan dan Cetak".</li>
	 *   <li><b>Persetujuan / setujui / viewOnly:</b> Field diganti Label, hanya radio status
	 *       yang aktif. Tombol save berubah label sesuai status radio yang dipilih.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Listener {@code tanggalData} memanggil {@link #reloadFormula}
	 * setiap kali tanggal berubah untuk menyinkronkan tahun anggaran (workspace) dengan tahun
	 * tanggal yang dipilih. Ini penting karena workspace difilter by tahun.</p>
	 *
	 * @param generalValueObject entitas {@link ais.database.model.akunting.KasKecil} yang
	 *                           diisi/ditampilkan
	 * @param disposisiSop       konteks SOP (bisa null untuk akses langsung)
	 * @param save               tombol simpan yang label-nya berubah sesuai status
	 * @param setujuiData        listener SOP untuk sinkronisasi; bisa null
	 * @return {@link MyGrid} yang berisi seluruh form, siap ditambahkan ke container
	 * @throws Exception jika terjadi error query atau pembuatan komponen ZK
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
		kasKecil = (KasKecil) generalValueObject;
		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}
		setujui = false;
		if (!persetujuan) {
			if (kasKecil != null && kasKecil.getStatus().equals(KasKecil.DISETUJU)) {
				setujui = true;
			} else {
				setujui = false;
			}
		}

		if (kasKecil != null && kasKecil.getPenggantianKasKecil() != null
				&& kasKecil.getPenggantianKasKecil().getStatus().equals(KasKecil.DISETUJU)) {
			setujui = true;
		}

		if (kasKecil.getDisposisiSop() != null && kasKecil.getDisposisiSop().getDisposisiSetuju() != null
				&& kasKecil.getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null
				&& kasKecil.getDisposisiSop().getDisposisiSetuju().getSelesai()) {
			viewOnly = true;
		}

		try {
			if (kasKecil.getSatuanKerja() == null) {
				kasKecil.setSatuanKerja(Common.getSatuanKerja());
			}
			tbmuser = Common.getCurrentUser();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/KasKecilAction.java:1204");
			// TODO: handle exception
		}

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja *"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(kasKecil.getSatuanKerja() == null ? "" : kasKecil.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", kasKecil.getSatuanKerja());
		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(kasKecil.getSatuanKerja() == null ? "" : kasKecil.getSatuanKerja().getNama()));
		} else {
			row.appendChild(satuanKerja);
		}
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		if (kasKecil.getKode() == null) {
			String noAgenda = generateCode(false);
			kasKecil.setKode(noAgenda);
		}

		kode = new Label(kasKecil.getKode());
		if (persetujuan) {
			row.appendChild(new Label(kasKecil.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Pengeluaran Kas Kecil *"));
		nama = new Textbox(kasKecil.getNama());

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(kasKecil.getNama()));
		} else {
			row.appendChild(nama);
		}

		nama.setWidth("90%");
		nama.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kas Kecil *"));
		jenisKasKecil = new Combobox();
		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();

		SatuanKerja parent = pt == null ? null : pt.getSatuanKerja();
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		jenisKasKecil.setWidth("90%");

		if (jenisKasKecil.getChildren().size() == 1 && kasKecil.getJenisKasKecil() == null) {
			jenisKasKecil.setSelectedIndex(0);
		}

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(
					new Label(kasKecil.getJenisKasKecil() == null ? "" : kasKecil.getJenisKasKecil().getNama()));
		} else {
			row.appendChild(jenisKasKecil);
		}

		saldo = new Label();
		final Label akun = new Label();
		final Label unit = new Label();

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				JenisKasKecil work = (JenisKasKecil) (jenisKasKecil.getSelectedItem() == null ? null
						: jenisKasKecil.getSelectedItem().getValue());

				kasKecil.setJenisKasKecil(work);
				kasKecil.setKode(kode.getValue());
				kasKecil.setNama(nama.getValue());
				kasKecil.setNilai(nilai);
				kasKecil.setKeterangan(keterangan.getValue());
				kasKecil.setTanggal(tanggal.getValue());
				kasKecil.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
				kasKecil.setFormula(array.toString());

				String sts = (String) (status.getSelectedItem() == null ? null : status.getSelectedItem().getValue());
				if (sts != null && sts.equals(DanaTalangan.DISETUJU)) {
					kasKecil.setDisetujuiOleh(tbmuser);
					kasKecil.setTanggalPersetujuan(WaktuUtil.getDate());
				} else {
					kasKecil.setDisetujuiOleh(null);
					kasKecil.setTanggalPersetujuan(null);
				}

				kasKecil.setStatus(sts);

				kasKecil.setSaldo(JenisKasKecilAction.hitungSaldo(kasKecil.getId(), kasKecil.getJenisKasKecil(),
						kasKecil.getTanggal()));

				if (kasKecil.getId() != null) {
					Common.refreshUpdate(kasKecil);
				}

				Double totalSaldo = kasKecil.getSaldo();

				unit.setValue(work == null || work.getSatuanKerja() == null ? "" : work.getSatuanKerja().getNama());

				akun.setValue(work == null || work.getAkun() == null ? ""
						: work.getAkun().getKode() + "-" + work.getAkun().getNama());

				saldo.setValue(Common.numberFormat.get().format(totalSaldo));

			}
		};

		EventListener eventListenerKas = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				SatuanKerja parent = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");

				if (parent != null) {

					Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
					if (parent != null) {
						satuanKerjas.clear();
						satuanKerjas.add(parent);
						satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
					}

					if (tbmuser != null && tbmuser.ambilSatuanKerja() != null) {
						satuanKerjas.add(tbmuser.ambilSatuanKerja());
					}

					Common.insertCombo(jenisKasKecil, new String[] { "kode", "nama", "keterangan" }, "akun",
							JenisKasKecil.class,

							Restrictions
									.and(Restrictions.eq("aktif", true),
											Restrictions.or(Restrictions.isNull("satuanKerja"),
													Restrictions.or(
															satuanKerjas.isEmpty()
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.or(
																			parent == null
																					? Restrictions.isNull("satuanKerja")
																					: Restrictions
																							.sqlRestriction("false"),
																			Restrictions.in("satuanKerja",
																					satuanKerjas)),
															Restrictions.eq("satuanKerja",
																	tbmuser.ambilSatuanKerja())))));

					Common.selectComboItem(true, jenisKasKecil, kasKecil.getJenisKasKecil());

					if (jenisKasKecil.getChildren().size() == 1 && kasKecil.getJenisKasKecil() == null) {
						jenisKasKecil.setSelectedIndex(0);
					}

				}

				eventListener.onEvent(arg0);

			}
		};

		satuanKerja.setEventListener(eventListenerKas);

		jenisKasKecil.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit"));
		row.appendChild(unit);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Saldo Kas Kecil"));
		row.appendChild(saldo);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Penerima"));
		row.appendChild(akun);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengajuan Kas Kecil *"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		tanggal = new MyDatebox(kasKecil.getTanggal());
		tanggal.setFormat(Common.dateFormat3.get().toPattern());
		if (persetujuan || setujui || viewOnly) {
			hbox.appendChild(new Label(Common.dateFormat6.get().format(kasKecil.getTanggal())));
		} else {
			tanggal.setParent(hbox);
		}

		jenisKasKecil.addEventListener("onChange", eventListener);

		nilaiHarusDikembalikan = new Label();
		nilaiHarusDikembalikan.setValue(Common.numberFormat.get().format(kasKecil.getSisa()));
		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bukti Pengeluaran"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, kasKecil.getId(), KasKecil.class.getName(), "Bukti Pengeluaran",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file bukti pengeluaran lebih dari satu file, zip dulu semua file tersebut");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		keterangan = new Textbox(kasKecil.getKeterangan() == null ? "" : kasKecil.getKeterangan());

		if (setujui) {
			row.appendChild(new Label(kasKecil.getKeterangan() == null ? "" : kasKecil.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(2);

		nilai = 0.0;
		rowtanggal = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowtanggal, "2");
		rowtanggal.setParent(rows);
		array = new JSONArray(kasKecil.getFormula());

		EventListener tanggalData = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowtanggal);
				rowFormula = Common.tampilanScroll1(rowtanggal);

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(tanggal.getValue());

				reloadFormula(rowFormula, array, persetujuan, setujui, viewOnly, calendar.get(Calendar.YEAR),
						new MyCheckboxConfigPilih(), nilaiHarusDikembalikan, saldo, edit);
			}
		};

		tanggal.addEventListener("onChange", tanggalData);
//		tampilkanAnggaran.addEventListener("onClick", tanggalData);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sisa Saldo Kas Kecil"));
		row.appendChild(nilaiHarusDikembalikan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Apakah penutupan kas kecil ?"));
		merupakanPenutupanKasKecil = new MyCheckboxConfig("Merupakan Penutupan Kas Kecil");
		if (Common.getApakahAdmin()) {
			row.appendChild(merupakanPenutupanKasKecil);
		} else if (persetujuan || setujui || viewOnly) {
			new MyLabelConfig(
					"Merupakan Penutupan Kas Kecil ? " + (kasKecil.getMerupakanPenutupanKasKecil() ? "Ya" : "Tidak"))
					.setParent(row);
		} else {
			row.appendChild(merupakanPenutupanKasKecil);
		}
		merupakanPenutupanKasKecil.setChecked(kasKecil.getMerupakanPenutupanKasKecil());

		if (kasKecil.getId() != null) {
			merupakanPenutupanKasKecil.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kasKecil.setMerupakanPenutupanKasKecil(merupakanPenutupanKasKecil.isChecked());
					Common.refreshUpdate(kasKecil);
				}
			});
		}

		row = new MyFormRow();
		row.setVisible(persetujuan && !viewOnly && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
		status = new Radiogroup();
		Radio comboitem = new Radio(UangMuka.PENGAJUAN);
		comboitem.setAttribute("value", UangMuka.PENGAJUAN);
		comboitem.setValue(UangMuka.PENGAJUAN);
		comboitem.setVisible(false);
		status.appendChild(comboitem);
		comboitem = new Radio(UangMuka.DISETUJU);
		comboitem.setAttribute("value", UangMuka.DISETUJU);
		comboitem.setValue(UangMuka.DISETUJU);
		status.appendChild(comboitem);
		comboitem = new Radio(UangMuka.DITOLAK);
		comboitem.setAttribute("value", UangMuka.DITOLAK);
		comboitem.setValue(UangMuka.DITOLAK);
		status.appendChild(comboitem);
		status.setWidth("90%");
		Common.selectRadioItem(status, kasKecil.getStatus());
		row.appendChild(status);

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("checkbox");
					if (selesai != null && selesai) {
						Common.selectRadioItem(status, UangMuka.DISETUJU);
						Common.freeze(status, true);
					} else {
						status.setSelectedItem(null);
						Common.freeze(status, false);
					}
				}
			}
		});

		if (setujuiData != null) {
			status.addEventListener("onClick", setujuiData);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setujuiData.onEvent(new Event("", null, kasKecil.getStatus().equals(UangMuka.DISETUJU)));
				}
			});
		}

		if (setujui) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
			row.appendChild(new ais.ui.util.MyLabelConfig(kasKecil.getStatus()));
		}

		eventListenerKas.onEvent(null);
		tanggalData.onEvent(null);

		EventListener s = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean setujui = status.getSelectedItem() == null ? false
						: status.getSelectedItem().getValue().equals(PenggantianKasKecil.DISETUJU);

				if (setujui) {
					save.setLabel("Selesaikan dan Setujui Kas Kecil");
				} else {
					save.setLabel(!persetujuan ? "Simpan dan Cetak" : "Ubah Status Persetujuan dan Cetak");
				}

				if (Common.getApakahAdmin()) {
					save.setVisible(true);
				}
			}
		};

		status.addEventListener("onClick", s);
		Common.createDefaultTimer(s);

		return grid;
	}

	/**
	 * Menambahkan tombol "Tambah Biaya" dan menginisialisasi tampilan rincian biaya pertama kali.
	 *
	 * <p><b>Tujuan:</b> Metode statis wrapper yang menyiapkan container untuk daftar item biaya.
	 * Menciptakan tombol "Tambah Biaya" di atas grid dan memanggil
	 * {@link #reloadDataFormula} untuk rendering pertama kali. Bersifat statis sehingga
	 * dapat dipakai oleh modul lain yang perlu menampilkan form kas kecil.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat {@link MyFormRow} baru ({@code rowU}) sebagai wadah grid rincian biaya.</li>
	 *   <li>Membuat tombol "Tambah Biaya" yang saat diklik menambahkan JSONObject baru
	 *       ke {@code array} dengan field default dan key unik acak, lalu memanggil
	 *       {@link #reloadDataFormula} untuk refresh.</li>
	 *   <li>Tombol tidak terlihat saat {@code persetujuan || setujui}.</li>
	 *   <li>Menambahkan {@code rowU} ke parent {@code rowFormula} dan memanggil
	 *       {@link #reloadDataFormula} untuk render isi awal.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Bersifat statis agar dapat dipanggil dari konteks yang berbeda
	 * (misalnya dari dialog eksternal yang menggunakan form kas kecil). Semua parameter
	 * yang dibutuhkan harus disuplai oleh pemanggil.</p>
	 *
	 * @param rowFormula           baris ZK tempat tombol Tambah dan grid diletakkan
	 * @param array                JSONArray yang dimodifikasi in-place saat tambah/hapus item
	 * @param persetujuan          {@code true} jika dalam mode persetujuan (tombol edit tersembunyi)
	 * @param setujui              {@code true} jika status sudah disetujui (semua input readonly)
	 * @param viewOnly             {@code true} untuk mode hanya baca penuh
	 * @param tahunWorkspace       tahun yang dipakai untuk filter workspace/anggaran
	 * @param tampilkanAnggaran    checkbox yang mengontrol kolom Anggaran (lebar 15% atau 0px)
	 * @param nilaiHarusDikembalikan label yang menampilkan sisa saldo = saldo - total biaya
	 * @param saldo                label yang menampilkan saldo kas kecil saat ini
	 * @param edit                 hak akses update (memengaruhi mode akun saat persetujuan)
	 * @throws Exception jika terjadi error pada pembuatan komponen ZK
	 */
	public static void reloadFormula(final Row rowFormula, final JSONArray array, final boolean persetujuan,
			final boolean setujui, final boolean viewOnly, final Integer tahunWorkspace,
			final Checkbox tampilkanAnggaran, final Label nilaiHarusDikembalikan, final Label saldo, final boolean edit)
			throws Exception {
		final MyFormRow rowU = new MyFormRow();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Biaya", "/img/svg/addthis.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(!persetujuan && !setujui);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("nama", "");
				jsonObject.put("qty", 0.0);
				jsonObject.put("harga", 0.0);
				jsonObject.put("jumlah", 0.0);
				Long key = Math.abs(Common.randLong());
				jsonObject.put("key", key);

				array.put(jsonObject);

				reloadDataFormula(rowU, array, persetujuan, setujui, viewOnly, tahunWorkspace, tampilkanAnggaran,
						nilaiHarusDikembalikan, saldo, edit);
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, array, persetujuan, setujui, viewOnly, tahunWorkspace, tampilkanAnggaran,
				nilaiHarusDikembalikan, saldo, edit);

	}

	/**
	 * Membangun ulang grid rincian biaya kas kecil dari JSONArray ke dalam Row ZK.
	 *
	 * <p><b>Tujuan:</b> Merender ulang seluruh tabel rincian biaya kas kecil dalam mode
	 * interaktif (input field) atau mode baca-saja (Label). Bersifat statis agar dapat
	 * dipanggil oleh modul lain dan oleh tombol hapus item (yang harus memanggil method ini
	 * untuk refresh setelah hapus).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membersihkan semua komponen anak {@code rowU} via {@link Common#clear(Row)}.</li>
	 *   <li>Membuat Grid baru dengan kolom: Keterangan Biaya, Anggaran (lebar 0 jika disembunyikan),
	 *       Akun, Tanggal, Qty, Harga, Jumlah, tombol hapus, dan aksi.</li>
	 *   <li>Membuat {@code hitungTotal} listener yang menjumlahkan semua jumlah dari JSONArray
	 *       dan memperbarui footer total, serta menghitung sisa saldo di {@code nilaiHarusDikembalikan}.</li>
	 *   <li>Untuk setiap item JSONObject dengan field {@code key} (skip item kosong):
	 *       <ul>
	 *         <li>Mencari atau memuat {@link ais.database.model.rab.Workspace} berdasarkan
	 *             akun dan tahun (query Hibernate sinkron).</li>
	 *         <li>Membuat komponen input: workspaceBanbox, akunBanbox, myTanggal, targetText,
	 *             qtyBox, hargaBox.</li>
	 *         <li>Mendaftarkan onChange/EventListener pada semua input untuk memperbarui
	 *             JSONObject dan memanggil {@code hitungTotal}.</li>
	 *         <li>Menambahkan widget upload lampiran per item.</li>
	 *         <li>Menambahkan tombol Hapus (mengganti item dengan JSONObject kosong dan reload).</li>
	 *       </ul>
	 *   </li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Catatan khusus mode persetujuan dengan edit=true:</b> Kolom akun menggunakan
	 * {@code akunBanbox} (input dapat diubah akun) meskipun dalam mode persetujuan. Ini
	 * memungkinkan penyetuju mengoreksi akun yang salah.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Urutan kolom dan cara penambahan komponen ke {@code rowD} harus
	 * sesuai dengan header kolom yang didefinisikan di awal metode. Query Workspace per item
	 * dilakukan sinkron; jika ada banyak item, ini bisa lambat.</p>
	 *
	 * @param rowU                 baris ZK target yang akan diisi grid; komponen lama dibersihkan
	 * @param array                JSONArray berisi item biaya; dimodifikasi in-place saat edit
	 * @param persetujuan          mode persetujuan (beberapa field read-only)
	 * @param setujui              sudah disetujui (semua field read-only)
	 * @param viewOnly             hanya tampil (semua field read-only)
	 * @param tahunWorkspace       tahun untuk filter workspace/anggaran
	 * @param tampilkanAnggaran    checkbox anggaran (memengaruhi lebar kolom Anggaran)
	 * @param nilaiHarusDikembalikan label sisa saldo yang diperbarui saat total berubah
	 * @param saldo                label saldo kas kecil saat ini (untuk hitung sisa)
	 * @param edit                 hak akses update (memungkinkan edit akun di mode persetujuan)
	 * @throws Exception jika error pada query Hibernate atau pembuatan komponen ZK
	 */
	public static void reloadDataFormula(final Row rowU, final JSONArray array, final boolean persetujuan,
			final boolean setujui, final boolean viewOnly, final Integer tahunWorkspace,
			final Checkbox tampilkanAnggaran, final Label nilaiHarusDikembalikan, final Label saldo, final boolean edit)
			throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Keterangan Biaya");
		column.setParent(columns);

		if (tampilkanAnggaran.isChecked()) {
			column.setWidth("15%");
		}

		column = new MyColumnConfig("Anggaran");
		column.setParent(columns);
		column.setWidth(tampilkanAnggaran.isChecked() ? "15%" : "0px");

		column = new MyColumnConfig("Akun");
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig("Tanggal");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("18%");

		column = new MyColumnConfig("Qty");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("8%");

		column = new MyColumnConfig("Harga");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("Jumlah");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Foot foot = new Foot();
		foot.setParent(grid);

		Footer footer = new Footer("Total");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		final Footer footerTotal = new Footer("");
		foot.appendChild(footerTotal);

		footer = new Footer("");
		foot.appendChild(footer);

		Rows rows = new Rows();
		rows.setParent(grid);

		final EventListener hitungTotal = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Double nilai = 0.0;
				for (int i = 0; i < array.length(); i++) {
					Double jumlah = 0.0;
					JSONObject jsonObject = array.getJSONObject(i);
					if (!jsonObject.isNull("jumlah")) {
						jumlah = jsonObject.getDouble("jumlah");
					}
					nilai += jumlah;
				}
				footerTotal.setLabel(Common.numberFormat.get().format(nilai));

				Double saldoData = 0.0;
				try {
					saldoData = Common.numberFormat.get().parse(saldo.getValue()).doubleValue();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/KasKecilAction.java:1836");
					// TODO: handle exception
				}

				Double dikembalikan = saldoData - nilai;
				nilaiHarusDikembalikan.setValue(Common.numberFormat.get().format(dikembalikan));

				if (nilaiHarusDikembalikan.getParent() != null) {
					if (saldoData.intValue() == 0) {
						nilaiHarusDikembalikan.getParent().setVisible(false);
					} else {
						nilaiHarusDikembalikan.getParent().setVisible(true);
					}
				}
			}

		};

		hitungTotal.onEvent(null);

		Session session = HibernateUtil.currentSession();

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
			}

			if (key != null) {

				Workspace workspace = (Workspace) (jsonObject.isNull("workspace") ? null
						: ConstantValues.ambil(Workspace.class.getName(),
								new BigDecimal(jsonObject.get("workspace") + "").longValue()));

				Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
						: ConstantValues.ambil(Akun.class.getName(),
								ais.common.CommonJSONUtil.ambilLong(jsonObject, "akun")));
				if (workspace == null && akunBiaya != null) {
					workspace = (Workspace) ConstantValues.simpleObject(
							session.createCriteria(Workspace.class)
									.add(Restrictions.or(Restrictions.eq("carryOver", true),
											Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true))))
									.add(Restrictions.eq("tahunWorkspace", tahunWorkspace))
									.add(Restrictions.eq("akun", akunBiaya)).addOrder(Order.desc("id"))
									.setMaxResults(1),
							Workspace.class);

					if (workspace == null || !workspace.getAktif()) {
						workspace = (Workspace) ConstantValues.simpleObject(session.createCriteria(Workspace.class)
								.add(Restrictions.or(Restrictions.eq("carryOver", true),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
								.add(Restrictions.eq("tahunWorkspace", tahunWorkspace))
								.add(Restrictions.eq("kode", akunBiaya.getKode())).addOrder(Order.desc("id"))
								.setMaxResults(1), Workspace.class);
					}

					System.out.println("workspace -> " + workspace + ", akunBiaya -> " + akunBiaya
							+ ", tahunWorkspace -> " + tahunWorkspace);
				}

				String nama = "";

				if (!jsonObject.isNull("nama")) {
					nama = jsonObject.get("nama") + "";
				}

				Double qty = 0.0;
				if (!jsonObject.isNull("qty")) {
					qty = jsonObject.getDouble("qty");
				}

				Double harga = 0.0;
				if (!jsonObject.isNull("harga")) {
					harga = jsonObject.getDouble("harga");
				}

				Double jumlah = 0.0;
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}

				Date tanggal = WaktuUtil.getDate();
				if (!jsonObject.isNull("tanggal")) {
					tanggal = Common.dateFormat9.get().parse(jsonObject.getString("tanggal"));
				}

				String nama_file = "";

				if (!jsonObject.isNull("nama_file")) {
					nama_file = jsonObject.get("nama_file") + "";
				}

				String link = "";

				if (!jsonObject.isNull("link")) {
					link = jsonObject.get("link") + "";
				}

				MyFormRow rowD = new MyFormRow();
				rowD.setValign("top");
				rowD.setParent(rows);

				final AmbilDataWorkspaceBanbox workspaceBanbox = new AmbilDataWorkspaceBanbox(false);
				workspaceBanbox.setAttribute("workspace", workspace);
				workspaceBanbox.setValue(workspace == null ? "" : workspace.toString());
				workspaceBanbox.setWidth("95%");
				workspaceBanbox.setReadonly(true);

				workspaceBanbox.setAttribute("janganDisabled", true);

				String dimulai = Common.getKonfigurasi("kas_kecil_dimulai", "").getNilai().trim().isEmpty() ? null
						: Common.getKonfigurasi("kas_kecil_dimulai", "").getNilai().trim();

				final AmbilDataAkunBanbox akunBanbox = new AmbilDataAkunBanbox(false, dimulai);
				akunBanbox.setAttribute("akun", akunBiaya);
				akunBanbox.setValue(akunBiaya == null ? "" : akunBiaya.toString());
				akunBanbox.setWidth("95%");
				akunBanbox.setAttribute("janganDisabled", true);

				final Label nilai = new Label(Common.numberFormat.get().format(jumlah));

				final MyTextbox targetText = new MyTextbox(nama);

				final MyDatebox myTanggal = new MyDatebox(tanggal);
				myTanggal.setFormat(Common.dateFormat3.get().toPattern());
				myTanggal.setWidth("95%");

				Vbox myvbox = new Vbox();
				myvbox.setParent(rowD);
				myvbox.setWidth("95%");

				final MyDoublebox qtyBox = new MyDoublebox(qty);
				final MyDoublebox hargaBox = new MyDoublebox(harga);

				targetText.setWidth("95%");
				qtyBox.setWidth("95%");
				hargaBox.setWidth("95%");

				if (persetujuan || setujui || viewOnly) {

					if (edit) {
						rowD.appendChild(new Label(workspace == null ? "" : workspace.getNama()));
						rowD.appendChild(akunBanbox);
					} else if (persetujuan) {
						rowD.appendChild(new Label(workspace == null ? "" : workspace.getNama()));
						rowD.appendChild(
								new Label(akunBiaya == null ? "" : akunBiaya.getKode() + "-" + akunBiaya.getNama()));
					} else {
						rowD.appendChild(workspaceBanbox);
						rowD.appendChild(akunBanbox);
					}
					rowD.appendChild(new Label(Common.dateFormat3.get().format(tanggal)));
					myvbox.appendChild(new Label(nama));
					rowD.appendChild(new Label(Common.numberFormat.get().format(qty)));
					rowD.appendChild(new Label(Common.numberFormat.get().format(harga)));

				} else {
					rowD.appendChild(workspaceBanbox);
					rowD.appendChild(akunBanbox);
					rowD.appendChild(myTanggal);
					myvbox.appendChild(targetText);
					rowD.appendChild(qtyBox);
					rowD.appendChild(hargaBox);
				}

				Long id_file = null;

				if (!jsonObject.isNull("id_file")) {
					id_file = Long.parseLong(jsonObject.get("id_file") + "");
				}

				final LampiranLain lampiranLain = id_file != null ? LampiranLain.ambil(true, id_file, "id")
						: LampiranLain.ambil(key, "Dokumen Kas Kecil");

				if (lampiranLain != null) {

					A a = new A(lampiranLain.getNama());
					a.setParent(myvbox);
					a.setWidth("95%");

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.display(lampiranLain);
						}
					});

				}

				else if (!nama_file.isEmpty() && !link.isEmpty()) {

					A a = new A(nama_file);
					a.setParent(myvbox);
					a.setWidth("95%");
					final String url = link;
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Clients.evalJavaScript(
									"popupCenter({url: '" + url + "', title: 'Data', w: 1200, h: 600});");
						}
					});

				} else {

					Hbox hbox = new Hbox();
					hbox.setParent(myvbox);
					LampiranLain.createDownloadUploadFileLain(hbox, key, "Dokumen Kas Kecil", "Bukti", false,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									LampiranLain lampiranLain = (LampiranLain) arg0.getData();
									jsonObject.put("link", lampiranLain.createLinkUri(false));
									jsonObject.put("nama_file", lampiranLain.getNama());
									jsonObject.put("id_file", lampiranLain.getId());
								}
							}, null, false, false, false, !(persetujuan || setujui || viewOnly));
				}

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Workspace workspace = (Workspace) (jsonObject.isNull("workspace") ? null
								: ConstantValues.ambil(Workspace.class.getName(),
										new BigDecimal(jsonObject.get("workspace") + "").longValue()));
						Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
								: ConstantValues.ambil(Akun.class.getName(),
										ais.common.CommonJSONUtil.ambilLong(jsonObject, "akun")));

						Workspace workspaceTemp = null;
						Akun akunTemp = null;
						if (arg0 != null && arg0.getData() instanceof Workspace) {
							workspaceTemp = (Workspace) arg0.getData();
						} else if (arg0 != null && arg0.getData() instanceof Akun) {
							akunTemp = (Akun) arg0.getData();
						}

						System.out.println("workspaceTemp -> " + workspaceTemp + ", akunTemp -> " + akunTemp);

						if (workspaceTemp != null) {
							workspace = workspaceTemp;
							akunBiaya = workspaceTemp.getAkun();
						}
						if (akunTemp != null) {
							akunBiaya = akunTemp;
							workspace = null;
						}

						if (workspace == null && akunBiaya != null) {
							Session session = HibernateUtil.currentSession();
							workspace = (Workspace) ConstantValues.simpleObject(session.createCriteria(Workspace.class)
									.add(Restrictions.or(Restrictions.eq("carryOver", true),
											Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true))))
									.add(Restrictions.eq("tahunWorkspace", tahunWorkspace))
									.add(Restrictions.eq("akun", akunBiaya)).addOrder(Order.desc("id"))
									.setMaxResults(1), Workspace.class);

							if (workspace == null || !workspace.getAktif()) {
								workspace = (Workspace) ConstantValues
										.simpleObject(session.createCriteria(Workspace.class)
												.add(Restrictions.or(Restrictions.eq("carryOver", true),
														Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true))))
												.add(Restrictions.eq("tahunWorkspace", tahunWorkspace))
												.add(Restrictions.eq("kode", akunBiaya.getKode()))
												.addOrder(Order.desc("id")).setMaxResults(1), Workspace.class);
							}

							System.out.println("workspace -> " + workspace + ", akunBiaya -> " + akunBiaya
									+ ", tahunWorkspace -> " + tahunWorkspace);
						}

						BigDecimal bigDecimal = workspace == null ? null : new BigDecimal(workspace.getId());

						jsonObject.put("workspace", workspace == null ? null : bigDecimal.toString());
						jsonObject.put("akun", akunBiaya == null ? null : akunBiaya.getId());
						jsonObject.put("nama", targetText.getValue());
						jsonObject.put("qty", qtyBox.getValue());
						jsonObject.put("harga", hargaBox.getValue());
						jsonObject.put("tanggal", Common.dateFormat9.get()
								.format(myTanggal.getValue() == null ? WaktuUtil.getDate() : myTanggal.getValue()));

						Double jumlah = (qtyBox.getValue() == null ? 0.0 : qtyBox.getValue())
								* (hargaBox.getValue() == null ? 0.0 : hargaBox.getValue());
						jsonObject.put("jumlah", jumlah);
						nilai.setValue(Common.numberFormat.get().format(jumlah));

						hitungTotal.onEvent(null);

						workspaceBanbox.setAttribute("workspace", workspace);
						workspaceBanbox.setValue(workspace == null ? "" : workspace.toString());
						akunBanbox.setAttribute("akun", akunBiaya);
						akunBanbox.setValue(akunBiaya == null ? "" : akunBiaya.toString());

					}
				};

				targetText.setRows(2);

				workspaceBanbox.setEventListener(eventListener);
				akunBanbox.setEventListener(eventListener);
				qtyBox.addEventListener("onChange", eventListener);
				targetText.addEventListener("onChange", eventListener);
				hargaBox.addEventListener("onChange", eventListener);
				myTanggal.addEventListener("onChange", eventListener);
				nilai.setParent(rowD);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												array.put(index, new JSONObject());

												reloadDataFormula(rowU, array, persetujuan, setujui, viewOnly,
														tahunWorkspace, tampilkanAnggaran, nilaiHarusDikembalikan,
														saldo, edit);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(
														"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																+ e.getMessage());
											}

										}

									}
								});

					}
				});

				if (persetujuan || setujui || viewOnly) {
					new Label().setParent(rowD);
				} else {
					button.setParent(rowD);
				}
			}
		}
	}

	/**
	 * Menyiapkan window form modal untuk pengajuan atau edit pengeluaran kas kecil.
	 *
	 * <p><b>Tujuan:</b> Metode private inti yang membangun window modal dengan layout
	 * Borderlayout (Center untuk form, South untuk toolbar tombol). Dipanggil dari
	 * {@link #onAdd(Event)} (entitas baru) dan {@link #init(GeneralValueObject)} (yang ada).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mengatur judul window "Pengeluaran Kas Kecil".</li>
	 *   <li>Menetapkan {@code dibuatOleh} dan {@code tanggalPembuatan} jika entitas baru.</li>
	 *   <li>Membersihkan window dan membuat Borderlayout baru.</li>
	 *   <li>Di Center: memanggil {@link #form} untuk membangun grid form.</li>
	 *   <li>Di South toolbar: tombol Batal (tutup window) dan Simpan (panggil {@link #onSave},
	 *       tutup window, lalu refresh grid via timer).</li>
	 *   <li>Jika status sudah disetujui ({@code setujui=true}): sembunyikan Simpan,
	 *       ubah Batal menjadi "Tutup".</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Perubahan tata letak window dilakukan di sini. Perubahan konten
	 * form dilakukan di {@link #form}. Entry point resmi adalah {@link #onAdd(Event)} dan
	 * {@link #init(GeneralValueObject)}.</p>
	 *
	 * @param kasKecil entitas yang ditampilkan; jika baru (id==null), pembuat dan tanggal
	 *                 diisi otomatis
	 * @throws Exception jika terjadi error pada pembuatan komponen ZK atau form
	 */
	private void init(final KasKecil kasKecil) throws Exception {
		addWindow.setTitle("Pengeluaran Kas Kecil");

		if (kasKecil.getDibuatOleh() == null) {
			kasKecil.setDibuatOleh(tbmuser);
			kasKecil.setTanggalPembuatan(new Date());
		}

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan dan Cetak", "/img/save.gif");

		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		disposisiSop = null;
		center.appendChild(form(kasKecil, disposisiSop, save, null));
		ais.ui.util.ZkCompat.setFlex(center, true);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {

					addWindow.setVisible(false);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							onSearchDefault(null);
						}
					});
				}
			}
		});
		save.setParent(toolbar);

		if (!persetujuan && setujui) {
			save.setVisible(false);
			cancel.setLabel("Tutup");
		}

	}

	/**
	 * Memvalidasi input, menyimpan entitas kas kecil, dan memicu cetak laporan otomatis.
	 *
	 * <p><b>Tujuan:</b> Event handler utama untuk tombol "Simpan dan Cetak" / "Selesaikan
	 * dan Setujui Kas Kecil". Melakukan validasi komprehensif, menyimpan ke database,
	 * menangani upload lampiran, dan memicu cetak laporan PDF dengan delay.</p>
	 *
	 * <p><b>Cara kerja — validasi:</b>
	 * <ol>
	 *   <li>Memeriksa satuan kerja terpilih (wajib isi).</li>
	 *   <li>Memvalidasi setiap item biaya: akun tidak boleh null, jumlah tidak boleh 0.</li>
	 *   <li>Memeriksa nama pengajuan tidak kosong.</li>
	 *   <li>Memeriksa jenis kas kecil terpilih.</li>
	 *   <li>Memeriksa tanggal diisi.</li>
	 *   <li>Memvalidasi saldo kas kecil: total biaya tidak boleh melebihi saldo tersedia.</li>
	 *   <li>Untuk kas kecil baru: memastikan tidak ada kas kecil lain yang belum disetujui
	 *       untuk jenis kas kecil yang sama (mencegah double pengajuan).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Cara kerja — penyimpanan:</b>
	 * <ol>
	 *   <li>Menetapkan semua field ke entitas dari komponen UI.</li>
	 *   <li>Mengatur status persetujuan ({@code disetujuiOleh}, {@code tanggalPersetujuan}).</li>
	 *   <li>Melakukan update (jika ada ID) atau save baru via Hibernate session.</li>
	 *   <li>Menangani update ref {@link LampiranLain} bukti pengeluaran menggunakan
	 *       StreamingHibernateUtil (sesi BLOB terpisah).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Cara kerja — pasca simpan:</b>
	 * <ol>
	 *   <li>Memicu cetak laporan dengan delay 2500ms via timer.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Seluruh operasi penyimpanan dibungkus dalam try-catch; error
	 * ditampilkan ke admin. Metode tetap mengembalikan {@code true} setelah catch (jika
	 * cetak timer sudah terjadwal), sehingga window akan ditutup. Pertimbangkan untuk
	 * mengembalikan {@code false} jika penyimpanan gagal.</p>
	 *
	 * @param event event ZK dari klik tombol simpan; dapat diabaikan
	 * @return {@code true} jika proses selesai (berhasil atau gagal dengan tangkapan exception)
	 *         dan window boleh ditutup; {@code false} jika validasi gagal
	 * @throws Exception jarang terjadi; sebagian besar exception ditangkap secara internal
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		try {
			if (satuanKerja.getAttribute("satuanKerja") == null) {
				MyMessageboxConfig.show("Mohon maaf, Satuan Kerja belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Satuan Kerja dari field pencarian yang tersedia; (2) Pastikan data Satuan Kerja sudah tercatat di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}

			kasKecil.setFormula(array.toString());
			JSONArray array = new JSONArray(kasKecil.getFormula());
			for (int i = 0; i < array.length(); i++) {

				JSONObject jsonObject = array.getJSONObject(i);
				Long key = null;
				if (!jsonObject.isNull("key")) {
					key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
				}

				if (key != null) {
					Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
							: ConstantValues.ambil(Akun.class.getName(),
									ais.common.CommonJSONUtil.ambilLong(jsonObject, "akun")));

					Double jumlah = 0.0;
					if (!jsonObject.isNull("jumlah")) {
						jumlah = jsonObject.getDouble("jumlah");
					}

					if (akunBiaya == null) {
						MyMessageboxConfig.show("Mohon maaf, ada akun biaya pengeluaran yang belum dipilih. Langkah yang dapat dilakukan: (1) Periksa setiap baris rincian biaya dan lengkapi kolom Akun yang masih kosong; (2) Pilih akun yang sesuai melalui field pencarian akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return false;
					}

					if (jumlah.intValue() == 0) {
						MyMessageboxConfig.show("Mohon maaf, ada nilai biaya pengeluaran yang masih nol. Langkah yang dapat dilakukan: (1) Periksa setiap baris rincian biaya dan isikan nominal yang valid; (2) Pastikan semua nilai biaya lebih dari nol; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return false;
					}
				}
			}

			nilai = 0.0;
			for (int i = 0; i < array.length(); i++) {
				Double jumlah = 0.0;

				JSONObject jsonObject = array.getJSONObject(i);
				Long key = null;
				if (!jsonObject.isNull("key")) {
					key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
				}

				if (key != null) {
					if (!jsonObject.isNull("jumlah")) {
						jumlah = jsonObject.getDouble("jumlah");
					}
					nilai += jumlah;
				}
			}

			System.out.println("array -> " + array);

			JenisKasKecil work = (JenisKasKecil) (jenisKasKecil.getSelectedItem() == null ? null
					: jenisKasKecil.getSelectedItem().getValue());

			if (nama.getValue().trim().equals("")) {
				MyMessageboxConfig.show("Mohon maaf, Judul Pengeluaran Kas Kecil belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Judul Pengeluaran dengan deskripsi singkat yang jelas; (2) Pastikan judul tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (work == null) {
				MyMessageboxConfig.show("Mohon maaf, Kas Kecil belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Jenis Kas Kecil dari dropdown yang tersedia; (2) Pastikan data kas kecil sudah terdaftar di master Jenis Kas Kecil; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (tanggal.getValue() == null) {
				MyMessageboxConfig.show("Mohon maaf, Tanggal Laporan belum diisi. Langkah yang dapat dilakukan: (1) Isikan atau pilih Tanggal Laporan menggunakan date picker; (2) Pastikan tanggal yang dipilih valid sesuai periode berjalan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}

			JenisKasKecil kecil = (JenisKasKecil) (jenisKasKecil.getSelectedItem() == null ? null
					: jenisKasKecil.getSelectedItem().getValue());

			if (kecil == null) {
				MyMessageboxConfig.show("Mohon maaf, Jenis Kas Kecil belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Jenis Kas Kecil dari dropdown yang tersedia; (2) Pastikan jenis kas kecil yang dibutuhkan sudah terdaftar di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}

			Double saldo = JenisKasKecilAction.hitungSaldo(kasKecil.getId(), kecil, tanggal.getValue());

			System.out.println("saldo -> " + saldo + ", nilai -> " + nilai);

			if (saldo < nilai) {
				MyMessageboxConfig.show("Mohon maaf, nilai pengeluaran kas kecil melebihi saldo yang tersedia. Langkah yang dapat dilakukan: (1) Kurangi nilai pengeluaran agar tidak melebihi saldo kas kecil saat ini; (2) Lakukan pengisian ulang kas kecil terlebih dahulu jika saldo tidak mencukupi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			Session session = HibernateUtil.currentSession();

			if (kasKecil.getId() == null) {
				List<String> kasKecil = session.createCriteria(KasKecil.class)
						.add(Restrictions.ne("status", KasKecil.DITOLAK)).setProjection(Projections.property("kode"))
						.add(Restrictions.eq("jenisKasKecil", kecil)).add(Restrictions.isNull("disetujuiOleh")).list();

				System.out.println("1. count -> " + kasKecil);

				if (!kasKecil.isEmpty()) {

					String s = "";
					for (String kode : kasKecil) {
						s += s.isEmpty() ? kode : "; " + kode;
					}

					MyMessageboxConfig.showFormat(
							"Mohon maaf, untuk \"{V1}\" masih terdapat dokumen kas kecil yang belum disetujui, antara lain: {V2}. Langkah yang dapat dilakukan: (1) buka dokumen kas kecil yang bersangkutan; (2) mintakan persetujuan kepada pihak yang berwenang; (3) setelah seluruh dokumen disetujui, silakan ulangi proses ini.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							kecil.getNama(), s);
					return false;
				}

			}

			if (kasKecil.getId() != null) {
				kasKecil = (KasKecil) session.load(KasKecil.class, kasKecil.getId());
			}

			if (kasKecil.getDibuatOleh() == null) {
				kasKecil.setDibuatOleh(tbmuser);
				kasKecil.setTanggalPembuatan(new Date());
			}
			if (disposisiSop != null && disposisiSop.getId() != null) {
				kasKecil.setDisposisiSop(disposisiSop);
			}

			kasKecil.setJenisKasKecil(kecil);
			kasKecil.setKode(kode.getValue());
			kasKecil.setNama(nama.getValue());
			kasKecil.setNilai(nilai);
			kasKecil.setKeterangan(keterangan.getValue());
			kasKecil.setTanggal(tanggal.getValue());
			kasKecil.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
			kasKecil.setFormula(array.toString());
			kasKecil.setSaldo(saldo);
			kasKecil.setMerupakanPenutupanKasKecil(merupakanPenutupanKasKecil.isChecked());

			String sts = (String) (status.getSelectedItem() == null ? null : status.getSelectedItem().getValue());
			if (sts != null && sts.equals(DanaTalangan.DISETUJU)) {
				kasKecil.setDisetujuiOleh(tbmuser);
				kasKecil.setTanggalPersetujuan(WaktuUtil.getDate());
			} else {
				kasKecil.setDisetujuiOleh(null);
				kasKecil.setTanggalPersetujuan(null);
			}

			kasKecil.setStatus(sts);

			System.out.println("sts -> " + sts);

			if (kasKecil.getId() != null) {
				Common.refreshUpdate(session, kasKecil);
			} else {
				kasKecil.setDibuatOleh(tbmuser);
				String noAgenda = generateCode(true);
				kode.setValue(noAgenda);
				kasKecil.setKode(kode.getValue());
				session.save(kasKecil);
			}

			session.flush();

			System.out.println("kasKecil -> " + kasKecil);

			if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(lainMahasiswa);
					lainMahasiswa.setRef(kasKecil.getId());

					session.getTransaction().begin();
					session.update(lainMahasiswa);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				cetak(KasKecilAction.this.kasKecil);
			}
		}, "Proses cetak", false, 2500);

		return true;
	}

	/**
	 * Membangun objek Criteria Hibernate untuk query daftar pengeluaran kas kecil.
	 *
	 * <p><b>Tujuan:</b> Implementasi {@link DataCriteria} yang membangun query Hibernate
	 * berdasarkan filter aktif di form pencarian. Digunakan oleh {@link #onSearchDefault(Event)}
	 * dan tombol ekspor data. Mendukung filter multi-dimensi termasuk filter jenis kas kecil.</p>
	 *
	 * <p><b>Cara kerja — filter yang diterapkan:</b>
	 * <ol>
	 *   <li><b>Satuan Kerja:</b> Filter hierarki satuan kerja via {@code SatuanKerjaTreeModel}.</li>
	 *   <li><b>Tanggal:</b> {@code date(tanggal_pembuatan) BETWEEN start AND end}; diabaikan
	 *       jika komponen tanggal null.</li>
	 *   <li><b>Status:</b> Eq status dari combobox (PENGAJUAN/DISETUJU/DITOLAK/Semua).</li>
	 *   <li><b>Aktif:</b> Filter aktif/semua berdasarkan checkbox.</li>
	 *   <li><b>Kode:</b> ILIKE anywhere pada field kode.</li>
	 *   <li><b>Nama:</b> ILIKE anywhere pada field nama.</li>
	 *   <li><b>Jenis Kas Kecil:</b> Jika {@code serachjenis} terisi, join ke jenisKasKecil
	 *       dan filter ILIKE pada nama atau kode jenis kas kecil.</li>
	 *   <li><b>Urutan:</b> ORDER BY id DESC jika {@code order=true}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Filter jenis kas kecil menggunakan join alias yang berbeda dari
	 * filter lain. Jika ada filter baru yang membutuhkan join, pastikan tidak bentrok dengan
	 * alias yang sudah ada.</p>
	 *
	 * @param order {@code true} untuk menambahkan ORDER BY id DESC; {@code false} untuk paging
	 * @return {@link Criteria} Hibernate siap execute, atau {@code null} jika {@code searchparent}
	 *         belum tersedia
	 */
	public Criteria initCriteria(boolean order) {
		if (searchparent == null) return null;

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KasKecil.class)

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(serachkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", serachkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(serachnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", serachnama.getValue().trim(), MatchMode.ANYWHERE));

		if (serachjenis != null && !serachjenis.getValue().trim().isEmpty()) {
			criteria.createAlias("jenisKasKecil", "jenisKasKecil")
					.add(serachjenis.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.ilike("jenisKasKecil.nama", serachjenis.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("jenisKasKecil.kode", serachjenis.getValue().trim(),
											MatchMode.ANYWHERE)));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));
		return criteria;
	}

	/**
	 * Menyegarkan grid daftar kas kecil dengan data terkini dari database.
	 *
	 * <p><b>Tujuan:</b> Implementasi {@link DataSearchDefault} yang dipanggil saat pengguna
	 * menekan tombol cari, mengubah filter, setelah simpan/hapus, atau saat inisialisasi halaman.
	 * Memperbarui paging dan mengisi ulang grid dengan data sesuai filter aktif.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@link Common#initPaging} dengan {@code initCriteria(false)} untuk
	 *       menghitung total data dan memperbarui paging.</li>
	 *   <li>Mengeksekusi query dengan {@code initCriteria(true)} (ORDER BY id DESC),
	 *       dibatasi {@link Common#ROWS_COUNT_ON_PAGE} baris, dioffset sesuai halaman aktif.</li>
	 *   <li>Menetapkan {@link KasKecilRenderer} sebagai renderer dan model ke grid.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Dipanggil dari banyak titik termasuk timer inisialisasi, listener
	 * paging, dan callback setelah simpan. Setiap pemanggilan memicu rendering ulang semua baris
	 * termasuk kalkulasi saldo per baris — dapat lambat jika banyak baris per halaman.</p>
	 *
	 * @param event event ZK pemicu; bisa null untuk refresh programatik
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KasKecil> kasKecil = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(kasKecil);
		grid.setRowRenderer(new KasKecilRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Mengembalikan istilah/nama modul ini untuk keperluan alur SOP.
	 *
	 * <p><b>Tujuan:</b> Implementasi {@link FormSop} yang menyediakan nama human-readable
	 * dari modul ini. Digunakan oleh sistem SOP untuk menampilkan nama modul di alur disposisi,
	 * notifikasi, dan log SOP.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika nama modul berubah, ubah string kembalian di sini.</p>
	 *
	 * @return string "Pengeluaran Kas Kecil" sebagai nama modul
	 * @throws Exception tidak akan dilempar; deklarasi ada karena kontrak antarmuka
	 */
	public String istilah() throws Exception {
		return "Pengeluaran Kas Kecil";
	}

	/**
	 * Mengembalikan entitas kas kecil yang sedang aktif di form.
	 *
	 * <p><b>Tujuan:</b> Implementasi {@link FormSop} yang memberikan akses ke entitas saat ini
	 * kepada sistem SOP untuk menyimpan referensi ke disposisi.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Field {@code kasKecil} diperbarui di {@link #form} dan
	 * {@link #init(KasKecil)}. Pastikan terkini sebelum sistem SOP memanggil ini.</p>
	 *
	 * @return entitas {@link ais.database.model.akunting.KasKecil} yang aktif; bisa null
	 * @throws Exception tidak akan dilempar
	 */
	@Override
	public DataSop ambil() throws Exception {
		return kasKecil;
	}

	/**
	 * Mengembalikan kelas entitas yang dikelola oleh controller ini.
	 *
	 * <p><b>Tujuan:</b> Implementasi {@link FormSop} yang memberikan informasi tipe kelas
	 * kepada framework SOP dan ekspor data generik.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jangan ubah nilai kembalian ini.</p>
	 *
	 * @return {@code KasKecil.class}
	 * @throws Exception tidak akan dilempar
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return KasKecil.class;
	}

	/**
	 * Menghasilkan kode unik untuk kas kecil baru berdasarkan konfigurasi nomor surat.
	 *
	 * <p><b>Tujuan:</b> Membuat kode dokumen terformat dari konfigurasi
	 * {@link NomorSuratAlurKeuangan#KAS_KECIL_DATA}. Kode ini menggabungkan prefix, tahun/bulan,
	 * dan nomor urut menjadi string seperti "KK/VI/2026/001".</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika konfigurasi nomor surat tidak tersedia, mengembalikan barcode acak.</li>
	 *   <li>Menentukan index: dari {@code nomorIndex} (sequential) atau dari
	 *       {@link #getindex(NomorSurat)} (hitungRowCount).</li>
	 *   <li>Jika {@code tambah=true}, menaikkan counter sequence.</li>
	 *   <li>Memformat kode dan memastikan keunikan via {@link ais.action.master.KodeUnikUtil}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Parameter {@code tambah=false} untuk preview, {@code tambah=true}
	 * hanya saat simpan definitif. Berbeda dari {@code PertangungjawabanKasBesarAction} yang
	 * menerima parameter {@code SatuanKerja}, metode ini tidak menerima satuan kerja karena
	 * format kode kas kecil tidak menggunakan variabel satuan kerja.</p>
	 *
	 * @param tambah {@code true} untuk menaikkan counter sequence (saat simpan);
	 *               {@code false} untuk preview
	 * @return kode unik yang terformat dan dipastikan belum digunakan
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurKeuangan.KAS_KECIL_DATA == null
				|| NomorSuratAlurKeuangan.KAS_KECIL_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurKeuangan.KAS_KECIL_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurKeuangan.KAS_KECIL_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurKeuangan.KAS_KECIL_DATA.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurKeuangan.KAS_KECIL_DATA.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurKeuangan.KAS_KECIL_DATA.getNomorSurat().format(index, WaktuUtil.getDate());
		return ais.action.master.KodeUnikUtil.pastikanUnik(KasKecil.class, noAgenda);
	}

	/**
	 * Menghitung nomor urut (index) berikutnya dari jumlah data di database untuk kode kas kecil.
	 *
	 * <p><b>Tujuan:</b> Menghitung index numerik berdasarkan rowCount data yang ada, dengan
	 * filter berdasarkan konfigurasi reset urutan ({@code resetUrutanTiapTahun},
	 * {@code resetUrutanTiapBulan}, {@code resetTiap}) dari konfigurasi nomor surat.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code nomorSurat} null, mengembalikan 0.</li>
	 *   <li>Membangun Criteria pada {@link ais.database.model.akunting.KasKecil} dengan
	 *       join ke {@code nomorSuratAlurKeuangan} dan {@code nomorSurat}.</li>
	 *   <li>Menerapkan filter reset periodik sesuai konfigurasi NomorSurat.</li>
	 *   <li>Mengeksekusi rowCount projection dan mengembalikan count + 1.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Rentan race condition; dimitigasi oleh
	 * {@link ais.action.master.KodeUnikUtil#pastikanUnik} di {@link #generateCode}.</p>
	 *
	 * @param nomorSurat konfigurasi nomor surat; jika null mengembalikan 0
	 * @return nomor urut berikutnya (rowCount + 1)
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(KasKecil.class)
				.createAlias("nomorSuratAlurKeuangan", "nomorSuratAlurKeuangan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurKeuangan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurKeuangan.nomorSurat", nomorSurat)

						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))

				.add(nomorSurat.getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetTiap() != null && (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang)) || nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	/**
	 * Menetapkan mode persetujuan secara programatik setelah konstruksi objek.
	 *
	 * <p><b>Tujuan:</b> Implementasi {@link FormSop} yang memungkinkan sistem SOP
	 * mengubah mode setelah instansiasi. Dipakai oleh framework SOP yang menginstansiasi
	 * controller secara generik.</p>
	 *
	 * <p><b>Cara kerja:</b> Menetapkan {@code this.persetujuan} ke nilai yang diberikan.
	 * Harus dipanggil sebelum {@link #form} atau {@link #init(KasKecil)} agar mode yang
	 * benar diterapkan saat membangun UI.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Konsisten dengan konstruktor berparameter
	 * {@link #KasKecilAction(boolean)}.</p>
	 *
	 * @param persetujuan {@code true} untuk mode persetujuan; {@code false} untuk pengajuan
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}
}
