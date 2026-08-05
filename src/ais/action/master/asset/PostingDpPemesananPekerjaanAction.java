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
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.PemilikAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
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
 * <h3>PostingDpPemesananPekerjaanAction</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan Action ZK (composer) yang mengelola seluruh alur kerja
 * posting jurnal akuntansi untuk transaksi uang muka (Down Payment / DP) pada
 * pemesanan pekerjaan atau Surat Perintah Kerja (SPK) dalam modul manajemen
 * aset. Proses posting mencakup pencatatan jurnal debet-kredit ke buku besar
 * akuntansi berdasarkan data tagihan pekerjaan yang telah disetujui, termasuk
 * perhitungan PPN, pinalti, dan penanganan pajak barang terkait. Halaman ini
 * memungkinkan operator keuangan untuk memposting satu per satu atau massal
 * (batch) seluruh transaksi DP pekerjaan yang memenuhi kriteria filter, serta
 * membatalkan posting yang sudah ada jika diperlukan koreksi.
 *
 * <b>Cara kerja:</b><br>
 * Composer ini di-wire ke halaman ZUL yang menampilkan daftar
 * {@link SaldoAwalMasterAsset} yang memiliki flag {@code merupakan_dp=true} dan
 * sudah mendapatkan persetujuan ({@code disetujuiOleh} tidak null). Saat
 * halaman dimuat, method {@code doAfterCompose} menginisialisasi semua
 * komponen UI (filter tanggal, combo pemilik, lokasi, ruang) dan langsung
 * memanggil {@code loadDataDenganProgressPosting} untuk mengisi grid. Setiap
 * baris grid dirender oleh inner class {@link SaldoAwalMasterAssetRenderer}
 * yang mengurai {@code jsonTermin} dari entitas untuk mendapatkan nilai
 * penagihan, PPN, pinalti, dan referensi akun jurnal. Posting per-baris
 * dilakukan melalui tombol "Posting Data" yang memanggil
 * {@link CommonAkunting#saveTransaksi} secara sinkron di sesi Hibernate yang
 * sedang aktif. Posting massal dilakukan di thread terpisah melalui dialog
 * konfirmasi yang dipanggil dari {@code onPostingSemua}.
 *
 * <b>Threading:</b><br>
 * Sebagian besar operasi berjalan di event-dispatch thread ZK. Khusus posting
 * massal ({@code onPostingSemua}), sebuah {@code Thread} baru (bukan
 * executor/pool) dijalankan untuk menghindari blokade UI selama iterasi
 * ratusan transaksi. Label progres diperbarui dari thread tersebut melalui
 * referensi {@code label} yang di-inject oleh
 * {@code Common.displayLoadBar}. Mekanisme coalescing reload
 * ({@code postingJurnalLoadingAktif} / {@code postingJurnalReloadTertunda})
 * menjamin tidak ada dua operasi muat data yang berjalan secara bersamaan,
 * sehingga aman untuk filter yang berubah-ubah saat data sedang diproses.
 *
 * <b>Pemeliharaan:</b><br>
 * Akun debet dan kredit diambil dari relasi
 * {@code penerimaanPengadaanMasterAsset -> pemesananPengadaanMasterAsset ->
 * jenisPemesananPengadaanAsset -> akunDp / akunUtangDp}. Jika penyedia
 * memiliki akun utang sendiri ({@code penyedia.akunUtang != null}), akun kredit
 * digantikan oleh akun utang penyedia. Blok kode lama yang menggunakan
 * {@code akunUtangPekerjaan} telah dikomentari dan tidak boleh diaktifkan
 * kembali tanpa tinjauan. Ref transaksi yang digunakan adalah string konstan
 * {@code "DP_PEKERJAAN"} yang juga digunakan pada query SQL untuk menghapus
 * jurnal saat pembatalan posting.
 */
public class PostingDpPemesananPekerjaanAction extends GenericAutowireComposer {

	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;

	private Textbox searchkode;
	private Combobox searchpemilikAsset;
	private Combobox searchlokasi;
	private AmbilDataRuangBanbox searchruang;

	private MyCheckboxConfig searchtampil;
	private MyCheckboxConfig searchtelahtampil;

	private boolean edit = false;

	private MyToolbarbuttonConfig sent;
	public boolean adminLain;

	private MyDatebox tglMulai;
	private MyDatebox tglSampai;

	private Tbmuser tbmuser;

	private North filter;
	private Row rowPosting;
	private Boolean sudah_posting = null;

	/**
	 * <b>Tujuan:</b><br>
	 * Dipanggil oleh framework ZK sebelum komponen halaman di-compose. Method ini
	 * bertanggung jawab untuk memeriksa keamanan sesi pengguna sebelum halaman
	 * dibangun, sehingga jika pengguna tidak berhak mengakses halaman ini, proses
	 * compose dapat dihentikan lebih awal tanpa membangun komponen yang tidak perlu.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@link Common#doCheckSecurity()} yang akan memeriksa apakah sesi
	 * pengguna aktif dan memiliki hak akses yang sesuai. Setelah itu melanjutkan
	 * ke implementasi super class untuk pemrosesan standar ZK. Jika keamanan
	 * gagal, framework ZK akan menghentikan proses dan mengarahkan pengguna ke
	 * halaman login atau menampilkan pesan akses ditolak.
	 *
	 * <b>Parameter:</b><br>
	 * @param page     Halaman ZK yang sedang di-compose.
	 * @param parent   Komponen induk di mana composer ini di-attach.
	 * @param compInfo Metadata komponen dari definisi ZUL.
	 *
	 * <b>Return:</b><br>
	 * @return {@link org.zkoss.zk.ui.metainfo.ComponentInfo} hasil dari super class,
	 *         digunakan oleh framework ZK untuk melanjutkan proses compose.
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika pemeriksaan keamanan gagal, method ini tidak melempar exception secara
	 * langsung; melainkan mengandalkan {@code Common.doCheckSecurity()} untuk
	 * menangani redirect atau tampilan error.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Method ini tidak boleh diubah kecuali ada perubahan mekanisme keamanan
	 * global. Selalu panggil {@code super.doBeforeCompose} agar pipeline ZK
	 * berjalan dengan benar.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Metode inisialisasi utama yang dipanggil ZK setelah seluruh komponen halaman
	 * selesai di-wire. Metode ini menyiapkan semua state awal: validasi sesi,
	 * pengambilan data pengguna, inisialisasi filter tanggal, pengisian combo
	 * pemilik aset dan lokasi, serta pemuatan data grid pertama kali.
	 *
	 * <b>Cara kerja:</b><br>
	 * Langkah pertama adalah memanggil {@code super.doAfterCompose(comp)} untuk
	 * menyelesaikan proses wire komponen ZK. Kemudian dilakukan validasi sesi
	 * melalui atribut {@code usersTemp} dan hak akses READ; jika tidak valid,
	 * pengguna diarahkan ke halaman logoff. Selanjutnya parameter URL
	 * {@code sudah_posting}, {@code mulai}, dan {@code sampai} dibaca untuk mode
	 * tampilan terbatas (filter disembunyikan, baris posting ditampilkan). Filter
	 * tanggal default diset ke 6 bulan terakhir. Combo {@code searchpemilikAsset}
	 * diisi dengan data {@link PemilikAsset} aktif, dan combo {@code searchlokasi}
	 * diisi dengan data {@link Lokasi} aktif. Jika ada kunci lokasi dari sesi,
	 * lokasi tersebut dipilih dan dinonaktifkan. Akhirnya metode
	 * {@code loadDataDenganProgressPosting} dipanggil untuk memuat data awal, dan
	 * {@link FilterLanjutHelper#setup(Component)} dipanggil untuk menyiapkan
	 * fitur filter lanjutan.
	 *
	 * <b>Parameter:</b><br>
	 * @param comp Komponen root ZK yang telah selesai di-compose dan di-wire.
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception saat parsing tanggal dari parameter URL ditangkap secara lokal
	 * dan ditampilkan hanya kepada pengguna admin melalui
	 * {@code Common.tampilErrorJikaAdmin(e)}. Ini menjamin halaman tetap bisa
	 * dimuat meskipun parameter URL memiliki format tanggal yang tidak valid.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika ada filter baru yang perlu diinisialisasi (misalnya combo satuan kerja),
	 * tambahkan setelah baris inisialisasi filter yang sudah ada. Pastikan semua
	 * komponen yang mungkin null (seperti {@code filter}, {@code rowPosting},
	 * {@code tglMulai}, {@code tglSampai}) di-guard sebelum diakses untuk
	 * menghindari NullPointerException saat halaman digunakan dalam mode embed.
	 *
	 * @throws Exception jika terjadi kesalahan fatal saat proses compose.
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
	 * <b>Tujuan:</b><br>
	 * Menangani event pembatalan posting untuk semua transaksi tagihan pekerjaan
	 * DP yang sudah diposting sebelumnya. Event ini dipicu oleh tombol
	 * "Batalkan Posting Semua" pada toolbar halaman.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menampilkan dialog konfirmasi kepada pengguna sebelum melakukan operasi
	 * massal. Jika pengguna menekan OK, sistem mengambil semua entitas
	 * {@link SaldoAwalMasterAsset} yang memiliki {@code postingHistory} tidak null
	 * menggunakan kriteria dari {@code initCriteria(true)}. Untuk setiap entitas
	 * tersebut, field {@code postingHistory} di-set ke null dan disimpan melalui
	 * {@code Common.refreshSaveOrUpdate}. Selain itu, semua record
	 * {@code akunting.grup_transaksi} dengan referensi {@code ref = 'DP_PEKERJAAN'}
	 * yang belum memiliki closing dihapus menggunakan native SQL query langsung
	 * ke database. Setelah semua pembatalan selesai, data grid dimuat ulang.
	 *
	 * <b>Parameter:</b><br>
	 * @param event Event ZK yang memicu pemanggilan method ini, biasanya event
	 *              onClick dari tombol toolbar pembatalan massal.
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * Tidak ada try-catch eksplisit di level method ini; exception yang tidak
	 * tertangkap akan disebarkan ke framework ZK untuk ditampilkan sebagai
	 * error dialog.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Query SQL pembatalan ({@code delete from akunting.grup_transaksi ...})
	 * menggunakan ID entitas secara langsung, sehingga aman untuk dijalankan
	 * per-entitas. Pastikan string konstan {@code 'DP_PEKERJAAN'} konsisten
	 * dengan yang digunakan di method posting lainnya. Pertimbangkan untuk
	 * menambahkan logging audit saat pembatalan massal dilakukan.
	 *
	 * @throws Exception jika terjadi kesalahan database atau kesalahan framework ZK.
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi tagihan pekerjaan ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<SaldoAwalMasterAsset> saldoAwalMasterAssets = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (SaldoAwalMasterAsset saldoAwalMasterAsset : saldoAwalMasterAssets) {
								saldoAwalMasterAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(saldoAwalMasterAsset);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where saldo_awal_master_asset="
														+ saldoAwalMasterAsset.getId() + " and ref = 'DP_PEKERJAAN'" + " and closing is null")
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
	 * <b>Tujuan:</b><br>
	 * Menangani event posting massal semua transaksi tagihan DP pekerjaan yang
	 * belum diposting. Method ini membuka dialog konfirmasi beserta form input
	 * tanggal posting dan keterangan sebelum memproses seluruh transaksi secara
	 * batch di thread terpisah.
	 *
	 * <b>Cara kerja:</b><br>
	 * Method membangun sebuah {@link MyWindow} modal yang berisi form input
	 * tanggal posting dan keterangan. Setelah pengguna menekan Simpan dan
	 * mengonfirmasi, sebuah {@link PostingHistory} baru dibuat dan disimpan ke
	 * database. Selanjutnya, semua {@link SaldoAwalMasterAsset} yang memenuhi
	 * kriteria (DP pekerjaan, sudah disetujui, belum diposting) diambil dari
	 * database. Untuk setiap entitas, {@code jsonTermin} diurai untuk mendapatkan:
	 * key transaksi, nama dan nomor pekerjaan, nilai penagihan, PPN, pinalti,
	 * dan jenis pajak barang. Akun debet diambil dari
	 * {@code jenisPemesananPengadaanAsset.akunDp} dan akun kredit dari
	 * {@code jenisPemesananPengadaanAsset.akunUtangDp}, kecuali jika penyedia
	 * memiliki akun utang sendiri yang menggantikan akun kredit. Jika ada pajak
	 * barang, transaksi diposting dengan dua akun kredit (akun pajak dan akun
	 * utang). Label progres diperbarui setiap iterasi. Setelah selesai, dialog
	 * ditutup dan data grid dimuat ulang. Semua ini berjalan di {@code Thread}
	 * terpisah untuk menghindari pembekuan UI.
	 *
	 * <b>Parameter:</b><br>
	 * @param event Event ZK yang memicu pemanggilan method ini, biasanya event
	 *              onClick dari tombol "Posting Semua" di toolbar halaman.
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception pada level individual transaksi ditangkap secara diam-diam
	 * ({@code catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingDpPemesananPekerjaanAction.java:397");}}) agar satu transaksi yang gagal tidak
	 * menghentikan pemrosesan transaksi lainnya. Exception pada parsing JSON
	 * juga ditangkap secara diam-diam. Sebaiknya tambahkan logging untuk
	 * keperluan audit dan debugging di masa mendatang.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Thread yang dijalankan memanggil {@code HibernateUtil.currentNativeSession()}
	 * untuk mendapatkan sesi Hibernate yang berdedikasi di luar sesi ZK biasa.
	 * Session harus ditutup di akhir melalui {@code HibernateUtil.closeSession()}.
	 * Nilai threshold {@code 0.1} digunakan untuk membedakan arah debet-kredit
	 * (positif vs negatif); jangan ubah tanpa memahami implikasi akuntansinya.
	 *
	 * @throws Exception jika terjadi kesalahan saat membangun komponen dialog ZK.
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
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting DP Tagihan Pekerjaan belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal dan pilih tanggal dari kalender; (2) Pastikan tanggal berada dalam periode akuntansi yang aktif; (3) ulangi proses posting ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
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
													PostingHistory.JENIS_TAGIHAN_DP_PEKERJAAN);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<SaldoAwalMasterAsset> saldoAwalMasterAssets = initCriteria(true)
													.list();

											int rowIndex = 1;
											for (SaldoAwalMasterAsset saldoAwalMasterAsset : saldoAwalMasterAssets) {

												SatuanKerja satuanKerja = (SatuanKerja) saldoAwalMasterAsset
														.getSatuanKerja();

												if (saldoAwalMasterAsset != null) {

													try {

														JSONObject jsonObject = new JSONObject(
																saldoAwalMasterAsset.getJsonTermin());
														try {

															if (jsonObject.isNull("key")) {
																continue;
															}

															Boolean setuju;
															if (!jsonObject.isNull("setuju")) {
																setuju = Boolean
																		.parseBoolean(jsonObject.get("setuju") + "");
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
																		.add(Restrictions.eq("saldoAwalMasterAsset",
																				saldoAwalMasterAsset))
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
																		penagihan = jsonObject.getDouble("penagihan");
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

																	final JenisPajakBarang jenisPajakBarang;
																	if (!jsonObject.isNull("pajak")) {
																		jenisPajakBarang = (JenisPajakBarang) ConstantValues
																				.ambil(JenisPajakBarang.class.getName(),
																						Long.parseLong(
																								jsonObject.get("pajak")
																										+ ""));
																	} else {
																		jenisPajakBarang = null;
																	}

																	try {

																		Akun akunKredit = saldoAwalMasterAsset
																				.getPenerimaanPengadaanMasterAsset()
																				.getPemesananPengadaanMasterAsset()
																				.getJenisPemesananPengadaanAsset() == null
																						? null
																						: saldoAwalMasterAsset
																								.getPenerimaanPengadaanMasterAsset()
																								.getPemesananPengadaanMasterAsset()
																								.getJenisPemesananPengadaanAsset()
																								.getAkunUtangDp();

																		Akun akunDebet = saldoAwalMasterAsset
																				.getPenerimaanPengadaanMasterAsset()
																				.getPemesananPengadaanMasterAsset()
																				.getJenisPemesananPengadaanAsset() == null
																						? null
																						: saldoAwalMasterAsset
																								.getPenerimaanPengadaanMasterAsset()
																								.getPemesananPengadaanMasterAsset()
																								.getJenisPemesananPengadaanAsset()
																								.getAkunDp();

																		if (saldoAwalMasterAsset != null
																				&& saldoAwalMasterAsset
																						.getPenyedia() != null
																				&& saldoAwalMasterAsset.getPenyedia()
																						.getAkunUtang() != null) {
																			akunKredit = saldoAwalMasterAsset
																					.getPenyedia().getAkunUtang();
																		}

																		Boolean apakahUangMasuk = true;
																		Akun akunDenda = null;
																		Akun akunPiutangDenda = null;
																		Double denda = 0.0;

																		String ket = "";
																		try {

																			ket = "Tagihan pekerjaan terhadap pemesanan \""
																					+ (saldoAwalMasterAsset.getKode()
																							+ "-" + nomor + "-" + nama
																							+ " "
																							+ saldoAwalMasterAsset
																									.getKeterangan())
																					+ "\" sebanyak "
																					+ Common.numberFormat.get().format(total);

																		} catch (Exception e) {
																			Common.tampilErrorJikaAdmin(e);
																		}

																		label.setValue(ket + " (" + Common.numberFormat.get()
																				.format(rowIndex * 100.0
																						/ saldoAwalMasterAssets.size())
																				+ " %)");

																		if (jenisPajakBarang != null
																				&& jenisPajakBarang.getAkun() != null) {

																			Double nilaiPajak = (penagihan
																					* (jenisPajakBarang.getPersen()
																							/ 100.0));

																			session.getTransaction().begin();
																			CommonAkunting.saveTransaksi(
																					new Akun[] { akunDebet },
																					new Akun[] { akunKredit,
																							jenisPajakBarang
																									.getAkun() },
																					akunDenda, akunPiutangDenda,
																					postingHistory, apakahUangMasuk,
																					ket,
																					saldoAwalMasterAsset
																							.getTanggalPembuatan(),
																					new Double[] { nilai },
																					new Double[] { nilai - nilaiPajak,
																							nilaiPajak },
																					denda, saldoAwalMasterAsset,
																					satuanKerja, "DP_PEKERJAAN",
																					session);
																			session.getTransaction().commit();

																		} else {

																			if (akunDebet != null
																					&& akunKredit != null) {

																				try {

																					session.getTransaction().begin();
																					if (nilai > 0.1) {
																						CommonAkunting.saveTransaksi(
																								akunDebet, akunKredit,
																								akunDenda,
																								akunPiutangDenda,
																								postingHistory,
																								apakahUangMasuk, ket,
																								saldoAwalMasterAsset
																										.getTanggalPembuatan(),
																								nilai, denda,
																								saldoAwalMasterAsset,
																								satuanKerja,
																								"DP_PEKERJAAN",
																								session);
																					} else {
																						CommonAkunting.saveTransaksi(
																								akunKredit, akunDebet,
																								akunDenda,
																								akunPiutangDenda,
																								postingHistory,
																								apakahUangMasuk, ket,
																								saldoAwalMasterAsset
																										.getTanggalPembuatan(),
																								nilai, denda,
																								saldoAwalMasterAsset,
																								satuanKerja,
																								"DP_PEKERJAAN",
																								session);
																					}
																					session.getTransaction().commit();
																				} catch (Exception e) {
																					Common.tampilErrorJikaAdmin(e);
																				}
																			}

																			saldoAwalMasterAsset
																					.setPostingHistory(postingHistory);
																			session.getTransaction().begin();
																			session.update(saldoAwalMasterAsset);
																			session.getTransaction().commit();
																		}
																	} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingDpPemesananPekerjaanAction.java:777");
																		// tangani kesalahan per-transaksi
																	}

																}
																rowIndex++;
															}
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingDpPemesananPekerjaanAction.java:784");
															// tangani kesalahan parsing JSON termin
														}

													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingDpPemesananPekerjaanAction.java:788");
														// tangani kesalahan akses entitas
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
	 * <b>Tujuan:</b><br>
	 * Inner class renderer yang bertanggung jawab untuk merender setiap baris
	 * pada grid daftar tagihan DP pekerjaan. Setiap baris menampilkan informasi
	 * lengkap tentang satu entri tagihan beserta tampilan jurnal debet-kredit
	 * dan tombol aksi posting / batalkan posting.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menerima {@code Object[]} berisi pasangan {@link JSONObject} (data termin
	 * dari {@code jsonTermin}) dan {@link SaldoAwalMasterAsset}. Renderer mengurai
	 * semua field dari JSONObject (key, nama, nomor, pinalti, penagihan, PPN,
	 * tanggal, pajak) dan membangun tampilan baris dengan: nomor/kode pekerjaan,
	 * nama penyedia, jenis pemesanan, nilai bersih, tanggal, tampilan jurnal
	 * via {@link GrupTransaksi#tampilkanJurnal}, status posting, dan tombol aksi.
	 * Akun debet dan kredit ditentukan dari relasi yang sama dengan proses
	 * posting. Tombol "Batalkan Posting" hanya muncul jika {@code adminLain} dan
	 * posting history tidak null. Tombol "Posting Data" hanya muncul jika belum
	 * diposting dan akun debet-kredit valid.
	 *
	 * <b>Parameter:</b><br>
	 * @param arg0 Baris ZK ({@link Row}) yang akan diisi komponen.
	 * @param arg1 Data baris berupa {@code Object[]} dengan index 0 adalah
	 *             {@link JSONObject} termin dan index 1 adalah
	 *             {@link SaldoAwalMasterAsset}.
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika akun debet atau kredit null, baris menampilkan pesan teks yang
	 * menjelaskan akun mana yang tidak tersedia, alih-alih menampilkan jurnal.
	 * Ini memudahkan operator untuk mengetahui konfigurasi akun mana yang
	 * perlu dilengkapi.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Renderer ini dipasang di {@code onSearchDefaultTanpaProgress} melalui
	 * {@code grid.setRowRenderer(new SaldoAwalMasterAssetRenderer())}. Jika ada
	 * kolom baru yang perlu ditambahkan ke grid, tambahkan juga definisi kolom
	 * di file ZUL yang berpasangan dengan halaman ini.
	 */
	class SaldoAwalMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b><br>
		 * Merender satu baris data tagihan DP pekerjaan ke dalam komponen ZK Row,
		 * meliputi seluruh informasi tagihan, pratinjau jurnal akuntansi, status
		 * posting, dan tombol aksi per-baris.
		 *
		 * <b>Cara kerja:</b><br>
		 * Mengurai {@code Object[]} menjadi {@link JSONObject} dan
		 * {@link SaldoAwalMasterAsset}, lalu mengekstrak semua field yang diperlukan.
		 * Nilai bersih dihitung sebagai {@code Math.abs((penagihan + PPN) - pinalti)}.
		 * Tampilan jurnal dibangun dengan {@link GrupTransaksi#tampilkanJurnal}
		 * yang menerima daftar akun debet dan kredit beserta nilainya. Tombol
		 * posting individual melakukan proses yang sama dengan {@code onPostingSemua}
		 * tetapi hanya untuk satu transaksi, menggunakan sesi Hibernate biasa
		 * (bukan native session) karena berjalan di event-dispatch thread.
		 *
		 * <b>Parameter:</b><br>
		 * @param arg0 Baris ZK target untuk menempatkan komponen-komponen UI.
		 * @param arg1 Data berupa {@code Object[]} dengan dua elemen:
		 *             elemen pertama adalah {@link JSONObject} termin,
		 *             elemen kedua adalah {@link SaldoAwalMasterAsset}.
		 *
		 * <b>Return:</b><br>
		 * Tidak ada nilai balik (void).
		 *
		 * <b>Penanganan error:</b><br>
		 * Jika akun debet atau kredit null (konfigurasi belum lengkap), baris
		 * menampilkan label peringatan yang menjelaskan akun mana yang hilang.
		 *
		 * <b>Pemeliharaan:</b><br>
		 * Perhatikan bahwa data no. bukti jurnal diambil dengan query Hibernate
		 * langsung di dalam render, sehingga setiap baris menghasilkan satu
		 * tambahan query ke database. Untuk optimasi performa pada dataset besar,
		 * pertimbangkan untuk memuat data ini secara batch sebelum rendering.
		 *
		 * @throws Exception jika terjadi kesalahan saat parsing JSON atau akses database.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			Object[] objects = (Object[]) arg1;
			final SaldoAwalMasterAsset saldoAwalMasterAsset = (SaldoAwalMasterAsset) objects[1];
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

			final JenisPajakBarang jenisPajakBarang;
			if (!jsonObject.isNull("pajak")) {
				jenisPajakBarang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
						Long.parseLong(jsonObject.get("pajak") + ""));
			} else {
				jenisPajakBarang = null;
			}

			Vbox aa;
			(aa = RevisiHelper
					.createNewRevisi(SaldoAwalMasterAsset.class, saldoAwalMasterAsset,
							saldoAwalMasterAsset.getKode() == null ? ""
									: saldoAwalMasterAsset.getKode() + (nomor.isEmpty() ? "" : " " + nomor)))
					.setParent(arg0);
			aa.appendChild(new Label(nomor));
			aa.appendChild(new Label(nama));

			if (saldoAwalMasterAsset.getDisposisiSop() != null) {
				A aaa;
				(aaa = new A()).setParent(aa);
				aaa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aaa, "SOP " + saldoAwalMasterAsset.getDisposisiSop().getKeterangan() + " ("
						+ saldoAwalMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aaa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(saldoAwalMasterAsset.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			new Label(saldoAwalMasterAsset.getPenyedia() == null ? "" : saldoAwalMasterAsset.getPenyedia().getNama())
					.setParent(arg0);

			new Label(saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()
					.getJenisPemesananPengadaanAsset() == null
							? ""
							: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
									.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset().getNama())
					.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setWidth("95%");
			vbox.setAlign("right");
			vbox.setPack("end");
			new Label(Common.numberFormat.get().format(nilai)).setParent(vbox);
			if (pinalti > 0.1) {
				new Label("Pinalti " + Common.numberFormat.get().format(pinalti)).setParent(vbox);
			}

			new Label(
					Common.dateFormat3.get().format(tanggalD != null ? tanggalD : saldoAwalMasterAsset.getTanggalPembuatan()))
					.setParent(arg0);

			Akun akunKredit = saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
					.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset() == null
							? null
							: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
									.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset()
									.getAkunUtangDp();

			Akun akunDebet = saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()
					.getJenisPemesananPengadaanAsset() == null ? null
							: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
									.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset().getAkunDp();

			if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getPenyedia() != null
					&& saldoAwalMasterAsset.getPenyedia().getAkunUtang() != null) {
				akunKredit = saldoAwalMasterAsset.getPenyedia().getAkunUtang();
			}

			if (akunDebet != null && akunKredit != null) {

				List<Akun> akunsDebets = new ArrayList<Akun>();
				List<Akun> akunsKredits = new ArrayList<Akun>();

				List<Double> nilaiDebets = new ArrayList<Double>();
				List<Double> nilaiKredits = new ArrayList<Double>();

				akunsDebets.add(akunDebet);
				nilaiDebets.add(nilai);

				if (jenisPajakBarang != null && jenisPajakBarang.getAkun() != null) {

					Double nilaiPajak = (penagihan * (jenisPajakBarang.getPersen() / 100.0));

					if (nilaiPajak > 0.01) {
						akunsKredits.add(jenisPajakBarang.getAkun());
						nilaiKredits.add(nilaiPajak);
					}
					akunsKredits.add(akunKredit);
					nilaiKredits.add(nilai - nilaiPajak);

				} else {

					akunsKredits.add(akunKredit);
					nilaiKredits.add(nilai);

				}

				GrupTransaksi.tampilkanJurnal(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits).setParent(arg0);

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
					.add(Restrictions.eq("saldoAwalMasterAsset", saldoAwalMasterAsset)).add(Restrictions.eq("ref", key))
					.setMaxResults(1).setProjection(Projections.property("kode")).uniqueResult();

			new Label(saldoAwalMasterAsset.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: saldoAwalMasterAsset.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && saldoAwalMasterAsset.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								saldoAwalMasterAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(saldoAwalMasterAsset);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where saldo_awal_master_asset="
														+ saldoAwalMasterAsset.getId() + " and ref = 'DP_PEKERJAAN'" + " and closing is null")
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
				button.setVisible(edit && saldoAwalMasterAsset.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_TAGIHAN_DP_PEKERJAAN);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunKredit = saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
										.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset() == null
												? null
												: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
														.getPemesananPengadaanMasterAsset()
														.getJenisPemesananPengadaanAsset().getAkunUtangDp();

								Akun akunDebet = saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
										.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset() == null
												? null
												: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
														.getPemesananPengadaanMasterAsset()
														.getJenisPemesananPengadaanAsset().getAkunDp();

								if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getPenyedia() != null
										&& saldoAwalMasterAsset.getPenyedia().getAkunUtang() != null) {
									akunKredit = saldoAwalMasterAsset.getPenyedia().getAkunUtang();
								}

								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {

										ket = "Tagihan DP pekerjaan terhadap pemesanan \""
												+ (saldoAwalMasterAsset.getKode() + "-" + nomor + "-" + nama + " "
														+ saldoAwalMasterAsset.getKeterangan());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (saldoAwalMasterAsset != null
											&& saldoAwalMasterAsset.getSatuanKerja() != null
													? saldoAwalMasterAsset.getSatuanKerja()
													: tbmuser.ambilSatuanKerja());

									if (tbmuser.ambilSatuanKerja() != null) {
										satuanKerja = tbmuser.ambilSatuanKerja();
									}

									if (jenisPajakBarang != null && jenisPajakBarang.getAkun() != null) {

										Double nilaiPajak = (penagihan * (jenisPajakBarang.getPersen() / 100.0));

										CommonAkunting.saveTransaksi(new Akun[] { akunDebet },
												new Akun[] { akunKredit, jenisPajakBarang.getAkun() }, akunDenda,
												akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
												saldoAwalMasterAsset.getTanggalPembuatan(), new Double[] { nilai },
												new Double[] { nilai - nilaiPajak, nilaiPajak }, denda,
												saldoAwalMasterAsset, satuanKerja, "DP_PEKERJAAN", session);

									} else {
										if (nilai > 0.1) {
											CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda,
													akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
													saldoAwalMasterAsset.getTanggalPembuatan(), nilai, denda,
													saldoAwalMasterAsset, satuanKerja, "DP_PEKERJAAN", session);
										} else {
											CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda,
													akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
													saldoAwalMasterAsset.getTanggalPembuatan(), nilai, denda,
													saldoAwalMasterAsset, satuanKerja, "DP_PEKERJAAN", session);
										}
									}

									saldoAwalMasterAsset.setPostingHistory(postingHistory);
									session.update(saldoAwalMasterAsset);
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
	 * <b>Tujuan:</b><br>
	 * Membangun dan mengembalikan objek {@link Criteria} Hibernate yang digunakan
	 * untuk mengambil data {@link SaldoAwalMasterAsset} sesuai dengan filter yang
	 * aktif di halaman. Criteria ini digunakan baik untuk pengambilan data grid
	 * maupun untuk proses posting dan pembatalan massal.
	 *
	 * <b>Cara kerja:</b><br>
	 * Jika parameter {@code sudah_posting} (dari URL) tidak null, criteria
	 * dibangun dalam mode terbatas: hanya memfilter berdasarkan tanggal persetujuan
	 * dan status posting (sudah/belum). Jika null (mode normal), criteria
	 * mempertimbangkan checkbox {@code searchtampil} dan {@code searchtelahtampil}
	 * untuk memfilter status posting. Dalam kedua mode, filter tambahan diterapkan:
	 * pemilik aset, lokasi, ruang, dan kode/keterangan termin (pencarian teks).
	 * Filter wajib yang selalu diterapkan adalah: harus ada
	 * {@code penerimaanPengadaanMasterAsset} (bukan null), JSON termin harus
	 * mengandung {@code "merupakan_dp":true}, harus sudah disetujui, dan nilai
	 * tidak boleh nol. Jika {@code order} true, hasil diurutkan secara descending
	 * berdasarkan ID.
	 *
	 * <b>Parameter:</b><br>
	 * @param order Jika {@code true}, menambahkan ORDER BY id DESC pada criteria.
	 *              Biasanya {@code true} untuk tampilan grid, {@code false} untuk
	 *              penghitungan jumlah halaman.
	 *
	 * <b>Return:</b><br>
	 * @return Objek {@link Criteria} yang siap dieksekusi untuk mengambil daftar
	 *         {@link SaldoAwalMasterAsset}.
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dalam blok mode terbatas ({@code sudah_posting != null}) ditangkap
	 * dan ditampilkan kepada admin melalui {@code Common.tampilErrorJikaAdmin(e)}.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Filter {@code ilike("jsonTermin", "\"merupakan_dp\":true", ANYWHERE)}
	 * bergantung pada format string JSON yang dihasilkan saat penyimpanan.
	 * Jika format JSON berubah (misalnya ada spasi setelah titik dua), filter
	 * ini akan gagal. Pertimbangkan untuk menambahkan kolom boolean terpisah
	 * di database untuk flag ini.
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(SaldoAwalMasterAsset.class);

		if (sudah_posting != null) {

			try {

				criteria.add(Restrictions.isNotNull("penerimaanPengadaanMasterAsset"))
						.add(Restrictions.ilike("jsonTermin", "\"merupakan_dp\":true", MatchMode.ANYWHERE))
						.add(Restrictions.isNotNull("jsonTermin")).add(Restrictions.isNotNull("disetujuiOleh"))
						/* Filter nilai disamakan dengan hitungan dasbor draft jurnal. */
						.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
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

			criteria.add(Restrictions.isNotNull("penerimaanPengadaanMasterAsset"))
					.add(Restrictions.ilike("jsonTermin", "\"merupakan_dp\":true", MatchMode.ANYWHERE))
					.add(Restrictions.isNotNull("jsonTermin"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.add(Restrictions.isNotNull("disetujuiOleh"))

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
								Restrictions.ilike("keteranganTermin", searchkode.getValue(), MatchMode.ANYWHERE)));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Mengambil data dari database sesuai criteria yang aktif, memfilter entri
	 * yang benar-benar merupakan DP (flag {@code merupakan_dp=true} dari JSON),
	 * dan mengisi model data grid tanpa menampilkan indikator progres.
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengambil maksimal 1000 data {@link SaldoAwalMasterAsset} dari database.
	 * Untuk setiap entitas, mengurai {@code jsonTermin} dan memeriksa flag
	 * {@code merupakan_dp}. Hanya entitas dengan flag {@code true} yang
	 * dimasukkan ke dalam daftar model sebagai {@code Object[]} berisi pasangan
	 * JSONObject dan SaldoAwalMasterAsset. Model kemudian diset ke grid dengan
	 * renderer {@link SaldoAwalMasterAssetRenderer}, mode paging "os" dengan
	 * ukuran halaman 10.
	 *
	 * <b>Parameter:</b><br>
	 * @param event Event ZK yang memicu pencarian (bisa null jika dipanggil
	 *              secara programatik).
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception saat parsing JSON ditangkap dan ditampilkan kepada admin
	 * melalui {@code Common.tampilErrorJikaAdmin(e)} agar entri yang rusak
	 * tidak menghentikan pemrosesan entri lainnya.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Batas 1000 baris ({@code setMaxResults(1000)}) adalah nilai tetap.
	 * Jika jumlah data DP pekerjaan melebihi 1000 dalam satu periode filter,
	 * data akan terpotong. Pertimbangkan untuk menambahkan paging server-side
	 * atau mengurangi periode filter default.
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {

		List<SaldoAwalMasterAsset> saldoAwalMasterAsset = initCriteria(true).setMaxResults(1000).list();

		List<Object[]> objects = new ArrayList<Object[]>();
		for (SaldoAwalMasterAsset pemesananPengadaan : saldoAwalMasterAsset) {
			try {

				JSONObject jsonObject = new JSONObject(pemesananPengadaan.getJsonTermin());

				if (jsonObject.isNull("key")) {
					continue;
				}

				Boolean merupakan_dp;
				if (!jsonObject.isNull("merupakan_dp")) {
					merupakan_dp = Boolean.parseBoolean(jsonObject.get("merupakan_dp") + "");
				} else {
					merupakan_dp = false;
				}

				if (merupakan_dp) {
					objects.add(new Object[] { jsonObject, pemesananPengadaan });
				}

			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		ListModel strset = new SimpleListModel(objects);
		grid.setRowRenderer(new SaldoAwalMasterAssetRenderer());
		grid.setModelCheckMobile(strset);
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");

	}

	/**
	 * <b>Tujuan:</b><br>
	 * Method event handler publik yang dipanggil oleh framework ZK saat terjadi
	 * event pencarian default dari toolbar filter (misalnya klik tombol Cari
	 * atau perubahan filter). Method ini mendelegasikan ke
	 * {@code loadDataDenganProgressPosting} untuk memuat data dengan tampilan
	 * indikator progres.
	 *
	 * <b>Cara kerja:</b><br>
	 * Merupakan delegator satu-baris ke
	 * {@link #loadDataDenganProgressPosting(Event)}. Keberadaan method publik
	 * ini diperlukan agar framework ZK dapat me-wire event {@code onSearchDefault}
	 * dari tombol di ZUL ke method ini secara otomatis melalui konvensi penamaan
	 * ZK ({@code on + NamaEvent}).
	 *
	 * <b>Parameter:</b><br>
	 * @param event Event ZK yang memicu pencarian, biasanya event onClick dari
	 *              tombol cari atau event onChange dari komponen filter.
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception akan disebarkan ke pemanggil dan ditangani oleh framework ZK.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jangan tambahkan logika bisnis di sini; selalu delegasikan ke
	 * {@code loadDataDenganProgressPosting} agar mekanisme coalescing reload
	 * tetap bekerja dengan benar.
	 *
	 * @throws Exception jika terjadi kesalahan yang tidak tertangkap di dalam
	 *                   {@code loadDataDenganProgressPosting}.
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/** Flag untuk mencegah dua proses load data posting berjalan bersamaan. */
	private boolean postingJurnalLoadingAktif = false;

	/** Flag penanda bahwa ada permintaan reload yang tertunda saat loading aktif. */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * <b>Tujuan:</b><br>
	 * Memuat data posting jurnal ke dalam grid dengan menampilkan indikator
	 * progres bertahap kepada pengguna. Method ini mengimplementasikan mekanisme
	 * coalescing untuk mencegah dua operasi load berjalan secara bersamaan,
	 * sambil tetap memproses permintaan reload yang datang selama loading sedang
	 * berlangsung.
	 *
	 * <b>Cara kerja:</b><br>
	 * Jika {@code postingJurnalLoadingAktif} sudah true, method menandai
	 * {@code postingJurnalReloadTertunda = true} dan menampilkan notifikasi
	 * kepada pengguna bahwa reload akan dilakukan setelah proses saat ini
	 * selesai. Jika tidak sedang loading, flag diset ke true, indikator progres
	 * 7% ditampilkan, dan {@code Common.createDefaultTimer} dipanggil untuk
	 * menjalankan loading secara asinkron di timer ZK berikutnya.
	 * Di dalam timer: progres diperbarui ke 48%, {@code onSearchDefaultTanpaProgress}
	 * dipanggil untuk memuat data, lalu progres diperbarui ke 92%. Di blok
	 * {@code finally}, flag direset dan jika ada reload tertunda, proses dimulai
	 * kembali secara rekursif. Jika tidak ada reload tertunda, indikator
	 * dinyatakan selesai (100%).
	 *
	 * <b>Parameter:</b><br>
	 * @param event Event ZK yang memicu pemuatan data (bisa null). Event ini
	 *              akan diteruskan ke {@code onSearchDefaultTanpaProgress} dan
	 *              ke pemanggilan rekursif jika ada reload tertunda.
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * Blok {@code finally} memastikan flag {@code postingJurnalLoadingAktif}
	 * selalu direset meskipun terjadi exception di dalam {@code onSearchDefaultTanpaProgress}.
	 * Ini mencegah halaman "terkunci" dan tidak bisa dimuat ulang.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Method ini tidak thread-safe dalam arti sesungguhnya karena berjalan
	 * sepenuhnya di event-dispatch thread ZK (single-threaded per sesi). Flag
	 * {@code postingJurnalLoadingAktif} tidak perlu dideklarasikan sebagai
	 * {@code volatile} karena tidak diakses dari thread lain. Nilai progres
	 * (7, 48, 92, 96, 100) adalah estimasi yang tidak memiliki arti teknis;
	 * ubah sesuai kebutuhan UX.
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
