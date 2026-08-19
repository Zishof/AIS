package ais.action.master.akunting;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
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
import ais.action.master.akunting.helper.TransaksiJurnalUmumHelper;
import ais.action.master.helper.RevisiGrupTransaksiHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.report.format1.akunting.LaporanBuktiPengeluaranKas;
import ais.action.report.format1.akunting.LaporanBuktiPengeluaranKasBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.Closing;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.JenisTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.akunting.Transaksi;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Workspace;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h3>GrupTransaksiAction — Pengelola Entri Jurnal Akuntansi</h3>
 *
 * <p><b>Untuk apa:</b> Kelas ini merupakan controller utama untuk modul Draft Jurnal dan
 * Semua Jurnal Tersimpan dalam sistem akuntansi eCampus. Kelas menangani seluruh siklus
 * hidup {@link ais.database.model.akunting.GrupTransaksi} — yaitu kelompok transaksi jurnal
 * umum yang terdiri dari satu atau lebih baris {@link ais.database.model.akunting.Transaksi}.
 * Fitur utama mencakup: penambahan jurnal baru via {@code TransaksiJurnalUmumHelper}, tampilan
 * daftar dengan filter multi-dimensi, posting jurnal ke buku besar, pembatalan posting, ekspor
 * Excel/PDF, impor data dari file, sinkronisasi total debet/kredit, serta pembersihan data
 * transaksi ganda.</p>
 *
 * <p><b>Cara kerja:</b> Kelas ini meng-extends {@code GenericAutowireComposer} sehingga
 * komponen ZK (Combobox, Textbox, MyGrid, Paging, dsb.) di-wire otomatis dari file ZUL
 * yang berpasangan. Inisialisasi dilakukan di {@link #doAfterCompose(Component)} yang
 * membaca parameter URL, mengatur izin CRUD berdasarkan {@code CommonPrivilages}, lalu
 * memanggil {@code onSearchDefault} untuk mengisi grid pertama kali. Filter data mencakup:
 * rentang tanggal, akun, satuan kerja, keterangan, jenis transaksi, status posting, jenis
 * jurnal, rentang nominal, dan nomor bukti. Query dibangun secara dinamis di
 * {@link #initCriteria(boolean)} menggunakan Hibernate Criteria API. Renderer baris grid
 * (kelas dalam {@code GrupTransaksiRenderer}) merender setiap {@code GrupTransaksi} beserta
 * kolom posting interaktif.</p>
 *
 * <p><b>Konstruktor ganda:</b> Kelas dapat diinstansiasi dengan
 * {@code new GrupTransaksiAction()} (default: tampil jurnal umum draft) atau
 * {@code new GrupTransaksiAction(semua, hanyaJurnalUmum)} untuk mode "Semua Jurnal Tersimpan"
 * ({@code semua=true}) yang dipakai oleh subkelas {@code SemuaGrupTransaksiAction}.</p>
 *
 * <p><b>Fitur filter tambahan (baru):</b> Filter Status Posting (Semua/Sudah Diposting/
 * Belum Diposting), filter Jenis Jurnal (Umum/Kas Masuk/Kas Keluar/Transaksi), filter
 * Rentang Nominal (min/maks totalDebet), dan filter No. Bukti (id PostingHistory). Filter-
 * filter ini dilayani oleh field {@code searchStatusPosting}, {@code searchJenisJurnal},
 * {@code searchNominalMin}, {@code searchNominalMax}, dan {@code searchNoBukti}.</p>
 *
 * <p><b>Threading:</b> Operasi "Singkronkan Transaksi" dan "Posting Data Jurnal" (massal)
 * berjalan di {@code Thread} latar menggunakan {@code HibernateUtil.currentNativeSession()}
 * supaya tidak memblokir UI thread ZK. Label progress diperbarui langsung dari thread latar.
 * Tidak ada sinkronisasi eksplisit — setiap iterasi membuka dan menutup session sendiri.</p>
 *
 * <p><b>Pemeliharaan:</b> Saat menambah filter baru, tambahkan field UI di ZUL, tambahkan
 * {@code @Wire} jika perlu, lalu tambahkan klausa {@code criteria.add(...)} di
 * {@link #initCriteria(boolean)}. Perhatikan urutan penambahan klausa agar tidak bertabrakan
 * dengan {@code alias} yang sudah dibuat (mis. alias {@code postingHistory} tidak boleh
 * dibuat dua kali). Gunakan whitelist parameter URL untuk filter yang dikirim dari dasbor
 * (lihat field {@code entitasDasbor}, {@code refDasbor}, {@code jenisDasbor}).</p>
 *
 * @see ais.database.model.akunting.GrupTransaksi
 * @see ais.database.model.akunting.Transaksi
 * @see ais.database.model.akunting.PostingHistory
 * @see ais.action.master.akunting.helper.TransaksiJurnalUmumHelper
 */
public class GrupTransaksiAction extends GenericAutowireComposer implements DataSearchDefault {

	/**
	 *
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyGrid grid;
	private Paging paging;

	private North filter;

	private Textbox searchnama;
	private MyDatebox searchmulai;
	private MyDatebox searchsampai;
	private AmbilDataAkunBanbox searchakun;
	private AmbilDataSatuanKerjaBanbox searchsatuanKerja;
	private Textbox searchketerangan;
	private Combobox searchjenis;

	// ── Filter tambahan ──
	/** Status posting: Semua / Sudah Diposting / Belum Diposting. */
	private Combobox searchStatusPosting;
	/** Cari berdasarkan No. Bukti posting (= id PostingHistory yang tampil sbg Nomor Transaksi). */
	private Textbox searchNoBukti;
	/** Jenis jurnal: Umum / Kas Masuk / Kas Keluar / Transaksi. */
	private Combobox searchJenisJurnal;
	/** Rentang nilai total (debet). */
	private org.zkoss.zul.Doublebox searchNominalMin;
	private org.zkoss.zul.Doublebox searchNominalMax;

	private MyToolbarbuttonConfig sent;
	private MyToolbarbuttonConfig batalkan;
	private MyCheckboxConfig searchtampil;
	private MyCheckboxConfig searchtampilsemua;

	private boolean edit = false;
	private boolean delete = false;
	private MyToolbarbuttonConfig find;
	private MyToolbarbuttonConfig add;
	private Tbmuser tbmuser;
	public boolean adminLain;

	private Workspace workspace = null;
	private MyCheckboxConfig tidakbalance;
	private boolean semua = false;
	private List<Akun> akuns = null;

	private MyColumnConfig colEdit;
	private MyColumnConfig colPosting;
	private boolean hanyaJurnalUmum = true;
	private Closing closing = null;
	private MyCheckboxConfig belumTerposting = null;

	private Long idGrup = null;

	private String tgl_closing = null;

	private Boolean sudah_posting = null;
	private Boolean sudah_closing = null;

	private Boolean pertangungjawaban = null;
	private Boolean kasKecil = null;
	private Boolean kasBesar = null;
	private Boolean pajak = null;
	private Boolean saldoAwalMasterAsset = null;
	private Boolean termin = null;
	private Boolean dp_balik = null;

	private Boolean dp_vendor = null;
	private Boolean pemesananPengadaanMasterAsset = null;

	/*
	 * Filter generic kiriman dasbor draft jurnal (PostingJurnalHelper):
	 * entitas = properti GrupTransaksi (whitelist), ref/jenis = nilai kolom
	 * ref/jenis (whitelist). Dipakai supaya angka closing yang di-click di
	 * dasbor selalu sama dengan data yang tampil di sini.
	 */
	private String entitasDasbor = null;
	private String refDasbor = null;
	private String jenisDasbor = null;
	private Row rowPosting;

	public static String[] contents = new String[] { "id", "grupTransaksi", "tanggalTransaksi", "tanggalDimasukkan",
			"akun", "debet", "kredit", "keterangan", "jenisTransaksi", "grupTransaksi.workspace",
			"grupTransaksi.satuanKerja", "grupTransaksi.keperluan", "grupTransaksi.kepada" };

	/**
	 * Konstruktor default untuk mode Draft Jurnal Umum.
	 *
	 * <p><b>Tujuan:</b> Membuat instance {@code GrupTransaksiAction} dengan konfigurasi
	 * standar: mode jurnal umum saja ({@code semua=false, hanyaJurnalUmum=true}). Mode ini
	 * digunakan pada halaman "Draft Jurnal" yang hanya menampilkan jurnal bertipe
	 * {@link ais.database.model.akunting.Transaksi#JURNAL_UMUM} yang belum terposting,
	 * serta semua jurnal yang sudah memiliki {@code PostingHistory} (agar jurnal terposting
	 * dari modul lain tetap terlihat).</p>
	 *
	 * <p><b>Cara kerja:</b> Mendelegasikan ke konstruktor utama
	 * {@link #GrupTransaksiAction(boolean, boolean)} dengan argumen {@code false, true}.
	 * Tidak ada inisialisasi field lain di sini — semua dilakukan di
	 * {@link #doAfterCompose(Component)}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Konstruktor ini dipakai saat ZUL membuat instance
	 * via atribut {@code use="..."}. Jangan hapus konstruktor no-arg karena ZK membutuhkannya
	 * untuk instansiasi reflektif.</p>
	 */
	public GrupTransaksiAction() {
		this(false, true);
	}

	/**
	 * Konstruktor utama yang menentukan mode tampilan jurnal.
	 *
	 * <p><b>Tujuan:</b> Memungkinkan kelas atau subkelas mengkonfigurasi dua perilaku
	 * utama: apakah menampilkan semua jurnal terposting dari semua modul ({@code semua=true},
	 * dipakai oleh {@code SemuaGrupTransaksiAction}), dan apakah membatasi tampilan hanya
	 * pada jenis jurnal umum ({@code hanyaJurnalUmum=true}, perilaku default).</p>
	 *
	 * <p><b>Cara kerja:</b> Menyimpan kedua parameter ke field instance. Nilai-nilai ini
	 * kemudian digunakan di {@link #initCriteria(boolean)} untuk membangun klausa filter
	 * Hibernate. Saat {@code semua=true}, kolom posting ditampilkan (lebar 5%) dan filter
	 * "Semua Jurnal Tersimpan" diaktifkan. Saat {@code hanyaJurnalUmum=true}, query
	 * menambahkan filter {@code jenisJurnal = JURNAL_UMUM OR postingHistory IS NOT NULL}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Nilai {@code hanyaJurnalUmum} bisa diubah secara runtime
	 * oleh {@link #initCriteria(boolean)} ketika parameter URL seperti {@code pertangungjawaban},
	 * {@code kasKecil}, {@code kasBesar}, dsb. diterima — artinya field ini tidak selalu
	 * mencerminkan nilai awal setelah {@code doAfterCompose} selesai.</p>
	 *
	 * @param semua         {@code true} untuk menampilkan semua jurnal terposting dari semua
	 *                      modul; {@code false} untuk mode draft jurnal umum
	 * @param hanyaJurnalUmum {@code true} untuk membatasi pada jenis jurnal umum saja (default);
	 *                      {@code false} untuk tidak ada batasan jenis jurnal
	 */
	public GrupTransaksiAction(boolean semua, boolean hanyaJurnalUmum) {
		this.semua = semua;
		this.hanyaJurnalUmum = hanyaJurnalUmum;
	}

	/**
	 * Pemeriksaan keamanan sebelum halaman ZK dikomposi.
	 *
	 * <p><b>Tujuan:</b> Memvalidasi bahwa pengguna memiliki sesi aktif dan hak akses
	 * yang valid sebelum komponen ZK dibuat. Metode ini dipanggil oleh framework ZK
	 * secara otomatis sebelum {@link #doAfterCompose(Component)}.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} yang memeriksa
	 * atribut sesi {@code usersTemp} dan mengalihkan ke halaman login jika sesi tidak valid.
	 * Kemudian mendelegasikan ke implementasi superclass untuk melanjutkan proses komposisi
	 * normal ZK. Jika keamanan gagal, eksekusi berhenti di {@code doCheckSecurity}
	 * dan halaman tidak dimuat.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak ada penanganan error eksplisit — jika sesi tidak
	 * valid, {@code Common.doCheckSecurity()} langsung melakukan redirect ke halaman
	 * logoff dan melempar exception yang ditangkap oleh framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Override ini wajib ada di semua Action yang membutuhkan
	 * autentikasi. Jangan hapus tanpa mengganti mekanisme keamanan lain.</p>
	 *
	 * @param page     halaman ZK saat ini
	 * @param parent   komponen induk
	 * @param compInfo informasi metadata komponen
	 * @return informasi komponen dari superclass
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Inisialisasi penuh controller setelah komponen ZK selesai dikomposi.
	 *
	 * <p><b>Tujuan:</b> Metode lifecycle utama ZK yang dipanggil setelah semua komponen
	 * di file ZUL berhasil dibuat dan di-wire ke field Java. Di sini dilakukan seluruh
	 * inisialisasi: validasi sesi, pembacaan parameter URL, pengaturan izin CRUD,
	 * inisialisasi komponen (date range, combo jenis transaksi, paging), pemasangan
	 * toolbar cetak/upload/admin, dan pemanggilan pencarian awal.</p>
	 *
	 * <p><b>Cara kerja langkah per langkah:</b>
	 * <ol>
	 *   <li>Validasi sesi: jika bukan parameter {@code akun} dan sesi tidak valid, logoff.</li>
	 *   <li>Membaca semua parameter URL: {@code pertangungjawaban}, {@code kasKecil},
	 *       {@code kasBesar}, {@code pajak}, {@code saldoAwalMasterAsset}, {@code termin},
	 *       {@code dp_balik}, {@code dp_vendor}, {@code pemesananPengadaanMasterAsset},
	 *       {@code workspace}, {@code closing}, {@code tgl_closing}, {@code sudah_posting},
	 *       {@code sudah_closing}, {@code grup}, {@code akun} (tunggal atau multi dengan koma),
	 *       {@code mulai}, {@code sampai}, dan parameter filter dasbor {@code entitas},
	 *       {@code ref}, {@code jenis}.</li>
	 *   <li>Menyesuaikan visibilitas komponen: menyembunyikan filter, tombol Tambah, kolom
	 *       Edit sesuai konteks pemanggilan (dari dasbor, dari modul lain, atau mandiri).</li>
	 *   <li>Inisialisasi date range default: 6 bulan ke belakang hingga besok.</li>
	 *   <li>Mengatur izin: {@code edit}, {@code delete}, {@code adminLain} (admin/APPROVE).</li>
	 *   <li>Memastikan data master {@code JenisTransaksi} tersedia — jika belum ada, dibuat
	 *       data awal 8 jenis transaksi standar.</li>
	 *   <li>Memasang toolbar: Cetak (rekap Transaksi), Upload data Excel, Cetak Gabungan
	 *       Bukti Transaksi (admin), Bersihkan Transaksi Double (admin), Hapus Data Transaksi
	 *       (konfig), Tidak Balance checkbox, Singkronkan Transaksi (admin), Posting Data
	 *       Jurnal massal (admin), History (revisi).</li>
	 *   <li>Memanggil {@code onSearchDefault} via timer untuk load data awal grid.</li>
	 *   <li>Memanggil {@code FilterLanjutHelper.setup} untuk mengaktifkan filter lanjutan.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Parsing parameter URL dibungkus try-catch — jika parameter
	 * tidak valid (mis. id bukan angka), error diabaikan secara diam-diam. Error pada
	 * operasi posting massal ditampilkan via {@code Common.tampilErrorJikaAdmin}.</p>
	 *
	 * <p><b>Threading:</b> Operasi sinkronisasi dan posting massal dijalankan di thread
	 * terpisah (non-UI thread) menggunakan {@code HibernateUtil.currentNativeSession()}.
	 * Label progress diperbarui langsung — perlu diperhatikan bahwa pembaruan label dari
	 * thread non-ZK bisa tidak terlihat tanpa mekanisme push; ini adalah trade-off yang
	 * disengaja untuk kesederhanaan.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika menambah parameter URL baru, tambahkan blok pembacaan
	 * parameter di awal metode ini dan tambahkan klausa terkait di {@link #initCriteria(boolean)}.
	 * Jangan lupa menambahkan field instance baru jika diperlukan.</p>
	 *
	 * @param comp komponen root dari halaman ZK (biasanya {@code Window} utama)
	 * @throws Exception jika inisialisasi Hibernate, ZK, atau komponen gagal
	 */
	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		if (execution.getParameter("akun") == null && (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ))) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (execution.getParameter("pertangungjawaban") != null
				&& !execution.getParameter("pertangungjawaban").trim().isEmpty()) {
			pertangungjawaban = Boolean.parseBoolean(execution.getParameter("pertangungjawaban").trim());
		}

		if (execution.getParameter("kasKecil") != null && !execution.getParameter("kasKecil").trim().isEmpty()) {
			kasKecil = Boolean.parseBoolean(execution.getParameter("kasKecil").trim());
		}

		if (execution.getParameter("kasBesar") != null && !execution.getParameter("kasBesar").trim().isEmpty()) {
			kasBesar = Boolean.parseBoolean(execution.getParameter("kasBesar").trim());
		}

		if (execution.getParameter("pajak") != null && !execution.getParameter("pajak").trim().isEmpty()) {
			pajak = Boolean.parseBoolean(execution.getParameter("pajak").trim());
		}

		if (execution.getParameter("saldoAwalMasterAsset") != null
				&& !execution.getParameter("saldoAwalMasterAsset").trim().isEmpty()) {
			saldoAwalMasterAsset = Boolean.parseBoolean(execution.getParameter("saldoAwalMasterAsset").trim());
		}

		if (execution.getParameter("termin") != null && !execution.getParameter("termin").trim().isEmpty()) {
			termin = Boolean.parseBoolean(execution.getParameter("termin").trim());
		}
		if (execution.getParameter("dp_balik") != null && !execution.getParameter("dp_balik").trim().isEmpty()) {
			dp_balik = Boolean.parseBoolean(execution.getParameter("dp_balik").trim());
		}

		if (execution.getParameter("dp_vendor") != null && !execution.getParameter("dp_vendor").trim().isEmpty()) {
			dp_vendor = Boolean.parseBoolean(execution.getParameter("dp_vendor").trim());
		}

		if (execution.getParameter("pemesananPengadaanMasterAsset") != null
				&& !execution.getParameter("pemesananPengadaanMasterAsset").trim().isEmpty()) {
			pemesananPengadaanMasterAsset = Boolean
					.parseBoolean(execution.getParameter("pemesananPengadaanMasterAsset").trim());
		}

		entitasDasbor = ais.action.master.helper.PostingJurnalHelper.ambilParameterEntitasClosing();
		if (execution.getParameter("ref") != null && !execution.getParameter("ref").trim().isEmpty()) {
			refDasbor = execution.getParameter("ref").trim();
		}
		if (execution.getParameter("jenis") != null && !execution.getParameter("jenis").trim().isEmpty()) {
			jenisDasbor = execution.getParameter("jenis").trim();
		}
		if (execution.getParameter("semua_jurnal") != null
				&& Boolean.parseBoolean(execution.getParameter("semua_jurnal").trim())) {
			hanyaJurnalUmum = false;
		}

		if (execution.getParameter("sudah_posting") != null
				&& !execution.getParameter("sudah_posting").trim().isEmpty()) {
			sudah_posting = Boolean.parseBoolean(execution.getParameter("sudah_posting").trim());
		}
		if (execution.getParameter("sudah_closing") != null
				&& !execution.getParameter("sudah_closing").trim().isEmpty()) {
			sudah_closing = Boolean.parseBoolean(execution.getParameter("sudah_closing").trim());
		}

		if (execution.getParameter("workspace") != null) {
			workspace = (Workspace) ConstantValues
					.simpleObject(
							HibernateUtil.currentSession().createCriteria(Workspace.class)
									.add(Restrictions.or(Restrictions.eq("carryOver", true),
											Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true))))
									.add(Restrictions.idEq(Long.parseLong(execution.getParameter("workspace")))),
							Workspace.class);
		}

		if (execution.getParameter("tgl_closing") != null && !execution.getParameter("tgl_closing").trim().isEmpty()) {
			tgl_closing = execution.getParameter("tgl_closing").trim();
		}

		if (execution.getParameter("closing") != null) {
			closing = (Closing) ConstantValues.simpleObject(HibernateUtil.currentSession().createCriteria(Closing.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("closing")))), Closing.class);
		}

		if (closing != null && filter != null) {
			filter.setVisible(false);
		}

		if (!ConstantValues.otomatisTerposting && !hanyaJurnalUmum) {
			colPosting.setWidth("5%");
		}

		if (searchmulai != null) { searchmulai.setReadonly(true); }
		if (searchsampai != null) { searchsampai.setReadonly(true); }

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (searchmulai != null) { searchmulai.setValue(calendar.getTime()); }
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (searchsampai != null) { searchsampai.setValue(calendar.getTime()); }

		if (add != null) {
			add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
			add.setTooltiptext("Tambah");
		}
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		searchsatuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		if (sudah_posting != null && rowPosting != null) {
			if (rowPosting != null) rowPosting.setVisible(true);
		}

		if ((execution.getParameter("grup") != null && !execution.getParameter("grup").trim().isEmpty())
				|| (tgl_closing != null && !tgl_closing.isEmpty()) || sudah_posting != null || sudah_closing != null
				|| pertangungjawaban != null || kasKecil != null || kasBesar != null || pajak != null
				|| saldoAwalMasterAsset != null || pemesananPengadaanMasterAsset != null) {

			try {

				if (searchtampilsemua != null)
					searchtampilsemua.setDisabled(true);

				if (searchtampil != null)
					searchtampil.setVisible(false);

				if (colEdit != null) {
					colEdit.setWidth("0px");
				}

				if (add != null) {
					add.setVisible(false);
				}

				if (filter != null) {
					filter.setVisible(false);
				}

				idGrup = execution.getParameter("grup") == null || execution.getParameter("grup").trim().isEmpty()
						? null
						: Long.parseLong(execution.getParameter("grup").trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/GrupTransaksiAction.java:504");
				// TODO: handle exception
			}

		}

		if (execution.getParameter("akun") != null && execution.getParameter("akun").contains(",")) {
			searchakun.setDisabled(true);
			searchakun.setAttribute("akun", null);
			searchakun.setValue("");

			List<Long> aks = new ArrayList<Long>();
			for (String s : execution.getParameter("akun").split(",")) {
				aks.add(Long.parseLong(s.trim()));
			}

			akuns = aks.isEmpty() ? new ArrayList<Akun>()
					: ConstantValues.simpleList(
							HibernateUtil.currentSession().createCriteria(Akun.class).add(Restrictions.in("id", aks)),
							Akun.class);

			if (searchtampilsemua != null)
				searchtampilsemua.setDisabled(true);

			if (searchtampil != null)
				searchtampil.setVisible(false);

			if (colEdit != null) {
				colEdit.setWidth("0px");
			}

			if (add != null) {
				add.setVisible(false);
			}

			if (filter != null) {
				filter.setVisible(false);
			}
		}

		else if (execution.getParameter("akun") != null) {
			Akun akun = (Akun) ConstantValues.simpleObject(HibernateUtil.currentSession().createCriteria(Akun.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("akun")))), Akun.class);

			if (akun != null) {
				akuns = new ArrayList<Akun>();
				akuns.add(akun);
				searchakun.setAttribute("akun", akun);
				searchakun.setValue(akun.getKode() + "-" + akun.getNama());
				searchakun.setDisabled(true);
				if (searchtampilsemua != null)
					searchtampilsemua.setDisabled(true);

				if (searchtampil != null)
					searchtampil.setVisible(false);

				if (colEdit != null) {
					colEdit.setWidth("0px");
				}

				if (add != null) {
					add.setVisible(false);
				}

				if (filter != null) {
					filter.setVisible(false);
				}
			}
		}

		if (execution.getParameter("mulai") != null) {
			try {
				searchmulai.setValue(Common.dateFormat8.get().parse(execution.getParameter("mulai")));
				searchmulai.setDisabled(true);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		if (execution.getParameter("sampai") != null) {
			try {
				searchsampai.setValue(Common.dateFormat8.get().parse(execution.getParameter("sampai")));
				searchsampai.setDisabled(true);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		adminLain = Common.getApakahAdmin() || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		if (adminLain) {
			semua = false;
		}

		if (sent != null)
			if (sent != null) { sent.setVisible(adminLain); }

		if (batalkan != null)
			if (batalkan != null) { batalkan.setVisible(adminLain); }

		Session session = HibernateUtil.currentSession();

		int count = ((Number) session.createCriteria(JenisTransaksi.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {

			String[] jenisTransaksies = new String[] { "PB,Penerimaan Bank", "JBI,Jurnal Penerimaan Bank",
					"JCO,Jurnal Pengeluaran Kas", "JMM,Jurnal Memorial", "JBO,Jurnal Pengeluaran Bank",
					"JCI,Jurnal Penerimaan Kas", "AJE,Jurnal Penyesuaian", "GJP,General Jurnal Voucer" };
			for (String k : jenisTransaksies) {
				if (k != null) {
					String[] s = k.trim().split(",");
					JenisTransaksi jenisTransaksi = new JenisTransaksi();
					jenisTransaksi.setKode(s[0].trim());
					jenisTransaksi.setNama(s[1].trim());
					jenisTransaksi.setKeterangan(s[0].trim() + " " + k.toString().trim());
					session.save(jenisTransaksi);
				}
			}

		}

		Common.insertComboDanSemua(searchjenis, new String[] { "nama", "akun" }, "kode", JenisTransaksi.class, "Semua",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (searchjenis != null) { searchjenis.setReadonly(true); }

		tbmuser = Common.getCurrentUser();

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		searchakun.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Transaksi.class, new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();
				Akun akun = (Akun) searchakun.getAttribute("akun");

				Criteria criteria = session.createCriteria(Transaksi.class)

						.add(akuns != null && !akuns.isEmpty() ? Restrictions.in("akun", akuns)
								: akun == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("akun", akun))

						.add(akun == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("akun", akun))
						.createCriteria("grupTransaksi")
						.add((searchsatuanKerja == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsatuanKerja.getAttribute("satuanKerja") == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("satuanKerja", searchsatuanKerja.getAttribute("satuanKerja"))))

						.add((akuns != null && !akuns.isEmpty()) ? Restrictions.isNotNull("postingHistory")
								: semua ? (searchtampilsemua != null && searchtampilsemua.isChecked()
										? Restrictions.sqlRestriction("true")
										: Restrictions.isNotNull("postingHistory"))

										: (hanyaJurnalUmum ? Restrictions.eq("jenisJurnal", Transaksi.JURNAL_UMUM)
												: Restrictions.sqlRestriction("true")))

						.add((searchmulai == null || searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_transaksi) between date('"
								+ Common.databaseDateFormat.get().format(searchmulai.getValue()) + "') and  date('"
								+ Common.databaseDateFormat.get().format(searchsampai.getValue()) + "')")))

						.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("kode", searchnama.getValue(), MatchMode.ANYWHERE))
						.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("deskripsi", searchketerangan.getValue(), MatchMode.ANYWHERE));

				if (order)
					criteria.addOrder(Order.desc("id"));
				return criteria;
			}
		}, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		if (!semua && (akuns == null || akuns.isEmpty())) {

			MyToolbarbuttonConfig upload = Common.uploadData(this, Transaksi.class, new EventListener() {

				@SuppressWarnings({ "rawtypes" })
				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] data = (Object[]) arg0.getData();
					Transaksi transaksi = (Transaksi) data[0];
					Session session = (Session) data[1];
					Map datum = (Map) data[2];
					List apakahSimpan = (List) data[3];
					List<String> warnings = (List<String>) data[6];

					if (transaksi.getAkun() == null) {

						warnings.add("Akun " + datum.get("akun") + " tidak ditemukan di database. Data sbb : " + datum);

						apakahSimpan.add(false);
						return;
					}
					if (transaksi.getTanggalTransaksi() == null) {

						warnings.add("Tanggal transaksi tidak ditemukan. Data sbb : " + datum);

						apakahSimpan.add(false);
						return;
					}
					GrupTransaksi grupTransaksi = transaksi.getGrupTransaksi();
					if (grupTransaksi == null && datum.get("grupTransaksi") != null
							&& !datum.get("grupTransaksi").toString().trim().isEmpty()
							&& (datum.get("grupTransaksi") instanceof String)) {
						grupTransaksi = (GrupTransaksi) session.createCriteria(GrupTransaksi.class)
								.add(Restrictions.eq("kode", datum.get("grupTransaksi").toString().trim()))
								.setMaxResults(1).uniqueResult();
						if (grupTransaksi == null) {
							grupTransaksi = new GrupTransaksi();
							grupTransaksi.setSatuanKerja(tbmuser.ambilSatuanKerja());
							grupTransaksi.setTotalKredit(0.0);
							grupTransaksi.setTotalDebet(0.0);
							grupTransaksi.setTbmuser(tbmuser);
							grupTransaksi.setTanggalTransaksi(transaksi.getTanggalTransaksi());
							grupTransaksi.setJenisJurnal(Transaksi.JURNAL_UMUM);
							grupTransaksi.setPegawai(tbmuser.ambilPegawai());
							grupTransaksi.setParentCode(datum.get("grupTransaksi").toString().trim());
							grupTransaksi.setKode(datum.get("grupTransaksi").toString().trim());
							grupTransaksi.setKeterangan(transaksi.getKeterangan());
							grupTransaksi.setJenisTransaksi(transaksi.getJenisTransaksi());

							try {
								session.getTransaction().begin();
								Common.refreshSaveOrUpdate(session, grupTransaksi);
								session.getTransaction().commit();
							} catch (Exception e) {
								session.getTransaction().rollback();
								warnings.add("Kesalahan simpan data " + grupTransaksi + ", error sbb " + e.getMessage()
										+ ". Data sbb : " + datum);
							}

						} else {
							grupTransaksi.setJenisTransaksi(transaksi.getJenisTransaksi());
							grupTransaksi.setTanggalTransaksi(transaksi.getTanggalTransaksi());

							try {
								session.getTransaction().begin();
								Common.refreshSaveOrUpdate(session, grupTransaksi);
								session.getTransaction().commit();
							} catch (Exception e) {
								session.getTransaction().rollback();
								warnings.add("Kesalahan simpan data " + grupTransaksi + ", error sbb " + e.getMessage()
										+ ". Data sbb : " + datum);
							}
						}
						transaksi.setGrupTransaksi(grupTransaksi);
					}

					transaksi.setJenisJurnal(Transaksi.JURNAL_UMUM);
					transaksi.setSimpan(true);
					transaksi.setParentCode(datum.get("grupTransaksi").toString().trim());
					transaksi.setKode(datum.get("grupTransaksi").toString().trim());
					transaksi.setMerupakanDebet(transaksi.getDebet() > 0.1);
				}
			}, contents);
			upload.setVisible((add != null && add.isVisible()) && edit && delete);
			Common.appendKeToolbar(upload, find, comp);

			MyToolbarbuttonConfig barcode = new MyToolbarbuttonConfig("Cetak Gabungan Bukti Transaksi",
					"/img/album.png");
			barcode.setParent(find.getParent());
			barcode.setVisible(Common.getApakahAdmin());
			barcode.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					LaporanBuktiPengeluaranKasBanyak buktiPengeluaranKasBanyak = new LaporanBuktiPengeluaranKasBanyak();
					buktiPengeluaranKasBanyak.setTitle("Bukti Transaksi");
					buktiPengeluaranKasBanyak.setClosable(true);
					buktiPengeluaranKasBanyak.setHeight("95%");
					buktiPengeluaranKasBanyak.setWidth("90%");
					buktiPengeluaranKasBanyak.setParent(page.getFirstRoot());
					buktiPengeluaranKasBanyak.onModal();
				}
			});

			MyToolbarbuttonConfig doubleTrx = new MyToolbarbuttonConfig("Bersihkan Transaksi Double",
					"/img/svg/warning-outline.svg");
			doubleTrx.setParent(find.getParent());
			doubleTrx.setVisible(Common.getApakahAdmin());
			doubleTrx.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show(
							"Apakah yakin ingin menghapus transaksi double (hapus salah satu jika double) ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();
											session.createSQLQuery(
													"delete from akunting.grup_transaksi where id in (select min(id) from akunting.grup_transaksi group by kode having count(*)>1);")
													.executeUpdate();
											session.createSQLQuery(
													"delete from akunting.transaksi where id in (select min(id) from akunting.transaksi group by grup_transaksi,akun,debet,kredit having count(*)>1);")
													.executeUpdate();
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain. Keterangan kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan tidak ada transaksi atau referensi yang masih menggunakan data ini; (3) apabila kesalahan masih berlanjut, mohon menghubungi administrator sistem.",
													e.getMessage()));
										}

									}

								}
							});

				}
			});

			doubleTrx = new MyToolbarbuttonConfig("Hapus Data Transaksi", "/img/svg/trash.svg");
			doubleTrx.setParent(find.getParent());
			doubleTrx.setVisible(Common.bolehKonfigurasi("tampilkan_bersihkan_jurnal", Konfigurasi.TIDAK_AKTIF));
			doubleTrx.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data transaksi ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();
											session.createSQLQuery("delete from akunting.grup_transaksi")
													.executeUpdate();

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain. Keterangan kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan tidak ada transaksi atau referensi yang masih menggunakan data ini; (3) apabila kesalahan masih berlanjut, mohon menghubungi administrator sistem.",
													e.getMessage()));
										}

									}

								}
							});

				}
			});

			tidakbalance = new MyCheckboxConfig("Tidak balance");
			tidakbalance.setParent(find.getParent());
			tidakbalance.addEventListener("onClick", new EventListener() {

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
		}

		MyToolbarbuttonConfig doubleTrx = new MyToolbarbuttonConfig("Singkronkan Transaksi", "/img/Check-icon.png");
		if (doubleTrx != null) { doubleTrx.setParent(find.getParent()); }
		if (doubleTrx != null) { doubleTrx.setVisible(Common.getApakahAdmin() && edit && delete && (akuns == null || akuns.isEmpty())); }
		doubleTrx.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final List<Long> grupTransaksis = initCriteria(false).setProjection(Projections.property("id")).list();
				final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Transaksi Grup Transaksi");
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						laporan.selesaikan(new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								onSearchDefault(null);
							}
						});
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
						int size = grupTransaksis.size();
						int index = 0;
						try {
							for (Long id : grupTransaksis) {
								index++;
								label.setValue("Memproses data transaksi ("
										+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");
								String kunciGrup = "ID:" + id;
								Session session = HibernateUtil.currentNativeSession();
								try {
									List<Object[]> transaksis = session.createCriteria(Transaksi.class)
											.setProjection(Projections.projectionList().add(Projections.sum("debet"))
													.add(Projections.sum("kredit")))
											.add(Restrictions.eq("grupTransaksi.id", id)).list();
									if (!transaksis.isEmpty()) {
										GrupTransaksi grupTransaksi = (GrupTransaksi) session
												.createCriteria(GrupTransaksi.class).add(Restrictions.idEq(id))
												.uniqueResult();
										if (grupTransaksi != null) {
											Number debet = (Number) transaksis.get(0)[0];
											Number kredit = (Number) transaksis.get(0)[1];
											kunciGrup = String.valueOf(grupTransaksi);
											System.out.println("Transaksi " + grupTransaksi + " debet " + debet
													+ " kredit " + kredit);
											grupTransaksi.setTotalDebet(debet == null ? 0.0 : debet.doubleValue());
											grupTransaksi.setTotalKredit(kredit == null ? 0.0 : kredit.doubleValue());
											session.getTransaction().begin();
											Common.refreshUpdate(session, grupTransaksi);
											session.getTransaction().commit();
										}
									}
									laporan.catatBerhasil(index - 1, kunciGrup, "Sinkronisasi total debet/kredit berhasil");
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
									laporan.catatGagalDetail(index - 1, kunciGrup, e);
								}
								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
								HibernateUtil.closeSession();
							}
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
							laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-grup): "
									+ ais.common.LaporanUpload.detailTeknisException(e));
						}
											} finally {
							label.setValue("");
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

			}
		});

		if (!ConstantValues.otomatisTerposting) {
			belumTerposting = new MyCheckboxConfig("Blm terposting");
			belumTerposting.setParent(find.getParent());
			belumTerposting.addEventListener("onClick", new EventListener() {

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
		}

		doubleTrx = new MyToolbarbuttonConfig("Posting Data Jurnal", "/img/Check-icon.png");
		if (doubleTrx != null) { doubleTrx.setParent(find.getParent()); }
		doubleTrx
				.setVisible(!ConstantValues.otomatisTerposting && edit && delete && (akuns == null || akuns.isEmpty()));
		doubleTrx.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final List<Long> grupTransaksis = initCriteria(false).setProjection(Projections.property("id")).list();
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
						int size = grupTransaksis.size();
						int index = 0;
						try {
							for (Long id : grupTransaksis) {
								index++;
								label.setValue("Memproses data transaksi ("
										+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");
								Session session = HibernateUtil.currentNativeSession();
								try {
									GrupTransaksi grupTransaksi = (GrupTransaksi) session
											.createCriteria(GrupTransaksi.class)
											.add(Restrictions.isNotNull("postingHistory")).add(Restrictions.idEq(id))
											.uniqueResult();
									if (grupTransaksi != null) {
										PostingHistory postingHistory = grupTransaksi.getPostingHistory();
										session.refresh(postingHistory);
										postingHistory.setPosting(true);
										session.getTransaction().begin();
										Common.refreshUpdate(session, postingHistory);
										session.getTransaction().commit();
									}
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}
								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
								HibernateUtil.closeSession();
							}
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
						label.setValue("");
											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		if (button != null) { button.setVisible(edit && delete && (akuns == null || akuns.isEmpty())); }
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiGrupTransaksiHelper revisiHelper = new RevisiGrupTransaksiHelper(new EventListener() {

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
		if (button != null) { button.setParent(find.getParent()); }

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Membatalkan seluruh posting transaksi jurnal umum yang terlihat di grid saat ini.
	 *
	 * <p><b>Tujuan:</b> Menyediakan fungsi massal untuk membatalkan status posting semua
	 * {@link ais.database.model.akunting.GrupTransaksi} yang sudah terposting (memiliki
	 * {@code PostingHistory}) dalam filter aktif. Operasi ini menghapus referensi
	 * {@code postingHistory} dari setiap {@code GrupTransaksi} sehingga jurnal kembali
	 * ke status "belum terposting" dan dapat diedit atau diposting ulang.</p>
	 *
	 * <p><b>Cara kerja:</b> Pertama menampilkan dialog konfirmasi kepada pengguna. Jika
	 * pengguna memilih OK, metode memanggil {@link #initCriteria(boolean)} dengan
	 * {@code order=true} untuk mendapatkan daftar semua {@code GrupTransaksi} yang
	 * terposting sesuai filter aktif. Kemudian iterasi setiap entitas, set
	 * {@code postingHistory=null}, dan simpan via {@code Common.refreshSaveOrUpdate}.
	 * Setelah selesai, refresh grid via timer.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak ada penanganan error eksplisit di level metode ini.
	 * Error dari Hibernate (mis. constraint violation) akan muncul sebagai exception tak
	 * tertangkap. Pertimbangkan untuk menambahkan try-catch jika diperlukan.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Operasi ini bersifat destruktif dan tidak dapat diurungkan
	 * tanpa backup. Pastikan hanya pengguna dengan hak APPROVE/admin yang dapat mengaksesnya.
	 * Tombol ini hanya terlihat jika {@code adminLain=true}.</p>
	 *
	 * @param event event ZK dari klik tombol "Batalkan Posting Semua"
	 * @throws Exception jika operasi Hibernate atau ZK gagal
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi jurnal umum ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<GrupTransaksi> grupTransaksis = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (GrupTransaksi grupTransaksi : grupTransaksis) {
								grupTransaksi.setPostingHistory(null);
								Common.refreshSaveOrUpdate(grupTransaksi);
							}
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(null);
							}
						});
					}
				});

	}

	/**
	 * Memposting semua transaksi jurnal umum yang belum terposting secara massal.
	 *
	 * <p><b>Tujuan:</b> Menyediakan fitur "posting batch" untuk semua
	 * {@link ais.database.model.akunting.GrupTransaksi} yang belum memiliki
	 * {@code PostingHistory} dalam filter aktif. Setiap jurnal yang balance (total debet
	 * sama dengan total kredit) akan mendapatkan {@code PostingHistory} baru dan semua
	 * {@code Transaksi}-nya akan diperbarui dengan status posting selesai dan tanggal
	 * posting. Jurnal yang tidak balance dicatat sebagai peringatan.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuka jendela modal kecil dengan form tanggal posting dan nama pengguna.</li>
	 *   <li>Pengguna mengisi tanggal dan mengklik "Simpan" yang memicu dialog konfirmasi
	 *       kedua.</li>
	 *   <li>Setelah konfirmasi OK, memulai thread latar yang mengiterasi semua
	 *       {@code GrupTransaksi} yang belum terposting sesuai filter aktif.</li>
	 *   <li>Untuk setiap jurnal: membuat {@code PostingHistory} baru, menghitung total
	 *       debet/kredit, memeriksa balance, dan jika balance memasang
	 *       {@code PostingHistory} ke {@code GrupTransaksi} dan semua {@code Transaksi}.</li>
	 *   <li>Jurnal tidak balance ditambahkan ke daftar peringatan.</li>
	 *   <li>Setelah selesai, menampilkan pesan sukses atau menawarkan download file error.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading:</b> Seluruh proses posting berjalan di thread non-UI menggunakan
	 * {@code HibernateUtil.currentNativeSession()}. Setiap iterasi membuka dan menutup
	 * session sendiri untuk menghindari session yang terlalu besar. Label progress
	 * diperbarui dengan persentase kemajuan.</p>
	 *
	 * <p><b>Penanganan error:</b> Error per-jurnal ditangkap dan dicatat sebagai peringatan
	 * tanpa menghentikan proses batch. Jika ada peringatan, pengguna dapat mengunduh
	 * file teks berisi daftar jurnal yang gagal.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini hanya tersedia jika
	 * {@code !ConstantValues.otomatisTerposting}. Jika sistem dikonfigurasi untuk
	 * posting otomatis, tombol ini tidak perlu ditampilkan.</p>
	 *
	 * @param event event ZK dari klik tombol "Posting Semua"
	 * @throws Exception jika pembuatan jendela modal atau komponen ZK gagal
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting transaksi jurnal umum");
		addWindow.setWidth("800px");
		addWindow.setHeight("300px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid mygrid = new MyGrid();
		mygrid.setWidth("100%");
		mygrid.setParent(center);
		mygrid.setWidth("100%");
		mygrid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(mygrid);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		Rows rows = new Rows();
		rows.setParent(mygrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal / Waktu"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diposting oleh"));
		final Label nama;
		row.appendChild(nama = new Label(tbmuser.getUserNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		final MyTextbox keterangan;
		row.appendChild(keterangan = new MyTextbox());
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Date tgl = tanggal.getValue();
				if (tgl == null) {
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting belum diisi. Langkah yang dapat dilakukan: (1) Isikan atau pilih Tanggal Posting menggunakan date picker; (2) Pastikan tanggal yang dipilih valid dan sesuai periode akuntansi berjalan; (3) ulangi proses posting. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi jurnal umum ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final List<String> warnings = new ArrayList<String>();

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											if (warnings.isEmpty()) {
												MyMessageboxConfig.show(
														"Posting transaksi jurnal umum berhasil dilakukan", "Informasi",
														MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
														new EventListener() {

															@Override
															public void onEvent(Event arg0) throws Exception {
																onSearchDefault(arg0);
															}
														});
											} else {
												MyMessageboxConfig.show(
														"Posting transaksi jurnal umum telah selesai dengan beberapa kesalahan, klik OK untuk download",
														"Informasi", MyMessageboxConfig.OK,
														MyMessageboxConfig.INFORMATION, new EventListener() {

															@Override
															public void onEvent(Event arg0) throws Exception {

																try {
																	File fileErr = new File(
																			Common.REAL_PATH + "/tmp/error_"
																					+ Common.randLong() + ".txt");
																	if (!fileErr.getParentFile().exists()) {
																		fileErr.getParentFile().mkdirs();
																	}

																	String err = "";
																	for (String s : warnings) {
																		err += (err.isEmpty() ? s : "\n\n" + s);
																	}

																	FileUtils.writeStringToFile(fileErr, err);
																	Filedownload.save(fileErr, "text/plain");
																	warnings.clear();
																} catch (Exception ee) {
																	Common.tampilErrorJikaAdmin(ee);
																}

																onSearchDefault(arg0);
															}
														});
											}
											addWindow.detach();
										}
									});

									new Thread(new Runnable() {

										@Override
										public void run() {
											try {

											List<GrupTransaksi> grupTransaksis = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();
											int size = grupTransaksis.size();
											int index = 0;
											for (GrupTransaksi grupTransaksi : grupTransaksis) {
												try {
													if (grupTransaksi != null) {
														index++;
														Session session = HibernateUtil.currentNativeSession();

														PostingHistory postingHistory = new PostingHistory(
																PostingHistory.JENIS_UMUM);
														postingHistory.setTbmuser(tbmuser);
														postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
														postingHistory.setKeterangan("Posting manual oleh "
																+ nama.getValue() + " pada waktu " + Common.dateFormat5.get()
																		.format(ais.ui.util.WaktuUtil.getDate()));
														session.getTransaction().begin();
														session.save(postingHistory);
														session.getTransaction().commit();

														// session.disconnect();
														if (session.isOpen()) {session.disconnect();session.close();}

														session = HibernateUtil.currentNativeSession();
														List<Transaksi> transaksis = session
																.createCriteria(Transaksi.class)
																.add(Restrictions.eq("grupTransaksi", grupTransaksi))
																.list();
														// session.disconnect();
														if (session.isOpen()) {session.disconnect();session.close();}

														Double totalDebet = 0.0;
														Double totalKredit = 0.0;

														String d = "";
														for (Transaksi transaksi : transaksis) {
															totalDebet += transaksi.getDebet();
															totalKredit += transaksi.getKredit();

															d += "\t" + transaksi.getAkun() == null ? ""
																	: transaksi.getAkun().getKode() + "-"
																			+ transaksi.getAkun().getNama();
															d += "\tD = "
																	+ Common.numberFormat.get().format(transaksi.getDebet());
															d += "\tK = "
																	+ Common.numberFormat.get().format(transaksi.getKredit());

														}

														label.setValue("("
																+ Common.numberFormat.get()
																		.format(((index * 1.0) / (size * 1.0)) * 100.0)
																+ "%) " + d);

														boolean balance = Common.numberFormat.get().format(totalDebet)
																.equals(Common.numberFormat.get().format(totalKredit));

														if (!balance) {
															warnings.add("Jurnal " + grupTransaksi.getKode() + " "
																	+ grupTransaksi.getKeterangan() + " "
																	+ Common.dateFormat.get()
																			.format(grupTransaksi.getTanggalTransaksi())
																	+ " tidak balance -> " + d);
														} else {

															try {
																session = HibernateUtil.currentNativeSession();
																session.refresh(grupTransaksi);
																grupTransaksi.setPostingHistory(postingHistory);
																session.getTransaction().begin();
																session.update(grupTransaksi);
																session.getTransaction().commit();

																// session.disconnect();
																if (session.isOpen()) {session.disconnect();session.close();}
															} catch (Exception e) {
																ais.common.Common.tampilErrorJikaAdmin(e);
															}

															try {

																for (Transaksi transaksi : transaksis) {

																	transaksi.setPostingHistory(postingHistory);
																	transaksi.setStatusPosting(
																			Transaksi.STATUS_POSTING_SELESAI);
																	transaksi.setTanggalPosting(
																			ais.ui.util.WaktuUtil.getDate());

																	session = HibernateUtil.currentNativeSession();
																	session.getTransaction().begin();
																	session.update(transaksi);
																	session.getTransaction().commit();
																	// session.disconnect();
																	if (session.isOpen()) {session.disconnect();session.close();}
																}

															} catch (Exception e) {
																ais.common.Common.tampilErrorJikaAdmin(e);
															}
														}

													}
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
												}
											}

											label.setValue("");
											HibernateUtil.closeSession();
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	class GrupTransaksiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final GrupTransaksi grupTransaksi = (GrupTransaksi) arg1;

			Closing closing = grupTransaksi.getClosing();

			Vbox a = RevisiHelper.createNewRevisi(GrupTransaksi.class, grupTransaksi, grupTransaksi.getKode());
			a.setParent(arg0);
			new Label(grupTransaksi.getJenisTransaksi() == null ? "" : grupTransaksi.getJenisTransaksi().getNama())
					.setParent(a);
			new Label(grupTransaksi.getSatuanKerja() == null ? "" : grupTransaksi.getSatuanKerja().getNama())
					.setParent(a);
			new Label(grupTransaksi.getWorkspace() == null ? "" : grupTransaksi.getWorkspace().toString()).setParent(a);

			new Label(closing == null ? "" : closing.getNama()).setParent(a);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			new Label(grupTransaksi.getTanggalTransaksi() == null ? ""
					: Common.dateFormat3.get().format(grupTransaksi.getTanggalTransaksi())).setParent(vbox);

			Vbox myvbox = new Vbox();
			myvbox.setParent(vbox);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, grupTransaksi.getId(), "Bukti Transaksi Jurnal Umum",
					"Bukti Transaksi", true, null, null, false, false, false, false);

			new Label(grupTransaksi.getPegawai() == null ? "" : grupTransaksi.getPegawai().getNama()).setParent(arg0);

			// FIX statement timeout (KE-8) menggagalkan SELURUH grid: populateDeskripsiLengkap()
			// menjalankan query per-baris saat grid dirender -- bila SATU baris kena statement_timeout
			// (kontensi DB sesaat), exception mentah dari sini dulu merusak render SELURUH grid (bukan
			// cuma baris ini), krn tidak ada try/catch di titik ini. Isolasi kegagalan ke baris ybs saja.
			Grid grid;
			Boolean balance;
			try {
				Object[] o = grupTransaksi.populateDeskripsiLengkap();
				grid = (Grid) o[1];
				balance = (Boolean) o[0];
			} catch (Exception eDeskripsi) {
				ais.common.ErrorAuditUtil.record(eDeskripsi,
						"GrupTransaksiRenderer.render: gagal populateDeskripsiLengkap utk grupTransaksi id=" + grupTransaksi.getId());
				grid = new Grid();
				grid.setWidth("100%");
				org.zkoss.zul.Rows rowsGagal = new org.zkoss.zul.Rows();
				rowsGagal.setParent(grid);
				org.zkoss.zul.Row rowGagal = new org.zkoss.zul.Row();
				rowGagal.setParent(rowsGagal);
				new ais.ui.util.MyLabelAgakKecilBoldMerah(
						"Rincian jurnal gagal dimuat (kendala sementara pada sistem) -- muat ulang halaman untuk mencoba lagi.")
						.setParent(rowGagal);
				balance = false;
			}
			grid.setParent(arg0);

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setDisabled(!balance);
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					LaporanBuktiPengeluaranKas buktiPengeluaranKas = new LaporanBuktiPengeluaranKas(grupTransaksi);
					buktiPengeluaranKas.setTitle("Laporan");
					buktiPengeluaranKas.setClosable(true);
					buktiPengeluaranKas.setHeight("90%");
					buktiPengeluaranKas.setWidth("900px");
					buktiPengeluaranKas.setParent(page.getFirstRoot());
					buktiPengeluaranKas.onModal();
				}
			});
			aksiButtons.add(button);

			if (!semua) {

				button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data Jurnal");
				// Batalkan posting PER BARIS mengikuti ketersediaan tombol "Batalkan Posting Semua"
				// (yang di zul tampil tanpa gating). Jadi tampil untuk setiap jurnal yang SUDAH
				// diposting dan BELUM masuk closing — konsisten dgn aksi massal di atas daftar.
				button.setVisible(grupTransaksi.getPostingHistory() != null && closing == null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								session.createSQLQuery("delete from akunting.posting_history where id="
										+ grupTransaksi.getPostingHistory().getId()).executeUpdate();

								onSearchDefault(null);
							}
						});

					}

				});
				aksiButtons.add(button);

				button = new MyToolbarbuttonConfig("", "/img/svg/check2-circle.svg");
				button.setTooltiptext("Posting Data Jurnal");
				button.setDisabled(!balance);
				button.setVisible(edit && grupTransaksi.getPostingHistory() == null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event arg0) throws Exception {

								Session session = HibernateUtil.currentSession();

								List<Transaksi> transaksis = session.createCriteria(Transaksi.class)
										.addOrder(Order.desc("debet")).addOrder(Order.asc("tanggalTransaksi"))
										.add(Restrictions.eq("grupTransaksi", grupTransaksi)).list();

								Double totalDebet = 0.0;
								Double totalKredit = 0.0;

								String d = "";
								for (Transaksi transaksi : transaksis) {
									totalDebet += transaksi.getDebet();
									totalKredit += transaksi.getKredit();

									d += "\t" + transaksi.getAkun() == null ? ""
											: transaksi.getAkun().getKode() + "-" + transaksi.getAkun().getNama();
									d += "\tD = " + Common.numberFormat.get().format(transaksi.getDebet());
									d += "\tK = " + Common.numberFormat.get().format(transaksi.getKredit());

								}

								boolean balance = Common.numberFormat.get().format(totalDebet)
										.equals(Common.numberFormat.get().format(totalKredit));

								if (!balance) {

									MyMessageboxConfig.showFormat(
											"Mohon maaf, jurnal dengan nomor \"{V1}\" ({V2}) tertanggal {V3} belum dapat diposting karena total debet dan total kredit tidak seimbang (tidak balance). Rincian transaksi: {V4}. Langkah yang dapat dilakukan: (1) periksa kembali nilai debet dan kredit pada setiap baris transaksi; (2) pastikan total debet sama persis dengan total kredit; (3) perbaiki data yang tidak sesuai, kemudian lakukan posting ulang.",
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
											grupTransaksi.getKode(), grupTransaksi.getKeterangan(),
											Common.dateFormat.get().format(grupTransaksi.getTanggalTransaksi()), d);
								} else {

									PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_UMUM);
									postingHistory.setTbmuser(Common.getCurrentUser());
									postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
									postingHistory.setKeterangan("Posting manual oleh "
											+ Common.getCurrentUser().getUserNama() + " pada waktu "
											+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
									session.save(postingHistory);

									for (Transaksi transaksi : transaksis) {
										GrupTransaksi grupTransaksi = transaksi.getGrupTransaksi();

										transaksi.setPostingHistory(postingHistory);

										transaksi.setStatusPosting(Transaksi.STATUS_POSTING_SELESAI);
										transaksi.setTanggalPosting(ais.ui.util.WaktuUtil.getDate());

										if (grupTransaksi != null) {
											session.refresh(grupTransaksi);
											grupTransaksi.setPostingHistory(postingHistory);
											session.update(grupTransaksi);
										}

										session.update(transaksi);

									}
								}

								onSearchDefault(null);
							}
						});

					}

				});
				aksiButtons.add(button);

				button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
				button.setTooltiptext("Ubah Data");
				button.setVisible(edit && closing == null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						TransaksiJurnalUmumHelper addWindow = new TransaksiJurnalUmumHelper(grupTransaksi);
						addWindow.setEventListener(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
						addWindow.setVisible(true);
						addWindow.onModal();
					}

				});
				aksiButtons.add(button);

			}

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && grupTransaksi.getPostingHistory() == null && closing == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Question",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Session session = HibernateUtil.currentSession();
											try {
												session.refresh(grupTransaksi);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/GrupTransaksiAction.java:1696");
												// TODO: handle exception
											}
											session.delete(grupTransaksi);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain. Keterangan kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan tidak ada transaksi atau referensi yang masih menggunakan data ini; (3) apabila kesalahan masih berlanjut, mohon menghubungi administrator sistem.",
													e.getMessage()));
										}

									}

								}
							});

				}
			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

			if (delete && edit && grupTransaksi.getPostingHistory() != null) {
				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Posting");
				checkbox.setChecked(grupTransaksi.getPostingHistory().getPosting());
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						PostingHistory postingHistory = grupTransaksi.getPostingHistory();
						session.refresh(postingHistory);
						postingHistory.setPosting(checkbox.isChecked());
						Common.refreshUpdate(session, postingHistory);
					}
				});
			} else {
				new Label().setParent(arg0);
			}
		}

	}

	/**
	 * Membuka formulir tambah jurnal baru via {@code TransaksiJurnalUmumHelper}.
	 *
	 * <p><b>Tujuan:</b> Event handler untuk tombol "Tambah" di toolbar yang membuka
	 * dialog modal penginputan jurnal umum baru. Membuat entitas {@code GrupTransaksi}
	 * kosong yang sudah dikaitkan dengan {@code Workspace} aktif (jika ada).</p>
	 *
	 * <p><b>Cara kerja:</b> Membuat instance baru {@code GrupTransaksi}, menyetel
	 * {@code workspace} dari field instance jika tersedia, lalu membuka
	 * {@code TransaksiJurnalUmumHelper} sebagai modal window. Helper tersebut
	 * menyediakan form entri jurnal lengkap dengan baris debet/kredit. Setelah
	 * helper ditutup (simpan atau batal), event listener memanggil
	 * {@link #onSearchDefault(Event)} untuk memperbarui grid.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tombol Tambah hanya terlihat jika pengguna memiliki hak
	 * {@code CREATE} dan konteks tidak dalam mode view-only (parameter URL dari modul
	 * lain). Pastikan {@code TransaksiJurnalUmumHelper} menangani {@code GrupTransaksi}
	 * baru dengan benar termasuk generate kode otomatis.</p>
	 *
	 * @param event event ZK dari klik tombol Tambah
	 * @throws Exception jika pembuatan atau pembukaan modal window gagal
	 */
	public void onAdd(Event event) throws Exception {
		GrupTransaksi grupTransaksi = new GrupTransaksi();
		grupTransaksi.setWorkspace(workspace);
		TransaksiJurnalUmumHelper addWindow = new TransaksiJurnalUmumHelper(grupTransaksi);
		addWindow.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Mengambil label item yang terpilih dari sebuah {@code Combobox} secara aman.
	 *
	 * <p><b>Tujuan:</b> Fungsi pembantu (helper) untuk mengekstrak label teks dari
	 * item yang sedang dipilih di sebuah {@code Combobox}, dengan penanganan null yang
	 * lengkap. Digunakan terutama untuk membaca nilai filter-filter dropdown seperti
	 * {@code searchStatusPosting} dan {@code searchJenisJurnal} sebelum membangun
	 * Hibernate Criteria.</p>
	 *
	 * <p><b>Cara kerja:</b> Memeriksa secara berurutan: apakah {@code Combobox} null,
	 * apakah ada item terpilih ({@code getSelectedItem()}), dan apakah label item tersebut
	 * tidak null. Jika semua kondisi terpenuhi, mengembalikan label yang sudah di-trim.
	 * Jika salah satu kondisi gagal, mengembalikan string kosong {@code ""}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini bersifat {@code private static} — dapat dipindah
	 * ke kelas utility jika dibutuhkan di tempat lain. Tidak ada efek samping.</p>
	 *
	 * @param c {@code Combobox} yang akan dibaca nilai pilihannya; boleh null
	 * @return label item terpilih (sudah di-trim), atau {@code ""} jika tidak ada pilihan
	 *         atau combobox null
	 */
	private static String comboLabel(Combobox c) {
		if (c == null || c.getSelectedItem() == null || c.getSelectedItem().getLabel() == null) {
			return "";
		}
		return c.getSelectedItem().getLabel().trim();
	}

	/**
	 * Memeriksa apakah label combobox status posting sama dengan nilai yang diberikan.
	 *
	 * <p><b>Tujuan:</b> Fungsi pembantu untuk perbandingan status posting yang case-insensitive
	 * antara nilai parameter {@code label} dengan label item yang terpilih di
	 * {@code searchStatusPosting}. Digunakan di {@link #initCriteria(boolean)} untuk
	 * menentukan apakah filter "Sudah Diposting" atau "Belum Diposting" aktif.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link #comboLabel(Combobox)} dengan
	 * {@code searchStatusPosting} untuk mendapatkan label terpilih, kemudian
	 * membandingkannya dengan {@code label} menggunakan {@code equalsIgnoreCase}
	 * sehingga perbandingan tidak peka huruf besar/kecil.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Nilai yang diharapkan untuk {@code label} adalah
	 * "Sudah Diposting" atau "Belum Diposting" — sesuaikan dengan label yang
	 * didefinisikan di file ZUL untuk {@code searchStatusPosting}.</p>
	 *
	 * @param label string yang akan dibandingkan dengan label terpilih di combobox
	 *              status posting; perbandingan tidak peka huruf besar/kecil
	 * @return {@code true} jika label terpilih di combobox sama dengan {@code label}
	 *         (case-insensitive); {@code false} dalam kasus lain
	 */
	private boolean isStatusPosting(String label) {
		return label.equalsIgnoreCase(comboLabel(searchStatusPosting));
	}

	/**
	 * Membangun Hibernate {@code Criteria} untuk query daftar {@code GrupTransaksi}.
	 *
	 * <p><b>Tujuan:</b> Metode inti yang mengkonstruksi query database secara dinamis
	 * berdasarkan seluruh state filter yang aktif: parameter URL yang diterima saat
	 * inisialisasi, nilai input filter di toolbar pencarian, dan mode tampilan
	 * ({@code semua}, {@code hanyaJurnalUmum}). Metode ini diimplementasikan dari
	 * antarmuka {@code DataCriteria} dan dipanggil oleh {@link #onSearchDefault(Event)},
	 * oleh tombol toolbar admin (sinkronisasi, posting massal), dan oleh komponen
	 * ekspor/cetak.</p>
	 *
	 * <p><b>Cara kerja — urutan penambahan klausa filter:</b>
	 * <ol>
	 *   <li><b>Filter entitas modul</b>: jika parameter URL modul aktif ({@code pertangungjawaban},
	 *       {@code kasKecil}, {@code kasBesar}, {@code pajak}, {@code pemesananPengadaanMasterAsset},
	 *       {@code saldoAwalMasterAsset}), tambahkan {@code isNotNull} pada field terkait
	 *       dan set {@code hanyaJurnalUmum=false}.</li>
	 *   <li><b>Filter dasbor</b>: jika {@code entitasDasbor} aktif, filter berdasarkan
	 *       entitas + restriksi ref dan jenis dari {@code PostingJurnalHelper}.</li>
	 *   <li><b>Filter tanggal closing</b>: jika {@code tgl_closing} aktif, filter jurnal
	 *       yang tanggalnya sebelum atau sama dengan tgl_closing dan belum closing.</li>
	 *   <li><b>Filter id grup</b>: jika {@code idGrup} aktif (dari parameter URL {@code grup}),
	 *       filter hanya jurnal dengan id tersebut.</li>
	 *   <li><b>Filter utama</b>: filter posting (belum/sudah), satuan kerja, workspace,
	 *       status posting (combobox baru), rentang tanggal, jenis transaksi, kode jurnal.</li>
	 *   <li><b>Filter tambahan baru</b>: jenis jurnal (combobox), rentang nominal
	 *       (totalDebet min/maks), nomor bukti (id PostingHistory).</li>
	 *   <li><b>Filter sudah_closing/sudah_posting</b>: filter berdasarkan parameter URL
	 *       dari dasbor draft jurnal, dengan alias {@code postingHistory}.</li>
	 *   <li><b>Filter closing</b>: jika mode closing aktif, filter berdasarkan entitas
	 *       {@code Closing} yang dipilih.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Kasus khusus akun</b>: jika parameter akun diterima (tunggal atau jamak),
	 * query di-switch dari {@code GrupTransaksi} ke {@code Transaksi} dengan GROUP BY
	 * {@code grupTransaksi}. Mode ini menonaktifkan filter dan tombol Tambah.</p>
	 *
	 * <p><b>Penanganan error:</b> Jika {@code searchparent} (atau beberapa field filter)
	 * null (kasus embedded), ditangani dengan {@code Restrictions.sqlRestriction("1=1")}.
	 * Error parsing nomor bukti diabaikan diam-diam.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Urutan klausa filter PENTING — alias Hibernate tidak boleh
	 * dibuat dua kali pada criteria yang sama. Klausa {@code postingHistory} harus
	 * dijaga agar hanya ada satu alias. Saat menambah filter baru, selalu periksa
	 * apakah alias yang dibutuhkan sudah ada.</p>
	 *
	 * @param order {@code true} untuk menambahkan ORDER BY (tanggal atau kode sesuai
	 *              konfigurasi); {@code false} untuk query COUNT tanpa ordering
	 * @return {@code Criteria} Hibernate yang siap dieksekusi, tidak pernah null
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Akun akun = (Akun) searchakun.getAttribute("akun");

		Criteria criteria = session.createCriteria(GrupTransaksi.class);

		if (pertangungjawaban != null && pertangungjawaban) {
			criteria.add(Restrictions.isNotNull("pertangungjawaban"));
			hanyaJurnalUmum = false;
		}

		if (kasKecil != null && kasKecil) {
			criteria.add(Restrictions.isNotNull("kasKecil"));
			hanyaJurnalUmum = false;
		}

		if (kasBesar != null && kasBesar) {
			criteria.add(Restrictions.isNotNull("kasBesar"));
			hanyaJurnalUmum = false;
		}

		if (pajak != null && pajak) {
			criteria.add(Restrictions.isNotNull("pajak"));
			hanyaJurnalUmum = false;
		}

		if (pemesananPengadaanMasterAsset != null && pemesananPengadaanMasterAsset) {
			criteria.add(Restrictions.isNotNull("pemesananPengadaanMasterAsset"))
					.add(dp_balik != null && dp_balik ? Restrictions.sqlRestriction("ref = 'DP_PEKERJAAN'")
							: Restrictions.sqlRestriction("ref is null"));
			hanyaJurnalUmum = false;
		}

		if (saldoAwalMasterAsset != null && saldoAwalMasterAsset) {
			criteria.add(Restrictions.isNotNull("saldoAwalMasterAsset")).add(dp_vendor != null && dp_vendor
					? Restrictions.sqlRestriction("ref = 'DP_BALIK_PEKERJAAN'")
					: termin == null ? Restrictions.sqlRestriction("ref is null")
							: Restrictions.sqlRestriction(
									"ref is not null and ref != 'DP_PEKERJAAN' and ref != 'DP_BALIK_PEKERJAAN'"));
			hanyaJurnalUmum = false;
		}

		if (entitasDasbor != null) {
			criteria.add(Restrictions.isNotNull(entitasDasbor))
					.add(ais.action.master.helper.PostingJurnalHelper.restriksiRefClosing(refDasbor))
					.add(ais.action.master.helper.PostingJurnalHelper.restriksiJenisClosing(jenisDasbor));
			hanyaJurnalUmum = false;
		}

		if (tgl_closing != null && !tgl_closing.trim().isEmpty()) {

			try {
				Date tgl = Common.dateFormat85.get().parse(tgl_closing);

				criteria.add(Restrictions.sqlRestriction("date(this_.tanggal_transaksi)<=date('"
						+ Common.databaseDateFormat.get().format(tgl) + "') and this_.closing is null"));

			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

		} else if (idGrup != null) {
			criteria.add(Restrictions.idEq(idGrup));
		} else {

			if (closing == null) {
				criteria.add(searchtampil == null || !searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"));

				if ((akuns != null && !akuns.isEmpty()) || akun != null
						|| !searchketerangan.getValue().trim().isEmpty()) {
					criteria = session.createCriteria(Transaksi.class)
							.setProjection(Projections.groupProperty("grupTransaksi"))

							.add(akuns != null && !akuns.isEmpty() ? Restrictions.in("akun", akuns)
									: akun == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("akun", akun))

							.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(),
											MatchMode.ANYWHERE))

							.createCriteria("grupTransaksi");

				} else if (order && (tidakbalance == null || !tidakbalance.isChecked())) {
					if (Common.bolehKonfigurasi("grup_transaksi_order_by_tanggal")) {
						criteria.addOrder(Order.desc("tanggalTransaksi"));
					} else {
						criteria.addOrder(Order.desc("kode"));
					}
				}

				criteria.add(tidakbalance != null && tidakbalance.isChecked()
						? Restrictions.sqlRestriction("total_debet != total_kredit")
						: Restrictions.sqlRestriction("true"))

						.add((searchsatuanKerja == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsatuanKerja.getAttribute("satuanKerja") == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("satuanKerja", searchsatuanKerja.getAttribute("satuanKerja"))))

						.add(workspace == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("workspace", workspace))

						.add(isStatusPosting("Sudah Diposting") ? Restrictions.isNotNull("postingHistory")
								: isStatusPosting("Belum Diposting") ? Restrictions.isNull("postingHistory")
								: (akuns != null && !akuns.isEmpty()) ? Restrictions.isNotNull("postingHistory")
								: semua ? (searchtampilsemua != null && searchtampilsemua.isChecked()
										? Restrictions.sqlRestriction("true")
										: Restrictions.isNotNull("postingHistory"))

										// Draft Jurnal utama: tampilkan draft Jurnal Umum DAN SEMUA yang sudah
										// ter-posting (punya no. bukti) apa pun jenisnya — supaya transaksi
										// terposting tidak hilang dari daftar.
										: (hanyaJurnalUmum
												? Restrictions.or(Restrictions.eq("jenisJurnal", Transaksi.JURNAL_UMUM),
														Restrictions.isNotNull("postingHistory"))
												: Restrictions.sqlRestriction("true")))

						.add((searchmulai == null || searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_transaksi) between date('"
								+ Common.databaseDateFormat.get().format(searchmulai.getValue()) + "') and  date('"
								+ Common.databaseDateFormat.get().format(searchsampai.getValue()) + "')")))

						.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenisTransaksi", searchjenis.getSelectedItem().getValue()))

						.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("kode", searchnama.getValue().trim(), MatchMode.ANYWHERE));

				// ── Filter tambahan: Jenis Jurnal, Rentang Nominal, No. Bukti posting ──
				String jenisJurnalFilter = comboLabel(searchJenisJurnal);
				if (!jenisJurnalFilter.isEmpty() && !"Semua".equalsIgnoreCase(jenisJurnalFilter)) {
					criteria.add(Restrictions.eq("jenisJurnal", jenisJurnalFilter));
				}

				if (searchNominalMin != null && searchNominalMin.getValue() != null) {
					criteria.add(Restrictions.ge("totalDebet", searchNominalMin.getValue()));
				}
				if (searchNominalMax != null && searchNominalMax.getValue() != null) {
					criteria.add(Restrictions.le("totalDebet", searchNominalMax.getValue()));
				}

				if (searchNoBukti != null && searchNoBukti.getValue() != null) {
					String bukti = searchNoBukti.getValue().replaceAll("[^0-9]", "");
					if (!bukti.trim().isEmpty()) {
						try {
							PostingHistory phFilter = new PostingHistory();
							phFilter.setId(Long.valueOf(bukti.trim()));
							criteria.add(Restrictions.eq("postingHistory", phFilter));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/GrupTransaksiAction.java:2030");
							// input bukti tidak valid -> abaikan filter
						}
					}
				}

				if (sudah_closing != null) {
					/*
					 * Dasbor draft jurnal mengirim sudah_closing true/false.
					 * Keduanya wajib difilter supaya angka yang di-click sama
					 * dengan data yang keluar.
					 */
					criteria.add(sudah_closing ? Restrictions.isNotNull("closing") : Restrictions.isNull("closing"));
				}

				else if ((sudah_posting != null && !sudah_posting)
						|| (belumTerposting != null && belumTerposting.isChecked())) {
					criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN)
							.add(Restrictions.or(Restrictions.isNull("postingHistory.id"),
									Restrictions.eq("postingHistory.posting", false)));
					if (sudah_posting != null) {
						/* Hitungan dasbor draft jurnal mengecualikan yang sudah closing. */
						criteria.add(Restrictions.isNull("closing"));
					}
				} else if ((sudah_posting != null && sudah_posting)) {
					criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN)
							.add(Restrictions.and(Restrictions.isNotNull("postingHistory.id"),
									Restrictions.eq("postingHistory.posting", true)))
							.add(Restrictions.isNull("closing"));
				}

			} else {
				criteria.add(Restrictions.eq("closing", closing));
				if (order) {
					criteria.addOrder(Order.desc("tanggalTransaksi"));
				}
			}
		}
		return criteria;
	}

	/**
	 * Menjalankan pencarian ulang dan memperbarui tampilan grid jurnal.
	 *
	 * <p><b>Tujuan:</b> Metode pencarian default yang diimplementasikan dari antarmuka
	 * {@code DataSearchDefault}. Metode ini dipanggil oleh: event paging, event perubahan
	 * filter (satuan kerja, akun), setelah operasi CRUD selesai (tambah/ubah/hapus),
	 * setelah posting/pembatalan posting, dan saat inisialisasi awal via timer.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code Common.initPaging} dengan {@code initCriteria(false)} (tanpa
	 *       ORDER BY) untuk menghitung total record dan mengatur navigasi paging.</li>
	 *   <li>Memanggil {@code initCriteria(true)} (dengan ORDER BY) dengan batasan
	 *       {@code ROWS_COUNT_ON_PAGE} record dan offset sesuai halaman aktif.</li>
	 *   <li>Membuat {@code SimpleListModel} dari hasil query dan menyetelnya ke {@code grid}
	 *       dengan renderer {@code GrupTransaksiRenderer}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Tidak ada penanganan error eksplisit — exception dari
	 * Hibernate atau rendering diteruskan ke ZK framework. Jika query gagal, grid
	 * kemungkinan menampilkan pesan error default ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Konstanta {@code Common.ROWS_COUNT_ON_PAGE} menentukan
	 * jumlah baris per halaman. Jika perlu mengubah ukuran halaman untuk halaman tertentu,
	 * override nilai setelah pemanggilan atau gunakan paging custom.</p>
	 *
	 * @param event event ZK yang memicu pencarian (bisa null jika dipanggil langsung)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<GrupTransaksi> grupTransaksi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(grupTransaksi);
		grid.setRowRenderer(new GrupTransaksiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
