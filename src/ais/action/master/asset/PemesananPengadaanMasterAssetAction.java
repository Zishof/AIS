package ais.action.master.asset;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox;
import ais.action.master.asset.helper.AmbilDataPermintaanPengadaanMasterAssetBanyak;
import ais.action.master.asset.helper.PemesananPengadaanMasterAssetDetailAction;
import ais.action.master.asset.helper.PemesananPengadaanMasterAssetHelper;
import ais.action.master.asset.helper.RevisiPemesananPengadaanMasterAssetHelper;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.sop.helper.SopUtil;
import ais.action.master.surat.util.SuratUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.asset.DokumenPenyediaAsset;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.JenisPajakPpn;
import ais.database.model.asset.JenisPemesananPengadaanAsset;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
import ais.database.model.asset.PemilikAsset;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.PenyediaAssetPunyaDokumen;
import ais.database.model.asset.PerjanjianKerjasamaMasterAsset;
import ais.database.model.asset.PerjanjianKerjasamaMasterAssetDetail;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h3>Untuk apa</h3>
 * Kelas ini adalah controller ZKoss (ZUL composer) utama untuk modul <b>Pemesanan Pengadaan Barang/Jasa</b>
 * (Purchase Order / SPK) dalam subsistem manajemen aset. Kelas ini mengelola seluruh siklus hidup
 * Purchase Order: pembuatan dokumen PO dengan baris rincian barang/jasa, alur persetujuan (approve/reject),
 * pengelolaan termin pembayaran, pembatalan, pencetakan PDF, dan ekspor data Excel. Kelas ini juga
 * mendukung integrasi dengan modul SOP (Alur Persetujuan), Perjanjian Kerjasama, Permintaan Pengadaan,
 * dan Anggaran (Workspace RAB).
 *
 * <h3>Cara kerja</h3>
 * Kelas ini mengimplementasikan antarmuka {@code FormSop} dan {@code DataCriteria}, sehingga dapat
 * diarahkan dari modul SOP untuk mengisi dan menyimpan data PO melalui alur disposisi. Pada saat
 * {@code doAfterCompose} dipanggil oleh ZKoss, kelas ini menginisialisasi komponen UI, menetapkan
 * hak akses pengguna (buat/ubah/hapus/setujui/tolak), memasang event listener pada paging dan timer
 * auto-refresh, serta menambah tombol aksi (Beli Langsung, Hitung Dibayar, History) ke toolbar.
 * Data grid diisi oleh {@code PemesananPengadaanMasterAssetRenderer} yang merender tiap baris PO
 * beserta tombol aksi inline. Formulir tambah/ubah PO dibangun secara programatik oleh metode
 * {@code form()} yang membangun grid ZUL dengan komponen input ZKoss. Rincian barang/jasa dikelola
 * oleh {@code PemesananPengadaanMasterAssetHelper}. Data termin pembayaran disimpan sebagai
 * {@code JSONArray} dalam field {@code formula} entitas PO, dan dirender oleh
 * {@code reloadDataFormula()}/{@code reloadFormula()}.
 *
 * <h3>Threading</h3>
 * Seluruh operasi UI dan akses Hibernate berjalan di thread ZKoss event-dispatcher. Operasi
 * yang membutuhkan jeda (seperti cetak PDF setelah simpan) menggunakan
 * {@code Common.createDefaultTimer()} agar tidak memblokir thread event. Tidak ada state static
 * yang berbagi data antar-desktop/session, kecuali {@code NomorSuratAlurPengadaan.PEMESANAN_PEMBELIAN_DATA}
 * yang merupakan konfigurasi read-only dan aman dibaca dari banyak thread.
 *
 * <h3>Pemeliharaan</h3>
 * Saat menambah field baru pada entitas {@code PemesananPengadaanMasterAsset}, pastikan:
 * (1) field tersebut diinisialisasi di {@code form()}, (2) disimpan di {@code onSave()},
 * (3) ditambahkan ke array {@code contents} di {@code doAfterCompose()} untuk ekspor Excel,
 * dan (4) dirender di {@code PemesananPengadaanMasterAssetRenderer.render()} jika perlu
 * ditampilkan di grid. Untuk field termin (JSON), perbarui {@code mapTermin()} dan
 * {@code paramTermin()} agar nilai terbaca saat cetak laporan.
 *
 * @author Generated Javadoc
 * @see PemesananPengadaanMasterAsset
 * @see PemesananPengadaanMasterAssetHelper
 * @see FormSop
 * @see DataCriteria
 */
public class PemesananPengadaanMasterAssetAction extends GenericAutowireComposer implements FormSop, DataCriteria {

	/** Serial version UID untuk serialisasi kelas oleh ZKoss. */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchkode;
	private Textbox searchketerangan;
	private AmbilDataPenyediaAssetBanbox searchPenyedia;
	private Combobox searchlokasi;
	private Textbox searchcatatan;
	private MyDatebox start;
	private MyDatebox end;

	private Label kode;
	private MyTextbox kodeInvoice;
	private MyTextbox keterangan;
	private MyDatebox tanggalPembuatan;
	private AmbilDataPenyediaAssetBanbox penyediaAsset;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset;
	private MyToolbarbuttonConfig add;
	private Combobox pemilikAsset;
	private Combobox lokasi;
	private AmbilDataRuangBanbox ruang;
	private Vbox permintaanPengadaanMasterAsset;
	private Tbmuser tbmuser;
	private boolean persetujuan = false;
	private MyCheckboxConfig tanpaAnggaran;
	private AmbilDataWorkspaceBanbox workspace;
	private Combobox perjanjianKerjasamaMasterAsset;

	private DisposisiSop disposisiSop = null;

	private MyCheckboxConfig lunasSaja;
	private MyCheckboxConfig blmLunasSaja;

	private MyCheckboxConfig blmDisetujui;
	private MyCheckboxConfig disetujui;

	private PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAssetData = null;

	private boolean tampilkanRuanganDamPemilikAset = Common.bolehKonfigurasi("tampilkanRuanganDamPemilikAset", Konfigurasi.TIDAK_AKTIF);

	private Tabpanel tabJenisPemesanan;

	/**
	 * <b>Tujuan:</b> Menampilkan konten tab "Jenis Pemesanan" secara lazy (baru dimuat saat tab diklik
	 * untuk pertama kali), sehingga menghindari pemuatan ZUL yang tidak perlu saat halaman pertama dibuka.
	 *
	 * <b>Cara kerja:</b> Memeriksa apakah {@code tabJenisPemesanan} sudah memiliki anak komponen.
	 * Jika belum, membuat {@code MyInclude} yang memuat ZUL
	 * {@code /pages/master/asset/jenis_pemesanan_pengadaan_asset.zul} dan menambahkannya ke dalam
	 * tab panel. Pendekatan ini memastikan ZUL sub-halaman hanya di-parse dan di-render sekali.
	 *
	 * <b>Parameter:</b>
	 * @param event event ZKoss yang dipicu saat tab diklik (biasanya event {@code onSelect} dari Tabbox).
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit; jika ZUL tidak ditemukan,
	 * ZKoss akan melempar {@code UiException} yang ditangkap oleh framework.
	 *
	 * <b>Pemeliharaan:</b> Jika path ZUL jenis pemesanan berubah, perbarui string path di sini.
	 * Pastikan ZUL tujuan kompatibel dengan konteks parent tab panel.
	 */
	public void onTampilJenisPemesanan(Event event) {
		if (tabJenisPemesanan.getChildren().size() == 0) {
			MyInclude include = new MyInclude("/pages/master/asset/jenis_pemesanan_pengadaan_asset.zul");
			include.setHeight("100%");
			include.setWidth("100%");
			tabJenisPemesanan.appendChild(include);
		}
	}

	/**
	 * <b>Tujuan:</b> Konstruktor default yang digunakan oleh ZKoss saat me-load halaman utama PO
	 * melalui ZUL. Menginisialisasi {@code tbmuser} dengan pengguna yang sedang login.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code Common.getCurrentUser()} untuk mendapatkan objek
	 * {@code Tbmuser} dari sesi pengguna aktif dan menyimpannya ke field instance {@code tbmuser}.
	 * Field ini digunakan sepanjang siklus hidup composer untuk mencatat pembuat/penyetuju dokumen.
	 *
	 * <b>Penanganan error:</b> Jika tidak ada sesi aktif, {@code Common.getCurrentUser()} dapat
	 * mengembalikan {@code null}; hal ini akan ditangkap saat verifikasi hak akses di
	 * {@code doAfterCompose()}.
	 *
	 * <b>Pemeliharaan:</b> Konstruktor ini hanya digunakan oleh ZKoss. Jangan tambahkan logika
	 * inisialisasi berat di sini karena komponen UI belum siap; gunakan {@code doAfterCompose()}.
	 */
	public PemesananPengadaanMasterAssetAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * <b>Tujuan:</b> Konstruktor alternatif yang digunakan saat action ini dibuat secara programatik
	 * (bukan dari ZUL), misalnya dari modul SOP untuk menampilkan form PO dalam mode persetujuan.
	 *
	 * <b>Cara kerja:</b> Menetapkan flag {@code persetujuan} sesuai parameter, kemudian menginisialisasi
	 * {@code tbmuser} dengan pengguna aktif. Ketika {@code persetujuan} bernilai {@code true}, form
	 * yang dihasilkan oleh metode {@code form()} akan menampilkan semua field dalam mode hanya-baca
	 * (Label, bukan input), dan menampilkan checkbox persetujuan sebagai pengganti tombol simpan biasa.
	 *
	 * <b>Parameter:</b>
	 * @param persetujuan {@code true} jika action ini digunakan dalam alur persetujuan SOP/disposisi,
	 *                    {@code false} untuk mode CRUD normal.
	 *
	 * <b>Penanganan error:</b> Sama seperti konstruktor default.
	 *
	 * <b>Pemeliharaan:</b> Jika ada mode baru selain persetujuan (misalnya mode preview), pertimbangkan
	 * menambah parameter enum alih-alih boolean agar lebih ekspresif.
	 */
	public PemesananPengadaanMasterAssetAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private EventListener eventListener = null;
	private Boolean pembelianLangsung = false;

	/**
	 * <b>Tujuan:</b> Hook ZKoss yang dipanggil sebelum proses compose dimulai. Digunakan untuk
	 * memvalidasi keamanan akses halaman sebelum komponen UI dibangun.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} yang memeriksa apakah pengguna
	 * memiliki akses ke halaman ini berdasarkan konfigurasi hak akses sistem. Jika tidak punya akses,
	 * method tersebut akan mengarahkan pengguna keluar (redirect ke halaman logoff). Setelah pengecekan,
	 * memanggil implementasi superclass untuk melanjutkan proses compose normal ZKoss.
	 *
	 * <b>Parameter:</b>
	 * @param page     halaman ZKoss saat ini yang sedang di-compose.
	 * @param parent   komponen parent tempat composer ini dipasang.
	 * @param compInfo metadata komponen dari ZUL yang sedang di-parse.
	 * @return {@code ComponentInfo} dari superclass untuk melanjutkan proses compose ZKoss.
	 *
	 * <b>Penanganan error:</b> Pengecekan keamanan di {@code Common.doCheckSecurity()} menangani
	 * kasus akses tidak sah dengan redirect; tidak ada exception yang dilempar dari metode ini.
	 *
	 * <b>Pemeliharaan:</b> Jangan hapus pemanggilan {@code Common.doCheckSecurity()} karena akan
	 * membuka celah keamanan akses halaman tanpa autentikasi.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Hook inisialisasi utama ZKoss yang dipanggil setelah seluruh komponen ZUL
	 * selesai di-wire ke field Java. Metode ini menyiapkan halaman daftar PO: validasi sesi,
	 * inisialisasi filter pencarian, paging, timer auto-refresh, hak akses tombol, dan tombol
	 * aksi tambahan (Beli Langsung, Hitung Dibayar, History).
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memeriksa validitas sesi ({@code usersTemp}) dan hak akses READ; jika gagal, redirect logoff.</li>
	 *   <li>Memasang event listener pada {@code searchparent} (satuan kerja) dan {@code searchPenyedia}
	 *       agar pencarian otomatis terpicu saat nilai berubah.</li>
	 *   <li>Mengatur nilai default filter tanggal: mulai = 6 bulan lalu, akhir = besok.</li>
	 *   <li>Mengisi combobox lokasi dengan data dari database dan mengunci lokasi jika ada atribut sesi.</li>
	 *   <li>Menentukan visibilitas tombol Tambah berdasarkan hak CREATE, serta flag edit/delete/approve/reject.</li>
	 *   <li>Menginisialisasi paging dan timer auto-refresh yang memanggil {@code onSearchDefault()}.</li>
	 *   <li>Menambah tombol "Beli Langsung" untuk membuat PO langsung dari daftar permintaan tanpa
	 *       melalui form tambah biasa.</li>
	 *   <li>Menambah tombol "Hitung Dibayar" yang menghitung ulang nilai pembayaran untuk semua PO
	 *       yang sesuai filter (maks 5000 data) dan menyimpan hasilnya.</li>
	 *   <li>Menambah tombol "History" untuk menampilkan riwayat revisi PO via
	 *       {@code RevisiPemesananPengadaanMasterAssetHelper}.</li>
	 *   <li>Menyiapkan tombol cetak data (ekspor Excel) via {@code Common.cetakData()}.</li>
	 *   <li>Menginisialisasi filter lanjut via {@code FilterLanjutHelper.setup()}.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param comp komponen root ZUL yang sudah di-compose oleh ZKoss.
	 * @throws Exception jika terjadi kesalahan inisialisasi komponen atau akses database.
	 *
	 * <b>Penanganan error:</b> Jika sesi tidak valid, pengguna diarahkan ke logoff. Kesalahan
	 * lain akan dipropagasi ke ZKoss error handler.
	 *
	 * <b>Pemeliharaan:</b> Saat menambah tombol toolbar baru, gunakan pola yang sama:
	 * buat {@code MyToolbarbuttonConfig}, tambahkan {@code addEventListener}, lalu
	 * {@code setParent(add.getParent())} agar tombol muncul di toolbar yang benar.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (searchparent == null) return;
		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (searchPenyedia != null) {
			searchPenyedia.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});
		}

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		Common.insertComboDanSemua(searchlokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (session.getAttribute("Lokasi") != null) {
			Common.selectComboItem(searchlokasi, session.getAttribute("Lokasi"));
			searchlokasi.setDisabled(true);
			session.removeAttribute("Lokasi");
		}
		LokasiAction.kunciLokasi(searchlokasi);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);

		Common.initPaging(paging, new EventListener() {
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

		String[] contents = new String[] { "id", "kode", "kodeInvoice", "penyedia", "keterangan",
				"jenisPemesananPengadaanAsset", "tanggalPembuatan", "tanggalPersetujuan", "pengirimanPalingLambat",
				"dibuatOleh", "disetujuiOleh", "ppn", "persenPpn", "dp", "satuanKerja",
				"permintaanPengadaanMasterAssets", "angarans", "disposisiSop", "tampaPermintaan", "tahun", "bulan",
				"nomorSuratAlurPengadaan", "catatanKesepakatan", "perjanjianKerjasamaMasterAsset", "nilai", "dibayar",
				"byTermin", "formula", "tanggalMulai", "tanggalSampai" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PemesananPengadaanMasterAsset.class, this,
				contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Beli Langsung", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<PermintaanPengadaanMasterAsset> permintaanPengadaanMasterAssets = new ArrayList<PermintaanPengadaanMasterAsset>();

						AmbilDataPermintaanPengadaanMasterAssetBanyak ambilPermintaanPengadaanMasterAsset = new AmbilDataPermintaanPengadaanMasterAssetBanyak(
								true, permintaanPengadaanMasterAssets,
								(SatuanKerja) searchparent.getAttribute("satuanKerja"));
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
								.appendChild(ambilPermintaanPengadaanMasterAsset);
						ambilPermintaanPengadaanMasterAsset.setWidth("90%");
						ambilPermintaanPengadaanMasterAsset.setHeight("90%");

						ambilPermintaanPengadaanMasterAsset.setEventListener(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								onSearchDefault(null);
							}
						});

						ambilPermintaanPengadaanMasterAsset.onModal();
					}
				});
			}
		});
		if (button != null) { button.setParent(add.getParent()); }

		button = new MyToolbarbuttonConfig("Hitung Dibayar", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<PemesananPengadaanMasterAsset> pemesananPengadaanMasterAssets = initCriteria(false)
								.addOrder(Order.desc("id")).setMaxResults(5000).list();

						for (PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset : pemesananPengadaanMasterAssets) {

							Double dibayar = pemesananPengadaanMasterAsset.hitungDibayar();

							if (dibayar.intValue() != pemesananPengadaanMasterAsset.getDibayar().intValue()) {
								pemesananPengadaanMasterAsset.setDibayar(dibayar);
								Session session = HibernateUtil.currentSession();
								Common.refreshUpdate(session, pemesananPengadaanMasterAsset);
								session.flush();
							}

						}
						onSearchDefault(null);
					}
				});

			}

		});
		if (button != null) { button.setParent(add.getParent()); }

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiPemesananPengadaanMasterAssetHelper revisiHelper = new RevisiPemesananPengadaanMasterAssetHelper(
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										onSearchDefault(arg0);
									}
								});
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		if (button != null) { button.setParent(add.getParent()); }
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * <b>Untuk apa:</b> Kelas inner renderer ZKoss yang bertanggung jawab merender satu baris
	 * data {@code PemesananPengadaanMasterAsset} (Purchase Order) ke dalam baris grid ({@code Row})
	 * di halaman daftar PO. Setiap baris berisi ringkasan data PO beserta tombol aksi inline.
	 *
	 * <b>Cara kerja:</b> Dipanggil oleh ZKoss untuk setiap objek dalam {@code ListModel} grid.
	 * Untuk setiap PO, renderer membuat:
	 * <ul>
	 *   <li>Komponen detail ({@code PemesananPengadaanMasterAssetDetailAction}) untuk sub-grid rincian barang.</li>
	 *   <li>Label revisi (kode PO, jenis pemesanan, kode invoice, kode perjanjian, workspace).</li>
	 *   <li>Label penyedia, pemilik aset, lokasi, ruang, jenis pemesanan, nilai, dan jumlah dibayar.</li>
	 *   <li>Label pembuat dan tanggal pembuatan, serta label penyetuju/penolak beserta tanggalnya.</li>
	 *   <li>Checkbox aktif/nonaktif yang dapat langsung diubah dari grid jika pengguna memiliki hak UPDATE.</li>
	 *   <li>Tombol aksi: Cetak, Setujui, Tolak, Batalkan, Ubah, dan Hapus — dengan visibilitas
	 *       masing-masing dikontrol oleh hak akses dan status persetujuan PO.</li>
	 * </ul>
	 *
	 * <b>Threading:</b> Dijalankan di thread ZKoss event-dispatcher. Semua aksi tombol menggunakan
	 * event listener anonim yang berjalan di thread yang sama. Akses database dilakukan via
	 * {@code HibernateUtil.currentSession()} yang terikat pada thread.
	 *
	 * <b>Pemeliharaan:</b> Jumlah sel yang dirender di sini harus sesuai persis dengan jumlah
	 * {@code <column>} yang didefinisikan di ZUL. Jika menambah/menghapus kolom, pastikan
	 * keduanya diperbarui bersama agar grid tidak rusak tampilannya.
	 */
	class PemesananPengadaanMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris objek {@code PemesananPengadaanMasterAsset} menjadi
		 * komponen ZKoss di dalam {@code Row} pada grid daftar PO.
		 *
		 * <b>Cara kerja:</b> Cast parameter {@code arg1} ke {@code PemesananPengadaanMasterAsset},
		 * kemudian membuat dan memasang komponen UI satu per satu ke {@code arg0} (Row). Urutan
		 * penambahan komponen mencerminkan urutan kolom di ZUL. Tombol aksi dikumpulkan dalam
		 * {@code Hbox} dan ditambahkan di akhir baris. Event listener untuk setiap tombol mengakses
		 * variabel lokal {@code pemesananPengadaanMasterAsset} dan label status via closure.
		 *
		 * <b>Parameter:</b>
		 * @param arg0 baris ZKoss ({@code Row}) tempat komponen akan ditambahkan.
		 * @param arg1 objek data dari model, yang di-cast menjadi {@code PemesananPengadaanMasterAsset}.
		 * @throws Exception jika terjadi kesalahan inisialisasi komponen ZKoss atau akses data.
		 *
		 * <b>Penanganan error:</b> Kesalahan saat delete ditangkap dan ditampilkan via
		 * {@code Common.tampilErrorJikaAdmin()} dan pesan messagebox ke pengguna. SOP disposisi
		 * dihapus terlebih dahulu via {@code SopUtil.hapusDisposisi()} sebelum menghapus PO.
		 *
		 * <b>Pemeliharaan:</b> Visibilitas tombol Setujui, Tolak, Batalkan, Ubah, dan Hapus
		 * mengikuti logika: Setujui hanya jika belum disetujui dan belum ditolak dan bukan
		 * pembelian langsung; Tolak jika belum ditolak; Batalkan jika sudah disetujui atau ditolak
		 * dan bukan pembelian langsung; Ubah jika belum disetujui atau admin; Hapus jika belum disetujui.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = (PemesananPengadaanMasterAsset) arg1;

			final PemesananPengadaanMasterAssetDetailAction detail;
			(detail = new PemesananPengadaanMasterAssetDetailAction(pemesananPengadaanMasterAsset)).setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PemesananPengadaanMasterAsset.class, pemesananPengadaanMasterAsset,
					pemesananPengadaanMasterAsset.getKode())).setParent(arg0);

			a.appendChild(new Label(pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset() == null ? ""
					: pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset().getNama()));

			a.appendChild(new Label(pemesananPengadaanMasterAsset.getKodeInvoice()));

			a.appendChild(new Label(pemesananPengadaanMasterAsset.getPerjanjianKerjasamaMasterAsset() == null ? ""
					: pemesananPengadaanMasterAsset.getPerjanjianKerjasamaMasterAsset().getKode()));

			if (pemesananPengadaanMasterAsset.getWorkspace() != null) {
				new Label(pemesananPengadaanMasterAsset.getWorkspace().getKode() + "-"
						+ pemesananPengadaanMasterAsset.getWorkspace().getNama()).setParent(a);
			}

			new Label(pemesananPengadaanMasterAsset.getPenyedia() == null ? ""
					: pemesananPengadaanMasterAsset.getPenyedia().getNama()).setParent(arg0);

			new Label(pemesananPengadaanMasterAsset.getPemilikAsset() == null ? ""
					: pemesananPengadaanMasterAsset.getPemilikAsset().getNama()).setParent(arg0);

			new Label(pemesananPengadaanMasterAsset.getLokasi() == null ? ""
					: (pemesananPengadaanMasterAsset.getLokasi().getNama())).setParent(arg0);

			new Label(pemesananPengadaanMasterAsset.getRuang() == null ? ""
					: pemesananPengadaanMasterAsset.getRuang().getNama()).setParent(arg0);

			new Label(pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset() == null ? ""
					: pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset().getNama()).setParent(arg0);

			a = new Vbox();
			a.setParent(arg0);
			a.setAlign("end");
			a.setPack("end");
			new Label(Common.numberFormat.get().format(pemesananPengadaanMasterAsset.getNilai())).setParent(a);
			new Label(Common.numberFormat.get().format(pemesananPengadaanMasterAsset.getDibayar())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(pemesananPengadaanMasterAsset.getDibuatOleh() == null ? ""
					: pemesananPengadaanMasterAsset.getDibuatOleh().getUserNama()).setParent(a);
			new MyLabelAgakKecil(pemesananPengadaanMasterAsset.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(pemesananPengadaanMasterAsset.getTanggalPembuatan()))
					.setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			final MyLabelAgakKecil disetujuiOleh = new MyLabelAgakKecil(
					pemesananPengadaanMasterAsset.getDisetujuiOleh() == null ? ""
							: pemesananPengadaanMasterAsset.getDisetujuiOleh().getUserNama());

			final MyLabelAgakKecil disetujuiTanggal = new MyLabelAgakKecil(
					pemesananPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
							: Common.dateFormat3.get().format(pemesananPengadaanMasterAsset.getTanggalPersetujuan()));

			final MyLabelAgakKecilBoldMerah ditolakOleh = new MyLabelAgakKecilBoldMerah(
					pemesananPengadaanMasterAsset.getDitolakOleh() == null ? ""
							: "Ditolak oleh " + pemesananPengadaanMasterAsset.getDitolakOleh().getUserNama());
			final MyLabelAgakKecilBoldMerah tanggalDitolak = new MyLabelAgakKecilBoldMerah(
					pemesananPengadaanMasterAsset.getTanggalDitolak() == null ? ""
							: Common.dateFormat3.get().format(pemesananPengadaanMasterAsset.getTanggalDitolak()));

			ditolakOleh.setParent(a);
			tanggalDitolak.setParent(a);
			disetujuiOleh.setParent(a);
			disetujuiTanggal.setParent(a);

			new Label(pemesananPengadaanMasterAsset.getPengirimanPalingLambat() == null ? ""
					: Common.dateFormat1.get().format(pemesananPengadaanMasterAsset.getPengirimanPalingLambat()))
					.setParent(arg0);

			new Label(pemesananPengadaanMasterAsset.getByTermin() ? "Ya" : "Tidak").setParent(arg0);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(Common.simpleString(pemesananPengadaanMasterAsset.getKeterangan())).setParent(vbox1);
			if (pemesananPengadaanMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pemesananPengadaanMasterAsset.getDisposisiSop().getKeterangan()
						+ " (" + pemesananPengadaanMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pemesananPengadaanMasterAsset.getDisposisiSop().getId(), null,
								null, true, arg0.getTarget());
					}
				});
			}

			if (pemesananPengadaanMasterAsset.getDisposisiSop() != null
					&& !pemesananPengadaanMasterAsset.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (edit) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(pemesananPengadaanMasterAsset.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pemesananPengadaanMasterAsset.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(pemesananPengadaanMasterAsset);
					}
				});
			} else {
				new Label(pemesananPengadaanMasterAsset.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Pemesanan Pengadaan Asset");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					cetak(pemesananPengadaanMasterAsset);
				}

			});
			button.setParent(toolbar);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");
			final MyToolbarbuttonConfig ditolak = new MyToolbarbuttonConfig("", "/img/svg/deny.svg");
			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null
					&& pemesananPengadaanMasterAsset.getDitolakOleh() == null
					&& !pemesananPengadaanMasterAsset.getPembelianLangsung());

			ditolak.setVisible(reject && pemesananPengadaanMasterAsset.getDitolakOleh() == null);

			dibatalkan.setVisible(reject
					&& (pemesananPengadaanMasterAsset.getDisetujuiOleh() != null
							|| pemesananPengadaanMasterAsset.getDitolakOleh() != null)
					&& !pemesananPengadaanMasterAsset.getPembelianLangsung());

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui pemesanan pengadaan barang/jasa ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();
										Integer countMasterAssetjumlah = ((Number) session
												.createCriteria(PemesananPengadaanMasterAssetDetail.class)
												.setProjection(Projections.count("id"))
												.add(Restrictions.eq("pemesananPengadaanMasterAsset",
														pemesananPengadaanMasterAsset))
												.add(Restrictions.lt("jumlah", 1.0)).uniqueResult()).intValue();

										if (!countMasterAssetjumlah.equals(0)) {
											MyMessageboxConfig.show("Mohon maaf, terdapat baris item pada pemesanan yang belum memiliki jumlah yang valid. Langkah yang dapat dilakukan: (1) Periksa setiap baris pada daftar barang/jasa pemesanan; (2) Isi jumlah pada baris yang masih kosong atau bernilai 0; (3) ulangi proses persetujuan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											return;
										}

										pemesananPengadaanMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
										pemesananPengadaanMasterAsset
												.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

										Common.refreshUpdate(session, pemesananPengadaanMasterAsset);

										disetujuiTanggal.setValue(
												pemesananPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																pemesananPengadaanMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pemesananPengadaanMasterAsset.getDisetujuiOleh() == null
												? ""
												: pemesananPengadaanMasterAsset.getDisetujuiOleh().getUserNama());

										ditolakOleh.setValue(pemesananPengadaanMasterAsset.getDitolakOleh() == null ? ""
												: "Ditolak oleh "
														+ pemesananPengadaanMasterAsset.getDitolakOleh().getUserNama());
										tanggalDitolak
												.setValue(pemesananPengadaanMasterAsset.getTanggalDitolak() == null ? ""
														: Common.dateFormat3.get().format(
																pemesananPengadaanMasterAsset.getTanggalDitolak()));

										disetujui.setVisible(
												approve && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null
														&& pemesananPengadaanMasterAsset.getDitolakOleh() == null);
										ditolak.setVisible(
												reject && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null
														&& pemesananPengadaanMasterAsset.getDitolakOleh() == null);
										dibatalkan.setVisible(
												reject && (pemesananPengadaanMasterAsset.getDisetujuiOleh() != null
														|| pemesananPengadaanMasterAsset.getDitolakOleh() != null));
										rubah.setVisible(
												(edit && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null)
														|| Common.getApakahAdmin());
										hapus.setVisible(
												delete && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										cetak(pemesananPengadaanMasterAsset);
									}
								}
							});
				}

			});
			disetujui.setParent(toolbar);

			ditolak.setTooltiptext("Ditolak");

			ditolak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menolak pemesanan pengadaan barang/jasa ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();
										pemesananPengadaanMasterAsset.setDitolakOleh(Common.getCurrentUser());
										pemesananPengadaanMasterAsset
												.setTanggalDitolak(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, pemesananPengadaanMasterAsset);

										ditolakOleh.setValue(pemesananPengadaanMasterAsset.getDitolakOleh() == null ? ""
												: "Ditolak oleh "
														+ pemesananPengadaanMasterAsset.getDitolakOleh().getUserNama());
										tanggalDitolak
												.setValue(pemesananPengadaanMasterAsset.getTanggalDitolak() == null ? ""
														: Common.dateFormat3.get().format(
																pemesananPengadaanMasterAsset.getTanggalDitolak()));

										disetujuiTanggal.setValue(
												pemesananPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																pemesananPengadaanMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pemesananPengadaanMasterAsset.getDisetujuiOleh() == null
												? ""
												: pemesananPengadaanMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null
														&& pemesananPengadaanMasterAsset.getDitolakOleh() == null);
										ditolak.setVisible(
												reject && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null
														&& pemesananPengadaanMasterAsset.getDitolakOleh() == null);
										dibatalkan.setVisible(
												reject && (pemesananPengadaanMasterAsset.getDisetujuiOleh() != null
														|| pemesananPengadaanMasterAsset.getDitolakOleh() != null));
										rubah.setVisible(
												(edit && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null)
														|| Common.getApakahAdmin());
										hapus.setVisible(
												delete && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										cetak(pemesananPengadaanMasterAsset);

									}
								}
							});
				}

			});
			ditolak.setParent(toolbar);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan pengadaan barang/jasa ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										pemesananPengadaanMasterAsset.setDisetujuiOleh(null);
										pemesananPengadaanMasterAsset.setTanggalPersetujuan(null);
										pemesananPengadaanMasterAsset.setDitolakOleh(null);
										pemesananPengadaanMasterAsset.setTanggalDitolak(null);

										Common.refreshUpdate(session, pemesananPengadaanMasterAsset);

										disetujuiTanggal.setValue(
												pemesananPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																pemesananPengadaanMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pemesananPengadaanMasterAsset.getDisetujuiOleh() == null
												? ""
												: pemesananPengadaanMasterAsset.getDisetujuiOleh().getUserNama());

										ditolakOleh.setValue(pemesananPengadaanMasterAsset.getDitolakOleh() == null ? ""
												: "Ditolak oleh "
														+ pemesananPengadaanMasterAsset.getDitolakOleh().getUserNama());
										tanggalDitolak
												.setValue(pemesananPengadaanMasterAsset.getTanggalDitolak() == null ? ""
														: Common.dateFormat3.get().format(
																pemesananPengadaanMasterAsset.getTanggalDitolak()));

										disetujui.setVisible(
												approve && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null
														&& pemesananPengadaanMasterAsset.getDitolakOleh() == null);

										ditolak.setVisible(
												reject && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null
														&& pemesananPengadaanMasterAsset.getDitolakOleh() == null);
										dibatalkan.setVisible(
												reject && (pemesananPengadaanMasterAsset.getDisetujuiOleh() != null
														|| pemesananPengadaanMasterAsset.getDitolakOleh() != null));
										rubah.setVisible(
												(edit && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null)
														|| Common.getApakahAdmin());
										hapus.setVisible(
												delete && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			dibatalkan.setParent(toolbar);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(
					(edit && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null) || Common.getApakahAdmin());
			rubah.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					init(pemesananPengadaanMasterAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && pemesananPengadaanMasterAsset.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();

											if (SopUtil.hapusDisposisi(session,
													pemesananPengadaanMasterAsset.getDisposisiSop())) {

												List<PemesananPengadaanMasterAssetDetail> pemesananPengadaanMasterAssetDetails = session
														.createCriteria(PemesananPengadaanMasterAssetDetail.class)
														.add(Restrictions.eq("pemesananPengadaanMasterAsset",
																pemesananPengadaanMasterAsset))
														.list();
												for (PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail : pemesananPengadaanMasterAssetDetails) {
													session.delete(pemesananPengadaanMasterAssetDetail);
												}

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event event) throws Exception {
														Session session = HibernateUtil.currentSession();
														Common.refreshDelete(session, pemesananPengadaanMasterAsset);
														session.flush();

														onSearchDefault(event);
													}
												});
											}
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

										}

									}

								}
							});

				}
			});
			hapus.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	/**
	 * <b>Tujuan:</b> Membuka formulir tambah/ubah PO secara programatik dari modul lain
	 * (misalnya dari modul Perjanjian Kerjasama atau Pembelian Langsung), tanpa melalui
	 * halaman utama daftar PO. Memungkinkan integrasi antar-modul asset.
	 *
	 * <b>Cara kerja:</b> Membuat instance baru {@code PemesananPengadaanMasterAssetAction}
	 * secara manual (tanpa ZUL compose), mengonfigurasi field yang diperlukan (perjanjian kerjasama,
	 * flag pembelian langsung, event listener callback, dan window), kemudian membuat {@code MyWindow}
	 * baru sebagai container form, memanggil {@code init()} untuk membangun UI form, dan menampilkan
	 * window sebagai modal. Window dilekatkan ke root komponen halaman aktif.
	 *
	 * <b>Parameter:</b>
	 * @param eventListener           callback yang dipanggil setelah PO berhasil disimpan,
	 *                                digunakan pemanggil untuk refresh tampilan.
	 * @param pemesananPengadaanMasterAsset objek PO yang akan dibuka (bisa baru {@code new} atau
	 *                                existing untuk mode ubah). Jika ada perjanjian kerjasama,
	 *                                diambil dari objek ini.
	 * @param pembelianLangsung       {@code true} jika PO merupakan pembelian langsung (tanpa PR),
	 *                                mempengaruhi visibilitas beberapa field di form.
	 * @throws Exception jika terjadi kesalahan inisialisasi komponen atau akses data.
	 *
	 * <b>Penanganan error:</b> Exception dipropagasi ke pemanggil. Pastikan pemanggil
	 * menangani exception atau membiarkan ZKoss error handler memprosesnya.
	 *
	 * <b>Pemeliharaan:</b> Jika ukuran window perlu diubah, modifikasi {@code setHeight("95%")}
	 * dan {@code setWidth("90%")}. Pastikan window bersifat closable agar pengguna bisa menutup
	 * tanpa menyimpan.
	 */
	public static void onAddExternal(EventListener eventListener,
			PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset, Boolean pembelianLangsung) throws Exception {
		PemesananPengadaanMasterAssetAction pemesananPengadaanMasterAssetAction = new PemesananPengadaanMasterAssetAction();
		pemesananPengadaanMasterAssetAction.perjanjianKerjasamaMasterAssetData = pemesananPengadaanMasterAsset
				.getPerjanjianKerjasamaMasterAsset();
		pemesananPengadaanMasterAssetAction.pembelianLangsung = pembelianLangsung;
		pemesananPengadaanMasterAssetAction.eventListener = eventListener;
		pemesananPengadaanMasterAssetAction.addWindow = new MyWindow();
		pemesananPengadaanMasterAssetAction.tbmuser = Common.getCurrentUser();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(pemesananPengadaanMasterAssetAction.addWindow);
		pemesananPengadaanMasterAssetAction.addWindow.setHeight("95%");
		pemesananPengadaanMasterAssetAction.addWindow.setWidth("90%");

		pemesananPengadaanMasterAssetAction.init(pemesananPengadaanMasterAsset);

		pemesananPengadaanMasterAssetAction.addWindow.setVisible(true);
		pemesananPengadaanMasterAssetAction.addWindow.setClosable(true);
		pemesananPengadaanMasterAssetAction.addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Menangani klik tombol "Tambah" di toolbar halaman daftar PO. Membuka
	 * formulir tambah PO baru dalam mode modal dialog.
	 *
	 * <b>Cara kerja:</b> Membuat objek {@code PemesananPengadaanMasterAsset} baru (kosong)
	 * dan memanggil {@code init()} untuk membangun dan mengisi form. Kemudian menampilkan
	 * {@code addWindow} (yang sudah di-wire dari ZUL) sebagai modal dialog.
	 *
	 * <b>Parameter:</b>
	 * @param event event ZKoss dari klik tombol Tambah (biasanya onClick).
	 * @throws Exception jika terjadi kesalahan saat membangun form atau menampilkan window.
	 *
	 * <b>Penanganan error:</b> Exception dipropagasi ke ZKoss error handler.
	 *
	 * <b>Pemeliharaan:</b> Delegator sederhana ke {@code init()}; jika perlu pra-pengisian
	 * nilai default (misalnya tanggal hari ini), lakukan di konstruktor
	 * {@code PemesananPengadaanMasterAsset} atau di {@code form()}.
	 */
	public void onAdd(Event event) throws Exception {
		init(new PemesananPengadaanMasterAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Menyiapkan dan mengisi {@code addWindow} dengan form tambah/ubah PO,
	 * termasuk tombol Simpan dan Batal. Digunakan baik untuk mode tambah (objek baru) maupun
	 * mode ubah (objek existing).
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menyimpan referensi PO ke field instance dan mengatur judul window sesuai mode.</li>
	 *   <li>Membersihkan konten window sebelumnya via {@code Common.clear()}.</li>
	 *   <li>Membangun layout Borderlayout dengan Center (berisi form dari {@code form()})
	 *       dan South (berisi toolbar dengan tombol Batal dan Simpan).</li>
	 *   <li>Tombol Batal hanya menyembunyikan window tanpa menyimpan.</li>
	 *   <li>Tombol Simpan memanggil {@code onSave()}: jika berhasil, merefresh grid,
	 *       mereset paging, menyembunyikan window, dan memanggil {@code eventListener} callback
	 *       jika ada (untuk mode external).</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param pemesananPengadaanMasterAsset objek PO yang akan ditampilkan di form;
	 *        jika {@code id} null maka mode tambah, sebaliknya mode ubah.
	 * @throws Exception jika terjadi kesalahan saat membangun komponen form.
	 *
	 * <b>Penanganan error:</b> Exception dari {@code form()} atau {@code onSave()} akan
	 * dipropagasi ke ZKoss error handler.
	 *
	 * <b>Pemeliharaan:</b> Jika menambah tombol aksi baru di toolbar form (misalnya "Cetak Preview"),
	 * tambahkan ke toolbar South setelah tombol Simpan. Jangan mengubah urutan Clear → form → layout
	 * karena hal ini memastikan komponen lama dibersihkan sebelum yang baru dibuat.
	 */
	private void init(final PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset) throws Exception {

		this.pemesananPengadaanMasterAsset = pemesananPengadaanMasterAsset;
		addWindow.setTitle(pemesananPengadaanMasterAsset.getId() == null ? "Tambah Pemesanan Barang/Jasa" : "Ubah Pemesanan Barang/Jasa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(pemesananPengadaanMasterAsset, disposisiSop, save, null));

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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	/**
	 * <b>Tujuan:</b> Menghasilkan dan merender baris-baris detail barang/jasa (line item) di
	 * dalam {@code gridMasterAsset} pada form PO baru. Metode ini dipanggil setelah pengguna
	 * memilih permintaan pengadaan atau perjanjian kerjasama, untuk mengisi otomatis daftar
	 * barang yang akan dipesan.
	 *
	 * <b>Cara kerja:</b> Hanya berjalan jika PO belum tersimpan ({@code id == null}).
	 * <ul>
	 *   <li>Jika mode "dengan permintaan" ({@code !tampaPermintaan}): iterasi setiap
	 *       {@code PermintaanPengadaanMasterAssetDetail} yang dipilih; cek apakah sudah ada
	 *       {@code PemesananPengadaanMasterAssetDetail} untuk kode PO dan detail PR tersebut;
	 *       jika belum, buat objek baru dengan data dari PR (aset, jumlah sisa, harga beli,
	 *       keterangan) dan linkage balik ke PR detail. Tambahkan sebagai baris di grid.</li>
	 *   <li>Jika ada perjanjian kerjasama ({@code perjanjianKerjasamaMasterAssets != null}):
	 *       proses serupa untuk setiap {@code PerjanjianKerjasamaMasterAssetDetail}.</li>
	 *   <li>Setelah semua baris ditambahkan, panggil event listener hitung-ulang total
	 *       via {@code Common.createDefaultTimer()}.</li>
	 * </ul>
	 *
	 * <b>Penanganan error:</b> Setiap baris dirender dalam blok try-catch individual;
	 * kesalahan render ditampilkan via {@code Common.tampilErrorJikaAdmin()} tanpa menghentikan
	 * proses render baris lainnya.
	 *
	 * <b>Pemeliharaan:</b> Metode ini mengakses {@code kode.getValue()} untuk mencari detail
	 * yang sudah ada, sehingga kode PO harus sudah di-generate sebelum metode ini dipanggil.
	 * Jika logika pengisian jumlah berubah (misalnya tidak dikurangi jumlah yang sudah datang),
	 * perbarui baris {@code setJumlah(jumlah - jumlahDatang)}.
	 */
	public void generateDetail() {

		if (PemesananPengadaanMasterAssetAction.this.pemesananPengadaanMasterAsset.getId() == null) {

			Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
			rows.setParent(gridMasterAsset);

			if (!PemesananPengadaanMasterAssetAction.this.pemesananPengadaanMasterAsset.getTampaPermintaan()) {
				Common.clear(rows);

				if (PemesananPengadaanMasterAssetAction.this.permintaanPengadaanMasterAssets != null) {
					for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : PemesananPengadaanMasterAssetAction.this.permintaanPengadaanMasterAssets) {

						Session session = HibernateUtil.currentSession();
						PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail = (PemesananPengadaanMasterAssetDetail) session
								.createCriteria(PemesananPengadaanMasterAssetDetail.class)
								.createAlias("pemesananPengadaanMasterAsset", "pemesananPengadaanMasterAsset")
								.add(Restrictions.eq("pemesananPengadaanMasterAsset.kode", kode.getValue().trim()))
								.add(Restrictions.eq("permintaanPengadaanMasterAssetDetail",
										permintaanPengadaanMasterAssetDetail))
								.setMaxResults(1).uniqueResult();

						if (pemesananPengadaanMasterAssetDetail == null) {
							pemesananPengadaanMasterAssetDetail = new PemesananPengadaanMasterAssetDetail();
							pemesananPengadaanMasterAssetDetail
									.setMasterAsset(permintaanPengadaanMasterAssetDetail.getMasterAsset());
							pemesananPengadaanMasterAssetDetail
									.setJumlah(permintaanPengadaanMasterAssetDetail.getJumlah()
											- permintaanPengadaanMasterAssetDetail.getJumlahDatang());
							pemesananPengadaanMasterAssetDetail
									.setKeterangan(permintaanPengadaanMasterAssetDetail.getKeterangan());
							pemesananPengadaanMasterAssetDetail
									.setPemesananPengadaanMasterAsset(pemesananPengadaanMasterAsset);
							pemesananPengadaanMasterAssetDetail
									.setHargaBeli(permintaanPengadaanMasterAssetDetail.getHargaBeli());
							pemesananPengadaanMasterAssetDetail
									.setPermintaanPengadaanMasterAssetDetail(permintaanPengadaanMasterAssetDetail);
							permintaanPengadaanMasterAssetDetail
									.setPemesananPengadaanMasterAssetDetail(pemesananPengadaanMasterAssetDetail);
						}

						try {
							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);
							pemesananPengadaanMasterAssetHelper.initRow(row, pemesananPengadaanMasterAssetDetail);
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					}
				}
			}

			if (PemesananPengadaanMasterAssetAction.this.perjanjianKerjasamaMasterAssets != null) {
				for (PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail : PemesananPengadaanMasterAssetAction.this.perjanjianKerjasamaMasterAssets) {

					Session session = HibernateUtil.currentSession();
					PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail = (PemesananPengadaanMasterAssetDetail) session
							.createCriteria(PemesananPengadaanMasterAssetDetail.class).add(Restrictions
									.eq("perjanjianKerjasamaMasterAssetDetail", perjanjianKerjasamaMasterAssetDetail))
							.setMaxResults(1).uniqueResult();
					if (pemesananPengadaanMasterAssetDetail == null) {
						pemesananPengadaanMasterAssetDetail = new PemesananPengadaanMasterAssetDetail();
						pemesananPengadaanMasterAssetDetail
								.setMasterAsset(perjanjianKerjasamaMasterAssetDetail.getMasterAsset());
						pemesananPengadaanMasterAssetDetail.setJumlah(perjanjianKerjasamaMasterAssetDetail.getJumlah());
						pemesananPengadaanMasterAssetDetail
								.setKeterangan(perjanjianKerjasamaMasterAssetDetail.getKeterangan());
						pemesananPengadaanMasterAssetDetail
								.setPemesananPengadaanMasterAsset(pemesananPengadaanMasterAsset);
						pemesananPengadaanMasterAssetDetail
								.setHargaBeli(perjanjianKerjasamaMasterAssetDetail.getHargaBeli());
						pemesananPengadaanMasterAssetDetail
								.setPerjanjianKerjasamaMasterAssetDetail(perjanjianKerjasamaMasterAssetDetail);

						pemesananPengadaanMasterAssetDetail.setPermintaanPengadaanMasterAssetDetail(
								perjanjianKerjasamaMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail());
					}

					try {
						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);
						pemesananPengadaanMasterAssetHelper.initRow(row, pemesananPengadaanMasterAssetDetail);
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
			}

			Common.createDefaultTimer(pemesananPengadaanMasterAssetHelper.eventListenerHitungUlang);
		}
	}

	/**
	 * <b>Tujuan:</b> Memvalidasi dan menyimpan data PO (beserta seluruh rincian barang/jasa)
	 * ke database. Menangani baik mode tambah (INSERT) maupun mode ubah (UPDATE), termasuk
	 * penyimpanan linkage ke PR dan perjanjian kerjasama. Setelah simpan, men-trigger cetak PDF
	 * otomatis dan proses persetujuan jika checkbox "Setujui" dicentang.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Validasi input: anggaran wajib jika tanpa permintaan dan tidak tanpa anggaran;
	 *       kode PO, penyedia, jenis pemesanan, dan permintaan pengadaan wajib diisi;
	 *       setiap detail barang harus memiliki master aset; keterangan wajib diisi.</li>
	 *   <li>Jika mode ubah, ambil ulang entitas dari database via {@code session.get()} untuk
	 *       menghindari masalah dengan lazy proxy (ObjectNotFoundException).</li>
	 *   <li>Set semua field entitas PO dari komponen form (kode, keterangan, tanggal, satuan kerja,
	 *       penyedia, lokasi, pemilik, ruang, jenis pemesanan, termin, DP, catatan, dll.).</li>
	 *   <li>Hitung total nilai PO dari jumlah harga total semua baris detail.</li>
	 *   <li>Simpan PO header (save atau update), kemudian iterasi baris detail:
	 *       saveOrUpdate setiap detail, lalu update linkage ke PR detail dan PR header.</li>
	 *   <li>Setelah simpan selesai, via timer: jika checkbox "Setujui" dicentang, set
	 *       disetujuiOleh dan tanggal persetujuan; kemudian cetak PDF via {@code cetak()}.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param event event ZKoss dari klik tombol Simpan.
	 * @return {@code true} jika simpan berhasil dan form boleh ditutup;
	 *         {@code false} jika validasi gagal (form tetap terbuka, pesan error ditampilkan).
	 * @throws Exception jika terjadi kesalahan akses database atau komponen.
	 *
	 * <b>Penanganan error:</b> Validasi gagal menampilkan messagebox dan mengembalikan {@code false}.
	 * Exception database tidak ditangkap secara eksplisit (dipropagasi ke ZKoss); untuk race condition
	 * kode duplikat, {@code KodeUnikUtil.pastikanUnik()} sudah menangani konflik kode.
	 *
	 * <b>Pemeliharaan:</b> Jika menambah field baru di entitas PO, tambahkan
	 * {@code pemesananPengadaanMasterAsset.setXxx()} setelah baris-baris set field yang sudah ada.
	 * Perhatikan bahwa {@code kode} adalah Label (bukan input) karena kode di-generate otomatis;
	 * nilai diambil dari {@code kode.getValue()}.
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (workspace.getAttribute("workspace") == null && !tanpaAnggaran.isChecked() && tampaPermintaan.isChecked()) {
			MyMessageboxConfig.show("Mohon maaf, Anggaran/Workspace belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih Anggaran/Workspace dan pilih anggaran yang sesuai; (2) Jika tidak ada anggaran, centang opsi 'Tanpa Anggaran'; (3) Jika memakai permintaan pengadaan, centang opsi 'Berdasarkan Permintaan'. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Pemesanan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode Pemesanan atau gunakan tombol generate kode; (2) Pastikan kode bersifat unik dan belum digunakan pada pemesanan lain; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (penyediaAsset.getAttribute("penyediaAsset") == null) {
			MyMessageboxConfig.show("Mohon maaf, Penyedia belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih Penyedia dan cari penyedia dari daftar; (2) Jika penyedia belum terdaftar, tambahkan terlebih dahulu melalui menu Data Penyedia; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if ((jenisPemesananPengadaanAsset.getSelectedItem() == null ? null
				: jenisPemesananPengadaanAsset.getSelectedItem().getAttribute("value")) == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Pemesanan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih jenis pemesanan dari dropdown yang tersedia; (2) Pastikan jenis pemesanan sudah dikonfigurasi di menu Jenis Pemesanan Pengadaan; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (!tampaPermintaan.isChecked()) {
			if (PemesananPengadaanMasterAssetAction.this.permintaanPengadaanMasterAssets.isEmpty()) {
				MyMessageboxConfig.show("Mohon maaf, Permintaan Pengadaan barang/jasa belum dipilih. Langkah yang dapat dilakukan: (1) Pilih permintaan pengadaan dari daftar yang tersedia; (2) Jika belum ada permintaan, buat terlebih dahulu melalui menu Permintaan Pengadaan; (3) Jika pemesanan tidak membutuhkan permintaan, centang opsi 'Tanpa Permintaan'. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}
		PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail = null;
		List<Row> rowsMasterAsset = gridMasterAsset.getRows().getChildren();
		for (Row row : rowsMasterAsset) {
			pemesananPengadaanMasterAssetDetail = (PemesananPengadaanMasterAssetDetail) row
					.getAttribute("pemesananPengadaanMasterAssetDetail");
			if (pemesananPengadaanMasterAssetDetail.getMasterAsset() == null) {
				MyMessageboxConfig.show("Mohon maaf, Data Barang pada daftar pemesanan belum lengkap. Langkah yang dapat dilakukan: (1) Klik tombol pilih barang pada baris yang masih kosong; (2) Cari dan pilih barang/jasa dari daftar master aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		if (keterangan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Keterangan Pemesanan Pengadaan barang/jasa belum diisi. Langkah yang dapat dilakukan: (1) Isi field Keterangan dengan deskripsi tujuan pemesanan; (2) Keterangan diperlukan untuk keperluan persetujuan dan dokumentasi; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							keterangan.focus();
						}
					});
			return false;
		}

//		if (pemesananPengadaanMasterAssetDetail == null) {
//			MyMessageboxConfig.show("Barang / Jasa harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.EXCLAMATION);
//			return false;
//		}

		Session session = HibernateUtil.currentSession();

		try {
			if (pemesananPengadaanMasterAsset.getId() != null) {
				// Pakai get() (bukan load()): load() menghasilkan lazy proxy yang baru gagal saat
				// di-inisialisasi nanti (setTampaPermintaan) bila row sudah dihapus →
				// ObjectNotFoundException di luar try. get() langsung kena DB, null bila tak ada.
				PemesananPengadaanMasterAsset existing = (PemesananPengadaanMasterAsset) session
						.get(PemesananPengadaanMasterAsset.class, pemesananPengadaanMasterAsset.getId());
				pemesananPengadaanMasterAsset = existing != null ? existing
						: new PemesananPengadaanMasterAsset();
			}
		} catch (Exception e) {
			pemesananPengadaanMasterAsset = new PemesananPengadaanMasterAsset();
		}

		if (disposisiSop != null && disposisiSop.getId() != null) {
			pemesananPengadaanMasterAsset.setDisposisiSop(disposisiSop);
		}
		pemesananPengadaanMasterAsset.setTampaPermintaan(tampaPermintaan.isChecked());

		pemesananPengadaanMasterAsset.setKode(kode.getValue());
		pemesananPengadaanMasterAsset.setKeterangan(keterangan.getValue());
		pemesananPengadaanMasterAsset.setTanggalPembuatan(tanggalPembuatan.getValue());

		String s = "";
		String a = "";
		for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : PemesananPengadaanMasterAssetAction.this.permintaanPengadaanMasterAssets) {
			s += s.isEmpty() ? permintaanPengadaanMasterAssetDetail.getId().toString()
					: "," + permintaanPengadaanMasterAssetDetail.getId();

			if (permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getWorkspace() != null) {
				a += a.isEmpty()
						? permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getWorkspace()
								.getId().toString()
						: "," + permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getWorkspace()
								.getId();
			}
		}
		pemesananPengadaanMasterAsset.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		pemesananPengadaanMasterAsset.setPermintaanPengadaanMasterAssets(s);
		pemesananPengadaanMasterAsset.setAngarans(a);
		pemesananPengadaanMasterAsset.setPenyedia((PenyediaAsset) penyediaAsset.getAttribute("penyediaAsset"));

		pemesananPengadaanMasterAsset.setLokasi(
				(Lokasi) (lokasi.getSelectedItem() == null || lokasi.getSelectedItem().getValue() == null ? null
						: lokasi.getSelectedItem().getValue()));
		pemesananPengadaanMasterAsset.setPemilikAsset((PemilikAsset) (pemilikAsset.getSelectedItem() == null
				|| pemilikAsset.getSelectedItem().getValue() == null ? null
						: pemilikAsset.getSelectedItem().getValue()));
		pemesananPengadaanMasterAsset.setRuang((Ruang) ruang.getAttribute("ruang"));

		pemesananPengadaanMasterAsset.setJenisPemesananPengadaanAsset(
				(JenisPemesananPengadaanAsset) (jenisPemesananPengadaanAsset.getSelectedItem() == null ? null
						: jenisPemesananPengadaanAsset.getSelectedItem().getAttribute("value")));

		pemesananPengadaanMasterAsset.setByTermin(byTermin.isChecked());

		pemesananPengadaanMasterAsset.setPengirimanPalingLambat(pengirimanPalingLambat.getValue());

		pemesananPengadaanMasterAsset.setDp(dp.getValue());
		pemesananPengadaanMasterAsset.setCatatanKesepakatan(catatanKesepakatan.getValue());

		pemesananPengadaanMasterAsset.setKodeInvoice(kodeInvoice.getValue().trim());

		pemesananPengadaanMasterAsset.setTanggalMulai(tanggalMulai.getValue());
		pemesananPengadaanMasterAsset.setTanggalSampai(tanggalSampai.getValue());

		Workspace work = (Workspace) workspace.getAttribute("workspace");

		pemesananPengadaanMasterAsset.setWorkspace(work);
		pemesananPengadaanMasterAsset.setTanpaAnggaran(tanpaAnggaran.isChecked());

		pemesananPengadaanMasterAsset.setPerjanjianKerjasamaMasterAsset(
				(PerjanjianKerjasamaMasterAsset) (perjanjianKerjasamaMasterAsset.getSelectedItem() == null ? null
						: perjanjianKerjasamaMasterAsset.getSelectedItem().getValue()));
		Double jumlah = 0.0;
		for (Row row : rowsMasterAsset) {
			pemesananPengadaanMasterAssetDetail = (PemesananPengadaanMasterAssetDetail) row
					.getAttribute("pemesananPengadaanMasterAssetDetail");
			Double j = pemesananPengadaanMasterAssetDetail.getHargaTotal();
			jumlah += j;
		}

		pemesananPengadaanMasterAsset.setNilai(jumlah);
		pemesananPengadaanMasterAsset.setFormula(array.toString());
		pemesananPengadaanMasterAsset.setPembelianLangsung(pembelianLangsung);
		pemesananPengadaanMasterAsset
				.setJenisPajakPpnDp((JenisPajakPpn) (jenisPajakPpnDp.getSelectedItem() == null ? null
						: jenisPajakPpnDp.getSelectedItem().getValue()));

		if (perjanjianKerjasamaMasterAssetData != null) {
			pemesananPengadaanMasterAsset.setPerjanjianKerjasamaMasterAsset(perjanjianKerjasamaMasterAssetData);
		}

		if (pemesananPengadaanMasterAsset.getId() != null) {
			session.update(pemesananPengadaanMasterAsset);
		} else {
			pemesananPengadaanMasterAsset.setDibuatOleh(Common.getCurrentUser());
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			pemesananPengadaanMasterAsset.setKode(kode.getValue());
			session.save(pemesananPengadaanMasterAsset);

		}

		for (Row row : rowsMasterAsset) {
			pemesananPengadaanMasterAssetDetail = (PemesananPengadaanMasterAssetDetail) row
					.getAttribute("pemesananPengadaanMasterAssetDetail");
			pemesananPengadaanMasterAssetDetail.setPemesananPengadaanMasterAsset(pemesananPengadaanMasterAsset);
			// FIX NonUniqueObjectException (PemesananPengadaanMasterAssetDetail#id): baris grid dapat
			// memegang instance DETACHED yang ber-id sama dengan instance PemesananPengadaanMasterAssetDetail
			// lain yang SUDAH ter-load (managed) di session ini — mis. akibat graf entitas / cascade yang
			// menginisialisasi koleksi detail saat tampil SOP. saveOrUpdate atas instance detached lalu
			// memicu "a different object with the same identifier value was already associated with the
			// session". Buang (evict) instance managed yang bentrok agar instance baris grid (yang memuat
			// nilai editan pengguna) dapat di-saveOrUpdate secara BERSIH — sekaligus MEMPERTAHANKAN jalur
			// audit saveOrUpdate (SaveOrUpdateAuditEventListener) & nilai editan, tanpa menyalin field
			// satu per satu (lih. pola serupa untuk PermintaanPengadaanMasterAssetDetail di bawah).
			if (pemesananPengadaanMasterAssetDetail.getId() != null) {
				Object detailBentrok = session.get(PemesananPengadaanMasterAssetDetail.class,
						pemesananPengadaanMasterAssetDetail.getId());
				if (detailBentrok != null && detailBentrok != pemesananPengadaanMasterAssetDetail) {
					session.evict(detailBentrok);
				}
			}
			session.saveOrUpdate(pemesananPengadaanMasterAssetDetail);
			session.flush();

			PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail = pemesananPengadaanMasterAssetDetail
					.getPermintaanPengadaanMasterAssetDetail();
			if (permintaanPengadaanMasterAssetDetail != null) {
				// Ambil instance yang SUDAH dikelola session lewat get(id) alih-alih refresh+saveOrUpdate
				// pada objek dari graf detail. Objek dari graf bisa berupa instance DETACHED yang
				// ber-id sama dgn instance lain yang sudah ter-load di session -> saveOrUpdate memicu
				// NonUniqueObjectException "a different object with the same identifier value was already
				// associated with the session". get() mengembalikan instance managed yang sama sehingga
				// aman disetel & di-flush.
				if (permintaanPengadaanMasterAssetDetail.getId() != null) {
					PermintaanPengadaanMasterAssetDetail managedDetail = (PermintaanPengadaanMasterAssetDetail) session
							.get(PermintaanPengadaanMasterAssetDetail.class,
									permintaanPengadaanMasterAssetDetail.getId());
					if (managedDetail != null) {
						permintaanPengadaanMasterAssetDetail = managedDetail;
					}
				}
				permintaanPengadaanMasterAssetDetail
						.setPemesananPengadaanMasterAssetDetail(pemesananPengadaanMasterAssetDetail);
				session.saveOrUpdate(permintaanPengadaanMasterAssetDetail);
				session.flush();

				if (permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset() != null
						&& permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
								.getPemesananPengadaanMasterAsset() == null) {
					PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset = permintaanPengadaanMasterAssetDetail
							.getPermintaanPengadaanMasterAsset();
					if (permintaanPengadaanMasterAsset.getId() != null) {
						PermintaanPengadaanMasterAsset managedHeader = (PermintaanPengadaanMasterAsset) session
								.get(PermintaanPengadaanMasterAsset.class, permintaanPengadaanMasterAsset.getId());
						if (managedHeader != null) {
							permintaanPengadaanMasterAsset = managedHeader;
						}
					}
					permintaanPengadaanMasterAsset.setPemesananPengadaanMasterAsset(pemesananPengadaanMasterAsset);
					session.saveOrUpdate(permintaanPengadaanMasterAsset);
					session.flush();
				}
			}

		}

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {

				if (setujui.isChecked()) {
					Session session = HibernateUtil.currentSession();
					pemesananPengadaanMasterAsset.setDisetujuiOleh(tbmuser);
					pemesananPengadaanMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
					Common.refreshUpdate(session, pemesananPengadaanMasterAsset);
				} else {
					Session session = HibernateUtil.currentSession();
					pemesananPengadaanMasterAsset.setDisetujuiOleh(null);
					pemesananPengadaanMasterAsset.setTanggalPersetujuan(null);
					Common.refreshUpdate(session, pemesananPengadaanMasterAsset);
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						cetak(pemesananPengadaanMasterAsset);
					}
				}, "Proses cetak", false, 2500);

			}
		});

		return true;
	}

	/**
	 * <b>Tujuan:</b> Mengekstrak data satu entri termin/progress dari objek {@code JSONObject}
	 * dan memasukkannya ke dalam {@code Map} terstruktur yang siap digunakan oleh laporan JasperReports.
	 * Metode ini memproses satu termin saja (berdasarkan index {@code i}).
	 *
	 * <b>Cara kerja:</b> Memeriksa apakah {@code jsonObject} memiliki field "key" yang valid
	 * (jika tidak, termin dianggap telah dihapus dan method mengembalikan {@code null}).
	 * Kemudian mengekstrak semua field: nama, nomor, pekerjaan (persen progress), penagihan (nilai DPP),
	 * pinalti, tanggalD (tanggal tagihan), setuju (status persetujuan termin), ppn (persen PPN),
	 * pajak (referensi ke {@code JenisPajakBarang}), dan daftar dokumen pendukung termin.
	 * Semua nilai dimasukkan ke {@code map} (untuk sub-laporan) dan ke {@code mapData} dengan
	 * prefix {@code "termin_i."} (untuk parameter laporan utama).
	 *
	 * <b>Parameter:</b>
	 * @param mapData    map parameter laporan utama tempat data termin ditambahkan dengan prefix.
	 * @param i          index termin dalam array JSON (digunakan sebagai bagian dari key parameter).
	 * @param jsonObject objek JSON satu entri termin dari field {@code formula} PO.
	 * @return {@code Map} berisi data termin yang sudah diformat, atau {@code null} jika termin
	 *         tidak valid (tidak punya field "key").
	 * @throws Exception jika terjadi kesalahan parsing tanggal atau akses ConstantValues.
	 *
	 * <b>Penanganan error:</b> Semua field JSON diakses defensif dengan {@code isNull()} check
	 * sebelum {@code get()}, sehingga field yang hilang menghasilkan nilai default (0.0, "", null, false).
	 *
	 * <b>Pemeliharaan:</b> Jika menambah field baru di JSON termin (misalnya "diskon"), tambahkan
	 * ekstraksi di sini DAN di {@code paramTermin()} agar konsisten. Juga perbarui laporan
	 * JasperReports yang menggunakan parameter ini.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map mapTermin(Map mapData, int i, JSONObject jsonObject) throws Exception {
		if (jsonObject.isNull("key")) {
			return null;
		}
		Map map = new HashMap();
		Iterator<String> iter = jsonObject.keys();
		while (iter.hasNext()) {
			String key = iter.next();
			map.put(key, jsonObject.get(key));

			mapData.put("termin_" + i + "." + key, jsonObject.get(key));
		}

		String nama = "";

		if (!jsonObject.isNull("nama")) {
			nama = jsonObject.get("nama") + "";
		}

		map.put("nama", nama);

		String nomor = "";

		if (!jsonObject.isNull("nomor")) {
			nomor = jsonObject.get("nomor") + "";
		}

		map.put("nomor", nomor);

		Double pekerjaan = 0.0;
		if (!jsonObject.isNull("pekerjaan")) {
			pekerjaan = jsonObject.getDouble("pekerjaan");
		}

		map.put("pekerjaan", pekerjaan);
		map.put("pekerjaan_f", Common.numberFormat.get().format(pekerjaan));

		Double penagihan = 0.0;
		if (!jsonObject.isNull("penagihan")) {
			penagihan = jsonObject.getDouble("penagihan");
		}

		map.put("penagihan", penagihan);
		map.put("penagihan_f", Common.numberFormat.get().format(penagihan));

		Double pinalti = 0.0;
		if (!jsonObject.isNull("pinalti")) {
			pinalti = jsonObject.getDouble("pinalti");
		}

		map.put("pinalti", pinalti);
		map.put("pinalti_f", Common.numberFormat.get().format(pinalti));

		Date tanggalD = null;

		if (!jsonObject.isNull("tanggalD")) {
			tanggalD = jsonObject.get("tanggalD").toString().isEmpty() ? null
					: Common.dateFormat1.get().parse(jsonObject.get("tanggalD") + "");
		}

		map.put("tanggalD", tanggalD);

		Boolean setuju;
		if (!jsonObject.isNull("setuju")) {
			setuju = Boolean.parseBoolean(jsonObject.get("setuju") + "");
		} else {
			setuju = false;
		}

		map.put("setuju", setuju);

		Double ppn = 0.0;
		if (!jsonObject.isNull("ppn")) {
			ppn = jsonObject.getDouble("ppn");
		}
		map.put("ppn", ppn);

		JenisPajakBarang jenisPajakBarang;
		if (!jsonObject.isNull("pajak")) {
			jenisPajakBarang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
					Long.parseLong(jsonObject.get("pajak") + ""));
		} else {
			jenisPajakBarang = null;
		}
		if (jenisPajakBarang != null) {
			Common.insertProperty(JenisPajakBarang.class, jenisPajakBarang, map, "pajak");
		}

		JSONArray dokumens;
		if (!jsonObject.isNull("dokumens")) {
			dokumens = jsonObject.getJSONArray("dokumens");
		} else {
			dokumens = new JSONArray();
			jsonObject.put("dokumens", dokumens);
		}

		for (int ia = 0; ia < dokumens.length(); ia++) {

			JSONObject jsonDokumen = dokumens.getJSONObject(ia);

			if (jsonDokumen.isNull("keyDok")) {
				continue;
			}
			String nama_file = "";

			if (!jsonDokumen.isNull("nama_file")) {
				nama_file = jsonDokumen.get("nama_file") + "";
			}

			String link = "";

			if (!jsonDokumen.isNull("link")) {
				link = jsonDokumen.get("link") + "";
			}

			map.put("dokumen_nama_file_" + ia, nama_file);
			map.put("dokumen_link_" + ia, link);
		}
		return map;
	}

	/**
	 * <b>Tujuan:</b> Delegator ke {@code paramTermin(mapData, parameters, pemesananPengadaanMasterAsset, null)}
	 * tanpa filter key, sehingga memproses semua termin yang ada di PO.
	 *
	 * <b>Cara kerja:</b> Memanggil overload {@code paramTermin} dengan parameter {@code keData = null},
	 * yang berarti semua entri termin dalam field {@code formula} PO akan diproses dan dimasukkan
	 * ke {@code parameters} sebagai list {@code mapsTermin} dan parameter total penagihan/pinalti/pekerjaan.
	 *
	 * <b>Parameter:</b>
	 * @param mapData    map parameter laporan utama.
	 * @param parameters map parameter laporan yang akan diisi dengan data termin.
	 * @param pemesananPengadaanMasterAsset PO yang datanya akan diproses.
	 * @throws Exception      jika terjadi kesalahan akses JSON atau database.
	 * @throws ParseException jika format tanggal dalam JSON tidak valid.
	 *
	 * <b>Pemeliharaan:</b> Delegator sederhana; perubahan logika harus dilakukan di overload
	 * {@code paramTermin} yang menerima parameter {@code keData}.
	 */
	@SuppressWarnings("rawtypes")
	public static void paramTermin(Map mapData, Map parameters,
			PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset) throws Exception, ParseException {
		paramTermin(mapData, parameters, pemesananPengadaanMasterAsset, null);
	}

	/**
	 * <b>Tujuan:</b> Memproses seluruh (atau sebagian) data termin dari field {@code formula}
	 * (JSONArray) milik PO dan mengisi {@code parameters} dengan list termin terformat
	 * ({@code mapsTermin}) serta total akumulasi nilai penagihan, pinalti, dan pekerjaan.
	 * Digunakan sebagai persiapan parameter sebelum generasi laporan JasperReports.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Parse {@code formula} PO sebagai {@code JSONArray}.</li>
	 *   <li>Jika parameter {@code keData} tidak null/kosong, hanya proses termin dengan
	 *       field "key" yang cocok (filter per-termin untuk laporan individual termin).</li>
	 *   <li>Untuk setiap termin yang lolos filter: ekstrak semua field (nama, nomor, pekerjaan,
	 *       penagihan, pinalti, tanggal, ppn, pajak, dokumen), format angka dengan
	 *       {@code Common.numberFormat}, dan tambahkan ke list {@code mapsTermin}.</li>
	 *   <li>Akumulasi total penagihan, pinalti, dan pekerjaan, kemudian masukkan ke {@code mapData}
	 *       dan {@code parameters} beserta versi terformat ({@code _f}).</li>
	 *   <li>Masukkan list {@code mapsTermin} ke {@code parameters}.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param mapData    map parameter laporan utama yang diisi dengan total dan data flat termin.
	 * @param parameters map yang menerima list {@code mapsTermin} untuk sub-laporan.
	 * @param pemesananPengadaanMasterAsset PO sumber data termin.
	 * @param keData     key termin yang difilter (jika null atau kosong, proses semua termin).
	 * @throws Exception      jika terjadi kesalahan akses JSON atau ConstantValues.
	 * @throws ParseException jika format tanggal dalam JSON tidak valid.
	 *
	 * <b>Penanganan error:</b> Field JSON diakses defensif dengan {@code isNull()} check;
	 * field "pajak" menggunakan {@code ConstantValues.ambil()} yang mengembalikan null jika
	 * id tidak ditemukan (aman karena ada null-check setelahnya).
	 *
	 * <b>Pemeliharaan:</b> Metode ini digunakan oleh {@code parameter()} untuk laporan cetak
	 * dan bisa digunakan oleh modul lain yang perlu data termin PO dalam format Map.
	 * Jika menambah field termin baru, ekstraksi harus ditambahkan di sini dan di {@code mapTermin()}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void paramTermin(Map mapData, Map parameters,
			PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset, String keData)
			throws Exception, ParseException {
		JSONArray arraya = new JSONArray(pemesananPengadaanMasterAsset.getFormula());

		Double totalpenagihan = 0.0;
		Double totalpinalti = 0.0;
		Double totalpekerjaan = 0.0;

		List<Map> mapsTermin = (List<Map>) parameters.get("mapsTermin");
		if (mapsTermin == null) {
			mapsTermin = new ArrayList<Map>();
		}
		for (int i = 0; i < arraya.length(); i++) {
			JSONObject jsonObject = arraya.getJSONObject(i);
			if (jsonObject.isNull("key")) {
				continue;
			}

			if (keData == null || keData.trim().isEmpty() || (jsonObject.get("key") + "").equalsIgnoreCase(keData)) {

				Map map = new HashMap();
				Iterator<String> iter = jsonObject.keys();
				while (iter.hasNext()) {
					String key = iter.next();
					map.put(key, jsonObject.get(key));

					mapData.put("termin_" + i + "." + key, jsonObject.get(key));
				}

				String nama = "";

				if (!jsonObject.isNull("nama")) {
					nama = jsonObject.get("nama") + "";
				}

				map.put("nama", nama);

				String nomor = "";

				if (!jsonObject.isNull("nomor")) {
					nomor = jsonObject.get("nomor") + "";
				}

				map.put("nomor", nomor);

				Double pekerjaan = 0.0;
				if (!jsonObject.isNull("pekerjaan")) {
					pekerjaan = jsonObject.getDouble("pekerjaan");
				}

				totalpekerjaan += pekerjaan;

				map.put("pekerjaan", pekerjaan);
				map.put("pekerjaan_f", Common.numberFormat.get().format(pekerjaan));

				Double penagihan = 0.0;
				if (!jsonObject.isNull("penagihan")) {
					penagihan = jsonObject.getDouble("penagihan");
				}

				map.put("penagihan", penagihan);
				map.put("penagihan_f", Common.numberFormat.get().format(penagihan));

				totalpenagihan += penagihan;

				Double pinalti = 0.0;
				if (!jsonObject.isNull("pinalti")) {
					pinalti = jsonObject.getDouble("pinalti");
				}

				totalpinalti += pinalti;

				map.put("pinalti", pinalti);
				map.put("pinalti_f", Common.numberFormat.get().format(pinalti));

				Date tanggalD = null;

				if (!jsonObject.isNull("tanggalD")) {
					tanggalD = jsonObject.get("tanggalD").toString().isEmpty() ? null
							: Common.dateFormat1.get().parse(jsonObject.get("tanggalD") + "");
				}

				map.put("tanggalD", tanggalD);

				Boolean setuju;
				if (!jsonObject.isNull("setuju")) {
					setuju = Boolean.parseBoolean(jsonObject.get("setuju") + "");
				} else {
					setuju = false;
				}

				map.put("setuju", setuju);

				Double ppn = 0.0;
				if (!jsonObject.isNull("ppn")) {
					ppn = jsonObject.getDouble("ppn");
				}
				map.put("ppn", ppn);

				JenisPajakBarang jenisPajakBarang;
				if (!jsonObject.isNull("pajak")) {
					jenisPajakBarang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
							Long.parseLong(jsonObject.get("pajak") + ""));
				} else {
					jenisPajakBarang = null;
				}
				if (jenisPajakBarang != null) {
					Common.insertProperty(JenisPajakBarang.class, jenisPajakBarang, map, "pajak");
				}

				JSONArray dokumens;
				if (!jsonObject.isNull("dokumens")) {
					dokumens = jsonObject.getJSONArray("dokumens");
				} else {
					dokumens = new JSONArray();
					jsonObject.put("dokumens", dokumens);
				}

				for (int ia = 0; ia < dokumens.length(); ia++) {

					JSONObject jsonDokumen = dokumens.getJSONObject(ia);

					if (jsonDokumen.isNull("keyDok")) {
						continue;
					}
					String nama_file = "";

					if (!jsonDokumen.isNull("nama_file")) {
						nama_file = jsonDokumen.get("nama_file") + "";
					}

					String link = "";

					if (!jsonDokumen.isNull("link")) {
						link = jsonDokumen.get("link") + "";
					}

					map.put("dokumen_nama_file_" + ia, nama_file);
					map.put("dokumen_link_" + ia, link);
				}

				mapsTermin.add(map);
			}
		}

		mapData.put("totalpenagihan", totalpenagihan);
		mapData.put("totalpinalti", totalpinalti);
		mapData.put("totalpekerjaan", totalpekerjaan);

		mapData.put("totalpenagihan_f", Common.numberFormat.get().format(totalpenagihan));
		mapData.put("totalpinalti_f", Common.numberFormat.get().format(totalpinalti));
		mapData.put("totalpekerjaan_f", Common.numberFormat.get().format(totalpekerjaan));

		parameters.put("mapsTermin", mapsTermin);
	}

	/**
	 * <b>Tujuan:</b> Menyiapkan map parameter lengkap yang diperlukan oleh template laporan
	 * JasperReports untuk mencetak dokumen PO (SPK/Surat Pemesanan). Parameter mencakup semua
	 * informasi header PO, data penyedia, kop surat satuan kerja, detail barang, data termin,
	 * status persetujuan, dan dokumen lampiran penyedia.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Refresh entitas PO dari database (tangkap exception jika sudah dihapus).</li>
	 *   <li>Buat map parameter acak via {@code HashMapGenerator.getRand()}.</li>
	 *   <li>Proses data termin via {@code paramTermin()} dan hitung total penagihan/pinalti/pekerjaan.</li>
	 *   <li>Jika ada perjanjian kerjasama, masukkan propertinya ke parameter dengan prefix "kerjasama".</li>
	 *   <li>Inisialisasi kop surat dari satuan kerja via {@code SuratUtil.initDefaultKop()}.</li>
	 *   <li>Masukkan semua properti entitas PO via {@code Common.insertProperty()} dengan prefix "data".</li>
	 *   <li>Proses data bank penyedia dari field JSON {@code bank} (array).</li>
	 *   <li>Cek dokumen penyedia ({@code PenyediaAssetPunyaDokumen}) dan masukkan ke parameter.</li>
	 *   <li>Proses parameter alur SOP via {@code DisposisiAlurSop.parameterMap()}.</li>
	 *   <li>Query detail barang PO dari database (diurutkan berdasarkan jenis dan kelompok aset),
	 *       bangun list map detail dengan semua field laporan, dan hitung total semua.</li>
	 *   <li>Nullkan semua field yang mengandung "disposisiSop" untuk menghindari serialisasi berlebihan.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param pemesananPengadaanMasterAsset PO yang akan dicetak; tidak boleh null.
	 * @return {@code Map} parameter siap pakai untuk JasperReports.
	 * @throws Exception jika terjadi kesalahan akses database atau JSON.
	 *
	 * <b>Penanganan error:</b> Refresh entitas ditangkap untuk mencegah crash jika entitas sudah
	 * dihapus. Parse JSON bank penyedia ditangkap agar cetak tidak gagal jika format JSON salah.
	 *
	 * <b>Pemeliharaan:</b> Urutan pemuatan parameter penting: termin harus diproses sebelum
	 * {@code Common.insertProperty()} agar tidak tertimpa. Jika menambah field baru ke laporan,
	 * tambahkan di sini dengan key yang sesuai dengan placeholder di template JasperReports.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map parameter(PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset) throws Exception {
		if (pemesananPengadaanMasterAsset != null && pemesananPengadaanMasterAsset.getId() != null) {
			try {
				HibernateUtil.currentSession().refresh(pemesananPengadaanMasterAsset);
			} catch (Exception eRefresh) {
				// Row mungkin sudah dihapus (UnresolvableObjectException) → lewati refresh,
				// pakai data objek yang ada agar cetak tidak crash.
				ais.common.Common.tampilErrorJikaAdmin(eRefresh);
			}
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", pemesananPengadaanMasterAsset.getId());

		PemesananPengadaanMasterAssetAction.paramTermin(parameters, parameters, pemesananPengadaanMasterAsset);

		List<Map> mapsTermin = (List<Map>) parameters.get("mapsTermin");
		if (mapsTermin == null) {
			mapsTermin = new ArrayList<Map>();
		}
		Double totalpenagihan = 0.0;
		Double totalpinalti = 0.0;
		Double totalpekerjaan = 0.0;
		for (Map m : mapsTermin) {
			Double pekerjaan = (Double) (m.get("pekerjaan") == null ? 0.0 : m.get("pekerjaan"));
			Double penagihan = (Double) (m.get("penagihan") == null ? 0.0 : m.get("penagihan"));
			Double pinalti = (Double) (m.get("pinalti") == null ? 0.0 : m.get("pinalti"));

			totalpenagihan += penagihan;
			totalpinalti += pinalti;
			totalpekerjaan += pekerjaan;
		}

		parameters.put("totalpenagihan", totalpenagihan);
		parameters.put("totalpinalti", totalpinalti);
		parameters.put("totalpekerjaan", totalpekerjaan);

		if (pemesananPengadaanMasterAsset.getPerjanjianKerjasamaMasterAsset() != null) {
			Common.insertProperty(PerjanjianKerjasamaMasterAsset.class,
					pemesananPengadaanMasterAsset.getPerjanjianKerjasamaMasterAsset(), parameters, "kerjasama");
		}

		SatuanKerja satuanKerja = pemesananPengadaanMasterAsset.getSatuanKerja();
		SuratUtil.initDefaultKop(parameters, satuanKerja);

		Common.insertProperty(PemesananPengadaanMasterAsset.class, pemesananPengadaanMasterAsset, parameters, "data");

		if (pemesananPengadaanMasterAsset.getPenyedia() != null) {

			try {
				JSONArray array = new JSONArray(pemesananPengadaanMasterAsset.getPenyedia().getBank());
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);
					Iterator<String> iter = jsonObject.keys();
					while (iter.hasNext()) {
						String key = iter.next();
						parameters.put("bank_" + i + "." + key, jsonObject.get(key));
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PemesananPengadaanMasterAssetAction.java:2051");
				// TODO: handle exception
			}

			Map<Long, DokumenPenyediaAsset> map = ConstantValues.ambilBerdasarClass(DokumenPenyediaAsset.class);
			List<DokumenPenyediaAsset> dokumenPenyediaAssets = new ArrayList<DokumenPenyediaAsset>();
			for (DokumenPenyediaAsset dokumenPenyediaAsset : map.values()) {
				dokumenPenyediaAssets.add(dokumenPenyediaAsset);
			}
			PenyediaAsset penyediaAsset = pemesananPengadaanMasterAsset.getPenyedia();
			Session session = HibernateUtil.currentSession();
			for (DokumenPenyediaAsset dokumenPenyediaAsset : dokumenPenyediaAssets) {

				PenyediaAssetPunyaDokumen temp = (PenyediaAssetPunyaDokumen) (penyediaAsset == null
						|| penyediaAsset.getId() == null
								? new PenyediaAssetPunyaDokumen()
								: session.createCriteria(PenyediaAssetPunyaDokumen.class)
										.add(Restrictions.eq("dokumenPenyediaAsset", dokumenPenyediaAsset))
										.add(Restrictions.eq("penyediaAsset", penyediaAsset)).setMaxResults(1)
										.uniqueResult());

				parameters.put("dokumen." + dokumenPenyediaAsset.getNama(), temp == null ? "" : temp.getKeterangan());
			}

		}

		DisposisiAlurSop.parameterMap(pemesananPengadaanMasterAsset.getDisposisiSop(), parameters);

		parameters.put("termin", pemesananPengadaanMasterAsset.getByTermin());

		Session session = HibernateUtil.currentSession();
		List<PemesananPengadaanMasterAssetDetail> pemesananPengadaanMasterAssetDetails = session
				.createCriteria(PemesananPengadaanMasterAssetDetail.class).createAlias("masterAsset", "masterAsset")
				.addOrder(Order.asc("masterAsset.jenisAsset")).addOrder(Order.asc("masterAsset.kelompokAsset"))
				.add(Restrictions.eq("pemesananPengadaanMasterAsset", pemesananPengadaanMasterAsset)).list();
		List<Map> maps = new ArrayList<Map>();
		Double totalSemua = 0.0;
		for (PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail : pemesananPengadaanMasterAssetDetails) {
			Map map = new HashMap();
			Common.insertProperty(PemesananPengadaanMasterAssetDetail.class, pemesananPengadaanMasterAssetDetail, map,
					"data");

			totalSemua += pemesananPengadaanMasterAssetDetail.getHargaTotal();

			map.put("kelompok_asset",
					pemesananPengadaanMasterAssetDetail.getMasterAsset().getKelompokAsset() == null ? ""
							: pemesananPengadaanMasterAssetDetail.getMasterAsset().getKelompokAsset().getNama());

			map.put("jenis_asset", pemesananPengadaanMasterAssetDetail.getMasterAsset().getJenisAsset() == null ? ""
					: pemesananPengadaanMasterAssetDetail.getMasterAsset().getJenisAsset().getNama());

			map.put("tipe_asset", pemesananPengadaanMasterAssetDetail.getMasterAsset().getTipe());

			map.put("spesifikasi", pemesananPengadaanMasterAssetDetail.getMasterAsset().getSpesifikasi());
			map.put("hargatotal", pemesananPengadaanMasterAssetDetail.getHargaTotal());
			map.put("pph", pemesananPengadaanMasterAssetDetail.getPersenPph());
			map.put("ppn", pemesananPengadaanMasterAssetDetail.getPersenPpn());
			map.put("hargapotongan", pemesananPengadaanMasterAssetDetail.getHargaPotongan());
			map.put("hargabeli", pemesananPengadaanMasterAssetDetail.getHargaBeli());
			map.put("jumlah", pemesananPengadaanMasterAssetDetail.getJumlah());
			map.put("nama", pemesananPengadaanMasterAssetDetail.getMasterAsset().getNama());
			map.put("kode", pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getKode());
			map.put("isbn", pemesananPengadaanMasterAssetDetail.getMasterAsset().getKode());

			map.put("penyedia",
					pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getPenyedia() == null ? ""
							: pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getPenyedia()
									.getNama());

			map.put("jenis_pemesanan_pengadaan_asset", pemesananPengadaanMasterAssetDetail
					.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset());

			String status = "";
			if (pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getDisetujuiOleh() == null) {
				status = "Belum disetujui";
			} else {
				status = "Disetujui oleh "
						+ pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getDisetujuiOleh()
								.getUserNama()
						+ " pada "
						+ (pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset()
								.getTanggalPersetujuan() == null ? ""
										: Common.dateFormat51.get().format(pemesananPengadaanMasterAssetDetail
												.getPemesananPengadaanMasterAsset().getTanggalPersetujuan()));
			}

			map.put("status_persetujuan", status);

			map.put("perpustakaan",
					pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getKeterangan());

			map.put("pengirimanPalingLambat",
					pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getPengirimanPalingLambat());

			map.put("tanggal_persetujuan",
					pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getTanggalPersetujuan());
			map.put("disetujui_oleh",
					pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getDisetujuiOleh() == null
							? ""
							: pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getDisetujuiOleh()
									.getUserNama());

			maps.add(map);
		}

		parameters.put("maps", maps);
		parameters.put("totalSemua", totalSemua);

		for (Object o : parameters.keySet()) {
			if (o.toString().contains("disposisiSop")) {
				parameters.put(o.toString(), null);
			}
		}
		return parameters;
	}

	/**
	 * <b>Tujuan:</b> Implementasi antarmuka {@code DataCriteria#cetakData()} yang menghasilkan
	 * file laporan PDF untuk satu PO. Digunakan oleh mekanisme ekspor data massal (tombol cetak
	 * data di toolbar) yang memanggil metode ini untuk setiap baris yang dipilih.
	 *
	 * <b>Cara kerja:</b> Cast {@code generalValueObject} ke {@code PemesananPengadaanMasterAsset},
	 * lalu memanggil {@code parameter()} untuk menyiapkan data laporan, kemudian
	 * {@code Report.generateFileReport()} untuk menghasilkan file PDF menggunakan template
	 * JasperReports {@code "asset/pemesanan_pengadaan"} dengan tanggal dokumen PO sebagai
	 * referensi waktu laporan.
	 *
	 * <b>Parameter:</b>
	 * @param generalValueObject objek PO yang dicast ke {@code PemesananPengadaanMasterAsset}.
	 * @return {@code File} laporan PDF yang dihasilkan di direktori temp laporan.
	 * @throws Exception jika terjadi kesalahan generasi laporan atau akses database.
	 *
	 * <b>Penanganan error:</b> Exception dipropagasi ke mekanisme ekspor di {@code Common.cetakData()}.
	 *
	 * <b>Pemeliharaan:</b> Nama template {@code "asset/pemesanan_pengadaan"} harus sesuai dengan
	 * nama file JRXML di direktori laporan server. Jika template diganti, perbarui string ini.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = (PemesananPengadaanMasterAsset) generalValueObject;
		List maps = null;
		File file = Report.generateFileReport(Report.PDF, parameter(pemesananPengadaanMasterAsset),
				"asset/pemesanan_pengadaan", pemesananPengadaanMasterAsset.getTanggalPembuatan(), maps, Common.locale);
		return file;
	}

	/**
	 * <b>Tujuan:</b> Menghasilkan dan menampilkan laporan PDF PO secara langsung di browser
	 * pengguna (inline preview/download). Dipanggil setelah simpan, setelah persetujuan,
	 * setelah penolakan, dan dari tombol cetak pada baris grid.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code parameter()} untuk menyiapkan data laporan dari PO,
	 * kemudian {@code Report.generatePDFReport()} yang menghasilkan PDF dari template JasperReports
	 * dan mengirimkannya ke browser pengguna melalui mekanisme ZKoss content delivery.
	 *
	 * <b>Parameter:</b>
	 * @param pemesananPengadaanMasterAsset PO yang akan dicetak; tidak boleh null dan sebaiknya
	 *        sudah tersimpan di database (id tidak null) agar data lengkap.
	 * @throws Exception jika terjadi kesalahan generasi laporan PDF atau akses database.
	 *
	 * <b>Penanganan error:</b> Exception dipropagasi ke pemanggil. Biasanya dipanggil dari
	 * dalam {@code Common.createDefaultTimer()} sehingga exception akan ditangkap oleh ZKoss
	 * timer error handler.
	 *
	 * <b>Pemeliharaan:</b> Metode ini menggunakan template yang sama dengan {@code cetakData()}.
	 * Digunakan sebagai {@code static} sehingga bisa dipanggil dari renderer atau context lain
	 * tanpa instance action.
	 */
	public static void cetak(PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset) throws Exception {

		Report.generatePDFReport(Report.PDF, parameter(pemesananPengadaanMasterAsset), "asset/pemesanan_pengadaan",
				pemesananPengadaanMasterAsset.getTanggalPembuatan());
	}

	private Checkbox searchaktif;

	/** Filter status persetujuan: Semua / Belum Disetujui / Telah Disetujui. */
	private Combobox searchStatusPersetujuan;

	/**
	 * <b>Tujuan:</b> Membangun objek {@code Criteria} Hibernate untuk query daftar PO sesuai
	 * filter yang aktif di form pencarian. Digunakan untuk menghitung jumlah total (paging)
	 * dan mengambil halaman data yang ditampilkan di grid.
	 *
	 * <b>Cara kerja:</b> Jika {@code searchparent} null (halaman belum siap), langsung return null.
	 * Bangun set satuan kerja yang relevan berdasarkan pilihan {@code searchparent}; jika parent
	 * dipilih, ambil semua turunannya via {@code satuanKerjaTreeModel.getChildsSet()}.
	 * Buat {@code Criteria} pada kelas {@code PemesananPengadaanMasterAsset} dengan restriction:
	 * <ul>
	 *   <li>Status aktif (atau semua jika searchaktif tidak dicentang).</li>
	 *   <li>Filter rentang tanggal pembuatan (antara {@code start} dan {@code end}).</li>
	 *   <li>Filter status persetujuan: Semua / Belum Disetujui (disetujuiOleh IS NULL) /
	 *       Telah Disetujui (disetujuiOleh IS NOT NULL).</li>
	 *   <li>Filter lunas/belum lunas.</li>
	 *   <li>Filter satuan kerja (berdasarkan set yang sudah dihitung).</li>
	 *   <li>Filter penyedia aset.</li>
	 *   <li>Filter lokasi.</li>
	 *   <li>Filter catatan kesepakatan (ILIKE).</li>
	 *   <li>Filter kode PO (ILIKE).</li>
	 *   <li>Filter keterangan (ILIKE).</li>
	 * </ul>
	 * Jika {@code order} true, tambahkan urutan descending berdasarkan ID.
	 *
	 * <b>Parameter:</b>
	 * @param order {@code true} untuk menambahkan ORDER BY id DESC pada criteria.
	 * @return {@code Criteria} yang sudah dikonfigurasi, atau {@code null} jika {@code searchparent} null.
	 *
	 * <b>Penanganan error:</b> Tidak ada exception yang dilempar; jika satuan kerja tidak ditemukan,
	 * set satuanKerjas akan kosong dan criteria akan mengembalikan semua data tanpa filter satuan kerja.
	 *
	 * <b>Pemeliharaan:</b> Jika menambah filter baru, tambahkan {@code .add(Restrictions.xxx())}
	 * dengan pola null-safe yang sama (gunakan {@code sqlRestriction("1=1")} sebagai kondisi "tidak filter").
	 * Pastikan urutan method implementasi interface {@code DataCriteria} ini dipertahankan.
	 */
	public Criteria initCriteria(boolean order) {
		if (searchparent == null) return null;

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PemesananPengadaanMasterAsset.class)

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(start == null || end == null || start.getValue() == null || end.getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
								+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + "')"))

				// Filter status persetujuan via combobox (Semua / Belum Disetujui / Telah Disetujui).
				.add(searchStatusPersetujuan == null || searchStatusPersetujuan.getSelectedItem() == null
						|| searchStatusPersetujuan.getSelectedItem().getLabel() == null
						|| "Semua".equalsIgnoreCase(searchStatusPersetujuan.getSelectedItem().getLabel().trim())
								? Restrictions.sqlRestriction("1=1")
								: "Belum Disetujui".equalsIgnoreCase(
										searchStatusPersetujuan.getSelectedItem().getLabel().trim())
												? Restrictions.isNull("disetujuiOleh")
												: Restrictions.isNotNull("disetujuiOleh"))

				// Filter status persetujuan via checkbox "Telah Disetujui" / "Belum Disetujui"
				// (pelengkap combobox). Keduanya OFF atau keduanya ON → semua; hanya "Telah Disetujui"
				// → disetujuiOleh IS NOT NULL; hanya "Belum Disetujui" → disetujuiOleh IS NULL.
				.add((disetujui != null && disetujui.isChecked()) == (blmDisetujui != null
						&& blmDisetujui.isChecked()) ? Restrictions.sqlRestriction("1=1")
								: (disetujui != null && disetujui.isChecked())
										? Restrictions.isNotNull("disetujuiOleh")
										: Restrictions.isNull("disetujuiOleh"))

				.add(lunasSaja != null && lunasSaja.isChecked() ? Restrictions.eq("lunas", true)
						: Restrictions.sqlRestriction("1=1"))

				.add(blmLunasSaja != null && blmLunasSaja.isChecked() ? Restrictions.eq("lunas", false)
						: Restrictions.sqlRestriction("1=1"))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add((searchPenyedia == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchPenyedia.getAttribute("penyediaAsset") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("penyedia", searchPenyedia.getAttribute("penyediaAsset"))))

				.add(searchlokasi.getSelectedItem() == null || searchlokasi.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))

				.add(searchcatatan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("catatanKesepakatan", searchcatatan.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE))

		;
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Menjalankan pencarian/refresh daftar PO berdasarkan filter yang aktif
	 * dan memperbarui tampilan grid beserta informasi paging. Dipanggil oleh timer auto-refresh,
	 * event listener filter, paging, dan setelah operasi CRUD berhasil.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Guard: jika {@code searchketerangan} null (komponen belum siap), langsung return.</li>
	 *   <li>Hitung jumlah total baris via {@code Common.initPaging(initCriteria(false), paging)}
	 *       untuk memperbarui informasi paging.</li>
	 *   <li>Ambil data halaman aktif via {@code initCriteria(true)} dengan limit
	 *       {@code Common.ROWS_COUNT_ON_PAGE} dan offset berdasarkan {@code paging.getActivePage()}.</li>
	 *   <li>Set renderer dan model pada grid, lalu panggil {@code setModelCheckMobile()} yang
	 *       memperbarui tampilan grid di browser.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param event event ZKoss yang memicu pencarian (bisa null jika dipanggil programatik).
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit; jika criteria gagal,
	 * exception dipropagasi ke ZKoss error handler atau timer error handler.
	 *
	 * <b>Pemeliharaan:</b> {@code initCriteria()} dipanggil dua kali (sekali tanpa order untuk
	 * paging, sekali dengan order untuk data). Jika query berat, pertimbangkan cache count.
	 * Pastikan grid memiliki renderer yang terdaftar sebelum model di-set.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchketerangan == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);
		List<PemesananPengadaanMasterAsset> pemesananPengadaanMasterAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pemesananPengadaanMasterAsset);
		grid.setRowRenderer(new PemesananPengadaanMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	private Set<PerjanjianKerjasamaMasterAssetDetail> perjanjianKerjasamaMasterAssets = null;
	private Set<PermintaanPengadaanMasterAssetDetail> permintaanPengadaanMasterAssets = null;
	private MyGrid gridMasterAsset;
	private PemesananPengadaanMasterAssetHelper pemesananPengadaanMasterAssetHelper = null;
	private MyCheckboxConfig setujui;
	private Radiogroup jenisPemesananPengadaanAsset;
	private MyCheckboxConfig tampaPermintaan;
	private MyDatebox pengirimanPalingLambat;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private MyDoublebox dp;
	private MyTextbox catatanKesepakatan;
	private MyCheckboxConfig byTermin;
	private JSONArray array;
	private Row rowFormula;
	private Row rowTermin;
	private Row rowDataTermin;
	private Row rowMulaiPerkerjaan;
	private MyDatebox tanggalMulai;
	private MyDatebox tanggalSampai;
	private Row rowSampaiPerkerjaan;
	private Combobox jenisPajakPpnDp;
	private Label totalPembayaranDp;
	private Row rowDp;
	private Row rowPPnDp;
	private Row rowTotalDp;
	private Row rowTag;

	/**
	 * <b>Tujuan:</b> Membangun dan mengembalikan grid form input/tampil untuk PO secara programatik.
	 * Ini adalah metode terpanjang dan terpenting di kelas ini, mengimplementasikan antarmuka
	 * {@code FormSop#form()} sehingga dapat diintegrasikan dengan alur persetujuan SOP.
	 *
	 * <b>Cara kerja:</b> Membangun {@code MyGrid} dua-kolom (30% label | 70% input) dengan baris-baris:
	 * Satuan Kerja, Perjanjian Kerjasama, Anggaran (dari PR), Permintaan Pengadaan (daftar PR + tombol pilih),
	 * Pesanan Langsung (checkbox tampaPermintaan), Anggaran (workspace RAB), Tanpa Anggaran (checkbox),
	 * Penyedia, Kode Pemesanan (auto-generate), Tanggal Pembuatan, Jenis Pemesanan (radiogroup),
	 * Nilai Pembayaran DP, PPN DP, Total DP, Nomor Tagihan, Pemilik (opsional), Lokasi, Ruang (opsional),
	 * Ketentuan Termin (checkbox byTermin), Rincian Termin (reloadFormula), Tanggal Mulai dan Selesai,
	 * Keterangan, Catatan Kesepakatan, Tanggal Pengiriman, Detail Barang (PemesananPengadaanMasterAssetHelper),
	 * Status Pemesanan (checkbox setujui untuk persetujuan).
	 *
	 * Mode persetujuan ({@code persetujuan = true}) menggantikan semua komponen input dengan Label
	 * read-only. Event listener dipasang untuk: kode otomatis berubah saat jenis pemesanan atau tanggal
	 * berubah; visibilitas baris DP/Termin/Anggaran berubah saat checkbox diklik; penyedia dan satuan
	 * kerja terisi otomatis saat perjanjian kerjasama dipilih; perjanjian kerjasama di-filter berdasarkan
	 * satuan kerja yang dipilih.
	 *
	 * <b>Parameter:</b>
	 * @param generalValueObject objek PO yang akan ditampilkan/diisi di form (dicast ke
	 *                           {@code PemesananPengadaanMasterAsset}).
	 * @param disposisiSop       disposisi SOP yang terkait (jika dari alur SOP); null untuk CRUD biasa.
	 * @param save               tombol Simpan yang event listenernya akan dipasang (atau dikelola
	 *                           oleh mekanisme SOP).
	 * @param setujuiData        event listener opsional untuk checkbox persetujuan (dari modul SOP).
	 * @return {@code MyGrid} komponen form yang sudah dibangun, siap ditambahkan ke container.
	 * @throws Exception jika terjadi kesalahan inisialisasi komponen atau akses database.
	 *
	 * <b>Penanganan error:</b> Exception dari komponen individu tidak ditangkap (dipropagasi).
	 * Data dari database diakses via {@code HibernateUtil.currentSession()} yang diasumsikan aktif.
	 *
	 * <b>Pemeliharaan:</b> Karena panjangnya metode ini, berhati-hati saat menambah baris form baru:
	 * (1) buat MyFormRow, (2) setParent ke rows, (3) appendChild label dan input, (4) handle mode
	 * persetujuan dengan Label read-only. Ikuti pola yang sudah ada untuk konsistensi visual.
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}

		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
		this.pemesananPengadaanMasterAsset = (PemesananPengadaanMasterAsset) generalValueObject;

		if (this.pemesananPengadaanMasterAsset.getDisetujuiOleh() != null) {
			persetujuan = true;
		}

		MyGrid grid = new MyGrid();
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(pemesananPengadaanMasterAsset.getSatuanKerja() == null ? ""
				: pemesananPengadaanMasterAsset.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", pemesananPengadaanMasterAsset.getSatuanKerja());
		if (persetujuan) {
			row.appendChild(new Label(pemesananPengadaanMasterAsset.getSatuanKerja() == null ? ""
					: pemesananPengadaanMasterAsset.getSatuanKerja().getNama()));
		} else {
			row.appendChild(satuanKerja);
		}
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perjanjian Kerjasama"));

		perjanjianKerjasamaMasterAsset = new Combobox();
		if (persetujuan || perjanjianKerjasamaMasterAssetData != null) {
			if (perjanjianKerjasamaMasterAssetData != null) {
				pemesananPengadaanMasterAsset.setPerjanjianKerjasamaMasterAsset(perjanjianKerjasamaMasterAssetData);
			}
			row.appendChild(new Label(pemesananPengadaanMasterAsset.getPerjanjianKerjasamaMasterAsset() == null
					? "(Tanpa Perjanjian Kerjasama)"
					: pemesananPengadaanMasterAsset.getPerjanjianKerjasamaMasterAsset().getKode()));
		} else {
			row.appendChild(perjanjianKerjasamaMasterAsset);
		}
		perjanjianKerjasamaMasterAsset.setWidth("90%");

		List<PermintaanPengadaanMasterAssetDetail> dataPermintaanPengadaanMasterAssetDetail = new ArrayList<PermintaanPengadaanMasterAssetDetail>();
		if (!pemesananPengadaanMasterAsset.getPermintaanPengadaanMasterAssets().isEmpty()) {

			List<Long> data = new ArrayList<Long>();
			for (String s : pemesananPengadaanMasterAsset.getPermintaanPengadaanMasterAssets().split(",")) {
				try {
					data.add(Long.parseLong(s.trim()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PemesananPengadaanMasterAssetAction.java:2527");
					// TODO: handle exception
				}
			}
			dataPermintaanPengadaanMasterAssetDetail = data.isEmpty()
					? new ArrayList<PermintaanPengadaanMasterAssetDetail>()
					: HibernateUtil.currentSession().createCriteria(PermintaanPengadaanMasterAssetDetail.class)
							.add(Restrictions.in("id", data)).list();

			if (dataPermintaanPengadaanMasterAssetDetail.isEmpty()) {
				dataPermintaanPengadaanMasterAssetDetail = HibernateUtil.currentSession()
						.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
						.createAlias("permintaanPengadaanMasterAsset", "permintaanPengadaanMasterAsset")
						.createAlias("pemesananPengadaanMasterAssetDetail", "pemesananPengadaanMasterAssetDetail")
						.add(Restrictions.eq("pemesananPengadaanMasterAssetDetail.pemesananPengadaanMasterAsset",
								pemesananPengadaanMasterAsset))
						.list();
			}

			if (!dataPermintaanPengadaanMasterAssetDetail.isEmpty()) {

				Set<Workspace> workspaces = new HashSet<Workspace>();
				for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : dataPermintaanPengadaanMasterAssetDetail) {
					if (permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
							.getWorkspace() != null) {
						workspaces.add(permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
								.getWorkspace());
					}
				}

				if (!workspaces.isEmpty()) {
					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Kode Anggaran"));
					Vbox unit = new Vbox();
					row.appendChild(unit);

					for (Workspace workspace : workspaces) {
						RevisiHelper.createNewRevisi(Workspace.class, workspace, workspace.toString()).setParent(unit);
					}
				}
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Permintaan Pengadaan *")));
		row.appendChild(permintaanPengadaanMasterAsset = new Vbox());
		permintaanPengadaanMasterAsset.setWidth("90%");
		PemesananPengadaanMasterAssetAction.this.permintaanPengadaanMasterAssets = new HashSet<PermintaanPengadaanMasterAssetDetail>();

		if (!pemesananPengadaanMasterAsset.getPermintaanPengadaanMasterAssets().isEmpty()) {
			PemesananPengadaanMasterAssetAction.this.permintaanPengadaanMasterAssets
					.addAll(dataPermintaanPengadaanMasterAssetDetail);
		}

		MyToolbarbuttonConfig button;
		permintaanPengadaanMasterAsset.appendChild(
				button = new MyToolbarbuttonConfig("Ambil Data Permintaan Barang/Jasa", "/img/svg/addthis.svg"));
		button.setVisible(!persetujuan && !pembelianLangsung);
		final MyGrid subPermintaanPengadaanMasterAsset = new MyGrid();
		permintaanPengadaanMasterAsset.appendChild(subPermintaanPengadaanMasterAsset);

		class PermintaanBarangEventListener implements EventListener {

			private PermintaanBarangEventListener getThis() {
				return this;
			}

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(subPermintaanPengadaanMasterAsset);

				if (PemesananPengadaanMasterAssetAction.this.permintaanPengadaanMasterAssets != null) {

					Map<Long, PermintaanPengadaanMasterAsset> lists = new HashMap<Long, PermintaanPengadaanMasterAsset>();
					for (PermintaanPengadaanMasterAssetDetail assetDetail : PemesananPengadaanMasterAssetAction.this.permintaanPengadaanMasterAssets) {
						lists.put(assetDetail.getPermintaanPengadaanMasterAsset().getId(),
								assetDetail.getPermintaanPengadaanMasterAsset());
					}

					Columns columns = new Columns();
					columns.setParent(subPermintaanPengadaanMasterAsset);

					MyColumnConfig column = new MyColumnConfig("Data Permintaan Pembelian");
					column.setParent(columns);

					if (!persetujuan) {
						column = new MyColumnConfig("Batal");
						column.setParent(columns);
						column.setWidth("15%");
					}

					Rows rows = new Rows();
					rows.setParent(subPermintaanPengadaanMasterAsset);

					for (final PermintaanPengadaanMasterAsset k : lists.values()) {

						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);

						// getDisetujuiOleh() adalah proxy Tbmuser; saat dijalankan dari timer/event
						// sesi awalnya sudah tertutup → getUserNama() memicu LazyInitialization.
						// Ambil ulang dari cache memakai id (aman) dengan fallback "" bila gagal.
						String namaDisetujuiOleh = "";
						if (k.getDisetujuiOleh() != null) {
							try {
								ais.database.model.Tbmuser uDisetujui = (ais.database.model.Tbmuser) ais.common.ConstantValues
										.ambil(ais.database.model.Tbmuser.class.getName(),
												k.getDisetujuiOleh().getUserId());
								namaDisetujuiOleh = uDisetujui == null ? "" : uDisetujui.getUserNama();
							} catch (Throwable t) {
								namaDisetujuiOleh = "";
							}
						}

						A a = new A(
								k.getKode() + "-" + k.getKeterangan()
										+ (k.getDisetujuiOleh() == null ? ""
												: " (" + (namaDisetujuiOleh + " "
														+ Common.dateFormat51.get().format(k.getTanggalPersetujuan())
														+ ")")));
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								PermintaanPengadaanMasterAssetAction.onAddExternal(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

									}
								}, k);

							}
						});
						a.setStyle("font-size:10px;");
						a.setParent(row);

						if (!persetujuan && !pembelianLangsung) {
							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
							button.setTooltiptext("Hapus Data");
							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
											MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
											MyMessageboxConfig.QUESTION, new EventListener() {

												@Override
												public void onEvent(Event event) throws Exception {
													int i = Integer.parseInt(event.getData().toString());
													if (i == MyMessageboxConfig.OK) {
														try {

															List<PermintaanPengadaanMasterAssetDetail> assetDetails = new ArrayList<PermintaanPengadaanMasterAssetDetail>();
															for (PermintaanPengadaanMasterAssetDetail assetDetail : PemesananPengadaanMasterAssetAction.this.permintaanPengadaanMasterAssets) {
																if (!assetDetail.getPermintaanPengadaanMasterAsset()
																		.getId().equals(k.getId())) {
																	assetDetails.add(assetDetail);
																}
															}

															PemesananPengadaanMasterAssetAction.this.permintaanPengadaanMasterAssets
																	.clear();
															PemesananPengadaanMasterAssetAction.this.permintaanPengadaanMasterAssets
																	.addAll(assetDetails);

															Common.createDefaultTimer(getThis());

														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

														}

													}

												}
											});

								}
							});
							button.setParent(row);
						}
					}
				}
				generateDetail();
			}

		}

		final PermintaanBarangEventListener permintaanBarangEventListener = new PermintaanBarangEventListener();

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<PermintaanPengadaanMasterAsset> permintaanPengadaanMasterAssets = new ArrayList<PermintaanPengadaanMasterAsset>();

						AmbilDataPermintaanPengadaanMasterAssetBanyak ambilPermintaanPengadaanMasterAsset = new AmbilDataPermintaanPengadaanMasterAssetBanyak(
								false, permintaanPengadaanMasterAssets,
								(SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
								.appendChild(ambilPermintaanPengadaanMasterAsset);
						ambilPermintaanPengadaanMasterAsset.setWidth("90%");
						ambilPermintaanPengadaanMasterAsset.setHeight("90%");

						ambilPermintaanPengadaanMasterAsset.setEventListener(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								PemesananPengadaanMasterAssetAction.this.permintaanPengadaanMasterAssets
										.addAll((List<PermintaanPengadaanMasterAssetDetail>) arg0.getData());

								PemesananPengadaanMasterAssetAction.this.perjanjianKerjasamaMasterAssets = null;

								permintaanBarangEventListener.onEvent(arg0);
							}
						});

						ambilPermintaanPengadaanMasterAsset.onModal();
					}
				});
			}
		});

		Common.createDefaultTimer(permintaanBarangEventListener);

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampilkan_pemesanan_langsung_di_po"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		tampaPermintaan = new MyCheckboxConfig("Merupakan pesanan langsung (tanpa ada permintaan)");
		if (persetujuan || pembelianLangsung) {
			row.appendChild(new Label("Merupakan pesanan langsung (tanpa ada permintaan) ? "
					+ (pemesananPengadaanMasterAsset.getTampaPermintaan() ? "Ya" : "Tidak")));
		} else {
			row.appendChild(tampaPermintaan);
		}
		tampaPermintaan.setChecked(pemesananPengadaanMasterAsset.getTampaPermintaan());

		workspace = new AmbilDataWorkspaceBanbox(false);
		final MyFormRow rowAnggaran = new MyFormRow();
		rowAnggaran.setParent(rows);
		rowAnggaran.appendChild(new ais.ui.util.MyLabelConfig("Anggaran *"));
		workspace.setValue(pemesananPengadaanMasterAsset.getWorkspace() == null ? ""
				: pemesananPengadaanMasterAsset.getWorkspace().toString());
		workspace.setAttribute("workspace", pemesananPengadaanMasterAsset.getWorkspace());
		workspace.setWidth("90%");
		workspace.setReadonly(true);

		if (persetujuan || pembelianLangsung) {
			rowAnggaran.appendChild(new Label(pemesananPengadaanMasterAsset.getWorkspace() == null ? ""
					: pemesananPengadaanMasterAsset.getWorkspace().toString()));
		} else {
			rowAnggaran.appendChild(workspace);
		}

		final MyFormRow rowtanpaAnggaran = new MyFormRow();
		rowtanpaAnggaran.setParent(rows);
		rowtanpaAnggaran.appendChild(new ais.ui.util.MyLabelConfig(""));
		tanpaAnggaran = new MyCheckboxConfig("Merupakan tanpa anggaran");
		if (persetujuan || pembelianLangsung) {
			rowtanpaAnggaran.appendChild(new Label("Merupakan tanpa anggaran ? "
					+ (pemesananPengadaanMasterAsset.getTanpaAnggaran() ? "Ya" : "Tidak")));
		} else {
			rowtanpaAnggaran.appendChild(tanpaAnggaran);
		}

		tanpaAnggaran.setChecked(pemesananPengadaanMasterAsset.getTanpaAnggaran());

		row = new MyFormRow();

		EventListener eventListenerPesanan = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowtanpaAnggaran.setVisible(tampaPermintaan.isChecked()
						&& Common.bolehKonfigurasi("tampilkan_tanpa_anggaran"));
				permintaanPengadaanMasterAsset.getParent().setVisible(!tampaPermintaan.isChecked());
				rowAnggaran.setVisible(tampaPermintaan.isChecked() && !tanpaAnggaran.isChecked());
			}

		};

		tampaPermintaan.addEventListener("onClick", eventListenerPesanan);
		tanpaAnggaran.addEventListener("onClick", eventListenerPesanan);
		eventListenerPesanan.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyedia *"));

		penyediaAsset = new AmbilDataPenyediaAssetBanbox();
		if (persetujuan) {
			row.appendChild(new Label(pemesananPengadaanMasterAsset.getPenyedia() == null ? ""
					: pemesananPengadaanMasterAsset.getPenyedia().getNama()));
		} else {
			row.appendChild(penyediaAsset);
		}

		penyediaAsset.setAttribute("penyediaAsset", pemesananPengadaanMasterAsset.getPenyedia());
		penyediaAsset.setValue(pemesananPengadaanMasterAsset.getPenyedia() == null ? ""
				: pemesananPengadaanMasterAsset.getPenyedia().getNama());
		penyediaAsset.setReadonly(true);
		penyediaAsset.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pemesanan *"));

		tanggalPembuatan = new MyDatebox(
				pemesananPengadaanMasterAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: pemesananPengadaanMasterAsset.getTanggalPembuatan());
		if (pemesananPengadaanMasterAsset.getKode() == null) {
			String noAgenda = generateCode(false);
			pemesananPengadaanMasterAsset.setKode(noAgenda);
		}

		kode = new Label(pemesananPengadaanMasterAsset.getKode());
		if (persetujuan) {
			row.appendChild(new Label(pemesananPengadaanMasterAsset.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan *"));
		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat6.get().format(tanggalPembuatan.getValue())));
		} else {
			row.appendChild(tanggalPembuatan);
		}
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setWidth("90%");
		tanggalPembuatan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pemesanan *"));
		jenisPemesananPengadaanAsset = new Radiogroup();
		if (persetujuan) {
			row.appendChild(new Label(pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset() == null ? ""
					: pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset().getNama()));
		} else {
			row.appendChild(jenisPemesananPengadaanAsset);
		}

		Common.insertRadio(jenisPemesananPengadaanAsset, "nama", JenisPemesananPengadaanAsset.class,
				Restrictions.eq("aktif", true));
		Common.selectRadioItem(jenisPemesananPengadaanAsset,
				pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset());
		jenisPemesananPengadaanAsset.setWidth("90%");

		EventListener jEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisPemesananPengadaanAsset j = (JenisPemesananPengadaanAsset) (jenisPemesananPengadaanAsset
						.getSelectedItem() == null ? null
								: jenisPemesananPengadaanAsset.getSelectedItem().getAttribute("value"));

				if (j != null && pemesananPengadaanMasterAsset.getId() == null) {
					pemesananPengadaanMasterAsset.setJenisPemesananPengadaanAsset(j);
					String noAgenda = generateCode(false);
					kode.setValue(noAgenda);
				}
			}
		};

		jenisPemesananPengadaanAsset.addEventListener("onClick", jEventListener);
		tanggalPembuatan.addEventListener("onChange", jEventListener);

		rowDp = new MyFormRow();
		rowDp.setParent(rows);
		dp = new MyDoublebox(pemesananPengadaanMasterAsset.getDp());
		rowDp.appendChild(new ais.ui.util.MyLabelConfig(
				pembelianLangsung ? "Nilai Pembayaran Beli Langsung" : "Nilai Pembayaran DP"));
		if (persetujuan) {
			rowDp.appendChild(new Label(Common.numberFormat.get().format(pemesananPengadaanMasterAsset.getDp())));
		} else {
			rowDp.appendChild(dp);
		}

		jenisPajakPpnDp = new Combobox();
		Common.insertComboDanSemua(jenisPajakPpnDp, new String[] { "nama" }, "keterangan", JenisPajakPpn.class,
				"Tanpa Pajak DP / Beli Langsung", Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPajakPpnDp, pemesananPengadaanMasterAsset.getJenisPajakPpnDp());

		rowPPnDp = new MyFormRow();
		rowPPnDp.setParent(rows);
		rowPPnDp.appendChild(new ais.ui.util.MyLabelConfig(
				pembelianLangsung ? "PPN Pembayaran Beli Langsung" : "PPN Pembayaran DP"));
		if (persetujuan) {
			rowPPnDp.appendChild(new Label(
					pemesananPengadaanMasterAsset.getJenisPajakPpnDp() == null ? "Tanpa Pajak DP / Beli Langsung"
							: pemesananPengadaanMasterAsset.getJenisPajakPpnDp().getNama()));
		} else {
			rowPPnDp.appendChild(jenisPajakPpnDp);
		}
		jenisPajakPpnDp.setWidth("90%");

		rowTotalDp = new MyFormRow();
		rowTotalDp.setParent(rows);
		rowTotalDp.appendChild(new ais.ui.util.MyLabelConfig(
				pembelianLangsung ? "Total Pembayaran Beli Langsung" : "Total Pembayaran DP"));
		rowTotalDp.appendChild(totalPembayaranDp = new Label(
				Common.numberFormat.get().format(pemesananPengadaanMasterAsset.getDptotal())));

		EventListener totalPembayaranDpEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pemesananPengadaanMasterAsset.setDp(dp.getValue());
				pemesananPengadaanMasterAsset
						.setJenisPajakPpnDp((JenisPajakPpn) (jenisPajakPpnDp.getSelectedItem() == null ? null
								: jenisPajakPpnDp.getSelectedItem().getValue()));
				totalPembayaranDp
						.setValue(Common.numberFormat.get().format(pemesananPengadaanMasterAsset.getDptotal()));
			}
		};

		jenisPajakPpnDp.addEventListener("onChange", totalPembayaranDpEventListener);
		dp.addEventListener("onChange", totalPembayaranDpEventListener);

		rowTag = new MyFormRow();
		rowTag.setParent(rows);
		kodeInvoice = new MyTextbox(pemesananPengadaanMasterAsset.getKodeInvoice());
		rowTag.appendChild(
				new ais.ui.util.MyLabelConfig(pembelianLangsung ? "Nomor Tagihan Beli Langsung" : "Nomor Tagihan DP"));
		if (persetujuan) {
			rowTag.appendChild(new Label(pemesananPengadaanMasterAsset.getKodeInvoice()));
		} else {
			rowTag.appendChild(kodeInvoice);
		}

		pemilikAsset = new Combobox();
		row = new MyFormRow();
		row.setVisible(tampilkanRuanganDamPemilikAset);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pemilik"));
		if (persetujuan) {
			row.appendChild(new Label(pemesananPengadaanMasterAsset.getPemilikAsset() == null ? ""
					: pemesananPengadaanMasterAsset.getPemilikAsset().getNama()));
		} else {
			row.appendChild(pemilikAsset);
		}

		Common.insertComboDanSemua(pemilikAsset, new String[] { "nama", "id" }, "keterangan", PemilikAsset.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(pemilikAsset, pemesananPengadaanMasterAsset.getPemilikAsset());
		pemilikAsset.setWidth("90%");

		lokasi = new Combobox();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi"));

		if (persetujuan) {
			row.appendChild(new Label(pemesananPengadaanMasterAsset.getLokasi() == null ? ""
					: pemesananPengadaanMasterAsset.getLokasi().getNama()));
		} else {
			row.appendChild(lokasi);
		}

		Common.insertComboDanSemua(lokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(lokasi, pemesananPengadaanMasterAsset.getLokasi());
		lokasi.setWidth("90%");

		LokasiAction.kunciLokasi(lokasi);

		ruang = new AmbilDataRuangBanbox();
		row = new MyFormRow();
		row.setVisible(tampilkanRuanganDamPemilikAset);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));
		ruang = new AmbilDataRuangBanbox();
		if (persetujuan) {
			row.appendChild(new Label(pemesananPengadaanMasterAsset.getRuang() == null ? ""
					: pemesananPengadaanMasterAsset.getRuang().getNama()));
		} else {
			row.appendChild(ruang);
		}

		ruang.setValue(pemesananPengadaanMasterAsset.getRuang() == null ? ""
				: (pemesananPengadaanMasterAsset.getRuang().getKodeRuangan()));
		ruang.setAttribute("ruang", pemesananPengadaanMasterAsset.getRuang());
		ruang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ketentuan pembayaran"));
		byTermin = new MyCheckboxConfig("Dengan cara termin / progress");
		if (persetujuan || (pembelianLangsung && perjanjianKerjasamaMasterAssetData == null)) {
			row.appendChild(new Label(pemesananPengadaanMasterAsset.getByTermin() ? "Dengan cara termin / progress"
					: "Tidak menggunakan termin"));
		} else {
			row.appendChild(byTermin);
		}
		byTermin.setChecked(pemesananPengadaanMasterAsset.getByTermin());

		rowTermin = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowTermin, "2");
		rowTermin.setParent(rows);
		rowTermin.appendChild(new ais.ui.util.MyLabelBoldConfig("Rincian Termin"));

		tanggalMulai = new MyDatebox(pemesananPengadaanMasterAsset.getTanggalMulai());
		rowMulaiPerkerjaan = new MyFormRow();
		rowMulaiPerkerjaan.setParent(rows);
		rowMulaiPerkerjaan.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai Perkerjaan *"));
		if (persetujuan) {
			rowMulaiPerkerjaan.appendChild(new Label(pemesananPengadaanMasterAsset.getTanggalMulai() == null ? ""
					: Common.dateFormat6.get().format(pemesananPengadaanMasterAsset.getTanggalMulai())));
		} else {
			rowMulaiPerkerjaan.appendChild(tanggalMulai);
		}
		tanggalMulai.setFormat(Common.dateFormat1.get().toPattern());
//		tanggalMulai.setWidth("90%");
		if (tanggalMulai != null) tanggalMulai.setReadonly(true);

		tanggalSampai = new MyDatebox(pemesananPengadaanMasterAsset.getTanggalSampai());
		rowSampaiPerkerjaan = new MyFormRow();
		rowSampaiPerkerjaan.setParent(rows);
		rowSampaiPerkerjaan.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Selesai Perkerjaan *"));
		if (persetujuan) {
			rowSampaiPerkerjaan.appendChild(new Label(pemesananPengadaanMasterAsset.getTanggalSampai() == null ? ""
					: Common.dateFormat6.get().format(pemesananPengadaanMasterAsset.getTanggalSampai())));
		} else {
			rowSampaiPerkerjaan.appendChild(tanggalSampai);
		}
		tanggalSampai.setFormat(Common.dateFormat1.get().toPattern());
//		tanggalSampai.setWidth("90%");
		if (tanggalSampai != null) tanggalSampai.setReadonly(true);

		rowDataTermin = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowDataTermin, "2");
		rowDataTermin.setParent(rows);
		array = new JSONArray(pemesananPengadaanMasterAsset.getFormula());
		rowFormula = Common.tampilanScroll1(rowDataTermin);
		reloadFormula(rowFormula, array, persetujuan, pemesananPengadaanMasterAsset);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowTermin.setVisible(byTermin.isChecked());
				rowDataTermin.setVisible(byTermin.isChecked());

				rowMulaiPerkerjaan.setVisible(byTermin.isChecked());
				rowSampaiPerkerjaan.setVisible(byTermin.isChecked());

				rowDp.setVisible(!byTermin.isChecked() && perjanjianKerjasamaMasterAssetData == null);
				rowPPnDp.setVisible(!byTermin.isChecked() && perjanjianKerjasamaMasterAssetData == null);
				rowTotalDp.setVisible(!byTermin.isChecked() && perjanjianKerjasamaMasterAssetData == null);
				rowTag.setVisible(!byTermin.isChecked() && perjanjianKerjasamaMasterAssetData == null);
			}
		};
		byTermin.addEventListener("onClick", eventListener);
		eventListener.onEvent(null);

		keterangan = new MyTextbox(pemesananPengadaanMasterAsset.getKeterangan());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan pemesanan pengadaan barang / jasa *"));
		row.appendChild(keterangan = new MyTextbox(pemesananPengadaanMasterAsset.getKeterangan()));

		if (persetujuan) {
			Html label = new Html(pemesananPengadaanMasterAsset.getKeterangan());
			row.appendChild(label);
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan Kesepakatan"));
		catatanKesepakatan = new MyTextbox(pemesananPengadaanMasterAsset.getCatatanKesepakatan());

		if (persetujuan) {
			Html label = new Html(pemesananPengadaanMasterAsset.getCatatanKesepakatan());
			row.appendChild(label);
		} else {
			row.appendChild(catatanKesepakatan);
		}

		catatanKesepakatan.setWidth("90%");
		catatanKesepakatan.setRows(3);

		pengirimanPalingLambat = new MyDatebox(pemesananPengadaanMasterAsset.getPengirimanPalingLambat());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengiriman Paling Lambat"));
		if (persetujuan) {
			row.appendChild(new Label(pengirimanPalingLambat.getValue() == null ? ""
					: Common.dateFormat6.get().format(pengirimanPalingLambat.getValue())));
		} else {
			row.appendChild(pengirimanPalingLambat);
		}
		pengirimanPalingLambat.setFormat(Common.dateFormat.get().toPattern());
		pengirimanPalingLambat.setWidth("90%");
		pengirimanPalingLambat.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild((pemesananPengadaanMasterAssetHelper = new PemesananPengadaanMasterAssetHelper(
				gridMasterAsset = new MyGrid()))
				.initDetail(pemesananPengadaanMasterAsset, persetujuan, pembelianLangsung, tampaPermintaan));

		// --- Referensi RINCIAN PENGAJUAN pada langkah disposisi ---
		// Sejumlah SOP pengadaan (mis. "Reimburse / Penggantian Dana" = penerimaan langsung tanpa
		// pemesanan) menyimpan rincian pengaju pada dokumen Penerimaan/Permintaan, BUKAN Pemesanan.
		// Namun langkah persetujuan tertentu (mis. "Persetujuan I" Bendahara Yayasan) dikonfigurasi
		// menampilkan form Pemesanan — sehingga "Daftar Pemesanan Barang/Jasa" kosong (entitas
		// Pemesanan fresh, id==null) padahal pengaju sudah mengisi rincian. Bila kita berada dalam
		// konteks disposisi dan entitas Pemesanan memang kosong, tampilkan rincian pengajuan yang
		// tertaut disposisiSop (Penerimaan -> Permintaan -> Pemesanan) sebagai referensi read-only,
		// agar aktor persetujuan tetap melihat "data pengajuan" tersebut. Bila tak ada rincian yang
		// ditemukan, helper mengembalikan null sehingga tak ada blok yang muncul (tanpa dampak pada
		// alur pengadaan normal yang gridnya sudah terisi).
		// Tampilkan referensi rincian pengajuan selama entitas PEMESANAN masih KOSONG (belum ada
		// baris detail) — BUKAN sekadar id==null. Pada SOP Reimburse, setelah langkah
		// "ditindaklanjuti" sebuah record Pemesanan KOSONG tetap tercipta (id != null) sehingga
		// dulu referensi (Daftar Penerimaan) HILANG. Dengan mengecek jumlah detail, referensi
		// tetap tampil SEBELUM & SESUDAH tindak lanjut, tanpa mengganggu pengadaan normal (yang
		// grid Pemesanan-nya sudah terisi -> pemesananKosong=false -> referensi tak tampil).
		boolean pemesananKosong = pemesananPengadaanMasterAsset.getId() == null;
		if (!pemesananKosong) {
			try {
				Number jmlDetailPemesanan = (Number) ais.database.hibernate.HibernateUtil.currentSession()
						.createCriteria(ais.database.model.asset.PemesananPengadaanMasterAssetDetail.class)
						.add(org.hibernate.criterion.Restrictions.eq("pemesananPengadaanMasterAsset",
								pemesananPengadaanMasterAsset))
						.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
				pemesananKosong = jmlDetailPemesanan == null || jmlDetailPemesanan.intValue() == 0;
			} catch (Exception eCekPemesanan) {
				pemesananKosong = false;
			}
		}
		if (this.disposisiSop != null && this.disposisiSop.getId() != null && pemesananKosong) {
			try {
				ais.ui.util.MyGroupboxStyled referensiPengajuan = ais.action.master.asset.helper.RincianPengajuanPengadaanReferensiHelper
						.bangunReferensiPengajuan(this.disposisiSop);
				if (referensiPengajuan != null) {
					MyFormRow rowReferensi = new MyFormRow();
					rowReferensi.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(rowReferensi, "2");
					rowReferensi.appendChild(referensiPengajuan);
				}
			} catch (Exception eReferensi) {
				ais.common.Common.tampilErrorJikaAdmin(eReferensi);
			}
		}

		row = new MyFormRow();
		row.setVisible(persetujuan && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Pemesanan"));
		row.appendChild(setujui = new MyCheckboxConfig("Setujui Pemesanan Barang / Jasa ini"));
		setujui.setChecked(pemesananPengadaanMasterAsset.getDisetujuiOleh() != null);

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null)
					System.out.println("selesai -> " + arg0.getTarget());
				if (arg0 != null && arg0.getTarget() instanceof Checkbox && setujui != arg0.getTarget()) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("selesai");
					System.out.println("selesai -> " + selesai);
					if (selesai != null && selesai) {
						setujui.setChecked(true);
						setujui.setDisabled(true);
					} else {
						setujui.setChecked(false);
						setujui.setDisabled(false);
					}
				}
			}
		});

		jEventListener.onEvent(null);

		final EventListener eventListenerPerjanjianKerjasamaMasterAsset = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				SatuanKerja satker = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");

				if (satuanKerja.getAttribute("satuanKerja") != null) {
					satuanKerja.setDisabled(true);
				}

				Common.insertComboDanSemua(perjanjianKerjasamaMasterAsset,
						new String[] { "kode", "penyedia", "satuanKerja" }, "keterangan",
						PerjanjianKerjasamaMasterAsset.class, "== Tanpa Perjanjian Kerjasama ==",
						Restrictions.and(
								Restrictions.or(Restrictions.isNull("satuanKerja"),
										satker == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("satuanKerja", satker)),

								Restrictions.or(Restrictions.isNotNull("disetujuiOleh"), Restrictions
										.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));

				Common.selectComboItem(true, perjanjianKerjasamaMasterAsset,
						pemesananPengadaanMasterAsset.getPerjanjianKerjasamaMasterAsset());
			}
		};

		satuanKerja.setEventListener(eventListenerPerjanjianKerjasamaMasterAsset);
		eventListenerPerjanjianKerjasamaMasterAsset.onEvent(null);

		EventListener eventListenerKerjasama = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PerjanjianKerjasamaMasterAsset asset = (PerjanjianKerjasamaMasterAsset) (perjanjianKerjasamaMasterAsset
						.getSelectedItem() == null ? null
								: perjanjianKerjasamaMasterAsset.getSelectedItem().getValue());

				if (asset != null) {

					pemesananPengadaanMasterAsset.setPerjanjianKerjasamaMasterAsset(asset);

					if (asset.getSatuanKerja() != null) {
						satuanKerja.setValue(asset.getSatuanKerja().getNama());
						satuanKerja.setAttribute("satuanKerja", asset.getSatuanKerja());

						satuanKerja.setDisabled(true);

					}

					if (asset.getPenyedia() != null) {
						penyediaAsset.setAttribute("penyediaAsset", asset.getPenyedia());
						penyediaAsset.setValue(asset.getPenyedia().getNama());
						penyediaAsset.setDisabled(true);
					}

				}

			}
		};

		perjanjianKerjasamaMasterAsset.addEventListener("onChange", eventListenerKerjasama);
		eventListenerKerjasama.onEvent(null);

		if (setujuiData != null) {
			setujui.addEventListener("onClick", setujuiData);

			setujuiData.onEvent(new Event("", null, pemesananPengadaanMasterAsset.getDisetujuiOleh() != null));
		}

		return grid;
	}

	/**
	 * <b>Tujuan:</b> Implementasi antarmuka {@code FormSop#istilah()} yang mengembalikan nama
	 * modul/jenis dokumen dalam bahasa Indonesia untuk ditampilkan di judul alur SOP.
	 *
	 * <b>Cara kerja:</b> Mengembalikan string literal "Pemesanan Pengadaan Barang/Jasa".
	 * Nilai ini digunakan oleh modul SOP untuk memberi label pada tab atau header alur disposisi.
	 *
	 * @return string nama istilah modul ini, selalu "Pemesanan Pengadaan Barang/Jasa".
	 * @throws Exception tidak dilempar; deklarasi exception mengikuti kontrak interface.
	 *
	 * <b>Pemeliharaan:</b> Jika nama modul berubah di UI, perbarui string ini agar konsisten
	 * dengan label di menu dan judul halaman.
	 */
	@Override
	public String istilah() throws Exception {
		return "Pemesanan Pengadaan Barang/Jasa";
	}

	/**
	 * <b>Tujuan:</b> Implementasi antarmuka {@code FormSop#ambil()} yang mengembalikan objek
	 * data PO yang sedang aktif diproses. Digunakan oleh modul SOP untuk mengambil referensi
	 * data dokumen yang terkait dengan disposisi.
	 *
	 * <b>Cara kerja:</b> Mengembalikan field instance {@code pemesananPengadaanMasterAsset}
	 * yang di-set saat metode {@code init()} dipanggil. Objek ini merupakan {@code DataSop}
	 * karena {@code PemesananPengadaanMasterAsset} mengimplementasikan antarmuka tersebut.
	 *
	 * @return objek {@code PemesananPengadaanMasterAsset} yang sedang aktif, atau null jika
	 *         belum ada PO yang diinisialisasi.
	 * @throws Exception tidak dilempar; deklarasi exception mengikuti kontrak interface.
	 *
	 * <b>Pemeliharaan:</b> Pastikan {@code init()} selalu dipanggil sebelum {@code ambil()}
	 * agar tidak mengembalikan null yang menyebabkan NullPointerException di modul SOP.
	 */
	@Override
	public DataSop ambil() throws Exception {
		return pemesananPengadaanMasterAsset;
	}

	/**
	 * <b>Tujuan:</b> Implementasi antarmuka {@code FormSop#ambilClass()} yang mengembalikan
	 * kelas Java dari entitas yang dikelola action ini. Digunakan oleh modul SOP dan framework
	 * generik untuk refleksi dan pembuatan criteria.
	 *
	 * <b>Cara kerja:</b> Mengembalikan literal kelas {@code PemesananPengadaanMasterAsset.class}.
	 * Framework dapat menggunakannya untuk membuat Hibernate criteria atau untuk menentukan
	 * tipe data dalam konteks generik SOP.
	 *
	 * @return kelas {@code PemesananPengadaanMasterAsset.class}.
	 * @throws Exception tidak dilempar; deklarasi exception mengikuti kontrak interface.
	 *
	 * <b>Pemeliharaan:</b> Delegator konstanta; tidak perlu diubah kecuali entitas utama berubah.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PemesananPengadaanMasterAsset.class;
	}

	/**
	 * <b>Tujuan:</b> Menghasilkan kode PO (nomor surat) yang unik berdasarkan format nomor
	 * surat yang dikonfigurasi di {@code NomorSuratAlurPengadaan.PEMESANAN_PEMBELIAN_DATA}.
	 * Mendukung format kode dengan substitusi variabel seperti JENIS PO, tahun, bulan, dan
	 * nomor urut berdasarkan hitungan baris di database.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika konfigurasi nomor surat PO belum diset, gunakan barcode acak via
	 *       {@code Common.getGeneratedBarCode()}.</li>
	 *   <li>Tentukan index urut: jika menggunakan index urut dari konfigurasi, ambil dari
	 *       {@code nomorSurat.getNomorIndex()}; jika tidak, hitung via {@code getindex()}
	 *       yang menghitung baris PO di database sesuai aturan reset (per tahun/bulan/tanggal).</li>
	 *   <li>Jika {@code tambah = true}, increment index di konfigurasi via
	 *       {@code NomorSurat.tambahIndexNomorSurat()} agar nomor berikutnya berbeda.</li>
	 *   <li>Format kode menggunakan {@code nomorSurat.format(index, tanggal)}.</li>
	 *   <li>Jika PO sudah memiliki jenis pemesanan, substitusi placeholder "JENIS PO" dengan
	 *       kode jenis pemesanan.</li>
	 *   <li>Pastikan kode unik via {@code KodeUnikUtil.pastikanUnik()} yang menambahkan sufiks
	 *       jika kode sudah dipakai.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param tambah {@code true} saat simpan definitif (increment index di database),
	 *               {@code false} saat hanya preview/tampil kode di form (preview tanpa increment).
	 * @return string kode PO yang unik dan terformat.
	 *
	 * <b>Penanganan error:</b> Jika konfigurasi nomor surat null, fallback ke barcode acak.
	 * {@code KodeUnikUtil.pastikanUnik()} menangani konflik duplikat dengan loop sufiks.
	 *
	 * <b>Pemeliharaan:</b> Jika format nomor surat perlu variabel baru (misalnya kode satuan kerja),
	 * tambahkan {@code noAgenda.replaceAll("VAR", nilai)} setelah baris substitusi "JENIS PO".
	 * Pastikan {@code tambah} hanya {@code true} saat benar-benar menyimpan untuk menghindari
	 * lompatan nomor urut.
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurPengadaan.PEMESANAN_PEMBELIAN_DATA == null
				|| NomorSuratAlurPengadaan.PEMESANAN_PEMBELIAN_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PEMESANAN_PEMBELIAN_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PEMESANAN_PEMBELIAN_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PEMESANAN_PEMBELIAN_DATA.getNomorSurat());

		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PEMESANAN_PEMBELIAN_DATA.getNomorSurat());
		}

		String noAgenda = NomorSuratAlurPengadaan.PEMESANAN_PEMBELIAN_DATA.getNomorSurat().format(index,
				tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue());

		if (pemesananPengadaanMasterAsset != null
				&& pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset() != null) {
			noAgenda = noAgenda.replaceAll("JENIS PO",
					pemesananPengadaanMasterAsset.getJenisPemesananPengadaanAsset().getKode());
		}

		return ais.action.master.KodeUnikUtil.pastikanUnik(PemesananPengadaanMasterAsset.class, noAgenda);
	}

	/**
	 * <b>Tujuan:</b> Menghitung index urut nomor surat PO berikutnya berdasarkan jumlah
	 * baris PO yang sudah ada di database, dengan mempertimbangkan berbagai aturan reset urutan
	 * (per tahun, per bulan, per tanggal tertentu, per kelompok nomor surat, atau global).
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code nomorSurat} null, kembalikan 0.</li>
	 *   <li>Ambil tahun dan bulan saat ini dari {@code WaktuUtil.getCalendar()}.</li>
	 *   <li>Buat criteria pada {@code PemesananPengadaanMasterAsset} dengan join LEFT ke
	 *       {@code nomorSuratAlurPengadaan} dan {@code nomorSurat}.</li>
	 *   <li>Filter berdasarkan aturan pengurutan: {@code urutBerdasarkanNomor} (filter per
	 *       nomorSurat spesifik), atau {@code urutBerdasarkanKelompok} (filter per kelompok),
	 *       atau tanpa filter.</li>
	 *   <li>Filter reset per tahun ({@code resetUrutanTiapTahun}).</li>
	 *   <li>Filter reset per bulan ({@code resetUrutanTiapBulan}).</li>
	 *   <li>Filter reset per tanggal tertentu ({@code resetTiap}) jika tanggal reset sudah
	 *       lewat atau hari ini.</li>
	 *   <li>Query rowCount sebagai index, increment 1, dan kembalikan.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param nomorSurat konfigurasi nomor surat yang menentukan aturan penomoran;
	 *                   jika null, kembalikan 0.
	 * @return index urut berikutnya (jumlah PO yang sudah ada + 1).
	 *
	 * <b>Penanganan error:</b> Jika query mengembalikan null, index default ke 0 kemudian di-increment.
	 * Tidak ada exception yang dilempar secara eksplisit.
	 *
	 * <b>Pemeliharaan:</b> Aturan reset urutan dikontrol sepenuhnya oleh konfigurasi {@code NomorSurat};
	 * tidak perlu mengubah kode ini saat mengubah aturan penomoran — cukup ubah konfigurasi di database.
	 * Namun jika ada aturan reset baru (misalnya per semester), perlu tambahkan kondisi di sini.
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PemesananPengadaanMasterAsset.class)
				.createAlias("nomorSuratAlurPengadaan", "nomorSuratAlurPengadaan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurPengadaan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurPengadaan.nomorSurat", nomorSurat)

						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))

				.add(nomorSurat.getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetTiap() != null && (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang))
						|| nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	/**
	 * <b>Tujuan:</b> Implementasi antarmuka {@code FormSop#setPersetujuan()} yang mengubah
	 * mode tampilan action antara mode persetujuan (read-only) dan mode normal (editable).
	 * Dipanggil oleh modul SOP saat menyiapkan form untuk alur disposisi.
	 *
	 * <b>Cara kerja:</b> Menyimpan nilai {@code persetujuan} ke field instance. Flag ini
	 * dibaca oleh {@code form()} untuk menentukan apakah setiap field input ditampilkan sebagai
	 * komponen input (false) atau Label read-only (true). Juga dibaca oleh beberapa event listener
	 * internal untuk mengontrol visibilitas tombol tertentu.
	 *
	 * <b>Parameter:</b>
	 * @param persetujuan {@code true} untuk mode persetujuan (form read-only + checkbox setujui);
	 *                    {@code false} untuk mode CRUD normal (form editable).
	 *
	 * <b>Pemeliharaan:</b> Setter sederhana. Pastikan flag ini di-set sebelum memanggil {@code form()},
	 * karena form hanya membaca flag ini saat dibangun (tidak reaktif terhadap perubahan setelah build).
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * <b>Tujuan:</b> Membersihkan dan membangun ulang seluruh tampilan grid rincian termin
	 * pembayaran dari data {@code JSONArray formula}. Dipanggil setiap kali ada perubahan pada
	 * data termin (tambah, ubah, hapus baris termin, atau approve/cancel termin individual).
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Bersihkan konten {@code rowU} via {@code Common.clear()}.</li>
	 *   <li>Buat grid baru dengan kolom: nomor, nama termin, flag DP, persen pekerjaan, nilai DPP,
	 *       nilai tagihan, nomor tagihan, tanggal tagihan, PPN, PPH (pajak), pinalti, dan aksi.</li>
	 *   <li>Buat footer dengan total nilai tagihan yang dihitung dari semua termin aktif.</li>
	 *   <li>Untuk setiap entri JSONArray yang memiliki "key" valid (dan cocok dengan {@code keyTampil}
	 *       jika ada filter), buat baris grid dengan komponen input (mode edit) atau Label (mode
	 *       persetujuan/termin sudah disetujui).</li>
	 *   <li>Setiap baris memiliki sub-detail (MyDetail) untuk menampilkan dan mengelola dokumen
	 *       lampiran termin (upload, tampil, hapus via {@code LampiranLain}).</li>
	 *   <li>Event listener pada setiap field input langsung menyimpan perubahan ke JSONObject
	 *       terkait dan ke database (jika PO sudah tersimpan).</li>
	 *   <li>Tombol setujui/batalkan termin mengubah flag "setuju" di JSON dan menyimpan ke DB,
	 *       lalu memanggil {@code reloadDataFormula()} untuk me-refresh tampilan.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param rowU                          baris container (Row) tempat grid termin akan diletakkan.
	 * @param array                         JSONArray berisi semua data termin PO.
	 * @param persetujuan                   {@code true} jika form dalam mode persetujuan (read-only).
	 * @param pemesananPengadaanMasterAsset PO pemilik data termin, untuk auto-save ke database.
	 * @param keyTampil                     key termin yang difilter (null untuk tampilkan semua).
	 * @throws Exception jika terjadi kesalahan membangun komponen ZKoss atau akses database.
	 *
	 * <b>Penanganan error:</b> Kesalahan delete/approve ditampilkan via messagebox. Parse tanggal
	 * dari JSON menggunakan {@code Common.dateFormat1} dengan guard {@code isNull()}.
	 *
	 * <b>Pemeliharaan:</b> Metode ini cukup kompleks karena menggabungkan build UI, akses JSON,
	 * dan auto-save. Saat menambah field termin baru: (1) tambahkan kolom header di grid,
	 * (2) tambahkan komponen input di baris, (3) tambahkan ekstraksi di event listener,
	 * (4) perbarui {@code mapTermin()} dan {@code paramTermin()} untuk laporan.
	 */
	public static void reloadDataFormula(final Row rowU, final JSONArray array, final boolean persetujuan,
			final PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset, final String keyTampil)
			throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setStyle("min-height:250px");

		Foot foot = new Foot();
		foot.setParent(grid);

		foot.appendChild(new Footer());
		foot.appendChild(new Footer("Total"));
		foot.appendChild(new Footer());
		foot.appendChild(new Footer());
		foot.appendChild(new Footer());
		final Footer totalTermin = new Footer();
		foot.appendChild(totalTermin); // lokal per-desktop: field static menyebabkan "belongs to another desktop"
		foot.appendChild(new Footer());
		foot.appendChild(new Footer());
		foot.appendChild(new Footer());
		foot.appendChild(new Footer());
		foot.appendChild(new Footer());
		foot.appendChild(new Footer());

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("40px");

		column = new MyColumnConfig("Nama Termin/Progress");
		column.setParent(columns);
		column.setWidth("14%");

		column = new MyColumnConfig("DP");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("4%");

		column = new MyColumnConfig("Progress % Pekerjaan");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("8%");

		column = new MyColumnConfig("Nilai DPP");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("Nilai Tagihan");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("No. Tag.");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("9%");

		column = new MyColumnConfig("Tanggal Tag.");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("PPN");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("8%");

		column = new MyColumnConfig("PPH");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("8%");

		column = new MyColumnConfig("Nilai PPH");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pinalti");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("5%");

		Rows rows = new Rows();
		rows.setParent(grid);

		final EventListener hitungTotal = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Double totalSemua = 0.0;
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);

					if (jsonObject.isNull("key")) {
						continue;
					}

					String key = jsonObject.get("key") + "";

					if (keyTampil == null || (keyTampil != null && key.equalsIgnoreCase(keyTampil))) {

						Double penagihan = 0.0;
						if (!jsonObject.isNull("penagihan")) {
							penagihan = jsonObject.getDouble("penagihan");
						}

						Double ppn = 0.0;
						if (!jsonObject.isNull("ppn")) {
							ppn = jsonObject.getDouble("ppn");
						}

						Double total = penagihan + ((ppn / 100.0) * penagihan);

						totalSemua += total;
					}
				}

				totalTermin.setLabel(Common.numberFormat.get().format(totalSemua));

			}
		};

		hitungTotal.onEvent(null);

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);

			if (jsonObject.isNull("key")) {
				continue;
			}

			String key = jsonObject.get("key") + "";

			if (keyTampil == null || (keyTampil != null && key.equalsIgnoreCase(keyTampil))) {

				final JSONArray dokumens;
				if (!jsonObject.isNull("dokumens")) {
					dokumens = jsonObject.getJSONArray("dokumens");
				} else {
					dokumens = new JSONArray();
					jsonObject.put("dokumens", dokumens);
				}

				String nama = "";

				if (!jsonObject.isNull("nama")) {
					nama = jsonObject.get("nama") + "";
				}

				String nomor = "";

				if (!jsonObject.isNull("nomor")) {
					nomor = jsonObject.get("nomor") + "";
				}

				Double pekerjaan = 0.0;
				if (!jsonObject.isNull("pekerjaan")) {
					pekerjaan = jsonObject.getDouble("pekerjaan");
				}

				Double penagihan = 0.0;
				if (!jsonObject.isNull("penagihan")) {
					penagihan = jsonObject.getDouble("penagihan");
				}

				Double pinalti = 0.0;
				if (!jsonObject.isNull("pinalti")) {
					pinalti = jsonObject.getDouble("pinalti");
				}

				Date tanggalD = null;

				if (!jsonObject.isNull("tanggalD")) {
					tanggalD = jsonObject.get("tanggalD").toString().isEmpty() ? null
							: Common.dateFormat1.get().parse(jsonObject.get("tanggalD") + "");
				}

				Double ppn = 0.0;
				if (!jsonObject.isNull("ppn")) {
					ppn = jsonObject.getDouble("ppn");
				}

				Double total = penagihan + ((ppn / 100.0) * penagihan);

				final Boolean setuju;
				if (!jsonObject.isNull("setuju")) {
					setuju = Boolean.parseBoolean(jsonObject.get("setuju") + "");
				} else {
					setuju = false;
				}

				final Boolean merupakan_dp;
				if (!jsonObject.isNull("merupakan_dp")) {
					merupakan_dp = Boolean.parseBoolean(jsonObject.get("merupakan_dp") + "");
				} else {
					merupakan_dp = false;
				}

				final JenisPajakBarang jenisPajakBarang;
				if (!jsonObject.isNull("pajak")) {
					jenisPajakBarang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
							Long.parseLong(jsonObject.get("pajak") + ""));
				} else {
					jenisPajakBarang = null;
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				final MyDetail detail = new MyDetail();
				detail.setParent(row);
				detail.addEventListener("onOpen", new EventListener() {

					private void reload() throws Exception {
						Common.clear(detail);
						if (detail.isOpen()) {

							Grid grid = new Grid();
							grid.setSclass("dgrid");
							grid.setWidth("100%");
							grid.setParent(detail);
							grid.setWidth("100%");
							grid.setHeight("100%");

							Columns columns = new Columns();
							columns.setParent(grid);

							MyColumnConfig column = new MyColumnConfig("Nama Dokumen");
							column.setParent(columns);
							column.setWidth("90%");

							column = new MyColumnConfig("Hapus");
							column.setParent(columns);

							Rows rows = new Rows();
							rows.setParent(grid);

							for (int i = 0; i < dokumens.length(); i++) {
								final int index = i;
								final JSONObject jsonDokumen = dokumens.getJSONObject(i);

								if (jsonDokumen.isNull("keyDok")) {
									continue;
								}

								Long keyDok = Long.parseLong(jsonDokumen.get("keyDok") + "");

								String nama_file = "";

								if (!jsonDokumen.isNull("nama_file")) {
									nama_file = jsonDokumen.get("nama_file") + "";
								}

								String link = "";

								if (!jsonDokumen.isNull("link")) {
									link = jsonDokumen.get("link") + "";
								}

								Long id_file = null;

								if (!jsonDokumen.isNull("id_file")) {
									id_file = Long.parseLong(jsonDokumen.get("id_file") + "");
								}

								final LampiranLain lampiranLain = id_file != null
										? LampiranLain.ambil(true, id_file, "id")
										: LampiranLain.ambil(keyDok, "Dokumen Termin PO");

								if (lampiranLain != null) {

									MyFormRow rowU = new MyFormRow();
									rowU.setParent(rows);

									A a = new A(lampiranLain.getNama());
									a.setParent(rowU);
									a.setWidth("95%");

									a.addEventListener("onClick", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											Common.display(lampiranLain);
										}
									});

								}

								else if (!nama_file.isEmpty() && !link.isEmpty()) {

									MyFormRow rowU = new MyFormRow();
									rowU.setParent(rows);

									final String url = link;
									A a = new A(nama_file);
									a.setParent(rowU);
									a.setWidth("95%");
									a.addEventListener("onClick", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											Clients.evalJavaScript("popupCenter({url: '" + url
													+ "', title: 'Data', w: 1200, h: 600});");
										}
									});

									if (!setuju) {
										MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
												"/img/svg/trash.svg");
										button.setTooltiptext("Hapus Data");
										button.setParent(rowU);
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												MyMessageboxConfig.show("Apakah yakin ingin menghapus dokumen ini ?",
														"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
														MyMessageboxConfig.QUESTION, new EventListener() {

															@Override
															public void onEvent(Event event) throws Exception {
																int i = Integer.parseInt(event.getData().toString());
																if (i == MyMessageboxConfig.OK) {
																	try {
																		dokumens.put(index, new JSONObject());

																		if (pemesananPengadaanMasterAsset
																				.getId() != null) {
																			Session session = HibernateUtil
																					.currentSession();
																			session.refresh(
																					pemesananPengadaanMasterAsset);
																			pemesananPengadaanMasterAsset
																					.setFormula(array.toString());
																			Common.refreshUpdate(session,
																					pemesananPengadaanMasterAsset);
																			session.flush();
																		}

																		reload();

																	} catch (Exception e) {
																		Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

																	}

																}

															}
														});

											}
										});

									}
								}

							}

							Foot foot = new Foot();
							foot.setParent(grid);

							Footer footer = new Footer();
							foot.appendChild(footer);

							Long key = Math.abs(Common.randLong());

							Hbox hbox = new Hbox();
							hbox.setParent(footer);
							if (!setuju) {
								LampiranLain.createDownloadUploadFileLain(hbox, key, "Dokumen Termin PO",
										"Dokumen Termin", false, new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												LampiranLain lampiranLain = (LampiranLain) arg0.getData();

												JSONObject jsonObject = new JSONObject();

												Long key = Math.abs(Common.randLong());

												jsonObject.put("keyDok", key);
												jsonObject.put("nama", "Dokumen ...");
												jsonObject.put("link", lampiranLain.createLinkUri(false));
												jsonObject.put("nama_file", lampiranLain.getNama());
												jsonObject.put("id_file", lampiranLain.getId());
												jsonObject.put("tanggal",
														Common.dateFormat1.get().format(WaktuUtil.getDate()));

												dokumens.put(jsonObject);

												if (pemesananPengadaanMasterAsset.getId() != null) {
													Session session = HibernateUtil.currentSession();
													session.refresh(pemesananPengadaanMasterAsset);
													pemesananPengadaanMasterAsset.setFormula(array.toString());
													Common.refreshUpdate(session, pemesananPengadaanMasterAsset);
													session.flush();
												}

												reload();
											}
										}, null, false, false, false, true);

								footer = new Footer();
								foot.appendChild(footer);

								footer = new Footer();
								foot.appendChild(footer);
							}
						}

					}

					@Override
					public void onEvent(Event arg0) throws Exception {
						reload();
					}
				});

				final MyTextbox targetText = new MyTextbox(nama);
				final MyCheckboxConfig merupakan_dpBox = new MyCheckboxConfig();
				merupakan_dpBox.setChecked(merupakan_dp);
				final MyDoublebox pekerjaanBox = new MyDoublebox(pekerjaan);
				final MyDoublebox penagihanBox = new MyDoublebox(penagihan);
//				final Label totalBox = new Label(Common.numberFormat.get().format(total));

				final MyDoublebox totalBox = new MyDoublebox(total);

				final MyDoublebox ppnBox = new MyDoublebox(ppn);
				final MyDoublebox pinaltiBox = new MyDoublebox(pinalti);

				final Combobox persenPph = new Combobox();
				Common.insertComboDanSemua(persenPph, new String[] { "nama", "persen" }, "keterangan",
						JenisPajakBarang.class, "Tanpa Pajak", Restrictions.eq("aktif", true));
				Common.selectComboItem(persenPph, jenisPajakBarang);

				targetText.setWidth("95%");
				pekerjaanBox.setWidth("95%");
				penagihanBox.setWidth("95%");
				totalBox.setWidth("95%");
				pinaltiBox.setWidth("95%");
				ppnBox.setWidth("95%");
				persenPph.setWidth("95%");

				final MyDatebox tanggalDate = new MyDatebox(tanggalD);
				tanggalDate.setWidth("95%");

				final MyTextbox nomorText = new MyTextbox(nomor);

				// Nilai PPH (nominal) = tarif JenisPajakBarang % x Nilai DPP (penagihan).
				final Label labelNilaiPph = new Label(Common.numberFormat.get().format(
						jenisPajakBarang == null || jenisPajakBarang.getPersen() == null ? 0.0
								: (double) Math.round((jenisPajakBarang.getPersen() / 100.0) * penagihan)));

				if (persetujuan || setuju) {
					row.appendChild(new Label(nama));
					row.appendChild(new Label(merupakan_dp ? "Ya" : "Tidak"));
					row.appendChild(new Label(Common.numberFormat.get().format(pekerjaan)));
					row.appendChild(new Label(Common.numberFormat.get().format(penagihan)));
					row.appendChild(new Label(Common.numberFormat.get().format(total)));
					row.appendChild(new Label(nomor));
					row.appendChild(new Label(tanggalD == null ? "" : Common.dateFormat1.get().format(tanggalD)));
					row.appendChild(new Label(Common.numberFormat.get().format(ppn)));
					row.appendChild(new Label(jenisPajakBarang == null ? "Tanpa Pajak" : jenisPajakBarang.getNama()));
					row.appendChild(labelNilaiPph);
					row.appendChild(new Label(Common.numberFormat.get().format(pinalti)));
				} else {
					row.appendChild(targetText);
					row.appendChild(merupakan_dpBox);
					row.appendChild(pekerjaanBox);
					row.appendChild(penagihanBox);
					row.appendChild(totalBox);
					row.appendChild(nomorText);
					row.appendChild(tanggalDate);
					row.appendChild(ppnBox);
					row.appendChild(persenPph);
					row.appendChild(labelNilaiPph);
					row.appendChild(pinaltiBox);
				}

				final EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Double n = penagihanBox.getValue() == null ? 0.0 : penagihanBox.getValue();
						Double p = pinaltiBox.getValue() == null ? 0.0 : pinaltiBox.getValue();
						Double ppn = ppnBox.getValue() == null ? 0.0 : ppnBox.getValue();

						if (p > n) {
							MyMessageboxConfig.show("Nilai pinalti tidak boleh lebih besar dari tagihan termin",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

							Double pinalti = 0.0;
							if (!jsonObject.isNull("pinalti")) {
								pinalti = jsonObject.getDouble("pinalti");
							}

							pinaltiBox.setValue(pinalti);
							return;
						}

						JenisPajakBarang jenisPajakBarang = (JenisPajakBarang) (persenPph.getSelectedItem() == null
								? null
								: persenPph.getSelectedItem().getValue());

						jsonObject.put("pajak", jenisPajakBarang == null ? null : jenisPajakBarang.getId());

						// Update kolom Nilai PPH = tarif pajak terpilih x DPP (penagihan) terbaru.
						labelNilaiPph.setValue(Common.numberFormat.get().format(
								jenisPajakBarang == null || jenisPajakBarang.getPersen() == null ? 0.0
										: (double) Math.round((jenisPajakBarang.getPersen() / 100.0) * n)));

						jsonObject.put("nama", targetText.getValue());
						jsonObject.put("pekerjaan", pekerjaanBox.getValue());
						jsonObject.put("merupakan_dp", merupakan_dpBox.isChecked());

						jsonObject.put("penagihan", n);
						jsonObject.put("pinalti", p);
						jsonObject.put("ppn", ppn);
						jsonObject.put("nomor", nomorText.getValue());
						jsonObject.put("tanggalD", tanggalDate.getValue() == null ? ""
								: Common.dateFormat1.get().format(tanggalDate.getValue()));

						Double total = n + ((ppn / 100.0) * n);
						jsonObject.put("total", total);

						totalBox.setValue(total);

						if (pemesananPengadaanMasterAsset.getId() != null) {
							Session session = HibernateUtil.currentSession();
							session.refresh(pemesananPengadaanMasterAsset);
							pemesananPengadaanMasterAsset.setFormula(array.toString());
							Common.refreshUpdate(session, pemesananPengadaanMasterAsset);
							session.flush();
						}

						hitungTotal.onEvent(arg0);
					}
				};

				totalBox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Double n = totalBox.getValue() == null ? 0.0 : totalBox.getValue();
						Double ppn = ppnBox.getValue() == null ? 0.0 : ppnBox.getValue();

						Double total = n / ((ppn + 100.0) / 100.0);

						penagihanBox.setValue(total);

						eventListener.onEvent(arg0);
					}
				});

				pekerjaanBox.addEventListener("onChange", eventListener);
				merupakan_dpBox.addEventListener("onClick", eventListener);
				targetText.addEventListener("onChange", eventListener);
				penagihanBox.addEventListener("onChange", eventListener);
				tanggalDate.addEventListener("onChange", eventListener);
				nomorText.addEventListener("onChange", eventListener);
				pinaltiBox.addEventListener("onChange", eventListener);
				ppnBox.addEventListener("onChange", eventListener);
				persenPph.addEventListener("onChange", eventListener);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
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

												array.put(index, new JSONObject());

												if (pemesananPengadaanMasterAsset.getId() != null) {
													Session session = HibernateUtil.currentSession();
													session.refresh(pemesananPengadaanMasterAsset);
													pemesananPengadaanMasterAsset.setFormula(array.toString());
													Common.refreshUpdate(session, pemesananPengadaanMasterAsset);
													session.flush();
												}

												reloadDataFormula(rowU, array, persetujuan,
														pemesananPengadaanMasterAsset, keyTampil);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

											}

										}

									}
								});

					}
				});

				Hbox hbox = new Hbox();
				hbox.setParent(row);

				if (!setuju) {
					final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");
					disetujui.setParent(hbox);

					disetujui.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							MyMessageboxConfig.show("Apakah yakin ingin mensetujui tagihan ini ?", "Pertanyaan",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {

													jsonObject.put("setuju", true);

													if (pemesananPengadaanMasterAsset.getId() != null) {
														Session session = HibernateUtil.currentSession();
														session.refresh(pemesananPengadaanMasterAsset);
														pemesananPengadaanMasterAsset.setFormula(array.toString());
														Common.refreshUpdate(session, pemesananPengadaanMasterAsset);
														session.flush();
													}

													reloadDataFormula(rowU, array, persetujuan,
															pemesananPengadaanMasterAsset, keyTampil);

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

												}

											}

										}
									});

						}
					});
				} else if (setuju) {

					final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("",
							"/img/svg/warning-outline.svg");
					disetujui.setParent(hbox);

					disetujui.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							MyMessageboxConfig.show("Apakah yakin ingin membatalkan persetujui tagihan ini ?",
									"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
									MyMessageboxConfig.QUESTION, new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {

													jsonObject.put("setuju", false);

													if (pemesananPengadaanMasterAsset.getId() != null) {
														Session session = HibernateUtil.currentSession();
														session.refresh(pemesananPengadaanMasterAsset);
														pemesananPengadaanMasterAsset.setFormula(array.toString());
														Common.refreshUpdate(session, pemesananPengadaanMasterAsset);
														session.flush();
													}

													reloadDataFormula(rowU, array, persetujuan,
															pemesananPengadaanMasterAsset, keyTampil);

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

												}

											}

										}
									});

						}
					});

				}

				if (persetujuan || setuju) {
					new Label().setParent(hbox);
				} else {

					button.setParent(hbox);
				}
			}
		}
	}

	/**
	 * <b>Tujuan:</b> Delegator ke overload {@code reloadFormula} tanpa filter key, sehingga
	 * menampilkan semua termin PO tanpa filter.
	 *
	 * <b>Cara kerja:</b> Memanggil {@code reloadFormula(rowFormula, array, persetujuan,
	 * pemesananPengadaanMasterAsset, null)} dengan {@code keyTampil = null}.
	 *
	 * <b>Parameter:</b>
	 * @param rowFormula                    baris Row container tempat tombol "Tambah Termin"
	 *                                      dan grid termin ditempatkan.
	 * @param array                         JSONArray data termin PO.
	 * @param persetujuan                   mode persetujuan (read-only) atau edit.
	 * @param pemesananPengadaanMasterAsset PO pemilik data termin.
	 * @throws Exception jika terjadi kesalahan membangun komponen.
	 *
	 * <b>Pemeliharaan:</b> Delegator sederhana. Logika inti ada di overload dengan parameter
	 * {@code keyTampil}.
	 */
	public static void reloadFormula(Row rowFormula, JSONArray array, boolean persetujuan,
			PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset) throws Exception {
		reloadFormula(rowFormula, array, persetujuan, pemesananPengadaanMasterAsset, null);
	}

	/**
	 * <b>Tujuan:</b> Membersihkan dan membangun ulang area container termin dalam form PO,
	 * termasuk tombol "Tambah Termin" dan seluruh grid data termin. Merupakan titik masuk
	 * utama untuk merender area termin; mendelegasikan render grid ke {@code reloadDataFormula()}.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Buat {@code MyFormRow} baru ({@code rowU}) sebagai container grid termin.</li>
	 *   <li>Buat tombol "Tambah Termin" yang hanya terlihat jika bukan mode persetujuan dan
	 *       tidak ada filter key. Saat diklik, menambah objek JSON baru ke {@code array} dan
	 *       memanggil {@code reloadDataFormula()}.</li>
	 *   <li>Tambahkan tombol ke {@code rowFormula}, lalu tambahkan {@code rowU} ke parent
	 *       dari {@code rowFormula} (baris berikutnya di grid form).</li>
	 *   <li>Panggil {@code reloadDataFormula()} untuk membangun konten grid termin.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param rowFormula                    Row ZKoss container tempat tombol Tambah Termin diletakkan.
	 * @param array                         JSONArray data termin yang akan dirender.
	 * @param persetujuan                   {@code true} untuk mode persetujuan (tombol Tambah disembunyikan).
	 * @param pemesananPengadaanMasterAsset PO pemilik data termin.
	 * @param keyTampil                     filter key termin (null untuk tampilkan semua termin).
	 * @throws Exception jika terjadi kesalahan membangun komponen ZKoss.
	 *
	 * <b>Penanganan error:</b> Exception dipropagasi ke pemanggil.
	 *
	 * <b>Pemeliharaan:</b> Metode ini static agar bisa dipanggil dari konteks lain (misalnya
	 * dari modul tagihan yang perlu menampilkan termin PO dalam konteks berbeda dengan filter key).
	 * Jangan ubah parameter JSONArray menjadi copy karena modifikasi di sini harus mempengaruhi
	 * array yang sama yang disimpan di entitas PO.
	 */
	public static void reloadFormula(final Row rowFormula, final JSONArray array, final boolean persetujuan,
			final PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset, final String keyTampil)
			throws Exception {
		final MyFormRow rowU = new MyFormRow();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Termin", "/img/svg/addthis.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(!persetujuan && (keyTampil == null || keyTampil.trim().isEmpty()));
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("nama", "");
				jsonObject.put("pekerjaan", 0.0);
				jsonObject.put("penagihan", 0.0);

				Long key = Math.abs(Common.randLong());
				jsonObject.put("key", key);
				array.put(jsonObject);

				reloadDataFormula(rowU, array, persetujuan, pemesananPengadaanMasterAsset, keyTampil);
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, array, persetujuan, pemesananPengadaanMasterAsset, keyTampil);

	}
}
