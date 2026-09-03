package ais.action.master.asset;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import ais.action.master.asset.util.AssetUtil;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.PemilikAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
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
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h3>PostingPengadaanAction</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan Action ZKoss (GenericAutowireComposer) yang mengelola proses posting
 * jurnal akuntansi untuk transaksi penerimaan barang/jasa dalam alur pengadaan aset. Halaman
 * ini diakses oleh staf akuntansi atau pejabat yang berwenang untuk mengkonversi catatan
 * penerimaan aset (SaldoAwalMasterAsset) menjadi entri jurnal debet-kredit yang valid dalam
 * sistem akuntansi terintegrasi. Posting berarti transaksi pengadaan aset secara resmi
 * dicatat dalam buku besar akuntansi melalui mekanisme {@link GrupTransaksi} dan
 * {@link PostingHistory}.<br><br>
 *
 * <b>Cara kerja:</b><br>
 * Setelah komponen ZUL dimuat, {@link #doAfterCompose(Component)} menginisialisasi filter,
 * hak akses, dan memuat data pertama kali. Pengguna dapat melihat daftar transaksi pengadaan
 * yang sudah disetujui dan siap di-posting. Untuk setiap baris, sistem menghitung akun
 * debet (berdasarkan kelompok aset - biaya penyusutan atau akun transaksi) dan akun kredit
 * (hutang penyedia atau akun uang muka). Jika ada PPh, akun pajak ditambahkan ke sisi kredit.
 * Sistem mendukung dua mode operasi:<br>
 * - <b>Mode individual</b>: tombol "Posting Data" per baris di {@link SaldoAwalMasterAssetDetailRenderer}
 *   memposting satu transaksi pada satu waktu, dieksekusi di ZKoss event-thread.<br>
 * - <b>Mode massal</b>: tombol "Posting Semua" melalui {@link #onPostingSemua(Event)} memposting
 *   seluruh transaksi yang lolos filter dalam satu batch menggunakan thread latar terpisah
 *   dengan progress bar.<br>
 * Selain itu, kelas mendukung parameter URL {@code sudah_posting} untuk menampilkan tampilan
 * terfokus pada data yang sudah atau belum di-posting tanpa filter tambahan.<br><br>
 *
 * <b>Threading:</b><br>
 * Terdapat perbedaan threading yang kritis antara dua mode posting:<br>
 * - Mode individual: berjalan sepenuhnya di ZKoss event-thread melalui timer default.<br>
 * - Mode massal ({@link #onPostingSemua}): membuat thread Java baru ({@code new Thread().start()})
 *   yang berjalan di latar. Thread ini menggunakan {@link HibernateUtil#currentNativeSession()}
 *   untuk mendapatkan sesi Hibernate baru yang independen dan menutupnya dengan
 *   {@link HibernateUtil#closeSession()} setelah selesai. Mekanisme progress bar menggunakan
 *   label ZKoss yang di-update dari thread latar, yang dimungkinkan oleh {@code displayLoadBar}.<br><br>
 *
 * <b>Pemeliharaan:</b><br>
 * Logika penentuan akun debet-kredit (asset fix vs non-fix, uang muka/DP, PPh) duplikasi
 * antara {@link SaldoAwalMasterAssetDetailRenderer#render} (untuk preview) dan proses
 * posting aktual di {@link #onPostingSemua} maupun handler individual. Jika aturan akuntansi
 * berubah, ketiga lokasi ini harus diperbarui secara bersamaan. Flag {@code postingJurnalLoadingAktif}
 * dan {@code postingJurnalReloadTertunda} digunakan untuk mencegah reload bersamaan yang dapat
 * menyebabkan tampilan tidak konsisten - pola ini penting dipertahankan saat mengubah logika
 * refresh data.
 *
 * @author AIS Team
 * @version 1.0
 * @see SaldoAwalMasterAsset
 * @see PostingHistory
 * @see GrupTransaksi
 * @see CommonAkunting
 */
public class PostingPengadaanAction extends GenericAutowireComposer {

	/**
	 * Serial version UID untuk serialisasi kelas ini. Nilai ini tidak boleh diubah
	 * kecuali ada perubahan struktur field yang tidak kompatibel ke belakang.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar transaksi pengadaan yang dapat di-posting. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman pada grid utama. */
	private Paging paging;

	/** Kotak teks pencarian berdasarkan kode transaksi pengadaan. */
	private Textbox searchkode;

	/** Combobox filter berdasarkan pemilik aset. */
	private Combobox searchpemilikAsset;

	/** Combobox filter berdasarkan lokasi aset. */
	private Combobox searchlokasi;

	/** Banbox untuk memilih ruang/lokasi spesifik sebagai filter tambahan. */
	private AmbilDataRuangBanbox searchruang;

	/** Checkbox filter untuk menampilkan transaksi yang belum di-posting. */
	private MyCheckboxConfig searchtampil;

	/** Checkbox filter untuk menampilkan transaksi yang sudah di-posting. */
	private MyCheckboxConfig searchtelahtampil;

	/** Flag yang menunjukkan apakah pengguna saat ini memiliki hak ubah (UPDATE). */
	private boolean edit = false;

	/** Tombol toolbar untuk memulai proses posting semua transaksi yang memenuhi filter. */
	private MyToolbarbuttonConfig sent;

	/**
	 * Flag yang menunjukkan apakah pengguna memiliki hak admin atau hak APPROVE.
	 * Digunakan untuk menampilkan tombol "Batalkan Posting Data" pada setiap baris.
	 */
	public boolean adminLain;

	/** Datebox batas awal rentang tanggal persetujuan untuk filter data. */
	private MyDatebox tglMulai;

	/** Datebox batas akhir rentang tanggal persetujuan untuk filter data. */
	private MyDatebox tglSampai;

	/** Pengguna yang sedang login, digunakan untuk identifikasi pada proses posting. */
	private Tbmuser tbmuser;

	/** Komponen North yang berisi panel filter; disembunyikan dalam mode {@code sudah_posting}. */
	private North filter;

	/** Baris dalam grid yang berisi kontrol spesifik untuk mode {@code sudah_posting}. */
	private Row rowPosting;

	/**
	 * Nilai dari parameter URL {@code sudah_posting}. Jika {@code true}, hanya tampilkan
	 * yang sudah di-posting; jika {@code false}, hanya yang belum; jika {@code null},
	 * gunakan checkbox filter normal.
	 */
	private Boolean sudah_posting = null;

	/**
	 * Dipanggil oleh ZKoss sebelum komposisi komponen dimulai, sebagai interceptor
	 * awal untuk validasi keamanan. Metode ini memanggil {@link Common#doCheckSecurity()}
	 * yang memeriksa token sesi dan memastikan request berasal dari sesi yang valid.
	 * Jika pemeriksaan gagal, pengguna diarahkan ke halaman logoff sebelum komponen
	 * ZUL selesai dibentuk.
	 *
	 * <p><b>Tujuan:</b> Mencegah akses tidak sah ke halaman posting pengadaan aset
	 * sebelum komponen ZUL sepenuhnya terbentuk dan data sensitif dimuat.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} untuk validasi sesi,
	 * kemudian mendelegasikan ke {@code super.doBeforeCompose} untuk proses autowiring
	 * standar ZKoss.</p>
	 *
	 * <p><b>Penanganan error:</b> Jika validasi keamanan gagal, request akan diarahkan
	 * ke halaman logoff. Exception dari super diteruskan ke ZKoss framework.</p>
	 *
	 * @param page     halaman ZKoss yang sedang dimuat.
	 * @param parent   komponen induk dalam hierarki komponen ZKoss.
	 * @param compInfo metadata komponen yang akan dibuat.
	 * @return {@code ComponentInfo} dari super yang diteruskan ke framework ZKoss.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Lifecycle callback ZKoss yang dipanggil setelah seluruh komponen halaman selesai
	 * dirakit. Metode ini merupakan titik inisialisasi utama untuk halaman posting
	 * transaksi pengadaan aset.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi bahasa, memvalidasi sesi pengguna, menerapkan
	 * parameter URL, menyiapkan filter dan combobox, memuat data awal dengan progress
	 * bar, serta mendaftarkan listener paging dan filter lanjut.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Inisialisasi bahasa menggunakan {@link Common#initLaguage()}.</li>
	 *   <li>Validasi sesi: jika {@code usersTemp} null atau tidak ada hak READ,
	 *       arahkan ke logoff.</li>
	 *   <li>Ambil pengguna saat ini dan parameter URL {@code sudah_posting}.</li>
	 *   <li>Jika {@code sudah_posting} tidak null, sembunyikan panel filter normal
	 *       dan tampilkan baris posting khusus.</li>
	 *   <li>Set rentang tanggal default: 6 bulan ke belakang hingga hari ini (readonly).</li>
	 *   <li>Terapkan parameter URL {@code mulai} dan {@code sampai} jika tersedia,
	 *       dan nonaktifkan datebox terkait.</li>
	 *   <li>Tentukan flag {@code adminLain} (admin atau APPROVE) untuk kontrol visibilitas
	 *       tombol batalkan posting.</li>
	 *   <li>Isi combobox pemilik aset dan lokasi dengan data aktif.</li>
	 *   <li>Kunci lokasi berdasarkan konfigurasi menggunakan {@link LokasiAction#kunciLokasi}.</li>
	 *   <li>Muat data awal menggunakan {@link #loadDataDenganProgressPosting(Event)}.</li>
	 *   <li>Daftarkan listener paging dan setup filter lanjut.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari parsing tanggal URL ditangkap secara
	 * lokal dan ditampilkan via {@link Common#tampilErrorJikaAdmin}. Exception lainnya
	 * diteruskan ke ZKoss framework.</p>
	 *
	 * @param comp komponen root hasil komposisi ZKoss.
	 * @throws Exception jika terjadi error pada proses inisialisasi komponen super.
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
		if (sent == null) return;
		if (sent != null) { sent.setVisible(edit); }

		Common.insertComboDanSemua(searchpemilikAsset, new String[] { "nama", "id" }, "keterangan", PemilikAsset.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(searchlokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (session.getAttribute("Lokasi") != null) {
			Common.selectComboItem(searchlokasi, session.getAttribute("Lokasi"));
			searchlokasi.setDisabled(true);
			session.removeAttribute("Lokasi");
		}
		LokasiAction.kunciLokasi(searchlokasi);

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
	 * Event handler yang dipanggil ketika pengguna mengklik tombol "Batalkan Posting Semua".
	 * Metode ini membatalkan posting untuk seluruh transaksi pengadaan yang lolos filter
	 * saat ini dan memiliki riwayat posting, dengan konfirmasi dialog terlebih dahulu.
	 *
	 * <p><b>Tujuan:</b> Memungkinkan pembatalan massal status posting transaksi pengadaan
	 * aset, misalnya ketika ditemukan kesalahan dalam batch posting sebelumnya.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menampilkan dialog konfirmasi menggunakan {@link MyMessageboxConfig}.</li>
	 *   <li>Jika dikonfirmasi (OK), mengambil semua {@link SaldoAwalMasterAsset} yang
	 *       sudah di-posting ({@code postingHistory} tidak null) sesuai filter aktif.</li>
	 *   <li>Untuk setiap entitas: mengatur {@code postingHistory} ke null, menyimpan
	 *       perubahan, dan menghapus entri {@link GrupTransaksi} terkait dari skema
	 *       {@code akunting.grup_transaksi} menggunakan SQL native dengan kondisi
	 *       {@code closing is null} (hanya entri yang belum ditutup).</li>
	 *   <li>Memuat ulang data menggunakan timer default untuk menghindari race condition
	 *       ZKoss.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari operasi database diteruskan ke caller
	 * (ZKoss framework). Pembatalan hanya dilakukan jika pengguna mengklik OK pada dialog.</p>
	 *
	 * @param event event ZKoss dari komponen yang memanggil handler ini.
	 * @throws Exception jika terjadi error saat mengakses database untuk pembatalan massal.
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi pengadaan ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<SaldoAwalMasterAsset> saldoAwalMasterAssetDetails = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (SaldoAwalMasterAsset saldoAwalMasterAsset : saldoAwalMasterAssetDetails) {
								saldoAwalMasterAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(saldoAwalMasterAsset);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where saldo_awal_master_asset="
														+ saldoAwalMasterAsset.getId() + " and closing is null")
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
	 * Event handler yang dipanggil ketika pengguna mengklik tombol "Posting Semua Transaksi".
	 * Metode ini membuka dialog modal untuk memasukkan tanggal dan keterangan posting,
	 * kemudian memproses seluruh transaksi pengadaan yang belum di-posting dalam thread
	 * latar dengan progress bar real-time.
	 *
	 * <p><b>Tujuan:</b> Memungkinkan posting massal jurnal akuntansi untuk semua transaksi
	 * pengadaan aset yang sudah disetujui dan belum di-posting, mengurangi waktu operasi
	 * dibandingkan posting individual satu per satu.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Membuat jendela modal baru dengan formulir tanggal, nama poster, dan keterangan.</li>
	 *   <li>Setelah pengguna mengklik Simpan dan mengkonfirmasi, membuat entitas
	 *       {@link PostingHistory} baru dengan jenis
	 *       {@code JENIS_PENERIMAAN_TAGIHAN_BARANG_JASA} dan menyimpannya ke database.</li>
	 *   <li>Membuat thread Java baru yang:
	 *     <ul>
	 *       <li>Membuka sesi native Hibernate baru.</li>
	 *       <li>Mengambil semua {@link SaldoAwalMasterAsset} yang belum di-posting
	 *           ({@code postingHistory} null) sesuai filter saat ini.</li>
	 *       <li>Untuk setiap aset, menghitung akun debet (asset fix → akunTransaksi;
	 *           non-fix → akunBiayaPenyusutan) dan akun kredit (hutang penyedia atau
	 *           akun uang muka jika ada DP).</li>
	 *       <li>Jika ada PPh pada detail, menambahkan akun pajak ke sisi kredit.</li>
	 *       <li>Memanggil {@link CommonAkunting#saveTransaksi} untuk membuat jurnal,
	 *           menentukan arah debet/kredit berdasarkan apakah nilai positif atau negatif.</li>
	 *       <li>Memperbarui progress bar label dengan persentase kemajuan.</li>
	 *       <li>Menetapkan {@code postingHistory} pada setiap aset dan menyimpannya.</li>
	 *       <li>Menutup sesi Hibernate setelah semua selesai.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Setelah thread selesai, menampilkan dialog sukses dan memuat ulang grid.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading:</b> Proses posting massal berjalan di thread latar Java yang terpisah
	 * dari ZKoss event-thread. Sesi Hibernate dibuat dan ditutup secara independen di thread
	 * ini. Update label progress dilakukan langsung dari thread latar, yang dimungkinkan
	 * oleh mekanisme push ZKoss.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception per-aset dalam loop ditangkap secara lokal
	 * dan diabaikan (blok catch kosong) agar satu aset yang gagal tidak menghentikan
	 * batch keseluruhan. Exception pada perhitungan jurnal ditampilkan via
	 * {@link Common#tampilErrorJikaAdmin}.</p>
	 *
	 * @param event event ZKoss dari tombol "Posting Semua" pada toolbar.
	 * @throws Exception jika terjadi error saat membangun jendela modal formulir posting.
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Transaksi Pengadaan");
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
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal dan pilih tanggal dari kalender; (2) Pastikan tanggal berada dalam periode akuntansi yang aktif; (3) ulangi proses posting pengadaan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi pengadaan ?", "Pertanyaan",
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
											MyMessageboxConfig.show("Posting transaksi pengadaan berhasil dilakukan",
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
													PostingHistory.JENIS_PENERIMAAN_TAGIHAN_BARANG_JASA);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<SaldoAwalMasterAsset> saldoAwalMasterAssets = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (SaldoAwalMasterAsset saldoAwalMasterAsset : saldoAwalMasterAssets) {

												SatuanKerja satuanKerja = saldoAwalMasterAsset.getSatuanKerja();

												try {

													List<SaldoAwalMasterAssetDetail> saldoAwalMasterAssetDetails = HibernateUtil
															.currentSession()
															.createCriteria(SaldoAwalMasterAssetDetail.class)
															.add(Restrictions.eq("saldoAwal", saldoAwalMasterAsset))
															.list();
													List<Akun> akunKredit = new ArrayList<Akun>();
													List<Double> nilais = new ArrayList<Double>();
													Double totalPph = 0.0;
													Map<Long, Double> akunsDebetsMap = new HashMap<Long, Double>();
													for (SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail : saldoAwalMasterAssetDetails) {
														Akun akunDebet = null;
														if (saldoAwalMasterAssetDetail.getMasterAsset()
																.getKelompokAsset() != null
																&& !saldoAwalMasterAssetDetail.getMasterAsset()
																		.getKelompokAsset().getMerupakanAssetFix()) {
															akunDebet = AssetUtil
																	.ambilDataAkun(
																			saldoAwalMasterAssetDetail.getMasterAsset()
																					.akunBiayaPenyusutanEfektif(),
																			satuanKerja);
														} else {
															akunDebet = AssetUtil.ambilDataAkun(
																	saldoAwalMasterAssetDetail.getMasterAsset()
																			.akunTransaksiEfektif(),
																	satuanKerja);
														}
														if (akunDebet != null) {
															Double n = akunsDebetsMap.get(akunDebet.getId());
															if (n == null) {
																n = 0.0;
															}
															n += saldoAwalMasterAssetDetail.getHargaTotal();

															akunsDebetsMap.put(akunDebet.getId(), n);
														}

														if (saldoAwalMasterAssetDetail.getJenisPajakBarang() != null
																&& saldoAwalMasterAssetDetail.getJenisPajakBarang()
																		.getAkun() != null) {
															akunKredit.add(saldoAwalMasterAssetDetail
																	.getJenisPajakBarang().getAkun());

															Double dpp = (saldoAwalMasterAssetDetail.getJumlah()
																	* saldoAwalMasterAssetDetail.getHarga());
															Double pph = ((saldoAwalMasterAssetDetail.getPersenPph()
																	/ 100.0) * dpp);
															nilais.add(pph);
															totalPph += pph;
														}
													}

													Akun akunKredit1 = saldoAwalMasterAsset
															.getPenerimaanPengadaanMasterAsset() == null
															|| saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
																	.getJenisPenerimaanBarang() == null
																			? null
																			: saldoAwalMasterAsset
																					.getPenerimaanPengadaanMasterAsset()
																					.getJenisPenerimaanBarang()
																					.getAkunHutangPenyedia();

													if (saldoAwalMasterAsset != null
															&& saldoAwalMasterAsset.getPenyedia() != null
															&& saldoAwalMasterAsset.getPenyedia()
																	.getAkunUtang() != null) {
														akunKredit1 = saldoAwalMasterAsset.getPenyedia().getAkunUtang();
													}

													Double nilai = saldoAwalMasterAsset.getNilai();

													if (saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() != null
															&& saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
																	.getPemesananPengadaanMasterAsset() != null
															&& saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
																	.getPemesananPengadaanMasterAsset()
																	.getJenisPemesananPengadaanAsset() != null
															&& saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
																	.getPemesananPengadaanMasterAsset()
																	.getDptotal() > 0.1) {

														Double t = nilai - saldoAwalMasterAsset
																.getPenerimaanPengadaanMasterAsset()
																.getPemesananPengadaanMasterAsset().getDptotal();
														if (t > 0.1) {
															akunKredit.add(akunKredit1);
															nilais.add(t);
														}
														akunKredit.add(
																saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
																		.getPemesananPengadaanMasterAsset()
																		.getJenisPemesananPengadaanAsset().getAkunDp());
														nilais.add(
																saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
																		.getPemesananPengadaanMasterAsset().getDptotal()
																		- totalPph);

													} else if (nilai > 0.1) {
														akunKredit.add(akunKredit1);
														nilais.add(nilai - totalPph);
													}

													if (!akunsDebetsMap.isEmpty() && !akunKredit.isEmpty()) {
														Boolean apakahUangMasuk = true;

														String ket = "";
														try {

															ket = (saldoAwalMasterAsset.getKode())
																	+ saldoAwalMasterAsset.getKeterangan();

														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														}

														label.setValue(
																ket + " ("
																		+ Common.numberFormat.get().format(rowIndex * 100.0
																				/ saldoAwalMasterAssetDetails.size())
																		+ " %)");

														try {

															Akun akunDenda = null;
															Akun akunPiutangDenda = null;
															Double denda = 0.0;

															session.getTransaction().begin();
															List<Akun> akunDebet = new ArrayList<Akun>();
															List<Double> nilaiDebets = new ArrayList<Double>();

															for (Long key : akunsDebetsMap.keySet()) {
																Akun akun = (Akun) ConstantValues
																		.ambil(Akun.class.getName(), key);
																if (akun != null) {
																	akunDebet.add(akun);
																	nilaiDebets.add(akunsDebetsMap.get(key));
																}
															}

															if (nilai > 0.1) {
																CommonAkunting.saveTransaksi(
																		akunDebet.toArray(new Akun[] {}),
																		akunKredit.toArray(new Akun[] {}), akunDenda,
																		akunPiutangDenda, postingHistory,
																		apakahUangMasuk, ket,
																		saldoAwalMasterAsset.getTanggalPersetujuan(),
																		nilaiDebets.toArray(new Double[] {}),
																		nilais.toArray(new Double[] {}), denda,
																		saldoAwalMasterAsset, satuanKerja, session);
															} else {
																CommonAkunting.saveTransaksi(
																		akunKredit.toArray(new Akun[] {}),
																		akunDebet.toArray(new Akun[] {}), akunDenda,
																		akunPiutangDenda, postingHistory,
																		apakahUangMasuk, ket,
																		saldoAwalMasterAsset.getTanggalPersetujuan(),
																		nilais.toArray(new Double[] {}),
																		nilaiDebets.toArray(new Double[] {}), denda,
																		saldoAwalMasterAsset, satuanKerja, session);
															}
															session.getTransaction().commit();
														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														}

														saldoAwalMasterAsset.setPostingHistory(postingHistory);
														session.getTransaction().begin();
														session.update(saldoAwalMasterAsset);
														session.getTransaction().commit();
													}
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingPengadaanAction.java:755");
													// Abaikan error per-item agar batch lainnya tetap diproses
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
	 * Kelas inner yang bertanggung jawab merender setiap baris grid daftar transaksi
	 * pengadaan aset yang siap atau sudah di-posting. Kelas ini mengimplementasikan
	 * {@code MyRowRenderer} dan digunakan oleh komponen {@link MyGrid} utama.
	 *
	 * <p><b>Untuk apa:</b> Menampilkan pratinjau jurnal debet-kredit untuk setiap
	 * {@link SaldoAwalMasterAsset} sebelum atau setelah di-posting, lengkap dengan
	 * informasi satuan kerja, nilai, tanggal persetujuan, status posting, dan tombol
	 * aksi (posting individual dan batalkan posting).</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Metode {@link #render(Row, Object)} membangun komponen UI untuk satu baris:
	 * <ol>
	 *   <li>Kolom kode: Vbox dengan helper revisi dan link SOP jika ada disposisi.</li>
	 *   <li>Kolom satuan kerja: nama satuan kerja dari aset.</li>
	 *   <li>Kolom nilai: nilai total aset dengan format ribuan.</li>
	 *   <li>Kolom tanggal persetujuan: tanggal persetujuan aset.</li>
	 *   <li>Kolom pratinjau jurnal: menghitung akun debet-kredit secara real-time dan
	 *       menampilkan menggunakan {@link GrupTransaksi#tampilkanJurnal}; menampilkan
	 *       pesan error spesifik jika akun debet atau kredit tidak lengkap.</li>
	 *   <li>Kolom status posting: teks "Belum diposting" atau detail riwayat posting
	 *       beserta nomor bukti.</li>
	 *   <li>Kolom toolbar: tombol "Batalkan Posting" (visible jika sudah posting dan
	 *       {@code adminLain}) dan "Posting Data" (visible jika belum posting).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading:</b> Renderer berjalan di ZKoss event-thread. Posting individual
	 * melalui tombol "Posting Data" menggunakan timer default ZKoss, bukan thread latar,
	 * sehingga UI tetap responsif meskipun proses singkat.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Logika perhitungan akun di sini (preview) harus selalu
	 * konsisten dengan logika di {@link #onPostingSemua} (batch) dan handler tombol
	 * "Posting Data" (individual). Perubahan aturan akuntansi harus diterapkan di
	 * ketiga lokasi secara bersamaan.</p>
	 */
	class SaldoAwalMasterAssetDetailRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris grid untuk entitas {@link SaldoAwalMasterAsset} yang diberikan.
		 * Metode ini membangun pratinjau jurnal lengkap dan tombol aksi posting untuk
		 * satu transaksi pengadaan aset.
		 *
		 * <p><b>Tujuan:</b> Memberikan tampilan informatif kepada pengguna tentang jurnal
		 * yang akan dibuat beserta status posting saat ini, sehingga pengguna dapat
		 * memvalidasi sebelum melakukan posting resmi.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Logika utama:
		 * <ol>
		 *   <li>Mengambil semua {@link SaldoAwalMasterAssetDetail} untuk aset ini.</li>
		 *   <li>Menghitung akun debet per detail: aset non-fix menggunakan
		 *       {@code akunBiayaPenyusutan}; aset fix menggunakan {@code akunTransaksi};
		 *       nilai diakumulasi per akun dalam {@code akunsDebetsMap}.</li>
		 *   <li>Menghitung akun kredit: hutang penyedia dari jenis penerimaan, atau
		 *       akun utang penyedia jika tersedia; jika ada uang muka (DP), akun kredit
		 *       dibagi antara hutang penyedia (sisa setelah DP) dan akun uang muka.</li>
		 *   <li>Menambahkan akun PPh ke kredit jika ada pajak pada detail.</li>
		 *   <li>Menampilkan pratinjau jurnal atau pesan error akun tidak ditemukan.</li>
		 *   <li>Mencari nomor bukti dari {@link GrupTransaksi} untuk status posting.</li>
		 *   <li>Tombol "Posting Data" individual membuat {@link PostingHistory} baru,
		 *       mengumpulkan ulang akun debet-kredit dari detail, memanggil
		 *       {@link CommonAkunting#saveTransaksi}, dan memperbarui entitas aset.</li>
		 * </ol>
		 * </p>
		 *
		 * <p><b>Penanganan error:</b> Pesan warning untuk akun yang tidak ditemukan
		 * dikumpulkan dalam string {@code warnings} dan ditampilkan oleh
		 * {@link GrupTransaksi#tampilkanJurnal}. Exception dalam kalkulasi posting
		 * individual ditampilkan via {@link Common#tampilErrorJikaAdmin}.</p>
		 *
		 * @param arg0 baris ZKoss {@link Row} yang akan diisi dengan komponen UI.
		 * @param arg1 objek {@link SaldoAwalMasterAsset} yang akan dirender.
		 * @throws Exception jika terjadi error saat membangun komponen UI atau mengakses database.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final SaldoAwalMasterAsset saldoAwalMasterAsset = (SaldoAwalMasterAsset) arg1;

			final SatuanKerja satuanKerja = saldoAwalMasterAsset.getSatuanKerja();

			Vbox a;
			(a = RevisiHelper.createNewRevisi(SaldoAwalMasterAsset.class, saldoAwalMasterAsset,
					saldoAwalMasterAsset.getKode() == null ? "" : saldoAwalMasterAsset.getKode())).setParent(arg0);
			if (saldoAwalMasterAsset.getDisposisiSop() != null) {
				A aaa;
				(aaa = new A()).setParent(a);
				aaa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aaa, "SOP " + saldoAwalMasterAsset.getDisposisiSop().getKeterangan() + " ("
						+ saldoAwalMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aaa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(saldoAwalMasterAsset.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			new Label(satuanKerja == null ? "" : satuanKerja.getNama()).setParent(arg0);

			Double nilai = saldoAwalMasterAsset.getNilai();

			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

			new Label(Common.dateFormat3.get().format(saldoAwalMasterAsset.getTanggalPersetujuan())).setParent(arg0);

			List<SaldoAwalMasterAssetDetail> saldoAwalMasterAssetDetails = HibernateUtil.currentSession()
					.createCriteria(SaldoAwalMasterAssetDetail.class)
					.add(Restrictions.eq("saldoAwal", saldoAwalMasterAsset)).list();

			List<Akun> akunKredit = new ArrayList<Akun>();
			List<Double> nilais = new ArrayList<Double>();

			String warnings = "";
			Map<Long, Double> akunsDebetsMap = new HashMap<Long, Double>();
			Double totalPph = 0.0;
			for (SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail : saldoAwalMasterAssetDetails) {

				Akun akunDebet = null;
				if (saldoAwalMasterAssetDetail.getMasterAsset().getKelompokAsset() != null
						&& !saldoAwalMasterAssetDetail.getMasterAsset().getKelompokAsset().getMerupakanAssetFix()) {
					akunDebet = AssetUtil.ambilDataAkun(
							saldoAwalMasterAssetDetail.getMasterAsset().akunBiayaPenyusutanEfektif(), satuanKerja);
				}

				else {
					akunDebet = AssetUtil.ambilDataAkun(saldoAwalMasterAssetDetail.getMasterAsset().akunTransaksiEfektif(),
							satuanKerja);
				}

				if (akunDebet != null) {
					Double n = akunsDebetsMap.get(akunDebet.getId());
					if (n == null) {
						n = 0.0;
					}
					n += saldoAwalMasterAssetDetail.getHargaTotal();

					akunsDebetsMap.put(akunDebet.getId(), n);

				} else {
					String s = "Akun \"" + saldoAwalMasterAssetDetail.getMasterAsset().getNama()
							+ "\" belum ditentukan";
					warnings += warnings.isEmpty() ? s : "; " + s;
				}

				if (saldoAwalMasterAssetDetail.getJenisPajakBarang() != null
						&& saldoAwalMasterAssetDetail.getJenisPajakBarang().getAkun() != null) {
					akunKredit.add(saldoAwalMasterAssetDetail.getJenisPajakBarang().getAkun());

					Double dpp = (saldoAwalMasterAssetDetail.getJumlah() * saldoAwalMasterAssetDetail.getHarga());
					Double pph = ((saldoAwalMasterAssetDetail.getPersenPph() / 100.0) * dpp);
					nilais.add(pph);
					totalPph += pph;
				}

			}

			Akun akunKredit1 = saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() == null
					|| saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getJenisPenerimaanBarang() == null
							? null
							: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getJenisPenerimaanBarang()
									.getAkunHutangPenyedia();

			if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getPenyedia() != null
					&& saldoAwalMasterAsset.getPenyedia().getAkunUtang() != null) {
				akunKredit1 = saldoAwalMasterAsset.getPenyedia().getAkunUtang();
			}

			if (saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() != null
					&& saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
							.getPemesananPengadaanMasterAsset() != null
					&& saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()
							.getJenisPemesananPengadaanAsset() != null
					&& saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()
							.getDptotal() > 0.1) {

				Double t = nilai - saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
						.getPemesananPengadaanMasterAsset().getDptotal();
				if (t > 0.1) {
					akunKredit.add(akunKredit1);
					nilais.add(t);
				}
				akunKredit.add(saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
						.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset().getAkunDp());
				nilais.add(saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()
						.getDptotal() - totalPph);

			} else if (nilai > 0.1) {
				akunKredit.add(akunKredit1);
				nilais.add(nilai - totalPph);
			}

			if (!akunsDebetsMap.isEmpty() && !akunKredit.isEmpty()) {

				List<Akun> akunsDebets = new ArrayList<Akun>();
				List<Akun> akunsKredits = new ArrayList<Akun>();

				List<Double> nilaiDebets = new ArrayList<Double>();
				List<Double> nilaiKredits = new ArrayList<Double>();

				for (Long key : akunsDebetsMap.keySet()) {
					Akun akun = (Akun) ConstantValues.ambil(Akun.class.getName(), key);
					if (akun != null) {
						akunsDebets.add(akun);
						nilaiDebets.add(akunsDebetsMap.get(key));
					}
				}

				for (int i = 0; i < akunKredit.size(); i++) {
					Akun akun = akunKredit.get(i);
					Double n = nilais.get(i);

					akunsKredits.add(akun);
					nilaiKredits.add(n);

				}

				GrupTransaksi.tampilkanJurnal(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits, warnings)
						.setParent(arg0);
			} else {
				new Label("Transaksi tidak valid."
						+ (akunsDebetsMap.isEmpty() ? " Debet: " + akunsDebetsMap.keySet() + "."
								: " Akun debet tidak ada.")
						+ (!akunKredit.isEmpty() ? " Kredit: " + akunKredit.toString() + "."
								: " Akun kredit tidak ada."))
						.setParent(arg0);
			}

			String bukti = "";
			Session session = HibernateUtil.currentSession();
			bukti = (String) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.eq("saldoAwalMasterAsset", saldoAwalMasterAsset)).setMaxResults(1)
					.setProjection(Projections.property("kode")).uniqueResult();

			new Label(saldoAwalMasterAsset.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: saldoAwalMasterAsset.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (!akunsDebetsMap.isEmpty() && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && saldoAwalMasterAsset.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								saldoAwalMasterAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(saldoAwalMasterAsset);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where saldo_awal_master_asset ="
														+ saldoAwalMasterAsset.getId() + " and closing is null")
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
				button.setVisible(edit && saldoAwalMasterAsset.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PENERIMAAN_TAGIHAN_BARANG_JASA);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								List<SaldoAwalMasterAssetDetail> saldoAwalMasterAssetDetails = session
										.createCriteria(SaldoAwalMasterAssetDetail.class)
										.add(Restrictions.eq("saldoAwal", saldoAwalMasterAsset)).list();

								Map<Long, Double> akunsDebetsMap = new HashMap<Long, Double>();
								Double totalPph = 0.0;
								List<Akun> akunKredit = new ArrayList<Akun>();
								List<Double> nilais = new ArrayList<Double>();
								for (SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail : saldoAwalMasterAssetDetails) {
									Akun akunDebet = null;
									if (saldoAwalMasterAssetDetail.getMasterAsset().getKelompokAsset() != null
											&& !saldoAwalMasterAssetDetail.getMasterAsset().getKelompokAsset()
													.getMerupakanAssetFix()) {
										akunDebet = AssetUtil.ambilDataAkun(
												saldoAwalMasterAssetDetail.getMasterAsset().akunBiayaPenyusutanEfektif(),
												satuanKerja);
									} else {
										akunDebet = AssetUtil.ambilDataAkun(
												saldoAwalMasterAssetDetail.getMasterAsset().akunTransaksiEfektif(),
												satuanKerja);
									}
									if (akunDebet != null) {
										Double n = akunsDebetsMap.get(akunDebet.getId());
										if (n == null) {
											n = 0.0;
										}
										n += saldoAwalMasterAssetDetail.getHargaTotal();

										akunsDebetsMap.put(akunDebet.getId(), n);
									}

									if (saldoAwalMasterAssetDetail.getJenisPajakBarang() != null
											&& saldoAwalMasterAssetDetail.getJenisPajakBarang().getAkun() != null) {
										akunKredit.add(saldoAwalMasterAssetDetail.getJenisPajakBarang().getAkun());

										Double dpp = (saldoAwalMasterAssetDetail.getJumlah()
												* saldoAwalMasterAssetDetail.getHarga());
										Double pph = ((saldoAwalMasterAssetDetail.getPersenPph() / 100.0) * dpp);
										nilais.add(pph);
										totalPph += pph;
									}
								}

								Akun akunKredit1 = saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() == null
										|| saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
												.getJenisPenerimaanBarang() == null ? null
														: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
																.getJenisPenerimaanBarang().getAkunHutangPenyedia();

								if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getPenyedia() != null
										&& saldoAwalMasterAsset.getPenyedia().getAkunUtang() != null) {
									akunKredit1 = saldoAwalMasterAsset.getPenyedia().getAkunUtang();
								}

								Double nilai = saldoAwalMasterAsset.getNilai();

								if (saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() != null
										&& saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
												.getPemesananPengadaanMasterAsset() != null
										&& saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
												.getPemesananPengadaanMasterAsset()
												.getJenisPemesananPengadaanAsset() != null
										&& saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
												.getPemesananPengadaanMasterAsset().getDptotal() > 0.1) {

									Double t = nilai - saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
											.getPemesananPengadaanMasterAsset().getDptotal();
									if (t > 0.1) {
										akunKredit.add(akunKredit1);
										nilais.add(t);
									}
									akunKredit.add(saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
											.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset()
											.getAkunDp());
									nilais.add(saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
											.getPemesananPengadaanMasterAsset().getDptotal() - totalPph);

								} else if (nilai > 0.1) {
									akunKredit.add(akunKredit1);
									nilais.add(nilai - totalPph);
								}

								if (!akunsDebetsMap.isEmpty() && !akunKredit.isEmpty()) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {

										ket = (saldoAwalMasterAsset.getKode()) + saldoAwalMasterAsset.getKeterangan();

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (saldoAwalMasterAsset != null
											&& saldoAwalMasterAsset.getSatuanKerja() != null
													? saldoAwalMasterAsset.getSatuanKerja()
													: tbmuser.ambilSatuanKerja());

									if (tbmuser.ambilSatuanKerja() != null) {
										satuanKerja = tbmuser.ambilSatuanKerja();
									}

									try {

										List<Akun> akunDebet = new ArrayList<Akun>();
										List<Double> nilaiDebets = new ArrayList<Double>();

										for (Long key : akunsDebetsMap.keySet()) {
											Akun akun = (Akun) ConstantValues.ambil(Akun.class.getName(), key);
											if (akun != null) {
												akunDebet.add(akun);
												nilaiDebets.add(akunsDebetsMap.get(key));
											}
										}

										if (nilai > 0.1) {
											CommonAkunting.saveTransaksi(akunDebet.toArray(new Akun[] {}),
													akunKredit.toArray(new Akun[] {}), akunDenda, akunPiutangDenda,
													postingHistory, apakahUangMasuk, ket,
													saldoAwalMasterAsset.getTanggalPersetujuan(),
													nilaiDebets.toArray(new Double[] {}),
													nilais.toArray(new Double[] {}), denda, saldoAwalMasterAsset,
													satuanKerja, session);
										} else {
											CommonAkunting.saveTransaksi(akunKredit.toArray(new Akun[] {}),
													akunDebet.toArray(new Akun[] {}), akunDenda, akunPiutangDenda,
													postingHistory, apakahUangMasuk, ket,
													saldoAwalMasterAsset.getTanggalPersetujuan(),
													nilais.toArray(new Double[] {}),
													nilaiDebets.toArray(new Double[] {}), denda, saldoAwalMasterAsset,
													satuanKerja, session);
										}
										session.flush();
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									saldoAwalMasterAsset.setPostingHistory(postingHistory);
									// merge() menangani kasus objek berbeda ber-ID sama sudah ada di session
									session.merge(saldoAwalMasterAsset);
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
	 * Membangun dan mengembalikan objek {@link Criteria} Hibernate yang digunakan untuk
	 * query data {@link SaldoAwalMasterAsset} sesuai kondisi dan filter yang aktif.
	 * Metode ini mendukung dua mode: mode normal (menggunakan checkbox filter) dan mode
	 * {@code sudah_posting} (menggunakan parameter URL, menyembunyikan filter normal).
	 *
	 * <p><b>Tujuan:</b> Memusatkan logika filter pencarian sehingga query untuk paging,
	 * data grid, dan batch posting massal menggunakan kriteria yang identik dan konsisten.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <b>Dalam mode {@code sudah_posting} tidak null:</b>
	 * <ul>
	 *   <li>Nilai tidak nol, tidak null, dan tidak termin (jsonTermin null).</li>
	 *   <li>Sudah disetujui (disetujuiOleh tidak null) dan aktif.</li>
	 *   <li>Rentang tanggal persetujuan sesuai {@code tglMulai} dan {@code tglSampai}.</li>
	 *   <li>Jika {@code sudah_posting == true}: hanya yang postingHistory.posting = true.</li>
	 *   <li>Jika {@code sudah_posting == false}: postingHistory null atau posting = false.</li>
	 * </ul>
	 * <b>Dalam mode normal (sudah_posting null):</b>
	 * <ul>
	 *   <li>Jika {@code searchtampil} dicentang: postingHistory null atau posting = false
	 *       (belum posting), dan {@code searchtelahtampil} di-uncheck.</li>
	 *   <li>Jika {@code searchtelahtampil} dicentang: logika sama (belum posting),
	 *       dan {@code searchtampil} di-uncheck.</li>
	 *   <li>Filter tambahan: jsonTermin null, aktif, disetujuiOleh tidak null,
	 *       nilai tidak nol, rentang tanggal persetujuan.</li>
	 * </ul>
	 * Filter umum (kedua mode): pemilik aset, lokasi, ruang, dan kode (substring).
	 * Pengurutan DESC by ID jika {@code order = true}.
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Jika {@code searchtampil} null, mengembalikan null segera.
	 * Exception dalam mode {@code sudah_posting} ditangkap dan ditampilkan via
	 * {@link Common#tampilErrorJikaAdmin}.</p>
	 *
	 * @param order {@code true} untuk menambahkan pengurutan DESC by ID (digunakan untuk
	 *              query data halaman); {@code false} untuk query count tanpa pengurutan.
	 * @return {@link Criteria} Hibernate yang sudah dikonfigurasi, atau {@code null} jika
	 *         komponen filter belum siap.
	 */
	public Criteria initCriteria(boolean order) {
		if (searchtampil == null) return null;

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(SaldoAwalMasterAsset.class);

		if (sudah_posting != null) {

			try {

				criteria.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
						.add(Restrictions.isNull("jsonTermin"))

						.add(Restrictions.isNotNull("disetujuiOleh"))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
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

			criteria

					.add(Restrictions.isNull("jsonTermin"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.isNotNull("disetujuiOleh"))

					.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))

					.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
							+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and  date('"
							+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

			;
		}

		criteria.add(
				searchpemilikAsset.getSelectedItem() == null || searchpemilikAsset.getSelectedItem().getValue() == null
						|| searchpemilikAsset.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("pemilikAsset", searchpemilikAsset.getSelectedItem().getValue()))
				.add(searchlokasi.getSelectedItem() == null || searchlokasi.getSelectedItem().getValue() == null
						|| searchlokasi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))
				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * Memperbarui tampilan grid dengan data transaksi pengadaan yang sesuai filter saat ini,
	 * tanpa menggunakan mekanisme progress bar. Metode ini dipanggil secara internal oleh
	 * {@link #loadDataDenganProgressPosting(Event)} setelah progress bar ditampilkan.
	 *
	 * <p><b>Tujuan:</b> Memisahkan logika refresh data aktual (query + render) dari logika
	 * pengelolaan progress bar, sehingga keduanya dapat diuji dan dikelola secara independen.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@link Common#initPaging} untuk menghitung total record dan memperbarui
	 *       komponen paging.</li>
	 *   <li>Mengambil halaman data {@link SaldoAwalMasterAsset} sesuai halaman aktif dengan
	 *       limit {@code Common.ROWS_COUNT_ON_PAGE}.</li>
	 *   <li>Menetapkan {@link SaldoAwalMasterAssetDetailRenderer} dan model ke grid.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari query atau renderer diteruskan ke caller
	 * ({@link #loadDataDenganProgressPosting}).</p>
	 *
	 * @param event event ZKoss yang memicu pencarian; dapat null ketika dipanggil secara
	 *              programatik dari {@link #loadDataDenganProgressPosting}.
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<SaldoAwalMasterAsset> saldoAwalMasterAssetDetail = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(saldoAwalMasterAssetDetail);
		grid.setRowRenderer(new SaldoAwalMasterAssetDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Event handler publik yang didelegasikan ke {@link #loadDataDenganProgressPosting(Event)}
	 * sebagai titik masuk tunggal untuk refresh data grid. Metode ini dipanggil oleh
	 * listener paging, timer inisialisasi, dan setelah operasi posting selesai.
	 *
	 * <p><b>Tujuan:</b> Menyediakan satu nama event handler standar yang diharapkan oleh
	 * ZKoss framework ({@code onSearchDefault}) sambil memastikan refresh selalu menggunakan
	 * mekanisme progress bar yang konsisten.</p>
	 *
	 * <p><b>Cara kerja:</b> Mendelegasikan seluruh proses ke
	 * {@link #loadDataDenganProgressPosting(Event)} yang mengelola state loading dan
	 * antrian reload.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception diteruskan ke caller (ZKoss framework).</p>
	 *
	 * @param event event ZKoss dari komponen yang memicu pencarian; dapat null ketika
	 *              dipanggil secara programatik.
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Flag boolean yang menunjukkan apakah proses muat data posting jurnal sedang berjalan.
	 * Digunakan untuk mencegah reload bersamaan yang dapat menyebabkan kondisi balapan (race
	 * condition) pada tampilan grid. Ketika bernilai {@code true}, request reload baru tidak
	 * langsung dieksekusi melainkan ditandai sebagai tertunda.
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag boolean yang menunjukkan apakah ada request reload yang tertunda saat
	 * {@code postingJurnalLoadingAktif} bernilai true. Ketika proses loading aktif selesai,
	 * sistem memeriksa flag ini dan jika true akan segera memulai proses loading ulang
	 * untuk memastikan data terbaru dimuat.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * Mengelola proses pemuatan data posting jurnal pengadaan dengan mekanisme progress bar
	 * yang informatif dan perlindungan terhadap reload bersamaan. Metode ini merupakan
	 * implementasi utama dari logika refresh data yang aman untuk konteks ZKoss.
	 *
	 * <p><b>Tujuan:</b> Mencegah kondisi balapan (race condition) saat multiple request
	 * reload datang hampir bersamaan (misalnya dari navigasi paging cepat atau setelah
	 * posting batch) dan memberikan umpan balik visual kepada pengguna selama proses
	 * pemuatan data yang mungkin memerlukan waktu cukup lama.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Jika {@code postingJurnalLoadingAktif} true (proses sedang berjalan), menandai
	 *       {@code postingJurnalReloadTertunda = true} dan menampilkan pesan loading bahwa
	 *       reload baru akan segera dilakukan, lalu keluar (tidak memulai loading baru).</li>
	 *   <li>Jika tidak ada loading aktif, mengaktifkan flag loading dan menampilkan progress
	 *       bar awal menggunakan {@code PostingJurnalLoadingUtil.show}.</li>
	 *   <li>Membuat timer default ZKoss yang menjalankan:
	 *     <ul>
	 *       <li>Update progress bar ke 48% dengan pesan "Mengambil Data".</li>
	 *       <li>Memanggil {@link #onSearchDefaultTanpaProgress} untuk query dan render data.</li>
	 *       <li>Update progress bar ke 92% dengan pesan "Merapikan Tampilan".</li>
	 *     </ul>
	 *   </li>
	 *   <li>Dalam blok {@code finally}: memeriksa apakah ada reload tertunda.
	 *     <ul>
	 *       <li>Jika ada: me-reset flag tertunda, memperbarui progress bar, dan membuat
	 *           timer baru untuk memulai ulang seluruh proses pemuatan.</li>
	 *       <li>Jika tidak ada: menyelesaikan progress bar ke 100%.</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading:</b> Seluruh eksekusi terjadi dalam ZKoss event-thread melalui
	 * timer default. Flag state ({@code postingJurnalLoadingAktif}, {@code postingJurnalReloadTertunda})
	 * aman diakses dari satu thread ini.</p>
	 *
	 * <p><b>Penanganan error:</b> Blok finally memastikan {@code postingJurnalLoadingAktif}
	 * selalu di-reset ke false meskipun terjadi exception dalam {@code onSearchDefaultTanpaProgress},
	 * sehingga halaman tidak terkunci dalam state loading permanen.</p>
	 *
	 * @param event event ZKoss yang memicu pemuatan; diteruskan ke
	 *              {@link #onSearchDefaultTanpaProgress} dan digunakan kembali jika ada
	 *              reload tertunda.
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
	// halaman ini: tanpa jendela modal, tanpa label progress, tanpa thread sendiri,
	// dan rentang tanggalnya datang sebagai parameter -- bukan dari datebox layar.
	// Pola sama dgn PostingKasKecilAction/PostingKasBesarAction/
	// PostingProsesTransferAction/PostingProsesTransitoriAction.
	//
	// PEMELIHARAAN: penentuan akun debet/kredit di sini HARUS tetap identik dengan
	// {@link #onPostingSemua} DAN dengan pratinjau jurnal pada renderer baris. Bila
	// aturan akunnya berubah, ubah di KETIGA tempat.
	// =====================================================================

	/**
	 * Kriteria tagihan vendor non-termin yang layak diposting pada rentang tanggal, tanpa
	 * bergantung pada komponen layar. Sama dengan bagian {@link #initCriteria(boolean)} yang
	 * tidak berhubungan dgn kotak pencarian, dan sama pula dgn kriteria baris "Penerimaan
	 * Tagihan Vendor" pada dasbor Draft Jurnal.
	 *
	 * <p>Saringan {@code jsonTermin is null} itulah yang memisahkan modul ini dari "Pekerjaan
	 * Vendor": dokumennya satu tabel, tetapi yang bertermin ditangani layar lain.</p>
	 */
	private static Criteria kriteriaPostingStatic(Session session, Date mulai, Date sampai) {
		Criteria c = session.createCriteria(SaldoAwalMasterAsset.class)
				.add(Restrictions.isNull("jsonTermin"))
				.add(Restrictions.isNotNull("disetujuiOleh"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/**
	 * Batalkan posting SEMUA tagihan vendor terposting dalam rentang.
	 *
	 * <p>Dijalankan pada {@code currentNativeSession()} dengan transaksi eksplisit per dokumen.
	 * Memakai {@code currentSession()} seperti tombol di layar hanya berhasil di dalam permintaan
	 * ZK, yang kerangkanya meng-commit sesi berjalan; dipanggil dari API perubahannya tidak
	 * pernah tersimpan sehingga pembatalan melaporkan sukses padahal jurnal dan penanda
	 * postingnya masih utuh.</p>
	 *
	 * <p>Baris {@code akunting.transaksi} dihapus lebih dulu karena {@code grup_transaksi}
	 * adalah induknya; layar hanya menghapus grup_transaksi dan meninggalkan barisnya. Jurnal
	 * yang SUDAH closing tidak ikut dihapus.</p>
	 */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(Date mulai, Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<SaldoAwalMasterAsset> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (SaldoAwalMasterAsset saldoAwal : daftar) {
				try {
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where saldo_awal_master_asset="
							+ saldoAwal.getId() + " and closing is null)").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where saldo_awal_master_asset="
							+ saldoAwal.getId() + " and closing is null").executeUpdate();
					saldoAwal.setPostingHistory(null);
					session.update(saldoAwal);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingPengadaanAction jalur API");
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
	 * Posting SEMUA tagihan vendor yang belum diposting dalam rentang. Penentuan akun per
	 * dokumen IDENTIK dengan {@link #onPostingSemua}:
	 *
	 * <ul>
	 *   <li><b>Debet</b>: satu baris per akun, dijumlahkan dari {@code hargaTotal} tiap detail.
	 *       Akunnya {@code akunBiayaPenyusutan} bila kelompok asetnya BUKAN aset tetap, selain
	 *       itu {@code akunTransaksi} -- keduanya lewat {@code AssetUtil.ambilDataAkun} supaya
	 *       akun per satuan kerja yang terpakai.</li>
	 *   <li><b>Kredit PPh</b>: tiap detail yang jenis pajak barangnya punya akun menambah satu
	 *       baris senilai {@code persenPph/100 x (jumlah x harga)}.</li>
	 *   <li><b>Kredit utang</b>: {@code penyedia.akunUtang} bila ada, selain itu
	 *       {@code penerimaan.jenisPenerimaanBarang.akunHutangPenyedia}.</li>
	 *   <li><b>Bila pemesanannya ber-DP</b> ({@code dptotal > 0,1}): sisa nilai di luar DP masuk
	 *       ke akun utang, dan DP-nya sendiri masuk ke
	 *       {@code jenisPemesananPengadaanAsset.akunDp} senilai {@code dptotal - totalPph}.
	 *       Tanpa DP: akun utang menerima {@code nilai - totalPph}.</li>
	 *   <li>bila {@code nilai} &le; 0,1 posisi debet/kredit ditukar -- sama seperti layar;</li>
	 *   <li>tanggal jurnal = {@code tanggalPersetujuan}, satuan kerja = milik dokumen.</li>
	 * </ul>
	 *
	 * <p><b>Dokumen yang jurnalnya belum lengkap dilewati.</b> Layar juga melewatinya bila peta
	 * akun debet atau daftar akun kredit kosong; di sini pemeriksaannya diperluas ke akun kredit
	 * yang bernilai null, karena {@code saveTransaksi} akan menerima larik berisi null dan
	 * jurnalnya jadi cacat.</p>
	 *
	 * <p><b>Penanda posting hanya dipasang bila jurnalnya benar-benar tersimpan.</b> Layar
	 * memasang {@code postingHistory} di luar blok penyimpanan, sehingga dokumen yang gagal
	 * dijurnal tetap hilang dari daftar draft dan tidak pernah diulang. Di sini urutannya
	 * dibalik supaya kegagalan tetap terlihat sebagai pekerjaan yang belum selesai.</p>
	 *
	 * <p><b>Yang TIDAK ikut diambil.</b> Dokumen yang sudah punya baris {@code postingHistory}
	 * dengan {@code posting=false} tetap terhitung sebagai draft di dasbor tetapi TIDAK diposting
	 * di sini -- persis seperti layar, yang juga menyembunyikan tombol posting barisnya. Selisih
	 * ini tertangkap penjaga kedua di API ("ada dokumen tetapi nol terproses" -&gt; penolakan).</p>
	 *
	 * @return jumlah dokumen yang BERHASIL diposting.
	 */
	@SuppressWarnings("unchecked")
	public static int postingSemua(Date mulai, Date sampai, Tbmuser oleh, Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<Long> ids = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory")).setProjection(Projections.property("id")).list();

			PostingHistory postingHistory = new PostingHistory(
					PostingHistory.JENIS_PENERIMAAN_TAGIHAN_BARANG_JASA);
			postingHistory.setTanggal(tglPosting == null ? new Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal penerimaan tagihan vendor dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			for (Long id : ids) {
				try {
					SaldoAwalMasterAsset saldoAwal = (SaldoAwalMasterAsset) session
							.createCriteria(SaldoAwalMasterAsset.class).add(Restrictions.idEq(id)).uniqueResult();
					if (saldoAwal == null) {
						continue;
					}
					SatuanKerja satuanKerja = saldoAwal.getSatuanKerja();

					List<SaldoAwalMasterAssetDetail> details = session
							.createCriteria(SaldoAwalMasterAssetDetail.class)
							.add(Restrictions.eq("saldoAwal", saldoAwal)).list();

					Map<Long, Double> akunsDebetsMap = new HashMap<Long, Double>();
					List<Akun> akunKredit = new ArrayList<Akun>();
					List<Double> nilais = new ArrayList<Double>();
					Double totalPph = 0.0;
					for (SaldoAwalMasterAssetDetail detail : details) {
						Akun akunDebet;
						if (detail.getMasterAsset().getKelompokAsset() != null
								&& !detail.getMasterAsset().getKelompokAsset().getMerupakanAssetFix()) {
							akunDebet = AssetUtil.ambilDataAkun(detail.getMasterAsset().akunBiayaPenyusutanEfektif(),
									satuanKerja);
						} else {
							akunDebet = AssetUtil.ambilDataAkun(detail.getMasterAsset().akunTransaksiEfektif(),
									satuanKerja);
						}
						if (akunDebet != null) {
							Double sudah = akunsDebetsMap.get(akunDebet.getId());
							if (sudah == null) {
								sudah = 0.0;
							}
							sudah += detail.getHargaTotal();
							akunsDebetsMap.put(akunDebet.getId(), sudah);
						}
						if (detail.getJenisPajakBarang() != null && detail.getJenisPajakBarang().getAkun() != null) {
							Double dpp = detail.getJumlah() * detail.getHarga();
							Double pph = (detail.getPersenPph() / 100.0) * dpp;
							akunKredit.add(detail.getJenisPajakBarang().getAkun());
							nilais.add(pph);
							totalPph += pph;
						}
					}

					Akun akunUtang = saldoAwal.getPenerimaanPengadaanMasterAsset() == null
							|| saldoAwal.getPenerimaanPengadaanMasterAsset().getJenisPenerimaanBarang() == null ? null
									: saldoAwal.getPenerimaanPengadaanMasterAsset().getJenisPenerimaanBarang()
											.getAkunHutangPenyedia();
					if (saldoAwal.getPenyedia() != null && saldoAwal.getPenyedia().getAkunUtang() != null) {
						akunUtang = saldoAwal.getPenyedia().getAkunUtang();
					}

					Double nilai = saldoAwal.getNilai();
					boolean lengkap = true;

					if (saldoAwal.getPenerimaanPengadaanMasterAsset() != null
							&& saldoAwal.getPenerimaanPengadaanMasterAsset()
									.getPemesananPengadaanMasterAsset() != null
							&& saldoAwal.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()
									.getJenisPemesananPengadaanAsset() != null
							&& saldoAwal.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()
									.getDptotal() > 0.1) {
						Double dptotal = saldoAwal.getPenerimaanPengadaanMasterAsset()
								.getPemesananPengadaanMasterAsset().getDptotal();
						Double sisa = nilai - dptotal;
						if (sisa > 0.1) {
							if (akunUtang == null) {
								lengkap = false;
							} else {
								akunKredit.add(akunUtang);
								nilais.add(sisa);
							}
						}
						Akun akunDp = saldoAwal.getPenerimaanPengadaanMasterAsset()
								.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset().getAkunDp();
						if (akunDp == null) {
							lengkap = false;
						} else {
							akunKredit.add(akunDp);
							nilais.add(dptotal - totalPph);
						}
					} else if (nilai > 0.1) {
						if (akunUtang == null) {
							lengkap = false;
						} else {
							akunKredit.add(akunUtang);
							nilais.add(nilai - totalPph);
						}
					}

					if (!lengkap || akunsDebetsMap.isEmpty() || akunKredit.isEmpty()) {
						// Jurnalnya tidak lengkap: dilewati, sama seperti layar yang tidak
						// menampilkan tombol posting untuk baris berjurnal tidak valid.
						continue;
					}

					List<Akun> akunDebets = new ArrayList<Akun>();
					List<Double> nilaiDebets = new ArrayList<Double>();
					for (Long kunciAkun : akunsDebetsMap.keySet()) {
						Akun akun = (Akun) ConstantValues.ambil(Akun.class.getName(), kunciAkun);
						if (akun != null) {
							akunDebets.add(akun);
							nilaiDebets.add(akunsDebetsMap.get(kunciAkun));
						}
					}
					if (akunDebets.isEmpty()) {
						continue;
					}

					String ket = "";
					try {
						ket = saldoAwal.getKode() + saldoAwal.getKeterangan();
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e, "PostingPengadaanAction jalur API");
					}

					boolean tersimpan = false;
					try {
						session = HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						if (nilai > 0.1) {
							CommonAkunting.saveTransaksi(akunDebets.toArray(new Akun[] {}),
									akunKredit.toArray(new Akun[] {}), null, null, postingHistory, true, ket,
									saldoAwal.getTanggalPersetujuan(), nilaiDebets.toArray(new Double[] {}),
									nilais.toArray(new Double[] {}), Double.valueOf(0.0), saldoAwal, satuanKerja,
									session);
						} else {
							CommonAkunting.saveTransaksi(akunKredit.toArray(new Akun[] {}),
									akunDebets.toArray(new Akun[] {}), null, null, postingHistory, true, ket,
									saldoAwal.getTanggalPersetujuan(), nilais.toArray(new Double[] {}),
									nilaiDebets.toArray(new Double[] {}), Double.valueOf(0.0), saldoAwal,
									satuanKerja, session);
						}
						session.getTransaction().commit();
						tersimpan = true;
					} catch (Exception e) {
						try {
							session.getTransaction().rollback();
						} catch (Exception ex) {
							// rollback gagal: kegagalan aslinya yang dilaporkan
						}
						ais.common.ErrorAuditUtil.record(e, "PostingPengadaanAction jalur API");
					}

					if (tersimpan) {
						// Penanda posting hanya dipasang bila jurnalnya BENAR-BENAR tersimpan.
						saldoAwal.setPostingHistory(postingHistory);
						session = HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.update(saldoAwal);
						session.getTransaction().commit();
						n++;
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "auto-audit PostingPengadaanAction.postingSemua");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingPengadaanAction jalur API");
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
