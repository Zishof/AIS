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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
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
import ais.action.master.library.helper.PengunaLokasiAction;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.asset.LokasiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;
import ais.database.model.inventory.Toko;
import ais.database.model.sirs.Gudang;
import ais.ui.util.GMapScript;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>LokasiAction — Kontroler CRUD Data Master Lokasi Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah kontroler ZKoss (composer) yang mengelola operasi CRUD lengkap untuk entitas
 * {@link Lokasi}. Data master lokasi digunakan di seluruh modul AIS sebagai referensi tempat
 * fisik, gudang, atau toko yang terhubung dengan aset, inventori, dan kegiatan operasional lainnya.
 * Setiap lokasi dapat dikaitkan dengan koordinat geografis (latitude/longitude untuk peta),
 * alamat fisik lengkap, sebuah entitas {@link Toko} (dari modul inventori), dan sebuah entitas
 * {@link Gudang} (dari modul SIRS). Selain itu, lokasi mendukung pembatasan akses pengguna
 * berdasarkan username yang dikonfigurasi di field detail alamat. Halaman ini memberikan
 * kemampuan pengelolaan terpusat atas seluruh lokasi dalam sistem.
 *
 * <b>Cara kerja:</b><br>
 * Controller ini mewarisi {@code GenericAutowireComposer} ZKoss sehingga komponen yang
 * dideklarasikan di file ZUL dengan ID yang cocok secara otomatis di-wire ke field Java melalui
 * mekanisme autowiring. Saat halaman dimuat, ZKoss memanggil {@code doBeforeCompose} untuk
 * memeriksa keamanan, kemudian {@code doAfterCompose} untuk memverifikasi sesi pengguna, mengatur
 * hak akses, memuat data awal, dan memasang event listener paging. Form tambah/ubah dirender
 * secara programatik di dalam modal window menggunakan {@code Borderlayout}. Penyimpanan data
 * dilakukan melalui {@code LokasiDao} (pola DAO) yang memberikan abstraksi akses database.
 * Fitur unik kelas ini adalah metode statis {@code kunciLokasi} yang mengunci pilihan lokasi
 * pengguna berdasarkan konfigurasi lokasi default di profil {@link Tbmuser}.
 *
 * <b>Threading:</b><br>
 * Seluruh operasi berjalan pada thread event ZKoss. Session HTTP ZKoss (variabel {@code session}
 * yang diwarisi dari {@code GenericAutowireComposer}) digunakan untuk menyimpan atribut sementara.
 * Akses ke {@code HibernateUtil.currentSession()} aman dalam konteks thread event tunggal ZKoss.
 * Metode statis {@code kunciLokasi} dapat dipanggil dari controller lain dan aman selama
 * dipanggil dalam thread event ZKoss yang sama.
 *
 * <b>Pemeliharaan:</b><br>
 * Untuk menambah field baru pada entitas {@link Lokasi}, perlu memperbarui metode privat
 * {@code init(Lokasi)} untuk menambah baris form input, metode {@code onSave} untuk menetapkan
 * nilai field baru, dan inner class {@code LokasiRenderer} untuk menampilkan nilai di grid.
 * Juga perlu memperbarui skema database dan file mapping Hibernate. Jika ada perubahan pada
 * struktur tabel lokasi di database, DDL ALTER TABLE dan file hibernate mapping harus diperbarui
 * secara bersamaan sebelum deploy.
 *
 * @author Tim Pengembang AIS
 * @version 1.0
 * @see Lokasi
 * @see LokasiDao
 * @see PengunaLokasiAction
 */
public class LokasiAction extends GenericAutowireComposer {

	/**
	 * Nomor versi serial untuk serialisasi kelas ini.
	 * Tidak perlu diubah kecuali ada perubahan struktur yang tidak kompatibel mundur.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZKoss yang digunakan untuk menampilkan form tambah atau ubah data lokasi. */
	private MyWindow addWindow;

	/** Komponen paging ZKoss untuk navigasi halaman pada grid daftar lokasi. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar seluruh data lokasi yang tersedia. */
	private MyGrid grid;

	/** Kotak teks pencarian berdasarkan nama lokasi pada area filter di atas grid. */
	private Textbox searchnama;

	/** Checkbox filter untuk menampilkan hanya data aktif (dicentang) atau semua data. */
	private Checkbox searchaktif;

	/** Kotak teks input nama lokasi pada form tambah/ubah data. */
	private Textbox nama;

	/** Kotak teks input keterangan atau deskripsi lokasi pada form tambah/ubah data. */
	private Textbox keterangan;

	/** Kotak teks input alamat utama lokasi pada form tambah/ubah data. */
	private Textbox alamat;

	/**
	 * Kotak teks input detail username pengguna yang dikaitkan dengan lokasi ini.
	 * Nilai berisi daftar username yang dipisahkan koma; pengguna dengan username tersebut
	 * akan otomatis dikunci ke lokasi ini saat mengakses sistem.
	 */
	private Textbox alamatDetail;

	/** Label yang menampilkan nilai koordinat gabungan (format teks) dari lokasi ini. */
	private Label coordinate;

	/** Kotak input bilangan desimal untuk nilai latitude (lintang) koordinat peta lokasi. */
	private MyDoublebox lat;

	/** Kotak input bilangan desimal untuk nilai longitude (bujur) koordinat peta lokasi. */
	private MyDoublebox lng;

	/** Penanda apakah pengguna saat ini memiliki hak akses untuk mengubah data (UPDATE privilege). */
	private boolean edit = false;

	/** Penanda apakah pengguna saat ini memiliki hak akses untuk menghapus data (DELETE privilege). */
	private boolean delete = false;

	/** Referensi ke objek entitas lokasi yang sedang diproses (tambah atau ubah). */
	private Lokasi lokasi;

	/** Tombol "Tambah" pada toolbar halaman, visibilitasnya dikendalikan oleh hak akses CREATE. */
	private MyToolbarbuttonConfig add;

	/** Combobox untuk memilih entitas Toko dari modul inventori yang dikaitkan dengan lokasi ini. */
	private Combobox toko;

	/** Combobox untuk memilih entitas Gudang dari modul SIRS yang dikaitkan dengan lokasi ini. */
	private Combobox gudang;

	/**
	 * Metode siklus hidup ZKoss yang dipanggil sebelum komponen halaman dibangun.
	 *
	 * <b>Tujuan:</b><br>
	 * Melakukan pemeriksaan keamanan akses pengguna sebelum halaman ZUL dirender sepenuhnya,
	 * mencegah akses tidak sah ke halaman manajemen lokasi.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi bahwa pengguna sudah
	 * login dan berwenang mengakses halaman ini. Kemudian mendelegasikan ke implementasi
	 * induk melalui {@code super.doBeforeCompose()} agar proses autowiring ZKoss berjalan normal.
	 * ZKoss memanggil metode ini secara otomatis sebelum proses composing dimulai.
	 *
	 * <b>Parameter:</b><br>
	 * @param page halaman ZKoss saat ini yang sedang dikompilasi oleh framework
	 * @param parent komponen induk dalam hierarki komponen ZUL
	 * @param compInfo informasi metadata komponen yang sedang diproses oleh ZKoss
	 *
	 * <b>Return:</b><br>
	 * @return objek {@code ComponentInfo} dari kelas induk yang berisi informasi komponen
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika pemeriksaan keamanan gagal, {@code Common.doCheckSecurity()} akan menghentikan
	 * proses render dan mengarahkan pengguna ke halaman login.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jangan hapus pemanggilan {@code super.doBeforeCompose()} karena metode induk ini
	 * menginisialisasi mekanisme autowiring ZKoss yang dibutuhkan seluruh halaman.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Metode siklus hidup ZKoss yang dipanggil setelah seluruh komponen halaman selesai dibangun.
	 *
	 * <b>Tujuan:</b><br>
	 * Menginisialisasi seluruh logika bisnis halaman manajemen lokasi, termasuk verifikasi
	 * sesi pengguna, pengaturan hak akses, pemuatan data awal, dan konfigurasi paging.
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan autowiring ZKoss.
	 * (2) Memanggil {@code Common.initLaguage()} untuk mengatur bahasa antarmuka.
	 * (3) Memeriksa atribut sesi {@code usersTemp} dan hak akses READ; jika tidak ada atau
	 *     tidak berwenang, menghapus atribut sesi dan memanggil {@code Common.goLogoff()}
	 *     untuk mengarahkan pengguna ke halaman login.
	 * (4) Mengatur visibilitas tombol Tambah berdasarkan hak akses CREATE.
	 * (5) Menetapkan nilai flag {@code edit} dan {@code delete} berdasarkan hak akses.
	 * (6) Memuat data awal ke grid dengan {@code onSearchDefault(null)}.
	 * (7) Mendaftarkan event listener paging untuk navigasi halaman.
	 *
	 * <b>Parameter:</b><br>
	 * @param comp komponen akar halaman ZUL yang sudah selesai dibangun oleh framework ZKoss
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan selama inisialisasi, misalnya kesalahan
	 *                   koneksi database saat memuat data awal
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Berbeda dengan controller lain, LokasiAction memiliki pemeriksaan sesi tambahan
	 * ({@code usersTemp}) yang perlu dipertahankan karena halaman ini memiliki persyaratan
	 * keamanan lebih ketat untuk menjaga integritas data lokasi sistem.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
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
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	/**
	 * Metode statis utilitas untuk mengunci pemilihan lokasi pada combobox sesuai dengan
	 * lokasi default yang dikonfigurasi di profil pengguna yang sedang login.
	 *
	 * <b>Tujuan:</b><br>
	 * Memastikan pengguna yang memiliki lokasi tetap yang dikonfigurasi di profilnya tidak
	 * dapat mengubah pilihan lokasi ke lokasi lain. Metode ini dipanggil dari controller lain
	 * yang memiliki komponen combobox lokasi agar konsisten dengan pembatasan lokasi pengguna.
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengambil objek {@link Tbmuser} pengguna yang sedang login menggunakan
	 * {@code Common.getCurrentUser()}. Jika pengguna valid, memiliki userId, dan memiliki
	 * lokasi yang dikonfigurasi (tidak null), maka metode ini:
	 * (1) Memilih lokasi yang sesuai di combobox menggunakan {@code Common.selectComboItem}.
	 * (2) Menonaktifkan combobox ({@code setDisabled(true)}) agar pengguna tidak dapat
	 *     mengubah pilihan lokasi.
	 * Jika pengguna tidak memiliki lokasi default yang dikonfigurasi, combobox tetap aktif
	 * dan tidak ada perubahan yang dilakukan.
	 *
	 * <b>Parameter:</b><br>
	 * @param combobox objek {@link Combobox} ZKoss yang akan dikunci sesuai lokasi pengguna;
	 *                 combobox ini harus sudah diisi dengan daftar lokasi yang tersedia
	 *                 sebelum metode ini dipanggil
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Metode ini dipanggil dari berbagai controller yang memerlukan pemilihan lokasi dengan
	 * pembatasan pengguna. Jika logika pembatasan lokasi berubah (misalnya berdasarkan
	 * peran atau satuan kerja), modifikasi hanya perlu dilakukan di satu tempat ini dan
	 * akan berlaku untuk semua controller yang menggunakannya.
	 */
	public static void kunciLokasi(Combobox combobox) {
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getUserId() != null && tbmuser.getLokasi() != null) {
			Common.selectComboItem(true, combobox, tbmuser.getLokasi());
			combobox.setDisabled(true);
		}
	}

	/**
	 * Inner class renderer yang bertanggung jawab merender setiap baris data pada grid
	 * daftar lokasi aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengkonversi objek {@link Lokasi} menjadi baris tampilan yang informatif di grid,
	 * termasuk komponen interaktif untuk mengubah status aktif dan tombol aksi baris.
	 *
	 * <b>Cara kerja:</b><br>
	 * Untuk setiap objek {@link Lokasi} dalam model grid, metode {@code render} dipanggil.
	 * Baris diisi dengan: komponen {@link PengunaLokasiAction} untuk menampilkan info
	 * pengguna terkait lokasi, nama (via RevisiHelper), latitude, longitude, alamat,
	 * keterangan, dan detail alamat (username pengguna). Checkbox Aktif yang dapat diklik
	 * langsung menyimpan perubahan status ke database. Tombol Ubah membuka form edit modal
	 * secara langsung (bukan via {@code Common.copyEditDeleteButtons}) karena logika hapus
	 * lokasi memerlukan penanganan error khusus untuk relasi data. Tombol Hapus menampilkan
	 * konfirmasi sebelum menghapus dan menangkap exception jika data memiliki relasi FK.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Berbeda dengan renderer di controller lain, LokasiRenderer mengimplementasikan tombol
	 * Ubah dan Hapus secara manual (tidak menggunakan {@code Common.copyEditDeleteButtons})
	 * untuk memberikan kontrol penuh atas pesan error saat penghapusan gagal karena relasi FK.
	 */
	class LokasiRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data lokasi ke dalam komponen ZKoss Row.
		 *
		 * <b>Tujuan:</b><br>
		 * Mengisi komponen {@link Row} ZKoss dengan semua data dari satu entitas {@link Lokasi},
		 * termasuk informasi pengguna terkait, data geografis, dan kontrol interaktif.
		 *
		 * <b>Cara kerja:</b><br>
		 * Membuat label-label untuk koordinat, alamat, keterangan, dan detail alamat dari
		 * entitas Lokasi. Menambahkan komponen {@link PengunaLokasiAction} yang merender
		 * tampilan khusus daftar pengguna yang terkait dengan lokasi ini. RevisiHelper
		 * membungkus tampilan nama agar dapat menampilkan riwayat perubahan. Checkbox Aktif
		 * memiliki event listener inline yang langsung menyimpan perubahan ke database.
		 * Tombol Ubah dan Hapus dibuat secara manual dalam {@link Hbox}; tombol hapus
		 * menampilkan dialog konfirmasi dan menangkap exception jika penghapusan gagal
		 * akibat constraint foreign key dengan menampilkan pesan error yang informatif.
		 *
		 * <b>Parameter:</b><br>
		 * @param arg0 objek {@link Row} ZKoss yang akan diisi dengan komponen-komponen data
		 * @param arg1 objek data {@link Lokasi} yang akan dirender sebagai satu baris
		 *
		 * <b>Penanganan error:</b><br>
		 * @throws Exception jika terjadi kesalahan saat membuat atau memasang komponen ZKoss;
		 *                   penghapusan data yang gagal akibat relasi FK ditangkap dan
		 *                   ditampilkan sebagai pesan error kepada pengguna
		 *
		 * <b>Pemeliharaan:</b><br>
		 * Urutan komponen yang ditambahkan ke {@code arg0} harus konsisten dengan urutan
		 * definisi kolom di file ZUL. Pesan error penghapusan dapat disesuaikan agar
		 * lebih deskriptif bagi pengguna akhir.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Lokasi lokasi = (Lokasi) arg1;

			final Label lat = new Label(lokasi.getLat() + "");
			final Label lng = new Label(lokasi.getLng() + "");
			final Label alamat = new Label(lokasi.getAlamat());

			new PengunaLokasiAction(lokasi).setParent(arg0);

			RevisiHelper.createNewRevisi(Lokasi.class, lokasi, lokasi.getNama()).setParent(arg0);
			lat.setParent(arg0);
			lng.setParent(arg0);
			alamat.setParent(arg0);
			new Label(lokasi.getKeterangan()).setParent(arg0);
			new Label(lokasi.getDetailAlamat()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(lokasi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					lokasi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(lokasi);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(lokasi);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(lokasi);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

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
	 * Event handler yang dipanggil ketika pengguna menekan tombol "Tambah" pada toolbar halaman.
	 *
	 * <b>Tujuan:</b><br>
	 * Membuka form tambah data lokasi baru dalam jendela modal yang bersih dan kosong,
	 * siap untuk diisi oleh pengguna dengan informasi lokasi yang baru.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat objek {@link Lokasi} baru (tanpa ID, semua field default null/kosong) dan
	 * meneruskannya ke metode privat {@code init(Lokasi)} yang membangun seluruh komponen
	 * form. Setelah form siap, jendela {@code addWindow} ditampilkan dalam mode modal.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZKoss yang berisi informasi kejadian klik tombol Tambah
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan saat membangun komponen form atau menampilkan
	 *                   jendela modal (misalnya komponen modal belum diinisialisasi dengan benar)
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Dipanggil otomatis oleh ZKoss berdasarkan event binding di ZUL. Jika ada inisialisasi
	 * tambahan yang diperlukan sebelum form dibuka (misalnya memuat data referensi), tambahkan
	 * di sini sebelum pemanggilan {@code init(new Lokasi())}.
	 */
	public void onAdd(Event event) throws Exception {
		init(new Lokasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Metode privat yang membangun seluruh antarmuka form tambah/ubah data lokasi.
	 *
	 * <b>Tujuan:</b><br>
	 * Membersihkan konten jendela modal dan membangun ulang semua komponen form secara
	 * programatik dengan data dari entitas lokasi yang diberikan. Mendukung mode tambah
	 * (objek tanpa ID) dan mode ubah (objek dengan ID yang sudah ada di database).
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menyimpan referensi objek lokasi dan mengatur judul jendela sesuai mode operasi.
	 * (2) Membersihkan komponen lama dari jendela dan menambahkan skrip Google Maps.
	 * (3) Membangun {@code MyBorderlayout} dengan Center berisi grid isian dan South berisi toolbar.
	 * (4) Grid satu kolom dibuat dengan baris untuk: nama lokasi, koordinat (label display),
	 *     latitude (input desimal), longitude (input desimal), keterangan, alamat, pilihan
	 *     toko (combobox dari modul inventori aktif), pilihan gudang (combobox dari SIRS aktif),
	 *     dan detail username pengguna yang terkait.
	 * (5) Setiap input diisi dengan nilai saat ini dari entitas lokasi; combobox toko dan gudang
	 *     diisi dengan data aktif dari database dan pilihan saat ini di-select.
	 * (6) Tombol Batal menutup jendela; tombol Simpan memanggil {@code onSave} dan menutup
	 *     jendela serta merefresh grid jika berhasil.
	 *
	 * <b>Parameter:</b><br>
	 * @param lokasi objek entitas {@link Lokasi} yang berisi data yang akan ditampilkan di form;
	 *               jika {@code getId() == null} maka ini adalah form tambah baru
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Perlu mempertahankan script GMapScript saat mereset form agar fungsionalitas peta
	 * tetap bekerja. Jika ada field baru pada entitas Lokasi, tambahkan baris form yang sesuai
	 * sebelum definisi South/toolbar di akhir metode ini.
	 */
	private void init(Lokasi lokasi) {
		this.lokasi = lokasi;
		addWindow.setTitle(lokasi.getId() == null ? "Tambah Lokasi" : "Ubah Lokasi");
		Common.clear(addWindow);
		addWindow.appendChild(new GMapScript());
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Lokasi"));
		row.appendChild(nama = new Textbox(lokasi.getNama() == null ? "" : lokasi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Koordinate"));
		row.appendChild(coordinate = new Label(lokasi.getCoordinate()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Latitude"));
		row.appendChild(lat = new MyDoublebox(lokasi.getLat()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Longitut"));
		row.appendChild(lng = new MyDoublebox(lokasi.getLng()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(lokasi.getKeterangan() == null ? "" : lokasi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
		row.appendChild(alamat = new Textbox(lokasi.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Toko"));
		row.appendChild(toko = new Combobox());
		Common.insertComboDanSemua(toko, "nama", Toko.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(toko, lokasi.getToko());
		toko.setWidth("90%");
		toko.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gudang"));
		row.appendChild(gudang = new Combobox());
		Common.insertComboDanSemua(gudang, "nama", Gudang.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(gudang, lokasi.getGudang());
		gudang.setWidth("90%");
		gudang.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Username pengguna"));
		row.appendChild(alamatDetail = new Textbox(lokasi.getDetailAlamat()));
		alamatDetail.setWidth("90%");
		alamatDetail.setRows(2);

		Common.initKeterangan(rows,
				"Username pengguna yang masuk pada lokasi ini, pisahkan dengan tanda koma (,) jika username lebih dari satu");

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
	 * Memvalidasi input form dan menyimpan data lokasi ke database melalui DAO.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengambil nilai dari semua komponen input form lokasi, melakukan validasi kelengkapan
	 * dan keunikan data, lalu menyimpan atau memperbarui entitas {@link Lokasi} ke database
	 * menggunakan {@link LokasiDao}. Metode ini dipanggil oleh tombol Simpan di form modal.
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memeriksa apakah field nama tidak kosong; jika kosong menampilkan pesan peringatan.
	 * (2) Memanggil {@code checkNamaLokasi()} untuk memastikan tidak ada lokasi lain dengan
	 *     nama yang sama di database; jika duplikat ditemukan menampilkan peringatan dan
	 *     mengembalikan {@code false}.
	 * (3) Mendapatkan instance {@code LokasiDao} dari {@code DaoFactory}.
	 * (4) Jika lokasi sudah ada (ID tidak null), memuat entitas dari DAO untuk memastikan
	 *     objek berada dalam state managed Hibernate.
	 * (5) Menetapkan semua nilai dari komponen form ke properti entitas, termasuk toko dan
	 *     gudang dari combobox (bisa null jika tidak dipilih).
	 * (6) Memanggil {@code lokasiDao.update()} atau {@code lokasiDao.save()} sesuai kondisi.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZKoss dari tombol Simpan yang memicu pemanggilan metode ini
	 *
	 * <b>Return:</b><br>
	 * @return {@code true} jika data berhasil disimpan; {@code false} jika validasi gagal
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan akses database melalui DAO
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Berbeda dengan controller lain, LokasiAction menggunakan DAO Pattern (bukan langsung
	 * {@code Common.refreshSaveOrUpdate}) untuk pemisahan lapisan yang lebih bersih.
	 * Saat menambah field baru, tambahkan setter yang sesuai sebelum blok save/update.
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Lokasi belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama Lokasi dengan nama yang jelas dan mudah diidentifikasi; (2) Lokasi digunakan untuk mencatat tempat penyimpanan/penempatan aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaLokasi();
		if (i) {
			MyMessageboxConfig.show("Nama Lokasi sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		LokasiDao lokasiDao = DaoFactory.getInstance().getLokasiDao();
		if (lokasi.getId() != null) {
			lokasi = lokasiDao.load(lokasi.getId());

		}
		lokasi.setGudang((Gudang) (gudang.getSelectedItem() == null ? null : gudang.getSelectedItem().getValue()));
		lokasi.setToko((Toko) (toko.getSelectedItem() == null ? null : toko.getSelectedItem().getValue()));
		lokasi.setCoordinate(coordinate.getValue());
		lokasi.setNama(nama.getValue());
		lokasi.setKeterangan(keterangan.getValue());
		lokasi.setLat(lat.getValue());
		lokasi.setLng(lng.getValue());
		lokasi.setAlamat(alamat.getValue());
		lokasi.setDetailAlamat(alamatDetail.getValue());

		if (lokasi.getId() != null) {
			lokasiDao.update(lokasi);
		} else {
			lokasiDao.save(lokasi);
		}

		return true;
	}

	/**
	 * Implementasi antarmuka {@link DataCriteria} untuk membangun objek Hibernate Criteria
	 * yang digunakan sebagai dasar query pencarian data lokasi.
	 *
	 * <b>Tujuan:</b><br>
	 * Membangun dan mengembalikan objek {@link Criteria} Hibernate yang mengkombinasikan
	 * kondisi filter aktif (aktif/nonaktif dan pencarian nama) untuk query data lokasi.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat Criteria untuk kelas {@link Lokasi}. Filter aktif menerapkan kondisi:
	 * jika checkbox searchaktif dicentang (atau null), hanya data dengan {@code aktif = true}
	 * atau {@code aktif IS NULL} yang diambil; jika tidak dicentang, semua data diambil.
	 * Filter nama menerapkan kondisi ILIKE (case-insensitive) dengan ANYWHERE matching
	 * jika pencarian tidak kosong. Parameter {@code order} mengontrol pengurutan berdasarkan
	 * nama ascending; diset {@code false} untuk query hitungan paging yang lebih efisien.
	 *
	 * <b>Parameter:</b><br>
	 * @param order {@code true} untuk menambahkan pengurutan nama ascending pada hasil query;
	 *              {@code false} untuk query tanpa pengurutan (digunakan untuk menghitung total)
	 *
	 * <b>Return:</b><br>
	 * @return objek {@link Criteria} Hibernate yang siap dieksekusi dengan semua filter diterapkan
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Tambahkan kondisi Restrictions baru di sini jika ada filter pencarian tambahan yang
	 * ditambahkan ke antarmuka pengguna (misalnya filter berdasarkan toko atau gudang).
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Lokasi.class)
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
	 * Memuat dan menampilkan data hasil pencarian lokasi ke grid utama halaman.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengeksekusi query pencarian berdasarkan kriteria filter aktif, menerapkan batasan
	 * paging, dan memperbarui model data grid sehingga pengguna melihat daftar lokasi
	 * yang sesuai filter dan halaman yang sedang aktif.
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menghitung total data sesuai filter untuk memperbarui komponen paging.
	 * (2) Mengambil data dengan batasan jumlah baris per halaman dan offset sesuai halaman aktif.
	 * (3) Membungkus hasil dalam {@code SimpleListModel} dan menetapkan renderer baru ke grid.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZKoss; dapat {@code null} jika dipanggil secara programatik
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Dipanggil setelah operasi simpan, hapus, atau saat filter pencarian berubah.
	 * Juga dipanggil saat halaman pertama kali dimuat melalui {@code doAfterCompose}.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Lokasi> lokasi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(lokasi);
		grid.setRowRenderer(new LokasiRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Memeriksa apakah nama lokasi yang diinput pada form sudah digunakan oleh lokasi lain
	 * di database untuk mencegah duplikasi data nama lokasi.
	 *
	 * <b>Tujuan:</b><br>
	 * Memastikan integritas data dengan menolak penyimpanan lokasi jika nama yang sama
	 * sudah ada di database (kecuali jika sedang mengedit lokasi yang sudah ada itu sendiri).
	 *
	 * <b>Cara kerja:</b><br>
	 * Menjalankan query Hibernate dengan proyeksi {@code rowCount()} pada tabel Lokasi
	 * dengan kondisi: nama sama persis (case-sensitive) dengan nilai dari field input,
	 * DAN ID lokasi berbeda dari ID lokasi yang sedang diedit (untuk mengizinkan penyimpanan
	 * tanpa perubahan nama pada data yang sudah ada). Jika {@code lokasi.getId()} null
	 * (mode tambah), kondisi ID diabaikan (sqlRestriction "1=1").
	 *
	 * <b>Return:</b><br>
	 * @return {@code true} jika nama sudah digunakan oleh lokasi lain (duplikat ditemukan);
	 *         {@code false} jika nama belum digunakan atau hanya digunakan oleh lokasi
	 *         yang sedang diedit
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Validasi ini bersifat case-sensitive (menggunakan {@code Restrictions.eq}). Jika
	 * diinginkan validasi case-insensitive, ganti dengan {@code Restrictions.ilike}.
	 * Pertimbangkan juga untuk menambahkan unique constraint di level database sebagai
	 * lapisan keamanan tambahan terhadap race condition.
	 */
	public Boolean checkNamaLokasi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Lokasi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.lokasi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.lokasi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
