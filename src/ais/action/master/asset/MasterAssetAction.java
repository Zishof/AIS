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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
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

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.asset.util.AssetUtil;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.akunting.Akun;
import ais.database.model.asset.JenisAsset;
import ais.database.model.asset.KategoriAsset;
import ais.database.model.asset.KelompokAsset;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.SatuanMasterAsset;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h3>MasterAssetAction — Halaman Manajemen Master Barang/Jasa (Aset)</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah ZKoss Composer yang mengelola halaman CRUD (Create, Read,
 * Update, Delete) untuk data master barang dan jasa yang digunakan dalam modul
 * aset institusi. Master aset merupakan referensi utama yang mendefinisikan
 * spesifikasi standar setiap jenis barang atau jasa yang dapat dipesan, diterima,
 * dan dikelola dalam sistem manajemen aset. Setiap entri master aset berisi
 * informasi seperti kode, nama, merk, tipe (habis pakai/tidak habis pakai/jasa/
 * non-aset), jenis aset, kelompok aset, kategori, satuan, spesifikasi teknis
 * (dimensi, berat), harga beli default, harga jual default, umur ekonomis untuk
 * penyusutan, serta konfigurasi akun akuntansi yang terkait.
 *
 * <b>Cara kerja:</b><br>
 * Saat halaman diinisialisasi melalui {@link #doAfterCompose(Component)}, kelas
 * ini memuat data master aset dari database menggunakan Hibernate Criteria yang
 * dibangun oleh {@link #initCriteria(boolean)}. Filter pencarian tersedia
 * berdasarkan kode, nama, jenis aset, tipe aset, kelompok aset, dan akun
 * akuntansi yang terkait (melalui kelompok aset). Hasil ditampilkan di grid
 * dengan renderer {@link MasterAssetRenderer}.
 *
 * Form tambah/ubah data dibuka melalui popup window modal yang diinisialisasi
 * oleh {@link #init(MasterAsset)}. Form berisi semua field data master aset,
 * termasuk upload gambar barang (JPG), dan menampilkan informasi akun akuntansi
 * dari kelompok aset yang dipilih (akun pembelian, akumulasi penyusutan, biaya
 * penyusutan) secara dinamis. Harga rata-rata barang dihitung dari data penerimaan
 * pengadaan yang ada.
 *
 * Kelas ini mengimplementasikan tiga interface utilitas: {@link DataCriteria} untuk
 * penyediaan criteria query, {@link DataSearchDefault} untuk operasi pencarian,
 * dan {@link DataInitDefault} untuk inisialisasi form dari data yang dipilih di grid.
 * Implementasi ketiga interface ini memungkinkan fitur cetak data (Excel) dan upload
 * data massal berjalan otomatis via {@link Common#cetakData} dan
 * {@link Common#uploadData}.
 *
 * <b>Threading:</b><br>
 * Semua operasi berjalan di thread UI ZKoss (single-thread per sesi pengguna).
 * Tidak ada thread latar yang digunakan. Upload gambar menggunakan Streaming
 * SessionFactory terpisah ({@link StreamingHibernateUtil}) untuk menulis BLOB
 * ke database tanpa memblokir SessionFactory utama.
 *
 * <b>Pemeliharaan:</b><br>
 * Jika ada field baru yang ditambahkan ke entitas {@link MasterAsset}, perlu
 * diperbarui di: (1) form {@link #init(MasterAsset)}, (2) metode {@link #onSave},
 * (3) array {@code contents} di {@link #doAfterCompose} untuk ekspor data, dan
 * (4) renderer {@link MasterAssetRenderer} jika perlu ditampilkan di grid.
 * Jika konfigurasi akun kelompok aset berubah format JSON, perbarui
 * {@link #tampilAkun}.
 *
 * @author AIS Development Team
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 * @see MasterAsset
 * @see KelompokAsset
 * @see JenisAsset
 * @see DataCriteria
 * @see DataSearchDefault
 * @see DataInitDefault
 */
public class MasterAssetAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * Nomor versi serial untuk serialisasi kelas ini sesuai kontrak {@code Serializable}.
	 * Nilai ini digunakan oleh Java untuk memverifikasi kompatibilitas versi saat
	 * deserialisasi. Tidak perlu diubah kecuali struktur field yang diserialisasi berubah.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/**
	 * Window popup modal yang digunakan untuk form tambah dan ubah data master aset.
	 * Komponen ini di-autowire dari ZUL dan dikelola siklus hidupnya (tampil/sembunyi)
	 * oleh metode {@link #init(MasterAsset)}, {@link #onAdd}, dan tombol-tombol
	 * di dalam form.
	 */
	private MyWindow addWindow;

	/**
	 * Komponen paging untuk navigasi halaman data pada grid master aset.
	 * Diinisialisasi dengan listener yang memanggil {@link #onSearchDefault} setiap
	 * kali halaman aktif berubah.
	 */
	private Paging paging;

	/**
	 * Komponen grid utama yang menampilkan daftar master aset sesuai filter aktif.
	 * Diperbarui setiap kali pencarian dilakukan atau setelah operasi simpan/hapus.
	 */
	private MyGrid grid;

	// === Field filter pencarian ===

	/**
	 * Textbox filter pencarian berdasarkan kode barang/jasa.
	 * Menggunakan ILIKE dengan mode ANYWHERE (substring match, case-insensitive).
	 */
	private Textbox searchkode;

	/**
	 * Textbox filter pencarian berdasarkan nama barang/jasa.
	 * Menggunakan ILIKE dengan mode ANYWHERE.
	 */
	private Textbox searchnama;

	/**
	 * Combobox filter berdasarkan jenis aset ({@link JenisAsset}).
	 * Jika ada filter dari sesi ("JenisAsset"), otomatis terpilih dan dinonaktifkan.
	 */
	private Combobox searchjenisAsset;

	/**
	 * Combobox filter berdasarkan tipe aset (Tidak Habis Pakai, Habis Pakai,
	 * Jasa, Tidak Habis Pakai Non Aset, atau Semua). Default "Semua" (value null).
	 */
	private Combobox searchtipeAsset;

	/**
	 * Combobox filter berdasarkan kelompok aset ({@link KelompokAsset}).
	 * Jika ada filter dari sesi ("KelompokAsset"), otomatis terpilih dan dinonaktifkan.
	 */
	private Combobox searchkelompokAsset;

	/**
	 * Banbox (autocomplete) filter berdasarkan akun akuntansi. Jika akun dipilih,
	 * query akan join ke kelompok aset dan mencari di kolom JSON akun transaksi,
	 * penyusutan, dan biaya penyusutan.
	 */
	private AmbilDataAkunBanbox searchakun;

	// === Field form input ===

	/** Textbox untuk input kode unik barang/jasa. Boleh kosong (opsional). */
	private Textbox kode;

	/** Textbox untuk input nama barang/jasa. Wajib diisi (tidak boleh kosong). */
	private Textbox nama;

	/** Textbox untuk input merk barang/jasa. */
	private Textbox merk;

	/** Intbox untuk panjang dimensi barang dalam satuan yang ditentukan oleh field {@code unit}. */
	private Intbox panjang;

	/** Intbox untuk lebar dimensi barang. */
	private Intbox lebar;

	/** Intbox untuk tinggi dimensi barang. */
	private Intbox tinggi;

	/** Intbox untuk berat barang dalam satuan yang ditentukan oleh field {@code unitberat}. */
	private Intbox berat;

	/** Textbox untuk satuan dimensi (misalnya "cm", "m"). */
	private Textbox unit;

	/** Textbox untuk satuan berat (misalnya "kg", "gram"). */
	private Textbox unitberat;

	/** Combobox untuk jenis aset yang terkait dengan barang/jasa ini. Wajib diisi. */
	private Combobox jenisAsset;

	/** Combobox untuk kelompok aset. Kelompok menentukan akun akuntansi yang digunakan. Wajib diisi. */
	private Combobox kelompokAsset;

	/** Textbox untuk keterangan tambahan tentang barang/jasa ini. Opsional, multi-baris. */
	private Textbox keterangan;

	/** Combobox untuk satuan barang/jasa ({@link SatuanMasterAsset}). Wajib diisi. */
	private Combobox satuanMasterAsset;

	/** Doublebox untuk umur ekonomis barang dalam satuan bulan. Digunakan untuk kalkulasi penyusutan. */
	private MyDoublebox umurEkonomis;

	/** Doublebox untuk nilai buku minimal barang setelah penyusutan penuh. */
	private MyDoublebox nilaiMinimal;

	/** Doublebox untuk harga beli satuan default yang digunakan saat pemesanan tidak menyebut harga. */
	private MyDoublebox hargaBeliDefault;

	/**
	 * Jenis aset yang dikunci dari sesi (navigasi dari halaman lain). Jika tidak null,
	 * combo jenisAsset di form akan diset ke nilai ini dan dinonaktifkan.
	 */
	private JenisAsset selectedJenisAsset;

	/**
	 * Kelompok aset yang dikunci dari sesi (navigasi dari halaman lain). Jika tidak null,
	 * combo kelompokAsset di form akan diset ke nilai ini dan dinonaktifkan.
	 */
	private KelompokAsset selectedKelompokAsset;

	/**
	 * Flag hak akses UPDATE. Diinisialisasi dari {@link CommonPrivilages} saat halaman
	 * dibuka. Mengontrol visibilitas tombol Ubah pada baris grid.
	 */
	private boolean edit = false;

	/**
	 * Flag hak akses DELETE. Diinisialisasi dari {@link CommonPrivilages} saat halaman
	 * dibuka. Mengontrol visibilitas tombol Hapus pada baris grid.
	 */
	private boolean delete = false;

	/**
	 * Referensi ke entitas {@link MasterAsset} yang sedang diedit/ditambah.
	 * Diinisialisasi oleh {@link #init(MasterAsset)} dan digunakan oleh
	 * {@link #onSave} untuk menentukan operasi insert atau update.
	 */
	private MasterAsset masterAsset;

	/**
	 * Tombol Tambah pada toolbar halaman. Visibilitasnya dikontrol oleh privilege CREATE.
	 * Digunakan juga sebagai referensi posisi untuk menambahkan tombol Cetak dan Upload
	 * via {@link Common#appendKeToolbar}.
	 */
	private MyToolbarbuttonConfig add;

	/**
	 * Referensi ke file lampiran gambar barang/jasa yang sedang diunggah atau baru saja
	 * diunggah. Null jika tidak ada gambar baru. Harus berformat JPG.
	 */
	protected LampiranLain kop;

	/**
	 * Combobox untuk tipe aset pada form edit. Pilihan: Tidak Habis Pakai, Habis Pakai,
	 * Jasa, atau Tidak Habis Pakai Non Aset. Wajib diisi. Perubahan tipe memperbarui
	 * pilihan jenis aset secara dinamis.
	 */
	private Combobox tipe;

	/**
	 * Textbox untuk spesifikasi teknis detail barang/jasa. Multi-baris, opsional.
	 * Misalnya berisi deskripsi bahan, standar, atau fitur khusus barang.
	 */
	private Textbox spesifikasi;

	/**
	 * Combobox untuk kategori aset ({@link KategoriAsset}). Opsional; default
	 * "=Tanpa Kategori=" jika tidak dipilih. Memfilter hanya kategori yang aktif.
	 */
	private Combobox kategoriAsset;

	/**
	 * Checkbox yang menandakan apakah barang ini dapat dipinjamkan. Jika dicentang,
	 * barang dapat muncul sebagai pilihan dalam modul peminjaman aset.
	 */
	private MyCheckboxConfig bolehDipinjam;

	/**
	 * Panel tab yang menampilkan informasi stok persediaan barang. Tab ini di-lazy load:
	 * kontennya (include stok_awal.zul) baru dimuat saat tab pertama kali diklik.
	 */
	private Tabpanel tabStokPersediaan;

	/**
	 * Checkbox yang menandakan apakah harga beli satuan boleh diubah saat pemesanan.
	 * Jika tidak dicentang, harga di form pemesanan akan terkunci ke harga default.
	 */
	private MyCheckboxConfig hargaBolehDiubah;

	/**
	 * Checkbox yang menandakan apakah barang ini boleh dijual ke pihak luar.
	 * Jika dicentang, field harga jual akan ditampilkan dan barang dapat masuk
	 * ke modul penjualan aset.
	 */
	private MyCheckboxConfig bolehDijual;

	/**
	 * Doublebox untuk harga jual satuan default. Hanya tampil jika checkbox
	 * {@code bolehDijual} dicentang. Digunakan sebagai referensi harga di modul penjualan.
	 */
	private MyDoublebox hargaJualDefault;

	/**
	 * <b>Tujuan:</b> Menangani event klik pada tab "Stok Persediaan" dengan metode
	 * lazy loading — konten tab baru dimuat saat pertama kali tab diklik, bukan
	 * saat halaman pertama kali dibuka. Ini mencegah loading tidak perlu jika
	 * pengguna tidak membutuhkan informasi stok.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memeriksa apakah {@code tabStokPersediaan} sudah memiliki children.
	 *       Jika sudah, berarti sudah dimuat sebelumnya dan tidak perlu dimuat ulang.</li>
	 *   <li>Jika belum ada children, membuat {@link MyWindow} invisible sebagai
	 *       container, kemudian memasang {@link MyInclude} yang memuat halaman
	 *       {@code /pages/master/asset/stok_awal.zul} ke dalamnya.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — Event ZKoss dari klik tab. Tidak digunakan secara langsung.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan eksplisit; error dari loading
	 * halaman ZUL akan muncul sebagai error ZKoss.<br>
	 *
	 * <b>Pemeliharaan:</b> Jika path ZUL stok awal berubah, perbarui string path di
	 * konstruktor {@link MyInclude}. Jika ada inisialisasi tambahan yang diperlukan
	 * untuk halaman stok (misalnya passing parameter), tambahkan setelah MyInclude dibuat.
	 *
	 * @param event event ZKoss dari klik tab Persediaan
	 */
	public void onPersediaan(Event event) {
		if (tabStokPersediaan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabStokPersediaan);
			MyInclude iframe = new MyInclude("/pages/master/asset/stok_awal.zul");
			iframe.setParent(window);
		}
	}

	/**
	 * <b>Tujuan:</b> Dipanggil oleh ZKoss sebelum proses komposisi halaman dimulai,
	 * untuk memeriksa keamanan akses sebelum komponen ZUL dibuat.<br>
	 *
	 * <b>Cara kerja:</b> Memanggil {@link Common#doCheckSecurity()} yang mengalihkan
	 * pengguna ke login jika sesi tidak valid. Kemudian mendelegasikan ke superclass
	 * untuk melanjutkan proses komposisi ZKoss normal.<br>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code page} — Halaman ZKoss yang sedang dikomposisi.</li>
	 *   <li>{@code parent} — Komponen induk dalam hierarki halaman.</li>
	 *   <li>{@code compInfo} — Metadata komponen ZKoss.</li>
	 * </ul>
	 *
	 * <b>Return:</b> {@code ComponentInfo} dari superclass untuk melanjutkan komposisi.<br>
	 *
	 * <b>Penanganan error:</b> Jika sesi tidak aman, redirect dilakukan oleh
	 * {@code doCheckSecurity} dan eksekusi berhenti di sana.<br>
	 *
	 * <b>Pemeliharaan:</b> Jangan menghapus pemanggilan {@code doCheckSecurity()}.
	 *
	 * @param page halaman ZKoss yang sedang dikomposisi
	 * @param parent komponen induk dalam hierarki ZUL
	 * @param compInfo metadata komponen ZKoss
	 * @return {@code ComponentInfo} untuk melanjutkan proses komposisi
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Metode inisialisasi utama yang dipanggil ZKoss setelah semua
	 * komponen ZUL berhasil di-autowire. Bertanggung jawab atas seluruh persiapan
	 * halaman manajemen master aset.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan autowiring.</li>
	 *   <li>Menginisialisasi bahasa antarmuka via {@link Common#initLaguage()}.</li>
	 *   <li>Memeriksa sesi dan hak READ; jika tidak valid, arahkan ke login.</li>
	 *   <li>Mendaftarkan event listener pada banbox akun agar pencarian diulang
	 *       setiap kali akun filter berubah.</li>
	 *   <li>Mengisi combo filter jenis aset dari database.</li>
	 *   <li>Mengisi combo filter tipe aset dengan nilai konstanta yang telah
	 *       didefinisikan di {@link MasterAsset}, termasuk opsi "Semua" dengan
	 *       nilai null yang dipilih sebagai default.</li>
	 *   <li>Jika sesi menyimpan "JenisAsset", mengunci filter jenis aset ke nilai
	 *       tersebut dan menyimpan ke field {@code selectedJenisAsset}.</li>
	 *   <li>Mengisi dan mengunci filter kelompok aset dari sesi "KelompokAsset"
	 *       jika ada.</li>
	 *   <li>Menginisialisasi tombol Tambah dengan privilege CREATE.</li>
	 *   <li>Membaca flag hak edit dan delete dari privilege UPDATE dan DELETE.</li>
	 *   <li>Melakukan pencarian awal untuk memuat data ke grid.</li>
	 *   <li>Menginisialisasi paging dengan listener pencarian.</li>
	 *   <li>Mendefinisikan array field untuk ekspor data dan menambahkan tombol
	 *       Cetak serta Upload ke toolbar.</li>
	 *   <li>Menginisialisasi filter lanjut.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code comp} — Komponen root ZUL yang telah selesai dikomposisi.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Jika sesi tidak valid, pengguna diarahkan ke login.<br>
	 *
	 * <b>Pemeliharaan:</b> Array {@code contents} mendefinisikan field apa saja yang
	 * diekspor ke Excel. Jika ada field baru di {@link MasterAsset}, tambahkan nama
	 * property-nya ke array ini agar muncul di ekspor data.
	 *
	 * @param comp komponen root ZUL yang telah selesai dikomposisi
	 * @throws Exception jika terjadi kesalahan pada proses inisialisasi komponen
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

		Common.insertCombo(searchjenisAsset, "nama", "keterangan", JenisAsset.class);

		MyComboitemConfig comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(MasterAsset.TIPE_TIDAK_HABIS_PAKAI); }
		if (comboitem != null) { comboitem.setValue(MasterAsset.TIPE_TIDAK_HABIS_PAKAI); }
		searchtipeAsset.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(MasterAsset.TIPE_HABIS_PAKAI); }
		if (comboitem != null) { comboitem.setValue(MasterAsset.TIPE_HABIS_PAKAI); }
		searchtipeAsset.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(MasterAsset.TIPE_JASA); }
		if (comboitem != null) { comboitem.setValue(MasterAsset.TIPE_JASA); }
		searchtipeAsset.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(MasterAsset.TIPE_TIDAK_HABIS_PAKAI_NON_ASET); }
		if (comboitem != null) { comboitem.setValue(MasterAsset.TIPE_TIDAK_HABIS_PAKAI_NON_ASET); }
		searchtipeAsset.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchtipeAsset.appendChild(comboitem);

		if (searchtipeAsset != null) { searchtipeAsset.setSelectedItem(comboitem); }

		if (session.getAttribute("JenisAsset") != null) {
			selectedJenisAsset = (JenisAsset) session.getAttribute("JenisAsset");
			Common.selectComboItem(searchjenisAsset, session.getAttribute("JenisAsset"));
			searchjenisAsset.setDisabled(true);
			session.removeAttribute("JenisAsset");
		}

		Common.insertCombo(searchkelompokAsset, "nama", "keterangan", KelompokAsset.class);
		if (session.getAttribute("KelompokAsset") != null) {
			selectedKelompokAsset = (KelompokAsset) session.getAttribute("KelompokAsset");
			Common.selectComboItem(searchkelompokAsset, session.getAttribute("KelompokAsset"));
			searchkelompokAsset.setDisabled(true);
			session.removeAttribute("KelompokAsset");
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

		String[] contents = new String[] { "id", "kode", "nama", "merk", "panjang", "lebar", "tinggi", "berat", "unit",
				"unitberat", "satuanMasterAsset", "spesifikasi", "jenisAsset", "tipe", "kelompokAsset",
				"defaultPenyedia", "akunTransaksi", "akunPenyusutan", "akunBiayaPenyusutan", "umurEkonomis",
				"nilaiMinimal", "hargaBeliDefault", "kategoriAsset", "hargaBolehDiubah", "bolehDijual",
				"hargaJualDefault" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, MasterAsset.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
        FilterLanjutHelper.setup(comp);
	}

	/**
	 * <h3>MasterAssetRenderer — Renderer Baris Grid Master Aset</h3>
	 *
	 * <b>Untuk apa:</b><br>
	 * Inner class yang merender setiap baris data {@link MasterAsset} pada grid
	 * halaman manajemen master aset. Setiap baris menampilkan informasi ringkasan
	 * barang/jasa beserta tombol aksi untuk Ubah, Copy, dan Hapus.<br>
	 *
	 * <b>Cara kerja:</b><br>
	 * Dipanggil oleh ZKoss untuk setiap objek dalam model grid. Renderer menampilkan
	 * kode, nama barang (dengan widget revisi dan gambar inline), jenis aset, tipe,
	 * kelompok aset, kategori, keterangan, umur ekonomis, serta tombol aksi standar.
	 * Gambar barang dimuat secara inline via {@link LampiranLain#createDownloadUploadFileLain}
	 * dalam mode read-only (tanpa upload).<br>
	 *
	 * <b>Threading:</b><br>
	 * Dijalankan di thread UI ZKoss. Tidak ada operasi database tambahan selain
	 * yang sudah dieksekusi oleh lazy-loading Hibernate untuk relasi entity.<br>
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika kolom-kolom grid di ZUL berubah jumlah atau urutannya, pastikan urutan
	 * pemanggilan {@code setParent(arg0)} di sini diperbarui agar sesuai.
	 */
	class MasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data master aset ke dalam komponen-komponen
		 * ZKoss yang ditempatkan pada row grid.<br>
		 *
		 * <b>Cara kerja:</b>
		 * <ol>
		 *   <li>Mengatur vertical align baris ke "top".</li>
		 *   <li>Menampilkan kode barang sebagai Label sel pertama.</li>
		 *   <li>Membuat Vbox dengan widget revisi untuk nama barang, kemudian di dalam
		 *       Vbox tersebut membuat Hbox yang memuat gambar barang secara inline
		 *       (read-only, preview saja, tidak dapat diupload dari sini).</li>
		 *   <li>Menampilkan jenis aset, tipe, kelompok aset, kategori, keterangan,
		 *       dan umur ekonomis (dalam satuan bulan) sebagai sel-sel berikutnya.</li>
		 *   <li>Menambahkan tombol aksi Ubah/Copy/Hapus via
		 *       {@link Common#copyEditDeleteButtons} sesuai hak akses.</li>
		 * </ol>
		 *
		 * <b>Parameter:</b>
		 * <ul>
		 *   <li>{@code arg0} — Komponen {@link Row} yang akan diisi dengan sel data.</li>
		 *   <li>{@code arg1} — Objek {@link MasterAsset} untuk baris ini.</li>
		 * </ul>
		 *
		 * <b>Return:</b> Tidak ada (void).<br>
		 *
		 * <b>Penanganan error:</b> Jika relasi {@code jenisAsset}, {@code kelompokAsset},
		 * atau {@code kategoriAsset} null, ditampilkan string kosong sebagai fallback.<br>
		 *
		 * <b>Pemeliharaan:</b> Parameter {@code false} terakhir di
		 * {@code createDownloadUploadFileLain} menonaktifkan mode upload; ini penting
		 * agar renderer tidak memunculkan tombol upload yang tidak diperlukan di grid.
		 *
		 * @param arg0 baris grid yang akan diisi komponen
		 * @param arg1 objek data master aset untuk baris ini
		 * @throws Exception jika terjadi kesalahan saat membangun komponen atau akses database
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final MasterAsset masterAsset = (MasterAsset) arg1;
			new Label(masterAsset.getKode()).setParent(arg0);
			Vbox s;
			(s = RevisiHelper.createNewRevisi(MasterAsset.class, masterAsset, masterAsset.getNama())).setParent(arg0);

			Vbox myvbox = new Vbox();
			myvbox.setParent(s);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);

			LampiranLain.createDownloadUploadFileLain(hbox, masterAsset.getId(), LampiranLain.GAMBAR_MASTER_ASSET,
					"Gambar", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, true, null, false, true);

			new Label(masterAsset.getJenisAsset() == null ? "" : masterAsset.getJenisAsset().getNama()).setParent(arg0);

			new Label(masterAsset.getTipe()).setParent(arg0);

			new Label(masterAsset.getKelompokAsset() == null ? "" : masterAsset.getKelompokAsset().getNama())
					.setParent(arg0);

			new Label(masterAsset.getKategoriAsset() == null ? "" : masterAsset.getKategoriAsset().getNama())
					.setParent(arg0);

			new Label(masterAsset.getKeterangan()).setParent(arg0);

			new Label(Common.numberFormat.get().format(masterAsset.getUmurEkonomis()) + " bulan").setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, masterAsset, MasterAssetAction.this).setParent(arg0);
		}

	}

	/**
	 * <b>Tujuan:</b> Implementasi interface {@link DataInitDefault} yang dipanggil
	 * saat pengguna mengklik tombol Ubah atau Copy di baris grid. Mengatur master
	 * aset yang dipilih sebagai objek aktif dan membuka window form edit.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Melakukan cast {@code obj} ke {@link MasterAsset} dan menyimpan ke
	 *       field instance {@code masterAsset}.</li>
	 *   <li>Memanggil {@link #init(MasterAsset)} untuk membangun konten form.</li>
	 *   <li>Membuat window terlihat dan menampilkannya sebagai modal.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code obj} — Objek {@link GeneralValueObject} yang merupakan instance
	 *       {@link MasterAsset} yang dipilih dari grid.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Exception dari {@link #init} akan muncul ke atas.<br>
	 *
	 * <b>Pemeliharaan:</b> Method ini dipanggil oleh framework via refleksi melalui
	 * interface {@link DataInitDefault}. Jangan ubah nama atau signature method.
	 *
	 * @param obj objek {@link MasterAsset} yang dipilih untuk diedit
	 * @throws Exception jika terjadi kesalahan saat membangun form
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		masterAsset = (MasterAsset) obj;
		init(masterAsset);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol Tambah pada toolbar halaman.
	 * Membuka form dalam mode "Tambah Baru" dengan entitas {@link MasterAsset}
	 * yang kosong.<br>
	 *
	 * <b>Cara kerja:</b> Memanggil {@link #init(MasterAsset)} dengan objek
	 * {@code MasterAsset} baru (ID null = mode insert), kemudian menampilkan
	 * window sebagai modal.<br>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — Event ZKoss dari klik tombol Tambah. Tidak digunakan langsung.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Exception dari {@link #init} akan muncul ke atas.<br>
	 *
	 * <b>Pemeliharaan:</b> Nama method {@code onAdd} dikaitkan ke tombol di ZUL
	 * melalui konvensi penamaan ZKoss. Jangan ganti nama tanpa memperbarui ZUL.
	 *
	 * @param event event ZKoss dari klik tombol Tambah
	 * @throws Exception jika terjadi kesalahan saat membangun form
	 */
	public void onAdd(Event event) throws Exception {
		init(new MasterAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan mengisi konten form tambah/ubah master aset
	 * secara penuh — membersihkan konten lama, membangun layout baru, dan mengisi
	 * semua field dengan data dari entitas {@link MasterAsset} yang diberikan.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menyimpan referensi entitas ke field instance dan mengatur judul window
	 *       berdasarkan mode (tambah/ubah).</li>
	 *   <li>Membersihkan konten window sebelumnya via {@link Common#clear}.</li>
	 *   <li>Membangun layout Borderlayout dengan Center (form) dan South (toolbar).</li>
	 *   <li>Membangun grid dua kolom (label dan input) yang berisi semua field:</li>
	 *   <li>Field input: kode, nama (wajib), merk, gambar (upload JPG), satuan,
	 *       tipe, jenis aset, kelompok aset, kategori, checkbox boleh dipinjam,
	 *       dimensi P/L/T (kondisional berdasarkan konfigurasi), berat/unit
	 *       (kondisional), spesifikasi, umur ekonomis (kondisional), nilai minimal
	 *       (kondisional), harga beli default, checkbox harga boleh diubah, label
	 *       harga rata-rata (dihitung dari history penerimaan), checkbox boleh dijual,
	 *       harga jual default (tampil kondisional), dan keterangan.</li>
	 *   <li>Mendaftarkan listener pada kombo tipe untuk memperbarui pilihan jenis aset
	 *       secara dinamis saat tipe berubah.</li>
	 *   <li>Mendaftarkan listener pada kombo kelompok aset untuk menampilkan informasi
	 *       akun akuntansi dan mengontrol visibilitas field umur ekonomis.</li>
	 *   <li>Menambahkan tombol Batal dan Simpan ke South toolbar.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code masterAsset} — Entitas yang akan diisi ke form. ID null berarti
	 *       mode tambah baru; ID tidak null berarti mode ubah data yang ada.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Exception database saat mengambil harga rata-rata
	 * akan muncul ke atas. Jika lampiran gambar null, field gambar tetap tampil
	 * kosong tanpa error.<br>
	 *
	 * <b>Pemeliharaan:</b> Jika ada field baru di {@link MasterAsset} yang perlu
	 * muncul di form, tambahkan baris MyFormRow yang sesuai di sini. Perhatikan
	 * visibilitas kondisional berdasarkan konfigurasi ({@link Common#getKonfigurasi})
	 * agar tidak semua institusi menampilkan field yang tidak relevan bagi mereka.
	 *
	 * @param masterAsset entitas master aset yang akan diisi ke form (ID null = tambah baru)
	 * @throws Exception jika terjadi kesalahan saat membangun komponen atau akses database
	 */
	@SuppressWarnings("deprecation")
	private void init(final MasterAsset masterAsset) throws Exception {
		this.masterAsset = masterAsset;
		addWindow.setTitle(masterAsset.getId() == null ? "Tambah Barang/Jasa" : "Ubah Barang/Jasa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(masterAsset.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Barang/Jasa *"));
		row.appendChild(nama = new Textbox(masterAsset.getNama() == null ? "" : masterAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Merk"));
		row.appendChild(merk = new Textbox(masterAsset.getMerk()));
		merk.setWidth("90%");

		kop = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gambar Barang/Jasa (JPG)"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, masterAsset.getId(), LampiranLain.GAMBAR_MASTER_ASSET, "Gambar",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kop = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan *"));
		row.appendChild(satuanMasterAsset = new Combobox());
		satuanMasterAsset.setWidth("90%");
		satuanMasterAsset.setReadonly(true);
		Common.insertCombo(satuanMasterAsset, "nama", SatuanMasterAsset.class);
		Common.selectComboItem(satuanMasterAsset, masterAsset.getSatuanMasterAsset());

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

		Common.selectComboItem(tipe, masterAsset.getTipe());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis *"));
		row.appendChild(jenisAsset = new Combobox());
		Common.insertCombo(jenisAsset, "nama", "keterangan", JenisAsset.class);
		Common.selectComboItem(jenisAsset, masterAsset.getJenisAsset());
		jenisAsset.setWidth("90%");
		jenisAsset.setReadonly(true);
		if (selectedJenisAsset != null) {
			Common.selectComboItem(jenisAsset, selectedJenisAsset);
			jenisAsset.setDisabled(true);
		}

		tipe.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String t = ((String) (tipe.getSelectedItem() == null ? null : tipe.getSelectedItem().getValue()));
				Common.insertCombo(jenisAsset, "nama", "keterangan", JenisAsset.class,
						t == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("tipe", t));
				Common.selectComboItem(jenisAsset, masterAsset.getJenisAsset());
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok *"));
		row.appendChild(kelompokAsset = new Combobox());
		Common.insertComboDanSemua(kelompokAsset, "nama", "keterangan", KelompokAsset.class);
		Common.selectComboItem(kelompokAsset, masterAsset.getKelompokAsset());
		kelompokAsset.setWidth("90%");
		kelompokAsset.setReadonly(true);
		if (selectedKelompokAsset != null) {
			Common.selectComboItem(kelompokAsset, selectedKelompokAsset);
			kelompokAsset.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori"));
		row.appendChild(kategoriAsset = new Combobox());
		Common.insertComboDanSemua(kategoriAsset, new String[] { "nama" }, "keterangan", KategoriAsset.class,
				"=Tanpa Kategori=", Restrictions.eq("aktif", true));
		Common.selectComboItem(kategoriAsset, masterAsset.getKategoriAsset());
		kategoriAsset.setWidth("90%");
		kategoriAsset.setReadonly(true);
		// Tampil/sembunyi field Kategori sesuai konfigurasi (default tampil). Nilai tetap tersimpan.
		row.setVisible(Common.bolehKonfigurasi(Konfigurasi.MASTER_ASET_KATEGORI, Konfigurasi.AKTIF));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(bolehDipinjam = new MyCheckboxConfig("Barang ini boleh dipinjam"));
		bolehDipinjam.setChecked(masterAsset.getBolehDipinjam());

		row = new MyFormRow();
		row.setVisible(
				Common.bolehKonfigurasi("tampil_p_l_t_di_asset"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("P/L/T/Unit"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(panjang = new Intbox(masterAsset.getPanjang()));
		hbox.appendChild(lebar = new Intbox(masterAsset.getLebar()));
		hbox.appendChild(tinggi = new Intbox(masterAsset.getTinggi()));
		hbox.appendChild(unit = new Textbox(masterAsset.getUnit()));
		tinggi.setCols(2);
		panjang.setCols(2);
		lebar.setCols(2);
		unit.setCols(3);

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampil_berat_unit_di_asset"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berat/Unit"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(berat = new Intbox(masterAsset.getBerat()));
		berat.setCols(2);
		hbox.appendChild(unitberat = new Textbox(masterAsset.getUnitberat()));
		unitberat.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Spesifikasi"));
		row.appendChild(spesifikasi = new Textbox(masterAsset.getSpesifikasi()));
		spesifikasi.setWidth("90%");
		spesifikasi.setRows(3);

		final MyFormRow row1 = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row1, "2");
		row1.setParent(rows);

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampil_umur_ekonomis_di_asset"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Umur Ekonomis / Masa pakai (dalam bulan) Default"));
		row.appendChild(umurEkonomis = new MyDoublebox(masterAsset.getUmurEkonomis()));
		umurEkonomis.setCols(2);

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampil_nilai_buku_minimal_di_asset"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Buku Minimal Default"));
		row.appendChild(nilaiMinimal = new MyDoublebox(masterAsset.getNilaiMinimal()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Harga Beli Satuan"));
		row.appendChild(hargaBeliDefault = new MyDoublebox(masterAsset.getHargaBeliDefault()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hargaBolehDiubah = new MyCheckboxConfig("Harga boleh diubah"));
		hargaBolehDiubah.setChecked(masterAsset.getHargaBolehDiubah());

		Number hargaBeliAvg = (Number) (masterAsset == null || masterAsset.getId() == null
				? masterAsset.getHargaBeliDefault()
				: HibernateUtil.currentSession().createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
						.setProjection(Projections.avg("hargaBeli"))
						.add(Restrictions.eq("masterAsset.id", masterAsset.getId())).uniqueResult());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Harga Rata-Rata"));
		row.appendChild(new Label(Common.numberFormat.get()
				.format(hargaBeliAvg == null ? masterAsset.getHargaBeliDefault() : hargaBeliAvg.doubleValue())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(bolehDijual = new MyCheckboxConfig("Barang ini boleh dijual"));
		bolehDijual.setChecked(masterAsset.getBolehDijual());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Harga Jual Satuan"));
		row.appendChild(hargaJualDefault = new MyDoublebox(masterAsset.getHargaJualDefault()));

		EventListener bolehDijualEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hargaJualDefault.getParent().setVisible(bolehDijual.isChecked());
			}
		};

		bolehDijual.addEventListener("onClick", bolehDijualEventListener);
		bolehDijualEventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(masterAsset.getKeterangan() == null ? "" : masterAsset.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				KelompokAsset kelompok = (KelompokAsset) (kelompokAsset.getSelectedItem() == null ? null
						: kelompokAsset.getSelectedItem().getValue());

				umurEkonomis.getParent().setVisible(true);

				tampilAkun(row1, kelompok);

				if (kelompok != null) {
					umurEkonomis.getParent().setVisible(kelompok.getEstimasiUmurPakai() > 0);

				}
			}

		};

		kelompokAsset.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

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
	 * <b>Tujuan:</b> Menampilkan informasi akun akuntansi dari kelompok aset yang
	 * dipilih di dalam form edit. Akun yang ditampilkan mencakup akun pembelian,
	 * akun akumulasi penyusutan, dan akun biaya penyusutan — semuanya diambil dari
	 * konfigurasi JSON di entitas {@link KelompokAsset}.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membersihkan konten {@code rowParent} yang merupakan baris placeholder
	 *       untuk tampilan akun (spanning dua kolom).</li>
	 *   <li>Jika {@code kelompokAsset} tidak null, membuat sub-baris tampilan gulir
	 *       via {@link Common#tampilanScroll4} dan menambahkan label "Akun Pembelian".</li>
	 *   <li>Mengurai JSON {@code akunTransaksi} dari kelompok aset dan merender
	 *       formula akun via {@link AssetUtil#reloadFormula} dalam mode read-only.</li>
	 *   <li>Melakukan hal yang sama untuk akun akumulasi penyusutan dan akun biaya
	 *       penyusutan dalam baris-baris terpisah.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code rowParent} — Baris placeholder spanning dua kolom tempat informasi
	 *       akun akan ditampilkan. Konten sebelumnya akan dibersihkan terlebih dahulu.</li>
	 *   <li>{@code kelompokAsset} — Kelompok aset yang dipilih. Jika null, tidak ada
	 *       informasi akun yang ditampilkan (row dibersihkan saja).</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Jika JSON akun tidak valid atau kosong, akan terjadi
	 * exception dari {@code new JSONArray(...)}. Pastikan konfigurasi JSON di
	 * {@link KelompokAsset} selalu valid.<br>
	 *
	 * <b>Pemeliharaan:</b> Jika kolom akun di kelompok aset bertambah (misalnya akun
	 * revaluasi), tambahkan blok tampilan baru di method ini mengikuti pola yang sama.
	 * Field JSON harus konsisten dengan yang dibaca oleh {@link AssetUtil#reloadFormula}.
	 *
	 * @param rowParent baris placeholder tempat informasi akun akan ditampilkan
	 * @param kelompokAsset kelompok aset yang dipilih, atau null jika tidak ada pilihan
	 * @throws Exception jika terjadi kesalahan saat mengurai JSON atau membangun komponen
	 */
	private void tampilAkun(Row rowParent, KelompokAsset kelompokAsset) throws Exception {
		Common.clear(rowParent);
		if (kelompokAsset != null) {
			Row rowP = Common.tampilanScroll4(rowParent);

			rowP.appendChild(new ais.ui.util.MyLabelConfig("Akun Pembelian"));
			JSONArray akunTransaksi = new JSONArray(kelompokAsset.getAkunTransaksi());
			Row rowFormula = Common.tampilanScroll1(rowP);
			AssetUtil.reloadFormula(rowFormula, akunTransaksi, false);

			MyFormRow row2 = new MyFormRow();
			row2.setParent(rowP.getParent());
			row2.appendChild(new ais.ui.util.MyLabelConfig("Akun Akumulasi Penyusutan"));
			JSONArray akunPenyusutan = new JSONArray(kelompokAsset.getAkunPenyusutan());
			rowFormula = Common.tampilanScroll1(row2);
			AssetUtil.reloadFormula(rowFormula, akunPenyusutan, false);

			MyFormRow row = new MyFormRow();
			row.setParent(rowP.getParent());
			row.appendChild(new ais.ui.util.MyLabelConfig("Akun Biaya"));
			JSONArray akunBiayaPenyusutan = new JSONArray(kelompokAsset.getAkunBiayaPenyusutan());
			rowFormula = Common.tampilanScroll1(row);
			AssetUtil.reloadFormula(rowFormula, akunBiayaPenyusutan, false);
		}
	}

	/**
	 * <b>Tujuan:</b> Memvalidasi input form, mengambil nilai dari semua field,
	 * dan menyimpan data master aset ke database. Dipanggil oleh tombol Simpan
	 * di dalam form edit/tambah.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memvalidasi field wajib: nama, satuan, jenis, tipe, dan kelompok.
	 *       Jika ada yang kosong, menampilkan pesan peringatan dan mengembalikan false.</li>
	 *   <li>Memvalidasi format gambar: jika ada gambar baru yang diunggah, harus
	 *       berekstensi .jpg (case-insensitive).</li>
	 *   <li>Jika mode ubah (ID tidak null), melakukan load ulang entitas dari
	 *       database untuk memastikan data terkini (menghindari stale object).</li>
	 *   <li>Mengambil nilai dari semua field input dan mengatur ke entitas
	 *       {@code masterAsset}: kategori, kode, umur ekonomis, satuan, nama,
	 *       keterangan, berat, jenis, kelompok, lebar, panjang, tinggi, unit,
	 *       unitberat, merk, nilai minimal, harga beli, spesifikasi, boleh dipinjam,
	 *       harga boleh diubah, tipe, boleh dijual, dan harga jual.</li>
	 *   <li>Menyimpan entitas ke database via {@link Common#refreshSaveOrUpdate}.</li>
	 *   <li>Jika ada gambar baru (kop tidak null dan punya ID), memperbarui referensi
	 *       gambar ke ID master aset menggunakan Streaming SessionFactory terpisah.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — Event ZKoss dari klik tombol Simpan. Tidak digunakan langsung.</li>
	 * </ul>
	 *
	 * <b>Return:</b> {@code true} jika data berhasil disimpan; {@code false} jika
	 * validasi gagal dan pengguna perlu memperbaiki input.<br>
	 *
	 * <b>Penanganan error:</b> Error upload gambar ke Streaming SessionFactory
	 * ditangani dengan rollback transaction dan ditampilkan kepada admin via
	 * {@link Common#tampilErrorJikaAdmin}. Error database lainnya akan muncul ke atas.<br>
	 *
	 * <b>Pemeliharaan:</b> Jika ada field baru yang ditambahkan ke {@link MasterAsset},
	 * tambahkan baris setter di blok "mengatur ke entitas" setelah blok validasi.
	 * Pastikan juga menambahkan field tersebut ke form di {@link #init(MasterAsset)}.
	 *
	 * @param event event ZKoss dari klik tombol Simpan
	 * @return true jika berhasil disimpan, false jika validasi gagal
	 * @throws Exception jika terjadi kesalahan database yang tidak tertangani
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Aset belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama aset/barang yang akan didaftarkan; (2) Pastikan nama cukup deskriptif untuk memudahkan pencarian; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (satuanMasterAsset.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Satuan Aset belum dipilih. Langkah yang dapat dilakukan: (1) Pilih satuan dari dropdown Satuan; (2) Jika satuan belum ada, tambahkan melalui menu Satuan Aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jenisAsset.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Aset belum dipilih. Langkah yang dapat dilakukan: (1) Pilih jenis aset dari dropdown Jenis; (2) Jika jenis belum ada, tambahkan melalui menu Jenis Aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (tipe.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tipe Aset belum dipilih. Langkah yang dapat dilakukan: (1) Pilih tipe aset dari dropdown Tipe; (2) Tipe menentukan apakah aset berupa inventaris atau barang habis pakai; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (kelompokAsset.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Kelompok Aset belum dipilih. Langkah yang dapat dilakukan: (1) Pilih kelompok aset dari dropdown Kelompok; (2) Jika kelompok belum ada, tambahkan melalui menu Kelompok Aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (kop != null && !kop.getNama().toLowerCase().endsWith("jpg")) {
			MyMessageboxConfig.show("Mohon maaf, file Gambar yang diunggah bukan format JPG. Langkah yang dapat dilakukan: (1) Konversi gambar ke format JPG menggunakan aplikasi pengolah gambar; (2) Unggah ulang file gambar berformat .jpg; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (masterAsset.getId() != null) {
			masterAsset = (MasterAsset) session.load(MasterAsset.class, masterAsset.getId());

		}

		masterAsset.setKategoriAsset((KategoriAsset) (kategoriAsset.getSelectedItem() == null
				|| kategoriAsset.getSelectedItem().getValue() == null ? null
						: kategoriAsset.getSelectedItem().getValue()));

		masterAsset.setKode(kode.getValue());
		masterAsset.setUmurEkonomis(umurEkonomis.getValue());
		masterAsset.setSatuanMasterAsset((SatuanMasterAsset) satuanMasterAsset.getSelectedItem().getValue());
		masterAsset.setNama(nama.getValue());
		masterAsset.setKeterangan(keterangan.getValue());
		masterAsset.setBerat(berat.getValue());
		masterAsset.setJenisAsset(
				(JenisAsset) (jenisAsset.getSelectedItem() == null || jenisAsset.getSelectedItem().getValue() == null
						? null
						: jenisAsset.getSelectedItem().getValue()));
		masterAsset.setKelompokAsset((KelompokAsset) (kelompokAsset.getSelectedItem() == null
				|| kelompokAsset.getSelectedItem().getValue() == null ? null
						: kelompokAsset.getSelectedItem().getValue()));
		masterAsset.setLebar(lebar.getValue());
		masterAsset.setPanjang(panjang.getValue());
		masterAsset.setTinggi(tinggi.getValue());
		masterAsset.setUnit(unit.getValue());
		masterAsset.setUnitberat(unitberat.getValue());
		masterAsset.setMerk(merk.getValue());
		masterAsset.setNilaiMinimal(nilaiMinimal.getValue());
		masterAsset.setHargaBeliDefault(hargaBeliDefault.getValue());
		masterAsset.setSpesifikasi(spesifikasi.getValue());

		masterAsset.setBolehDipinjam(bolehDipinjam.isChecked());
		masterAsset.setHargaBolehDiubah(hargaBolehDiubah.isChecked());

		masterAsset.setTipe((String) (tipe.getSelectedItem() == null || tipe.getSelectedItem().getValue() == null ? null
				: tipe.getSelectedItem().getValue()));

		masterAsset.setBolehDijual(bolehDijual.isChecked());
		masterAsset.setHargaJualDefault(hargaJualDefault.getValue());

		Common.refreshSaveOrUpdate(session, masterAsset);

		if (kop != null && kop.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kop);
				kop.setRef(masterAsset.getId());

				session.getTransaction().begin();
				session.update(kop);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		return true;
	}

	/**
	 * <b>Tujuan:</b> Implementasi interface {@link DataCriteria} yang membangun
	 * dan mengembalikan objek {@link Criteria} Hibernate untuk query data master aset
	 * sesuai semua filter pencarian yang aktif di antarmuka pengguna.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mengambil akun dari banbox filter akun.</li>
	 *   <li>Membuka sesi Hibernate dan membuat criteria dasar untuk {@link MasterAsset}.</li>
	 *   <li>Jika parameter {@code order} true, menambahkan pengurutan ascending by nama.</li>
	 *   <li>Jika ada akun terpilih pada filter, join ke tabel kelompok aset dan
	 *       mencari ID akun tersebut dalam kolom JSON:
	 *       {@code akunPenyusutan}, {@code akunBiayaPenyusutan}, dan {@code akunTransaksi}.
	 *       Format pencarian menggunakan substring {@code ":" + akun.getId() + "}"}
	 *       yang sesuai dengan format JSON serialisasi yang digunakan sistem.</li>
	 *   <li>Menambahkan filter teks kode (ILIKE ANYWHERE).</li>
	 *   <li>Menambahkan filter teks nama (ILIKE ANYWHERE).</li>
	 *   <li>Menambahkan filter jenis aset jika ada pilihan di combo.</li>
	 *   <li>Menambahkan filter tipe aset jika ada pilihan di combo.</li>
	 *   <li>Menambahkan filter kelompok aset jika ada pilihan di combo.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code order} — Jika {@code true}, menambahkan ORDER BY nama ASC ke criteria.
	 *       Gunakan {@code false} untuk query count pada inisialisasi paging.</li>
	 * </ul>
	 *
	 * <b>Return:</b> Objek {@link Criteria} Hibernate yang siap untuk dieksekusi.<br>
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan eksplisit; exception Hibernate
	 * akan muncul ke atas jika terjadi masalah query atau koneksi.<br>
	 *
	 * <b>Pemeliharaan:</b> Format pencarian akun dalam JSON {@code ":" + id + "}"}
	 * bergantung pada format serialisasi yang digunakan oleh {@link AssetUtil}.
	 * Jika format JSON berubah, perbarui pola substring ini. Jika ada join alias
	 * yang dibuat ({@code kelompokAsset}), pastikan tidak konflik jika method
	 * dipanggil berulang dalam satu sesi.
	 *
	 * @param order jika {@code true} maka hasil diurutkan ascending berdasarkan nama
	 * @return objek {@link Criteria} dengan semua filter aktif yang sudah diterapkan
	 */
	public Criteria initCriteria(boolean order) {

		Akun akun = (Akun) searchakun.getAttribute("akun");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MasterAsset.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));

		if (akun != null && akun.getId() != null) {
			criteria.createAlias("kelompokAsset", "kelompokAsset").add(Restrictions.or(
					Restrictions.ilike("kelompokAsset.akunPenyusutan", ":" + akun.getId() + "}", MatchMode.ANYWHERE),
					Restrictions.or(
							Restrictions.ilike("kelompokAsset.akunBiayaPenyusutan", ":" + akun.getId() + "}",
									MatchMode.ANYWHERE),
							Restrictions.ilike("kelompokAsset.akunTransaksi", ":" + akun.getId() + "}",
									MatchMode.ANYWHERE))

			));
		}

		criteria

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchjenisAsset.getSelectedItem() == null || searchjenisAsset.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisAsset", searchjenisAsset.getSelectedItem().getValue()))

				.add(searchtipeAsset.getSelectedItem() == null || searchtipeAsset.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tipe", searchtipeAsset.getSelectedItem().getValue()))

				.add(searchkelompokAsset.getSelectedItem() == null
						|| searchkelompokAsset.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kelompokAsset", searchkelompokAsset.getSelectedItem().getValue()));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Implementasi interface {@link DataSearchDefault} yang mengeksekusi
	 * query pencarian master aset berdasarkan filter aktif dan memuat hasilnya ke grid.
	 * Juga dipanggil sebagai event handler dari ZUL saat tombol Cari diklik atau
	 * filter berubah.<br>
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menginisialisasi paging berdasarkan total jumlah data dari criteria tanpa ordering.</li>
	 *   <li>Mengambil satu halaman data menggunakan {@link ConstantValues#simpleList}
	 *       dengan criteria ordered, dibatasi oleh {@link Common#ROWS_COUNT_ON_PAGE}
	 *       dan offset sesuai halaman aktif paging.</li>
	 *   <li>Membungkus hasil dalam {@link SimpleListModel} dan menetapkan renderer
	 *       {@link MasterAssetRenderer} serta model ke grid.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code event} — Event ZKoss pemicu pencarian. Dapat {@code null} jika
	 *       dipanggil secara programatik (misalnya setelah simpan).</li>
	 * </ul>
	 *
	 * <b>Return:</b> Tidak ada (void).<br>
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan eksplisit; exception Hibernate
	 * atau ZKoss akan muncul ke atas.<br>
	 *
	 * <b>Pemeliharaan:</b> Penggunaan {@link ConstantValues#simpleList} (dibandingkan
	 * langsung memanggil {@code .list()}) memungkinkan penambahan cache atau
	 * transformasi hasil secara terpusat. Jika perilaku ini perlu diubah, cukup
	 * perbarui {@code ConstantValues.simpleList}.
	 *
	 * @param event event ZKoss pemicu, atau null jika dipanggil secara programatik
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<MasterAsset> masterAsset = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						MasterAsset.class);
		ListModel strset = new SimpleListModel(masterAsset);
		grid.setRowRenderer(new MasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

}
