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
import ais.database.model.asset.PembayaranPengadaanMasterAsset;
import ais.database.model.asset.PembayaranPengadaanMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
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
 * <h3>Untuk apa</h3>
 * {@code PostingPembayaranAction} adalah kontroler ZK (ZKoss 5.5,
 * {@link GenericAutowireComposer}) untuk halaman <b>Posting Jurnal
 * Pembayaran Pengadaan Aset</b>. Kelas ini mengelola proses pencatatan
 * jurnal akuntansi (posting) atas transaksi pembayaran kepada vendor
 * dalam modul pengadaan aset. Setiap detail pembayaran pengadaan
 * ({@link PembayaranPengadaanMasterAssetDetail}) yang sudah memiliki akun
 * debet dan kredit yang valid dapat diposting ke jurnal umum melalui
 * {@code CommonAkunting.saveTransaksi}, menghasilkan entitas
 * {@link PostingHistory} dan {@link GrupTransaksi} yang menjadi bukti
 * pencatatan akuntansi.
 *
 * <h3>Cara kerja</h3>
 * Pada inisialisasi ({@link #doAfterCompose}), kelas ini:
 * <ul>
 *   <li>Memverifikasi sesi dan hak akses pengguna.</li>
 *   <li>Mengisi filter tanggal default (6 bulan ke belakang hingga hari ini).</li>
 *   <li>Menentukan apakah pengguna adalah admin atau punya hak APPROVE
 *       (flag {@link #adminLain}) yang menentukan visibilitas tombol batal
 *       posting.</li>
 *   <li>Mengisi combo pemilik aset.</li>
 *   <li>Memuat data awal dengan progress bar via
 *       {@link #loadDataDenganProgressPosting}.</li>
 *   <li>Menyiapkan paging dan filter lanjut.</li>
 * </ul>
 *
 * <p>Grid utama menggunakan {@code PembayaranPengadaanMasterAssetRenderer}
 * yang untuk setiap baris menampilkan:
 * <ul>
 *   <li>Kode tagihan + link proses transfer (bila ada) + link alur SOP.</li>
 *   <li>Nama penyedia dan jenis pembayaran.</li>
 *   <li>Nilai dibayar dan tanggal transaksi.</li>
 *   <li>Preview tabel jurnal (HTML) dengan kolom Akun/Debet/Kredit bila akun
 *       valid, atau pesan error akun tidak ditemukan.</li>
 *   <li>Status posting (belum/sudah) beserta nomor bukti jurnal.</li>
 *   <li>Tombol Batalkan Posting (hanya admin) dan Posting Manual (bila belum
 *       diposting).</li>
 * </ul>
 *
 * <p>Dua operasi massal tersedia:
 * <ul>
 *   <li>{@link #onPostingSemua}: membuka form modal untuk memilih tanggal
 *       dan keterangan, kemudian memposting seluruh transaksi yang belum
 *       diposting dalam satu {@link PostingHistory} melalui thread background.</li>
 *   <li>{@link #onBatalkanPostingSemua}: membatalkan semua posting yang
 *       terkena filter aktif dengan menghapus {@code GrupTransaksi} terkait.</li>
 * </ul>
 *
 * <p>Logika penentuan akun debet/kredit bersifat multi-level:
 * <ol>
 *   <li>Akun debet: dari {@code JenisPenerimaanBarang.akunHutangPenyedia},
 *       atau di-override oleh {@code PenyediaAsset.akunUtang}.</li>
 *   <li>Bila {@code JenisPemesananPengadaanAsset.akunUtangDariAnggaran} true,
 *       akun debet diambil per detail pemesanan dari anggaran
 *       ({@code PermintaanPengadaanMasterAsset.akun}).</li>
 *   <li>Akun kredit: dari {@code JenisPembayaranBarang.akun}, atau di-override
 *       oleh {@code CaraPembayaranTransfer.akun} (transfer) atau
 *       {@code CaraPembayaranTransfer.akunTransitori} (transitori).</li>
 * </ol>
 *
 * <h3>Threading</h3>
 * Operasi posting massal ({@link #onPostingSemua}) dijalankan di thread
 * background (via {@code new Thread(...).start()}) menggunakan
 * {@code HibernateUtil.currentNativeSession()} agar tidak bergantung pada
 * thread-local session ZK. Progress ditampilkan via {@code label.setValue}.
 * Operasi lain berjalan di event thread ZK. Flag {@link #postingJurnalLoadingAktif}
 * dan {@link #postingJurnalReloadTertunda} digunakan untuk mengantri reload
 * grid agar tidak terjadi reload bersamaan.
 *
 * <h3>Pemeliharaan</h3>
 * <ul>
 *   <li>Logika penentuan akun di renderer dan di thread posting IDENTIK;
 *       bila ada perubahan aturan akun, kedua tempat harus diperbarui
 *       secara konsisten.</li>
 *   <li>SQL native {@code delete from akunting.grup_transaksi where ... and closing is null}
 *       digunakan langsung; pastikan nama tabel/schema tidak berubah.</li>
 *   <li>Filter {@link FilterLanjutHelper} di-setup dari ZUL; pastikan
 *       komponen ID-nya sesuai dengan yang diharapkan helper.</li>
 *   <li>Untuk memperluas filter (misalnya filter penyedia), tambahkan kondisi
 *       di {@link #initCriteria} dan pastikan komponen ZUL yang sesuai
 *       juga ditambahkan.</li>
 * </ul>
 *
 * @author AIS Development Team
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 * @see PembayaranPengadaanMasterAsset
 * @see PembayaranPengadaanMasterAssetDetail
 * @see PostingHistory
 * @see CommonAkunting
 */
public class PostingPembayaranAction extends GenericAutowireComposer {

	/**
	 * ID serialisasi untuk kompatibilitas deserialisasi antar versi kelas,
	 * misalnya saat sesi HTTP di-persist ke disk.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar detail pembayaran pengadaan untuk diposting. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman pada grid utama. */
	private Paging paging;

	/** Input teks pencarian berdasarkan kode atau nama pembayaran pengadaan. */
	private Textbox searchkode;

	/** Combo box filter berdasarkan pemilik aset. */
	private Combobox searchpemilikAsset;

	/**
	 * Banbox filter berdasarkan ruang; diisi dari komponen ZUL.
	 * Bisa null bila ZUL tidak menyertakan filter ruang.
	 */
	private AmbilDataRuangBanbox searchruang;

	/**
	 * Checkbox filter untuk menampilkan transaksi yang belum diposting
	 * (postingHistory IS NULL).
	 */
	private MyCheckboxConfig searchtampil;

	/**
	 * Checkbox filter untuk menampilkan transaksi yang sudah diposting
	 * (postingHistory IS NOT NULL).
	 */
	private MyCheckboxConfig searchtelahtampil;

	/** Flag apakah pengguna saat ini memiliki hak ubah (UPDATE). */
	private boolean edit = false;

	/** Tombol toolbar untuk posting semua transaksi massal. */
	private MyToolbarbuttonConfig sent;

	/**
	 * Flag apakah pengguna adalah admin sistem atau memiliki hak APPROVE.
	 * Nilai {@code true} mengaktifkan tombol "Batalkan Posting" per baris
	 * dan fitur posting massal. Di-set pada {@link #doAfterCompose}.
	 */
	public boolean adminLain;

	/** Datebox filter tanggal awal periode transaksi. */
	private MyDatebox tglMulai;

	/** Datebox filter tanggal akhir periode transaksi. */
	private MyDatebox tglSampai;

	/**
	 * Pengguna yang sedang login; di-cache agar tidak perlu memanggil
	 * {@code Common.getCurrentUser()} berulang kali dari thread background.
	 */
	private Tbmuser tbmuser;

	/**
	 * <b>Tujuan:</b> Metode yang dipanggil ZK framework sebelum halaman ZUL
	 * dikompilasi untuk melakukan pemeriksaan keamanan awal pada level sesi.
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} untuk
	 * memastikan pengguna memiliki hak akses ke halaman ini. Bila tidak,
	 * pengguna diarahkan ke halaman logoff. Implementasi super kemudian
	 * dipanggil untuk melanjutkan proses compose normal.
	 *
	 * <p><b>Parameter:</b>
	 * @param page     halaman ZK tempat komponen akan dimuat
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo metadata komponen yang akan dikompilasi
	 * @return {@code ComponentInfo} dari super untuk dilanjutkan ke proses
	 *         compose berikutnya
	 *
	 * <p><b>Pemeliharaan:</b> Jangan hapus pemanggilan {@code doCheckSecurity};
	 * ini adalah pemeriksaan keamanan wajib pertama.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Metode inisialisasi utama yang dipanggil ZK setelah semua
	 * komponen ZUL berhasil di-wire ke field kelas. Melakukan pengaturan sesi,
	 * hak akses, filter default, dan pemuatan data awal untuk halaman posting
	 * pembayaran pengadaan aset.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Inisialisasi bahasa via {@code Common.initLaguage()}.</li>
	 *   <li>Memverifikasi sesi {@code usersTemp} dan hak READ; bila tidak valid,
	 *       redirect ke logoff.</li>
	 *   <li>Menyimpan pengguna saat ini ke {@link #tbmuser} untuk digunakan
	 *       di thread background.</li>
	 *   <li>Mengatur filter tanggal default: {@link #tglMulai} = 6 bulan lalu,
	 *       {@link #tglSampai} = hari ini, keduanya readonly.</li>
	 *   <li>Menentukan {@link #adminLain}: true bila pengguna adalah admin
	 *       atau punya hak APPROVE.</li>
	 *   <li>Menetapkan hak UPDATE ke {@link #edit} dan visibilitas tombol
	 *       {@link #sent}.</li>
	 *   <li>Mengisi combo {@link #searchpemilikAsset} dengan daftar pemilik
	 *       aset yang aktif.</li>
	 *   <li>Memuat data pertama kali dengan progress bar via
	 *       {@link #loadDataDenganProgressPosting}.</li>
	 *   <li>Menginisialisasi paging dengan listener yang memuat ulang data.</li>
	 *   <li>Menyiapkan filter lanjut via {@link FilterLanjutHelper}.</li>
	 * </ol>
	 *
	 * <p><b>Parameter:</b>
	 * @param comp komponen root ZUL yang baru selesai dikompilasi
	 * @throws Exception bila terjadi kesalahan inisialisasi komponen atau akses
	 *                   database
	 *
	 * <p><b>Penanganan error:</b> Bila sesi tidak valid, method langsung return
	 * setelah redirect logoff.
	 *
	 * <p><b>Pemeliharaan:</b> Bila ada filter baru yang ditambahkan ke ZUL,
	 * pastikan komponen di-wire sebagai field dan inisialisasinya ditambahkan
	 * di sini.
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
	 * <b>Tujuan:</b> Menangani event klik tombol "Batalkan Posting Semua"
	 * di toolbar. Membatalkan posting untuk semua transaksi yang memenuhi
	 * filter aktif saat ini dan sudah pernah diposting, dengan konfirmasi
	 * pengguna terlebih dahulu.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menampilkan dialog konfirmasi via {@code MyMessageboxConfig}.</li>
	 *   <li>Bila dikonfirmasi, mengambil semua detail pembayaran yang sudah
	 *       diposting (filter {@code Restrictions.isNotNull("postingHistory")})
	 *       sesuai kriteria aktif.</li>
	 *   <li>Untuk setiap detail: menghapus referensi {@code postingHistory}
	 *       (set null + save), lalu menghapus semua {@code GrupTransaksi}
	 *       terkait yang belum closing via SQL native.</li>
	 *   <li>Setelah selesai, memuat ulang data melalui timer default.</li>
	 * </ol>
	 *
	 * <p><b>Parameter:</b>
	 * @param event event ZK dari tombol batalkan posting semua
	 * @throws Exception bila terjadi kesalahan saat dialog atau akses database
	 *
	 * <p><b>Penanganan error:</b> Exception dari SQL native atau update entitas
	 * akan merembet ke event thread ZK dan ditangani oleh error handler global.
	 *
	 * <p><b>Pemeliharaan:</b> SQL native menghapus dari {@code akunting.grup_transaksi};
	 * bila skema berubah, sesuaikan query di sini. Operasi ini tidak dapat
	 * dibalik; pastikan dialog konfirmasi tetap ada.
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi pembayaran ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<PembayaranPengadaanMasterAssetDetail> pembayaranPengadaanMasterAssets = initCriteria(
									true).add(Restrictions.isNotNull("postingHistory")).list();

							for (PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAsset : pembayaranPengadaanMasterAssets) {
								pembayaranPengadaanMasterAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pembayaranPengadaanMasterAsset);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where pembayaran_pengadaan_master_asset_detail="
												+ pembayaranPengadaanMasterAsset.getId() + " and closing is null")
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
	 * <b>Tujuan:</b> Menangani event klik tombol "Posting Semua" di toolbar.
	 * Membuka jendela modal yang memungkinkan pengguna memilih tanggal posting
	 * dan keterangan, kemudian memposting semua transaksi yang belum diposting
	 * sesuai filter aktif dalam satu batch menggunakan thread background.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat jendela modal ({@link MyWindow}) dengan form tanggal,
	 *       label pengguna, dan field keterangan.</li>
	 *   <li>Tombol Simpan memvalidasi tanggal, menampilkan dialog konfirmasi
	 *       kedua, lalu (bila dikonfirmasi) menjalankan thread background.</li>
	 *   <li>Thread background menggunakan {@code HibernateUtil.currentNativeSession()}
	 *       agar tidak bergantung pada session thread-local ZK.</li>
	 *   <li>Membuat satu {@link PostingHistory} untuk seluruh batch dengan
	 *       tanggal, pengguna, dan keterangan yang dipilih.</li>
	 *   <li>Iterasi setiap detail yang belum diposting; untuk tiap detail
	 *       menentukan akun debet/kredit sesuai logika multi-level, kemudian
	 *       memanggil {@code CommonAkunting.saveTransaksi}.</li>
	 *   <li>Memperbarui progress label di UI saat setiap transaksi diproses.</li>
	 *   <li>Setelah selesai, memanggil {@link #onSearchDefault} dan menutup
	 *       jendela modal.</li>
	 * </ol>
	 *
	 * <p><b>Parameter:</b>
	 * @param event event ZK dari tombol Posting Semua
	 * @throws Exception bila terjadi kesalahan saat membangun komponen form
	 *
	 * <p><b>Penanganan error:</b> Kesalahan pada setiap transaksi individual
	 * ditangkap secara lokal (blok try-catch per iterasi) agar satu transaksi
	 * bermasalah tidak menghentikan seluruh batch. Kesalahan ditampilkan via
	 * {@code Common.tampilErrorJikaAdmin} di event thread.
	 *
	 * <p><b>Threading:</b> Thread background menggunakan session native terpisah.
	 * Setiap transaksi di-commit secara individual. Hasil callback ke ZK UI
	 * dilakukan via {@code label.setValue} yang aman dari thread background.
	 *
	 * <p><b>Pemeliharaan:</b> Logika penentuan akun di thread ini harus
	 * selalu konsisten dengan yang ada di {@code PembayaranPengadaanMasterAssetRenderer}.
	 * Bila ada perubahan aturan akun, perbarui kedua tempat secara bersamaan.
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
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting Pembayaran belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal dan pilih tanggal dari kalender; (2) Pastikan tanggal berada dalam periode akuntansi yang aktif; (3) ulangi proses posting ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
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
											MyMessageboxConfig.show("Posting transaksi Pembayaran berhasil dilakukan",
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
													PostingHistory.JENIS_PEMBAYARAN_TAGIHAN);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<PembayaranPengadaanMasterAssetDetail> pembayaranPengadaanMasterAssetDetails = initCriteria(
													true).add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail : pembayaranPengadaanMasterAssetDetails) {

												if (pembayaranPengadaanMasterAssetDetail != null) {

													try {

														Akun akunDebet1 = pembayaranPengadaanMasterAssetDetail
																.getPenerimaanPengadaanMasterAsset() == null
																|| pembayaranPengadaanMasterAssetDetail
																		.getPenerimaanPengadaanMasterAsset()
																		.getJenisPenerimaanBarang() == null
																				? null
																				: pembayaranPengadaanMasterAssetDetail
																						.getPenerimaanPengadaanMasterAsset()
																						.getJenisPenerimaanBarang()
																						.getAkunHutangPenyedia();

														if (pembayaranPengadaanMasterAssetDetail
																.getPembayaranPengadaanMasterAsset() != null
																&& pembayaranPengadaanMasterAssetDetail
																		.getPembayaranPengadaanMasterAsset()
																		.getPenyedia() != null
																&& pembayaranPengadaanMasterAssetDetail
																		.getPembayaranPengadaanMasterAsset()
																		.getPenyedia().getAkunUtang() != null) {
															akunDebet1 = pembayaranPengadaanMasterAssetDetail
																	.getPembayaranPengadaanMasterAsset().getPenyedia()
																	.getAkunUtang();
														}

														List<Akun> akunDebets = new ArrayList<Akun>();

														Double nilai = pembayaranPengadaanMasterAssetDetail
																.getDibayar();
														List<Double> nilais = new ArrayList<Double>();

														if (pembayaranPengadaanMasterAssetDetail
																.getPenerimaanPengadaanMasterAsset() != null
																&& pembayaranPengadaanMasterAssetDetail
																		.getPenerimaanPengadaanMasterAsset()
																		.getPemesananPengadaanMasterAsset() != null
																&& pembayaranPengadaanMasterAssetDetail
																		.getPenerimaanPengadaanMasterAsset()
																		.getPemesananPengadaanMasterAsset()
																		.getJenisPemesananPengadaanAsset() != null
																&& pembayaranPengadaanMasterAssetDetail
																		.getPenerimaanPengadaanMasterAsset()
																		.getPemesananPengadaanMasterAsset()
																		.getJenisPemesananPengadaanAsset()
																		.getAkunUtangDariAnggaran()) {

															List<PemesananPengadaanMasterAssetDetail> pemesananPengadaanMasterAssetDetails = session
																	.createCriteria(
																			PemesananPengadaanMasterAssetDetail.class)
																	.add(Restrictions.eq(
																			"pemesananPengadaanMasterAsset",
																			pembayaranPengadaanMasterAssetDetail
																					.getPenerimaanPengadaanMasterAsset()
																					.getPemesananPengadaanMasterAsset()))
																	.list();

															for (PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail : pemesananPengadaanMasterAssetDetails) {
																try {

																	Double j = (pemesananPengadaanMasterAssetDetail
																			.getJumlah()
																			* pemesananPengadaanMasterAssetDetail
																					.getHargaBeli())
																			- ((pemesananPengadaanMasterAssetDetail
																					.getHargaPotongan() / 100.0)
																					* (pemesananPengadaanMasterAssetDetail
																							.getJumlah()
																							* pemesananPengadaanMasterAssetDetail
																									.getHargaBeli()));

																	Akun akun = pemesananPengadaanMasterAssetDetail
																			.getPermintaanPengadaanMasterAssetDetail()
																			.getPermintaanPengadaanMasterAsset()
																			.getAkun();

																	if (akun != null) {
																		akunDebets.add(akun);
																		nilais.add(j);
																	}

																} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingPembayaranAction.java:650");
																	// Detail anggaran tidak memiliki akun; lewati
																}
															}
														} else {
															akunDebets.add(akunDebet1);
															nilais.add(nilai);
														}

														Akun akunKredit = pembayaranPengadaanMasterAssetDetail
																.getPembayaranPengadaanMasterAsset()
																.getJenisPembayaranBarang() == null
																		? null
																		: pembayaranPengadaanMasterAssetDetail
																				.getPembayaranPengadaanMasterAsset()
																				.getJenisPembayaranBarang().getAkun();

														if (pembayaranPengadaanMasterAssetDetail
																.getDaftarPengajuanTransfer() != null
																&& pembayaranPengadaanMasterAssetDetail
																		.getDaftarPengajuanTransfer()
																		.getProsesTransfer() != null
																&& pembayaranPengadaanMasterAssetDetail
																		.getDaftarPengajuanTransfer()
																		.getProsesTransfer()
																		.getCaraPembayaranTransfer() != null
																&& pembayaranPengadaanMasterAssetDetail
																		.getDaftarPengajuanTransfer().getTransfer()
																&& pembayaranPengadaanMasterAssetDetail
																		.getDaftarPengajuanTransfer()
																		.getProsesTransfer().getCaraPembayaranTransfer()
																		.getAkun() != null) {
															akunKredit = pembayaranPengadaanMasterAssetDetail
																	.getDaftarPengajuanTransfer().getProsesTransfer()
																	.getCaraPembayaranTransfer().getAkun();
														}

														if (pembayaranPengadaanMasterAssetDetail
																.getDaftarPengajuanTransfer() != null
																&& pembayaranPengadaanMasterAssetDetail
																		.getDaftarPengajuanTransfer()
																		.getProsesTransfer() != null
																&& pembayaranPengadaanMasterAssetDetail
																		.getDaftarPengajuanTransfer()
																		.getProsesTransfer()
																		.getCaraPembayaranTransfer() != null
																&& pembayaranPengadaanMasterAssetDetail
																		.getDaftarPengajuanTransfer().getTransitori()
																&& pembayaranPengadaanMasterAssetDetail
																		.getDaftarPengajuanTransfer()
																		.getProsesTransfer().getCaraPembayaranTransfer()
																		.getAkunTransitori() != null) {
															akunKredit = pembayaranPengadaanMasterAssetDetail
																	.getDaftarPengajuanTransfer().getProsesTransfer()
																	.getCaraPembayaranTransfer().getAkunTransitori();
														}

														if (!akunDebets.isEmpty() && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {

																ket = "Pembayaran terhadap tagihan \""
																		+ (pembayaranPengadaanMasterAssetDetail
																				.getPenerimaanPengadaanMasterAsset()
																				.getKodeTagihan()
																				+ "-"
																				+ pembayaranPengadaanMasterAssetDetail
																						.getPenerimaanPengadaanMasterAsset()
																						.getPenyedia().getNama()
																				+ "-"
																				+ pembayaranPengadaanMasterAssetDetail
																						.getPenerimaanPengadaanMasterAsset()
																						.getKeterangan())
																		+ "\" sebanyak "
																		+ Common.numberFormat.get().format(
																				pembayaranPengadaanMasterAssetDetail
																						.getDibayar());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(ket + " ("
																	+ Common.numberFormat.get().format(rowIndex * 100.0
																			/ pembayaranPengadaanMasterAssetDetails
																					.size())
																	+ " %)");

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
																			pembayaranPengadaanMasterAssetDetail
																					.getTanggalTransaksi(),
																			nilais.toArray(new Double[] {}),
																			new Double[] { nilai }, denda,
																			pembayaranPengadaanMasterAssetDetail,
																			pembayaranPengadaanMasterAssetDetail
																					.getPenerimaanPengadaanMasterAsset()
																					.getPemesananPengadaanMasterAsset()
																					.getSatuanKerja(),
																			session);
																} else {
																	CommonAkunting.saveTransaksi(
																			new Akun[] { akunKredit },
																			akunDebets.toArray(new Akun[] {}),
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			pembayaranPengadaanMasterAssetDetail
																					.getTanggalTransaksi(),
																			new Double[] { nilai },
																			nilais.toArray(new Double[] {}), denda,
																			pembayaranPengadaanMasterAssetDetail,
																			pembayaranPengadaanMasterAssetDetail
																					.getPenerimaanPengadaanMasterAsset()
																					.getPemesananPengadaanMasterAsset()
																					.getSatuanKerja(),
																			session);
																}

																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															pembayaranPengadaanMasterAssetDetail
																	.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(pembayaranPengadaanMasterAssetDetail);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingPembayaranAction.java:793");
														// Lanjutkan ke item berikutnya bila ada error per item
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
	 * <b>Tujuan:</b> Kelas renderer dalam (inner class) yang bertanggung jawab
	 * menggambar setiap baris pada grid daftar detail pembayaran pengadaan yang
	 * akan atau sudah diposting ke jurnal. Mengimplementasikan
	 * {@code MyRowRenderer} dari framework AIS.
	 *
	 * <p><b>Cara kerja:</b> Untuk setiap entitas
	 * {@link PembayaranPengadaanMasterAssetDetail}:
	 * <ol>
	 *   <li>Menampilkan revisi + kode tagihan + tautan proses transfer
	 *       (bila ada) + tautan alur SOP (bila ada).</li>
	 *   <li>Nama penyedia dan jenis pembayaran barang.</li>
	 *   <li>Nilai dibayar dan tanggal transaksi.</li>
	 *   <li>Menentukan akun debet (dari JenisPenerimaanBarang atau PenyediaAsset,
	 *       atau per detail anggaran bila flag {@code akunUtangDariAnggaran} true)
	 *       dan akun kredit (dari JenisPembayaranBarang atau CaraPembayaranTransfer).</li>
	 *   <li>Bila akun valid: menampilkan tabel HTML preview jurnal
	 *       (Akun/Debet/Kredit). Bila tidak valid: menampilkan pesan error.</li>
	 *   <li>Status posting (belum/sudah + nomor bukti dari GrupTransaksi).</li>
	 *   <li>Toolbar: tombol Batalkan Posting (admin saja) dan Posting Manual
	 *       (bila belum diposting dan akun valid).</li>
	 * </ol>
	 *
	 * <p><b>Threading:</b> Berjalan di event thread ZK dengan session Hibernate
	 * thread-local. Tombol Posting Manual dan Batalkan menggunakan
	 * {@code Common.createDefaultTimer} untuk menunda operasi database ke
	 * dalam event ZK berikutnya.
	 *
	 * <p><b>Pemeliharaan:</b> Logika penentuan akun di sini HARUS konsisten
	 * dengan logika di thread background {@link #onPostingSemua}. Bila ada
	 * perubahan aturan akun, perbarui keduanya secara bersamaan.
	 */
	class PembayaranPengadaanMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Mengisi satu baris grid dengan data satu entitas
		 * {@link PembayaranPengadaanMasterAssetDetail} beserta preview jurnal
		 * dan tombol aksi posting.
		 *
		 * <p><b>Cara kerja:</b> Mengambil entitas dari {@code arg1}, membangun
		 * semua komponen UI secara programatik (Label, A, MyHtml, MyToolbarbuttonConfig),
		 * dan menyambungkannya ke baris {@code arg0}. Preview jurnal dirender
		 * sebagai HTML tabel inline. Status posting diambil dari field
		 * {@code postingHistory} di entitas dan nomor bukti dari query
		 * {@link GrupTransaksi}.
		 *
		 * <p><b>Parameter:</b>
		 * @param arg0 baris {@link Row} ZK yang akan diisi komponen
		 * @param arg1 objek data bertipe {@link PembayaranPengadaanMasterAssetDetail}
		 * @throws Exception bila terjadi kesalahan saat membuat komponen atau
		 *                   mengakses database
		 *
		 * <p><b>Penanganan error:</b> Exception saat mengambil detail anggaran
		 * per item ditangkap dan dilanjutkan ke item berikutnya. Pesan akun
		 * tidak valid ditampilkan inline di sel.
		 *
		 * <p><b>Pemeliharaan:</b> Bila struktur HTML preview jurnal berubah,
		 * cukup ubah bagian pembuatan string {@code deskripsi}. Pastikan
		 * {@code style} yang digunakan kompatibel dengan browser yang didukung.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail = (PembayaranPengadaanMasterAssetDetail) arg1;

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(PembayaranPengadaanMasterAssetDetail.class,
					pembayaranPengadaanMasterAssetDetail,
					pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getKodeTagihan()))
					.setParent(arg0);

			if (pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer() != null
					&& pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A a = new A(pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer()
						.getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);

			}

			final PembayaranPengadaanMasterAsset pembayaranPengadaanMasterAsset = pembayaranPengadaanMasterAssetDetail
					.getPembayaranPengadaanMasterAsset();

			if (pembayaranPengadaanMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(aaa);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pembayaranPengadaanMasterAsset.getDisposisiSop().getKeterangan() + " ("
						+ pembayaranPengadaanMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pembayaranPengadaanMasterAsset.getDisposisiSop().getId(), null,
								null, true, arg0.getTarget());
					}
				});
			}

			new Label(pembayaranPengadaanMasterAsset.getPenyedia() == null ? ""
					: pembayaranPengadaanMasterAsset.getPenyedia().getNama()).setParent(arg0);

			new Label(pembayaranPengadaanMasterAsset.getJenisPembayaranBarang() == null ? ""
					: pembayaranPengadaanMasterAsset.getJenisPembayaranBarang().getNama()).setParent(arg0);

			Double nilai = pembayaranPengadaanMasterAssetDetail.getDibayar();

			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

			new Label(Common.dateFormat3.get().format(pembayaranPengadaanMasterAssetDetail.getTanggalTransaksi()))
					.setParent(arg0);

			Akun akunDebet1 = pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() == null
					|| pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getJenisPenerimaanBarang() == null ? null
									: pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
											.getJenisPenerimaanBarang().getAkunHutangPenyedia();
			if (pembayaranPengadaanMasterAsset != null && pembayaranPengadaanMasterAsset.getPenyedia() != null
					&& pembayaranPengadaanMasterAsset.getPenyedia().getAkunUtang() != null) {
				akunDebet1 = pembayaranPengadaanMasterAsset.getPenyedia().getAkunUtang();
			}

			List<Akun> akunDebets = new ArrayList<Akun>();

			List<Double> nilais = new ArrayList<Double>();

			if (pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null
					&& pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getPemesananPengadaanMasterAsset() != null
					&& pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset() != null
					&& pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset()
							.getAkunUtangDariAnggaran()) {

				Session session = HibernateUtil.currentSession();
				List<PemesananPengadaanMasterAssetDetail> pemesananPengadaanMasterAssetDetails = session
						.createCriteria(PemesananPengadaanMasterAssetDetail.class)
						.add(Restrictions.eq("pemesananPengadaanMasterAsset", pembayaranPengadaanMasterAssetDetail
								.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()))
						.list();

				for (PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail : pemesananPengadaanMasterAssetDetails) {
					try {

						Double j = (pemesananPengadaanMasterAssetDetail.getJumlah()
								* pemesananPengadaanMasterAssetDetail.getHargaBeli())
								- ((pemesananPengadaanMasterAssetDetail.getHargaPotongan() / 100.0)
										* (pemesananPengadaanMasterAssetDetail.getJumlah()
												* pemesananPengadaanMasterAssetDetail.getHargaBeli()));

						Akun akun = pemesananPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail()
								.getPermintaanPengadaanMasterAsset().getAkun();

						if (akun != null) {
							akunDebets.add(akun);
							nilais.add(j);
						}

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingPembayaranAction.java:995");
						// Detail anggaran tidak memiliki akun; lewati
					}
				}
			} else {
				akunDebets.add(akunDebet1);
				nilais.add(nilai);
			}

			Akun akunKredit = pembayaranPengadaanMasterAsset.getJenisPembayaranBarang() == null ? null
					: pembayaranPengadaanMasterAsset.getJenisPembayaranBarang().getAkun();

			if (pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer() != null
					&& pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer() != null
					&& pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer()
							.getCaraPembayaranTransfer() != null
					&& pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer()
							.getCaraPembayaranTransfer().getAkun() != null) {
				akunKredit = pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer().getProsesTransfer()
						.getCaraPembayaranTransfer().getAkun();
			}

			if (!akunDebets.isEmpty() && akunKredit != null) {

				Akun akunDenda = null;
				Double denda = 0.0;

				if (denda != null && denda > 0.1 && akunDenda == null) {
					new Label("Transaksi tidak valid. Ada denda " + Common.numberFormat.get().format(denda)
							+ ", namun Akun denda tidak ditemukan").setParent(arg0);
				} else {

					String deskripsi = "<table style='width:100%;'>" + "<thead>";
					deskripsi += "<tr>";
					deskripsi += "<th style='border:solid;border-width: thin;'>Akun</th>";
					deskripsi += "<th style='border:solid;border-width: thin;'>Debet</th>";
					deskripsi += "<th style='border:solid;border-width: thin;'>Kredit</th>";
					deskripsi += "</tr>" + "</thead>" + "<tbody>";

					for (int i = 0; i < akunDebets.size(); i++) {
						Akun akun = akunDebets.get(i);
						Double n = nilais.get(i);
						deskripsi += "<tr>";
						deskripsi += "<td style='border:solid;border-width: thin;' >" + akun.getKode() + " - "
								+ akun.getNama() + "</td>";

						deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
								+ Common.numberFormat.get().format(n > 0.0 ? Math.abs(n) : 0.0) + "</td>";
						deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
								+ Common.numberFormat.get().format(n > 0.0 ? 0.0 : Math.abs(n)) + "</td>";
						deskripsi += "</tr>";
					}

					deskripsi += "<tr>";
					deskripsi += "<td style='border:solid;border-width: thin;' >" + akunKredit.getKode() + " - "
							+ akunKredit.getNama() + "</td>";

					deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
							+ Common.numberFormat.get().format(nilai > 0.0 ? 0.0 : Math.abs(nilai - denda)) + "</td>";
					deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
							+ Common.numberFormat.get().format(nilai > 0.0 ? Math.abs(nilai - denda) : 0.0) + "</td>";
					deskripsi += "</tr>";

					if (denda != null && denda > 0.1 && akunDenda != null) {
						deskripsi += "<tr>";
						deskripsi += "<td style='border:solid;border-width: thin;' >" + akunDenda.getKode() + " - "
								+ akunDenda.getNama() + "</td>";

						deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
								+ Common.numberFormat.get().format(denda > 0.0 ? 0.0 : Math.abs(denda)) + "</td>";
						deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
								+ Common.numberFormat.get().format(denda > 0.0 ? Math.abs(denda) : 0.0) + "</td>";
						deskripsi += "</tr>";
					}

					deskripsi += "</tbody></table>";
					new ais.ui.util.MyHtml(deskripsi).setParent(arg0);
				}
			} else {
				new Label("Transaksi tidak valid."
						+ (!akunDebets.isEmpty() ? " Debet: " + akunDebets.toString() : " Akun debet tidak ada.")
						+ (akunKredit != null ? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
								: " Akun kredit tidak ada."))
						.setParent(arg0);
			}

			Session session = HibernateUtil.currentSession();
			String bukti = "";
			bukti = (String) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.eq("pembayaranPengadaanMasterAssetDetail", pembayaranPengadaanMasterAssetDetail))
					.setMaxResults(1).setProjection(Projections.property("kode")).uniqueResult();

			new Label(pembayaranPengadaanMasterAssetDetail.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: pembayaranPengadaanMasterAssetDetail.getPostingHistory().toString() + ", no. bukti : " + bukti)
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (!akunDebets.isEmpty() && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(
						edit && adminLain && pembayaranPengadaanMasterAssetDetail.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pembayaranPengadaanMasterAssetDetail.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pembayaranPengadaanMasterAssetDetail);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where pembayaran_pengadaan_master_asset_detail="
												+ pembayaranPengadaanMasterAssetDetail.getId() + " and closing is null")
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
						edit && pembayaranPengadaanMasterAssetDetail.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PEMBAYARAN_TAGIHAN);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								try {

									Akun akunDebet1 = pembayaranPengadaanMasterAssetDetail
											.getPenerimaanPengadaanMasterAsset() == null
											|| pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
													.getJenisPenerimaanBarang() == null
															? null
															: pembayaranPengadaanMasterAssetDetail
																	.getPenerimaanPengadaanMasterAsset()
																	.getJenisPenerimaanBarang().getAkunHutangPenyedia();

									if (pembayaranPengadaanMasterAssetDetail.getPembayaranPengadaanMasterAsset() != null
											&& pembayaranPengadaanMasterAssetDetail.getPembayaranPengadaanMasterAsset()
													.getPenyedia() != null
											&& pembayaranPengadaanMasterAssetDetail.getPembayaranPengadaanMasterAsset()
													.getPenyedia().getAkunUtang() != null) {
										akunDebet1 = pembayaranPengadaanMasterAssetDetail
												.getPembayaranPengadaanMasterAsset().getPenyedia().getAkunUtang();
									}

									List<Akun> akunDebets = new ArrayList<Akun>();

									Double nilai = pembayaranPengadaanMasterAssetDetail.getDibayar();
									List<Double> nilais = new ArrayList<Double>();

									if (pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null
											&& pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
													.getPemesananPengadaanMasterAsset() != null
											&& pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
													.getPemesananPengadaanMasterAsset()
													.getJenisPemesananPengadaanAsset() != null
											&& pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
													.getPemesananPengadaanMasterAsset()
													.getJenisPemesananPengadaanAsset().getAkunUtangDariAnggaran()) {

										List<PemesananPengadaanMasterAssetDetail> pemesananPengadaanMasterAssetDetails = session
												.createCriteria(PemesananPengadaanMasterAssetDetail.class)
												.add(Restrictions.eq("pemesananPengadaanMasterAsset",
														pembayaranPengadaanMasterAssetDetail
																.getPenerimaanPengadaanMasterAsset()
																.getPemesananPengadaanMasterAsset()))
												.list();

										for (PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail : pemesananPengadaanMasterAssetDetails) {
											try {

												Double j = (pemesananPengadaanMasterAssetDetail.getJumlah()
														* pemesananPengadaanMasterAssetDetail.getHargaBeli())
														- ((pemesananPengadaanMasterAssetDetail.getHargaPotongan()
																/ 100.0)
																* (pemesananPengadaanMasterAssetDetail.getJumlah()
																		* pemesananPengadaanMasterAssetDetail
																				.getHargaBeli()));

												Akun akun = pemesananPengadaanMasterAssetDetail
														.getPermintaanPengadaanMasterAssetDetail()
														.getPermintaanPengadaanMasterAsset().getAkun();

												if (akun != null) {
													akunDebets.add(akun);
													nilais.add(j);
												}

											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingPembayaranAction.java:1215");
												// Detail anggaran tidak memiliki akun; lewati
											}
										}
									} else {
										akunDebets.add(akunDebet1);
										nilais.add(nilai);
									}

									Akun akunKredit = pembayaranPengadaanMasterAssetDetail
											.getPembayaranPengadaanMasterAsset().getJenisPembayaranBarang() == null
													? null
													: pembayaranPengadaanMasterAssetDetail
															.getPembayaranPengadaanMasterAsset()
															.getJenisPembayaranBarang().getAkun();

									if (pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer() != null
											&& pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer()
													.getProsesTransfer() != null
											&& pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer()
													.getProsesTransfer().getCaraPembayaranTransfer() != null
											&& pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer()
													.getTransfer()
											&& pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer()
													.getProsesTransfer().getCaraPembayaranTransfer()
													.getAkun() != null) {
										akunKredit = pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer()
												.getProsesTransfer().getCaraPembayaranTransfer().getAkun();
									}

									if (pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer() != null
											&& pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer()
													.getProsesTransfer() != null
											&& pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer()
													.getProsesTransfer().getCaraPembayaranTransfer() != null
											&& pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer()
													.getTransitori()
											&& pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer()
													.getProsesTransfer().getCaraPembayaranTransfer()
													.getAkunTransitori() != null) {
										akunKredit = pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer()
												.getProsesTransfer().getCaraPembayaranTransfer().getAkunTransitori();
									}

									if (!akunDebets.isEmpty() && akunKredit != null) {
										Boolean apakahUangMasuk = true;

										String ket = "";
										try {

											ket = "Pembayaran terhadap tagihan \""
													+ (pembayaranPengadaanMasterAssetDetail
															.getPenerimaanPengadaanMasterAsset().getKodeTagihan()
															+ "-"
															+ pembayaranPengadaanMasterAssetDetail
																	.getPenerimaanPengadaanMasterAsset().getPenyedia()
																	.getNama()
															+ "-"
															+ pembayaranPengadaanMasterAssetDetail
																	.getPenerimaanPengadaanMasterAsset()
																	.getKeterangan())
													+ "\" sebanyak " + Common.numberFormat.get()
															.format(pembayaranPengadaanMasterAssetDetail.getDibayar());

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}

										try {

											Akun akunDenda = null;
											Akun akunPiutangDenda = null;
											Double denda = 0.0;

											session.getTransaction().begin();
											if (nilai > 0.1) {
												CommonAkunting.saveTransaksi(akunDebets.toArray(new Akun[] {}),
														new Akun[] { akunKredit }, akunDenda, akunPiutangDenda,
														postingHistory, apakahUangMasuk, ket,
														pembayaranPengadaanMasterAssetDetail.getTanggalTransaksi(),
														nilais.toArray(new Double[] {}), new Double[] { nilai }, denda,
														pembayaranPengadaanMasterAssetDetail,
														pembayaranPengadaanMasterAssetDetail
																.getPenerimaanPengadaanMasterAsset()
																.getPemesananPengadaanMasterAsset().getSatuanKerja(),
														session);
											} else {
												CommonAkunting.saveTransaksi(new Akun[] { akunKredit },
														akunDebets.toArray(new Akun[] {}), akunDenda, akunPiutangDenda,
														postingHistory, apakahUangMasuk, ket,
														pembayaranPengadaanMasterAssetDetail.getTanggalTransaksi(),
														new Double[] { nilai }, nilais.toArray(new Double[] {}), denda,
														pembayaranPengadaanMasterAssetDetail,
														pembayaranPengadaanMasterAssetDetail
																.getPenerimaanPengadaanMasterAsset()
																.getPemesananPengadaanMasterAsset().getSatuanKerja(),
														session);
											}
											session.getTransaction().commit();
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}

										pembayaranPengadaanMasterAssetDetail.setPostingHistory(postingHistory);
										session.getTransaction().begin();
										session.update(pembayaranPengadaanMasterAssetDetail);
										session.getTransaction().commit();
									}
								} catch (Exception e) {
									// Error posting manual per baris; tampilkan bila admin
									Common.tampilErrorJikaAdmin(e);
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
	 * <b>Tujuan:</b> Membangun objek {@link Criteria} Hibernate yang merepresentasikan
	 * semua kondisi filter pencarian yang aktif untuk daftar detail pembayaran
	 * pengadaan aset. Kriteria ini digunakan baik untuk paging maupun pengambilan
	 * data halaman.
	 *
	 * <p><b>Cara kerja:</b> Membuat kriteria pada entitas
	 * {@link PembayaranPengadaanMasterAssetDetail} dengan kondisi-kondisi berikut:
	 * <ol>
	 *   <li>Filter status posting: {@code postingHistory IS NULL} bila
	 *       {@link #searchtampil} tercentang; {@code postingHistory IS NOT NULL}
	 *       bila {@link #searchtelahtampil} tercentang; keduanya tidak tercentang
	 *       = tidak ada filter.</li>
	 *   <li>Join alias ke {@code pembayaranPengadaanMasterAsset}.</li>
	 *   <li>Join alias LEFT ke {@code daftarPengajuanTransfer}.</li>
	 *   <li>Filter validitas transaksi: harus ada {@code jenisPembayaranBarang}
	 *       ATAU ada proses transfer yang sudah ditransfer (transfer=true).</li>
	 *   <li>Filter nilai: {@code dibayar != 0} dan tidak null.</li>
	 *   <li>Filter tanggal transaksi antara {@link #tglMulai} dan
	 *       {@link #tglSampai} (null-safe: bila tanggal null atau kosong,
	 *       filter diabaikan via SQL "1=1").</li>
	 *   <li>Filter pemilik aset bila combo pemilik dipilih.</li>
	 *   <li>Filter ruang bila banbox ruang dipilih.</li>
	 *   <li>Filter kode/nama pembayaran (LIKE anywhere).</li>
	 *   <li>Bila {@code order} true, tambahkan ORDER BY id DESC.</li>
	 * </ol>
	 *
	 * <p><b>Parameter:</b>
	 * @param order {@code true} untuk menyertakan ORDER BY (query data);
	 *              {@code false} untuk query count paging
	 * @return {@link Criteria} Hibernate yang siap dieksekusi
	 *
	 * <p><b>Pemeliharaan:</b> Kondisi filter validitas transaksi (OR jenisPembayaranBarang
	 * / transfer) adalah aturan bisnis penting: hanya transaksi yang memiliki
	 * akun yang valid yang ditampilkan untuk diposting. Jangan hapus kondisi ini.
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PembayaranPengadaanMasterAssetDetail.class)

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(!searchtelahtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNotNull("postingHistory"))

				.createAlias("pembayaranPengadaanMasterAsset", "pembayaranPengadaanMasterAsset")

				.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.isNotNull("pembayaranPengadaanMasterAsset.jenisPembayaranBarang"),

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
								: Restrictions.eq("pembayaranPengadaanMasterAsset.pemilikAsset",
										searchpemilikAsset.getSelectedItem().getValue()))

				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pembayaranPengadaanMasterAsset.ruang", searchruang.getAttribute("ruang"))))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("pembayaranPengadaanMasterAsset.kode", searchkode.getValue(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("pembayaranPengadaanMasterAsset.nama", searchkode.getValue(),
										MatchMode.ANYWHERE)));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Memuat data grid secara langsung tanpa progress bar,
	 * digunakan sebagai implementasi inti pencarian yang dipanggil dari dalam
	 * {@link #loadDataDenganProgressPosting} setelah progress bar ditampilkan.
	 *
	 * <p><b>Cara kerja:</b> Menginisialisasi paging dengan total record dari
	 * {@link #initCriteria}, kemudian mengambil halaman aktif dengan batas
	 * {@code Common.ROWS_COUNT_ON_PAGE}. Hasil dimasukkan ke model
	 * {@link SimpleListModel} dan diset ke grid dengan renderer
	 * {@link PembayaranPengadaanMasterAssetRenderer}.
	 *
	 * <p><b>Parameter:</b>
	 * @param event event ZK pemicu (boleh null)
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini bersifat private karena hanya boleh
	 * dipanggil melalui {@link #loadDataDenganProgressPosting} untuk memastikan
	 * progress bar selalu ditampilkan dengan benar.
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PembayaranPengadaanMasterAssetDetail> pembayaranPengadaanMasterAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pembayaranPengadaanMasterAsset);
		grid.setRowRenderer(new PembayaranPengadaanMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Titik masuk publik untuk memuat ulang data grid. Mendelegasikan
	 * ke {@link #loadDataDenganProgressPosting} agar progress bar selalu
	 * ditampilkan saat data dimuat.
	 *
	 * <p><b>Cara kerja:</b> Delegator satu baris ke
	 * {@link #loadDataDenganProgressPosting}. Dipanggil dari paging listener,
	 * setelah posting/pembatalan individual, dan dari callback batch posting.
	 *
	 * <p><b>Parameter:</b>
	 * @param event event ZK pemicu (boleh null bila dipanggil programatik)
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Flag yang menandakan apakah proses loading data posting jurnal sedang
	 * berjalan. Digunakan untuk mencegah reload bersamaan yang bisa menyebabkan
	 * kondisi balapan (race condition) pada grid.
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag yang menandakan adanya permintaan reload yang diterima saat loading
	 * sedang berjalan. Bila true, reload akan dijalankan ulang segera setelah
	 * loading selesai.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * <b>Tujuan:</b> Memuat data grid dengan menampilkan progress bar
	 * ({@code PostingJurnalLoadingUtil}) selama proses berlangsung. Menangani
	 * kondisi reload bersamaan dengan mekanisme antrian sederhana menggunakan
	 * flag {@link #postingJurnalLoadingAktif} dan {@link #postingJurnalReloadTertunda}.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Bila loading sedang berjalan ({@link #postingJurnalLoadingAktif}
	 *       true), tandai {@link #postingJurnalReloadTertunda} dan return
	 *       dengan pesan "akan dimuat ulang setelah selesai".</li>
	 *   <li>Set {@link #postingJurnalLoadingAktif} true dan tampilkan progress
	 *       bar awal (7%).</li>
	 *   <li>Jadwalkan timer default untuk menjalankan
	 *       {@link #onSearchDefaultTanpaProgress} dengan progress update (48%
	 *       saat fetch, 92% saat merapikan).</li>
	 *   <li>Di blok {@code finally}: reset flag, lalu bila ada reload tertunda,
	 *       jadwalkan timer lagi untuk reload ulang. Bila tidak ada, tampilkan
	 *       progress selesai (100%).</li>
	 * </ol>
	 *
	 * <p><b>Parameter:</b>
	 * @param event event ZK asli yang memicu reload (boleh null); diteruskan
	 *              ke {@link #onSearchDefaultTanpaProgress} dan ke reload
	 *              tertunda bila ada
	 *
	 * <p><b>Threading:</b> Berjalan sepenuhnya di event thread ZK. Penggunaan
	 * timer ZK memastikan UI dapat di-refresh antara langkah-langkah loading.
	 * Flag boolean tidak perlu sinkronisasi karena ZK menjamin satu event
	 * per sesi diproses secara berurutan.
	 *
	 * <p><b>Pemeliharaan:</b> Angka persentase progress (7, 48, 92, 96, 100)
	 * bersifat kosmetik; sesuaikan bila ingin membagi proses menjadi lebih
	 * banyak langkah. Pastikan blok {@code finally} selalu mengeksekusi reset
	 * flag agar loading tidak macet bila terjadi exception.
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
