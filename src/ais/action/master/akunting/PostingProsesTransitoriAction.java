package ais.action.master.akunting;

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
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.akunting.Transitori;
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
 * <h3>PostingProsesTransitoriAction — Pengendali Halaman Posting Proses Transitori</h3>
 *
 * <p><strong>Untuk apa:</strong>
 * Kelas ini merupakan ZK Composer yang mengelola seluruh proses posting transaksi
 * transitori ke dalam buku besar akuntansi (jurnal umum). Transaksi transitori adalah
 * transaksi keuangan yang berasal dari proses pengajuan transfer yang melewati rekening
 * antara (transitori) sebelum dicairkan ke tujuan akhir. Kelas ini menyediakan
 * antarmuka untuk melihat daftar transitori yang sudah maupun belum diposting, serta
 * memungkinkan operator keuangan melakukan posting satu per satu atau secara massal
 * (batch) ke modul jurnal umum.</p>
 *
 * <p><strong>Cara kerja:</strong>
 * Halaman ZUL yang menggunakan composer ini menampilkan grid berisi data {@link Transitori}
 * yang sudah disetujui (field {@code prosesTransitori.disetujuiOleh} tidak null) dan
 * memiliki nominal tidak nol. Setiap baris menampilkan kode transitori, nama, nominal,
 * tanggal persetujuan, pasangan jurnal debet-kredit, status posting, dan tombol aksi.
 *
 * <ul>
 *   <li>Tombol "Posting Data" (ikon centang) tersedia bila transaksi belum diposting
 *       dan pengguna memiliki hak UPDATE. Posting satu baris dikerjakan langsung dalam
 *       sesi Hibernate yang dikelola ({@code currentSession}).</li>
 *   <li>Tombol "Batalkan Posting" (ikon peringatan) tersedia bila transaksi sudah
 *       diposting dan pengguna adalah admin atau memiliki hak APPROVE. Pembatalan
 *       menghapus {@link PostingHistory} dari entitas dan menghapus entri
 *       {@link GrupTransaksi} terkait dari tabel akunting.</li>
 *   <li>Tombol "Posting Semua" membuka jendela konfirmasi dengan form tanggal dan
 *       keterangan, lalu menjalankan loop posting dalam {@link Thread} latar belakang
 *       menggunakan {@code currentNativeSession()} agar tidak memblokir utas UI ZK.</li>
 *   <li>Tombol "Batalkan Posting Semua" membatalkan seluruh posting sekaligus.</li>
 * </ul>
 *
 * <p><strong>Threading:</strong>
 * Operasi posting massal berjalan dalam utas terpisah ({@code new Thread(...).start()}).
 * Sesi Hibernate yang digunakan di dalam utas ini adalah
 * {@code HibernateUtil.currentNativeSession()} — sesi mentah yang harus dibuka, di-commit,
 * dan ditutup secara manual. Jangan pernah menggunakan {@code currentSession()} (sesi
 * yang dikelola) di dalam utas latar belakang karena sesi tersebut terikat pada konteks
 * ZK/HTTP thread-local dan akan menghasilkan error. Setelah loop selesai, sesi ditutup
 * dengan {@code session.disconnect(); session.close()} dan kemudian
 * {@code HibernateUtil.closeSession()}.</p>
 *
 * <p><strong>Pemeliharaan:</strong>
 * Bila logika penentuan akun debet-kredit transitori berubah (misalnya penambahan jenis
 * transitori baru), perbarui blok dalam {@link TransitoriRenderer#render} dan dalam
 * loop di {@link #onPostingSemua}. Pastikan {@code PostingHistory.PENGAJUAN_TRANSITORI}
 * tetap merupakan konstanta yang valid. Filter tanggal bergantung pada
 * {@link ais.action.master.helper.PostingJurnalHelper} — jika ada perubahan rentang
 * default, perbarui helper tersebut, bukan kelas ini.</p>
 *
 * @author Generated Javadoc
 * @see PostingHistory
 * @see Transitori
 * @see CommonAkunting
 */
public class PostingProsesTransitoriAction extends GenericAutowireComposer {

	/**
	 * ID serialisasi untuk kompatibilitas serialisasi Java.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar transaksi transitori. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman data grid. */
	private Paging paging;

	/** Kotak teks pencarian berdasarkan kode atau nama transaksi. */
	private Textbox searchkode;

	/** Checkbox filter untuk menampilkan hanya transaksi yang belum diposting. */
	private MyCheckboxConfig searchtampil;

	/** Checkbox filter untuk menampilkan hanya transaksi yang sudah diposting. */
	private MyCheckboxConfig searchtelahtampil;

	/** Flag apakah pengguna saat ini memiliki hak ubah (UPDATE). */
	private boolean edit = false;

	/** Tombol toolbar yang mengendalikan visibilitas aksi kirim/posting. */
	private MyToolbarbuttonConfig sent;

	/**
	 * Flag apakah pengguna adalah admin atau memiliki hak APPROVE,
	 * digunakan untuk menentukan tampilan tombol batalkan posting.
	 */
	public boolean adminLain;

	/** Tanggal awal rentang filter data transitori. */
	private MyDatebox tglMulai;

	/** Tanggal akhir rentang filter data transitori. */
	private MyDatebox tglSampai;

	/** Data pengguna yang sedang login, diambil saat inisialisasi halaman. */
	private Tbmuser tbmuser;

	/**
	 * <h3>doBeforeCompose — Pemeriksaan Keamanan Sebelum Halaman Dibangun</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Metode ini dipanggil oleh kerangka ZK sebelum komponen-komponen halaman ZUL
	 * diinisialisasi dan di-wire ke composer. Tujuan utamanya adalah melakukan
	 * pemeriksaan keamanan (security check) untuk memastikan bahwa pengguna yang
	 * mengakses halaman ini telah login dan memiliki otorisasi yang sesuai.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Metode ini memanggil {@link Common#doCheckSecurity()} yang akan memeriksa sesi
	 * pengguna saat ini. Jika pengguna tidak terautentikasi atau tidak memiliki akses
	 * ke modul yang relevan, metode tersebut akan melakukan redirect ke halaman login
	 * atau menampilkan pesan kesalahan sebelum halaman sempat dirender. Setelah
	 * pemeriksaan selesai, metode memanggil implementasi super untuk melanjutkan
	 * proses komposisi halaman secara normal.</p>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Jika keamanan gagal, {@code Common.doCheckSecurity()} akan menangani redirect
	 * secara internal. Tidak ada penanganan exception tambahan di sini.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Jangan menghapus pemanggilan {@code Common.doCheckSecurity()} dari metode ini.
	 * Semua halaman sensitif harus melalui pemeriksaan ini sebelum halaman dirender.</p>
	 *
	 * @param page     halaman ZK yang sedang dikomposisi
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo metadata informasi komponen dari ZUL
	 * @return informasi komponen yang dikembalikan oleh implementasi super
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
	 * Metode ini dipanggil oleh kerangka ZK setelah seluruh komponen pada halaman ZUL
	 * selesai diinisialisasi dan di-wire ke field-field di kelas ini (auto-wire).
	 * Metode ini bertanggung jawab atas inisialisasi penuh halaman: pemeriksaan sesi,
	 * pengaturan rentang tanggal default, penentuan hak akses pengguna, pemuatan data
	 * awal, serta registrasi listener paging dan timer auto-refresh.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan auto-wire.</li>
	 *   <li>Menginisialisasi bahasa antarmuka via {@code Common.initLaguage()}.</li>
	 *   <li>Memeriksa apakah atribut sesi {@code "usersTemp"} tersedia dan pengguna
	 *       memiliki hak READ. Jika tidak, halaman di-redirect ke halaman logoff.</li>
	 *   <li>Mengambil data pengguna aktif dan menyimpannya di field {@code tbmuser}.</li>
	 *   <li>Mengatur rentang tanggal default: tanggal mulai 6 bulan ke belakang dari
	 *       tanggal hari ini, tanggal sampai adalah hari ini. Komponen datebox dibuat
	 *       read-only agar pengguna menggunakan filter melalui tombol khusus.</li>
	 *   <li>Menentukan flag {@code adminLain} berdasarkan apakah pengguna adalah
	 *       administrator atau memiliki hak APPROVE.</li>
	 *   <li>Menentukan flag {@code edit} berdasarkan hak UPDATE dan mengatur visibilitas
	 *       tombol {@code sent}.</li>
	 *   <li>Membaca parameter status posting dari dasbor via
	 *       {@code PostingJurnalHelper.ambilParameterSudahPosting()} dan menerapkan
	 *       parameter tanggal dari dasbor bila ada.</li>
	 *   <li>Memanggil {@link #loadDataDenganProgressPosting(Event)} untuk memuat data
	 *       pertama kali.</li>
	 *   <li>Mendaftarkan listener paging sehingga navigasi halaman memuat ulang data.</li>
	 *   <li>Mendaftarkan timer default ZK untuk auto-refresh berkala.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Jika sesi tidak valid, pengguna diarahkan ke halaman logoff dan metode berhenti
	 * lebih awal dengan {@code return}. Exception dari {@code super.doAfterCompose}
	 * dibiarkan naik ke atas (throws Exception).</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Jika ada field baru yang perlu diinisialisasi saat halaman dibuka, tambahkan di
	 * sini setelah blok pemeriksaan sesi. Jangan mengubah urutan inisialisasi karena
	 * beberapa langkah bergantung pada langkah sebelumnya.</p>
	 *
	 * @param comp komponen akar hasil komposisi ZUL
	 * @throws Exception jika terjadi kesalahan selama inisialisasi komponen
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

		sudahPostingDasbor = ais.action.master.helper.PostingJurnalHelper.ambilParameterSudahPosting();
		ais.action.master.helper.PostingJurnalHelper.terapkanParameterTanggal(tglMulai, tglSampai);

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
	 * <h3>onBatalkanPostingSemua — Pembatalan Massal Seluruh Posting Transitori</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Metode ini menangani klik tombol "Batalkan Posting Semua" pada toolbar halaman.
	 * Tujuannya adalah membatalkan seluruh posting transaksi transitori yang sesuai
	 * dengan filter aktif saat ini (rentang tanggal, status, dan kata kunci pencarian),
	 * sehingga semua transaksi tersebut kembali ke status "belum diposting" dan
	 * entri jurnal terkait dihapus dari tabel {@code akunting.grup_transaksi}.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Pertama, ditampilkan dialog konfirmasi kepada pengguna. Jika pengguna menekan
	 * "OK", sistem mengambil semua entitas {@link Transitori} yang memenuhi kriteria
	 * filter aktif dan memiliki {@code postingHistory} tidak null (sudah diposting).
	 * Untuk setiap entitas tersebut:
	 * <ol>
	 *   <li>Field {@code postingHistory} disetel ke {@code null}.</li>
	 *   <li>Entitas diperbarui di basis data via {@code Common.refreshSaveOrUpdate}.</li>
	 *   <li>Semua baris {@link GrupTransaksi} yang merujuk ke transitori ini
	 *       (kolom {@code transitori}) dan bukan merupakan closing entry dihapus
	 *       langsung via SQL native.</li>
	 * </ol>
	 * Setelah loop selesai, timer default dipicu untuk memuat ulang grid.</p>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Jika terjadi kegagalan pada salah satu iterasi, tidak ada mekanisme rollback
	 * otomatis — setiap operasi dikerjakan dalam sesi yang dikelola. Bila diperlukan
	 * atomisitas, pertimbangkan untuk membungkus seluruh loop dalam satu transaksi.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Pastikan query SQL native menggunakan skema yang benar ({@code akunting.grup_transaksi}).
	 * Jika nama kolom berubah di migrasi basis data, perbarui string SQL di sini.</p>
	 *
	 * @param event event ZK yang dipicu saat tombol diklik
	 * @throws Exception jika terjadi kesalahan akses basis data atau komponen UI
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi transitori ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<Transitori> transitoris = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (Transitori transitori : transitoris) {
								transitori.setPostingHistory(null);
								Common.refreshSaveOrUpdate(transitori);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.transaksi where grup_transaksi in"
												+ " (select id from akunting.grup_transaksi where transitori=" + transitori.getId()
												+ " and closing is null)")
										.executeUpdate();
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where transitori=" + transitori.getId() + " and closing is null")
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
	 * <h3>onPostingSemua — Pembukaan Dialog dan Eksekusi Posting Massal Transitori</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Metode ini menangani klik tombol "Posting Semua" dan bertanggung jawab atas
	 * keseluruhan alur posting massal: membuka jendela konfirmasi dengan form input
	 * (tanggal dan keterangan), memvalidasi input, meminta konfirmasi ulang, lalu
	 * menjalankan proses posting dalam utas latar belakang agar UI tidak membeku
	 * selama proses berlangsung.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Membuat dan menampilkan {@link MyWindow} modal berisi form: tanggal posting,
	 *       nama pengguna yang melakukan posting, dan keterangan opsional.</li>
	 *   <li>Tombol "Batal" menutup jendela tanpa melakukan apa pun.</li>
	 *   <li>Tombol "Simpan": memvalidasi bahwa tanggal tidak kosong, lalu memunculkan
	 *       dialog konfirmasi kedua.</li>
	 *   <li>Jika pengguna mengkonfirmasi, sistem membuat {@link Label} progress via
	 *       {@code Common.displayLoadBar} dan memulai {@link Thread} baru.</li>
	 *   <li>Di dalam utas baru, menggunakan {@code HibernateUtil.currentNativeSession()}
	 *       untuk membuat dan menyimpan satu {@link PostingHistory} baru dengan jenis
	 *       {@code PostingHistory.PENGAJUAN_TRANSITORI}.</li>
	 *   <li>Mengambil semua ID transitori yang belum diposting via kriteria Hibernate
	 *       dengan proyeksi ID (untuk efisiensi memori pada dataset besar).</li>
	 *   <li>Untuk setiap ID, memuat entitas {@link Transitori} secara fresh dari
	 *       sesi native, memastikan {@link DaftarPengajuanTransfer} memiliki referensi
	 *       balik ke transitori, kemudian menentukan akun debet dan kredit.</li>
	 *   <li>Jika nominal positif (lebih dari 0.1), akun debet adalah akun pengajuan
	 *       transfer dan akun kredit adalah akun transitori dari cara pembayaran.
	 *       Jika nominal negatif, posisi debet-kredit dibalik.</li>
	 *   <li>Memanggil {@link CommonAkunting#saveTransaksi} untuk membuat
	 *       {@link GrupTransaksi} dan menyimpan jurnal ke basis data.</li>
	 *   <li>Memperbarui field {@code postingHistory} pada entitas {@link Transitori}.</li>
	 *   <li>Label progress diperbarui setelah setiap iterasi untuk menampilkan
	 *       persentase penyelesaian.</li>
	 *   <li>Setelah loop selesai, sesi native ditutup dan label progress dikosongkan.</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong>
	 * Seluruh loop berjalan di utas non-ZK. Komunikasi ke UI dilakukan hanya melalui
	 * {@code label.setValue()} yang diizinkan oleh ZK dalam push mode. Jangan memanggil
	 * komponen ZK lain dari dalam utas ini.</p>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Setiap iterasi dibungkus dalam blok {@code try-catch}. Kesalahan pada satu
	 * transitori tidak menghentikan proses keseluruhan. Kesalahan ditampilkan hanya
	 * kepada pengguna admin via {@code Common.tampilErrorJikaAdmin}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Pastikan konstanta {@code PostingHistory.PENGAJUAN_TRANSITORI} masih valid.
	 * Jika logika penentuan akun berubah, perbarui blok pengambilan {@code akunDebet}
	 * dan {@code akunKredit} di dalam loop.</p>
	 *
	 * @param event event ZK yang dipicu saat tombol diklik
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

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi transitori ?", "Pertanyaan",
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
											MyMessageboxConfig.show("Posting transaksi transitori berhasil dilakukan",
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

									final SatuanKerja satuanKerja = Common.getSatuanKerja();

									new Thread(new Runnable() {

										@Override
										public void run() {
											try {

											List<Long> transitoris = initCriteria(true)
													.add(Restrictions.isNull("postingHistory"))
													.setProjection(Projections.property("id")).list();

											Session session = HibernateUtil.currentNativeSession();

											PostingHistory postingHistory = new PostingHistory(
													PostingHistory.PENGAJUAN_TRANSITORI);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											int rowIndex = 1;
											for (Long transitoriid : transitoris) {

												try {
													Transitori transitori = (Transitori) session
															.createCriteria(Transitori.class)
															.add(Restrictions.idEq(transitoriid)).uniqueResult();
													if (transitori != null) {

														DaftarPengajuanTransfer daftarPengajuanTransfer = transitori
																.getDaftarPengajuanTransfer();
														if (daftarPengajuanTransfer != null && daftarPengajuanTransfer
																.getTransitoriData() == null) {
															session.refresh(daftarPengajuanTransfer);
															daftarPengajuanTransfer.setTransitoriData(transitori);
															session.getTransaction().begin();
															Common.refreshUpdate(session, daftarPengajuanTransfer);
															session.getTransaction().commit();
														}

														Date tgl = transitori.getProsesTransitori()
																.getTanggalPersetujuan();
														Akun akunDebet = transitori.getDaftarPengajuanTransfer()
																.getAkun();
														Akun akunKredit = transitori.getDaftarPengajuanTransfer()
																.getProsesTransfer().getCaraPembayaranTransfer()
																.getAkunTransitori();

														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															Double nominal = transitori.getDaftarPengajuanTransfer()
																	.getNominal();

															String ket = "";
															try {

																ket = "Pengajuan transitori \"" + transitori.getNama()
																		+ "\" senilai "
																		+ Common.numberFormat.get().format(
																				transitori.getDaftarPengajuanTransfer()
																						.getNominal());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}
															label.setValue(ket + " ("
																	+ Common.numberFormat.get().format(
																			rowIndex * 100.0 / transitoris.size())
																	+ " %)");

															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																session.getTransaction().begin();

																if (nominal > 0.1) {
																	CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket, tgl, nominal, denda,
																			transitori, satuanKerja, session);
																} else {
																	CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket, tgl, nominal, denda,
																			transitori, satuanKerja, session);
																}

																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															transitori.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(transitori);
															session.getTransaction().commit();
														}
													}
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);

												}
												rowIndex++;
											}
											// session.disconnect();
											if (session.isOpen()) {session.disconnect();session.close();}
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
	 * <h3>TransitoriRenderer — Renderer Baris Grid Transaksi Transitori</h3>
	 *
	 * <p><strong>Untuk apa:</strong>
	 * Kelas dalam (inner class) ini bertanggung jawab merender setiap baris pada
	 * grid utama halaman posting transitori. Setiap baris merepresentasikan satu
	 * entitas {@link Transitori} dan menampilkan informasi lengkap beserta tombol
	 * aksi yang relevan dengan status posting dan hak akses pengguna.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Kelas ini memperluas {@code ais.ui.util.MyRowRenderer} dan mengimplementasikan
	 * metode {@link #render(Row, Object)} yang dipanggil oleh ZK untuk setiap baris
	 * dalam model list grid. Renderer ini menghasilkan tampilan per-baris berisi:
	 * kode transitori (dengan link ke proses transfer), nama transitori, nominal,
	 * tanggal, pasangan jurnal debet-kredit, status posting (sudah/belum beserta
	 * nomor bukti), dan toolbar aksi.</p>
	 *
	 * <p><strong>Threading:</strong>
	 * Renderer dijalankan dalam utas ZK event yang sama, sehingga aman menggunakan
	 * {@code HibernateUtil.currentSession()}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Jika kolom grid pada ZUL bertambah atau berubah urutan, sesuaikan urutan
	 * pemanggilan {@code setParent(arg0)} di metode render ini agar tetap sinkron
	 * dengan definisi {@code <columns>} di file ZUL.</p>
	 */
	class TransitoriRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render — Mengisi Satu Baris Grid dengan Data Transitori</h3>
		 *
		 * <p><strong>Tujuan:</strong>
		 * Metode ini dipanggil oleh kerangka ZK untuk setiap elemen dalam model list
		 * grid. Tujuannya adalah mengisi baris ({@link Row}) dengan komponen-komponen
		 * ZK yang menampilkan data satu transaksi transitori secara lengkap, termasuk
		 * status posting dan tombol-tombol aksi yang sesuai dengan hak akses pengguna.</p>
		 *
		 * <p><strong>Cara kerja:</strong>
		 * <ol>
		 *   <li>Menyetel vertical-align baris ke "top".</li>
		 *   <li>Melakukan cast {@code arg1} ke {@link Transitori}.</li>
		 *   <li>Memeriksa dan memperbaiki referensi balik {@code transitoriData} pada
		 *       {@link DaftarPengajuanTransfer} jika belum diset.</li>
		 *   <li>Menampilkan kode transitori dalam {@link Vbox} dengan link ke form
		 *       proses transfer (via {@code ProsesTransferAction.onAddExternal}).</li>
		 *   <li>Menampilkan nama transitori sebagai {@link Label}.</li>
		 *   <li>Mengambil tanggal persetujuan, akun debet, akun kredit, dan nominal.</li>
		 *   <li>Menampilkan nominal dalam format angka dan tanggal dalam format singkat.</li>
		 *   <li>Jika akun debet dan kredit keduanya ada, menampilkan visualisasi jurnal
		 *       via {@code GrupTransaksi.tampilkanJurnal()}. Jika tidak, menampilkan pesan
		 *       kesalahan yang menjelaskan akun mana yang tidak ditemukan.</li>
		 *   <li>Mencari nomor bukti {@link GrupTransaksi} terkait transitori ini untuk
		 *       ditampilkan bersama status posting.</li>
		 *   <li>Menampilkan status posting: "Belum diposting" atau informasi posting
		 *       beserta nomor bukti.</li>
		 *   <li>Membuat {@link Hbox} toolbar berisi tombol aksi:
		 *       <ul>
		 *         <li>Tombol "Batalkan Posting" — hanya tampil jika sudah diposting
		 *             dan pengguna adalah admin/APPROVE.</li>
		 *         <li>Tombol "Posting Data" — hanya tampil jika belum diposting
		 *             dan pengguna memiliki hak UPDATE.</li>
		 *       </ul>
		 *   </li>
		 * </ol>
		 *
		 * <p><strong>Penanganan error:</strong>
		 * Jika akun tidak lengkap, tombol posting tidak ditampilkan sama sekali
		 * (blok {@code if (akunKredit != null && akunKredit != null)}).
		 * Keterangan kesalahan ditampilkan di kolom jurnal.</p>
		 *
		 * <p><strong>Pemeliharaan:</strong>
		 * Pastikan query pencarian nomor bukti menggunakan alias kolom yang benar
		 * ({@code "transitori"} bukan nama lain) sesuai mapping Hibernate
		 * {@link GrupTransaksi}.</p>
		 *
		 * @param arg0 baris ZK yang akan diisi komponen
		 * @param arg1 objek data, diharapkan bertipe {@link Transitori}
		 * @throws Exception jika terjadi kesalahan akses basis data atau pembuatan komponen
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Transitori transitori = (Transitori) arg1;

			DaftarPengajuanTransfer daftarPengajuanTransfer = transitori.getDaftarPengajuanTransfer();
			if (daftarPengajuanTransfer != null && daftarPengajuanTransfer.getTransitoriData() == null) {
				daftarPengajuanTransfer.setTransitoriData(transitori);
				Common.refreshUpdate(daftarPengajuanTransfer);
			}

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(Transitori.class, transitori,
					transitori.getKode() == null ? "" : transitori.getKode())).setParent(arg0);
			if (transitori.getDaftarPengajuanTransfer() != null
					&& transitori.getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A a = new A(transitori.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, transitori.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);
			}

			new Label(transitori.getNama()).setParent(arg0);

			Date tgl = transitori.getProsesTransitori().getTanggalPersetujuan();

			Akun akunDebet = transitori.getDaftarPengajuanTransfer().getAkun();
			Akun akunKredit = transitori.getDaftarPengajuanTransfer().getProsesTransfer().getCaraPembayaranTransfer()
					.getAkunTransitori();
			Double nilai = transitori.getDaftarPengajuanTransfer().getNominal();
			new Label(Common.numberFormat.get().format(transitori.getDaftarPengajuanTransfer().getNominal())).setParent(arg0);

			new Label(Common.dateFormat3.get().format(tgl)).setParent(arg0);

			if (akunDebet != null && akunKredit != null) {

				GrupTransaksi.tampilkanJurnal(akunDebet, nilai, akunKredit, nilai).setParent(arg0);

			} else {
				new Label("Transaksi tidak valid."
						+ (akunDebet != null ? " Debet: " + akunDebet.toString() + "." : " Akun debet tidak ada.")
						+ (akunKredit != null ? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
								: " Akun kredit tidak ada."))
						.setParent(arg0);
			}

			String bukti = "";
			Session session = HibernateUtil.currentSession();
			bukti = (String) session.createCriteria(GrupTransaksi.class).add(Restrictions.eq("transitori", transitori))
					.setMaxResults(1).setProjection(Projections.property("kode")).uniqueResult();

			new Label(transitori.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: transitori.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunKredit != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && transitori.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								transitori.setPostingHistory(null);
								Common.refreshSaveOrUpdate(transitori);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.transaksi where grup_transaksi in"
												+ " (select id from akunting.grup_transaksi where transitori=" + transitori.getId()
												+ " and closing is null)")
										.executeUpdate();
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where transitori=" + transitori.getId() + " and closing is null")
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
				button.setVisible(edit && transitori.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(PostingHistory.PENGAJUAN_TRANSITORI);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);
								SatuanKerja satuanKerja = Common.getSatuanKerja();

								Date tgl = transitori.getProsesTransitori().getTanggalPersetujuan();
								Akun akunDebet = transitori.getDaftarPengajuanTransfer().getAkun();
								Akun akunKredit = transitori.getDaftarPengajuanTransfer().getProsesTransfer()
										.getCaraPembayaranTransfer().getAkunTransitori();

								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									Double nominal = transitori.getDaftarPengajuanTransfer().getNominal();

									String ket = "";
									try {

										ket = "Pengajuan transitori \"" + transitori.getNama() + "\" senilai "
												+ Common.numberFormat.get()
														.format(transitori.getDaftarPengajuanTransfer().getNominal());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									try {

										Akun akunDenda = null;
										Akun akunPiutangDenda = null;
										Double denda = 0.0;

										if (nominal > 0.1) {
											CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda,
													akunPiutangDenda, postingHistory, apakahUangMasuk, ket, tgl,
													nominal, denda, transitori, satuanKerja, session);
										} else {
											CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda,
													akunPiutangDenda, postingHistory, apakahUangMasuk, ket, tgl,
													nominal, denda, transitori, satuanKerja, session);
										}
										session.flush();
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									transitori.setPostingHistory(postingHistory);
									session.update(transitori);
									session.flush();
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
	 * <h3>initCriteria — Membangun Kriteria Query Hibernate untuk Data Transitori</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Metode ini membangun dan mengembalikan objek {@link Criteria} Hibernate yang
	 * mencerminkan seluruh kondisi filter yang aktif di halaman, termasuk filter
	 * tanggal, status posting, kata kunci pencarian, dan kondisi bisnis yang wajib
	 * dipenuhi (sudah disetujui, nominal tidak nol, bertipe transfer).</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Metode ini mengambil sesi Hibernate yang dikelola via {@code currentSession()}
	 * dan membangun kriteria bertahap:
	 * <ol>
	 *   <li>Batasan status posting dari parameter dasbor
	 *       ({@code PostingJurnalHelper.restriksiPosting}).</li>
	 *   <li>Join ke {@code prosesTransitori} dan filter {@code disetujuiOleh} tidak null
	 *       untuk memastikan hanya transitori yang sudah disetujui ditampilkan.</li>
	 *   <li>Left join ke {@code daftarPengajuanTransfer} dan {@code prosesTransfer}.</li>
	 *   <li>Filter checkbox "belum diposting" ({@code searchtampil}) dan
	 *       "sudah diposting" ({@code searchtelahtampil}).</li>
	 *   <li>Memastikan nominal tidak nol dan tidak null.</li>
	 *   <li>Filter rentang tanggal berdasarkan {@code prosesTransitori.tanggalPersetujuan}.</li>
	 *   <li>Filter wajib {@code transfer = true} (hanya transitori jenis transfer).</li>
	 *   <li>Filter kata kunci yang mencari di kode proses transfer, kode pengajuan,
	 *       nama, dan keterangan menggunakan ILIKE (tidak sensitif huruf besar/kecil).</li>
	 *   <li>Jika parameter {@code order} true, menambahkan urutan descending berdasarkan
	 *       ID prosesTransitori dan ID transitori.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Tidak ada penanganan exception eksplisit. Kesalahan query Hibernate akan dilempar
	 * sebagai {@code HibernateException} ke pemanggil.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Jika ada penambahan filter baru di UI (misalnya filter satuan kerja), tambahkan
	 * batasan baru di sini. Pastikan alias yang digunakan konsisten dengan nama
	 * properti pada entitas Hibernate.</p>
	 *
	 * @param order apakah kriteria harus disertai klausa ORDER BY (true untuk tampilan
	 *              grid, false untuk penghitungan total paging)
	 * @return objek {@link Criteria} yang siap dieksekusi dengan {@code .list()} atau
	 *         {@code .setMaxResults().setFirstResult().list()}
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Transitori.class).add(ais.action.master.helper.PostingJurnalHelper.restriksiPosting("postingHistory", sudahPostingDasbor)).createAlias("prosesTransitori", "prosesTransitori")

				.add(Restrictions.isNotNull("prosesTransitori.disetujuiOleh"))

				.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer", Criteria.LEFT_JOIN)
				.createAlias("daftarPengajuanTransfer.prosesTransfer", "prosesTransfer", Criteria.LEFT_JOIN)

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(!searchtelahtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNotNull("postingHistory"))

				.add(Restrictions.ne("daftarPengajuanTransfer.nominal", 0.0))
				.add(Restrictions.isNotNull("daftarPengajuanTransfer.nominal"))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1")
						: (Restrictions
						.between("prosesTransitori.tanggalPersetujuan", tglMulai.getValue(), tglSampai.getValue())))




				.add(Restrictions.eq("transfer", true))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("prosesTransfer.kode", searchkode.getValue(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("daftarPengajuanTransfer.kode", searchkode.getValue(),
												MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("nama", searchkode.getValue(), MatchMode.ANYWHERE),
												Restrictions.ilike("keterangan", searchkode.getValue(),
														MatchMode.ANYWHERE)))));

		if (order)
			criteria.addOrder(Order.desc("prosesTransitori.id")).addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <h3>onSearchDefaultTanpaProgress — Pemuatan Data Grid Tanpa Indikator Progres</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Metode privat ini melakukan pemuatan data aktual ke dalam grid: menghitung total
	 * baris untuk paging, mengambil data sesuai halaman aktif, dan mengatur model
	 * serta renderer grid. Metode ini dipanggil dari dalam
	 * {@link #loadDataDenganProgressPosting} setelah indikator progres ditampilkan.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * <ol>
	 *   <li>Memanggil {@code Common.initPaging(initCriteria(false), paging)} untuk
	 *       menghitung dan mengatur jumlah total halaman berdasarkan kriteria tanpa
	 *       klausa ORDER (lebih efisien untuk COUNT).</li>
	 *   <li>Memanggil {@code initCriteria(true)} dengan {@code setMaxResults} dan
	 *       {@code setFirstResult} untuk mengambil hanya baris pada halaman aktif.</li>
	 *   <li>Membungkus hasil list dalam {@link SimpleListModel}.</li>
	 *   <li>Menyetel renderer baru ({@link TransitoriRenderer}) dan model ke grid.</li>
	 * </ol>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Konstanta {@code Common.ROWS_COUNT_ON_PAGE} menentukan jumlah baris per halaman.
	 * Jangan memanggil metode ini langsung dari luar — selalu gunakan
	 * {@link #loadDataDenganProgressPosting} agar indikator progres berjalan dengan benar.</p>
	 *
	 * @param event event ZK yang menjadi pemicu (dapat null)
	 */
//	waktuTransfer
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Transitori> transitori = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(transitori);
		grid.setRowRenderer(new TransitoriRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <h3>onSearchDefault — Titik Masuk Publik Pencarian Data</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Metode publik ini adalah titik masuk standar yang dipanggil oleh ZK melalui
	 * konvensi penamaan event (misalnya dari event {@code onSearchDefault} di ZUL
	 * atau dari listener paging). Metode ini mendelegasikan seluruh pekerjaan ke
	 * {@link #loadDataDenganProgressPosting} agar indikator progres selalu aktif
	 * saat data dimuat.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Hanya meneruskan event ke {@code loadDataDenganProgressPosting(event)}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Jangan menambahkan logika langsung di sini. Semua logika pemuatan data harus
	 * ada di {@link #loadDataDenganProgressPosting} atau {@link #onSearchDefaultTanpaProgress}.</p>
	 *
	 * @param event event ZK yang memicu pencarian (dapat null)
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Status posting yang diterima dari parameter dasbor draft jurnal.
	 * Nilai null berarti halaman dibuka langsung dari menu (tanpa parameter dasbor).
	 */
	private Boolean sudahPostingDasbor = null;

	/**
	 * Flag pengaman untuk mencegah dua permintaan load berjalan bersamaan.
	 * True berarti proses loading sedang berjalan.
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag yang menandai bahwa ada permintaan reload tertunda karena proses
	 * loading sebelumnya masih berjalan.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * <h3>loadDataDenganProgressPosting — Pemuatan Data dengan Indikator Progres Bertahap</h3>
	 *
	 * <p><strong>Tujuan:</strong>
	 * Metode privat ini adalah orkestrasi utama pemuatan data grid posting transitori.
	 * Tujuannya adalah menampilkan indikator progres bertahap kepada pengguna selama
	 * data sedang dimuat, sekaligus mencegah dua proses pemuatan berjalan bersamaan
	 * (race condition) yang dapat menyebabkan tampilan tidak konsisten.</p>
	 *
	 * <p><strong>Cara kerja:</strong>
	 * Metode ini menggunakan pola "loading flag dengan antrian tunggal":
	 * <ol>
	 *   <li>Jika flag {@code postingJurnalLoadingAktif} sudah true (sedang loading),
	 *       set {@code postingJurnalReloadTertunda = true} dan tampilkan notifikasi
	 *       progres "permintaan tertunda", lalu langsung return tanpa memulai proses
	 *       baru.</li>
	 *   <li>Jika belum loading, set flag aktif, reset flag tertunda, tampilkan
	 *       progres awal (7%), lalu jadwalkan eksekusi via {@code Common.createDefaultTimer}
	 *       agar UI sempat merender indikator progres.</li>
	 *   <li>Di dalam callback timer: tampilkan progres 48% (mengambil data), panggil
	 *       {@link #onSearchDefaultTanpaProgress} untuk query dan render grid, tampilkan
	 *       progres 92% (merapikan tampilan).</li>
	 *   <li>Blok {@code finally} selalu membersihkan flag. Jika ada reload tertunda
	 *       ({@code postingJurnalReloadTertunda = true}), jadwalkan pemanggilan
	 *       rekursif via timer baru. Jika tidak ada, tandai progres selesai (100%).</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong>
	 * Seluruh metode ini berjalan di utas ZK (event thread). Penggunaan timer
	 * ({@code createDefaultTimer}) adalah cara ZK untuk "melepas" event thread sejenak
	 * agar browser dapat merender pembaruan UI sebelum query berat dijalankan.</p>
	 *
	 * <p><strong>Penanganan error:</strong>
	 * Blok {@code finally} memastikan flag selalu dibersihkan meskipun terjadi exception
	 * di dalam blok try. Ini mencegah sistem "terkunci" dalam state loading selamanya.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong>
	 * Persentase progres (7, 48, 92, 96, 100) bersifat informatif dan dapat disesuaikan.
	 * Pastikan {@code PostingJurnalLoadingUtil} tersedia dan dapat diakses dari halaman.</p>
	 *
	 * @param event event ZK yang menjadi pemicu (dapat null bila dipanggil secara internal)
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

	// =====================================================================
	// JALUR NON-ZK (dasbor Draft Jurnal lewat API POS)
	//
	// Kembaran non-ZK dari tombol "Posting Semua" dan "Batalkan Posting Semua" di
	// halaman ini: tanpa dialog, tanpa label progress, tanpa thread sendiri, dan
	// rentang tanggalnya datang sebagai parameter -- bukan dari datebox layar.
	// Pola sama dgn PostingKasKecilAction/PostingKasBesarAction/PostingProsesTransferAction.
	//
	// PEMELIHARAAN: logika per-dokumen di sini HARUS tetap identik dengan
	// {@link #onPostingSemua}. Bila penentuan akun debet/kredit transitori berubah,
	// ubah di KEDUA tempat.
	// =====================================================================

	/**
	 * Kriteria transitori yang layak diposting pada rentang tanggal, tanpa bergantung pada
	 * komponen layar. Sama dengan bagian {@link #initCriteria(boolean)} yang tidak berhubungan
	 * dgn kotak pencarian, dan sama pula dgn kriteria baris "Transitori" pada dasbor Draft
	 * Jurnal -- termasuk saringan {@code transfer = true} yang membatasi transitori ke jenis
	 * yang memang berpasangan dgn pengajuan transfer.
	 */
	private static Criteria kriteriaPostingStatic(Session session, java.util.Date mulai, java.util.Date sampai) {
		Criteria c = session.createCriteria(Transitori.class)
				.createAlias("prosesTransitori", "prosesTransitori")
				.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer", Criteria.LEFT_JOIN)
				.add(Restrictions.isNotNull("prosesTransitori.disetujuiOleh"))
				.add(Restrictions.ne("daftarPengajuanTransfer.nominal", 0.0))
				.add(Restrictions.isNotNull("daftarPengajuanTransfer.nominal"))
				.add(Restrictions.eq("transfer", true));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.between("prosesTransitori.tanggalPersetujuan", mulai, sampai));
		}
		return c;
	}

	/**
	 * Batalkan posting SEMUA transitori terposting dalam rentang.
	 *
	 * <p>Dijalankan pada {@code currentNativeSession()} dengan transaksi eksplisit per dokumen.
	 * Memakai {@code currentSession()} seperti tombol di layar hanya berhasil di dalam permintaan
	 * ZK, yang kerangkanya meng-commit sesi berjalan; dipanggil dari API perubahannya tidak
	 * pernah tersimpan sehingga pembatalan melaporkan sukses padahal jurnal dan penanda
	 * postingnya masih utuh.</p>
	 *
	 * <p>Baris {@code akunting.transaksi} dihapus lebih dulu karena {@code grup_transaksi}
	 * adalah induknya; jurnal yang SUDAH closing tidak ikut dihapus.</p>
	 */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<Transitori> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (Transitori transitori : daftar) {
				try {
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where transitori=" + transitori.getId()
							+ " and closing is null)").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where transitori="
							+ transitori.getId() + " and closing is null").executeUpdate();
					transitori.setPostingHistory(null);
					session.update(transitori);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingProsesTransitoriAction jalur API");
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
	 * Posting SEMUA transitori yang belum diposting dalam rentang. Logika per-dokumen IDENTIK
	 * dengan {@link #onPostingSemua}:
	 *
	 * <ul>
	 *   <li>tanggal jurnal = tanggal persetujuan proses transitori;</li>
	 *   <li>akun debet = akun pengajuan transfer yang berpasangan;</li>
	 *   <li>akun kredit = akun transitori milik cara pembayaran transfer;</li>
	 *   <li>nominal diambil dari pengajuan transfer, dan bila &le; 0,1 posisi debet/kredit
	 *       ditukar -- sama seperti layar;</li>
	 *   <li>bila pengajuan transfer belum menunjuk balik ke transitori ini
	 *       ({@code transitoriData} kosong), tautannya dilengkapi lebih dulu. Langkah ini ada
	 *       di layar dan dipertahankan: tanpa tautan itu pembatalan dan penelusuran jurnal
	 *       kehilangan jejak pasangannya.</li>
	 * </ul>
	 *
	 * <p><b>Satuan kerja.</b> Layar memakai satuan kerja PENGGUNA yang sedang login
	 * ({@code Common.getSatuanKerja()}, dari konteks sesi ZK). Dari API konteks itu biasanya
	 * kosong; entitas Transitori sendiri tidak menyimpan satuan kerja, jadi dipakai satuan kerja
	 * pengguna bila tersedia dan satuan kerja PENGAJUAN TRANSFER pasangannya sebagai cadangan --
	 * lebih baik daripada jurnal tanpa satuan kerja, yang membuat laporan per unit kehilangan
	 * barisnya.</p>
	 *
	 * @return jumlah dokumen yang BERHASIL diposting.
	 */
	@SuppressWarnings("unchecked")
	public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<Long> ids = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory")).setProjection(Projections.property("id")).list();

			PostingHistory postingHistory = new PostingHistory(PostingHistory.PENGAJUAN_TRANSITORI);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal transitori dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			SatuanKerja satuanKerjaPengguna = null;
			try {
				satuanKerjaPengguna = Common.getSatuanKerja();
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(empty-catch) PostingProsesTransitoriAction.postingSemua-satuanKerja");
			}

			for (Long id : ids) {
				try {
					Transitori transitori = (Transitori) session.createCriteria(Transitori.class)
							.add(Restrictions.idEq(id)).uniqueResult();
					if (transitori == null || transitori.getDaftarPengajuanTransfer() == null) {
						continue;
					}
					DaftarPengajuanTransfer dpt = transitori.getDaftarPengajuanTransfer();

					// Lengkapi tautan balik pengajuan -> transitori, sama seperti layar.
					if (dpt.getTransitoriData() == null) {
						try {
							session.refresh(dpt);
							dpt.setTransitoriData(transitori);
							session = HibernateUtil.currentNativeSession();
							session.getTransaction().begin();
							Common.refreshUpdate(session, dpt);
							session.getTransaction().commit();
						} catch (Exception e) {
							try {
								session.getTransaction().rollback();
							} catch (Exception ex) {
								// rollback gagal: kegagalan aslinya yang dilaporkan
							}
							ais.common.ErrorAuditUtil.record(e, "PostingProsesTransitoriAction jalur API");
						}
					}

					java.util.Date tgl = transitori.getProsesTransitori().getTanggalPersetujuan();
					Akun akunDebet = dpt.getAkun();
					Akun akunKredit = dpt.getProsesTransfer() == null ? null
							: (dpt.getProsesTransfer().getCaraPembayaranTransfer() == null ? null
									: dpt.getProsesTransfer().getCaraPembayaranTransfer().getAkunTransitori());
					if (akunDebet == null || akunKredit == null) {
						// Jurnalnya tidak lengkap: dilewati, sama seperti layar yang tidak
						// menampilkan tombol posting untuk baris berjurnal tidak valid.
						continue;
					}

					Double nominal = dpt.getNominal();
					String ket = "";
					try {
						ket = "Pengajuan transitori \"" + transitori.getNama() + "\" senilai "
								+ Common.numberFormat.get().format(nominal);
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e, "PostingProsesTransitoriAction jalur API");
					}

					SatuanKerja satuanKerja = satuanKerjaPengguna != null ? satuanKerjaPengguna
							: dpt.getSatuanKerja();

					boolean tersimpan = false;
					try {
						session = HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						if (nominal > 0.1) {
							CommonAkunting.saveTransaksi(akunDebet, akunKredit, null, null, postingHistory,
									Boolean.TRUE, ket, tgl, nominal, Double.valueOf(0.0), transitori, satuanKerja,
									session);
						} else {
							CommonAkunting.saveTransaksi(akunKredit, akunDebet, null, null, postingHistory,
									Boolean.TRUE, ket, tgl, nominal, Double.valueOf(0.0), transitori, satuanKerja,
									session);
						}
						session.getTransaction().commit();
						tersimpan = true;
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e, "PostingProsesTransitoriAction jalur API");
					}

					if (tersimpan) {
						// Penanda posting hanya dipasang bila jurnalnya BENAR-BENAR tersimpan.
						transitori.setPostingHistory(postingHistory);
						session = HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.update(transitori);
						session.getTransaction().commit();
						n++;
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "auto-audit PostingProsesTransitoriAction.postingSemua");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingProsesTransitoriAction jalur API");
		} finally {
			try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PostingProsesTransitoriAction.postingSemua-disconnect"); }
			try { HibernateUtil.closeSession(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PostingProsesTransitoriAction.postingSemua-close"); }
		}
		return n;
	}
}
