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
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.akunting.MataUangDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Matauang;

/**
 * <h3>MataUangAction — Kontroler CRUD Master Mata Uang</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini bertanggung jawab mengelola data master <em>Mata Uang</em> (valuta) dalam
 * modul akunting sistem eCampus. Data mata uang digunakan sebagai referensi dalam
 * transaksi keuangan multi-valuta, seperti Rupiah (IDR), Dolar AS (USD), dan lainnya.
 * Melalui halaman ini, administrator dapat melihat daftar mata uang, menambah mata uang
 * baru, mengubah data yang sudah ada, dan menghapus mata uang yang tidak lagi digunakan.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Controller ini merupakan subkelas dari {@link GenericAutowireComposer} ZK Framework.
 * Komponen-komponen UI seperti {@code addWindow}, {@code grid}, {@code searchnama},
 * {@code nama}, dan {@code keterangan} di-wire secara otomatis berdasarkan nama
 * atribut yang cocok dengan id komponen di file ZUL yang bersangkutan.</p>
 *
 * <p>Alur kerja utama kelas ini adalah:</p>
 * <ol>
 *   <li>{@code doBeforeCompose} memverifikasi keamanan akses sebelum halaman dibangun.</li>
 *   <li>{@code doAfterCompose} memeriksa sesi pengguna, menetapkan visibilitas tombol,
 *       membaca flag hak akses, dan menjalankan pencarian awal untuk mengisi grid.</li>
 *   <li>{@code onSearchDefault} memuat data dari Hibernate dan memperbarui grid.</li>
 *   <li>{@code onAdd} membuka formulir untuk menambah mata uang baru.</li>
 *   <li>{@code init(Matauang)} membangun formulir input secara programatik dan mengisinya
 *       dengan data entitas yang akan diedit (atau kosong untuk data baru).</li>
 *   <li>{@code onSave} memvalidasi input, memeriksa duplikasi nama, lalu menyimpan
 *       atau memperbarui data menggunakan {@link MataUangDao}.</li>
 *   <li>{@code checkNamaMatauang} melakukan pengecekan duplikasi nama ke basis data.</li>
 * </ol>
 *
 * <p>Berbeda dengan beberapa Action lain, controller ini menggunakan pola DAO eksplisit
 * ({@link MataUangDao}) untuk operasi persist, bukan metode generik
 * {@code Common.refreshSaveOrUpdate}. Ini memberikan kontrol lebih terhadap siklus
 * hidup entitas.</p>
 *
 * <p>Halaman ini tidak menggunakan komponen Paging sehingga seluruh data ditampilkan
 * sekaligus dengan batasan {@code Common.MAX_RESULT}.</p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh operasi dieksekusi di thread event ZK. Sesi Hibernate yang digunakan adalah
 * sesi yang dikelola framework ({@code HibernateUtil.currentSession()}) dan tidak boleh
 * ditutup secara manual oleh kelas ini.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Jika entitas {@link Matauang} mendapatkan kolom baru, tambahkan field input di
 * {@code init(Matauang)}, tambahkan pengecekan validasi di {@code onSave} jika perlu,
 * dan pastikan nilai field baru tersebut di-set sebelum pemanggilan DAO.</p>
 *
 * @author eCampus Dev Team
 * @see Matauang
 * @see MataUangDao
 */
public class MataUangAction extends GenericAutowireComposer {

	/**
	 * Versi serial untuk serialisasi kelas. Nilai konstan ini memastikan kompatibilitas
	 * deserialisasi antar versi deployment aplikasi.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZK yang digunakan sebagai formulir tambah/ubah data mata uang. */
	private MyWindow addWindow;

	/** Grid ZK yang menampilkan daftar data mata uang. */
	private MyGrid grid;

	/** Kotak teks untuk input kata kunci pencarian berdasarkan nama mata uang. */
	private Textbox searchnama;

	/** Kotak teks untuk input nama mata uang pada formulir tambah/ubah. */
	private Textbox nama;

	/** Kotak teks multiline untuk input keterangan mata uang. */
	private Textbox keterangan;

	/** Flag apakah pengguna memiliki hak akses ubah (UPDATE). */
	private boolean edit = false;

	/** Flag apakah pengguna memiliki hak akses hapus (DELETE). */
	private boolean delete = false;

	/** Entitas Matauang yang sedang diproses (mode tambah atau ubah). */
	private Matauang matauang;

	/** Tombol toolbar untuk membuka formulir penambahan data baru. */
	private MyToolbarbuttonConfig add;

	/**
	 * Melakukan pemeriksaan keamanan sebelum komponen ZUL dibangun.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memastikan bahwa hanya pengguna yang memiliki hak akses sah yang dapat
	 * mengakses halaman manajemen mata uang. Pemeriksaan dilakukan seawal
	 * mungkin dalam siklus hidup halaman untuk mencegah akses tidak sah.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mendelegasikan pemeriksaan ke {@code Common.doCheckSecurity()} yang
	 * memverifikasi sesi pengguna dan privilege akses. Jika gagal, pengguna
	 * akan diarahkan ke halaman login. Jika berhasil, metode induk dipanggil
	 * untuk melanjutkan proses inisialisasi komponen ZK secara normal.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pemanggilan {@code Common.doCheckSecurity()} tidak boleh dihapus karena
	 * ini merupakan lapisan keamanan pertama yang melindungi modul ini.</p>
	 *
	 * @param page     halaman ZK yang sedang dibangun
	 * @param parent   komponen induk controller
	 * @param compInfo informasi metadata komponen dari file ZUL
	 * @return informasi komponen yang diteruskan ke framework ZK
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Menginisialisasi komponen UI setelah semua komponen ZUL selesai di-wire.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Titik utama inisialisasi halaman. Melakukan pemeriksaan sesi, konfigurasi
	 * hak akses tombol, dan menjalankan pencarian awal untuk mengisi grid data.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} agar ZK menyelesaikan
	 *       proses autowiring komponen dari ZUL ke field-field kelas ini.</li>
	 *   <li>Memeriksa atribut sesi {@code "usersTemp"} dan privilege READ.
	 *       Jika salah satu tidak terpenuhi, atribut sesi dibersihkan dan
	 *       pengguna diarahkan ke halaman logout melalui {@code Common.goLogoff()}.
	 *       Pengecekan ini adalah lapisan keamanan kedua setelah {@code doBeforeCompose}.</li>
	 *   <li>Mengatur visibilitas tombol Tambah berdasarkan privilege CREATE.
	 *       Menyetel tooltip teks tombol.</li>
	 *   <li>Menyimpan flag {@code edit} dan {@code delete} untuk digunakan oleh renderer.</li>
	 *   <li>Memanggil {@code onSearchDefault(null)} untuk mengisi grid dengan data awal.</li>
	 * </ol></p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika sesi tidak valid, metode segera keluar setelah memanggil
	 * {@code Common.goLogoff()} menggunakan pernyataan {@code return}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada kebutuhan menambahkan tombol atau komponen baru pada toolbar,
	 * lakukan di sini setelah inisialisasi yang sudah ada.</p>
	 *
	 * @param comp komponen ZK root dari halaman ini
	 * @throws Exception jika terjadi kesalahan saat inisialisasi komponen ZK
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		add.setVisible(CommonPrivilages
				.checkPrevilages(CommonPrivilages.CREATE));
		if (add != null) { add.setTooltiptext("Tambah"); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
	}

	/**
	 * Kelas renderer dalam untuk menampilkan setiap baris data {@link Matauang} pada grid.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Merender satu baris data mata uang ke dalam komponen ZK Row, lengkap dengan
	 * informasi nama mata uang, keterangan, dan tombol aksi ubah/hapus yang
	 * disesuaikan dengan hak akses pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Objek data di-cast ke {@link Matauang} dan ditampilkan kolom per kolom:
	 * nama mata uang melalui {@code RevisiHelper} (agar ada tautan riwayat revisi),
	 * dan keterangan sebagai Label biasa. Tombol aksi dibuat secara manual dengan
	 * Hbox sebagai kontainer. Tombol Ubah memanggil {@code init(matauang)} dan
	 * menampilkan modal. Tombol Hapus menampilkan dialog konfirmasi; jika dikonfirmasi,
	 * memanggil {@code Common.refreshDelete(matauang)} dan memperbarui grid.
	 * Jika hapus gagal karena ada relasi FK, ditampilkan pesan error yang informatif
	 * beserta detail pengecualian.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada kolom baru yang perlu ditampilkan, tambahkan Label atau komponen
	 * yang sesuai sebelum blok Hbox tombol aksi.</p>
	 */
	class MatauangRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data Matauang ke dalam komponen ZK Row.
		 *
		 * <p><b>Tujuan:</b><br>
		 * Mengkonversi objek domain {@link Matauang} menjadi representasi visual
		 * berupa komponen ZK yang mencakup nama, keterangan, dan tombol aksi.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Mengatur alignment Row ke "top", kemudian menambahkan komponen secara
		 * berurutan: nama mata uang via {@code RevisiHelper.createNewRevisi}
		 * (yang membungkus nama dengan tautan riwayat perubahan), keterangan
		 * sebagai Label biasa, dan Hbox berisi tombol Ubah dan Hapus.
		 * Tombol Hapus menggunakan dialog konfirmasi dua langkah sebelum
		 * eksekusi penghapusan, dengan penanganan error untuk kasus FK violation.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Jika penghapusan gagal karena constraint database, pengecualian ditangkap
		 * dan pesan error ditampilkan ke pengguna beserta detail teknisnya.
		 * {@code Common.tampilErrorJikaAdmin(e)} memastikan detail teknis hanya
		 * terlihat oleh administrator sistem.</p>
		 *
		 * @param arg0 baris ZK (Row) tempat komponen akan ditambahkan
		 * @param arg1 objek data, harus berupa {@link Matauang}
		 * @throws Exception jika terjadi kesalahan saat membuat komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			final Matauang matauang = (Matauang) arg1;

			RevisiHelper.createNewRevisi(Matauang.class, matauang,
					matauang.getNama()).setParent(arg0);
			new Label(matauang.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(matauang);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
							"Question", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event)
										throws Exception {
									int i = new Integer(event.getData()
											.toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(matauang);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
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
	 * Menangani event klik tombol "Tambah" untuk membuka formulir data mata uang baru.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Menyediakan jalur masuk ke formulir penambahan mata uang baru dengan
	 * membuat entitas {@link Matauang} kosong dan meneruskannya ke {@code init}.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat objek {@link Matauang} baru tanpa ID (menandakan mode tambah),
	 * memanggil {@code init(Matauang)} untuk membangun formulir, kemudian
	 * menampilkan jendela modal untuk interaksi pengguna.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak perlu diubah kecuali ada logika pra-inisialisasi khusus sebelum
	 * formulir ditampilkan (misalnya mengisi nilai default tertentu).</p>
	 *
	 * @param event event ZK yang dipicu saat tombol Tambah diklik
	 * @throws Exception jika terjadi kesalahan saat membangun atau menampilkan formulir
	 */
	public void onAdd(Event event) throws Exception {
		init(new Matauang());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membangun formulir tambah/ubah data mata uang secara programatik.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Membangun seluruh isi jendela dialog {@code addWindow} secara programatik
	 * untuk mendukung operasi tambah maupun ubah data mata uang. Pendekatan
	 * programatik memungkinkan nilai field diisi berdasarkan data entitas yang
	 * diberikan, sehingga formulir yang sama dapat digunakan untuk kedua mode.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menyimpan referensi entitas ke field instance dan mengatur judul
	 *       jendela berdasarkan mode (tambah jika ID null, ubah jika ID ada).</li>
	 *   <li>Membersihkan konten jendela lama dengan {@code Common.clear(addWindow)}.</li>
	 *   <li>Membangun {@link Borderlayout} dengan area Center (formulir input) dan
	 *       area South (toolbar tombol aksi).</li>
	 *   <li>Pada area Center, membuat grid dengan baris formulir:
	 *       <ul>
	 *         <li>Baris 1: "Nama Matauang" + Textbox yang terisi dengan nilai
	 *             {@code matauang.getNama()} atau string kosong jika null.</li>
	 *         <li>Baris 2: "Keterangan" + Textbox multiline (4 baris) terisi
	 *             dengan keterangan mata uang.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Pada area South, membuat toolbar dengan tombol Batal dan Simpan.
	 *       Tombol Batal menyembunyikan jendela. Tombol Simpan memanggil
	 *       {@code onSave}, memperbarui grid, dan menyembunyikan jendela jika berhasil.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada field baru pada entitas {@link Matauang}, tambahkan baris formulir
	 * di blok Rows dan pastikan field tersebut juga di-set di {@code onSave}.</p>
	 *
	 * @param matauang entitas mata uang yang datanya akan ditampilkan; ID null = mode tambah
	 */
	private void init(Matauang matauang) {
		this.matauang = matauang;
		addWindow.setTitle(matauang.getId() == null ? "Tambah Matauang" : "Ubah Matauang");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Matauang"));
		row.appendChild(nama = new Textbox(matauang.getNama() == null ? ""
				: matauang.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				matauang.getKeterangan() == null ? "" : matauang
						.getKeterangan()));
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
	 * Memvalidasi input dan menyimpan atau memperbarui data mata uang ke basis data.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Menangani proses validasi dan persistensi data {@link Matauang}. Dipanggil
	 * saat pengguna mengklik tombol Simpan pada formulir dialog.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li><b>Validasi nama wajib:</b> Memeriksa apakah nama mata uang tidak
	 *       kosong. Jika kosong, menampilkan dialog peringatan dan mengembalikan
	 *       {@code false}.</li>
	 *   <li><b>Cek duplikasi:</b> Memanggil {@code checkNamaMatauang()} untuk
	 *       memastikan nama yang diinputkan belum ada di basis data (kecuali pada
	 *       entitas yang sedang diedit itu sendiri). Jika duplikat, menampilkan
	 *       peringatan dan mengembalikan {@code false}.</li>
	 *   <li><b>Load entitas:</b> Jika mode ubah (ID tidak null), memuat ulang
	 *       entitas dari DAO untuk mendapatkan instance yang ter-manage oleh
	 *       sesi Hibernate.</li>
	 *   <li><b>Set nilai:</b> Menetapkan nama dan keterangan ke entitas.</li>
	 *   <li><b>Persist:</b> Memanggil {@code matauangDao.update()} atau
	 *       {@code matauangDao.save()} sesuai mode operasi.</li>
	 * </ol></p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Validasi input dilakukan secara bertahap sebelum operasi basis data.
	 * Pengecualian dari DAO akan dilempar ke pemanggil untuk ditangani lebih lanjut.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada field wajib baru, tambahkan validasinya sebelum pemanggilan
	 * {@code checkNamaMatauang()}. Jika ada field opsional baru, tambahkan
	 * setter-nya setelah {@code matauang.setKeterangan()}.</p>
	 *
	 * @param event event ZK yang memicu penyimpanan
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan saat operasi basis data melalui DAO
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Mata Uang belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nama Mata Uang dengan nama yang valid; (2) Pastikan nama tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		/*
		 * if (keterangan.getValue().trim().equals("")) {
		 * MyMessageboxConfig.show("Keterangan harus diisi", "Peringatan",
		 * MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION); return false; }
		 */

		boolean i = checkNamaMatauang();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Nama Mata Uang sudah ada di database. Langkah yang dapat dilakukan: (1) Gunakan nama mata uang yang berbeda dan belum terdaftar; (2) Periksa daftar mata uang yang sudah ada melalui menu pencarian; (3) ulangi proses simpan dengan nama baru. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		MataUangDao matauangDao = DaoFactory.getInstance().getMataUangDao();
		if (matauang.getId() != null) {
			matauang = matauangDao.load(matauang.getId());

		}

		matauang.setNama(nama.getValue());
		matauang.setKeterangan(keterangan.getValue());

		if (matauang.getId() != null) {
			matauangDao.update(matauang);
		} else {
			matauangDao.save(matauang);
		}

		return true;
	}

	/**
	 * Memuat dan menampilkan daftar mata uang berdasarkan kata kunci pencarian.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memperbarui tampilan grid dengan data terbaru dari basis data. Dipanggil
	 * saat halaman dibuka pertama kali dan setiap kali data berubah atau
	 * pengguna melakukan pencarian.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menggunakan Criteria Hibernate langsung (tanpa {@code initCriteria})
	 * untuk membangun query. Query diurutkan ascending berdasarkan nama dan
	 * difilter menggunakan pencarian case-insensitive (ilike) pada kolom nama
	 * dengan mode ANYWHERE. Hasil dibatasi oleh {@code Common.MAX_RESULT}.
	 * Hasilnya dimasukkan ke dalam {@link SimpleListModel} dan ditetapkan
	 * ke grid bersama dengan renderer {@link MatauangRenderer}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika paging perlu ditambahkan, tambahkan komponen Paging di ZUL dan
	 * refactor metode ini untuk menggunakan pola yang sama dengan controller
	 * lain yang sudah mengimplementasikan paging.</p>
	 *
	 * @param event event ZK yang memicu pencarian; dapat {@code null} jika dipanggil
	 *              secara programatik
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<Matauang> matauang = session
				.createCriteria(Matauang.class)
				.addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("nama", searchnama.getValue(),
						MatchMode.ANYWHERE)).setMaxResults(Common.MAX_RESULT)
				.list();
		ListModel strset = new SimpleListModel(matauang);
		grid.setRowRenderer(new MatauangRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Memeriksa apakah nama mata uang yang diinputkan sudah ada di basis data.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Mencegah penyimpanan mata uang dengan nama yang duplikat. Validasi
	 * dilakukan di level aplikasi sebelum operasi INSERT/UPDATE ke basis data,
	 * sehingga pesan error yang ditampilkan lebih ramah pengguna daripada
	 * exception constraint database.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menggunakan Hibernate Criteria dengan proyeksi {@code rowCount()} untuk
	 * menghitung jumlah baris yang memiliki nama sama dengan input (setelah
	 * trimming). Untuk mode ubah, kondisi {@code Restrictions.ne("id", this.matauang.getId())}
	 * ditambahkan agar entitas yang sedang diedit dikecualikan dari perhitungan,
	 * sehingga pengguna tetap bisa menyimpan tanpa mengubah nama.
	 * Untuk mode tambah (ID null), kondisi {@code sqlRestriction("1=1")} digunakan
	 * agar tidak ada pengecualian.</p>
	 *
	 * <p><b>Return:</b><br>
	 * Mengembalikan {@code true} jika ada duplikat (nama sudah ada), dan
	 * {@code false} jika nama belum ada (aman untuk disimpan).</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini melakukan pengecekan case-sensitive karena menggunakan
	 * {@code Restrictions.eq}. Jika diperlukan pengecekan case-insensitive,
	 * ganti dengan {@code Restrictions.ilike("nama", ..., MatchMode.EXACT)}.</p>
	 *
	 * @return {@code true} jika nama sudah ada di basis data (duplikat);
	 *         {@code false} jika nama belum ada
	 */
	public Boolean checkNamaMatauang() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session
				.createCriteria(Matauang.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.matauang.getId() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ne("id",
						this.matauang.getId())).uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
