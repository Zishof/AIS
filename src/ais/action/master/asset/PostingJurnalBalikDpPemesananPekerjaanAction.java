package ais.action.master.asset;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
 * <h3>PostingJurnalBalikDpPemesananPekerjaanAction</h3>
 *
 * <b>Untuk apa:</b><br>
 * Controller ZK (ZUL composer) yang mengelola proses <em>posting jurnal balik</em>
 * atas transaksi Down Payment (DP) pada pemesanan pengadaan pekerjaan (work order).
 * Jurnal balik (reversal) dibutuhkan ketika DP yang sudah dicatat sebagai utang DP
 * kemudian harus diakui sebagai tagihan pekerjaan nyata, sehingga entri akuntansi
 * awal harus dibalik dan digantikan dengan entri yang mencerminkan realisasi tagihan.
 * Halaman ini menampilkan daftar pemesanan yang memenuhi kriteria DP bertermin,
 * sudah disetujui, dan memiliki formula termin yang menyatakan {@code merupakan_dp:true}
 * serta {@code setuju:true}. Pengguna dapat memposting satu per satu maupun
 * memposting atau membatalkan posting seluruh data sekaligus.<br><br>
 *
 * <b>Cara kerja:</b><br>
 * <ol>
 *   <li>Saat halaman pertama kali dimuat, {@code doAfterCompose} membaca parameter
 *       URL {@code sudah_posting}, mengisi filter tanggal default (6 bulan ke belakang
 *       sampai hari ini), mengisi combo pemilik aset dan lokasi, mengunci lokasi sesuai
 *       sesi pengguna, kemudian memanggil {@code loadDataDenganProgressPosting} untuk
 *       menampilkan data grid.</li>
 *   <li>Filter pencarian ({@code initCriteria}) membangun {@code Criteria} Hibernate yang
 *       memfilter {@link PemesananPengadaanMasterAsset} berdasarkan: status posting,
 *       rentang tanggal persetujuan, pemilik aset, lokasi, ruang, dan kode/nama.
 *       Khusus bila parameter {@code sudah_posting} diberikan dari URL, filter panel
 *       disembunyikan dan hanya data dengan status posting yang sesuai yang ditampilkan.</li>
 *   <li>Data di-render oleh inner class {@link PemesananPengadaanMasterAssetRenderer}
 *       yang mengurai field {@code formula} (JSON array termin) dan menampilkan
 *       informasi akun debet/kredit, nilai tagihan, status posting, serta tombol
 *       Posting dan Batalkan Posting per baris.</li>
 *   <li>Proses posting massal ({@code onPostingSemua}) berjalan di thread terpisah
 *       agar tidak memblok UI ZK; progress ditampilkan melalui
 *       {@code PostingJurnalLoadingUtil}. Setiap termin yang belum diposting akan
 *       dibuatkan {@link GrupTransaksi} baru dengan ref {@code DP_BALIK_PEKERJAAN}
 *       dan entitas {@link PostingHistory} yang sama.</li>
 *   <li>Mekanisme anti-tumpang-tindih reload (flag {@code postingJurnalLoadingAktif}
 *       dan {@code postingJurnalReloadTertunda}) memastikan permintaan reload yang
 *       masuk selagi proses sebelumnya berjalan tidak hilang, melainkan dieksekusi
 *       segera setelah proses selesai.</li>
 * </ol>
 *
 * <b>Threading:</b><br>
 * Proses posting massal ({@code onPostingSemua}) membuka thread baru Java
 * ({@code new Thread(...).start()}) untuk menghindari timeout ZK. Thread tersebut
 * menggunakan {@code HibernateUtil.currentNativeSession()} agar memiliki sesi
 * Hibernate mandiri dari sesi UI. Semua operasi tulis dibungkus transaksi eksplisit
 * ({@code beginTransaction / commit}). Thread UI diperbarui melalui timer ZK
 * ({@code Common.createDefaultTimer}) setelah thread selesai.
 * Flag {@code postingJurnalLoadingAktif} bukan {@code volatile} sehingga hanya aman
 * diakses dari thread event ZK tunggal; hindari mengaksesnya dari thread latar.<br><br>
 *
 * <b>Pemeliharaan:</b><br>
 * <ul>
 *   <li>Konfigurasi akun debet/kredit diambil dari
 *       {@code JenisPemesananPengadaanAsset.getAkunUtangPekerjaan()} dan
 *       {@code getAkunUtangDp()}; pastikan akun-akun tersebut diisi di master jenis
 *       pemesanan agar posting tidak diabaikan.</li>
 *   <li>Ref jurnal yang digunakan adalah literal string {@code "DP_BALIK_PEKERJAAN"};
 *       jika berubah, sesuaikan pula query hapus SQL di {@code onBatalkanPostingSemua}
 *       dan tombol batalkan per baris.</li>
 *   <li>Halaman ZUL pasangan harus menyediakan komponen: {@code grid}, {@code searchkode},
 *       {@code searchpemilikAsset}, {@code searchlokasi}, {@code searchruang},
 *       {@code searchtampil}, {@code searchtelahtampil}, {@code tglMulai},
 *       {@code tglSampai}, {@code sent}, {@code filter}, {@code rowPosting}.</li>
 * </ul>
 *
 * @author AIS Development Team
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 */
public class PostingJurnalBalikDpPemesananPekerjaanAction extends GenericAutowireComposer {

	/**
	 * Versi serial untuk serialisasi kelas oleh mekanisme ZK.
	 * Nilai ini tidak perlu diubah kecuali ada perubahan struktural pada field-field
	 * yang di-serialisasi.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar pemesanan dengan status posting jurnal balik DP. */
	private MyGrid grid;

	/** Kotak teks pencarian berdasarkan kode atau nama pemesanan. */
	private Textbox searchkode;

	/** Combo filter pemilik aset; diisi dari tabel {@link PemilikAsset} yang aktif. */
	private Combobox searchpemilikAsset;

	/** Combo filter lokasi; diisi dari tabel {@link Lokasi} yang aktif. */
	private Combobox searchlokasi;

	/** Banbox filter ruang; nilai ruang disimpan sebagai atribut komponen. */
	private AmbilDataRuangBanbox searchruang;

	/** Checkbox filter untuk menampilkan data yang belum diposting. */
	private MyCheckboxConfig searchtampil;

	/** Checkbox filter untuk menampilkan data yang telah diposting. */
	private MyCheckboxConfig searchtelahtampil;

	/**
	 * Menandai apakah pengguna memiliki hak ubah (UPDATE privilege).
	 * Digunakan untuk mengatur visibilitas tombol posting dan batalkan posting.
	 */
	private boolean edit = false;

	/** Tombol "Posting Semua" pada toolbar atas; visibilitasnya bergantung pada hak {@code edit}. */
	private MyToolbarbuttonConfig sent;

	/**
	 * Menandai apakah pengguna adalah admin atau memiliki hak APPROVE.
	 * Digunakan untuk mengatur visibilitas tombol "Batalkan Posting" per baris.
	 */
	public boolean adminLain;

	/** Datebox tanggal awal filter rentang tanggal persetujuan. */
	private MyDatebox tglMulai;

	/** Datebox tanggal akhir filter rentang tanggal persetujuan. */
	private MyDatebox tglSampai;

	/** Pengguna yang sedang login; digunakan saat membuat {@link PostingHistory}. */
	private Tbmuser tbmuser;

	/** Panel filter di bagian North; disembunyikan bila parameter {@code sudah_posting} diberikan. */
	private North filter;

	/** Baris form untuk menampilkan info status posting; ditampilkan bila {@code sudah_posting} diberikan. */
	private Row rowPosting;

	/**
	 * Status posting yang difilter secara programatik dari parameter URL.
	 * {@code null} berarti tidak ada filter khusus (tampilkan semua);
	 * {@code true} hanya menampilkan yang sudah diposting;
	 * {@code false} hanya menampilkan yang belum diposting.
	 */
	private Boolean sudah_posting = null;

	/**
	 * Mengembalikan {@code true} bila kedua akun merujuk entitas yang sama (dibandingkan
	 * berdasarkan {@code id} agar kebal terhadap identity-map/override equals). Dipakai
	 * sebagai penjaga agar jurnal balik DP tidak pernah dibuat dengan akun debet == akun
	 * kredit (yang akan menghasilkan jurnal Dr X / Cr X = bernilai nol/tak bermakna).
	 */
	private static boolean akunSama(Akun a, Akun b) {
		if (a == null || b == null) {
			return false;
		}
		if (a == b) {
			return true;
		}
		return a.getId() != null && a.getId().equals(b.getId());
	}

	/**
	 * <b>Tujuan:</b> Interceptor siklus hidup ZK yang dijalankan <em>sebelum</em> komponen
	 * ZUL di-compose. Digunakan untuk memeriksa keamanan akses halaman sebelum komponen
	 * apapun diinisialisasi, sehingga pengguna yang tidak berhak tidak dapat mengakses
	 * data maupun antarmuka.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} yang akan melempar exception atau
	 * melakukan redirect jika pengguna tidak memiliki sesi yang valid. Setelah itu
	 * mendelegasikan ke implementasi induk {@code super.doBeforeCompose} agar proses
	 * compose ZK berjalan normal.<br><br>
	 *
	 * <b>Parameter:</b><br>
	 * @param page halaman ZK tempat komponen akan di-compose
	 * @param parent komponen induk dalam hierarki ZK
	 * @param compInfo metadata komponen dari ZUL
	 *
	 * <b>Return:</b><br>
	 * @return {@code ComponentInfo} dari implementasi induk, digunakan ZK untuk
	 *         melanjutkan proses compose.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika {@code doCheckSecurity} mendeteksi sesi tidak valid, proses akan dihentikan
	 * sebelum {@code super.doBeforeCompose} dipanggil. Exception runtime dapat dilempar
	 * oleh framework ZK jika terjadi masalah pada proses compose itu sendiri.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jangan menambahkan logika bisnis di method ini; tetap gunakan hanya untuk
	 * pemeriksaan keamanan awal. Logika inisialisasi data harus ditempatkan di
	 * {@code doAfterCompose}.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Metode inisialisasi utama yang dijalankan ZK <em>setelah</em> seluruh
	 * komponen ZUL berhasil di-wire ke field Java. Di sinilah semua logika inisialisasi
	 * halaman dilakukan: validasi sesi, pembacaan parameter URL, pengaturan default filter
	 * tanggal, pengisian combo, penguncian lokasi, penentuan hak akses, dan pemuatan
	 * data grid awal.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose} agar wire otomatis ZK selesai.</li>
	 *   <li>Memeriksa sesi {@code usersTemp} dan privilege READ; jika tidak ada, pengguna
	 *       diarahkan ke halaman logoff.</li>
	 *   <li>Membaca parameter URL {@code sudah_posting}; jika ada, menyembunyikan panel
	 *       filter dan menampilkan baris status posting.</li>
	 *   <li>Menetapkan rentang tanggal default: tglMulai = 6 bulan lalu, tglSampai = hari ini.
	 *       Jika parameter URL {@code mulai} dan {@code sampai} diberikan, nilai dari URL
	 *       digunakan dan datebox dinonaktifkan.</li>
	 *   <li>Menentukan flag {@code adminLain} dari privilege ADMIN atau APPROVE, dan
	 *       flag {@code edit} dari privilege UPDATE.</li>
	 *   <li>Mengisi combo pemilik aset dan lokasi dari database; jika sesi menyimpan
	 *       atribut {@code Lokasi}, lokasi tersebut dipilih dan combo dikunci.</li>
	 *   <li>Memanggil {@code loadDataDenganProgressPosting(null)} untuk memuat data
	 *       grid pertama kali.</li>
	 *   <li>Menyiapkan filter lanjut melalui {@code FilterLanjutHelper.setup}.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param comp komponen root ZUL yang baru saja selesai di-compose oleh ZK
	 * @throws Exception jika terjadi kesalahan parsing tanggal dari parameter URL,
	 *         atau jika inisialisasi komponen ZK gagal
	 *
	 * <b>Penanganan error:</b><br>
	 * Kesalahan parsing parameter tanggal ditangkap secara lokal dan ditampilkan
	 * hanya kepada admin melalui {@code Common.tampilErrorJikaAdmin}. Kesalahan
	 * sesi dialihkan ke logoff tanpa menampilkan pesan error.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Urutan inisialisasi penting: combo harus diisi sebelum {@code loadDataDenganProgressPosting}
	 * dipanggil, karena filter bergantung pada nilai combo. Jika ada field ZUL baru
	 * yang perlu diinisialisasi, tambahkan di sini dengan null-check yang sesuai.
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

        FilterLanjutHelper.setup(comp);
}

	/**
	 * <b>Tujuan:</b> Menangani event pembatalan posting <em>semua</em> transaksi tagihan
	 * pekerjaan sekaligus. Pengguna diminta konfirmasi terlebih dahulu melalui dialog
	 * pertanyaan sebelum operasi dijalankan.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menampilkan dialog konfirmasi bertipe QUESTION dengan pilihan OK dan CANCEL.</li>
	 *   <li>Jika pengguna memilih OK, mengambil semua {@link PemesananPengadaanMasterAsset}
	 *       yang memiliki {@code postingHistory} tidak null (sudah diposting) menggunakan
	 *       {@code initCriteria(true)}.</li>
	 *   <li>Untuk setiap pemesanan, menyetel {@code postingHistory} menjadi null (membatalkan
	 *       posting di level entitas) dan menghapus entri jurnal terkait dari tabel
	 *       {@code akunting.grup_transaksi} dengan ref {@code DP_BALIK_PEKERJAAN}
	 *       yang belum closing.</li>
	 *   <li>Setelah selesai, memuat ulang data grid melalui timer ZK.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param event event ZK yang memicu pemanggilan method ini, biasanya dari onClick
	 *              tombol "Batalkan Posting Semua" di toolbar
	 * @throws Exception jika terjadi kesalahan Hibernate atau ZK saat menghapus data
	 *
	 * <b>Penanganan error:</b><br>
	 * Tidak ada try-catch eksplisit; jika terjadi exception Hibernate, exception akan
	 * merambat ke framework ZK dan ditampilkan sebagai error dialog. Disarankan untuk
	 * menambahkan penanganan error eksplisit pada pemeliharaan mendatang.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Operasi ini bersifat massal dan tidak dapat diurungkan; pastikan hak akses
	 * {@code adminLain} sudah diperiksa di level tampilan sebelum tombol ini dapat diklik.
	 * Query SQL native yang digunakan bergantung pada nama kolom dan skema
	 * {@code akunting.grup_transaksi}; perbarui jika ada perubahan skema.
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi tagihan pekerjaan ?", "Pertanyaan",
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
												+ pemesananPengadaanMasterAsset.getId()
												+ " and ref = 'DP_BALIK_PEKERJAAN'" + " and closing is null")
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
	 * <b>Tujuan:</b> Menampilkan dialog modal pengisian parameter posting dan kemudian
	 * mengeksekusi proses posting jurnal balik DP untuk <em>semua</em> transaksi
	 * yang belum diposting dalam rentang tanggal yang dipilih.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Membuat {@link MyWindow} modal berukuran 800x300 px yang berisi form
	 *       dengan kolom: Tanggal/Waktu (datebox), Diposting oleh (label nama pengguna
	 *       aktif), dan Keterangan (textarea).</li>
	 *   <li>Tombol "Batal" menutup window tanpa melakukan apa-apa.</li>
	 *   <li>Tombol "Simpan" memvalidasi bahwa tanggal sudah diisi, lalu menampilkan
	 *       konfirmasi posting kedua.</li>
	 *   <li>Jika dikonfirmasi, memulai thread baru yang:
	 *     <ul>
	 *       <li>Membuat satu entitas {@link PostingHistory} bersama untuk semua transaksi
	 *           dalam batch ini, berisi tanggal, pengguna, dan keterangan.</li>
	 *       <li>Iterasi setiap {@link PemesananPengadaanMasterAsset} dari hasil
	 *           {@code initCriteria}.</li>
	 *       <li>Untuk setiap termin dalam field {@code formula} (JSON array) yang
	 *           {@code setuju:true} dan belum ada entri jurnal ({@code rowCount==0}),
	 *           menghitung nilai tagihan (penagihan + PPN - pinalti) dan memanggil
	 *           {@code CommonAkunting.saveTransaksi}.</li>
	 *       <li>Jika nilai positif (&gt;0.1): debet=akunUtangPekerjaan, kredit=akunUtangDp;
	 *           jika nol/negatif: posisi dibalik.</li>
	 *       <li>Setelah tiap transaksi berhasil, mengisi {@code postingHistory} pada
	 *           pemesanan dan menyimpannya kembali.</li>
	 *       <li>Memperbarui label progress dengan persentase kemajuan.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Setelah thread selesai, menampilkan pesan sukses dan memanggil
	 *       {@code onSearchDefault} untuk memuat ulang grid.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param event event ZK dari onClick tombol "Posting Semua" di toolbar
	 * @throws Exception jika terjadi kesalahan pembuatan komponen ZK atau akses database
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception pada level termin individual diabaikan (blok catch kosong) agar
	 * termin lain tetap diproses. Exception pada level pemesanan individual juga
	 * diabaikan. Hanya kesalahan pada pembuatan {@code PostingHistory} atau
	 * inisialisasi yang akan menghentikan proses. Disarankan untuk menambahkan
	 * logging pada blok catch kosong di pemeliharaan mendatang.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Thread baru menggunakan {@code HibernateUtil.currentNativeSession()} dan harus
	 * menutup sesi dengan {@code HibernateUtil.closeSession()} di akhir (sudah dilakukan).
	 * Pastikan tidak ada akses ke komponen ZK dari dalam thread latar selain melalui
	 * {@code label.setValue} yang di-sync oleh ZK server push. Keterangan posting
	 * secara otomatis menyertakan rentang tanggal dari filter; jika format tanggal
	 * diubah, sesuaikan pula string keterangan.
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Transaksi tagihan pekerjaan");
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
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting Jurnal Balik DP belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal dan pilih tanggal dari kalender; (2) Pastikan tanggal berada dalam periode akuntansi yang aktif; (3) ulangi proses posting ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi tagihan pekerjaan ?",
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
													PostingHistory.JENIS_TAGIHAN_DP_BALIK_PEKERJAAN);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<PemesananPengadaanMasterAsset> pemesananPengadaanMasterAssets = initCriteria(
													true).list();

											int rowIndex = 1;
											for (PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset : pemesananPengadaanMasterAssets) {

												SatuanKerja satuanKerja = (SatuanKerja) pemesananPengadaanMasterAsset
														.getSatuanKerja();

												if (pemesananPengadaanMasterAsset != null) {

													try {

														JSONArray array = new JSONArray(
																pemesananPengadaanMasterAsset.getFormula());
														for (int i = 0; i < array.length(); i++) {
															try {

																JSONObject jsonObject = array.getJSONObject(i);

																if (jsonObject.isNull("key")) {
																	continue;
																}

																Boolean setuju;
																if (!jsonObject.isNull("setuju")) {
																	setuju = Boolean.parseBoolean(
																			jsonObject.get("setuju") + "");
																} else {
																	setuju = false;
																}

																if (setuju) {

																	final String key;

																	if (!jsonObject.isNull("key")) {
																		key = jsonObject.get("key") + "";
																	} else {
																		key = "";
																	}

																	Number bukti = (Number) session
																			.createCriteria(GrupTransaksi.class)
																			.add(Restrictions.eq(
																					"pemesananPengadaanMasterAsset",
																					pemesananPengadaanMasterAsset))
																			.add(Restrictions.eq("ref", key))
																			.setProjection(Projections.rowCount())
																			.uniqueResult();

																	if (bukti == null || bukti.intValue() == 0) {

																		final String nama;

																		if (!jsonObject.isNull("nama")) {
																			nama = jsonObject.get("nama") + "";
																		} else {
																			nama = "";
																		}

																		final String nomor;

																		if (!jsonObject.isNull("nomor")) {
																			nomor = jsonObject.get("nomor") + "";
																		} else {
																			nomor = "";
																		}

																		Double pinalti = 0.0;
																		if (!jsonObject.isNull("pinalti")) {
																			pinalti = jsonObject.getDouble("pinalti");
																		}

																		final Double penagihan;
																		if (!jsonObject.isNull("penagihan")) {
																			penagihan = jsonObject
																					.getDouble("penagihan");
																		} else {
																			penagihan = 0.0;
																		}

																		Double ppn = 0.0;
																		if (!jsonObject.isNull("ppn")) {
																			ppn = jsonObject.getDouble("ppn");
																		}
																		Double total = penagihan
																				+ ((ppn / 100.0) * penagihan);
																		final Double nilai = Math.abs(total - pinalti);

																		try {

																			Akun akunDebet = pemesananPengadaanMasterAsset
																					.getJenisPemesananPengadaanAsset() == null
																							? null
																							: pemesananPengadaanMasterAsset
																									.getJenisPemesananPengadaanAsset()
																									.getAkunUtangPekerjaan();

																			Akun akunKredit = pemesananPengadaanMasterAsset
																					.getJenisPemesananPengadaanAsset() == null
																							? null
																							: pemesananPengadaanMasterAsset
																									.getJenisPemesananPengadaanAsset()
																									.getAkunUtangDp();

																			Boolean apakahUangMasuk = true;
																			Akun akunDenda = null;
																			Akun akunPiutangDenda = null;
																			Double denda = 0.0;

																			String ket = "";
																			try {

																				ket = "Tagihan pekerjaan terhadap pemesanan \""
																						+ (pemesananPengadaanMasterAsset
																								.getKode() + "-" + nomor
																								+ "-" + nama + " "
																								+ pemesananPengadaanMasterAsset
																										.getKeterangan())
																						+ "\" sebanyak "
																						+ Common.numberFormat.get().format(
																								pemesananPengadaanMasterAsset
																										.getDptotal());

																			} catch (Exception e) {
																				Common.tampilErrorJikaAdmin(e);
																			}

																			label.setValue(ket + " ("
																					+ Common.numberFormat.get()
																							.format(rowIndex * 100.0
																									/ pemesananPengadaanMasterAssets
																											.size())
																					+ " %)");

																			// PENJAGA: akun debet & kredit tak boleh sama (jurnal nol tak valid) -> lewati.
																			if (akunDebet != null
																					&& akunKredit != null
																					&& !akunSama(akunDebet, akunKredit)) {

																				try {

																					session.getTransaction().begin();
																					if (nilai > 0.1) {
																						CommonAkunting.saveTransaksi(
																								akunDebet, akunKredit,
																								akunDenda,
																								akunPiutangDenda,
																								postingHistory,
																								apakahUangMasuk, ket,
																								pemesananPengadaanMasterAsset
																										.getTanggalPembuatan(),
																								nilai, denda,
																								pemesananPengadaanMasterAsset,
																								satuanKerja,
																								"DP_BALIK_PEKERJAAN",
																								session);
																					} else {
																						CommonAkunting.saveTransaksi(
																								akunKredit, akunDebet,
																								akunDenda,
																								akunPiutangDenda,
																								postingHistory,
																								apakahUangMasuk, ket,
																								pemesananPengadaanMasterAsset
																										.getTanggalPembuatan(),
																								nilai, denda,
																								pemesananPengadaanMasterAsset,
																								satuanKerja,
																								"DP_BALIK_PEKERJAAN",
																								session);
																					}
																					session.getTransaction().commit();
																				} catch (Exception e) {
																					Common.tampilErrorJikaAdmin(e);
																				}

																				pemesananPengadaanMasterAsset
																						.setPostingHistory(
																								postingHistory);
																				session.getTransaction().begin();
																				session.update(
																						pemesananPengadaanMasterAsset);
																				session.getTransaction().commit();
																			}
																		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingJurnalBalikDpPemesananPekerjaanAction.java:788");
																			// exception pada level akun/transaksi diabaikan agar termin lain tetap diproses
																		}

																	}
																	rowIndex++;
																}
															} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingJurnalBalikDpPemesananPekerjaanAction.java:795");
																// exception pada level termin diabaikan agar termin lain tetap diproses
															}
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingJurnalBalikDpPemesananPekerjaanAction.java:799");
														// exception pada level formula JSON diabaikan agar pemesanan lain tetap diproses
													}
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

	/**
	 * <b>Untuk apa:</b><br>
	 * Inner class renderer baris grid yang bertanggung jawab merender setiap baris
	 * data pada grid utama halaman ini. Setiap baris merepresentasikan satu entri
	 * termin DP dari suatu {@link PemesananPengadaanMasterAsset}, bukan pemesanan
	 * secara keseluruhan (satu pemesanan bisa menghasilkan beberapa baris jika
	 * memiliki banyak termin DP yang disetujui).<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Menerima {@code Object[]} berisi dua elemen: {@code [0]} adalah {@link JSONObject}
	 * termin dari field {@code formula}, dan {@code [1]} adalah entitas
	 * {@link PemesananPengadaanMasterAsset}. Renderer mengurai data JSON untuk
	 * mendapatkan nomor termin, nama, nilai penagihan, PPN, pinalti, dan tanggal.
	 * Kemudian menampilkan kolom-kolom berikut:
	 * <ul>
	 *   <li>Kolom 1: Nomor/nama termin + link SOP jika ada</li>
	 *   <li>Kolom 2: Nama penyedia</li>
	 *   <li>Kolom 3: Nama jenis pemesanan</li>
	 *   <li>Kolom 4: Nilai tagihan (dengan info pinalti jika ada)</li>
	 *   <li>Kolom 5: Tanggal tagihan</li>
	 *   <li>Kolom 6: Preview jurnal debet/kredit</li>
	 *   <li>Kolom 7: Status posting (belum/sudah + nomor bukti)</li>
	 *   <li>Kolom 8: Toolbar aksi (tombol Posting dan Batalkan Posting)</li>
	 * </ul>
	 *
	 * <b>Threading:</b><br>
	 * Renderer dijalankan di thread event ZK (main thread); tidak ada threading tambahan.
	 * Akses Hibernate menggunakan {@code HibernateUtil.currentSession()} yang aman
	 * dalam konteks ini.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Visibilitas tombol "Batalkan Posting" bergantung pada flag {@code adminLain} (outer class).
	 * Visibilitas tombol "Posting" bergantung pada flag {@code edit} dan
	 * {@code postingHistory == null}. Jika ada penambahan kolom grid di ZUL, sesuaikan
	 * urutan {@code setParent(arg0)} pada renderer ini.
	 */
	class PemesananPengadaanMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris grid dengan data termin DP dari
		 * {@link PemesananPengadaanMasterAsset} beserta kontrol aksi posting.<br><br>
		 *
		 * <b>Cara kerja:</b><br>
		 * Mengurai {@code Object[]} input menjadi {@link JSONObject} termin dan entitas
		 * pemesanan, kemudian mengekstrak semua field yang dibutuhkan dengan null-check
		 * menggunakan {@code jsonObject.isNull()}. Menghitung nilai bersih tagihan sebagai
		 * {@code Math.abs((penagihan + ppn%) - pinalti)}. Menampilkan semua kolom secara
		 * berurutan ke {@code arg0} (baris ZK). Tombol posting per baris menggunakan
		 * timer ZK untuk menunda eksekusi Hibernate satu siklus event.<br><br>
		 *
		 * <b>Parameter:</b><br>
		 * @param arg0 baris ZK ({@link Row}) yang akan diisi komponen-komponen UI
		 * @param arg1 data baris berupa {@code Object[]} dengan elemen
		 *             {@code [0]=JSONObject termin} dan {@code [1]=PemesananPengadaanMasterAsset}
		 * @throws Exception jika terjadi kesalahan parse JSON, akses Hibernate,
		 *                   atau manipulasi komponen ZK
		 *
		 * <b>Penanganan error:</b><br>
		 * Parse tanggal dari JSON menggunakan {@code Common.dateFormat1} dan dapat
		 * mengembalikan null jika format tidak sesuai; dalam hal ini tanggal pembuatan
		 * pemesanan digunakan sebagai fallback. Exception dari operasi Hibernate dalam
		 * event listener tombol ditangani oleh framework ZK.<br><br>
		 *
		 * <b>Pemeliharaan:</b><br>
		 * Penghapusan jurnal saat batalkan posting menggunakan SQL native dengan filter
		 * {@code closing is null}; data dengan closing tidak akan terhapus (dimaksudkan
		 * agar jurnal periode tutup tidak disentuh).
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			Object[] objects = (Object[]) arg1;
			final PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = (PemesananPengadaanMasterAsset) objects[1];
			JSONObject jsonObject = (JSONObject) objects[0];

			final String key;

			if (!jsonObject.isNull("key")) {
				key = jsonObject.get("key") + "";
			} else {
				key = "";
			}

			final String nama;

			if (!jsonObject.isNull("nama")) {
				nama = jsonObject.get("nama") + "";
			} else {
				nama = "";
			}

			final String nomor;

			if (!jsonObject.isNull("nomor")) {
				nomor = jsonObject.get("nomor") + "";
			} else {
				nomor = "";
			}

			Double pinalti = 0.0;
			if (!jsonObject.isNull("pinalti")) {
				pinalti = jsonObject.getDouble("pinalti");
			}
			final Double penagihan;
			if (!jsonObject.isNull("penagihan")) {
				penagihan = jsonObject.getDouble("penagihan");
			} else {
				penagihan = 0.0;
			}

			Double ppn = 0.0;
			if (!jsonObject.isNull("ppn")) {
				ppn = jsonObject.getDouble("ppn");
			}
			Double total = penagihan + ((ppn / 100.0) * penagihan);
			final Double nilai = Math.abs(total - pinalti);

			Date tanggalD = null;

			if (!jsonObject.isNull("tanggalD")) {
				tanggalD = jsonObject.get("tanggalD").toString().isEmpty() ? null
						: Common.dateFormat1.get().parse(jsonObject.get("tanggalD") + "");
			}

			Vbox aa;
			(aa = RevisiHelper.createNewRevisi(PemesananPengadaanMasterAsset.class, pemesananPengadaanMasterAsset,
					pemesananPengadaanMasterAsset.getKode() == null ? ""
							: pemesananPengadaanMasterAsset.getKode() + (nomor.isEmpty() ? "" : " " + nomor)))
					.setParent(arg0);
			aa.appendChild(new Label(nomor));
			aa.appendChild(new Label(nama));

			if (pemesananPengadaanMasterAsset.getDisposisiSop() != null) {
				A aaa;
				(aaa = new A()).setParent(aa);
				aaa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aaa, "SOP " + pemesananPengadaanMasterAsset.getDisposisiSop().getKeterangan() + " ("
						+ pemesananPengadaanMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aaa.addEventListener("onClick", new EventListener() {

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

			Vbox vbox = new Vbox();
			vbox.setWidth("95%");
			vbox.setAlign("right");
			vbox.setPack("end");
			new Label(Common.numberFormat.get().format(nilai)).setParent(vbox);
			if (pinalti > 0.1) {
				new Label("Pinalti " + Common.numberFormat.get().format(pinalti)).setParent(vbox);
			}

			new Label(Common.dateFormat3.get()
					.format(tanggalD != null ? tanggalD : pemesananPengadaanMasterAsset.getTanggalPembuatan()))
					.setParent(arg0);

			Akun akunDebet = pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset() == null ? null
					: pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset().getAkunUtangPekerjaan();

			/*
			 * Kredit memakai getAkunUtangDp() — DISELARASKAN dengan proses posting nyata
			 * (onPostingSemua & posting per-baris). Sebelumnya preview memakai getAkunDp()
			 * sehingga jurnal yang DIPRATINJAU bisa berbeda dari yang benar-benar diposting.
			 */
			Akun akunKredit = pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset() == null ? null
					: pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset().getAkunUtangDp();

			/*
			 * PENJAGA: akun debet & kredit TIDAK BOLEH SAMA. Bila master Jenis Pemesanan
			 * menyetel Akun Utang Pekerjaan (debet) = Akun Uang Muka/DP (kredit), jurnal balik
			 * menjadi Dr X / Cr X = bernilai NOL (netral, tak bermakna). Tandai "Transaksi tidak
			 * valid" agar tidak dianggap jurnal wajar dan tidak bisa diposting.
			 */
			if (akunDebet != null && akunKredit != null && !akunSama(akunDebet, akunKredit)) {

				List<Akun> akunsDebets = new ArrayList<Akun>();
				List<Akun> akunsKredits = new ArrayList<Akun>();

				List<Double> nilaiDebets = new ArrayList<Double>();
				List<Double> nilaiKredits = new ArrayList<Double>();

				akunsDebets.add(akunDebet);
				nilaiDebets.add(nilai);

				akunsKredits.add(akunKredit);
				nilaiKredits.add(nilai);

				GrupTransaksi.tampilkanJurnal(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits).setParent(arg0);

			} else {
				new Label("Transaksi tidak valid."
						+ (akunDebet != null ? " Debet: " + akunDebet.getKode() + "-" + akunDebet.getNama() + "."
								: " Akun debet tidak ada.")
						+ (akunKredit != null ? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
								: " Akun kredit tidak ada.")
						+ (akunDebet != null && akunKredit != null && akunSama(akunDebet, akunKredit)
								? " Akun debet & kredit SAMA — jurnal balik akan bernilai nol. Perbaiki master Jenis Pemesanan Pengadaan Asset: Akun Utang Pekerjaan (debet) harus berbeda dari Akun Utang DP/Uang Muka (kredit)."
								: ""))
						.setParent(arg0);
			}

			String bukti = "";
			Session session = HibernateUtil.currentSession();
			bukti = (String) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.eq("pemesananPengadaanMasterAsset", pemesananPengadaanMasterAsset))
					.add(Restrictions.eq("ref", key)).setMaxResults(1).setProjection(Projections.property("kode"))
					.uniqueResult();

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
												+ pemesananPengadaanMasterAsset.getId()
												+ " and ref = 'DP_BALIK_PEKERJAAN'" + " and closing is null")
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
				// Sembunyikan tombol Posting bila akun debet & kredit sama (jurnal nol tak valid).
				button.setVisible(edit && pemesananPengadaanMasterAsset.getPostingHistory() == null && tbmuser != null
						&& !akunSama(akunDebet, akunKredit));
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_TAGIHAN_DP_BALIK_PEKERJAAN);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunDebet = pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset() == null
										? null
										: pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset()
												.getAkunUtangPekerjaan();

								Akun akunKredit = pemesananPengadaanMasterAsset
										.getJenisPemesananPengadaanAsset() == null ? null
												: pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset()
														.getAkunUtangDp();

								// PENJAGA: jangan posting bila akun debet & kredit sama (jurnal nol tak valid).
								if (akunDebet != null && akunKredit != null && !akunSama(akunDebet, akunKredit)) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {

										ket = "Tagihan pekerjaan terhadap pemesanan \""
												+ (pemesananPengadaanMasterAsset.getKode() + "-" + nomor + "-" + nama
														+ " " + pemesananPengadaanMasterAsset.getKeterangan())
												+ "\" sebanyak " + Common.numberFormat.get()
														.format(pemesananPengadaanMasterAsset.getDptotal());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

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
												pemesananPengadaanMasterAsset.getTanggalPembuatan(), nilai, denda,
												pemesananPengadaanMasterAsset, satuanKerja, "DP_BALIK_PEKERJAAN",
												session);
									} else {
										CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket,
												pemesananPengadaanMasterAsset.getTanggalPembuatan(), nilai, denda,
												pemesananPengadaanMasterAsset, satuanKerja, "DP_BALIK_PEKERJAAN",
												session);
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
	 * <b>Tujuan:</b> Membangun objek {@link Criteria} Hibernate yang mencerminkan
	 * semua kondisi filter aktif pada halaman, digunakan baik untuk query data grid
	 * maupun untuk operasi posting/pembatalan massal.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat {@code Criteria} dari {@link PemesananPengadaanMasterAsset} pada sesi
	 * Hibernate saat ini. Filter yang diterapkan secara kondisional:
	 * <ul>
	 *   <li><b>Mode sudah_posting (dari URL):</b> Memfilter hanya pemesanan DP bertermin
	 *       yang disetujui, dalam rentang tanggal persetujuan, lalu membagi berdasarkan
	 *       status posting melalui {@code LEFT_JOIN} ke {@code postingHistory}.</li>
	 *   <li><b>Mode interaktif (filter manual):</b> Menerapkan checkbox
	 *       {@code searchtampil} (belum diposting) dan {@code searchtelahtampil}
	 *       (keduanya secara eksklusif), menambahkan filter standar DP bertermin
	 *       dengan rentang tanggal persetujuan.</li>
	 *   <li><b>Filter umum (keduanya):</b> Pemilik aset, lokasi, ruang, dan pencarian
	 *       teks pada kode/nama (ILIKE, case-insensitive).</li>
	 *   <li>Jika {@code order=true}, menambahkan urutan descending berdasarkan ID.</li>
	 * </ul>
	 *
	 * <b>Parameter:</b><br>
	 * @param order {@code true} untuk menambahkan {@code ORDER BY id DESC};
	 *              {@code false} untuk query tanpa urutan (digunakan untuk perhitungan)
	 * @return {@link Criteria} yang siap dieksekusi dengan {@code .list()} atau
	 *         {@code .setMaxResults().list()}
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika terjadi exception pada mode {@code sudah_posting} (misalnya format tanggal
	 * tidak valid di filter), exception ditangkap dan ditampilkan ke admin, lalu
	 * {@code Criteria} yang sudah terbentuk sebagian dikembalikan (bisa menghasilkan
	 * data yang tidak sesuai). Lebih baik memastikan filter tanggal selalu valid
	 * sebelum {@code initCriteria} dipanggil.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Filter tanggal menggunakan {@code sqlRestriction} dengan fungsi {@code date()}
	 * database; pastikan kompatibel dengan versi PostgreSQL yang digunakan.
	 * Jika tabel/kolom berubah, perbarui nama field Hibernate (bukan nama kolom DB)
	 * pada {@code Restrictions.eq} dan {@code Restrictions.ilike}.
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PemesananPengadaanMasterAsset.class);

		if (sudah_posting != null) {

			try {

				criteria.add(Restrictions.ilike("formula", "\"merupakan_dp\":true", MatchMode.ANYWHERE))
						/* Filter termin disetujui disamakan dengan hitungan dasbor draft jurnal. */
						.add(Restrictions.ilike("formula", "\"setuju\":true", MatchMode.ANYWHERE))
						.add(Restrictions.eq("byTermin", true))

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

			criteria.add(Restrictions.ilike("formula", "\"merupakan_dp\":true", MatchMode.ANYWHERE))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("byTermin", true))

					.add(Restrictions.isNotNull("disetujuiOleh"))

					.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(tanggal_persetujuan) between date('"
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
						// FIX (ERROR QueryException "could not resolve property: nama"):
						// PemesananPengadaanMasterAsset tidak punya field "nama", yang ada
						// "keterangan" -- salah nama properti (kemungkinan copy-paste dari
						// Action lain yang entity-nya punya field "nama").
						: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE),
								Restrictions.ilike("keterangan", searchkode.getValue(), MatchMode.ANYWHERE)));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Melakukan pencarian dan pembaruan data grid secara langsung
	 * tanpa menampilkan progress bar, dengan mengurai field {@code formula} JSON
	 * dari setiap pemesanan untuk mengekstrak termin-termin DP yang relevan.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code initCriteria(true).setMaxResults(1000).list()} untuk
	 *       mengambil maksimal 1000 pemesanan yang memenuhi filter.</li>
	 *   <li>Untuk setiap pemesanan, mengurai JSON array {@code formula} dan hanya
	 *       memasukkan termin yang memiliki {@code setuju:true} DAN
	 *       {@code merupakan_dp:true} ke dalam list hasil.</li>
	 *   <li>Setiap elemen hasil adalah {@code Object[]} berisi {@code [JSONObject, PemesananPengadaanMasterAsset]}.</li>
	 *   <li>Menetapkan model grid dengan {@code SimpleListModel}, renderer
	 *       {@link PemesananPengadaanMasterAssetRenderer}, mode paging 10 baris per halaman.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param event event ZK yang memicu pencarian, atau {@code null} jika dipanggil
	 *              secara programatik
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception pada parse JSON per termin dan per pemesanan diabaikan (blok catch kosong)
	 * agar termin/pemesanan lain tetap ditampilkan. Disarankan menambahkan logging.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Batas 1000 baris ({@code setMaxResults(1000)}) bersifat hardcoded; jika volume
	 * data besar, pertimbangkan untuk menambahkan paging server-side. Method ini
	 * dipanggil oleh {@code loadDataDenganProgressPosting} bukan langsung dari event ZUL.
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {

		List<PemesananPengadaanMasterAsset> pemesananPengadaanMasterAsset = initCriteria(true).setMaxResults(1000)
				.list();

		List<Object[]> objects = new ArrayList<Object[]>();
		for (PemesananPengadaanMasterAsset pemesananPengadaan : pemesananPengadaanMasterAsset) {
			try {
				JSONArray array = new JSONArray(pemesananPengadaan.getFormula());
				for (int i = 0; i < array.length(); i++) {
					try {

						JSONObject jsonObject = array.getJSONObject(i);

						if (jsonObject.isNull("key")) {
							continue;
						}

						Boolean setuju;
						if (!jsonObject.isNull("setuju")) {
							setuju = Boolean.parseBoolean(jsonObject.get("setuju") + "");
						} else {
							setuju = false;
						}

						Boolean merupakan_dp;
						if (!jsonObject.isNull("merupakan_dp")) {
							merupakan_dp = Boolean.parseBoolean(jsonObject.get("merupakan_dp") + "");
						} else {
							merupakan_dp = false;
						}

						if (setuju && merupakan_dp) {
							objects.add(new Object[] { jsonObject, pemesananPengadaan });
						}

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingJurnalBalikDpPemesananPekerjaanAction.java:1353");
						// exception pada parse termin individual diabaikan
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingJurnalBalikDpPemesananPekerjaanAction.java:1357");
				// exception pada parse formula pemesanan diabaikan
			}
		}

		ListModel strset = new SimpleListModel(objects);
		grid.setRowRenderer(new PemesananPengadaanMasterAssetRenderer());
		grid.setModelCheckMobile(strset);
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");

	}


	/**
	 * <b>Tujuan:</b> Event handler ZK yang dipanggil ketika pengguna mengklik tombol
	 * "Cari" atau ketika komponen filter mengirimkan event pencarian default.
	 * Bertindak sebagai delegator ke {@code loadDataDenganProgressPosting}.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Meneruskan event ke {@code loadDataDenganProgressPosting(event)} yang mengurus
	 * mekanisme progress bar dan anti-tumpang-tindih reload. Method ini diekspos
	 * sebagai event handler publik agar dapat dipanggil dari ZUL maupun dari kode
	 * lain yang membutuhkan refresh grid.<br><br>
	 *
	 * <b>Parameter:</b><br>
	 * @param event event ZK yang memicu pencarian, biasanya {@code onClick} atau
	 *              {@code onChange} dari komponen filter; boleh {@code null}
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai kembalian; efek samping adalah pembaruan tampilan grid.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika ada perubahan logika pencarian, ubah {@code onSearchDefaultTanpaProgress};
	 * method ini hanya sebagai titik masuk event yang tidak perlu diubah.
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Flag yang mencegah dua pemuatan data berjalan bersamaan.
	 * {@code true} berarti proses pemuatan sedang aktif; permintaan baru akan ditandai
	 * sebagai tertunda dan dieksekusi setelah proses saat ini selesai.
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag yang menandai ada permintaan reload baru yang masuk saat pemuatan sedang berlangsung.
	 * Setelah proses aktif selesai, jika flag ini {@code true}, pemuatan akan diulangi sekali lagi.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * <b>Tujuan:</b> Mengkoordinasikan pemuatan data grid dengan menampilkan progress bar
	 * informatif dan mencegah tumpang-tindih antara dua permintaan reload yang hampir
	 * bersamaan. Ini adalah method inti yang mengorkestrasi siklus muat-tampil-selesai
	 * untuk halaman posting jurnal balik DP.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Jika {@code postingJurnalLoadingAktif} sudah {@code true} (proses lain sedang
	 *       berjalan), menetapkan {@code postingJurnalReloadTertunda = true} dan menampilkan
	 *       notifikasi bahwa data akan dimuat ulang setelah proses selesai, lalu
	 *       langsung kembali.</li>
	 *   <li>Jika tidak ada proses aktif, mengatur flag dan menampilkan progress awal
	 *       di level 7%.</li>
	 *   <li>Menggunakan {@code Common.createDefaultTimer} untuk menunda satu siklus event
	 *       ZK sebelum benar-benar memanggil {@code onSearchDefaultTanpaProgress}.</li>
	 *   <li>Di dalam timer, memperbarui progress ke 48%, memanggil pencarian, lalu
	 *       memperbarui ke 92%.</li>
	 *   <li>Setelah selesai (blok {@code finally}), mereset flag dan:
	 *     <ul>
	 *       <li>Jika ada reload tertunda, menampilkan progress 96% dan menjalankan
	 *           reload baru melalui timer lagi.</li>
	 *       <li>Jika tidak ada, menampilkan progress selesai 100%.</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param event event ZK asal yang memicu pemuatan; diteruskan ke
	 *              {@code onSearchDefaultTanpaProgress}; boleh {@code null}
	 *
	 * <b>Penanganan error:</b><br>
	 * Blok {@code try-finally} memastikan flag {@code postingJurnalLoadingAktif}
	 * selalu direset meski terjadi exception selama pencarian. Exception dari
	 * {@code onSearchDefaultTanpaProgress} akan merambat ke framework ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Flag {@code postingJurnalLoadingAktif} dan {@code postingJurnalReloadTertunda}
	 * hanya aman diakses dari thread event ZK (single-threaded event dispatch).
	 * Jangan akses dari thread latar. Nilai persentase progress (7, 48, 92, 96, 100)
	 * bersifat estetik dan dapat disesuaikan tanpa dampak fungsional.
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
