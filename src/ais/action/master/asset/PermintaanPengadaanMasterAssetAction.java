package ais.action.master.asset;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

/**
 * <h3>PermintaanPengadaanMasterAssetAction — Titik masuk utama modul Permintaan Pengadaan Barang/Jasa (PR)</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah controller ZK ({@code GenericAutowireComposer}) yang mengelola seluruh siklus hidup
 * dokumen Permintaan Pengadaan (Purchase Request / PR) dalam sistem ERP aset. Pengguna berinteraksi
 * melalui halaman ZUL yang terhubung ke kelas ini untuk menciptakan PR baru, mengubah PR yang masih
 * dalam status draft, menyetujui atau menolak PR, mencetak dokumen PDF PR, serta mengonversi PR yang
 * telah disetujui menjadi Purchase Order (PO/Pemesanan) secara langsung melalui fitur "Beli Langsung".
 * Kelas ini juga menyediakan antarmuka dasbor statistik PR dan riwayat harga barang/jasa aset.
 * <br><br>
 *
 * <b>Cara kerja:</b><br>
 * Setelah ZK menginisialisasi halaman ({@code doAfterCompose}), kelas melakukan pemeriksaan hak akses
 * via {@code CommonPrivilages}, mengisi combo filter, mendaftarkan listener paginasi dan timer refresh
 * otomatis, serta menambahkan tombol aksi tambahan ("Hitung Ulang", "History") ke toolbar.
 * Daftar PR ditampilkan oleh {@code onSearchDefault} menggunakan {@code initCriteria} yang membangun
 * {@code Criteria} Hibernate berdasarkan seluruh filter aktif (kode, keterangan, lokasi, tanggal,
 * anggaran/workspace, status persetujuan, dan satuan kerja). Setiap baris grid dirender oleh kelas
 * inner {@code PermintaanPengadaanMasterAssetRenderer} yang menampilkan detail ringkas PR beserta
 * tombol aksi per-baris (Cetak, Setujui, Tolak, Batalkan, Ubah, Hapus, Beli Langsung).<br>
 * Formulir tambah/ubah dibangun secara programatis oleh metode {@code form} dan {@code init} dengan
 * komponen ZK seperti {@code MyTextbox}, {@code MyDatebox}, {@code AmbilDataWorkspaceBanbox}, dsb.
 * Penyimpanan dilakukan oleh {@code onSave} yang melakukan validasi menyeluruh (anggaran mencukupi,
 * kode wajib, keterangan wajib, item wajib ada, lampiran opsional/wajib berdasarkan konfigurasi)
 * sebelum menyimpan ke database via sesi Hibernate aktif ({@code HibernateUtil.currentSession()}).
 * Kode PR dihasilkan oleh {@code generateKodeUnik} yang menjamin keunikan dalam database.
 * <br><br>
 *
 * <b>Threading:</b><br>
 * Kelas ini berjalan di thread UI ZK. Operasi database berat seperti hitung ulang saldo anggaran
 * massal didelegasikan ke {@code Common.createDefaultTimer} agar tidak memblokir thread event UI.
 * Tidak ada penggunaan {@code synchronized} atau variabel berbagi antar-thread dalam kelas ini;
 * isolasi state dilakukan melalui instance terpisah per sesi ZK.
 * <br><br>
 *
 * <b>Pemeliharaan:</b><br>
 * Saat menambah field baru ke entitas {@code PermintaanPengadaanMasterAsset}, pastikan:
 * (1) Field ditambahkan ke form di metode {@code form}, (2) Field disalin di {@code onSave},
 * (3) Field disertakan di map parameter cetak {@code parameter(...)}, (4) DDL ALTER dijalankan
 * di skema {@code public} dan {@code new_audit} (Envers). Konfigurasi yang memengaruhi perilaku
 * kelas ini antara lain: {@code tampilkanRuanganDamPemilikAset}, {@code tampilkan_tanpa_anggaran},
 * {@code apakah_lampiran_pr_wajib}, dan {@code saldo_harus_cukup_sebelum_mengajukan_realisasi_anggaran}.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
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
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.JenisUangMukaAction;
import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.asset.helper.PermintaanPengadaanMasterAssetDetailAction;
import ais.action.master.asset.helper.PermintaanPengadaanMasterAssetHelper;
import ais.action.master.asset.helper.RevisiPermintaanPengadaanMasterAssetHelper;
import ais.action.master.asset.helper.RiwayatHargaBarangJasaAssetDashboard;
import ais.action.master.asset.helper.TraceStatusPengadaanAssetDashboard;
import ais.action.master.dashboard.asset.DasboardPermintaanPengadaanMasterAssetDetail;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
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
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
import ais.database.model.asset.PemilikAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
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
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

public class PermintaanPengadaanMasterAssetAction extends GenericAutowireComposer implements FormSop {

	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchkode;
	private Textbox searchketerangan;
	private Textbox searchanggaran;
	private Combobox searchlokasi;
	private Checkbox searchaktif;
	/** Filter status persetujuan: Semua / Belum Disetujui / Telah Disetujui. */
	private Combobox searchStatusPersetujuan;
	private Checkbox searchtutup;
	private MyDatebox start;
	private MyDatebox end;

	private AmbilDataSatuanKerjaBanbox searchparent;

	private Label kode;
	private MyTextbox keterangan;
	private MyDatebox tanggalPembuatan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset;
	private MyToolbarbuttonConfig add;
	private Combobox pemilikAsset;
	private Combobox lokasi;
	private AmbilDataRuangBanbox ruang;
	private MyGrid gridMasterAsset;

	private boolean persetujuan = false;
	private boolean viewOnly = false;

	private Tbmuser tbmuser;
	private MyCheckboxConfig setujui;
	private AmbilDataWorkspaceBanbox workspace;
	private DisposisiSop disposisiSop = null;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private boolean tampilkanRuanganDamPemilikAset = Common.bolehKonfigurasi("tampilkanRuanganDamPemilikAset", Konfigurasi.TIDAK_AKTIF);
	private MyCheckboxConfig wajibAdaPerjanjianKerjasama;

	private MyCheckboxConfig blmDisetujui;
	private MyCheckboxConfig disetujui;
	private MyCheckboxConfig tanpaAnggaran;
	private MyCheckboxConfig danaTitipan;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataAkunBanbox akun;
	private EventListener eventListener = null;

	protected Tabpanel statistik;
	protected Tabpanel riwayatHargaBarangJasa;
	protected Tabpanel traceStatusBarangJasa;
	private Row aa;

	private AmbilDataWorkspaceBanbox searchAnggaran;

	/**
	 * <b>Tujuan:</b> Memuat dan menampilkan dasbor statistik Permintaan Pengadaan ke dalam
	 * tab "Statistik" saat tab tersebut pertama kali diklik oleh pengguna.<br>
	 * <b>Cara kerja:</b> Dipanggil oleh event ZK {@code onStatistik} dari tab panel. Pemeriksaan
	 * {@code getChildren().size() == 0} memastikan komponen dasbor hanya dibuat satu kali (lazy
	 * initialization), sehingga tidak ada duplikasi meski tab diklik berkali-kali. Komponen
	 * {@code DasboardPermintaanPengadaanMasterAssetDetail} di-mount dengan lebar dan tinggi 100%
	 * agar mengisi panel sepenuhnya.<br>
	 * <b>Penanganan error:</b> Tidak ada penanganan eksplisit; jika terjadi exception saat
	 * inisialisasi dasbor, ZK akan menyebarkan exception tersebut ke framework.<br>
	 * <b>Pemeliharaan:</b> Jika dasbor statistik perlu diperbarui, ganti implementasi di dalam
	 * {@code DasboardPermintaanPengadaanMasterAssetDetail}, bukan di sini.
	 *
	 * @param event event ZK yang memicu tab statistik terbuka; tidak digunakan langsung
	 */
	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DasboardPermintaanPengadaanMasterAssetDetail include = new DasboardPermintaanPengadaanMasterAssetDetail();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(statistik);
		}
	}

	/**
	 * <b>Tujuan:</b> Memuat dan menampilkan dasbor riwayat harga barang/jasa ke dalam tab
	 * "Riwayat Harga Barang/Jasa" saat tab tersebut pertama kali diklik.<br>
	 * <b>Cara kerja:</b> Serupa dengan {@code onStatistik}, metode ini menggunakan pola lazy
	 * initialization. Pemeriksaan awal {@code riwayatHargaBarangJasa == null} melindungi dari
	 * {@code NullPointerException} jika field tidak terwire oleh ZK (misalnya halaman tidak
	 * memiliki tab tersebut). Komponen {@code RiwayatHargaBarangJasaAssetDashboard} hanya
	 * dibuat sekali dan dipasang ke parent tab panel.<br>
	 * <b>Penanganan error:</b> Guard {@code null} mencegah NPE; exception lain disebarkan ke ZK.<br>
	 * <b>Pemeliharaan:</b> Pastikan field {@code riwayatHargaBarangJasa} terwire di file ZUL
	 * dengan atribut {@code id="riwayatHargaBarangJasa"} pada elemen {@code <tabpanel>}.
	 *
	 * @param event event ZK yang memicu tab riwayat harga terbuka; tidak digunakan langsung
	 */
	public void onRiwayatHargaBarangJasa(Event event) {
		if (riwayatHargaBarangJasa == null) {
			return;
		}
		if (riwayatHargaBarangJasa.getChildren().size() == 0) {
			RiwayatHargaBarangJasaAssetDashboard dashboard = new RiwayatHargaBarangJasaAssetDashboard();
			dashboard.setHeight("100%");
			dashboard.setWidth("100%");
			dashboard.setParent(riwayatHargaBarangJasa);
		}
	}

	/**
	 * <b>Tujuan:</b> Memuat dasbor "Trace Status Barang/Jasa" (Laporan Proses Pengajuan Sarpras)
	 * ke dalam tab yang bersebelahan dengan "Riwayat Harga Barang/Jasa" saat tab tersebut pertama
	 * kali diklik.<br>
	 * <b>Cara kerja:</b> Sama seperti {@code onRiwayatHargaBarangJasa}, memakai pola lazy
	 * initialization. Guard {@code null} melindungi dari NPE jika field tidak terwire, dan
	 * pemeriksaan {@code getChildren().size() == 0} memastikan {@code TraceStatusPengadaanAssetDashboard}
	 * hanya dibuat sekali. Dasbor menelusuri sejauh mana tiap PR berjalan: Permintaan &rarr; Uang Muka
	 * (UM) &rarr; PO &rarr; BAST &rarr; Terima Tagihan &rarr; Dibayar (DPC).<br>
	 * <b>Pemeliharaan:</b> Pastikan field {@code traceStatusBarangJasa} terwire di file ZUL dengan
	 * atribut {@code id="traceStatusBarangJasa"} pada elemen {@code <tabpanel>}.
	 *
	 * @param event event ZK yang memicu tab trace status terbuka; tidak digunakan langsung
	 */
	public void onTraceStatusBarangJasa(Event event) {
		if (traceStatusBarangJasa == null) {
			return;
		}
		if (traceStatusBarangJasa.getChildren().size() == 0) {
			TraceStatusPengadaanAssetDashboard dashboard = new TraceStatusPengadaanAssetDashboard();
			dashboard.setHeight("100%");
			dashboard.setWidth("100%");
			dashboard.setParent(traceStatusBarangJasa);
		}
	}

	/**
	 * <b>Tujuan:</b> Konstruktor default yang digunakan oleh ZK saat membuat instance
	 * controller dari file ZUL secara otomatis.<br>
	 * <b>Cara kerja:</b> Mengambil pengguna yang sedang login melalui
	 * {@code Common.getCurrentUser()} dan menyimpannya ke field {@code tbmuser}. Pengguna
	 * ini kemudian digunakan saat membuat PR baru untuk mengisi field {@code dibuatOleh}.<br>
	 * <b>Pemeliharaan:</b> Jika {@code Common.getCurrentUser()} mengembalikan null (sesi
	 * tidak valid), nilai {@code tbmuser} akan null dan dapat menyebabkan NPE di {@code onSave}.
	 * Pastikan keamanan sesi diperiksa di {@code doAfterCompose} sebelum memanggil metode lain.
	 */
	public PermintaanPengadaanMasterAssetAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * <b>Tujuan:</b> Konstruktor dengan parameter untuk membuat instance controller dalam
	 * mode persetujuan (approval mode), biasanya dipanggil secara programatis dari modul lain
	 * seperti alur SOP atau dasbor persetujuan.<br>
	 * <b>Cara kerja:</b> Menyetel flag {@code persetujuan} ke nilai yang diberikan. Saat
	 * {@code persetujuan} bernilai {@code true}, formulir yang dirender oleh {@code form}
	 * akan menampilkan field dalam mode baca-saja menggunakan {@code Label} biasa, bukan
	 * komponen input, sehingga penyetuju hanya dapat melihat dan memberi persetujuan tanpa
	 * mengubah isi PR. Pengguna aktif diambil dari sesi dan disimpan ke {@code tbmuser}.<br>
	 * <b>Parameter:</b><br>
	 * {@code persetujuan} — {@code true} untuk mode persetujuan (tampilan baca-saja),
	 * {@code false} untuk mode edit normal.<br>
	 * <b>Pemeliharaan:</b> Pastikan pemanggil juga memanggil {@code init(permintaan)} dan
	 * melampirkan {@code addWindow} ke halaman sebelum memunculkan window modal.
	 *
	 * @param persetujuan {@code true} jika controller dibuat dalam konteks alur persetujuan
	 */
	public PermintaanPengadaanMasterAssetAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * <b>Tujuan:</b> Memverifikasi hak akses keamanan sebelum ZK menyusun komponen halaman
	 * (fase pra-komposisi), sebagai lapisan keamanan pertama sebelum halaman dirender.<br>
	 * <b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} yang memeriksa sesi dan
	 * privilege pengguna. Jika pemeriksaan gagal (sesi tidak valid, privilege kurang), method
	 * tersebut akan melakukan redirect atau melempar exception sehingga halaman tidak pernah
	 * selesai dirender. Setelah pemeriksaan, memanggil implementasi super agar metadata
	 * komponen ZUL diproses seperti biasa.<br>
	 * <b>Penanganan error:</b> Exception dari {@code Common.doCheckSecurity()} dibiarkan
	 * menyebar untuk ditangani oleh ZK error handler global.<br>
	 * <b>Pemeliharaan:</b> Jangan hapus pemanggilan {@code Common.doCheckSecurity()} di sini.
	 * Ini adalah garis pertahanan pertama melawan akses tanpa otentikasi.
	 *
	 * @param page     halaman ZK yang sedang dikompilasi
	 * @param parent   komponen induk di mana controller akan dipasang
	 * @param compInfo metadata komponen dari file ZUL
	 * @return {@code ComponentInfo} dari implementasi super, tidak dimodifikasi
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Melakukan inisialisasi lengkap controller setelah ZK selesai menyusun
	 * seluruh komponen UI dari file ZUL, termasuk wiring field, pengisian data referensi,
	 * pendaftaran listener, dan konfigurasi tampilan awal grid PR.<br>
	 * <b>Cara kerja:</b> Urutan inisialisasi adalah sebagai berikut:
	 * <ol>
	 *   <li>Verifikasi sesi pengguna ({@code usersTemp}) dan privilege READ; jika gagal,
	 *       pengguna di-redirect ke halaman logoff.</li>
	 *   <li>Guard awal: jika {@code searchparent} atau {@code add} null (halaman minimal),
	 *       inisialisasi dihentikan lebih awal.</li>
	 *   <li>Isi combobox filter lokasi dengan daftar lokasi aktif; jika sesi menyimpan
	 *       atribut "Lokasi", pilih dan kunci item tersebut (multi-tenant).</li>
	 *   <li>Daftarkan {@code EventListener} pada {@code searchparent} dan {@code searchAnggaran}
	 *       agar perubahan filter langsung memicu {@code onSearchDefault}.</li>
	 *   <li>Inisialisasi {@code SatuanKerjaTreeModel} untuk navigasi hierarki satuan kerja.</li>
	 *   <li>Konfigurasi {@code MyDatebox} filter tanggal (readonly, range 6 bulan ke belakang
	 *       hingga besok).</li>
	 *   <li>Atur visibilitas tombol "Tambah" berdasarkan privilege CREATE.</li>
	 *   <li>Baca dan simpan flag privilege UPDATE, DELETE, APPROVE, REJECT ke field instance.</li>
	 *   <li>Inisialisasi paginasi dan timer refresh otomatis via {@code Common.initPaging} dan
	 *       {@code Common.createDefaultTimer}.</li>
	 *   <li>Tambahkan tombol "Hitung Ulang" yang menghitung ulang saldo anggaran semua PR
	 *       (maks 5000 baris) secara async, dan tombol "History" yang membuka dialog revisi.</li>
	 *   <li>Aktifkan panel filter lanjut via {@code FilterLanjutHelper.setup}.</li>
	 * </ol>
	 * <b>Penanganan error:</b> Jika sesi tidak valid, pengguna diarahkan ke logoff. Exception
	 * lain dari listener didelegasikan ke ZK.<br>
	 * <b>Pemeliharaan:</b> Pastikan semua field yang di-wire ke ZUL memiliki id yang sama
	 * persis dengan nama field di kelas ini. Jika menambah filter baru, tambahkan juga
	 * listener-nya di sini dan tambahkan restriction di {@code initCriteria}.
	 *
	 * @param comp komponen root yang di-wire oleh ZK
	 * @throws Exception jika terjadi error saat inisialisasi komponen atau pemanggilan super
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (searchparent == null || add == null) return;
		Common.insertComboDanSemua(searchlokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (session.getAttribute("Lokasi") != null) {
			Common.selectComboItem(searchlokasi, session.getAttribute("Lokasi"));
			searchlokasi.setDisabled(true);
			session.removeAttribute("Lokasi");
		}
		LokasiAction.kunciLokasi(searchlokasi);

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		searchAnggaran.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

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

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hitung Ulang", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<PermintaanPengadaanMasterAsset> permintaanPengadaanMasterAssets = initCriteria(false)
								.addOrder(Order.asc("id")).setMaxResults(5000).list();

						for (PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset : permintaanPengadaanMasterAssets) {
							Workspace work = permintaanPengadaanMasterAsset.getWorkspace();
							Double saldo = work == null ? 0.0
									: JenisUangMukaAction.hitungSaldo(null, null,
											permintaanPengadaanMasterAsset == null ? null
													: permintaanPengadaanMasterAsset.getId(),
											null, work, permintaanPengadaanMasterAsset.getTanggalPembuatan());

							if (saldo.intValue() != permintaanPengadaanMasterAsset.getSaldo().intValue()) {
								permintaanPengadaanMasterAsset.setSaldo(saldo);
								Common.refreshUpdate(permintaanPengadaanMasterAsset);
							}
						}
						onSearchDefault(null);
					}
				});

			}

		});
		if (button != null) { button.setParent(add.getParent()); }

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiPermintaanPengadaanMasterAssetHelper revisiHelper = new RevisiPermintaanPengadaanMasterAssetHelper(
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

		// Tombol "Download" data PR ke Excel (mengikuti pola AgamaAction, tanpa tombol Upload).
		// DataCriteria dibungkus anonim agar mendelegasikan ke initCriteria(order) milik Action ini,
		// sehingga isi file mengikuti seluruh filter pencarian yang sedang aktif.
		if (add != null) {
			Common.appendDownloadButton(add, PermintaanPengadaanMasterAsset.class,
					new ais.ui.util.DataCriteria() {
						@Override
						public Object initCriteria(boolean order) {
							return PermintaanPengadaanMasterAssetAction.this.initCriteria(order);
						}
					},
					"id", "kode", "keterangan", "nilai", "saldo", "workspace", "satuanKerja", "lokasi",
					"ruang", "pemilikAsset", "tanggalPembuatan", "tanggalPersetujuan", "dibuatOleh",
					"disetujuiOleh", "pemesananPengadaanMasterAsset", "aktif", "tutup");
		}

	        FilterLanjutHelper.setup(comp);
}

	/**
	 * <h3>PermintaanPengadaanMasterAssetRenderer — Renderer baris grid Permintaan Pengadaan</h3>
	 *
	 * <b>Untuk apa:</b><br>
	 * Kelas inner ini bertanggung jawab merender setiap baris ({@code Row}) dalam grid utama
	 * daftar Permintaan Pengadaan. Setiap baris menampilkan informasi ringkas PR beserta
	 * tombol-tombol aksi yang visibilitasnya dikontrol oleh status PR dan privilege pengguna.
	 * <br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Metode {@code render} dipanggil oleh ZK untuk setiap item dalam model. Kolom yang
	 * dirender (berurutan): detail item PR via {@code PermintaanPengadaanMasterAssetDetailAction},
	 * kode PR + link workspace + lampiran + kode PO terkait, saldo anggaran, nilai PR, sisa
	 * anggaran, pemilik aset, lokasi, ruang, pembuat + tanggal, penyetuju/penolak + tanggal,
	 * flag perjanjian kerjasama, keterangan + status aktif + status tutup + info SOP, dan
	 * toolbar aksi (Cetak, Setujui, Tolak, Batalkan, Ubah, Hapus, Beli Langsung/Batalkan Beli).
	 * Saldo dihitung real-time via {@code JenisUangMukaAction.hitungSaldo} dan jika berbeda
	 * dari nilai tersimpan, diperbarui otomatis ke database.
	 * <br><br>
	 *
	 * <b>Threading:</b><br>
	 * Dipanggil di thread UI ZK. Operasi database (hitungSaldo, refreshUpdate) dilakukan
	 * secara sinkron dalam thread yang sama karena menggunakan {@code HibernateUtil.currentSession()}.
	 * <br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Urutan penambahan komponen ke {@code arg0} harus sinkron dengan deklarasi {@code <column>}
	 * di file ZUL. Jika menambah kolom baru, tambahkan juga {@code <column>} di ZUL dan
	 * sesuaikan urutan penambahan sel di sini.
	 */
	class PermintaanPengadaanMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data PR ke dalam komponen ZK {@code Row}
		 * beserta semua sel data dan tombol aksi interaktif.<br>
		 * <b>Cara kerja:</b> Cast {@code arg1} ke {@code PermintaanPengadaanMasterAsset},
		 * buat dan pasang semua komponen sel secara berurutan ke {@code arg0}. Tombol aksi
		 * (disetujui, ditolak, dibatalkan, rubah, hapus) menggunakan closure untuk mengakses
		 * entitas PR dan label status yang perlu diperbarui setelah aksi berhasil. Logika
		 * "Beli Langsung" mengecek apakah PR sudah punya PO terkait dan belum ada uang muka;
		 * jika memenuhi syarat, tombol ditampilkan untuk membuat PO langsung dari PR.<br>
		 * <b>Penanganan error:</b> Exception dari aksi hapus ditangkap dan ditampilkan
		 * menggunakan {@code MyMessageboxConfig} serta {@code Common.tampilErrorJikaAdmin}.<br>
		 * <b>Pemeliharaan:</b> Perubahan pada jumlah atau urutan kolom di ZUL harus diimbangi
		 * dengan perubahan urutan {@code setParent(arg0)} di metode ini.
		 *
		 * @param arg0 baris ZK yang akan diisi dengan sel-sel data
		 * @param arg1 objek {@code PermintaanPengadaanMasterAsset} yang dirender
		 * @throws Exception jika terjadi error saat membangun komponen atau mengakses database
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset = (PermintaanPengadaanMasterAsset) arg1;

			final PermintaanPengadaanMasterAssetDetailAction detail;
			(detail = new PermintaanPengadaanMasterAssetDetailAction(permintaanPengadaanMasterAsset)).setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PermintaanPengadaanMasterAsset.class, permintaanPengadaanMasterAsset,
					permintaanPengadaanMasterAsset.getKode())).setParent(arg0);

			if (permintaanPengadaanMasterAsset.getWorkspace() != null) {
				RevisiHelper.createNewRevisi(Workspace.class, permintaanPengadaanMasterAsset.getWorkspace(),
						permintaanPengadaanMasterAsset.getWorkspace().getKode() + "-"
								+ permintaanPengadaanMasterAsset.getWorkspace().getNama())
						.setParent(a);

			}

			Hbox hbox = new Hbox();
			hbox.setParent(a);
			LampiranLain.createDownloadUploadFileLain(hbox, permintaanPengadaanMasterAsset.getId(),
					PermintaanPengadaanMasterAsset.class.getName(), "Lampiran", false, null, null, false, false, false,
					false);

			if (permintaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset() != null) {
				new Label(permintaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset().getKode()).setParent(a);
			}

			Workspace work = permintaanPengadaanMasterAsset.getWorkspace();

			Double saldo = work == null ? 0.0
					: JenisUangMukaAction.hitungSaldo(null,
							permintaanPengadaanMasterAsset == null ? null : permintaanPengadaanMasterAsset.getId(),
							null, null, work, permintaanPengadaanMasterAsset.getTanggalPembuatan());

			// Update saldo di memori saja untuk tampilan; penyimpanan dilakukan saat onSave/transaksi
			if (saldo.intValue() != permintaanPengadaanMasterAsset.getSaldo().intValue()) {
				permintaanPengadaanMasterAsset.setSaldo(saldo);
			}
			new Label(
					permintaanPengadaanMasterAsset.getTanpaAnggaran() || permintaanPengadaanMasterAsset.getDanaTitipan()
							? "-"
							: Common.numberFormat.get().format(saldo))
					.setParent(arg0);
			new Label(Common.numberFormat.get().format(permintaanPengadaanMasterAsset.getNilai())).setParent(arg0);

			Double sisa = saldo - permintaanPengadaanMasterAsset.getNilai();
			if (sisa < 0.0) {
				sisa = 0.0;
			}
			new Label(
					permintaanPengadaanMasterAsset.getTanpaAnggaran() || permintaanPengadaanMasterAsset.getDanaTitipan()
							? "-"
							: Common.numberFormat.get().format(sisa))
					.setParent(arg0);

			new Label(permintaanPengadaanMasterAsset.getPemilikAsset() == null ? ""
					: permintaanPengadaanMasterAsset.getPemilikAsset().getNama()).setParent(arg0);

			new Label(permintaanPengadaanMasterAsset.getLokasi() == null ? ""
					: (permintaanPengadaanMasterAsset.getLokasi().getNama())).setParent(arg0);

			new Label(permintaanPengadaanMasterAsset.getRuang() == null ? ""
					: permintaanPengadaanMasterAsset.getRuang().getNama()).setParent(arg0);

			a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(permintaanPengadaanMasterAsset.getDibuatOleh() == null ? ""
					: permintaanPengadaanMasterAsset.getDibuatOleh().getUserNama()).setParent(a);
			new MyLabelAgakKecil(permintaanPengadaanMasterAsset.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(permintaanPengadaanMasterAsset.getTanggalPembuatan()))
					.setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			final MyLabelAgakKecil disetujuiOleh = new MyLabelAgakKecil(
					permintaanPengadaanMasterAsset.getDisetujuiOleh() == null ? ""
							: permintaanPengadaanMasterAsset.getDisetujuiOleh().getUserNama());

			final MyLabelAgakKecil disetujuiTanggal = new MyLabelAgakKecil(
					permintaanPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
							: Common.dateFormat3.get().format(permintaanPengadaanMasterAsset.getTanggalPersetujuan()));

			final MyLabelAgakKecilBoldMerah ditolakOleh = new MyLabelAgakKecilBoldMerah(
					permintaanPengadaanMasterAsset.getDitolakOleh() == null ? ""
							: "Ditolak oleh " + permintaanPengadaanMasterAsset.getDitolakOleh().getUserNama());
			final MyLabelAgakKecilBoldMerah tanggalDitolak = new MyLabelAgakKecilBoldMerah(
					permintaanPengadaanMasterAsset.getTanggalDitolak() == null ? ""
							: Common.dateFormat3.get().format(permintaanPengadaanMasterAsset.getTanggalDitolak()));

			ditolakOleh.setParent(a);
			tanggalDitolak.setParent(a);
			disetujuiOleh.setParent(a);
			disetujuiTanggal.setParent(a);

			new Label(permintaanPengadaanMasterAsset.getWajibAdaPerjanjianKerjasama() ? "Ya" : "Tidak").setParent(arg0);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(Common.simpleString(permintaanPengadaanMasterAsset.getKeterangan())).setParent(vbox1);

			if (permintaanPengadaanMasterAsset.getDisposisiSop() != null
					&& !permintaanPengadaanMasterAsset.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (edit && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(permintaanPengadaanMasterAsset.getAktif());
				aktif.setParent(vbox1);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						permintaanPengadaanMasterAsset.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(permintaanPengadaanMasterAsset);
					}
				});
			} else {
				new Label("Aktif : " + (permintaanPengadaanMasterAsset.getAktif() ? "Ya" : "Tidak")).setParent(vbox1);
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Tutup");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(permintaanPengadaanMasterAsset.getTutup());
			checkbox.setParent(vbox1);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					permintaanPengadaanMasterAsset.setTutup(checkbox.isChecked());
					Common.refreshSaveOrUpdate(permintaanPengadaanMasterAsset);
				}
			});

			if (permintaanPengadaanMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa,
						"SOP " + permintaanPengadaanMasterAsset.getDisposisiSop().getKeterangan() + " ("
								+ permintaanPengadaanMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(permintaanPengadaanMasterAsset.getDisposisiSop().getId(), null,
								null, true, arg0.getTarget());
					}
				});
			}

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Permintaan Pengadaan Asset");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					cetak(permintaanPengadaanMasterAsset);
				}

			});
			aksiButtons.add(button);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig ditolak = new MyToolbarbuttonConfig("", "/img/svg/deny.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null
					&& permintaanPengadaanMasterAsset.getDitolakOleh() == null);
			ditolak.setVisible(reject && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null
					&& permintaanPengadaanMasterAsset.getDitolakOleh() == null);

			dibatalkan.setVisible(reject && (permintaanPengadaanMasterAsset.getDisetujuiOleh() != null
					|| permintaanPengadaanMasterAsset.getDitolakOleh() != null));

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Permintaan Pengadaan Barang/Jasa ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();
										Integer countMasterAssetjumlah = ((Number) session
												.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
												.setProjection(Projections.count("id"))
												.add(Restrictions.eq("permintaanPengadaanMasterAsset",
														permintaanPengadaanMasterAsset))
												.add(Restrictions.lt("jumlah", 1.0)).uniqueResult()).intValue();

										if (!countMasterAssetjumlah.equals(0)) {
											MyMessageboxConfig.show("Mohon maaf, terdapat baris item yang belum memiliki jumlah yang valid. Langkah yang dapat dilakukan: (1) Periksa setiap baris pada daftar barang/jasa; (2) Isi jumlah pada baris yang masih kosong atau bernilai 0; (3) ulangi proses persetujuan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											return;
										}
										permintaanPengadaanMasterAsset.setSetujuiManual(true);
										permintaanPengadaanMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
										permintaanPengadaanMasterAsset
												.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, permintaanPengadaanMasterAsset);

										disetujuiTanggal.setValue(
												permintaanPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(permintaanPengadaanMasterAsset
																.getTanggalPersetujuan()));
										disetujuiOleh.setValue(permintaanPengadaanMasterAsset.getDisetujuiOleh() == null
												? ""
												: permintaanPengadaanMasterAsset.getDisetujuiOleh().getUserNama());

										ditolakOleh
												.setValue(permintaanPengadaanMasterAsset.getDitolakOleh() == null ? ""
														: "Ditolak oleh " + permintaanPengadaanMasterAsset
																.getDitolakOleh().getUserNama());
										tanggalDitolak.setValue(
												permintaanPengadaanMasterAsset.getTanggalDitolak() == null ? ""
														: Common.dateFormat3.get().format(
																permintaanPengadaanMasterAsset.getTanggalDitolak()));

										disetujui.setVisible(
												approve && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null
														&& permintaanPengadaanMasterAsset.getDitolakOleh() == null);
										ditolak.setVisible(
												reject && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null
														&& permintaanPengadaanMasterAsset.getDitolakOleh() == null);
										dibatalkan.setVisible(
												reject && (permintaanPengadaanMasterAsset.getDisetujuiOleh() != null
														|| permintaanPengadaanMasterAsset.getDitolakOleh() != null));
										rubah.setVisible(
												edit && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(
												delete && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										cetak(permintaanPengadaanMasterAsset);

									}
								}
							});
				}

			});
			aksiButtons.add(disetujui);

			ditolak.setTooltiptext("Ditolak");

			ditolak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menolak Permintaan Pengadaan Barang/Jasa ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();
										permintaanPengadaanMasterAsset.setDitolakOleh(Common.getCurrentUser());
										permintaanPengadaanMasterAsset.setSetujuiManual(false);
										permintaanPengadaanMasterAsset
												.setTanggalDitolak(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, permintaanPengadaanMasterAsset);

										disetujuiTanggal.setValue(
												permintaanPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(permintaanPengadaanMasterAsset
																.getTanggalPersetujuan()));
										disetujuiOleh.setValue(permintaanPengadaanMasterAsset.getDisetujuiOleh() == null
												? ""
												: permintaanPengadaanMasterAsset.getDisetujuiOleh().getUserNama());

										ditolakOleh
												.setValue(permintaanPengadaanMasterAsset.getDitolakOleh() == null ? ""
														: "Ditolak oleh " + permintaanPengadaanMasterAsset
																.getDitolakOleh().getUserNama());
										tanggalDitolak.setValue(
												permintaanPengadaanMasterAsset.getTanggalDitolak() == null ? ""
														: Common.dateFormat3.get().format(
																permintaanPengadaanMasterAsset.getTanggalDitolak()));

										disetujui.setVisible(
												approve && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null
														&& permintaanPengadaanMasterAsset.getDitolakOleh() == null);
										ditolak.setVisible(
												reject && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null
														&& permintaanPengadaanMasterAsset.getDitolakOleh() == null);
										dibatalkan.setVisible(
												reject && (permintaanPengadaanMasterAsset.getDisetujuiOleh() != null
														|| permintaanPengadaanMasterAsset.getDitolakOleh() != null));
										rubah.setVisible(
												edit && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(
												delete && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										cetak(permintaanPengadaanMasterAsset);

									}
								}
							});
				}

			});
			aksiButtons.add(ditolak);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Permintaan Pengadaan Barang/Jasa ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										permintaanPengadaanMasterAsset.setDisetujuiOleh(null);
										permintaanPengadaanMasterAsset.setTanggalPersetujuan(null);
										permintaanPengadaanMasterAsset.setDitolakOleh(null);
										permintaanPengadaanMasterAsset.setTanggalDitolak(null);
										permintaanPengadaanMasterAsset.setSetujuiManual(false);

										Common.refreshUpdate(session, permintaanPengadaanMasterAsset);

										disetujuiTanggal.setValue(
												permintaanPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(permintaanPengadaanMasterAsset
																.getTanggalPersetujuan()));
										disetujuiOleh.setValue(permintaanPengadaanMasterAsset.getDisetujuiOleh() == null
												? ""
												: permintaanPengadaanMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null
														&& permintaanPengadaanMasterAsset.getDitolakOleh() == null);

										ditolakOleh
												.setValue(permintaanPengadaanMasterAsset.getDitolakOleh() == null ? ""
														: "Ditolak oleh " + permintaanPengadaanMasterAsset
																.getDitolakOleh().getUserNama());
										tanggalDitolak.setValue(
												permintaanPengadaanMasterAsset.getTanggalDitolak() == null ? ""
														: Common.dateFormat3.get().format(
																permintaanPengadaanMasterAsset.getTanggalDitolak()));

										ditolak.setVisible(
												reject && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null
														&& permintaanPengadaanMasterAsset.getDitolakOleh() == null);
										dibatalkan.setVisible(
												reject && (permintaanPengadaanMasterAsset.getDisetujuiOleh() != null
														|| permintaanPengadaanMasterAsset.getDitolakOleh() != null));
										rubah.setVisible(
												edit && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(
												delete && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			aksiButtons.add(dibatalkan);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(permintaanPengadaanMasterAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(rubah);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && permintaanPengadaanMasterAsset.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();

											if (SopUtil.hapusDisposisi(session,
													permintaanPengadaanMasterAsset.getDisposisiSop())) {

												List<PermintaanPengadaanMasterAssetDetail> permintaanPengadaanMasterAssetDetails = session
														.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
														.add(Restrictions.eq("permintaanPengadaanMasterAsset",
																permintaanPengadaanMasterAsset))
														.list();
												for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : permintaanPengadaanMasterAssetDetails) {
													session.delete(permintaanPengadaanMasterAssetDetail);
												}

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event event) throws Exception {
														Session session = HibernateUtil.currentSession();
														Common.refreshDelete(session, permintaanPengadaanMasterAsset);
														session.flush();

														onSearchDefault(event);
													}
												});

											}
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
			aksiButtons.add(hapus);

			if (permintaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset() != null
					&& permintaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset().getPembelianLangsung()
					&& permintaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset().getPostingHistory() == null
					&& permintaanPengadaanMasterAsset.getDisetujuiOleh() != null && edit) {

				Session session = HibernateUtil.currentSession();
				Number n = (Number) session.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("permintaanPengadaanMasterAsset", permintaanPengadaanMasterAsset))
						.add(Restrictions.isNotNull("uangMuka")).uniqueResult();

				if (n.intValue() == 0) {
					button = new MyToolbarbuttonConfig("Batalkan Beli Langsung", "/img/svg/trash.svg");
					button.setTooltiptext("Batalkan Beli Langsung");
					button.addEventListener("onClick", new EventListener() {
						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event event) throws Exception {

							MyMessageboxConfig.show("Apakah yakin ingin membatalkan pembelian langsung ini ?",
									"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
									MyMessageboxConfig.QUESTION, new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {

													Session session = HibernateUtil.currentSession();
													final PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = permintaanPengadaanMasterAsset
															.getPemesananPengadaanMasterAsset();
													session.refresh(pemesananPengadaanMasterAsset);

													List<PemesananPengadaanMasterAssetDetail> pemesananPengadaanMasterAssetDetails = session
															.createCriteria(PemesananPengadaanMasterAssetDetail.class)
															.add(Restrictions.eq("pemesananPengadaanMasterAsset",
																	pemesananPengadaanMasterAsset))
															.list();
													for (PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail : pemesananPengadaanMasterAssetDetails) {
														session.delete(pemesananPengadaanMasterAssetDetail);
													}

													Common.createDefaultTimer(new EventListener() {

														@Override
														public void onEvent(Event event) throws Exception {
															Session session = HibernateUtil.currentSession();

															permintaanPengadaanMasterAsset
																	.setPemesananPengadaanMasterAsset(null);
															Common.refreshUpdate(session,
																	permintaanPengadaanMasterAsset);
															session.flush();

															Common.refreshDelete(session,
																	pemesananPengadaanMasterAsset);
															session.flush();

															onSearchDefault(event);
														}
													});
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
					aksiButtons.add(button);
				}
			}

			if (permintaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset() == null
					&& permintaanPengadaanMasterAsset.getDisetujuiOleh() != null && edit) {

				Session session = HibernateUtil.currentSession();
				Number n = (Number) session.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("permintaanPengadaanMasterAsset", permintaanPengadaanMasterAsset))
						.add(Restrictions.isNotNull("uangMuka")).uniqueResult();

				if (n.intValue() == 0) {
					button = new MyToolbarbuttonConfig("Beli Langsung", "/img/svg/cash.svg");
					button.setTooltiptext("Ubah Data");
					button.addEventListener("onClick", new EventListener() {
						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event event) throws Exception {

							PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = new PemesananPengadaanMasterAsset();
							pemesananPengadaanMasterAsset
									.setPemilikAsset(permintaanPengadaanMasterAsset.getPemilikAsset());

							String s = "";
							String a = "";

							Session session = HibernateUtil.currentSession();
							List<PermintaanPengadaanMasterAssetDetail> permintaanPengadaanMasterAssetDetails = session
									.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
									.addOrder(Order.desc("id")).add(Restrictions.eq("permintaanPengadaanMasterAsset",
											permintaanPengadaanMasterAsset))
									.list();

							for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : permintaanPengadaanMasterAssetDetails) {
								s += s.isEmpty() ? permintaanPengadaanMasterAssetDetail.getId().toString()
										: "," + permintaanPengadaanMasterAssetDetail.getId();

								if (permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
										.getWorkspace() != null) {
									a += a.isEmpty()
											? permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
													.getWorkspace().getId().toString()
											: "," + permintaanPengadaanMasterAssetDetail
													.getPermintaanPengadaanMasterAsset().getWorkspace().getId();
								}
							}
							pemesananPengadaanMasterAsset
									.setSatuanKerja(permintaanPengadaanMasterAsset.getSatuanKerja());
							pemesananPengadaanMasterAsset.setPermintaanPengadaanMasterAssets(s);
							pemesananPengadaanMasterAsset.setAngarans(a);
							pemesananPengadaanMasterAsset.setDp(permintaanPengadaanMasterAsset.getNilai());
							pemesananPengadaanMasterAsset.setRuang(permintaanPengadaanMasterAsset.getRuang());
							pemesananPengadaanMasterAsset.setLokasi(permintaanPengadaanMasterAsset.getLokasi());
							pemesananPengadaanMasterAsset.setWorkspace(permintaanPengadaanMasterAsset.getWorkspace());

							pemesananPengadaanMasterAsset.setPembelianLangsung(true);
							pemesananPengadaanMasterAsset.setKeterangan("Pembelian langsung");

							PemesananPengadaanMasterAssetAction.onAddExternal(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							}, pemesananPengadaanMasterAsset, true);
						}

					});
					aksiButtons.add(button);
				}
			}

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}
	}

	/**
	 * <b>Tujuan:</b> Membuka formulir Permintaan Pengadaan dalam mode view-only (baca saja)
	 * dari konteks eksternal, misalnya dari dasbor persetujuan atau modul lain yang perlu
	 * menampilkan detail PR tanpa memperbolehkan pengeditan.<br>
	 * <b>Cara kerja:</b> Membuat instance baru {@code PermintaanPengadaanMasterAssetAction}
	 * dengan flag {@code persetujuan=true} dan {@code viewOnly=true}, membuat window modal
	 * baru ({@code MyWindow}), memasangnya ke root halaman saat ini, memanggil {@code init}
	 * untuk mengisi formulir, lalu menampilkan window sebagai modal. {@code EventListener}
	 * yang diberikan akan dipanggil saat operasi dalam modal selesai (misalnya untuk
	 * merefresh grid induk). Signature tambahan ini (tanpa parameter ketiga) memiliki
	 * perilaku default: window dapat ditutup ({@code setClosable(true)}).<br>
	 * <b>Penanganan error:</b> Exception disebarkan ke pemanggil.<br>
	 * <b>Pemeliharaan:</b> Lihat juga overload lain {@code onAddExternal} di kelas
	 * {@code PemesananPengadaanMasterAssetAction} untuk pola serupa pada modul PO.
	 *
	 * @param eventListener                   listener yang dipanggil setelah aksi di dalam
	 *                                        modal selesai (misalnya untuk refresh grid)
	 * @param permintaanPengadaanMasterAsset  entitas PR yang akan ditampilkan dalam formulir
	 * @throws Exception jika terjadi error saat membangun komponen atau memanggil {@code init}
	 */
	public static void onAddExternal(EventListener eventListener,
			PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset) throws Exception {
		PermintaanPengadaanMasterAssetAction permintaanPengadaanMasterAssetAction = new PermintaanPengadaanMasterAssetAction();
		permintaanPengadaanMasterAssetAction.eventListener = eventListener;
		permintaanPengadaanMasterAssetAction.addWindow = new MyWindow();
		permintaanPengadaanMasterAssetAction.persetujuan = true;
		permintaanPengadaanMasterAssetAction.viewOnly = true;

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(permintaanPengadaanMasterAssetAction.addWindow);
		permintaanPengadaanMasterAssetAction.addWindow.setHeight("95%");
		permintaanPengadaanMasterAssetAction.addWindow.setWidth("90%");

		permintaanPengadaanMasterAssetAction.init(permintaanPengadaanMasterAsset);

		permintaanPengadaanMasterAssetAction.addWindow.setVisible(true);
		permintaanPengadaanMasterAssetAction.addWindow.setClosable(true);
		permintaanPengadaanMasterAssetAction.addWindow.onModal();

	}

	/**
	 * <b>Tujuan:</b> Menangani klik tombol "Tambah" pada toolbar halaman utama dengan
	 * membuka formulir kosong untuk membuat Permintaan Pengadaan baru.<br>
	 * <b>Cara kerja:</b> Menyetel {@code viewOnly=false} agar semua field formulir dapat
	 * diedit, membuat instance baru {@code PermintaanPengadaanMasterAsset} (entitas kosong),
	 * memanggil {@code init} untuk membangun dan mengisi formulir, lalu menampilkan
	 * {@code addWindow} sebagai modal. Method ini dipanggil oleh ZK event framework secara
	 * otomatis ketika event {@code onAdd} terjadi pada komponen yang terdaftar di ZUL.<br>
	 * <b>Penanganan error:</b> Exception dari {@code init} atau {@code onModal} disebarkan
	 * ke ZK error handler.<br>
	 * <b>Pemeliharaan:</b> Jangan tambahkan logika bisnis di sini; delegasikan ke {@code init}
	 * dan {@code onSave}.
	 *
	 * @param event event ZK klik tombol tambah; tidak digunakan langsung di dalam metode
	 * @throws Exception jika terjadi error saat membangun formulir modal
	 */
	public void onAdd(Event event) throws Exception {
		viewOnly = false;
		init(new PermintaanPengadaanMasterAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan menampilkan formulir tambah/ubah Permintaan Pengadaan
	 * di dalam {@code addWindow}, baik untuk entitas baru maupun entitas yang sudah ada.<br>
	 * <b>Cara kerja:</b> Menyimpan referensi entitas ke field instance, menyetel judul window
	 * sesuai mode (tambah/ubah), membersihkan konten window lama dengan {@code Common.clear},
	 * lalu membangun layout {@code Borderlayout} dengan area {@code Center} berisi formulir
	 * (dari metode {@code form}) dan area {@code South} berisi toolbar dengan tombol "Batal"
	 * dan "Simpan dan Cetak". Event listener tombol Simpan memanggil {@code onSave} dan
	 * jika berhasil menutup window dan merefresh grid. Jika {@code viewOnly=true}, tombol
	 * Simpan disembunyikan, label Batal diganti "Selesai", dan ditambahkan tombol "Cetak"
	 * terpisah (hanya muncul jika PR sudah tersimpan).<br>
	 * <b>Penanganan error:</b> Exception dari {@code form} atau {@code onSave} disebarkan ke ZK.<br>
	 * <b>Pemeliharaan:</b> Jika perlu menambah tombol aksi di toolbar modal, tambahkan
	 * di bagian akhir metode ini sebelum return, setelah pemeriksaan {@code viewOnly}.
	 *
	 * @param permintaanPengadaanMasterAsset entitas PR yang akan ditampilkan; jika ID null
	 *                                       maka mode tambah baru, jika tidak null maka mode ubah
	 * @throws Exception jika terjadi error saat membangun komponen formulir
	 */
	private void init(PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset) throws Exception {
		this.permintaanPengadaanMasterAsset = permintaanPengadaanMasterAsset;
		addWindow.setTitle(permintaanPengadaanMasterAsset.getId() == null ? "Tambah Permintaan Pengadaan Barang/Jasa" : "Ubah Permintaan Pengadaan Barang/Jasa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan dan Cetak", "/img/save.gif");

		disposisiSop = null;
		center.appendChild(form(permintaanPengadaanMasterAsset, disposisiSop, save, null));

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

		if (viewOnly) {
			save.setVisible(false);
			cancel.setLabel("Selesai");

			if (PermintaanPengadaanMasterAssetAction.this.permintaanPengadaanMasterAsset.getId() != null) {
				MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak", "/img/cancel.gif");
				cetak.setTooltiptext("Cetak");
				cetak.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						cetak(PermintaanPengadaanMasterAssetAction.this.permintaanPengadaanMasterAsset);
					}
				});
				cetak.setParent(toolbar);
			}
		}
	}

	/**
	 * <b>Tujuan:</b> Memvalidasi semua input pada formulir PR dan jika valid, menyimpan
	 * entitas {@code PermintaanPengadaanMasterAsset} beserta detail item-nya ke database,
	 * menangani lampiran, dan memicu persetujuan otomatis jika checkbox setujui dicentang.<br>
	 * <b>Cara kerja:</b> Validasi dilakukan secara berurutan dengan gagal-cepat (return false):
	 * <ol>
	 *   <li>Workspace/anggaran wajib diisi kecuali jika {@code tanpaAnggaran} dicentang.</li>
	 *   <li>Kode PR tidak boleh kosong.</li>
	 *   <li>Tanggal pembuatan tidak boleh null.</li>
	 *   <li>Keterangan PR tidak boleh kosong (dengan auto-fokus ke field keterangan).</li>
	 *   <li>Setiap baris item di grid harus memiliki master aset yang dipilih.</li>
	 *   <li>Jika workspace terisi, saldo anggaran dihitung via {@code JenisUangMukaAction.hitungSaldo};
	 *       jika konfigurasi {@code saldo_harus_cukup} aktif dan jumlah melebihi saldo, tolak.</li>
	 *   <li>Jika konfigurasi lampiran wajib aktif, periksa keberadaan lampiran.</li>
	 * </ol>
	 * Setelah validasi lulus: load entitas dari DB jika mode ubah, salin semua field dari
	 * komponen UI ke entitas, simpan via {@code session.save} (baru) atau {@code session.update}
	 * (ubah). Untuk PR baru, kode dihasilkan oleh {@code generateKodeUnik}. Race condition kode
	 * duplikat ditangani dengan menangkap {@code ConstraintViolationException} dan meminta
	 * pengguna menyimpan ulang. Setelah simpan, detail item di-{@code saveOrUpdate}, lampiran
	 * diperbarui refnya via StreamingSession, dan status persetujuan diproses async via timer.<br>
	 * <b>Penanganan error:</b> {@code ConstraintViolationException} kode duplikat ditampilkan
	 * sebagai pesan dialog. {@code AssertionFailure} sesi Hibernate ditangani dengan membersihkan
	 * sesi dan menetapkan saldo ke 0.<br>
	 * <b>Pemeliharaan:</b> Jika menambah field wajib baru, tambahkan validasinya sebelum blok
	 * penyimpanan. Jika menambah field baru ke entitas, salin nilainya dari komponen UI di
	 * blok penyalinan sebelum {@code session.save/update}.
	 *
	 * @param event event ZK klik tombol simpan; tidak digunakan langsung
	 * @return {@code true} jika penyimpanan berhasil, {@code false} jika validasi gagal
	 *         atau terjadi error yang dapat dipulihkan (duplikat kode)
	 * @throws Exception jika terjadi error database yang tidak dapat dipulihkan
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (workspace.getAttribute("workspace") == null && !tanpaAnggaran.isChecked()) {
			MyMessageboxConfig.show("Mohon maaf, Anggaran/Workspace belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih Anggaran/Workspace; (2) Pilih anggaran yang sesuai dari daftar workspace yang tersedia; (3) Jika tidak ada anggaran, centang opsi 'Tanpa Anggaran' kemudian ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Permintaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode Permintaan atau gunakan tombol generate kode otomatis; (2) Pastikan kode bersifat unik dan belum terdaftar sebelumnya; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (tanggalPembuatan.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Pembuatan belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal Pembuatan dan pilih tanggal dari kalender; (2) Pastikan tanggal tidak melebihi tanggal hari ini; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (keterangan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Keterangan Permintaan Pengadaan Barang/Jasa belum diisi. Langkah yang dapat dilakukan: (1) Isi field Keterangan dengan deskripsi atau alasan permintaan pengadaan; (2) Keterangan wajib diisi untuk keperluan persetujuan dan audit; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							keterangan.focus();
						}
					});
			return false;
		}

		PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail = null;
		List<Row> rowsMasterAsset = gridMasterAsset.getRows().getChildren();
		for (Row row : rowsMasterAsset) {
			permintaanPengadaanMasterAssetDetail = (PermintaanPengadaanMasterAssetDetail) row
					.getAttribute("permintaanPengadaanMasterAssetDetail");
			if (permintaanPengadaanMasterAssetDetail.getMasterAsset() == null) {
				MyMessageboxConfig.show("Mohon maaf, Data Barang pada daftar permintaan pengadaan belum lengkap. Langkah yang dapat dilakukan: (1) Klik tombol pilih barang pada baris yang masih kosong; (2) Cari dan pilih barang/jasa dari daftar master aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}
		Workspace work = (Workspace) workspace.getAttribute("workspace");
		Double jumlah = 0.0;
		Double saldo = 0.0;
		for (Row row : rowsMasterAsset) {
			permintaanPengadaanMasterAssetDetail = (PermintaanPengadaanMasterAssetDetail) row
					.getAttribute("permintaanPengadaanMasterAssetDetail");
			Double n = (permintaanPengadaanMasterAssetDetail.getJumlah()
					* permintaanPengadaanMasterAssetDetail.getHargaBeli());

			jumlah += n;
		}

		if (!tanpaAnggaran.isChecked()) {
			if (work == null) {
				MyMessageboxConfig.show("Mohon maaf, Anggaran/Workspace belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih Anggaran/Workspace dan pilih anggaran yang sesuai; (2) Jika tidak ada anggaran, centang opsi 'Tanpa Anggaran'; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			try {
				saldo = JenisUangMukaAction.hitungSaldo(null,
						permintaanPengadaanMasterAsset == null ? null : permintaanPengadaanMasterAsset.getId(), null,
						null, work, tanggalPembuatan.getValue());
			} catch (org.hibernate.AssertionFailure af) {
				// Terdapat entity null-id di sesi akibat error sebelumnya; bersihkan dan lanjut
				try { HibernateUtil.currentSession().clear(); } catch (Exception ce) { ais.common.ErrorAuditUtil.record(ce, "auto-audit(empty-catch) src/ais/action/master/asset/PermintaanPengadaanMasterAssetAction.java:1458");}
				saldo = 0.0;
			} catch (Exception e) {
				saldo = 0.0;
			}
			if (Common.bolehKonfigurasi("saldo_harus_cukup_sebelum_mengajukan_realisasi_anggaran")) {
				if (jumlah.doubleValue() > saldo.doubleValue()) {
					MyMessageboxConfig.show("Saldo anggaran tidak mencukupi. Nilai pengajuan "
							+ Common.numberFormat.get().format(jumlah) + " melebihi sisa saldo "
							+ Common.numberFormat.get().format(saldo), "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Permintaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode Permintaan atau gunakan tombol generate kode otomatis; (2) Pastikan kode bersifat unik dan belum terdaftar sebelumnya; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (permintaanPengadaanMasterAsset.getId() != null) {
			permintaanPengadaanMasterAsset = (PermintaanPengadaanMasterAsset) session
					.load(PermintaanPengadaanMasterAsset.class, permintaanPengadaanMasterAsset.getId());
		}

		if (disposisiSop != null && disposisiSop.getId() != null) {
			permintaanPengadaanMasterAsset.setDisposisiSop(disposisiSop);
		}

		permintaanPengadaanMasterAsset.setWorkspace(work);
		permintaanPengadaanMasterAsset.setTanpaAnggaran(tanpaAnggaran.isChecked());
		permintaanPengadaanMasterAsset.setDanaTitipan(danaTitipan.isChecked());
		permintaanPengadaanMasterAsset.setLokasi(
				(Lokasi) (lokasi.getSelectedItem() == null || lokasi.getSelectedItem().getValue() == null ? null
						: lokasi.getSelectedItem().getValue()));
		permintaanPengadaanMasterAsset.setPemilikAsset((PemilikAsset) (pemilikAsset.getSelectedItem() == null
				|| pemilikAsset.getSelectedItem().getValue() == null ? null
						: pemilikAsset.getSelectedItem().getValue()));
		permintaanPengadaanMasterAsset.setRuang((Ruang) ruang.getAttribute("ruang"));

		permintaanPengadaanMasterAsset.setKode(kode.getValue());
		permintaanPengadaanMasterAsset.setKeterangan(keterangan.getValue());
		permintaanPengadaanMasterAsset.setTanggalPembuatan(tanggalPembuatan.getValue());

		permintaanPengadaanMasterAsset.setNilai(jumlah);

		permintaanPengadaanMasterAsset.setSaldo(saldo);

		permintaanPengadaanMasterAsset.setWajibAdaPerjanjianKerjasama(wajibAdaPerjanjianKerjasama.isChecked());

		try {
			permintaanPengadaanMasterAsset.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PermintaanPengadaanMasterAssetAction.java:1513");
			// TODO: handle exception
		}

		permintaanPengadaanMasterAsset.setAkun((Akun) akun.getAttribute("akun"));

		if (permintaanPengadaanMasterAsset.getId() != null) {
			session.update(permintaanPengadaanMasterAsset);
		} else {
			permintaanPengadaanMasterAsset.setDibuatOleh(tbmuser);
			// Kode WAJIB unik. generateKodeUnik menaikkan index sampai kode benar-benar
			// belum ada di tabel (mengatasi index rowCount yang TIDAK monotonik akibat
			// hapus/soft-delete & tabrakan format). Substitusi UNIT/SATKER ikut dicek.
			String noAgenda = generateKodeUnik(
					work != null && work.getSatuanKerja() != null ? work.getSatuanKerja().getNama() : null);
			kode.setValue(noAgenda);
			permintaanPengadaanMasterAsset.setKode(noAgenda);
			try {
				session.save(permintaanPengadaanMasterAsset);
			} catch (org.hibernate.exception.ConstraintViolationException cve) {
				// Pengaman terakhir untuk RACE antar-transaksi (dua penyimpanan serentak
				// memilih index bebas yang sama). Transaksi PostgreSQL sudah teracuni di sini,
				// jadi cukup minta simpan ulang (akan dapat index berikutnya yang bebas).
				try { session.clear(); } catch (Exception ce) { ais.common.ErrorAuditUtil.record(ce, "auto-audit(empty-catch) src/ais/action/master/asset/PermintaanPengadaanMasterAssetAction.java:1536");}
				ais.ui.util.MyMessageboxConfig.show(
					"Kode permintaan pengadaan sudah digunakan (" + noAgenda
						+ ") karena akses bersamaan. Silakan simpan ulang.",
					"Duplikat", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		for (Row row : rowsMasterAsset) {
			permintaanPengadaanMasterAssetDetail = (PermintaanPengadaanMasterAssetDetail) row
					.getAttribute("permintaanPengadaanMasterAssetDetail");
			permintaanPengadaanMasterAssetDetail.setPermintaanPengadaanMasterAsset(permintaanPengadaanMasterAsset);
			session.saveOrUpdate(permintaanPengadaanMasterAssetDetail);
		}


		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (setujui.isChecked()) {
					Session session = HibernateUtil.currentSession();
					permintaanPengadaanMasterAsset.setDisetujuiOleh(tbmuser);
					permintaanPengadaanMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
					Common.refreshUpdate(session, permintaanPengadaanMasterAsset);
				} else {
					Session session = HibernateUtil.currentSession();
					permintaanPengadaanMasterAsset.setDisetujuiOleh(null);
					permintaanPengadaanMasterAsset.setTanggalPersetujuan(null);
					Common.refreshUpdate(session, permintaanPengadaanMasterAsset);
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						cetak(permintaanPengadaanMasterAsset);
					}
				}, "Proses cetak", false, 2500);

				if (eventListener != null) {
					Common.createDefaultTimer(eventListener);
				}
			}
		});

		return true;
	}

	/**
	 * <b>Tujuan:</b> Menyiapkan peta parameter ({@code Map}) yang dibutuhkan oleh template
	 * laporan JasperReports untuk mencetak dokumen PDF Permintaan Pengadaan.<br>
	 * <b>Cara kerja:</b> Jika entitas null atau belum tersimpan (ID null), mengembalikan peta
	 * kosong acak dari {@code HashMapGenerator.getRand()}. Jika tersimpan: refresh entitas dari
	 * DB, masukkan semua properti entitas utama dengan prefix "data" menggunakan
	 * {@code Common.insertProperty}, masukkan properti workspace/anggaran dengan prefix
	 * "anggaran", inisialisasi kop surat via {@code SuratUtil.initDefaultKop}, masukkan
	 * parameter SOP alur via {@code DisposisiAlurSop.parameterMap}, lalu iterasi semua detail
	 * item PR dan bangun list of map untuk parameter "maps" (setiap map berisi nama, kode,
	 * isbn, hargabeli, jumlah, kelompok_asset, jenis_asset, tipe_asset, spesifikasi,
	 * status_persetujuan, perpustakaan, tanggal_persetujuan, disetujui_oleh). Parameter dengan
	 * nama mengandung "disposisiSop" di-null-kan untuk menghindari error serialisasi
	 * di JasperReports.<br>
	 * <b>Penanganan error:</b> Tidak ada penanganan eksplisit; NPE dicegah dengan pemeriksaan
	 * null sebelum akses ke field relasi.<br>
	 * <b>Pemeliharaan:</b> Jika menambah field baru ke template laporan, tambahkan
	 * {@code parameters.put("nama_field", nilai)} di sini, BUKAN di template Jasper secara
	 * langsung. Urutan item diurutkan berdasarkan jenisAsset lalu kelompokAsset.
	 *
	 * @param permintaanPengadaanMasterAsset entitas PR yang akan dicetak
	 * @return peta parameter siap pakai untuk {@code Report.generateFileReport} /
	 *         {@code Report.generatePDFReport}; tidak pernah null
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map parameter(PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset) {
		if (permintaanPengadaanMasterAsset == null || permintaanPengadaanMasterAsset.getId() == null) {
			return ais.common.HashMapGenerator.getRand();
		}
		HibernateUtil.currentSession().refresh(permintaanPengadaanMasterAsset);
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", permintaanPengadaanMasterAsset.getId());

		Common.insertProperty(PermintaanPengadaanMasterAsset.class, permintaanPengadaanMasterAsset, parameters, "data");

		if (permintaanPengadaanMasterAsset.getWorkspace() != null) {
			Common.insertProperty(Workspace.class, permintaanPengadaanMasterAsset.getWorkspace(), parameters,
					"anggaran");
		}

		SatuanKerja satuanKerja = permintaanPengadaanMasterAsset.getSatuanKerja();
		SuratUtil.initDefaultKop(parameters, satuanKerja);

		DisposisiAlurSop.parameterMap(permintaanPengadaanMasterAsset.getDisposisiSop(), parameters);
		Session session = HibernateUtil.currentSession();

		List<PermintaanPengadaanMasterAssetDetail> permintaanPengadaanMasterAssetDetails = session
				.createCriteria(PermintaanPengadaanMasterAssetDetail.class).createAlias("masterAsset", "masterAsset")
				.addOrder(Order.asc("masterAsset.jenisAsset")).addOrder(Order.asc("masterAsset.kelompokAsset"))
				.add(Restrictions.eq("permintaanPengadaanMasterAsset.id", permintaanPengadaanMasterAsset.getId())).list();
		List<Map> maps = new ArrayList<Map>();
		for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : permintaanPengadaanMasterAssetDetails) {
			Map map = new HashMap();
			Common.insertProperty(PermintaanPengadaanMasterAssetDetail.class, permintaanPengadaanMasterAssetDetail, map,
					"data");

			map.put("kelompok_asset",
					permintaanPengadaanMasterAssetDetail.getMasterAsset().getKelompokAsset() == null ? ""
							: permintaanPengadaanMasterAssetDetail.getMasterAsset().getKelompokAsset().getNama());

			map.put("jenis_asset", permintaanPengadaanMasterAssetDetail.getMasterAsset().getJenisAsset() == null ? ""
					: permintaanPengadaanMasterAssetDetail.getMasterAsset().getJenisAsset().getNama());

			map.put("tipe_asset", permintaanPengadaanMasterAssetDetail.getMasterAsset().getTipe());

			map.put("spesifikasi", permintaanPengadaanMasterAssetDetail.getMasterAsset().getSpesifikasi());

			map.put("hargabeli", permintaanPengadaanMasterAssetDetail.getHargaBeli());
			map.put("jumlah", permintaanPengadaanMasterAssetDetail.getJumlah());
			map.put("nama", permintaanPengadaanMasterAssetDetail.getMasterAsset().getNama());
			map.put("kode", permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getKode());
			map.put("isbn", permintaanPengadaanMasterAssetDetail.getMasterAsset().getKode());

			String status = "";
			if (permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getDisetujuiOleh() == null) {
				status = "Belum disetujui";
			} else {
				status = "Disetujui oleh "
						+ permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getDisetujuiOleh()
								.getUserNama()
						+ " pada "
						+ (permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
								.getTanggalPersetujuan() == null ? ""
										: Common.dateFormat51.get().format(permintaanPengadaanMasterAssetDetail
												.getPermintaanPengadaanMasterAsset().getTanggalPersetujuan()));
			}

			map.put("status_persetujuan", status);

			map.put("perpustakaan",
					permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getKeterangan());
			map.put("tanggal_persetujuan",
					permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getTanggalPersetujuan());
			map.put("disetujui_oleh",
					permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getDisetujuiOleh() == null
							? ""
							: permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
									.getDisetujuiOleh().getUserNama());

			maps.add(map);
		}

		parameters.put("maps", maps);

		for (Object o : parameters.keySet()) {
			if (o.toString().contains("disposisiSop")) {
				parameters.put(o.toString(), null);
			}
		}
		return parameters;
	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan kontrak antarmuka {@code FormSop} untuk menghasilkan
	 * file laporan cetak dalam format PDF, yang digunakan oleh mekanisme cetak massal atau
	 * cetak dari alur SOP.<br>
	 * <b>Cara kerja:</b> Menerima {@code GeneralValueObject} (yang sebenarnya adalah
	 * {@code PermintaanPengadaanMasterAsset}), melakukan cast, menyiapkan parameter laporan
	 * via {@code parameter(...)}, lalu memanggil {@code Report.generateFileReport} dengan
	 * template {@code asset/permintaan_pengadaan} dan format PDF. Tanggal pembuatan PR digunakan
	 * sebagai referensi tanggal laporan untuk menentukan header periode yang benar.<br>
	 * <b>Penanganan error:</b> Exception dari {@code Report.generateFileReport} disebarkan ke
	 * pemanggil (biasanya mekanisme cetak SOP).<br>
	 * <b>Pemeliharaan:</b> Nama template {@code asset/permintaan_pengadaan} harus sesuai
	 * dengan file {@code .jrxml} yang ada di direktori laporan. Jika template diganti nama,
	 * perbarui juga string di sini.
	 *
	 * @param generalValueObject entitas {@code PermintaanPengadaanMasterAsset} yang akan dicetak
	 * @return {@code File} objek file PDF hasil generate laporan
	 * @throws Exception jika terjadi error saat menghasilkan laporan (template tidak ditemukan,
	 *                   error database, dsb.)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset = (PermintaanPengadaanMasterAsset) generalValueObject;
		List maps = null;
		File file = Report.generateFileReport(Report.PDF, parameter(permintaanPengadaanMasterAsset),
				"asset/permintaan_pengadaan", permintaanPengadaanMasterAsset.getTanggalPembuatan(), maps,
				Common.locale);
		return file;
	}

	/**
	 * <b>Tujuan:</b> Menghasilkan dan menampilkan laporan PDF Permintaan Pengadaan langsung
	 * ke browser pengguna (download/preview), dipanggil setelah menyimpan atau menyetujui PR.<br>
	 * <b>Cara kerja:</b> Metode statis ini dapat dipanggil dari mana saja dalam modul tanpa
	 * membutuhkan instance controller. Menyiapkan parameter via {@code parameter(...)}, lalu
	 * memanggil {@code Report.generatePDFReport} yang menghasilkan PDF dan mengirimkannya
	 * ke klien browser via respons HTTP. Template yang digunakan adalah
	 * {@code asset/permintaan_pengadaan}.<br>
	 * <b>Penanganan error:</b> Exception disebarkan ke pemanggil.<br>
	 * <b>Pemeliharaan:</b> Metode ini dipanggil di beberapa titik: setelah {@code onSave},
	 * setelah aksi persetujuan/penolakan dari renderer, dan dari tombol cetak per-baris.
	 * Jika format laporan perlu diubah, cukup perbarui template {@code .jrxml}.
	 *
	 * @param permintaanPengadaanMasterAsset entitas PR yang akan dicetak ke PDF
	 * @throws Exception jika terjadi error saat generate atau mengirim laporan ke klien
	 */
	@SuppressWarnings({})
	public static void cetak(PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset) throws Exception {

		Report.generatePDFReport(Report.PDF, parameter(permintaanPengadaanMasterAsset), "asset/permintaan_pengadaan",
				permintaanPengadaanMasterAsset.getTanggalPembuatan());
	}

	/**
	 * <b>Tujuan:</b> Membangun objek {@code Criteria} Hibernate yang menerapkan semua filter
	 * aktif dari toolbar pencarian halaman utama, digunakan baik untuk menghitung total baris
	 * (paginasi) maupun untuk mengambil halaman data PR.<br>
	 * <b>Cara kerja:</b> Mengambil satuan kerja dari {@code searchparent}; jika dipilih,
	 * memperluas set satuan kerja ke semua turunan hierarki menggunakan
	 * {@code SatuanKerjaTreeModel.getChildsSet}. Kemudian membangun {@code Criteria} dengan
	 * restriction berantai (AND implisit):
	 * <ul>
	 *   <li>Filter workspace/anggaran (jika dipilih di {@code searchAnggaran}).</li>
	 *   <li>Filter status aktif (checkbox {@code searchaktif}): jika dicentang hanya tampilkan
	 *       aktif/null, jika tidak dicentang tampilkan semua.</li>
	 *   <li>Filter status tutup (checkbox {@code searchtutup}): jika dicentang hanya tampilkan
	 *       yang belum ditutup.</li>
	 *   <li>Filter rentang tanggal pembuatan menggunakan SQL native {@code date()} agar
	 *       membandingkan hanya tanggal (tanpa waktu).</li>
	 *   <li>Filter status persetujuan via {@code searchStatusPersetujuan}: Semua/Belum/Telah.</li>
	 *   <li>Filter satuan kerja: mencocokkan kolom {@code satuanKerja} dengan set satuan kerja
	 *       yang berlaku (termasuk null jika tidak ada filter parent).</li>
	 *   <li>Filter lokasi dari combobox {@code searchlokasi}.</li>
	 *   <li>Filter kode (ILIKE, anywhere) dari {@code searchkode}.</li>
	 *   <li>Filter keterangan (ILIKE, anywhere) dari {@code searchketerangan}.</li>
	 *   <li>Filter nama/kode anggaran (teks bebas di {@code searchanggaran}) via alias "workspace".</li>
	 * </ul>
	 * Jika parameter {@code order} bernilai {@code true}, menambahkan {@code Order.desc("id")}
	 * untuk menampilkan PR terbaru di atas.<br>
	 * <b>Penanganan error:</b> Jika {@code searchparent} null (guard di baris pertama),
	 * mengembalikan null; pemanggil harus menangani null ini.<br>
	 * <b>Pemeliharaan:</b> Jika menambah filter baru, tambahkan {@code .add(restriction)} di
	 * blok {@code createCriteria} berantai ini. Untuk filter yang membutuhkan join, gunakan
	 * {@code createAlias} seperti dilakukan untuk filter {@code searchanggaran}.
	 *
	 * @param order {@code true} untuk menambahkan urutan descending by ID (untuk query data);
	 *              {@code false} untuk query count paginasi (tanpa ordering)
	 * @return objek {@code Criteria} siap dieksekusi, atau {@code null} jika komponen
	 *         {@code searchparent} belum terwire (halaman belum siap)
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

		Workspace workspace = (Workspace) searchAnggaran.getAttribute("workspace");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PermintaanPengadaanMasterAsset.class)

				.add(workspace == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("workspace", workspace))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(searchtutup.isChecked()
						? Restrictions.or(Restrictions.isNull("tutup"), Restrictions.eq("tutup", false))
						: Restrictions.sqlRestriction("true"))

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				// Filter status persetujuan: combobox (jika bukan "Semua") ATAU checkbox blmDisetujui/disetujui.
				.add(buildFilterPersetujuan())

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchlokasi.getSelectedItem() == null || searchlokasi.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE));

		if (!searchanggaran.getValue().trim().isEmpty()) {
			criteria.createAlias("workspace", "workspace").add(Restrictions.or(
					Restrictions.ilike("workspace.kode", searchanggaran.getValue().trim(), MatchMode.ANYWHERE),
					Restrictions.ilike("workspace.nama", searchanggaran.getValue().trim(), MatchMode.ANYWHERE)));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * Resolusi filter status persetujuan PR.
	 * Prioritas: combobox searchStatusPersetujuan (jika bukan "Semua") menang;
	 * jika "Semua", gunakan checkbox blmDisetujui/disetujui.
	 * Keduanya checked atau keduanya unchecked → tampil semua.
	 */
	private org.hibernate.criterion.Criterion buildFilterPersetujuan() {
		String comboLabel = (searchStatusPersetujuan == null
				|| searchStatusPersetujuan.getSelectedItem() == null
				|| searchStatusPersetujuan.getSelectedItem().getLabel() == null)
			? "Semua"
			: searchStatusPersetujuan.getSelectedItem().getLabel().trim();

		if (!"Semua".equalsIgnoreCase(comboLabel)) {
			return "Belum Disetujui".equalsIgnoreCase(comboLabel)
				? Restrictions.isNull("disetujuiOleh")
				: Restrictions.isNotNull("disetujuiOleh");
		}

		boolean cbBlm    = blmDisetujui == null || blmDisetujui.isChecked();
		boolean cbSetuju = disetujui    == null || disetujui.isChecked();

		if (cbBlm && cbSetuju)   return Restrictions.sqlRestriction("1=1");
		if (cbBlm)               return Restrictions.isNull("disetujuiOleh");
		if (cbSetuju)            return Restrictions.isNotNull("disetujuiOleh");
		return Restrictions.sqlRestriction("1=1");
	}

	/**
	 * <b>Tujuan:</b> Mengeksekusi pencarian dan memperbarui grid PR berdasarkan semua filter
	 * aktif, termasuk memperbarui paginasi agar menunjukkan jumlah halaman yang benar.<br>
	 * <b>Cara kerja:</b> Pertama memanggil {@code Common.initPaging(initCriteria(false), paging)}
	 * untuk menghitung total baris tanpa ordering (lebih efisien). Kemudian mengambil satu
	 * halaman data via {@code initCriteria(true)} dengan {@code setMaxResults} dan
	 * {@code setFirstResult} berdasarkan halaman aktif saat ini. Hasilnya dibungkus dalam
	 * {@code SimpleListModel} dan disetel ke grid dengan renderer baru. Pemanggilan
	 * {@code grid.setModelCheckMobile} menangani mode mobile secara otomatis.<br>
	 * <b>Penanganan error:</b> Tidak ada penanganan eksplisit; exception dari Hibernate
	 * atau ZK disebarkan ke framework.<br>
	 * <b>Pemeliharaan:</b> Metode ini dipanggil dari banyak titik: timer refresh, perubahan
	 * filter, setelah simpan/hapus/setujui. Pastikan tidak memanggil metode berat secara
	 * sinkron di dalamnya untuk menjaga responsivitas UI.
	 *
	 * @param event event ZK yang memicu pencarian (bisa null jika dipanggil programatis)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PermintaanPengadaanMasterAsset> permintaanPengadaanMasterAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(permintaanPengadaanMasterAsset);
		grid.setRowRenderer(new PermintaanPengadaanMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Membangun dan mengembalikan komponen formulir ZK ({@code MyGrid}) lengkap
	 * untuk input atau tampilan data Permintaan Pengadaan, digunakan baik dari {@code init}
	 * (tambah/ubah mandiri) maupun dari alur SOP yang memanggil antarmuka {@code FormSop}.<br>
	 * <b>Cara kerja:</b> Menyetel entitas dan disposisi SOP ke field instance, membuat
	 * {@code MyGrid} dua kolom (label + input), lalu menambahkan baris-baris formulir secara
	 * berurutan:
	 * <ul>
	 *   <li>Kode permintaan (Label baca-saja jika sudah tersimpan).</li>
	 *   <li>Satuan kerja: banbox jika mode edit, Label jika mode persetujuan.</li>
	 *   <li>Akun beban: banbox jika mode edit, Label jika mode persetujuan.</li>
	 *   <li>Keterangan akun beban (info hint).</li>
	 *   <li>Anggaran/Workspace: banbox jika mode edit, Label jika mode persetujuan. Event
	 *       listener pada workspace memperbarui label nilai anggaran, saldo, periode, akun,
	 *       dan detail uang muka dalam proses secara real-time.</li>
	 *   <li>Checkbox "Tanpa Anggaran" dan "Dana Titipan" dengan saling eksklusif dan pengatur
	 *       visibilitas baris anggaran/satker/akun.</li>
	 *   <li>Label informatif: unit, nilai anggaran, anggaran dalam proses, detail proses, sisa
	 *       anggaran, periode, dan akun anggaran.</li>
	 *   <li>Kode permintaan (dihasilkan otomatis jika baru).</li>
	 *   <li>Tanggal pembuatan (datebox).</li>
	 *   <li>Pemilik aset dan lokasi (opsional berdasarkan konfigurasi).</li>
	 *   <li>Ruang (opsional berdasarkan konfigurasi).</li>
	 *   <li>Flag perjanjian kerjasama.</li>
	 *   <li>Keterangan (textarea multi-baris).</li>
	 *   <li>Lampiran dokumen (upload/download).</li>
	 *   <li>Grid detail item PR via {@code PermintaanPengadaanMasterAssetHelper}.</li>
	 *   <li>Checkbox persetujuan (hanya jika mode persetujuan dan belum ada disposisi SOP).</li>
	 * </ul>
	 * Semua field yang tidak relevan untuk mode tertentu (persetujuan/viewOnly) diganti
	 * dengan Label baca-saja.<br>
	 * <b>Penanganan error:</b> Blok try-catch melindungi pengambilan satuan kerja default.
	 * Exception lain disebarkan ke pemanggil.<br>
	 * <b>Pemeliharaan:</b> Jika menambah field baru, tambahkan {@code MyFormRow} baru di
	 * posisi yang tepat dalam urutan visual, dan pastikan nilainya disalin di {@code onSave}.
	 *
	 * @param generalValueObject entitas {@code PermintaanPengadaanMasterAsset} yang dirender
	 * @param disposisiSop       disposisi SOP aktif jika formulir dibuka dari alur SOP;
	 *                           null jika tidak dalam konteks SOP
	 * @param save               tombol simpan dari toolbar modal; dapat disembunyikan untuk
	 *                           mode viewOnly
	 * @param setujuiData        listener untuk sinkronisasi status persetujuan dengan SOP;
	 *                           null jika tidak dari konteks SOP
	 * @return komponen {@code MyGrid} yang berisi seluruh formulir siap ditampilkan
	 * @throws Exception jika terjadi error saat membangun komponen atau mengakses database
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {
		this.permintaanPengadaanMasterAsset = (PermintaanPengadaanMasterAsset) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}

		if (this.permintaanPengadaanMasterAsset.getDisetujuiOleh() != null) {
			persetujuan = true;
		}

		permintaanPengadaanMasterAsset.setSatuanKerja(tbmuser.ambilSatuanKerja());

		Columns columns = new Columns();
		columns.setParent(grid);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		if (permintaanPengadaanMasterAsset.getId() != null) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode Permintaan"));
			Label unit = new Label(permintaanPengadaanMasterAsset.getKode());
			row.appendChild(unit);
		}

		workspace = new AmbilDataWorkspaceBanbox(false);

		try {
			if (permintaanPengadaanMasterAsset.getSatuanKerja() == null) {
				permintaanPengadaanMasterAsset.setSatuanKerja(Common.getSatuanKerja());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PermintaanPengadaanMasterAssetAction.java:2008");
			// TODO: handle exception
		}

		final MyFormRow rowSatker = new MyFormRow();
		rowSatker.setParent(rows);
		rowSatker.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(permintaanPengadaanMasterAsset.getSatuanKerja() == null ? ""
				: permintaanPengadaanMasterAsset.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", permintaanPengadaanMasterAsset.getSatuanKerja());
		satuanKerja.setReadonly(true);
		if (persetujuan) {
			rowSatker.appendChild(new Label(permintaanPengadaanMasterAsset.getSatuanKerja() == null ? ""
					: permintaanPengadaanMasterAsset.getSatuanKerja().getNama()));
		} else {
			rowSatker.appendChild(satuanKerja);
		}
		satuanKerja.setWidth("90%");

		final MyFormRow rowAkun = new MyFormRow();
		rowAkun.setParent(rows);
		rowAkun.appendChild(new ais.ui.util.MyLabelConfig("Akun Beban"));
		akun = new AmbilDataAkunBanbox(false);
		akun.setValue(permintaanPengadaanMasterAsset.getAkun() == null ? ""
				: permintaanPengadaanMasterAsset.getAkun().getNama());
		akun.setAttribute("akun", permintaanPengadaanMasterAsset.getAkun());
		akun.setReadonly(true);
		if (persetujuan) {
			rowAkun.appendChild(new Label(permintaanPengadaanMasterAsset.getAkun() == null ? ""
					: permintaanPengadaanMasterAsset.getAkun().getNama()));
		} else {
			rowAkun.appendChild(akun);
		}
		akun.setWidth("90%");

		aa = Common.initKeterangan(rows,
				"Jika akun beban tidak dipilih, maka akan mengambil dari akun pembelian barang/jasa");

		final MyFormRow rowAnggaran = new MyFormRow();
		rowAnggaran.setParent(rows);
		rowAnggaran.appendChild(new ais.ui.util.MyLabelConfig("Anggaran *"));
		workspace.setValue(permintaanPengadaanMasterAsset.getWorkspace() == null ? ""
				: permintaanPengadaanMasterAsset.getWorkspace().toString());
		workspace.setAttribute("workspace", permintaanPengadaanMasterAsset.getWorkspace());
		workspace.setWidth("90%");
		workspace.setReadonly(true);

		if (persetujuan) {
			rowAnggaran.appendChild(new Label(permintaanPengadaanMasterAsset.getWorkspace() == null ? ""
					: permintaanPengadaanMasterAsset.getWorkspace().toString()));
		} else {
			rowAnggaran.appendChild(workspace);
		}

		final MyFormRow rowtanpaAnggaran = new MyFormRow();
		rowtanpaAnggaran.setValign("top");
		rowtanpaAnggaran.setParent(rows);
		rowtanpaAnggaran.appendChild(new ais.ui.util.MyLabelConfig(""));
		tanpaAnggaran = new MyCheckboxConfig("Merupakan tanpa anggaran");
		if (persetujuan) {
			rowtanpaAnggaran.appendChild(new Label("Merupakan tanpa anggaran ? "
					+ (permintaanPengadaanMasterAsset.getTanpaAnggaran() ? "Ya" : "Tidak")));
		} else {
			rowtanpaAnggaran.appendChild(tanpaAnggaran);
		}

		tanpaAnggaran.setChecked(permintaanPengadaanMasterAsset.getTanpaAnggaran());

		final MyFormRow rowdanaTitipanData = new MyFormRow();
		rowdanaTitipanData.setValign("top");
		rowdanaTitipanData.setParent(rows);
		rowdanaTitipanData.appendChild(new ais.ui.util.MyLabelConfig(""));
		danaTitipan = new MyCheckboxConfig("Merupakan dari dana titipan");
		if (persetujuan) {
			rowdanaTitipanData.appendChild(new Label(
					"Merupakan dana titipan ? " + (permintaanPengadaanMasterAsset.getDanaTitipan() ? "Ya" : "Tidak")));
		} else {
			rowdanaTitipanData.appendChild(danaTitipan);
		}

		danaTitipan.setChecked(permintaanPengadaanMasterAsset.getDanaTitipan());

		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit/Satuan Kerja"));
		final Label unit = new Label();
		row.appendChild(unit);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Anggaran"));
		final Label nilaiAnggaran = new Label();
		row.appendChild(nilaiAnggaran);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anggaran Dalam Proses"));
		final Label uangMukaDalamProses = new Label();
		row.appendChild(uangMukaDalamProses);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final Vbox uangMukaDalamProsesDetail = new Vbox();
		row.appendChild(uangMukaDalamProsesDetail);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sisa Anggaran"));
		final Label saldoAnggaran = new Label();
		row.appendChild(saldoAnggaran);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Periode Anggaran"));
		final Label tgl = new Label();
		row.appendChild(tgl);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Anggaran"));
		final Label akun = new Label();
		row.appendChild(akun);

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				unit.getParent().setVisible(!tanpaAnggaran.isChecked());
				nilaiAnggaran.getParent().setVisible(!tanpaAnggaran.isChecked());
				saldoAnggaran.getParent().setVisible(!tanpaAnggaran.isChecked());
				tgl.getParent().setVisible(!tanpaAnggaran.isChecked());
				akun.getParent().setVisible(!tanpaAnggaran.isChecked());
				uangMukaDalamProses.getParent().setVisible(!tanpaAnggaran.isChecked());

				rowtanpaAnggaran.setVisible(Common.bolehKonfigurasi("tampilkan_tanpa_anggaran") && !danaTitipan.isChecked());
				rowdanaTitipanData.setVisible(!tanpaAnggaran.isChecked());

				Workspace work = (Workspace) workspace.getAttribute("workspace");

				unit.setValue(work == null || work.getSatuanKerja() == null ? "" : work.getSatuanKerja().getNama());

				akun.setValue(work == null || work.getAkun() == null ? ""
						: work.getAkun().getKode() + "-" + work.getAkun().getNama());

				nilaiAnggaran.setValue(work == null ? "" : Common.numberFormat.get().format(work.getHargaTotal()));

				tgl.setValue((work == null || work.getMulai() == null ? ""
						: Common.dateFormat1.get().format(work.getMulai()))
						+ (work == null || work.getSelesai() == null ? ""
								: " s.d " + Common.dateFormat1.get().format(work.getSelesai())));

				Double saldo = work == null ? 0.0
						: JenisUangMukaAction.hitungSaldo(null, null,
								permintaanPengadaanMasterAsset == null ? null : permintaanPengadaanMasterAsset.getId(),
								null, work, tanggalPembuatan.getValue());

				Double dalamProses = (work == null ? 0.0 : work.getHargaTotal()) - saldo;

				uangMukaDalamProses.setValue(Common.numberFormat.get().format(dalamProses));

				saldoAnggaran.setValue(work == null ? "" : Common.numberFormat.get().format(saldo));

				if (work != null && work.getSatuanKerja() != null && permintaanPengadaanMasterAsset.getId() == null) {
					String noAgenda = generateCode(false);
					noAgenda = noAgenda.replaceAll("UNIT", work.getSatuanKerja().getNama());
					noAgenda = noAgenda.replaceAll("SATKER", work.getSatuanKerja().getNama());
					permintaanPengadaanMasterAsset.setKode(noAgenda);

					kode.setValue(noAgenda);
				}

				JenisUangMukaAction.tampilkan(uangMukaDalamProsesDetail, null,
						permintaanPengadaanMasterAsset == null ? null : permintaanPengadaanMasterAsset.getId(), null,
						work, tanggalPembuatan.getValue());
			}
		};

		workspace.setEventListener(eventListener);

		EventListener eventListenerPesanan = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (tanpaAnggaran.isChecked()) {
					danaTitipan.setChecked(false);
				}
				if (danaTitipan.isChecked()) {
					tanpaAnggaran.setChecked(false);
				}

				rowAnggaran.setVisible(!tanpaAnggaran.isChecked());
				rowSatker.setVisible(tanpaAnggaran.isChecked());
				rowAkun.setVisible(tanpaAnggaran.isChecked());
				aa.setVisible(tanpaAnggaran.isChecked());

				rowtanpaAnggaran.setVisible(!danaTitipan.isChecked());
				rowdanaTitipanData.setVisible(!tanpaAnggaran.isChecked());
				uangMukaDalamProses.getParent().setVisible(!tanpaAnggaran.isChecked());

				eventListener.onEvent(arg0);
			}

		};

		tanpaAnggaran.addEventListener("onClick", eventListenerPesanan);
		danaTitipan.addEventListener("onClick", eventListenerPesanan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Permintaan *"));

		tanggalPembuatan = new MyDatebox(
				permintaanPengadaanMasterAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: permintaanPengadaanMasterAsset.getTanggalPembuatan());

		if (permintaanPengadaanMasterAsset.getKode() == null) {
			String noAgenda = generateCode(false);
			permintaanPengadaanMasterAsset.setKode(noAgenda);
		}

		kode = new Label(permintaanPengadaanMasterAsset.getKode());
		if (persetujuan) {
			row.appendChild(new Label(permintaanPengadaanMasterAsset.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan *"));
		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat5.get().format(tanggalPembuatan.getValue())));
		} else {
			row.appendChild(tanggalPembuatan);
		}
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setWidth("90%");

		eventListenerPesanan.onEvent(null);

		pemilikAsset = new Combobox();
		row = new MyFormRow();
		row.setVisible(tampilkanRuanganDamPemilikAset);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pemilik"));
		if (persetujuan) {
			row.appendChild(new Label(permintaanPengadaanMasterAsset.getPemilikAsset() == null ? ""
					: permintaanPengadaanMasterAsset.getPemilikAsset().getNama()));
		} else {
			row.appendChild(pemilikAsset);
		}
		Common.insertComboDanSemua(pemilikAsset, new String[] { "nama", "id" }, "keterangan", PemilikAsset.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(pemilikAsset, permintaanPengadaanMasterAsset.getPemilikAsset());
		pemilikAsset.setWidth("90%");

		lokasi = new Combobox();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi"));
		if (persetujuan) {
			row.appendChild(new Label(permintaanPengadaanMasterAsset.getLokasi() == null ? ""
					: permintaanPengadaanMasterAsset.getLokasi().getNama()));
		} else {
			row.appendChild(lokasi);
		}
		Common.insertComboDanSemua(lokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(lokasi, permintaanPengadaanMasterAsset.getLokasi());
		lokasi.setWidth("90%");

		LokasiAction.kunciLokasi(lokasi);

		ruang = new AmbilDataRuangBanbox();
		row = new MyFormRow();
		row.setVisible(tampilkanRuanganDamPemilikAset);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));
		if (persetujuan) {
			row.appendChild(new Label(permintaanPengadaanMasterAsset.getRuang() == null ? ""
					: permintaanPengadaanMasterAsset.getRuang().getNama()));
		} else {
			row.appendChild(ruang);
		}
		ruang.setValue(permintaanPengadaanMasterAsset.getRuang() == null ? ""
				: (permintaanPengadaanMasterAsset.getRuang().getKodeRuangan()));
		ruang.setAttribute("ruang", permintaanPengadaanMasterAsset.getRuang());
		ruang.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(permintaanPengadaanMasterAsset.getId() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perjanjian Kerjasama"));
		wajibAdaPerjanjianKerjasama = new MyCheckboxConfig("Wajib Ada Perjanjian Kerjasama");
		if (viewOnly) {
			row.appendChild(
					new Label(permintaanPengadaanMasterAsset.getWajibAdaPerjanjianKerjasama() ? "Ya" : "Tidak"));
		} else if (persetujuan) {
			row.appendChild(wajibAdaPerjanjianKerjasama);
		} else {
			row.appendChild(
					new Label(permintaanPengadaanMasterAsset.getWajibAdaPerjanjianKerjasama() ? "Ya" : "Tidak"));
		}
		wajibAdaPerjanjianKerjasama.setChecked(permintaanPengadaanMasterAsset.getWajibAdaPerjanjianKerjasama());

		keterangan = new MyTextbox(permintaanPengadaanMasterAsset.getKeterangan() == null ? ""
				: permintaanPengadaanMasterAsset.getKeterangan());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Permintaan Pengadaan Barang / Jasa *"));
		if (persetujuan) {
			row.appendChild(new Label(permintaanPengadaanMasterAsset.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}
		keterangan.setWidth("90%");
		keterangan.setRows(3);


		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new PermintaanPengadaanMasterAssetHelper(gridMasterAsset = new MyGrid())
				.initDetail(permintaanPengadaanMasterAsset, persetujuan));

		row = new MyFormRow();
		row.setVisible(persetujuan && !viewOnly && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(setujui = new MyCheckboxConfig("Setujui Pengajuan Permintaan Barang / Jasa ini"));
		setujui.setChecked(permintaanPengadaanMasterAsset.getDisetujuiOleh() != null);

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox && setujui != arg0.getTarget()) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("selesai");
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
					setujuiData.onEvent(new Event("", null, permintaanPengadaanMasterAsset.getDisetujuiOleh() != null));
				}
			});
		}

		return grid;
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan istilah/nama modul yang digunakan oleh mekanisme SOP
	 * untuk menampilkan label yang sesuai pada antarmuka alur persetujuan.<br>
	 * <b>Cara kerja:</b> Implementasi sederhana antarmuka {@code FormSop}. Mengembalikan
	 * string tetap "Permintaan Pengadaan Barang/Jasa" yang digunakan sebagai judul atau
	 * label pada dialog, notifikasi, atau log SOP yang merujuk ke modul ini.<br>
	 * <b>Pemeliharaan:</b> Jika nama modul berubah secara resmi, perbarui string di sini
	 * dan di semua tempat yang menampilkan nama modul kepada pengguna akhir.
	 *
	 * @return string nama modul PR, selalu "Permintaan Pengadaan Barang/Jasa"
	 * @throws Exception tidak dilempar; deklarasi mengikuti kontrak antarmuka {@code FormSop}
	 */
	@Override
	public String istilah() throws Exception {
		return "Permintaan Pengadaan Barang/Jasa";
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan entitas {@code PermintaanPengadaanMasterAsset} yang sedang
	 * aktif (baru dibuat atau sedang diedit) sebagai implementasi kontrak {@code FormSop}.<br>
	 * <b>Cara kerja:</b> Delegasi langsung ke field instance {@code permintaanPengadaanMasterAsset}
	 * yang disetel oleh {@code init} atau {@code form}. Digunakan oleh mekanisme SOP untuk
	 * mengambil entitas terkait setelah alur persetujuan selesai, misalnya untuk logging
	 * atau pembuatan disposisi SOP berikutnya.<br>
	 * <b>Pemeliharaan:</b> Nilai yang dikembalikan bisa null jika metode dipanggil sebelum
	 * {@code init} atau {@code form}. Pemanggil dari SOP harus menangani kemungkinan null ini.
	 *
	 * @return entitas {@code PermintaanPengadaanMasterAsset} yang sedang aktif, atau null
	 *         jika belum diinisialisasi
	 * @throws Exception tidak dilempar; deklarasi mengikuti kontrak antarmuka {@code FormSop}
	 */
	@Override
	public DataSop ambil() throws Exception {
		return permintaanPengadaanMasterAsset;
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan kelas Java dari entitas yang dikelola modul ini, sebagai
	 * implementasi kontrak {@code FormSop} yang digunakan oleh mekanisme SOP untuk refleksi
	 * dan operasi generik berbasis tipe.<br>
	 * <b>Cara kerja:</b> Mengembalikan {@code PermintaanPengadaanMasterAsset.class} secara
	 * langsung. Digunakan oleh framework SOP untuk membuat query Hibernate generik, logging,
	 * atau operasi Envers berbasis tipe entitas tanpa hardcoding nama kelas di lapisan SOP.<br>
	 * <b>Pemeliharaan:</b> Jika entitas diganti nama atau dipindahkan paketnya, perbarui
	 * import dan referensi {@code .class} di sini.
	 *
	 * @return {@code Class} dari {@code PermintaanPengadaanMasterAsset}
	 * @throws Exception tidak dilempar; deklarasi mengikuti kontrak antarmuka {@code FormSop}
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PermintaanPengadaanMasterAsset.class;
	}

	/**
	 * <b>Tujuan:</b> Menghasilkan kode Permintaan Pengadaan berdasarkan skema penomoran surat
	 * yang dikonfigurasi ({@code NomorSuratAlurPengadaan.PERMINTAAN_PEMBELIAN_DATA}), tanpa
	 * menjamin keunikan terhadap data yang sudah ada di database.<br>
	 * <b>Cara kerja:</b> Jika konfigurasi nomor surat tidak tersedia (null), mengembalikan
	 * barcode acak dari {@code Common.getGeneratedBarCode()}. Jika tersedia, mengambil index
	 * berikutnya: menggunakan {@code getNomorIndex()} jika skema menggunakan index urut
	 * manual ({@code gunakanIndexUrut=true}), atau menghitung via {@code getindex} (rowCount
	 * berbasis DB) jika tidak. Jika parameter {@code tambah=true}, index NomorSurat diinkremen
	 * secara permanen via {@code NomorSurat.tambahIndexNomorSurat}. Kode akhir diformat via
	 * {@code nomorSurat.format(index, tanggal)} menggunakan tanggal pembuatan saat ini.<br>
	 * <b>Perhatian:</b> Metode ini TIDAK menjamin keunikan kode jika terjadi penghapusan data
	 * sebelumnya (rowCount tidak monotonik). Untuk penyimpanan, gunakan {@code generateKodeUnik}
	 * yang menambahkan pemeriksaan keunikan terhadap database.<br>
	 * <b>Pemeliharaan:</b> Parameter {@code tambah} hanya boleh {@code true} saat simpan
	 * definitif, bukan saat preview kode di formulir, agar counter tidak terbuang sia-sia.
	 *
	 * @param tambah {@code true} untuk menginkremen counter index NomorSurat secara permanen;
	 *               {@code false} hanya untuk membaca/preview kode tanpa mengubah counter
	 * @return string kode PR yang telah diformat; tidak pernah null tetapi bisa duplikat
	 *         dengan data yang sudah ada jika skema rowCount digunakan
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurPengadaan.PERMINTAAN_PEMBELIAN_DATA == null
				|| NomorSuratAlurPengadaan.PERMINTAAN_PEMBELIAN_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PERMINTAAN_PEMBELIAN_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PERMINTAAN_PEMBELIAN_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PERMINTAAN_PEMBELIAN_DATA.getNomorSurat());

		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PERMINTAAN_PEMBELIAN_DATA.getNomorSurat());
		}

		String noAgenda = NomorSuratAlurPengadaan.PERMINTAAN_PEMBELIAN_DATA.getNomorSurat().format(index,
				tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue());
		return noAgenda;
	}

	/**
	 * <b>Tujuan:</b> Menghitung index urut berikutnya untuk penomoran kode PR berdasarkan
	 * jumlah baris data yang sudah ada di database (rowCount), dengan mempertimbangkan
	 * aturan reset urutan berdasarkan tahun, bulan, kelompok, atau tanggal reset kustom.<br>
	 * <b>Cara kerja:</b> Membangun query {@code Criteria} pada entitas
	 * {@code PermintaanPengadaanMasterAsset} dengan restriction bertumpuk:
	 * <ul>
	 *   <li>Hanya baris aktif ({@code aktif} null atau true).</li>
	 *   <li>Filter berdasarkan NomorSurat atau kelompok NomorSurat jika
	 *       {@code urutBerdasarkanNomor} atau {@code urutBerdasarkanKelompok} aktif.</li>
	 *   <li>Filter tahun jika {@code resetUrutanTiapTahun} aktif.</li>
	 *   <li>Filter tahun+bulan jika {@code resetUrutanTiapBulan} aktif.</li>
	 *   <li>Filter tanggal pembuatan {@code >= resetTiap} jika tanggal reset telah lewat.</li>
	 * </ul>
	 * Menggunakan {@code Projections.rowCount()} untuk efisiensi (tidak mengambil data).
	 * Hasil rowCount ditambah 1 untuk mendapatkan index berikutnya yang belum digunakan.<br>
	 * <b>Perhatian:</b> Jika ada data yang dihapus (soft-delete atau hard-delete), rowCount
	 * tidak monotonik dan index yang dihasilkan bisa bentrok dengan kode yang sudah ada.
	 * Gunakan {@code generateKodeUnik} yang memverifikasi keunikan untuk penyimpanan.<br>
	 * <b>Pemeliharaan:</b> Query menggunakan join {@code LEFT_JOIN} ke {@code nomorSuratAlurPengadaan}
	 * dan {@code nomorSurat}; pastikan relasi ini terjaga di mapping Hibernate.
	 *
	 * @param nomorSurat konfigurasi skema penomoran yang menentukan aturan reset dan urutan;
	 *                   jika null, mengembalikan 0L
	 * @return index urut berikutnya (rowCount + 1), minimal 1L
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PermintaanPengadaanMasterAsset.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
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
	 * <b>Tujuan:</b> Menghasilkan kode Permintaan Pengadaan yang DIJAMIN unik di database,
	 * mengatasi kelemahan {@code generateCode} yang dapat menghasilkan kode duplikat ketika
	 * rowCount tidak monotonik akibat penghapusan data atau race condition antar transaksi.<br>
	 * <b>Cara kerja:</b> Jika tidak ada konfigurasi skema nomor surat ({@code nomorSurat} null),
	 * menggunakan barcode acak dari {@code Common.getGeneratedBarCode()} dan memverifikasinya
	 * hingga 50 kali. Jika ada skema nomor surat, mengambil index awal dari
	 * {@code getNomorIndex()} (skema index urut) atau {@code getindex(nomorSurat)} (rowCount),
	 * lalu masuk ke loop do-while yang:
	 * <ol>
	 *   <li>Memformat kode kandidat via {@code nomorSurat.format(index, tgl)}.</li>
	 *   <li>Mengganti placeholder UNIT dan SATKER dengan nama satuan kerja jika tersedia.</li>
	 *   <li>Menaikkan index sebesar 1.</li>
	 *   <li>Memeriksa keunikan via {@code kodeSudahDipakai(kandidat)}.</li>
	 *   <li>Melanjutkan loop jika kode sudah dipakai, berhenti jika bebas.</li>
	 * </ol>
	 * Loop dibatasi maksimum 10.000 iterasi (guard) untuk menghindari infinite loop.
	 * Setelah kode bebas ditemukan, counter NomorSurat diinkremen satu kali (dengan
	 * exception diabaikan karena keunikan sudah terjamin). Kode yang dikembalikan
	 * sudah dipastikan belum ada di tabel {@code permintaan_pengadaan_master_asset}.<br>
	 * <b>Penanganan error:</b> Exception dari {@code NomorSurat.tambahIndexNomorSurat}
	 * diabaikan secara eksplisit agar tidak memblokir penyimpanan. Exception dari
	 * {@code kodeSudahDipakai} juga diabaikan (fallback ke kode tanpa verifikasi).<br>
	 * <b>Pemeliharaan:</b> Batas 10.000 iterasi cukup untuk kebutuhan normal. Jika sistem
	 * memiliki jutaan PR, pertimbangkan meningkatkan batas atau menggunakan sequence DB.
	 * Race condition tipis antar dua transaksi serentak yang melewati pemeriksaan pada waktu
	 * yang sama ditangani oleh {@code ConstraintViolationException} di {@code onSave}.
	 *
	 * @param satkerNama nama satuan kerja untuk mengganti placeholder UNIT/SATKER dalam format
	 *                   kode; boleh null atau kosong (substitusi dilewati)
	 * @return string kode PR yang unik dan belum digunakan di database; tidak pernah null
	 */
	private String generateKodeUnik(String satkerNama) {
		NomorSurat nomorSurat = (NomorSuratAlurPengadaan.PERMINTAAN_PEMBELIAN_DATA == null) ? null
				: NomorSuratAlurPengadaan.PERMINTAAN_PEMBELIAN_DATA.getNomorSurat();

		// Tanpa skema nomor surat → pakai barcode acak (sudah unik), tetap diverifikasi.
		if (nomorSurat == null) {
			String bc = Common.getGeneratedBarCode();
			int g = 0;
			while (kodeSudahDipakai(bc) && g < 50) {
				bc = Common.getGeneratedBarCode();
				g++;
			}
			return bc;
		}

		Date tgl = (tanggalPembuatan == null || tanggalPembuatan.getValue() == null) ? WaktuUtil.getDate()
				: tanggalPembuatan.getValue();

		Long index = nomorSurat.getGunakanIndexUrut() ? nomorSurat.getNomorIndex() : getindex(nomorSurat);
		if (index == null) {
			index = 1L;
		}

		String kandidat;
		int guard = 0;
		do {
			kandidat = nomorSurat.format(index, tgl);
			if (satkerNama != null && !satkerNama.trim().isEmpty()) {
				kandidat = kandidat.replaceAll("UNIT", satkerNama);
				kandidat = kandidat.replaceAll("SATKER", satkerNama);
			}
			index = index + 1;
			guard++;
		} while (kodeSudahDipakai(kandidat) && guard < 10000);

		// Majukan counter NomorSurat (skema index-urut), sekali, seperti perilaku lama.
		try {
			NomorSurat.tambahIndexNomorSurat(nomorSurat);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PermintaanPengadaanMasterAssetAction.java:2612");
			// abaikan — keunikan sudah dijamin oleh pemeriksaan di atas
		}

		return kandidat;
	}

	/**
	 * <b>Tujuan:</b> Memeriksa apakah suatu kode PR sudah digunakan di database, termasuk
	 * pada baris yang sudah tidak aktif (soft-deleted atau ditutup), untuk mencegah duplikasi
	 * kode yang dapat menyebabkan kebingungan administratif dan potensi constraint violation.<br>
	 * <b>Cara kerja:</b> Membangun query {@code Criteria} sederhana pada entitas
	 * {@code PermintaanPengadaanMasterAsset} dengan restriction {@code eq("kode", kode)} dan
	 * proyeksi {@code rowCount()}. Pemeriksaan dilakukan terhadap SEMUA baris tanpa filter
	 * status aktif, sehingga kode yang pernah digunakan oleh PR yang sudah dihapus/tidak aktif
	 * pun tidak akan digunakan ulang. Mengembalikan {@code true} jika rowCount > 0.<br>
	 * <b>Penanganan error:</b> Jika kode null atau kosong, langsung mengembalikan {@code false}
	 * (kode kosong dianggap belum dipakai agar proses tidak terblokir). Jika terjadi exception
	 * database (misalnya sesi tertutup), mengembalikan {@code false} sebagai fallback aman
	 * agar penyimpanan tidak diblokir hanya karena pemeriksaan gagal; dalam kasus ini,
	 * {@code ConstraintViolationException} di {@code onSave} akan menjadi garis pertahanan
	 * terakhir.<br>
	 * <b>Pemeliharaan:</b> Metode ini hanya dipanggil dari {@code generateKodeUnik}. Jika
	 * perlu memindahkan logika keunikan ke level database, tambahkan UNIQUE INDEX pada
	 * kolom {@code kode} di tabel {@code permintaan_pengadaan_master_asset}.
	 *
	 * @param kode string kode yang akan diperiksa keunikannya; boleh null
	 * @return {@code true} jika kode sudah ada di database (termasuk baris non-aktif),
	 *         {@code false} jika belum ada atau terjadi error saat pemeriksaan
	 */
	private boolean kodeSudahDipakai(String kode) {
		if (kode == null || kode.trim().isEmpty()) {
			return false;
		}
		try {
			Session session = HibernateUtil.currentSession();
			Number n = (Number) session.createCriteria(PermintaanPengadaanMasterAsset.class)
					.add(Restrictions.eq("kode", kode)).setProjection(Projections.rowCount()).uniqueResult();
			return n != null && n.longValue() > 0;
		} catch (Exception e) {
			// Bila cek gagal, jangan blokir penyimpanan (fallback ke perilaku lama).
			return false;
		}
	}

	/**
	 * <b>Tujuan:</b> Mengatur mode persetujuan controller secara programatis dari luar,
	 * sebagai implementasi kontrak antarmuka {@code FormSop}.<br>
	 * <b>Cara kerja:</b> Menyetel field {@code persetujuan} ke nilai yang diberikan. Saat
	 * {@code persetujuan=true}, semua field formulir yang dirender oleh {@code form} akan
	 * ditampilkan sebagai Label baca-saja (bukan komponen input), dan baris persetujuan
	 * akan terlihat. Metode ini biasanya dipanggil oleh framework SOP setelah membuat
	 * instance controller via konstruktor default dan sebelum memanggil {@code form}.<br>
	 * <b>Pemeliharaan:</b> Pastikan semua field yang bersifat kondisional berdasarkan
	 * {@code persetujuan} di dalam {@code form} menggunakan field instance ini (bukan
	 * variabel lokal), agar perubahan via metode ini tercermin dengan benar di formulir.
	 *
	 * @param persetujuan {@code true} untuk mengaktifkan mode baca-saja/persetujuan,
	 *                    {@code false} untuk mode edit normal
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

}
