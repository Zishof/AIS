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
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PenggantianKasKecil;
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
 * <h3>PostingPenggantianKasKecilAction — Pengendali Halaman Posting Penggantian Kas Kecil</h3>
 *
 * <p><strong>Untuk apa:</strong>
 * Kelas ini adalah ZK Composer yang mengelola proses posting transaksi penggantian
 * kas kecil (petty cash replenishment) ke dalam buku besar akuntansi. Penggantian
 * kas kecil ({@link PenggantianKasKecil}) terjadi ketika saldo kas kecil yang sudah
 * digunakan digantikan (diisi ulang) dari rekening bank atau rekening lain melalui
 * proses transfer yang disetujui. Pencatatan jurnal ini mendebit akun kas kecil
 * (menambah saldo) dan mengkredit akun asal dana (mengurangi kas bank/transitori).</p>
 *
 * <p>Kelas ini menyediakan antarmuka untuk melihat daftar penggantian kas kecil yang
 * sudah disetujui ({@code disetujuiOleh} tidak null), mengelola status postingnya
 * ke jurnal umum, serta melakukan posting atau pembatalan posting baik per baris
 * maupun secara massal (batch) untuk semua data yang sesuai filter.</p>
 *
 * <p><strong>Cara kerja:</strong>
 * Grid menampilkan entitas {@link PenggantianKasKecil} yang memiliki referensi ke
 * {@code kasKecil} (tidak null), memiliki {@code daftarPengajuanTransfer} dengan
 * {@code prosesTransfer} tidak null, sudah disetujui, dan memiliki nilai tidak nol.
 * Filter tersedia berdasarkan satuan kerja (hierarki organisasi), rentang tanggal
 * persetujuan, status posting, dan kata kunci teks bebas.
 *
 * <p>Logika penentuan akun untuk jurnal:</p>
 * <ul>
 *   <li>Akun debet: {@code kasKecil.jenisKasKecil.akun} (akun kas kecil yang diisi ulang).</li>
 *   <li>Akun kredit: ditentukan secara kondisional dari cara pembayaran transfer:
 *       <ul>
 *         <li>Jika pengajuan transfer bersifat transitori: akun kredit =
 *             {@code daftarPengajuanTransfer.prosesTransfer.caraPembayaranTransfer.akunTransitori}.</li>
 *         <li>Jika pengajuan transfer biasa: akun kredit =
 *             {@code daftarPengajuanTransfer.prosesTransfer.caraPembayaranTransfer.akun}.</li>
 *         <li>Selain itu: akun kredit null, transaksi tidak valid untuk diposting.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p><strong>Threading:</strong>
 * Operasi posting massal berjalan dalam {@link Thread} terpisah menggunakan
 * {@code HibernateUtil.currentNativeSession()} yang dikelola manual. Setelah
 * loop selesai, sesi ditutup dengan {@code HibernateUtil.closeSession()}.</p>
 *
 * <p><strong>Pemeliharaan:</strong>
 * Logika akun kondisional (transitori/transfer/null) harus konsisten di tiga
 * tempat: loop massal ({@link #onPostingSemua}), renderer ({@link PenggantianKasKecilRenderer}),
 * dan posting per baris (di dalam renderer). Konstanta: {@code PostingHistory.JENIS_PENGGANTIAN_KAS_KECIL}.</p>
 *
 * @author Generated Javadoc
 * @see PenggantianKasKecil
 * @see PostingHistory
 * @see CommonAkunting
 */
public class PostingPenggantianKasKecilAction extends GenericAutowireComposer {

	/**
	 * ID serialisasi untuk kompatibilitas serialisasi Java.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar penggantian kas kecil. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman data grid. */
	private Paging paging;

	/** Kotak teks pencarian berdasarkan kode, nama, atau keterangan. */
	private Textbox searchkode;

	/** Checkbox filter untuk menampilkan hanya transaksi yang belum diposting. */
	private MyCheckboxConfig searchtampil;

	/** Checkbox filter untuk menampilkan hanya transaksi yang sudah diposting. */
	private MyCheckboxConfig searchtelahtampil;

	/**
	 * Model pohon satuan kerja untuk mendukung filter hierarki organisasi.
	 * Memungkinkan filter mencakup satuan kerja induk dan seluruh turunannya.
	 */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Komponen banbox pemilih satuan kerja pada filter halaman.
	 * Perubahan nilai memicu {@code onSearchDefault}.
	 */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** Flag apakah pengguna memiliki hak ubah (UPDATE) pada modul ini. */
	private boolean edit = false;

	/** Tombol toolbar posting semua. */
	private MyToolbarbuttonConfig sent;

	/**
	 * Flag apakah pengguna adalah admin atau memiliki hak APPROVE,
	 * menentukan visibilitas tombol batalkan posting.
	 */
	public boolean adminLain;

	/** Tanggal awal rentang filter berdasarkan tanggal persetujuan penggantian. */
	private MyDatebox tglMulai;

	/** Tanggal akhir rentang filter. */
	private MyDatebox tglSampai;

	/** Data pengguna yang sedang login, diambil saat inisialisasi halaman. */
	private Tbmuser tbmuser;

	/**
	 * <h3>doBeforeCompose — Pemeriksaan Keamanan Sebelum Halaman Dibangun</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Dipanggil ZK sebelum komponen halaman diinisialisasi. Memastikan pengguna
	 * terautentikasi dan berhak mengakses halaman posting penggantian kas kecil.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Memanggil {@link Common#doCheckSecurity()} yang memeriksa sesi aktif dan otorisasi.
	 * Jika gagal, ZK diredirect ke halaman login. Kemudian memanggil super.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Pemeriksaan keamanan wajib ada dan tidak boleh dihapus dari metode ini.</p>
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
	 * Bertanggung jawab atas inisialisasi lengkap halaman posting penggantian kas kecil:
	 * validasi sesi, konfigurasi filter satuan kerja, pengaturan tanggal default,
	 * penentuan hak akses, pemuatan data awal, serta registrasi listener paging
	 * dan timer auto-refresh.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Memanggil super untuk auto-wire.</li>
	 *   <li>Menginisialisasi bahasa antarmuka.</li>
	 *   <li>Memvalidasi sesi dan hak READ; jika gagal, redirect logout dan return.</li>
	 *   <li>Mengambil pengguna aktif ke {@code tbmuser}.</li>
	 *   <li>Mengonfigurasi {@code searchparent} dengan listener auto-reload.</li>
	 *   <li>Menginisialisasi {@link SatuanKerjaTreeModel}.</li>
	 *   <li>Mengatur rentang tanggal: 6 bulan ke belakang hingga hari ini, read-only.</li>
	 *   <li>Menentukan {@code adminLain} dan {@code edit} berdasarkan hak akses.</li>
	 *   <li>Memuat data pertama kali dan mendaftarkan listener paging dan timer.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Kegagalan validasi sesi: redirect logout, return awal. Exception lain dilempar.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Inisialisasi {@code searchparent} harus dilakukan sebelum pemuatan data pertama
	 * agar filter satuan kerja sudah aktif saat data dimuat.</p>
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
	 * <h3>onBatalkanPostingSemua — Pembatalan Massal Seluruh Posting Penggantian Kas Kecil</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Menangani klik tombol "Batalkan Posting Semua" untuk membatalkan seluruh posting
	 * transaksi penggantian kas kecil yang memenuhi filter aktif. Semua transaksi yang
	 * sudah diposting dikembalikan ke status "belum diposting" dan entri jurnal
	 * terkait dihapus dari basis data.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Menampilkan dialog konfirmasi. Jika dikonfirmasi:
	 * <ol>
	 *   <li>Mengambil semua {@link PenggantianKasKecil} yang memenuhi filter dan
	 *       sudah diposting ({@code postingHistory} tidak null).</li>
	 *   <li>Untuk setiap entitas: set {@code postingHistory} ke null, simpan,
	 *       hapus baris {@link GrupTransaksi} dengan kolom {@code penggantian_kas_kecil}
	 *       yang bukan closing entry.</li>
	 *   <li>Muat ulang grid via timer default.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Tidak ada rollback otomatis per iterasi. Setiap entitas diproses dalam sesi
	 * Hibernate yang dikelola.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Query SQL menggunakan kolom {@code penggantian_kas_kecil} pada tabel
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
							List<PenggantianKasKecil> penggantianKasKecils = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (PenggantianKasKecil penggantianKasKecil : penggantianKasKecils) {
								penggantianKasKecil.setPostingHistory(null);
								Common.refreshSaveOrUpdate(penggantianKasKecil);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where penggantian_kas_kecil="
														+ penggantianKasKecil.getId() + " and closing is null")
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
	 * <h3>onPostingSemua — Pembukaan Dialog dan Eksekusi Posting Massal Penggantian Kas Kecil</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Menangani klik tombol "Posting Semua Kas Kecil" dengan membuka dialog konfirmasi,
	 * memvalidasi input tanggal dan keterangan, lalu menjalankan proses posting massal
	 * seluruh penggantian kas kecil yang belum diposting dalam utas latar belakang
	 * agar antarmuka pengguna tetap responsif selama proses berlangsung.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Menampilkan jendela modal "Posting Transaksi Kas Kecil" dengan form
	 *       tanggal, nama pengguna, dan keterangan opsional.</li>
	 *   <li>Validasi: tanggal wajib diisi.</li>
	 *   <li>Konfirmasi kedua sebelum memulai proses.</li>
	 *   <li>Dalam utas latar belakang:
	 *       <ul>
	 *         <li>Membuka sesi native Hibernate.</li>
	 *         <li>Membuat {@link PostingHistory} baru dengan jenis
	 *             {@code JENIS_PENGGANTIAN_KAS_KECIL} dan menyimpannya.</li>
	 *         <li>Mengambil semua {@link PenggantianKasKecil} yang belum diposting.</li>
	 *         <li>Untuk setiap penggantian:
	 *             <ul>
	 *               <li>Menentukan satuan kerja dari entitas atau null.</li>
	 *               <li>Akun debet: {@code kasKecil.jenisKasKecil.akun}.</li>
	 *               <li>Akun kredit: kondisional (transitori/transfer/null).</li>
	 *               <li>Jika kedua akun tersedia: memanggil {@link CommonAkunting#saveTransaksi}
	 *                   dengan nilai dari {@code getNilai()} dan tanggal persetujuan.</li>
	 *               <li>Memperbarui {@code postingHistory} entitas.</li>
	 *               <li>Memperbarui label progres dengan persentase.</li>
	 *             </ul>
	 *         </li>
	 *         <li>Menutup sesi native dan mengosongkan label progres.</li>
	 *       </ul>
	 *   </li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong>
	 * Loop di utas non-ZK menggunakan {@code currentNativeSession()}. Hanya
	 * {@code label.setValue()} yang boleh dipanggil dari utas ini.</p>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Try-catch per iterasi. Kesalahan individual dilaporkan via {@code tampilErrorJikaAdmin}
	 * dan tidak menghentikan proses keseluruhan.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Logika penentuan akun kredit kondisional harus identik antara loop ini dan
	 * {@link PenggantianKasKecilRenderer#render}. Jika logika berubah, perbarui keduanya.</p>
	 *
	 * @param event event ZK dari klik tombol
	 * @throws Exception jika terjadi kesalahan saat membangun komponen UI jendela
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Transaksi Kas Kecil");
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

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi kas kecil ?", "Pertanyaan",
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
											MyMessageboxConfig.show("Posting transaksi kas kecil berhasil dilakukan",
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
													PostingHistory.JENIS_PENGGANTIAN_KAS_KECIL);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<PenggantianKasKecil> penggantianKasKecils = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (PenggantianKasKecil penggantianKasKecil : penggantianKasKecils) {

												SatuanKerja satuanKerja = (SatuanKerja) (penggantianKasKecil
														.getSatuanKerja() != null ? penggantianKasKecil.getSatuanKerja()
																: null);

												if (penggantianKasKecil != null) {

													try {
														Akun akunDebet = penggantianKasKecil.getKasKecil()
																.getJenisKasKecil().getAkun();

														Akun akunKredit = penggantianKasKecil
																.getDaftarPengajuanTransfer().getTransitori()
																		? penggantianKasKecil
																				.getDaftarPengajuanTransfer()
																				.getProsesTransfer()
																				.getCaraPembayaranTransfer()
																				.getAkunTransitori()
																		: penggantianKasKecil
																				.getDaftarPengajuanTransfer()
																				.getTransfer()
																						? penggantianKasKecil
																								.getDaftarPengajuanTransfer()
																								.getProsesTransfer()
																								.getCaraPembayaranTransfer()
																								.getAkun()
																						: null;

														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {
																ket = "Persetujuan penggantian kas kecil \""
																		+ penggantianKasKecil.getKode()
																		+ "\" pada pengeluaran \""
																		+ penggantianKasKecil.getNama() + "\" senilai "
																		+ Common.numberFormat.get()
																				.format(penggantianKasKecil.getNilai());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(ket + " (" + Common.numberFormat.get().format(
																	rowIndex * 100.0 / penggantianKasKecils.size())
																	+ " %)");

															Double nilai = penggantianKasKecil.getNilai();

															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																session.getTransaction().begin();

																if (nilai > 0.1) {
																	CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			penggantianKasKecil.getTanggalPersetujuan(),
																			nilai, denda, penggantianKasKecil,
																			satuanKerja, session);
																} else {
																	CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			penggantianKasKecil.getTanggalPersetujuan(),
																			nilai, denda, penggantianKasKecil,
																			satuanKerja, session);
																}

																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															penggantianKasKecil.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(penggantianKasKecil);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PostingPenggantianKasKecilAction.java:628");
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
	 * <h3>PenggantianKasKecilRenderer — Renderer Baris Grid Penggantian Kas Kecil</h3>
	 *
	 * <p><strong>Untuk apa:</strong>
	 * Kelas dalam ini merender setiap baris grid halaman posting penggantian kas kecil.
	 * Setiap baris merepresentasikan satu entitas {@link PenggantianKasKecil} dan
	 * menampilkan informasi lengkap: kode (dengan link proses transfer), nama,
	 * informasi SOP terkait, nominal, tanggal persetujuan, pasangan jurnal debet-kredit,
	 * status posting, dan tombol aksi (posting per baris dan batalkan posting).</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Memperluas {@code ais.ui.util.MyRowRenderer}. Penentuan akun kredit menggunakan
	 * logika kondisional identik dengan loop massal: transitori → akunTransitori,
	 * transfer biasa → akun dari cara pembayaran, selain itu null.
	 * Jika disposisi SOP tersedia, link ke alur SOP ditampilkan dengan font kecil.</p>
	 *
	 * <p><strong>Threading:</strong>
	 * Berjalan di utas ZK event. Aman menggunakan {@code currentSession()}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Logika akun kredit kondisional harus identik dengan yang ada di {@link #onPostingSemua}.
	 * Perubahan pada satu tempat harus direplikasi ke tempat lain untuk menjaga konsistensi
	 * antara tampilan grid dan proses posting.</p>
	 */
	class PenggantianKasKecilRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render — Mengisi Satu Baris Grid dengan Data Penggantian Kas Kecil</h3>
		 *
		 * <p><strong>Tujuan:</strong>
		 * Mengisi {@link Row} ZK dengan semua informasi visual dan interaktif dari satu
		 * entitas {@link PenggantianKasKecil}, termasuk tombol aksi yang dikondisikan
		 * berdasarkan status posting dan hak akses pengguna.</p>
		 *
		 * <p><strong>Cara kerja:</strong>
		 * <ol>
		 *   <li>Menyetel vertical-align baris ke "top".</li>
		 *   <li>Menampilkan kode dalam {@link Vbox} via {@code RevisiHelper}, dengan
		 *       link ke proses transfer jika tersedia.</li>
		 *   <li>Menampilkan nama. Jika ada disposisi SOP, menampilkan link SOP kecil
		 *       dengan {@code UIClassHelper.applyReadMore} dan listener ke
		 *       {@code TampilanAlurSopAction}.</li>
		 *   <li>Menampilkan nilai dari {@code getNilai()} dalam format angka.</li>
		 *   <li>Menampilkan tanggal persetujuan.</li>
		 *   <li>Menentukan akun debet ({@code kasKecil.jenisKasKecil.akun}) dan akun
		 *       kredit (kondisional: transitori → akunTransitori, transfer → akun).</li>
		 *   <li>Menampilkan visualisasi jurnal atau pesan error jika akun tidak lengkap.</li>
		 *   <li>Mencari nomor bukti {@link GrupTransaksi} dan menampilkan status posting.</li>
		 *   <li>Membuat toolbar dengan tombol batalkan posting dan tombol posting per baris,
		 *       hanya jika kedua akun tersedia.</li>
		 * </ol>
		 *
		 * <p><strong>Penanganan error:</strong>
		 * Tombol aksi hanya ditampilkan jika akun lengkap. Keterangan error (akun tidak
		 * ada) ditampilkan di kolom jurnal jika akun tidak tersedia.</p>
		 *
		 * <p><strong>Pemeliharaan:</strong>
		 * Pastikan query {@link GrupTransaksi} menggunakan properti
		 * {@code "penggantianKasKecil"} sesuai mapping Hibernate. Logika akun kredit
		 * kondisional harus identik dengan yang ada di {@link #onPostingSemua}.</p>
		 *
		 * @param arg0 baris ZK yang akan diisi komponen
		 * @param arg1 objek data, diharapkan bertipe {@link PenggantianKasKecil}
		 * @throws Exception jika terjadi kesalahan akses basis data atau pembuatan komponen
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PenggantianKasKecil penggantianKasKecil = (PenggantianKasKecil) arg1;

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(PenggantianKasKecil.class, penggantianKasKecil,
					penggantianKasKecil.getKode() == null ? "" : penggantianKasKecil.getKode())).setParent(arg0);

			if (penggantianKasKecil.getDaftarPengajuanTransfer() != null
					&& penggantianKasKecil.getDaftarPengajuanTransfer().getProsesTransfer() != null) {
				A a = new A(penggantianKasKecil.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, penggantianKasKecil.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);
			}

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(penggantianKasKecil.getNama()).setParent(a);
			if (penggantianKasKecil.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + penggantianKasKecil.getDisposisiSop().getKeterangan()
						+ " (" + penggantianKasKecil.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(penggantianKasKecil.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			Double nilai = penggantianKasKecil.getNilai();

			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

			new Label(Common.dateFormat3.get().format(penggantianKasKecil.getTanggalPersetujuan())).setParent(arg0);

			Akun akunDebet = penggantianKasKecil.getKasKecil().getJenisKasKecil().getAkun();
			Akun akunKredit = penggantianKasKecil.getDaftarPengajuanTransfer().getTransitori()
					? penggantianKasKecil.getDaftarPengajuanTransfer().getProsesTransfer().getCaraPembayaranTransfer()
							.getAkunTransitori()
					: penggantianKasKecil.getDaftarPengajuanTransfer().getTransfer()
							? penggantianKasKecil.getDaftarPengajuanTransfer().getProsesTransfer()
									.getCaraPembayaranTransfer().getAkun()
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
					.add(Restrictions.eq("penggantianKasKecil", penggantianKasKecil)).setMaxResults(1)
					.setProjection(Projections.property("kode")).uniqueResult();

			new Label(penggantianKasKecil.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: penggantianKasKecil.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && penggantianKasKecil.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								penggantianKasKecil.setPostingHistory(null);
								Common.refreshSaveOrUpdate(penggantianKasKecil);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where penggantian_kas_kecil="
														+ penggantianKasKecil.getId() + " and closing is null")
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
				button.setVisible(edit && penggantianKasKecil.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PENGGANTIAN_KAS_KECIL);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);
								Akun akunDebet = penggantianKasKecil.getKasKecil().getJenisKasKecil().getAkun();
								Akun akunKredit = penggantianKasKecil.getDaftarPengajuanTransfer().getTransitori()
										? penggantianKasKecil.getDaftarPengajuanTransfer().getProsesTransfer()
												.getCaraPembayaranTransfer().getAkunTransitori()
										: penggantianKasKecil.getDaftarPengajuanTransfer().getTransfer()
												? penggantianKasKecil.getDaftarPengajuanTransfer().getProsesTransfer()
														.getCaraPembayaranTransfer().getAkun()
												: null;

								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									Double nilai = penggantianKasKecil.getNilai();

									String ket = "";
									try {

										ket = "Persetujuan penggantian kas kecil \"" + penggantianKasKecil.getKode()
												+ "\" pada pengeluaran \"" + penggantianKasKecil.getNama()
												+ "\" senilai "
												+ Common.numberFormat.get().format(penggantianKasKecil.getNilai());
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (penggantianKasKecil
											.getSatuanKerja() != null ? penggantianKasKecil.getSatuanKerja()
													: tbmuser.ambilSatuanKerja());

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket,
												penggantianKasKecil.getTanggalPersetujuan(), nilai, denda,
												penggantianKasKecil, satuanKerja, session);
									} else {
										CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket,
												penggantianKasKecil.getTanggalPersetujuan(), nilai, denda,
												penggantianKasKecil, satuanKerja, session);
									}

									penggantianKasKecil.setPostingHistory(postingHistory);
									session.update(penggantianKasKecil);
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
	 * <h3>initCriteria — Membangun Kriteria Query Hibernate untuk Data Penggantian Kas Kecil</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Membangun objek {@link Criteria} Hibernate yang mencerminkan semua filter aktif:
	 * satuan kerja dengan hierarki, kondisi wajib bisnis ({@code kasKecil} tidak null,
	 * {@code prosesTransfer} tidak null, {@code disetujuiOleh} tidak null, nilai tidak
	 * nol), status posting, rentang tanggal persetujuan, dan kata kunci teks bebas.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Mengambil satuan kerja dari {@code searchparent} dan mengumpulkan turunannya.</li>
	 *   <li>Filter wajib: {@code kasKecil} tidak null (hanya penggantian yang memiliki
	 *       relasi ke kas kecil yang valid).</li>
	 *   <li>Join ke {@code daftarPengajuanTransfer} dengan filter wajib
	 *       {@code prosesTransfer} tidak null.</li>
	 *   <li>Filter satuan kerja: global atau dalam set yang dipilih.</li>
	 *   <li>Filter status posting berdasarkan checkbox.</li>
	 *   <li>Filter wajib: {@code disetujuiOleh} tidak null (hanya yang sudah disetujui).</li>
	 *   <li>Filter wajib: nilai tidak 0.0 dan tidak null.</li>
	 *   <li>Filter rentang tanggal berdasarkan {@code tanggal_persetujuan}.</li>
	 *   <li>Filter kata kunci pada kode, nama, dan keterangan.</li>
	 *   <li>Jika {@code order} true, ORDER BY id descending.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Tidak ada penanganan eksplisit. Kesalahan Hibernate dilempar ke pemanggil.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Alias {@code "daftarPengajuanTransfer"} harus konsisten dengan nama properti
	 * di entitas {@link PenggantianKasKecil}. Filter {@code kasKecil} tidak null
	 * adalah kondisi wajib — tanpanya, NPE akan terjadi saat mengambil akun debet
	 * di renderer.</p>
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

		Criteria criteria = session.createCriteria(PenggantianKasKecil.class).add(Restrictions.isNotNull("kasKecil"))

				.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer")

				.add(Restrictions.isNotNull("daftarPengajuanTransfer.prosesTransfer"))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas))))

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(!searchtelahtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNotNull("postingHistory"))

				.add(Restrictions.isNotNull("disetujuiOleh"))

				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.tanggal_persetujuan) between date('"
								+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and date('"
								+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("nama", searchkode.getValue(), MatchMode.ANYWHERE),
										Restrictions.ilike("keterangan", searchkode.getValue(), MatchMode.ANYWHERE))));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <h3>onSearchDefaultTanpaProgress — Pemuatan Data Grid Penggantian Kas Kecil Tanpa Progres</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Melakukan pemuatan data aktual ke grid: menghitung total baris untuk paging,
	 * mengambil data sesuai halaman aktif, dan mengatur model serta renderer grid.
	 * Dipanggil dari dalam {@link #loadDataDenganProgressPosting} setelah indikator
	 * progres ditampilkan kepada pengguna.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Menghitung total halaman via {@code Common.initPaging}, mengambil baris halaman
	 * aktif dengan {@code setMaxResults} dan {@code setFirstResult}, membungkus dalam
	 * {@link SimpleListModel}, lalu menyetel renderer {@link PenggantianKasKecilRenderer}
	 * dan model ke grid.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Selalu panggil via {@link #loadDataDenganProgressPosting}, bukan langsung,
	 * agar indikator progres berfungsi dengan benar.</p>
	 *
	 * @param event event pemicu (dapat null)
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenggantianKasKecil> penggantianKasKecil = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penggantianKasKecil);
		grid.setRowRenderer(new PenggantianKasKecilRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <h3>onSearchDefault — Titik Masuk Publik Pencarian Data Penggantian Kas Kecil</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Titik masuk standar yang dipanggil ZK untuk memuat ulang data grid penggantian
	 * kas kecil. Mendelegasikan ke {@link #loadDataDenganProgressPosting} agar
	 * indikator progres selalu aktif selama pemuatan data.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Jangan menambahkan logika di sini. Semua logika pemuatan harus ada di
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
	 * Orkestrasi utama pemuatan data grid posting penggantian kas kecil dengan tampilan
	 * indikator progres bertahap kepada pengguna. Mencegah race condition saat beberapa
	 * permintaan reload datang bersamaan menggunakan pola "loading flag dengan antrian
	 * tunggal" — permintaan yang datang saat loading aktif dicatat dan dieksekusi setelah
	 * proses selesai, bukan dieksekusi secara bersamaan.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Jika {@code postingJurnalLoadingAktif = true}: set flag tertunda, tampilkan
	 *       notifikasi "permintaan diterima", dan return langsung tanpa memulai proses baru.</li>
	 *   <li>Jika belum loading: set flag aktif, reset flag tertunda, tampilkan indikator
	 *       progres awal (7%), jadwalkan eksekusi data via {@code Common.createDefaultTimer}
	 *       agar browser dapat merender indikator sebelum query berat dimulai.</li>
	 *   <li>Dalam callback timer: tampilkan progres 48% (mengambil data), panggil
	 *       {@link #onSearchDefaultTanpaProgress} untuk query dan render grid,
	 *       tampilkan progres 92% (merapikan tampilan).</li>
	 *   <li>Blok {@code finally}: selalu membersihkan flag. Jika ada tertunda, jadwalkan
	 *       pemanggilan rekursif ke diri sendiri via timer baru. Jika tidak ada tertunda,
	 *       tandai indikator selesai (100%).</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong>
	 * Seluruh logika berjalan di utas ZK event (bukan background thread). Penggunaan
	 * {@code createDefaultTimer} adalah cara ZK-idiomatis untuk "melepas" utas event
	 * sejenak agar browser sempat merender pembaruan UI sebelum query berikutnya dimulai.</p>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Blok {@code finally} memastikan flag {@code postingJurnalLoadingAktif} selalu
	 * disetel ke false meskipun terjadi exception di dalam blok try. Ini mencegah
	 * halaman "terkunci" dalam state loading selamanya jika terjadi error.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Persentase progres (7, 48, 92, 96, 100) bersifat informatif dan dapat disesuaikan.
	 * Pola ini identik di semua kelas posting — jika perilaku perlu diubah secara
	 * konsisten, pertimbangkan ekstraksi ke helper bersama.</p>
	 *
	 * @param event event pemicu (dapat null bila dipanggil secara internal dari paging
	 *              atau timer)
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
