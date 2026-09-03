package ais.action.master.akunting;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

import java.io.File;
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

import ais.action.master.akunting.helper.AmbilDataUangMukaBanbox;
import ais.action.master.dashboard.akunting.DasboardPertangungjawaban;
import ais.action.master.akunting.helper.MonitorPertangungjawabanDashboard;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.akunting.LaporanPertangungjawaban;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.Pajak;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.JenisPajakPpn;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Satuan;
import ais.database.model.rab.SatuanKerja;
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
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h3>PertangungjawabanAction — Pengelola Pertanggungjawaban Uang Muka</h3>
 *
 * <p><strong>Untuk apa:</strong><br>
 * Kelas ini merupakan Action (komposer ZK) yang mengelola seluruh siklus hidup
 * dokumen pertanggungjawaban uang muka di sistem keuangan eCampus. Pertangungjawaban
 * adalah laporan realisasi penggunaan dana uang muka yang sebelumnya telah disetujui
 * dan dicairkan kepada pemohon. Melalui halaman ini, staf keuangan dan pemegang uang
 * muka dapat membuat, mengubah, menghapus, menyetujui, dan mencetak dokumen LPJ
 * (Laporan Pertanggungjawaban).</p>
 *
 * <p><strong>Cara kerja:</strong><br>
 * Kelas ini mewarisi {@code GenericAutowireComposer} dari ZK Framework 5.5 sehingga
 * komponen UI yang dideklarasikan di file ZUL secara otomatis di-wire ke field Java
 * berdasarkan nama komponen. Alur kerja utama:</p>
 * <ol>
 *   <li>Pengguna membuka halaman; {@code doAfterCompose} dipanggil, menginisialisasi
 *       filter (combo status, datebox rentang tanggal, banbox satuan kerja), tombol
 *       aksi toolbar, dan timer pencarian otomatis.</li>
 *   <li>Pencarian dilakukan via {@code onSearchDefault} yang memanggil
 *       {@code initCriteria} dan menampilkan hasilnya di {@code MyGrid} dengan
 *       renderer {@code PertangungjawabanRenderer}.</li>
 *   <li>Renderer menampilkan setiap baris dengan rincian formula (JSONArray biaya,
 *       qty, harga, PPh, PPN) dalam grid bertingkat, tombol cetak, dan tombol CRUD.</li>
 *   <li>Form tambah/ubah dibuka via {@code init} yang memanggil {@code form} untuk
 *       membangun MyGrid form dinamis beserta semua field, radiogroup status, dan
 *       tombol simpan/batal.</li>
 *   <li>Penyimpanan dilakukan di {@code onSave} dengan validasi lengkap sebelum
 *       persist ke Hibernate session. Setelah simpan, pajak (PPh) dibuat via
 *       {@code Pajak.buat} dan {@code DaftarPengajuanTransfer} terbentuk otomatis
 *       bila sudah disetujui.</li>
 * </ol>
 *
 * <p><strong>Mode persetujuan:</strong><br>
 * Konstruktor menerima parameter boolean {@code persetujuan}. Bila true, tombol
 * tambah tersembunyi, form menjadi read-only untuk field utama, dan hanya
 * radiogroup status yang bisa diubah (untuk menyetujui/menolak LPJ). Mode ini
 * diaktifkan oleh halaman persetujuan atau oleh SOP alur keuangan.</p>
 *
 * <p><strong>Formula Biaya (JSONArray):</strong><br>
 * Rincian biaya disimpan sebagai JSON string di kolom {@code formula} entitas
 * {@code Pertangungjawaban}. Setiap item JSON berisi: nama, qty, harga, ppn (persen),
 * pajak (id JenisPajakBarang/PPh), ntpn, npwp, namaWp, tanggalStor, id_file (lampiran),
 * dan berbagai nilai turunan. Method {@code reloadFormula} dan {@code reloadDataFormula}
 * membangun ulang UI tabel biaya secara dinamis dari array ini.</p>
 *
 * <p><strong>Threading:</strong><br>
 * Kelas ini adalah <em>stateful per-sesi ZK</em>. Setiap pengguna memiliki instance
 * sendiri yang diciptakan per compose. Tidak ada sharing state antar pengguna. Method
 * yang memanggil Hibernate menggunakan {@code HibernateUtil.currentSession()} yang
 * terikat ke thread melalui ThreadLocal. Operasi async (timer ZK) menggunakan
 * {@code Common.createDefaultTimer} sehingga aman terhadap konteks UI ZK.</p>
 *
 * <p><strong>Pemeliharaan:</strong><br>
 * Bila ada perubahan field entitas {@code Pertangungjawaban}, perlu memperbarui
 * array {@code contents} (untuk ekspor Excel/cetak), renderer
 * {@code PertangungjawabanRenderer}, dan method {@code onSave}. Konfigurasi
 * {@code pph_mengurangi_lpj} dan {@code sponsor_tampil_lpj} diambil dari tabel
 * konfigurasi saat startup; perubahan konfigurasi baru berlaku setelah restart.</p>
 *
 * @see Pertangungjawaban
 * @see UangMukaAction
 * @see DaftarPengajuanTransfer
 * @see Pajak
 */
public class PertangungjawabanAction extends GenericAutowireComposer
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
	private Combobox searchstatus;
	private MyDatebox start;
	private MyDatebox end;
	private Textbox nama;
	private Label kode;
	private Textbox keterangan;
	private AmbilDataUangMukaBanbox uangMuka;

	public Pertangungjawaban pertangungjawaban;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;

	private Double nilai;

	private boolean persetujuan = false;

	private Tbmuser tbmuser;

	private Radiogroup status;

	private DisposisiSop disposisiSop = null;

	private JSONArray array;

	private Row rowFormula;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private boolean setujui = false;

	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private Label nilaiHarusDikembalikan;
	private MyDatebox tanggalStor;

	protected double dikembalikan = 0.0;
	protected double nilaipajak = 0.0;
	private boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");
	private Textbox namaSponsor;

	private MyDoublebox dariSponsor;

	private boolean viewOnly = false;

	/**
	 * <h3>Konstruktor Default</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Membuat instance {@code PertangungjawabanAction} untuk mode normal (bukan mode
	 * persetujuan). Mode normal memungkinkan staf untuk membuat pengajuan LPJ baru,
	 * mengubah, dan menghapus LPJ yang belum disetujui.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Konstruktor ini dipanggil oleh ZK Framework secara otomatis ketika file ZUL
	 * yang mengacu pada kelas ini di-compose tanpa parameter {@code persetujuan}
	 * dalam URL, atau ketika kode Java membuat instance baru untuk operasi non-persetujuan.
	 * Konstruktor memanggil {@code Common.getCurrentUser()} untuk mendapatkan pengguna
	 * sesi yang sedang aktif dan menyimpannya di field {@code tbmuser}. Nilai
	 * {@code persetujuan} tetap false (default) sehingga tombol "Tambah" akan tampil
	 * dan form tidak dalam mode baca saja.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil pada thread ZK request yang meng-compose halaman. Aman karena setiap
	 * pengguna mendapat instance terpisah.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Jika ada field instance tambahan yang perlu diinisialisasi di awal, inisialisasi
	 * dilakukan di sini atau di {@code doAfterCompose}, bukan di deklarasi field, agar
	 * urutan inisialisasi dapat dikontrol dengan jelas.</p>
	 */
	public PertangungjawabanAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * <h3>Konstruktor dengan Mode Persetujuan</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Membuat instance {@code PertangungjawabanAction} dalam mode persetujuan atau
	 * mode pengajuan biasa tergantung nilai parameter {@code persetujuan}. Konstruktor
	 * ini umumnya dipanggil oleh modul SOP atau dari kode Java lain yang ingin
	 * menampilkan halaman LPJ dalam konteks proses persetujuan tertentu.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Parameter {@code persetujuan} disimpan ke field instance sehingga digunakan
	 * oleh seluruh method lain dalam kelas ini untuk mengontrol visibilitas dan
	 * editabilitas komponen UI. Bila {@code persetujuan = true}: tombol tambah
	 * disembunyikan, field utama form menjadi read-only berupa Label, dan hanya
	 * radiogroup status (Setuju/Tolak) yang bisa diubah oleh approver. Setelah
	 * set flag, konstruktor memanggil {@code Common.getCurrentUser()} untuk
	 * mendapatkan pengguna aktif dari sesi saat ini.</p>
	 *
	 * <p><strong>Parameter:</strong><br>
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan (halaman
	 *        hanya untuk approver), {@code false} untuk mode pengajuan normal.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil pada thread ZK request. Aman karena setiap pengguna mendapat instance
	 * terpisah. Field {@code tbmuser} adalah pengguna yang saat ini login, bukan
	 * pengguna yang akan menyetujui.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila logika mode persetujuan berkembang (misal ada mode "hanya lihat" yang
	 * berbeda), pertimbangkan menambah konstruktor baru atau enum mode daripada
	 * menambah lebih banyak flag boolean yang sulit dibaca.</p>
	 *
	 * @param persetujuan true jika instance ini digunakan dalam konteks persetujuan LPJ
	 */
	public PertangungjawabanAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
	}

	public static String[] contents = new String[] { "id", "kode", "nama", "keterangan", "uangMuka", "formula", "nilai",
			"dariSponsor", "namaSponsor", "dibuatOleh", "disetujuiOleh", "tanggalPembuatan", "tanggalPersetujuan",
			"status", "disposisiSop", "daftarPengajuanTransfer.prosesTransfer.kode",
			"daftarPengajuanTransfer.prosesTransfer.nama", "daftarPengajuanTransfer.prosesTransfer.tanggalPembuatan",
			"daftarPengajuanTransfer.prosesTransfer.disetujuiOleh",
			"daftarPengajuanTransfer.prosesTransfer.tanggalPersetujuan",
			"daftarPengajuanTransfer.prosesTransfer.realisasikanOleh",
			"daftarPengajuanTransfer.prosesTransfer.tanggalRealisasikan", "aktif" };

	protected Tabpanel statistik;
	/** Tabpanel tab "Monitor" — dashboard pemantauan + pembayaran DPC (lazy). */
	protected Tabpanel monitor;

	private MyDatebox tanggalPersetujuanManual;

	/**
	 * <h3>onStatistik — Memuat Dasbor Statistik Pertanggungjawaban</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Menangani event klik atau aktivasi tab "Statistik" pada halaman pertanggungjawaban.
	 * Method ini memuat komponen dasbor analisis grafis pertanggungjawaban secara
	 * lazy (hanya dibuat saat pertama kali tab dibuka) sehingga tidak membebani
	 * waktu muat awal halaman.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method memeriksa apakah {@code statistik} (Tabpanel) sudah memiliki komponen
	 * anak. Bila belum ada (ukuran {@code getChildren().size() == 0}), instance baru
	 * {@code DasboardPertangungjawaban} dibuat dan dimasukkan sebagai anak dari
	 * {@code statistik}. Komponen dasbor diatur agar mengisi seluruh lebar dan tinggi
	 * panel dengan {@code setHeight("100%")} dan {@code setWidth("100%")}. Bila
	 * sudah ada anak (tab pernah dibuka sebelumnya), method tidak melakukan apa pun
	 * sehingga dasbor tidak di-reload ulang dan performanya terjaga.</p>
	 *
	 * <p><strong>Parameter:</strong><br>
	 * @param event Event ZK yang memicu method ini, biasanya event {@code onSelect}
	 *        dari Tabbox ketika pengguna mengklik tab Statistik. Parameter ini tidak
	 *        digunakan dalam implementasi saat ini namun wajib ada sesuai konvensi
	 *        event handler ZK.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Method tidak mendeklarasikan throws Exception karena {@code DasboardPertangungjawaban}
	 * adalah komponen ZK yang pembuatannya tidak melempar checked exception. Bila
	 * terjadi error runtime saat pembuatan dasbor, ZK akan menanganinya melalui
	 * exception handler global.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil pada thread ZK event queue. Aman untuk mengakses dan memodifikasi
	 * komponen UI karena berada di konteks event ZK.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila ingin memperbarui data dasbor setiap kali tab dibuka (bukan hanya sekali),
	 * hapus kondisi {@code size() == 0} dan selalu hapus anak lama dengan
	 * {@code Common.clear(statistik)} sebelum membuat instance baru. Namun ini akan
	 * meningkatkan beban query setiap klik tab.</p>
	 *
	 * @param event event ZK pemicu; tidak digunakan secara langsung
	 */
	/** Memuat dashboard "Monitor" (pemantauan pertanggungjawaban + pembayaran DPC) secara lazy. */
	public void onMonitor(Event event) {
		if (monitor != null && monitor.getChildren().size() == 0) {
			MonitorPertangungjawabanDashboard dashboard = new MonitorPertangungjawabanDashboard();
			dashboard.setHeight("100%");
			dashboard.setWidth("100%");
			dashboard.setParent(monitor);
		}
	}

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DasboardPertangungjawaban include = new DasboardPertangungjawaban();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(statistik);
		}
	}

	/**
	 * <h3>doBeforeCompose — Pemeriksaan Keamanan Sebelum Compose</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Override method lifecycle ZK yang dipanggil sebelum komponen UI di-compose dari
	 * file ZUL. Method ini memastikan bahwa pengguna memiliki hak akses yang valid
	 * sebelum halaman dibangun, mencegah akses tidak sah ke modul pertanggungjawaban.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method memanggil {@code Common.doCheckSecurity()} yang memeriksa apakah pengguna
	 * sesi saat ini memiliki privilege untuk mengakses resource yang sedang dibuka.
	 * Pemeriksaan ini dilakukan berdasarkan konfigurasi hak akses (privilege) yang
	 * terdaftar untuk URL atau modul ini. Jika pengguna tidak memiliki akses,
	 * {@code doCheckSecurity} akan mengarahkan ulang (redirect) ke halaman logoff atau
	 * halaman error akses. Setelah pemeriksaan keamanan, method memanggil implementasi
	 * induk {@code super.doBeforeCompose} untuk melanjutkan proses compose normal ZK.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param page Halaman ZK yang sedang di-compose
	 * @param parent Komponen induk tempat halaman ini akan dipasang
	 * @param compInfo Informasi metadata komponen dari ZUL parser</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * @return {@code ComponentInfo} dari implementasi induk yang dibutuhkan ZK untuk
	 *         melanjutkan proses compose.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Bila {@code doCheckSecurity} menemukan akses tidak valid, ia mengarahkan pengguna
	 * ke halaman lain dan proses compose tidak dilanjutkan. Tidak ada exception yang
	 * dilempar ke pemanggil dalam kondisi normal.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil pada thread pemrosesan HTTP request ZK, sebelum komponen apapun
	 * dibuat. Aman dan tidak ada state yang perlu disinkronkan.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Method ini adalah titik tunggal untuk menambahkan logika keamanan level halaman.
	 * Jangan menghapus panggilan {@code super.doBeforeCompose} karena akan menyebabkan
	 * compose gagal. Bila perlu menambahkan logika keamanan tambahan (misal: cek
	 * IP whitelist), lakukan sebelum panggilan super.</p>
	 *
	 * @param page halaman ZK yang sedang diproses
	 * @param parent komponen induk dalam hierarki komponen ZK
	 * @param compInfo informasi metadata komponen dari file ZUL
	 * @return ComponentInfo untuk dilanjutkan ke mekanisme compose ZK
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <h3>doAfterCompose — Inisialisasi Halaman Setelah Compose</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Method lifecycle ZK yang dipanggil setelah seluruh komponen UI dari file ZUL
	 * telah di-compose dan di-wire ke field Java. Method ini bertanggung jawab untuk
	 * menginisialisasi semua komponen UI ke kondisi awal yang benar: mengisi dropdown
	 * filter status, mengatur datebox rentang tanggal default (6 bulan terakhir hingga
	 * besok), mengkonfigurasi privilege tombol aksi, memasang event listener untuk
	 * paging, dan memulai pencarian awal via timer.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Langkah-langkah inisialisasi yang dilakukan secara berurutan:</p>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose} untuk wire komponen.</li>
	 *   <li>Memanggil {@code Common.initLaguage()} untuk inisialisasi lokalisasi.</li>
	 *   <li>Memeriksa sesi: bila {@code usersTemp} null atau tidak punya READ privilege,
	 *       pengguna di-logoff.</li>
	 *   <li>Memasang event listener pada {@code searchparent} (banbox satuan kerja)
	 *       agar pencarian otomatis dijalankan saat satuan kerja dipilih.</li>
	 *   <li>Membuat instance {@code SatuanKerjaTreeModel} untuk navigasi hierarki satuan
	 *       kerja.</li>
	 *   <li>Mengatur datebox {@code start} dan {@code end}: start = 6 bulan lalu, end
	 *       = besok. Keduanya readonly agar hanya bisa diubah lewat date picker.</li>
	 *   <li>Mengisi {@code searchstatus} dengan pilihan: Semua (null), Pengajuan,
	 *       Disetujui, dan Ditolak. Default terpilih adalah Semua.</li>
	 *   <li>Membaca parameter URL {@code persetujuan} untuk menentukan mode tampilan.</li>
	 *   <li>Mengatur visibilitas tombol tambah berdasarkan privilege CREATE dan mode.</li>
	 *   <li>Menginisialisasi paging dengan event listener yang memanggil
	 *       {@code onSearchDefault}.</li>
	 *   <li>Membuat tombol cetak data (ekspor) dan upload data via {@code Common}.</li>
	 *   <li>Membuat tombol "Hitung Ulang" yang menjalankan rekontrol nilai formula
	 *       seluruh pertanggungjawaban secara batch dalam timer async.</li>
	 *   <li>Memulai pencarian otomatis pertama via {@code Common.createDefaultTimer}.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception yang dilempar oleh {@code super.doAfterCompose} disebarkan ke pemanggil
	 * (ZK framework) yang akan menanganinya. Error pada inisialisasi individu biasanya
	 * dibungkus dalam blok try-catch inline.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil sekali saat halaman diload pada thread ZK request. Timer yang dibuat
	 * di sini akan dieksekusi pada thread event queue ZK.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila ada filter baru yang perlu ditambahkan (misalnya filter jenis pertanggungjawaban),
	 * inisialisasinya dilakukan di sini. Pastikan urutan inisialisasi dijaga: privilege
	 * harus dicek sebelum komponen diakses, datebox harus diisi sebelum pencarian
	 * pertama dijalankan.</p>
	 *
	 * @param comp komponen root yang telah di-compose dari file ZUL
	 * @throws Exception jika terjadi error pada proses inisialisasi ZK atau akses sesi
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

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

		Comboitem comboitemSemua = new Comboitem("Semua");
		if (comboitemSemua != null) { comboitemSemua.setValue(null); }
		searchstatus.appendChild(comboitemSemua);

		Comboitem comboitem = new Comboitem(Pertangungjawaban.PENGAJUAN);
		if (comboitem != null) { comboitem.setValue(Pertangungjawaban.PENGAJUAN); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(Pertangungjawaban.DISETUJU);
		if (comboitem != null) { comboitem.setValue(Pertangungjawaban.DISETUJU); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(Pertangungjawaban.DITOLAK);
		if (comboitem != null) { comboitem.setValue(Pertangungjawaban.DITOLAK); }
		searchstatus.appendChild(comboitem);

		if (searchstatus != null) { searchstatus.setSelectedItem(comboitemSemua); }
		if (searchstatus != null) { searchstatus.setReadonly(true); }

		if (execution.getParameter("persetujuan") != null) {
			boolean persetujuanDariUrl = Boolean.parseBoolean(execution.getParameter("persetujuan"));
			// Parameter URL TIDAK BOLEH menaikkan mode dari pengajuan ke persetujuan --
			// hanya menu Persetujuan (konstruktor super(true), lihat
			// PersetujuanPertangungjawabanAction) atau hak APPROVE eksplisit pada menu
			// aktif yang boleh mengaktifkannya. Mencegah eskalasi via ?persetujuan=true
			// di menu Pertanggungjawaban Uang Muka biasa.
			persetujuan = persetujuanDariUrl
					? (persetujuan || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE))
					: false;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE) && !persetujuan);
		}

		if (add != null) { add.setTooltiptext("Tambah"); }
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Pertangungjawaban.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Pertangungjawaban.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		// add bisa null bila pengguna tak punya hak tambah (Common.tambahData mengembalikan null).
		if (add != null) {
			if (persetujuan) {
				add.setVisible(false);
			} else {
				add.setLabel("Pengajuan Pertanggungjawaban Uang Muka");
			}
		}

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
						List<Pertangungjawaban> pertangungjawabans = initCriteria(false).addOrder(Order.asc("id"))
								.setMaxResults(5000).list();

						for (Pertangungjawaban pertangungjawaban : pertangungjawabans) {

							try {
								if (pertangungjawaban.getDaftarPengajuanTransfer() == null
										&& pertangungjawaban.getDisetujuiOleh() != null) {
									Session session = HibernateUtil.currentSession();
									DaftarPengajuanTransfer d = (DaftarPengajuanTransfer) session
											.createCriteria(DaftarPengajuanTransfer.class)
											.createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
													Restrictions.eq("disposisiSop.aktif", true)))
											.addOrder(Order.desc("id"))
											.add(Restrictions.eq("pertangungjawaban", pertangungjawaban))
											.setMaxResults(1).uniqueResult();
									if (d != null) {
										pertangungjawaban.setDaftarPengajuanTransfer(d);
										Common.refreshUpdate(session, pertangungjawaban);
									}
								}
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}

							Double totalPajak = 0.0;
							Double nilai = 0.0;
							JSONArray array = new JSONArray(pertangungjawaban.getFormula());
							for (int i = 0; i < array.length(); i++) {

								JSONObject jsonObject = array.getJSONObject(i);

								try {
									Pajak.buat(pertangungjawaban, null, jsonObject, null);
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}

								Double ppn = 0.0;
								if (!jsonObject.isNull("ppn")) {
									ppn = jsonObject.getDouble("ppn");
								}

								Double jumlah = 0.0;
								if (!jsonObject.isNull("jumlah")) {
									jumlah = jsonObject.getDouble("jumlah");
								}

								JenisPajakBarang barang;
								if (!jsonObject.isNull("pajak")) {
									barang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
											Long.parseLong(jsonObject.get("pajak") + ""));
								} else {
									barang = null;
								}
								Double nilaippn = ((ppn / 100.0) * jumlah);
								Double pajak = barang == null ? 0.0 : ((barang.getPersen() / 100.0) * jumlah);
								totalPajak += pajak;
								Double tot = (jumlah + nilaippn) - (pph_mengurangi_lpj ? pajak : 0.0);

								nilai += tot;

							}

							System.out.println(
									"pajak " + totalPajak + " pertangungjawaban " + pertangungjawaban.getPajak()
											+ " nilai " + nilai + " pertangungjawaban " + pertangungjawaban.getNilai());

							if (nilai.intValue() != pertangungjawaban.getNilai().intValue()
									|| totalPajak.intValue() != pertangungjawaban.getPajak().intValue()) {

								// Basisnya WAJIB nilai panjar (uangMuka.getNilai()), bukan nilai
								// realisasi LPJ lama -- rumus sama dengan layar ZK dan jalur REST,
								// lihat Pertangungjawaban.hitungDikembalikan.
								Double dikembalikan = Pertangungjawaban.hitungDikembalikan(
										pertangungjawaban.getUangMuka() == null ? 0.0
												: pertangungjawaban.getUangMuka().getNilai(),
										pertangungjawaban.getDariSponsor(), nilai);

								pertangungjawaban.setPajak(totalPajak);
								pertangungjawaban.setNilai(nilai);
								pertangungjawaban.setDikembalikan(dikembalikan);
								Session session = HibernateUtil.currentSession();
								Common.refreshUpdate(session, pertangungjawaban);
							}

						}
						onSearchDefault(null);
					}
				});

			}

		});
		if (button != null) { button.setParent(add.getParent()); }
	}

	/**
	 * <h3>PertangungjawabanRenderer — Renderer Baris Grid Pertanggungjawaban</h3>
	 *
	 * <p><strong>Untuk apa:</strong><br>
	 * Kelas inner yang mengimplementasikan {@code MyRowRenderer} untuk merender setiap
	 * baris data pertanggungjawaban dalam grid utama halaman. Setiap baris menampilkan
	 * informasi lengkap LPJ beserta sub-grid biaya, status persetujuan, dan tombol aksi.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method {@code render} dipanggil ZK untuk setiap objek {@code Pertangungjawaban}
	 * dalam model. Baris diisi dengan: kode (dengan link revisi), nama, link proses
	 * transfer (bila ada), link uang muka induk, periode, dibuat oleh+tanggal, status
	 * (dengan approver+tanggal), nama sponsor+nilai (bila ada), sub-grid formula biaya
	 * (JSONArray), nilai dikembalikan, keterangan+link SOP+status transfer, aktif/checkbox,
	 * dan tombol CRUD+cetak. Bila LPJ disetujui dan belum punya DaftarPengajuanTransfer,
	 * timer async membuat transfer secara otomatis.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil dari thread event ZK saat grid me-render model. Mengakses
	 * {@code HibernateUtil.currentSession()} untuk lazy-load bila diperlukan.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila ada kolom baru di grid, tambahkan header kolom di ZUL (kolom di file ZUL
	 * harus sesuai urutan dengan cell yang ditambahkan di sini). Perhatikan bahwa
	 * JSONArray parsing untuk formula dilakukan di sini juga untuk tampilan read-only.</p>
	 */
	class PertangungjawabanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render — Merender Satu Baris Data Pertanggungjawaban ke Grid Row</h3>
		 *
		 * <p><strong>Tujuan:</strong><br>
		 * Mengisi satu baris ({@code Row}) ZK grid dengan semua informasi dari satu
		 * entitas {@code Pertangungjawaban}. Dipanggil oleh framework ZK untuk setiap
		 * elemen dalam model saat grid di-render atau di-refresh.</p>
		 *
		 * <p><strong>Cara kerja:</strong><br>
		 * Row diisi secara berurutan sesuai kolom yang dideklarasikan di ZUL. Sel-sel
		 * yang dibuat meliputi: Vbox kode+nama+link proses transfer, Vbox uang muka+periode,
		 * Vbox dibuat oleh+tanggal, Vbox status+approver+tanggal persetujuan, Vbox sponsor,
		 * Grid sub-formula biaya (baris per item JSONArray), Label dikembalikan, Vbox
		 * keterangan+SOP+status transfer, checkbox/label aktif, dan Hbox tombol aksi.
		 * Bila disetujui tapi belum punya DaftarPengajuanTransfer, timer otomatis
		 * membuatnya via {@code DaftarPengajuanTransfer.simpanPertangungjawaban}.</p>
		 *
		 * <p><strong>Parameter:</strong>
		 * @param arg0 Row ZK yang akan diisi komponen-komponen sel.
		 * @param arg1 Object yang merupakan instance {@code Pertangungjawaban} dari model.</p>
		 *
		 * <p><strong>Penanganan error:</strong><br>
		 * Exception dilempar ke ZK framework yang menanganinya. JSONException pada
		 * parsing formula mengakibatkan baris tidak ter-render dengan benar.</p>
		 *
		 * <p><strong>Threading:</strong><br>
		 * Dipanggil dari thread event ZK. Semua operasi UI sinkron.</p>
		 *
		 * <p><strong>Pemeliharaan:</strong><br>
		 * Urutan appendChild ke arg0 harus sesuai persis dengan urutan kolom di ZUL.
		 * Bila ada kolom baru, tambahkan di posisi yang tepat di ZUL dan di sini.</p>
		 *
		 * @param arg0 baris grid ZK yang akan diisi
		 * @param arg1 entitas Pertangungjawaban yang akan ditampilkan
		 * @throws Exception jika terjadi error saat merender komponen
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Pertangungjawaban pertangungjawaban = (Pertangungjawaban) arg1;

			if (pertangungjawaban.getDibuatOleh() == null) {
				pertangungjawaban.setDibuatOleh(tbmuser);
			}

			if (pertangungjawaban.getDisetujuiOleh() != null
					&& pertangungjawaban.getDaftarPengajuanTransfer() == null) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (pertangungjawaban.getDisetujuiOleh() != null) {
							DaftarPengajuanTransfer.simpanPertangungjawaban(pertangungjawaban);
						}

					}
				});
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Pertangungjawaban.class, pertangungjawaban,
					pertangungjawaban.getKode() == null ? "" : pertangungjawaban.getKode().trim().toString()))
					.setParent(arg0);

			if (pertangungjawaban.getDaftarPengajuanTransfer() != null
					&& pertangungjawaban.getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A aaa = new A(pertangungjawaban.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				aaa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, pertangungjawaban.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				aaa.setStyle("font-size:12px;");
				aaa.setParent(a);
			}

			new Label(pertangungjawaban.getNama()).setParent(a);

			new Label(pertangungjawaban.getUangMuka() == null ? ""
					: pertangungjawaban.getUangMuka().getKode() + "-" + pertangungjawaban.getUangMuka().getNama())
					.setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label((pertangungjawaban.getUangMuka().getMulai() == null ? ""
					: Common.dateFormat1.get().format(pertangungjawaban.getUangMuka().getMulai()))).setParent(a);
			new Label((pertangungjawaban.getUangMuka().getSelesai() == null ? ""
					: " sd " + Common.dateFormat1.get().format(pertangungjawaban.getUangMuka().getSelesai()))).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label(pertangungjawaban.getDibuatOleh() == null ? "" : pertangungjawaban.getDibuatOleh().getUserNama())
					.setParent(a);
			new Label(pertangungjawaban.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(pertangungjawaban.getTanggalPembuatan())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label(pertangungjawaban.getStatus()).setParent(a);
			(new Label(pertangungjawaban.getDisetujuiOleh() == null ? ""
					: pertangungjawaban.getDisetujuiOleh().getUserNama())).setParent(a);
			(new Label(pertangungjawaban.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(pertangungjawaban.getTanggalPersetujuan()))).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label(pertangungjawaban.getNamaSponsor().isEmpty() ? "" : pertangungjawaban.getNamaSponsor())
					.setParent(a);
			(new Label(pertangungjawaban.getNamaSponsor().isEmpty() ? ""
					: Common.numberFormat.get().format(pertangungjawaban.getDariSponsor()))).setParent(a);

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setWidth("100%");
			grid.setParent(arg0);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

			MyColumnConfig column = new MyColumnConfig("Keterangan Biaya");
			column.setParent(columns);

			column = new MyColumnConfig("Qty");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig("Harga");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("20%");

			column = new MyColumnConfig("PPN");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig("PPH");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("15%");

			column = new MyColumnConfig("Jumlah");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("20%");

			Rows rows = new Rows();
			rows.setParent(grid);
			Double nilai = 0.0;
			JSONArray array = new JSONArray(pertangungjawaban.getFormula());
			for (int i = 0; i < array.length(); i++) {

				JSONObject jsonObject = array.getJSONObject(i);

				String nama = "";

				if (!jsonObject.isNull("nama")) {
					nama = jsonObject.get("nama") + "";
				}

				String ntpn = "";

				if (!jsonObject.isNull("ntpn")) {
					ntpn = jsonObject.get("ntpn") + "";
				}

				String npwp = "";

				if (!jsonObject.isNull("npwp")) {
					npwp = jsonObject.get("npwp") + "";
				}

				String namaWp = "";

				if (!jsonObject.isNull("namaWp")) {
					namaWp = jsonObject.get("namaWp") + "";
				}

				String tanggalStor = "";

				if (!jsonObject.isNull("tanggalStor")) {
					tanggalStor = jsonObject.get("tanggalStor") + "";
				}

				Double qty = 0.0;
				if (!jsonObject.isNull("qty")) {
					qty = jsonObject.getDouble("qty");
				}

				Double harga = 0.0;
				if (!jsonObject.isNull("harga")) {
					harga = jsonObject.getDouble("harga");
				}

				Double ppn = 0.0;
				if (!jsonObject.isNull("ppn")) {
					ppn = jsonObject.getDouble("ppn");
				}

				Double jumlah = 0.0;
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}

				JenisPajakBarang barang;
				if (!jsonObject.isNull("pajak")) {
					barang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
							Long.parseLong(jsonObject.get("pajak") + ""));
				} else {
					barang = null;
				}

				Double nilaippn = ((ppn / 100.0) * jumlah);

				Double pajak = barang == null ? 0.0 : ((barang.getPersen() / 100.0) * jumlah);

				Double tot = (jumlah + nilaippn) - (pph_mengurangi_lpj ? pajak : 0.0);

				nilai += tot;

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				row.appendChild(new MyLabelAgakKecil(nama));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(qty)));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(harga)));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(nilaippn)));
				if (barang != null) {
					row.appendChild(new Vbox(new Component[] { new MyLabelAgakKecil(Common.numberFormat.get().format(pajak)),
							new MyLabelAgakKecil(ntpn), new MyLabelAgakKecil(npwp), new MyLabelAgakKecil(namaWp),
							new MyLabelAgakKecil(tanggalStor)

					}));
				} else {
					row.appendChild(new MyLabelAgakKecil());
				}
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(tot)));
			}
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

			Footer footerTotal = new Footer(Common.numberFormat.get().format(nilai));
			foot.appendChild(footerTotal);
			pertangungjawaban.setNilai(nilai);
			new Label(Common.numberFormat.get().format(pertangungjawaban.getDikembalikan())).setParent(arg0);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new Label(Common.simpleString(pertangungjawaban.getKeterangan())).setParent(vbox1);
			if (pertangungjawaban.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pertangungjawaban.getDisposisiSop().getKeterangan() + " ("
						+ pertangungjawaban.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pertangungjawaban.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			DaftarPengajuanTransfer.tampilStatus(pertangungjawaban.getDaftarPengajuanTransfer(), vbox1);

			if (pertangungjawaban.getDisposisiSop() != null && !pertangungjawaban.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (persetujuan && !pertangungjawaban.getStatus().equals(Pertangungjawaban.DISETUJU)) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(pertangungjawaban.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pertangungjawaban.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(pertangungjawaban);
					}
				});
			} else {
				new Label(pertangungjawaban.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			Hbox hbx;
			(hbx = Common.copyEditDeleteButtons(edit,
					!persetujuan && !pertangungjawaban.getStatus().equals(Pertangungjawaban.DISETUJU),
					delete && !persetujuan && !pertangungjawaban.getStatus().equals(Pertangungjawaban.DISETUJU),
					pertangungjawaban, PertangungjawabanAction.this)).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(pertangungjawaban);
				}
			});
			button.setParent(hbx);
		}

	}

	/**
	 * <h3>cetakData — Menghasilkan File PDF Laporan Pertanggungjawaban untuk Ekspor</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Mengimplementasikan kontrak interface {@code DataCriteria} untuk menghasilkan
	 * file PDF dari data pertanggungjawaban tertentu. Method ini dipanggil oleh
	 * mekanisme ekspor generik {@code Common.cetakData} ketika pengguna mengklik
	 * tombol "Cetak Semua" atau "Ekspor" di toolbar, bukan oleh aksi cetak per baris.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method meng-cast parameter {@code generalValueObject} menjadi {@code Pertangungjawaban},
	 * kemudian membuat instance {@code LaporanPertangungjawaban} (kelas laporan JasperReports)
	 * yang diisi dengan data LPJ tersebut. Window laporan dikonfigurasi dengan judul
	 * "Laporan", tinggi 90%, lebar 900px, dan visibility false (tidak ditampilkan langsung).
	 * Selanjutnya, {@code Report.generateFileReport} dipanggil dengan format PDF,
	 * parameter dari {@code generateParameter()}, nama template
	 * "akunting/pertangungjawaban", tanggal saat ini, dan toolbar baru. File PDF yang
	 * dihasilkan dikembalikan ke pemanggil untuk dikirimkan ke browser pengguna sebagai
	 * unduhan.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param generalValueObject Objek data yang akan dicetak; harus merupakan instance
	 *        {@code Pertangungjawaban} yang valid dengan semua relasi sudah ter-load.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return {@code File} objek file PDF sementara yang telah dibuat di sistem file
	 *         server. File ini kemudian dikirim sebagai unduhan ke browser. Caller
	 *         bertanggung jawab untuk pembersihan file sementara setelah pengiriman.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil (framework ekspor generik). Error umum yang
	 * mungkin terjadi: template JasperReports tidak ditemukan, data null reference
	 * saat generate parameter, atau masalah I/O saat membuat file sementara.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil pada thread event ZK. Pembuatan file PDF adalah operasi sinkron yang
	 * dapat memakan waktu beberapa detik tergantung kompleksitas laporan.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Template laporan berada di folder report dengan nama "akunting/pertangungjawaban".
	 * Bila format laporan perlu diubah, modifikasi template JRXML tersebut, bukan method
	 * ini. Bila ada field baru yang perlu masuk ke laporan, tambahkan di
	 * {@code LaporanPertangungjawaban.generateParameter()}.</p>
	 *
	 * @param generalValueObject entitas Pertangungjawaban yang akan dicetak
	 * @return file PDF hasil generate laporan
	 * @throws Exception jika proses pembuatan laporan gagal
	 */
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		Pertangungjawaban pertangungjawaban = (Pertangungjawaban) generalValueObject;
		LaporanPertangungjawaban buktiPengeluaranKas = new LaporanPertangungjawaban(pertangungjawaban);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setVisible(false);
		File file = Report.generateFileReport(Report.PDF, buktiPengeluaranKas.generateParameter(),
				"akunting/pertangungjawaban", ais.ui.util.WaktuUtil.getDate(), null, new Toolbar());
		return file;
	}

	/**
	 * <h3>cetak — Menampilkan Laporan Pertanggungjawaban dalam Modal</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Method statis untuk menampilkan laporan pertanggungjawaban sebagai window modal
	 * dalam antarmuka ZK. Method ini dipanggil dari tombol cetak per baris di
	 * {@code PertangungjawabanRenderer} maupun dari {@code onSave} setelah simpan
	 * berhasil agar pengguna langsung melihat dan bisa mencetak dokumen LPJ-nya.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method membuat instance {@code LaporanPertangungjawaban} dan mengisi semua
	 * propertinya: judul "Laporan", closable true (bisa ditutup pengguna), tinggi 90%,
	 * lebar 900px. Kemudian window dilampirkan ke root component halaman yang sedang
	 * aktif menggunakan {@code ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()}
	 * dan langsung dibuka dalam mode modal dengan {@code onModal()}. Ini membuat tampilan
	 * laporan PDF/HTML inline di dalam antarmuka tanpa membuka tab browser baru.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param pertangungjawaban Objek {@code Pertangungjawaban} yang akan ditampilkan
	 *        laporannya. Objek ini harus sudah ter-load lengkap beserta relasi
	 *        {@code uangMuka} dan {@code formula}.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value (void). Efek samping adalah munculnya window modal laporan
	 * di browser pengguna.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil. Error umum: halaman ZK tidak aktif (jika
	 * dipanggil di luar konteks event), data LPJ null, atau template laporan tidak
	 * ditemukan.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Harus dipanggil dari thread event ZK karena memanipulasi komponen UI dan
	 * mengakses {@code ExecutionsCtrl.getCurrentCtrl()}. Tidak aman untuk dipanggil
	 * dari thread background.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Method ini bersifat statis untuk memudahkan pemanggilan dari renderer dalam
	 * kelas inner. Jika implementasi laporan berubah (misal: ganti dari modal ZK ke
	 * buka tab baru), ubah method ini saja dan semua pemanggil otomatis mengikuti.
	 * Pastikan {@code LaporanPertangungjawaban} memiliki constructor yang menerima
	 * {@code Pertangungjawaban}.</p>
	 *
	 * @param pertangungjawaban data LPJ yang akan ditampilkan dalam laporan modal
	 * @throws Exception jika gagal membuat atau menampilkan laporan
	 */
	public static void cetak(Pertangungjawaban pertangungjawaban) throws Exception {
		LaporanPertangungjawaban buktiPengeluaranKas = new LaporanPertangungjawaban(pertangungjawaban);
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
	 * Mengimplementasikan kontrak interface {@code DataInitDefault} yang memungkinkan
	 * framework generik CRUD memanggil inisialisasi form tanpa mengetahui tipe entitas
	 * yang spesifik. Method ini menjadi jembatan antara mekanisme CRUD generik dengan
	 * implementasi spesifik {@code Pertangungjawaban}.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method melakukan cast {@code generalValueObject} ke tipe {@code Pertangungjawaban}
	 * dan menyimpannya ke field instance {@code pertangungjawaban}. Kemudian memanggil
	 * overload private {@code init(Pertangungjawaban)} yang melakukan inisialisasi
	 * sesungguhnya: membersihkan window, membangun layout Borderlayout, membuat form
	 * via {@code form()}, dan menambahkan toolbar simpan/batal. Setelah inisialisasi
	 * selesai, window ditampilkan dengan {@code setVisible(true)} dan dibuka dalam
	 * mode modal dengan {@code onModal()} agar pengguna tidak bisa berinteraksi dengan
	 * halaman latar selama form terbuka.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param obj Objek {@code GeneralValueObject} yang merupakan instance
	 *        {@code Pertangungjawaban}. Bila ini adalah LPJ baru, obj adalah instance
	 *        kosong; bila ubah, obj adalah entitas yang sudah ada.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Efek samping adalah tampilnya window modal form LPJ.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil. ClassCastException akan terjadi jika pemanggil
	 * memasukkan objek bukan {@code Pertangungjawaban}, yang seharusnya tidak terjadi
	 * dalam penggunaan normal framework CRUD.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Harus dipanggil dari thread event ZK. Manipulasi komponen UI dilakukan secara
	 * langsung sehingga tidak aman dari thread lain.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Method ini tidak perlu diubah kecuali kontrak interface berubah. Seluruh logika
	 * inisialisasi ada di {@code init(Pertangungjawaban)} dan {@code form()}. Bila ada
	 * kebutuhan untuk menampilkan form dengan konfigurasi berbeda (tanpa modal, atau
	 * embedded), buat overload baru daripada mengubah method ini.</p>
	 *
	 * @param obj entitas Pertangungjawaban (baru atau yang sudah ada) untuk ditampilkan di form
	 * @throws Exception jika inisialisasi form atau komponen ZK gagal
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pertangungjawaban = (Pertangungjawaban) obj;
		init(pertangungjawaban);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <h3>onAddExternal — Membuka Form LPJ dari Modul Lain</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Method statis yang memungkinkan modul lain (misalnya: {@code UangMukaRenderer},
	 * modul SOP, atau dasbor) untuk membuka form detail pertanggungjawaban dalam mode
	 * view-only tanpa harus menavigasi ke halaman pertanggungjawaban. Ini memfasilitasi
	 * alur kerja cross-modul yang seamless.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method membuat instance baru {@code PertangungjawabanAction} secara programatik
	 * dan mengkonfigurasinya tanpa melalui ZK compose dari file ZUL. Langkah-langkah:</p>
	 * <ol>
	 *   <li>Membuat instance baru dan menyiapkan {@code addWindow} sebagai {@code MyWindow}
	 *       baru tanpa file ZUL.</li>
	 *   <li>Menetapkan mode: {@code persetujuan = true} (form tidak menampilkan tombol
	 *       tambah), {@code setujui = true}, {@code viewOnly = true} (semua field
	 *       read-only).</li>
	 *   <li>Melampirkan window ke root page yang sedang aktif.</li>
	 *   <li>Mengatur dimensi window: tinggi 95%, lebar 550px.</li>
	 *   <li>Memanggil {@code init(pertangungjawaban)} untuk membangun form dengan data
	 *       yang diberikan.</li>
	 *   <li>Menampilkan window sebagai modal yang bisa ditutup pengguna.</li>
	 * </ol>
	 * <p>Event listener parameter saat ini tidak digunakan aktif (tidak di-wire ke
	 * aksi apapun) namun disediakan untuk konsistensi pola API dan kemungkinan
	 * pengembangan callback di masa mendatang.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param eventListener EventListener yang dipanggil bila ada event dari form ini.
	 *        Saat ini belum dihubungkan ke aksi spesifik; untuk penggunaan mendatang.
	 * @param pertangungjawaban Data LPJ yang akan ditampilkan dalam mode view-only.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Efek samping adalah tampilnya window modal view LPJ.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil. Error umum: {@code getCurrentPage()} mengembalikan
	 * null jika dipanggil di luar konteks request ZK aktif.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Harus dipanggil dari thread event ZK karena mengakses {@code ExecutionsCtrl}
	 * dan memanipulasi komponen UI.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila event listener perlu dihubungkan (misal: untuk refresh halaman pemanggil
	 * setelah perubahan data), simpan {@code eventListener} ke field instance dan
	 * panggil setelah operasi selesai, mengikuti pola yang ada di {@code UangMukaAction.onAddExternal}.</p>
	 *
	 * @param eventListener listener untuk callback (saat ini belum digunakan aktif)
	 * @param pertangungjawaban data LPJ yang akan ditampilkan
	 * @throws Exception jika gagal membuat atau menampilkan window form
	 */
	public static void onAddExternal(EventListener eventListener, Pertangungjawaban pertangungjawaban)
			throws Exception {
		PertangungjawabanAction pertangungjawabanAction = new PertangungjawabanAction();
		pertangungjawabanAction.addWindow = new MyWindow();
		pertangungjawabanAction.persetujuan = true;
		pertangungjawabanAction.setujui = true;
		pertangungjawabanAction.viewOnly = true;

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(pertangungjawabanAction.addWindow);
		pertangungjawabanAction.addWindow.setHeight("95%");
		pertangungjawabanAction.addWindow.setWidth("550px");

		pertangungjawabanAction.init(pertangungjawaban);

		pertangungjawabanAction.addWindow.setVisible(true);
		pertangungjawabanAction.addWindow.setClosable(true);
		pertangungjawabanAction.addWindow.onModal();

	}

	/**
	 * <h3>onAdd — Membuka Form Tambah Pertanggungjawaban Baru</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Menangani event klik tombol "Tambah" / "Pengajuan Pertanggungjawaban Uang Muka"
	 * di toolbar halaman. Method ini memulai alur pengajuan LPJ baru dengan membuka
	 * form kosong dalam mode tambah (bukan ubah).</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method pertama-tama memastikan flag {@code viewOnly} di-reset ke false agar
	 * form yang akan dibuka dalam mode edit penuh (bukan mode baca saja). Kemudian
	 * memanggil {@code init(new Pertangungjawaban())} dengan objek {@code Pertangungjawaban}
	 * kosong baru, yang menyebabkan {@code init} membangun form dalam konteks "create
	 * new" (ID null). Kode kosong akan di-generate otomatis oleh {@code generateCode}
	 * di dalam {@code form()}, dan field-field kosong siap diisi pengguna. Setelah
	 * inisialisasi, window ditampilkan dengan {@code setVisible(true)} dan dibuka modal
	 * dengan {@code onModal()}. Exception dari {@code onModal} ditangkap dan ditampilkan
	 * hanya kepada admin untuk debugging.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param event Event klik dari ZK yang memicu method ini. Parameter tidak digunakan
	 *        secara langsung namun wajib ada sesuai konvensi event handler ZK.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Efek samping adalah tampilnya window modal form LPJ baru.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dari {@code init} dilempar ke pemanggil. Exception dari {@code onModal}
	 * (yang bisa terjadi jika window tidak dalam kondisi valid untuk modal) ditangkap
	 * dan ditampilkan via {@code Common.tampilErrorJikaAdmin}.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil pada thread event ZK. Semua operasi UI dilakukan secara sinkron.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Reset {@code viewOnly = false} di sini penting karena {@code viewOnly} mungkin
	 * di-set true saat membuka record yang sudah selesai diproses SOP. Tanpa reset ini,
	 * form tambah baru pun akan read-only jika sebelumnya membuka record view-only.</p>
	 *
	 * @param event event klik ZK dari tombol tambah
	 * @throws Exception jika proses inisialisasi form gagal
	 */
	public void onAdd(Event event) throws Exception {
		viewOnly = false;
		init(new Pertangungjawaban());
		addWindow.setVisible(true);
		try {
			addWindow.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * <h3>form — Membangun Grid Form Pertanggungjawaban</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Membangun dan mengembalikan komponen {@code MyGrid} yang berisi seluruh form
	 * pengisian atau tampilan data pertanggungjawaban uang muka. Method ini adalah
	 * inti dari UI layer kelas ini — semua field, binding event, validasi visual,
	 * dan logika tampilan bersumber di sini.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method membangun form secara programatik dengan pola MyFormRow berisi 2 kolom:
	 * label di kiri dan komponen input/read-only di kanan. Komponen yang ditampilkan
	 * bergantung pada kombinasi flag {@code persetujuan}, {@code setujui}, dan
	 * {@code viewOnly}:</p>
	 * <ul>
	 *   <li>Bila salah satu flag true, field diganti Label read-only.</li>
	 *   <li>Bila semua flag false, field adalah komponen input yang bisa diedit.</li>
	 * </ul>
	 * <p>Field-field yang dibangun antara lain:</p>
	 * <ul>
	 *   <li>Satuan Kerja (AmbilDataSatuanKerjaBanbox) — dengan event listener yang
	 *       memperbarui banbox Uang Muka berdasarkan satuan kerja terpilih.</li>
	 *   <li>Uang Muka (AmbilDataUangMukaBanbox) — dengan event listener yang mengisi
	 *       otomatis label periode, akun anggaran, nilai uang muka, saldo, dll.</li>
	 *   <li>Kode LPJ (auto-generate via generateCode).</li>
	 *   <li>Judul Pengajuan, Nama Sponsor, Nilai Sponsor (kondisional via konfigurasi
	 *       {@code sponsor_tampil_lpj}).</li>
	 *   <li>Rincian Formula Biaya (tabel biaya dengan Tambah/Hapus per baris) via
	 *       {@code reloadFormula}.</li>
	 *   <li>Nilai Pengembalian (dihitung otomatis: nilai uangMuka + dariSponsor - total
	 *       biaya), Tanggal Stor (tampil bila pengembalian > 0.01).</li>
	 *   <li>Radiogroup Status (hanya tampil di mode persetujuan).</li>
	 *   <li>Tanggal Persetujuan Manual, Keterangan.</li>
	 * </ul>
	 * <p>Event listener tombol simpan mengontrol label tombol berdasarkan status yang
	 * dipilih: "Uang Muka Selesai", "Pertanggungjawaban Selesaikan", atau
	 * "Ajukan dan Cetak".</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param generalValueObject Entitas {@code Pertangungjawaban} yang datanya akan
	 *        ditampilkan/diedit di form.
	 * @param disposisiSop DisposisiSop dari alur SOP yang sedang aktif, atau null
	 *        bila bukan dari alur SOP.
	 * @param save Tombol simpan yang akan dikonfigurasi labelnya oleh event listener
	 *        status dalam form ini.
	 * @param setujuiData EventListener tambahan untuk event klik radiogroup status,
	 *        digunakan oleh halaman SOP untuk sinkronisasi state persetujuan.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return {@code MyGrid} yang berisi seluruh komponen form dan siap untuk
	 *         dimasukkan ke dalam Center layout window.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil. Error umum: JSONException bila formula bukan
	 * JSON valid, HibernateException bila session tidak aktif.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Harus dipanggil dari thread event ZK. Membuat banyak komponen UI secara langsung.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Method ini sangat panjang. Bila ada field baru, tambahkan di bagian yang logis
	 * (setelah Uang Muka, sebelum Formula, atau setelah Status). Jaga urutan karena
	 * field-field saling bergantung melalui event listener. Setiap penambahan field
	 * harus mempertimbangkan 3 mode: edit, persetujuan, dan view-only.</p>
	 *
	 * @param generalValueObject entitas Pertangungjawaban untuk ditampilkan
	 * @param disposisiSop disposisi SOP aktif atau null
	 * @param save tombol simpan yang labelnya dikontrol oleh form ini
	 * @param setujuiData listener tambahan untuk event status
	 * @return MyGrid form yang telah dibangun lengkap
	 * @throws Exception jika terjadi error saat membangun komponen form
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}

		dikembalikan = 0.0;
		nilaipajak = 0.0;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
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

		pertangungjawaban = (Pertangungjawaban) generalValueObject;

		setujui = false;
		if (!persetujuan) {
			if (pertangungjawaban != null && pertangungjawaban.getStatus().equals(Pertangungjawaban.DISETUJU)) {
				setujui = true;
			} else {
				setujui = false;
			}
		}

		if (pertangungjawaban.getDisposisiSop() != null
				&& pertangungjawaban.getDisposisiSop().getDisposisiSetuju() != null
				&& pertangungjawaban.getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null
				&& pertangungjawaban.getDisposisiSop().getDisposisiSetuju().getSelesai()) {
			viewOnly = true;
		}

		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();

		SatuanKerja parent = pt == null ? null : pt.getSatuanKerja();

		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		try {
			if (pertangungjawaban.getSatuanKerja() == null) {
				pertangungjawaban.setSatuanKerja(Common.getSatuanKerja());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PertangungjawabanAction.java:1478");
			// TODO: handle exception
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja *"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(
				pertangungjawaban.getSatuanKerja() == null ? "" : pertangungjawaban.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", pertangungjawaban.getSatuanKerja());

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(
					pertangungjawaban.getSatuanKerja() == null ? "" : pertangungjawaban.getSatuanKerja().getNama()));
		} else {
			row.appendChild(satuanKerja);
		}

		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Uang Muka *"));

		uangMuka = new AmbilDataUangMukaBanbox();
		uangMuka.setAttribute("uangMuka", pertangungjawaban.getUangMuka());
		uangMuka.setValue(pertangungjawaban.getUangMuka() == null ? "" : pertangungjawaban.getUangMuka().getKode());

		uangMuka.setReadonly(true);
		uangMuka.setWidth("90%");

		satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				uangMuka.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

				if (pertangungjawaban.getId() == null) {
					String noAgenda = generateCode(false, (SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
					pertangungjawaban.setKode(noAgenda);
					kode.setValue(noAgenda);
				}
			}
		});

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(pertangungjawaban.getUangMuka() == null ? ""
					: pertangungjawaban.getUangMuka().getKode() + "-" + pertangungjawaban.getUangMuka().getNama()));
		} else {
			row.appendChild(uangMuka);
		}

		if (satuanKerja.getAttribute("satuanKerja") != null) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					uangMuka.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
				}
			});
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		if (pertangungjawaban.getId() == null) {
			String noAgenda = generateCode(false, (SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
			pertangungjawaban.setKode(noAgenda);
		}

		kode = new Label(pertangungjawaban.getKode());
		if (persetujuan) {
			row.appendChild(new Label(pertangungjawaban.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		nama = new Textbox(pertangungjawaban.getNama());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Pengajuan *"));
		nama.setWidth("90%");

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(pertangungjawaban.getNama()));
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
		final Label sisaAnggaran = new Label();
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

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kegiatan *"));
		final Label mulai = new Label();
		row.appendChild(mulai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Laporan *"));
		final Label selesai = new Label();
		row.appendChild(selesai);

		nilaiHarusDikembalikan = new Label();
		tanggalStor = new MyDatebox(pertangungjawaban.getTanggalStor());
		tanggalStor.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Pengajuan Uang Muka *"));
		final Label nilaiPengajuan = new Label();
		row.appendChild(nilaiPengajuan);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				UangMuka work = (UangMuka) (uangMuka.getAttribute("uangMuka"));

				if ((work == null || uangMuka.getParent() == null) && pertangungjawaban.getUangMuka() != null) {
					work = pertangungjawaban.getUangMuka();
				}

				if (work != null && kode.getValue().trim().isEmpty()) {
					kode.setValue(work.getKode());
				}
				if (work != null && nama.getValue().trim().isEmpty()) {
					nama.setValue(work.getNama());
				}

				mulai.setValue(
						work == null || work.getMulai() == null ? "" : Common.dateFormat4.get().format(work.getMulai()));
				selesai.setValue(
						work == null || work.getSelesai() == null ? "" : Common.dateFormat4.get().format(work.getSelesai()));

				unit.setValue(work == null || work.getSatuanKerja() == null ? "" : work.getSatuanKerja().getNama());

				akun.setValue(work == null || work.getAkun() == null ? ""
						: work.getAkun().getKode() + "-" + work.getAkun().getNama());

				nilaiPengajuan.setValue(work == null ? "" : Common.numberFormat.get().format(work.getNilai()));

				tgl.setValue((work == null || work.getMulai() == null ? "" : Common.dateFormat1.get().format(work.getMulai()))
						+ (work == null || work.getSelesai() == null ? ""
								: " s.d " + Common.dateFormat1.get().format(work.getSelesai())));

				Double saldoSekarang = work == null ? 0.0 : work.getSaldo();
				Double anggaran = (work == null || work.getWorkspace() == null ? 0.0
						: work.getWorkspace().getHargaTotal());
				Double dalamProses = anggaran - saldoSekarang;
				saldo.setValue(Common.numberFormat.get().format(anggaran));
				uangMukaDalamProses.setValue(Common.numberFormat.get().format(dalamProses));

				sisaAnggaran.setValue(Common.numberFormat.get().format(saldoSekarang));

				if (work != null && work.getWorkspace() != null) {

					JenisUangMukaAction.tampilkan(uangMukaDalamProsesDetail, null, null, null, work.getWorkspace(),
							work.getTanggalPembuatan());

				}
			}
		};

		uangMuka.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan Oleh"));
		row.appendChild(new Label(
				pertangungjawaban.getDibuatOleh() == null ? "" : pertangungjawaban.getDibuatOleh().getUserNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan Tanggal"));
		row.appendChild(new Label(pertangungjawaban.getTanggalPembuatan() == null ? ""
				: Common.dateFormat1.get().format(pertangungjawaban.getTanggalPembuatan())));

		boolean sponsor_tampil_lpj = Common.bolehKonfigurasi("sponsor_tampil_lpj");

		row = new MyFormRow();
		row.setVisible(sponsor_tampil_lpj);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Sponsor"));
		namaSponsor = new Textbox(pertangungjawaban.getNamaSponsor());

		namaSponsor.setWidth("90%");

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(pertangungjawaban.getNamaSponsor()));
		} else {
			row.appendChild(namaSponsor);
		}

		row = new MyFormRow();
		row.setVisible(sponsor_tampil_lpj);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Sponsor"));
		dariSponsor = new MyDoublebox(pertangungjawaban.getDariSponsor());

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(Common.numberFormat.get().format(pertangungjawaban.getDariSponsor())));
		} else {
			row.appendChild(dariSponsor);
		}

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Rincian Laporan Pertanggungjawaban"));

		nilai = 0.0;
		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		array = new JSONArray(pertangungjawaban.getFormula());
		rowFormula = Common.tampilanScroll1(row);
		reloadFormula(rowFormula, array);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Pengembalian *"));
		row.appendChild(nilaiHarusDikembalikan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Stor *"));
		if (persetujuan || setujui || viewOnly) {
			row.appendChild(
					new Label(tanggalStor.getValue() == null ? "" : Common.dateFormat3.get().format(tanggalStor.getValue())));
		} else {
			row.appendChild(tanggalStor);
		}
		row.setVisible(dikembalikan > 0.01);

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
		Common.selectRadioItem(status, pertangungjawaban.getStatus());
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
					setujuiData.onEvent(new Event("", null, pertangungjawaban.getStatus().equals(UangMuka.DISETUJU)));
				}
			});
		}

		if (setujui || viewOnly) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
			row.appendChild(new ais.ui.util.MyLabelConfig(pertangungjawaban.getStatus()));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Persetujuan"));
		tanggalPersetujuanManual = new MyDatebox(pertangungjawaban.getTanggalPersetujuanManual());
		if (pertangungjawaban.getPostingHistory() == null) {
			row.appendChild(tanggalPersetujuanManual);
		} else {
			row.appendChild(new Label(Common.dateFormat1.get()
					.format(pertangungjawaban.getTanggalPersetujuanManual() == null ? WaktuUtil.getDate()
							: pertangungjawaban.getTanggalPersetujuanManual())));
		}
		tanggalPersetujuanManual.setReadonly(true);
		tanggalPersetujuanManual.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (pertangungjawaban != null && pertangungjawaban.getId() != null) {
					pertangungjawaban.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
					Common.refreshUpdate(pertangungjawaban);
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		keterangan = new Textbox(pertangungjawaban.getKeterangan() == null ? "" : pertangungjawaban.getKeterangan());

		if (setujui) {
			row.appendChild(new Label(pertangungjawaban.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(3);

		EventListener s = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean setujui = status.getSelectedItem() == null ? false
						: status.getSelectedItem().getValue().equals(DanaTalangan.DISETUJU);

				if (tanggalPersetujuanManual != null && tanggalPersetujuanManual.getParent() != null) {
					if (tanggalPersetujuanManual.getValue() == null) {
						tanggalPersetujuanManual.setValue(WaktuUtil.getDate());
					}
					tanggalPersetujuanManual.getParent().setVisible(setujui);
				}

				if (setujui) {
					if (dikembalikan > 0.1) {
						save.setLabel("Uang Muka Selesai");
					} else {
						save.setLabel("Pertanggungjawaban Selesaikan");
					}
				} else {
					save.setLabel(!persetujuan ? "Ajukan dan Cetak" : "Ubah Status Persetujuan dan Cetak");
				}
			}
		};

		status.addEventListener("onClick", s);
		Common.createDefaultTimer(s);

		Common.createDefaultTimer(eventListener);

		return grid;
	}

	/**
	 * <h3>reloadDataFormula — Membangun Ulang Tabel Rincian Biaya LPJ</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Membangun atau membangun ulang grid tabel rincian biaya pertanggungjawaban
	 * berdasarkan data JSONArray yang diberikan. Method ini dipanggil setiap kali
	 * ada perubahan pada baris biaya: saat menambah baris baru, menghapus baris,
	 * atau mengubah nilai field dalam baris yang sudah ada.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method pertama menghapus semua komponen anak dari {@code rowU} dengan
	 * {@code Common.clear(rowU)}, lalu membangun grid baru. Grid memiliki 11 kolom:
	 * Keterangan Biaya, PPh Pasal, Qty, Satuan, Harga, DPP, PPN, Pot.PPH, Jumlah,
	 * Hps (hapus), dan aksi. Di footer grid terdapat total keseluruhan yang dihitung
	 * oleh event listener hitungTotal. Setiap baris data dari JSONArray diproses dan
	 * dirender sebagai baris grid. Untuk setiap baris:</p>
	 * <ul>
	 *   <li>Dalam mode edit: field ditampilkan sebagai input (Textbox, Combobox,
	 *       MyDoublebox, MyDatebox) dengan event listener onChange yang mengupdate
	 *       JSONObject dan memanggil hitungTotal.</li>
	 *   <li>Dalam mode view/setujui/persetujuan: field ditampilkan sebagai Label.</li>
	 *   <li>Lampiran bukti (LampiranLain) ditampilkan sebagai link klik-untuk-buka.
	 *       Bila belum ada lampiran dan dalam mode edit, ditampilkan widget upload.</li>
	 * </ul>
	 * <p>EventListener hitungTotal yang dikombinasikan dengan onChange setiap field
	 * memastikan total biaya, nilai pengembalian, dan visibilitas row Tanggal Stor
	 * selalu konsisten saat pengguna mengetik.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param rowU Row ZK yang menjadi container untuk grid biaya. Semua anak lama
	 *        akan dibersihkan dan diganti dengan grid baru.
	 * @param array JSONArray berisi data baris-baris biaya LPJ. Setiap elemen adalah
	 *        JSONObject dengan key: key, nama, satuan, satuanId, ntpn, npwp, namaWp,
	 *        tanggalStor, pajak (PPh), ppn, pajak_ppn, qty, harga, jumlah, id_file,
	 *        link, nama_file.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Grid baru langsung dipasang sebagai anak {@code rowU}.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil. JSONException bila JSON malformed. Item array
	 * dengan key null (item yang dihapus secara logis) di-skip via {@code continue}.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Harus dipanggil dari thread event ZK. Membuat banyak komponen UI secara langsung.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila ada field baru di baris biaya (misal: kode akun per baris), tambahkan kolom
	 * baru di header dan render field baru di setiap baris. Ingat untuk juga memperbarui
	 * hitungTotal, onSave (validasi + kalkulasi), dan renderer grid induk. Perhatikan
	 * bahwa {@code pph_mengurangi_lpj} dibaca dari konfigurasi setiap kali method ini
	 * dipanggil untuk memastikan nilai terkini.</p>
	 *
	 * @param rowU container Row ZK tempat grid biaya akan dipasang
	 * @param array data JSONArray rincian biaya yang akan dirender
	 * @throws Exception jika terjadi error saat membangun komponen atau parsing JSON
	 */
	public void reloadDataFormula(final Row rowU, final JSONArray array) throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

		// Lebar kolom dirapikan agar total ≤100% (sebelumnya menjumlah 113% sehingga grid meluap
		// dan kolom Hapus terpotong). Kolom Hapus dilebarkan agar tombolnya muat.
		MyColumnConfig column = new MyColumnConfig("Keterangan Biaya");
		column.setParent(columns);
		column.setWidth("18%");

		column = new MyColumnConfig("Pph Pasal");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("Qty");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("5%");

		column = new MyColumnConfig("Satuan");
		column.setParent(columns);
		column.setWidth("5%");

		column = new MyColumnConfig("Harga");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("DPP");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("PPN");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Pot. PPH");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("8%");

		column = new MyColumnConfig("Jumlah");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("9%");

		column = new MyColumnConfig("Hapus");
		column.setAlign("center");
		column.setParent(columns);
		column.setWidth("8%");

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

				boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");
				nilaipajak = 0.0;
				dikembalikan = 0.0;
				Double nilai = 0.0;
				for (int i = 0; i < array.length(); i++) {
					Double jumlah = 0.0;
					JSONObject jsonObject = array.getJSONObject(i);
					if (!jsonObject.isNull("jumlah")) {
						jumlah = jsonObject.getDouble("jumlah");
					}

					Double ppn = 0.0;
					if (!jsonObject.isNull("ppn")) {
						ppn = jsonObject.getDouble("ppn");
					}

					JenisPajakBarang barang;
					if (!jsonObject.isNull("pajak")) {
						barang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
								Long.parseLong(jsonObject.get("pajak") + ""));
					} else {
						barang = null;
					}
					Double nilaippn = ((ppn / 100.0) * jumlah);
					Double pajak = barang == null ? 0.0 : ((barang.getPersen() / 100.0) * jumlah);
					nilaipajak += pajak;
					Double tot = (jumlah + nilaippn) - (pph_mengurangi_lpj ? pajak : 0.0);

					nilai += tot;
				}
				footerTotal.setLabel(Common.numberFormat.get().format(nilai));

				UangMuka work = (UangMuka) (uangMuka.getAttribute("uangMuka"));

				dikembalikan = Pertangungjawaban.hitungDikembalikan(work == null ? 0.0 : work.getNilai(),
						dariSponsor.getValue(), nilai);

				nilaiHarusDikembalikan.setValue(Common.numberFormat.get().format(dikembalikan));

				try {
					// Dalam mode persetujuan/setujui/viewOnly, tanggalStor tidak pernah
					// di-appendChild ke row (diganti Label) sehingga getParent() null.
					if (tanggalStor.getParent() != null) {
						tanggalStor.getParent().setVisible(dikembalikan > 0.01);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PertangungjawabanAction.java:2093");
					// TODO: handle exception
				}
			}

		};

		hitungTotal.onEvent(null);

		dariSponsor.addEventListener("onChange", hitungTotal);
		boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);

			Long key;
			if (jsonObject.isNull("key")) {
				continue;
			} else {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
			}

			String nama = "";

			if (!jsonObject.isNull("nama")) {
				nama = jsonObject.get("nama") + "";
			}

			String satuan = "";

			if (!jsonObject.isNull("satuan")) {
				satuan = jsonObject.get("satuan") + "";
			}

			Long satuanId = null;

			if (!jsonObject.isNull("satuanId")) {
				satuanId = Long.parseLong(jsonObject.get("satuanId") + "");
			}

			String ntpn = "";

			if (!jsonObject.isNull("ntpn")) {
				ntpn = jsonObject.get("ntpn") + "";
			}

			String npwp = "";

			if (!jsonObject.isNull("npwp")) {
				npwp = jsonObject.get("npwp") + "";
			}

			String namaWp = "";

			if (!jsonObject.isNull("namaWp")) {
				namaWp = jsonObject.get("namaWp") + "";
			}

			String tanggalStor = "";

			if (!jsonObject.isNull("tanggalStor")) {
				tanggalStor = jsonObject.get("tanggalStor") + "";
			}
			Date tglStor = null;
			try {
				tglStor = tanggalStor.isEmpty() ? null : Common.dateFormat1.get().parse(tanggalStor);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PertangungjawabanAction.java:2160");
				// TODO: handle exception
			}

			final JenisPajakBarang jenisPajakBarang;
			if (!jsonObject.isNull("pajak")) {
				jenisPajakBarang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
						Long.parseLong(jsonObject.get("pajak") + ""));
			} else {
				jenisPajakBarang = null;
			}

			Double ppn = 0.0;
			if (!jsonObject.isNull("ppn")) {
				ppn = jsonObject.getDouble("ppn");
			}

			final JenisPajakPpn jenisPajakPpn;
			if (!jsonObject.isNull("pajak_ppn")) {
				jenisPajakPpn = (JenisPajakPpn) ConstantValues.ambil(JenisPajakPpn.class.getName(),
						Long.parseLong(jsonObject.get("pajak_ppn") + ""));
			} else {
				jenisPajakPpn = ppn.intValue() == 11 ? JenisPajakPpn.PPN : null;
			}

			Double qty = 0.0;
			if (!jsonObject.isNull("qty")) {
				qty = jsonObject.getDouble("qty");
			}

			Double harga = 0.0;
			if (!jsonObject.isNull("harga")) {
				harga = jsonObject.getDouble("harga");
			}

			Double jumlah = qty * harga;

			Double pajak_nilai = jenisPajakBarang == null ? 0.0 : ((jenisPajakBarang.getPersen() / 100.0) * jumlah);

			String nama_file = "";

			if (!jsonObject.isNull("nama_file")) {
				nama_file = jsonObject.get("nama_file") + "";
			}

			String link = "";

			if (!jsonObject.isNull("link")) {
				link = jsonObject.get("link") + "";
			}
			Double nilaippn = ((ppn / 100.0) * jumlah);
			Double tot = (jumlah + nilaippn) - (pph_mengurangi_lpj ? pajak_nilai : 0.0);

			MyFormRow rowData = new MyFormRow();
			rowData.setValign("top");
			rowData.setParent(rows);
			final Combobox comboboxPajak = new Combobox();
			final Label nilaiDpp = new Label(Common.numberFormat.get().format(jumlah));

			final Label total = new Label(Common.numberFormat.get().format(tot));

			final Combobox persenPpn = new Combobox();
			Common.insertComboDanSemua(persenPpn, new String[] { "nama" }, "keterangan", JenisPajakPpn.class,
					"Tanpa PPN", Restrictions.eq("aktif", true));
			Common.selectComboItem(persenPpn, jenisPajakPpn);

			final MyTextbox targetText = new MyTextbox(nama);

			Vbox myvbox = new Vbox();
			myvbox.setParent(rowData);
			myvbox.setWidth("95%");

			final MyDoublebox qtyBox = new MyDoublebox(qty);

			final Combobox satuanBox = new Combobox();
			satuanBox.setReadonly(true);
			final MyDoublebox hargaBox = new MyDoublebox(harga);

			targetText.setWidth("95%");
			qtyBox.setWidth("95%");
			hargaBox.setWidth("95%");
			persenPpn.setWidth("85%");

			satuanBox.setWidth("95%");

			Common.insertComboDanSemua(comboboxPajak, new String[] { "nama", "persen" }, "keterangan",
					JenisPajakBarang.class, "Tanpa Pajak", Restrictions.eq("aktif", true));
			Common.selectComboItem(comboboxPajak, jenisPajakBarang);
			comboboxPajak.setWidth("95%");

			final Label pajak_nilaiBox = new Label(Common.numberFormat.get().format(pajak_nilai));
			pajak_nilaiBox.setWidth("95%");

			final Label ppn_nilaiBox = new Label(Common.numberFormat.get().format(nilaippn));
			ppn_nilaiBox.setWidth("95%");

			final MyTextbox ntpnText = new MyTextbox(ntpn);
			ntpnText.setWidth("85%");
			final MyTextbox npwpText = new MyTextbox(npwp);
			npwpText.setWidth("85%");
			final MyTextbox namaWpText = new MyTextbox(namaWp);
			namaWpText.setWidth("85%");

			final MyDatebox tanggalStorText = new MyDatebox(tglStor);
			tanggalStorText.setWidth("85%");
			tanggalStorText.setReadonly(true);

			if (persetujuan || setujui || viewOnly) {
				myvbox.appendChild(new Label(nama));
				if (jenisPajakBarang != null) {
					Vbox aa;
					rowData.appendChild(aa = new Vbox(new Component[] {
							new MyLabelAgakKecil(jenisPajakBarang == null ? "" : jenisPajakBarang.getNama()),
							new MyLabelAgakKecil(ntpn), new MyLabelAgakKecil(npwp), new MyLabelAgakKecil(namaWp),
							new MyLabelAgakKecil(tanggalStor) }));
					aa.setWidth("100%");
				} else {
					rowData.appendChild(
							new MyLabelAgakKecil(jenisPajakBarang == null ? "" : jenisPajakBarang.getNama()));
				}
				rowData.appendChild(new Label(Common.numberFormat.get().format(qty)));

				rowData.appendChild(new Label(satuan));

				rowData.appendChild(new Label(Common.numberFormat.get().format(harga)));
				rowData.appendChild(nilaiDpp);
				rowData.appendChild(ppn_nilaiBox);
				rowData.appendChild(new Label(Common.numberFormat.get().format(pajak_nilai)));
			} else {
				myvbox.appendChild(targetText);
				Vbox aa;
				rowData.appendChild(aa = new Vbox(new Component[] { comboboxPajak,
						new Hbox(new Component[] { new MyLabelAgakKecil("NTPN"), ntpnText }),
						new Hbox(new Component[] { new MyLabelAgakKecil("NPWP"), npwpText }),
						new Hbox(new Component[] { new MyLabelAgakKecil("Nama WP"), namaWpText }),
						new Hbox(new Component[] { new MyLabelAgakKecil("Tgl Stor"), tanggalStorText }) }));
				aa.setWidth("100%");
				rowData.appendChild(qtyBox);

				Common.insertCombo(satuanBox, "nama", Satuan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
				Common.selectComboItem(satuanBox, new Satuan(satuanId));

				rowData.appendChild(satuanBox);

				rowData.appendChild(hargaBox);
				rowData.appendChild(nilaiDpp);
				Vbox a;
				rowData.appendChild(a = new Vbox(new Component[] { persenPpn, ppn_nilaiBox }));
				a.setWidth("100%");
				rowData.appendChild(pajak_nilaiBox);
			}

			Long id_file = null;

			if (!jsonObject.isNull("id_file")) {
				id_file = Long.parseLong(jsonObject.get("id_file") + "");
			}

			final LampiranLain lampiranLain = id_file != null ? LampiranLain.ambil(true, id_file, "id")
					: LampiranLain.ambil(key, "Dokumen Pertangungjawaban");

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
						Clients.evalJavaScript("popupCenter({url: '" + url + "', title: 'Data', w: 1200, h: 600});");
					}
				});

			} else {

				Hbox hbox = new Hbox();
				hbox.setParent(myvbox);
				LampiranLain.createDownloadUploadFileLain(hbox, key, "Dokumen Pertangungjawaban", "Bukti", false,
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
					boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");
					JenisPajakBarang barang = (JenisPajakBarang) (comboboxPajak.getSelectedItem() == null ? null
							: comboboxPajak.getSelectedItem().getValue());

					Satuan satuanD = (Satuan) (satuanBox.getSelectedItem() == null ? null
							: satuanBox.getSelectedItem().getValue());

					ntpnText.getParent().setVisible(barang != null);
					npwpText.getParent().setVisible(barang != null);
					namaWpText.getParent().setVisible(barang != null);
					tanggalStorText.getParent().setVisible(barang != null);

					jsonObject.put("satuan", satuanD == null ? "" : satuanD.getNama());
					jsonObject.put("satuanId", satuanD == null ? "-1" : satuanD.getId().toString());

					jsonObject.put("ntpn", ntpnText.getValue());
					jsonObject.put("npwp", npwpText.getValue());
					jsonObject.put("namaWp", namaWpText.getValue());
					jsonObject.put("tanggalStor", tanggalStorText.getValue() == null ? ""
							: Common.dateFormat1.get().format(tanggalStorText.getValue()));

					JenisPajakPpn pajakPpn = (JenisPajakPpn) (persenPpn.getSelectedItem() == null ? null
							: persenPpn.getSelectedItem().getValue());

					jsonObject.put("pajak_ppn", pajakPpn != null ? pajakPpn.getId() : null);

					jsonObject.put("pajak", barang != null ? barang.getId() : null);

					jsonObject.put("nama", targetText.getValue());
					jsonObject.put("qty", qtyBox.getValue());
					jsonObject.put("harga", hargaBox.getValue());

					Double ppn = pajakPpn == null ? 0.0 : pajakPpn.getPersen();

					jsonObject.put("ppn", ppn);

					Double jumlah = (qtyBox.getValue() == null ? 0.0 : qtyBox.getValue())
							* (hargaBox.getValue() == null ? 0.0 : hargaBox.getValue());
					Double nilaippn = ((ppn / 100.0) * jumlah);
					jsonObject.put("nilaippn", nilaippn);
					jsonObject.put("jumlah", jumlah);
					nilaiDpp.setValue(Common.numberFormat.get().format(jumlah));

					Double pajak = barang == null ? 0.0 : ((barang.getPersen() / 100.0) * jumlah);
					pajak_nilaiBox.setValue(Common.numberFormat.get().format(pajak));
					jsonObject.put("pajak_nilai", pajak);

					ppn_nilaiBox.setValue(Common.numberFormat.get().format(nilaippn));

					Double tot = (jumlah + nilaippn) - (pph_mengurangi_lpj ? pajak : 0.0);
					total.setValue(Common.numberFormat.get().format(tot));
					jsonObject.put("total", tot);
					hitungTotal.onEvent(null);
				}
			};

			targetText.setRows(2);

			persenPpn.addEventListener("onChange", eventListener);
			qtyBox.addEventListener("onChange", eventListener);
			targetText.addEventListener("onChange", eventListener);
			hargaBox.addEventListener("onChange", eventListener);
			comboboxPajak.addEventListener("onChange", eventListener);

			ntpnText.addEventListener("onChange", eventListener);
			npwpText.addEventListener("onChange", eventListener);
			namaWpText.addEventListener("onChange", eventListener);
			tanggalStorText.addEventListener("onChange", eventListener);

			satuanBox.addEventListener("onChange", eventListener);

			if (ntpnText.getParent() != null)
				ntpnText.getParent().setVisible(jenisPajakBarang != null);
			if (npwpText.getParent() != null)
				npwpText.getParent().setVisible(jenisPajakBarang != null);
			if (namaWpText.getParent() != null)
				namaWpText.getParent().setVisible(jenisPajakBarang != null);
			if (tanggalStorText.getParent() != null)
				tanggalStorText.getParent().setVisible(jenisPajakBarang != null);

			rowData.appendChild(total);

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

											reloadDataFormula(rowU, array);

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
				new Label().setParent(rowData);
			} else {
				button.setParent(rowData);
			}

		}
	}

	/**
	 * <h3>reloadFormula — Inisialisasi Container Rincian Formula Biaya</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Membangun container utama untuk seksi rincian biaya LPJ: tombol "Tambah Biaya"
	 * dan Row kosong sebagai placeholder, kemudian memuat data awal dari JSONArray
	 * melalui {@code reloadDataFormula}. Method ini dipanggil sekali saat form
	 * pertama dibangun via {@code form()}, berbeda dengan {@code reloadDataFormula}
	 * yang dipanggil setiap kali data berubah.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method membuat {@code MyFormRow} baru (rowU) sebagai container data, kemudian
	 * membuat tombol "Tambah Biaya" dengan event listener onClick yang:
	 * membuat JSONObject kosong baru dengan field standar (nama, qty, harga, jumlah
	 * masing-masing 0, dan key random unik), menambahkannya ke array, lalu memanggil
	 * {@code reloadDataFormula} untuk merender ulang grid. Tombol "Tambah Biaya"
	 * hanya terlihat bila bukan mode persetujuan dan bukan mode setujui.
	 * Tombol dipasang ke {@code rowFormula} (parameter), dan {@code rowU} dipasang
	 * ke parent dari rowFormula (row setelah tombol). Terakhir, {@code reloadDataFormula}
	 * dipanggil untuk merender data awal dari array ke dalam rowU.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param rowFormula Row dari Grid form yang menjadi tempat tombol "Tambah Biaya"
	 *        dipasang. Container rowU (untuk data grid) dipasang setelah rowFormula
	 *        di parent yang sama.
	 * @param array JSONArray berisi data baris biaya yang sudah ada (bila ubah) atau
	 *        kosong (bila tambah baru).</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Komponen UI dibangun dan dipasang ke dalam hierarki
	 * komponen ZK secara langsung.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil. JSONException dapat terjadi bila array tidak
	 * valid saat {@code reloadDataFormula} dipanggil.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Harus dipanggil dari thread event ZK. Key random dibuat dengan
	 * {@code Common.randLong()} yang menggunakan {@code Math.random()}, aman dari
	 * sisi threading karena hanya diakses dari satu thread event sekaligus.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Key random pada JSONObject baru penting untuk identifikasi baris saat hapus
	 * (meng-set JSONObject menjadi kosong di posisi index yang sesuai). Jangan menghapus
	 * field "key" dari JSONObject baru. Bila field default baris baru perlu ditambah
	 * (misal: nilai PPN default 11%), tambahkan di blok pembuatan JSONObject dalam
	 * event listener tombol Tambah Biaya.</p>
	 *
	 * @param rowFormula row induk tempat tombol "Tambah Biaya" dipasang
	 * @param array JSONArray data biaya yang akan ditampilkan
	 * @throws Exception jika terjadi error saat membangun komponen atau merender data
	 */
	public void reloadFormula(final Row rowFormula, final JSONArray array) throws Exception {
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

				reloadDataFormula(rowU, array);
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, array);

	}

	/**
	 * <h3>init(Pertangungjawaban) — Membangun Layout Window Form LPJ</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Method private yang membangun struktur layout lengkap window form pertanggungjawaban:
	 * Borderlayout dengan Center berisi form dan South berisi toolbar simpan/batal.
	 * Method ini dipanggil baik dari {@code init(GeneralValueObject)} (framework CRUD)
	 * maupun dari {@code onAdd} (tombol tambah baru).</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Langkah-langkah yang dilakukan:</p>
	 * <ol>
	 *   <li>Bila {@code SatuanKerjaTreeModel} belum ada, buat instance baru.</li>
	 *   <li>Bila pengguna pembuat belum di-set, set ke pengguna saat ini dan
	 *       tanggal pembuatan ke sekarang.</li>
	 *   <li>Set judul window sesuai mode: "Pengajuan..." atau "Persetujuan...".</li>
	 *   <li>Simpan referensi ke field {@code this.pertangungjawaban}.</li>
	 *   <li>Bersihkan window dengan {@code Common.clear(addWindow)}.</li>
	 *   <li>Buat {@code MyBorderlayout} dan pasang ke addWindow.</li>
	 *   <li>Buat Center dengan flex=true, tambahkan form dari {@code form()} ke dalam
	 *       Center. DisposisiSop di-reset ke null sebelum form dibangun.</li>
	 *   <li>Buat South dengan toolbar berisi tombol Batal (setVisible=false) dan
	 *       tombol Simpan. Tombol simpan memanggil {@code onSave} dan bila sukses
	 *       memanggil {@code onSearchDefault} dan menutup window.</li>
	 *   <li>Bila mode "sudah disetujui dan bukan persetujuan" ({@code setujui && !persetujuan}),
	 *       tombol simpan disembunyikan dan label Batal diganti "Tutup".</li>
	 * </ol>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param pertangungjawaban Entitas LPJ yang akan ditampilkan/diedit. Boleh berisi
	 *        data dari database (ubah) atau objek baru (tambah).</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Window dibangun dan siap untuk dimodal-kan oleh pemanggil.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil. Error umum: Hibernate session tidak aktif saat
	 * {@code form()} mencoba load data relasi.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Harus dipanggil dari thread event ZK. Method membuat banyak komponen UI secara
	 * sinkron.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila ada tombol baru yang perlu ditambahkan ke toolbar (misal: tombol preview),
	 * tambahkan setelah tombol simpan di toolbar. Pastikan tombol baru juga memiliki
	 * visibilitas yang sesuai untuk semua mode (persetujuan, setujui, viewOnly).</p>
	 *
	 * @param pertangungjawaban data LPJ yang akan diinisialisasi ke form window
	 * @throws Exception jika terjadi error saat membangun layout atau form
	 */
	private void init(final Pertangungjawaban pertangungjawaban) throws Exception {

		if (pertangungjawaban.getDibuatOleh() == null) {
			pertangungjawaban.setDibuatOleh(tbmuser);
			pertangungjawaban.setTanggalPembuatan(new Date());
		}

		addWindow.setTitle((!persetujuan ? "Pengajuan" : "Persetujuan") + " Pertanggungjawaban");
		this.pertangungjawaban = pertangungjawaban;
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig(
				!persetujuan ? "Ajukan dan Cetak" : "Ubah Status Persetujuan dan Cetak", "/img/save.gif");

		disposisiSop = null;
		center.appendChild(form(pertangungjawaban, disposisiSop, save, null));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
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
					onSearchDefault(null);
					addWindow.setVisible(false);

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
	 * <h3>onSave — Validasi dan Simpan Data Pertanggungjawaban</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Melakukan validasi input form, menghitung total biaya dari formula, menyimpan
	 * atau memperbarui entitas {@code Pertangungjawaban} ke database, dan memicu
	 * proses-proses terkait: pembuatan record pajak (PPh), pembuatan
	 * {@code DaftarPengajuanTransfer} bila disetujui, update relasi ke {@code UangMuka}
	 * induk, dan cetak laporan otomatis.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Validasi yang dilakukan sebelum simpan:</p>
	 * <ul>
	 *   <li>Uang Muka wajib dipilih.</li>
	 *   <li>Judul Pengajuan wajib diisi.</li>
	 *   <li>Tanggal Stor wajib diisi bila ada sisa yang harus dikembalikan
	 *       (dikembalikan > 0.1).</li>
	 *   <li>Total nilai biaya tidak boleh melebihi nilai uang muka yang diajukan.</li>
	 * </ul>
	 * <p>Setelah validasi lolos:</p>
	 * <ol>
	 *   <li>Hitung ulang total nilai dan pajak dari JSONArray.</li>
	 *   <li>Load ulang entitas dari session Hibernate bila ID sudah ada (untuk menghindari
	 *       stale data dan optionistic locking issues).</li>
	 *   <li>Set semua field entitas dari komponen UI.</li>
	 *   <li>Bila status DISETUJU: set disetujuiOleh ke pengguna saat ini dan tanggal
	 *       persetujuan. Bila tidak: clear kedua field tersebut.</li>
	 *   <li>Simpan (save baru atau update) ke session dan flush.</li>
	 *   <li>Jalankan timer async untuk: update relasi uangMuka.pertangungjawaban, cetak
	 *       laporan, dan buat record Pajak per baris formula.</li>
	 * </ol>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param event Event dari tombol simpan ZK. Tidak digunakan langsung tetapi
	 *        diperlukan oleh konvensi event handler.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return {@code true} bila simpan berhasil dan pemanggil harus menutup window.
	 *         {@code false} bila validasi gagal dan form harus tetap terbuka untuk
	 *         koreksi pengguna.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Validasi menampilkan {@code MyMessageboxConfig} dan return false. Exception Hibernate
	 * atau lainnya dilempar ke pemanggil (event handler simpan).</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil dari thread event ZK. Operasi Hibernate sinkron. Timer async post-save
	 * dieksekusi di thread event queue ZK terpisah.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Perhatikan bahwa {@code nilaipajak} dan {@code dikembalikan} adalah field instance
	 * yang diupdate oleh hitungTotal dan dibaca kembali di onSave. Bila ada refactoring
	 * kalkulasi, pastikan kedua field ini tetap sinkron. Race condition tidak mungkin
	 * karena semua dalam satu thread event ZK.</p>
	 *
	 * @param event event ZK dari tombol simpan
	 * @return true jika berhasil disimpan, false jika validasi gagal
	 * @throws Exception jika terjadi error saat operasi Hibernate atau proses terkait
	 */
	public boolean onSave(Event event) throws Exception {

		UangMuka work = (UangMuka) (uangMuka.getAttribute("uangMuka"));
		if (work == null) {
			MyMessageboxConfig.show("Mohon maaf, Uang Muka belum dipilih. Langkah yang dapat dilakukan: (1) Pilih data Uang Muka yang akan dipertanggungjawabkan dari field pencarian; (2) Pastikan pengajuan uang muka sudah ada dan berstatus valid; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Judul Pengajuan belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Judul Pengajuan dengan deskripsi singkat pertanggungjawaban; (2) Pastikan judul tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (tanggalStor.getValue() == null && dikembalikan > 0.1) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Stor belum diisi. Langkah yang dapat dilakukan: (1) Isikan atau pilih Tanggal Stor menggunakan date picker; (2) Tanggal stor diperlukan karena terdapat nilai yang dikembalikan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");
		nilai = 0.0;
		for (int i = 0; i < array.length(); i++) {
			Double jumlah = 0.0;
			JSONObject jsonObject = array.getJSONObject(i);
			if (!jsonObject.isNull("jumlah")) {
				jumlah = jsonObject.getDouble("jumlah");
			}

			JenisPajakBarang barang;
			if (!jsonObject.isNull("pajak")) {
				barang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
						Long.parseLong(jsonObject.get("pajak") + ""));
			} else {
				barang = null;
			}

			Double ppn = 0.0;
			if (!jsonObject.isNull("ppn")) {
				ppn = jsonObject.getDouble("ppn");
			}

			Double pajak = barang == null ? 0.0 : ((barang.getPersen() / 100.0) * jumlah);

			Double tot = (jumlah + ((ppn / 100.0) * jumlah)) - (pph_mengurangi_lpj ? pajak : 0.0);

			nilai += tot;
		}

		if (work.getNilai().longValue() < nilai.longValue()) {
			MyMessageboxConfig.show("Mohon maaf, nilai yang dipertanggungjawabkan melebihi sisa nilai pengajuan. Langkah yang dapat dilakukan: (1) Kurangi total nilai pertanggungjawaban agar tidak melebihi sisa nilai pengajuan uang muka; (2) Periksa kembali rincian biaya yang dimasukkan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pertangungjawaban.getId() != null) {
			pertangungjawaban = (Pertangungjawaban) session.load(Pertangungjawaban.class, pertangungjawaban.getId());
		}

		if (pertangungjawaban.getDibuatOleh() == null) {
			pertangungjawaban.setDibuatOleh(tbmuser);
			pertangungjawaban.setTanggalPembuatan(new Date());
		}
		if (disposisiSop != null && disposisiSop.getId() != null) {
			pertangungjawaban.setDisposisiSop(disposisiSop);
		}

		pertangungjawaban.setUangMuka(work);
		pertangungjawaban.setKode(kode.getValue());
		pertangungjawaban.setNama(nama.getValue());
		pertangungjawaban.setNilai(nilai);
		pertangungjawaban.setDikembalikan(dikembalikan);
		pertangungjawaban.setPajak(nilaipajak);
		pertangungjawaban.setKeterangan(keterangan.getValue());

		pertangungjawaban.setFormula(array.toString());

		pertangungjawaban.setNamaSponsor(namaSponsor.getValue());
		pertangungjawaban.setDariSponsor(dariSponsor.getValue());

		pertangungjawaban.setTanggalStor(tanggalStor.getValue());

		String sts = (String) (status.getSelectedItem() == null ? null : status.getSelectedItem().getValue());
		if (sts != null && sts.equals(DanaTalangan.DISETUJU)) {
			pertangungjawaban.setDisetujuiOleh(tbmuser);
			pertangungjawaban.setTanggalPersetujuan(tanggalPersetujuanManual.getValue());
		} else {
			pertangungjawaban.setDisetujuiOleh(null);
			pertangungjawaban.setTanggalPersetujuan(null);
		}

		pertangungjawaban.setStatus(sts);
		pertangungjawaban.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
		if (pertangungjawaban.getId() != null) {
			session.update(pertangungjawaban);
		} else {
			pertangungjawaban.setDibuatOleh(tbmuser);
			String noAgenda = generateCode(true, (SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
			kode.setValue(noAgenda);
			pertangungjawaban.setKode(kode.getValue());
			session.save(pertangungjawaban);
		}

		session.flush();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				UangMuka work = (UangMuka) (uangMuka.getAttribute("uangMuka"));
				if (work != null) {
					Session session = HibernateUtil.currentSession();
					session.refresh(work);
					work.setPertangungjawaban(pertangungjawaban);
					Common.refreshUpdate(session, work);
					session.flush();

				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						cetak(PertangungjawabanAction.this.pertangungjawaban);
					}
				}, "Proses cetak", false, 2500);

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						for (int i = 0; i < array.length(); i++) {
							JSONObject jsonObject = array.getJSONObject(i);
							try {
								Pajak.buat(pertangungjawaban, null, jsonObject, null);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}

						if (pertangungjawaban.getDisetujuiOleh() != null) {
							DaftarPengajuanTransfer.simpanPertangungjawaban(pertangungjawaban);
						}
					}
				});
			}
		});

		return true;
	}

	/**
	 * <h3>initCriteria — Membangun Hibernate Criteria Pencarian LPJ</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Membangun objek {@code Criteria} Hibernate yang merepresentasikan query pencarian
	 * data pertanggungjawaban berdasarkan semua filter yang aktif di UI. Method ini
	 * dipanggil dua kali untuk setiap pencarian: sekali untuk menghitung total baris
	 * (paging) tanpa order, dan sekali untuk mengambil data halaman saat ini dengan
	 * order descending by ID.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method membaca nilai filter dari komponen UI dan menambahkan restriction ke
	 * Criteria secara bertahap:</p>
	 * <ul>
	 *   <li>Filter satuan kerja: membaca dari {@code searchparent} banbox, membangun
	 *       Set satuan kerja beserta seluruh hierarki anaknya via
	 *       {@code satuanKerjaTreeModel.getChildsSet}. Query menggunakan OR antara
	 *       isNull("satuanKerja") dan in(satuanKerjas) untuk mencakup LPJ tanpa
	 *       satuan kerja.</li>
	 *   <li>Filter tanggal: SQL restriction "date(tanggal_pembuatan) between date(start)
	 *       and date(end)" menggunakan format database agar independen dari timezone.</li>
	 *   <li>Filter status: eq("status", selectedValue) atau sqlRestriction("true") bila
	 *       "Semua" dipilih.</li>
	 *   <li>Filter aktif: bila checkbox aktif dicentang, hanya tampilkan aktif=true atau
	 *       aktif IS NULL.</li>
	 *   <li>Filter kode dan nama: ilike dengan MatchMode.ANYWHERE untuk pencarian
	 *       substring case-insensitive.</li>
	 * </ul>
	 * <p>Bila parameter {@code order} true, criteria ditambahkan
	 * {@code Order.desc("id")} sehingga data terbaru tampil di atas.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param order {@code true} untuk menambahkan order descending by ID (digunakan
	 *        saat mengambil data halaman), {@code false} untuk count saja (digunakan
	 *        saat menghitung jumlah baris untuk paging).</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return {@code Criteria} Hibernate yang siap untuk dieksekusi dengan
	 *         {@code setMaxResults}, {@code setFirstResult}, dan {@code list()} atau
	 *         {@code setProjection(rowCount()).uniqueResult()}.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception dilempar ke pemanggil. Error umum: HibernateException bila session
	 * tidak aktif, NullPointerException bila filter tanggal start/end null (sudah
	 * diantisipasi dengan pengecekan null).</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil dari thread event ZK. Menggunakan {@code HibernateUtil.currentSession()}
	 * yang terikat ke thread. Tidak aman untuk dipanggil dari thread berbeda.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila ada filter baru di UI (misalnya filter jenis LPJ atau filter approver),
	 * tambahkan restriction baru di sini. Perhatikan bahwa setiap restriction dengan
	 * join alias baru harus menggunakan nama alias yang unik agar tidak konflik dengan
	 * restriction lain. Format tanggal untuk SQL restriction menggunakan
	 * {@code Common.databaseDateFormat} yang thread-safe.</p>
	 *
	 * @param order true untuk tambahkan order DESC by ID, false untuk tanpa order
	 * @return Criteria Hibernate yang dikonfigurasi dengan semua filter aktif
	 */
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Pertangungjawaban.class)

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
		if (order)
			criteria.addOrder(Order.desc("id"));
		return criteria;
	}

	/**
	 * <h3>onSearchDefault — Eksekusi Pencarian dan Render Grid Hasil</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Mengimplementasikan kontrak interface {@code DataSearchDefault} untuk menjalankan
	 * pencarian data pertanggungjawaban berdasarkan filter yang aktif dan menampilkan
	 * hasilnya di grid utama halaman. Method ini dipanggil dari banyak tempat: timer
	 * awal, perubahan paging, perubahan filter satuan kerja, setelah simpan, dan setelah
	 * hitung ulang.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method melakukan dua langkah utama:</p>
	 * <ol>
	 *   <li>Menghitung jumlah total data dengan {@code initCriteria(false)} dan mengupdate
	 *       komponen paging melalui {@code Common.initPaging(criteria, paging)} agar
	 *       navigasi halaman mencerminkan jumlah data terkini.</li>
	 *   <li>Mengambil data halaman saat ini dengan {@code initCriteria(true)},
	 *       {@code setMaxResults(Common.ROWS_COUNT_ON_PAGE)}, dan
	 *       {@code setFirstResult(ROWS_COUNT_ON_PAGE * activePage)} sesuai halaman
	 *       paging yang aktif.</li>
	 * </ol>
	 * <p>Hasil query dibungkus dalam {@code SimpleListModel} dan di-set ke grid
	 * dengan renderer {@code PertangungjawabanRenderer}. ZK kemudian memanggil
	 * renderer untuk setiap baris data.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param event Event yang memicu pencarian (bisa null bila dipanggil secara
	 *        programatik dari timer atau post-save). Parameter tidak digunakan
	 *        dalam implementasi ini.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Grid diperbarui secara langsung sebagai efek samping.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception Hibernate dilempar ke pemanggil. Bila dipanggil dari timer ZK,
	 * ZK framework akan menangkap exception dan menampilkannya sesuai konfigurasi
	 * error handler global.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Dipanggil dari thread event ZK. {@code @SuppressWarnings("unchecked")} digunakan
	 * karena {@code Criteria.list()} mengembalikan raw List tanpa generic type parameter
	 * pada Hibernate 3.x yang digunakan.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila ada kebutuhan penanganan error yang lebih robust (seperti di UangMukaAction
	 * yang memiliki rollback/retry), tambahkan blok try-catch di sini. Konstanta
	 * {@code Common.ROWS_COUNT_ON_PAGE} mengontrol ukuran halaman secara global.</p>
	 *
	 * @param event event pemicu pencarian, boleh null
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pertangungjawaban> pertangungjawaban = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pertangungjawaban);
		grid.setRowRenderer(new PertangungjawabanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <h3>istilah — Mengembalikan Nama Modul untuk SOP dan Label</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Mengimplementasikan kontrak interface {@code FormSop} yang mengharuskan setiap
	 * form menyediakan nama istilah modul yang ditanganinya. Nama ini digunakan oleh
	 * framework SOP untuk menampilkan label yang deskriptif dalam alur persetujuan,
	 * log disposisi, dan notifikasi.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method mengembalikan string literal "Pertanggungjawaban Uang Muka" yang merupakan
	 * nama resmi modul ini. String ini bersifat statis dan tidak bergantung pada data
	 * apapun.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return String "Pertanggungjawaban Uang Muka" sebagai nama istilah modul.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila nama modul perlu diubah (misalnya untuk alasan lokalisasi atau branding),
	 * cukup ubah string yang dikembalikan. Pertimbangkan untuk menggunakan konstanta
	 * atau resource bundle bila sistem mendukung multi-bahasa.</p>
	 *
	 * @return nama istilah modul pertanggungjawaban uang muka
	 * @throws Exception tidak akan dilempar dalam implementasi ini
	 */
	@Override
	public String istilah() throws Exception {
		return "Pertanggungjawaban Uang Muka";
	}

	/**
	 * <h3>ambil — Mengembalikan Entitas LPJ yang Sedang Aktif</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Mengimplementasikan kontrak interface {@code FormSop} yang memungkinkan
	 * framework SOP mengambil referensi ke entitas {@code DataSop} (implementor
	 * {@code Pertangungjawaban}) yang sedang diproses di form ini. Digunakan
	 * oleh mekanisme alur SOP untuk mengaitkan disposisi dengan entitas yang benar.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method mengembalikan field {@code pertangungjawaban} yang merupakan entitas
	 * yang sedang aktif di form. Entitas ini di-set saat {@code init} dipanggil
	 * dan mungkin null bila form belum diinisialisasi.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return Entitas {@code Pertangungjawaban} yang sedang aktif di form, atau null
	 *         bila form belum diinisialisasi.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Method ini tidak boleh pernah mengembalikan objek selain {@code Pertangungjawaban}
	 * atau null karena akan menyebabkan ClassCastException di framework SOP.</p>
	 *
	 * @return entitas Pertangungjawaban yang sedang aktif
	 * @throws Exception tidak akan dilempar dalam implementasi ini
	 */
	@Override
	public DataSop ambil() throws Exception {
		return pertangungjawaban;
	}

	/**
	 * <h3>ambilClass — Mengembalikan Tipe Kelas Entitas yang Dikelola</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Mengimplementasikan kontrak interface {@code FormSop} untuk menyediakan
	 * referensi tipe kelas entitas yang dikelola oleh form ini. Digunakan oleh
	 * framework SOP dan CRUD generik untuk operasi refleksi, pembuatan query
	 * dinamis, dan identifikasi tipe entitas tanpa perlu instanceof.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method mengembalikan {@code Pertangungjawaban.class}, yang merupakan Class
	 * object dari entitas Hibernate yang dikelola kelas ini. Penggunaan
	 * {@code @SuppressWarnings("rawtypes")} diperlukan karena return type interface
	 * menggunakan raw {@code Class} tanpa generic parameter untuk kompatibilitas
	 * Java 1.5/1.6 era lama.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return {@code Class} object dari {@code Pertangungjawaban}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Method ini hanya perlu diubah bila nama atau paket kelas entitas berubah.
	 * Bila menggunakan generics di masa mendatang, pertimbangkan untuk mengubah
	 * signature menjadi {@code Class<Pertangungjawaban>}.</p>
	 *
	 * @return Class object untuk tipe entitas Pertangungjawaban
	 * @throws Exception tidak akan dilempar dalam implementasi ini
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return Pertangungjawaban.class;
	}

	/**
	 * <h3>generateCode — Menghasilkan Kode Nomor Surat LPJ</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Menghasilkan kode nomor surat unik untuk dokumen pertanggungjawaban baru
	 * berdasarkan konfigurasi format nomor surat yang telah ditetapkan administrator.
	 * Kode ini berfungsi sebagai identifikasi dokumen yang readable oleh manusia
	 * (contoh: "LPJ/001/REKTORAT/VI/2026").</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method pertama memeriksa apakah konfigurasi nomor surat pertanggungjawaban
	 * ({@code NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_DATA}) sudah dikonfigurasi.
	 * Bila belum, fallback ke barcode random via {@code Common.getGeneratedBarCode()}.
	 * Bila sudah dikonfigurasi:</p>
	 * <ol>
	 *   <li>Tentukan index: bila konfigurasi menggunakan index urut global
	 *       ({@code getGunakanIndexUrut()}), ambil dari {@code getNomorIndex()}.
	 *       Bila tidak, hitung dari jumlah data yang sudah ada via {@code getindex()}.</li>
	 *   <li>Bila parameter {@code tambah} true (simpan sungguhan, bukan preview),
	 *       increment index di database via {@code NomorSurat.tambahIndexNomorSurat}
	 *       untuk mencegah duplikasi pada simpan berikutnya.</li>
	 *   <li>Format kode menggunakan {@code format(index, tanggal, satuanKerja)} yang
	 *       menggabungkan prefix, index dengan padding, tahun/bulan, dan kode satuan kerja
	 *       sesuai pola format yang dikonfigurasi.</li>
	 *   <li>Pastikan kode unik di database via {@code KodeUnikUtil.pastikanUnik}
	 *       dengan menambahkan sufiks "-2", "-3", dst bila duplikat.</li>
	 * </ol>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param tambah {@code true} bila kode ini untuk simpan sungguhan (index akan
	 *        di-increment), {@code false} bila hanya untuk preview/display sementara.
	 * @param satuanKerja Satuan kerja yang kodenya akan dimasukkan dalam format nomor
	 *        surat. Boleh null bila format tidak menggunakan kode satuan kerja.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return String kode nomor surat yang unik dan sudah terformat.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception tidak dilempar dari method ini. Bila format gagal karena konfigurasi
	 * salah, {@code NomorSurat.format} mungkin melempar runtime exception.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Tidak thread-safe bila dipanggil bersamaan karena increment index tidak
	 * menggunakan locking optimistik. Dalam praktiknya aman karena ZK menjalankan
	 * event satu per satu per session, dan race condition antar user sangat jarang
	 * karena sudah ada guard {@code KodeUnikUtil.pastikanUnik}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila format nomor surat berubah, ubah konfigurasi di tabel nomor_surat melalui
	 * UI admin, bukan di kode ini. Method ini hanya mengikuti konfigurasi yang ada.
	 * Perhatikan bahwa {@code tambah=false} digunakan saat form pertama dibuka (preview)
	 * dan {@code tambah=true} hanya saat {@code onSave} setelah validasi lolos.</p>
	 *
	 * @param tambah true untuk increment counter nomor surat di database
	 * @param satuanKerja satuan kerja untuk substitusi dalam format nomor surat
	 * @return kode nomor surat yang telah diformat dan dijamin unik
	 */
	private String generateCode(boolean tambah, SatuanKerja satuanKerja) {
		if (NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_DATA == null
				|| NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		NomorSurat ns = NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_DATA.getNomorSurat();
		Long index = (tambah && ns.getGunakanIndexUrut()) ? NomorSurat.ambilLaluTambahIndexNomorSurat(ns)
				: (ns.getGunakanIndexUrut() ? ns.getNomorIndex() : getindex(ns));
		String noAgenda = ns.format(index, WaktuUtil.getDate(), satuanKerja);
		return ais.action.master.KodeUnikUtil.pastikanUnik(Pertangungjawaban.class, noAgenda);
	}

	/**
	 * <h3>getindex — Menghitung Index Berikutnya untuk Nomor Surat LPJ</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Menghitung index (nomor urut) berikutnya yang akan digunakan dalam pembentukan
	 * kode nomor surat pertanggungjawaban. Index dihitung berdasarkan jumlah data
	 * yang sudah ada dengan mempertimbangkan scope urutan (per nomor surat, per
	 * kelompok, per tahun, per bulan, atau sejak tanggal reset tertentu) sesuai
	 * konfigurasi {@code NomorSurat}.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method membuat Hibernate Criteria pada kelas {@code Pertangungjawaban} dengan
	 * left join ke {@code nomorSuratAlurKeuangan} dan {@code nomorSurat} untuk
	 * menghitung jumlah dokumen yang masuk dalam scope yang sama dengan konfigurasi
	 * nomor surat yang diberikan. Restrictions yang diterapkan:</p>
	 * <ul>
	 *   <li>Bila urut berdasarkan nomor surat spesifik: hanya hitung yang memiliki
	 *       FK ke nomor surat yang sama.</li>
	 *   <li>Bila urut berdasarkan kelompok nomor surat: hanya hitung yang satu
	 *       kelompok.</li>
	 *   <li>Bila reset tiap tahun: hanya hitung yang tahun-nya sama dengan tahun saat ini.</li>
	 *   <li>Bila reset tiap bulan: hanya hitung yang tahun dan bulan-nya sama.</li>
	 *   <li>Bila ada tanggal reset spesifik: hanya hitung yang tanggal pembuatan
	 *       >= tanggal reset.</li>
	 * </ul>
	 * <p>Hasil rowCount() ditambahkan 1 dan dikembalikan sebagai index berikutnya.</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param nomorSurat Konfigurasi nomor surat yang menentukan scope penomoran.
	 *        Bila null, method langsung mengembalikan 0.</p>
	 *
	 * <p><strong>Return:</strong>
	 * @return Long berupa index urut berikutnya (jumlah data dalam scope + 1).
	 *         Minimum return value adalah 1L.</p>
	 *
	 * <p><strong>Penanganan error:</strong><br>
	 * Exception Hibernate dilempar ke pemanggil. Bila {@code nomorSurat} null,
	 * method langsung return 0L tanpa query database.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Menggunakan {@code HibernateUtil.currentSession()} yang terikat ke thread.
	 * Tidak ada sinkronisasi eksplisit; dalam skenario concurrency tinggi, dua
	 * pengguna bisa mendapat index yang sama (race condition), yang kemudian
	 * diatasi oleh {@code KodeUnikUtil.pastikanUnik} dengan sufiks.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Perhatikan bahwa kalkulasi bulan menggunakan {@code Calendar.MONTH + 1} karena
	 * Java Calendar menggunakan 0-based month. Field {@code tahun} dan {@code bulan}
	 * di entitas {@code Pertangungjawaban} harus tersimpan dengan benar saat save
	 * agar query ini akurat. Bila ada perubahan pada struktur tabel atau field
	 * nomorSuratAlurKeuangan, update alias path dalam createAlias.</p>
	 *
	 * @param nomorSurat konfigurasi nomor surat untuk menentukan scope urutan
	 * @return index urut berikutnya untuk nomor surat dalam scope yang ditentukan
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(Pertangungjawaban.class)
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
	 * <h3>setPersetujuan — Mengatur Mode Persetujuan Secara Programatik</h3>
	 *
	 * <p><strong>Tujuan:</strong><br>
	 * Mengimplementasikan kontrak interface {@code FormSop} yang memungkinkan
	 * framework SOP mengatur mode persetujuan pada instance form ini tanpa harus
	 * membuat instance baru. Digunakan ketika alur SOP perlu mengubah konteks
	 * tampilan form secara dinamis berdasarkan tahapan alur.</p>
	 *
	 * <p><strong>Cara kerja:</strong><br>
	 * Method hanya menyimpan nilai {@code persetujuan} ke field instance. Perubahan
	 * ini akan berdampak pada semua method yang membaca field {@code persetujuan}:
	 * {@code form()} untuk mengontrol read-only field, {@code onSave()} untuk logika
	 * persetujuan, {@code init()} untuk judul window, dan berbagai event listener
	 * inline di form. Perubahan mode baru efektif saat form diinisialisasi ulang
	 * (panggilan {@code init} atau {@code form} berikutnya).</p>
	 *
	 * <p><strong>Parameter:</strong>
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan,
	 *        {@code false} untuk mode pengajuan normal.</p>
	 *
	 * <p><strong>Return:</strong><br>
	 * Tidak ada return value. Perubahan mode langsung tersimpan di field instance.</p>
	 *
	 * <p><strong>Threading:</strong><br>
	 * Tidak thread-safe bila dipanggil dari thread lain saat form sedang ditampilkan.
	 * Dalam penggunaan normal, dipanggil sebelum form diinisialisasi, jadi aman.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong><br>
	 * Bila ada mode tambahan selain persetujuan (misal: mode audit atau mode direktur),
	 * pertimbangkan untuk menggunakan enum daripada boolean. Method ini adalah satu-satunya
	 * setter resmi untuk field {@code persetujuan} dari luar kelas.</p>
	 *
	 * @param persetujuan true untuk mode persetujuan, false untuk mode pengajuan
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

}
