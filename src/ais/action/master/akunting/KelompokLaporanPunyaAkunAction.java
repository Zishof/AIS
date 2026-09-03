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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.akunting.KelompokLaporanPunyaAkunDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.KelompokLaporan;
import ais.database.model.akunting.KelompokLaporanPunyaAkun;

/**
 * <h3>KelompokLaporanPunyaAkunAction — Kontroler CRUD Pemetaan Akun ke Kelompok Laporan</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini mengelola data master pemetaan antara {@link Akun} akunting dengan
 * {@link KelompokLaporan} (kelompok laporan keuangan). Entitas
 * {@link KelompokLaporanPunyaAkun} merupakan tabel asosiasi (join table) yang menentukan
 * akun mana saja yang termasuk dalam kelompok laporan tertentu. Pemetaan ini digunakan
 * sebagai dasar pengklasifikasian akun saat menghasilkan laporan keuangan seperti
 * laporan posisi keuangan atau laporan laba rugi — sistem mengetahui akun mana yang
 * harus dimasukkan ke bagian laporan yang mana berdasarkan pemetaan ini.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Controller ini merupakan subkelas dari {@link GenericAutowireComposer} ZK Framework.
 * Field-field UI di-wire otomatis berdasarkan nama komponen di ZUL. Berbeda dengan
 * controller lain yang hanya memiliki satu field pencarian, controller ini memiliki
 * <em>dua</em> filter pencarian: {@code searchnama} untuk nama/kode akun dan
 * {@code searchlaporan} untuk keterangan kelompok laporan.</p>
 *
 * <p>Alur kerja utama:</p>
 * <ol>
 *   <li>{@code doBeforeCompose} memverifikasi keamanan akses awal.</li>
 *   <li>{@code doAfterCompose} memeriksa sesi, menginisialisasi combobox kelompok
 *       laporan (diisi dari basis data), mengkonfigurasi hak akses tombol, dan
 *       menjalankan pencarian awal.</li>
 *   <li>{@code onSearchDefault} mengambil data dengan dua join (akun dan kelompokLaporan)
 *       dan dua filter pencarian.</li>
 *   <li>{@code init(KelompokLaporanPunyaAkun)} membangun formulir dengan input
 *       banbox akun dan combobox kelompok laporan.</li>
 *   <li>{@code onSave} memvalidasi kedua field wajib dan menyimpan melalui DAO.</li>
 * </ol>
 *
 * <p>Formulir menggunakan dua komponen input khusus:
 * {@link AmbilDataAkunBanbox} (mode kode, bukan nama) untuk memilih akun, dan
 * Combobox yang diisi dari master {@link KelompokLaporan} untuk memilih kelompok laporan.
 * Combobox diinisialisasi sekali di {@code doAfterCompose} dan digunakan kembali
 * oleh setiap formulir yang dibuka.</p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh metode dieksekusi di thread event ZK. Sesi Hibernate yang digunakan
 * dikelola framework dan tidak boleh ditutup manual.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Jika entitas {@link KelompokLaporanPunyaAkun} mendapat kolom baru, tambahkan
 * di formulir {@code init} dan set nilainya di {@code onSave}. Perhatikan bahwa
 * combobox kelompok laporan adalah field yang sama digunakan ulang antar pemanggilan
 * formulir — pastikan {@code Common.selectComboItem} dipanggil dengan benar untuk
 * mengatur nilai awalnya.</p>
 *
 * @author eCampus Dev Team
 * @see KelompokLaporanPunyaAkun
 * @see KelompokLaporan
 * @see Akun
 * @see KelompokLaporanPunyaAkunDao
 */
public class KelompokLaporanPunyaAkunAction extends GenericAutowireComposer {

	/**
	 * Versi serial untuk serialisasi kelas. Menjaga kompatibilitas deserialisasi.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZK untuk formulir tambah/ubah pemetaan akun-kelompok laporan. */
	private MyWindow addWindow;

	/** Grid ZK yang menampilkan daftar pemetaan akun ke kelompok laporan. */
	private MyGrid grid;

	/** Kotak teks untuk filter pencarian berdasarkan nama atau kode akun. */
	private Textbox searchnama;

	/**
	 * Kotak teks untuk filter pencarian berdasarkan keterangan kelompok laporan.
	 * Filter ini bekerja bersamaan dengan {@code searchnama} untuk menyempurnakan
	 * hasil pencarian.
	 */
	private Textbox searchlaporan;

	/**
	 * Komponen banbox khusus untuk memilih Akun dari daftar akun berdasarkan kode.
	 * Diinisialisasi dengan mode kode ({@code new AmbilDataAkunBanbox(false)})
	 * agar menampilkan dan mengembalikan kode akun, bukan nama.
	 */
	private AmbilDataAkunBanbox akun;

	/**
	 * Combobox untuk memilih kelompok laporan. Diinisialisasi sekali di
	 * {@code doAfterCompose} dengan data dari tabel {@link KelompokLaporan}
	 * dan kemudian digunakan kembali oleh formulir.
	 */
	private Combobox kelompokLaporan;

	/** Flag apakah pengguna memiliki hak akses ubah (UPDATE). */
	private boolean edit = false;

	/** Flag apakah pengguna memiliki hak akses hapus (DELETE). */
	private boolean delete = false;

	/** Entitas yang sedang diproses dalam siklus CRUD aktif. */
	private KelompokLaporanPunyaAkun kelompokLaporanPunyaAkun;

	/** Tombol toolbar untuk membuka formulir penambahan data baru. */
	private MyToolbarbuttonConfig add;

	/**
	 * Melakukan pemeriksaan keamanan sebelum komponen ZUL dibangun.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memproteksi halaman pemetaan akun-kelompok laporan dari akses tanpa
	 * otorisasi. Pemeriksaan dilakukan sebelum komponen UI diinisialisasi.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mendelegasikan ke {@code Common.doCheckSecurity()} untuk verifikasi sesi
	 * dan privilege, kemudian meneruskan ke metode induk {@code super.doBeforeCompose}
	 * untuk kelanjutan inisialisasi komponen ZK secara normal.</p>
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
	 * Menyelesaikan inisialisasi halaman dengan memeriksa sesi, mengisi combobox
	 * kelompok laporan, mengkonfigurasi hak akses, dan mengisi grid data awal.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk autowiring ZK.</li>
	 *   <li>Memverifikasi sesi dan privilege READ; jika tidak valid, redirect logout.</li>
	 *   <li>Menginisialisasi combobox {@code kelompokLaporan} dengan data dari tabel
	 *       {@link KelompokLaporan} menggunakan {@code Common.insertCombo}. Combobox
	 *       ini dibuat baru di sini karena belum di-wire dari ZUL (dikelola secara
	 *       programatik). Field {@code keterangan} dari {@link KelompokLaporan}
	 *       digunakan sebagai teks yang ditampilkan di combobox.</li>
	 *   <li>Mengkonfigurasi visibilitas tombol Tambah.</li>
	 *   <li>Menyimpan flag hak akses edit dan delete.</li>
	 *   <li>Menjalankan pencarian awal dengan {@code onSearchDefault(null)}.</li>
	 * </ol></p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika sesi tidak valid, metode keluar lebih awal setelah redirect logout.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika entity {@link KelompokLaporan} menggunakan field berbeda sebagai label
	 * tampilan (misalnya "nama" bukan "keterangan"), ubah parameter kedua pada
	 * pemanggilan {@code Common.insertCombo}.</p>
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

		Common.insertCombo(kelompokLaporan = new Combobox(), "keterangan", KelompokLaporan.class);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
	}

	/**
	 * Kelas renderer dalam untuk menampilkan setiap baris pemetaan akun-kelompok laporan.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Merender satu baris data {@link KelompokLaporanPunyaAkun} ke dalam Row ZK,
	 * menampilkan kode akun (dengan riwayat revisi), nama akun, kelompok laporan,
	 * dan tombol aksi ubah/hapus.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Objek data di-cast ke {@link KelompokLaporanPunyaAkun}. Kolom yang ditampilkan:
	 * <ol>
	 *   <li>Kode akun melalui {@code RevisiHelper} (dengan null check).</li>
	 *   <li>Nama akun sebagai Label (dengan null check).</li>
	 *   <li>Keterangan kelompok laporan sebagai Label (dengan null check).</li>
	 *   <li>Tombol Ubah dan Hapus dalam Hbox.</li>
	 * </ol>
	 * Semua properti diperiksa null karena relasi ke akun dan kelompok laporan
	 * bersifat nullable (LEFT JOIN digunakan di query).</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada kolom tambahan yang perlu ditampilkan, tambahkan Label sebelum Hbox.</p>
	 */
	class KelompokLaporanPunyaAkunRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data KelompokLaporanPunyaAkun ke dalam Row ZK.
		 *
		 * <p><b>Tujuan:</b><br>
		 * Mengkonversi objek domain {@link KelompokLaporanPunyaAkun} menjadi
		 * representasi visual berupa komponen ZK yang mencakup kode akun, nama akun,
		 * keterangan kelompok laporan, dan tombol aksi.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Mengatur alignment Row ke "top". Menampilkan kode akun melalui RevisiHelper,
		 * nama akun dan keterangan kelompok laporan sebagai Label biasa (keduanya dengan
		 * null check menggunakan operator ternary). Membuat Hbox dengan tombol Ubah
		 * (membuka formulir edit) dan Hapus (konfirmasi dua langkah + refreshDelete).
		 * Penggunaan {@code parseInt} (bukan {@code new Integer}) pada event hapus
		 * merupakan praktik yang sedikit lebih baik.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Pengecualian saat penghapusan ditangkap dengan pesan informatif.
		 * Detail teknis hanya untuk admin via {@code Common.tampilErrorJikaAdmin}.</p>
		 *
		 * @param arg0 baris ZK (Row) tempat komponen ditambahkan
		 * @param arg1 objek data, harus berupa {@link KelompokLaporanPunyaAkun}
		 * @throws Exception jika terjadi kesalahan saat membuat komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			final KelompokLaporanPunyaAkun kelompokLaporanPunyaAkun = (KelompokLaporanPunyaAkun) arg1;

			RevisiHelper.createNewRevisi(KelompokLaporanPunyaAkun.class, kelompokLaporanPunyaAkun,
					kelompokLaporanPunyaAkun.getAkun() == null ? "" : kelompokLaporanPunyaAkun.getAkun().getKode())
					.setParent(arg0);
			new Label(kelompokLaporanPunyaAkun.getAkun() == null ? "" : kelompokLaporanPunyaAkun.getAkun().getNama())
					.setParent(arg0);
			new Label(kelompokLaporanPunyaAkun.getKelompokLaporan() == null ? ""
					: kelompokLaporanPunyaAkun.getKelompokLaporan().getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kelompokLaporanPunyaAkun);
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
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(kelompokLaporanPunyaAkun);

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
	 * Menangani event klik tombol "Tambah" untuk membuka formulir pemetaan baru.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memulai siklus penambahan data dengan membuat entitas pemetaan kosong
	 * dan membuka formulir input untuk pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat objek {@link KelompokLaporanPunyaAkun} baru tanpa ID (mode tambah),
	 * memanggil {@code init} untuk membangun formulir kosong, lalu menampilkan
	 * jendela modal untuk interaksi pengguna.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak perlu diubah kecuali ada nilai default yang perlu diisi saat formulir
	 * pertama kali dibuka.</p>
	 *
	 * @param event event ZK yang dipicu saat tombol Tambah diklik
	 * @throws Exception jika terjadi kesalahan saat membangun atau menampilkan formulir
	 */
	public void onAdd(Event event) throws Exception {
		init(new KelompokLaporanPunyaAkun());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membangun formulir tambah/ubah pemetaan akun-kelompok laporan secara programatik.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Membangun seluruh isi formulir dialog {@code addWindow} untuk operasi
	 * tambah maupun ubah pemetaan {@link KelompokLaporanPunyaAkun}.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menyimpan referensi entitas dan mengatur judul jendela.</li>
	 *   <li>Membersihkan konten jendela lama dengan {@code Common.clear}.</li>
	 *   <li>Membangun {@link Borderlayout} dengan area Center (formulir) dan South (toolbar).</li>
	 *   <li>Pada area Center, membuat grid 2 kolom dengan dua baris formulir:
	 *       <ul>
	 *         <li>Baris 1: "Akun" + {@link AmbilDataAkunBanbox} dengan mode kode
	 *             ({@code false} artinya tampilkan kode bukan nama). Nilai diisi dari
	 *             kode akun jika ada. Entitas akun yang dipilih disimpan sebagai
	 *             atribut "akun" pada komponen banbox.</li>
	 *         <li>Baris 2: "Kelompok Laporan" + Combobox {@code kelompokLaporan}
	 *             yang sudah diisi saat {@code doAfterCompose}. Item yang sesuai
	 *             dengan entitas saat ini dipilih menggunakan
	 *             {@code Common.selectComboItem}.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Pada area South, membuat toolbar dengan tombol Batal dan Simpan.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Combobox kelompok laporan digunakan kembali (tidak dibuat ulang setiap kali
	 * formulir dibuka). Pastikan {@code Common.selectComboItem} selalu dipanggil
	 * untuk memastikan item yang benar dipilih setiap kali formulir dibuka.</p>
	 *
	 * @param kelompokLaporanPunyaAkun entitas yang datanya akan ditampilkan; ID null = mode tambah
	 */
	private void init(KelompokLaporanPunyaAkun kelompokLaporanPunyaAkun) {
		this.kelompokLaporanPunyaAkun = kelompokLaporanPunyaAkun;
		addWindow.setTitle(kelompokLaporanPunyaAkun.getId() == null ? "Tambah KelompokLaporanPunyaAkun" : "Ubah KelompokLaporanPunyaAkun");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun"));
		row.appendChild(akun = new AmbilDataAkunBanbox(false));
		akun.setValue(kelompokLaporanPunyaAkun.getAkun() == null ? "" : kelompokLaporanPunyaAkun.getAkun().getKode());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Laporan"));
		row.appendChild(kelompokLaporan);
		Common.selectComboItem(kelompokLaporan, kelompokLaporanPunyaAkun.getKelompokLaporan());
		kelompokLaporan.setWidth("90%");

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
	 * Memvalidasi input dan menyimpan pemetaan akun-kelompok laporan ke basis data.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Menangani validasi input dan persistensi data {@link KelompokLaporanPunyaAkun}.
	 * Dipanggil saat pengguna mengklik tombol Simpan pada formulir dialog.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li><b>Validasi akun:</b> Memeriksa apakah komponen banbox akun telah terisi
	 *       (atribut "akun" tidak null). Akun adalah entitas wajib untuk pemetaan ini.</li>
	 *   <li><b>Validasi kelompok laporan:</b> Memeriksa apakah item dipilih di combobox
	 *       kelompok laporan. Kelompok laporan adalah entitas wajib lainnya.</li>
	 *   <li><b>Load entitas:</b> Jika mode ubah (ID tidak null), memuat ulang entitas
	 *       dari DAO untuk mendapatkan instance yang ter-manage Hibernate.</li>
	 *   <li><b>Set nilai:</b> Menetapkan akun (dari atribut banbox) dan kelompok
	 *       laporan (dari item yang dipilih di combobox) ke entitas.</li>
	 *   <li><b>Persist:</b> Memanggil {@code update()} atau {@code save()} pada DAO.</li>
	 * </ol></p>
	 *
	 * <p><b>Catatan penting:</b><br>
	 * Validasi akun menggunakan {@code akun.getAttribute("akun")} karena
	 * {@link AmbilDataAkunBanbox} menyimpan entitas yang dipilih sebagai atribut
	 * komponen, bukan sebagai nilai teks. Ini adalah pola yang konsisten dengan
	 * banbox lain dalam sistem.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada field wajib baru, tambahkan validasinya sebelum blok load DAO.
	 * Sejak perbaikan duplikasi lintas kelompok laporan (integritas jurnal penutup), metode
	 * ini menolak simpan bila akun yang dipilih sudah terpetakan ke kelompok laporan LAIN
	 * pada jenis laporan yang sama — lihat {@link KelompokLaporanPunyaAkun} untuk latar
	 * belakang dampaknya pada {@code TutupBukuHelper} dan dashboard akuntansi.</p>
	 *
	 * @param event event ZK yang memicu penyimpanan
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan saat operasi DAO
	 */
	public boolean onSave(Event event) throws Exception {
		if (akun.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Akun melalui field pencarian akun yang tersedia; (2) Pastikan akun yang dibutuhkan sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (kelompokLaporan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Kelompok Laporan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Kelompok Laporan dari dropdown yang tersedia; (2) Pastikan kelompok laporan yang sesuai sudah terdaftar di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Akun akunTerpilih = (Akun) akun.getAttribute("akun");
		KelompokLaporan kelompokTerpilih = (KelompokLaporan) kelompokLaporan.getSelectedItem().getValue();
		KelompokLaporan bentrok = cariBentrokJenisLaporan(akunTerpilih, kelompokTerpilih,
				kelompokLaporanPunyaAkun.getId());
		if (bentrok != null) {
			MyMessageboxConfig.show("Mohon maaf, Akun ini sudah terpetakan ke Kelompok Laporan '"
					+ (bentrok.getKeterangan() == null ? "-" : bentrok.getKeterangan())
					+ "' pada jenis laporan yang sama. Langkah yang dapat dilakukan: (1) Pilih Kelompok Laporan yang berbeda jenis laporannya; (2) Hapus dulu pemetaan akun ini dari kelompok tersebut bila memang ingin dipindah; (3) ulangi proses simpan. Satu akun tidak boleh terhitung di dua baris pada jenis laporan yang sama karena akan melipatgandakan nominal laporan dan jurnal penutup.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		KelompokLaporanPunyaAkunDao kelompokLaporanPunyaAkunDao = DaoFactory.getInstance()
				.getKelompokLaporanPunyaAkunDao();
		if (kelompokLaporanPunyaAkun.getId() != null) {
			kelompokLaporanPunyaAkun = kelompokLaporanPunyaAkunDao.load(kelompokLaporanPunyaAkun.getId());

		}

		kelompokLaporanPunyaAkun.setAkun(akunTerpilih);
		kelompokLaporanPunyaAkun.setKelompokLaporan(kelompokTerpilih);

		if (kelompokLaporanPunyaAkun.getId() != null) {
			kelompokLaporanPunyaAkunDao.update(kelompokLaporanPunyaAkun);
		} else {
			kelompokLaporanPunyaAkunDao.save(kelompokLaporanPunyaAkun);
		}

		return true;
	}

	/**
	 * Mencari baris {@link KelompokLaporanPunyaAkun} LAIN yang memetakan akun yang sama ke
	 * kelompok laporan pada jenis laporan yang sama dengan {@code kelompokTujuan}.
	 *
	 * <p><b>Tujuan:</b> mencegah satu akun terhitung di dua baris cetak sekaligus pada satu
	 * jenis laporan (Neraca/Rugi Laba/Arus Kas) — duplikasi semacam ini melipatgandakan
	 * nominal di {@code LaporanKeuanganCoaHelper}, dashboard akuntansi, dan (paling berat)
	 * jurnal penutup {@code TutupBukuHelper}, karena seluruh konsumen tersebut melakukan
	 * INNER JOIN pada kolom akun tanpa menyaring per kelompok tertentu.</p>
	 *
	 * <p><b>Cakupan sengaja per jenis laporan, bukan per kelompok laporan:</b> dua baris
	 * dengan kelompok laporan berbeda tetap saling bentrok bila keduanya berada pada jenis
	 * laporan yang sama, karena mesin-mesin di atas menyaring akun berdasarkan jenis laporan,
	 * bukan kelompok spesifik.</p>
	 *
	 * @param akun akun yang akan dipetakan
	 * @param kelompokTujuan kelompok laporan tujuan pemetaan
	 * @param idBarisIni id baris {@link KelompokLaporanPunyaAkun} yang sedang diubah (boleh
	 *                   {@code null} untuk baris baru), dikecualikan dari pencarian
	 * @return kelompok laporan lain yang sudah memuat akun ini pada jenis laporan yang sama,
	 *         atau {@code null} bila tidak ada bentrok
	 */
	@SuppressWarnings("unchecked")
	private KelompokLaporan cariBentrokJenisLaporan(Akun akun, KelompokLaporan kelompokTujuan, Long idBarisIni) {
		if (akun == null || kelompokTujuan == null || kelompokTujuan.getJenisLaporan() == null) {
			return null;
		}
		String hql = "select k from KelompokLaporanPunyaAkun k "
				+ "where k.akun = :akun and k.kelompokLaporan.jenisLaporan = :jenisLaporan "
				+ (idBarisIni != null ? "and k.id != :idBarisIni " : "") + "order by k.id";
		org.hibernate.Query query = HibernateUtil.currentSession().createQuery(hql);
		query.setParameter("akun", akun);
		query.setParameter("jenisLaporan", kelompokTujuan.getJenisLaporan());
		if (idBarisIni != null) {
			query.setParameter("idBarisIni", idBarisIni);
		}
		List<KelompokLaporanPunyaAkun> bentrok = query.setMaxResults(1).list();
		return bentrok.isEmpty() ? null : bentrok.get(0).getKelompokLaporan();
	}

	/**
	 * Memuat dan menampilkan daftar pemetaan akun-kelompok laporan berdasarkan filter pencarian.
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memperbarui tampilan grid dengan data terbaru. Mendukung dua filter pencarian
	 * yang bekerja bersamaan: filter nama/kode akun dan filter keterangan kelompok laporan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membangun Hibernate Criteria dengan dua JOIN LEFT:
	 * <ul>
	 *   <li>{@code createAlias("akun", "akun", LEFT_JOIN)}: untuk mengakses field
	 *       akun dalam query dan filter, dengan LEFT JOIN agar baris tanpa akun
	 *       tetap ditampilkan.</li>
	 *   <li>{@code createAlias("kelompokLaporan", "kelompokLaporan", LEFT_JOIN)}:
	 *       untuk mengakses field kelompok laporan dalam query dan filter.</li>
	 * </ul>
	 * Dua filter diterapkan secara bersamaan (AND):
	 * <ol>
	 *   <li>Filter ilike pada {@code kelompokLaporan.keterangan} berdasarkan
	 *       nilai {@code searchlaporan}.</li>
	 *   <li>Filter ilike pada {@code akun.nama} berdasarkan nilai {@code searchnama}.</li>
	 * </ol>
	 * Jika field pencarian kosong, filter ilike dengan string kosong bersifat
	 * "match all" sehingga semua data tetap ditampilkan. Hasil diurutkan
	 * ascending berdasarkan kode akun dan dibatasi {@code Common.MAX_RESULT}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika filter tambahan diperlukan (misalnya berdasarkan kode akun), tambahkan
	 * kondisi Restrictions baru. Pertimbangkan mengganti filter nama dengan OR
	 * antara nama dan kode akun untuk pencarian yang lebih fleksibel.</p>
	 *
	 * @param event event ZK yang memicu pencarian; dapat {@code null}
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<KelompokLaporanPunyaAkun> kelompokLaporanPunyaAkun = session.createCriteria(KelompokLaporanPunyaAkun.class)
				.createAlias("akun", "akun", Criteria.LEFT_JOIN)
				.createAlias("kelompokLaporan", "kelompokLaporan", Criteria.LEFT_JOIN).addOrder(Order.asc("akun.kode"))
				.add(Restrictions.ilike("kelompokLaporan.keterangan", searchlaporan.getValue(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("akun.nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(kelompokLaporanPunyaAkun);
		grid.setRowRenderer(new KelompokLaporanPunyaAkunRenderer());
		grid.setModelCheckMobile(strset);

	}
}
