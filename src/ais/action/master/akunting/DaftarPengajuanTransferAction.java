package ais.action.master.akunting;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Html;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.maintenance.MainAction;
import ais.action.master.dashboard.akunting.DasboardDaftarPengajuanTransfer;
import ais.action.master.dashboard.akunting.DasboardDaftarPengajuanTransferPerJenis;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>DaftarPengajuanTransferAction — Monitor Daftar Pengajuan Transfer Keuangan</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Controller ZK berbasis {@link GenericAutowireComposer} yang menangani tampilan dan
 * pemantauan daftar pengajuan transfer keuangan ({@link DaftarPengajuanTransfer}).
 * Pengajuan transfer adalah proses formal di mana unit kerja mengajukan kebutuhan
 * pembayaran (uang muka, LPJ, kas kecil, termin DP, penggajian, dll.) ke bagian
 * keuangan pusat. Setiap pengajuan memiliki siklus status: Belum diproses → Sudah
 * diajukan (proses transfer dibuat) → Transitori (dana sudah masuk akun perantara)
 * atau Sudah Transfer (dana sudah ditransfer ke penerima). Halaman ini memungkinkan
 * bagian keuangan memantau seluruh pengajuan dengan filter multi-dimensi (status,
 * satuan kerja, rentang waktu, kode/nama), melihat keterangan dan status SOP
 * persetujuan, serta mengedit keterangan langsung dari grid tanpa popup. Halaman
 * ini juga menampilkan dua tab dasbor statistik: Ringkasan Proses dan Ringkasan
 * Jenis untuk analisis distribusi pengajuan.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Inisialisasi di {@code doAfterCompose}: mengatur tinggi layout adaptif via
 * {@code applyTransferListHeight}, mengisi rentang tanggal default 6 bulan ke
 * belakang, mendaftarkan paging, timer refresh, dan listener satuan kerja. Dasbor
 * statistik dimuat secara lazy melalui timer pada tick pertama untuk tab "Ringkasan
 * Proses" dan hanya dimuat saat tab "Ringkasan Jenis" diklik (lazy load). Grid
 * mendukung filter: belum diproses, sudah diajukan, sudah transfer, transitori,
 * aktif, rentang tanggal, dan kode/nama. Keterangan dapat diedit langsung di grid
 * via Textbox inline yang menyimpan ke database saat onChange. Fitur ekspor Excel
 * tersedia melalui {@link Common#cetakData}.</p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh operasi berjalan di event thread ZK. Pemuatan dasbor statistik menggunakan
 * timer ZK (createDefaultTimer) untuk menunda ke tick berikutnya. Sesi Hibernate
 * adalah sesi terkelola yang tidak boleh ditutup manual. Dasbor statistik dimuat
 * lazy untuk menghindari delay saat halaman pertama dibuka.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Array {@code contents} mendefinisikan field-field yang diekspor ke Excel —
 * jika ada field baru pada {@link DaftarPengajuanTransfer} yang perlu diekspor,
 * tambahkan ke array ini. Metode {@code applyTransferListHeight} membaca konfigurasi
 * tinggi frame dari {@link MainAction#getConfiguredFrameMinimumHeight} — jika layout
 * halaman berubah, sesuaikan kalkulasi offset tinggi di sini. Status rendering di
 * {@code DaftarPengajuanTransferRenderer} menggunakan logika multi-kondisi yang perlu
 * diselaraskan jika ada status baru pada alur transfer.</p>
 *
 * @see DaftarPengajuanTransfer
 * @see DasboardDaftarPengajuanTransfer
 * @see DasboardDaftarPengajuanTransferPerJenis
 * @see DataCriteria
 * @see DataSearchDefault
 */
public class DaftarPengajuanTransferAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * Nomor versi serialisasi untuk kompatibilitas mekanisme serialisasi Java.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Komponen paging untuk navigasi halaman daftar pengajuan transfer. */
	private Paging paging;

	/** Grid utama penampil daftar {@link DaftarPengajuanTransfer}. */
	private MyGrid grid;

	/** Window root halaman untuk pengaturan tinggi adaptif. */
	private Window window;

	/** Tabbox utama yang berisi tab "Daftar Transfer" dan "Statistik". */
	private Tabbox mainTabbox;

	/** Container tabpanels utama untuk pengaturan tinggi adaptif. */
	private Tabpanels mainTabpanels;

	/** Panel tab yang berisi daftar transfer dan filternya. */
	private Tabpanel daftarTransferPanel;

	/** Layout Borderlayout di dalam panel daftar transfer. */
	private Borderlayout daftarTransferLayout;

	/** Grid yang berisi filter pencarian (row filter). */
	private MyGrid filterGrid;

	/** Grid pembungkus daftar hasil pencarian (untuk scroll dan tinggi adaptif). */
	private MyGrid listWrapperGrid;

	/** Field teks pencarian berdasarkan kode atau nama pengajuan transfer. */
	private Textbox searchnama;

	/** Checkbox filter untuk menampilkan hanya yang aktif atau semua. */
	private Checkbox searchaktif;

	/** Datebox awal rentang waktu filter pengajuan. */
	private MyDatebox start;

	/** Datebox akhir rentang waktu filter pengajuan. */
	private MyDatebox end;

	/** Checkbox filter: tampilkan hanya yang sudah diajukan (proses transfer dibuat tapi belum direalisasi). */
	private Checkbox searchsudah;

	/** Checkbox filter: tampilkan hanya yang belum diproses (belum ada proses transfer). */
	private Checkbox searchbelum;

	/** Checkbox filter: tampilkan hanya yang sudah transfer (telah direalisasi). */
	private Checkbox searchsudahtrnf;

	/** Checkbox filter: tampilkan hanya yang masuk akun transitori. */
	private Checkbox searchTransitori;

	/** Banbox pemilih satuan kerja sebagai filter hierarki unit kerja. */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** Tombol pencarian/find di toolbar untuk referensi penambahan tombol cetak. */
	private MyToolbarbuttonConfig find;

	/** Filter jenis pembayaran: "Semua", "Tagihan Vendor", atau "PPh / Pajak". */
	private Combobox filterJenisPembayaran;

	/**
	 * Array nama field {@link DaftarPengajuanTransfer} yang akan digunakan untuk
	 * ekspor Excel via {@link Common#cetakData}. Urutan array ini menentukan
	 * urutan kolom pada file Excel yang dihasilkan.
	 */
	public static String[] contents = new String[] { "id", "kode", "nama", "keterangan", "satuanKerja", "waktu",
			"disposisiSop", "aktif", "pembayaranDpMasterAssetDetail", "pembayaranTerminMasterAssetDetail",
			"pembayaranGaji", "pembayaranPengadaanMasterAssetDetail", "pertangungjawaban", "uangMuka",
			"penggantianKasKecil", "nominal", "atasNamaSumber", "noRekSumber", "bankSumber", "prosesTransfer" };

	/** Panel tab statistik yang dimuat secara lazy (hanya saat diperlukan). */
	protected Tabpanel statistik;

	/** Model pohon satuan kerja untuk navigasi hierarki filter unit kerja. */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Mengembalikan string kosong jika nilai null, atau nilai aslinya.
	 * Helper null-safe untuk menghindari NullPointerException saat menampilkan
	 * string di komponen ZK.
	 *
	 * <p><b>Tujuan:</b> Menyederhanakan pengecekan null berulang di renderer
	 * grid dan metode-metode helper lainnya.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah. Fungsi sederhana ini aman
	 * dan konsisten dengan pola yang digunakan di seluruh modul akunting.</p>
	 *
	 * @param value nilai String yang mungkin null
	 * @return string asli jika tidak null, atau "" jika null
	 */
	private static String text(String value) {
		return value == null ? "" : value;
	}

	/**
	 * Mengevaluasi nilai Boolean secara aman terhadap null, mengembalikan
	 * {@code true} hanya jika nilai secara eksplisit adalah {@link Boolean#TRUE}.
	 *
	 * <p><b>Tujuan:</b> Menghindari NullPointerException pada field Boolean
	 * nullable dari entitas Hibernate. Digunakan untuk flag {@code transfer},
	 * {@code transitori}, dan {@code aktif} pada {@link DaftarPengajuanTransfer}.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanfaatkan {@link Boolean#TRUE}.equals(value) yang
	 * aman terhadap null — mengembalikan false jika value null.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah. Pola ini konsisten dengan
	 * standar penanganan Boolean nullable di kodebase AIS.</p>
	 *
	 * @param value nilai Boolean yang mungkin null
	 * @return true hanya jika value adalah Boolean.TRUE; false untuk null atau Boolean.FALSE
	 */
	private static boolean isTrue(Boolean value) {
		return Boolean.TRUE.equals(value);
	}

	/**
	 * Memformat nilai Double sebagai string angka yang diformat sesuai locale,
	 * dengan default 0.0 jika nilai null.
	 *
	 * <p><b>Tujuan:</b> Menyederhanakan pemformatan nilai nominal di renderer grid
	 * dengan penanganan null yang konsisten. Digunakan untuk menampilkan nominal
	 * pengajuan transfer.</p>
	 *
	 * <p><b>Cara kerja:</b> Menggunakan {@link Common#numberFormat} (ThreadLocal)
	 * untuk format yang konsisten dengan locale. Jika value null, menggunakan 0.0.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika format angka perlu diubah (misalnya menambahkan
	 * simbol mata uang), lakukan di level Common.numberFormat, bukan di sini.</p>
	 *
	 * @param value nilai Double yang mungkin null
	 * @return string angka yang diformat; "0" jika value null
	 */
	private static String formatNilai(Double value) {
		return Common.numberFormat.get().format(value == null ? 0.0 : value.doubleValue());
	}

	/**
	 * Membangun HTML panel informasi bergaya kartu modern untuk digunakan di dasbor
	 * statistik. Panel memiliki border biru, latar gradien, dan bayangan yang lembut.
	 *
	 * <p><b>Tujuan:</b> Menyediakan blok informasi visual yang konsisten dan menarik
	 * untuk penjelasan kontekstual di atas konten dasbor statistik. Panel ini
	 * menjelaskan apa yang akan ditampilkan di bawahnya sehingga pengguna memahami
	 * konteks data tanpa perlu membaca dokumentasi.</p>
	 *
	 * <p><b>Cara kerja:</b> Membangun string HTML dengan inline CSS menggunakan
	 * gradient, border-radius, dan box-shadow. Judul ditampilkan dengan warna biru
	 * tebal, isi dengan teks abu-abu kecil.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika tema visual aplikasi berubah, perbarui warna
	 * dan style di sini. Gunakan variabel CSS jika memungkinkan untuk konsistensi
	 * tema global.</p>
	 *
	 * @param judul teks judul panel yang ditampilkan dengan font biru tebal
	 * @param isi teks penjelasan yang ditampilkan di bawah judul
	 * @return string HTML panel informasi siap digunakan dengan komponen {@link Html} ZK
	 */
	private static String panelInfo(String judul, String isi) {
		return "<div style='margin:10px 10px 12px 10px; padding:14px 16px; border:1px solid #dbeafe; "
				+ "border-radius:14px; background:linear-gradient(135deg,#eff6ff 0%,#ffffff 70%); "
				+ "box-shadow:0 8px 20px rgba(15,23,42,0.06); color:#334155;'>"
				+ "<div style='font-size:15px; font-weight:700; color:#1d4ed8; margin-bottom:4px;'>" + judul
				+ "</div><div style='font-size:12.5px; line-height:1.55;'>" + isi + "</div></div>";
	}

	/**
	 * Mengembalikan nilai integer yang tidak lebih kecil dari minimum yang ditentukan.
	 * Helper untuk memastikan nilai tinggi layout tidak jatuh di bawah batas minimum
	 * yang dapat menyebabkan layout kolaps atau tidak dapat digulir.
	 *
	 * <p><b>Tujuan:</b> Mencegah nilai tinggi negatif atau nol yang terjadi ketika
	 * konfigurasi tinggi frame tidak tersedia atau menghasilkan nilai yang terlalu
	 * kecil setelah pengurangan offset.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah. Fungsi sederhana ini aman dan
	 * tidak memiliki efek samping.</p>
	 *
	 * @param value nilai integer yang akan diperiksa
	 * @param minimum nilai minimum yang diizinkan
	 * @return {@code value} jika lebih besar atau sama dengan minimum; {@code minimum} jika tidak
	 */
	private static int safeInt(int value, int minimum) {
		return value < minimum ? minimum : value;
	}

	/**
	 * Menghitung tinggi halaman transfer yang sesuai berdasarkan konfigurasi tinggi
	 * frame aplikasi dan mode tampilan (mobile/desktop).
	 *
	 * <p><b>Tujuan:</b> Mendapatkan nilai tinggi dasar yang akan digunakan untuk
	 * mengatur tinggi berbagai komponen layout agar halaman dapat digulir dengan
	 * nyaman tanpa konten terpotong.</p>
	 *
	 * <p><b>Cara kerja:</b> Membaca konfigurasi tinggi frame dari
	 * {@link MainAction#getConfiguredFrameMinimumHeight(boolean)} dengan mempertimbangkan
	 * apakah pengguna dalam mode mobile (menghasilkan nilai lebih kecil) atau desktop.
	 * Nilai minimum adalah 3000px untuk mobile atau 2200px untuk desktop.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika layout halaman berubah secara signifikan (misalnya
	 * ada header atau footer tambahan), sesuaikan nilai minimum di sini.</p>
	 *
	 * @return tinggi halaman dalam piksel sebagai integer
	 */
	private int getTransferPageHeight() {
		int tinggi = MainAction.getConfiguredFrameMinimumHeight(Common.isMobile());
		return safeInt(tinggi, Common.isMobile() ? 3000 : 2200);
	}

	/**
	 * Menerapkan tinggi adaptif ke semua komponen layout utama halaman daftar
	 * pengajuan transfer agar konten tidak terpotong di layar kecil maupun besar.
	 *
	 * <p><b>Tujuan:</b> Memastikan halaman daftar pengajuan transfer dapat digulir
	 * dengan benar di semua ukuran layar. Masalah tinggi yang tidak tepat dapat
	 * menyebabkan konten grid tersembunyi atau scroll tidak berfungsi. Metode ini
	 * mengatasi keterbatasan ZK dalam menghitung tinggi otomatis untuk layout
	 * bersarang yang kompleks.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menghitung tinggi dasar via {@code getTransferPageHeight()}.</li>
	 *   <li>Menurunkan tinggi untuk komponen bersarang: panel (-50), layout (-80),
	 *       wrapper list (-220), grid data (-300) dari tinggi dasar.</li>
	 *   <li>Menerapkan height dan style CSS ke: window, mainTabbox, mainTabpanels,
	 *       statistik, daftarTransferPanel, daftarTransferLayout, filterGrid,
	 *       listWrapperGrid, dan grid.</li>
	 *   <li>Setiap komponen mendapat min-height dan overflow-auto untuk memastikan
	 *       scroll bekerja dengan benar.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Seluruh operasi dibungkus dalam try-catch yang
	 * melaporkan error hanya ke admin via {@link Common#tampilErrorJikaAdmin}.
	 * Ini mencegah error penghitungan tinggi dari merusak ketersediaan halaman.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada komponen baru yang ditambahkan ke layout
	 * halaman, tambahkan pengaturan tingginya di sini. Offset tinggi (-50, -80, dll.)
	 * perlu disesuaikan jika ada perubahan layout yang menambah elemen baru di
	 * luar grid utama.</p>
	 */
	private void applyTransferListHeight() {
		try {
			int tinggi = getTransferPageHeight();
			String tinggiHalaman = tinggi + "px";
			String tinggiPanel = (tinggi - 50) + "px";
			String tinggiLayout = (tinggi - 80) + "px";
			String tinggiList = (tinggi - 220) + "px";
			String tinggiGrid = (tinggi - 300) + "px";

			if (window != null) {
				window.setHeight("100%");
				window.setStyle("width:100%;height:100%;min-height:" + tinggiHalaman + ";overflow:auto;box-sizing:border-box;");
			}
			if (mainTabbox != null) {
				mainTabbox.setWidth("100%");
				mainTabbox.setHeight("100%");
				mainTabbox.setStyle("width:100%;height:100%;min-height:" + tinggiHalaman + ";overflow:visible;box-sizing:border-box;");
			}
			if (mainTabpanels != null) {
				mainTabpanels.setHeight("100%");
				mainTabpanels.setStyle("width:100%;height:100%;min-height:" + tinggiPanel + ";overflow:auto;box-sizing:border-box;");
			}
			if (statistik != null) {
				statistik.setHeight("100%");
				statistik.setStyle("min-height:" + tinggiPanel + ";overflow:auto;box-sizing:border-box;");
			}
			if (daftarTransferPanel != null) {
				daftarTransferPanel.setHeight("100%");
				daftarTransferPanel.setStyle("min-height:" + tinggiPanel + ";overflow:auto;padding-bottom:80px;box-sizing:border-box;background:#f8fafc;");
			}
			if (daftarTransferLayout != null) {
				daftarTransferLayout.setWidth("100%");
				daftarTransferLayout.setHeight(tinggiLayout);
				daftarTransferLayout.setStyle("width:100%;min-height:" + tinggiLayout + ";overflow:visible;box-sizing:border-box;background:#f8fafc;");
			}
			if (filterGrid != null) {
				filterGrid.setWidth("100%");
				filterGrid.setStyle("width:100%;overflow:visible;box-sizing:border-box;");
			}
			if (listWrapperGrid != null) {
				listWrapperGrid.setWidth("100%");
				listWrapperGrid.setHeight(tinggiList);
				listWrapperGrid.setStyle("width:100%;height:" + tinggiList + ";min-height:" + tinggiList + ";overflow:auto;padding-bottom:90px;box-sizing:border-box;");
			}
			if (grid != null) {
				grid.setWidth("100%");
				grid.setHeight(tinggiGrid);
				grid.setStyle("width:100%;height:" + tinggiGrid + ";min-height:" + tinggiGrid + ";overflow:auto;padding-bottom:90px;box-sizing:border-box;");
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menangani event klik tab "Statistik" yang memuat konten dasbor statistik
	 * secara lazy (hanya saat tab pertama kali diklik) untuk menghindari
	 * loading berlebihan saat halaman pertama dibuka.
	 *
	 * <p><b>Tujuan:</b> Mengimplementasikan lazy loading untuk tab dasbor statistik
	 * yang berisi dua sub-dasbor berat ({@link DasboardDaftarPengajuanTransfer}
	 * dan {@link DasboardDaftarPengajuanTransferPerJenis}). Dengan lazy loading,
	 * waktu muat halaman awal lebih cepat karena dasbor statistik tidak diinisialisasi
	 * hingga pengguna benar-benar membuka tab tersebut.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memeriksa apakah panel statistik null; jika null, return.</li>
	 *   <li>Memeriksa apakah panel statistik sudah memiliki anak (sudah dimuat);
	 *       jika sudah, return (tidak perlu memuat ulang).</li>
	 *   <li>Menghitung tinggi minimal konten dari konfigurasi frame.</li>
	 *   <li>Membangun Tabbox dengan dua tab: "Ringkasan Proses" dan "Ringkasan Jenis".</li>
	 *   <li>Tab "Ringkasan Proses" langsung diisi dengan panel info dan
	 *       {@link DasboardDaftarPengajuanTransfer}.</li>
	 *   <li>Tab "Ringkasan Jenis" dimuat secara lazy — konten hanya dibuat saat
	 *       tab diklik pertama kali (cek tabpanel2.getChildren().isEmpty()).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari pembuatan komponen dasbor
	 * disebarkan ke ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada tab statistik baru yang perlu ditambahkan,
	 * tambahkan di sini dengan pola lazy loading yang sama. Jangan memuat konten
	 * berat di konstruktor atau doAfterCompose; gunakan pola lazy ini.</p>
	 *
	 * @param event event ZK yang memicu pembukaan tab statistik; bisa null
	 */
	public void onStatistik(Event event) {

		if (statistik == null) {
			return;
		}
		if (statistik.getChildren().size() == 0) {

			int tinggiMinimal = ais.action.maintenance.MainAction
					.getConfiguredFrameMinimumHeight(Common.isMobile()) - 190;
			if (tinggiMinimal < 860) {
				tinggiMinimal = 860;
			}
			final String tinggiIsi = (tinggiMinimal - 120) + "px";

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(statistik);
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");
			tabbox.setStyle("border:0;background:#f8fafc;min-height:" + tinggiMinimal + "px;");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tab1 = new MyTabConfig("Ringkasan Proses");
			tab1.setParent(tabs);
			tab1.setSelected(true);

			MyTabConfig tab2 = new MyTabConfig("Ringkasan Jenis");
			tab2.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);
			tabpanels.setHeight("100%");
			tabpanels.setStyle("height:100%;min-height:" + (tinggiMinimal - 50) + "px;");

			Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
			tabpanel1.setParent(tabpanels);
			tabpanel1.setHeight("100%");
			tabpanel1.setStyle("padding:0;background:#f8fafc;overflow:auto;min-height:" + (tinggiMinimal - 60) + "px;");

			final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
			tabpanel2.setParent(tabpanels);
			tabpanel2.setHeight("100%");
			tabpanel2.setStyle("padding:0;background:#f8fafc;overflow:auto;min-height:" + (tinggiMinimal - 60) + "px;");

			tabpanel1.appendChild(new Html(panelInfo("Ringkasan Proses",
					"Memperlihatkan berapa pengajuan yang belum diproses, sedang diajukan, masuk transitori, atau sudah selesai transfer. Informasi ini membantu memantau pekerjaan harian tanpa harus membuka data satu per satu.")));

			DasboardDaftarPengajuanTransfer include = new DasboardDaftarPengajuanTransfer();
			include.setHeight(tinggiIsi);
			include.setWidth("100%");
			include.setParent(tabpanel1);

			tab2.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel2.getChildren().isEmpty()) {
						tabpanel2.appendChild(new Html(panelInfo("Ringkasan Jenis",
								"Mengelompokkan pengajuan berdasarkan jenis kebutuhan, seperti uang muka, LPJ, kas kecil, termin, DP, diskon, dan pajak. Data ini memudahkan bagian keuangan melihat jenis pekerjaan yang paling banyak muncul.")));

						DasboardDaftarPengajuanTransferPerJenis include = new DasboardDaftarPengajuanTransferPerJenis();
						include.setHeight(tinggiIsi);
						include.setWidth("100%");
						include.setParent(tabpanel2);
					}
				}
			});

		}
	}

	/**
	 * Dipanggil ZK sebelum komponen halaman dibangun. Memverifikasi hak akses
	 * keamanan melalui {@link Common#doCheckSecurity()}.
	 *
	 * <p><b>Tujuan:</b> Gerbang keamanan awal yang memastikan hanya pengguna
	 * dengan akses valid yang dapat membuka halaman daftar pengajuan transfer.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link Common#doCheckSecurity()} lalu
	 * mendelegasikan ke super untuk melanjutkan proses komposisi ZK.</p>
	 *
	 * <p><b>Penanganan error:</b> Jika akses tidak sah, proses dihentikan dan
	 * pengguna diarahkan ke halaman logoff.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah kecuali ada kebutuhan keamanan
	 * tambahan sebelum komponen dibangun.</p>
	 *
	 * @param page halaman ZK yang sedang diproses
	 * @param parent komponen induk dalam hierarki ZK
	 * @param compInfo metadata komponen dari ZUL
	 * @return ComponentInfo dari implementasi super
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Dipanggil ZK setelah seluruh komponen halaman selesai di-autowire. Menginisialisasi
	 * halaman daftar pengajuan transfer: bahasa, tinggi layout adaptif, dasbor statistik
	 * lazy, filter satuan kerja, rentang tanggal, paging, timer refresh, dan tombol ekspor.
	 *
	 * <p><b>Tujuan:</b> Menyiapkan halaman daftar pengajuan transfer secara lengkap
	 * agar siap digunakan untuk pemantauan dan pengelolaan alur transfer keuangan
	 * organisasi.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membangun MyButtonTabbox dengan 5 tab: Dasbor (lazy), Proses Transfer
	 *       (lazy include), Daftar Transfer (eager sub-ZUL), Transitori (lazy include),
	 *       Cara Transfer (lazy include).</li>
	 *   <li>Memanggil super.doAfterComposeOri untuk meng-autowire field dari sub-ZUL eager.</li>
	 *   <li>Memanggil {@code applyTransferListHeight()} untuk mengatur tinggi awal.</li>
	 *   <li>Mendaftarkan listener pada {@code searchparent} untuk auto-search saat
	 *       satuan kerja filter berubah.</li>
	 *   <li>Menginisialisasi {@link SatuanKerjaTreeModel}.</li>
	 *   <li>Menetapkan datebox start/end sebagai readonly.</li>
	 *   <li>Mengisi start: 6 bulan lalu, end: besok.</li>
	 *   <li>Memanggil {@code onSearchDefault} untuk data awal.</li>
	 *   <li>Menginisialisasi paging dengan listener.</li>
	 *   <li>Membuat tombol cetak Excel via {@link Common#cetakData} menggunakan
	 *       array {@code contents} dan menambahkannya ke toolbar.</li>
	 *   <li>Membuat timer untuk refresh data dan tinggi layout secara berkala.</li>
	 *   <li>Memanggil {@code applyTransferListHeight()} sekali lagi di akhir untuk
	 *       memastikan tinggi sudah benar setelah semua komponen termuat.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception disebarkan ke ZK. Sesi tidak valid
	 * tidak ada pengecekan eksplisit di sini karena sudah ditangani di
	 * {@code doBeforeCompose} via doCheckSecurity.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada inisialisasi baru yang diperlukan (misalnya
	 * filter combobox baru), tambahkan setelah blok inisialisasi yang sudah ada.
	 * Timer ganda (statistik + refresh) adalah desain yang disengaja untuk
	 * memisahkan inisialisasi dasbor dari refresh periodik data utama.</p>
	 *
	 * @param comp komponen root halaman ZK
	 * @throws Exception jika terjadi kesalahan saat inisialisasi
	 */
	public void doAfterCompose(Component comp) throws Exception {
		Div mainContainer = (Div) comp.getFellow("mainContainer");
		int[] tabAktif = {0};
		MyButtonTabbox btabs = MyButtonTabbox.buat(mainContainer, "100%", tabAktif);

		// Tab 0: Dasbor — lazy, reproduces onStatistik logic directly into panel
		// (statistik field will be null after conversion; onStatistik guards with null check)
		btabs.tambahTabLazy(0, "Dasbor", new MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				int tinggiMinimal = ais.action.maintenance.MainAction
						.getConfiguredFrameMinimumHeight(Common.isMobile()) - 190;
				if (tinggiMinimal < 860) tinggiMinimal = 860;
				final String tinggiIsi = (tinggiMinimal - 120) + "px";

				Tabbox tabbox = new Tabbox();
				tabbox.setParent(panel);
				tabbox.setHeight("100%");
				tabbox.setWidth("100%");
				tabbox.setStyle("border:0;background:#f8fafc;min-height:" + tinggiMinimal + "px;");

				Tabs tabs = new Tabs();
				tabs.setParent(tabbox);

				MyTabConfig tab1 = new MyTabConfig("Ringkasan Proses");
				tab1.setParent(tabs);
				tab1.setSelected(true);

				MyTabConfig tab2 = new MyTabConfig("Ringkasan Jenis");
				tab2.setParent(tabs);

				Tabpanels tabpanels = new Tabpanels();
				tabpanels.setParent(tabbox);
				tabpanels.setHeight("100%");
				tabpanels.setStyle("height:100%;min-height:" + (tinggiMinimal - 50) + "px;");

				Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
				tabpanel1.setParent(tabpanels);
				tabpanel1.setHeight("100%");
				tabpanel1.setStyle("padding:0;background:#f8fafc;overflow:auto;min-height:" + (tinggiMinimal - 60) + "px;");

				final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
				tabpanel2.setParent(tabpanels);
				tabpanel2.setHeight("100%");
				tabpanel2.setStyle("padding:0;background:#f8fafc;overflow:auto;min-height:" + (tinggiMinimal - 60) + "px;");

				tabpanel1.appendChild(new Html(panelInfo("Ringkasan Proses",
						"Memperlihatkan berapa pengajuan yang belum diproses, sedang diajukan, masuk transitori, atau sudah selesai transfer. Informasi ini membantu memantau pekerjaan harian tanpa harus membuka data satu per satu.")));

				DasboardDaftarPengajuanTransfer include = new DasboardDaftarPengajuanTransfer();
				include.setHeight(tinggiIsi);
				include.setWidth("100%");
				include.setParent(tabpanel1);

				tab2.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanel2.getChildren().isEmpty()) {
							tabpanel2.appendChild(new Html(panelInfo("Ringkasan Jenis",
									"Mengelompokkan pengajuan berdasarkan jenis kebutuhan, seperti uang muka, LPJ, kas kecil, termin, DP, diskon, dan pajak. Data ini memudahkan bagian keuangan melihat jenis pekerjaan yang paling banyak muncul.")));
							DasboardDaftarPengajuanTransferPerJenis inc2 = new DasboardDaftarPengajuanTransferPerJenis();
							inc2.setHeight(tinggiIsi);
							inc2.setWidth("100%");
							inc2.setParent(tabpanel2);
						}
					}
				});
			}
		});

		// Tab 1: Proses Transfer (include)
		btabs.tambahTabLazy(1, "Proses Transfer", new MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				MyButtonTabbox.muatZul(panel,
						"/WEB-INF/z/x/y/pages/master/akunting/proses_transfer.zul");
			}
		});

		// Tab 2: Daftar Transfer (inline → eager sub-ZUL, loaded before super for field wiring)
		Div panelDaftarTransfer = btabs.tambahTab(2, "Daftar Transfer");
		MyButtonTabbox.muatZulEager(panelDaftarTransfer,
				"/WEB-INF/z/x/y/pages/master/akunting/daftar_pengajuan_transfer_tab_2.zul");

		// Tab 3: Transitori (include)
		btabs.tambahTabLazy(3, "Transitori", new MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				MyButtonTabbox.muatZul(panel,
						"/WEB-INF/z/x/y/pages/master/akunting/transitori.zul");
			}
		});

		// Tab 4: Cara Transfer (include)
		btabs.tambahTabLazy(4, "Cara Transfer", new MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				MyButtonTabbox.muatZul(panel,
						"/WEB-INF/z/x/y/pages/master/akunting/cara_pembayaran_transfer.zul");
			}
		});

		super.doAfterCompose(comp);
		btabs.pulihkanSeleksi(5);

		Common.initLaguage();
		applyTransferListHeight();

		// statistik field is null after conversion; onStatistik returns early — kept for compatibility
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onStatistik(null);
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

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(DaftarPengajuanTransfer.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		// Tombol "Singkronkan": periksa ulang & sinkronkan semua tabel referensi ke Daftar Transfer
		// (multi-thread + bar progres). Setelah selesai, daftar di-refresh.
		final Component compRoot = comp;
		MyToolbarbuttonConfig singkronToolbarbutton = new MyToolbarbuttonConfig("Singkronkan",
				"/img/svg/refresh-cw.svg");
		singkronToolbarbutton.setTooltiptext("Periksa ulang & sinkronkan semua data referensi ke Daftar Transfer");
		singkronToolbarbutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event evSingkron) throws Exception {
				ais.action.master.akunting.helper.SinkronDaftarPengajuanTransferHelper.sinkronkan(compRoot,
						new EventListener() {
							@Override
							public void onEvent(Event evSelesai) throws Exception {
								onSearchDefault(null);
								applyTransferListHeight();
							}
						});
			}
		});
		Common.appendKeToolbar(singkronToolbarbutton, find, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
				applyTransferListHeight();
			}
		});
		applyTransferListHeight();
	}

	/**
	 * Inner class renderer yang mengisi setiap baris grid dengan data dari satu
	 * objek {@link DaftarPengajuanTransfer}, termasuk status proses transfer
	 * dan field keterangan yang dapat diedit langsung.
	 *
	 * <p><b>Tujuan:</b> Menampilkan informasi pengajuan transfer dengan kolom:
	 * Nama/Kode (dengan link revisi), Waktu Pengajuan, Akun, Rekening Sumber,
	 * Nominal, Status Proses, Keterangan (editable inline), dan link SOP jika ada.
	 * Keterangan yang dapat diedit langsung mengurangi kebutuhan membuka formulir
	 * edit hanya untuk perubahan catatan minor.</p>
	 *
	 * <p><b>Cara kerja:</b> Status proses transfer ditentukan dengan logika
	 * bertingkat: jika {@code prosesTransfer} null → "Belum diproses"; jika ada
	 * tapi {@code realisasikanOleh} null → "Sudah diajukan"; jika
	 * {@code transfer=true} → "Sudah Transfer"; jika {@code transitori=true} →
	 * "Transitori"; selain itu → "Diajukan". Keterangan cara pembayaran ditambahkan
	 * dalam tanda kurung jika ada. Textbox keterangan menyimpan langsung saat onChange.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada status baru pada alur transfer (misalnya
	 * "Ditolak" atau "Dalam Verifikasi"), tambahkan kondisi pada logika status di
	 * dalam metode render. Pastikan urutan appendChild konsisten dengan kolom ZUL.</p>
	 */
	class DaftarPengajuanTransferRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Mengisi satu baris grid dengan data dari satu objek {@link DaftarPengajuanTransfer}.
		 *
		 * <p><b>Tujuan:</b> Menampilkan informasi lengkap pengajuan transfer dalam
		 * grid yang informatif, dengan status transfer yang mudah dibaca, keterangan
		 * yang dapat diedit langsung, dan link ke alur SOP persetujuan jika ada.</p>
		 *
		 * <p><b>Cara kerja:</b>
		 * <ol>
		 *   <li>Mengcast {@code arg1} ke {@link DaftarPengajuanTransfer}.</li>
		 *   <li>Menampilkan nama (via RevisiHelper dengan link audit trail) dan kode
		 *       sebagai sub-label dalam Vbox yang sama.</li>
		 *   <li>Menampilkan waktu pengajuan yang diformat.</li>
		 *   <li>Menampilkan info akun dalam format "kode-nama".</li>
		 *   <li>Menampilkan info rekening sumber (bank, atas nama, nomor rekening)
		 *       dalam Vbox bertumpuk.</li>
		 *   <li>Menampilkan nominal yang diformat.</li>
		 *   <li>Menampilkan status transfer dengan logika bertingkat.</li>
		 *   <li>Menampilkan Textbox keterangan yang bisa diedit. Saat onChange,
		 *       menyimpan langsung ke database via refreshSaveOrUpdate.</li>
		 *   <li>Menampilkan link SOP jika ada disposisiSop, menggunakan
		 *       {@link UIClassHelper#applyReadMore} untuk teks panjang.</li>
		 *   <li>Jika tidak ada SOP, menambahkan Label kosong agar kolom tidak geser.</li>
		 * </ol>
		 * </p>
		 *
		 * <p><b>Penanganan error:</b> NullPointerException ditangani dengan operator
		 * ternary. Exception dari penyimpanan keterangan disebarkan ke ZK.</p>
		 *
		 * <p><b>Pemeliharaan:</b> Logika status transfer perlu diselaraskan jika ada
		 * status baru yang ditambahkan ke entitas. Urutan appendChild harus sesuai
		 * urutan kolom di ZUL daftar_pengajuan_transfer.zul.</p>
		 *
		 * @param arg0 baris grid ZK yang akan diisi
		 * @param arg1 objek data yang akan dicast ke {@link DaftarPengajuanTransfer}
		 * @throws Exception jika terjadi kesalahan rendering atau penyimpanan keterangan
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(DaftarPengajuanTransfer.class, daftarPengajuanTransfer,
					text(daftarPengajuanTransfer.getNama()))).setParent(arg0);

			new Label(text(daftarPengajuanTransfer.getKode())).setParent(a);

			// Badge jenis: PPh/Pajak atau Tagihan Vendor
			if (daftarPengajuanTransfer.getPajak() != null) {
				String kodePengajuan = daftarPengajuanTransfer.getPajak().getKode();
				if (kodePengajuan != null && !kodePengajuan.isEmpty()) {
					Label lblKode = new Label(kodePengajuan);
					lblKode.setStyle("font-size:10px; color:#4b5563;");
					lblKode.setParent(a);
				}
				// Kode PO + kode pengajuan pembayaran termin (tambahan permintaan): untuk pajak TERMIN
				// (Opsi B, relasi induk lain null, keyData = id termin-detail) ditelusuri keyData →
				// termin-detail → PO.getKode() dan PembayaranTerminMasterAsset.getKode() (mis.
				// PO: 0095/PO/...  dan  Termin: 124/TRM/YTB/VI/2026). Detail dimuat SEKALI.
				try {
					if (daftarPengajuanTransfer.getPajak().getKeyData() != null
							&& daftarPengajuanTransfer.getPajak().getSaldoAwalMasterAssetDetail() == null
							&& daftarPengajuanTransfer.getPajak().getSaldoAwal() == null
							&& daftarPengajuanTransfer.getPajak().getPertangungjawaban() == null
							&& daftarPengajuanTransfer.getPajak().getPertangungjawabanKasBesar() == null) {
						ais.database.model.asset.PembayaranTerminMasterAssetDetail terminDetail = (ais.database.model.asset.PembayaranTerminMasterAssetDetail) HibernateUtil
								.currentSession().get(
										ais.database.model.asset.PembayaranTerminMasterAssetDetail.class,
										daftarPengajuanTransfer.getPajak().getKeyData());
						if (terminDetail != null) {
							// Kode PO
							if (terminDetail.getPemesananPengadaanMasterAsset() != null
									&& terminDetail.getPemesananPengadaanMasterAsset().getKode() != null
									&& !terminDetail.getPemesananPengadaanMasterAsset().getKode().trim().isEmpty()) {
								Label lblPo = new Label(
										"PO: " + terminDetail.getPemesananPengadaanMasterAsset().getKode().trim());
								lblPo.setStyle("font-size:10px; color:#4b5563;");
								lblPo.setParent(a);
							}
							// Kode pengajuan pembayaran termin
							if (terminDetail.getPembayaranTerminMasterAsset() != null
									&& terminDetail.getPembayaranTerminMasterAsset().getKode() != null
									&& !terminDetail.getPembayaranTerminMasterAsset().getKode().trim().isEmpty()) {
								Label lblTermin = new Label(
										"Termin: " + terminDetail.getPembayaranTerminMasterAsset().getKode().trim());
								lblTermin.setStyle("font-size:10px; color:#4b5563;");
								lblTermin.setParent(a);
							}
						}
					}
				} catch (Exception exTermin) { ais.common.ErrorAuditUtil.record(exTermin, "auto-audit(empty-catch) src/ais/action/master/akunting/DaftarPengajuanTransferAction.java:769");
					// abaikan; baris kode PO/termin bersifat opsional
				}
				Label badge = new Label("PPh / Pajak");
				badge.setStyle("font-size:10px; font-weight:bold; color:#fff;"
						+ " background:#d97706; padding:1px 7px; border-radius:9px;");
				badge.setParent(a);
			} else if (daftarPengajuanTransfer.getSaldoAwalMasterAsset() != null
					|| daftarPengajuanTransfer.getPembayaranPengadaanMasterAssetDetail() != null
					|| daftarPengajuanTransfer.getPembayaranTerminMasterAssetDetail() != null
					|| daftarPengajuanTransfer.getPembayaranDpMasterAssetDetail() != null) {
				// Kode PO induk (tambahan permintaan): telusuri PO dari jenis sumber tagihan vendor.
				// Termin & DP menyimpan PO langsung; Pengadaan & SaldoAwal lewat Penerimaan (BAST).
				ais.database.model.asset.PemesananPengadaanMasterAsset poVendor = null;
				try {
					if (daftarPengajuanTransfer.getPembayaranTerminMasterAssetDetail() != null) {
						poVendor = daftarPengajuanTransfer.getPembayaranTerminMasterAssetDetail()
								.getPemesananPengadaanMasterAsset();
					} else if (daftarPengajuanTransfer.getPembayaranDpMasterAssetDetail() != null) {
						poVendor = daftarPengajuanTransfer.getPembayaranDpMasterAssetDetail()
								.getPemesananPengadaanMasterAsset();
					} else if (daftarPengajuanTransfer.getPembayaranPengadaanMasterAssetDetail() != null
							&& daftarPengajuanTransfer.getPembayaranPengadaanMasterAssetDetail()
									.getSaldoAwalMasterAsset() != null
							&& daftarPengajuanTransfer.getPembayaranPengadaanMasterAssetDetail()
									.getSaldoAwalMasterAsset().getPenerimaanPengadaanMasterAsset() != null) {
						poVendor = daftarPengajuanTransfer.getPembayaranPengadaanMasterAssetDetail()
								.getSaldoAwalMasterAsset().getPenerimaanPengadaanMasterAsset()
								.getPemesananPengadaanMasterAsset();
					} else if (daftarPengajuanTransfer.getSaldoAwalMasterAsset() != null
							&& daftarPengajuanTransfer.getSaldoAwalMasterAsset()
									.getPenerimaanPengadaanMasterAsset() != null) {
						poVendor = daftarPengajuanTransfer.getSaldoAwalMasterAsset()
								.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset();
					}
				} catch (Exception exPo) {
					poVendor = null;
				}
				if (poVendor != null && poVendor.getKode() != null && !poVendor.getKode().trim().isEmpty()) {
					Label lblPo = new Label("PO: " + poVendor.getKode().trim());
					lblPo.setStyle("font-size:10px; color:#4b5563;");
					lblPo.setParent(a);
				}
				Label badge = new Label(ais.common.Common.getBahasaConfig("Tagihan Vendor"));
				badge.setStyle("font-size:10px; font-weight:bold; color:#fff;"
						+ " background:#2563eb; padding:1px 7px; border-radius:9px;");
				badge.setParent(a);
			}

			new Label(daftarPengajuanTransfer.getWaktu() == null ? ""
					: Common.dateFormat.get().format(daftarPengajuanTransfer.getWaktu())).setParent(arg0);

			new Label(daftarPengajuanTransfer.getAkun() == null ? ""
					: daftarPengajuanTransfer.getAkun().getKode() + "-" + daftarPengajuanTransfer.getAkun().getNama())
					.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			// Kolom Bank: nama bank + atas nama pemilik rekening + nomor rekening tujuan transfer.
			// Sumber data sama dengan kolom Akun (mis. untuk PPh = akun pajak yang bersangkutan).
			// Rekening PENYEDIA (vendor): untuk baris pembayaran vendor, utamakan rekening penyedia
			// bila sudah diisi. Untuk baris pajak, penyediaVendor = null → tetap pakai rekening akun.
			ais.database.model.asset.PenyediaAsset penyediaVendor = null;
			try {
				ais.database.model.asset.PemesananPengadaanMasterAsset poBank = null;
				if (daftarPengajuanTransfer.getPembayaranTerminMasterAssetDetail() != null) {
					poBank = daftarPengajuanTransfer.getPembayaranTerminMasterAssetDetail()
							.getPemesananPengadaanMasterAsset();
				} else if (daftarPengajuanTransfer.getPembayaranDpMasterAssetDetail() != null) {
					poBank = daftarPengajuanTransfer.getPembayaranDpMasterAssetDetail()
							.getPemesananPengadaanMasterAsset();
				} else if (daftarPengajuanTransfer.getPembayaranPengadaanMasterAssetDetail() != null
						&& daftarPengajuanTransfer.getPembayaranPengadaanMasterAssetDetail()
							.getSaldoAwalMasterAsset() != null
						&& daftarPengajuanTransfer.getPembayaranPengadaanMasterAssetDetail()
							.getSaldoAwalMasterAsset().getPenerimaanPengadaanMasterAsset() != null) {
					poBank = daftarPengajuanTransfer.getPembayaranPengadaanMasterAssetDetail()
							.getSaldoAwalMasterAsset().getPenerimaanPengadaanMasterAsset()
							.getPemesananPengadaanMasterAsset();
				} else if (daftarPengajuanTransfer.getSaldoAwalMasterAsset() != null
						&& daftarPengajuanTransfer.getSaldoAwalMasterAsset()
							.getPenerimaanPengadaanMasterAsset() != null) {
					poBank = daftarPengajuanTransfer.getSaldoAwalMasterAsset()
							.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset();
				}
				if (poBank != null) {
					penyediaVendor = poBank.getPenyedia();
				}
			} catch (Exception exPenyedia) {
				penyediaVendor = null;
			}
			String bankVendor = penyediaVendor == null ? "" : text(penyediaVendor.getBank());
			String atasNamaVendor = penyediaVendor == null ? "" : text(penyediaVendor.getAtasNama());
			String noRekVendor = penyediaVendor == null ? "" : text(penyediaVendor.getNoRek());
			boolean adaRekVendor = bankVendor.trim().length() > 0 || noRekVendor.trim().length() > 0;

			String namaBank = daftarPengajuanTransfer.getBankSumber() == null ? ""
					: text(daftarPengajuanTransfer.getBankSumber().getNama());
			String atasNama = text(daftarPengajuanTransfer.getAtasNamaSumber());
			String noRek = text(daftarPengajuanTransfer.getNoRekSumber());

			boolean adaInfoBank = namaBank.trim().length() > 0 || atasNama.trim().length() > 0
					|| noRek.trim().length() > 0;

			if (adaRekVendor) {
				// Pembayaran ke vendor & rekening penyedia sudah diisi → pakai rekening penyedia.
				if (bankVendor.trim().length() > 0) {
					Label labelBank = new Label(bankVendor);
					labelBank.setStyle("font-weight:bold;");
					labelBank.setParent(vbox);
				}
				if (atasNamaVendor.trim().length() > 0) {
					new Label("a.n. " + atasNamaVendor).setParent(vbox);
				}
				if (noRekVendor.trim().length() > 0) {
					Label labelNoRek = new Label("No. Rek: " + noRekVendor);
					labelNoRek.setStyle("font-size:11px; color:#374151;");
					labelNoRek.setParent(vbox);
				}
			} else if (!adaInfoBank) {
				// Tidak ada rekening pada akun terkait → beri petunjuk agar diisi di master Akun.
				Label hint = new Label(penyediaVendor != null ? "Rekening penyedia belum diisi"
						: "Rekening belum diatur pada akun");
				hint.setStyle("font-size:10px; color:#9ca3af; font-style:italic;");
				hint.setParent(vbox);
			} else {
				if (namaBank.trim().length() > 0) {
					Label labelBank = new Label(namaBank);
					labelBank.setStyle("font-weight:bold;");
					labelBank.setParent(vbox);
				}
				if (atasNama.trim().length() > 0) {
					new Label("a.n. " + atasNama).setParent(vbox);
				}
				if (noRek.trim().length() > 0) {
					Label labelNoRek = new Label("No. Rek: " + noRek);
					labelNoRek.setStyle("font-size:11px; color:#374151;");
					labelNoRek.setParent(vbox);
				}
			}

			new Label(formatNilai(daftarPengajuanTransfer.getNominal())).setParent(arg0);

			new Label(daftarPengajuanTransfer.getProsesTransfer() == null ? "Belum diproses"
					: daftarPengajuanTransfer.getProsesTransfer() != null
							&& daftarPengajuanTransfer.getProsesTransfer().getRealisasikanOleh() == null
									? "Sudah diajukan"
									: (isTrue(daftarPengajuanTransfer.getTransfer()) ? "Sudah Transfer"
											: (isTrue(daftarPengajuanTransfer.getTransitori()) ? "Transitori" : "Diajukan"))
											+ (daftarPengajuanTransfer.getProsesTransfer() == null
													|| daftarPengajuanTransfer.getProsesTransfer()
															.getCaraPembayaranTransfer() == null
																	? ""
																	: " (" + daftarPengajuanTransfer.getProsesTransfer()
																			.getCaraPembayaranTransfer().getNama()
																			+ ")"))
					.setParent(arg0);

			final Textbox keterangan = new Textbox(text(daftarPengajuanTransfer.getKeterangan()));
			keterangan.setWidth("95%");
			keterangan.setRows(2);
			keterangan.setParent(arg0);
			keterangan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					daftarPengajuanTransfer.setKeterangan(keterangan.getValue());
					Common.refreshSaveOrUpdate(daftarPengajuanTransfer);
				}
			});


			if (daftarPengajuanTransfer.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(arg0);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + text(daftarPengajuanTransfer.getDisposisiSop().getKeterangan())
						+ (daftarPengajuanTransfer.getDisposisiSop().getSop() == null ? ""
								: " (" + text(daftarPengajuanTransfer.getDisposisiSop().getSop().getNama()) + ")"));
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(daftarPengajuanTransfer.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			} else {
				new Label().setParent(arg0);
			}

		}

	}

	/**
	 * Membangun objek Hibernate Criteria untuk query daftar {@link DaftarPengajuanTransfer}
	 * berdasarkan semua filter aktif, dengan opsi pengurutan.
	 *
	 * <p><b>Tujuan:</b> Memusatkan logika query multi-filter di satu tempat untuk
	 * menghindari duplikasi antara perhitungan paging dan pengambilan data. Filter
	 * status yang tersedia memungkinkan bagian keuangan fokus pada subset pengajuan
	 * yang relevan dengan tugasnya saat ini.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memproses hierarki satuan kerja dari {@code searchparent}.</li>
	 *   <li>Membangun Criteria dengan LEFT JOIN ke {@code prosesTransfer} agar
	 *       field proses transfer dapat digunakan dalam filter.</li>
	 *   <li>Menerapkan filter berlapis:
	 *     <ul>
	 *       <li>Satuan kerja: null (global) ATAU dalam set satuan kerja pengguna.</li>
	 *       <li>Rentang tanggal waktu pengajuan (jika start dan end tidak null).</li>
	 *       <li>Status aktif: hanya aktif jika checkbox aktif.</li>
	 *       <li>Filter belum: prosesTransfer IS NULL.</li>
	 *       <li>Filter sudah: prosesTransfer IS NOT NULL AND realisasikanOleh IS NULL.</li>
	 *       <li>Filter sudah transfer: transfer=true AND prosesTransfer.realisasikanOleh IS NOT NULL.</li>
	 *       <li>Filter transitori: transitori=true AND prosesTransfer.realisasikanOleh IS NOT NULL.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Filter teks pencarian (kode ILIKE ATAU nama ILIKE).</li>
	 *   <li>Jika {@code order} true, tambahkan ORDER BY id DESC.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate disebarkan ke caller.
	 * Filter tanggal menggunakan SQL native date() untuk perbandingan tanpa waktu.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Filter status (belum/sudah/sudahtrnf/transitori) bersifat
	 * additif (masing-masing adalah filter independen). Jika ada status baru, tambahkan
	 * field Checkbox baru dan klausa filter yang sesuai.</p>
	 *
	 * @param order jika true, tambahkan ORDER BY id DESC ke query
	 * @return objek {@link Criteria} yang siap dieksekusi
	 */
	/**
	 * Filter status DPT sebagai OR (bukan AND). Semua status yang DICENTANG digabung dengan OR;
	 * tanpa centang → tanpa batasan (tampilkan semua). Selain memperbaiki bug "≥2 centang = kosong",
	 * "Ditransfer" direlaksasi menjadi "sudah direalisasi &amp; BUKAN transitori" sehingga status
	 * tampilan bare "Diajukan" (realisasi, transfer≠true, transitori≠true) — yang sebelumnya TIDAK
	 * tercakup filter mana pun — ikut terfilter. Memakai alias {@code prosesTransfer} dari initCriteria.
	 */
	private org.hibernate.criterion.Criterion filterStatus() {
		org.hibernate.criterion.Disjunction or = Restrictions.disjunction();
		boolean ada = false;
		if (searchbelum.isChecked()) {
			or.add(Restrictions.isNull("prosesTransfer"));
			ada = true;
		}
		if (searchsudah.isChecked()) {
			or.add(Restrictions.and(Restrictions.isNotNull("prosesTransfer"),
					Restrictions.isNull("prosesTransfer.realisasikanOleh")));
			ada = true;
		}
		if (searchsudahtrnf.isChecked()) {
			or.add(Restrictions.and(Restrictions.isNotNull("prosesTransfer.realisasikanOleh"),
					Restrictions.or(Restrictions.isNull("transitori"), Restrictions.eq("transitori", false))));
			ada = true;
		}
		if (searchTransitori.isChecked()) {
			or.add(Restrictions.and(Restrictions.eq("transitori", true),
					Restrictions.isNotNull("prosesTransfer.realisasikanOleh")));
			ada = true;
		}
		return ada ? or : Restrictions.sqlRestriction("true");
	}

	public Criteria initCriteria(boolean order) {
		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DaftarPengajuanTransfer.class)

				.createAlias("prosesTransfer", "prosesTransfer", Criteria.LEFT_JOIN)
				.createAlias("saldoAwalMasterAsset", "vendorSaldo", Criteria.LEFT_JOIN)
				.createAlias("vendorSaldo.penyedia", "vendorPenyedia", Criteria.LEFT_JOIN)
				// Pajak breakdown ("PPh Bukti Potong") tertaut ke tagihan vendor via pajak.saldoAwal;
				// join ini dipakai untuk menyembunyikannya saat tagihan BELUM selesai diajukan.
				.createAlias("pajak", "pjkBd", Criteria.LEFT_JOIN)
				.createAlias("pjkBd.saldoAwal", "pjkBdSaldo", Criteria.LEFT_JOIN)

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add((start == null || end == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (start.getValue() == null || end.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction("date(this_.waktu) between date('"
								+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				// Sembunyikan baris PPh BREAKDOWN ("PPh Bukti Potong") bila pengajuan pembayaran
				// tagihan vendor BELUM selesai diajukan. Pajak breakdown tertaut ke tagihan via
				// pajak.saldoAwal; "selesai diajukan" ditandai saldoAwal.disetujuiOleh != null
				// (lihat DaftarPengajuanTransfer.simpanSaldoAwalMasterAsset yang membuat DPT vendor
				// hanya setelah tagihan disetujui). DPT non-breakdown (pjkBdSaldo null) tidak terdampak.
				.add(Restrictions.or(Restrictions.isNull("pjkBdSaldo.id"),
						Restrictions.isNotNull("pjkBdSaldo.disetujuiOleh")))

				// ③ Filter status digabung OR (lihat filterStatus()). Sebelumnya tiap checkbox
				// status di-.add() terpisah = di-AND-kan Hibernate → mencentang ≥2 status
				// (mis. Belum diproses + Diajukan) selalu menghasilkan kosong (kondisi mustahil).
				.add(filterStatus());

		// Filter jenis pembayaran: pisahkan Tagihan Vendor dari PPh/Pajak
		String jenisPembayaran = filterJenisPembayaran == null || filterJenisPembayaran.getValue() == null
				? "Semua" : filterJenisPembayaran.getValue().trim();
		if ("Tagihan Vendor".equals(jenisPembayaran)) {
			criteria.add(Restrictions.isNull("pjkBd.id"));
		} else if ("PPh / Pajak".equals(jenisPembayaran)) {
			criteria.add(Restrictions.isNotNull("pjkBd.id"));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: buatFilterKodeJudul(session, searchnama.getValue().trim()));
		return criteria;
	}

	/** Cache resolusi DPT id untuk sepasang pemanggilan initCriteria (count + list). */
	private transient String cacheDptKodeSearch = null;
	private transient java.util.Set<Long> cacheDptIds = null;

	/**
	 * Filter "Kode/Judul": selain kode/nama/nama-penyedia/pjkBd.kode yang sudah ada, juga cocokkan
	 * kode INDUK termin (Termin / PO / BAST) — baik untuk baris tagihan vendor termin (relasi
	 * langsung) maupun baris PPh/Pajak termin (via keyData) — lewat OR in("id", ...).
	 */
	private org.hibernate.criterion.Criterion buatFilterKodeJudul(Session session, String search) {
		// Delegasi ke perkakas bersama agar tab "Proses Transfer" memakai pencarian yang sama.
		return ais.action.master.akunting.helper.DaftarPengajuanTransferSearchHelper.filterKodeJudul(session, search);
	}

	/**
	 * Kumpulkan id {@link DaftarPengajuanTransfer} yang kode INDUK termin-nya (Termin / PO / BAST)
	 * mengandung {@code search}. Baris tagihan vendor termin dicocokkan lewat relasi langsung
	 * {@code pembayaranTerminMasterAssetDetail}; baris PPh/Pajak termin lewat {@code pajak.keyData}
	 * (= id termin-detail). Hasil di-cache per string karena initCriteria dipanggil 2x.
	 */
	private java.util.Set<Long> dptIdByKodeInduk(Session session, String search) {
		if (search != null && search.equals(cacheDptKodeSearch) && cacheDptIds != null) {
			return cacheDptIds;
		}
		java.util.Set<Long> ids = new java.util.HashSet<Long>();
		if (session != null && search != null && !search.trim().isEmpty()) {
			String q = "%" + search.trim() + "%";
			final String DPT = "ais.database.model.akunting.DaftarPengajuanTransfer";
			final String BAST = "ais.database.model.asset.PenerimaanPengadaanMasterAsset";
			final String TD = "ais.database.model.asset.PembayaranTerminMasterAssetDetail";
			// (A) Baris TAGIHAN VENDOR termin (relasi langsung).
			tambahIdDpt(session, ids, "select dpt.id from " + DPT + " dpt "
					+ "where dpt.pembayaranTerminMasterAssetDetail.pembayaranTerminMasterAsset.kode like :q", q);
			tambahIdDpt(session, ids, "select dpt.id from " + DPT + " dpt "
					+ "where dpt.pembayaranTerminMasterAssetDetail.pemesananPengadaanMasterAsset.kode like :q", q);
			tambahIdDpt(session, ids, "select dpt.id from " + DPT + " dpt, " + BAST + " b "
					+ "where b.pemesananPengadaanMasterAsset = dpt.pembayaranTerminMasterAssetDetail.pemesananPengadaanMasterAsset "
					+ "and b.kode like :q", q);
			// (B) Baris PPh/PAJAK termin (via keyData = id termin-detail).
			java.util.Set<Long> td = new java.util.HashSet<Long>();
			tambahIdDpt(session, td, "select d.id from " + TD + " d where d.pembayaranTerminMasterAsset.kode like :q", q);
			tambahIdDpt(session, td, "select d.id from " + TD + " d where d.pemesananPengadaanMasterAsset.kode like :q", q);
			tambahIdDpt(session, td, "select d.id from " + TD + " d, " + BAST + " b "
					+ "where b.pemesananPengadaanMasterAsset = d.pemesananPengadaanMasterAsset and b.kode like :q", q);
			if (!td.isEmpty()) {
				try {
					@SuppressWarnings("rawtypes")
					java.util.List res = session.createQuery("select dpt.id from " + DPT + " dpt "
							+ "where dpt.pajak.keyData in (:ids) "
							+ "and dpt.pajak.saldoAwalMasterAssetDetail is null and dpt.pajak.saldoAwal is null "
							+ "and dpt.pajak.pertangungjawaban is null and dpt.pajak.pertangungjawabanKasBesar is null")
							.setParameterList("ids", td).list();
					for (Object o : res) {
						if (o instanceof Long) {
							ids.add((Long) o);
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}
		cacheDptKodeSearch = search;
		cacheDptIds = ids;
		return ids;
	}

	/** Jalankan satu query HQL (parameter {@code :q}) dan tambahkan id Long hasilnya ke {@code out}. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void tambahIdDpt(Session session, java.util.Set<Long> out, String hql, String qParam) {
		try {
			java.util.List hasil = session.createQuery(hql).setString("q", qParam).list();
			for (Object o : hasil) {
				if (o instanceof Long) {
					out.add((Long) o);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menjalankan pencarian daftar pengajuan transfer dan memperbarui grid beserta
	 * paging dengan hasil yang ditemukan, kemudian menyesuaikan tinggi layout.
	 *
	 * <p><b>Tujuan:</b> Handler utama event pencarian yang memuat data pengajuan
	 * transfer sesuai filter aktif dan menampilkannya di grid. Dipanggil saat
	 * filter berubah, paging dinavigasi, setelah operasi simpan, atau timer
	 * refresh berdetak.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menghitung total record untuk paging via {@link Common#initPaging}.</li>
	 *   <li>Mengambil data halaman aktif dengan limit dan offset paging.</li>
	 *   <li>Membuat SimpleListModel dan menyetel renderer ke grid.</li>
	 *   <li>Memanggil {@code applyTransferListHeight()} untuk memastikan tinggi
	 *       layout sudah benar setelah data dimuat.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate disebarkan ke ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Pemanggilan {@code applyTransferListHeight()} di sini
	 * memastikan tinggi diperbarui setiap kali data dimuat, menangani kasus di mana
	 * tinggi frame berubah di antara refresh (misalnya perubahan zoom browser).</p>
	 *
	 * @param event event ZK yang memicu pencarian; bisa null jika dipanggil programatis
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DaftarPengajuanTransfer> daftarPengajuanTransfer = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(daftarPengajuanTransfer);
		grid.setRowRenderer(new DaftarPengajuanTransferRenderer());
		grid.setModelCheckMobile(strset);
		applyTransferListHeight();

	}

}
