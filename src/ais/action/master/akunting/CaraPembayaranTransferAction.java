package ais.action.master.akunting;

import java.util.List;
import java.util.Set;

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
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.CaraPembayaranTransfer;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>CaraPembayaranTransferAction — Pengelola Master Cara Pembayaran Transfer</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Controller ZK berbasis {@link GenericAutowireComposer} yang menangani manajemen data
 * master {@link CaraPembayaranTransfer}. Entitas ini mendefinisikan cara-cara pembayaran
 * melalui transfer bank yang tersedia dalam sistem keuangan, misalnya transfer via BCA,
 * Mandiri, atau BSI. Setiap cara pembayaran transfer dikaitkan dengan sebuah {@link Akun}
 * buku besar utama dan secara opsional dengan {@link Akun} transitori (akun perantara
 * sebelum dana terkonfirmasi) serta dengan {@link SatuanKerja} tertentu agar tiap
 * unit kerja hanya melihat cara pembayaran yang relevan dengan lingkup operasionalnya.
 * Cara pembayaran ini digunakan oleh modul proses transfer ({@code ProsesTransferAction})
 * dan oleh daftar pengajuan transfer ({@code DaftarPengajuanTransferAction}) sebagai
 * referensi saluran pembayaran yang tersedia.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Halaman ZUL menggunakan autowiring ZK untuk menyuntikkan komponen grid, paging,
 * dan field pencarian ke dalam field-field kelas ini. Siklus hidup dimulai dengan
 * {@code doBeforeCompose} (verifikasi keamanan) dilanjutkan {@code doAfterCompose}
 * (inisialisasi paging, timer, filter satuan kerja, hak akses, dan data awal).
 * Filter pencarian mendukung filter aktif, kode, nama, serta filter hierarki satuan
 * kerja menggunakan {@link SatuanKerjaTreeModel}. Formulir tambah/ubah dibangun
 * secara programatis dalam {@code init(CaraPembayaranTransfer)} dan ditampilkan
 * sebagai popup modal. Fitur unik pada halaman ini adalah checkbox interaktif
 * langsung di grid: checkbox "Aktif" dan "Default" dapat diubah tanpa membuka
 * formulir. Checkbox "Default" memastikan hanya satu cara pembayaran per satuan
 * kerja yang berstatus default dengan membatalkan status default record lain
 * secara otomatis.</p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh operasi berjalan di event thread ZK. Penggunaan {@link Common#createDefaultTimer}
 * menunda eksekusi pencarian awal ke timer tick pertama agar UI terlebih dahulu
 * selesai dirender. Sesi Hibernate adalah sesi terkelola yang tidak boleh ditutup
 * secara manual.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Jika ada penambahan field pada {@link CaraPembayaranTransfer} (misalnya nomor rekening
 * atau nama bank), perbarui metode {@code init(CaraPembayaranTransfer)} untuk menambah
 * baris formulir dan metode {@code onSave} untuk menyimpan field baru. Renderer
 * {@code CaraPembayaranTransferRenderer} juga perlu diperbarui agar kolom baru tampil
 * di grid. Logika multi-satuan kerja menggunakan {@link SatuanKerjaTreeModel} yang
 * memuat seluruh hierarki — pastikan perubahan pada struktur satuan kerja tidak
 * mempengaruhi filter ini.</p>
 *
 * @see CaraPembayaranTransfer
 * @see AmbilDataAkunBanbox
 * @see SatuanKerjaTreeModel
 */
public class CaraPembayaranTransferAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * Nomor versi serialisasi untuk kompatibilitas mekanisme serialisasi Java.
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	/** Popup window untuk formulir tambah/ubah cara pembayaran transfer. */
	private MyWindow addWindow;

	/** Komponen paging untuk navigasi halaman daftar cara pembayaran. */
	private Paging paging;

	/** Grid utama penampil daftar {@link CaraPembayaranTransfer}. */
	private MyGrid grid;

	/** Field teks pencarian berdasarkan nama cara pembayaran. */
	private Textbox serachnama;

	/** Field teks pencarian berdasarkan kode cara pembayaran. */
	private Textbox serachkode;

	/** Checkbox untuk menampilkan hanya yang aktif (dicentang) atau semua. */
	private Checkbox searchaktif;

	/** Banbox pemilih satuan kerja sebagai filter hierarki unit kerja. */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** Field input nama cara pembayaran pada formulir. */
	private Textbox nama;

	/** Field input kode unik cara pembayaran pada formulir. */
	private Textbox kode;

	/** Field input deskripsi cara pembayaran pada formulir. */
	private Textbox deskripsi;

	/** Banbox pemilih akun buku besar utama untuk cara pembayaran ini. */
	private AmbilDataAkunBanbox akun;

	/** Banbox pemilih akun transitori (akun perantara) opsional. */
	private AmbilDataAkunBanbox akunTransitori;

	/** Objek {@link CaraPembayaranTransfer} yang sedang aktif di formulir. */
	public CaraPembayaranTransfer caraPembayaranTransfer;

	/** Tombol tambah data baru; visibilitasnya bergantung pada hak CREATE. */
	private MyToolbarbuttonConfig add;

	/** Apakah pengguna memiliki hak UPDATE. */
	private boolean edit;

	/** Apakah pengguna memiliki hak DELETE. */
	private boolean delete;

	/** Banbox pemilih satuan kerja pada formulir tambah/ubah. */
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	/** Model pohon satuan kerja untuk navigasi hierarki unit kerja. */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Mengembalikan string kosong jika nilai null, atau nilai itu sendiri.
	 * Digunakan untuk menghindari NullPointerException saat menampilkan
	 * nilai string di komponen Label atau Textbox ZK.
	 *
	 * <p><b>Tujuan:</b> Helper null-safe untuk konversi nilai String ke
	 * tampilan aman tanpa perlu pengecekan null berulang di setiap titik.</p>
	 *
	 * <p><b>Cara kerja:</b> Jika {@code value} null, kembalikan string kosong "".
	 * Jika tidak null, kembalikan nilai aslinya.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini bersifat stateless dan final secara
	 * efektif. Tidak perlu diubah kecuali ada kebutuhan pemformatan tambahan.</p>
	 *
	 * @param value nilai String yang mungkin null
	 * @return string asli jika tidak null, atau "" jika null
	 */
	private static String text(String value) {
		return value == null ? "" : value;
	}

	/**
	 * Mengevaluasi nilai Boolean secara aman terhadap null, mengembalikan
	 * {@code true} hanya jika nilai secara eksplisit adalah {@link Boolean#TRUE}.
	 *
	 * <p><b>Tujuan:</b> Menghindari NullPointerException saat memanggil
	 * {@code Boolean.booleanValue()} pada referensi yang bisa null. Digunakan
	 * khususnya untuk field Boolean pada entitas Hibernate yang bisa null
	 * karena tipe wrapper dipakai (bukan primitif).</p>
	 *
	 * <p><b>Cara kerja:</b> Menggunakan {@link Boolean#TRUE#equals(Object)}
	 * yang aman terhadap null — jika {@code value} null, equals mengembalikan
	 * false, bukan NullPointerException.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah. Pola ini konsisten dengan
	 * praktik standar di seluruh kodebase AIS untuk menangani Boolean nullable.</p>
	 *
	 * @param value nilai Boolean yang mungkin null
	 * @return {@code true} hanya jika value secara eksplisit Boolean.TRUE; {@code false} untuk null atau Boolean.FALSE
	 */
	private static boolean isTrue(Boolean value) {
		return Boolean.TRUE.equals(value);
	}

	/**
	 * Dipanggil ZK sebelum komponen halaman dibangun. Memverifikasi hak akses
	 * keamanan pengguna melalui {@link Common#doCheckSecurity()} sebelum halaman
	 * dirender.
	 *
	 * <p><b>Tujuan:</b> Gerbang keamanan pertama dalam siklus hidup halaman.
	 * Mencegah halaman dimuat jika sesi pengguna tidak valid atau tidak memiliki
	 * akses ke modul ini.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link Common#doCheckSecurity()} yang
	 * memeriksa validitas sesi dan hak akses modul, kemudian mendelegasikan
	 * ke implementasi super untuk melanjutkan proses komposisi komponen ZK.</p>
	 *
	 * <p><b>Penanganan error:</b> {@link Common#doCheckSecurity()} akan menghentikan
	 * proses dan mengarahkan ke halaman logoff jika sesi tidak valid.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah kecuali ada mekanisme keamanan
	 * tambahan yang perlu dijalankan pada tahap ini.</p>
	 *
	 * @param page halaman ZK yang sedang diproses
	 * @param parent komponen induk dalam hierarki ZK
	 * @param compInfo metadata komponen dari ZUL
	 * @return ComponentInfo dari implementasi super
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Dipanggil ZK setelah seluruh komponen halaman selesai di-autowire. Menginisialisasi
	 * state awal halaman cara pembayaran transfer: validasi sesi, bahasa, filter satuan
	 * kerja, paging, timer refresh, dan hak akses CRUD.
	 *
	 * <p><b>Tujuan:</b> Menyiapkan halaman secara lengkap setelah komponen ZUL
	 * tersedia. Ini adalah titik masuk utama inisialisasi setelah autowiring selesai.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} dan {@link Common#initLaguage()}.</li>
	 *   <li>Validasi sesi dan hak READ; jika tidak ada, logout.</li>
	 *   <li>Mendaftarkan event listener pada {@code searchparent} agar perubahan
	 *       satuan kerja filter langsung memicu pencarian ulang.</li>
	 *   <li>Menginisialisasi {@link SatuanKerjaTreeModel} untuk navigasi hierarki.</li>
	 *   <li>Mengatur visibilitas tombol Tambah berdasarkan hak CREATE.</li>
	 *   <li>Menyimpan flag {@code edit} dan {@code delete} berdasarkan hak UPDATE/DELETE.</li>
	 *   <li>Menginisialisasi paging dengan listener yang memanggil {@code onSearchDefault}.</li>
	 *   <li>Membuat timer default yang memuat data awal pada tick pertama.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari super atau inisialisasi komponen
	 * akan disebarkan ke ZK. Jika sesi tidak valid, pengguna diarahkan ke logoff.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada komponen filter baru (misalnya filter bank),
	 * tambahkan inisialisasinya setelah blok inisialisasi yang sudah ada.</p>
	 *
	 * @param comp komponen root halaman ZK
	 * @throws Exception jika terjadi kesalahan saat inisialisasi
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		}


		if (add != null) { add.setTooltiptext("Tambah"); }
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	/**
	 * Inner class renderer yang mengisi setiap baris grid dengan data dari satu objek
	 * {@link CaraPembayaranTransfer}. Menyertakan checkbox interaktif "Aktif" dan
	 * "Default" yang bisa diubah langsung dari grid tanpa membuka formulir edit.
	 *
	 * <p><b>Tujuan:</b> Menampilkan data cara pembayaran transfer di grid dengan
	 * kolom: Kode (dengan link revisi), Nama, Deskripsi, Akun Utama, Akun Transitori,
	 * Satuan Kerja, checkbox Aktif, checkbox Default, dan tombol Ubah/Hapus.</p>
	 *
	 * <p><b>Cara kerja:</b> Checkbox "Aktif" menyimpan langsung ke database saat
	 * diubah melalui {@link Common#refreshSaveOrUpdate}. Checkbox "Default" lebih
	 * kompleks: saat diaktifkan, terlebih dahulu mencari semua record yang berstatus
	 * default pada satuan kerja yang sama dan membatalkan status default mereka,
	 * kemudian menyimpan status default baru pada record ini. Ini memastikan hanya
	 * satu cara pembayaran default per satuan kerja.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada kolom baru di grid ZUL, tambahkan Label
	 * yang sesuai di sini sesuai urutan kolom. Logika checkbox default bergantung
	 * pada field {@code satuanKerja} dan {@code defaultPembayaran} di entitas.</p>
	 */
	class CaraPembayaranTransferRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Mengisi satu baris grid dengan data dari satu objek {@link CaraPembayaranTransfer},
		 * termasuk checkbox interaktif untuk status aktif dan default pembayaran.
		 *
		 * <p><b>Tujuan:</b> Merender setiap baris pada grid daftar cara pembayaran transfer
		 * dengan kolom yang informatif dan checkbox yang dapat diinteraksikan langsung
		 * tanpa harus membuka popup formulir edit, mempercepat proses manajemen data.</p>
		 *
		 * <p><b>Cara kerja:</b>
		 * <ol>
		 *   <li>Mengcast {@code arg1} ke {@link CaraPembayaranTransfer}.</li>
		 *   <li>Menambahkan Label untuk kode (via RevisiHelper), nama, deskripsi,
		 *       info akun utama, info akun transitori, dan info satuan kerja.</li>
		 *   <li>Checkbox "Aktif": langsung menyimpan perubahan via
		 *       {@link Common#refreshSaveOrUpdate} saat dicentang/dilepas.</li>
		 *   <li>Checkbox "Default": saat dicentang, menonaktifkan status default
		 *       semua cara pembayaran lain pada satuan kerja yang sama sebelum
		 *       menyimpan status default baru. Saat dilepas, cukup menyimpan
		 *       perubahan tanpa dampak ke record lain. Memanggil
		 *       {@code onSearchDefault} agar grid terefresh.</li>
		 *   <li>Tombol Ubah/Hapus via {@link Common#copyEditDeleteButtons}.</li>
		 * </ol>
		 * </p>
		 *
		 * <p><b>Penanganan error:</b> Exception dari penyimpanan disebarkan ke ZK.
		 * Tidak ada rollback eksplisit; sesi Hibernate terkelola oleh framework.</p>
		 *
		 * <p><b>Pemeliharaan:</b> Pastikan urutan appendChild sesuai urutan kolom ZUL.
		 * Jika logika default berubah (misalnya default global tanpa memperhatikan
		 * satuan kerja), ubah filter Criteria pada listener checkbox Default.</p>
		 *
		 * @param arg0 baris grid ZK yang akan diisi
		 * @param arg1 objek data yang akan dicast ke {@link CaraPembayaranTransfer}
		 * @throws Exception jika terjadi kesalahan rendering atau penyimpanan
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final CaraPembayaranTransfer caraPembayaranTransfer = (CaraPembayaranTransfer) arg1;

			RevisiHelper.createNewRevisi(CaraPembayaranTransfer.class, caraPembayaranTransfer,
					text(caraPembayaranTransfer.getKode()).trim())
					.setParent(arg0);
			new Label(text(caraPembayaranTransfer.getNama())).setParent(arg0);
			new Label(text(caraPembayaranTransfer.getDeskripsi())).setParent(arg0);

			new Label(caraPembayaranTransfer.getAkun() == null ? ""
					: caraPembayaranTransfer.getAkun().getKode() + "-" + caraPembayaranTransfer.getAkun().getNama())
					.setParent(arg0);

			new Label(caraPembayaranTransfer.getAkunTransitori() == null ? ""
					: caraPembayaranTransfer.getAkunTransitori().getKode() + "-"
							+ caraPembayaranTransfer.getAkunTransitori().getNama())
					.setParent(arg0);

			new Label(caraPembayaranTransfer.getSatuanKerja() == null ? ""
					: caraPembayaranTransfer.getSatuanKerja().getKode() + "-"
							+ caraPembayaranTransfer.getSatuanKerja().getNama())
					.setParent(arg0);

			final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
			aktif.setChecked(isTrue(caraPembayaranTransfer.getAktif()));
			aktif.setParent(arg0);
			aktif.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					caraPembayaranTransfer.setAktif(aktif.isChecked());
					Common.refreshSaveOrUpdate(caraPembayaranTransfer);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Default");
			checkbox.setChecked(isTrue(caraPembayaranTransfer.getDefaultPembayaran()));
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					if (checkbox.isChecked()) {
						List<CaraPembayaranTransfer> defaults = session.createCriteria(CaraPembayaranTransfer.class)
								.add(Restrictions.eq("defaultPembayaran", true))
								.add(caraPembayaranTransfer.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", caraPembayaranTransfer.getId()))
								.add(caraPembayaranTransfer.getSatuanKerja() == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.eq("satuanKerja", caraPembayaranTransfer.getSatuanKerja()))
								.list();
						for (CaraPembayaranTransfer defaultTransfer : defaults) {
							defaultTransfer.setDefaultPembayaran(false);
							Common.refreshSaveOrUpdate(session, defaultTransfer);
						}
					}
					caraPembayaranTransfer.setDefaultPembayaran(checkbox.isChecked());
					Common.refreshSaveOrUpdate(session, caraPembayaranTransfer);
					session.flush();
					onSearchDefault(null);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, caraPembayaranTransfer, CaraPembayaranTransferAction.this)
					.setParent(arg0);
		}

	}

	/**
	 * Menangani klik tombol Tambah pada toolbar. Membuat objek
	 * {@link CaraPembayaranTransfer} baru dan membuka formulir dalam mode modal.
	 *
	 * <p><b>Tujuan:</b> Menyediakan titik masuk bagi pengguna yang ingin menambahkan
	 * cara pembayaran transfer baru ke master data sistem keuangan.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code init(new CaraPembayaranTransfer())} untuk
	 * membangun formulir kosong, kemudian menampilkan {@code addWindow} dalam mode modal
	 * sehingga pengguna dapat mengisi data baru.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari pembangunan formulir atau pembukaan
	 * modal disebarkan ke ZK untuk ditampilkan sebagai error dialog.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada nilai default yang perlu diisi saat tambah baru
	 * (misalnya satuan kerja dari konteks sesi pengguna), lakukan setelah {@code init}.</p>
	 *
	 * @param event event klik ZK dari tombol Tambah
	 * @throws Exception jika terjadi kesalahan saat membangun formulir atau membuka modal
	 */
	public void onAdd(Event event) throws Exception {
		init(new CaraPembayaranTransfer());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membangun konten formulir tambah/ubah di dalam {@code addWindow} berdasarkan
	 * objek {@link CaraPembayaranTransfer} yang diberikan.
	 *
	 * <p><b>Tujuan:</b> Menyiapkan formulir interaktif cara pembayaran transfer yang
	 * memungkinkan pengguna mengisi kode, nama, deskripsi, akun utama, akun transitori,
	 * dan satuan kerja. Formulir dibangun secara programatis agar mudah dikontrol
	 * dari Java.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mengatur judul window sesuai mode (tambah/ubah).</li>
	 *   <li>Menyimpan referensi objek ke field {@code caraPembayaranTransfer}.</li>
	 *   <li>Membersihkan konten window sebelumnya.</li>
	 *   <li>Membangun Borderlayout dengan Center (grid formulir) dan South (toolbar).</li>
	 *   <li>Menambahkan baris: Kode, Nama (wajib), Deskripsi (5 baris), Akun (wajib),
	 *       Akun Transitori (opsional), Satuan Kerja (opsional).</li>
	 *   <li>Mengisi nilai awal dari objek yang diedit menggunakan setAttribute("akun",...)</li>
	 *   <li>Tombol Batal menutup window; Simpan memanggil {@code onSave}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception saat membangun komponen disebarkan ke ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada field baru, tambahkan MyFormRow yang sesuai.
	 * Perhatikan bahwa akun disimpan via setAttribute/getAttribute bukan via model
	 * binding langsung, sesuai pola {@link AmbilDataAkunBanbox}.</p>
	 *
	 * @param caraPembayaranTransfer objek yang akan ditampilkan/diedit; null-id = mode tambah
	 * @throws Exception jika terjadi kesalahan saat membangun komponen formulir
	 */
	private void init(CaraPembayaranTransfer caraPembayaranTransfer) throws Exception {
		addWindow.setTitle(caraPembayaranTransfer.getId() == null ? "Tambah Cara Pembayaran" : "Ubah Cara Pembayaran");
		this.caraPembayaranTransfer = caraPembayaranTransfer;
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(
				(caraPembayaranTransfer.getKode() == null ? "" : caraPembayaranTransfer.getKode().trim())));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama *"));
		row.appendChild(nama = new Textbox(
				caraPembayaranTransfer.getNama() == null ? "" : caraPembayaranTransfer.getNama().trim()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi"));
		row.appendChild(deskripsi = new Textbox(
				caraPembayaranTransfer.getDeskripsi() == null ? "" : caraPembayaranTransfer.getDeskripsi()));
		deskripsi.setWidth("90%");
		deskripsi.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun *"));
		row.appendChild(akun = new AmbilDataAkunBanbox(false));
		akun.setValue(caraPembayaranTransfer.getAkun() == null ? "" : caraPembayaranTransfer.getAkun().getNama());
		akun.setAttribute("akun", caraPembayaranTransfer.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Transitori"));
		row.appendChild(akunTransitori = new AmbilDataAkunBanbox(false));
		akunTransitori.setValue(caraPembayaranTransfer.getAkunTransitori() == null ? ""
				: caraPembayaranTransfer.getAkunTransitori().toString());
		akunTransitori.setAttribute("akun", caraPembayaranTransfer.getAkunTransitori());
		akunTransitori.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(false));
		satuanKerja.setValue(caraPembayaranTransfer.getSatuanKerja() == null ? ""
				: caraPembayaranTransfer.getSatuanKerja().getKode() + "-"
						+ caraPembayaranTransfer.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", caraPembayaranTransfer.getSatuanKerja());
		satuanKerja.setWidth("90%");

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

	}

	/**
	 * Memvalidasi dan menyimpan data cara pembayaran transfer dari formulir ke database.
	 *
	 * <p><b>Tujuan:</b> Melakukan validasi berlapis pada input pengguna sebelum
	 * menyimpan atau memperbarui entitas {@link CaraPembayaranTransfer}. Mengembalikan
	 * {@code true} jika berhasil agar caller dapat menutup popup, atau {@code false}
	 * jika validasi gagal dengan pesan yang sesuai ditampilkan ke pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Validasi nama tidak boleh kosong.</li>
	 *   <li>Validasi akun utama harus dipilih (getAttribute("akun") tidak null).</li>
	 *   <li>Cek duplikasi kode via {@code checkKode()} — kode boleh kosong, hanya
	 *       duplikat yang tidak boleh.</li>
	 *   <li>Cek duplikasi nama via {@code checkNama()}.</li>
	 *   <li>Jika mode ubah, reload entitas dari sesi Hibernate untuk menghindari
	 *       stale object exception.</li>
	 *   <li>Mengisi field entitas dari input formulir termasuk akun transitori dan
	 *       satuan kerja yang bersifat opsional (bisa null).</li>
	 *   <li>Menyimpan via {@link Common#refreshSaveOrUpdate} dan flush sesi.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate disebarkan ke ZK. Tidak ada
	 * penanganan ConstraintViolation eksplisit; validasi duplikat dilakukan di atas
	 * untuk mencegah hal ini.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada field wajib baru, tambahkan validasi di awal.
	 * Akun transitori dan satuan kerja sengaja dibiarkan opsional (bisa null).</p>
	 *
	 * @param event event ZK dari tombol Simpan (bisa null)
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan database saat penyimpanan
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nama dengan nama cara pembayaran transfer yang sesuai; (2) Pastikan nama tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (akun.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Akun transfer melalui field pencarian akun yang tersedia; (2) Pastikan akun yang sesuai sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		boolean i = checkKode();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Kode Item sudah ada di database. Langkah yang dapat dilakukan: (1) Gunakan kode item yang berbeda dan belum terdaftar; (2) Periksa daftar cara pembayaran yang sudah ada melalui menu pencarian; (3) ulangi proses simpan dengan kode baru. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		i = checkNama();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Nama Item sudah ada di database. Langkah yang dapat dilakukan: (1) Gunakan nama item yang berbeda dan belum terdaftar; (2) Periksa daftar cara pembayaran yang sudah ada melalui menu pencarian; (3) ulangi proses simpan dengan nama baru. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (caraPembayaranTransfer.getId() != null) {
			caraPembayaranTransfer = (CaraPembayaranTransfer) session.load(CaraPembayaranTransfer.class,
					caraPembayaranTransfer.getId());
		}
		caraPembayaranTransfer.setAkun((Akun) akun.getAttribute("akun"));
		caraPembayaranTransfer.setAkunTransitori((Akun) akunTransitori.getAttribute("akun"));
		caraPembayaranTransfer.setKode(kode.getValue() == null ? "" : kode.getValue().trim());
		caraPembayaranTransfer.setNama(nama.getValue() == null ? "" : nama.getValue().trim());
		caraPembayaranTransfer.setDeskripsi(deskripsi.getValue());

		caraPembayaranTransfer.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		Common.refreshSaveOrUpdate(session, caraPembayaranTransfer);
		session.flush();

		return true;
	}

	/**
	 * Membangun objek Hibernate Criteria untuk query daftar {@link CaraPembayaranTransfer}
	 * berdasarkan filter aktif pada form pencarian, dengan opsi pengurutan.
	 *
	 * <p><b>Tujuan:</b> Memusatkan logika pembuatan query pencarian di satu tempat
	 * sehingga dapat digunakan baik untuk menghitung total (tanpa order, untuk paging)
	 * maupun untuk mengambil data aktual (dengan order, untuk grid). Ini menghindari
	 * duplikasi logika filter antara perhitungan paging dan pengambilan data.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mendapatkan satuan kerja yang dipilih dari filter {@code searchparent}.</li>
	 *   <li>Mengambil set satuan kerja pengguna via {@code SekolahUtil.ambilSatuanKerjas()}.</li>
	 *   <li>Jika ada filter parent, membersihkan set dan mengisi ulang dengan parent
	 *       beserta seluruh turunannya via {@link SatuanKerjaTreeModel#getChildsSet}.</li>
	 *   <li>Membangun Criteria dengan empat klausa filter:
	 *     <ul>
	 *       <li>Filter satuan kerja: data yang satuanKerja-nya null (global) atau
	 *           termasuk dalam set satuan kerja pengguna.</li>
	 *       <li>Filter aktif: menampilkan hanya yang aktif (null atau true) jika
	 *           checkbox aktif dicentang.</li>
	 *       <li>Filter kode: ILIKE jika teks pencarian kode tidak kosong.</li>
	 *       <li>Filter nama: ILIKE jika teks pencarian nama tidak kosong.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Jika {@code order} true, menambahkan ORDER BY kode ASC.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate disebarkan ke caller.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada filter baru, tambahkan klausa .add() di sini.
	 * Logika satuan kerja menggunakan OR(isNull, in(satuanKerjas)) untuk mengakomodasi
	 * data global (tanpa satuan kerja) dan data per unit.</p>
	 *
	 * @param order jika {@code true}, tambahkan ORDER BY kode ASC ke query
	 * @return objek {@link Criteria} yang siap dieksekusi dengan .list() atau .setProjection()
	 */
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CaraPembayaranTransfer.class)

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(serachkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", serachkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(serachnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", serachnama.getValue().trim(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.asc("kode"));
		return criteria;
	}

	/**
	 * Menjalankan pencarian data {@link CaraPembayaranTransfer} dan memperbarui
	 * tampilan grid serta informasi paging.
	 *
	 * <p><b>Tujuan:</b> Handler utama event pencarian yang dipanggil saat filter
	 * berubah, paging dinavigasi, atau setelah operasi simpan/hapus. Memastikan
	 * grid selalu menampilkan data terkini sesuai filter aktif.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@link Common#initPaging(Criteria, Paging)} dengan Criteria
	 *       tanpa order untuk menghitung jumlah total record (untuk paging).</li>
	 *   <li>Mengambil data halaman aktif menggunakan Criteria dengan order, dibatasi
	 *       oleh {@link Common#ROWS_COUNT_ON_PAGE} dan offset paging aktif.</li>
	 *   <li>Membuat {@link SimpleListModel} dan menyetel renderer baru ke grid.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate disebarkan ke ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah kecuali ada kebutuhan sorting
	 * tambahan atau logika post-processing hasil query.</p>
	 *
	 * @param event event ZK yang memicu pencarian; bisa null jika dipanggil programatis
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CaraPembayaranTransfer> caraPembayaranTransfer = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(caraPembayaranTransfer);
		grid.setRowRenderer(new CaraPembayaranTransferRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Memeriksa duplikasi kode cara pembayaran transfer di database, dengan pengecualian
	 * untuk record yang sedang diedit. Kode boleh kosong (tidak diperiksa jika kosong).
	 *
	 * <p><b>Tujuan:</b> Mencegah dua cara pembayaran memiliki kode yang sama karena
	 * kode digunakan sebagai identifikasi ringkas dalam laporan dan proses transfer.
	 * Dipanggil dari {@code onSave} sebelum penyimpanan.</p>
	 *
	 * <p><b>Cara kerja:</b> Jika kode kosong, langsung mengembalikan false (tidak ada
	 * duplikat). Jika tidak kosong, menghitung record dengan kode yang sama menggunakan
	 * Hibernate Criteria dengan {@link Projections#rowCount()}. Mode edit ditangani
	 * dengan Restrictions.ne("id",...) untuk mengecualikan record saat ini.</p>
	 *
	 * <p><b>Return:</b> {@code true} jika kode sudah dipakai; {@code false} jika aman.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika keunikan kode perlu dibatasi per satuan kerja
	 * (bukan global), tambahkan filter satuan kerja pada Criteria.</p>
	 *
	 * @return {@code true} jika kode sudah ada di database; {@code false} jika belum ada
	 */
	public Boolean checkKode() {
		if (kode == null || kode.getValue() == null || kode.getValue().trim().isEmpty()) {
			return false;
		}

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(CaraPembayaranTransfer.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.caraPembayaranTransfer.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.caraPembayaranTransfer.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	/**
	 * Memeriksa duplikasi nama cara pembayaran transfer di database, dengan pengecualian
	 * untuk record yang sedang diedit.
	 *
	 * <p><b>Tujuan:</b> Memastikan tidak ada dua cara pembayaran dengan nama yang identik
	 * agar pengguna tidak bingung saat memilih cara pembayaran dari daftar. Dipanggil
	 * dari {@code onSave} setelah pemeriksaan kode.</p>
	 *
	 * <p><b>Cara kerja:</b> Menghitung record dengan nama sama menggunakan Hibernate
	 * Criteria dengan {@link Projections#rowCount()}. Untuk mode edit, record yang
	 * sedang diedit dikecualikan via Restrictions.ne("id",...).</p>
	 *
	 * <p><b>Return:</b> {@code true} jika nama sudah dipakai; {@code false} jika aman.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Pemeriksaan ini bersifat case-sensitive (menggunakan
	 * Restrictions.eq). Jika diperlukan pemeriksaan case-insensitive, ganti dengan
	 * Restrictions.ilike.</p>
	 *
	 * @return {@code true} jika nama sudah ada di database; {@code false} jika belum ada
	 */
	public Boolean checkNama() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(CaraPembayaranTransfer.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.caraPembayaranTransfer.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.caraPembayaranTransfer.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	/**
	 * Implementasi antarmuka {@link DataInitDefault} yang memungkinkan controller lain
	 * membuka formulir edit cara pembayaran transfer secara programatis, misalnya
	 * dari tombol Ubah pada baris grid yang menggunakan pola generik AIS.
	 *
	 * <p><b>Tujuan:</b> Menyediakan titik masuk standar yang kompatibel dengan pola
	 * tombol Ubah generik ({@link Common#copyEditDeleteButtons}) di seluruh modul AIS.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengcast parameter {@link GeneralValueObject} ke
	 * {@link CaraPembayaranTransfer}, membangun formulir via {@code init}, dan membuka
	 * window modal.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception disebarkan ke ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jangan mengubah signature; ini kontrak interface
	 * {@link DataInitDefault}.</p>
	 *
	 * @param obj objek {@link GeneralValueObject} yang dicast ke {@link CaraPembayaranTransfer}
	 * @throws Exception jika terjadi kesalahan saat membangun formulir atau modal
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		CaraPembayaranTransfer caraPembayaranTransfer = (CaraPembayaranTransfer) obj;
		init(caraPembayaranTransfer);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
