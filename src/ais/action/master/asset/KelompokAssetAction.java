package ais.action.master.asset;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.asset.util.AssetUtil;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.Konfigurasi;
import ais.database.dao.DaoFactory;
import ais.database.dao.asset.KelompokAssetDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.asset.KelompokAsset;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>KelompokAssetAction — Kontroler CRUD Kelompok Aset</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah kontroler ZK (ZKoss Composer) untuk halaman manajemen kelompok aset
 * (kelompok barang/jasa) di modul manajemen aset eCampus. Kelompok aset adalah data
 * referensi hierarkis yang mengklasifikasikan aset berdasarkan kategori, misalnya:
 * "Peralatan Laboratorium", "Furnitur Kantor", "Kendaraan Dinas", "Bangunan dan Infrastruktur",
 * dan sebagainya. Setiap kelompok aset dapat dikonfigurasi dengan:
 * (1) Induk kelompok untuk membentuk hierarki kategori.
 * (2) Estimasi umur pakai dalam satuan bulan, digunakan untuk kalkulasi penyusutan default.
 * (3) Flag apakah termasuk aset tetap (fix asset) yang mempengaruhi perlakuan akuntansi.
 * (4) Akun pembelian (akunTransaksi) dalam format JSON array — akun debit saat pembelian aset.
 * (5) Akun akumulasi penyusutan (akunPenyusutan) dalam format JSON array.
 * (6) Akun biaya penyusutan (akunBiayaPenyusutan) dalam format JSON array.
 * Kelompok akun dalam format JSON array memungkinkan konfigurasi multi-akun per kelompok
 * sesuai kebutuhan chart of accounts institusi.<br><br>
 *
 * <b>Cara kerja:</b><br>
 * Halaman ini menggunakan pola CRUD dengan GenericAutowireComposer ZKoss 5.5. Setiap baris
 * grid memiliki komponen {@code MyDetail} yang dapat dibuka untuk menampilkan sub-panel
 * daftar aset dalam kelompok tersebut (lazy-loaded via MyInclude ke master_asset.zul).
 * Form tambah/ubah dibangun programatik dengan widget khusus {@code AssetUtil.reloadFormula}
 * untuk menampilkan dan mengedit array akun dalam format JSONArray. Penyimpanan data
 * menggunakan {@code KelompokAssetDao} (DAO layer) bukan langsung ke Hibernate session,
 * berbeda dengan kebanyakan Action lain yang menggunakan {@code Common.refreshSaveOrUpdate}.
 * Validasi duplikasi nama dilakukan via query {@code checkNamaKelompokAsset()} sebelum simpan.<br><br>
 *
 * <b>Threading:</b><br>
 * Tidak ada pemrosesan latar belakang. Semua operasi dilakukan di thread utama ZK.
 * Sub-panel daftar aset dalam MyDetail dimuat secara lazy saat pengguna membuka detail,
 * menggunakan sesi ZK yang sama.<br><br>
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas ini mengimplementasikan tiga antarmuka: {@code DataCriteria}, {@code DataSearchDefault},
 * dan {@code DataInitDefault}. Akun disimpan sebagai JSON array string di kolom teks database,
 * bukan sebagai relasi FK, sehingga perubahan struktur akun tidak berdampak pada integritas
 * referensial tapi membutuhkan konsistensi manual. Filter pencarian mendukung nama dan akun.
 * Duplikasi nama dicegah via query count dengan eksklusi ID entitas saat ini (mode ubah).
 *
 * @author eCampus Dev Team
 * @version 1.0
 */
public class KelompokAssetAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Serial version UID untuk serialisasi kelas ZK Composer ini.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal untuk form tambah/ubah kelompok aset. */
	private MyWindow addWindow;

	/** Komponen paginasi untuk grid daftar kelompok aset. */
	private Paging paging;

	/** Grid utama yang menampilkan daftar kelompok aset. */
	private MyGrid grid;

	/** Kotak teks filter pencarian berdasarkan nama kelompok aset. */
	private Textbox searchnama;

	/** Banbox filter berdasarkan akun (mencari kelompok yang menggunakan akun tertentu). */
	private AmbilDataAkunBanbox searchakun;

	/** Kotak teks input nama kelompok aset pada form tambah/ubah. */
	private Textbox nama;

	/** Combobox untuk memilih kelompok induk (hierarki kelompok aset). */
	private Combobox induk;

	/** Kotak teks keterangan tambahan pada form tambah/ubah. */
	private Textbox keterangan;

	/** Penanda apakah pengguna memiliki hak ubah data. */
	private boolean edit = false;

	/** Penanda apakah pengguna memiliki hak hapus data. */
	private boolean delete = false;

	/** Entitas kelompok aset yang sedang dikelola (tambah atau ubah). */
	private KelompokAsset kelompokAsset;

	/** Tombol toolbar tambah data baru. */
	private MyToolbarbuttonConfig add;

	/**
	 * Array JSON akun pembelian aset (debit saat pembelian). Disimpan sebagai string JSON
	 * di kolom akunTransaksi entitas {@code KelompokAsset}.
	 */
	private JSONArray akunTransaksi;

	/**
	 * Array JSON akun akumulasi penyusutan. Disimpan sebagai string JSON
	 * di kolom akunPenyusutan entitas {@code KelompokAsset}.
	 */
	private JSONArray akunPenyusutan;

	/**
	 * Array JSON akun biaya penyusutan. Disimpan sebagai string JSON
	 * di kolom akunBiayaPenyusutan entitas {@code KelompokAsset}.
	 */
	private JSONArray akunBiayaPenyusutan;

	/** JSONArray akun Beban Pokok Penjualan (HPP) per satuan kerja — akun debit saat posting HPP kantin. */
	private JSONArray akunBebanPokokPenjualan;

	/** Input estimasi umur pakai dalam bulan untuk kalkulasi penyusutan default. */
	private MyDoublebox estimasiUmurPakai;

	/** Checkbox penanda apakah kelompok ini merupakan kategori aset tetap (fixed asset). */
	private MyCheckboxConfig merupakanAssetFix;
	/** Checkbox penanda "Merupakan Pekerjaan Dalam Pelaksanaan" (proses simpan sama dgn aset fix). */
	private MyCheckboxConfig merupakanPekerjaanDalamPelaksanaan;

	/**
	 * Pemeriksaan keamanan sebelum halaman dikompilasi oleh ZK.
	 *
	 * <b>Tujuan:</b><br>
	 * Memastikan pengguna yang mengakses halaman kelompok aset telah memiliki sesi
	 * yang valid dan hak akses sesuai sebelum proses komposisi halaman dimulai.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk validasi, lalu {@code super.doBeforeCompose}
	 * untuk melanjutkan proses ZK normal.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pola standar keamanan eCampus. Tidak dimodifikasi tanpa koordinasi tim keamanan.
	 *
	 * @param page     Halaman ZK yang sedang dimuat.
	 * @param parent   Komponen induk.
	 * @param compInfo Informasi metadata komponen ZUL.
	 * @return {@code ComponentInfo} untuk melanjutkan proses komposisi ZK.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Inisialisasi komponen dan logika bisnis setelah semua elemen ZUL terhubung.
	 *
	 * <b>Tujuan:</b><br>
	 * Titik masuk utama inisialisasi halaman kelompok aset. Menyiapkan hak akses,
	 * event listener filter akun, data awal grid, paginasi, dan tombol toolbar.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memanggil {@code super.doAfterCompose} dan menginisialisasi bahasa.
	 * (2) Memeriksa sesi dan hak akses READ; jika tidak valid, mengarahkan ke logoff.
	 * (3) Mengatur event listener pada banbox filter akun untuk memicu pencarian ulang.
	 * (4) Mengatur visibilitas tombol tambah berdasarkan hak akses CREATE.
	 * (5) Membaca hak akses UPDATE dan DELETE.
	 * (6) Memanggil {@code onSearchDefault} untuk memuat data awal.
	 * (7) Mengatur listener paginasi.
	 * (8) Mendaftarkan tombol cetak dan upload data ke toolbar.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika sesi tidak valid atau hak akses READ tidak ada, pengguna diarahkan ke logoff.
	 *
	 * @param comp Komponen akar halaman ZUL yang telah di-autowire.
	 * @throws Exception Jika terjadi kesalahan inisialisasi ZK atau Hibernate.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		searchakun.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
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

		String[] contents = new String[] { "id", "nama", "induk", "akunTransaksi", "akunPenyusutan",
				"akunBiayaPenyusutan", "estimasiUmurPakai", "keterangan", };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelompokAsset.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Renderer baris grid untuk daftar kelompok aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Merender setiap entri {@code KelompokAsset} ke dalam baris grid ZK dengan
	 * komponen detail expandable yang menampilkan daftar aset dalam kelompok tersebut.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Setiap baris memiliki komponen {@code MyDetail} di kolom pertama yang ketika dibuka
	 * akan melakukan lazy-loading sub-panel daftar aset (master_asset.zul) dengan menyimpan
	 * referensi kelompok aset ke atribut sesi ZK. Kolom lain menampilkan nama kelompok
	 * (via RevisiHelper), nama induk, estimasi umur pakai, keterangan, dan tombol aksi.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Penambahan kolom baru memerlukan sinkronisasi dengan header di ZUL.
	 * Sub-panel daftar aset menggunakan atribut sesi "KelompokAsset" sebagai filter.
	 */
	class KelompokAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data kelompok aset ke dalam baris grid ZK.
		 *
		 * <b>Tujuan:</b><br>
		 * Mengisi sel-sel baris grid dengan data entitas {@code KelompokAsset}, termasuk
		 * komponen detail expandable untuk menampilkan daftar aset dalam kelompok ini.<br><br>
		 *
		 * <b>Cara kerja:</b><br>
		 * (1) Membuat {@code MyDetail} di kolom pertama dengan event listener onOpen.
		 * (2) Saat detail dibuka, membersihkan konten lama, membuat Div container,
		 *     menyimpan kelompok ke atribut sesi, dan memuat master_asset.zul via MyInclude.
		 * (3) Kolom berikutnya menampilkan nama (dengan RevisiHelper), nama induk,
		 *     estimasi umur pakai (diformat dengan numberFormat), keterangan.
		 * (4) Tombol ubah/hapus via {@code Common.copyEditDeleteButtons}.<br><br>
		 *
		 * <b>Penanganan error:</b><br>
		 * Tidak ada penanganan error lokal. Exception diteruskan ke ZK framework.
		 *
		 * @param arg0 Baris grid ZK yang akan diisi.
		 * @param arg1 Objek {@code KelompokAsset} yang dirender.
		 * @throws Exception Jika terjadi kesalahan pembuatan komponen ZK.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final KelompokAsset kelompokAsset = (KelompokAsset) arg1;

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

						session.setAttribute("KelompokAsset", kelompokAsset);
						MyInclude iframe = new MyInclude("/pages/master/asset/master_asset.zul");
						iframe.setHeight("650px");
						iframe.setWidth("100%");
						iframe.setParent(groupbox);
					}
				}
			});

			RevisiHelper.createNewRevisi(KelompokAsset.class, kelompokAsset, kelompokAsset.getNama()).setParent(arg0);

			new Label(kelompokAsset.getInduk() == null ? "" : kelompokAsset.getInduk().getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(kelompokAsset.getEstimasiUmurPakai())).setParent(arg0);
			new Label(kelompokAsset.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, kelompokAsset, KelompokAssetAction.this).setParent(arg0);

		}

	}

	/**
	 * Menangani klik tombol "Tambah" untuk membuat kelompok aset baru.
	 *
	 * <b>Tujuan:</b><br>
	 * Membuka form tambah kelompok aset baru dengan instance kosong {@code KelompokAsset},
	 * lalu menampilkan jendela modal form.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code init(new KelompokAsset())} dengan objek kosong,
	 * kemudian menampilkan {@code addWindow} dalam mode modal.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Tidak ada validasi pre-tambah. Semua validasi dilakukan di {@code onSave}.
	 *
	 * @param event Event ZK dari klik tombol tambah.
	 * @throws Exception Jika terjadi kesalahan inisialisasi form atau jendela modal.
	 */
	public void onAdd(Event event) throws Exception {
		init(new KelompokAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Menginisialisasi dan membangun form tambah/ubah kelompok aset secara programatik.
	 *
	 * <b>Tujuan:</b><br>
	 * Membersihkan dan membangun ulang form lengkap kelompok aset yang mencakup nama,
	 * induk kelompok, estimasi umur pakai, flag aset tetap, tiga konfigurasi akun
	 * (pembelian, akumulasi penyusutan, biaya penyusutan), dan keterangan.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Mengatur judul jendela dan membersihkan konten lama.
	 * (2) Membangun Borderlayout dengan Center (grid form 2 kolom) dan South (toolbar).
	 * (3) Textbox nama kelompok (wajib).
	 * (4) Combobox induk kelompok (semi-opsional; bisa merupakan induk utama).
	 * (5) MyDoublebox estimasi umur pakai dalam bulan.
	 * (6) MyCheckboxConfig flag aset tetap.
	 * (7) Baris akun pembelian, akumulasi penyusutan, dan biaya penyusutan menggunakan
	 *     {@code AssetUtil.reloadFormula} untuk merender dan mengedit JSONArray akun.
	 * (8) Event listener checkbox aset tetap menyembunyikan/menampilkan baris akun
	 *     (hanya relevan untuk aset tetap).
	 * (9) Toolbar dengan tombol Batal dan Simpan.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * JSONArray akun diinisialisasi dari string JSON yang tersimpan di entitas. Jika string
	 * JSON tidak valid, konstruktor {@code new JSONArray(string)} akan melempar exception.
	 * Pastikan data akun di database selalu valid JSON.
	 *
	 * @param kelompokAsset Entitas yang akan ditampilkan/diedit di form.
	 * @throws Exception Jika terjadi kesalahan inisialisasi komponen ZK atau parsing JSON.
	 */
	private void init(KelompokAsset kelompokAsset) throws Exception {
		this.kelompokAsset = kelompokAsset;
		addWindow.setTitle(kelompokAsset.getId() == null ? "Tambah Kelompok Barang/Jasa" : "Ubah Kelompok Barang/Jasa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelompok Barang/Jasa"));
		row.appendChild(nama = new Textbox(kelompokAsset.getNama() == null ? "" : kelompokAsset.getNama()));
		nama.setWidth("90%");

		MyFormRow rowInduk = new MyFormRow();
		rowInduk.setParent(rows);
		rowInduk.appendChild(new ais.ui.util.MyLabelConfig("Kelompok ini meng-induk pada"));
		rowInduk.appendChild(induk = new Combobox());
		Common.insertComboDanSemua(induk, new String[] { "nomorUrut", "nama" }, "keterangan", KelompokAsset.class,
				"== Merupakan Induk Utama ==");
		Common.selectComboItem(true, induk, kelompokAsset.getInduk());
		induk.setWidth("90%");
		induk.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Estimasi Umur Pakai (bulan)"));
		row.appendChild(estimasiUmurPakai = new MyDoublebox(kelompokAsset.getEstimasiUmurPakai()));
		estimasiUmurPakai.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		org.zkoss.zul.Hbox hboxPenandaKelompok = new org.zkoss.zul.Hbox();
		hboxPenandaKelompok.setSpacing("20px");
		row.appendChild(hboxPenandaKelompok);
		hboxPenandaKelompok.appendChild(merupakanAssetFix = new MyCheckboxConfig("Merupakan Aset Fix"));
		merupakanAssetFix.setChecked(kelompokAsset.getMerupakanAssetFix());
		hboxPenandaKelompok.appendChild(
				merupakanPekerjaanDalamPelaksanaan = new MyCheckboxConfig("Merupakan Pekerjaan Dalam Pelaksanaan"));
		merupakanPekerjaanDalamPelaksanaan.setChecked(kelompokAsset.getMerupakanPekerjaanDalamPelaksanaan());

		// Konfigurasi tampil/sembunyi tiap akun pada form ini (default AKTIF = tampil).
		final boolean tampilAkunFix = Common.bolehKonfigurasi(Konfigurasi.KELOMPOK_ASET_AKUN_FIX_ASET,
				Konfigurasi.AKTIF);
		final boolean tampilAkumulasi = Common.bolehKonfigurasi(Konfigurasi.KELOMPOK_ASET_AKUN_AKUMULASI_PENYUSUTAN,
				Konfigurasi.AKTIF);
		final boolean tampilBiayaPenyusutan = Common.bolehKonfigurasi(Konfigurasi.KELOMPOK_ASET_AKUN_BIAYA_PENYUSUTAN,
				Konfigurasi.AKTIF);
		final boolean tampilBebanPokok = Common.bolehKonfigurasi(Konfigurasi.KELOMPOK_ASET_AKUN_BEBAN_POKOK_PENJUALAN,
				Konfigurasi.AKTIF);

		final MyFormRow row1 = new MyFormRow();
		row1.setParent(rows);
		row1.appendChild(new ais.ui.util.MyLabelConfig("Akun Pembelian"));
		akunTransaksi = new JSONArray(kelompokAsset.getAkunTransaksi());
		Row rowFormula = Common.tampilanScroll1(row1);
		AssetUtil.reloadFormula(rowFormula, akunTransaksi, edit);

		final MyFormRow row2 = new MyFormRow();
		row2.setParent(rows);
		row2.appendChild(new ais.ui.util.MyLabelConfig("Akun Akumulasi Penyusutan"));
		akunPenyusutan = new JSONArray(kelompokAsset.getAkunPenyusutan());
		rowFormula = Common.tampilanScroll1(row2);
		AssetUtil.reloadFormula(rowFormula, akunPenyusutan, edit);

		final MyFormRow rowAkunBiaya = new MyFormRow();
		rowAkunBiaya.setParent(rows);
		final ais.ui.util.MyLabelConfig labelAkunBiaya = new ais.ui.util.MyLabelConfig("Akun Biaya");
		rowAkunBiaya.appendChild(labelAkunBiaya);
		akunBiayaPenyusutan = new JSONArray(kelompokAsset.getAkunBiayaPenyusutan());
		rowFormula = Common.tampilanScroll1(rowAkunBiaya);
		AssetUtil.reloadFormula(rowFormula, akunBiayaPenyusutan, edit);
		rowAkunBiaya.setVisible(tampilBiayaPenyusutan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Beban Pokok Penjualan (HPP)"));
		akunBebanPokokPenjualan = new JSONArray(kelompokAsset.getAkunBebanPokokPenjualan());
		rowFormula = Common.tampilanScroll1(row);
		AssetUtil.reloadFormula(rowFormula, akunBebanPokokPenjualan, edit);
		row.setVisible(tampilBebanPokok);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(kelompokAsset.getKeterangan() == null ? "" : kelompokAsset.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

		final EventListener merupakanAssetFixEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean isFixAsset = merupakanAssetFix.isChecked();
				row1.setVisible(isFixAsset && tampilAkunFix);
				row2.setVisible(isFixAsset && tampilAkumulasi);
				// Biaya: "Akun Biaya" untuk non-aset, "Akun Biaya Penyusutan" untuk aset tetap
				labelAkunBiaya.setValue(isFixAsset ? "Akun Biaya Penyusutan" : "Akun Biaya");
			}
		};
		merupakanAssetFix.addEventListener("onClick", merupakanAssetFixEventListener);
		merupakanAssetFixEventListener.onEvent(null);
		// "Pekerjaan Dalam Pelaksanaan" (CIP) = pasti aset → paksa "Aset Fix" tercentang + dikunci (wajib),
		// lalu PICU listener aset fix (setChecked programatik tak memicu onClick).
		final EventListener merupakanPekerjaanEventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (merupakanPekerjaanDalamPelaksanaan.isChecked()) {
					merupakanAssetFix.setChecked(true);
					merupakanAssetFix.setDisabled(true);
				} else {
					merupakanAssetFix.setDisabled(false);
				}
				merupakanAssetFixEventListener.onEvent(arg0);
			}
		};
		merupakanPekerjaanDalamPelaksanaan.addEventListener("onClick", merupakanPekerjaanEventListener);
		merupakanPekerjaanEventListener.onEvent(null);

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
	 * Menyimpan data kelompok aset ke database setelah validasi.
	 *
	 * <b>Tujuan:</b><br>
	 * Memvalidasi input (nama wajib dan tidak duplikat), kemudian menyimpan atau memperbarui
	 * entitas {@code KelompokAsset} ke database melalui {@code KelompokAssetDao}.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Memvalidasi nama tidak kosong; jika kosong, return false.
	 * (2) Memanggil {@code checkNamaKelompokAsset()} untuk cek duplikasi; jika duplikat, return false.
	 * (3) Mengambil DAO kelompok aset dari DaoFactory.
	 * (4) Jika mode ubah, me-load ulang entitas via DAO untuk menghindari stale state.
	 * (5) Mengisi semua field entitas dari nilai komponen form (nama, keterangan, induk,
	 *     akunPenyusutan, akunBiayaPenyusutan, akunTransaksi, estimasiUmurPakai, merupakanAssetFix).
	 * (6) Memanggil {@code update} atau {@code save} via DAO tergantung mode.<br><br>
	 *
	 * <b>Return:</b><br>
	 * {@code true} jika berhasil disimpan; {@code false} jika validasi gagal.<br><br>
	 *
	 * <b>Penanganan error:</b><br>
	 * Duplikasi nama ditangani secara manual via query count. Exception DAO diteruskan ke ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Penggunaan DAO layer (bukan Common.refreshSaveOrUpdate) membuat kode ini berbeda
	 * dari pola Action lain. Pastikan DAO memanggil commit transaksi Hibernate yang sesuai.
	 *
	 * @param event Event ZK dari klik tombol Simpan.
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal.
	 * @throws Exception Jika terjadi kesalahan DAO saat menyimpan data.
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Kelompok Aset belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama Kelompok dengan nama yang jelas dan deskriptif; (2) Kelompok aset digunakan untuk pengelompokan dan perhitungan penyusutan; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaKelompokAsset();
		if (i) {
			MyMessageboxConfig.show("Nama Kelompok Aset sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		KelompokAssetDao kelompokAssetDao = DaoFactory.getInstance().getKelompokAssetDao();
		if (kelompokAsset.getId() != null) {
			kelompokAsset = kelompokAssetDao.load(kelompokAsset.getId());

		}

		kelompokAsset.setNama(nama.getValue());
		kelompokAsset.setKeterangan(keterangan.getValue());
		kelompokAsset.setInduk(
				(KelompokAsset) (induk.getSelectedItem() == null ? null : induk.getSelectedItem().getValue()));

		kelompokAsset.setAkunPenyusutan(akunPenyusutan.toString());
		kelompokAsset.setAkunBiayaPenyusutan(akunBiayaPenyusutan.toString());
		kelompokAsset.setAkunTransaksi(akunTransaksi.toString());
		kelompokAsset.setAkunBebanPokokPenjualan(akunBebanPokokPenjualan.toString());
		kelompokAsset.setEstimasiUmurPakai(estimasiUmurPakai.getValue());
		kelompokAsset.setMerupakanAssetFix(merupakanAssetFix.isChecked());
		kelompokAsset.setMerupakanPekerjaanDalamPelaksanaan(merupakanPekerjaanDalamPelaksanaan.isChecked());

		if (kelompokAsset.getId() != null) {
			kelompokAssetDao.update(kelompokAsset);
		} else {
			kelompokAssetDao.save(kelompokAsset);
		}

		return true;
	}

	/**
	 * Membangun Criteria Hibernate untuk kueri daftar kelompok aset.
	 *
	 * <b>Tujuan:</b><br>
	 * Menyusun filter Hibernate yang mendukung pencarian berdasarkan nama dan akun.
	 * Digunakan oleh {@code onSearchDefault} dan {@code Common.initPaging}.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Jika filter akun diisi, menambahkan kondisi OR yang mencari akun di tiga kolom JSON
	 * (akunPenyusutan, akunBiayaPenyusutan, akunTransaksi) menggunakan ILIKE dengan
	 * pola ":{id}}". Pola ini mencocokkan format JSON array di mana ID akun disimpan
	 * sebagai nilai properti. Filter nama menggunakan ILIKE partial match standar.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pencarian akun di kolom JSON menggunakan pola string matching karena akun disimpan
	 * sebagai JSON bukan FK. Jika format JSON berubah, pola pencarian perlu diperbarui.
	 *
	 * @param order {@code true} untuk menambahkan ORDER BY nama ASC.
	 * @return Objek {@code Criteria} Hibernate siap dieksekusi.
	 */
	public Criteria initCriteria(boolean order) {

		Akun akun = (Akun) searchakun.getAttribute("akun");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelompokAsset.class);

		if (akun != null && akun.getId() != null) {
			criteria.add(Restrictions.or(
					Restrictions.ilike("akunPenyusutan", ":" + akun.getId() + "}", MatchMode.ANYWHERE),
					Restrictions.or(
							Restrictions.ilike("akunBiayaPenyusutan", ":" + akun.getId() + "}", MatchMode.ANYWHERE),
							Restrictions.ilike("akunTransaksi", ":" + akun.getId() + "}", MatchMode.ANYWHERE))

			));
		}

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * Memuat ulang data grid kelompok aset berdasarkan filter saat ini.
	 *
	 * <b>Tujuan:</b><br>
	 * Implementasi {@code DataSearchDefault}. Me-refresh tampilan grid daftar kelompok aset
	 * setelah perubahan filter atau operasi CRUD.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * (1) Menghitung total data dan memperbarui paginasi.
	 * (2) Mengeksekusi kueri dengan criteria terurut, dibatasi per halaman.
	 * (3) Menetapkan renderer dan model ke grid ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Dipanggil dari inisialisasi, paginasi, dan setelah operasi CRUD.
	 *
	 * @param event Event ZK yang memicu pencarian ulang, bisa {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelompokAsset> kelompokAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelompokAsset);
		grid.setRowRenderer(new KelompokAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Memeriksa apakah nama kelompok aset yang akan disimpan sudah ada di database.
	 *
	 * <b>Tujuan:</b><br>
	 * Mencegah duplikasi nama kelompok aset di database dengan melakukan query count
	 * sebelum penyimpanan. Pada mode ubah, mengecualikan entitas yang sedang diedit
	 * dari pengecekan agar nama yang sama dapat dipertahankan.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Menggunakan Hibernate Criteria dengan Projections.rowCount() untuk menghitung
	 * berapa baris yang memiliki nama yang sama persis (case-sensitive via Restrictions.eq).
	 * Jika entitas sedang diedit (ID tidak null), menambahkan kondisi ne("id", currentId)
	 * untuk mengecualikan entitas saat ini dari hitungan.<br><br>
	 *
	 * <b>Return:</b><br>
	 * {@code true} jika nama sudah digunakan oleh kelompok lain (duplikat); {@code false}
	 * jika nama aman untuk digunakan.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pengecekan ini case-sensitive karena menggunakan {@code Restrictions.eq}.
	 * Untuk pengecekan case-insensitive, ganti dengan {@code Restrictions.ilike}.
	 *
	 * @return {@code true} jika nama sudah terduplikat di database; {@code false} jika belum.
	 */
	public Boolean checkNamaKelompokAsset() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KelompokAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.kelompokAsset.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kelompokAsset.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	/**
	 * Implementasi antarmuka {@code DataInitDefault} untuk inisialisasi form dari objek generik.
	 *
	 * <b>Tujuan:</b><br>
	 * Dipanggil oleh mekanisme generik ubah data (tombol ubah di renderer) dengan
	 * {@code GeneralValueObject} yang merupakan entitas {@code KelompokAsset}.
	 * Mendelegasikan ke {@code init(KelompokAsset)} setelah melakukan cast tipe.<br><br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Meng-cast {@code obj} ke {@code KelompokAsset}, memanggil {@code init} untuk
	 * membangun form, lalu menampilkan jendela modal dalam mode ubah.<br><br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Kontrak antarmuka {@code DataInitDefault}. Dipanggil oleh {@code Common.copyEditDeleteButtons}.
	 * Jangan mengubah tanda tangan metode ini.
	 *
	 * @param obj Entitas {@code KelompokAsset} sebagai {@code GeneralValueObject}.
	 * @throws Exception Jika cast gagal atau inisialisasi form mengalami kesalahan.
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kelompokAsset = (KelompokAsset) obj;
		init(kelompokAsset);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
