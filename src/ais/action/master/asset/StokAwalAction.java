package ais.action.master.asset;

import java.util.List;

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

import ais.action.master.asset.helper.AmbilDataMasterAssetBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.DetailTransaksiAsset;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.StokAwal;
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

/**
 * <h3>StokAwalAction — Kontroler CRUD Data Master Stok Awal Barang Habis Pakai</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah kontroler ZKoss (composer) yang mengelola operasi CRUD lengkap untuk entitas
 * {@link StokAwal}. Data stok awal merepresentasikan saldo pembuka (opening balance) jumlah
 * barang habis pakai yang dimiliki oleh setiap satuan kerja pada suatu tanggal tertentu. Data
 * ini menjadi dasar perhitungan stok berjalan di modul manajemen aset — tanpa stok awal yang
 * akurat, laporan persediaan dan mutasi barang tidak akan mencerminkan kondisi nyata gudang.
 * Setiap entitas {@link StokAwal} menghubungkan satu {@link MasterAsset} (barang habis pakai)
 * dengan satu {@link SatuanKerja} beserta jumlah dan tanggal perolehan stok awalnya.
 *
 * <b>Cara kerja:</b><br>
 * Controller mengimplementasikan tiga antarmuka standar: {@code DataCriteria} untuk query
 * Hibernate, {@code DataSearchDefault} untuk rendering grid, dan {@code DataInitDefault} untuk
 * inisialisasi form dari luar. Selain CRUD standar, kelas ini memiliki fitur khusus "Singkronkan
 * dengan stok" yang sangat penting: tombol ini menghapus semua entri {@link DetailTransaksiAsset}
 * yang berasal dari saldo awal, lalu membuatnya ulang berdasarkan data stok awal saat ini. Proses
 * sinkronisasi ini menjamin konsistensi antara tabel stok_awal dan detail_transaksi_asset saat
 * ada perubahan data stok awal yang perlu direfleksikan ke riwayat transaksi. Setiap kali
 * pengguna menyimpan satu entri stok awal, sinkronisasi parsial juga terjadi secara otomatis
 * untuk entri tersebut melalui timer asinkron ZKoss.
 *
 * <b>Threading:</b><br>
 * Operasi sinkronisasi massal ("Singkronkan dengan stok") dan pembaruan detail transaksi
 * setelah simpan menggunakan {@code Common.createDefaultTimer} yang menjalankan operasi
 * dalam timer ZKoss. Timer ini masih berjalan di thread event ZKoss namun dengan delay
 * kecil untuk memastikan UI tidak terblokir terlalu lama. Tidak ada thread terpisah yang
 * digunakan. Operasi SQL DELETE native ({@code session.createSQLQuery}) dieksekusi langsung
 * pada session Hibernate yang terikat ke thread event saat ini.
 *
 * <b>Pemeliharaan:</b><br>
 * Kode SQL native yang menghapus dari {@code asset.detail_transaksi_asset} hardcode nama skema
 * dan tabel. Jika skema database berubah, query SQL di tombol sinkronisasi dan {@code onSave}
 * perlu diperbarui. Konstanta {@code LibraryUtil.SALDO_AWAL} mendefinisikan kode transaksi
 * untuk entri saldo awal di tabel detail transaksi — jangan ubah nilai ini tanpa memastikan
 * konsistensi dengan modul pelaporan stok yang bergantung pada kode ini.
 *
 * @author Tim Pengembang AIS
 * @version 1.0
 * @see StokAwal
 * @see DetailTransaksiAsset
 * @see MasterAsset
 * @see SatuanKerja
 */
public class StokAwalAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Nomor versi serial untuk serialisasi kelas ini.
	 * Tidak perlu diubah kecuali ada perubahan struktur yang tidak kompatibel mundur.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZKoss yang digunakan untuk menampilkan form tambah atau ubah data stok awal. */
	private MyWindow addWindow;

	/** Komponen paging ZKoss untuk navigasi halaman pada grid daftar stok awal. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar seluruh data stok awal yang tersedia. */
	private MyGrid grid;

	/** Kotak teks pencarian berdasarkan nama atau kode barang habis pakai pada area filter. */
	private Textbox searchnama;

	/** Kotak teks input keterangan atau deskripsi entri stok awal pada form tambah/ubah. */
	private Textbox keterangan;

	/** Penanda apakah pengguna saat ini memiliki hak akses untuk mengubah data (UPDATE privilege). */
	private boolean edit = false;

	/** Penanda apakah pengguna saat ini memiliki hak akses untuk menghapus data (DELETE privilege). */
	private boolean delete = false;

	/** Referensi ke objek entitas stok awal yang sedang diproses (tambah atau ubah). */
	private StokAwal stokAwal;

	/** Tombol "Tambah" pada toolbar halaman, visibilitasnya dikendalikan oleh hak akses CREATE. */
	private MyToolbarbuttonConfig add;

	/**
	 * Komponen banbox (autocomplete input) untuk memilih barang habis pakai dari daftar
	 * master aset bertipe habis pakai.
	 */
	private AmbilDataMasterAssetBanbox masterAsset;

	/** Kotak input bilangan desimal untuk mengisi jumlah stok awal barang. */
	private MyDoublebox jumlah;

	/** Kotak input tanggal untuk mengisi tanggal perolehan atau pencatatan stok awal. */
	private MyDatebox tanggal;

	/**
	 * Komponen banbox untuk memilih satuan kerja yang memiliki stok awal ini.
	 * Satuan kerja menentukan unit organisasi yang bertanggung jawab atas stok barang.
	 */
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	/**
	 * Metode siklus hidup ZKoss yang dipanggil sebelum komponen halaman dibangun.
	 *
	 * <b>Tujuan:</b><br>
	 * Melakukan pemeriksaan keamanan akses pengguna sebelum halaman ZUL dirender sepenuhnya,
	 * mencegah akses tidak sah ke halaman manajemen stok awal.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi bahwa pengguna sudah
	 * login dan berwenang. Mendelegasikan ke implementasi induk untuk menyelesaikan proses
	 * autowiring ZKoss. Dipanggil otomatis oleh framework ZKoss sebelum composing.
	 *
	 * <b>Parameter:</b><br>
	 * @param page halaman ZKoss saat ini yang sedang dikompilasi
	 * @param parent komponen induk dalam hierarki komponen ZUL
	 * @param compInfo informasi metadata komponen yang sedang diproses
	 *
	 * <b>Return:</b><br>
	 * @return objek {@code ComponentInfo} dari kelas induk untuk kelanjutan proses composing
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika pemeriksaan keamanan gagal, pengguna akan diarahkan ke halaman login.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jangan hapus pemanggilan {@code super.doBeforeCompose()} karena diperlukan untuk
	 * mekanisme autowiring ZKoss yang menghubungkan komponen ZUL ke field Java.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Metode siklus hidup ZKoss yang dipanggil setelah seluruh komponen halaman selesai dibangun.
	 *
	 * <b>Tujuan:</b><br>
	 * Menginisialisasi seluruh logika bisnis halaman manajemen stok awal, termasuk pengaturan
	 * hak akses, pemuatan data awal, konfigurasi paging, tombol cetak/upload, dan tombol
	 * sinkronisasi stok yang merupakan fitur khusus halaman ini.
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menyelesaikan autowiring ZKoss dan mengatur bahasa antarmuka.
	 * (2) Mengatur visibilitas tombol Tambah berdasarkan hak akses CREATE.
	 * (3) Menetapkan flag {@code edit} dan {@code delete} berdasarkan hak akses pengguna.
	 * (4) Memuat data stok awal ke grid dan memasang event listener paging.
	 * (5) Menambahkan tombol cetak (ekspor Excel) dengan kolom: id, masterAsset, satuanKerja,
	 *     jumlah, tanggal, keterangan.
	 * (6) Menambahkan tombol unggah data (impor) untuk administrator.
	 * (7) Menambahkan tombol "Singkronkan dengan stok" yang mengeksekusi sinkronisasi massal:
	 *     menghapus semua DetailTransaksiAsset yang berasal dari stok awal dan saldo awal
	 *     asset detail, kemudian membuatnya ulang dari data stok awal saat ini. Proses ini
	 *     berjalan dalam timer asinkron untuk tidak memblokir UI terlalu lama.
	 *
	 * <b>Parameter:</b><br>
	 * @param comp komponen akar halaman ZUL yang sudah selesai dibangun oleh framework ZKoss
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan selama inisialisasi atau pemuatan data
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Tombol sinkronisasi menggunakan SQL native yang merujuk skema {@code asset} secara
	 * hardcode. Saat skema database berubah atau tabel direfaktor, query SQL di dalam listener
	 * tombol ini wajib diperbarui. Batas {@code setMaxResults(50000)} di query sinkronisasi
	 * membatasi jumlah data yang diproses sekaligus — pertimbangkan paginasi jika data melebihi batas.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "masterAsset", "satuanKerja", "jumlah", "tanggal", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(StokAwal.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, StokAwal.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Singkronkan dengan stok", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Stok Awal dengan Stok");

				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<StokAwal> longs = initCriteria(false).addOrder(Order.asc("id")).setMaxResults(50000)
								.list();

						String inSql = "";
						for (StokAwal l : longs) {
							inSql += inSql.isEmpty() ? l.getId() + "" : "," + l.getId();
						}

						if (!inSql.trim().isEmpty()) {

							Session session = HibernateUtil.currentSession();
							session.createSQLQuery(
									"delete from asset.detail_transaksi_asset where saldo_awal_master_asset_detail is not null;")
									.executeUpdate();

							session.createSQLQuery(
									"delete from asset.detail_transaksi_asset where stok_awal in (" + inSql + ");")
									.executeUpdate();

							int nomorBaris = 0;
							for (StokAwal stokAwal : longs) {
								String kunci = String.valueOf(stokAwal);
								try {
									DetailTransaksiAsset detailTransaksiAsset = new DetailTransaksiAsset();
									detailTransaksiAsset.setStokAwal(stokAwal);
									detailTransaksiAsset.setQtyBonus(0.0);
									detailTransaksiAsset.setMasterAsset(stokAwal.getMasterAsset());
									detailTransaksiAsset.setKeterangan("Transaksi Saldo Awal");
									detailTransaksiAsset.setKodeTransaksi(LibraryUtil.SALDO_AWAL);
									detailTransaksiAsset.setQty(stokAwal.getJumlah());
									detailTransaksiAsset.setTanggal(stokAwal.getTanggal());

									session.save(detailTransaksiAsset);
									session.flush();
									laporan.catatBerhasil(nomorBaris, kunci, "Sinkronisasi stok berhasil");
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
									laporan.catatGagalDetail(nomorBaris, kunci, e);
								}
								nomorBaris++;
							}
						}
						laporan.selesaikan(new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								onSearchDefault(null);
							}
						});
					}
				});

			}

		});
		if (button != null) { button.setParent(add.getParent()); }
	}

	/**
	 * Inner class renderer yang bertanggung jawab merender setiap baris data pada grid
	 * daftar stok awal barang habis pakai.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengkonversi objek {@link StokAwal} menjadi baris tampilan yang informatif di grid,
	 * menampilkan semua informasi relevan entitas beserta tombol aksi standar.
	 *
	 * <b>Cara kerja:</b><br>
	 * Untuk setiap objek {@link StokAwal} dalam model, metode {@code render} membuat Label
	 * untuk: kode barang (dari masterAsset), nama barang (melalui RevisiHelper), nama satuan
	 * kerja, jumlah (diformat sebagai angka desimal), tanggal (diformat sebagai tanggal),
	 * dan keterangan. Tombol salin, ubah, dan hapus ditambahkan via
	 * {@code Common.copyEditDeleteButtons} di sel terakhir.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Format angka menggunakan {@code Common.numberFormat.get()} dan format tanggal menggunakan
	 * {@code Common.dateFormat1.get()} yang merupakan ThreadLocal untuk thread-safety. Urutan
	 * komponen harus konsisten dengan header kolom di file ZUL.
	 */
	class StokAwalRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data stok awal ke dalam komponen ZKoss Row.
		 *
		 * <b>Tujuan:</b><br>
		 * Mengisi komponen {@link Row} ZKoss dengan semua data dari satu entitas {@link StokAwal},
		 * termasuk informasi barang, satuan kerja, jumlah, tanggal, dan tombol aksi.
		 *
		 * <b>Cara kerja:</b><br>
		 * Cast {@code arg1} ke {@link StokAwal} dan membuat Label untuk setiap atribut:
		 * kode masterAsset (string), nama masterAsset via RevisiHelper (untuk riwayat revisi),
		 * nama satuan kerja (string kosong jika null), jumlah yang diformat menggunakan
		 * {@code Common.numberFormat}, tanggal yang diformat menggunakan {@code Common.dateFormat1},
		 * dan keterangan. Di sel terakhir ditambahkan tombol standar salin, ubah, hapus.
		 *
		 * <b>Parameter:</b><br>
		 * @param arg0 objek {@link Row} ZKoss yang akan diisi dengan komponen-komponen data
		 * @param arg1 objek data {@link StokAwal} yang akan dirender sebagai satu baris
		 *
		 * <b>Penanganan error:</b><br>
		 * @throws Exception jika terjadi kesalahan saat membuat atau memasang komponen ZKoss,
		 *                   atau jika {@code stokAwal.getMasterAsset()} null (NullPointerException)
		 *
		 * <b>Pemeliharaan:</b><br>
		 * {@code Common.numberFormat.get()} dan {@code Common.dateFormat1.get()} adalah ThreadLocal
		 * — selalu gunakan {@code .get()} untuk mengakses formatter yang aman per thread.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final StokAwal stokAwal = (StokAwal) arg1;
			new Label(stokAwal.getMasterAsset().getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(StokAwal.class, stokAwal, stokAwal.getMasterAsset().getNama()).setParent(arg0);
			new Label(stokAwal.getSatuanKerja() == null ? "" : stokAwal.getSatuanKerja().getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(stokAwal.getJumlah())).setParent(arg0);
			new Label(Common.dateFormat1.get().format(stokAwal.getTanggal())).setParent(arg0);
			new Label(stokAwal.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, stokAwal, StokAwalAction.this).setParent(arg0);

		}

	}

	/**
	 * Event handler yang dipanggil ketika pengguna menekan tombol "Tambah" pada toolbar halaman.
	 *
	 * <b>Tujuan:</b><br>
	 * Membuka form tambah data stok awal baru dalam jendela modal untuk memungkinkan
	 * pengguna memasukkan data saldo pembuka barang habis pakai.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat objek {@link StokAwal} baru kosong tanpa ID, meneruskannya ke metode
	 * {@code init(StokAwal)} yang membangun form, kemudian menampilkan jendela modal.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZKoss dari klik tombol Tambah pada toolbar
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan saat membangun komponen form atau modal
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Dipanggil otomatis oleh ZKoss melalui konvensi penamaan event handler. Tidak ada
	 * logika tambahan yang diperlukan di sini selain memanggil init dengan entitas baru.
	 */
	public void onAdd(Event event) throws Exception {
		init(new StokAwal());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Implementasi antarmuka {@link DataInitDefault} untuk menginisialisasi form dari luar kelas.
	 *
	 * <b>Tujuan:</b><br>
	 * Menyediakan titik masuk publik untuk sistem generik (seperti tombol salin data) agar
	 * dapat membuka form edit stok awal dengan objek data yang sudah ditentukan.
	 *
	 * <b>Cara kerja:</b><br>
	 * Melakukan cast objek {@link GeneralValueObject} ke {@link StokAwal} dan menyimpannya ke
	 * field instance. Kemudian mendelegasikan ke metode {@code init(StokAwal)} yang membangun
	 * form dengan data tersebut, dan menampilkan jendela modal.
	 *
	 * <b>Parameter:</b><br>
	 * @param obj objek {@link GeneralValueObject} yang merupakan instance {@link StokAwal}
	 *            yang akan diedit atau disalin datanya
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika cast gagal atau terjadi kesalahan saat membangun form modal
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Implementasi antarmuka ini diperlukan agar infrastruktur generik {@code Common.copyEditDeleteButtons}
	 * dapat membuka form edit tanpa referensi langsung ke tipe controller yang spesifik.
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		stokAwal = (StokAwal) obj;
		init(stokAwal);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Metode privat yang membangun seluruh antarmuka form tambah/ubah data stok awal.
	 *
	 * <b>Tujuan:</b><br>
	 * Membersihkan konten jendela modal dan membangun ulang semua komponen form secara
	 * programatik dengan data dari entitas stok awal yang diberikan, mendukung mode tambah
	 * (entitas baru) dan mode ubah (entitas yang sudah ada di database).
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menyimpan referensi entitas dan mengatur judul jendela sesuai mode operasi.
	 * (2) Membersihkan semua komponen lama dari jendela menggunakan {@code Common.clear}.
	 * (3) Membangun tata letak dengan {@code MyBorderlayout}: Center berisi grid isian 2 kolom
	 *     (label 30%, input 70%) dan South berisi toolbar Batal/Simpan.
	 * (4) Baris form yang dibangun: banbox master aset (tipe habis pakai, readonly dengan
	 *     picker), banbox satuan kerja (readonly dengan picker), input jumlah desimal, input
	 *     tanggal, dan textarea keterangan.
	 * (5) Setiap komponen diisi dengan nilai saat ini dari entitas stok awal.
	 * (6) Tombol Batal menutup jendela tanpa menyimpan; tombol Simpan memanggil {@code onSave}
	 *     yang melakukan validasi dan penyimpanan, lalu menutup jendela dan merefresh grid.
	 *
	 * <b>Parameter:</b><br>
	 * @param stokAwal objek entitas {@link StokAwal} berisi data yang akan ditampilkan di form;
	 *                 jika {@code getId() == null} maka ini adalah penambahan data baru
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan saat membangun komponen ZKoss
	 *
	 * <b>Pemeliharaan:</b><br>
	 * {@code AmbilDataMasterAssetBanbox} dibuat dengan parameter {@code MasterAsset.TIPE_HABIS_PAKAI}
	 * untuk memfilter hanya barang habis pakai. Jika tipe aset yang diizinkan berubah,
	 * sesuaikan parameter konstruktor banbox ini.
	 */
	private void init(StokAwal stokAwal) throws Exception {
		this.stokAwal = stokAwal;
		addWindow.setTitle(stokAwal.getId() == null ? "Tambah Stok Awal" : "Ubah Stok Awal");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Barang Habis Pakai *"));
		row.appendChild(masterAsset = new AmbilDataMasterAssetBanbox(MasterAsset.TIPE_HABIS_PAKAI));
		masterAsset.setReadonly(true);
		masterAsset.setAttribute("masterAsset", stokAwal.getMasterAsset());
		masterAsset.setValue(stokAwal.getMasterAsset() == null ? "" : stokAwal.getMasterAsset().getNama());
		masterAsset.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja *"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
		satuanKerja.setReadonly(true);
		satuanKerja.setAttribute("satuanKerja", stokAwal.getSatuanKerja());
		satuanKerja.setValue(stokAwal.getSatuanKerja() == null ? "" : stokAwal.getSatuanKerja().getNama());
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah *"));
		row.appendChild(jumlah = new MyDoublebox(stokAwal.getJumlah()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal *"));
		row.appendChild(tanggal = new MyDatebox(stokAwal.getTanggal()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(stokAwal.getKeterangan()));
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
	 * Memvalidasi input form, menyimpan data stok awal ke database, dan menyinkronkan
	 * entri detail transaksi aset yang terkait secara asinkron.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengambil nilai dari komponen input form, memvalidasi kelengkapan data wajib, menyimpan
	 * atau memperbarui entitas {@link StokAwal} ke database melalui Hibernate, dan kemudian
	 * secara asinkron memperbarui entri {@link DetailTransaksiAsset} yang berasal dari stok
	 * awal ini agar data transaksi stok selalu konsisten.
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memeriksa apakah masterAsset sudah dipilih; jika belum menampilkan peringatan.
	 * (2) Memeriksa apakah satuanKerja sudah dipilih; jika belum menampilkan peringatan.
	 * (3) Mendapatkan session Hibernate dan memuat ulang entitas jika sudah ada di database.
	 * (4) Menetapkan semua nilai dari komponen form ke properti entitas stok awal.
	 * (5) Menyimpan entitas menggunakan {@code Common.refreshSaveOrUpdate}.
	 * (6) Menggunakan {@code Common.createDefaultTimer} untuk menjalankan sinkronisasi
	 *     asinkron: menghapus entri DetailTransaksiAsset lama untuk stok awal ini, lalu
	 *     membuat entri baru dengan data terkini dan kode transaksi {@code LibraryUtil.SALDO_AWAL}.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZKoss dari tombol Simpan yang memicu pemanggilan metode ini
	 *
	 * <b>Return:</b><br>
	 * @return {@code true} jika data berhasil disimpan dan sinkronisasi dijadwalkan;
	 *         {@code false} jika validasi gagal (form tetap terbuka)
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan akses database Hibernate
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Query SQL native DELETE menggunakan ID stok awal secara langsung dalam string SQL —
	 * ini aman karena ID bertipe Long (integer) sehingga tidak rentan SQL injection.
	 * Sinkronisasi via timer hanya memperbarui entri untuk stok awal yang baru disimpan,
	 * berbeda dengan sinkronisasi massal yang memperbarui semua entri.
	 */
	public boolean onSave(Event event) throws Exception {
		if (masterAsset.getAttribute("masterAsset") == null) {
			MyMessageboxConfig.show("Mohon maaf, Barang Habis Pakai belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Barang Habis Pakai; (2) Cari dan pilih barang dari daftar master aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Mohon maaf, Satuan Kerja belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih pada field Satuan Kerja; (2) Pilih satuan kerja yang sesuai dari daftar; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (stokAwal.getId() != null) {
			stokAwal = (StokAwal) session.load(StokAwal.class, stokAwal.getId());

		}

		stokAwal.setMasterAsset((MasterAsset) masterAsset.getAttribute("masterAsset"));
		stokAwal.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		stokAwal.setJumlah(jumlah.getValue());
		stokAwal.setKeterangan(keterangan.getValue());
		stokAwal.setTanggal(tanggal.getValue());

		Common.refreshSaveOrUpdate(session, stokAwal);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();

				session.createSQLQuery(
						"delete from asset.detail_transaksi_asset where stok_awal = " + stokAwal.getId() + ";")
						.executeUpdate();
				DetailTransaksiAsset detailTransaksiAsset = new DetailTransaksiAsset();
				detailTransaksiAsset.setStokAwal(stokAwal);
				detailTransaksiAsset.setQtyBonus(0.0);
				detailTransaksiAsset.setMasterAsset(stokAwal.getMasterAsset());
				detailTransaksiAsset.setKeterangan("Transaksi Saldo Awal");
				detailTransaksiAsset.setKodeTransaksi(LibraryUtil.SALDO_AWAL);
				detailTransaksiAsset.setQty(stokAwal.getJumlah());
				detailTransaksiAsset.setTanggal(stokAwal.getTanggal());

				session.save(detailTransaksiAsset);

			}
		});

		return true;
	}

	/**
	 * Implementasi antarmuka {@link DataCriteria} untuk membangun objek Hibernate Criteria
	 * yang digunakan sebagai dasar query pencarian data stok awal.
	 *
	 * <b>Tujuan:</b><br>
	 * Membangun dan mengembalikan objek {@link Criteria} Hibernate dengan join ke
	 * {@link MasterAsset} dan kondisi filter pencarian yang aktif saat ini.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat Criteria untuk kelas {@link StokAwal} dengan alias join ke {@code masterAsset}.
	 * Pengurutan hasil berdasarkan tanggal descending (terbaru di atas) jika parameter
	 * {@code order} bernilai true. Filter pencarian nama diterapkan sebagai OR antara
	 * pencarian nama masterAsset dan kode masterAsset menggunakan ILIKE case-insensitive
	 * dengan MatchMode.ANYWHERE. Berbeda dari controller lain yang hanya memfilter satu field,
	 * StokAwal memungkinkan pencarian berdasarkan nama atau kode barang.
	 *
	 * <b>Parameter:</b><br>
	 * @param order {@code true} untuk menambahkan pengurutan tanggal descending pada hasil;
	 *              {@code false} untuk query tanpa pengurutan (untuk menghitung total paging)
	 *
	 * <b>Return:</b><br>
	 * @return objek {@link Criteria} Hibernate yang siap dieksekusi dengan join dan filter diterapkan
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Alias "masterAsset" digunakan dalam Restrictions untuk mengakses properti entitas terkait.
	 * Jika ada perubahan nama relasi di entitas {@link StokAwal}, alias dan nama properti
	 * di Restrictions harus diperbarui secara bersamaan.
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(StokAwal.class).createAlias("masterAsset", "masterAsset");

		if (order)
			criteria.addOrder(Order.desc("tanggal"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(
						Restrictions.ilike("masterAsset.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("masterAsset.kode", searchnama.getValue().trim(), MatchMode.ANYWHERE)));
		return criteria;
	}

	/**
	 * Memuat dan menampilkan data hasil pencarian stok awal ke grid utama halaman.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengeksekusi query pencarian berdasarkan kriteria filter aktif, menerapkan batasan
	 * paging, dan memperbarui model data grid sehingga pengguna melihat daftar stok awal
	 * yang sesuai filter pada halaman yang sedang aktif.
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menghitung total data sesuai filter untuk memperbarui komponen paging.
	 * (2) Mengambil data dengan batasan jumlah baris per halaman dan offset sesuai halaman aktif.
	 * (3) Membungkus hasil dalam {@code SimpleListModel} dan menetapkan renderer baru ke grid.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZKoss; dapat {@code null} jika dipanggil secara programatik
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Dipanggil setelah operasi simpan, hapus, atau saat filter pencarian berubah.
	 * Juga dipanggil setelah operasi sinkronisasi massal selesai agar grid menampilkan
	 * data terkini yang sudah tersinkronisasi.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<StokAwal> stokAwal = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(stokAwal);
		grid.setRowRenderer(new StokAwalRenderer());
		grid.setModelCheckMobile(strset);

	}

}
