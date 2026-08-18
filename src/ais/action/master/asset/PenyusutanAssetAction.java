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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import ais.action.report.helper.asset.LaporanPenyusutan;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.AssetDetail;
import ais.database.model.asset.PenyusutanAsset;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h3>PenyusutanAssetAction — Kontroler CRUD Penyusutan Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZK (ZKoss Composer) untuk halaman manajemen penyusutan aset
 * di modul manajemen aset eCampus. Penyusutan aset (depreciation) adalah proses pencatatan
 * penurunan nilai ekonomis suatu aset dari waktu ke waktu sesuai umur ekonomisnya. Halaman
 * ini memungkinkan administrator keuangan untuk melihat daftar penyusutan per aset, melakukan
 * sinkronisasi penyusutan secara massal terhadap seluruh aset yang memenuhi syarat (memiliki
 * harga beli, nilai minimal, tanggal beli, dan umur ekonomis), serta melihat laporan penyusutan
 * dalam format yang dapat dicetak atau diekspor.<br><br>
 *
 * <b>Cara kerja:</b><br>
 * Implementasi menggunakan pola GenericAutowireComposer dari ZKoss 5.5, di mana komponen-komponen
 * ZUL (grid, paging, textbox filter, datebox rentang tanggal) di-autowire otomatis berdasarkan
 * nama field yang cocok dengan atribut id komponen di file ZUL. Saat halaman dimuat,
 * {@code doAfterCompose} dipanggil untuk menginisialisasi hak akses, rentang tanggal default
 * filter (6 bulan ke belakang), serta mendaftarkan timer untuk auto-refresh. Fitur utama
 * "Singkronkan Penyusutan" membuka dialog modal pemilihan tanggal, lalu menjalankan proses
 * kalkulasi penyusutan di thread latar untuk setiap {@code AssetDetail} yang memenuhi syarat,
 * menghitung selisih bulan antara tanggal beli aset dan tanggal target sinkronisasi, kemudian
 * menyimpan atau memperbarui entitas {@code PenyusutanAsset} untuk setiap bulan (tahun ke-0
 * hingga ke-N) menggunakan sesi Hibernate terpisah per iterasi. Filter pencarian mendukung
 * kombinasi nama aset, merk, jenis aset, ruang, rentang tanggal penyusutan, dan rentang
 * tanggal beli, semuanya disusun melalui Hibernate Criteria API.<br><br>
 *
 * <b>Threading:</b><br>
 * Proses sinkronisasi penyusutan dijalankan di thread anonim terpisah (bukan thread utama ZK)
 * untuk menghindari pemblokiran antarmuka pengguna. Label progress diperbarui secara bertahap
 * per aset. Setiap iterasi aset membuka dan menutup sesi Hibernate tersendiri melalui
 * {@code HibernateUtil.currentNativeSession()} dan {@code HibernateUtil.closeSession()}.
 * Tidak ada mekanisme sinkronisasi antar-thread (tidak ada {@code synchronized} atau
 * {@code volatile}) sehingga kehati-hatian diperlukan jika proses ini dijalankan bersamaan
 * oleh beberapa pengguna.<br><br>
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas ini mengimplementasikan tiga antarmuka: {@code DataCriteria} (menyediakan kriteria
 * Hibernate untuk filter), {@code DataSearchDefault} (memuat ulang grid setelah pencarian),
 * dan {@code DataInitDefault} (menginisialisasi form edit dari objek generik). Jika model
 * penyusutan berubah (misalnya metode garis lurus vs. saldo menurun), logika perhitungan ada
 * di entitas {@code PenyusutanAsset} dan di blok thread sinkronisasi di sini. Penambahan
 * filter baru cukup dilakukan di metode {@code initCriteria}. Laporan terpisah dikelola
 * oleh kelas {@code LaporanPenyusutan}.
 *
 * @author eCampus Dev Team
 * @version 1.0
 */
public class PenyusutanAssetAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Serial version UID untuk serialisasi kelas ini sebagai komponen ZK.
	 * Nilai negatif ini ditetapkan secara eksplisit agar konsisten antar versi deploy.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal tambah/ubah data penyusutan aset. */
	private MyWindow addWindow;

	/** Komponen paginasi grid daftar penyusutan aset. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar penyusutan aset. */
	private MyGrid grid;

	/** Kotak teks filter pencarian berdasarkan nama aset. */
	private Textbox searchnama;

	/** Kotak teks filter pencarian berdasarkan merk aset. */
	private Textbox searchmerk;

	/** Combobox filter berdasarkan jenis aset. */
	private Combobox searchjenisAsset;

	/** Banbox filter berdasarkan ruang tempat aset berada. */
	private AmbilDataRuangBanbox searchruang;

	/** Datebox untuk tanggal awal rentang filter penyusutan (per tanggal). */
	private MyDatebox start;

	/** Datebox untuk tanggal akhir rentang filter penyusutan (per tanggal). */
	private MyDatebox end;

	/** Datebox untuk tanggal awal rentang filter tanggal pembelian aset. */
	private MyDatebox startBeli;

	/** Datebox untuk tanggal akhir rentang filter tanggal pembelian aset. */
	private MyDatebox endBeli;

	/** Input nilai penyusutan per periode pada form edit, bersifat readonly (dihitung otomatis). */
	private MyDoublebox nilaiPenyusutan;

	/** Input nilai buku aset pada form edit, bersifat readonly (dihitung otomatis). */
	private MyDoublebox nilaiBuku;

	/** Kotak teks keterangan tambahan pada form tambah/ubah penyusutan. */
	private Textbox keterangan;

	/** Penanda apakah pengguna saat ini memiliki hak ubah data. */
	private boolean edit = false;

	/** Penanda apakah pengguna saat ini memiliki hak hapus data. */
	private boolean delete = false;

	/** Entitas penyusutan aset yang sedang dikelola (tambah atau ubah). */
	private PenyusutanAsset penyusutanAsset;

	/** Tombol toolbar temukan/cari sebagai titik acuan penambahan tombol lain di toolbar. */
	private MyToolbarbuttonConfig find;

	/** Panel tab untuk menampilkan laporan penyusutan (lazy-loaded). */
	private Tabpanel laporanPenyusutan;

	/**
	 * Menampilkan laporan penyusutan aset di dalam tab yang disediakan.
	 *
	 * <b>Tujuan:</b><br>
	 * Memuat konten laporan penyusutan aset ke dalam panel tab {@code laporanPenyusutan}
	 * secara lazy (hanya dimuat ketika tab tersebut pertama kali diklik oleh pengguna).
	 * Pola ini menghemat sumber daya karena laporan berat (query data besar, render grafik)
	 * tidak diinisialisasi saat halaman pertama kali dibuka.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Metode ini dipanggil oleh event handler ZK ketika tab Laporan Penyusutan dipilih.
	 * Jika panel tab belum memiliki komponen anak (children kosong), maka sebuah instance
	 * {@code LaporanPenyusutan} dibuat dengan lebar dan tinggi 100%, lalu ditambahkan sebagai
	 * anak dari {@code laporanPenyusutan}. Jika sudah ada isinya, tidak ada yang dilakukan,
	 * sehingga laporan tidak dimuat ulang setiap kali tab diklik.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Tidak ada penanganan error eksplisit. Kegagalan inisialisasi {@code LaporanPenyusutan}
	 * akan menjadi exception yang muncul di sisi klien ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika laporan penyusutan perlu diganti dengan komponen lain, cukup ubah baris
	 * pembuatan instance di dalam blok kondisional ini.
	 *
	 * @param event Event ZK yang memicu metode ini (biasanya onSelect dari Tabbox),
	 *              tidak digunakan secara langsung dalam implementasi ini.
	 */
	public void onLaporanPenyusutan(Event event) {

		if (laporanPenyusutan.getChildren().size() == 0) {
			LaporanPenyusutan laporan = new LaporanPenyusutan();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(laporanPenyusutan);
		}
	}

	/** Model pohon satuan kerja untuk navigasi hierarki unit kerja pada filter pencarian. */
	private SatuanKerjaTreeModel satuanKerjaTreeModel = null;

	/** Banbox filter berdasarkan satuan kerja (unit kerja/divisi) pemilik aset. */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/**
	 * Pemeriksaan keamanan sebelum halaman dikompilasi oleh ZK.
	 *
	 * <b>Tujuan:</b><br>
	 * Memastikan bahwa pengguna yang mengakses halaman penyusutan aset telah memiliki
	 * sesi yang valid dan hak akses yang sesuai (READ) sebelum proses komposisi halaman
	 * dimulai. Jika tidak, pengguna akan diarahkan ke halaman login.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} yang memeriksa sesi dan hak akses.
	 * Jika validasi gagal, metode tersebut secara internal melakukan redirect ke halaman
	 * logoff. Setelah itu, metode parent {@code super.doBeforeCompose} dipanggil agar
	 * proses ZK berjalan normal.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pola ini adalah standar untuk semua halaman di aplikasi eCampus. Jangan menghapus
	 * atau memodifikasi pemanggilan {@code Common.doCheckSecurity()} tanpa koordinasi
	 * dengan tim keamanan.
	 *
	 * @param page     Halaman ZK yang sedang dimuat.
	 * @param parent   Komponen induk dari komposer ini.
	 * @param compInfo Informasi metadata komponen dari file ZUL.
	 * @return {@code ComponentInfo} hasil dari {@code super.doBeforeCompose}, digunakan
	 *         oleh ZK untuk melanjutkan proses komposisi.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Inisialisasi komponen dan logika bisnis setelah semua elemen ZUL terhubung (autowired).
	 *
	 * <b>Tujuan:</b><br>
	 * Metode ini adalah titik masuk utama inisialisasi halaman penyusutan aset. Dipanggil
	 * otomatis oleh ZK setelah seluruh komponen di file ZUL berhasil di-autowire ke field
	 * di kelas ini. Semua pengaturan awal halaman dilakukan di sini: hak akses, rentang
	 * tanggal default, tombol toolbar tambahan, serta event listener.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Langkah-langkah yang dilakukan secara berurutan:
	 * (1) Memanggil {@code super.doAfterCompose} dan menginisialisasi bahasa via
	 * {@code Common.initLaguage()}.
	 * (2) Memeriksa hak akses UPDATE dan DELETE dari {@code CommonPrivilages}.
	 * (3) Mengatur listener paginasi agar setiap pergantian halaman memicu
	 * {@code onSearchDefault}.
	 * (4) Mengatur event listener pada banbox satuan kerja agar perubahan filter unit
	 * kerja memicu pencarian ulang.
	 * (5) Menginisialisasi model pohon satuan kerja untuk filter hierarki unit kerja.
	 * (6) Mengatur nilai default rentang tanggal filter penyusutan (mulai: 6 bulan lalu,
	 * akhir: besok) dan rentang tanggal beli (mulai: 6 tahun lalu, akhir: besok).
	 * (7) Menambahkan tombol cetak data dan tombol "Singkronkan Penyusutan" ke toolbar.
	 * (8) Tombol sinkronisasi membuka dialog modal pemilihan tanggal, lalu menjalankan
	 * proses sinkronisasi di thread latar yang melakukan looping semua AssetDetail valid
	 * dan menyimpan PenyusutanAsset per bulan.
	 * (9) Mendaftarkan timer default untuk auto-refresh grid secara periodik.
	 * (10) Mengaktifkan filter lanjutan melalui {@code FilterLanjutHelper.setup}.<br><br>
	 *
	 * <b>Parameter:</b><br>
	 * {@code comp} — Komponen akar dari halaman ZUL yang telah dikompilasi oleh ZK.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari {@code super.doAfterCompose} dan langkah inisialisasi lain diteruskan
	 * ke ZK untuk ditangani di level framework. Kesalahan dalam thread sinkronisasi tidak
	 * ditangkap secara eksplisit sehingga dapat menyebabkan thread berhenti tanpa notifikasi.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Rentang tanggal default (6 bulan / 6 tahun) dapat disesuaikan di sini. Jika logika
	 * sinkronisasi perlu dipindahkan ke service layer, refaktor blok thread di dalam
	 * event listener tombol sinkronisasi menjadi method tersendiri.
	 *
	 * @param comp Komponen akar halaman ZUL yang telah berhasil di-autowire.
	 * @throws Exception Jika terjadi kesalahan pada inisialisasi komponen ZK atau Hibernate.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

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

		if (startBeli != null) { startBeli.setReadonly(true); }
		if (endBeli != null) { endBeli.setReadonly(true); }

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 6);
		if (startBeli != null) { startBeli.setValue(calendar.getTime()); }
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (endBeli != null) { endBeli.setValue(calendar.getTime()); }

		String[] contents = new String[] { "id", "nama", "assetDetail.hargaBeli", "assetDetail.tanggalBeli",
				"perTanggal", "tahunKe", "nilaiPenyusutan", "nilaiBuku", "keterangan", "assetDetail" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
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
							MyMessageboxConfig.show("Mohon maaf, Tanggal Penyusutan belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal Penyusutan dan pilih tanggal dari kalender; (2) Pastikan tanggal yang dipilih berada dalam periode akuntansi yang aktif; (3) ulangi proses sinkronisasi penyusutan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
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
	 * Renderer baris grid untuk daftar penyusutan aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Kelas inner ini bertanggung jawab merender setiap baris data penyusutan aset
	 * ke dalam baris grid ZK ({@code Row}). Setiap baris menampilkan informasi lengkap
	 * satu entri penyusutan: nama aset, posisi tahun penyusutan terhadap umur ekonomis,
	 * tanggal beli, tanggal penyusutan, harga beli, akumulasi penyusutan, nilai buku
	 * saat ini, keterangan, serta tombol aksi ubah/hapus.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Metode {@code render} dipanggil oleh ZK untuk setiap objek {@code PenyusutanAsset}
	 * dalam model list. Nilai buku sisa dihitung secara inline: nilai buku = harga beli
	 * dikurangi (nilai penyusutan per periode dikali tahun ke). Penghitungan ini bersifat
	 * display-only dan tidak mengubah data di database. Kolom keterangan menggabungkan
	 * teks keterangan manual dengan komponen keterangan dari {@code AssetDetailAction}.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika kolom baru perlu ditambahkan, tambahkan komponen di sini dan sesuaikan
	 * jumlah kolom header di file ZUL yang bersesuaian.
	 */
	class PenyusutanAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data penyusutan aset ke dalam komponen {@code Row} ZK.
		 *
		 * <b>Tujuan:</b><br>
		 * Mengisi setiap sel baris grid dengan data dari entitas {@code PenyusutanAsset},
		 * termasuk kalkulasi nilai buku dan tombol aksi ubah/hapus.<br><br>
		 *
		 * <b>Cara kerja:</b><br>
		 * Membuat komponen Label untuk setiap kolom data (nama, tahun ke / umur ekonomis,
		 * tanggal beli, tanggal penyusutan, harga beli, akumulasi penyusutan, nilai buku
		 * sisa). Nilai buku dihitung sebagai: {@code hargaBeli - (nilaiPenyusutan * tahunKe)}.
		 * Keterangan ditampilkan dalam Vbox yang menggabungkan keterangan manual dan
		 * keterangan aset detail. Baris ditutup dengan tombol aksi standar
		 * (salin/ubah/hapus) dari {@code Common.copyEditDeleteButtons}.<br><br>
		 *
		 * <b>Penanganan error:</b><br>
		 * Jika format umur ekonomis gagal, blok try-catch menampilkan hanya tahun ke
		 * tanpa rasio terhadap umur ekonomis.
		 *
		 * @param arg0 Baris grid ZK yang akan diisi komponen.
		 * @param arg1 Objek data {@code PenyusutanAsset} yang dirender pada baris ini.
		 * @throws Exception Jika terjadi kesalahan saat membuat atau menambahkan komponen ZK.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PenyusutanAsset penyusutanAsset = (PenyusutanAsset) arg1;

			RevisiHelper.createNewRevisi(PenyusutanAsset.class, penyusutanAsset, penyusutanAsset.getNama())
					.setParent(arg0);
			try {
				new Label(penyusutanAsset.getTahunKe().toString() + " / "
						+ Common.numberFormat.get().format(penyusutanAsset.getAssetDetail().getUmurEkonomis()))
						.setParent(arg0);
			} catch (Exception e) {
				new Label(penyusutanAsset.getTahunKe().toString()).setParent(arg0);
			}
			new Label(penyusutanAsset.getAssetDetail() == null
					|| penyusutanAsset.getAssetDetail().getTanggalBeli() == null ? ""
							: Common.dateFormat2.get().format(penyusutanAsset.getAssetDetail().getTanggalBeli()))
					.setParent(arg0);
			new Label(penyusutanAsset.getPerTanggal() == null ? ""
					: Common.dateFormat2.get().format(penyusutanAsset.getPerTanggal())).setParent(arg0);

			Double beli = penyusutanAsset.getAssetDetail() == null ? 0.0
					: penyusutanAsset.getAssetDetail().getHargaBeli();
			Double menyusut = penyusutanAsset.getNilaiPenyusutan() * penyusutanAsset.getTahunKe().doubleValue();
			Double sisa = beli - menyusut;

			new Label(
					penyusutanAsset.getAssetDetail() == null || penyusutanAsset.getAssetDetail().getHargaBeli() == null
							? ""
							: Common.numberFormat.get().format(beli))
					.setParent(arg0);

			new Label(Common.numberFormat.get().format(menyusut)).setParent(arg0);
			new Label(Common.numberFormat.get().format(sisa)).setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(penyusutanAsset.getKeterangan()).setParent(a);
			AssetDetailAction.tampilKet(penyusutanAsset.getAssetDetail()).setParent(a);

			Common.copyEditDeleteButtons(edit, delete, penyusutanAsset, PenyusutanAssetAction.this).setParent(arg0);

		}

	}

	/**
	 * Menangani klik tombol "Tambah" untuk membuat entri penyusutan aset baru.
	 *
	 * <b>Tujuan:</b><br>
	 * Membuka form tambah penyusutan baru dengan membuat instance {@code PenyusutanAsset}
	 * kosong, lalu menampilkan jendela modal form tambah/ubah.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code init(new PenyusutanAsset())} untuk menginisialisasi form dengan
	 * objek kosong, kemudian menampilkan {@code addWindow} dalam mode modal. Judul jendela
	 * akan otomatis menampilkan "Tambah Nilai Penyusutan" karena ID entitas masih null.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Validasi bisnis (misalnya harus memilih aset terlebih dahulu) sebaiknya ditambahkan
	 * di sini sebelum membuka form.
	 *
	 * @param event Event ZK dari klik tombol tambah, tidak digunakan secara langsung.
	 * @throws Exception Jika terjadi kesalahan saat inisialisasi form atau membuka jendela modal.
	 */
	public void onAdd(Event event) throws Exception {
		init(new PenyusutanAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Implementasi antarmuka {@code DataInitDefault} untuk inisialisasi form dari objek generik.
	 *
	 * <b>Tujuan:</b><br>
	 * Dipanggil oleh mekanisme generik (misalnya tombol ubah di renderer grid) dengan
	 * objek {@code GeneralValueObject} yang merupakan entitas {@code PenyusutanAsset}
	 * yang sudah ada. Metode ini melakukan cast ke tipe yang benar dan mendelegasikan
	 * ke {@code init(PenyusutanAsset)}.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Meng-cast {@code obj} ke {@code PenyusutanAsset}, menyimpannya ke field instance,
	 * memanggil {@code init(penyusutanAsset)} untuk mengisi form, lalu menampilkan
	 * jendela modal dalam mode edit.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Metode ini merupakan kontrak dari antarmuka {@code DataInitDefault} dan dipanggil
	 * oleh {@code Common.copyEditDeleteButtons} secara generik. Jangan mengubah tanda tangan
	 * metode ini.
	 *
	 * @param obj Objek {@code GeneralValueObject} yang berisi entitas {@code PenyusutanAsset}
	 *            yang akan diedit. Harus merupakan instance dari {@code PenyusutanAsset}.
	 * @throws Exception Jika cast gagal atau inisialisasi form mengalami kesalahan.
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		penyusutanAsset = (PenyusutanAsset) obj;
		init(penyusutanAsset);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Menginisialisasi dan membangun form tambah/ubah penyusutan aset secara programatik.
	 *
	 * <b>Tujuan:</b><br>
	 * Membersihkan konten {@code addWindow} yang mungkin ada sebelumnya dan membangun
	 * ulang seluruh form input penyusutan aset (nama, tahun penyusutan, nilai penyusutan,
	 * nilai buku, keterangan) secara programatik menggunakan ZK API. Pendekatan ini
	 * digunakan karena form dibangun secara dinamis, bukan dari file ZUL statis.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menyimpan referensi entitas ke field instance dan mengatur judul jendela.
	 * (2) Membersihkan konten jendela dengan {@code Common.clear(addWindow)}.
	 * (3) Membangun layout Borderlayout dengan Center untuk grid form dan South untuk toolbar.
	 * (4) Membuat grid dua kolom (label 30%, input 70%) dengan baris-baris form.
	 * (5) Baris nama dan tahun ke ditampilkan sebagai Label readonly (tidak dapat diubah pengguna).
	 * (6) Nilai penyusutan dan nilai buku dirender sebagai MyDoublebox yang juga bersifat readonly.
	 * (7) Hanya keterangan yang dapat diubah secara bebas.
	 * (8) Toolbar berisi tombol Batal (menutup jendela) dan Simpan (memanggil {@code onSave}).<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika field baru perlu ditambahkan ke form, tambahkan MyFormRow baru di antara baris
	 * yang sudah ada. Pastikan field tersebut juga diproses di {@code onSave}.
	 *
	 * @param penyusutanAsset Entitas penyusutan aset yang akan ditampilkan/diedit.
	 *                        Jika ID null, form dalam mode tambah; jika ada ID, mode ubah.
	 */
	private void init(PenyusutanAsset penyusutanAsset) {
		this.penyusutanAsset = penyusutanAsset;
		addWindow.setTitle(penyusutanAsset.getId() == null ? "Tambah Nilai Penyusutan" : "Ubah Nilai Penyusutan");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyusutan Aset"));
		row.appendChild(new Label(penyusutanAsset.getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Penyusutan Ke"));
		row.appendChild(new Label(penyusutanAsset.getTahunKe().toString()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Penyusutan *"));
		row.appendChild(nilaiPenyusutan = new MyDoublebox(penyusutanAsset.getNilaiPenyusutan()));
		nilaiPenyusutan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Buku *"));
		row.appendChild(nilaiBuku = new MyDoublebox(penyusutanAsset.getNilaiBuku()));
		nilaiBuku.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(penyusutanAsset.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	/**
	 * Menyimpan perubahan data penyusutan aset ke database.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengambil nilai dari field input form (nilai buku, nilai penyusutan, keterangan)
	 * dan menyimpannya ke entitas {@code PenyusutanAsset} yang sedang diedit, kemudian
	 * melakukan saveOrUpdate via Hibernate.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Mengambil sesi Hibernate aktif via {@code HibernateUtil.currentSession()}.
	 * (2) Jika entitas sudah memiliki ID (mode ubah), me-load ulang entitas dari database
	 *     untuk menghindari stale state dan konflik versi Hibernate.
	 * (3) Mengisi field entitas dari nilai komponen form.
	 * (4) Memanggil {@code Common.refreshSaveOrUpdate} yang menangani commit transaksi.<br><br>
	 *
	 * <b>Return:</b><br>
	 * {@code true} jika penyimpanan berhasil, sehingga pemanggil dapat menutup form
	 * dan me-refresh grid. Saat ini selalu mengembalikan {@code true} (tidak ada validasi
	 * input di metode ini karena field nilaiPenyusutan dan nilaiBuku bersifat readonly).<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari Hibernate (misalnya constraint violation) akan diteruskan ke pemanggil
	 * dan ditangani oleh framework ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika validasi perlu ditambahkan (misalnya nilai penyusutan tidak boleh nol),
	 * tambahkan pengecekan sebelum memanggil {@code Common.refreshSaveOrUpdate}.
	 *
	 * @param event Event ZK yang memicu penyimpanan (biasanya klik tombol Simpan).
	 * @return {@code true} jika data berhasil disimpan ke database.
	 * @throws Exception Jika terjadi kesalahan Hibernate saat menyimpan data.
	 */
	public boolean onSave(Event event) throws Exception {

		Session session = HibernateUtil.currentSession();
		if (penyusutanAsset.getId() != null) {
			penyusutanAsset = (PenyusutanAsset) session.load(PenyusutanAsset.class, penyusutanAsset.getId());

		}

		penyusutanAsset.setNilaiBuku(nilaiBuku.getValue());
		penyusutanAsset.setNilaiPenyusutan(nilaiPenyusutan.getValue());
		penyusutanAsset.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, penyusutanAsset);

		return true;
	}

	/**
	 * Membangun objek Criteria Hibernate untuk kueri daftar penyusutan aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Menyusun kriteria filter Hibernate yang digunakan bersama oleh metode
	 * {@code onSearchDefault} (untuk menampilkan data) dan {@code Common.initPaging}
	 * (untuk menghitung total halaman paginasi). Semua kondisi filter dari antarmuka
	 * pengguna dikombinasikan di sini dalam satu Criteria.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Membangun Criteria dari {@code PenyusutanAsset} dengan beberapa alias join
	 * (assetDetail, asset, masterAsset). Filter yang diterapkan:
	 * (1) Filter satuan kerja: membatasi berdasarkan hierarki unit kerja yang dipilih
	 *     atau semua unit kerja yang diizinkan jika tidak ada pilihan spesifik.
	 * (2) Filter rentang tanggal penyusutan (pertanggal) menggunakan SQL date comparison.
	 * (3) Filter rentang tanggal beli aset (tanggalbeli) menggunakan SQL date comparison.
	 * (4) Filter nama aset dengan ILIKE (case-insensitive, partial match).
	 * (5) Filter merk aset dengan ILIKE pada nama merk masterAsset.
	 * (6) Filter jenis aset dari combobox jenis aset.
	 * (7) Filter ruang tempat aset berada dari banbox ruang.<br><br>
	 *
	 * <b>Parameter:</b><br>
	 * {@code order} — Jika {@code true}, kriteria ditambahkan urutan descending berdasarkan
	 * tanggal beli dan ID penyusutan. Jika {@code false}, hanya digunakan untuk count (paginasi).<br><br>
	 *
	 * <b>Return:</b><br>
	 * Objek {@code Criteria} Hibernate yang siap dieksekusi dengan {@code list()} atau
	 * {@code rowCount()} sesuai kebutuhan pemanggil.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Filter tambahan cukup ditambahkan sebagai {@code .add(...)} baru. Pastikan alias
	 * yang dibutuhkan sudah didefinisikan via {@code createAlias} di awal.
	 *
	 * @param order {@code true} untuk menambahkan klausa ORDER BY pada kriteria.
	 * @return Objek {@code Criteria} untuk kueri data penyusutan aset.
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
		Criteria criteria = session.createCriteria(PenyusutanAsset.class).createAlias("assetDetail", "assetDetail")
				.createAlias("assetDetail.asset", "asset").createAlias("asset.masterAsset", "masterAsset")

				.add(Restrictions.or(Restrictions.isNull("assetDetail.satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("assetDetail.satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("assetDetail.satuanKerja", satuanKerjas))));

		if (order)
			criteria.addOrder(Order.desc("assetDetail.tanggalBeli")).addOrder(Order.desc("id"));

		criteria.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
				"date(this_.pertanggal) between date('" + Common.databaseDateFormat.get().format(start.getValue())
						+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add((startBeli == null || endBeli == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(tanggalbeli) between date('" + Common.databaseDateFormat.get().format(startBeli.getValue())
								+ "') and date('" + Common.databaseDateFormat.get().format(endBeli.getValue()) + "')")))

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
	 * Memuat ulang data grid penyusutan aset berdasarkan kriteria filter saat ini.
	 *
	 * <b>Tujuan:</b><br>
	 * Metode ini merupakan implementasi dari antarmuka {@code DataSearchDefault} dan
	 * berfungsi sebagai titik pusat untuk me-refresh tampilan grid daftar penyusutan
	 * aset. Dipanggil setelah pengguna mengubah filter, menambah/mengubah/menghapus
	 * data, atau saat timer auto-refresh terpicu.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memanggil {@code Common.initPaging} dengan kriteria tanpa order untuk menghitung
	 *     total data dan mengatur komponen paginasi.
	 * (2) Mengeksekusi kueri Hibernate dengan criteria yang sudah diurutkan, dibatasi
	 *     {@code ROWS_COUNT_ON_PAGE} baris, dimulai dari offset halaman aktif paginasi.
	 * (3) Membungkus hasil dalam {@code SimpleListModel} dan mengatur renderer grid
	 *     ke {@code PenyusutanAssetRenderer} sebelum menerapkan model ke grid.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari Hibernate diteruskan ke ZK framework. Tidak ada penanganan error
	 * lokal di metode ini.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika kolom pengurutan default perlu diubah, modifikasi klausa addOrder di
	 * metode {@code initCriteria}.
	 *
	 * @param event Event ZK yang memicu pencarian ulang, bisa {@code null} jika
	 *              dipanggil secara programatik (misalnya setelah simpan data).
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenyusutanAsset> penyusutanAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penyusutanAsset);
		grid.setRowRenderer(new PenyusutanAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

}
