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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.JenisPekerjaanPenyedia;
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
 * <h3>JenisPekerjaanPenyediaAction &mdash; Controller CRUD Jenis Pekerjaan Penyedia</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah controller ZK (composer) untuk halaman manajemen data master
 * <em>Jenis Pekerjaan Penyedia</em>, yaitu daftar klasifikasi jenis pekerjaan yang
 * dapat dilakukan oleh vendor atau penyedia barang dan jasa dalam lingkup pengadaan
 * aset institusi. Contoh jenis pekerjaan meliputi: konstruksi bangunan, instalasi listrik,
 * pengadaan alat tulis kantor, layanan teknologi informasi, perawatan kendaraan, dan
 * sebagainya. Data master ini menjadi referensi wajib saat mendaftarkan vendor baru,
 * sehingga sistem dapat memverifikasi kesesuaian antara jenis pekerjaan yang ditawarkan
 * vendor dengan kebutuhan pengadaan aset institusi. Setiap jenis pekerjaan memiliki kode
 * identifikasi dan flag aktif/nonaktif, memungkinkan manajemen siklus hidup klasifikasi
 * tanpa menghapus historis data vendor yang sudah terdaftar. Halaman ini mendukung
 * operasi tambah, ubah, nonaktifkan, ekspor Excel, dan impor data massal.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Controller mewarisi {@code GenericAutowireComposer} ZK dan mengimplementasikan
 * {@code DataCriteria}, {@code DataSearchDefault}, dan {@code DataInitDefault}.
 * Perbedaan utama dari controller master aset lain adalah penggunaan
 * {@code ConstantValues.simpleList(...)} pada {@code onSearchDefault} untuk mengambil
 * daftar data, yang mungkin menerapkan logika tambahan seperti eager-load asosiasi
 * atau transformasi khusus dibandingkan panggilan {@code .list()} biasa.
 * Alur kerja utama:
 * <ol>
 *   <li>{@code doBeforeCompose} memverifikasi keamanan sesi.</li>
 *   <li>{@code doAfterCompose} menginisialisasi hak akses, memuat data awal, menyiapkan
 *       paging, dan mendaftarkan tombol cetak (kolom: id, kode, nama, keterangan, aktif)
 *       serta tombol upload impor ke toolbar.</li>
 *   <li>{@code onSearchDefault} mengambil data dengan filter nama dan status aktif
 *       menggunakan {@code ConstantValues.simpleList}.</li>
 *   <li>{@code JenisPekerjaanPenyediaRenderer} merender setiap baris dengan kode, nama,
 *       keterangan, checkbox aktif (inline edit), dan tombol aksi.</li>
 *   <li>Form popup dibangun oleh {@code init(JenisPekerjaanPenyedia)} dengan field kode,
 *       nama (wajib), dan keterangan.</li>
 *   <li>{@code onSave} memvalidasi duplikasi nama dan menyimpan data.</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh operasi dijalankan pada thread ZK event (UI thread). Sesi Hibernate dari
 * {@code HibernateUtil.currentSession()} terikat pada thread HTTP request aktif.
 * Perubahan flag aktif melalui checkbox inline dijalankan sinkron pada UI thread.
 * Tidak ada kebutuhan sinkronisasi tambahan.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Jika menambah kolom baru ke entitas {@code JenisPekerjaanPenyedia}, perbarui array
 * {@code contents} di {@code doAfterCompose} dan tambahkan baris form di {@code init}.
 * Jika perlu mengganti {@code ConstantValues.simpleList} dengan {@code .list()} biasa,
 * pastikan tidak ada eager-load atau transformasi yang hilang. Selalu uji dengan
 * berbagai kombinasi hak akses untuk memastikan visibilitas tombol benar.</p>
 *
 * @author Tim Pengembang AIS
 * @version 1.0
 * @see JenisPekerjaanPenyedia
 * @see ConstantValues
 * @see DataCriteria
 * @see DataSearchDefault
 * @see DataInitDefault
 */
public class JenisPekerjaanPenyediaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal popup untuk form tambah/ubah data jenis pekerjaan penyedia. */
	private MyWindow addWindow;

	/** Komponen paging ZK untuk navigasi halaman pada grid data. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar jenis pekerjaan penyedia aset. */
	private MyGrid grid;

	/** Kotak teks filter pencarian berdasarkan nama jenis pekerjaan. */
	private Textbox searchnama;

	/**
	 * Checkbox filter status aktif; jika dicentang, hanya menampilkan jenis pekerjaan
	 * yang aktif atau yang belum ditetapkan statusnya; jika tidak dicentang, tampilkan semua.
	 */
	private Checkbox searchaktif;

	/** Kotak teks input nama jenis pekerjaan pada form tambah/ubah. */
	private Textbox nama;

	/** Kotak teks input keterangan jenis pekerjaan. */
	private Textbox keterangan;

	/** Flag yang menandakan apakah pengguna memiliki hak ubah data. */
	private boolean edit = false;

	/** Flag yang menandakan apakah pengguna memiliki hak hapus data. */
	private boolean delete = false;

	/** Entitas jenis pekerjaan penyedia yang sedang aktif diedit atau ditambahkan. */
	private JenisPekerjaanPenyedia jenisPekerjaanPenyedia;

	/** Tombol toolbar untuk menambah data jenis pekerjaan penyedia baru. */
	private MyToolbarbuttonConfig add;

	/** Kotak teks input kode unik jenis pekerjaan penyedia. */
	private Textbox kode;

	/**
	 * <h3>doBeforeCompose &mdash; Verifikasi Keamanan Sebelum Render Halaman</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memastikan keamanan akses halaman manajemen jenis pekerjaan penyedia sebelum
	 * komponen ZK dirender. Dipanggil oleh ZK Framework sebelum proses pembuatan
	 * komponen dimulai.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi bahwa pengguna
	 * memiliki sesi aktif yang valid. Pengguna tanpa sesi valid diarahkan ke halaman
	 * login. Selanjutnya memanggil {@code super.doBeforeCompose} untuk melanjutkan
	 * proses lifecycle ZK dan mengembalikan hasilnya ke framework.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Sesi tidak valid ditangani oleh redirect internal di {@code doCheckSecurity}.
	 * Tidak ada exception yang dilempar secara eksplisit dari metode ini.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pertahankan pemanggilan {@code doCheckSecurity} sebagai lapisan keamanan wajib
	 * pertama. Selalu pastikan {@code super.doBeforeCompose} dipanggil agar lifecycle
	 * ZK berjalan dengan benar.</p>
	 *
	 * @param page     halaman ZK yang sedang diproses
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo metadata komponen yang sedang dikomposisi
	 * @return {@code ComponentInfo} dari kelas induk untuk melanjutkan proses ZK
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <h3>doAfterCompose &mdash; Inisialisasi Lengkap Halaman Jenis Pekerjaan Penyedia</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode lifecycle ZK yang dipanggil setelah seluruh komponen halaman selesai di-wire.
	 * Melakukan inisialisasi menyeluruh: pengaturan hak akses tombol, pemuatan data grid
	 * pertama kali, penyiapan paging, serta pendaftaran tombol cetak ekspor Excel dan
	 * tombol upload impor data massal ke toolbar.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} dan {@code Common.initLaguage()}.</li>
	 *   <li>Mengatur visibilitas tombol Tambah berdasarkan hak CREATE pengguna.</li>
	 *   <li>Menetapkan flag {@code edit} dan {@code delete}.</li>
	 *   <li>Memuat data awal dengan {@code onSearchDefault(null)}.</li>
	 *   <li>Mendaftarkan listener paging untuk reload grid saat halaman berubah.</li>
	 *   <li>Mendaftarkan tombol cetak (kolom: id, kode, nama, keterangan, aktif) dan
	 *       tombol upload impor dengan visibilitas yang bergantung pada hak CREATE+UPDATE+DELETE.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari {@code super.doAfterCompose} dibiarkan menyebar.
	 * Upload null-check mencegah NullPointerException.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Perbarui array {@code contents} jika menambah atau menghapus kolom pada entitas
	 * {@code JenisPekerjaanPenyedia}. Kolom pada array harus berkorespondensi dengan
	 * nama getter pada entitas (tanpa prefix "get").</p>
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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisPekerjaanPenyedia.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisPekerjaanPenyedia.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <h3>JenisPekerjaanPenyediaRenderer &mdash; Renderer Baris Grid Jenis Pekerjaan Penyedia</h3>
	 *
	 * <p><b>Untuk apa:</b><br>
	 * Inner class renderer yang mengisi setiap baris grid dengan data entitas
	 * {@code JenisPekerjaanPenyedia}. Menampilkan kode, nama, keterangan, dan
	 * checkbox "Aktif" yang dapat diubah langsung dari grid (inline editing),
	 * serta tombol aksi Salin/Ubah/Hapus yang disesuaikan dengan hak akses pengguna.
	 * Pendekatan inline editing untuk status aktif memberikan kemudahan bagi operator
	 * dalam mengelola siklus hidup jenis pekerjaan tanpa perlu membuka form detail.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Setiap baris menampilkan: kode (label), nama dengan RevisiHelper, keterangan (label),
	 * checkbox Aktif dengan listener {@code onCheck} yang menyimpan perubahan langsung ke
	 * database melalui {@code Common.refreshSaveOrUpdate}, dan tombol Salin/Ubah/Hapus.
	 * Checkbox dinonaktifkan jika pengguna tidak memiliki hak UPDATE.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Urutan komponen harus sesuai dengan definisi kolom di file ZUL. Jika menambah kolom
	 * baru, tambahkan komponen sebelum pemanggilan {@code copyEditDeleteButtons}.</p>
	 */
	class JenisPekerjaanPenyediaRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render &mdash; Mengisi Satu Baris Grid dengan Data Jenis Pekerjaan Penyedia</h3>
		 *
		 * <p><b>Tujuan:</b><br>
		 * Metode inti renderer yang mengisi baris ZK dengan data satu entitas
		 * {@code JenisPekerjaanPenyedia}, menampilkan kode, nama, keterangan, checkbox
		 * aktif yang dapat diubah langsung, dan tombol aksi standar.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Cast {@code arg1} ke {@code JenisPekerjaanPenyedia}, lalu menambahkan secara
		 * berurutan: label kode, label nama dengan RevisiHelper (mendukung audit jejak
		 * perubahan), label keterangan, checkbox Aktif dengan listener onCheck (menyimpan
		 * langsung via {@code Common.refreshSaveOrUpdate}), dan tombol Salin/Ubah/Hapus
		 * melalui {@code Common.copyEditDeleteButtons} yang mengacu ke controller induk
		 * untuk callback edit dan hapus.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Exception dari komponen ZK atau operasi simpan inline dibiarkan menyebar ke ZK.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Jika perlu menambah validasi saat toggle aktif (misalnya cek ketergantungan data),
		 * tambahkan logika sebelum {@code Common.refreshSaveOrUpdate} di listener onCheck.</p>
		 *
		 * @param arg0 baris ZK {@code Row} yang akan diisi konten
		 * @param arg1 objek data {@code JenisPekerjaanPenyedia} untuk baris ini
		 * @throws Exception jika terjadi error saat pembuatan komponen ZK atau operasi simpan
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisPekerjaanPenyedia jenisPekerjaanPenyedia = (JenisPekerjaanPenyedia) arg1;
			new Label(jenisPekerjaanPenyedia.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(JenisPekerjaanPenyedia.class, jenisPekerjaanPenyedia,
					jenisPekerjaanPenyedia.getNama()).setParent(arg0);
			new Label(jenisPekerjaanPenyedia.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisPekerjaanPenyedia.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPekerjaanPenyedia.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisPekerjaanPenyedia);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisPekerjaanPenyedia, JenisPekerjaanPenyediaAction.this)
					.setParent(arg0);

		}

	}

	/**
	 * <h3>onAdd &mdash; Membuka Form Tambah Data Jenis Pekerjaan Penyedia Baru</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Event handler yang dipicu saat pengguna mengklik tombol "Tambah" di toolbar.
	 * Mempersiapkan dan menampilkan form kosong untuk penginputan jenis pekerjaan
	 * penyedia baru yang belum ada di database.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat instance {@code JenisPekerjaanPenyedia} baru dengan ID null (menandakan
	 * data belum ada di database), memanggil {@code init(jenisPekerjaanPenyedia)} untuk
	 * membangun konten form di dalam popup {@code addWindow}, kemudian menampilkan popup
	 * dalam mode modal dengan {@code setVisible(true)} dan {@code onModal()}.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception diteruskan ke ZK framework melalui deklarasi {@code throws Exception}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak perlu dimodifikasi kecuali perilaku pembukaan popup berubah secara mendasar.
	 * Untuk mengubah konten form, modifikasi metode {@code init(JenisPekerjaanPenyedia)}.</p>
	 *
	 * @param event event ZK dari klik tombol Tambah di toolbar
	 * @throws Exception jika terjadi error saat membangun atau menampilkan form popup
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisPekerjaanPenyedia());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <h3>init(GeneralValueObject) &mdash; Implementasi Interface DataInitDefault untuk Impor</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi metode interface {@code DataInitDefault} yang memungkinkan sistem
	 * impor data massal (upload Excel) untuk membuka form edit dengan data jenis pekerjaan
	 * tertentu. Berfungsi sebagai jembatan adaptor antara sistem impor generik dan form
	 * spesifik jenis pekerjaan penyedia ini.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Melakukan cast parameter {@code GeneralValueObject} ke {@code JenisPekerjaanPenyedia},
	 * menyimpan ke field instance {@code jenisPekerjaanPenyedia}, lalu mendelegasikan ke
	 * metode {@code init(JenisPekerjaanPenyedia)} untuk membangun tampilan form popup.
	 * Setelah form siap, menampilkan popup dalam mode modal.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * {@code ClassCastException} terjadi jika {@code obj} bukan instance yang benar;
	 * ini dianggap error pemrograman dan dibiarkan menyebar ke atas. Exception lainnya
	 * dari komponen ZK juga dibiarkan menyebar.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak perlu dimodifikasi kecuali tipe parameter interface atau tipe entitas berubah.</p>
	 *
	 * @param obj objek data yang harus dapat di-cast ke {@code JenisPekerjaanPenyedia}
	 * @throws Exception jika terjadi error saat membangun atau menampilkan form popup
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisPekerjaanPenyedia = (JenisPekerjaanPenyedia) obj;
		init(jenisPekerjaanPenyedia);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <h3>init(JenisPekerjaanPenyedia) &mdash; Membangun Ulang Konten Form Popup Jenis Pekerjaan</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode internal yang membangun ulang seluruh konten popup {@code addWindow} secara
	 * programatik setiap kali form dibuka, baik untuk menambah data baru maupun mengubah
	 * data yang sudah ada. Pendekatan rebuild memastikan tidak ada state lama yang
	 * tertinggal dari sesi edit sebelumnya.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menyimpan referensi entitas ke field instance dan mengatur judul popup.</li>
	 *   <li>Membersihkan konten popup lama dengan {@code Common.clear(addWindow)}.</li>
	 *   <li>Membangun layout baru: {@code MyBorderlayout} dengan Center berisi grid form
	 *       dua kolom (30%/sisa) dan South berisi toolbar tombol aksi.</li>
	 *   <li>Baris form: Kode (opsional, input bebas), Nama (wajib, ditandai dengan *),
	 *       dan Keterangan (multiline, 3 baris).</li>
	 *   <li>Toolbar South berisi tombol Batal (sembunyikan popup tanpa simpan) dan Simpan
	 *       (panggil {@code onSave}, tutup popup jika berhasil).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari komponen ZK menyebar ke caller. Nilai null pada entitas (kode, nama,
	 * keterangan) ditangani oleh konstruktor {@code Textbox} yang menerima null sebagai
	 * string kosong.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Untuk menambah field baru, tambahkan baris form baru dan field Textbox terkait di
	 * kelas induk, lalu perbarui {@code onSave} untuk menetapkan nilai field tersebut.</p>
	 *
	 * @param jenisPekerjaanPenyedia entitas yang akan ditampilkan; ID null = tambah baru,
	 *                               ID tidak null = ubah data yang sudah ada
	 */
	private void init(JenisPekerjaanPenyedia jenisPekerjaanPenyedia) {
		this.jenisPekerjaanPenyedia = jenisPekerjaanPenyedia;
		addWindow.setTitle(jenisPekerjaanPenyedia.getId() == null ? "Tambah Jenis Pekerjaan Penyedia" : "Ubah Jenis Pekerjaan Penyedia");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Jenis Pekerjaan"));
		row.appendChild(kode = new Textbox(jenisPekerjaanPenyedia.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Pekerjaan *"));
		row.appendChild(nama = new Textbox(jenisPekerjaanPenyedia.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisPekerjaanPenyedia.getKeterangan()));
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
	 * <h3>onSave &mdash; Memvalidasi dan Menyimpan Data Jenis Pekerjaan Penyedia</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memvalidasi seluruh input form dan menyimpan data jenis pekerjaan penyedia ke
	 * database. Memastikan nama tidak kosong dan tidak duplikat, karena nama jenis
	 * pekerjaan digunakan sebagai label klasifikasi yang harus unik untuk menghindari
	 * ambiguitas dalam pemetaan vendor ke kategori pekerjaan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memvalidasi nama tidak boleh kosong; tampilkan dialog peringatan INFORMATION
	 *       dan kembalikan {@code false} jika kosong.</li>
	 *   <li>Memanggil {@code checkNamaJenisPekerjaanPenyedia()} untuk memeriksa duplikasi
	 *       nama di database; jika duplikat, tampilkan peringatan dan kembalikan {@code false}.</li>
	 *   <li>Jika mode ubah (ID tidak null), reload entitas dari sesi Hibernate menggunakan
	 *       {@code session.load()} agar berada dalam managed state sebelum perubahan diterapkan.</li>
	 *   <li>Set kode, nama, dan keterangan dari input form ke entitas.</li>
	 *   <li>Simpan ke database dengan {@code Common.refreshSaveOrUpdate} dan kembalikan
	 *       {@code true} jika berhasil.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Validasi kosong dan duplikasi ditangani dengan dialog pesan ZK. Error database
	 * tidak terduga menyebar melalui {@code throws Exception}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika menambah field wajib baru, tambahkan validasi di awal metode ini sebelum
	 * blok {@code checkNamaJenisPekerjaanPenyedia()}. Jika field baru bersifat opsional,
	 * cukup tambahkan setter setelah blok reload entitas.</p>
	 *
	 * @param event event ZK dari klik tombol Simpan di toolbar form popup
	 * @return {@code true} jika validasi lolos dan data berhasil disimpan;
	 *         {@code false} jika ada validasi yang gagal
	 * @throws Exception jika terjadi kesalahan pada operasi database
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Jenis Pekerjaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama jenis pekerjaan yang sesuai; (2) Jenis pekerjaan digunakan untuk menentukan kompetensi penyedia; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaJenisPekerjaanPenyedia();
		if (i) {
			MyMessageboxConfig.show("Nama Jenis Pekerjaan sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisPekerjaanPenyedia.getId() != null) {
			jenisPekerjaanPenyedia = (JenisPekerjaanPenyedia) session.load(JenisPekerjaanPenyedia.class,
					jenisPekerjaanPenyedia.getId());

		}

		jenisPekerjaanPenyedia.setKode(kode.getValue());
		jenisPekerjaanPenyedia.setNama(nama.getValue());
		jenisPekerjaanPenyedia.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisPekerjaanPenyedia);

		return true;
	}

	/**
	 * <h3>initCriteria &mdash; Membangun Kriteria Query untuk Pencarian Jenis Pekerjaan Penyedia</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Membangun objek {@code Criteria} Hibernate dengan kondisi filter pencarian jenis
	 * pekerjaan penyedia, mencakup filter status aktif dan filter nama. Implementasi
	 * interface {@code DataCriteria} yang digunakan oleh {@code onSearchDefault},
	 * sistem cetak, dan ekspor data.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat kriteria untuk entitas {@code JenisPekerjaanPenyedia} dengan:
	 * <ul>
	 *   <li>Filter aktif: jika {@code searchaktif} null atau dicentang, terapkan filter
	 *       {@code aktif IS NULL OR aktif = true}; jika tidak dicentang, gunakan
	 *       {@code sqlRestriction("true")} untuk menampilkan semua termasuk nonaktif.</li>
	 *   <li>Filter nama: ilike ANYWHERE jika teks tidak kosong, atau sqlRestriction("true")
	 *       jika kosong (tampilkan semua nama).</li>
	 *   <li>Pengurutan nama A-Z dengan {@code Order.asc("nama")} jika parameter {@code order}
	 *       bernilai true (digunakan saat mengambil data untuk tampil, bukan untuk COUNT).</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception Hibernate dibiarkan menyebar ke caller. Tidak ada penanganan khusus.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Untuk menambah filter tambahan (misalnya filter berdasarkan kode), tambahkan
	 * {@code criteria.add(...)} baru. Pastikan komponen ZUL yang sesuai juga ditambahkan
	 * agar pengguna dapat berinteraksi dengan filter baru tersebut.</p>
	 *
	 * @param order jika {@code true}, hasil diurutkan nama A-Z; jika {@code false},
	 *              tidak ada pengurutan (untuk query COUNT paging yang lebih efisien)
	 * @return objek {@code Criteria} Hibernate yang siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPekerjaanPenyedia.class)
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
	 * <h3>onSearchDefault &mdash; Memuat Ulang Data Grid Jenis Pekerjaan Penyedia</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi interface {@code DataSearchDefault} yang memuat ulang grid data
	 * jenis pekerjaan penyedia berdasarkan filter aktif saat ini. Dipanggil saat halaman
	 * pertama dimuat, saat filter berubah, saat paging berganti, serta setelah operasi
	 * simpan atau hapus berhasil dilakukan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memperbarui komponen paging dengan total baris menggunakan {@code initCriteria(false)},
	 * kemudian mengambil data halaman aktif menggunakan {@code ConstantValues.simpleList}
	 * (yang mungkin menerapkan eager-load atau transformasi tambahan dibandingkan panggilan
	 * {@code .list()} biasa) dengan batas baris dan offset sesuai paging aktif.
	 * Hasil dibungkus dalam {@code SimpleListModel} dan ditetapkan ke grid dengan
	 * renderer {@code JenisPekerjaanPenyediaRenderer} yang baru dibuat.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Error Hibernate dibiarkan menyebar. {@code @SuppressWarnings("unchecked")} menangani
	 * casting generik hasil query Hibernate.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika {@code ConstantValues.simpleList} diubah atau dihapus, ganti dengan panggilan
	 * {@code initCriteria(true).setMaxResults(...).setFirstResult(...).list()} langsung.
	 * Pastikan tidak ada eager-loading yang hilang jika melakukan perubahan ini.</p>
	 *
	 * @param event event ZK pemicu pencarian; dapat null jika dipanggil secara programatik
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPekerjaanPenyedia> jenisPekerjaanPenyedia = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				JenisPekerjaanPenyedia.class);
		ListModel strset = new SimpleListModel(jenisPekerjaanPenyedia);
		grid.setRowRenderer(new JenisPekerjaanPenyediaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <h3>checkNamaJenisPekerjaanPenyedia &mdash; Memeriksa Duplikasi Nama Jenis Pekerjaan Penyedia</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memverifikasi keunikan nama jenis pekerjaan penyedia di database sebelum penyimpanan
	 * dilakukan. Duplikasi nama dapat menyebabkan ambiguitas saat pemetaan vendor ke
	 * kategori pekerjaan, sehingga validasi ini penting untuk menjaga integritas data master.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menggunakan Hibernate Criteria untuk menghitung jumlah baris pada tabel
	 * {@code JenisPekerjaanPenyedia} yang memiliki nama yang sama (tepat, sesuai collation
	 * database) dengan nilai di komponen {@code nama} textbox.
	 * Pada mode ubah (ID entitas tidak null), mengecualikan entitas yang sedang diedit
	 * dengan menambahkan kondisi {@code Restrictions.ne("id", this.jenisPekerjaanPenyedia.getId())},
	 * sehingga pengguna dapat menyimpan data dengan nama yang tidak diubah.
	 * Mengembalikan {@code true} jika count lebih dari nol (nama sudah ada).</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Hasil {@code uniqueResult()} di-cast ke {@code Number} untuk kompatibilitas lintas
	 * implementasi database yang berbeda. Exception Hibernate dibiarkan menyebar ke caller.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pertimbangkan menambahkan UNIQUE INDEX pada kolom nama di level database sebagai
	 * perlindungan tambahan terhadap race condition pada insert bersamaan dari pengguna
	 * berbeda. Jika aturan keunikan berubah (misalnya boleh duplikat nama asal kode berbeda),
	 * modifikasi kondisi pada metode ini sesuai aturan bisnis baru.</p>
	 *
	 * @return {@code true} jika nama yang diinput sudah ada di database pada entitas lain;
	 *         {@code false} jika nama unik atau hanya dimiliki oleh entitas yang sedang diedit
	 */
	public Boolean checkNamaJenisPekerjaanPenyedia() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisPekerjaanPenyedia.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.jenisPekerjaanPenyedia.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisPekerjaanPenyedia.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
