package ais.action.master.akunting;

import java.util.List;

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
import org.zkoss.zul.Hbox;
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
import ais.database.dao.akunting.DevisiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Devisi;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>DevisiAction — Kontroler CRUD Master Divisi/Departemen Akunting</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini mengelola data master <em>Devisi</em> (Divisi) dalam modul akunting
 * sistem eCampus. Devisi merepresentasikan unit organisasi atau departemen yang
 * digunakan dalam pencatatan transaksi keuangan untuk memungkinkan pelaporan
 * keuangan per unit (cost center / profit center). Contoh divisi: Fakultas Teknik,
 * Rektorat, UPT Perpustakaan, dan sebagainya. Setiap devisi memiliki kode unik
 * dan nama yang digunakan sebagai referensi dalam jurnal dan laporan keuangan.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Controller ini merupakan subkelas dari {@link GenericAutowireComposer} ZK Framework.
 * Field-field UI seperti {@code addWindow}, {@code grid}, {@code searchnama}, {@code kode},
 * {@code nama}, dan {@code keterangan} di-wire otomatis berdasarkan nama komponen di ZUL.
 * Alur kerja utama:</p>
 * <ol>
 *   <li>{@code doBeforeCompose} memverifikasi keamanan akses awal.</li>
 *   <li>{@code doAfterCompose} memeriksa sesi (dua kali untuk memastikan — redundan
 *       namun aman), mengkonfigurasi hak akses tombol, dan menjalankan pencarian awal.</li>
 *   <li>{@code onSearchDefault} mengambil data dari Hibernate dan memperbarui grid.</li>
 *   <li>{@code onAdd} membuka formulir data baru.</li>
 *   <li>{@code init(Devisi)} membangun formulir secara programatik dengan tiga field
 *       input: kode, nama, dan keterangan.</li>
 *   <li>{@code onSave} memvalidasi kode dan nama, mengecek duplikasi kode, lalu
 *       menyimpan melalui {@link DevisiDao}.</li>
 *   <li>{@code checkKodeDevisi} memastikan kode devisi bersifat unik.</li>
 * </ol>
 *
 * <p>Berbeda dengan GrupAkun dan Matauang yang hanya mengecek duplikasi nama,
 * DevisiAction mengecek duplikasi pada field <em>kode</em> (bukan nama).
 * Kode devisi bersifat unik dan digunakan sebagai identifikasi singkat dalam
 * sistem akunting.</p>
 *
 * <p>Halaman ini tidak menggunakan paging, menampilkan semua data hingga
 * batas {@code Common.MAX_RESULT}.</p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh metode dieksekusi di thread event ZK. Sesi Hibernate
 * ({@code HibernateUtil.currentSession()}) dikelola framework dan tidak boleh
 * ditutup manual.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Perhatikan bahwa {@code doAfterCompose} memiliki pengecekan sesi yang redundan
 * (dua kali pengecekan {@code usersTemp}). Ini aman namun dapat disederhanakan
 * pada pemeliharaan berikutnya. Jika entitas {@link Devisi} mendapat kolom baru,
 * tambahkan di {@code init(Devisi)} dan set nilainya di {@code onSave}.</p>
 *
 * @author eCampus Dev Team
 * @see Devisi
 * @see DevisiDao
 */
public class DevisiAction extends GenericAutowireComposer {

	/**
	 * Versi serial untuk serialisasi kelas. Menjaga kompatibilitas deserialisasi.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZK untuk formulir tambah/ubah data devisi. */
	private MyWindow addWindow;

	/** Grid ZK yang menampilkan daftar data devisi. */
	private MyGrid grid;

	/** Kotak teks untuk input kata kunci pencarian berdasarkan nama devisi. */
	private Textbox searchnama;

	/** Kotak teks untuk input nama devisi pada formulir tambah/ubah. */
	private Textbox nama;

	/** Kotak teks multiline untuk input keterangan devisi. */
	private Textbox keterangan;

	/** Flag apakah pengguna memiliki hak akses ubah (UPDATE). */
	private boolean edit = false;

	/** Flag apakah pengguna memiliki hak akses hapus (DELETE). */
	private boolean delete = false;

	/** Entitas Devisi yang sedang diproses dalam siklus CRUD aktif. */
	private Devisi devisi;

	/** Tombol toolbar untuk membuka formulir penambahan data baru. */
	private MyToolbarbuttonConfig add;

	/**
	 * Kotak teks untuk input kode unik devisi pada formulir tambah/ubah.
	 * Kode ini bersifat unik di seluruh sistem dan dikecek duplikasinya
	 * sebelum penyimpanan.
	 */
	private Textbox kode;

	/**
	 * Melakukan pemeriksaan keamanan sebelum komponen ZUL dibangun.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memverifikasi hak akses pengguna sebelum halaman dirender untuk mencegah
	 * akses tidak sah ke data master devisi akunting.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mendelegasikan pemeriksaan ke {@code Common.doCheckSecurity()} yang
	 * memverifikasi sesi aktif dan privilege modul. Jika gagal, pengguna
	 * diarahkan ke halaman login. Kemudian {@code super.doBeforeCompose}
	 * dipanggil untuk melanjutkan inisialisasi komponen ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak boleh dihapus — ini adalah lapisan keamanan pertama halaman ini.</p>
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
	 * Menyelesaikan inisialisasi halaman dan menyiapkan UI yang siap digunakan
	 * pengguna: memeriksa sesi, mengkonfigurasi hak akses, dan mengisi grid awal.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan autowiring.</li>
	 *   <li>Pengecekan sesi pertama: memeriksa {@code "usersTemp"} dan privilege READ.
	 *       Jika tidak valid, membersihkan sesi dan mengarahkan ke logout.</li>
	 *   <li>Pengecekan sesi kedua (redundan): memeriksa {@code "usersTemp"} lagi.
	 *       Ini adalah sisa dari copy-paste dan aman namun tidak diperlukan.</li>
	 *   <li>Mengkonfigurasi visibilitas tombol Tambah dan tooltip-nya.</li>
	 *   <li>Menyimpan flag hak akses edit dan delete.</li>
	 *   <li>Menjalankan pencarian awal dengan {@code onSearchDefault(null)}.</li>
	 * </ol></p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika validasi sesi gagal, metode keluar lebih awal setelah redirect logout.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pengecekan sesi yang redundan (dua kali) dapat disederhanakan menjadi satu
	 * pengecekan pada pemeliharaan berikutnya.</p>
	 *
	 * @param comp komponen ZK root dari halaman ini
	 * @throws Exception jika terjadi kesalahan saat inisialisasi komponen ZK
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (session.getAttribute("usersTemp") == null) {
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
		onSearchDefault(null);
	}

	/**
	 * Kelas renderer dalam untuk menampilkan setiap baris data {@link Devisi} pada grid.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Merender satu baris data devisi ke dalam komponen ZK Row. Setiap baris
	 * menampilkan kode devisi, nama devisi (dengan riwayat revisi), keterangan,
	 * dan tombol aksi ubah/hapus.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Objek data di-cast ke {@link Devisi}. Kode ditampilkan sebagai Label biasa.
	 * Nama ditampilkan melalui {@code RevisiHelper} untuk memungkinkan akses
	 * riwayat perubahan. Keterangan ditampilkan sebagai Label. Tombol aksi dibuat
	 * dalam Hbox: tombol Ubah membuka formulir edit, tombol Hapus memerlukan
	 * konfirmasi sebelum eksekusi penghapusan dengan penanganan error FK violation.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika kolom baru perlu ditampilkan, tambahkan Label sebelum Hbox tombol aksi.
	 * Pastikan urutan komponen sesuai dengan header kolom di file ZUL.</p>
	 */
	class DevisiRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data Devisi ke dalam komponen ZK Row.
		 *
		 * <p><b>Tujuan:</b><br>
		 * Mengkonversi objek domain {@link Devisi} menjadi representasi visual
		 * berupa komponen ZK yang mencakup kode, nama, keterangan, dan tombol aksi.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Mengatur alignment Row ke "top". Menambahkan Label kode dari {@code getKode()},
		 * komponen RevisiHelper untuk nama dari {@code getNama()}, Label keterangan,
		 * dan Hbox berisi tombol Ubah dan Hapus. Tombol Hapus menggunakan dialog
		 * konfirmasi dua langkah. Kesalahan penghapusan karena FK constraint
		 * ditangkap dan pesan informatif ditampilkan.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Pengecualian saat penghapusan ditangkap; detail teknis hanya ditampilkan
		 * kepada admin melalui {@code Common.tampilErrorJikaAdmin}.</p>
		 *
		 * @param arg0 baris ZK (Row) tempat komponen ditambahkan
		 * @param arg1 objek data, harus berupa {@link Devisi}
		 * @throws Exception jika terjadi kesalahan saat membuat komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Devisi devisi = (Devisi) arg1;

			new Label(devisi.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(Devisi.class, devisi, devisi.getNama()).setParent(arg0);
			new Label(devisi.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(devisi);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Question",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(devisi);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	/**
	 * Menangani event klik tombol "Tambah" untuk membuka formulir data devisi baru.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memulai siklus penambahan data baru dengan membuat entitas {@link Devisi}
	 * kosong dan membuka formulir input untuk pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat objek {@link Devisi} baru tanpa ID, memanggil {@code init(Devisi)}
	 * untuk membangun formulir input, lalu menampilkan jendela modal.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak perlu diubah kecuali ada kebutuhan inisialisasi nilai default tertentu.</p>
	 *
	 * @param event event ZK yang dipicu saat tombol Tambah diklik
	 * @throws Exception jika terjadi kesalahan saat membangun atau menampilkan formulir
	 */
	public void onAdd(Event event) throws Exception {
		init(new Devisi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membangun formulir tambah/ubah data devisi secara programatik.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Membangun seluruh isi formulir dialog {@code addWindow} secara programatik
	 * untuk mendukung operasi tambah maupun ubah data {@link Devisi}.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menyimpan referensi entitas dan mengatur judul jendela sesuai mode
	 *       (tambah jika ID null, ubah jika ID ada).</li>
	 *   <li>Membersihkan konten jendela lama.</li>
	 *   <li>Membangun {@link Borderlayout} dengan area Center (formulir input)
	 *       dan area South (toolbar tombol aksi).</li>
	 *   <li>Pada area Center, membuat grid 2 kolom dengan tiga baris formulir:
	 *       <ul>
	 *         <li>Baris 1: "Kode Devisi" + Textbox untuk kode unik devisi.
	 *             Nilai diisi dari {@code devisi.getKode()} atau string kosong.</li>
	 *         <li>Baris 2: "Nama Devisi" + Textbox untuk nama devisi.</li>
	 *         <li>Baris 3: "Keterangan" + Textbox multiline (4 baris) untuk
	 *             keterangan tambahan.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Pada area South, membuat toolbar dengan tombol Batal dan Simpan.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada field baru pada entitas {@link Devisi}, tambahkan baris formulir
	 * setelah baris keterangan dan pastikan nilainya di-set di {@code onSave}.</p>
	 *
	 * @param devisi entitas yang datanya akan ditampilkan; ID null = mode tambah
	 */
	private void init(Devisi devisi) {
		this.devisi = devisi;
		addWindow.setTitle(devisi.getId() == null ? "Tambah Devisi" : "Ubah Devisi");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Devisi"));
		row.appendChild(kode = new Textbox(devisi.getKode() == null ? "" : devisi.getKode()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Devisi"));
		row.appendChild(nama = new Textbox(devisi.getNama() == null ? "" : devisi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(devisi.getKeterangan() == null ? "" : devisi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(4);
		keterangan.setRows(4);

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
	 * Memvalidasi input dan menyimpan atau memperbarui data devisi ke basis data.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Menangani validasi input dan persistensi data {@link Devisi}. Dipanggil
	 * saat pengguna mengklik tombol Simpan pada formulir dialog.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li><b>Validasi kode wajib:</b> Memeriksa apakah kode devisi tidak kosong.
	 *       Kode adalah identifikasi unik devisi sehingga wajib diisi.</li>
	 *   <li><b>Validasi nama wajib:</b> Memeriksa apakah nama devisi tidak kosong.</li>
	 *   <li><b>Cek duplikasi kode:</b> Memanggil {@code checkKodeDevisi()} untuk
	 *       memastikan kode yang diinputkan belum digunakan oleh devisi lain.
	 *       Kode yang sama pada devisi yang sedang diedit dikecualikan dari pengecekan.</li>
	 *   <li><b>Load entitas:</b> Jika mode ubah, memuat ulang entitas dari DAO
	 *       untuk mendapatkan instance yang ter-manage oleh Hibernate.</li>
	 *   <li><b>Set nilai:</b> Menetapkan kode (setelah trim), nama, dan keterangan
	 *       ke entitas.</li>
	 *   <li><b>Persist:</b> Memanggil {@code update()} atau {@code save()} pada DAO
	 *       sesuai mode operasi.</li>
	 * </ol></p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Setiap kegagalan validasi menampilkan pesan yang sesuai dan mengembalikan
	 * {@code false} untuk menghentikan proses simpan. Pengecualian DAO dilempar
	 * ke pemanggil.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada field baru yang wajib diisi, tambahkan validasinya sebelum
	 * pemanggilan {@code checkKodeDevisi()}. Field opsional baru cukup
	 * ditambahkan setter-nya setelah {@code devisi.setKeterangan()}.</p>
	 *
	 * @param event event ZK yang memicu penyimpanan
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan saat operasi DAO
	 */
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Devisi belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Kode Devisi dengan kode yang valid; (2) Pastikan kode tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Devisi belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nama Devisi dengan nama yang valid; (2) Pastikan nama tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		boolean i = checkKodeDevisi();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Kode Devisi sudah ada di database. Langkah yang dapat dilakukan: (1) Gunakan kode yang berbeda dan belum pernah diregistrasi; (2) Periksa daftar devisi yang sudah ada melalui menu pencarian; (3) ulangi proses simpan dengan kode baru. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", 1,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		DevisiDao devisiDao = DaoFactory.getInstance().getDevisiDao();
		if (devisi.getId() != null) {
			devisi = devisiDao.load(devisi.getId());

		}

		devisi.setKode(kode.getValue().trim());
		devisi.setNama(nama.getValue());
		devisi.setKeterangan(keterangan.getValue());

		if (devisi.getId() != null) {
			devisiDao.update(devisi);
		} else {
			devisiDao.save(devisi);
		}
		return true;
	}

	/**
	 * Memuat dan menampilkan daftar devisi berdasarkan kata kunci pencarian.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memperbarui tampilan grid dengan data devisi terbaru dari basis data.
	 * Dipanggil saat halaman pertama dibuka, saat pengguna mencari, dan
	 * setelah operasi simpan atau hapus berhasil.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menggunakan Hibernate Criteria dengan urutan ascending berdasarkan nama
	 * dan filter case-insensitive pada kolom nama dengan mode ANYWHERE.
	 * Filter hanya aktif jika {@code searchnama} tidak kosong (menggunakan
	 * {@code sqlRestriction("true")} jika kosong untuk menampilkan semua data).
	 * Hasil dibatasi oleh {@code Common.MAX_RESULT} dan ditetapkan ke grid
	 * bersama renderer {@link DevisiRenderer}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika filter tambahan diperlukan (misalnya pencarian berdasarkan kode),
	 * tambahkan kondisi OR di dalam Restrictions.</p>
	 *
	 * @param event event ZK yang memicu pencarian; dapat {@code null}
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<Devisi> devisi = session.createCriteria(Devisi.class).addOrder(Order.asc("nama"))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(devisi);
		grid.setRowRenderer(new DevisiRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Memeriksa apakah kode devisi yang diinputkan sudah ada di basis data.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Mencegah penyimpanan devisi dengan kode yang duplikat. Kode devisi
	 * harus unik karena digunakan sebagai identifikasi singkat dalam sistem
	 * akunting dan laporan keuangan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menggunakan Hibernate Criteria dengan proyeksi {@code rowCount()} untuk
	 * menghitung baris yang memiliki kode sama (exact match setelah trim).
	 * Untuk mode ubah, kondisi pengecualian ID ditambahkan agar entitas yang
	 * sedang diedit tidak dianggap duplikat terhadap dirinya sendiri.
	 * Untuk mode tambah (ID null), kondisi {@code sqlRestriction("1=1")} digunakan
	 * sebagai placeholder yang selalu benar (tidak ada pengecualian).</p>
	 *
	 * <p><b>Return:</b><br>
	 * Mengembalikan {@code true} jika kode sudah digunakan oleh devisi lain
	 * (duplikat); {@code false} jika kode aman untuk digunakan.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pengecekan bersifat case-sensitive. Jika perlu case-insensitive (kode
	 * "DIV001" = "div001"), ganti dengan ilike. Pastikan basis data juga memiliki
	 * UNIQUE constraint pada kolom kode untuk mencegah race condition.</p>
	 *
	 * @return {@code true} jika kode sudah ada; {@code false} jika kode belum digunakan
	 */
	public Boolean checkKodeDevisi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Devisi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.devisi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.devisi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
