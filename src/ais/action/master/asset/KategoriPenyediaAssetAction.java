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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.KategoriPenyediaAsset;
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
 * <h3>KategoriPenyediaAssetAction &mdash; Controller CRUD Kategori Penyedia Aset</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah controller ZK (composer) untuk halaman manajemen data master
 * <em>Kategori Penyedia Aset</em>, yaitu daftar klasifikasi kategori yang digunakan
 * untuk mengelompokkan vendor atau penyedia barang dan jasa dalam proses pengadaan
 * aset institusi. Contoh kategori penyedia meliputi: pemasok bahan bangunan, distributor
 * peralatan elektronik, kontraktor jasa konstruksi, penyedia layanan perawatan, vendor
 * alat tulis dan perlengkapan kantor, dan sebagainya. Kategori ini berbeda dari Jenis
 * Pekerjaan Penyedia karena bersifat lebih umum dan hierarkis: satu kategori dapat
 * mencakup beberapa jenis pekerjaan. Data master ini digunakan sebagai referensi
 * klasifikasi saat mendaftarkan vendor baru, memudahkan pencarian dan perbandingan
 * vendor pada proses seleksi pengadaan. Fitur flag aktif memungkinkan pengelolaan
 * siklus hidup kategori tanpa menghapus data historis. Halaman ini mendukung operasi
 * tambah, ubah, nonaktifkan, ekspor Excel, dan impor data massal.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Controller mewarisi {@code GenericAutowireComposer} ZK dan mengimplementasikan
 * {@code DataCriteria}, {@code DataSearchDefault}, dan {@code DataInitDefault}.
 * Secara struktural, kelas ini sangat mirip dengan {@code JenisPekerjaanPenyediaAction}
 * dengan perbedaan utama pada entitas yang dikelola ({@code KategoriPenyediaAsset}
 * bukan {@code JenisPekerjaanPenyedia}) dan penggunaan {@code .list()} biasa (bukan
 * {@code ConstantValues.simpleList}) pada {@code onSearchDefault}.
 * Alur kerja utama:
 * <ol>
 *   <li>{@code doBeforeCompose} memverifikasi keamanan sesi.</li>
 *   <li>{@code doAfterCompose} menginisialisasi hak akses, memuat data, menyiapkan
 *       paging, serta mendaftarkan tombol cetak (kolom: id, kode, nama, keterangan, aktif)
 *       dan tombol upload impor ke toolbar.</li>
 *   <li>{@code onSearchDefault} memuat ulang grid dengan filter nama dan status aktif
 *       menggunakan {@code .list()} biasa dari Hibernate Criteria.</li>
 *   <li>{@code KategoriPenyediaAssetRenderer} merender setiap baris dengan kode, nama
 *       (dengan RevisiHelper), keterangan, checkbox aktif yang dapat diubah langsung
 *       dari grid (inline edit), dan tombol aksi Salin/Ubah/Hapus.</li>
 *   <li>Form popup dibangun oleh {@code init(KategoriPenyediaAsset)} dengan field
 *       kode (opsional), nama (wajib), dan keterangan (multiline).</li>
 *   <li>{@code onSave} memeriksa duplikasi nama dan menyimpan data.</li>
 *   <li>{@code checkNamaKategoriPenyediaAsset} memeriksa keunikan nama di database.</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh operasi dijalankan pada thread ZK event (UI thread). Sesi Hibernate dari
 * {@code HibernateUtil.currentSession()} terikat pada thread HTTP request yang aktif saat itu.
 * Perubahan flag aktif melalui checkbox inline di grid juga dijalankan sinkron pada UI thread.
 * Tidak ada kebutuhan sinkronisasi tambahan karena tidak ada penggunaan thread latar belakang.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Untuk menambah kolom baru ke entitas {@code KategoriPenyediaAsset}, tambahkan field
 * {@code Textbox} baru di kelas ini, tambahkan baris form di {@code init(KategoriPenyediaAsset)},
 * perbarui {@code onSave} untuk menetapkan nilai field baru, dan perbarui array {@code contents}
 * di {@code doAfterCompose} agar kolom baru tersedia pada fitur ekspor/impor Excel.
 * Pastikan menguji dengan berbagai kombinasi hak akses (READ, CREATE, UPDATE, DELETE) untuk
 * memverifikasi visibilitas dan fungsionalitas tombol yang dikontrol oleh {@code CommonPrivilages}.</p>
 *
 * @author Tim Pengembang AIS
 * @version 1.0
 * @see KategoriPenyediaAsset
 * @see DataCriteria
 * @see DataSearchDefault
 * @see DataInitDefault
 * @see CommonPrivilages
 */
public class KategoriPenyediaAssetAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal popup untuk form tambah/ubah data kategori penyedia. */
	private MyWindow addWindow;

	/** Komponen paging ZK untuk navigasi halaman pada grid data. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar kategori penyedia aset. */
	private MyGrid grid;

	/** Kotak teks filter pencarian berdasarkan nama kategori penyedia. */
	private Textbox searchnama;

	/**
	 * Checkbox filter status aktif; jika dicentang, hanya menampilkan kategori yang aktif
	 * atau yang belum ditetapkan status aktifnya (null); jika tidak dicentang, semua ditampilkan.
	 */
	private Checkbox searchaktif;

	/** Kotak teks input nama kategori penyedia pada form tambah/ubah. */
	private Textbox nama;

	/** Kotak teks input keterangan kategori penyedia. */
	private Textbox keterangan;

	/** Flag yang menandakan apakah pengguna saat ini memiliki hak ubah data. */
	private boolean edit = false;

	/** Flag yang menandakan apakah pengguna saat ini memiliki hak hapus data. */
	private boolean delete = false;

	/** Entitas kategori penyedia aset yang sedang aktif diedit atau ditambahkan. */
	private KategoriPenyediaAsset kategoriPenyediaAsset;

	/** Tombol toolbar untuk menambah data kategori penyedia baru. */
	private MyToolbarbuttonConfig add;

	/** Kotak teks input kode unik kategori penyedia. */
	private Textbox kode;

	/**
	 * <h3>doBeforeCompose &mdash; Verifikasi Keamanan Sebelum Render Halaman</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memastikan keamanan akses halaman manajemen kategori penyedia aset sebelum
	 * komponen ZK dirender oleh framework. Dipanggil oleh ZK sebagai bagian dari
	 * lifecycle halaman sebelum proses pembuatan komponen dimulai.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi bahwa pengguna
	 * memiliki sesi yang valid dan sudah terautentikasi. Jika sesi tidak valid atau
	 * pengguna belum login, proses akan dihentikan dan pengguna dialihkan ke halaman
	 * login secara otomatis. Setelah pemeriksaan keamanan berhasil, memanggil
	 * {@code super.doBeforeCompose(page, parent, compInfo)} untuk melanjutkan proses
	 * lifecycle ZK normal dan mengembalikan {@code ComponentInfo} yang diperlukan.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Sesi tidak valid ditangani oleh logika redirect internal dalam {@code doCheckSecurity}.
	 * Tidak ada exception yang dilempar secara eksplisit dari metode ini sendiri.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan menghapus atau memindahkan pemanggilan {@code Common.doCheckSecurity()} karena
	 * ini merupakan lapisan keamanan wajib pertama. Selalu pastikan {@code super.doBeforeCompose}
	 * dipanggil agar lifecycle ZK dapat berjalan dengan benar dan semua komponen dapat
	 * diinisialisasi dengan baik.</p>
	 *
	 * @param page     halaman ZK yang sedang dalam proses komposisi
	 * @param parent   komponen induk dalam hierarki komponen ZK saat ini
	 * @param compInfo metadata dan informasi komponen yang sedang dikomposisi oleh ZK
	 * @return {@code ComponentInfo} dari implementasi kelas induk, digunakan ZK untuk
	 *         melanjutkan proses pembuatan komponen berikutnya
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <h3>doAfterCompose &mdash; Inisialisasi Lengkap Halaman Kategori Penyedia Aset</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode lifecycle ZK yang dipanggil setelah seluruh komponen halaman berhasil di-wire
	 * secara otomatis oleh ZK. Bertanggung jawab atas inisialisasi menyeluruh halaman:
	 * pengaturan bahasa antarmuka, konfigurasi hak akses tombol berdasarkan privilege
	 * pengguna, pemuatan data grid pertama kali, inisialisasi paging, serta pendaftaran
	 * tombol cetak ekspor Excel dan upload impor data ke toolbar halaman.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} agar ZK menyelesaikan wire
	 *       komponen secara otomatis berdasarkan nama field dan ID komponen ZUL.</li>
	 *   <li>Menginisialisasi bahasa antarmuka dengan {@code Common.initLaguage()}.</li>
	 *   <li>Jika tombol Tambah ({@code add}) tidak null, mengatur visibilitas berdasarkan
	 *       hak CREATE pengguna dan menetapkan tooltip teks "Tambah".</li>
	 *   <li>Menetapkan flag {@code edit} berdasarkan hak UPDATE dan flag {@code delete}
	 *       berdasarkan hak DELETE pengguna saat ini.</li>
	 *   <li>Memanggil {@code onSearchDefault(null)} untuk memuat data kategori penyedia
	 *       pertama kali ke dalam grid.</li>
	 *   <li>Mendaftarkan listener paging anonim yang memanggil {@code onSearchDefault}
	 *       setiap kali halaman aktif grid berubah.</li>
	 *   <li>Membuat tombol cetak ekspor Excel dengan kolom id, kode, nama, keterangan,
	 *       aktif dan menambahkannya ke toolbar halaman.</li>
	 *   <li>Membuat tombol upload impor data dan mengatur visibilitasnya hanya aktif
	 *       jika pengguna memiliki hak CREATE (tombol add visible) sekaligus hak UPDATE
	 *       dan DELETE.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari {@code super.doAfterCompose} dibiarkan menyebar sesuai kontrak
	 * {@code throws Exception}. Null-check pada tombol upload mencegah
	 * {@code NullPointerException} jika fitur upload tidak tersedia di konfigurasi
	 * sistem saat ini.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika menambah atau menghapus kolom pada entitas {@code KategoriPenyediaAsset},
	 * perbarui array {@code contents} agar sinkron dengan field yang tersedia di entitas.
	 * Setiap elemen array harus berkorespondensi dengan nama getter tanpa prefix "get"
	 * (huruf pertama lowercase) pada entitas, agar sistem ekspor/impor dapat memetakan
	 * kolom dengan benar.</p>
	 *
	 * @param comp komponen root ZUL yang telah selesai dirender dan di-wire oleh ZK
	 * @throws Exception jika terjadi kesalahan pada inisialisasi komponen kelas induk
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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KategoriPenyediaAsset.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KategoriPenyediaAsset.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <h3>KategoriPenyediaAssetRenderer &mdash; Renderer Baris Grid Kategori Penyedia Aset</h3>
	 *
	 * <p><b>Untuk apa:</b><br>
	 * Inner class renderer yang mengisi setiap baris grid dengan data entitas
	 * {@code KategoriPenyediaAsset}. Menampilkan informasi lengkap kategori penyedia
	 * meliputi kode, nama dengan dukungan RevisiHelper untuk jejak audit perubahan,
	 * keterangan, dan checkbox "Aktif" yang dapat diubah langsung dari grid tanpa
	 * perlu membuka form detail (inline editing). Tombol aksi Salin/Ubah/Hapus disertakan
	 * dengan visibilitas yang disesuaikan dengan hak akses pengguna yang sedang login.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Pada setiap pemanggilan {@code render(Row, Object)}, objek {@code arg1} di-cast
	 * ke {@code KategoriPenyediaAsset} dan komponen ZK ditambahkan ke baris secara
	 * berurutan sesuai urutan kolom yang didefinisikan di file ZUL:
	 * kode (Label), nama (RevisiHelper), keterangan (Label), checkbox Aktif dengan
	 * listener onCheck, dan tombol Salin/Ubah/Hapus. Checkbox dinonaktifkan jika pengguna
	 * tidak memiliki hak UPDATE, mencegah modifikasi oleh pengguna read-only.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Urutan penambahan komponen ke baris ({@code arg0}) harus selalu selaras dengan
	 * jumlah dan urutan {@code <column>} yang didefinisikan di file ZUL terkait.
	 * Jika menambah kolom baru, tambahkan komponen baru sebelum pemanggilan
	 * {@code copyEditDeleteButtons} dan tambahkan definisi kolom yang sesuai di ZUL.</p>
	 */
	class KategoriPenyediaAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render &mdash; Mengisi Satu Baris Grid dengan Data Kategori Penyedia Aset</h3>
		 *
		 * <p><b>Tujuan:</b><br>
		 * Metode inti renderer yang dipanggil oleh ZK untuk setiap entitas
		 * {@code KategoriPenyediaAsset} dalam model grid. Mengisi baris dengan label data
		 * dan komponen interaktif (checkbox aktif dan tombol aksi) yang sesuai hak akses.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Cast {@code arg1} ke {@code KategoriPenyediaAsset} menggunakan final variable
		 * agar dapat diakses dari dalam anonymous EventListener. Kemudian secara berurutan
		 * menambahkan komponen: Label kode, komponen nama dari RevisiHelper (mendukung
		 * klik untuk melihat riwayat perubahan), Label keterangan, MyCheckboxConfig Aktif
		 * dengan listener onCheck yang langsung memanggil {@code Common.refreshSaveOrUpdate}
		 * untuk menyimpan perubahan status ke database, dan akhirnya tombol Salin/Ubah/Hapus.
		 * Checkbox juga disimpan sebagai atribut baris dengan kunci "checkbox" untuk
		 * kemungkinan akses dari kode luar renderer.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Exception dari operasi simpan inline atau pembuatan komponen ZK dibiarkan
		 * menyebar ke framework ZK untuk ditangani di level yang lebih tinggi.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Jika perlu menambahkan konfirmasi sebelum toggle aktif (misalnya jika ada vendor
		 * aktif yang menggunakan kategori ini), tambahkan dialog konfirmasi di dalam listener
		 * {@code onCheck} sebelum pemanggilan {@code Common.refreshSaveOrUpdate}.</p>
		 *
		 * @param arg0 baris ZK {@code Row} yang akan diisi dengan komponen data
		 * @param arg1 objek data {@code KategoriPenyediaAsset} yang akan ditampilkan pada baris ini
		 * @throws Exception jika terjadi error saat pembuatan komponen ZK atau operasi simpan
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final KategoriPenyediaAsset kategoriPenyediaAsset = (KategoriPenyediaAsset) arg1;
			new Label(kategoriPenyediaAsset.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(KategoriPenyediaAsset.class, kategoriPenyediaAsset,
					kategoriPenyediaAsset.getNama()).setParent(arg0);
			new Label(kategoriPenyediaAsset.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kategoriPenyediaAsset.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kategoriPenyediaAsset.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kategoriPenyediaAsset);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, kategoriPenyediaAsset, KategoriPenyediaAssetAction.this)
					.setParent(arg0);

		}

	}

	/**
	 * <h3>onAdd &mdash; Membuka Form Tambah Data Kategori Penyedia Aset Baru</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Event handler yang dipicu ketika pengguna mengklik tombol "Tambah" pada toolbar
	 * halaman kategori penyedia aset. Bertanggung jawab mempersiapkan dan menampilkan
	 * form kosong yang siap diisi oleh pengguna untuk membuat kategori penyedia baru.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat instance {@code KategoriPenyediaAsset} baru yang masih kosong (ID null
	 * menandakan entitas belum persisten di database), kemudian memanggil metode
	 * {@code init(kategoriPenyediaAsset)} untuk membangun seluruh konten form di dalam
	 * popup {@code addWindow}. Setelah form siap, popup ditampilkan dalam mode modal
	 * menggunakan {@code addWindow.setVisible(true)} dan {@code addWindow.onModal()},
	 * sehingga interaksi pengguna dikunci ke dalam popup sampai ditutup.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception diteruskan ke atas melalui deklarasi {@code throws Exception} dan
	 * akan ditangani oleh ZK Framework di level yang lebih tinggi.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini tidak perlu dimodifikasi kecuali ada perubahan fundamental pada cara
	 * popup dibuka atau tipe entitas yang diinisialisasi. Untuk mengubah konten form
	 * atau field yang tersedia, modifikasi metode {@code init(KategoriPenyediaAsset)}.</p>
	 *
	 * @param event event ZK yang diterima dari interaksi klik tombol Tambah di antarmuka
	 * @throws Exception jika terjadi kesalahan saat membangun komponen form atau menampilkan popup
	 */
	public void onAdd(Event event) throws Exception {
		init(new KategoriPenyediaAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <h3>init(GeneralValueObject) &mdash; Implementasi Interface DataInitDefault untuk Impor</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi metode wajib dari interface {@code DataInitDefault} yang memungkinkan
	 * subsistem impor data massal (fitur upload Excel) untuk membuka form edit dengan
	 * data kategori penyedia yang sudah ada. Metode ini berfungsi sebagai adaptor yang
	 * menghubungkan mekanisme impor generik dengan form spesifik kategori penyedia aset.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Melakukan cast tipe parameter {@code GeneralValueObject} ke {@code KategoriPenyediaAsset}
	 * dan menyimpannya ke field instance {@code kategoriPenyediaAsset}. Kemudian mendelegasikan
	 * pembangunan form ke metode private {@code init(KategoriPenyediaAsset)}. Setelah form
	 * berhasil dibangun, menampilkan popup dalam mode modal dengan cara yang sama seperti
	 * {@code onAdd}.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * {@code ClassCastException} akan terjadi jika parameter {@code obj} bukan instance
	 * {@code KategoriPenyediaAsset}; ini merupakan error pemrograman dan dibiarkan menyebar
	 * ke atas. Exception lainnya dari komponen ZK juga dibiarkan menyebar.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini tidak perlu dimodifikasi selama tipe entitas dan interface tidak berubah.
	 * Jika perlu menambahkan pre-processing sebelum form dibuka (misalnya loading lazy
	 * associations), tambahkan sebelum pemanggilan {@code init(kategoriPenyediaAsset)}.</p>
	 *
	 * @param obj objek data yang harus dapat di-cast ke {@code KategoriPenyediaAsset};
	 *            biasanya disediakan oleh subsistem impor Excel
	 * @throws Exception jika terjadi error saat membangun komponen form atau menampilkan popup
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kategoriPenyediaAsset = (KategoriPenyediaAsset) obj;
		init(kategoriPenyediaAsset);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <h3>init(KategoriPenyediaAsset) &mdash; Membangun Ulang Konten Form Popup Kategori Penyedia</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode internal utama yang membangun ulang seluruh konten popup {@code addWindow}
	 * secara programatik setiap kali form dibuka, baik untuk skenario tambah data baru
	 * maupun ubah data yang sudah ada. Pendekatan membangun ulang (rebuild) ini dipilih
	 * untuk memastikan bahwa form selalu bersih dari nilai lama atau state tersisa dari
	 * sesi edit sebelumnya, menghindari potensi kebocoran data antar sesi editing.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menyimpan referensi entitas {@code kategoriPenyediaAsset} ke field instance
	 *       untuk digunakan oleh {@code onSave} saat tombol Simpan diklik.</li>
	 *   <li>Mengatur judul popup: "Tambah Kategori Penyedia" jika ID entitas null
	 *       (data baru), atau "Ubah Kategori Penyedia" jika ID tidak null (data lama).</li>
	 *   <li>Membersihkan seluruh konten popup lama menggunakan {@code Common.clear(addWindow)}.</li>
	 *   <li>Membangun layout baru: {@code MyBorderlayout} sebagai root layout popup, dengan
	 *       {@code Center} yang berisi {@code MyGrid} grid form dua kolom (lebar 30% untuk
	 *       label dan sisa lebar untuk komponen input), dan {@code South} yang berisi
	 *       toolbar dengan tombol aksi.</li>
	 *   <li>Menambahkan baris-baris form ke dalam grid:
	 *       <ul>
	 *         <li>Baris 1: Label "Kode Kategori Penyedia" dan {@code Textbox} kode
	 *             (opsional, diisi dari {@code kategoriPenyediaAsset.getKode()}).</li>
	 *         <li>Baris 2: Label "Nama Kategori Penyedia" dan {@code Textbox} nama
	 *             (wajib diisi, diisi dari {@code getNama()}).</li>
	 *         <li>Baris 3: Label "Keterangan" dan {@code Textbox} keterangan multiline
	 *             dengan 3 baris (diisi dari {@code getKeterangan()}).</li>
	 *       </ul>
	 *   </li>
	 *   <li>Menambahkan toolbar di South dengan tombol Batal (menyembunyikan popup tanpa
	 *       menyimpan) dan tombol Simpan (memanggil {@code onSave}, lalu menyembunyikan
	 *       popup jika {@code onSave} mengembalikan true).</li>
	 *   <li>Memasang {@code borderlayout} ke dalam {@code addWindow}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Metode ini tidak melempar exception secara eksplisit. Error dari komponen ZK
	 * dibiarkan menyebar ke caller ({@code onAdd} atau {@code init(GeneralValueObject)}).
	 * Nilai null pada field entitas (kode, nama, keterangan) diangani oleh konstruktor
	 * {@code Textbox} yang dapat menerima nilai null.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Untuk menambah field baru, tambahkan field {@code Textbox} atau komponen input
	 * lain di level kelas, buat baris form baru di dalam {@code Rows} sebelum blok
	 * {@code South}, dan perbarui metode {@code onSave} untuk membaca dan menetapkan
	 * nilai field tersebut ke entitas. Jika perlu mengubah persentase lebar kolom label,
	 * modifikasi {@code column.setWidth("30%")} menjadi persentase yang diinginkan.</p>
	 *
	 * @param kategoriPenyediaAsset entitas kategori penyedia yang akan ditampilkan di form;
	 *                              jika ID-nya {@code null}, form digunakan untuk tambah data baru;
	 *                              jika ID tidak {@code null}, form digunakan untuk ubah data yang ada
	 */
	private void init(KategoriPenyediaAsset kategoriPenyediaAsset) {
		this.kategoriPenyediaAsset = kategoriPenyediaAsset;
		addWindow.setTitle(kategoriPenyediaAsset.getId() == null ? "Tambah Kategori Penyedia" : "Ubah Kategori Penyedia");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Kategori Penyedia"));
		row.appendChild(kode = new Textbox(kategoriPenyediaAsset.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kategori Penyedia"));
		row.appendChild(nama = new Textbox(kategoriPenyediaAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kategoriPenyediaAsset.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setSclass("ais-btn ais-btn-merah");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setSclass("ais-btn ais-btn-hijau");
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
	 * <h3>onSave &mdash; Memvalidasi dan Menyimpan Data Kategori Penyedia Aset ke Database</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode yang dipanggil saat pengguna mengklik tombol "Simpan" pada form popup
	 * tambah/ubah kategori penyedia. Bertanggung jawab untuk memvalidasi seluruh input
	 * yang diberikan pengguna, memeriksa keunikan nama di database untuk mencegah
	 * duplikasi data master, dan akhirnya menyimpan perubahan ke database baik sebagai
	 * operasi insert (data baru) maupun update (data yang sudah ada).</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memeriksa apakah nilai {@code nama} textbox kosong setelah di-trim;
	 *       jika kosong, menampilkan dialog peringatan dengan ikon INFORMATION dan
	 *       mengembalikan {@code false} untuk membatalkan penyimpanan.</li>
	 *   <li>Memanggil {@code checkNamaKategoriPenyediaAsset()} untuk memeriksa apakah
	 *       nama yang diinput sudah ada di database pada entitas lain; jika duplikat,
	 *       menampilkan dialog peringatan dan mengembalikan {@code false}.</li>
	 *   <li>Mengambil sesi Hibernate saat ini. Jika entitas sedang dalam mode ubah
	 *       (ID tidak null), me-reload entitas dari database menggunakan {@code session.load()}
	 *       untuk memastikan entitas berada dalam managed state Hibernate sebelum perubahan
	 *       diterapkan, menghindari masalah detached entity.</li>
	 *   <li>Menetapkan nilai kode, nama, dan keterangan dari input form ke entitas.</li>
	 *   <li>Memanggil {@code Common.refreshSaveOrUpdate(session, kategoriPenyediaAsset)}
	 *       untuk melakukan persist (saveOrUpdate) dan flush ke database.</li>
	 *   <li>Mengembalikan {@code true} untuk menandakan bahwa penyimpanan berhasil.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Validasi input (nama kosong, nama duplikat) ditangani dengan dialog pesan ZK yang
	 * informatif kepada pengguna. Error database yang tidak terduga (constraint violation,
	 * koneksi terputus, dll) dibiarkan menyebar ke atas melalui {@code throws Exception}
	 * dan akan ditangani oleh ZK Framework atau blok try-catch di level atas.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika menambah field wajib baru ke entitas, tambahkan validasi kosong di awal metode
	 * ini sebelum blok {@code checkNamaKategoriPenyediaAsset()}. Untuk field opsional,
	 * cukup tambahkan setter setelah blok reload entitas tanpa menambahkan validasi wajib.
	 * Jika aturan bisnis duplikasi berubah, modifikasi {@code checkNamaKategoriPenyediaAsset()}.</p>
	 *
	 * @param event event ZK yang diterima dari klik tombol Simpan pada form popup
	 * @return {@code true} jika semua validasi lolos dan data berhasil disimpan ke database;
	 *         {@code false} jika ada validasi yang gagal (nama kosong atau nama duplikat)
	 * @throws Exception jika terjadi kesalahan pada operasi database atau komponen ZK
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Kategori Penyedia belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama kategori penyedia; (2) Kategori digunakan untuk mengelompokkan vendor/kontraktor berdasarkan jenisnya; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaKategoriPenyediaAsset();
		if (i) {
			MyMessageboxConfig.show("Nama Kategori Penyedia sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kategoriPenyediaAsset.getId() != null) {
			kategoriPenyediaAsset = (KategoriPenyediaAsset) session.load(KategoriPenyediaAsset.class,
					kategoriPenyediaAsset.getId());

		}

		kategoriPenyediaAsset.setKode(kode.getValue());
		kategoriPenyediaAsset.setNama(nama.getValue());
		kategoriPenyediaAsset.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, kategoriPenyediaAsset);

		return true;
	}

	/**
	 * <h3>initCriteria &mdash; Membangun Kriteria Query Hibernate untuk Pencarian Kategori Penyedia</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Membangun dan mengembalikan objek {@code Criteria} Hibernate yang berisi seluruh
	 * kondisi filter yang aktif untuk pencarian data kategori penyedia aset. Metode ini
	 * merupakan implementasi interface {@code DataCriteria} yang digunakan oleh berbagai
	 * konsumen: {@code onSearchDefault} untuk mengambil data paged, sistem paging untuk
	 * menghitung total baris, serta fitur cetak/ekspor untuk mengambil semua data.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mengambil sesi Hibernate saat ini menggunakan {@code HibernateUtil.currentSession()},
	 * membuat kriteria untuk entitas {@code KategoriPenyediaAsset}, lalu menambahkan
	 * kondisi filter secara berurutan:
	 * <ul>
	 *   <li><b>Filter status aktif:</b> Ditetapkan secara kondisional menggunakan operator
	 *       ternary. Jika komponen {@code searchaktif} bernilai null (belum diinisialisasi)
	 *       atau sedang dalam keadaan dicentang (checked), menerapkan filter OR yang mencakup
	 *       baris dengan nilai aktif null ({@code isNull("aktif")}) atau aktif true
	 *       ({@code eq("aktif", true)}). Jika tidak dicentang, menerapkan
	 *       {@code sqlRestriction("true")} yang secara efektif menonaktifkan filter ini
	 *       dan menampilkan semua baris termasuk yang nonaktif.</li>
	 *   <li><b>Filter nama:</b> Jika nilai {@code searchnama} tidak kosong setelah di-trim,
	 *       menerapkan {@code Restrictions.ilike("nama", ..., MatchMode.ANYWHERE)} untuk
	 *       pencarian case-insensitive yang mencari teks di manapun dalam nama. Jika kosong,
	 *       menerapkan {@code sqlRestriction("true")} untuk menampilkan semua nama.</li>
	 *   <li><b>Pengurutan:</b> Jika parameter {@code order} bernilai true, menambahkan
	 *       {@code Order.asc("nama")} untuk pengurutan alfabetis A-Z berdasarkan nama.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari operasi Hibernate (seperti sesi tidak valid atau koneksi database
	 * terputus) dibiarkan menyebar ke caller tanpa penanganan khusus.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Untuk menambah filter baru (misalnya filter berdasarkan kode), tambahkan
	 * {@code criteria.add(Restrictions...)} baru setelah kondisi filter yang sudah ada.
	 * Pastikan komponen ZUL yang sesuai (misalnya Textbox searchkode) juga ditambahkan
	 * ke halaman agar pengguna dapat berinteraksi dengan filter baru tersebut.</p>
	 *
	 * @param order jika {@code true}, hasil query akan diurutkan berdasarkan nama secara
	 *              ascending A-Z; jika {@code false}, tidak ada pengurutan yang ditambahkan
	 *              (lebih efisien untuk query COUNT yang digunakan oleh komponen paging)
	 * @return objek {@code Criteria} Hibernate yang sudah dikonfigurasi dengan semua
	 *         kondisi filter aktif dan siap untuk dieksekusi oleh caller
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KategoriPenyediaAsset.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * <h3>onSearchDefault &mdash; Memuat Ulang Data Grid Kategori Penyedia dengan Filter Aktif</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi interface {@code DataSearchDefault} yang memuat ulang dan menyegarkan
	 * tampilan grid data kategori penyedia aset berdasarkan kondisi filter yang aktif saat
	 * ini. Metode ini menjadi titik pusat pembaruan tampilan data yang dipanggil dari
	 * berbagai konteks: inisialisasi halaman pertama kali, perubahan paging, dan setelah
	 * operasi CRUD (tambah, ubah, hapus) berhasil dilakukan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Pertama-tama memperbarui komponen {@code paging} dengan total baris yang sesuai
	 * filter aktif menggunakan {@code Common.initPaging(initCriteria(false), paging)},
	 * di mana {@code initCriteria(false)} menghasilkan query tanpa pengurutan (lebih
	 * efisien untuk operasi COUNT). Kemudian mengambil daftar data halaman aktif
	 * menggunakan {@code initCriteria(true)} dengan batas baris {@code Common.ROWS_COUNT_ON_PAGE}
	 * dan offset yang dihitung dari nomor halaman aktif pada komponen paging. Hasil daftar
	 * dibungkus dalam {@code SimpleListModel} dan ditetapkan ke grid bersama dengan
	 * instance {@code KategoriPenyediaAssetRenderer} yang baru dibuat.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Error Hibernate dari eksekusi query dibiarkan menyebar ke atas. Anotasi
	 * {@code @SuppressWarnings("unchecked")} diperlukan untuk menangani casting generik
	 * yang tidak aman dari hasil {@code .list()} Hibernate Criteria.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Konstanta {@code Common.ROWS_COUNT_ON_PAGE} mengontrol jumlah baris per halaman
	 * secara global untuk seluruh aplikasi. Jika perlu menambah pengurutan sekunder
	 * (misalnya diurutkan berdasarkan kode jika nama sama), tambahkan {@code Order.asc("kode")}
	 * pada {@code initCriteria} setelah {@code Order.asc("nama")}.</p>
	 *
	 * @param event event ZK yang memicu pencarian; dapat bernilai {@code null} jika
	 *              metode dipanggil secara programatik (bukan dari interaksi pengguna langsung)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KategoriPenyediaAsset> kategoriPenyediaAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kategoriPenyediaAsset);
		grid.setRowRenderer(new KategoriPenyediaAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <h3>checkNamaKategoriPenyediaAsset &mdash; Memeriksa Duplikasi Nama Kategori Penyedia</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memverifikasi keunikan nama kategori penyedia aset yang diinput oleh pengguna
	 * dengan cara memeriksa apakah nama tersebut sudah ada di database. Pemeriksaan
	 * ini dilakukan sebelum operasi penyimpanan untuk mencegah duplikasi data master
	 * kategori penyedia yang dapat menyebabkan ambiguitas pada proses klasifikasi vendor.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mengambil sesi Hibernate saat ini dan membuat query Criteria untuk menghitung
	 * jumlah baris pada tabel {@code KategoriPenyediaAsset} yang memiliki nilai nama
	 * yang sama persis (sesuai collation database, biasanya case-insensitive pada
	 * PostgreSQL dengan collation default) dengan nilai pada komponen {@code nama} textbox
	 * setelah di-trim. Jika entitas sedang dalam mode ubah (field {@code kategoriPenyediaAsset}
	 * sudah memiliki ID yang tidak null), menambahkan kondisi eksklusif
	 * {@code Restrictions.ne("id", this.kategoriPenyediaAsset.getId())} untuk mengecualikan
	 * entitas yang sedang diedit dari penghitungan, sehingga pengguna dapat menyimpan
	 * perubahan lain tanpa harus mengubah nama. Untuk mode tambah (ID null), menggunakan
	 * {@code sqlRestriction("1=1")} yang tidak mengecualikan baris apapun.
	 * Mengembalikan {@code true} jika jumlah baris yang ditemukan lebih dari nol.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Hasil {@code uniqueResult()} dari proyeksi rowCount di-cast ke {@code Number}
	 * (bukan langsung ke Integer) untuk memastikan kompatibilitas lintas implementasi
	 * database yang berbeda (PostgreSQL, MySQL, dll). Exception Hibernate dibiarkan
	 * menyebar ke caller tanpa penanganan eksplisit.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pertimbangkan untuk menambahkan UNIQUE INDEX pada kolom nama di level database
	 * sebagai lapisan keamanan tambahan terhadap race condition pada operasi insert
	 * bersamaan dari dua pengguna yang mengakses halaman ini secara bersamaan.
	 * Jika aturan bisnis keunikan berubah (misalnya nama boleh duplikat selama kode
	 * berbeda), modifikasi kondisi pada metode ini sesuai dengan aturan bisnis yang baru.</p>
	 *
	 * @return {@code true} jika nama yang diinput oleh pengguna sudah ada di database
	 *         pada entitas kategori penyedia lain (bukan entitas yang sedang diedit);
	 *         {@code false} jika nama tersebut unik atau hanya dimiliki oleh entitas
	 *         yang sedang diedit (sehingga penyimpanan boleh dilanjutkan)
	 */
	public Boolean checkNamaKategoriPenyediaAsset() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KategoriPenyediaAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.kategoriPenyediaAsset.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kategoriPenyediaAsset.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
