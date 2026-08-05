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
import ais.database.model.asset.JenisPerjanjianKerjasamaAsset;
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
 * <h3>JenisPerjanjianKerjasamaAssetAction — Kontroler CRUD Data Master Jenis Perjanjian Kerjasama Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah kontroler ZKoss (composer) yang mengelola operasi CRUD lengkap untuk entitas
 * {@link JenisPerjanjianKerjasamaAsset}. Data master jenis perjanjian kerjasama berfungsi sebagai
 * referensi tipe kontrak atau perjanjian dalam modul manajemen aset, khususnya untuk mencatat
 * dan mengklasifikasikan berbagai jenis kerjasama pengadaan barang/jasa antara institusi dengan
 * pihak ketiga (vendor, kontraktor, atau mitra bisnis). Setiap jenis perjanjian memiliki konfigurasi
 * akun akuntansi yang terkait: akun pekerjaan (debet biaya) dan akun hutang pekerjaan (kredit
 * kewajiban). Konfigurasi akun ini secara otomatis digunakan saat membuat jurnal dari transaksi
 * yang menggunakan jenis perjanjian tertentu, memastikan konsistensi pencatatan akuntansi.
 *
 * <b>Cara kerja:</b><br>
 * Controller mengimplementasikan tiga antarmuka: {@code DataCriteria} untuk membangun query
 * Hibernate, {@code DataSearchDefault} untuk rendering grid, dan {@code DataInitDefault} untuk
 * inisialisasi form dari luar kelas. Arsitektur secara keseluruhan identik dengan
 * {@link JenisPemesananPengadaanAssetAction} namun lebih sederhana karena hanya memiliki dua
 * akun (bukan tiga) dan tidak ada flag konfigurasi tambahan. Penyimpanan data dilakukan
 * langsung melalui {@code Common.refreshSaveOrUpdate} tanpa lapisan DAO tambahan. Setiap baris
 * grid dilengkapi checkbox aktif/nonaktif yang dapat diubah secara langsung tanpa membuka form,
 * serta tombol aksi salin, ubah, dan hapus. Tombol cetak dan unggah data juga tersedia di toolbar.
 *
 * <b>Threading:</b><br>
 * Semua operasi berjalan pada thread event ZKoss tunggal. Tidak ada mekanisme concurrency
 * atau operasi asinkron. Session Hibernate yang digunakan adalah {@code HibernateUtil.currentSession()}
 * yang terikat pada sesi pengguna aktif. Objek field yang menyimpan referensi komponen ZKoss
 * (seperti {@code nama}, {@code akunDp}, dll.) adalah state controller yang shared hanya dalam
 * satu request/event, sehingga tidak menimbulkan masalah thread-safety dalam skenario normal.
 *
 * <b>Pemeliharaan:</b><br>
 * Nama field akun di kelas ini menggunakan penamaan yang sedikit berbeda dari konteksnya:
 * {@code akunDp} mewakili "Akun Pekerjaan" (bukan uang muka/down payment), dan {@code akunUtangDp}
 * mewakili "Akun Hutang Pekerjaan". Penamaan ini adalah warisan dari struktur entitas awal dan
 * sebaiknya tidak diubah untuk menghindari refaktor yang berpotensi merusak. Jika ada perubahan
 * pada entitas {@link JenisPerjanjianKerjasamaAsset} (tambah field baru), perlu memperbarui:
 * array {@code contents} di {@code doAfterCompose}, metode {@code init} untuk baris form baru,
 * metode {@code onSave} untuk menyimpan nilai field baru, dan renderer untuk kolom baru di grid.
 *
 * @author Tim Pengembang AIS
 * @version 1.0
 * @see JenisPerjanjianKerjasamaAsset
 * @see DataCriteria
 * @see DataSearchDefault
 * @see DataInitDefault
 */
public class JenisPerjanjianKerjasamaAssetAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Nomor versi serial untuk serialisasi kelas ini.
	 * Tidak perlu diubah kecuali ada perubahan struktur yang tidak kompatibel mundur.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZKoss untuk menampilkan form tambah atau ubah data jenis perjanjian kerjasama. */
	private MyWindow addWindow;

	/** Komponen paging ZKoss untuk navigasi halaman pada grid daftar jenis perjanjian kerjasama. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar seluruh data jenis perjanjian kerjasama yang tersedia. */
	private MyGrid grid;

	/** Kotak teks pencarian berdasarkan nama jenis perjanjian pada area filter di atas grid. */
	private Textbox searchnama;

	/** Checkbox filter untuk menampilkan hanya data aktif (dicentang) atau semua data. */
	private Checkbox searchaktif;

	/** Kotak teks input nama jenis perjanjian kerjasama pada form tambah/ubah data. */
	private Textbox nama;

	/** Kotak teks input keterangan atau deskripsi jenis perjanjian kerjasama pada form tambah/ubah. */
	private Textbox keterangan;

	/** Penanda apakah pengguna saat ini memiliki hak akses untuk mengubah data (UPDATE privilege). */
	private boolean edit = false;

	/** Penanda apakah pengguna saat ini memiliki hak akses untuk menghapus data (DELETE privilege). */
	private boolean delete = false;

	/** Referensi ke objek entitas jenis perjanjian kerjasama yang sedang diproses. */
	private JenisPerjanjianKerjasamaAsset jenisPerjanjianKerjasamaAsset;

	/** Tombol "Tambah" pada toolbar halaman, visibilitasnya dikendalikan oleh hak akses CREATE. */
	private MyToolbarbuttonConfig add;

	/** Kotak teks input kode unik jenis perjanjian kerjasama pada form tambah/ubah data. */
	private Textbox kode;

	/**
	 * Komponen banbox (autocomplete input) untuk memilih akun pekerjaan dari daftar akun.
	 * Meskipun field bernama {@code akunDp}, dalam konteks kerjasama ini mewakili akun
	 * untuk mencatat biaya pekerjaan (akun beban/biaya di sisi debet jurnal).
	 */
	private AmbilDataAkunBanbox akunDp;

	/**
	 * Komponen banbox untuk memilih akun hutang pekerjaan dari daftar akun.
	 * Akun ini digunakan di sisi kredit jurnal untuk mencatat kewajiban pembayaran
	 * kepada pihak yang memberikan jasa atau pekerjaan sesuai perjanjian.
	 */
	private AmbilDataAkunBanbox akunUtangDp;

	/**
	 * Metode siklus hidup ZKoss yang dipanggil sebelum komponen halaman dibangun.
	 *
	 * <b>Tujuan:</b><br>
	 * Melakukan pemeriksaan keamanan akses pengguna sebelum halaman ZUL dirender sepenuhnya,
	 * mencegah akses tidak sah ke halaman manajemen jenis perjanjian kerjasama aset.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi bahwa pengguna sudah
	 * login dan berwenang mengakses halaman ini. Mendelegasikan ke implementasi induk melalui
	 * {@code super.doBeforeCompose()} untuk menyelesaikan inisialisasi ZKoss. Dipanggil secara
	 * otomatis oleh framework ZKoss sebelum proses composing dimulai.
	 *
	 * <b>Parameter:</b><br>
	 * @param page halaman ZKoss saat ini yang sedang dikompilasi oleh framework
	 * @param parent komponen induk dalam hierarki komponen ZUL
	 * @param compInfo informasi metadata komponen yang sedang diproses oleh ZKoss
	 *
	 * <b>Return:</b><br>
	 * @return objek {@code ComponentInfo} dari kelas induk untuk kelanjutan proses composing
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika pemeriksaan keamanan gagal, {@code Common.doCheckSecurity()} akan menghentikan
	 * proses dan mengarahkan pengguna ke halaman login.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jangan hapus pemanggilan {@code super.doBeforeCompose()} karena dibutuhkan untuk
	 * mekanisme autowiring komponen ZKoss yang menghubungkan elemen ZUL ke field Java.
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
	 * Menginisialisasi seluruh logika bisnis halaman manajemen jenis perjanjian kerjasama,
	 * termasuk pengaturan hak akses, pemuatan data awal, konfigurasi paging, dan penambahan
	 * tombol cetak serta unggah ke toolbar.
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menyelesaikan autowiring ZKoss dan mengatur preferensi bahasa antarmuka.
	 * (2) Mengatur visibilitas tombol Tambah berdasarkan hak akses CREATE pengguna.
	 * (3) Menetapkan nilai flag {@code edit} dan {@code delete} berdasarkan hak akses.
	 * (4) Memuat data jenis perjanjian ke grid dengan {@code onSearchDefault(null)}.
	 * (5) Mendaftarkan event listener paging untuk navigasi halaman grid.
	 * (6) Menambahkan tombol cetak (ekspor Excel) dengan kolom yang dikonfigurasi dalam
	 *     array {@code contents}: id, kode, nama, akunDp, akunUtangDp, keterangan, aktif.
	 * (7) Menambahkan tombol unggah data (impor) yang hanya terlihat untuk pengguna yang
	 *     memiliki semua hak akses CREATE, UPDATE, dan DELETE secara bersamaan.
	 *
	 * <b>Parameter:</b><br>
	 * @param comp komponen akar halaman ZUL yang sudah selesai dibangun oleh framework ZKoss
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan selama inisialisasi komponen atau pemuatan data
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Array {@code contents} mendefinisikan kolom yang akan disertakan dalam ekspor Excel.
	 * Nama setiap elemen harus sesuai dengan nama property (getter) di entitas
	 * {@link JenisPerjanjianKerjasamaAsset} agar refleksi Java dapat mengaksesnya dengan benar.
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

		String[] contents = new String[] { "id", "kode", "nama", "akunDp", "akunUtangDp", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisPerjanjianKerjasamaAsset.class, this,
				contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisPerjanjianKerjasamaAsset.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Inner class renderer yang bertanggung jawab merender setiap baris data pada grid
	 * daftar jenis perjanjian kerjasama aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengkonversi objek {@link JenisPerjanjianKerjasamaAsset} menjadi baris tampilan yang
	 * informatif di grid, termasuk informasi akun terkait dan kontrol interaktif untuk
	 * mengubah status aktif dan mengakses operasi CRUD.
	 *
	 * <b>Cara kerja:</b><br>
	 * Untuk setiap objek data, metode {@code render} membuat Label untuk kode dan nama
	 * (via RevisiHelper), label akun pekerjaan dan akun hutang dalam format "kode nama"
	 * (atau string kosong jika akun belum dikonfigurasi), label keterangan, checkbox Aktif
	 * yang langsung menyimpan perubahan ke database, dan tombol salin/ubah/hapus melalui
	 * {@code Common.copyEditDeleteButtons}. Checkbox yang diubah langsung menyimpan status
	 * aktif/nonaktif ke database menggunakan {@code Common.refreshSaveOrUpdate}.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Urutan komponen yang ditambahkan ke Row harus konsisten dengan definisi kolom (column)
	 * di file ZUL yang sesuai. Jika ada perubahan urutan atau penambahan kolom baru, kedua
	 * tempat (renderer dan ZUL) harus diperbarui secara bersamaan untuk menghindari data
	 * tampil di kolom yang salah.
	 */
	class JenisPerjanjianKerjasamaAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data jenis perjanjian kerjasama ke dalam komponen ZKoss Row.
		 *
		 * <b>Tujuan:</b><br>
		 * Mengisi komponen {@link Row} ZKoss dengan semua data dari satu entitas
		 * {@link JenisPerjanjianKerjasamaAsset}, termasuk label informatif, checkbox interaktif,
		 * dan tombol aksi untuk operasi CRUD.
		 *
		 * <b>Cara kerja:</b><br>
		 * (1) Mengatur vertical alignment baris ke atas dan melakukan cast objek ke tipe entitas.
		 * (2) Menambahkan Label kode sebagai sel pertama.
		 * (3) Menambahkan RevisiHelper untuk nama (mendukung tampilan riwayat perubahan).
		 * (4) Menambahkan Label akun pekerjaan dalam format "kode nama" jika tidak null.
		 * (5) Menambahkan Label akun hutang pekerjaan dalam format "kode nama" jika tidak null.
		 * (6) Menambahkan Label keterangan.
		 * (7) Menambahkan checkbox Aktif yang interaktif: saat diklik, langsung menyimpan
		 *     perubahan status ke database via {@code Common.refreshSaveOrUpdate} tanpa
		 *     perlu membuka form edit.
		 * (8) Menambahkan tombol salin, ubah, dan hapus via {@code Common.copyEditDeleteButtons}.
		 *
		 * <b>Parameter:</b><br>
		 * @param arg0 objek {@link Row} ZKoss yang akan diisi dengan komponen-komponen data
		 * @param arg1 objek data {@link JenisPerjanjianKerjasamaAsset} yang akan dirender
		 *
		 * <b>Penanganan error:</b><br>
		 * @throws Exception jika terjadi kesalahan saat membuat atau memasang komponen ZKoss,
		 *                   atau saat menyimpan perubahan status checkbox ke database
		 *
		 * <b>Pemeliharaan:</b><br>
		 * Checkbox yang dinonaktifkan ({@code setDisabled(!edit)}) saat edit=false memastikan
		 * pengguna tanpa hak akses UPDATE tidak dapat mengubah status aktif langsung dari grid.
		 * Konsistensi ini harus dijaga jika ada perubahan pada model hak akses sistem.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisPerjanjianKerjasamaAsset jenisPerjanjianKerjasamaAsset = (JenisPerjanjianKerjasamaAsset) arg1;
			new Label(jenisPerjanjianKerjasamaAsset.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(JenisPerjanjianKerjasamaAsset.class, jenisPerjanjianKerjasamaAsset,
					jenisPerjanjianKerjasamaAsset.getNama()).setParent(arg0);
			new Label(jenisPerjanjianKerjasamaAsset.getAkunDp() == null ? ""
					: (jenisPerjanjianKerjasamaAsset.getAkunDp().getKode() + " "
							+ jenisPerjanjianKerjasamaAsset.getAkunDp().getNama()))
					.setParent(arg0);

			new Label(jenisPerjanjianKerjasamaAsset.getAkunUtangDp() == null ? ""
					: (jenisPerjanjianKerjasamaAsset.getAkunUtangDp().getKode() + " "
							+ jenisPerjanjianKerjasamaAsset.getAkunUtangDp().getNama()))
					.setParent(arg0);

			new Label(jenisPerjanjianKerjasamaAsset.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisPerjanjianKerjasamaAsset.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPerjanjianKerjasamaAsset.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisPerjanjianKerjasamaAsset);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisPerjanjianKerjasamaAsset,
					JenisPerjanjianKerjasamaAssetAction.this).setParent(arg0);

		}

	}

	/**
	 * Event handler yang dipanggil ketika pengguna menekan tombol "Tambah" pada toolbar halaman.
	 *
	 * <b>Tujuan:</b><br>
	 * Membuka form tambah data jenis perjanjian kerjasama baru dalam jendela modal untuk
	 * memungkinkan pengguna mendefinisikan kategori perjanjian baru beserta konfigurasi
	 * akun akuntansinya.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat objek {@link JenisPerjanjianKerjasamaAsset} baru kosong (tanpa ID) dan
	 * meneruskannya ke metode {@code init(JenisPerjanjianKerjasamaAsset)} yang membangun
	 * komponen form secara programatik. Kemudian menampilkan jendela modal {@code addWindow}.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZKoss dari klik tombol Tambah pada toolbar halaman
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan saat membangun komponen form atau modal
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Dipanggil otomatis oleh ZKoss berdasarkan konvensi penamaan event handler. Tidak ada
	 * logika tambahan yang diperlukan selain mendelegasikan ke metode init.
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisPerjanjianKerjasamaAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Implementasi antarmuka {@link DataInitDefault} untuk menginisialisasi form dari luar kelas.
	 *
	 * <b>Tujuan:</b><br>
	 * Menyediakan titik masuk publik bagi sistem generik (seperti tombol salin dan ubah dari
	 * {@code Common.copyEditDeleteButtons}) untuk membuka form edit dengan objek data yang
	 * sudah ditentukan tanpa mengetahui tipe controller secara spesifik.
	 *
	 * <b>Cara kerja:</b><br>
	 * Melakukan cast objek {@link GeneralValueObject} ke {@link JenisPerjanjianKerjasamaAsset}
	 * dan menyimpannya ke field instance. Mendelegasikan ke metode privat
	 * {@code init(JenisPerjanjianKerjasamaAsset)} yang membangun form dengan data tersebut.
	 * Menampilkan jendela modal setelah form siap.
	 *
	 * <b>Parameter:</b><br>
	 * @param obj objek {@link GeneralValueObject} yang merupakan instance
	 *            {@link JenisPerjanjianKerjasamaAsset} yang akan diedit atau disalin datanya
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika cast gagal (tipe objek tidak kompatibel) atau terjadi kesalahan
	 *                   saat membangun komponen form modal
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Implementasi ini diperlukan karena antarmuka {@code DataInitDefault} digunakan oleh
	 * infrastruktur generik CRUD. Jika parameter yang diterima bukan instance
	 * {@link JenisPerjanjianKerjasamaAsset}, akan terjadi ClassCastException yang perlu
	 * dideteksi saat testing integrasi.
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisPerjanjianKerjasamaAsset = (JenisPerjanjianKerjasamaAsset) obj;
		init(jenisPerjanjianKerjasamaAsset);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Metode privat yang membangun seluruh antarmuka form tambah/ubah data jenis perjanjian kerjasama.
	 *
	 * <b>Tujuan:</b><br>
	 * Membersihkan konten jendela modal dan membangun ulang semua komponen form secara
	 * programatik berdasarkan data entitas yang diberikan, mendukung mode tambah (entitas
	 * baru tanpa ID) dan mode ubah (entitas yang sudah ada di database dengan ID).
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menyimpan referensi entitas ke field instance dan mengatur judul jendela sesuai mode.
	 * (2) Membersihkan konten jendela dengan {@code Common.clear(addWindow)}.
	 * (3) Membangun {@code MyBorderlayout} dengan Center berisi grid form dan South berisi toolbar.
	 * (4) Grid 2 kolom (label 30%, input 70%) dengan baris-baris untuk:
	 *     - Kode jenis kerjasama (textbox, opsional)
	 *     - Nama jenis kerjasama (textbox, wajib diisi, ditandai dengan *)
	 *     - Akun Pekerjaan (banbox AmbilDataAkunBanbox, wajib diisi, ditandai dengan *)
	 *     - Akun Hutang Pekerjaan (banbox AmbilDataAkunBanbox, wajib diisi, ditandai dengan *)
	 *     - Keterangan (textarea 3 baris, opsional)
	 * (5) Setiap banbox akun diisi dengan nilai saat ini dari entitas dan atribut "akun"
	 *     diset untuk memungkinkan retrieval nilai yang dipilih di metode {@code onSave}.
	 * (6) Tombol Batal menutup jendela; tombol Simpan memanggil {@code onSave} dan menutup
	 *     jendela serta merefresh grid jika penyimpanan berhasil.
	 *
	 * <b>Parameter:</b><br>
	 * @param jenisPerjanjianKerjasamaAsset objek entitas berisi data yang akan ditampilkan;
	 *                                      jika {@code getId() == null} maka ini mode tambah baru
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan saat membangun komponen ZKoss
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Banbox akun menggunakan pola atribut ("akun") untuk menyimpan objek yang dipilih karena
	 * komponen banbox tidak mendukung binding langsung ke objek entitas. Pastikan nama atribut
	 * "akun" konsisten antara penulisan di sini (setAttribute) dan pembacaan di onSave (getAttribute).
	 */
	private void init(JenisPerjanjianKerjasamaAsset jenisPerjanjianKerjasamaAsset) throws Exception {
		this.jenisPerjanjianKerjasamaAsset = jenisPerjanjianKerjasamaAsset;
		addWindow.setTitle(jenisPerjanjianKerjasamaAsset.getId() == null ? "Tambah Jenis Perjanjian Kerjasama" : "Ubah Jenis Perjanjian Kerjasama");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Jenis Kerjasama"));
		row.appendChild(kode = new Textbox(jenisPerjanjianKerjasamaAsset.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Kerjasama *"));
		row.appendChild(nama = new Textbox(jenisPerjanjianKerjasamaAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Pekerjaan *"));
		row.appendChild(akunDp = new AmbilDataAkunBanbox(false));
		akunDp.setValue(jenisPerjanjianKerjasamaAsset.getAkunDp() == null ? ""
				: jenisPerjanjianKerjasamaAsset.getAkunDp().toString());
		akunDp.setAttribute("akun", jenisPerjanjianKerjasamaAsset.getAkunDp());
		akunDp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Hutang Pekerjaan *"));
		row.appendChild(akunUtangDp = new AmbilDataAkunBanbox(false));
		akunUtangDp.setValue(jenisPerjanjianKerjasamaAsset.getAkunUtangDp() == null ? ""
				: jenisPerjanjianKerjasamaAsset.getAkunUtangDp().toString());
		akunUtangDp.setAttribute("akun", jenisPerjanjianKerjasamaAsset.getAkunUtangDp());
		akunUtangDp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisPerjanjianKerjasamaAsset.getKeterangan()));
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
	 * Memvalidasi input form dan menyimpan data jenis perjanjian kerjasama ke database.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengambil nilai dari semua komponen input form, melakukan validasi kelengkapan data
	 * yang wajib diisi, dan menyimpan atau memperbarui entitas
	 * {@link JenisPerjanjianKerjasamaAsset} ke database melalui Hibernate. Dipanggil
	 * oleh event listener tombol Simpan di form modal.
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memeriksa apakah nama tidak kosong; jika kosong menampilkan pesan peringatan
	 *     informasi dan mengembalikan {@code false}.
	 * (2) Memeriksa apakah akun pekerjaan ({@code akunDp}) sudah dipilih dari banbox;
	 *     jika belum menampilkan peringatan tipe EXCLAMATION.
	 * (3) Memeriksa apakah akun hutang pekerjaan ({@code akunUtangDp}) sudah dipilih;
	 *     jika belum menampilkan peringatan tipe EXCLAMATION.
	 * (4) Mendapatkan session Hibernate saat ini. Jika entitas sudah ada di database
	 *     (ID tidak null), memuat ulang dari session menggunakan {@code load()} agar
	 *     perubahan dapat ditrack oleh Hibernate dirty checking.
	 * (5) Menetapkan kode, nama, akun pekerjaan, akun hutang, dan keterangan dari
	 *     komponen form ke properti entitas.
	 * (6) Menyimpan melalui {@code Common.refreshSaveOrUpdate(session, entitas)}.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZKoss dari tombol Simpan yang memicu pemanggilan metode ini
	 *
	 * <b>Return:</b><br>
	 * @return {@code true} jika data berhasil disimpan ke database; {@code false} jika
	 *         validasi gagal (sehingga form tetap terbuka untuk perbaikan pengguna)
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan akses database Hibernate, misalnya pelanggaran
	 *                   constraint atau koneksi database terputus
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Urutan validasi sebaiknya mengikuti urutan tampilan field di form agar pengguna mendapat
	 * feedback yang intuitif dan tidak membingungkan. Saat ada field wajib baru yang ditambahkan
	 * ke entitas, tambahkan validasi yang sesuai di blok awal metode ini sebelum akses database.
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Jenis Perjanjian Kerjasama belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama jenis perjanjian kerjasama; (2) Nama ini digunakan untuk mengidentifikasi jenis perjanjian dalam transaksi; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (akunDp.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Pekerjaan belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Akun Pekerjaan; (2) Pilih akun jurnal pekerjaan dari daftar akun; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (akunUtangDp.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Hutang Pekerjaan belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Akun Hutang Pekerjaan; (2) Pilih akun jurnal hutang pekerjaan dari daftar akun; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisPerjanjianKerjasamaAsset.getId() != null) {
			jenisPerjanjianKerjasamaAsset = (JenisPerjanjianKerjasamaAsset) session
					.load(JenisPerjanjianKerjasamaAsset.class, jenisPerjanjianKerjasamaAsset.getId());

		}

		jenisPerjanjianKerjasamaAsset.setKode(kode.getValue());
		jenisPerjanjianKerjasamaAsset.setNama(nama.getValue());
		jenisPerjanjianKerjasamaAsset.setAkunDp((Akun) akunDp.getAttribute("akun"));
		jenisPerjanjianKerjasamaAsset.setAkunUtangDp((Akun) akunUtangDp.getAttribute("akun"));
		jenisPerjanjianKerjasamaAsset.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisPerjanjianKerjasamaAsset);

		return true;
	}

	/**
	 * Implementasi antarmuka {@link DataCriteria} untuk membangun objek Hibernate Criteria
	 * yang digunakan sebagai dasar query pencarian data jenis perjanjian kerjasama.
	 *
	 * <b>Tujuan:</b><br>
	 * Membangun dan mengembalikan objek {@link Criteria} Hibernate yang mengkombinasikan
	 * kondisi filter aktif dan pencarian nama untuk mengambil data dari database sesuai
	 * parameter yang diinput pengguna.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat Criteria untuk kelas {@link JenisPerjanjianKerjasamaAsset}. Filter aktif:
	 * jika checkbox searchaktif dicentang atau null (tidak ada di ZUL), hanya data dengan
	 * {@code aktif = true} atau {@code aktif IS NULL} yang diambil; jika tidak dicentang,
	 * semua data diambil menggunakan sqlRestriction("true"). Parameter {@code order}
	 * mengontrol penambahan pengurutan nama ascending — diset false untuk query hitungan
	 * total data yang lebih efisien. Filter nama menggunakan ILIKE case-insensitive dengan
	 * ANYWHERE matching jika kotak pencarian tidak kosong.
	 *
	 * <b>Parameter:</b><br>
	 * @param order {@code true} untuk menambahkan pengurutan nama ascending pada hasil query;
	 *              {@code false} untuk query tanpa pengurutan (untuk menghitung total paging)
	 *
	 * <b>Return:</b><br>
	 * @return objek {@link Criteria} Hibernate yang siap dieksekusi dengan semua filter
	 *         pencarian yang telah dikonfigurasi
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Metode ini dipanggil dua kali per refresh grid: sekali tanpa order untuk menghitung
	 * total (digunakan oleh paging), dan sekali dengan order untuk mengambil data halaman.
	 * Jika ada filter pencarian baru (misalnya berdasarkan kode), tambahkan kondisi
	 * Restrictions yang sesuai dan deklarasikan komponen filter baru di field kelas.
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPerjanjianKerjasamaAsset.class)
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
	 * Memuat dan menampilkan data hasil pencarian jenis perjanjian kerjasama ke grid utama halaman.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengeksekusi query pencarian berdasarkan kriteria filter aktif, menerapkan batasan
	 * paging, dan memperbarui model data grid sehingga pengguna melihat daftar jenis
	 * perjanjian yang sesuai filter pada halaman yang sedang aktif.
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memanggil {@code Common.initPaging} dengan criteria tanpa order untuk menghitung
	 *     total data dan memperbarui komponen paging ZKoss secara otomatis.
	 * (2) Mengeksekusi query dengan criteria berurutan (order=true), dibatasi
	 *     {@code Common.ROWS_COUNT_ON_PAGE} baris, dimulai dari offset yang dihitung
	 *     berdasarkan nomor halaman aktif di komponen paging.
	 * (3) Membungkus list hasil query dalam {@code SimpleListModel} dan menetapkannya ke
	 *     grid bersama instance renderer baru {@code JenisPerjanjianKerjasamaAssetRenderer}.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZKoss; dapat {@code null} jika dipanggil secara programatik
	 *              (misalnya setelah operasi simpan data atau saat inisialisasi halaman)
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Membuat instance renderer baru setiap kali dipanggil adalah pola standar ZKoss untuk
	 * memastikan state renderer bersih. Jika renderer perlu menyimpan state antar render,
	 * pertimbangkan untuk membuat renderer sebagai field instance bukan variabel lokal.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPerjanjianKerjasamaAsset> jenisPerjanjianKerjasamaAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisPerjanjianKerjasamaAsset);
		grid.setRowRenderer(new JenisPerjanjianKerjasamaAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

}
