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
import ais.database.model.asset.DokumenPenyediaAsset;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>DokumenPenyediaAssetAction — Kontroler Master Template Dokumen Penyedia Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZK (ZKoss 5.5) untuk halaman manajemen master
 * data Template Dokumen Penyedia Aset. Setiap entri data mendefinisikan satu
 * jenis dokumen persyaratan yang harus dipenuhi oleh penyedia aset (vendor/supplier)
 * sebelum dapat mengikuti proses pengadaan di institusi. Contoh dokumen antara
 * lain: Surat Izin Usaha Perdagangan (SIUP), Nomor Pokok Wajib Pajak (NPWP),
 * Sertifikat Badan Usaha (SBU), Akta Pendirian Perusahaan, dan sebagainya.
 * Template ini menjadi acuan bagi sistem untuk secara otomatis membuat entitas
 * {@code PenyediaAssetPunyaDokumen} saat penyedia baru mendaftar, sehingga
 * daftar dokumen yang harus dilengkapi sudah tersedia secara sistematis.
 * Field {@code wajib} menentukan apakah dokumen merupakan syarat mutlak,
 * sementara {@code nomorUrut} menentukan urutan tampil dokumen pada formulir
 * pengisian.
 *
 * <b>Cara kerja:</b><br>
 * Kontroler mengimplementasikan {@code DataCriteria}, {@code DataSearchDefault},
 * dan {@code DataInitDefault}. Siklus hidup dimulai dari {@code doBeforeCompose}
 * (pemeriksaan keamanan), dilanjutkan {@code doAfterCompose} (inisialisasi hak
 * akses, paginasi, pencarian awal, tombol ekspor/unggah). Grid menggunakan
 * {@code DokumenPenyediaAssetRenderer} yang menampilkan kode, nama (revisi),
 * keterangan, {@code MyIntbox} nomor urut (simpan langsung saat onChange),
 * checkbox aktif (simpan langsung saat onCheck), checkbox wajib (simpan langsung
 * saat onCheck), dan tombol aksi salin/ubah/hapus. Validasi duplikasi nama
 * dilakukan di {@code checkNamaDokumenPenyediaAsset} sebelum penyimpanan.
 * Urutan tampil grid diatur berdasarkan nomor urut lalu nama.
 *
 * <b>Threading:</b><br>
 * Semua operasi berjalan di thread event ZK menggunakan sesi Hibernate per-thread.
 * Tidak ada thread latar belakang. Event listener untuk checkbox aktif, checkbox
 * wajib, Intbox nomor urut, dan tombol simpan semuanya berjalan sinkron di
 * thread event ZK. Perubahan melalui checkbox atau Intbox langsung disimpan
 * ke basis data tanpa konfirmasi pengguna.
 *
 * <b>Pemeliharaan:</b><br>
 * Validasi duplikasi nama menggunakan {@code Restrictions.eq} (case-sensitive).
 * Jika perlu case-insensitive, ganti dengan {@code Restrictions.ilike}. Field
 * {@code wajib} tidak ada di formulir tambah/ubah (hanya di renderer grid),
 * sehingga saat menambah dokumen baru nilai default wajib diatur dari entitas.
 * Jika perlu field wajib di formulir, tambahkan checkbox di metode
 * {@code init(DokumenPenyediaAsset)} dan tangani di {@code onSave}. Urutan grid
 * menggunakan dua level sorting: nomor urut ascending lalu nama ascending.
 *
 * @author Sistem AIS
 * @version 1.0
 * @see DokumenPenyediaAsset
 * @see PenyediaAssetPunyaDokumenAction
 * @see DataCriteria
 * @see DataSearchDefault
 * @see DataInitDefault
 */
public class DokumenPenyediaAssetAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Nomor versi serialisasi untuk kompatibilitas serialisasi Java.
	 * Nilai ini tidak boleh diubah kecuali terjadi perubahan struktural pada kelas.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZK untuk formulir tambah atau ubah data dokumen penyedia aset. */
	private MyWindow addWindow;

	/** Komponen paginasi ZK untuk navigasi halaman pada grid data utama. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar template dokumen penyedia aset. */
	private MyGrid grid;

	/** Kotak teks untuk memasukkan kata kunci pencarian berdasarkan nama dokumen. */
	private Textbox searchnama;

	/** Checkbox filter untuk menampilkan hanya data dokumen dengan status aktif. */
	private Checkbox searchaktif;

	/** Kotak teks pada formulir untuk memasukkan nama template dokumen. */
	private Textbox nama;

	/** Kotak teks pada formulir untuk memasukkan keterangan atau deskripsi dokumen. */
	private Textbox keterangan;

	/**
	 * Flag hak akses ubah data. Bernilai {@code true} jika pengguna memiliki
	 * hak {@code CommonPrivilages.UPDATE}.
	 */
	private boolean edit = false;

	/**
	 * Flag hak akses hapus data. Bernilai {@code true} jika pengguna memiliki
	 * hak {@code CommonPrivilages.DELETE}.
	 */
	private boolean delete = false;

	/** Entitas template dokumen penyedia aset yang sedang diedit atau dibuat baru. */
	private DokumenPenyediaAsset dokumenPenyediaAsset;

	/** Tombol toolbar untuk menambah data template dokumen penyedia aset baru. */
	private MyToolbarbuttonConfig add;

	/** Kotak teks pada formulir untuk memasukkan kode unik template dokumen. */
	private Textbox kode;

	/**
	 * <b>Tujuan:</b> Memeriksa keamanan akses sebelum komponen ZK dikompilasi dan
	 * dirakit oleh framework, memastikan hanya pengguna terautentikasi yang dapat
	 * mengakses halaman manajemen template dokumen penyedia aset.
	 *
	 * <b>Cara kerja:</b> Dipanggil oleh framework ZK sebelum fase {@code doAfterCompose}.
	 * Memanggil {@code Common.doCheckSecurity()} yang melakukan redirect ke login
	 * jika sesi tidak valid atau pengguna tidak memiliki hak akses. Jika valid,
	 * melanjutkan dengan memanggil {@code super.doBeforeCompose} untuk proses
	 * perakitan komponen normal.
	 *
	 * <b>Parameter:</b><br>
	 * - {@code page}: halaman ZK yang sedang dikompose<br>
	 * - {@code parent}: komponen induk dalam hierarki ZK<br>
	 * - {@code compInfo}: metadata komponen dari file ZUL
	 *
	 * <b>Return:</b> {@code ComponentInfo} dari pemanggilan super yang diperlukan
	 * untuk melanjutkan proses komposisi ZK.
	 *
	 * <b>Penanganan error:</b> Redirect otomatis ke login jika autentikasi gagal.
	 * Eksekusi tidak akan mencapai super jika keamanan gagal.
	 *
	 * <b>Pemeliharaan:</b> Pemanggilan {@code Common.doCheckSecurity()} tidak boleh
	 * dihapus atau dipindahkan karena merupakan garis pertahanan keamanan utama.
	 *
	 * @param page     halaman ZK yang sedang dikompose
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo informasi metadata komponen dari ZUL
	 * @return ComponentInfo hasil dari super.doBeforeCompose
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Menginisialisasi seluruh komponen UI dan logika bisnis setelah
	 * framework ZK selesai merakit komponen dari file ZUL, mencakup hak akses
	 * pengguna, paginasi, pencarian awal, dan pendaftaran tombol ekspor serta unggah.
	 *
	 * <b>Cara kerja:</b> Urutan inisialisasi: (1) memanggil super dan bahasa
	 * antarmuka, (2) menyetel visibilitas tombol tambah sesuai hak CREATE dan tooltip
	 * "Tambah", (3) membaca flag edit dan delete dari hak akses pengguna, (4)
	 * menjalankan pencarian awal untuk memuat data, (5) memasang listener paginasi
	 * untuk memuat ulang grid saat halaman berubah, (6) mendaftarkan tombol ekspor
	 * data dengan tujuh kolom: id, kode, nama, keterangan, nomorUrut, wajib, aktif,
	 * (7) mendaftarkan tombol unggah data dengan visibilitas terbatas pada pengguna
	 * yang memiliki hak tambah, ubah, dan hapus sekaligus.
	 *
	 * <b>Parameter:</b> {@code comp} adalah komponen root halaman ZUL.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception} umum.
	 *
	 * <b>Pemeliharaan:</b> Array {@code contents} harus konsisten dengan field
	 * yang ada pada entitas {@code DokumenPenyediaAsset}. Nama "nomorUrut" dan
	 * "wajib" adalah nama properti Java (camelCase) yang akan di-introspeksi
	 * oleh mekanisme ekspor untuk memanggil getter yang sesuai.
	 *
	 * @param comp komponen root halaman ZUL yang telah selesai dirakit
	 * @throws Exception jika terjadi kesalahan saat inisialisasi komponen
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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "nomorUrut", "wajib", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(DokumenPenyediaAsset.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, DokumenPenyediaAsset.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <b>Tujuan:</b> Kelas renderer baris yang merender satu entitas
	 * {@code DokumenPenyediaAsset} ke dalam baris {@code Row} ZK pada grid utama,
	 * dengan kontrol interaktif untuk nomor urut, status aktif, dan status wajib
	 * yang semuanya dapat diubah langsung tanpa membuka formulir.
	 *
	 * <b>Cara kerja:</b> Setiap baris menampilkan: (1) Label kode dokumen, (2) Vbox
	 * nama dengan tautan revisi via RevisiHelper, (3) Label keterangan, (4) MyIntbox
	 * nomor urut yang dapat diedit dengan listener onChange yang langsung menyimpan
	 * ke basis data, dinonaktifkan jika tidak memiliki hak ubah, (5) Checkbox aktif
	 * dengan listener onCheck yang langsung menyimpan, (6) Checkbox wajib dengan
	 * listener onCheck yang langsung menyimpan, (7) tombol aksi salin/ubah/hapus
	 * via {@code Common.copyEditDeleteButtons}. Kedua checkbox dinonaktifkan jika
	 * pengguna tidak memiliki hak ubah.
	 *
	 * <b>Parameter:</b><br>
	 * - {@code arg0}: baris Row ZK yang akan diisi<br>
	 * - {@code arg1}: objek DokumenPenyediaAsset yang akan dirender
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception}. Perubahan checkbox dan
	 * intbox langsung disimpan tanpa konfirmasi.
	 *
	 * <b>Pemeliharaan:</b> Tiga kontrol interaktif (nomorUrut, aktif, wajib)
	 * menggunakan pola listener inline yang langsung memanggil
	 * {@code Common.refreshSaveOrUpdate}. Jika perlu menambah kolom interaktif,
	 * ikuti pola yang sama.
	 */
	class DokumenPenyediaAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data {@code DokumenPenyediaAsset} ke dalam
		 * komponen {@code Row} ZK dengan kolom kode, nama revisi, keterangan, nomor
		 * urut interaktif, checkbox aktif, checkbox wajib, dan tombol aksi.
		 *
		 * <b>Cara kerja:</b> Dipanggil oleh framework ZK untuk tiap baris yang perlu
		 * dirender. Cast dilakukan dari Object ke DokumenPenyediaAsset. MyIntbox nomor
		 * urut terhubung ke listener onChange yang memperbarui field nomorUrut pada
		 * entitas dan menyimpannya langsung. Checkbox aktif dan wajib masing-masing
		 * terhubung ke listener onCheck yang memperbarui field terkait dan menyimpan
		 * langsung. Semua kontrol dinonaktifkan jika pengguna tidak memiliki hak ubah.
		 * Tombol aksi didelegasikan ke {@code Common.copyEditDeleteButtons} yang
		 * mengelola visibilitas dan pemanggilan {@code DataInitDefault.init}.
		 *
		 * <b>Parameter:</b><br>
		 * - {@code arg0}: baris Row ZK target<br>
		 * - {@code arg1}: objek DokumenPenyediaAsset yang akan ditampilkan
		 *
		 * <b>Return:</b> Tidak ada nilai kembalian (void).
		 *
		 * <b>Penanganan error:</b> Melempar {@code Exception}. Perubahan langsung
		 * disimpan tanpa penanganan error khusus di level renderer.
		 *
		 * <b>Pemeliharaan:</b> Urutan setParent harus sesuai urutan kolom di ZUL.
		 * Jika ada kolom baru, tambahkan sebelum pemanggilan copyEditDeleteButtons.
		 *
		 * @param arg0 baris Row ZK yang akan diisi komponen-komponen sel
		 * @param arg1 objek data DokumenPenyediaAsset yang akan dirender
		 * @throws Exception jika terjadi kesalahan pembuatan atau pemasangan komponen
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final DokumenPenyediaAsset dokumenPenyediaAsset = (DokumenPenyediaAsset) arg1;
			new Label(dokumenPenyediaAsset.getKode()).setParent(arg0);
			RevisiHelper
					.createNewRevisi(DokumenPenyediaAsset.class, dokumenPenyediaAsset, dokumenPenyediaAsset.getNama())
					.setParent(arg0);
			new Label(dokumenPenyediaAsset.getKeterangan()).setParent(arg0);

			final MyIntbox nomorUrut = new MyIntbox(dokumenPenyediaAsset.getNomorUrut());
			nomorUrut.setDisabled(!edit);
			nomorUrut.setWidth("90%");
			nomorUrut.setParent(arg0);
			nomorUrut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dokumenPenyediaAsset.setNomorUrut(nomorUrut.getValue());
					Common.refreshSaveOrUpdate(dokumenPenyediaAsset);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(dokumenPenyediaAsset.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dokumenPenyediaAsset.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(dokumenPenyediaAsset);
				}
			});

			final MyCheckboxConfig wajib = new MyCheckboxConfig("Wajib");
			wajib.setDisabled(!edit);
			wajib.setChecked(dokumenPenyediaAsset.getWajib());
			wajib.setParent(arg0);
			wajib.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dokumenPenyediaAsset.setWajib(wajib.isChecked());
					Common.refreshSaveOrUpdate(dokumenPenyediaAsset);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, dokumenPenyediaAsset, DokumenPenyediaAssetAction.this)
					.setParent(arg0);

		}

	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" pada toolbar untuk membuka
	 * formulir penambahan template dokumen penyedia aset baru dalam jendela modal.
	 *
	 * <b>Cara kerja:</b> Membuat instance baru {@code DokumenPenyediaAsset} yang
	 * kosong (tanpa ID, menandakan data baru) dan meneruskannya ke metode
	 * {@code init(DokumenPenyediaAsset)} untuk membangun formulir modal. Jendela
	 * modal kemudian ditampilkan dan diaktifkan. Jendela yang sama ({@code addWindow})
	 * digunakan untuk operasi tambah dan ubah, dengan judul berbeda.
	 *
	 * <b>Parameter:</b> {@code event} adalah event ZK dari klik tombol tambah.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception} umum.
	 *
	 * <b>Pemeliharaan:</b> Merupakan titik masuk CREATE pada halaman ini.
	 * Nama metode harus sesuai dengan konfigurasi event di ZUL atau listener
	 * programatik.
	 *
	 * @param event event ZK yang dipicu saat tombol tambah diklik
	 * @throws Exception jika terjadi kesalahan saat inisialisasi formulir
	 */
	public void onAdd(Event event) throws Exception {
		init(new DokumenPenyediaAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan kontrak {@code DataInitDefault} untuk
	 * menginisialisasi formulir dengan data entitas dari operasi salin atau ubah
	 * yang dipicu dari tombol aksi pada baris grid utama.
	 *
	 * <b>Cara kerja:</b> Menerima {@code GeneralValueObject} dan melakukan type-cast
	 * ke {@code DokumenPenyediaAsset}. Menyimpan referensi ke field instance
	 * {@code dokumenPenyediaAsset}, kemudian mendelegasikan ke metode private
	 * {@code init(DokumenPenyediaAsset)} untuk membangun komponen formulir. Setelah
	 * formulir selesai dibangun, jendela modal ditampilkan dan diaktifkan.
	 * Metode ini dipanggil secara otomatis oleh infrastruktur
	 * {@code Common.copyEditDeleteButtons} ketika tombol salin atau ubah diklik
	 * pada baris grid.
	 *
	 * <b>Parameter:</b> {@code obj} adalah entitas {@code DokumenPenyediaAsset}
	 * yang dibungkus sebagai {@code GeneralValueObject}.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception}. ClassCastException
	 * jika tipe objek tidak sesuai ditangkap oleh framework ZK.
	 *
	 * <b>Pemeliharaan:</b> Signature metode ini merupakan kontrak antarmuka
	 * {@code DataInitDefault} dan tidak boleh diubah.
	 *
	 * @param obj objek GeneralValueObject yang akan di-cast ke DokumenPenyediaAsset
	 * @throws Exception jika cast gagal atau inisialisasi formulir bermasalah
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		dokumenPenyediaAsset = (DokumenPenyediaAsset) obj;
		init(dokumenPenyediaAsset);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan menginisialisasi seluruh komponen formulir modal
	 * untuk menambah atau mengubah data template dokumen penyedia aset, mengisi
	 * field kode, nama, dan keterangan sesuai data entitas yang diberikan.
	 *
	 * <b>Cara kerja:</b> Menyetel judul jendela ("Tambah Dokumen" atau "Ubah Dokumen")
	 * berdasarkan keberadaan ID entitas, membersihkan isi jendela, lalu membangun
	 * tata letak {@code MyBorderlayout} dengan area tengah berisi grid formulir
	 * dua kolom (30%-sisanya) dan area selatan berisi toolbar tombol aksi. Formulir
	 * memiliki tiga baris: (1) Kode Dokumen (opsional, Textbox), (2) Nama Dokumen
	 * (wajib saat simpan, Textbox), dan (3) Keterangan (area teks 3 baris). Field
	 * wajib dan nomor urut tidak ada di formulir ini karena dikelola langsung
	 * via checkbox dan intbox di baris grid. Nilai awal field diambil dari properti
	 * entitas. Tombol Batal menutup jendela tanpa menyimpan; tombol Simpan
	 * memvalidasi dan menyimpan via {@code onSave}.
	 *
	 * <b>Parameter:</b> {@code dokumenPenyediaAsset} adalah entitas yang datanya
	 * akan ditampilkan. ID null berarti data baru yang akan ditambahkan.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit. Kesalahan
	 * pembuatan komponen ZK ditangkap oleh framework.
	 *
	 * <b>Pemeliharaan:</b> Perhatikan bahwa field "wajib" dan "nomorUrut" tidak
	 * ada di formulir ini karena nilai defaultnya diatur pada entitas dan dapat
	 * diubah langsung dari grid. Jika perlu menambah field wajib di formulir,
	 * tambahkan checkbox dan tangani di {@code onSave}. Variabel lokal {@code grid}
	 * di sini adalah grid formulir, berbeda dari field instance {@code grid} (grid utama).
	 *
	 * @param dokumenPenyediaAsset entitas DokumenPenyediaAsset yang akan ditampilkan
	 */
	private void init(DokumenPenyediaAsset dokumenPenyediaAsset) {
		this.dokumenPenyediaAsset = dokumenPenyediaAsset;
		addWindow.setTitle(dokumenPenyediaAsset.getId() == null ? "Tambah Dokumen" : "Ubah Dokumen");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Dokumen"));
		row.appendChild(kode = new Textbox(dokumenPenyediaAsset.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Dokumen"));
		row.appendChild(nama = new Textbox(dokumenPenyediaAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(dokumenPenyediaAsset.getKeterangan()));
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
	 * <b>Tujuan:</b> Memvalidasi input formulir dan menyimpan data template dokumen
	 * penyedia aset (baru atau yang sudah ada) ke basis data, dengan pemeriksaan
	 * duplikasi nama sebelum penyimpanan untuk memastikan integritas data.
	 *
	 * <b>Cara kerja:</b> Dua tahap validasi berurutan: (1) nama tidak boleh kosong;
	 * jika kosong, dialog informasi ditampilkan dan metode mengembalikan {@code false},
	 * (2) {@code checkNamaDokumenPenyediaAsset()} dipanggil untuk memeriksa duplikasi
	 * nama di basis data (dengan pengecualian entitas yang sedang diedit berdasarkan
	 * ID); jika duplikat, dialog informasi ditampilkan dan metode mengembalikan
	 * {@code false}. Jika semua validasi lolos, sesi Hibernate diambil; untuk data
	 * yang ada (ID tidak null), entitas dimuat ulang via {@code session.load} agar
	 * sesi mengelolanya dengan benar. Tiga field diatur: kode, nama, dan keterangan.
	 * Field wajib dan nomor urut tidak diatur di sini karena dikelola langsung dari
	 * grid via checkbox dan intbox. Akhirnya entitas disimpan via
	 * {@code Common.refreshSaveOrUpdate}.
	 *
	 * <b>Parameter:</b> {@code event} adalah event ZK dari klik tombol simpan.
	 *
	 * <b>Return:</b> {@code true} jika penyimpanan berhasil; {@code false} jika
	 * validasi gagal.
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception} umum. Jika terjadi
	 * pelanggaran constraint lain di basis data, Hibernate melempar exception
	 * yang ditangkap framework ZK.
	 *
	 * <b>Pemeliharaan:</b> Berbeda dengan kelas lain, metode ini TIDAK menyetel
	 * field wajib dan nomorUrut karena dikelola via kontrol inline di grid.
	 * Jika field wajib ditambahkan ke formulir di masa depan, tambahkan
	 * {@code dokumenPenyediaAsset.setWajib(nilai)} setelah pengaturan keterangan.
	 *
	 * @param event event ZK dari klik tombol simpan
	 * @return true jika validasi lolos dan penyimpanan berhasil, false jika tidak
	 * @throws Exception jika terjadi kesalahan Hibernate atau ZK saat penyimpanan
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Dokumen Penyedia belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama jenis dokumen penyedia; (2) Nama ini digunakan untuk mengidentifikasi dokumen dalam daftar persyaratan vendor; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaDokumenPenyediaAsset();
		if (i) {
			MyMessageboxConfig.show("Nama Dokumen sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (dokumenPenyediaAsset.getId() != null) {
			dokumenPenyediaAsset = (DokumenPenyediaAsset) session.load(DokumenPenyediaAsset.class,
					dokumenPenyediaAsset.getId());

		}

		dokumenPenyediaAsset.setKode(kode.getValue());
		dokumenPenyediaAsset.setNama(nama.getValue());
		dokumenPenyediaAsset.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, dokumenPenyediaAsset);

		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun objek {@code Criteria} Hibernate untuk pencarian data
	 * template dokumen penyedia aset berdasarkan filter status aktif dan kata kunci
	 * nama yang aktif pada antarmuka pencarian halaman.
	 *
	 * <b>Cara kerja:</b> Membuat {@code Criteria} untuk kelas {@code DokumenPenyediaAsset}
	 * dengan dua filter: (1) Filter aktif: jika checkbox {@code searchaktif} null
	 * atau dicentang, hanya data dengan aktif true atau null yang ditampilkan;
	 * jika tidak dicentang, semua data (termasuk tidak aktif) ditampilkan.
	 * (2) Filter nama: pencarian case-insensitive pada kolom nama menggunakan
	 * {@code MatchMode.ANYWHERE}; jika kosong, semua data ditampilkan. Jika
	 * {@code order} true, diurutkan berdasarkan dua kriteria: nomor urut ascending
	 * sebagai urutan primer, lalu nama ascending sebagai urutan sekunder. Pengurutan
	 * dua level ini memastikan dokumen dengan nomor urut yang sama diurutkan
	 * alfabetis.
	 *
	 * <b>Parameter:</b> {@code order} menentukan apakah ada klausa ORDER BY.
	 *
	 * <b>Return:</b> Objek {@code Criteria} Hibernate yang siap dieksekusi.
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error khusus.
	 *
	 * <b>Pemeliharaan:</b> Urutan dua level (nomorUrut lalu nama) berbeda dari
	 * kelas CRUD lain yang hanya menggunakan satu level urutan. Jika perlu
	 * mengubah urutan menjadi berdasarkan nama saja, hapus
	 * {@code addOrder(Order.asc("nomorUrut"))}.
	 *
	 * @param order jika true, hasil diurutkan berdasarkan nomorUrut lalu nama ascending
	 * @return objek Criteria Hibernate yang sudah dikonfigurasi dengan semua filter
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DokumenPenyediaAsset.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"));
		criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Melakukan pencarian data template dokumen penyedia aset
	 * berdasarkan filter yang aktif dan memperbarui tampilan grid utama beserta
	 * paginasi sesuai halaman yang sedang aktif.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code Common.initPaging} dengan kriteria tanpa
	 * urutan untuk menghitung total data dan memperbarui komponen paginasi. Kemudian
	 * mengambil data halaman saat ini dengan batasan jumlah baris per halaman dan
	 * offset berdasarkan halaman aktif. Hasil dibungkus dalam {@code SimpleListModel}
	 * dan ditetapkan ke grid bersama renderer baru {@code DokumenPenyediaAssetRenderer}.
	 * Metode dipanggil pada inisialisasi halaman, saat paginasi berubah, dan setelah
	 * operasi simpan data berhasil.
	 *
	 * <b>Parameter:</b> {@code event} adalah event ZK pemicu pencarian, dapat null
	 * jika dipanggil secara programatik.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit. Anotasi
	 * {@code @SuppressWarnings("unchecked")} untuk type-safety pada konversi List.
	 *
	 * <b>Pemeliharaan:</b> Konsistensi antara {@code initCriteria(false)} (untuk
	 * count paginasi) dan {@code initCriteria(true)} (untuk data) harus dijaga.
	 * Perhatikan bahwa hasil ini diurutkan dua level (nomorUrut dan nama) berbeda
	 * dari grid tanpa urutan yang digunakan untuk count paginasi.
	 *
	 * @param event event ZK pemicu pencarian, dapat null jika dipanggil programatik
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DokumenPenyediaAsset> dokumenPenyediaAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(dokumenPenyediaAsset);
		grid.setRowRenderer(new DokumenPenyediaAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Memeriksa apakah nama dokumen yang dimasukkan pada formulir
	 * sudah ada di basis data untuk mencegah duplikasi nama template dokumen
	 * penyedia aset.
	 *
	 * <b>Cara kerja:</b> Membangun kueri Hibernate dengan proyeksi {@code rowCount}
	 * untuk menghitung jumlah entitas {@code DokumenPenyediaAsset} yang memiliki
	 * nama yang sama persis dengan nilai field {@code nama} saat ini (case-sensitive,
	 * menggunakan {@code Restrictions.eq}). Untuk operasi ubah (entitas sudah punya
	 * ID), entitas dengan ID yang sama dikecualikan dari pencarian menggunakan
	 * {@code Restrictions.ne("id", id)} sehingga nama yang tidak berubah tetap
	 * dianggap valid. Untuk data baru (ID null), semua entitas dengan nama tersebut
	 * dianggap duplikat dan mengembalikan true.
	 *
	 * <b>Parameter:</b> Tidak ada parameter langsung; membaca dari field instance
	 * {@code nama} (Textbox) dan {@code this.dokumenPenyediaAsset}.
	 *
	 * <b>Return:</b> {@code true} jika nama sudah ada di basis data (duplikat dan
	 * penyimpanan harus ditolak); {@code false} jika nama belum ada dan aman untuk
	 * disimpan.
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit. Jika
	 * {@code uniqueResult()} mengembalikan null (tidak mungkin untuk rowCount),
	 * akan terjadi NullPointerException. Pastikan koneksi basis data aktif.
	 *
	 * <b>Pemeliharaan:</b> Pola ini identik dengan
	 * {@code KelompokParameterTambahanPerbaikanAssetAction.checkNamaKelompokParameterTambahanPerbaikanAsset}.
	 * Perbandingan nama menggunakan {@code Restrictions.eq} yang case-sensitive.
	 * Jika perlu case-insensitive, ganti dengan {@code Restrictions.ilike} dan
	 * {@code MatchMode.EXACT}.
	 *
	 * @return Boolean true jika nama sudah ada (duplikat), false jika nama belum digunakan
	 */
	public Boolean checkNamaDokumenPenyediaAsset() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(DokumenPenyediaAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.dokumenPenyediaAsset.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.dokumenPenyediaAsset.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
