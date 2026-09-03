package ais.action.master.akunting;

import java.util.List;
import java.util.Set;

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
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.JenisKasBesar;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>JenisKasBesarAction — Controller Master Jenis Kas Besar</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah ZK GenericAutowireComposer yang mengelola halaman master
 * Jenis Kas Besar dalam modul akuntansi. Kas besar (petty cash atau kas besar
 * operasional) adalah kas fisik yang dipegang oleh satuan kerja tertentu untuk
 * kebutuhan operasional sehari-hari. Setiap {@link JenisKasBesar} merepresentasikan
 * satu rekening kas besar pada satuan kerja tertentu, dengan akun sumber (dari mana
 * dana kas besar diambil) dan akun penerima (tempat kas besar dicatat). Pengguna
 * dengan hak yang sesuai dapat menambah, mengubah, dan mengelola status kas besar
 * melalui antarmuka ini.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Siklus hidup halaman mengikuti pola standar ZK GenericAutowireComposer:
 * <ol>
 *   <li>{@code doBeforeCompose} — pemeriksaan keamanan sebelum halaman dikompilasi.</li>
 *   <li>{@code doAfterCompose} — inisialisasi komponen: mengecek sesi dan hak READ,
 *       menginisialisasi bahasa, mendaftarkan listener pada filter satuan kerja,
 *       menginisialisasi model pohon satuan kerja, mengatur tombol tambah dan
 *       hak akses, menginisialisasi paging, dan membuat timer default untuk
 *       refresh awal.</li>
 *   <li>Inner class {@code JenisKasBesarRenderer} — merender setiap baris grid
 *       dengan: kode (revisi history), nama, keterangan, akun sumber, akun penerima,
 *       tanggal saldo awal, satuan kerja, checkbox Aktif (inline), checkbox Default
 *       (inline), dan tombol Ubah/Hapus.</li>
 *   <li>{@code init(GeneralValueObject)} — implementasi DataInitDefault untuk
 *       membuka form edit dari renderer.</li>
 *   <li>{@code onAdd} — membuka form untuk menambahkan kas besar baru.</li>
 *   <li>Metode {@code init(JenisKasBesar)} (private) — membangun form modal dengan
 *       field: satuan kerja, kode, nama, tanggal saldo awal, akun sumber, akun
 *       penerima, dan keterangan.</li>
 *   <li>{@code onSave} — memvalidasi input (satuan kerja, nama, akun sumber, akun
 *       penerima wajib diisi), menyimpan entitas, dan memanggil
 *       {@code JenisKasBesar.reloadDefault()} untuk memperbarui cache.</li>
 *   <li>{@code initCriteria} — membangun Hibernate Criteria dengan filter satuan
 *       kerja (hierarki), status aktif, kode, dan nama.</li>
 *   <li>{@code onSearchDefault} — menjalankan query dan memperbarui grid.</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b><br>
 * Semua operasi UI berjalan di thread ZK event-dispatcher. Timer default ZK
 * digunakan untuk refresh awal data. Operasi penyimpanan di {@code onSave}
 * menggunakan session Hibernate terkelola dan memanggil {@code session.flush()}
 * secara eksplisit setelah penyimpanan untuk memastikan data segera ditulis
 * ke database sebelum {@code reloadDefault()} dipanggil.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * - Penentuan hierarki satuan kerja dalam {@code init(JenisKasBesar)} menggunakan
 *   {@code PerguruanTinggiUtil.getPerguruanTinggi()} sebagai root; ini mengasumsikan
 *   bahwa satu instance aplikasi hanya melayani satu perguruan tinggi. Jika multi-tenant,
 *   logika ini perlu disesuaikan.<br>
 * - {@code JenisKasBesar.reloadDefault()} harus dipanggil setelah setiap perubahan
 *   agar cache in-memory diperbarui; ini sudah dilakukan di {@code onSave}.<br>
 * - Saat tambah data baru, satuan kerja diisi otomatis dari konteks pengguna
 *   ({@code Common.getSatuanKerja()}) dalam blok try-catch karena tidak semua
 *   pengguna pasti memiliki satuan kerja.<br>
 * - Checkbox Aktif dan Default di renderer langsung menyimpan ke database via
 *   {@code Common.refreshSaveOrUpdate}; keduanya dinonaktifkan (disabled) bagi
 *   pengguna tanpa hak UPDATE (flag {@code edit}), sama seperti tombol Ubah/Hapus
 *   pada baris yang sama, dan listener {@code onCheck} juga menolak perubahan
 *   jika {@code edit} bernilai false sebagai lapis pertahanan kedua.</p>
 */
public class JenisKasBesarAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * Serial version UID untuk kompatibilitas serialisasi ZK session.
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox serachnama;
	private Textbox serachkode;
	/** Checkbox filter untuk hanya menampilkan kas besar yang aktif. */
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox kode;
	private Textbox keterangan;
	/** Komponen pencarian akun sumber kas besar (dari mana dana diambil). */
	private AmbilDataAkunBanbox akun;

	/** Objek jenis kas besar yang sedang diedit atau baru dibuat. */
	public JenisKasBesar jenisKasBesar;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;

	/** Komponen pencarian satuan kerja pada form edit/tambah. */
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	/** Komponen pencarian satuan kerja pada toolbar filter. */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** DateBox untuk tanggal saldo awal kas besar. */
	private MyDatebox tanggal;

	/** Komponen pencarian akun penerima kas besar (tempat kas dicatat). */
	private AmbilDataAkunBanbox akunPenerima;

	/**
	 * <b>Tujuan:</b> Melakukan pemeriksaan keamanan sebelum halaman ZUL dikompilasi.
	 * Dipanggil oleh framework ZK sebagai langkah paling awal dalam siklus hidup
	 * komposisi halaman, sebelum komponen apapun diinstansiasi atau di-autowire.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} yang memverifikasi bahwa pengguna
	 * saat ini terautentikasi dan memiliki hak akses ke halaman master kas besar.
	 * Jika tidak berhak, pengguna diarahkan ke halaman logoff. Kemudian memanggil
	 * implementasi super untuk melanjutkan proses komposisi halaman secara normal.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari super dikembalikan ke framework ZK untuk ditangani.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan hapus pemanggilan keamanan. Logika yang memerlukan komponen ZUL
	 * harus diletakkan di {@link #doAfterCompose(Component)}.</p>
	 *
	 * @param page     halaman ZK yang sedang dikompilasi
	 * @param parent   komponen induk dari composer ini
	 * @param compInfo metadata komponen dari ZUL
	 * @return {@code ComponentInfo} dari implementasi super
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Menginisialisasi seluruh komponen UI dan logika bisnis halaman
	 * master jenis kas besar setelah framework ZK selesai meng-autowire komponen ZUL.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah inisialisasi yang dilakukan secara berurutan:
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan autowiring.</li>
	 *   <li>Memanggil {@code Common.initLaguage()} untuk lokalisasi bahasa.</li>
	 *   <li>Memeriksa sesi pengguna dan hak READ; jika tidak valid, arahkan ke logoff.</li>
	 *   <li>Mendaftarkan listener pada {@code searchparent} (filter satuan kerja)
	 *       agar pencarian diperbarui saat satuan kerja dipilih di filter.</li>
	 *   <li>Menginisialisasi {@code SatuanKerjaTreeModel} untuk dukungan filter hierarki
	 *       (mencakup satuan kerja yang dipilih beserta seluruh turunannya).</li>
	 *   <li>Mengatur visibilitas tombol "Tambah" berdasarkan hak CREATE (null-safe
	 *       dengan dua pemeriksaan if).</li>
	 *   <li>Menetapkan flag {@code edit} dan {@code delete} sesuai hak akses UPDATE
	 *       dan DELETE.</li>
	 *   <li>Menginisialisasi paging dengan listener standar yang memanggil
	 *       {@code onSearchDefault(null)} saat halaman berubah.</li>
	 *   <li>Membuat timer default ZK yang memanggil {@code onSearchDefault(null)}
	 *       untuk menampilkan data awal setelah siklus event pertama selesai.</li>
	 * </ol>
	 * Catatan: Berbeda dengan kebanyakan action, {@code doAfterCompose} di sini
	 * tidak langsung memanggil {@code onSearchDefault}, melainkan menggunakan
	 * timer untuk menunda pemanggilan pertama satu siklus event.
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari super atau inisialisasi komponen diteruskan ke framework ZK.
	 * Pengecekan sesi di awal mencegah eksekusi lanjutan jika pengguna tidak berhak.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Penggunaan timer untuk {@code onSearchDefault} pertama memberikan waktu bagi
	 * komponen ZUL untuk selesai dirender sebelum data dimuat. Ini berguna jika
	 * ada komponen yang memerlukan layout pass sebelum data dapat ditampilkan.</p>
	 *
	 * @param comp komponen root ZUL yang telah selesai di-autowire
	 * @throws Exception jika terjadi error saat inisialisasi komponen atau framework ZK
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
	 * <h3>JenisKasBesarRenderer — Renderer Baris Grid Jenis Kas Besar</h3>
	 *
	 * <p><b>Untuk apa:</b><br>
	 * Inner class ini merender setiap baris pada grid master jenis kas besar.
	 * Menampilkan informasi lengkap tentang setiap kas besar termasuk identitas
	 * (kode dan nama), informasi akuntansi (akun sumber dan penerima), periode
	 * (tanggal saldo awal), dan kepemilikan (satuan kerja). Checkbox Aktif dan
	 * Default dapat diubah langsung di grid tanpa perlu membuka form edit.</p>
	 *
	 * <p><b>Cara kerja render:</b><br>
	 * Untuk setiap objek {@link JenisKasBesar}:
	 * <ol>
	 *   <li>Menampilkan kode menggunakan widget revisi history
	 *       ({@code RevisiHelper.createNewRevisi}).</li>
	 *   <li>Menampilkan nama sebagai Label.</li>
	 *   <li>Menampilkan keterangan.</li>
	 *   <li>Menampilkan akun sumber menggunakan {@code MyLabelAgakKecil} (font sedikit lebih kecil)
	 *       dengan format "kode-nama".</li>
	 *   <li>Menampilkan akun penerima dengan format yang sama.</li>
	 *   <li>Menampilkan tanggal saldo awal menggunakan format tanggal standar.</li>
	 *   <li>Menampilkan nama satuan kerja dengan format "kode-nama".</li>
	 *   <li>Checkbox "Aktif" yang dapat diubah langsung; perubahan langsung disimpan
	 *       via {@code Common.refreshSaveOrUpdate}.</li>
	 *   <li>Checkbox "Default" yang dapat diubah langsung; menandai satu kas besar
	 *       sebagai default untuk digunakan secara otomatis.</li>
	 *   <li>Tombol Ubah/Hapus melalui {@code Common.copyEditDeleteButtons} yang
	 *       menerima referensi {@code JenisKasBesarAction.this} sebagai
	 *       {@code DataInitDefault} untuk membuka form edit.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading:</b><br>
	 * Semua listener event berjalan di thread ZK event-dispatcher. Penyimpanan
	 * checkbox menggunakan {@code Common.refreshSaveOrUpdate} yang sinkron.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Urutan penambahan komponen ke {@code arg0} harus sesuai dengan urutan kolom
	 * di file ZUL. {@code MyLabelAgakKecil} digunakan untuk kolom akun agar nama
	 * akun yang panjang tetap terbaca dalam kolom yang relatif sempit.</p>
	 */
	class JenisKasBesarRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris grid dengan data dari objek
		 * {@link JenisKasBesar}, menampilkan semua kolom informasi beserta
		 * kontrol interaktif checkbox dan tombol aksi.
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Metode ini dipanggil oleh framework ZK untuk setiap item dalam model daftar.
		 * Semua komponen dibuat secara programatik dan ditambahkan ke baris ({@code arg0})
		 * sesuai urutan kolom. Null check dilakukan pada properti yang bisa bernilai null
		 * (akun, akunPenerima, satuanKerja) menggunakan operator ternary untuk memberikan
		 * string kosong sebagai fallback. Checkbox aktif dan default dinonaktifkan
		 * (disabled) berdasarkan flag {@code edit} (hak UPDATE), konsisten dengan tombol
		 * Ubah/Hapus pada baris yang sama yang dipagari oleh {@code Common.copyEditDeleteButtons}
		 * memakai flag {@code edit}/{@code delete} yang sama. Listener {@code onCheck} juga
		 * menolak penyimpanan jika {@code edit} bernilai false, sebagai lapis pertahanan kedua
		 * terhadap permintaan AU yang dibuat langsung tanpa melalui UI (bypass disabled state).</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Exception dari listener checkbox diteruskan ke framework ZK. Tidak ada
		 * penanganan khusus karena {@code refreshSaveOrUpdate} sudah menangani
		 * exception Hibernate secara internal.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Jika ada kolom baru yang ditambahkan di ZUL, tambahkan juga di sini
		 * pada posisi yang tepat. Gunakan {@code MyLabelAgakKecil} untuk teks
		 * yang berpotensi panjang seperti nama akun.</p>
		 *
		 * @param arg0 baris ZK yang akan diisi komponen UI
		 * @param arg1 objek data, dicast ke {@link JenisKasBesar}
		 * @throws Exception jika terjadi error saat membuat komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisKasBesar jenisKasBesar = (JenisKasBesar) arg1;

			RevisiHelper
					.createNewRevisi(JenisKasBesar.class, jenisKasBesar,
							jenisKasBesar.getKode() == null ? "" : jenisKasBesar.getKode().trim().toString())
					.setParent(arg0);
			new Label(jenisKasBesar.getNama()).setParent(arg0);
			new Label(jenisKasBesar.getKeterangan()).setParent(arg0);

			new MyLabelAgakKecil(jenisKasBesar.getAkun() == null ? ""
					: jenisKasBesar.getAkun().getKode() + "-" + jenisKasBesar.getAkun().getNama()).setParent(arg0);

			new MyLabelAgakKecil(jenisKasBesar.getAkunPenerima() == null ? ""
					: jenisKasBesar.getAkunPenerima().getKode() + "-" + jenisKasBesar.getAkunPenerima().getNama())
					.setParent(arg0);

			new Label(Common.dateFormat.get().format(jenisKasBesar.getTanggal())).setParent(arg0);

			new Label(jenisKasBesar.getSatuanKerja() == null ? ""
					: jenisKasBesar.getSatuanKerja().getKode() + "-" + jenisKasBesar.getSatuanKerja().getNama())
					.setParent(arg0);

			final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
			aktif.setDisabled(!edit);
			aktif.setChecked(jenisKasBesar.getAktif());
			aktif.setParent(arg0);
			aktif.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (!edit) {
						return;
					}
					jenisKasBesar.setAktif(aktif.isChecked());
					Common.refreshSaveOrUpdate(jenisKasBesar);
				}
			});

			final MyCheckboxConfig defaultData = new MyCheckboxConfig("Default");
			defaultData.setDisabled(!edit);
			defaultData.setChecked(jenisKasBesar.getDefaultData());
			defaultData.setParent(arg0);
			defaultData.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (!edit) {
						return;
					}
					jenisKasBesar.setDefaultData(defaultData.isChecked());
					Common.refreshSaveOrUpdate(jenisKasBesar);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisKasBesar, JenisKasBesarAction.this).setParent(arg0);

		}

	}

	/**
	 * <b>Tujuan:</b> Implementasi dari antarmuka {@link DataInitDefault} yang
	 * memungkinkan komponen lain (khususnya {@code Common.copyEditDeleteButtons})
	 * membuka form edit melalui referensi generik {@link GeneralValueObject}.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menerima objek {@link GeneralValueObject}, melakukan downcast ke
	 * {@link JenisKasBesar}, menyimpan referensinya ke field instance
	 * ({@code jenisKasBesar}), lalu memanggil metode {@code init(JenisKasBesar)}
	 * (private) untuk membangun form edit secara programatik. Setelah form siap,
	 * jendela modal {@code addWindow} ditampilkan kepada pengguna.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * ClassCastException terjadi jika objek bukan {@link JenisKasBesar}; ini
	 * tidak seharusnya terjadi dalam alur normal. Exception lain dari {@code init}
	 * diteruskan ke framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini diperlukan untuk kontrak {@code DataInitDefault}. Tidak perlu
	 * diubah kecuali ada perubahan signifikan pada cara form edit dibuka atau
	 * pada tipe entitas yang dikelola.</p>
	 *
	 * @param obj objek {@link GeneralValueObject} yang merupakan instance {@link JenisKasBesar}
	 * @throws Exception jika terjadi error saat membangun form atau menampilkan modal
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisKasBesar = (JenisKasBesar) obj;
		init(jenisKasBesar);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Menangani aksi pengguna ketika menekan tombol "Tambah"
	 * pada toolbar halaman, membuka form untuk menambahkan jenis kas besar baru.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat instance {@link JenisKasBesar} baru (kosong) dan memanggil
	 * {@code init(JenisKasBesar)} untuk membangun form secara programatik.
	 * Kemudian menampilkan jendela modal {@code addWindow}. Berbeda dengan
	 * metode {@code onAdd} di kelas lain, di sini terdapat try-catch pada
	 * pemanggilan {@code addWindow.onModal()} untuk menampilkan error jika
	 * terjadi masalah saat membuka modal (misalnya layout issue).</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari {@code init(JenisKasBesar)} diteruskan ke framework ZK.
	 * Exception dari {@code addWindow.onModal()} ditangkap dan ditampilkan
	 * melalui {@code Common.tampilErrorJikaAdmin} sehingga hanya admin yang
	 * melihat detail error teknis, sementara pengguna biasa melihat pesan umum.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Try-catch pada {@code onModal()} adalah pola defensif untuk menangkap
	 * error ZK yang kadang terjadi saat membangun modal dinamis yang kompleks.
	 * Jika error ini terjadi berulang, investigasi apakah ada masalah pada
	 * struktur layout yang dibangun di {@code init(JenisKasBesar)}.</p>
	 *
	 * @param event event ZK dari klik tombol "Tambah"
	 * @throws Exception jika terjadi error fatal saat membuat objek atau membangun form
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisKasBesar());
		addWindow.setVisible(true);
		try {
			addWindow.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * <b>Tujuan:</b> Membangun dan menginisialisasi form tambah/ubah jenis kas besar
	 * secara programatik di dalam jendela modal {@code addWindow}, dengan field-field
	 * yang diperlukan untuk mendefinisikan kas besar baru atau mengubah yang sudah ada.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah pembangunan form:
	 * <ol>
	 *   <li>Mengatur judul jendela dan menyimpan referensi entitas.</li>
	 *   <li>Membersihkan konten lama dari {@code addWindow}.</li>
	 *   <li>Membangun Borderlayout dengan Center (form input) dan South (toolbar).</li>
	 *   <li>Menentukan hierarki satuan kerja untuk form: mengambil perguruan tinggi
	 *       via {@code PerguruanTinggiUtil.getPerguruanTinggi()}, menggunakan
	 *       satuan kerja PT sebagai root, dan membangun set satuan kerja yang valid.</li>
	 *   <li>Mengisi satuan kerja default dari konteks pengguna ({@code Common.getSatuanKerja()})
	 *       jika objek baru dan satuan kerja belum terisi; dalam blok try-catch karena
	 *       tidak semua pengguna memiliki satuan kerja.</li>
	 *   <li>Menambahkan field form:
	 *       <ul>
	 *         <li>Satuan Kerja (AmbilDataSatuanKerjaBanbox wajib) — menentukan unit
	 *             yang memiliki kas besar ini.</li>
	 *         <li>Kode (Textbox) — kode identifikasi kas besar.</li>
	 *         <li>Nama (Textbox) — nama deskriptif kas besar.</li>
	 *         <li>Tanggal Saldo Awal (MyDatebox readonly) — tanggal pembukaan kas besar.</li>
	 *         <li>Akun Sumber (AmbilDataAkunBanbox wajib) — akun asal pengisian kas besar.</li>
	 *         <li>Akun Penerima (AmbilDataAkunBanbox wajib) — akun pencatatan kas besar.</li>
	 *         <li>Keterangan (Textbox multiline 3 baris).</li>
	 *       </ul>
	 *   </li>
	 *   <li>Toolbar South berisi tombol Batal dan Simpan.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Try-catch pada pengisian satuan kerja default mencegah error jika pengguna
	 * tidak memiliki satuan kerja. Exception dari konstruksi komponen diteruskan
	 * ke pemanggil.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Format tanggal pada DateBox menggunakan {@code Common.dateFormat.get().toPattern()};
	 * pastikan konsisten dengan format yang diharapkan di seluruh aplikasi.
	 * Readonly pada DateBox berarti pengguna hanya bisa memilih melalui kalender popup.</p>
	 *
	 * @param jenisKasBesar objek {@link JenisKasBesar} yang akan diedit (ID tidak null)
	 *                      atau objek baru (ID null)
	 * @throws Exception jika terjadi error saat membangun komponen ZK
	 */
	private void init(JenisKasBesar jenisKasBesar) throws Exception {
		addWindow.setTitle(jenisKasBesar.getId() == null ? "Tambah Jenis Kas Besar" : "Ubah Jenis Kas Besar");
		this.jenisKasBesar = jenisKasBesar;
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

		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();

		SatuanKerja parent = pt == null ? null : pt.getSatuanKerja();

		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		try {
			if (jenisKasBesar.getSatuanKerja() == null) {
				jenisKasBesar.setSatuanKerja(Common.getSatuanKerja());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/JenisKasBesarAction.java:547");
			// pengguna mungkin tidak memiliki satuan kerja; biarkan field kosong
		}

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja *"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(jenisKasBesar.getSatuanKerja() == null ? "" : jenisKasBesar.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", jenisKasBesar.getSatuanKerja());

		row.appendChild(satuanKerja);

		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox((jenisKasBesar.getKode() == null ? "" : jenisKasBesar.getKode().trim())));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(jenisKasBesar.getNama() == null ? "" : jenisKasBesar.getNama().trim()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Saldo Awal *"));
		row.appendChild(tanggal = new MyDatebox(jenisKasBesar.getTanggal()));
		tanggal.setFormat(Common.dateFormat.get().toPattern());
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Sumber Kas Besar *"));
		row.appendChild(akun = new AmbilDataAkunBanbox(false));
		akun.setValue(jenisKasBesar.getAkun() == null ? "" : jenisKasBesar.getAkun().getNama());
		akun.setAttribute("akun", jenisKasBesar.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Penerima Kas Besar *"));
		row.appendChild(akunPenerima = new AmbilDataAkunBanbox(false));
		akunPenerima
				.setValue(jenisKasBesar.getAkunPenerima() == null ? "" : jenisKasBesar.getAkunPenerima().toString());
		akunPenerima.setAttribute("akun", jenisKasBesar.getAkunPenerima());
		akunPenerima.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(jenisKasBesar.getKeterangan() == null ? "" : jenisKasBesar.getKeterangan()));
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

	}

	/**
	 * <b>Tujuan:</b> Memvalidasi input pengguna dan menyimpan data jenis kas besar
	 * ke database, kemudian memperbarui cache in-memory agar perubahan segera
	 * berlaku di seluruh aplikasi.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Proses validasi dan penyimpanan:
	 * <ol>
	 *   <li><b>Validasi satuan kerja:</b> Harus dipilih; jika null, tampilkan peringatan
	 *       dan kembalikan false.</li>
	 *   <li><b>Validasi nama:</b> Tidak boleh kosong setelah trim.</li>
	 *   <li><b>Validasi akun sumber:</b> Harus dipilih melalui banbox; jika null,
	 *       tampilkan peringatan.</li>
	 *   <li><b>Validasi akun penerima:</b> Harus dipilih; berbeda dengan akun sumber
	 *       yang merupakan asal dana, akun penerima adalah tempat kas besar dicatat.</li>
	 *   <li><b>Load entity (mode ubah):</b> Jika ID tidak null, memuat ulang entity
	 *       dari database menggunakan {@code session.load} untuk mendapatkan state
	 *       terkini dan menghindari konflik versi Hibernate.</li>
	 *   <li><b>Pengisian data:</b> Menetapkan semua field dari komponen form ke entity.</li>
	 *   <li><b>Simpan dan flush:</b> Menggunakan {@code Common.refreshSaveOrUpdate}
	 *       kemudian {@code session.flush()} secara eksplisit untuk memastikan
	 *       data ditulis ke database segera.</li>
	 *   <li><b>Reload cache:</b> Memanggil {@code JenisKasBesar.reloadDefault()} untuk
	 *       memperbarui cache static in-memory yang digunakan oleh modul lain.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Setiap validasi yang gagal menampilkan dialog menggunakan
	 * {@code MyMessageboxConfig.EXCLAMATION} atau {@code INFORMATION} dan
	 * mengembalikan {@code false}. Exception dari Hibernate diteruskan ke
	 * framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Panggilan {@code session.flush()} setelah {@code refreshSaveOrUpdate} sangat
	 * penting di sini karena {@code reloadDefault()} yang dipanggil sesudahnya
	 * membaca data dari database; tanpa flush, data terbaru belum tentu tersedia.
	 * Perhatikan bahwa {@code akunPenerima.getValue()} menggunakan nilai toString()
	 * dari objek akun untuk display di banbox; nilai aktual diambil dari attribute.</p>
	 *
	 * @param event event ZK yang memicu penyimpanan
	 * @return {@code true} jika berhasil disimpan dan form boleh ditutup,
	 *         {@code false} jika validasi gagal
	 * @throws Exception jika terjadi error yang tidak tertangani saat penyimpanan
	 */
	public boolean onSave(Event event) throws Exception {
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Mohon maaf, Satuan Kerja belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Satuan Kerja dari field pencarian yang tersedia; (2) Pastikan data Satuan Kerja sudah tercatat di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nama dengan nama kas besar yang sesuai; (2) Pastikan nama tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (akun.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Sumber belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Akun Sumber melalui field pencarian akun yang tersedia; (2) Pastikan akun yang sesuai sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (akunPenerima.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Penerima belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Akun Penerima melalui field pencarian akun yang tersedia; (2) Pastikan akun yang sesuai sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		Session session = HibernateUtil.currentSession();
		if (jenisKasBesar.getId() != null) {
			jenisKasBesar = (JenisKasBesar) session.load(JenisKasBesar.class, jenisKasBesar.getId());
		}
		jenisKasBesar.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		jenisKasBesar.setAkun((Akun) akun.getAttribute("akun"));
		jenisKasBesar.setAkunPenerima((Akun) akunPenerima.getAttribute("akun"));
		jenisKasBesar.setTanggal(tanggal.getValue());
		jenisKasBesar.setKode(kode.getValue());
		jenisKasBesar.setNama(nama.getValue());
		jenisKasBesar.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisKasBesar);
		session.flush();

		JenisKasBesar.reloadDefault();
		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun objek Hibernate {@link Criteria} untuk query data
	 * {@link JenisKasBesar} sesuai filter yang aktif di toolbar pencarian.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membangun criteria dengan kondisi-kondisi berikut:
	 * <ul>
	 *   <li><b>Filter satuan kerja:</b> Jika filter {@code searchparent} terisi,
	 *       membatasi query ke satuan kerja yang dipilih beserta turunannya.
	 *       Kondisi OR memperhitungkan kas besar tanpa satuan kerja ({@code isNull})
	 *       dan kas besar yang masuk dalam set satuan kerja yang valid ({@code in}).
	 *       Jika tidak ada satuan kerja yang terisi dalam set, menggunakan
	 *       {@code sqlRestriction("1=1")} sebagai fallback.</li>
	 *   <li><b>Filter aktif:</b> Jika {@code searchaktif} null atau dicentang,
	 *       hanya menampilkan yang aktif (null atau true). Jika tidak dicentang,
	 *       menampilkan semua.</li>
	 *   <li><b>Filter kode:</b> ILIKE (case-insensitive) pada field {@code kode}
	 *       jika tidak kosong.</li>
	 *   <li><b>Filter nama:</b> ILIKE pada field {@code nama} jika tidak kosong.</li>
	 *   <li><b>Pengurutan:</b> Jika {@code order=true}, ascending berdasarkan
	 *       {@code kode} sehingga daftar terurut secara alfanumerik.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Null-safe menggunakan {@code sqlRestriction("1=1")} atau {@code sqlRestriction("true")}
	 * sebagai fallback. Exception dari Hibernate diteruskan ke pemanggil.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Logika filter satuan kerja identik dengan yang ada di
	 * {@code PertangungjawabanPajakAction}; jika perlu mengubah logika ini,
	 * pertimbangkan untuk membuat helper method bersama. Format kondisi OR bertingkat
	 * perlu diuji dengan cermat untuk memastikan hasilnya sesuai harapan.</p>
	 *
	 * @param order {@code true} untuk menyertakan ORDER BY, {@code false} untuk COUNT
	 * @return objek {@link Criteria} yang telah dikonfigurasi dengan semua filter aktif
	 */
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisKasBesar.class)

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

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
	 * <b>Tujuan:</b> Menjalankan pencarian data jenis kas besar berdasarkan filter
	 * aktif dan memperbarui tampilan grid dengan hasilnya, disertai pembaruan
	 * komponen paging.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Melakukan dua panggilan {@link #initCriteria(boolean)}: pertama dengan
	 * {@code order=false} untuk menghitung total record dan memperbarui paging,
	 * kemudian dengan {@code order=true} untuk mengambil data halaman aktif
	 * menggunakan {@code setMaxResults} dan {@code setFirstResult}. Hasilnya
	 * dibungkus dalam {@code SimpleListModel} dan diset ke grid bersama
	 * {@code JenisKasBesarRenderer} baru.
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari Hibernate diteruskan ke framework ZK. Jika paging null,
	 * data selalu dimulai dari baris pertama.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini dipanggil dari timer default (inisialisasi), listener paging,
	 * listener filter satuan kerja, dan callback setelah operasi CRUD.
	 * Pastikan idempoten tanpa efek samping.</p>
	 *
	 * @param event event ZK pemicu pencarian (bisa null jika dipanggil programatik,
	 *              misalnya dari timer atau setelah simpan)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisKasBesar> jenisKasBesar = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(jenisKasBesar);
		grid.setRowRenderer(new JenisKasBesarRenderer());
		grid.setModelCheckMobile(strset);

	}

}
