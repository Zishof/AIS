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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
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
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.KelompokParameterTambahanPerbaikanAsset;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>KelompokParameterTambahanPerbaikanAssetAction — Kontroler Master Kelompok Parameter Tambahan Perbaikan Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZK (ZKoss 5.5) untuk halaman manajemen master
 * data Kelompok Parameter Tambahan Perbaikan Aset. Setiap kelompok merepresentasikan
 * kategori atau grup yang mengorganisasikan parameter-parameter tambahan yang
 * diperlukan dalam proses perbaikan (maintenance/overhaul) aset milik institusi.
 * Contoh kelompok parameter adalah "Kondisi Fisik", "Kerusakan Mekanis",
 * "Kerusakan Elektrikal", dan sebagainya, yang masing-masing dapat memiliki
 * parameter detail di bawahnya. Pengelompokan ini memudahkan pencatatan dan
 * pelaporan kondisi aset secara terstruktur pada saat proses perbaikan berlangsung.
 * Data kelompok ini menjadi referensi bagi modul perbaikan aset untuk menampilkan
 * formulir inspeksi yang terorganisir.
 *
 * <b>Cara kerja:</b><br>
 * Kontroler ini merupakan turunan langsung dari {@code GenericAutowireComposer}
 * tanpa mengimplementasikan antarmuka tambahan (berbeda dari kelas CRUD lain yang
 * mengimplementasikan {@code DataInitDefault}). Siklus hidup dimulai dari
 * {@code doBeforeCompose} yang memeriksa keamanan akses dan validitas sesi,
 * dilanjutkan {@code doAfterCompose} yang menginisialisasi komponen UI, memeriksa
 * sesi pengguna, mengatur hak akses tombol, dan menjalankan pencarian awal.
 * Grid utama menggunakan {@code KelompokParameterTambahanPerbaikanAssetRenderer}
 * sebagai renderer baris yang menampilkan nama (dengan tautan revisi), keterangan,
 * checkbox aktif (dengan penyimpanan langsung), Intbox nomor urut (dengan
 * penyimpanan langsung saat berubah), dan tombol ubah serta hapus yang dibangun
 * secara mandiri (bukan menggunakan {@code Common.copyEditDeleteButtons}). Validasi
 * duplikasi nama dilakukan sebelum penyimpanan melalui {@code checkNamaKelompokParameterTambahanPerbaikanAsset}.
 *
 * <b>Threading:</b><br>
 * Seluruh interaksi dengan basis data dilakukan melalui sesi Hibernate per-thread
 * yang dikelola oleh konteks ZK. Tidak ada thread latar belakang yang dibuat.
 * Event listener untuk checkbox aktif, Intbox nomor urut, tombol hapus, dan
 * tombol simpan semuanya berjalan di thread event ZK yang sama, memastikan
 * konsistensi akses sesi Hibernate. Perhatikan bahwa listener onChange pada
 * Intbox nomor urut akan langsung menyimpan ke basis data tanpa konfirmasi.
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas ini tidak mengimplementasikan {@code DataInitDefault} sehingga tombol
 * ubah dibangun secara manual dalam renderer menggunakan listener inline. Jika
 * perlu menambahkan fitur salin data, implementasikan {@code DataInitDefault}
 * dan daftarkan ke {@code Common.copyEditDeleteButtons}. Untuk menambah field
 * baru pada entitas {@code KelompokParameterTambahanPerbaikanAsset}, tambahkan
 * baris formulir di {@code init(KelompokParameterTambahanPerbaikanAsset)} dan
 * penanganan nilainya di {@code onSave}. Validasi duplikasi nama dilakukan
 * secara terpisah dari validasi field kosong untuk memberikan pesan yang spesifik.
 *
 * @author Sistem AIS
 * @version 1.0
 * @see KelompokParameterTambahanPerbaikanAsset
 */
public class KelompokParameterTambahanPerbaikanAssetAction extends GenericAutowireComposer {

	/**
	 * Nomor versi serialisasi untuk kompatibilitas serialisasi Java.
	 * Nilai ini tidak boleh diubah kecuali terjadi perubahan struktural
	 * yang disengaja pada kelas ini.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZK untuk formulir tambah atau ubah data kelompok parameter. */
	private MyWindow addWindow;

	/** Komponen paginasi ZK untuk navigasi halaman pada grid data utama. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar kelompok parameter tambahan perbaikan aset. */
	private MyGrid grid;

	/** Kotak teks untuk memasukkan kata kunci pencarian berdasarkan nama kelompok. */
	private Textbox searchnama;

	/** Kotak teks pada formulir untuk memasukkan nama kelompok parameter baru atau yang diubah. */
	private Textbox nama;

	/** Kotak teks pada formulir untuk memasukkan keterangan atau deskripsi kelompok. */
	private Textbox keterangan;

	/**
	 * Flag hak akses ubah data. Bernilai {@code true} jika pengguna saat ini
	 * memiliki hak {@code CommonPrivilages.UPDATE}.
	 */
	private boolean edit = false;

	/**
	 * Flag hak akses hapus data. Bernilai {@code true} jika pengguna saat ini
	 * memiliki hak {@code CommonPrivilages.DELETE}.
	 */
	private boolean delete = false;

	/** Entitas kelompok parameter yang sedang diedit atau dibuat baru. */
	private KelompokParameterTambahanPerbaikanAsset kelompokParameterTambahanPerbaikanAsset;

	/** Tombol toolbar untuk menambah data kelompok parameter baru. */
	private MyToolbarbuttonConfig add;

	/**
	 * <b>Tujuan:</b> Memeriksa keamanan akses sebelum komponen ZK dikompilasi dan
	 * dirakit oleh framework, memastikan hanya pengguna terautentikasi yang dapat
	 * mengakses halaman manajemen kelompok parameter tambahan perbaikan aset.
	 *
	 * <b>Cara kerja:</b> Metode ini dipanggil oleh framework ZK sebelum fase
	 * {@code doAfterCompose}. Metode memanggil {@code Common.doCheckSecurity()} untuk
	 * memverifikasi keautentikan sesi pengguna dan mengarahkan ke login jika tidak
	 * valid. Setelah berhasil, memanggil {@code super.doBeforeCompose} untuk
	 * melanjutkan proses perakitan komponen ZK secara normal.
	 *
	 * <b>Parameter:</b><br>
	 * - {@code page}: halaman ZK yang sedang dikompose<br>
	 * - {@code parent}: komponen induk dalam hierarki ZK<br>
	 * - {@code compInfo}: metadata informasi komponen dari ZUL
	 *
	 * <b>Return:</b> Objek {@code ComponentInfo} dari pemanggilan super yang
	 * diperlukan oleh framework untuk melanjutkan komposisi.
	 *
	 * <b>Penanganan error:</b> Jika pengguna tidak terautentikasi, {@code Common.doCheckSecurity()}
	 * melakukan redirect dan eksekusi tidak mencapai {@code super.doBeforeCompose}.
	 *
	 * <b>Pemeliharaan:</b> Jangan hapus pemanggilan {@code Common.doCheckSecurity()}
	 * karena akan membuka celah keamanan serius pada halaman ini.
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
	 * framework ZK selesai merakit komponen dari file ZUL, termasuk validasi sesi
	 * pengguna, pengaturan hak akses, paginasi, dan pencarian awal data.
	 *
	 * <b>Cara kerja:</b> Setelah wire otomatis selesai, metode ini melakukan
	 * serangkaian inisialisasi: (1) memanggil {@code super.doAfterCompose} dan
	 * {@code Common.initLaguage()} untuk bahasa antarmuka, (2) memeriksa apakah
	 * atribut sesi {@code usersTemp} ada dan apakah pengguna memiliki hak READ;
	 * jika tidak, sesi dibersihkan dan pengguna diarahkan ke halaman logoff,
	 * (3) menyetel visibilitas dan tooltip tombol tambah sesuai hak CREATE,
	 * (4) membaca flag edit dan delete dari hak akses, (5) menjalankan
	 * {@code onSearchDefault} untuk memuat data awal, dan (6) memasang event
	 * listener paginasi agar grid diperbarui saat halaman berubah. Perhatikan
	 * bahwa kelas ini tidak mendaftarkan tombol ekspor atau unggah data.
	 *
	 * <b>Parameter:</b> {@code comp} adalah komponen root halaman ZUL yang telah
	 * selesai dirakit.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception} umum. Jika sesi tidak
	 * valid, pengguna dialihkan ke logoff sehingga inisialisasi UI tidak selesai
	 * dijalankan.
	 *
	 * <b>Pemeliharaan:</b> Pemeriksaan {@code session.getAttribute("usersTemp")}
	 * adalah lapisan keamanan tambahan di luar {@code Common.doCheckSecurity()}.
	 * Jika diperlukan tombol ekspor, tambahkan seperti pola di kelas CRUD lain
	 * menggunakan {@code Common.cetakData} dan {@code Common.appendKeToolbar}.
	 *
	 * @param comp komponen root halaman ZUL yang telah selesai dirakit
	 * @throws Exception jika terjadi kesalahan saat inisialisasi komponen UI
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
	 * <b>Tujuan:</b> Kelas renderer baris grid yang merender satu entitas
	 * {@code KelompokParameterTambahanPerbaikanAsset} ke dalam baris komponen
	 * {@code Row} ZK pada grid utama, dengan tombol aksi yang dibangun secara mandiri.
	 *
	 * <b>Cara kerja:</b> Setiap baris menampilkan: (1) Vbox dengan tautan revisi
	 * nama melalui {@code RevisiHelper.createNewRevisi}, (2) Label keterangan,
	 * (3) Checkbox aktif dengan listener {@code onCheck} yang langsung menyimpan
	 * perubahan ke basis data, (4) {@code Intbox} untuk nomor urut dengan listener
	 * {@code onChange} yang langsung menyimpan perubahan, dan (5) Hbox berisi
	 * tombol ubah (ikon edit) dan tombol hapus (ikon trash) yang dibangun manual.
	 * Tombol hapus hanya terlihat jika {@code delete} true DAN data bukan data
	 * default ({@code getDefaultData()} false). Dialog konfirmasi ditampilkan
	 * sebelum penghapusan dilakukan, dengan penanganan error jika data memiliki
	 * relasi dengan data lain yang mencegah penghapusan.
	 *
	 * <b>Parameter:</b><br>
	 * - {@code arg0}: baris {@code Row} ZK yang akan diisi komponen<br>
	 * - {@code arg1}: objek {@code KelompokParameterTambahanPerbaikanAsset} yang akan dirender
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception}. Jika hapus gagal akibat
	 * constraint FK, pesan error dengan detail ditampilkan ke pengguna.
	 *
	 * <b>Pemeliharaan:</b> Berbeda dengan kelas CRUD lain, renderer ini membangun
	 * tombol aksi secara mandiri tanpa {@code Common.copyEditDeleteButtons}. Hal ini
	 * disebabkan oleh kebutuhan penyembunyian tombol hapus untuk data default dan
	 * karena kelas tidak mengimplementasikan {@code DataInitDefault}.
	 */
	class KelompokParameterTambahanPerbaikanAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data {@code KelompokParameterTambahanPerbaikanAsset}
		 * ke dalam komponen {@code Row} ZK dengan semua kolom, kontrol interaktif,
		 * dan tombol aksi yang diperlukan pada grid utama.
		 *
		 * <b>Cara kerja:</b> Metode ini dipanggil oleh framework ZK untuk setiap baris
		 * yang perlu dirender. Objek data di-cast ke tipe yang tepat, lalu komponen-komponen
		 * ZK dibuat secara programatik: nama dengan revisi via RevisiHelper, label keterangan,
		 * checkbox aktif yang menyimpan langsung on check, Intbox nomor urut yang menyimpan
		 * langsung on change, dan Hbox dengan dua tombol (Ubah Data dan Hapus Data).
		 * Tombol Hapus memicu dialog konfirmasi sebelum memanggil {@code Common.refreshDelete}.
		 * Jika delete gagal, pesan error informatif ditampilkan.
		 *
		 * <b>Parameter:</b><br>
		 * - {@code arg0}: baris Row ZK target<br>
		 * - {@code arg1}: objek KelompokParameterTambahanPerbaikanAsset yang akan ditampilkan
		 *
		 * <b>Return:</b> Tidak ada nilai kembalian (void).
		 *
		 * <b>Penanganan error:</b> Melempar {@code Exception}. Penghapusan yang melanggar
		 * FK akan ditangkap dan pesannya ditampilkan ke pengguna.
		 *
		 * <b>Pemeliharaan:</b> Jika perlu menambah kolom, tambahkan komponen baru
		 * sebelum baris {@code toolbar.setParent(arg0)} untuk mempertahankan urutan.
		 *
		 * @param arg0 baris Row ZK yang akan diisi komponen-komponen sel
		 * @param arg1 objek data KelompokParameterTambahanPerbaikanAsset yang akan dirender
		 * @throws Exception jika terjadi kesalahan pembuatan komponen atau akses basis data
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final KelompokParameterTambahanPerbaikanAsset kelompokParameterTambahanPerbaikanAsset = (KelompokParameterTambahanPerbaikanAsset) arg1;

			RevisiHelper
					.createNewRevisi(KelompokParameterTambahanPerbaikanAsset.class,
							kelompokParameterTambahanPerbaikanAsset, kelompokParameterTambahanPerbaikanAsset.getNama())
					.setParent(arg0);
			new Label(kelompokParameterTambahanPerbaikanAsset.getKeterangan()).setParent(arg0);


			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kelompokParameterTambahanPerbaikanAsset.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokParameterTambahanPerbaikanAsset.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kelompokParameterTambahanPerbaikanAsset);
				}
			});

			final Intbox intbox = new Intbox(kelompokParameterTambahanPerbaikanAsset.getNomorUrut());
			intbox.setWidth("90%");
			intbox.setParent(arg0);
			intbox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokParameterTambahanPerbaikanAsset.setNomorUrut(intbox.getValue());
					Common.refreshSaveOrUpdate(kelompokParameterTambahanPerbaikanAsset);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kelompokParameterTambahanPerbaikanAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && !kelompokParameterTambahanPerbaikanAsset.getDefaultData());
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(kelompokParameterTambahanPerbaikanAsset);
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
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" pada toolbar untuk membuka
	 * formulir penambahan kelompok parameter tambahan perbaikan aset baru dalam
	 * jendela modal.
	 *
	 * <b>Cara kerja:</b> Metode ini membuat instance baru {@code KelompokParameterTambahanPerbaikanAsset}
	 * yang kosong (tanpa ID, menandakan data baru) dan meneruskannya ke metode
	 * {@code init} private untuk membangun formulir. Setelah formulir dibangun,
	 * jendela modal ditampilkan dan diaktifkan. Jendela yang sama digunakan untuk
	 * operasi tambah dan ubah, dengan judul yang berbeda berdasarkan keberadaan ID.
	 *
	 * <b>Parameter:</b> {@code event} adalah event ZK dari klik tombol tambah,
	 * tidak digunakan langsung dalam metode ini.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Tidak melempar exception secara eksplisit. Kesalahan
	 * pada {@code init} akan ditangkap oleh framework ZK.
	 *
	 * <b>Pemeliharaan:</b> Nama metode {@code onAdd} harus sesuai dengan atribut
	 * event pada tombol tambah di file ZUL atau dikonfigurasi sebagai listener
	 * programatik.
	 *
	 * @param event event ZK yang dipicu saat tombol tambah diklik
	 * @throws Exception jika terjadi kesalahan saat inisialisasi formulir
	 */
	public void onAdd(Event event) throws Exception {
		init(new KelompokParameterTambahanPerbaikanAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan menginisialisasi seluruh komponen formulir modal
	 * untuk menambah atau mengubah data kelompok parameter tambahan perbaikan aset,
	 * mengisi field formulir sesuai data entitas yang diberikan.
	 *
	 * <b>Cara kerja:</b> Metode ini menyimpan referensi entitas, menyetel judul
	 * jendela ("Tambah Kelompok" atau "Ubah Kelompok" berdasarkan ID), membersihkan
	 * isi jendela dengan {@code Common.clear}, lalu membangun tata letak menggunakan
	 * {@code MyBorderlayout} dengan area tengah berisi grid formulir dua kolom
	 * (35%-sisanya) dan area selatan berisi toolbar. Formulir memiliki dua baris:
	 * (1) Nama Kelompok (wajib diisi saat simpan) dan (2) Keterangan (area teks
	 * 3 baris). Nilai awal diambil dari properti entitas dengan null-safe check
	 * (menggunakan string kosong sebagai fallback). Toolbar berisi tombol Batal
	 * (menutup jendela) dan Simpan (memanggil {@code onSave}).
	 *
	 * <b>Parameter:</b> {@code kelompokParameterTambahanPerbaikanAsset} adalah
	 * entitas yang datanya akan ditampilkan. Jika ID null, berarti data baru.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Tidak melempar exception secara eksplisit. Kesalahan
	 * pembuatan komponen ZK ditangkap oleh framework.
	 *
	 * <b>Pemeliharaan:</b> Untuk menambah field baru, buat {@code MyFormRow} baru
	 * sebelum blok South/toolbar. Pastikan field baru juga ditangani di {@code onSave}.
	 * Variabel lokal {@code grid} di sini berbeda dengan field instance {@code grid}
	 * (grid utama halaman).
	 *
	 * @param kelompokParameterTambahanPerbaikanAsset entitas yang akan ditampilkan di formulir
	 */
	private void init(KelompokParameterTambahanPerbaikanAsset kelompokParameterTambahanPerbaikanAsset) {
		this.kelompokParameterTambahanPerbaikanAsset = kelompokParameterTambahanPerbaikanAsset;
		addWindow.setTitle(kelompokParameterTambahanPerbaikanAsset.getId() == null ? "Tambah Kelompok" : "Ubah Kelompok");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelompok"));
		row.appendChild(nama = new Textbox(kelompokParameterTambahanPerbaikanAsset.getNama() == null ? ""
				: kelompokParameterTambahanPerbaikanAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kelompokParameterTambahanPerbaikanAsset.getKeterangan() == null ? ""
				: kelompokParameterTambahanPerbaikanAsset.getKeterangan()));
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
	 * <b>Tujuan:</b> Memvalidasi input formulir dan menyimpan data kelompok parameter
	 * tambahan perbaikan aset ke basis data, dengan pemeriksaan duplikasi nama
	 * sebelum penyimpanan.
	 *
	 * <b>Cara kerja:</b> Metode ini melakukan dua tahap validasi: (1) memeriksa
	 * apakah nama kelompok tidak kosong; jika kosong, menampilkan dialog peringatan
	 * dan mengembalikan {@code false}, (2) memanggil
	 * {@code checkNamaKelompokParameterTambahanPerbaikanAsset()} untuk memeriksa
	 * apakah nama tersebut sudah ada di basis data (dengan pengecualian untuk entitas
	 * yang sedang diedit berdasarkan ID); jika duplikat, menampilkan peringatan dan
	 * mengembalikan {@code false}. Jika semua validasi lolos, sesi Hibernate diambil;
	 * untuk data yang ada (ID tidak null), entitas dimuat ulang dari basis data untuk
	 * menghindari masalah detached entity. Kemudian nama dan keterangan diatur pada
	 * entitas sebelum dipersistensikan via {@code Common.refreshSaveOrUpdate}.
	 *
	 * <b>Parameter:</b> {@code event} adalah event ZK dari klik tombol simpan.
	 *
	 * <b>Return:</b> {@code true} jika data berhasil disimpan; {@code false} jika
	 * validasi gagal.
	 *
	 * <b>Penanganan error:</b> Melempar {@code Exception} umum. Pelanggaran constraint
	 * basis data ditangkap oleh framework ZK.
	 *
	 * <b>Pemeliharaan:</b> Perhatikan bahwa nomor urut tidak diatur di sini karena
	 * diatur langsung melalui Intbox di renderer. Jika ada field baru yang perlu
	 * divalidasi, tambahkan blok validasi sebelum logika penyimpanan.
	 *
	 * @param event event ZK dari klik tombol simpan
	 * @return true jika penyimpanan berhasil, false jika validasi gagal
	 * @throws Exception jika terjadi kesalahan Hibernate atau ZK
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Kelompok Parameter belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama kelompok parameter perbaikan; (2) Kelompok digunakan untuk mengelompokkan parameter tambahan perbaikan aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaKelompokParameterTambahanPerbaikanAsset();
		if (i) {
			MyMessageboxConfig.show("Nama Kelompok sudah ada di database", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelompokParameterTambahanPerbaikanAsset.getId() != null) {
			kelompokParameterTambahanPerbaikanAsset = (KelompokParameterTambahanPerbaikanAsset) session.load(
					KelompokParameterTambahanPerbaikanAsset.class, kelompokParameterTambahanPerbaikanAsset.getId());

		}

		kelompokParameterTambahanPerbaikanAsset.setNama(nama.getValue());
		kelompokParameterTambahanPerbaikanAsset.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, kelompokParameterTambahanPerbaikanAsset);

		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun objek {@code Criteria} Hibernate untuk digunakan
	 * dalam kueri pencarian data kelompok parameter tambahan perbaikan aset
	 * berdasarkan filter nama yang aktif pada antarmuka pencarian.
	 *
	 * <b>Cara kerja:</b> Metode mengambil sesi Hibernate saat ini dan membuat
	 * {@code Criteria} untuk kelas {@code KelompokParameterTambahanPerbaikanAsset}.
	 * Filter yang diterapkan adalah pencarian nama case-insensitive menggunakan
	 * {@code MatchMode.ANYWHERE}; jika kotak teks pencarian kosong, semua data
	 * ditampilkan menggunakan {@code sqlRestriction("true")}. Berbeda dengan
	 * kelas CRUD lain, tidak ada filter status aktif di sini karena semua kelompok
	 * (aktif maupun tidak) ditampilkan tanpa filter. Jika parameter {@code order}
	 * bernilai true, hasil diurutkan berdasarkan nama secara ascending.
	 *
	 * <b>Parameter:</b> {@code order} menentukan apakah kriteria harus menyertakan
	 * klausa ORDER BY nama ascending.
	 *
	 * <b>Return:</b> Objek {@code Criteria} Hibernate yang siap dieksekusi.
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error khusus.
	 *
	 * <b>Pemeliharaan:</b> Jika perlu menambah filter status aktif, tambahkan
	 * Restrictions sesuai pola di kelas CRUD lain dalam paket yang sama.
	 *
	 * @param order jika true, hasil diurutkan berdasarkan nama secara ascending
	 * @return objek Criteria Hibernate yang sudah dikonfigurasi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelompokParameterTambahanPerbaikanAsset.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Melakukan pencarian data kelompok parameter tambahan perbaikan
	 * aset dan memperbarui tampilan grid utama beserta paginasi sesuai halaman aktif.
	 *
	 * <b>Cara kerja:</b> Metode pertama memanggil {@code Common.initPaging} dengan
	 * kriteria tanpa urutan untuk menghitung total halaman. Kemudian mengambil data
	 * halaman saat ini dengan batasan {@code ROWS_COUNT_ON_PAGE} baris dan offset
	 * berdasarkan halaman aktif paginasi. Hasil dibungkus dalam {@code SimpleListModel}
	 * dan ditetapkan ke grid bersama renderer baru. Metode ini dipanggil saat
	 * inisialisasi, saat paginasi berubah, dan setelah operasi simpan atau hapus.
	 *
	 * <b>Parameter:</b> {@code event} adalah event ZK yang memicu pencarian, dapat null.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit. Anotasi
	 * {@code @SuppressWarnings("unchecked")} menekan peringatan type-safety.
	 *
	 * <b>Pemeliharaan:</b> Pemanggilan {@code initCriteria(false)} dan
	 * {@code initCriteria(true)} harus konsisten untuk memastikan jumlah halaman
	 * sesuai dengan data yang ditampilkan.
	 *
	 * @param event event ZK pemicu pencarian, dapat null jika dipanggil programatik
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelompokParameterTambahanPerbaikanAsset> kelompokParameterTambahanPerbaikanAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelompokParameterTambahanPerbaikanAsset);
		grid.setRowRenderer(new KelompokParameterTambahanPerbaikanAssetRenderer());
		grid.setModelCheckMobile(strset);



	}

	/**
	 * <b>Tujuan:</b> Memeriksa apakah nama kelompok yang dimasukkan pada formulir
	 * sudah ada di basis data, untuk mencegah duplikasi nama kelompok parameter
	 * tambahan perbaikan aset.
	 *
	 * <b>Cara kerja:</b> Metode membangun kueri Hibernate dengan proyeksi
	 * {@code rowCount} untuk menghitung jumlah entitas {@code KelompokParameterTambahanPerbaikanAsset}
	 * yang memiliki nama yang sama persis (case-sensitive, menggunakan
	 * {@code Restrictions.eq}) dengan nilai field {@code nama} saat ini. Untuk
	 * operasi ubah data (entitas sudah memiliki ID), entitas dengan ID yang sama
	 * dikecualikan dari pencarian menggunakan {@code Restrictions.ne("id", id)},
	 * sehingga nama yang tidak berubah tetap dianggap valid. Untuk data baru
	 * (ID null), semua entitas dengan nama tersebut dianggap duplikat.
	 *
	 * <b>Parameter:</b> Tidak ada parameter langsung; metode membaca nilai dari
	 * field instance {@code nama} (Textbox) dan
	 * {@code this.kelompokParameterTambahanPerbaikanAsset}.
	 *
	 * <b>Return:</b> {@code true} jika nama sudah ada di basis data (duplikat);
	 * {@code false} jika nama belum ada dan aman untuk disimpan.
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit. Jika
	 * {@code uniqueResult()} mengembalikan null (tidak mungkin untuk rowCount),
	 * akan terjadi NullPointerException. Pastikan koneksi basis data aktif sebelum
	 * memanggil metode ini.
	 *
	 * <b>Pemeliharaan:</b> Metode ini menggunakan perbandingan nama yang case-sensitive
	 * (bukan ilike). Jika perlu case-insensitive, ganti {@code Restrictions.eq}
	 * dengan {@code Restrictions.ilike} dengan {@code MatchMode.EXACT}.
	 *
	 * @return Boolean true jika nama sudah ada (duplikat), false jika belum ada
	 */
	public Boolean checkNamaKelompokParameterTambahanPerbaikanAsset() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KelompokParameterTambahanPerbaikanAsset.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.kelompokParameterTambahanPerbaikanAsset.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kelompokParameterTambahanPerbaikanAsset.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
