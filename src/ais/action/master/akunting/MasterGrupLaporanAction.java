package ais.action.master.akunting;

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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
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
import ais.database.dao.DaoFactory;
import ais.database.dao.akunting.MasterGrupLaporanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.MasterGrupLaporan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>MasterGrupLaporanAction — Kontroler CRUD Master Grup Laporan Keuangan</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini mengelola data master <em>Grup Laporan</em> dalam modul akunting sistem
 * eCampus. Grup Laporan adalah pengelompokan tertinggi dalam hierarki laporan keuangan
 * — misalnya "Aktiva", "Kewajiban", "Pendapatan" — yang menentukan bagian mana sebuah
 * kelompok akun akan ditampilkan dalam laporan keuangan. Setiap grup laporan memiliki:
 * <ul>
 *   <li>Nomor urut: menentukan posisi grup dalam laporan (tampil dari kecil ke besar).</li>
 *   <li>Nama: diambil dari konstanta yang sudah didefinisikan ({@link MasterGrupLaporan#AKTIVA},
 *       {@link MasterGrupLaporan#KEWAJIBAN}, {@link MasterGrupLaporan#PENDAPATAN}) atau
 *       nilai lain yang dimasukkan langsung.</li>
 *   <li>Flag tampilkan akun rinci: menentukan apakah akun individual ditampilkan
 *       atau hanya totalnya.</li>
 *   <li>Sub grup (keterangan): deskripsi sub-klasifikasi dalam grup.</li>
 * </ul></p>
 *
 * <p><b>Cara kerja:</b><br>
 * Controller ini merupakan subkelas dari {@link GenericAutowireComposer} ZK Framework
 * dan mengimplementasikan antarmuka {@link DataCriteria}, {@link DataSearchDefault},
 * dan {@link DataInitDefault}. Field-field UI di-wire otomatis dari ZUL.
 * Alur kerja utama:</p>
 * <ol>
 *   <li>{@code doBeforeCompose} memverifikasi keamanan akses.</li>
 *   <li>{@code doAfterCompose} memeriksa sesi, menambahkan tombol cetak dan unggah
 *       ke toolbar, mengkonfigurasi hak akses, dan menjalankan pencarian awal.</li>
 *   <li>{@code onSearchDefault} menggunakan {@code initCriteria} untuk query data
 *       dan memperbarui grid.</li>
 *   <li>{@code init(MasterGrupLaporan)} membangun formulir dengan combobox nama
 *       yang diisi dari nilai yang sudah ada di database ATAU dari konstanta default
 *       jika database masih kosong.</li>
 *   <li>{@code onSave} memvalidasi nama dan menyimpan melalui {@link MasterGrupLaporanDao}.</li>
 *   <li>{@code initCriteria} membangun query Hibernate dengan filter nama.</li>
 * </ol>
 *
 * <p><b>Logika pemilihan nama unik:</b><br>
 * Combobox nama di formulir diisi dari daftar nama yang sudah ada di database
 * ({@code Projections.groupProperty("nama")}). Jika database masih kosong (daftar kosong),
 * tiga nama default (AKTIVA, KEWAJIBAN, PENDAPATAN) digunakan sebagai pilihan awal.
 * Pengguna juga bisa mengetikkan nama lain langsung di combobox.</p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh metode dieksekusi di thread event ZK. Sesi Hibernate ({@code HibernateUtil.currentSession()})
 * dikelola framework dan tidak boleh ditutup manual.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Jika konstanta nama baru ditambahkan ke {@link MasterGrupLaporan}, tambahkan
 * juga sebagai default item di blok {@code else} dalam {@code init}. Jika entitas
 * mendapat kolom baru, tambahkan input di formulir dan setter di {@code onSave}.</p>
 *
 * @author eCampus Dev Team
 * @see MasterGrupLaporan
 * @see MasterGrupLaporanDao
 * @see DataCriteria
 * @see DataSearchDefault
 * @see DataInitDefault
 */
public class MasterGrupLaporanAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Versi serial untuk serialisasi kelas. Menjaga kompatibilitas deserialisasi.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZK untuk formulir tambah/ubah data grup laporan. */
	private MyWindow addWindow;

	/** Grid ZK yang menampilkan daftar data grup laporan. */
	private MyGrid grid;

	/** Kotak teks untuk filter pencarian berdasarkan nama grup laporan. */
	private Textbox searchnama;

	/**
	 * Combobox untuk memilih atau mengetikkan nama grup laporan.
	 * Diisi secara dinamis dari nilai yang sudah ada di database,
	 * atau dari konstanta default jika database masih kosong.
	 */
	private Combobox nama;

	/**
	 * Kotak teks untuk input sub grup laporan (keterangan).
	 * Digunakan sebagai deskripsi sub-klasifikasi dalam grup laporan utama.
	 */
	private Textbox keterangan;

	/**
	 * Kotak input angka untuk menentukan nomor urut tampil grup laporan
	 * dalam laporan keuangan. Semakin kecil nilainya, semakin awal posisinya.
	 */
	private MyIntbox nomorUrut;

	/** Flag apakah pengguna memiliki hak akses ubah (UPDATE). */
	private boolean edit = false;

	/** Flag apakah pengguna memiliki hak akses hapus (DELETE). */
	private boolean delete = false;

	/** Entitas MasterGrupLaporan yang sedang diproses dalam siklus CRUD aktif. */
	private MasterGrupLaporan masterGrupLaporan;

	/** Tombol toolbar untuk membuka formulir penambahan data baru. */
	private MyToolbarbuttonConfig add;

	/**
	 * Checkbox untuk menentukan apakah akun dalam grup ini ditampilkan secara
	 * rinci (per akun individual) atau hanya totalnya saja dalam laporan keuangan.
	 */
	private MyCheckboxConfig tampilkanAkunRinci;

	/**
	 * Melakukan pemeriksaan keamanan sebelum komponen ZUL dibangun.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memproteksi halaman manajemen grup laporan dari akses tanpa otorisasi.
	 * Pemeriksaan dilakukan di awal siklus hidup halaman sebelum UI dirender.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mendelegasikan ke {@code Common.doCheckSecurity()} untuk verifikasi sesi
	 * dan privilege akses modul, kemudian meneruskan ke metode induk ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak boleh dihapus — ini adalah gerbang keamanan pertama halaman ini.</p>
	 *
	 * @param page     halaman ZK yang sedang dibangun
	 * @param parent   komponen induk controller
	 * @param compInfo informasi metadata komponen dari ZUL
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
	 * Menyelesaikan inisialisasi halaman: memeriksa sesi, mengkonfigurasi hak akses,
	 * menambahkan tombol cetak dan unggah, kemudian mengisi grid dengan data awal.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk autowiring ZK.</li>
	 *   <li>Memverifikasi sesi dan privilege READ; jika tidak valid, redirect logout.</li>
	 *   <li>Mengkonfigurasi visibilitas dan tooltip tombol Tambah.</li>
	 *   <li>Menyimpan flag hak akses edit dan delete untuk digunakan renderer.</li>
	 *   <li>Menambahkan tombol cetak data ke toolbar melalui {@code Common.cetakData}.
	 *       Array {@code contents} mendefinisikan field-field yang akan diekspor:
	 *       id, nomorUrut, nama, tampilkanAkunRinci, dan keterangan.</li>
	 *   <li>Menambahkan tombol unggah data ke toolbar. Hanya terlihat jika pengguna
	 *       memiliki hak tambah, ubah, dan hapus sekaligus.</li>
	 *   <li>Menjalankan pencarian awal dengan {@code onSearchDefault(null)}.</li>
	 * </ol></p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika sesi tidak valid, metode keluar lebih awal setelah redirect logout.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada field baru yang perlu diekspor, tambahkan ke array {@code contents}.
	 * Perhatikan bahwa urutan elemen dalam array menentukan urutan kolom di ekspor.</p>
	 *
	 * @param comp komponen ZK root dari halaman ini
	 * @throws Exception jika terjadi kesalahan saat inisialisasi
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		String[] contents = new String[] { "id", "nomorUrut", "nama", "tampilkanAkunRinci", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, MasterGrupLaporan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		onSearchDefault(null);
	}

	/**
	 * Kelas renderer dalam untuk menampilkan setiap baris data {@link MasterGrupLaporan} pada grid.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Merender satu baris data grup laporan ke dalam Row ZK, menampilkan nomor urut,
	 * nama grup (dengan riwayat revisi), status tampilkan akun rinci, keterangan sub grup,
	 * dan tombol aksi yang dikelola secara generik.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Kolom yang ditampilkan:
	 * <ol>
	 *   <li>Nomor urut sebagai Label ({@code getNomorUrut().toString()}).</li>
	 *   <li>Nama grup melalui {@code RevisiHelper.createNewRevisi} untuk riwayat perubahan.</li>
	 *   <li>Status tampilkan akun rinci: "Ya" atau "Tidak" berdasarkan boolean.</li>
	 *   <li>Keterangan sub grup sebagai Label biasa.</li>
	 *   <li>Tombol aksi (ubah dan hapus) melalui {@code Common.copyEditDeleteButtons}
	 *       yang secara otomatis menangani logika ubah (memanggil {@code init(obj)})
	 *       dan hapus (konfirmasi + {@code Common.refreshDelete}).</li>
	 * </ol>
	 * Penggunaan {@code Common.copyEditDeleteButtons} memungkinkan tombol ini
	 * bekerja dengan antarmuka generik {@link DataInitDefault} yang diimplementasikan
	 * oleh kelas luar.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada kolom baru, tambahkan Label sebelum pemanggilan
	 * {@code Common.copyEditDeleteButtons}. Pastikan urutan komponen sesuai
	 * dengan header kolom di ZUL.</p>
	 */
	class MasterGrupLaporanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data MasterGrupLaporan ke dalam Row ZK.
		 *
		 * <p><b>Tujuan:</b><br>
		 * Mengkonversi objek domain {@link MasterGrupLaporan} menjadi representasi
		 * visual berupa komponen ZK yang mencakup semua field tampilan dan tombol aksi.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Mengatur alignment Row ke "top". Menambahkan Label nomor urut, komponen
		 * RevisiHelper untuk nama, Label "Ya"/"Tidak" untuk flag rinci, Label keterangan,
		 * dan komponen tombol aksi generik. Tombol aksi menggunakan {@code MasterGrupLaporanAction.this}
		 * sebagai referensi controller untuk delegate ke metode {@code init(GeneralValueObject)}.</p>
		 *
		 * @param arg0 baris ZK (Row) tempat komponen ditambahkan
		 * @param arg1 objek data, harus berupa {@link MasterGrupLaporan}
		 * @throws Exception jika terjadi kesalahan saat membuat komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			MasterGrupLaporan masterGrupLaporan = (MasterGrupLaporan) arg1;

			new Label(masterGrupLaporan.getNomorUrut().toString()).setParent(arg0);
			RevisiHelper.createNewRevisi(MasterGrupLaporan.class, masterGrupLaporan, masterGrupLaporan.getNama())
					.setParent(arg0);
			new Label(masterGrupLaporan.getTampilkanAkunRinci() ? "Ya" : "Tidak").setParent(arg0);
			new Label(masterGrupLaporan.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, masterGrupLaporan, MasterGrupLaporanAction.this).setParent(arg0);
		}

	}

	/**
	 * Menangani event klik tombol "Tambah" untuk membuka formulir grup laporan baru.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memulai siklus penambahan data baru dengan membuat entitas kosong dan
	 * membuka formulir input untuk pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat objek {@link MasterGrupLaporan} baru tanpa ID, memanggil metode
	 * privat {@code init(MasterGrupLaporan)} untuk membangun formulir kosong,
	 * lalu menampilkan jendela modal.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak perlu diubah kecuali ada nilai default yang perlu diisi saat formulir
	 * pertama dibuka (misalnya nomor urut default).</p>
	 *
	 * @param event event ZK yang dipicu saat tombol Tambah diklik
	 * @throws Exception jika terjadi kesalahan saat membangun atau menampilkan formulir
	 */
	public void onAdd(Event event) throws Exception {
		init(new MasterGrupLaporan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membangun formulir tambah/ubah data grup laporan secara programatik.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Membangun seluruh isi formulir dialog {@code addWindow} untuk operasi
	 * tambah maupun ubah data {@link MasterGrupLaporan}. Formulir ini memiliki
	 * beberapa keunikan dibanding formulir CRUD lain: combobox nama diisi secara
	 * dinamis dari data yang sudah ada, dan ada checkbox untuk flag tampilan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menyimpan referensi entitas dan mengatur judul jendela.</li>
	 *   <li>Membersihkan konten jendela lama.</li>
	 *   <li>Membangun {@link Borderlayout} dengan area Center dan South.</li>
	 *   <li>Pada area Center, membuat grid 2 kolom dengan baris-baris formulir:
	 *       <ul>
	 *         <li>Baris 1: "Nomor Urut" + {@link MyIntbox} dengan nilai awal
	 *             dari {@code getNomorUrut()}.</li>
	 *         <li>Baris 2: "Nama Grup" + Combobox yang diisi dari query
	 *             {@code Projections.groupProperty("nama")} untuk mendapatkan
	 *             nilai nama yang sudah ada secara unik. Jika hasil query kosong
	 *             (database masih kosong), tiga item default ditambahkan: AKTIVA,
	 *             KEWAJIBAN, dan PENDAPATAN. Item yang sesuai dipilih dengan
	 *             {@code Common.selectComboItem}.</li>
	 *         <li>Baris keterangan informatif via {@code Common.initKeterangan}
	 *             untuk memberi petunjuk bahwa pengguna bisa mengetik nama lain.</li>
	 *         <li>Baris 3: Label kosong + Checkbox "Tampilkan akun secara rinci"
	 *             dengan nilai awal dari {@code getTampilkanAkunRinci()}.</li>
	 *         <li>Baris 4: "Sub Grup" + Textbox multiline (3 baris) untuk keterangan.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Pada area South, membuat toolbar dengan tombol Batal dan Simpan.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika konstanta nama baru ditambahkan ke {@link MasterGrupLaporan}, tambahkan
	 * item baru di blok {@code else} agar tersedia sebagai pilihan default.
	 * Perhatikan bahwa metode ini menggunakan {@code @SuppressWarnings("unchecked")}
	 * untuk query list yang tidak terparametrisasi.</p>
	 *
	 * @param masterGrupLaporan entitas yang datanya ditampilkan; ID null = mode tambah
	 */
	@SuppressWarnings("unchecked")
	private void init(MasterGrupLaporan masterGrupLaporan) {
		this.masterGrupLaporan = masterGrupLaporan;
		addWindow.setTitle(masterGrupLaporan.getId() == null ? "Tambah Master Grup Laporan" : "Ubah Master Grup Laporan");
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Urut"));
		row.appendChild(nomorUrut = new MyIntbox(masterGrupLaporan.getNomorUrut()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Grup"));

		nama = new Combobox();
		List<String> namas = HibernateUtil.currentSession().createCriteria(MasterGrupLaporan.class)
				.setProjection(Projections.groupProperty("nama")).list();
		if (!namas.isEmpty()) {
			for (String n : namas) {
				MyComboitemConfig comboitem = new MyComboitemConfig(n);
				comboitem.setValue(n);
				nama.appendChild(comboitem);
			}
		} else {
			MyComboitemConfig comboitem = new MyComboitemConfig(MasterGrupLaporan.AKTIVA);
			comboitem.setValue(MasterGrupLaporan.AKTIVA);
			nama.appendChild(comboitem);
			comboitem = new MyComboitemConfig(MasterGrupLaporan.KEWAJIBAN);
			comboitem.setValue(MasterGrupLaporan.KEWAJIBAN);
			nama.appendChild(comboitem);
			comboitem = new MyComboitemConfig(MasterGrupLaporan.PENDAPATAN);
			comboitem.setValue(MasterGrupLaporan.PENDAPATAN);
			nama.appendChild(comboitem);
		}

		row.appendChild(nama);
		Common.selectComboItem(nama, masterGrupLaporan.getNama());
		nama.setWidth("90%");

		Common.initKeterangan(rows, "Ketikkan nama lain jika tidak ada di pilihan");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(tampilkanAkunRinci = new MyCheckboxConfig("Tampilkan akun secara rinci"));
		tampilkanAkunRinci.setChecked(masterGrupLaporan.getTampilkanAkunRinci());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sub Grup"));
		row.appendChild(keterangan = new Textbox(
				masterGrupLaporan.getKeterangan() == null ? "" : masterGrupLaporan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
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
	 * Memvalidasi input dan menyimpan atau memperbarui data grup laporan ke basis data.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Menangani validasi input dan persistensi data {@link MasterGrupLaporan}.
	 * Dipanggil saat pengguna mengklik tombol Simpan pada formulir dialog.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li><b>Validasi nama wajib:</b> Memeriksa apakah nilai nama (dari combobox)
	 *       tidak kosong setelah trimming. Nama adalah field wajib karena digunakan
	 *       sebagai judul bagian dalam laporan keuangan.</li>
	 *   <li><b>Load entitas:</b> Jika mode ubah (ID tidak null), memuat ulang entitas
	 *       dari DAO untuk mendapatkan instance yang ter-manage Hibernate.</li>
	 *   <li><b>Set nilai:</b> Menetapkan semua field ke entitas:
	 *       <ul>
	 *         <li>Nomor urut dari {@link MyIntbox#getValue()}.</li>
	 *         <li>Nama dari {@link Combobox#getValue()} (nilai teks yang diketikkan
	 *             atau dipilih, bukan value dari item yang dipilih).</li>
	 *         <li>Flag tampilkan akun rinci dari checkbox.</li>
	 *         <li>Keterangan sub grup dari textbox.</li>
	 *       </ul>
	 *   </li>
	 *   <li><b>Persist:</b> Memanggil {@code update()} atau {@code save()} pada DAO.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak ada pengecekan duplikasi di sini — beberapa entri dengan nama yang sama
	 * diperbolehkan (misalnya dua entri "Aktiva" dengan sub grup berbeda). Jika
	 * kombinasi nama+keterangan harus unik, tambahkan pengecekan yang sesuai.</p>
	 *
	 * @param event event ZK yang memicu penyimpanan
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan saat operasi DAO
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Master Grup Laporan belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nama dengan nama master grup laporan yang sesuai; (2) Pastikan nama tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		MasterGrupLaporanDao masterGrupLaporanDao = DaoFactory.getInstance().getMasterGrupLaporanDao();
		if (masterGrupLaporan.getId() != null) {
			masterGrupLaporan = masterGrupLaporanDao.load(masterGrupLaporan.getId());

		}

		masterGrupLaporan.setNomorUrut(nomorUrut.getValue());
		masterGrupLaporan.setNama(nama.getValue());
		masterGrupLaporan.setTampilkanAkunRinci(tampilkanAkunRinci.isChecked());
		masterGrupLaporan.setKeterangan(keterangan.getValue());

		if (masterGrupLaporan.getId() != null) {
			masterGrupLaporanDao.update(masterGrupLaporan);
		} else {
			masterGrupLaporanDao.save(masterGrupLaporan);
		}

		return true;
	}

	/**
	 * Memuat dan menampilkan daftar grup laporan berdasarkan kata kunci pencarian.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi dari {@link DataSearchDefault}. Memperbarui tampilan grid
	 * dengan data terbaru. Menggunakan {@code initCriteria} untuk membangun
	 * query yang konsisten antara pencarian dan paging.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code initCriteria(true)} untuk mendapatkan query dengan ordering,
	 * membatasi hasil dengan {@code Common.MAX_RESULT}, kemudian menampilkan
	 * hasilnya di grid menggunakan {@link MasterGrupLaporanRenderer}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Halaman ini tidak menggunakan paging. Jika data bisa melebihi MAX_RESULT,
	 * pertimbangkan menambahkan komponen Paging dan mengimplementasikan pola
	 * yang sama dengan controller yang sudah memiliki paging.</p>
	 *
	 * @param event event ZK yang memicu pencarian; dapat {@code null}
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		List<MasterGrupLaporan> masterGrupLaporan = initCriteria(true).setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(masterGrupLaporan);
		grid.setRowRenderer(new MasterGrupLaporanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun kriteria Hibernate untuk query pencarian MasterGrupLaporan.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi dari {@link DataCriteria}. Membangun objek {@link Criteria}
	 * Hibernate yang dapat digunakan baik untuk menghitung total baris (paging)
	 * maupun untuk mengambil data hasil pencarian.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat Criteria pada kelas {@link MasterGrupLaporan} dengan pengurutan
	 * ascending berdasarkan nama dan filter ilike case-insensitive pada kolom nama.
	 * Jika {@code searchnama} kosong, {@code sqlRestriction("true")} digunakan
	 * sebagai filter yang selalu benar (menampilkan semua data). Jika ada isi,
	 * filter substring ({@code ANYWHERE}) diterapkan pada kolom nama.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika perlu menambah filter berdasarkan keterangan atau nomor urut,
	 * tambahkan kondisi {@code .add(Restrictions...)} setelah kondisi yang ada.</p>
	 *
	 * @param order {@code true} jika hasil harus diurutkan; {@code false} untuk
	 *              query tanpa pengurutan (untuk perhitungan jumlah baris paging)
	 * @return objek {@link Criteria} Hibernate yang siap dieksekusi
	 */
	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(MasterGrupLaporan.class).addOrder(Order.asc("nama"))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
	}

	/**
	 * Inisialisasi formulir untuk mode ubah data, dipanggil dari mekanisme generik.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Implementasi dari antarmuka {@link DataInitDefault}. Memungkinkan komponen
	 * generik seperti {@code Common.copyEditDeleteButtons} untuk membuka formulir
	 * ubah dengan data entitas yang dipilih.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Objek {@link GeneralValueObject} di-cast ke {@link MasterGrupLaporan},
	 * kemudian diteruskan ke metode privat {@code init(MasterGrupLaporan)} yang
	 * membangun formulir dengan data yang ada. Setelah itu jendela modal ditampilkan.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * ClassCastException akan terjadi jika {@code obj} bukan {@link MasterGrupLaporan}.
	 * Ini adalah kondisi pemrograman yang tidak seharusnya terjadi.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak perlu diubah kecuali antarmuka {@link DataInitDefault} berubah.</p>
	 *
	 * @param obj objek entitas yang akan diedit, harus berupa {@link MasterGrupLaporan}
	 * @throws Exception jika terjadi kesalahan saat membangun formulir
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		MasterGrupLaporan masterGrupLaporan = (MasterGrupLaporan) obj;
		init(masterGrupLaporan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
