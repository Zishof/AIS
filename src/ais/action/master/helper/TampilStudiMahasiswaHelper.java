package ais.action.master.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.poi.ss.usermodel.IndexedColors;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.EksporFromFeederAction;
import ais.action.master.MahasiswaAction;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiMahasiswa;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.helper.util.PenilaianUtil;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanKurikulumMahasiswa;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.PesanFormalHelper;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Kelas;
import ais.database.model.KelompokMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowStyled;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper akademik yang membangun jendela ZK <b>"Studi Mahasiswa"</b>: gabungan dasbor ringkasan
 * (IPK/IPS/SKS, validitas nilai, kelulusan MK, kehadiran, tugas akhir) dan tab riwayat KRS per
 * semester (Reguler, Semester Pendek, Remedial) untuk satu {@link Mahasiswa}. Dipanggil dari
 * banyak menu (Manajemen KRS, profil mahasiswa, dsb.) lewat {@link #tampil(Mahasiswa, DataLoader,
 * Boolean)} atau variannya.
 *
 * <p><b>Struktur jendela:</b> {@link #tampil(Mahasiswa, DataLoader, Boolean, Integer)} membangun
 * satu {@code MyWindow} modal berisi {@code Tabbox} 5 tab (Dasbor, KRS, KRS SP, Remedial, Absensi)
 * yang dimuat MALAS (lazy, hanya saat tab pertama kali diklik/dipilih) lewat listener bersama.
 * Tab <b>Dasbor</b> didelegasikan ke {@link #initDashboard(Tabpanel, Mahasiswa, MyWindow, DataLoader,
 * boolean)} (method statis, dipakai juga tombol "Hitung Ulang"); tab <b>KRS/KRS SP/Remedial</b>
 * memakai instance {@link #initMain(Mahasiswa, DataLoader, Component, Integer)} yang membangun
 * {@link MyGrid} dengan {@link DataRenderer} sebagai baris-per-semester (expander KRS memakai
 * {@code MyDetail}, isi detail didelegasikan ke {@code StudiMahasiswaHelper.display(...)} saat
 * dibuka). Tab <b>Absensi</b> memuat {@code DashboardRekapAbsensiMahasiswa}.</p>
 *
 * <p><b>Dasbor (initDashboard):</b> membaca seluruh {@link Detailperkuliahan} mahasiswa (session
 * Hibernate dedikasi, SELALU ditutup lewat {@link #closeOpenedSession(Session)}), meresolusi MK
 * ekivalen ({@code Common.getMatakuliahApakahEkivalen}), lalu menyusun kartu ringkasan (SKS
 * kumulatif, IPK, IPS, valid/tidak valid, lulus/belum, MK konversi), deteksi <b>mata kuliah
 * berkode sama/ekivalen</b> (potensi SKS ganda — {@link #buildDuplikatEkivalenHtml}), grafik tren
 * per semester, serta panel portal tambahan: distribusi nilai huruf, rekap kehadiran, rekap KRS
 * per semester, tugas akhir/skripsi, dan "Mata Kuliah Belum Diambil" (bandingkan kurikulum prodi
 * vs MK yang sudah tercatat — {@link #buildMkBelumDiambilHtml}, dengan pemilih kurikulum &amp;
 * filter jenis MK). Tombol toolbar: cetak laporan ber-grafik ({@code DashboardReportKit}, lihat
 * {@link #buildSumberLaporanStudi}), ekspor Excel multi-sheet ({@link #exportStudiMahasiswaExcel}),
 * dan hitung ulang (membangun ulang dasbor dengan {@code keDatabase=true} sehingga
 * {@code mahasiswa.reInitDetailperkuliahan(session)} dipanggil).</p>
 *
 * <p><b>Tab KRS (initMain/DataRenderer):</b> grid baris = satu semester (hasil
 * {@code Common.generateSemestersForGrid}), tiap baris merender KRS terkait lewat
 * {@link DataRenderer#render}: menangani kasus cuti (tampilkan revisi status saja), semester
 * konversi ({@code semester == 0}), semester "Lulus" ({@code semester == 1000}, tahap -1), serta
 * baris normal dengan editor Kelas/Dosen PA/Status Mahasiswa/Status Awal inline — tiap perubahan
 * langsung <b>commit transaksi Hibernate native session</b> (bukan lewat form simpan terpisah).
 * Status yang sudah dipaksa lewat konfigurasi {@code batasStudi}/{@code paksaAktifSemester}
 * ditampilkan sebagai label baca-saja, bukan combo.</p>
 *
 * <p><b>State instance utama:</b> {@code Mahasiswa mahasiswa}, {@code MyGrid grid}, flag tampilan
 * ({@code tampilKonversi}, {@code remedial}, {@code semesterPendek}, {@code smtSelected},
 * {@code edit}, {@code bukaLangsungKeKrs}), {@code MyDetail detailUtama} (baris KRS pertama yang
 * otomatis dibuka bila jendela diminta langsung ke semester tertentu).</p>
 *
 * <p><b>Kuirk/hal non-obvious:</b> banyak method statis privat ({@code dataKrsPerSemester},
 * {@code dataKehadiran}, {@code dataSkripsi}, {@code dataPengajuanJudulTA}, {@code
 * dataMkBelumDiambil}) membuka SESSION HIBERNATE SENDIRI ({@code getSessionFactory().openSession()})
 * karena dipanggil SETELAH session utama {@link #initDashboard} ditutup — pola ini dipakai
 * berulang dan wajib dipertahankan bila menambah panel baru. Grup helper HTML privat ({@code th},
 * {@code td}, {@code tdColor}, {@code tdBadge}, {@code thDup}, {@code tdDup}, {@code
 * progressCardHtml}, {@code insightItem}, {@code recommendationItemHtml}, dsb.) sekadar
 * membungkus fragmen {@code <table>/<div>} berstyle inline — tidak memuat logika domain.</p>
 */
public class TampilStudiMahasiswaHelper {

	/**
	 * Grid riwayat KRS per semester yang dibangun {@link #initMain(Mahasiswa, DataLoader, Component, Integer)}
	 * dan dirender oleh {@link DataRenderer}. Disimpan sebagai state instance karena
	 * {@link #onSearchDefault(Event, boolean)} MEMBANGUN ULANG grid ini (bukan sekadar me-refresh model)
	 * setiap kali rentang semester diubah atau tombol "Refresh / Hitung IP/IPK" ditekan.
	 */
	private MyGrid grid;
	/**
	 * Mahasiswa yang sedang ditampilkan. Diisi di {@link #tampil(Mahasiswa, DataLoader, Boolean, Integer)}
	 * dan menjadi acuan seluruh query tab KRS/SP/Remedial serta {@link DataRenderer}.
	 */
	private Mahasiswa mahasiswa;
	/**
	 * Bila {@code true}, baris mata kuliah KONVERSI turut ditampilkan pada detail KRS
	 * (diteruskan ke {@code StudiMahasiswaHelper}). Berasal dari konstruktor, tetapi DITIMPA oleh
	 * argumen {@code tampilKonversi} pada {@link #tampil(Mahasiswa, DataLoader, Boolean, Integer)}.
	 */
	private Boolean tampilKonversi = false;
	/**
	 * Menandai tab yang sedang aktif adalah tab <b>Remedial</b>. Diset ulang oleh listener tab bersama
	 * pada {@link #tampil(Mahasiswa, DataLoader, Boolean, Integer)} SEBELUM {@code initMain} dipanggil,
	 * lalu dibaca {@link DataRenderer} untuk memilih jenis KRS yang dimuat.
	 */
	private boolean remedial = false;
	/**
	 * Penanda jenis semester yang dimuat: {@code null} = KRS reguler, {@code Perkuliahan.SEMESTER_PENDEK}
	 * = tab "KRS SP". Sama seperti {@link #remedial}, nilainya DITIMPA oleh listener tab bersama sesuai
	 * tab yang diklik, sehingga nilai dari konstruktor hanya berlaku sampai tab pertama dimuat.
	 */
	private Integer semesterPendek = null;

	/**
	 * Cache pengguna yang sedang login ({@code Common.getCurrentUser()}). Dipakai untuk gerbang
	 * tampilan/otorisasi: visibilitas tombol "Bersihkan KRS Double", akses sinkronisasi Feeder, dan
	 * pengecekan role saat mengubah Status Awal / menghapus data nilai. Di-<i>refresh</i> ulang di
	 * beberapa titik ({@link #initMain(Mahasiswa, DataLoader, Component, Integer)} dan dalam listener
	 * {@link DataRenderer}) karena satu jendela bisa hidup lama.
	 */
	private Tbmuser tbmuser;
	/** Combo batas BAWAH rentang semester yang ditampilkan pada grid KRS. Lihat {@link #onSearchDefault(Event, boolean)}. */
	private Combobox semesterMulai;
	/** Combo batas ATAS rentang semester yang ditampilkan pada grid KRS. Lihat {@link #onSearchDefault(Event, boolean)}. */
	private Combobox semesterSampai;
	/**
	 * Tombol toolbar "Refresh / Hitung IP/IPK" pada tab KRS. Menjalankan
	 * {@link #onSearchDefault(Event, boolean)} dengan {@code keDatabase = true} sehingga hasil hitung
	 * ulang IP/IPK DITULIS ke database saat baris dirender.
	 */
	private MyToolbarbuttonConfig toolbarbuttonLihat;
	/**
	 * Parameter ekstrakurikuler dari konstruktor. <b>Tidak dipakai</b> di kelas ini (karena itu ditandai
	 * {@code @SuppressWarnings("unused")}); dipertahankan agar tanda tangan konstruktor tetap kompatibel
	 * dengan pemanggil lama.
	 */
	@SuppressWarnings("unused")
	private Integer ekstrakurikuler;
	/**
	 * Semester yang diminta pemanggil untuk langsung dipilih/dibuka. Bila tidak {@code null}, kedua combo
	 * semester dipaksa ke nilai ini dan baris detail pertama ({@link #detailUtama}) otomatis dibuka lewat
	 * timer setelah grid selesai dirender.
	 */
	private Integer smtSelected = null;
	/**
	 * Izin ubah dari pemanggil. Bila {@code false}, jendela di-<i>freeze</i> (baca saja) kecuali tombol
	 * Tutup, combo semester, dan tombol Refresh &mdash; lihat
	 * {@link #tampil(Mahasiswa, DataLoader, Boolean, Integer)}. Perhatikan bahwa {@code true} SAJA belum
	 * cukup: pengguna tetap harus lolos gerbang role istimewa/{@code admin_lain_...} agar jendela tidak
	 * dibekukan.
	 */
	private Boolean edit = true;
	/**
	 * Baris {@code MyDetail} KRS PERTAMA yang dirender pada grid. Disimpan agar
	 * {@link #onSearchDefault(Event, boolean)} dapat membukanya otomatis (beserta memicu listener
	 * pemuatan detailnya) ketika {@link #smtSelected} diisi. Direset ke {@code null} setiap kali grid
	 * dibangun ulang.
	 */
	private MyDetail detailUtama = null;

	/**
	 * Membentuk helper dengan preferensi tampilan awal. Perhatikan bahwa {@code semesterPendek} dan
	 * {@code tampilKonversi} yang diberikan di sini bersifat SEMENTARA untuk alur yang memanggil
	 * {@link #initMain(Mahasiswa, DataLoader, Component, Integer)} secara langsung: pada alur normal
	 * {@link #tampil(Mahasiswa, DataLoader, Boolean, Integer)} keduanya ditimpa (masing-masing oleh
	 * listener tab dan oleh argumen {@code tampilKonversi}).
	 *
	 * @param semesterPendek {@code Perkuliahan.SEMESTER_PENDEK} untuk memuat KRS semester pendek,
	 *                       {@code null} untuk KRS reguler.
	 * @param ekstrakurikuler tidak dipakai kelas ini; lihat {@link #ekstrakurikuler}.
	 * @param tampilKonversi {@code true} agar baris mata kuliah konversi ikut ditampilkan.
	 * @param edit {@code false} untuk membuka jendela dalam mode baca saja (di-<i>freeze</i>).
	 */
	public TampilStudiMahasiswaHelper(Integer semesterPendek, Integer ekstrakurikuler, Boolean tampilKonversi,
			Boolean edit) {
		this.semesterPendek = semesterPendek;
		this.ekstrakurikuler = ekstrakurikuler;
		this.tampilKonversi = tampilKonversi;
		this.edit = edit;
	}

	/**
	 * Varian singkat: buka jendela Studi Mahasiswa langsung ke tab Dasbor (perilaku default),
	 * tanpa memilih semester awal tertentu. Lihat {@link #tampil(Mahasiswa, DataLoader, Boolean, Integer)}.
	 *
	 * @param mahasiswa mahasiswa yang datanya ditampilkan.
	 * @param dataLoader dipanggil ulang saat jendela ditutup agar grid pemanggil me-refresh datanya; boleh {@code null}.
	 * @param tampilKonversi bila {@code true}, tampilan KRS turut menampilkan baris mata kuliah konversi.
	 */
	public void tampil(Mahasiswa mahasiswa, DataLoader dataLoader, Boolean tampilKonversi) throws Exception {
		tampil(mahasiswa, dataLoader, tampilKonversi, null);
	}

	/**
	 * Bila {@code true}, jendela studi mahasiswa dibuka LANGSUNG pada tab <b>KRS</b> (Rencana Studi
	 * Mahasiswa), bukan tab Dasbor. Diset lewat {@link #tampil(Mahasiswa, DataLoader, Boolean, Integer, boolean)}
	 * dan dipakai tombol "Daftar KRS" pada daftar Manajemen KRS agar pengguna langsung melihat KRS.
	 * Default {@code false} (perilaku lama: buka tab Dasbor) &mdash; tak mengubah pemanggil lain.
	 */
	private boolean bukaLangsungKeKrs = false;

	/**
	 * Varian {@link #tampil(Mahasiswa, DataLoader, Boolean, Integer)} dengan opsi membuka langsung
	 * ke tab KRS.
	 *
	 * @param langsungKeKrs {@code true} &rarr; tab KRS terpilih &amp; termuat saat jendela terbuka
	 *                      (bukan tab Dasbor).
	 */
	public void tampil(final Mahasiswa mahasiswa, final DataLoader dataLoader, Boolean tampilKonversi,
			final Integer smtMulai, boolean langsungKeKrs) throws Exception {
		this.bukaLangsungKeKrs = langsungKeKrs;
		tampil(mahasiswa, dataLoader, tampilKonversi, smtMulai);
	}

	/**
	 * Method utama: membangun dan menampilkan (modal) jendela "Studi Mahasiswa" berisi 5 tab
	 * (Dasbor, KRS, KRS SP, Remedial, Absensi). Sebelum membangun UI, menyinkronkan status
	 * mahasiswa terkini lewat {@code Common.singkronisasiStatusMahasiswa(...)}. Isi tiap tab
	 * dimuat MALAS: listener {@code onClick} bersama pada tab Dasbor/KRS/KRS SP/Remedial baru
	 * membangun kontennya saat tab tersebut pertama kali dipilih (Dasbor via
	 * {@link #initDashboard}, tab lain via {@link #initMain(Mahasiswa, DataLoader, Component, Integer)});
	 * tab Absensi memuat {@code DashboardRekapAbsensiMahasiswa} lewat listener terpisah. Tab
	 * default yang dimuat pertama adalah Dasbor, kecuali {@link #bukaLangsungKeKrs} bernilai
	 * {@code true} (lihat {@link #tampil(Mahasiswa, DataLoader, Boolean, Integer, boolean)}).
	 *
	 * <p>Bila pengguna bukan admin/pemegang hak khusus (role Akademik/Admin Fakultas/Admin
	 * Jurusan/Administrator, atau terdaftar di konfigurasi
	 * {@code admin_lain_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs}) dan parameter
	 * {@code edit} bernilai {@code false}, jendela di-freeze (baca saja) kecuali tombol
	 * Tutup/filter semester/Refresh.</p>
	 *
	 * @param mahasiswa mahasiswa yang datanya ditampilkan.
	 * @param dataLoader dipanggil ulang saat jendela ditutup agar grid pemanggil me-refresh datanya; boleh {@code null}.
	 * @param tampilKonversi bila {@code true}, tampilan KRS turut menampilkan baris mata kuliah konversi.
	 * @param smtMulai semester yang otomatis dipilih/dibuka pada tab KRS saat jendela terbuka; boleh {@code null}.
	 */
	public void tampil(final Mahasiswa mahasiswa, final DataLoader dataLoader, Boolean tampilKonversi,
			final Integer smtMulai) throws Exception {

		Common.singkronisasiStatusMahasiswa(null, mahasiswa, Common.getCurrentTahunAkademik(),
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP, false);

		this.tbmuser = Common.getCurrentUser();
		this.mahasiswa = mahasiswa;
		this.tampilKonversi = tampilKonversi;

		final MyWindow window = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		Common.clear(window);
		window.setWidth("98%");
		window.setHeight("98%");

		Tabbox tabbox = new Tabbox();
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		tabbox.setParent(Common.tampilanScrollTabbox(window));

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabDasbor = new MyTabConfig("Dasbor");
		MyTabConfig tab = new MyTabConfig("KRS");
		MyTabConfig tabsp = new MyTabConfig("KRS SP");
		MyTabConfig tabremedial = new MyTabConfig("Remedial");
		MyTabConfig tababsen = new MyTabConfig("Absensi");

		tabs.appendChild(tabDasbor);
		tabs.appendChild(tab);
		tabs.appendChild(tabsp);
		tabs.appendChild(tabremedial);
		tabs.appendChild(tababsen);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelDasbor = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelsp = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelremedial = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelabsen = new ais.ui.util.MyTabpanel();

		tabpanelDasbor.setParent(tabpanels);
		tabpanel.setParent(tabpanels);
		tabpanelsp.setParent(tabpanels);
		tabpanelremedial.setParent(tabpanels);
		tabpanelabsen.setParent(tabpanels);

		EventListener listener = new EventListener() {
			/**
			 * Pemuat MALAS (lazy) isi tab, dipakai BERSAMA oleh tab Dasbor, KRS, KRS SP dan Remedial.
			 * Panel tujuan ditentukan dari {@code arg0.getTarget()} ({@link MyTabConfig#getLinkedPanel()});
			 * bila {@code arg0} {@code null} &mdash; pemanggilan langsung untuk memuat tab default &mdash;
			 * atau targetnya bukan {@code MyTabConfig}, panel jatuh ke tab Dasbor.
			 *
			 * <p>Isi hanya dibangun bila panel masih KOSONG, sehingga klik berulang pada tab yang sama tidak
			 * membangun ulang UI. Untuk tab Dasbor pekerjaan didelegasikan ke
			 * {@link TampilStudiMahasiswaHelper#initDashboard(Tabpanel, Mahasiswa, MyWindow, DataLoader, boolean)}
			 * dengan {@code keDatabase = false}. Untuk tiga tab lain, listener LEBIH DAHULU menimpa state
			 * instance {@link TampilStudiMahasiswaHelper#semesterPendek} dan
			 * {@link TampilStudiMahasiswaHelper#remedial} sesuai tab yang diklik (Remedial memaksa
			 * {@code semesterPendek = null}) &mdash; inilah sebabnya nilai dari konstruktor tidak bertahan
			 * &mdash; lalu membangun {@code Borderlayout} berisi kartu identitas mahasiswa (foto, NIM, nama,
			 * HP/email, fakultas/jurusan/program), grid KRS dari
			 * {@link TampilStudiMahasiswaHelper#initMain(Mahasiswa, DataLoader, Component, Integer)}, dan
			 * toolbar bawah.
			 *
			 * <p><b>Gerbang baca-saja:</b> pada akhir pembangunan tab KRS, jendela di-{@code Common.freeze}
			 * bila parameter {@code edit} bernilai {@code false}, ATAU bila pengguna bukan role istimewa
			 * (Akademik / Admin Fakultas / Admin Jurusan / Administrator) dan role-nya tidak terdaftar pada
			 * konfigurasi {@code admin_lain_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs}.
			 * Tombol Tutup, kedua combo semester dan tombol Refresh sengaja tetap diaktifkan setelah freeze.
			 * Perhatikan bahwa pembekuan ini hanya berlaku untuk panel tab KRS/SP/Remedial; tab Dasbor
			 * dibangun pada cabang {@code return} lebih awal sehingga tidak melewati blok ini.
			 */
			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(final Event arg0) throws Exception {
				Tabpanel parent = tabpanelDasbor;
				try {
					if (arg0 != null && arg0.getTarget() instanceof MyTabConfig) {
						parent = ((MyTabConfig) arg0.getTarget()).getLinkedPanel();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:223");
					// safe fallback
				}

				if (parent == tabpanelDasbor) {
					if (tabpanelDasbor.getChildren().isEmpty()) {
						// PERBAIKAN: Menambahkan parameter window & dataLoader agar bisa ditutup dari
						// Dasbor
						TampilStudiMahasiswaHelper.initDashboard(tabpanelDasbor, mahasiswa, window, dataLoader, false);
					}
					return;
				}

				TampilStudiMahasiswaHelper.this.semesterPendek = (parent == tabpanel ? null
						: Perkuliahan.SEMESTER_PENDEK);
				TampilStudiMahasiswaHelper.this.remedial = (parent == tabpanelremedial);

				if (TampilStudiMahasiswaHelper.this.remedial) {
					TampilStudiMahasiswaHelper.this.semesterPendek = null;
				}

				if (parent.getChildren().isEmpty()) {
					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(parent);

					Center center = new Center();
					ais.ui.util.ZkCompat.setFlex(center, true);
					center.setParent(borderlayout);

					Grid grid = new Grid();
					grid.setSclass("dgrid fgrid");
					grid.setWidth("100%");
					grid.setHeight("100%");
					grid.setParent(center);

					Columns columns = new Columns();
					columns.setParent(grid);
					MyColumnConfig column1 = new MyColumnConfig();
					column1.setWidth("60px");
					column1.setParent(columns);
					MyColumnConfig column2 = new MyColumnConfig();
					column2.setParent(columns);

					Rows rows = new Rows();
					rows.setParent(grid);

					Row row = new MyRowStyled();
					row.setParent(rows);

					try {
						CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(row);
					} catch (Exception e) {
						new MyLabelKecil().setParent(row);
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:276");
					}

					Hbox hbox1 = new Hbox();
					hbox1.setParent(row);

					Vbox vbox = new Vbox();
					vbox.setParent(hbox1);
					vbox.appendChild(new MyLabelBoldAja(mahasiswa.getNim()));
					vbox.appendChild(new MyLabelBoldAja(mahasiswa.getNama()));

					Hbox hboxInfo = new Hbox();
					hboxInfo.setParent(vbox);
					mahasiswa.tampilkanHp(hboxInfo);
					mahasiswa.tampilkanEmail(hboxInfo);

					hbox1.appendChild(new Space());

					Vbox vboxAkademik = new Vbox();
					vboxAkademik.setParent(hbox1);
					String namaFakultas = (mahasiswa.getJurusan() != null
							&& mahasiswa.getJurusan().getFakultas() != null)
									? mahasiswa.getJurusan().getFakultas().getNama()
									: "";
					String namaJurusan = (mahasiswa.getJurusan() != null) ? mahasiswa.getJurusan().getNama() : "";
					String program = (mahasiswa.getProgram() != null) ? mahasiswa.getProgram() : "";

					vboxAkademik.appendChild(new MyLabelBoldAja(namaFakultas));
					vboxAkademik.appendChild(new MyLabelBoldAja(namaJurusan));
					vboxAkademik.appendChild(new MyLabelBoldAja(program));

					MyRowStyled row1 = new MyRowStyled();
					ais.ui.util.ZkCompat.setSpans(row1, "2");
					row1.setParent(rows);

					MyRowStyled row11 = new MyRowStyled();
					ais.ui.util.ZkCompat.setSpans(row11, "2");
					row11.setParent(rows);
					row11.appendChild(initMain(mahasiswa, dataLoader, row1, smtMulai));

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					toolbar.setStyle(
							"background: #ffffff; border-top: 1px solid #e2e8f0; padding: 16px 24px; display: flex; justify-content: flex-end; gap: 12px;");
					toolbar.setParent(south);

					MyToolbarbuttonConfig buttonTutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
					buttonTutup.setAttribute("janganDisabled", true);
					buttonTutup.setTooltiptext("Tutup");
					buttonTutup.addEventListener("onClick", new EventListener() {
						/**
						 * Tombol "Tutup" pada toolbar tab KRS: memanggil kembali {@code dataLoader} (bila ada) agar grid
						 * pemanggil me-refresh datanya, lalu melepas jendela dari komponen induk ({@code window.detach()}).
						 */
						@Override
						public void onEvent(Event event) throws Exception {
							if (dataLoader != null)
								dataLoader.loadData(event);
							window.detach();
						}
					});
					buttonTutup.setParent(toolbar);

					Tbmuser currentUser = Common.getCurrentUser();
					boolean adminLainBoleh = false;
					String admLain = Common
							.getKonfigurasi("admin_lain_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs", "")
							.getNilai();

					if (currentUser != null && currentUser.hakAkses() != null && admLain != null) {
						String[] aa = admLain.split(";");
						for (String a : aa) {
							if (a.trim().equalsIgnoreCase(currentUser.hakAkses().getRoleId())) {
								adminLainBoleh = true;
								break;
							}
						}
					}

					boolean isPrivilegedUser = currentUser != null && currentUser.hakAkses() != null
							&& ("Akademik".equalsIgnoreCase(currentUser.hakAkses().getRoleId())
									|| (ConstantValues.roleAdminFakultas != null && currentUser.hakAkses().getRoleId()
											.equals(ConstantValues.roleAdminFakultas.getRoleId()))
									|| (ConstantValues.roleAdminJurusan != null && currentUser.hakAkses().getRoleId()
											.equals(ConstantValues.roleAdminJurusan.getRoleId()))
									|| Tbmrole.ADMINISTRATOR.equals(currentUser.hakAkses().getRoleId()));

					if (!edit) {
						Common.freeze(window, true);
						buttonTutup.setDisabled(false);
						semesterMulai.setDisabled(false);
						semesterSampai.setDisabled(false);
						toolbarbuttonLihat.setDisabled(false);
					} else if (!adminLainBoleh && !isPrivilegedUser) {
						Common.freeze(window, true);
						buttonTutup.setDisabled(false);
						semesterMulai.setDisabled(false);
						semesterSampai.setDisabled(false);
						toolbarbuttonLihat.setDisabled(false);
					}
				}
			}
		};

		// Muat konten awal. Default: tab Dasbor (perilaku lama). Bila diminta (tombol "Daftar KRS"
		// pada daftar Manajemen KRS), buka LANGSUNG ke tab KRS — pilih tab KRS lalu muat isinya.
		if (bukaLangsungKeKrs) {
			try {
				tab.setSelected(true);
			} catch (Exception eSel) { ais.common.ErrorAuditUtil.record(eSel, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:384");
				// abaikan: pemilihan tab hanya kosmetik, pemuatan isi tetap dijalankan di bawah.
			}
			listener.onEvent(new Event("onClick", tab));
		} else {
			listener.onEvent(null);
		}
		tabDasbor.addEventListener("onClick", listener);
		tab.addEventListener("onClick", listener);
		tabsp.addEventListener("onClick", listener);
		tabremedial.addEventListener("onClick", listener);

		tababsen.addEventListener("onClick", new EventListener() {
			/**
			 * Pemuat malas tab <b>Absensi</b> (terpisah dari listener tab bersama karena isinya bukan grid
			 * KRS): sekali saja membangun {@code DashboardRekapAbsensiMahasiswa} untuk mahasiswa terkait dan
			 * memenuhi seluruh panel.
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelabsen.getChildren().isEmpty()) {
					DashboardRekapAbsensiMahasiswa laporan = new DashboardRekapAbsensiMahasiswa(mahasiswa);
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanelabsen);
				}
			}
		});

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Varian tanpa pilihan kurikulum (perilaku default): kurikulum dideteksi OTOMATIS = kurikulum
	 * TERBARU milik prodi mahasiswa. Setara memanggil {@link #buildMkBelumDiambilHtml(Mahasiswa, List, Long)}
	 * dengan {@code kurikulumId = null}.
	 */
	private static String buildMkBelumDiambilHtml(Mahasiswa mahasiswa,
			List<Map<String, Object>> detailData) {
		return buildMkBelumDiambilHtml(mahasiswa, detailData, null);
	}

	@SuppressWarnings("unchecked")
	/**
	 * HTML daftar MATA KULIAH BELUM DIAMBIL: bandingkan MK pada kurikulum prodi dengan MK yang sudah
	 * tercatat diambil ({@code detailData}, dicocokkan per KODE). Alat bantu (ekivalensi tak diperiksa
	 * penuh). Aman terhadap error (mengembalikan pesan, tak melempar).
	 *
	 * @param kurikulumId bila {@code null} &rarr; kurikulum dideteksi OTOMATIS (terbaru prodi mahasiswa,
	 *                    perilaku default/awal); bila diisi &rarr; memakai kurikulum ber-id tersebut
	 *                    (dipilih pengguna dari combo). Jika id tak ditemukan, jatuh ke deteksi otomatis.
	 */
	private static String buildMkBelumDiambilHtml(Mahasiswa mahasiswa,
			List<Map<String, Object>> detailData, Long kurikulumId) {
		return buildMkBelumDiambilHtml(mahasiswa, detailData, kurikulumId, null);
	}

	/**
	 * Versi dengan filter jenis/status mata kuliah (Wajib / Pilihan / Wajib Peminatan, dll).
	 * {@code statusFilter} null/kosong = semua jenis.
	 */
	private static String buildMkBelumDiambilHtml(Mahasiswa mahasiswa,
			List<Map<String, Object>> detailData, Long kurikulumId, String statusFilter) {
		MkbdData d = dataMkBelumDiambil(mahasiswa, detailData, kurikulumId, statusFilter);
		if (d.pesan != null) {
			return "<div style='padding:12px;color:#64748b;'>" + escapeHtml(d.pesan) + "</div>";
		}
		StringBuilder rows = new StringBuilder();
		for (String[] r : d.rows) {
			rows.append("<tr><td style='text-align:center'>").append(escapeHtml(r[0])).append("</td><td>")
					.append(escapeHtml(r[1])).append("</td><td>").append(escapeHtml(r[2]))
					.append("</td><td style='text-align:center'>").append(escapeHtml(r[3]))
					.append("</td><td style='text-align:center'>").append(escapeHtml(r[4]))
					.append("</td><td style='text-align:center'>").append(escapeHtml(r[5])).append("</td></tr>");
		}
		int nomor = d.rows.size();
		StringBuilder html = new StringBuilder();
		html.append("<div style='font-family:Arial,Helvetica,sans-serif;'>");
		html.append("<div style='display:flex;gap:10px;flex-wrap:wrap;margin-bottom:10px;'>");
		html.append(kotakRingkasMkbd("Kurikulum", escapeHtml(d.kurikulumNama), "#2563eb"));
		html.append(kotakRingkasMkbd(statusFilter == null || statusFilter.trim().isEmpty()
				? "Total MK Kurikulum" : "Total MK (" + escapeHtml(statusFilter) + ")", String.valueOf(d.totalKur), "#475569"));
		html.append(kotakRingkasMkbd("Belum Diambil", String.valueOf(nomor), nomor == 0 ? "#16a34a" : "#ea580c"));
		html.append(kotakRingkasMkbd("SKS Belum Diambil", String.valueOf(d.sksBelum), "#7c3aed"));
		html.append("</div>");
		if (nomor == 0) {
			html.append("<div style='padding:10px;background:#ecfdf5;border:1px solid #a7f3d0;border-radius:8px;"
					+ "color:#166534;'>Semua mata kuliah"
					+ (statusFilter == null || statusFilter.trim().isEmpty() ? "" : " (" + escapeHtml(statusFilter) + ")")
					+ " pada kurikulum ini sudah tercatat diambil.</div>");
		} else {
			html.append("<div style='overflow-x:auto;'><table style='width:100%;border-collapse:collapse;font-size:12px;'>");
			html.append("<thead><tr style='background:#f8fafc;color:#334155;'>")
					.append("<th style='padding:7px;border-bottom:1px solid #e2e8f0;width:40px;'>No</th>")
					.append("<th style='padding:7px;border-bottom:1px solid #e2e8f0;'>Kode</th>")
					.append("<th style='padding:7px;border-bottom:1px solid #e2e8f0;'>Mata Kuliah</th>")
					.append("<th style='padding:7px;border-bottom:1px solid #e2e8f0;width:60px;'>SKS</th>")
					.append("<th style='padding:7px;border-bottom:1px solid #e2e8f0;width:60px;'>Smt</th>")
					.append("<th style='padding:7px;border-bottom:1px solid #e2e8f0;width:120px;'>Jenis</th></tr></thead><tbody>");
			html.append(rows);
			html.append("</tbody></table></div>");
		}
		html.append("</div>");
		return html.toString();
	}

	/** Hasil data "MK Belum Diambil" (dipakai bersama oleh tampilan HTML dan export Excel). */
	private static class MkbdData {
		/** Nama kurikulum yang dipakai sebagai pembanding (hasil deteksi otomatis atau pilihan pengguna). */
		String kurikulumNama = "";
		/** Jumlah seluruh MK pada kurikulum tersebut SETELAH filter jenis/status diterapkan. */
		int totalKur = 0;
		/** Total SKS dari MK yang belum diambil (penjumlahan kolom SKS pada {@link #rows}). */
		int sksBelum = 0;
		/** Baris hasil, tiap elemen {@code {no, kode, nama, sks, semester, jenis/status}} (nilai mentah, belum di-escape HTML). */
		List<String[]> rows = new ArrayList<String[]>(); // {no, kode, nama, sks, smt, jenis/status}
		/**
		 * Bila tidak {@code null}, berisi pesan penjelas kenapa perbandingan tidak dapat dilakukan
		 * (mis. prodi/kurikulum tak ditemukan, atau terjadi error) &mdash; pemanggil WAJIB memeriksa
		 * field ini lebih dulu karena {@link #rows} tidak bermakna dalam keadaan tersebut.
		 */
		String pesan = null;
	}

	/**
	 * Hitung daftar MK kurikulum yang BELUM diambil mahasiswa, dengan opsi filter jenis/status MK.
	 * Session DEDIKASI sendiri (dipanggil setelah session utama initDashboard ditutup).
	 */
	private static MkbdData dataMkBelumDiambil(Mahasiswa mahasiswa,
			List<Map<String, Object>> detailData, Long kurikulumId, String statusFilter) {
		MkbdData res = new MkbdData();
		final String filter = (statusFilter == null || statusFilter.trim().isEmpty()) ? null : statusFilter.trim();
		org.hibernate.Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Set<String> taken = new HashSet<String>();
			if (detailData != null) {
				for (Map<String, Object> dt : detailData) {
					Object k = dt == null ? null : dt.get("kode");
					if (k != null && !k.toString().trim().isEmpty()) {
						taken.add(k.toString().trim().toUpperCase());
					}
				}
			}
			ais.database.model.Kurikulum kurikulum = null;
			if (kurikulumId != null) {
				kurikulum = (ais.database.model.Kurikulum) session.get(ais.database.model.Kurikulum.class, kurikulumId);
			}
			if (kurikulum == null && mahasiswa != null && mahasiswa.getJurusan() != null) {
				kurikulum = (ais.database.model.Kurikulum) session.createCriteria(ais.database.model.Kurikulum.class)
						.add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
						.addOrder(Order.desc("tahun")).setMaxResults(1).uniqueResult();
			}
			if (kurikulum == null) {
				res.pesan = "Kurikulum program studi tidak ditemukan untuk mahasiswa ini.";
				return res;
			}
			res.kurikulumNama = kurikulum.getNama() == null ? String.valueOf(kurikulum.getTahun()) : kurikulum.getNama();
			@SuppressWarnings("unchecked")
			List<ais.database.model.KurikulumPunyaMatakuliah> daftar = session
					.createCriteria(ais.database.model.KurikulumPunyaMatakuliah.class)
					.add(Restrictions.eq("kurikulum", kurikulum))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
					.createAlias("matakuliah", "matakuliah").addOrder(Order.asc("semester")).list();

			int nomor = 0;
			for (ais.database.model.KurikulumPunyaMatakuliah kpm : daftar) {
				Matakuliah mk = kpm == null ? null : kpm.getMatakuliah();
				if (mk == null) {
					continue;
				}
				String status = mk.getStatus() == null ? "" : mk.getStatus().trim();
				if (filter != null && !status.equalsIgnoreCase(filter)) {
					continue;
				}
				res.totalKur++;
				String kode = mk.getKode() == null ? "" : mk.getKode().trim();
				if (!kode.isEmpty() && taken.contains(kode.toUpperCase())) {
					continue;
				}
				nomor++;
				int sks = mk.getSks() == null ? 0 : mk.getSks().intValue();
				res.sksBelum += sks;
				res.rows.add(new String[] { String.valueOf(nomor), kode, mk.getNama() == null ? "-" : mk.getNama(),
						String.valueOf(sks), kpm.getSemester() == null ? "-" : String.valueOf(kpm.getSemester()),
						status.isEmpty() ? "-" : status });
			}
			return res;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit TampilStudiMahasiswaHelper.dataMkBelumDiambil");
			res.pesan = "Gagal memuat perbandingan kurikulum.";
			return res;
		} finally {
			closeOpenedSession(session);
		}
	}

	/** Daftar jenis/status MK (Wajib / Pilihan / Wajib Peminatan, dll) untuk combo filter — distinct dari master MK. */
	private static List<String> daftarStatusMatakuliah() {
		List<String> hasil = new ArrayList<String>();
		org.hibernate.Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			@SuppressWarnings("unchecked")
			List<Object> list = session.createCriteria(Matakuliah.class)
					.setProjection(org.hibernate.criterion.Projections.distinct(
							org.hibernate.criterion.Projections.property("status")))
					.list();
			for (Object o : list) {
				if (o != null && !o.toString().trim().isEmpty() && !hasil.contains(o.toString().trim())) {
					hasil.add(o.toString().trim());
				}
			}
			java.util.Collections.sort(hasil, String.CASE_INSENSITIVE_ORDER);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TampilStudiMahasiswaHelper.daftarStatusMatakuliah");
		} finally {
			closeOpenedSession(session);
		}
		if (hasil.isEmpty()) {
			// Fallback nilai standar bila query gagal / data kosong.
			hasil.add("Wajib");
			hasil.add("Pilihan");
			hasil.add("Wajib Peminatan");
		}
		return hasil;
	}

	/**
	 * Mengambil daftar kurikulum yang dimiliki PRODI (jurusan) mahasiswa untuk mengisi combo
	 * "Pilih Kurikulum" pada panel Mata Kuliah Belum Diambil. Diurutkan terbaru dulu
	 * (tahun menurun). Tiap elemen berupa {@code Object[]&#123;Long id, String label&#125;} di mana
	 * label memuat nama kurikulum, tahun, serta rentang Angkatan berlaku (mulai&ndash;sampai) bila
	 * tersedia &mdash; sesuai kebutuhan "pilih kurikulum sesuai prodi mhs (mulai berlaku, Angkatan)".
	 *
	 * <p>Memakai session DEDIKASI sendiri ({@code openSession}) yang SELALU ditutup di blok
	 * {@code finally} lewat {@link #closeOpenedSession(org.hibernate.Session)} agar tidak membocorkan
	 * koneksi &mdash; konsisten dengan {@link #buildMkBelumDiambilHtml(Mahasiswa, List, Long)} yang
	 * juga dipanggil setelah session utama ditutup. Aman terhadap error: bila gagal, mengembalikan
	 * daftar kosong sehingga combo hanya berisi pilihan "Otomatis" (perilaku default tetap jalan).
	 *
	 * @param mahasiswa mahasiswa acuan (dipakai prodi/jurusannya); boleh {@code null}.
	 * @return daftar {@code [id, label]} kurikulum prodi; tidak pernah {@code null} (bisa kosong).
	 */
	@SuppressWarnings("unchecked")
	private static List<Object[]> daftarKurikulumProdi(Mahasiswa mahasiswa) {
		List<Object[]> hasil = new ArrayList<Object[]>();
		if (mahasiswa == null || mahasiswa.getJurusan() == null) {
			return hasil;
		}
		org.hibernate.Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			List<ais.database.model.Kurikulum> list = session
					.createCriteria(ais.database.model.Kurikulum.class)
					.add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
					.addOrder(Order.desc("tahun")).list();
			for (ais.database.model.Kurikulum k : list) {
				if (k == null || k.getId() == null) {
					continue;
				}
				String nama = (k.getNama() == null || k.getNama().trim().isEmpty())
						? (k.getTahun() == null ? "Kurikulum" : String.valueOf(k.getTahun()))
						: k.getNama().trim();
				StringBuilder label = new StringBuilder(nama);
				if (k.getTahun() != null && (k.getNama() == null || !k.getNama().contains(String.valueOf(k.getTahun())))) {
					label.append(" (").append(k.getTahun()).append(")");
				}
				if (k.getTahunAngkatanMulai() != null || k.getTahunAngkatanSampai() != null) {
					label.append(" — Angkatan ")
							.append(k.getTahunAngkatanMulai() == null ? "…" : k.getTahunAngkatanMulai())
							.append(" s/d ")
							.append(k.getTahunAngkatanSampai() == null ? "…" : k.getTahunAngkatanSampai());
				}
				hasil.add(new Object[] { k.getId(), label.toString() });
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:581");
			// Diamkan: bila gagal, combo cukup berisi pilihan "Otomatis" (default tetap berfungsi).
		} finally {
			closeOpenedSession(session);
		}
		return hasil;
	}

	/** Satu kartu ringkas kecil (label + nilai berwarna) dipakai pada header panel "MK Belum Diambil". */
	private static String kotakRingkasMkbd(String label, String nilai, String warna) {
		return "<div style='flex:1;min-width:120px;background:#fff;border:1px solid #e2e8f0;border-left:4px solid "
				+ warna + ";border-radius:8px;padding:9px 12px;'>"
				+ "<div style='font-size:11px;color:#64748b;'>" + escapeHtml(label) + "</div>"
				+ "<div style='font-size:16px;font-weight:800;color:" + warna + ";'>" + escapeHtml(nilai)
				+ "</div></div>";
	}

	/**
	 * Membangun konten tab <b>Dasbor</b> (dipanggil malas saat tab pertama dipilih, dan ulang oleh
	 * tombol "Hitung Ulang"). Membaca seluruh {@link Detailperkuliahan} milik {@code mahasiswa} via
	 * session Hibernate dedikasi (SELALU ditutup di {@code finally} lewat
	 * {@link #closeOpenedSession(Session)}), meresolusi tiap MK ke ekivalennya
	 * ({@code Common.getMatakuliahApakahEkivalen}), lalu menghitung ringkasan (SKS
	 * kumulatif/tercatat, valid/tidak valid, lulus/belum &mdash; hanya bila konfigurasi Nilai Huruf
	 * yang berlaku mengizinkan status lulus, mis. jenjang S2 &mdash;, MK konversi) serta mendeteksi
	 * <b>kelompok kode MK sama/ekivalen</b> yang berpotensi membuat SKS Kumulatif lebih besar
	 * daripada hitung ulang resmi. Hasil dirender sebagai portal ZK (grid header, grafik tren,
	 * insight, komposisi status, catatan per semester, panel MK Belum Diambil dengan pemilih
	 * kurikulum/jenis MK, distribusi nilai, kehadiran, rekap KRS, dan tugas akhir) plus toolbar
	 * cetak laporan/ekspor Excel/hitung ulang/tutup.
	 *
	 * <p>Efek samping: membaca (tidak menulis) data KRS/nilai; {@code keDatabase=true} memicu
	 * {@code mahasiswa.reInitDetailperkuliahan(session)} dan sinkronisasi ulang tiap
	 * {@link KrsMahasiswa} lewat {@code Common.singkronkanKrsMahasiswa(...)} (bisa menulis KRS bila
	 * belum ada/berubah). No-op bila {@code parent == null}.</p>
	 *
	 * @param parent tabpanel Dasbor tujuan; dibersihkan lebih dulu ({@code Common.clear}).
	 * @param mahasiswa mahasiswa acuan.
	 * @param window jendela induk (dipakai tombol Tutup); boleh {@code null} bila dipanggil tanpa jendela modal.
	 * @param dataLoader dipanggil ulang saat tombol Tutup ditekan agar pemanggil me-refresh datanya; boleh {@code null}.
	 * @param keDatabase {@code true} untuk memaksa sinkronisasi ulang KRS/detail perkuliahan ke database (tombol "Hitung Ulang").
	 */
	public static void initDashboard(final Tabpanel parent, final Mahasiswa mahasiswa, final MyWindow window,
			final DataLoader dataLoader, boolean keDatabase) {
		boolean isMobile = Common.isMobile();
		if (parent == null) {
			return;
		}
		Common.clear(parent);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(parent);

		Center center = new Center();
		center.setAutoscroll(true);
		center.setParent(borderlayout);

		Vbox container = new Vbox();
		container.setWidth("100%");
		container.setSpacing("18px");
		container.setStyle("padding:18px; background:#f5f7fb; min-height:100%; box-sizing:border-box;");
		container.setParent(center);

		container.appendChild(new Html(buildDashboardHeaderHtml(mahasiswa)));

		Map<String, Map<String, Object>> mapSemesterData = new TreeMap<String, Map<String, Object>>();
		List<Map<String, Object>> detailData = new ArrayList<Map<String, Object>>();

		int totalValid = 0;
		int totalTidakValid = 0;
		int totalLulus = 0;
		int totalTidakLulus = 0;
		int totalKonversi = 0;
		int totalSksDiambil = 0;
		int semesterMaksimal = semesterAkademikMaksimal(mahasiswa);

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			List<Detailperkuliahan> detailperkuliahans = new ArrayList<Detailperkuliahan>();
			if (mahasiswa != null && semesterMaksimal >= 0) {
				detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.ge("semester", Integer.valueOf(0)))
						.add(Restrictions.le("semester", Integer.valueOf(semesterMaksimal)))
						.addOrder(Order.asc("semester"))
						.addOrder(Order.asc("id")).list();
			}

			if (keDatabase && mahasiswa != null) {
				mahasiswa.reInitDetailperkuliahan(session);
			}

			Collection<Long> detailperkuliahans2 = null;
			try {
				detailperkuliahans2 = mahasiswa == null ? null
						: mahasiswa.saringBerdasarNilaiDan0(mahasiswa.ambilDetailperkuliahan());
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:650");
			}
			Set<Long> validIds = new HashSet<Long>();
			if (detailperkuliahans2 != null) {
				validIds.addAll(detailperkuliahans2);
			}

			// Alasan mengapa sebuah mata kuliah "Tidak Valid" (map id -> penjelasan)
			Map<Long, String> alasanTidakValid = new HashMap<Long, String>();
			try {
				Map<Long, String> m = mahasiswa == null ? null
						: mahasiswa.alasanTidakValidDetail(mahasiswa.ambilDetailperkuliahan());
				if (m != null) {
					alasanTidakValid = m;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:666");
			}

			for (Detailperkuliahan dp : detailperkuliahans) {
				if (dp == null || !semesterAkademikValid(dp.getSemester(), semesterMaksimal)) {
					continue;
				}

				Detailperkuliahan dpValid = null;
				if (dp.getId() != null && validIds.contains(dp.getId())) {
					dpValid = dp;
				}

				Matakuliah matakuliah = dp.getPerkuliahan() != null ? dp.getPerkuliahan().getMatakuliah()
						: dp.getMatakuliahKonversi();
				if (matakuliah == null) {
					continue;
				}

				// Kode/Nama/SKS ASLI sebelum diselaraskan ke ekivalen → untuk membandingkan
				// "pengambilan ganda kode sama" vs "ekivalensi/penyetaraan dari kode lain".
				String kodeAsli = nvl(matakuliah.getKode(), "-");
				String namaAsli = nvl(matakuliah.getNama(), "-");
				Integer sksAsliInt = matakuliah.getSks();

				try {
					Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah,
							mahasiswa == null ? null : mahasiswa.getNim(), true);
					if (matakuliahs != null && matakuliahs.length > 0 && matakuliahs[0] != null) {
						matakuliah = matakuliahs[0];
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:698");
				}

				if (dpValid != null) {
					totalValid++;
				} else {
					totalTidakValid++;
				}

				// Status lulus HANYA dihitung/ditampilkan bila konfigurasi Nilai Huruf yang cocok
				// mencentang "Tampilkan status lulus/tidak lulus" (mis. hanya jenjang S2). Bila tidak,
				// status lulus/tidak-lulus disembunyikan di seluruh dasbor ini (totalLulus tetap 0).
				boolean tampilkanLulus = Boolean.TRUE.equals(ais.common.ConstantValues
						.tampilkanStatusLulusDariNilaiHuruf(dp.getNilaiHuruf(), dp.getMahasiswa()));
				if (tampilkanLulus) {
					if (Boolean.TRUE.equals(dp.getLulus())) {
						totalLulus++;
					} else {
						totalTidakLulus++;
					}
				}

				if (dp.getMatakuliahKonversi() != null) {
					totalKonversi++;
				}

				Integer sksMk = matakuliah.getSks();
				if (sksMk != null) {
					totalSksDiambil += sksMk.intValue();
				}

				String idSmtKey = dp.getIdSmt();
				Integer semesterPendekVal = dp.getPerkuliahan() != null ? dp.getPerkuliahan().getStatusSemesterPendek()
						: null;
				Integer tahapVal = dp.getTahap();
				Integer smtAsli = dp.getSemester();

				if (smtAsli != null && smtAsli.intValue() > 0 && idSmtKey != null
						&& !mapSemesterData.containsKey(idSmtKey)) {
					Map<String, Object> paramData = new HashMap<String, Object>();
					paramData.put("semesterAsli", smtAsli);
					paramData.put("tahap", tahapVal);
					paramData.put("semesterPendek", semesterPendekVal);
					mapSemesterData.put(idSmtKey, paramData);
				}

				Map<String, Object> dt = new HashMap<String, Object>();
				dt.put("kode", nvl(matakuliah.getKode(), "-"));
				dt.put("kodeAsli", kodeAsli);
				dt.put("namaAsli", namaAsli);
				dt.put("sksAsli", sksAsliInt == null ? Integer.valueOf(0) : sksAsliInt);
				dt.put("nama", nvl(matakuliah.getNama(), "-"));
				dt.put("smt", dp.getSemester());
				dt.put("sks", sksMk == null ? Integer.valueOf(0) : sksMk);
				dt.put("nilai_angka", dp.getTotalNilai());
				dt.put("nilai_ip", dp.getTotalIP());
				// MK SKS=0 yang sudah bernilai ditampilkan dengan nilai huruf "L" (selaras transkrip),
				// tetap TIDAK ikut menghitung IPK (SKS=0 -> kontribusi 0). Opsional per konfigurasi.
				String hurufTampil = dp.getNilaiHuruf();
				if (sksMk != null && sksMk.intValue() == 0 && hurufTampil != null && !hurufTampil.trim().isEmpty()
						&& Common.bolehKonfigurasi("tampilkan_matakuliah_sks_0_bernilai_di_transkrip",
								ais.database.model.Konfigurasi.TIDAK_AKTIF)) {
					hurufTampil = "L";
				}
				dt.put("huruf", hurufTampil);
				dt.put("valid", dpValid != null ? "Valid" : "Tidak Valid");
				dt.put("lulus", Boolean.TRUE.equals(dp.getLulus()) ? "Lulus" : "Belum/Tidak Lulus");
				dt.put("tampilLulus", tampilkanLulus ? "1" : "0");
				if (dpValid == null && dp.getId() != null) {
					String alasan = alasanTidakValid.get(dp.getId());
					if (alasan != null && alasan.length() > 0) {
						dt.put("alasan", alasan);
					}
				}
				String alasanLulus = dp.alasanBelumLulus();
				if (alasanLulus != null && alasanLulus.length() > 0) {
					dt.put("alasanLulus", alasanLulus);
				}
				detailData.add(dt);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:779");
		} finally {
			closeOpenedSession(session);
		}

		// === DETEKSI MATA KULIAH BERKODE SAMA / EKIVALEN (potensi SKS ganda) ===
		// detailData.kode SUDAH diselaraskan ke ekivalen, jadi MK berkode beda yang ekivalen
		// pun jatuh ke kode yang sama. Kode yang muncul >1x berpotensi membuat SKS Kumulatif
		// lebih besar daripada hasil hitung ulang resmi → ditampilkan untuk verifikasi.
		java.util.LinkedHashMap<String, List<Map<String, Object>>> grupKode = new java.util.LinkedHashMap<String, List<Map<String, Object>>>();
		for (Map<String, Object> dt : detailData) {
			String k = String.valueOf(dt.get("kode"));
			List<Map<String, Object>> g = grupKode.get(k);
			if (g == null) {
				g = new ArrayList<Map<String, Object>>();
				grupKode.put(k, g);
			}
			g.add(dt);
		}
		java.util.LinkedHashMap<String, List<Map<String, Object>>> grupGanda = new java.util.LinkedHashMap<String, List<Map<String, Object>>>();
		int kelompokKodeGanda = 0;
		int sksPotensiGanda = 0;
		int jumlahMkEkivalen = 0;
		int jumlahMkKodeSama = 0;
		for (Map.Entry<String, List<Map<String, Object>>> e : grupKode.entrySet()) {
			List<Map<String, Object>> g = e.getValue();
			if (g != null && g.size() > 1) {
				grupGanda.put(e.getKey(), g);
				kelompokKodeGanda++;
				int totalGrup = 0;
				int samaCount = 0;
				for (Map<String, Object> dt : g) {
					totalGrup += toIntSks(dt.get("sks"));
					String ka = String.valueOf(dt.get("kodeAsli"));
					// kodeAsli == kode resolved → kode benar-benar sama; selain itu → ekivalen.
					if (ka != null && ka.equals(e.getKey())) {
						samaCount++;
					} else {
						jumlahMkEkivalen++;
					}
				}
				if (samaCount > 1) {
					jumlahMkKodeSama += samaCount;
				}
				sksPotensiGanda += (totalGrup - toIntSks(g.get(0).get("sks")));
			}
		}

		List<Map<String, Object>> trendData = new ArrayList<Map<String, Object>>();
		double kumulatifSks = 0.0;
		double finalIpk = 0.0;
		double currentIps = 0.0;

		for (Map.Entry<String, Map<String, Object>> entry : mapSemesterData.entrySet()) {
			String idSmt = entry.getKey();
			Map<String, Object> params = entry.getValue();

			Integer smt = (Integer) params.get("semesterAsli");
			Integer dynamicTahapan = (Integer) params.get("tahap");
			Integer dynamicSemesterPendek = (Integer) params.get("semesterPendek");

			KrsMahasiswa krsMahasiswa = null;
			try {
				krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt, dynamicTahapan, dynamicSemesterPendek,
						keDatabase);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:845");
			}

			if (krsMahasiswa != null && krsMahasiswa.getMahasiswa() != null
					&& krsMahasiswa.getMahasiswa().getStatusKeluar() != null
					&& krsMahasiswa.getMahasiswa().getSemesterLulus() != null && krsMahasiswa.getSemester() != null
					&& krsMahasiswa.getSemester().intValue() > krsMahasiswa.getMahasiswa().getSemesterLulus().intValue()) {
				continue;
			}

			String namaDosenPembimbing = "-";
			String keteranganKrs = "-";
			String catatanKrs = "-";
			String catatanKhs = "-";
			double ips = 0.0;
			double ipk = 0.0;
			int sksSmt = 0;
			int sksKum = 0;
			int sksKonv = 0;
			int sksBukanKonv = 0;

			if (krsMahasiswa != null) {
				namaDosenPembimbing = krsMahasiswa.getDosenPa() == null ? "-" : nvl(krsMahasiswa.getDosenPa().getNama(), "-");
				try {
					keteranganKrs = nvl(KrsDetailHelper.rubahKeteranganPengambilanKRS(krsMahasiswa, false), "-");
				} catch (Exception e) {
					keteranganKrs = "-";
				}
				catatanKrs = nvl(krsMahasiswa.getCatatan(), "-");
				catatanKhs = nvl(krsMahasiswa.getCatatan(), "-");
				ips = krsMahasiswa.getIps() != null ? krsMahasiswa.getIps().doubleValue() : 0.0;
				ipk = krsMahasiswa.getIpk() != null ? krsMahasiswa.getIpk().doubleValue() : 0.0;
				sksSmt = krsMahasiswa.getSksYangDiambil() != null ? krsMahasiswa.getSksYangDiambil().intValue() : 0;
				sksKum = krsMahasiswa.getSksk() != null ? krsMahasiswa.getSksk().intValue() : 0;
				sksKonv = krsMahasiswa.getSksKonversi() != null ? krsMahasiswa.getSksKonversi().intValue() : 0;
				sksBukanKonv = krsMahasiswa.getSksBukanKonversi() != null
						? krsMahasiswa.getSksBukanKonversi().intValue()
						: 0;
			}

			kumulatifSks = sksKum;
			finalIpk = ipk;
			currentIps = ips;

			Map<String, Object> tr = new HashMap<String, Object>();
			tr.put("idSmt", idSmt);
			tr.put("smt", smt);
			tr.put("sks", Integer.valueOf(sksSmt));
			tr.put("sksk", Integer.valueOf(sksKum));
			tr.put("skskonversi", Integer.valueOf(sksKonv));
			tr.put("sksBukanKonversi", Integer.valueOf(sksBukanKonv));
			tr.put("ips", Double.valueOf(ips));
			tr.put("ipk", Double.valueOf(ipk));
			tr.put("namaDosenPembimbing", namaDosenPembimbing);
			tr.put("keteranganKrs", keteranganKrs);
			tr.put("catatanKrs", catatanKrs);
			tr.put("catatanKhs", catatanKhs);
			trendData.add(tr);
		}

		Groupbox infoBox = createModernGroupbox(container, "Ringkasan Cepat Studi Mahasiswa",
				"Panel ini menampilkan kondisi akademik utama mahasiswa dalam angka sederhana. Gunakan bagian ini untuk melihat perkembangan SKS, IPK, IPS, kelulusan mata kuliah, dan validitas data nilai secara cepat.");
		org.zkoss.zul.Div hInfo = new org.zkoss.zul.Div();
		hInfo.setStyle(isMobile ? "display:block; padding:18px;" : "display:flex; flex-wrap:wrap; gap:14px; padding:18px;");
		hInfo.setWidth("100%");
		hInfo.setParent(infoBox);

		hInfo.appendChild(createBoxInfo("SKS Kumulatif", String.valueOf((int) kumulatifSks), "#2563eb", isMobile,
				"Jumlah SKS yang sudah dihitung sebagai capaian sampai semester terakhir."));
		hInfo.appendChild(createBoxInfo("IPK Terkini", formatNumber(finalIpk), "#16a34a", isMobile,
				"Rata-rata nilai kumulatif mahasiswa sampai data terakhir."));
		hInfo.appendChild(createBoxInfo("IPS Terakhir", formatNumber(currentIps), "#0d9488", isMobile,
				"Nilai rata-rata semester terakhir yang sudah tercatat."));
		hInfo.appendChild(createBoxInfo("Total Mata Kuliah", String.valueOf(detailData.size()), "#475569", isMobile,
				"Jumlah mata kuliah yang pernah diambil atau tercatat pada studi mahasiswa."));
		hInfo.appendChild(createBoxInfo("Valid / Tidak Valid", totalValid + " / " + totalTidakValid, "#7c3aed", isMobile,
				"Membantu operator melihat mata kuliah yang sudah masuk perhitungan nilai dan yang belum."));
		// Kotak "Lulus / Belum" hanya tampil bila ada MK yang konfigurasinya mengizinkan status lulus
		// (mis. jenjang S2). totalLulus+totalTidakLulus==0 berarti tak ada -> sembunyikan.
		if (totalLulus + totalTidakLulus > 0) {
			hInfo.appendChild(createBoxInfo("Lulus / Belum", totalLulus + " / " + totalTidakLulus, "#ea580c", isMobile,
					"Perbandingan mata kuliah yang sudah lulus dan yang masih perlu perhatian."));
		}
		hInfo.appendChild(createBoxInfo("MK Konversi", String.valueOf(totalKonversi), "#db2777", isMobile,
				"Mata kuliah dari proses konversi/penyetaraan nilai."));
		hInfo.appendChild(createBoxInfo("SKS Riwayat", String.valueOf(totalSksDiambil), "#0891b2", isMobile,
				"Akumulasi SKS pada riwayat detail mata kuliah yang ditampilkan."));
		hInfo.appendChild(createBoxInfo("MK Ekivalen", String.valueOf(jumlahMkEkivalen), "#dc2626", isMobile,
				"Jumlah mata kuliah yang berasal dari KODE LAIN melalui ekivalensi/penyetaraan (kode asli berbeda dengan kode yang dipakai menghitung)."));
		hInfo.appendChild(createBoxInfo("MK Kode Sama", String.valueOf(jumlahMkKodeSama), "#ea580c", isMobile,
				"Jumlah mata kuliah dengan KODE IDENTIK yang terambil lebih dari sekali."));
		hInfo.appendChild(createBoxInfo("Potensi SKS Ganda", String.valueOf(sksPotensiGanda), "#b91c1c", isMobile,
				"Perkiraan SKS yang berpotensi terhitung ganda akibat kode sama/ekivalen. Cocokkan dengan selisih SKS saat hitung ulang."));

		appendSectionIntro(container, "Susunan Dasbor Studi Mahasiswa",
				"Panel berikut disusun seperti portal sehingga ringkasan, grafik, catatan, dan daftar mata kuliah dapat dibaca per bagian. Setiap panel memiliki fungsi sederhana agar staf akademik, wali akademik, dan mahasiswa mudah memahami kondisi studi tanpa membaca data mentah terlalu banyak.");

		MyPortallayout portalLayout = new MyPortallayout();
		portalLayout.setWidth("100%");
		portalLayout.setMaximizedMode("whole");
		portalLayout.setStyle("margin-top:2px;padding:0;background:transparent;");
		portalLayout.setParent(container);

		String pcWidth = isMobile ? "100%" : "50%";
		MyPortalchildren pcTop = new MyPortalchildren();
		pcTop.setWidth("100%");
		pcTop.setStyle("padding:6px;box-sizing:border-box;");
		pcTop.setParent(portalLayout);

		// Panel verifikasi kode sama/ekivalen (hanya muncul bila ada) — ditaruh paling atas
		// agar operator mudah mengecek penyebab selisih SKS.
		if (!grupGanda.isEmpty()) {
			Vbox panelDuplikat = createPortalPanel(pcTop, "Mata Kuliah Berkode Sama / Ekivalen (Perlu Verifikasi)",
					"Daftar mata kuliah yang muncul lebih dari sekali dengan kode sama atau hasil penyetaraan/ekivalensi. Item ini berpotensi membuat SKS Kumulatif lebih besar daripada hasil hitung ulang. Periksa pengambilan ganda, ekivalensi, atau konversinya.");
			new Html(buildDuplikatEkivalenHtml(grupGanda)).setParent(panelDuplikat);
		}

		MyPortalchildren pcLeft = new MyPortalchildren();
		pcLeft.setWidth(pcWidth);
		pcLeft.setStyle("padding:6px;box-sizing:border-box;");
		pcLeft.setParent(portalLayout);

		MyPortalchildren pcRight = new MyPortalchildren();
		pcRight.setWidth(pcWidth);
		pcRight.setStyle("padding:6px;box-sizing:border-box;");
		pcRight.setParent(portalLayout);

		MyPortalchildren pcBottom = new MyPortalchildren();
		pcBottom.setWidth("100%");
		pcBottom.setStyle("padding:6px;box-sizing:border-box;");
		pcBottom.setParent(portalLayout);

		Vbox panelChart = createPortalPanel(pcTop, "Grafik Tren Studi",
				"Panel ini menggambarkan perubahan SKS, IPS, dan IPK dari semester ke semester. Tujuannya agar perkembangan studi mahasiswa mudah dilihat sekilas, apakah cenderung membaik, stabil, atau perlu perhatian.");
		createTrendAkademikHtml(trendData).setParent(panelChart);

		Vbox panelInsight = createPortalPanel(pcLeft, "Analisa Singkat Akademik",
				"Panel ini menerjemahkan data nilai menjadi kesimpulan sederhana, seperti arah IPK, capaian SKS, dan status kelulusan mata kuliah. Bagian ini membantu wali akademik memberi arahan tanpa harus membaca seluruh tabel.");
		new Html(buildAcademicInsightHtml(trendData, totalValid, totalTidakValid, totalLulus, totalTidakLulus)).setParent(panelInsight);

		Vbox panelTrend = createPortalPanel(pcLeft, "Rangkuman Nilai per Semester",
				"Tabel ini menampilkan IPS, IPK, SKS semester, SKS kumulatif, dan SKS konversi per semester. Gunakan panel ini untuk membandingkan hasil studi antar semester secara berurutan.");
		Grid gridTrend = new Grid();
		gridTrend.setMold("paging");
		gridTrend.setPageSize(10);
		gridTrend.setStyle("border:0;margin:0;");
		gridTrend.setParent(panelTrend);
		Columns colsTrend = new Columns();
		colsTrend.setParent(gridTrend);
		new MyColumnConfig("ID Smt").setParent(colsTrend);
		new MyColumnConfig("Smt").setParent(colsTrend);
		new MyColumnConfig("SKS Smt").setParent(colsTrend);
		new MyColumnConfig("SKS Kum").setParent(colsTrend);
		new MyColumnConfig("SKS Konv").setParent(colsTrend);
		new MyColumnConfig("IPS").setParent(colsTrend);
		new MyColumnConfig("IPK").setParent(colsTrend);

		Rows rowsTrend = new Rows();
		rowsTrend.setParent(gridTrend);
		for (Map<String, Object> tr : trendData) {
			Row rowTr = new MyRowStyled();
			rowTr.setParent(rowsTrend);
			new Label(String.valueOf(tr.get("idSmt"))).setParent(rowTr);
			new Label(String.valueOf(tr.get("smt"))).setParent(rowTr);
			new Label(String.valueOf(tr.get("sks"))).setParent(rowTr);
			new Label(String.valueOf(tr.get("sksk"))).setParent(rowTr);
			new Label(String.valueOf(tr.get("skskonversi"))).setParent(rowTr);
			Label lblIps = new Label(formatNumber(toDouble(tr.get("ips"))));
			lblIps.setStyle("font-weight:700;color:#0d9488;");
			lblIps.setParent(rowTr);
			Label lblIpk = new Label(formatNumber(toDouble(tr.get("ipk"))));
			lblIpk.setStyle("font-weight:700;color:#16a34a;");
			lblIpk.setParent(rowTr);
		}

		Vbox panelList = createPortalPanel(pcRight, "Daftar Historis Pengambilan Mata Kuliah",
				"Daftar ini berisi semua mata kuliah yang pernah diambil, nilai yang tercatat, status validitas, dan status kelulusan. Panel ini berguna untuk menemukan mata kuliah yang perlu dicek ulang atau menjadi perhatian akademik.");
		Grid gridList = new Grid();
		gridList.setMold("paging");
		gridList.setPageSize(15);
		gridList.setStyle("border:0;margin:0;");
		gridList.setParent(panelList);

		Columns colsList = new Columns();
		colsList.setParent(gridList);
		MyColumnConfig col1 = new MyColumnConfig("Kode");
		col1.setWidth("14%");
		col1.setParent(colsList);
		new MyColumnConfig("Mata Kuliah").setParent(colsList);
		MyColumnConfig col3 = new MyColumnConfig("Smt");
		col3.setWidth("8%");
		col3.setParent(colsList);
		MyColumnConfig col4 = new MyColumnConfig("SKS");
		col4.setWidth("8%");
		col4.setParent(colsList);
		MyColumnConfig col5 = new MyColumnConfig("Angka / IP / Huruf");
		col5.setWidth(isMobile ? "28%" : "21%");
		col5.setParent(colsList);
		MyColumnConfig col6 = new MyColumnConfig("Status");
		col6.setWidth("17%");
		col6.setParent(colsList);

		Rows rowsList = new Rows();
		rowsList.setParent(gridList);
		for (Map<String, Object> dt : detailData) {
			Row rowDt = new MyRowStyled();
			rowDt.setParent(rowsList);
			new Label(String.valueOf(dt.get("kode"))).setParent(rowDt);
			new Label(String.valueOf(dt.get("nama"))).setParent(rowDt);
			new Label(String.valueOf(dt.get("smt"))).setParent(rowDt);
			new Label(String.valueOf(dt.get("sks"))).setParent(rowDt);

			String nilaiAngkaStr = dt.get("nilai_angka") != null ? formatNumber(toDouble(dt.get("nilai_angka"))) : "0";
			String nilaiIpStr = dt.get("nilai_ip") != null ? formatNumber(toDouble(dt.get("nilai_ip"))) : "0";
			String hurufStr = dt.get("huruf") != null ? String.valueOf(dt.get("huruf")) : "-";
			new MyLabelBoldAja(nilaiAngkaStr + " / " + nilaiIpStr + " / " + hurufStr).setParent(rowDt);

			boolean isValid = "Valid".equals(String.valueOf(dt.get("valid")));
			boolean tampilLulusDt = "1".equals(String.valueOf(dt.get("tampilLulus")));
			// Gabungkan status lulus ke badge HANYA bila konfig Nilai Huruf mengizinkan (mis. jenjang S2);
			// selain itu badge cukup menampilkan status validitas saja (tanpa " - Lulus/Tidak Lulus").
			String statusText = tampilLulusDt
					? String.valueOf(dt.get("valid")) + " - " + String.valueOf(dt.get("lulus"))
					: String.valueOf(dt.get("valid"));
			Vbox boxStatus = new Vbox();
			boxStatus.setWidth("100%");
			boxStatus.setSpacing("3px");
			boxStatus.setParent(rowDt);
			Label lblStatus = new Label(statusText);
			lblStatus.setStyle(isValid
					? "color:#166534;background:#dcfce7;padding:4px 10px;border-radius:999px;font-size:11px;font-weight:700;"
					: "color:#991b1b;background:#fee2e2;padding:4px 10px;border-radius:999px;font-size:11px;font-weight:700;");
			lblStatus.setParent(boxStatus);
				boolean belumLulus = tampilLulusDt && "Belum/Tidak Lulus".equals(String.valueOf(dt.get("lulus")));
				String alasanLulus = dt.get("alasanLulus") != null ? String.valueOf(dt.get("alasanLulus")) : "";
				if (belumLulus && alasanLulus.length() > 0) {
					new Html("<div title='" + escapeHtml(alasanLulus)
							+ "' style='font-size:9.5px;line-height:1.35;color:#b45309;background:#fffbeb;"
							+ "border:1px solid #fde68a;border-radius:6px;padding:4px 6px;'><b>Alasan belum lulus:</b> "
							+ escapeHtml(alasanLulus) + "</div>").setParent(boxStatus);
				}
			String alasanStatus = dt.get("alasan") != null ? String.valueOf(dt.get("alasan")) : "";
			if (!isValid && alasanStatus.length() > 0) {
				new Html("<div title='" + escapeHtml(alasanStatus)
						+ "' style='font-size:9.5px;line-height:1.35;color:#b91c1c;background:#fef2f2;"
						+ "border:1px solid #fecaca;border-radius:6px;padding:4px 6px;'><b>Alasan tidak valid:</b> "
						+ escapeHtml(alasanStatus) + "</div>").setParent(boxStatus);
			}
		}

		Vbox panelKomposisi = createPortalPanel(pcRight, "Komposisi Status Mata Kuliah",
				"Panel ini memperlihatkan komposisi sederhana antara mata kuliah valid, belum valid, lulus, belum lulus, dan konversi. Tujuannya agar operator cepat tahu bagian data mana yang paling perlu diperiksa.");
		new Html(buildSubjectCompositionHtml(totalValid, totalTidakValid, totalLulus, totalTidakLulus, totalKonversi, detailData.size()))
				.setParent(panelKomposisi);

		Vbox panelCatatan = createPortalPanel(pcBottom, "Catatan Akademik dan Dosen Pembimbing",
				"Bagian ini menampilkan catatan KRS/KHS, dosen pembimbing akademik, dan keterangan pengambilan KRS. Informasi ini membantu pengguna memahami alasan atau konteks di balik kondisi studi mahasiswa.");
		Grid gridCatatan = new Grid();
		gridCatatan.setMold("paging");
		gridCatatan.setPageSize(10);
		gridCatatan.setStyle("border:0;margin:0;");
		gridCatatan.setParent(panelCatatan);

		Columns colsCatatan = new Columns();
		colsCatatan.setParent(gridCatatan);
		MyColumnConfig colCt1 = new MyColumnConfig("ID Smt");
		colCt1.setWidth("10%");
		colCt1.setParent(colsCatatan);
		MyColumnConfig colCt2 = new MyColumnConfig("SMT");
		colCt2.setWidth("8%");
		colCt2.setParent(colsCatatan);
		MyColumnConfig colCt3 = new MyColumnConfig("Dosen PA");
		colCt3.setWidth("20%");
		colCt3.setParent(colsCatatan);
		new MyColumnConfig("Keterangan").setParent(colsCatatan);
		new MyColumnConfig("Catatan KRS").setParent(colsCatatan);
		new MyColumnConfig("Catatan KHS").setParent(colsCatatan);

		Rows rowsCatatan = new Rows();
		rowsCatatan.setParent(gridCatatan);
		for (Map<String, Object> tr : trendData) {
			Row rowCt = new MyRowStyled();
			rowCt.setParent(rowsCatatan);
			new Label(String.valueOf(tr.get("idSmt"))).setParent(rowCt);
			new Label(String.valueOf(tr.get("smt"))).setParent(rowCt);
			new Label(String.valueOf(tr.get("namaDosenPembimbing"))).setParent(rowCt);
			// Keterangan sengaja memakai komponen Html agar format keterangan dari KRS tetap terbaca rapi.
			new Html(tr.get("keteranganKrs") == null ? "-" : String.valueOf(tr.get("keteranganKrs"))).setParent(rowCt);
			new Label(String.valueOf(tr.get("catatanKrs"))).setParent(rowCt);
			new Label(String.valueOf(tr.get("catatanKhs"))).setParent(rowCt);
		}

		Vbox panelRekomendasi = createPortalPanel(pcBottom, "Rekomendasi Tindak Lanjut Akademik",
				"Panel ini memberikan arahan umum berdasarkan data nilai dan KRS, misalnya data yang perlu divalidasi atau mata kuliah yang belum lulus. Rekomendasi ini bukan keputusan final, tetapi alat bantu agar pemeriksaan akademik lebih terarah.");
		new Html(buildFollowUpRecommendationHtml(trendData, totalValid, totalTidakValid, totalLulus, totalTidakLulus, totalKonversi))
				.setParent(panelRekomendasi);

		// Panel MK BELUM DIAMBIL dari kurikulum prodi (alat bantu pemantauan akademik).
		Vbox panelBelumDiambil = createPortalPanel(pcBottom, "Mata Kuliah Belum Diambil (Kurikulum)",
				"Membandingkan mata kuliah pada kurikulum prodi dengan yang sudah diambil mahasiswa. Yang tampil "
						+ "adalah MK kurikulum yang BELUM tercatat diambil (alat bantu; periksa ekivalensi bila perlu).");

		// Pemilih Kurikulum. Default KOSONG = item pertama "(Otomatis …)" bernilai null → tabel tampil
		// seperti semula (kurikulum terbaru prodi). Bila pengguna memilih kurikulum tertentu (yang
		// terdaftar pada prodi mahasiswa), tabel dibangun ulang memakai kurikulum tersebut. detailData
		// disalin ke variabel final agar bisa dipakai di dalam listener (kompatibel Java 1.6/1.7).
		final List<Map<String, Object>> detailDataMkbd = detailData;
		final org.zkoss.zul.Combobox cbKurikulumMkbd = new org.zkoss.zul.Combobox();
		cbKurikulumMkbd.setReadonly(true);
		cbKurikulumMkbd.setWidth("380px");
		cbKurikulumMkbd.setStyle("max-width:100%;");
		cbKurikulumMkbd.setTooltiptext("Pilih kurikulum sesuai prodi mahasiswa; kosongkan untuk otomatis (terbaru).");
		org.zkoss.zul.Comboitem itemOtomatis = new org.zkoss.zul.Comboitem("(Otomatis — kurikulum terbaru sesuai prodi)");
		itemOtomatis.setValue(null);
		cbKurikulumMkbd.appendChild(itemOtomatis);
		for (Object[] kur : daftarKurikulumProdi(mahasiswa)) {
			org.zkoss.zul.Comboitem it = new org.zkoss.zul.Comboitem(String.valueOf(kur[1]));
			it.setValue(kur[0]);
			cbKurikulumMkbd.appendChild(it);
		}
		cbKurikulumMkbd.setSelectedItem(itemOtomatis);

		// Filter jenis/status MK (Wajib / Pilihan / Wajib Peminatan, dll). Default "(Semua Jenis)" = tanpa filter.
		final org.zkoss.zul.Combobox cbJenisMkbd = new org.zkoss.zul.Combobox();
		cbJenisMkbd.setReadonly(true);
		cbJenisMkbd.setWidth("200px");
		cbJenisMkbd.setTooltiptext("Saring MK belum diambil berdasarkan jenis: Wajib, Pilihan, Wajib Peminatan, dll.");
		org.zkoss.zul.Comboitem itemSemuaJenis = new org.zkoss.zul.Comboitem("(Semua Jenis)");
		itemSemuaJenis.setValue(null);
		cbJenisMkbd.appendChild(itemSemuaJenis);
		for (String st : daftarStatusMatakuliah()) {
			org.zkoss.zul.Comboitem it = new org.zkoss.zul.Comboitem(st);
			it.setValue(st);
			cbJenisMkbd.appendChild(it);
		}
		cbJenisMkbd.setSelectedItem(itemSemuaJenis);

		org.zkoss.zul.Hbox barKurikulum = new org.zkoss.zul.Hbox();
		barKurikulum.setAlign("center");
		barKurikulum.setStyle("gap:8px;flex-wrap:wrap;margin-bottom:6px;");
		new Html("<span style='font-size:12px;font-weight:700;color:#334155;'>Pilih Kurikulum:</span>")
				.setParent(barKurikulum);
		barKurikulum.appendChild(cbKurikulumMkbd);
		new Html("<span style='font-size:12px;font-weight:700;color:#334155;margin-left:6px;'>Jenis MK:</span>")
				.setParent(barKurikulum);
		barKurikulum.appendChild(cbJenisMkbd);
		barKurikulum.setParent(panelBelumDiambil);

		// Wadah khusus HTML agar bisa diganti isinya saat kurikulum/jenis dipilih (tanpa membangun ulang panel).
		final org.zkoss.zul.Div hostMkbd = new org.zkoss.zul.Div();
		hostMkbd.setWidth("100%");
		hostMkbd.setParent(panelBelumDiambil);
		new Html(buildMkBelumDiambilHtml(mahasiswa, detailData, null)).setParent(hostMkbd);

		// Listener bersama: bangun ulang tabel memakai kurikulum + jenis MK yang sedang dipilih.
		final EventListener refreshMkbd = new EventListener() {
			/**
			 * Bangun ulang HANYA tabel "Mata Kuliah Belum Diambil" sesuai kurikulum dan jenis MK yang sedang
			 * dipilih, tanpa membangun ulang panel/dasbor. Isi {@code hostMkbd} dikosongkan lalu diisi
			 * {@code Html} baru dari {@link TampilStudiMahasiswaHelper#buildMkBelumDiambilHtml(Mahasiswa, List, Long, String)}.
			 *
			 * <p>Nilai {@code null} pada kedua combo bermakna "tanpa batasan": kurikulum {@code null} &rarr;
			 * deteksi otomatis kurikulum terbaru prodi, jenis {@code null} &rarr; semua jenis MK. Listener ini
			 * dipasang PADA KEDUA combo sehingga perubahan salah satu selalu memakai nilai terkini keduanya.
			 * Dipakai kembali {@code detailDataMkbd} (salinan final {@code detailData} yang sudah dimuat),
			 * jadi tidak ada query ulang riwayat MK &mdash; hanya query kurikulum yang diulang.
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				Long idKur = (cbKurikulumMkbd.getSelectedItem() == null
						|| cbKurikulumMkbd.getSelectedItem().getValue() == null) ? null
								: ((Number) cbKurikulumMkbd.getSelectedItem().getValue()).longValue();
				String jenis = (cbJenisMkbd.getSelectedItem() == null
						|| cbJenisMkbd.getSelectedItem().getValue() == null) ? null
								: cbJenisMkbd.getSelectedItem().getValue().toString();
				ais.common.Common.clear(hostMkbd);
				new Html(buildMkBelumDiambilHtml(mahasiswa, detailDataMkbd, idKur, jenis)).setParent(hostMkbd);
			}
		};
		cbKurikulumMkbd.addEventListener("onChange", refreshMkbd);
		cbJenisMkbd.addEventListener("onChange", refreshMkbd);

		// ── Panel tambahan (relevan dengan riwayat pembelajaran mahasiswa) ──
		Vbox panelDistribusi = createPortalPanel(pcRight, "Distribusi Nilai Huruf",
				"Grafik donat sebaran nilai huruf (A/B/C/D/E, dst.) dari seluruh mata kuliah yang tercatat, untuk melihat proporsi nilai baik dibanding yang perlu perhatian.");
		new Html(buildDistribusiNilaiHtml(detailData)).setParent(panelDistribusi);

		Vbox panelKehadiran = createPortalPanel(pcLeft, "Rekap Kehadiran Perkuliahan",
				"Rekap kehadiran mahasiswa (Hadir / Izin / Sakit / Alpa) dari seluruh pertemuan perkuliahan yang tercatat, sebagai indikator keaktifan mengikuti perkuliahan.");
		new Html(buildKehadiranHtml(mahasiswa)).setParent(panelKehadiran);

		Vbox panelKrs = createPortalPanel(pcBottom, "Rekap KRS per Semester",
				"Ringkasan KRS tiap semester: SKS diambil, jumlah MK yang sudah/belum disetujui, IPS, dan IPK. Berguna memantau perkembangan pengambilan rencana studi.");
		new Html(buildKrsPerSemesterHtml(mahasiswa)).setParent(panelKrs);

		Vbox panelTugasAkhir = createPortalPanel(pcBottom, "Tugas Akhir & Pengajuan Judul",
				"Riwayat pengajuan judul tugas akhir (status Pengajuan/Disetujui/Ditolak/Sidang) beserta data skripsi: pembimbing, nilai, status sidang, dan tanggal sidang.");
		new Html(buildTugasAkhirHtml(mahasiswa)).setParent(panelTugasAkhir);

		South south = new South();
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("background:#ffffff;border-top:1px solid #e2e8f0;padding:12px 18px;text-align:right;");
		toolbar.setParent(south);

		// Cetak PDF ber-GRAFIK: gunakan mesin laporan DashboardReportKit (pola sama dengan log_login.zul).
		// Laporan HTML lengkap (grafik donut/batang HTML-CSS + tabel) dibuka di iframe modal lalu bisa
		// dicetak / disimpan sebagai PDF lewat dialog cetak peramban — sehingga SEMUA grafik ikut tercetak.
		final Vbox ownerLaporanStudi = panelDistribusi;
		MyToolbarbuttonConfig buttonPdf = new MyToolbarbuttonConfig("Cetak Laporan (Grafik & Tabel)", "/img/print.png");
		buttonPdf.setAttribute("janganDisabled", true);
		buttonPdf.setTooltiptext("Buka laporan lengkap ber-grafik (Distribusi Nilai, Kehadiran, SKS per Semester) + tabel yang siap dicetak atau disimpan sebagai PDF.");
		buttonPdf.addEventListener("onClick", new EventListener() {
			/**
			 * Tombol "Cetak Laporan (Grafik &amp; Tabel)": membuka laporan HTML lengkap ber-grafik lewat
			 * {@code DashboardReportKit.bukaLaporan} dengan sumber data dari
			 * {@link TampilStudiMahasiswaHelper#buildSumberLaporanStudi(Mahasiswa, List)}. Laporan tampil di
			 * iframe modal sehingga bisa dicetak/disimpan sebagai PDF oleh peramban beserta seluruh grafiknya.
			 * {@code ownerLaporanStudi} (panel Distribusi Nilai) hanya dipakai sebagai komponen induk/pemilik
			 * jendela laporan.
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				ais.action.master.helper.DashboardReportKit.bukaLaporan(ownerLaporanStudi,
						buildSumberLaporanStudi(mahasiswa, detailDataMkbd));
			}
		});
		buttonPdf.setParent(toolbar);

		MyToolbarbuttonConfig buttonExport = new MyToolbarbuttonConfig("Export Excel", "/img/excel.png");
		buttonExport.setAttribute("janganDisabled", true);
		buttonExport.setTooltiptext("Unduh data studi mahasiswa (Ringkasan, Riwayat MK, KRS, Kehadiran, Distribusi Nilai, Skripsi, Pengajuan Judul, MK Belum Diambil) ke berkas Excel (.xlsx, sheet terpisah).");
		buttonExport.addEventListener("onClick", new EventListener() {
			/**
			 * Tombol "Export Excel": mengunduh seluruh data studi mahasiswa ke berkas {@code .xlsx} multi-sheet
			 * lewat {@link TampilStudiMahasiswaHelper#exportStudiMahasiswaExcel(Mahasiswa, List)}. Memakai
			 * {@code detailDataMkbd} yang sudah dimuat dasbor, sehingga riwayat MK tidak diquery ulang.
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				exportStudiMahasiswaExcel(mahasiswa, detailDataMkbd);
			}
		});
		buttonExport.setParent(toolbar);

		MyToolbarbuttonConfig buttonHitungUlang = new MyToolbarbuttonConfig("Hitung Ulang", "/img/svg/refresh.svg");
		buttonHitungUlang.setAttribute("janganDisabled", true);
		buttonHitungUlang.setTooltiptext("Hitung ulang ringkasan akademik dan sinkronkan data terbaru ke database.");
		buttonHitungUlang.addEventListener("onClick", new EventListener() {
			/**
			 * Tombol "Hitung Ulang": membangun ULANG seluruh dasbor pada panel yang sama dengan
			 * {@code keDatabase = true}, sehingga {@code initDashboard} memanggil
			 * {@code mahasiswa.reInitDetailperkuliahan(session)} &mdash; artinya hasil hitung ulang IP/IPK dan
			 * sinkronisasi nilai benar-benar DITULIS ke database, bukan sekadar dihitung untuk tampilan.
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				TampilStudiMahasiswaHelper.initDashboard(parent, mahasiswa, window, dataLoader, true);
			}
		});
		buttonHitungUlang.setParent(toolbar);

		if (window != null) {
			MyToolbarbuttonConfig buttonTutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			buttonTutup.setAttribute("janganDisabled", true);
			buttonTutup.setTooltiptext("Tutup jendela studi mahasiswa.");
			buttonTutup.addEventListener("onClick", new EventListener() {
				/**
				 * Tombol "Tutup" pada toolbar dasbor (hanya dibuat bila {@code window} tidak {@code null}, yaitu
				 * saat dasbor dipakai di dalam jendela Studi Mahasiswa): memanggil ulang {@code dataLoader} agar
				 * grid pemanggil me-refresh datanya, lalu melepas jendela.
				 */
				@Override
				public void onEvent(Event event) throws Exception {
					if (dataLoader != null) {
						dataLoader.loadData(event);
					}
					window.detach();
				}
			});
			buttonTutup.setParent(toolbar);
		}
	}


	/**
	 * Export data dasbor studi mahasiswa ke berkas Excel (.xlsx) berisi 3 sheet:
	 * <b>Ringkasan</b> (identitas + total MK/SKS), <b>Riwayat Mata Kuliah</b> (seluruh pengambilan MK
	 * dengan nilai/status), dan <b>MK Belum Diambil</b> (perbandingan kurikulum). Memakai POI langsung
	 * dan Filedownload agar berkas langsung terunduh oleh pengguna.
	 */
	private static void exportStudiMahasiswaExcel(Mahasiswa mahasiswa, List<Map<String, Object>> detailData) {
		try {
			org.zkoss.poi.xssf.usermodel.XSSFWorkbook wb = new org.zkoss.poi.xssf.usermodel.XSSFWorkbook();

			// Sheet 1: Ringkasan
			org.zkoss.poi.xssf.usermodel.XSSFSheet s0 = wb.createSheet("Ringkasan");
			int totMk = detailData == null ? 0 : detailData.size();
			int totSks = 0;
			if (detailData != null) {
				for (Map<String, Object> m : detailData) {
					totSks += xlsParseInt(xlsMapStr(m, "sks"));
				}
			}
			int rr0 = 0;
			rr0 = xlsRingkas(s0, rr0, "NIM", mahasiswa == null ? "" : xlsNz(mahasiswa.getNim()));
			rr0 = xlsRingkas(s0, rr0, "Nama", mahasiswa == null ? "" : xlsNz(mahasiswa.getNama()));
			rr0 = xlsRingkas(s0, rr0, "Program Studi",
					mahasiswa == null || mahasiswa.getJurusan() == null ? "" : xlsNz(mahasiswa.getJurusan().getNama()));
			rr0 = xlsRingkas(s0, rr0, "Total Mata Kuliah (tercatat)", String.valueOf(totMk));
			xlsRingkas(s0, rr0, "Total SKS (tercatat)", String.valueOf(totSks));

			// Sheet 2: Riwayat Mata Kuliah
			org.zkoss.poi.xssf.usermodel.XSSFSheet s1 = wb.createSheet("Riwayat Mata Kuliah");
			xlsHeader(s1, new String[] { "No", "Kode", "Mata Kuliah", "SKS", "Nilai Huruf", "Nilai Angka", "Semester",
					"IPS", "IPK", "Valid", "Lulus", "Dosen PA", "Catatan KRS", "Catatan KHS" });
			int r1 = 1, no1 = 0;
			if (detailData != null) {
				for (Map<String, Object> m : detailData) {
					no1++;
					org.zkoss.poi.xssf.usermodel.XSSFRow row = s1.createRow(r1++);
					int c = 0;
					xlsSel(row, c++, String.valueOf(no1));
					xlsSel(row, c++, xlsMapStr(m, "kode"));
					xlsSel(row, c++, xlsMapStr(m, "nama"));
					xlsSel(row, c++, xlsMapStr(m, "sks"));
					xlsSel(row, c++, xlsMapStr(m, "huruf"));
					xlsSel(row, c++, xlsMapStr(m, "nilai_angka"));
					xlsSel(row, c++, xlsMapStr(m, "smt"));
					xlsSel(row, c++, xlsMapStr(m, "ips"));
					xlsSel(row, c++, xlsMapStr(m, "ipk"));
					xlsSel(row, c++, xlsBool(m, "valid", "Valid", "Tidak Valid"));
					xlsSel(row, c++, xlsBool(m, "lulus", "Lulus", "Belum"));
					xlsSel(row, c++, xlsMapStr(m, "namaDosenPembimbing"));
					xlsSel(row, c++, xlsMapStr(m, "catatanKrs"));
					xlsSel(row, c++, xlsMapStr(m, "catatanKhs"));
				}
			}

			// Sheet 3: MK Belum Diambil (semua jenis, kurikulum otomatis terbaru)
			MkbdData d = dataMkBelumDiambil(mahasiswa, detailData, null, null);
			org.zkoss.poi.xssf.usermodel.XSSFSheet s2 = wb.createSheet("MK Belum Diambil");
			if (d.pesan != null) {
				s2.createRow(0).createCell(0).setCellValue(d.pesan);
			} else {
				xlsHeader(s2, new String[] { "No", "Kode", "Mata Kuliah", "SKS", "Semester", "Jenis" });
				int r2 = 1;
				for (String[] rw : d.rows) {
					org.zkoss.poi.xssf.usermodel.XSSFRow row = s2.createRow(r2++);
					for (int i = 0; i < rw.length; i++) {
						xlsSel(row, i, rw[i]);
					}
				}
			}

			// Sheet 4: Rekap KRS per Semester
			xlsSheetTabel(wb, "Rekap KRS per Semester",
					new String[] { "Semester", "Jenis", "SKS Diambil", "MK Disetujui", "MK Belum", "IPS", "IPK",
							"Tahun Akademik" },
					dataKrsPerSemester(mahasiswa));

			// Sheet 5: Rekap Kehadiran
			int[] kh = dataKehadiran(mahasiswa);
			org.zkoss.poi.xssf.usermodel.XSSFSheet s5 = wb.createSheet("Rekap Kehadiran");
			xlsHeader(s5, new String[] { "Kategori", "Jumlah" });
			int rr5 = 1;
			String[] khLabel = { "Hadir", "Izin", "Sakit", "Alpa", "Total Pertemuan" };
			for (int i = 0; i < khLabel.length; i++) {
				org.zkoss.poi.xssf.usermodel.XSSFRow row = s5.createRow(rr5++);
				xlsSel(row, 0, khLabel[i]);
				xlsSel(row, 1, String.valueOf(kh[i]));
			}

			// Sheet 6: Distribusi Nilai Huruf
			org.zkoss.poi.xssf.usermodel.XSSFSheet s6 = wb.createSheet("Distribusi Nilai");
			xlsHeader(s6, new String[] { "Nilai Huruf", "Jumlah MK" });
			java.util.LinkedHashMap<String, Integer> distNilai = new java.util.LinkedHashMap<String, Integer>();
			if (detailData != null) {
				for (Map<String, Object> m : detailData) {
					String h = xlsMapStr(m, "huruf").trim().toUpperCase();
					if (h.isEmpty() || h.equals("-")) {
						continue;
					}
					distNilai.put(h, Integer.valueOf((distNilai.get(h) == null ? 0 : distNilai.get(h).intValue()) + 1));
				}
			}
			int rr6 = 1;
			for (Map.Entry<String, Integer> e : distNilai.entrySet()) {
				org.zkoss.poi.xssf.usermodel.XSSFRow row = s6.createRow(rr6++);
				xlsSel(row, 0, e.getKey());
				xlsSel(row, 1, String.valueOf(e.getValue()));
			}

			// Sheet 7: Skripsi / Tugas Akhir
			xlsSheetTabel(wb, "Skripsi",
					new String[] { "Judul", "Tahun Akademik", "Pembimbing", "Nilai", "Huruf", "Status", "Tgl Sidang" },
					dataSkripsi(mahasiswa));

			// Sheet 8: Pengajuan Judul TA
			xlsSheetTabel(wb, "Pengajuan Judul TA",
					new String[] { "Judul", "Status", "Tahun Akademik", "Dosen", "Keterangan" },
					dataPengajuanJudulTA(mahasiswa));

			java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
			wb.write(bout);
			bout.close();
			String nim = mahasiswa != null && mahasiswa.getNim() != null && !mahasiswa.getNim().trim().isEmpty()
					? mahasiswa.getNim().trim() : "mahasiswa";
			org.zkoss.zul.Filedownload.save(bout.toByteArray(),
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
					"Studi_Mahasiswa_" + nim + ".xlsx");
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
			try {
				PesanFormalHelper.tampilkanGagalException("ekspor data Studi Mahasiswa ke Excel",
						e,
						new String[] {
								"Pastikan data mahasiswa yang akan diekspor (Nilai, KRS, Pengajuan Judul TA, dsb.) sudah lengkap dan tidak sedang diubah oleh proses lain.",
								"Coba ulangi proses ekspor beberapa saat lagi.",
								"Bila data mahasiswa sangat banyak, coba persempit cakupan data yang diekspor.",
								"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
						});
			} catch (Exception ignore) {
			}
		}
	}

	/** Isi satu sel Excel (kolom {@code col}) pada {@code row} dengan {@code val} (null -> string kosong). */
	private static void xlsSel(org.zkoss.poi.xssf.usermodel.XSSFRow row, int col, String val) {
		row.createCell(col).setCellValue(val == null ? "" : val);
	}

	/** Tulis baris header (baris 0) sheet Excel dari daftar nama kolom. */
	private static void xlsHeader(org.zkoss.poi.xssf.usermodel.XSSFSheet sheet, String[] cols) {
		org.zkoss.poi.xssf.usermodel.XSSFRow row = sheet.createRow(0);
		for (int i = 0; i < cols.length; i++) {
			row.createCell(i).setCellValue(cols[i]);
		}
	}

	/** Buat satu sheet berisi header + baris data (List&lt;String[]&gt;). Aman bila data kosong. */
	private static void xlsSheetTabel(org.zkoss.poi.xssf.usermodel.XSSFWorkbook wb, String namaSheet, String[] header,
			List<String[]> rows) {
		org.zkoss.poi.xssf.usermodel.XSSFSheet sheet = wb.createSheet(namaSheet);
		xlsHeader(sheet, header);
		int r = 1;
		if (rows != null) {
			for (String[] rw : rows) {
				org.zkoss.poi.xssf.usermodel.XSSFRow row = sheet.createRow(r++);
				for (int i = 0; i < rw.length; i++) {
					xlsSel(row, i, rw[i]);
				}
			}
		}
	}

	/** Tulis satu baris label:nilai (dua kolom) pada sheet "Ringkasan" di baris {@code r}; mengembalikan baris berikutnya. */
	private static int xlsRingkas(org.zkoss.poi.xssf.usermodel.XSSFSheet sheet, int r, String label, String val) {
		org.zkoss.poi.xssf.usermodel.XSSFRow row = sheet.createRow(r);
		row.createCell(0).setCellValue(label);
		row.createCell(1).setCellValue(val == null ? "" : val);
		return r + 1;
	}

	/** Ambil {@code m.get(key)} sebagai String (null-safe, memakai {@code toString()}), atau string kosong bila tak ada. */
	private static String xlsMapStr(Map<String, Object> m, String key) {
		if (m == null) {
			return "";
		}
		Object v = m.get(key);
		return v == null ? "" : v.toString();
	}

	/** Null -> string kosong (tanpa trim), selain itu nilai apa adanya. */
	private static String xlsNz(String s) {
		return s == null ? "" : s;
	}

	/**
	 * Terjemahkan nilai boolean-like pada {@code m.get(key)} (Boolean literal, atau String
	 * "true"/"1"/"ya" vs "false"/"0"/"tidak") menjadi label {@code t} (true) atau {@code f} (false)
	 * untuk sel Excel; nilai lain dikembalikan apa adanya.
	 */
	private static String xlsBool(Map<String, Object> m, String key, String t, String f) {
		if (m == null) {
			return "";
		}
		Object v = m.get(key);
		if (v == null) {
			return "";
		}
		if (v instanceof Boolean) {
			return ((Boolean) v).booleanValue() ? t : f;
		}
		String s = v.toString().trim();
		if (s.equalsIgnoreCase("true") || s.equals("1") || s.equalsIgnoreCase("ya")) {
			return t;
		}
		if (s.equalsIgnoreCase("false") || s.equals("0") || s.equalsIgnoreCase("tidak")) {
			return f;
		}
		return s;
	}

	/** Parse String ke int dengan toleransi koma desimal (locale ID); 0 bila kosong/gagal. */
	private static int xlsParseInt(String s) {
		try {
			return s == null || s.trim().isEmpty() ? 0 : (int) Double.parseDouble(s.trim().replace(",", "."));
		} catch (Exception e) {
			return 0;
		}
	}

	// ============================================================================================
	// PANEL TAMBAHAN: Distribusi Nilai, Rekap KRS per Semester, Rekap Kehadiran, Tugas Akhir.
	// Tiap method data membuka SESSION SENDIRI (dipanggil setelah session utama initDashboard ditutup).
	// ============================================================================================

	/** Sel header {@code <th>} berstyle untuk tabel panel Rekap KRS/Tugas Akhir. */
	private static String th(String s) {
		return "<th style='padding:6px 7px;border-bottom:1px solid #e2e8f0;text-align:left;'>" + escapeHtml(s) + "</th>";
	}

	/** Sel data {@code <td>} berstyle, opsional rata tengah. */
	private static String td(String s, boolean center) {
		return "<td style='padding:6px 7px;border-bottom:1px solid #f1f5f9;" + (center ? "text-align:center;" : "")
				+ "'>" + escapeHtml(s) + "</td>";
	}

	/** Sel data {@code <td>} rata tengah, tebal, dengan warna teks kustom (mis. angka SKS/MK). */
	private static String tdColor(String s, String color) {
		return "<td style='padding:6px 7px;border-bottom:1px solid #f1f5f9;text-align:center;font-weight:700;color:"
				+ color + ";'>" + escapeHtml(s) + "</td>";
	}

	/**
	 * Sel data berisi badge status berwarna: hijau untuk kata mengandung "setuju"/"lulus", merah
	 * untuk "tolak"/"gagal", biru untuk "sidang"/"seminar"/"proses"/"bimbingan", kuning untuk
	 * "pengajuan"/"menunggu", abu-abu untuk selain itu. Dipakai kolom Status pada tabel Pengajuan
	 * Judul TA dan Skripsi.
	 */
	private static String tdBadge(String s) {
		String bg = "#e2e8f0", fg = "#334155";
		String low = s == null ? "" : s.toLowerCase();
		if (low.contains("setuju") || low.contains("lulus")) {
			bg = "#dcfce7"; fg = "#166534";
		} else if (low.contains("tolak") || low.contains("gagal")) {
			bg = "#fee2e2"; fg = "#991b1b";
		} else if (low.contains("sidang") || low.contains("seminar") || low.contains("proses") || low.contains("bimbingan")) {
			bg = "#dbeafe"; fg = "#1e40af";
		} else if (low.contains("pengajuan") || low.contains("menunggu")) {
			bg = "#fef9c3"; fg = "#854d0e";
		}
		return "<td style='padding:6px 7px;border-bottom:1px solid #f1f5f9;text-align:center;'><span style='padding:2px 8px;"
				+ "border-radius:999px;font-size:11px;font-weight:700;background:" + bg + ";color:" + fg + ";'>"
				+ escapeHtml(s) + "</span></td>";
	}

	/** Trim string; null/kosong -> {@code "-"} (placeholder tampilan tabel). */
	private static String nzTrim(String s) {
		return s == null || s.trim().isEmpty() ? "-" : s.trim();
	}

	/** Nama {@link ais.database.model.Dosen}, aman terhadap null/lazy-load gagal -> {@code "-"}. */
	private static String dosenNama(ais.database.model.Dosen d) {
		try {
			return d == null || d.getNama() == null || d.getNama().trim().isEmpty() ? "-" : d.getNama().trim();
		} catch (Exception e) {
			return "-";
		}
	}

	/** Format tanggal ke {@code dd-MM-yyyy}; null/error -> {@code "-"}. */
	private static String tglIndo(java.util.Date d) {
		if (d == null) {
			return "-";
		}
		try {
			return new java.text.SimpleDateFormat("dd-MM-yyyy").format(d);
		} catch (Exception e) {
			return "-";
		}
	}

	/**
	 * Ambil ID MK yang benar-benar dirangkum oleh satu header KRS. Jangan memakai jumlah SKS sebagai
	 * penanda keberadaan MK karena MK dengan bobot 0 SKS tetap sah.
	 */
	private static List<Long> idDetailPerkuliahanKrs(KrsMahasiswa krs) {
		List<Long> hasil = new ArrayList<Long>();
		if (krs == null || krs.getSksYangDiambilS() == null || krs.getSksYangDiambilS().trim().isEmpty()) {
			return hasil;
		}
		Set<Long> unik = new HashSet<Long>();
		for (String nilai : krs.getSksYangDiambilS().split(",")) {
			try {
				Long id = Long.valueOf(nilai.trim());
				if (id.longValue() > 0L && unik.add(id)) {
					hasil.add(id);
				}
			} catch (Exception e) {
				// Abaikan token lama/rusak; keberadaan MK tetap diverifikasi kembali ke Detailperkuliahan.
			}
		}
		return hasil;
	}

	/** Hasil: {jumlah MK valid, disetujui, belum disetujui}. */
	private static int[] ringkasanDetailPerkuliahanKrs(org.hibernate.Session session, Mahasiswa mahasiswa,
			KrsMahasiswa krs) {
		int[] hasil = new int[] { 0, 0, 0 };
		List<Long> ids = idDetailPerkuliahanKrs(krs);
		if (session == null || mahasiswa == null || ids.isEmpty()) {
			return hasil;
		}
		try {
			Criteria criteria = session.createCriteria(ais.database.model.Detailperkuliahan.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.in("id", ids));
			if (Integer.valueOf(0).equals(krs.getSemester())) {
				criteria.add(Restrictions.eq("semester", Integer.valueOf(0)))
						.add(Restrictions.isNotNull("matakuliahKonversi"));
			} else {
				criteria.add(Restrictions.or(Restrictions.isNotNull("perkuliahan"),
						Restrictions.isNotNull("matakuliahKonversi")));
			}
			@SuppressWarnings("unchecked")
			List<Integer> persetujuans = criteria
					.setProjection(org.hibernate.criterion.Projections.property("persetujuan")).list();
			for (Integer persetujuan : persetujuans) {
				hasil[0]++;
				if (ais.database.model.Detailperkuliahan.DISETUJUI.equals(persetujuan)) {
					hasil[1]++;
				} else if (ais.database.model.Detailperkuliahan.BELUM_DISETUJUI.equals(persetujuan)) {
					hasil[2]++;
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit TampilStudiMahasiswaHelper.ringkasanDetailPerkuliahanKrs");
		}
		return hasil;
	}

	/**
	 * Semester terakhir yang boleh masuk ringkasan akademik. Aturannya sama dengan
	 * pilihan default tab KRS: semester berjalan, dibatasi semester lulus bila ada.
	 */
	private static int semesterAkademikMaksimal(Mahasiswa mahasiswa) {
		if (mahasiswa == null) {
			return 0;
		}
		Integer semesterBerjalan = mahasiswa.currentSemester();
		Integer semesterLulus = mahasiswa.getSemesterLulus();
		int hasil = semesterBerjalan == null ? 0 : semesterBerjalan.intValue();
		if (semesterLulus != null && semesterLulus.intValue() > 0
				&& (hasil <= 0 || hasil > semesterLulus.intValue())) {
			hasil = semesterLulus.intValue();
		}
		return hasil > 0 ? hasil : 0;
	}

	/**
	 * Uji apakah satu nomor semester layak dihitung pada ringkasan akademik: tidak {@code null},
	 * tidak negatif, dan tidak melebihi {@code semesterMaksimal} (lihat
	 * {@link #semesterAkademikMaksimal(Mahasiswa)}). Semester {@code 0} SENGAJA dianggap valid karena
	 * dipakai sebagai penanda mata kuliah KONVERSI. Bila {@code semesterMaksimal} negatif, seluruh
	 * semester dianggap tidak valid (fail-closed).
	 *
	 * @param semester nomor semester dari {@link Detailperkuliahan}/KRS; boleh {@code null}.
	 * @param semesterMaksimal batas atas hasil {@link #semesterAkademikMaksimal(Mahasiswa)}.
	 * @return {@code true} bila semester boleh ikut dihitung.
	 */
	private static boolean semesterAkademikValid(Integer semester, int semesterMaksimal) {
		return semester != null && semester.intValue() >= 0 && semesterMaksimal >= 0
				&& semester.intValue() <= semesterMaksimal;
	}

	/** Ambil pasangan semester-TA memakai rumus yang sama dengan {@link KrsMahasiswa#getTahunAkademik()}. */
	private static Map<Integer, String> tahunAkademikPerSemesterKrs(Mahasiswa mahasiswa,
			int semesterMaksimal) {
		Map<Integer, String> hasil = new HashMap<Integer, String>();
		if (mahasiswa == null || semesterMaksimal <= 0 || mahasiswa.getTahunangkatan() == null
				|| mahasiswa.getSemesterMulai() == null) {
			return hasil;
		}
		for (int semester = 1; semester <= semesterMaksimal; semester++) {
			Integer tahun = Common.getTahunAkademik(Integer.valueOf(semester), mahasiswa.getTahunangkatan(),
					mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
			if (tahun != null && tahun.intValue() > 0) {
				hasil.put(Integer.valueOf(semester), tahun + "/" + (tahun.intValue() + 1));
			}
		}
		return hasil;
	}

	/** Data KRS per semester: {semester, jenis, sksDiambil, mkDisetujui, mkBelum, ips, ipk, tahunAkademik}. */
	private static List<String[]> dataKrsPerSemester(Mahasiswa mahasiswa) {
		List<String[]> res = new ArrayList<String[]>();
		if (mahasiswa == null) {
			return res;
		}
		int semesterMaksimal = semesterAkademikMaksimal(mahasiswa);
		if (semesterMaksimal < 0) {
			return res;
		}
		Map<Integer, String> tahunAkademikKrs = tahunAkademikPerSemesterKrs(mahasiswa, semesterMaksimal);
		org.hibernate.Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			@SuppressWarnings("unchecked")
			List<ais.database.model.KrsMahasiswa> list = session.createCriteria(ais.database.model.KrsMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.ge("semester", Integer.valueOf(0)))
					.add(Restrictions.le("semester", Integer.valueOf(semesterMaksimal)))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
					.addOrder(Order.asc("semester")).addOrder(Order.asc("id")).list();
			for (ais.database.model.KrsMahasiswa krs : list) {
				try {
					Integer sem = krs.getSemester();
					if (!semesterAkademikValid(sem, semesterMaksimal)) {
						continue;
					}
					int[] ringkasanMk = ringkasanDetailPerkuliahanKrs(session, mahasiswa, krs);
					if (ringkasanMk[0] == 0) {
						continue;
					}
					res.add(new String[] { sem == null ? "-" : String.valueOf(sem),
							krs.getSemesterPendek() != null ? "Semester Pendek" : "Reguler",
							krs.getSksYangDiambil() == null ? "0" : String.valueOf(krs.getSksYangDiambil()),
							String.valueOf(ringkasanMk[1]), String.valueOf(ringkasanMk[2]),
							krs.getIps() == null ? "-" : formatNumber(krs.getIps().doubleValue()),
							krs.getIpk() == null ? "-" : formatNumber(krs.getIpk().doubleValue()),
							tahunAkademikKrs.containsKey(sem) ? nzTrim(tahunAkademikKrs.get(sem))
									: nzTrim(krs.getTahunAkademik()) });
				} catch (Exception ex) {
					ais.common.ErrorAuditUtil.record(ex, "auto-audit(row) TampilStudiMahasiswaHelper.dataKrsPerSemester");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit TampilStudiMahasiswaHelper.dataKrsPerSemester");
		} finally {
			closeOpenedSession(session);
		}
		return res;
	}

	/** Panel HTML "Rekap KRS per Semester": grafik batang SKS diambil per semester + tabel {@link #dataKrsPerSemester}. */
	private static String buildKrsPerSemesterHtml(Mahasiswa mahasiswa) {
		List<String[]> rows = dataKrsPerSemester(mahasiswa);
		if (rows.isEmpty()) {
			return "<div style='padding:12px;color:#64748b;'>Belum ada data KRS yang tercatat untuk mahasiswa ini.</div>";
		}
		String[] labels = new String[rows.size()];
		double[] vals = new double[rows.size()];
		for (int i = 0; i < rows.size(); i++) {
			labels[i] = "Smt " + rows.get(i)[0] + (rows.get(i)[1].startsWith("Semester") ? " (SP)" : "");
			vals[i] = xlsParseInt(rows.get(i)[2]);
		}
		StringBuilder html = new StringBuilder();
		html.append(ais.ui.util.HtmlChartHelper.barHorizontal("SKS Diambil per Semester",
				"Jumlah SKS yang diambil mahasiswa pada tiap semester (termasuk semester pendek bila ada).",
				labels, vals, "#2563eb"));
		html.append("<div style='overflow-x:auto;margin-top:8px;'><table style='width:100%;border-collapse:collapse;font-size:12px;'>");
		html.append("<thead><tr style='background:#f8fafc;color:#334155;'>").append(th("Smt")).append(th("Jenis"))
				.append(th("SKS")).append(th("MK Disetujui")).append(th("MK Belum")).append(th("IPS")).append(th("IPK"))
				.append(th("TA")).append("</tr></thead><tbody>");
		for (String[] r : rows) {
			html.append("<tr>").append(td(r[0], true)).append(td(r[1], false)).append(td(r[2], true))
					.append(tdColor(r[3], "#16a34a")).append(tdColor(r[4], "#ea580c")).append(td(r[5], true))
					.append(td(r[6], true)).append(td(r[7], false)).append("</tr>");
		}
		html.append("</tbody></table></div>");
		return html.toString();
	}

	/** Rekap kehadiran: {hadir, izin, sakit, alpa, total}. */
	private static int[] dataKehadiran(Mahasiswa mahasiswa) {
		int[] r = new int[] { 0, 0, 0, 0, 0 };
		if (mahasiswa == null) {
			return r;
		}
		org.hibernate.Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			@SuppressWarnings("unchecked")
			List<Long> pkIds = session.createCriteria(ais.database.model.Detailperkuliahan.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.isNull("ikutiPerkuliahan"))
					.createAlias("perkuliahan", "pk")
					.setProjection(org.hibernate.criterion.Projections.distinct(
							org.hibernate.criterion.Projections.property("pk.id")))
					.list();
			if (pkIds != null && !pkIds.isEmpty()) {
				@SuppressWarnings("unchecked")
				List<String> abs = session.createCriteria(ais.database.model.Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
						.createAlias("perkuliahan", "pk").add(Restrictions.in("pk.id", pkIds))
						.add(Restrictions.isNotNull("absensi"))
						.setProjection(org.hibernate.criterion.Projections.property("absensi")).list();
				Map<String, Integer> c = ais.database.model.Perkuliahan.hitungStatus(abs, mahasiswa.getId());
				r[0] = c.get("M") == null ? 0 : c.get("M").intValue();
				r[1] = c.get("I") == null ? 0 : c.get("I").intValue();
				r[2] = c.get("S") == null ? 0 : c.get("S").intValue();
				r[3] = c.get("A") == null ? 0 : c.get("A").intValue();
				r[4] = c.get("T") == null ? 0 : c.get("T").intValue();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit TampilStudiMahasiswaHelper.dataKehadiran");
		} finally {
			closeOpenedSession(session);
		}
		return r;
	}

	/** Panel HTML "Rekap Kehadiran": donat Hadir/Izin/Sakit/Alpa dari {@link #dataKehadiran} + ringkasan angka. */
	private static String buildKehadiranHtml(Mahasiswa mahasiswa) {
		int[] k = dataKehadiran(mahasiswa);
		int total = k[4] > 0 ? k[4] : (k[0] + k[1] + k[2] + k[3]);
		if (total == 0) {
			return "<div style='padding:12px;color:#64748b;'>Belum ada rekaman kehadiran pada perkuliahan mahasiswa ini.</div>";
		}
		double persenHadir = k[0] * 100.0 / total;
		StringBuilder html = new StringBuilder();
		html.append(ais.ui.util.HtmlChartHelper.donut("Rekap Kehadiran Perkuliahan",
				"Proporsi kehadiran mahasiswa di seluruh pertemuan perkuliahan yang tercatat: Hadir, Izin, Sakit, dan Alpa.",
				new String[] { "Hadir", "Izin", "Sakit", "Alpa" }, new double[] { k[0], k[1], k[2], k[3] },
				new String[] { "#16a34a", "#0ea5e9", "#f59e0b", "#dc2626" }, formatNumber(persenHadir) + "%"));
		html.append("<div style='margin-top:8px;font-size:12px;color:#334155;'>Total pertemuan tercatat: <b>")
				.append(total).append("</b> — Hadir <b style='color:#16a34a'>").append(k[0]).append("</b>, Izin <b>")
				.append(k[1]).append("</b>, Sakit <b>").append(k[2]).append("</b>, Alpa <b style='color:#dc2626'>")
				.append(k[3]).append("</b>.</div>");
		return html.toString();
	}

	/** Sebaran nilai huruf (donut) dari detailData yang sudah dimuat — tanpa query tambahan. */
	private static String buildDistribusiNilaiHtml(List<Map<String, Object>> detailData) {
		String[] urut = { "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D", "E" };
		java.util.LinkedHashMap<String, Integer> peta = new java.util.LinkedHashMap<String, Integer>();
		for (String u : urut) {
			peta.put(u, Integer.valueOf(0));
		}
		if (detailData != null) {
			for (Map<String, Object> m : detailData) {
				String h = xlsMapStr(m, "huruf").trim().toUpperCase();
				if (h.isEmpty() || h.equals("-")) {
					continue;
				}
				peta.put(h, Integer.valueOf((peta.get(h) == null ? 0 : peta.get(h).intValue()) + 1));
			}
		}
		List<String> labels = new ArrayList<String>();
		List<Double> vals = new ArrayList<Double>();
		List<String> colors = new ArrayList<String>();
		String[] palet = { "#16a34a", "#22c55e", "#4ade80", "#84cc16", "#a3e635", "#eab308", "#f59e0b", "#f97316",
				"#ef4444", "#b91c1c" };
		int idx = 0, tot = 0;
		for (Map.Entry<String, Integer> e : peta.entrySet()) {
			int v = e.getValue() == null ? 0 : e.getValue().intValue();
			if (v > 0) {
				labels.add(e.getKey());
				vals.add(Double.valueOf(v));
				colors.add(palet[Math.min(idx, palet.length - 1)]);
				tot += v;
			}
			idx++;
		}
		if (labels.isEmpty()) {
			return "<div style='padding:12px;color:#64748b;'>Belum ada nilai huruf yang tercatat.</div>";
		}
		double[] varr = new double[vals.size()];
		for (int i = 0; i < varr.length; i++) {
			varr[i] = vals.get(i).doubleValue();
		}
		return ais.ui.util.HtmlChartHelper.donut("Distribusi Nilai Huruf",
				"Sebaran nilai huruf dari seluruh mata kuliah yang tercatat. Membantu melihat proporsi nilai baik dibanding yang perlu perhatian.",
				labels.toArray(new String[labels.size()]), varr, colors.toArray(new String[colors.size()]), tot + " MK");
	}

	/** Data skripsi: {judul, tahunAkademik, pembimbing, totalNilai, nilaiHuruf, status, tglSidang}. */
	private static List<String[]> dataSkripsi(Mahasiswa mahasiswa) {
		List<String[]> res = new ArrayList<String[]>();
		if (mahasiswa == null) {
			return res;
		}
		org.hibernate.Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			@SuppressWarnings("unchecked")
			List<ais.database.model.Skripsi> list = session.createCriteria(ais.database.model.Skripsi.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.desc("id")).list();
			for (ais.database.model.Skripsi s : list) {
				try {
					String status;
					if (s.getTelahSidang() != null && s.getTelahSidang().intValue() == 1) {
						status = Boolean.TRUE.equals(s.getLulus()) ? "Lulus Sidang" : "Sudah Sidang";
					} else if (s.getJadwalSidangTugasAkhir() != null) {
						status = "Terjadwal Sidang";
					} else {
						status = "Proses Bimbingan";
					}
					res.add(new String[] { nzTrim(s.getJudul()), nzTrim(s.getTahunAkademik()),
							dosenNama(s.getPembimbing()),
							s.getTotalNilai() == null ? "-" : formatNumber(s.getTotalNilai().doubleValue()),
							nzTrim(s.getNilaiHuruf()), status, tglIndo(s.getTanggalSidang()) });
				} catch (Exception ex) {
					ais.common.ErrorAuditUtil.record(ex, "auto-audit(row) TampilStudiMahasiswaHelper.dataSkripsi");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit TampilStudiMahasiswaHelper.dataSkripsi");
		} finally {
			closeOpenedSession(session);
		}
		return res;
	}

	/** Data pengajuan judul TA: {judul, status, tahunAkademik, dosen, keterangan}. */
	private static List<String[]> dataPengajuanJudulTA(Mahasiswa mahasiswa) {
		List<String[]> res = new ArrayList<String[]>();
		if (mahasiswa == null) {
			return res;
		}
		org.hibernate.Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			@SuppressWarnings("unchecked")
			List<ais.database.model.MahasiswaRequestTugasAkhir> list = session
					.createCriteria(ais.database.model.MahasiswaRequestTugasAkhir.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.desc("id")).list();
			for (ais.database.model.MahasiswaRequestTugasAkhir r : list) {
				try {
					res.add(new String[] { nzTrim(r.getJudul()), nzTrim(r.getStatus()), nzTrim(r.getTahunAkademik()),
							dosenNama(r.getDosen1()), nzTrim(r.getKeterangan()) });
				} catch (Exception ex) {
					ais.common.ErrorAuditUtil.record(ex, "auto-audit(row) TampilStudiMahasiswaHelper.dataPengajuanJudulTA");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit TampilStudiMahasiswaHelper.dataPengajuanJudulTA");
		} finally {
			closeOpenedSession(session);
		}
		return res;
	}

	/**
	 * Panel HTML "Tugas Akhir &amp; Pengajuan Judul": dua tabel berurutan &mdash; riwayat pengajuan
	 * judul ({@link #dataPengajuanJudulTA}, status sebagai badge) dan riwayat skripsi
	 * ({@link #dataSkripsi}, status turunan dari {@code telahSidang}/{@code jadwalSidangTugasAkhir}/
	 * {@code lulus}). Pesan kosong bila keduanya tidak ada data.
	 */
	private static String buildTugasAkhirHtml(Mahasiswa mahasiswa) {
		List<String[]> skr = dataSkripsi(mahasiswa);
		List<String[]> req = dataPengajuanJudulTA(mahasiswa);
		if (skr.isEmpty() && req.isEmpty()) {
			return "<div style='padding:12px;color:#64748b;'>Belum ada data tugas akhir / pengajuan judul untuk mahasiswa ini.</div>";
		}
		StringBuilder html = new StringBuilder();
		html.append("<div style='font-weight:800;color:#0f172a;font-size:13px;margin:2px 0 6px;'>Pengajuan Judul Tugas Akhir</div>");
		if (req.isEmpty()) {
			html.append("<div style='padding:8px;color:#64748b;'>Belum ada pengajuan judul.</div>");
		} else {
			html.append("<div style='overflow-x:auto;'><table style='width:100%;border-collapse:collapse;font-size:12px;'>")
					.append("<thead><tr style='background:#f8fafc;color:#334155;'>").append(th("Judul")).append(th("Status"))
					.append(th("TA")).append(th("Dosen")).append(th("Keterangan")).append("</tr></thead><tbody>");
			for (String[] r : req) {
				html.append("<tr>").append(td(r[0], false)).append(tdBadge(r[1])).append(td(r[2], true))
						.append(td(r[3], false)).append(td(r[4], false)).append("</tr>");
			}
			html.append("</tbody></table></div>");
		}
		html.append("<div style='font-weight:800;color:#0f172a;font-size:13px;margin:12px 0 6px;'>Skripsi / Tugas Akhir</div>");
		if (skr.isEmpty()) {
			html.append("<div style='padding:8px;color:#64748b;'>Belum ada data skripsi.</div>");
		} else {
			html.append("<div style='overflow-x:auto;'><table style='width:100%;border-collapse:collapse;font-size:12px;'>")
					.append("<thead><tr style='background:#f8fafc;color:#334155;'>").append(th("Judul")).append(th("TA"))
					.append(th("Pembimbing")).append(th("Nilai")).append(th("Huruf")).append(th("Status"))
					.append(th("Tgl Sidang")).append("</tr></thead><tbody>");
			for (String[] r : skr) {
				html.append("<tr>").append(td(r[0], false)).append(td(r[1], true)).append(td(r[2], false))
						.append(td(r[3], true)).append(tdColor(r[4], "#16a34a")).append(tdBadge(r[5]))
						.append(td(r[6], true)).append("</tr>");
			}
			html.append("</tbody></table></div>");
		}
		return html.toString();
	}


	/** Adaptasi {@code List<String[]>} menjadi {@code List<Object[]>} (setiap {@code String[]} dititipkan apa adanya sebagai {@code Object[]}) agar cocok dengan API tabel {@code DashboardReportKit}. */
	private static List<Object[]> keObjectList(List<String[]> rows) {
		List<Object[]> out = new ArrayList<Object[]>();
		if (rows != null) {
			for (String[] r : rows) {
				out.add(r);
			}
		}
		return out;
	}

	/** Parse String ke double dengan toleransi koma desimal dan placeholder {@code "-"}; 0 bila kosong/gagal. */
	private static double parseDoubleAman(String s) {
		try {
			return s == null || s.trim().isEmpty() || s.trim().equals("-") ? 0
					: Double.parseDouble(s.trim().replace(",", "."));
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * Bangun {@link ais.action.master.helper.DashboardReportKit.SumberLaporan} untuk laporan studi
	 * mahasiswa: KPI ringkasan, donut Distribusi Nilai dan Kehadiran, batang SKS per Semester, serta
	 * tabel KRS / Skripsi / Pengajuan Judul / Riwayat MK / MK Belum Diambil. Dipakai tombol "Cetak
	 * Laporan (Grafik dan Tabel)" - mesin kit merender grafik HTML/CSS + tabel menjadi laporan yang
	 * siap dicetak/disimpan PDF (pola sama dengan log_login.zul). Data diambil malas (saat diklik).
	 */
	private static ais.action.master.helper.DashboardReportKit.SumberLaporan buildSumberLaporanStudi(
			final Mahasiswa mahasiswa, final List<Map<String, Object>> detailData) {
		final String nim = mahasiswa == null ? "-" : nzTrim(mahasiswa.getNim());
		final String nama = mahasiswa == null ? "-" : nzTrim(mahasiswa.getNama());
		final String prodi = mahasiswa == null || mahasiswa.getJurusan() == null ? "-"
				: nzTrim(mahasiswa.getJurusan().getNama());
		return new ais.action.master.helper.DashboardReportKit.SumberLaporan() {
			/** Judul tetap laporan cetak: "Dasbor Studi Mahasiswa". */
			public String judul() {
				return "Dasbor Studi Mahasiswa";
			}

			/** Subjudul laporan: {@code NIM - Nama} mahasiswa (nilai sudah di-{@code nzTrim}, {@code "-"} bila kosong). */
			public String subjudul() {
				return nim + " - " + nama;
			}

			/** Paragraf pengantar laporan, memuat nama program studi mahasiswa. */
			public String deskripsi() {
				return "Ringkasan riwayat pembelajaran mahasiswa: nilai, KRS, kehadiran, dan tugas akhir. Program Studi: "
						+ prodi + ".";
			}

			/**
			 * Susunan bagian laporan, berurutan: KPI Ringkasan Akademik; donut Distribusi Nilai Huruf, Rekap
			 * Kehadiran dan Komposisi Kelulusan MK; batang SKS per Semester; garis Tren IPK dan Tren IPS;
			 * lalu tabel Rekap KRS per Semester, Skripsi/TA, Pengajuan Judul TA, Riwayat Pengambilan MK, dan
			 * MK Belum Diambil.
			 *
			 * <p><b>Hal non-obvious:</b> setiap {@code PenyediaBaris} di bawah baru dieksekusi saat
			 * {@code DashboardReportKit} merender bagiannya (pengambilan data MALAS). Bagian yang bersumber
			 * dari {@code detailData} memakai data yang sudah dimuat dasbor, tetapi bagian yang memanggil
			 * {@code dataKrsPerSemester} / {@code dataKehadiran} / {@code dataSkripsi} /
			 * {@code dataPengajuanJudulTA} / {@code dataMkBelumDiambil} MEMBUKA SESSION HIBERNATE BARU setiap
			 * kali dipanggil &mdash; dan {@code dataKrsPerSemester} dipanggil EMPAT kali (batang, dua garis,
			 * satu tabel), sehingga satu kali cetak laporan menjalankan query KRS berulang. Ini konsekuensi
			 * dari pola "tiap penyedia berdiri sendiri" yang dipakai kit laporan.
			 */
			public List<ais.action.master.helper.DashboardReportKit.Bagian> bagian() {
				List<ais.action.master.helper.DashboardReportKit.Bagian> list = new ArrayList<ais.action.master.helper.DashboardReportKit.Bagian>();

				list.add(ais.action.master.helper.DashboardReportKit.kpi("Ringkasan Akademik",
						"Angka-angka penting studi mahasiswa.",
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							/**
							 * KPI ringkasan: total mata kuliah, total SKS tercatat, jumlah MK lulus dan belum lulus &mdash;
							 * seluruhnya dihitung dari {@code detailData} yang sudah dimuat (tanpa query tambahan).
							 */
							public List<Object[]> ambil() {
								int totMk = detailData == null ? 0 : detailData.size();
								int totSks = 0, lulus = 0;
								if (detailData != null) {
									for (Map<String, Object> m : detailData) {
										totSks += xlsParseInt(xlsMapStr(m, "sks"));
										if ("Lulus".equals(xlsBool(m, "lulus", "Lulus", "Belum"))) {
											lulus++;
										}
									}
								}
								List<Object[]> r = new ArrayList<Object[]>();
								r.add(new Object[] { "Total Mata Kuliah", Integer.valueOf(totMk) });
								r.add(new Object[] { "Total SKS Tercatat", Integer.valueOf(totSks) });
								r.add(new Object[] { "MK Lulus", Integer.valueOf(lulus) });
								r.add(new Object[] { "MK Belum Lulus", Integer.valueOf(totMk - lulus) });
								return r;
							}
						}));

				list.add(ais.action.master.helper.DashboardReportKit.donut("Distribusi Nilai Huruf",
						"Sebaran nilai huruf dari seluruh mata kuliah yang tercatat.",
						new String[] { "Huruf", "Jumlah" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							/**
							 * Baris donut Distribusi Nilai Huruf: menghitung frekuensi nilai huruf dari {@code detailData}
							 * (di-trim dan di-UPPERCASE); entri kosong atau {@code "-"} dilewati. {@code LinkedHashMap}
							 * dipakai agar urutan huruf mengikuti urutan kemunculan pertama pada data.
							 */
							public List<Object[]> ambil() {
								java.util.LinkedHashMap<String, Integer> peta = new java.util.LinkedHashMap<String, Integer>();
								if (detailData != null) {
									for (Map<String, Object> m : detailData) {
										String h = xlsMapStr(m, "huruf").trim().toUpperCase();
										if (h.isEmpty() || h.equals("-")) {
											continue;
										}
										peta.put(h, Integer.valueOf((peta.get(h) == null ? 0 : peta.get(h).intValue()) + 1));
									}
								}
								List<Object[]> r = new ArrayList<Object[]>();
								for (Map.Entry<String, Integer> e : peta.entrySet()) {
									r.add(new Object[] { e.getKey(), e.getValue() });
								}
								return r;
							}
						}));

				list.add(ais.action.master.helper.DashboardReportKit.donut("Rekap Kehadiran",
						"Proporsi Hadir / Izin / Sakit / Alpa pada perkuliahan mahasiswa.",
						new String[] { "Kategori", "Jumlah" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							/**
							 * Baris donut Rekap Kehadiran: Hadir/Izin/Sakit/Alpa dari {@link #dataKehadiran(Mahasiswa)}
							 * (elemen indeks 0-3; indeks 4 berisi total dan sengaja tidak ditampilkan). Membuka session
							 * Hibernate sendiri.
							 */
							public List<Object[]> ambil() {
								int[] k = dataKehadiran(mahasiswa);
								List<Object[]> r = new ArrayList<Object[]>();
								r.add(new Object[] { "Hadir", Integer.valueOf(k[0]) });
								r.add(new Object[] { "Izin", Integer.valueOf(k[1]) });
								r.add(new Object[] { "Sakit", Integer.valueOf(k[2]) });
								r.add(new Object[] { "Alpa", Integer.valueOf(k[3]) });
								return r;
							}
						}));

				list.add(ais.action.master.helper.DashboardReportKit.donut("Komposisi Kelulusan Mata Kuliah",
						"Perbandingan mata kuliah yang sudah lulus dibanding yang belum lulus.",
						new String[] { "Status", "Jumlah" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							/**
							 * Baris donut Komposisi Kelulusan MK: mencacah {@code detailData} menjadi dua kelompok Lulus dan
							 * Belum Lulus berdasarkan kunci {@code "lulus"}.
							 */
							public List<Object[]> ambil() {
								int lulus = 0, belum = 0;
								if (detailData != null) {
									for (Map<String, Object> m : detailData) {
										if ("Lulus".equals(xlsBool(m, "lulus", "Lulus", "Belum"))) {
											lulus++;
										} else {
											belum++;
										}
									}
								}
								List<Object[]> r = new ArrayList<Object[]>();
								r.add(new Object[] { "Lulus", Integer.valueOf(lulus) });
								r.add(new Object[] { "Belum Lulus", Integer.valueOf(belum) });
								return r;
							}
						}));

				list.add(ais.action.master.helper.DashboardReportKit.batang("SKS Diambil per Semester",
						"Jumlah SKS yang diambil tiap semester.", new String[] { "Semester", "SKS" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							/**
							 * Baris batang SKS Diambil per Semester dari {@link #dataKrsPerSemester(Mahasiswa)}; label diberi
							 * akhiran {@code " (SP)"} bila jenis semesternya bukan Reguler. Membuka session Hibernate sendiri.
							 */
							public List<Object[]> ambil() {
								List<Object[]> r = new ArrayList<Object[]>();
								for (String[] k : dataKrsPerSemester(mahasiswa)) {
									r.add(new Object[] { "Smt " + k[0] + ("Reguler".equals(k[1]) ? "" : " (SP)"),
											Integer.valueOf(xlsParseInt(k[2])) });
								}
								return r;
							}
						}));

				list.add(ais.action.master.helper.DashboardReportKit.garis("Tren IPK per Semester",
						"Perkembangan IPK dari semester ke semester (garis naik berarti membaik). Nilai diskalakan agar tren tampak jelas.",
						new String[] { "Semester", "IPK x100" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							/**
							 * Baris garis Tren IPK per semester. Nilai IPK (indeks 6 hasil
							 * {@link #dataKrsPerSemester(Mahasiswa)}) DIKALI 100 dan dibulatkan ke bilangan bulat karena
							 * grafik garis kit laporan hanya menerima nilai bulat &mdash; karena itu judul kolomnya
							 * "IPK x100". Membuka session Hibernate sendiri.
							 */
							public List<Object[]> ambil() {
								List<Object[]> r = new ArrayList<Object[]>();
								for (String[] k : dataKrsPerSemester(mahasiswa)) {
									r.add(new Object[] { "Smt " + k[0],
											Integer.valueOf((int) Math.round(parseDoubleAman(k[6]) * 100)) });
								}
								return r;
							}
						}));

				list.add(ais.action.master.helper.DashboardReportKit.garis("Tren IPS per Semester",
						"Perkembangan IPS (indeks prestasi semester) dari semester ke semester. Nilai diskalakan agar tren tampak jelas.",
						new String[] { "Semester", "IPS x100" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							/**
							 * Baris garis Tren IPS per semester; sama seperti tren IPK tetapi memakai IPS (indeks 5) dan juga
							 * diskalakan 100x. Membuka session Hibernate sendiri.
							 */
							public List<Object[]> ambil() {
								List<Object[]> r = new ArrayList<Object[]>();
								for (String[] k : dataKrsPerSemester(mahasiswa)) {
									r.add(new Object[] { "Smt " + k[0],
											Integer.valueOf((int) Math.round(parseDoubleAman(k[5]) * 100)) });
								}
								return r;
							}
						}));

				list.add(ais.action.master.helper.DashboardReportKit.tabel("Rekap KRS per Semester",
						"Detail KRS tiap semester (SKS, persetujuan, IPS, IPK).",
						new String[] { "Smt", "Jenis", "SKS", "MK Disetujui", "MK Belum", "IPS", "IPK", "TA" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							/**
							 * Tabel Rekap KRS per Semester: seluruh kolom {@link #dataKrsPerSemester(Mahasiswa)} diteruskan
							 * apa adanya lewat {@link #keObjectList(List)}. Membuka session Hibernate sendiri.
							 */
							public List<Object[]> ambil() {
								return keObjectList(dataKrsPerSemester(mahasiswa));
							}
						}));

				list.add(ais.action.master.helper.DashboardReportKit.tabel("Skripsi / Tugas Akhir",
						"Data skripsi mahasiswa beserta status sidang.",
						new String[] { "Judul", "TA", "Pembimbing", "Nilai", "Huruf", "Status", "Tgl Sidang" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							/** Tabel Skripsi/Tugas Akhir dari {@link #dataSkripsi(Mahasiswa)}. Membuka session Hibernate sendiri. */
							public List<Object[]> ambil() {
								return keObjectList(dataSkripsi(mahasiswa));
							}
						}));

				list.add(ais.action.master.helper.DashboardReportKit.tabel("Pengajuan Judul Tugas Akhir",
						"Riwayat pengajuan judul TA beserta statusnya.",
						new String[] { "Judul", "Status", "TA", "Dosen", "Keterangan" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							/** Tabel Pengajuan Judul TA dari {@link #dataPengajuanJudulTA(Mahasiswa)}. Membuka session Hibernate sendiri. */
							public List<Object[]> ambil() {
								return keObjectList(dataPengajuanJudulTA(mahasiswa));
							}
						}));

				list.add(ais.action.master.helper.DashboardReportKit.tabel("Riwayat Pengambilan Mata Kuliah",
						"Seluruh mata kuliah yang pernah diambil beserta nilai dan status kelulusan.",
						new String[] { "No", "Kode", "Mata Kuliah", "SKS", "Huruf", "Smt", "Lulus" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							/**
							 * Tabel Riwayat Pengambilan Mata Kuliah: menomori ulang dan meratakan {@code detailData} menjadi
							 * kolom {No, Kode, Mata Kuliah, SKS, Huruf, Smt, Lulus}. Tanpa query tambahan.
							 */
							public List<Object[]> ambil() {
								List<Object[]> r = new ArrayList<Object[]>();
								if (detailData != null) {
									int no = 0;
									for (Map<String, Object> m : detailData) {
										no++;
										r.add(new Object[] { String.valueOf(no), xlsMapStr(m, "kode"), xlsMapStr(m, "nama"),
												xlsMapStr(m, "sks"), xlsMapStr(m, "huruf"), xlsMapStr(m, "smt"),
												xlsBool(m, "lulus", "Lulus", "Belum") });
									}
								}
								return r;
							}
						}));

				list.add(ais.action.master.helper.DashboardReportKit.tabel("Mata Kuliah Belum Diambil",
						"Mata kuliah kurikulum yang belum tercatat diambil mahasiswa.",
						new String[] { "No", "Kode", "Mata Kuliah", "SKS", "Smt", "Jenis" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							/**
							 * Tabel MK Belum Diambil. Sengaja memakai kurikulum OTOMATIS dan TANPA filter jenis
							 * ({@code kurikulumId} dan {@code statusFilter} keduanya {@code null}), sehingga isi laporan cetak
							 * TIDAK mengikuti pilihan combo Kurikulum/Jenis MK pada dasbor. Membuka session Hibernate sendiri.
							 */
							public List<Object[]> ambil() {
								return keObjectList(dataMkBelumDiambil(mahasiswa, detailData, null, null).rows);
							}
						}));

				return list;
			}
		};
	}

	/** Kartu judul + deskripsi singkat (kotak putih rounded) di atas susunan portal dasbor. */
	private static void appendSectionIntro(Component parent, String title, String description) {
		new Html("<div style='border:1px solid #e2e8f0;background:#ffffff;border-radius:16px;padding:14px 16px;"
				+ "box-shadow:0 10px 24px rgba(15,23,42,.06);margin:0 0 4px 0;'>"
				+ "<div style='font-size:15px;font-weight:900;color:#0f172a;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:12px;color:#64748b;line-height:1.6;margin-top:5px;'>" + escapeHtml(description)
				+ "</div></div>").setParent(parent);
	}

	/**
	 * Tabel HTML mata kuliah berkode sama / ekivalen (potensi SKS ganda) untuk verifikasi.
	 * Tiap kelompok = satu kode (sudah ter-resolve ekivalen) yang muncul lebih dari sekali,
	 * disertai kode asli tiap pengambilan agar ekivalensi mudah dikenali.
	 */
	private static String buildDuplikatEkivalenHtml(java.util.LinkedHashMap<String, List<Map<String, Object>>> grupGanda) {
		int totalKelompok = grupGanda.size();
		int jEkiv = 0;
		int jSama = 0;
		int totalEkstra = 0;
		for (Map.Entry<String, List<Map<String, Object>>> e : grupGanda.entrySet()) {
			List<Map<String, Object>> g = e.getValue();
			int totalGrup = 0;
			int samaCount = 0;
			for (Map<String, Object> dt : g) {
				totalGrup += toIntSks(dt.get("sks"));
				String ka = String.valueOf(dt.get("kodeAsli"));
				if (ka != null && ka.equals(e.getKey())) {
					samaCount++;
				} else {
					jEkiv++;
				}
			}
			if (samaCount > 1) {
				jSama += samaCount;
			}
			totalEkstra += (totalGrup - (g.isEmpty() ? 0 : toIntSks(g.get(0).get("sks"))));
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:12px;line-height:1.5;'>");
		sb.append("<div style='margin:0 0 10px;padding:10px 12px;background:#fef2f2;border:1px solid #fecaca;border-radius:8px;color:#991b1b;'>")
				.append("Ditemukan <b>").append(totalKelompok).append("</b> kelompok kode bermasalah: <b>").append(jEkiv)
				.append("</b> mata kuliah <b>ekivalen</b> (dari kode lain) dan <b>").append(jSama)
				.append("</b> mata kuliah <b>kode sama</b>. Potensi <b>").append(totalEkstra)
				.append(" SKS</b> terhitung ganda &mdash; cocokkan dengan selisih SKS saat \"Hitung Ulang\".")
				.append("</div>");
		sb.append("<div style='overflow:auto;'><table style='width:100%;border-collapse:collapse;'>");
		sb.append("<thead><tr style='background:#f1f5f9;'>").append(thDup("Kode Asli")).append(thDup("Nama Mata Kuliah"))
				.append(thDup("SKS")).append(thDup("Smt")).append(thDup("Disamakan ke Kode")).append(thDup("Jenis"))
				.append(thDup("Status")).append("</tr></thead><tbody>");
		for (Map.Entry<String, List<Map<String, Object>>> e : grupGanda.entrySet()) {
			String kode = e.getKey();
			List<Map<String, Object>> g = e.getValue();
			int totalGrup = 0;
			for (Map<String, Object> dt : g) {
				totalGrup += toIntSks(dt.get("sks"));
			}
			int extra = totalGrup - (g.isEmpty() ? 0 : toIntSks(g.get(0).get("sks")));
			String namaUtama = g.isEmpty() ? "" : String.valueOf(g.get(0).get("nama"));
			sb.append("<tr><td colspan='7' style='padding:8px;border-top:2px solid #fdba74;background:#fff7ed;font-weight:700;color:#9a3412;'>")
					.append("Kode utama ").append(escapeHtml(kode)).append(" &mdash; ").append(escapeHtml(namaUtama))
					.append(" &middot; muncul ").append(g.size()).append("&times; &middot; potensi kelebihan ")
					.append(extra).append(" SKS").append("</td></tr>");
			for (Map<String, Object> dt : g) {
				String kodeAsli = String.valueOf(dt.get("kodeAsli"));
				boolean ekivalen = kodeAsli != null && !kodeAsli.equals(kode);
				String jenis = ekivalen
						? "<span style='color:#b91c1c;font-weight:700;'>Ekivalen &rarr; " + escapeHtml(kode) + "</span>"
						: "<span style='color:#9a3412;font-weight:700;'>Kode sama</span>";
				sb.append("<tr>").append(tdDup(escapeHtml(kodeAsli)))
						.append(tdDup(escapeHtml(String.valueOf(dt.get("namaAsli")))))
						.append(tdDup(String.valueOf(toIntSks(dt.get("sksAsli")))))
						.append(tdDup(escapeHtml(String.valueOf(dt.get("smt")))))
						.append(tdDup(escapeHtml(kode)))
						.append(tdDup(jenis))
						.append(tdDup(escapeHtml(String.valueOf(dt.get("valid"))) + " / "
								+ escapeHtml(String.valueOf(dt.get("lulus")))))
						.append("</tr>");
			}
		}
		sb.append("</tbody></table></div></div>");
		return sb.toString();
	}

	/** Sel header {@code <th>} khusus tabel "MK Berkode Sama/Ekivalen" ({@link #buildDuplikatEkivalenHtml}). */
	private static String thDup(String s) {
		return "<th style='text-align:left;padding:6px 8px;border-bottom:1px solid #cbd5e1;font-size:11px;color:#475569;white-space:nowrap;'>"
				+ escapeHtml(s) + "</th>";
	}

	/** Sel data {@code <td>} khusus tabel "MK Berkode Sama/Ekivalen"; {@code s} sudah di-escape oleh pemanggil bila perlu. */
	private static String tdDup(String s) {
		return "<td style='padding:6px 8px;border-bottom:1px solid #eef2f7;vertical-align:top;'>" + (s == null ? "" : s)
				+ "</td>";
	}

	/** Konversi Number/String (mis. dari peta {@code detailData}) ke int SKS; 0 bila gagal. */
	private static int toIntSks(Object o) {
		if (o instanceof Number) {
			return ((Number) o).intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * Buat satu panel {@code MyPanelConfig} (kartu putih rounded, tak bisa dilipat/ditutup) berisi
	 * judul dan deskripsi opsional, dipasang ke {@code parent} kolom portal ({@code pcTop}/
	 * {@code pcLeft}/{@code pcRight}/{@code pcBottom}). Mengembalikan {@link Vbox} isi (body) panel
	 * agar pemanggil menambahkan konten (grafik/tabel/HTML) di dalamnya.
	 */
	private static Vbox createPortalPanel(MyPortalchildren parent, String title, String description) {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setTitle(title);
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle("margin-bottom:12px;border:1px solid #e5e7eb;border-radius:18px;overflow:hidden;"
				+ "background:#ffffff;box-shadow:0 14px 28px rgba(15,23,42,.08);");
		panel.setParent(parent);

		Panelchildren children = new Panelchildren();
		children.setStyle("padding:0;background:#ffffff;");
		children.setParent(panel);

		Vbox body = new Vbox();
		body.setWidth("100%");
		body.setSpacing("12px");
		body.setStyle("padding:14px;box-sizing:border-box;background:#ffffff;");
		body.setParent(children);
		if (description != null && description.trim().length() > 0) {
			new Html("<div style='font-size:12px;line-height:1.6;color:#64748b;margin-bottom:2px;'>"
					+ escapeHtml(description) + "</div>").setParent(body);
		}
		return body;
	}

	/**
	 * Panel HTML "Komposisi Status Mata Kuliah": grid kartu progres (validitas nilai; kelulusan MK
	 * &mdash; hanya ditampilkan bila ada data lulus/tidak lulus; MK konversi) dari
	 * {@link #progressCardHtml}.
	 */
	private static String buildSubjectCompositionHtml(int totalValid, int totalTidakValid, int totalLulus,
			int totalTidakLulus, int totalKonversi, int totalMk) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;'>");
		sb.append(progressCardHtml("Validitas Nilai", totalValid, totalValid + totalTidakValid, "#7c3aed",
				"Menunjukkan berapa banyak nilai yang sudah masuk perhitungan akademik."));
		if (totalLulus + totalTidakLulus > 0) {
			sb.append(progressCardHtml("Kelulusan Mata Kuliah", totalLulus, totalLulus + totalTidakLulus, "#16a34a",
					"Menunjukkan perbandingan mata kuliah yang sudah lulus terhadap seluruh riwayat."));
		}
		sb.append(progressCardHtml("Mata Kuliah Konversi", totalKonversi, totalMk, "#db2777",
				"Menunjukkan mata kuliah yang berasal dari proses konversi atau penyetaraan."));
		sb.append("</div>");
		return sb.toString();
	}

	/** Kartu progres (judul, "value / total", bar persentase clamp 0-100%, deskripsi kecil) berwarna {@code color}. */
	private static String progressCardHtml(String title, int value, int total, String color, String description) {
		int safeTotal = total <= 0 ? 1 : total;
		int pct = (int) Math.round(value * 100.0 / safeTotal);
		if (pct < 0) {
			pct = 0;
		} else if (pct > 100) {
			pct = 100;
		}
		return "<div style='border:1px solid #e2e8f0;border-radius:15px;padding:14px;background:#f8fafc;'>"
				+ "<div style='font-size:12px;font-weight:900;color:#0f172a;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:24px;font-weight:900;color:" + color + ";margin-top:4px;'>" + value + " / "
				+ total + "</div>"
				+ "<div style='height:9px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:8px;'>"
				+ "<div style='width:" + pct + "%;height:9px;background:" + color + ";border-radius:999px;'></div></div>"
				+ "<div style='font-size:11px;color:#64748b;line-height:1.45;margin-top:8px;'>" + escapeHtml(description)
				+ "</div></div>";
	}

	/**
	 * Panel HTML "Rekomendasi Tindak Lanjut Akademik": rangkaian kartu saran sederhana yang
	 * diturunkan dari angka ringkasan (validasi nilai, MK belum lulus, kondisi IPK/IPS/SKS
	 * terakhir dari {@code trendData}, riwayat konversi) &mdash; bukan aturan bisnis tersimpan,
	 * murni teks bantu untuk staf/wali akademik.
	 */
	private static String buildFollowUpRecommendationHtml(List<Map<String, Object>> trendData, int totalValid,
			int totalTidakValid, int totalLulus, int totalTidakLulus, int totalKonversi) {
		double ipkAkhir = 0.0;
		double ipsAkhir = 0.0;
		int sksAkhir = 0;
		if (trendData != null && !trendData.isEmpty()) {
			Map<String, Object> akhir = trendData.get(trendData.size() - 1);
			ipkAkhir = toDouble(akhir.get("ipk"));
			ipsAkhir = toDouble(akhir.get("ips"));
			sksAkhir = toInt(akhir.get("sksk"));
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:12px;'>");
		if (totalTidakValid > 0) {
			sb.append(recommendationItemHtml("Validasi Data Nilai", totalTidakValid
					+ " mata kuliah belum masuk status valid. Periksa nilai, KRS, atau proses sinkronisasi agar laporan akademik lebih akurat.", "#fff7ed", "#9a3412"));
		} else {
			sb.append(recommendationItemHtml("Validasi Data Nilai", "Semua mata kuliah yang terbaca sudah berstatus valid. Tetap lakukan cek berkala setelah ada perubahan nilai.", "#ecfdf5", "#166534"));
		}
		if (totalLulus + totalTidakLulus > 0) {
			if (totalTidakLulus > 0) {
				sb.append(recommendationItemHtml("Mata Kuliah Perlu Perhatian", totalTidakLulus
						+ " mata kuliah belum/tidak lulus. Gunakan data ini untuk arahan perbaikan studi, pengambilan ulang, atau konseling akademik.", "#fef2f2", "#991b1b"));
			} else {
				sb.append(recommendationItemHtml("Kelulusan Mata Kuliah", "Tidak ada mata kuliah tidak lulus pada data yang terbaca. Mahasiswa dapat diarahkan menjaga konsistensi nilai.", "#ecfdf5", "#166534"));
			}
		}
		sb.append(recommendationItemHtml("Kondisi Terakhir", "IPK terakhir " + formatNumber(ipkAkhir) + ", IPS terakhir "
				+ formatNumber(ipsAkhir) + ", dan SKS kumulatif " + sksAkhir
				+ ". Gunakan angka ini sebagai bahan diskusi dengan dosen pembimbing akademik.", "#eff6ff", "#1d4ed8"));
		if (totalKonversi > 0) {
			sb.append(recommendationItemHtml("Riwayat Konversi", totalKonversi
					+ " mata kuliah berasal dari konversi. Pastikan dokumen penyetaraan dan nilai asal sudah tersimpan rapi.", "#fdf2f8", "#9d174d"));
		}
		sb.append("</div>");
		return sb.toString();
	}

	/** Kartu rekomendasi (judul + deskripsi) dengan warna latar/teks kustom, dipakai {@link #buildFollowUpRecommendationHtml}. */
	private static String recommendationItemHtml(String title, String description, String bg, String color) {
		return "<div style='background:" + bg + ";border:1px solid rgba(15,23,42,.07);border-radius:15px;padding:14px;'>"
				+ "<div style='font-size:13px;font-weight:900;color:" + color + ";'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:12px;line-height:1.55;color:#334155;margin-top:6px;'>" + escapeHtml(description)
				+ "</div></div>";
	}


	/** Kartu KPI ringkas tanpa deskripsi. Lihat {@link #createBoxInfo(String, String, String, boolean, String)}. */
	private static Vbox createBoxInfo(String title, String value, String colorHex, boolean isMobile) {
		return createBoxInfo(title, value, colorHex, isMobile, "");
	}

	/**
	 * Kartu KPI "Ringkasan Cepat Studi Mahasiswa" (judul kecil huruf besar, nilai besar berwarna,
	 * deskripsi opsional). {@code isMobile} mengubah lebar kartu jadi 100% (tersusun vertikal) alih-alih
	 * lebar tetap 190px pada tata letak flex desktop.
	 */
	private static Vbox createBoxInfo(String title, String value, String colorHex, boolean isMobile, String description) {
		Vbox box = new Vbox();
		box.setAlign("start");
		box.setPack("center");
		box.setStyle("flex:1 1 " + (isMobile ? "100%" : "190px")
				+ ";min-height:116px;border:1px solid #e2e8f0;padding:14px;background:#ffffff;border-radius:14px;"
				+ "box-shadow:0 10px 22px rgba(15,23,42,0.05);box-sizing:border-box;margin-bottom:"
				+ (isMobile ? "12px" : "0") + ";");

		Label lblTitle = new Label(title);
		lblTitle.setStyle("font-size:11px;font-weight:800;color:#64748b;text-transform:uppercase;letter-spacing:.6px;");
		lblTitle.setParent(box);

		Label lblValue = new Label(value);
		lblValue.setStyle("font-size:25px;font-weight:900;margin-top:5px;color:" + colorHex + ";letter-spacing:-.6px;");
		lblValue.setParent(box);

		if (description != null && description.trim().length() > 0) {
			Label lblDesc = new Label(description);
			lblDesc.setStyle("font-size:11px;line-height:1.45;color:#64748b;margin-top:7px;");
			lblDesc.setParent(box);
		}

		return box;
	}

	/** {@code Groupbox} bergaya kartu modern (mold "3d", rounded, shadow) dengan caption dan deskripsi opsional; dipakai sebagai bingkai "Ringkasan Cepat Studi Mahasiswa". */
	private static Groupbox createModernGroupbox(Component parent, String title, String description) {
		Groupbox groupbox = new Groupbox();
		groupbox.setMold("3d");
		groupbox.setStyle("border-radius:16px;box-shadow:0 14px 30px rgba(15,23,42,0.07);background:#ffffff;"
				+ "border:1px solid #e2e8f0;overflow:hidden;margin-bottom:0;");
		groupbox.setParent(parent);
		org.zkoss.zul.Caption caption = new org.zkoss.zul.Caption(title);
		caption.setStyle("font-weight:800;font-size:14px;color:#0f172a;padding:13px 16px;background:#ffffff;");
		groupbox.appendChild(caption);
		if (description != null && description.trim().length() > 0) {
			Html desc = new Html("<div style='padding:0 16px 12px 16px;color:#64748b;font-size:12px;line-height:1.55;'>"
					+ escapeHtml(description) + "</div>");
			desc.setParent(groupbox);
		}
		return groupbox;
	}

	/**
	 * Grafik batang HTML/CSS "Grafik Tren Studi": satu batang per semester dengan tinggi
	 * proporsional terhadap SKS Kumulatif tertinggi ({@code maxSks}, minimum tinggi 8px agar tetap
	 * terlihat), disertai label semester, SKS semester/kumulatif, dan IPS/IPK. Pesan placeholder
	 * bila {@code trendData} kosong.
	 */
	private static Html createTrendAkademikHtml(List<Map<String, Object>> trendData) {
		StringBuilder html = new StringBuilder();
		html.append("<div style='padding:14px 16px 18px 16px;'>");
		if (trendData == null || trendData.isEmpty()) {
			html.append("<div style='padding:18px;border:1px dashed #cbd5e1;border-radius:12px;color:#64748b;text-align:center;'>Belum ada data semester yang bisa digambarkan.</div>");
			html.append("</div>");
			return new Html(html.toString());
		}
		int maxSks = 1;
		for (Map<String, Object> tr : trendData) {
			int sksKum = toInt(tr.get("sksk"));
			if (sksKum > maxSks) {
				maxSks = sksKum;
			}
		}
		html.append("<div style='display:flex;align-items:flex-end;gap:10px;height:238px;padding:12px 6px 6px 6px;border-left:1px solid #e2e8f0;border-bottom:1px solid #e2e8f0;background:linear-gradient(180deg,#ffffff,#f8fafc);border-radius:12px;'>");
		for (Map<String, Object> tr : trendData) {
			int sksKum = toInt(tr.get("sksk"));
			int sksSmt = toInt(tr.get("sks"));
			double ips = toDouble(tr.get("ips"));
			double ipk = toDouble(tr.get("ipk"));
			int height = (int) Math.round((sksKum * 165.0) / maxSks);
			if (height < 8) {
				height = 8;
			}
			html.append("<div style='flex:1;min-width:42px;text-align:center;'>");
			html.append("<div title='SKS Kumulatif " + sksKum + "' style='height:").append(height)
					.append("px;border-radius:12px 12px 4px 4px;background:linear-gradient(180deg,#2563eb,#38bdf8);box-shadow:0 8px 18px rgba(37,99,235,.18);'></div>");
			html.append("<div style='font-weight:800;color:#0f172a;font-size:11px;margin-top:8px;'>Smt ")
					.append(escapeHtml(String.valueOf(tr.get("smt")))).append("</div>");
			html.append("<div style='font-size:10px;color:#64748b;'>SKS ").append(sksSmt).append(" / Kum ")
					.append(sksKum).append("</div>");
			html.append("<div style='font-size:10px;color:#16a34a;font-weight:700;'>IPS ").append(formatNumber(ips))
					.append(" &bull; IPK ").append(formatNumber(ipk)).append("</div>");
			html.append("</div>");
		}
		html.append("</div>");
		html.append("<div style='display:flex;gap:10px;margin-top:12px;flex-wrap:wrap;'>");
		html.append("<span style='background:#eff6ff;color:#1d4ed8;border-radius:999px;padding:5px 10px;font-size:11px;font-weight:700;'>Batang biru = SKS kumulatif</span>");
		html.append("<span style='background:#ecfdf5;color:#15803d;border-radius:999px;padding:5px 10px;font-size:11px;font-weight:700;'>Teks hijau = IPS dan IPK</span>");
		html.append("</div></div>");
		return new Html(html.toString());
	}

	/**
	 * Header hero (gradient warna tema, {@code var(--ais-theme-primary/accent)}) menampilkan nama,
	 * NIM, prodi, program, fakultas mahasiswa serta blok penjelasan singkat fungsi dasbor. Aman
	 * terhadap null (fallback {@code "-"}); kegagalan resolusi fakultas/prodi/program diredam.
	 */
	private static String buildDashboardHeaderHtml(Mahasiswa mahasiswa) {
		String nim = mahasiswa == null ? "-" : nvl(mahasiswa.getNim(), "-");
		String nama = mahasiswa == null ? "-" : nvl(mahasiswa.getNama(), "-");
		String fakultas = "-";
		String prodi = "-";
		String program = "-";
		try {
			fakultas = mahasiswa != null && mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getFakultas() != null
					? nvl(mahasiswa.getJurusan().getFakultas().getNama(), "-")
					: "-";
			prodi = mahasiswa != null && mahasiswa.getJurusan() != null ? nvl(mahasiswa.getJurusan().getNama(), "-") : "-";
			program = mahasiswa != null ? nvl(mahasiswa.getProgram(), "-") : "-";
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:1543");
			// safe profile fallback
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='border-radius:18px;overflow:hidden;background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);color:white;box-shadow:0 18px 35px rgba(15,23,42,.18);'>");
		sb.append("<div style='padding:22px 24px;display:flex;gap:18px;align-items:center;justify-content:space-between;flex-wrap:wrap;'>");
		sb.append("<div>");
		sb.append("<div style='font-size:12px;text-transform:uppercase;letter-spacing:1px;opacity:.76;font-weight:800;'>Dasbor Studi Mahasiswa</div>");
		sb.append("<div style='font-size:25px;font-weight:900;line-height:1.2;margin-top:4px;'>").append(escapeHtml(nama)).append("</div>");
		sb.append("<div style='font-size:13px;opacity:.9;margin-top:5px;'>NIM ").append(escapeHtml(nim)).append(" &bull; ")
				.append(escapeHtml(prodi)).append(" &bull; ").append(escapeHtml(program)).append("</div>");
		sb.append("</div>");
		sb.append("<div style='background:rgba(255,255,255,.12);border:1px solid rgba(255,255,255,.18);border-radius:14px;padding:12px 14px;min-width:260px;'>");
		sb.append("<div style='font-size:12px;font-weight:800;margin-bottom:5px;'>Fungsi panel</div>");
		sb.append("<div style='font-size:12px;line-height:1.55;opacity:.92;'>Menjelaskan ringkasan nilai, SKS, status validitas, dan catatan akademik mahasiswa dalam tampilan yang mudah dibaca oleh staf, dosen pembimbing, maupun pimpinan.</div>");
		sb.append("</div>");
		sb.append("</div>");
		sb.append("<div style='padding:0 24px 18px 24px;font-size:12px;opacity:.86;'>Fakultas: ").append(escapeHtml(fakultas)).append("</div>");
		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * Panel HTML "Analisa Singkat Akademik": kartu insight (arah IPK naik/turun dibanding semester
	 * pertama tercatat, IPS terakhir, SKS tercapai, validitas data nilai, dan &mdash; bila relevan
	 * &mdash; kelulusan MK) diturunkan dari {@code trendData} per semester.
	 */
	private static String buildAcademicInsightHtml(List<Map<String, Object>> trendData, int totalValid,
			int totalTidakValid, int totalLulus, int totalTidakLulus) {
		double ipkAwal = 0;
		double ipkAkhir = 0;
		double ipsAkhir = 0;
		int sksAkhir = 0;
		if (trendData != null && !trendData.isEmpty()) {
			Map<String, Object> awal = trendData.get(0);
			Map<String, Object> akhir = trendData.get(trendData.size() - 1);
			ipkAwal = toDouble(awal.get("ipk"));
			ipkAkhir = toDouble(akhir.get("ipk"));
			ipsAkhir = toDouble(akhir.get("ips"));
			sksAkhir = toInt(akhir.get("sksk"));
		}
		String arahIpk = ipkAkhir >= ipkAwal ? "stabil/naik" : "turun";
		String warnaIpk = ipkAkhir >= ipkAwal ? "#16a34a" : "#dc2626";
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='padding:14px 16px 18px 16px;'>");
		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;'>");
		sb.append(insightItem("Arah IPK", "IPK " + arahIpk, "Dari " + formatNumber(ipkAwal) + " menjadi " + formatNumber(ipkAkhir), warnaIpk));
		sb.append(insightItem("IPS Terakhir", formatNumber(ipsAkhir), "Nilai semester terakhir yang perlu dipantau.", "#0d9488"));
		sb.append(insightItem("SKS Tercapai", String.valueOf(sksAkhir), "Jumlah SKS kumulatif terakhir.", "#2563eb"));
		sb.append(insightItem("Data Nilai", totalValid + " valid", totalTidakValid + " belum valid/perlu dicek.", "#7c3aed"));
		if (totalLulus + totalTidakLulus > 0) {
			sb.append(insightItem("Kelulusan MK", totalLulus + " lulus", totalTidakLulus + " belum/tidak lulus.", "#ea580c"));
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	/** Kartu insight tunggal (judul kecil, nilai besar berwarna, deskripsi) dipakai {@link #buildAcademicInsightHtml}. */
	private static String insightItem(String title, String value, String description, String color) {
		return "<div style='border:1px solid #e2e8f0;border-radius:14px;padding:13px;background:#f8fafc;'>"
				+ "<div style='font-size:11px;text-transform:uppercase;letter-spacing:.6px;color:#64748b;font-weight:800;'>"
				+ escapeHtml(title) + "</div>" + "<div style='font-size:20px;font-weight:900;color:" + color
				+ ";margin-top:4px;'>" + escapeHtml(value) + "</div>"
				+ "<div style='font-size:11px;color:#64748b;line-height:1.45;margin-top:5px;'>"
				+ escapeHtml(description) + "</div></div>";
	}

	/**
	 * Tutup session Hibernate yang dibuka manual via {@code getSessionFactory().openSession()} oleh
	 * method data panel dasbor ({@link #dataKrsPerSemester}, {@link #dataKehadiran}, dst.) —
	 * {@code clear()} lalu {@code close()}, keduanya diredam bila gagal. No-op bila {@code session == null}.
	 */
	private static void closeOpenedSession(Session session) {
		if (session != null) {
			try {
				session.clear();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:1609");
			}
			try {
				session.close();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:1614");
			}
		}
	}

	/** Konversi Object (Number atau String) ke int; 0 bila null/gagal parse. */
	private static int toInt(Object value) {
		if (value == null) {
			return 0;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(value));
		} catch (Exception e) {
			return 0;
		}
	}

	/** Konversi Object (Number atau String) ke double; 0.0 bila null/gagal parse. */
	private static double toDouble(Object value) {
		if (value == null) {
			return 0.0;
		}
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(value));
		} catch (Exception e) {
			return 0.0;
		}
	}

	/** Format angka pakai {@code Common.numberFormat} (locale ID); fallback {@code String.valueOf} bila formatter gagal. */
	private static String formatNumber(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	/** Null/kosong (setelah trim) -> {@code fallback}, selain itu nilai apa adanya. */
	private static String nvl(String value, String fallback) {
		if (value == null || value.trim().length() == 0) {
			return fallback;
		}
		return value;
	}

	/** Escape karakter HTML dasar ({@code & < > " '}) agar aman disisipkan ke fragmen {@code Html}; null -> string kosong. */
	private static String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		String data = value;
		data = data.replace("&", "&amp;");
		data = data.replace("<", "&lt;");
		data = data.replace(">", "&gt;");
		data = data.replace("\"", "&quot;");
		data = data.replace("'", "&#39;");
		return data;
	}

	
	/** Varian tanpa semester awal terpilih. Lihat {@link #initMain(Mahasiswa, DataLoader, Component, Integer)}. */
	public MyGrid initMain(final Mahasiswa mahasiswa, final DataLoader dataLoader, Component parent) {
		return initMain(mahasiswa, dataLoader, parent, null);
	}

	/**
	 * Bangun toolbar filter semester + toolbar aksi dan {@link MyGrid} riwayat KRS pada tab
	 * KRS/KRS SP/Remedial (`parent` diisi langsung, dipanggil dari listener tab di
	 * {@link #tampil(Mahasiswa, DataLoader, Boolean, Integer)}). Toolbar berisi: combo semester
	 * "mulai"/"sampai" (default dari konfigurasi {@code default_pemilihan_semester_mulai/sampai},
	 * dibatasi semester lulus mahasiswa bila ada) yang memicu {@link #onSearchDefault} saat
	 * berubah; tombol Refresh/Hitung IP-IPK ({@code mahasiswa.reInit()} lalu cari ulang ke
	 * database); Download Semua KRS; {@link #bersihkanKrsMahasiswaDouble} (disembunyikan bila
	 * pengguna adalah dosen/mahasiswa atau konfigurasi
	 * {@code tampilkan_tombol_bersihkan_krs_double} nonaktif); tombol pindah semester KRS +1/-1
	 * (hanya untuk mahasiswa pindahan/alih prodi); Lihat Kurikulum; Catatan (cetak PDF konsultasi);
	 * Biodata; Ambil Nilai dari Neo Feeder (bila fitur feeder aktif — menjalankan
	 * {@code MahasiswaAction.ambilNilaiDariFeeder} di thread terpisah dengan progress bar); History
	 * (buka {@code RevisiDetailPerkuliahanDariMahasiswaHelper}). Grid hasil dipasang kolom via
	 * {@link #createGridColumn} lalu diisi lewat {@link #onSearchDefault(Event, boolean)}.
	 *
	 * @param smtSelected bila tidak {@code null}, kedua combo semester dipaksa ke semester ini
	 *                    (dipakai saat jendela diminta fokus ke satu semester tertentu).
	 * @return grid riwayat KRS yang baru dibangun (juga disimpan di field {@link #grid}).
	 */
	public MyGrid initMain(final Mahasiswa mahasiswa, final DataLoader dataLoader, Component parent,
			Integer smtSelected) {
		this.smtSelected = smtSelected;

		Toolbar hbox = new Toolbar();
		hbox.setWidth("100%");
		hbox.setStyle("display:flex;flex-wrap:wrap;align-items:center;gap:6px;padding:8px 10px;box-sizing:border-box;");
		parent.appendChild(hbox);
		Label labelTampilkan = new Label(ais.common.Common.getBahasaConfig("Tampilkan mulai semester"));
		labelTampilkan.setStyle("white-space:nowrap;margin-right:4px;");
		hbox.appendChild(labelTampilkan);

		semesterMulai = new Combobox();
		semesterSampai = new Combobox();

		int maxSemester = (mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus() > 0)
				? mahasiswa.getSemesterLulus()
				: 40;
		for (Integer i = 1; i <= maxSemester; i++) {
			MyComboitemConfig comboitemMulai = new MyComboitemConfig(i.toString());
			comboitemMulai.setValue(i);
			semesterMulai.appendChild(comboitemMulai);

			MyComboitemConfig comboitemSampai = new MyComboitemConfig(i.toString());
			comboitemSampai.setValue(i);
			semesterSampai.appendChild(comboitemSampai);
		}

		hbox.appendChild(semesterMulai);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(semesterSampai);

		int defaultPemilihanSemesterMulai = Common
				.getKonfigurasi("default_pemilihan_semester_mulai", mahasiswa.currentSemester() + "").niliaInteger();
		int defaultPemilihanSemesterSampai = Common
				.getKonfigurasi("default_pemilihan_semester_sampai", mahasiswa.currentSemester() + "").niliaInteger();

		if (mahasiswa.getSemesterLulus() != null) {
			if (defaultPemilihanSemesterMulai > mahasiswa.getSemesterLulus())
				defaultPemilihanSemesterMulai = 1;
			if (defaultPemilihanSemesterSampai > mahasiswa.getSemesterLulus())
				defaultPemilihanSemesterSampai = mahasiswa.getSemesterLulus();
		}

		if (defaultPemilihanSemesterMulai != defaultPemilihanSemesterSampai) {
			Common.selectComboItem(semesterMulai, defaultPemilihanSemesterMulai);
			Common.selectComboItem(semesterSampai, defaultPemilihanSemesterSampai);
		} else {
			int currentSmt = (mahasiswa.getSemesterLulus() != null
					&& mahasiswa.currentSemester() > mahasiswa.getSemesterLulus()) ? mahasiswa.getSemesterLulus()
							: mahasiswa.currentSemester();
			Common.selectComboItem(semesterMulai, currentSmt);
			Common.selectComboItem(semesterSampai, currentSmt);
		}

		if (smtSelected != null) {
			Common.selectComboItem(semesterMulai, smtSelected);
			Common.selectComboItem(semesterSampai, smtSelected);
		}

		semesterMulai.setWidth("60px");
		semesterSampai.setWidth("60px");
		semesterMulai.setReadonly(true);
		semesterSampai.setReadonly(true);

		EventListener searchListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null, false);
			}
		};

		semesterMulai.addEventListener("onChange", searchListener);
		semesterSampai.addEventListener("onChange", searchListener);

		toolbarbuttonLihat = new MyToolbarbuttonConfig("Refresh / Hitung IP/IPK", "/img/options.png");
		hbox.appendChild(toolbarbuttonLihat);
		toolbarbuttonLihat.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				mahasiswa.reInit();
				onSearchDefault(null, true);
			}
		});

		final MyToolbarbuttonConfig download = new MyToolbarbuttonConfig("Download Semua KRS", "/img/excel.png");
		hbox.appendChild(download);
		download.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PenilaianUtil.downloadSemuaKRS(mahasiswa);
			}
		});

		MyToolbarbuttonConfig bersihkan = bersihkanKrsMahasiswaDouble("Lihat data KRS Double", "/img/excel.png");
		hbox.appendChild(bersihkan);

		tbmuser = Common.getCurrentUser();
		bersihkan.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
				&& Konfigurasi.AKTIF.equals(
						Common.getKonfigurasi("tampilkan_tombol_bersihkan_krs_double", Konfigurasi.AKTIF).getNilai()));

		this.mahasiswa = mahasiswa;

		if (mahasiswa.getMerupakanPindahan() || mahasiswa.getMerupakanAlihProdi()) {
			MyToolbarbuttonConfig pindahkanSemesterKeAtas = new MyToolbarbuttonConfig(
					"Pindahkan KRS Ke Semester Atas (smt +1)", "/img/excel.png");
			hbox.appendChild(pindahkanSemesterKeAtas);
			pindahkanSemesterKeAtas.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					PenilaianUtil.pindahkanSemuaKRS(mahasiswa, 1);
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null, true);
						}
					});
				}
			});

			MyToolbarbuttonConfig pindahkanSemesterKeBawah = new MyToolbarbuttonConfig(
					"Pindahkan KRS Ke Semester Bawah (smt -1)", "/img/excel.png");
			hbox.appendChild(pindahkanSemesterKeBawah);
			pindahkanSemesterKeBawah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					PenilaianUtil.pindahkanSemuaKRS(mahasiswa, -1);
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null, true);
						}
					});
				}
			});
		}

		final MyToolbarbuttonConfig kurikulum = new MyToolbarbuttonConfig("Lihat Kurikulum", "/img/excel.png");
		hbox.appendChild(kurikulum);
		kurikulum.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanKurikulumMahasiswa laporanKurikulum = new LaporanKurikulumMahasiswa(mahasiswa, null);
				laporanKurikulum.setTitle("Riwayat Kurikulum");
				laporanKurikulum.setHeight("95%");
				laporanKurikulum.setWidth("90%");
				laporanKurikulum.setClosable(true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanKurikulum);
				laporanKurikulum.onModal();
			}
		});

		final MyToolbarbuttonConfig catatanMurikulum = new MyToolbarbuttonConfig("Catatan", "/img/print.png");
		hbox.appendChild(catatanMurikulum);
		catatanMurikulum.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				@SuppressWarnings("rawtypes")
				final Map parameters = ais.common.HashMapGenerator.getRand();
				parameters.put("perkuliahan", mahasiswa.getId());
				Report.generatePDFReport(Report.PDF, parameters, "catatan_konsultasi", ais.ui.util.WaktuUtil.getDate());
			}
		});

		MyToolbarbuttonConfig buttonBiodata = new MyToolbarbuttonConfig("Biodata", "/img/online-icon_access.png");
		buttonBiodata.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
				CommonReportHelper.onCetakBiodataMahasiswa(biodataMahasiswa);
			}
		});
		buttonBiodata.setParent(hbox);

		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder() && Konfigurasi.AKTIF
				.equals(Common.getKonfigurasi("aktifkan_terhubung_langsung_ke_feeder", Konfigurasi.AKTIF).getNilai())) {
			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Ambil Nilai",
					"/img/Finance-Invoice-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					MyMessageboxConfig.show(
							"Data nilai yang sudah diinputkan di sistem atau nilai mahasiswa lebih dari 0, tidak bisa diambil dari Feeder. Hanya perkuliahan yg belum dinilai saja yg bisa diambil.\nApakah Anda yakin ingin melanjutkan?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										String[] kon = EksporFromFeederAction.koneksi();
										final String ip = kon[0];
										final String port = kon[1];
										final String username = kon[2];
										final String password = kon[3];
										final String url = kon[4];

										if (!EksporFromFeederAction.exists(url)) {
											MyMessageboxConfig.show(
													ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
													"Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										final List<String> errorLog = new ArrayList<String>();
										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}

												if (!errorLog.isEmpty()) {
													StringBuilder err = new StringBuilder();
													for (String s : errorLog) {
														if (err.length() > 0)
															err.append(
																	"\n----------------------------------------------------------------------------------------------------------\n");
														err.append(s);
													}

													MyMessageboxConfig.show(
															"Error Terjadi, catatan error akan otomatis ter-download",
															"Error Terjadi", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);

													File file = new File(
															"/opt/ecampus/error_" + Common.randLong() + ".txt");
													if (!file.getParentFile().exists())
														file.getParentFile().mkdirs();

													FileUtils.writeStringToFile(file, err.toString());
													Filedownload.save(file, "text/plain");
												}

												Common.createDefaultTimer(new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {
														onSearchDefault(null, true);
													}
												});
											}
										});

										new Thread(new Runnable() {
											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), null);
													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + token);

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														myLabelProsesDetail
																.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}
													MahasiswaAction.ambilNilaiDariFeeder(feederConnector, token, 0,
															mahasiswa, tbmuser, null);
													// FIX "gagal diam-diam": penanda sukses (setValue("")) dipindah ke akhir try agar exception di bawah tidak dianggap sukses; catch tengah yang menelan exception ambilNilaiDariFeeder dihapus.
													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
															"pengambilan data nilai mahasiswa dari Neo Feeder",
															null, e,
															new String[] {
																	"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																	"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
																	"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
															.replace("\n", " "));
												}
											}
										}).start();
									}
								}
							});
				}
			});
			hbox.appendChild(buttonTagihan);
		}

		MyToolbarbuttonConfig buttonHistory = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		buttonHistory.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				RevisiDetailPerkuliahanDariMahasiswaHelper revisiHelper = new RevisiDetailPerkuliahanDariMahasiswaHelper(
						mahasiswa, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								mahasiswa.reInit();
								onSearchDefault(null, true);
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();
			}
		});
		buttonHistory.setParent(hbox);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setSclass("dgrid krs-history-contained-fields");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");

		Columns columns = new Columns();
		columns.setParent(grid);

		createGridColumn(columns, "", "3%");
		createGridColumn(columns, "T/A", "7%");
		createGridColumn(columns, "Smt", "4%");

		if (ConstantValues.jumlahTahapan.isEmpty()) {
			ConstantValues.initJumlahTahapan();
		}

		boolean tampilkanTahap = ConstantValues.aktifkanTahapan
				&& ConstantValues.getJumlahTahapan(mahasiswa.getProgram(), mahasiswa.getJurusan()) > 2;
		String tahapWidth = tampilkanTahap ? "4%" : "0%";
		createGridColumn(columns, "Tahap", tahapWidth);
		createGridColumn(columns, "Keterangan", tampilkanTahap ? "14%" : "16%");
		createGridColumn(columns, "Komentar", "6%");
		createGridColumn(columns, "Catatan", tampilkanTahap ? "12%" : "14%");
		createGridColumn(columns, "IP/IPK", "6%");
		createGridColumn(columns, "SKS/SKSK", "7%");
		createGridColumn(columns, "Kelas", "6%");
		createGridColumn(columns, "Dosen PA", "8%");
		createGridColumn(columns, "Status", "8%");
		createGridColumn(columns, "Tanggal Status", "7%");
		createGridColumn(columns, "Keterangan", "8%");

		onSearchDefault(null, false);

		if (!edit && parent != null) {
			Common.freeze(parent, true);
		}

		return grid;
	}

	/** Tambahkan satu kolom grid ({@code MyColumnConfig}) berlabel dan lebar tertentu ke {@code parent}. */
	private void createGridColumn(Columns parent, String label, String width) {
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(parent);
		column.setLabel(label);
		column.setWidth(width);
	}

	/**
	 * Buat tombol toolbar "Lihat data KRS Double" yang, saat diklik, memicu proses ekspor Excel
	 * berbasis {@code Common.getBahasaConfig} + {@link Timer} polling (bukan alur ekspor
	 * {@link #exportStudiMahasiswaExcel} yang lain) untuk menampilkan/mendata KRS ganda milik
	 * mahasiswa yang berpotensi perlu dibersihkan — hasil ditulis ke berkas sementara di
	 * {@code /tmp/cetak_data_<timestamp>.xlsx} lalu ditampilkan dalam jendela "Cetak Data" saat
	 * proses selesai (ditandai label kosong) atau ditutup otomatis bila label berisi {@code "-"}.
	 * Kegunaan &amp; visibilitas tombol diatur pemanggil di {@link #initMain(Mahasiswa, DataLoader,
	 * Component, Integer)} (disembunyikan untuk dosen/mahasiswa atau bila konfigurasi
	 * {@code tampilkan_tombol_bersihkan_krs_double} nonaktif).
	 *
	 * @param buttonLabel teks tombol.
	 * @param buttonImage path ikon tombol.
	 * @return tombol toolbar siap dipasang ke parent oleh pemanggil.
	 */
	public MyToolbarbuttonConfig bersihkanKrsMahasiswaDouble(String buttonLabel, String buttonImage) {
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				final List<Long> dataDihapus = new ArrayList<Long>();
				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				Clients.showBusy(label.getValue());

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/cetak_data_"
								+ URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
								+ ".xlsx");
				final File file = new File(filename);
				file.createNewFile();

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Clients.showBusy(label.getValue());
							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {
								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
								spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());
								spreadsheet.setMaxrows(intbox.getValue() + 3);
								spreadsheet.setMaxcolumns(8);
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								toolbar.setStyle(
										"background: #ffffff; border-top: 1px solid #e2e8f0; padding: 16px 24px; display: flex; justify-content: flex-end; gap: 12px;");
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										FileInputStream fis = null;
										try {
											fis = new FileInputStream(file);
											Filedownload.save(fis,
													"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										} finally {
											if (fis != null) {
												try {
													fis.close();
												} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2115");
												}
											}
										}
									}
								});
								print.setParent(toolbar);

								final int jumlahDouble = dataDihapus.size();
								MyToolbarbuttonConfig proses = new MyToolbarbuttonConfig(
										"Proses pembersihan data double", "/img/excel.png");
								proses.setVisible(jumlahDouble > 0);
								proses.setTooltiptext(
										"Menghapus permanen salah satu data KRS ganda yang ditandai warna merah");
								proses.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										final String daftarId;
										{
											StringBuilder sb = new StringBuilder();
											for (Long id : dataDihapus) {
												if (sb.length() > 0) {
													sb.append(",");
												}
												sb.append(id);
											}
											daftarId = sb.toString();
										}
										MyMessageboxConfig.show(
												"Mohon perhatian Bapak/Ibu. Tindakan ini akan MENGHAPUS SECARA PERMANEN sebanyak "
														+ jumlahDouble
														+ " baris data KRS (detail perkuliahan) yang ditandai dengan warna merah, yaitu salah satu dari setiap pasangan/kelompok data ganda (double)."
														+ "\n\nKonsekuensi yang perlu Bapak/Ibu pahami sebelum melanjutkan:"
														+ "\n1. Penghapusan bersifat PERMANEN dan TIDAK DAPAT DIBATALKAN maupun dikembalikan. Berbeda dengan penonaktifan, data yang telah dihapus tidak lagi tersimpan di basis data."
														+ "\n2. Apabila baris KRS yang dihapus ternyata sudah memiliki nilai, kehadiran, atau rekaman akademik lain, keterkaitan tersebut dapat ikut hilang atau menjadi tidak lengkap."
														+ "\n3. Sangat disarankan Bapak/Ibu MENGUNDUH data terlebih dahulu (tombol \"Download Data\") sebagai cadangan sebelum melanjutkan."
														+ "\n\nApakah Bapak/Ibu benar-benar yakin ingin menghapus permanen " + jumlahDouble
														+ " baris data KRS ganda tersebut?",
												"Konfirmasi Penghapusan Permanen Data Ganda",
												org.zkoss.zul.Messagebox.YES | org.zkoss.zul.Messagebox.NO,
												MyMessageboxConfig.QUESTION, new EventListener() {
													@Override
													public void onEvent(Event ev) throws Exception {
														if (!"onYes".equals(ev.getName())) {
															return;
														}
														if (daftarId.isEmpty()) {
															return;
														}
														String sql = "delete from detailperkuliahan where id in (" + daftarId + ")";
														Session session = null;
														try {
															session = HibernateUtil.currentSession();
															session.createSQLQuery(sql).executeUpdate();
															onSearchDefault(ev, false);
															window.detach();
														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														} finally {
															HibernateUtil.closeSession();
														}
													}
												});
									}
								});
								proses.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}
						} catch (Exception e) {
							Clients.clearBusy();
						}
					}
				});
				timer.start();

				try {
					Clients.showBusy(label.getValue());

					new Thread(new Runnable() {
						@SuppressWarnings("unchecked")
						@Override
						public void run() {
							Session session = null;
							try {
								XSSFWorkbook workbook = new XSSFWorkbook();
								XSSFSheet sheet = workbook.createSheet("DATA KRS");
								sheet.setDefaultColumnWidth(20);
								XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
								lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								lockedNumericStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
								lockedNumericStyle.setLocked(true);

								session = HibernateUtil.currentNativeSession();

								String sql = "select \na.mahasiswa,c.kode,a.semester,a.tahunakademik \n"
										+ "from detailperkuliahan a  \n"
										+ "left join perkuliahan b on (a.perkuliahan=b.id) \n"
										+ "inner join matakuliah c on (a.matakuliah_konversi=c.id or b.matakuliah=c.id) \n"
										+ "where a.mahasiswa=" + mahasiswa.getId()
										+ (semesterPendek == null ? " and b.status_semesterpendek is null "
												: "  and b.status_semesterpendek = " + semesterPendek + " ")
										+ "  group by a.mahasiswa,c.kode,a.semester,a.tahunakademik \n"
										+ "having count(a.id)>1";

								List<Object[]> data = session.createSQLQuery(sql).list();
								intbox.setValue(data.size());

								int rowIndex = 0;
								XSSFRow rowhead = sheet.createRow((short) 0);
								String[] columns = new String[] { "id", "mahasiswa", "matakuliah", "tahun akademik",
										"semester", "nilai", "huruf", "dosen" };
								for (int i = 0; i < columns.length; i++) {
									rowhead.createCell(i).setCellValue(columns[i].toUpperCase());
								}

								for (Object[] o : data) {
									try {
										if (o == null || o[0] == null || o[1] == null || o[2] == null || o[3] == null)
											continue;

										Long mhsId = Long.parseLong(o[0].toString());
										Integer semester = Integer.parseInt(o[2].toString());
										String kode = o[1].toString().trim();
										String tahunakademik = o[3].toString().trim();

										List<Detailperkuliahan> detailperkuliahans = session
												.createCriteria(Detailperkuliahan.class)
												.add(Restrictions.eq("semester", semester))
												.add(Restrictions.eq("tahunAkademik", tahunakademik))
												.add(Restrictions.eq("mahasiswa.id", mhsId))
												.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
												.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)
												.createAlias("matakuliahKonversi", "matakuliahKonversi",
														Criteria.LEFT_JOIN)
												.add(Restrictions.or(Restrictions.eq("matakuliah.kode", kode),
														Restrictions.eq("matakuliahKonversi.kode", kode)))
												.addOrder(Order.asc("totalNilai"))
												.addOrder(Order.desc("perkuliahan.dosen1")).list();

									int index = 0;
									for (Detailperkuliahan dp : detailperkuliahans) {
										rowIndex++;
										XSSFRow row = sheet.createRow(rowIndex);
										XSSFCell cell0 = row.createCell(0);
										boolean akanDihapus = index < detailperkuliahans.size() - 1;
										if (akanDihapus) {
											dataDihapus.add(dp.getId());
										}
										index++;

										label.setValue("Sedang memproses data " + dp + " ");
										cell0.setCellValue(dp.getId());
										row.createCell(1).setCellValue(
												dp.getMahasiswa() == null ? "" : dp.getMahasiswa().toString());

										String mkVal = dp.getPerkuliahan() == null
												? (dp.getMatakuliahKonversi() == null ? ""
														: dp.getMatakuliahKonversi().toString())
												: dp.getPerkuliahan().toString();

										row.createCell(2).setCellValue(mkVal);
										row.createCell(3).setCellValue(dp.getTahunAkademik());
										row.createCell(4).setCellValue(dp.getSemester());
										row.createCell(5).setCellValue(dp.getTotalNilai());
										row.createCell(6).setCellValue(dp.getNilaiHuruf());
										row.createCell(7)
												.setCellValue(dp.getPerkuliahan() != null
															? dp.getPerkuliahan().populateDosen().values().toString()
															: "");
										if (akanDihapus) {
											for (int cellIndex = 0; cellIndex <= 7; cellIndex++) {
												row.getCell(cellIndex).setCellStyle(lockedNumericStyle);
											}
										}
									}
										session.clear();
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
								}

								FileOutputStream fileOut = null;
								try {
									fileOut = new FileOutputStream(filename);
									workbook.write(fileOut);
								} catch (IOException e) {
									Common.tampilErrorJikaAdmin(e);
								} finally {
									if (fileOut != null) {
										try {
											fileOut.close();
										} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2280");
										}
									}
								}

								data.clear();
								data = null;
								label.setValue("");
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								label.setValue("-");
							} finally {
								if (session != null) {
									try {
										session.clear();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2295");
									}
									try {
										session.disconnect();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2299");
									}
									try {
										session.close();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2303");
									}
								}
								HibernateUtil.closeSession();
							}
						}
					}).start();

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		return toolbarbutton;
	}

	/**
	 * Perender baris grid KRS pada tab KRS/KRS SP/Remedial: satu baris {@code MyGrid} = satu
	 * semester (data string array dari {@code Common.generateSemestersForGrid}, indeks 0=tahun
	 * ajaran, 1=semester(,tahap lama), 3=tahap). Nested class instance (bukan statis) karena
	 * mengakses banyak state induk ({@link #mahasiswa}, {@link #semesterPendek}, {@link #remedial},
	 * {@link #detailUtama}, {@link #edit}, {@link #onSearchDefault}).
	 *
	 * @see #render(Row, Object)
	 */
	class DataRenderer extends ais.ui.util.MyRowRenderer {
		private boolean keDatabase;

		/** @param keDatabase diteruskan ke {@code Common.singkronkanKrsMahasiswa} — {@code true} memaksa sinkronisasi KRS ke database saat baris dirender. */
		public DataRenderer(boolean keDatabase) {
			this.keDatabase = keDatabase;
		}

		/**
		 * Render satu baris KRS. Alur: sinkronkan {@link KrsMahasiswa} semester ini
		 * ({@code Common.singkronkanKrsMahasiswa}); sembunyikan baris bila mahasiswa sudah
		 * keluar/lulus pada semester sebelum semester baris ini; pasang {@code MyDetail} expander
		 * yang, saat dibuka, mendelegasikan render detail nilai/komentar ke
		 * {@code StudiMahasiswaHelper.display(...)} (instance baru per baris). Tiga kasus baris:
		 * <ol>
		 * <li><b>Cuti disetujui</b> (ada {@link PendaftaranCutiMahasiswa} disetujui pada semester/tahap
		 *     ini): tampilkan revisi status + label "Cuti" beserta tanggal/keterangan cuti, kolom lain kosong.</li>
		 * <li><b>Tahap -1 (Konversi lama) / semester 0 (Konversi)</b>: tampilkan keterangan
		 *     pengambilan KRS dan jumlah komentar via {@link ais.common.listener.DataLoader}-style timer
		 *     ({@code Common.createDefaultTimer}/{@code createDefaultTimerNoBusy}), tanpa editor Kelas/Dosen/Status.</li>
		 * <li><b>Baris normal</b>: tampilkan editor inline {@code AmbilDataKelasBanbox} (Kelas) dan
		 *     {@code AmbilDataDosenBanbox} (Dosen PA) yang LANGSUNG commit transaksi Hibernate native
		 *     session saat dipilih (turut memperbarui {@code mahasiswa.kelas}/{@code mahasiswa.dosen}
		 *     bila baris ini semester berjalan mahasiswa); combo Status Mahasiswa (disembunyikan jadi
		 *     label bila status sudah dipaksa via konfigurasi {@code batasStudi}/
		 *     {@code paksaAktifSemester}, atau bila semester 1 / ada SKS bukan konversi) yang saat
		 *     diubah memvalidasi lewat {@code HistoryStatusMahasiswaUtil.checkStatus} lalu
		 *     meng-update {@link HistoryStatusMahasiswa} langsung ke DB; kode status Keluar/Drop/Lulus
		 *     ("K"/"D"/"L") otomatis mengosongkan &amp; menyembunyikan Kelas/Dosen PA; serta combo Status
		 *     Awal Mahasiswa (dikunci di semester 1, dan bila konfigurasi membatasi role yang boleh
		 *     mengubahnya).</li>
		 * </ol>
		 * Efek samping: berbagai cabang menjalankan {@code session.getTransaction().begin()/commit()}
		 * langsung terhadap {@link KrsMahasiswa}/{@link Mahasiswa}/{@link HistoryStatusMahasiswa} di
		 * luar siklus simpan form biasa — perubahan Kelas/Dosen PA/Status tersimpan seketika saat
		 * pengguna memilih nilai baru, tanpa tombol "Simpan" terpisah.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			Vbox vbox = new Vbox();

			final MyLabelAgakKecil catatan = new MyLabelAgakKecil();
			final MyLabelAgakKecil catatanKhs = new MyLabelAgakKecil();
			catatan.setParent(vbox);
			catatanKhs.setParent(vbox);
			final String[] data = (String[]) arg1;

			final MyDetail detail = new MyDetail();
			if (detailUtama == null) {
				detailUtama = detail;
			}

			Integer smt;
			try {
				smt = Integer.parseInt(data[1].split(",")[0]);
			} catch (Exception e) {
				smt = 0;
			}
			final Integer semester = smt;

			Integer tahap;
			try {
				tahap = Integer.parseInt(data[3]);
			} catch (Exception e) {
				tahap = 0;
			}
			final Integer tahapan = tahap;

			final KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan,
					semesterPendek, keDatabase);

			if (krsMahasiswa != null && krsMahasiswa.getMahasiswa() != null
					&& krsMahasiswa.getMahasiswa().getStatusKeluar() != null
					&& krsMahasiswa.getMahasiswa().getSemesterLulus() != null && krsMahasiswa.getSemester() != null
					&& krsMahasiswa.getSemester() > krsMahasiswa.getMahasiswa().getSemesterLulus()) {
				arg0.setVisible(false);
				return;
			}

			detail.setVisible(!semester.equals(1000));
			final String tahunAjaran = data[0];
			final Html komentarshtml = new ais.ui.util.MyHtml("");
			final Html html = new ais.ui.util.MyHtml("");
			final Label ip = new Label();
			final A sks = new A();

			sks.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					KrsMahasiswa krs = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek,
							true);
					PenilaianUtil.downloadSemuaKRS(krs.getSkskS(), mahasiswa);
				}
			});

			detail.setParent(arg0);

			EventListener eventListener = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						Tbmuser currentUser = Common.getCurrentUser();
						boolean adminLainBoleh = false;
						String admLain = Common.getKonfigurasi(
								"admin_lain_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs", "").getNilai();
						if (admLain != null) {
							String[] aa = admLain.split(";");
							for (String a : aa) {
								if (a.trim().equalsIgnoreCase(currentUser.hakAkses().getRoleId())) {
									adminLainBoleh = true;
									break;
								}
							}
						}

						boolean adminNormalBoleh = Konfigurasi.AKTIF.equalsIgnoreCase(
								Common.getKonfigurasi("admin_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs",
										Konfigurasi.TIDAK_AKTIF).getNilai())
								&& Common.getApakahAdmin();

						StudiMahasiswaHelper studiMahasiswaHelper = new StudiMahasiswaHelper(semesterPendek, remedial,
								adminLainBoleh || adminNormalBoleh, tampilKonversi, edit);
						studiMahasiswaHelper.display(mahasiswa, tahunAjaran, semester, tahapan, detail, html,
								komentarshtml, ip, sks, catatan, catatanKhs);
					}
				}
			};

			detail.addEventListener("onOpen", eventListener);
			detail.setAttribute("eventListener", eventListener);

			if (detailUtama == null)
				detailUtama = detail;

			PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahap, false);
			if (pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan()) {
				RevisiHelper.createNewRevisi(KrsMahasiswa.class, krsMahasiswa, data[0]).setParent(arg0);
				String labelSmt = (tahapan != null && tahapan.equals(-1)) ? ""
						: (semester.equals(0) ? "Konversi" : semester.equals(1000) ? "Lulus" : semester.toString());
				new Label(labelSmt).setParent(arg0);
				new Label((tahapan != null && tahapan.equals(-1)) ? "" : tahapan.toString()).setParent(arg0);
				html.setParent(arg0);
				new Label(ais.common.Common.getBahasaConfig("Cuti")).setParent(arg0);
				ip.setParent(arg0);
				sks.setParent(arg0);
				new Label().setParent(arg0);
				new Label().setParent(arg0);
				new Label().setParent(arg0);
				new Label(ais.common.Common.getBahasaConfig("Cuti")).setParent(arg0);
				new Label(Common.dateFormat1.get().format(pendaftaranCutiMahasiswa.getTanggal())).setParent(arg0);
				new Label(pendaftaranCutiMahasiswa.getKeterangan()).setParent(arg0);

			} else {
				HistoryStatusMahasiswa tempHistoryStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
						.getHistoryStatusMahasiswa(krsMahasiswa, keDatabase);
				StatusMahasiswa statusMahasiswa = tempHistoryStatusMahasiswa.getStatusMahasiswa();
				if (ConstantValues.CUTI != null && statusMahasiswa != null
						&& statusMahasiswa.getId().equals(ConstantValues.CUTI.getId())) {
					statusMahasiswa = ConstantValues.AKTIF;
				}

				RevisiHelper.createNewRevisi(KrsMahasiswa.class, krsMahasiswa, data[0]).setParent(arg0);
				String revisiSmtLabel = (tahapan != null && tahapan.equals(-1)) ? ""
						: (semester.equals(0) ? "Konversi" : semester.equals(1000) ? "Lulus" : semester.toString());
				RevisiHelper.createNewRevisi(HistoryStatusMahasiswa.class, tempHistoryStatusMahasiswa, revisiSmtLabel)
						.setParent(arg0);

				new Label((tahapan != null && tahapan.equals(-1)) ? "" : tahapan.toString()).setParent(arg0);
				html.setParent(arg0);
				komentarshtml.setParent(arg0);

				if (tahapan != null && tahapan.equals(-1)) {
					for (int i = 0; i < 9; i++)
						new Label().setParent(arg0);
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							ais.ui.util.KrsMahasiswaAnalisisPopupHelper.pasang(
									html, mahasiswa, krsMahasiswa, remedial);
							Integer komentars = krsMahasiswa.getKomentars();
							komentarshtml.setContent(
									komentars == 0 ? "Tidak ada komentar" : "Terdapat " + komentars + " komentar");
						}
					});
				} else if (semester.equals(0)) {
					new Label().setParent(arg0);
					ip.setParent(arg0);
					sks.setParent(arg0);
					for (int i = 0; i < 6; i++)
						new Label().setParent(arg0);

					Common.createDefaultTimerNoBusy(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							ais.ui.util.KrsMahasiswaAnalisisPopupHelper.pasang(
									html, mahasiswa, krsMahasiswa, remedial);
							Integer komentars = krsMahasiswa.getKomentars();
							komentarshtml.setContent(
									komentars == 0 ? "Tidak ada komentar" : "Terdapat " + komentars + " komentar");
						}
					});
				} else {
					vbox.setParent(arg0);
					ip.setParent(arg0);
					sks.setParent(arg0);

					final AmbilDataKelasBanbox kelasPa = new AmbilDataKelasBanbox();
					kelasPa.setReadonly(true);
					kelasPa.setWidth("90%");
					kelasPa.setParent(arg0);
					kelasPa.setEventListener(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Kelas kelas = (Kelas) kelasPa.getAttribute("kelas");
							krsMahasiswa.setKelas(kelas == null ? null : kelas.getNama());

							Session session = null;
							try {
								session = HibernateUtil.currentNativeSession();
								session.getTransaction().begin();
								Common.refreshSaveOrUpdate(session, krsMahasiswa);
								session.getTransaction().commit();

								if (mahasiswa.currentSemester().equals(semester) && semesterPendek == null) {
									mahasiswa.setKelas(kelas == null ? null : kelas.getNama());
									session.getTransaction().begin();
									Common.refreshSaveOrUpdate(session, mahasiswa);
									session.getTransaction().commit();
								}
							} catch (Exception ex) {
								if (session != null && session.getTransaction().isActive())
									session.getTransaction().rollback();
								throw ex;
							} finally {
								if (session != null) {
									try {
										session.clear();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2531");
									}
									try {
										session.disconnect();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2535");
									}
									try {
										session.close();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2539");
									}
								}
								HibernateUtil.closeSession();
							}
						}
					});

					final AmbilDataDosenBanbox dosenPa = new AmbilDataDosenBanbox();
					dosenPa.setReadonly(true);
					dosenPa.setWidth("90%");
					dosenPa.setParent(arg0);
					dosenPa.setEventListener(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Dosen dosen = (Dosen) dosenPa.getAttribute("dosen");
							krsMahasiswa.setDosenPa(dosen);

							Session session = null;
							try {
								session = HibernateUtil.currentNativeSession();
								session.getTransaction().begin();
								Common.refreshSaveOrUpdate(session, krsMahasiswa);
								session.getTransaction().commit();

								if (mahasiswa.currentSemester().equals(semester) && semesterPendek == null) {
									mahasiswa.setDosen(dosen == null ? null : dosen.getId());
									session.getTransaction().begin();
									Common.refreshSaveOrUpdate(session, mahasiswa);
									session.getTransaction().commit();
								}
							} catch (Exception ex) {
								if (session != null && session.getTransaction().isActive())
									session.getTransaction().rollback();
								throw ex;
							} finally {
								if (session != null) {
									try {
										session.clear();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2578");
									}
									try {
										session.disconnect();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2582");
									}
									try {
										session.close();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2586");
									}
								}
								HibernateUtil.closeSession();
							}
						}
					});

					Vbox vbox2 = new Vbox();
					vbox2.setParent(arg0);
					vbox2.setWidth("100%");
					vbox2.setSclass("krs-inline-field-cell");
					vbox2.setSpacing("2px");
					vbox2.setStyle("padding:1px 2px;");
					tambahCaptionKecil(vbox2, "Status");

					final Combobox status = new Combobox();
					Common.insertCombo(status, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
					status.setReadonly(true);

					Label lblStatus = new Label(tempHistoryStatusMahasiswa == null
							|| tempHistoryStatusMahasiswa.getStatusMahasiswa() == null ? null
									: tempHistoryStatusMahasiswa.getStatusMahasiswa().getNama());

					// Status sudah DIPAKSA (Non Aktif via batasStudi / Aktif via paksaAktifSemester) -> tampilkan
					// LABEL saja (bukan combo yang bisa diubah), agar jelas & tak bisa diedit di sini.
					boolean statusDipaksa = semesterDalamDaftar(mahasiswa == null ? null : mahasiswa.getBatasStudi(), semester)
							|| semesterDalamDaftar(mahasiswa == null ? null : mahasiswa.getPaksaAktifSemester(), semester);
					if (statusDipaksa) {
						lblStatus.setParent(vbox2);
					} else if (mahasiswa != null && mahasiswa.getKelompokStatusMahasiswa() != null
							&& mahasiswa.getKelompokStatusMahasiswa().getSmtMulai() <= semester
							&& mahasiswa.getKelompokStatusMahasiswa().getSmtSampai() >= semester) {
						lblStatus.setParent(vbox2);
					} else if ((smt != null && smt.equals(1)) || krsMahasiswa.getSksBukanKonversi() > 0) {
						lblStatus.setParent(vbox2);
					} else {
						status.setParent(vbox2);
					}

					status.setWidth("100%");
					status.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							if (status.getSelectedItem() == null)
								return;

							HistoryStatusMahasiswa tempHsm = HistoryStatusMahasiswaUtil
									.getHistoryStatusMahasiswa(krsMahasiswa, false);

							if (HistoryStatusMahasiswaUtil.checkStatus(mahasiswa,
									(StatusMahasiswa) status.getSelectedItem().getValue(), semester, tahapan, tempHsm,
									semesterPendek != null && semesterPendek.equals(Perkuliahan.SEMESTER_PENDEK))) {
								Session session = null;
								try {
									session = HibernateUtil.currentNativeSession();
									StatusMahasiswa selectedStatus = (StatusMahasiswa) status.getSelectedItem()
											.getValue();
									tempHsm.put(selectedStatus.getId().toString(), "status_force");
									tempHsm.setStatusMahasiswa(selectedStatus);

									session.getTransaction().begin();
									Common.refreshUpdate(session, tempHsm);
									session.getTransaction().commit();
								} catch (Exception ex) {
									if (session != null && session.getTransaction().isActive())
										session.getTransaction().rollback();
									throw ex;
								} finally {
									if (session != null) {
										try {
											session.clear();
										} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2657");
										}
										try {
											session.disconnect();
										} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2661");
										}
										try {
											session.close();
										} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2665");
										}
									}
									HibernateUtil.closeSession();
								}
							} else {
								Common.selectComboItem(status, tempHsm.getStatusMahasiswa());
							}

							String kodeStatus = tempHsm.getStatusMahasiswa().getKodeEpsbed();
							if (kodeStatus != null && !kodeStatus.isEmpty()) {
								if (kodeStatus.equalsIgnoreCase("K") || kodeStatus.equalsIgnoreCase("D")
										|| kodeStatus.equalsIgnoreCase("L")) {
									boolean berubah = false;
									if (krsMahasiswa != null && krsMahasiswa.getDosenPa() != null) {
										krsMahasiswa.setDosenPa(null);
										berubah = true;
									}
									if (krsMahasiswa.getKelas() != null && !krsMahasiswa.getKelas().trim().isEmpty()) {
										krsMahasiswa.setKelas(null);
										berubah = true;
									}

									if (berubah) {
										Session session = null;
										try {
											session = HibernateUtil.currentSession();
											if (mahasiswa.currentSemester().equals(semester)
													&& semesterPendek == null) {
												session.createSQLQuery(
														"update mahasiswa set dosen=null where id=" + mahasiswa.getId())
														.executeUpdate();
												session.createSQLQuery(
														"update mahasiswa set kelas=null where id=" + mahasiswa.getId())
														.executeUpdate();
											}
											session.update(krsMahasiswa);
										} finally {
											// currentSession is not natively cleared here
										}
									}
									dosenPa.setValue("");
									dosenPa.setAttribute("dosen", null);
									dosenPa.setVisible(false);
									kelasPa.setValue("");
									kelasPa.setAttribute("kelas", null);
									kelasPa.setVisible(false);
								} else {
									dosenPa.setVisible(true);
									kelasPa.setVisible(true);
								}
							} else {
								dosenPa.setVisible(true);
								kelasPa.setVisible(true);
							}

							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(null, true);
								}
							});
						}
					});

					final Combobox statusAwal = new Combobox();
					Common.insertCombo(statusAwal, new String[] { "nama", "kode" }, "keterangan",
							StatusAwalMahasiswa.class, Restrictions.eq("aktif", true));
					statusAwal.setReadonly(true);
					if (smt != null && smt.equals(1))
						statusAwal.setDisabled(true);

					tbmuser = Common.getCurrentUser();
					String authStr = Common.getKonfigurasi("status_awal_mahasiswa_hanya_boleh_diubah_oleh", "")
							.getNilai();
					List<String> stringsStatusAwal = new ArrayList<String>();
					if (authStr != null) {
						for (String s : authStr.split(";")) {
							stringsStatusAwal.add(s.trim());
						}
					}

					boolean bolehUbahStatusAwal = authStr == null || authStr.isEmpty()
							|| (tbmuser != null && tbmuser.hakAkses() != null
									&& stringsStatusAwal.contains(tbmuser.hakAkses().getRoleId()));

					tambahCaptionKecil(vbox2, "Status Awal");
					if (bolehUbahStatusAwal) {
						statusAwal.setParent(vbox2);
					} else {
						new Label(tempHistoryStatusMahasiswa.getStatusAwalMahasiswa() == null ? ""
								: tempHistoryStatusMahasiswa.getStatusAwalMahasiswa().getNama()).setParent(vbox2);
					}
					statusAwal.setWidth("100%");
					statusAwal.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							if (statusAwal.getSelectedItem() == null)
								return;

							HistoryStatusMahasiswa tempHsm = HistoryStatusMahasiswaUtil
									.getHistoryStatusMahasiswa(krsMahasiswa, false);
							Session session = null;
							try {
								session = HibernateUtil.currentNativeSession();
								StatusAwalMahasiswa saMahasiswa = (StatusAwalMahasiswa) statusAwal.getSelectedItem()
										.getValue();
								tempHsm.setStatusAwalMahasiswa(saMahasiswa);

								session.getTransaction().begin();
								Common.refreshUpdate(session, tempHsm);
								session.getTransaction().commit();
							} catch (Exception ex) {
								if (session != null && session.getTransaction().isActive())
									session.getTransaction().rollback();
								throw ex;
							} finally {
								if (session != null) {
									try {
										session.clear();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2785");
									}
									try {
										session.disconnect();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2789");
									}
									try {
										session.close();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2793");
									}
								}
								HibernateUtil.closeSession();
							}

							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(null, true);
								}
							});
						}
					});

					vbox2 = new Vbox();
					vbox2.setParent(arg0);
					vbox2.setWidth("100%");
					vbox2.setSclass("krs-inline-field-cell");
					vbox2.setSpacing("2px");
					vbox2.setStyle("padding:1px 2px;");
					tambahCaptionKecil(vbox2, "Tanggal Status");

					final MyDatebox tanggalStatus = new MyDatebox();
					tanggalStatus.setParent(vbox2);
					tanggalStatus.setWidth("100%");
					tanggalStatus.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							HistoryStatusMahasiswa tempHsm = HistoryStatusMahasiswaUtil
									.getHistoryStatusMahasiswa(krsMahasiswa, true);
							tempHsm.setTanggalStatus(tanggalStatus.getValue());

							Session session = null;
							try {
								session = HibernateUtil.currentNativeSession();
								session.getTransaction().begin();
								Common.refreshUpdate(session, tempHsm);
								session.getTransaction().commit();
								tempHsm.write("tulis ulang dari " + this.getClass().getName());
							} catch (Exception ex) {
								if (session != null && session.getTransaction().isActive())
									session.getTransaction().rollback();
								throw ex;
							} finally {
								if (session != null) {
									try {
										session.clear();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2840");
									}
									try {
										session.disconnect();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2844");
									}
									try {
										session.close();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2848");
									}
								}
								HibernateUtil.closeSession();
							}
						}
					});

					final Combobox program = new Combobox();
					Common.initPrograms(program);
					program.setReadonly(true);
					if (smt != null && smt.equals(1))
						program.setDisabled(true);
					tambahCaptionKecil(vbox2, "Program");
					program.setParent(vbox2);
					program.setWidth("100%");
					program.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							if (program.getSelectedItem() == null)
								return;

							HistoryStatusMahasiswa tempHsm = HistoryStatusMahasiswaUtil
									.getHistoryStatusMahasiswa(krsMahasiswa, false);
							Session session = null;
							try {
								session = HibernateUtil.currentNativeSession();
								String p = (String) program.getSelectedItem().getValue();
								tempHsm.setProgram(p);

								session.getTransaction().begin();
								Common.refreshUpdate(session, tempHsm);
								session.getTransaction().commit();
							} catch (Exception ex) {
								if (session != null && session.getTransaction().isActive())
									session.getTransaction().rollback();
								throw ex;
							} finally {
								if (session != null) {
									try {
										session.clear();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2889");
									}
									try {
										session.disconnect();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2893");
									}
									try {
										session.close();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2897");
									}
								}
								HibernateUtil.closeSession();
							}

							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(null, true);
								}
							});
						}
					});

					final Textbox keterangan = new Textbox();
					keterangan.setParent(arg0);
					keterangan.setWidth("90%");
					keterangan.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							HistoryStatusMahasiswa tempHsm = HistoryStatusMahasiswaUtil
									.getHistoryStatusMahasiswa(krsMahasiswa, true);
							Session session = null;
							try {
								session = HibernateUtil.currentNativeSession();
								tempHsm.setKeterangan(keterangan.getValue());
								session.getTransaction().begin();
								Common.refreshUpdate(session, tempHsm);
								session.getTransaction().commit();
								tempHsm.write("tulis ulang dari " + this.getClass().getName());
							} catch (Exception ex) {
								if (session != null && session.getTransaction().isActive())
									session.getTransaction().rollback();
								throw ex;
							} finally {
								if (session != null) {
									try {
										session.clear();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2936");
									}
									try {
										session.disconnect();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2940");
									}
									try {
										session.close();
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:2944");
									}
								}
								HibernateUtil.closeSession();
							}
						}
					});

					program.setDisabled(true);
					statusAwal.setDisabled(true);
					status.setDisabled(true);
					tanggalStatus.setDisabled(true);
					keterangan.setDisabled(true);

					Common.selectComboItem(status, statusMahasiswa);
					Common.selectComboItem(statusAwal, tempHistoryStatusMahasiswa.getStatusAwalMahasiswa());
					Common.selectComboItem(program, tempHistoryStatusMahasiswa.getProgram());

					tanggalStatus.setValue(
							tempHistoryStatusMahasiswa == null ? null : tempHistoryStatusMahasiswa.getTanggalStatus());
					keterangan.setValue(
							tempHistoryStatusMahasiswa == null ? null : tempHistoryStatusMahasiswa.getKeterangan());

					tbmuser = Common.getCurrentUser();
					boolean adminLainBoleh = false;
					String admLainStatus = Common.getKonfigurasi("admin_lain_bisa_mengubah_status_mahasiswa", "")
							.getNilai();
					if (admLainStatus != null) {
						for (String a : admLainStatus.split(";")) {
							if (a.trim().equalsIgnoreCase(tbmuser.hakAkses().getRoleId())) {
								adminLainBoleh = true;
								break;
							}
						}
					}

					if (adminLainBoleh || Common.getApakahAdmin()) {
						status.setDisabled(false);
						statusAwal.setDisabled(false);
						program.setDisabled(false);
						tanggalStatus.setDisabled(false);
						keterangan.setDisabled(false);
						dosenPa.setDisabled(false);
						kelasPa.setDisabled(false);
					} else {
						statusAwal.setDisabled(true);
						program.setDisabled(true);
						status.setDisabled(true);
						tanggalStatus.setDisabled(true);
						keterangan.setDisabled(true);
						dosenPa.setDisabled(true);
						kelasPa.setDisabled(true);
					}

					if (mahasiswa.getProgramMahasiswa() != null || mahasiswa.getProgramSelaluIkutDataUtama()) {
						program.setDisabled(true);
					}

					String kodeStatus = statusMahasiswa == null ? "" : statusMahasiswa.getKodeEpsbed();
					if ("K".equalsIgnoreCase(kodeStatus) || "L".equalsIgnoreCase(kodeStatus)) {
						boolean berubah = false;
						if (krsMahasiswa != null && krsMahasiswa.getDosenPa() != null) {
							krsMahasiswa.setDosenPa(null);
							berubah = true;
						}
						if (krsMahasiswa.getKelas() != null && !krsMahasiswa.getKelas().trim().isEmpty()) {
							krsMahasiswa.setKelas(null);
							berubah = true;
						}

						if (berubah) {
							Session session = HibernateUtil.currentSession();
							if (mahasiswa.currentSemester().equals(semester) && semesterPendek == null) {
								session.createSQLQuery("update mahasiswa set dosen=null where id=" + mahasiswa.getId())
										.executeUpdate();
								session.createSQLQuery("update mahasiswa set kelas=null where id=" + mahasiswa.getId())
										.executeUpdate();
							}
							session.update(krsMahasiswa);
						}
						dosenPa.setVisible(false);
						kelasPa.setVisible(false);
					}

					catatan.setValue(krsMahasiswa.getCatatan());
					catatanKhs.setValue(krsMahasiswa.getCatatanKhs());

					Vbox vbox1 = new Vbox();
					vbox1.setParent(vbox);
					Hbox hbox1 = new Hbox();

					LampiranLain.createDownloadUploadFileLain(hbox1, krsMahasiswa.getId(), "KRS_DISETUJUI", "Catatan",
							false, new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
								}
							}, null, false, false, false, true);

					hbox1.setParent(vbox1);

					ais.ui.util.KrsMahasiswaAnalisisPopupHelper.pasang(
							html, mahasiswa, krsMahasiswa, remedial);
					Integer komentars = krsMahasiswa.getKomentars();
					komentarshtml
							.setContent(komentars == 0 ? "Tidak ada komentar" : "Terdapat " + komentars + " komentar");

					dosenPa.setAttribute("dosen", krsMahasiswa.getDosenPa());
					dosenPa.setAttribute("myValue", krsMahasiswa.getDosenPa());
					dosenPa.setValue(krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama());

					kelasPa.setValue(krsMahasiswa.getKelas());
					kelasPa.setReadonly(true);

					Double ipmhs = krsMahasiswa.getIps();
					Double ipkmhs = krsMahasiswa.getIpk();
					Integer sksmhss = krsMahasiswa.getSksYangDiambil();
					Integer sksmhs = krsMahasiswa.getSksk();

					ip.setValue(
							Common.numberFormat.get().format(ipmhs) + " / " + Common.numberFormat.get().format(ipkmhs));

					Integer skskonversi = krsMahasiswa.getSksKonversi();
					Integer sksBukanKonversi = krsMahasiswa.getSksBukanKonversi();

					String labelSks = Common.numberFormat.get().format(sksmhss) + " / "
							+ Common.numberFormat.get().format(sksmhs);
					if (skskonversi > 0) {
						labelSks += " (Bukan Konversi : " + Common.numberFormat.get().format(sksBukanKonversi)
								+ " SKS, Konversi " + Common.numberFormat.get().format(skskonversi) + " SKS)";
					}
					sks.setLabel(labelSks);

					if (mahasiswa.getStatusAwalSelaluIkutDataUtama() || (semester != null
							&& mahasiswa.getSmtStatusAwal() != null && mahasiswa.getSmtStatusAwal() <= semester
							&& mahasiswa.getStatusAwalMahasiswaSetelahSmtTertentu() != null)) {
						statusAwal.setDisabled(true);
					}

					KelompokMahasiswa kelompokMahasiswa = mahasiswa.getKelompokMahasiswa();
					if (kelompokMahasiswa != null) {
						if (kelompokMahasiswa.getStatusAwalMahasiswa() != null
								&& kelompokMahasiswa.getSmtMulai() <= semester
								&& kelompokMahasiswa.getSmtSampai() >= semester) {
							statusAwal.setDisabled(true);
						} else if (kelompokMahasiswa.getStatusAwalMahasiswa2() != null
								&& kelompokMahasiswa.getSmtMulai2() <= semester
								&& kelompokMahasiswa.getSmtSampai2() >= semester) {
							statusAwal.setDisabled(true);
						} else if (kelompokMahasiswa.getStatusAwalMahasiswa3() != null
								&& kelompokMahasiswa.getSmtMulai3() <= semester
								&& kelompokMahasiswa.getSmtSampai3() >= semester) {
							statusAwal.setDisabled(true);
						}
					}

					if (semester != null && mahasiswa.getSmtStatusAwalLagi() != null
							&& mahasiswa.getSmtStatusAwalLagi() <= semester
							&& mahasiswa.getStatusAwalMahasiswaSetelahSmtTertentuLagi() != null) {
						statusAwal.setDisabled(true);
					}

					if (mahasiswa.getDosenPaSelaluSama())
						dosenPa.setDisabled(true);
					if (mahasiswa.getKelasSelaluSama())
						kelasPa.setDisabled(true);

					if (semester != null && mahasiswa.getBatasStudi() != null && !mahasiswa.getBatasStudi().isEmpty()) {
						for (String s : mahasiswa.getBatasStudi().split(",")) {
							if (s.trim().equalsIgnoreCase(semester.toString())) {
								status.setDisabled(true);
								break;
							}
						}
					}

					// Combo/Bandbox yang DISABLED diganti Label agar teksnya terbaca penuh & jelas (permintaan
					// user: combo disabled -> label). statusAwal (Combobox), dosenPa/kelasPa (Bandbox).
					gantiInputDisabledJadiLabel(statusAwal);
					gantiInputDisabledJadiLabel(dosenPa);
					gantiInputDisabledJadiLabel(kelasPa);
				}
			}
		}
	}

	/** Caption kecil (dim) di atas field pada sel Status/Tanggal agar jelas & rapi. No-op bila gagal. */
	private static void tambahCaptionKecil(Vbox cell, String teks) {
		try {
			Label c = new Label(teks);
			c.setStyle("font-size:9px;color:#94a3b8;line-height:1.1;");
			c.setParent(cell);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:3135");
		}
	}

	/** True bila semester termuat di daftar semester dipisah koma (mis. "3,4,5"). */
	private static boolean semesterDalamDaftar(String daftar, Integer semester) {
		if (daftar == null || daftar.trim().isEmpty() || semester == null) {
			return false;
		}
		for (String s : daftar.split(",")) {
			if (s != null && s.trim().equalsIgnoreCase(String.valueOf(semester))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Bila komponen input (Combobox/Bandbox/Textbox) DISABLED, sembunyikan & sisipkan Label berisi
	 * teksnya di posisi sama -> lebih jelas (tak terpotong seperti combo disabled). No-op bila null/
	 * bukan input/tidak disabled.
	 */
	private static void gantiInputDisabledJadiLabel(org.zkoss.zk.ui.Component comp) {
		if (!(comp instanceof org.zkoss.zul.impl.InputElement) || comp.getParent() == null) {
			return;
		}
		try {
			org.zkoss.zul.impl.InputElement ie = (org.zkoss.zul.impl.InputElement) comp;
			if (!ie.isDisabled()) {
				return;
			}
			String teks = null;
			if (comp instanceof org.zkoss.zul.Combobox) {
				org.zkoss.zul.Combobox cb = (org.zkoss.zul.Combobox) comp;
				teks = cb.getSelectedItem() != null ? cb.getSelectedItem().getLabel() : cb.getValue();
			} else {
				teks = ie.getText();
			}
			org.zkoss.zul.Label lbl = new org.zkoss.zul.Label(teks == null ? "" : teks);
			lbl.setStyle("font-weight:600;color:#334155;");
			comp.getParent().insertBefore(lbl, comp);
			comp.setVisible(false);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TampilStudiMahasiswaHelper.java:3177");
		}
	}

	/**
	 * Muat ulang isi grid KRS sesuai rentang semester terpilih ({@link #semesterMulai}/
	 * {@link #semesterSampai}; default semester berjalan mahasiswa bila belum ada seleksi). Membuat
	 * ulang model baris ({@code Common.generateSemestersForGrid}) dan memasang {@link DataRenderer}
	 * baru dengan flag {@code keDatabase}. Bila {@link #smtSelected} diisi, baris detail pertama
	 * ({@link #detailUtama}) otomatis dibuka lewat timer setelah grid selesai dirender, agar
	 * jendela langsung fokus ke semester yang diminta.
	 *
	 * @param event event pemicu (tidak dipakai isinya, boleh {@code null}); dipertahankan karena
	 *              method ini juga dipasang langsung sebagai target listener {@code onClick}/{@code onChange}.
	 * @param keDatabase diteruskan ke {@link DataRenderer} — {@code true} memaksa sinkronisasi KRS ke database saat render (mis. tombol "Refresh / Hitung IP/IPK").
	 */
	public void onSearchDefault(Event event, boolean keDatabase) {
		detailUtama = null;
		Integer mulai = (Integer) (semesterMulai.getSelectedItem() == null ? 0
				: semesterMulai.getSelectedItem().getValue());

		if (semesterSampai.getSelectedItem() == null || semesterSampai.getSelectedItem().getValue() == null) {
			Integer smt = mahasiswa.currentSemester();
			Common.selectComboItem(true, semesterSampai, smt);
		}

		Integer sampai = (Integer) (semesterSampai.getSelectedItem() == null ? 0
				: semesterSampai.getSelectedItem().getValue());

		ListModel strset = new SimpleListModel(
				Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendek));
		grid.setRowRenderer(new DataRenderer(keDatabase));
		grid.setModelCheckMobile(strset);

		if (smtSelected != null) {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (detailUtama != null) {
						detailUtama.setOpen(true);
						EventListener eventListener = (EventListener) detailUtama.getAttribute("eventListener");
						if (eventListener != null) {
							eventListener.onEvent(arg0);
						}
					}
				}
			});
		}
	}
}
