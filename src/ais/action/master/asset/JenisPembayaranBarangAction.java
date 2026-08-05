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
import ais.database.model.asset.JenisPembayaranBarang;
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
 * <h3>JenisPembayaranBarangAction &mdash; Controller CRUD Jenis Pembayaran Barang</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah controller ZK (composer) untuk halaman manajemen data master
 * <em>Jenis Pembayaran Barang</em> (juga dikenal sebagai "Cara Bayar"), yaitu daftar
 * metode pembayaran yang digunakan dalam transaksi pengadaan barang dan aset institusi.
 * Contoh jenis pembayaran meliputi: tunai, transfer bank, cek, giro, kartu kredit,
 * dan mekanisme pembayaran lainnya yang relevan dengan proses keuangan institusi.
 * Setiap jenis pembayaran dikaitkan dengan kode identifikasi unik dan akun akuntansi
 * yang sesuai, sehingga setiap transaksi pembayaran barang dapat dijurnal secara otomatis
 * ke akun yang tepat. Data master ini juga mendukung flag aktif/nonaktif sehingga cara
 * bayar yang sudah tidak digunakan dapat dinonaktifkan tanpa menghapus historis transaksi.
 * Halaman ini menyediakan operasi tambah, ubah, nonaktifkan, serta ekspor dan impor data.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Controller mewarisi {@code GenericAutowireComposer} ZK dan mengimplementasikan
 * {@code DataCriteria}, {@code DataSearchDefault}, dan {@code DataInitDefault}.
 * Alur kerja utama:
 * <ol>
 *   <li>{@code doBeforeCompose} memverifikasi keamanan sesi.</li>
 *   <li>{@code doAfterCompose} menginisialisasi hak akses, memuat data awal, menyiapkan
 *       paging, serta mendaftarkan tombol cetak (ekspor Excel dengan kolom id, kode, nama,
 *       akun, keterangan, aktif) dan tombol upload impor ke toolbar.</li>
 *   <li>{@code onSearchDefault} memuat ulang grid dengan filter nama dan status aktif.
 *       Filter aktif menggunakan checkbox {@code searchaktif}: jika dicentang, hanya
 *       menampilkan jenis pembayaran yang aktif atau null-aktif; jika tidak dicentang,
 *       menampilkan semua.</li>
 *   <li>{@code JenisPembayaranBarangRenderer} merender setiap baris dengan kode, nama,
 *       akun, keterangan, checkbox aktif yang dapat diubah langsung di grid (inline edit),
 *       dan tombol Salin/Ubah/Hapus.</li>
 *   <li>Form popup tambah/ubah dibangun oleh {@code init(JenisPembayaranBarang)} dengan
 *       field kode, nama (wajib), akun (wajib), dan keterangan.</li>
 *   <li>{@code onSave} memvalidasi dan menyimpan data.</li>
 * </ol>
 * Fitur khas kelas ini: checkbox "Aktif" dapat diubah langsung dari grid tanpa membuka
 * form edit, memberikan pengalaman yang lebih cepat bagi operator.</p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh operasi berjalan pada thread ZK event (UI thread). Perubahan flag aktif melalui
 * checkbox inline di grid juga dijalankan secara sinkron pada UI thread yang sama.
 * Sesi Hibernate dari {@code HibernateUtil.currentSession()} terikat pada thread HTTP aktif.
 * Tidak ada risiko race condition selama satu pengguna berinteraksi dengan satu sesi.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Jika menambah kolom baru, perbarui array {@code contents} di {@code doAfterCompose}
 * agar ekspor Excel menyertakan kolom tersebut. Checkbox aktif di grid menggunakan
 * {@code Common.refreshSaveOrUpdate} langsung tanpa melalui {@code onSave}, sehingga
 * tidak ada validasi duplikasi nama yang terjadi saat toggle aktif. Jika perlu validasi
 * tambahan saat toggle, tambahkan logika di listener {@code onCheck} di renderer.
 * Pastikan selalu menguji dengan berbagai kombinasi hak akses (CREATE/UPDATE/DELETE).</p>
 *
 * @author Tim Pengembang AIS
 * @version 1.0
 * @see JenisPembayaranBarang
 * @see AmbilDataAkunBanbox
 * @see DataCriteria
 * @see DataSearchDefault
 * @see DataInitDefault
 */
public class JenisPembayaranBarangAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal popup untuk form tambah/ubah data jenis pembayaran. */
	private MyWindow addWindow;

	/** Komponen paging ZK untuk navigasi halaman pada grid data. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar jenis pembayaran barang. */
	private MyGrid grid;

	/** Kotak teks filter pencarian berdasarkan nama jenis pembayaran. */
	private Textbox searchnama;

	/**
	 * Checkbox filter status aktif di toolbar pencarian; jika dicentang, hanya
	 * menampilkan jenis pembayaran yang aktif atau belum ditetapkan status aktifnya.
	 */
	private Checkbox searchaktif;

	/** Kotak teks input nama jenis pembayaran pada form tambah/ubah. */
	private Textbox nama;

	/** Kotak teks input keterangan jenis pembayaran. */
	private Textbox keterangan;

	/** Flag yang menandakan apakah pengguna memiliki hak ubah data. */
	private boolean edit = false;

	/** Flag yang menandakan apakah pengguna memiliki hak hapus data. */
	private boolean delete = false;

	/** Entitas jenis pembayaran barang yang sedang aktif diedit atau ditambahkan. */
	private JenisPembayaranBarang jenisPembayaranBarang;

	/** Tombol toolbar untuk menambah data jenis pembayaran baru. */
	private MyToolbarbuttonConfig add;

	/** Kotak teks input kode unik jenis pembayaran. */
	private Textbox kode;

	/** Komponen pencarian dan pemilihan akun akuntansi terkait jenis pembayaran. */
	private AmbilDataAkunBanbox akun;

	/**
	 * <h3>doBeforeCompose &mdash; Verifikasi Keamanan Sebelum Render Halaman</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memastikan keamanan akses sebelum halaman manajemen jenis pembayaran barang
	 * dirender. Dipanggil oleh ZK sebelum pembuatan komponen dimulai.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi sesi aktif.
	 * Pengguna yang tidak valid diarahkan ke halaman login secara otomatis.
	 * Setelah itu memanggil {@code super.doBeforeCompose} untuk melanjutkan
	 * proses lifecycle ZK normal dan mengembalikan hasilnya.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Sesi tidak valid ditangani oleh redirect internal {@code doCheckSecurity}.
	 * Tidak ada exception eksplisit dari metode ini.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pertahankan pemanggilan {@code doCheckSecurity} sebagai lapisan keamanan wajib.
	 * Selalu panggil {@code super.doBeforeCompose} agar lifecycle ZK berjalan benar.</p>
	 *
	 * @param page     halaman ZK yang sedang diproses
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo metadata komponen
	 * @return {@code ComponentInfo} dari kelas induk
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <h3>doAfterCompose &mdash; Inisialisasi Lengkap Halaman Jenis Pembayaran Barang</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode lifecycle ZK yang dipanggil setelah seluruh komponen selesai di-wire.
	 * Melakukan inisialisasi lengkap halaman: pengaturan hak akses tombol, pemuatan
	 * data grid awal, penyiapan paging, serta pendaftaran tombol cetak dan upload
	 * ke toolbar halaman jenis pembayaran barang.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} dan {@code Common.initLaguage()}.</li>
	 *   <li>Mengatur visibilitas tombol Tambah berdasarkan hak CREATE.</li>
	 *   <li>Menetapkan flag {@code edit} dan {@code delete}.</li>
	 *   <li>Memanggil {@code onSearchDefault(null)} untuk memuat data awal.</li>
	 *   <li>Mendaftarkan listener paging untuk reload grid saat halaman berubah.</li>
	 *   <li>Mendaftarkan tombol cetak Excel (kolom: id, kode, nama, akun, keterangan, aktif)
	 *       dan tombol upload impor ke toolbar, dengan visibilitas upload bergantung pada
	 *       kombinasi hak CREATE, UPDATE, DELETE.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari {@code super.doAfterCompose} dibiarkan menyebar. Upload null-check
	 * mencegah NullPointerException jika fitur upload tidak tersedia.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Perbarui array {@code contents} jika menambah atau menghapus kolom pada entitas
	 * {@code JenisPembayaranBarang}. Kolom pada array ini harus berkorespondensi
	 * dengan getter pada entitas.</p>
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

		String[] contents = new String[] { "id", "kode", "nama", "akun", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisPembayaranBarang.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisPembayaranBarang.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <h3>JenisPembayaranBarangRenderer &mdash; Renderer Baris Grid Jenis Pembayaran Barang</h3>
	 *
	 * <p><b>Untuk apa:</b><br>
	 * Inner class renderer yang mengisi setiap baris grid dengan data entitas
	 * {@code JenisPembayaranBarang}. Fitur khas renderer ini adalah checkbox "Aktif"
	 * yang dapat diubah langsung dari grid (inline editing) tanpa perlu membuka popup
	 * form ubah, sehingga operator dapat dengan cepat mengaktifkan atau menonaktifkan
	 * jenis pembayaran tanpa mengganggu alur kerja utama.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Setiap baris menampilkan: kode, nama (dengan RevisiHelper), kolom akun (format
	 * "kode nama"), keterangan, checkbox Aktif yang terintegrasi dengan event listener
	 * {@code onCheck} untuk penyimpanan langsung, dan tombol aksi Salin/Ubah/Hapus.
	 * Checkbox dinonaktifkan ({@code setDisabled(!edit)}) jika pengguna tidak memiliki
	 * hak UPDATE, sehingga read-only user tidak bisa mengubah status aktif.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Urutan penambahan komponen ke baris harus sesuai dengan definisi kolom di file ZUL.
	 * Listener {@code onCheck} menggunakan {@code Common.refreshSaveOrUpdate} langsung
	 * untuk menyimpan perubahan status aktif, bypass validasi {@code onSave}.</p>
	 */
	class JenisPembayaranBarangRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render &mdash; Mengisi Satu Baris Grid dengan Data Jenis Pembayaran Barang</h3>
		 *
		 * <p><b>Tujuan:</b><br>
		 * Metode inti renderer yang mengisi baris ZK dengan data satu entitas
		 * {@code JenisPembayaranBarang}, mencakup kode, nama, akun, keterangan,
		 * checkbox aktif yang bisa diubah langsung, dan tombol aksi.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Cast {@code arg1} ke {@code JenisPembayaranBarang}, lalu menambahkan komponen
		 * secara berurutan ke {@code arg0}: label kode, label nama dengan revisi, label
		 * akun (format "kode nama" atau kosong jika null), label keterangan, checkbox
		 * Aktif dengan listener onCheck yang langsung menyimpan perubahan, dan tombol
		 * Salin/Ubah/Hapus. Checkbox disimpan sebagai atribut baris untuk akses dari
		 * luar jika diperlukan.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Akun null ditangani dengan operator ternary menampilkan string kosong.
		 * Exception dari operasi simpan inline dibiarkan menyebar ke ZK.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Jika format tampilan akun perlu diubah atau checkbox perlu validasi tambahan
		 * saat toggle, modifikasi bagian yang sesuai di dalam metode ini.</p>
		 *
		 * @param arg0 baris ZK {@code Row} yang akan diisi konten
		 * @param arg1 objek data {@code JenisPembayaranBarang} untuk baris ini
		 * @throws Exception jika terjadi error saat pembuatan komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisPembayaranBarang jenisPembayaranBarang = (JenisPembayaranBarang) arg1;
			new Label(jenisPembayaranBarang.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(JenisPembayaranBarang.class, jenisPembayaranBarang,
					jenisPembayaranBarang.getNama()).setParent(arg0);
			new Label(jenisPembayaranBarang.getAkun() == null ? ""
					: (jenisPembayaranBarang.getAkun().getKode() + " " + jenisPembayaranBarang.getAkun().getNama()))
					.setParent(arg0);
			new Label(jenisPembayaranBarang.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisPembayaranBarang.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPembayaranBarang.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisPembayaranBarang);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisPembayaranBarang, JenisPembayaranBarangAction.this)
					.setParent(arg0);

		}

	}

	/**
	 * <h3>onAdd &mdash; Membuka Form Tambah Data Jenis Pembayaran Barang Baru</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Event handler yang dipicu saat pengguna mengklik tombol "Tambah" pada toolbar.
	 * Menyiapkan dan menampilkan form kosong untuk penginputan jenis pembayaran baru.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat instance {@code JenisPembayaranBarang} baru (ID null = data belum ada di
	 * database), lalu memanggil {@code init(jenisPembayaranBarang)} yang membangun konten
	 * form popup. Popup kemudian ditampilkan dalam mode modal.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception diteruskan ke ZK framework melalui {@code throws Exception}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak perlu dimodifikasi kecuali perilaku pembukaan popup berubah fundamental.
	 * Untuk mengubah isi form, modifikasi metode {@code init(JenisPembayaranBarang)}.</p>
	 *
	 * @param event event ZK dari klik tombol Tambah
	 * @throws Exception jika terjadi error saat membangun komponen form popup
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisPembayaranBarang());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <h3>init(GeneralValueObject) &mdash; Implementasi Interface DataInitDefault</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi interface {@code DataInitDefault} yang memungkinkan sistem impor data
	 * (upload Excel) untuk membuka form edit dengan data jenis pembayaran tertentu.
	 * Bertindak sebagai adaptor antara sistem impor generik dan form spesifik ini.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Melakukan cast {@code obj} ke {@code JenisPembayaranBarang}, menyimpan ke field
	 * instance, lalu mendelegasikan ke {@code init(JenisPembayaranBarang)} untuk membangun
	 * form. Setelah form siap, menampilkan popup dalam mode modal.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * ClassCastException akan terjadi jika {@code obj} bukan {@code JenisPembayaranBarang};
	 * ini dianggap error programmer dan dibiarkan menyebar.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak perlu dimodifikasi kecuali tipe parameter interface berubah.</p>
	 *
	 * @param obj objek data yang harus dapat di-cast ke {@code JenisPembayaranBarang}
	 * @throws Exception jika terjadi error saat membangun atau menampilkan form
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisPembayaranBarang = (JenisPembayaranBarang) obj;
		init(jenisPembayaranBarang);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <h3>init(JenisPembayaranBarang) &mdash; Membangun Ulang Konten Form Popup Jenis Pembayaran</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode internal yang membangun ulang seluruh konten popup {@code addWindow} secara
	 * programatik untuk form tambah atau ubah data jenis pembayaran barang. Pendekatan
	 * rebuild memastikan form selalu bersih dari data sesi sebelumnya.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menyimpan referensi entitas dan mengatur judul popup.</li>
	 *   <li>Membersihkan konten popup lama dan membangun layout baru.</li>
	 *   <li>Grid form dua kolom (30%/sisa) dengan baris-baris: Kode (opsional, input bebas),
	 *       Nama (wajib), Akun (wajib, via {@code AmbilDataAkunBanbox}), Keterangan.</li>
	 *   <li>Toolbar South dengan tombol Batal (sembunyikan popup) dan Simpan (panggil
	 *       {@code onSave}, tutup popup jika berhasil).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari komponen ZK menyebar ke caller. Nilai null pada entitas
	 * ditangani dengan nilai string kosong atau null check pada akun.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Untuk menambah field baru (misalnya flag khusus atau referensi entitas lain),
	 * tambahkan baris form baru dan perbarui {@code onSave} untuk menetapkan nilainya.</p>
	 *
	 * @param jenisPembayaranBarang entitas yang akan ditampilkan; ID null = tambah baru, tidak null = ubah
	 * @throws Exception jika terjadi error saat membangun komponen ZK
	 */
	private void init(JenisPembayaranBarang jenisPembayaranBarang) throws Exception {
		this.jenisPembayaranBarang = jenisPembayaranBarang;
		addWindow.setTitle(jenisPembayaranBarang.getId() == null ? "Tambah Cara Bayar" : "Ubah Cara Bayar");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Cara Bayar"));
		row.appendChild(kode = new Textbox(jenisPembayaranBarang.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Cara Bayar *"));
		row.appendChild(nama = new Textbox(jenisPembayaranBarang.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Cara Bayar *"));
		row.appendChild(akun = new AmbilDataAkunBanbox(false));
		akun.setValue(jenisPembayaranBarang.getAkun() == null ? "" : jenisPembayaranBarang.getAkun().getNama());
		akun.setAttribute("akun", jenisPembayaranBarang.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisPembayaranBarang.getKeterangan()));
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
	 * <h3>onSave &mdash; Memvalidasi dan Menyimpan Data Jenis Pembayaran Barang</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memvalidasi seluruh input form dan menyimpan data jenis pembayaran barang ke
	 * database. Memastikan bahwa setiap jenis pembayaran memiliki nama yang valid dan
	 * akun akuntansi yang terhubung sebagai syarat minimal data yang dapat disimpan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Validasi nama tidak boleh kosong; tampilkan peringatan dan kembalikan
	 *       {@code false} jika kosong.</li>
	 *   <li>Validasi akun tidak boleh null (cek atribut "akun" pada {@code AmbilDataAkunBanbox});
	 *       tampilkan peringatan dengan ikon seru dan kembalikan {@code false} jika null.</li>
	 *   <li>Jika mode ubah (ID tidak null), reload entitas dari sesi Hibernate menggunakan
	 *       {@code session.load()} untuk managed state.</li>
	 *   <li>Set kode, nama, akun, dan keterangan dari input form ke entitas.</li>
	 *   <li>Simpan dengan {@code Common.refreshSaveOrUpdate} dan kembalikan {@code true}.</li>
	 * </ol>
	 * Catatan: berbeda dengan controller lain, metode ini tidak memeriksa duplikasi nama
	 * karena jenis pembayaran diidentifikasi juga oleh kode unik, bukan hanya nama.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Validasi ditangani dengan dialog pesan ZK. Error database menyebar melalui
	 * {@code throws Exception}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika perlu menambah validasi duplikasi nama, tambahkan metode
	 * {@code checkNamaJenisPembayaranBarang()} serupa dengan controller lain dan
	 * panggil sebelum blok simpan. Jika menambah field, tambahkan setter setelah
	 * blok reload entitas.</p>
	 *
	 * @param event event ZK dari klik tombol Simpan
	 * @return {@code true} jika data berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi error pada operasi database
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Cara Bayar belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama cara pembayaran; (2) Nama ini digunakan untuk mengidentifikasi metode pembayaran dalam transaksi; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (akun.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Cara Pembayaran belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Akun; (2) Pilih akun jurnal yang sesuai untuk cara pembayaran ini; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisPembayaranBarang.getId() != null) {
			jenisPembayaranBarang = (JenisPembayaranBarang) session.load(JenisPembayaranBarang.class,
					jenisPembayaranBarang.getId());

		}

		jenisPembayaranBarang.setKode(kode.getValue());
		jenisPembayaranBarang.setNama(nama.getValue());
		jenisPembayaranBarang.setAkun((Akun) akun.getAttribute("akun"));
		jenisPembayaranBarang.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisPembayaranBarang);

		return true;
	}

	/**
	 * <h3>initCriteria &mdash; Membangun Kriteria Query untuk Pencarian Jenis Pembayaran</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Membangun {@code Criteria} Hibernate yang berisi kondisi filter pencarian jenis
	 * pembayaran barang, mencakup filter status aktif dan filter nama. Implementasi
	 * interface {@code DataCriteria} yang digunakan oleh {@code onSearchDefault}
	 * dan sistem cetak/ekspor.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat kriteria untuk {@code JenisPembayaranBarang} dengan kondisi bertingkat:
	 * <ul>
	 *   <li>Filter aktif: jika {@code searchaktif} null atau dicentang, menerapkan filter
	 *       {@code aktif IS NULL OR aktif = true} (tampilkan yang aktif dan yang belum
	 *       ditetapkan); jika tidak dicentang, menerapkan {@code sqlRestriction("true")}
	 *       (tampilkan semua termasuk yang nonaktif).</li>
	 *   <li>Filter nama: ilike ANYWHERE jika teks tidak kosong, atau true jika kosong.</li>
	 *   <li>Pengurutan nama A-Z jika parameter {@code order} bernilai true.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception Hibernate dibiarkan menyebar ke caller.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika perlu menambah filter (misalnya filter berdasarkan kode atau akun),
	 * tambahkan {@code criteria.add(...)} baru. Logika filter aktif dapat diperluas
	 * untuk mendukung filter tiga status (Semua/Aktif/Nonaktif) jika diperlukan.</p>
	 *
	 * @param order jika {@code true}, hasil diurutkan nama A-Z; {@code false} untuk COUNT
	 * @return {@code Criteria} siap dieksekusi sesuai filter aktif
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPembayaranBarang.class)
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
	 * <h3>onSearchDefault &mdash; Memuat Ulang Data Grid Jenis Pembayaran dengan Filter Aktif</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi interface {@code DataSearchDefault} yang memuat ulang grid data
	 * jenis pembayaran barang berdasarkan filter aktif saat ini. Dipanggil pada saat
	 * halaman pertama kali dimuat, saat filter berubah, saat paging berubah, dan setelah
	 * operasi simpan atau hapus.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memperbarui komponen paging dengan total baris menggunakan {@code initCriteria(false)},
	 * kemudian mengambil data halaman aktif menggunakan {@code initCriteria(true)} dengan
	 * batas baris dan offset dari paging. Membungkus hasil dalam {@code SimpleListModel}
	 * dan menetapkannya ke grid dengan renderer baru.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Error Hibernate dibiarkan menyebar. {@code @SuppressWarnings("unchecked")} menangani
	 * casting generik dari {@code list()} Hibernate.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika jumlah baris per halaman perlu diubah, modifikasi konstanta
	 * {@code Common.ROWS_COUNT_ON_PAGE} secara global.</p>
	 *
	 * @param event event ZK pemicu; dapat null jika dipanggil programatik
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPembayaranBarang> jenisPembayaranBarang = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisPembayaranBarang);
		grid.setRowRenderer(new JenisPembayaranBarangRenderer());
		grid.setModelCheckMobile(strset);

	}

}
