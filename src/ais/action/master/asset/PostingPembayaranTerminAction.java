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
import ais.database.model.asset.PembayaranTerminMasterAsset;
import ais.database.model.asset.PembayaranTerminMasterAssetDetail;
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
 * <h3>PostingPembayaranTerminAction — Posting Jurnal Pembayaran Termin Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini bertanggung jawab atas seluruh alur kerja posting jurnal akuntansi
 * untuk transaksi pembayaran termin pengadaan aset. Pembayaran termin adalah
 * pembayaran bertahap kepada penyedia barang/jasa berdasarkan progres pekerjaan
 * atau jadwal yang disepakati dalam kontrak pengadaan. Setiap transaksi pembayaran
 * termin yang telah disetujui dan memiliki akun yang valid perlu diposting ke buku
 * besar agar tercatat secara resmi dalam sistem akuntansi institusi. Kelas ini
 * menyediakan antarmuka ZK (ZKoss) untuk memfilter, menampilkan, memposting, dan
 * membatalkan posting transaksi-transaksi tersebut secara individual maupun massal.<br>
 * <br>
 *
 * <b>Cara kerja:</b><br>
 * Setelah ZK menginisialisasi composer melalui {@code doAfterCompose}, kelas ini
 * memuat daftar detail pembayaran termin ({@code PembayaranTerminMasterAssetDetail})
 * berdasarkan berbagai filter: rentang tanggal transaksi, pemilik aset, ruangan,
 * kode/nama penyedia, dan status posting (belum/sudah). Setiap baris ditampilkan
 * oleh inner class {@code PembayaranTerminMasterAssetRenderer} yang menampilkan
 * informasi penyedia, jenis pembayaran, nominal, tanggal, akun jurnal debet/kredit,
 * serta tombol aksi (posting satuan dan batalkan posting). Posting massal dilakukan
 * melalui dialog konfirmasi yang menjalankan proses di thread terpisah untuk
 * menghindari pemblokiran UI. Posting satuan dilakukan langsung dari baris grid.
 * Pembatalan posting menghapus entri {@code GrupTransaksi} terkait dari skema
 * akuntansi dan mereset {@code postingHistory} pada entitas detail pembayaran.<br>
 * <br>
 * Filter tambahan tersedia lewat {@code FilterLanjutHelper} yang terpasang di
 * komponen ZUL. Pemuat data menggunakan mekanisme progress ({@code loadDataDenganProgressPosting})
 * yang mencegah reload ganda bila permintaan baru datang saat reload sebelumnya masih berjalan.<br>
 * <br>
 *
 * <b>Threading:</b><br>
 * Posting massal ({@code onPostingSemua}) menjalankan iterasi posting di thread
 * Java baru menggunakan {@code HibernateUtil.currentNativeSession()} agar tidak
 * bergantung pada sesi managed ZK. Thread latar ini memanggil
 * {@code CommonAkunting.saveTransaksi} per baris, lalu menutup sesi setelah selesai.
 * UI diperbarui melalui {@code Common.displayLoadBar} dan callback event ZK.
 * Variabel {@code postingJurnalLoadingAktif} dan {@code postingJurnalReloadTertunda}
 * digunakan untuk koalesensi reload sehingga permintaan reload yang masuk saat
 * proses sedang berjalan tidak diabaikan melainkan dijadwalkan ulang.<br>
 * <br>
 *
 * <b>Pemeliharaan:</b><br>
 * Pastikan akun debet (dari {@code JenisPemesananPengadaanAsset.getAkunUtangPekerjaan})
 * dan akun kredit (dari {@code Penyedia.getAkunUtang}) selalu terisi sebelum data
 * dapat diposting — jika salah satu null, baris akan menampilkan pesan validasi dan
 * tombol posting tidak ditampilkan. Logika threshold nilai ({@code > 0.1}) memastikan
 * transaksi dengan nilai sangat kecil tidak menimbulkan masalah arah debet/kredit.
 * Kelas ini menggunakan {@code serialVersionUID} statis dan berjalan di atas ZKoss
 * 5.5 / Java 1.7. Tambahkan indeks pada kolom {@code posting_history} di tabel
 * {@code pembayaran_termin_master_asset_detail} jika volume data besar.
 */
public class PostingPembayaranTerminAction extends GenericAutowireComposer {

	/**
	 * Versi serial untuk serialisasi kelas ini sesuai mekanisme ZK composer.
	 * Nilai ini tidak perlu diubah kecuali ada perubahan struktur field yang
	 * tidak kompatibel ke belakang.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar detail pembayaran termin. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman data grid. */
	private Paging paging;

	/** Kotak teks untuk filter berdasarkan kode invoice atau nama penyedia. */
	private Textbox searchkode;

	/** Combobox untuk filter berdasarkan pemilik aset. */
	private Combobox searchpemilikAsset;

	/** Komponen banbox untuk filter berdasarkan ruangan. */
	private AmbilDataRuangBanbox searchruang;

	/** Checkbox filter untuk menampilkan hanya data yang belum diposting. */
	private MyCheckboxConfig searchtampil;

	/** Checkbox filter untuk menampilkan hanya data yang sudah diposting. */
	private MyCheckboxConfig searchtelahtampil;

	/** Flag apakah pengguna memiliki hak UPDATE (untuk menampilkan tombol posting). */
	private boolean edit = false;

	/** Tombol toolbar untuk memulai proses posting massal. */
	private MyToolbarbuttonConfig sent;

	/** Flag apakah pengguna adalah admin atau memiliki hak APPROVE (untuk batalkan posting). */
	public boolean adminLain;

	/** Datebox tanggal awal filter rentang transaksi. */
	private MyDatebox tglMulai;

	/** Datebox tanggal akhir filter rentang transaksi. */
	private MyDatebox tglSampai;

	/** Pengguna yang sedang login, digunakan sebagai pemilik transaksi posting. */
	private Tbmuser tbmuser;

	/**
	 * <b>Tujuan:</b> Dipanggil ZK sebelum composer di-compose ke halaman.
	 * Melakukan pengecekan keamanan agar halaman hanya dapat diakses oleh
	 * pengguna yang telah terautentikasi.<br>
	 * <br>
	 * <b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} untuk
	 * memverifikasi sesi dan hak akses pengguna, kemudian mendelegasikan
	 * ke implementasi superclass agar proses inisialisasi ZK berjalan normal.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code page} — halaman ZK yang sedang dimuat.<br>
	 * {@code parent} — komponen induk tempat composer dipasang.<br>
	 * {@code compInfo} — metadata komponen dari ZK framework.<br>
	 * <br>
	 * <b>Return:</b> {@code ComponentInfo} dari superclass yang diperlukan ZK
	 * untuk melanjutkan proses compose.<br>
	 * <br>
	 * <b>Penanganan error:</b> Jika sesi tidak valid, {@code Common.doCheckSecurity()}
	 * akan mengarahkan pengguna ke halaman logoff sebelum metode ini selesai.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jangan hapus pemanggilan {@code Common.doCheckSecurity()}
	 * karena tanpanya halaman dapat diakses tanpa autentikasi.
	 *
	 * @param page     halaman ZK yang sedang di-compose
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo informasi metadata komponen ZK
	 * @return ComponentInfo hasil superclass untuk kelanjutan proses ZK
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Metode lifecycle ZK yang dipanggil setelah seluruh komponen
	 * di halaman selesai di-wire ke field-field composer ini. Bertanggung jawab
	 * atas inisialisasi penuh halaman posting pembayaran termin, termasuk validasi
	 * sesi, pengisian filter combo, pengaturan rentang tanggal awal, dan pemuatan
	 * data pertama kali.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * 1. Memanggil {@code super.doAfterCompose} agar wire binding ZK bekerja.<br>
	 * 2. Memanggil {@code Common.initLaguage()} untuk memuat konfigurasi bahasa.<br>
	 * 3. Memvalidasi sesi ({@code usersTemp}) dan hak READ; jika tidak valid,
	 *    mengarahkan ke logoff.<br>
	 * 4. Mengambil pengguna aktif via {@code Common.getCurrentUser()}.<br>
	 * 5. Menginisialisasi rentang tanggal: tglMulai = 6 bulan lalu, tglSampai = hari ini,
	 *    keduanya diset readonly agar tidak dapat diedit langsung oleh pengguna.<br>
	 * 6. Menentukan flag {@code adminLain} (admin atau memiliki hak APPROVE) dan
	 *    flag {@code edit} (memiliki hak UPDATE).<br>
	 * 7. Mengisi combobox pemilik aset dengan data aktif dari database.<br>
	 * 8. Memanggil {@code loadDataDenganProgressPosting} untuk memuat data awal.<br>
	 * 9. Menginisialisasi event paging dan setup filter lanjutan.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code comp} — root komponen ZK yang di-compose, digunakan untuk setup filter lanjutan.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Bila sesi tidak valid atau hak READ tidak terpenuhi,
	 * atribut sesi dibersihkan dan pengguna diarahkan ke halaman logoff. Exception
	 * yang tidak tertangani akan dilempar ke ZK framework.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jika filter baru ditambahkan ke ZUL, pastikan inisialisasi
	 * nilainya dilakukan di sini. Nilai default rentang tanggal (6 bulan) dapat
	 * dikonfigurasi sesuai kebutuhan bisnis.
	 *
	 * @param comp komponen root ZK hasil compose
	 * @throws Exception jika inisialisasi gagal karena error Hibernate atau ZK
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
	 * <b>Tujuan:</b> Menangani aksi pengguna untuk membatalkan posting semua transaksi
	 * pembayaran termin yang sudah pernah diposting, sesuai filter aktif saat ini.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Menampilkan dialog konfirmasi kepada pengguna. Jika pengguna memilih OK,
	 * sistem mengambil semua {@code PembayaranTerminMasterAssetDetail} yang memiliki
	 * {@code postingHistory} (sudah diposting) berdasarkan {@code initCriteria}.
	 * Untuk setiap detail, sistem mereset {@code postingHistory} menjadi null melalui
	 * {@code Common.refreshSaveOrUpdate}, lalu menghapus entri {@code GrupTransaksi}
	 * terkait dari skema {@code akunting} menggunakan SQL native (hanya yang belum
	 * closing). Setelah selesai, data di-reload melalui timer default ZK.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK dari tombol batalkan posting semua, tidak digunakan
	 * secara langsung dalam logika bisnis.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Jika terjadi exception saat penghapusan, akan
	 * dipropagasikan oleh ZK. Disarankan untuk menambahkan try-catch di setiap
	 * iterasi agar kegagalan satu baris tidak menghentikan seluruh proses.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Query SQL native harus disesuaikan jika nama kolom atau
	 * skema database berubah. Flag {@code closing is null} memastikan hanya jurnal
	 * periode berjalan yang dihapus, bukan jurnal penutup.
	 *
	 * @param event event ZK yang memicu aksi ini
	 * @throws Exception jika terjadi error saat akses database atau update entitas
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi pembayaran termin ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<PembayaranTerminMasterAssetDetail> pembayaranTerminMasterAssetDetails = initCriteria(
									true).add(Restrictions.isNotNull("postingHistory")).list();

							for (PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail : pembayaranTerminMasterAssetDetails) {
								pembayaranTerminMasterAssetDetail.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pembayaranTerminMasterAssetDetail);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where pembayaran_termin_master_asset_detail="
												+ pembayaranTerminMasterAssetDetail.getId() + " and closing is null")
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
	 * <b>Tujuan:</b> Menangani aksi pengguna untuk memposting semua transaksi
	 * pembayaran termin yang belum diposting secara massal dalam satu operasi batch.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Membuka dialog modal dengan form input tanggal posting dan keterangan. Saat
	 * pengguna mengklik Simpan, sistem memvalidasi tanggal tidak boleh kosong, lalu
	 * menampilkan konfirmasi kedua. Jika dikonfirmasi, sistem membuat satu entitas
	 * {@code PostingHistory} baru dengan jenis {@code JENIS_PEMBAYARAN_TAGIHAN_TERMIN}
	 * dan menyimpannya ke database. Selanjutnya, semua detail pembayaran termin yang
	 * belum diposting (sesuai filter aktif) diiterasi di thread terpisah. Untuk setiap
	 * detail yang memiliki akun debet dan kredit valid, sistem membuat jurnal melalui
	 * {@code CommonAkunting.saveTransaksi} dengan arah debet/kredit yang ditentukan
	 * oleh tanda nilai (positif atau negatif/koreksi). Setelah semua baris diproses,
	 * sesi Hibernate ditutup dan UI di-refresh melalui callback load bar.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK dari tombol "Posting Semua", tidak digunakan secara
	 * langsung.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Baris yang gagal di-catch per-item agar satu kegagalan
	 * tidak membatalkan seluruh batch. Error akun null ditangani dengan melewati baris
	 * tersebut (blok if akunDebet/akunKredit != null). Keterangan yang gagal diformat
	 * di-catch dan dilaporkan via {@code Common.tampilErrorJikaAdmin}.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jenis posting {@code PostingHistory.JENIS_PEMBAYARAN_TAGIHAN_TERMIN}
	 * harus konsisten dengan konstanta yang digunakan di modul pelaporan. Thread latar
	 * menggunakan {@code HibernateUtil.currentNativeSession()} — jangan ubah ke
	 * currentSession karena tidak thread-safe dengan ZK desktop.
	 *
	 * @param event event ZK yang memicu aksi ini
	 * @throws Exception jika dialog gagal dibuat atau terjadi error UI ZK
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
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting Pembayaran Termin belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal dan pilih tanggal dari kalender; (2) Pastikan tanggal berada dalam periode akuntansi yang aktif; (3) ulangi proses posting ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
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
													PostingHistory.JENIS_PEMBAYARAN_TAGIHAN_TERMIN);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<PembayaranTerminMasterAssetDetail> pembayaranTerminMasterAssetDetails = initCriteria(
													true).add(Restrictions.isNull("postingHistory")).list();

											for (PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail : pembayaranTerminMasterAssetDetails) {

												if (pembayaranTerminMasterAssetDetail != null) {

													try {

														Akun akunDebet = pembayaranTerminMasterAssetDetail
																.getPemesananPengadaanMasterAsset() == null
																|| pembayaranTerminMasterAssetDetail
																		.getPemesananPengadaanMasterAsset()
																		.getJenisPemesananPengadaanAsset() == null
																				? null
																				: pembayaranTerminMasterAssetDetail
																						.getPemesananPengadaanMasterAsset()
																						.getJenisPemesananPengadaanAsset()
																						.getAkunUtangPekerjaan();

														Akun akunKredit = pembayaranTerminMasterAssetDetail
																.getPemesananPengadaanMasterAsset().getPenyedia()
																.getAkunUtang();
														Double nilai = pembayaranTerminMasterAssetDetail.getDibayar();

														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {

																ket = "Pembayaran termin pembayaran tagihan \""
																		+ (pembayaranTerminMasterAssetDetail
																				.getPemesananPengadaanMasterAsset()
																				.getKodeInvoice()
																				+ "-"
																				+ pembayaranTerminMasterAssetDetail
																						.getPemesananPengadaanMasterAsset()
																						.getPenyedia().getNama()
																				+ "-"
																				+ pembayaranTerminMasterAssetDetail
																						.getKeterangan())
																		+ "\" sebanyak "
																		+ Common.numberFormat.get().format(nilai);

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															Akun akunDenda = null;
															Akun akunPiutangDenda = null;
															Double denda = 0.0;

															SatuanKerja satuanKerja = tbmuser.ambilSatuanKerja();

															if (nilai > 0.1) {
																CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																		akunDenda, akunPiutangDenda, postingHistory,
																		apakahUangMasuk, ket,
																		pembayaranTerminMasterAssetDetail
																				.getTanggalTransaksi(),
																		nilai, denda, pembayaranTerminMasterAssetDetail,
																		satuanKerja, session);
															} else {
																CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																		akunDenda, akunPiutangDenda, postingHistory,
																		apakahUangMasuk, ket,
																		pembayaranTerminMasterAssetDetail
																				.getTanggalTransaksi(),
																		nilai, denda, pembayaranTerminMasterAssetDetail,
																		satuanKerja, session);
															}

															pembayaranTerminMasterAssetDetail
																	.setPostingHistory(postingHistory);
															session.update(pembayaranTerminMasterAssetDetail);
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingPembayaranTerminAction.java:586");
														// error per-item diabaikan agar batch tetap berlanjut
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
	 * <b>Tujuan:</b> Inner class renderer baris grid untuk menampilkan satu entitas
	 * {@code PembayaranTerminMasterAssetDetail} beserta aksi yang dapat dilakukan
	 * pengguna (posting satuan dan batalkan posting satuan).<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Metode {@code render} dipanggil ZK untuk setiap objek dalam model list grid.
	 * Untuk setiap detail pembayaran termin, renderer menampilkan: kode invoice
	 * dengan link ke proses transfer jika ada, link SOP disposisi jika ada, nama
	 * penyedia, jenis pembayaran, nominal, tanggal transaksi, preview jurnal akuntansi
	 * (debet/kredit), status posting dengan nomor bukti, dan tombol aksi. Tombol
	 * "Batalkan Posting" hanya terlihat bagi admin/pemilik hak APPROVE dan hanya
	 * jika baris sudah diposting. Tombol "Posting" hanya terlihat jika baris belum
	 * diposting dan pengguna memiliki hak UPDATE. Posting satuan membuat
	 * {@code PostingHistory} baru per baris.<br>
	 * <br>
	 * <b>Threading:</b> Aksi batalkan dan posting per-baris menggunakan
	 * {@code Common.createDefaultTimer} untuk menghindari blokir UI ZK.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Urutan kolom renderer harus sesuai dengan deklarasi
	 * {@code <columns>} di file ZUL. Jika kolom berubah, renderer ini wajib
	 * diperbarui selaras.
	 */
	class PembayaranTerminMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data detail pembayaran termin ke dalam
		 * komponen ZK Row yang telah disediakan oleh grid.<br>
		 * <br>
		 * <b>Cara kerja:</b><br>
		 * Meng-cast {@code arg1} ke {@code PembayaranTerminMasterAssetDetail}, lalu
		 * mengekstrak parent {@code PembayaranTerminMasterAsset}. Membangun komponen
		 * ZK (Label, A, Hbox, Toolbar, MyToolbarbuttonConfig) dan menambahkannya ke
		 * {@code arg0} (Row) secara berurutan sesuai kolom grid. Akun jurnal diambil
		 * dari relasi entitas secara langsung; jika null, menampilkan pesan validasi
		 * berisi informasi akun yang hilang. Nomor bukti jurnal diambil dari
		 * {@code GrupTransaksi} via Hibernate criteria dengan proyeksi kode.<br>
		 * <br>
		 * <b>Parameter:</b><br>
		 * {@code arg0} — Row ZK tempat komponen akan ditambahkan.<br>
		 * {@code arg1} — objek {@code PembayaranTerminMasterAssetDetail} yang dirender.<br>
		 * <br>
		 * <b>Return:</b> void<br>
		 * <br>
		 * <b>Penanganan error:</b> Jika akun null, baris tetap ditampilkan dengan pesan
		 * deskriptif dan tombol posting tidak dimunculkan. Error di event listener tombol
		 * akan dipropagasikan ke ZK exception handler.<br>
		 * <br>
		 * <b>Pemeliharaan:</b> Pastikan urutan appendChild ke {@code arg0} sesuai urutan
		 * kolom dalam ZUL agar tampilan tidak bergeser.
		 *
		 * @param arg0 Row ZK yang akan diisi komponen
		 * @param arg1 objek data yang akan dirender (PembayaranTerminMasterAssetDetail)
		 * @throws Exception jika terjadi error saat query Hibernate atau konstruksi komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail = (PembayaranTerminMasterAssetDetail) arg1;
			final PembayaranTerminMasterAsset pembayaranTerminMasterAsset = pembayaranTerminMasterAssetDetail
					.getPembayaranTerminMasterAsset();
			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(PembayaranTerminMasterAssetDetail.class,
					pembayaranTerminMasterAssetDetail,
					pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset() == null ? ""
							: pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset().getKodeInvoice()))
					.setParent(arg0);

			if (pembayaranTerminMasterAssetDetail.getDaftarPengajuanTransfer() != null
					&& pembayaranTerminMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer() != null) {
				A a = new A(
						pembayaranTerminMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, pembayaranTerminMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);
			}

			if (pembayaranTerminMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(aaa);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pembayaranTerminMasterAsset.getDisposisiSop().getKeterangan() + " ("
						+ pembayaranTerminMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pembayaranTerminMasterAsset.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			}

			new Label(pembayaranTerminMasterAsset.getPenyedia() == null ? ""
					: pembayaranTerminMasterAsset.getPenyedia().getNama()).setParent(arg0);

			new Label(pembayaranTerminMasterAsset.getJenisPembayaranBarang() == null ? ""
					: pembayaranTerminMasterAsset.getJenisPembayaranBarang().getNama()).setParent(arg0);

			Double nilai = pembayaranTerminMasterAssetDetail.getDibayar();

			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

			new Label(Common.dateFormat3.get().format(pembayaranTerminMasterAssetDetail.getTanggalTransaksi()))
					.setParent(arg0);

			Akun akunDebet = pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset() == null
					|| pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset()
							.getJenisPemesananPengadaanAsset() == null ? null
									: pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset()
											.getJenisPemesananPengadaanAsset().getAkunUtangPekerjaan();

			Akun akunKredit = pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset().getPenyedia()
					.getAkunUtang();

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

			Session session = HibernateUtil.currentSession();
			String bukti = "";
			bukti = (String) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.eq("pembayaranTerminMasterAssetDetail", pembayaranTerminMasterAssetDetail))
					.setMaxResults(1).setProjection(Projections.property("kode")).uniqueResult();

			new Label(pembayaranTerminMasterAssetDetail.getPostingHistory() == null
					? Common.getBahasaConfig("Belum diposting")
					: pembayaranTerminMasterAssetDetail.getPostingHistory().toString() + ", no. bukti : " + bukti)
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && pembayaranTerminMasterAssetDetail.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pembayaranTerminMasterAssetDetail.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pembayaranTerminMasterAssetDetail);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where pembayaran_termin_master_asset_detail="
												+ pembayaranTerminMasterAssetDetail.getId() + " and closing is null")
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
						edit && pembayaranTerminMasterAssetDetail.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PEMBAYARAN_TAGIHAN_TERMIN);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Double nilai = pembayaranTerminMasterAssetDetail.getDibayar();

								Akun akunDebet = pembayaranTerminMasterAssetDetail
										.getPemesananPengadaanMasterAsset() == null
										|| pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset()
												.getJenisPemesananPengadaanAsset() == null
														? null
														: pembayaranTerminMasterAssetDetail
																.getPemesananPengadaanMasterAsset()
																.getJenisPemesananPengadaanAsset()
																.getAkunUtangPekerjaan();

								Akun akunKredit = pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset()
										.getPenyedia().getAkunUtang();

								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {

										ket = "Pembayaran termin pembayaran tagihan \""
												+ (pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset()
														.getKodeInvoice()
														+ "-"
														+ pembayaranTerminMasterAssetDetail
																.getPemesananPengadaanMasterAsset().getPenyedia()
																.getNama()
														+ "-" + pembayaranTerminMasterAssetDetail.getKeterangan())
												+ "\" sebanyak " + Common.numberFormat.get().format(nilai);

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = tbmuser.ambilSatuanKerja();

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket,
												pembayaranTerminMasterAssetDetail.getTanggalTransaksi(), nilai, denda,
												pembayaranTerminMasterAssetDetail, satuanKerja, session);
									} else {
										CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket,
												pembayaranTerminMasterAssetDetail.getTanggalTransaksi(), nilai, denda,
												pembayaranTerminMasterAssetDetail, satuanKerja, session);
									}

									pembayaranTerminMasterAssetDetail.setPostingHistory(postingHistory);
									session.update(pembayaranTerminMasterAssetDetail);
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
	 * <b>Tujuan:</b> Membangun objek {@code Criteria} Hibernate yang merangkum semua
	 * kondisi filter aktif untuk mengambil data {@code PembayaranTerminMasterAssetDetail}
	 * dari database. Criteria ini digunakan baik untuk menghitung jumlah total (paging)
	 * maupun untuk mengambil halaman data.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Membuka sesi Hibernate managed dan membuat criteria pada entitas
	 * {@code PembayaranTerminMasterAssetDetail} dengan join ke alias
	 * {@code pembayaranTerminMasterAsset} dan {@code penyedia}. Filter diterapkan
	 * berdasarkan: status posting (belum/sudah via checkbox), syarat sudah disetujui
	 * (disetujuiOleh tidak null), keberadaan jenis pembayaran atau transfer yang disetujui,
	 * nilai dibayar tidak nol dan tidak null, rentang tanggal transaksi, pemilik aset,
	 * ruangan, serta kode/nama penyedia. Kondisi yang tidak diaktifkan menggunakan
	 * {@code Restrictions.sqlRestriction("1=1")} sebagai passthrough. Jika {@code order}
	 * true, hasil diurutkan berdasarkan ID descending.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code order} — jika true, criteria akan ditambahkan pengurutan {@code Order.desc("id")}.<br>
	 * <br>
	 * <b>Return:</b> Objek {@code Criteria} yang siap dieksekusi untuk query paging atau
	 * pengambilan data.<br>
	 * <br>
	 * <b>Penanganan error:</b> Jika komponen filter belum diinisialisasi (null), guard
	 * null-check pada setiap kondisi mencegah NullPointerException.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Tambahkan filter baru sebagai {@code .add()} tambahan pada
	 * criteria yang sudah ada. Pastikan alias yang diperlukan sudah didefinisikan di atas
	 * sebelum digunakan dalam Restrictions.
	 *
	 * @param order apakah hasil harus diurutkan secara descending berdasarkan ID
	 * @return objek Criteria Hibernate yang merepresentasikan filter aktif
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PembayaranTerminMasterAssetDetail.class)

				.createAlias("pembayaranTerminMasterAsset", "pembayaranTerminMasterAsset")

				.createAlias("pembayaranTerminMasterAsset.penyedia", "penyedia")

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(!searchtelahtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNotNull("postingHistory"))

				.add(Restrictions.isNotNull("pembayaranTerminMasterAsset.disetujuiOleh"))

				.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.isNotNull("pembayaranTerminMasterAsset.jenisPembayaranBarang"),

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
								: Restrictions.eq("pembayaranTerminMasterAsset.pemilikAsset",
										searchpemilikAsset.getSelectedItem().getValue()))

				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pembayaranTerminMasterAsset.ruang", searchruang.getAttribute("ruang"))))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("pembayaranTerminMasterAsset.kode", searchkode.getValue(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("penyedia.nama", searchkode.getValue(), MatchMode.ANYWHERE)));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Melakukan pencarian dan pemuatan data ke grid tanpa menampilkan
	 * indikator progress. Metode ini adalah inti logika tampil data yang dipanggil
	 * dari dalam mekanisme progress wrapper.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.initPaging} untuk menghitung total baris dan mengatur
	 * state paging, lalu mengambil satu halaman data dari database menggunakan
	 * {@code initCriteria(true)} dengan batas {@code Common.ROWS_COUNT_ON_PAGE} baris
	 * dimulai dari offset sesuai halaman aktif. Hasil dimasukkan ke
	 * {@code SimpleListModel} dan diterapkan ke grid bersama renderer.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK asal pemanggil (boleh null), tidak digunakan secara langsung.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate akan dipropagasikan. Pastikan sesi
	 * Hibernate aktif saat metode ini dipanggil.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Metode ini tidak boleh dipanggil langsung dari event handler
	 * ZK; gunakan {@code loadDataDenganProgressPosting} agar indikator progress
	 * ditampilkan dengan benar.
	 *
	 * @param event event ZK asal pemanggil, boleh null
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PembayaranTerminMasterAsset> pembayaranTerminMasterAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pembayaranTerminMasterAsset);
		grid.setRowRenderer(new PembayaranTerminMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Entry point publik untuk memuat ulang data grid, dipanggil dari
	 * event handler ZK (misalnya setelah operasi simpan, posting, atau batalkan)
	 * maupun dari tombol pencarian.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Mendelegasikan sepenuhnya ke {@code loadDataDenganProgressPosting(event)}
	 * agar mekanisme progress dan koalesensi reload aktif.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK pemicu, diteruskan ke metode delegate.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Didelegasikan ke metode yang dipanggil.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Metode ini wajib tetap ada karena ZK memanggil metode
	 * bernama {@code onSearchDefault} secara konvensional melalui event binding ZUL.
	 *
	 * @param event event ZK pemicu reload, dapat null
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Flag apakah proses loading data posting jurnal sedang berjalan.
	 * Digunakan untuk mencegah eksekusi ganda yang dapat menyebabkan
	 * race condition pada state grid.
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag apakah ada permintaan reload yang masuk saat loading sedang berjalan.
	 * Jika true, setelah loading selesai akan otomatis dilakukan reload ulang
	 * untuk memastikan data terbaru ditampilkan.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * <b>Tujuan:</b> Memuat data posting jurnal pembayaran termin ke dalam grid
	 * dengan menampilkan indikator progress bertahap kepada pengguna, sekaligus
	 * mencegah eksekusi ganda (koalesensi reload).<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Jika loading sedang aktif ({@code postingJurnalLoadingAktif} true), permintaan
	 * baru ditandai sebagai tertunda ({@code postingJurnalReloadTertunda}) dan
	 * indikator progress diperbarui untuk memberi tahu pengguna. Jika tidak ada
	 * loading aktif, flag diaktifkan dan {@code PostingJurnalLoadingUtil.show}
	 * dipanggil untuk memulai tampilan progress. Data diambil melalui timer default
	 * ZK agar UI sempat diperbarui sebelum query berjalan. Setelah query selesai,
	 * progress diperbarui ke tahap hampir selesai. Di blok {@code finally}, flag
	 * loading direset. Jika ada reload tertunda, proses dimulai ulang secara rekursif
	 * via timer baru; jika tidak, progress ditandai selesai.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK original pemicu reload, diteruskan ke
	 * {@code onSearchDefaultTanpaProgress} dan reload ulang jika tertunda.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Query dalam blok try dilindungi oleh finally sehingga
	 * flag loading selalu direset meskipun terjadi exception. Exception dari
	 * {@code onSearchDefaultTanpaProgress} akan dipropagasikan ke ZK exception handler.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Mekanisme koalesensi ini efektif untuk satu thread UI ZK.
	 * Jangan mengakses {@code postingJurnalLoadingAktif} dari thread latar karena
	 * tidak thread-safe. Progress percentage (7, 48, 92, 96, 100) dapat disesuaikan
	 * untuk pengalaman visual yang lebih baik.
	 *
	 * @param event event ZK pemicu, dapat null jika dipanggil dari inisialisasi
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
	 * Kriteria dokumen yang SAMA dengan baris "Pembayaran Termin Vendor" di dasbor draft jurnal:
	 * detail pembayaran termin yang induknya sudah disetujui, punya sumber akun kredit, dan nilai
	 * dibayar tidak nol, pada rentang tanggal transaksi.
	 *
	 * <p>PERBEDAAN dari {@code initCriteria} layar: klausa tanggalnya DIBERI KURUNG. Versi layar
	 * menulis "this_.tanggal_transaksi is null or date(...) between ..." telanjang di dalam
	 * sqlRestriction; Hibernate tidak membungkusnya, sehingga presedensi AND/OR SQL membuat cabang
	 * "between" lolos dari seluruh filter lain. Baris tanpa tanggal tetap ikut terpilih, mengikuti
	 * maksud layar.</p>
	 */
	private static Criteria kriteriaPembayaranTerminStatic(Session session, java.util.Date mulai,
			java.util.Date sampai) {
		Criteria c = session.createCriteria(PembayaranTerminMasterAssetDetail.class)
				.createAlias("pembayaranTerminMasterAsset", "pembayaranTerminMasterAsset")
				.add(Restrictions.isNotNull("pembayaranTerminMasterAsset.disetujuiOleh"))
				.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer", Criteria.LEFT_JOIN)
				.add(Restrictions.or(
						Restrictions.isNotNull("pembayaranTerminMasterAsset.jenisPembayaranBarang"),
						Restrictions.and(Restrictions.isNotNull("daftarPengajuanTransfer.prosesTransfer"),
								Restrictions.eq("daftarPengajuanTransfer.transfer", true))))
				.add(Restrictions.ne("dibayar", 0.0)).add(Restrictions.isNotNull("dibayar"));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("(this_.tanggal_transaksi is null"
					+ " or date(this_.tanggal_transaksi) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "'))"));
		}
		return c;
	}

	/**
	 * Posting SEMUA pembayaran termin vendor pada rentang -- jalur API dasbor Draft Jurnal POS.
	 * Jurnal per dokumen mengikuti tombol layar: debet akun utang pekerjaan jenis pemesanan,
	 * kredit akun utang penyedia pemesanan, senilai dibayar (&le; 0.1 memutar posisi); satuan
	 * kerja diambil dari pengguna yang memposting, sama dengan layar. Penanda hanya dicap bila
	 * jurnal tersimpan; riwayat diberi {@code posting=true} -- lihat catatan di
	 * {@code PostingPembayaranAction.postingSemua}.
	 */
	public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<?> daftar = kriteriaPembayaranTerminStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory")).list();
			if (daftar.isEmpty()) {
				return 0;
			}

			PostingHistory postingHistory = new PostingHistory(
					PostingHistory.JENIS_PEMBAYARAN_TAGIHAN_TERMIN);
			postingHistory.setTbmuser(oleh);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTanggalPosting(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setPosting(true);
			postingHistory.setKeterangan("Posting massal pembayaran termin vendor dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			ais.database.model.rab.SatuanKerja satuanKerja = oleh == null ? null
					: oleh.ambilSatuanKerja();

			for (Object o : daftar) {
				PembayaranTerminMasterAssetDetail d = (PembayaranTerminMasterAssetDetail) o;
				if (d == null || d.getPemesananPengadaanMasterAsset() == null) {
					continue;
				}
				try {
					Akun akunDebet = d.getPemesananPengadaanMasterAsset()
							.getJenisPemesananPengadaanAsset() == null ? null
									: d.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset()
											.getAkunUtangPekerjaan();
					Akun akunKredit = d.getPemesananPengadaanMasterAsset().getPenyedia() == null ? null
							: d.getPemesananPengadaanMasterAsset().getPenyedia().getAkunUtang();
					Double nilai = d.getDibayar();
					if (akunDebet == null || akunKredit == null) {
						continue;
					}

					String ket = "Pembayaran termin pembayaran tagihan sebanyak "
							+ Common.numberFormat.get().format(nilai);
					try {
						ket = "Pembayaran termin pembayaran tagihan \""
								+ d.getPemesananPengadaanMasterAsset().getKodeInvoice() + "-"
								+ d.getPemesananPengadaanMasterAsset().getPenyedia().getNama() + "-"
								+ d.getKeterangan() + "\" sebanyak "
								+ Common.numberFormat.get().format(nilai);
					} catch (Exception e) {
						// Pemesanannya boleh tidak lengkap; kalimat baku di atas tetap terpakai.
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
					ais.common.ErrorAuditUtil.record(e, "PostingPembayaranTerminAction jalur API");
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
			List<?> daftar = kriteriaPembayaranTerminStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (Object o : daftar) {
				PembayaranTerminMasterAssetDetail d = (PembayaranTerminMasterAssetDetail) o;
				if (d == null) {
					continue;
				}
				try {
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where pembayaran_termin_master_asset_detail="
							+ d.getId() + " and closing is null)").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where pembayaran_termin_master_asset_detail="
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
					ais.common.ErrorAuditUtil.record(e, "kriteriaPembayaranTerminStatic jalur API");
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
