package ais.action.master.asset;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.helper.asset.LaporanPenyusutan;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.AssetDetail;
import ais.database.model.asset.PenyusutanAsset;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h3>AssetDetailAction — Manajemen Detail Unit Aset dan Penyusutan</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini mengelola tampilan dan operasi data {@code AssetDetail}, yaitu unit-unit
 * fisik individual dari sebuah aset (misalnya setiap komputer dalam kelompok aset
 * "Komputer"). Setiap AssetDetail memiliki barcode unik, nama, tanggal beli, harga
 * beli, umur ekonomis, dan lokasi (ruangan, satuan kerja). Kelas ini juga menyediakan
 * fitur sinkronisasi penyusutan (depreciation) yang menghitung dan menyimpan record
 * {@code PenyusutanAsset} untuk setiap bulan dari tanggal pembelian hingga tanggal
 * target yang dipilih pengguna. Tab laporan penyusutan disediakan secara lazy-load
 * melalui komponen {@code LaporanPenyusutan}. Tab penyusutan detail dimuat via
 * MyInclude ke halaman ZUL terpisah. Kelas ini mengimplementasikan {@code DataCriteria}
 * dan {@code DataSearchDefault} untuk kompatibilitas dengan utilitas cetak generik.<br>
 * <br>
 *
 * <b>Cara kerja:</b><br>
 * Setelah ZK menginisialisasi composer melalui {@code doAfterCompose}, sistem mengatur
 * rentang tanggal filter default (6 bulan lalu hingga besok), menginisialisasi model
 * pohon satuan kerja untuk filter hierarki, mendaftarkan tombol cetak (export Excel
 * penyusutan) dan tombol sinkronisasi penyusutan. Sinkronisasi penyusutan bekerja
 * dengan mengambil semua AssetDetail yang memenuhi syarat (hargaBeli, nilaiMinimal,
 * tanggalBeli, umurEkonomis > 0), lalu untuk setiap unit menghitung selisih bulan
 * antara tanggal beli dan tanggal target. Untuk setiap bulan yang belum ada record
 * PenyusutanAsset, record baru dibuat. Proses ini berjalan di thread latar dengan
 * pembaruan progress.<br>
 * <br>
 * Setiap baris grid dirender oleh inner class {@code PenyusutanAssetRenderer} yang
 * menampilkan detail expandable (sub-grid penyusutan per bulan), barcode, bulan
 * penyusutan saat ini / umur ekonomis, tanggal beli, tanggal penyusutan terakhir,
 * nilai perolehan, akumulasi penyusutan, nilai buku, dan keterangan dengan link
 * ke dokumen sumber. Metode statis {@code tampilKet} menyediakan komponen keterangan
 * yang dapat digunakan oleh kelas lain (termasuk AssetAction.AssetRenderer).<br>
 * <br>
 *
 * <b>Threading:</b><br>
 * Sinkronisasi penyusutan ({@code onSingkronkan}) berjalan di thread Java baru
 * menggunakan {@code HibernateUtil.currentNativeSession()} per iterasi unit aset,
 * dengan commit per batch bulan. Thread menutup sesi setelah selesai dan memperbarui
 * label progress secara langsung (diperbolehkan oleh ZK push mechanism).<br>
 * <br>
 *
 * <b>Pemeliharaan:</b><br>
 * Perhitungan selisih bulan menggunakan {@code ChronoUnit.MONTHS.between(YearMonth)}
 * yang akurat untuk kalender Gregorian. Nilai umurEkonomis disimpan sebagai jumlah
 * bulan (bukan tahun). Pastikan field {@code tanggalBeli}, {@code hargaBeli},
 * {@code nilaiMinimal}, dan {@code umurEkonomis} terisi sebelum proses sinkronisasi
 * dapat berjalan untuk sebuah unit aset. Kelas ini adalah dual-use: sebagai top-level
 * ZK composer (untuk halaman asset_detail.zul) dan sebagai helper komponen dalam
 * {@code AssetAction.AssetRenderer} (konstruktor tersendiri).
 */
public class AssetDetailAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * Versi serial untuk serialisasi kelas ini sesuai mekanisme ZK composer.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Komponen paging untuk navigasi halaman data grid. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar detail unit aset. */
	private MyGrid grid;

	/** Kotak teks filter berdasarkan nama unit aset. */
	private Textbox searchnama;

	/** Kotak teks filter berdasarkan merk master aset. */
	private Textbox searchmerk;

	/** Combobox filter berdasarkan jenis aset. */
	private Combobox searchjenisAsset;

	/** Banbox filter berdasarkan ruangan spesifik. */
	private AmbilDataRuangBanbox searchruang;

	/** Datebox tanggal awal filter rentang tanggal beli aset. */
	private MyDatebox start;

	/** Datebox tanggal akhir filter rentang tanggal beli aset. */
	private MyDatebox end;

	/** Tombol pencarian/referensi yang digunakan sebagai anchor penambahan tombol toolbar. */
	private MyToolbarbuttonConfig find;

	/** Panel tab untuk laporan penyusutan aset (lazy-load). */
	private Tabpanel laporanPenyusutan;

	/**
	 * <b>Tujuan:</b> Menangani event klik tab "Laporan Penyusutan" untuk memuat
	 * komponen laporan penyusutan secara lazy (hanya jika belum pernah dimuat).<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Memeriksa apakah panel laporan penyusutan sudah memiliki anak komponen.
	 * Jika belum, membuat instance {@code LaporanPenyusutan} dan menambahkannya
	 * langsung ke panel dengan dimensi 100% agar mengisi seluruh area tab.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK dari klik tab laporan penyusutan.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dipropagasikan ke ZK exception handler.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jika {@code LaporanPenyusutan} memerlukan parameter
	 * tambahan di masa depan, sesuaikan di sini.
	 *
	 * @param event event ZK dari klik tab Laporan Penyusutan
	 */
	public void onLaporanPenyusutan(Event event) {

		if (laporanPenyusutan.getChildren().size() == 0) {
			LaporanPenyusutan laporan = new LaporanPenyusutan();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(laporanPenyusutan);
		}
	}

	/** Panel tab untuk tampilan penyusutan detail per unit (lazy-load via MyInclude). */
	private Tabpanel tabPenyusutan;

	/**
	 * <b>Tujuan:</b> Menangani event klik tab "Penyusutan" untuk memuat halaman
	 * detail penyusutan per unit aset secara lazy melalui MyInclude (iframe ZK).<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Memeriksa apakah panel penyusutan sudah memiliki anak komponen. Jika belum,
	 * membuat MyWindow sebagai kontainer dan MyInclude yang mengarah ke ZUL
	 * penyusutan_asset_detail.zul. Menggunakan iframe-style inclusion agar halaman
	 * penyusutan berjalan dalam konteks terpisah.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK dari klik tab Penyusutan.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dipropagasikan ke ZK exception handler.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Path ZUL "/pages/master/asset/penyusutan_asset_detail.zul"
	 * harus tersedia di WebContent. Jika path berubah, perbarui di sini.
	 *
	 * @param event event ZK dari klik tab Penyusutan
	 */
	public void onPenyusutan(Event event) {

		if (tabPenyusutan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabPenyusutan);
			MyInclude iframe = new MyInclude("/pages/master/asset/penyusutan_asset_detail.zul");
			iframe.setParent(window);
		}
	}

	/** Model pohon satuan kerja untuk mendukung filter hierarki dengan anak-cucunya. */
	private SatuanKerjaTreeModel satuanKerjaTreeModel = null;

	/** Banbox filter berdasarkan satuan kerja dengan dukungan hierarki. */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/**
	 * <b>Tujuan:</b> Dipanggil ZK sebelum composer di-compose ke halaman.
	 * Melakukan pengecekan keamanan agar halaman hanya dapat diakses oleh
	 * pengguna terautentikasi.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} lalu mendelegasikan ke superclass.<br>
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
	 * halaman selesai di-wire. Menginisialisasi penuh halaman detail unit aset:
	 * pengaturan paging, filter satuan kerja, filter tanggal, tombol cetak dan
	 * sinkronisasi penyusutan, serta pemuatan data awal.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * 1. Menginisialisasi paging dengan event listener reload.<br>
	 * 2. Mengatur event listener pada searchparent agar perubahan satuan kerja
	 *    memicu reload data.<br>
	 * 3. Membangun {@code SatuanKerjaTreeModel} untuk filter hierarki.<br>
	 * 4. Mengatur datebox start dan end menjadi readonly, dengan nilai default:
	 *    start = 6 bulan lalu, end = besok.<br>
	 * 5. Mendaftarkan tombol cetak (export Excel data penyusutan) via
	 *    {@code Common.cetakData}.<br>
	 * 6. Membuat dan mendaftarkan tombol "Singkronkan Penyusutan" yang membuka
	 *    dialog pemilihan tanggal, lalu menjalankan proses sinkronisasi di thread
	 *    latar menggunakan {@code ChronoUnit.MONTHS.between} untuk menghitung
	 *    selisih bulan dan membuat record PenyusutanAsset yang belum ada.<br>
	 * 7. Memuat data awal via timer default ZK.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code comp} — root komponen ZK hasil compose.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Guard null pada start/end mencegah NPE jika komponen
	 * opsional tidak ada di ZUL. Exception Hibernate dalam thread sinkronisasi
	 * diabaikan per unit untuk mempertahankan kontinuitas batch.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Proses sinkronisasi menggunakan {@code ChronoUnit.MONTHS}
	 * dari Java 8 (tersedia sejak Java 8, di-target Java 8 via --release 8).
	 * Jika logika umur ekonomis berubah (dari bulan ke tahun), perbarui di sini.
	 *
	 * @param comp komponen root ZK hasil compose
	 * @throws Exception jika inisialisasi gagal karena error Hibernate atau ZK
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		String[] contents = new String[] { "id", "nama", "assetDetail.hargaBeli", "assetDetail.tanggalBeli",
				"perTanggal", "tahunKe", "nilaiPenyusutan", "nilaiBuku", "keterangan", "assetDetail" };
		// PENTING: "contents" di atas adalah properti PenyusutanAsset (perTanggal/tahunKe/
		// nilaiPenyusutan/nilaiBuku/assetDetail.*), TAPI grid tab ini (initCriteria/onSearchDefault)
		// meng-query AssetDetail -- entitas yg TIDAK PUNYA properti-properti itu. Kalau dulu
		// Common.cetakData(this, contents) dipakai langsung, tiap kolom penyusutan gagal diresolve
		// (ditangkap diam-diam jadi null oleh CommonDownloadUpload) -> Excel ter-download tapi
		// kolom nilainya kosong semua, terlihat seperti "tidak ada data". Di sini dibuatkan
		// DataCriteria KHUSUS utk tombol Download yg benar2 meng-query PenyusutanAsset (mirror
		// filter initCriteria() di atas, tapi berbasis PenyusutanAsset+alias assetDetail), supaya
		// kolom "contents" cocok dgn entitas barisnya.
		DataCriteria dataCriteriaPenyusutan = new DataCriteria() {
			@Override
			public Object initCriteria(boolean order) {
				SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
				Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
				if (parent != null) {
					satuanKerjas.clear();
					satuanKerjas.add(parent);
					satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
				}

				Session session = HibernateUtil.currentSession();
				Criteria criteria = session.createCriteria(PenyusutanAsset.class)
						.createAlias("assetDetail", "assetDetail").createAlias("assetDetail.asset", "asset")
						.createAlias("asset.masterAsset", "masterAsset")

						.add(Restrictions.or(Restrictions.isNull("assetDetail.satuanKerja"),
								satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(
												parent == null ? Restrictions.isNull("assetDetail.satuanKerja")
														: Restrictions.sqlRestriction("false"),
												Restrictions.in("assetDetail.satuanKerja", satuanKerjas))));

				if (order)
					criteria.addOrder(Order.desc("perTanggal")).addOrder(Order.desc("id"));

				criteria

						.add((start == null || end == null || start.getValue() == null || end.getValue() == null)
								? Restrictions.sqlRestriction("1=1")
								: (Restrictions.sqlRestriction("date(assetDetail.tanggalbeli) between date('"
										+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
										+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

						.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("assetDetail.nama", searchnama.getValue().trim(),
										MatchMode.ANYWHERE))

						.add(searchmerk.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.ilike("masterAsset.merk", searchmerk.getValue(), MatchMode.ANYWHERE))

						.add(searchjenisAsset.getSelectedItem() == null
								|| searchjenisAsset.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("masterAsset.jenisAsset",
												searchjenisAsset.getSelectedItem().getValue()))

						.add((searchruang == null) ? Restrictions.sqlRestriction("1=1")
								: (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("asset.ruang", searchruang.getAttribute("ruang"))));
				return criteria;
			}
		};
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(dataCriteriaPenyusutan, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig singkron = new MyToolbarbuttonConfig("Singkronkan Penyusutan", "/img/excel.png");
		Common.appendKeToolbar(singkron, find, comp);
		singkron.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tanggal Penyusutan", "none", true);
				window.setParent(page.getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");
				final MyDatebox tahunMulai = new MyDatebox(ais.ui.util.WaktuUtil.getDate());

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("30%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Penyusutan *"));
				row.appendChild(tahunMulai);
				tahunMulai.setWidth("90%");
				tahunMulai.setReadonly(true);

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Singkronkan Penyusutan Aset", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (tahunMulai.getValue() == null) {
							MyMessageboxConfig.show("Mohon maaf, Tanggal Penyusutan belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal Penyusutan dan pilih tanggal dari kalender; (2) Pastikan tanggal berada dalam periode akuntansi yang aktif; (3) ulangi proses sinkronisasi penyusutan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Penyusutan Aset");

						final Label label = Common.displayLoadBar(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								laporan.selesaikan(new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										onSearchDefault(null);
										window.detach();
									}
								});
							}
						});

						new Thread(new Runnable() {

							@SuppressWarnings("unchecked")
							@Override
							public void run() {
								try {

								Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
								calendar.setTime(tahunMulai.getValue());

								Session session = HibernateUtil.currentNativeSession();
								List<AssetDetail> ids = session.createCriteria(AssetDetail.class)
										.add(Restrictions.gt("hargaBeli", 0.1))
										.add(Restrictions.gt("nilaiMinimal", 0.1))
										.add(Restrictions.isNotNull("tanggalBeli"))
										.add(Restrictions.gt("umurEkonomis", 0.1)).list();
								HibernateUtil.closeSession();
								int i = 1;
								for (AssetDetail assetDetail : ids) {
									label.setValue("Singkronkan penyusutan aset " + assetDetail + " ("
											+ Common.numberFormat.get().format((i * 100.0 / ids.size())) + "%)");
									String kunciAsset = String.valueOf(assetDetail);
									try {

									long monthsBetween = ChronoUnit.MONTHS.between(
											YearMonth.from(LocalDate.parse(
													Common.databaseDateFormat.get().format(assetDetail.getTanggalBeli()))),
											YearMonth.from(LocalDate
													.parse(Common.databaseDateFormat.get().format(tahunMulai.getValue()))));

									int selisih = (int) monthsBetween;
									if (selisih >= 0) {
										session = HibernateUtil.currentNativeSession();
										for (int j = 0; j <= selisih; j++) {

											PenyusutanAsset penyusutanAsset = (PenyusutanAsset) session
													.createCriteria(PenyusutanAsset.class)
													.add(Restrictions.eq("assetDetail", assetDetail))
													.add(Restrictions.eq("tahunKe", j)).setMaxResults(1).uniqueResult();
											if (penyusutanAsset == null) {
												penyusutanAsset = new PenyusutanAsset();
											}
											penyusutanAsset.setAssetDetail(assetDetail);
											penyusutanAsset.setTahunKe(j);

											session.getTransaction().begin();
											session.saveOrUpdate(penyusutanAsset);
											session.getTransaction().commit();
										}
										HibernateUtil.closeSession();
									}
									laporan.catatBerhasil(i - 1, kunciAsset, "Sinkronisasi penyusutan berhasil ("
											+ (selisih + 1) + " periode)");
									} catch (Exception ePerItem) {
										Common.tampilErrorJikaAdmin(ePerItem);
										laporan.catatGagalDetail(i - 1, kunciAsset, ePerItem);
									}

									i++;
								}
															} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-aset): "
											+ ais.common.LaporanUpload.detailTeknisException(e));
								} finally {
									label.setValue("");
									ais.database.hibernate.HibernateUtil.closeSession();
								}
							}
						}).start();

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		FilterLanjutHelper.setup(comp);
	}

	/**
	 * <b>Tujuan:</b> Inner class renderer baris grid yang menampilkan satu entitas
	 * {@code AssetDetail} beserta sub-grid penyusutan yang dapat di-expand,
	 * informasi kondisi penyusutan saat ini, dan keterangan dengan link dokumen sumber.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Untuk setiap baris, renderer membuat komponen {@code MyDetail} yang berfungsi
	 * sebagai expand/collapse trigger untuk menampilkan sub-grid berisi semua record
	 * {@code PenyusutanAsset} unit tersebut (diurutkan per tanggal descending).
	 * Sub-grid menampilkan: nomor bulan, tanggal awal (beli), tanggal penyusutan,
	 * nilai perolehan, akumulasi penyusutan (nilai penyusutan × bulan ke-), nilai buku
	 * (perolehan − akumulasi), dan keterangan. Di luar detail, baris menampilkan
	 * nama/barcode unit, bulan saat ini vs umur ekonomis (dari PenyusutanAsset terakhir
	 * yang sudah jatuh tempo), tanggal beli, tanggal penyusutan terakhir, nilai
	 * perolehan, akumulasi, nilai buku, dan kolom keterangan via {@code tampilKet}.<br>
	 * <br>
	 * <b>Threading:</b> Berjalan di thread UI ZK. Query penyusutan aktif di onOpen
	 * sub-detail menggunakan sesi Hibernate managed.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Urutan kolom harus sesuai deklarasi di ZUL. Query
	 * PenyusutanAsset terakhir menggunakan filter {@code le("perTanggal", today)}
	 * agar hanya menampilkan penyusutan yang sudah jatuh tempo, bukan yang akan datang.
	 */
	class PenyusutanAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data detail unit aset ke dalam komponen
		 * ZK Row, termasuk sub-grid penyusutan expandable dan semua informasi kondisi
		 * penyusutan saat ini.<br>
		 * <br>
		 * <b>Cara kerja:</b><br>
		 * Meng-cast {@code arg1} ke {@code AssetDetail}. Membuat MyDetail sebagai
		 * komponen expand/collapse. Event onOpen mengambil semua PenyusutanAsset
		 * untuk unit ini (order per tanggal desc) dan merender sub-grid dengan
		 * kolom: Bulan, Tanggal Awal, Penyusutan Tanggal, Nilai Perolehan,
		 * Akumulasi Penyusutan, Nilai Buku, Keterangan. Setelah detail, baris utama
		 * menampilkan nama+barcode via RevisiHelper, bulan ke-/umur ekonomis,
		 * tanggal beli, tanggal penyusutan terakhir jatuh tempo, nilai perolehan,
		 * akumulasi, nilai buku, dan kolom keterangan dengan link dokumen sumber.<br>
		 * <br>
		 * <b>Parameter:</b><br>
		 * {@code arg0} — Row ZK yang akan diisi komponen.<br>
		 * {@code arg1} — objek {@code AssetDetail} yang dirender.<br>
		 * <br>
		 * <b>Return:</b> void<br>
		 * <br>
		 * <b>Penanganan error:</b> Null checks pada penyusutanAsset dan field terkait
		 * mencegah NPE untuk unit yang belum memiliki record penyusutan. Exception
		 * pada try-catch bulan dipropagasikan ke Label fallback (hanya tampilkan bulan ke-).<br>
		 * <br>
		 * <b>Pemeliharaan:</b> Query PenyusutanAsset terakhir menggunakan
		 * {@code le("perTanggal", WaktuUtil.getDate())} dan {@code desc("perTanggal")}
		 * sehingga hanya penyusutan yang sudah terjadi yang ditampilkan sebagai "saat ini".
		 *
		 * @param arg0 Row ZK yang akan diisi komponen
		 * @param arg1 objek data AssetDetail yang akan dirender
		 * @throws Exception jika terjadi error saat query atau konstruksi komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final AssetDetail assetDetail = (AssetDetail) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			detail.addEventListener("onOpen", new EventListener() {
				@SuppressWarnings("unchecked")
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						Common.clear(detail);
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 300px;");
						groupbox.setWidth("95%");
						groupbox.setParent(detail);

						Session session = HibernateUtil.currentSession();
						Criteria criteria = session.createCriteria(PenyusutanAsset.class)
								.add(Restrictions.eq("assetDetail", assetDetail)).addOrder(Order.desc("perTanggal"))
								.addOrder(Order.desc("id"));

						List<PenyusutanAsset> penyusutanAssets = criteria.list();

						MyGrid grid = new MyGrid();
						grid.setWidth("100%");
						grid.setMold("paging");
						grid.setPageSize(50);
						grid.getPagingChild().setMold("os");
						grid.setParent(groupbox);

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig();
						column.setParent(columns);
						column.setLabel("Bulan");
						column.setWidth("5%");

						column = new MyColumnConfig();
						column.setParent(columns);
						column.setLabel("Tanggal Awal");
						column.setWidth("12%");

						column = new MyColumnConfig();
						column.setParent(columns);
						column.setLabel("Penyusutan Tanggal");
						column.setWidth("12%");

						column = new MyColumnConfig();
						column.setParent(columns);
						column.setLabel("Nilai Perolehan");
						column.setAlign("right");
						column.setWidth("15%");

						column = new MyColumnConfig();
						column.setParent(columns);
						column.setLabel("Akumulasi Penyusutan");
						column.setAlign("right");
						column.setWidth("15%");

						column = new MyColumnConfig();
						column.setParent(columns);
						column.setLabel("Nilai Buku");
						column.setAlign("right");
						column.setWidth("15%");

						column = new MyColumnConfig();
						column.setParent(columns);
						column.setLabel("Keterangan");

						Rows rows = new Rows();
						rows.setParent(grid);

						for (PenyusutanAsset penyusutanAsset : penyusutanAssets) {
							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);

							row.appendChild(new Label(penyusutanAsset.getTahunKe() + ""));

							new Label(assetDetail == null || assetDetail.getTanggalBeli() == null ? ""
									: Common.dateFormat2.get().format(assetDetail.getTanggalBeli())).setParent(row);

							new Label(penyusutanAsset == null || penyusutanAsset.getPerTanggal() == null ? ""
									: Common.dateFormat2.get().format(penyusutanAsset.getPerTanggal())).setParent(row);

							Double beli = penyusutanAsset == null || penyusutanAsset.getAssetDetail() == null ? 0.0
									: penyusutanAsset.getAssetDetail().getHargaBeli();
							Double menyusut = penyusutanAsset == null ? 0.0
									: penyusutanAsset.getNilaiPenyusutan() * penyusutanAsset.getTahunKe().doubleValue();
							Double sisa = beli - menyusut;

							new Label(penyusutanAsset == null || penyusutanAsset.getAssetDetail() == null
									|| penyusutanAsset.getAssetDetail().getHargaBeli() == null ? ""
											: Common.numberFormat.get().format(beli))
									.setParent(row);

							new Label(Common.numberFormat.get().format(menyusut)).setParent(row);
							new Label(Common.numberFormat.get().format(sisa)).setParent(row);
							new Label(assetDetail.getKeterangan()).setParent(row);
						}
					}
				}
			});

			Vbox a;
			(a = RevisiHelper.createNewRevisi(AssetDetail.class, assetDetail, assetDetail.getNama())).setParent(arg0);
			a.appendChild(new Label(assetDetail.getBarcode()));

			Session session = HibernateUtil.currentSession();
			PenyusutanAsset penyusutanAsset = (PenyusutanAsset) session.createCriteria(PenyusutanAsset.class)
					.add(Restrictions.eq("assetDetail", assetDetail))
					.add(Restrictions.le("perTanggal", WaktuUtil.getDate())).setMaxResults(1)
					.addOrder(Order.desc("perTanggal")).uniqueResult();

			try {
				new Label((penyusutanAsset == null ? ""
						: (penyusutanAsset.getTahunKe().toString() + " / ")
								+ Common.numberFormat.get().format(assetDetail.getUmurEkonomis())))
						.setParent(arg0);
			} catch (Exception e) {
				new Label(penyusutanAsset == null ? "" : penyusutanAsset.getTahunKe().toString()).setParent(arg0);
			}
			new Label(assetDetail == null || assetDetail.getTanggalBeli() == null ? ""
					: Common.dateFormat2.get().format(assetDetail.getTanggalBeli())).setParent(arg0);
			new Label(penyusutanAsset == null || penyusutanAsset.getPerTanggal() == null ? ""
					: Common.dateFormat2.get().format(penyusutanAsset.getPerTanggal())).setParent(arg0);

			Double beli = penyusutanAsset == null || penyusutanAsset.getAssetDetail() == null ? 0.0
					: penyusutanAsset.getAssetDetail().getHargaBeli();
			Double menyusut = penyusutanAsset == null ? 0.0
					: penyusutanAsset.getNilaiPenyusutan() * penyusutanAsset.getTahunKe().doubleValue();
			Double sisa = beli - menyusut;

			new Label(penyusutanAsset == null || penyusutanAsset.getAssetDetail() == null
					|| penyusutanAsset.getAssetDetail().getHargaBeli() == null ? "" : Common.numberFormat.get().format(beli))
					.setParent(arg0);

			new Label(Common.numberFormat.get().format(menyusut)).setParent(arg0);
			new Label(Common.numberFormat.get().format(sisa)).setParent(arg0);

			AssetDetailAction.tampilKet(assetDetail).setParent(arg0);

		}

	}

	/**
	 * <b>Tujuan:</b> Membangun dan mengembalikan komponen Vbox berisi keterangan
	 * unit aset beserta link ke dokumen sumber pengadaan atau saldo awal terkait.
	 * Metode ini bersifat statis agar dapat dipanggil dari kelas lain
	 * (seperti {@code AssetAction.AssetRenderer}) tanpa perlu instance composer.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Membuat Vbox dan menambahkan Label berisi keterangan aset. Kemudian secara
	 * berurutan memeriksa keberadaan dokumen sumber melalui rantai relasi entitas:
	 * 1. Permintaan pengadaan (PermintaanPengadaanMasterAsset): ditampilkan sebagai link
	 *    yang membuka dialog detail permintaan pengadaan.<br>
	 * 2. Penerimaan pengadaan melalui saldo awal: ditampilkan sebagai link yang
	 *    menghasilkan laporan PDF penerimaan pengadaan.<br>
	 * 3. Saldo awal langsung: ditampilkan sebagai link yang mencetak laporan saldo awal.<br>
	 * Selanjutnya, memeriksa keberadaan informasi SOP disposisi dari tiga sumber:
	 * 1. SOP dari permintaan pengadaan.<br>
	 * 2. SOP dari penerimaan pengadaan melalui saldo awal.<br>
	 * 3. SOP dari pemesanan pengadaan yang terkait dengan penerimaan melalui saldo awal.<br>
	 * Setiap link SOP membuka popup alur SOP via {@code TampilanAlurSopAction.prosess}.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code assetDetail} — entitas AssetDetail yang keterangannya akan ditampilkan,
	 * beserta relasi-relasi yang akan diperiksa untuk dokumen sumber.<br>
	 * <br>
	 * <b>Return:</b> Komponen {@code Vbox} ZK berisi Label keterangan dan link-link
	 * dokumen sumber, siap untuk ditambahkan ke parent komponen.<br>
	 * <br>
	 * <b>Penanganan error:</b> Setiap kondisi if-else menggunakan null-check berantai
	 * yang panjang untuk mencegah NPE pada rantai relasi yang bisa null. Jika tidak ada
	 * dokumen sumber yang ditemukan, Vbox hanya berisi Label keterangan saja.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Urutan prioritas dokumen sumber (permintaan → penerimaan via
	 * saldo → saldo langsung) mencerminkan alur pengadaan normal. Jika alur pengadaan
	 * berubah, perbarui urutan pemeriksaan di sini. Metode ini dipanggil dari renderer
	 * di kelas ini dan dari AssetAction sehingga perubahan berdampak pada dua halaman.
	 *
	 * @param assetDetail entitas AssetDetail yang akan ditampilkan keterangannya
	 * @return Vbox ZK berisi keterangan dan link dokumen sumber terkait
	 */
	public static Vbox tampilKet(final AssetDetail assetDetail) {
		Vbox a = new Vbox();

		new Label(assetDetail.getKeterangan()).setParent(a);

		if (assetDetail.getAsset() != null && assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail() != null
				&& assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail()
						.getPermintaanPengadaanMasterAsset() != null) {
			A aa;
			(aa = new A(assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail()
					.getPermintaanPengadaanMasterAsset().getKode())).setParent(a);
			aa.setStyle("font-size:9px;");
			aa.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					PermintaanPengadaanMasterAssetAction.onAddExternal(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAsset());
				}
			});
		}

		else if (assetDetail.getAsset() != null && assetDetail.getAsset().getSaldoAwalMasterAssetDetail() != null
				&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail()
						.getPenerimaanPengadaanMasterAssetDetail() != null
				&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset() != null) {
			A aa;
			(aa = new A(assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
					.getPenerimaanPengadaanMasterAsset().getKode())).setParent(a);
			aa.setStyle("font-size:9px;");
			aa.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@SuppressWarnings({})
						@Override
						public void onEvent(Event arg0) throws Exception {

							Report.generatePDFReport(Report.PDF,
									PenerimaanPengadaanMasterAssetAction.parameter(assetDetail.getAsset()
											.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
											.getPenerimaanPengadaanMasterAsset()),
									"asset/penerimaan_pengadaan",
									assetDetail.getAsset().getSaldoAwalMasterAssetDetail()
											.getPenerimaanPengadaanMasterAssetDetail()
											.getPenerimaanPengadaanMasterAsset().getTanggalPembuatan());
						}
					});
				}
			});
		}

		else if (assetDetail.getAsset() != null && assetDetail.getAsset().getSaldoAwalMasterAssetDetail() != null
				&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail()
						.getPenerimaanPengadaanMasterAssetDetail() != null
				&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset() != null
				&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getSaldoAwal() != null) {
			A aa;
			(aa = new A(assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getSaldoAwal().getKode())).setParent(a);
			aa.setStyle("font-size:9px;");
			aa.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					SaldoAwalMasterAssetAction
							.cetak(assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getSaldoAwal());
				}
			});
		}

		if (assetDetail.getAsset() != null && assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail() != null
				&& assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail()
						.getPermintaanPengadaanMasterAsset() != null
				&& assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset()
						.getDisposisiSop() != null) {
			A aa;
			(aa = new A("SOP "
					+ assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAsset().getDisposisiSop().getKeterangan()
					+ " ("
					+ assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAsset().getDisposisiSop().getSop().getNama()
					+ ")")).setParent(a);
			aa.setStyle("font-size:9px;");
			aa.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					TampilanAlurSopAction.prosess(
							assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail()
									.getPermintaanPengadaanMasterAsset().getDisposisiSop().getId(),
							null, null, true, arg0.getTarget());
				}
			});
		}

		else if (assetDetail.getAsset() != null && assetDetail.getAsset().getSaldoAwalMasterAssetDetail() != null
				&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail()
						.getPenerimaanPengadaanMasterAssetDetail() != null
				&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset() != null
				&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset().getDisposisiSop() != null) {
			A aa;
			(aa = new A("SOP "
					+ assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
							.getPenerimaanPengadaanMasterAsset().getDisposisiSop().getKeterangan()
					+ " ("
					+ assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
							.getPenerimaanPengadaanMasterAsset().getDisposisiSop().getSop().getNama()
					+ ")")).setParent(a);
			aa.setStyle("font-size:9px;");
			aa.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					TampilanAlurSopAction.prosess(assetDetail.getAsset().getSaldoAwalMasterAssetDetail()
							.getPenerimaanPengadaanMasterAssetDetail().getPenerimaanPengadaanMasterAsset()
							.getDisposisiSop().getId(), null, null, true, arg0.getTarget());
				}
			});
		}

		else if (assetDetail.getAsset() != null && assetDetail.getAsset().getSaldoAwalMasterAssetDetail() != null
				&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail()
						.getPenerimaanPengadaanMasterAssetDetail() != null
				&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset() != null
				&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset() != null
				&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()
						.getDisposisiSop() != null) {
			A aa;
			(aa = new A("SOP "
					+ assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
							.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset().getDisposisiSop()
							.getKeterangan()
					+ " ("
					+ assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
							.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset().getDisposisiSop()
							.getSop().getNama()
					+ ")")).setParent(a);
			aa.setStyle("font-size:9px;");
			aa.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					TampilanAlurSopAction.prosess(
							assetDetail.getAsset().getSaldoAwalMasterAssetDetail()
									.getPenerimaanPengadaanMasterAssetDetail().getPenerimaanPengadaanMasterAsset()
									.getPemesananPengadaanMasterAsset().getDisposisiSop().getId(),
							null, null, true, arg0.getTarget());
				}
			});
		}

		return a;
	}

	/**
	 * <b>Tujuan:</b> Membangun objek {@code Criteria} Hibernate yang merangkum semua
	 * kondisi filter aktif untuk mengambil data {@code AssetDetail} dari database.
	 * Digunakan untuk paging (tanpa order) maupun pengambilan halaman data (dengan order).<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Menentukan set satuan kerja relevan berdasarkan filter hierarki satuan kerja
	 * (jika ada yang dipilih, tambahkan semua turunannya via SatuanKerjaTreeModel).
	 * Membuat criteria pada {@code AssetDetail} dengan join ke alias asset dan masterAsset.
	 * Filter satuan kerja menggunakan OR antara null (aset tanpa satuan kerja) dan
	 * member set; logika khusus saat set kosong (tampilkan semua). Filter tanggal
	 * menggunakan SQL date comparison pada tanggalbeli. Filter nama menggunakan ILIKE.
	 * Filter merk menggunakan ILIKE pada masterAsset.merk. Filter jenis aset dan ruangan
	 * juga diterapkan jika aktif. Jika {@code order} true, diurutkan berdasarkan
	 * tanggalBeli descending lalu ID descending.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code order} — apakah hasil harus diurutkan.<br>
	 * <br>
	 * <b>Return:</b> Objek {@code Criteria} Hibernate siap dieksekusi.<br>
	 * <br>
	 * <b>Penanganan error:</b> Guard null pada start/end dan searchruang mencegah NPE.
	 * Guard null pada getSelectedItem mencegah exception saat combo belum dipilih.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Filter nama menggunakan sqlRestriction("true") saat kosong
	 * (bukan "1=1") — periksa konsistensi dengan filter lain jika ada perubahan.
	 * Kolom database "tanggalbeli" (huruf kecil semua) digunakan dalam sqlRestriction.
	 *
	 * @param order apakah hasil harus diurutkan berdasarkan tanggal beli dan ID
	 * @return objek Criteria Hibernate untuk query AssetDetail
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
		Criteria criteria = session.createCriteria(AssetDetail.class).createAlias("asset", "asset")
				.createAlias("asset.masterAsset", "masterAsset")

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

		;
		if (order)
			criteria.addOrder(Order.desc("tanggalBeli")).addOrder(Order.desc("id"));

		criteria

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.tanggalbeli) between date('" + Common.databaseDateFormat.get().format(start.getValue())
								+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchmerk.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("masterAsset.merk", searchmerk.getValue(), MatchMode.ANYWHERE))

				.add(searchjenisAsset.getSelectedItem() == null || searchjenisAsset.getSelectedItem().getValue() == null
						|| searchjenisAsset.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("masterAsset.jenisAsset",
										searchjenisAsset.getSelectedItem().getValue()))

				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("asset.ruang", searchruang.getAttribute("ruang"))));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Memuat dan menampilkan daftar detail unit aset ke grid sesuai
	 * filter aktif. Dipanggil dari event handler ZK (paging, perubahan filter satuan
	 * kerja, timer inisialisasi, atau setelah sinkronisasi selesai).<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.initPaging} untuk menghitung total dan mengatur state
	 * paging. Mengambil satu halaman data {@code AssetDetail} via {@code initCriteria(true)}
	 * dengan limit ROWS_COUNT_ON_PAGE dan offset sesuai halaman aktif. Membungkus
	 * hasil dalam SimpleListModel dan menerapkan renderer {@code PenyusutanAssetRenderer}
	 * ke grid.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK pemicu, boleh null.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate dipropagasikan ke ZK exception handler.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Nama metode onSearchDefault digunakan oleh ZK event binding
	 * konvensional dan oleh antarmuka {@code DataSearchDefault}. Jangan diubah.
	 *
	 * @param event event ZK pemicu reload, boleh null
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<AssetDetail> penyusutanAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penyusutanAsset);
		grid.setRowRenderer(new PenyusutanAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

}
