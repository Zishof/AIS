package ais.action.master.akunting;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
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
import ais.action.master.akunting.helper.RevisiUangMukaHelper;
import ais.action.master.asset.PenerimaanPengadaanMasterAssetAction;
import ais.action.master.asset.PermintaanPengadaanMasterAssetAction;
import ais.action.master.asset.helper.AmbilDataPermintaanPengadaanMasterAssetBanyak;
import ais.action.master.dashboard.akunting.DasboardUangMuka;
import ais.action.master.akunting.helper.MonitorUangMukaDashboard;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.akunting.LaporanUangMuka;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.JenisUangMuka;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
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
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h3>UangMukaAction — Pengelola Pengajuan Uang Muka</h3>
 *
 * <p><strong>Untuk apa:</strong><br>
 * Kelas ini merupakan Action (komposer ZK) yang mengelola seluruh siklus hidup
 * dokumen uang muka (advance payment) dalam sistem keuangan eCampus. Uang muka
 * adalah dana yang diajukan oleh staf atau unit kerja untuk membiayai kegiatan
 * yang akan datang sebelum realisasi anggaran dilakukan. Melalui halaman ini,
 * pengguna dapat membuat pengajuan uang muka baru, memantau status pencairan,
 * mengaitkan dengan permintaan pengadaan (PR), dan melihat pertanggungjawaban
 * yang terkait.</p>
 *
 * <p><strong>Cara kerja:</strong><br>
 * Kelas ini mewarisi {@code GenericAutowireComposer} dari ZK Framework 5.5 sehingga
 * komponen UI yang dideklarasikan di file ZUL secara otomatis di-wire ke field Java
 * berdasarkan nama komponen. Alur kerja utama:</p>
 * <ol>
 *   <li>Pengguna membuka halaman; {@code doAfterCompose} dipanggil, menginisialisasi
 *       filter (combo status, datebox rentang tanggal, checkbox LPJ/DPC/BAST, banbox
 *       satuan kerja dan anggaran), tombol aksi toolbar, dan timer pencarian otomatis.</li>
 *   <li>Pencarian dilakukan via {@code onSearchDefault} yang memanggil
 *       {@code initCriteria} dan menampilkan hasilnya di {@code MyGrid} dengan
 *       renderer {@code UangMukaRenderer}.</li>
 *   <li>Renderer menampilkan setiap baris dengan: kode, nama, link proses transfer,
 *       link workspace anggaran, link pertanggungjawaban terkait, link BAST aset,
 *       saldo, nilai, sisa, periode, status persetujuan, dan tombol CRUD/cetak.</li>
 *   <li>Form tambah/ubah dibuka via {@code init} yang memanggil {@code form} untuk
 *       membangun MyGrid form dinamis. Form mendukung tiga mode: uang muka biasa
 *       (dari anggaran workspace), uang muka tanpa anggaran (flag {@code tanpaAnggaran}),
 *       dan uang muka dari permintaan pengadaan/PR (flag {@code ambilDariPr}).</li>
 *   <li>Penyimpanan dilakukan di {@code onSave} dengan validasi lengkap termasuk
 *       validasi saldo anggaran yang cukup (bila konfigurasi aktif), persist ke
 *       Hibernate, update relasi PR-detail ke uang muka, buat DaftarPengajuanTransfer
 *       bila disetujui, dan cetak laporan otomatis.</li>
 * </ol>
 *
 * <p><strong>Kategori Jenis Uang Muka:</strong><br>
 * Setiap uang muka dapat dikategorikan ke dalam {@code JenisUangMuka} yang
 * dikonfigurasi per satuan kerja. Pemilihan jenis uang muka wajib saat status
 * disetujui karena menentukan akun debit/kredit untuk jurnal transfer dana.</p>
 *
 * <p><strong>Integrasi dengan PR (Purchase Request):</strong><br>
 * Bila flag {@code ambilDariPr} aktif, pengguna dapat memilih satu atau lebih
 * {@code PermintaanPengadaanMasterAsset} (PR) beserta rincian itemnya. Nilai
 * uang muka akan dihitung otomatis dari total harga item PR yang dipilih.
 * Relasi ini disimpan sebagai string ID comma-separated di kolom
 * {@code permintaanPengadaanMasterAssets}.</p>
 *
 * <p><strong>Saldo dan Anggaran:</strong><br>
 * Saldo uang muka dihitung oleh {@code JenisUangMukaAction.hitungSaldo} dan
 * disimpan di entitas untuk kemudahan tampilan. Setiap kali form dibuka atau
 * tombol "Hitung Ulang" diklik, saldo direkontrol ulang dari history transaksi.</p>
 *
 * <p><strong>Threading:</strong><br>
 * Kelas ini adalah stateful per-sesi ZK. Setiap pengguna memiliki instance
 * sendiri. Field {@code permintaanPengadaanMasterAssets} (Set) diakses hanya
 * dari thread event ZK. {@code onSearchDefault} memiliki mekanisme rollback/retry
 * untuk mengatasi PostgreSQL "aborted transaction" error pada lingkungan produksi.</p>
 *
 * <p><strong>Pemeliharaan:</strong><br>
 * Bila ada field baru pada entitas {@code UangMuka}, perbarui: array {@code contents}
 * (untuk ekspor), renderer {@code UangMukaRenderer}, method {@code form()}, dan
 * {@code onSave()}. Konfigurasi seperti {@code saldo_harus_cukup_sebelum_mengajukan_realisasi_anggaran},
 * {@code tampilkan_tanpa_anggaran}, dan {@code tgl_laporan_pengajuan_uang_muka}
 * diambil dari tabel konfigurasi saat runtime.</p>
 *
 * @see UangMuka
 * @see PertangungjawabanAction
 * @see JenisUangMukaAction
 * @see DaftarPengajuanTransfer
 */
public class UangMukaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 * Serial version UID untuk kebutuhan serialisasi Serializable yang diwariskan
	 * dari {@code GenericAutowireComposer}. Nilai ini tidak boleh diubah sembarangan
	 * karena dapat mempengaruhi kompatibilitas session serialisasi ZK.
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox serachnama;
	private Textbox serachkode;
	private Checkbox searchaktif;
	private Checkbox searchblmLpj;
	private Checkbox searchtelahLpj;
	private Checkbox searchdariPr;

	private Checkbox searchtelahDpc;
	private Checkbox searchtelahBast;

	private Textbox searchanggaran;
	private Combobox searchstatus;
	private MyDatebox start;
	private MyDatebox end;
	private Textbox nama;
	private Label kode;
	private Textbox keterangan;
	private AmbilDataWorkspaceBanbox workspace;

	private AmbilDataWorkspaceBanbox searchAnggaran;

	public UangMuka uangMuka;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;

	private MyDoublebox nilai;

	private MyDatebox mulai;

	private Label selesai;

	private boolean persetujuan = false;

	private Tbmuser tbmuser;

	private Radiogroup status;

	private DisposisiSop disposisiSop = null;

	private SatuanKerjaTreeModel satuanKerjaTreeModel = null;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private Combobox jenisUangMuka;

	private boolean setujui;

	private MyCheckboxConfig tanpaAnggaran;
	private MyCheckboxConfig ambilDariPr;
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private MyDatebox sampai;

	private boolean viewOnly = false;

	private EventListener eventListener = null;
	private Set<PermintaanPengadaanMasterAssetDetail> permintaanPengadaanMasterAssets = null;

	private Vbox permintaanPengadaanMasterAsset;

	private AmbilDataAkunBanbox akun;

	private Row rowAkun;

	/**
	 * <h3>Konstruktor Default</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Membuat instance {@code UangMukaAction} untuk mode pengajuan normal. Mode ini
	 * memungkinkan staf untuk membuat pengajuan uang muka baru, mengubah yang belum
	 * disetujui, dan menghapus data yang masih dalam status pengajuan.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Konstruktor dipanggil oleh ZK Framework secara otomatis ketika file ZUL yang
	 * mengacu pada kelas ini di-compose. Memanggil {@code Common.getCurrentUser()}
	 * untuk mendapatkan pengguna sesi yang sedang aktif. Flag {@code persetujuan}
	 * tetap false sehingga tombol tambah akan tampil dan form dapat diedit penuh.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil pada thread ZK request. Setiap pengguna mendapat instance terpisah.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Field {@code satuanKerjaTreeModel} tidak diinisialisasi di konstruktor; akan
	 * dibuat saat {@code doAfterCompose} atau {@code form} dipanggil. Ini disengaja
	 * karena konstruktor perlu ringan dan tidak mengakses database.</p>
	 */
	public UangMukaAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * <h3>Konstruktor dengan Mode Persetujuan</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Membuat instance {@code UangMukaAction} dalam mode persetujuan atau mode
	 * pengajuan biasa sesuai parameter. Digunakan oleh modul SOP, dasbor keuangan,
	 * atau kode Java lain yang perlu membuka halaman uang muka dalam konteks
	 * tertentu.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Parameter {@code persetujuan} disimpan ke field instance dan mengendalikan
	 * perilaku seluruh kelas: bila true, tombol tambah disembunyikan, form menjadi
	 * read-only, dan hanya radiogroup status persetujuan yang aktif. Konstruktor
	 * juga memanggil {@code Common.getCurrentUser()} untuk mendapatkan pengguna aktif
	 * yang akan digunakan sebagai approver bila status disetujui.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan,
	 *        {@code false} untuk mode pengajuan normal.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil pada thread ZK request. Aman karena per instance per sesi.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Perubahan field {@code setujui} dan {@code viewOnly} dilakukan kemudian di
	 * {@code form()} berdasarkan state entitas, bukan di konstruktor. Konstruktor
	 * hanya menerima mode awal dari pemanggil.</p>
	 *
	 * @param persetujuan true jika instance ini digunakan dalam konteks persetujuan uang muka
	 */
	public UangMukaAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
	}

	public static String[] contents = new String[] { "id", "kode", "nama", "keterangan", "workspace", "jenisUangMuka",
			"mulai", "selesai", "nilai", "dibuatOleh", "disetujuiOleh", "tanggalPembuatan", "tanggalPersetujuan",
			"status", "pertangungjawaban.kode", "pertangungjawaban.keterangan", "pertangungjawaban.nilai",
			"pertangungjawaban.pajak", "pertangungjawaban.dikembalikan", "pertangungjawaban.dibuatOleh",
			"pertangungjawaban.disetujuiOleh", "pertangungjawaban.tanggalPembuatan",
			"pertangungjawaban.tanggalPersetujuan", "pertangungjawaban.status", "disposisiSop",
			"daftarPengajuanTransfer.prosesTransfer.kode", "daftarPengajuanTransfer.prosesTransfer.nama",
			"daftarPengajuanTransfer.prosesTransfer.tanggalPembuatan",
			"daftarPengajuanTransfer.prosesTransfer.disetujuiOleh",
			"daftarPengajuanTransfer.prosesTransfer.tanggalPersetujuan",
			"daftarPengajuanTransfer.prosesTransfer.realisasikanOleh",
			"daftarPengajuanTransfer.prosesTransfer.tanggalRealisasikan", "aktif" };

	protected Tabpanel statistik;
	/** Tabpanel tab "Monitor" — dashboard pemantauan + pembayaran DPC (lazy). */
	protected Tabpanel monitor;

	private MyDatebox tanggalPersetujuanManual;

	private Label sisaAnggaran;

	/**
	 * <h3>onStatistik — Memuat Dasbor Statistik Uang Muka</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Menangani event aktivasi tab "Statistik" pada halaman uang muka. Method ini
	 * memuat komponen dasbor analisis uang muka secara lazy untuk menghindari
	 * beban query yang tidak perlu saat halaman pertama kali dimuat.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Memeriksa apakah {@code statistik} (Tabpanel) sudah memiliki anak. Bila belum,
	 * membuat instance {@code DasboardUangMuka} baru, mengatur dimensinya 100% x 100%,
	 * dan memasangnya sebagai anak Tabpanel. Bila sudah ada anak (tab pernah dibuka),
	 * method tidak melakukan apa pun. Pola lazy-loading ini memastikan query dasbor
	 * hanya dijalankan saat pengguna benar-benar membuka tab tersebut, bukan saat
	 * halaman dimuat pertama kali.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param event Event ZK dari Tabbox saat tab Statistik dipilih. Tidak digunakan
	 *        dalam implementasi ini tetapi wajib ada sesuai konvensi ZK.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Komponen dasbor dipasang langsung ke Tabpanel.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil dari thread event ZK. Aman untuk memanipulasi komponen UI.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila dasbor perlu di-refresh setiap kali tab dibuka, ubah kondisi dari
	 * {@code size() == 0} menjadi selalu clear dan re-create. Pertimbangkan trade-off
	 * antara data terkini dan performa query.</p>
	 *
	 * @param event event ZK dari pilihan tab statistik
	 */
	/** Memuat dashboard "Monitor" (pemantauan uang muka + pertanggungjawaban + pembayaran DPC) secara lazy. */
	public void onMonitor(Event event) {
		if (monitor != null && monitor.getChildren().size() == 0) {
			MonitorUangMukaDashboard dashboard = new MonitorUangMukaDashboard();
			dashboard.setHeight("100%");
			dashboard.setWidth("100%");
			dashboard.setParent(monitor);
		}
	}

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DasboardUangMuka include = new DasboardUangMuka();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(statistik);
		}
	}

	/**
	 * <h3>doBeforeCompose — Pemeriksaan Keamanan Sebelum Compose</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Override lifecycle ZK yang dipanggil sebelum komponen UI di-compose dari file
	 * ZUL. Memastikan pengguna memiliki hak akses yang valid ke modul uang muka
	 * sebelum halaman mulai dibangun.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi privilege akses
	 * pengguna terhadap URL/modul yang diakses. Bila tidak memiliki akses, pengguna
	 * diarahkan ke halaman logoff atau error. Setelah pemeriksaan aman, memanggil
	 * {@code super.doBeforeCompose} untuk melanjutkan proses compose normal ZK
	 * termasuk wire komponen dan binding.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param page Halaman ZK yang sedang di-compose
	 * @param parent Komponen induk dalam hierarki ZK
	 * @param compInfo Metadata komponen dari ZUL parser</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return ComponentInfo dari super.doBeforeCompose untuk dilanjutkan ke ZK.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil pada thread HTTP request. Tidak ada state yang perlu disinkronkan.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Jangan hapus panggilan {@code super.doBeforeCompose}. Bila perlu menambahkan
	 * logika pre-compose tambahan, lakukan sebelum panggilan super.</p>
	 *
	 * @param page halaman ZK yang sedang diproses
	 * @param parent komponen induk dalam hierarki ZK
	 * @param compInfo informasi metadata komponen
	 * @return ComponentInfo untuk dilanjutkan ke mekanisme compose ZK
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <h3>doAfterCompose — Inisialisasi Halaman Uang Muka Setelah Compose</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Method lifecycle ZK yang dipanggil setelah seluruh komponen UI dari file ZUL
	 * selesai di-compose dan di-wire ke field Java. Bertanggung jawab menginisialisasi
	 * semua komponen ke kondisi awal: filter, privilege, paging, toolbar, dan pencarian
	 * awal. Method ini juga menambahkan tombol "Hitung Ulang" dan "History" yang
	 * bersifat khusus untuk modul uang muka.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Langkah-langkah inisialisasi berurutan:</p>
	 * <ol>
	 *   <li>Super compose, inisialisasi bahasa, cek sesi dan privilege READ.</li>
	 *   <li>Guard: bila {@code searchparent} null (mungkin karena ZUL kondisional),
	 *       return lebih awal.</li>
	 *   <li>Pasang event listener pada {@code searchparent} dan {@code searchAnggaran}
	 *       untuk trigger pencarian saat pilihan berubah.</li>
	 *   <li>Inisialisasi {@code SatuanKerjaTreeModel} untuk hierarki satuan kerja.</li>
	 *   <li>Atur datebox start (6 bulan lalu) dan end (besok), keduanya readonly.</li>
	 *   <li>Isi combo status: Semua, Pengajuan (hidden), Disetuju, Ditolak.</li>
	 *   <li>Baca parameter URL {@code persetujuan} bila ada.</li>
	 *   <li>Atur visibilitas tombol tambah dan privilege CRUD.</li>
	 *   <li>Inisialisasi paging, tombol cetak, dan upload.</li>
	 *   <li>Tambahkan tombol "Hitung Ulang" yang menjalankan rekontrol nilai, saldo,
	 *       dan relasi PR-detail untuk semua uang muka dalam filter saat ini secara
	 *       batch async dalam timer.</li>
	 *   <li>Tambahkan tombol "History" yang membuka {@code RevisiUangMukaHelper}
	 *       untuk melihat riwayat revisi uang muka.</li>
	 *   <li>Jalankan pencarian awal via timer dan setup filter lanjut.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dari super dilempar ke ZK. Error pada rekontrol "Hitung Ulang"
	 * ditangkap per item dalam loop dengan blok try-catch kosong agar item
	 * bermasalah tidak menghentikan seluruh batch.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil sekali saat halaman diload. Timer yang dibuat dieksekusi di thread
	 * event queue ZK yang terpisah.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila ada filter baru (misalnya filter berdasarkan kasbon/dana talangan), tambahkan
	 * inisialisasinya di sini. Pastikan komponen baru yang di-wire dari ZUL diperiksa
	 * null-nya sebelum diakses untuk menghindari NullPointerException bila ZUL
	 * kondisional tidak menyertakan komponen tersebut.</p>
	 *
	 * @param comp komponen root yang telah di-compose dari file ZUL
	 * @throws Exception jika terjadi error pada proses inisialisasi
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (searchparent == null) return;
		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (searchAnggaran != null) {
			searchAnggaran.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);

				}
			});
		}

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		// searchstatus bisa null bila ZUL halaman ini tidak punya combobox status -> jangan di-deref.
		Comboitem comboitemSemua = new Comboitem("Semua");
		comboitemSemua.setValue(null);
		if (searchstatus != null) {
			searchstatus.appendChild(comboitemSemua);

			Comboitem comboitem = new Comboitem(UangMuka.PENGAJUAN);
			comboitem.setValue(UangMuka.PENGAJUAN);
			comboitem.setVisible(false);
			searchstatus.appendChild(comboitem);
			comboitem = new Comboitem(UangMuka.DISETUJU);
			comboitem.setValue(UangMuka.DISETUJU);
			searchstatus.appendChild(comboitem);
			comboitem = new Comboitem(UangMuka.DITOLAK);
			comboitem.setValue(UangMuka.DITOLAK);
			searchstatus.appendChild(comboitem);

			searchstatus.setSelectedItem(comboitemSemua);
			searchstatus.setReadonly(true);
		}

		if (execution.getParameter("persetujuan") != null) {
			boolean persetujuanDariUrl = Boolean.parseBoolean(execution.getParameter("persetujuan"));
			// Parameter URL TIDAK BOLEH menaikkan mode dari pengajuan ke persetujuan --
			// hanya menu Persetujuan (konstruktor super(true), lihat PersetujuanUangMukaAction)
			// atau hak APPROVE eksplisit pada menu aktif yang boleh mengaktifkannya. Mencegah
			// eskalasi via ?persetujuan=true di menu Pengajuan (lihat Javadoc UangMuka.java
			// bagian "Catatan pengendalian internal").
			persetujuan = persetujuanDariUrl
					? (persetujuan || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE))
					: false;
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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(UangMuka.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, UangMuka.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		if (add != null) {
			if (persetujuan) {
				add.setVisible(false);
			} else {
				add.setLabel("Pengajuan Uang Muka Baru");
			}
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hitung Ulang", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<UangMuka> uangMukas = initCriteria(false).addOrder(Order.asc("id")).setMaxResults(5000)
								.list();

						for (UangMuka uangMuka : uangMukas) {

							try {
								if (uangMuka.getDaftarPengajuanTransfer() == null
										&& uangMuka.getDisetujuiOleh() != null) {
									Session session = HibernateUtil.currentSession();
									DaftarPengajuanTransfer d = (DaftarPengajuanTransfer) session
											.createCriteria(DaftarPengajuanTransfer.class)
											.createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
													Restrictions.eq("disposisiSop.aktif", true)))
											.addOrder(Order.desc("id")).add(Restrictions.eq("uangMuka", uangMuka))
											.setMaxResults(1).uniqueResult();
									if (d != null) {
										uangMuka.setDaftarPengajuanTransfer(d);
										Common.refreshUpdate(session, uangMuka);
										session.flush();
									}
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/UangMukaAction.java:608");
								// TODO: handle exception
							}

							if (uangMuka.getAmbilDariPr() && !uangMuka.getPermintaanPengadaanMasterAssets().isEmpty()) {
								List<PermintaanPengadaanMasterAssetDetail> dataPermintaanPengadaanMasterAssetDetail = new ArrayList<PermintaanPengadaanMasterAssetDetail>();

								List<Long> data = new ArrayList<Long>();
								for (String s : uangMuka.getPermintaanPengadaanMasterAssets().split(",")) {
									try {
										data.add(Long.parseLong(s.trim()));
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/UangMukaAction.java:619");
										// TODO: handle exception
									}
								}
								dataPermintaanPengadaanMasterAssetDetail = data.isEmpty()
										? new ArrayList<PermintaanPengadaanMasterAssetDetail>()
										: HibernateUtil.currentSession()
												.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
												.add(Restrictions.in("id", data)).list();

								if (dataPermintaanPengadaanMasterAssetDetail.isEmpty()) {
									dataPermintaanPengadaanMasterAssetDetail = HibernateUtil.currentSession()
											.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
											.add(Restrictions.eq("uangMuka", uangMuka)).list();
								}

								for (PermintaanPengadaanMasterAssetDetail assetDetail : dataPermintaanPengadaanMasterAssetDetail) {

									if (assetDetail.getUangMuka() == null || (assetDetail.getUangMuka() != null
											&& !assetDetail.getUangMuka().getId().equals(uangMuka.getId()))) {
										assetDetail.setUangMuka(uangMuka);
										Session session = HibernateUtil.currentSession();
										Common.refreshUpdate(session, assetDetail);
										session.flush();
									}

								}

							}

							Double saldoSekarang = JenisUangMukaAction.hitungSaldo(uangMuka.getId(), null, null, null,
									uangMuka.getWorkspace(), uangMuka.getTanggalPembuatan());

							if (saldoSekarang.longValue() != uangMuka.getSaldo().longValue()) {
								uangMuka.setSaldo(saldoSekarang);
								Session session = HibernateUtil.currentSession();
								Common.refreshUpdate(session, uangMuka);
								session.flush();
							}

						}
						onSearchDefault(null);
					}
				});

			}

		});
		if (button != null && add != null && add.getParent() != null) { button.setParent(add.getParent()); }

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiUangMukaHelper revisiHelper = new RevisiUangMukaHelper(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		if (button != null && add != null && add.getParent() != null) { button.setParent(add.getParent()); }

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * <h3>UangMukaRenderer — Renderer Baris Grid Uang Muka</h3>
	 *
	 * <p><strong>Untuk apa:</strong><br>
	 * Kelas inner yang mengimplementasikan {@code MyRowRenderer} untuk merender setiap
	 * baris data uang muka dalam grid utama halaman. Setiap baris menampilkan informasi
	 * lengkap uang muka beserta tautan ke proses transfer, workspace anggaran, LPJ
	 * terkait, penerimaan aset, dan tombol aksi CRUD.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method {@code render} dipanggil ZK untuk setiap {@code UangMuka} dalam model.
	 * Baris diisi dengan: kode+nama+link proses transfer+link workspace+link LPJ+link BAST,
	 * saldo, nilai, sisa (saldo-nilai), periode kegiatan, tanggal laporan, dibuat oleh+tanggal,
	 * status+approver+tanggal, keterangan+SOP+status transfer+info kasbon, aktif/checkbox,
	 * dan tombol CRUD+cetak. Bila disetujui tapi belum ada transfer, timer async membuat
	 * {@code DaftarPengajuanTransfer} secara otomatis. Saldo juga direkontrol setiap
	 * render via {@code JenisUangMukaAction.hitungSaldo}.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil dari thread event ZK saat grid me-render model. Operasi hitungSaldo
	 * mengakses database melalui currentSession.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Jumlah dan urutan sel harus sesuai dengan kolom grid di file ZUL. Bila ada
	 * kolom baru (misal: kolom kasbon), tambahkan di posisi yang sesuai di kedua tempat.</p>
	 */
	class UangMukaRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render — Merender Satu Baris Data Uang Muka ke Grid Row</h3>
		 *
		 * <p><strong>Tujuan:</strong><br>
		 * Mengisi satu baris ({@code Row}) ZK grid dengan semua informasi dari satu
		 * entitas {@code UangMuka}. Dipanggil oleh framework ZK untuk setiap elemen
		 * dalam model saat grid di-render atau di-refresh.</p>
		 *
		 * <p><strong>Cara kerja:</strong><br>
		 * Pertama merekontrol saldo dengan {@code JenisUangMukaAction.hitungSaldo} dan
		 * memperbarui entitas bila berubah. Kemudian mengisi Row dengan sel-sel: Vbox
		 * kode+nama+link proses transfer+link workspace+link LPJ (anchor onClick membuka
		 * PertangungjawabanAction.onAddExternal)+link BAST, Label saldo, nilai, sisa,
		 * periode mulai-sampai, tanggal laporan, Vbox dibuat oleh+tanggal, Vbox
		 * status+approver+tanggal, Vbox keterangan+SOP+transfer+kasbon, checkbox/label
		 * aktif, dan Hbox tombol aksi (edit/delete/cetak).</p>
		 *
		 * <p><strong>Parameter:</strong>
		 * @param arg0 Row ZK yang akan diisi komponen-komponen sel.
		 * @param arg1 Object yang merupakan instance {@code UangMuka} dari model.</p>
		 *
		 * <p><strong>Penanganan error:</strong><br>
		 * Exception dilempar ke ZK framework. Error pada hitungSaldo tidak boleh
		 * menghentikan render; pertimbangkan try-catch bila saldo tidak kritis.</p>
		 *
		 * <p><strong>Threading:</strong><br>
		 * Dipanggil dari thread event ZK. Akses database melalui currentSession.</p>
		 *
		 * <p><strong>Pemeliharaan:</strong><br>
		 * Urutan appendChild harus sesuai persis dengan urutan kolom di ZUL. Rekontrol
		 * saldo di setiap render mungkin mahal bila data banyak; pertimbangkan lazy
		 * rekontrol hanya bila saldo berpotensi stale (misal: setelah ada transaksi baru).</p>
		 *
		 * @param arg0 baris grid ZK yang akan diisi
		 * @param arg1 entitas UangMuka yang akan ditampilkan
		 * @throws Exception jika terjadi error saat merender komponen
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final UangMuka uangMuka = (UangMuka) arg1;

			if (uangMuka.getDibuatOleh() == null) {
				uangMuka.setDibuatOleh(tbmuser);
			}

			if (uangMuka.getDisetujuiOleh() != null && uangMuka.getDaftarPengajuanTransfer() == null) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (uangMuka.getStatus().equals(UangMuka.DISETUJU)) {
							DaftarPengajuanTransfer.simpanUangMuka(uangMuka);
						}

					}
				});
			}

			Double saldo = JenisUangMukaAction.hitungSaldo(uangMuka.getId(), null, null, null, uangMuka.getWorkspace(),
					uangMuka.getTanggalPembuatan());

			if (saldo.intValue() != uangMuka.getSaldo().intValue()) {
				uangMuka.setSaldo(saldo);
				Common.refreshUpdate(uangMuka);
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(UangMuka.class, uangMuka,
					uangMuka.getKode() == null ? "" : uangMuka.getKode().trim().toString())).setParent(arg0);
			new Label(uangMuka.getNama()).setParent(a);

			if (uangMuka.getDaftarPengajuanTransfer() != null
					&& uangMuka.getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A aaa = new A(uangMuka.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				aaa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, uangMuka.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				aaa.setStyle("font-size:12px;");
				aaa.setParent(a);
			}

			if (uangMuka.getWorkspace() != null) {
				RevisiHelper
						.createNewRevisi(Workspace.class, uangMuka.getWorkspace(),
								uangMuka.getWorkspace().getKode() + "-" + uangMuka.getWorkspace().getNama())
						.setParent(a);

			}

			if (uangMuka.getPertangungjawaban() != null) {
				A aa = new A(uangMuka.getPertangungjawaban().getKode() + " "
						+ Common.numberFormat.get().format(uangMuka.getPertangungjawaban().getNilai())
						+ (uangMuka.getPertangungjawaban().getDibuatOleh() == null ? ""
								: " " + uangMuka.getPertangungjawaban().getDibuatOleh().getUserNama())
						+ " " + uangMuka.getPertangungjawaban().getKeterangan());
				aa.setParent(a);
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						PertangungjawabanAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, uangMuka.getPertangungjawaban());

					}
				});
				aa.setStyle("font-size:9px;");
			}

			if (uangMuka.getPenerimaanPengadaanMasterAsset() != null) {

				A aa = new A(uangMuka.getPenerimaanPengadaanMasterAsset().getKode());
				aa.setParent(a);
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						PenerimaanPengadaanMasterAssetAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, uangMuka.getPenerimaanPengadaanMasterAsset());

					}
				});
				aa.setStyle("font-size:9px;");

			}

			new Label(Common.numberFormat.get().format(uangMuka.getSaldo())).setParent(arg0);
			new Label(Common.numberFormat.get().format(uangMuka.getNilai())).setParent(arg0);
			// Kolom "Sisa": UM berdasarkan PR (ambilDariPr) TIDAK memotong anggaran lagi karena anggaran
			// sudah dipotong saat pengajuan PR. Maka sisa = saldo anggaran apa adanya (JANGAN kurangi nilai
			// UM lagi → itulah penyebab sisa minus). Hanya UM biasa (non-PR) yang menampilkan saldo - nilai.
			// (Saldo sendiri sudah mengecualikan UM-from-PR via JenisUangMukaAction.buildPenggunaanAnggaranCriteria.)
			new Label(uangMuka.getSaldo() <= 0.0 ? ""
					: Common.numberFormat.get().format(uangMuka.getAmbilDariPr() ? uangMuka.getSaldo()
							: uangMuka.getSaldo() - uangMuka.getNilai())).setParent(arg0);

			new Label((uangMuka.getMulai() == null ? ""
					: (Common.dateFormat1.get().format(uangMuka.getMulai())) + " sd "
							+ Common.dateFormat1.get().format(uangMuka.getSampai())))
					.setParent(arg0);
			new Label((uangMuka.getSelesai() == null ? "" : Common.dateFormat1.get().format(uangMuka.getSelesai())))
					.setParent(arg0);

			a = new Vbox();
			a.setParent(arg0);
			new Label(uangMuka.getDibuatOleh() == null ? "" : uangMuka.getDibuatOleh().getUserNama()).setParent(a);
			new Label(uangMuka.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(uangMuka.getTanggalPembuatan())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label(uangMuka.getStatus()).setParent(a);
			(new Label(uangMuka.getDisetujuiOleh() == null ? "" : uangMuka.getDisetujuiOleh().getUserNama()))
					.setParent(a);
			(new Label(uangMuka.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(uangMuka.getTanggalPersetujuan()))).setParent(a);

			a = new Vbox();
			a.setParent(arg0);

			if (uangMuka.getDisposisiSop() != null) {
				new Label(Common.simpleString(uangMuka.getKeterangan())).setParent(a);
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + uangMuka.getDisposisiSop().getKeterangan() + " ("
						+ uangMuka.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(uangMuka.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			} else {
				new Label(uangMuka.getKeterangan()).setParent(a);
			}

			DaftarPengajuanTransfer.tampilStatus(uangMuka.getDaftarPengajuanTransfer(), a);

			if (uangMuka.getDanaTalangan() != null && uangMuka.getDanaTalangan().getDisetujuiOleh() != null) {
				new Label("Kasbon : " + uangMuka.getDanaTalangan().getNama() + " "
						+ Common.numberFormat.get().format(uangMuka.getDanaTalangan().getNilai())).setParent(a);
			}

			if (uangMuka.getDisposisiSop() != null && !uangMuka.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (persetujuan && !uangMuka.getStatus().equals(UangMuka.DISETUJU) && edit) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(uangMuka.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						uangMuka.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(uangMuka);
					}
				});
			} else {
				new Label(uangMuka.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			Hbox hbx;
			(hbx = Common.copyEditDeleteButtons(edit, !persetujuan && !uangMuka.getStatus().equals(UangMuka.DISETUJU),
					delete && !persetujuan && !uangMuka.getStatus().equals(UangMuka.DISETUJU), uangMuka,
					UangMukaAction.this)).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(uangMuka);
				}
			});
			button.setParent(hbx);
		}

	}

	/**
	 * <h3>cetak — Menampilkan Laporan Uang Muka dalam Modal</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Method statis untuk menampilkan laporan uang muka sebagai window modal ZK.
	 * Dipanggil dari tombol cetak per baris di {@code UangMukaRenderer}, dari
	 * {@code onAddExternal} saat pengguna membuka detail, dari tombol "Cetak" di
	 * window view-only, dan secara otomatis setelah {@code onSave} berhasil.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Membuat instance {@code LaporanUangMuka} dengan data uang muka, mengkonfigurasi
	 * properti window (judul "Laporan", closable, tinggi 90%, lebar 900px), melampirkan
	 * ke root page aktif, dan membuka sebagai modal dengan {@code onModal()}. Pengguna
	 * dapat melihat, mencetak (Ctrl+P), atau menutup laporan.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param uangMuka Data {@code UangMuka} yang akan ditampilkan laporannya. Harus
	 *        ter-load lengkap beserta relasi workspace, jenisUangMuka, dan pertangungjawaban.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Window modal laporan muncul sebagai efek samping.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil. Error umum: template laporan tidak ditemukan,
	 * data null reference pada generate parameter.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Harus dipanggil dari thread event ZK karena mengakses {@code ExecutionsCtrl}
	 * dan memanipulasi komponen UI. Tidak aman dari thread background.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Method statis untuk kemudahan pemanggilan dari renderer inner class. Template
	 * laporan di "akunting/uangMuka". Bila format laporan perlu diubah, modifikasi
	 * template JRXML, bukan method ini.</p>
	 *
	 * @param uangMuka data uang muka yang akan ditampilkan dalam laporan modal
	 * @throws Exception jika gagal membuat atau menampilkan laporan
	 */
	public static void cetak(UangMuka uangMuka) throws Exception {
		LaporanUangMuka buktiPengeluaranKas = new LaporanUangMuka(uangMuka);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		buktiPengeluaranKas.onModal();
	}

	/**
	 * <h3>init(GeneralValueObject) — Inisialisasi Form dari Antarmuka Generik</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Mengimplementasikan kontrak interface {@code DataInitDefault} sebagai jembatan
	 * antara framework CRUD generik dengan implementasi spesifik {@code UangMuka}.
	 * Memungkinkan mekanisme edit/copy generik memanggil form tanpa mengetahui tipe
	 * entitas secara spesifik.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Meng-cast {@code generalValueObject} ke {@code UangMuka}, menyimpan ke field
	 * instance, memanggil overload private {@code init(UangMuka)} yang membangun
	 * seluruh layout window, kemudian menampilkan window sebagai modal.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param obj Entitas {@code UangMuka} yang akan diedit atau ditampilkan. Instance
	 *        baru bila tambah, instance dari database bila ubah.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Window modal form uang muka muncul sebagai efek samping.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil. ClassCastException bila obj bukan UangMuka.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Harus dipanggil dari thread event ZK.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Method ini tidak perlu diubah; semua logika ada di {@code init(UangMuka)} dan
	 * {@code form()}.</p>
	 *
	 * @param obj entitas UangMuka untuk ditampilkan di form
	 * @throws Exception jika gagal membangun atau menampilkan form
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		uangMuka = (UangMuka) obj;
		init(uangMuka);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <h3>onAddExternal — Membuka Form Uang Muka dari Modul Lain</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Method statis yang memungkinkan modul lain (renderer LPJ, dasbor, SOP) untuk
	 * membuka form detail uang muka dalam mode view-only tanpa navigasi ke halaman
	 * utama uang muka. Mendukung alur cross-modul yang seamless.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Membuat instance {@code UangMukaAction} secara programatik, menyimpan
	 * {@code eventListener} untuk callback, membuat {@code MyWindow} baru, mengatur
	 * mode ({@code persetujuan=true}, {@code setujui=true}, {@code viewOnly=true}),
	 * melampirkan window ke root page aktif, mengatur dimensi (95% x 550px), memanggil
	 * {@code init(uangMuka)} untuk membangun form, dan membuka sebagai modal.
	 * Event listener disimpan di field instance sehingga dapat dipanggil oleh
	 * {@code onSave} setelah operasi selesai bila ada perubahan yang perlu di-propagate
	 * ke halaman pemanggil.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param eventListener EventListener yang dipanggil oleh onSave setelah simpan,
	 *        memungkinkan halaman pemanggil untuk refresh datanya.
	 * @param uangMuka Data {@code UangMuka} yang akan ditampilkan dalam mode view-only.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Window modal muncul sebagai efek samping.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil. Error umum: getCurrentPage null bila dipanggil
	 * di luar konteks request ZK aktif.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Harus dipanggil dari thread event ZK.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bedakan dengan {@code PertangungjawabanAction.onAddExternal}: di sini
	 * eventListener aktif disimpan dan digunakan. Di PertangungjawabanAction
	 * eventListener tidak aktif. Bila perlu preview dengan data terbaru, panggil
	 * {@code session.refresh(uangMuka)} sebelum masuk form.</p>
	 *
	 * @param eventListener callback yang dipanggil setelah operasi pada form selesai
	 * @param uangMuka data uang muka yang akan ditampilkan
	 * @throws Exception jika gagal membuat atau menampilkan window form
	 */
	public static void onAddExternal(EventListener eventListener, UangMuka uangMuka) throws Exception {
		UangMukaAction uangMukaAction = new UangMukaAction();
		uangMukaAction.eventListener = eventListener;
		uangMukaAction.addWindow = new MyWindow();
		uangMukaAction.persetujuan = true;
		uangMukaAction.setujui = true;
		uangMukaAction.viewOnly = true;

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(uangMukaAction.addWindow);
		uangMukaAction.addWindow.setHeight("95%");
		uangMukaAction.addWindow.setWidth("550px");

		uangMukaAction.init(uangMuka);

		uangMukaAction.addWindow.setVisible(true);
		uangMukaAction.addWindow.setClosable(true);
		uangMukaAction.addWindow.onModal();

	}

	/**
	 * <h3>onAdd — Membuka Form Pengajuan Uang Muka Baru</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Menangani event klik tombol "Pengajuan Uang Muka Baru" di toolbar halaman.
	 * Membuka form dalam mode tambah (create new) dengan semua field kosong dan
	 * siap diisi oleh pemohon.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Pertama me-reset {@code viewOnly = false} untuk memastikan form terbuka dalam
	 * mode edit meskipun sebelumnya pernah membuka record view-only. Kemudian memanggil
	 * {@code init(new UangMuka())} dengan objek kosong yang menyebabkan form dibangun
	 * dalam konteks "create new" (ID null). Kode akan di-generate otomatis dan field
	 * satuan kerja akan diisi default dari satuan kerja pengguna. Window ditampilkan
	 * dan dibuka modal; error dari {@code onModal} (jarang terjadi) ditangkap dan
	 * ditampilkan hanya kepada admin.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param event Event klik ZK dari tombol tambah. Tidak digunakan dalam implementasi.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Window modal form baru muncul sebagai efek samping.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dari {@code init} dilempar ke ZK. Exception dari {@code onModal}
	 * ditangkap dan ditampilkan via {@code Common.tampilErrorJikaAdmin}.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil dari thread event ZK. Sinkron.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Reset {@code viewOnly = false} di sini sangat penting. Tanpanya, jika pengguna
	 * sebelumnya membuka record view-only (sudah selesai SOP), form tambah berikutnya
	 * akan read-only secara tidak sengaja karena {@code viewOnly} masih true.</p>
	 *
	 * @param event event klik ZK dari tombol tambah pengajuan uang muka baru
	 * @throws Exception jika gagal menginisialisasi form
	 */
	public void onAdd(Event event) throws Exception {
		viewOnly = false;
		init(new UangMuka());
		addWindow.setVisible(true);
		try {
			addWindow.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * <h3>form — Membangun Grid Form Pengajuan Uang Muka</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Membangun dan mengembalikan komponen {@code MyGrid} yang berisi seluruh form
	 * pengisian atau tampilan data uang muka. Ini adalah method terpanjang dan paling
	 * kompleks dalam kelas ini — mengelola tiga mode tampilan (edit/persetujuan/view),
	 * tiga mode pengisian sumber anggaran (workspace/tanpaAnggaran/dariPR), serta
	 * integrasi dengan PR (permintaan pengadaan) secara interaktif.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Form dibangun secara programatik dengan pola MyFormRow 2 kolom. Field-field
	 * utama yang dibangun:</p>
	 * <ul>
	 *   <li>Satuan Kerja (readonly banbox, defaultnya dari satuan kerja pengguna).</li>
	 *   <li>Checkbox "Tanpa Anggaran" (kondisional dari konfigurasi) dan "Ambil dari PR".</li>
	 *   <li>Akun (diperlukan bila tanpa anggaran).</li>
	 *   <li>Anggaran/Workspace (banbox, wajib bila bukan tanpaAnggaran dan bukan dariPR).</li>
	 *   <li>Bagian Permintaan Pengadaan (Vbox dengan tombol "Ambil Data" dan sub-grid
	 *       daftar PR yang dipilih, dengan tombol hapus per item).</li>
	 *   <li>Kode (auto-generate), Judul Pengajuan.</li>
	 *   <li>Label-label info: Unit, Total Anggaran, Anggaran Dalam Proses (dengan
	 *       detail Vbox), Sisa Anggaran, Periode, Akun Anggaran.</li>
	 *   <li>JenisUangMuka (combo, dimuat berdasarkan satuan kerja yang relevan).</li>
	 *   <li>Tanggal Mulai, Tanggal Sampai (dengan kalkulasi otomatis Tanggal Laporan).</li>
	 *   <li>Jumlah Pengajuan (MyDoublebox), Diajukan Oleh, Diajukan Tanggal.</li>
	 *   <li>Info Kasbon/DanaTalangan bila ada.</li>
	 *   <li>Radiogroup Status Pengajuan (hanya di mode persetujuan).</li>
	 *   <li>Akun Penerima (jenisUangMuka, tampil di mode persetujuan/setujui).</li>
	 *   <li>Tanggal Persetujuan Manual, Keterangan.</li>
	 * </ul>
	 * <p>EventListener {@code eventListenerE} mengontrol visibilitas baris berdasarkan
	 * kombinasi flag tanpaAnggaran dan ambilDariPr. EventListener {@code eventListener}
	 * pada workspace mengisi label saldo dan detail anggaran secara reaktif.
	 * EventListener {@code eventListenerJenisUangMuka} memuat combo jenis uang muka
	 * sesuai satuan kerja. Class lokal {@code PermintaanBarangEventListener}
	 * merender ulang sub-grid daftar PR yang dipilih.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param generalValueObject Entitas {@code UangMuka} yang datanya akan ditampilkan.
	 * @param disposisiSop DisposisiSop dari alur SOP aktif, atau null bila bukan dari SOP.
	 * @param save Tombol simpan yang labelnya dikontrol oleh event listener status dalam form.
	 * @param setujuiData EventListener tambahan untuk klik radiogroup status, digunakan
	 *        oleh halaman SOP untuk sinkronisasi state persetujuan.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return {@code MyGrid} yang berisi seluruh komponen form, siap dipasang ke Center layout.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil. Error umum: HibernateException bila session
	 * tidak aktif, NullPointerException bila workspace null saat kalkulasi saldo.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Harus dipanggil dari thread event ZK. Membuat banyak komponen UI secara sinkron.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Method ini sangat panjang karena mencakup tiga mode sumber anggaran sekaligus.
	 * Bila ada mode baru (misal: dari kasbon/dana talangan), tambahkan checkbox dan
	 * tampilkan panel yang relevan. Pastikan {@code eventListenerE} juga mengontrol
	 * visibilitas panel baru. Setiap perubahan field harus dipertimbangkan untuk
	 * semua 3 mode: edit, persetujuan, view-only.</p>
	 *
	 * @param generalValueObject entitas UangMuka yang akan ditampilkan/diedit
	 * @param disposisiSop disposisi SOP aktif atau null
	 * @param save tombol simpan yang labelnya dikontrol form
	 * @param setujuiData listener tambahan untuk event status
	 * @return MyGrid form yang telah dibangun lengkap
	 * @throws Exception jika terjadi error saat membangun komponen form
	 */
	@SuppressWarnings("unchecked")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		uangMuka = (UangMuka) generalValueObject;
		if (uangMuka.getDibuatOleh() == null) {
			uangMuka.setDibuatOleh(tbmuser);
			uangMuka.setTanggalPembuatan(new Date());
		}

		setujui = false;
		if (!persetujuan) {
			if (uangMuka != null && uangMuka.getStatus().equals(UangMuka.DISETUJU)) {
				setujui = true;
			} else {
				setujui = false;
			}
		}

		if (uangMuka.getDisposisiSop() != null && uangMuka.getDisposisiSop().getDisposisiSetuju() != null
				&& uangMuka.getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null
				&& uangMuka.getDisposisiSop().getDisposisiSetuju().getSelesai()) {
			viewOnly = true;
		}

		// REVISI: pengajuan dibuka lewat disposisi SOP yang MASIH aktif (belum selesai) untuk direvisi
		// pemohon (bukan mode approver, bukan view-only). Saat revisi, "Jumlah Pengajuan" boleh diubah
		// meski status sudah "Disetujui" pada siklus sebelumnya (permintaan user 11-08).
		final boolean revisi = !persetujuan && !viewOnly && uangMuka.getDisposisiSop() != null
				&& uangMuka.getDisposisiSop().getId() != null
				&& (uangMuka.getDisposisiSop().getAktif() == null
						|| Boolean.TRUE.equals(uangMuka.getDisposisiSop().getAktif()));

		System.out.println("uangMuka -> " + uangMuka);

		workspace = new AmbilDataWorkspaceBanbox(false);

		try {
			if (uangMuka.getSatuanKerja() == null) {
				uangMuka.setSatuanKerja(Common.getSatuanKerja());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/UangMukaAction.java:1312");
			// TODO: handle exception
		}

		final MyFormRow rowSatker = new MyFormRow();
		rowSatker.setParent(rows);
		rowSatker.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja *"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(uangMuka.getSatuanKerja() == null ? "" : uangMuka.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", uangMuka.getSatuanKerja());
		satuanKerja.setReadonly(true);
		if (persetujuan || setujui || viewOnly) {
			rowSatker.appendChild(
					new Label(uangMuka.getSatuanKerja() == null ? "" : uangMuka.getSatuanKerja().getNama()));
		} else {
			rowSatker.appendChild(satuanKerja);
		}
		satuanKerja.setWidth("90%");

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setVisible(Common.bolehKonfigurasi("tampilkan_tanpa_anggaran"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		tanpaAnggaran = new MyCheckboxConfig("Merupakan tanpa anggaran");
		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label("Merupakan tanpa anggaran ? " + (uangMuka.getTanpaAnggaran() ? "Ya" : "Tidak")));
		} else {
			row.appendChild(tanpaAnggaran);
		}

		tanpaAnggaran.setChecked(uangMuka.getTanpaAnggaran());

		final MyFormRow rowAkunPilih = new MyFormRow();
		rowAkunPilih.setParent(rows);
		rowAkunPilih.appendChild(new ais.ui.util.MyLabelConfig("Akun *"));
		akun = new AmbilDataAkunBanbox(false);
		akun.setValue(uangMuka.getAkun() == null ? "" : uangMuka.getAkun().getNama());
		akun.setAttribute("akun", uangMuka.getAkun());
		akun.setReadonly(true);
		if (persetujuan || setujui || viewOnly) {
			rowAkunPilih.appendChild(new Label(uangMuka.getAkun() == null ? "" : uangMuka.getAkun().getNama()));
		} else {
			rowAkunPilih.appendChild(akun);
		}
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		ambilDariPr = new MyCheckboxConfig("Ambil dari permintaan pembelian (PR)");
		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(
					"Merupakan dari permintaan pembelian (PR) ? " + (uangMuka.getAmbilDariPr() ? "Ya" : "Tidak")));
		} else {
			row.appendChild(ambilDariPr);
		}

		ambilDariPr.setChecked(uangMuka.getAmbilDariPr());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anggaran *"));
		workspace.setValue(uangMuka.getWorkspace() == null ? "" : uangMuka.getWorkspace().toString());
		workspace.setAttribute("workspace", uangMuka.getWorkspace());
		workspace.setWidth("90%");

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(uangMuka.getWorkspace() == null ? "" : uangMuka.getWorkspace().toString()));
		} else {
			row.appendChild(workspace);
		}

		List<PermintaanPengadaanMasterAssetDetail> dataPermintaanPengadaanMasterAssetDetail = new ArrayList<PermintaanPengadaanMasterAssetDetail>();
		if (!uangMuka.getPermintaanPengadaanMasterAssets().isEmpty()) {

			List<Long> data = new ArrayList<Long>();
			for (String s : uangMuka.getPermintaanPengadaanMasterAssets().split(",")) {
				try {
					data.add(Long.parseLong(s.trim()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/UangMukaAction.java:1392");
					// TODO: handle exception
				}
			}
			dataPermintaanPengadaanMasterAssetDetail = data.isEmpty()
					? new ArrayList<PermintaanPengadaanMasterAssetDetail>()
					: HibernateUtil.currentSession().createCriteria(PermintaanPengadaanMasterAssetDetail.class)
							.add(Restrictions.in("id", data)).list();

			if (dataPermintaanPengadaanMasterAssetDetail.isEmpty()) {
				dataPermintaanPengadaanMasterAssetDetail = HibernateUtil.currentSession()
						.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
						.add(Restrictions.eq("uangMuka", uangMuka)).list();
			}

			if (!dataPermintaanPengadaanMasterAssetDetail.isEmpty()) {

				Set<Workspace> workspaces = new HashSet<Workspace>();
				for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : dataPermintaanPengadaanMasterAssetDetail) {
					if (permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
							.getWorkspace() != null) {
						workspaces.add(permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
								.getWorkspace());
					}
				}

				if (!workspaces.isEmpty()) {
					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Kode Anggaran"));
					Vbox unit = new Vbox();
					row.appendChild(unit);

					for (Workspace workspace : workspaces) {
						RevisiHelper.createNewRevisi(Workspace.class, workspace, workspace.toString()).setParent(unit);
					}
				}
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Permintaan Pengadaan *")));
		row.appendChild(permintaanPengadaanMasterAsset = new Vbox());
		permintaanPengadaanMasterAsset.setWidth("90%");
		UangMukaAction.this.permintaanPengadaanMasterAssets = new HashSet<PermintaanPengadaanMasterAssetDetail>();

		if (!uangMuka.getPermintaanPengadaanMasterAssets().isEmpty()) {
			UangMukaAction.this.permintaanPengadaanMasterAssets.addAll(dataPermintaanPengadaanMasterAssetDetail);
		}

		MyToolbarbuttonConfig button;
		permintaanPengadaanMasterAsset.appendChild(
				button = new MyToolbarbuttonConfig("Ambil Data Permintaan Barang/Jasa", "/img/svg/addthis.svg"));
		button.setVisible(!persetujuan);
		final MyGrid subPermintaanPengadaanMasterAsset = new MyGrid();
		permintaanPengadaanMasterAsset.appendChild(subPermintaanPengadaanMasterAsset);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));

		if (uangMuka.getKode() == null) {
			String noAgenda = generateCode(false);
			uangMuka.setKode(noAgenda);
		}

		kode = new Label(uangMuka.getKode());
		if (persetujuan) {
			row.appendChild(new Label(uangMuka.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		nama = new Textbox(uangMuka.getNama());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Pengajuan *"));
		nama.setWidth("90%");

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(uangMuka.getNama()));
		} else {
			row.appendChild(nama);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit/Satuan Kerja"));
		final Label unit = new Label();
		row.appendChild(unit);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Total Anggaran"));
		final Label saldo = new Label();
		row.appendChild(saldo);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anggaran Dalam Proses"));
		final Label uangMukaDalamProses = new Label();
		row.appendChild(uangMukaDalamProses);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final Vbox uangMukaDalamProsesDetail = new Vbox();
		row.appendChild(uangMukaDalamProsesDetail);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sisa Anggaran"));
		sisaAnggaran = new Label();
		row.appendChild(sisaAnggaran);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Periode Anggaran"));
		final Label tgl = new Label();
		row.appendChild(tgl);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Anggaran"));
		final Label akun = new Label();
		row.appendChild(akun);

		jenisUangMuka = new Combobox();

		final EventListener eventListenerJenisUangMuka = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();

				SatuanKerja parent = pt == null ? null : pt.getSatuanKerja();

				if (tbmuser.ambilSatuanKerja() != null) {
					parent = tbmuser.ambilSatuanKerja();
				}

				if (UangMukaAction.this.satuanKerja.getAttribute("satuanKerja") != null) {
					parent = (SatuanKerja) UangMukaAction.this.satuanKerja.getAttribute("satuanKerja");
				}

				Workspace work = (Workspace) workspace.getAttribute("workspace");
				if (work != null && !tanpaAnggaran.isChecked()) {
					parent = work.getSatuanKerja();
				}

				Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
				if (parent != null) {
					satuanKerjas.clear();
					satuanKerjas.add(parent);
					satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
				}

				Common.insertCombo(jenisUangMuka, new String[] { "kode", "nama" }, "keterangan", JenisUangMuka.class,
						Restrictions.and(Restrictions.eq("aktif", true),
								Restrictions.or(Restrictions.isNull("satuanKerja"),
										satuanKerjas.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(
														parent == null ? Restrictions.isNull("satuanKerja")
																: Restrictions.sqlRestriction("false"),
														Restrictions.in("satuanKerja", satuanKerjas)))));

				if (uangMuka.getId() == null && parent != null && parent.getId() != null) {
					uangMuka.setSatuanKerja(parent);
				}

				Common.selectComboItem(true, jenisUangMuka, uangMuka.getJenisUangMuka());
			}
		};

		jenisUangMuka.setWidth("90%");

		satuanKerja.setEventListener(eventListenerJenisUangMuka);

		eventListenerJenisUangMuka.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kegiatan *"));
		mulai = new MyDatebox(uangMuka.getMulai());
		sampai = new MyDatebox(uangMuka.getSampai());

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		if (persetujuan || setujui || viewOnly) {

			hbox.appendChild(
					new Label(uangMuka.getMulai() == null ? "" : Common.dateFormat1.get().format(uangMuka.getMulai())));
			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
			hbox.appendChild(new Label(
					uangMuka.getSampai() == null ? "" : Common.dateFormat1.get().format(uangMuka.getSampai())));
		} else {
			hbox.appendChild(mulai);
			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
			hbox.appendChild(sampai);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Laporan *"));
		selesai = new Label(
				uangMuka.getSelesai() == null ? "" : Common.dateFormat2.get().format(uangMuka.getSelesai()));

		row.appendChild(selesai);

		mulai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				int tglpengajuan = 14;
				try {
					tglpengajuan = Integer.parseInt(Common
							.getKonfigurasi("tgl_laporan_pengajuan_uang_muka", tglpengajuan + "").getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/UangMukaAction.java:1613");
					// TODO: handle exception
				}

				if (sampai.getValue() != null && mulai.getValue() != null
						&& mulai.getValue().after(sampai.getValue())) {
					sampai.setValue(mulai.getValue());
				}

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(sampai.getValue());
				calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + tglpengajuan);

				uangMuka.setMulai(mulai.getValue());
				uangMuka.setSampai(sampai.getValue());

				selesai.setValue(
						uangMuka.getSelesai() == null ? "" : Common.dateFormat2.get().format(uangMuka.getSelesai()));

			}
		});

		sampai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				int tglpengajuan = 14;
				try {
					tglpengajuan = Integer.parseInt(Common
							.getKonfigurasi("tgl_laporan_pengajuan_uang_muka", tglpengajuan + "").getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/UangMukaAction.java:1644");
					// TODO: handle exception
				}

				if (sampai.getValue() != null && mulai.getValue() != null
						&& mulai.getValue().after(sampai.getValue())) {
					sampai.setValue(mulai.getValue());
				}

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(sampai.getValue());
				calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + tglpengajuan);

				uangMuka.setMulai(mulai.getValue());
				uangMuka.setSampai(sampai.getValue());

				selesai.setValue(
						uangMuka.getSelesai() == null ? "" : Common.dateFormat2.get().format(uangMuka.getSelesai()));

			}
		});

		Common.initKeterangan(rows, "* Tanggal laporan tidak boleh lebih dari 7 hari dari tanggal kegiatan");

		final EventListener eventListenerE = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowSatker.setVisible(tanpaAnggaran.isChecked() || ambilDariPr.isChecked());
				// FIX (ERROR NullPointerException): dalam mode persetujuan/setujui/viewOnly,
				// komponen berikut TIDAK selalu di-appendChild ke row (diganti Label seperti
				// tanpaAnggaran di bawah), sehingga getParent() null -- sama seperti alasan
				// tanpaAnggaran.getParent() sudah digerbangi lebih dulu. Digerbangi juga di
				// sini agar konsisten, bukan cuma menunggu tertangkap di try/catch di bawah.
				boolean visibleAnggaran = !tanpaAnggaran.isChecked() && !ambilDariPr.isChecked();
				if (unit.getParent() != null) unit.getParent().setVisible(visibleAnggaran);
				if (saldo.getParent() != null) saldo.getParent().setVisible(visibleAnggaran);
				if (uangMukaDalamProses.getParent() != null) uangMukaDalamProses.getParent().setVisible(visibleAnggaran);
				if (tgl.getParent() != null) tgl.getParent().setVisible(visibleAnggaran);
				if (akun.getParent() != null) akun.getParent().setVisible(visibleAnggaran);
				if (uangMukaDalamProsesDetail.getParent() != null) {
					uangMukaDalamProsesDetail.getParent().setVisible(visibleAnggaran);
				}
				rowAkunPilih.setVisible(tanpaAnggaran.isChecked() && !ambilDariPr.isChecked());
				if (sisaAnggaran.getParent() != null) sisaAnggaran.getParent().setVisible(visibleAnggaran);

				/* Perbaikan NullPointerException yang berulang di log: kedua komponen ini bisa
				 * BELUM terpasang ke parent-nya saat listener ini jalan (workspace dibuat lewat
				 * new AmbilDataWorkspaceBanbox(...) di kode, bukan dari ZUL, dan pada mode
				 * persetujuan/viewOnly blok yang memuatnya tidak selalu dirender). getParent()
				 * lalu bernilai null dan .setVisible() melempar NPE -- tertangkap catch di bawah
				 * sehingga tidak merusak layar, tetapi TERCATAT ke ErrorAuditUtil pada setiap
				 * perubahan checkbox sehingga membanjiri log. Dijaga dengan pola null-check yang
				 * sama seperti baris-baris di atasnya (unit/saldo/tgl/akun/sisaAnggaran).
				 * try/catch tetap dipertahankan sebagai jaring pengaman. */
				try {
					if (workspace != null && workspace.getParent() != null) {
						workspace.getParent().setVisible(!tanpaAnggaran.isChecked() && !ambilDariPr.isChecked());
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/UangMukaAction.java:1686");
				}

				try {
					if (permintaanPengadaanMasterAsset != null
							&& permintaanPengadaanMasterAsset.getParent() != null) {
						permintaanPengadaanMasterAsset.getParent().setVisible(ambilDariPr.isChecked());
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/UangMukaAction.java:1692");
				}

				try {

					// Dalam mode persetujuan/setujui/viewOnly, tanpaAnggaran tidak pernah
					// di-appendChild ke row (diganti Label) sehingga getParent() null.
					if (tanpaAnggaran.getParent() != null) {
						tanpaAnggaran.getParent()
								.setVisible(!ambilDariPr.isChecked()
										&& Common.bolehKonfigurasi("tampilkan_tanpa_anggaran"));
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/UangMukaAction.java:1701");
					// TODO: handle exception
				}
			}
		};

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListenerE.onEvent(arg0);

				Workspace work = (Workspace) workspace.getAttribute("workspace");

				if (work != null && kode.getValue().trim().isEmpty()) {
					kode.setValue(work.getKode());
				}
				if (work != null && nama.getValue().trim().isEmpty()) {
					nama.setValue(work.getNama());
				}

				if (work != null && mulai.getValue() == null) {
					mulai.setValue(work.getMulai());
				}

				unit.setValue(work == null || work.getSatuanKerja() == null ? "" : work.getSatuanKerja().getNama());

				akun.setValue(work == null || work.getAkun() == null ? ""
						: work.getAkun().getKode() + "-" + work.getAkun().getNama());

				saldo.setValue(work == null ? "" : Common.numberFormat.get().format(work.getHargaTotal()));

				tgl.setValue((work == null || work.getMulai() == null ? ""
						: Common.dateFormat1.get().format(work.getMulai()))
						+ (work == null || work.getSelesai() == null ? ""
								: " s.d " + Common.dateFormat1.get().format(work.getSelesai())));

				eventListenerJenisUangMuka.onEvent(arg0);

				if (ambilDariPr.isChecked() && uangMuka.getId() == null) {
					Double n = 0.0;

					for (PermintaanPengadaanMasterAssetDetail assetDetail : UangMukaAction.this.permintaanPengadaanMasterAssets) {
						Double k = assetDetail.getJumlah() * assetDetail.getHargaBeli();
						n += k;
					}

					nilai.setValue(n);
					uangMuka.setNilai(n);
				}

				Double saldoSekarang = JenisUangMukaAction.hitungSaldo(uangMuka.getId(), null, null, null, work,
						uangMuka.getTanggalPembuatan());
				Double dalamProses = (work == null ? 0.0 : work.getHargaTotal()) - saldoSekarang;

				uangMukaDalamProses.setValue(Common.numberFormat.get().format(dalamProses));

				sisaAnggaran.setValue(Common.numberFormat.get().format(saldoSekarang));

				JenisUangMukaAction.tampilkan(uangMukaDalamProsesDetail, uangMuka.getId(), null, null, work,
						uangMuka.getTanggalPembuatan());

			}
		};

		workspace.setEventListener(eventListener);
		tanpaAnggaran.addEventListener("onClick", eventListener);
		ambilDariPr.addEventListener("onClick", eventListener);

		eventListenerE.onEvent(null);

		nilai = new MyDoublebox(uangMuka.getNilai());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Pengajuan"));

		// Kunci nominal saat approver/setujui/view-only, KECUALI saat REVISI (pemohon boleh ubah nominal).
		if (!revisi && (persetujuan || setujui || viewOnly)) {
			row.appendChild(new Label(
					uangMuka.getNilai() == null ? "" : Common.numberFormat.get().format(uangMuka.getNilai())));
		} else {
			row.appendChild(nilai);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan Oleh"));
		row.appendChild(new Label(uangMuka.getDibuatOleh() == null ? "" : uangMuka.getDibuatOleh().getUserNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan Tanggal"));
		row.appendChild(new Label(uangMuka.getTanggalPembuatan() == null ? ""
				: Common.dateFormat1.get().format(uangMuka.getTanggalPembuatan())));

		if (uangMuka.getDanaTalangan() != null && uangMuka.getDanaTalangan().getDisetujuiOleh() != null) {

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kasbon"));
			row.appendChild(
					new Label(uangMuka.getDanaTalangan().getKode() + " - " + uangMuka.getDanaTalangan().getNama()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Kasbon"));
			row.appendChild(new Label(uangMuka.getDanaTalangan().getNilai() == null ? ""
					: Common.numberFormat.get().format(uangMuka.getDanaTalangan().getNilai())));

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
		Common.selectRadioItem(status, uangMuka.getStatus());
		row.appendChild(status);

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("selesai");
					if (selesai != null && selesai) {
						Common.selectRadioItem(status, UangMuka.DISETUJU);
						Common.freeze(status, true);

//						rowAkun.setVisible(true);
					} else {
						status.setSelectedItem(null);
						Common.freeze(status, false);

//						rowAkun.setVisible(false);
					}
				}
			}
		});

		if (setujuiData != null) {
			status.addEventListener("onClick", setujuiData);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setujuiData.onEvent(new Event("", null, uangMuka.getStatus().equals(UangMuka.DISETUJU)));
				}
			});
		}

		if (setujui) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
			row.appendChild(new ais.ui.util.MyLabelConfig(uangMuka.getStatus()));
		}

		rowAkun = new MyFormRow();
		rowAkun.setVisible(persetujuan || setujui);
		rowAkun.setParent(rows);
		rowAkun.appendChild(new ais.ui.util.MyLabelConfig("Akun Penerima *"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Persetujuan"));
		tanggalPersetujuanManual = new MyDatebox(uangMuka.getTanggalPersetujuanManual());
		if (uangMuka.getPostingHistory() == null) {
			row.appendChild(tanggalPersetujuanManual);
		} else {
			row.appendChild(new Label(
					Common.dateFormat1.get().format(uangMuka.getTanggalPersetujuanManual() == null ? WaktuUtil.getDate()
							: uangMuka.getTanggalPersetujuanManual())));
		}
		tanggalPersetujuanManual.setReadonly(true);
		tanggalPersetujuanManual.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (uangMuka != null && uangMuka.getId() != null) {
					uangMuka.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
					Common.refreshUpdate(uangMuka);
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		keterangan = new Textbox(uangMuka.getKeterangan() == null ? "" : uangMuka.getKeterangan());

		if (setujui || viewOnly) {
			row.appendChild(new Label(uangMuka.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(3);

		/**
		 * Event listener lokal milik {@link UangMukaAction}. Kelas ini menangani event untuk komponen induk dan
		 * meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link UangMukaAction} dan dapat mengakses state
		 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code getThis()}, {@code onEvent}().
		 * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see UangMukaAction
		 */
		class PermintaanBarangEventListener implements EventListener {

			private PermintaanBarangEventListener getThis() {
				return this;
			}

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(subPermintaanPengadaanMasterAsset);

				if (UangMukaAction.this.permintaanPengadaanMasterAssets != null) {

					Map<Long, PermintaanPengadaanMasterAsset> lists = new HashMap<Long, PermintaanPengadaanMasterAsset>();
					for (PermintaanPengadaanMasterAssetDetail assetDetail : UangMukaAction.this.permintaanPengadaanMasterAssets) {
						lists.put(assetDetail.getPermintaanPengadaanMasterAsset().getId(),
								assetDetail.getPermintaanPengadaanMasterAsset());
					}

					Columns columns = new Columns();
					columns.setParent(subPermintaanPengadaanMasterAsset);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

					MyColumnConfig column = new MyColumnConfig("Data Permintaan Pembelian");
					column.setParent(columns);

					if (!persetujuan) {
						column = new MyColumnConfig("Batal");
						column.setParent(columns);
						column.setWidth("15%");
					}

					Rows rows = new Rows();
					rows.setParent(subPermintaanPengadaanMasterAsset);

					for (final PermintaanPengadaanMasterAsset k : lists.values()) {

						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);

						A a = new A(k.getKode() + "-" + k.getKeterangan()
								+ (k.getDisetujuiOleh() == null ? ""
										: " (" + k.getDisetujuiOleh().getUserNama() + " "
												+ Common.dateFormat51.get().format(k.getTanggalPersetujuan()) + ")"));
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								PermintaanPengadaanMasterAssetAction.onAddExternal(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

									}
								}, k);

							}
						});
						a.setStyle("font-size:10px;");
						a.setParent(row);

						if (!persetujuan) {
							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
							button.setTooltiptext("Hapus Data");
							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
											MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
											MyMessageboxConfig.QUESTION, new EventListener() {

												@Override
												public void onEvent(Event event) throws Exception {
													int i = Integer.parseInt(event.getData().toString());
													if (i == MyMessageboxConfig.OK) {
														try {

															List<PermintaanPengadaanMasterAssetDetail> assetDetails = new ArrayList<PermintaanPengadaanMasterAssetDetail>();
															for (PermintaanPengadaanMasterAssetDetail assetDetail : UangMukaAction.this.permintaanPengadaanMasterAssets) {
																if (!assetDetail.getPermintaanPengadaanMasterAsset()
																		.getId().equals(k.getId())) {
																	assetDetails.add(assetDetail);
																}
															}

															UangMukaAction.this.permintaanPengadaanMasterAssets.clear();
															UangMukaAction.this.permintaanPengadaanMasterAssets
																	.addAll(assetDetails);

															Common.createDefaultTimer(getThis());

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
							button.setParent(row);
						}
					}
				}

				eventListener.onEvent(null);
			}

		}

		final PermintaanBarangEventListener permintaanBarangEventListener = new PermintaanBarangEventListener();

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<PermintaanPengadaanMasterAsset> permintaanPengadaanMasterAssets = new ArrayList<PermintaanPengadaanMasterAsset>();

						AmbilDataPermintaanPengadaanMasterAssetBanyak ambilPermintaanPengadaanMasterAsset = new AmbilDataPermintaanPengadaanMasterAssetBanyak(
								false, permintaanPengadaanMasterAssets,
								(SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
								.appendChild(ambilPermintaanPengadaanMasterAsset);
						ambilPermintaanPengadaanMasterAsset.setWidth("90%");
						ambilPermintaanPengadaanMasterAsset.setHeight("90%");

						ambilPermintaanPengadaanMasterAsset.setEventListener(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								UangMukaAction.this.permintaanPengadaanMasterAssets
										.addAll((List<PermintaanPengadaanMasterAssetDetail>) arg0.getData());

								permintaanBarangEventListener.onEvent(arg0);
							}
						});

						ambilPermintaanPengadaanMasterAsset.onModal();
					}
				});
			}
		});

		Common.createDefaultTimer(permintaanBarangEventListener);
		if (uangMuka.getPostingHistory() == null) {
			rowAkun.appendChild(jenisUangMuka);
		} else {
			rowAkun.appendChild(
					new Label(uangMuka.getJenisUangMuka() == null ? "" : uangMuka.getJenisUangMuka().getNama()));
		}

		jenisUangMuka.setReadonly(true);

		EventListener s = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean setujui = status.getSelectedItem() == null ? false
						: status.getSelectedItem().getValue().equals(DanaTalangan.DISETUJU);
//				rowAkun.setVisible(setujui);

				if (tanggalPersetujuanManual != null && tanggalPersetujuanManual.getParent() != null) {
					if (tanggalPersetujuanManual.getValue() == null) {
						tanggalPersetujuanManual.setValue(WaktuUtil.getDate());
					}
					tanggalPersetujuanManual.getParent().setVisible(setujui);
				}

				if (UangMukaAction.this.disposisiSop != null && UangMukaAction.this.disposisiSop.getId() != null) {

					if (UangMukaAction.this.disposisiSop.getDisposisiEnd() != null
							&& UangMukaAction.this.disposisiSop.getDisposisiEnd().getSelesai()) {
						if (UangMukaAction.this.uangMuka.getDanaTalangan() != null
								&& UangMukaAction.this.uangMuka.getDanaTalangan().getDisetujuiOleh() != null) {
							save.setLabel("Transfer Uang Muka dan kembalikan Kasbon");
						} else {
							save.setLabel("Transfer Uang Muka");
						}
					} else {
						save.setLabel(!persetujuan ? "Ajukan dan Cetak" : "Ubah Status Persetujuan dan Cetak");
					}

				} else {

					if (setujui) {

						if (UangMukaAction.this.uangMuka.getDanaTalangan() != null
								&& UangMukaAction.this.uangMuka.getDanaTalangan().getDisetujuiOleh() != null) {
							save.setLabel("Transfer Uang Muka dan kembalikan Kasbon");
						} else {
							save.setLabel("Transfer Uang Muka");
						}
					} else {
						save.setLabel(!persetujuan ? "Ajukan dan Cetak" : "Ubah Status Persetujuan dan Cetak");
					}
				}
			}
		};

		status.addEventListener("onClick", s);
		Common.createDefaultTimer(s);

		return grid;
	}

	/**
	 * <h3>init(UangMuka) — Membangun Layout Window Form Uang Muka</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Method private yang membangun struktur layout lengkap window form uang muka:
	 * Borderlayout dengan Center berisi form dan South berisi toolbar simpan/batal.
	 * Dipanggil dari {@code init(GeneralValueObject)} dan {@code onAdd}.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Langkah-langkah:</p>
	 * <ol>
	 *   <li>Buat SatuanKerjaTreeModel bila belum ada.</li>
	 *   <li>Set judul window: "Pengajuan Uang Muka" atau "Persetujuan Uang Muka".</li>
	 *   <li>Bersihkan window dengan {@code Common.clear}.</li>
	 *   <li>Buat Borderlayout, Center (flex), South (flex) dengan toolbar.</li>
	 *   <li>Buat tombol simpan dengan label default "Ajukan dan Cetak" atau
	 *       "Ubah Status Persetujuan dan Cetak" sesuai mode.</li>
	 *   <li>Reset {@code disposisiSop = null} sebelum memanggil {@code form()}.</li>
	 *   <li>Pasang form ke Center, Borderlayout ke window.</li>
	 *   <li>Buat tombol Batal (tutup window) dan tombol Simpan (memanggil onSave,
	 *       bila true tutup window dan refresh grid).</li>
	 *   <li>Bila mode "sudah disetujui dan bukan persetujuan": sembunyikan simpan,
	 *       ubah label batal ke "Tutup".</li>
	 *   <li>Bila viewOnly: sembunyikan simpan, ubah label batal ke "Tutup", dan
	 *       bila uang muka sudah ada ID-nya tambahkan tombol "Cetak" terpisah.</li>
	 * </ol>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param uangMuka Entitas {@code UangMuka} yang akan ditampilkan atau diedit.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Window dibangun siap untuk di-modal-kan oleh pemanggil.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil. Error umum: Hibernate session tidak aktif saat
	 * {@code form()} memuat data relasi.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Harus dipanggil dari thread event ZK.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Perbedaan utama dengan {@code PertangungjawabanAction.init}: di sini terdapat
	 * tombol "Cetak" ekstra di mode viewOnly. Bila ada tombol baru yang perlu ditambahkan
	 * (misal: tombol "Revisi"), tambahkan di sini dengan logika visibilitas yang sesuai
	 * untuk ketiga mode.</p>
	 *
	 * @param uangMuka data uang muka yang akan diinisialisasi ke form window
	 * @throws Exception jika terjadi error saat membangun layout atau form
	 */
	private void init(UangMuka uangMuka) throws Exception {

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}

		addWindow.setTitle((!persetujuan ? "Pengajuan" : "Persetujuan") + " Uang Muka");
		Common.clear(addWindow);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig(
				!persetujuan ? "Ajukan dan Cetak" : "Ubah Status Persetujuan dan Cetak", "/img/save.gif");

		disposisiSop = null;
		center.appendChild(form(uangMuka, disposisiSop, save, null));

		borderlayout.setParent(addWindow);

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

		if (viewOnly) {
			save.setVisible(false);
			cancel.setLabel("Tutup");

			if (UangMukaAction.this.uangMuka.getId() != null) {
				MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak", "/img/cancel.gif");
				cetak.setTooltiptext("Cetak");
				cetak.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						cetak(UangMukaAction.this.uangMuka);
					}
				});
				cetak.setParent(toolbar);
			}
		}
	}

	/**
	 * <h3>onSave — Validasi dan Simpan Data Uang Muka</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Melakukan validasi input form secara menyeluruh, menyimpan atau memperbarui
	 * entitas {@code UangMuka} ke database, mengupdate relasi dengan detail PR yang
	 * dipilih, membuat {@code DaftarPengajuanTransfer} bila disetujui, memicu cetak
	 * laporan otomatis, dan memanggil callback eventListener bila ada.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Validasi yang dilakukan sebelum simpan (menampilkan Messagebox dan return false
	 * bila gagal):</p>
	 * <ul>
	 *   <li>Satuan Kerja wajib bila tanpaAnggaran atau ambilDariPr dicentang.</li>
	 *   <li>Judul Pengajuan wajib diisi.</li>
	 *   <li>Akun wajib bila tanpaAnggaran dicentang.</li>
	 *   <li>Workspace wajib bila bukan tanpaAnggaran dan bukan ambilDariPr.</li>
	 *   <li>JenisUangMuka wajib dipilih bila mode persetujuan.</li>
	 *   <li>Tanggal Mulai dan Sampai wajib diisi.</li>
	 *   <li>Tanggal Laporan (selesai) wajib diisi.</li>
	 *   <li>Nilai pengajuan wajib diisi.</li>
	 *   <li>Workspace wajib dipilih (redundan, double check).</li>
	 *   <li>Bila konfigurasi {@code saldo_harus_cukup}: saldo anggaran harus >= nilai
	 *       yang diajukan, dihitung via {@code JenisUangMukaAction.hitungSaldo}.</li>
	 *   <li>Bila ambilDariPr: daftar PR harus tidak kosong.</li>
	 * </ul>
	 * <p>Setelah validasi lolos:</p>
	 * <ol>
	 *   <li>Load ulang entitas dari session bila ID sudah ada.</li>
	 *   <li>Set semua field entitas dari komponen UI termasuk saldo, akun, PR list,
	 *       dan konfigurasi tanpaAnggaran/ambilDariPr.</li>
	 *   <li>Bila status DISETUJU: set disetujuiOleh dan tanggal persetujuan.
	 *       Bila tidak: clear keduanya.</li>
	 *   <li>Simpan (save baru dengan kode baru atau update) ke session dan flush.</li>
	 *   <li>Timer async: update FK uangMuka di setiap assetDetail PR yang dipilih,
	 *       buat DaftarPengajuanTransfer bila disetujui, panggil eventListener
	 *       callback, dan cetak laporan otomatis.</li>
	 * </ol>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param event Event dari tombol simpan ZK. Tidak digunakan secara langsung.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return {@code true} bila simpan berhasil, {@code false} bila validasi gagal.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Validasi tampilkan Messagebox dan return false. Exception Hibernate dilempar
	 * ke pemanggil (event handler simpan).</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil dari thread event ZK. Timer post-save async dieksekusi di thread
	 * event queue ZK. Operasi Hibernate sinkron dalam satu transaksi per request.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Perhatikan bahwa kode uang muka di-generate ulang saat simpan baru dengan
	 * {@code generateCode(true)} untuk memastikan index di-increment. Saldo disimpan
	 * dari nilai {@code sisaAnggaran} label (hasil kalkulasi sebelumnya) atau
	 * dihitung ulang bila parsing gagal. Ini adalah safety fallback penting.</p>
	 *
	 * @param event event ZK dari tombol simpan
	 * @return true jika berhasil disimpan, false jika validasi gagal
	 * @throws Exception jika terjadi error pada operasi Hibernate atau proses terkait
	 */
	public boolean onSave(Event event) throws Exception {
		if (satuanKerja.getAttribute("satuanKerja") == null && (tanpaAnggaran.isChecked() || ambilDariPr.isChecked())) {
			MyMessageboxConfig.show("Mohon maaf, Satuan Kerja belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Satuan Kerja dari field pencarian yang tersedia; (2) Pastikan data Satuan Kerja sudah tercatat di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Judul Pengajuan belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Judul Pengajuan dengan deskripsi singkat yang jelas; (2) Pastikan judul tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (akun.getAttribute("akun") == null && tanpaAnggaran.isChecked()) {
			MyMessageboxConfig.show("Mohon maaf, Akun belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Akun melalui field pencarian akun yang tersedia; (2) Pastikan akun yang sesuai sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (workspace.getAttribute("workspace") == null && !tanpaAnggaran.isChecked() && !ambilDariPr.isChecked()) {
			MyMessageboxConfig.show("Mohon maaf, Anggaran belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Anggaran dari field pencarian yang tersedia; (2) Pastikan anggaran yang dibutuhkan sudah terdaftar di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (persetujuan) {
			if (jenisUangMuka.getSelectedItem() == null || jenisUangMuka.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Mohon maaf, Akun Penerima belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Akun Penerima dari dropdown Jenis Uang Muka yang tersedia; (2) Pastikan jenis uang muka yang memiliki akun penerima sudah dikonfigurasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		if (mulai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Mulai belum diisi. Langkah yang dapat dilakukan: (1) Pilih Tanggal Mulai menggunakan date picker; (2) Pastikan tanggal mulai tidak melebihi tanggal sampai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (sampai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Sampai belum diisi. Langkah yang dapat dilakukan: (1) Pilih Tanggal Sampai menggunakan date picker; (2) Pastikan tanggal sampai tidak lebih awal dari tanggal mulai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (selesai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Laporan belum diisi. Langkah yang dapat dilakukan: (1) Pilih Tanggal Laporan menggunakan date picker; (2) Pastikan tanggal laporan valid sesuai periode berjalan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (nilai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Nilai belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nilai dengan nominal pengajuan uang muka yang valid; (2) Pastikan nilai tidak kosong dan lebih dari nol; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (!tanpaAnggaran.isChecked() && !ambilDariPr.isChecked()) {
			Workspace workValidasi = workspace == null ? null : (Workspace) workspace.getAttribute("workspace");
			if (workValidasi == null) {
				MyMessageboxConfig.show("Mohon maaf, Anggaran/Workspace belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Anggaran atau Workspace dari field pencarian yang tersedia; (2) Pastikan anggaran yang dibutuhkan sudah terdaftar dan memiliki saldo cukup; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (Common.bolehKonfigurasi("saldo_harus_cukup_sebelum_mengajukan_realisasi_anggaran")) {
				Double saldoValidasi = JenisUangMukaAction.hitungSaldo(uangMuka == null ? null : uangMuka.getId(), null,
						null, null, workValidasi, mulai.getValue());
				Double nilaiDiajukan = nilai.getValue() == null ? 0.0D : nilai.getValue();
				if (saldoValidasi.doubleValue() < nilaiDiajukan.doubleValue()) {
					MyMessageboxConfig.show("Nilai yang diajukan tidak boleh melebihi sisa saldo. Sisa saldo: "
							+ Common.numberFormat.get().format(saldoValidasi) + ", nilai pengajuan: "
							+ Common.numberFormat.get().format(nilaiDiajukan), "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (ambilDariPr.isChecked()) {
			if (UangMukaAction.this.permintaanPengadaanMasterAssets.isEmpty()) {
				MyMessageboxConfig.show("Mohon maaf, Permintaan Pengadaan barang/jasa belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Permintaan Pengadaan dari daftar yang tersedia; (2) Pastikan permintaan pengadaan sudah diajukan dan disetujui; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (uangMuka.getId() != null) {
			uangMuka = (UangMuka) session.load(UangMuka.class, uangMuka.getId());
		}

		if (uangMuka.getDibuatOleh() == null) {
			uangMuka.setDibuatOleh(tbmuser);
			uangMuka.setTanggalPembuatan(new Date());
		}

		if (disposisiSop != null && disposisiSop.getId() != null) {
			uangMuka.setDisposisiSop(disposisiSop);
		}
		uangMuka.setTanpaAnggaran(tanpaAnggaran.isChecked());
		uangMuka.setWorkspace((Workspace) (tanpaAnggaran.isChecked() ? null : workspace.getAttribute("workspace")));
		uangMuka.setKode(kode.getValue());
		uangMuka.setNama(nama.getValue());
		uangMuka.setNilai(nilai.getValue());
		uangMuka.setKeterangan(keterangan.getValue());
		uangMuka.setMulai(mulai.getValue());
		uangMuka.setStatus((String) (status.getSelectedItem() == null ? null : status.getSelectedItem().getValue()));
		uangMuka.setJenisUangMuka((JenisUangMuka) (jenisUangMuka.getSelectedItem() == null ? null
				: jenisUangMuka.getSelectedItem().getValue()));
		Double saldoUntukDisimpan = 0.0D;
		try {
			saldoUntukDisimpan = Common.numberFormat.get().parse(sisaAnggaran.getValue()).doubleValue();
		} catch (Exception e) {
			Workspace workSaldo = workspace == null ? null : (Workspace) workspace.getAttribute("workspace");
			saldoUntukDisimpan = JenisUangMukaAction.hitungSaldo(uangMuka == null ? null : uangMuka.getId(), null,
					null, null, workSaldo, mulai.getValue());
		}
		uangMuka.setSaldo(saldoUntukDisimpan);

		uangMuka.setAkun((Akun) akun.getAttribute("akun"));

		String s = "";
		String a = "";
		for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : UangMukaAction.this.permintaanPengadaanMasterAssets) {
			s += s.isEmpty() ? permintaanPengadaanMasterAssetDetail.getId().toString()
					: "," + permintaanPengadaanMasterAssetDetail.getId();

			if (permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getWorkspace() != null) {
				a += a.isEmpty()
						? permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getWorkspace()
								.getId().toString()
						: "," + permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getWorkspace()
								.getId();
			}
		}

		uangMuka.setPermintaanPengadaanMasterAssets(s);
		uangMuka.setAngarans(a);
		uangMuka.setAmbilDariPr(ambilDariPr.isChecked());
		uangMuka.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		uangMuka.setSampai(sampai.getValue());

		String sts = (String) (status.getSelectedItem() == null ? null : status.getSelectedItem().getValue());
		if (sts != null && sts.equals(DanaTalangan.DISETUJU)) {
			uangMuka.setDisetujuiOleh(tbmuser);
			uangMuka.setTanggalPersetujuan(tanggalPersetujuanManual.getValue());
		} else {
			uangMuka.setDisetujuiOleh(null);
			uangMuka.setTanggalPersetujuan(null);
		}
		uangMuka.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
		uangMuka.setStatus(sts);

		if (uangMuka.getId() != null) {
			session.update(uangMuka);
		} else {
			uangMuka.setDibuatOleh(tbmuser);
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			uangMuka.setKode(kode.getValue());
			session.save(uangMuka);
		}
		session.flush();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : UangMukaAction.this.permintaanPengadaanMasterAssets) {
					permintaanPengadaanMasterAssetDetail.setUangMuka(uangMuka);
					Common.refreshUpdate(session, permintaanPengadaanMasterAssetDetail);
				}
				session.flush();

				if (eventListener != null) {
					Common.createDefaultTimer(eventListener);
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (uangMuka.getStatus().equals(UangMuka.DISETUJU)) {
							DaftarPengajuanTransfer.simpanUangMuka(uangMuka);
						}

					}
				});

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						cetak(UangMukaAction.this.uangMuka);
					}
				}, "Proses cetak", false, 2500);

			}
		});

		return true;
	}

	/**
	 * <h3>initCriteria — Membangun Hibernate Criteria Pencarian Uang Muka</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Membangun objek {@code Criteria} Hibernate yang merepresentasikan query pencarian
	 * data uang muka berdasarkan semua filter yang aktif di UI. Method ini lebih
	 * kompleks dari counterpart-nya di {@code PertangungjawabanAction} karena
	 * mendukung lebih banyak filter: checkbox LPJ, DPC, BAST, dan filter anggaran
	 * terpisah.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Guard pertama: bila {@code searchparent} null, return null (untuk mencegah
	 * NullPointerException dalam kondisi ZUL belum siap). Kemudian:</p>
	 * <ul>
	 *   <li>Filter satuan kerja dengan hierarki via SatuanKerjaTreeModel.</li>
	 *   <li>Filter LPJ: kombinasi {@code searchtelahLpj} dan {@code searchblmLpj}:
	 *       keduanya dicentang = semua; hanya telahLpj = isNotNull(pertangungjawaban);
	 *       hanya blmLpj = isNull(pertangungjawaban); keduanya kosong = sqlRestriction("false")
	 *       yang berarti tidak tampil apapun (harus pilih salah satu).</li>
	 *   <li>Filter workspace/anggaran dari banbox {@code searchAnggaran}.</li>
	 *   <li>Filter tanggal pembuatan (SQL restriction date between).</li>
	 *   <li>Filter status dari combo.</li>
	 *   <li>Filter aktif dari checkbox.</li>
	 *   <li>Filter BAST: bila {@code searchtelahBast} dicentang, hanya tampilkan
	 *       yang punya {@code penerimaanPengadaanMasterAsset} (isNotNull).</li>
	 *   <li>Filter kode dan nama via ilike ANYWHERE.</li>
	 *   <li>Filter teks anggaran: bila diisi, join ke workspace dan filter kode/nama
	 *       workspace dengan ilike.</li>
	 *   <li>Filter DPC (Daftar Pencairan): join ke daftarPengajuanTransfer dan filter
	 *       isNotNull(prosesTransfer).</li>
	 *   <li>Filter dari PR: cek permintaanPengadaanMasterAssets tidak kosong string.</li>
	 * </ul>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param order {@code true} untuk menambahkan Order.desc("id"), {@code false}
	 *        untuk count saja (paging).</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return {@code Criteria} siap eksekusi, atau {@code null} bila searchparent null.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil. Return null bila searchparent null perlu
	 * diantisipasi oleh {@code onSearchDefault}.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Menggunakan {@code HibernateUtil.currentSession()} yang terikat ke thread.
	 * Tidak thread-safe lintas thread.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila ada filter baru (misal: filter kasbon/dana talangan), tambahkan restriction
	 * baru dengan alias yang unik untuk menghindari konflik. Perhatikan bahwa beberapa
	 * restriction menggunakan join alias (daftarPengajuanTransfer, workspace) yang
	 * hanya boleh dibuat sekali per Criteria — jangan membuat alias yang sama dua kali.</p>
	 *
	 * @param order true untuk tambahkan order DESC by ID, false untuk count saja
	 * @return Criteria dengan semua filter aktif, atau null bila searchparent null
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

		Criterion criterion = Restrictions.sqlRestriction("false");

		if (searchtelahLpj.isChecked() && searchblmLpj.isChecked()) {
			criterion = Restrictions.sqlRestriction("true");
		}

		else if (searchtelahLpj.isChecked()) {
			criterion = Restrictions.or(criterion, Restrictions.isNotNull("pertangungjawaban"));
		} else if (searchblmLpj.isChecked()) {
			criterion = Restrictions.or(criterion, Restrictions.isNull("pertangungjawaban"));
		}

		Workspace workspace = (Workspace) searchAnggaran.getAttribute("workspace");

		Criteria criteria = session.createCriteria(UangMuka.class)

				.add(workspace == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("workspace", workspace))

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

				.add(searchtelahBast.isChecked() ? Restrictions.isNotNull("penerimaanPengadaanMasterAsset")
						: Restrictions.sqlRestriction("true"))

				.add(criterion)

				.add(serachkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", serachkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(serachnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", serachnama.getValue().trim(), MatchMode.ANYWHERE));

		if (!searchanggaran.getValue().trim().isEmpty()) {
			criteria.createAlias("workspace", "workspace").add(Restrictions.or(
					Restrictions.ilike("workspace.kode", searchanggaran.getValue().trim(), MatchMode.ANYWHERE),
					Restrictions.ilike("workspace.nama", searchanggaran.getValue().trim(), MatchMode.ANYWHERE)));
		}

		if (searchtelahDpc.isChecked()) {
			criteria.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer")
					.add(Restrictions.isNotNull("daftarPengajuanTransfer.prosesTransfer"));
		}

		if (searchdariPr.isChecked()) {
			criteria.add(Restrictions.ne("permintaanPengadaanMasterAssets", ""));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));
		return criteria;
	}

	/**
	 * <h3>onSearchDefault — Eksekusi Pencarian dan Render Grid Uang Muka</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Mengimplementasikan kontrak {@code DataSearchDefault} untuk menjalankan pencarian
	 * data uang muka dan menampilkan hasilnya di grid. Method ini lebih robust dari
	 * counterpart-nya di {@code PertangungjawabanAction} karena memiliki mekanisme
	 * rollback dan retry untuk mengatasi error PostgreSQL "current transaction is
	 * aborted" yang dapat terjadi pada lingkungan produksi dengan operasi background.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method membungkus pemanggilan {@code loadGridUangMuka()} dalam try-catch.
	 * Bila berhasil, selesai. Bila gagal (umumnya karena transaksi Hibernate dalam
	 * status aborted akibat error sebelumnya di timer background), langkah recovery:</p>
	 * <ol>
	 *   <li>Ambil current session.</li>
	 *   <li>Bila ada transaksi aktif: rollback.</li>
	 *   <li>Bila session masih open: clear (menghapus semua entitas dari cache L1).</li>
	 *   <li>Bila tidak ada transaksi aktif: beginTransaction() baru agar
	 *       {@code createCriteria} bisa bekerja (ThreadLocalSessionContext memerlukan
	 *       transaksi aktif).</li>
	 *   <li>Coba {@code loadGridUangMuka()} sekali lagi.</li>
	 * </ol>
	 * <p>Exception dari percobaan kedua dibiarkan surfacing ke pemanggil dan
	 * dicetak ke stack trace.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param event Event pemicu. Boleh null bila dipanggil programatik dari timer.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Grid diperbarui sebagai efek samping.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Error pertama ditangkap dan recovery dilakukan. Error kedua di-print dan
	 * ditampilkan via {@code Common.tampilErrorJikaAdmin}. Session tidak ditutup
	 * manual karena ThreadLocalSessionContext dikelola oleh FilterJSP di akhir request.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil dari thread event ZK. Operasi Hibernate sinkron. Mekanisme recovery
	 * rollback/beginTransaction aman karena hanya satu thread event per sesi.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Mekanisme recovery ini adalah pola yang baik untuk halaman yang sering dipanggil
	 * setelah operasi background. Bila PostgreSQL diganti DBMS lain, periksa apakah
	 * error "aborted transaction" masih relevan. Untuk MySQL/MariaDB, error ini
	 * biasanya tidak terjadi karena autocommit mode berbeda.</p>
	 *
	 * @param event event pemicu pencarian, boleh null
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		try {
			loadGridUangMuka();
		} catch (Exception e) {
			/*
			 * Pencegahan error PostgreSQL: current transaction is aborted.
			 * Jika sebelumnya ada proses background/flush yang gagal, currentSession dapat
			 * berada pada transaksi abort. Rollback lalu ulangi sekali agar UI tetap tampil.
			 */
			Session session = null;
			try {
				session = HibernateUtil.currentSession();
				if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
				if (session != null && session.isOpen()) {
					session.clear();
				}
				/*
				 * Setelah rollback, transaksi lama tidak aktif lagi. ThreadLocalSessionContext
				 * MEWAJIBKAN transaksi aktif untuk createCriteria, jadi mulai transaksi baru
				 * sebelum mengulang load — kalau tidak akan muncul "createCriteria is not valid
				 * without active transaction". Transaksi ini ditutup/rollback oleh teardown
				 * FilterJSP di akhir request (currentSession tidak ditutup manual).
				 */
				if (session != null && session.isOpen()
						&& (session.getTransaction() == null || !session.getTransaction().isActive())) {
					session.beginTransaction();
				}
				loadGridUangMuka();
			} catch (Exception ex) {
				ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akunting/UangMukaAction.java:2748");
				Common.tampilErrorJikaAdmin(ex);
			} finally {
				// currentSession tidak ditutup manual.
			}
		}
	}

	/**
	 * <h3>loadGridUangMuka — Muat Data dan Render Grid Uang Muka</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Method private yang berisi logika aktual untuk menjalankan query dan merender
	 * grid uang muka. Dipisahkan dari {@code onSearchDefault} agar recovery rollback
	 * bisa memanggil ulang logika ini dengan mudah tanpa duplikasi kode.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Dua langkah:</p>
	 * <ol>
	 *   <li>Hitung jumlah total data dengan {@code initCriteria(false)} dan perbarui
	 *       paging via {@code Common.initPaging}.</li>
	 *   <li>Ambil data halaman saat ini dengan batasan {@code ROWS_COUNT_ON_PAGE}
	 *       dan offset berdasarkan halaman aktif paging.</li>
	 * </ol>
	 * <p>Hasil dibungkus dalam {@code SimpleListModel} dan di-set ke grid dengan
	 * renderer {@code UangMukaRenderer}. ZK memanggil renderer untuk setiap baris.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Grid diperbarui langsung.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception Hibernate dilempar ke pemanggil (onSearchDefault) yang menanganinya
	 * dengan mekanisme rollback/retry.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Harus dipanggil dari thread event ZK. Menggunakan currentSession Hibernate.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Method ini sengaja dibuat kecil dan fokus. Jangan tambahkan logika kompleks
	 * di sini; letakkan di {@code initCriteria} (filter) atau renderer. Bila perlu
	 * mengubah jumlah baris per halaman secara dinamis, baca dari konfigurasi sebelum
	 * memanggil setMaxResults.</p>
	 */
	@SuppressWarnings("unchecked")
	private void loadGridUangMuka() {
		Common.initPaging(initCriteria(false), paging);

		List<UangMuka> uangMuka = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(uangMuka);
		grid.setRowRenderer(new UangMukaRenderer());
		grid.setModelCheckMobile(strset);
	}

	/**
	 * <h3>istilah — Mengembalikan Nama Modul untuk SOP dan Label</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Mengimplementasikan kontrak interface {@code FormSop} untuk menyediakan nama
	 * resmi modul uang muka. Digunakan oleh framework SOP untuk menampilkan label
	 * deskriptif dalam alur persetujuan, log disposisi, notifikasi, dan tampilan
	 * riwayat proses.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Mengembalikan string literal "Pengajuan Uang Muka". Statis dan tidak bergantung
	 * pada data. Dipanggil oleh framework SOP saat perlu label human-readable untuk
	 * modul ini dalam konteks alur kerja keuangan.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return String "Pengajuan Uang Muka" sebagai nama istilah modul.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila nama modul perlu distandarisasi atau dilokalisasi, ubah string ini.
	 * Pastikan konsisten dengan label tombol dan judul halaman di file ZUL.</p>
	 *
	 * @return nama istilah modul pengajuan uang muka
	 * @throws Exception tidak akan dilempar dalam implementasi ini
	 */
	@Override
	public String istilah() throws Exception {
		return "Pengajuan Uang Muka";
	}

	/**
	 * <h3>ambil — Mengembalikan Entitas UangMuka yang Sedang Aktif</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Mengimplementasikan kontrak interface {@code FormSop} untuk menyediakan
	 * referensi ke entitas {@code UangMuka} yang sedang diproses di form. Digunakan
	 * oleh mekanisme alur SOP untuk mengaitkan disposisi dan aksi persetujuan
	 * dengan entitas yang benar.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Mengembalikan field {@code uangMuka} yang di-set saat {@code init} dipanggil.
	 * Nilai ini mencerminkan entitas yang sedang aktif di form. Bisa null bila form
	 * belum diinisialisasi atau setelah tutup window.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return Entitas {@code UangMuka} aktif atau null bila belum diinisialisasi.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Method ini tidak boleh mengembalikan objek selain UangMuka atau null untuk
	 * menghindari ClassCastException di framework SOP.</p>
	 *
	 * @return entitas UangMuka yang sedang aktif di form
	 * @throws Exception tidak akan dilempar dalam implementasi ini
	 */
	@Override
	public DataSop ambil() throws Exception {
		return uangMuka;
	}

	/**
	 * <h3>ambilClass — Mengembalikan Tipe Kelas Entitas UangMuka</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Mengimplementasikan kontrak interface {@code FormSop} untuk menyediakan
	 * referensi tipe kelas {@code UangMuka}. Digunakan oleh framework SOP dan CRUD
	 * generik untuk operasi refleksi, identifikasi tipe entitas, pembuatan query
	 * dinamis, dan pengaturan hak akses berbasis tipe.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Mengembalikan {@code UangMuka.class}. {@code @SuppressWarnings("rawtypes")}
	 * diperlukan karena return type interface menggunakan raw {@code Class} untuk
	 * kompatibilitas Java 1.5/1.6 era lama.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return {@code Class} object dari {@code UangMuka}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Hanya perlu diubah bila nama atau paket kelas entitas berubah. Untuk versi
	 * Java yang lebih baru, pertimbangkan {@code Class<UangMuka>} sebagai return type.</p>
	 *
	 * @return Class object untuk tipe entitas UangMuka
	 * @throws Exception tidak akan dilempar dalam implementasi ini
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return UangMuka.class;
	}

	/**
	 * <h3>generateCode — Menghasilkan Kode Nomor Surat Uang Muka</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Menghasilkan kode nomor surat unik untuk pengajuan uang muka baru berdasarkan
	 * konfigurasi format nomor surat yang telah ditetapkan administrator sistem.
	 * Kode ini berfungsi sebagai identifikasi dokumen yang human-readable dan
	 * digunakan sebagai referensi dalam komunikasi antar unit kerja.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Memeriksa apakah konfigurasi {@code NomorSuratAlurKeuangan.UANG_MUKA_DATA}
	 * sudah ada. Bila belum, fallback ke {@code Common.getGeneratedBarCode()}.
	 * Bila sudah:</p>
	 * <ol>
	 *   <li>Tentukan index dari konfigurasi (getGunakanIndexUrut) atau hitung
	 *       via {@code getindex}.</li>
	 *   <li>Bila {@code tambah=true}: increment index via
	 *       {@code NomorSurat.tambahIndexNomorSurat} sebelum generate agar counter
	 *       bertambah permanen di database.</li>
	 *   <li>Format kode dengan {@code format(index, tanggal)} tanpa parameter satuan
	 *       kerja (berbeda dengan generateCode di PertangungjawabanAction yang
	 *       menerima SatuanKerja).</li>
	 *   <li>Pastikan unik via {@code KodeUnikUtil.pastikanUnik}.</li>
	 * </ol>
	 * <p>Perbedaan dengan {@code PertangungjawabanAction.generateCode}: tidak ada
	 * parameter satuan kerja karena nomor surat uang muka tidak menggunakan substitusi
	 * kode satuan kerja.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param tambah {@code true} bila untuk simpan sungguhan (increment counter),
	 *        {@code false} untuk preview/display sementara.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return String kode nomor surat unik yang sudah terformat.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception tidak dilempar dari method ini dalam kondisi normal. Runtime exception
	 * dari format yang salah akan surfacing ke pemanggil.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Tidak thread-safe untuk simpan bersamaan dari pengguna berbeda. Dijamin aman
	 * dari duplikasi oleh {@code KodeUnikUtil.pastikanUnik} yang menambahkan sufiks.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Konfigurasi format ada di tabel nomor_surat yang diakses via
	 * {@code NomorSuratAlurKeuangan.UANG_MUKA_DATA}. Ubah melalui UI admin,
	 * bukan kode ini. Pastikan {@code tambah=false} saat preview dan {@code tambah=true}
	 * hanya di {@code onSave} setelah validasi lolos.</p>
	 *
	 * @param tambah true untuk increment counter nomor surat di database
	 * @return kode nomor surat yang telah diformat dan dijamin unik
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurKeuangan.UANG_MUKA_DATA == null
				|| NomorSuratAlurKeuangan.UANG_MUKA_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		NomorSurat ns = NomorSuratAlurKeuangan.UANG_MUKA_DATA.getNomorSurat();
		Long index = (tambah && ns.getGunakanIndexUrut()) ? NomorSurat.ambilLaluTambahIndexNomorSurat(ns)
				: (ns.getGunakanIndexUrut() ? ns.getNomorIndex() : getindex(ns));
		String noAgenda = ns.format(index, WaktuUtil.getDate());
		return ais.action.master.KodeUnikUtil.pastikanUnik(UangMuka.class, noAgenda);
	}

	/**
	 * <h3>getindex — Menghitung Index Berikutnya untuk Nomor Surat Uang Muka</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Menghitung nomor urut berikutnya untuk kode uang muka berdasarkan jumlah data
	 * yang sudah ada dalam scope yang ditentukan oleh konfigurasi {@code NomorSurat}.
	 * Mendukung berbagai scope penomoran: global, per tahun, per bulan, per kelompok
	 * nomor surat, atau sejak tanggal reset tertentu.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Identik dengan implementasi di {@code PertangungjawabanAction} tetapi query
	 * dilakukan terhadap entitas {@code UangMuka} bukan {@code Pertangungjawaban}.
	 * Method membuat Criteria dengan left join ke {@code nomorSuratAlurKeuangan} dan
	 * {@code nomorSurat}, menerapkan restriction scope sesuai konfigurasi, menghitung
	 * rowCount, menambahkan 1, dan mengembalikan hasilnya sebagai index berikutnya.
	 * Bila {@code nomorSurat} null, langsung return 0L tanpa query.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param nomorSurat Konfigurasi format nomor surat yang menentukan scope penomoran.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return Long index berikutnya (minimum 1L).</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception Hibernate dilempar ke pemanggil.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Menggunakan currentSession. Tidak thread-safe lintas thread; aman untuk
	 * penggunaan per-event ZK karena satu thread per sesi.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Perhatikan {@code Calendar.MONTH + 1} untuk konversi 0-based ke 1-based.
	 * Field {@code tahun} dan {@code bulan} di {@code UangMuka} harus tersimpan
	 * dengan nilai yang benar saat save agar query ini akurat.</p>
	 *
	 * @param nomorSurat konfigurasi nomor surat untuk menentukan scope urutan
	 * @return index urut berikutnya dalam scope yang ditentukan
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(UangMuka.class)
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
						.equals(Common.dateFormat8.get().format(sekarang))
						|| nomorSurat.getResetTiap().before(sekarang))
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
	 * <h3>setPersetujuan — Mengatur Mode Persetujuan Secara Programatik</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Mengimplementasikan kontrak interface {@code FormSop} untuk memungkinkan
	 * framework SOP mengatur mode persetujuan pada instance ini secara dinamis.
	 * Digunakan ketika alur SOP perlu mengubah mode form berdasarkan tahapan proses
	 * yang sedang berjalan.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Menyimpan nilai parameter ke field {@code persetujuan}. Efek perubahan baru
	 * terasa saat form diinisialisasi ulang via panggilan {@code init} atau
	 * {@code form} berikutnya. Method tidak langsung memperbarui komponen UI yang
	 * sedang tampil karena form harus dibangun ulang untuk mencerminkan perubahan mode.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param persetujuan {@code true} untuk mode persetujuan, {@code false} untuk
	 *        mode pengajuan normal.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Tidak thread-safe bila dipanggil dari thread lain saat form ditampilkan.
	 * Aman dalam penggunaan normal (dipanggil sebelum form dibangun).</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Ini adalah satu-satunya setter resmi untuk field {@code persetujuan} dari luar
	 * kelas. Jangan menambahkan logika kompleks di sini.</p>
	 *
	 * @param persetujuan true untuk mode persetujuan, false untuk mode pengajuan
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * <h3>cetakData — Menghasilkan File PDF Laporan Uang Muka untuk Ekspor</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Mengimplementasikan kontrak interface untuk menghasilkan file PDF laporan
	 * uang muka yang dapat diunduh. Dipanggil oleh mekanisme ekspor generik
	 * {@code Common.cetakData} saat pengguna mengklik tombol "Cetak Semua" atau
	 * "Ekspor" di toolbar, bukan oleh tombol cetak per baris.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Identik dengan {@code PertangungjawabanAction.cetakData} tetapi menggunakan
	 * entitas {@code UangMuka} dan template laporan "akunting/uangMuka". Meng-cast
	 * parameter ke {@code UangMuka}, membuat {@code LaporanUangMuka} dengan visibility
	 * false (tidak tampil langsung, hanya untuk generate file), kemudian memanggil
	 * {@code Report.generateFileReport} dalam format PDF. File PDF dikembalikan
	 * ke framework ekspor untuk dikirim sebagai unduhan ke browser.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param generalValueObject Entitas {@code UangMuka} yang akan dicetak. Harus
	 *        ter-load lengkap beserta relasi yang dibutuhkan template laporan.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return {@code File} objek file PDF sementara di server yang siap dikirim
	 *         sebagai unduhan.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil (framework ekspor). Error umum: template
	 * tidak ditemukan, data null reference, atau masalah I/O file.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil dari thread event ZK. Pembuatan PDF sinkron dan bisa memakan
	 * waktu beberapa detik untuk data kompleks.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Template laporan berada di folder dengan nama "akunting/uangMuka". Modifikasi
	 * format laporan cukup di template JRXML. Bila ada field baru yang perlu masuk
	 * laporan, tambahkan di {@code LaporanUangMuka.generateParameter()}.</p>
	 *
	 * @param generalValueObject entitas UangMuka yang akan dicetak menjadi PDF
	 * @return file PDF hasil generate laporan
	 * @throws Exception jika proses pembuatan laporan gagal
	 */
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		UangMuka uangMuka = (UangMuka) generalValueObject;
		LaporanUangMuka buktiPengeluaranKas = new LaporanUangMuka(uangMuka);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setVisible(false);
		File file = Report.generateFileReport(Report.PDF, buktiPengeluaranKas.generateParameter(), "akunting/uangMuka",
				ais.ui.util.WaktuUtil.getDate(), null, new Toolbar());
		return file;
	}
}
