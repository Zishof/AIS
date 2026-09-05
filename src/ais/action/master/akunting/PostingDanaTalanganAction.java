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
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.GrupTransaksi;
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
 * <h3>PostingDanaTalanganAction — Pengendali Halaman Posting Dana Talangan</h3>
 *
 * <p><strong>Untuk apa:</strong>
 * Kelas ini adalah ZK Composer yang mengelola proses posting transaksi dana talangan
 * (advance fund / uang talangan) ke dalam buku besar akuntansi. Dana talangan adalah
 * dana yang diberikan terlebih dahulu kepada suatu unit/pihak dalam organisasi untuk
 * membiayai kegiatan, sebelum dilakukan pertanggungjawaban. Kelas ini menyediakan
 * antarmuka untuk melihat daftar dana talangan yang sudah disetujui, mengelola status
 * postingnya ke jurnal umum, serta melakukan posting atau pembatalan posting baik
 * secara individual per baris maupun secara massal (batch) untuk semua data
 * yang sesuai filter.</p>
 *
 * <p><strong>Cara kerja:</strong>
 * Halaman ZUL yang menggunakan composer ini menampilkan grid berisi entitas
 * {@link DanaTalangan} yang memiliki field {@code disetujuiOleh} tidak null dan
 * nilai dana talangan tidak nol. Filter tersedia berdasarkan satuan kerja (dengan
 * dukungan hierarki organisasi via {@link SatuanKerjaTreeModel}), rentang tanggal
 * persetujuan, status posting, dan kata kunci teks bebas.
 *
 * <ul>
 *   <li>Setiap baris menampilkan kode, nama, informasi SOP terkait (jika ada),
 *       nominal, tanggal persetujuan, pasangan jurnal debet-kredit, status posting,
 *       dan tombol aksi.</li>
 *   <li>Tombol "Posting Data" memposting satu transaksi secara langsung menggunakan
 *       sesi Hibernate yang dikelola.</li>
 *   <li>Tombol "Batalkan Posting" menghapus {@link PostingHistory} dan membersihkan
 *       entri {@link GrupTransaksi} dari tabel akunting.</li>
 *   <li>Tombol "Posting Semua" membuka dialog konfirmasi, lalu menjalankan loop
 *       posting dalam {@link Thread} latar belakang.</li>
 * </ul>
 *
 * <p><strong>Threading:</strong>
 * Operasi posting massal berjalan dalam utas baru (bukan utas ZK). Sesi yang
 * digunakan adalah {@code HibernateUtil.currentNativeSession()} yang harus dikelola
 * manual (begin/commit/close). Setelah loop selesai, sesi ditutup dengan
 * {@code HibernateUtil.closeSession()}. Jangan menggunakan {@code currentSession()}
 * di dalam utas latar belakang.</p>
 *
 * <p><strong>Pemeliharaan:</strong>
 * Penentuan akun debet diambil dari {@code danaTalangan.getUangMuka().getJenisUangMuka().getAkun()}
 * dan akun kredit dari {@code danaTalangan.getJenisUangMuka().getAkunKelebihan()}.
 * Jika struktur relasi entitas berubah, pastikan jalur ini diperbarui di tiga
 * tempat: dalam loop massal, dalam renderer baris, dan dalam posting per baris
 * (di dalam renderer). Konstanta {@code PostingHistory.JENIS_PERSETUJUAN_UANG_MUKA}
 * harus tetap valid.</p>
 *
 * @author Generated Javadoc
 * @see DanaTalangan
 * @see PostingHistory
 * @see CommonAkunting
 */
public class PostingDanaTalanganAction extends GenericAutowireComposer {

	/**
	 * ID serialisasi untuk kompatibilitas serialisasi Java.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar transaksi dana talangan. */
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
	 * Digunakan untuk mengumpulkan satuan kerja anak dari satuan kerja yang dipilih.
	 */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Komponen banbox pemilih satuan kerja pada filter, mendukung pencarian
	 * dan pemilihan satuan kerja dari hierarki organisasi.
	 */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** Flag apakah pengguna saat ini memiliki hak ubah (UPDATE). */
	private boolean edit = false;

	/** Tombol toolbar untuk posting semua data. */
	private MyToolbarbuttonConfig sent;

	/**
	 * Flag apakah pengguna adalah admin atau memiliki hak APPROVE,
	 * menentukan visibilitas tombol batalkan posting.
	 */
	public boolean adminLain;

	/** Tanggal awal rentang filter data dana talangan. */
	private MyDatebox tglMulai;

	/** Tanggal akhir rentang filter data dana talangan. */
	private MyDatebox tglSampai;

	/** Data pengguna yang sedang login, diambil saat inisialisasi halaman. */
	private Tbmuser tbmuser;

	/**
	 * <h3>doBeforeCompose — Pemeriksaan Keamanan Sebelum Halaman Dibangun</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Dipanggil oleh ZK sebelum komponen halaman diinisialisasi. Melakukan
	 * pemeriksaan keamanan untuk memastikan pengguna telah terautentikasi dan
	 * berhak mengakses halaman posting dana talangan ini.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Memanggil {@link Common#doCheckSecurity()} yang memeriksa sesi aktif dan
	 * otorisasi pengguna. Jika gagal, redirect ke halaman login dilakukan secara
	 * otomatis oleh helper tersebut. Kemudian memanggil implementasi super.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Jangan menghapus pemanggilan {@code doCheckSecurity()} — wajib ada di setiap
	 * composer halaman sensitif.</p>
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
	 * Bertanggung jawab atas inisialisasi lengkap halaman posting dana talangan:
	 * validasi sesi, konfigurasi filter satuan kerja, pengaturan rentang tanggal
	 * default, penentuan hak akses, pemuatan data awal, serta registrasi
	 * listener paging dan timer auto-refresh.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan auto-wire.</li>
	 *   <li>Menginisialisasi bahasa antarmuka.</li>
	 *   <li>Memvalidasi sesi ({@code "usersTemp"}) dan hak READ; jika gagal, logout.</li>
	 *   <li>Mengambil data pengguna aktif ke {@code tbmuser}.</li>
	 *   <li>Mengonfigurasi komponen {@code searchparent} (banbox satuan kerja) dengan
	 *       listener yang memuat ulang data saat satuan kerja berubah.</li>
	 *   <li>Menginisialisasi {@link SatuanKerjaTreeModel} tanpa membatasi ke satuan
	 *       kerja tertentu (parameter {@code false}).</li>
	 *   <li>Mengatur rentang tanggal default: 6 bulan ke belakang hingga hari ini,
	 *       mode read-only.</li>
	 *   <li>Menentukan flag {@code adminLain} dan {@code edit} berdasarkan hak akses.</li>
	 *   <li>Memuat data pertama kali via {@link #loadDataDenganProgressPosting}.</li>
	 *   <li>Mendaftarkan listener paging dan timer auto-refresh.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Kegagalan validasi sesi menyebabkan redirect logout dan return awal.
	 * Exception lain dilempar ke atas.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Jika ada filter baru yang perlu diinisialisasi, tambahkan setelah blok validasi
	 * sesi. Urutan inisialisasi sangat penting.</p>
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
	 * <h3>onBatalkanPostingSemua — Pembatalan Massal Seluruh Posting Dana Talangan</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Menangani klik tombol "Batalkan Posting Semua" untuk membatalkan seluruh posting
	 * transaksi dana talangan yang memenuhi filter aktif. Semua transaksi yang sudah
	 * diposting akan dikembalikan ke status "belum diposting" dan entri jurnal
	 * terkait dihapus dari basis data.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Menampilkan dialog konfirmasi. Jika pengguna mengkonfirmasi:
	 * <ol>
	 *   <li>Mengambil semua {@link DanaTalangan} yang memenuhi filter dan sudah diposting
	 *       ({@code postingHistory} tidak null).</li>
	 *   <li>Untuk setiap entitas: set {@code postingHistory} ke null, simpan perubahan,
	 *       hapus baris {@link GrupTransaksi} terkait (kolom {@code dana_talangan})
	 *       yang bukan closing entry via SQL native.</li>
	 *   <li>Setelah loop, muat ulang grid via timer default.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Tidak ada rollback otomatis per iterasi. Kegagalan satu baris dapat meninggalkan
	 * data dalam kondisi tidak konsisten. Pertimbangkan membungkus dalam satu transaksi
	 * jika atomisitas diperlukan.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Query SQL native menggunakan skema {@code akunting.grup_transaksi} dan kolom
	 * {@code dana_talangan}. Perbarui jika nama skema/kolom berubah.</p>
	 *
	 * @param event event ZK dari klik tombol
	 * @throws Exception jika terjadi kesalahan akses basis data atau UI
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi dana talangan ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<DanaTalangan> danaTalangans = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (DanaTalangan danaTalangan : danaTalangans) {
								danaTalangan.setPostingHistory(null);
								Common.refreshSaveOrUpdate(danaTalangan);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
												+ " (select id from akunting.grup_transaksi where dana_talangan="
												+ danaTalangan.getId() + " and closing is null)")
										.executeUpdate();
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where dana_talangan="
												+ danaTalangan.getId() + " and closing is null")
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
	 * <h3>onPostingSemua — Pembukaan Dialog dan Eksekusi Posting Massal Dana Talangan</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Menangani klik tombol "Posting Semua" dengan membuka jendela konfirmasi,
	 * memvalidasi input tanggal dan keterangan, lalu menjalankan proses posting
	 * seluruh dana talangan yang belum diposting dalam utas latar belakang agar
	 * antarmuka pengguna tetap responsif.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Membuat dan menampilkan jendela modal dengan form: tanggal posting,
	 *       nama pengguna yang memposting, dan keterangan opsional.</li>
	 *   <li>Tombol "Batal" menutup jendela tanpa aksi.</li>
	 *   <li>Tombol "Simpan": memvalidasi tanggal tidak kosong, lalu konfirmasi kedua.</li>
	 *   <li>Jika dikonfirmasi: membuat label progres, memulai utas baru.</li>
	 *   <li>Dalam utas: membuka sesi native, membuat satu {@link PostingHistory} baru
	 *       dengan jenis {@code JENIS_PERSETUJUAN_UANG_MUKA}, menyimpannya.</li>
	 *   <li>Mengambil semua {@link DanaTalangan} yang belum diposting sesuai filter.</li>
	 *   <li>Untuk setiap dana talangan:
	 *       <ul>
	 *         <li>Menentukan satuan kerja dari entitas atau null.</li>
	 *         <li>Mengambil akun debet dari {@code uangMuka.jenisUangMuka.akun} dan
	 *             akun kredit dari {@code jenisUangMuka.akunKelebihan}.</li>
	 *         <li>Jika kedua akun tersedia: memanggil {@link CommonAkunting#saveTransaksi}
	 *             dengan nominal, tanggal persetujuan, dan keterangan deskriptif.</li>
	 *         <li>Memperbarui field {@code postingHistory} pada entitas.</li>
	 *         <li>Memperbarui label progres dengan persentase penyelesaian.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Setelah loop: mengosongkan label dan menutup sesi native.</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong>
	 * Loop berjalan di utas non-ZK. Hanya {@code label.setValue()} yang boleh
	 * dipanggil dari utas ini untuk komunikasi ke UI.</p>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Kesalahan per iterasi ditangkap dan ditampilkan via {@code tampilErrorJikaAdmin}.
	 * Blok try-catch luar juga tersedia untuk menangani error yang tidak terduga.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Perhatikan bahwa akun debet dan kredit diambil dari jalur yang berbeda
	 * (akun debet via {@code uangMuka.jenisUangMuka} di loop massal, sedangkan
	 * di renderer diambil dari {@code jenisUangMuka} langsung). Pastikan konsistensi
	 * jika struktur entitas berubah.</p>
	 *
	 * @param event event ZK dari klik tombol
	 * @throws Exception jika terjadi kesalahan saat membangun komponen UI jendela
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Transaksi Uang Muka");
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

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi dana talangan ?", "Pertanyaan",
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
													"Posting transaksi dana talangan berhasil dilakukan", "Informasi",
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
													PostingHistory.JENIS_PERSETUJUAN_UANG_MUKA);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<DanaTalangan> danaTalangans = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (DanaTalangan danaTalangan : danaTalangans) {

												SatuanKerja satuanKerja = (SatuanKerja) (danaTalangan
														.getSatuanKerja() != null ? danaTalangan.getSatuanKerja()
																: null);

												if (danaTalangan != null) {

													try {
														Akun akunDebet = danaTalangan.getUangMuka().getJenisUangMuka()
																.getAkun();
														Akun akunKredit = danaTalangan.getJenisUangMuka()
																.getAkunKelebihan();

														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {

																ket = "Persetujuan dana talangan \""
																		+ danaTalangan.getKode()
																		+ "\" pada penggunaan anggaran \""
																		+ danaTalangan
																				.getUangMuka().getWorkspace().getKode()
																		+ " "
																		+ danaTalangan.getUangMuka().getWorkspace()
																				.getNama()
																		+ "\" senilai " + Common.numberFormat.get()
																				.format(danaTalangan.getNilai());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(ket + " ("
																	+ Common.numberFormat.get().format(
																			rowIndex * 100.0 / danaTalangans.size())
																	+ " %)");

															Double nilai = danaTalangan.getNilai();

															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																session.getTransaction().begin();

																if (nilai > 0.1) {
																	CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			danaTalangan.getTanggalPersetujuan(), nilai,
																			denda, danaTalangan, satuanKerja, session);
																} else {
																	CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			danaTalangan.getTanggalPersetujuan(), nilai,
																			denda, danaTalangan, satuanKerja, session);
																}

																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															danaTalangan.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(danaTalangan);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PostingDanaTalanganAction.java:624");
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
	 * <h3>DanaTalanganRenderer — Renderer Baris Grid Transaksi Dana Talangan</h3>
	 *
	 * <p><strong>Untuk apa:</strong>
	 * Kelas dalam ini merender setiap baris pada grid utama halaman posting dana
	 * talangan. Setiap baris merepresentasikan satu entitas {@link DanaTalangan} dan
	 * menampilkan informasi lengkap: kode, nama, informasi SOP terkait, nominal,
	 * tanggal persetujuan, pasangan jurnal, status posting, dan tombol aksi.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Kelas ini memperluas {@code ais.ui.util.MyRowRenderer} dan mengimplementasikan
	 * metode {@link #render(Row, Object)}. Jika dana talangan memiliki referensi SOP
	 * ({@code disposisiSop} tidak null), tautan ke alur SOP ditampilkan di bawah
	 * nama dengan ukuran font kecil. Akun untuk visualisasi jurnal diambil dari
	 * {@code jenisUangMuka} (bukan dari uangMuka) karena itulah akun yang relevan
	 * untuk tampilan grid.</p>
	 *
	 * <p><strong>Threading:</strong>
	 * Berjalan di utas ZK event. Aman menggunakan {@code currentSession()}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Perhatikan perbedaan jalur akun antara renderer (dari {@code jenisUangMuka})
	 * dan loop massal (dari {@code uangMuka.jenisUangMuka}). Pastikan konsistensi
	 * jika entitas berubah.</p>
	 */
	class DanaTalanganRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render — Mengisi Satu Baris Grid dengan Data Dana Talangan</h3>
		 *
		 * <p><strong>Tujuan:</strong>
		 * Mengisi {@link Row} ZK dengan semua informasi visual dan interaktif
		 * dari satu entitas {@link DanaTalangan}, termasuk tombol-tombol aksi
		 * yang dikondisikan berdasarkan status posting dan hak akses pengguna.</p>
		 *
		 * <p><strong>Cara kerja:</strong>
		 * <ol>
		 *   <li>Menyetel vertical-align baris ke "top".</li>
		 *   <li>Menampilkan kode dana talangan dalam {@link Vbox} via {@code RevisiHelper},
		 *       dengan link ke proses transfer jika ada.</li>
		 *   <li>Menampilkan nama dana talangan. Jika ada disposisi SOP, menampilkan
		 *       tautan SOP dengan font kecil menggunakan {@code UIClassHelper.applyReadMore}
		 *       dan listener ke {@code TampilanAlurSopAction}.</li>
		 *   <li>Mengambil akun debet dari {@code jenisUangMuka.akun} dan akun kredit
		 *       dari {@code jenisUangMuka.akunKelebihan} untuk keperluan tampilan.</li>
		 *   <li>Menampilkan nominal dan tanggal persetujuan.</li>
		 *   <li>Menampilkan visualisasi jurnal atau pesan kesalahan akun tidak lengkap.</li>
		 *   <li>Mencari nomor bukti {@link GrupTransaksi} dan menampilkan status posting.</li>
		 *   <li>Membuat toolbar aksi dengan tombol batalkan posting dan posting per baris.</li>
		 * </ol>
		 *
		 * <p><strong>Penanganan error:</strong>
		 * Komponen tombol hanya ditampilkan jika kedua akun (debet dan kredit) tersedia.
		 * Keterangan error akun ditampilkan di kolom jurnal jika akun tidak lengkap.</p>
		 *
		 * <p><strong>Pemeliharaan:</strong>
		 * Pastikan query {@link GrupTransaksi} menggunakan properti {@code "danaTalangan"}
		 * sesuai mapping Hibernate. Jika kolom ZUL berubah, sesuaikan urutan komponen.</p>
		 *
		 * @param arg0 baris ZK yang akan diisi komponen
		 * @param arg1 objek data, diharapkan bertipe {@link DanaTalangan}
		 * @throws Exception jika terjadi kesalahan akses basis data atau pembuatan komponen
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final DanaTalangan danaTalangan = (DanaTalangan) arg1;

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(DanaTalangan.class, danaTalangan,
					danaTalangan.getKode() == null ? "" : danaTalangan.getKode())).setParent(arg0);

			if (danaTalangan.getUangMuka() != null && danaTalangan.getUangMuka().getDaftarPengajuanTransfer() != null
					&& danaTalangan.getUangMuka().getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A a = new A(danaTalangan.getUangMuka().getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, danaTalangan.getUangMuka().getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);
			}

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(danaTalangan.getNama()).setParent(a);
			if (danaTalangan.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + danaTalangan.getDisposisiSop().getKeterangan()
						+ " (" + danaTalangan.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(danaTalangan.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			Akun akunDebet = danaTalangan.getJenisUangMuka().getAkun();
			Akun akunKredit = danaTalangan.getJenisUangMuka().getAkunKelebihan();

			Double nilai = danaTalangan.getNilai();

			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

			new Label(Common.dateFormat3.get().format(danaTalangan.getTanggalPersetujuan())).setParent(arg0);

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
					.add(Restrictions.eq("danaTalangan", danaTalangan)).setMaxResults(1)
					.setProjection(Projections.property("kode")).uniqueResult();

			new Label(danaTalangan.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: danaTalangan.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && danaTalangan.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								danaTalangan.setPostingHistory(null);
								Common.refreshSaveOrUpdate(danaTalangan);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
												+ " (select id from akunting.grup_transaksi where dana_talangan="
												+ danaTalangan.getId() + " and closing is null)")
										.executeUpdate();
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where dana_talangan="
												+ danaTalangan.getId() + " and closing is null")
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
				button.setVisible(edit && danaTalangan.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PERSETUJUAN_UANG_MUKA);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunDebet = danaTalangan.getJenisUangMuka().getAkun();
								Akun akunKredit = danaTalangan.getJenisUangMuka().getAkunKelebihan();

								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									Double nilai = danaTalangan.getNilai();

									String ket = "";
									try {

										ket = "Persetujuan dana talangan \"" + danaTalangan.getKode()
												+ "\" pada penggunaan anggaran \""
												+ danaTalangan.getUangMuka().getWorkspace().getKode() + " "
												+ danaTalangan.getUangMuka().getWorkspace().getNama() + "\" senilai "
												+ Common.numberFormat.get().format(danaTalangan.getNilai());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (danaTalangan.getSatuanKerja() != null
											? danaTalangan.getSatuanKerja()
											: tbmuser.ambilSatuanKerja());

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket,
												danaTalangan.getTanggalPersetujuan(), nilai, denda, danaTalangan,
												satuanKerja, session);
									} else {
										CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket,
												danaTalangan.getTanggalPersetujuan(), nilai, denda, danaTalangan,
												satuanKerja, session);
									}

									danaTalangan.setPostingHistory(postingHistory);
									session.update(danaTalangan);
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
	 * <h3>initCriteria — Membangun Kriteria Query Hibernate untuk Data Dana Talangan</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Membangun objek {@link Criteria} Hibernate yang mencerminkan semua kondisi filter
	 * aktif: satuan kerja (dengan hierarki), status posting, rentang tanggal persetujuan,
	 * kondisi bisnis wajib (disetujuiOleh tidak null, nilai tidak nol), dan kata kunci.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Mengambil satuan kerja yang dipilih dari {@code searchparent}. Jika dipilih,
	 *       mengumpulkan semua satuan kerja anak via {@code SatuanKerjaTreeModel.getChildsSet}.</li>
	 *   <li>Filter satuan kerja: transaksi yang satuan kerjanya null (global) atau termasuk
	 *       dalam set satuan kerja yang dipilih beserta turunannya.</li>
	 *   <li>Filter status posting berdasarkan checkbox.</li>
	 *   <li>Filter wajib: {@code disetujuiOleh} tidak null.</li>
	 *   <li>Filter wajib: nilai tidak 0.0 dan tidak null.</li>
	 *   <li>Filter rentang tanggal menggunakan SQL native {@code date()} untuk akurasi
	 *       tanpa pengaruh komponen waktu (jam).</li>
	 *   <li>Filter kata kunci pada kode, nama, dan keterangan menggunakan ILIKE.</li>
	 *   <li>Jika {@code order} true, tambahkan ORDER BY id descending.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Tidak ada penanganan exception eksplisit. Kesalahan Hibernate dilempar ke pemanggil.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Format tanggal SQL ({@code Common.databaseDateFormat}) harus konsisten dengan
	 * format yang didukung database (PostgreSQL: 'YYYY-MM-DD'). Jika basis data diganti,
	 * periksa kompatibilitas fungsi {@code date()}.</p>
	 *
	 * @param order true untuk menambahkan ORDER BY (tampilan grid),
	 *              false untuk query COUNT (paging)
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

		Criteria criteria = session.createCriteria(DanaTalangan.class)

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas))))

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(!searchtelahtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNotNull("postingHistory"))

				.add(Restrictions.isNotNull("disetujuiOleh"))

				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
						+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and  date('"
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
	 * <h3>onSearchDefaultTanpaProgress — Pemuatan Data Grid Tanpa Indikator Progres</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Melakukan pemuatan data aktual ke grid: menghitung total baris untuk paging,
	 * mengambil data sesuai halaman aktif, dan mengatur model serta renderer grid.
	 * Dipanggil dari dalam {@link #loadDataDenganProgressPosting} setelah indikator
	 * progres ditampilkan sehingga pengguna mendapat umpan balik visual.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Menghitung total halaman via {@code Common.initPaging}, mengambil baris halaman
	 * aktif dengan {@code setMaxResults} dan {@code setFirstResult}, membungkus dalam
	 * {@link SimpleListModel}, lalu menyetel renderer {@link DanaTalanganRenderer}
	 * dan model ke grid.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Jangan memanggil metode ini langsung — selalu gunakan
	 * {@link #loadDataDenganProgressPosting} agar progres berjalan dengan benar.</p>
	 *
	 * @param event event pemicu (dapat null)
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DanaTalangan> danaTalangan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(danaTalangan);
		grid.setRowRenderer(new DanaTalanganRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <h3>onSearchDefault — Titik Masuk Publik Pencarian Data Dana Talangan</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Titik masuk standar yang dipanggil ZK untuk memuat ulang data grid.
	 * Mendelegasikan ke {@link #loadDataDenganProgressPosting} agar indikator
	 * progres selalu aktif.</p>
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
	 * Orkestrasi utama pemuatan data grid posting dana talangan dengan tampilan
	 * indikator progres bertahap. Mencegah race condition saat beberapa permintaan
	 * reload datang bersamaan, misalnya saat pengguna cepat berpindah halaman atau
	 * mengubah filter sebelum proses selesai.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Menggunakan pola "loading flag dengan antrian tunggal":
	 * <ol>
	 *   <li>Jika sudah loading ({@code postingJurnalLoadingAktif = true}): catat
	 *       permintaan tertunda dan return langsung.</li>
	 *   <li>Jika belum: set flag aktif, tampilkan progres 7%, jadwalkan via timer.</li>
	 *   <li>Dalam callback: tampilkan progres 48%, muat data, tampilkan progres 92%.</li>
	 *   <li>Blok finally: bersihkan flag. Jika ada tertunda, jadwalkan ulang secara
	 *       rekursif. Jika tidak, tandai selesai (100%).</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong>
	 * Seluruh logika berjalan di utas ZK event. Timer memberikan jeda agar browser
	 * sempat merender pembaruan sebelum query berat dijalankan.</p>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Blok finally memastikan flag selalu dibersihkan meskipun ada exception,
	 * mencegah halaman "terkunci" dalam state loading.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Persentase progres bersifat informatif. Pastikan {@code PostingJurnalLoadingUtil}
	 * tersedia. Pola ini identik di semua kelas posting — pertimbangkan ekstraksi
	 * ke helper bersama jika ada perubahan perilaku.</p>
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


	// ================================================================ mesin posting massal

	/**
	 * Penyaring dokumen yang layak diposting, disamakan dengan {@link #initCriteria}:
	 * sudah disetujui, nilainya tidak nol, dan berada dalam rentang tanggal persetujuan.
	 */
	private static Criteria kriteriaPostingStatic(Session session, java.util.Date mulai, java.util.Date sampai) {
		// Cakupan penyewa (satuan kerja): tanpa ini, jalur API men-scan/memposting
		// dokumen dana talangan SELURUH instalasi (lintas Yayasan), bukan hanya milik
		// penyewa yang sedang memanggil -- lihat catatan sama pada
		// PostingTransaksiPembayaranGajiAction.kriteriaPostingStatic(). Himpunan kosong
		// (Yayasan tidak teridentifikasi) fail-CLOSED, bukan fail-open seperti
		// initCriteria(boolean) pada layar ZK.
		Set<SatuanKerja> satuanKerjasPengguna = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		Criteria c = session.createCriteria(DanaTalangan.class)
				.add(Restrictions.isNotNull("disetujuiOleh"))
				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
				.add(satuanKerjasPengguna.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.in("satuanKerja", satuanKerjasPengguna)));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/**
	 * Batalkan posting SEMUA dana talangan terposting dalam rentang: hapus jurnal yang belum
	 * closing, lalu lepas penanda postingnya. Transaksinya dibuka sendiri karena dipanggil
	 * dari API, di mana tidak ada kerangka ZK yang meng-commit-kan sesi berjalan.
	 *
	 * @return jumlah dokumen yang berhasil dibatalkan postingnya.
	 */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<DanaTalangan> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (DanaTalangan dok : daftar) {
				try {
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where dana_talangan=" + dok.getId()
							+ "  and closing is null)").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where dana_talangan="
							+ dok.getId() + " and closing is null").executeUpdate();
					dok.setPostingHistory(null);
					session.update(dok);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					Common.tampilErrorJikaAdmin(e);
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

	/**
	 * Posting SEMUA dana talangan yang belum diposting dalam rentang. Pasangan akunnya sama
	 * dengan layar: debet ke akun jenis uang muka milik uang muka yang ditalangi, kredit ke
	 * akun kelebihan pada sumber dana talangannya.
	 *
	 * @return jumlah dokumen yang berhasil diposting.
	 */
	@SuppressWarnings("unchecked")
	public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		PostingHistory postingHistory = null;
		try {
			List<DanaTalangan> kandidat = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory")).list();
			List<DanaTalangan> daftar = new java.util.ArrayList<DanaTalangan>();
			for (DanaTalangan dok : kandidat) {
				Akun akunDebet = dok == null || dok.getUangMuka() == null
						|| dok.getUangMuka().getJenisUangMuka() == null ? null
						: dok.getUangMuka().getJenisUangMuka().getAkun();
				Akun akunKredit = dok == null || dok.getJenisUangMuka() == null ? null
						: dok.getJenisUangMuka().getAkunKelebihan();
				if (akunDebet != null && akunKredit != null && dok.getNilai() != null
						&& Math.abs(dok.getNilai()) > 0.1) {
					daftar.add(dok);
				}
			}
			// Validasi seluruh kandidat sebelum membuat riwayat posting. Selain mencegah
			// PostingHistory kosong, ini membuat angka draft hanya diproses bila kedua
			// akun jurnalnya memang tersedia.
			if (daftar.isEmpty()) {
				return 0;
			}

			postingHistory = new PostingHistory(PostingHistory.JENIS_PERSETUJUAN_UANG_MUKA);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal dana talangan dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			for (DanaTalangan dok : daftar) {
				if (dok == null) {
					continue;
				}
				try {
					Akun akunDebet = dok.getUangMuka() == null || dok.getUangMuka().getJenisUangMuka() == null ? null
							: dok.getUangMuka().getJenisUangMuka().getAkun();
					Akun akunKredit = dok.getJenisUangMuka() == null ? null
							: dok.getJenisUangMuka().getAkunKelebihan();
					if (akunDebet == null || akunKredit == null) {
						continue;
					}
					Double nilai = dok.getNilai();
					if (nilai == null || nilai <= 0.1) {
						continue;
					}

					String ket = "Persetujuan dana talangan \"" + dok.getKode() + "\" senilai "
							+ Common.numberFormat.get().format(nilai);

					session.getTransaction().begin();
					boolean tersimpan = CommonAkunting.saveTransaksi(akunDebet, akunKredit, null, null,
							postingHistory, true, ket, dok.getTanggalPersetujuan(), nilai, 0.0, dok,
							dok.getSatuanKerja(), session);
					if (!tersimpan) {
						session.getTransaction().rollback();
						ais.common.ErrorAuditUtil.record(
								new IllegalStateException("Tanggal jurnal Dana Talangan " + dok.getKode()
										+ " berada pada periode yang sudah closing."),
								"PostingDanaTalanganAction jalur API");
						continue;
					}
					dok.setPostingHistory(postingHistory);
					session.update(dok);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						if (session.getTransaction().isActive()) session.getTransaction().rollback();
					} catch (Exception rollbackError) {
						ais.common.ErrorAuditUtil.record(rollbackError,
								"PostingDanaTalanganAction rollback jalur API");
					}
					ais.common.ErrorAuditUtil.record(e, "PostingDanaTalanganAction jalur API");
				}
			}

			// Bila semua dokumen gagal, riwayat massal ini tidak merepresentasikan jurnal
			// apa pun. Hapus kembali agar audit trail tidak dipenuhi riwayat kosong.
			if (n == 0 && postingHistory != null) {
				try {
					session.getTransaction().begin();
					session.delete(postingHistory);
					session.getTransaction().commit();
				} catch (Exception e) {
					try {
						if (session.getTransaction().isActive()) session.getTransaction().rollback();
					} catch (Exception rollbackError) {
						ais.common.ErrorAuditUtil.record(rollbackError,
								"PostingDanaTalanganAction rollback hapus riwayat kosong");
					}
					ais.common.ErrorAuditUtil.record(e,
							"PostingDanaTalanganAction hapus riwayat kosong jalur API");
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


}
