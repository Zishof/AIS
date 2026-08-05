package ais.action.master.akunting;

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
import ais.database.model.akunting.AkunArusKas;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>AkunArusKasAction — Kontroler CRUD Master Akun Arus Kas</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini bertanggung jawab mengelola data master <em>Akun Arus Kas</em> dalam modul
 * akunting sistem eCampus. Akun Arus Kas adalah entitas yang menghubungkan sebuah
 * {@link Akun} akunting dengan keterangan peruntukannya dalam laporan arus kas perusahaan
 * (misalnya: akun kas masuk dari operasional, kas keluar untuk investasi, dan sebagainya).
 * Halaman ini memungkinkan pengguna yang berwenang untuk melihat daftar, menambah,
 * mengubah, dan menghapus data Akun Arus Kas secara lengkap.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Controller ini merupakan subkelas dari {@link GenericAutowireComposer} ZK Framework,
 * sehingga komponen-komponen UI (seperti {@code addWindow}, {@code paging}, {@code grid},
 * {@code searchnama}) di-<em>autowire</em> secara otomatis berdasarkan nama atribut yang
 * cocok dengan id komponen di file ZUL. Alur umum kerjanya adalah sebagai berikut:</p>
 * <ol>
 *   <li>{@code doBeforeCompose} dipanggil pertama kali untuk memverifikasi hak akses
 *       pengguna sebelum halaman dibangun.</li>
 *   <li>{@code doAfterCompose} dipanggil setelah semua komponen ZUL selesai di-wire;
 *       di sini visibilitas tombol Tambah dikonfigurasi, hak ubah/hapus diperiksa,
 *       paging diinisialisasi, dan pencarian awal dijalankan.</li>
 *   <li>Grid data diisi melalui {@code onSearchDefault} yang membangun {@link Criteria}
 *       Hibernate lewat {@code initCriteria}, kemudian menampilkan hasilnya via
 *       {@link AkunArusKasRenderer}.</li>
 *   <li>Formulir tambah/ubah dibangun secara programatik di dalam {@code init(AkunArusKas)}
 *       yang mengisi komponen input dengan data entitas yang sedang diedit.</li>
 *   <li>Penyimpanan data dilakukan oleh {@code onSave} yang memvalidasi input, memuat
 *       ulang entitas dari sesi Hibernate jika sedang mengubah data yang ada, lalu
 *       memanggil {@code Common.refreshSaveOrUpdate} untuk persist.</li>
 * </ol>
 * <p>Implementasi antarmuka {@link DataCriteria}, {@link DataSearchDefault}, dan
 * {@link DataInitDefault} memungkinkan komponen generik (seperti ekspor Excel dan unggah
 * data) berinteraksi dengan controller ini secara seragam.</p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh metode dalam kelas ini dieksekusi di thread event ZK (ZK Desktop thread)
 * dan tidak memerlukan sinkronisasi tambahan. Sesi Hibernate yang digunakan adalah
 * sesi yang dikelola oleh framework (managed session) yang diperoleh melalui
 * {@code HibernateUtil.currentSession()} — sesi ini tidak boleh ditutup secara manual.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Jika entitas {@link AkunArusKas} mendapatkan kolom baru, tambahkan baris input
 * yang sesuai di dalam metode {@code init(AkunArusKas)} dan pastikan nilai kolom
 * baru tersebut juga di-set di dalam metode {@code onSave}. Jika skema pencarian
 * perlu diperluas (misalnya filter berdasarkan keterangan), tambahkan kondisi
 * {@link Restrictions} yang sesuai di dalam {@code initCriteria}.</p>
 *
 * @author eCampus Dev Team
 * @see AkunArusKas
 * @see Akun
 * @see DataCriteria
 * @see DataSearchDefault
 * @see DataInitDefault
 */
public class AkunArusKasAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Versi serial untuk serialisasi kelas ini. Nilai ini tetap konstan selama
	 * struktur kelas tidak berubah secara signifikan agar deserialisasi berjalan
	 * dengan benar di lingkungan terdistribusi.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZK yang digunakan sebagai formulir tambah/ubah data. */
	private MyWindow addWindow;

	/** Komponen paging ZK untuk navigasi halaman pada daftar data. */
	private Paging paging;

	/** Grid ZK yang menampilkan daftar AkunArusKas. */
	private MyGrid grid;

	/** Kotak teks untuk input kata kunci pencarian berdasarkan nama/kode akun. */
	private Textbox searchnama;

	/** Kotak teks untuk input keterangan pada formulir tambah/ubah. */
	private Textbox keterangan;

	/** Flag apakah pengguna memiliki hak ubah (UPDATE). */
	private boolean edit = false;

	/** Flag apakah pengguna memiliki hak hapus (DELETE). */
	private boolean delete = false;

	/** Entitas AkunArusKas yang sedang diproses (ditambah atau diubah). */
	private AkunArusKas akunArusKas;

	/** Tombol toolbar untuk membuka formulir tambah data baru. */
	private MyToolbarbuttonConfig add;

	/**
	 * Komponen banbox khusus untuk memilih Akun dari daftar akun yang tersedia.
	 * Komponen ini menggunakan popup pencarian akun dan menyimpan entitas
	 * {@link Akun} yang dipilih sebagai atribut dengan kunci "akun".
	 */
	private AmbilDataAkunBanbox akun;

	/**
	 * Melakukan pemeriksaan keamanan sebelum komponen ZUL dibangun.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode ini merupakan titik masuk pertama dalam siklus hidup halaman ZK.
	 * Tujuannya adalah memastikan bahwa pengguna yang mengakses halaman ini
	 * memiliki hak akses yang sah sebelum komponen UI apa pun diinisialisasi.
	 * Dengan melakukan pemeriksaan di sini, kita mencegah pengguna tanpa hak
	 * bahkan melihat struktur halaman.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mendelegasikan pemeriksaan keamanan ke {@code Common.doCheckSecurity()},
	 * yang akan memeriksa apakah sesi pengguna aktif dan apakah pengguna
	 * memiliki privilege READ untuk modul yang bersangkutan. Jika pemeriksaan
	 * gagal, metode tersebut akan melempar pengecualian atau mengarahkan ulang
	 * pengguna ke halaman login sehingga eksekusi tidak melanjutkan ke
	 * {@code doAfterCompose}. Setelah pemeriksaan berhasil, metode induk
	 * ({@code super.doBeforeCompose}) dipanggil untuk melanjutkan proses
	 * inisialisasi komponen ZK secara normal.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika pengguna tidak memiliki hak akses, {@code Common.doCheckSecurity()}
	 * akan menangani pengalihan atau menampilkan pesan error. Metode ini
	 * tidak melempar pengecualian yang perlu ditangani oleh pemanggil.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan menghapus pemanggilan {@code Common.doCheckSecurity()} karena
	 * hal ini akan membuka celah keamanan. Logika keamanan tambahan yang
	 * bersifat per-halaman dapat ditambahkan setelah pemanggilan tersebut.</p>
	 *
	 * @param page     halaman ZK yang sedang dibangun
	 * @param parent   komponen induk tempat controller ini akan dipasang
	 * @param compInfo informasi metadata komponen dari file ZUL
	 * @return informasi komponen yang diteruskan ke framework ZK
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Menginisialisasi komponen UI setelah semua komponen ZUL selesai di-wire.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode ini adalah titik utama inisialisasi halaman. Semua pengaturan awal
	 * seperti visibilitas tombol, pembacaan hak akses pengguna, inisialisasi
	 * paging, penambahan tombol cetak dan unggah, serta eksekusi pencarian
	 * awal dilakukan di sini.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah yang dilakukan secara berurutan adalah:
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} agar framework ZK
	 *       menyelesaikan proses autowiring komponen.</li>
	 *   <li>Memanggil {@code Common.initLaguage()} untuk menginisialisasi label
	 *       bahasa pada UI sesuai dengan lokalisasi yang dikonfigurasi.</li>
	 *   <li>Mengatur visibilitas tombol Tambah berdasarkan hak CREATE pengguna.
	 *       Pengecekan null pada {@code add} dilakukan untuk menghindari NPE
	 *       apabila tombol tersebut tidak ada di ZUL.</li>
	 *   <li>Membaca hak akses UPDATE dan DELETE dan menyimpannya ke field
	 *       {@code edit} dan {@code delete} yang akan digunakan oleh renderer.</li>
	 *   <li>Memanggil {@code onSearchDefault(null)} untuk mengisi grid dengan
	 *       data awal saat halaman pertama kali dibuka.</li>
	 *   <li>Menginisialisasi komponen paging dengan listener yang akan memicu
	 *       ulang pencarian setiap kali pengguna berpindah halaman.</li>
	 *   <li>Menambahkan tombol cetak data (ekspor ke Excel) ke toolbar melalui
	 *       {@code Common.cetakData}.</li>
	 *   <li>Menambahkan tombol unggah data ke toolbar. Tombol ini hanya
	 *       ditampilkan jika pengguna memiliki hak tambah, ubah, dan hapus
	 *       sekaligus.</li>
	 * </ol></p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Metode ini mendeklarasikan {@code throws Exception} karena {@code super.doAfterCompose}
	 * dan beberapa inisialisasi komponen ZK dapat melempar pengecualian. Pengecualian
	 * akan ditangani oleh mekanisme error handling ZK di lapisan atasnya.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada komponen baru yang perlu diinisialisasi saat halaman dibuka,
	 * tambahkan kode inisialisasinya di metode ini. Pastikan urutan pemanggilan
	 * {@code super.doAfterCompose} selalu berada di baris pertama agar
	 * autowiring selesai sebelum kode kustom dieksekusi.</p>
	 *
	 * @param comp komponen ZK root dari halaman ini (biasanya Window atau Div)
	 * @throws Exception jika terjadi kesalahan saat inisialisasi komponen ZK
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

		String[] contents = new String[] { "id", "akun", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(AkunArusKas.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AkunArusKas.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Kelas renderer dalam (inner class) untuk menampilkan setiap baris data
	 * {@link AkunArusKas} pada grid daftar.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Kelas ini bertanggung jawab merender satu baris data pada grid daftar
	 * Akun Arus Kas. Setiap baris menampilkan kode akun, nama akun (dengan
	 * tautan revisi), keterangan, serta tombol aksi (ubah dan hapus) yang
	 * visibilitasnya disesuaikan dengan hak akses pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Metode {@code render} dipanggil oleh ZK untuk setiap elemen dalam
	 * model data grid. Parameter {@code arg1} di-cast ke {@link AkunArusKas}
	 * dan data-datanya ditampilkan sebagai komponen {@link Label} dalam Row.
	 * Kolom-kolom yang ditampilkan adalah:
	 * <ol>
	 *   <li>Kode akun ({@code akunArusKas.getKode()})</li>
	 *   <li>Nama akun dengan tautan riwayat revisi (melalui {@code RevisiHelper})</li>
	 *   <li>Keterangan akun arus kas</li>
	 *   <li>Tombol aksi (Ubah dan Hapus) melalui {@code Common.copyEditDeleteButtons}</li>
	 * </ol>
	 * Tombol Ubah dan Hapus dikelola oleh {@code Common.copyEditDeleteButtons}
	 * yang secara otomatis mengatur visibilitas berdasarkan flag {@code edit}
	 * dan {@code delete} dari kelas luar, serta memanggil metode
	 * {@code init(GeneralValueObject)} dan {@code Common.refreshDelete} masing-masing.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada kolom baru yang perlu ditampilkan, tambahkan komponen Label atau
	 * komponen ZK lain yang sesuai sebelum baris tombol aksi. Pastikan urutan
	 * penambahan komponen ke {@code arg0} (Row) sesuai dengan urutan kolom
	 * yang didefinisikan di file ZUL (header kolom).</p>
	 */
	class AkunArusKasRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data AkunArusKas ke dalam komponen ZK Row.
		 *
		 * <p><b>Tujuan:</b><br>
		 * Mengkonversi objek domain {@link AkunArusKas} menjadi representasi
		 * visual berupa serangkaian komponen ZK (Label, Toolbar, dll.) yang
		 * ditempatkan di dalam baris grid ({@link Row}).</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Metode ini pertama-tama mengatur alignment vertikal baris ke "top"
		 * untuk konsistensi tampilan. Kemudian objek {@code arg1} di-cast ke
		 * {@link AkunArusKas}. Label kode akun dibuat dari {@code getKode()},
		 * kemudian nama akun ditampilkan melalui {@code RevisiHelper.createNewRevisi}
		 * yang membungkusnya dengan komponen yang memungkinkan pengguna melihat
		 * riwayat perubahan data. Keterangan ditampilkan sebagai Label biasa.
		 * Terakhir, tombol aksi dibuat melalui {@code Common.copyEditDeleteButtons}
		 * yang menangani logika ubah (membuka {@code init}) dan hapus (konfirmasi
		 * lalu {@code Common.refreshDelete}) secara generik.</p>
		 *
		 * <p><b>Parameter:</b><br>
		 * {@code arg0} adalah Row ZK yang menjadi kontainer; {@code arg1} adalah
		 * objek data yang harus merupakan instance dari {@link AkunArusKas}.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Jika cast {@code arg1} ke {@link AkunArusKas} gagal (karena model tidak
		 * sesuai), akan terjadi {@code ClassCastException}. Ini menunjukkan
		 * ketidakcocokan antara model yang diset ke grid dan renderer yang digunakan.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Jika entitas {@link AkunArusKas} memiliki kolom baru yang perlu
		 * ditampilkan, tambahkan Label atau komponen yang sesuai sebelum
		 * pemanggilan {@code Common.copyEditDeleteButtons}.</p>
		 *
		 * @param arg0 baris ZK (Row) tempat komponen akan ditambahkan
		 * @param arg1 objek data yang akan dirender, harus berupa {@link AkunArusKas}
		 * @throws Exception jika terjadi kesalahan saat membuat komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final AkunArusKas akunArusKas = (AkunArusKas) arg1;
			new Label(akunArusKas.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(AkunArusKas.class, akunArusKas, akunArusKas.getNama()).setParent(arg0);
			new Label(akunArusKas.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, akunArusKas, AkunArusKasAction.this).setParent(arg0);

		}

	}

	/**
	 * Menangani event klik tombol "Tambah" untuk membuka formulir penambahan data baru.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Menyediakan jalur masuk ke formulir penambahan data baru. Metode ini
	 * dipanggil secara otomatis oleh ZK ketika pengguna mengklik tombol
	 * dengan event "onAdd" pada halaman ini.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat objek {@link AkunArusKas} baru (tanpa ID, menandakan mode tambah)
	 * dan meneruskannya ke metode {@code init(AkunArusKas)} yang akan membangun
	 * formulir secara programatik. Setelah formulir dibangun, jendela modal
	 * {@code addWindow} ditampilkan dan diaktifkan sebagai dialog modal.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini tidak perlu diubah kecuali ada kebutuhan untuk melakukan
	 * validasi atau persiapan data khusus sebelum formulir ditampilkan.</p>
	 *
	 * @param event event ZK yang dipicu saat tombol Tambah diklik
	 * @throws Exception jika terjadi kesalahan saat membangun formulir atau menampilkan modal
	 */
	public void onAdd(Event event) throws Exception {
		init(new AkunArusKas());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Inisialisasi formulir untuk mode ubah data, dipanggil dari mekanisme generik.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi dari antarmuka {@link DataInitDefault}. Metode ini memungkinkan
	 * komponen generik (seperti tombol edit pada {@code Common.copyEditDeleteButtons})
	 * untuk membuka formulir ubah dengan data entitas yang dipilih, tanpa perlu
	 * mengetahui detail implementasi controller ini.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Objek {@link GeneralValueObject} di-cast ke {@link AkunArusKas} kemudian
	 * diteruskan ke metode privat {@code init(AkunArusKas)} yang membangun
	 * formulir dengan data entitas yang ada. Setelah itu, jendela modal
	 * ditampilkan sehingga pengguna dapat mengubah data.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika {@code obj} bukan instance dari {@link AkunArusKas}, akan terjadi
	 * {@code ClassCastException}. Ini adalah kondisi pemrograman yang tidak
	 * seharusnya terjadi karena komponen yang memanggil metode ini harus
	 * sudah tahu tipe entitas yang ditangani.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini tidak perlu diubah kecuali antarmuka {@link DataInitDefault}
	 * berubah. Logika pembangunan formulir ada di metode privat
	 * {@code init(AkunArusKas)}.</p>
	 *
	 * @param obj objek entitas yang akan diedit, harus berupa {@link AkunArusKas}
	 * @throws Exception jika terjadi kesalahan saat membangun formulir
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		akunArusKas = (AkunArusKas) obj;
		init(akunArusKas);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membangun formulir tambah/ubah AkunArusKas secara programatik.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode privat ini membangun seluruh isi formulir dialog (addWindow) secara
	 * programatik menggunakan API ZK. Pendekatan programatik dipilih agar
	 * formulir dapat digunakan ulang untuk mode tambah maupun ubah dengan
	 * cara mengisi nilai awal field berdasarkan data entitas yang diberikan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah pembangunan formulir:
	 * <ol>
	 *   <li>Menyimpan referensi entitas ke field instance dan mengatur judul
	 *       jendela berdasarkan mode (tambah/ubah, dilihat dari {@code getId() == null}).</li>
	 *   <li>Membersihkan konten jendela lama dengan {@code Common.clear(addWindow)}.</li>
	 *   <li>Membangun layout {@link Borderlayout} dengan area Center untuk formulir
	 *       dan area South untuk toolbar tombol aksi.</li>
	 *   <li>Pada area Center, membuat grid 2 kolom dengan baris-baris formulir:
	 *       <ul>
	 *         <li>Baris 1: label "Akun Arus Kas" + komponen {@link AmbilDataAkunBanbox}
	 *             yang memungkinkan pengguna memilih akun dari daftar. Nilai awal
	 *             diisi dari {@code akunArusKas.getAkun().getNama()} dan entitas
	 *             akunnya disimpan sebagai atribut komponen dengan kunci "akun".</li>
	 *         <li>Baris 2: label "Keterangan" + Textbox multiline (3 baris) untuk
	 *             keterangan akun arus kas.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Pada area South, membuat toolbar dengan tombol Batal (menyembunyikan
	 *       jendela) dan tombol Simpan (memanggil {@code onSave}, memperbarui
	 *       grid, lalu menyembunyikan jendela jika berhasil).</li>
	 *   <li>Memasang Borderlayout ke dalam addWindow.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada field baru pada entitas {@link AkunArusKas}, tambahkan baris
	 * formulir baru di dalam blok Rows setelah baris keterangan yang sudah ada.
	 * Pastikan juga nilai field baru tersebut di-set di dalam metode
	 * {@code onSave}.</p>
	 *
	 * @param akunArusKas entitas yang datanya akan ditampilkan di formulir;
	 *                    jika ID-nya null, formulir berjalan dalam mode tambah
	 */
	private void init(AkunArusKas akunArusKas) {
		this.akunArusKas = akunArusKas;
		addWindow.setTitle(akunArusKas.getId() == null ? "Tambah Akun Arus Kas" : "Ubah Akun Arus Kas");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Arus Kas"));
		row.appendChild(akun = new AmbilDataAkunBanbox());
		akun.setValue(akunArusKas.getAkun() == null ? "" : akunArusKas.getAkun().getNama());
		akun.setAttribute("akun", akunArusKas.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(akunArusKas.getKeterangan()));
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
	 * Menyimpan atau memperbarui data AkunArusKas ke basis data.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode ini menangani proses validasi input dan persistensi data entitas
	 * {@link AkunArusKas} ke basis data. Dipanggil saat pengguna mengklik
	 * tombol Simpan pada formulir dialog.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah yang dilakukan:
	 * <ol>
	 *   <li><b>Validasi:</b> Memeriksa apakah komponen akun telah terisi
	 *       (atribut "akun" pada {@code AmbilDataAkunBanbox} tidak null).
	 *       Jika belum diisi, menampilkan dialog peringatan dan mengembalikan
	 *       {@code false} untuk menghentikan proses simpan.</li>
	 *   <li><b>Reload entitas:</b> Jika sedang dalam mode ubah (ID tidak null),
	 *       entitas dimuat ulang dari sesi Hibernate menggunakan {@code session.load()}
	 *       untuk mendapatkan proxy entitas yang ter-attach ke sesi aktif. Ini
	 *       penting agar perubahan dapat dilacak oleh Hibernate.</li>
	 *   <li><b>Set nilai:</b> Menetapkan nilai akun (dari atribut komponen
	 *       banbox) dan keterangan ke entitas.</li>
	 *   <li><b>Persist:</b> Memanggil {@code Common.refreshSaveOrUpdate(session, akunArusKas)}
	 *       yang akan melakukan INSERT (jika baru) atau UPDATE (jika sudah ada)
	 *       melalui sesi Hibernate yang dikelola framework.</li>
	 * </ol></p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Validasi dilakukan sebelum operasi basis data untuk mencegah penyimpanan
	 * data yang tidak lengkap. Jika terjadi kesalahan saat persist, pengecualian
	 * akan dilempar ke pemanggil (listener tombol Simpan) yang akan menanganinya.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada field wajib baru, tambahkan validasi sebelum blok session.load().
	 * Jika ada field baru yang perlu disimpan, tambahkan setter-nya setelah
	 * {@code akunArusKas.setKeterangan}.</p>
	 *
	 * @param event event ZK yang memicu penyimpanan (biasanya klik tombol Simpan)
	 * @return {@code true} jika data berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan saat operasi basis data
	 */
	public boolean onSave(Event event) throws Exception {
		if (akun.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Arus Kas belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Akun Arus Kas melalui field pencarian akun yang tersedia; (2) Pastikan akun yang sesuai sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (akunArusKas.getId() != null) {
			akunArusKas = (AkunArusKas) session.load(AkunArusKas.class, akunArusKas.getId());

		}

		akunArusKas.setAkun((Akun) akun.getAttribute("akun"));
		akunArusKas.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, akunArusKas);

		return true;
	}

	/**
	 * Membangun kriteria Hibernate untuk query pencarian AkunArusKas.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi dari antarmuka {@link DataCriteria}. Metode ini membangun
	 * objek {@link Criteria} Hibernate yang dapat digunakan baik untuk menghitung
	 * total baris (untuk paging) maupun untuk mengambil data halaman tertentu.
	 * Parameter {@code order} menentukan apakah hasil perlu diurutkan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat kriteria pada kelas {@link AkunArusKas} dengan join ke entitas
	 * {@link Akun} melalui {@code createAlias("akun", "akun")}. Join ini
	 * diperlukan agar field-field dari entitas akun dapat digunakan sebagai
	 * parameter pencarian dan pengurutan. Jika parameter {@code order} bernilai
	 * {@code true}, pengurutan ascending berdasarkan kode akun dan kemudian
	 * nama akun ditambahkan. Filter pencarian menggunakan nilai dari
	 * {@code searchnama}: jika kosong, semua data ditampilkan (menggunakan
	 * {@code sqlRestriction("true")}); jika ada isi, dilakukan pencarian
	 * case-insensitive (ilike) pada kolom kode ATAU nama akun dengan mode
	 * {@code ANYWHERE} (substring match).</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Untuk menambah filter baru (misalnya berdasarkan keterangan), tambahkan
	 * kondisi {@code .add(Restrictions.ilike(...))} setelah filter yang sudah ada.
	 * Pastikan alias sudah didefinisikan sebelum digunakan dalam Restrictions.</p>
	 *
	 * @param order {@code true} jika hasil harus diurutkan; {@code false} untuk
	 *              query tanpa pengurutan (digunakan saat menghitung total baris)
	 * @return objek {@link Criteria} Hibernate yang siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AkunArusKas.class).createAlias("akun", "akun");

		if (order)
			criteria.addOrder(Order.asc("akun.kode")).addOrder(Order.asc("akun.nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")

				: Restrictions.or(Restrictions.ilike("akun.kode", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("akun.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

		);
		return criteria;
	}

	/**
	 * Menjalankan pencarian data dan memperbarui tampilan grid AkunArusKas.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi dari antarmuka {@link DataSearchDefault}. Metode ini adalah
	 * inti dari operasi pencarian dan pembaruan tampilan. Dipanggil saat halaman
	 * pertama dibuka, saat pengguna mengklik tombol Cari, saat berpindah halaman
	 * paging, dan setelah operasi simpan atau hapus berhasil.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkahnya:
	 * <ol>
	 *   <li>Memanggil {@code Common.initPaging(initCriteria(false), paging)} untuk
	 *       menghitung total baris hasil pencarian dan mengkonfigurasi komponen
	 *       paging (total halaman, dll.). Query tanpa ordering digunakan untuk
	 *       efisiensi.</li>
	 *   <li>Menjalankan query dengan ordering dan batasan halaman aktif:
	 *       {@code setMaxResults(ROWS_COUNT_ON_PAGE)} dan
	 *       {@code setFirstResult(ROWS_COUNT_ON_PAGE * activePage)} untuk
	 *       mengambil hanya data pada halaman yang sedang aktif.</li>
	 *   <li>Membungkus hasil dalam {@link SimpleListModel} dan menyetel model
	 *       serta renderer ke grid untuk memperbarui tampilan.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini tidak perlu diubah kecuali logika paging berubah. Filter
	 * pencarian dikelola oleh {@code initCriteria}.</p>
	 *
	 * @param event event ZK yang memicu pencarian; dapat berupa {@code null}
	 *              jika dipanggil secara programatik
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<AkunArusKas> akunArusKas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(akunArusKas);
		grid.setRowRenderer(new AkunArusKasRenderer());
		grid.setModelCheckMobile(strset);

	}

}
