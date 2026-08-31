package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Column;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.TugasPertemuan;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Komponen dashboard khusus untuk dashboard tren aktivitas perkuliahan. Kelas ini memilih variasi
 * data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas
 * induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code int DASHBOARD_SAMPLE_LIMIT}, {@code int
 * MAX_WORKER}, {@code int TOP_BAR_LIMIT}, {@code Combobox searchfakultas}, {@code Combobox searchjurusan},
 * {@code Combobox tahunAkademik}, {@code Combobox semesterAbsensi}, {@code Combobox searchsemester};
 * inisialisasi/lifecycle ({@code initFakultas()}, {@code init()}, {@code initSemesterCombobox()});
 * pembacaan/pencarian ({@code refreshSemesterKeOptions()}, {@code getDashboardBodyContainer()}, {@code
 * loadDashboardData()}, {@code loadDokumenPerkuliahan()}, {@code getStatusKehadiranOrder()}, {@code
 * getStatusKehadiranLabel()}); validasi/perhitungan ({@code isPertemuanOnlineTerhitung()}); pelaporan/ekspor
 * ({@code renderWelcomeDashboard()}, {@code renderDashboardDataOnUi()}, {@code renderDashboardDataNow()}, {@code
 * renderGridRekap()}, {@code renderGridRekapPage()}, {@code renderPageNavDash()}); operasi domain lain ({@code
 * purgeExpiredDashCache()}, {@code applyUserRestrictionToFilters()}, {@code dashCacheKey()}, {@code
 * filteredMatkulStats()}, {@code processDashboardRows()}, {@code buildMatkulStatFromRekapRow()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardTrenAktivitasPerkuliahan extends MyWindow {

	private static final long serialVersionUID = 1L;
	private static final int DASHBOARD_SAMPLE_LIMIT = Integer.MAX_VALUE;
	private static final int MAX_WORKER = 100;
	private static final int TOP_BAR_LIMIT = 12;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();
	private AmbilDataMahasiswaBanbox searchMahasiswa = new AmbilDataMahasiswaBanbox();

	private Center mainCenter;
	private org.zkoss.zul.Div mainContainer;
	/*
	 * Filter dashboard ditempatkan di dalam mainContainer, bukan North, supaya ikut
	 * bergerak ketika area Center di-scroll. Semua isi dashboard dinamis
	 * dirender ke dashboardBodyContainer agar filter tidak ikut terhapus saat reload.
	 */
	private org.zkoss.zul.Div dashboardBodyContainer;
	private org.zkoss.zul.Div summaryContainer;
	private org.zkoss.zul.Div dashboardVisualContainer;
	private Grid gridRekap;
	private Rows gridRows;

	// ── Filter + paging MK ──────────────────────────────────────────────────
	private String filterMkGrid = "";
	private org.zkoss.zul.Textbox txFilterMkGrid;
	private static final int PAGE_SIZE_GRID = 10;
	private int pageGrid = 0;
	private org.zkoss.zul.Div gridRekapContainer;
	private DashboardData lastDashboardData;

	// ── L1 cache (5 menit TTL) ──────────────────────────────────────────────
	private static final ConcurrentHashMap<String, DashboardData> DASH_CACHE  = new ConcurrentHashMap<String, DashboardData>();
	private static final ConcurrentHashMap<String, Long>          DASH_EXPIRY = new ConcurrentHashMap<String, Long>();
	private static final long DASH_CACHE_TTL_MS = 5L * 60 * 1000;

	/**
	 * Buang entry kadaluarsa dari kedua map cache (optimasi RAM Fase 2): TTL sebelumnya
	 * hanya dicek saat READ key yang sama, sehingga kombinasi filter user yang tidak
	 * diakses lagi menumpuk selamanya (value DashboardData berisi agregat besar).
	 * Dipanggil saat put (jarang). Race dengan put paralel key sama hanya berakibat
	 * cache-miss sekali — bukan masalah correctness.
	 */
	private static void purgeExpiredDashCache() {
		long sekarang = System.currentTimeMillis();
		for (java.util.Iterator<java.util.Map.Entry<String, Long>> it = DASH_EXPIRY.entrySet().iterator(); it
				.hasNext();) {
			java.util.Map.Entry<String, Long> e = it.next();
			Long kadaluarsa = e.getValue();
			if (kadaluarsa == null || kadaluarsa <= sekarang) {
				it.remove();
				DASH_CACHE.remove(e.getKey());
			}
		}
	}

	private TreeMap<String, Long> pertemuansa = new TreeMap<String, Long>();
	private transient Html dashboardLoadingHtml;

	public DashboardTrenAktivitasPerkuliahan() {
		super();
		try {
			init();
			initFakultas();
		} catch (Throwable e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:116");
			Common.tampilErrorJikaAdmin(new Exception("Gagal inisialisasi UI Dasbor: " + e.getMessage()));
		}
	}

	public DashboardTrenAktivitasPerkuliahan(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initFakultas();
		} catch (Throwable e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:127");
			Common.tampilErrorJikaAdmin(new Exception("Gagal inisialisasi UI Dasbor: " + e.getMessage()));
		}
	}

	private void initFakultas() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Tren Aktivitas Perkuliahan");
		// Tinggi 100% MENGIKUTI PARENT: di-mount via BaseDasbordPortal.mountWrappedTinggiPenuh
		// yang menyiapkan rantai tinggi-penuh (tabpanel 100% → portal/kolom/panel/host flex)
		// sehingga height:100% mengakar ke tinggi tab. Lantai pengaman ADAPTIF
		// (calc(100vh - 220px)) — mengikuti tinggi layar/parent, bukan angka tetap;
		// mencegah kolaps bila pengukuran flex gagal sekaligus tetap terasa penuh.
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");
		setStyle("min-height:calc(100vh - 220px);");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setStyle("min-height:calc(100vh - 220px);");
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid filterGrid = new MyGrid();
		filterGrid.setWidth("100%");
		filterGrid.setStyle("border:none; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; "
				+ "padding:10px 12px; box-sizing:border-box; box-shadow:0 10px 26px rgba(15,23,42,.06); "
				+ "margin-bottom:12px; overflow:visible;");
		/*
		 * Parent sementara. Setelah mainContainer dibuat, filterGrid dipindahkan ke
		 * mainContainer agar ikut scroll dan tidak sticky di atas.
		 */
		filterGrid.setParent(north);

		// Kolom eksplisit agar input filter (combo/search) LEBIH LEBAR & tidak terpotong: kolom
		// LABEL dibuat sempit, kolom INPUT dibuat lebar. Grid memiliki 8 kolom (title row span "8"):
		// label di kolom ganjil (1,3,5), input di kolom genap (2,4,6), tombol "Muat Data" di kolom 7.
		// fixedLayout=true agar lebar persen dihormati.
		ais.ui.util.ZkCompat.setFixedLayout(filterGrid, true);
		Columns filterCols = new Columns();
		filterCols.setParent(filterGrid);
		String[] lebarKolomFilter = { "9%", "18%", "9%", "18%", "9%", "18%", "14%", "5%" };
		for (String lebar : lebarKolomFilter) {
			Column kol = new Column();
			kol.setWidth(lebar);
			kol.setParent(filterCols);
		}

		Rows filterRows = new Rows();
		filterRows.setParent(filterGrid);

		MyFormRow titleRow = new MyFormRow();
		titleRow.setParent(filterRows);
		ais.ui.util.ZkCompat.setSpans(titleRow, "8");
		titleRow.appendChild(new Html(buildFilterHeaderHtml()));

		MyFormRow rowFilter1 = new MyFormRow();
		rowFilter1.setParent(filterRows);
		rowFilter1.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		rowFilter1.appendChild(searchfakultas);
		searchfakultas.setWidth("95%");
		rowFilter1.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		rowFilter1.appendChild(searchjurusan);
		searchjurusan.setWidth("95%");
		rowFilter1.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		rowFilter1.appendChild(tahunAkademik);
		tahunAkademik.setWidth("95%");
		tahunAkademik.setReadonly(true);

		MyFormRow rowFilter2 = new MyFormRow();
		rowFilter2.setParent(filterRows);
		rowFilter2.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		initSemesterCombobox();
		rowFilter2.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("95%");
		semesterAbsensi.setReadonly(true);
		rowFilter2.appendChild(new ais.ui.util.MyLabelConfig("Semester ke"));
		rowFilter2.appendChild(searchsemester);
		searchsemester.setWidth("95%");
		rowFilter2.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.initPrograms(searchprogram);
		rowFilter2.appendChild(searchprogram);
		searchprogram.setWidth("95%");

		final EventListener eventSemester = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				refreshSemesterKeOptions();
			}
		};
		semesterAbsensi.addEventListener("onChange", eventSemester);
		eventSemester.onEvent(null);

		MyFormRow rowFilter3 = new MyFormRow();
		rowFilter3.setParent(filterRows);
		rowFilter3.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		rowFilter3.appendChild(searchDosen);
		searchDosen.setWidth("95%");
		rowFilter3.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		rowFilter3.appendChild(searchMahasiswa);
		searchMahasiswa.setWidth("95%");
		rowFilter3.appendChild(new ais.ui.util.MyLabelConfig("Cari MK"));
		txFilterMkGrid = new org.zkoss.zul.Textbox();
		//txFilterMkGrid.setPlaceholder("Kode / nama MK...");
		txFilterMkGrid.setWidth("95%");
		txFilterMkGrid.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				filterMkGrid = txFilterMkGrid.getValue();
				pageGrid = 0;
				if (lastDashboardData != null) {
					renderGridRekapPage(lastDashboardData);
				}
			}
		});
		rowFilter3.appendChild(txFilterMkGrid);

		Hbox hboxAction = new Hbox();
		hboxAction.setSpacing("8px");
		rowFilter3.appendChild(hboxAction);

		MyToolbarbuttonConfig btnLoad = new MyToolbarbuttonConfig("Muat Data Dasbor", "/img/svg/refresh-cw.svg");
		btnLoad.setStyle("font-weight:900; border-radius:999px; padding:7px 14px; background:#2563eb; color:#ffffff; border:0;");
		btnLoad.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadDashboardData();
			}
		});
		btnLoad.setParent(hboxAction);

		applyUserRestrictionToFilters();

		mainCenter = new Center();
		mainCenter.setParent(borderlayout);
		mainCenter.setAutoscroll(true);
		ais.ui.util.ZkCompat.setFlex(mainCenter, true);
		mainCenter.setStyle("overflow-y:auto; overflow-x:hidden; background:#f6f8fb;");

		mainContainer = new org.zkoss.zul.Div();
		mainContainer.setWidth("100%");
		mainContainer.setStyle("background:#f6f8fb; padding:14px; box-sizing:border-box; padding-bottom:48px;");
		mainContainer.setParent(mainCenter);

		/*
		 * Pindahkan filter dari North ke mainContainer supaya ketika Center di-scroll,
		 * filter ikut naik bersama konten dashboard. North kemudian disembunyikan agar
		 * tidak menyisakan ruang kosong di atas.
		 */
		filterGrid.setParent(mainContainer);
		north.setVisible(false);

		dashboardBodyContainer = new org.zkoss.zul.Div();
		dashboardBodyContainer.setWidth("100%");
		dashboardBodyContainer.setStyle("margin-top:12px;");
		dashboardBodyContainer.setParent(mainContainer);

		renderWelcomeDashboard();

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDashboardData();
			}
		});
	}

	private void initSemesterCombobox() {
		semesterAbsensi = new Combobox();
		Comboitem itemGanjil = new MyComboitemConfig(Perkuliahan.GANJIL);
		itemGanjil.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(itemGanjil);

		Comboitem itemGenap = new MyComboitemConfig(Perkuliahan.GENAP);
		itemGenap.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(itemGenap);

		Comboitem itemSp = new MyComboitemConfig(Perkuliahan.SP);
		itemSp.setValue(Perkuliahan.SP);
		semesterAbsensi.appendChild(itemSp);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP, true);
	}

	private void refreshSemesterKeOptions() {
		try {
			Common.clear(searchsemester);
			searchsemester.setSelectedItem(null);
			Comboitem semua = new MyComboitemConfig("Semua");
			semua.setValue(null);
			searchsemester.appendChild(semua);

			String semester = semesterAbsensi.getSelectedItem() == null ? null
					: (String) semesterAbsensi.getSelectedItem().getValue();
			if (Perkuliahan.GENAP.equals(semester)) {
				for (int i : Common.genap) {
					if (i == 0) {
						continue;
					}
					Comboitem item = new MyComboitemConfig(String.valueOf(i));
					item.setValue(Integer.valueOf(i));
					searchsemester.appendChild(item);
				}
			} else if (Perkuliahan.GANJIL.equals(semester)) {
				for (int i : Common.ganjil) {
					Comboitem item = new MyComboitemConfig(String.valueOf(i));
					item.setValue(Integer.valueOf(i));
					searchsemester.appendChild(item);
				}
			}
			searchsemester.setSelectedItem(semua);
			searchsemester.setReadonly(true);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:352");
		}
	}

	private void applyUserRestrictionToFilters() {
		try {
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser == null) {
				return;
			}
			Dosen dosenLogin = tbmuser.ambilDosen();
			Mahasiswa mahasiswaLogin = tbmuser.getMahasiswa();
			if (dosenLogin != null && dosenLogin.getNama() != null) {
				searchDosen.setAttribute("dosen", dosenLogin);
				searchDosen.setValue(dosenLogin.getNama());
				searchDosen.setReadonly(true);
				searchDosen.setDisabled(true);
			}
			if (mahasiswaLogin != null && mahasiswaLogin.getNama() != null) {
				searchMahasiswa.setAttribute("mahasiswa", mahasiswaLogin);
				searchMahasiswa.setValue(mahasiswaLogin.getNama());
				searchMahasiswa.setReadonly(true);
				searchMahasiswa.setDisabled(true);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:377");
		}
	}

	private org.zkoss.zul.Div getDashboardBodyContainer() {
		return dashboardBodyContainer == null ? mainContainer : dashboardBodyContainer;
	}

	private String dashCacheKey(String tahun, String semester, Integer semesterKe, String program,
			Fakultas fakultas, Jurusan jurusan, Dosen dosen, Mahasiswa mahasiswa) {
		return (tahun == null ? "" : tahun) + "|"
				+ (semester == null ? "" : semester) + "|"
				+ (semesterKe == null ? "" : semesterKe) + "|"
				+ (program == null ? "" : program) + "|"
				+ (fakultas == null ? "" : fakultas.getId()) + "|"
				+ (jurusan == null ? "" : jurusan.getId()) + "|"
				+ (dosen == null ? "" : dosen.getId()) + "|"
				+ (mahasiswa == null ? "" : mahasiswa.getId());
	}

	private List<MatkulDashboardStat> filteredMatkulStats(DashboardData data) {
		if (filterMkGrid == null || filterMkGrid.trim().isEmpty()) return data.matkulStats;
		String q = filterMkGrid.trim().toLowerCase();
		List<MatkulDashboardStat> res = new ArrayList<MatkulDashboardStat>();
		for (MatkulDashboardStat s : data.matkulStats) {
			if ((s.kode != null && s.kode.toLowerCase().contains(q))
					|| (s.matakuliah != null && s.matakuliah.toLowerCase().contains(q)))
				res.add(s);
		}
		return res;
	}

	private void renderWelcomeDashboard() {
		if (mainContainer == null) {
			return;
		}
		org.zkoss.zul.Div body = getDashboardBodyContainer();
		Common.clear(body);
		body.appendChild(new Html(buildWelcomeHtml()));
	}

	private void loadDashboardData() {
		if (mainContainer == null) {
			return;
		}
		final String tahun = getComboStringValue(tahunAkademik);
		final String semester = getComboStringValue(semesterAbsensi);
		final Integer semesterKe = getComboIntegerValue(searchsemester);
		final String program = getComboStringValue(searchprogram);
		final Fakultas fakultas = getComboObjectValue(searchfakultas, Fakultas.class);
		final Jurusan jurusan = getComboObjectValue(searchjurusan, Jurusan.class);
		final Dosen dosen = searchDosen == null ? null : (Dosen) searchDosen.getAttribute("dosen");
		final Mahasiswa mahasiswa = searchMahasiswa == null ? null : (Mahasiswa) searchMahasiswa.getAttribute("mahasiswa");
		final Tbmuser currentUser = Common.getCurrentUser();

		if (tahun == null || semester == null) {
			showSimpleMessage("Mohon maaf, data dashboard belum dapat ditampilkan karena Tahun Akademik dan Jenis Semester belum dipilih. Langkah yang dapat dilakukan: (1) klik dropdown Tahun Akademik dan pilih tahun yang sesuai; (2) klik dropdown Jenis Semester dan pilih jenis semester; (3) klik tombol Muat Data untuk menampilkan dashboard. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.");
			return;
		}

		final Desktop desktop = Executions.getCurrent() == null ? null : Executions.getCurrent().getDesktop();
		tampilkanLoadingDashboard("Menyiapkan filter dan struktur dashboard e-Learning...", 5);

		final String cacheKey = dashCacheKey(tahun, semester, semesterKe, program, fakultas, jurusan, dosen, mahasiswa);
		Long cacheExp = DASH_EXPIRY.get(cacheKey);
		if (cacheExp != null && cacheExp > System.currentTimeMillis() && DASH_CACHE.containsKey(cacheKey)) {
			// Jalur cache masih berada di event thread ZK, sehingga UI dapat dirender
			// langsung. Executions.schedule di sini tidak sah bila server push belum aktif.
			renderDashboardDataNow(currentUser, DASH_CACHE.get(cacheKey));
			return;
		}

		// Executions.schedule() hanya sah ketika server-push aktif. Helper ini
		// menyalakannya sebelum worker dimulai, memakai pool berbatas, lalu melepas
		// reference push di finally sehingga tidak bocor setelah dashboard selesai.
		ais.common.AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() {
			@Override
			public void run() {
				Session session = null;
				try {
					scheduleLoading(desktop, "Menghitung jumlah perkuliahan sesuai filter...", 15);
					session = HibernateUtil.openSession();

					Number totalNumber = (Number) session.createSQLQuery("select count(b.id) as jumlah "
							+ DashboardRekapPertemuanPerkuliahan.generateWhereCount(tahun, semesterKe, semester, dosen,
									program, jurusan, fakultas, mahasiswa)).uniqueResult();
					final int totalPerkuliahan = totalNumber == null ? 0 : totalNumber.intValue();
					final int limit = Math.max(totalPerkuliahan, 1);

					if (totalPerkuliahan <= 0) {
						final DashboardData emptyData = new DashboardData();
						emptyData.filterCaption = buildFilterCaption(tahun, semester, semesterKe, program, fakultas, jurusan, dosen,
								mahasiswa);
						renderDashboardDataOnUi(desktop, currentUser, emptyData);
						return;
					}

					scheduleLoading(desktop, "Mengambil daftar mata kuliah/perkuliahan dari query rekap pertemuan...", 25);
					String sql = "select b.id, "
							+ "max(c.kode||'-'||c.nama||' '||b.semester||' '||b.kelas||' '||(case when dp.nama is null then '' else ' Dsn:'||dp.nama end)||' '||(case when dp2.nama is null then '' else ', '||dp2.nama end)) as info, "
							+ "count(*) as qty_pertemuan, "
							+ "sum(case when a.catatan is not null and a.catatan != '' then 1 else 0 end) qty_catatan, "
							+ "max(uj.qty) as qty_ujian, max(dis.qty) as qty_diskusi, "
							+ "sum(case when a.judultugas is not null and a.judultugas != '' then 1 else 0 end) qty_tugas,max(tk.qty) as qty_tugas_kelompok,max(pk.qty) as qty_pengumuman,max(pi.qty) as qty_ref "
							+ DashboardRekapPertemuanPerkuliahan.generateWhere(tahun, semesterKe, semester, dosen, program,
									jurusan, fakultas, mahasiswa, 0, limit, true);

					@SuppressWarnings("unchecked")
					final List<Object[]> rows = session.createSQLQuery(sql).list();
					final DashboardData dashboardData = new DashboardData();
					dashboardData.totalPerkuliahanFilter = totalPerkuliahan;
					dashboardData.totalPerkuliahanDimuat = rows == null ? 0 : rows.size();
					dashboardData.sampleLimit = totalPerkuliahan;
					dashboardData.filterCaption = buildFilterCaption(tahun, semester, semesterKe, program, fakultas, jurusan, dosen,
							mahasiswa);

					if (rows == null || rows.isEmpty()) {
						renderDashboardDataOnUi(desktop, currentUser, dashboardData);
						return;
					}

					FastAggregateData fastAggregateData = loadFastAggregateData(rows);
					processDashboardRows(desktop, rows, dashboardData, fastAggregateData);
					purgeExpiredDashCache();
					DASH_CACHE.put(cacheKey, dashboardData);
					DASH_EXPIRY.put(cacheKey, System.currentTimeMillis() + DASH_CACHE_TTL_MS);
					renderDashboardDataOnUi(desktop, currentUser, dashboardData);
				} catch (final Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:499");
					renderErrorOnUi(desktop, e);
				} finally {
					if (session != null) {
						try {
							session.clear();
							session.disconnect();
						} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:506");
						}
						closeHibernateSessionQuietly(session);
					}
				}
			}
		});
	}

	private void processDashboardRows(final Desktop desktop, final List<Object[]> rows, final DashboardData dashboardData,
			final FastAggregateData fastAggregateData) throws Exception {
		final int size = rows.size();
		final MatkulDashboardStat[] stats = new MatkulDashboardStat[size];
		final AtomicInteger processed = new AtomicInteger(0);
		final ConcurrentHashMap<String, Long> pertemuanIdMap = new ConcurrentHashMap<String, Long>();
		int worker = Math.min(MAX_WORKER, Math.max(1, size));
		ExecutorService executor = Executors.newFixedThreadPool(worker);

		for (int i = 0; i < size; i++) {
			final int index = i;
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						stats[index] = buildMatkulStatFromRekapRow(rows.get(index), pertemuanIdMap, fastAggregateData);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:532");
					} finally {
						int current = processed.incrementAndGet();
						if (current == 1 || current % 5 == 0 || current == size) {
							int percent = 25 + (int) ((current * 55.0) / Math.max(size, 1));
							scheduleLoading(desktop, "Menganalisis " + current + " dari " + size
									+ " perkuliahan: pertemuan, tugas, materi, video, audio, online, nilai, RPS, kehadiran, dan dokumen...",
									percent);
						}
					}
				}
			});
		}

		executor.shutdown();
		executor.awaitTermination(2, TimeUnit.HOURS);

		scheduleLoading(desktop, "Menggabungkan statistik dan menyiapkan visualisasi modern...", 85);
		for (MatkulDashboardStat stat : stats) {
			if (stat == null) {
				continue;
			}
			dashboardData.matkulStats.add(stat);
			dashboardData.totalPertemuan += stat.qtyPertemuan;
			dashboardData.totalCatatan += stat.qtyCatatan;
			dashboardData.totalTugas += stat.qtyTugas;
			dashboardData.totalUjian += stat.qtyUjian;
			dashboardData.totalDiskusi += stat.qtyDiskusi;
			dashboardData.totalMateri += stat.qtyMateri;
			dashboardData.totalAudio += stat.qtyAudio;
			dashboardData.totalVideo += stat.qtyVideo;
			dashboardData.totalTugasKelompok += stat.qtyTugasKelompok;
			dashboardData.totalPengumuman += stat.qtyPengumuman;
			dashboardData.totalBukuReferensi += stat.qtyBukuReferensi;
			dashboardData.totalPertemuanOnline += stat.qtyPertemuanOnline;
			dashboardData.totalOnlineUser += stat.qtyTotalOnline;
			dashboardData.totalDinilai += stat.qtyDinilai;
			dashboardData.totalBelumDinilai += stat.qtyBelumDinilai;
			dashboardData.totalDokumenUpload += stat.qtyDokumenUpload;
			dashboardData.totalDokumenTarget += stat.qtyDokumenTarget;
			dashboardData.totalRpsMahasiswa += stat.qtyRpsMahasiswa;
			dashboardData.totalRpsAdmin += stat.qtyRpsAdmin;
			mergeStatusMap(dashboardData.kehadiranDosen, stat.kehadiranDosen);
			mergeStatusMap(dashboardData.kehadiranMahasiswa, stat.kehadiranMahasiswa);
			mergeStatusMap(dashboardData.kehadiranSiswa, stat.kehadiranSiswa);
			mergeStatusMap(dashboardData.kehadiranGuru, stat.kehadiranGuru);

			ProdiDashboardStat prodiStat = dashboardData.prodiStats.get(stat.prodi);
			if (prodiStat == null) {
				prodiStat = new ProdiDashboardStat(stat.prodi);
				dashboardData.prodiStats.put(stat.prodi, prodiStat);
			}
			prodiStat.add(stat);

			for (String jenis : stat.dokumenPerJenis.keySet()) {
				Integer jumlah = dashboardData.dokumenPerJenis.get(jenis);
				dashboardData.dokumenPerJenis.put(jenis,
						Integer.valueOf((jumlah == null ? 0 : jumlah.intValue()) + stat.dokumenPerJenis.get(jenis).intValue()));
			}
		}
		pertemuansa = new TreeMap<String, Long>(pertemuanIdMap);
	}

	private MatkulDashboardStat buildMatkulStatFromRekapRow(Object[] objects,
			ConcurrentHashMap<String, Long> pertemuanIdMap, FastAggregateData fastAggregateData) throws Exception {
		if (objects == null || objects.length == 0 || objects[0] == null) {
			return null;
		}
		Long perkuliahanId = toLong(objects[0]);
		if (perkuliahanId == null) {
			return null;
		}

		Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(), perkuliahanId);
		if (perkuliahan == null) {
			return null;
		}

		MatkulDashboardStat stat = new MatkulDashboardStat();
		stat.perkuliahanId = perkuliahanId;
		stat.info = objects.length > 1 && objects[1] != null ? objects[1].toString() : perkuliahan.infoSimple();
		stat.kode = perkuliahan.getMatakuliah() == null ? "" : safeText(perkuliahan.getMatakuliah().getKode());
		stat.matakuliah = perkuliahan.getMatakuliah() == null ? "Tanpa Mata Kuliah" : safeText(perkuliahan.getMatakuliah().getNama());
		stat.kelas = safeText(perkuliahan.getKelas());
		stat.prodi = perkuliahan.getJurusan() == null ? "Tanpa Prodi" : safeText(perkuliahan.getJurusan().getNama());
		stat.dosen = buildDosenNames(perkuliahan);

		stat.qtyPertemuan = toInt(getObject(objects, 2));
		stat.qtyCatatan = toInt(getObject(objects, 3));
		stat.qtyUjian = toInt(getObject(objects, 4));
		stat.qtyDiskusi = toInt(getObject(objects, 5));
		stat.qtyTugas = toInt(getObject(objects, 6));
		stat.qtyTugasKelompok = toInt(getObject(objects, 7));
		stat.qtyPengumuman = toInt(getObject(objects, 8));
		stat.qtyBukuReferensi = toInt(getObject(objects, 9));

		int jumlahMahasiswa = safeInt(perkuliahan.ambilJumlahDetailperkuliahan());
		TreeMap<String, Long> pertemuans = perkuliahan.ambilPertemuan();
		List<Dosen> dosens = perkuliahan.populateDosenBuNama();

		FastPerkuliahanAggregate fast = fastAggregateData == null ? null : fastAggregateData.byPerkuliahan.get(perkuliahanId);
		if (fast != null) {
			if (fast.pertemuanIds != null) {
				for (Long pertemuanId : fast.pertemuanIds) {
					if (pertemuanId != null) {
						pertemuanIdMap.put(pertemuanId.toString(), pertemuanId);
					}
				}
			}
			stat.qtyMateri = fast.qtyMateri;
			stat.qtyAudio = fast.qtyAudio;
			stat.qtyVideo = fast.qtyVideo;
			stat.qtyDokumenTarget = fast.qtyDokumenTarget;
			stat.qtyDokumenUpload = fast.qtyDokumenUpload;
			stat.dokumenPerJenis.putAll(fast.dokumenPerJenis);
			mergeStatusMap(stat.kehadiranDosen, fast.kehadiranDosen);
			mergeStatusMap(stat.kehadiranMahasiswa, fast.kehadiranMahasiswa);
			mergeStatusMap(stat.kehadiranSiswa, fast.kehadiranSiswa);
			mergeStatusMap(stat.kehadiranGuru, fast.kehadiranGuru);
			int totalPertemuanValidFast = fast.pertemuanIds == null ? 0 : fast.pertemuanIds.size();
			stat.qtyHadirMahasiswa = getStatusCount(stat.kehadiranMahasiswa, "M") + getStatusCount(stat.kehadiranSiswa, "M");
			stat.persenHadirMahasiswa = totalPertemuanValidFast > 0 ? (stat.qtyHadirMahasiswa * 100.0) / totalPertemuanValidFast : 0.0;
		} else {

		int totalPertemuanValid = 0;
		double totalPersenOnlineMahasiswa = 0.0;
		double totalPersenAksesMahasiswa = 0.0;
		String perhitunganRekap = Common
				.getKonfigurasi("perhitungan_rekap_online_dihitung_berdasarkan", "Online Dosen dan Mahasiswa").getNilai();

		for (Long pertemuanId : pertemuans.values()) {
			if (pertemuanId == null) {
				continue;
			}
			pertemuanIdMap.put(pertemuanId.toString(), pertemuanId);
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanId.toString());
			if (pertemuan == null) {
				continue;
			}
			totalPertemuanValid++;

			stat.qtyMateri += safeInt(pertemuan.ambilJumlahPertemuanFileContent());
			stat.qtyAudio += safeInt(pertemuan.ambilJumlahAudioPertemuan());
			stat.qtyVideo += safeInt(pertemuan.ambilJumlahVideoPertemuan());
			stat.qtyUploadTugas += safeInt(pertemuan.ambilJumlahTugasFileContent());
			stat.qtyDiskusiDetail += safeInt(pertemuan.ambilJumlahPertemuanPunyaDiskusi());

			Collection<TugasPertemuan> tugasPertemuans = pertemuan.ambilTugasPertemuanTotal().values();
			if (tugasPertemuans != null) {
				for (TugasPertemuan tugasPertemuan : tugasPertemuans) {
					if (tugasPertemuan != null) {
						stat.qtyUploadTugas += safeInt(tugasPertemuan.ambilJumlahTugasFileContent());
					}
				}
			}

			if (isHadirMenurutMahasiswa(pertemuan, dosens)) {
				stat.qtyHadirMahasiswa++;
			}
			if (isRpsMenurutMahasiswa(pertemuan, dosens)) {
				stat.qtyRpsMahasiswa++;
			}
			if (isRpsMenurutAdmin(pertemuan, dosens)) {
				stat.qtyRpsAdmin++;
			}

			TreeMap<String, String> onlineMahasiswa = pertemuan.ambilData("online", null, "", null, null,
					new String[] { "Mahasiswa" });
			TreeMap<String, String> onlineDosen = pertemuan.ambilData("online", null, "", null, null,
					new String[] { "Dosen" });
			TreeMap<String, String> aksesMahasiswa = pertemuan.ambilData("akses", null, "", null, null,
					new String[] { "Mahasiswa" });
			TreeMap<String, String> aksesDosen = pertemuan.ambilData("akses", null, "", null, null,
					new String[] { "Dosen" });

			int jmlDosenOnline = onlineDosen == null ? 0 : onlineDosen.size();
			int jmlMahasiswaOnline = onlineMahasiswa == null ? 0 : onlineMahasiswa.size();
			int jmlMahasiswaAkses = aksesMahasiswa == null ? 0 : aksesMahasiswa.size();
			int jmlDosenAkses = aksesDosen == null ? 0 : aksesDosen.size();
			double persenOnline = jumlahMahasiswa > 0 ? (jmlMahasiswaOnline * 100.0) / jumlahMahasiswa : 0.0;
			double persenAkses = jumlahMahasiswa > 0 ? (jmlMahasiswaAkses * 100.0) / jumlahMahasiswa : 0.0;

			if (isPertemuanOnlineTerhitung(perhitunganRekap, jmlDosenOnline, jmlMahasiswaOnline, persenOnline)) {
				stat.qtyPertemuanOnline++;
			}
			stat.qtyTotalOnline += (jmlDosenOnline + jmlMahasiswaOnline);
			stat.qtyTotalAkses += (jmlDosenAkses + jmlMahasiswaAkses);
			totalPersenOnlineMahasiswa += persenOnline;
			totalPersenAksesMahasiswa += persenAkses;
			akumulasiKehadiranPertemuan(pertemuan, stat);
		}

		stat.qtyTugas += stat.qtyUploadTugas;
		stat.qtyDiskusi = Math.max(stat.qtyDiskusi, stat.qtyDiskusiDetail);
		stat.persenOnlineMahasiswa = totalPertemuanValid > 0 ? totalPersenOnlineMahasiswa / totalPertemuanValid : 0.0;
		stat.persenAksesMahasiswa = totalPertemuanValid > 0 ? totalPersenAksesMahasiswa / totalPertemuanValid : 0.0;
		stat.persenHadirMahasiswa = totalPertemuanValid > 0 ? (stat.qtyHadirMahasiswa * 100.0) / totalPertemuanValid : 0.0;
		stat.persenRpsMahasiswa = totalPertemuanValid > 0 ? (stat.qtyRpsMahasiswa * 100.0) / totalPertemuanValid : 0.0;
		stat.persenRpsAdmin = totalPertemuanValid > 0 ? (stat.qtyRpsAdmin * 100.0) / totalPertemuanValid : 0.0;
		}

		Integer[] statusKrs = perkuliahan.ambilStatusKrs();
		if (statusKrs != null && statusKrs.length > 3) {
			stat.qtyDinilai = safeInt(statusKrs[2]);
			stat.qtyBelumDinilai = safeInt(statusKrs[3]);
			int totalNilai = stat.qtyDinilai + stat.qtyBelumDinilai;
			stat.persenPenilaian = totalNilai > 0 ? (stat.qtyDinilai * 100.0) / totalNilai : 0.0;
		}

		if (fast == null) {
			loadDokumenPerkuliahan(perkuliahanId, stat);
		}
		return stat;
	}

	private void loadDokumenPerkuliahan(Long perkuliahanId, MatkulDashboardStat stat) {
		List<String> jenisLampiran = buildJenisLampiranDashboard();
		for (String jenis : jenisLampiran) {
			stat.qtyDokumenTarget++;
			try {
				LampiranLain lam = LampiranLain.ambil(perkuliahanId, jenis);
				if (lam != null && lam.getId() != null) {
					stat.qtyDokumenUpload++;
					String labelJenis = DashboardTimelinePertemuan.getLabelLampiranDashboardELearning(jenis);
					Integer qty = stat.dokumenPerJenis.get(labelJenis);
					stat.dokumenPerJenis.put(labelJenis, Integer.valueOf((qty == null ? 0 : qty.intValue()) + 1));
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:760");
			}
		}
	}

	private List<String> buildJenisLampiranDashboard() {
		return DashboardTimelinePertemuan.buildJenisLampiranRekapELearning();
	}


	private void akumulasiKehadiranPertemuan(Pertemuan pertemuan, MatkulDashboardStat stat) {
		if (pertemuan == null || stat == null) {
			return;
		}
		try {
			String absensi = pertemuan.getAbsensi();
			if (absensi == null || absensi.trim().length() == 0) {
				return;
			}
			String[] nilais = absensi.split(";");
			for (int i = 0; i < nilais.length; i++) {
				String nn = nilais[i];
				if (nn == null || nn.trim().length() == 0) {
					continue;
				}
				String lower = nn.toLowerCase();
				String[] split = nn.split(",");
				String kode = split.length > 2 ? split[2] : "-";
				if (lower.endsWith("dosen")) {
					incrementStatus(stat.kehadiranDosen, kode);
				} else if (lower.endsWith("mahasiswa")) {
					incrementStatus(stat.kehadiranMahasiswa, kode);
				} else if (lower.endsWith("siswa")) {
					incrementStatus(stat.kehadiranSiswa, kode);
				} else if (lower.endsWith("guru")) {
					incrementStatus(stat.kehadiranGuru, kode);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:799");
		}
	}

	private static void incrementStatus(TreeMap<String, Integer> map, String kode) {
		if (map == null) {
			return;
		}
		String clean = normalizeStatusKode(kode);
		Integer qty = map.get(clean);
		map.put(clean, Integer.valueOf((qty == null ? 0 : qty.intValue()) + 1));
	}

	private static void mergeStatusMap(TreeMap<String, Integer> target, TreeMap<String, Integer> source) {
		if (target == null || source == null) {
			return;
		}
		for (String kode : source.keySet()) {
			Integer src = source.get(kode);
			Integer dst = target.get(kode);
			target.put(kode, Integer.valueOf((dst == null ? 0 : dst.intValue()) + (src == null ? 0 : src.intValue())));
		}
	}

	private static TreeMap<String, Integer> createStatusMap() {
		TreeMap<String, Integer> map = new TreeMap<String, Integer>();
		String[] codes = getStatusKehadiranOrder();
		for (int i = 0; i < codes.length; i++) {
			map.put(codes[i], Integer.valueOf(0));
		}
		return map;
	}

	private static String[] getStatusKehadiranOrder() {
		return new String[] { "M", "A", "S", "I", "-" };
	}

	private static String normalizeStatusKode(String kode) {
		if (kode == null || kode.trim().length() == 0) {
			return "-";
		}
		String clean = kode.trim().toUpperCase();
		if (clean.length() > 1 && !"-".equals(clean)) {
			clean = clean.substring(0, 1);
		}
		return clean;
	}

	private static String getStatusKehadiranLabel(String kode) {
		String clean = normalizeStatusKode(kode);
		if ("M".equals(clean)) {
			return "Hadir";
		}
		if ("A".equals(clean)) {
			return "Mangkir";
		}
		if ("S".equals(clean)) {
			return "Sakit";
		}
		if ("I".equals(clean)) {
			return "Izin";
		}
		if ("-".equals(clean)) {
			return "Belum Ada Keterangan";
		}
		return clean;
	}

	private static int getStatusCount(TreeMap<String, Integer> map, String kode) {
		if (map == null) {
			return 0;
		}
		Integer qty = map.get(normalizeStatusKode(kode));
		return qty == null ? 0 : qty.intValue();
	}

	private List<MatkulDashboardStat> getTopMatkulByKehadiran(List<MatkulDashboardStat> list, int limit) {
		List<MatkulDashboardStat> copy = new ArrayList<MatkulDashboardStat>();
		if (list != null) {
			copy.addAll(list);
		}
		Collections.sort(copy, new Comparator<MatkulDashboardStat>() {
			@Override
			public int compare(MatkulDashboardStat o1, MatkulDashboardStat o2) {
				return Long.valueOf(totalKehadiranVisible(o2)).compareTo(Long.valueOf(totalKehadiranVisible(o1)));
			}
		});
		for (int i = copy.size() - 1; i >= 0; i--) {
			if (totalKehadiranVisible(copy.get(i)) <= 0) {
				copy.remove(i);
			}
		}
		if (copy.size() > limit) {
			return new ArrayList<MatkulDashboardStat>(copy.subList(0, limit));
		}
		return copy;
	}


	private FastAggregateData loadFastAggregateData(List<Object[]> rows) {
		FastAggregateData aggregate = new FastAggregateData();
		List<Long> perkuliahanIds = new ArrayList<Long>();
		if (rows == null || rows.isEmpty()) {
			return aggregate;
		}
		Set<Long> seen = new HashSet<Long>();
		for (Object[] row : rows) {
			Long id = toLong(getObject(row, 0));
			if (id != null && !seen.contains(id)) {
				seen.add(id);
				perkuliahanIds.add(id);
				aggregate.byPerkuliahan.put(id, new FastPerkuliahanAggregate());
			}
		}
		if (perkuliahanIds.isEmpty()) {
			return aggregate;
		}
		List<List<Long>> chunks = splitLongChunks(perkuliahanIds, 1000);
		loadPertemuanIdsFast(aggregate, chunks);
		loadSimpleCountFast(aggregate, chunks, "pertemuan_file_content", "qtyMateri");
		loadSimpleCountFast(aggregate, chunks, "video_pertemuan", "qtyVideo");
		loadSimpleCountFast(aggregate, chunks, "audio_pertemuan", "qtyAudio");
		loadDokumenFast(aggregate, chunks);
		loadKehadiranFast(aggregate, chunks);
		return aggregate;
	}

	private List<List<Long>> splitLongChunks(List<Long> ids, int chunkSize) {
		List<List<Long>> chunks = new ArrayList<List<Long>>();
		if (ids == null || ids.isEmpty()) {
			return chunks;
		}
		int size = chunkSize <= 0 ? 1000 : chunkSize;
		for (int i = 0; i < ids.size(); i += size) {
			chunks.add(new ArrayList<Long>(ids.subList(i, Math.min(ids.size(), i + size))));
		}
		return chunks;
	}

	private String buildInLongSql(List<Long> ids) {
		StringBuilder sb = new StringBuilder();
		for (Long id : ids) {
			if (id == null) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(id.longValue());
		}
		return sb.length() == 0 ? "-1" : sb.toString();
	}

	private void loadPertemuanIdsFast(FastAggregateData aggregate, List<List<Long>> chunks) {
		for (List<Long> chunk : chunks) {
			try {
				String sql = "select id, perkuliahan from pertemuan where perkuliahan in (" + buildInLongSql(chunk)
						+ ") and perkuliahan is not null";
				@SuppressWarnings("unchecked")
				List<Object[]> data = Common.ambilSql(sql);
				if (data == null) {
					continue;
				}
				for (Object[] row : data) {
					Long pertemuanId = toLong(getObject(row, 0));
					Long perkuliahanId = toLong(getObject(row, 1));
					FastPerkuliahanAggregate fast = aggregate.byPerkuliahan.get(perkuliahanId);
					if (fast != null && pertemuanId != null) {
						fast.pertemuanIds.add(pertemuanId);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:971");
			}
		}
	}

	private boolean isStreamingFileTable(String tableName) {
		if (tableName == null) {
			return false;
		}
		String t = tableName.trim().toLowerCase();
		return "lampiran_lain".equals(t) || "pertemuan_file_content".equals(t) || "video_pertemuan".equals(t)
				|| "audio_pertemuan".equals(t) || "tugas_file_content".equals(t);
	}

	@SuppressWarnings("unchecked")
	private List<Object[]> ambilSqlDashboard(String sql, String tableName) {
		if (isStreamingFileTable(tableName)) {
			return Common.ambilSqlStreaming(sql);
		}
		return Common.ambilSql(sql);
	}

	private void loadSimpleCountFast(FastAggregateData aggregate, List<List<Long>> chunks, String tableName, String fieldName) {
		if (isStreamingFileTable(tableName)) {
			loadSimpleCountFastFromStreamingFileDb(aggregate, chunks, tableName, fieldName);
			return;
		}
		for (List<Long> chunk : chunks) {
			try {
				String sql = "select p.perkuliahan, count(x.id) from pertemuan p inner join " + tableName
						+ " x on (x.pertemuan = p.id) where p.perkuliahan in (" + buildInLongSql(chunk)
						+ ") group by p.perkuliahan";
				@SuppressWarnings("unchecked")
				List<Object[]> data = Common.ambilSql(sql);
				applySimpleCountRows(aggregate, data, fieldName);
			} catch (Exception e) {
				// Jika query gagal, dashboard tetap berjalan dengan data agregat lain.
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:1008");
			}
		}
	}

	private void loadSimpleCountFastFromStreamingFileDb(FastAggregateData aggregate, List<List<Long>> chunks,
			String tableName, String fieldName) {
		for (List<Long> chunk : chunks) {
			try {
				Map<Long, Long> pertemuanToPerkuliahan = buildPertemuanToPerkuliahanMap(aggregate, chunk);
				if (pertemuanToPerkuliahan.isEmpty()) {
					continue;
				}
				List<Long> semuaPertemuanIds = new ArrayList<Long>(pertemuanToPerkuliahan.keySet());
				List<List<Long>> pertemuanChunks = splitLongChunks(semuaPertemuanIds, 1000);
				for (List<Long> pertemuanChunk : pertemuanChunks) {
					String sql = "select pertemuan, count(id) from " + tableName + " where pertemuan in ("
							+ buildInLongSql(pertemuanChunk) + ") group by pertemuan";
					@SuppressWarnings("unchecked")
					List<Object[]> data = Common.ambilSqlStreaming(sql);
					if (data == null) {
						continue;
					}
					List<Object[]> rowsByPerkuliahan = new ArrayList<Object[]>();
					for (Object[] row : data) {
						Long pertemuanId = toLong(getObject(row, 0));
						Long perkuliahanId = pertemuanToPerkuliahan.get(pertemuanId);
						if (perkuliahanId == null) {
							continue;
						}
						rowsByPerkuliahan.add(new Object[] { perkuliahanId, getObject(row, 1) });
					}
					applySimpleCountRows(aggregate, rowsByPerkuliahan, fieldName);
				}
			} catch (Exception e) {
				// Tabel file/blob berada di koneksi streaming dan tidak memiliki tabel pertemuan.
				// Karena itu query streaming tidak boleh join ke pertemuan; jika tetap gagal,
				// dashboard tetap berjalan dengan agregat lain.
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:1046");
			}
		}
	}

	private Map<Long, Long> buildPertemuanToPerkuliahanMap(FastAggregateData aggregate, List<Long> perkuliahanIds) {
		Map<Long, Long> map = new HashMap<Long, Long>();
		if (aggregate == null || perkuliahanIds == null || perkuliahanIds.isEmpty()) {
			return map;
		}
		for (Long perkuliahanId : perkuliahanIds) {
			if (perkuliahanId == null) {
				continue;
			}
			FastPerkuliahanAggregate fast = aggregate.byPerkuliahan.get(perkuliahanId);
			if (fast == null || fast.pertemuanIds == null || fast.pertemuanIds.isEmpty()) {
				continue;
			}
			for (Long pertemuanId : fast.pertemuanIds) {
				if (pertemuanId != null) {
					map.put(pertemuanId, perkuliahanId);
				}
			}
		}
		return map;
	}

	private void applySimpleCountRows(FastAggregateData aggregate, List<Object[]> data, String fieldName) {
		if (data == null) {
			return;
		}
		for (Object[] row : data) {
			Long perkuliahanId = toLong(getObject(row, 0));
			int jumlah = toInt(getObject(row, 1));
			FastPerkuliahanAggregate fast = aggregate.byPerkuliahan.get(perkuliahanId);
			if (fast == null) {
				continue;
			}
			if ("qtyMateri".equals(fieldName)) {
				fast.qtyMateri += jumlah;
			} else if ("qtyVideo".equals(fieldName)) {
				fast.qtyVideo += jumlah;
			} else if ("qtyAudio".equals(fieldName)) {
				fast.qtyAudio += jumlah;
			}
		}
	}

	private void loadDokumenFast(final FastAggregateData aggregate, List<List<Long>> chunks) {
		final List<String> jenisLampiran = buildJenisLampiranDashboard();
		if (jenisLampiran == null || jenisLampiran.isEmpty()) {
			return;
		}
		for (FastPerkuliahanAggregate fast : aggregate.byPerkuliahan.values()) {
			fast.qtyDokumenTarget = jenisLampiran.size();
		}
		String inJenis = buildInStringSql(jenisLampiran);
		for (List<Long> chunk : chunks) {
			try {
				String sql = "select ref, nama, count(id) from lampiran_lain where ref in (" + buildInLongSql(chunk)
						+ ") and nama in (" + inJenis + ") group by ref, nama";
				@SuppressWarnings("unchecked")
				List<Object[]> data = Common.ambilSqlStreaming(sql);
				if (data == null) {
					continue;
				}
				for (Object[] row : data) {
					Long perkuliahanId = toLong(getObject(row, 0));
					String jenis = getObject(row, 1) == null ? "" : getObject(row, 1).toString();
					int jumlah = toInt(getObject(row, 2));
					if (jumlah <= 0) {
						continue;
					}
					FastPerkuliahanAggregate fast = aggregate.byPerkuliahan.get(perkuliahanId);
					if (fast == null) {
						continue;
					}
					String label = DashboardTimelinePertemuan.getLabelLampiranDashboardELearning(jenis);
					Integer old = fast.dokumenPerJenis.get(label);
					fast.dokumenPerJenis.put(label, Integer.valueOf((old == null ? 0 : old.intValue()) + jumlah));
					fast.qtyDokumenUpload += jumlah;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:1129");
			}
		}
	}

	private String buildInStringSql(List<String> values) {
		StringBuilder sb = new StringBuilder();
		for (String value : values) {
			if (value == null) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append('\'').append(value.replace("'", "''")).append('\'');
		}
		return sb.length() == 0 ? "''" : sb.toString();
	}

	private void loadKehadiranFast(FastAggregateData aggregate, List<List<Long>> chunks) {
		for (List<Long> chunk : chunks) {
			try {
				String sql = "select p.perkuliahan, "
						+ "case when lower(x.item) like '%mahasiswa' then 'Mahasiswa' "
						+ "when lower(x.item) like '%siswa' then 'Siswa' "
						+ "when lower(x.item) like '%dosen' then 'Dosen' "
						+ "when lower(x.item) like '%guru' then 'Guru' else 'Lainnya' end as role_absen, "
						+ "upper(coalesce(nullif(trim(split_part(x.item, ',', 3)), ''), '-')) as kode, count(*) "
						+ "from pertemuan p cross join lateral regexp_split_to_table(coalesce(p.absensi, ''), ';') as x(item) "
						+ "where p.perkuliahan in (" + buildInLongSql(chunk) + ") and trim(x.item) <> '' "
						+ "group by p.perkuliahan, role_absen, kode";
				@SuppressWarnings("unchecked")
				List<Object[]> data = Common.ambilSql(sql);
				if (data == null) {
					continue;
				}
				for (Object[] row : data) {
					Long perkuliahanId = toLong(getObject(row, 0));
					String role = getObject(row, 1) == null ? "" : getObject(row, 1).toString();
					String kode = normalizeStatusKode(getObject(row, 2) == null ? "-" : getObject(row, 2).toString());
					int jumlah = toInt(getObject(row, 3));
					FastPerkuliahanAggregate fast = aggregate.byPerkuliahan.get(perkuliahanId);
					if (fast == null || jumlah <= 0) {
						continue;
					}
					if ("Dosen".equals(role)) {
						addStatus(fast.kehadiranDosen, kode, jumlah);
					} else if ("Mahasiswa".equals(role)) {
						addStatus(fast.kehadiranMahasiswa, kode, jumlah);
					} else if ("Siswa".equals(role)) {
						addStatus(fast.kehadiranSiswa, kode, jumlah);
					} else if ("Guru".equals(role)) {
						addStatus(fast.kehadiranGuru, kode, jumlah);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:1185");
			}
		}
	}

	private static void addStatus(TreeMap<String, Integer> map, String kode, int jumlah) {
		if (map == null || jumlah == 0) {
			return;
		}
		String clean = normalizeStatusKode(kode);
		Integer old = map.get(clean);
		map.put(clean, Integer.valueOf((old == null ? 0 : old.intValue()) + jumlah));
	}

	private boolean isPertemuanOnlineTerhitung(String mode, int jmlDosenOnline, int jmlMahasiswaOnline,
			double persenOnlineMahasiswa) {
		if ("Online Mahasiswa 15%".equals(mode)) {
			return persenOnlineMahasiswa >= 15.0;
		}
		if ("Online Mahasiswa 25%".equals(mode)) {
			return persenOnlineMahasiswa >= 25.0;
		}
		if ("Online Mahasiswa 30%".equals(mode)) {
			return persenOnlineMahasiswa >= 30.0;
		}
		if ("Online Mahasiswa 50%".equals(mode)) {
			return persenOnlineMahasiswa >= 50.0;
		}
		if ("Online Mahasiswa 60%".equals(mode)) {
			return persenOnlineMahasiswa >= 60.0;
		}
		if ("Online Mahasiswa 75%".equals(mode)) {
			return persenOnlineMahasiswa >= 75.0;
		}
		return jmlDosenOnline > 0 || jmlMahasiswaOnline > 0;
	}

	private boolean isHadirMenurutMahasiswa(Pertemuan pertemuan, List<Dosen> dosens) {
		try {
			String[] nilais = pertemuan.getKeteranganKonfirmasi() != null ? pertemuan.getKeteranganKonfirmasi().split(";")
					: new String[0];
			for (String nn : nilais) {
				if (nn == null) {
					continue;
				}
				String lower = nn.toLowerCase();
				if (lower.endsWith("mahasiswa") || lower.endsWith("siswa")) {
					String[] split = nn.split(",");
					Long formatId = split.length > 0 ? parseLongSafe(split[0]) : null;
					for (Dosen dd : dosens) {
						Statusabsensi stAbsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
								pertemuan.retreiveAbsensiIdKonfirmasi(formatId, dd));
						if (stAbsensi != null && stAbsensi.getId() != null && stAbsensi.getId().equals(1L)) {
							return true;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:1244");
		}
		return false;
	}

	private boolean isRpsMenurutMahasiswa(Pertemuan pertemuan, List<Dosen> dosens) {
		try {
			String[] nilais = pertemuan.getKeteranganSesuaiDenganRps() != null
					? pertemuan.getKeteranganSesuaiDenganRps().split(";")
					: new String[0];
			for (String nn : nilais) {
				if (nn == null) {
					continue;
				}
				String lower = nn.toLowerCase();
				if (lower.endsWith("mahasiswa") || lower.endsWith("siswa")) {
					String[] split = nn.split(",");
					Long formatId = split.length > 0 ? parseLongSafe(split[0]) : null;
					for (Dosen dd : dosens) {
						Long status = pertemuan.retreiveAbsensiIdKonfirmasiRps(formatId, dd);
						if (status != null && status.equals(1L)) {
							return true;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:1271");
		}
		return false;
	}

	private boolean isRpsMenurutAdmin(Pertemuan pertemuan, List<Dosen> dosens) {
		try {
			if (pertemuan.getPerkuliahan() != null && pertemuan.getPerkuliahan().getSemuaNilaiSesuaiRps().equals(1L)
					&& pertemuan.getPerkuliahan().getSemuaPertemuanSesuaiRps()) {
				return true;
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:1282");
		}
		try {
			String[] nilais = pertemuan.getKeteranganSesuaiOlehAkademik() != null
					? pertemuan.getKeteranganSesuaiOlehAkademik().split(";")
					: new String[0];
			for (String nn : nilais) {
				if (nn == null) {
					continue;
				}
				if (nn.toLowerCase().endsWith("admin")) {
					String[] split = nn.split(",");
					Long formatId = split.length > 0 ? parseLongSafe(split[0]) : null;
					for (Dosen dd : dosens) {
						Long status = pertemuan.retreiveAbsensiIdKonfirmasiRps(formatId, dd);
						if (status != null && status.equals(1L)) {
							return true;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:1304");
		}
		return false;
	}

	private void renderDashboardDataOnUi(Desktop desktop, final Tbmuser currentUser, final DashboardData data) {
		if (desktop == null || !desktop.isAlive() || !desktop.isServerPushEnabled()) {
			return;
		}
		Executions.schedule(desktop, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				renderDashboardDataNow(currentUser, data);
			}
		}, new Event("onRenderElearningDashboard"));
	}

	private void renderDashboardDataNow(Tbmuser currentUser, DashboardData data) {
		org.zkoss.zul.Div body = getDashboardBodyContainer();
		lastDashboardData = data;
		pageGrid = 0;
		Common.clear(body);
		DashboardUiUtil.injectGlobalCss(body);
		summaryContainer = new org.zkoss.zul.Div();
		summaryContainer.setWidth("100%");
		summaryContainer.setStyle("margin-bottom:12px;");
		summaryContainer.setParent(body);
		if (currentUser != null && pertemuansa != null && !pertemuansa.isEmpty()) {
			Component summary = DashboardTimelinePertemuan.buildDashboardSummary(currentUser, true, pertemuansa);
			if (summary != null) summaryContainer.appendChild(summary);
		}
		dashboardVisualContainer = new org.zkoss.zul.Div();
		dashboardVisualContainer.setWidth("100%");
		dashboardVisualContainer.setParent(body);
		dashboardVisualContainer.appendChild(new Html(buildDashboardHtml(data)));
		renderGridRekap(data);
	}

	private void renderGridRekap(DashboardData data) {
		gridRekapContainer = new org.zkoss.zul.Div();
		gridRekapContainer.setStyle("margin-top:12px;");
		gridRekapContainer.setParent(getDashboardBodyContainer());
		renderGridRekapPage(data);
	}

	private void renderGridRekapPage(DashboardData data) {
		if (gridRekapContainer == null) return;
		Common.clear(gridRekapContainer);
		List<MatkulDashboardStat> allStats = filteredMatkulStats(data);
		int total = allStats.size();
		if (total == 0) {
			gridRekapContainer.appendChild(new Html(buildEmptyHtml("Tidak ada mata kuliah yang cocok dengan pencarian.")));
			return;
		}
		int from = pageGrid * PAGE_SIZE_GRID;
		if (from >= total) { pageGrid = 0; from = 0; }
		int to = Math.min(from + PAGE_SIZE_GRID, total);
		List<MatkulDashboardStat> page = allStats.subList(from, to);
		gridRekapContainer.appendChild(new Html(DashboardUiUtil.pageInfoBar(pageGrid, PAGE_SIZE_GRID, total)));
		Grid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setStyle("border-radius:16px; overflow:hidden; border:1px solid #e5e7eb; background:#ffffff;");
		grid.setParent(gridRekapContainer);
		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(grid);
		columns.appendChild(new org.zkoss.zul.Column("Mata Kuliah", null, "32%"));
		columns.appendChild(new org.zkoss.zul.Column("Prodi", null, "18%"));
		columns.appendChild(new org.zkoss.zul.Column("Pertemuan", null, "10%"));
		columns.appendChild(new org.zkoss.zul.Column("Materi", null, "10%"));
		columns.appendChild(new org.zkoss.zul.Column("Tugas", null, "10%"));
		columns.appendChild(new org.zkoss.zul.Column("Ujian", null, "10%"));
		columns.appendChild(new org.zkoss.zul.Column("Dokumen Upload", null, "10%"));
		Rows rows = new Rows();
		rows.setParent(grid);
		for (MatkulDashboardStat stat : page) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(stat.kode + " - " + stat.matakuliah + (isEmpty(stat.kelas) ? "" : " (" + stat.kelas + ")")));
			row.appendChild(new Label(stat.prodi));
			row.appendChild(new Label(formatNumber(stat.qtyPertemuan)));
			row.appendChild(new Label(formatNumber(stat.qtyMateri)));
			row.appendChild(new Label(formatNumber(stat.qtyTugas)));
			row.appendChild(new Label(formatNumber(stat.qtyUjian)));
			row.appendChild(new Label(formatNumber(stat.qtyDokumenUpload)));
		}
		renderPageNavDash(gridRekapContainer, pageGrid, total);
	}

	private void renderPageNavDash(org.zkoss.zul.Div container, int currentPage, int total) {
		DashboardUiUtil.renderPageNav(container, currentPage, PAGE_SIZE_GRID, total,
			new EventListener() {
				public void onEvent(Event e) throws Exception {
					pageGrid--;
					if (lastDashboardData != null) renderGridRekapPage(lastDashboardData);
				}
			},
			new EventListener() {
				public void onEvent(Event e) throws Exception {
					pageGrid++;
					if (lastDashboardData != null) renderGridRekapPage(lastDashboardData);
				}
			}
		);
	}

	private void tampilkanLoadingDashboard(String message, int percent) {
		if (mainContainer == null) {
			return;
		}
		org.zkoss.zul.Div body = getDashboardBodyContainer();
		Common.clear(body);
		dashboardLoadingHtml = new Html(buildLoadingDashboardHtml(message, percent));
		Vbox container = new Vbox();
		container.setWidth("100%");
		container.setStyle("padding:4px; box-sizing:border-box;");
		container.setParent(body);
		container.appendChild(dashboardLoadingHtml);
	}

	private void scheduleLoading(Desktop desktop, final String message, final int percent) {
		if (desktop == null || !desktop.isAlive() || !desktop.isServerPushEnabled()) {
			return;
		}
		try {
			Executions.schedule(desktop, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (dashboardLoadingHtml != null) {
						dashboardLoadingHtml.setContent(buildLoadingDashboardHtml(message, percent));
					}
				}
			}, new Event("onElearningDashboardProgress"));
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:1437");
		}
	}

	private void renderErrorOnUi(Desktop desktop, final Exception error) {
		if (desktop == null || !desktop.isAlive() || !desktop.isServerPushEnabled()) {
			return;
		}
		try {
			Executions.schedule(desktop, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					org.zkoss.zul.Div body = getDashboardBodyContainer();
					Common.clear(body);
					body.appendChild(new Html(buildErrorHtml(error)));
				}
			}, new Event("onElearningDashboardError"));
		} catch (IllegalStateException e) {
			// Desktop dapat ditutup atau server-push dinonaktifkan setelah pemeriksaan di atas.
			// Ini lifecycle normal saat pengguna pindah halaman; tidak perlu menjadi error sistem.
		}
	}

	private void showSimpleMessage(String message) {
		if (mainContainer == null) {
			return;
		}
		org.zkoss.zul.Div body = getDashboardBodyContainer();
		Common.clear(body);
		body.appendChild(new Html(buildEmptyHtml(message)));
	}

	private String buildDashboardHtml(DashboardData data) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"font-family:Arial,sans-serif; color:#0f172a;\">");
		sb.append(buildHeroHtml(data));
		if (data == null || data.matkulStats.isEmpty()) {
			sb.append(buildEmptyHtml("Data tidak ditemukan untuk filter ini."));
			sb.append("</div>");
			return sb.toString();
		}
		sb.append(buildMetricCardsHtml(data));
		sb.append(buildInsightHtml(data));
		sb.append(buildKehadiranGlobalHtml(data));
		sb.append(buildKehadiranPerMatkulHtml(data));
		sb.append("<div style=\"display:flex; flex-wrap:wrap; gap:12px; align-items:stretch;\">");
		sb.append(buildBarPanelHtml("Aktivitas per Program Studi", "Menampilkan perbandingan pertemuan, materi, tugas, ujian, dan dokumen per prodi.", buildProdiBars(data)));
		sb.append(buildBarPanelHtml("Top Mata Kuliah Paling Aktif", "Urutan mata kuliah berdasarkan total aktivitas e-Learning.", buildMatkulBars(data, "aktivitas")));
		sb.append(buildBarPanelHtml("Distribusi Materi Pembelajaran", "Jumlah file/materi yang diunggah pada tiap mata kuliah.", buildMatkulBars(data, "materi")));
		sb.append(buildBarPanelHtml("Distribusi Tugas dan Ujian", "Akumulasi tugas, upload tugas, dan ujian sesuai pola rekap pertemuan.", buildMatkulBars(data, "tugasUjian")));
		sb.append(buildBarPanelHtml("Audio, Video, dan Diskusi", "Kanal pembelajaran sinkron/asinkron yang digunakan dosen dan mahasiswa.", buildMatkulBars(data, "media")));
		sb.append(buildBarPanelHtml("Total Kehadiran per Mata Kuliah", "Akumulasi status M, A, S, I, dan - sesuai mode aktif: PT menampilkan Dosen/Mahasiswa, Sekolah menampilkan Siswa/Guru.", buildMatkulBars(data, "kehadiran")));
		sb.append(buildBarPanelHtml("Kelengkapan Dokumen Perkuliahan", "Jumlah dokumen yang benar-benar sudah diupload berdasarkan LampiranLain.getId().", buildMatkulBars(data, "dokumen")));
		sb.append("</div>");
		sb.append(buildDokumenJenisHtml(data));
		sb.append("</div>");
		return sb.toString();
	}

	private String buildHeroHtml(DashboardData data) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"padding:20px; border-radius:20px; background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); color:#fff; box-shadow:0 18px 38px rgba(15,23,42,.16); margin-bottom:12px;\">");
		sb.append("<div style=\"display:flex; justify-content:space-between; gap:16px; flex-wrap:wrap; align-items:flex-start;\">");
		sb.append("<div style=\"max-width:780px;\">");
		sb.append("<div style=\"font-size:11px; letter-spacing:.14em; text-transform:uppercase; font-weight:900; opacity:.88;\">Dasbor Analitik e-Learning</div>");
		sb.append("<div style=\"font-size:25px; font-weight:900; margin-top:8px; line-height:1.25;\">Tren Aktivitas Perkuliahan</div>");
		sb.append("<div style=\"font-size:12px; line-height:1.65; margin-top:8px; opacity:.9;\">Menggunakan pola pengambilan data dari Rekap Pertemuan Perkuliahan: perkuliahan, pertemuan, file, audio, video, tugas, ujian, diskusi, online, nilai, kesesuaian RPS, dan kelengkapan dokumen.</div>");
		sb.append("</div>");
		sb.append("<div style=\"min-width:210px; padding:12px 14px; border-radius:16px; background:rgba(255,255,255,.14); border:1px solid rgba(255,255,255,.22);\">");
		sb.append("<div style=\"font-size:11px; text-transform:uppercase; letter-spacing:.12em; font-weight:900; opacity:.85;\">Filter Aktif</div>");
		sb.append("<div style=\"font-size:12px; line-height:1.55; margin-top:6px;\">").append(escapeHtml(data.filterCaption)).append("</div>");
		sb.append("</div>");
		sb.append("</div>");
		sb.append("</div>");
		return sb.toString();
	}

	private String buildMetricCardsHtml(DashboardData data) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"display:flex; flex-wrap:wrap; gap:10px; margin-bottom:12px;\">");
		appendMetric(sb, "Perkuliahan", data.totalPerkuliahanDimuat, "Mata kuliah dimuat", "#2563eb");
		appendMetric(sb, "Pertemuan", data.totalPertemuan, "Total pertemuan", "#0891b2");
		appendMetric(sb, "Materi", data.totalMateri, "File materi", "#7c3aed");
		appendMetric(sb, "Tugas", data.totalTugas, "Tugas + upload", "#ea580c");
		appendMetric(sb, "Ujian", data.totalUjian, "Ujian terdaftar", "#dc2626");
		appendMetric(sb, "Diskusi", data.totalDiskusi, "Forum/diskusi", "#16a34a");
		appendMetric(sb, "Audio", data.totalAudio, "Audio pembelajaran", "#0d9488");
		appendMetric(sb, "Video", data.totalVideo, "Video pembelajaran", "#4f46e5");
		if (data.totalDokumenUpload > 0) {
			appendMetric(sb, "Dokumen Upload", data.totalDokumenUpload, "RPS/SAP/dll", "#9333ea");
		}
		appendMetric(sb, "Online", data.totalPertemuanOnline, "Pertemuan online", "#0284c7");
		boolean pt = isPerguruanTinggiMode();
		boolean ya = isSekolahMode();
		if (pt && sumStatusMap(data.kehadiranDosen) > 0) {
			appendMetric(sb, "Hdr. Dosen", sumStatusMap(data.kehadiranDosen), "Rekap M/A/S/I/-", "#2563eb");
		}
		if (pt && sumStatusMap(data.kehadiranMahasiswa) > 0) {
			appendMetric(sb, "Hdr. Mahasiswa", sumStatusMap(data.kehadiranMahasiswa), "Rekap M/A/S/I/-", "#16a34a");
		}
		if (ya && sumStatusMap(data.kehadiranSiswa) > 0) {
			appendMetric(sb, "Hdr. Siswa", sumStatusMap(data.kehadiranSiswa), "Rekap M/A/S/I/-", "#0d9488");
		}
		if (ya && sumStatusMap(data.kehadiranGuru) > 0) {
			appendMetric(sb, "Hdr. Guru", sumStatusMap(data.kehadiranGuru), "Rekap M/A/S/I/-", "#7c3aed");
		}
		appendMetric(sb, "Dinilai", data.totalDinilai, "KRS sudah dinilai", "#15803d");
		appendMetric(sb, "Belum Dinilai", data.totalBelumDinilai, "Butuh tindak lanjut", "#b45309");
		sb.append("</div>");
		return sb.toString();
	}

	private void appendMetric(StringBuilder sb, String title, long value, String subtitle, String color) {
		sb.append("<div style=\"flex:1 1 145px; min-width:145px; padding:14px; border-radius:18px; background:#ffffff; border:1px solid #e5e7eb; box-shadow:0 12px 28px rgba(15,23,42,.07);\">");
		sb.append("<div style=\"height:6px; width:44px; border-radius:999px; background:").append(color).append("; margin-bottom:10px;\"></div>");
		sb.append("<div style=\"font-size:11px; color:#64748b; font-weight:900; text-transform:uppercase; letter-spacing:.08em;\">").append(escapeHtml(title)).append("</div>");
		sb.append("<div style=\"font-size:24px; font-weight:900; margin-top:4px; color:#0f172a;\">").append(formatNumber(value)).append("</div>");
		sb.append("<div style=\"font-size:11px; color:#64748b; margin-top:4px;\">").append(escapeHtml(subtitle)).append("</div>");
		sb.append("</div>");
	}

	private String buildInsightHtml(DashboardData data) {
		double persenDokumen = data.totalDokumenTarget > 0 ? (data.totalDokumenUpload * 100.0) / data.totalDokumenTarget : 0.0;
		double persenNilai = (data.totalDinilai + data.totalBelumDinilai) > 0
				? (data.totalDinilai * 100.0) / (data.totalDinilai + data.totalBelumDinilai)
				: 0.0;
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"display:flex; flex-wrap:wrap; gap:10px; margin-bottom:12px;\">");
		if (data.totalDokumenUpload > 0) {
			sb.append(buildGaugeCard("Kelengkapan Dokumen", persenDokumen, data.totalDokumenUpload + " / " + data.totalDokumenTarget + " dokumen terupload"));
		}
		sb.append(buildGaugeCard("Progress Penilaian", persenNilai, data.totalDinilai + " dinilai, " + data.totalBelumDinilai + " belum"));
		sb.append(buildGaugeCard("RPS Menurut Mahasiswa", percent(data.totalRpsMahasiswa, data.totalPertemuan), data.totalRpsMahasiswa + " dari " + data.totalPertemuan + " pertemuan"));
		sb.append(buildGaugeCard("RPS Menurut Admin/Mutu", percent(data.totalRpsAdmin, data.totalPertemuan), data.totalRpsAdmin + " dari " + data.totalPertemuan + " pertemuan"));
		sb.append("</div>");
		return sb.toString();
	}

	private String buildGaugeCard(String title, double percent, String note) {
		double p = clamp(percent);
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"flex:1 1 235px; min-width:235px; background:#fff; border:1px solid #e5e7eb; border-radius:18px; padding:14px; box-shadow:0 12px 28px rgba(15,23,42,.06);\">");
		sb.append("<div style=\"display:flex; justify-content:space-between; gap:10px; align-items:center;\">");
		sb.append("<div style=\"font-size:12px; font-weight:900; color:#0f172a;\">").append(escapeHtml(title)).append("</div>");
		sb.append("<div style=\"font-size:17px; font-weight:900; color:#2563eb;\">").append(formatDecimal(p)).append("%</div>");
		sb.append("</div><div style=\"height:10px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:10px;\"><div style=\"height:10px; width:").append(formatDecimal(p)).append("%; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4)); border-radius:999px;\"></div></div>");
		sb.append("<div style=\"font-size:11px; color:#64748b; margin-top:8px;\">").append(escapeHtml(note)).append("</div></div>");
		return sb.toString();
	}

	private String buildBarPanelHtml(String title, String subtitle, List<DashboardBar> bars) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"flex:1 1 470px; min-width:300px; background:#fff; border:1px solid #e5e7eb; border-radius:20px; padding:16px; box-shadow:0 14px 30px rgba(15,23,42,.07); box-sizing:border-box;\">");
		sb.append("<div style=\"font-size:16px; font-weight:900; color:#0f172a;\">").append(escapeHtml(title)).append("</div>");
		sb.append("<div style=\"font-size:11px; color:#64748b; margin-top:4px; line-height:1.5;\">").append(escapeHtml(subtitle)).append("</div>");
		if (bars == null || bars.isEmpty()) {
			sb.append("<div style=\"padding:18px; color:#64748b; font-size:12px;\">Tidak ada data.</div>");
		} else {
			long max = 0;
			for (DashboardBar b : bars) {
				if (b.value > max) {
					max = b.value;
				}
			}
			for (DashboardBar b : bars) {
				double width = max > 0 ? (b.value * 100.0) / max : 0.0;
				sb.append("<div style=\"margin-top:12px;\">");
				sb.append("<div style=\"display:flex; justify-content:space-between; gap:8px; font-size:11px; color:#334155;\"><span style=\"font-weight:800; max-width:75%; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;\">")
						.append(escapeHtml(b.label)).append("</span><span style=\"font-weight:900; color:#0f172a;\">").append(formatNumber(b.value)).append("</span></div>");
				sb.append("<div style=\"height:12px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:5px;\"><div style=\"height:12px; width:")
						.append(formatDecimal(width)).append("%; background:linear-gradient(90deg,").append(b.color)
						.append(",#06b6d4); border-radius:999px;\"></div></div>");
				sb.append("</div>");
			}
		}
		sb.append("</div>");
		return sb.toString();
	}


	private boolean isPerguruanTinggiMode() {
		try {
			boolean[] ptYa = Common.chekPtAtauSekolah();
			return ptYa != null && ptYa.length > 0 && ptYa[0];
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:1617");
			return false;
		}
	}

	private boolean isSekolahMode() {
		try {
			boolean[] ptYa = Common.chekPtAtauSekolah();
			return ptYa != null && ptYa.length > 1 && ptYa[1];
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:1627");
			return false;
		}
	}

	private long totalKehadiranVisible(MatkulDashboardStat stat) {
		if (stat == null) {
			return 0L;
		}
		long total = 0L;
		if (isPerguruanTinggiMode()) {
			total += sumStatusMap(stat.kehadiranDosen);
			total += sumStatusMap(stat.kehadiranMahasiswa);
		}
		if (isSekolahMode()) {
			total += sumStatusMap(stat.kehadiranSiswa);
			total += sumStatusMap(stat.kehadiranGuru);
		}
		return total;
	}

	private String buildKehadiranGlobalHtml(DashboardData data) {
		boolean pt = isPerguruanTinggiMode();
		boolean ya = isSekolahMode();
		boolean tampilDosen = pt && sumStatusMap(data.kehadiranDosen) > 0;
		boolean tampilMahasiswa = pt && sumStatusMap(data.kehadiranMahasiswa) > 0;
		boolean tampilSiswa = ya && sumStatusMap(data.kehadiranSiswa) > 0;
		boolean tampilGuru = ya && sumStatusMap(data.kehadiranGuru) > 0;
		if (!tampilDosen && !tampilMahasiswa && !tampilSiswa && !tampilGuru) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"margin-bottom:12px; background:#fff; border:1px solid #e5e7eb; border-radius:20px; padding:16px; box-shadow:0 14px 30px rgba(15,23,42,.07);\">");
		sb.append("<div style=\"font-size:16px; font-weight:900; color:#0f172a;\">Rekap Kehadiran Global</div>");
		sb.append("<div style=\"font-size:11px; color:#64748b; margin-top:4px;\">M = Hadir, A = Mangkir/Tidak Ada Alasan, S = Sakit, I = Izin, - = Belum Ada Keterangan/Belum Absen.</div>");
		sb.append("<div style=\"display:grid; grid-template-columns:repeat(auto-fit,minmax(230px,1fr)); gap:10px; margin-top:12px;\">");
		if (tampilDosen) {
			sb.append(buildKehadiranRoleBox("Dosen", data.kehadiranDosen, "#2563eb"));
		}
		if (tampilMahasiswa) {
			sb.append(buildKehadiranRoleBox("Mahasiswa", data.kehadiranMahasiswa, "#16a34a"));
		}
		if (tampilSiswa) {
			sb.append(buildKehadiranRoleBox("Siswa", data.kehadiranSiswa, "#0d9488"));
		}
		if (tampilGuru) {
			sb.append(buildKehadiranRoleBox("Guru", data.kehadiranGuru, "#7c3aed"));
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	private String buildKehadiranRoleBox(String role, TreeMap<String, Integer> map, String color) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"border:1px solid #e5e7eb; border-radius:16px; padding:12px; background:#f8fafc;\">");
		sb.append("<div style=\"display:flex; justify-content:space-between; align-items:center; gap:8px;\"><span style=\"font-size:12px; font-weight:900; color:#0f172a;\">").append(escapeHtml(role)).append("</span>");
		sb.append("<span style=\"height:6px; width:42px; border-radius:999px; background:").append(color).append(";\"></span></div>");
		sb.append("<div style=\"display:flex; flex-wrap:wrap; gap:6px; margin-top:10px;\">");
		String[] codes = getStatusKehadiranOrder();
		for (int i = 0; i < codes.length; i++) {
			String kode = codes[i];
			int jumlah = getStatusCount(map, kode);
			sb.append("<span title=\"").append(escapeHtml(getStatusKehadiranLabel(kode))).append("\" style=\"padding:7px 8px; border-radius:12px; background:#fff; border:1px solid #e2e8f0; font-size:11px; font-weight:900; color:#0f172a;\">")
					.append(kode).append(": ").append(formatNumber(jumlah)).append("</span>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	private String buildKehadiranPerMatkulHtml(DashboardData data) {
		boolean pt = isPerguruanTinggiMode();
		boolean ya = isSekolahMode();
		List<MatkulDashboardStat> top = getTopMatkulByKehadiran(data.matkulStats, TOP_BAR_LIMIT);
		if ((!pt && !ya) || top == null || top.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"margin-bottom:12px; background:#fff; border:1px solid #e5e7eb; border-radius:20px; padding:16px; box-shadow:0 14px 30px rgba(15,23,42,.07);\">");
		sb.append("<div style=\"font-size:16px; font-weight:900; color:#0f172a;\">Rekap Kehadiran per Mata Kuliah</div>");
		sb.append("<div style=\"font-size:11px; color:#64748b; margin-top:4px;\">Menampilkan mata kuliah dengan data kehadiran terbanyak pada batas data dashboard.</div>");
		for (MatkulDashboardStat stat : top) {
			sb.append("<div style=\"margin-top:12px; border:1px solid #e5e7eb; border-radius:16px; padding:12px; background:#f8fafc;\">");
			sb.append("<div style=\"font-size:12px; font-weight:900; color:#0f172a;\">").append(escapeHtml(stat.shortTitle())).append("</div>");
			sb.append("<div style=\"font-size:10px; color:#64748b; margin-top:3px;\">").append(escapeHtml(stat.prodi)).append(" | ").append(escapeHtml(stat.kelas)).append("</div>");
			sb.append("<div style=\"display:grid; grid-template-columns:repeat(auto-fit,minmax(210px,1fr)); gap:8px; margin-top:9px;\">");
			if (pt && sumStatusMap(stat.kehadiranDosen) > 0) {
				sb.append(buildKehadiranRoleBox("Dosen", stat.kehadiranDosen, "#2563eb"));
			}
			if (pt && sumStatusMap(stat.kehadiranMahasiswa) > 0) {
				sb.append(buildKehadiranRoleBox("Mahasiswa", stat.kehadiranMahasiswa, "#16a34a"));
			}
			if (ya && sumStatusMap(stat.kehadiranSiswa) > 0) {
				sb.append(buildKehadiranRoleBox("Siswa", stat.kehadiranSiswa, "#0d9488"));
			}
			if (ya && sumStatusMap(stat.kehadiranGuru) > 0) {
				sb.append(buildKehadiranRoleBox("Guru", stat.kehadiranGuru, "#7c3aed"));
			}
			sb.append("</div></div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String buildDokumenJenisHtml(DashboardData data) {
		if (data.dokumenPerJenis == null || data.dokumenPerJenis.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"margin-top:12px; background:#fff; border:1px solid #e5e7eb; border-radius:20px; padding:16px; box-shadow:0 14px 30px rgba(15,23,42,.07);\">");
		sb.append("<div style=\"font-size:16px; font-weight:900; color:#0f172a;\">Dokumen Terupload per Jenis Lampiran</div>");
		sb.append("<div style=\"font-size:11px; color:#64748b; margin-top:4px;\">Hanya menghitung lampiran yang sudah memiliki ID upload.</div>");
		sb.append("<div style=\"display:flex; flex-wrap:wrap; gap:8px; margin-top:12px;\">");
		for (String jenis : data.dokumenPerJenis.keySet()) {
			int jumlah = data.dokumenPerJenis.get(jenis) == null ? 0 : data.dokumenPerJenis.get(jenis).intValue();
			if (jumlah <= 0) {
				continue;
			}
			sb.append("<div style=\"padding:10px 12px; border-radius:14px; background:#eff6ff; color:#1d4ed8; border:1px solid #bfdbfe; font-size:12px; font-weight:900;\">")
					.append(escapeHtml(jenis)).append(" <span style=\"color:#0f172a;\">(").append(formatNumber(jumlah))
					.append(")</span></div>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	private List<DashboardBar> buildProdiBars(DashboardData data) {
		List<DashboardBar> bars = new ArrayList<DashboardBar>();
		for (ProdiDashboardStat p : data.prodiStats.values()) {
			bars.add(new DashboardBar(p.prodi, p.totalAktivitas(), "#2563eb"));
		}
		return sortAndLimitBars(bars);
	}

	private List<DashboardBar> buildMatkulBars(DashboardData data, String mode) {
		List<DashboardBar> bars = new ArrayList<DashboardBar>();
		for (MatkulDashboardStat s : data.matkulStats) {
			long value;
			String color = "#2563eb";
			if ("materi".equals(mode)) {
				value = s.qtyMateri;
				color = "#7c3aed";
			} else if ("tugasUjian".equals(mode)) {
				value = s.qtyTugas + s.qtyUjian;
				color = "#ea580c";
			} else if ("media".equals(mode)) {
				value = s.qtyAudio + s.qtyVideo + s.qtyDiskusi;
				color = "#0d9488";
			} else if ("dokumen".equals(mode)) {
				value = s.qtyDokumenUpload;
				color = "#9333ea";
			} else if ("kehadiran".equals(mode)) {
				value = totalKehadiranVisible(s);
				color = "#2563eb";
			} else {
				value = s.totalAktivitas();
			}
			if (value > 0) {
				bars.add(new DashboardBar(s.shortTitle(), value, color));
			}
		}
		return sortAndLimitBars(bars);
	}

	private List<DashboardBar> sortAndLimitBars(List<DashboardBar> bars) {
		Collections.sort(bars, new Comparator<DashboardBar>() {
			@Override
			public int compare(DashboardBar o1, DashboardBar o2) {
				return Long.valueOf(o2.value).compareTo(Long.valueOf(o1.value));
			}
		});
		if (bars.size() > TOP_BAR_LIMIT) {
			return new ArrayList<DashboardBar>(bars.subList(0, TOP_BAR_LIMIT));
		}
		return bars;
	}

	private List<MatkulDashboardStat> getTopMatkul(List<MatkulDashboardStat> list, int limit) {
		List<MatkulDashboardStat> copy = new ArrayList<MatkulDashboardStat>(list);
		Collections.sort(copy, new Comparator<MatkulDashboardStat>() {
			@Override
			public int compare(MatkulDashboardStat o1, MatkulDashboardStat o2) {
				return Long.valueOf(o2.totalAktivitas()).compareTo(Long.valueOf(o1.totalAktivitas()));
			}
		});
		if (copy.size() > limit) {
			return new ArrayList<MatkulDashboardStat>(copy.subList(0, limit));
		}
		return copy;
	}

	private String buildLoadingDashboardHtml(String message, int percent) {
		int p = percent < 0 ? 0 : (percent > 100 ? 100 : percent);
		return "<div style=\"padding:22px; border-radius:20px; background:#ffffff; border:1px solid #e5e7eb; "
				+ "box-shadow:0 18px 38px rgba(15,23,42,.10); color:#0f172a; font-family:Arial,sans-serif;\">"
				+ "<div style=\"display:flex; align-items:center; justify-content:space-between; gap:12px; flex-wrap:wrap;\">"
				+ "<div><div style=\"font-size:11px; letter-spacing:.12em; text-transform:uppercase; color:#2563eb; font-weight:900;\">Memproses Dasbor e-Learning</div>"
				+ "<div style=\"font-size:18px; font-weight:900; margin-top:6px;\"><i class=\"fa fa-spinner fa-spin\"></i> Mengambil Data Tren Aktivitas Perkuliahan</div>"
				+ "<div style=\"font-size:12px; color:#64748b; margin-top:8px; line-height:1.55;\">"
				+ escapeHtml(message) + "</div></div>"
				+ "<div style=\"min-width:86px; text-align:right; font-size:30px; font-weight:900; color:#0f172a;\">"
				+ p + "%</div></div>"
				+ "<div style=\"height:12px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:18px;\"><div style=\"height:12px; width:"
				+ p + "%; border-radius:999px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));\"></div></div>"
				+ "<div style=\"display:flex; gap:8px; flex-wrap:wrap; margin-top:14px; font-size:11px; color:#475569;\">"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#eff6ff; color:#1d4ed8; font-weight:800;\">Filter</span>"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#fef3c7; color:#92400e; font-weight:800;\">Query Rekap</span>"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#ecfdf5; color:#166534; font-weight:800;\">Agregasi</span>"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#f5f3ff; color:#5b21b6; font-weight:800;\">CSS Chart</span>"
				+ "</div></div>";
	}

	private String buildFilterHeaderHtml() {
		return "<div style=\"padding:8px 10px 10px 10px; font-family:Arial,sans-serif;\">"
				+ "<div style=\"font-size:16px; font-weight:900; color:#0f172a;\">Filter Dasbor Tren Aktivitas Perkuliahan</div>"
				+ "<div style=\"font-size:11px; color:#64748b; margin-top:4px;\">Filter mengikuti pola Rekap Pertemuan Perkuliahan agar hasil dashboard selaras dengan laporan rekap.</div>"
				+ "</div>";
	}

	private String buildWelcomeHtml() {
		return "<div style=\"font-family:Arial,sans-serif; padding:22px; border-radius:20px; background:#ffffff; border:1px solid #e5e7eb; box-shadow:0 14px 30px rgba(15,23,42,.07);\">"
				+ "<div style=\"font-size:22px; font-weight:900; color:#0f172a;\">Dasbor Tren Aktivitas Perkuliahan</div>"
				+ "<div style=\"font-size:12px; color:#64748b; line-height:1.6; margin-top:8px;\">Gunakan tombol <b>Muat Data Dasbor</b> untuk menampilkan ringkasan modern berbasis data rekap pertemuan.</div>"
				+ "</div>";
	}

	private String buildEmptyHtml(String message) {
		return "<div style=\"font-family:Arial,sans-serif; padding:22px; border-radius:18px; background:#ffffff; border:1px dashed #cbd5e1; color:#64748b;\">"
				+ escapeHtml(message) + "</div>";
	}

	private String buildErrorHtml(Exception error) {
		String message = error == null ? "Terjadi kesalahan saat memproses dashboard." : error.getMessage();
		return "<div style=\"font-family:Arial,sans-serif; padding:22px; border-radius:18px; background:#fff1f2; border:1px solid #fecdd3; color:#991b1b;\">"
				+ "<div style=\"font-weight:900; font-size:16px;\">Gagal memuat dashboard</div>"
				+ "<div style=\"font-size:12px; margin-top:8px; line-height:1.6;\">" + escapeHtml(message) + "</div></div>";
	}

	private String buildFilterCaption(String tahun, String semester, Integer semesterKe, String program, Fakultas fakultas,
			Jurusan jurusan, Dosen dosen, Mahasiswa mahasiswa) {
		StringBuilder sb = new StringBuilder();
		sb.append("TA ").append(tahun == null ? "-" : tahun).append(", ");
		sb.append("Semester ").append(semester == null ? "Semua" : semester);
		if (semesterKe != null) {
			sb.append(" ke-").append(semesterKe);
		}
		sb.append(", Program ").append(program == null ? "Semua" : program);
		sb.append(", Fakultas ").append(fakultas == null ? "Semua" : fakultas.getNama());
		sb.append(", Prodi ").append(jurusan == null ? "Semua" : jurusan.getNama());
		if (dosen != null) {
			sb.append(", Dosen ").append(dosen.getNama());
		}
		if (mahasiswa != null) {
			sb.append(", Mahasiswa ").append(mahasiswa.getNama());
		}
		return sb.toString();
	}

	private Object getObject(Object[] objects, int index) {
		return objects != null && objects.length > index ? objects[index] : null;
	}

	private Long toLong(Object obj) {
		if (obj instanceof Number) {
			return Long.valueOf(((Number) obj).longValue());
		}
		try {
			return obj == null ? null : Long.valueOf(obj.toString());
		} catch (Exception e) {
			return null;
		}
	}

	private int toInt(Object obj) {
		if (obj instanceof Number) {
			return ((Number) obj).intValue();
		}
		try {
			return obj == null ? 0 : Integer.parseInt(obj.toString());
		} catch (Exception e) {
			return 0;
		}
	}

	private int safeInt(Integer value) {
		return value == null ? 0 : value.intValue();
	}

	private int safeInt(int value) {
		return value;
	}

	private Long parseLongSafe(String str) {
		try {
			return str == null ? null : Long.valueOf(str.trim());
		} catch (Exception e) {
			return null;
		}
	}

	private String buildDosenNames(Perkuliahan perkuliahan) {
		StringBuilder sb = new StringBuilder();
		try {
			List<Dosen> dosens = perkuliahan.populateDosenBuNama();
			for (Dosen dd : dosens) {
				if (dd == null || dd.getNama() == null) {
					continue;
				}
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(dd.getNama());
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java:1940");
		}
		return sb.toString();
	}

	private String safeText(String text) {
		return text == null ? "" : text;
	}

	private boolean isEmpty(String text) {
		return text == null || text.trim().length() == 0;
	}

	private String getComboStringValue(Combobox combobox) {
		try {
			return combobox != null && combobox.getSelectedItem() != null ? (String) combobox.getSelectedItem().getValue()
					: null;
		} catch (Exception e) {
			return null;
		}
	}

	private Integer getComboIntegerValue(Combobox combobox) {
		try {
			return combobox != null && combobox.getSelectedItem() != null ? (Integer) combobox.getSelectedItem().getValue()
					: null;
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T getComboObjectValue(Combobox combobox, Class<T> clazz) {
		try {
			Object value = combobox != null && combobox.getSelectedItem() != null ? combobox.getSelectedItem().getValue()
					: null;
			return clazz.isInstance(value) ? (T) value : null;
		} catch (Exception e) {
			return null;
		}
	}

	private double percent(long value, long total) {
		return total > 0 ? (value * 100.0) / total : 0.0;
	}

	private double clamp(double value) {
		if (value < 0.0) {
			return 0.0;
		}
		if (value > 100.0) {
			return 100.0;
		}
		return value;
	}

	private String formatNumber(long value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String formatDecimal(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf((int) value);
		}
	}

	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}


	private static class FastAggregateData {
		Map<Long, FastPerkuliahanAggregate> byPerkuliahan = new HashMap<Long, FastPerkuliahanAggregate>();
	}

	private static class FastPerkuliahanAggregate {
		List<Long> pertemuanIds = new ArrayList<Long>();
		int qtyMateri;
		int qtyAudio;
		int qtyVideo;
		int qtyDokumenUpload;
		int qtyDokumenTarget;
		TreeMap<String, Integer> dokumenPerJenis = new TreeMap<String, Integer>();
		TreeMap<String, Integer> kehadiranDosen = createStatusMap();
		TreeMap<String, Integer> kehadiranMahasiswa = createStatusMap();
		TreeMap<String, Integer> kehadiranSiswa = createStatusMap();
		TreeMap<String, Integer> kehadiranGuru = createStatusMap();
	}

	private static class DashboardData {
		String filterCaption = "";
		int totalPerkuliahanFilter;
		int totalPerkuliahanDimuat;
		int sampleLimit;
		long totalPertemuan;
		long totalCatatan;
		long totalTugas;
		long totalUjian;
		long totalDiskusi;
		long totalMateri;
		long totalAudio;
		long totalVideo;
		long totalTugasKelompok;
		long totalPengumuman;
		long totalBukuReferensi;
		long totalPertemuanOnline;
		long totalOnlineUser;
		long totalDinilai;
		long totalBelumDinilai;
		long totalDokumenUpload;
		long totalDokumenTarget;
		long totalRpsMahasiswa;
		long totalRpsAdmin;
		List<MatkulDashboardStat> matkulStats = new ArrayList<MatkulDashboardStat>();
		TreeMap<String, ProdiDashboardStat> prodiStats = new TreeMap<String, ProdiDashboardStat>();
		TreeMap<String, Integer> dokumenPerJenis = new TreeMap<String, Integer>();
		TreeMap<String, Integer> kehadiranDosen = createStatusMap();
		TreeMap<String, Integer> kehadiranMahasiswa = createStatusMap();
		TreeMap<String, Integer> kehadiranSiswa = createStatusMap();
		TreeMap<String, Integer> kehadiranGuru = createStatusMap();
	}

	private static class ProdiDashboardStat {
		String prodi;
		long pertemuan;
		long materi;
		long tugas;
		long ujian;
		long diskusi;
		long audio;
		long video;
		long dokumen;

		ProdiDashboardStat(String prodi) {
			this.prodi = prodi;
		}

		void add(MatkulDashboardStat stat) {
			pertemuan += stat.qtyPertemuan;
			materi += stat.qtyMateri;
			tugas += stat.qtyTugas;
			ujian += stat.qtyUjian;
			diskusi += stat.qtyDiskusi;
			audio += stat.qtyAudio;
			video += stat.qtyVideo;
			dokumen += stat.qtyDokumenUpload;
		}

		long totalAktivitas() {
			return pertemuan + materi + tugas + ujian + diskusi + audio + video + dokumen;
		}
	}

	private static class MatkulDashboardStat {
		Long perkuliahanId;
		String info = "";
		String kode = "";
		String matakuliah = "";
		String kelas = "";
		String prodi = "";
		String dosen = "";
		int qtyPertemuan;
		int qtyCatatan;
		int qtyTugas;
		int qtyUploadTugas;
		int qtyUjian;
		int qtyDiskusi;
		int qtyDiskusiDetail;
		int qtyMateri;
		int qtyAudio;
		int qtyVideo;
		int qtyTugasKelompok;
		int qtyPengumuman;
		int qtyBukuReferensi;
		int qtyPertemuanOnline;
		int qtyTotalOnline;
		int qtyTotalAkses;
		int qtyHadirMahasiswa;
		int qtyRpsMahasiswa;
		int qtyRpsAdmin;
		int qtyDinilai;
		int qtyBelumDinilai;
		int qtyDokumenUpload;
		int qtyDokumenTarget;
		double persenOnlineMahasiswa;
		double persenAksesMahasiswa;
		double persenHadirMahasiswa;
		double persenRpsMahasiswa;
		double persenRpsAdmin;
		double persenPenilaian;
		TreeMap<String, Integer> dokumenPerJenis = new TreeMap<String, Integer>();
		TreeMap<String, Integer> kehadiranDosen = createStatusMap();
		TreeMap<String, Integer> kehadiranMahasiswa = createStatusMap();
		TreeMap<String, Integer> kehadiranSiswa = createStatusMap();
		TreeMap<String, Integer> kehadiranGuru = createStatusMap();

		long totalAktivitas() {
			return qtyPertemuan + qtyCatatan + qtyTugas + qtyUjian + qtyDiskusi + qtyMateri + qtyAudio + qtyVideo
					+ qtyDokumenUpload;
		}

		long totalKehadiran() {
			return sumStatusMap(kehadiranDosen) + sumStatusMap(kehadiranMahasiswa) + sumStatusMap(kehadiranSiswa)
					+ sumStatusMap(kehadiranGuru);
		}

		String shortTitle() {
			String title = (kode == null || kode.length() == 0 ? "" : kode + " - ") + matakuliah;
			if (title.length() > 52) {
				return title.substring(0, 49) + "...";
			}
			return title;
		}
	}


	private static long sumStatusMap(TreeMap<String, Integer> map) {
		if (map == null) {
			return 0;
		}
		long total = 0;
		for (String key : map.keySet()) {
			Integer value = map.get(key);
			total += value == null ? 0 : value.intValue();
		}
		return total;
	}

	private static class DashboardBar {
		String label;
		long value;
		String color;

		DashboardBar(String label, long value, String color) {
			this.label = label;
			this.value = value;
			this.color = color;
		}
	}

	private static void closeHibernateSessionQuietly(Session session) {
		ais.common.ElearningSessionUtil.closeQuietly(session);
	}

}
