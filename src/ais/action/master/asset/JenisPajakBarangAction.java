package ais.action.master.asset;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.asset.JenisPajakBarang;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>JenisPajakBarangAction — Kontroler Master Jenis Pajak Barang/Jasa</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZK (ZKoss 5.5) untuk halaman manajemen master
 * data Jenis Pajak Barang/Jasa pada modul aset. Setiap entri data mendefinisikan
 * satu jenis pajak yang dapat diterapkan pada transaksi pengadaan atau penerimaan
 * barang/jasa, meliputi kode pajak, nama pajak, akun hutang pajak, akun dana
 * titipan pajak, serta persentase tarif pajak yang berlaku. Data ini menjadi
 * referensi utama bagi proses penghitungan pajak pada dokumen pembelian dan
 * faktur di seluruh modul aset dan akunting. Dengan pemisahan jenis pajak
 * menjadi entitas terpisah, sistem mendukung pengelolaan multi-tarif pajak
 * seperti PPN, PPh pasal 22, dan pajak lainnya secara independen.
 *
 * <b>Cara kerja:</b><br>
 * Kontroler ini mengimplementasikan tiga antarmuka sekaligus: {@code DataCriteria}
 * untuk membangun kriteria pencarian Hibernate, {@code DataSearchDefault} untuk
 * menjalankan pencarian dan memuat ulang grid, serta {@code DataInitDefault}
 * untuk menginisialisasi formulir ubah/salin data. Siklus hidup dimulai dari
 * {@code doBeforeCompose} yang memeriksa keamanan akses, dilanjutkan
 * {@code doAfterCompose} yang menginisialisasi komponen UI, mengatur hak akses
 * tombol, memasang paginasi, dan mendaftarkan tombol ekspor data serta unggah.
 * Grid utama menggunakan {@code JenisPajakBarangRenderer} sebagai renderer baris
 * yang menampilkan kode, nama (dengan tautan revisi), akun hutang pajak, akun
 * dana titipan, persentase, keterangan, checkbox aktif, dan tombol aksi
 * (salin/ubah/hapus). Tab "Data PPN" dimuat secara malas (lazy load) hanya
 * ketika pengguna pertama kali mengklik tab tersebut melalui {@code onDataPpn}.
 * Formulir tambah/ubah dibangun secara programatik di dalam {@code MyWindow}
 * modal menggunakan komponen ZK seperti {@code Borderlayout}, {@code MyGrid},
 * {@code AmbilDataAkunBanbox}, dan {@code MyDoublebox}.
 *
 * <b>Threading:</b><br>
 * Seluruh interaksi dengan basis data dilakukan melalui {@code HibernateUtil.currentSession()}
 * yang merupakan sesi Hibernate yang dikelola per-thread oleh konteks ZK. Tidak
 * ada thread latar belakang yang dibuat oleh kelas ini. Semua operasi berjalan
 * di thread event ZK sehingga aman terhadap pembaruan komponen UI. Listener
 * event checkbox aktif dan tombol simpan berjalan secara sinkron dalam thread
 * event ZK yang sama.
 *
 * <b>Pemeliharaan:</b><br>
 * Untuk menambahkan field baru, tambahkan variabel instance, inisialisasi di
 * metode {@code init(JenisPajakBarang)}, set nilai di {@code onSave}, dan
 * tambahkan ke array {@code contents} agar ikut diekspor/diunggah. Pastikan
 * entitas {@code JenisPajakBarang} memiliki kolom yang sesuai dan mapping
 * Hibernate sudah dikonfigurasi. Jika tarif pajak perlu divalidasi (misal: 0–100),
 * tambahkan logika validasi di {@code onSave} sebelum pemanggilan
 * {@code Common.refreshSaveOrUpdate}. Perhatikan bahwa akun hutang pajak
 * ({@code akun}) bersifat wajib, sedangkan akun dana titipan opsional.
 *
 * @author Sistem AIS
 * @version 1.0
 * @see JenisPajakBarang
 * @see DataCriteria
 * @see DataSearchDefault
 * @see DataInitDefault
 */
public class JenisPajakBarangAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Nomor versi serialisasi untuk kompatibilitas serialisasi Java.
	 * Nilai ini tidak boleh diubah kecuali terjadi perubahan struktural
	 * yang disengaja pada kelas ini.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZK untuk formulir tambah atau ubah data jenis pajak barang/jasa. */
	private MyWindow addWindow;

	/** Komponen paginasi ZK untuk navigasi halaman pada grid data utama. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar jenis pajak barang/jasa. */
	private MyGrid grid;

	/** Kotak teks untuk memasukkan kata kunci pencarian berdasarkan nama pajak. */
	private Textbox searchnama;

	/** Checkbox filter untuk membatasi tampilan hanya pada data dengan status aktif. */
	private Checkbox searchaktif;

	/** Kotak teks pada formulir untuk memasukkan nama jenis pajak barang/jasa. */
	private Textbox nama;

	/** Kotak teks pada formulir untuk memasukkan keterangan atau deskripsi tambahan. */
	private Textbox keterangan;

	/**
	 * Flag hak akses ubah data. Bernilai {@code true} jika pengguna saat ini
	 * memiliki hak {@code CommonPrivilages.UPDATE}, {@code false} jika tidak.
	 */
	private boolean edit = false;

	/**
	 * Flag hak akses hapus data. Bernilai {@code true} jika pengguna saat ini
	 * memiliki hak {@code CommonPrivilages.DELETE}, {@code false} jika tidak.
	 */
	private boolean delete = false;

	/** Entitas jenis pajak barang/jasa yang sedang diedit atau dibuat baru. */
	private JenisPajakBarang jenisPajakBarang;

	/** Tombol toolbar untuk menambah data jenis pajak barang/jasa baru. */
	private MyToolbarbuttonConfig add;

	/** Kotak teks pada formulir untuk memasukkan kode unik jenis pajak. */
	private Textbox kode;

	/** Komponen pemilih akun untuk akun hutang pajak (akun kredit pada jurnal pajak). */
	private AmbilDataAkunBanbox akun;

	/** Komponen input angka desimal untuk memasukkan persentase tarif pajak (misalnya 11 untuk 11%). */
	private MyDoublebox persen;

	/** Panel tab yang menampung konten halaman data PPN yang dimuat secara malas. */
	private Tabpanel dataPpn;

	/** Komponen pemilih akun untuk akun dana titipan pajak (akun sementara sebelum disetor). */
	private AmbilDataAkunBanbox akunDanaTitipan;

	/**
	 * <b>Tujuan:</b> Menangani event ketika pengguna mengklik tab "Data PPN" untuk
	 * memuat konten halaman PPN secara malas (lazy loading) ke dalam panel tab.
	 *
	 * <b>Cara kerja:</b> Metode ini memeriksa apakah panel tab {@code dataPpn} sudah
	 * memiliki anak komponen. Jika belum (ukuran children == 0), maka akan dibuat
	 * sebuah {@code MyWindow} baru dengan lebar dan tinggi 100% sebagai wadah,
	 * kemudian disisipkan komponen {@code MyInclude} yang memuat halaman ZUL
	 * {@code /pages/master/asset/jenis_pajak_ppn.zul} ke dalamnya. Pendekatan lazy
	 * loading ini memastikan bahwa halaman PPN hanya dimuat ke memori ketika
	 * benar-benar dibutuhkan, sehingga mempercepat waktu inisialisasi halaman utama
	 * dan menghemat sumber daya server.
	 *
	 * <b>Parameter:</b> Event yang dipicu ketika tab PPN diklik oleh pengguna.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error khusus. Jika halaman ZUL
	 * yang dirujuk tidak ditemukan, ZK akan melempar exception yang ditangkap
	 * oleh framework ZK secara otomatis dan ditampilkan sebagai pesan error.
	 *
	 * <b>Pemeliharaan:</b> Jika path halaman PPN berubah, perbarui argumen string
	 * pada konstruktor {@code MyInclude}. Jika perlu melewatkan parameter ke
	 * halaman PPN, gunakan {@code iframe.setDynamicProperty} sebelum {@code setParent}.
	 *
	 * @param event event ZK yang dipicu saat tab PPN diklik, tidak digunakan secara langsung
	 */
	public void onDataPpn(Event event) {

		if (dataPpn.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(dataPpn);
			MyInclude iframe = new MyInclude("/pages/master/asset/jenis_pajak_ppn.zul");
			iframe.setParent(window);
		}
	}

	/**
	 * <b>Tujuan:</b> Memeriksa keamanan akses sebelum komponen ZK dikompilasi dan
	 * dirakit oleh framework, memastikan hanya pengguna yang terautentikasi dan
	 * berhak yang dapat mengakses halaman ini.
	 *
	 * <b>Cara kerja:</b> Metode ini dipanggil oleh framework ZK sebelum fase
	 * {@code doAfterCompose}. Metode memanggil {@code Common.doCheckSecurity()} yang
	 * akan mengarahkan pengguna ke halaman login jika sesi tidak valid atau tidak
	 * memiliki hak akses yang diperlukan. Setelah pemeriksaan keamanan berhasil,
	 * metode memanggil implementasi induk {@code super.doBeforeCompose} untuk
	 * melanjutkan proses perakitan komponen secara normal.
	 *
	 * <b>Parameter:</b><br>
	 * - {@code page}: halaman ZK saat ini yang sedang dikompose<br>
	 * - {@code parent}: komponen induk dalam hierarki komponen ZK<br>
	 * - {@code compInfo}: metadata informasi komponen dari file ZUL
	 *
	 * <b>Return:</b> Objek {@code ComponentInfo} dari pemanggilan super yang
	 * diperlukan oleh framework ZK untuk melanjutkan proses komposisi.
	 *
	 * <b>Penanganan error:</b> {@code Common.doCheckSecurity()} akan melakukan
	 * redirect ke halaman login jika pengguna tidak terautentikasi, sehingga
	 * eksekusi tidak akan mencapai pemanggilan {@code super} jika keamanan gagal.
	 *
	 * <b>Pemeliharaan:</b> Jangan menghapus pemanggilan {@code Common.doCheckSecurity()}
	 * dari metode ini karena akan membuka celah keamanan pada halaman ini.
	 *
	 * @param page     halaman ZK yang sedang dikompose
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo informasi metadata komponen dari ZUL
	 * @return {@code ComponentInfo} hasil dari pemanggilan {@code super.doBeforeCompose}
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Menginisialisasi seluruh komponen UI dan logika bisnis setelah
	 * framework ZK selesai merakit komponen dari file ZUL, termasuk pengaturan hak
	 * akses, paginasi, pencarian default, dan tombol ekspor/impor data.
	 *
	 * <b>Cara kerja:</b> Metode ini dipanggil secara otomatis oleh framework ZK
	 * setelah fase wire (pengikatan variabel). Urutan inisialisasi adalah sebagai
	 * berikut: (1) memanggil {@code super.doAfterCompose} untuk memproses wire
	 * otomatis, (2) memanggil {@code Common.initLaguage()} untuk mengatur bahasa
	 * antarmuka, (3) menyetel visibilitas tombol tambah sesuai hak akses CREATE,
	 * (4) membaca flag edit dan delete dari hak akses pengguna, (5) menjalankan
	 * pencarian awal dengan {@code onSearchDefault}, (6) memasang event listener
	 * paginasi agar grid diperbarui ketika halaman berubah, (7) mendaftarkan tombol
	 * ekspor data dengan kolom yang telah ditentukan, dan (8) mendaftarkan tombol
	 * unggah data yang hanya terlihat jika pengguna memiliki hak tambah, ubah,
	 * dan hapus sekaligus.
	 *
	 * <b>Parameter:</b> {@code comp} adalah komponen root dari halaman ZUL yang
	 * telah selesai dirakit oleh framework ZK.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception} umum yang akan ditangkap
	 * oleh framework ZK. Jika terjadi error pada inisialisasi komponen, framework
	 * akan menampilkan pesan error sesuai konfigurasi ZK.
	 *
	 * <b>Pemeliharaan:</b> Jika ada kolom baru yang perlu diekspor, tambahkan nama
	 * properti ke array {@code contents}. Urutan elemen dalam array {@code contents}
	 * menentukan urutan kolom pada file ekspor Excel. Pastikan nama properti sesuai
	 * dengan getter pada kelas entitas {@code JenisPajakBarang}.
	 *
	 * @param comp komponen root halaman ZUL yang telah selesai dirakit
	 * @throws Exception jika terjadi kesalahan saat inisialisasi komponen UI
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "akun", "akunDanaTitipan", "persen", "keterangan",
				"aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisPajakBarang.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisPajakBarang.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <b>Tujuan:</b> Kelas renderer baris grid yang bertanggung jawab merender satu
	 * baris data {@code JenisPajakBarang} ke dalam baris komponen {@code Row} ZK
	 * pada grid utama halaman manajemen jenis pajak barang/jasa.
	 *
	 * <b>Cara kerja:</b> Setiap kali grid perlu menampilkan satu entri data, metode
	 * {@code render} dipanggil dengan objek {@code Row} kosong dan objek data
	 * {@code JenisPajakBarang}. Renderer membuat dan menambahkan komponen berikut
	 * ke baris secara berurutan: (1) Label kode pajak, (2) Vbox dengan tautan revisi
	 * nama melalui {@code RevisiHelper.createNewRevisi} yang menampilkan nama dan
	 * memungkinkan pengguna melihat riwayat perubahan, (3) Label kode dan nama akun
	 * hutang pajak (kosong jika null), (4) Label kode dan nama akun dana titipan
	 * (kosong jika null), (5) Label persentase tarif yang diformat menggunakan
	 * {@code Common.numberFormat}, (6) Label keterangan, (7) Checkbox aktif yang
	 * dapat diklik untuk langsung mengubah status aktif tanpa membuka formulir,
	 * dan (8) tombol aksi salin/ubah/hapus melalui {@code Common.copyEditDeleteButtons}.
	 *
	 * <b>Parameter:</b><br>
	 * - {@code arg0}: baris {@code Row} ZK yang akan diisi komponen<br>
	 * - {@code arg1}: objek data {@code JenisPajakBarang} yang akan dirender
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception} umum. Jika akun (hutang
	 * pajak atau dana titipan) bernilai null, renderer menggunakan string kosong
	 * sebagai pengganti agar tidak terjadi NullPointerException.
	 *
	 * <b>Pemeliharaan:</b> Jika ada kolom baru pada grid ZUL, tambahkan komponen
	 * yang sesuai di akhir metode render ini dengan urutan yang sama dengan urutan
	 * kolom di file ZUL. Perhatikan bahwa checkbox aktif memiliki event listener
	 * yang langsung menyimpan perubahan ke basis data tanpa konfirmasi.
	 */
	class JenisPajakBarangRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data {@code JenisPajakBarang} ke dalam
		 * komponen {@code Row} ZK dengan semua kolom yang diperlukan pada grid utama.
		 *
		 * <b>Cara kerja:</b> Metode ini dipanggil oleh framework ZK setiap kali sebuah
		 * baris perlu dirender pada grid. Objek data di-cast dari {@code Object} ke
		 * {@code JenisPajakBarang}, kemudian komponen-komponen ZK dibuat secara
		 * programatik dan ditambahkan ke baris menggunakan {@code setParent(arg0)}.
		 * Checkbox aktif memiliki listener {@code onCheck} yang langsung memperbarui
		 * field aktif pada entitas dan menyimpannya ke basis data via
		 * {@code Common.refreshSaveOrUpdate}. Tombol aksi memanfaatkan metode
		 * utilitas {@code Common.copyEditDeleteButtons} yang secara otomatis mengatur
		 * visibilitas tombol berdasarkan flag {@code edit} dan {@code delete}.
		 *
		 * <b>Parameter:</b><br>
		 * - {@code arg0}: baris {@code Row} ZK target<br>
		 * - {@code arg1}: objek {@code JenisPajakBarang} yang akan ditampilkan
		 *
		 * <b>Return:</b> Tidak ada nilai kembalian (void).
		 *
		 * <b>Penanganan error:</b> Melempar {@code Exception} jika terjadi kesalahan
		 * pembuatan komponen ZK. Null guard diterapkan untuk akun hutang dan titipan.
		 *
		 * <b>Pemeliharaan:</b> Urutan pemanggilan {@code setParent} harus sesuai dengan
		 * urutan definisi {@code <column>} pada file ZUL terkait.
		 *
		 * @param arg0 baris Row ZK yang akan diisi komponen-komponen sel
		 * @param arg1 objek data JenisPajakBarang yang akan dirender pada baris
		 * @throws Exception jika terjadi kesalahan saat pembuatan atau pemasangan komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisPajakBarang jenisPajakBarang = (JenisPajakBarang) arg1;
			new Label(jenisPajakBarang.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(JenisPajakBarang.class, jenisPajakBarang, jenisPajakBarang.getNama())
					.setParent(arg0);
			new Label(jenisPajakBarang.getAkun() == null ? ""
					: (jenisPajakBarang.getAkun().getKode() + " " + jenisPajakBarang.getAkun().getNama()))
					.setParent(arg0);
			new Label(jenisPajakBarang.getAkunDanaTitipan() == null ? ""
					: (jenisPajakBarang.getAkunDanaTitipan().getKode() + " "
							+ jenisPajakBarang.getAkunDanaTitipan().getNama()))
					.setParent(arg0);
			new Label(Common.numberFormat.get().format(jenisPajakBarang.getPersen())).setParent(arg0);
			new Label(jenisPajakBarang.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisPajakBarang.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPajakBarang.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisPajakBarang);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisPajakBarang, JenisPajakBarangAction.this).setParent(arg0);

		}

	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" pada toolbar halaman
	 * untuk membuka formulir penambahan data jenis pajak barang/jasa baru.
	 *
	 * <b>Cara kerja:</b> Metode ini dipanggil oleh framework ZK saat tombol tambah
	 * diklik. Metode membuat instance baru dari {@code JenisPajakBarang} yang kosong
	 * tanpa ID (menandakan data baru), kemudian meneruskan ke metode {@code init}
	 * yang berparameter entitas untuk membangun dan menampilkan formulir modal.
	 * Setelah {@code init} dipanggil, jendela modal ditampilkan secara programatik.
	 *
	 * <b>Parameter:</b> {@code event} adalah event ZK yang dipicu saat tombol tambah
	 * diklik, tidak digunakan secara langsung dalam metode ini.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception} umum. Jika terjadi
	 * kesalahan saat membangun komponen formulir, framework ZK akan menangkap
	 * dan menampilkan pesan error yang sesuai.
	 *
	 * <b>Pemeliharaan:</b> Nama metode ini harus cocok dengan atribut {@code onAdd}
	 * pada komponen tombol di file ZUL, atau dikonfigurasi sebagai event listener
	 * secara programatik di {@code doAfterCompose}.
	 *
	 * @param event event ZK yang dipicu saat tombol tambah diklik
	 * @throws Exception jika terjadi kesalahan saat inisialisasi formulir tambah
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisPajakBarang());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan antarmuka {@code DataInitDefault} untuk
	 * menginisialisasi formulir modal dengan data entitas yang diberikan, digunakan
	 * untuk operasi salin atau ubah data dari baris grid.
	 *
	 * <b>Cara kerja:</b> Metode ini merupakan implementasi dari kontrak antarmuka
	 * {@code DataInitDefault.init(GeneralValueObject)}. Metode melakukan type-cast
	 * pada parameter {@code obj} dari {@code GeneralValueObject} ke
	 * {@code JenisPajakBarang}, menyimpannya ke field instance {@code jenisPajakBarang},
	 * kemudian mendelegasikan ke metode private {@code init(JenisPajakBarang)} untuk
	 * membangun antarmuka formulir. Setelah itu, jendela modal ditampilkan dan
	 * diaktifkan sebagai modal. Metode ini dipanggil oleh
	 * {@code Common.copyEditDeleteButtons} ketika tombol salin atau ubah pada baris
	 * grid diklik.
	 *
	 * <b>Parameter:</b> {@code obj} adalah objek {@code GeneralValueObject} yang
	 * merupakan entitas {@code JenisPajakBarang} yang dipilih dari grid.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception} umum. Cast yang gagal akan
	 * menghasilkan {@code ClassCastException} yang ditangkap oleh framework ZK.
	 *
	 * <b>Pemeliharaan:</b> Metode ini adalah jembatan antara infrastruktur generik
	 * {@code DataInitDefault} dan logika spesifik kelas ini. Jangan ubah signature
	 * metode ini karena merupakan kontrak antarmuka.
	 *
	 * @param obj objek GeneralValueObject yang akan di-cast ke JenisPajakBarang
	 * @throws Exception jika terjadi kesalahan saat inisialisasi atau cast tipe data
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisPajakBarang = (JenisPajakBarang) obj;
		init(jenisPajakBarang);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan menginisialisasi seluruh komponen formulir modal
	 * untuk menambah atau mengubah data jenis pajak barang/jasa, dengan mengisi
	 * semua field formulir sesuai data entitas yang diberikan.
	 *
	 * <b>Cara kerja:</b> Metode private ini adalah inti dari pembuatan formulir.
	 * Pertama, field instance {@code this.jenisPajakBarang} ditetapkan ke entitas
	 * yang diberikan. Kemudian, judul jendela disesuaikan: "Tambah" jika ID null,
	 * "Ubah" jika ID ada. Isi jendela dibersihkan dengan {@code Common.clear}, lalu
	 * dibangun tata letak menggunakan {@code MyBorderlayout} dengan area tengah
	 * berisi {@code MyGrid} dua kolom (30%-sisanya) dan area selatan berisi toolbar
	 * tombol aksi. Baris-baris formulir dibuat menggunakan {@code MyFormRow} untuk:
	 * kode (opsional), nama (wajib), akun hutang pajak via {@code AmbilDataAkunBanbox}
	 * (wajib), akun dana titipan via {@code AmbilDataAkunBanbox} (opsional), persen
	 * via {@code MyDoublebox}, dan keterangan. Nilai awal setiap field diambil dari
	 * properti entitas. Tombol Batal menutup jendela, tombol Simpan memanggil
	 * {@code onSave} dan menutup jendela jika berhasil.
	 *
	 * <b>Parameter:</b> {@code jenisPajakBarang} adalah entitas yang datanya akan
	 * ditampilkan pada formulir. Jika ID null, berarti data baru.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception} umum. Komponen
	 * {@code AmbilDataAkunBanbox} menyimpan referensi objek {@code Akun} terpilih
	 * sebagai atribut bernama "akun" pada komponen tersebut.
	 *
	 * <b>Pemeliharaan:</b> Untuk menambah field baru, buat {@code MyFormRow} baru
	 * dan tambahkan di antara baris yang sudah ada. Pastikan field tersebut juga
	 * ditangani di {@code onSave}. Perhatikan bahwa variabel lokal {@code grid}
	 * di metode ini berbeda dengan field instance {@code grid} di kelas (grid utama).
	 *
	 * @param jenisPajakBarang entitas JenisPajakBarang yang akan ditampilkan di formulir
	 * @throws Exception jika terjadi kesalahan saat membangun komponen ZK
	 */
	private void init(JenisPajakBarang jenisPajakBarang) throws Exception {
		this.jenisPajakBarang = jenisPajakBarang;
		addWindow.setTitle(jenisPajakBarang.getId() == null ? "Tambah Pajak Barang/Jasa" : "Ubah Pajak Barang/Jasa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pajak Barang/Jasa"));
		row.appendChild(kode = new Textbox(jenisPajakBarang.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Pajak Barang/Jasa *"));
		row.appendChild(nama = new Textbox(jenisPajakBarang.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Hutang Pajak *"));
		row.appendChild(akun = new AmbilDataAkunBanbox(false));
		akun.setValue(jenisPajakBarang.getAkun() == null ? "" : jenisPajakBarang.getAkun().getNama());
		akun.setAttribute("akun", jenisPajakBarang.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Dana Titipan Pajak"));
		row.appendChild(akunDanaTitipan = new AmbilDataAkunBanbox(false));
		akunDanaTitipan.setValue(
				jenisPajakBarang.getAkunDanaTitipan() == null ? "" : jenisPajakBarang.getAkunDanaTitipan().getNama());
		akunDanaTitipan.setAttribute("akun", jenisPajakBarang.getAkunDanaTitipan());
		akunDanaTitipan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Persen pajak *"));
		row.appendChild(persen = new MyDoublebox(jenisPajakBarang.getPersen()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisPajakBarang.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
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
		borderlayout.setParent(addWindow);

	}

	/**
	 * <b>Tujuan:</b> Memvalidasi input formulir dan menyimpan data jenis pajak
	 * barang/jasa (baru atau yang sudah ada) ke basis data melalui sesi Hibernate.
	 *
	 * <b>Cara kerja:</b> Metode ini pertama-tama memvalidasi dua field wajib:
	 * (1) nama pajak tidak boleh kosong, (2) akun hutang pajak harus dipilih
	 * (atribut "akun" pada komponen {@code AmbilDataAkunBanbox} tidak boleh null).
	 * Jika salah satu validasi gagal, dialog peringatan ditampilkan dan metode
	 * mengembalikan {@code false} untuk menghentikan proses penyimpanan. Jika semua
	 * validasi lolos, sesi Hibernate saat ini diambil. Untuk data yang sudah ada
	 * (ID tidak null), entitas dimuat ulang dari basis data menggunakan
	 * {@code session.load} untuk memastikan sesi Hibernate mengelola entitas tersebut
	 * dengan benar. Kemudian semua field diatur pada entitas sebelum dipersistensikan
	 * menggunakan {@code Common.refreshSaveOrUpdate}.
	 *
	 * <b>Parameter:</b> {@code event} adalah event ZK dari klik tombol simpan,
	 * tidak digunakan secara langsung dalam metode ini.
	 *
	 * <b>Return:</b> {@code true} jika data berhasil disimpan; {@code false} jika
	 * validasi gagal dan penyimpanan dibatalkan.
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception} umum. Jika terjadi
	 * pelanggaran constraint basis data (misalnya duplikasi kode yang unik),
	 * Hibernate akan melempar exception yang akan ditangkap oleh framework ZK.
	 * Nilai persentase dari {@code MyDoublebox} dapat bernilai null jika input
	 * kosong; pastikan entitas menangani nilai null dengan benar.
	 *
	 * <b>Pemeliharaan:</b> Jika ada field wajib baru, tambahkan blok validasi
	 * sebelum logika penyimpanan. Penggunaan {@code session.load} (bukan
	 * {@code session.get}) berarti jika entitas tidak ditemukan di basis data,
	 * akan dilempar {@code ObjectNotFoundException} saat akses properti.
	 *
	 * @param event event ZK dari klik tombol simpan
	 * @return true jika penyimpanan berhasil, false jika validasi gagal
	 * @throws Exception jika terjadi kesalahan Hibernate atau ZK saat penyimpanan
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Pajak Barang/Jasa belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama jenis pajak; (2) Nama pajak digunakan untuk pengidentifikasian pajak dalam transaksi pengadaan; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (akun.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Hutang Pajak Barang/Jasa belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Akun Hutang Pajak; (2) Pilih akun jurnal hutang pajak yang sesuai; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisPajakBarang.getId() != null) {
			jenisPajakBarang = (JenisPajakBarang) session.load(JenisPajakBarang.class, jenisPajakBarang.getId());

		}

		jenisPajakBarang.setKode(kode.getValue());
		jenisPajakBarang.setNama(nama.getValue());
		jenisPajakBarang.setAkun((Akun) akun.getAttribute("akun"));
		jenisPajakBarang.setAkunDanaTitipan((Akun) akunDanaTitipan.getAttribute("akun"));
		jenisPajakBarang.setPersen(persen.getValue());
		jenisPajakBarang.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisPajakBarang);

		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun objek {@code Criteria} Hibernate untuk digunakan
	 * dalam kueri pencarian data jenis pajak barang/jasa berdasarkan filter yang
	 * aktif pada antarmuka pencarian.
	 *
	 * <b>Cara kerja:</b> Metode ini mengambil sesi Hibernate saat ini dan membuat
	 * {@code Criteria} untuk kelas {@code JenisPajakBarang}. Dua filter diterapkan:
	 * (1) Filter status aktif: jika checkbox {@code searchaktif} dicentang atau null,
	 * hanya data dengan {@code aktif = true} atau {@code aktif = null} yang
	 * disertakan; jika checkbox tidak dicentang, semua data (termasuk yang tidak
	 * aktif) ditampilkan menggunakan {@code sqlRestriction("true")}. (2) Filter
	 * nama: jika kotak teks {@code searchnama} tidak kosong, diterapkan pencarian
	 * case-insensitive menggunakan {@code MatchMode.ANYWHERE} pada kolom nama.
	 * Jika parameter {@code order} bernilai {@code true}, hasil diurutkan berdasarkan
	 * kolom nama secara ascending.
	 *
	 * <b>Parameter:</b> {@code order} menentukan apakah kriteria harus menyertakan
	 * klausa ORDER BY berdasarkan nama secara ascending.
	 *
	 * <b>Return:</b> Objek {@code Criteria} Hibernate yang sudah dikonfigurasi
	 * dengan semua filter dan urutan yang diperlukan, siap untuk dieksekusi.
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error khusus. Jika sesi
	 * Hibernate tidak tersedia, {@code HibernateUtil.currentSession()} akan melempar
	 * exception. Metode ini dipanggil dua kali dalam {@code onSearchDefault}: sekali
	 * untuk menghitung jumlah halaman (tanpa order) dan sekali untuk mengambil data
	 * (dengan order).
	 *
	 * <b>Pemeliharaan:</b> Untuk menambah filter baru (misalnya filter berdasarkan
	 * persentase), tambahkan klausa {@code criteria.add} baru di akhir metode ini.
	 * Pastikan nama properti yang digunakan dalam {@code Restrictions} sesuai
	 * dengan nama field pada kelas entitas {@code JenisPajakBarang}.
	 *
	 * @param order jika true, hasil akan diurutkan berdasarkan nama secara ascending
	 * @return objek Criteria Hibernate yang sudah dikonfigurasi dengan semua filter aktif
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPajakBarang.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Melakukan pencarian data jenis pajak barang/jasa berdasarkan
	 * filter yang aktif dan memperbarui tampilan grid utama beserta informasi
	 * paginasi sesuai halaman yang sedang aktif.
	 *
	 * <b>Cara kerja:</b> Metode ini pertama memanggil {@code Common.initPaging}
	 * dengan kriteria tanpa urutan untuk menghitung dan memperbarui jumlah total
	 * halaman pada komponen paginasi. Kemudian, metode membangun kriteria dengan
	 * urutan dan menerapkan batasan {@code setMaxResults(Common.ROWS_COUNT_ON_PAGE)}
	 * serta {@code setFirstResult} berdasarkan halaman aktif paginasi untuk
	 * mengambil hanya data yang relevan untuk halaman saat ini. Hasil query
	 * dibungkus dalam {@code SimpleListModel} dan ditetapkan ke grid bersama
	 * renderer baru {@code JenisPajakBarangRenderer}. Metode ini dipanggil saat
	 * inisialisasi halaman, saat paginasi berubah, dan setelah operasi simpan data.
	 *
	 * <b>Parameter:</b> {@code event} adalah event ZK yang memicu pencarian,
	 * dapat berupa null (pada pemanggilan programatik) atau event dari paginasi.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit. Anotasi
	 * {@code @SuppressWarnings("unchecked")} digunakan untuk menekan peringatan
	 * type-safety pada konversi {@code List} ke {@code ListModel}.
	 *
	 * <b>Pemeliharaan:</b> Metode ini dipanggil setiap kali grid perlu diperbarui.
	 * Untuk mengubah jumlah baris per halaman, modifikasi konstanta
	 * {@code Common.ROWS_COUNT_ON_PAGE}. Pemanggilan {@code initCriteria(false)}
	 * untuk paginasi dan {@code initCriteria(true)} untuk data harus tetap
	 * konsisten untuk memastikan jumlah total hasil sesuai dengan data yang
	 * ditampilkan.
	 *
	 * @param event event ZK yang memicu pencarian, dapat null jika dipanggil secara programatik
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPajakBarang> jenisPajakBarang = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisPajakBarang);
		grid.setRowRenderer(new JenisPajakBarangRenderer());
		grid.setModelCheckMobile(strset);

	}

}
