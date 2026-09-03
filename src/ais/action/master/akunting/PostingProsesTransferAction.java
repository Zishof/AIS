package ais.action.master.akunting;

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
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
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
 * <h3>PostingProsesTransferAction — Controller Posting Jurnal Proses Transfer Dana</h3>
 *
 * <p><strong>Untuk apa:</strong><br>
 * Kelas ini merupakan controller ZK (GenericAutowireComposer) yang mengelola halaman posting
 * jurnal akuntansi untuk transaksi proses transfer dana. Tujuan utamanya adalah memindahkan
 * data DaftarPengajuanTransfer yang telah disetujui dan direalisasikan ke dalam buku besar
 * akuntansi (GrupTransaksi) melalui mekanisme posting. Posting berarti membuat entri jurnal
 * debet-kredit yang mencatat perpindahan dana antar akun sesuai dengan metode pembayaran
 * transfer yang dipilih. Halaman ini digunakan oleh bagian keuangan atau bendahara untuk
 * mengesahkan transaksi transfer ke dalam laporan keuangan resmi.</p>
 *
 * <p><strong>Cara kerja:</strong><br>
 * Setelah halaman dimuat (doAfterCompose), controller membaca konfigurasi tanggal default
 * (6 bulan terakhir), memeriksa hak akses pengguna, kemudian memanggil
 * {@code loadDataDenganProgressPosting} untuk menampilkan daftar DaftarPengajuanTransfer
 * yang memenuhi syarat posting. Setiap baris ditampilkan oleh inner class
 * {@code DaftarPengajuanTransferRenderer} yang menunjukkan informasi kode, nama, nominal,
 * tanggal, rencana jurnal debet-kredit, serta status posting (sudah/belum). Pengguna dapat
 * melakukan posting satu per satu melalui tombol baris, atau sekaligus massal melalui
 * {@code onPostingSemua}. Proses posting massal dijalankan di thread latar belakang menggunakan
 * {@code HibernateUtil.currentNativeSession()} agar tidak memblokir antarmuka. Setiap
 * transaksi menghasilkan satu {@code PostingHistory} (batch) dan sejumlah {@code GrupTransaksi}
 * (baris jurnal debet-kredit). Apabila transaksi melibatkan pajak barang (PPh), akun dana
 * titipan pajak ditambahkan ke sisi debet dengan nilai proporsional, sehingga nilai akun
 * kas berkurang sesuai nominal setelah dipotong pajak. Pembatalan posting menghapus
 * GrupTransaksi terkait dan mengosongkan referensi PostingHistory pada entitas.</p>
 *
 * <p><strong>Threading:</strong><br>
 * Proses posting massal (onPostingSemua) dijalankan dalam {@code new Thread(...).start()}.
 * Thread ini menggunakan {@code HibernateUtil.currentNativeSession()} — yaitu session Hibernate
 * khusus background yang membutuhkan manajemen transaksi eksplisit (begin/commit) dan harus
 * ditutup secara manual di blok {@code finally} via {@code session.disconnect(); session.close()}.
 * Jangan gunakan {@code currentSession()} di dalam thread ini karena session tersebut dikelola
 * oleh ZK lifecycle dan tidak aman diakses dari thread non-ZK. Progress ditampilkan ke pengguna
 * melalui {@code Label} yang diperbarui secara berkala. Posting satu baris (tombol per baris)
 * menggunakan {@code currentSession()} yang dijalankan dalam event timer ZK sehingga tetap
 * di-thread ZK dan aman.</p>
 *
 * <p><strong>Pemeliharaan:</strong><br>
 * Jika skema akun berubah (misalnya penambahan jenis pajak baru atau perubahan mapping akun
 * transitori), periksa logika penentuan {@code akunDebet} di dalam loop posting — khususnya
 * cabang {@code getTransitori()} yang mengalihkan akun debet ke {@code akunTransitori} milik
 * CaraPembayaranTransfer. Pastikan pula field {@code SaldoAwalMasterAssetDetail} terus
 * diperbarui saat ada perubahan skema asset. Variabel {@code sudahPostingDasbor} diisi dari
 * parameter yang dikirimkan oleh dasbor draft jurnal; jika fitur dasbor berubah, pastikan
 * parameter ini konsisten. Filter tanggal menggunakan fungsi SQL {@code date()} yang bersifat
 * spesifik PostgreSQL — perhatikan ini jika database berubah.</p>
 *
 * @see DaftarPengajuanTransfer
 * @see PostingHistory
 * @see GrupTransaksi
 * @see CommonAkunting
 */
public class PostingProsesTransferAction extends GenericAutowireComposer {

	/**
	 * Serial version UID untuk serialisasi kelas ini sebagai Serializable ZK composer.
	 * Nilai ini tidak perlu diubah kecuali ada perubahan struktural mayor pada kelas.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar DaftarPengajuanTransfer dengan status postingnya. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman data pada grid. */
	private Paging paging;

	/** Kotak teks pencarian berdasarkan kode pengajuan, kode proses transfer, nama, atau keterangan. */
	private Textbox searchkode;

	/**
	 * Checkbox filter untuk menampilkan hanya data yang belum diposting.
	 * Jika dicentang, hanya DaftarPengajuanTransfer dengan postingHistory null yang ditampilkan.
	 */
	private MyCheckboxConfig searchtampil;

	/**
	 * Checkbox filter untuk menampilkan hanya data yang sudah diposting.
	 * Jika dicentang, hanya DaftarPengajuanTransfer dengan postingHistory tidak null yang ditampilkan.
	 */
	private MyCheckboxConfig searchtelahtampil;

	/**
	 * Flag apakah pengguna memiliki hak UPDATE pada halaman ini.
	 * Digunakan untuk menentukan visibilitas tombol Posting dan Batalkan Posting.
	 */
	private boolean edit = false;

	/** Tombol "Posting Semua" di toolbar atas. Hanya tampil jika pengguna memiliki hak UPDATE. */
	private MyToolbarbuttonConfig sent;

	/**
	 * Flag apakah pengguna adalah admin atau memiliki hak APPROVE.
	 * Tombol Batalkan Posting hanya muncul jika flag ini true.
	 */
	public boolean adminLain;

	/** Datebox tanggal mulai filter rentang tanggal realisasi transfer. */
	private MyDatebox tglMulai;

	/** Datebox tanggal sampai filter rentang tanggal realisasi transfer. */
	private MyDatebox tglSampai;

	/** Pengguna yang sedang login, digunakan untuk mengisi informasi pembuat jurnal. */
	private Tbmuser tbmuser;

	/**
	 * Intercept lifecycle ZK sebelum halaman di-compose untuk memeriksa keamanan akses.
	 * Metode ini dipanggil oleh ZK framework sebelum komponen halaman dibuat. Jika pengguna
	 * tidak memiliki sesi yang valid, halaman akan diarahkan ke halaman login atau akses ditolak.
	 *
	 * <p><strong>Tujuan:</strong> Memastikan hanya pengguna yang telah terautentikasi yang
	 * dapat mengakses halaman posting transfer ini.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memanggil {@code Common.doCheckSecurity()} yang akan
	 * memeriksa atribut sesi dan mengarahkan ulang ke halaman login jika sesi tidak valid.
	 * Kemudian mendelegasikan ke implementasi superclass.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Jika sesi tidak valid, {@code Common.doCheckSecurity()}
	 * akan menghentikan proses sebelum halaman dirender.</p>
	 *
	 * @param page     Halaman ZK yang sedang di-compose.
	 * @param parent   Komponen induk dalam hierarki komponen ZK.
	 * @param compInfo Informasi metadata komponen dari file ZUL.
	 * @return ComponentInfo yang diteruskan ke superclass untuk proses compose normal.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Inisialisasi halaman setelah seluruh komponen ZUL selesai di-compose dan di-wire.
	 * Metode ini adalah titik masuk utama lifecycle ZK composer, dipanggil satu kali
	 * setelah ZK selesai membangun pohon komponen dari file ZUL.
	 *
	 * <p><strong>Tujuan:</strong> Menyiapkan semua komponen UI halaman posting transfer,
	 * termasuk filter tanggal, hak akses pengguna, listener paging, dan memuat data
	 * awal ke dalam grid.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menjalankan auto-wire ZK
	 *       yang menghubungkan field Java dengan komponen ZUL berdasarkan nama.</li>
	 *   <li>Menginisialisasi dukungan bahasa dengan {@code Common.initLaguage()}.</li>
	 *   <li>Memeriksa keberadaan sesi pengguna dan hak READ; jika tidak ada, mengarahkan
	 *       ke halaman logoff.</li>
	 *   <li>Mengambil pengguna saat ini ke field {@code tbmuser}.</li>
	 *   <li>Menetapkan rentang tanggal default: tglMulai = hari ini minus 6 bulan,
	 *       tglSampai = hari ini. Datebox dibuat readonly agar pengguna menggunakan
	 *       kontrol lain untuk mengubah tanggal (sesuai konfigurasi PostingJurnalHelper).</li>
	 *   <li>Menentukan flag {@code adminLain} dan {@code edit} berdasarkan hak akses.</li>
	 *   <li>Membaca parameter posting dari dasbor (sudahPostingDasbor) dan menerapkan
	 *       parameter tanggal dari konfigurasi PostingJurnalHelper.</li>
	 *   <li>Memanggil {@code loadDataDenganProgressPosting} untuk menampilkan data awal.</li>
	 *   <li>Memasang listener paging agar setiap ganti halaman memuat ulang data.</li>
	 *   <li>Memasang timer default ZK untuk auto-refresh data secara berkala.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong> Jika sesi tidak valid, metode langsung return
	 * setelah menghapus atribut sesi dan mengarahkan ke logoff. Exception dari inisialisasi
	 * komponen diteruskan ke ZK framework.</p>
	 *
	 * @param comp Komponen root halaman ZK yang telah selesai di-compose.
	 * @throws Exception Jika terjadi kesalahan saat inisialisasi komponen atau pemanggilan
	 *                   superclass.
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
	 * Event handler untuk membatalkan posting semua transaksi transfer yang tampil di grid.
	 * Metode ini dipanggil ketika pengguna mengklik tombol "Batalkan Posting Semua" di toolbar.
	 *
	 * <p><strong>Tujuan:</strong> Membatalkan posting secara massal untuk semua
	 * DaftarPengajuanTransfer yang saat ini memiliki postingHistory (sudah diposting)
	 * sesuai filter yang aktif.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Menampilkan dialog konfirmasi kepada pengguna sebelum melakukan pembatalan.</li>
	 *   <li>Jika pengguna mengklik OK, mengambil semua DaftarPengajuanTransfer yang sudah
	 *       diposting (postingHistory tidak null) menggunakan {@code initCriteria(true)}.</li>
	 *   <li>Untuk setiap transaksi, mengosongkan field postingHistory dan menyimpannya
	 *       kembali ke database menggunakan {@code Common.refreshSaveOrUpdate}.</li>
	 *   <li>Menghapus semua GrupTransaksi terkait dari tabel akunting.grup_transaksi
	 *       menggunakan SQL native, dengan syarat {@code daftar_pengajuan_transfer} sesuai
	 *       ID entitas dan {@code closing is null} (bukan jurnal penutup).</li>
	 *   <li>Setelah selesai, memuat ulang data melalui timer default ZK.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong> Kesalahan dalam proses pembatalan tidak ditangani
	 * secara eksplisit di level ini; exception akan diteruskan ke ZK framework. Pastikan
	 * transaksi database konsisten — jika ada kegagalan di tengah loop, data mungkin
	 * dalam keadaan sebagian dibatalkan.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika nama kolom FK di tabel grup_transaksi berubah
	 * (dari {@code daftar_pengajuan_transfer}), perbarui query SQL native di metode ini.</p>
	 *
	 * @param event Event ZK yang memicu pemanggilan metode ini, biasanya onClick dari toolbar.
	 * @throws Exception Jika terjadi kesalahan saat mengakses database atau memperbarui entitas.
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi transfer ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<DaftarPengajuanTransfer> daftarPengajuanTransfers = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (DaftarPengajuanTransfer daftarPengajuanTransfer : daftarPengajuanTransfers) {
								daftarPengajuanTransfer.setPostingHistory(null);
								Common.refreshSaveOrUpdate(daftarPengajuanTransfer);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.transaksi where grup_transaksi in"
														+ " (select id from akunting.grup_transaksi where daftar_pengajuan_transfer="
														+ daftarPengajuanTransfer.getId() + " and closing is null)")
										.executeUpdate();
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where daftar_pengajuan_transfer="
														+ daftarPengajuanTransfer.getId() + " and closing is null")
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
	 * Event handler untuk memposting semua transaksi transfer yang belum diposting secara massal.
	 * Menampilkan dialog form isian tanggal posting dan keterangan, lalu menjalankan proses
	 * posting di thread latar belakang agar antarmuka tetap responsif.
	 *
	 * <p><strong>Tujuan:</strong> Memungkinkan petugas keuangan memposting seluruh
	 * DaftarPengajuanTransfer yang belum diposting dalam satu operasi, menghasilkan satu
	 * {@code PostingHistory} sebagai batch dan banyak {@code GrupTransaksi} sebagai entri
	 * jurnal di buku besar.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Membuat jendela modal (MyWindow) berisi form dengan field: tanggal posting,
	 *       nama pengguna yang memposting (read-only), dan keterangan bebas.</li>
	 *   <li>Tombol "Simpan" memvalidasi tanggal wajib diisi, lalu menampilkan konfirmasi
	 *       kedua sebelum memulai proses.</li>
	 *   <li>Setelah konfirmasi, mengambil pengguna saat ini dan menampilkan progress bar
	 *       menggunakan {@code Common.displayLoadBar}.</li>
	 *   <li>Membuat {@code Thread} baru yang menjalankan posting di latar belakang:
	 *     <ul>
	 *       <li>Membuat satu {@code PostingHistory} dengan jenis
	 *           {@code JENIS_PENGAJUAN_TRANSFER} dan menyimpannya.</li>
	 *       <li>Mengambil daftar ID DaftarPengajuanTransfer yang belum diposting.</li>
	 *       <li>Untuk setiap ID, mengambil ulang entitas dari database (fresh load),
	 *           menentukan akun debet (dengan atau tanpa transitori) dan akun kredit
	 *           dari CaraPembayaranTransfer.</li>
	 *       <li>Jika ada SaldoAwalMasterAsset terkait, menghitung pajak PPh per detail
	 *           dan menambahkan akun dana titipan pajak ke daftar debet.</li>
	 *       <li>Memanggil {@code CommonAkunting.saveTransaksi} untuk menyimpan jurnal.</li>
	 *       <li>Mengaitkan PostingHistory ke entitas dan menyimpan perubahan.</li>
	 *       <li>Memperbarui label progress dengan persentase penyelesaian.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Setelah thread selesai, menutup session native dan menampilkan pesan sukses.</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong> Proses posting berjalan di {@code new Thread} yang
	 * menggunakan {@code HibernateUtil.currentNativeSession()}. Session ini HARUS ditutup
	 * secara eksplisit setelah selesai. Jangan mengakses komponen ZK langsung dari thread
	 * ini — gunakan label yang telah di-capture sebelum thread dimulai.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception per-transaksi ditangkap dan ditampilkan
	 * hanya jika pengguna adalah admin (via {@code Common.tampilErrorJikaAdmin}). Proses
	 * berlanjut ke transaksi berikutnya meskipun satu transaksi gagal.</p>
	 *
	 * @param event Event ZK yang memicu pemanggilan metode ini dari tombol toolbar.
	 * @throws Exception Jika terjadi kesalahan saat membangun komponen UI dialog.
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

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi transfer ?", "Pertanyaan",
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
											MyMessageboxConfig.show("Posting transaksi transfer berhasil dilakukan",
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

											List<Long> daftarPengajuanTransfers = initCriteria(true)
													.add(Restrictions.isNull("postingHistory"))
													.setProjection(Projections.property("id")).list();

											Session session = HibernateUtil.currentNativeSession();

											PostingHistory postingHistory = new PostingHistory(
													PostingHistory.JENIS_PENGAJUAN_TRANSFER);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											int rowIndex = 1;
											for (Long daftarPengajuanTransferid : daftarPengajuanTransfers) {

												try {
													DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
															.createCriteria(DaftarPengajuanTransfer.class)
															.createAlias("disposisiSop", "disposisiSop",
																	Criteria.LEFT_JOIN)
															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(Restrictions.or(
																	Restrictions.isNull("disposisiSop.aktif"),
																	Restrictions.eq("disposisiSop.aktif", true)))
															.add(Restrictions.idEq(daftarPengajuanTransferid))
															.uniqueResult();
													if (daftarPengajuanTransfer != null) {

														Akun akunDebet = daftarPengajuanTransfer.getAkun();
														Date tgl = daftarPengajuanTransfer.getProsesTransfer()
																.getTanggalPersetujuan();
														if (daftarPengajuanTransfer.getProsesTransfer()
																.getTanggalRealisasikan() != null) {
															tgl = daftarPengajuanTransfer.getProsesTransfer()
																	.getTanggalRealisasikan();
														}
														if (daftarPengajuanTransfer.getTransitori()) {
															akunDebet = daftarPengajuanTransfer.getProsesTransfer()
																	.getCaraPembayaranTransfer().getAkunTransitori();

														}

														Akun akunKredit = daftarPengajuanTransfer.getProsesTransfer()
																.getCaraPembayaranTransfer().getAkun();

														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															Double nominal = daftarPengajuanTransfer.getNominal();

															String ket = "";
															try {

																ket = "Daftar pengajuan transfer \""
																		+ daftarPengajuanTransfer.getNama()
																		+ "\" senominal " + Common.numberFormat.get().format(
																				daftarPengajuanTransfer.getNominal());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}
															label.setValue(ket + " (" + Common.numberFormat.get().format(
																	rowIndex * 100.0 / daftarPengajuanTransfers.size())
																	+ " %)");

															List<Akun> akunsDebets = new ArrayList<Akun>();
															List<Double> nilaiDebets = new ArrayList<Double>();

															Double totalPajak = 0.0;
															if (daftarPengajuanTransfer
																	.getSaldoAwalMasterAsset() != null) {

																List<SaldoAwalMasterAssetDetail> saldoAwalMasterAssetDetails = session
																		.createCriteria(
																				SaldoAwalMasterAssetDetail.class)
																		.add(Restrictions.eq("saldoAwal",
																				daftarPengajuanTransfer
																						.getSaldoAwalMasterAsset()))
																		.list();
																for (SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail : saldoAwalMasterAssetDetails) {
																	if (saldoAwalMasterAssetDetail
																			.getJenisPajakBarang() != null
																			&& saldoAwalMasterAssetDetail
																					.getJenisPajakBarang()
																					.getAkunDanaTitipan() != null) {
																		akunsDebets.add(saldoAwalMasterAssetDetail
																				.getJenisPajakBarang()
																				.getAkunDanaTitipan());

																		Double dpp = (saldoAwalMasterAssetDetail
																				.getJumlah()
																				* saldoAwalMasterAssetDetail
																						.getHarga());
																		Double pph = ((saldoAwalMasterAssetDetail
																				.getPersenPph() / 100.0) * dpp);
																		nilaiDebets.add(pph);
																		totalPajak += pph;
																	}
																}

															}

															akunsDebets.add(akunDebet);
															nilaiDebets.add(nominal - totalPajak);

															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																List<Akun> akunsKredits = new ArrayList<Akun>();
																akunsKredits.add(akunKredit);
																List<Double> nilaiKredits = new ArrayList<Double>();
																nilaiKredits.add(nominal);

																session.getTransaction().begin();

																if (nominal > 0.1) {
																	CommonAkunting.saveTransaksi(
																			akunsDebets.toArray(new Akun[] {}),
																			akunsKredits.toArray(new Akun[] {}),
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket, tgl,
																			nilaiDebets.toArray(new Double[] {}),
																			nilaiKredits.toArray(new Double[] {}),
																			denda, daftarPengajuanTransfer, satuanKerja,
																			session);
																} else {
																	CommonAkunting.saveTransaksi(
																			akunsKredits.toArray(new Akun[] {}),
																			akunsDebets.toArray(new Akun[] {}),
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket, tgl,
																			nilaiKredits.toArray(new Double[] {}),
																			nilaiDebets.toArray(new Double[] {}), denda,
																			daftarPengajuanTransfer, satuanKerja,
																			session);
																}

																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															daftarPengajuanTransfer.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(daftarPengajuanTransfer);
															session.getTransaction().commit();
														}
													}
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);

												}
												rowIndex++;
											}
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
	 * Renderer baris grid untuk menampilkan satu entri DaftarPengajuanTransfer beserta
	 * status posting, pratinjau jurnal, dan tombol aksi posting/batalkan per baris.
	 *
	 * <p><strong>Tujuan:</strong> Mengisi setiap baris grid dengan informasi lengkap
	 * dari satu DaftarPengajuanTransfer: kode, nama, link ke ProsesTransfer terkait,
	 * nominal, tanggal, pratinjau jurnal debet-kredit, status posting, dan tombol aksi.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Mengambil entitas {@code DaftarPengajuanTransfer} dari parameter arg1.</li>
	 *   <li>Membuat Vbox kode revisi (RevisiHelper) di kolom pertama dengan link ke
	 *       ProsesTransfer jika ada.</li>
	 *   <li>Kolom kedua menampilkan nama pengajuan dan link alur SOP jika ada DisposisiSop.</li>
	 *   <li>Menentukan akun debet: jika flag transitori aktif, gunakan akunTransitori
	 *       dari CaraPembayaranTransfer; jika tidak, gunakan akun langsung dari entitas.</li>
	 *   <li>Menentukan tanggal jurnal: gunakan tanggalRealisasikan jika ada, jika tidak
	 *       gunakan tanggalPersetujuan dari ProsesTransfer.</li>
	 *   <li>Jika ada SaldoAwalMasterAsset terkait, menghitung pajak PPh per detail
	 *       menggunakan currentSession() (bukan native session karena ini di-thread ZK).</li>
	 *   <li>Menampilkan pratinjau jurnal menggunakan {@code GrupTransaksi.tampilkanJurnal},
	 *       atau pesan error jika akun debet/kredit tidak lengkap.</li>
	 *   <li>Mengambil nomor bukti dari GrupTransaksi terkait dan menampilkan status posting.</li>
	 *   <li>Menambahkan tombol "Batalkan Posting" (hanya untuk admin/edit) dan tombol
	 *       "Posting Data" (untuk pengguna dengan hak edit yang belum diposting).</li>
	 *   <li>Tombol "Posting Data" per baris menggunakan currentSession() dan menyimpan
	 *       PostingHistory baru langsung, tanpa thread latar belakang.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong> Jika akun debet atau kredit null, baris
	 * menampilkan pesan "Transaksi tidak valid" dengan detail akun mana yang kosong,
	 * dan tombol aksi tidak ditampilkan untuk mencegah posting data yang cacat.</p>
	 */
	class DaftarPengajuanTransferRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Mengisi komponen UI untuk satu baris grid dari objek DaftarPengajuanTransfer.
		 * Dipanggil oleh ZK framework secara otomatis untuk setiap baris dalam model data grid.
		 *
		 * <p><strong>Tujuan:</strong> Merender satu baris tabel yang menampilkan informasi
		 * lengkap satu DaftarPengajuanTransfer termasuk status posting dan tombol aksi.</p>
		 *
		 * <p><strong>Cara kerja:</strong> Menulis komponen ZK (Label, A, Vbox, Hbox,
		 * Toolbarbutton) sebagai anak dari objek Row (arg0). Setiap kolom dibuat dengan
		 * memanggil setParent(arg0) pada komponen yang bersangkutan. Lihat dokumentasi
		 * kelas {@code DaftarPengajuanTransferRenderer} untuk penjelasan lengkap alur rendering.</p>
		 *
		 * <p><strong>Penanganan error:</strong> Jika terjadi exception saat render,
		 * exception diteruskan ke ZK framework yang akan menampilkan pesan error standar.</p>
		 *
		 * @param arg0 Baris ZK (Row) yang menjadi wadah komponen-komponen UI kolom.
		 * @param arg1 Objek data dari model, diharapkan bertipe DaftarPengajuanTransfer.
		 * @throws Exception Jika terjadi kesalahan akses database atau pembuatan komponen UI.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) arg1;

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(DaftarPengajuanTransfer.class, daftarPengajuanTransfer,
					daftarPengajuanTransfer.getKode() == null ? "" : daftarPengajuanTransfer.getKode()))
					.setParent(arg0);

			if (daftarPengajuanTransfer.getProsesTransfer() != null) {

				A a = new A(daftarPengajuanTransfer.getProsesTransfer().getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, daftarPengajuanTransfer.getProsesTransfer());

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);
			}

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(daftarPengajuanTransfer.getNama()).setParent(a);
			if (daftarPengajuanTransfer.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + daftarPengajuanTransfer.getDisposisiSop().getKeterangan()
						+ " (" + daftarPengajuanTransfer.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(daftarPengajuanTransfer.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			}

			Akun akunDebet = daftarPengajuanTransfer.getAkun();
			Date tgl = daftarPengajuanTransfer.getProsesTransfer().getTanggalPersetujuan();
			if (daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan() != null) {
				tgl = daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan();
			}
			if (daftarPengajuanTransfer.getTransitori()) {
				akunDebet = daftarPengajuanTransfer.getProsesTransfer().getCaraPembayaranTransfer().getAkunTransitori();

			}

			Akun akunKredit = daftarPengajuanTransfer.getProsesTransfer().getCaraPembayaranTransfer().getAkun();
			Double nilai = daftarPengajuanTransfer.getNominal();
			new Label(Common.numberFormat.get().format(daftarPengajuanTransfer.getNominal())).setParent(arg0);

			new Label(Common.dateFormat3.get().format(tgl)).setParent(arg0);

			if (akunDebet != null && akunKredit != null) {

				List<Akun> akunsDebets = new ArrayList<Akun>();
				List<Double> nilaiDebets = new ArrayList<Double>();

				Double totalPajak = 0.0;
				if (daftarPengajuanTransfer.getSaldoAwalMasterAsset() != null) {
					Session session = HibernateUtil.currentSession();
					List<SaldoAwalMasterAssetDetail> saldoAwalMasterAssetDetails = session
							.createCriteria(SaldoAwalMasterAssetDetail.class)
							.add(Restrictions.eq("saldoAwal", daftarPengajuanTransfer.getSaldoAwalMasterAsset()))
							.list();
					for (SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail : saldoAwalMasterAssetDetails) {
						if (saldoAwalMasterAssetDetail.getJenisPajakBarang() != null
								&& saldoAwalMasterAssetDetail.getJenisPajakBarang().getAkunDanaTitipan() != null) {
							akunsDebets.add(saldoAwalMasterAssetDetail.getJenisPajakBarang().getAkunDanaTitipan());

							Double dpp = (saldoAwalMasterAssetDetail.getJumlah()
									* saldoAwalMasterAssetDetail.getHarga());
							Double pph = ((saldoAwalMasterAssetDetail.getPersenPph() / 100.0) * dpp);
							nilaiDebets.add(pph);
							totalPajak += pph;
						}
					}

				}

				akunsDebets.add(akunDebet);
				nilaiDebets.add(nilai - totalPajak);

				List<Akun> akunsKredits = new ArrayList<Akun>();
				akunsKredits.add(akunKredit);
				List<Double> nilaiKredits = new ArrayList<Double>();
				nilaiKredits.add(nilai);
				String warnings = "";
				GrupTransaksi.tampilkanJurnal(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits, warnings)
						.setParent(arg0);

			} else {
				new Label("Transaksi tidak valid."
						+ (akunDebet != null ? " Debet: " + akunDebet.toString() + "." : " Akun debet tidak ada.")
						+ (akunKredit != null ? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
								: " Akun kredit tidak ada."))
						.setParent(arg0);
			}

			String bukti = "";
			Session session = HibernateUtil.currentSession();
			bukti = (String) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.eq("daftarPengajuanTransfer", daftarPengajuanTransfer)).setMaxResults(1)
					.setProjection(Projections.property("kode")).uniqueResult();

			new Label(daftarPengajuanTransfer.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: daftarPengajuanTransfer.getPostingHistory().toString() + ", no. bukti : " + bukti)
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunKredit != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && daftarPengajuanTransfer.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								daftarPengajuanTransfer.setPostingHistory(null);
								Common.refreshSaveOrUpdate(daftarPengajuanTransfer);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.transaksi where grup_transaksi in"
														+ " (select id from akunting.grup_transaksi where daftar_pengajuan_transfer="
														+ daftarPengajuanTransfer.getId() + " and closing is null)")
										.executeUpdate();
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where daftar_pengajuan_transfer="
														+ daftarPengajuanTransfer.getId() + " and closing is null")
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
				button.setVisible(edit && daftarPengajuanTransfer.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PENGAJUAN_TRANSFER);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);
								SatuanKerja satuanKerja = Common.getSatuanKerja();

								Date tgl = daftarPengajuanTransfer.getProsesTransfer().getTanggalPersetujuan();
								if (daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan() != null) {
									tgl = daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan();
								}
								Akun akunDebet = daftarPengajuanTransfer.getAkun();
								if (daftarPengajuanTransfer.getTransitori()) {
									akunDebet = daftarPengajuanTransfer.getProsesTransfer().getCaraPembayaranTransfer()
											.getAkunTransitori();
								}
								Akun akunKredit = daftarPengajuanTransfer.getProsesTransfer()
										.getCaraPembayaranTransfer().getAkun();
								Double nominal = daftarPengajuanTransfer.getNominal();

								if (akunDebet != null && akunKredit != null) {

									List<Akun> akunsDebets = new ArrayList<Akun>();
									List<Double> nilaiDebets = new ArrayList<Double>();

									Double totalPajak = 0.0;
									if (daftarPengajuanTransfer.getSaldoAwalMasterAsset() != null) {

										List<SaldoAwalMasterAssetDetail> saldoAwalMasterAssetDetails = session
												.createCriteria(SaldoAwalMasterAssetDetail.class)
												.add(Restrictions.eq("saldoAwal",
														daftarPengajuanTransfer.getSaldoAwalMasterAsset()))
												.list();
										for (SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail : saldoAwalMasterAssetDetails) {
											if (saldoAwalMasterAssetDetail.getJenisPajakBarang() != null
													&& saldoAwalMasterAssetDetail.getJenisPajakBarang()
															.getAkunDanaTitipan() != null) {
												akunsDebets.add(saldoAwalMasterAssetDetail.getJenisPajakBarang()
														.getAkunDanaTitipan());

												Double dpp = (saldoAwalMasterAssetDetail.getJumlah()
														* saldoAwalMasterAssetDetail.getHarga());
												Double pph = ((saldoAwalMasterAssetDetail.getPersenPph() / 100.0)
														* dpp);
												nilaiDebets.add(pph);
												totalPajak += pph;
											}
										}

									}

									akunsDebets.add(akunDebet);
									nilaiDebets.add(nominal - totalPajak);

									Boolean apakahUangMasuk = true;

									String ket = "";
									try {

										ket = "Daftar pengajuan transfer \"" + daftarPengajuanTransfer.getNama()
												+ "\" senominal "
												+ Common.numberFormat.get().format(daftarPengajuanTransfer.getNominal());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									try {

										Akun akunDenda = null;
										Akun akunPiutangDenda = null;
										Double denda = 0.0;

										List<Akun> akunsKredits = new ArrayList<Akun>();
										akunsKredits.add(akunKredit);
										List<Double> nilaiKredits = new ArrayList<Double>();
										nilaiKredits.add(nominal);

										if (nominal > 0.1) {
											CommonAkunting.saveTransaksi(akunsDebets.toArray(new Akun[] {}),
													akunsKredits.toArray(new Akun[] {}), akunDenda, akunPiutangDenda,
													postingHistory, apakahUangMasuk, ket, tgl,
													nilaiDebets.toArray(new Double[] {}),
													nilaiKredits.toArray(new Double[] {}), denda,
													daftarPengajuanTransfer, satuanKerja, session);
										} else {
											CommonAkunting.saveTransaksi(akunsKredits.toArray(new Akun[] {}),
													akunsDebets.toArray(new Akun[] {}), akunDenda, akunPiutangDenda,
													postingHistory, apakahUangMasuk, ket, tgl,
													nilaiKredits.toArray(new Double[] {}),
													nilaiDebets.toArray(new Double[] {}), denda,
													daftarPengajuanTransfer, satuanKerja, session);
										}

										session.flush();
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									daftarPengajuanTransfer.setPostingHistory(postingHistory);
									session.update(daftarPengajuanTransfer);
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
	 * Membuat dan mengembalikan objek Criteria Hibernate untuk query DaftarPengajuanTransfer
	 * sesuai semua filter yang aktif di UI.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan satu titik definisi filter data yang konsisten
	 * untuk digunakan baik saat menghitung jumlah data (paging) maupun saat mengambil
	 * data halaman aktif.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Membuat Criteria pada entitas DaftarPengajuanTransfer dengan sesi terkelola.</li>
	 *   <li>Menerapkan restriksi posting dari {@code PostingJurnalHelper.restriksiPosting}
	 *       berdasarkan nilai {@code sudahPostingDasbor} (null = semua, true = sudah posting,
	 *       false = belum posting dari dasbor draft jurnal).</li>
	 *   <li>Melakukan left join ke disposisiSop dan memfilter hanya yang aktif.</li>
	 *   <li>Melakukan inner join ke prosesTransfer dan memfilter hanya yang sudah
	 *       direalisasikan (realisasikanOleh not null) dan sudah disetujui (disetujuiOleh
	 *       not null) — hanya transfer yang benar-benar selesai yang bisa diposting.</li>
	 *   <li>Filter checkbox belum posting (searchtampil) dan sudah posting (searchtelahtampil).</li>
	 *   <li>Filter nominal tidak nol dan tidak null.</li>
	 *   <li>Filter tanggal realisasi dalam rentang tglMulai–tglSampai menggunakan SQL native
	 *       dengan fungsi {@code date()} PostgreSQL.</li>
	 *   <li>Filter pencarian teks bebas (searchkode) mencakup kode proses transfer, kode
	 *       pengajuan, nama, dan keterangan.</li>
	 *   <li>Jika parameter {@code order} true, menambahkan urutan descending berdasarkan
	 *       id proses transfer dan id pengajuan transfer.</li>
	 * </ol>
	 *
	 * <p><strong>Pemeliharaan:</strong> Filter tanggal menggunakan fungsi SQL {@code date()}
	 * yang khusus PostgreSQL. Jika kolom {@code tanggal_realisasikan} berganti nama, perbarui
	 * SQL restriction di sini. Jika filter baru perlu ditambahkan (misal filter satuan kerja),
	 * tambahkan restriction baru sebelum baris pengurutan.</p>
	 *
	 * @param order Jika true, menambahkan klausa ORDER BY (desc id prosesTransfer, desc id).
	 *              Jika false, hanya menghasilkan Criteria untuk keperluan count paging.
	 * @return Objek Criteria yang siap dieksekusi untuk mengambil list atau menghitung jumlah.
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(DaftarPengajuanTransfer.class).add(ais.action.master.helper.PostingJurnalHelper.restriksiPosting("postingHistory", sudahPostingDasbor))
				.createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
						Restrictions.eq("disposisiSop.aktif", true)))
				.createAlias("prosesTransfer", "prosesTransfer")

				.add(Restrictions.isNotNull("prosesTransfer.realisasikanOleh"))
				.add(Restrictions.isNotNull("prosesTransfer.disetujuiOleh"))

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(!searchtelahtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNotNull("postingHistory"))

				.add(Restrictions.ne("nominal", 0.0)).add(Restrictions.isNotNull("nominal"))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(tanggal_realisasikan) between date('" + Common.databaseDateFormat.get().format(tglMulai.getValue())
								+ "') and  date('" + Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("prosesTransfer.kode", searchkode.getValue(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("nama", searchkode.getValue(), MatchMode.ANYWHERE),
												Restrictions.ilike("keterangan", searchkode.getValue(),
														MatchMode.ANYWHERE)))));

		if (order)
			criteria.addOrder(Order.desc("prosesTransfer.id")).addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * Memuat dan menampilkan data DaftarPengajuanTransfer ke grid tanpa menampilkan progress bar.
	 * Metode internal ini hanya dipanggil dari dalam {@code loadDataDenganProgressPosting}
	 * setelah progress bar sudah ditampilkan.
	 *
	 * <p><strong>Tujuan:</strong> Memisahkan logika pengambilan dan penampilan data dari
	 * logika manajemen progress bar, sehingga lebih mudah diuji dan dipelihara.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Menghitung total data dan memperbarui komponen paging menggunakan
	 *       {@code Common.initPaging(initCriteria(false), paging)}.</li>
	 *   <li>Mengambil data halaman aktif dengan setMaxResults (batas per halaman)
	 *       dan setFirstResult (offset halaman aktif).</li>
	 *   <li>Membungkus hasil dalam SimpleListModel dan menetapkan renderer
	 *       DaftarPengajuanTransferRenderer ke grid.</li>
	 *   <li>Memanggil setModelCheckMobile untuk mendukung tampilan mobile.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dari Hibernate query akan diteruskan
	 * ke caller. Caller ({@code loadDataDenganProgressPosting}) membungkus pemanggilan
	 * ini dalam blok try-finally untuk memastikan flag loading direset.</p>
	 *
	 * @param event Event ZK yang memicu reload, dapat null jika dipanggil secara programatik.
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DaftarPengajuanTransfer> daftarPengajuanTransfer = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(daftarPengajuanTransfer);
		grid.setRowRenderer(new DaftarPengajuanTransferRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Event handler publik untuk memuat ulang data grid. Dipanggil oleh ZK framework
	 * ketika event onSearchDefault diterima (misalnya dari tombol Cari atau paging).
	 *
	 * <p><strong>Tujuan:</strong> Menjadi titik masuk publik untuk memuat ulang data,
	 * dengan menggunakan mekanisme progress bar agar UX lebih baik saat data banyak.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Mendelegasikan ke
	 * {@code loadDataDenganProgressPosting(event)} yang akan menampilkan progress bar
	 * dan memuat data di timer event berikutnya.</p>
	 *
	 * @param event Event ZK yang memicu pencarian ulang data.
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Status posting yang diterima dari parameter dasbor draft jurnal.
	 * Null berarti halaman dibuka dari menu biasa (tampilkan semua).
	 * True berarti hanya tampilkan yang sudah diposting (dari dasbor jurnal posted).
	 * False berarti hanya tampilkan yang belum diposting (dari dasbor draft jurnal).
	 */
	private Boolean sudahPostingDasbor = null;

	/**
	 * Flag untuk mencegah pemanggilan ganda loadDataDenganProgressPosting secara bersamaan.
	 * Jika true, berarti sedang ada proses loading yang berjalan.
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag yang menandai ada permintaan reload baru yang masuk saat loading sedang berlangsung.
	 * Setelah loading selesai, jika flag ini true, loading akan dijalankan ulang.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * Memuat data posting jurnal ke grid dengan menampilkan progress bar selama proses berlangsung.
	 * Metode ini adalah wrapper utama yang memastikan tidak ada dua proses loading berjalan
	 * bersamaan, dan menangani permintaan reload yang masuk saat loading sedang aktif.
	 *
	 * <p><strong>Tujuan:</strong> Memberikan pengalaman pengguna yang lebih baik saat
	 * memuat data dengan menampilkan indikator progress, dan mencegah race condition
	 * pada load bersamaan akibat paging atau perubahan filter yang cepat.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Jika {@code postingJurnalLoadingAktif} sudah true (loading sedang berjalan),
	 *       set {@code postingJurnalReloadTertunda = true} dan tampilkan notifikasi,
	 *       kemudian return tanpa memulai loading baru.</li>
	 *   <li>Set {@code postingJurnalLoadingAktif = true} dan tampilkan progress bar
	 *       awal via {@code PostingJurnalLoadingUtil.show}.</li>
	 *   <li>Menjalankan loading data melalui {@code Common.createDefaultTimer} agar
	 *       UI dapat dirender ulang dahulu sebelum query database dieksekusi.</li>
	 *   <li>Di dalam timer event: memperbarui progress ke 48%, memanggil
	 *       {@code onSearchDefaultTanpaProgress}, memperbarui progress ke 92%.</li>
	 *   <li>Di blok finally: mereset flag loading. Jika ada reload tertunda, memulai
	 *       loading baru. Jika tidak, menandai progress selesai (100%).</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong> Seluruh metode ini berjalan di thread ZK event
	 * (bukan background thread). Timer ZK digunakan untuk memisahkan render progress bar
	 * dari eksekusi query, bukan untuk multi-threading.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika perlu menambahkan langkah loading (misalnya
	 * agregasi tambahan), tambahkan update progress antara 48% dan 92% dengan nilai
	 * intermediate yang sesuai.</p>
	 *
	 * @param event Event ZK yang memicu loading, dapat null jika dipanggil dari timer atau
	 *              inisialisasi. Diteruskan ke {@code onSearchDefaultTanpaProgress}.
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
	// Pola ini mengikuti PostingKasKecilAction/PostingKasBesarAction.
	//
	// PEMELIHARAAN: logika per-dokumen di sini HARUS tetap identik dengan
	// {@link #onPostingSemua}. Bila penentuan akun debet/kredit, perlakuan transitori,
	// atau pemecahan PPh berubah, ubah di KEDUA tempat.
	// =====================================================================

	/**
	 * Kriteria pengajuan transfer yang layak diposting pada rentang tanggal, tanpa bergantung
	 * pada komponen layar. Sama dengan bagian {@link #initCriteria(boolean)} yang tidak
	 * berhubungan dgn kotak pencarian, dan sama pula dgn kriteria baris "Jurnal Pengajuan
	 * Transfer" pada dasbor Draft Jurnal.
	 */
	private static Criteria kriteriaPostingStatic(Session session, java.util.Date mulai, java.util.Date sampai) {
		Criteria c = session.createCriteria(DaftarPengajuanTransfer.class)
				.createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN)
				.createAlias("prosesTransfer", "prosesTransfer")
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
						Restrictions.eq("disposisiSop.aktif", true)))
				.add(Restrictions.isNotNull("prosesTransfer.realisasikanOleh"))
				.add(Restrictions.isNotNull("prosesTransfer.disetujuiOleh"))
				.add(Restrictions.ne("nominal", 0.0)).add(Restrictions.isNotNull("nominal"));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(tanggal_realisasikan) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/**
	 * Batalkan posting SEMUA pengajuan transfer terposting dalam rentang. Mengikuti perilaku
	 * tombol lama: hapus baris grup_transaksi yang BELUM closing lalu kosongkan postingHistory --
	 * jurnal yang sudah closing TIDAK terhapus.
	 */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		// Transaksi dibuka sendiri. Memakai currentSession() seperti tombol di layar hanya
		// berhasil di dalam permintaan ZK, yang kerangkanya meng-commit sesi berjalan;
		// dipanggil dari API perubahannya tidak pernah tersimpan sehingga pembatalan
		// melaporkan sukses padahal jurnal dan penanda postingnya masih utuh.
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<DaftarPengajuanTransfer> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (DaftarPengajuanTransfer dpt : daftar) {
				try {
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					// Baris transaksi dihapus lebih dulu -- grup_transaksi adalah induknya.
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where daftar_pengajuan_transfer="
							+ dpt.getId() + " and closing is null)").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where daftar_pengajuan_transfer="
							+ dpt.getId() + " and closing is null").executeUpdate();
					dpt.setPostingHistory(null);
					session.update(dpt);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingProsesTransferAction jalur API");
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
	 * Posting SEMUA pengajuan transfer yang belum diposting dalam rentang. Logika per-dokumen
	 * IDENTIK dengan {@link #onPostingSemua}:
	 *
	 * <ul>
	 *   <li>akun debet = akun pengajuan; bila pengajuan bertanda <i>transitori</i>, debetnya
	 *       diganti akun transitori milik cara pembayaran transfer;</li>
	 *   <li>akun kredit = akun cara pembayaran transfer;</li>
	 *   <li>tanggal jurnal = tanggal realisasi bila ada, selain itu tanggal persetujuan;</li>
	 *   <li>bila pengajuan bertaut Saldo Awal Master Asset, PPh tiap barisnya dipecah lebih dulu
	 *       ke akun dana titipan jenis pajaknya, dan sisa nominal barulah masuk akun debet.</li>
	 * </ul>
	 *
	 * <p><b>Satuan kerja.</b> Layar ZK memakai satuan kerja PENGGUNA yang sedang login
	 * ({@code Common.getSatuanKerja()}, dibaca dari konteks sesi ZK). Dari API konteks itu
	 * biasanya kosong, jadi di sini dipakai satuan kerja pengguna bila tersedia dan satuan kerja
	 * DOKUMEN sebagai cadangan -- lebih baik daripada menulis jurnal tanpa satuan kerja sama
	 * sekali, yang membuat laporan per unit kehilangan barisnya.</p>
	 *
	 * @return jumlah dokumen yang BERHASIL diposting.
	 */
	@SuppressWarnings("unchecked")
	public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting) {
		return posting(mulai, sampai, oleh, tglPosting, null);
	}

	/**
	 * Memposting satu proses transfer melalui mesin jurnal yang sama dengan posting massal.
	 * Penyaring {@code postingHistory is null} membuat pemanggilan ulang aman dan tidak
	 * menghasilkan jurnal ganda.
	 */
	public static int postingSatu(long prosesTransferId, Tbmuser oleh, java.util.Date tglPosting) {
		return posting(null, null, oleh, tglPosting, Long.valueOf(prosesTransferId));
	}

	@SuppressWarnings("unchecked")
	private static int posting(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting, Long prosesTransferId) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			Criteria kriteria = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory"));
			if (prosesTransferId != null) {
				kriteria.add(Restrictions.eq("prosesTransfer.id", prosesTransferId));
			}
			List<Long> ids = kriteria.setProjection(Projections.property("id")).list();
			if (ids == null || ids.isEmpty()) {
				return 0;
			}

			PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_PENGAJUAN_TRANSFER);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan((prosesTransferId == null
					? "Posting massal pengajuan transfer dari dasbor jurnal"
					: "Posting otomatis saat realisasi proses transfer #" + prosesTransferId)
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
						"auto-audit(empty-catch) PostingProsesTransferAction.postingSemua-satuanKerja");
			}

			for (Long id : ids) {
				try {
					DaftarPengajuanTransfer dpt = (DaftarPengajuanTransfer) session
							.createCriteria(DaftarPengajuanTransfer.class)
							.createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
									Restrictions.eq("disposisiSop.aktif", true)))
							.add(Restrictions.idEq(id)).uniqueResult();
					if (dpt == null) {
						continue;
					}

					Akun akunDebet = dpt.getAkun();
					java.util.Date tgl = dpt.getProsesTransfer().getTanggalPersetujuan();
					if (dpt.getProsesTransfer().getTanggalRealisasikan() != null) {
						tgl = dpt.getProsesTransfer().getTanggalRealisasikan();
					}
					if (dpt.getTransitori()) {
						akunDebet = dpt.getProsesTransfer().getCaraPembayaranTransfer().getAkunTransitori();
					}
					Akun akunKredit = dpt.getProsesTransfer().getCaraPembayaranTransfer().getAkun();
					if (akunDebet == null || akunKredit == null) {
						// Jurnalnya tidak lengkap: dilewati, sama seperti layar yang tidak
						// menampilkan tombol posting untuk baris berjurnal tidak valid.
						continue;
					}

					Double nominal = dpt.getNominal();
					String ket = "";
					try {
						ket = "Daftar pengajuan transfer \"" + dpt.getNama() + "\" senominal "
								+ Common.numberFormat.get().format(dpt.getNominal());
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e, "PostingProsesTransferAction jalur API");
					}

					List<Akun> akunsDebets = new ArrayList<Akun>();
					List<Double> nilaiDebets = new ArrayList<Double>();
					Double totalPajak = 0.0;
					if (dpt.getSaldoAwalMasterAsset() != null) {
						List<SaldoAwalMasterAssetDetail> detail = session
								.createCriteria(SaldoAwalMasterAssetDetail.class)
								.add(Restrictions.eq("saldoAwal", dpt.getSaldoAwalMasterAsset())).list();
						for (SaldoAwalMasterAssetDetail d : detail) {
							if (d.getJenisPajakBarang() != null
									&& d.getJenisPajakBarang().getAkunDanaTitipan() != null) {
								akunsDebets.add(d.getJenisPajakBarang().getAkunDanaTitipan());
								Double dpp = (d.getJumlah() * d.getHarga());
								Double pph = ((d.getPersenPph() / 100.0) * dpp);
								nilaiDebets.add(pph);
								totalPajak += pph;
							}
						}
					}
					akunsDebets.add(akunDebet);
					nilaiDebets.add(nominal - totalPajak);

					List<Akun> akunsKredits = new ArrayList<Akun>();
					akunsKredits.add(akunKredit);
					List<Double> nilaiKredits = new ArrayList<Double>();
					nilaiKredits.add(nominal);

					SatuanKerja satuanKerja = satuanKerjaPengguna != null ? satuanKerjaPengguna
							: dpt.getSatuanKerja();

					boolean tersimpan = false;
					try {
						session = HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						if (nominal > 0.1) {
							CommonAkunting.saveTransaksi(akunsDebets.toArray(new Akun[] {}),
									akunsKredits.toArray(new Akun[] {}), null, null, postingHistory, Boolean.TRUE,
									ket, tgl, nilaiDebets.toArray(new Double[] {}),
									nilaiKredits.toArray(new Double[] {}), Double.valueOf(0.0), dpt, satuanKerja,
									session);
						} else {
							CommonAkunting.saveTransaksi(akunsKredits.toArray(new Akun[] {}),
									akunsDebets.toArray(new Akun[] {}), null, null, postingHistory, Boolean.TRUE,
									ket, tgl, nilaiKredits.toArray(new Double[] {}),
									nilaiDebets.toArray(new Double[] {}), Double.valueOf(0.0), dpt, satuanKerja,
									session);
						}
						session.getTransaction().commit();
						tersimpan = true;
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e, "PostingProsesTransferAction jalur API");
					}

					if (tersimpan) {
						// Penanda posting hanya dipasang bila jurnalnya BENAR-BENAR tersimpan.
						// Layar lama memasangnya di luar blok penyimpanan, sehingga dokumen yang
						// jurnalnya gagal tetap hilang dari daftar draft tanpa punya jurnal.
						dpt.setPostingHistory(postingHistory);
						session = HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.update(dpt);
						session.getTransaction().commit();
						n++;
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "auto-audit PostingProsesTransferAction.postingSemua");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingProsesTransferAction jalur API");
		} finally {
			try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PostingProsesTransferAction.postingSemua-disconnect"); }
			try { HibernateUtil.closeSession(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PostingProsesTransferAction.postingSemua-close"); }
		}
		return n;
	}
}
