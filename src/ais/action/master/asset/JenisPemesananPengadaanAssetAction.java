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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.asset.JenisPemesananPengadaanAsset;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>JenisPemesananPengadaanAssetAction — Kontroler CRUD Data Master Jenis Pemesanan Pengadaan Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZKoss (composer) yang mengelola seluruh operasi CRUD
 * (Create, Read, Update, Delete) untuk entitas {@link JenisPemesananPengadaanAsset}. Data master
 * jenis pemesanan pengadaan berfungsi sebagai referensi tipe transaksi pemesanan barang/jasa dalam
 * modul manajemen aset. Setiap jenis pemesanan memiliki konfigurasi akun akuntansi yang terkait,
 * yaitu akun uang muka (DP), akun hutang penyedia, dan akun hutang pekerjaan. Selain itu, jenis
 * pemesanan juga mengatur apakah utang diambil langsung dari anggaran dan apakah terdapat proses
 * penerimaan barang/jasa. Halaman ini umumnya diakses melalui menu konfigurasi modul pengadaan
 * aset oleh pengguna dengan peran administrator atau manajer pengadaan.
 *
 * <b>Cara kerja:</b><br>
 * Controller ini mengimplementasikan tiga antarmuka: {@code DataCriteria} untuk membangun
 * query pencarian Hibernate, {@code DataSearchDefault} untuk menampilkan hasil pencarian ke grid,
 * dan {@code DataInitDefault} untuk menginisialisasi form tambah/ubah dari luar kelas.
 * Saat halaman ZUL dimuat, ZKoss memanggil {@code doBeforeCompose} (cek keamanan) diikuti
 * {@code doAfterCompose} yang mengatur hak akses, merender grid data awal, memasang event paging,
 * serta menambahkan tombol cetak dan unggah ke toolbar. Form tambah/ubah dirender secara
 * programatik di dalam {@code MyWindow} modal menggunakan {@code Borderlayout} dengan area tengah
 * berisi grid isian dan area selatan berisi toolbar aksi (Batal/Simpan). Data disimpan melalui
 * {@code Common.refreshSaveOrUpdate} yang menangani transaksi Hibernate secara otomatis. Setiap
 * baris grid dilengkapi checkbox aktif/nonaktif yang dapat diubah langsung tanpa membuka form.
 *
 * <b>Threading:</b><br>
 * Semua operasi dijalankan pada thread event ZKoss (ZK event thread). Tidak ada mekanisme
 * multi-threading atau akses konkuren secara eksplisit di kelas ini. Session Hibernate yang
 * digunakan adalah {@code HibernateUtil.currentSession()} yang terikat pada thread/sesi pengguna
 * aktif. Oleh karena itu, kelas ini tidak thread-safe jika diakses dari thread lain.
 *
 * <b>Pemeliharaan:</b><br>
 * Saat menambah field baru pada entitas {@link JenisPemesananPengadaanAsset}, perlu dilakukan
 * perubahan pada: (1) array {@code contents} di {@code doAfterCompose} untuk cetak/upload,
 * (2) metode {@code init(JenisPemesananPengadaanAsset)} untuk menambah baris form, dan
 * (3) metode {@code onSave} untuk menyimpan nilai field baru. Inner class renderer
 * {@code JenisPemesananPengadaanAssetRenderer} juga harus diperbarui agar kolom baru tampil
 * di grid. Pastikan DDL database sudah diperbarui sebelum deploy.
 *
 * @author Tim Pengembang AIS
 * @version 1.0
 * @see JenisPemesananPengadaanAsset
 * @see DataCriteria
 * @see DataSearchDefault
 * @see DataInitDefault
 */
public class JenisPemesananPengadaanAssetAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Nomor versi serial untuk serialisasi objek kelas ini.
	 * Nilai ini tidak perlu diubah kecuali terjadi perubahan struktur data yang tidak kompatibel
	 * dengan versi sebelumnya.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZKoss yang digunakan untuk menampilkan form tambah atau ubah data jenis pemesanan. */
	private MyWindow addWindow;

	/** Komponen paging ZKoss untuk navigasi halaman pada grid daftar jenis pemesanan pengadaan. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar seluruh data jenis pemesanan pengadaan yang tersedia. */
	private MyGrid grid;

	/** Kotak teks pencarian berdasarkan nama jenis pemesanan pada area filter di atas grid. */
	private Textbox searchnama;

	/** Checkbox filter untuk menampilkan hanya data yang aktif (dicentang) atau semua data. */
	private Checkbox searchaktif;

	/** Kotak teks input nama jenis pemesanan pada form tambah/ubah data. */
	private Textbox nama;

	/** Kotak teks input keterangan atau deskripsi jenis pemesanan pada form tambah/ubah data. */
	private Textbox keterangan;

	/** Penanda apakah pengguna saat ini memiliki hak akses untuk mengubah data (UPDATE privilege). */
	private boolean edit = false;

	/** Penanda apakah pengguna saat ini memiliki hak akses untuk menghapus data (DELETE privilege). */
	private boolean delete = false;

	/** Referensi ke objek entitas jenis pemesanan pengadaan yang sedang diproses (tambah atau ubah). */
	private JenisPemesananPengadaanAsset jenisPemesananPengadaanAsset;

	/** Tombol "Tambah" pada toolbar halaman, visibilitasnya dikendalikan oleh hak akses CREATE. */
	private MyToolbarbuttonConfig add;

	/** Kotak teks input kode unik jenis pemesanan pada form tambah/ubah data. */
	private Textbox kode;

	/** Komponen banbox (autocomplete input) untuk memilih akun uang muka (DP) dari daftar akun. */
	private AmbilDataAkunBanbox akunDp;

	/** Komponen banbox untuk memilih akun hutang kepada penyedia barang/jasa. */
	private AmbilDataAkunBanbox akunUtangDp;

	/** Checkbox konfigurasi apakah akun utang diambil langsung dari pos anggaran yang tersedia. */
	private MyCheckboxConfig akunUtangDariAnggaran;

	/** Checkbox konfigurasi apakah jenis pemesanan ini memerlukan proses penerimaan barang/jasa. */
	private MyCheckboxConfig adaProsesPenerimaan;

	/** Komponen banbox untuk memilih akun hutang atas pekerjaan atau jasa yang telah dilakukan. */
	private AmbilDataAkunBanbox akunUtangPekerjaan;

	/**
	 * Metode siklus hidup ZKoss yang dipanggil sebelum komponen halaman dibangun.
	 *
	 * <b>Tujuan:</b><br>
	 * Melakukan pemeriksaan keamanan akses pengguna sebelum halaman ZUL dirender sepenuhnya.
	 * Jika pengguna tidak memiliki sesi yang valid atau hak akses yang diperlukan, permintaan
	 * akan ditolak sejak dini tanpa membangun komponen antarmuka yang tidak perlu.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi bahwa pengguna sudah
	 * login dan memiliki hak akses ke halaman ini. Setelah itu mendelegasikan ke implementasi
	 * induk melalui {@code super.doBeforeCompose()} agar proses autowiring ZKoss berjalan normal.
	 * Metode ini dipanggil secara otomatis oleh framework ZKoss sebelum composing dimulai.
	 *
	 * <b>Parameter:</b><br>
	 * @param page halaman ZKoss saat ini yang sedang dikompilasi oleh framework
	 * @param parent komponen induk dalam hierarki komponen ZUL
	 * @param compInfo informasi metadata komponen yang sedang diproses oleh ZKoss
	 *
	 * <b>Return:</b><br>
	 * @return objek {@code ComponentInfo} dari kelas induk yang berisi informasi komponen
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika pemeriksaan keamanan gagal, {@code Common.doCheckSecurity()} akan melempar exception
	 * atau melakukan redirect ke halaman login sesuai konfigurasi sistem.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jangan hapus pemanggilan {@code super.doBeforeCompose()} karena diperlukan untuk proses
	 * autowiring komponen ZKoss agar berjalan dengan benar.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Metode siklus hidup ZKoss yang dipanggil setelah seluruh komponen halaman selesai dibangun.
	 *
	 * <b>Tujuan:</b><br>
	 * Menginisialisasi seluruh komponen antarmuka pengguna dan mengatur logika bisnis awal
	 * halaman. Ini adalah titik masuk utama controller setelah ZKoss menyelesaikan proses
	 * composing dan autowiring semua komponen yang didefinisikan di file ZUL.
	 *
	 * <b>Cara kerja:</b><br>
	 * Metode ini melakukan serangkaian inisialisasi secara berurutan:
	 * (1) Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan proses autowiring ZKoss.
	 * (2) Memanggil {@code Common.initLaguage()} untuk mengatur preferensi bahasa antarmuka.
	 * (3) Mengatur visibilitas tombol Tambah berdasarkan hak akses CREATE pengguna saat ini.
	 * (4) Menetapkan nilai boolean {@code edit} dan {@code delete} berdasarkan hak akses UPDATE
	 *     dan DELETE pengguna untuk digunakan oleh renderer grid.
	 * (5) Memanggil {@code onSearchDefault(null)} untuk memuat data awal ke grid.
	 * (6) Mendaftarkan event listener paging sehingga navigasi halaman memuat ulang data grid.
	 * (7) Menambahkan tombol cetak data (ekspor) ke toolbar menggunakan {@code Common.cetakData}.
	 * (8) Menambahkan tombol unggah data (impor) ke toolbar; tombol ini hanya terlihat jika
	 *     pengguna memiliki ketiga hak akses CREATE, UPDATE, dan DELETE sekaligus.
	 *
	 * <b>Parameter:</b><br>
	 * @param comp komponen akar halaman ZUL yang sudah selesai dibangun oleh framework ZKoss
	 *
	 * <b>Penanganan error:</b><br>
	 * Metode ini mendeklarasikan {@code throws Exception} karena {@code super.doAfterCompose()}
	 * dan berbagai inisialisasi komponen dapat melempar exception yang perlu ditangani oleh
	 * framework ZKoss di level yang lebih tinggi.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika ada field baru yang perlu dimasukkan ke ekspor/impor, tambahkan nama field tersebut
	 * ke dalam array {@code contents}. Urutan field dalam array menentukan urutan kolom pada
	 * file hasil ekspor Excel.
	 *
	 * @throws Exception jika terjadi kesalahan selama inisialisasi komponen atau pemuatan data
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

		String[] contents = new String[] { "id", "kode", "nama", "akunDp", "akunUtangDp", "akunUtangPekerjaan",
				"akunUtangDariAnggaran", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisPemesananPengadaanAsset.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisPemesananPengadaanAsset.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Inner class renderer yang bertanggung jawab merender setiap baris data pada grid
	 * daftar jenis pemesanan pengadaan aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengubah objek {@link JenisPemesananPengadaanAsset} menjadi komponen-komponen ZKoss
	 * yang dapat ditampilkan sebagai satu baris ({@link Row}) dalam grid antarmuka pengguna.
	 * Setiap baris menampilkan semua atribut penting entitas beserta kontrol interaktif.
	 *
	 * <b>Cara kerja:</b><br>
	 * Untuk setiap objek data yang ada dalam model grid, metode {@code render} dipanggil
	 * oleh ZKoss. Metode ini membuat komponen {@link Label} untuk menampilkan kode, nama
	 * (melalui RevisiHelper agar dapat dilihat riwayat revisinya), akun DP, akun hutang DP,
	 * akun hutang pekerjaan, flag utang dari anggaran (Ya/Tidak), flag ada proses penerimaan
	 * (Ya/Tidak), dan keterangan. Selain itu, ditambahkan checkbox "Aktif" yang dapat langsung
	 * diubah tanpa membuka form edit — perubahan langsung disimpan via
	 * {@code Common.refreshSaveOrUpdate}. Di bagian akhir baris ditambahkan tombol aksi
	 * salin, ubah, dan hapus melalui {@code Common.copyEditDeleteButtons}.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Renderer ini adalah inner class non-static sehingga dapat mengakses field dan metode
	 * outer class seperti {@code edit}, {@code delete}, dan {@code addWindow} secara langsung.
	 * Jika ada kolom baru yang ditambahkan ke entitas, tambahkan komponen Label atau input
	 * yang sesuai di dalam metode {@code render} ini sesuai urutan kolom header di ZUL.
	 */
	class JenisPemesananPengadaanAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data jenis pemesanan pengadaan ke dalam komponen ZKoss Row.
		 *
		 * <b>Tujuan:</b><br>
		 * Mengisi komponen {@link Row} ZKoss dengan semua data dari satu entitas
		 * {@link JenisPemesananPengadaanAsset}, termasuk label informatif dan kontrol
		 * interaktif seperti checkbox aktif dan tombol aksi.
		 *
		 * <b>Cara kerja:</b><br>
		 * Pertama, mengatur vertical alignment baris ke atas (top). Kemudian melakukan
		 * cast objek {@code arg1} ke {@link JenisPemesananPengadaanAsset} untuk mengakses
		 * semua atributnya. Label kode ditambahkan sebagai sel pertama, diikuti RevisiHelper
		 * untuk nama (memungkinkan tampilan riwayat revisi entitas). Label akun DP, hutang DP,
		 * dan hutang pekerjaan ditampilkan dalam format "kode nama" jika akun tidak null, atau
		 * string kosong jika null. Flag boolean ditampilkan sebagai teks "Ya" atau "Tidak".
		 * Checkbox Aktif dibuat dengan listener inline yang langsung menyimpan perubahan status
		 * ke database saat pengguna mengklik checkbox tanpa perlu menekan tombol simpan.
		 * Tombol salin, ubah, dan hapus ditambahkan di sel terakhir; visibilitasnya dikontrol
		 * oleh flag {@code edit} dan {@code delete} dari outer class.
		 *
		 * <b>Parameter:</b><br>
		 * @param arg0 objek {@link Row} ZKoss yang akan diisi dengan komponen-komponen data
		 * @param arg1 objek data {@link JenisPemesananPengadaanAsset} yang akan dirender
		 *
		 * <b>Penanganan error:</b><br>
		 * @throws Exception jika terjadi kesalahan saat membuat atau memasang komponen ZKoss
		 *
		 * <b>Pemeliharaan:</b><br>
		 * Urutan penambahan komponen ke {@code arg0} harus konsisten dengan urutan definisi
		 * kolom ({@code <column>}) pada file ZUL agar data tampil di kolom yang benar.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisPemesananPengadaanAsset jenisPemesananPengadaanAsset = (JenisPemesananPengadaanAsset) arg1;
			new Label(jenisPemesananPengadaanAsset.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(JenisPemesananPengadaanAsset.class, jenisPemesananPengadaanAsset,
					jenisPemesananPengadaanAsset.getNama()).setParent(arg0);
			new Label(jenisPemesananPengadaanAsset.getAkunDp() == null ? ""
					: (jenisPemesananPengadaanAsset.getAkunDp().getKode() + " "
							+ jenisPemesananPengadaanAsset.getAkunDp().getNama()))
					.setParent(arg0);

			new Label(jenisPemesananPengadaanAsset.getAkunUtangDp() == null ? ""
					: (jenisPemesananPengadaanAsset.getAkunUtangDp().getKode() + " "
							+ jenisPemesananPengadaanAsset.getAkunUtangDp().getNama()))
					.setParent(arg0);

			new Label(jenisPemesananPengadaanAsset.getAkunUtangPekerjaan() == null ? ""
					: (jenisPemesananPengadaanAsset.getAkunUtangPekerjaan().getKode() + " "
							+ jenisPemesananPengadaanAsset.getAkunUtangPekerjaan().getNama()))
					.setParent(arg0);

			new Label(jenisPemesananPengadaanAsset.getAkunUtangDariAnggaran() ? "Ya" : "Tidak").setParent(arg0);
			new Label(jenisPemesananPengadaanAsset.getAdaProsesPenerimaan() ? "Ya" : "Tidak").setParent(arg0);

			new Label(jenisPemesananPengadaanAsset.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisPemesananPengadaanAsset.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPemesananPengadaanAsset.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisPemesananPengadaanAsset);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisPemesananPengadaanAsset,
					JenisPemesananPengadaanAssetAction.this).setParent(arg0);

		}

	}

	/**
	 * Event handler yang dipanggil ketika pengguna menekan tombol "Tambah" pada toolbar halaman.
	 *
	 * <b>Tujuan:</b><br>
	 * Membuka form tambah data baru jenis pemesanan pengadaan dalam jendela modal.
	 * Metode ini merupakan titik masuk untuk alur kerja penambahan entitas baru.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat objek {@link JenisPemesananPengadaanAsset} baru (kosong, tanpa ID) kemudian
	 * meneruskannya ke metode {@code init(JenisPemesananPengadaanAsset)} yang akan membangun
	 * seluruh komponen form secara programatik di dalam {@code addWindow}. Setelah form siap,
	 * jendela modal ditampilkan dengan memanggil {@code addWindow.setVisible(true)} dan
	 * {@code addWindow.onModal()} agar pengguna tidak dapat berinteraksi dengan halaman
	 * di belakang jendela selama form masih terbuka.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZKoss yang berisi informasi kejadian klik tombol Tambah
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan saat membangun komponen form atau menampilkan modal
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Metode ini dipanggil secara otomatis oleh ZKoss berdasarkan konvensi penamaan
	 * {@code on + NamaEvent} yang dikonfigurasi di file ZUL melalui atribut {@code onClick}.
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisPemesananPengadaanAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Implementasi antarmuka {@link DataInitDefault} untuk menginisialisasi form dari luar kelas.
	 *
	 * <b>Tujuan:</b><br>
	 * Menyediakan titik masuk publik yang dapat dipanggil oleh sistem umum (seperti fitur
	 * salin data di {@code Common.copyEditDeleteButtons}) untuk membuka form edit atau tambah
	 * dengan objek data yang sudah ditentukan dari luar controller ini.
	 *
	 * <b>Cara kerja:</b><br>
	 * Melakukan cast objek {@link GeneralValueObject} yang diterima menjadi
	 * {@link JenisPemesananPengadaanAsset}, menyimpannya ke field instance, lalu meneruskan
	 * ke metode {@code init(JenisPemesananPengadaanAsset)} yang tipe-spesifik untuk membangun
	 * form. Setelah form siap, jendela modal ditampilkan sehingga pengguna dapat melihat dan
	 * mengubah data yang dipilih.
	 *
	 * <b>Parameter:</b><br>
	 * @param obj objek {@link GeneralValueObject} yang merupakan instance
	 *            {@link JenisPemesananPengadaanAsset} yang akan diedit atau disalin
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika cast gagal (tipe objek tidak kompatibel) atau jika terjadi
	 *                   kesalahan saat membangun komponen form modal
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Implementasi antarmuka ini diperlukan agar sistem generik {@code Common.copyEditDeleteButtons}
	 * dapat membuka form edit tanpa mengetahui tipe controller secara spesifik.
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisPemesananPengadaanAsset = (JenisPemesananPengadaanAsset) obj;
		init(jenisPemesananPengadaanAsset);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Metode privat yang membangun seluruh antarmuka form tambah/ubah data jenis pemesanan pengadaan.
	 *
	 * <b>Tujuan:</b><br>
	 * Membersihkan konten jendela modal yang ada dan membangun ulang semua komponen form
	 * secara programatik berdasarkan data entitas yang diberikan. Metode ini mendukung
	 * dua mode: tambah data baru (objek tanpa ID) dan ubah data yang sudah ada (objek dengan ID).
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menyimpan referensi objek ke field instance dan mengatur judul jendela sesuai mode.
	 * (2) Membersihkan semua komponen lama dari {@code addWindow} menggunakan {@code Common.clear}.
	 * (3) Membangun layout menggunakan {@code MyBorderlayout} dengan panel Center berisi grid isian
	 *     dan panel South berisi toolbar Batal/Simpan.
	 * (4) Membuat grid dua kolom (label 30% + input 70%) dengan baris-baris untuk: kode, nama,
	 *     akun DP, akun hutang DP, akun hutang pekerjaan, checkbox utang dari anggaran, checkbox
	 *     ada proses penerimaan, dan keterangan.
	 * (5) Setiap komponen input diisi dengan nilai saat ini dari objek entitas yang diberikan.
	 * (6) Tombol Batal menutup jendela; tombol Simpan memanggil {@code onSave} dan menutup
	 *     jendela jika berhasil, lalu merefresh grid.
	 *
	 * <b>Parameter:</b><br>
	 * @param jenisPemesananPengadaanAsset objek entitas yang berisi data yang akan ditampilkan
	 *        di form; jika {@code getId() == null} maka ini adalah form tambah baru, jika tidak
	 *        maka form ubah data yang sudah ada
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan saat membangun komponen ZKoss atau mengakses
	 *                   data dari objek entitas
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Tambahkan baris form baru di antara baris keterangan dan toolbar jika ada field baru.
	 * Pastikan field komponen input (sebagai field instance) juga didaftarkan di deklarasi
	 * field kelas agar dapat diakses oleh metode {@code onSave}.
	 */
	private void init(JenisPemesananPengadaanAsset jenisPemesananPengadaanAsset) throws Exception {
		this.jenisPemesananPengadaanAsset = jenisPemesananPengadaanAsset;
		addWindow.setTitle(jenisPemesananPengadaanAsset.getId() == null ? "Tambah Jenis Pemesanan Pengadaan" : "Ubah Jenis Pemesanan Pengadaan");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Jenis Pemesanan"));
		row.appendChild(kode = new Textbox(jenisPemesananPengadaanAsset.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Pemesanan *"));
		row.appendChild(nama = new Textbox(jenisPemesananPengadaanAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Uang Muka *"));
		row.appendChild(akunDp = new AmbilDataAkunBanbox(false));
		akunDp.setValue(jenisPemesananPengadaanAsset.getAkunDp() == null ? ""
				: jenisPemesananPengadaanAsset.getAkunDp().toString());
		akunDp.setAttribute("akun", jenisPemesananPengadaanAsset.getAkunDp());
		akunDp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Hutang Penyedia *"));
		row.appendChild(akunUtangDp = new AmbilDataAkunBanbox(false));
		akunUtangDp.setValue(jenisPemesananPengadaanAsset.getAkunUtangDp() == null ? ""
				: jenisPemesananPengadaanAsset.getAkunUtangDp().toString());
		akunUtangDp.setAttribute("akun", jenisPemesananPengadaanAsset.getAkunUtangDp());
		akunUtangDp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Hutang Pekerjaan *"));
		row.appendChild(akunUtangPekerjaan = new AmbilDataAkunBanbox(false));
		akunUtangPekerjaan.setValue(jenisPemesananPengadaanAsset.getAkunUtangPekerjaan() == null ? ""
				: jenisPemesananPengadaanAsset.getAkunUtangPekerjaan().toString());
		akunUtangPekerjaan.setAttribute("akun", jenisPemesananPengadaanAsset.getAkunUtangPekerjaan());
		akunUtangPekerjaan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(akunUtangDariAnggaran = new MyCheckboxConfig("Akun Utang Ambil Langsung Dari Anggaran"));
		akunUtangDariAnggaran.setChecked(jenisPemesananPengadaanAsset.getAkunUtangDariAnggaran());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(adaProsesPenerimaan = new MyCheckboxConfig("Terdapat Proses Penerimaan Barang/Jasa"));
		adaProsesPenerimaan.setChecked(jenisPemesananPengadaanAsset.getAdaProsesPenerimaan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisPemesananPengadaanAsset.getKeterangan()));
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
	 * Memvalidasi input form dan menyimpan data jenis pemesanan pengadaan ke database.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengambil nilai dari semua komponen input form, melakukan validasi kelengkapan data
	 * yang wajib diisi, dan menyimpan atau memperbarui entitas {@link JenisPemesananPengadaanAsset}
	 * ke database melalui Hibernate. Metode ini dipanggil oleh tombol Simpan di form modal.
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memeriksa apakah field nama tidak kosong; jika kosong menampilkan pesan peringatan
	 *     dan mengembalikan {@code false} untuk mencegah penutupan form.
	 * (2) Memeriksa apakah akun DP sudah dipilih dari banbox; jika belum menampilkan peringatan.
	 * (3) Memeriksa apakah akun hutang DP sudah dipilih; jika belum menampilkan peringatan.
	 * (4) Memeriksa apakah akun hutang pekerjaan sudah dipilih; jika belum menampilkan peringatan.
	 * (5) Jika validasi lolos, mendapatkan session Hibernate saat ini. Jika entitas sudah ada
	 *     di database (ID tidak null), memuat ulang dari session Hibernate menggunakan {@code load()}
	 *     agar berada dalam state managed dan perubahan dapat dideteksi oleh Hibernate.
	 * (6) Menetapkan semua nilai dari komponen input ke properti entitas.
	 * (7) Memanggil {@code Common.refreshSaveOrUpdate} yang menangani save atau update sesuai
	 *     keberadaan ID entitas dan melakukan flush serta commit transaksi.
	 *
	 * <b>Return:</b><br>
	 * @return {@code true} jika data berhasil disimpan; {@code false} jika validasi gagal
	 *         (form tetap terbuka agar pengguna dapat memperbaiki input)
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZKoss dari tombol Simpan yang memicu pemanggilan metode ini
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan akses database (misalnya constraint violation
	 *                   atau koneksi terputus) yang perlu ditangani oleh pemanggil
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Saat menambah field baru yang wajib diisi, tambahkan validasi di blok awal sebelum
	 * akses database. Urutan validasi sebaiknya mengikuti urutan tampilan field di form
	 * agar pengguna mendapat feedback yang intuitif.
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Jenis Pemesanan Pengadaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama jenis pemesanan yang sesuai; (2) Nama ini digunakan untuk mengidentifikasi jenis pemesanan dalam transaksi; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (akunDp.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Uang Muka belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Akun Uang Muka; (2) Pilih akun jurnal untuk uang muka dari daftar akun; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (akunUtangDp.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Hutang Penyedia belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Akun Hutang Penyedia; (2) Pilih akun jurnal hutang penyedia dari daftar akun; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (akunUtangPekerjaan.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Hutang Pekerjaan belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Akun Hutang Pekerjaan; (2) Pilih akun jurnal hutang pekerjaan dari daftar akun; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisPemesananPengadaanAsset.getId() != null) {
			jenisPemesananPengadaanAsset = (JenisPemesananPengadaanAsset) session
					.load(JenisPemesananPengadaanAsset.class, jenisPemesananPengadaanAsset.getId());

		}

		jenisPemesananPengadaanAsset.setKode(kode.getValue());
		jenisPemesananPengadaanAsset.setNama(nama.getValue());
		jenisPemesananPengadaanAsset.setAkunDp((Akun) akunDp.getAttribute("akun"));
		jenisPemesananPengadaanAsset.setAkunUtangDp((Akun) akunUtangDp.getAttribute("akun"));
		jenisPemesananPengadaanAsset.setAkunUtangPekerjaan((Akun) akunUtangPekerjaan.getAttribute("akun"));
		jenisPemesananPengadaanAsset.setAkunUtangDariAnggaran(akunUtangDariAnggaran.isChecked());
		jenisPemesananPengadaanAsset.setKeterangan(keterangan.getValue());
		jenisPemesananPengadaanAsset.setAdaProsesPenerimaan(adaProsesPenerimaan.isChecked());

		Common.refreshSaveOrUpdate(session, jenisPemesananPengadaanAsset);

		return true;
	}

	/**
	 * Implementasi antarmuka {@link DataCriteria} untuk membangun objek Hibernate Criteria
	 * yang digunakan sebagai dasar query pencarian data jenis pemesanan pengadaan.
	 *
	 * <b>Tujuan:</b><br>
	 * Membangun dan mengembalikan objek {@link Criteria} Hibernate yang mengkombinasikan
	 * semua kondisi filter pencarian yang aktif saat ini. Criteria ini digunakan oleh
	 * {@code onSearchDefault} untuk mengambil data dari database.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat Criteria untuk kelas {@link JenisPemesananPengadaanAsset} menggunakan session
	 * Hibernate saat ini. Filter aktif diterapkan: jika checkbox searchaktif dicentang (atau
	 * null/tidak ada), hanya data dengan {@code aktif = true} atau {@code aktif IS NULL} yang
	 * diambil; jika tidak dicentang, semua data diambil (sqlRestriction "true"). Filter nama
	 * diterapkan sebagai ILIKE (case-insensitive LIKE) dengan MatchMode.ANYWHERE jika kotak
	 * pencarian tidak kosong. Parameter {@code order} mengontrol apakah hasil diurutkan
	 * berdasarkan nama secara ascending — berguna untuk query hitungan (tanpa order) yang
	 * lebih efisien secara performa.
	 *
	 * <b>Parameter:</b><br>
	 * @param order {@code true} jika hasil perlu diurutkan berdasarkan nama secara ascending;
	 *              {@code false} untuk query tanpa pengurutan (biasanya digunakan untuk
	 *              menghitung total data untuk paging)
	 *
	 * <b>Return:</b><br>
	 * @return objek {@link Criteria} Hibernate yang siap dieksekusi dengan semua filter
	 *         pencarian yang telah diterapkan
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika ada filter pencarian baru yang ditambahkan ke form (misalnya filter berdasarkan
	 * kode atau akun), tambahkan kondisi Restrictions yang sesuai di sini. Pastikan field
	 * filter komponen juga sudah dideklarasikan sebagai field instance di kelas ini.
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPemesananPengadaanAsset.class)
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
	 * Implementasi antarmuka {@link DataSearchDefault} untuk memuat dan menampilkan data
	 * hasil pencarian ke grid utama halaman.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengeksekusi query pencarian berdasarkan kriteria filter aktif saat ini, menerapkan
	 * batasan paging, dan memperbarui model data grid sehingga pengguna melihat daftar
	 * data yang sesuai filter dan halaman yang dipilih.
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memanggil {@code Common.initPaging(initCriteria(false), paging)} untuk menghitung
	 *     total jumlah data yang sesuai filter dan memperbarui komponen paging ZKoss.
	 * (2) Mengeksekusi query dengan {@code initCriteria(true)} yang menyertakan pengurutan,
	 *     dibatasi {@code Common.ROWS_COUNT_ON_PAGE} baris, dimulai dari offset yang dihitung
	 *     dari halaman aktif paging saat ini.
	 * (3) Membungkus hasil query dalam {@code SimpleListModel} dan menetapkannya ke grid
	 *     bersama renderer {@code JenisPemesananPengadaanAssetRenderer} yang baru.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZKoss; dapat {@code null} jika dipanggil secara programatik
	 *              (misalnya setelah simpan data atau saat inisialisasi halaman)
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Metode ini dipanggil secara otomatis oleh ZKoss melalui konvensi event binding di ZUL
	 * (biasanya pada event onSearch atau onChange komponen filter) dan juga dipanggil secara
	 * eksplisit dari berbagai tempat dalam kelas ini setelah operasi data berhasil.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPemesananPengadaanAsset> jenisPemesananPengadaanAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisPemesananPengadaanAsset);
		grid.setRowRenderer(new JenisPemesananPengadaanAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

}
