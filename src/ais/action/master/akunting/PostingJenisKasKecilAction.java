package ais.action.master.akunting;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
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
import org.zkoss.zul.Columns;
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
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.JenisKasKecil;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>PostingJenisKasKecilAction — Pengendali Halaman Posting Saldo Awal Kas Kecil</h3>
 *
 * <p><strong>Untuk apa:</strong>
 * Kelas ini adalah ZK Composer yang mengelola proses posting transaksi saldo awal
 * kas kecil ke dalam buku besar akuntansi. Kas kecil (petty cash) adalah dana tunai
 * kecil yang dikelola oleh suatu unit untuk pengeluaran operasional sehari-hari
 * yang tidak memerlukan proses pembayaran formal. Saldo awal kas kecil perlu diposting
 * ke jurnal umum agar tercatat dalam sistem akuntansi sebagai aset kas yang diakui.
 * Kelas ini menyediakan antarmuka untuk melihat daftar entitas {@link JenisKasKecil}
 * yang siap diposting, melakukan posting individual per baris maupun secara massal,
 * serta membatalkan posting jika diperlukan.</p>
 *
 * <p><strong>Cara kerja:</strong>
 * Grid menampilkan entitas {@link JenisKasKecil} yang memiliki referensi ke
 * {@code daftarPengajuanTransfer} dengan {@code prosesTransfer} tidak null, serta
 * saldo awal ({@code saldoAwal}) yang tidak nol. Logika penentuan akun kredit untuk
 * jurnal bersifat kondisional:
 * <ul>
 *   <li>Jika pengajuan transfer bersifat transitori ({@code transitori = true}):
 *       akun kredit diambil dari {@code caraPembayaranTransfer.akunTransitori}.</li>
 *   <li>Jika pengajuan transfer bersifat transfer biasa ({@code transfer = true}):
 *       akun kredit diambil dari {@code caraPembayaranTransfer.akun}.</li>
 *   <li>Selain itu: akun kredit null dan transaksi tidak dapat diposting.</li>
 * </ul>
 *
 * <p>Akun debet selalu diambil dari field {@code jenisKasKecil.akun} (akun kas kecil
 * itu sendiri). Filter tersedia berdasarkan satuan kerja dengan dukungan hierarki
 * organisasi, rentang tanggal, status posting, dan kata kunci teks bebas.</p>
 *
 * <p><strong>Threading:</strong>
 * Operasi posting massal berjalan dalam {@link Thread} terpisah menggunakan
 * {@code HibernateUtil.currentNativeSession()}. Sesi ini harus dikelola manual
 * (begin/commit/close). Setelah loop selesai, sesi ditutup dengan
 * {@code HibernateUtil.closeSession()}.</p>
 *
 * <p><strong>Pemeliharaan:</strong>
 * Logika penentuan akun kredit kondisional (transitori vs transfer vs null) harus
 * dipertahankan konsistensinya antara loop massal, renderer, dan posting per baris.
 * Konstanta yang digunakan: {@code PostingHistory.JENIS_SALDO_AWAL_KAS_KECIL}.</p>
 *
 * @author Generated Javadoc
 * @see JenisKasKecil
 * @see PostingHistory
 * @see CommonAkunting
 */
public class PostingJenisKasKecilAction extends GenericAutowireComposer {

	/**
	 * ID serialisasi untuk kompatibilitas serialisasi Java.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar jenis kas kecil yang akan diposting. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman data grid. */
	private Paging paging;

	/** Kotak teks pencarian berdasarkan kode, nama, atau keterangan kas kecil. */
	private Textbox searchkode;

	/** Checkbox filter untuk menampilkan hanya transaksi yang belum diposting. */
	private MyCheckboxConfig searchtampil;

	/** Checkbox filter untuk menampilkan hanya transaksi yang sudah diposting. */
	private MyCheckboxConfig searchtelahtampil;

	/**
	 * Model pohon satuan kerja untuk mendukung filter hierarki organisasi.
	 * Memungkinkan filter mencakup satuan kerja induk beserta seluruh turunannya.
	 */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Komponen banbox pemilih satuan kerja pada filter halaman.
	 * Perubahan nilai komponen ini memicu {@code onSearchDefault}.
	 */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** Flag apakah pengguna memiliki hak ubah (UPDATE) pada modul ini. */
	private boolean edit = false;

	/** Tombol toolbar posting semua, visibilitasnya dikontrol oleh flag {@code edit}. */
	private MyToolbarbuttonConfig sent;

	/**
	 * Flag apakah pengguna adalah admin atau memiliki hak APPROVE,
	 * menentukan apakah tombol batalkan posting ditampilkan.
	 */
	public boolean adminLain;

	/** Tanggal awal rentang filter data kas kecil berdasarkan tanggal pembukaan. */
	private MyDatebox tglMulai;

	/** Tanggal akhir rentang filter data kas kecil. */
	private MyDatebox tglSampai;

	/** Data pengguna yang sedang login, diambil saat inisialisasi halaman. */
	private Tbmuser tbmuser;

	/**
	 * <h3>doBeforeCompose — Pemeriksaan Keamanan Sebelum Halaman Dibangun</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Dipanggil ZK sebelum komponen halaman diinisialisasi. Memastikan pengguna
	 * terautentikasi dan berhak mengakses halaman posting saldo awal kas kecil.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Memanggil {@link Common#doCheckSecurity()} untuk pemeriksaan sesi dan otorisasi,
	 * kemudian memanggil implementasi super untuk melanjutkan proses komposisi.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Pemeriksaan keamanan ini wajib ada dan tidak boleh dihapus dari metode ini.</p>
	 *
	 * @param page     halaman ZK yang sedang dikomposisi
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo metadata informasi komponen
	 * @return informasi komponen dari implementasi super
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <h3>doAfterCompose — Inisialisasi Penuh Halaman Setelah Komponen ZUL Siap</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Dipanggil ZK setelah semua komponen ZUL selesai di-wire ke field kelas ini.
	 * Bertanggung jawab atas inisialisasi lengkap halaman: validasi sesi, konfigurasi
	 * filter satuan kerja, pengaturan tanggal default, penentuan hak akses, pemuatan
	 * data awal, serta registrasi listener paging dan timer auto-refresh.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk auto-wire.</li>
	 *   <li>Menginisialisasi bahasa antarmuka.</li>
	 *   <li>Memvalidasi sesi dan hak READ; jika gagal, redirect logout.</li>
	 *   <li>Mengambil data pengguna aktif ke {@code tbmuser}.</li>
	 *   <li>Mengonfigurasi {@code searchparent} dengan listener auto-reload.</li>
	 *   <li>Menginisialisasi {@link SatuanKerjaTreeModel}.</li>
	 *   <li>Mengatur rentang tanggal default: 6 bulan ke belakang hingga hari ini,
	 *       keduanya dalam mode read-only.</li>
	 *   <li>Menentukan flag {@code adminLain} dan {@code edit}.</li>
	 *   <li>Memuat data pertama kali dan mendaftarkan listener paging dan timer.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Kegagalan validasi sesi menyebabkan redirect logout dan return awal.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Inisialisasi {@code searchparent} harus dilakukan sebelum pemuatan data pertama
	 * agar filter satuan kerja aktif sejak awal.</p>
	 *
	 * @param comp komponen akar hasil komposisi ZUL
	 * @throws Exception jika terjadi kesalahan selama inisialisasi
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

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (tglSampai != null) tglSampai.setValue(ais.ui.util.WaktuUtil.getDate());
		if (tglMulai != null) tglMulai.setValue(calendar.getTime());
		if (tglMulai != null) tglMulai.setReadonly(true);
		if (tglSampai != null) tglSampai.setReadonly(true);

		adminLain = Common.getApakahAdmin() || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		if (sent != null) { sent.setVisible(edit); }

		loadDataDenganProgressPosting(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDenganProgressPosting(null);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDenganProgressPosting(null);

			}
		});

	}

	/**
	 * <h3>onBatalkanPostingSemua — Pembatalan Massal Seluruh Posting Saldo Awal Kas Kecil</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Menangani klik tombol "Batalkan Posting Semua" untuk membatalkan seluruh posting
	 * transaksi saldo awal kas kecil yang memenuhi filter aktif. Semua transaksi yang
	 * sudah diposting dikembalikan ke status "belum diposting" dan entri jurnal
	 * terkait dihapus dari basis data.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Menampilkan dialog konfirmasi. Jika dikonfirmasi:
	 * <ol>
	 *   <li>Mengambil semua {@link JenisKasKecil} yang memenuhi filter dan sudah diposting.</li>
	 *   <li>Untuk setiap entitas: set {@code postingHistory} ke null, simpan,
	 *       hapus baris {@link GrupTransaksi} dengan kolom {@code jenis_kas_kecil}
	 *       yang bukan closing entry.</li>
	 *   <li>Muat ulang grid via timer default setelah selesai.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Tidak ada rollback otomatis per iterasi. Operasi satu per satu dalam sesi yang
	 * dikelola.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Query SQL menggunakan kolom {@code jenis_kas_kecil} pada tabel
	 * {@code akunting.grup_transaksi}. Sesuaikan jika nama kolom berubah.</p>
	 *
	 * @param event event ZK dari klik tombol
	 * @throws Exception jika terjadi kesalahan akses basis data atau UI
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi kas kecil ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<JenisKasKecil> jenisKasKecils = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (JenisKasKecil jenisKasKecil : jenisKasKecils) {
								jenisKasKecil.setPostingHistory(null);
								Common.refreshSaveOrUpdate(jenisKasKecil);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
												+ " (select id from akunting.grup_transaksi where jenis_kas_kecil="
												+ jenisKasKecil.getId() + " and closing is null)")
										.executeUpdate();
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where jenis_kas_kecil="
												+ jenisKasKecil.getId() + " and closing is null")
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
	 * <h3>onPostingSemua — Pembukaan Dialog dan Eksekusi Posting Massal Saldo Awal Kas Kecil</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Menangani klik tombol "Posting Semua Saldo Kas Kecil" dengan membuka dialog
	 * konfirmasi, memvalidasi input tanggal, lalu menjalankan proses posting seluruh
	 * kas kecil yang belum diposting dalam utas latar belakang.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Menampilkan jendela modal "Posting Transaksi Saldo Kas Kecil" dengan form
	 *       tanggal, nama pengguna, dan keterangan.</li>
	 *   <li>Validasi: tanggal wajib diisi sebelum lanjut.</li>
	 *   <li>Konfirmasi kedua sebelum proses dimulai.</li>
	 *   <li>Dalam utas latar belakang: membuat {@link PostingHistory} baru dengan
	 *       jenis {@code JENIS_SALDO_AWAL_KAS_KECIL}.</li>
	 *   <li>Untuk setiap {@link JenisKasKecil} yang belum diposting:
	 *       <ul>
	 *         <li>Mengambil satuan kerja entitas atau null.</li>
	 *         <li>Akun debet: {@code jenisKasKecil.akun}.</li>
	 *         <li>Akun kredit: dipilih secara kondisional — transitori maka
	 *             {@code akunTransitori}, transfer maka {@code akun} dari cara
	 *             pembayaran, selain itu null.</li>
	 *         <li>Jika kedua akun tersedia: memanggil {@link CommonAkunting#saveTransaksi}
	 *             dengan saldo awal sebagai nilai dan tanggal pembukaan kas kecil.</li>
	 *         <li>Memperbarui field {@code postingHistory} entitas.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Setelah loop: menutup sesi native dan mengosongkan label progres.</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong>
	 * Loop berjalan di utas non-ZK menggunakan {@code currentNativeSession()}.
	 * Jangan memanggil komponen ZK lain selain {@code label.setValue()}.</p>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Try-catch per iterasi dengan pelaporan ke admin. Error keseluruhan loop
	 * tidak menghentikan proses.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Logika pemilihan akun kredit kondisional (transitori/transfer/null) harus
	 * identik dengan logika di {@link JenisKasKecilRenderer#render}.</p>
	 *
	 * @param event event ZK dari klik tombol
	 * @throws Exception jika terjadi kesalahan saat membangun komponen UI jendela
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Transaksi Saldo Kas Kecil");
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
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting belum diisi. Langkah yang dapat dilakukan: (1) Isikan atau pilih Tanggal Posting menggunakan date picker; (2) Pastikan tanggal yang dipilih valid dan sesuai periode akuntansi berjalan; (3) ulangi proses posting. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				// if (keterangan.getValue().trim().equals("")) {
				// MyMessageboxConfig.show("Keterangan harus diisi",
				// "Peringatan", MyMessageboxConfig.OK,
				// MyMessageboxConfig.EXCLAMATION);
				// return;
				// }

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi saldo kas kecil ?", "Pertanyaan",
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
													"Posting transaksi saldo kas kecil berhasil dilakukan", "Informasi",
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
													PostingHistory.JENIS_SALDO_AWAL_KAS_KECIL);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<JenisKasKecil> jenisKasKecils = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (JenisKasKecil jenisKasKecil : jenisKasKecils) {

												SatuanKerja satuanKerja = (SatuanKerja) (jenisKasKecil
														.getSatuanKerja() != null ? jenisKasKecil.getSatuanKerja()
																: null);

												if (jenisKasKecil != null) {

													try {
														Akun akunDebet = jenisKasKecil.getAkun();
														Akun akunKredit = jenisKasKecil.getDaftarPengajuanTransfer()
																.getTransitori()
																		? jenisKasKecil.getDaftarPengajuanTransfer()
																				.getProsesTransfer()
																				.getCaraPembayaranTransfer()
																				.getAkunTransitori()
																		: jenisKasKecil.getDaftarPengajuanTransfer()
																				.getTransfer()
																						? jenisKasKecil
																								.getDaftarPengajuanTransfer()
																								.getProsesTransfer()
																								.getCaraPembayaranTransfer()
																								.getAkun()
																						: null;

														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {
																ket = "Saldo awal kas kecil \""
																		+ jenisKasKecil.getKode()
																		+ "\" pada pengeluaran \""
																		+ jenisKasKecil.getNama() + "\" senilai "
																		+ Common.numberFormat.get()
																				.format(jenisKasKecil.getSaldoAwal());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(ket + " ("
																	+ Common.numberFormat.get().format(
																			rowIndex * 100.0 / jenisKasKecils.size())
																	+ " %)");

															Double nilai = jenisKasKecil.getSaldoAwal();

															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																session.getTransaction().begin();

																if (nilai > 0.1) {
																	CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			jenisKasKecil.getTanggal(), nilai, denda,
																			jenisKasKecil, satuanKerja, session);
																} else {
																	CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			jenisKasKecil.getTanggal(), nilai, denda,
																			jenisKasKecil, satuanKerja, session);
																}

																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															jenisKasKecil.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(jenisKasKecil);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PostingJenisKasKecilAction.java:608");
														// TODO: handle
														// exception
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
	 * <h3>JenisKasKecilRenderer — Renderer Baris Grid Saldo Awal Kas Kecil</h3>
	 *
	 * <p><strong>Untuk apa:</strong>
	 * Kelas dalam ini merender setiap baris grid halaman posting saldo awal kas kecil.
	 * Setiap baris merepresentasikan satu entitas {@link JenisKasKecil} dan menampilkan
	 * informasi: kode (dengan link proses transfer), nama, saldo awal, tanggal
	 * pembukaan, pasangan jurnal debet-kredit, status posting, serta tombol aksi.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Memperluas {@code ais.ui.util.MyRowRenderer}. Penentuan akun kredit menggunakan
	 * logika kondisional yang sama dengan loop massal: transitori → akunTransitori,
	 * transfer → akun dari cara pembayaran, selain itu null. Tombol aksi posting dan
	 * batalkan posting hanya ditampilkan jika kedua akun tersedia.</p>
	 *
	 * <p><strong>Threading:</strong>
	 * Berjalan di utas ZK event. Aman menggunakan {@code currentSession()}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Logika akun kredit kondisional harus konsisten dengan implementasi di loop massal
	 * pada {@link #onPostingSemua}. Perubahan pada satu tempat harus direplikasi ke tempat lain.</p>
	 */
	class JenisKasKecilRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render — Mengisi Satu Baris Grid dengan Data Saldo Awal Kas Kecil</h3>
		 *
		 * <p><strong>Tujuan:</strong>
		 * Mengisi {@link Row} ZK dengan semua informasi visual dan interaktif dari satu
		 * entitas {@link JenisKasKecil}, termasuk tombol aksi yang dikondisikan berdasarkan
		 * status posting dan hak akses pengguna.</p>
		 *
		 * <p><strong>Cara kerja:</strong>
		 * <ol>
		 *   <li>Menyetel vertical-align baris ke "top".</li>
		 *   <li>Menampilkan kode dalam {@link Vbox} via {@code RevisiHelper}, dengan link
		 *       ke proses transfer jika {@code daftarPengajuanTransfer.prosesTransfer}
		 *       tidak null.</li>
		 *   <li>Menampilkan nama kas kecil sebagai {@link Label}.</li>
		 *   <li>Menghitung dan menampilkan saldo awal ({@code jenisKasKecil.getSaldoAwal()})
		 *       dalam format angka.</li>
		 *   <li>Menampilkan tanggal pembukaan kas kecil.</li>
		 *   <li>Menentukan akun debet ({@code jenisKasKecil.akun}) dan akun kredit
		 *       (kondisional: transitori → akunTransitori, transfer → akun, null).</li>
		 *   <li>Menampilkan visualisasi jurnal atau pesan error akun tidak lengkap.</li>
		 *   <li>Mencari nomor bukti {@link GrupTransaksi} dan menampilkan status posting.</li>
		 *   <li>Membuat toolbar dengan tombol batalkan posting dan posting per baris,
		 *       hanya jika akun debet dan kredit keduanya tersedia.</li>
		 * </ol>
		 *
		 * <p><strong>Penanganan error:</strong>
		 * Tombol aksi hanya ditampilkan jika akun lengkap. Keterangan error ditampilkan
		 * di kolom jurnal jika akun tidak tersedia.</p>
		 *
		 * <p><strong>Pemeliharaan:</strong>
		 * Pastikan query {@link GrupTransaksi} menggunakan properti {@code "jenisKasKecil"}
		 * sesuai mapping Hibernate. Logika akun kredit kondisional harus identik dengan
		 * yang ada di {@link #onPostingSemua}.</p>
		 *
		 * @param arg0 baris ZK yang akan diisi komponen
		 * @param arg1 objek data, diharapkan bertipe {@link JenisKasKecil}
		 * @throws Exception jika terjadi kesalahan akses basis data atau pembuatan komponen
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisKasKecil jenisKasKecil = (JenisKasKecil) arg1;

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(JenisKasKecil.class, jenisKasKecil,
					jenisKasKecil.getKode() == null ? "" : jenisKasKecil.getKode())).setParent(arg0);

			if (jenisKasKecil.getDaftarPengajuanTransfer() != null
					&& jenisKasKecil.getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A a = new A(jenisKasKecil.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, jenisKasKecil.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);
			}

			new Label(jenisKasKecil.getNama()).setParent(arg0);

			Double nilai = jenisKasKecil.getSaldoAwal();

			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

			new Label(Common.dateFormat3.get().format(jenisKasKecil.getTanggal())).setParent(arg0);

			Akun akunDebet = jenisKasKecil.getAkun();
			Akun akunKredit = jenisKasKecil.getDaftarPengajuanTransfer().getTransitori()
					? jenisKasKecil.getDaftarPengajuanTransfer().getProsesTransfer().getCaraPembayaranTransfer()
							.getAkunTransitori()
					: jenisKasKecil.getDaftarPengajuanTransfer().getTransfer()
							? jenisKasKecil.getDaftarPengajuanTransfer().getProsesTransfer().getCaraPembayaranTransfer()
									.getAkun()
							: null;

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
					.add(Restrictions.eq("jenisKasKecil", jenisKasKecil)).setMaxResults(1)
					.setProjection(Projections.property("kode")).uniqueResult();

			new Label(jenisKasKecil.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: jenisKasKecil.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && jenisKasKecil.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								jenisKasKecil.setPostingHistory(null);
								Common.refreshSaveOrUpdate(jenisKasKecil);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
												+ " (select id from akunting.grup_transaksi where jenis_kas_kecil="
												+ jenisKasKecil.getId() + " and closing is null)")
										.executeUpdate();
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where jenis_kas_kecil="
												+ jenisKasKecil.getId() + " and closing is null")
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
				button.setVisible(edit && jenisKasKecil.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_SALDO_AWAL_KAS_KECIL);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);
								Akun akunDebet = jenisKasKecil.getAkun();

								Akun akunKredit = jenisKasKecil.getDaftarPengajuanTransfer().getTransitori()
										? jenisKasKecil.getDaftarPengajuanTransfer().getProsesTransfer()
												.getCaraPembayaranTransfer().getAkunTransitori()
										: jenisKasKecil.getDaftarPengajuanTransfer().getTransfer()
												? jenisKasKecil.getDaftarPengajuanTransfer().getProsesTransfer()
														.getCaraPembayaranTransfer().getAkun()
												: null;

								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									Double nilai = jenisKasKecil.getSaldoAwal();

									String ket = "";
									try {
										ket = "Saldo awal kas kecil \"" + jenisKasKecil.getKode()
												+ "\" pada pengeluaran \"" + jenisKasKecil.getNama() + "\" senilai "
												+ Common.numberFormat.get().format(jenisKasKecil.getSaldoAwal());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (jenisKasKecil.getSatuanKerja() != null
											? jenisKasKecil.getSatuanKerja()
											: tbmuser.ambilSatuanKerja());

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket, jenisKasKecil.getTanggal(), nilai,
												denda, jenisKasKecil, satuanKerja, session);
									} else {
										CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket, jenisKasKecil.getTanggal(), nilai,
												denda, jenisKasKecil, satuanKerja, session);
									}

									jenisKasKecil.setPostingHistory(postingHistory);
									session.update(jenisKasKecil);
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
	 * <h3>initCriteria — Membangun Kriteria Query Hibernate untuk Data Saldo Awal Kas Kecil</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Membangun objek {@link Criteria} Hibernate yang mencerminkan semua filter aktif
	 * di halaman ini: satuan kerja dengan hierarki, status posting, kondisi wajib bisnis
	 * (prosesTransfer tidak null, saldoAwal tidak nol), rentang tanggal, dan kata kunci.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Mengambil satuan kerja yang dipilih dari {@code searchparent} dan mengumpulkan
	 *       turunannya via {@code SatuanKerjaTreeModel}.</li>
	 *   <li>Membuat join ke {@code daftarPengajuanTransfer} dan memfilter wajib:
	 *       {@code prosesTransfer} tidak null (kas kecil harus terhubung ke proses transfer).</li>
	 *   <li>Filter satuan kerja: global (null) atau dalam set yang dipilih.</li>
	 *   <li>Filter status posting berdasarkan checkbox.</li>
	 *   <li>Filter wajib: saldo awal tidak 0.0 dan tidak null.</li>
	 *   <li>Filter rentang tanggal menggunakan fungsi SQL {@code date(this_.tanggal)}.</li>
	 *   <li>Filter kata kunci pada kode, nama, dan keterangan.</li>
	 *   <li>Jika {@code order} true, ORDER BY id descending.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Tidak ada penanganan eksplisit. Kesalahan Hibernate dilempar ke pemanggil.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Alias {@code "daftarPengajuanTransfer"} digunakan untuk filter wajib prosesTransfer.
	 * Pastikan nama alias konsisten dengan nama properti di entitas {@link JenisKasKecil}.</p>
	 *
	 * @param order true untuk menambahkan ORDER BY, false untuk query COUNT
	 * @return objek {@link Criteria} siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(JenisKasKecil.class)

				.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer")

				.add(Restrictions.isNotNull("daftarPengajuanTransfer.prosesTransfer"))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas))))

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(!searchtelahtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNotNull("postingHistory"))

				.add(Restrictions.ne("saldoAwal", 0.0)).add(Restrictions.isNotNull("saldoAwal"))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.tanggal) between date('" + Common.databaseDateFormat.get().format(tglMulai.getValue())
								+ "') and  date('" + Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("nama", searchkode.getValue(), MatchMode.ANYWHERE),
										Restrictions.ilike("keterangan", searchkode.getValue(), MatchMode.ANYWHERE))));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <h3>onSearchDefaultTanpaProgress — Pemuatan Data Grid Saldo Awal Kas Kecil Tanpa Progres</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Melakukan pemuatan data aktual ke grid: menghitung total baris untuk paging,
	 * mengambil data sesuai halaman aktif, dan mengatur model serta renderer grid.
	 * Dipanggil dari dalam {@link #loadDataDenganProgressPosting}.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Menghitung total halaman, mengambil baris halaman aktif, membungkus dalam
	 * {@link SimpleListModel}, lalu menyetel renderer {@link JenisKasKecilRenderer}
	 * dan model ke grid.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Selalu panggil via {@link #loadDataDenganProgressPosting}, bukan langsung.</p>
	 *
	 * @param event event pemicu (dapat null)
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisKasKecil> jenisKasKecil = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisKasKecil);
		grid.setRowRenderer(new JenisKasKecilRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <h3>onSearchDefault — Titik Masuk Publik Pencarian Data Saldo Awal Kas Kecil</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Titik masuk standar yang dipanggil ZK untuk memuat ulang data grid saldo awal
	 * kas kecil. Mendelegasikan ke {@link #loadDataDenganProgressPosting} agar
	 * indikator progres selalu aktif selama pemuatan data.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Jangan menambahkan logika di sini. Semua logika pemuatan ada di
	 * {@link #loadDataDenganProgressPosting}.</p>
	 *
	 * @param event event ZK pemicu pencarian (dapat null)
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Flag pengaman untuk mencegah dua permintaan load berjalan bersamaan.
	 * True berarti proses loading sedang aktif.
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag yang menandai ada permintaan reload tertunda karena loading sebelumnya
	 * masih berjalan.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * <h3>loadDataDenganProgressPosting — Pemuatan Data dengan Indikator Progres Bertahap</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Orkestrasi utama pemuatan data grid posting saldo awal kas kecil dengan tampilan
	 * indikator progres bertahap. Mencegah race condition saat beberapa permintaan
	 * reload datang bersamaan dengan menggunakan pola "loading flag dengan antrian tunggal".</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Jika sudah loading: catat permintaan tertunda, tampilkan notifikasi, return.</li>
	 *   <li>Jika belum: set flag aktif, tampilkan progres 7%, jadwalkan via timer ZK.</li>
	 *   <li>Dalam callback timer: tampilkan progres 48%, muat data, tampilkan 92%.</li>
	 *   <li>Blok finally: bersihkan flag. Jika ada tertunda, jadwalkan ulang rekursif.
	 *       Jika tidak, tandai selesai 100%.</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong>
	 * Seluruh logika di utas ZK event. Timer memberikan jeda untuk rendering browser.</p>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Blok finally memastikan flag dibersihkan meskipun ada exception, mencegah
	 * halaman terkunci dalam state loading.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Pola ini identik di semua kelas posting. Jika perilaku perlu diubah,
	 * pertimbangkan ekstraksi ke helper bersama.</p>
	 *
	 * @param event event pemicu (dapat null bila dipanggil internal)
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
	 * Kriteria dokumen saldo awal kas kecil yang SAMA dengan baris "Saldo Awal Kas Kecil" di
	 * dasbor draft jurnal ({@code DraftJurnalRingkasanUtil}): jenis kas kecil yang terhubung
	 * proses transfer (syarat wajib layar) dan saldo awalnya tidak nol, pada rentang tanggal
	 * pembukaan. Kata kunci pencarian layar tidak ikut, tetapi filter satuan kerja TETAP
	 * diterapkan -- lihat catatan di bawah; klaim javadoc lama ("dasbor menghitung global")
	 * mendokumentasikan celah ini sebagai fitur, padahal method yang sama dipakai
	 * {@link #postingSemua} untuk MEMPOSTING (memutasi) dokumen, bukan sekadar menghitung.
	 */
	private static Criteria kriteriaSaldoAwalKasKecilStatic(Session session, java.util.Date mulai,
			java.util.Date sampai) {
		// Cakupan penyewa (satuan kerja): tanpa ini, jalur API men-scan/memposting
		// dokumen saldo awal kas kecil SELURUH instalasi (lintas Yayasan), bukan hanya
		// milik penyewa yang sedang memanggil -- lihat catatan sama pada
		// PostingTransaksiPembayaranGajiAction.kriteriaPostingStatic(). Himpunan kosong
		// (Yayasan tidak teridentifikasi) fail-CLOSED, bukan fail-open seperti
		// initCriteria(boolean) pada layar ZK.
		Set<SatuanKerja> satuanKerjasPengguna = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		Criteria c = session.createCriteria(JenisKasKecil.class)
				.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer")
				.add(Restrictions.isNotNull("daftarPengajuanTransfer.prosesTransfer"))
				.add(Restrictions.ne("saldoAwal", 0.0)).add(Restrictions.isNotNull("saldoAwal"))
				.add(satuanKerjasPengguna.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.in("satuanKerja", satuanKerjasPengguna)));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(this_.tanggal) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/**
	 * Posting SEMUA saldo awal kas kecil pada rentang -- jalur API dasbor Draft Jurnal POS.
	 *
	 * <p>Jurnal per dokumen mengikuti tombol layar: Dr akun jenis kas kecil, Cr akun cara
	 * pembayaran transfer pengajuannya (transitori -> {@code akunTransitori}, transfer ->
	 * {@code akun}, selain itu dokumen dilewati), senilai saldo awal pada tanggal pembukaan,
	 * dengan idiom lama nilai &lt;= 0.1 membalik pasangan. Berbeda dari tombol layar yang
	 * mengecap dokumen meski {@code saveTransaksi} gagal, di sini dokumen hanya dicap bila
	 * jurnalnya benar tersimpan. Rantai pengajuan transfer di-null-guard penuh -- tombol
	 * layar membiarkan NPE-nya ditelan catch per baris.</p>
	 *
	 * <p>Transaksi dibuka sendiri (currentNativeSession + begin/commit per dokumen): dipanggil
	 * dari API tidak ada kerangka ZK yang meng-commit sesi berjalan, dan kegagalan satu
	 * dokumen tidak membatalkan dokumen lain yang sudah sah terjurnal.</p>
	 */
	public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<?> daftar = kriteriaSaldoAwalKasKecilStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory")).list();
			if (daftar.isEmpty()) {
				return 0;
			}

			PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_SALDO_AWAL_KAS_KECIL);
			postingHistory.setTbmuser(oleh);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTanggalPosting(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setPosting(true);
			postingHistory.setKeterangan("Posting massal saldo awal kas kecil dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			for (Object o : daftar) {
				JenisKasKecil jk = (JenisKasKecil) o;
				if (jk == null || jk.getDaftarPengajuanTransfer() == null) {
					continue;
				}
				try {
					Akun akunDebet = jk.getAkun();
					Akun akunKredit = null;
					if (jk.getDaftarPengajuanTransfer().getProsesTransfer() != null
							&& jk.getDaftarPengajuanTransfer().getProsesTransfer()
									.getCaraPembayaranTransfer() != null) {
						if (Boolean.TRUE.equals(jk.getDaftarPengajuanTransfer().getTransitori())) {
							akunKredit = jk.getDaftarPengajuanTransfer().getProsesTransfer()
									.getCaraPembayaranTransfer().getAkunTransitori();
						} else if (Boolean.TRUE.equals(jk.getDaftarPengajuanTransfer().getTransfer())) {
							akunKredit = jk.getDaftarPengajuanTransfer().getProsesTransfer()
									.getCaraPembayaranTransfer().getAkun();
						}
					}
					Double nilai = jk.getSaldoAwal();
					if (akunDebet == null || akunKredit == null || nilai == null || nilai == 0.0) {
						continue;
					}

					String ket = "Saldo awal kas kecil \"" + jk.getKode() + "\" senilai "
							+ Common.numberFormat.get().format(nilai);
					try {
						ket = "Saldo awal kas kecil \"" + jk.getKode() + "\" pada pengeluaran \""
								+ jk.getNama() + "\" senilai " + Common.numberFormat.get().format(nilai);
					} catch (Exception e) {
						// Namanya boleh kosong; kalimat baku di atas tetap terpakai.
					}

					boolean tersimpan;
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					if (nilai > 0.1) {
						tersimpan = CommonAkunting.saveTransaksi(new Akun[] { akunDebet },
								new Akun[] { akunKredit }, null, null, postingHistory, true, ket,
								jk.getTanggal(), new Double[] { nilai }, new Double[] { nilai }, 0.0, jk,
								jk.getSatuanKerja(), session);
					} else {
						tersimpan = CommonAkunting.saveTransaksi(new Akun[] { akunKredit },
								new Akun[] { akunDebet }, null, null, postingHistory, true, ket,
								jk.getTanggal(), new Double[] { nilai }, new Double[] { nilai }, 0.0, jk,
								jk.getSatuanKerja(), session);
					}
					if (tersimpan) {
						jk.setPostingHistory(postingHistory);
						session.update(jk);
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
					ais.common.ErrorAuditUtil.record(e, "PostingJenisKasKecilAction jalur API");
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
					ais.common.ErrorAuditUtil.record(e, "PostingJenisKasKecilAction jalur API");
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
	 * Membatalkan posting SEMUA saldo awal kas kecil terposting pada rentang: jurnal
	 * turunannya dihapus (baris transaksi dulu, lalu grupnya -- hanya yang belum closing),
	 * lalu penandanya dilepas. Mengikuti bentuk mesin-mesin batal lain di keluarga ini.
	 */
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<?> daftar = kriteriaSaldoAwalKasKecilStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (Object o : daftar) {
				JenisKasKecil jk = (JenisKasKecil) o;
				if (jk == null) {
					continue;
				}
				try {
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where jenis_kas_kecil="
							+ jk.getId() + " and closing is null)").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where jenis_kas_kecil="
							+ jk.getId() + " and closing is null").executeUpdate();
					jk.setPostingHistory(null);
					session.update(jk);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingJenisKasKecilAction jalur API");
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
