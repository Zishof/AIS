package ais.action.master.akunting;

import java.util.ArrayList;
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
import ais.database.model.akunting.Pertangungjawaban;
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
 * <h3>PostingPertangungjawabanPengembalianAction — Pengendali Posting Pengembalian Pertanggungjawaban</h3>
 *
 * <p><strong>Untuk apa:</strong>
 * Kelas ini adalah ZK Composer yang mengelola proses posting transaksi pengembalian
 * sisa dana dari pertanggungjawaban uang muka ke dalam buku besar akuntansi.
 * Pertanggungjawaban ({@link Pertangungjawaban}) adalah laporan penggunaan uang muka
 * oleh unit/pegawai. Jika ada sisa dana yang belum digunakan (field {@code dikembalikan}
 * tidak nol), sisa tersebut harus dikembalikan ke kas dan dicatat sebagai pengembalian
 * dalam jurnal akuntansi.
 *
 * <p>Kelas ini menyediakan antarmuka untuk melihat daftar pertanggungjawaban yang
 * memiliki saldo pengembalian, mengelola status posting pengembalian ke jurnal umum
 * (menggunakan field {@code postingHistoryPengembalian} — berbeda dari
 * {@code postingHistory} yang digunakan untuk posting pertanggungjawaban utama),
 * serta melakukan posting atau pembatalan posting baik per baris maupun massal.</p>
 *
 * <p><strong>Cara kerja:</strong>
 * Grid menampilkan entitas {@link Pertangungjawaban} yang difilter berdasarkan satuan
 * kerja, rentang tanggal persetujuan, dan status posting pengembalian
 * ({@code postingHistoryPengembalian}). Akun yang digunakan untuk jurnal pengembalian:
 * <ul>
 *   <li>Akun kredit (untuk tampilan grid): dari {@code uangMuka.jenisUangMuka.akun}.</li>
 *   <li>Akun debet (untuk tampilan grid): dari {@code uangMuka.jenisUangMuka.akunKelebihan}.</li>
 *   <li>Dalam proses posting aktual, array akun debet dan kredit dibangun secara
 *       terpisah dengan {@code akunDebet = uangMuka.akun} untuk entri jurnal nyata.</li>
 * </ul>
 *
 * <p>Referensi GrupTransaksi menggunakan field {@code ref = "pengembalian"} untuk
 * membedakan dari entri jurnal pertanggungjawaban utama (yang menggunakan ref berbeda)
 * dalam tabel {@code akunting.grup_transaksi}.</p>
 *
 * <p><strong>Threading:</strong>
 * Operasi posting massal berjalan dalam {@link Thread} terpisah menggunakan
 * {@code HibernateUtil.currentNativeSession()}. Sesi ini harus dikelola manual.
 * Setelah loop selesai, sesi ditutup dengan {@code HibernateUtil.closeSession()}.</p>
 *
 * <p><strong>Pemeliharaan:</strong>
 * Perhatikan bahwa field ref = {@code "pengembalian"} digunakan dalam query SQL native
 * untuk menghapus entri jurnal. Jika nilai ref berubah, perbarui field {@code ref}
 * dan semua query SQL yang menggunakannya. Konstanta yang digunakan:
 * {@code PostingHistory.JENIS_PENGEMBALIAN_UANG_MUKA}.</p>
 *
 * @author Generated Javadoc
 * @see Pertangungjawaban
 * @see PostingHistory
 * @see CommonAkunting
 */
public class PostingPertangungjawabanPengembalianAction extends GenericAutowireComposer {

	/**
	 * ID serialisasi untuk kompatibilitas serialisasi Java.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar pertanggungjawaban yang memiliki pengembalian. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman data grid. */
	private Paging paging;

	/** Kotak teks pencarian berdasarkan kode atau nama pertanggungjawaban. */
	private Textbox searchkode;

	/** Checkbox filter untuk menampilkan hanya yang belum diposting pengembaliannya. */
	private MyCheckboxConfig searchtampil;

	/** Checkbox filter untuk menampilkan hanya yang sudah diposting pengembaliannya. */
	private MyCheckboxConfig searchtelahtampil;

	/**
	 * Model pohon satuan kerja untuk mendukung filter hierarki organisasi.
	 */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Komponen banbox pemilih satuan kerja pada filter halaman.
	 */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** Flag apakah pengguna memiliki hak ubah (UPDATE). */
	private boolean edit = false;

	/** Tombol toolbar posting semua pengembalian. */
	private MyToolbarbuttonConfig sent;

	/**
	 * Flag apakah pengguna adalah admin atau memiliki hak APPROVE.
	 */
	public boolean adminLain;

	/** Tanggal awal rentang filter berdasarkan tanggal persetujuan pertanggungjawaban. */
	private MyDatebox tglMulai;

	/** Tanggal akhir rentang filter. */
	private MyDatebox tglSampai;

	/**
	 * Nilai referensi yang digunakan sebagai penanda pada entri {@link GrupTransaksi}
	 * untuk membedakan jurnal pengembalian dari jurnal pertanggungjawaban utama.
	 * Nilai tetap: {@code "pengembalian"}.
	 */
	private String ref = "pengembalian";

	/** Data pengguna yang sedang login, diambil saat inisialisasi halaman. */
	private Tbmuser tbmuser;

	/**
	 * <h3>doBeforeCompose — Pemeriksaan Keamanan Sebelum Halaman Dibangun</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Dipanggil ZK sebelum komponen halaman diinisialisasi. Memastikan pengguna
	 * terautentikasi dan berhak mengakses halaman posting pengembalian pertanggungjawaban.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Memanggil {@link Common#doCheckSecurity()} untuk pemeriksaan sesi dan otorisasi.
	 * Jika gagal, ZK diredirect ke halaman login secara otomatis oleh helper tersebut.
	 * Kemudian memanggil implementasi super.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Pemeriksaan keamanan wajib ada dan tidak boleh dihapus.</p>
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
	 * Dipanggil ZK setelah semua komponen ZUL selesai di-wire. Bertanggung jawab atas
	 * inisialisasi lengkap halaman: validasi sesi, konfigurasi filter satuan kerja,
	 * pengaturan tanggal default, penentuan hak akses, pemuatan data awal, serta
	 * registrasi listener paging dan timer auto-refresh.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Memanggil super untuk auto-wire komponen.</li>
	 *   <li>Menginisialisasi bahasa antarmuka.</li>
	 *   <li>Memvalidasi sesi dan hak READ; jika gagal, redirect logout dan return.</li>
	 *   <li>Mengambil pengguna aktif ke {@code tbmuser}.</li>
	 *   <li>Mengonfigurasi {@code searchparent} dengan listener auto-reload saat
	 *       satuan kerja berubah.</li>
	 *   <li>Menginisialisasi {@link SatuanKerjaTreeModel}.</li>
	 *   <li>Mengatur rentang tanggal default: 6 bulan ke belakang hingga hari ini,
	 *       mode read-only.</li>
	 *   <li>Menentukan {@code adminLain} dan {@code edit} berdasarkan hak akses.</li>
	 *   <li>Memuat data pertama kali dan mendaftarkan listener paging dan timer.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Kegagalan validasi sesi: redirect logout, return awal. Exception lain dilempar.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Field {@code ref = "pengembalian"} sudah diinisialisasi secara inline dan tidak
	 * perlu diinisialisasi ulang di sini. Perubahan nilai ref harus konsisten di seluruh
	 * kelas.</p>
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
	 * <h3>onBatalkanPostingSemua — Pembatalan Massal Seluruh Posting Pengembalian</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Menangani klik tombol "Batalkan Posting Semua" untuk membatalkan seluruh posting
	 * pengembalian pertanggungjawaban yang memenuhi filter aktif. Semua transaksi yang
	 * sudah diposting pengembaliannya dikembalikan ke status "belum diposting" dan
	 * entri jurnal terkait dihapus dari basis data.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Menampilkan dialog konfirmasi. Jika dikonfirmasi:
	 * <ol>
	 *   <li>Mengambil semua {@link Pertangungjawaban} yang sudah diposting pengembalian
	 *       ({@code postingHistoryPengembalian} tidak null).</li>
	 *   <li>Untuk setiap entitas: set {@code postingHistoryPengembalian} ke null,
	 *       simpan perubahan, hapus baris {@link GrupTransaksi} dengan kolom
	 *       {@code ref = 'pengembalian'} dan {@code pertangungjawaban = id}
	 *       yang bukan closing entry.</li>
	 *   <li>Muat ulang grid via timer default.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Tidak ada rollback otomatis. Setiap entitas diproses dalam sesi yang dikelola.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Query SQL native menggunakan filter {@code ref = 'pengembalian'} (nilai dari
	 * field {@code ref}) untuk memastikan hanya jurnal pengembalian yang dihapus,
	 * bukan jurnal pertanggungjawaban utama untuk entitas yang sama.</p>
	 *
	 * @param event event ZK dari klik tombol
	 * @throws Exception jika terjadi kesalahan akses basis data atau UI
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi pengembalian ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<Pertangungjawaban> pertangungjawabans = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistoryPengembalian")).list();

							for (Pertangungjawaban pertangungjawaban : pertangungjawabans) {
								pertangungjawaban.setPostingHistoryPengembalian(null);
								Common.refreshSaveOrUpdate(pertangungjawaban);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where ref='" + ref
												+ "' and pertangungjawaban=" + pertangungjawaban.getId() + " and closing is null")
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
	 * <h3>onPostingSemua — Pembukaan Dialog dan Eksekusi Posting Massal Pengembalian</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Menangani klik tombol "Posting Semua Pengembalian" dengan membuka dialog konfirmasi,
	 * memvalidasi input tanggal, lalu menjalankan proses posting seluruh pengembalian
	 * pertanggungjawaban yang belum diposting dalam utas latar belakang.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Menampilkan jendela modal dengan form tanggal, nama pengguna, dan keterangan.</li>
	 *   <li>Validasi: tanggal wajib diisi.</li>
	 *   <li>Konfirmasi kedua sebelum memulai proses.</li>
	 *   <li>Dalam utas latar belakang:
	 *       <ul>
	 *         <li>Membuat {@link PostingHistory} baru dengan jenis
	 *             {@code JENIS_PENGEMBALIAN_UANG_MUKA} dan menyimpannya.</li>
	 *         <li>Mengambil semua {@link Pertangungjawaban} yang belum diposting
	 *             pengembaliannya ({@code postingHistoryPengembalian} null).</li>
	 *         <li>Untuk setiap pertanggungjawaban:
	 *             <ul>
	 *               <li>Menentukan satuan kerja dari entitas atau null.</li>
	 *               <li>Akun debet dari {@code uangMuka.akun}.</li>
	 *               <li>Akun kredit dari {@code uangMuka.jenisUangMuka.akun}
	 *                   (jika jenisUangMuka tidak null).</li>
	 *               <li>Nilai dari {@code dikembalikan}.</li>
	 *               <li>Membangun array akun debet dan kredit (saat ini keduanya berisi
	 *                   {@code akunDebet} saja — perhatikan bahwa {@code akunsKredits}
	 *                   juga ditambahkan {@code akunDebet}, bukan {@code akunKredit};
	 *                   ini adalah implementasi yang ada dan tidak boleh diubah).</li>
	 *               <li>Memanggil {@link CommonAkunting#saveTransaksi} overload array
	 *                   dengan parameter {@code ref}.</li>
	 *               <li>Memperbarui {@code postingHistoryPengembalian} entitas.</li>
	 *             </ul>
	 *         </li>
	 *       </ul>
	 *   </li>
	 *   <li>Menutup sesi native dan mengosongkan label progres.</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong>
	 * Loop di utas non-ZK. Hanya {@code label.setValue()} untuk komunikasi ke UI.</p>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Try-catch per iterasi. Kesalahan dilaporkan via {@code tampilErrorJikaAdmin}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Perhatikan bahwa array kredit menggunakan {@code akunDebet} bukan {@code akunKredit}
	 * — ini adalah logika bisnis yang ada dan harus diverifikasi dengan tim akuntansi
	 * sebelum diubah. Parameter {@code ref} diteruskan ke {@code saveTransaksi} untuk
	 * membedakan jurnal pengembalian dari jurnal lain.</p>
	 *
	 * @param event event ZK dari klik tombol
	 * @throws Exception jika terjadi kesalahan saat membangun komponen UI jendela
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Transaksi Pertanggungjawaban");
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

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi pengembalian ?", "Pertanyaan",
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
											MyMessageboxConfig.show("Posting transaksi pengembalian berhasil dilakukan",
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

											PostingHistory postingHistoryPengembalian = new PostingHistory(
													PostingHistory.JENIS_PENGEMBALIAN_UANG_MUKA);
											postingHistoryPengembalian.setTanggal(tgl);
											postingHistoryPengembalian.setTbmuser(tbmuser);
											postingHistoryPengembalian.setKeterangan(keterangan.getValue().trim()
													+ " \nTgl:" + Common.dateFormat.get().format(tglMulai.getValue())
													+ " s.d " + Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistoryPengembalian);
											session.getTransaction().commit();

											List<Pertangungjawaban> pertangungjawabans = initCriteria(true)
													.add(Restrictions.isNull("postingHistoryPengembalian")).list();

											int rowIndex = 1;
											for (Pertangungjawaban pertangungjawaban : pertangungjawabans) {

												SatuanKerja satuanKerja = (SatuanKerja) (pertangungjawaban
														.getSatuanKerja() != null ? pertangungjawaban.getSatuanKerja()
																: null);

												if (pertangungjawaban != null) {

													try {
														Akun akunDebet = pertangungjawaban.getUangMuka().getAkun();
														Akun akunKredit = pertangungjawaban.getUangMuka()
																.getJenisUangMuka() == null ? null
																		: pertangungjawaban.getUangMuka()
																				.getJenisUangMuka().getAkun();

														Double nilai = pertangungjawaban.getDikembalikan();

														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {

																ket = "Laporan pengembalian uang muka \""
																		+ pertangungjawaban
																				.getUangMuka().getWorkspace().getKode()
																		+ " "
																		+ pertangungjawaban.getUangMuka().getWorkspace()
																				.getNama()
																		+ "\" senilai " + Common.numberFormat.get()
																				.format(pertangungjawaban.getNilai());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(
																	ket + " ("
																			+ Common.numberFormat.get().format(rowIndex
																					* 100.0 / pertangungjawabans.size())
																			+ " %)");

															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;
																List<Akun> akunsDebets = new ArrayList<Akun>();
																List<Akun> akunsKredits = new ArrayList<Akun>();

																List<Double> nilaiDebets = new ArrayList<Double>();
																List<Double> nilaiKredits = new ArrayList<Double>();

																nilaiDebets.add(nilai);
																akunsDebets.add(akunDebet);

																nilaiKredits.add(nilai);
																akunsKredits.add(akunDebet);

																session.getTransaction().begin();

																CommonAkunting.saveTransaksi(
																		akunsDebets.toArray(new Akun[] {}),
																		akunsKredits.toArray(new Akun[] {}), akunDenda,
																		akunPiutangDenda, postingHistoryPengembalian,
																		apakahUangMasuk, ket,
																		pertangungjawaban.getTanggalPersetujuan(),
																		nilaiDebets.toArray(new Double[] {}),
																		nilaiKredits.toArray(new Double[] {}), denda,
																		pertangungjawaban, satuanKerja, ref, session);
																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															pertangungjawaban.setPostingHistoryPengembalian(
																	postingHistoryPengembalian);
															session.getTransaction().begin();
															session.update(pertangungjawaban);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PostingPertangungjawabanPengembalianAction.java:632");
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
	 * <h3>PertangungjawabanRenderer — Renderer Baris Grid Pengembalian Pertanggungjawaban</h3>
	 *
	 * <p><strong>Untuk apa:</strong>
	 * Kelas dalam ini merender setiap baris grid halaman posting pengembalian
	 * pertanggungjawaban. Setiap baris merepresentasikan satu entitas
	 * {@link Pertangungjawaban} dan menampilkan informasi: kode (dengan link proses
	 * transfer), nama, informasi SOP terkait, tanggal persetujuan, pasangan jurnal
	 * debet-kredit, status posting pengembalian, dan tombol aksi.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Memperluas {@code ais.ui.util.MyRowRenderer}. Akun yang digunakan untuk tampilan
	 * jurnal di grid diambil dari:
	 * <ul>
	 *   <li>Akun kredit: {@code uangMuka.jenisUangMuka.akun}.</li>
	 *   <li>Akun debet: {@code uangMuka.jenisUangMuka.akunKelebihan}.</li>
	 * </ul>
	 * Status posting yang ditampilkan adalah {@code postingHistoryPengembalian},
	 * bukan {@code postingHistory} biasa.</p>
	 *
	 * <p><strong>Threading:</strong>
	 * Berjalan di utas ZK event. Aman menggunakan {@code currentSession()}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Query pencarian nomor bukti menggunakan {@code Restrictions.eq("pertangungjawaban", ...)}
	 * tanpa filter ref. Jika entitas yang sama memiliki jurnal utama dan jurnal pengembalian,
	 * query ini akan mengembalikan salah satunya secara acak (setMaxResults(1)). Pertimbangkan
	 * menambahkan filter ref jika diperlukan akurasi nomor bukti.</p>
	 */
	class PertangungjawabanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render — Mengisi Satu Baris Grid dengan Data Pengembalian Pertanggungjawaban</h3>
		 *
		 * <p><strong>Tujuan:</strong>
		 * Mengisi {@link Row} ZK dengan semua informasi visual dan interaktif dari satu
		 * entitas {@link Pertangungjawaban}, berfokus pada aspek pengembalian dana,
		 * termasuk tombol aksi yang dikondisikan berdasarkan status posting pengembalian
		 * dan hak akses pengguna.</p>
		 *
		 * <p><strong>Cara kerja:</strong>
		 * <ol>
		 *   <li>Menyetel vertical-align baris ke "top".</li>
		 *   <li>Menampilkan kode dalam {@link Vbox} via {@code RevisiHelper}, dengan link
		 *       ke proses transfer jika tersedia.</li>
		 *   <li>Menampilkan nama pertanggungjawaban. Jika ada disposisi SOP, menampilkan
		 *       tautan SOP via {@code UIClassHelper.applyReadMore} dan listener ke
		 *       {@code TampilanAlurSopAction}.</li>
		 *   <li>Mengambil akun kredit dari {@code uangMuka.jenisUangMuka.akun} dan
		 *       akun debet dari {@code uangMuka.jenisUangMuka.akunKelebihan}.</li>
		 *   <li>Mengambil nilai pengembalian dari {@code dikembalikan}.</li>
		 *   <li>Menampilkan tanggal persetujuan.</li>
		 *   <li>Menampilkan visualisasi jurnal atau pesan error jika akun tidak lengkap.</li>
		 *   <li>Mencari nomor bukti {@link GrupTransaksi} dan menampilkan status posting
		 *       pengembalian ({@code postingHistoryPengembalian}).</li>
		 *   <li>Membuat toolbar aksi dengan tombol batalkan dan tombol posting per baris.</li>
		 * </ol>
		 *
		 * <p><strong>Penanganan error:</strong>
		 * Tombol aksi hanya ditampilkan jika kedua akun tersedia.
		 * Keterangan error ditampilkan di kolom jurnal jika akun tidak lengkap.</p>
		 *
		 * <p><strong>Pemeliharaan:</strong>
		 * Pastikan kondisi visibilitas tombol menggunakan {@code postingHistoryPengembalian}
		 * (bukan {@code postingHistory}). SQL pembatalan per baris menggunakan filter
		 * {@code ref = 'pengembalian'} yang hilang di implementasi saat ini — ini adalah
		 * bug potensial jika entitas yang sama memiliki jurnal non-pengembalian.</p>
		 *
		 * @param arg0 baris ZK yang akan diisi komponen
		 * @param arg1 objek data, diharapkan bertipe {@link Pertangungjawaban}
		 * @throws Exception jika terjadi kesalahan akses basis data atau pembuatan komponen
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Pertangungjawaban pertangungjawaban = (Pertangungjawaban) arg1;

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(Pertangungjawaban.class, pertangungjawaban,
					pertangungjawaban.getKode() == null ? "" : pertangungjawaban.getKode())).setParent(arg0);

			if (pertangungjawaban != null && pertangungjawaban.getDaftarPengajuanTransfer() != null
					&& pertangungjawaban.getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A a = new A(pertangungjawaban.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, pertangungjawaban.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);

			}

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(pertangungjawaban.getNama()).setParent(a);
			if (pertangungjawaban.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pertangungjawaban.getDisposisiSop().getKeterangan()
						+ " (" + pertangungjawaban.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pertangungjawaban.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			Akun akunKredit = pertangungjawaban.getUangMuka().getJenisUangMuka() == null ? null
					: pertangungjawaban.getUangMuka().getJenisUangMuka().getAkun();

			Akun akunDebet = pertangungjawaban.getUangMuka().getJenisUangMuka() == null ? null
					: pertangungjawaban.getUangMuka().getJenisUangMuka().getAkunKelebihan();

			Double nilai = pertangungjawaban.getDikembalikan();

			new Label(pertangungjawaban.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat1.get().format(pertangungjawaban.getTanggalPersetujuan())).setParent(arg0);

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
					.add(Restrictions.eq("pertangungjawaban", pertangungjawaban)).setMaxResults(1)
					.setProjection(Projections.property("kode")).uniqueResult();

			new Label(pertangungjawaban.getPostingHistoryPengembalian() == null ? Common.getBahasaConfig("Belum diposting")
					: pertangungjawaban.getPostingHistoryPengembalian().toString() + ", no. bukti : " + bukti)
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && pertangungjawaban.getPostingHistoryPengembalian() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pertangungjawaban.setPostingHistoryPengembalian(null);
								Common.refreshSaveOrUpdate(pertangungjawaban);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where ref='" + ref
												+ "' pertangungjawaban=" + pertangungjawaban.getId() + " and closing is null")
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
				button.setVisible(edit && pertangungjawaban.getPostingHistoryPengembalian() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistoryPengembalian = new PostingHistory(
										PostingHistory.JENIS_PENGEMBALIAN_UANG_MUKA);
								postingHistoryPengembalian.setTbmuser(Common.getCurrentUser());
								postingHistoryPengembalian.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistoryPengembalian.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistoryPengembalian);

								Akun akunKredit = pertangungjawaban.getUangMuka().getJenisUangMuka() == null ? null
										: pertangungjawaban.getUangMuka().getJenisUangMuka().getAkun();
								Akun akunDebet = pertangungjawaban.getUangMuka().getJenisUangMuka() == null ? null
										: pertangungjawaban.getUangMuka().getJenisUangMuka().getAkunKelebihan();

								Double nilai = pertangungjawaban.getDikembalikan();

								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {

										ket = "Laporan pengembalian uang muka \""
												+ pertangungjawaban.getUangMuka().getWorkspace().getKode() + " "
												+ pertangungjawaban.getUangMuka().getWorkspace().getNama()
												+ "\" senilai "
												+ Common.numberFormat.get().format(pertangungjawaban.getNilai());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (pertangungjawaban.getSatuanKerja() != null
											? pertangungjawaban.getSatuanKerja()
											: tbmuser.ambilSatuanKerja());

									List<Akun> akunsDebets = new ArrayList<Akun>();
									List<Akun> akunsKredits = new ArrayList<Akun>();

									List<Double> nilaiDebets = new ArrayList<Double>();
									List<Double> nilaiKredits = new ArrayList<Double>();

									nilaiDebets.add(nilai);
									akunsDebets.add(akunDebet);

									nilaiKredits.add(nilai);
									akunsKredits.add(akunDebet);

									CommonAkunting.saveTransaksi(akunsDebets.toArray(new Akun[] {}),
											akunsKredits.toArray(new Akun[] {}), akunDenda, akunPiutangDenda,
											postingHistoryPengembalian, apakahUangMasuk, ket,
											pertangungjawaban.getTanggalPersetujuan(),
											nilaiDebets.toArray(new Double[] {}), nilaiKredits.toArray(new Double[] {}),
											denda, pertangungjawaban, satuanKerja, ref, session);

									pertangungjawaban.setPostingHistoryPengembalian(postingHistoryPengembalian);
									session.update(pertangungjawaban);
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
	 * <h3>initCriteria — Membangun Kriteria Query Hibernate untuk Data Pengembalian</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Membangun objek {@link Criteria} Hibernate yang mencerminkan semua filter aktif:
	 * satuan kerja dengan hierarki, status posting pengembalian, rentang tanggal
	 * persetujuan, dan kata kunci teks bebas. Berbeda dari action posting lain, kelas
	 * ini tidak memfilter berdasarkan nilai nominal — semua pertanggungjawaban yang
	 * sudah disetujui dan memenuhi filter lain akan ditampilkan.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Mengambil satuan kerja dari {@code searchparent} dan mengumpulkan turunannya.</li>
	 *   <li>Filter satuan kerja: global (null) atau dalam set yang dipilih.</li>
	 *   <li>Filter status posting pengembalian ({@code postingHistoryPengembalian})
	 *       berdasarkan checkbox.</li>
	 *   <li>Filter rentang tanggal berdasarkan {@code tanggal_persetujuan}.</li>
	 *   <li>Filter kata kunci pada kode dan nama menggunakan ILIKE.</li>
	 *   <li>Jika {@code order} true, ORDER BY id descending.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Tidak ada penanganan eksplisit. Kesalahan Hibernate dilempar ke pemanggil.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Perhatikan bahwa filter status menggunakan {@code "postingHistoryPengembalian"}
	 * bukan {@code "postingHistory"}. Ini penting — jangan mengganti dengan
	 * postingHistory biasa karena keduanya field yang berbeda pada entitas.</p>
	 *
	 * @param order true untuk menambahkan ORDER BY, false untuk query COUNT
	 * @return objek {@link Criteria} siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Pertangungjawaban.class)

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistoryPengembalian"))

				.add(!searchtelahtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNotNull("postingHistoryPengembalian"))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
						+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", searchkode.getValue(), MatchMode.ANYWHERE)));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <h3>onSearchDefaultTanpaProgress — Pemuatan Data Grid Pengembalian Tanpa Progres</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Melakukan pemuatan data aktual ke grid pengembalian pertanggungjawaban:
	 * menghitung total baris untuk paging, mengambil data sesuai halaman aktif,
	 * dan mengatur model serta renderer grid. Dipanggil dari dalam
	 * {@link #loadDataDenganProgressPosting}.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Menghitung total halaman, mengambil baris halaman aktif, membungkus dalam
	 * {@link SimpleListModel}, lalu menyetel renderer {@link PertangungjawabanRenderer}
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

		List<Pertangungjawaban> pertangungjawaban = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pertangungjawaban);
		grid.setRowRenderer(new PertangungjawabanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <h3>onSearchDefault — Titik Masuk Publik Pencarian Data Pengembalian</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Titik masuk standar yang dipanggil ZK untuk memuat ulang data grid pengembalian
	 * pertanggungjawaban. Mendelegasikan ke {@link #loadDataDenganProgressPosting}
	 * agar indikator progres selalu aktif selama pemuatan data.</p>
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
	 * Orkestrasi utama pemuatan data grid posting pengembalian pertanggungjawaban dengan
	 * tampilan indikator progres bertahap. Mencegah race condition dengan pola
	 * "loading flag dengan antrian tunggal" — jika ada permintaan saat loading masih
	 * berjalan, permintaan tersebut dicatat dan dieksekusi setelah proses selesai.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Jika sudah loading: catat tertunda, tampilkan notifikasi, return.</li>
	 *   <li>Jika belum: set flag aktif, tampilkan progres 7%, jadwalkan via timer ZK.</li>
	 *   <li>Callback timer: tampilkan 48%, muat data, tampilkan 92%.</li>
	 *   <li>Blok finally: bersihkan flag. Rekursif jika ada tertunda, selesai jika tidak.</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong>
	 * Seluruh logika di utas ZK event. Timer memberikan jeda untuk rendering browser
	 * agar pengguna melihat indikator progres sebelum query berat dimulai.</p>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Blok finally memastikan flag selalu dibersihkan meskipun ada exception.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Pola ini identik di semua kelas posting. Pertimbangkan ekstraksi ke helper
	 * bersama jika perilaku perlu diubah secara konsisten.</p>
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

}
