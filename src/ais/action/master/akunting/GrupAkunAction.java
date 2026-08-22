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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
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
import ais.database.dao.akunting.GrupAkunDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.GrupAkun;

/**
 * <h3>GrupAkunAction — Kontroler CRUD Master Grup Akun</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini mengelola data master <em>Grup Akun</em> dalam modul akunting sistem eCampus.
 * Grup Akun adalah pengelompokan akun-akun akunting ke dalam kategori yang lebih besar,
 * misalnya "Aset Lancar", "Kewajiban Jangka Panjang", "Pendapatan Usaha", dan sebagainya.
 * Pengelompokan ini digunakan sebagai dasar penyusunan laporan keuangan seperti neraca
 * dan laporan laba rugi. Halaman ini memungkinkan administrator keuangan untuk
 * menambah, mengubah, dan menghapus grup akun sesuai kebutuhan organisasi.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Controller ini merupakan subkelas dari {@link GenericAutowireComposer} ZK Framework
 * sehingga field-field UI seperti {@code addWindow}, {@code grid}, {@code searchnama},
 * {@code nama}, dan {@code keterangan} di-wire otomatis berdasarkan nama komponen di ZUL.
 * Alur kerja utama:</p>
 * <ol>
 *   <li>{@code doBeforeCompose} melakukan pemeriksaan keamanan awal.</li>
 *   <li>{@code doAfterCompose} memeriksa sesi, menetapkan hak akses tombol,
 *       dan menjalankan pencarian awal untuk mengisi grid.</li>
 *   <li>{@code onSearchDefault} mengambil data dari Hibernate dan memperbarui grid.</li>
 *   <li>{@code onAdd} membuka formulir kosong untuk penambahan grup akun baru.</li>
 *   <li>{@code init(GrupAkun)} membangun formulir secara programatik dan mengisinya
 *       dengan data entitas (untuk ubah) atau kosong (untuk tambah).</li>
 *   <li>{@code onSave} memvalidasi input, mengecek duplikasi, lalu menyimpan
 *       melalui {@link GrupAkunDao}.</li>
 *   <li>{@code checkNamaGrupAkun} memastikan tidak ada duplikasi nama grup akun.</li>
 * </ol>
 *
 * <p>Controller ini menggunakan pola DAO eksplisit ({@link GrupAkunDao}) untuk
 * operasi persist, yang memberikan kontrol penuh atas siklus hidup entitas.</p>
 *
 * <p>Halaman ini tidak menggunakan paging sehingga seluruh data ditampilkan
 * dengan batasan {@code Common.MAX_RESULT} baris.</p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh metode dieksekusi di thread event ZK. Sesi Hibernate yang digunakan
 * ({@code HibernateUtil.currentSession()}) dikelola oleh framework dan tidak boleh
 * ditutup manual oleh kelas ini.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Perhatikan bahwa beberapa pesan validasi dalam kode masih menyebutkan "Agama" —
 * ini adalah sisa copy-paste dari modul lain yang belum diubah. Jika perlu diperbaiki,
 * ganti teks pesan validasi tersebut di {@code onSave} dan {@code checkNamaGrupAkun}.
 * Jika ada kolom baru pada entitas {@link GrupAkun}, tambahkan field di {@code init}
 * dan setter di {@code onSave}.</p>
 *
 * @author eCampus Dev Team
 * @see GrupAkun
 * @see GrupAkunDao
 */
public class GrupAkunAction extends GenericAutowireComposer {

	/**
	 * Versi serial untuk serialisasi kelas. Menjaga kompatibilitas deserialisasi
	 * antar versi deployment.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZK yang digunakan sebagai formulir tambah/ubah data. */
	private MyWindow addWindow;

	/** Grid ZK yang menampilkan daftar data grup akun. */
	private MyGrid grid;

	/** Kotak teks untuk input kata kunci pencarian berdasarkan nama grup akun. */
	private Textbox searchnama;

	/** Kotak teks untuk input nama grup akun pada formulir tambah/ubah. */
	private Textbox nama;

	/** Kotak teks multiline untuk input keterangan grup akun. */
	private Textbox keterangan;

	/** Flag apakah pengguna memiliki hak akses ubah (UPDATE). */
	private boolean edit = false;

	/** Flag apakah pengguna memiliki hak akses hapus (DELETE). */
	private boolean delete = false;

	/** Entitas GrupAkun yang sedang diproses dalam siklus CRUD aktif. */
	private GrupAkun grupAkun;

	/** Tombol toolbar untuk membuka formulir penambahan data baru. */
	private MyToolbarbuttonConfig add;

	/**
	 * Melakukan pemeriksaan keamanan sebelum komponen ZUL dibangun.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Melindungi halaman dari akses tanpa otorisasi dengan memverifikasi
	 * hak akses pengguna sebelum komponen UI apa pun diinisialisasi. Ini
	 * adalah lapisan keamanan pertama dalam siklus hidup halaman ZK.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mendelegasikan pemeriksaan ke {@code Common.doCheckSecurity()} yang
	 * memverifikasi sesi aktif dan privilege modul. Jika gagal, pengguna
	 * diarahkan ke halaman login. Jika berhasil, proses inisialisasi komponen
	 * ZK dilanjutkan melalui {@code super.doBeforeCompose}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak boleh dihapus atau dimodifikasi tanpa mempertimbangkan implikasi
	 * keamanan secara menyeluruh.</p>
	 *
	 * @param page     halaman ZK yang sedang dibangun
	 * @param parent   komponen induk controller
	 * @param compInfo informasi metadata komponen dari ZUL
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
	 * Menyelesaikan inisialisasi halaman setelah proses autowiring ZK selesai.
	 * Di sini dilakukan pemeriksaan sesi, konfigurasi hak akses, dan pengisian
	 * data awal pada grid.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan
	 *       autowiring komponen ZK dari file ZUL.</li>
	 *   <li>Memverifikasi atribut sesi {@code "usersTemp"} dan privilege READ.
	 *       Jika tidak valid, membersihkan sesi dan mengarahkan pengguna ke
	 *       logout menggunakan {@code Common.goLogoff()}.</li>
	 *   <li>Mengatur visibilitas tombol Tambah berdasarkan privilege CREATE.
	 *       Guard null pada {@code add} mencegah NPE jika tombol tidak ada di ZUL.</li>
	 *   <li>Menyimpan flag hak akses update dan delete untuk digunakan renderer.</li>
	 *   <li>Menjalankan pencarian awal dengan {@code onSearchDefault(null)}
	 *       untuk mengisi grid saat halaman pertama kali dibuka.</li>
	 * </ol></p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika sesi tidak valid, metode keluar lebih awal setelah redirect logout.
	 * Pengecualian lain dilempar ke framework ZK untuk penanganan lebih lanjut.</p>
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
	 * Kelas renderer dalam untuk menampilkan setiap baris data {@link GrupAkun} pada grid.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Merender satu baris data grup akun ke dalam komponen ZK Row dengan kolom
	 * nama (beserta tautan revisi), keterangan, dan tombol aksi ubah/hapus.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Objek data di-cast ke {@link GrupAkun}. Nama grup akun ditampilkan melalui
	 * {@code RevisiHelper.createNewRevisi} yang membungkus nama dengan komponen
	 * yang memungkinkan pengguna melihat riwayat perubahan. Keterangan ditampilkan
	 * sebagai Label biasa. Tombol Ubah dan Hapus dibuat secara manual dalam Hbox.
	 * Tombol Ubah membuka formulir edit. Tombol Hapus memerlukan konfirmasi dua
	 * langkah sebelum memanggil {@code Common.refreshDelete}. Jika hapus gagal
	 * karena FK violation, pesan error informatif ditampilkan.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika kolom baru perlu ditampilkan, tambahkan Label sebelum Hbox tombol aksi.</p>
	 */
	class GrupAkunRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data GrupAkun ke dalam komponen ZK Row.
		 *
		 * <p><b>Tujuan:</b><br>
		 * Mengkonversi objek domain {@link GrupAkun} menjadi tampilan visual berupa
		 * komponen ZK yang mencakup nama (dengan riwayat revisi), keterangan,
		 * dan tombol aksi.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Mengatur alignment Row ke "top" untuk konsistensi layout. Menampilkan
		 * nama via RevisiHelper dan keterangan sebagai Label. Membuat Hbox dengan
		 * tombol edit dan hapus. Tombol edit memanggil {@code init(grupAkun)} untuk
		 * membangun dan menampilkan formulir ubah. Tombol hapus menampilkan dialog
		 * konfirmasi; jika dikonfirmasi, memanggil {@code Common.refreshDelete}
		 * dalam blok try-catch untuk menangani potensi FK violation.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Pengecualian saat penghapusan ditangkap dan pesannya ditampilkan kepada
		 * pengguna. {@code Common.tampilErrorJikaAdmin} memastikan detail teknis
		 * hanya terlihat oleh admin.</p>
		 *
		 * @param arg0 baris ZK (Row) tempat komponen ditambahkan
		 * @param arg1 objek data, harus berupa {@link GrupAkun}
		 * @throws Exception jika terjadi kesalahan saat membuat komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			final GrupAkun grupAkun = (GrupAkun) arg1;

			RevisiHelper.createNewRevisi(GrupAkun.class, grupAkun,
					grupAkun.getNama()).setParent(arg0);
			new Label(grupAkun.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(grupAkun);
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

											Common.refreshDelete(grupAkun);

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
	 * Menangani event klik tombol "Tambah" untuk membuka formulir grup akun baru.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memulai siklus penambahan data baru dengan membuat entitas kosong dan
	 * membuka formulir input untuk pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat objek {@link GrupAkun} baru tanpa ID (mode tambah), memanggil
	 * {@code init(GrupAkun)} untuk membangun formulir kosong, lalu menampilkan
	 * jendela modal untuk interaksi pengguna.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak perlu diubah kecuali ada kebutuhan inisialisasi nilai default
	 * tertentu sebelum formulir dibuka.</p>
	 *
	 * @param event event ZK yang dipicu saat tombol Tambah diklik
	 * @throws Exception jika terjadi kesalahan saat membangun atau menampilkan formulir
	 */
	public void onAdd(Event event) throws Exception {
		init(new GrupAkun());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membangun formulir tambah/ubah data grup akun secara programatik.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Membangun dan mengisi formulir dialog {@code addWindow} untuk operasi
	 * tambah maupun ubah data {@link GrupAkun}. Formulir dibuat secara
	 * programatik agar dapat digunakan untuk kedua mode dengan cara yang
	 * konsisten dan mudah dipelihara.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menyimpan referensi entitas dan mengatur judul jendela sesuai mode.</li>
	 *   <li>Membersihkan konten jendela lama dengan {@code Common.clear(addWindow)}.</li>
	 *   <li>Membangun {@link Borderlayout} dengan:
	 *       <ul>
	 *         <li>Area Center berisi grid 2 kolom dengan baris input:
	 *             "Nama Grup Akun" + Textbox, dan "Keterangan" + Textbox multiline.</li>
	 *         <li>Area South berisi toolbar dengan tombol Batal dan Simpan.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Tombol Batal menyembunyikan jendela tanpa menyimpan.</li>
	 *   <li>Tombol Simpan memanggil {@code onSave}, dan jika berhasil,
	 *       memperbarui grid dan menutup jendela.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tambahkan baris formulir baru sebelum deklarasi South jika entitas
	 * mendapatkan kolom baru. Pastikan nilai field baru juga di-set di {@code onSave}.</p>
	 *
	 * @param grupAkun entitas yang datanya akan ditampilkan; ID null = mode tambah
	 */
	private void init(GrupAkun grupAkun) {
		this.grupAkun = grupAkun;
		addWindow.setTitle(grupAkun.getId() == null ? "Tambah Grup Akun" : "Ubah Grup Akun");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Grup Akun"));
		row.appendChild(nama = new Textbox(grupAkun.getNama() == null ? ""
				: grupAkun.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				grupAkun.getKeterangan() == null ? "" : grupAkun
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
	 * Memvalidasi input dan menyimpan atau memperbarui data grup akun ke basis data.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Menangani validasi input dan persistensi data {@link GrupAkun}. Dipanggil
	 * saat pengguna mengklik tombol Simpan pada formulir dialog.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li><b>Validasi nama wajib:</b> Memeriksa apakah nama tidak kosong.
	 *       Catatan: pesan yang ditampilkan masih menyebutkan "Agama" (sisa copy-paste)
	 *       — harus diperbarui menjadi "Grup Akun" pada pemeliharaan berikutnya.</li>
	 *   <li><b>Cek duplikasi:</b> Memanggil {@code checkNamaGrupAkun()} untuk
	 *       memastikan nama belum ada di basis data. Catatan: pesan duplikat
	 *       juga masih menyebutkan "Agama" dan perlu diperbaiki.</li>
	 *   <li><b>Load entitas:</b> Jika mode ubah, memuat ulang entitas dari DAO.</li>
	 *   <li><b>Set nilai:</b> Menetapkan nama dan keterangan ke entitas.</li>
	 *   <li><b>Persist:</b> Memanggil {@code update()} atau {@code save()} pada DAO.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Perbaiki pesan validasi yang masih menyebutkan "Agama" menjadi "Grup Akun".
	 * Jika ada field wajib baru, tambahkan validasinya sebelum pengecekan duplikat.</p>
	 *
	 * @param event event ZK yang memicu penyimpanan
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan saat operasi DAO
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nama dengan nama grup akun yang valid; (2) Pastikan nama tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		/*
		 * if (keterangan.getValue().trim().equals("")) {
		 * MyMessageboxConfig.show("Keterangan harus diisi", "Peringatan",
		 * MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION); return false; }
		 */

		boolean i = checkNamaGrupAkun();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Nama tersebut sudah ada di database. Langkah yang dapat dilakukan: (1) Gunakan nama yang berbeda dan belum pernah diregistrasi; (2) Periksa daftar grup akun yang sudah ada melalui menu pencarian; (3) ulangi proses simpan dengan nama baru. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					1, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		GrupAkunDao grupAkunDao = DaoFactory.getInstance().getGrupAkunDao();
		if (grupAkun.getId() != null) {
			grupAkun = grupAkunDao.load(grupAkun.getId());

		}

		grupAkun.setNama(nama.getValue());
		grupAkun.setKeterangan(keterangan.getValue());

		if (grupAkun.getId() != null) {
			grupAkunDao.update(grupAkun);
		} else {
			grupAkunDao.save(grupAkun);
		}

		return true;
	}

	/**
	 * Memuat dan menampilkan daftar grup akun berdasarkan kata kunci pencarian.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memperbarui tampilan grid dengan data terbaru. Dipanggil saat halaman
	 * pertama dibuka, saat pengguna mencari, dan setelah operasi simpan/hapus.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menggunakan Hibernate Criteria untuk membangun query dengan urutan ascending
	 * berdasarkan nama dan filter ilike (case-insensitive substring search) pada
	 * kolom nama. Hasil dibatasi oleh {@code Common.MAX_RESULT} dan dimasukkan
	 * ke {@link SimpleListModel} lalu ditetapkan ke grid bersama renderer.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika paging perlu ditambahkan di masa mendatang, refactor metode ini untuk
	 * menggunakan pola paging seperti yang ada di controller lain.</p>
	 *
	 * @param event event ZK yang memicu pencarian; dapat {@code null}
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<GrupAkun> agama = session
				.createCriteria(GrupAkun.class)
				.addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("nama", searchnama.getValue(),
						MatchMode.ANYWHERE)).setMaxResults(Common.MAX_RESULT)
				.list();
		ListModel strset = new SimpleListModel(agama);
		grid.setRowRenderer(new GrupAkunRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Memeriksa apakah nama grup akun yang diinputkan sudah ada di basis data.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Mencegah penyimpanan data dengan nama yang duplikat. Validasi dilakukan
	 * di level aplikasi sebelum operasi database untuk memberikan pesan error
	 * yang lebih ramah pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menggunakan Hibernate Criteria dengan proyeksi {@code rowCount()} untuk
	 * menghitung baris yang memiliki nama sama (exact match setelah trim).
	 * Untuk mode ubah, kondisi pengecualian ID ditambahkan agar entitas yang
	 * sedang diedit tidak dianggap duplikat terhadap dirinya sendiri.
	 * Untuk mode tambah (ID null), kondisi {@code sqlRestriction("1=1")} digunakan
	 * sebagai placeholder yang selalu benar.</p>
	 *
	 * <p><b>Return:</b><br>
	 * {@code true} jika duplikat ditemukan; {@code false} jika nama aman digunakan.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pengecekan saat ini bersifat case-sensitive ({@code Restrictions.eq}).
	 * Jika perlu case-insensitive, ganti dengan {@code Restrictions.ilike}
	 * dengan MatchMode EXACT.</p>
	 *
	 * @return {@code true} jika nama sudah ada di basis data; {@code false} jika belum ada
	 */
	public Boolean checkNamaGrupAkun() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session
				.createCriteria(GrupAkun.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.grupAkun.getId() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ne("id",
						this.grupAkun.getId())).uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
