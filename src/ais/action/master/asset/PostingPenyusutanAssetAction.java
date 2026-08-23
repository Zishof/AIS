package ais.action.master.asset;

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

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.asset.util.AssetUtil;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.PemilikAsset;
import ais.database.model.asset.PenyusutanAsset;
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
 * <h3>PostingPenyusutanAssetAction — Posting Jurnal Penyusutan Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini mengelola seluruh alur kerja posting jurnal akuntansi untuk transaksi
 * penyusutan aset (depresiasi). Penyusutan aset adalah pengakuan penurunan nilai
 * ekonomis aset tetap secara berkala (per bulan) sepanjang umur ekonomisnya.
 * Setiap record {@code PenyusutanAsset} merepresentasikan penyusutan satu bulan
 * untuk satu unit aset detail. Kelas ini menyediakan antarmuka ZK untuk memfilter
 * data penyusutan berdasarkan berbagai kriteria (rentang tanggal, pemilik aset,
 * lokasi, ruangan, barcode, nama master aset), menampilkan preview jurnal akuntansi
 * (akun debet biaya penyusutan dan akun kredit akumulasi penyusutan), serta
 * melakukan posting dan pembatalan posting secara individual maupun massal.<br>
 * <br>
 *
 * <b>Cara kerja:</b><br>
 * Setelah ZK menginisialisasi composer melalui {@code doAfterCompose}, sistem
 * mengisi filter combo (pemilik aset, lokasi), mengunci lokasi jika ada session
 * attribute khusus, mengambil parameter tanggal dari {@code PostingJurnalHelper},
 * dan memuat data penyusutan pertama kali melalui {@code loadDataDenganProgressPosting}.
 * Setiap baris ditampilkan oleh inner class {@code PenyusutanAssetRenderer} yang
 * menunjukkan barcode, pemilik aset, satuan kerja, nama aset, ruangan, nilai
 * penyusutan, tanggal penyusutan, preview jurnal debet/kredit, status posting dengan
 * nomor bukti, dan tombol aksi. Akun untuk jurnal diperoleh via
 * {@code AssetUtil.ambilDataAkun} yang mendukung override per satuan kerja.
 * Posting massal berjalan di thread terpisah untuk menghindari pemblokiran UI.
 * Mekanisme progress ({@code PostingJurnalLoadingUtil}) dan koalesensi reload
 * ({@code postingJurnalLoadingAktif}/{@code postingJurnalReloadTertunda}) memastikan
 * pengalaman pengguna yang responsif.<br>
 * <br>
 *
 * <b>Threading:</b><br>
 * Posting massal ({@code onPostingSemua}) menjalankan iterasi di thread Java baru
 * menggunakan {@code HibernateUtil.currentNativeSession()}. Setiap baris penyusutan
 * diproses dalam transaksi terpisah (begin → saveTransaksi → commit → update entitas →
 * commit) untuk menghindari transaksi yang terlalu panjang. Thread latar memanggil
 * {@code label.setValue} untuk memperbarui progress bar yang tampak di UI.
 * Variabel flag loading menggunakan akses single-thread ZK UI sehingga tidak
 * memerlukan sinkronisasi eksplisit.<br>
 * <br>
 *
 * <b>Pemeliharaan:</b><br>
 * Akun debet ({@code akunBiayaPenyusutan}) dan akun kredit ({@code akunPenyusutan})
 * dikonfigurasi di level {@code MasterAsset} dan dapat di-override per satuan kerja
 * melalui {@code AssetUtil.ambilDataAkun}. Pastikan setiap master aset memiliki
 * kedua akun ini dikonfigurasi sebelum penyusutan dapat diposting. Parameter
 * {@code sudahPostingDasbor} mendukung navigasi dari dasbor draft jurnal sehingga
 * filter status posting dapat dipre-set dari luar.
 */
public class PostingPenyusutanAssetAction extends GenericAutowireComposer {

	/**
	 * Versi serial untuk serialisasi kelas ini sesuai mekanisme ZK composer.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar record penyusutan aset. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman data grid. */
	private Paging paging;

	/** Kotak teks filter berdasarkan nama master aset atau kode. */
	private Textbox searchkode;

	/** Kotak teks filter berdasarkan barcode unit aset. */
	private Textbox searchbarkode;

	/** Combobox filter berdasarkan pemilik aset. */
	private Combobox searchpemilikAsset;

	/** Combobox filter berdasarkan lokasi aset. */
	private Combobox searchlokasi;

	/** Banbox filter berdasarkan ruangan spesifik. */
	private AmbilDataRuangBanbox searchruang;

	/** Checkbox filter untuk menampilkan hanya data yang belum diposting. */
	private MyCheckboxConfig searchtampil;

	/** Checkbox filter untuk menampilkan hanya data yang sudah diposting. */
	private MyCheckboxConfig searchtelahtampil;

	/** Flag apakah pengguna memiliki hak UPDATE (menampilkan tombol posting). */
	private boolean edit = false;

	/** Tombol toolbar untuk memulai proses posting massal. */
	private MyToolbarbuttonConfig sent;

	/** Flag apakah pengguna adalah admin atau memiliki hak APPROVE (batalkan posting). */
	public boolean adminLain;

	/** Datebox tanggal awal filter rentang penyusutan. */
	private MyDatebox tglMulai;

	/** Datebox tanggal akhir filter rentang penyusutan. */
	private MyDatebox tglSampai;

	/** Pengguna yang sedang login, digunakan untuk mengisi posting history. */
	private Tbmuser tbmuser;

	/**
	 * <b>Tujuan:</b> Dipanggil ZK sebelum composer di-compose ke halaman.
	 * Melakukan pengecekan keamanan untuk memastikan halaman hanya diakses
	 * oleh pengguna yang terautentikasi.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk verifikasi sesi dan hak
	 * akses, kemudian mendelegasikan ke superclass untuk melanjutkan proses compose.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code page} — halaman ZK yang sedang dimuat.<br>
	 * {@code parent} — komponen induk.<br>
	 * {@code compInfo} — metadata komponen ZK.<br>
	 * <br>
	 * <b>Return:</b> {@code ComponentInfo} dari superclass.<br>
	 * <br>
	 * <b>Penanganan error:</b> Jika sesi tidak valid, pengguna diarahkan ke logoff.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jangan hapus pemanggilan {@code Common.doCheckSecurity()}.
	 *
	 * @param page     halaman ZK yang sedang di-compose
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo informasi metadata komponen ZK
	 * @return ComponentInfo hasil superclass
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Metode lifecycle ZK yang dipanggil setelah seluruh komponen
	 * halaman selesai di-wire ke field composer. Menginisialisasi penuh halaman
	 * posting penyusutan: validasi sesi, pengisian filter, pengaturan rentang tanggal,
	 * konfigurasi tombol, dan pemuatan data awal.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * 1. Memanggil super.doAfterCompose dan Common.initLaguage.<br>
	 * 2. Memvalidasi sesi (usersTemp) dan hak READ; jika gagal diarahkan ke logoff.<br>
	 * 3. Mengambil pengguna aktif.<br>
	 * 4. Menginisialisasi rentang tanggal default: tglMulai 6 bulan lalu, tglSampai hari ini,
	 *    keduanya readonly.<br>
	 * 5. Menentukan flag adminLain dan edit dari hak akses pengguna.<br>
	 * 6. Guard: jika komponen sent null, return early (mode view-only).<br>
	 * 7. Mengisi combo pemilik aset dan lokasi dari database (hanya yang aktif).<br>
	 * 8. Mengunci lokasi dari session attribute jika tersedia (navigasi dari modul lain).<br>
	 * 9. Mengambil parameter sudahPosting dan tanggal dari {@code PostingJurnalHelper}.<br>
	 * 10. Memuat data via {@code loadDataDenganProgressPosting}.<br>
	 * 11. Menginisialisasi event paging dan setup filter lanjutan.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code comp} — root komponen ZK hasil compose.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception akan dipropagasikan ke ZK. Guard null pada
	 * komponen UI mencegah NPE jika komponen opsional tidak ada di ZUL.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jika filter baru ditambahkan ke ZUL, inisialisasi nilainya
	 * di sini. Parameter {@code Lokasi} dari session attribute memungkinkan navigasi
	 * dari halaman lain dengan lokasi pre-selected.
	 *
	 * @param comp komponen root ZK hasil compose
	 * @throws Exception jika inisialisasi gagal karena error Hibernate atau ZK
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

		sudahPostingDasbor = ais.action.master.helper.PostingJurnalHelper.ambilParameterSudahPosting();
		ais.action.master.helper.PostingJurnalHelper.terapkanParameterTanggal(tglMulai, tglSampai);

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
	 * <b>Tujuan:</b> Menangani aksi pengguna untuk membatalkan posting semua transaksi
	 * penyusutan yang sudah pernah diposting, sesuai filter aktif.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Menampilkan dialog konfirmasi. Jika pengguna memilih OK, mengambil semua
	 * {@code PenyusutanAsset} yang memiliki postingHistory (tidak null) sesuai
	 * {@code initCriteria}. Untuk setiap record, mereset postingHistory ke null
	 * dan menghapus GrupTransaksi terkait dari skema akunting (hanya yang belum
	 * closing). Setelah selesai, me-reload data via timer default.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK dari tombol batalkan posting semua.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception akan dipropagasikan. Disarankan menambahkan
	 * try-catch per-item agar satu kegagalan tidak menghentikan seluruh proses.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Query SQL native harus disesuaikan jika nama kolom atau
	 * skema database berubah. Kolom penyusutan_asset di grup_transaksi adalah FK
	 * ke tabel penyusutan_asset.
	 *
	 * @param event event ZK yang memicu aksi pembatalan posting massal
	 * @throws Exception jika terjadi error saat akses database
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi penyusutan ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<PenyusutanAsset> penyusutanAssets = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (PenyusutanAsset penyusutanAsset : penyusutanAssets) {
								penyusutanAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(penyusutanAsset);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where penyusutan_asset="
												+ penyusutanAsset.getId() + " and closing is null")
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
	 * <b>Tujuan:</b> Menangani aksi pengguna untuk memposting semua transaksi
	 * penyusutan yang belum diposting secara massal dalam satu operasi batch.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Membuka dialog modal dengan form input tanggal posting dan keterangan.
	 * Setelah validasi dan konfirmasi ganda, membuat satu entitas {@code PostingHistory}
	 * baru dengan jenis {@code JENIS_PENYUSUTAN_ASET}. Di thread latar, mengiterasi
	 * semua PenyusutanAsset belum diposting sesuai filter aktif. Untuk setiap record
	 * yang valid (master aset tidak null), mengambil akun debet (biaya penyusutan)
	 * dan kredit (akumulasi penyusutan) via {@code AssetUtil.ambilDataAkun}. Jika
	 * kedua akun ada, membuat jurnal via {@code CommonAkunting.saveTransaksi} dengan
	 * arah debet/kredit ditentukan tanda nilai (positif atau koreksi negatif). Progress
	 * bar diperbarui per item. Setelah selesai, sesi Hibernate ditutup dan UI di-refresh.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK dari tombol "Posting Semua".<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Setiap item diproses dalam try-catch terpisah.
	 * Error transaksi Hibernate per-item di-catch dan dilaporkan via
	 * {@code Common.tampilErrorJikaAdmin}. Exception outer akan dipropagasikan.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jenis posting {@code PostingHistory.JENIS_PENYUSUTAN_ASET}
	 * harus konsisten dengan konstanta di modul pelaporan. Transaksi Hibernate dibuka
	 * dan ditutup per-item di thread latar untuk menghindari transaksi yang terlalu
	 * panjang dan lock database.
	 *
	 * @param event event ZK yang memicu aksi posting massal
	 * @throws Exception jika dialog gagal dibuat atau terjadi error UI ZK
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Penyusutan");
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
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting Penyusutan belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal dan pilih tanggal dari kalender; (2) Pastikan tanggal berada dalam periode akuntansi yang aktif; (3) ulangi proses posting penyusutan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi penyusutan ?", "Pertanyaan",
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
											MyMessageboxConfig.show("Posting transaksi penyusutan berhasil dilakukan",
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
													PostingHistory.JENIS_PENYUSUTAN_ASET);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<PenyusutanAsset> penyusutanAssets = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (PenyusutanAsset penyusutanAsset : penyusutanAssets) {

												if (penyusutanAsset != null && penyusutanAsset.getAssetDetail()
														.getAsset().getMasterAsset() != null) {

													try {

														Akun akunDebet = AssetUtil.ambilDataAkun(
																penyusutanAsset.getAssetDetail().getAsset()
																		.getMasterAsset().getAkunBiayaPenyusutan(),
																penyusutanAsset.getAssetDetail().getSatuanKerja());

														Akun akunKredit = AssetUtil.ambilDataAkun(
																penyusutanAsset.getAssetDetail().getAsset()
																		.getMasterAsset().getAkunPenyusutan(),
																penyusutanAsset.getAssetDetail().getSatuanKerja());

														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {

																ket = "Penyusutan \""
																		+ (penyusutanAsset.getAssetDetail().getBarcode()
																				+ "-"
																				+ penyusutanAsset
																						.getAssetDetail().getNama())
																		+ "\" bulan ke "
																		+ Common.numberFormat.get()
																				.format(penyusutanAsset.getTahunKe())
																		+ " dari sebanyak "
																		+ Common.numberFormat.get().format(penyusutanAsset
																				.getAssetDetail().getUmurEkonomis())
																		+ " bulan nilai buku "
																		+ Common.numberFormat.get()
																				.format(penyusutanAsset.getNilaiBuku())
																		+ " " + penyusutanAsset.getKeterangan() + " "
																		+ penyusutanAsset.getAssetDetail()
																				.getKeterangan();

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(ket + " ("
																	+ Common.numberFormat.get().format(
																			rowIndex * 100.0 / penyusutanAssets.size())
																	+ " %)");

															Double nilai = penyusutanAsset.getNilaiPenyusutan();
															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																session.getTransaction().begin();
																if (nilai > 0.1) {
																	CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			penyusutanAsset.getPerTanggal(), nilai,
																			denda, penyusutanAsset, penyusutanAsset
																					.getAssetDetail().getSatuanKerja(),
																			session);
																} else {
																	CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			penyusutanAsset.getPerTanggal(), nilai,
																			denda, penyusutanAsset, penyusutanAsset
																					.getAssetDetail().getSatuanKerja(),
																			session);
																}
																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															penyusutanAsset.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(penyusutanAsset);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingPenyusutanAssetAction.java:594");
														// error per-item diabaikan agar batch tetap berlanjut
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
	 * <b>Tujuan:</b> Inner class renderer baris grid yang menampilkan satu entitas
	 * {@code PenyusutanAsset} beserta aksi posting dan batalkan posting per baris.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Dipanggil ZK untuk setiap objek dalam model list grid. Menampilkan: barcode
	 * unit aset, pemilik aset, satuan kerja, nama aset, ruangan, nilai penyusutan
	 * (dengan link revisi riwayat perubahan), tanggal penyusutan, preview jurnal
	 * debet/kredit (atau pesan error jika akun tidak ada), status posting dengan nomor
	 * bukti, dan tombol aksi (batalkan/posting). Akun diambil via
	 * {@code AssetUtil.ambilDataAkun} yang mendukung override per satuan kerja.
	 * Nomor bukti diambil via proyeksi property "kode" dari GrupTransaksi terkait.<br>
	 * <br>
	 * <b>Threading:</b> Berjalan di thread UI ZK, menggunakan sesi Hibernate managed.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Urutan kolom harus sesuai dengan deklarasi kolom di ZUL.
	 * Perubahan pada struktur relasi PenyusutanAsset → AssetDetail → Asset → MasterAsset
	 * akan mempengaruhi logika pengambilan akun di renderer ini.
	 */
	class PenyusutanAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data penyusutan aset ke dalam komponen
		 * ZK Row, menampilkan semua informasi relevan dan tombol aksi posting.<br>
		 * <br>
		 * <b>Cara kerja:</b><br>
		 * Meng-cast {@code arg1} ke {@code PenyusutanAsset}. Menambahkan Label untuk
		 * barcode, pemilik, satuan kerja, nama, ruangan. Menampilkan nilai penyusutan
		 * via {@code RevisiHelper} untuk mendukung riwayat perubahan. Menampilkan
		 * tanggal penyusutan. Mengambil akun debet dan kredit lalu menampilkan preview
		 * jurnal atau pesan validasi jika akun tidak ada. Mengambil nomor bukti dari
		 * GrupTransaksi dan menampilkan status posting. Menambahkan tombol batalkan
		 * dan posting per baris (visibilitas dikontrol oleh flag edit dan adminLain).<br>
		 * <br>
		 * <b>Parameter:</b><br>
		 * {@code arg0} — Row ZK tempat komponen ditambahkan.<br>
		 * {@code arg1} — objek {@code PenyusutanAsset} yang dirender.<br>
		 * <br>
		 * <b>Return:</b> void<br>
		 * <br>
		 * <b>Penanganan error:</b> Exception dipropagasikan ke ZK exception handler.<br>
		 * <br>
		 * <b>Pemeliharaan:</b> Pastikan urutan appendChild ke Row sesuai urutan kolom ZUL.
		 *
		 * @param arg0 Row ZK yang akan diisi komponen
		 * @param arg1 objek data PenyusutanAsset yang akan dirender
		 * @throws Exception jika terjadi error saat query atau konstruksi komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PenyusutanAsset penyusutanAsset = (PenyusutanAsset) arg1;
			new Label(penyusutanAsset.getAssetDetail().getBarcode()).setParent(arg0);

			new Label(penyusutanAsset.getAssetDetail().getPemilikAsset() == null ? ""
					: penyusutanAsset.getAssetDetail().getPemilikAsset().getNama()).setParent(arg0);

			new Label(penyusutanAsset.getAssetDetail().getSatuanKerja() == null ? ""
					: penyusutanAsset.getAssetDetail().getSatuanKerja().getNama()).setParent(arg0);
			new Label(penyusutanAsset.getAssetDetail().getNama()).setParent(arg0);
			new Label(penyusutanAsset.getAssetDetail().getRuang() == null ? ""
					: penyusutanAsset.getAssetDetail().getRuang().getNama()).setParent(arg0);

			Double nilai = penyusutanAsset.getNilaiPenyusutan();

			RevisiHelper.createNewRevisi(PenyusutanAsset.class, penyusutanAsset, Common.numberFormat.get().format(nilai))
					.setParent(arg0);

			new Label(Common.dateFormat4.get().format(penyusutanAsset.getPerTanggal())).setParent(arg0);

			Akun akunDebet = AssetUtil.ambilDataAkun(
					penyusutanAsset.getAssetDetail().getAsset().getMasterAsset().getAkunBiayaPenyusutan(),
					penyusutanAsset.getAssetDetail().getSatuanKerja());

			Akun akunKredit = AssetUtil.ambilDataAkun(
					penyusutanAsset.getAssetDetail().getAsset().getMasterAsset().getAkunPenyusutan(),
					penyusutanAsset.getAssetDetail().getSatuanKerja());

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
					.add(Restrictions.eq("penyusutanAsset", penyusutanAsset)).setMaxResults(1)
					.setProjection(Projections.property("kode")).uniqueResult();

			new Label(penyusutanAsset.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: penyusutanAsset.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && penyusutanAsset.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								penyusutanAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(penyusutanAsset);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where penyusutan_asset="
												+ penyusutanAsset.getId() + " and closing is null")
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
				button.setVisible(edit && penyusutanAsset.getPostingHistory() == null && tbmuser != null
						&& tbmuser.ambilSatuanKerja() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PENYUSUTAN_ASET);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunDebet = AssetUtil.ambilDataAkun(
										penyusutanAsset.getAssetDetail().getAsset().getMasterAsset()
												.getAkunBiayaPenyusutan(),
										penyusutanAsset.getAssetDetail().getSatuanKerja());

								Akun akunKredit = AssetUtil.ambilDataAkun(
										penyusutanAsset.getAssetDetail().getAsset().getMasterAsset()
												.getAkunPenyusutan(),
										penyusutanAsset.getAssetDetail().getSatuanKerja());

								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {

										ket = "Penyusutan \""
												+ (penyusutanAsset.getAssetDetail().getBarcode() + "-"
														+ penyusutanAsset.getAssetDetail().getNama())
												+ "\" bulan ke "
												+ Common.numberFormat.get().format(penyusutanAsset.getTahunKe())
												+ " dari sebanyak "
												+ Common.numberFormat.get()
														.format(penyusutanAsset.getAssetDetail().getUmurEkonomis())
												+ " bulan nilai buku "
												+ Common.numberFormat.get().format(penyusutanAsset.getNilaiBuku()) + " "
												+ penyusutanAsset.getKeterangan() + " "
												+ penyusutanAsset.getAssetDetail().getKeterangan();

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;
									Double nilai = penyusutanAsset.getNilaiPenyusutan();

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket, penyusutanAsset.getPerTanggal(),
												nilai, denda, penyusutanAsset, tbmuser.ambilSatuanKerja(), session);
									} else {
										CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket, penyusutanAsset.getPerTanggal(),
												nilai, denda, penyusutanAsset, tbmuser.ambilSatuanKerja(), session);
									}

									penyusutanAsset.setPostingHistory(postingHistory);
									session.update(penyusutanAsset);
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
	 * <b>Tujuan:</b> Membangun objek {@code Criteria} Hibernate yang merangkum semua
	 * kondisi filter aktif untuk mengambil data {@code PenyusutanAsset} dari database.
	 * Criteria ini digunakan untuk paging, pengambilan data halaman, maupun operasi
	 * massal (posting/batalkan semua).<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Guard null pada {@code searchtampil} mengembalikan null jika komponen belum siap.
	 * Membuat criteria pada entitas {@code PenyusutanAsset} dengan join ke alias
	 * assetDetail, asset, dan masterAsset. Filter diterapkan: status posting dari
	 * dasbor ({@code restriksiPosting}), belum/sudah diposting (checkbox), nilai
	 * penyusutan tidak nol, rentang tanggal (pertanggal), pemilik aset, lokasi,
	 * ruangan, barcode, dan nama/kode master aset. Kondisi tidak aktif menggunakan
	 * sqlRestriction("1=1") sebagai passthrough. Jika {@code order} true, diurutkan
	 * berdasarkan ID descending.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code order} — apakah hasil harus diurutkan berdasarkan ID descending.<br>
	 * <br>
	 * <b>Return:</b> Objek {@code Criteria} siap dieksekusi, atau null jika komponen
	 * filter belum diinisialisasi.<br>
	 * <br>
	 * <b>Penanganan error:</b> Guard null pada searchtampil mencegah NPE saat metode
	 * dipanggil sebelum komponen siap. Guard null pada tglMulai/tglSampai mencegah
	 * NullPointerException pada pemformatan tanggal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Kolom database yang digunakan dalam sqlRestriction
	 * (pertanggal) harus konsisten dengan nama kolom aktual di tabel penyusutan_asset.
	 *
	 * @param order apakah hasil harus diurutkan secara descending berdasarkan ID
	 * @return objek Criteria Hibernate atau null jika filter belum siap
	 */
	public Criteria initCriteria(boolean order) {
		if (searchtampil == null) return null;
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PenyusutanAsset.class).add(ais.action.master.helper.PostingJurnalHelper.restriksiPosting("postingHistory", sudahPostingDasbor))

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(!searchtelahtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNotNull("postingHistory"))

				.createAlias("assetDetail", "assetDetail").createAlias("assetDetail.asset", "asset")
				.createAlias("asset.masterAsset", "masterAsset")

				.add(Restrictions.ne("nilaiPenyusutan", 0.0)).add(Restrictions.isNotNull("nilaiPenyusutan"))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(pertanggal) between date('" + Common.databaseDateFormat.get().format(tglMulai.getValue())
								+ "') and  date('" + Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchpemilikAsset.getSelectedItem() == null
						|| searchpemilikAsset.getSelectedItem().getValue() == null
						|| searchpemilikAsset.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("asset.pemilikAsset",
										searchpemilikAsset.getSelectedItem().getValue()))
				.add(searchlokasi.getSelectedItem() == null || searchlokasi.getSelectedItem().getValue() == null
						|| searchlokasi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("asset.lokasi", searchlokasi.getSelectedItem().getValue()))
				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("asset.ruang", searchruang.getAttribute("ruang"))))

				.add(searchbarkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("masterassetDetail.barcode", searchbarkode.getValue().trim(),
								MatchMode.ANYWHERE))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("masterAsset.nama", searchkode.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("masterAsset.kode", searchkode.getValue().trim(),
										MatchMode.ANYWHERE)));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Melakukan pencarian dan pemuatan data penyusutan ke grid tanpa
	 * indikator progress. Metode inti yang dipanggil dari dalam mekanisme progress wrapper.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.initPaging} untuk menghitung total dan state paging,
	 * lalu mengambil satu halaman data via {@code initCriteria(true)} dengan limit
	 * dan offset sesuai halaman aktif. Membungkus hasil dalam SimpleListModel dan
	 * menerapkan renderer ke grid.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK asal pemanggil, tidak digunakan langsung.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate akan dipropagasikan.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jangan panggil metode ini langsung dari event handler ZK;
	 * gunakan {@code loadDataDenganProgressPosting} agar progress ditampilkan.
	 *
	 * @param event event ZK asal pemanggil, boleh null
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenyusutanAsset> penyusutanAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penyusutanAsset);
		grid.setRowRenderer(new PenyusutanAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Entry point publik untuk memuat ulang data grid penyusutan,
	 * dipanggil dari event handler ZK maupun callback setelah operasi posting/batalkan.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Mendelegasikan sepenuhnya ke {@code loadDataDenganProgressPosting(event)}
	 * agar mekanisme progress dan koalesensi reload berjalan dengan benar.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK pemicu, diteruskan ke delegate.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Didelegasikan ke metode yang dipanggil.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Nama metode onSearchDefault wajib dipertahankan karena
	 * digunakan oleh ZK event binding konvensional.
	 *
	 * @param event event ZK pemicu reload
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Status posting yang dipre-set dari navigasi dasbor draft jurnal.
	 * Jika null, berarti halaman dibuka langsung dari menu (tidak ada pre-filter status posting).
	 * Nilai ini digunakan oleh {@code PostingJurnalHelper.restriksiPosting} dalam criteria.
	 */
	private Boolean sudahPostingDasbor = null;

	/**
	 * Flag apakah proses loading data posting jurnal penyusutan sedang berjalan.
	 * Digunakan untuk koalesensi reload agar tidak ada dua proses loading yang berjalan bersamaan.
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag apakah ada permintaan reload yang masuk saat loading sedang aktif.
	 * Jika true setelah loading selesai, sistem akan otomatis melakukan reload ulang.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * <b>Tujuan:</b> Memuat data posting jurnal penyusutan ke grid dengan menampilkan
	 * indikator progress bertahap dan mencegah eksekusi ganda (koalesensi reload).<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Jika loading sedang aktif, permintaan baru ditandai tertunda dan progress diperbarui
	 * untuk memberi informasi kepada pengguna. Jika tidak aktif, flag diset, progress
	 * ditampilkan, dan data dimuat via timer ZK agar UI sempat diperbarui sebelum query
	 * berjalan. Setelah query selesai (di blok try), progress diperbarui ke tahap hampir
	 * selesai. Di blok finally, flag loading direset. Jika ada reload tertunda, dilakukan
	 * rekursi via timer baru; jika tidak, progress ditandai selesai 100%.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK original pemicu reload, diteruskan ke pencarian data
	 * dan reload tertunda.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Query dalam blok try dilindungi oleh finally sehingga
	 * flag loading selalu direset. Exception dipropagasikan ke ZK handler.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Persentase progress (7, 48, 92, 96, 100) dapat disesuaikan
	 * untuk pengalaman visual. Flag-flag ini aman diakses single-thread di event thread ZK.
	 *
	 * @param event event ZK pemicu, dapat null jika dipanggil dari inisialisasi
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
	// JALUR NON-ZK: baris "Jurnal Penyusutan" pada dasbor Draft Jurnal
	// PEMELIHARAAN: akun & nilai HARUS tetap identik dengan {@link #onPostingSemua}.
	// =====================================================================

	/**
	 * Kriteria penyusutan aset yang layak dijurnal -- sama dengan penghitung baris
	 * "Jurnal Penyusutan" pada dasbor: nilai penyusutan bukan nol dan
	 * {@code date(pertanggal)} di dalam rentang.
	 */
	private static Criteria kriteriaPostingPenyusutanStatic(Session session, java.util.Date mulai,
			java.util.Date sampai) {
		Criteria c = session.createCriteria(PenyusutanAsset.class)
				.add(Restrictions.ne("nilaiPenyusutan", 0.0))
				.add(Restrictions.isNotNull("nilaiPenyusutan"));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(this_.pertanggal) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/** Batalkan posting SEMUA jurnal penyusutan dalam rentang. */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemuaPenyusutan(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<PenyusutanAsset> daftar = kriteriaPostingPenyusutanStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (PenyusutanAsset susut : daftar) {
				try {
					String syarat = "penyusutan_asset=" + susut.getId() + " and closing is null";
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where " + syarat + ")").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where " + syarat)
							.executeUpdate();
					susut.setPostingHistory(null);
					session.update(susut);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingPenyusutanAssetAction jalur API");
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
	 * Posting SEMUA jurnal penyusutan yang belum dibuat dalam rentang.
	 *
	 * <p>Debet = {@code masterAsset.akunBiayaPenyusutan} (bebannya), kredit =
	 * {@code masterAsset.akunPenyusutan} (akumulasi penyusutannya) -- keduanya lewat
	 * {@code AssetUtil.ambilDataAkun} dengan satuan kerja detail asetnya, supaya akun per
	 * satuan kerja yang terpakai. Nilai dari {@code nilaiPenyusutan}, tanggal jurnal dari
	 * {@code perTanggal}; bila nilainya &le; 0,1 posisi ditukar.</p>
	 *
	 * <p><b>Dua penyimpangan sadar:</b> dokumen berakun tidak lengkap dilewati, dan penanda
	 * posting hanya dipasang bila jurnalnya benar-benar tersimpan.</p>
	 */
	@SuppressWarnings("unchecked")
	public static int postingSemuaPenyusutan(java.util.Date mulai, java.util.Date sampai,
			Tbmuser oleh, java.util.Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<Long> ids = kriteriaPostingPenyusutanStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory"))
					.setProjection(org.hibernate.criterion.Projections.property("id")).list();

			PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_PENYUSUTAN_ASET);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal jurnal penyusutan dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			for (Long id : ids) {
				try {
					session = HibernateUtil.currentNativeSession();
					PenyusutanAsset susut = (PenyusutanAsset) session
							.createCriteria(PenyusutanAsset.class).add(Restrictions.idEq(id)).uniqueResult();
					if (susut == null || susut.getAssetDetail() == null
							|| susut.getAssetDetail().getAsset() == null
							|| susut.getAssetDetail().getAsset().getMasterAsset() == null) {
						continue;
					}
					Akun akunDebet = AssetUtil.ambilDataAkun(
							susut.getAssetDetail().getAsset().getMasterAsset().getAkunBiayaPenyusutan(),
							susut.getAssetDetail().getSatuanKerja());
					Akun akunKredit = AssetUtil.ambilDataAkun(
							susut.getAssetDetail().getAsset().getMasterAsset().getAkunPenyusutan(),
							susut.getAssetDetail().getSatuanKerja());
					if (akunDebet == null || akunKredit == null) {
						continue;
					}

					String ket = "Penyusutan \"" + susut.getAssetDetail().getBarcode() + "-"
							+ susut.getAssetDetail().getNama() + "\" bulan ke "
							+ Common.numberFormat.get().format(susut.getTahunKe()) + " dari sebanyak "
							+ Common.numberFormat.get().format(susut.getAssetDetail().getUmurEkonomis())
							+ " bulan nilai buku "
							+ Common.numberFormat.get().format(susut.getNilaiBuku()) + " "
							+ susut.getKeterangan() + " " + susut.getAssetDetail().getKeterangan();
					Double nilai = susut.getNilaiPenyusutan();

					boolean tersimpan = false;
					try {
						session.getTransaction().begin();
						if (nilai > 0.1) {
							CommonAkunting.saveTransaksi(akunDebet, akunKredit, null, null, postingHistory,
									true, ket, susut.getPerTanggal(), nilai, Double.valueOf(0.0), susut,
									susut.getAssetDetail().getSatuanKerja(), session);
						} else {
							CommonAkunting.saveTransaksi(akunKredit, akunDebet, null, null, postingHistory,
									true, ket, susut.getPerTanggal(), nilai, Double.valueOf(0.0), susut,
									susut.getAssetDetail().getSatuanKerja(), session);
						}
						session.getTransaction().commit();
						tersimpan = true;
					} catch (Exception e) {
						try {
							session.getTransaction().rollback();
						} catch (Exception ex) {
							// rollback gagal: kegagalan aslinya yang dilaporkan
						}
						ais.common.ErrorAuditUtil.record(e, "PostingPenyusutanAssetAction jalur API");
					}

					if (tersimpan) {
						susut.setPostingHistory(postingHistory);
						session.getTransaction().begin();
						session.update(susut);
						session.getTransaction().commit();
						n++;
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "PostingPenyusutanAssetAction jalur API");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingPenyusutanAssetAction jalur API");
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
