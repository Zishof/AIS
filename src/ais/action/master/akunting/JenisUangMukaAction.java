package ais.action.master.akunting;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
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
import org.zkoss.zul.A;
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
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.asset.PermintaanPengadaanMasterAssetAction;
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
import ais.database.model.akunting.JenisUangMuka;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.rab.PenggunaanAnggaran;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>JenisUangMukaAction — Pengelola Data Master Jenis Uang Muka</h3>
 *
 * <p><strong>Untuk apa:</strong> Kelas ini merupakan ZK GenericAutowireComposer yang berfungsi
 * sebagai pengendali (controller) halaman master data Jenis Uang Muka dalam modul akuntansi.
 * Jenis Uang Muka adalah kategori atau tipe uang muka yang berlaku di suatu satuan kerja,
 * misalnya uang muka perjalanan dinas, uang muka pembelian barang, atau uang muka kegiatan.
 * Setiap jenis memiliki konfigurasi akun debet (akun penerima), akun kredit pengembalian
 * (akun kelebihan), dan opsional akun sponsor, yang semuanya terhubung ke Chart of Accounts
 * sistem akuntansi AIS.</p>
 *
 * <p><strong>Cara kerja:</strong> Kelas ini mengimplementasikan pola CRUD standar ZK dengan alur
 * sebagai berikut:</p>
 * <ol>
 *   <li>Saat halaman dimuat, {@link #doBeforeCompose} melakukan pemeriksaan keamanan (security
 *       check) agar hanya pengguna yang berwenang yang dapat mengakses halaman.</li>
 *   <li>{@link #doAfterCompose} menginisialisasi komponen ZK, hak akses pengguna (CREATE,
 *       UPDATE, DELETE), listener paging, dan memuat data awal melalui
 *       {@link #onSearchDefault}.</li>
 *   <li>Pencarian data dikontrol oleh {@link #initCriteria} yang membangun Hibernate Criteria
 *       berdasarkan filter nama, kode, status aktif, dan satuan kerja.</li>
 *   <li>Formulir tambah/ubah dimuat secara dinamis (programatik) di dalam {@link #init(JenisUangMuka)}
 *       menggunakan komponen ZK seperti Borderlayout, MyGrid, AmbilDataAkunBanbox, dan
 *       AmbilDataSatuanKerjaBanbox.</li>
 *   <li>Penyimpanan dilakukan oleh {@link #onSave} yang memvalidasi input, menyimpan entitas
 *       ke Hibernate session, dan memanggil {@code JenisUangMuka.reloadDefault()} untuk
 *       memperbarui cache default.</li>
 * </ol>
 *
 * <p>Selain CRUD master, kelas ini juga menyediakan serangkaian metode statis untuk
 * menghitung dan menampilkan saldo anggaran yang sedang dalam proses (dalam proses penggunaan
 * melalui uang muka, permintaan pengadaan, atau pertanggungjawaban). Metode-metode ini
 * dipanggil dari form uang muka dan form pengadaan aset untuk memberikan informasi saldo
 * workspace secara real-time.</p>
 *
 * <p><strong>Threading:</strong> Kelas ini tidak menggunakan thread latar belakang. Semua operasi
 * database dilakukan pada thread ZK yang sama menggunakan {@code HibernateUtil.currentSession()}
 * (sesi terkelola). Metode statis yang menerima parameter {@code Session} dapat dipanggil dari
 * konteks thread lain selama sesi yang diberikan valid dan terbuka.</p>
 *
 * <p><strong>Pemeliharaan:</strong> Jika terdapat penambahan tipe sumber penggunaan anggaran
 * baru (selain UangMuka, PermintaanPengadaanMasterAssetDetail, dan Pertangungjawaban), perlu
 * dilakukan pembaruan pada metode {@link #resolveKodeTampilan}, {@link #resolveDibuatOleh},
 * {@link #resolveKeterangan}, dan {@link #openSumberAnggaranDalamProses}. Penambahan field
 * baru di {@link JenisUangMuka} juga harus diikuti dengan penambahan baris di metode
 * {@link #init(JenisUangMuka)} (formulir) dan {@link #onSave}. Perhatikan bahwa
 * {@code JenisUangMuka.reloadDefault()} harus tetap dipanggil setiap kali ada perubahan
 * data agar cache in-memory tidak basi.</p>
 *
 * @see JenisUangMuka
 * @see UangMuka
 * @see PenggunaanAnggaran
 * @see Workspace
 */
public class JenisUangMukaAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * Serial version UID untuk serialisasi kelas.
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox serachnama;
	private Textbox serachkode;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox kode;
	private Textbox keterangan;
	private AmbilDataAkunBanbox akun;

	public JenisUangMuka jenisUangMuka;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;

	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private AmbilDataAkunBanbox akunKelebihan;

	private AmbilDataAkunBanbox akunSponsor;

	/**
	 * Dipanggil sebelum komponen ZK dirakit (compose). Metode ini melakukan pemeriksaan
	 * keamanan melalui {@code Common.doCheckSecurity()} untuk memastikan pengguna yang
	 * mengakses halaman telah terautentikasi dengan benar. Jika pemeriksaan gagal, framework
	 * ZK akan menghentikan proses pemuatan halaman.
	 *
	 * <p><strong>Tujuan:</strong> Menjadi gerbang pertama keamanan sebelum halaman ditampilkan,
	 * sehingga pengguna yang tidak memiliki sesi valid tidak dapat mengakses konten halaman
	 * master Jenis Uang Muka.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Metode ini memanggil {@code Common.doCheckSecurity()} yang
	 * akan memeriksa apakah sesi pengguna valid. Jika tidak valid, pengguna akan diarahkan ke
	 * halaman login atau halaman error. Setelah itu, metode parent
	 * {@code super.doBeforeCompose()} dipanggil untuk melanjutkan proses inisialisasi ZK
	 * secara normal.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Error keamanan ditangani oleh
	 * {@code Common.doCheckSecurity()} secara internal. Jika terjadi exception pada
	 * pemanggilan parent, exception tersebut akan diteruskan ke framework ZK.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Metode ini bersifat boilerplate keamanan dan tidak
	 * perlu diubah kecuali ada perubahan pada mekanisme keamanan global aplikasi.</p>
	 *
	 * @param page      halaman ZK yang sedang dimuat
	 * @param parent    komponen induk dalam pohon komponen ZK
	 * @param compInfo  informasi metadata komponen ZK
	 * @return ComponentInfo hasil dari pemanggilan super, digunakan framework ZK
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Dipanggil setelah semua komponen ZK berhasil dirakit dan dihubungkan ke field-field
	 * kelas ini melalui mekanisme autowire ZK. Metode ini melakukan inisialisasi lengkap
	 * halaman master Jenis Uang Muka.
	 *
	 * <p><strong>Tujuan:</strong> Menjadi titik utama inisialisasi halaman, mencakup
	 * pemeriksaan sesi pengguna, inisialisasi bahasa, pengaturan hak akses tombol, pendaftaran
	 * listener paging, dan pemuatan data awal ke grid.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Langkah-langkah inisialisasi dilakukan secara berurutan:
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan proses autowire ZK.</li>
	 *   <li>Memanggil {@code Common.initLaguage()} untuk mengatur bahasa antarmuka.</li>
	 *   <li>Memeriksa atribut sesi {@code usersTemp} dan hak READ; jika tidak ada, pengguna
	 *       diarahkan ke halaman logoff melalui {@code Common.goLogoff()}.</li>
	 *   <li>Mendaftarkan EventListener pada komponen {@code searchparent} (filter satuan kerja)
	 *       agar pencarian otomatis diperbarui saat satuan kerja filter berubah.</li>
	 *   <li>Membuat instance {@link SatuanKerjaTreeModel} untuk navigasi hierarki satuan kerja.</li>
	 *   <li>Mengatur visibilitas dan tooltip tombol Tambah berdasarkan hak CREATE.</li>
	 *   <li>Menyimpan flag {@code edit} dan {@code delete} dari hak UPDATE dan DELETE.</li>
	 *   <li>Memuat data awal dengan {@link #onSearchDefault}.</li>
	 *   <li>Mendaftarkan listener paging agar data diperbarui saat pengguna berpindah halaman.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><strong>Parameter:</strong> Parameter {@code comp} adalah root komponen halaman ZK
	 * yang diteruskan ke {@code super.doAfterCompose()} untuk penyelesaian proses autowire.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Jika sesi pengguna tidak valid, metode akan
	 * memanggil {@code Common.goLogoff()} dan langsung {@code return} untuk menghentikan
	 * inisialisasi lebih lanjut.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada komponen baru yang perlu diinisialisasi
	 * (misalnya filter tambahan), tambahkan inisialisasinya di sini setelah blok pemeriksaan
	 * sesi.</p>
	 *
	 * @param comp komponen root ZK yang telah dirakit
	 * @throws Exception jika terjadi error saat inisialisasi komponen ZK atau akses database
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

		// add.setDisabled((Common.getCurrentUser().getRoot() == null ||
		// !Common.getCurrentUser().getRoot()));

		if (add != null) { add.setTooltiptext("Tambah"); }
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
	 * Menghitung saldo tersedia pada suatu {@link Workspace} anggaran pada titik waktu
	 * tertentu, dengan mengecualikan transaksi-transaksi tertentu dari perhitungan.
	 *
	 * <p><strong>Tujuan:</strong> Metode ini digunakan untuk mengetahui berapa sisa anggaran
	 * yang masih dapat digunakan dari sebuah workspace, dengan mempertimbangkan semua
	 * penggunaan anggaran yang telah terjadi hingga waktu yang ditentukan, kecuali
	 * transaksi-transaksi yang dikecualikan (misalnya transaksi yang sedang dalam proses
	 * simpan/ubah saat ini).</p>
	 *
	 * <p><strong>Cara kerja:</strong> Saldo dihitung dengan formula:
	 * {@code saldoAwal - totalPenggunaan}. Nilai saldo awal diambil dari
	 * {@code workspace.getHargaTotal()}, sedangkan total penggunaan dihitung dari tabel
	 * {@link PenggunaanAnggaran} menggunakan Hibernate Criteria melalui
	 * {@link #buildPenggunaanAnggaranCriteria}. Tanggal batas atas dilebarkan satu menit
	 * menggunakan {@link #plusOneMinute} agar transaksi tepat pada detik yang sama masih
	 * termasuk dalam perhitungan. Jika workspace tidak aktif (null atau aktif=false),
	 * metode langsung mengembalikan 0.0.</p>
	 *
	 * <p><strong>Parameter:</strong></p>
	 * <ul>
	 *   <li>{@code bukanId} — ID UangMuka yang dikecualikan dari perhitungan (null jika
	 *       tidak ada pengecualian uang muka)</li>
	 *   <li>{@code bukanIdPermintaanPengadaanMasterAsset} — ID PermintaanPengadaan yang
	 *       dikecualikan (null jika tidak ada)</li>
	 *   <li>{@code bukanGrupTransaksi} — ID GrupTransaksi yang dikecualikan (null jika
	 *       tidak ada)</li>
	 *   <li>{@code bukanSaldoAwalMasterAssetDetail} — ID SaldoAwalDetail yang dikecualikan
	 *       (null jika tidak ada)</li>
	 *   <li>{@code workspace} — workspace anggaran yang akan dihitung saldonya; jika null
	 *       atau tidak aktif, mengembalikan 0.0</li>
	 *   <li>{@code tanggal} — batas atas waktu perhitungan; jika null, digunakan waktu saat
	 *       ini</li>
	 * </ul>
	 *
	 * <p><strong>Return:</strong> Saldo tersedia (Double) dalam satuan mata uang. Jika terjadi
	 * error, mengembalikan 0.0 dan menampilkan pesan error jika pengguna adalah admin.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception yang terjadi saat query database
	 * ditangkap dan diteruskan ke {@code Common.tampilErrorJikaAdmin(e)} agar hanya terlihat
	 * oleh administrator, kemudian mengembalikan 0.0.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada jenis penggunaan anggaran baru yang perlu
	 * dikecualikan, tambahkan parameter baru dan perbarui {@link #buildPenggunaanAnggaranCriteria}
	 * sesuai kebutuhan.</p>
	 *
	 * @param bukanId                              ID UangMuka yang dikecualikan, atau null
	 * @param bukanIdPermintaanPengadaanMasterAsset ID PermintaanPengadaan yang dikecualikan, atau null
	 * @param bukanGrupTransaksi                   ID GrupTransaksi yang dikecualikan, atau null
	 * @param bukanSaldoAwalMasterAssetDetail      ID SaldoAwalDetail yang dikecualikan, atau null
	 * @param workspace                            workspace anggaran target perhitungan
	 * @param tanggal                              batas atas waktu perhitungan
	 * @return saldo tersedia dalam Double, atau 0.0 jika workspace tidak aktif atau terjadi error
	 */
	public static Double hitungSaldo(Long bukanId, Long bukanIdPermintaanPengadaanMasterAsset, Long bukanGrupTransaksi,
			Long bukanSaldoAwalMasterAssetDetail, Workspace workspace, Date tanggal) {

		if (!isWorkspaceAktif(workspace)) {
			return 0.0D;
		}

		try {
			Session session = HibernateUtil.currentSession();
			Double saldoAwal = nvl(workspace.getHargaTotal());
			Criteria criteria = buildPenggunaanAnggaranCriteria(session, workspace, plusOneMinute(tanggal), bukanId,
					bukanIdPermintaanPengadaanMasterAsset, bukanGrupTransaksi, bukanSaldoAwalMasterAssetDetail);
			Number totalPenggunaan = (Number) criteria.setProjection(Projections.sum("nilai")).uniqueResult();
			return saldoAwal - (totalPenggunaan == null ? 0.0D : totalPenggunaan.doubleValue());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 0.0D;
		}
	}

	/**
	 * Memeriksa apakah suatu {@link Workspace} dalam keadaan aktif dan dapat digunakan
	 * untuk perhitungan anggaran.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan pemeriksaan null-safe untuk status aktif
	 * workspace sebelum dilakukan query database yang mungkin menghasilkan hasil tidak valid
	 * jika workspace tidak aktif.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Mengembalikan {@code true} jika workspace tidak null
	 * DAN (field aktif null ATAU field aktif bernilai true). Konvensi ini mengikuti pola
	 * aplikasi AIS di mana null pada field aktif berarti aktif secara implisit.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Tidak melempar exception; null-safe.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Perubahan pada konvensi field aktif di
	 * {@link Workspace} harus diikuti perubahan pada metode ini.</p>
	 *
	 * @param workspace workspace yang akan diperiksa status aktifnya
	 * @return true jika workspace aktif atau belum diatur (null), false jika tidak aktif atau null
	 */
	private static boolean isWorkspaceAktif(Workspace workspace) {
		return workspace != null && (workspace.getAktif() == null || workspace.getAktif().booleanValue());
	}

	/**
	 * Mengamankan nilai Double dari kemungkinan null dengan mengembalikan 0.0 jika null.
	 *
	 * <p><strong>Tujuan:</strong> Utility null-safe untuk nilai numerik Double agar tidak
	 * terjadi NullPointerException saat operasi aritmetika.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Jika parameter {@code value} adalah null, mengembalikan
	 * 0.0D; jika tidak, mengembalikan nilai aslinya.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Tidak melempar exception; null-safe sepenuhnya.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Metode ini dapat diganti dengan library utility
	 * seperti Apache Commons Lang jika tersedia di classpath, namun sengaja dibuat sederhana
	 * untuk kompatibilitas Java 1.7.</p>
	 *
	 * @param value nilai Double yang mungkin null
	 * @return nilai asli jika tidak null, atau 0.0D jika null
	 */
	private static Double nvl(Double value) {
		return value == null ? 0.0D : value;
	}

	/**
	 * Mengamankan nilai String dari kemungkinan null dengan mengembalikan string kosong
	 * yang sudah di-trim jika null.
	 *
	 * <p><strong>Tujuan:</strong> Utility null-safe untuk nilai String agar tidak terjadi
	 * NullPointerException saat operasi string, sekaligus memastikan tidak ada spasi
	 * berlebihan di awal atau akhir string.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Jika parameter {@code value} adalah null, mengembalikan
	 * string kosong {@code ""}; jika tidak, mengembalikan {@code value.trim()}.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Tidak melempar exception; null-safe sepenuhnya.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Metode private helper ini digunakan secara internal
	 * untuk pemrosesan label dan teks tampilan.</p>
	 *
	 * @param value nilai String yang mungkin null
	 * @return string yang sudah di-trim, atau string kosong jika null
	 */
	private static String safe(String value) {
		return value == null ? "" : value.trim();
	}

	/**
	 * Menambahkan satu menit (tepatnya 59 detik dan 999 milidetik) ke suatu tanggal agar
	 * transaksi yang terjadi tepat pada detik batas atas masih termasuk dalam rentang
	 * perhitungan.
	 *
	 * <p><strong>Tujuan:</strong> Menghindari kasus tepian (edge case) di mana transaksi
	 * yang terjadi pada detik yang sama persis dengan parameter tanggal tidak ikut dihitung
	 * dalam saldo. Dengan menambahkan 59 detik 999 ms, semua transaksi dalam menit yang sama
	 * dapat dimasukkan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Membuat Calendar baru menggunakan
	 * {@code WaktuUtil.getCalendar()} (aman untuk multi-tenant), menyetel waktu ke tanggal
	 * yang diberikan (atau waktu saat ini jika null), kemudian menyetel detik ke 59 dan
	 * milidetik ke 999 tanpa mengubah jam atau menit.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Tidak melempar exception; jika tanggal null,
	 * digunakan {@code WaktuUtil.getDate()} sebagai fallback.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika granularitas waktu perlu diubah (misal ke
	 * satuan jam), ubah konstanta Calendar yang diset di sini.</p>
	 *
	 * @param tanggal tanggal basis perhitungan; jika null, digunakan waktu saat ini
	 * @return tanggal yang sudah disesuaikan dengan detik=59 dan milidetik=999
	 */
	private static Date plusOneMinute(Date tanggal) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}

	/**
	 * Membangun objek Hibernate {@link Criteria} untuk query tabel {@link PenggunaanAnggaran}
	 * dengan berbagai kriteria pengecualian (exclusion) yang fleksibel.
	 *
	 * <p><strong>Tujuan:</strong> Menyatukan logika pembangunan query penggunaan anggaran
	 * dalam satu metode terpusat agar dapat digunakan ulang oleh {@link #hitungSaldo} dan
	 * {@link #ambilData} tanpa duplikasi kode.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Kriteria dasar mencakup filter workspace, filter waktu
	 * ({@code waktu <= batasTanggal}), dan filter aktif (null atau true). Setiap parameter
	 * pengecualian yang tidak null akan menambahkan kriteria LEFT JOIN dan Restriction
	 * {@code isNull OR ne(id)} pada alias yang sesuai, sehingga hanya record yang bukan
	 * berasal dari entitas yang dikecualikan yang akan ikut dalam query.</p>
	 *
	 * <p><strong>Parameter:</strong></p>
	 * <ul>
	 *   <li>{@code session} — Hibernate session yang akan digunakan untuk query</li>
	 *   <li>{@code workspace} — workspace yang menjadi basis filter utama</li>
	 *   <li>{@code batasTanggal} — batas atas waktu transaksi yang dimasukkan</li>
	 *   <li>{@code bukanId} — ID UangMuka yang dikecualikan, null jika tidak ada</li>
	 *   <li>{@code bukanIdPermintaanPengadaanMasterAsset} — ID PR yang dikecualikan</li>
	 *   <li>{@code bukanGrupTransaksi} — ID GrupTransaksi yang dikecualikan</li>
	 *   <li>{@code bukanSaldoAwalMasterAssetDetail} — ID SaldoAwalDetail yang dikecualikan</li>
	 * </ul>
	 *
	 * <p><strong>Return:</strong> Objek {@link Criteria} siap pakai yang dapat langsung
	 * ditambahkan proyeksi atau ordering sesuai kebutuhan pemanggil.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception Hibernate akan diteruskan ke pemanggil.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada jenis sumber penggunaan anggaran baru (misal
	 * dari modul lain), tambahkan blok LEFT JOIN dan pengecualian baru di sini.</p>
	 *
	 * @param session                              Hibernate session aktif
	 * @param workspace                            workspace anggaran target
	 * @param batasTanggal                         batas atas waktu transaksi
	 * @param bukanId                              ID UangMuka yang dikecualikan, atau null
	 * @param bukanIdPermintaanPengadaanMasterAsset ID PR yang dikecualikan, atau null
	 * @param bukanGrupTransaksi                   ID GrupTransaksi yang dikecualikan, atau null
	 * @param bukanSaldoAwalMasterAssetDetail      ID SaldoAwalDetail yang dikecualikan, atau null
	 * @return Criteria Hibernate yang sudah dikonfigurasi
	 */
	private static Criteria buildPenggunaanAnggaranCriteria(Session session, Workspace workspace, Date batasTanggal,
			Long bukanId, Long bukanIdPermintaanPengadaanMasterAsset, Long bukanGrupTransaksi,
			Long bukanSaldoAwalMasterAssetDetail) {
		Criteria criteria = session.createCriteria(PenggunaanAnggaran.class, "pa")
				.add(Restrictions.le("waktu", batasTanggal))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("workspace", workspace));

		// SELALU kecualikan baris penggunaan yang berasal dari UANG MUKA BERDASARKAN PR: anggaran
		// sudah dipotong lewat baris PR-nya, sehingga baris uang muka-nya TIDAK boleh ikut memotong
		// (mencegah pemotongan DUA KALI → saldo anggaran minus). Memakai KOLOM mentah
		// permintaanPengadaanMasterAssets (non-kosong = berbasis PR) — bukan getter yang ter-mask
		// oleh ambilDariPr — agar ampuh untuk SEMUA data termasuk baris lama yang stored aktif=true.
		criteria.createAlias("uangMuka", "um", Criteria.LEFT_JOIN);
		criteria.add(Restrictions.or(Restrictions.isNull("um.id"),
				Restrictions.or(Restrictions.isNull("um.permintaanPengadaanMasterAssets"),
						Restrictions.eq("um.permintaanPengadaanMasterAssets", ""))));

		if (bukanSaldoAwalMasterAssetDetail != null) {
			criteria.createAlias("saldoAwalMasterAssetDetail", "saldoDetail", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.isNull("saldoDetail.id"),
							Restrictions.ne("saldoDetail.id", bukanSaldoAwalMasterAssetDetail)));
		}
		if (bukanIdPermintaanPengadaanMasterAsset != null) {
			criteria.createAlias("permintaanPengadaanMasterAssetDetail", "prDetail", Criteria.LEFT_JOIN)
					.createAlias("prDetail.permintaanPengadaanMasterAsset", "pr", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.isNull("pr.id"),
							Restrictions.ne("pr.id", bukanIdPermintaanPengadaanMasterAsset)));
		}
		if (bukanId != null) {
			// alias "um" sudah dibuat di atas → cukup tambah kondisi pengecualian id.
			criteria.add(Restrictions.or(Restrictions.isNull("um.id"), Restrictions.ne("um.id", bukanId)));
		}
		if (bukanGrupTransaksi != null) {
			criteria.createAlias("grupTransaksi", "gt", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.isNull("gt.id"), Restrictions.ne("gt.id", bukanGrupTransaksi)));
		}
		return criteria;
	}

	/**
	 * Menghitung total penggunaan anggaran yang sedang dalam proses (in-progress) untuk
	 * suatu workspace pada titik waktu tertentu, dengan opsi menyimpan hasilnya ke
	 * field {@code realisasiProses} di entitas workspace.
	 *
	 * <p><strong>Tujuan:</strong> Versi ringan dari {@link #hitungSaldo} yang menggunakan
	 * session yang sudah ada (tidak membuka session baru), sehingga lebih efisien untuk
	 * dipanggil berulang kali dalam satu transaksi. Digunakan untuk memperbarui field
	 * {@code realisasiProses} pada workspace agar laporan keuangan selalu akurat.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Query SUM dari {@link PenggunaanAnggaran} dengan
	 * filter workspace dan waktu. Jika parameter {@code simpan} adalah true dan ada
	 * perbedaan signifikan (lebih dari 0.0001) antara nilai tersimpan dan nilai terhitung,
	 * workspace akan diperbarui menggunakan {@code Common.refreshUpdate()}. Session yang
	 * digunakan adalah session yang diberikan, atau {@code HibernateUtil.currentSession()}
	 * jika parameter session null.</p>
	 *
	 * <p><strong>Parameter:</strong></p>
	 * <ul>
	 *   <li>{@code workspace} — workspace target; jika null atau tidak aktif, return 0.0</li>
	 *   <li>{@code tanggal} — batas atas waktu perhitungan</li>
	 *   <li>{@code simpan} — jika true, workspace akan di-update dengan nilai terhitung</li>
	 *   <li>{@code session} — Hibernate session yang digunakan; null = gunakan currentSession()</li>
	 * </ul>
	 *
	 * <p><strong>Return:</strong> Total penggunaan anggaran dalam proses sebagai Double,
	 * atau 0.0 jika workspace tidak aktif atau terjadi error.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception ditangkap dan diteruskan ke
	 * {@code Common.tampilErrorJikaAdmin(e)}, kemudian mengembalikan 0.0.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Gunakan metode ini (bukan {@link #hitungSaldo})
	 * jika Anda sudah memiliki session aktif untuk menghindari overhead pembukaan session
	 * baru.</p>
	 *
	 * @param workspace workspace anggaran target
	 * @param tanggal   batas atas waktu perhitungan
	 * @param simpan    true jika hasil harus disimpan ke field realisasiProses workspace
	 * @param session   Hibernate session yang digunakan, atau null untuk currentSession()
	 * @return total penggunaan anggaran dalam proses sebagai Double
	 */
	// Metode overload tanpa membuka session baru, sehingga lebih ringan dan aman untuk dipanggil berulang.
	public static Double hitungSaldoDalamProses(Workspace workspace, java.util.Date tanggal, boolean simpan,
			Session session) {

		if (!isWorkspaceAktif(workspace)) {
			return 0.0D;
		}
		if (session == null) {
			session = HibernateUtil.currentSession();
		}

		try {
			Number terpakai = (Number) session.createCriteria(ais.database.model.rab.PenggunaanAnggaran.class)
					.add(Restrictions.eq("workspace", workspace))
					.add(Restrictions.le("waktu", plusOneMinute(tanggal)))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.sum("nilai")).uniqueResult();

			Double proses = terpakai == null ? 0.0D : terpakai.doubleValue();
			if (simpan) {
				Double targetRealisasi = nvl(workspace.getRealisasiProses());
				if (Math.abs(targetRealisasi.doubleValue() - proses.doubleValue()) > 0.0001D) {
					workspace.setRealisasiProses(proses);
					Common.refreshUpdate(session, workspace);
				}
			}
			return proses;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 0.0D;
		}
	}

	/**
	 * Menampilkan daftar penggunaan anggaran yang sedang dalam proses (dalam proses uang muka,
	 * permintaan pengadaan, atau pertanggungjawaban) ke dalam komponen {@link Vbox} yang
	 * diberikan, dikelompokkan berdasarkan kode dokumen sumbernya.
	 *
	 * <p><strong>Tujuan:</strong> Memberikan tampilan ringkas kepada pengguna mengenai
	 * transaksi-transaksi yang sedang menggunakan anggaran workspace tertentu, sehingga
	 * pengguna dapat melihat detail penggunaan dan mengklik link untuk membuka formulir
	 * dokumen sumber terkait.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memanggil {@link #ambilData} untuk mendapatkan daftar
	 * {@link PenggunaanAnggaran}, kemudian mengelompokkannya berdasarkan kode dokumen sumber
	 * (diperoleh dari {@link #resolveKodeTampilan}) menggunakan
	 * {@link AnggaranDalamProsesGroup}. Setiap grup di-render sebagai anchor ZK dengan
	 * label ringkas berisi kode, nilai, jumlah transaksi, pembuat, dan keterangan. Klik
	 * pada anchor akan membuka formulir dokumen sumber terkait melalui
	 * {@link #openSumberAnggaranDalamProses}.</p>
	 *
	 * <p><strong>Parameter:</strong></p>
	 * <ul>
	 *   <li>{@code uangMukaDalamProsesDetail} — Vbox target tempat link-link ditampilkan;
	 *       jika null, metode langsung return tanpa efek</li>
	 *   <li>{@code bukanId} — ID UangMuka yang dikecualikan dari tampilan</li>
	 *   <li>{@code bukanPermintaanPengadaanMasterAssetId} — ID PR yang dikecualikan</li>
	 *   <li>{@code bukanGrupTransaksi} — ID GrupTransaksi yang dikecualikan</li>
	 *   <li>{@code workspace} — workspace anggaran yang datanya ditampilkan</li>
	 *   <li>{@code tanggal} — batas atas waktu data yang ditampilkan</li>
	 * </ul>
	 *
	 * <p><strong>Penanganan error:</strong> Jika workspace tidak aktif, Vbox akan dikosongkan
	 * dan tidak ada konten yang ditampilkan.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika format label perlu diubah (misalnya menambahkan
	 * kolom tanggal), ubah logika pembentukan string {@code label} di dalam
	 * {@link #renderAnggaranDalamProsesGroup}.</p>
	 *
	 * @param uangMukaDalamProsesDetail Vbox ZK tempat link penggunaan anggaran ditampilkan
	 * @param bukanId                   ID UangMuka yang dikecualikan, atau null
	 * @param bukanPermintaanPengadaanMasterAssetId ID PR yang dikecualikan, atau null
	 * @param bukanGrupTransaksi        ID GrupTransaksi yang dikecualikan, atau null
	 * @param workspace                 workspace anggaran target
	 * @param tanggal                   batas atas waktu data
	 */
	public static void tampilkan(Vbox uangMukaDalamProsesDetail, Long bukanId,
			Long bukanPermintaanPengadaanMasterAssetId, Long bukanGrupTransaksi, Workspace workspace, Date tanggal) {
		if (uangMukaDalamProsesDetail == null) {
			return;
		}
		Common.clear(uangMukaDalamProsesDetail);
		List<PenggunaanAnggaran> penggunaanAnggarans = JenisUangMukaAction.ambilData(bukanId,
				bukanPermintaanPengadaanMasterAssetId, bukanGrupTransaksi, workspace, tanggal);

		Map<String, AnggaranDalamProsesGroup> groupMap = new LinkedHashMap<String, AnggaranDalamProsesGroup>();
		for (int i = 0; penggunaanAnggarans != null && i < penggunaanAnggarans.size(); i++) {
			PenggunaanAnggaran penggunaanAnggaran = penggunaanAnggarans.get(i);
			if (penggunaanAnggaran == null) {
				continue;
			}
			String kode = resolveKodeTampilan(penggunaanAnggaran);
			if (kode == null || kode.trim().isEmpty()) {
				kode = "TANPA_KODE_" + (penggunaanAnggaran.getId() == null ? i : penggunaanAnggaran.getId().longValue());
			}
			AnggaranDalamProsesGroup group = groupMap.get(kode);
			if (group == null) {
				group = new AnggaranDalamProsesGroup(kode, penggunaanAnggaran);
				groupMap.put(kode, group);
			}
			group.add(penggunaanAnggaran);
		}

		for (AnggaranDalamProsesGroup group : groupMap.values()) {
			renderAnggaranDalamProsesGroup(uangMukaDalamProsesDetail, group);
		}
	}

	/**
	 * Merender satu grup penggunaan anggaran sebagai anchor (link) ZK ke dalam Vbox induk.
	 *
	 * <p><strong>Tujuan:</strong> Mengubah data grup anggaran dalam proses menjadi elemen
	 * antarmuka yang dapat diklik (anchor) sehingga pengguna dapat langsung membuka
	 * dokumen sumber penggunaan anggaran tersebut.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Membangun label teks dari kode grup, nilai total,
	 * jumlah transaksi (jika lebih dari 1), nama pembuat, dan keterangan. Kemudian membuat
	 * komponen {@link A} ZK dengan gaya font kecil yang saat diklik akan memanggil
	 * {@link #openSumberAnggaranDalamProses} dengan referensi {@code PenggunaanAnggaran}
	 * sumber dari grup.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Jika {@code group} atau {@code parent} null,
	 * atau jika entitas sumber null, metode langsung return tanpa efek.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Gaya CSS anchor dapat disesuaikan pada baris
	 * {@code a.setStyle()} jika desain antarmuka berubah.</p>
	 *
	 * @param parent Vbox induk tempat anchor ditambahkan
	 * @param group  grup penggunaan anggaran yang akan di-render
	 */
	private static void renderAnggaranDalamProsesGroup(Vbox parent, final AnggaranDalamProsesGroup group) {
		if (group == null || parent == null) {
			return;
		}
		final PenggunaanAnggaran sumber = group.sumber;
		if (sumber == null) {
			return;
		}

		String label = group.kode + " | " + Common.numberFormat.get().format(group.nilai);
		if (group.jumlahData > 1) {
			label += " | " + group.jumlahData + " transaksi digabung";
		}
		if (!safe(group.dibuatOleh).isEmpty()) {
			label += " | " + safe(group.dibuatOleh);
		}
		if (!safe(group.keterangan).isEmpty()) {
			label += " | " + safe(group.keterangan);
		}

		A a = new A(label);
		a.setStyle("font-size:10px; display:block; margin:2px 0; line-height:1.35;");
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				openSumberAnggaranDalamProses(sumber);
			}
		});
		parent.appendChild(a);
	}

	/**
	 * Membuka formulir dokumen sumber dari suatu entitas {@link PenggunaanAnggaran} sebagai
	 * popup modal ZK. Jenis dokumen yang dibuka bergantung pada entitas mana yang terhubung:
	 * UangMuka, PermintaanPengadaanMasterAsset, atau Pertangungjawaban.
	 *
	 * <p><strong>Tujuan:</strong> Memudahkan pengguna untuk langsung melihat atau mengubah
	 * dokumen yang menjadi sumber penggunaan anggaran, tanpa harus navigasi ke halaman lain.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memeriksa field relasi pada {@link PenggunaanAnggaran}
	 * secara berurutan: pertama UangMuka, kemudian PermintaanPengadaanMasterAssetDetail (dan
	 * PR induknya), kemudian Pertangungjawaban. Formulir pertama yang ditemukan akan dibuka
	 * menggunakan metode {@code onAddExternal} dari action terkait. EventListener kosong
	 * diberikan sebagai callback karena tidak ada aksi lanjutan yang diperlukan.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Jika {@code penggunaanAnggaran} null atau tidak
	 * ada entitas terhubung yang ditemukan, metode tidak melakukan apa pun.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada jenis sumber baru, tambahkan blok
	 * pengecekan dan pemanggilan action yang sesuai di sini.</p>
	 *
	 * @param penggunaanAnggaran entitas PenggunaanAnggaran yang dokumen sumbernya akan dibuka
	 * @throws Exception jika terjadi error saat membuka formulir popup
	 */
	private static void openSumberAnggaranDalamProses(final PenggunaanAnggaran penggunaanAnggaran) throws Exception {
		if (penggunaanAnggaran == null) {
			return;
		}
		final UangMuka uangMuka = penggunaanAnggaran.getUangMuka();
		if (uangMuka != null) {
			UangMukaAction.onAddExternal(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
				}
			}, uangMuka);
			return;
		}

		PermintaanPengadaanMasterAssetDetail prDetail = penggunaanAnggaran.getPermintaanPengadaanMasterAssetDetail();
		if (prDetail != null && prDetail.getPermintaanPengadaanMasterAsset() != null) {
			final PermintaanPengadaanMasterAsset pr = prDetail.getPermintaanPengadaanMasterAsset();
			PermintaanPengadaanMasterAssetAction.onAddExternal(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
				}
			}, pr);
			return;
		}

		final Pertangungjawaban pertangungjawaban = penggunaanAnggaran.getPertangungjawaban();
		if (pertangungjawaban != null) {
			PertangungjawabanAction.onAddExternal(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
				}
			}, pertangungjawaban);
		}
	}

	/**
	 * Menentukan kode tampilan (display code) dari suatu entitas {@link PenggunaanAnggaran}
	 * dengan mencari kode dari entitas sumber terkait secara berurutan.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan label unik dan bermakna untuk setiap record
	 * penggunaan anggaran dalam tampilan, menggunakan kode dokumen sumber yang paling
	 * relevan untuk pengguna.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memeriksa berurutan: kode dari PermintaanPengadaan,
	 * kode dari Pertangungjawaban, kode langsung dari PenggunaanAnggaran itu sendiri,
	 * kemudian kode dari UangMuka. Mengembalikan string kosong jika semua null.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Null-safe; mengembalikan string kosong jika
	 * input null atau semua relasi null.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Urutan prioritas kode dapat diubah sesuai
	 * kebutuhan bisnis dengan menggeser urutan pengecekan.</p>
	 *
	 * @param penggunaanAnggaran entitas yang kode tampilannya dicari
	 * @return kode tampilan sebagai String, atau string kosong jika tidak ditemukan
	 */
	private static String resolveKodeTampilan(PenggunaanAnggaran penggunaanAnggaran) {
		if (penggunaanAnggaran == null) {
			return "";
		}
		PermintaanPengadaanMasterAssetDetail prDetail = penggunaanAnggaran.getPermintaanPengadaanMasterAssetDetail();
		if (prDetail != null && prDetail.getPermintaanPengadaanMasterAsset() != null
				&& prDetail.getPermintaanPengadaanMasterAsset().getKode() != null) {
			return prDetail.getPermintaanPengadaanMasterAsset().getKode();
		}
		Pertangungjawaban pertangungjawaban = penggunaanAnggaran.getPertangungjawaban();
		if (pertangungjawaban != null && pertangungjawaban.getKode() != null) {
			return pertangungjawaban.getKode();
		}
		if (penggunaanAnggaran.getKode() != null) {
			return penggunaanAnggaran.getKode();
		}
		UangMuka uangMuka = penggunaanAnggaran.getUangMuka();
		if (uangMuka != null && uangMuka.getKode() != null) {
			return uangMuka.getKode();
		}
		return "";
	}

	/**
	 * Menentukan nilai numerik yang relevan dari suatu entitas {@link PenggunaanAnggaran}
	 * untuk keperluan tampilan dan pengelompokan.
	 *
	 * <p><strong>Tujuan:</strong> Mendapatkan nilai yang paling tepat dari record penggunaan
	 * anggaran, dengan logika khusus untuk pertanggungjawaban di mana nilai dikembalikan
	 * adalah negatif dari jumlah yang dikembalikan (karena pertanggungjawaban mengurangi
	 * saldo yang terpakai).</p>
	 *
	 * <p><strong>Cara kerja:</strong> Jika field {@code nilai} pada PenggunaanAnggaran tidak
	 * null, gunakan nilai tersebut. Jika null tetapi ada Pertangungjawaban, gunakan negatif
	 * dari field {@code dikembalikan} Pertangungjawaban. Jika semua null, kembalikan 0.0.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Null-safe; mengembalikan 0.0 jika input null.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Logika negatif untuk pertanggungjawaban mengikuti
	 * konvensi domain akuntansi AIS. Jangan ubah kecuali ada perubahan model domain.</p>
	 *
	 * @param penggunaanAnggaran entitas yang nilainya akan ditentukan
	 * @return nilai numerik sebagai double primitif
	 */
	private static double resolveNilaiTampilan(PenggunaanAnggaran penggunaanAnggaran) {
		if (penggunaanAnggaran == null) {
			return 0.0D;
		}
		if (penggunaanAnggaran.getNilai() != null) {
			return penggunaanAnggaran.getNilai().doubleValue();
		}
		Pertangungjawaban pertangungjawaban = penggunaanAnggaran.getPertangungjawaban();
		if (pertangungjawaban != null) {
			return -nvl(pertangungjawaban.getDikembalikan()).doubleValue();
		}
		return 0.0D;
	}

	/**
	 * Menentukan nama pembuat (creator) dari suatu entitas {@link PenggunaanAnggaran}
	 * dengan menelusuri relasi ke entitas sumber terkait.
	 *
	 * <p><strong>Tujuan:</strong> Menampilkan informasi siapa yang membuat transaksi
	 * penggunaan anggaran untuk keperluan audit dan transparansi dalam tampilan daftar
	 * penggunaan anggaran dalam proses.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memeriksa berurutan: nama pembuat dari UangMuka,
	 * kemudian dari PermintaanPengadaan, kemudian dari Pertangungjawaban. Mengembalikan
	 * string kosong jika semua null atau tidak ada pembuat yang terdefinisi.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Null-safe; mengembalikan string kosong jika
	 * input null atau semua relasi null.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada entitas sumber baru, tambahkan blok
	 * pengecekan di sini.</p>
	 *
	 * @param penggunaanAnggaran entitas yang nama pembuatnya dicari
	 * @return nama pembuat sebagai String, atau string kosong jika tidak ditemukan
	 */
	private static String resolveDibuatOleh(PenggunaanAnggaran penggunaanAnggaran) {
		if (penggunaanAnggaran == null) {
			return "";
		}
		UangMuka uangMuka = penggunaanAnggaran.getUangMuka();
		if (uangMuka != null && uangMuka.getDibuatOleh() != null) {
			return uangMuka.getDibuatOleh().getUserNama();
		}
		PermintaanPengadaanMasterAssetDetail prDetail = penggunaanAnggaran.getPermintaanPengadaanMasterAssetDetail();
		if (prDetail != null && prDetail.getPermintaanPengadaanMasterAsset() != null
				&& prDetail.getPermintaanPengadaanMasterAsset().getDibuatOleh() != null) {
			return prDetail.getPermintaanPengadaanMasterAsset().getDibuatOleh().getUserNama();
		}
		Pertangungjawaban pertangungjawaban = penggunaanAnggaran.getPertangungjawaban();
		if (pertangungjawaban != null && pertangungjawaban.getDibuatOleh() != null) {
			return pertangungjawaban.getDibuatOleh().getUserNama();
		}
		return "";
	}

	/**
	 * Menentukan keterangan dari suatu entitas {@link PenggunaanAnggaran} dengan menelusuri
	 * relasi ke entitas sumber terkait.
	 *
	 * <p><strong>Tujuan:</strong> Menampilkan deskripsi singkat mengenai transaksi
	 * penggunaan anggaran untuk keperluan informasi dalam tampilan daftar penggunaan
	 * anggaran dalam proses.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memeriksa berurutan: keterangan dari UangMuka,
	 * kemudian dari PermintaanPengadaan, kemudian dari Pertangungjawaban, dan terakhir
	 * keterangan langsung dari PenggunaanAnggaran itu sendiri.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Null-safe; tidak melempar exception jika
	 * input null atau relasi null.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Urutan prioritas keterangan dapat disesuaikan
	 * sesuai kebutuhan tampilan.</p>
	 *
	 * @param penggunaanAnggaran entitas yang keterangannya dicari
	 * @return keterangan sebagai String, atau null jika tidak ditemukan
	 */
	private static String resolveKeterangan(PenggunaanAnggaran penggunaanAnggaran) {
		if (penggunaanAnggaran == null) {
			return "";
		}
		UangMuka uangMuka = penggunaanAnggaran.getUangMuka();
		if (uangMuka != null) {
			return uangMuka.getKeterangan();
		}
		PermintaanPengadaanMasterAssetDetail prDetail = penggunaanAnggaran.getPermintaanPengadaanMasterAssetDetail();
		if (prDetail != null && prDetail.getPermintaanPengadaanMasterAsset() != null) {
			return prDetail.getPermintaanPengadaanMasterAsset().getKeterangan();
		}
		Pertangungjawaban pertangungjawaban = penggunaanAnggaran.getPertangungjawaban();
		if (pertangungjawaban != null) {
			return pertangungjawaban.getKeterangan();
		}
		return penggunaanAnggaran.getKeterangan();
	}

	/**
	 * Kelas inner statis untuk mengelompokkan beberapa entitas {@link PenggunaanAnggaran}
	 * yang memiliki kode dokumen sumber yang sama menjadi satu grup tampilan.
	 *
	 * <p><strong>Tujuan:</strong> Menghindari duplikasi baris dalam tampilan ketika satu
	 * dokumen sumber (misalnya satu PR) menghasilkan beberapa record PenggunaanAnggaran
	 * (misalnya untuk beberapa item detail). Dengan pengelompokan ini, pengguna hanya
	 * melihat satu baris per dokumen sumber dengan total nilai yang digabungkan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Setiap instance menyimpan kode grup, entitas sumber
	 * pertama yang ditemukan (untuk keperluan navigasi saat diklik), total nilai kumulatif,
	 * jumlah data yang digabungkan, nama pembuat pertama yang ditemukan, dan keterangan
	 * pertama yang ditemukan. Metode {@link #add} menambahkan satu entitas ke grup dan
	 * memperbarui nilai-nilai tersebut.</p>
	 *
	 * <p><strong>Threading:</strong> Kelas ini tidak thread-safe; hanya digunakan dalam
	 * konteks single-thread ZK event processing.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika perlu menampilkan informasi tambahan per grup
	 * (misalnya tanggal terbaru), tambahkan field baru dan perbarui metode {@link #add}.</p>
	 */
	private static class AnggaranDalamProsesGroup {
		/** Kode unik yang menjadi kunci pengelompokan */
		private String kode;
		/** Entitas PenggunaanAnggaran pertama dalam grup, digunakan untuk navigasi saat diklik */
		private PenggunaanAnggaran sumber;
		/** Total nilai kumulatif dari semua entitas dalam grup */
		private double nilai;
		/** Jumlah entitas yang telah digabungkan dalam grup ini */
		private int jumlahData;
		/** Nama pembuat pertama yang berhasil di-resolve dari entitas dalam grup */
		private String dibuatOleh;
		/** Keterangan pertama yang berhasil di-resolve dari entitas dalam grup */
		private String keterangan;

		/**
		 * Membuat grup baru dengan kode dan entitas sumber awal.
		 *
		 * <p><strong>Tujuan:</strong> Menginisialisasi grup penggunaan anggaran dengan
		 * kode pengelompokan dan entitas pertama sebagai referensi navigasi.</p>
		 *
		 * <p><strong>Cara kerja:</strong> Menyimpan kode dan sumber; nilai, jumlahData,
		 * dibuatOleh, dan keterangan masih kosong/nol, akan diisi melalui {@link #add}.</p>
		 *
		 * <p><strong>Pemeliharaan:</strong> Konstruktor ini hanya digunakan secara internal
		 * oleh metode {@link JenisUangMukaAction#tampilkan}.</p>
		 *
		 * @param kode   kode unik pengelompokan (berasal dari kode dokumen sumber)
		 * @param sumber entitas PenggunaanAnggaran pertama sebagai referensi navigasi
		 */
		private AnggaranDalamProsesGroup(String kode, PenggunaanAnggaran sumber) {
			this.kode = kode;
			this.sumber = sumber;
		}

		/**
		 * Menambahkan satu entitas {@link PenggunaanAnggaran} ke dalam grup ini dan
		 * memperbarui nilai kumulatif serta informasi terkait.
		 *
		 * <p><strong>Tujuan:</strong> Mengakumulasi data dari beberapa entitas yang
		 * memiliki kode dokumen sumber yang sama sehingga dapat ditampilkan sebagai
		 * satu baris ringkasan.</p>
		 *
		 * <p><strong>Cara kerja:</strong> Menambahkan nilai dari entitas ke total grup
		 * menggunakan {@link JenisUangMukaAction#resolveNilaiTampilan}, menaikkan
		 * counter jumlah data, dan mengisi field {@code dibuatOleh} dan
		 * {@code keterangan} hanya jika belum terisi (menggunakan entitas pertama yang
		 * memberikan nilai tidak kosong).</p>
		 *
		 * <p><strong>Penanganan error:</strong> Null-safe; jika entitas null, metode
		 * langsung return.</p>
		 *
		 * <p><strong>Pemeliharaan:</strong> Jika perlu mengakumulasi field tambahan,
		 * tambahkan logika akumulasi di sini.</p>
		 *
		 * @param penggunaanAnggaran entitas yang akan ditambahkan ke grup
		 */
		private void add(PenggunaanAnggaran penggunaanAnggaran) {
			if (penggunaanAnggaran == null) {
				return;
			}
			this.nilai += resolveNilaiTampilan(penggunaanAnggaran);
			this.jumlahData++;
			if (this.sumber == null) {
				this.sumber = penggunaanAnggaran;
			}
			if (safe(this.dibuatOleh).isEmpty()) {
				this.dibuatOleh = resolveDibuatOleh(penggunaanAnggaran);
			}
			if (safe(this.keterangan).isEmpty()) {
				this.keterangan = resolveKeterangan(penggunaanAnggaran);
			}
		}
	}

	/**
	 * Mengambil daftar entitas {@link PenggunaanAnggaran} dari database untuk workspace
	 * tertentu dengan berbagai kriteria pengecualian, diurutkan berdasarkan waktu dan ID.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan data mentah penggunaan anggaran yang sedang
	 * dalam proses, yang kemudian digunakan oleh {@link #tampilkan} untuk ditampilkan
	 * kepada pengguna.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memanggil {@link #buildPenggunaanAnggaranCriteria}
	 * untuk membangun query dengan semua kriteria pengecualian, kemudian menambahkan
	 * pengurutan berdasarkan waktu ascending dan ID ascending, dan mengeksekusi query.
	 * Jika workspace tidak aktif, mengembalikan list kosong tanpa query ke database.</p>
	 *
	 * <p><strong>Parameter:</strong></p>
	 * <ul>
	 *   <li>{@code bukanId} — ID UangMuka yang dikecualikan, null jika tidak ada</li>
	 *   <li>{@code bukanPermintaanPengadaanMasterAssetId} — ID PR yang dikecualikan</li>
	 *   <li>{@code bukanGrupTransaksi} — ID GrupTransaksi yang dikecualikan</li>
	 *   <li>{@code workspace} — workspace anggaran target; null/tidak aktif = list kosong</li>
	 *   <li>{@code tanggal} — batas atas waktu pengambilan data</li>
	 * </ul>
	 *
	 * <p><strong>Return:</strong> List {@link PenggunaanAnggaran} terurut, atau list kosong
	 * jika workspace tidak aktif atau terjadi error.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception ditangkap dan diteruskan ke
	 * {@code Common.tampilErrorJikaAdmin(e)}, kemudian mengembalikan list kosong.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Urutan sorting dapat diubah sesuai kebutuhan
	 * tampilan dengan mengganti atau menambahkan {@code addOrder()} di sini.</p>
	 *
	 * @param bukanId                              ID UangMuka yang dikecualikan, atau null
	 * @param bukanPermintaanPengadaanMasterAssetId ID PR yang dikecualikan, atau null
	 * @param bukanGrupTransaksi                   ID GrupTransaksi yang dikecualikan, atau null
	 * @param workspace                            workspace anggaran target
	 * @param tanggal                              batas atas waktu pengambilan data
	 * @return List PenggunaanAnggaran terurut, atau list kosong jika tidak ada data
	 */
	@SuppressWarnings("unchecked")
	public static List<PenggunaanAnggaran> ambilData(Long bukanId, Long bukanPermintaanPengadaanMasterAssetId,
			Long bukanGrupTransaksi, Workspace workspace, Date tanggal) {

		if (!isWorkspaceAktif(workspace)) {
			return new ArrayList<PenggunaanAnggaran>();
		}

		try {
			Session session = HibernateUtil.currentSession();
			Criteria criteria = buildPenggunaanAnggaranCriteria(session, workspace, plusOneMinute(tanggal), bukanId,
					bukanPermintaanPengadaanMasterAssetId, bukanGrupTransaksi, null);
			return criteria.addOrder(Order.asc("waktu")).addOrder(Order.asc("id")).list();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return new ArrayList<PenggunaanAnggaran>();
		}
	}

	/**
	 * Kelas inner untuk merender setiap baris (row) data {@link JenisUangMuka} pada grid
	 * utama halaman master Jenis Uang Muka.
	 *
	 * <p><strong>Tujuan:</strong> Mengubah setiap entitas JenisUangMuka menjadi baris
	 * tampilan grid ZK dengan kolom-kolom yang relevan: kode revisi, nama, keterangan,
	 * akun penerima, akun kelebihan, akun sponsor, satuan kerja, checkbox aktif, checkbox
	 * default, dan tombol aksi.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Meng-extend {@code MyRowRenderer} dan mengoverride
	 * metode {@code render(Row, Object)}. Setiap kolom diisi menggunakan komponen ZK yang
	 * sesuai. Checkbox Aktif dan Default menggunakan EventListener inline yang langsung
	 * menyimpan perubahan ke database saat diubah, tanpa memerlukan tombol simpan terpisah;
	 * keduanya dinonaktifkan (disabled) dan listener-nya menolak penyimpanan bagi pengguna
	 * tanpa hak UPDATE (flag {@code edit}), konsisten dengan tombol Ubah/Hapus di baris yang sama.
	 * Tombol aksi (salin/ubah/hapus) dibuat melalui {@code Common.copyEditDeleteButtons}.</p>
	 *
	 * <p><strong>Threading:</strong> Renderer dipanggil pada thread ZK event, aman untuk
	 * mengakses {@code HibernateUtil.currentSession()}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada kolom baru di {@link JenisUangMuka},
	 * tambahkan kolom di sini DAN tambahkan deklarasi kolom yang sesuai di file ZUL
	 * yang menggunakan grid ini.</p>
	 */
	class JenisUangMukaRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data {@link JenisUangMuka} ke dalam komponen {@link Row} ZK.
		 *
		 * <p><strong>Tujuan:</strong> Mengisi baris grid dengan semua kolom data jenis uang
		 * muka termasuk kode, nama, keterangan, akun-akun terkait, satuan kerja, flag aktif
		 * dan default yang bisa langsung diubah, serta tombol aksi.</p>
		 *
		 * <p><strong>Cara kerja:</strong> Cast parameter {@code arg1} ke {@link JenisUangMuka},
		 * kemudian secara berurutan menambahkan komponen ZK ke Row. Checkbox Aktif dan Default
		 * masing-masing dinonaktifkan (disabled) berdasarkan flag {@code edit}, dan memiliki
		 * EventListener {@code onCheck} yang menolak perubahan jika {@code edit} bernilai false
		 * (lapis pertahanan kedua), atau memanggil {@code Common.refreshSaveOrUpdate} secara
		 * langsung jika pengguna berhak. Tombol aksi dibuat dengan
		 * {@code Common.copyEditDeleteButtons} menggunakan flag {@code edit} dan {@code delete}
		 * dari outer class.</p>
		 *
		 * <p><strong>Penanganan error:</strong> Exception dari EventListener akan diteruskan
		 * ke framework ZK untuk penanganan standar.</p>
		 *
		 * <p><strong>Pemeliharaan:</strong> Urutan penambahan komponen ke Row harus sesuai
		 * dengan urutan deklarasi kolom di file ZUL agar tidak terjadi pergeseran kolom.</p>
		 *
		 * @param arg0 komponen Row ZK yang akan diisi
		 * @param arg1 objek data yang harus berupa instansi JenisUangMuka
		 * @throws Exception jika terjadi error saat render komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisUangMuka jenisUangMuka = (JenisUangMuka) arg1;

			RevisiHelper
					.createNewRevisi(JenisUangMuka.class, jenisUangMuka,
							jenisUangMuka.getKode() == null ? "" : jenisUangMuka.getKode().trim().toString())
					.setParent(arg0);
			new Label(jenisUangMuka.getNama()).setParent(arg0);
			new Label(jenisUangMuka.getKeterangan()).setParent(arg0);

			new Label(jenisUangMuka.getAkun() == null ? ""
					: jenisUangMuka.getAkun().getKode() + "-" + jenisUangMuka.getAkun().getNama()).setParent(arg0);

			new Label(jenisUangMuka.getAkunKelebihan() == null ? ""
					: jenisUangMuka.getAkunKelebihan().getKode() + "-" + jenisUangMuka.getAkunKelebihan().getNama())
					.setParent(arg0);

			new Label(jenisUangMuka.getAkunSponsor() == null ? ""
					: jenisUangMuka.getAkunSponsor().getKode() + "-" + jenisUangMuka.getAkunSponsor().getNama())
					.setParent(arg0);

			new Label(jenisUangMuka.getSatuanKerja() == null ? ""
					: jenisUangMuka.getSatuanKerja().getKode() + "-" + jenisUangMuka.getSatuanKerja().getNama())
					.setParent(arg0);

			final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
			aktif.setDisabled(!edit);
			aktif.setChecked(jenisUangMuka.getAktif());
			aktif.setParent(arg0);
			aktif.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (!edit) {
						return;
					}
					jenisUangMuka.setAktif(aktif.isChecked());
					Common.refreshSaveOrUpdate(jenisUangMuka);
				}
			});

			final MyCheckboxConfig defaultData = new MyCheckboxConfig("Default");
			defaultData.setDisabled(!edit);
			defaultData.setChecked(jenisUangMuka.getDefaultData());
			defaultData.setParent(arg0);
			defaultData.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (!edit) {
						return;
					}
					jenisUangMuka.setDefaultData(defaultData.isChecked());
					Common.refreshSaveOrUpdate(jenisUangMuka);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisUangMuka, JenisUangMukaAction.this).setParent(arg0);
		}

	}

	/**
	 * Implementasi dari antarmuka {@link DataInitDefault} untuk menginisialisasi formulir
	 * tambah/ubah dengan data objek yang sudah ada.
	 *
	 * <p><strong>Tujuan:</strong> Dipanggil oleh framework ketika pengguna mengklik tombol
	 * Ubah (Edit) pada baris grid, untuk membuka popup formulir yang sudah terisi dengan
	 * data entitas yang dipilih.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Melakukan casting {@code obj} ke {@link JenisUangMuka},
	 * menyimpannya ke field instance {@code jenisUangMuka}, kemudian memanggil
	 * {@link #init(JenisUangMuka)} untuk membangun dan menampilkan formulir, dan terakhir
	 * menampilkan popup {@code addWindow} sebagai modal dialog.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception diteruskan ke framework ZK.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Metode ini adalah bagian dari kontrak antarmuka
	 * DataInitDefault dan tidak boleh diubah signaturnya.</p>
	 *
	 * @param obj objek GeneralValueObject yang harus berupa instansi JenisUangMuka
	 * @throws Exception jika terjadi error saat membangun formulir atau menampilkan modal
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisUangMuka = (JenisUangMuka) obj;
		init(jenisUangMuka);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Menangani event klik tombol Tambah untuk membuka formulir tambah Jenis Uang Muka baru.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan titik masuk untuk penambahan data baru dengan
	 * membuka popup formulir yang kosong dan siap diisi oleh pengguna.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Membuat instance {@link JenisUangMuka} baru (tanpa ID),
	 * memanggil {@link #init(JenisUangMuka)} untuk membangun formulir kosong, kemudian
	 * menampilkan popup sebagai modal dialog. Exception dari {@code addWindow.onModal()}
	 * ditangkap dan diteruskan ke {@code Common.tampilErrorJikaAdmin}.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dari {@code onModal()} ditangkap
	 * dan ditampilkan ke admin melalui {@code Common.tampilErrorJikaAdmin(e)}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Nama metode {@code onAdd} mengikuti konvensi ZK
	 * event handler (button dengan id "add" akan otomatis memanggil metode ini).</p>
	 *
	 * @param event event ZK dari klik tombol Tambah
	 * @throws Exception jika terjadi error saat membangun formulir
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisUangMuka());
		addWindow.setVisible(true);
		try {
			addWindow.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun dan mengisi formulir tambah/ubah Jenis Uang Muka secara programatik
	 * ke dalam popup {@code addWindow}.
	 *
	 * <p><strong>Tujuan:</strong> Menyiapkan semua komponen formulir (field input,
	 * banbox akun, banbox satuan kerja, tombol simpan/batal) dan mengisinya dengan
	 * data dari entitas {@link JenisUangMuka} yang diberikan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Membersihkan konten addWindow dengan
	 * {@code Common.clear(addWindow)}, kemudian membangun layout Borderlayout dengan
	 * Center (konten formulir) dan South (toolbar tombol). Di area Center, dibuat MyGrid
	 * dengan baris-baris formulir menggunakan MyFormRow. Setiap field memiliki label
	 * konfigurasi (MyLabelConfig) dan komponen input yang sesuai. Untuk satuan kerja,
	 * diambil dari PerguruanTinggi aktif dan diperluas ke seluruh hierarki anak menggunakan
	 * SatuanKerjaTreeModel. Tombol Batal menutup addWindow; tombol Simpan memanggil
	 * {@link #onSave} dan menutup popup jika berhasil.</p>
	 *
	 * <p><strong>Parameter:</strong> {@code jenisUangMuka} — entitas yang akan ditampilkan
	 * di formulir. Jika ID null, formulir dalam mode Tambah; jika ID tidak null, mode Ubah.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dari inisialisasi satuan kerja default
	 * ditangkap secara diam-diam untuk menghindari gangguan pada pengguna.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada field baru di JenisUangMuka, tambahkan
	 * MyFormRow baru di sini dengan label dan komponen input yang sesuai, kemudian
	 * tambahkan logika pengambilan/penyimpanan di {@link #onSave}.</p>
	 *
	 * @param jenisUangMuka entitas JenisUangMuka yang akan ditampilkan/diubah di formulir
	 * @throws Exception jika terjadi error saat membangun komponen ZK
	 */
	private void init(JenisUangMuka jenisUangMuka) throws Exception {
		addWindow.setTitle(jenisUangMuka.getId() == null ? "Tambah Jenis Uang Muka" : "Ubah Jenis Uang Muka");
		this.jenisUangMuka = jenisUangMuka;
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
			if (jenisUangMuka.getSatuanKerja() == null) {
				jenisUangMuka.setSatuanKerja(Common.getSatuanKerja());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/JenisUangMukaAction.java:1325");
			// Tidak ada satuan kerja default, diabaikan
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja *"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(jenisUangMuka.getSatuanKerja() == null ? "" : jenisUangMuka.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", jenisUangMuka.getSatuanKerja());

		row.appendChild(satuanKerja);

		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox((jenisUangMuka.getKode() == null ? "" : jenisUangMuka.getKode().trim())));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(jenisUangMuka.getNama() == null ? "" : jenisUangMuka.getNama().trim()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Penerima Uang Muka *"));
		row.appendChild(akun = new AmbilDataAkunBanbox(false));
		akun.setValue(jenisUangMuka.getAkun() == null ? "" : jenisUangMuka.getAkun().getNama());
		akun.setAttribute("akun", jenisUangMuka.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Pengembalian Uang Muka *"));
		row.appendChild(akunKelebihan = new AmbilDataAkunBanbox(false));
		akunKelebihan
				.setValue(jenisUangMuka.getAkunKelebihan() == null ? "" : jenisUangMuka.getAkunKelebihan().toString());
		akunKelebihan.setAttribute("akun", jenisUangMuka.getAkunKelebihan());
		akunKelebihan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Sponsor Uang Muka"));
		row.appendChild(akunSponsor = new AmbilDataAkunBanbox(false));
		akunSponsor.setValue(jenisUangMuka.getAkunSponsor() == null ? "" : jenisUangMuka.getAkunSponsor().toString());
		akunSponsor.setAttribute("akun", jenisUangMuka.getAkunSponsor());
		akunSponsor.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(jenisUangMuka.getKeterangan() == null ? "" : jenisUangMuka.getKeterangan()));
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
	 * Memvalidasi dan menyimpan data formulir Jenis Uang Muka ke database, kemudian
	 * memperbarui cache default jenis uang muka.
	 *
	 * <p><strong>Tujuan:</strong> Melakukan validasi input wajib dan menyimpan entitas
	 * {@link JenisUangMuka} baru atau yang diubah ke database menggunakan Hibernate,
	 * serta memastikan cache in-memory JenisUangMuka diperbarui setelah penyimpanan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Tahap validasi dilakukan terlebih dahulu:
	 * satuan kerja, nama, akun penerima, dan akun pengembalian wajib diisi. Jika ada
	 * yang kosong, ditampilkan pesan peringatan via {@code MyMessageboxConfig} dan metode
	 * mengembalikan {@code false}. Jika validasi lulus, entitas JenisUangMuka di-reload
	 * dari session (untuk data yang sudah ada) atau menggunakan instance baru, kemudian
	 * semua field diset dari nilai komponen formulir. Penyimpanan menggunakan
	 * {@code Common.refreshSaveOrUpdate(session, jenisUangMuka)} diikuti
	 * {@code session.flush()} dan {@code JenisUangMuka.reloadDefault()} untuk
	 * memperbarui cache.</p>
	 *
	 * <p><strong>Return:</strong> {@code true} jika penyimpanan berhasil, {@code false}
	 * jika validasi gagal.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception database dari Hibernate diteruskan
	 * ke framework ZK melalui signature {@code throws Exception}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada field wajib baru, tambahkan validasi
	 * sebelum blok penyimpanan. Jika ada field opsional baru, tambahkan setter setelah
	 * validasi. Pastikan {@code JenisUangMuka.reloadDefault()} selalu dipanggil di akhir.</p>
	 *
	 * @param event event ZK dari klik tombol Simpan
	 * @return true jika berhasil disimpan, false jika validasi gagal
	 * @throws Exception jika terjadi error database saat penyimpanan
	 */
	public boolean onSave(Event event) throws Exception {
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Mohon maaf, Satuan Kerja belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Satuan Kerja dari field pencarian yang tersedia; (2) Pastikan data Satuan Kerja sudah tercatat di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nama dengan nama jenis uang muka yang sesuai; (2) Pastikan nama tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (akun.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Penerima Uang Muka belum dipilih. Langkah yang dapat dilakukan: (1) Pilih akun penerima melalui field pencarian akun; (2) Pastikan akun yang sesuai sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (akunKelebihan.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun Pengembalian Uang Muka belum dipilih. Langkah yang dapat dilakukan: (1) Pilih akun pengembalian melalui field pencarian akun; (2) Pastikan akun yang sesuai sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisUangMuka.getId() != null) {
			jenisUangMuka = (JenisUangMuka) session.load(JenisUangMuka.class, jenisUangMuka.getId());
		}
		jenisUangMuka.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		jenisUangMuka.setAkun((Akun) akun.getAttribute("akun"));
		jenisUangMuka.setAkunKelebihan((Akun) akunKelebihan.getAttribute("akun"));
		jenisUangMuka.setAkunSponsor((Akun) akunSponsor.getAttribute("akun"));
		jenisUangMuka.setKode(kode.getValue());
		jenisUangMuka.setNama(nama.getValue());
		jenisUangMuka.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisUangMuka);
		session.flush();
		JenisUangMuka.reloadDefault();
		return true;
	}

	/**
	 * Membangun objek Hibernate {@link Criteria} untuk query data {@link JenisUangMuka}
	 * berdasarkan filter yang aktif di formulir pencarian.
	 *
	 * <p><strong>Tujuan:</strong> Menyatukan logika pembangunan query pencarian ke dalam
	 * satu metode yang dapat digunakan baik untuk perhitungan total paging maupun untuk
	 * pengambilan data halaman tertentu.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Membangun Criteria Hibernate dengan filter berantai:
	 * <ol>
	 *   <li>Filter satuan kerja: jika ada parent dipilih di filter, hanya ambil JenisUangMuka
	 *       yang satuan kerjanya berada dalam hierarki parent tersebut. Jika tidak ada parent,
	 *       ambil semua termasuk yang satuan kerjanya null.</li>
	 *   <li>Filter aktif: jika checkbox aktif dicentang, hanya tampilkan yang aktif (null
	 *       atau true). Jika tidak dicentang, tampilkan semua.</li>
	 *   <li>Filter kode: pencarian case-insensitive dengan ANYWHERE match.</li>
	 *   <li>Filter nama: pencarian case-insensitive dengan ANYWHERE match.</li>
	 * </ol>
	 * Jika parameter {@code order} adalah true, menambahkan pengurutan berdasarkan kode
	 * ascending.</p>
	 *
	 * <p><strong>Parameter:</strong> {@code order} — true jika hasil harus diurutkan
	 * berdasarkan kode (untuk tampilan), false jika tidak (untuk count).</p>
	 *
	 * <p><strong>Return:</strong> Objek {@link Criteria} siap pakai.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada filter baru (misalnya filter berdasarkan
	 * akun), tambahkan blok Restriction baru di akhir chain {@code .add()}.</p>
	 *
	 * @param order true untuk menambahkan pengurutan berdasarkan kode asc
	 * @return Criteria Hibernate yang sudah dikonfigurasi sesuai filter aktif
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
		Criteria criteria = session.createCriteria(JenisUangMuka.class)

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
	 * Menangani event pencarian default dengan memuat ulang data grid sesuai filter yang aktif
	 * dan halaman paging yang dipilih.
	 *
	 * <p><strong>Tujuan:</strong> Menjadi handler utama untuk memuat atau memperbarui
	 * tampilan daftar Jenis Uang Muka, baik dipanggil saat inisialisasi halaman, perubahan
	 * filter, pergantian halaman paging, maupun setelah operasi simpan/hapus.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Pertama, menghitung total data untuk paging menggunakan
	 * {@code Common.initPaging(initCriteria(false), paging)}. Kemudian mengambil data halaman
	 * aktif dengan batasan {@code Common.ROWS_COUNT_ON_PAGE} dan offset berdasarkan halaman
	 * aktif. Hasil query dibungkus dalam {@code SimpleListModel} dan diset ke grid beserta
	 * renderer {@link JenisUangMukaRenderer} melalui {@code grid.setModelCheckMobile}.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception Hibernate diteruskan ke framework ZK.
	 * Jika paging null, digunakan halaman pertama (offset 0).</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Nama metode {@code onSearchDefault} mengikuti
	 * konvensi ZK dan dipanggil otomatis oleh komponen yang memiliki event sesuai.
	 * Perubahan pada jumlah item per halaman dilakukan di {@code Common.ROWS_COUNT_ON_PAGE}.</p>
	 *
	 * @param event event ZK yang memicu pencarian (dapat null untuk panggilan programatik)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisUangMuka> jenisUangMuka = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(jenisUangMuka);
		grid.setRowRenderer(new JenisUangMukaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
