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
import ais.database.model.asset.PemilikAsset;
import ais.database.model.asset.PerjanjianKerjasamaMasterAsset;
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
 * <h3>PostingPerjanjianKerjasamaAction — Halaman Posting Jurnal Perjanjian Kerjasama Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah ZKoss Composer yang mengelola halaman posting jurnal akuntansi
 * untuk transaksi perjanjian kerjasama aset. Modul ini memungkinkan petugas
 * keuangan/aset memposting atau membatalkan posting entri jurnal debet-kredit
 * yang terkait dengan perjanjian kerjasama antara institusi dan penyedia/vendor
 * tertentu. Setiap perjanjian yang telah disetujui dan memiliki nilai DP akan
 * menghasilkan satu pasang entri jurnal (akun debet dan akun kredit) berdasarkan
 * konfigurasi jenis perjanjian kerjasama aset yang bersangkutan. Halaman ini
 * juga mendukung posting massal (semua data sekaligus) maupun posting per-baris
 * secara individual, serta pembatalan posting baik per-baris maupun massal.
 *
 * <b>Cara kerja:</b><br>
 * Saat halaman diinisialisasi melalui {@link #doAfterCompose(Component)}, kelas
 * ini memuat data perjanjian kerjasama yang telah disetujui dari database
 * menggunakan Hibernate Criteria yang dibangun oleh {@link #initCriteria(boolean)}.
 * Filter tersedia berdasarkan pemilik aset, lokasi, ruang, tanggal pembuatan,
 * kode/nama, serta status posting. Proses muat data menggunakan mekanisme
 * progress loading non-blokir via {@link #loadDataDenganProgressPosting(Event)}
 * dengan flag {@code postingJurnalLoadingAktif} untuk mencegah tumpang tindih
 * permintaan reload. Jika permintaan reload baru datang saat proses sebelumnya
 * masih berjalan, reload akan ditangguhkan dan dijalankan kembali setelah
 * proses pertama selesai.
 *
 * Setiap baris data dirender oleh {@link PerjanjianKerjasamaMasterAssetRenderer}
 * yang menampilkan informasi kode, penyedia, jenis perjanjian, nilai DP,
 * tanggal persetujuan, preview jurnal dalam format tabel HTML, serta status
 * posting. Tombol Posting dan Batalkan Posting muncul sesuai hak akses pengguna
 * dan status data.
 *
 * Proses posting massal berjalan di thread terpisah (non-UI thread) untuk
 * menghindari pemblokiran antarmuka, dilengkapi progress label yang diperbarui
 * per-transaksi. Posting menggunakan {@link CommonAkunting#saveTransaksi} untuk
 * menyimpan entri jurnal ke tabel {@code akunting.grup_transaksi}.
 *
 * <b>Threading:</b><br>
 * Proses posting massal di {@link #onPostingSemua(Event)} menggunakan
 * {@code new Thread(...).start()} dengan native session Hibernate yang dibuka
 * khusus untuk thread tersebut. Thread UI utama ZKoss tidak boleh memblokir;
 * semua operasi panjang didelegasikan ke thread latar. Flag
 * {@code postingJurnalLoadingAktif} dan {@code postingJurnalReloadTertunda}
 * tidak sinkron dengan {@code synchronized}, sehingga aman digunakan hanya
 * dalam konteks event-driven single-thread ZKoss (satu pengguna per sesi).
 *
 * <b>Pemeliharaan:</b><br>
 * Jika tabel {@code akunting.grup_transaksi} berubah skema (misalnya kolom
 * foreign key perjanjian kerjasama), perbarui query SQL native di
 * {@link #onBatalkanPostingSemua(Event)} dan di renderer per-baris. Jika
 * logika penentuan akun debet/kredit berubah, cukup ubah konfigurasi pada
 * entitas {@code JenisPerjanjianKerjasamaAsset}. Kelas ini bergantung pada
 * {@link CommonPrivilages} untuk cek hak akses; jika hierarki privilege berubah,
 * perbarui konstanta yang digunakan di sini.
 *
 * @author AIS Development Team
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 * @see PerjanjianKerjasamaMasterAsset
 * @see PostingHistory
 * @see CommonAkunting
 */
public class PostingPerjanjianKerjasamaAction extends GenericAutowireComposer {

	/**
	 * Nomor versi serial untuk serialisasi kelas ini sesuai kontrak {@code Serializable}.
	 * Nilai ini digunakan oleh mekanisme serialisasi Java untuk memverifikasi kompatibilitas
	 * versi saat deserialisasi. Tidak perlu diubah kecuali struktur kelas berubah secara
	 * tidak kompatibel dengan versi yang tersimpan.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/**
	 * Komponen grid utama halaman yang menampilkan daftar perjanjian kerjasama
	 * beserta status posting jurnal masing-masing. Grid diisi ulang setiap kali
	 * filter berubah atau setelah operasi posting/pembatalan selesai.
	 */
	private MyGrid grid;

	/**
	 * Komponen paging untuk navigasi halaman data pada grid perjanjian kerjasama.
	 * Diinisialisasi oleh {@link Common#initPaging} dengan listener yang memanggil
	 * {@link #loadDataDenganProgressPosting(Event)} setiap kali halaman berganti.
	 */
	private Paging paging;

	/**
	 * Kotak pencarian teks untuk memfilter perjanjian berdasarkan kode atau nama
	 * perjanjian kerjasama. Pencarian bersifat case-insensitive dan menggunakan
	 * mode pencocokan di mana saja (ANYWHERE).
	 */
	private Textbox searchkode;

	/**
	 * Combobox filter pemilik aset. Memuat semua entitas {@code PemilikAsset}
	 * yang aktif. Jika item dipilih, criteria akan ditambahkan filter
	 * {@code eq("pemilikAsset", ...)}.
	 */
	private Combobox searchpemilikAsset;

	/**
	 * Combobox filter lokasi aset. Memuat semua entitas {@code Lokasi} yang aktif.
	 * Jika sesi menyimpan atribut "Lokasi", combo akan terpilih otomatis dan
	 * dinonaktifkan (kunci lokasi dari halaman sebelumnya).
	 */
	private Combobox searchlokasi;

	/**
	 * Komponen banbox (autocomplete) untuk filter ruang/lokasi spesifik.
	 * Nilai ruang yang dipilih disimpan sebagai atribut komponen dengan key "ruang".
	 * Jika {@code null}, filter ruang tidak diterapkan.
	 */
	private AmbilDataRuangBanbox searchruang;

	/**
	 * Checkbox filter "Tampilkan yang belum diposting". Jika dicentang, hanya
	 * perjanjian dengan {@code postingHistory == null} yang ditampilkan. Tidak dapat
	 * dicentang bersamaan dengan {@code searchtelahtampil}.
	 */
	private MyCheckboxConfig searchtampil;

	/**
	 * Checkbox filter "Tampilkan yang telah diposting". Jika dicentang, hanya
	 * perjanjian dengan {@code postingHistory != null} yang ditampilkan. Tidak dapat
	 * dicentang bersamaan dengan {@code searchtampil}.
	 */
	private MyCheckboxConfig searchtelahtampil;

	/**
	 * Flag yang menandakan apakah pengguna saat ini memiliki hak akses UPDATE
	 * (hak ubah/edit data). Nilai diinisialisasi di {@link #doAfterCompose(Component)}
	 * dan digunakan untuk mengontrol visibilitas tombol Posting dan Batalkan Posting.
	 */
	private boolean edit = false;

	/**
	 * Tombol toolbar untuk memposting semua transaksi sekaligus (posting massal).
	 * Visibilitasnya dikontrol oleh flag {@code edit}; hanya muncul jika pengguna
	 * memiliki hak UPDATE.
	 */
	private MyToolbarbuttonConfig sent;

	/**
	 * Flag yang menandakan apakah pengguna adalah admin atau memiliki hak APPROVE.
	 * Digunakan untuk mengontrol visibilitas tombol Batalkan Posting per-baris,
	 * yang hanya boleh digunakan oleh admin atau approver.
	 */
	public boolean adminLain;

	/**
	 * Komponen datebox batas tanggal mulai filter periode perjanjian kerjasama.
	 * Diinisialisasi dengan tanggal 6 bulan sebelum tanggal sekarang. Bersifat
	 * read-only agar pengguna tidak dapat mengetik manual (harus lewat date picker).
	 */
	private MyDatebox tglMulai;

	/**
	 * Komponen datebox batas tanggal akhir filter periode perjanjian kerjasama.
	 * Diinisialisasi dengan tanggal hari ini. Bersifat read-only.
	 */
	private MyDatebox tglSampai;

	/**
	 * Referensi ke pengguna yang sedang login, diambil dari {@link Common#getCurrentUser()}
	 * saat inisialisasi halaman. Digunakan untuk mencatat siapa yang melakukan
	 * operasi posting manual.
	 */
	private Tbmuser tbmuser;

	/**
	 * <b>Tujuan:</b> Dipanggil oleh ZKoss sebelum proses komposisi halaman dimulai,
	 * digunakan untuk memeriksa keamanan akses halaman sebelum komponen ZUL dirender.<br>
	 *
	 * <b>Cara kerja:</b> Memanggil {@link Common#doCheckSecurity()} yang akan
	 * mengalihkan pengguna ke halaman login jika sesi tidak valid atau tidak
	 * memiliki hak akses ke halaman ini. Setelah pemeriksaan, delegasi ke
	 * implementasi superclass untuk melanjutkan proses komposisi normal.<br>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code page} — Halaman ZKoss yang sedang dikomposisi.</li>
	 *   <li>{@code parent} — Komponen induk tempat halaman akan dipasang.</li>
	 *   <li>{@code compInfo} — Metadata informasi komponen ZKoss.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Mengembalikan {@code ComponentInfo} dari superclass untuk
	 * melanjutkan proses komposisi ZKoss secara normal.<br>
	 *
	 * <b>Penanganan error:</b> Jika keamanan tidak terpenuhi, {@code doCheckSecurity}
	 * akan melakukan redirect dan proses berhenti di sana. Tidak ada exception yang
	 * dilempar dari method ini secara langsung.<br>
	 *
	 * <b>Pemeliharaan:</b> Jangan menghapus pemanggilan {@code doCheckSecurity()} karena
	 * ini adalah lini pertahanan pertama akses tidak sah ke halaman ini.
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
	 * <b>Tujuan:</b> Metode inisialisasi utama yang dipanggil oleh ZKoss setelah
	 * semua komponen ZUL berhasil dibuat dan autowired ke field-field kelas ini.
	 * Bertanggung jawab penuh atas persiapan awal halaman posting perjanjian
	 * kerjasama, mulai dari validasi sesi, inisialisasi filter, hingga pemuatan
	 * data awal ke grid.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan
	 *       autowiring komponen ZUL ke field Java.</li>
	 *   <li>Menginisialisasi bahasa antarmuka via {@link Common#initLaguage()}.</li>
	 *   <li>Memeriksa validitas sesi (atribut "usersTemp") dan hak akses READ.
	 *       Jika tidak valid, sesi dibersihkan dan pengguna dialihkan ke halaman
	 *       login menggunakan {@link Common#goLogoff()}.</li>
	 *   <li>Mengambil data pengguna yang sedang login ke field {@code tbmuser}.</li>
	 *   <li>Menetapkan rentang tanggal default: {@code tglMulai} = 6 bulan lalu,
	 *       {@code tglSampai} = hari ini, keduanya read-only.</li>
	 *   <li>Menentukan flag {@code adminLain} berdasarkan status admin atau
	 *       privilege APPROVE.</li>
	 *   <li>Menentukan flag {@code edit} berdasarkan privilege UPDATE.</li>
	 *   <li>Mengisi combo pemilik aset dan lokasi dengan data dari database,
	 *       menyaring yang aktif saja.</li>
	 *   <li>Jika sesi menyimpan atribut "Lokasi" (dari navigasi halaman lain),
	 *       combo lokasi dipilih otomatis dan dinonaktifkan.</li>
	 *   <li>Mengunci lokasi sesuai konfigurasi via {@code LokasiAction.kunciLokasi}.</li>
	 *   <li>Memuat data awal ke grid via {@link #loadDataDenganProgressPosting(Event)}.</li>
	 *   <li>Menginisialisasi paging dengan listener yang memanggil reload saat
	 *       halaman berganti.</li>
	 *   <li>Menyiapkan filter lanjut via {@link FilterLanjutHelper#setup(Component)}.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code comp} — Komponen root halaman ZUL yang telah selesai dikomposisi.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Jika sesi tidak valid, pengguna diarahkan ke login
	 * dan method berhenti. Exception lain akan muncul sebagai error ZKoss.<br>
	 *
	 * <b>Pemeliharaan:</b> Jika ada filter baru yang ditambahkan ke ZUL (misalnya
	 * filter satuan kerja), tambahkan inisialisasi combo-nya di sini. Pastikan
	 * urutan inisialisasi dipertahankan agar tidak terjadi NullPointerException
	 * saat komponen belum siap.
	 *
	 * @param comp komponen root halaman ZUL yang telah selesai dikomposisi
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

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (tglSampai != null) tglSampai.setValue(ais.ui.util.WaktuUtil.getDate());
		if (tglMulai != null) tglMulai.setValue(calendar.getTime());
		if (tglMulai != null) tglMulai.setReadonly(true);
		if (tglSampai != null) tglSampai.setReadonly(true);

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
	 * <b>Tujuan:</b> Menangani event klik tombol "Batalkan Posting Semua" pada toolbar
	 * halaman. Membatalkan posting jurnal untuk seluruh perjanjian kerjasama yang
	 * saat ini tampil di grid dan telah memiliki riwayat posting.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menampilkan dialog konfirmasi kepada pengguna untuk memastikan niat
	 *       pembatalan massal.</li>
	 *   <li>Jika pengguna mengonfirmasi (OK), mengambil semua
	 *       {@code PerjanjianKerjasamaMasterAsset} yang sesuai filter aktif dan
	 *       memiliki {@code postingHistory} tidak null.</li>
	 *   <li>Untuk setiap perjanjian, menetapkan {@code postingHistory = null} dan
	 *       menyimpan perubahan ke database.</li>
	 *   <li>Menghapus entri jurnal terkait dari tabel {@code akunting.grup_transaksi}
	 *       menggunakan SQL native langsung — hanya baris yang belum di-closing
	 *       (closing is null).</li>
	 *   <li>Setelah semua dibatalkan, memuat ulang data grid.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — Event ZKoss yang dikirim saat tombol diklik. Tidak
	 *       digunakan secara langsung dalam method ini.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Jika pengguna menekan Batal pada dialog konfirmasi,
	 * tidak ada perubahan yang terjadi. Tidak ada try-catch eksplisit; jika
	 * database error terjadi, exception akan muncul sebagai error ZKoss.<br>
	 *
	 * <b>Pemeliharaan:</b> Jika nama kolom foreign key di tabel
	 * {@code akunting.grup_transaksi} berubah (misalnya dari
	 * {@code perjanjian_kerjasama_master_asset} menjadi nama lain), perbarui
	 * query SQL native di dalam method ini.
	 *
	 * @param event event ZKoss dari klik tombol batalkan posting semua
	 * @throws Exception jika terjadi kesalahan database saat pembatalan
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi perjanjian kerjasama ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<PerjanjianKerjasamaMasterAsset> perjanjianKerjasamaMasterAssets = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset : perjanjianKerjasamaMasterAssets) {
								perjanjianKerjasamaMasterAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(perjanjianKerjasamaMasterAsset);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where perjanjian_kerjasama_master_asset="
												+ perjanjianKerjasamaMasterAsset.getId() + " and closing is null")
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
	 * <b>Tujuan:</b> Menangani event klik tombol "Posting Semua" pada toolbar halaman.
	 * Membuka popup dialog yang memungkinkan petugas memasukkan tanggal posting
	 * dan keterangan, kemudian memposting semua perjanjian kerjasama yang belum
	 * diposting secara massal dalam satu operasi batch.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat dan menampilkan {@link MyWindow} modal berisi form input:
	 *       tanggal/waktu posting, nama pengguna yang memposting, dan kolom keterangan.</li>
	 *   <li>Tombol Batal menutup popup tanpa melakukan apapun.</li>
	 *   <li>Tombol Simpan memvalidasi bahwa tanggal telah diisi, kemudian menampilkan
	 *       dialog konfirmasi kedua sebelum benar-benar memulai proses.</li>
	 *   <li>Jika dikonfirmasi, proses posting berjalan di thread latar:
	 *       <ul>
	 *         <li>Membuat satu entitas {@link PostingHistory} baru dengan jenis
	 *             {@code JENIS_PERJANJIAN_KERJASAMA} dan keterangan yang mencakup
	 *             rentang tanggal filter aktif.</li>
	 *         <li>Menyimpan {@code PostingHistory} ke database.</li>
	 *         <li>Mengambil semua perjanjian yang belum diposting sesuai filter.</li>
	 *         <li>Untuk setiap perjanjian, menentukan akun debet dan kredit dari
	 *             konfigurasi jenis perjanjian, membuat entri jurnal menggunakan
	 *             {@link CommonAkunting#saveTransaksi}, dan memperbarui status
	 *             posting perjanjian.</li>
	 *         <li>Nilai DP positif menghasilkan debet di akun DP dan kredit di akun
	 *             utang DP; nilai negatif menghasilkan posisi sebaliknya.</li>
	 *         <li>Progress ditampilkan melalui {@link Common#displayLoadBar} dengan
	 *             persentase berdasarkan index baris.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Setelah thread selesai, tampilkan notifikasi sukses dan tutup popup.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — Event ZKoss dari klik tombol. Tidak digunakan langsung.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Setiap perjanjian individual diproses dalam blok
	 * try-catch terpisah; error pada satu baris tidak menghentikan batch keseluruhan.
	 * Error tampilan hanya ditampilkan kepada admin via {@link Common#tampilErrorJikaAdmin}.<br>
	 *
	 * <b>Pemeliharaan:</b> Jika konstanta jenis posting berubah di kelas
	 * {@link PostingHistory}, perbarui penggunaan {@code PostingHistory.JENIS_PERJANJIAN_KERJASAMA}
	 * di sini. Jika struktur keterangan posting perlu distandarkan, sesuaikan
	 * format string keterangan.
	 *
	 * @param event event ZKoss dari klik tombol posting semua
	 * @throws Exception jika terjadi kesalahan saat membangun komponen popup
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Transaksi Perjanjian Kerjasama");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
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
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting Perjanjian Kerjasama belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal dan pilih tanggal dari kalender; (2) Pastikan tanggal berada dalam periode akuntansi yang aktif; (3) ulangi proses posting ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi perjanjian kerjasama ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
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
													"Posting transaksi Pembayaran perjanjian kerjasama dilakukan",
													"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
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
													PostingHistory.JENIS_PERJANJIAN_KERJASAMA);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<PerjanjianKerjasamaMasterAsset> perjanjianKerjasamaMasterAssets = initCriteria(
													true).add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset : perjanjianKerjasamaMasterAssets) {

												SatuanKerja satuanKerja = (SatuanKerja) perjanjianKerjasamaMasterAsset
														.getSatuanKerja();

												if (perjanjianKerjasamaMasterAsset != null) {

													try {

														Akun akunDebet = perjanjianKerjasamaMasterAsset
																.getJenisPerjanjianKerjasamaAsset() == null
																		? null
																		: perjanjianKerjasamaMasterAsset
																				.getJenisPerjanjianKerjasamaAsset()
																				.getAkunDp();

														Akun akunKredit = perjanjianKerjasamaMasterAsset
																.getJenisPerjanjianKerjasamaAsset() == null
																		? null
																		: perjanjianKerjasamaMasterAsset
																				.getJenisPerjanjianKerjasamaAsset()
																				.getAkunUtangDp();

														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {

																ket = "Perjanjian kerjasama terhadap kode \""
																		+ (perjanjianKerjasamaMasterAsset.getKode()
																				+ "-"
																				+ perjanjianKerjasamaMasterAsset
																						.getKeterangan())
																		+ "\" pada penyedia "
																		+ perjanjianKerjasamaMasterAsset.getPenyedia()
																				.getNama()
																		+ " sebanyak " + Common.numberFormat.get().format(
																				perjanjianKerjasamaMasterAsset.getDp());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(ket + " ("
																	+ Common.numberFormat.get().format(rowIndex * 100.0
																			/ perjanjianKerjasamaMasterAssets.size())
																	+ " %)");

															Double nilai = perjanjianKerjasamaMasterAsset.getDp();
															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																session.getTransaction().begin();
																if (nilai > 0.1) {
																	CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			perjanjianKerjasamaMasterAsset
																					.getTanggalPersetujuan(),
																			nilai, denda,
																			perjanjianKerjasamaMasterAsset, satuanKerja,
																			session);
																} else {
																	CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			perjanjianKerjasamaMasterAsset
																					.getTanggalPersetujuan(),
																			nilai, denda,
																			perjanjianKerjasamaMasterAsset, satuanKerja,
																			session);
																}
																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															perjanjianKerjasamaMasterAsset
																	.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(perjanjianKerjasamaMasterAsset);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingPerjanjianKerjasamaAction.java:720");
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
	 * <h3>PerjanjianKerjasamaMasterAssetRenderer — Renderer Baris Grid Perjanjian Kerjasama</h3>
	 *
	 * <b>Untuk apa:</b><br>
	 * Inner class yang bertanggung jawab merender setiap baris data
	 * {@link PerjanjianKerjasamaMasterAsset} pada grid halaman posting jurnal.
	 * Setiap baris menampilkan informasi lengkap perjanjian beserta preview
	 * jurnal akuntansi dan tombol aksi Posting / Batalkan Posting.<br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Dipanggil oleh ZKoss untuk setiap objek dalam model grid. Renderer
	 * mengambil data perjanjian, menampilkan kode (dengan link revisi), nama
	 * penyedia, jenis perjanjian, nilai DP, tanggal persetujuan, dan preview
	 * tabel jurnal HTML. Jika akun debet dan kredit tersedia, ditampilkan tabel
	 * jurnal standar; jika tidak, ditampilkan pesan error validasi. Tombol
	 * Posting dan Batalkan Posting muncul berdasarkan hak akses dan status
	 * posting perjanjian.<br>
	 *
	 * <b>Threading:</b><br>
	 * Dijalankan di thread UI ZKoss. Query database ke {@code GrupTransaksi}
	 * untuk mengambil nomor bukti dilakukan secara sinkron dalam metode render.<br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika kolom-kolom pada ZUL grid berubah (ditambah atau dihapus), pastikan
	 * urutan pemanggilan {@code setParent(arg0)} di sini diperbarui agar sesuai
	 * dengan jumlah kolom di ZUL.
	 */
	class PerjanjianKerjasamaMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data perjanjian kerjasama ke dalam
		 * komponen-komponen ZKoss yang ditempatkan pada row grid.<br>
		 *
		 * <b>Cara kerja:</b>
		 * <ol>
		 *   <li>Mengatur vertical align baris ke "top".</li>
		 *   <li>Menampilkan kode perjanjian dengan widget revisi
		 *       ({@link RevisiHelper#createNewRevisi}) sebagai sel pertama.</li>
		 *   <li>Jika perjanjian memiliki disposisi SOP, menampilkan link kecil
		 *       menuju alur SOP terkait.</li>
		 *   <li>Menampilkan nama penyedia, jenis perjanjian, nilai DP, dan tanggal
		 *       persetujuan sebagai sel-sel berikutnya.</li>
		 *   <li>Mengambil akun debet dan kredit dari jenis perjanjian. Jika keduanya
		 *       ada, membangun dan menampilkan tabel HTML preview jurnal. Jika tidak,
		 *       menampilkan pesan validasi.</li>
		 *   <li>Mengambil nomor bukti dari tabel {@code GrupTransaksi} untuk
		 *       perjanjian ini dan menampilkan status posting.</li>
		 *   <li>Menambahkan tombol Batalkan Posting (hanya untuk admin dengan
		 *       perjanjian sudah diposting) dan tombol Posting (hanya untuk
		 *       pengguna dengan hak edit dan perjanjian belum diposting).</li>
		 * </ol>
		 *
		 * <b>Parameter:</b>
		 * <ul>
		 *   <li>{@code arg0} — Komponen {@link Row} yang akan diisi dengan sel-sel data.</li>
		 *   <li>{@code arg1} — Objek data {@link PerjanjianKerjasamaMasterAsset} untuk baris ini.</li>
		 * </ul>
		 *
		 * <b>Return:</b> Tidak ada (void).<br>
		 *
		 * <b>Penanganan error:</b> Jika akun debet atau kredit tidak dikonfigurasi,
		 * ditampilkan pesan "Transaksi tidak valid" dengan detail akun mana yang
		 * tidak ada. Tombol aksi tidak akan muncul jika konfigurasi akun tidak lengkap.<br>
		 *
		 * <b>Pemeliharaan:</b> Jika format tabel jurnal preview perlu diubah (misalnya
		 * menambah kolom denda), perbarui blok pembangunan string {@code deskripsi}
		 * di dalam method ini.
		 *
		 * @param arg0 baris grid yang akan diisi komponen
		 * @param arg1 objek data perjanjian kerjasama untuk baris ini
		 * @throws Exception jika terjadi kesalahan saat membangun komponen atau query database
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset = (PerjanjianKerjasamaMasterAsset) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PerjanjianKerjasamaMasterAsset.class, perjanjianKerjasamaMasterAsset,
					perjanjianKerjasamaMasterAsset.getKode() == null ? "" : perjanjianKerjasamaMasterAsset.getKode()))
					.setParent(arg0);

			if (perjanjianKerjasamaMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + perjanjianKerjasamaMasterAsset.getDisposisiSop().getKeterangan() + " ("
						+ perjanjianKerjasamaMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(perjanjianKerjasamaMasterAsset.getDisposisiSop().getId(), null,
								null, true, arg0.getTarget());
					}
				});
			}

			new Label(perjanjianKerjasamaMasterAsset.getPenyedia() == null ? ""
					: perjanjianKerjasamaMasterAsset.getPenyedia().getNama()).setParent(arg0);

			new Label(perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset() == null ? ""
					: perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset().getNama()).setParent(arg0);

			Double nilai = perjanjianKerjasamaMasterAsset.getDp();

			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

			new Label(Common.dateFormat3.get().format(perjanjianKerjasamaMasterAsset.getTanggalPersetujuan())).setParent(arg0);

			Akun akunDebet = perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset() == null ? null
					: perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset().getAkunDp();

			Akun akunKredit = perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset() == null ? null
					: perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset().getAkunUtangDp();

			if (akunDebet != null && akunKredit != null) {

				Akun akunDenda = null;
				Double denda = 0.0;

				if (denda != null && denda > 0.1 && akunDenda == null) {
					new Label("Transaksi tidak valid. Ada denda " + Common.numberFormat.get().format(denda)
							+ ", namun Akun denda tidak ditemukan").setParent(arg0);
				} else {

					String deskripsi = "<table style='width:100%;'>" + "<thead>";
					deskripsi += "<tr>";
					deskripsi += "<th style='border:solid;border-width: thin;'>Akun</th>";
					deskripsi += "<th style='border:solid;border-width: thin;'>Debet</th>";
					deskripsi += "<th style='border:solid;border-width: thin;'>Kredit</th>";
					deskripsi += "</tr>" + "</thead>" + "<tbody>";

					deskripsi += "<tr>";
					deskripsi += "<td style='border:solid;border-width: thin;' >" + akunDebet.getKode() + " - "
							+ akunDebet.getNama() + "</td>";

					deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
							+ Common.numberFormat.get().format(nilai > 0.0 ? Math.abs(nilai) : 0.0) + "</td>";
					deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
							+ Common.numberFormat.get().format(nilai > 0.0 ? 0.0 : Math.abs(nilai)) + "</td>";
					deskripsi += "</tr>";

					deskripsi += "<tr>";
					deskripsi += "<td style='border:solid;border-width: thin;' >" + akunKredit.getKode() + " - "
							+ akunKredit.getNama() + "</td>";

					deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
							+ Common.numberFormat.get().format(nilai > 0.0 ? 0.0 : Math.abs(nilai - denda)) + "</td>";
					deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
							+ Common.numberFormat.get().format(nilai > 0.0 ? Math.abs(nilai - denda) : 0.0) + "</td>";
					deskripsi += "</tr>";

					if (denda != null && denda > 0.1 && akunDenda != null) {
						deskripsi += "<tr>";
						deskripsi += "<td style='border:solid;border-width: thin;' >" + akunDenda.getKode() + " - "
								+ akunDenda.getNama() + "</td>";

						deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
								+ Common.numberFormat.get().format(denda > 0.0 ? 0.0 : Math.abs(denda)) + "</td>";
						deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
								+ Common.numberFormat.get().format(denda > 0.0 ? Math.abs(denda) : 0.0) + "</td>";
						deskripsi += "</tr>";
					}

					deskripsi += "</tbody></table>";
					new ais.ui.util.MyHtml(deskripsi).setParent(arg0);
				}
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
					.add(Restrictions.eq("perjanjianKerjasamaMasterAsset", perjanjianKerjasamaMasterAsset))
					.setMaxResults(1).setProjection(Projections.property("kode")).uniqueResult();

			new Label(perjanjianKerjasamaMasterAsset.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: perjanjianKerjasamaMasterAsset.getPostingHistory().toString() + ", no. bukti : " + bukti)
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && perjanjianKerjasamaMasterAsset.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								perjanjianKerjasamaMasterAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(perjanjianKerjasamaMasterAsset);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where perjanjian_kerjasama_master_asset="
												+ perjanjianKerjasamaMasterAsset.getId() + " and closing is null")
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
				button.setVisible(
						edit && perjanjianKerjasamaMasterAsset.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PERJANJIAN_KERJASAMA);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunDebet = perjanjianKerjasamaMasterAsset
										.getJenisPerjanjianKerjasamaAsset() == null ? null
												: perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset()
														.getAkunDp();

								Akun akunKredit = perjanjianKerjasamaMasterAsset
										.getJenisPerjanjianKerjasamaAsset() == null ? null
												: perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset()
														.getAkunUtangDp();

								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {

										ket = "Perjanjian kerjasama terhadap kode \""
												+ (perjanjianKerjasamaMasterAsset.getKode() + "-"
														+ perjanjianKerjasamaMasterAsset.getKeterangan())
												+ "\" pada penyedia "
												+ perjanjianKerjasamaMasterAsset.getPenyedia().getNama() + " sebanyak "
												+ Common.numberFormat.get().format(perjanjianKerjasamaMasterAsset.getDp());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;
									Double nilai = perjanjianKerjasamaMasterAsset.getDp();

									SatuanKerja satuanKerja = (SatuanKerja) (perjanjianKerjasamaMasterAsset != null
											&& perjanjianKerjasamaMasterAsset.getSatuanKerja() != null
													? perjanjianKerjasamaMasterAsset.getSatuanKerja()
													: tbmuser.ambilSatuanKerja());

									if (tbmuser.ambilSatuanKerja() != null) {
										satuanKerja = tbmuser.ambilSatuanKerja();
									}

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket,
												perjanjianKerjasamaMasterAsset.getTanggalPersetujuan(), nilai, denda,
												perjanjianKerjasamaMasterAsset, satuanKerja, session);
									} else {
										CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket,
												perjanjianKerjasamaMasterAsset.getTanggalPersetujuan(), nilai, denda,
												perjanjianKerjasamaMasterAsset, satuanKerja, session);
									}

									perjanjianKerjasamaMasterAsset.setPostingHistory(postingHistory);
									session.update(perjanjianKerjasamaMasterAsset);
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
	 * yang merepresentasikan query pencarian data perjanjian kerjasama sesuai
	 * semua filter yang aktif di antarmuka pengguna.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuka sesi Hibernate saat ini via {@link HibernateUtil#currentSession()}.</li>
	 *   <li>Membuat criteria dasar untuk entitas {@link PerjanjianKerjasamaMasterAsset}.</li>
	 *   <li>Menambahkan filter status posting berdasarkan checkbox
	 *       {@code searchtampil} (belum diposting) dan {@code searchtelahtampil}
	 *       (sudah diposting).</li>
	 *   <li>Membatasi hanya pada perjanjian yang telah disetujui
	 *       ({@code disetujuiOleh} tidak null).</li>
	 *   <li>Membatasi hanya pada perjanjian dengan nilai DP tidak nol dan tidak null.</li>
	 *   <li>Menambahkan filter rentang tanggal pembuatan antara {@code tglMulai}
	 *       dan {@code tglSampai} menggunakan SQL native.</li>
	 *   <li>Menambahkan filter pemilik aset, lokasi, dan ruang jika dipilih.</li>
	 *   <li>Menambahkan filter kode/nama menggunakan ILIKE (case-insensitive)
	 *       dengan mode ANYWHERE.</li>
	 *   <li>Jika parameter {@code order} true, menambahkan ordering descending
	 *       berdasarkan ID.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code order} — Jika {@code true}, criteria akan diurutkan secara
	 *       descending berdasarkan kolom ID. Gunakan {@code false} untuk query
	 *       penghitung (count) yang tidak memerlukan ordering.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Objek {@link Criteria} Hibernate yang siap untuk dieksekusi
	 * dengan memanggil {@code .list()} atau {@code .uniqueResult()}. Criteria yang
	 * sama harus digunakan untuk pagination dan pengambilan data agar konsisten.<br>
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit; exception
	 * Hibernate akan muncul ke level atas jika terjadi masalah koneksi atau query.<br>
	 *
	 * <b>Pemeliharaan:</b> Jika ada filter baru yang ditambahkan ke ZUL, tambahkan
	 * kondisi Restrictions yang sesuai di sini. Pastikan nama property Hibernate
	 * sesuai dengan field di entitas {@link PerjanjianKerjasamaMasterAsset}.
	 *
	 * @param order jika {@code true} maka hasil query diurutkan descending berdasarkan ID
	 * @return objek {@link Criteria} Hibernate dengan semua filter aktif yang sudah diterapkan
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PerjanjianKerjasamaMasterAsset.class)

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(!searchtelahtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNotNull("postingHistory"))

				.add(Restrictions.isNotNull("disetujuiOleh"))

				.add(Restrictions.ne("dp", 0.0)).add(Restrictions.isNotNull("dp"))
				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(tanggal_pembuatan) between date('" + Common.databaseDateFormat.get().format(tglMulai.getValue())
								+ "') and  date('" + Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchpemilikAsset.getSelectedItem() == null
						|| searchpemilikAsset.getSelectedItem().getValue() == null
						|| searchpemilikAsset.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("pemilikAsset", searchpemilikAsset.getSelectedItem().getValue()))
				.add(searchlokasi.getSelectedItem() == null || searchlokasi.getSelectedItem().getValue() == null
						|| searchlokasi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))
				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", searchkode.getValue(), MatchMode.ANYWHERE)));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Mengeksekusi pencarian dan memuat data ke grid tanpa mekanisme
	 * progress loading. Metode ini adalah implementasi inti pencarian yang dipanggil
	 * dari dalam mekanisme progress di {@link #loadDataDenganProgressPosting(Event)}.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menginisialisasi paging berdasarkan total jumlah data dari criteria
	 *       tanpa ordering.</li>
	 *   <li>Mengambil halaman data yang sesuai berdasarkan halaman aktif paging,
	 *       dengan batas {@link Common#ROWS_COUNT_ON_PAGE} baris per halaman.</li>
	 *   <li>Membungkus data dalam {@link SimpleListModel} dan menetapkan renderer
	 *       serta model ke grid.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — Event ZKoss pemicu pencarian. Dapat {@code null} jika
	 *       dipanggil secara programatik.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit.<br>
	 *
	 * <b>Pemeliharaan:</b> Jangan memanggil method ini langsung dari event handler;
	 * selalu gunakan {@link #loadDataDenganProgressPosting(Event)} agar mekanisme
	 * anti-tumpang tindih dan progress indicator berjalan dengan benar.
	 *
	 * @param event event ZKoss pemicu, dapat null
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PerjanjianKerjasamaMasterAsset> perjanjianKerjasamaMasterAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(perjanjianKerjasamaMasterAsset);
		grid.setRowRenderer(new PerjanjianKerjasamaMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Delegator publik untuk event pencarian default yang dapat
	 * dipanggil dari ZUL (misalnya saat tombol Cari diklik atau filter berubah).
	 * Mendelegasikan ke {@link #loadDataDenganProgressPosting(Event)} agar
	 * mekanisme progress loading tetap aktif.<br>
	 *
	 * <b>Cara kerja:</b> Memanggil {@link #loadDataDenganProgressPosting(Event)}
	 * dengan event yang diterima, yang akan mengelola flag loading dan progress
	 * indicator secara otomatis.<br>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — Event ZKoss yang memicu pencarian. Diteruskan ke
	 *       {@code loadDataDenganProgressPosting}.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Tidak ada; exception akan muncul dari layer bawah.<br>
	 *
	 * <b>Pemeliharaan:</b> Method ini dipanggil dari ZUL dan dari beberapa bagian
	 * kode internal. Jangan ubah nama method ini tanpa memperbarui referensi di ZUL.
	 *
	 * @param event event ZKoss dari interaksi pengguna atau pemanggilan programatik
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Flag yang menandakan apakah proses pemuatan data posting jurnal sedang berjalan.
	 * Digunakan untuk mencegah permintaan reload berikutnya dieksekusi secara bersamaan,
	 * sehingga menghindari race condition pada operasi grid dan paging.
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag yang menandakan bahwa ada permintaan reload data yang tertunda (diterima
	 * saat proses sebelumnya masih berjalan). Jika {@code true}, setelah proses
	 * selesai, reload akan segera dieksekusi ulang.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * <b>Tujuan:</b> Memuat data grid perjanjian kerjasama dengan mekanisme progress
	 * indicator yang informatif dan anti-tumpang tindih. Ini adalah metode inti
	 * untuk semua operasi pemuatan data di halaman ini.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code postingJurnalLoadingAktif} bernilai true (proses sebelumnya
	 *       masih berjalan), tandai {@code postingJurnalReloadTertunda = true},
	 *       tampilkan pesan antri, dan return tanpa memulai proses baru.</li>
	 *   <li>Jika tidak ada proses aktif, set flag aktif dan reset flag tertunda,
	 *       lalu tampilkan progress awal via {@code PostingJurnalLoadingUtil.show}.</li>
	 *   <li>Menjadwalkan timer ZKoss untuk mengeksekusi pemuatan data aktual di
	 *       event berikutnya (non-blokir UI), memperbarui progress di 48% dan 92%.</li>
	 *   <li>Setelah pemuatan selesai (dalam blok finally), reset flag aktif.</li>
	 *   <li>Jika ada reload tertunda, jadwalkan ulang pemuatan data sekali lagi.</li>
	 *   <li>Jika tidak ada reload tertunda, tandai progress selesai (100%).</li>
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
	 * <b>Penanganan error:</b> Blok try-finally memastikan flag aktif selalu
	 * direset meskipun terjadi exception di dalam pemuatan data, sehingga
	 * halaman tidak terkunci dalam kondisi "loading selamanya".<br>
	 *
	 * <b>Pemeliharaan:</b> Jika threshold persentase progress perlu disesuaikan
	 * atau pesan progress perlu diubah, cari semua pemanggilan
	 * {@code PostingJurnalLoadingUtil.show/update/complete} di method ini.
	 * Jangan mengakses field {@code postingJurnalLoadingAktif} dari thread latar
	 * karena field ini tidak sinkron.
	 *
	 * @param event event ZKoss yang memicu pemuatan data, dapat null
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
