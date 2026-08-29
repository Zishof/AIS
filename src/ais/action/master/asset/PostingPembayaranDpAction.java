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

import ais.action.master.akunting.ProsesTransferAction;
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
import ais.database.model.asset.PembayaranDpMasterAsset;
import ais.database.model.asset.PembayaranDpMasterAssetDetail;
import ais.database.model.asset.PemilikAsset;
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
 * <h3>PostingPembayaranDpAction — Posting Jurnal Akuntansi Pembayaran Uang Muka (DP) Pengadaan Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan Action (Composer ZK) yang mengelola proses posting jurnal akuntansi untuk
 * transaksi pembayaran uang muka (DP / Down Payment) dalam modul pengadaan aset. Posting jurnal
 * berarti mencatat transaksi pembayaran DP ke dalam buku besar akuntansi (tabel grup_transaksi)
 * dengan pasangan akun debet dan kredit yang sesuai. Kelas ini juga menyediakan fitur pembatalan
 * posting (reversal) baik satu per satu maupun secara massal. Halaman ini dapat diakses oleh
 * pengguna dengan hak akses UPDATE dan APPROVE pada modul aset.
 *
 * <b>Cara kerja:</b><br>
 * Setelah halaman ZUL dimuat, metode {@link #doAfterCompose(Component)} menginisialisasi filter
 * pencarian (tanggal, pemilik aset, ruang, status posting) dan memuat data grid menggunakan
 * {@link #loadDataDenganProgressPosting(Event)}. Grid menampilkan daftar {@link PembayaranDpMasterAssetDetail}
 * yang sudah disetujui dan memiliki nilai DP lebih dari nol. Setiap baris grid dirender oleh inner
 * class {@link PembayaranDpMasterAssetRenderer} yang menampilkan pasangan akun jurnal debet/kredit
 * beserta tombol aksi posting dan pembatalan posting per baris. Tombol "Posting Semua" membuka dialog
 * konfirmasi dan menjalankan proses posting massal di latar belakang menggunakan thread terpisah
 * agar antarmuka pengguna tetap responsif. Akun debet diambil dari jenis pemesanan pengadaan aset
 * (getAkunDp), sedangkan akun kredit ditentukan berdasarkan jenis pembayaran barang, cara pembayaran
 * transfer, atau akun transitori sesuai kondisi transfer.
 *
 * <b>Threading:</b><br>
 * Proses posting massal berjalan di dalam {@code new Thread(Runnable).start()} sehingga tidak
 * memblokir event thread ZK. State loading dikelola dengan flag {@code postingJurnalLoadingAktif}
 * dan {@code postingJurnalReloadTertunda} untuk mencegah reload ganda yang tidak perlu. Semua
 * operasi basis data menggunakan sesi Hibernate dedikasi yang dibuka dan ditutup secara eksplisit
 * di dalam thread latar belakang. Update UI setelah thread selesai dilakukan melalui
 * {@code Common.displayLoadBar} dengan callback event listener.
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas ini bergantung pada konfigurasi akun di entitas {@link ais.database.model.asset.JenisPemesananPengadaanAsset}
 * (akunDp) dan {@link ais.database.model.akunting.JenisPembayaranBarang} (akun). Jika struktur akun
 * berubah, pastikan logika resolusi akun di {@link #onPostingSemua(Event)} dan
 * {@link PembayaranDpMasterAssetRenderer#render(Row, Object)} juga diperbarui. Filter tanggal
 * menggunakan format SQL langsung (sqlRestriction) sehingga harus disesuaikan jika skema basis data
 * berubah. Tambahkan indeks pada kolom tanggal_transaksi dan posting_history di tabel
 * pembayaran_dp_master_asset_detail untuk performa optimal pada data besar.
 */
public class PostingPembayaranDpAction extends GenericAutowireComposer {

	/**
	 * Serial version UID untuk serialisasi kelas ini.
	 * Nilai ini dibangkitkan secara otomatis dan tidak boleh diubah sembarangan
	 * karena dapat menyebabkan inkompatibilitas deserialisasi antar versi aplikasi.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar transaksi pembayaran DP yang siap atau sudah diposting. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman pada grid data pembayaran DP. */
	private Paging paging;

	/** Textbox pencarian berdasarkan kode atau nama pembayaran DP master aset. */
	private Textbox searchkode;

	/** Combobox filter pemilik aset untuk mempersempit hasil pencarian. */
	private Combobox searchpemilikAsset;

	/** Banbox pencarian berdasarkan ruang/lokasi fisik aset yang bersangkutan. */
	private AmbilDataRuangBanbox searchruang;

	/** Checkbox filter untuk menampilkan hanya transaksi yang belum diposting. */
	private MyCheckboxConfig searchtampil;

	/** Checkbox filter untuk menampilkan hanya transaksi yang sudah diposting. */
	private MyCheckboxConfig searchtelahtampil;

	/** Flag yang menandakan apakah pengguna saat ini memiliki hak akses UPDATE. */
	private boolean edit = false;

	/** Tombol toolbar "Kirim" yang hanya ditampilkan jika pengguna memiliki hak UPDATE. */
	private MyToolbarbuttonConfig sent;

	/** Flag yang menandakan apakah pengguna adalah admin atau memiliki hak APPROVE. */
	public boolean adminLain;

	/** Datebox untuk filter tanggal awal rentang pencarian transaksi. */
	private MyDatebox tglMulai;

	/** Datebox untuk filter tanggal akhir rentang pencarian transaksi. */
	private MyDatebox tglSampai;

	/** Data pengguna yang sedang login, digunakan untuk otorisasi tombol posting. */
	private Tbmuser tbmuser;

	/**
	 * <h3>doBeforeCompose — Pemeriksaan Keamanan Sebelum Halaman Dimuat</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Metode ini dipanggil oleh framework ZK sebelum proses wiring komponen ZUL ke Action dilakukan.
	 * Fungsinya adalah melakukan pemeriksaan keamanan awal untuk memastikan halaman hanya dapat
	 * diakses oleh pengguna yang berwenang.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@link Common#doCheckSecurity()} yang akan memeriksa apakah sesi pengguna valid
	 * dan pengguna memiliki hak akses ke halaman ini. Jika tidak berwenang, pengguna akan
	 * diarahkan ke halaman login atau halaman error. Setelah pemeriksaan, delegasi ke implementasi
	 * superclass untuk melanjutkan proses komposisi halaman secara normal.
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika pemeriksaan keamanan gagal, {@link Common#doCheckSecurity()} akan menghentikan proses
	 * pemuatan halaman dengan redirect atau exception. Tidak ada penanganan exception tambahan di
	 * metode ini karena keamanan dikelola sepenuhnya oleh Common.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Metode ini adalah standar keamanan yang digunakan di semua Action halaman AIS. Jangan hapus
	 * atau modifikasi pemeriksaan keamanan ini tanpa konsultasi dengan tim arsitektur.
	 *
	 * @param page     halaman ZK yang sedang dikomposisi
	 * @param parent   komponen induk tempat halaman ini akan dipasang
	 * @param compInfo informasi metadata komponen dari file ZUL
	 * @return informasi komponen hasil dari superclass untuk dilanjutkan ke proses wiring
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <h3>doAfterCompose — Inisialisasi Halaman Posting Pembayaran DP Setelah Wiring Selesai</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Metode ini dipanggil secara otomatis oleh framework ZK setelah seluruh komponen ZUL
	 * berhasil di-wire ke field-field kelas ini. Metode ini bertanggung jawab untuk menginisialisasi
	 * semua state awal halaman: validasi sesi, pengaturan filter tanggal default, pengisian combobox,
	 * penentuan hak akses, dan pemuatan data grid pertama kali.
	 *
	 * <b>Cara kerja:</b><br>
	 * Pertama memvalidasi sesi pengguna; jika tidak valid, pengguna diarahkan ke halaman logoff.
	 * Kemudian mengambil data pengguna saat ini via {@link Common#getCurrentUser()}. Filter tanggal
	 * diinisialisasi dengan rentang 6 bulan ke belakang hingga hari ini dan dibuat readonly untuk
	 * mencegah pengeditan langsung. Hak akses diperiksa untuk menentukan visibilitas tombol edit
	 * dan tombol sent. Combobox pemilik aset diisi dengan semua pemilik yang aktif atau null aktif.
	 * Data grid dimuat pertama kali melalui {@link #loadDataDenganProgressPosting(Event)}.
	 * Paging diinisialisasi dengan listener yang akan memuat ulang data saat halaman berganti.
	 * Filter lanjut diinisialisasi melalui {@link FilterLanjutHelper#setup(Component)}.
	 *
	 * <b>Parameter:</b><br>
	 * @param comp komponen root ZUL yang telah selesai di-wire, digunakan untuk setup filter lanjut
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika sesi tidak valid (usersTemp null atau hak READ tidak ada), pengguna langsung diarahkan
	 * ke halaman logoff tanpa melanjutkan inisialisasi. Exception dari superclass dilempar ke atas.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Rentang tanggal default 6 bulan dapat diubah dengan memodifikasi nilai parameter pada
	 * calendar.set(Calendar.MONTH, ...). Jika ada filter baru yang perlu ditambahkan, inisialisasi
	 * harus dilakukan di sini sebelum pemanggilan loadDataDenganProgressPosting.
	 *
	 * @throws Exception jika terjadi error pada proses inisialisasi superclass atau komponen ZK
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
	 * <h3>onBatalkanPostingSemua — Pembatalan Posting Semua Transaksi Pembayaran DP</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Metode ini menangani event klik tombol "Batalkan Posting Semua" pada toolbar halaman.
	 * Fungsinya adalah membatalkan posting jurnal untuk semua transaksi pembayaran DP yang
	 * sesuai dengan filter aktif dan sudah berstatus terposting, sehingga transaksi tersebut
	 * kembali ke status belum diposting.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menampilkan dialog konfirmasi sebelum melakukan tindakan. Jika pengguna menyetujui,
	 * sistem mencari semua {@link PembayaranDpMasterAssetDetail} yang memiliki postingHistory
	 * tidak null (sudah diposting) sesuai filter kriteria aktif. Untuk setiap transaksi,
	 * postingHistory di-set ke null dan data diperbarui. Kemudian semua entri grup_transaksi
	 * yang terkait (yang belum closing) dihapus menggunakan SQL native langsung ke skema akunting.
	 * Setelah semua batalkan selesai, grid dimuat ulang menggunakan timer default.
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika pengguna memilih CANCEL pada dialog konfirmasi, tidak ada tindakan yang dilakukan.
	 * Error pada operasi basis data tidak di-catch secara eksplisit di metode ini; pastikan
	 * transaksi Hibernate dikelola dengan benar oleh {@link Common#refreshSaveOrUpdate}.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Perhatikan query SQL langsung yang menghapus dari skema akunting.grup_transaksi. Jika
	 * nama skema atau kolom berubah, query ini harus diperbarui secara manual.
	 *
	 * @param event event ZK yang dipicu oleh klik tombol, tidak digunakan langsung di metode ini
	 * @throws Exception jika terjadi error pada operasi basis data atau komponen ZK
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi pembayaran DP ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<PembayaranDpMasterAssetDetail> pembayaranDpMasterAssets = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail : pembayaranDpMasterAssets) {
								pembayaranDpMasterAssetDetail.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pembayaranDpMasterAssetDetail);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where pembayaran_dp_master_asset_detail="
												+ pembayaranDpMasterAssetDetail.getId() + " and closing is null")
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
	 * <h3>onPostingSemua — Posting Jurnal Massal untuk Semua Transaksi Pembayaran DP</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Metode ini menangani event klik tombol "Posting Semua" pada toolbar halaman. Fungsinya adalah
	 * membuka dialog form posting yang meminta tanggal transaksi dan keterangan, kemudian memposting
	 * semua transaksi pembayaran DP yang belum diposting secara massal di latar belakang.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat jendela modal baru berisi form dengan field tanggal posting dan keterangan. Setelah
	 * pengguna mengisi dan mengklik Simpan, dibuat satu {@link PostingHistory} baru yang akan
	 * digunakan sebagai referensi untuk semua transaksi dalam batch ini. Proses posting kemudian
	 * dijalankan di thread terpisah untuk menjaga responsivitas UI. Untuk setiap
	 * {@link PembayaranDpMasterAssetDetail} yang belum diposting (postingHistory null), sistem
	 * menentukan akun debet dari jenis pemesanan pengadaan aset (getAkunDp) dan akun kredit dari
	 * jenis pembayaran barang, cara pembayaran transfer, atau akun transitori. Jika kedua akun
	 * valid, dipanggil {@link CommonAkunting#saveTransaksi} untuk menyimpan entri jurnal. Progress
	 * ditampilkan melalui label load bar yang diperbarui setiap iterasi. Setelah semua selesai,
	 * ditampilkan pesan sukses dan grid dimuat ulang.
	 *
	 * <b>Logika penentuan akun kredit:</b><br>
	 * 1. Default: akun dari jenis pembayaran barang (getJenisPembayaranBarang().getAkun())<br>
	 * 2. Override jika transfer: akun dari cara pembayaran transfer (getCaraPembayaranTransfer().getAkun())<br>
	 * 3. Override jika transitori: akun transitori dari cara pembayaran (getAkunTransitori())
	 *
	 * <b>Threading:</b><br>
	 * Seluruh iterasi posting berjalan di {@code new Thread(Runnable)} dengan sesi Hibernate
	 * native yang terpisah. Sesi ini ditutup secara eksplisit di akhir thread. Jangan mengakses
	 * komponen ZK dari dalam thread ini secara langsung; gunakan timer atau event untuk update UI.
	 *
	 * <b>Penanganan error:</b><br>
	 * Error pada satu transaksi ditangkap dengan try-catch dan dilaporkan via
	 * {@link Common#tampilErrorJikaAdmin}. Transaksi lain tetap dilanjutkan meskipun satu gagal.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jenis posting history yang digunakan adalah {@link PostingHistory#JENIS_PEMBAYARAN_TAGIHAN_DP}.
	 * Jika jenis baru ditambahkan, sesuaikan konstanta ini. Logika pemilihan akun kredit cukup
	 * kompleks; pastikan unit test tersedia sebelum memodifikasi bagian ini.
	 *
	 * @param event event ZK yang dipicu oleh klik tombol Posting Semua
	 * @throws Exception jika terjadi error pada pembuatan komponen ZK atau operasi basis data awal
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Transaksi Pembayaran");
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
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal dan pilih tanggal posting yang sesuai; (2) Pastikan tanggal yang dipilih berada dalam periode akuntansi yang aktif; (3) ulangi proses posting ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
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
													PostingHistory.JENIS_PEMBAYARAN_TAGIHAN_DP);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<PembayaranDpMasterAssetDetail> pembayaranDpMasterAssets = initCriteria(
													true).add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail : pembayaranDpMasterAssets) {

												if (pembayaranDpMasterAssetDetail != null) {

													try {

														PembayaranDpMasterAsset pembayaranDpMasterAsset = pembayaranDpMasterAssetDetail
																.getPembayaranDpMasterAsset();

														Akun akunDebet = pembayaranDpMasterAssetDetail
																.getPemesananPengadaanMasterAsset() == null
																|| pembayaranDpMasterAssetDetail
																		.getPemesananPengadaanMasterAsset()
																		.getJenisPemesananPengadaanAsset() == null
																				? null
																				: pembayaranDpMasterAssetDetail
																						.getPemesananPengadaanMasterAsset()
																						.getJenisPemesananPengadaanAsset()
																						.getAkunDp();
														Double nilai = pembayaranDpMasterAssetDetail.getDibayar();

														Akun akunKredit = pembayaranDpMasterAsset
																.getJenisPembayaranBarang() == null ? null
																		: pembayaranDpMasterAsset
																				.getJenisPembayaranBarang().getAkun();

														if (pembayaranDpMasterAssetDetail
																.getDaftarPengajuanTransfer() != null
																&& pembayaranDpMasterAssetDetail
																		.getDaftarPengajuanTransfer()
																		.getProsesTransfer() != null
																&& pembayaranDpMasterAssetDetail
																		.getDaftarPengajuanTransfer()
																		.getProsesTransfer()
																		.getCaraPembayaranTransfer() != null
																&& pembayaranDpMasterAssetDetail
																		.getDaftarPengajuanTransfer().getTransfer()
																&& pembayaranDpMasterAssetDetail
																		.getDaftarPengajuanTransfer()
																		.getProsesTransfer().getCaraPembayaranTransfer()
																		.getAkun() != null) {
															akunKredit = pembayaranDpMasterAssetDetail
																	.getDaftarPengajuanTransfer().getProsesTransfer()
																	.getCaraPembayaranTransfer().getAkun();
														}

														if (pembayaranDpMasterAssetDetail
																.getDaftarPengajuanTransfer() != null
																&& pembayaranDpMasterAssetDetail
																		.getDaftarPengajuanTransfer()
																		.getProsesTransfer() != null
																&& pembayaranDpMasterAssetDetail
																		.getDaftarPengajuanTransfer()
																		.getProsesTransfer()
																		.getCaraPembayaranTransfer() != null
																&& pembayaranDpMasterAssetDetail
																		.getDaftarPengajuanTransfer().getTransitori()
																&& pembayaranDpMasterAssetDetail
																		.getDaftarPengajuanTransfer()
																		.getProsesTransfer().getCaraPembayaranTransfer()
																		.getAkunTransitori() != null) {
															akunKredit = pembayaranDpMasterAssetDetail
																	.getDaftarPengajuanTransfer().getProsesTransfer()
																	.getCaraPembayaranTransfer().getAkunTransitori();
														}

														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {

																ket = "Pembayaran terhadap tagihan DP \""
																		+ (pembayaranDpMasterAssetDetail
																				.getPemesananPengadaanMasterAsset()
																				.getKodeInvoice()
																				+ "-"
																				+ pembayaranDpMasterAssetDetail
																						.getPemesananPengadaanMasterAsset()
																						.getPenyedia().getNama()
																				+ "-"
																				+ pembayaranDpMasterAsset
																						.getKeterangan())
																		+ "\" sebanyak "
																		+ Common.numberFormat.get()
																				.format(pembayaranDpMasterAsset
																						.getNilaiDibayar());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(ket + " (" + Common.numberFormat.get().format(
																	rowIndex * 100.0 / pembayaranDpMasterAssets.size())
																	+ " %)");

															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																session.getTransaction().begin();
																if (nilai > 0.1) {
																	CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			pembayaranDpMasterAssetDetail
																					.getTanggalTransaksi(),
																			nilai, denda, pembayaranDpMasterAssetDetail,
																			pembayaranDpMasterAssetDetail
																					.getPemesananPengadaanMasterAsset()
																					.getSatuanKerja(),
																			session);
																} else {
																	CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			pembayaranDpMasterAssetDetail
																					.getTanggalTransaksi(),
																			nilai, denda, pembayaranDpMasterAssetDetail,
																			pembayaranDpMasterAssetDetail
																					.getPemesananPengadaanMasterAsset()
																					.getSatuanKerja(),
																			session);
																}
																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															pembayaranDpMasterAssetDetail
																	.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(pembayaranDpMasterAssetDetail);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingPembayaranDpAction.java:639");
														// exception per-item diabaikan agar iterasi lanjut
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
	 * <h3>PembayaranDpMasterAssetRenderer — Renderer Baris Grid Posting Pembayaran DP</h3>
	 *
	 * <b>Untuk apa:</b><br>
	 * Inner class ini bertanggung jawab merender setiap baris pada grid halaman posting pembayaran DP.
	 * Setiap baris merepresentasikan satu {@link PembayaranDpMasterAssetDetail} beserta informasi
	 * jurnal akuntansinya (pasangan akun debet dan kredit) dan tombol aksi posting/pembatalan.
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengimplementasi metode render dari MyRowRenderer. Untuk setiap baris, ditampilkan:
	 * kode invoice (dengan link ke proses transfer jika ada), nama penyedia, jenis pembayaran barang,
	 * nilai pembayaran, tanggal transaksi, preview jurnal debet/kredit, status posting, dan tombol
	 * aksi. Logika penentuan akun kredit sama dengan logika di onPostingSemua. Tombol Posting dan
	 * Batalkan Posting hanya ditampilkan sesuai kondisi hak akses dan status posting saat ini.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Kelas ini erat kaitannya dengan logika posting di {@link #onPostingSemua(Event)}. Jika
	 * logika resolusi akun berubah di satu tempat, harus diperbarui di keduanya untuk konsistensi.
	 */
	class PembayaranDpMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render — Merender Satu Baris Data Pembayaran DP ke Grid</h3>
		 *
		 * <b>Tujuan:</b><br>
		 * Mengisi komponen-komponen ZK pada satu baris grid dengan data dari satu entitas
		 * {@link PembayaranDpMasterAssetDetail}, termasuk informasi jurnal dan tombol aksi.
		 *
		 * <b>Cara kerja:</b><br>
		 * Mengambil data header pembayaran DP dari detail, menentukan akun debet dan kredit,
		 * menampilkan preview jurnal menggunakan {@link GrupTransaksi#tampilkanJurnal}, dan
		 * menampilkan status posting. Jika akun tidak valid, ditampilkan pesan error. Tombol
		 * posting/pembatalan dibuat dengan listener yang menjalankan operasi melalui timer default
		 * untuk memastikan eksekusi di event thread yang tepat.
		 *
		 * <b>Penanganan error:</b><br>
		 * Jika akun debet atau kredit null, baris tetap dirender dengan pesan informatif tentang
		 * akun mana yang tidak tersedia. Tombol posting tidak ditampilkan untuk baris dengan akun
		 * tidak valid.
		 *
		 * @param arg0 komponen Row ZK yang akan diisi dengan data dan komponen child
		 * @param arg1 objek data baris, dicast ke {@link PembayaranDpMasterAssetDetail}
		 * @throws Exception jika terjadi error pada operasi basis data atau pembuatan komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail = (PembayaranDpMasterAssetDetail) arg1;
			final PembayaranDpMasterAsset pembayaranDpMasterAsset = pembayaranDpMasterAssetDetail
					.getPembayaranDpMasterAsset();
			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(PembayaranDpMasterAssetDetail.class, pembayaranDpMasterAssetDetail,
					pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset() == null ? ""
							: pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset().getKodeInvoice()))
					.setParent(arg0);

			if (pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer() != null
					&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer() != null) {
				A a = new A(pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);
			}

			if (pembayaranDpMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(aaa);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pembayaranDpMasterAsset.getDisposisiSop().getKeterangan() + " ("
						+ pembayaranDpMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pembayaranDpMasterAsset.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			}

			new Label(pembayaranDpMasterAsset.getPenyedia() == null ? ""
					: pembayaranDpMasterAsset.getPenyedia().getNama()).setParent(arg0);

			new Label(pembayaranDpMasterAsset.getJenisPembayaranBarang() == null ? ""
					: pembayaranDpMasterAsset.getJenisPembayaranBarang().getNama()).setParent(arg0);

			Double nilai = pembayaranDpMasterAssetDetail.getDibayar();

			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

			new Label(Common.dateFormat3.get().format(pembayaranDpMasterAssetDetail.getTanggalTransaksi())).setParent(arg0);

			Akun akunDebet = pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset() == null
					|| pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset()
							.getJenisPemesananPengadaanAsset() == null ? null
									: pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset()
											.getJenisPemesananPengadaanAsset().getAkunDp();

			Akun akunKredit = pembayaranDpMasterAsset.getJenisPembayaranBarang() == null ? null
					: pembayaranDpMasterAsset.getJenisPembayaranBarang().getAkun();

			if (pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer() != null
					&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer() != null
					&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer()
							.getCaraPembayaranTransfer() != null
					&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer().getTransfer()
					&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer()
							.getCaraPembayaranTransfer().getAkun() != null) {
				akunKredit = pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer()
						.getCaraPembayaranTransfer().getAkun();
			}

			if (pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer() != null
					&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer() != null
					&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer()
							.getCaraPembayaranTransfer() != null
					&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer().getTransitori()
					&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer()
							.getCaraPembayaranTransfer().getAkunTransitori() != null) {
				akunKredit = pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer()
						.getCaraPembayaranTransfer().getAkunTransitori();
			}

			if (akunDebet != null && akunKredit != null) {

				GrupTransaksi.tampilkanJurnal(akunDebet, nilai, akunKredit, nilai).setParent(arg0);
			} else {
				new Label("Transaksi tidak valid."
						+ (akunDebet != null ? " Debet: " + akunDebet.toString() : " Akun debet tidak ada.")
						+ (akunKredit != null ? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
								: " Akun kredit tidak ada."))
						.setParent(arg0);
			}
			Session session = HibernateUtil.currentSession();
			String bukti = "";
			bukti = (String) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.eq("pembayaranDpMasterAssetDetail", pembayaranDpMasterAssetDetail))
					.setMaxResults(1).setProjection(Projections.property("kode")).uniqueResult();

			new Label(pembayaranDpMasterAssetDetail.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: pembayaranDpMasterAssetDetail.getPostingHistory().toString() + ", no. bukti : " + bukti)
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && pembayaranDpMasterAssetDetail.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pembayaranDpMasterAssetDetail.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pembayaranDpMasterAssetDetail);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where pembayaran_dp_master_asset_detail="
												+ pembayaranDpMasterAssetDetail.getId() + " and closing is null")
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
				button.setVisible(edit && pembayaranDpMasterAssetDetail.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PEMBAYARAN_TAGIHAN_DP);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								try {

									Akun akunDebet = pembayaranDpMasterAssetDetail
											.getPemesananPengadaanMasterAsset() == null
											|| pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset()
													.getJenisPemesananPengadaanAsset() == null
															? null
															: pembayaranDpMasterAssetDetail
																	.getPemesananPengadaanMasterAsset()
																	.getJenisPemesananPengadaanAsset().getAkunDp();
									Double nilai = pembayaranDpMasterAssetDetail.getDibayar();

									Akun akunKredit = pembayaranDpMasterAssetDetail.getPembayaranDpMasterAsset()
											.getJenisPembayaranBarang() == null ? null
													: pembayaranDpMasterAssetDetail.getPembayaranDpMasterAsset()
															.getJenisPembayaranBarang().getAkun();

									if (pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer() != null
											&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer()
													.getProsesTransfer() != null
											&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer()
													.getProsesTransfer().getCaraPembayaranTransfer() != null
											&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer().getTransfer()
											&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer()
													.getProsesTransfer().getCaraPembayaranTransfer()
													.getAkun() != null) {
										akunKredit = pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer()
												.getProsesTransfer().getCaraPembayaranTransfer().getAkun();
									}

									if (pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer() != null
											&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer()
													.getProsesTransfer() != null
											&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer()
													.getProsesTransfer().getCaraPembayaranTransfer() != null
											&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer()
													.getTransitori()
											&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer()
													.getProsesTransfer().getCaraPembayaranTransfer()
													.getAkunTransitori() != null) {
										akunKredit = pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer()
												.getProsesTransfer().getCaraPembayaranTransfer().getAkunTransitori();
									}

									if (akunDebet != null && akunKredit != null) {
										Boolean apakahUangMasuk = true;

										String ket = "";
										try {

											ket = "Pembayaran terhadap tagihan DP \"" + (pembayaranDpMasterAssetDetail
													.getPemesananPengadaanMasterAsset().getKodeInvoice()
													+ "-"
													+ pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset()
															.getPenyedia().getNama()
													+ "-" + pembayaranDpMasterAssetDetail.getKeterangan())
													+ "\" sebanyak " + Common.numberFormat.get().format(nilai);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}

										try {

											Akun akunDenda = null;
											Akun akunPiutangDenda = null;
											Double denda = 0.0;

											session.getTransaction().begin();

											if (nilai > 0.1) {
												CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda,
														akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
														pembayaranDpMasterAssetDetail.getTanggalTransaksi(), nilai,
														denda, pembayaranDpMasterAssetDetail,
														pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset()
																.getSatuanKerja(),
														session);
											} else {
												CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda,
														akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
														pembayaranDpMasterAssetDetail.getTanggalTransaksi(), nilai,
														denda, pembayaranDpMasterAssetDetail,
														pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset()
																.getSatuanKerja(),
														session);
											}
											session.getTransaction().commit();
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}

										pembayaranDpMasterAssetDetail.setPostingHistory(postingHistory);
										session.getTransaction().begin();
										session.update(pembayaranDpMasterAssetDetail);
										session.getTransaction().commit();
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingPembayaranDpAction.java:978");
									// exception per-item diabaikan agar grid tetap dimuat ulang
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
	 * <h3>initCriteria — Membangun Kriteria Pencarian Data Pembayaran DP</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Membangun objek {@link Criteria} Hibernate yang mencerminkan seluruh filter pencarian
	 * yang aktif pada halaman, untuk digunakan dalam query data grid maupun penghitungan total
	 * data untuk paging.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat kriteria pada entitas {@link PembayaranDpMasterAssetDetail} dengan alias ke tabel
	 * pembayaranDpMasterAsset dan daftarPengajuanTransfer (LEFT JOIN). Menambahkan filter:
	 * status belum/sudah diposting (dari checkbox), hanya yang sudah disetujui (disetujuiOleh
	 * tidak null), nilai dibayar tidak nol, rentang tanggal transaksi, filter pemilik aset,
	 * filter ruang, dan filter kode/nama. Jika parameter {@code order} true, ditambahkan urutan
	 * descending berdasarkan id untuk tampilan terbaru di atas.
	 *
	 * <b>Parameter:</b><br>
	 * @param order jika true, hasil query akan diurutkan descending berdasarkan id
	 *
	 * <b>Return:</b><br>
	 * @return objek {@link Criteria} yang siap dieksekusi atau ditambahkan filter tambahan
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Filter tanggal menggunakan sqlRestriction dengan format tanggal dari
	 * {@link Common#databaseDateFormat} — pastikan format ini sesuai dengan database yang digunakan.
	 * Filter LEFT JOIN pada daftarPengajuanTransfer diperlukan untuk menampilkan transaksi yang
	 * belum memiliki pengajuan transfer. Jangan hapus alias ini.
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PembayaranDpMasterAssetDetail.class)

				.createAlias("pembayaranDpMasterAsset", "pembayaranDpMasterAsset")

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(!searchtelahtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNotNull("postingHistory"))

				.add(Restrictions.isNotNull("pembayaranDpMasterAsset.disetujuiOleh"))

				.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.isNotNull("pembayaranDpMasterAsset.jenisPembayaranBarang"),

						Restrictions.and(Restrictions.isNotNull("daftarPengajuanTransfer.prosesTransfer"),
								Restrictions.eq("daftarPengajuanTransfer.transfer", true))))

				.add(Restrictions.ne("dibayar", 0.0)).add(Restrictions.isNotNull("dibayar"))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"this_.tanggal_transaksi is null or date(this_.tanggal_transaksi) between date('"
								+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and date('"
								+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchpemilikAsset.getSelectedItem() == null
						|| searchpemilikAsset.getSelectedItem().getValue() == null
						|| searchpemilikAsset.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("pembayaranDpMasterAsset.pemilikAsset",
										searchpemilikAsset.getSelectedItem().getValue()))

				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pembayaranDpMasterAsset.ruang", searchruang.getAttribute("ruang"))))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("pembayaranDpMasterAsset.kode", searchkode.getValue(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("pembayaranDpMasterAsset.nama", searchkode.getValue(),
										MatchMode.ANYWHERE)));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <h3>onSearchDefaultTanpaProgress — Memuat Data Grid Tanpa Indikator Progress</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Melakukan query data aktual ke basis data dan memperbarui model grid serta paging,
	 * tanpa menampilkan atau memperbarui indikator progress loading. Metode ini adalah inti
	 * dari operasi pencarian dan biasanya dipanggil dari dalam callback timer yang sudah
	 * mengelola state loading secara terpisah.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@link #initCriteria(boolean)} dua kali: pertama tanpa urutan untuk menghitung
	 * total data paging, kemudian dengan urutan untuk mengambil data halaman aktif dengan batas
	 * {@link Common#ROWS_COUNT_ON_PAGE} baris. Hasil query dibungkus dalam {@link SimpleListModel}
	 * dan di-set ke grid dengan renderer {@link PembayaranDpMasterAssetRenderer}.
	 *
	 * <b>Penanganan error:</b><br>
	 * Method ini tidak menangani exception secara eksplisit; exception akan dilempar ke pemanggil
	 * dan ditangkap oleh blok finally di {@link #loadDataDenganProgressPosting(Event)}.
	 *
	 * @param event event pencarian, dapat null jika dipanggil dari inisialisasi
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PembayaranDpMasterAssetDetail> pembayaranDpMasterAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pembayaranDpMasterAsset);
		grid.setRowRenderer(new PembayaranDpMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <h3>onSearchDefault — Entry Point Pencarian Data dari Event ZK</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Metode delegasi yang menjadi entry point standar untuk event pencarian dari komponen ZUL
	 * (misalnya klik tombol Cari, onChange filter, atau onPaging). Metode ini mendelegasikan
	 * pemuatan data ke {@link #loadDataDenganProgressPosting(Event)} yang mengelola state loading
	 * dan progress indicator secara aman.
	 *
	 * <b>Cara kerja:</b><br>
	 * Langsung mendelegasikan ke {@link #loadDataDenganProgressPosting(Event)} dengan event
	 * yang diterima. Tidak ada logika tambahan di metode ini — seluruh kompleksitas dikelola
	 * oleh metode yang dipanggil.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Metode ini dipanggil oleh berbagai event listener dan callback. Jangan tambahkan logika
	 * langsung di sini; tambahkan di onSearchDefaultTanpaProgress jika perlu.
	 *
	 * @param event event ZK yang memicu pencarian, dilewatkan ke metode loading
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Flag yang menandakan apakah proses loading data posting jurnal sedang berjalan.
	 * Digunakan untuk mencegah eksekusi ganda yang dapat menyebabkan kondisi balapan (race condition)
	 * pada antarmuka pengguna atau basis data.
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag yang menandakan apakah ada permintaan reload yang tertunda saat loading sedang aktif.
	 * Jika true, setelah loading saat ini selesai, loading akan diulang sekali lagi untuk
	 * memastikan data terbaru (akibat perubahan filter saat loading berlangsung) dimuat.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * <h3>loadDataDenganProgressPosting — Memuat Data Grid dengan Indikator Progress</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Metode utama untuk memuat data grid pembayaran DP dengan indikator progress loading yang
	 * ditampilkan ke pengguna. Metode ini mengelola state loading untuk mencegah eksekusi ganda
	 * dan menangani permintaan reload yang datang saat proses loading sedang berlangsung.
	 *
	 * <b>Cara kerja:</b><br>
	 * Jika loading sedang aktif ({@code postingJurnalLoadingAktif} true), permintaan baru dicatat
	 * sebagai tertunda dan metode langsung keluar. Jika tidak, flag aktif di-set true dan
	 * progress indicator ditampilkan melalui {@link ais.ui.util.PostingJurnalLoadingUtil}. Proses
	 * loading sebenarnya dijalankan di dalam callback timer ZK (menggunakan
	 * {@link Common#createDefaultTimer}) untuk menghindari pemblokiran event thread. Di dalam
	 * callback, dipanggil {@link #onSearchDefaultTanpaProgress(Event)} dengan progress diperbarui
	 * bertahap. Blok finally memastikan flag loading selalu di-reset dan mengecek apakah ada
	 * reload tertunda yang perlu dieksekusi ulang.
	 *
	 * <b>Threading:</b><br>
	 * Seluruh metode ini berjalan di event thread ZK, bukan thread terpisah. Timer ZK digunakan
	 * untuk melepas kontrol sebentar agar UI dapat diperbarui sebelum query berjalan.
	 *
	 * <b>Penanganan error:</b><br>
	 * Blok try-finally memastikan flag {@code postingJurnalLoadingAktif} selalu di-reset ke false
	 * meskipun terjadi exception, sehingga loading tidak macet selamanya.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Nilai persentase progress (7, 48, 92, 96, 100) bersifat kosmetik dan dapat disesuaikan.
	 * Pastikan flag loading selalu di-reset di blok finally; jangan tambahkan return statement
	 * sebelum blok finally tanpa mempertimbangkan implikasinya.
	 *
	 * @param event event ZK yang memicu pemuatan data, dapat null jika dipanggil dari inisialisasi
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


	// ================================================================= jalur API dasbor draft jurnal

	/**
	 * Kriteria dokumen yang SAMA dengan baris "Pembayaran DP Vendor" di dasbor draft jurnal:
	 * detail pembayaran DP yang induknya sudah disetujui, punya sumber akun kredit, dan nilai
	 * dibayar tidak nol, pada rentang tanggal transaksi.
	 *
	 * <p>PERBEDAAN dari {@code initCriteria} layar: klausa tanggalnya DIBERI KURUNG. Versi layar
	 * menulis "this_.tanggal_transaksi is null or date(...) between ..." telanjang di dalam
	 * sqlRestriction; Hibernate tidak membungkusnya, sehingga presedensi AND/OR SQL membuat cabang
	 * "between" lolos dari seluruh filter lain. Baris tanpa tanggal tetap ikut terpilih, mengikuti
	 * maksud layar.</p>
	 */
	private static Criteria kriteriaPembayaranDpStatic(Session session, java.util.Date mulai,
			java.util.Date sampai) {
		Criteria c = session.createCriteria(PembayaranDpMasterAssetDetail.class)
				.createAlias("pembayaranDpMasterAsset", "pembayaranDpMasterAsset")
				.add(Restrictions.isNotNull("pembayaranDpMasterAsset.disetujuiOleh"))
				.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer", Criteria.LEFT_JOIN)
				.add(Restrictions.or(
						Restrictions.isNotNull("pembayaranDpMasterAsset.jenisPembayaranBarang"),
						Restrictions.and(Restrictions.isNotNull("daftarPengajuanTransfer.prosesTransfer"),
								Restrictions.eq("daftarPengajuanTransfer.transfer", true))))
				.add(Restrictions.ne("dibayar", 0.0)).add(Restrictions.isNotNull("dibayar"))
				// pilih menentukan nilai efektif: getDibayar() menolkan baris yang tidak dipilih.
				.add(Restrictions.eq("pilih", true));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("(this_.tanggal_transaksi is null"
					+ " or date(this_.tanggal_transaksi) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "'))"));
		}
		return c;
	}

	/**
	 * Posting SEMUA pembayaran DP vendor pada rentang -- jalur API dasbor Draft Jurnal POS.
	 * Jurnal per dokumen mengikuti tombol layar: debet akun DP jenis pemesanan, kredit akun jenis
	 * pembayaran barang (ditimpa akun cara pembayaran transfer/transitori), senilai dibayar;
	 * nilai &le; 0.1 memutar posisi. Penanda hanya dicap bila jurnal tersimpan, dan riwayatnya
	 * diberi {@code posting=true} -- lihat catatan di {@code PostingPembayaranAction.postingSemua}.
	 */
	public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<?> daftar = kriteriaPembayaranDpStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory")).list();
			if (daftar.isEmpty()) {
				return 0;
			}

			PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_PEMBAYARAN_TAGIHAN_DP);
			postingHistory.setTbmuser(oleh);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTanggalPosting(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setPosting(true);
			postingHistory.setKeterangan("Posting massal pembayaran DP vendor dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			for (Object o : daftar) {
				PembayaranDpMasterAssetDetail d = (PembayaranDpMasterAssetDetail) o;
				if (d == null || d.getPembayaranDpMasterAsset() == null) {
					continue;
				}
				try {
					Akun akunDebet = d.getPemesananPengadaanMasterAsset() == null
							|| d.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset() == null
									? null
									: d.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset()
											.getAkunDp();
					Akun akunKredit = d.getPembayaranDpMasterAsset().getJenisPembayaranBarang() == null
							? null
							: d.getPembayaranDpMasterAsset().getJenisPembayaranBarang().getAkun();
					if (d.getDaftarPengajuanTransfer() != null
							&& d.getDaftarPengajuanTransfer().getProsesTransfer() != null
							&& d.getDaftarPengajuanTransfer().getProsesTransfer()
									.getCaraPembayaranTransfer() != null
							&& Boolean.TRUE.equals(d.getDaftarPengajuanTransfer().getTransfer())
							&& d.getDaftarPengajuanTransfer().getProsesTransfer().getCaraPembayaranTransfer()
									.getAkun() != null) {
						akunKredit = d.getDaftarPengajuanTransfer().getProsesTransfer()
								.getCaraPembayaranTransfer().getAkun();
					}
					if (d.getDaftarPengajuanTransfer() != null
							&& d.getDaftarPengajuanTransfer().getProsesTransfer() != null
							&& d.getDaftarPengajuanTransfer().getProsesTransfer()
									.getCaraPembayaranTransfer() != null
							&& Boolean.TRUE.equals(d.getDaftarPengajuanTransfer().getTransitori())
							&& d.getDaftarPengajuanTransfer().getProsesTransfer().getCaraPembayaranTransfer()
									.getAkunTransitori() != null) {
						akunKredit = d.getDaftarPengajuanTransfer().getProsesTransfer()
								.getCaraPembayaranTransfer().getAkunTransitori();
					}
					Double nilai = d.getDibayar();
					if (akunDebet == null || akunKredit == null || nilai == null
							|| nilai == 0.0) {
						continue;
					}

					String ket = "Pembayaran terhadap tagihan DP sebanyak "
							+ Common.numberFormat.get().format(nilai);
					try {
						ket = "Pembayaran terhadap tagihan DP \""
								+ d.getPemesananPengadaanMasterAsset().getKodeInvoice() + "-"
								+ d.getPemesananPengadaanMasterAsset().getPenyedia().getNama() + "-"
								+ d.getPembayaranDpMasterAsset().getKeterangan() + "\" sebanyak "
								+ Common.numberFormat.get()
										.format(d.getPembayaranDpMasterAsset().getNilaiDibayar());
					} catch (Exception e) {
						// Pemesanannya boleh tidak lengkap; kalimat baku di atas tetap terpakai.
					}

					ais.database.model.rab.SatuanKerja satuanKerja = null;
					try {
						satuanKerja = d.getPemesananPengadaanMasterAsset().getSatuanKerja();
					} catch (Exception e) {
						// Satuan kerja boleh kosong.
					}

					boolean tersimpan;
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					if (nilai > 0.1) {
						tersimpan = CommonAkunting.saveTransaksi(new Akun[] { akunDebet },
								new Akun[] { akunKredit }, null, null, postingHistory, true, ket,
								d.getTanggalTransaksi(), new Double[] { nilai }, new Double[] { nilai },
								0.0, d, satuanKerja, session);
					} else {
						tersimpan = CommonAkunting.saveTransaksi(new Akun[] { akunKredit },
								new Akun[] { akunDebet }, null, null, postingHistory, true, ket,
								d.getTanggalTransaksi(), new Double[] { nilai }, new Double[] { nilai },
								0.0, d, satuanKerja, session);
					}
					if (tersimpan) {
						d.setPostingHistory(postingHistory);
						session.update(d);
						session.getTransaction().commit();
						n++;
					} else {
						session.getTransaction().rollback();
					}
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingPembayaranDpAction jalur API");
				}
			}

			if (n == 0) {
				// Tidak satu dokumen pun terjurnal: riwayat kosong tidak ditinggalkan.
				try {
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.delete(postingHistory);
					session.getTransaction().commit();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "jalur API dasbor draft jurnal");
				}
			}
		} finally {
			try {
				session.disconnect();
				HibernateUtil.closeSession();
			} catch (Exception e) {
				// penutupan sesi manual: kegagalannya tidak menutupi hasil posting
			}
		}
		return n;
	}

	/**
	 * Membatalkan posting SEMUA dokumen terposting pada rentang: jurnal turunannya dihapus (baris
	 * transaksi lebih dulu, lalu grupnya -- hanya yang belum closing), kemudian penandanya dilepas.
	 * Layar ZK menghapus grup_transaksi langsung tanpa membersihkan baris transaksinya.
	 */
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<?> daftar = kriteriaPembayaranDpStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (Object o : daftar) {
				PembayaranDpMasterAssetDetail d = (PembayaranDpMasterAssetDetail) o;
				if (d == null) {
					continue;
				}
				try {
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where pembayaran_dp_master_asset_detail="
							+ d.getId() + " and closing is null)").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where pembayaran_dp_master_asset_detail="
							+ d.getId() + " and closing is null").executeUpdate();
					d.setPostingHistory(null);
					session.update(d);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "kriteriaPembayaranDpStatic jalur API");
				}
			}
		} finally {
			try {
				session.disconnect();
				HibernateUtil.closeSession();
			} catch (Exception e) {
				// penutupan sesi manual: kegagalannya tidak menutupi hasil pembatalan
			}
		}
		return n;
	}

}
