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
import ais.database.model.akunting.KasBesar;
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
 * <h3>PostingKasBesarAction — Pengelola Posting Jurnal Kas Besar ke Buku Besar</h3>
 *
 * <p><strong>Untuk apa:</strong> Kelas ini adalah ZK GenericAutowireComposer yang
 * mengelola proses posting transaksi {@link KasBesar} ke jurnal umum (buku besar)
 * dalam sistem akuntansi AIS. Kas Besar merupakan transaksi pengeluaran kas yang
 * telah disetujui melalui mekanisme kas besar (pengisian ulang kas kecil atau
 * pengeluaran langsung dari rekening). Posting berarti membuat entri jurnal akuntansi
 * (GrupTransaksi dengan detail debet/kredit) yang merepresentasikan transaksi tersebut
 * dalam buku besar, sehingga laporan keuangan dapat dihasilkan.</p>
 *
 * <p><strong>Cara kerja:</strong> Alur kerja utama kelas ini adalah:</p>
 * <ol>
 *   <li>Halaman menampilkan daftar KasBesar yang telah disetujui (disetujuiOleh tidak
 *       null) dengan nilai tidak nol, difilter berdasarkan rentang tanggal persetujuan,
 *       satuan kerja, kode/nama, dan status posting.</li>
 *   <li>Setiap baris grid menampilkan kode KasBesar, nama, nilai, tanggal persetujuan,
 *       preview jurnal (debet/kredit), status posting, dan tombol aksi.</li>
 *   <li>Tombol "Posting Semua" ({@link #onPostingSemua}) membuka dialog konfirmasi
 *       tanggal dan keterangan posting, kemudian menjalankan proses posting massal
 *       dalam thread latar belakang menggunakan {@code currentNativeSession()}.</li>
 *   <li>Tombol per baris "Posting Data" dan "Batalkan Posting" tersedia di setiap baris
 *       untuk posting atau pembatalan individual.</li>
 *   <li>Tombol "Batalkan Posting Semua" ({@link #onBatalkanPostingSemua}) membatalkan
 *       semua posting dalam filter aktif setelah konfirmasi.</li>
 *   <li>Halaman mendukung parameter URL {@code sudah_posting} untuk mode tampilan
 *       terfokus (sudah/belum diposting) yang menyembunyikan filter dan menampilkan
 *       info posting.</li>
 * </ol>
 *
 * <p><strong>Logika Jurnal:</strong> Untuk KasBesar yang berasal dari KasKecil
 * (pengisian ulang): Debet = akun JenisKasKecil, Kredit = akun JenisKasBesar. Untuk
 * KasBesar langsung: Debet = akun penerima JenisKasBesar, Kredit = akun JenisKasBesar.
 * Jika nilai negatif (pembayaran balik), posisi debet/kredit dibalik.</p>
 *
 * <p><strong>Threading:</strong> Proses posting massal ({@link #onPostingSemua})
 * menggunakan thread Java terpisah dengan {@code HibernateUtil.currentNativeSession()}
 * (session native yang harus dikelola secara manual: begin/commit/close). Thread ZK
 * event menggunakan {@code currentSession()} (managed). Untuk pembatalan dan posting
 * individual, digunakan managed session karena berjalan pada thread ZK event.</p>
 *
 * <p><strong>Mekanisme Progress:</strong> Metode {@link #loadDataDenganProgressPosting}
 * mengelola flag {@code postingJurnalLoadingAktif} untuk mencegah reload bersamaan.
 * Jika ada permintaan reload saat loading berlangsung, flag
 * {@code postingJurnalReloadTertunda} diset dan reload akan dieksekusi setelah proses
 * selesai. Ini mencegah race condition pada pembaruan grid.</p>
 *
 * <p><strong>Pemeliharaan:</strong> Jika ada perubahan pada logika jurnal KasBesar
 * (misalnya penambahan akun denda), perbarui logika di {@link #onPostingSemua} dan
 * di {@link KasBesarRenderer} (tombol posting per baris). Kedua tempat harus selalu
 * sinkron. Jika ada filter baru, perbarui {@link #initCriteria}.</p>
 *
 * @see KasBesar
 * @see PostingHistory
 * @see GrupTransaksi
 * @see CommonAkunting
 */
public class PostingKasBesarAction extends GenericAutowireComposer {

	/** Serial version UID untuk serialisasi kelas. */
	private static final long serialVersionUID = -5779730217402400328L;

	private MyGrid grid;

	private Paging paging;

	private Textbox searchkode;
	private MyCheckboxConfig searchtampil;
	private MyCheckboxConfig searchtelahtampil;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private boolean edit = false;

	private MyToolbarbuttonConfig sent;
	/** Flag apakah pengguna memiliki hak akses admin atau hak APPROVE. */
	public boolean adminLain;

	private MyDatebox tglMulai;
	private MyDatebox tglSampai;

	private North filter;
	private Tbmuser tbmuser;
	private Row rowPosting;
	/** Nilai dari parameter URL sudah_posting; null jika tidak ada parameter tersebut. */
	private Boolean sudah_posting = null;

	/**
	 * Dipanggil sebelum komponen ZK dirakit untuk melakukan pemeriksaan keamanan halaman.
	 *
	 * <p><strong>Tujuan:</strong> Memastikan hanya pengguna dengan sesi valid yang dapat
	 * mengakses halaman posting kas besar sebelum komponen UI dibangun.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memanggil {@code Common.doCheckSecurity()} untuk
	 * verifikasi sesi, kemudian melanjutkan ke implementasi parent ZK.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Ditangani oleh {@code Common.doCheckSecurity()}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Boilerplate keamanan standar; tidak perlu diubah.</p>
	 *
	 * @param page     halaman ZK yang sedang dimuat
	 * @param parent   komponen induk dalam pohon ZK
	 * @param compInfo metadata komponen ZK
	 * @return ComponentInfo dari pemanggilan super
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Dipanggil setelah semua komponen ZK berhasil dirakit. Melakukan inisialisasi
	 * lengkap halaman posting kas besar termasuk parameter URL, filter tanggal,
	 * hak akses, dan pemuatan data awal.
	 *
	 * <p><strong>Tujuan:</strong> Menyiapkan semua aspek halaman agar siap digunakan,
	 * termasuk menangani mode khusus yang diaktifkan oleh parameter URL.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Langkah inisialisasi berurutan:
	 * <ol>
	 *   <li>Verifikasi sesi dan hak READ; logoff jika tidak valid.</li>
	 *   <li>Menyimpan pengguna saat ini ke field {@code tbmuser}.</li>
	 *   <li>Membaca parameter URL {@code sudah_posting} untuk mode tampilan terfokus.
	 *       Jika ada, menyembunyikan panel filter dan menampilkan rowPosting.</li>
	 *   <li>Membaca parameter URL {@code mulai} dan {@code sampai} untuk menyetel
	 *       rentang tanggal dari parameter URL (format yyyyMMdd).</li>
	 *   <li>Mendaftarkan listener pada searchparent (filter satuan kerja).</li>
	 *   <li>Membuat SatuanKerjaTreeModel untuk navigasi hierarki.</li>
	 *   <li>Mengatur rentang tanggal default: 6 bulan lalu hingga sekarang.</li>
	 *   <li>Mengatur visibilitas tombol sent berdasarkan hak UPDATE.</li>
	 *   <li>Memuat data awal melalui {@link #loadDataDenganProgressPosting}.</li>
	 *   <li>Mendaftarkan listener paging.</li>
	 *   <li>Membuat timer default untuk pembaruan data setelah halaman siap.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dari parsing parameter URL ditangkap
	 * dan ditampilkan ke admin. Jika sesi tidak valid, langsung return.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada parameter URL baru, tambahkan pembacaannya
	 * di sini. Jika ada komponen baru yang perlu diinisialisasi, tambahkan setelah blok
	 * pemeriksaan sesi.</p>
	 *
	 * @param comp komponen root ZK yang telah dirakit
	 * @throws Exception jika terjadi error saat inisialisasi komponen ZK
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
	 * Menangani event klik tombol "Batalkan Posting Semua" untuk membatalkan seluruh
	 * posting kas besar dalam filter yang aktif setelah konfirmasi pengguna.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan cara massal untuk membatalkan semua posting
	 * kas besar yang ada dalam filter aktif, misalnya ketika terjadi kesalahan posting
	 * atau perlu koreksi jurnal secara keseluruhan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menampilkan dialog konfirmasi MyMessageboxConfig.
	 * Jika pengguna mengonfirmasi (OK), mengambil semua KasBesar yang sudah diposting
	 * ({@code postingHistory != null}) dalam filter aktif, kemudian untuk setiap KasBesar:
	 * <ol>
	 *   <li>Menyetel {@code postingHistory} ke null.</li>
	 *   <li>Menyimpan perubahan dengan {@code Common.refreshSaveOrUpdate}.</li>
	 *   <li>Menghapus GrupTransaksi terkait dari database menggunakan SQL native:
	 *       {@code DELETE FROM akunting.grup_transaksi WHERE kas_besar=id AND closing IS NULL}.</li>
	 * </ol>
	 * Setelah selesai, memuat ulang data grid via timer default.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dari operasi database diteruskan ke
	 * framework ZK melalui signature throws di EventListener.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Filter {@code closing IS NULL} pada SQL hapus
	 * penting untuk tidak menghapus entri yang sudah di-closing. Jangan ubah filter ini.</p>
	 *
	 * @param event event ZK dari klik tombol Batalkan Posting Semua
	 * @throws Exception jika terjadi error saat dialog atau operasi database
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi kas besar ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<KasBesar> kasBesars = initCriteria(true).add(Restrictions.isNotNull("postingHistory"))
									.list();

							for (KasBesar kasBesar : kasBesars) {
								kasBesar.setPostingHistory(null);
								Common.refreshSaveOrUpdate(kasBesar);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where kas_besar=" + kasBesar.getId() + " and closing is null")
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
	 * Menangani event klik tombol "Posting Semua" untuk membuka dialog konfirmasi dan
	 * menjalankan proses posting massal semua KasBesar yang belum diposting dalam filter
	 * aktif menggunakan thread latar belakang.
	 *
	 * <p><strong>Tujuan:</strong> Memungkinkan staf akuntansi memposting semua transaksi
	 * kas besar yang belum diposting dalam satu operasi massal, menghemat waktu dibanding
	 * posting satu per satu. Dialog meminta tanggal posting, nama poster (otomatis dari
	 * pengguna aktif), dan keterangan opsional.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Membuka popup dialog konfirmasi dengan field:
	 * tanggal posting (wajib), nama poster (read-only, otomatis), keterangan (opsional,
	 * multi-baris). Setelah pengguna mengklik Simpan dan mengonfirmasi:
	 * <ol>
	 *   <li>Membuat {@link PostingHistory} baru dengan jenis JENIS_PENGGUNAAN_KAS_BESAR
	 *       dan menyimpannya dalam transaksi terpisah menggunakan native session.</li>
	 *   <li>Mengambil semua KasBesar yang belum diposting dalam filter aktif.</li>
	 *   <li>Untuk setiap KasBesar: menentukan akun debet dan kredit berdasarkan jenis
	 *       (dari KasKecil atau langsung), memanggil {@code CommonAkunting.saveTransaksi},
	 *       dan menyetel postingHistory ke KasBesar.</li>
	 *   <li>Label progress diperbarui setiap iterasi dengan persentase kemajuan.</li>
	 *   <li>Setelah semua selesai, menutup session native dan menampilkan pesan sukses.</li>
	 * </ol>
	 * Proses berjalan dalam thread Java terpisah menggunakan {@code new Thread().start()}.
	 * {@code HibernateUtil.currentNativeSession()} digunakan (bukan currentSession())
	 * karena thread ini bukan thread ZK event.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception per KasBesar ditangkap secara diam-diam
	 * (try-catch kosong) agar satu KasBesar yang gagal tidak menghentikan proses keseluruhan.
	 * Exception saat penyimpanan transaksi ditampilkan ke admin.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika logika jurnal KasBesar berubah, perbarui
	 * blok penentuan akunDebets dan akunKredit di dalam loop. Pastikan {@code closeSession()}
	 * dipanggil di akhir thread untuk mencegah kebocoran koneksi database.</p>
	 *
	 * @param event event ZK dari klik tombol Posting Semua
	 * @throws Exception jika terjadi error saat membangun dialog popup
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Transaksi Kas Besar");
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

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi kas besar ?", "Pertanyaan",
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
											MyMessageboxConfig.show("Posting transaksi kas besar berhasil dilakukan",
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
													PostingHistory.JENIS_PENGGUNAAN_KAS_BESAR);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<KasBesar> kasBesars = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (KasBesar kasBesar : kasBesars) {

												SatuanKerja satuanKerja = (SatuanKerja) (kasBesar
														.getSatuanKerja() != null ? kasBesar.getSatuanKerja() : null);

												if (kasBesar != null) {

													try {
														List<Akun> akunDebets = new ArrayList<Akun>();
														List<Double> nilaiDebets = new ArrayList<Double>();

														Akun akunKredit = null;
														if (kasBesar.getKasKecil() != null
																&& kasBesar.getKasKecil().getJenisKasKecil() != null
																&& kasBesar.getKasKecil().getJenisKasKecil()
																		.getAkun() != null
																&& kasBesar.getJenisKasBesar() != null
																&& kasBesar.getJenisKasBesar().getAkun() != null) {

															akunDebets.add(kasBesar.getKasKecil().getJenisKasKecil()
																	.getAkun());

															akunKredit = kasBesar.getJenisKasBesar().getAkun();

														} else {
															if (kasBesar.getJenisKasBesar().getAkunPenerima() != null) {
																akunDebets.add(
																		kasBesar.getJenisKasBesar().getAkunPenerima());
															}
															akunKredit = kasBesar.getJenisKasBesar().getAkun();
														}

														if (!akunDebets.isEmpty() && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {
																ket = "Persetujuan kas besar \"" + kasBesar.getKode()
																		+ "\" pada pengeluaran \"" + kasBesar.getNama()
																		+ "\" senilai " + Common.numberFormat.get()
																				.format(kasBesar.getNilai());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(ket + " ("
																	+ Common.numberFormat.get()
																			.format(rowIndex * 100.0 / kasBesars.size())
																	+ " %)");

															Double nilai = kasBesar.getNilai();
															nilaiDebets.add(nilai);
															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																session.getTransaction().begin();

																if (nilai > 0.1) {
																	CommonAkunting.saveTransaksi(
																			akunDebets.toArray(new Akun[] {}),
																			new Akun[] { akunKredit }, akunDenda,
																			akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			kasBesar.getTanggalPersetujuan(),
																			nilaiDebets.toArray(new Double[] {}),
																			new Double[] { nilai }, denda, kasBesar,
																			satuanKerja, session);
																} else {
																	CommonAkunting.saveTransaksi(
																			new Akun[] { akunKredit },
																			akunDebets.toArray(new Akun[] {}),
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			kasBesar.getTanggalPersetujuan(),
																			new Double[] { nilai },
																			nilaiDebets.toArray(new Double[] {}), denda,
																			kasBesar, satuanKerja, session);
																}

																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															kasBesar.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(kasBesar);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PostingKasBesarAction.java:629");
														// Exception per-baris ditangkap untuk melanjutkan proses berikutnya
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
	 * Kelas inner untuk merender setiap baris data {@link KasBesar} pada grid utama
	 * halaman posting kas besar.
	 *
	 * <p><strong>Tujuan:</strong> Mengubah setiap entitas KasBesar menjadi baris tampilan
	 * grid yang informatif dengan kolom kode, nama+SOP, nilai, tanggal persetujuan,
	 * preview jurnal, status posting, dan tombol aksi posting/batalkan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Metode {@code render} mengisi row dengan:
	 * <ul>
	 *   <li>Kolom kode: Vbox berisi kode revisi KasBesar dan link proses transfer (jika ada).</li>
	 *   <li>Kolom nama: Vbox berisi nama dan link SOP (jika ada).</li>
	 *   <li>Kolom nilai: nilai diformat sebagai angka.</li>
	 *   <li>Kolom tanggal: tanggal persetujuan.</li>
	 *   <li>Kolom jurnal: preview jurnal debet/kredit menggunakan
	 *       {@code GrupTransaksi.tampilkanJurnal}. Jika akun tidak valid, menampilkan
	 *       pesan error deskriptif.</li>
	 *   <li>Kolom status: status posting (Belum diposting / nama poster + no. bukti).</li>
	 *   <li>Kolom aksi: tombol Batalkan Posting (visible jika edit+admin+sudah diposting)
	 *       dan tombol Posting Data (visible jika edit+belum diposting+tbmuser ada),
	 *       hanya ditampilkan jika akun valid.</li>
	 * </ul>
	 * Tombol Posting Data per baris menggunakan managed session (currentSession()) karena
	 * berjalan pada thread ZK event.</p>
	 *
	 * <p><strong>Threading:</strong> Render dipanggil pada thread ZK event; aman untuk
	 * mengakses currentSession().</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada kolom baru di KasBesar, tambahkan di sini
	 * dan sesuaikan dengan deklarasi kolom di file ZUL.</p>
	 */
	class KasBesarRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data {@link KasBesar} ke dalam komponen {@link Row} ZK,
		 * lengkap dengan preview jurnal dan tombol aksi posting.
		 *
		 * <p><strong>Tujuan:</strong> Menampilkan semua informasi relevan KasBesar dalam
		 * satu baris grid yang mudah dibaca, termasuk preview jurnal akuntansi yang akan
		 * terbentuk saat posting, serta tombol untuk melakukan atau membatalkan posting.</p>
		 *
		 * <p><strong>Cara kerja:</strong> Menentukan akun debet dan kredit berdasarkan
		 * jenis KasBesar (dari KasKecil atau langsung). Jika akun valid, menampilkan
		 * jurnal preview dan tombol aksi. Tombol Posting Data saat diklik akan:
		 * <ol>
		 *   <li>Membuat PostingHistory baru dan menyimpannya.</li>
		 *   <li>Memanggil CommonAkunting.saveTransaksi untuk membuat GrupTransaksi.</li>
		 *   <li>Menyetel postingHistory ke KasBesar dan menyimpan perubahan.</li>
		 *   <li>Memuat ulang grid via loadDataDenganProgressPosting.</li>
		 * </ol>
		 * </p>
		 *
		 * <p><strong>Penanganan error:</strong> Exception saat menampilkan label error
		 * jurnal ditangkap dan diganti dengan pesan "Transaksi tidak valid."</p>
		 *
		 * <p><strong>Pemeliharaan:</strong> Logika penentuan akun harus sinkron dengan
		 * logika di {@link #onPostingSemua} agar posting massal dan per-baris konsisten.</p>
		 *
		 * @param arg0 komponen Row ZK yang akan diisi
		 * @param arg1 objek data yang harus berupa instansi KasBesar
		 * @throws Exception jika terjadi error saat render komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final KasBesar kasBesar = (KasBesar) arg1;

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(KasBesar.class, kasBesar,
					kasBesar.getKode() == null ? "" : kasBesar.getKode())).setParent(arg0);
			if (kasBesar != null && kasBesar.getDaftarPengajuanTransfer() != null
					&& kasBesar.getDaftarPengajuanTransfer().getProsesTransfer() != null) {
				A a = new A(kasBesar.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, kasBesar.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);
			}

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(kasBesar.getNama()).setParent(a);
			if (kasBesar.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + kasBesar.getDisposisiSop().getKeterangan()
						+ " (" + kasBesar.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(kasBesar.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			Double nilai = kasBesar.getNilai();

			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

			new Label(Common.dateFormat3.get().format(kasBesar.getTanggalPersetujuan())).setParent(arg0);

			List<Akun> akunDebets = new ArrayList<Akun>();
			List<Akun> akunsKredits = new ArrayList<Akun>();

			if (kasBesar.getKasKecil() != null && kasBesar.getKasKecil().getJenisKasKecil() != null
					&& kasBesar.getKasKecil().getJenisKasKecil().getAkun() != null
					&& kasBesar.getJenisKasBesar() != null && kasBesar.getJenisKasBesar().getAkun() != null) {

				akunDebets.add(kasBesar.getKasKecil().getJenisKasKecil().getAkun());
				List<Double> nilaiDebets = new ArrayList<Double>();
				nilaiDebets.add(nilai);

				List<Double> nilaiKredits = new ArrayList<Double>();
				akunsKredits.add(kasBesar.getJenisKasBesar().getAkun());
				nilaiKredits.add(nilai);

				GrupTransaksi.tampilkanJurnal(akunDebets, nilaiDebets, akunsKredits, nilaiKredits).setParent(arg0);

			} else {

				List<Double> nilaiDebets = new ArrayList<Double>();
				if (kasBesar.getJenisKasBesar() != null && kasBesar.getJenisKasBesar().getAkunPenerima() != null) {
					akunDebets.add(kasBesar.getJenisKasBesar().getAkunPenerima());
					nilaiDebets.add(nilai);
				}

				Akun akunKredit = kasBesar.getJenisKasBesar() == null ? null : kasBesar.getJenisKasBesar().getAkun();

				if (!akunDebets.isEmpty() && akunKredit != null
						&& kasBesar.getJenisKasBesar().getAkunPenerima() != null) {

					List<Double> nilaiKredits = new ArrayList<Double>();
					akunsKredits.add(akunKredit);
					nilaiKredits.add(nilai);

					GrupTransaksi.tampilkanJurnal(akunDebets, nilaiDebets, akunsKredits, nilaiKredits).setParent(arg0);

				} else {
					try {
						new Label("Transaksi tidak valid." + (kasBesar.getJenisKasBesar() == null
								|| kasBesar.getJenisKasBesar().getAkunPenerima() == null
										? " Debet: " + kasBesar.getJenisKasBesar().getAkunPenerima().toString() + "."
										: " Akun debet tidak ada.")
								+ (akunKredit != null
										? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
										: " Akun kredit tidak ada."))
								.setParent(arg0);
					} catch (Exception e) {
						new Label(ais.common.Common.getBahasaConfig("Transaksi tidak valid.")).setParent(arg0);
					}
				}
			}

			String bukti = "";
			Session session = HibernateUtil.currentSession();
			bukti = (String) session.createCriteria(GrupTransaksi.class).add(Restrictions.eq("kasBesar", kasBesar))
					.setMaxResults(1).setProjection(Projections.property("kode")).uniqueResult();

			new Label(kasBesar.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: kasBesar.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (!akunDebets.isEmpty() && !akunsKredits.isEmpty()) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && kasBesar.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								kasBesar.setPostingHistory(null);
								Common.refreshSaveOrUpdate(kasBesar);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where kas_besar=" + kasBesar.getId() + " and closing is null")
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
				button.setVisible(edit && kasBesar.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PENGGUNAAN_KAS_BESAR);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								List<Akun> akunDebets = new ArrayList<Akun>();
								List<Double> nilaiDebets = new ArrayList<Double>();
								Akun akunKredit = null;
								if (kasBesar.getKasKecil() != null && kasBesar.getKasKecil().getJenisKasKecil() != null
										&& kasBesar.getKasKecil().getJenisKasKecil().getAkun() != null
										&& kasBesar.getJenisKasBesar() != null
										&& kasBesar.getJenisKasBesar().getAkun() != null) {

									akunDebets.add(kasBesar.getKasKecil().getJenisKasKecil().getAkun());

									akunKredit = kasBesar.getJenisKasBesar().getAkun();

								} else {
									if (kasBesar.getJenisKasBesar().getAkunPenerima() != null) {
										akunDebets.add(kasBesar.getJenisKasBesar().getAkunPenerima());
									}
									akunKredit = kasBesar.getJenisKasBesar().getAkun();
								}

								if (!akunDebets.isEmpty() && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									Double nilai = kasBesar.getNilai();
									nilaiDebets.add(nilai);
									String ket = "";
									try {

										ket = "Persetujuan kas besar \"" + kasBesar.getKode() + "\" pada pengeluaran \""
												+ kasBesar.getNama() + "\" senilai "
												+ Common.numberFormat.get().format(kasBesar.getNilai());
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (kasBesar.getSatuanKerja() != null
											? kasBesar.getSatuanKerja()
											: tbmuser.ambilSatuanKerja());

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebets.toArray(new Akun[] {}),
												new Akun[] { akunKredit }, akunDenda, akunPiutangDenda, postingHistory,
												apakahUangMasuk, ket, kasBesar.getTanggalPersetujuan(),
												nilaiDebets.toArray(new Double[] {}), new Double[] { nilai }, denda,
												kasBesar, satuanKerja, session);
									} else {
										CommonAkunting.saveTransaksi(new Akun[] { akunKredit },
												akunDebets.toArray(new Akun[] {}), akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket, kasBesar.getTanggalPersetujuan(),
												new Double[] { nilai }, nilaiDebets.toArray(new Double[] {}), denda,
												kasBesar, satuanKerja, session);
									}
								}
								kasBesar.setPostingHistory(postingHistory);
								session.update(kasBesar);

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
	 * Membangun Hibernate {@link Criteria} untuk query {@link KasBesar} berdasarkan filter
	 * yang aktif di halaman atau berdasarkan parameter URL {@code sudah_posting}.
	 *
	 * <p><strong>Tujuan:</strong> Menyatukan logika pembangunan query KasBesar agar
	 * dapat digunakan untuk paging, pengambilan data aktual, maupun operasi massal
	 * (batalkan posting semua, posting semua).</p>
	 *
	 * <p><strong>Cara kerja:</strong> Terdapat dua jalur logika berdasarkan nilai
	 * {@code sudah_posting}:
	 * <ul>
	 *   <li>Jika {@code sudah_posting} tidak null (mode terfokus dari URL): hanya filter
	 *       berdasarkan nilai>0, disetujuiOleh tidak null, rentang tanggal, dan filter
	 *       status posting sesuai nilai boolean sudah_posting.</li>
	 *   <li>Jika {@code sudah_posting} null (mode normal): tambahkan filter satuan kerja
	 *       (dengan hierarki), disetujuiOleh tidak null, nilai>0, rentang tanggal persetujuan,
	 *       dan filter kode/nama/keterangan. Checkbox searchtampil (belum posting) dan
	 *       searchtelahtampil (sudah posting) saling eksklusif.</li>
	 * </ul>
	 * Jika {@code order} true, menambahkan pengurutan berdasarkan ID descending.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dalam blok sudah_posting ditangkap
	 * dan ditampilkan ke admin.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada filter baru, tambahkan di blok yang
	 * sesuai (mode terfokus atau mode normal). Pastikan filter posting status konsisten
	 * antara kedua jalur.</p>
	 *
	 * @param order true untuk menambahkan pengurutan berdasarkan ID desc
	 * @return Criteria Hibernate yang sudah dikonfigurasi sesuai filter aktif
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

		Criteria criteria = session.createCriteria(KasBesar.class);

		if (sudah_posting != null) {

			try {

				criteria.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
						.add(Restrictions.isNotNull("disetujuiOleh"))

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

			criteria.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
					satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(
									parent == null ? Restrictions.isNull("satuanKerja")
											: Restrictions.sqlRestriction("false"),
									Restrictions.in("satuanKerja", satuanKerjas))))

					.add(Restrictions.isNotNull("disetujuiOleh"))

					.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))

					.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
							+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and date('"
							+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

					.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("nama", searchkode.getValue(), MatchMode.ANYWHERE),
											Restrictions.ilike("keterangan", searchkode.getValue(),
													MatchMode.ANYWHERE))));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * Memuat data KasBesar ke grid tanpa tampilan progress bar, langsung dari Criteria.
	 *
	 * <p><strong>Tujuan:</strong> Melakukan pembaruan grid aktual (query + render) sebagai
	 * metode internal yang dipanggil dari dalam {@link #loadDataDenganProgressPosting}
	 * setelah progress bar ditampilkan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menghitung total untuk paging menggunakan criteria
	 * tanpa order, kemudian mengambil data halaman aktif dengan batasan ROWS_COUNT_ON_PAGE
	 * dan offset berdasarkan halaman aktif. Membungkus hasil dalam SimpleListModel dan
	 * memuat ke grid dengan {@link KasBesarRenderer}.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception Hibernate diteruskan ke framework ZK.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Metode ini private; hanya dipanggil oleh
	 * {@link #loadDataDenganProgressPosting} dan tidak boleh dipanggil langsung dari luar.</p>
	 *
	 * @param event event ZK pemicu (dapat null)
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KasBesar> kasBesar = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kasBesar);
		grid.setRowRenderer(new KasBesarRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Handler event pencarian default yang mendelegasikan ke
	 * {@link #loadDataDenganProgressPosting} untuk pembaruan grid dengan progress bar.
	 *
	 * <p><strong>Tujuan:</strong> Menjadi titik masuk terpusat untuk semua pembaruan
	 * grid, memastikan konsistensi penggunaan mekanisme progress bar.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Hanya memanggil
	 * {@link #loadDataDenganProgressPosting(Event)} dengan event yang diterima.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Metode ini mengikuti konvensi ZK event handler.
	 * Jangan melakukan logika langsung di sini; delegasikan ke loadDataDenganProgressPosting.</p>
	 *
	 * @param event event ZK pemicu (dapat null)
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/** Flag yang menandakan apakah proses loading data sedang berlangsung. */
	private boolean postingJurnalLoadingAktif = false;
	/** Flag yang menandakan ada permintaan reload tertunda saat loading berlangsung. */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * Memuat ulang data grid dengan menampilkan progress bar dan mengelola concurrent
	 * reload menggunakan mekanisme flag untuk mencegah race condition.
	 *
	 * <p><strong>Tujuan:</strong> Memberikan feedback visual kepada pengguna saat data
	 * grid sedang dimuat (progress bar dengan pesan status), sekaligus mencegah multiple
	 * reload yang terjadi bersamaan yang dapat menyebabkan inkonsistensi tampilan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menggunakan dua flag boolean:
	 * {@code postingJurnalLoadingAktif} dan {@code postingJurnalReloadTertunda}.
	 * <ol>
	 *   <li>Jika {@code postingJurnalLoadingAktif} true saat metode dipanggil, set
	 *       {@code postingJurnalReloadTertunda = true} dan tampilkan pesan "tertunda",
	 *       kemudian return — tidak ada reload baru dimulai.</li>
	 *   <li>Jika tidak sedang loading, set flag aktif dan jalankan proses loading melalui
	 *       {@code Common.createDefaultTimer}.</li>
	 *   <li>Dalam callback timer, tampilkan pesan progress 48%, panggil
	 *       {@link #onSearchDefaultTanpaProgress}, tampilkan 92%.</li>
	 *   <li>Dalam blok finally, reset flag aktif. Jika {@code postingJurnalReloadTertunda}
	 *       true, jadwalkan reload baru; jika tidak, tampilkan pesan selesai (100%).</li>
	 * </ol>
	 * Progress ditampilkan menggunakan {@code PostingJurnalLoadingUtil} dengan nilai
	 * persentase 7%, 48%, 92%, 96%, 100%.</p>
	 *
	 * <p><strong>Threading:</strong> Semua operasi dilakukan pada thread ZK event melalui
	 * timer; tidak ada thread tambahan di metode ini sendiri.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dari {@link #onSearchDefaultTanpaProgress}
	 * diteruskan; blok finally memastikan flag selalu direset meskipun terjadi exception.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Persentase progress bersifat estimasi visual dan
	 * dapat disesuaikan. Mekanisme flag ini bersifat thread-unsafe secara teori (ZK adalah
	 * single-thread per desktop), tetapi aman dalam praktik ZK.</p>
	 *
	 * @param event event ZK yang memicu reload (dapat null untuk pemanggilan programatik)
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
