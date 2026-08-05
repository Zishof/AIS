package ais.action.master.asset;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
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
import ais.database.model.asset.CaraPengadaanAsset;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>CaraPengadaanAssetAction &mdash; Controller CRUD Cara Pengadaan Aset</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah controller ZK (composer) untuk halaman manajemen data master
 * <em>Cara Pengadaan Aset</em>, yaitu daftar metode atau mekanisme pengadaan aset
 * yang digunakan oleh institusi, seperti pembelian langsung, tender, hibah, donasi,
 * dan sebagainya. Setiap cara pengadaan dihubungkan dengan dua akun akuntansi:
 * akun aset utama dan akun transaksi (opsional), sehingga setiap transaksi pengadaan
 * dapat secara otomatis dijurnal ke akun yang tepat sesuai metode pengadaannya.
 * Tanpa standarisasi cara pengadaan, laporan keuangan dan audit aset akan sulit
 * dilacak karena setiap unit mungkin mencatat transaksi ke akun yang berbeda.
 * Halaman ini menyediakan operasi tambah, ubah, dan hapus data cara pengadaan,
 * dilengkapi fitur ekspor Excel dan impor data massal.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Controller ini mewarisi {@code GenericAutowireComposer} dari ZK Framework dan
 * mengimplementasikan tiga interface: {@code DataCriteria} (membangun query kriteria),
 * {@code DataSearchDefault} (pencarian ulang grid), dan {@code DataInitDefault}
 * (inisialisasi form dari data eksternal seperti impor). Alur kerja utama:
 * <ol>
 *   <li>{@code doBeforeCompose} memverifikasi keamanan sesi sebelum halaman dirender.</li>
 *   <li>{@code doAfterCompose} menginisialisasi hak akses tombol, memanggil
 *       {@code onSearchDefault} untuk data awal, menyiapkan paging, serta mendaftarkan
 *       tombol cetak (ekspor Excel) dan tombol upload (impor data) ke toolbar.</li>
 *   <li>{@code onSearchDefault} memuat data ke grid dengan filter nama dan paging.</li>
 *   <li>{@code CaraPengadaanAssetRenderer} merender setiap baris grid dengan data
 *       cara pengadaan beserta kolom nama, akun, akun transaksi, keterangan, dan tombol aksi.</li>
 *   <li>Form tambah/ubah dibangun programatik oleh {@code init(CaraPengadaanAsset)} dengan
 *       input nama, akun (melalui {@code AmbilDataAkunBanbox}), akun transaksi (opsional),
 *       dan keterangan.</li>
 *   <li>{@code onSave} memvalidasi input dan menyimpan data ke database.</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh operasi berjalan pada thread ZK event (UI thread). Sesi Hibernate diambil dari
 * {@code HibernateUtil.currentSession()} yang terikat pada thread HTTP request aktif.
 * Tidak ada penggunaan thread latar belakang atau sinkronisasi tambahan yang diperlukan.
 * Komponen {@code AmbilDataAkunBanbox} melakukan pencarian akun secara sinkron pada
 * thread yang sama sehingga tidak ada risiko race condition.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Untuk menambah field baru ke cara pengadaan, tambahkan field komponen ZK di kelas ini,
 * tambahkan baris form di {@code init(CaraPengadaanAsset)}, dan tambahkan setter di
 * {@code onSave}. Pastikan array {@code contents} di {@code doAfterCompose} juga diperbarui
 * agar kolom baru tersedia pada ekspor/impor Excel. Jika ingin menambah validasi duplikasi
 * nama, tambahkan metode {@code checkNama...} serupa dengan pola di controller lain.
 * Selalu verifikasi visibilitas tombol cetak dan upload dengan hak akses yang berbeda.</p>
 *
 * @author Tim Pengembang AIS
 * @version 1.0
 * @see CaraPengadaanAsset
 * @see AmbilDataAkunBanbox
 * @see DataCriteria
 * @see DataSearchDefault
 * @see DataInitDefault
 */
public class CaraPengadaanAssetAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal popup untuk form tambah/ubah data cara pengadaan. */
	private MyWindow addWindow;

	/** Komponen paging ZK untuk navigasi halaman pada grid data. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar cara pengadaan aset. */
	private MyGrid grid;

	/** Kotak teks filter pencarian berdasarkan nama cara pengadaan. */
	private Textbox searchnama;

	/** Kotak teks input nama cara pengadaan pada form tambah/ubah. */
	private Textbox nama;

	/** Komponen pencarian dan pemilihan akun aset utama. */
	private AmbilDataAkunBanbox akun;

	/** Kotak teks input keterangan cara pengadaan. */
	private Textbox keterangan;

	/** Flag yang menandakan apakah pengguna saat ini memiliki hak ubah data. */
	private boolean edit = false;

	/** Flag yang menandakan apakah pengguna saat ini memiliki hak hapus data. */
	private boolean delete = false;

	/** Entitas cara pengadaan aset yang sedang aktif diedit atau ditambahkan. */
	private CaraPengadaanAsset caraPengadaanAsset;

	/** Tombol toolbar untuk menambah data cara pengadaan baru. */
	private MyToolbarbuttonConfig add;

	/** Komponen pencarian dan pemilihan akun transaksi (opsional, bisa kosong). */
	private AmbilDataAkunBanbox akunTransaksi;

	/**
	 * <h3>doBeforeCompose &mdash; Verifikasi Keamanan Sebelum Render Halaman</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Dipanggil oleh ZK tepat sebelum pembuatan komponen halaman dimulai, untuk
	 * memastikan bahwa hanya pengguna yang terautentikasi yang dapat mengakses
	 * halaman manajemen cara pengadaan aset ini.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi sesi aktif.
	 * Jika tidak valid, pengguna dialihkan ke halaman login. Kemudian memanggil
	 * {@code super.doBeforeCompose} untuk melanjutkan proses lifecycle ZK dan
	 * mengembalikan {@code ComponentInfo} yang diperlukan oleh framework.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika sesi tidak valid, {@code Common.doCheckSecurity()} melakukan redirect dan
	 * proses berhenti. Tidak ada exception yang dilempar secara eksplisit.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Selalu pertahankan pemanggilan {@code Common.doCheckSecurity()} sebagai
	 * lapisan keamanan pertama. Jangan menghapus pemanggilan {@code super.doBeforeCompose}.</p>
	 *
	 * @param page     halaman ZK yang sedang diproses
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo informasi metadata komponen
	 * @return {@code ComponentInfo} dari kelas induk untuk melanjutkan proses ZK
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <h3>doAfterCompose &mdash; Inisialisasi Halaman Setelah Komponen ZK Selesai Dibuat</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode lifecycle ZK yang dipanggil setelah seluruh komponen halaman selesai di-wire.
	 * Melakukan pengaturan lengkap halaman: inisialisasi bahasa, pengaturan hak akses
	 * tombol, pemuatan data grid awal, penyiapan paging, serta pendaftaran tombol
	 * cetak (ekspor Excel) dan upload (impor data massal) ke toolbar halaman.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} agar ZK menyelesaikan wire komponen.</li>
	 *   <li>Inisialisasi bahasa dengan {@code Common.initLaguage()}.</li>
	 *   <li>Mengatur visibilitas tombol Tambah berdasarkan hak CREATE pengguna.</li>
	 *   <li>Menetapkan flag {@code edit} dan {@code delete} sesuai hak UPDATE dan DELETE.</li>
	 *   <li>Memanggil {@code onSearchDefault(null)} untuk memuat data awal ke grid.</li>
	 *   <li>Mendaftarkan listener paging untuk reload grid saat halaman berubah.</li>
	 *   <li>Membuat tombol cetak Excel dengan kolom id, nama, akun, akunTransaksi, keterangan.</li>
	 *   <li>Membuat tombol upload impor data dan mengatur visibilitasnya.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari {@code super.doAfterCompose} dibiarkan menyebar. Tombol upload
	 * tidak ditampilkan jika {@code Common.uploadData} mengembalikan null.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika menambah kolom pada entitas, perbarui array {@code contents} dan pastikan
	 * kolom tersebut tersedia sebagai getter di entitas {@code CaraPengadaanAsset}.</p>
	 *
	 * @param comp komponen root ZUL yang telah selesai dirender
	 * @throws Exception jika terjadi kesalahan pada inisialisasi komponen induk
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

		String[] contents = new String[] { "id", "nama", "akun", "akunTransaksi", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, CaraPengadaanAsset.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <h3>CaraPengadaanAssetRenderer &mdash; Renderer Baris Grid Cara Pengadaan Aset</h3>
	 *
	 * <p><b>Untuk apa:</b><br>
	 * Inner class renderer yang mengisi setiap baris grid dengan data entitas
	 * {@code CaraPengadaanAsset}. Menampilkan informasi lengkap cara pengadaan mencakup
	 * nama, kode dan nama akun aset, kode dan nama akun transaksi (jika ada), keterangan,
	 * serta tombol-tombol aksi Salin/Ubah/Hapus yang disesuaikan dengan hak akses pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Pada setiap pemanggilan {@code render}, objek {@code arg1} di-cast ke
	 * {@code CaraPengadaanAsset}. Komponen ditambahkan ke baris dalam urutan:
	 * nama (dengan RevisiHelper), label akun (format "kode-nama"), label akun transaksi,
	 * label keterangan, dan akhirnya tombol aksi Salin/Ubah/Hapus melalui
	 * {@code Common.copyEditDeleteButtons}. Jika akun atau akun transaksi null,
	 * ditampilkan string kosong.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Urutan kolom harus sesuai dengan definisi kolom di file ZUL.
	 * Visibilitas tombol dikontrol oleh flag {@code edit} dan {@code delete}
	 * dari kelas induk melalui {@code Common.copyEditDeleteButtons}.</p>
	 */
	class CaraPengadaanAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render &mdash; Mengisi Satu Baris Grid dengan Data Cara Pengadaan Aset</h3>
		 *
		 * <p><b>Tujuan:</b><br>
		 * Metode inti renderer yang mengisi baris ZK dengan data satu entitas
		 * {@code CaraPengadaanAsset}, menampilkan nama, akun aset, akun transaksi,
		 * keterangan, dan tombol aksi yang sesuai hak akses.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Melakukan cast {@code arg1} ke {@code CaraPengadaanAsset}, lalu menambahkan
		 * secara berurutan: label nama dengan revisi, label akun dengan format
		 * "kode-nama" (atau kosong jika null), label akun transaksi, label keterangan,
		 * dan tombol Salin/Ubah/Hapus melalui {@code Common.copyEditDeleteButtons} yang
		 * menggunakan referensi ke controller induk untuk mengakses {@code init} dan
		 * callback penghapusan.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Nilai null pada akun atau akun transaksi ditangani dengan operator ternary
		 * yang menampilkan string kosong. Exception dari komponen ZK dibiarkan menyebar.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Jika format tampilan akun perlu diubah (misalnya pemisah tanda "-" menjadi " - "),
		 * modifikasi ekspresi string di baris Label akun dan akun transaksi.</p>
		 *
		 * @param arg0 baris ZK {@code Row} yang akan diisi konten
		 * @param arg1 objek data {@code CaraPengadaanAsset} untuk baris ini
		 * @throws Exception jika terjadi error saat pembuatan komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final CaraPengadaanAsset caraPengadaanAsset = (CaraPengadaanAsset) arg1;

			RevisiHelper.createNewRevisi(CaraPengadaanAsset.class, caraPengadaanAsset, caraPengadaanAsset.getNama())
					.setParent(arg0);
			new Label(caraPengadaanAsset.getAkun() == null ? ""
					: caraPengadaanAsset.getAkun().getKode() + "-" + caraPengadaanAsset.getAkun().getNama())
							.setParent(arg0);
			new Label(caraPengadaanAsset.getAkunTransaksi() == null ? ""
					: caraPengadaanAsset.getAkunTransaksi().getKode() + "-"
							+ caraPengadaanAsset.getAkunTransaksi().getNama()).setParent(arg0);
			new Label(caraPengadaanAsset.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, caraPengadaanAsset, CaraPengadaanAssetAction.this)
					.setParent(arg0);

		}

	}

	/**
	 * <h3>onAdd &mdash; Membuka Form Tambah Data Cara Pengadaan Aset Baru</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Event handler yang dipicu saat pengguna mengklik tombol "Tambah" pada toolbar.
	 * Menyiapkan form kosong untuk penginputan cara pengadaan aset baru yang belum
	 * pernah disimpan ke database.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat instance {@code CaraPengadaanAsset} baru (ID null = data baru), memanggil
	 * {@code init(caraPengadaanAsset)} untuk membangun konten form popup, lalu
	 * menampilkan popup dalam mode modal dengan {@code addWindow.setVisible(true)}
	 * dan {@code addWindow.onModal()}.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception diteruskan ke ZK framework melalui {@code throws Exception}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak perlu dimodifikasi kecuali perilaku pembukaan popup berubah secara fundamental.</p>
	 *
	 * @param event event ZK dari klik tombol Tambah
	 * @throws Exception jika terjadi error saat membangun komponen form popup
	 */
	public void onAdd(Event event) throws Exception {
		init(new CaraPengadaanAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <h3>init(GeneralValueObject) &mdash; Implementasi Interface DataInitDefault untuk Impor Data</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi metode interface {@code DataInitDefault} yang memungkinkan sistem
	 * impor data (upload Excel) untuk membuka form edit dengan data yang sudah ada.
	 * Metode ini bertindak sebagai jembatan antara sistem impor generik dan form spesifik
	 * cara pengadaan aset.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Melakukan cast parameter {@code GeneralValueObject} ke {@code CaraPengadaanAsset},
	 * menyimpannya ke field instance, lalu mendelegasikan ke
	 * {@code init(CaraPengadaanAsset)} untuk membangun form. Setelah form siap,
	 * menampilkan popup dalam mode modal.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception ClassCastException akan terjadi jika {@code obj} bukan instance
	 * {@code CaraPengadaanAsset}; ini dianggap error programmer dan dibiarkan menyebar.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini hanya perlu diubah jika tipe parameter interface berubah atau
	 * jika perlu menambahkan logika pre-processing sebelum membuka form.</p>
	 *
	 * @param obj objek {@code GeneralValueObject} yang berisi data cara pengadaan,
	 *            harus dapat di-cast ke {@code CaraPengadaanAsset}
	 * @throws Exception jika terjadi error saat membangun atau menampilkan form popup
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		caraPengadaanAsset = (CaraPengadaanAsset) obj;
		init(caraPengadaanAsset);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <h3>init(CaraPengadaanAsset) &mdash; Membangun Ulang Konten Form Popup Cara Pengadaan</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode internal yang membangun ulang seluruh konten popup {@code addWindow} secara
	 * programatik untuk form tambah atau ubah data cara pengadaan aset. Memastikan form
	 * selalu bersih dari data sesi sebelumnya dengan cara membersihkan dan membangun
	 * ulang setiap kali dipanggil.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menyimpan referensi entitas ke field instance.</li>
	 *   <li>Mengatur judul popup berdasarkan ID entitas (null = tambah, tidak null = ubah).</li>
	 *   <li>Membersihkan konten popup lama dengan {@code Common.clear(addWindow)}.</li>
	 *   <li>Membangun layout: {@code MyBorderlayout} dengan Center berisi grid form dua
	 *       kolom (30% label, sisa input) dan South berisi toolbar tombol Batal dan Simpan.</li>
	 *   <li>Baris form: Nama (wajib), Akun (wajib, pakai {@code AmbilDataAkunBanbox}),
	 *       Akun Transaksi (opsional, pakai {@code AmbilDataAkunBanbox}), info keterangan
	 *       kebijakan akun transaksi, dan Keterangan bebas.</li>
	 *   <li>Nilai awal setiap input diambil dari entitas yang diterima; null untuk akun
	 *       ditangani dengan operator ternary menjadi string kosong.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Metode ini tidak melempar exception secara eksplisit. Error komponen ZK menyebar ke caller.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Untuk menambah field baru, tambahkan komponen input dan baris form baru sebelum
	 * blok South, lalu perbarui {@code onSave} untuk menetapkan nilainya ke entitas.</p>
	 *
	 * @param caraPengadaanAsset entitas cara pengadaan yang akan ditampilkan di form;
	 *                           ID null berarti form tambah data baru, ID tidak null berarti ubah
	 */
	private void init(CaraPengadaanAsset caraPengadaanAsset) {
		this.caraPengadaanAsset = caraPengadaanAsset;
		addWindow.setTitle(caraPengadaanAsset.getId() == null ? "Tambah Cara Pengadaan Aset" : "Ubah Cara Pengadaan Aset");
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

		MyFormRow row = new MyFormRow(); row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Cara Pengadaan *"));
		row.appendChild(nama = new Textbox(caraPengadaanAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun *"));

		row.appendChild(akun = new AmbilDataAkunBanbox(false));
		akun.setValue(caraPengadaanAsset.getAkun() == null ? "" : caraPengadaanAsset.getAkun().getNama());
		akun.setAttribute("akun", caraPengadaanAsset.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Transaksi"));

		row.appendChild(akunTransaksi = new AmbilDataAkunBanbox(false));
		akunTransaksi.setValue(
				caraPengadaanAsset.getAkunTransaksi() == null ? "" : caraPengadaanAsset.getAkunTransaksi().toString());
		akunTransaksi.setAttribute("akun", caraPengadaanAsset.getAkunTransaksi());
		akunTransaksi.setWidth("90%");

		Common.initKeterangan(rows, "* Akun transkasi boleh dikosongkan, jika dikosongkan akan menggunakan Akun Aset");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(caraPengadaanAsset.getKeterangan()));
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
	 * <h3>onSave &mdash; Memvalidasi dan Menyimpan Data Cara Pengadaan ke Database</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memvalidasi seluruh input form, memeriksa keunikan nama, dan menyimpan data
	 * cara pengadaan aset ke database (insert untuk baru, update untuk yang sudah ada).
	 * Metode ini memastikan bahwa setiap cara pengadaan selalu memiliki nama yang valid
	 * dan akun aset yang terhubung.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Validasi nama tidak boleh kosong; jika kosong, tampilkan dialog peringatan
	 *       dan kembalikan {@code false}.</li>
	 *   <li>Validasi akun tidak boleh null (cek atribut "akun" pada komponen
	 *       {@code AmbilDataAkunBanbox}); jika null, tampilkan peringatan dan kembalikan
	 *       {@code false}.</li>
	 *   <li>Cek duplikasi nama dengan {@code checkNamaCaraPengadaanAsset()}; jika duplikat,
	 *       tampilkan peringatan dan kembalikan {@code false}.</li>
	 *   <li>Jika mode ubah (ID tidak null), reload entitas dari sesi Hibernate menggunakan
	 *       {@code session.load()} agar berada dalam managed state.</li>
	 *   <li>Set nama, akun, akun transaksi (boleh null), dan keterangan dari input form.</li>
	 *   <li>Simpan dengan {@code Common.refreshSaveOrUpdate} dan kembalikan {@code true}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Validasi input ditangani dengan dialog pesan ZK. Error database tidak terduga
	 * menyebar melalui {@code throws Exception}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika menambah field wajib baru, tambahkan validasi di awal metode ini sebelum
	 * blok pemeriksaan duplikasi. Akun transaksi sengaja boleh null karena akan
	 * fallback ke akun aset utama saat penjurnalan.</p>
	 *
	 * @param event event ZK dari klik tombol Simpan
	 * @return {@code true} jika data berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi error pada operasi database
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Cara Pengadaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama cara pengadaan yang sesuai; (2) Nama ini digunakan untuk mengidentifikasi metode pengadaan dalam transaksi; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (akun.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Cara Pengadaan belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Akun; (2) Pilih akun jurnal yang sesuai untuk cara pengadaan ini; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		boolean i = checkNamaCaraPengadaanAsset();
		if (i) {
			MyMessageboxConfig.show("Nama Cara Pengadaan sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (caraPengadaanAsset.getId() != null) {
			caraPengadaanAsset = (CaraPengadaanAsset) session.load(CaraPengadaanAsset.class,
					caraPengadaanAsset.getId());

		}

		caraPengadaanAsset.setNama(nama.getValue());
		caraPengadaanAsset.setAkun((Akun) akun.getAttribute("akun"));
		caraPengadaanAsset.setAkunTransaksi((Akun) akunTransaksi.getAttribute("akun"));

		caraPengadaanAsset.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, caraPengadaanAsset);

		return true;
	}

	/**
	 * <h3>initCriteria &mdash; Membangun Kriteria Query Hibernate untuk Pencarian Cara Pengadaan</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Membangun objek {@code Criteria} Hibernate yang berisi kondisi filter pencarian
	 * cara pengadaan aset. Implementasi interface {@code DataCriteria} yang digunakan
	 * bersama oleh {@code onSearchDefault} (untuk mengambil data paged) dan sistem
	 * cetak/ekspor (untuk mengambil semua data tanpa paging).</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat kriteria untuk entitas {@code CaraPengadaanAsset}, menambahkan filter
	 * pencarian nama jika {@code searchnama} tidak kosong (menggunakan ilike ANYWHERE
	 * untuk pencarian case-insensitive yang mengandung kata kunci), dan menambahkan
	 * pengurutan abjad berdasarkan nama jika parameter {@code order} bernilai true.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari Hibernate dibiarkan menyebar ke caller.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tambahkan kondisi filter baru dengan {@code criteria.add(...)} jika perlu
	 * memperluas kemampuan pencarian, misalnya filter berdasarkan akun.</p>
	 *
	 * @param order jika {@code true}, hasil diurutkan nama A-Z; jika {@code false},
	 *              tidak ada pengurutan (digunakan untuk query COUNT paging)
	 * @return objek {@code Criteria} siap dieksekusi sesuai filter aktif
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CaraPengadaanAsset.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * <h3>onSearchDefault &mdash; Memuat Ulang Data Grid Cara Pengadaan dengan Filter Aktif</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Event handler dan implementasi interface {@code DataSearchDefault} yang memuat
	 * ulang data grid cara pengadaan aset berdasarkan filter aktif dan paging saat ini.
	 * Dipanggil saat halaman dimuat pertama kali, saat paging berubah, dan setelah
	 * operasi simpan atau hapus berhasil.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code Common.initPaging} untuk memperbarui hitungan total baris pada
	 * komponen paging, kemudian mengambil data halaman aktif menggunakan {@code initCriteria(true)}
	 * dengan batas baris dan offset sesuai halaman aktif. Hasil dibungkus dalam
	 * {@code SimpleListModel} dan ditetapkan ke grid dengan renderer baru.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Error Hibernate dibiarkan menyebar. Anotasi {@code @SuppressWarnings("unchecked")}
	 * menangani casting generik dari {@code list()}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Konstanta {@code Common.ROWS_COUNT_ON_PAGE} mengontrol jumlah baris per halaman
	 * secara global untuk seluruh aplikasi.</p>
	 *
	 * @param event event ZK pemicu pencarian; dapat null jika dipanggil programatik
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CaraPengadaanAsset> caraPengadaanAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(caraPengadaanAsset);
		grid.setRowRenderer(new CaraPengadaanAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <h3>checkNamaCaraPengadaanAsset &mdash; Memeriksa Duplikasi Nama Cara Pengadaan</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memverifikasi apakah nama cara pengadaan yang diinput oleh pengguna sudah ada di
	 * database, untuk mencegah duplikasi data master yang dapat menyebabkan kerancuan
	 * pada proses penjurnalan otomatis pengadaan aset.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menggunakan Hibernate Criteria untuk menghitung jumlah baris pada tabel
	 * {@code CaraPengadaanAsset} yang memiliki nama yang sama dengan input {@code nama}.
	 * Pada mode ubah (ID tidak null), mengecualikan entitas yang sedang diedit dengan
	 * kondisi {@code ne("id", ...)}, sehingga pengguna tetap bisa menyimpan tanpa
	 * mengubah nama. Mengembalikan {@code true} jika count lebih dari nol.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Hasil {@code uniqueResult()} di-cast ke {@code Number} untuk kompatibilitas
	 * lintas database. Exception Hibernate dibiarkan menyebar.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pertimbangkan menambahkan UNIQUE INDEX pada kolom nama di level database sebagai
	 * perlindungan tambahan terhadap race condition pada insert bersamaan.</p>
	 *
	 * @return {@code true} jika nama sudah ada di database (duplikat);
	 *         {@code false} jika nama unik atau hanya dimiliki oleh entitas yang sedang diedit
	 */
	public Boolean checkNamaCaraPengadaanAsset() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(CaraPengadaanAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.caraPengadaanAsset.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.caraPengadaanAsset.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
