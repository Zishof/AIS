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
import ais.database.model.asset.JenisPengapusanBarang;
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
 * <h3>JenisPengapusanBarangAction — Kontroler Master Jenis Pengapusan (Penghapusan) Barang</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZK (ZKoss 5.5) untuk halaman manajemen master
 * data Jenis Pengapusan Barang (juga disebut Jenis Penghapusan Barang) dalam
 * modul aset. Pengapusan (disposal) adalah proses penghapusan aset dari daftar
 * inventaris institusi, yang dapat dilakukan karena berbagai alasan seperti
 * penjualan, pemusnahan, hibah, atau kehilangan. Setiap jenis pengapusan
 * mendefinisikan perlakuan akuntansi yang berbeda melalui pasangan akun debet
 * dan akun kredit yang sesuai. Misalnya, pengapusan karena penjualan mencatat
 * debet kas dan kredit aset, sedangkan pengapusan karena pemusnahan mencatat
 * debet kerugian dan kredit aset. Data ini menjadi referensi wajib pada
 * transaksi penghapusan aset untuk memastikan jurnal akuntansi yang dihasilkan
 * secara otomatis sudah benar sesuai standar akuntansi yang berlaku.
 *
 * <b>Cara kerja:</b><br>
 * Kontroler ini mengimplementasikan tiga antarmuka: {@code DataCriteria} untuk
 * kriteria pencarian Hibernate, {@code DataSearchDefault} untuk memuat ulang
 * grid, dan {@code DataInitDefault} untuk menginisialisasi formulir dari baris
 * grid (operasi salin/ubah). Siklus hidup dimulai dari {@code doBeforeCompose}
 * yang memeriksa keamanan, dilanjutkan {@code doAfterCompose} yang mengatur hak
 * akses, paginasi, pencarian awal, dan tombol ekspor/unggah. Grid menggunakan
 * {@code JenisPengapusanBarangRenderer} yang menampilkan kode, nama (revisi),
 * akun debet, akun kredit, keterangan, checkbox aktif (simpan langsung), dan
 * tombol aksi. Formulir modal dibangun secara programatik dengan field kode,
 * nama, akun debet, akun kredit, dan keterangan menggunakan
 * {@code AmbilDataAkunBanbox} untuk pemilihan akun dari master akun.
 *
 * <b>Threading:</b><br>
 * Semua interaksi basis data berjalan di thread event ZK melalui sesi Hibernate
 * per-thread. Tidak ada thread latar belakang. Event listener checkbox aktif
 * dan tombol simpan berjalan sinkron. Akses sesi Hibernate aman karena ZK
 * menjamin eksekusi event di satu thread per sesi.
 *
 * <b>Pemeliharaan:</b><br>
 * Validasi di {@code onSave} memerlukan tiga field wajib: nama, akun debet,
 * dan akun kredit. Berbeda dari kelas pajak yang hanya memerlukan nama dan
 * satu akun. Jika perlu menambahkan jenis pengapusan baru yang memerlukan
 * akun tambahan, tambahkan {@code AmbilDataAkunBanbox} baru di formulir.
 * Pastikan entitas {@code JenisPengapusanBarang} memiliki mapping Hibernate
 * yang benar untuk akun debet dan kredit sebagai many-to-one ke entitas
 * {@code Akun}.
 *
 * @author Sistem AIS
 * @version 1.0
 * @see JenisPengapusanBarang
 * @see DataCriteria
 * @see DataSearchDefault
 * @see DataInitDefault
 */
public class JenisPengapusanBarangAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Nomor versi serialisasi untuk kompatibilitas serialisasi Java.
	 * Nilai ini tidak boleh diubah kecuali terjadi perubahan struktural pada kelas.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZK untuk formulir tambah atau ubah data jenis pengapusan barang. */
	private MyWindow addWindow;

	/** Komponen paginasi ZK untuk navigasi halaman pada grid data utama. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar jenis pengapusan barang. */
	private MyGrid grid;

	/** Kotak teks untuk memasukkan kata kunci pencarian berdasarkan nama jenis pengapusan. */
	private Textbox searchnama;

	/** Checkbox filter untuk menampilkan hanya data dengan status aktif. */
	private Checkbox searchaktif;

	/** Kotak teks pada formulir untuk memasukkan nama jenis pengapusan barang. */
	private Textbox nama;

	/** Kotak teks pada formulir untuk memasukkan keterangan atau deskripsi pengapusan. */
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

	/** Entitas jenis pengapusan barang yang sedang diedit atau dibuat baru. */
	private JenisPengapusanBarang jenisPengapusanBarang;

	/** Tombol toolbar untuk menambah data jenis pengapusan barang baru. */
	private MyToolbarbuttonConfig add;

	/** Kotak teks pada formulir untuk memasukkan kode unik jenis pengapusan. */
	private Textbox kode;

	/** Komponen pemilih akun untuk akun debet pada jurnal pengapusan barang. */
	private AmbilDataAkunBanbox debet;

	/** Komponen pemilih akun untuk akun kredit pada jurnal pengapusan barang. */
	private AmbilDataAkunBanbox kredit;

	/**
	 * <b>Tujuan:</b> Memeriksa keamanan akses sebelum komponen ZK dikompilasi dan
	 * dirakit oleh framework, memastikan hanya pengguna terautentikasi yang dapat
	 * mengakses halaman manajemen jenis pengapusan barang.
	 *
	 * <b>Cara kerja:</b> Dipanggil oleh framework ZK sebelum {@code doAfterCompose}.
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi sesi dan hak
	 * akses pengguna. Jika tidak valid, pengguna diarahkan ke halaman login dan
	 * eksekusi berhenti. Jika valid, dilanjutkan dengan memanggil
	 * {@code super.doBeforeCompose} untuk proses perakitan komponen normal.
	 *
	 * <b>Parameter:</b><br>
	 * - {@code page}: halaman ZK yang sedang dikompose<br>
	 * - {@code parent}: komponen induk dalam hierarki ZK<br>
	 * - {@code compInfo}: metadata komponen dari file ZUL
	 *
	 * <b>Return:</b> {@code ComponentInfo} dari pemanggilan super.
	 *
	 * <b>Penanganan error:</b> Redirect otomatis jika autentikasi gagal.
	 *
	 * <b>Pemeliharaan:</b> Pemanggilan {@code Common.doCheckSecurity()} adalah
	 * lapisan keamanan kritis yang tidak boleh dihapus atau dipindahkan.
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
	 * framework ZK selesai merakit komponen, mencakup hak akses, paginasi,
	 * pencarian awal, tombol ekspor data, dan tombol unggah data.
	 *
	 * <b>Cara kerja:</b> Urutan inisialisasi: (1) super dan bahasa antarmuka,
	 * (2) tombol tambah disetel visibilitasnya sesuai hak CREATE dan tooltip
	 * "Tambah", (3) flag edit dan delete diisi dari hak akses pengguna, (4)
	 * pencarian awal dijalankan via {@code onSearchDefault(null)}, (5) listener
	 * paginasi dipasang untuk memuat ulang grid saat halaman berubah, (6) tombol
	 * ekspor data didaftarkan dengan array kolom: id, kode, nama, debet, kredit,
	 * keterangan, aktif, (7) tombol unggah data didaftarkan dengan visibilitas
	 * terbatas pada pengguna yang memiliki hak tambah, ubah, dan hapus sekaligus.
	 *
	 * <b>Parameter:</b> {@code comp} adalah komponen root halaman ZUL.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception} umum. Jika terjadi
	 * kesalahan inisialisasi komponen, framework ZK menampilkan pesan error.
	 *
	 * <b>Pemeliharaan:</b> Array {@code contents} menentukan kolom yang diekspor.
	 * "debet" dan "kredit" adalah properti FK pada {@code JenisPengapusanBarang}
	 * yang merupakan referensi ke entitas {@code Akun}; pastikan getter-nya
	 * menghasilkan representasi string yang bermakna saat diekspor.
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

		String[] contents = new String[] { "id", "kode", "nama", "debet", "kredit", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisPengapusanBarang.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisPengapusanBarang.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <b>Tujuan:</b> Kelas renderer baris yang merender satu entitas
	 * {@code JenisPengapusanBarang} ke dalam baris {@code Row} ZK pada grid utama
	 * halaman manajemen jenis pengapusan barang, termasuk akun jurnal dan kontrol interaktif.
	 *
	 * <b>Cara kerja:</b> Setiap baris menampilkan kolom berikut secara berurutan:
	 * (1) Label kode pengapusan, (2) Vbox nama dengan tautan revisi via RevisiHelper,
	 * (3) Label akun debet (kode + nama, kosong jika null), (4) Label akun kredit
	 * (kode + nama, kosong jika null), (5) Label keterangan, (6) Checkbox aktif
	 * dengan listener onCheck yang langsung menyimpan perubahan, (7) tombol aksi
	 * salin/ubah/hapus via {@code Common.copyEditDeleteButtons}. Checkbox aktif
	 * dinonaktifkan jika pengguna tidak memiliki hak ubah.
	 *
	 * <b>Parameter:</b><br>
	 * - {@code arg0}: baris Row ZK yang akan diisi<br>
	 * - {@code arg1}: objek JenisPengapusanBarang yang akan dirender
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception}. Null guard diterapkan
	 * pada akun debet dan kredit untuk mencegah NullPointerException.
	 *
	 * <b>Pemeliharaan:</b> Urutan setParent harus sesuai urutan kolom di file ZUL.
	 */
	class JenisPengapusanBarangRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data {@code JenisPengapusanBarang} ke
		 * dalam komponen {@code Row} ZK dengan semua kolom termasuk informasi akun
		 * jurnal debet dan kredit, checkbox aktif interaktif, dan tombol aksi.
		 *
		 * <b>Cara kerja:</b> Metode dipanggil oleh framework ZK untuk tiap baris.
		 * Cast dilakukan dari Object ke JenisPengapusanBarang. Label akun debet dan
		 * kredit menampilkan kombinasi kode dan nama akun, atau string kosong jika
		 * akun belum dipilih (null). Checkbox aktif terhubung ke event onCheck yang
		 * langsung memperbarui field aktif di entitas dan menyimpannya ke basis data
		 * via {@code Common.refreshSaveOrUpdate} tanpa konfirmasi. Tombol aksi
		 * memanfaatkan {@code Common.copyEditDeleteButtons} yang otomatis mengatur
		 * visibilitas berdasarkan flag edit dan delete.
		 *
		 * <b>Parameter:</b><br>
		 * - {@code arg0}: baris Row ZK target<br>
		 * - {@code arg1}: objek JenisPengapusanBarang yang akan ditampilkan
		 *
		 * <b>Return:</b> Tidak ada nilai kembalian (void).
		 *
		 * <b>Penanganan error:</b> Melempar {@code Exception}. Null pada akun
		 * debet/kredit ditangani dengan string kosong.
		 *
		 * <b>Pemeliharaan:</b> Jika ada kolom baru di ZUL, tambahkan komponen
		 * sebelum pemanggilan copyEditDeleteButtons untuk mempertahankan urutan.
		 *
		 * @param arg0 baris Row ZK yang akan diisi komponen-komponen sel
		 * @param arg1 objek data JenisPengapusanBarang yang akan dirender
		 * @throws Exception jika terjadi kesalahan pembuatan atau pemasangan komponen
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisPengapusanBarang jenisPengapusanBarang = (JenisPengapusanBarang) arg1;
			new Label(jenisPengapusanBarang.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(JenisPengapusanBarang.class, jenisPengapusanBarang,
					jenisPengapusanBarang.getNama()).setParent(arg0);
			new Label(jenisPengapusanBarang.getDebet() == null ? ""
					: (jenisPengapusanBarang.getDebet().getKode() + " " + jenisPengapusanBarang.getDebet().getNama()))
					.setParent(arg0);
			new Label(jenisPengapusanBarang.getKredit() == null ? ""
					: (jenisPengapusanBarang.getKredit().getKode() + " " + jenisPengapusanBarang.getKredit().getNama()))
					.setParent(arg0);
			new Label(jenisPengapusanBarang.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisPengapusanBarang.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPengapusanBarang.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisPengapusanBarang);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisPengapusanBarang, JenisPengapusanBarangAction.this)
					.setParent(arg0);

		}

	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" pada toolbar untuk membuka
	 * formulir penambahan data jenis pengapusan barang baru dalam jendela modal.
	 *
	 * <b>Cara kerja:</b> Membuat instance baru {@code JenisPengapusanBarang} yang
	 * kosong (tanpa ID) dan meneruskannya ke metode {@code init(JenisPengapusanBarang)}
	 * untuk membangun formulir. Jendela modal kemudian ditampilkan dan diaktifkan.
	 * Instance entitas baru ini akan memiliki ID null sampai berhasil disimpan,
	 * sehingga logika di {@code onSave} dan {@code init} dapat membedakan operasi
	 * tambah dari operasi ubah.
	 *
	 * <b>Parameter:</b> {@code event} adalah event ZK dari klik tombol tambah.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception} umum yang ditangkap
	 * oleh framework ZK.
	 *
	 * <b>Pemeliharaan:</b> Metode ini merupakan titik masuk untuk operasi CREATE
	 * pada halaman ini.
	 *
	 * @param event event ZK yang dipicu saat tombol tambah diklik
	 * @throws Exception jika terjadi kesalahan saat inisialisasi formulir
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisPengapusanBarang());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan kontrak {@code DataInitDefault} untuk
	 * menginisialisasi formulir dengan data entitas dari operasi salin atau ubah
	 * yang dipicu dari tombol aksi pada baris grid.
	 *
	 * <b>Cara kerja:</b> Menerima {@code GeneralValueObject} dan melakukan type-cast
	 * ke {@code JenisPengapusanBarang}. Menyimpan referensi ke field instance,
	 * kemudian mendelegasikan ke {@code init(JenisPengapusanBarang)} untuk membangun
	 * komponen formulir. Setelah formulir dibangun, jendela modal ditampilkan dan
	 * diaktifkan. Metode ini dipanggil secara otomatis oleh infrastruktur
	 * {@code Common.copyEditDeleteButtons} saat tombol salin atau ubah diklik.
	 *
	 * <b>Parameter:</b> {@code obj} adalah entitas {@code JenisPengapusanBarang}
	 * yang dibungkus sebagai {@code GeneralValueObject} dari grid.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception}. ClassCastException
	 * terjadi jika tipe objek tidak sesuai, ditangkap framework ZK.
	 *
	 * <b>Pemeliharaan:</b> Jangan ubah signature metode ini karena merupakan
	 * kontrak antarmuka {@code DataInitDefault}.
	 *
	 * @param obj objek GeneralValueObject yang akan di-cast ke JenisPengapusanBarang
	 * @throws Exception jika cast gagal atau inisialisasi formulir bermasalah
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisPengapusanBarang = (JenisPengapusanBarang) obj;
		init(jenisPengapusanBarang);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan menginisialisasi seluruh komponen formulir modal
	 * untuk menambah atau mengubah data jenis pengapusan barang, termasuk field
	 * pemilihan akun debet dan kredit untuk jurnal akuntansi.
	 *
	 * <b>Cara kerja:</b> Metode menyetel judul jendela ("Tambah Jenis Pengapusan"
	 * atau "Ubah Jenis Pengapusan"), membersihkan isi jendela, lalu membangun
	 * tata letak dengan {@code MyBorderlayout}. Area tengah berisi grid formulir
	 * dua kolom (30%-sisanya) dengan lima baris: (1) Kode jenis penghapusan
	 * (opsional), (2) Nama jenis penghapusan (wajib, ditandai dengan *),
	 * (3) Akun Debet (wajib, dipilih via {@code AmbilDataAkunBanbox}),
	 * (4) Akun Kredit (wajib, dipilih via {@code AmbilDataAkunBanbox}),
	 * (5) Keterangan (area teks 3 baris). Nilai awal field diambil dari properti
	 * entitas, dengan toString() untuk akun (karena format kode+nama). Toolbar
	 * selatan berisi tombol Batal (menutup jendela) dan Simpan (memvalidasi dan
	 * menyimpan via {@code onSave}).
	 *
	 * <b>Parameter:</b> {@code jenisPengapusanBarang} adalah entitas yang datanya
	 * akan ditampilkan. ID null berarti data baru.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception}. Nilai toString() untuk
	 * akun null menggunakan string kosong sebagai fallback.
	 *
	 * <b>Pemeliharaan:</b> Untuk menambah field baru, tambahkan {@code MyFormRow}
	 * baru sebelum blok South. Perhatikan bahwa {@code debet} dan {@code kredit}
	 * menggunakan {@code toString()} bukan {@code getNama()} untuk nilai awal,
	 * yang mungkin perlu disesuaikan jika format toString() berubah.
	 *
	 * @param jenisPengapusanBarang entitas JenisPengapusanBarang yang akan ditampilkan
	 * @throws Exception jika terjadi kesalahan saat membangun komponen ZK
	 */
	private void init(JenisPengapusanBarang jenisPengapusanBarang) throws Exception {
		this.jenisPengapusanBarang = jenisPengapusanBarang;
		addWindow.setTitle(jenisPengapusanBarang.getId() == null ? "Tambah Jenis Pengapusan" : "Ubah Jenis Pengapusan");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Jenis Penghapusan"));
		row.appendChild(kode = new Textbox(jenisPengapusanBarang.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Penghapusan *"));
		row.appendChild(nama = new Textbox(jenisPengapusanBarang.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Debet *"));
		row.appendChild(debet = new AmbilDataAkunBanbox(false));
		debet.setValue(jenisPengapusanBarang.getDebet() == null ? "" : jenisPengapusanBarang.getDebet().toString());
		debet.setAttribute("akun", jenisPengapusanBarang.getDebet());
		debet.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Kredit *"));
		row.appendChild(kredit = new AmbilDataAkunBanbox(false));
		kredit.setValue(jenisPengapusanBarang.getKredit() == null ? "" : jenisPengapusanBarang.getKredit().toString());
		kredit.setAttribute("akun", jenisPengapusanBarang.getKredit());
		kredit.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisPengapusanBarang.getKeterangan()));
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
	 * <b>Tujuan:</b> Memvalidasi input formulir dan menyimpan data jenis pengapusan
	 * barang ke basis data, dengan validasi tiga field wajib: nama, akun debet,
	 * dan akun kredit.
	 *
	 * <b>Cara kerja:</b> Tiga tahap validasi berurutan: (1) nama tidak boleh kosong,
	 * (2) akun debet wajib dipilih (atribut "akun" tidak boleh null), (3) akun
	 * kredit wajib dipilih (atribut "akun" tidak boleh null). Jika salah satu
	 * validasi gagal, dialog peringatan ditampilkan dan metode mengembalikan
	 * {@code false}. Jika semua valid, sesi Hibernate diambil; untuk data yang ada
	 * (ID tidak null), entitas dimuat ulang dengan {@code session.load} agar sesi
	 * Hibernate mengelolanya dengan benar. Semua field (kode, nama, debet, kredit,
	 * keterangan) diatur pada entitas sebelum disimpan via
	 * {@code Common.refreshSaveOrUpdate}.
	 *
	 * <b>Parameter:</b> {@code event} adalah event ZK dari klik tombol simpan.
	 *
	 * <b>Return:</b> {@code true} jika penyimpanan berhasil; {@code false} jika
	 * validasi gagal.
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception} umum. Dialog peringatan
	 * untuk akun menggunakan ikon EXCLAMATION (bukan INFORMATION) untuk menandakan
	 * kesalahan yang lebih kritis.
	 *
	 * <b>Pemeliharaan:</b> Berbeda dengan {@code JenisPajakBarangAction.onSave}
	 * yang hanya mewajibkan satu akun, metode ini mewajibkan dua akun (debet dan
	 * kredit) karena setiap jurnal pengapusan harus memiliki pasangan debet-kredit
	 * yang lengkap.
	 *
	 * @param event event ZK dari klik tombol simpan
	 * @return true jika validasi lolos dan penyimpanan berhasil, false jika tidak
	 * @throws Exception jika terjadi kesalahan Hibernate atau ZK saat penyimpanan
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Jenis Penghapusan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama jenis penghapusan; (2) Nama jenis penghapusan digunakan untuk klasifikasi penghapusan aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (debet.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Debet cara penghapusan belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Akun Debet; (2) Pilih akun jurnal debet yang sesuai dari daftar akun; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (kredit.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Kredit cara penghapusan belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Akun Kredit; (2) Pilih akun jurnal kredit yang sesuai dari daftar akun; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisPengapusanBarang.getId() != null) {
			jenisPengapusanBarang = (JenisPengapusanBarang) session.load(JenisPengapusanBarang.class,
					jenisPengapusanBarang.getId());

		}

		jenisPengapusanBarang.setKode(kode.getValue());
		jenisPengapusanBarang.setNama(nama.getValue());
		jenisPengapusanBarang.setDebet((Akun) debet.getAttribute("akun"));
		jenisPengapusanBarang.setKredit((Akun) kredit.getAttribute("akun"));
		jenisPengapusanBarang.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisPengapusanBarang);

		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun objek {@code Criteria} Hibernate untuk pencarian data
	 * jenis pengapusan barang berdasarkan filter status aktif dan kata kunci nama
	 * yang aktif pada antarmuka pencarian.
	 *
	 * <b>Cara kerja:</b> Membuat {@code Criteria} untuk kelas {@code JenisPengapusanBarang}
	 * dengan dua filter: (1) Filter aktif: jika checkbox {@code searchaktif} null
	 * atau dicentang, hanya data aktif atau null yang ditampilkan (OR antara null
	 * dan true); jika tidak dicentang, semua data ditampilkan via
	 * {@code sqlRestriction("true")}. (2) Filter nama: pencarian case-insensitive
	 * pada kolom nama menggunakan {@code MatchMode.ANYWHERE}; jika kosong, semua
	 * data ditampilkan. Jika {@code order} true, diurutkan nama ascending.
	 *
	 * <b>Parameter:</b> {@code order} menentukan apakah ada klausa ORDER BY nama.
	 *
	 * <b>Return:</b> Objek {@code Criteria} Hibernate yang siap dieksekusi.
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error khusus.
	 *
	 * <b>Pemeliharaan:</b> Pola filter aktif identik dengan {@code JenisPajakBarangAction.initCriteria}.
	 * Jika perlu menambah filter berdasarkan akun debet atau kredit, gunakan
	 * createAlias untuk join ke tabel akun dan tambahkan Restrictions pada alias.
	 *
	 * @param order jika true, hasil diurutkan berdasarkan nama secara ascending
	 * @return objek Criteria Hibernate yang sudah dikonfigurasi dengan semua filter
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPengapusanBarang.class)
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
	 * <b>Tujuan:</b> Melakukan pencarian data jenis pengapusan barang berdasarkan
	 * filter yang aktif dan memperbarui tampilan grid utama beserta paginasi.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code Common.initPaging} dengan kriteria tanpa
	 * urutan untuk menghitung total data dan halaman. Kemudian mengambil data halaman
	 * saat ini dengan batasan jumlah baris per halaman dan offset berdasarkan halaman
	 * aktif. Hasil dibungkus dalam {@code SimpleListModel} dan ditetapkan ke grid
	 * bersama renderer baru {@code JenisPengapusanBarangRenderer}. Metode ini
	 * dipanggil pada inisialisasi, perubahan paginasi, dan setelah operasi simpan.
	 *
	 * <b>Parameter:</b> {@code event} adalah event ZK pemicu, dapat null.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit. Anotasi
	 * {@code @SuppressWarnings("unchecked")} untuk type-safety pada list.
	 *
	 * <b>Pemeliharaan:</b> Konsistensi antara pemanggilan {@code initCriteria(false)}
	 * (untuk count paginasi) dan {@code initCriteria(true)} (untuk data) harus
	 * dijaga agar jumlah halaman sesuai dengan data yang ditampilkan.
	 *
	 * @param event event ZK pemicu pencarian, dapat null jika dipanggil programatik
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPengapusanBarang> jenisPengapusanBarang = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisPengapusanBarang);
		grid.setRowRenderer(new JenisPengapusanBarangRenderer());
		grid.setModelCheckMobile(strset);

	}

}
