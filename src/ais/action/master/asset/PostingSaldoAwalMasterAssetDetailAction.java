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

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.asset.util.AssetUtil;
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
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PemilikAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
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
 * <h3>PostingSaldoAwalMasterAssetDetailAction — Posting Jurnal Akuntansi Saldo Awal Detail Pengadaan Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah Action (Composer ZK) yang mengelola proses posting jurnal akuntansi untuk
 * transaksi penerimaan barang/jasa dalam modul pengadaan aset, khususnya saldo awal master aset.
 * Posting jurnal berarti mencatat transaksi penerimaan pengadaan ke dalam buku besar akuntansi
 * (tabel grup_transaksi) dengan pasangan akun debet (akun aset per detail) dan kredit (akun
 * jenis penerimaan barang). Kelas ini mendukung pembatalan posting (reversal) baik satu per satu
 * maupun massal, serta mendukung multiple akun debet untuk satu transaksi penerimaan yang memiliki
 * banyak detail aset dengan kategori berbeda-beda.
 *
 * <b>Cara kerja:</b><br>
 * Setelah halaman ZUL dimuat, {@link #doAfterCompose(Component)} menginisialisasi filter pencarian
 * (tanggal, pemilik aset, lokasi, ruang, status posting), mengisi combobox pemilik dan lokasi,
 * mengunci pilihan lokasi jika ada atribut sesi "Lokasi", dan memuat data grid pertama kali.
 * Grid menampilkan daftar {@link PenerimaanPengadaanMasterAsset} yang sudah disetujui dengan nilai
 * tidak nol, dirender oleh inner class {@link PenerimaanPengadaanMasterAssetDetailRenderer}.
 * Setiap baris menampilkan kode penerimaan, kode pemesanan, satuan kerja, preview jurnal multi-akun,
 * status posting, dan tombol aksi. Akun debet diambil per detail menggunakan
 * {@link AssetUtil#ambilDataAkun} dengan fallback ke akun dari permintaan pengadaan jika null.
 * Posting massal berjalan di thread latar belakang menggunakan sesi Hibernate native terpisah.
 *
 * <b>Threading:</b><br>
 * Proses posting massal ({@link #onPostingSemua(Event)}) berjalan di dalam {@code new Thread(Runnable).start()}
 * sehingga tidak memblokir event thread ZK. State loading dikelola dengan flag
 * {@code postingJurnalLoadingAktif} dan {@code postingJurnalReloadTertunda} untuk mencegah
 * reload yang tidak perlu saat loading sedang aktif. Update UI dilakukan melalui
 * {@code Common.displayLoadBar} dengan callback setelah thread selesai.
 *
 * <b>Pemeliharaan:</b><br>
 * Berbeda dengan PostingPembayaranDpAction, kelas ini menggunakan multi-akun debet karena satu
 * penerimaan pengadaan dapat berisi banyak detail aset dengan tipe akun yang berbeda. Logika
 * pemetaan akun ada di {@link AssetUtil#ambilDataAkun} dan sebagai fallback menggunakan akun
 * dari PermintaanPengadaanMasterAsset. Pastikan akun transaksi di setiap MasterAsset dikonfigurasi
 * dengan benar. Filter lokasi mendukung "kunci lokasi" (LokasiAction.kunciLokasi) yang membatasi
 * pilihan lokasi berdasarkan peran pengguna. Jika skema tabel berubah (nama kolom/tabel di
 * akunting), perbarui query SQL native di onBatalkanPostingSemua dan renderer.
 */
public class PostingSaldoAwalMasterAssetDetailAction extends GenericAutowireComposer {

	/**
	 * Serial version UID untuk serialisasi kelas ini.
	 * Nilai ini dibangkitkan otomatis dan tidak boleh diubah tanpa alasan yang jelas.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/**
	 * Filter kategori kelompok aset untuk sub-tab Penyusutan (diisi dari arg createComponents):
	 * null = semua (perilaku standalone lama), "fixasset" = kelompok NON-CIP (Fix Aset /
	 * tidak dalam pekerjaan), "pekerjaan" = kelompok CIP (Aset dalam Pekerjaan).
	 */
	private String filterKelompok = null;

	/** EXISTS: penerimaan ini punya minimal 1 detail yang kelompok aset-nya CIP (dalam pekerjaan). */
	private static final String SQL_EXISTS_KELOMPOK_CIP =
			"exists (select 1 from asset.penerimaan_pengadaan_master_asset_detail d "
			+ "join asset.master_asset m on d.masterasset = m.id "
			+ "join asset.kelompok_asset k on m.kelompok_asset = k.id "
			+ "where d.penerimaan_pengadaan_master_asset = this_.id "
			+ "and coalesce(k.merupakanpekerjaandalampelaksanaan, false) = true)";

	/** Grid utama yang menampilkan daftar transaksi penerimaan pengadaan yang siap atau sudah diposting. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman pada grid data penerimaan pengadaan. */
	private Paging paging;

	/** Textbox pencarian berdasarkan kode penerimaan, kode tagihan, atau kode pemesanan. */
	private Textbox searchkode;

	/** Combobox filter pemilik aset untuk mempersempit hasil pencarian. */
	private Combobox searchpemilikAsset;

	/** Combobox filter lokasi aset untuk mempersempit hasil pencarian berdasarkan lokasi. */
	private Combobox searchlokasi;

	/** Banbox pencarian berdasarkan ruang/lokasi fisik spesifik dari aset yang bersangkutan. */
	private AmbilDataRuangBanbox searchruang;

	/** Checkbox filter untuk menampilkan hanya transaksi yang belum diposting. */
	private MyCheckboxConfig searchtampil;

	/** Checkbox filter untuk menampilkan hanya transaksi yang sudah diposting. */
	private MyCheckboxConfig searchtelahtampil;

	/** Flag yang menandakan apakah pengguna saat ini memiliki hak akses UPDATE. */
	private boolean edit = false;

	/** Tombol toolbar "Kirim" yang hanya ditampilkan jika pengguna memiliki hak UPDATE. */
	private MyToolbarbuttonConfig sent;

	/** Flag yang menandakan apakah pengguna adalah admin atau memiliki hak APPROVE. */
	public boolean adminLain;

	/** Datebox untuk filter tanggal awal rentang pencarian berdasarkan tanggal persetujuan. */
	private MyDatebox tglMulai;

	/** Datebox untuk filter tanggal akhir rentang pencarian berdasarkan tanggal persetujuan. */
	private MyDatebox tglSampai;

	/** Data pengguna yang sedang login, digunakan untuk otorisasi tombol posting. */
	private Tbmuser tbmuser;

	/**
	 * <h3>doBeforeCompose — Pemeriksaan Keamanan Sebelum Halaman Dimuat</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Memverifikasi bahwa pengguna yang mengakses halaman ini telah terotentikasi dan
	 * berwenang sebelum proses wiring komponen ZUL dimulai.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@link Common#doCheckSecurity()} yang memeriksa sesi dan hak akses pengguna.
	 * Jika gagal, pengguna diarahkan keluar secara otomatis. Kemudian mendelegasikan ke superclass
	 * untuk melanjutkan proses komposisi halaman.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Standar keamanan yang harus ada di semua halaman Action AIS. Jangan hapus atau modifikasi
	 * pemeriksaan ini tanpa konsultasi dengan tim arsitektur.
	 *
	 * @param page     halaman ZK yang sedang dikomposisi
	 * @param parent   komponen induk tempat halaman ini dipasang
	 * @param compInfo informasi metadata komponen dari file ZUL
	 * @return ComponentInfo dari superclass untuk melanjutkan proses wiring
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <h3>doAfterCompose — Inisialisasi Halaman Posting Saldo Awal Asset Setelah Wiring Selesai</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Menginisialisasi seluruh state halaman setelah komponen ZUL berhasil di-wire. Meliputi
	 * validasi sesi, pengaturan filter tanggal default 6 bulan ke belakang, pengisian combobox
	 * pemilik aset dan lokasi, penanganan kunci lokasi dari sesi, penentuan hak akses,
	 * dan pemuatan data grid pertama kali.
	 *
	 * <b>Cara kerja:</b><br>
	 * Pertama memvalidasi sesi; jika tidak valid, pengguna diarahkan ke logoff. Kemudian mengambil
	 * data pengguna via {@link Common#getCurrentUser()}. Filter tanggal diinisialisasi dengan
	 * rentang 6 bulan ke belakang hingga hari ini dan dibuat readonly. Hak akses adminLain dan
	 * edit diperiksa. Combobox pemilik aset dan lokasi diisi dengan data aktif dari basis data.
	 * Jika sesi memiliki atribut "Lokasi", lokasi tersebut dipilih otomatis dan combobox dikunci.
	 * Selanjutnya dipanggil LokasiAction.kunciLokasi untuk pembatasan lokasi berdasarkan role.
	 * Data grid dimuat via {@link #loadDataDenganProgressPosting(Event)} dan paging dikonfigurasi.
	 * Filter lanjut diinisialisasi via {@link FilterLanjutHelper#setup(Component)}.
	 *
	 * <b>Parameter:</b><br>
	 * @param comp komponen root ZUL yang telah selesai di-wire
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika sesi tidak valid, pengguna langsung diarahkan logoff tanpa melanjutkan inisialisasi.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Mekanisme kunci lokasi menggunakan atribut sesi "Lokasi" dan LokasiAction.kunciLokasi.
	 * Pastikan atribut sesi ini di-set dengan benar oleh halaman yang menavigasi ke sini.
	 *
	 * @throws Exception jika terjadi error pada inisialisasi superclass atau komponen ZK
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		// Mode filter kategori dari query-param MyInclude (sub-tab Penyusutan). Standalone = null.
		try {
			if (org.zkoss.zk.ui.Executions.getCurrent() != null) {
				String fk = org.zkoss.zk.ui.Executions.getCurrent().getParameter("filterKelompok");
				if (fk != null && !fk.trim().isEmpty()) {
					filterKelompok = fk.trim();
				}
			}
		} catch (Exception eArgKelompok) { ais.common.ErrorAuditUtil.record(eArgKelompok, "auto-audit(empty-catch) src/ais/action/master/asset/PostingSaldoAwalMasterAssetDetailAction.java:240");
		}
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
	 * <h3>onBatalkanPostingSemua — Pembatalan Posting Semua Transaksi Penerimaan Pengadaan</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Menangani event klik tombol "Batalkan Posting Semua" pada toolbar. Membatalkan posting
	 * jurnal untuk semua transaksi penerimaan pengadaan yang sesuai filter aktif dan sudah
	 * berstatus terposting, sehingga transaksi kembali ke status belum diposting.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menampilkan dialog konfirmasi. Jika disetujui, mencari semua {@link PenerimaanPengadaanMasterAsset}
	 * yang memiliki postingHistory tidak null berdasarkan kriteria filter aktif. Untuk setiap
	 * transaksi: set postingHistory ke null, perbarui entitas, dan hapus entri grup_transaksi
	 * yang terkait (yang belum closing) menggunakan SQL native langsung ke skema akunting.
	 * Setelah semua selesai, grid dimuat ulang melalui timer default.
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika pengguna memilih CANCEL, tidak ada tindakan. Error basis data tidak di-catch
	 * secara eksplisit; Hibernate mengelola transaksi melalui Common.refreshSaveOrUpdate.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Query SQL native mengacu pada kolom "penerimaan_pengadaan_master_asset" di skema
	 * akunting.grup_transaksi. Perbarui jika nama kolom atau skema berubah.
	 *
	 * @param event event ZK yang dipicu oleh klik tombol, tidak digunakan langsung
	 * @throws Exception jika terjadi error pada operasi basis data atau komponen ZK
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi pengadaan ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<PenerimaanPengadaanMasterAsset> penerimaanPengadaanMasterAssets = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset : penerimaanPengadaanMasterAssets) {
								penerimaanPengadaanMasterAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(penerimaanPengadaanMasterAsset);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where penerimaan_pengadaan_master_asset="
												+ penerimaanPengadaanMasterAsset.getId() + " and closing is null")
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
	 * <h3>onPostingSemua — Posting Jurnal Massal untuk Semua Transaksi Penerimaan Pengadaan</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Menangani event klik tombol "Posting Semua". Membuka dialog form posting yang meminta
	 * tanggal dan keterangan, kemudian memposting semua transaksi penerimaan pengadaan yang
	 * belum diposting secara massal di thread latar belakang.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat jendela modal dengan form tanggal dan keterangan. Setelah pengguna mengisi dan
	 * klik Simpan, dibuat satu {@link PostingHistory} baru. Di thread latar belakang, untuk
	 * setiap {@link PenerimaanPengadaanMasterAsset} yang belum diposting: ambil detail-detail
	 * penerimaan, kumpulkan daftar akun debet (satu per detail, via AssetUtil.ambilDataAkun
	 * dengan fallback ke akun permintaan), jumlahkan nilai total, dan tentukan akun kredit dari
	 * jenis penerimaan barang. Jika daftar akun debet tidak kosong dan akun kredit valid, panggil
	 * {@link CommonAkunting#saveTransaksi} dengan overload multi-akun. Progress ditampilkan
	 * melalui label load bar. Setelah semua selesai, tampilkan pesan sukses dan muat ulang grid.
	 *
	 * <b>Perbedaan dari PostingPembayaranDpAction:</b><br>
	 * Kelas ini menggunakan daftar (List) akun debet dan nilai debet karena satu penerimaan
	 * pengadaan dapat memiliki banyak detail aset dengan akun transaksi yang berbeda-beda.
	 * Akun kredit tetap satu (dari jenis penerimaan barang).
	 *
	 * <b>Threading:</b><br>
	 * Seluruh iterasi posting berjalan di thread terpisah dengan sesi Hibernate native. Sesi
	 * ditutup secara eksplisit di akhir thread dengan HibernateUtil.closeSession().
	 *
	 * <b>Penanganan error:</b><br>
	 * Error per transaksi ditangkap dengan try-catch dan dilaporkan via Common.tampilErrorJikaAdmin.
	 * Transaksi lain tetap dilanjutkan. Jenis posting history: JENIS_PENERIMAAN_BARANG_JASA.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pastikan akun transaksi di setiap MasterAsset selalu dikonfigurasi. Jika akun null,
	 * fallback ke akun dari PermintaanPengadaanMasterAsset. Jika keduanya null, transaksi
	 * dilewati (akunsDebets.isEmpty() true).
	 *
	 * @param event event ZK yang dipicu oleh klik tombol Posting Semua
	 * @throws Exception jika terjadi error pada pembuatan komponen ZK atau operasi basis data awal
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
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting Saldo Awal belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal dan pilih tanggal dari kalender; (2) Pastikan tanggal berada dalam periode akuntansi yang aktif; (3) ulangi proses posting saldo awal ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
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
													PostingHistory.JENIS_PENERIMAAN_BARANG_JASA);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<PenerimaanPengadaanMasterAsset> penerimaanPengadaanMasterAssets = initCriteria(
													true).add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset : penerimaanPengadaanMasterAssets) {

												if (penerimaanPengadaanMasterAsset != null) {

													try {
														SatuanKerja satuanKerja = penerimaanPengadaanMasterAsset
																.getSatuanKerja();
														List<PenerimaanPengadaanMasterAssetDetail> penerimaanPengadaanMasterAssetDetails = session
																.createCriteria(
																		PenerimaanPengadaanMasterAssetDetail.class)
																.add(Restrictions.eq("penerimaanPengadaanMasterAsset",
																		penerimaanPengadaanMasterAsset))
																.addOrder(Order.asc("id")).list();
														List<Akun> akunsDebets = new ArrayList<Akun>();
														List<Double> nilaiDebet = new ArrayList<Double>();
														Double nilai = 0.0;
														for (PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail : penerimaanPengadaanMasterAssetDetails) {
															Akun akunDebet = AssetUtil.ambilDataAkun(
																	penerimaanPengadaanMasterAssetDetail
																			.getMasterAsset().getAkunTransaksi(),
																	satuanKerja);

															if (akunDebet == null
																	&& penerimaanPengadaanMasterAssetDetail
																			.getPemesananPengadaanMasterAssetDetail() != null
																	&& penerimaanPengadaanMasterAssetDetail
																			.getPemesananPengadaanMasterAssetDetail()
																			.getPermintaanPengadaanMasterAssetDetail() != null
																	&& penerimaanPengadaanMasterAssetDetail
																			.getPemesananPengadaanMasterAssetDetail()
																			.getPermintaanPengadaanMasterAssetDetail()
																			.getPermintaanPengadaanMasterAsset()
																			.getAkun() != null) {
																akunDebet = penerimaanPengadaanMasterAssetDetail
																		.getPemesananPengadaanMasterAssetDetail()
																		.getPermintaanPengadaanMasterAssetDetail()
																		.getPermintaanPengadaanMasterAsset().getAkun();
															}
															// Fallback: akunBiayaPenyusutan untuk barang/jasa bukan aset tetap
															if (akunDebet == null
																	&& penerimaanPengadaanMasterAssetDetail.getMasterAsset() != null) {
																MasterAsset masterAsset = penerimaanPengadaanMasterAssetDetail.getMasterAsset();
																boolean isFixAsset = masterAsset.getKelompokAsset() == null
																		|| masterAsset.getKelompokAsset().getMerupakanAssetFix();
																if (!isFixAsset) {
																	akunDebet = AssetUtil.ambilDataAkun(
																			masterAsset.getAkunBiayaPenyusutan(),
																			satuanKerja);
																}
															}
															Double d = penerimaanPengadaanMasterAssetDetail
																	.getHargaTotal();
															akunsDebets.add(akunDebet);
															nilaiDebet.add(d);
															nilai += d;
														}

														Akun akunKredit = penerimaanPengadaanMasterAsset
																.getJenisPenerimaanBarang() == null ? null
																		: penerimaanPengadaanMasterAsset
																				.getJenisPenerimaanBarang().getAkun();

														if (!akunsDebets.isEmpty() && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {

																ket = (penerimaanPengadaanMasterAsset.getKode()) + " "
																		+ penerimaanPengadaanMasterAsset
																				.getKeterangan();

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(ket + " ("
																	+ Common.numberFormat.get().format(rowIndex * 100.0
																			/ penerimaanPengadaanMasterAssetDetails
																					.size())
																	+ " %)");

															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																List<Akun> akunKredits = new ArrayList<Akun>();
																akunKredits.add(akunKredit);
																List<Double> nilaiKredit = new ArrayList<Double>();
																nilaiKredit.add(nilai);

																session.getTransaction().begin();
																CommonAkunting.saveTransaksi(akunsDebets, akunKredits,
																		akunDenda, akunPiutangDenda, postingHistory,
																		apakahUangMasuk, ket,
																		penerimaanPengadaanMasterAsset
																				.getTanggalPersetujuan(),
																		nilaiDebet, nilaiKredit, denda,
																		penerimaanPengadaanMasterAsset, satuanKerja,
																		session);
																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															penerimaanPengadaanMasterAsset
																	.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															Common.refreshUpdate(session,
																	penerimaanPengadaanMasterAsset);
															session.getTransaction().commit();
														}
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
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
	 * <h3>PenerimaanPengadaanMasterAssetDetailRenderer — Renderer Baris Grid Posting Saldo Awal</h3>
	 *
	 * <b>Untuk apa:</b><br>
	 * Inner class ini bertanggung jawab merender setiap baris pada grid halaman posting saldo awal
	 * master aset. Setiap baris merepresentasikan satu {@link PenerimaanPengadaanMasterAsset}
	 * beserta informasi jurnal multi-akun dan tombol aksi posting/pembatalan.
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengimplementasi metode render dari MyRowRenderer. Untuk setiap penerimaan pengadaan:
	 * 1. Tampilkan kode penerimaan dan tautan SOP jika ada.
	 * 2. Tampilkan kode pemesanan dan nama satuan kerja.
	 * 3. Query detail penerimaan, kumpulkan daftar akun debet dan nilai per detail.
	 * 4. Update nilai total pada entitas jika berbeda dengan yang tersimpan.
	 * 5. Tampilkan nilai total dan tanggal persetujuan.
	 * 6. Tentukan akun kredit dari jenis penerimaan barang.
	 * 7. Tampilkan preview jurnal multi-akun atau pesan error jika akun tidak valid.
	 * 8. Tampilkan nomor bukti posting dan status posting.
	 * 9. Tampilkan tombol Batalkan Posting dan tombol Posting dengan kondisi visibilitas.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Update nilai total (setNilai) dilakukan saat render untuk sinkronisasi data yang mungkin
	 * berubah akibat modifikasi detail. Ini berdampak pada performa jika data banyak — pertimbangkan
	 * untuk memindahkan sinkronisasi ini ke proses batch terpisah jika diperlukan.
	 */
	class PenerimaanPengadaanMasterAssetDetailRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <h3>render — Merender Satu Baris Data Penerimaan Pengadaan ke Grid</h3>
		 *
		 * <b>Tujuan:</b><br>
		 * Mengisi komponen-komponen ZK pada satu baris grid dengan data lengkap dari satu entitas
		 * {@link PenerimaanPengadaanMasterAsset}, termasuk informasi jurnal multi-akun dan tombol aksi.
		 *
		 * <b>Cara kerja:</b><br>
		 * Mengambil satuan kerja dari pemesanan pengadaan, menampilkan kode penerimaan dengan
		 * revisi history, tautan SOP, kode pemesanan, satuan kerja. Kemudian query detail untuk
		 * mendapatkan multi-akun debet dan nilai, menentukan akun kredit, menampilkan jurnal
		 * atau pesan error, menampilkan status posting, dan tombol aksi per baris.
		 *
		 * <b>Penanganan error:</b><br>
		 * Jika akun debet kosong atau kredit null, ditampilkan pesan informatif. Tombol posting
		 * tidak muncul untuk baris dengan akun tidak valid. Perlu satuanKerja tidak null untuk
		 * tombol posting per-baris (guard tambahan pada visibilitas tombol).
		 *
		 * @param arg0 komponen Row ZK yang akan diisi dengan komponen child
		 * @param arg1 objek data baris, dicast ke {@link PenerimaanPengadaanMasterAsset}
		 * @throws Exception jika terjadi error pada operasi basis data atau pembuatan komponen
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset = (PenerimaanPengadaanMasterAsset) arg1;

			final SatuanKerja satuanKerja = (SatuanKerja) (penerimaanPengadaanMasterAsset
					.getPemesananPengadaanMasterAsset().getSatuanKerja());

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PenerimaanPengadaanMasterAsset.class, penerimaanPengadaanMasterAsset,
					penerimaanPengadaanMasterAsset.getKode() == null ? "" : penerimaanPengadaanMasterAsset.getKode()))
					.setParent(arg0);

			if (penerimaanPengadaanMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + penerimaanPengadaanMasterAsset.getDisposisiSop().getKeterangan() + " ("
						+ penerimaanPengadaanMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(penerimaanPengadaanMasterAsset.getDisposisiSop().getId(), null,
								null, true, arg0.getTarget());
					}
				});
			}

			new Label(penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset() == null ? ""
					: penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset().getKode()).setParent(arg0);

			new Label(satuanKerja == null ? "" : satuanKerja.getNama()).setParent(arg0);

			Session session = HibernateUtil.currentSession();
			@SuppressWarnings("unchecked")
			List<PenerimaanPengadaanMasterAssetDetail> penerimaanPengadaanMasterAssetDetails = session
					.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset))
					.addOrder(Order.asc("id")).list();
			List<Akun> akunsDebets = new ArrayList<Akun>();
			List<Double> nilaiDebet = new ArrayList<Double>();
			Double nilai = 0.0;
			for (PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail : penerimaanPengadaanMasterAssetDetails) {
				Akun akunDebet = AssetUtil.ambilDataAkun(
						penerimaanPengadaanMasterAssetDetail.getMasterAsset().getAkunTransaksi(), satuanKerja);

				if (akunDebet == null
						&& penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail() != null
						&& penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
								.getPermintaanPengadaanMasterAssetDetail() != null
						&& penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
								.getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset()
								.getAkun() != null) {
					akunDebet = penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset().getAkun();
				}
				Double d = penerimaanPengadaanMasterAssetDetail.getHargaTotal();
				akunsDebets.add(akunDebet);
				nilaiDebet.add(d);
				nilai += d;
			}
			if (penerimaanPengadaanMasterAsset.getNilai().intValue() != nilai.intValue()) {
				penerimaanPengadaanMasterAsset.setNilai(nilai);
				Common.refreshUpdate(session, penerimaanPengadaanMasterAsset);
			}

			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

			new Label(Common.dateFormat3.get().format(penerimaanPengadaanMasterAsset.getTanggalPersetujuan())).setParent(arg0);

			Akun akunKredit = penerimaanPengadaanMasterAsset.getJenisPenerimaanBarang() == null ? null
					: penerimaanPengadaanMasterAsset.getJenisPenerimaanBarang().getAkun();

			if (!akunsDebets.isEmpty() && akunKredit != null) {
				List<Akun> akunKredits = new ArrayList<Akun>();
				akunKredits.add(akunKredit);
				List<Double> nilaiKredit = new ArrayList<Double>();
				nilaiKredit.add(nilai);
				GrupTransaksi.tampilkanJurnal(akunsDebets, nilaiDebet, akunKredits, nilaiKredit).setParent(arg0);
			} else {
				new Label("Transaksi tidak valid."
						+ (!akunsDebets.isEmpty() ? " Debet: " + akunsDebets + "." : " Akun debet tidak ada.")
						+ (akunKredit != null ? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
								: " Akun kredit tidak ada."))
						.setParent(arg0);
			}

			String bukti = "";

			bukti = (String) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.eq("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset))
					.setMaxResults(1).setProjection(Projections.property("kode")).uniqueResult();

			new Label(penerimaanPengadaanMasterAsset.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: penerimaanPengadaanMasterAsset.getPostingHistory().toString() + ", no. bukti : " + bukti)
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (!akunsDebets.isEmpty() && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && penerimaanPengadaanMasterAsset.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								penerimaanPengadaanMasterAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(penerimaanPengadaanMasterAsset);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where penerimaan_pengadaan_master_asset="
												+ penerimaanPengadaanMasterAsset.getId() + " and closing is null")
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
				button.setVisible(edit && penerimaanPengadaanMasterAsset.getPostingHistory() == null && tbmuser != null
						&& satuanKerja != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PENERIMAAN_BARANG_JASA);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								List<PenerimaanPengadaanMasterAssetDetail> penerimaanPengadaanMasterAssetDetails = session
										.createCriteria(PenerimaanPengadaanMasterAssetDetail.class).add(Restrictions
												.eq("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset))
										.addOrder(Order.asc("id")).list();
								List<Akun> akunsDebets = new ArrayList<Akun>();
								List<Double> nilaiDebet = new ArrayList<Double>();
								Double nilai = 0.0;
								for (PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail : penerimaanPengadaanMasterAssetDetails) {
									Akun akunDebet = AssetUtil.ambilDataAkun(
											penerimaanPengadaanMasterAssetDetail.getMasterAsset().getAkunTransaksi(),
											satuanKerja);

									if (akunDebet == null
											&& penerimaanPengadaanMasterAssetDetail
													.getPemesananPengadaanMasterAssetDetail() != null
											&& penerimaanPengadaanMasterAssetDetail
													.getPemesananPengadaanMasterAssetDetail()
													.getPermintaanPengadaanMasterAssetDetail() != null
											&& penerimaanPengadaanMasterAssetDetail
													.getPemesananPengadaanMasterAssetDetail()
													.getPermintaanPengadaanMasterAssetDetail()
													.getPermintaanPengadaanMasterAsset().getAkun() != null) {
										akunDebet = penerimaanPengadaanMasterAssetDetail
												.getPemesananPengadaanMasterAssetDetail()
												.getPermintaanPengadaanMasterAssetDetail()
												.getPermintaanPengadaanMasterAsset().getAkun();
									}
									Double d = penerimaanPengadaanMasterAssetDetail.getHargaTotal();
									akunsDebets.add(akunDebet);
									nilaiDebet.add(d);
									nilai += d;
								}

								Akun akunKredit = penerimaanPengadaanMasterAsset.getJenisPenerimaanBarang() == null
										? null
										: penerimaanPengadaanMasterAsset.getJenisPenerimaanBarang().getAkun();

								if (!akunsDebets.isEmpty() && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {

										ket = (penerimaanPengadaanMasterAsset.getKode()) + " "
												+ penerimaanPengadaanMasterAsset.getKeterangan();

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = penerimaanPengadaanMasterAsset.getSatuanKerja();

									List<Akun> akunKredits = new ArrayList<Akun>();
									akunKredits.add(akunKredit);
									List<Double> nilaiKredit = new ArrayList<Double>();
									nilaiKredit.add(nilai);

									CommonAkunting.saveTransaksi(akunsDebets, akunKredits, akunDenda, akunPiutangDenda,
											postingHistory, apakahUangMasuk, ket,
											penerimaanPengadaanMasterAsset.getTanggalPersetujuan(), nilaiDebet,
											nilaiKredit, denda, penerimaanPengadaanMasterAsset, satuanKerja, session);

									penerimaanPengadaanMasterAsset.setPostingHistory(postingHistory);

									Common.refreshUpdate(session, penerimaanPengadaanMasterAsset);
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
	 * <h3>initCriteria — Membangun Kriteria Pencarian Data Penerimaan Pengadaan Aset</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Membangun objek {@link Criteria} Hibernate yang mencerminkan seluruh kondisi filter pencarian
	 * aktif. Digunakan untuk query data grid maupun penghitungan total untuk paging.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat kriteria pada entitas {@link PenerimaanPengadaanMasterAsset} dengan alias ke tabel
	 * pemesananPengadaanMasterAsset. Menambahkan filter: status belum/sudah diposting (dari
	 * checkbox), hanya yang sudah disetujui (disetujuiOleh tidak null), nilai tidak nol,
	 * rentang tanggal persetujuan, filter pemilik aset, filter lokasi, filter ruang, dan filter
	 * kode (pencarian pada kode pemesanan, kode tagihan, atau kode penerimaan). Jika order true,
	 * ditambahkan urutan descending berdasarkan id.
	 *
	 * <b>Parameter:</b><br>
	 * @param order jika true, tambahkan ORDER BY id DESC
	 *
	 * <b>Return:</b><br>
	 * @return objek {@link Criteria} siap dieksekusi atau ditambahkan filter tambahan
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Filter tanggal menggunakan sqlRestriction pada kolom tanggal_persetujuan (bukan tanggal
	 * transaksi seperti di PostingPembayaranDpAction). Pastikan format tanggal konsisten dengan
	 * {@link Common#databaseDateFormat}. Filter kode menggunakan OR pada tiga kolom berbeda
	 * untuk memudahkan pencarian tanpa harus tahu tipe kode yang digunakan.
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PenerimaanPengadaanMasterAsset.class)

				.createAlias("pemesananPengadaanMasterAsset", "pemesananPengadaanMasterAsset")

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(!searchtelahtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNotNull("postingHistory"))

				.add(Restrictions.isNotNull("disetujuiOleh"))

				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
						+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and  date('"
						+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchpemilikAsset.getSelectedItem() == null
						|| searchpemilikAsset.getSelectedItem().getValue() == null
						|| searchpemilikAsset.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("pemilikAsset", searchpemilikAsset.getSelectedItem().getValue()))
				.add(searchlokasi.getSelectedItem() == null || searchlokasi.getSelectedItem().getValue() == null
						|| searchlokasi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))
				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("pemesananPengadaanMasterAsset.kode", searchkode.getValue(),
										MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("kodeTagihan", searchkode.getValue(), MatchMode.ANYWHERE),
										Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE))));

		// Sub-tab Penyusutan: Fix Aset (tak ada detail CIP) vs Aset dalam Pekerjaan (ada detail CIP).
		if ("pekerjaan".equals(filterKelompok)) {
			criteria.add(Restrictions.sqlRestriction(SQL_EXISTS_KELOMPOK_CIP));
		} else if ("fixasset".equals(filterKelompok)) {
			criteria.add(Restrictions.sqlRestriction("not " + SQL_EXISTS_KELOMPOK_CIP));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <h3>onSearchDefaultTanpaProgress — Memuat Data Grid Tanpa Indikator Progress</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Melakukan query data aktual ke basis data dan memperbarui model grid serta paging,
	 * tanpa menampilkan atau memperbarui indikator progress loading. Dipanggil dari dalam
	 * callback timer yang sudah mengelola state loading secara terpisah.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@link #initCriteria(boolean)} dua kali: tanpa urutan untuk paging, dengan
	 * urutan untuk query halaman aktif. Hasil dibungkus dalam {@link SimpleListModel} dan
	 * di-set ke grid dengan renderer {@link PenerimaanPengadaanMasterAssetDetailRenderer}.
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dilempar ke pemanggil dan ditangkap oleh blok finally di
	 * {@link #loadDataDenganProgressPosting(Event)}.
	 *
	 * @param event event pencarian, dapat null jika dari inisialisasi
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenerimaanPengadaanMasterAsset> penerimaanPengadaanMasterAssetDetail = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penerimaanPengadaanMasterAssetDetail);
		grid.setRowRenderer(new PenerimaanPengadaanMasterAssetDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <h3>onSearchDefault — Entry Point Pencarian Data dari Event ZK</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Metode delegasi yang menjadi entry point standar untuk semua event pencarian dari
	 * komponen ZUL. Mendelegasikan ke {@link #loadDataDenganProgressPosting(Event)} yang
	 * mengelola state loading dan progress indicator.
	 *
	 * <b>Cara kerja:</b><br>
	 * Langsung mendelegasikan ke loadDataDenganProgressPosting. Tidak ada logika tambahan
	 * di metode ini. Dipanggil oleh: onChange filter, onPaging, callback setelah posting.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jangan tambahkan logika langsung di sini; gunakan onSearchDefaultTanpaProgress.
	 *
	 * @param event event ZK yang memicu pencarian, dilewatkan ke metode loading
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Flag yang menandakan apakah proses loading data posting jurnal sedang berjalan.
	 * Digunakan untuk mencegah eksekusi ganda yang dapat menyebabkan kondisi balapan pada UI.
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag yang menandakan ada permintaan reload tertunda saat loading sedang aktif.
	 * Jika true, setelah loading selesai, loading akan diulang sekali untuk memuat data terbaru.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * <h3>loadDataDenganProgressPosting — Memuat Data Grid dengan Indikator Progress</h3>
	 *
	 * <b>Tujuan:</b><br>
	 * Memuat data grid penerimaan pengadaan dengan indikator progress loading yang ditampilkan
	 * ke pengguna. Mengelola state loading untuk mencegah eksekusi ganda dan menangani
	 * permintaan reload yang datang saat proses loading sedang berlangsung.
	 *
	 * <b>Cara kerja:</b><br>
	 * Jika loading sedang aktif ({@code postingJurnalLoadingAktif} true), permintaan baru dicatat
	 * sebagai tertunda dan metode keluar. Jika tidak, flag aktif di-set true, progress indicator
	 * ditampilkan. Proses loading dijalankan dalam callback timer ZK. Di dalam callback,
	 * {@link #onSearchDefaultTanpaProgress(Event)} dipanggil dengan update progress bertahap.
	 * Blok finally memastikan flag selalu di-reset dan mengecek reload tertunda.
	 *
	 * <b>Threading:</b><br>
	 * Seluruh metode berjalan di event thread ZK menggunakan timer. Tidak ada thread terpisah
	 * di sini (berbeda dengan thread di onPostingSemua).
	 *
	 * <b>Penanganan error:</b><br>
	 * Blok try-finally memastikan flag loading selalu di-reset meskipun terjadi exception.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Nilai persentase progress (7, 48, 92, 96, 100) bersifat kosmetik. Jangan tambahkan
	 * return statement sebelum blok finally tanpa pertimbangan yang matang.
	 *
	 * @param event event ZK yang memicu pemuatan data, dapat null dari inisialisasi
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
