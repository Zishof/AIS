package ais.action.master.dashboard.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.maintenance.MainAction;
import ais.action.master.sekolah.SiswaAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * <h2>Dasbor Statistik Siswa (konteks sekolah) &mdash; wadah tabbox tab "Statistik"</h2>
 *
 * <p>Kelas ini adalah <b>wadah (container)</b> yang dipasang pada tab "Statistik" layar master
 * Siswa ({@code /pages/master/sekolah/siswa.zul}, ditangani {@link SiswaAction#onStatistik}).
 * Kelas ini sendiri <b>bukan</b> penggambar grafik; ia membangun sebuah {@link Tabbox} berisi
 * tiga tab dan mengisi salah satunya dengan tabel rekap buatannya sendiri:</p>
 *
 * <ol>
 *   <li><b>"Dasbor"</b> &mdash; tab pertama (terpilih otomatis). Diisi eager di {@link #init()}
 *       dengan satu instance {@link DashboardRingkasanSiswa}, yaitu dasbor grafik modern
 *       (donat gender, batang bertumpuk per angkatan, garis tren, radar antar-sekolah, dsb).
 *       Seluruh isi visual tab ini milik kelas tersebut, bukan kelas ini.</li>
 *   <li><b>"Data"</b> &mdash; satu-satunya tab yang benar-benar diisi oleh kelas ini.
 *       Berisi panel saringan (Yayasan, Sekolah, rentang Tahun Masuk) di {@code North} dan
 *       tabel rekap <i>jumlah siswa per sekolah per tahun masuk</i> di {@code Center},
 *       ditambah grafik batang ringkas dan tombol "Download". Dibangun oleh {@link #reload()}.</li>
 *   <li><b>"Pendidikan"</b> &mdash; <b>tab mati</b>. Listener {@code onClick}-nya ada, tetapi
 *       seluruh badan listener dikomentari, sehingga tab ini selamanya kosong. Sepuluh tab
 *       lain (Status, Agama, Usia, Tipe, Unit Kerja, PTKP, Ikatan, Atasan, Masa Kerja,
 *       Asuransi) juga masih tersisa sebagai blok komentar &mdash; jejak salin-tempel dari
 *       dasbor kepegawaian, bukan rencana aktif untuk modul siswa.</li>
 * </ol>
 *
 * <h3>Alur kerja tab "Data"</h3>
 * <p>{@link #init()} hanya membangun kerangka dan saringan; isi tabel dibangun {@link #reload()}
 * yang dipicu oleh (a) perubahan salah satu dari empat saringan, dan (b) timer bawaan
 * {@code Common.createDefaultTimer(...)} sehingga pemuatan pertama terjadi setelah halaman
 * selesai dirender. Untuk setiap sekolah dan setiap tahun dalam rentang, {@link #reload()}
 * menjalankan satu kueri {@code COUNT} terpisah pada {@link Siswa} &mdash; jumlah kueri adalah
 * <i>jumlah sekolah &times; jumlah tahun</i>, jadi memperlebar rentang tahun menaikkan beban
 * database secara linier (rentang bawaan 5 tahun terakhir).</p>
 * <p>Setiap angka pada tabel (termasuk baris "Total") dirender sebagai tautan {@link A} yang,
 * bila diklik, memanggil {@code Common.cetakDataCustomButton(Siswa.class, ...)} dengan daftar
 * kolom {@link SiswaAction#contents}. Artinya angka rekap bukan sekadar angka: ia adalah
 * <b>pintu ekspor Excel berisi data siswa lengkap per baris</b>.</p>
 *
 * <h3>STATUS KEAMANAN &mdash; RENTAN (kebocoran PII lintas sekolah/yayasan)</h3>
 * <p><b>Kesimpulan: kelas ini TIDAK aman.</b> Rinciannya sengaja dicatat lengkap karena
 * mekanismenya <i>berbeda</i> dari pola dasbor fail-open yang sudah dikenal di modul lain,
 * sehingga mudah salah diklasifikasikan:</p>
 * <ul>
 *   <li><b>Nol pemeriksaan peran.</b> Tidak ada satu pun cek {@code tbmuser.getSiswa()},
 *       {@code tbmuser.getOrangTua()}, {@code getGuru()}, maupun {@code CommonPrivilages.checkPrevilages(...)}
 *       di kelas ini. Ini <b>bukan</b> pola "memfilter siswa tetapi lupa orang tua" &mdash; di sini
 *       bahkan penyaringan untuk akun siswa pun tidak ada sama sekali, sehingga secara kategori
 *       lebih longgar daripada pola tersebut. Satu-satunya pemakaian
 *       {@code Common.getCurrentUser()} di kelas ini adalah untuk membaca tinggi desktop
 *       ({@code MainAction.desktopHeights}) demi mengatur CSS {@code min-height}, bukan untuk
 *       otorisasi.</li>
 *   <li><b>Saringan tenant dimatikan oleh dua ternary terbalik.</b> Lihat catatan rinci pada
 *       {@link #reload()}. Kombinasi {@link Combobox} {@code searchyayasan}/{@code searchsekolah}
 *       memang dipra-pilih dan di-{@code disable} untuk pengguna non-admin oleh
 *       {@code Common.initYayasanDanSekolahDanSemua(...)}, tetapi kueri di {@link #reload()}
 *       justru <b>membuang</b> pilihan tersebut. Pembatasan yang diandalkan grid utama
 *       {@link SiswaAction} karena itu tidak berlaku di dasbor ini.</li>
 *   <li><b>Tidak ada cache.</b> Kelas ini tidak memakai {@code loadDataWithCache},
 *       {@code isPersonal}, maupun {@code DashboardCacheUtil}; hasil kueri hanya hidup selama
 *       komponen ZK ini ada. Amplifier "hasil bocor ditulis ke cache L3 app-wide" yang tercatat
 *       pada dasbor pelanggaran/apresiasi <b>TIDAK</b> berlaku di sini.</li>
 *   <li><b>Volume dan jenis data.</b> Tabel dasbor itu sendiri hanya menampilkan agregat (nama
 *       sekolah + angka), bukan baris per siswa. Kebocoran terjadi pada <i>drill-down</i>:
 *       ekspor menarik hingga {@code setMaxResults(1048576)} baris {@link Siswa} dengan 116
 *       kolom {@link SiswaAction#contents}, termasuk NIK siswa/ayah/ibu/wali, penghasilan orang
 *       tua, seluruh nomor telepon &amp; WhatsApp, alamat rumah beserta koordinat lintang/bujur,
 *       nomor rekening bank, data kesehatan (golongan darah, riwayat penyakit, kebutuhan
 *       khusus), serta nomor KIP/KPS/KKS. Tautan "Total" mengekspor seluruh sekolah sekaligus
 *       ({@code Restrictions.in("sekolah", sekolahs)}).</li>
 *   <li><b>Prasyarat eksploitasi.</b> Cukup satu akun sah yang perannya memuat menu "Siswa"
 *       (id menu {@code 887727}); tab "Statistik" pada {@code siswa.zul} tidak punya syarat
 *       tampil tambahan, dan tautan ekspor tidak diikat ke privilese apa pun &mdash; berbeda dari
 *       tombol Tambah/Ubah/Hapus di {@link SiswaAction} yang masih memeriksa
 *       {@code CommonPrivilages.checkPrevilages(...)}. Operator ber-hak-baca di satu sekolah
 *       dapat mengunduh data seluruh sekolah pada seluruh yayasan.</li>
 * </ul>
 * <p><b>Ironi arah dampak:</b> justru pengguna <i>non-admin</i> (yang punya konteks sekolah,
 * sehingga combobox terisi nilai non-null) yang menerima hasil tanpa saringan; sedangkan
 * pengguna tanpa konteks sekolah (lazimnya admin) malah menabrak {@code NullPointerException}
 * sehingga tab "Data" gagal tampil. Perbaikan NPE tanpa memperbaiki ternary-nya akan
 * memperluas kebocoran, bukan menutupnya.</p>
 *
 * <h3>Batas tanggung jawab</h3>
 * <p>Perilaku umum jendela, lifecycle, dan mekanisme render tetap milik {@link MyWindow}.
 * Kelas ini hanya memuat perbedaan spesifik dasbor siswa. Query dan saringan di sini
 * <b>tidak boleh</b> disalin ke action lain; jika dibutuhkan rekap serupa, panggil ulang
 * kelas ini atau sediakan service bersama yang menyertakan penyaringan tenant.</p>
 *
 * @see MyWindow
 * @see DashboardRingkasanSiswa Dasbor grafik yang dipasang pada tab "Dasbor".
 * @see SiswaAction             Layar master pemanggil ({@code onStatistik}) dan sumber {@code contents}.
 */
public class DasboardSiswa extends MyWindow {

	/** Versi serialisasi komponen ZK (diwarisi dari {@link MyWindow}). */
	private static final long serialVersionUID = 3557603220165512688L;
	/**
	 * Wilayah {@code Center} dari borderlayout tab "Data"; tempat tabel rekap digambar ulang
	 * setiap kali {@link #reload()} dipanggil. Diisi di {@link #init()}.
	 */
	private Center center;
	/**
	 * <b>Tidak terpakai.</b> Nilai {@code 750} tidak pernah dibaca di kelas ini; sisa
	 * salin-tempel dari dasbor lain. Lebar komponen diatur lewat {@code setWidth("100%")}.
	 */
	private int width = 750;
	/**
	 * <b>Tidak terpakai.</b> Nilai {@code 100} tidak pernah dibaca; lihat catatan pada
	 * {@link #width}.
	 */
	private int height = 100;
	/**
	 * Tabel rekap tab "Data" yang sedang tampil. <b>Hanya diisi oleh {@link #reload()}</b>;
	 * selama {@link #reload()} belum sukses dijalankan nilainya tetap {@code null}, sehingga
	 * tombol "Download" (yang membacanya lewat {@code DasboardSiswa.this.grid}) tidak punya
	 * grid untuk diunduh. Perhatikan bahwa {@link #init()} juga membuat variabel lokal bernama
	 * {@code grid} untuk panel saringan &mdash; variabel lokal itu <i>membayangi</i> field ini
	 * dan bukan grid yang diunduh.
	 */
	private Grid grid;

	/**
	 * Saringan yayasan. Diisi dan (untuk non-admin) dipra-pilih serta dinonaktifkan oleh
	 * {@code Common.initYayasanDanSekolahDanSemua(...)}. <b>Pilihannya diabaikan</b> oleh kueri
	 * di {@link #reload()} &mdash; lihat catatan keamanan pada method tersebut.
	 */
	private Combobox searchyayasan = new Combobox();
	/**
	 * Saringan sekolah. Sama seperti {@link #searchyayasan}: pilihannya diabaikan kueri, dan
	 * pada kondisi "belum ada sekolah terpilih" justru memicu {@code NullPointerException} di
	 * {@link #reload()}.
	 */
	private Combobox searchsekolah = new Combobox();

	/** Batas bawah rentang tahun masuk. Nilai bawaan: tahun berjalan dikurangi 4. */
	private MyIntbox mulai = new MyIntbox();
	/** Batas atas rentang tahun masuk. Nilai bawaan: tahun berjalan. */
	private MyIntbox sampai = new MyIntbox();
	/**
	 * Kalender acuan (zona waktu aplikasi, via {@code WaktuUtil.getCalendar()}) untuk menghitung
	 * tahun bawaan saringan dan nilai cadangan di {@link #reload()}. Diisi di {@link #init()}.
	 */
	private Calendar calendar;

	/**
	 * Membuat dasbor dengan judul/border bawaan {@link MyWindow} lalu langsung membangun
	 * antarmuka lewat {@link #init()}.
	 *
	 * <p>Ini konstruktor yang dipakai jalur produksi ({@link SiswaAction#onStatistik}).
	 * Kegagalan {@link #init()} tidak dilempar ke pemanggil melainkan diserap
	 * {@code Common.tampilErrorJikaAdmin(e)} &mdash; pesan kesalahan hanya tampil bagi admin,
	 * sehingga bagi pengguna biasa kegagalan pembangunan tampil sebagai tab kosong tanpa
	 * penjelasan.</p>
	 */
	public DasboardSiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Varian konstruktor yang meneruskan judul, gaya border, dan sifat dapat-ditutup ke
	 * {@link MyWindow} sebelum membangun antarmuka lewat {@link #init()}.
	 *
	 * <p>Catatan: ketiga argumen ini efektif tidak berpengaruh karena {@link #init()} segera
	 * menimpanya dengan {@code setBorder("none")} dan {@code setClosable(false)}. Penanganan
	 * kesalahan identik dengan konstruktor tanpa argumen.</p>
	 *
	 * @param title    judul jendela yang diteruskan ke {@link MyWindow}.
	 * @param border   gaya border ZK; ditimpa menjadi {@code "none"} oleh {@link #init()}.
	 * @param closable apakah jendela dapat ditutup; ditimpa menjadi {@code false} oleh {@link #init()}.
	 */
	public DasboardSiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun seluruh kerangka antarmuka dasbor: tabbox tiga tab beserta isinya, panel
	 * saringan tab "Data", dan pendaftaran pemicu pemuatan data.
	 *
	 * <p><b>Kapan dipanggil.</b> Sekali saja, dari kedua konstruktor. Tidak pernah dipanggil
	 * ulang; penyegaran data ditangani {@link #reload()}.</p>
	 *
	 * <p><b>Yang dikerjakan, berurutan:</b></p>
	 * <ol>
	 *   <li>Menyetel jendela menjadi 100% &times; 100%, tanpa border, tidak dapat ditutup.</li>
	 *   <li>Membuat {@link Tabbox} dengan tab "Dasbor", "Data", dan "Pendidikan".</li>
	 *   <li>Tab "Dasbor": panelnya diberi {@code setHeight("20000px")}. Tinggi ini <b>harus</b>
	 *       lewat properti tinggi ZK, bukan {@code setStyle("min-height:...")}, karena ZK
	 *       menyetel tinggi div-konten internal ke tinggi viewport dan mengabaikan
	 *       {@code min-height} (lihat komentar di badan method). Satu
	 *       {@link DashboardRingkasanSiswa} langsung dipasang di sini (eager) karena tab ini
	 *       adalah tab pertama yang terpilih; listener {@code onClick}-nya karena itu praktis
	 *       kode mati &mdash; penjaga {@code getChildren().size() == 0} tidak akan pernah
	 *       terpenuhi.</li>
	 *   <li>Tab "Pendidikan": listener terdaftar tetapi badannya seluruhnya dikomentari, jadi
	 *       tab ini permanen kosong.</li>
	 *   <li>Tab "Data": membangun {@link Borderlayout}. Tinggi minimumnya 25000px, kecuali bila
	 *       tinggi desktop pengguna diketahui dari {@code MainAction.desktopHeights} &mdash; maka
	 *       dipakai 90% dari nilai itu. Ini satu-satunya pemakaian
	 *       {@code Common.getCurrentUser()} di kelas ini dan <b>murni kosmetik</b>, bukan
	 *       otorisasi.</li>
	 *   <li>Mengisi {@code North} dengan grid saringan: combobox Yayasan dan Sekolah (diisi
	 *       {@code Common.initYayasanDanSekolahDanSemua}), serta sepasang {@link MyIntbox}
	 *       rentang tahun masuk yang dibawa-nilai ke <i>tahun berjalan &minus; 4</i> sampai
	 *       <i>tahun berjalan</i>.</li>
	 *   <li>Memasang satu {@link EventListener} bersama yang memanggil {@link #reload()} pada
	 *       {@code onChange} keempat saringan, dan mendaftarkannya juga ke
	 *       {@code Common.createDefaultTimer(...)} sehingga pemuatan pertama berjalan setelah
	 *       halaman selesai dirender (halaman tidak terasa lambat saat dibuka).</li>
	 *   <li>Menyiapkan {@link #center} sebagai wadah tabel rekap, lalu menambahkan tombol
	 *       "Download" yang mengunduh {@link #grid} lewat {@code UIUtil.downloadGrid(...)}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> murni pembangunan pohon komponen ZK dan pendaftaran listener.
	 * Method ini tidak menjalankan kueri data siswa sama sekali &mdash; seluruh pembacaan
	 * database terjadi di {@link #reload()} (dan di dalam {@link DashboardRingkasanSiswa}
	 * yang dibuatnya). Timer yang didaftarkan berarti kueri akan berjalan otomatis tanpa
	 * interaksi pengguna.</p>
	 *
	 * <p><b>Kuirk:</b> variabel lokal {@code Grid grid} di method ini membayangi field
	 * {@link #grid}; yang dibaca tombol "Download" adalah field, yang baru terisi setelah
	 * {@link #reload()} berhasil. Objek {@link MyFormRow} yang sama juga dipakai ulang untuk
	 * tiga baris saringan dengan {@code setParent(rows)} berulang &mdash; hasilnya satu baris
	 * berisi enam sel, bukan tiga baris terpisah.</p>
	 *
	 * @throws Exception bila pembangunan komponen ZK atau pengisian combobox gagal; ditangkap
	 *                   dan diserap oleh konstruktor pemanggil.
	 */
	@SuppressWarnings("deprecation")
	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabDasbor = new MyTabConfig("Dasbor");
		tabDasbor.setParent(tabs);

		MyTabConfig tab1 = new MyTabConfig("Data");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Pendidikan");
		tab2.setParent(tabs);

//		MyTabConfig tab3 = new MyTabConfig("Status");
//		tab3.setParent(tabs);
//
//		MyTabConfig tab4 = new MyTabConfig("Agama");
//		tab4.setParent(tabs);
//
//		MyTabConfig tab5 = new MyTabConfig("Usia");
//		tab5.setParent(tabs);
//
//		MyTabConfig tab6 = new MyTabConfig("Tipe");
//		tab6.setParent(tabs);
//
//		MyTabConfig tab7 = new MyTabConfig("Unit Kerja");
//		tab7.setParent(tabs);
//
//		MyTabConfig tab8 = new MyTabConfig("PTKP");
//		tab8.setParent(tabs);
//
//		MyTabConfig tab9 = new MyTabConfig("Ikatan");
//		tab9.setParent(tabs);
//
//		MyTabConfig tab10 = new MyTabConfig("Atasan");
//		tab10.setParent(tabs);
//
//		MyTabConfig tab11 = new MyTabConfig("Masa Kerja");
//		tab11.setParent(tabs);
//
//		MyTabConfig tab12 = new MyTabConfig("Asuransi");
//		tab12.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelDasbor = new ais.ui.util.MyTabpanel();
		tabpanelDasbor.setParent(tabpanels);
		// Tab "Dasbor": pakai setHeight (properti tinggi ZK) PERSIS pola tab "Data"
		// (tabpanel1.setHeight("30330px")) — inilah yang benar-benar dihormati mold tabbox ZK
		// sehingga panel jadi 20000px & konten tampil penuh. (setStyle("min-height:..") TIDAK cukup
		// karena ZK menyetel tinggi div-konten internal ke tinggi viewport, mengabaikan min-height.)
		tabpanelDasbor.setHeight("20000px");
		tabDasbor.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelDasbor.getChildren().size() == 0) {
					DashboardRingkasanSiswa d = new DashboardRingkasanSiswa();
					d.setWidth("100%");
					d.setParent(tabpanelDasbor);
				}
			}
		});
		// Auto-load: Dasbor adalah tab pertama (default terpilih).
		DashboardRingkasanSiswa dRingkasanSiswa = new DashboardRingkasanSiswa();
		dRingkasanSiswa.setWidth("100%");
		dRingkasanSiswa.setParent(tabpanelDasbor);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("30330px");

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
//					DasboardSiswaPendidikan dasboardSiswaPendidikan = new DasboardSiswaPendidikan();
//					dasboardSiswaPendidikan.setHeight("100%");
//					dasboardSiswaPendidikan.setWidth("100%");
//					dasboardSiswaPendidikan.setParent(tabpanel2);
				}
			}
		});

//		final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
//		tabpanel3.setParent(tabpanels);
//		tab3.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel3.getChildren().size() == 0) {
//					DasboardSiswaStatusSiswa dasboardSiswaStatusSiswa = new DasboardSiswaStatusSiswa();
//					dasboardSiswaStatusSiswa.setHeight("100%");
//					dasboardSiswaStatusSiswa.setWidth("100%");
//					dasboardSiswaStatusSiswa.setParent(tabpanel3);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
//		tabpanel4.setParent(tabpanels);
//		tab4.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel4.getChildren().size() == 0) {
//					DasboardSiswaAgama dasboardSiswaAgama = new DasboardSiswaAgama();
//					dasboardSiswaAgama.setHeight("100%");
//					dasboardSiswaAgama.setWidth("100%");
//					dasboardSiswaAgama.setParent(tabpanel4);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel5 = new ais.ui.util.MyTabpanel();
//		tabpanel5.setParent(tabpanels);
//		tab5.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel5.getChildren().size() == 0) {
//					DasboardSiswaUsia dasboardSiswaUsia = new DasboardSiswaUsia();
//					dasboardSiswaUsia.setHeight("100%");
//					dasboardSiswaUsia.setWidth("100%");
//					dasboardSiswaUsia.setParent(tabpanel5);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel6 = new ais.ui.util.MyTabpanel();
//		tabpanel6.setParent(tabpanels);
//		tab6.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel6.getChildren().size() == 0) {
//					DasboardSiswaTipeSiswa dasboardSiswaTipeSiswa = new DasboardSiswaTipeSiswa();
//					dasboardSiswaTipeSiswa.setHeight("100%");
//					dasboardSiswaTipeSiswa.setWidth("100%");
//					dasboardSiswaTipeSiswa.setParent(tabpanel6);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel7 = new ais.ui.util.MyTabpanel();
//		tabpanel7.setParent(tabpanels);
//		tab7.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel7.getChildren().size() == 0) {
//					DasboardSiswaUnitKerja dasboardSiswaUnitKerja = new DasboardSiswaUnitKerja();
//					dasboardSiswaUnitKerja.setHeight("100%");
//					dasboardSiswaUnitKerja.setWidth("100%");
//					dasboardSiswaUnitKerja.setParent(tabpanel7);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel8 = new ais.ui.util.MyTabpanel();
//		tabpanel8.setParent(tabpanels);
//		tab8.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel8.getChildren().size() == 0) {
//					DasboardSiswaPtkpSiswa dasboardSiswaPtkpSiswa = new DasboardSiswaPtkpSiswa();
//					dasboardSiswaPtkpSiswa.setHeight("100%");
//					dasboardSiswaPtkpSiswa.setWidth("100%");
//					dasboardSiswaPtkpSiswa.setParent(tabpanel8);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel9 = new ais.ui.util.MyTabpanel();
//		tabpanel9.setParent(tabpanels);
//		tab9.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel9.getChildren().size() == 0) {
//					DasboardSiswaIkatanKerja dasboardSiswaIkatanKerja = new DasboardSiswaIkatanKerja();
//					dasboardSiswaIkatanKerja.setHeight("100%");
//					dasboardSiswaIkatanKerja.setWidth("100%");
//					dasboardSiswaIkatanKerja.setParent(tabpanel9);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel10 = new ais.ui.util.MyTabpanel();
//		tabpanel10.setParent(tabpanels);
//		tab10.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel10.getChildren().size() == 0) {
//					DasboardSiswaJenisJabatan dasboardSiswaJenisJabatan = new DasboardSiswaJenisJabatan();
//					dasboardSiswaJenisJabatan.setHeight("100%");
//					dasboardSiswaJenisJabatan.setWidth("100%");
//					dasboardSiswaJenisJabatan.setParent(tabpanel10);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel11 = new ais.ui.util.MyTabpanel();
//		tabpanel11.setParent(tabpanels);
//		tab11.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel11.getChildren().size() == 0) {
//					DasboardSiswaMasaKerja dasboardSiswaMasaKerja = new DasboardSiswaMasaKerja();
//					dasboardSiswaMasaKerja.setHeight("100%");
//					dasboardSiswaMasaKerja.setWidth("100%");
//					dasboardSiswaMasaKerja.setParent(tabpanel11);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel12 = new ais.ui.util.MyTabpanel();
//		tabpanel12.setParent(tabpanels);
//		tab12.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel12.getChildren().size() == 0) {
//					DasboardSiswaAsuransi dasboardSiswaAsuransi = new DasboardSiswaAsuransi();
//					dasboardSiswaAsuransi.setHeight("100%");
//					dasboardSiswaAsuransi.setWidth("100%");
//					dasboardSiswaAsuransi.setParent(tabpanel12);
//				}
//			}
//		});

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(tabpanel1);
		borderlayout.setStyle("min-height:25000px");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getUserId() != null) {
			Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
			if (desktopHeight != null) {
				borderlayout.setStyle("min-height:" + (desktopHeight * 0.9) + "px");
			}
		}

		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(borderlayout);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}

		};

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan);
		searchyayasan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		searchsekolah.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Masuk"));
		Hbox hbox = new Hbox();
		hbox.appendChild(mulai);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(sampai);

		mulai.setCols(5);
		sampai.setCols(5);

		calendar = ais.ui.util.WaktuUtil.getCalendar();

		mulai.setValue(calendar.get(Calendar.YEAR) - 4);
		sampai.setValue(calendar.get(Calendar.YEAR));

		searchyayasan.addEventListener("onChange", eventListener);
		searchsekolah.addEventListener("onChange", eventListener);
		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);

		center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		Common.createDefaultTimer(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "6");
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DasboardSiswa.this.grid);
			}
		});

	}

	/**
	 * Membangun ulang isi tab "Data": tabel rekap jumlah siswa per sekolah per tahun masuk,
	 * baris "Total", dan grafik batang ringkasan.
	 *
	 * <p><b>Kapan dipanggil.</b> Dari {@link EventListener} bersama yang dipasang
	 * {@link #init()} &mdash; yaitu saat salah satu dari empat saringan berubah, dan sekali
	 * secara otomatis lewat timer bawaan segera setelah halaman dirender.</p>
	 *
	 * <p><b>Yang dikerjakan, berurutan:</b></p>
	 * <ol>
	 *   <li>Mengosongkan {@link #center} lalu membuat {@link Grid} baru dan menyimpannya ke
	 *       field {@link #grid} (inilah satu-satunya titik pengisian field tersebut).</li>
	 *   <li>Membaca rentang tahun dari {@link #mulai}/{@link #sampai}; bila kosong, memakai
	 *       cadangan <i>tahun berjalan &minus; 4</i> sampai <i>tahun berjalan</i>.</li>
	 *   <li>Menyusun tiga kolom ("Sekolah", "Tahun Masuk", "Jumlah") lalu mengambil daftar
	 *       {@link Sekolah} aktif, diurutkan berdasarkan nama.</li>
	 *   <li>Untuk setiap sekolah &times; setiap tahun dalam rentang, menjalankan satu kueri
	 *       {@code COUNT} pada {@link Siswa} (hanya siswa dengan {@code namaSiswa} terisi dan
	 *       {@code aktif} bernilai null atau true). Angka nol tidak menambahkan baris.</li>
	 *   <li>Menyusun baris "Total" dan grafik batang lewat
	 *       {@code DashboardModernHtmlUtil.createAnyChart(...)}.</li>
	 * </ol>
	 *
	 * <h3>PERINGATAN KEAMANAN &mdash; saringan tenant tidak berfungsi</h3>
	 * <p>Kueri daftar sekolah memakai dua ternary yang <b>terbalik arah</b>, sehingga tidak
	 * pernah menyaring apa pun:</p>
	 * <ul>
	 *   <li><b>Yayasan.</b> Cabang "ada yang dipilih" menghasilkan
	 *       {@code Restrictions.sqlRestriction("true")} (tanpa saringan), sedangkan cabang
	 *       "tidak ada yang dipilih" memanggil
	 *       {@code CommonSearchFilterHelper.eqSelectedWithId("yayasan", ...)} yang &mdash; karena
	 *       memang tidak ada nilai terpilih &mdash; mengembalikan {@code 1=1}. Kedua cabang
	 *       sama-sama tanpa saringan: combobox Yayasan efektif mati.</li>
	 *   <li><b>Sekolah.</b> Cabang "ada yang dipilih" juga menghasilkan
	 *       {@code sqlRestriction("true")}. Cabang "tidak ada yang dipilih" justru
	 *       men-<i>dereference</i> {@code searchsekolah.getSelectedItem().getValue()} &mdash;
	 *       tepat nilai yang baru saja dipastikan null oleh kondisinya sendiri &mdash; sehingga
	 *       <b>selalu melempar {@code NullPointerException}</b>.</li>
	 * </ul>
	 * <p>Akibat praktisnya: pengguna yang punya konteks sekolah (umumnya non-admin, combobox-nya
	 * dipra-pilih dan dinonaktifkan oleh {@code Common.initYayasanDanSekolahDanSemua}) melewati
	 * cabang {@code "true"} dan mendapat rekap <b>seluruh sekolah di seluruh yayasan</b>;
	 * sedangkan pengguna tanpa konteks sekolah menabrak NPE dan tab "Data" gagal tampil.
	 * Tidak ada pemeriksaan peran apa pun di method ini &mdash; tidak untuk siswa, orang tua,
	 * guru, maupun privilese menu.</p>
	 * <p><b>Dampak terberat ada pada drill-down, bukan pada angkanya.</b> Setiap angka dirender
	 * sebagai {@link A} yang membuka ekspor {@code Common.cetakDataCustomButton(Siswa.class,
	 * ..., SiswaAction.contents)}: 116 kolom data pribadi siswa dan orang tua (NIK, penghasilan,
	 * telepon/WhatsApp, alamat beserta lintang/bujur, rekening bank, data kesehatan, nomor
	 * KIP/KPS/KKS) hingga batas satu juta baris. Kriteria ekspor per sel dibatasi pada satu
	 * {@code sekolah} dari perulangan, tetapi perulangan itu sendiri mencakup semua sekolah;
	 * sedangkan tautan "Total" mengekspor semuanya sekaligus lewat
	 * {@code Restrictions.in("sekolah", sekolahs)} tanpa batas tenant.</p>
	 * <p><b>Catatan perbaikan:</b> memperbaiki NPE saja (tanpa membalik arah kedua ternary)
	 * akan <i>memperluas</i> kebocoran ke pengguna yang saat ini terlindung kebetulan oleh
	 * kegagalan itu.</p>
	 *
	 * <p><b>Bug fungsional lain yang teramati.</b> (1) {@code categoryModel.setValue(nama, "",
	 * count)} dipanggil di dalam perulangan tahun dengan kunci yang sama untuk satu sekolah,
	 * sehingga nilai tahun berikutnya <i>menimpa</i> tahun sebelumnya &mdash; grafik batang
	 * hanya memperlihatkan angka tahun terakhir yang bukan nol, bukan total sekolah tersebut,
	 * dan karenanya tidak cocok dengan tabel di atasnya. (2) Satu {@link MyFormRow} dipakai
	 * untuk semua tahun dari satu sekolah, sehingga satu baris berisi 1 label + N angka
	 * sementara header hanya mendeklarasikan tiga kolom. (3) Cabang {@code sekolah == null}
	 * pada kedua kriteria adalah kode mati &mdash; elemen daftar hasil Hibernate tidak pernah
	 * null. (4) Variabel {@code total} menjumlahkan lintas sekolah dan lintas tahun, jadi
	 * angkanya benar hanya jika pembacaan lintas-tenant di atas memang diinginkan.</p>
	 *
	 * <p><b>Efek samping:</b> menghapus dan membangun ulang seluruh isi {@link #center},
	 * menimpa field {@link #grid}, dan menjalankan <i>jumlah sekolah &times; jumlah tahun</i>
	 * kueri {@code COUNT} pada session Hibernate thread ZK. Tidak ada penulisan data.</p>
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	private void reload() {
		Common.clear(center);
		grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		int mul = mulai.getValue() == null ? (calendar.get(Calendar.YEAR) - 4) : mulai.getValue();
		int sam = sampai.getValue() == null ? (calendar.get(Calendar.YEAR)) : sampai.getValue();

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Sekolah");
		column.setParent(columns);

		column.setParent(columns);
		column = new MyColumnConfig("Tahun Masuk");
		column.setAlign("center");
		column.setParent(columns);
		column.setWidth("10%");

		column.setParent(columns);
		column = new MyColumnConfig("Jumlah");
		column.setAlign("center");
		column.setParent(columns);
		column.setWidth("5%");

		final List<Sekolah> sekolahs = ConstantValues.simpleList(HibernateUtil.currentSession()
				.createCriteria(Sekolah.class)
				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						? CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)
						: Restrictions.sqlRestriction("true"))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						? Restrictions.eq("id", ((Sekolah) searchsekolah.getSelectedItem().getValue()).getId())
						: Restrictions.sqlRestriction("true"))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Sekolah.class);

		Rows rows = new Rows();
		rows.setParent(grid);

		SimpleCategoryModel categoryModel = new SimpleCategoryModel();
		categoryModel.clear();
		int total = 0;
		for (final Sekolah sekolah : sekolahs) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.appendChild(new MyLabelBoldAja(sekolah.getNama()));

			for (int i = mul; i <= sam; i++) {
				final int thn = i;
				Integer count = ((Number) HibernateUtil.currentSession().createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa",""))
						.add(Restrictions.eq("tahunMasuk", thn))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.rowCount())
						.add(sekolah == null ? Restrictions.isNull("sekolah") : Restrictions.eq("sekolah", sekolah))

						.uniqueResult()).intValue();
				if (count > 0) {
					categoryModel.setValue(sekolah == null ? "Tidak Ditentukan" : sekolah.getNama(), "", count);
				}

				total += count;

				A a = new A(count + "");
				a.setStyle("font-size:12px;");
				a.setParent(row);
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						EventListener eventListener = (EventListener) Common
								.cetakDataCustomButton(Siswa.class, new DataCriteriaWithColumn() {

									@Override
									public Object[] initCriteria(boolean order) {

										try {

											Criteria criteria = HibernateUtil.currentSession()
													.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.eq("tahunMasuk", thn))
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(sekolah == null ? Restrictions.isNull("sekolah")
															: Restrictions.eq("sekolah", sekolah));

											return new Object[] { criteria, SiswaAction.contents };

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
										return null;
									}

								}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
										new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "" })
								.getAttribute("eventListener");

						eventListener.onEvent(null);
					}
				});

				if (count > 0) {
					row.setParent(rows);
				}
			}
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelBolder("Total"));

		A a = new A(total + "");
		a.setStyle("font-size:16px;font-weight: bolder;");
		a.setParent(row);
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				EventListener eventListener = (EventListener) Common
						.cetakDataCustomButton(Siswa.class, new DataCriteriaWithColumn() {

							@Override
							public Object[] initCriteria(boolean order) {

								try {

									Criteria criteria = HibernateUtil.currentSession().createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa",""))
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(sekolahs.isEmpty() ? Restrictions.sqlRestriction("false")
													: Restrictions.in("sekolah", sekolahs));

									return new Object[] { criteria, SiswaAction.contents };

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
								return null;
							}

						}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
								new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "" })
						.getAttribute("eventListener");

				eventListener.onEvent(null);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.setSpans((4) + "");
		row.setAlign("center");

		row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, "Dasbor Siswa", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
}
}
