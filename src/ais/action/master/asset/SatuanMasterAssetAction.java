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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
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
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.SatuanMasterAsset;

/**
 * <h3>SatuanMasterAssetAction &mdash; Controller CRUD Satuan Ukuran Aset</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini merupakan controller ZK (composer) untuk halaman manajemen data master
 * <em>Satuan Ukuran Aset</em>, yaitu daftar satuan pengukuran yang digunakan dalam
 * pencatatan dan pengadaan aset institusi (contoh: Buah, Rim, Pak, Dus, Gulung, Pcs, Lusin).
 * Satuan ini menjadi referensi wajib pada setiap item pengadaan dan inventaris aset agar
 * konsistensi pencatatan terjaga lintas modul (pengadaan, penerimaan, gudang, dan laporan).
 * Tanpa satuan yang terstandarisasi, rekap kuantitas aset akan sulit dibandingkan dan
 * diaudit. Halaman ini menyediakan operasi tambah, ubah, dan hapus data satuan secara
 * terpandu dengan validasi duplikasi nama.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Controller ini mewarisi {@code GenericAutowireComposer} dari ZK Framework sehingga
 * seluruh komponen ZUL yang memiliki ID yang sama dengan nama field di kelas ini
 * secara otomatis diwire (disuntikkan) oleh ZK pada saat halaman dimuat. Alur kerja
 * utama adalah sebagai berikut:
 * <ol>
 *   <li>{@code doBeforeCompose} dipanggil pertama kali untuk verifikasi keamanan sesi
 *       sebelum halaman dirender.</li>
 *   <li>{@code doAfterCompose} dipanggil setelah seluruh komponen ZUL selesai dirender;
 *       di sini dilakukan inisialisasi awal: pengaturan hak akses tombol Tambah/Ubah/Hapus,
 *       pengisian data bawaan (seeding) jika tabel masih kosong, serta pemanggilan
 *       {@code onSearchDefault} untuk menampilkan data pertama kali.</li>
 *   <li>Grid data diisi melalui {@code onSearchDefault} yang memanggil {@code initCriteria}
 *       untuk membangun query Hibernate Criteria dengan filter nama dan pengurutan.</li>
 *   <li>Tiap baris grid dirender oleh inner class {@code SatuanMasterAssetRenderer}
 *       yang membuat tombol Ubah dan Hapus inline per baris.</li>
 *   <li>Popup tambah/ubah dikelola oleh {@code init(SatuanMasterAsset)} yang membangun
 *       ulang konten {@code addWindow} secara programatik setiap kali dipanggil.</li>
 *   <li>Penyimpanan data dilakukan oleh {@code onSave} dengan validasi nama tidak boleh
 *       kosong dan tidak boleh duplikat.</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh metode di kelas ini dijalankan pada thread ZK event (UI thread) yang dikelola
 * oleh ZK Server Push. Tidak ada penggunaan thread latar belakang secara eksplisit.
 * Sesi Hibernate yang digunakan adalah sesi yang dikelola oleh
 * {@code HibernateUtil.currentSession()}, yang terikat pada thread request HTTP saat itu.
 * Karena itu, tidak diperlukan sinkronisasi tambahan selama operasi database berlangsung
 * di dalam satu siklus request-response ZK.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Jika perlu menambah kolom baru pada entitas {@code SatuanMasterAsset}, tambahkan field
 * {@code Textbox} baru di kelas ini, tambahkan baris form di metode {@code init}, dan
 * pastikan {@code onSave} menetapkan nilai field tersebut ke entitas sebelum memanggil
 * {@code Common.refreshSaveOrUpdate}. Data seeding awal (7 satuan default) hanya dijalankan
 * sekali saat tabel kosong; untuk menambah satuan default, cukup tambahkan blok
 * {@code satuanMasterAsset = new SatuanMasterAsset(); ...setNama(...); Common.refreshSaveOrUpdate(...)}
 * di dalam blok {@code if (count == 0)} pada {@code doAfterCompose}.
 * Pastikan pengujian dilakukan dengan hak akses berbeda (admin, operator, viewer) untuk
 * memverifikasi visibilitas tombol yang dikontrol oleh {@code CommonPrivilages}.</p>
 *
 * @author Tim Pengembang AIS
 * @version 1.0
 * @see SatuanMasterAsset
 * @see Common
 * @see CommonPrivilages
 * @see HibernateUtil
 */
public class SatuanMasterAssetAction extends GenericAutowireComposer {

	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal popup untuk form tambah/ubah data satuan. */
	private MyWindow addWindow;

	/** Komponen paging ZK untuk navigasi halaman pada grid data. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar satuan aset. */
	private MyGrid grid;

	/** Kotak teks filter pencarian berdasarkan nama satuan. */
	private Textbox searchnama;

	/** Kotak teks input nama satuan pada form tambah/ubah. */
	private Textbox nama;

	/** Kotak teks input keterangan satuan pada form tambah/ubah. */
	private Textbox keterangan;

	/** Flag yang menandakan apakah pengguna saat ini memiliki hak ubah data. */
	private boolean edit = false;

	/** Flag yang menandakan apakah pengguna saat ini memiliki hak hapus data. */
	private boolean delete = false;

	/** Entitas satuan aset yang sedang aktif diedit atau ditambahkan. */
	private SatuanMasterAsset satuanMasterAsset;

	/** Tombol toolbar untuk menambah data satuan baru. */
	private MyToolbarbuttonConfig add;

	/**
	 * <h3>doBeforeCompose &mdash; Verifikasi Keamanan Sebelum Render Halaman</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode ini dipanggil oleh ZK Framework tepat sebelum proses pembuatan komponen
	 * halaman dimulai. Fungsi utamanya adalah memastikan bahwa pengguna yang mengakses
	 * halaman telah terautentikasi dan berwenang, sehingga konten halaman tidak pernah
	 * dirender untuk pengguna yang tidak sah.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} yang memeriksa sesi pengguna aktif.
	 * Jika sesi tidak valid atau pengguna belum login, {@code doCheckSecurity} akan
	 * mengarahkan (redirect) ke halaman login secara otomatis. Setelah pemeriksaan
	 * keamanan, metode memanggil implementasi induk
	 * {@code super.doBeforeCompose(page, parent, compInfo)} untuk melanjutkan proses
	 * normal ZK dan mengembalikan {@code ComponentInfo} yang diperlukan oleh framework.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika sesi tidak valid, {@code Common.doCheckSecurity()} mengalihkan ke halaman
	 * login dan proses berhenti di sana. Tidak ada pengecualian yang dilempar secara
	 * eksplisit dari metode ini selain yang mungkin dilempar oleh kelas induk.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan menghapus pemanggilan {@code Common.doCheckSecurity()} karena hal ini
	 * merupakan lapisan keamanan pertama sebelum halaman dirender. Selalu pastikan
	 * {@code super.doBeforeCompose} dipanggil agar lifecycle ZK berjalan dengan benar.</p>
	 *
	 * @param page     halaman ZK yang sedang diproses
	 * @param parent   komponen induk dalam hierarki komponen ZK
	 * @param compInfo informasi metadata komponen yang sedang dikomposisi
	 * @return {@code ComponentInfo} dari implementasi kelas induk, digunakan ZK untuk
	 *         melanjutkan proses pembuatan komponen
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <h3>doAfterCompose &mdash; Inisialisasi Halaman Setelah Komponen ZK Selesai Dibuat</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode lifecycle ZK yang dipanggil setelah seluruh komponen halaman berhasil
	 * dibuat dan di-wire oleh ZK. Di sinilah seluruh logika inisialisasi awal halaman
	 * dijalankan: pemeriksaan hak akses pengguna, seeding data default, pengaturan
	 * visibilitas tombol, serta pemuatan data pertama kali ke grid.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah eksekusi:
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} agar ZK menyelesaikan proses
	 *       wire komponen secara otomatis.</li>
	 *   <li>Menginisialisasi bahasa antarmuka dengan {@code Common.initLaguage()}.</li>
	 *   <li>Memeriksa apakah sesi pengguna {@code usersTemp} masih valid dan pengguna
	 *       memiliki hak READ; jika tidak, sesi dihapus dan pengguna dialihkan ke halaman
	 *       logout.</li>
	 *   <li>Jika tabel {@code SatuanMasterAsset} masih kosong (count == 0), memasukkan
	 *       7 satuan default (Buah, Rim, Pak, Dus, Gulung, Pcs, Lusin) sebagai data awal.</li>
	 *   <li>Mengatur visibilitas tombol "Tambah" berdasarkan hak CREATE pengguna.</li>
	 *   <li>Menetapkan flag {@code edit} dan {@code delete} sesuai hak UPDATE dan DELETE.</li>
	 *   <li>Memanggil {@code onSearchDefault(null)} untuk menampilkan data awal di grid.</li>
	 *   <li>Menginisialisasi komponen paging dengan listener yang memanggil ulang
	 *       {@code onSearchDefault} setiap kali halaman ganti.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika sesi tidak valid (usersTemp null atau tidak punya hak READ), pengguna langsung
	 * dialihkan ke halaman logoff dan metode mengembalikan kontrol tanpa melanjutkan
	 * inisialisasi. Pengecualian dari {@code super.doAfterCompose} dibiarkan menyebar
	 * ke atas sesuai kontrak {@code throws Exception}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Data seeding (7 satuan default) hanya dijalankan saat tabel benar-benar kosong.
	 * Jika perlu menambah atau mengubah data default, modifikasi blok {@code if (count == 0)}.
	 * Pastikan tidak menghapus pemeriksaan {@code usersTemp} karena ini merupakan lapisan
	 * keamanan sesi.</p>
	 *
	 * @param comp komponen root ZUL yang telah selesai dirender oleh ZK
	 * @throws Exception jika terjadi kesalahan saat inisialisasi komponen induk atau
	 *                   saat operasi database pada proses seeding data default
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(SatuanMasterAsset.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			SatuanMasterAsset satuanMasterAsset = new SatuanMasterAsset();
			satuanMasterAsset.setNama("Buah");
			Common.refreshSaveOrUpdate(session, satuanMasterAsset);
			satuanMasterAsset = new SatuanMasterAsset();
			satuanMasterAsset.setNama("Rim");
			Common.refreshSaveOrUpdate(session, satuanMasterAsset);
			satuanMasterAsset = new SatuanMasterAsset();
			satuanMasterAsset.setNama("Pak");
			Common.refreshSaveOrUpdate(session, satuanMasterAsset);
			satuanMasterAsset = new SatuanMasterAsset();
			satuanMasterAsset.setNama("Dus");
			Common.refreshSaveOrUpdate(session, satuanMasterAsset);
			satuanMasterAsset = new SatuanMasterAsset();
			satuanMasterAsset.setNama("Gulung");
			Common.refreshSaveOrUpdate(session, satuanMasterAsset);
			satuanMasterAsset = new SatuanMasterAsset();
			satuanMasterAsset.setNama("Pcs");
			Common.refreshSaveOrUpdate(session, satuanMasterAsset);
			satuanMasterAsset = new SatuanMasterAsset();
			satuanMasterAsset.setNama("Lusin");
			Common.refreshSaveOrUpdate(session, satuanMasterAsset);
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
	 * <h3>SatuanMasterAssetRenderer &mdash; Renderer Baris Grid Satuan Aset</h3>
	 *
	 * <p><b>Untuk apa:</b><br>
	 * Inner class yang bertanggung jawab merender setiap baris pada grid utama halaman
	 * satuan aset. Kelas ini mengimplementasikan antarmuka renderer ZK dengan mengisi
	 * setiap {@code Row} dengan konten data entitas {@code SatuanMasterAsset} dan
	 * tombol-tombol aksi (Ubah dan Hapus) yang disesuaikan dengan hak akses pengguna.
	 * Dengan pola inner class ini, renderer memiliki akses penuh ke state controller
	 * induk (flag {@code edit}, {@code delete}, dan referensi {@code addWindow}).</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Metode {@code render(Row, Object)} dipanggil oleh ZK untuk setiap elemen model
	 * yang ditampilkan dalam grid. Tiap baris menampilkan:
	 * <ul>
	 *   <li>Kolom Nama: dirender menggunakan {@code RevisiHelper.createNewRevisi} yang
	 *       membuat label nama satuan dengan dukungan pelacakan revisi/audit.</li>
	 *   <li>Kolom Keterangan: {@code Label} biasa dengan teks keterangan satuan.</li>
	 *   <li>Kolom Aksi: {@code Hbox} berisi tombol Ubah (ikon edit) dan tombol Hapus
	 *       (ikon tempat sampah), masing-masing dengan event listener onClick.</li>
	 * </ul>
	 * Tombol Ubah membuka popup {@code addWindow} dengan data satuan yang dipilih
	 * melalui {@code init(satuanMasterAsset)}. Tombol Hapus menampilkan dialog konfirmasi
	 * sebelum menghapus data; jika terjadi error FK, pesan error ditampilkan kepada admin.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika perlu menambah kolom baru, tambahkan komponen baru sebelum {@code toolbar.setParent(arg0)}.
	 * Pastikan urutan kolom di sini sesuai dengan definisi {@code <column>} di file ZUL.
	 * Visibilitas tombol Ubah dan Hapus dikendalikan oleh flag {@code edit} dan {@code delete}
	 * dari kelas induk.</p>
	 */
	class SatuanMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render &mdash; Mengisi Konten Satu Baris Grid dengan Data Satuan Aset</h3>
		 *
		 * <p><b>Tujuan:</b><br>
		 * Metode inti renderer yang dipanggil ZK untuk setiap entitas {@code SatuanMasterAsset}
		 * dalam model grid. Metode ini mengisi baris dengan label data dan tombol aksi
		 * yang sesuai dengan hak akses pengguna yang sedang login.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Cast parameter {@code arg1} ke {@code SatuanMasterAsset}, lalu secara berurutan
		 * menambahkan komponen ke dalam {@code Row arg0}: label nama dengan dukungan revisi,
		 * label keterangan, lalu {@code Hbox} berisi tombol Ubah dan Hapus. Tombol Ubah
		 * memanggil {@code init(satuanMasterAsset)} dan membuka popup modal. Tombol Hapus
		 * menampilkan dialog konfirmasi; jika dikonfirmasi, memanggil {@code Common.refreshDelete}
		 * dan memperbarui grid. Jika penghapusan gagal karena relasi FK, exception ditangkap
		 * dan pesan error ditampilkan.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Kegagalan hapus akibat constraint foreign key ditangkap dan ditampilkan
		 * sebagai pesan dialog kepada pengguna. Admin mendapatkan detail pesan exception.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Jangan mengubah urutan penambahan komponen ke {@code arg0} tanpa menyesuaikan
		 * jumlah dan urutan kolom pada file ZUL yang bersangkutan.</p>
		 *
		 * @param arg0 baris ZK {@code Row} yang akan diisi konten
		 * @param arg1 objek data {@code SatuanMasterAsset} yang akan ditampilkan pada baris ini
		 * @throws Exception jika terjadi error saat pembuatan atau penambahan komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final SatuanMasterAsset satuanMasterAsset = (SatuanMasterAsset) arg1;

			RevisiHelper.createNewRevisi(SatuanMasterAsset.class, satuanMasterAsset, satuanMasterAsset.getNama())
					.setParent(arg0);
			new Label(satuanMasterAsset.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(satuanMasterAsset);
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
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(satuanMasterAsset);

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
	 * <h3>onAdd &mdash; Membuka Form Tambah Data Satuan Aset Baru</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Event handler yang dipicu ketika pengguna mengklik tombol "Tambah" pada toolbar
	 * halaman. Metode ini menyiapkan form kosong untuk penginputan data satuan aset baru
	 * yang belum pernah disimpan ke database.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat instance {@code SatuanMasterAsset} baru yang masih kosong (ID null menandakan
	 * data belum ada di database), kemudian memanggil {@code init(satuanMasterAsset)} untuk
	 * membangun tampilan form di dalam popup {@code addWindow}. Setelah form siap,
	 * {@code addWindow.setVisible(true)} menampilkan popup dan {@code addWindow.onModal()}
	 * mengunci interaksi ke luar popup (modal).</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Pengecualian diteruskan ke atas (throws Exception) dan ditangani oleh framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini hanya perlu dimodifikasi jika perilaku pembukaan popup berubah secara
	 * mendasar. Untuk mengubah konten form, modifikasi metode {@code init}.</p>
	 *
	 * @param event event ZK yang diterima dari klik tombol Tambah di antarmuka pengguna
	 * @throws Exception jika terjadi kesalahan saat membangun komponen form atau
	 *                   menampilkan popup modal
	 */
	public void onAdd(Event event) throws Exception {
		init(new SatuanMasterAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <h3>init &mdash; Membangun Ulang Konten Form Popup Tambah/Ubah Satuan Aset</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode internal yang membangun ulang seluruh konten popup {@code addWindow} secara
	 * programatik setiap kali form dibuka, baik untuk menambah data baru maupun mengubah
	 * data yang sudah ada. Pendekatan membangun ulang (rebuild) ini memastikan bahwa
	 * nilai lama dari sesi sebelumnya tidak tersisa di form.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah:
	 * <ol>
	 *   <li>Menyimpan referensi entitas {@code satuanMasterAsset} yang diterima ke field
	 *       instance untuk digunakan oleh {@code onSave}.</li>
	 *   <li>Mengatur judul popup: "Tambah SatuanMasterAsset" jika ID null, atau
	 *       "Ubah SatuanMasterAsset" jika ID tidak null.</li>
	 *   <li>Membersihkan konten popup lama dengan {@code Common.clear(addWindow)}.</li>
	 *   <li>Membangun layout popup: {@code MyBorderlayout} dengan Center berisi grid form
	 *       dua kolom (label 35% dan input), dan South berisi toolbar tombol Batal dan Simpan.</li>
	 *   <li>Mengisi komponen input ({@code nama} dan {@code keterangan}) dengan nilai dari
	 *       entitas yang diterima; jika null, diganti string kosong.</li>
	 *   <li>Tombol Batal menyembunyikan popup tanpa menyimpan. Tombol Simpan memanggil
	 *       {@code onSave} dan menyembunyikan popup jika berhasil.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Metode ini tidak melempar exception secara eksplisit. Error dari komponen ZK
	 * dibiarkan menyebar ke caller.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika menambah field baru ke entitas {@code SatuanMasterAsset}, tambahkan field
	 * {@code Textbox} baru di kelas ini dan tambahkan baris form yang sesuai di dalam
	 * blok {@code Rows} sebelum blok {@code South}. Perbarui juga {@code onSave} untuk
	 * menetapkan nilai field baru tersebut.</p>
	 *
	 * @param satuanMasterAsset entitas satuan aset yang akan ditampilkan di form;
	 *                          jika ID-nya {@code null}, berarti form untuk tambah data baru;
	 *                          jika ID tidak {@code null}, berarti form untuk ubah data yang sudah ada
	 */
	private void init(SatuanMasterAsset satuanMasterAsset) {
		this.satuanMasterAsset = satuanMasterAsset;
		addWindow.setTitle(satuanMasterAsset.getId() == null ? "Tambah SatuanMasterAsset" : "Ubah SatuanMasterAsset");
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

		MyFormRow row = new MyFormRow(); row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Satuan"));
		row.appendChild(nama = new Textbox(satuanMasterAsset.getNama() == null ? "" : satuanMasterAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				satuanMasterAsset.getKeterangan() == null ? "" : satuanMasterAsset.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
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
	 * <h3>onSave &mdash; Memvalidasi dan Menyimpan Data Satuan Aset ke Database</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Metode yang dipanggil saat pengguna mengklik tombol "Simpan" pada form popup.
	 * Bertanggung jawab untuk memvalidasi input pengguna, memeriksa duplikasi nama,
	 * dan menyimpan perubahan ke database baik untuk data baru (insert) maupun data
	 * yang sudah ada (update).</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memvalidasi bahwa field {@code nama} tidak kosong; jika kosong, menampilkan
	 *       dialog peringatan dan mengembalikan {@code false}.</li>
	 *   <li>Memanggil {@code checkNamaSatuanMasterAsset()} untuk memeriksa apakah nama
	 *       yang diinput sudah ada di database (dengan pengecualian untuk data yang
	 *       sedang diedit itu sendiri); jika duplikat, menampilkan peringatan dan
	 *       mengembalikan {@code false}.</li>
	 *   <li>Jika entitas sudah ada di database (ID tidak null), me-reload entitas dari
	 *       sesi Hibernate saat ini menggunakan {@code session.load()} untuk memastikan
	 *       entity dalam managed state sebelum diubah.</li>
	 *   <li>Menetapkan nilai nama dan keterangan dari input form ke entitas.</li>
	 *   <li>Memanggil {@code Common.refreshSaveOrUpdate} untuk melakukan persist ke
	 *       database dan flush sesi.</li>
	 *   <li>Mengembalikan {@code true} jika semua langkah berhasil.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Validasi kosong dan duplikasi ditangani dengan dialog pesan kepada pengguna.
	 * Error database yang tidak terduga dibiarkan menyebar ke atas melalui
	 * {@code throws Exception}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika menambah field wajib baru, tambahkan validasi sebelum blok
	 * {@code checkNamaSatuanMasterAsset()}. Jika menambah field opsional, cukup
	 * tambahkan setter setelah blok reload entitas.</p>
	 *
	 * @param event event ZK yang diterima dari klik tombol Simpan pada form popup
	 * @return {@code true} jika data berhasil disimpan ke database;
	 *         {@code false} jika validasi gagal (nama kosong atau nama sudah ada)
	 * @throws Exception jika terjadi kesalahan pada operasi database saat menyimpan
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Satuan Aset belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama satuan yang sesuai (contoh: Unit, Buah, Set, Paket); (2) Satuan digunakan untuk pencatatan jumlah aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaSatuanMasterAsset();
		if (i) {
			MyMessageboxConfig.show("Nama Status Asset sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (satuanMasterAsset.getId() != null) {
			satuanMasterAsset = (SatuanMasterAsset) session.load(SatuanMasterAsset.class, satuanMasterAsset.getId());

		}

		satuanMasterAsset.setNama(nama.getValue());
		satuanMasterAsset.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, satuanMasterAsset);

		return true;
	}

	/**
	 * <h3>initCriteria &mdash; Membangun Kriteria Query Hibernate untuk Pencarian Satuan Aset</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Membangun objek {@code Criteria} Hibernate yang berisi seluruh kondisi filter
	 * pencarian satuan aset berdasarkan input pengguna pada form pencarian. Metode ini
	 * digunakan oleh {@code onSearchDefault} untuk mengambil data (dengan paging) dan
	 * untuk menghitung total baris (untuk paging).</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mengambil sesi Hibernate saat ini, membuat kriteria untuk entitas
	 * {@code SatuanMasterAsset}, lalu menambahkan kondisi:
	 * <ul>
	 *   <li>Jika {@code searchnama} tidak kosong, menambahkan filter
	 *       {@code ilike("nama", ..., ANYWHERE)} untuk pencarian nama case-insensitive
	 *       dengan mode "mengandung teks" (LIKE %keyword%).</li>
	 *   <li>Jika {@code searchnama} kosong, menambahkan {@code sqlRestriction("true")}
	 *       yang berarti tidak ada filter (ambil semua data).</li>
	 *   <li>Jika parameter {@code order} bernilai {@code true}, menambahkan
	 *       {@code Order.asc("nama")} sehingga hasil diurutkan abjad A-Z berdasarkan nama.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Tidak ada penanganan error eksplisit; exception dari Hibernate dibiarkan
	 * menyebar ke caller.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika menambah filter baru (misalnya filter berdasarkan tanggal dibuat), tambahkan
	 * {@code criteria.add(...)} baru di dalam metode ini. Pastikan filter baru juga
	 * tercermin di komponen ZUL yang sesuai.</p>
	 *
	 * @param order jika {@code true}, hasil query akan diurutkan berdasarkan nama secara
	 *              ascending; jika {@code false}, tidak ada pengurutan (digunakan untuk
	 *              query count paging)
	 * @return objek {@code Criteria} Hibernate yang siap dieksekusi untuk mengambil
	 *         atau menghitung data satuan aset sesuai filter aktif
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(SatuanMasterAsset.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * <h3>onSearchDefault &mdash; Memuat Ulang Data Grid Satuan Aset dengan Filter Aktif</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Event handler utama untuk pencarian dan pembaruan tampilan grid. Dipanggil pada
	 * saat halaman pertama kali dimuat, saat pengguna mengklik tombol Cari, saat
	 * paging berubah, serta setelah operasi simpan atau hapus data berhasil dilakukan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code Common.initPaging(initCriteria(false), paging)} untuk
	 *       memperbarui total baris pada komponen paging tanpa pengurutan (lebih efisien
	 *       karena query COUNT tidak memerlukan ORDER BY).</li>
	 *   <li>Mengambil daftar {@code SatuanMasterAsset} menggunakan {@code initCriteria(true)}
	 *       dengan pengurutan nama, dibatasi oleh {@code Common.ROWS_COUNT_ON_PAGE} baris
	 *       dan dioffset sesuai halaman aktif pada komponen paging.</li>
	 *   <li>Membungkus list hasil ke dalam {@code SimpleListModel} dan menetapkannya ke
	 *       grid dengan renderer {@code SatuanMasterAssetRenderer} yang baru dibuat.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Error dari query Hibernate dibiarkan menyebar. Penggunaan {@code @SuppressWarnings("unchecked")}
	 * diperlukan karena casting hasil {@code list()} Hibernate ke {@code List} bertipe generik.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika perlu menambah kolom pengurutan alternatif, ubah kriteria pada {@code initCriteria(true)}.
	 * Konstanta {@code Common.ROWS_COUNT_ON_PAGE} mengontrol jumlah baris per halaman secara global.</p>
	 *
	 * @param event event ZK yang memicu pencarian; dapat {@code null} jika dipanggil
	 *              secara programatik (bukan dari interaksi pengguna langsung)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<SatuanMasterAsset> satuanMasterAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(satuanMasterAsset);
		grid.setRowRenderer(new SatuanMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <h3>checkNamaSatuanMasterAsset &mdash; Memeriksa Duplikasi Nama Satuan Aset di Database</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Memverifikasi apakah nama satuan yang diinput oleh pengguna sudah ada di database.
	 * Pemeriksaan ini dilakukan sebelum penyimpanan untuk mencegah duplikasi data master
	 * yang dapat menyebabkan inkonsistensi pada modul pengadaan dan inventaris.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menggunakan Hibernate Criteria untuk menghitung jumlah baris pada tabel
	 * {@code SatuanMasterAsset} yang memiliki nama yang sama (persis, case-sensitive
	 * sesuai collation database) dengan nilai di {@code nama} textbox.
	 * Jika entitas yang sedang diedit sudah memiliki ID (mode ubah), menambahkan
	 * kondisi {@code ne("id", ...)} untuk mengecualikan entitas itu sendiri dari
	 * penghitungan, sehingga pengguna tetap bisa menyimpan tanpa mengubah nama.
	 * Mengembalikan {@code true} jika count lebih dari 0 (nama sudah ada).</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Error dari query Hibernate dibiarkan menyebar. Hasil dari {@code uniqueResult()}
	 * di-cast ke {@code Number} untuk menghindari ketergantungan pada tipe numerik
	 * spesifik yang dikembalikan oleh implementasi Hibernate.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika aturan duplikasi berubah (misalnya nama boleh duplikat asalkan kode berbeda),
	 * modifikasi kondisi pada metode ini. Pertimbangkan juga menambahkan UNIQUE INDEX
	 * di level database sebagai lapisan keamanan tambahan.</p>
	 *
	 * @return {@code true} jika nama yang diinput sudah ada di database (duplikat);
	 *         {@code false} jika nama belum ada atau hanya dimiliki oleh entitas
	 *         yang sedang diedit itu sendiri
	 */
	public Boolean checkNamaSatuanMasterAsset() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(SatuanMasterAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim())).add(this.satuanMasterAsset.getId() == null
						? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", this.satuanMasterAsset.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
