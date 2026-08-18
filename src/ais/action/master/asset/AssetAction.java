package ais.action.master.asset;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import ais.action.master.asset.helper.AmbilDataMasterAssetBanbox;
import ais.action.master.asset.helper.AssetDetailAction;
import ais.action.master.dashboard.helper.DashboardRekapAset;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.Report;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Asset;
import ais.database.model.asset.AssetDetail;
import ais.database.model.asset.JenisAsset;
import ais.database.model.asset.KelompokAsset;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h3>AssetAction — Manajemen CRUD Barang Inventaris / Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini mengelola seluruh siklus hidup data aset (barang inventaris tidak habis
 * pakai) dalam sistem e-Campus. Fungsi utamanya mencakup: menampilkan daftar aset
 * dengan berbagai filter (nama, jenis, kelompok, lokasi, ruangan, satuan kerja),
 * menambah aset baru, mengubah aset yang ada, menghapus aset, mengunduh dan mengunggah
 * data dalam format Excel (dengan barcode), serta mencetak QR code / barcode untuk
 * seluruh aset yang sesuai filter. Setiap aset ({@code Asset}) merupakan kelompok
 * dari detail-detail fisik ({@code AssetDetail}) yang merepresentasikan unit-unit
 * individual aset tersebut. Kelas ini mengimplementasikan antarmuka
 * {@code DataCriteria}, {@code DataSearchDefault}, dan {@code DataInitDefault}
 * untuk kompatibilitas dengan utilitas cetak dan upload generik sistem.<br>
 * <br>
 *
 * <b>Cara kerja:</b><br>
 * Setelah ZK menginisialisasi composer via {@code doAfterCompose}, semua filter
 * diinisialisasi, tombol aksi dikonfigurasi sesuai hak pengguna, dan data awal
 * dimuat secara asinkron melalui {@code Common.createDefaultTimer}. Setiap baris
 * dirender oleh inner class {@code AssetRenderer} yang menampilkan sub-panel
 * {@code AssetDetailAction} untuk daftar detail unit, informasi master aset, jenis,
 * kelompok, keterangan, link ke dokumen pengadaan/saldo awal terkait, serta jumlah
 * unit dan tombol CRUD standar. Form tambah/ubah dirender secara programatik di
 * dalam {@code MyWindow} modal. Fitur cetak QR code menghasilkan file PDF berisi
 * QR code dan barcode untuk setiap unit aset menggunakan library Barbecue dan
 * laporan Jasper. Fitur upload Excel memungkinkan impor massal data {@code AssetDetail}
 * dengan pencocokan master aset berdasarkan kode atau nama.<br>
 * <br>
 *
 * <b>Threading:</b><br>
 * Semua operasi utama berjalan di thread UI ZK (event dispatch thread). Proses
 * render QR code per unit dalam handler "QRcode Semua" berjalan sinkron di thread
 * UI; untuk dataset besar pertimbangkan memindahkannya ke thread latar. Operasi
 * upload dilakukan oleh framework {@code Common.uploadData} yang mengelola
 * threading-nya sendiri.<br>
 * <br>
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas ini bergantung pada {@code SatuanKerjaTreeModel} untuk mendukung filter
 * hierarki satuan kerja (induk beserta seluruh turunannya). Saat ruangan dipilih
 * sebagai filter, query dialihkan ke entitas {@code AssetDetail} (bukan {@code Asset})
 * karena ruangan tercatat di level detail. Pastikan template laporan Jasper
 * {@code asset/crcode_asset} tersedia di direktori report. Field {@code barcode}
 * pada {@code AssetDetail} bersifat unik — fungsi {@code AssetDetail.generateBarcode}
 * dipanggil otomatis saat upload jika barcode kosong.
 */
public class AssetAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Versi serial untuk serialisasi kelas ini sesuai mekanisme ZK composer.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Window modal yang digunakan untuk form tambah dan ubah aset. */
	private MyWindow addWindow;

	/** Komponen paging untuk navigasi halaman data grid. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar aset. */
	private MyGrid grid;

	/** Kotak teks untuk filter berdasarkan nama aset/barcode. */
	private Textbox searchnama;

	/** Combobox untuk filter berdasarkan jenis aset. */
	private Combobox searchjenisAsset;

	/** Combobox untuk filter berdasarkan kelompok aset. */
	private Combobox searchkelompokAsset;

	/** Combobox untuk filter berdasarkan lokasi aset. */
	private Combobox searchLokasi;

	/** Banbox untuk filter berdasarkan ruangan spesifik. */
	private AmbilDataRuangBanbox searchruang;

	/** Banbox untuk filter berdasarkan satuan kerja (mendukung hierarki). */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** Banbox untuk input master aset pada form tambah/ubah. */
	private AmbilDataMasterAssetBanbox masterAsset;

	/** Kotak teks untuk input keterangan tambahan aset pada form. */
	private Textbox keterangan;

	/** Flag apakah pengguna memiliki hak UPDATE. */
	private boolean edit = false;

	/** Flag apakah pengguna memiliki hak DELETE. */
	private boolean delete = false;

	/** Entitas aset yang sedang diedit (null jika mode tambah). */
	private Asset asset;

	/** Tombol tambah aset baru di toolbar. */
	private MyToolbarbuttonConfig add;

	/** Panel tab untuk menampilkan rekap inventaris aset. */
	private Tabpanel rekapInventaris;

	/** Pengguna yang sedang login. */
	private Tbmuser tbmuser = null;

	/** Model pohon satuan kerja untuk mendukung filter hierarki. */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * <b>Tujuan:</b> Menangani event tab "Rekap Inventaris" yang diklik oleh pengguna.
	 * Memuat dashboard rekap aset secara lazy (hanya jika tab belum pernah dimuat)
	 * untuk menghemat waktu inisialisasi halaman.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Memeriksa apakah {@code rekapInventaris} sudah memiliki anak komponen.
	 * Jika belum, membuat instance {@code DashboardRekapAset} dan memasangnya
	 * ke dalam panel menggunakan {@code BaseDasbordPortal.mountWrapped} dengan
	 * judul dan deskripsi yang sesuai. Pemuatan lazy ini memastikan dashboard
	 * yang berat tidak dimuat kecuali benar-benar dibutuhkan.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK dari klik tab, tidak digunakan secara langsung.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dari {@code mountWrapped} akan dipropagasikan
	 * ke ZK exception handler.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jika {@code DashboardRekapAset} memerlukan parameter
	 * tambahan di masa depan, sesuaikan di sini.
	 *
	 * @param event event ZK dari klik tab Rekap Inventaris
	 */
	public void onInventaris(Event event) {

		if (rekapInventaris.getChildren().size() == 0) {
			DashboardRekapAset laporan = new DashboardRekapAset();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapInventaris,
				"Rekap Aset", "Gambaran jumlah, nilai, dan kondisi aset/inventaris yang dimiliki institusi.");
		}
	}

	/**
	 * <b>Tujuan:</b> Dipanggil ZK sebelum composer di-compose ke halaman.
	 * Melakukan pengecekan keamanan agar halaman hanya dapat diakses oleh
	 * pengguna yang telah terautentikasi.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi sesi dan
	 * hak akses, kemudian mendelegasikan ke superclass.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code page} — halaman ZK yang sedang dimuat.<br>
	 * {@code parent} — komponen induk tempat composer dipasang.<br>
	 * {@code compInfo} — metadata komponen dari ZK framework.<br>
	 * <br>
	 * <b>Return:</b> {@code ComponentInfo} dari superclass untuk melanjutkan proses compose.<br>
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
	 * halaman selesai di-wire. Bertanggung jawab atas inisialisasi penuh halaman
	 * manajemen aset: validasi sesi, pengisian filter combo, konfigurasi tombol
	 * aksi, setup upload/download Excel, setup QR code massal, dan pemuatan data awal.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * 1. Memvalidasi sesi dan hak READ; jika gagal, diarahkan ke logoff.<br>
	 * 2. Mengambil pengguna aktif dan membangun {@code SatuanKerjaTreeModel}.<br>
	 * 3. Mengisi combo filter (kelompok, lokasi, jenis aset) dari database.<br>
	 * 4. Mengatur visibilitas tombol tambah berdasarkan hak CREATE.<br>
	 * 5. Menetapkan flag {@code edit} dan {@code delete} dari hak UPDATE/DELETE.<br>
	 * 6. Menginisialisasi event paging dan event perubahan filter ruangan.<br>
	 * 7. Mendaftarkan fitur cetakData (download barcode Excel) dan uploadData
	 *    (upload Excel dengan pencocokan master aset).<br>
	 * 8. Menambahkan tombol "QRcode Semua" yang menghasilkan PDF berisi QR code
	 *    dan barcode untuk semua aset yang sesuai filter aktif.<br>
	 * 9. Memuat data awal via timer default ZK.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code comp} — root komponen ZK hasil compose.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception pada proses upload/cetak ditangani per-item
	 * via {@code Common.tampilErrorJikaAdmin}. Exception inisialisasi akan dipropagasikan.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jika template laporan QR code berubah, sesuaikan nama
	 * template pada pemanggilan {@code Report.generatePDFReport}.
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

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		Common.insertComboDanSemua(searchkelompokAsset, new String[] { "nama", "id" }, "keterangan",
				KelompokAsset.class);
		Common.insertComboDanSemua(searchLokasi, new String[] { "nama", "alamat" }, "keterangan", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(searchjenisAsset, new String[] { "nama", "id" }, "keterangan", JenisAsset.class);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		searchruang.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "asset.masterAsset", "pemilikAsset", "lokasi", "ruang", "satuanKerja",
				"barcode", "nama", "statusAsset", "hargaBeli", "tanggalBeli", "alamat", "detailAlamat", "keterangan",
				"saranaBersama" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				return initCriteriaDetail(order);
			}
		}, contents);
		if (cetakToolbarbutton != null) { cetakToolbarbutton.setLabel("Download barcode"); }
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AssetDetail.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] data = (Object[]) arg0.getData();
				AssetDetail detail = (AssetDetail) data[0];
				Session session = (Session) data[1];
				@SuppressWarnings("rawtypes")
				Map datum = (Map) data[2];
				try {
					String masterAssetStr = (String) datum.get("asset.masterAsset");
					for (Object d : datum.keySet()) {
						if (d != null && d.toString().trim().equalsIgnoreCase("asset.masterAsset")) {
							masterAssetStr = (String) datum.get(d);
						}
					}

					System.out.println("masterAssetStr -> " + masterAssetStr);

					MasterAsset masterAsset = (MasterAsset) session.createCriteria(MasterAsset.class)
							.add(Restrictions.ilike("kode", masterAssetStr.trim(), MatchMode.EXACT)).setMaxResults(1)
							.addOrder(Order.asc("id")).uniqueResult();
					if (masterAsset == null) {
						masterAsset = (MasterAsset) session.createCriteria(MasterAsset.class)
								.add(Restrictions.ilike("nama", masterAssetStr.trim(), MatchMode.EXACT))
								.setMaxResults(1).addOrder(Order.asc("id")).uniqueResult();
					}

					if (masterAsset != null) {
						Asset asset = (Asset) session.createCriteria(Asset.class)
								.add(Restrictions.eq("masterAsset", masterAsset)).setMaxResults(1)
								.addOrder(Order.asc("id")).uniqueResult();
						if (asset == null) {
							asset = new Asset();
							asset.setMasterAsset(masterAsset);
							asset.setTbmuser(tbmuser);
							asset.setDibuatOleh(tbmuser);
							session.save(asset);
							session.flush();
						}

						detail.setAsset(asset);

						if (detail.getBarcode() == null || detail.getBarcode().trim().isEmpty()) {
							detail.setBarcode(AssetDetail.generateBarcode(detail, null, true));
						}
					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

		}, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				HibernateUtil.currentSession().createSQLQuery("delete from asset.asset_detail where asset is null;")
						.executeUpdate();
			}

		}, contents);
		if (upload != null) { upload.setLabel("Upload barcode"); }
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("QRcode Semua", "/img/print.png");
		if (button != null) { button.setParent(add.getParent()); }
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();
				List<AssetDetail> assetDetails = session.createCriteria(AssetDetail.class).addOrder(Order.desc("id"))

						.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										Restrictions.ilike("barcode", searchnama.getValue(), MatchMode.ANYWHERE),
										Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE)))

						.createAlias("asset", "asset").createAlias("asset.masterAsset", "masterAsset")

						.add(searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("ruang", searchruang.getAttribute("ruang")))

						.add(searchLokasi.getSelectedItem() == null || searchLokasi.getSelectedItem().getValue() == null
								|| searchLokasi.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("lokasi", searchLokasi.getSelectedItem().getValue()))

						.add(searchjenisAsset.getSelectedItem() == null
								|| searchjenisAsset.getSelectedItem().getValue() == null
								|| searchjenisAsset.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("masterAsset.jenisAsset",
												searchjenisAsset.getSelectedItem().getValue()))
						.add(searchkelompokAsset.getSelectedItem() == null
								|| searchkelompokAsset.getSelectedItem().getValue() == null
								|| searchkelompokAsset.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("masterAsset.kelompokAsset",
												searchkelompokAsset.getSelectedItem().getValue()))

						.addOrder(Order.asc("asset.nama"))

						.list();

				Map parameters = ais.common.HashMapGenerator.getRand();
				List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
				for (AssetDetail assetDetail : assetDetails) {
					try {
						String lokasigambar = null;
						Asset asset = assetDetail.getAsset();

						LampiranLain gambar = LampiranLain.ambil(asset.getMasterAsset().getId(),
								LampiranLain.GAMBAR_MASTER_ASSET);

						if (gambar != null) {
							lokasigambar = FileFotoLain.ambilLinkLampiranLain(gambar, false, false, LampiranLain.class);
						}

						System.out.println("lokasigambar -> " + lokasigambar);

						Map map = new java.util.HashMap<String, Serializable>();
						map.put("status",
								assetDetail.getStatusAsset() == null ? "" : assetDetail.getStatusAsset().getNama());
						map.put("judul", assetDetail.getNama());

						File myfilebarcode = new File(
								Common.ambilREAL_PATH_REPORT() + "/barcode_" + assetDetail.getBarcode() + ".png");

						Barcode mybarcode = BarcodeFactory.createCode128B(assetDetail.getBarcode());
						BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
						map.put("barcode", myfilebarcode.getAbsolutePath());
						map.put("barcode_data", assetDetail.getBarcode());
						map.put("ruang", assetDetail.getRuang() == null ? ""
								: assetDetail.getRuang().getKodeRuangan() + "-" + assetDetail.getRuang().getNama());
						map.put("tanggal", assetDetail.getTanggalBeli());
						map.put("tanggal_format", Common.dateFormat1.get().format(assetDetail.getTanggalBeli()));
						Common.insertProperty(AssetDetail.class, assetDetail, map, "");
						map.put("gambar", lokasigambar);

						String code = assetDetail.getBarcode() + "\n" + assetDetail.getNama() + "\n"
								+ (assetDetail.getRuang() == null ? ""
										: assetDetail.getRuang().getKodeRuangan() + "-"
												+ assetDetail.getRuang().getNama() + "\n")
								+ Common.dateFormat1.get().format(assetDetail.getTanggalBeli());

						myfilebarcode = new File(
								Common.ambilREAL_PATH_REPORT() + "/crcode_" + assetDetail.getBarcode() + ".png");

						BarcodeCommon.generateCRCode(code, myfilebarcode);
						map.put("cr_code", myfilebarcode.getAbsolutePath());

						maps.add(map);
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
				Common.insertProperty(Asset.class, asset, parameters, "", 3);
				Report.generatePDFReport(Report.PDF, parameters, "asset/crcode_asset", ais.ui.util.WaktuUtil.getDate(),
						maps);
			}
		});
		FilterLanjutHelper.setup(comp);
	}

	/**
	 * <b>Tujuan:</b> Inner class renderer baris grid yang menampilkan satu entitas
	 * {@code Asset} beserta sub-panel detail unit, informasi master aset, dan
	 * tombol CRUD standar.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Untuk setiap baris, renderer membuat instance {@code AssetDetailAction} yang
	 * ditampilkan sebagai sub-komponen inline dalam baris grid, lalu menambahkan
	 * revisi kode, revisi nama, jenis aset, kelompok aset, keterangan, link ke
	 * dokumen sumber (penerimaan pengadaan, permintaan pengadaan, atau saldo awal),
	 * jumlah unit (count dari {@code AssetDetail}), dan tombol salin/ubah/hapus.
	 * Logika link dokumen sumber menggunakan if-else berantai untuk memprioritaskan
	 * penerimaan pengadaan, kemudian permintaan pengadaan, kemudian saldo awal.<br>
	 * <br>
	 * <b>Threading:</b> Berjalan di thread UI ZK, menggunakan sesi Hibernate managed.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Urutan kolom renderer harus sesuai dengan deklarasi
	 * {@code <columns>} di file ZUL terkait.
	 */
	class AssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data aset ke dalam komponen ZK Row,
		 * termasuk sub-panel detail unit, informasi hierarki, dokumen sumber,
		 * dan tombol aksi CRUD.<br>
		 * <br>
		 * <b>Cara kerja:</b><br>
		 * Meng-cast {@code arg1} ke {@code Asset}. Membuat {@code AssetDetailAction}
		 * sebagai komponen anak baris pertama. Mengambil dan menampilkan kode master
		 * aset, nama, jenis, kelompok, keterangan, link dokumen sumber (dicari secara
		 * hierarkis: saldo awal via penerimaan → permintaan pengadaan → saldo awal
		 * langsung). Menghitung jumlah unit via proyeksi rowCount Hibernate. Terakhir
		 * menambahkan tombol salin/ubah/hapus standar.<br>
		 * <br>
		 * <b>Parameter:</b><br>
		 * {@code arg0} — Row ZK tempat komponen ditambahkan.<br>
		 * {@code arg1} — objek {@code Asset} yang dirender.<br>
		 * <br>
		 * <b>Return:</b> void<br>
		 * <br>
		 * <b>Penanganan error:</b> Exception dipropagasikan ke ZK exception handler.<br>
		 * <br>
		 * <b>Pemeliharaan:</b> Jika relasi entitas Asset berubah, perbarui logika
		 * penentuan link dokumen sumber di metode ini.
		 *
		 * @param arg0 Row ZK yang akan diisi komponen
		 * @param arg1 objek data Asset yang akan dirender
		 * @throws Exception jika terjadi error saat query atau konstruksi komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Asset asset = (Asset) arg1;

			final AssetDetailAction assetDetailAction = new AssetDetailAction(asset,
					(SatuanKerja) searchparent.getAttribute("satuanKerja"),
					(Lokasi) (searchLokasi.getSelectedItem() == null ? null
							: searchLokasi.getSelectedItem().getValue()),
					(Ruang) searchruang.getAttribute("ruang"), new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					});
			assetDetailAction.setParent(arg0);

			RevisiHelper.createNewRevisi(Asset.class, asset,
					asset.getMasterAsset() == null ? "" : asset.getMasterAsset().getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(Asset.class, asset, asset.getNama()).setParent(arg0);

			new Label(asset.getMasterAsset() == null ? ""
					: asset.getMasterAsset().getJenisAsset() == null ? ""
							: asset.getMasterAsset().getJenisAsset().getNama())
					.setParent(arg0);
			new Label(asset.getMasterAsset() == null ? ""
					: asset.getMasterAsset().getKelompokAsset() == null ? ""
							: asset.getMasterAsset().getKelompokAsset().getNama())
					.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(asset.getKeterangan()).setParent(vbox);

			// Link BAST (penerimaan barang)
			if (asset.getSaldoAwalMasterAssetDetail() != null
					&& asset.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail() != null
					&& asset.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
							.getPenerimaanPengadaanMasterAsset() != null) {

				final PenerimaanPengadaanMasterAsset bast = asset.getSaldoAwalMasterAssetDetail()
						.getPenerimaanPengadaanMasterAssetDetail().getPenerimaanPengadaanMasterAsset();
				A aaa = new A("BAST: " + bast.getKode());
				aaa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						PenerimaanPengadaanMasterAssetAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, bast);
					}
				});
				aaa.setStyle("font-size:12px;");
				aaa.setParent(vbox);

			} else if (asset.getPermintaanPengadaanMasterAssetDetail() != null) {

				final PermintaanPengadaanMasterAsset prq = asset.getPermintaanPengadaanMasterAssetDetail()
						.getPermintaanPengadaanMasterAsset();
				A aaa = new A(prq.getKode());
				aaa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						PermintaanPengadaanMasterAssetAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, prq);
					}
				});
				aaa.setStyle("font-size:12px;");
				aaa.setParent(vbox);
			}

			// Link Tagihan Vendor (saldo awal / invoice) — ditampilkan terpisah dari BAST
			if (asset.getSaldoAwalMasterAssetDetail() != null
					&& asset.getSaldoAwalMasterAssetDetail().getSaldoAwal() != null) {

				final SaldoAwalMasterAsset tagihan = asset.getSaldoAwalMasterAssetDetail().getSaldoAwal();
				String labelTagihan = "Tagihan: " + tagihan.getKode();
				if (tagihan.getJsonTermin() != null) {
					try {
						org.json.JSONObject jTermin = new org.json.JSONObject(tagihan.getJsonTermin());
						if (!jTermin.isNull("kode")) {
							labelTagihan = "Tagihan Termin: " + jTermin.getString("kode");
						}
					} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/master/asset/AssetAction.java:673");
					}
				}
				A aaaTagihan = new A(labelTagihan);
				aaaTagihan.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						SaldoAwalMasterAssetAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, tagihan);
					}
				});
				aaaTagihan.setStyle("font-size:12px;");
				aaaTagihan.setParent(vbox);
			}
			Session session = HibernateUtil.currentSession();
			Number jml = (Number) session.createCriteria(AssetDetail.class).add(Restrictions.eq("asset", asset))
					.setProjection(Projections.rowCount()).uniqueResult();
			new Label(Common.numberFormat.get().format(jml)).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, asset, AssetAction.this).setParent(arg0);
		}

	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" di toolbar untuk membuka
	 * form penambahan aset baru dalam dialog modal.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Membuat instance {@code Asset} baru (kosong) dan memanggil {@code init(Asset)}
	 * untuk menginisialisasi form, kemudian membuat window modal terlihat dan aktif.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK dari klik tombol Tambah.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dari {@code init} akan dipropagasikan.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Metode ini mengikuti konvensi penamaan ZK event handler
	 * {@code onAdd} yang dipanggil otomatis oleh framework melalui binding ZUL.
	 *
	 * @param event event ZK dari klik tombol Tambah
	 * @throws Exception jika inisialisasi form gagal
	 */
	public void onAdd(Event event) throws Exception {
		init(new Asset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Implementasi antarmuka {@code DataInitDefault} yang dipanggil
	 * oleh framework ketika pengguna mengklik tombol ubah (salin/edit) pada baris
	 * grid. Membuka form edit untuk aset yang dipilih.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Meng-cast {@code obj} ke {@code Asset}, menyimpannya ke field instance,
	 * memanggil {@code init(Asset)} untuk mengisi form dengan data aset tersebut,
	 * lalu menampilkan window modal.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code obj} — entitas {@code Asset} yang akan diedit, dibungkus sebagai
	 * {@code GeneralValueObject}.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> ClassCastException jika {@code obj} bukan {@code Asset}.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Metode ini wajib ada sebagai implementasi kontrak
	 * {@code DataInitDefault} yang digunakan oleh {@code Common.copyEditDeleteButtons}.
	 *
	 * @param obj entitas Asset yang akan diedit, dicast dari GeneralValueObject
	 * @throws Exception jika inisialisasi form gagal
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		asset = (Asset) obj;
		init(asset);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Menginisialisasi dan menampilkan form tambah/ubah aset di
	 * dalam window modal, termasuk membangun semua komponen ZK form secara programatik.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Mengatur judul window (berbeda antara mode tambah dan ubah), membersihkan
	 * konten window lama, lalu membangun layout Borderlayout dengan Center (grid form)
	 * dan South (toolbar Batal/Simpan). Form berisi dua field: pilihan master aset
	 * (via {@code AmbilDataMasterAssetBanbox}) dan keterangan (textarea). Tombol Batal
	 * menyembunyikan window; tombol Simpan memanggil {@code onSave} dan jika berhasil
	 * menyembunyikan window dan me-refresh grid.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code asset} — entitas {@code Asset} yang akan diisi ke form; jika ID null
	 * berarti mode tambah, jika ID tidak null berarti mode ubah.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dari konstruksi komponen ZK akan dipropagasikan.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Tambahkan field form baru di sini jika model {@code Asset}
	 * diperluas. Pastikan field baru juga ditangani di {@code onSave}.
	 *
	 * @param asset entitas Asset yang akan ditampilkan di form (baru atau existing)
	 * @throws Exception jika konstruksi komponen ZK gagal
	 */
	private void init(Asset asset) throws Exception {
		this.asset = asset;
		addWindow.setTitle(asset.getId() == null ? "Tambah Barang Inventaris" : "Ubah Barang Inventaris");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Asset *"));
		row.appendChild(masterAsset = new AmbilDataMasterAssetBanbox(MasterAsset.TIPE_TIDAK_HABIS_PAKAI));
		masterAsset.setAttribute("masterAsset", asset.getMasterAsset());
		masterAsset.setValue(asset.getMasterAsset() == null ? "" : asset.getMasterAsset().getNama());
		masterAsset.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(asset.getKeterangan() == null ? "" : asset.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
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
	 * <b>Tujuan:</b> Menyimpan data aset (baru atau yang diubah) ke database
	 * setelah melakukan validasi input dari form modal.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Memvalidasi bahwa master aset telah dipilih. Jika aset sudah ada (ID tidak null),
	 * meload ulang entitas dari sesi Hibernate untuk menghindari masalah detached
	 * object. Kemudian mengisi field aset dari nilai form (masterAsset, keterangan,
	 * tbmuser, dibuatOleh) dan menyimpannya via {@code Common.refreshSaveOrUpdate}.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK dari tombol Simpan, tidak digunakan secara langsung
	 * dalam logika validasi.<br>
	 * <br>
	 * <b>Return:</b> {@code true} jika simpan berhasil, {@code false} jika validasi
	 * gagal (master aset belum dipilih).<br>
	 * <br>
	 * <b>Penanganan error:</b> Jika master aset null, menampilkan pesan peringatan
	 * dan mengembalikan false. Exception Hibernate akan dipropagasikan.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Tambahkan validasi field baru di awal metode ini sebelum
	 * blok simpan. Jika ada field wajib baru di model Asset, pastikan divalidasi.
	 *
	 * @param event event ZK dari tombol Simpan
	 * @return true jika simpan berhasil, false jika validasi gagal
	 * @throws Exception jika terjadi error saat operasi database
	 */
	public boolean onSave(Event event) throws Exception {
		if (masterAsset.getAttribute("masterAsset") == null) {
			MyMessageboxConfig.show("Mohon maaf, Master Aset belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Master Aset; (2) Cari dan pilih aset dari daftar master aset yang tersedia; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (asset.getId() != null) {
			asset = (Asset) session.load(Asset.class, asset.getId());

		}

		asset.setMasterAsset((MasterAsset) masterAsset.getAttribute("masterAsset"));
		asset.setKeterangan(keterangan.getValue());
		asset.setTbmuser(tbmuser);
		asset.setDibuatOleh(tbmuser);

		Common.refreshSaveOrUpdate(session, asset);

		return true;
	}

	/**
	 * <b>Tujuan:</b> Membangun {@code Criteria} Hibernate untuk mengambil data
	 * {@code AssetDetail} (bukan {@code Asset}) yang digunakan untuk fungsi
	 * download barcode Excel. Criteria ini mencakup filter nama, ruangan, jenis
	 * aset, lokasi, dan kelompok aset.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Membuat criteria pada {@code AssetDetail} dengan join ke alias {@code asset}
	 * dan {@code masterAsset}. Filter nama menggunakan ILIKE pada nama master aset.
	 * Filter ruangan, lokasi, jenis, dan kelompok diterapkan jika combo terkait
	 * memiliki item terpilih yang tidak null. Jika {@code order} true, diurutkan
	 * berdasarkan nama master aset (asc) lalu barcode (asc).<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code order} — apakah hasil harus diurutkan.<br>
	 * <br>
	 * <b>Return:</b> Objek {@code Criteria} siap eksekusi untuk query detail aset.<br>
	 * <br>
	 * <b>Penanganan error:</b> Guard null-check pada setiap kondisi filter mencegah NPE.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Berbeda dari {@code initCriteria} yang menggunakan
	 * {@code Asset} sebagai root entity. Metode ini khusus untuk kebutuhan ekspor
	 * detail (barcode level unit).
	 *
	 * @param order apakah hasil harus diurutkan berdasarkan nama dan barcode
	 * @return objek Criteria Hibernate untuk query AssetDetail
	 */
	public Criteria initCriteriaDetail(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AssetDetail.class).createAlias("asset", "asset")
				.createAlias("asset.masterAsset", "masterAsset");

		if (order)
			criteria.addOrder(Order.asc("masterAsset.nama")).addOrder(Order.asc("barcode"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("masterAsset.nama", searchnama.getValue(), MatchMode.ANYWHERE))

				.add(searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruang", searchruang.getAttribute("ruang")))

				.add(searchjenisAsset.getSelectedItem() == null || searchjenisAsset.getSelectedItem().getValue() == null
						|| searchjenisAsset.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("masterAsset.jenisAsset",
										searchjenisAsset.getSelectedItem().getValue()))

				.add(searchLokasi.getSelectedItem() == null || searchLokasi.getSelectedItem().getValue() == null
						|| searchLokasi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("lokasi", searchLokasi.getSelectedItem().getValue()))

				.add(searchkelompokAsset.getSelectedItem() == null
						|| searchkelompokAsset.getSelectedItem().getValue() == null
						|| searchkelompokAsset.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("masterAsset.kelompokAsset",
										searchkelompokAsset.getSelectedItem().getValue()));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Membangun {@code Criteria} Hibernate utama untuk menampilkan
	 * daftar aset di grid, mendukung dua mode query berbeda tergantung apakah filter
	 * ruangan aktif atau tidak.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Menentukan set satuan kerja yang relevan berdasarkan filter hierarki satuan kerja
	 * (jika ada satuan kerja terpilih, ambil juga semua turunannya via
	 * {@code SatuanKerjaTreeModel.getChildsSet}). Kemudian:<br>
	 * - Jika filter ruangan aktif: query dilakukan pada {@code AssetDetail} dengan
	 *   proyeksi groupProperty "asset" sehingga menghasilkan daftar {@code Asset} unik
	 *   yang memiliki unit di ruangan tersebut. Filter satuan kerja diterapkan dengan
	 *   logika OR antara null (tanpa satuan kerja) dan member set.<br>
	 * - Jika filter ruangan tidak aktif: query dilakukan langsung pada {@code Asset}
	 *   dengan join ke masterAsset, filter satuan kerja, nama, lokasi, jenis, dan kelompok.<br>
	 * Filter satuan kerja menggunakan logika OR yang kompleks untuk mendukung kasus
	 * aset tanpa satuan kerja (null) dan aset milik satuan kerja dalam set.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code order} — apakah hasil harus diurutkan.<br>
	 * <br>
	 * <b>Return:</b> Objek {@code Criteria} siap dieksekusi, bisa query {@code Asset}
	 * atau {@code AssetDetail} tergantung mode.<br>
	 * <br>
	 * <b>Penanganan error:</b> Jika set satuan kerja kosong, {@code sqlRestriction("1=1")}
	 * digunakan sebagai passthrough (tampilkan semua).<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Perhatikan bahwa tipe return criteria berbeda antara dua
	 * cabang (AssetDetail vs Asset). Pemanggil ({@code onSearchDefault}) menggunakan
	 * raw type sehingga ini aman, tapi perlu perhatian ekstra jika direfaktor.
	 *
	 * @param order apakah hasil harus diurutkan
	 * @return objek Criteria Hibernate untuk query Asset atau AssetDetail
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
		Criteria criteria;
		if (searchruang.getAttribute("ruang") != null) {

			criteria = session.createCriteria(AssetDetail.class)

					.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("satuanKerja"),
									Restrictions.or(
											parent == null ? Restrictions.isNull("satuanKerja")
													: Restrictions.sqlRestriction("false"),
											Restrictions.in("satuanKerja", satuanKerjas))))

					.setProjection(Projections.groupProperty("asset"))

					.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.ilike("barcode", searchnama.getValue(), MatchMode.ANYWHERE),
									Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE)))

					.createAlias("asset", "asset").createAlias("asset.masterAsset", "masterAsset")

					.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))

					.add(searchLokasi.getSelectedItem() == null || searchLokasi.getSelectedItem().getValue() == null
							|| searchLokasi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("lokasi", searchLokasi.getSelectedItem().getValue()))

					.add(searchjenisAsset.getSelectedItem() == null
							|| searchjenisAsset.getSelectedItem().getValue() == null
							|| searchjenisAsset.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("masterAsset.jenisAsset",
											searchjenisAsset.getSelectedItem().getValue()))
					.add(searchkelompokAsset.getSelectedItem() == null
							|| searchkelompokAsset.getSelectedItem().getValue() == null
							|| searchkelompokAsset.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("masterAsset.kelompokAsset",
											searchkelompokAsset.getSelectedItem().getValue()));

		} else {

			criteria = session.createCriteria(Asset.class)

					.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("satuanKerja"),
									Restrictions.or(
											parent == null ? Restrictions.isNull("satuanKerja")
													: Restrictions.sqlRestriction("false"),
											Restrictions.in("satuanKerja", satuanKerjas))))

					.createAlias("masterAsset", "masterAsset");

			if (order)
				criteria.addOrder(Order.desc("id")).addOrder(Order.asc("masterAsset.nama")).addOrder(Order.asc("id"));

			criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
					: Restrictions.ilike("masterAsset.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

					.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))

					.add(searchLokasi.getSelectedItem() == null || searchLokasi.getSelectedItem().getValue() == null
							|| searchLokasi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("lokasi", searchLokasi.getSelectedItem().getValue()))

					.add(searchjenisAsset.getSelectedItem() == null
							|| searchjenisAsset.getSelectedItem().getValue() == null
							|| searchjenisAsset.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("masterAsset.jenisAsset",
											searchjenisAsset.getSelectedItem().getValue()))
					.add(searchkelompokAsset.getSelectedItem() == null
							|| searchkelompokAsset.getSelectedItem().getValue() == null
							|| searchkelompokAsset.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("masterAsset.kelompokAsset",
											searchkelompokAsset.getSelectedItem().getValue()));
		}
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Entry point publik untuk memuat ulang dan menampilkan data
	 * daftar aset di grid sesuai filter aktif. Dipanggil dari event handler ZK
	 * (tombol cari, perubahan filter, paging) maupun dari callback setelah simpan/hapus.<br>
	 * <br>
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.initPaging} untuk menghitung total baris dan mengatur
	 * state paging. Mengambil satu halaman data dari database menggunakan
	 * {@code initCriteria(true)} dengan limit dan offset sesuai halaman aktif.
	 * Membungkus hasil dalam {@code SimpleListModel} dan menerapkan renderer
	 * {@code AssetRenderer} ke grid.<br>
	 * <br>
	 * <b>Parameter:</b><br>
	 * {@code event} — event ZK pemicu, boleh null.<br>
	 * <br>
	 * <b>Return:</b> void<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception Hibernate akan dipropagasikan ke ZK.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Metode ini dipanggil secara konvensional oleh ZK melalui
	 * nama {@code onSearchDefault}. Jangan ubah nama metode ini.
	 *
	 * @param event event ZK pemicu, boleh null
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Asset> asset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(asset);
		grid.setRowRenderer(new AssetRenderer());
		grid.setModelCheckMobile(strset);

	}

}
