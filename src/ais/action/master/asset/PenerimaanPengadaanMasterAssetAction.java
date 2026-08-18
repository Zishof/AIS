package ais.action.master.asset;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataUangMukaBanbox;
import ais.action.master.asset.helper.AmbilDataPemesananPengadaanAsetBanbox;
import ais.action.master.asset.helper.BarangDalamProsesDashboard;
import ais.action.master.asset.helper.InventarisDashboard;
import ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox;
import ais.action.master.asset.helper.PenerimaanPengadaanMasterAssetDetailAction;
import ais.action.master.asset.helper.PenerimaanPengadaanMasterAssetHelper;
import ais.action.master.asset.helper.RevisiPenerimaanPengadaanMasterAssetHelper;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.sop.helper.SopUtil;
import ais.action.master.surat.util.SuratUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.DetailTransaksiAsset;
import ais.database.model.asset.DokumenPenyediaAsset;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.JenisPenerimaanBarang;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.asset.PembayaranPengadaanMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
import ais.database.model.asset.PemilikAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.PenyediaAssetPunyaDokumen;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h3>PenerimaanPengadaanMasterAssetAction — Controller ZKoss untuk Penerimaan Barang/Jasa (BAST)</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah controller utama modul Penerimaan Pengadaan Barang dan Jasa dalam sistem manajemen aset.
 * Ia mengelola seluruh siklus hidup dokumen BAST (Berita Acara Serah Terima), mulai dari pencatatan
 * penerimaan barang dari vendor, verifikasi kuantitas yang diterima, pengelolaan informasi termin
 * pembayaran bertahap, hingga alur persetujuan dan pembatalan dokumen. Controller ini juga mendukung
 * sinkronisasi stok ke tabel {@code detail_transaksi_asset} setelah persetujuan diberikan, sehingga
 * saldo aset perusahaan selalu terbarui secara akurat dan konsisten.
 *
 * <b>Cara kerja:</b><br>
 * Controller mewarisi {@link GenericAutowireComposer} dari ZKoss sehingga komponen UI yang dideklarasikan
 * di file ZUL di-wire otomatis ke field-field pada kelas ini. Siklus hidup dimulai di
 * {@link #doBeforeCompose} untuk pemeriksaan keamanan, dilanjutkan di {@link #doAfterCompose} untuk
 * inisialisasi filter, paging, timer, dan tombol-tombol aksi. Daftar data ditampilkan melalui
 * {@link PenerimaanPengadaanMasterAssetRenderer} yang merender setiap baris grid beserta tombol Cetak,
 * Setujui, Batalkan, Ubah, dan Hapus. Formulir tambah/ubah dibuat secara programatik di metode
 * {@link #form} dan {@link #init}, sementara penyimpanan data dikerjakan oleh {@link #onSave}.
 * Kelas ini juga mengimplementasikan antarmuka {@link FormSop} agar dapat diintegrasikan ke dalam
 * alur persetujuan SOP (Standar Operasional Prosedur) yang ada pada sistem.
 *
 * <b>Threading:</b><br>
 * Semua operasi UI berjalan di thread ZKoss event-dispatch. Operasi database menggunakan sesi Hibernate
 * melalui {@link ais.database.hibernate.HibernateUtil#currentSession()} yang di-bind ke thread request.
 * Sinkronisasi stok dieksekusi melalui {@link Common#createDefaultTimer} agar tidak memblokir event
 * thread utama. Tidak ada akses konkuren langsung ke field instance karena setiap HTTP request ZKoss
 * mendapatkan instance composer sendiri.
 *
 * <b>Pemeliharaan:</b><br>
 * Ketika menambahkan kolom baru pada tabel {@code penerimaan_pengadaan_master_asset}, tambahkan pula
 * DDL ALTER TABLE di skema {@code public} dan {@code new_audit} (Envers). Kolom yang berhubungan dengan
 * persetujuan harus juga menangani penghapusan atau penambahan entri di {@code detail_transaksi_asset}
 * agar saldo stok tetap konsisten. Pastikan kode unik dihasilkan melalui
 * {@link ais.action.master.KodeUnikUtil#pastikanUnik} untuk menghindari duplikasi pasca penghapusan data.
 *
 * @author Tim Pengembang AIS
 * @version 1.0
 * @see PenerimaanPengadaanMasterAsset
 * @see PenerimaanPengadaanMasterAssetHelper
 * @see FormSop
 */
public class PenerimaanPengadaanMasterAssetAction extends GenericAutowireComposer implements FormSop {

	/**
	 * Serial version UID untuk kompatibilitas serialisasi kelas.
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchkode;
	private Textbox searchkodepo;
	private Textbox searchketerangan;
	private Textbox searchketerangantermin;
	private Combobox searchJenisPenerimaanBarang;
	private AmbilDataPenyediaAssetBanbox searchPenyedia;
	private Combobox searchlokasi;
	private MyDatebox start;
	private MyDatebox end;

	private boolean tampilkanRuanganDamPemilikAset = Common.bolehKonfigurasi("tampilkanRuanganDamPemilikAset", Konfigurasi.TIDAK_AKTIF);

	private Label kode;
	private MyTextbox keterangan;
	private MyDatebox tanggalPembuatan;
	private AmbilDataPemesananPengadaanAsetBanbox pemesananPengadaanMasterAsset;
	private Label penyedia;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset;
	private MyToolbarbuttonConfig add;
	private Combobox pemilikAsset;
	private Combobox lokasi;
	private AmbilDataRuangBanbox ruang;
	private MyGrid gridMasterAsset;
	private PenerimaanPengadaanMasterAssetHelper penerimaanPengadaanMasterAssetHelper;
	private DisposisiSop disposisiSop = null;
	private MyCheckboxConfig tampaPemesanan;
	private boolean persetujuan;
	private Tbmuser tbmuser;
	private MyCheckboxConfig setujui;
	private Combobox jenisPenerimaanBarang;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private MyTextbox kurir;

	private MyCheckboxConfig blmDisetujui;
	private MyCheckboxConfig disetujui;
	private AmbilDataWorkspaceBanbox workspace;
	private MyCheckboxConfig tanpaAnggaran;
	private Row rowTermin;
	private Combobox kodeTermin;
	private Row rowTerminProgres;
	private Label progresTermin;
	private Row rowTerminDokumen;
	private Vbox dokumenTermin;

	private Tabpanel tabInventaris;
	/** Tabpanel dashboard "Barang Dalam Proses" (CIP) — rekap + grafik. */
	private Tabpanel barangDalamProses;

	/**
	 * Menangani event klik tab "Inventaris" untuk memuat halaman monitor barang tidak habis pakai.
	 *
	 * <b>Tujuan:</b><br>
	 * Metode ini memuat konten tab Inventaris secara malas (lazy loading) — halaman hanya di-include
	 * ke dalam tab ketika pengguna pertama kali mengkliknya. Ini menghindari beban awal yang berat
	 * saat halaman utama dibuka.
	 *
	 * <b>Cara kerja:</b><br>
	 * Metode memeriksa apakah tab panel sudah memiliki anak komponen. Jika belum, sebuah {@link MyWindow}
	 * dibuat sebagai kontainer penuh ({@code height: 100%, width: 100%}), kemudian {@link MyInclude}
	 * digunakan untuk menyertakan file ZUL monitor inventaris barang tidak habis pakai. Setelah pemuatan
	 * pertama, pemeriksaan {@code size() == 0} memastikan tidak ada pemuatan ganda.
	 *
	 * <b>Parameter:</b><br>
	 * @param event event ZKoss yang dipicu saat tab diklik; tidak digunakan secara langsung di dalam metode.
	 *
	 * <b>Penanganan error:</b><br>
	 * ZKoss akan melempar exception jika file ZUL yang ditentukan tidak ditemukan. Tidak ada blok
	 * try-catch di sini sehingga exception akan merambat ke atas dan ditangani oleh framework.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika path file ZUL monitor inventaris berubah, perbarui string path di dalam {@link MyInclude}.
	 * Pastikan file ZUL baru mendukung embedding (tidak memerlukan parameter sesi khusus).
	 */
	public void onInventaris(Event event) {
		if (tabInventaris.getChildren().size() == 0) {
			// Enhance: tampilan dashboard eye-catching (rekap+grafik) gantikan grid monitor polos.
			InventarisDashboard dashboard = new InventarisDashboard();
			dashboard.setHeight("100%");
			dashboard.setWidth("100%");
			dashboard.setParent(tabInventaris);
		}
	}

	/**
	 * Menangani klik tab "Barang Dalam Proses" dengan memuat dashboard rekap+grafik CIP
	 * ({@link BarangDalamProsesDashboard}) secara lazy (sekali saja).
	 */
	public void onBarangDalamProses(Event event) {
		if (barangDalamProses == null) {
			return;
		}
		if (barangDalamProses.getChildren().size() == 0) {
			BarangDalamProsesDashboard dashboard = new BarangDalamProsesDashboard();
			dashboard.setHeight("100%");
			dashboard.setWidth("100%");
			dashboard.setParent(barangDalamProses);
		}
	}

	private Tabpanel tabStokPersediaan;

	/**
	 * Menangani event klik tab "Stok Persediaan" untuk memuat halaman monitor stok aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Memuat konten tab Stok Persediaan secara malas agar performa halaman utama tetap baik.
	 * Serupa dengan {@link #onInventaris}, tab ini hanya dimuat saat pertama kali diklik oleh pengguna.
	 *
	 * <b>Cara kerja:</b><br>
	 * Metode mengecek apakah tab panel {@code tabStokPersediaan} sudah berisi komponen anak. Jika belum,
	 * sebuah {@link MyWindow} transparan dibuat dengan ukuran 100% dan di dalamnya di-include file ZUL
	 * untuk monitor stok aset. Visibilitas tab ini sendiri dikendalikan oleh konfigurasi sistem
	 * {@code tampilkan_stok_persediakan} yang diperiksa di {@link #doAfterCompose}.
	 *
	 * <b>Parameter:</b><br>
	 * @param event event ZKoss yang dipicu saat tab diklik; tidak digunakan secara langsung.
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari inisialisasi komponen ZKoss akan merambat ke framework dan ditampilkan sebagai
	 * pesan error standar. Tidak ada penanganan khusus di metode ini.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Konfigurasi {@code tampilkan_stok_persediakan} harus terdaftar di tabel konfigurasi sistem.
	 * Jika fitur stok persediaan dihapus, hapus juga field {@code tabStokPersediaan} beserta
	 * referensinya di file ZUL terkait.
	 */
	public void onPersediaan(Event event) {
		if (tabStokPersediaan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabStokPersediaan);
			MyInclude iframe = new MyInclude("/pages/master/asset/monitor_stok_asset.zul");
			iframe.setParent(window);
		}
	}

	/**
	 * Konstruktor default — membuat instance controller dan mengambil pengguna yang sedang login.
	 *
	 * <b>Tujuan:</b><br>
	 * Inisialisasi controller untuk penggunaan normal melalui ZKoss compose lifecycle, di mana
	 * ZKoss secara otomatis membuat instance ini ketika halaman ZUL yang terkait di-load.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@link Common#getCurrentUser()} untuk mendapatkan objek {@link Tbmuser} dari sesi
	 * pengguna yang sedang aktif dan menyimpannya ke field {@code tbmuser}. Field ini digunakan
	 * kemudian di {@link #onSave} untuk mencatat siapa yang membuat atau menyetujui dokumen.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Tidak perlu diubah kecuali terjadi perubahan cara pengambilan pengguna aktif dari sesi.
	 */
	public PenerimaanPengadaanMasterAssetAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Konstruktor dengan parameter persetujuan — membuat controller dalam mode persetujuan SOP.
	 *
	 * <b>Tujuan:</b><br>
	 * Digunakan ketika controller ini diinstansiasi secara programatik dari alur SOP (misalnya oleh
	 * {@link #onAddExternal}) untuk menampilkan formulir penerimaan dalam mode baca/persetujuan,
	 * bukan mode tambah/ubah biasa.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menyimpan nilai {@code persetujuan} ke field instance dan mengambil pengguna aktif dari sesi.
	 * Ketika {@code persetujuan = true}, metode {@link #form} akan menampilkan komponen sebagai Label
	 * (hanya baca) alih-alih input yang dapat diedit. Beberapa field formulir seperti Pemesanan PO
	 * dan Kode Penerimaan akan dirender sebagai teks statis.
	 *
	 * <b>Parameter:</b><br>
	 * @param persetujuan {@code true} jika controller digunakan dalam konteks persetujuan SOP
	 *                    (mode tampilan/readonly), {@code false} untuk mode tambah/ubah normal.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika ada tambahan mode tampilan lain (misalnya mode hanya-cetak), pertimbangkan untuk
	 * mengganti flag boolean ini dengan enum untuk kejelasan kode.
	 */
	public PenerimaanPengadaanMasterAssetAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Hook siklus hidup ZKoss yang dipanggil sebelum proses compose komponen UI dimulai.
	 *
	 * <b>Tujuan:</b><br>
	 * Melakukan pemeriksaan keamanan akses sebelum halaman dirender sama sekali. Ini adalah titik
	 * paling awal dalam siklus hidup ZKoss di mana akses dapat dicegah tanpa menampilkan konten
	 * halaman kepada pengguna yang tidak berhak.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@link Common#doCheckSecurity()} yang memeriksa apakah pengguna memiliki hak akses
	 * ke halaman ini. Jika tidak, pengguna akan diarahkan ke halaman login atau halaman akses ditolak.
	 * Kemudian memanggil implementasi induk {@code super.doBeforeCompose} untuk melanjutkan proses
	 * inisialisasi ZKoss normal.
	 *
	 * <b>Parameter:</b><br>
	 * @param page     halaman ZKoss yang sedang dikompose.
	 * @param parent   komponen induk tempat controller ini di-attach.
	 * @param compInfo metadata komponen dari file ZUL.
	 * @return {@link org.zkoss.zk.ui.metainfo.ComponentInfo} dari implementasi induk.
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika pengguna tidak memiliki sesi valid, {@link Common#doCheckSecurity()} akan melakukan
	 * redirect dan biasanya melempar exception yang menghentikan render halaman.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jangan menghapus panggilan ke {@link Common#doCheckSecurity()} karena ini adalah satu-satunya
	 * pengaman akses di titik paling awal siklus hidup halaman ini.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Hook siklus hidup ZKoss yang dipanggil setelah seluruh komponen UI selesai di-compose dari file ZUL.
	 *
	 * <b>Tujuan:</b><br>
	 * Melakukan semua inisialisasi yang memerlukan komponen UI sudah tersedia: memeriksa sesi pengguna,
	 * mengisi combo filter, menginisialisasi paging, mendaftarkan event listener, mengatur visibilitas
	 * tombol berdasarkan hak akses, dan menambahkan tombol tambahan (Sinkronkan Stok, History Revisi).
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memvalidasi sesi pengguna dan hak baca; jika tidak valid, logout dan hentikan eksekusi.</li>
	 *   <li>Mendaftarkan event listener pada {@code searchparent} dan {@code searchPenyedia} agar
	 *       perubahan filter langsung memicu {@link #onSearchDefault}.</li>
	 *   <li>Mengatur visibilitas tab Stok Persediaan berdasarkan konfigurasi sistem.</li>
	 *   <li>Mengisi combo filter jenis penerimaan barang dan lokasi.</li>
	 *   <li>Mengunci combo lokasi jika ada atribut sesi "Lokasi" yang spesifik.</li>
	 *   <li>Mengatur visibilitas tombol Tambah berdasarkan hak akses CREATE.</li>
	 *   <li>Menginisialisasi paging dan timer auto-refresh.</li>
	 *   <li>Menambahkan tombol "Singkronkan dengan stok" yang melakukan sinkronisasi massal
	 *       {@link DetailTransaksiAsset} dari semua penerimaan yang sudah disetujui.</li>
	 *   <li>Menambahkan tombol "History" untuk membuka {@link RevisiPenerimaanPengadaanMasterAssetHelper}.</li>
	 *   <li>Menyiapkan filter lanjut via {@link FilterLanjutHelper#setup}.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param comp komponen root halaman ZUL yang sudah selesai di-compose.
	 * @throws Exception jika terjadi error saat inisialisasi komponen atau akses database.
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika sesi pengguna tidak valid ({@code usersTemp} null), method langsung memanggil
	 * {@link Common#goLogoff()} dan {@code return} untuk menghentikan eksekusi. Exception lain
	 * dari inisialisasi komponen akan merambat ke ZKoss error handler.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Perubahan filter baru harus ditambahkan di sini bersama dengan kode di {@link #initCriteria}.
	 * Tombol-tombol tambahan harus memiliki validasi hak akses yang tepat sebelum ditampilkan.
	 * Hindari operasi database yang berat di metode ini karena akan memperlambat loading halaman.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
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

		searchPenyedia.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (tabStokPersediaan != null) {
			tabStokPersediaan.setVisible(Common.bolehKonfigurasi("tampilkan_stok_persediakan"));
			tabStokPersediaan.getLinkedTab()
					.setVisible(Common.bolehKonfigurasi("tampilkan_stok_persediakan"));
		}

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		Common.insertComboDanSemua(searchJenisPenerimaanBarang, new String[] { "nama" }, "alamat",
				JenisPenerimaanBarang.class, Restrictions.eq("aktif", true));

		Common.insertComboDanSemua(searchlokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (session.getAttribute("Lokasi") != null) {
			Common.selectComboItem(searchlokasi, session.getAttribute("Lokasi"));
			searchlokasi.setDisabled(true);
			session.removeAttribute("Lokasi");
		}
		LokasiAction.kunciLokasi(searchlokasi);

		if (add == null) return;
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);

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

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Singkronkan dengan stok", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Penerimaan Pengadaan dengan Stok");

				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Long> longs = initCriteria(false).setProjection(Projections.property("id"))
								.add(Restrictions.isNotNull("disetujuiOleh")).addOrder(Order.asc("id"))
								.setMaxResults(50000).list();

						String inSql = "";
						for (Long l : longs) {
							inSql += inSql.isEmpty() ? l + "" : "," + l;
						}

						if (!inSql.trim().isEmpty()) {

							Session session = HibernateUtil.currentSession();
							session.createSQLQuery(
									"delete from asset.detail_transaksi_asset where saldo_awal_master_asset_detail is not null;")
									.executeUpdate();
							session.createSQLQuery(
									"delete from asset.detail_transaksi_asset where penerimaan_pengadaan_master_asset_detail in (select id from asset.penerimaan_pengadaan_master_asset_detail where penerimaan_pengadaan_master_asset in ("
											+ inSql + "));")
									.executeUpdate();
							List<PenerimaanPengadaanMasterAssetDetail> penerimaanPengadaanMasterAssetDetails = session
									.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
									.add(Restrictions.in("penerimaanPengadaanMasterAsset.id", longs)).list();
							int nomorBaris = 0;
							for (PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail : penerimaanPengadaanMasterAssetDetails) {

								String kunci = String.valueOf(penerimaanPengadaanMasterAssetDetail);
								try {
									DetailTransaksiAsset detailTransaksiAsset = new DetailTransaksiAsset();
									detailTransaksiAsset.setPenerimaanPengadaanMasterAssetDetail(
											penerimaanPengadaanMasterAssetDetail);
									detailTransaksiAsset.setQtyBonus(0.0);

									detailTransaksiAsset
											.setMasterAsset(penerimaanPengadaanMasterAssetDetail.getMasterAsset());
									detailTransaksiAsset.setKeterangan("Transaksi Terima Barang/Jasa");
									detailTransaksiAsset.setKodeTransaksi(LibraryUtil.BELI_MASUK);
									detailTransaksiAsset.setPemilikAsset(penerimaanPengadaanMasterAssetDetail
											.getPenerimaanPengadaanMasterAsset().getPemilikAsset());
									detailTransaksiAsset.setLokasi(penerimaanPengadaanMasterAssetDetail
											.getPenerimaanPengadaanMasterAsset().getLokasi());
									detailTransaksiAsset.setRuang(penerimaanPengadaanMasterAssetDetail
											.getPenerimaanPengadaanMasterAsset().getRuang());
									detailTransaksiAsset.setQty(penerimaanPengadaanMasterAssetDetail.getDiterima());
									detailTransaksiAsset.setTanggal(penerimaanPengadaanMasterAssetDetail
											.getPenerimaanPengadaanMasterAsset().getTanggalPembuatan());

									session.save(detailTransaksiAsset);
									session.flush();

									// FASE 1: BAST -> stok masuk kantin untuk produk yang tertaut (fail-safe).
									ais.action.master.inventory.KantinAssetSyncUtil
											.syncPengadaanDariBast(session, penerimaanPengadaanMasterAssetDetail);
									laporan.catatBerhasil(nomorBaris, kunci, "Sinkronisasi stok berhasil");
								} catch (Exception e) {
									ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PenerimaanPengadaanMasterAssetAction.java:572");
									laporan.catatGagalDetail(nomorBaris, kunci, e);
								}
								nomorBaris++;
							}
							penerimaanPengadaanMasterAssetDetails.clear();
							penerimaanPengadaanMasterAssetDetails = null;
						}
						laporan.selesaikan(new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								onSearchDefault(null);
							}
						});
					}
				});

			}

		});
		if (button != null) { button.setParent(add.getParent()); }

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiPenerimaanPengadaanMasterAssetHelper revisiHelper = new RevisiPenerimaanPengadaanMasterAssetHelper(
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										onSearchDefault(arg0);
									}
								});
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		if (button != null) { button.setParent(add.getParent()); }

		// Tombol "Download" data BAST/Penerimaan ke Excel (mengikuti pola AgamaAction, tanpa tombol Upload).
		// DataCriteria dibungkus anonim agar mendelegasikan ke initCriteria(order) milik Action ini,
		// sehingga isi file mengikuti seluruh filter pencarian yang sedang aktif.
		if (add != null) {
			Common.appendDownloadButton(add, PenerimaanPengadaanMasterAsset.class,
					new ais.ui.util.DataCriteria() {
						@Override
						public Object initCriteria(boolean order) {
							return PenerimaanPengadaanMasterAssetAction.this.initCriteria(order);
						}
					},
					"id", "kode", "kodeTagihan", "keterangan", "nilai", "pemesananPengadaanMasterAsset",
					"penyedia", "lokasi", "ruang", "jenisPenerimaanBarang", "tanggalPembuatan",
					"tanggalPersetujuan", "dibuatOleh", "disetujuiOleh", "aktif");
		}

	        FilterLanjutHelper.setup(comp);
}

	/**
	 * <h3>PenerimaanPengadaanMasterAssetRenderer — Renderer baris grid daftar penerimaan pengadaan</h3>
	 *
	 * <b>Untuk apa:</b><br>
	 * Kelas inner ini bertanggung jawab untuk merender setiap baris pada grid daftar penerimaan
	 * pengadaan. Setiap baris menampilkan informasi lengkap satu dokumen penerimaan beserta
	 * tombol-tombol aksi (Cetak, Setujui, Batalkan, Ubah, Hapus) yang visibilitasnya disesuaikan
	 * dengan hak akses pengguna dan status persetujuan dokumen.
	 *
	 * <b>Cara kerja:</b><br>
	 * Metode {@link #render} menerima satu objek {@link PenerimaanPengadaanMasterAsset} dan
	 * memetakannya ke dalam sel-sel {@link Row} ZKoss. Detail item penerimaan dirender oleh
	 * {@link PenerimaanPengadaanMasterAssetDetailAction}. Informasi pembayaran terkait (kode PO bayar)
	 * dimuat langsung dari database dan ditampilkan sebagai link yang dapat diklik untuk membuka
	 * dokumen pembayaran. Tombol persetujuan hanya tampil jika pengguna memiliki hak APPROVE dan
	 * dokumen belum disetujui; tombol pembatalan hanya tampil jika pengguna memiliki hak REJECT dan
	 * dokumen sudah disetujui. Persetujuan memicu sinkronisasi stok ke {@link DetailTransaksiAsset}.
	 *
	 * <b>Threading:</b><br>
	 * Renderer ini dijalankan di thread event ZKoss. Operasi database di dalamnya (query pembayaran,
	 * query detail saat persetujuan) menggunakan sesi Hibernate yang di-bind ke thread yang sama.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika ada kolom baru yang ditambahkan ke grid, tambahkan juga di sini dan sesuaikan di file ZUL
	 * yang mendefinisikan header kolom. Perhatikan urutan penambahan komponen ke {@link Row} harus
	 * sesuai persis dengan urutan definisi kolom di ZUL.
	 */
	class PenerimaanPengadaanMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data penerimaan pengadaan ke dalam komponen-komponen ZKoss.
		 *
		 * <b>Tujuan:</b><br>
		 * Mengonversi satu objek domain {@link PenerimaanPengadaanMasterAsset} menjadi tampilan baris
		 * grid yang interaktif dengan informasi detail dan tombol aksi yang sesuai konteks.
		 *
		 * <b>Cara kerja:</b><br>
		 * <ol>
		 *   <li>Membuat {@link PenerimaanPengadaanMasterAssetDetailAction} sebagai sel pertama yang
		 *       menampilkan detail item barang/jasa.</li>
		 *   <li>Menampilkan informasi revisi, workspace, keterangan termin, dan uang muka di sel kedua.</li>
		 *   <li>Menampilkan nama penyedia dan link ke dokumen pembayaran terkait di sel ketiga.</li>
		 *   <li>Menampilkan lokasi, dibuat oleh/tanggal, disetujui oleh/tanggal, dan keterangan.</li>
		 *   <li>Jika belum disetujui dan pengguna memiliki hak edit, menampilkan checkbox Aktif interaktif.</li>
		 *   <li>Membuat toolbar berisi tombol Cetak, Setujui, Batalkan, Ubah, Hapus dengan visibilitas
		 *       dan event listener yang sesuai.</li>
		 * </ol>
		 *
		 * <b>Parameter:</b><br>
		 * @param arg0 baris {@link Row} ZKoss yang akan diisi komponen.
		 * @param arg1 objek data {@link PenerimaanPengadaanMasterAsset} yang akan ditampilkan.
		 * @throws Exception jika terjadi error saat pembuatan komponen atau akses database.
		 *
		 * <b>Penanganan error:</b><br>
		 * Exception dari operasi persetujuan/pembatalan/hapus ditangkap oleh listener internal dan
		 * ditampilkan via {@link Common#tampilErrorJikaAdmin}. Operasi hapus memeriksa constraint FK
		 * dan menampilkan pesan informatif jika data tidak bisa dihapus.
		 *
		 * <b>Pemeliharaan:</b><br>
		 * Saat menambah/menghapus tombol aksi, pastikan visibilitas defaultnya sudah benar. Operasi
		 * sinkronisasi stok dalam event persetujuan harus tetap sinkron dengan logika di
		 * {@link #onSave} untuk konsistensi data.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset = (PenerimaanPengadaanMasterAsset) arg1;

			final PenerimaanPengadaanMasterAssetDetailAction detail;
			(detail = new PenerimaanPengadaanMasterAssetDetailAction(penerimaanPengadaanMasterAsset,
					penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null)).setParent(arg0);

			Vbox aaaaaa;
			(aaaaaa = RevisiHelper.createNewRevisi(PenerimaanPengadaanMasterAsset.class, penerimaanPengadaanMasterAsset,
					penerimaanPengadaanMasterAsset.getKode())).setParent(arg0);

			if (penerimaanPengadaanMasterAsset.getWorkspace() != null) {
				new MyLabelKecil(penerimaanPengadaanMasterAsset.getWorkspace().getKode() + "-"
						+ penerimaanPengadaanMasterAsset.getWorkspace().getNama()).setParent(aaaaaa);
			}

			if (penerimaanPengadaanMasterAsset.getKeteranganTermin() != null
					&& !penerimaanPengadaanMasterAsset.getKeteranganTermin().trim().isEmpty()) {
				new MyLabelKecil(penerimaanPengadaanMasterAsset.getKeteranganTermin()).setParent(aaaaaa);
			}

			if (penerimaanPengadaanMasterAsset.getUangMuka() != null) {
				new Label(penerimaanPengadaanMasterAsset.getUangMuka() == null ? ""
						: penerimaanPengadaanMasterAsset.getUangMuka().getKode() + "-"
								+ penerimaanPengadaanMasterAsset.getUangMuka().getNama())
						.setParent(aaaaaa);
			}

			Vbox aaa = new Vbox();
			aaa.setParent(arg0);

			new Label(penerimaanPengadaanMasterAsset.getPenyedia() == null ? ""
					: penerimaanPengadaanMasterAsset.getPenyedia().getNama()).setParent(aaa);

			List<PembayaranPengadaanMasterAssetDetail> pembayaranPengadaanMasterAssetDetails = HibernateUtil
					.currentSession().createCriteria(PembayaranPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset)).list();
			for (final PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail : pembayaranPengadaanMasterAssetDetails) {
				A aa = new A(pembayaranPengadaanMasterAssetDetail.getPembayaranPengadaanMasterAsset().getKode());

				aa.setStyle("font-size:10px");
				aaa.appendChild(aa);

				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (pembayaranPengadaanMasterAssetDetail.getPembayaranPengadaanMasterAsset()
								.getDisposisiSop() != null) {
							TampilanAlurSopAction.prosess(pembayaranPengadaanMasterAssetDetail
									.getPembayaranPengadaanMasterAsset().getDisposisiSop().getId(), null, null, true,
									arg0.getTarget());
						} else {
							PembayaranPengadaanMasterAssetAction
									.cetak(pembayaranPengadaanMasterAssetDetail.getPembayaranPengadaanMasterAsset());
						}
					}
				});
			}
			pembayaranPengadaanMasterAssetDetails.clear();
			pembayaranPengadaanMasterAssetDetails = null;

			new Label(penerimaanPengadaanMasterAsset.getLokasi() == null ? ""
					: (penerimaanPengadaanMasterAsset.getLokasi().getNama())).setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(penerimaanPengadaanMasterAsset.getDibuatOleh() == null ? ""
					: penerimaanPengadaanMasterAsset.getDibuatOleh().getUserNama()).setParent(a);
			new MyLabelAgakKecil(penerimaanPengadaanMasterAsset.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(penerimaanPengadaanMasterAsset.getTanggalPembuatan()))
					.setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			final MyLabelAgakKecil disetujuiOleh;
			(disetujuiOleh = new MyLabelAgakKecil(penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null ? ""
					: penerimaanPengadaanMasterAsset.getDisetujuiOleh().getUserNama())).setParent(a);
			final MyLabelAgakKecil disetujuiTanggal;
			(disetujuiTanggal = new MyLabelAgakKecil(penerimaanPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(penerimaanPengadaanMasterAsset.getTanggalPersetujuan())))
					.setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);

			new MyLabelKecil(Common.simpleString(penerimaanPengadaanMasterAsset.getKeterangan())).setParent(vbox1);
			if (penerimaanPengadaanMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa,
						"SOP " + penerimaanPengadaanMasterAsset.getDisposisiSop().getKeterangan() + " ("
								+ penerimaanPengadaanMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(penerimaanPengadaanMasterAsset.getDisposisiSop().getId(), null,
								null, true, arg0.getTarget());
					}
				});
			}
			new MyLabelKecil(penerimaanPengadaanMasterAsset.getKurir() == null
					|| penerimaanPengadaanMasterAsset.getKurir().trim().isEmpty() ? ""
							: "Nama Kurir : " + penerimaanPengadaanMasterAsset.getKurir())
					.setParent(vbox1);

			if (penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null && edit) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(penerimaanPengadaanMasterAsset.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						penerimaanPengadaanMasterAsset.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(penerimaanPengadaanMasterAsset);
					}
				});
			} else {
				new Label(penerimaanPengadaanMasterAsset.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Penerimaan Pengadaan Asset");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(penerimaanPengadaanMasterAsset);
				}

			});
			button.setParent(toolbar);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && penerimaanPengadaanMasterAsset.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Penerimaan Pengadaan ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();
										Integer countMasterAssetjumlah = ((Number) session
												.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
												.setProjection(Projections.count("id"))
												.add(Restrictions.eq("penerimaanPengadaanMasterAsset",
														penerimaanPengadaanMasterAsset))
												.add(Restrictions.lt("jumlah", 1.0)).uniqueResult()).intValue();

										if (!countMasterAssetjumlah.equals(0)) {
											MyMessageboxConfig.show("Mohon maaf, terdapat baris penerimaan yang belum memiliki jumlah yang valid. Langkah yang dapat dilakukan: (1) Periksa setiap baris pada daftar penerimaan barang/jasa; (2) Isi jumlah pada baris yang masih kosong atau bernilai 0; (3) ulangi proses persetujuan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											return;
										}

										countMasterAssetjumlah = ((Number) session
												.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
												.setProjection(Projections.count("id"))
												.add(Restrictions.eq("penerimaanPengadaanMasterAsset",
														penerimaanPengadaanMasterAsset))
												.add(Restrictions.lt("diterima", 1.0)).uniqueResult()).intValue();

										if (!countMasterAssetjumlah.equals(0)) {
											MyMessageboxConfig.show("Mohon maaf, terdapat baris penerimaan yang kolom 'Data Diterima' belum diisi. Langkah yang dapat dilakukan: (1) Periksa setiap baris pada daftar barang/jasa yang diterima; (2) Isi kolom 'Diterima' dengan jumlah aktual yang diterima; (3) ulangi proses persetujuan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											return;
										}

										penerimaanPengadaanMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
										penerimaanPengadaanMasterAsset
												.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

										Common.refreshUpdate(session, penerimaanPengadaanMasterAsset);

										List<PenerimaanPengadaanMasterAssetDetail> penerimaanPengadaanMasterAssetDetails = session
												.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
												.add(Restrictions.eq("penerimaanPengadaanMasterAsset",
														penerimaanPengadaanMasterAsset))
												.list();

										session.createSQLQuery(
												"delete from asset.detail_transaksi_asset where penerimaan_pengadaan_master_asset_detail in (select id from asset.penerimaan_pengadaan_master_asset_detail where penerimaan_pengadaan_master_asset = "
														+ penerimaanPengadaanMasterAsset.getId() + ");")
												.executeUpdate();
										for (PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail : penerimaanPengadaanMasterAssetDetails) {

											DetailTransaksiAsset detailTransaksiAsset = new DetailTransaksiAsset();
											detailTransaksiAsset.setPenerimaanPengadaanMasterAssetDetail(
													penerimaanPengadaanMasterAssetDetail);
											detailTransaksiAsset.setQtyBonus(0.0);

											detailTransaksiAsset.setMasterAsset(
													penerimaanPengadaanMasterAssetDetail.getMasterAsset());
											detailTransaksiAsset.setKeterangan("Transaksi Terima Barang/Jasa");
											detailTransaksiAsset.setKodeTransaksi(LibraryUtil.BELI_MASUK);
											detailTransaksiAsset
													.setPemilikAsset(penerimaanPengadaanMasterAsset.getPemilikAsset());
											detailTransaksiAsset.setLokasi(penerimaanPengadaanMasterAsset.getLokasi());
											detailTransaksiAsset.setRuang(penerimaanPengadaanMasterAsset.getRuang());
											detailTransaksiAsset
													.setQty(penerimaanPengadaanMasterAssetDetail.getDiterima());
											detailTransaksiAsset
													.setTanggal(penerimaanPengadaanMasterAsset.getTanggalPembuatan());

											session.save(detailTransaksiAsset);
											session.flush();

											// FASE 1: BAST -> stok masuk kantin untuk produk yang tertaut (fail-safe).
											ais.action.master.inventory.KantinAssetSyncUtil
													.syncPengadaanDariBast(session, penerimaanPengadaanMasterAssetDetail);
										}

										disetujuiTanggal.setValue(
												penerimaanPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(penerimaanPengadaanMasterAsset
																.getTanggalPersetujuan()));
										disetujuiOleh.setValue(penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null
												? ""
												: penerimaanPengadaanMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && penerimaanPengadaanMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(
												edit && penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(
												delete && penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										cetak(penerimaanPengadaanMasterAsset);
									}
								}
							});
				}

			});
			disetujui.setParent(toolbar);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Penerimaan Pengadaan Aset ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										penerimaanPengadaanMasterAsset.setDisetujuiOleh(null);
										penerimaanPengadaanMasterAsset.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, penerimaanPengadaanMasterAsset);

										session.createSQLQuery(
												"delete from asset.detail_transaksi_asset where penerimaan_pengadaan_master_asset_detail in (select id from asset.penerimaan_pengadaan_master_asset_detail where penerimaan_pengadaan_master_asset = "
														+ penerimaanPengadaanMasterAsset.getId() + ");")
												.executeUpdate();

										session.createSQLQuery(
												"delete from asset.asset where permintaan_pengadaan_master_asset_detail in (select id from asset.penerimaan_pengadaan_master_asset_detail where penerimaan_pengadaan_master_asset = "
														+ penerimaanPengadaanMasterAsset.getId() + ");")
												.executeUpdate();

										disetujuiTanggal.setValue(
												penerimaanPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(penerimaanPengadaanMasterAsset
																.getTanggalPersetujuan()));
										disetujuiOleh.setValue(penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null
												? ""
												: penerimaanPengadaanMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && penerimaanPengadaanMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(
												edit && penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(
												delete && penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			dibatalkan.setParent(toolbar);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit && penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(penerimaanPengadaanMasterAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
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

											Session session = HibernateUtil.currentSession();
											if (SopUtil.hapusDisposisi(session,
													penerimaanPengadaanMasterAsset.getDisposisiSop())) {
												List<PenerimaanPengadaanMasterAssetDetail> penerimaanPengadaanMasterAssetDetails = session
														.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
														.add(Restrictions.eq("penerimaanPengadaanMasterAsset",
																penerimaanPengadaanMasterAsset))
														.list();
												for (PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail : penerimaanPengadaanMasterAssetDetails) {
													session.delete(penerimaanPengadaanMasterAssetDetail);
												}
												session.flush();

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														Session session = HibernateUtil.currentSession();
														Common.refreshDelete(session, penerimaanPengadaanMasterAsset);
														session.flush();
														onSearchDefault(arg0);
													}
												});
											}

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
			hapus.setParent(toolbar);
			toolbar.setParent(arg0);
		}
	}

	/**
	 * Menangani event klik tombol "Tambah" untuk membuka formulir penambahan penerimaan baru.
	 *
	 * <b>Tujuan:</b><br>
	 * Memulai alur penambahan dokumen penerimaan pengadaan baru dengan membuat objek domain kosong
	 * dan menampilkan dialog modal formulir.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat instance {@link PenerimaanPengadaanMasterAsset} baru (tanpa ID) dan meneruskannya ke
	 * metode {@link #init} yang akan membangun formulir secara programatik. Setelah formulir siap,
	 * {@code addWindow} ditampilkan sebagai dialog modal sehingga pengguna tidak bisa berinteraksi
	 * dengan halaman di belakangnya sampai formulir ditutup.
	 *
	 * <b>Parameter:</b><br>
	 * @param event event ZKoss yang dipicu saat tombol Tambah diklik; tidak digunakan langsung.
	 * @throws Exception jika terjadi error saat inisialisasi formulir.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika ada validasi awal yang perlu dilakukan sebelum membuka formulir (misalnya cek apakah
	 * periode pengadaan masih aktif), tambahkan di sini sebelum memanggil {@link #init}.
	 */
	public void onAdd(Event event) throws Exception {
		init(new PenerimaanPengadaanMasterAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Menginisialisasi dan membangun tampilan dialog formulir tambah/ubah penerimaan pengadaan.
	 *
	 * <b>Tujuan:</b><br>
	 * Menyiapkan seluruh struktur UI di dalam {@code addWindow} untuk operasi tambah (objek baru)
	 * maupun ubah (objek yang sudah ada di database). Ini termasuk membersihkan konten dialog
	 * sebelumnya, membangun layout Borderlayout, memanggil {@link #form} untuk membuat formulir,
	 * dan mendaftarkan event listener pada tombol Simpan dan Batal.
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menyimpan referensi objek penerimaan ke field instance dan mereset {@code disposisiSop}.</li>
	 *   <li>Mengatur judul dialog sesuai mode (Tambah atau Ubah).</li>
	 *   <li>Membersihkan konten dialog sebelumnya dengan {@link Common#clear}.</li>
	 *   <li>Membuat {@link Borderlayout} dengan area Center (formulir) dan South (toolbar tombol).</li>
	 *   <li>Memanggil {@link #form} untuk membangun formulir dan menempatkannya di area Center.</li>
	 *   <li>Tombol Batal menutup dialog tanpa menyimpan; tombol Simpan memanggil {@link #onSave}
	 *       dan jika berhasil memperbarui grid serta menutup dialog.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param penerimaanPengadaanMasterAsset objek penerimaan yang akan diedit; jika {@code id == null}
	 *                                       maka ini adalah operasi tambah baru.
	 * @throws Exception jika terjadi error saat membangun komponen UI atau mengakses database.
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dilempar ke pemanggil. Pastikan {@link #onAdd} dan handler ubah di renderer
	 * menangkap atau membiarkan exception ini.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika ada inisialisasi tambahan yang diperlukan sebelum formulir ditampilkan (misalnya preload
	 * data referensi), tambahkan di sini setelah pemanggilan {@link Common#clear}. Perubahan ukuran
	 * dialog harus dilakukan di sini juga.
	 */
	private void init(PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset) throws Exception {
		this.penerimaanPengadaanMasterAsset = penerimaanPengadaanMasterAsset;
		addWindow.setTitle(penerimaanPengadaanMasterAsset.getId() == null ? "Tambah Penerimaan Pengadaan Barang/Jasa" : "Ubah Penerimaan Pengadaan Barang/Jasa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");

		disposisiSop = null;
		center.appendChild(form(penerimaanPengadaanMasterAsset, disposisiSop, save, null));

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

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	/**
	 * Menghasilkan dan merender baris-baris detail item barang/jasa dalam grid formulir penerimaan.
	 *
	 * <b>Tujuan:</b><br>
	 * Metode ini mengisi grid detail ({@code gridMasterAsset}) dengan baris-baris item yang harus
	 * diterima, berdasarkan sumber data yang relevan: Uang Muka (UM), Pemesanan Pengadaan (PO),
	 * atau keduanya. Jika dokumen penerimaan sudah memiliki ID (mode edit), baris yang sudah ada
	 * di database akan dimuat; jika belum ada, baris baru akan dibuat dan disimpan secara idempotensi.
	 *
	 * <b>Cara kerja:</b><br>
	 * Metode ini memiliki tiga cabang utama berdasarkan sumber data:
	 * <ol>
	 *   <li><b>Uang Muka (UM):</b> Query {@link PermintaanPengadaanMasterAssetDetail} berdasarkan
	 *       relasi ke Uang Muka, kemudian untuk setiap item mencari apakah sudah ada detail penerimaan
	 *       yang sesuai (mempertimbangkan kode termin jika PO berjenis termin). Jika belum ada dan
	 *       dokumen sudah punya ID, item baru disimpan ke database secara idempotensi (cek rowCount
	 *       sebelum save). Item kemudian dirender ke grid via
	 *       {@link PenerimaanPengadaanMasterAssetHelper#initRow}.</li>
	 *   <li><b>Pemesanan Pengadaan (PO) tanpa UM:</b> Serupa dengan cabang UM tetapi query
	 *       {@link PemesananPengadaanMasterAssetDetail} berdasarkan PO. Harga beli diambil dari
	 *       JSON termin jika tersedia, atau dari detail PO. Setelah render, timer hitung ulang
	 *       total dipicu via {@link PenerimaanPengadaanMasterAssetHelper#eventListenerHitungUlang}.</li>
	 *   <li><b>Tidak ada sumber:</b> Metode langsung return tanpa melakukan apa-apa.</li>
	 * </ol>
	 * Jika PO bertipe termin dan kodeTermin belum dipilih, metode langsung return (validasi awal).
	 *
	 * <b>Parameter:</b><br>
	 * @param mypemesananPengadaanMasterAsset objek PO yang terkait; boleh null jika menggunakan UM.
	 * @param uangMuka                        objek Uang Muka yang terkait; boleh null jika menggunakan PO.
	 * @param kodeTermin                      kode termin yang dipilih (misal "T1", "T2"); null jika bukan PO termin.
	 * @param jsonObject                      data JSON termin yang berisi informasi penagihan dan pajak;
	 *                                        null jika bukan dari pemilihan termin baru.
	 *
	 * <b>Penanganan error:</b><br>
	 * Setiap iterasi render baris dibungkus dalam try-catch yang memanggil
	 * {@link Common#tampilErrorJikaAdmin} agar error pada satu item tidak menghentikan render item lainnya.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Logika idempotensi (cek sudahAda sebelum save) krusial untuk mencegah duplicate key. Jika
	 * ada perubahan struktur detail penerimaan, pastikan kedua cabang (UM dan PO) diperbarui secara
	 * konsisten. Query termin menggunakan LEFT_JOIN alias yang harus konsisten dengan mapping Hibernate.
	 */
	@SuppressWarnings("unchecked")
	public void generateDetail(PemesananPengadaanMasterAsset mypemesananPengadaanMasterAsset, UangMuka uangMuka,
			String kodeTermin, JSONObject jsonObject) {

		if (mypemesananPengadaanMasterAsset != null && mypemesananPengadaanMasterAsset.getByTermin()
				&& (kodeTermin == null || kodeTermin.trim().isEmpty())) {
			return;
		}

		if (uangMuka != null) {

			Session session = HibernateUtil.currentSession();
			List<PermintaanPengadaanMasterAssetDetail> permintaanPengadaanMasterAssetDetails = session
					.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("uangMuka", uangMuka)).list();

			System.out.println("permintaanPengadaanMasterAssetDetails -> "
					+ permintaanPengadaanMasterAssetDetails.size() + ", uangMuka -> " + uangMuka);

			try {
				penerimaanPengadaanMasterAssetHelper.columnsData(mypemesananPengadaanMasterAsset, persetujuan);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
			rows.setParent(gridMasterAsset);

			Common.clear(rows);

			for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : permintaanPengadaanMasterAssetDetails) {

				PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = penerimaanPengadaanMasterAsset.getId() == null ? null
						: (PenerimaanPengadaanMasterAssetDetail) session
						.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
						.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset",
								Criteria.LEFT_JOIN)

						.add(kodeTermin == null || kodeTermin.trim().isEmpty()
								|| !mypemesananPengadaanMasterAsset.getByTermin() ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("penerimaanPengadaanMasterAsset.kodeTermin", kodeTermin))

						.add(Restrictions.eq("penerimaanPengadaanMasterAsset.id", penerimaanPengadaanMasterAsset.getId()))
						.add(Restrictions.eq("permintaanPengadaanMasterAsset", permintaanPengadaanMasterAssetDetail))
						.setMaxResults(1).uniqueResult();
				if (penerimaanPengadaanMasterAssetDetail == null) {
					penerimaanPengadaanMasterAssetDetail = new PenerimaanPengadaanMasterAssetDetail();
					penerimaanPengadaanMasterAssetDetail
							.setMasterAsset(permintaanPengadaanMasterAssetDetail.getMasterAsset());
					penerimaanPengadaanMasterAssetDetail.setJumlah(permintaanPengadaanMasterAssetDetail.getJumlah());
					penerimaanPengadaanMasterAssetDetail.setDiterima(permintaanPengadaanMasterAssetDetail.getJumlah());
					penerimaanPengadaanMasterAssetDetail
							.setKeterangan(permintaanPengadaanMasterAssetDetail.getKeterangan());

					if (jsonObject != null && !jsonObject.isNull("penagihan")) {
						try {
							Double penagihan = jsonObject.getDouble("penagihan");
							penerimaanPengadaanMasterAssetDetail.setHargaBeli(penagihan);
							penerimaanPengadaanMasterAssetDetail.setHargaBeliDiEntry(penagihan);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							ais.common.Common.tampilErrorJikaAdmin(e);
						}

					} else {
						penerimaanPengadaanMasterAssetDetail
								.setHargaBeli(permintaanPengadaanMasterAssetDetail.getHargaBeli());
					}

					penerimaanPengadaanMasterAssetDetail
							.setPermintaanPengadaanMasterAsset(permintaanPengadaanMasterAssetDetail);

					if (penerimaanPengadaanMasterAsset.getId() != null) {
						penerimaanPengadaanMasterAssetDetail
								.setPenerimaanPengadaanMasterAsset(penerimaanPengadaanMasterAsset);
						// Idempotensi: cegah duplicate key (kodeunik) saat generateDetail terpanggil
						// ulang/double-submit — insert hanya bila detail utk permintaan-detail ini belum ada.
						Number sudahAda = (Number) session.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
								.add(Restrictions.eq("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset))
								.add(Restrictions.eq("permintaanPengadaanMasterAsset",
										permintaanPengadaanMasterAssetDetail))
								.setProjection(Projections.rowCount()).uniqueResult();
						if (sudahAda == null || sudahAda.longValue() == 0L) {
							session.save(penerimaanPengadaanMasterAssetDetail);
							session.flush();
						}
					}
				}

				try {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					penerimaanPengadaanMasterAssetHelper.initRow(row, penerimaanPengadaanMasterAssetDetail);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

		}

		else if (mypemesananPengadaanMasterAsset != null && mypemesananPengadaanMasterAsset.getId() != null) {
			Session session = HibernateUtil.currentSession();
			List<PemesananPengadaanMasterAssetDetail> pemesananPengadaanMasterAssetDetails = session
					.createCriteria(PemesananPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("pemesananPengadaanMasterAsset", mypemesananPengadaanMasterAsset)).list();

			try {
				penerimaanPengadaanMasterAssetHelper.columnsData(mypemesananPengadaanMasterAsset, persetujuan);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
			rows.setParent(gridMasterAsset);

			Common.clear(rows);

			for (PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail : pemesananPengadaanMasterAssetDetails) {

				PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = penerimaanPengadaanMasterAsset.getId() == null ? null
						: (PenerimaanPengadaanMasterAssetDetail) session
						.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
						.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset",
								Criteria.LEFT_JOIN)

						.add(kodeTermin == null || kodeTermin.trim().isEmpty()
								|| !mypemesananPengadaanMasterAsset.getByTermin() ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("penerimaanPengadaanMasterAsset.kodeTermin", kodeTermin))

						.add(Restrictions.eq("penerimaanPengadaanMasterAsset.id", penerimaanPengadaanMasterAsset.getId()))
						.add(Restrictions.eq("pemesananPengadaanMasterAssetDetail",
								pemesananPengadaanMasterAssetDetail))
						.setMaxResults(1).uniqueResult();
				if (penerimaanPengadaanMasterAssetDetail == null) {
					penerimaanPengadaanMasterAssetDetail = new PenerimaanPengadaanMasterAssetDetail();
					penerimaanPengadaanMasterAssetDetail
							.setMasterAsset(pemesananPengadaanMasterAssetDetail.getMasterAsset());
					penerimaanPengadaanMasterAssetDetail.setJumlah(pemesananPengadaanMasterAssetDetail.getJumlah());
					penerimaanPengadaanMasterAssetDetail.setDiterima(pemesananPengadaanMasterAssetDetail.getJumlah());
					penerimaanPengadaanMasterAssetDetail
							.setKeterangan(pemesananPengadaanMasterAssetDetail.getKeterangan());

					if (jsonObject != null && !jsonObject.isNull("penagihan")) {
						try {
							Double penagihan = jsonObject.getDouble("penagihan");
							penerimaanPengadaanMasterAssetDetail.setHargaBeli(penagihan);
							penerimaanPengadaanMasterAssetDetail.setHargaBeliDiEntry(penagihan);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							ais.common.Common.tampilErrorJikaAdmin(e);
						}

					} else {
						penerimaanPengadaanMasterAssetDetail
								.setHargaBeli(pemesananPengadaanMasterAssetDetail.getHargaBeli());
					}

					penerimaanPengadaanMasterAssetDetail
							.setPemesananPengadaanMasterAssetDetail(pemesananPengadaanMasterAssetDetail);

					if (penerimaanPengadaanMasterAsset.getId() != null) {
						penerimaanPengadaanMasterAssetDetail
								.setPenerimaanPengadaanMasterAsset(penerimaanPengadaanMasterAsset);
						// Idempotensi: cegah duplicate key (kodeunik) saat generateDetail terpanggil
						// ulang/double-submit — insert hanya bila detail utk pemesanan-detail ini belum ada.
						Number sudahAda = (Number) session.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
								.add(Restrictions.eq("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset))
								.add(Restrictions.eq("pemesananPengadaanMasterAssetDetail",
										pemesananPengadaanMasterAssetDetail))
								.setProjection(Projections.rowCount()).uniqueResult();
						if (sudahAda == null || sudahAda.longValue() == 0L) {
							session.save(penerimaanPengadaanMasterAssetDetail);
							session.flush();
						}
					}
				}

				try {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					penerimaanPengadaanMasterAssetHelper.initRow(row, penerimaanPengadaanMasterAssetDetail);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

			Common.createDefaultTimer(penerimaanPengadaanMasterAssetHelper.eventListenerHitungUlang);
		}
	}

	/**
	 * Memvalidasi input formulir dan menyimpan data penerimaan pengadaan ke database.
	 *
	 * <b>Tujuan:</b><br>
	 * Metode ini adalah inti dari operasi simpan. Ia memvalidasi seluruh input pengguna, kemudian
	 * menyimpan atau memperbarui entitas {@link PenerimaanPengadaanMasterAsset} beserta semua
	 * {@link PenerimaanPengadaanMasterAssetDetail}-nya, menghitung nilai total, dan jika dicentang
	 * untuk langsung disetujui, juga mencatat persetujuan dan memicu cetak otomatis.
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li><b>Validasi:</b> Memeriksa keberadaan Anggaran (jika mode tanpa pemesanan dan bukan
	 *       tanpa anggaran), Kode Penerimaan, Jenis Penerimaan Barang, Pemesanan/Uang Muka (jika
	 *       tidak tanpa pemesanan), Termin (jika PO bertipe termin), Keterangan, dan kelengkapan
	 *       data MasterAsset pada setiap baris detail. Jika ada validasi gagal, menampilkan pesan
	 *       peringatan dan mengembalikan {@code false}.</li>
	 *   <li><b>Load entitas:</b> Untuk operasi update, me-load ulang entitas dari database untuk
	 *       menghindari stale data.</li>
	 *   <li><b>Set field:</b> Menyalin semua nilai dari komponen UI ke entitas domain.</li>
	 *   <li><b>Simpan header:</b> Untuk data baru, generate kode unik terlebih dahulu menggunakan
	 *       {@link #generateCode(boolean)}, set pembuat, lalu {@code session.save}. Untuk data
	 *       yang sudah ada, {@code session.update}.</li>
	 *   <li><b>Simpan detail:</b> Iterasi baris grid detail, menyimpan setiap
	 *       {@link PenerimaanPengadaanMasterAssetDetail} dan memperbarui relasi balik pada
	 *       {@link PemesananPengadaanMasterAssetDetail}. Akumulasi nilai total.</li>
	 *   <li><b>Update nilai dan uang muka:</b> Menyimpan total ke header dan memperbarui relasi
	 *       Uang Muka jika ada.</li>
	 *   <li><b>Persetujuan opsional:</b> Melalui {@link Common#createDefaultTimer}, jika checkbox
	 *       setujui dicentang, mencatat persetujuan; jika tidak, menghapus persetujuan. Dilanjutkan
	 *       dengan timer cetak otomatis.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param event event ZKoss dari tombol Simpan; tidak digunakan langsung di dalam metode.
	 * @return {@code true} jika penyimpanan berhasil; {@code false} jika ada validasi yang gagal.
	 * @throws Exception jika terjadi error database yang tidak tertangani.
	 *
	 * <b>Penanganan error:</b><br>
	 * Error pada iterasi detail ditangkap dalam blok try-catch dengan komentar
	 * {@code // TODO: handle exception}. Sebaiknya error ini di-log atau ditampilkan kepada admin.
	 * Constraint violation dari database akan merambat sebagai runtime exception.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika ada field baru pada entitas penerimaan, tambahkan setter-nya di bagian "Set field".
	 * Pastikan kode generate ({@link #generateCode}) memang dipanggil dengan {@code tambah=true}
	 * hanya saat save pertama, bukan saat update, agar nomor urut tidak boros.
	 */
	@SuppressWarnings("unchecked")
	/**
	 * Bangun baris detail BAST dari rincian item REIMBURSEMENT (formula JSON) —
	 * klon pola cabang UangMuka pada {@link #generateDetail}: hanya baris item yang
	 * DIPETAKAN ke Barang (key "masterAsset" pada JSON) yang menjadi baris BAST;
	 * baris biaya murni (tanpa barang) dilewati. Untuk BAST tersimpan (id != null),
	 * baris yang sudah ada dirender ulang tanpa membuat duplikat.
	 */
	public void generateDetailReimbursement(ais.database.model.akunting.ReimbursementPegawai reimb) {
		if (reimb == null || gridMasterAsset == null) {
			return;
		}
		Session session = HibernateUtil.currentSession();

		try {
			penerimaanPengadaanMasterAssetHelper.columnsData(null, persetujuan);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
		rows.setParent(gridMasterAsset);
		Common.clear(rows);

		// BAST sudah tersimpan: render baris yang ada saja (idempoten, tanpa duplikat).
		if (penerimaanPengadaanMasterAsset.getId() != null) {
			List<PenerimaanPengadaanMasterAssetDetail> details = session
					.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset)).list();
			for (PenerimaanPengadaanMasterAssetDetail detail : details) {
				try {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					penerimaanPengadaanMasterAssetHelper.initRow(row, detail);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
			return;
		}

		int dibuat = 0;
		try {
			JSONArray array = new JSONArray(reimb.getFormula() == null || reimb.getFormula().trim().isEmpty() ? "[]"
					: reimb.getFormula());
			for (int i = 0; i < array.length(); i++) {
				JSONObject o = array.optJSONObject(i);
				if (o == null || o.length() == 0) {
					continue;
				}
				long masterAssetId = o.optLong("masterAsset", 0);
				if (masterAssetId <= 0) {
					continue; // baris biaya murni — bukan barang
				}
				ais.database.model.asset.MasterAsset masterAsset = (ais.database.model.asset.MasterAsset) session
						.get(ais.database.model.asset.MasterAsset.class, Long.valueOf(masterAssetId));
				if (masterAsset == null) {
					continue;
				}

				PenerimaanPengadaanMasterAssetDetail detail = new PenerimaanPengadaanMasterAssetDetail();
				detail.setMasterAsset(masterAsset);
				double qty = o.optDouble("qty", 1.0);
				detail.setJumlah(Double.valueOf(qty));
				detail.setDiterima(Double.valueOf(qty));
				detail.setHargaBeli(Double.valueOf(o.optDouble("harga", 0.0)));
				detail.setKeterangan(o.optString("nama", "") + " (Reimbursement " + reimb.getKode() + ")");

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				penerimaanPengadaanMasterAssetHelper.initRow(row, detail);
				dibuat++;
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (dibuat == 0) {
			try {
				MyMessageboxConfig.show(
						"Tidak ada baris rincian reimbursement yang dipetakan ke Barang (Master Asset). "
								+ "Langkah yang dapat dilakukan: (1) buka pengajuan reimbursement lalu pilih Barang (Asset) pada baris item yang berupa barang; "
								+ "(2) atau tambahkan baris barang secara manual dengan mencentang 'Tanpa Pemesanan'.",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) generateDetailReimbursement-info");
			}
		}
	}

	public boolean onSave(Event event) throws Exception {

		if (workspace.getAttribute("workspace") == null && !tanpaAnggaran.isChecked() && tampaPemesanan.isChecked()) {
			MyMessageboxConfig.show("Mohon maaf, Anggaran belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih Anggaran dan pilih anggaran yang sesuai dari daftar; (2) Jika tidak ada anggaran, centang opsi 'Tanpa Anggaran'; (3) Jika memakai pemesanan (PO/UM), hapus centang 'Tanpa Pemesanan'. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Penerimaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode Penerimaan atau gunakan tombol generate kode; (2) Pastikan kode bersifat unik dan belum digunakan pada penerimaan lain; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (jenisPenerimaanBarang.getSelectedItem() == null
				|| jenisPenerimaanBarang.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Penerimaan Barang/Jasa belum dipilih. Langkah yang dapat dilakukan: (1) Pilih jenis penerimaan dari dropdown yang tersedia; (2) Pastikan jenis penerimaan sudah dikonfigurasi di menu Jenis Penerimaan Barang; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		UangMuka uangMukaD = (UangMuka) uangMuka.getAttribute("uangMuka");
		ais.database.model.akunting.ReimbursementPegawai reimbursementD = (ais.database.model.akunting.ReimbursementPegawai) reimbursement
				.getAttribute("reimbursementPegawai");
		PemesananPengadaanMasterAsset mypemesananPengadaanMasterAsset = (PemesananPengadaanMasterAsset) pemesananPengadaanMasterAsset
				.getAttribute("pemesananPengadaanMasterAsset");

		if (!tampaPemesanan.isChecked()) {
			if (mypemesananPengadaanMasterAsset == null && uangMukaD == null && reimbursementD == null) {
				MyMessageboxConfig.show("Mohon maaf, Pemesanan Pengadaan (PO) atau Uang Muka (UM) belum dipilih. Langkah yang dapat dilakukan: (1) Pilih nomor PO dari daftar pemesanan yang sudah disetujui; (2) Atau pilih Uang Muka yang relevan jika penerimaan berdasarkan UM; (3) Jika tanpa PO/UM, centang opsi 'Tanpa Pemesanan'. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		if (rowTermin.isVisible()) {

			if (kodeTermin.getSelectedItem() == null || kodeTermin.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Mohon maaf, Termin belum dipilih. Langkah yang dapat dilakukan: (1) Pilih termin penerimaan dari dropdown Termin; (2) Pastikan termin sudah didaftarkan pada pemesanan terkait; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}

		}

		if (keterangan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Keterangan Penerimaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Keterangan dengan deskripsi penerimaan barang/jasa; (2) Keterangan wajib diisi untuk keperluan dokumentasi dan audit; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		List<Row> rowsMasterAsset = gridMasterAsset.getRows().getChildren();
		for (Row row : rowsMasterAsset) {
			PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = (PenerimaanPengadaanMasterAssetDetail) row
					.getAttribute("penerimaanPengadaanMasterAssetDetail");
			if (penerimaanPengadaanMasterAssetDetail.getMasterAsset() == null) {
				MyMessageboxConfig.show("Mohon maaf, Data Barang pada daftar penerimaan belum lengkap. Langkah yang dapat dilakukan: (1) Klik tombol pilih barang pada baris yang masih kosong; (2) Cari dan pilih barang/jasa yang diterima dari daftar; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (penerimaanPengadaanMasterAsset.getId() != null) {
			penerimaanPengadaanMasterAsset = (PenerimaanPengadaanMasterAsset) session
					.load(PenerimaanPengadaanMasterAsset.class, penerimaanPengadaanMasterAsset.getId());

		}

		if (disposisiSop != null && disposisiSop.getId() != null) {
			penerimaanPengadaanMasterAsset.setDisposisiSop(disposisiSop);
		}

		penerimaanPengadaanMasterAsset
				.setJenisPenerimaanBarang((JenisPenerimaanBarang) jenisPenerimaanBarang.getSelectedItem().getValue());
		penerimaanPengadaanMasterAsset.setKode(kode.getValue());
		penerimaanPengadaanMasterAsset.setKeterangan(keterangan.getValue());
		penerimaanPengadaanMasterAsset.setTanggalPembuatan(tanggalPembuatan.getValue());
		penerimaanPengadaanMasterAsset.setPemesananPengadaanMasterAsset(mypemesananPengadaanMasterAsset);
		penerimaanPengadaanMasterAsset.setPenyedia(
				mypemesananPengadaanMasterAsset == null ? null : mypemesananPengadaanMasterAsset.getPenyedia());

		penerimaanPengadaanMasterAsset.setLokasi(
				(Lokasi) (lokasi.getSelectedItem() == null || lokasi.getSelectedItem().getValue() == null ? null
						: lokasi.getSelectedItem().getValue()));
		penerimaanPengadaanMasterAsset.setPemilikAsset((PemilikAsset) (pemilikAsset.getSelectedItem() == null
				|| pemilikAsset.getSelectedItem().getValue() == null ? null
						: pemilikAsset.getSelectedItem().getValue()));
		penerimaanPengadaanMasterAsset.setRuang((Ruang) ruang.getAttribute("ruang"));
		penerimaanPengadaanMasterAsset.setTampaPemesanan(tampaPemesanan.isChecked());

		penerimaanPengadaanMasterAsset.setKurir(kurir.getValue().trim());

		Workspace work = (Workspace) workspace.getAttribute("workspace");

		penerimaanPengadaanMasterAsset.setWorkspace(work);
		penerimaanPengadaanMasterAsset.setTanpaAnggaran(tanpaAnggaran.isChecked());

		penerimaanPengadaanMasterAsset.setKodeTermin(
				(String) (kodeTermin.getSelectedItem() == null ? null : kodeTermin.getSelectedItem().getValue()));

		penerimaanPengadaanMasterAsset.setKeteranganTermin((String) (kodeTermin.getSelectedItem() == null ? null
				: kodeTermin.getSelectedItem().getLabel() + " " + kodeTermin.getSelectedItem().getDescription()));

		penerimaanPengadaanMasterAsset.setJsonTermin(
				(kodeTermin.getSelectedItem() == null || kodeTermin.getSelectedItem().getAttribute("value") == null
						? null
						: kodeTermin.getSelectedItem().getAttribute("value") + ""));
		penerimaanPengadaanMasterAsset.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());

		penerimaanPengadaanMasterAsset.setUangMuka(uangMukaD);
		penerimaanPengadaanMasterAsset.setReimbursementPegawai(reimbursementD);

		if (penerimaanPengadaanMasterAsset.getId() != null) {
			session.update(penerimaanPengadaanMasterAsset);
		} else {
			penerimaanPengadaanMasterAsset.setDibuatOleh(Common.getCurrentUser());
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			penerimaanPengadaanMasterAsset.setKode(kode.getValue());
			session.save(penerimaanPengadaanMasterAsset);

		}

		Double nilai = 0.0;
		for (Row row : rowsMasterAsset) {
			try {
				PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = (PenerimaanPengadaanMasterAssetDetail) row
						.getAttribute("penerimaanPengadaanMasterAssetDetail");
				penerimaanPengadaanMasterAssetDetail.setPenerimaanPengadaanMasterAsset(penerimaanPengadaanMasterAsset);
				// Pakai refreshUpdate (bukan saveOrUpdate+flush langsung): bila baris detail sudah
				// stale/terhapus di DB, flush langsung melempar StaleStateException "unexpected row
				// count [0]" yang meracuni transaksi & menggagalkan sisa penyimpanan. refreshUpdate
				// menangani kasus stale/transient secara terpusat (evict/clear lalu lewati) sehingga
				// baris lain tetap tersimpan.
				ais.common.Common.refreshUpdate(session, penerimaanPengadaanMasterAssetDetail);

				Double total = penerimaanPengadaanMasterAssetDetail.getHargaTotal();

				nilai += total;

				PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail = penerimaanPengadaanMasterAssetDetail
						.getPemesananPengadaanMasterAssetDetail();
				if (pemesananPengadaanMasterAssetDetail != null) {
					session.refresh(pemesananPengadaanMasterAssetDetail);
					pemesananPengadaanMasterAssetDetail
							.setPenerimaanPengadaanMasterAssetDetail(penerimaanPengadaanMasterAssetDetail);
					session.saveOrUpdate(pemesananPengadaanMasterAssetDetail);
					session.flush();
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}
		penerimaanPengadaanMasterAsset.setNilai(nilai);
		session.update(penerimaanPengadaanMasterAsset);

		if (uangMukaD != null) {
			session.refresh(uangMukaD);
			uangMukaD.setPenerimaanPengadaanMasterAsset(penerimaanPengadaanMasterAsset);
			Common.refreshUpdate(session, uangMukaD);
		}

		// Tautkan balik reimbursement -> BAST (klon pola uang muka) sehingga
		// reimbursement yang sudah diterima tidak muncul lagi di picker.
		if (reimbursementD != null) {
			session.refresh(reimbursementD);
			reimbursementD.setPenerimaanPengadaanMasterAsset(penerimaanPengadaanMasterAsset);
			Common.refreshUpdate(session, reimbursementD);
		}

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {

				if (setujui.isChecked()) {
					penerimaanPengadaanMasterAsset.setDisetujuiOleh(tbmuser);
					penerimaanPengadaanMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
					Common.refreshUpdate(penerimaanPengadaanMasterAsset);
				} else {
					penerimaanPengadaanMasterAsset.setDisetujuiOleh(null);
					penerimaanPengadaanMasterAsset.setTanggalPersetujuan(null);
					Common.refreshUpdate(penerimaanPengadaanMasterAsset);
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						cetak(penerimaanPengadaanMasterAsset);
					}
				}, "Proses cetak", false, 2500);

			}
		});

		return true;
	}

	/**
	 * Membangun Map parameter yang digunakan sebagai data untuk generate laporan PDF penerimaan.
	 *
	 * <b>Tujuan:</b><br>
	 * Menyiapkan semua data yang diperlukan oleh template JasperReports ({@code asset/penerimaan_pengadaan})
	 * dalam bentuk {@link Map}, termasuk data header penerimaan, detail item, informasi bank penyedia,
	 * dokumen penyedia, informasi SOP, dan total nilai transaksi.
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Me-refresh objek dari database untuk memastikan data paling mutakhir.</li>
	 *   <li>Menggunakan {@link ais.common.HashMapGenerator#getRand()} untuk membuat Map dengan key unik.</li>
	 *   <li>Memasukkan ID dan seluruh properti objek via {@link Common#insertProperty}.</li>
	 *   <li>Menginisialisasi kop surat berdasarkan Satuan Kerja terkait via {@link SuratUtil#initDefaultKop}.</li>
	 *   <li>Jika ada penyedia, mengekstrak informasi bank dari JSON dan memasukkan ke Map dengan prefix
	 *       {@code bank_i.key}, serta mengambil dokumen penyedia dari tabel relasi.</li>
	 *   <li>Memasukkan parameter alur SOP via {@link DisposisiAlurSop#parameterMap}.</li>
	 *   <li>Mengquery semua detail item, membangun List of Map dengan informasi lengkap per item
	 *       (nama, harga, jumlah diterima, status persetujuan, dll.), dan menghitung total semua.</li>
	 *   <li>Memasukkan list detail dan total ke Map utama.</li>
	 *   <li>Menghapus entri yang mengandung "disposisiSop" dari Map untuk menghindari error serialisasi.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param penerimaanPengadaanMasterAsset entitas penerimaan yang akan di-generate laporannya;
	 *                                       harus memiliki ID yang valid untuk refresh database.
	 * @return {@link Map} berisi semua parameter laporan siap pakai oleh JasperReports.
	 *
	 * <b>Penanganan error:</b><br>
	 * Parsing JSON bank penyedia dilakukan dalam try-catch; jika JSON tidak valid, bagian bank
	 * dilewati tanpa error. NullPointerException pada item detail juga dicegah dengan pemeriksaan
	 * null eksplisit.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika template JasperReports diperbarui dengan field baru, tambahkan key yang sesuai ke Map
	 * di metode ini. Pastikan penamaan key konsisten antara kode Java dan template {@code .jrxml}.
	 * Metode ini bersifat {@code static} sehingga dapat dipanggil dari kelas lain tanpa instance.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map parameter(PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset) {
		if (penerimaanPengadaanMasterAsset != null && penerimaanPengadaanMasterAsset.getId() != null) {
			// Baris penerimaan bisa jadi sudah TIDAK ada di DB (mis. terhapus, atau simpan-nya
			// ter-rollback) sehingga refresh() melempar UnresolvableObjectException "No row with
			// the given identifier exists". Untuk cetak, cukup lanjut memakai objek in-memory agar
			// proses tidak gagal total.
			try {
				HibernateUtil.currentSession().refresh(penerimaanPengadaanMasterAsset);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PenerimaanPengadaanMasterAssetAction.java:1722");
				// diabaikan sengaja: pakai data objek yang ada
			}
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", penerimaanPengadaanMasterAsset.getId());

		Common.insertProperty(PenerimaanPengadaanMasterAsset.class, penerimaanPengadaanMasterAsset, parameters, "data");

		SatuanKerja satuanKerja = penerimaanPengadaanMasterAsset.getSatuanKerja();
		SuratUtil.initDefaultKop(parameters, satuanKerja);

		if (penerimaanPengadaanMasterAsset.getPenyedia() != null) {
			try {

				JSONArray array = new JSONArray(penerimaanPengadaanMasterAsset.getPenyedia().getBank());
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);
					Iterator<String> iter = jsonObject.keys();
					while (iter.hasNext()) {
						String key = iter.next();
						parameters.put("bank_" + i + "." + key, jsonObject.get(key));
					}
				}

			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PenerimaanPengadaanMasterAssetAction.java:1748");
				// TODO: handle exception
			}
			Map<Long, DokumenPenyediaAsset> map = ConstantValues.ambilBerdasarClass(DokumenPenyediaAsset.class);
			List<DokumenPenyediaAsset> dokumenPenyediaAssets = new ArrayList<DokumenPenyediaAsset>();
			for (DokumenPenyediaAsset dokumenPenyediaAsset : map.values()) {
				dokumenPenyediaAssets.add(dokumenPenyediaAsset);
			}
			PenyediaAsset penyediaAsset = penerimaanPengadaanMasterAsset.getPenyedia();
			Session session = HibernateUtil.currentSession();
			for (DokumenPenyediaAsset dokumenPenyediaAsset : dokumenPenyediaAssets) {

				PenyediaAssetPunyaDokumen temp = (PenyediaAssetPunyaDokumen) (penyediaAsset == null
						|| penyediaAsset.getId() == null
								? new PenyediaAssetPunyaDokumen()
								: session.createCriteria(PenyediaAssetPunyaDokumen.class)
										.add(Restrictions.eq("dokumenPenyediaAsset", dokumenPenyediaAsset))
										.add(Restrictions.eq("penyediaAsset", penyediaAsset)).setMaxResults(1)
										.uniqueResult());

				parameters.put("dokumen." + dokumenPenyediaAsset.getNama(), temp == null ? "" : temp.getKeterangan());
			}

		}

		DisposisiAlurSop.parameterMap(penerimaanPengadaanMasterAsset.getDisposisiSop(), parameters);

		Session session = HibernateUtil.currentSession();

		List<PenerimaanPengadaanMasterAssetDetail> penerimaanPengadaanMasterAssetDetails = session
				.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
				.add(Restrictions.eq("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset)).list();
		List<Map> maps = new ArrayList<Map>();
		Double totalSemua = 0.0;
		for (PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail : penerimaanPengadaanMasterAssetDetails) {
			Map map = new HashMap();
			Common.insertProperty(PenerimaanPengadaanMasterAssetDetail.class, penerimaanPengadaanMasterAssetDetail, map,
					"data");
			totalSemua += penerimaanPengadaanMasterAssetDetail.getHargaTotal();
			map.put("hargatotal", penerimaanPengadaanMasterAssetDetail.getHargaTotal());
			map.put("pph", penerimaanPengadaanMasterAssetDetail.getPersenPph());
			map.put("ppn", penerimaanPengadaanMasterAssetDetail.getPersenPpn());

			map.put("hargapotongan", penerimaanPengadaanMasterAssetDetail.getHargaPotongan());
			map.put("hargabeli", penerimaanPengadaanMasterAssetDetail.getHargaBeli());
			map.put("diterima", penerimaanPengadaanMasterAssetDetail.getDiterima());
			map.put("jumlah", penerimaanPengadaanMasterAssetDetail.getJumlah());
			map.put("nama", penerimaanPengadaanMasterAssetDetail.getMasterAsset().getNama());
			map.put("kode", penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getKode());
			map.put("isbn", penerimaanPengadaanMasterAssetDetail.getMasterAsset().getKode());

			map.put("penyedia",
					penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getPenyedia() == null ? ""
							: penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getPenyedia()
									.getNama());

			String status = "";
			if (penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getDisetujuiOleh() == null) {
				status = "Belum disetujui";
			} else {
				status = "Disetujui oleh "
						+ penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getDisetujuiOleh()
								.getUserNama()
						+ " pada "
						+ (penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
								.getTanggalPersetujuan() == null ? ""
										: Common.dateFormat51.get().format(penerimaanPengadaanMasterAssetDetail
												.getPenerimaanPengadaanMasterAsset().getTanggalPersetujuan()));
			}

			map.put("status_persetujuan", status);

			map.put("perpustakaan",
					penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getKeterangan());
			map.put("tanggal_persetujuan",
					penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getTanggalPersetujuan());
			map.put("disetujui_oleh",
					penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getDisetujuiOleh() == null
							? ""
							: penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
									.getDisetujuiOleh().getUserNama());

			maps.add(map);
		}

		parameters.put("maps", maps);
		parameters.put("totalSemua", totalSemua);

		for (Object o : parameters.keySet()) {
			if (o.toString().contains("disposisiSop")) {
				parameters.put(o.toString(), null);
			}
		}
		return parameters;
	}

	/**
	 * Menghasilkan file PDF laporan penerimaan pengadaan untuk diunduh atau ditampilkan.
	 *
	 * <b>Tujuan:</b><br>
	 * Implementasi metode dari antarmuka atau base class yang mengonversi data penerimaan pengadaan
	 * menjadi file PDF menggunakan mesin laporan JasperReports. File ini dapat diunduh oleh pengguna
	 * atau ditampilkan di browser sebagai lampiran.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menerima {@link GeneralValueObject} yang di-cast ke {@link PenerimaanPengadaanMasterAsset},
	 * kemudian memanggil {@link #parameter} untuk membangun Map data laporan, dan selanjutnya
	 * memanggil {@link Report#generateFileReport} dengan template {@code asset/penerimaan_pengadaan}
	 * untuk menghasilkan file PDF. Tanggal pembuatan dokumen digunakan untuk resolusi template
	 * yang mungkin berbeda per periode.
	 *
	 * <b>Parameter:</b><br>
	 * @param generalValueObject objek domain {@link PenerimaanPengadaanMasterAsset} yang di-wrap
	 *                           dalam tipe generik {@link GeneralValueObject}.
	 * @return {@link File} objek yang merepresentasikan file PDF yang sudah digenerate.
	 * @throws Exception jika terjadi error saat generate laporan (template tidak ditemukan, data kosong, dll.).
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari {@link Report#generateFileReport} akan merambat ke pemanggil. Pastikan template
	 * {@code asset/penerimaan_pengadaan.jrxml} sudah terdeploy di direktori laporan.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika nama template laporan berubah, perbarui string {@code "asset/penerimaan_pengadaan"} di sini
	 * dan pastikan file JRXML baru sudah tersedia di direktori yang benar.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset = (PenerimaanPengadaanMasterAsset) generalValueObject;
		List maps = null;
		File file = Report.generateFileReport(Report.PDF, parameter(penerimaanPengadaanMasterAsset),
				"asset/penerimaan_pengadaan", penerimaanPengadaanMasterAsset.getTanggalPembuatan(), maps,
				Common.locale);
		return file;
	}

	/**
	 * Memicu pencetakan laporan PDF penerimaan pengadaan secara asinkron melalui timer.
	 *
	 * <b>Tujuan:</b><br>
	 * Menampilkan PDF laporan BAST (Berita Acara Serah Terima) di browser pengguna setelah operasi
	 * penyimpanan atau persetujuan selesai. Pencetakan dilakukan secara asinkron agar tidak memblokir
	 * thread UI.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat timer default ZKoss yang akan mengeksekusi {@link Report#generatePDFReport} pada
	 * event berikutnya. Metode {@link #parameter} dipanggil untuk menyiapkan data laporan, lalu
	 * hasilnya dikirim ke browser sebagai popup PDF. Penggunaan timer memastikan state UI sudah
	 * terstabilkan sebelum proses generate laporan yang bisa memakan waktu dimulai.
	 *
	 * <b>Parameter:</b><br>
	 * @param penerimaanPengadaanMasterAsset entitas penerimaan yang akan dicetak laporannya.
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari {@link Report#generatePDFReport} akan merambat ke ZKoss error handler global.
	 * Tidak ada try-catch internal.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika pencetakan perlu dikondisikan (misalnya berdasarkan konfigurasi), tambahkan pengecekan
	 * sebelum {@link Common#createDefaultTimer}. Metode ini bersifat {@code private} dan hanya
	 * dipanggil dari dalam kelas ini.
	 */
	private void cetak(final PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset) {
		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {

				Report.generatePDFReport(Report.PDF, parameter(penerimaanPengadaanMasterAsset),
						"asset/penerimaan_pengadaan", penerimaanPengadaanMasterAsset.getTanggalPembuatan());
			}
		});
	}

	private Checkbox searchaktif;
	/** Filter status persetujuan: Semua / Belum Disetujui / Telah Disetujui. */
	private Combobox searchStatusPersetujuan;
	private MyDatebox tanggalPersetujuanManual;
	private AmbilDataUangMukaBanbox uangMuka;
	private ais.action.master.akunting.helper.AmbilDataReimbursementBanbox reimbursement;

	/**
	 * Membangun objek {@link Criteria} Hibernate untuk query daftar penerimaan pengadaan sesuai filter aktif.
	 *
	 * <b>Tujuan:</b><br>
	 * Metode ini adalah sumber tunggal (single source of truth) untuk semua query daftar penerimaan
	 * pengadaan. Ia menggabungkan semua filter yang aktif di UI (satuan kerja, rentang tanggal,
	 * status aktif, penyedia, lokasi, jenis penerimaan, kode, keterangan termin, kode PO, keterangan,
	 * dan status persetujuan) menjadi satu Criteria yang konsisten.
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Mengambil Satuan Kerja yang dipilih dan menghitung set satuan kerja yang relevan
	 *       (termasuk anak-anaknya dalam hierarki) via {@link SatuanKerjaTreeModel#getChildsSet}.</li>
	 *   <li>Membuat Criteria pada {@link PenerimaanPengadaanMasterAsset} dengan filter:
	 *     <ul>
	 *       <li>Status aktif (jika checkbox aktif dicentang, filter aktif=true atau null).</li>
	 *       <li>Rentang tanggal pembuatan antara nilai {@code start} dan {@code end}.</li>
	 *       <li>LEFT JOIN ke pemesananPengadaanMasterAsset untuk filter kode PO dan keterangan PO.</li>
	 *       <li>Status persetujuan (Semua/Belum Disetujui/Telah Disetujui) via combobox.</li>
	 *       <li>Filter satuan kerja (null atau dalam set yang relevan).</li>
	 *       <li>Filter penyedia dari banbox pencarian.</li>
	 *       <li>Filter lokasi dari combobox.</li>
	 *       <li>Filter jenis penerimaan barang dari combobox.</li>
	 *       <li>Filter ILIKE pada kode, keteranganTermin, kode PO, dan keterangan.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Menambahkan ORDER BY id DESC jika parameter {@code order} adalah {@code true}.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param order {@code true} untuk menambahkan ORDER BY id DESC pada query; {@code false} untuk
	 *              query tanpa urutan (biasanya digunakan untuk hitung total record paging).
	 * @return {@link Criteria} yang sudah terkonfigurasi dengan semua filter aktif; atau {@code null}
	 *         jika {@code searchparent} belum diinisialisasi.
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika {@code searchparent} null (saat dipanggil sebelum UI siap), metode mengembalikan null.
	 * Pemanggil harus memeriksa nilai return sebelum menggunakan. Filter yang null-safe menggunakan
	 * {@code Restrictions.sqlRestriction("1=1")} atau {@code Restrictions.sqlRestriction("true")}
	 * sebagai fallback.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Setiap filter baru di UI harus memiliki entri baru di sini. Pastikan alias LEFT_JOIN pada
	 * {@code pemesananPengadaanMasterAsset} hanya didefinisikan sekali. Perubahan nama kolom database
	 * harus diperbarui di semua {@code sqlRestriction} dan {@code ilike} yang menggunakannya.
	 */
	public Criteria initCriteria(boolean order) {
		if (searchparent == null) return null;

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenerimaanPengadaanMasterAsset.class)

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.createAlias("pemesananPengadaanMasterAsset", "pemesananPengadaanMasterAsset", Criteria.LEFT_JOIN)

				// Filter status persetujuan via combobox (Semua / Belum Disetujui / Telah Disetujui).
				.add(searchStatusPersetujuan == null || searchStatusPersetujuan.getSelectedItem() == null
						|| searchStatusPersetujuan.getSelectedItem().getLabel() == null
						|| "Semua".equalsIgnoreCase(searchStatusPersetujuan.getSelectedItem().getLabel().trim())
								? Restrictions.sqlRestriction("1=1")
								: "Belum Disetujui".equalsIgnoreCase(
										searchStatusPersetujuan.getSelectedItem().getLabel().trim())
												? Restrictions.isNull("disetujuiOleh")
												: Restrictions.isNotNull("disetujuiOleh"))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add((searchPenyedia == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchPenyedia.getAttribute("penyediaAsset") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("penyedia", searchPenyedia.getAttribute("penyediaAsset"))))

				.add(searchlokasi.getSelectedItem() == null || searchlokasi.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))

				.add(searchJenisPenerimaanBarang.getSelectedItem() == null
						|| searchJenisPenerimaanBarang.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisPenerimaanBarang",
										searchJenisPenerimaanBarang.getSelectedItem().getValue()))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchketerangantermin.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keteranganTermin", searchketerangantermin.getValue().trim(),
								MatchMode.ANYWHERE))

				.add(searchkodepo.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("pemesananPengadaanMasterAsset.kode", searchkodepo.getValue().trim(),
								MatchMode.ANYWHERE))

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("keterangan", searchketerangan.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("pemesananPengadaanMasterAsset.keterangan",
										searchketerangan.getValue().trim(), MatchMode.ANYWHERE)));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * Menangani event pencarian dan memperbarui tampilan grid daftar penerimaan pengadaan.
	 *
	 * <b>Tujuan:</b><br>
	 * Metode ini adalah handler utama untuk semua operasi refresh data: pencarian berdasarkan filter,
	 * navigasi halaman (paging), dan refresh otomatis via timer. Ia memastikan grid selalu menampilkan
	 * data terkini sesuai filter yang aktif.
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@link Common#initPaging} dengan Criteria tanpa order untuk menghitung total
	 *       record dan memperbarui komponen paging.</li>
	 *   <li>Memanggil {@link #initCriteria} dengan order=true, membatasi hasil ke
	 *       {@link Common#ROWS_COUNT_ON_PAGE} baris mulai dari offset sesuai halaman aktif.</li>
	 *   <li>Membuat {@link SimpleListModel} dari hasil query dan menset ke grid dengan renderer
	 *       {@link PenerimaanPengadaanMasterAssetRenderer}.</li>
	 *   <li>Memanggil {@code grid.setModelCheckMobile} untuk mendukung tampilan mobile responsif.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param event event ZKoss yang memicu pencarian; bisa null jika dipanggil secara programatik.
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari query Hibernate akan merambat ke ZKoss error handler. Jika {@link #initCriteria}
	 * mengembalikan null (searchparent belum siap), akan terjadi NullPointerException.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika ada perubahan jumlah baris per halaman, ubah konstanta {@link Common#ROWS_COUNT_ON_PAGE}.
	 * Jika perlu menambahkan sort kolom dari header grid, tambahkan logika pengurutan sebelum
	 * panggilan ke {@code initCriteria}.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PenerimaanPengadaanMasterAsset> penerimaanPengadaanMasterAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penerimaanPengadaanMasterAsset);
		grid.setRowRenderer(new PenerimaanPengadaanMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun dan mengembalikan formulir lengkap penerimaan pengadaan sebagai komponen {@link MyGrid}.
	 *
	 * <b>Tujuan:</b><br>
	 * Metode ini adalah factory utama untuk UI formulir penerimaan. Ia membangun seluruh formulir
	 * secara programatik — mulai dari informasi pemesanan/uang muka, kode dan tanggal penerimaan,
	 * jenis penerimaan, lokasi/pemilik/ruang, keterangan, termin, nama kurir, hingga grid detail
	 * item barang/jasa — dan mengintegrasikannya dengan event listener untuk interaktivitas.
	 * Ini juga merupakan implementasi dari antarmuka {@link FormSop}.
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Inisialisasi model satuan kerja jika belum ada.</li>
	 *   <li>Menyimpan objek penerimaan dan disposisi SOP ke state instance.</li>
	 *   <li>Jika sudah disetujui, mengaktifkan mode {@code persetujuan} (readonly).</li>
	 *   <li>Membuat {@link MyGrid} 2-kolom (label | input) sebagai kontainer formulir.</li>
	 *   <li>Menampilkan kode anggaran dari permintaan pengadaan terkait jika ada.</li>
	 *   <li>Membangun baris formulir satu per satu: Pemesanan PO, Uang Muka, mode tanpa pemesanan,
	 *       Anggaran, tanpa anggaran, Kode Penerimaan, Penyedia, Jenis Penerimaan, Tanggal,
	 *       Pemilik, Lokasi, Ruang, Keterangan, Termin, Nama Kurir.</li>
	 *   <li>Menambahkan grid detail item via {@link PenerimaanPengadaanMasterAssetHelper#initDetail}.</li>
	 *   <li>Mendaftarkan event listener pada pemilihan PO dan Uang Muka untuk mengisi detail otomatis.</li>
	 *   <li>Mendaftarkan event listener pada pemilihan termin untuk mengisi dokumen dan progres termin.</li>
	 *   <li>Menambahkan baris Status Penerimaan (checkbox setujui) dan Tanggal Persetujuan manual.</li>
	 *   <li>Mendaftarkan eventListenerSetuju di atribut grid untuk komunikasi dengan komponen eksternal.</li>
	 *   <li>Memicu event listener secara programatik melalui {@link Common#createDefaultTimer} untuk
	 *       mengisi data awal saat formulir pertama kali dibuka.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param generalValueObject  objek {@link PenerimaanPengadaanMasterAsset} yang akan diedit.
	 * @param disposisiSop        objek alur SOP yang terkait; null jika tidak ada alur SOP aktif.
	 * @param save                tombol simpan yang akan ditambahkan ke toolbar; tidak di-attach di sini
	 *                            (dikelola oleh {@link #init}).
	 * @param setujuiData         event listener tambahan untuk checkbox setujui; null jika tidak ada.
	 * @return {@link MyGrid} yang berisi seluruh formulir siap untuk ditampilkan.
	 * @throws Exception jika terjadi error saat membangun komponen atau mengakses database.
	 *
	 * <b>Penanganan error:</b><br>
	 * Tidak ada try-catch internal; exception merambat ke pemanggil. Pastikan template ZUL dan
	 * helper terkait sudah terdeploy dengan benar.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Saat menambah field baru ke entitas penerimaan, tambahkan baris formulir baru di sini dan
	 * pastikan nilai field tersebut juga di-set di {@link #onSave}. Perhatikan urutan baris karena
	 * formulir ini tidak menggunakan binding; semua dikelola secara manual.
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}

		this.penerimaanPengadaanMasterAsset = (PenerimaanPengadaanMasterAsset) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;

		if (this.penerimaanPengadaanMasterAsset.getDisetujuiOleh() != null) {
			persetujuan = true;
		}

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		List<PermintaanPengadaanMasterAssetDetail> dataPermintaanPengadaanMasterAssetDetail = new ArrayList<PermintaanPengadaanMasterAssetDetail>();
		if (penerimaanPengadaanMasterAsset.getId() != null
				&& penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset() != null) {

			dataPermintaanPengadaanMasterAssetDetail = HibernateUtil.currentSession()
					.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
					.createAlias("permintaanPengadaanMasterAsset", "permintaanPengadaanMasterAsset")
					.add(Restrictions.isNotNull("permintaanPengadaanMasterAsset.workspace"))
					.createAlias("pemesananPengadaanMasterAssetDetail", "pemesananPengadaanMasterAssetDetail")
					.add(Restrictions.eq("pemesananPengadaanMasterAssetDetail.pemesananPengadaanMasterAsset",
							penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset()))
					.list();

			if (!dataPermintaanPengadaanMasterAssetDetail.isEmpty()) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Kode Anggaran"));
				Vbox unit = new Vbox();
				row.appendChild(unit);

				Set<Workspace> workspaces = new HashSet<Workspace>();
				for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : dataPermintaanPengadaanMasterAssetDetail) {
					if (permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
							.getWorkspace() != null) {
						workspaces.add(permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
								.getWorkspace());
					}
				}

				for (Workspace workspace : workspaces) {
					RevisiHelper.createNewRevisi(Workspace.class, workspace, workspace.toString()).setParent(unit);
				}
			}
		}

		final MyFormRow rowPemesanan = new MyFormRow();
		rowPemesanan.setParent(rows);
		rowPemesanan.appendChild(new ais.ui.util.MyLabelConfig("Pemesanan Pengadaan *"));

		pemesananPengadaanMasterAsset = new AmbilDataPemesananPengadaanAsetBanbox();

		if (persetujuan) {
			rowPemesanan.appendChild(
					new Label(penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset() == null ? ""
							: penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset().toString()));
		} else {
			rowPemesanan.appendChild(pemesananPengadaanMasterAsset);
		}

		pemesananPengadaanMasterAsset.setWidth("90%");
		pemesananPengadaanMasterAsset.setAttribute("pemesananPengadaanMasterAsset",
				penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset());
		pemesananPengadaanMasterAsset
				.setValue(penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset() == null ? ""
						: penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset().toString());
		pemesananPengadaanMasterAsset.setReadonly(true);

		final MyFormRow rowUangMuka = new MyFormRow();
		rowUangMuka.setParent(rows);
		rowUangMuka.appendChild(new ais.ui.util.MyLabelConfig("Ambil dari uang muka"));

		uangMuka = new AmbilDataUangMukaBanbox(true);
		uangMuka.setAttribute("uangMuka", penerimaanPengadaanMasterAsset.getUangMuka());
		uangMuka.setValue(penerimaanPengadaanMasterAsset.getUangMuka() == null ? ""
				: penerimaanPengadaanMasterAsset.getUangMuka().getKode());

		uangMuka.setReadonly(true);
		uangMuka.setWidth("90%");

		if (persetujuan) {
			rowUangMuka.appendChild(new Label(penerimaanPengadaanMasterAsset.getUangMuka() == null ? ""
					: penerimaanPengadaanMasterAsset.getUangMuka().getKode() + "-"
							+ penerimaanPengadaanMasterAsset.getUangMuka().getNama()));
		} else {
			rowUangMuka.appendChild(uangMuka);
		}

		// BAST dari REIMBURSEMENT pegawai (klon pola uang muka): pilih reimbursement
		// DISETUJUI yang belum diterima; rincian barang diambil dari baris item
		// reimbursement yang dipetakan ke Barang (MasterAsset).
		final MyFormRow rowReimbursement = new MyFormRow();
		rowReimbursement.setParent(rows);
		rowReimbursement.appendChild(new ais.ui.util.MyLabelConfig("Ambil dari reimbursement"));

		reimbursement = new ais.action.master.akunting.helper.AmbilDataReimbursementBanbox();
		reimbursement.setAttribute("reimbursementPegawai", penerimaanPengadaanMasterAsset.getReimbursementPegawai());
		reimbursement.setValue(penerimaanPengadaanMasterAsset.getReimbursementPegawai() == null ? ""
				: penerimaanPengadaanMasterAsset.getReimbursementPegawai().getKode());
		reimbursement.setReadonly(true);
		reimbursement.setWidth("90%");

		if (persetujuan) {
			rowReimbursement.appendChild(new Label(penerimaanPengadaanMasterAsset.getReimbursementPegawai() == null
					? ""
					: penerimaanPengadaanMasterAsset.getReimbursementPegawai().getKode()));
		} else {
			rowReimbursement.appendChild(reimbursement);
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setVisible(Common.bolehKonfigurasi("tampilkan_penerimaan_langsung_di_po"));
		row.setParent(rows);


		tampaPemesanan = new MyCheckboxConfig("Merupakan penerimaan langsung (tanpa ada pemesanan)");
		if (persetujuan) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Merupakan penerimaan langsung (tanpa ada pemesanan)"));
			row.appendChild(new Label(penerimaanPengadaanMasterAsset.getTampaPemesanan() ? "Ya" : "Tidak"));
		} else {
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(tampaPemesanan);
		}

		tampaPemesanan.setChecked(penerimaanPengadaanMasterAsset.getTampaPemesanan());

		workspace = new AmbilDataWorkspaceBanbox(false);
		final MyFormRow rowAnggaran = new MyFormRow();
		rowAnggaran.setParent(rows);
		rowAnggaran.appendChild(new ais.ui.util.MyLabelConfig("Anggaran *"));
		workspace.setValue(penerimaanPengadaanMasterAsset.getWorkspace() == null ? ""
				: penerimaanPengadaanMasterAsset.getWorkspace().toString());
		workspace.setAttribute("workspace", penerimaanPengadaanMasterAsset.getWorkspace());
		workspace.setWidth("90%");
		workspace.setReadonly(true);

		if (persetujuan) {
			rowAnggaran.appendChild(new Label(penerimaanPengadaanMasterAsset.getWorkspace() == null ? ""
					: penerimaanPengadaanMasterAsset.getWorkspace().toString()));
		} else {
			rowAnggaran.appendChild(workspace);
		}

		final MyFormRow rowtanpaAnggaran = new MyFormRow();
		rowtanpaAnggaran.setParent(rows);
		rowtanpaAnggaran.appendChild(new ais.ui.util.MyLabelConfig(""));
		tanpaAnggaran = new MyCheckboxConfig("Merupakan tanpa anggaran");
		if (persetujuan) {
			rowtanpaAnggaran.appendChild(new Label("Merupakan tanpa anggaran ? "
					+ (penerimaanPengadaanMasterAsset.getTanpaAnggaran() ? "Ya" : "Tidak")));
		} else {
			rowtanpaAnggaran.appendChild(tanpaAnggaran);
		}

		tanpaAnggaran.setChecked(penerimaanPengadaanMasterAsset.getTanpaAnggaran());

		row = new MyFormRow();

		final EventListener eventListenerPesanan = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				UangMuka uangMukaD = (UangMuka) uangMuka.getAttribute("uangMuka");
				ais.database.model.akunting.ReimbursementPegawai reimbD = (ais.database.model.akunting.ReimbursementPegawai) reimbursement
						.getAttribute("reimbursementPegawai");

				rowPemesanan.setVisible(!tampaPemesanan.isChecked() && uangMukaD == null && reimbD == null);
				rowtanpaAnggaran.setVisible(Common.bolehKonfigurasi("tampilkan_tanpa_anggaran") && tampaPemesanan.isChecked() && uangMukaD == null && reimbD == null);
				rowAnggaran.setVisible(tampaPemesanan.isChecked() && !tanpaAnggaran.isChecked() && uangMukaD == null && reimbD == null);

				rowUangMuka.setVisible(
						pemesananPengadaanMasterAsset.getAttribute("pemesananPengadaanMasterAsset") == null
								&& reimbD == null);
				rowReimbursement.setVisible(
						pemesananPengadaanMasterAsset.getAttribute("pemesananPengadaanMasterAsset") == null
								&& uangMukaD == null);
			}

		};

		tampaPemesanan.addEventListener("onClick", eventListenerPesanan);
		tanpaAnggaran.addEventListener("onClick", eventListenerPesanan);

		eventListenerPesanan.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Penerimaan *"));

		if (penerimaanPengadaanMasterAsset.getKode() == null) {
			String noAgenda = generateCode(false);
			penerimaanPengadaanMasterAsset.setKode(noAgenda);
		}

		tanggalPembuatan = new MyDatebox(
				penerimaanPengadaanMasterAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: penerimaanPengadaanMasterAsset.getTanggalPembuatan());

		kode = new Label(penerimaanPengadaanMasterAsset.getKode());
		if (persetujuan) {
			row.appendChild(new Label(penerimaanPengadaanMasterAsset.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyedia"));

		penyedia = new Label(penerimaanPengadaanMasterAsset.getPenyedia() == null ? ""
				: penerimaanPengadaanMasterAsset.getPenyedia().getNama());

		row.appendChild(penyedia);

		jenisPenerimaanBarang = new Combobox();
		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();

		SatuanKerja parent = pt == null ? null : pt.getSatuanKerja();
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Common.insertCombo(jenisPenerimaanBarang, new String[] { "kode", "nama" }, "keterangan",
				JenisPenerimaanBarang.class,

				Restrictions.and(Restrictions.eq("aktif", true),
						Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										satuanKerjas.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(
														parent == null ? Restrictions.isNull("satuanKerja")
																: Restrictions.sqlRestriction("false"),
														Restrictions.in("satuanKerja", satuanKerjas)),
										Restrictions.eq("satuanKerja", tbmuser.ambilSatuanKerja())))));

		Common.selectComboItem(true, jenisPenerimaanBarang, penerimaanPengadaanMasterAsset.getJenisPenerimaanBarang());
		jenisPenerimaanBarang.setWidth("90%");
		jenisPenerimaanBarang.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Penerimaan Barang/Jasa *"));
		row.appendChild(jenisPenerimaanBarang);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Penerimaan"));

		tanggalPembuatan = new MyDatebox(
				penerimaanPengadaanMasterAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: penerimaanPengadaanMasterAsset.getTanggalPembuatan());

		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat51.get()
					.format(penerimaanPengadaanMasterAsset.getTanggalPembuatan() == null
							? ais.ui.util.WaktuUtil.getDate()
							: penerimaanPengadaanMasterAsset.getTanggalPembuatan())));
		} else {
			row.appendChild(tanggalPembuatan);
		}

		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(tampilkanRuanganDamPemilikAset);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pemilik"));

		pemilikAsset = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(penerimaanPengadaanMasterAsset.getPemilikAsset() == null ? ""
					: penerimaanPengadaanMasterAsset.getPemilikAsset().getNama()));
		} else {
			row.appendChild(pemilikAsset);
		}

		Common.insertComboDanSemua(pemilikAsset, new String[] { "nama", "id" }, "keterangan", PemilikAsset.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(pemilikAsset, penerimaanPengadaanMasterAsset.getPemilikAsset());
		pemilikAsset.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi"));

		lokasi = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(penerimaanPengadaanMasterAsset.getLokasi() == null ? ""
					: penerimaanPengadaanMasterAsset.getLokasi().getNama()));
		} else {
			row.appendChild(lokasi);
		}

		Common.insertComboDanSemua(lokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(lokasi, penerimaanPengadaanMasterAsset.getLokasi());
		lokasi.setWidth("90%");

		LokasiAction.kunciLokasi(lokasi);

		row = new MyFormRow();
		row.setVisible(tampilkanRuanganDamPemilikAset);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));
		ruang = new AmbilDataRuangBanbox();
		if (persetujuan) {
			row.appendChild(new Label(penerimaanPengadaanMasterAsset.getRuang() == null ? ""
					: penerimaanPengadaanMasterAsset.getRuang().getNama()));
		} else {
			row.appendChild(ruang);
		}
		ruang.setValue(penerimaanPengadaanMasterAsset.getRuang() == null ? ""
				: (penerimaanPengadaanMasterAsset.getRuang().getKodeRuangan()));
		ruang.setAttribute("ruang", penerimaanPengadaanMasterAsset.getRuang());
		ruang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Penerimaan Barang/Jasa*"));
		keterangan = new MyTextbox(penerimaanPengadaanMasterAsset.getKeterangan() == null ? ""
				: penerimaanPengadaanMasterAsset.getKeterangan());
		if (persetujuan) {
			row.appendChild(new Label(penerimaanPengadaanMasterAsset.getKeterangan() == null ? ""
					: penerimaanPengadaanMasterAsset.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		rowTermin = new MyFormRow();
		rowTermin.setVisible(false);
		rowTermin.setParent(rows);
		rowTermin.appendChild(new ais.ui.util.MyLabelConfig("Termin ke *"));

		kodeTermin = new Combobox();
		rowTermin.appendChild(kodeTermin);
		kodeTermin.setReadonly(true);
		kodeTermin.setWidth("90%");

		rowTerminProgres = new MyFormRow();
		rowTerminProgres.setVisible(false);
		rowTerminProgres.setParent(rows);
		rowTerminProgres.appendChild(new ais.ui.util.MyLabelConfig("Progres"));

		progresTermin = new Label();
		rowTerminProgres.appendChild(progresTermin);

		rowTerminDokumen = new MyFormRow();
		rowTerminDokumen.setVisible(false);
		rowTerminDokumen.setParent(rows);
		rowTerminDokumen.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Termin"));

		dokumenTermin = new Vbox();
		rowTerminDokumen.appendChild(dokumenTermin);

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampilan_nama_kurir_di_penerimaan"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kurir"));
		kurir = new MyTextbox(
				penerimaanPengadaanMasterAsset.getKurir() == null ? "" : penerimaanPengadaanMasterAsset.getKurir());
		if (persetujuan) {
			row.appendChild(new Label(penerimaanPengadaanMasterAsset.getKurir() == null ? ""
					: penerimaanPengadaanMasterAsset.getKurir()));
		} else {
			row.appendChild(kurir);
		}
		kurir.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild((penerimaanPengadaanMasterAssetHelper = new PenerimaanPengadaanMasterAssetHelper(
				gridMasterAsset = new MyGrid()))
				.initDetail(penerimaanPengadaanMasterAsset, persetujuan, tampaPemesanan));

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				eventListenerPesanan.onEvent(null);

				rowTermin.setVisible(false);
				rowTerminProgres.setVisible(false);
				rowTerminDokumen.setVisible(false);

				PemesananPengadaanMasterAsset mypemesananPengadaanMasterAsset = (PemesananPengadaanMasterAsset) pemesananPengadaanMasterAsset
						.getAttribute("pemesananPengadaanMasterAsset");
				UangMuka uangMukaD = (UangMuka) uangMuka.getAttribute("uangMuka");

				rowUangMuka.setVisible(mypemesananPengadaanMasterAsset == null);

				penyedia.setValue(
						mypemesananPengadaanMasterAsset == null || mypemesananPengadaanMasterAsset.getPenyedia() == null
								? ""
								: (mypemesananPengadaanMasterAsset).getPenyedia().getNama());
				Common.clear(kodeTermin);
				if (mypemesananPengadaanMasterAsset != null) {
					rowTermin.setVisible(mypemesananPengadaanMasterAsset.getByTermin());
					rowTerminProgres.setVisible(mypemesananPengadaanMasterAsset.getByTermin());
					rowTerminDokumen.setVisible(mypemesananPengadaanMasterAsset.getByTermin());

					String p = "";
					try {

						JSONArray array = new JSONArray(mypemesananPengadaanMasterAsset.getFormula());
						for (int i = 0; i < array.length(); i++) {

							JSONObject jsonObject = array.getJSONObject(i);

							if (jsonObject.isNull("key")) {
								continue;
							}

							String nama = "";

							if (!jsonObject.isNull("nama")) {
								nama = jsonObject.get("nama") + "";
							}

							String nomor = "";

							if (!jsonObject.isNull("nomor")) {
								nomor = jsonObject.get("nomor") + "";
							}

							Double pekerjaan = 0.0;
							if (!jsonObject.isNull("pekerjaan")) {
								pekerjaan = jsonObject.getDouble("pekerjaan");
							}

							Double penagihan = 0.0;
							if (!jsonObject.isNull("penagihan")) {
								penagihan = jsonObject.getDouble("penagihan");
							}

							JenisPajakBarang jenisPajakBarang;
							if (!jsonObject.isNull("pajak")) {
								jenisPajakBarang = (JenisPajakBarang) ConstantValues.ambil(
										JenisPajakBarang.class.getName(), Long.parseLong(jsonObject.get("pajak") + ""));
							} else {
								jenisPajakBarang = null;
							}

							Comboitem comboitem = new Comboitem(nomor + " " + nama);
							comboitem.setDescription("Progress " + Common.numberFormat.get().format(pekerjaan)
									+ "% nilai " + Common.numberFormat.get().format(penagihan) + ", Pajak "
									+ (jenisPajakBarang == null ? "\"Tanpa Pajak\"" : jenisPajakBarang.getNama()));
							comboitem.setValue(jsonObject.get("key") + "");
							comboitem.setAttribute("value", jsonObject);
							kodeTermin.appendChild(comboitem);

							if (penerimaanPengadaanMasterAsset.getKodeTermin().equals(jsonObject.get("key") + "")) {
								p = Common.numberFormat.get().format(pekerjaan) + "%";

								final JSONArray dokumens;
								if (!jsonObject.isNull("dokumens")) {
									dokumens = jsonObject.getJSONArray("dokumens");
								} else {
									dokumens = new JSONArray();
									jsonObject.put("dokumens", dokumens);
								}

								Common.clear(dokumenTermin);

								for (int ii = 0; ii < dokumens.length(); ii++) {

									final JSONObject jsonDokumen = dokumens.getJSONObject(ii);

									if (jsonDokumen.isNull("keyDok")) {
										continue;
									}

									Long keyDok = Long.parseLong(jsonDokumen.get("keyDok") + "");

									Long id_file = null;

									if (!jsonDokumen.isNull("id_file")) {
										id_file = Long.parseLong(jsonDokumen.get("id_file") + "");
									}

									String nama_file = "";

									if (!jsonDokumen.isNull("nama_file")) {
										nama_file = jsonDokumen.get("nama_file") + "";
									}

									String link = "";

									if (!jsonDokumen.isNull("link")) {
										link = jsonDokumen.get("link") + "";
									}

									final LampiranLain lampiranLain = id_file != null
											? LampiranLain.ambil(true, id_file, "id")
											: LampiranLain.ambil(keyDok, "Dokumen Termin PO");

									if (lampiranLain != null) {

										A a = new A(lampiranLain.getNama());
										a.setParent(dokumenTermin);
										a.setWidth("95%");

										a.addEventListener("onClick", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												Common.display(lampiranLain);
											}
										});

									} else if (!nama_file.isEmpty() && !link.isEmpty()) {

										A a = new A(nama_file);
										a.setParent(dokumenTermin);
										a.setWidth("95%");
										final String url = link;
										a.addEventListener("onClick", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												Clients.evalJavaScript("popupCenter({url: '" + url
														+ "', title: 'Data', w: 1200, h: 600});");
											}
										});

									}
								}
							}
						}
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					progresTermin.setValue(p);
				}

				Common.selectComboItem(true, kodeTermin, penerimaanPengadaanMasterAsset.getKodeTermin());

				generateDetail(mypemesananPengadaanMasterAsset, uangMukaD,
						penerimaanPengadaanMasterAsset.getKodeTermin(), null);
			}
		};

		pemesananPengadaanMasterAsset.setEventListener(eventListener);
		uangMuka.setEventListener(eventListener);

		reimbursement.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListenerPesanan.onEvent(null);
				ais.database.model.akunting.ReimbursementPegawai reimbD = (ais.database.model.akunting.ReimbursementPegawai) reimbursement
						.getAttribute("reimbursementPegawai");
				generateDetailReimbursement(reimbD);
			}
		});

		EventListener eventListenerTermin = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PemesananPengadaanMasterAsset mypemesananPengadaanMasterAsset = (PemesananPengadaanMasterAsset) pemesananPengadaanMasterAsset
						.getAttribute("pemesananPengadaanMasterAsset");
				UangMuka uangMukaD = (UangMuka) uangMuka.getAttribute("uangMuka");
				JSONObject jsonObject = (JSONObject) (kodeTermin.getSelectedItem() == null ? null
						: kodeTermin.getSelectedItem().getAttribute("value"));
				if (jsonObject != null) {
					penerimaanPengadaanMasterAsset.setPemesananPengadaanMasterAsset(mypemesananPengadaanMasterAsset);
					penerimaanPengadaanMasterAsset.setKodeTermin((String) (kodeTermin.getSelectedItem() == null ? null
							: kodeTermin.getSelectedItem().getValue()));
					penerimaanPengadaanMasterAsset.setJsonTermin(jsonObject + "");
					penerimaanPengadaanMasterAssetHelper.setTermin(jsonObject, penerimaanPengadaanMasterAsset);

					Double pekerjaan = 0.0;
					if (!jsonObject.isNull("pekerjaan")) {
						pekerjaan = jsonObject.getDouble("pekerjaan");
					}

					progresTermin.setValue(Common.numberFormat.get().format(pekerjaan) + "%");

					final JSONArray dokumens;
					if (!jsonObject.isNull("dokumens")) {
						dokumens = jsonObject.getJSONArray("dokumens");
					} else {
						dokumens = new JSONArray();
						jsonObject.put("dokumens", dokumens);
					}

					Common.clear(dokumenTermin);

					for (int i = 0; i < dokumens.length(); i++) {

						final JSONObject jsonDokumen = dokumens.getJSONObject(i);

						if (jsonDokumen.isNull("keyDok")) {
							continue;
						}

						Long keyDok = Long.parseLong(jsonDokumen.get("keyDok") + "");

						Long id_file = null;

						if (!jsonDokumen.isNull("id_file")) {
							id_file = Long.parseLong(jsonDokumen.get("id_file") + "");
						}

						String nama_file = "";

						if (!jsonDokumen.isNull("nama_file")) {
							nama_file = jsonDokumen.get("nama_file") + "";
						}

						String link = "";

						if (!jsonDokumen.isNull("link")) {
							link = jsonDokumen.get("link") + "";
						}

						final LampiranLain lampiranLain = id_file != null ? LampiranLain.ambil(true, id_file, "id")
								: LampiranLain.ambil(keyDok, "Dokumen Termin PO");

						if (lampiranLain != null) {

							A a = new A(lampiranLain.getNama());
							a.setParent(dokumenTermin);
							a.setWidth("95%");

							a.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Common.display(lampiranLain);
								}
							});

						} else if (!nama_file.isEmpty() && !link.isEmpty()) {

							A a = new A(nama_file);
							a.setParent(dokumenTermin);
							a.setWidth("95%");
							final String url = link;
							a.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Clients.evalJavaScript(
											"popupCenter({url: '" + url + "', title: 'Data', w: 1200, h: 600});");
								}
							});

						}
					}
					generateDetail(mypemesananPengadaanMasterAsset, uangMukaD,
							penerimaanPengadaanMasterAsset.getKodeTermin(), jsonObject);
				}

			}
		};

		kodeTermin.addEventListener("onChange", eventListenerTermin);

		row = new MyFormRow();
		row.setVisible(persetujuan && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Penerimaan"));
		row.appendChild(setujui = new MyCheckboxConfig("Setujui Penerimaan Barang / Jasa ini"));
		setujui.setChecked(penerimaanPengadaanMasterAsset.getDisetujuiOleh() != null);

		row = new MyFormRow();
		row.setVisible(penerimaanPengadaanMasterAsset.getDisetujuiOleh() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Persetujuan"));
		tanggalPersetujuanManual = new MyDatebox(penerimaanPengadaanMasterAsset.getTanggalPersetujuanManual());
		if (penerimaanPengadaanMasterAsset.getPostingHistory() == null) {
			row.appendChild(tanggalPersetujuanManual);
		} else {
			row.appendChild(new Label(Common.dateFormat1.get()
					.format(penerimaanPengadaanMasterAsset.getTanggalPersetujuanManual() == null ? WaktuUtil.getDate()
							: penerimaanPengadaanMasterAsset.getTanggalPersetujuanManual())));
		}
		tanggalPersetujuanManual.setReadonly(true);
		tanggalPersetujuanManual.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (penerimaanPengadaanMasterAsset != null && penerimaanPengadaanMasterAsset.getId() != null) {
					penerimaanPengadaanMasterAsset.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
					Common.refreshUpdate(penerimaanPengadaanMasterAsset);
				}
			}
		});

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox && setujui != arg0.getTarget()) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("selesai");

					if (tanggalPersetujuanManual != null && tanggalPersetujuanManual.getParent() != null) {
						if (tanggalPersetujuanManual.getValue() == null) {
							tanggalPersetujuanManual.setValue(WaktuUtil.getDate());
						}
						tanggalPersetujuanManual.getParent().setVisible(selesai != null && selesai);
					}

					if (selesai != null && selesai) {
						setujui.setChecked(true);
						setujui.setDisabled(true);
					} else {
						setujui.setChecked(false);
						setujui.setDisabled(false);
					}
				}
			}
		});

		if (setujuiData != null) {
			setujui.addEventListener("onClick", setujuiData);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setujuiData.onEvent(new Event("", null, penerimaanPengadaanMasterAsset.getDisetujuiOleh() != null));
				}
			});
		}

		Common.createDefaultTimer(eventListenerTermin);
		Common.createDefaultTimer(eventListener);

		return grid;
	}

	/**
	 * Mengembalikan istilah/nama modul yang digunakan dalam konteks alur SOP.
	 *
	 * <b>Tujuan:</b><br>
	 * Implementasi metode dari antarmuka {@link FormSop} yang menyediakan label tekstual untuk modul
	 * ini agar dapat ditampilkan dalam antarmuka alur persetujuan SOP secara konsisten.
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengembalikan string literal "Penerimaan Barang / Jasa" yang merupakan nama resmi modul ini
	 * dalam sistem. Nama ini digunakan di judul dialog SOP, notifikasi, dan laporan alur kerja.
	 *
	 * <b>Return:</b><br>
	 * @return String "Penerimaan Barang / Jasa" — nama resmi modul dalam konteks SOP.
	 * @throws Exception tidak dilempar dalam implementasi ini, namun dideklarasikan karena kontrak antarmuka.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika nama modul berubah dalam kebijakan bisnis, perbarui string ini dan pastikan konsistensi
	 * dengan label di file ZUL, laporan, dan dokumentasi sistem.
	 */
	@Override
	public String istilah() throws Exception {
		return "Penerimaan Barang / Jasa";
	}

	/**
	 * Mengembalikan entitas penerimaan pengadaan yang sedang diedit sebagai objek {@link DataSop}.
	 *
	 * <b>Tujuan:</b><br>
	 * Implementasi metode dari antarmuka {@link FormSop} yang menyediakan akses ke entitas domain
	 * yang sedang dalam proses pengisian formulir. Dipanggil oleh mekanisme SOP untuk mendapatkan
	 * referensi ke data yang sedang diproses dalam alur persetujuan.
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengembalikan field instance {@code penerimaanPengadaanMasterAsset} yang sudah diset oleh
	 * {@link #init} atau {@link #form} sebelum metode ini dipanggil. Kelas
	 * {@link PenerimaanPengadaanMasterAsset} mengimplementasikan {@link DataSop} sehingga dapat
	 * dikembalikan langsung.
	 *
	 * <b>Return:</b><br>
	 * @return objek {@link PenerimaanPengadaanMasterAsset} yang sedang aktif di formulir;
	 *         bisa null jika formulir belum diinisialisasi.
	 * @throws Exception tidak dilempar dalam implementasi ini.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Tidak perlu diubah kecuali ada perubahan tipe entitas yang dikelola controller ini.
	 */
	@Override
	public DataSop ambil() throws Exception {
		return penerimaanPengadaanMasterAsset;
	}

	/**
	 * Mengembalikan kelas entitas domain yang dikelola oleh controller ini.
	 *
	 * <b>Tujuan:</b><br>
	 * Implementasi metode dari antarmuka {@link FormSop} yang menyediakan informasi tipe kelas
	 * entitas. Digunakan oleh framework SOP untuk query dan operasi generik berbasis tipe kelas,
	 * seperti pencarian history disposisi dan audit trail.
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengembalikan literal kelas {@link PenerimaanPengadaanMasterAsset#getClass() .class}.
	 * Ini adalah metode delegasi satu baris yang memberikan informasi tipe kepada framework SOP.
	 *
	 * <b>Return:</b><br>
	 * @return {@link Class} literal {@code PenerimaanPengadaanMasterAsset.class}.
	 * @throws Exception tidak dilempar dalam implementasi ini.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Tidak perlu diubah selama entitas utama yang dikelola tetap
	 * {@link PenerimaanPengadaanMasterAsset}.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PenerimaanPengadaanMasterAsset.class;
	}

	/**
	 * Menghasilkan kode unik untuk dokumen penerimaan pengadaan menggunakan tanggal dari komponen UI.
	 *
	 * <b>Tujuan:</b><br>
	 * Metode convenience yang mendelegasikan ke overload statis {@link #generateCode(boolean, Date)}
	 * dengan tanggal diambil dari komponen datebox {@code tanggalPembuatan} di formulir.
	 *
	 * <b>Cara kerja:</b><br>
	 * Jika {@code tanggalPembuatan} null (formulir belum diinisialisasi), menggunakan tanggal hari ini
	 * dari {@link WaktuUtil#getDate()}. Kemudian mendelegasikan ke metode statis yang melakukan
	 * generate kode sesungguhnya berdasarkan template nomor surat dan indeks urut.
	 *
	 * <b>Parameter:</b><br>
	 * @param tambah {@code true} untuk menginkremen indeks nomor surat setelah generate (operasi save
	 *               pertama); {@code false} untuk hanya preview kode tanpa menginkremen (tampilan awal formulir).
	 * @return String kode unik yang sudah dijamin tidak duplikat via {@link ais.action.master.KodeUnikUtil#pastikanUnik}.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pastikan {@code tanggalPembuatan} sudah diinisialisasi sebelum metode ini dipanggil dari
	 * {@link #onSave}. Jika komponen datebox diganti, sesuaikan cara pengambilan nilai tanggalnya.
	 */
	public String generateCode(boolean tambah) {
		return generateCode(tambah, tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue());
	}

	/**
	 * Menghasilkan kode dokumen penerimaan pengadaan berdasarkan template nomor surat dan tanggal.
	 *
	 * <b>Tujuan:</b><br>
	 * Metode statis ini adalah implementasi utama generate kode penerimaan. Ia menggunakan konfigurasi
	 * {@link NomorSuratAlurPengadaan#PENERIMAAN_PEMBELIAN_DATA} untuk mendapatkan template nomor surat
	 * dan menghitung indeks urut yang tepat, kemudian memastikan hasilnya unik di database.
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Jika konfigurasi nomor surat tidak tersedia, menggunakan barcode random via
	 *       {@link Common#getGeneratedBarCode()}.</li>
	 *   <li>Menentukan indeks: jika konfigurasi menggunakan indeks urut sendiri
	 *       ({@code gunakanIndexUrut}), ambil dari field {@code nomorIndex}; jika tidak, hitung
	 *       dari database via {@link #getindex}.</li>
	 *   <li>Jika {@code tambah=true}, menginkremen indeks via
	 *       {@link NomorSurat#tambahIndexNomorSurat}.</li>
	 *   <li>Format kode menggunakan {@link NomorSurat#format(Long, Date)} dengan indeks dan tanggal.</li>
	 *   <li>Memastikan kode unik via {@link ais.action.master.KodeUnikUtil#pastikanUnik} yang akan
	 *       menambahkan suffix -2, -3, dst jika kode sudah ada.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param tambah          {@code true} untuk menginkremen counter nomor surat; {@code false} untuk
	 *                        preview kode tanpa mengubah counter.
	 * @param tanggalPembuatan tanggal yang digunakan dalam format nomor surat (bulan, tahun, dll.).
	 * @return String kode dokumen yang sudah dijamin unik di tabel penerimaan pengadaan.
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika ada race condition antara dua pengguna yang menyimpan secara bersamaan, constraint unik
	 * di database akan menangkap duplikasi yang lolos dari pengecekan ini.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Konfigurasi template nomor surat dikelola di tabel {@link NomorSuratAlurPengadaan}. Perubahan
	 * format kode harus dilakukan di sana, bukan di kode Java ini.
	 */
	public static String generateCode(boolean tambah, Date tanggalPembuatan) {
		if (NomorSuratAlurPengadaan.PENERIMAAN_PEMBELIAN_DATA == null
				|| NomorSuratAlurPengadaan.PENERIMAAN_PEMBELIAN_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PENERIMAAN_PEMBELIAN_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PENERIMAAN_PEMBELIAN_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PENERIMAAN_PEMBELIAN_DATA.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PENERIMAAN_PEMBELIAN_DATA.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurPengadaan.PENERIMAAN_PEMBELIAN_DATA.getNomorSurat().format(index,
				tanggalPembuatan);
		return ais.action.master.KodeUnikUtil.pastikanUnik(PenerimaanPengadaanMasterAsset.class, noAgenda);
	}

	/**
	 * Menghitung indeks urut berikutnya untuk penomoran dokumen penerimaan pengadaan.
	 *
	 * <b>Tujuan:</b><br>
	 * Metode ini menghitung jumlah dokumen penerimaan yang sudah ada sesuai aturan penomoran
	 * (per nomor surat, per kelompok, per tahun, per bulan, atau sejak tanggal reset tertentu)
	 * untuk menentukan indeks urut berikutnya yang akan digunakan dalam generate kode.
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Jika {@code nomorSurat} null, mengembalikan 0L.</li>
	 *   <li>Mengambil tahun dan bulan saat ini via {@link WaktuUtil#getCalendar()}.</li>
	 *   <li>Membuat Criteria pada {@link PenerimaanPengadaanMasterAsset} dengan JOIN ke
	 *       {@link NomorSuratAlurPengadaan} dan {@link NomorSurat}.</li>
	 *   <li>Menerapkan filter berdasarkan konfigurasi:
	 *     <ul>
	 *       <li>Jika {@code urutBerdasarkanNomor}: filter by nomor surat spesifik.</li>
	 *       <li>Jika {@code urutBerdasarkanKelompok}: filter by kelompok nomor surat.</li>
	 *       <li>Jika {@code resetUrutanTiapTahun}: filter by tahun saat ini.</li>
	 *       <li>Jika {@code resetUrutanTiapBulan}: filter by tahun dan bulan saat ini.</li>
	 *       <li>Jika ada {@code resetTiap} date: filter tanggalPembuatan >= tanggal reset.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Mengquery rowCount, menambah 1, dan mengembalikan sebagai indeks berikutnya.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param nomorSurat konfigurasi nomor surat yang menentukan aturan penomoran; null mengembalikan 0.
	 * @return {@code Long} indeks urut berikutnya (selalu minimal 1); tidak pernah null.
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika database tidak dapat diakses, exception Hibernate akan merambat ke pemanggil.
	 * Hasil null dari query dikonversi ke 0 sebelum increment.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Logika ini harus konsisten dengan {@link ais.action.master.KodeUnikUtil#pastikanUnik} agar
	 * indeks yang digenerate tidak terlalu cepat habis oleh suffix duplikasi. Jika ada perubahan
	 * struktur tabel penomoran, perbarui alias JOIN di sini.
	 */
	public static Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PenerimaanPengadaanMasterAsset.class)
				.createAlias("nomorSuratAlurPengadaan", "nomorSuratAlurPengadaan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurPengadaan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurPengadaan.nomorSurat", nomorSurat)

						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))

				.add(nomorSurat.getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetTiap() != null && (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang))
						|| nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	/**
	 * Menyetel mode persetujuan controller secara eksplisit dari luar kelas.
	 *
	 * <b>Tujuan:</b><br>
	 * Implementasi metode dari antarmuka {@link FormSop} yang memungkinkan mekanisme SOP
	 * mengubah mode tampilan controller antara mode edit dan mode persetujuan (readonly)
	 * setelah controller sudah diinstansiasi.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menyimpan nilai parameter {@code persetujuan} ke field instance dengan nama yang sama.
	 * Field ini kemudian digunakan di metode {@link #form} untuk menentukan apakah komponen
	 * UI ditampilkan sebagai input (edit) atau Label (read-only persetujuan).
	 *
	 * <b>Parameter:</b><br>
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan (readonly);
	 *                    {@code false} untuk mode edit normal.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pastikan setter ini selalu konsisten dengan konstruktor
	 * {@link #PenerimaanPengadaanMasterAssetAction(boolean)}.
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Membuka dialog formulir penerimaan pengadaan dari konteks eksternal (modul lain atau alur SOP).
	 *
	 * <b>Tujuan:</b><br>
	 * Metode statis ini memungkinkan modul lain (seperti alur SOP atau dashboard) untuk membuka
	 * formulir penerimaan pengadaan secara programatik tanpa harus melalui navigasi halaman.
	 * Dialog terbuka sebagai modal di atas halaman yang sedang aktif.
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Membuat instance baru {@link PenerimaanPengadaanMasterAssetAction} dengan mode persetujuan
	 *       aktif ({@code persetujuan = true}) karena konteks eksternal biasanya untuk review/persetujuan.</li>
	 *   <li>Membuat {@link MyWindow} baru sebagai container dialog dengan ukuran 95% x 90%.</li>
	 *   <li>Meng-attach window ke root komponen halaman yang sedang aktif via
	 *       {@link ExecutionsCtrl#getCurrentCtrl()}.</li>
	 *   <li>Memanggil {@link #init} untuk membangun formulir di dalam window.</li>
	 *   <li>Menampilkan window sebagai dialog modal yang dapat ditutup.</li>
	 * </ol>
	 * Parameter {@code eventListener} diterima tetapi saat ini tidak digunakan dalam implementasi —
	 * ini mungkin dimaksudkan untuk callback saat formulir ditutup.
	 *
	 * <b>Parameter:</b><br>
	 * @param eventListener listener yang dapat dipanggil setelah aksi selesai; saat ini tidak diimplementasikan.
	 * @param penerimaanPengadaanMasterAsset entitas penerimaan yang akan ditampilkan/diedit di dialog.
	 * @throws Exception jika terjadi error saat membangun formulir atau menampilkan dialog.
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari {@link #init} akan merambat ke pemanggil. Pastikan ada penanganan di sisi
	 * pemanggil untuk menampilkan error yang informatif kepada pengguna.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pertimbangkan untuk mengimplementasikan {@code eventListener} sebagai callback yang dipanggil
	 * saat dialog ditutup setelah penyimpanan, sehingga halaman pemanggil dapat me-refresh datanya.
	 * Ukuran dialog (95% x 90%) dapat disesuaikan jika formulir membutuhkan lebih banyak ruang.
	 */
	public static void onAddExternal(EventListener eventListener,
			PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset) throws Exception {
		PenerimaanPengadaanMasterAssetAction prosesTransferAction = new PenerimaanPengadaanMasterAssetAction();
		prosesTransferAction.persetujuan = true;
		prosesTransferAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(prosesTransferAction.addWindow);
		prosesTransferAction.addWindow.setHeight("95%");
		prosesTransferAction.addWindow.setWidth("90%");

		prosesTransferAction.init(penerimaanPengadaanMasterAsset);

		prosesTransferAction.addWindow.setVisible(true);
		prosesTransferAction.addWindow.setClosable(true);
		prosesTransferAction.addWindow.onModal();

	}

}
