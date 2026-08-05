package ais.action.master.asset;

import java.util.Calendar;
import java.util.Date;
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
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
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

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemilikAsset;
import ais.database.model.rab.SatuanKerja;
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
 * <h3>PostingPemesananDpAction — Halaman Posting Jurnal Pembayaran DP Pemesanan Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah ZKoss Composer yang mengelola halaman posting jurnal akuntansi
 * untuk transaksi uang muka (Down Payment / DP) pada pemesanan pengadaan aset.
 * Modul ini memungkinkan petugas keuangan memposting atau membatalkan posting
 * entri jurnal debet-kredit yang terkait dengan kewajiban DP kepada vendor/penyedia
 * pada saat Purchase Order (PO) pemesanan aset disetujui. Setiap pemesanan yang
 * telah disetujui dan memiliki nilai DP akan menghasilkan satu pasang entri jurnal
 * berdasarkan konfigurasi akun debet (akun DP) dan akun kredit (akun utang DP)
 * dari jenis pemesanan yang bersangkutan.
 *
 * <b>Cara kerja:</b><br>
 * Saat halaman diinisialisasi melalui {@link #doAfterCompose(Component)}, kelas ini
 * memuat data pemesanan yang telah disetujui dari database menggunakan Hibernate
 * Criteria yang dibangun oleh {@link #initCriteria(boolean)}. Filter tersedia
 * berdasarkan pemilik aset, lokasi, ruang, rentang tanggal persetujuan, kode/keterangan,
 * serta status posting. Metode ini juga mendukung parameter URL {@code sudah_posting}
 * yang secara otomatis membatasi tampilan hanya pada data yang sudah atau belum
 * diposting, serta parameter {@code mulai} dan {@code sampai} untuk mengunci rentang
 * tanggal dari pemanggil luar (misalnya dari dashboard akuntansi).
 *
 * Proses pemuatan data menggunakan mekanisme progress loading non-blokir via
 * {@link #loadDataDenganProgressPosting(Event)} dengan flag anti-tumpang tindih.
 * Setiap baris dirender oleh {@link PemesananPengadaanMasterAssetRenderer} yang
 * menampilkan informasi kode, penyedia, jenis pemesanan, nilai DP, tanggal
 * persetujuan, preview jurnal, status posting, dan tombol aksi.
 *
 * Posting massal berjalan di thread terpisah untuk menjaga responsivitas UI,
 * dilengkapi progress bar yang diperbarui per-transaksi. Logika penentuan posisi
 * debet/kredit bergantung pada tanda nilai DP: positif menggunakan debet di akun
 * DP dan kredit di akun utang DP; negatif membalik posisi tersebut.
 *
 * <b>Threading:</b><br>
 * Proses posting massal di {@link #onPostingSemua(Event)} menggunakan native session
 * Hibernate ({@link HibernateUtil#currentNativeSession()}) yang dibuka dan ditutup
 * secara eksplisit di dalam thread latar. Flag {@code postingJurnalLoadingAktif}
 * dan {@code postingJurnalReloadTertunda} tidak disinkronkan dengan {@code synchronized},
 * aman hanya dalam model event-driven single-thread ZKoss per sesi pengguna.
 *
 * <b>Pemeliharaan:</b><br>
 * Jika skema tabel {@code akunting.grup_transaksi} berubah (khususnya nama kolom
 * foreign key pemesanan), perbarui query SQL native di metode pembatalan posting.
 * Jika konfigurasi akun DP berubah, cukup ubah pada entitas
 * {@code JenisPemesananPengadaanAsset}. Jika parameter URL baru perlu didukung,
 * tambahkan penanganannya di {@link #doAfterCompose(Component)}.
 *
 * @author AIS Development Team
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 * @see PemesananPengadaanMasterAsset
 * @see PostingHistory
 * @see CommonAkunting
 */
public class PostingPemesananDpAction extends GenericAutowireComposer {

	/**
	 * Nomor versi serial untuk serialisasi kelas ini sesuai kontrak {@code Serializable}.
	 * Nilai ini digunakan oleh Java untuk memverifikasi kompatibilitas versi saat
	 * deserialisasi. Tidak perlu diubah kecuali struktur field yang diserialisasi berubah.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/**
	 * Komponen grid utama halaman yang menampilkan daftar pemesanan pengadaan aset
	 * beserta status posting DP masing-masing. Grid diperbarui setiap kali filter
	 * berubah atau setelah operasi posting/pembatalan selesai.
	 */
	private MyGrid grid;

	/**
	 * Komponen paging untuk navigasi halaman data pada grid pemesanan. Diinisialisasi
	 * dengan listener yang memanggil {@link #loadDataDenganProgressPosting(Event)}
	 * setiap kali halaman aktif berubah.
	 */
	private Paging paging;

	/**
	 * Kotak pencarian teks untuk memfilter pemesanan berdasarkan kode atau keterangan.
	 * Pencarian bersifat case-insensitive menggunakan mode ANYWHERE (substring match).
	 */
	private Textbox searchkode;

	/**
	 * Combobox filter pemilik aset. Memuat semua {@code PemilikAsset} yang aktif.
	 * Jika item terpilih, criteria query akan dibatasi ke pemilik aset tersebut.
	 */
	private Combobox searchpemilikAsset;

	/**
	 * Combobox filter lokasi aset. Memuat semua {@code Lokasi} yang aktif.
	 * Jika sesi menyimpan atribut "Lokasi", combo otomatis terpilih dan dinonaktifkan.
	 */
	private Combobox searchlokasi;

	/**
	 * Banbox (autocomplete) untuk filter ruang/lokasi detail. Nilai ruang yang dipilih
	 * disimpan sebagai atribut komponen dengan key "ruang". Jika komponen ini null
	 * atau tidak ada ruang terpilih, filter ruang tidak diterapkan.
	 */
	private AmbilDataRuangBanbox searchruang;

	/**
	 * Checkbox filter "Tampilkan yang belum diposting". Jika dicentang, query criteria
	 * akan menyertakan kondisi postingHistory null atau belum di-posting. Dinonaktifkan
	 * secara otomatis jika {@code searchtelahtampil} dicentang.
	 */
	private MyCheckboxConfig searchtampil;

	/**
	 * Checkbox filter "Tampilkan yang telah diposting". Jika dicentang, hanya
	 * pemesanan dengan posting history aktif yang ditampilkan. Dinonaktifkan
	 * secara otomatis jika {@code searchtampil} dicentang.
	 */
	private MyCheckboxConfig searchtelahtampil;

	/**
	 * Flag hak akses UPDATE pengguna saat ini. Diinisialisasi saat halaman dibuka
	 * dan digunakan untuk mengontrol visibilitas tombol Posting dan Batalkan Posting.
	 */
	private boolean edit = false;

	/**
	 * Tombol toolbar untuk posting massal semua transaksi DP pemesanan. Hanya
	 * tampil jika pengguna memiliki hak UPDATE.
	 */
	private MyToolbarbuttonConfig sent;

	/**
	 * Flag yang menandakan apakah pengguna adalah admin atau memiliki privilege
	 * APPROVE. Hanya admin/approver yang dapat membatalkan posting per-baris.
	 */
	public boolean adminLain;

	/**
	 * Datebox untuk batas awal periode tanggal persetujuan pemesanan. Diinisialisasi
	 * default ke 6 bulan sebelum hari ini, bersifat read-only. Dapat di-override
	 * oleh parameter URL "mulai" (format yyyyMMdd), yang dalam kasus tersebut juga
	 * akan dinonaktifkan.
	 */
	private MyDatebox tglMulai;

	/**
	 * Datebox untuk batas akhir periode tanggal persetujuan pemesanan. Diinisialisasi
	 * default ke hari ini, bersifat read-only. Dapat di-override oleh parameter URL
	 * "sampai" (format yyyyMMdd).
	 */
	private MyDatebox tglSampai;

	/**
	 * Referensi ke pengguna yang sedang login, diambil dari sesi saat inisialisasi.
	 * Digunakan untuk konteks posting manual dan penentuan satuan kerja default.
	 */
	private Tbmuser tbmuser;

	/**
	 * Komponen North (panel filter bagian atas) yang disembunyikan jika halaman
	 * dibuka dengan parameter URL {@code sudah_posting}, karena dalam mode tersebut
	 * filter tidak diperlukan.
	 */
	private North filter;

	/**
	 * Baris khusus yang ditampilkan saat halaman dibuka dalam mode parameter URL
	 * {@code sudah_posting}, menggantikan panel filter yang disembunyikan.
	 */
	private Row rowPosting;

	/**
	 * Nilai dari parameter URL {@code sudah_posting}. Jika {@code true}, halaman
	 * hanya menampilkan pemesanan yang sudah diposting. Jika {@code false}, hanya
	 * menampilkan yang belum diposting. Jika {@code null} (tidak ada parameter),
	 * menggunakan filter manual dari checkbox.
	 */
	private Boolean sudah_posting = null;

	/**
	 * <b>Tujuan:</b> Dipanggil oleh ZKoss sebelum proses komposisi halaman dimulai,
	 * untuk memeriksa keamanan akses sebelum komponen ZUL dibuat.<br>
	 *
	 * <b>Cara kerja:</b> Memanggil {@link Common#doCheckSecurity()} yang mengalihkan
	 * pengguna ke login jika sesi tidak valid. Kemudian mendelegasikan ke superclass
	 * untuk melanjutkan proses komposisi ZKoss normal.<br>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code page} — Halaman ZKoss yang sedang dikomposisi.</li>
	 *   <li>{@code parent} — Komponen induk dalam hierarki halaman.</li>
	 *   <li>{@code compInfo} — Metadata komponen ZKoss.</li>
	 * </ul>
	 *
	 * <b>Return:</b> {@code ComponentInfo} dari superclass untuk melanjutkan komposisi.<br>
	 *
	 * <b>Penanganan error:</b> Jika sesi tidak aman, {@code doCheckSecurity} melakukan
	 * redirect dan eksekusi berhenti. Tidak ada exception yang dilempar langsung.<br>
	 *
	 * <b>Pemeliharaan:</b> Jangan menghapus pemanggilan {@code doCheckSecurity()}.
	 *
	 * @param page halaman ZKoss yang sedang dikomposisi
	 * @param parent komponen induk dalam hierarki ZUL
	 * @param compInfo metadata komponen ZKoss
	 * @return {@code ComponentInfo} untuk melanjutkan proses komposisi
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Metode inisialisasi utama yang dipanggil ZKoss setelah semua
	 * komponen ZUL berhasil di-autowire. Bertanggung jawab atas persiapan lengkap
	 * halaman posting DP pemesanan, dari validasi sesi hingga pemuatan data awal.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan autowiring.</li>
	 *   <li>Menginisialisasi bahasa antarmuka via {@link Common#initLaguage()}.</li>
	 *   <li>Memeriksa sesi dan hak READ; jika tidak valid, arahkan ke login.</li>
	 *   <li>Mengambil pengguna saat ini ke field {@code tbmuser}.</li>
	 *   <li>Membaca parameter URL {@code sudah_posting}; jika ada, sembunyikan panel
	 *       filter dan tampilkan baris khusus {@code rowPosting}.</li>
	 *   <li>Menetapkan rentang tanggal default (6 bulan lalu s.d. hari ini, read-only).</li>
	 *   <li>Jika parameter URL {@code mulai} atau {@code sampai} tersedia (format
	 *       yyyyMMdd dari {@code Common.dateFormat8}), gunakan nilai tersebut dan
	 *       nonaktifkan datebox yang bersangkutan.</li>
	 *   <li>Menentukan flag {@code adminLain} dan {@code edit} dari privilege.</li>
	 *   <li>Mengisi combo pemilik aset dan lokasi dengan data aktif dari database.</li>
	 *   <li>Mengunci lokasi dari sesi/konfigurasi via {@code LokasiAction.kunciLokasi}.</li>
	 *   <li>Memuat data awal ke grid via {@link #loadDataDenganProgressPosting(Event)}.</li>
	 *   <li>Menginisialisasi paging dan filter lanjut.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code comp} — Komponen root halaman ZUL yang telah selesai dikomposisi.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Error parsing tanggal dari parameter URL ditangkap
	 * dan ditampilkan hanya kepada admin via {@link Common#tampilErrorJikaAdmin}.<br>
	 *
	 * <b>Pemeliharaan:</b> Jika ada parameter URL baru yang perlu didukung, tambahkan
	 * penanganannya setelah blok parameter {@code sudah_posting}. Pastikan format
	 * tanggal konsisten dengan {@code Common.dateFormat8}.
	 *
	 * @param comp komponen root ZUL yang telah selesai dikomposisi
	 * @throws Exception jika terjadi kesalahan pada proses inisialisasi komponen
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tbmuser = Common.getCurrentUser();
		if (execution.getParameter("sudah_posting") != null
				&& !execution.getParameter("sudah_posting").trim().isEmpty()) {
			sudah_posting = Boolean.parseBoolean(execution.getParameter("sudah_posting").trim());
		}

		if (sudah_posting != null && filter != null) {
			filter.setVisible(false);
			if (rowPosting != null) rowPosting.setVisible(true);
		}
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (tglSampai != null) tglSampai.setValue(ais.ui.util.WaktuUtil.getDate());
		if (tglMulai != null) tglMulai.setValue(calendar.getTime());
		if (tglMulai != null) tglMulai.setReadonly(true);
		if (tglSampai != null) tglSampai.setReadonly(true);

		if (execution.getParameter("mulai") != null) {
			try {
				if (tglMulai != null) tglMulai.setValue(Common.dateFormat8.get().parse(execution.getParameter("mulai")));
				if (tglMulai != null) tglMulai.setDisabled(true);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		if (execution.getParameter("sampai") != null) {
			try {
				if (tglSampai != null) tglSampai.setValue(Common.dateFormat8.get().parse(execution.getParameter("sampai")));
				if (tglSampai != null) tglSampai.setDisabled(true);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}
		adminLain = Common.getApakahAdmin() || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		if (sent != null) { sent.setVisible(edit); }

		Common.insertComboDanSemua(searchpemilikAsset, new String[] { "nama", "id" }, "keterangan", PemilikAsset.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(searchlokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (session.getAttribute("Lokasi") != null) {
			Common.selectComboItem(searchlokasi, session.getAttribute("Lokasi"));
			searchlokasi.setDisabled(true);
			session.removeAttribute("Lokasi");
		}
		LokasiAction.kunciLokasi(searchlokasi);

		loadDataDenganProgressPosting(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDenganProgressPosting(null);

			}
		});

        FilterLanjutHelper.setup(comp);
	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Batalkan Posting Semua" untuk
	 * membatalkan posting DP seluruh pemesanan yang tampil di grid dan sudah
	 * memiliki riwayat posting.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menampilkan dialog konfirmasi pembatalan massal kepada pengguna.</li>
	 *   <li>Jika dikonfirmasi, mengambil semua {@code PemesananPengadaanMasterAsset}
	 *       sesuai filter aktif yang memiliki {@code postingHistory} tidak null.</li>
	 *   <li>Untuk setiap pemesanan, menetapkan {@code postingHistory = null} dan
	 *       menyimpan perubahan.</li>
	 *   <li>Menghapus entri jurnal terkait dari {@code akunting.grup_transaksi}
	 *       menggunakan SQL native — hanya baris tanpa closing dan tanpa ref
	 *       (untuk memastikan tidak menghapus jurnal referensi).</li>
	 *   <li>Memuat ulang data grid setelah semua selesai.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — Event ZKoss dari klik tombol. Tidak digunakan langsung.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Tidak ada try-catch eksplisit; error database akan
	 * muncul sebagai error ZKoss. Jika pengguna menekan Batal, tidak ada perubahan.<br>
	 *
	 * <b>Pemeliharaan:</b> Klausa {@code and ref is null} pada query SQL native
	 * penting untuk tidak menghapus jurnal yang merupakan referensi dari transaksi
	 * lain. Jangan hapus kondisi ini.
	 *
	 * @param event event ZKoss dari klik tombol batalkan posting semua
	 * @throws Exception jika terjadi kesalahan database saat pembatalan
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi pemesanan ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<PemesananPengadaanMasterAsset> pemesananPengadaanMasterAssets = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset : pemesananPengadaanMasterAssets) {
								pemesananPengadaanMasterAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pemesananPengadaanMasterAsset);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where pemesanan_pengadaan_master_asset="
												+ pemesananPengadaanMasterAsset.getId() + " and ref is null" + " and closing is null")
										.executeUpdate();
							}
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDenganProgressPosting(null);
							}
						});
					}
				});

	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Posting Semua" untuk memposting
	 * pembayaran DP semua pemesanan pengadaan aset yang belum diposting secara massal
	 * dalam satu operasi batch yang berjalan di thread latar.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat dan menampilkan window modal berisi form input: tanggal posting,
	 *       nama pengguna yang memposting (read-only), dan kolom keterangan.</li>
	 *   <li>Tombol Batal menutup popup tanpa perubahan apapun.</li>
	 *   <li>Tombol Simpan memvalidasi tanggal, kemudian meminta konfirmasi kedua.</li>
	 *   <li>Jika dikonfirmasi, thread latar dimulai:
	 *       <ul>
	 *         <li>Membuat {@link PostingHistory} baru dengan jenis
	 *             {@code JENIS_PEMBAYARAN_DP_PEMESANAN} dan keterangan mencakup
	 *             rentang tanggal aktif filter.</li>
	 *         <li>Menyimpan posting history ke database.</li>
	 *         <li>Mengambil semua pemesanan belum diposting sesuai filter.</li>
	 *         <li>Untuk setiap pemesanan, mengambil akun debet dan kredit dari
	 *             konfigurasi jenis pemesanan, menyimpan transaksi jurnal via
	 *             {@link CommonAkunting#saveTransaksi}, dan memperbarui status
	 *             posting pemesanan.</li>
	 *         <li>Progress label diperbarui per transaksi dengan persentase.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Setelah thread selesai, ditampilkan notifikasi sukses dan popup ditutup.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — Event ZKoss dari klik tombol. Tidak digunakan langsung.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Error per-baris ditangani dalam blok try-catch terpisah
	 * agar batch tidak terhenti. Error ditampilkan hanya kepada admin.<br>
	 *
	 * <b>Pemeliharaan:</b> Jika format keterangan posting perlu diubah atau konstanta
	 * jenis posting berubah, sesuaikan di blok inisialisasi {@code PostingHistory}.
	 *
	 * @param event event ZKoss dari klik tombol posting semua
	 * @throws Exception jika terjadi kesalahan saat membangun komponen popup
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Transaksi Pembayaran DP");
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
		row.appendChild(new ais.ui.util.MyLabelConfig(
				Common.getCurrentUser().ambilPegawai() == null ? Common.getCurrentUser().getUserId()
						: Common.getCurrentUser().ambilPegawai().getNama()));

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
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting Pemesanan DP belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal dan pilih tanggal dari kalender; (2) Pastikan tanggal berada dalam periode akuntansi yang aktif; (3) ulangi proses posting ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi Pembayaran DP ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Tbmuser tbmuser = Common.getCurrentUser();

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											MyMessageboxConfig.show(
													"Posting transaksi Pembayaran DP berhasil dilakukan", "Informasi",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
													new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															onSearchDefault(arg0);
														}
													});

											addWindow.detach();
										}
									});

									new Thread(new Runnable() {

										@Override
										public void run() {
											try {

											Session session = HibernateUtil.currentNativeSession();

											PostingHistory postingHistory = new PostingHistory(
													PostingHistory.JENIS_PEMBAYARAN_DP_PEMESANAN);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<PemesananPengadaanMasterAsset> pemesananPengadaanMasterAssets = initCriteria(
													true).add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset : pemesananPengadaanMasterAssets) {

												SatuanKerja satuanKerja = (SatuanKerja) pemesananPengadaanMasterAsset
														.getSatuanKerja();

												if (pemesananPengadaanMasterAsset != null) {

													try {

														Akun akunDebet = pemesananPengadaanMasterAsset
																.getJenisPemesananPengadaanAsset() == null
																		? null
																		: pemesananPengadaanMasterAsset
																				.getJenisPemesananPengadaanAsset()
																				.getAkunDp();

														Akun akunKredit = pemesananPengadaanMasterAsset
																.getJenisPemesananPengadaanAsset() == null
																		? null
																		: pemesananPengadaanMasterAsset
																				.getJenisPemesananPengadaanAsset()
																				.getAkunUtangDp();

														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {

																ket = "Hutang DP terhadap pemesanan \""
																		+ (pemesananPengadaanMasterAsset.getKode() + "-"
																				+ pemesananPengadaanMasterAsset
																						.getKeterangan())
																		+ "\" sebanyak "
																		+ Common.numberFormat.get()
																				.format(pemesananPengadaanMasterAsset
																						.getDptotal());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(ket + " ("
																	+ Common.numberFormat.get().format(rowIndex * 100.0
																			/ pemesananPengadaanMasterAssets.size())
																	+ " %)");

															Double nilai = pemesananPengadaanMasterAsset.getDptotal();
															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																session.getTransaction().begin();
																if (nilai > 0.1) {
																	CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			pemesananPengadaanMasterAsset
																					.getTanggalPersetujuan(),
																			nilai, denda, pemesananPengadaanMasterAsset,
																			satuanKerja, session);
																} else {
																	CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			pemesananPengadaanMasterAsset
																					.getTanggalPersetujuan(),
																			nilai, denda, pemesananPengadaanMasterAsset,
																			satuanKerja, session);
																}
																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															pemesananPengadaanMasterAsset
																	.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(pemesananPengadaanMasterAsset);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingPemesananDpAction.java:725");
														// diabaikan agar batch tidak terhenti karena satu baris gagal
													}

												}
												rowIndex++;
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

	/**
	 * <h3>PemesananPengadaanMasterAssetRenderer — Renderer Baris Grid DP Pemesanan</h3>
	 *
	 * <b>Untuk apa:</b><br>
	 * Inner class yang merender setiap baris data {@link PemesananPengadaanMasterAsset}
	 * pada grid halaman posting DP pemesanan. Menampilkan informasi lengkap pemesanan
	 * beserta preview jurnal akuntansi dan tombol aksi Posting / Batalkan Posting.<br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Dipanggil oleh ZKoss untuk setiap objek model grid. Renderer menampilkan kode
	 * pemesanan (dengan link revisi), nama penyedia, jenis pemesanan, nilai DP,
	 * tanggal persetujuan, dan preview jurnal menggunakan
	 * {@link GrupTransaksi#tampilkanJurnal}. Jika konfigurasi akun tidak lengkap,
	 * ditampilkan pesan validasi. Status posting dan nomor bukti juga ditampilkan.
	 * Tombol aksi muncul sesuai hak akses dan status data.<br>
	 *
	 * <b>Threading:</b><br>
	 * Dijalankan di thread UI ZKoss. Query ke {@code GrupTransaksi} untuk nomor
	 * bukti dilakukan secara sinkron dalam render.<br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika kolom grid ZUL berubah, perbarui urutan {@code setParent(arg0)} di
	 * metode render ini agar sesuai dengan definisi kolom di ZUL.
	 */
	class PemesananPengadaanMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data pemesanan pengadaan aset ke dalam
		 * komponen-komponen ZKoss yang ditempatkan pada row grid.<br>
		 *
		 * <b>Cara kerja:</b>
		 * <ol>
		 *   <li>Mengatur vertical align baris ke "top".</li>
		 *   <li>Menampilkan kode pemesanan dengan widget revisi sebagai sel pertama.
		 *       Jika ada disposisi SOP, menampilkan link kecil menuju alur SOP.</li>
		 *   <li>Menampilkan nama penyedia, jenis pemesanan, nilai DP, dan tanggal
		 *       persetujuan sebagai sel berikutnya.</li>
		 *   <li>Mengambil akun debet dan kredit dari jenis pemesanan. Jika tersedia,
		 *       menampilkan preview jurnal via {@code GrupTransaksi.tampilkanJurnal};
		 *       jika tidak, menampilkan pesan validasi.</li>
		 *   <li>Mengambil nomor bukti dari {@code GrupTransaksi} dan menampilkan
		 *       status posting (belum/sudah beserta nomor bukti).</li>
		 *   <li>Menambahkan tombol Batalkan Posting dan tombol Posting sesuai kondisi.</li>
		 * </ol>
		 *
		 * <b>Parameter:</b>
		 * <ul>
		 *   <li>{@code arg0} — Komponen {@link Row} yang akan diisi dengan sel-sel data.</li>
		 *   <li>{@code arg1} — Objek {@link PemesananPengadaanMasterAsset} untuk baris ini.</li>
		 * </ul>
		 *
		 * <b>Return:</b> Tidak ada (void).<br>
		 *
		 * <b>Penanganan error:</b> Jika konfigurasi akun tidak lengkap, ditampilkan
		 * pesan informasi dan tombol aksi tidak muncul untuk baris tersebut.<br>
		 *
		 * <b>Pemeliharaan:</b> Metode {@code GrupTransaksi.tampilkanJurnal} membangun
		 * tampilan jurnal standar. Jika format jurnal perlu disesuaikan, gunakan
		 * implementasi tampilan khusus seperti yang dilakukan di renderer perjanjian.
		 *
		 * @param arg0 baris grid yang akan diisi komponen
		 * @param arg1 objek data pemesanan pengadaan untuk baris ini
		 * @throws Exception jika terjadi kesalahan saat membangun komponen atau query
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = (PemesananPengadaanMasterAsset) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PemesananPengadaanMasterAsset.class, pemesananPengadaanMasterAsset,
					pemesananPengadaanMasterAsset.getKode() == null ? "" : pemesananPengadaanMasterAsset.getKode()))
					.setParent(arg0);

			if (pemesananPengadaanMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pemesananPengadaanMasterAsset.getDisposisiSop().getKeterangan() + " ("
						+ pemesananPengadaanMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pemesananPengadaanMasterAsset.getDisposisiSop().getId(), null,
								null, true, arg0.getTarget());
					}
				});
			}

			new Label(pemesananPengadaanMasterAsset.getPenyedia() == null ? ""
					: pemesananPengadaanMasterAsset.getPenyedia().getNama()).setParent(arg0);

			new Label(pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset() == null ? ""
					: pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset().getNama()).setParent(arg0);

			Double nilai = pemesananPengadaanMasterAsset.getDp();

			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

			new Label(Common.dateFormat3.get().format(pemesananPengadaanMasterAsset.getTanggalPersetujuan())).setParent(arg0);

			Akun akunDebet = pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset() == null ? null
					: pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset().getAkunDp();

			Akun akunKredit = pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset() == null ? null
					: pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset().getAkunUtangDp();

			if (akunDebet != null && akunKredit != null) {

				GrupTransaksi.tampilkanJurnal(akunDebet, nilai, akunKredit, nilai).setParent(arg0);
			} else {
				new Label("Transaksi tidak valid."
						+ (akunDebet != null ? " Debet: " + akunDebet.getKode() + "-" + akunDebet.getNama() + "."
								: " Akun debet tidak ada.")
						+ (akunKredit != null ? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
								: " Akun kredit tidak ada."))
						.setParent(arg0);
			}

			String bukti = "";
			Session session = HibernateUtil.currentSession();
			bukti = (String) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.eq("pemesananPengadaanMasterAsset", pemesananPengadaanMasterAsset))
					.setMaxResults(1).setProjection(Projections.property("kode")).uniqueResult();

			new Label(pemesananPengadaanMasterAsset.getPostingHistory() == null
					? Common.getBahasaConfig("Belum diposting")
					: pemesananPengadaanMasterAsset.getPostingHistory().toString() + ", no. bukti : " + bukti)
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && pemesananPengadaanMasterAsset.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pemesananPengadaanMasterAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pemesananPengadaanMasterAsset);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where pemesanan_pengadaan_master_asset="
												+ pemesananPengadaanMasterAsset.getId() + " and ref is null" + " and closing is null")
										.executeUpdate();

								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										loadDataDenganProgressPosting(null);
									}
								});
							}
						});

					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("", "/img/svg/check2-circle.svg");
				button.setTooltiptext("Posting Data");
				button.setVisible(edit && pemesananPengadaanMasterAsset.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PEMBAYARAN_DP_PEMESANAN);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunDebet = pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset() == null
										? null
										: pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset().getAkunDp();

								Akun akunKredit = pemesananPengadaanMasterAsset
										.getJenisPemesananPengadaanAsset() == null ? null
												: pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset()
														.getAkunUtangDp();

								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {

										ket = "Hutang DP terhadap pemesanan \""
												+ (pemesananPengadaanMasterAsset.getKode() + "-"
														+ pemesananPengadaanMasterAsset.getKeterangan())
												+ "\" sebanyak " + Common.numberFormat.get()
														.format(pemesananPengadaanMasterAsset.getDptotal());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;
									Double nilai = pemesananPengadaanMasterAsset.getDptotal();

									SatuanKerja satuanKerja = (SatuanKerja) (pemesananPengadaanMasterAsset != null
											&& pemesananPengadaanMasterAsset.getSatuanKerja() != null
													? pemesananPengadaanMasterAsset.getSatuanKerja()
													: tbmuser.ambilSatuanKerja());

									if (tbmuser.ambilSatuanKerja() != null) {
										satuanKerja = tbmuser.ambilSatuanKerja();
									}

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket,
												pemesananPengadaanMasterAsset.getTanggalPersetujuan(), nilai, denda,
												pemesananPengadaanMasterAsset, satuanKerja, session);
									} else {
										CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket,
												pemesananPengadaanMasterAsset.getTanggalPersetujuan(), nilai, denda,
												pemesananPengadaanMasterAsset, satuanKerja, session);
									}

									pemesananPengadaanMasterAsset.setPostingHistory(postingHistory);
									session.update(pemesananPengadaanMasterAsset);
								}

								loadDataDenganProgressPosting(null);
							}
						});

					}

				});
				button.setParent(toolbar);
			}

		}
	}

	/**
	 * <b>Tujuan:</b> Membangun dan mengembalikan objek {@link Criteria} Hibernate
	 * yang merepresentasikan query pencarian data pemesanan pengadaan aset sesuai
	 * semua filter aktif. Mendukung dua mode: mode parameter URL (ketika
	 * {@code sudah_posting != null}) dan mode filter manual dari checkbox UI.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuka sesi Hibernate saat ini dan membuat criteria dasar untuk
	 *       {@link PemesananPengadaanMasterAsset}.</li>
	 *   <li>Jika {@code sudah_posting != null} (mode URL parameter):
	 *       <ul>
	 *         <li>Membatasi pada pemesanan yang disetujui, aktif, DP tidak nol,
	 *             dan dalam rentang tanggal persetujuan yang ditentukan.</li>
	 *         <li>Jika {@code sudah_posting == true}: filter hanya yang memiliki
	 *             posting history dengan status posting=true.</li>
	 *         <li>Jika {@code sudah_posting == false}: filter hanya yang belum
	 *             memiliki posting atau status posting=false.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Jika {@code sudah_posting == null} (mode filter manual):
	 *       <ul>
	 *         <li>Jika {@code searchtampil} dicentang: tampilkan yang belum diposting
	 *             atau status=false, dan uncheck {@code searchtelahtampil}.</li>
	 *         <li>Jika {@code searchtelahtampil} dicentang: logika serupa.</li>
	 *         <li>Menambahkan semua filter standar: disetujuiOleh, aktif, DP, tanggal.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Di luar blok kondisi: menambahkan filter pemilik aset, lokasi, ruang,
	 *       dan teks kode/keterangan.</li>
	 *   <li>Jika {@code order == true}, menambahkan pengurutan descending by ID.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code order} — Jika {@code true}, criteria akan diurutkan descending
	 *       berdasarkan ID. Gunakan {@code false} untuk query count pagination.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Objek {@link Criteria} Hibernate yang siap untuk dieksekusi.<br>
	 *
	 * <b>Penanganan error:</b> Mode URL parameter memiliki blok try-catch yang
	 * mencatat error ke admin jika terjadi kesalahan membangun criteria.<br>
	 *
	 * <b>Pemeliharaan:</b> Jika ada filter baru di ZUL, tambahkan Restrictions yang
	 * sesuai di bagian "di luar blok kondisi". Pastikan alias yang dibuat untuk
	 * join tidak konflik (misalnya alias "postingHistory" tidak boleh dibuat dua kali).
	 *
	 * @param order jika {@code true} maka hasil diurutkan descending berdasarkan ID
	 * @return objek {@link Criteria} dengan semua filter aktif yang sudah diterapkan
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PemesananPengadaanMasterAsset.class);

		if (sudah_posting != null) {

			try {

				criteria.add(Restrictions.ne("dp", 0.0)).add(Restrictions.isNotNull("dp"))

						.add(Restrictions.isNotNull("disetujuiOleh"))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
								+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and date('"
								+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")));

				if (sudah_posting) {
					criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN)
							.add(Restrictions.eq("postingHistory.posting", true));
				} else {
					criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN)
							.add(Restrictions.or(Restrictions.isNull("postingHistory.id"),
									Restrictions.eq("postingHistory.posting", false)));
				}

			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

		} else {

			if (searchtampil.isChecked()) {
				criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN).add(Restrictions.or(
						Restrictions.isNull("postingHistory.id"), Restrictions.eq("postingHistory.posting", false)));

				searchtelahtampil.setChecked(false);
			}

			else if (searchtelahtampil.isChecked()) {
				criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN).add(Restrictions.or(
						Restrictions.isNull("postingHistory.id"), Restrictions.eq("postingHistory.posting", false)));

				searchtampil.setChecked(false);
			}

			criteria

					.add(Restrictions.isNotNull("disetujuiOleh"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.ne("dp", 0.0)).add(Restrictions.isNotNull("dp"))
					.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
							+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and  date('"
							+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")));
		}

		criteria.add(
				searchpemilikAsset.getSelectedItem() == null || searchpemilikAsset.getSelectedItem().getValue() == null
						|| searchpemilikAsset.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("pemilikAsset", searchpemilikAsset.getSelectedItem().getValue()))
				.add(searchlokasi.getSelectedItem() == null || searchlokasi.getSelectedItem().getValue() == null
						|| searchlokasi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))
				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE),
								Restrictions.ilike("keterangan", searchkode.getValue(), MatchMode.ANYWHERE)));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Mengeksekusi query database dan memuat hasil ke grid tanpa
	 * progress indicator. Metode internal yang dipanggil dari dalam mekanisme
	 * progress loading di {@link #loadDataDenganProgressPosting(Event)}.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menginisialisasi paging berdasarkan total count dari criteria non-ordered.</li>
	 *   <li>Mengambil satu halaman data menggunakan criteria ordered dengan batas
	 *       {@link Common#ROWS_COUNT_ON_PAGE} dan offset sesuai halaman aktif.</li>
	 *   <li>Membungkus hasil dalam {@link SimpleListModel} dan menetapkan renderer
	 *       serta model ke grid utama.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — Event ZKoss pemicu. Dapat {@code null}.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan eksplisit.<br>
	 *
	 * <b>Pemeliharaan:</b> Selalu gunakan melalui {@link #loadDataDenganProgressPosting}
	 * untuk menjaga konsistensi mekanisme anti-tumpang tindih dan progress indicator.
	 *
	 * @param event event ZKoss pemicu, dapat null
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PemesananPengadaanMasterAsset> pemesananPengadaanMasterAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pemesananPengadaanMasterAsset);
		grid.setRowRenderer(new PemesananPengadaanMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Delegator publik untuk event pencarian default, dapat dipanggil
	 * dari ZUL saat pengguna mengklik tombol Cari atau filter berubah. Mendelegasikan
	 * ke {@link #loadDataDenganProgressPosting(Event)}.<br>
	 *
	 * <b>Cara kerja:</b> Meneruskan event ke {@link #loadDataDenganProgressPosting(Event)}
	 * yang mengelola flag loading dan progress indicator secara otomatis.<br>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — Event ZKoss pemicu pencarian.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Tidak ada; exception muncul dari layer bawah.<br>
	 *
	 * <b>Pemeliharaan:</b> Nama method ini dikaitkan ke ZUL sebagai event handler.
	 * Jangan ganti nama tanpa memperbarui referensi di ZUL yang bersangkutan.
	 *
	 * @param event event ZKoss dari interaksi pengguna atau pemanggilan programatik
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Flag penjaga untuk mencegah dua proses pemuatan data berjalan bersamaan.
	 * Bernilai {@code true} saat ada proses pemuatan yang sedang aktif, dan
	 * kembali {@code false} setelah selesai (di blok finally).
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag yang menandakan ada permintaan reload yang masuk saat proses loading
	 * masih aktif. Jika {@code true}, setelah proses selesai akan segera
	 * memulai satu siklus reload lagi untuk memastikan data terkini ditampilkan.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * <b>Tujuan:</b> Memuat data grid pemesanan DP dengan mekanisme progress indicator
	 * yang informatif dan perlindungan dari tumpang tindih request reload. Ini adalah
	 * entry point utama untuk semua operasi tampil-ulang data di halaman ini.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code postingJurnalLoadingAktif} true, menandai reload tertunda,
	 *       menampilkan pesan antri, dan return tanpa memulai proses baru.</li>
	 *   <li>Jika aman, set flag aktif, reset flag tertunda, tampilkan progress
	 *       awal (7%) via {@code PostingJurnalLoadingUtil.show}.</li>
	 *   <li>Menjadwalkan timer ZKoss untuk menjalankan pemuatan aktual
	 *       ({@link #onSearchDefaultTanpaProgress}) di event berikutnya,
	 *       memperbarui progress ke 48% dan 92%.</li>
	 *   <li>Blok finally memastikan flag aktif direset meski terjadi exception.</li>
	 *   <li>Jika ada reload tertunda, menjadwalkan satu siklus reload lagi.</li>
	 *   <li>Jika tidak ada reload tertunda, menandai progress selesai (100%).</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — Event ZKoss pemicu. Diteruskan ke
	 *       {@link #onSearchDefaultTanpaProgress(Event)}. Dapat {@code null}.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Blok finally menjamin flag aktif direset agar halaman
	 * tidak terkunci dalam kondisi loading permanen meski terjadi exception.<br>
	 *
	 * <b>Pemeliharaan:</b> Jangan memanggil method ini dari thread latar. Field-field
	 * flag tidak disinkronkan dan hanya aman dalam model single-thread ZKoss per sesi.
	 * Pesan progress dapat diperbarui sesuai kebutuhan UX.
	 *
	 * @param event event ZKoss pemicu pemuatan data, dapat null
	 */
	private void loadDataDenganProgressPosting(final org.zkoss.zk.ui.event.Event event) {
		if (postingJurnalLoadingAktif) {
			postingJurnalReloadTertunda = true;
			ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Ulang Data Posting Jurnal",
					"Permintaan reload baru diterima. Data akan dimuat ulang setelah proses yang berjalan selesai.", 12);
			return;
		}
		postingJurnalLoadingAktif = true;
		postingJurnalReloadTertunda = false;
		ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Data Posting Jurnal",
				"Menyiapkan filter dan tabel data jurnal.", 7);
		Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event timerEvent) throws Exception {
				try {
					ais.ui.util.PostingJurnalLoadingUtil.update("Mengambil Data Posting Jurnal",
							"Mencari data sesuai tanggal, status posting, dan filter halaman.", 48);
					onSearchDefaultTanpaProgress(event);
					ais.ui.util.PostingJurnalLoadingUtil.update("Merapikan Tampilan",
							"Menyusun tabel, paging, status posting, dan preview jurnal.", 92);
				} finally {
					boolean reloadLagi = postingJurnalReloadTertunda;
					postingJurnalReloadTertunda = false;
					postingJurnalLoadingAktif = false;
					if (reloadLagi) {
						ais.ui.util.PostingJurnalLoadingUtil.update("Memuat Ulang Data Posting Jurnal",
								"Filter atau halaman berubah saat data sedang diproses. Data akan dimuat ulang sekarang.", 96);
						Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
							@Override
							public void onEvent(org.zkoss.zk.ui.event.Event ulangEvent) throws Exception {
								loadDataDenganProgressPosting(event);
							}
						});
					} else {
						ais.ui.util.PostingJurnalLoadingUtil.complete("Data Posting Jurnal Siap",
								"Tabel sudah selesai dimuat dan siap digunakan.", 100);
					}
				}
			}
		});
	}

}
