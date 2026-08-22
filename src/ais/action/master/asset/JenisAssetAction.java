package ais.action.master.asset;

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
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.PesanFormalHelper;
import ais.database.dao.DaoFactory;
import ais.database.dao.asset.JenisAssetDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.JenisAsset;
import ais.database.model.asset.MasterAsset;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>JenisAssetAction — Kontroler CRUD Data Master Jenis Barang/Jasa (Aset)</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah kontroler ZKoss (composer) yang mengelola operasi CRUD lengkap untuk entitas
 * {@link JenisAsset}. Data master jenis aset berfungsi sebagai kategori atau kelompok untuk
 * semua barang dan jasa yang dikelola dalam sistem manajemen aset. Setiap entitas
 * {@link JenisAsset} memiliki nama unik, tipe aset (menentukan perilaku dalam sistem), dan
 * keterangan. Tipe aset dapat berupa: Tidak Habis Pakai (aset tetap yang dicatat secara
 * individual), Habis Pakai (barang yang dikelola sebagai stok), Jasa (layanan), atau Tidak Habis
 * Pakai Non Aset (barang inventori non-aset). Pengelompokan ini sangat penting karena menentukan
 * alur kerja pemesanan, penerimaan, dan pelaporan yang berbeda untuk setiap tipe aset.
 *
 * <b>Cara kerja:</b><br>
 * Controller mengimplementasikan dua antarmuka: {@code DataCriteria} untuk membangun query
 * Hibernate dan {@code DataSearchDefault} untuk rendering grid. Fitur khusus kelas ini adalah
 * kemampuan drill-down: setiap baris jenis aset di grid memiliki komponen {@link MyDetail}
 * yang dapat dikembangkan (expand) untuk menampilkan daftar aset yang termasuk dalam jenis
 * tersebut melalui embed halaman {@code master_asset.zul}. Informasi jenis aset yang dipilih
 * diteruskan ke halaman yang di-include melalui atribut sesi. Validasi keunikan nama dilakukan
 * sebelum penyimpanan menggunakan query Hibernate dengan proyeksi rowCount. Penyimpanan menggunakan
 * {@link JenisAssetDao} (pola DAO) bukan langsung via {@code Common.refreshSaveOrUpdate}.
 *
 * <b>Threading:</b><br>
 * Seluruh operasi berjalan pada thread event ZKoss tunggal. Session HTTP ZKoss digunakan untuk
 * meneruskan data jenis aset ke halaman yang di-include ({@code session.setAttribute("JenisAsset",
 * jenisAsset)}). Akses Hibernate melalui {@code HibernateUtil.currentSession()} aman dalam
 * konteks single-threaded event ZKoss. Tidak ada operasi asinkron atau multi-threading.
 *
 * <b>Pemeliharaan:</b><br>
 * Halaman drill-down {@code master_asset.zul} yang di-include pada komponen MyDetail membaca
 * atribut sesi "JenisAsset" untuk memfilter aset yang ditampilkan. Jika nama atribut sesi ini
 * berubah, perlu diperbarui di sini dan di halaman yang di-include. Konstanta tipe aset
 * ({@code MasterAsset.TIPE_HABIS_PAKAI} dll.) didefinisikan di kelas {@link MasterAsset} dan
 * digunakan sebagai nilai combobox tipe — jangan hardcode string tipe di kelas ini. Validasi
 * keunikan nama hanya di level aplikasi; pertimbangkan menambahkan UNIQUE constraint di database
 * untuk keamanan terhadap race condition pada skenario konkuren.
 *
 * @author Tim Pengembang AIS
 * @version 1.0
 * @see JenisAsset
 * @see JenisAssetDao
 * @see MasterAsset
 */
public class JenisAssetAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * Nomor versi serial untuk serialisasi kelas ini.
	 * Tidak perlu diubah kecuali ada perubahan struktur yang tidak kompatibel mundur.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZKoss yang digunakan untuk menampilkan form tambah atau ubah data jenis aset. */
	private MyWindow addWindow;

	/** Komponen paging ZKoss untuk navigasi halaman pada grid daftar jenis aset. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar seluruh data jenis aset yang tersedia. */
	private MyGrid grid;

	/** Kotak teks pencarian berdasarkan nama jenis aset pada area filter di atas grid. */
	private Textbox searchnama;

	/** Kotak teks input nama jenis aset pada form tambah/ubah; harus unik di seluruh database. */
	private Textbox nama;

	/** Kotak teks input keterangan atau deskripsi jenis aset pada form tambah/ubah data. */
	private Textbox keterangan;

	/** Penanda apakah pengguna saat ini memiliki hak akses untuk mengubah data (UPDATE privilege). */
	private boolean edit = false;

	/** Penanda apakah pengguna saat ini memiliki hak akses untuk menghapus data (DELETE privilege). */
	private boolean delete = false;

	/** Referensi ke objek entitas jenis aset yang sedang diproses (tambah atau ubah). */
	private JenisAsset jenisAsset;

	/** Tombol "Tambah" pada toolbar halaman, visibilitasnya dikendalikan oleh hak akses CREATE. */
	private MyToolbarbuttonConfig add;

	/**
	 * Combobox untuk memilih tipe aset dari pilihan yang tersedia: Tidak Habis Pakai,
	 * Habis Pakai, Jasa, Tidak Habis Pakai Non Aset, atau Tidak Ditentukan. Nilai yang
	 * dipilih menentukan perilaku aset dalam alur kerja pengadaan dan pelaporan.
	 */
	private Combobox tipe;

	/**
	 * Metode siklus hidup ZKoss yang dipanggil sebelum komponen halaman dibangun.
	 *
	 * <b>Tujuan:</b><br>
	 * Melakukan pemeriksaan keamanan akses pengguna sebelum halaman ZUL dirender sepenuhnya,
	 * mencegah akses tidak sah ke halaman manajemen jenis aset.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi otentikasi dan otorisasi
	 * pengguna. Mendelegasikan ke implementasi induk melalui {@code super.doBeforeCompose()}
	 * untuk menyelesaikan proses inisialisasi ZKoss. Dipanggil secara otomatis oleh framework.
	 *
	 * <b>Parameter:</b><br>
	 * @param page halaman ZKoss saat ini yang sedang dikompilasi oleh framework
	 * @param parent komponen induk dalam hierarki komponen ZUL
	 * @param compInfo informasi metadata komponen yang sedang diproses oleh ZKoss
	 *
	 * <b>Return:</b><br>
	 * @return objek {@code ComponentInfo} dari kelas induk untuk kelanjutan proses composing
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika pemeriksaan keamanan gagal, pengguna akan diarahkan ke halaman login oleh
	 * {@code Common.doCheckSecurity()}.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jangan hapus pemanggilan {@code super.doBeforeCompose()} karena dibutuhkan untuk
	 * inisialisasi mekanisme autowiring komponen ZKoss.
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
	 * Menginisialisasi seluruh logika bisnis halaman manajemen jenis aset, termasuk verifikasi
	 * sesi pengguna, pengaturan hak akses, pemuatan data awal, dan konfigurasi tombol toolbar.
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menyelesaikan autowiring ZKoss dan mengatur bahasa antarmuka.
	 * (2) Memeriksa atribut sesi {@code usersTemp} dan hak akses READ; jika tidak ada atau
	 *     tidak berwenang, menghapus sesi dan memanggil {@code Common.goLogoff()}.
	 * (3) Mengatur visibilitas tombol Tambah berdasarkan hak akses CREATE.
	 * (4) Menetapkan flag {@code edit} dan {@code delete} berdasarkan hak akses pengguna.
	 * (5) Memuat data jenis aset ke grid dengan {@code onSearchDefault(null)}.
	 * (6) Memasang event listener paging untuk navigasi halaman.
	 * (7) Menambahkan tombol cetak data (ekspor) dengan kolom: id, nama, tipe, keterangan.
	 * (8) Menambahkan tombol unggah data (impor) untuk pengguna dengan semua hak akses.
	 *
	 * <b>Parameter:</b><br>
	 * @param comp komponen akar halaman ZUL yang sudah selesai dibangun oleh framework ZKoss
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan selama inisialisasi komponen atau koneksi database
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Metode ini menggunakan {@code Common.cetakData(this, contents)} tanpa parameter kelas
	 * entitas (berbeda dari controller lain). Ini berarti ekspor menggunakan data dari grid
	 * saat ini, bukan mengakses ulang database. Pastikan array {@code contents} sesuai dengan
	 * field yang ada di entitas {@link JenisAsset} dan dapat diakses via getter refleksi.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

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

		String[] contents = new String[] { "id", "nama", "tipe", "keterangan", };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisAsset.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Inner class renderer yang bertanggung jawab merender setiap baris data pada grid
	 * daftar jenis aset beserta fitur drill-down untuk melihat aset di dalamnya.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengkonversi objek {@link JenisAsset} menjadi baris tampilan interaktif di grid,
	 * dengan kemampuan expand/collapse untuk menampilkan daftar aset yang terkategori
	 * dalam jenis aset tersebut melalui halaman yang di-include secara dinamis.
	 *
	 * <b>Cara kerja:</b><br>
	 * Untuk setiap baris, dibuat komponen {@link MyDetail} yang dapat dikembangkan. Saat
	 * pengguna mengklik expand pada MyDetail, event {@code onOpen} memuat halaman
	 * {@code master_asset.zul} di dalam div dengan tinggi 650px. Sebelum memuat halaman,
	 * entitas jenis aset yang bersangkutan disimpan ke atribut sesi HTTP agar halaman
	 * yang di-include dapat mengakses dan memfilter aset berdasarkan jenis ini. Label nama
	 * (via RevisiHelper) dan tipe (atau "Tidak Ditentukan" jika null) ditampilkan di sel
	 * berikutnya. Tombol Ubah dan Hapus dibuat secara manual karena memerlukan penanganan
	 * error yang spesifik, tidak menggunakan {@code Common.copyEditDeleteButtons}.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Kunci atribut sesi "JenisAsset" harus konsisten dengan yang dibaca oleh
	 * {@code master_asset.zul}. Jika kunci ini berubah, keduanya harus diperbarui bersamaan.
	 * Tinggi iframe 650px dapat disesuaikan jika daftar aset per jenis sangat panjang.
	 */
	class JenisAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data jenis aset ke dalam komponen ZKoss Row dengan fitur drill-down.
		 *
		 * <b>Tujuan:</b><br>
		 * Mengisi komponen {@link Row} ZKoss dengan data dari satu entitas {@link JenisAsset},
		 * termasuk komponen {@link MyDetail} expandable untuk menampilkan daftar aset terkait
		 * dan tombol aksi untuk operasi ubah dan hapus.
		 *
		 * <b>Cara kerja:</b><br>
		 * (1) Membuat {@link MyDetail} dan mendaftarkan listener {@code onOpen}: saat dibuka,
		 *     membersihkan konten MyDetail, membuat div container dengan caption "Daftar Aset",
		 *     menyimpan jenis aset ke sesi, dan memuat {@code master_asset.zul} via {@link MyInclude}.
		 * (2) Menambahkan nama jenis aset via RevisiHelper (mendukung riwayat revisi).
		 * (3) Menambahkan label tipe aset atau teks "Tidak Ditentukan" jika tipe null.
		 * (4) Menambahkan label keterangan jenis aset.
		 * (5) Membuat {@link Hbox} berisi tombol Ubah dan Hapus secara manual. Tombol ubah
		 *     membuka form edit modal. Tombol hapus menampilkan konfirmasi dan menangkap
		 *     exception relasi FK dengan pesan error informatif.
		 *
		 * <b>Parameter:</b><br>
		 * @param arg0 objek {@link Row} ZKoss yang akan diisi dengan komponen-komponen data
		 * @param arg1 objek data {@link JenisAsset} yang akan dirender sebagai satu baris
		 *
		 * <b>Penanganan error:</b><br>
		 * @throws Exception jika terjadi kesalahan saat membangun atau memasang komponen ZKoss
		 *
		 * <b>Pemeliharaan:</b><br>
		 * Path halaman yang di-include ({@code /pages/master/asset/master_asset.zul}) adalah path
		 * relatif ke context root aplikasi web. Perubahan struktur direktori pages harus
		 * diikuti dengan pembaruan path ini.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisAsset jenisAsset = (JenisAsset) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						groupbox.setParent(detail);
						groupbox.appendChild(new MyCaptionStyled("Daftar Aset"));

						session.setAttribute("JenisAsset", jenisAsset);
						MyInclude iframe = new MyInclude("/pages/master/asset/master_asset.zul");
						iframe.setHeight("650px");
						iframe.setWidth("100%");
						iframe.setParent(groupbox);
					}
				}
			});

			RevisiHelper.createNewRevisi(JenisAsset.class, jenisAsset, jenisAsset.getNama()).setParent(arg0);
			new Label(jenisAsset.getTipe() == null ? "Tidak Ditentukan" : jenisAsset.getTipe()).setParent(arg0);

			new Label(jenisAsset.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(jenisAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(jenisAsset);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"penghapusan data Jenis Asset " + jenisAsset.getNama(),
													"Data Jenis Asset ini kemungkinan besar masih digunakan/direferensikan oleh "
															+ "data Asset atau transaksi lain di sistem, sehingga database menolak "
															+ "penghapusan demi menjaga integritas data.",
													e,
													new String[] {
															"Periksa apakah masih ada data Asset yang menggunakan Jenis Asset ini, lalu ubah/hapus data tersebut terlebih dahulu.",
															"Nonaktifkan saja data ini (bukan menghapus) apabila masih dirujuk oleh data lain." });
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	/**
	 * Event handler yang dipanggil ketika pengguna menekan tombol "Tambah" pada toolbar halaman.
	 *
	 * <b>Tujuan:</b><br>
	 * Membuka form tambah data jenis aset baru dalam jendela modal untuk memungkinkan
	 * pengguna mendefinisikan kategori aset baru dalam sistem.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat objek {@link JenisAsset} baru kosong (tanpa ID) dan meneruskannya ke metode
	 * privat {@code init(JenisAsset)} yang membangun komponen form. Kemudian menampilkan
	 * jendela {@code addWindow} dalam mode modal.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZKoss dari klik tombol Tambah pada toolbar
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan saat membangun komponen form atau modal
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Dipanggil otomatis oleh ZKoss berdasarkan konvensi penamaan event handler. Tidak ada
	 * logika tambahan yang diperlukan; inisialisasi form ditangani sepenuhnya di {@code init}.
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Metode privat yang membangun seluruh antarmuka form tambah/ubah data jenis aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Membersihkan konten jendela modal dan membangun ulang semua komponen form secara
	 * programatik dengan data dari entitas jenis aset yang diberikan, mendukung mode
	 * tambah (entitas baru) dan mode ubah (entitas yang sudah ada di database).
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menyimpan referensi entitas dan mengatur judul jendela sesuai mode operasi.
	 * (2) Membangun {@code MyBorderlayout} dengan Center (grid isian) dan South (toolbar).
	 * (3) Grid 2 kolom (label 30%, input 70%) berisi baris-baris form:
	 *     - Nama jenis aset (textbox, pre-filled dengan nilai saat ini atau string kosong jika null)
	 *     - Tipe aset (combobox readonly dengan 5 pilihan: 4 tipe + "Tidak Ditentukan")
	 *     - Keterangan (textarea 3 baris)
	 * (4) Combobox tipe diisi dengan konstanta dari kelas {@link MasterAsset} dan pilihan
	 *     "Tidak Ditentukan" (nilai null) sebagai opsi default. Pilihan saat ini di-select
	 *     menggunakan {@code Common.selectComboItem(tipe, jenisAsset.getTipe())}.
	 * (5) Tombol Batal menutup jendela; tombol Simpan memanggil {@code onSave} dan menutup
	 *     jendela serta merefresh grid jika berhasil.
	 *
	 * <b>Parameter:</b><br>
	 * @param jenisAsset objek entitas {@link JenisAsset} berisi data yang akan ditampilkan
	 *                   di form; jika {@code getId() == null} maka ini adalah penambahan baru
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan saat membangun komponen ZKoss
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika ada tipe aset baru yang perlu ditambahkan ke sistem, tambahkan {@code MyComboitemConfig}
	 * baru di blok pengisian combobox tipe, dan pastikan konstanta tipe yang sesuai sudah
	 * didefinisikan di kelas {@link MasterAsset}. Gunakan selalu konstanta dari MasterAsset,
	 * jangan hardcode string tipe aset langsung.
	 */
	private void init(JenisAsset jenisAsset) throws Exception {
		this.jenisAsset = jenisAsset;
		addWindow.setTitle(jenisAsset.getId() == null ? "Tambah Jenis Barang/Jasa" : "Ubah Jenis Barang/Jasa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Barang/Jasa"));
		row.appendChild(nama = new Textbox(jenisAsset.getNama() == null ? "" : jenisAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe *"));
		row.appendChild(tipe = new Combobox());
		tipe.setWidth("90%");
		tipe.setReadonly(true);

		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel(MasterAsset.TIPE_TIDAK_HABIS_PAKAI);
		comboitem.setValue(MasterAsset.TIPE_TIDAK_HABIS_PAKAI);
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(MasterAsset.TIPE_HABIS_PAKAI);
		comboitem.setValue(MasterAsset.TIPE_HABIS_PAKAI);
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(MasterAsset.TIPE_JASA);
		comboitem.setValue(MasterAsset.TIPE_JASA);
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(MasterAsset.TIPE_TIDAK_HABIS_PAKAI_NON_ASET);
		comboitem.setValue(MasterAsset.TIPE_TIDAK_HABIS_PAKAI_NON_ASET);
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak Ditentukan");
		comboitem.setValue(null);
		tipe.appendChild(comboitem);

		Common.selectComboItem(tipe, jenisAsset.getTipe());
		tipe.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisAsset.getKeterangan() == null ? "" : jenisAsset.getKeterangan()));
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
	 * Memvalidasi input form dan menyimpan data jenis aset ke database melalui DAO.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengambil nilai dari komponen form, memvalidasi kelengkapan dan keunikan nama jenis aset,
	 * lalu menyimpan atau memperbarui entitas {@link JenisAsset} ke database menggunakan
	 * {@link JenisAssetDao}. Dipanggil oleh event listener tombol Simpan di form modal.
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memeriksa nama tidak kosong; jika kosong menampilkan peringatan dan return false.
	 * (2) Memanggil {@code checkNamaJenisAsset()} untuk memastikan nama belum digunakan;
	 *     jika duplikat menampilkan peringatan dan return false.
	 * (3) Mendapatkan DAO instance dari {@code DaoFactory}. Jika entitas sudah ada (ID tidak
	 *     null), memuat ulang dari DAO untuk memastikan objek dalam state managed Hibernate.
	 * (4) Menetapkan nama dari input, tipe dari item yang dipilih di combobox (null jika
	 *     "Tidak Ditentukan"), dan keterangan dari textarea.
	 * (5) Memanggil {@code jenisAssetDao.update()} atau {@code jenisAssetDao.save()} sesuai kondisi.
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZKoss dari tombol Simpan yang memicu pemanggilan metode ini
	 *
	 * <b>Return:</b><br>
	 * @return {@code true} jika data berhasil disimpan; {@code false} jika validasi gagal
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika terjadi kesalahan akses database melalui DAO
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Ekspresi {@code tipe.getSelectedItem().getValue()} dapat menghasilkan null untuk pilihan
	 * "Tidak Ditentukan" — ini disengaja dan disimpan sebagai null di database. Pastikan
	 * logika tampilan di renderer menangani nilai null dengan menampilkan "Tidak Ditentukan".
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Jenis Aset belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama Jenis Aset dengan nama yang jelas; (2) Jenis aset menentukan klasifikasi dan perlakuan akuntansi aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaJenisAsset();
		if (i) {
			MyMessageboxConfig.show("Nama Jenis Asset sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		JenisAssetDao jenisAssetDao = DaoFactory.getInstance().getJenisAssetDao();
		if (jenisAsset.getId() != null) {
			jenisAsset = jenisAssetDao.load(jenisAsset.getId());

		}

		jenisAsset.setNama(nama.getValue());
		jenisAsset.setTipe((String) (tipe.getSelectedItem() == null ? null : tipe.getSelectedItem().getValue()));
		jenisAsset.setKeterangan(keterangan.getValue());

		if (jenisAsset.getId() != null) {
			jenisAssetDao.update(jenisAsset);
		} else {
			jenisAssetDao.save(jenisAsset);
		}

		return true;
	}

	/**
	 * Implementasi antarmuka {@link DataCriteria} untuk membangun objek Hibernate Criteria
	 * yang digunakan sebagai dasar query pencarian data jenis aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Membangun dan mengembalikan objek {@link Criteria} Hibernate dengan kondisi filter
	 * pencarian nama yang aktif saat ini untuk mengambil data jenis aset dari database.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat Criteria untuk kelas {@link JenisAsset} tanpa filter aktif/nonaktif (karena
	 * entitas JenisAsset tidak memiliki field aktif). Jika parameter {@code order} true,
	 * menambahkan pengurutan nama ascending. Filter nama diterapkan sebagai ILIKE
	 * case-insensitive dengan ANYWHERE matching jika kotak pencarian tidak kosong.
	 *
	 * <b>Parameter:</b><br>
	 * @param order {@code true} untuk menambahkan pengurutan nama ascending pada hasil;
	 *              {@code false} untuk query tanpa pengurutan (untuk menghitung total paging)
	 *
	 * <b>Return:</b><br>
	 * @return objek {@link Criteria} Hibernate yang siap dieksekusi dengan filter diterapkan
	 *
	 * <b>Pemeliharaan:</b><br>
	 * JenisAsset tidak memiliki filter aktif/nonaktif karena tidak ada field aktif di entitas.
	 * Jika di masa mendatang field aktif ditambahkan, tambahkan filter yang sesuai di sini
	 * dan tambahkan komponen filter di ZUL beserta field {@code searchaktif} di kelas ini.
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisAsset.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * Memuat dan menampilkan data hasil pencarian jenis aset ke grid utama halaman.
	 *
	 * <b>Tujuan:</b><br>
	 * Mengeksekusi query pencarian berdasarkan kriteria filter aktif, menerapkan batasan
	 * paging, dan memperbarui model data grid sehingga pengguna melihat daftar jenis aset
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
	 * Dipanggil setelah operasi simpan, hapus, atau saat filter pencarian berubah. Juga
	 * dipanggil saat inisialisasi halaman. Setiap kali dipanggil, renderer baru dibuat —
	 * ini adalah pola standar ZKoss untuk memastikan state renderer bersih.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisAsset> jenisAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisAsset);
		grid.setRowRenderer(new JenisAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Memeriksa apakah nama jenis aset yang diinput pada form sudah digunakan oleh
	 * jenis aset lain di database untuk mencegah duplikasi nama.
	 *
	 * <b>Tujuan:</b><br>
	 * Memastikan integritas data dengan menolak penyimpanan jika nama yang sama sudah ada
	 * di database (kecuali jika sedang mengedit entitas yang sama). Nama jenis aset harus
	 * unik karena digunakan sebagai identifikasi kategori di berbagai modul lainnya.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menjalankan query Hibernate dengan proyeksi {@code rowCount()} pada tabel JenisAsset
	 * dengan kondisi: nama sama persis (case-sensitive via {@code Restrictions.eq}) dengan
	 * nilai dari field input, DAN ID entitas berbeda dari ID yang sedang diedit. Jika
	 * {@code jenisAsset.getId()} null (mode tambah baru), kondisi exclusi ID diabaikan
	 * dengan menggunakan sqlRestriction "1=1" yang selalu benar.
	 *
	 * <b>Return:</b><br>
	 * @return {@code true} jika nama sudah digunakan oleh jenis aset lain (duplikat ada);
	 *         {@code false} jika nama belum digunakan atau hanya digunakan oleh entitas
	 *         yang sedang diedit saat ini
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Validasi ini bersifat case-sensitive. Pertimbangkan menambahkan UNIQUE constraint di
	 * level database pada kolom nama di tabel jenis_asset sebagai lapisan keamanan tambahan
	 * terhadap race condition pada skenario akses konkuren dari banyak pengguna.
	 */
	public Boolean checkNamaJenisAsset() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.jenisAsset.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisAsset.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
