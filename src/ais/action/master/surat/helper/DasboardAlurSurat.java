package ais.action.master.surat.helper;
import ais.ui.util.DashboardGridExportHelper;

/* PATCH_2026_06_08_STATUS_DILEWATI_TO_MENUNGGU_PERSETUJUAN */

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


/* DASBOARD_ALUR_SURAT_V17_UIUX_EXPLANATION_REFACTOR_2026_06_04
 * - Menambahkan penjelasan end-user pada setiap panel modern.
 * - Menjaga semua grafik/trend/ringkasan memakai HTML/CSS, bukan HTML/CSS.
 * - Mempertahankan kompatibilitas Java 1.7 dan pola session existing.
 */


















/* DASBOARD_ALUR_SURAT_UNIFIED_V16A_FIX_LOADING_HELPER_SCOPE_2026_05_30 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V16_LOADING_PROGRESS_2026_05_30 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V15A_FIX_AVG_SLA_DETAIL_COUNTERS_2026_05_29 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V15_AVG_SLA_EXPLANATION_POPUP_2026_05_29 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V14_SIMPLE_NODE_TIMELINE_TOP_ALIGN_2026_05_29 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V13_COMPACT_KLASIFIKASI_PARAMETER_2026_05_29 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V12_GROUP_BY_PARENT_TIMELINE_2026_05_29 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V11A_FIX_EVENTLISTENER_HELPER_SCOPE_2026_05_29 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V11_FILTER_JENIS_ALUR_PERFORMA_DETAIL_2026_05_29 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V10_FIX_COUNTER_POPUP_COUNT_SYNC_2026_05_28 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V9_CLICKABLE_COUNTER_DASHBOARDS_2026_05_28 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V8_PAGING_PERFORMA_COMPACT_REPORT_2026_05_28 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V7_FAST_PARENT_SEQUENCE_CACHE_2026_05_28 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V6_PARENT_SEQUENCE_SLA_PAGING_2026_05_28 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V5_FIX_PENDING_KONSEPTOR_PEJABAT_2026_05_28 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V4_SLA_BUCKET_AND_KONTROL_SLA_2026_05_28 */
/* DASBOARD_ALUR_SURAT_UNIFIED_V3_SINGLE_TAB_2026_05_28 */
/* DASBOARD_ALUR_SURAT_KOMBINASI_ACCESS_CONTROL_V2_2026_05_28
 * Updated: role getMelihatSemuaSurat() + whitelist dapat melihat semua surat masuk/keluar.
 */
/* DASBOARD_ALUR_SURAT_KOMBINASI_MASUK_KELUAR_V1_2026_05_28
 * Kombinasi alur persetujuan Surat Keluar + Surat Masuk.
 * Dibuat dari pengembangan DasboardAlurSurat.java dan pola DasboardSop_WORKFLOW_ANALYTICS_V6.java.
 */

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.A;
import org.zkoss.zul.Column;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.Tbmrole;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Siswa;
import ais.database.model.surat.AlurPersetujuanSuratKeluar;
import ais.database.model.surat.AlurPersetujuanSuratKeluarStatus;
import ais.database.model.surat.AlurPersetujuanSuratMasuk;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;
import ais.database.model.surat.KlasifikasiSuratKeluarParemeterValue;
import ais.database.model.surat.SuratKeluar;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Memantau perjalanan surat masuk dan keluar agar antrian, keterlambatan, dan beban disposisi mudah diketahui.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardAlurSurat extends MyPortallayout {

	private static final long serialVersionUID = -9006490521125337935L;

	/* V3_COMPAT_MODE_CONSTANTS: konstanta lama dipertahankan agar pemanggilan lama tetap compile, tetapi UI hanya satu tab gabungan. */
	public static final String MODE_FULL = "FULL";
	public static final String MODE_ALUR = "ALUR";
	public static final String MODE_SLA = "SLA";
	public static final String MODE_KELUAR = "KELUAR";
	public static final String MODE_MASUK = "MASUK";
	public static final String MODE_GABUNGAN = "GABUNGAN";

	public static boolean debug = false;

	private static final int DETAIL_PAGE_SIZE = 10;
	private static final int RUNTIME_FILTER_SCAN_BATCH = 300;
	private static final int RUNTIME_FILTER_MAX_ROWS = 10000;
	private static final int DASHBOARD_SAMPLE_LIMIT = 500;
	private static final int SLA_WARNING_HOURS = 24;
	private static final int SLA_OVERDUE_HOURS = 48;
	private static final String FILTER_ALUR_SEMUA = "SEMUA";
	private static final String FILTER_ALUR_MASUK = "MASUK";
	private static final String FILTER_ALUR_KELUAR = "KELUAR";
	private static final String EVENT_RENDER_ALUR_DASHBOARD_V16 = "onRenderAlurDashboardV16";
	private static final String LABEL_STATUS_MENUNGGU_PERSETUJUAN = "Menunggu Persetujuan";
	private static final String LABEL_RINGKASAN_MENUNGGU_PERSETUJUAN = "Menunggu Persetujuan";

	/*
	 * SQL_INDEX_ALUR_SURAT_V7:
	 * Buat index berikut di PostgreSQL untuk mempercepat query parent-sequence:
	 * - alur_persetujuan_surat_keluar_status(surat_keluar, id)
	 * - alur_persetujuan_surat_masuk_status(surat_masuk, id)
	 * - surat_keluar(tanggal)
	 * - surat_masuk(tanggal)
	 */

	private Tbmuser tbmuser;
	private String mode = MODE_GABUNGAN;

	private Date dashboardFilterMulai;
	private Date dashboardFilterSampai;
	private SatuanKerja dashboardFilterSatker;
	private String dashboardFilterKeyword = "";
	private String dashboardFilterJenisAlur = FILTER_ALUR_SEMUA;

	/*
	 * V7 FAST CACHE:
	 * Cache alur per parent surat agar calculateSlaHours()/getPreviousAlurStatus()
	 * tidak query ulang per baris. Key = ID SuratKeluar/SuratMasuk.
	 */
	private Map<Long, List> alurKeluarByParentIdCache = new HashMap<Long, List>();
	private Map<Long, List> alurMasukByParentIdCache = new HashMap<Long, List>();

	public DasboardAlurSurat() throws Exception {
		this(MODE_GABUNGAN);
	}

	/**
	 * Constructor lama tetap dipertahankan agar pemanggilan lama seperti
	 * new DasboardAlurSurat(DasboardAlurSurat.MODE_SLA) tidak langsung error.
	 * Namun mulai V3 semua mode digabung dalam satu tab "Alur Persetujuan".
	 */
	public DasboardAlurSurat(String mode) throws Exception {
		super();
		this.mode = MODE_GABUNGAN;
		setWidth("100%");
		setMaximizedMode("whole");
		tbmuser = Common.getCurrentUser();
		init();
	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Alur Surat");
		renderHomeDasborContent(this, dashboardFilterMulai, dashboardFilterSampai, dashboardFilterSatker,
				dashboardFilterKeyword);
	}

	private boolean includeKeluar() {
		return !FILTER_ALUR_MASUK.equalsIgnoreCase(dashboardFilterJenisAlur);
	}

	private boolean includeMasuk() {
		return !FILTER_ALUR_KELUAR.equalsIgnoreCase(dashboardFilterJenisAlur);
	}

	private void renderHomeDasborContent(final Component parent, final Date mulai, final Date sampai,
			final SatuanKerja satuanKerja, final String keyword) throws Exception {
		if (parent == null) {
			return;
		}

		tampilkanLoadingDashboardAlurV16(parent, "Menyiapkan Dashboard Alur Persetujuan", 
				"Menyiapkan filter dan antrian proses dashboard...", 5);

		/*
		 * V16:
		 * Jika komponen sudah attach ke page, gunakan echo event agar loading muncul dulu
		 * sebelum query berat dijalankan. Jika belum attach, tetap fallback synchronous
		 * agar constructor lama tetap aman.
		 */
		if (parent.getPage() == null) {
			renderHomeDasborContentInternal(parent, mulai, sampai, satuanKerja, keyword);
			return;
		}

		final DashboardRenderRequestV16 request = new DashboardRenderRequestV16(parent, mulai, sampai, satuanKerja,
				keyword);
		final EventListener listener = new EventListener() {
			public void onEvent(Event event) throws Exception {
				try {
					parent.removeEventListener(EVENT_RENDER_ALUR_DASHBOARD_V16, this);
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:243");
				}
				DashboardRenderRequestV16 r = (DashboardRenderRequestV16) event.getData();
				renderHomeDasborContentInternal(r.parent, r.mulai, r.sampai, r.satuanKerja, r.keyword);
			}
		};
		parent.addEventListener(EVENT_RENDER_ALUR_DASHBOARD_V16, listener);
		Events.echoEvent(EVENT_RENDER_ALUR_DASHBOARD_V16, parent, request);
	}


	private void renderHomeDasborContentInternal(final Component parent, Date mulai, Date sampai, SatuanKerja satuanKerja,
			String keyword) throws Exception {
		if (parent == null) {
			return;
		}
		Common.clear(parent);

		if (mulai == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
			mulai = calendar.getTime();
		}
		if (sampai == null) {
			sampai = new Date();
		}
		if (keyword == null) {
			keyword = "";
		}

		dashboardFilterMulai = mulai;
		dashboardFilterSampai = sampai;
		dashboardFilterSatker = satuanKerja;
		dashboardFilterKeyword = keyword.trim();

		clearParentSequenceCacheV7();

		MyPortalchildren wrapper = new MyPortalchildren();
		wrapper.setWidth("100%");
		wrapper.setStyle("padding:6px; box-sizing:border-box;");
		wrapper.setParent(parent);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setTitle(getDashboardTitle());
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle("margin-bottom:12px; border:1px solid #dbeafe; border-radius:18px; overflow:hidden;"
				+ "background:#ffffff; box-shadow:0 14px 28px rgba(15,23,42,.08);");
		panel.setParent(wrapper);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setStyle("padding:0; background:#f6f8fb;");
		panelchildren.setParent(panel);

		Div shell = new Div();
		shell.setWidth("100%");
		shell.setStyle("background:#f6f8fb; padding:14px; box-sizing:border-box; overflow:auto;");
		shell.setParent(panelchildren);

		tampilkanLoadingDashboardAlurV16(shell, "Mengambil Data Alur Surat",
				"Menghitung KPI, SLA, overdue, aging, dan parent sequence surat masuk/keluar...", 35);
		final AlurSuratDashboardData data = loadDashboardData();

		tampilkanLoadingDashboardAlurV16(shell, "Menyusun Tampilan Dashboard",
				"Menyusun card ringkasan, kontrol SLA, report alur, dan popup detail...", 85);
		Common.clear(shell);

		renderHeroDasbor(shell, data);
		renderGlobalFilter(shell, dashboardFilterMulai, dashboardFilterSampai, dashboardFilterSatker,
				dashboardFilterKeyword);
		renderMetricCards(shell, data);
		renderModeSpecificDashboards(shell, data);
	}

	private String getDashboardTitle() {
		return "Alur Persetujuan Surat";
	}

	private void renderModeSpecificDashboards(Component parent, AlurSuratDashboardData data) {
		/*
		 * V3 Unified:
		 * Semua dashboard alur surat masuk + keluar diringkas dalam satu layar.
		 * Mode lama (ALUR/SLA/FULL/KELUAR/MASUK/GABUNGAN) tidak lagi membuat tab terpisah.
		 * Dashboard yang redundan antar mode dihapus dari render utama.
		 */
		MyPortallayout portalLayout = new MyPortallayout();
		portalLayout.setWidth("100%");
		portalLayout.setMaximizedMode("whole");
		portalLayout.setStyle("margin-top:12px;");
		portalLayout.setParent(parent);

		String pcWidth = Common.isMobile() ? "100%" : "50%";

		MyPortalchildren pcTop = createPortalChild(portalLayout, "100%");
		MyPortalchildren pcLeft = createPortalChild(portalLayout, pcWidth);
		MyPortalchildren pcRight = createPortalChild(portalLayout, pcWidth);
		MyPortalchildren pcBottom = createPortalChild(portalLayout, "100%");

		// Ringkasan gabungan masuk vs keluar ditempatkan paling atas.
		renderJenisSuratComparison(pcTop, data);

		// Inti alur dan SLA. Ini menggantikan tab "Alur Persetujuan Surat",
		// "Kontrol SLA Surat", dan "Analitik Alur & SLA".
		renderFunnelAlurSurat(pcLeft, data);
		renderSlaControlDashboard(pcRight, data);
		renderSlaAgingDashboard(pcLeft, data);

		// V4: kontrol SLA detail mengikuti contoh dokumen "kontrol SLA".
		renderTabelDetailKontrolSla(pcTop, data);
		renderReportAlurSuratKeluarPaging(pcTop, data);
		renderReportAlurSuratMasukPaging(pcTop, data);
		renderPerformaSlaKonseptorJabatan(pcBottom, data);
		renderTrendDistribusiSla(pcBottom, data);

		renderSebaranAlur(pcRight, data);
		renderBebanPejabat(pcLeft, data);
		renderSlaRiskByPejabat(pcRight, data);

		// Dashboard pendukung yang tidak redundan.
		renderAktivitasTerbaru(pcLeft, data);
		renderBottleneckAlur(pcRight, data);

		// Insight manajerial.
		renderWorkflowMatrix(pcBottom, data);
		renderSlaExecutionPlan(pcBottom, data);
		renderSlaAuditReadiness(pcBottom, data);
	}

	private MyPortalchildren createPortalChild(Component parent, String width) {
		MyPortalchildren pc = new MyPortalchildren();
		pc.setWidth(width);
		pc.setStyle("padding:6px; box-sizing:border-box;");
		pc.setParent(parent);
		return pc;
	}

	private void tampilkanLoadingDashboardAlurV16(Component parent, String judul, String detail, int persen) {
		if (parent == null) {
			return;
		}
		try {
			if (persen < 0) {
				persen = 0;
			}
			if (persen > 100) {
				persen = 100;
			}
			Common.clear(parent);
			final Vbox container = new Vbox();
			container.setWidth("100%");
			container.setStyle("box-sizing:border-box; padding:14px;");
			container.setParent(parent);

			Html htmlLoading = new Html(buildLoadingDashboardAlurHtmlV16(judul, detail, persen));
			container.appendChild(htmlLoading);
		} catch (Exception e) {
			debugError("tampilkanLoadingDashboardAlurV16", e);
		}
	}

	private String buildLoadingDashboardAlurHtmlV16(String judul, String detail, int persen) {
		String title = judul == null ? "Memproses Dashboard Alur Surat..." : judul;
		String desc = detail == null ? "Mohon tunggu, sistem sedang mengambil data alur surat." : detail;
		return "<div style='padding:18px; border-radius:18px; background:#ffffff; border:1px solid #e5e7eb;"
				+ "box-shadow:0 12px 28px rgba(15,23,42,.08); color:#334155; font-family:Arial, sans-serif;'>"
				+ "<div style='display:flex; align-items:center; gap:12px;'>"
				+ "<div style='width:42px; height:42px; border-radius:14px; display:flex; align-items:center; justify-content:center;"
				+ "background:linear-gradient(135deg,#2563eb,#06b6d4); color:#fff; font-size:18px;'>"
				+ "<i class=\"fa fa-spinner fa-spin\"></i></div>"
				+ "<div style='flex:1; min-width:0;'>"
				+ "<div style='font-size:15px; font-weight:900; color:#0f172a;'>" + safeHtml(title) + "</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:3px; line-height:1.45;'>" + safeHtml(desc)
				+ "</div></div>"
				+ "<div style='font-size:18px; font-weight:900; color:#2563eb;'>" + persen + "%</div></div>"
				+ "<div style='margin-top:14px; height:10px; border-radius:999px; background:#e2e8f0; overflow:hidden;'>"
				+ "<div style='height:10px; width:" + persen
				+ "%; border-radius:999px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div></div>"
				+ "<div style='margin-top:10px; display:flex; gap:6px; flex-wrap:wrap;'>"
				+ "<span style='font-size:10px; font-weight:800; color:#1d4ed8; background:#dbeafe; border-radius:999px; padding:4px 8px;'>Query alur</span>"
				+ "<span style='font-size:10px; font-weight:800; color:#047857; background:#d1fae5; border-radius:999px; padding:4px 8px;'>Hitung SLA</span>"
				+ "<span style='font-size:10px; font-weight:800; color:#7c2d12; background:#ffedd5; border-radius:999px; padding:4px 8px;'>Parent sequence</span>"
				+ "<span style='font-size:10px; font-weight:800; color:#5b21b6; background:#ede9fe; border-radius:999px; padding:4px 8px;'>Render dashboard</span>"
				+ "</div></div>";
	}


	private void renderHeroDasbor(Component parent, final AlurSuratDashboardData d) {
		Div hero = new Div();
		hero.setStyle("position:relative; overflow:hidden; border-radius:18px; padding:22px 24px; color:#ffffff;"
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);"
				+ "box-shadow:0 14px 30px rgba(15,23,42,.18); box-sizing:border-box;");
		hero.setParent(parent);

		appendHtml(hero,
				"<div style='position:absolute; right:-60px; top:-70px; width:210px; height:210px; border-radius:999px; background:rgba(255,255,255,.12);'></div>"
						+ "<div style='position:absolute; right:90px; bottom:-70px; width:160px; height:160px; border-radius:999px; background:rgba(255,255,255,.10);'></div>");

		Hbox content = new Hbox();
		content.setWidth("100%");
		content.setPack("justify");
		content.setAlign("center");
		content.setStyle("position:relative; z-index:1; gap:16px; flex-wrap:wrap;");
		content.setParent(hero);

		Vbox titleBox = new Vbox();
		titleBox.setStyle("max-width:780px;");
		titleBox.setParent(content);

		appendHtml(titleBox,
				"<div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;'>Surat Workflow Control Center</div>"
						+ "<div style='font-size:28px; line-height:1.15; font-weight:800; margin-top:6px;'>"
						+ safeHtml(getDashboardTitle()) + "</div>"
						+ "<div style='font-size:13px; opacity:.90; margin-top:8px;'>Pantau gabungan alur persetujuan surat keluar dan surat masuk, status disposisi, bottleneck pejabat/jabatan, serta kontrol SLA dalam satu tab Alur Persetujuan. Angka utama dapat diklik untuk melihat detail grid paging 10 data.</div>");

		String satkerText = dashboardFilterSatker == null ? "Semua Satker" : dashboardFilterSatker.getNama();
		String keywordText = dashboardFilterKeyword == null || dashboardFilterKeyword.trim().isEmpty() ? "Tanpa keyword"
				: dashboardFilterKeyword.trim();
		appendHtml(titleBox, "<div style='margin-top:12px; display:flex; gap:8px; flex-wrap:wrap;'>"
				+ badgeHtml("Periode: " + formatDate(dashboardFilterMulai) + " s.d. " + formatDate(dashboardFilterSampai))
				+ badgeHtml(satkerText) + badgeHtml("Cari: " + keywordText) + badgeHtml("Mode: " + getJenisModeText())
				+ badgeHtml("SLA: Warning " + SLA_WARNING_HOURS + " jam, Overdue " + SLA_OVERDUE_HOURS + " jam")
				+ "</div>");

		Hbox numberBox = new Hbox();
		numberBox.setStyle("gap:10px; flex-wrap:wrap;");
		numberBox.setParent(content);

		createHeroNumber(numberBox, "Total Tahap", d.totalTahap, "Detail Semua Tahap Alur",
				createAllProvider());
		createHeroNumber(numberBox, "Surat Keluar", d.totalKeluar, "Detail Semua Alur Surat Keluar",
				createKeluarProvider(createAllKeluarProvider()));
		createHeroNumber(numberBox, "Surat Masuk", d.totalMasuk, "Detail Semua Alur Surat Masuk",
				createMasukProvider(createAllMasukProvider()));
		createHeroNumber(numberBox, "SLA Overdue", d.slaOverdue, "Detail SLA Overdue",
				createSlaOverdueProvider());
	}

	private String badgeHtml(String text) {
		return "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>"
				+ safeHtml(text) + "</span>";
	}

	private String getJenisModeText() {
		if (FILTER_ALUR_MASUK.equalsIgnoreCase(dashboardFilterJenisAlur)) {
			return "Hanya Surat Masuk";
		}
		if (FILTER_ALUR_KELUAR.equalsIgnoreCase(dashboardFilterJenisAlur)) {
			return "Hanya Surat Keluar";
		}
		return "Semua Alur Surat";
	}

	private void createHeroNumber(Component parent, String title, long value, String detailTitle,
			final DetailDataProvider provider) {
		Vbox box = new Vbox();
		box.setStyle("background:rgba(255,255,255,.16); border:1px solid rgba(255,255,255,.24); padding:12px 16px; border-radius:14px; min-width:125px; text-align:center;");
		box.setParent(parent);
		createDetailNumber(box, String.valueOf(value), detailTitle, provider,
				"font-size:25px; font-weight:900; color:#ffffff; text-decoration:none; cursor:pointer;");
		appendHtml(box, "<div style='font-size:11px; opacity:.88; margin-top:4px;'>" + safeHtml(title) + "</div>");
	}

	private void renderGlobalFilter(final Component parent, Date mulai, Date sampai, final SatuanKerja satuanKerja,
			String keyword) throws Exception {
		final Div filterContainer = new Div();
		filterContainer.setParent(parent);
		filterContainer.setStyle("margin-top:12px; padding:14px; background:#ffffff; border:1px solid #e8eef6; "
				+ "border-radius:16px; box-shadow:0 10px 26px rgba(15,23,42,0.04); box-sizing:border-box;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(filterContainer);
		toolbar.setStyle("border:0; background:transparent; padding:0; display:flex; flex-wrap:wrap; align-items:center; gap:8px;");

		new MyLabelAgakKecil("Mulai:").setParent(toolbar);
		final MyDatebox dbMulai = new MyDatebox(mulai);
		dbMulai.setReadonly(true);
		dbMulai.setCols(5);
		dbMulai.setParent(toolbar);

		new MyLabelAgakKecil("Sampai:").setParent(toolbar);
		final MyDatebox dbSampai = new MyDatebox(sampai);
		dbSampai.setReadonly(true);
		dbSampai.setCols(5);
		dbSampai.setParent(toolbar);

		new MyLabelAgakKecil("Satker:").setParent(toolbar);
		final AmbilDataSatuanKerjaBanbox cbSatker = new AmbilDataSatuanKerjaBanbox();
		cbSatker.setCols(8);
		cbSatker.setReadonly(true);
		if (satuanKerja != null) {
			cbSatker.setValue(satuanKerja.getNama());
			cbSatker.setAttribute("satuanKerja", satuanKerja);
		}
		cbSatker.setParent(toolbar);

		new MyLabelAgakKecil("Jenis:").setParent(toolbar);
		final Combobox cbJenisAlur = new Combobox();
		cbJenisAlur.setReadonly(true);
		cbJenisAlur.setCols(20);
		cbJenisAlur.setTooltiptext("Pilih jenis alur surat yang ingin ditampilkan");

		Comboitem itemSemua = new Comboitem("Tampil Semua Alur Surat");
		itemSemua.setValue(FILTER_ALUR_SEMUA);
		itemSemua.setParent(cbJenisAlur);

		Comboitem itemMasuk = new Comboitem("Tampil Hanya Surat Masuk");
		itemMasuk.setValue(FILTER_ALUR_MASUK);
		itemMasuk.setParent(cbJenisAlur);

		Comboitem itemKeluar = new Comboitem("Tampil Hanya Surat Keluar");
		itemKeluar.setValue(FILTER_ALUR_KELUAR);
		itemKeluar.setParent(cbJenisAlur);

		if (FILTER_ALUR_MASUK.equalsIgnoreCase(dashboardFilterJenisAlur)) {
			cbJenisAlur.setSelectedItem(itemMasuk);
		} else if (FILTER_ALUR_KELUAR.equalsIgnoreCase(dashboardFilterJenisAlur)) {
			cbJenisAlur.setSelectedItem(itemKeluar);
		} else {
			cbJenisAlur.setSelectedItem(itemSemua);
		}
		cbJenisAlur.setParent(toolbar);

		new MyLabelAgakKecil("Cari:").setParent(toolbar);
		final Textbox txtKeyword = new Textbox();
		txtKeyword.setCols(16);
		txtKeyword.setValue(keyword == null ? "" : keyword);
		txtKeyword.setTooltiptext("Cari kode, nomor surat, asal, agenda, nama, perihal, klasifikasi, alur, pejabat, jabatan, atau catatan disposisi");
		txtKeyword.setParent(toolbar);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan Dasbor", "/img/svg/search.svg");
		refresh.setTooltiptext("Refresh dashboard berdasarkan filter global");
		refresh.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; "
				+ "padding:6px 14px; margin-left:4px;");
		refresh.setParent(toolbar);

		EventListener refreshListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				dashboardFilterJenisAlur = DasboardAlurSurat.this.getSelectedJenisAlurFilterV11(cbJenisAlur);
				renderHomeDasborContent(DasboardAlurSurat.this, dbMulai.getValue(), dbSampai.getValue(),
						(SatuanKerja) cbSatker.getAttribute("satuanKerja"), txtKeyword.getValue());
			}
		};
		refresh.addEventListener("onClick", refreshListener);
		txtKeyword.addEventListener("onOK", refreshListener);
		cbJenisAlur.addEventListener("onChange", refreshListener);
		cbJenisAlur.addEventListener("onSelect", refreshListener);
		dbMulai.addEventListener("onChange", refreshListener);
		dbSampai.addEventListener("onChange", refreshListener);
	}

	private String getSelectedJenisAlurFilterV11(Combobox cbJenisAlur) {
		try {
			if (cbJenisAlur != null && cbJenisAlur.getSelectedItem() != null
					&& cbJenisAlur.getSelectedItem().getValue() != null) {
				return String.valueOf(cbJenisAlur.getSelectedItem().getValue());
			}
		} catch (Exception e) {
			debugError("getSelectedJenisAlurFilterV11", e);
		}
		return FILTER_ALUR_SEMUA;
	}


	private void renderMetricCards(Component parent, AlurSuratDashboardData d) {
		Div wrap = new Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px;");
		wrap.setParent(parent);

		createMetricCard(wrap, "Menunggu", d.menunggu, "Belum disetujui / belum ditolak", "#fef3c7", "#92400e",
				"!", "Detail Status Menunggu", createPendingProvider());
		createMetricCard(wrap, "Disetujui", d.disetujui, "Tahap yang sudah disetujui", "#dcfce7", "#166534", "✓",
				"Detail Status Disetujui", createApprovedProvider());
		createMetricCard(wrap, "Ditolak", d.ditolak, "Tahap yang ditolak", "#fee2e2", "#991b1b", "×",
				"Detail Status Ditolak", createRejectedProvider());
		createMetricCard(wrap, "Direvisi", d.direvisi, "Tahap/surat yang sudah direvisi", "#dbeafe", "#1e40af",
				"R", "Detail Status Direvisi", createRevisedProvider());
		createMetricCard(wrap, "Overdue SLA", d.slaOverdue, "Menunggu lebih dari " + SLA_OVERDUE_HOURS + " jam",
				"#fee2e2", "#991b1b", "⏱", "Detail SLA Overdue", createSlaOverdueProvider());
		createMetricCard(wrap, "Warning SLA", d.slaWarning, "Menunggu " + SLA_WARNING_HOURS + "-"
				+ SLA_OVERDUE_HOURS + " jam", "#ffedd5", "#9a3412", "!", "Detail SLA Warning",
				createSlaWarningProvider());
		createMetricCard(wrap, "Aman SLA", d.slaAman, "Menunggu masih di bawah warning", "#ecfdf5", "#166534", "✓",
				"Detail SLA Aman", createSlaSafeProvider());
		createAverageSlaMetricCardV15(wrap, d);
	}

	private void createMetricCard(Component parent, String title, long value, String desc, String bg, String color,
			String icon, final String detailTitle, final DetailDataProvider provider) {
		Div card = new Div();
		card.setStyle("flex:1 1 155px; min-width:155px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:14px;"
				+ "box-shadow:0 10px 22px rgba(15,23,42,.06); box-sizing:border-box;");
		card.setParent(parent);

		Hbox top = new Hbox();
		top.setWidth("100%");
		top.setPack("justify");
		top.setAlign("center");
		top.setParent(card);

		appendHtml(top,
				"<div style='width:38px; height:38px; border-radius:12px; display:flex; align-items:center; justify-content:center; font-weight:800; background:"
						+ bg + "; color:" + color + ";'>" + safeHtml(icon) + "</div>");
		createDetailNumber(top, String.valueOf(value), detailTitle, provider,
				"font-size:26px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer;");

		appendHtml(card, "<div style='font-size:12px; color:#64748b; margin-top:10px;'>" + safeHtml(title)
				+ "</div>" + "<div style='font-size:11px; color:#94a3b8; margin-top:3px;'>" + safeHtml(desc)
				+ "</div>");
	}


	private void createAverageSlaMetricCardV15(Component parent, final AlurSuratDashboardData d) {
		Div card = new Div();
		card.setStyle("flex:1 1 210px; min-width:210px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:14px;"
				+ "box-shadow:0 10px 22px rgba(15,23,42,.06); box-sizing:border-box;");
		card.setParent(parent);

		Hbox top = new Hbox();
		top.setWidth("100%");
		top.setPack("justify");
		top.setAlign("center");
		top.setParent(card);

		appendHtml(top,
				"<div style='width:38px; height:38px; border-radius:12px; display:flex; align-items:center; justify-content:center; font-weight:800; background:#ede9fe; color:#5b21b6;'>H</div>");

		A angka = new A(String.valueOf(Math.round(d == null ? 0.0d : d.avgSlaHours)));
		angka.setTooltiptext("Klik untuk melihat proses penghitungan rata-rata SLA");
		angka.setStyle("font-size:26px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer;"
				+ "border-bottom:1px dashed #5b21b6;");
		angka.setParent(top);
		angka.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) throws Exception {
				showAverageSlaCalculationPopupV15(d);
			}
		});

		appendHtml(card, "<div style='font-size:12px; color:#64748b; margin-top:10px;'>Rata-rata SLA</div>"
				+ "<div style='font-size:11px; color:#94a3b8; margin-top:3px; line-height:1.35;'>"
				+ "Total durasi SLA item selesai ÷ jumlah item selesai. Item yang masih menunggu tidak ikut rata-rata.</div>"
				+ "<div style='margin-top:8px; display:flex; flex-wrap:wrap; gap:5px;'>"
				+ "<span style='border-radius:999px; padding:3px 7px; background:#f5f3ff; color:#5b21b6; font-size:10px; font-weight:900;'>"
				+ "Total " + (d == null ? 0L : d.avgSlaTotalHours) + " jam</span>"
				+ "<span style='border-radius:999px; padding:3px 7px; background:#ecfdf5; color:#166534; font-size:10px; font-weight:900;'>"
				+ (d == null ? 0L : d.avgSlaClosedCount) + " selesai</span>"
				+ "</div>");
	}

	private void showAverageSlaCalculationPopupV15(final AlurSuratDashboardData d) {
		try {
			final Window window = new Window();
			window.setTitle("Proses Penghitungan Rata-rata SLA");
			window.setWidth(Common.isMobile() ? "96%" : "820px");
			window.setHeight(Common.isMobile() ? "88%" : "650px");
			window.setClosable(true);
			window.setSizable(true);
			window.setMaximizable(true);
			window.setBorder("normal");
			window.setStyle("border-radius:16px; overflow:hidden;");
			if (DasboardAlurSurat.this.getPage() != null) {
				window.setPage(DasboardAlurSurat.this.getPage());
			} else if (DasboardAlurSurat.this.getParent() != null) {
				window.setParent(DasboardAlurSurat.this.getParent());
			}

			Div wrapper = new Div();
			wrapper.setParent(window);
			wrapper.setWidth("100%");
			wrapper.setHeight("100%");
			wrapper.setStyle("box-sizing:border-box; padding:14px; background:#f8fafc; overflow:auto;");

			long totalJam = d == null ? 0L : d.avgSlaTotalHours;
			long jumlahSelesai = d == null ? 0L : d.avgSlaClosedCount;
			long jumlahDikeluarkan = d == null ? 0L : d.avgSlaExcludedPendingCount;
			long rata = jumlahSelesai <= 0L ? 0L : Math.round((totalJam * 1.0d) / jumlahSelesai);

			appendHtml(wrapper, "<div style='border-radius:18px; padding:16px; color:#ffffff;"
					+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); box-shadow:0 14px 28px rgba(124,58,237,.20);'>"
					+ "<div style='font-size:12px; text-transform:uppercase; letter-spacing:.10em; opacity:.82;'>Formula Rata-rata SLA</div>"
					+ "<div style='font-size:28px; font-weight:900; margin-top:6px;'>"
					+ totalJam + " jam ÷ " + jumlahSelesai + " item selesai = " + rata + " jam</div>"
					+ "<div style='font-size:12px; line-height:1.55; margin-top:8px; opacity:.92;'>"
					+ "Rata-rata SLA dihitung dari item alur yang sudah ditindaklanjuti, yaitu sudah disetujui atau ditolak. "
					+ "Item yang masih menunggu belum dimasukkan ke rata-rata karena durasinya masih berjalan.</div>"
					+ "</div>");

			appendHtml(wrapper, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(170px,1fr)); gap:10px; margin-top:12px;'>"
					+ avgSlaInfoCardV15("Total Durasi SLA", totalJam + " jam", "Akumulasi durasi semua item selesai.", "#ede9fe", "#5b21b6")
					+ avgSlaInfoCardV15("Jumlah Item Selesai", String.valueOf(jumlahSelesai), "Item yang sudah disetujui/ditolak.", "#dcfce7", "#166534")
					+ avgSlaInfoCardV15("Tidak Dihitung", String.valueOf(jumlahDikeluarkan), "Item yang masih menunggu.", "#fef3c7", "#92400e")
					+ avgSlaInfoCardV15("Rata-rata", rata + " jam", "Hasil pembulatan rata-rata.", "#e0f2fe", "#075985")
					+ "</div>");

			appendHtml(wrapper, "<div style='margin-top:12px; padding:12px 14px; border-radius:14px; background:#ffffff; "
					+ "border:1px solid #e5e7eb; color:#475569; font-size:12px; line-height:1.55;'>"
					+ "<b>Cara membaca:</b> jika total durasi SLA 120 jam dari 10 item selesai, maka rata-rata SLA adalah 12 jam. "
					+ "Durasi per item dihitung berdasarkan konsep alur: dari waktu tindak lanjut alur sebelumnya ke waktu tindak lanjut alur saat ini. "
					+ "Jika tidak ada alur sebelumnya, waktu awal memakai waktu parent surat.</div>");

			renderAverageSlaSampleGridV15(wrapper);

			window.doModal();
		} catch (Exception e) {
			debugError("showAverageSlaCalculationPopupV15", e);
		}
	}

	private String avgSlaInfoCardV15(String title, String value, String desc, String bg, String color) {
		return "<div style='border-radius:15px; padding:13px; background:" + bg + "; border:1px solid rgba(15,23,42,.08);'>"
				+ "<div style='font-size:11px; font-weight:900; color:" + color + ";'>" + safeHtml(title) + "</div>"
				+ "<div style='font-size:24px; font-weight:900; line-height:1; color:" + color + "; margin-top:8px;'>"
				+ safeHtml(value) + "</div>"
				+ "<div style='font-size:11px; color:" + color + "; opacity:.82; line-height:1.35; margin-top:7px;'>"
				+ safeHtml(desc) + "</div></div>";
	}

	private void renderAverageSlaSampleGridV15(Component parent) {
		try {
			final DetailDataProvider provider = createRuntimeFilteredProvider(createAllProvider(), new RuntimeRowFilter() {
				public boolean accept(Object row) {
					return !isPending(row);
				}
			});

			appendHtml(parent, "<div style='font-size:13px; font-weight:900; color:#0f172a; margin-top:16px; margin-bottom:8px;'>"
					+ "Contoh Item yang Masuk Perhitungan</div>");

			final Paging paging = new Paging();
			paging.setPageSize(DETAIL_PAGE_SIZE);
			paging.setDetailed(true);
			long total = provider.count();
			paging.setTotalSize(total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total);
			paging.setParent(parent);

			final Grid grid = new Grid();
			grid.setParent(parent);
			grid.setSclass("dgrid fgrid");
			grid.setStyle(compactGridStyleV8());

			Columns columns = new Columns();
			columns.setParent(grid);
			addCompactColumnV8(columns, "Jenis", "80px");
			addCompactColumnV8(columns, "Tanggal", "90px");
			addCompactColumnV8(columns, "Kode", "120px");
			addCompactColumnV8(columns, "Alur", null);
			addCompactColumnV8(columns, "Status", "95px");
			addCompactColumnV8(columns, "Durasi", "85px");

			final Rows rows = new Rows();
			rows.setParent(grid);

			EventListener refresh = new EventListener() {
				public void onEvent(Event event) throws Exception {
					Common.clear(rows);
					List dataRows = provider.list(paging.getActivePage() * DETAIL_PAGE_SIZE, DETAIL_PAGE_SIZE);
					preloadParentSequenceCacheV7(dataRows);
					for (int i = 0; dataRows != null && i < dataRows.size(); i++) {
						Object item = dataRows.get(i);
						Row row = new Row();
						row.setStyle(compactRowStyleV8() + " vertical-align:top;");
						row.setParent(rows);
						addCompactCellV8(row, isMasuk(item) ? "Masuk" : "Keluar", true);
						addCompactCellV8(row, formatDate(getTanggal(item)), false);
						addCompactCellV8(row, getKode(item), true);
						addCompactCellV8(row, getAlurName(item), false);
						addCompactCellV8(row, getStatusText(item), true);
						addCompactCellV8(row, calculateSlaHours(item) + " jam", true, "#0f172a", "right");
					}
				}
			};
			paging.addEventListener("onPaging", refresh);
			refresh.onEvent(null);
		} catch (Exception e) {
			debugError("renderAverageSlaSampleGridV15", e);
		}
	}

	private void renderFunnelAlurSurat(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel(" Alur Persetujuan Surat Masuk & Keluar", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; margin-bottom:12px; line-height:1.55;'> menggambarkan posisi seluruh tahap alur: menunggu, disetujui, ditolak, direvisi, dan selesai. Klik angka untuk melihat detail gabungan surat masuk dan keluar.</div>");
		long max = max(new long[] { d.menunggu, d.disetujui, d.ditolak, d.direvisi, d.selesai });
		renderFunnelRow(pch, "Menunggu", d.menunggu, max, "#f59e0b", "Detail Status Menunggu", createPendingProvider());
		renderFunnelRow(pch, "Disetujui", d.disetujui, max, "#16a34a", "Detail Status Disetujui",
				createApprovedProvider());
		renderFunnelRow(pch, "Ditolak", d.ditolak, max, "#dc2626", "Detail Status Ditolak", createRejectedProvider());
		renderFunnelRow(pch, "Direvisi", d.direvisi, max, "#2563eb", "Detail Status Direvisi", createRevisedProvider());
		renderFunnelRow(pch, "Selesai", d.selesai, max, "#0891b2", "Detail Status Selesai", createFinishedProvider());
	}

	private void renderFunnelRow(Component parent, String label, long value, long max, String color, String detailTitle,
			DetailDataProvider provider) {
		Hbox row = new Hbox();
		row.setWidth("100%");
		row.setAlign("center");
		row.setStyle("gap:10px; margin:9px 0;");
		row.setParent(parent);

		appendHtml(row, "<div style='width:190px; font-size:12px; color:#334155; font-weight:800;'>"
				+ safeHtml(label) + "</div>");

		long pct = max <= 0L ? 0L : Math.round((value * 100.0d) / max);
		if (pct < 4L && value > 0L) {
			pct = 4L;
		}
		appendHtml(row,
				"<div style='flex:1; background:#e5e7eb; border-radius:999px; height:12px; overflow:hidden;'>"
						+ "<div style='height:12px; width:" + pct + "%; background:" + color
						+ "; border-radius:999px;'></div></div>");
		createDetailNumber(row, String.valueOf(value), detailTitle, provider,
				"width:58px; text-align:right; font-size:13px; font-weight:900; color:#0f172a; text-decoration:none;");
	}

	private void renderSlaControlDashboard(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel("Dashboard Kontrol SLA Surat Masuk & Keluar", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>SLA dihitung dari waktu pembuatan surat/tahap sampai waktu persetujuan/penolakan. Untuk yang belum selesai, SLA dihitung sampai waktu saat ini. Overdue jika lebih dari "
						+ SLA_OVERDUE_HOURS + " jam.</div>");

		Hbox row = new Hbox();
		row.setWidth("100%");
		row.setStyle("gap:12px; flex-wrap:wrap;");
		row.setParent(pch);

		createPressureCard(row, "Overdue", d.slaOverdue, "Menunggu lebih dari " + SLA_OVERDUE_HOURS + " jam",
				"#fee2e2", "#991b1b", "Detail SLA Overdue", createSlaOverdueProvider());
		createPressureCard(row, "Warning", d.slaWarning, "Menunggu " + SLA_WARNING_HOURS + "-" + SLA_OVERDUE_HOURS
				+ " jam", "#ffedd5", "#9a3412", "Detail SLA Warning", createSlaWarningProvider());
		createPressureCard(row, "Aman", d.slaAman, "Menunggu masih aman", "#ecfdf5", "#166534", "Detail SLA Aman",
				createSlaSafeProvider());
		createPressureCard(row, "Rata-rata Jam", Math.round(d.avgSlaHours), "Target maks. 24 jam",
				"#ede9fe", "#5b21b6", "Detail Semua Tahap Alur", createAllProvider());
		createPressureCard(row, "Kepatuhan SLA", d.slaCompliancePct, "Target minimal 95% tepat waktu",
				(d.slaCompliancePct >= 95L ? "#dcfce7" : "#fee2e2"),
				(d.slaCompliancePct >= 95L ? "#166534" : "#991b1b"), "Detail Semua Tahap Alur", createAllProvider());
		createPressureCard(row, "Outstanding", d.menunggu, "Surat/tahap aktif yang belum selesai",
				"#dbeafe", "#1e40af", "Detail Status Menunggu", createPendingProvider());

		appendHtml(pch, "<div style='margin-top:12px;'>"
				+ gaugeHtml("Completion Rate", percent(d.disetujui + d.selesai, Math.max(1L, d.totalTahap)),
						"Proporsi tahap disetujui/selesai terhadap seluruh tahap.", "#16a34a")
				+ gaugeHtml("Queue Pressure", percent(d.menunggu, Math.max(1L, d.totalTahap)),
						"Proporsi tahap yang masih menunggu.", "#f59e0b")
				+ gaugeHtml("SLA Risk", percent(d.slaOverdue + d.slaWarning, Math.max(1L, d.totalTahap)),
						"Proporsi item warning/overdue terhadap total tahap.", "#dc2626") + "</div>");
	}

	private void renderSlaAgingDashboard(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel("Aging SLA Menunggu Persetujuan / Disposisi", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>Aging membantu membaca umur antrian surat masuk dan keluar. Semakin banyak item pada bucket 48+ jam, semakin besar risiko bottleneck pelayanan surat.</div>");
		long max = max(new long[] { d.age0_24, d.age24_48, d.age48_72, d.age72Plus });
		renderFunnelRow(pch, "0 - 24 Jam", d.age0_24, max, "#16a34a", "Detail SLA 0 - 24 Jam",
				createSlaAge0_24Provider());
		renderFunnelRow(pch, "24 - 48 Jam", d.age24_48, max, "#f59e0b", "Detail SLA 24 - 48 Jam",
				createSlaAge24_48Provider());
		renderFunnelRow(pch, "48 - 72 Jam", d.age48_72, max, "#ea580c", "Detail SLA 48 - 72 Jam",
				createSlaAge48_72Provider());
		renderFunnelRow(pch, "> 72 Jam", d.age72Plus, max, "#dc2626", "Detail SLA > 72 Jam",
				createSlaAge72PlusProvider());
	}


	private void renderReportAlurSuratKeluarPaging(Component parent, AlurSuratDashboardData d) {
		renderPagedAlurReport(parent, "Report Alur Disposisi Surat Keluar",
				"Data alur keluar digrup per parent SuratKeluar. Setiap baris menampilkan rincian alur persetujuan yang sedang berjalan, dengan urutan alur berdasarkan ID ascending.",
				createKeluarProvider(createAllKeluarProvider()), false);
	}

	private void renderReportAlurSuratMasukPaging(Component parent, AlurSuratDashboardData d) {
		renderPagedAlurReport(parent, "Report Alur Disposisi Surat Masuk",
				"Data alur masuk digrup per parent SuratMasuk. Setiap baris menampilkan rincian alur persetujuan yang sedang berjalan, dengan urutan alur berdasarkan ID ascending.",
				createMasukProvider(createAllMasukProvider()), true);
	}

	private void renderPagedAlurReport(Component parent, String title, String description,
			final DetailDataProvider provider, boolean suratMasuk) {
		try {
			Panelchildren pch = createModernPanel(title, parent);
			appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:10px;'>"
					+ safeHtml(description) + "</div>");

			final DetailDataProvider parentGroupedProvider = createParentGroupedProviderV12(provider);

			final Paging paging = new Paging();
			paging.setPageSize(DETAIL_PAGE_SIZE);
			paging.setDetailed(true);
			paging.setParent(pch);

			final Grid grid = new Grid();
			grid.setParent(pch);
			grid.setSclass("dgrid fgrid");
			grid.setStyle(compactGridStyleV8());

			Columns columns = new Columns();
			columns.setParent(grid);
			addCompactColumnV8(columns, "Jenis", "80px");
			addCompactColumnV8(columns, "Tanggal", "90px");
			addCompactColumnV8(columns, "Kode / No Surat", "120px");
			addCompactColumnV8(columns, "Perihal", "230px");
			addCompactColumnV8(columns, "Rincian Alur Menunggu Persetujuan", null);
			addCompactColumnV8(columns, "Ringkasan", "170px");

			final Rows rows = new Rows();
			rows.setParent(grid);

			final EventListener refresh = new EventListener() {
				public void onEvent(Event event) throws Exception {
					Common.clear(rows);
					long total = parentGroupedProvider == null ? 0L : parentGroupedProvider.count();
					paging.setTotalSize(total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total);
					List dataRows = parentGroupedProvider == null ? new ArrayList()
							: parentGroupedProvider.list(paging.getActivePage() * DETAIL_PAGE_SIZE, DETAIL_PAGE_SIZE);
					if (dataRows == null || dataRows.isEmpty()) {
						Row row = new Row();
						row.setParent(rows);
						Label empty = new Label(ais.common.Common.getBahasaConfig("Tidak ada data alur pada filter ini."));
						empty.setStyle("padding:14px; color:#64748b; font-size:12px;");
						empty.setParent(row);
						return;
					}
					for (int i = 0; i < dataRows.size(); i++) {
						Row row = new Row();
						row.setParent(rows);
						renderAlurReportRow(row, dataRows.get(i));
					}
				}
			};
			paging.addEventListener("onPaging", refresh);
			refresh.onEvent(null);
		} catch (Exception e) {
			debugError("renderPagedAlurReport-" + title, e);
		}
	}

	private void renderAlurReportRow(Row row, Object status) {
		row.setStyle(compactRowStyleV8());
		if (status instanceof AlurParentGroupRow) {
			renderParentGroupCompactRowV12(row, (AlurParentGroupRow) status);
			return;
		}
		renderParentGroupCompactRowV12(row, buildParentGroupRowV12(status));
	}


	private void renderTabelDetailKontrolSla(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel("Tabel Detail Kontrol SLA - Pengawasan Utama", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
						+ "Tabel ini meniru konsep pengawasan utama SLA: melihat agenda/kode, perihal, konseptor, jabatan saat ini, waktu masuk, durasi berjalan, status SLA, dan action. "
						+ "Baris diprioritaskan dari item overdue/warning lalu aktivitas terbaru.</div>");

		if (d.slaWatchlist == null || d.slaWatchlist.isEmpty()) {
			appendEmptyState(pch, "Belum ada data pengawasan SLA pada filter saat ini.");
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='overflow:auto; border:1px solid #e5e7eb; border-radius:14px;'>")
				.append("<table style='width:100%; border-collapse:collapse; background:#ffffff; font-size:12px;'>")
				.append("<thead><tr style='background:#f8fafc; color:#334155;'>")
				.append("<th style='padding:10px; text-align:left;'>No. Agenda</th>")
				.append("<th style='padding:10px; text-align:left;'>Perihal Surat</th>")
				.append("<th style='padding:10px; text-align:left;'>Pengirim</th>")
				.append("<th style='padding:10px; text-align:left;'>Penerima</th>")
				.append("<th style='padding:10px; text-align:left;'>Waktu Masuk</th>")
				.append("<th style='padding:10px; text-align:right;'>Durasi Berjalan</th>")
				.append("<th style='padding:10px; text-align:left;'>Status SLA</th>")
				.append("<th style='padding:10px; text-align:left;'>Action</th>")
				.append("</tr></thead><tbody>");

		int max = d.slaWatchlist.size() > 12 ? 12 : d.slaWatchlist.size();
		for (int i = 0; i < max; i++) {
			AlurSuratItem item = (AlurSuratItem) d.slaWatchlist.get(i);
			sb.append("<tr style='border-top:1px solid #f1f5f9;'>")
					.append("<td style='padding:10px; font-weight:800; color:#0f172a;'>").append(safeHtml(item.kode))
					.append("</td><td style='padding:10px; color:#334155;'>").append(safeHtml(item.perihal))
					.append("<div style='font-size:10px; color:#64748b; margin-top:2px;'>").append(safeHtml(item.jenis))
					.append("</div></td><td style='padding:10px;'>").append(safeHtml(item.konseptor))
					.append("</td><td style='padding:10px;'>").append(safeHtml(item.pejabat))
					.append("</td><td style='padding:10px;'>").append(safeHtml(item.waktuMasuk))
					.append("</td><td style='padding:10px; text-align:right; font-weight:900;'>").append(item.slaHours)
					.append(" jam</td><td style='padding:10px;'>")
					.append("<span style='display:inline-block; border-radius:999px; padding:4px 8px; background:")
					.append(item.slaBg).append("; color:").append(item.slaColor)
					.append("; font-size:10px; font-weight:900;'>").append(safeHtml(item.statusSla))
					.append("</span></td><td style='padding:10px; color:").append(item.slaColor)
					.append("; font-weight:800;'>").append(safeHtml(item.action)).append("</td></tr>");
		}
		sb.append("</tbody></table></div>");
		appendHtml(pch, sb.toString());
	}

	private void renderPerformaSlaKonseptorJabatan(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel("Report Performa SLA per Konseptor / Jabatan Saat Ini", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:10px;'>"
						+ "Menampilkan performa tindakan berdasarkan pengirim/konseptor dan penerima/jabatan saat ini. "
						+ "Report ini sekarang memakai paging 10 data per halaman agar tetap ringan jika datanya banyak. "
						+ "Status mengikuti interval: 0-40 Sangat Tidak Baik, 41-70 Tidak Baik, 71-85 Cukup Baik, 86-99 Baik, 100 Sangat Baik.</div>");

		final List list = sortSlaPerformance(d.slaPerformanceMap);
		if (list == null || list.isEmpty()) {
			appendEmptyState(pch, "Belum ada data performa SLA per konseptor/jabatan pada filter ini.");
			return;
		}

		final Paging paging = new Paging();
		paging.setPageSize(DETAIL_PAGE_SIZE);
		paging.setDetailed(true);
		paging.setTotalSize(list.size());
		paging.setParent(pch);

		final Grid grid = new Grid();
		grid.setParent(pch);
		grid.setSclass("dgrid fgrid");
		grid.setStyle(compactGridStyleV8());

		Columns columns = new Columns();
		columns.setParent(grid);
		addCompactColumnV8(columns, "Jenis Surat", "105px");
		addCompactColumnV8(columns, "Pengirim", "180px");
		addCompactColumnV8(columns, "Penerima", "180px");
		addCompactColumnV8(columns, "Tepat Waktu", "95px");
		addCompactColumnV8(columns, "Tidak Tepat", "95px");
		addCompactColumnV8(columns, "Status SLA", "150px");
		addCompactColumnV8(columns, "Rinci", "95px");

		final Rows rows = new Rows();
		rows.setParent(grid);

		final EventListener refresh = new EventListener() {
			public void onEvent(Event event) throws Exception {
				Common.clear(rows);
				int start = paging.getActivePage() * DETAIL_PAGE_SIZE;
				int end = start + DETAIL_PAGE_SIZE;
				if (end > list.size()) {
					end = list.size();
				}
				for (int i = start; i < end; i++) {
					SlaPerformanceRow rowData = (SlaPerformanceRow) list.get(i);
					Row row = new Row();
					row.setStyle(compactRowStyleV8());
					row.setParent(rows);
					renderSlaPerformanceRowV8(row, rowData);
				}
			}
		};
		paging.addEventListener("onPaging", refresh);
		try {
			refresh.onEvent(null);
		} catch (Exception e) {
			debugError("renderPerformaSlaKonseptorJabatan-refresh", e);
		}
	}

	private void renderSlaPerformanceRowV8(Row row, final SlaPerformanceRow rowData) {
		long pct = rowData.totalClosed <= 0L ? 0L : Math.round((rowData.tepatWaktu * 100.0d) / rowData.totalClosed);
		long latePct = rowData.totalClosed <= 0L ? 0L : 100L - pct;
		String status = getSlaPerformanceStatus(pct);
		String color = getSlaPerformanceColor(pct);

		addCompactCellV8(row, rowData.jenisSurat, false);
		addCompactCellV8(row, rowData.konseptor, true);
		addCompactCellV8(row, rowData.jabatan, false);
		addCompactCellV8(row, pct + "%", true, "#166534", "right");
		addCompactCellV8(row, latePct + "%", true, "#991b1b", "right");
		addCompactCellV8(row, status, true, color, "left");
		addSlaPerformanceDetailButtonCellV11(row, rowData);
	}


	private void addSlaPerformanceDetailButtonCellV11(Row row, final SlaPerformanceRow rowData) {
		Hbox box = new Hbox();
		box.setAlign("center");
		box.setPack("center");
		box.setStyle("padding:0;");
		box.setParent(row);

		A detail = new A("Lihat Rinci");
		detail.setTooltiptext("Klik untuk melihat data detail pengirim dan penerima ini");
		detail.setStyle("display:inline-block; padding:3px 8px; border-radius:999px; background:#eff6ff;"
				+ "border:1px solid #bfdbfe; color:#1d4ed8; font-size:10px; font-weight:800;"
				+ "text-decoration:none; line-height:1.2; white-space:nowrap;");
		detail.setParent(box);
		detail.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Performa SLA: " + rowData.konseptor + " → " + rowData.jabatan,
						createSlaPerformanceDetailProviderV11(rowData.konseptor, rowData.jabatan));
			}
		});
	}

	private DetailDataProvider createSlaPerformanceDetailProviderV11(final String pengirim, final String penerima) {
		return createRuntimeFilteredProvider(createAllProvider(), new RuntimeRowFilter() {
			public boolean accept(Object row) {
				return normalizePairKeyV11(getPengirimAlurName(row)).equals(normalizePairKeyV11(pengirim))
						&& normalizePairKeyV11(getPenerimaAlurName(row)).equals(normalizePairKeyV11(penerima));
			}
		});
	}

	private String normalizePairKeyV11(String value) {
		return value == null ? "" : value.trim().toLowerCase();
	}


		private void renderTrendDistribusiSla(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel("Tren Kepatuhan & Distribusi Status SLA", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
						+ "menggantikan line/pie chart dengan visual bar ringan yang kompatibel dengan ZK lama: tren kepatuhan per bulan serta distribusi Selesai Tepat Waktu, Selesai Terlambat, dan Sedang Berjalan.</div>");

		Hbox row = new Hbox();
		row.setWidth("100%");
		row.setStyle("gap:12px; flex-wrap:wrap;");
		row.setParent(pch);

		Vbox left = new Vbox();
		left.setStyle("flex:1 1 360px;");
		left.setParent(row);
		appendHtml(left, "<div style='font-size:13px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Tren Kepatuhan SLA Bulanan</div>");
		renderTrendComplianceBars(left, d);

		Vbox right = new Vbox();
		right.setStyle("flex:1 1 360px;");
		right.setParent(row);
		appendHtml(right, "<div style='font-size:13px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Distribusi Status SLA Saat Ini</div>");
		long total = d.selesaiTepatWaktu + d.selesaiTerlambat + d.menunggu;
		appendHtml(right, gaugeHtml("Selesai Tepat Waktu", percent(d.selesaiTepatWaktu, Math.max(1L, total)),
				d.selesaiTepatWaktu + " tahap selesai sesuai target 24 jam.", "#16a34a"));
		appendHtml(right, gaugeHtml("Selesai Terlambat", percent(d.selesaiTerlambat, Math.max(1L, total)),
				d.selesaiTerlambat + " tahap selesai melewati target.", "#dc2626"));
		appendHtml(right, gaugeHtml("Sedang Berjalan", percent(d.menunggu, Math.max(1L, total)),
				d.menunggu + " tahap masih aktif / menunggu.", "#f59e0b"));
	}

	private void renderTrendComplianceBars(Component parent, AlurSuratDashboardData d) {
		if (d.trendSlaTotal == null || d.trendSlaTotal.isEmpty()) {
			appendEmptyState(parent, "Belum ada data tren SLA selesai pada filter ini.");
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (String key : d.trendSlaTotal.keySet()) {
			Integer totalObj = (Integer) d.trendSlaTotal.get(key);
			Integer onObj = (Integer) d.trendSlaOnTime.get(key);
			long total = totalObj == null ? 0L : totalObj.longValue();
			long on = onObj == null ? 0L : onObj.longValue();
			long pct = total <= 0L ? 0L : Math.round((on * 100.0d) / total);
			sb.append("<div style='padding:8px 0; border-bottom:1px solid #f1f5f9;'>")
					.append("<div style='display:flex; justify-content:space-between; gap:10px;'>")
					.append("<div style='font-size:12px; font-weight:800; color:#334155;'>").append(safeHtml(key))
					.append("</div><div style='font-size:12px; font-weight:900; color:#0f172a;'>")
					.append(pct).append("%</div></div>")
					.append("<div style='margin-top:7px; height:9px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>")
					.append("<div style='height:9px; width:").append(pct)
					.append("%; background:").append(pct >= 95L ? "#16a34a" : (pct >= 85L ? "#f59e0b" : "#dc2626"))
					.append("; border-radius:999px;'></div></div></div>");
		}
		appendHtml(parent, sb.toString());
	}


	private void renderSebaranAlur(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel("Top Alur / Tahap Persetujuan", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; margin-bottom:10px;'>Menunjukkan tahap/alur yang paling sering muncul pada data status persetujuan surat masuk dan keluar. Klik angka untuk melihat data detail; jumlah popup disamakan dengan angka counter.</div>");
		renderCounterListClickable(pch, d.perAlur, "Belum ada data alur untuk filter ini.", "ALUR", "Detail Top Alur / Tahap");
	}

	private void renderBebanPejabat(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel("Beban Pejabat / Jabatan", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; margin-bottom:10px;'>Menunjukkan pejabat/jabatan yang paling sering terlibat dalam alur persetujuan surat masuk dan keluar. Klik angka untuk melihat data detail.</div>");
		renderCounterListClickable(pch, d.perPejabat, "Belum ada data pejabat/jabatan untuk filter ini.", "PEJABAT", "Detail Beban Pejabat / Jabatan");
	}

	private void renderSlaRiskByPejabat(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel("Risiko SLA per Pejabat / Jabatan", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; margin-bottom:10px;'>Daftar ini membantu menentukan titik eskalasi SLA. Nilai tertinggi berarti paling banyak item overdue/warning. Klik angka untuk melihat detail risiko; jumlah popup disamakan dengan angka counter.</div>");
		renderCounterListClickable(pch, d.slaRiskPerPejabat, "Belum ada risiko SLA per pejabat/jabatan.", "SLA_RISK_PEJABAT", "Detail Risiko SLA per Pejabat / Jabatan");
	}

	private void renderAktivitasTerbaru(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel("Aktivitas Alur Terbaru", parent);
		if (d.recentItems == null || d.recentItems.isEmpty()) {
			appendEmptyState(pch, "Belum ada aktivitas terbaru untuk filter ini.");
			return;
		}
		for (int i = 0; i < d.recentItems.size(); i++) {
			AlurSuratItem item = d.recentItems.get(i);
			appendHtml(pch,
					"<div style='display:flex; gap:10px; padding:11px 0; border-bottom:1px solid #f1f5f9;'>"
							+ "<div style='width:10px; height:10px; margin-top:5px; border-radius:999px; background:"
							+ item.color + ";'></div>" + "<div style='flex:1; min-width:0;'>"
							+ "<div style='font-size:12px; font-weight:800; color:#0f172a;'>"
							+ safeHtml(item.jenis) + " · " + safeHtml(item.kode) + " - " + safeHtml(item.perihal)
							+ "</div>" + "<div style='font-size:11px; color:#64748b; margin-top:3px;'>"
							+ safeHtml(item.alur) + " · " + safeHtml(item.pejabat) + "</div>"
							+ "<div style='font-size:11px; color:#94a3b8; margin-top:3px;'>" + safeHtml(item.waktu)
							+ " · SLA " + item.slaHours + " jam</div></div>"
							+ "<div style='font-size:10px; color:#1d4ed8; background:#dbeafe; border-radius:999px; padding:4px 8px; height:14px; white-space:nowrap;'>"
							+ safeHtml(item.status) + "</div></div>");
		}
	}

	private void renderBottleneckAlur(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel("Bottleneck Alur, Klasifikasi & Unit", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>Ringkasan ini membaca alur, klasifikasi, dan unit kerja yang paling sering memunculkan antrian/risiko SLA. Klik angka untuk membuka data detail; jumlah popup disamakan dengan angka counter.</div>");
		Hbox box = new Hbox();
		box.setWidth("100%");
		box.setStyle("gap:12px; flex-wrap:wrap;");
		box.setParent(pch);
		Vbox left = new Vbox();
		left.setStyle("flex:1 1 300px;");
		left.setParent(box);
		appendHtml(left, "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Klasifikasi terbanyak</div>");
		renderCounterListClickable(left, d.perKlasifikasi, "Belum ada data klasifikasi.", "KLASIFIKASI", "Detail Bottleneck Klasifikasi");
		Vbox right = new Vbox();
		right.setStyle("flex:1 1 300px;");
		right.setParent(box);
		appendHtml(right, "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Satker terbanyak</div>");
		renderCounterListClickable(right, d.perSatker, "Belum ada data satuan kerja.", "SATKER", "Detail Bottleneck Unit / Satker");
	}

	private void renderSlaExecutionPlan(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel("Prioritas Eksekusi Kontrol SLA", parent);
		String p1 = d.slaOverdue > 0 ? "Tuntaskan " + d.slaOverdue + " item yang sudah overdue lebih dari "
				+ SLA_OVERDUE_HOURS + " jam." : "Tidak ada item overdue pada filter ini.";
		String p2 = d.slaWarning > 0 ? "Pantau " + d.slaWarning + " item warning agar tidak berubah menjadi overdue."
				: "Tidak ada item warning pada filter ini.";
		String p3 = topLabel(d.slaRiskPerPejabat, "Belum ada pejabat/jabatan dominan")
				+ " perlu dipantau sebagai titik potensi bottleneck SLA.";
		String p4 = topLabel(d.perAlur, "Belum ada alur dominan")
				+ " menjadi alur/tahap dengan volume tertinggi pada filter ini.";

		appendHtml(pch, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(230px,1fr)); gap:12px;'>"
				+ actionPlanCard("1", "Overdue", p1, "#fee2e2", "#991b1b")
				+ actionPlanCard("2", "Warning", p2, "#ffedd5", "#9a3412")
				+ actionPlanCard("3", "Eskalasi Pejabat", p3, "#ede9fe", "#5b21b6")
				+ actionPlanCard("4", "Kontrol Alur", p4, "#dbeafe", "#1e40af") + "</div>");
	}

	private void renderWorkflowMatrix(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel("Matriks Kesehatan Workflow Alur Surat", parent);
		long completion = percent(d.disetujui + d.selesai, Math.max(1L, d.totalTahap));
		long queue = percent(d.menunggu, Math.max(1L, d.totalTahap));
		long risk = percent(d.slaOverdue + d.slaWarning, Math.max(1L, d.totalTahap));
		long correction = percent(d.ditolak + d.direvisi, Math.max(1L, d.totalTahap));
		long score = 100 - ((queue * 35) / 100) - ((risk * 35) / 100) - ((correction * 15) / 100)
				+ ((completion * 15) / 100);
		if (score < 0) {
			score = 0;
		}
		if (score > 100) {
			score = 100;
		}
		String status = score >= 80 ? "Sehat" : (score >= 60 ? "Perlu Dipantau" : "Prioritas Perbaikan");
		String bg = score >= 80 ? "#dcfce7" : (score >= 60 ? "#fef3c7" : "#fee2e2");
		String color = score >= 80 ? "#166534" : (score >= 60 ? "#92400e" : "#991b1b");

		appendHtml(pch, "<div style='display:flex; gap:14px; flex-wrap:wrap; align-items:stretch;'>"
				+ "<div style='flex:1 1 230px; border-radius:16px; padding:16px; color:#ffffff; background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); box-shadow:0 12px 24px rgba(37,99,235,.18);'>"
				+ "<div style='font-size:11px; letter-spacing:.08em; text-transform:uppercase; opacity:.82;'>Workflow Health Score</div>"
				+ "<div style='font-size:46px; line-height:1; font-weight:900; margin-top:10px;'>" + score + "</div>"
				+ "<div style='display:inline-block; margin-top:12px; border-radius:999px; background:" + bg
				+ "; color:" + color + "; padding:5px 10px; font-size:11px; font-weight:800;'>" + status + "</div>"
				+ "</div>" + "<div style='flex:2 1 420px;'>"
				+ gaugeHtml("Completion Rate", completion, "Tahap disetujui/selesai terhadap total tahap.", "#16a34a")
				+ gaugeHtml("Queue Pressure", queue, "Tahap yang masih menunggu.", "#f59e0b")
				+ gaugeHtml("SLA Risk", risk, "Tahap warning/overdue SLA.", "#dc2626")
				+ gaugeHtml("Correction Risk", correction, "Tahap revisi/ditolak.", "#7c3aed") + "</div></div>");
	}

	private void renderSlaAuditReadiness(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel("Kesiapan Audit SLA & Tata Kelola Surat", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>Kesiapan audit dilihat dari keterisian waktu persetujuan/penolakan, kode surat, perihal, klasifikasi, dan jejak pejabat/jabatan.</div>");
		Hbox box = new Hbox();
		box.setWidth("100%");
		box.setStyle("gap:12px; flex-wrap:wrap;");
		box.setParent(pch);
		createPressureCard(box, "Tanpa Kode", d.tanpaKode, "Kode surat kosong", "#fee2e2", "#991b1b",
				"Detail Surat Tanpa Kode", createTanpaKodeProvider());
		createPressureCard(box, "Tanpa Perihal", d.tanpaPerihal, "Perihal belum lengkap", "#ffedd5", "#9a3412",
				"Detail Surat Tanpa Perihal", createTanpaPerihalProvider());
		createPressureCard(box, "Tanpa Alur", d.tanpaAlur, "Alur belum terbaca", "#ede9fe", "#5b21b6",
				"Detail Status Tanpa Alur", createTanpaAlurProvider());
		createPressureCard(box, "Tanpa Pejabat", d.tanpaPejabat, "Pejabat/jabatan belum terbaca", "#dbeafe",
				"#1e40af", "Detail Status Tanpa Pejabat", createTanpaPejabatProvider());
	}

	private void renderJenisSuratComparison(Component parent, AlurSuratDashboardData d) {
		Panelchildren pch = createModernPanel("Komposisi Surat Masuk vs Surat Keluar", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>membandingkan volume dan risiko antara alur surat masuk dan surat keluar. Gunakan untuk melihat apakah bottleneck lebih dominan di surat masuk atau keluar.</div>");
		long max = max(new long[] { d.totalKeluar, d.totalMasuk, d.pendingKeluar, d.pendingMasuk, d.overdueKeluar,
				d.overdueMasuk });
		renderFunnelRow(pch, "Total Tahap Keluar", d.totalKeluar, max, "#2563eb", "Detail Semua Alur Surat Keluar",
				createKeluarProvider(createAllKeluarProvider()));
		renderFunnelRow(pch, "Total Tahap Masuk", d.totalMasuk, max, "#0891b2", "Detail Semua Alur Surat Masuk",
				createMasukProvider(createAllMasukProvider()));
		renderFunnelRow(pch, "Menunggu Keluar", d.pendingKeluar, max, "#f59e0b", "Detail Menunggu Surat Keluar",
				createKeluarProvider(createPendingKeluarProvider()));
		renderFunnelRow(pch, "Menunggu Masuk", d.pendingMasuk, max, "#f97316", "Detail Menunggu Surat Masuk",
				createMasukProvider(createPendingMasukProvider()));
		renderFunnelRow(pch, "Overdue Keluar", d.overdueKeluar, max, "#dc2626", "Detail Overdue Surat Keluar",
				createSlaOverdueKeluarProvider());
		renderFunnelRow(pch, "Overdue Masuk", d.overdueMasuk, max, "#b91c1c", "Detail Overdue Surat Masuk",
				createSlaOverdueMasukProvider());
	}

	private void createPressureCard(Component parent, String title, long value, String desc, String bg, String color,
			String detailTitle, DetailDataProvider provider) {
		Vbox card = new Vbox();
		card.setStyle("flex:1 1 170px; min-width:170px; border-radius:14px; padding:13px; background:" + bg
				+ "; border:1px solid rgba(15,23,42,.08); box-sizing:border-box;");
		card.setParent(parent);
		createDetailNumber(card, String.valueOf(value), detailTitle, provider,
				"font-size:25px; line-height:1; font-weight:900; color:" + color + "; text-decoration:none;");
		appendHtml(card, "<div style='font-size:12px; font-weight:900; color:" + color + "; margin-top:8px;'>"
				+ safeHtml(title) + "</div>" + "<div style='font-size:11px; color:" + color
				+ "; opacity:.82; margin-top:5px;'>" + safeHtml(desc) + "</div>");
	}

	private AlurSuratDashboardData loadDashboardData() {
		AlurSuratDashboardData d = new AlurSuratDashboardData();
		d.mulai = dashboardFilterMulai;
		d.sampai = dashboardFilterSampai;
		d.satker = dashboardFilterSatker;
		d.keyword = dashboardFilterKeyword;

		try {
			d.totalKeluar = includeKeluar() ? countProvider(createKeluarProvider(createAllKeluarProvider())) : 0L;
			d.totalMasuk = includeMasuk() ? countProvider(createMasukProvider(createAllMasukProvider())) : 0L;
			d.totalTahap = d.totalKeluar + d.totalMasuk;

			d.pendingKeluar = includeKeluar() ? countProvider(createKeluarProvider(createPendingKeluarProvider())) : 0L;
			d.pendingMasuk = includeMasuk() ? countProvider(createMasukProvider(createPendingMasukProvider())) : 0L;
			d.menunggu = d.pendingKeluar + d.pendingMasuk;

			d.disetujui = countProvider(createApprovedProvider());
			d.ditolak = countProvider(createRejectedProvider());
			d.direvisi = countProvider(createRevisedProvider());
			d.selesai = countProvider(createFinishedProvider());

			d.overdueKeluar = includeKeluar() ? countProvider(createSlaOverdueKeluarProvider()) : 0L;
			d.overdueMasuk = includeMasuk() ? countProvider(createSlaOverdueMasukProvider()) : 0L;
			d.slaOverdue = d.overdueKeluar + d.overdueMasuk;
			d.slaWarning = countProvider(createSlaWarningProvider());
			d.slaAman = countProvider(createSlaSafeProvider());

			d.tanpaKode = countProvider(createTanpaKodeProvider());
			d.tanpaPerihal = countProvider(createTanpaPerihalProvider());
			d.tanpaAlur = countProvider(createTanpaAlurProvider());
			d.tanpaPejabat = countProvider(createTanpaPejabatProvider());
		} catch (Exception e) {
			debugError("loadDashboardData-counts", e);
		}

		try {
			List rows = new ArrayList();
			if (includeKeluar()) {
				rows.addAll(listProvider(createKeluarProvider(createAllKeluarProvider()), 0, DASHBOARD_SAMPLE_LIMIT));
			}
			if (includeMasuk()) {
				rows.addAll(listProvider(createMasukProvider(createAllMasukProvider()), 0, DASHBOARD_SAMPLE_LIMIT));
			}
			analyzeRows(d, rows);
		} catch (Exception e) {
			debugError("loadDashboardData-sample", e);
		}

		return d;
	}

	private void analyzeRows(AlurSuratDashboardData d, List rows) {
		if (rows == null) {
			return;
		}
		preloadParentSequenceCacheV7(rows);
		long finishedSlaCount = 0L;
		long finishedSlaTotal = 0L;
		for (int i = 0; i < rows.size(); i++) {
			Object row = rows.get(i);
			if (row == null) {
				continue;
			}

			addCounter(d.perAlur, getAlurName(row));
			addCounter(d.perPejabat, getPejabatName(row));
			addCounter(d.perKlasifikasi, getKlasifikasiName(row));
			addCounter(d.perSatker, getSatkerName(row));

			long slaHours = calculateSlaHours(row);
			boolean pending = isPending(row);
			AlurSuratItem slaItem = buildRecentItem(row);
			updateSlaPerformance(d, row, slaHours, pending);

			if (pending) {
				if (slaHours > SLA_OVERDUE_HOURS) {
					addCounter(d.slaRiskPerPejabat, getPejabatName(row));
					if (slaHours > 72L) {
						d.age72Plus++;
					} else {
						d.age48_72++;
					}
					if (d.slaWatchlist.size() < 25) {
						d.slaWatchlist.add(slaItem);
					}
				} else if (slaHours > SLA_WARNING_HOURS) {
					addCounter(d.slaRiskPerPejabat, getPejabatName(row));
					d.age24_48++;
					if (d.slaWatchlist.size() < 25) {
						d.slaWatchlist.add(slaItem);
					}
				} else {
					d.age0_24++;
					if (d.slaWatchlist.size() < 10) {
						d.slaWatchlist.add(slaItem);
					}
				}
			} else {
				finishedSlaCount++;
				finishedSlaTotal += slaHours;
				d.slaClosed++;
				if (slaHours <= SLA_WARNING_HOURS) {
					d.selesaiTepatWaktu++;
				} else {
					d.selesaiTerlambat++;
				}
				addTrendSla(d, row, slaHours <= SLA_WARNING_HOURS);
			}

			if (d.recentItems.size() < 10) {
				d.recentItems.add(slaItem);
			}
		}
		d.avgSlaTotalHours = finishedSlaTotal;
		d.avgSlaClosedCount = finishedSlaCount;
		d.avgSlaExcludedPendingCount = rows == null ? 0L : Math.max(0L, rows.size() - finishedSlaCount);
		d.avgSlaHours = finishedSlaCount <= 0L ? 0.0d : (finishedSlaTotal * 1.0d / finishedSlaCount);
		d.slaCompliancePct = d.slaClosed <= 0L ? 100L : Math.round((d.selesaiTepatWaktu * 100.0d) / d.slaClosed);
	}

	private void updateSlaPerformance(AlurSuratDashboardData d, Object row, long slaHours, boolean pending) {
		/*
		 * V11:
		 * Report performa SLA digabung berdasarkan pasangan Pengirim -> Penerima.
		 * Jika ada data berulang dengan pengirim dan penerima yang sama, maka dihitung
		 * dalam satu baris saja, meskipun berasal dari surat masuk/keluar berbeda.
		 */
		String pengirim = getPengirimAlurName(row);
		String penerima = getPenerimaAlurName(row);
		String key = normalizePairKeyV11(pengirim) + "||" + normalizePairKeyV11(penerima);

		SlaPerformanceRow perf = (SlaPerformanceRow) d.slaPerformanceMap.get(key);
		if (perf == null) {
			perf = new SlaPerformanceRow();
			perf.jenisSurat = isMasuk(row) ? "Surat Masuk" : "Surat Keluar";
			perf.konseptor = pengirim;
			perf.jabatan = penerima;
			d.slaPerformanceMap.put(key, perf);
		} else if (perf.jenisSurat != null && perf.jenisSurat.indexOf(isMasuk(row) ? "Surat Masuk" : "Surat Keluar") < 0) {
			perf.jenisSurat = "Gabungan";
		}

		if (pending) {
			perf.outstanding++;
			if (slaHours > SLA_OVERDUE_HOURS) {
				perf.overdue++;
			}
			return;
		}

		perf.totalClosed++;
		if (slaHours <= SLA_WARNING_HOURS) {
			perf.tepatWaktu++;
		} else {
			perf.tidakTepatWaktu++;
		}
	}

	private void addTrendSla(AlurSuratDashboardData d, Object row, boolean onTime) {
		String key = formatTrendMonth(getTanggal(row));
		Integer total = (Integer) d.trendSlaTotal.get(key);
		d.trendSlaTotal.put(key, total == null ? 1 : total + 1);
		if (onTime) {
			Integer on = (Integer) d.trendSlaOnTime.get(key);
			d.trendSlaOnTime.put(key, on == null ? 1 : on + 1);
		} else if (!d.trendSlaOnTime.containsKey(key)) {
			d.trendSlaOnTime.put(key, 0);
		}
	}

	private AlurSuratItem buildRecentItem(Object row) {
		AlurSuratItem item = new AlurSuratItem();
		item.jenis = isMasuk(row) ? "Surat Masuk" : "Surat Keluar";
		item.kode = getKode(row);
		item.perihal = getPerihal(row);
		item.alur = getAlurName(row);
		item.pejabat = getPenerimaAlurName(row);
		item.konseptor = getPengirimAlurName(row);
		item.status = getStatusText(row);
		item.waktu = formatDateTime(getStatusTime(row));
		item.waktuMasuk = formatDateTime(getStartTime(row));
		item.slaHours = calculateSlaHours(row);
		item.color = getStatusColor(row);
		item.statusSla = getSlaStatusLabel(row);
		item.slaColor = getSlaStatusColor(row);
		item.slaBg = getSlaStatusBg(row);
		item.action = getSlaAction(row);
		return item;
	}


	/* ACCESS_CONTROL_SURAT_VISIBILITY_REUSE_2026_05_28
	 * Helper reusable untuk memastikan role dengan getMelihatSemuaSurat()==true
	 * atau username whitelist konfigurasi dapat melihat semua surat.
	 * Selain itu, filter mengikuti pola SuratKeluarAction/SuratMasukAction.
	 */
	private boolean bolehMelihatSemuaSurat(Tbmuser user) {
		try {
			if (user == null) {
				return false;
			}
			Tbmrole role = user.hakAkses();
			if (role != null && role.getRoleId() != null && Boolean.TRUE.equals(role.getMelihatSemuaSurat())) {
				return true;
			}
			return usernameAdaDiWhitelistLihatSemuaSurat(user);
		} catch (Exception e) {
			debugError("bolehMelihatSemuaSurat", e);
			return false;
		}
	}

	private boolean usernameAdaDiWhitelistLihatSemuaSurat(Tbmuser user) {
		try {
			if (user == null || user.getUserId() == null) {
				return false;
			}
			String nilai = Common.getKonfigurasi("daftar_username_yg_bisa_lihat_semua_surat", "").getNilai();
			String[] daftarUsernameYgBisaLihatSemua = nilai == null ? new String[0] : nilai.split(",");
			for (int i = 0; i < daftarUsernameYgBisaLihatSemua.length; i++) {
				String s = daftarUsernameYgBisaLihatSemua[i];
				if (s != null && user.getUserId() != null && s.trim().equalsIgnoreCase(user.getUserId())) {
					return true;
				}
			}
		} catch (Exception e) {
			debugError("usernameAdaDiWhitelistLihatSemuaSurat", e);
		}
		return false;
	}

	private Criterion falseCriterionSuratAccess() {
		return Restrictions.sqlRestriction("false");
	}

	private Criterion trueCriterionSuratAccess() {
		return Restrictions.sqlRestriction("true");
	}

	private Criterion eqOrFalse(String propertyName, Object value) {
		return value == null ? falseCriterionSuratAccess() : Restrictions.eq(propertyName, value);
	}

	private Criterion createSuratKeluarVisibilityCriterion(String suratPrefix, String klasifikasiPrefix) {
		if (tbmuser == null) {
			return trueCriterionSuratAccess();
		}
		try {
			Tbmrole tbmrole = tbmuser.hakAkses();
			if (bolehMelihatSemuaSurat(tbmuser)) {
				return trueCriterionSuratAccess();
			}

			if (tbmrole != null && tbmrole.getRoleId() != null && !Boolean.TRUE.equals(tbmrole.getMelihatSemuaSurat())
					&& tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
				Criterion c = eqOrFalse(suratPrefix + "dosen", tbmuser.getDosen());
				c = Restrictions.or(c, eqOrFalse(suratPrefix + "guru", tbmuser.getGuru()));
				c = Restrictions.or(c, Restrictions.eq(suratPrefix + "konseptor", tbmuser));
				c = Restrictions.or(c, Restrictions.ilike(klasifikasiPrefix + "kodeGrupPengguna",
						";" + tbmrole.getRoleId() + ";", MatchMode.ANYWHERE));
				return c;
			}
			if (tbmuser.getMahasiswa() != null) {
				return Restrictions.eq(suratPrefix + "mahasiswa", tbmuser.getMahasiswa());
			}
			if (tbmuser.ambilDosen() != null) {
				return Restrictions.eq(suratPrefix + "dosen", tbmuser.ambilDosen());
			}
			if (tbmuser.getSiswa() != null) {
				return Restrictions.eq(suratPrefix + "siswa", tbmuser.getSiswa());
			}
			if (tbmuser.ambilGuru() != null) {
				return Restrictions.eq(suratPrefix + "guru", tbmuser.ambilGuru());
			}
		} catch (Exception e) {
			debugError("createSuratKeluarVisibilityCriterion", e);
		}
		return trueCriterionSuratAccess();
	}

	private Criterion createSuratMasukVisibilityCriterion(String suratPrefix, String klasifikasiPrefix,
			String statusPrefix) {
		if (tbmuser == null) {
			return trueCriterionSuratAccess();
		}
		try {
			Tbmrole tbmrole = tbmuser.hakAkses();
			if (bolehMelihatSemuaSurat(tbmuser)) {
				return trueCriterionSuratAccess();
			}

			if (tbmrole != null && tbmrole.getRoleId() != null && !Boolean.TRUE.equals(tbmrole.getMelihatSemuaSurat())
					&& tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
				Criterion c = Restrictions.eq(suratPrefix + "konseptor", tbmuser);
				c = Restrictions.or(c, Restrictions.ilike(klasifikasiPrefix + "kodeGrupPengguna",
						";" + tbmrole.getRoleId() + ";", MatchMode.ANYWHERE));
				return c;
			}
			if (statusPrefix != null && tbmuser.getMahasiswa() != null) {
				return Restrictions.eq(statusPrefix + "mahasiswa", tbmuser.getMahasiswa());
			}
			if (statusPrefix != null && tbmuser.getSiswa() != null) {
				return Restrictions.eq(statusPrefix + "siswa", tbmuser.getSiswa());
			}
		} catch (Exception e) {
			debugError("createSuratMasukVisibilityCriterion", e);
		}
		return trueCriterionSuratAccess();
	}

	private Criteria createBaseKeluarCriteria(Session session, boolean order) {
		Criteria criteria = session.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
				.add(Restrictions.isNotNull("kodeUnik"))
				.createAlias("suratKeluar", "suratKeluar")
				.createAlias("alurPersetujuanSuratKeluar", "alur", Criteria.LEFT_JOIN)
				.createAlias("pejabat", "pejabat", Criteria.LEFT_JOIN)
				.createAlias("jenisJabatan", "jenisJabatan", Criteria.LEFT_JOIN)
				.createAlias("suratKeluar.klasifikasiSuratKeluar", "klasifikasi", Criteria.LEFT_JOIN)
				.createAlias("suratKeluar.satuanKerja", "satker", Criteria.LEFT_JOIN);

		if (dashboardFilterMulai != null) {
			criteria.add(Restrictions.ge("suratKeluar.tanggal", dashboardFilterMulai));
		}
		if (dashboardFilterSampai != null) {
			criteria.add(Restrictions.le("suratKeluar.tanggal", dashboardFilterSampai));
		}
		if (dashboardFilterSatker != null) {
			criteria.add(Restrictions.eq("suratKeluar.satuanKerja", dashboardFilterSatker));
		}

		criteria.add(createSuratKeluarVisibilityCriterion("suratKeluar.", "klasifikasi."));

		if (dashboardFilterKeyword != null && !dashboardFilterKeyword.trim().isEmpty()) {
			String keyword = dashboardFilterKeyword.trim();
			criteria.add(orAll(new Criterion[] {
					Restrictions.ilike("suratKeluar.kode", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("suratKeluar.agenda", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("suratKeluar.nama", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("suratKeluar.perihal", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("catatanDisposisi", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("alur.nama", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("klasifikasi.nama", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("satker.nama", keyword, MatchMode.ANYWHERE) }));
		}

		if (order) {
			// V6: urutkan alur berdasarkan parent SuratKeluar lalu ID status ascending.
			criteria.addOrder(Order.asc("suratKeluar.id"));
			criteria.addOrder(Order.asc("id"));
		}
		return criteria;
	}

	private Criteria createBaseMasukCriteria(Session session, boolean order) {
		Criteria criteria = session.createCriteria(AlurPersetujuanSuratMasukStatus.class)
				.add(Restrictions.isNotNull("kodeUnik"))
				.createAlias("suratMasuk", "suratMasuk")
				.createAlias("alurPersetujuanSuratMasuk", "alur", Criteria.LEFT_JOIN)
				.createAlias("pejabat", "pejabat", Criteria.LEFT_JOIN)
				.createAlias("jenisJabatan", "jenisJabatan", Criteria.LEFT_JOIN)
				.createAlias("suratMasuk.klasifikasiSuratMasuk", "klasifikasi", Criteria.LEFT_JOIN)
				.createAlias("suratMasuk.satuanKerja", "satker", Criteria.LEFT_JOIN);

		if (dashboardFilterMulai != null) {
			criteria.add(Restrictions.ge("suratMasuk.tanggal", dashboardFilterMulai));
		}
		if (dashboardFilterSampai != null) {
			criteria.add(Restrictions.le("suratMasuk.tanggal", dashboardFilterSampai));
		}
		if (dashboardFilterSatker != null) {
			criteria.add(Restrictions.eq("suratMasuk.satuanKerja", dashboardFilterSatker));
		}

		criteria.add(createSuratMasukVisibilityCriterion("suratMasuk.", "klasifikasi.", ""));

		if (dashboardFilterKeyword != null && !dashboardFilterKeyword.trim().isEmpty()) {
			String keyword = dashboardFilterKeyword.trim();
			criteria.add(orAll(new Criterion[] {
					Restrictions.ilike("suratMasuk.kode", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("suratMasuk.noSurat", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("suratMasuk.asal", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("suratMasuk.nama", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("suratMasuk.perihal", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("suratMasuk.ringkasan", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("catatanDisposisi", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("alur.nama", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("klasifikasi.nama", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("satker.nama", keyword, MatchMode.ANYWHERE) }));
		}

		if (order) {
			// V6: urutkan alur berdasarkan parent SuratMasuk lalu ID status ascending.
			criteria.addOrder(Order.asc("suratMasuk.id"));
			criteria.addOrder(Order.asc("id"));
		}
		return criteria;
	}

	private Criterion orAll(Criterion[] criterions) {
		if (criterions == null || criterions.length == 0) {
			return Restrictions.sqlRestriction("true");
		}
		Criterion result = criterions[0];
		for (int i = 1; i < criterions.length; i++) {
			result = Restrictions.or(result, criterions[i]);
		}
		return result;
	}

	/**
	 * Kontrak callback/strategi bersarang milik {@link DasboardAlurSurat}. Tipe ini memisahkan satu variasi
	 * perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DasboardAlurSurat} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
	 * implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code build}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see DasboardAlurSurat
	 */
	private interface CriteriaBuilder {
		Criteria build(Session session, boolean order) throws Exception;
	}

	/**
	 * Kontrak callback/strategi bersarang milik {@link DasboardAlurSurat}. Tipe ini memisahkan satu variasi
	 * perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DasboardAlurSurat} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
	 * implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code count()}, {@code list}(). Aturan
	 * bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see DasboardAlurSurat
	 */
	private interface DetailDataProvider {
		long count() throws Exception;

		List list(int first, int max) throws Exception;
	}

	/**
	 * Kontrak callback/strategi bersarang milik {@link DasboardAlurSurat}. Tipe ini memisahkan satu variasi
	 * perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DasboardAlurSurat} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
	 * implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code accept}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see DasboardAlurSurat
	 */
	private interface RuntimeRowFilter {
		boolean accept(Object row);
	}

	private CriteriaBuilder createAllKeluarProvider() {
		return new CriteriaBuilder() {
			public Criteria build(Session session, boolean order) {
				return createBaseKeluarCriteria(session, order);
			}
		};
	}

	private CriteriaBuilder createAllMasukProvider() {
		return new CriteriaBuilder() {
			public Criteria build(Session session, boolean order) {
				return createBaseMasukCriteria(session, order);
			}
		};
	}

	private CriteriaBuilder createPendingKeluarProvider() {
		return new CriteriaBuilder() {
			public Criteria build(Session session, boolean order) {
				return createBaseKeluarCriteria(session, order).add(pendingKeluarCriterion());
			}
		};
	}

	private CriteriaBuilder createPendingMasukProvider() {
		return new CriteriaBuilder() {
			public Criteria build(Session session, boolean order) {
				return createBaseMasukCriteria(session, order).add(pendingMasukCriterion());
			}
		};
	}

	private DetailDataProvider createSlaOverdueKeluarProvider() {
		return createRuntimeFilteredProvider(createKeluarProvider(createAllKeluarProvider()), new RuntimeRowFilter() {
			public boolean accept(Object row) {
				return isPending(row) && calculateSlaHours(row) > SLA_OVERDUE_HOURS;
			}
		});
	}

	private DetailDataProvider createSlaOverdueMasukProvider() {
		return createRuntimeFilteredProvider(createMasukProvider(createAllMasukProvider()), new RuntimeRowFilter() {
			public boolean accept(Object row) {
				return isPending(row) && calculateSlaHours(row) > SLA_OVERDUE_HOURS;
			}
		});
	}

	private DetailDataProvider createKeluarProvider(final CriteriaBuilder builder) {
		return criteriaDetailProvider(builder);
	}

	private DetailDataProvider createMasukProvider(final CriteriaBuilder builder) {
		return criteriaDetailProvider(builder);
	}

	private DetailDataProvider createAllProvider() {
		return combinedProvider(new DetailDataProvider[] { includeKeluar() ? createKeluarProvider(createAllKeluarProvider()) : null,
				includeMasuk() ? createMasukProvider(createAllMasukProvider()) : null });
	}

	private DetailDataProvider createPendingProvider() {
		return combinedProvider(new DetailDataProvider[] {
				includeKeluar() ? createKeluarProvider(createPendingKeluarProvider()) : null,
				includeMasuk() ? createMasukProvider(createPendingMasukProvider()) : null });
	}

	private DetailDataProvider createApprovedProvider() {
		return combinedProvider(new DetailDataProvider[] {
				includeKeluar() ? createKeluarProvider(new CriteriaBuilder() {
					public Criteria build(Session session, boolean order) {
						return createBaseKeluarCriteria(session, order).add(Restrictions.eq("disetujui", true));
					}
				}) : null,
				includeMasuk() ? createMasukProvider(new CriteriaBuilder() {
					public Criteria build(Session session, boolean order) {
						return createBaseMasukCriteria(session, order).add(Restrictions.eq("disetujui", true));
					}
				}) : null });
	}

	private DetailDataProvider createRejectedProvider() {
		return combinedProvider(new DetailDataProvider[] {
				includeKeluar() ? createKeluarProvider(new CriteriaBuilder() {
					public Criteria build(Session session, boolean order) {
						return createBaseKeluarCriteria(session, order).add(Restrictions.eq("ditolak", true));
					}
				}) : null,
				includeMasuk() ? createMasukProvider(new CriteriaBuilder() {
					public Criteria build(Session session, boolean order) {
						return createBaseMasukCriteria(session, order).add(Restrictions.eq("ditolak", true));
					}
				}) : null });
	}

	private DetailDataProvider createRevisedProvider() {
		return combinedProvider(new DetailDataProvider[] {
				includeKeluar() ? createKeluarProvider(new CriteriaBuilder() {
					public Criteria build(Session session, boolean order) {
						return createBaseKeluarCriteria(session, order).add(Restrictions.eq("telahDirevisi", true));
					}
				}) : null,
				includeMasuk() ? createMasukProvider(new CriteriaBuilder() {
					public Criteria build(Session session, boolean order) {
						return createBaseMasukCriteria(session, order).add(Restrictions.eq("telahDirevisi", true));
					}
				}) : null });
	}

	private DetailDataProvider createFinishedProvider() {
		return createApprovedProvider();
	}

	private DetailDataProvider createSlaOverdueProvider() {
		return combinedProvider(new DetailDataProvider[] { includeKeluar() ? createSlaOverdueKeluarProvider() : null,
				includeMasuk() ? createSlaOverdueMasukProvider() : null });
	}

	private DetailDataProvider createSlaAge0_24Provider() {
		return createSlaBucketProvider(0, SLA_WARNING_HOURS);
	}

	private DetailDataProvider createSlaAge24_48Provider() {
		return createSlaBucketProvider(SLA_WARNING_HOURS, SLA_OVERDUE_HOURS);
	}

	private DetailDataProvider createSlaAge48_72Provider() {
		return createSlaBucketProvider(SLA_OVERDUE_HOURS, 72);
	}

	private DetailDataProvider createSlaAge72PlusProvider() {
		return createSlaBucketProvider(72, -1);
	}

	private DetailDataProvider createSlaWarningProvider() {
		return createSlaBucketProvider(SLA_WARNING_HOURS, SLA_OVERDUE_HOURS);
	}

	private DetailDataProvider createSlaSafeProvider() {
		return createSlaBucketProvider(0, SLA_WARNING_HOURS);
	}

	private DetailDataProvider createSlaBucketProvider(final int minExclusive, final int maxInclusive) {
		return createRuntimeFilteredProvider(createPendingProvider(), new RuntimeRowFilter() {
			public boolean accept(Object row) {
				long hours = calculateSlaHours(row);
				if (maxInclusive < 0) {
					return hours > minExclusive;
				}
				if (minExclusive <= 0) {
					return hours >= 0 && hours <= maxInclusive;
				}
				return hours > minExclusive && hours <= maxInclusive;
			}
		});
	}

	private DetailDataProvider createTanpaKodeProvider() {
		return combinedProvider(new DetailDataProvider[] {
				includeKeluar() ? createKeluarProvider(new CriteriaBuilder() {
					public Criteria build(Session session, boolean order) {
						return createBaseKeluarCriteria(session, order)
								.add(Restrictions.or(Restrictions.isNull("suratKeluar.kode"), Restrictions.eq("suratKeluar.kode", "")));
					}
				}) : null,
				includeMasuk() ? createMasukProvider(new CriteriaBuilder() {
					public Criteria build(Session session, boolean order) {
						return createBaseMasukCriteria(session, order)
								.add(Restrictions.or(Restrictions.isNull("suratMasuk.kode"), Restrictions.eq("suratMasuk.kode", "")));
					}
				}) : null });
	}

	private DetailDataProvider createTanpaPerihalProvider() {
		return combinedProvider(new DetailDataProvider[] {
				includeKeluar() ? createKeluarProvider(new CriteriaBuilder() {
					public Criteria build(Session session, boolean order) {
						return createBaseKeluarCriteria(session, order).add(Restrictions.or(Restrictions.isNull("suratKeluar.perihal"),
								Restrictions.eq("suratKeluar.perihal", "")));
					}
				}) : null,
				includeMasuk() ? createMasukProvider(new CriteriaBuilder() {
					public Criteria build(Session session, boolean order) {
						return createBaseMasukCriteria(session, order).add(Restrictions.or(Restrictions.isNull("suratMasuk.perihal"),
								Restrictions.eq("suratMasuk.perihal", "")));
					}
				}) : null });
	}

	private DetailDataProvider createTanpaAlurProvider() {
		return combinedProvider(new DetailDataProvider[] {
				includeKeluar() ? createKeluarProvider(new CriteriaBuilder() {
					public Criteria build(Session session, boolean order) {
						return createBaseKeluarCriteria(session, order).add(Restrictions.isNull("alurPersetujuanSuratKeluar"));
					}
				}) : null,
				includeMasuk() ? createMasukProvider(new CriteriaBuilder() {
					public Criteria build(Session session, boolean order) {
						return createBaseMasukCriteria(session, order).add(Restrictions.isNull("alurPersetujuanSuratMasuk"));
					}
				}) : null });
	}

	private DetailDataProvider createTanpaPejabatProvider() {
		return combinedProvider(new DetailDataProvider[] {
				includeKeluar() ? createKeluarProvider(new CriteriaBuilder() {
					public Criteria build(Session session, boolean order) {
						return createBaseKeluarCriteria(session, order)
								.add(Restrictions.and(Restrictions.isNull("pejabat"), Restrictions.isNull("jenisJabatan")));
					}
				}) : null,
				includeMasuk() ? createMasukProvider(new CriteriaBuilder() {
					public Criteria build(Session session, boolean order) {
						return createBaseMasukCriteria(session, order)
								.add(Restrictions.and(Restrictions.isNull("pejabat"), Restrictions.isNull("jenisJabatan")));
					}
				}) : null });
	}

	private Criterion pendingKeluarCriterion() {
		/*
		 * V5 FIX:
		 * Overdue / outstanding hanya untuk alur yang belum ditindaklanjuti.
		 * Sesuai model AlurPersetujuanSuratKeluarStatus dan AlurPersetujuanSuratMasukStatus:
		 * belum tindak lanjut = disetujui == false/null DAN ditolak == false/null.
		 * Field selesai tidak dipakai sebagai syarat overdue.
		 */
		return belumDitindaklanjutiCriterion();
	}

	private Criterion pendingMasukCriterion() {
		return belumDitindaklanjutiCriterion();
	}

	private Criterion belumDitindaklanjutiCriterion() {
		/*
		 * V6: belum tindak lanjut = belum disetujui, belum ditolak, dan belum ada pelaku alur
		 * pada status saat ini (konseptor/mahasiswa/siswa semuanya null).
		 */
		Criterion belumApprove = Restrictions.or(Restrictions.isNull("disetujui"), Restrictions.eq("disetujui", false));
		Criterion belumTolak = Restrictions.or(Restrictions.isNull("ditolak"), Restrictions.eq("ditolak", false));
		Criterion belumAdaPelaku = Restrictions.and(Restrictions.isNull("konseptor"),
				Restrictions.and(Restrictions.isNull("mahasiswa"), Restrictions.isNull("siswa")));
		return Restrictions.and(Restrictions.and(belumApprove, belumTolak), belumAdaPelaku);
	}

	private DetailDataProvider criteriaDetailProvider(final CriteriaBuilder builder) {
		return new DetailDataProvider() {
			public long count() throws Exception {
				Session session = HibernateUtil.currentSession();
				Criteria criteria = builder.build(session, false);
				criteria.setProjection(Projections.rowCount());
				Object result = criteria.uniqueResult();
				return result == null ? 0L : ((Number) result).longValue();
			}

			public List list(int first, int max) throws Exception {
				Session session = HibernateUtil.currentSession();
				return builder.build(session, true).setFirstResult(first).setMaxResults(max).list();
			}
		};
	}

	private DetailDataProvider combinedProvider(final DetailDataProvider[] providers) {
		return new DetailDataProvider() {
			public long count() throws Exception {
				long total = 0L;
				if (providers == null) {
					return total;
				}
				for (int i = 0; i < providers.length; i++) {
					if (providers[i] != null) {
						total += providers[i].count();
					}
				}
				return total;
			}

			public List list(int first, int max) throws Exception {
				List result = new ArrayList();
				if (providers == null || max <= 0) {
					return result;
				}
				long skip = first;
				int remaining = max;
				for (int i = 0; i < providers.length && remaining > 0; i++) {
					if (providers[i] == null) {
						continue;
					}
					long count = providers[i].count();
					if (skip >= count) {
						skip -= count;
						continue;
					}
					List sub = providers[i].list((int) skip, remaining);
					if (sub != null) {
						result.addAll(sub);
						remaining = max - result.size();
					}
					skip = 0L;
				}
				return result;
			}
		};
	}

	private DetailDataProvider createRuntimeFilteredProvider(final DetailDataProvider baseProvider,
			final RuntimeRowFilter filter) {
		return new DetailDataProvider() {
			private List collect(int first, int max, boolean countOnly) throws Exception {
				List result = new ArrayList();
				if (baseProvider == null || filter == null) {
					return result;
				}
				int offset = 0;
				int accepted = 0;
				while (offset < RUNTIME_FILTER_MAX_ROWS) {
					List rows = baseProvider.list(offset, RUNTIME_FILTER_SCAN_BATCH);
					if (rows == null || rows.isEmpty()) {
						break;
					}
					preloadParentSequenceCacheV7(rows);
					for (int i = 0; i < rows.size(); i++) {
						Object row = rows.get(i);
						if (filter.accept(row)) {
							if (!countOnly && accepted >= first && result.size() < max) {
								result.add(row);
							}
							accepted++;
						}
					}
					if (rows.size() < RUNTIME_FILTER_SCAN_BATCH) {
						break;
					}
					offset += RUNTIME_FILTER_SCAN_BATCH;
				}
				if (countOnly) {
					result.add(Integer.valueOf(accepted));
				}
				return result;
			}

			public long count() throws Exception {
				List count = collect(0, 0, true);
				return count == null || count.isEmpty() ? 0L : ((Integer) count.get(0)).longValue();
			}

			public List list(int first, int max) throws Exception {
				return collect(first, max, false);
			}
		};
	}

	private long countProvider(DetailDataProvider provider) throws Exception {
		return provider == null ? 0L : provider.count();
	}

	private List listProvider(DetailDataProvider provider, int first, int max) throws Exception {
		return provider == null ? new ArrayList() : provider.list(first, max);
	}

	private void createDetailNumber(Component parent, String value, final String title, final DetailDataProvider provider,
			String style) {
		A a = new A(value == null ? "0" : value);
		a.setStyle(style);
		a.setTooltiptext("Klik untuk melihat detail data");
		a.setParent(parent);
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail(title, provider);
			}
		});
	}

	private void viewDetail(final String title, final DetailDataProvider provider) {
		try {
			final Window window = new Window();
			window.setTitle(title);
			window.setWidth(Common.isMobile() ? "96%" : "90%");
			window.setHeight(Common.isMobile() ? "88%" : "78%");
			window.setClosable(true);
			window.setSizable(true);
			window.setMaximizable(true);
			window.setBorder("normal");
			window.setStyle("border-radius:16px; overflow:hidden;");
			if (DasboardAlurSurat.this.getPage() != null) {
				window.setPage(DasboardAlurSurat.this.getPage());
			} else if (DasboardAlurSurat.this.getParent() != null) {
				window.setParent(DasboardAlurSurat.this.getParent());
			}

			Div wrapper = new Div();
			wrapper.setParent(window);
			wrapper.setWidth("100%");
			wrapper.setHeight("100%");
			wrapper.setStyle("box-sizing:border-box; padding:12px; background:#f8fafc; overflow:auto;");

			appendHtml(wrapper, "<div style='padding:12px 14px; margin-bottom:10px; border-radius:14px; "
					+ "background:#ffffff; border:1px solid #e5e7eb; color:#475569; font-size:12px; line-height:1.55;'>"
					+ "Data ditampilkan per parent surat dalam grid paging " + DETAIL_PAGE_SIZE
					+ " baris per halaman. Satu baris mewakili satu Surat Masuk/Surat Keluar dan menampilkan timeline alur persetujuan yang sedang berjalan.</div>");

			final DetailDataProvider parentGroupedProvider = createParentGroupedProviderV12(provider);

			final Paging paging = new Paging();
			paging.setPageSize(DETAIL_PAGE_SIZE);
			paging.setDetailed(true);
			paging.setParent(wrapper);

			final Grid grid = new Grid();
			grid.setParent(wrapper);
			grid.setSclass("dgrid fgrid");
			grid.setStyle(compactGridStyleV8());
			grid.setMold("paging");
			grid.setPageSize(DETAIL_PAGE_SIZE);

			Columns columns = new Columns();
			columns.setParent(grid);
			addColumn(columns, "Jenis", "95px");
			addColumn(columns, "Tanggal", "105px");
			addColumn(columns, "Kode / No Surat", "145px");
			addColumn(columns, "Perihal", "260px");
			addColumn(columns, "Rincian Alur Menunggu Persetujuan", null);
			addColumn(columns, "Ringkasan", "190px");

			final Rows rows = new Rows();
			rows.setParent(grid);

			final EventListener refresh = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(rows);
					long total = parentGroupedProvider == null ? 0L : parentGroupedProvider.count();
					paging.setTotalSize(total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total);

					List dataRows = parentGroupedProvider == null ? new ArrayList()
							: parentGroupedProvider.list(paging.getActivePage() * DETAIL_PAGE_SIZE, DETAIL_PAGE_SIZE);
					if (dataRows == null || dataRows.isEmpty()) {
						Row row = new Row();
						row.setParent(rows);
						Label empty = new Label(ais.common.Common.getBahasaConfig("Tidak ada data untuk indikator ini."));
						empty.setStyle("padding:14px; color:#64748b; font-size:12px;");
						empty.setParent(row);
						return;
					}
					for (int i = 0; i < dataRows.size(); i++) {
						Row row = new Row();
						row.setParent(rows);
						renderDetailRow(row, dataRows.get(i));
					}
				}
			};
			paging.addEventListener("onPaging", refresh);
			refresh.onEvent(null);
			window.doModal();
		} catch (Exception e) {
			debugError("viewDetail-" + title, e);
		}
	}


	private String compactGridStyleV8() {
		return "margin-top:8px; border:1px solid #e5e7eb; border-radius:14px; overflow:hidden;"
				+ "background:#ffffff; font-size:11px;";
	}

	private String compactRowStyleV8() {
		return "min-height:28px; height:auto; vertical-align:top;";
	}

	private void addCompactColumnV8(Columns columns, String label, String width) {
		Column column = new Column(label);
		if (width != null) {
			column.setWidth(width);
		}
		column.setStyle("font-size:11px; font-weight:800; color:#334155; padding:6px 7px;"
				+ "background:#f8fafc; line-height:1.2;");
		column.setParent(columns);
	}

	private void addCompactCellV8(Row row, String value, boolean strong) {
		addCompactCellV8(row, value, strong, "#334155", "left");
	}

	private void addCompactCellV8(Row row, String value, boolean strong, String color, String align) {
		Label label = new Label(value == null || value.trim().isEmpty() ? "-" : value);
		label.setTooltiptext(value == null ? "" : value);
		label.setStyle("font-size:11px; color:" + color + "; line-height:1.25; padding:1px 0;"
				+ "white-space:normal; max-height:32px; overflow:hidden; display:block;"
				+ "text-align:" + (align == null ? "left" : align) + ";"
				+ (strong ? "font-weight:800;" : "font-weight:500;"));
		label.setParent(row);
	}


	private void addColumn(Columns columns, String label, String width) {
		Column column = new Column(label);
		if (width != null) {
			column.setWidth(width);
		}
		column.setStyle("font-size:12px; font-weight:800; color:#334155;");
		column.setParent(columns);
	}


	private DetailDataProvider createParentGroupedProviderV12(final DetailDataProvider baseProvider) {
		return new DetailDataProvider() {
			private List collectGroups(int first, int max, boolean countOnly) throws Exception {
				List result = new ArrayList();
				if (baseProvider == null) {
					return result;
				}

				Map grouped = new LinkedHashMap();
				List orderedGroups = new ArrayList();
				int offset = 0;

				while (offset < RUNTIME_FILTER_MAX_ROWS) {
					List rows = baseProvider.list(offset, RUNTIME_FILTER_SCAN_BATCH);
					if (rows == null || rows.isEmpty()) {
						break;
					}
					preloadParentSequenceCacheV7(rows);

					for (int i = 0; i < rows.size(); i++) {
						Object row = rows.get(i);
						String key = getParentGroupKeyV12(row);
						if (!grouped.containsKey(key)) {
							AlurParentGroupRow groupRow = buildParentGroupRowV12(row);
							grouped.put(key, groupRow);
							orderedGroups.add(groupRow);
						}
					}

					if (rows.size() < RUNTIME_FILTER_SCAN_BATCH) {
						break;
					}
					offset += RUNTIME_FILTER_SCAN_BATCH;
				}

				if (countOnly) {
					result.add(Integer.valueOf(orderedGroups.size()));
					return result;
				}

				if (first < 0) {
					first = 0;
				}
				int end = first + max;
				if (end > orderedGroups.size()) {
					end = orderedGroups.size();
				}
				for (int i = first; i < end; i++) {
					result.add(orderedGroups.get(i));
				}
				return result;
			}

			public long count() throws Exception {
				List count = collectGroups(0, 0, true);
				return count == null || count.isEmpty() ? 0L : ((Integer) count.get(0)).longValue();
			}

			public List list(int first, int max) throws Exception {
				return collectGroups(first, max, false);
			}
		};
	}

	private String getParentGroupKeyV12(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				SuratKeluar surat = ((AlurPersetujuanSuratKeluarStatus) status).getSuratKeluar();
				return "KELUAR-" + (surat != null && surat.getId() != null ? String.valueOf(surat.getId())
						: String.valueOf(System.identityHashCode(status)));
			}
			if (status instanceof AlurPersetujuanSuratMasukStatus) {
				SuratMasuk surat = ((AlurPersetujuanSuratMasukStatus) status).getSuratMasuk();
				return "MASUK-" + (surat != null && surat.getId() != null ? String.valueOf(surat.getId())
						: String.valueOf(System.identityHashCode(status)));
			}
		} catch (Exception e) {
			debugError("getParentGroupKeyV12", e);
		}
		return "UNKNOWN-" + String.valueOf(System.identityHashCode(status));
	}

	private AlurParentGroupRow buildParentGroupRowV12(Object status) {
		AlurParentGroupRow group = new AlurParentGroupRow();
		try {
			group.jenis = isMasuk(status) ? "Surat Masuk" : "Surat Keluar";
			group.tanggal = formatDate(getTanggal(status));
			group.kode = getKode(status);
			group.perihal = getPerihal(status);

			List alurs = new ArrayList();
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				SuratKeluar suratKeluar = ((AlurPersetujuanSuratKeluarStatus) status).getSuratKeluar();
				alurs = loadAlurKeluarByParent(suratKeluar);
				group.parameterHtml = buildKlasifikasiSuratKeluarParameterCompactHtmlV13(suratKeluar);
			} else if (status instanceof AlurPersetujuanSuratMasukStatus) {
				alurs = loadAlurMasukByParent(((AlurPersetujuanSuratMasukStatus) status).getSuratMasuk());
				group.parameterHtml = "";
			}

			group.timelineHtml = buildAlurDilewatiTimelineHtmlV12(alurs);
			group.ringkasanHtml = buildParentRingkasanHtmlV12(alurs);
		} catch (Exception e) {
			debugError("buildParentGroupRowV12", e);
			group.jenis = isMasuk(status) ? "Surat Masuk" : "Surat Keluar";
			group.tanggal = formatDate(getTanggal(status));
			group.kode = getKode(status);
			group.perihal = getPerihal(status);
			group.parameterHtml = "";
			group.timelineHtml = "<div style='font-size:11px; color:#991b1b;'>Gagal memuat timeline alur.</div>";
			group.ringkasanHtml = "<div style='font-size:11px; color:#991b1b;'>Error</div>";
		}
		return group;
	}

	private String buildAlurDilewatiTimelineHtmlV12(List alurs) {
		if (alurs == null || alurs.isEmpty()) {
			return "<div style='padding:6px 9px; border-radius:999px; background:#f8fafc; border:1px dashed #cbd5e1; color:#64748b; font-size:10px; font-weight:800; display:inline-block;'>Belum ada data alur</div>";
		}

		/*
		 * V14:
		 * Timeline dibuat lebih sederhana agar baris tidak terlalu tinggi.
		 * Bentuknya node-node horizontal dengan panah antar node.
		 */
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:flex; align-items:center; gap:5px; flex-wrap:wrap; line-height:1.1; padding:1px 0;'>");
		int no = 1;
		int tampil = 0;
		for (int i = 0; i < alurs.size(); i++) {
			Object alur = alurs.get(i);
			if (!isAlurSudahDilewatiV12(alur)) {
				continue;
			}
			if (tampil > 0) {
				sb.append("<span style='color:#94a3b8; font-size:13px; font-weight:900;'>&rarr;</span>");
			}
			tampil++;
			String color = getStatusColor(alur);
			String bg = getSlaStatusBg(alur);
			String status = getStatusText(alur);
			String title = safeHtml(getAlurName(alur) + " | " + getPengirimAlurName(alur) + " -> "
					+ getPenerimaAlurName(alur) + " | " + status + " | " + calculateSlaHours(alur) + " jam");
			sb.append("<span title='").append(title)
					.append("' style='display:inline-flex; align-items:center; gap:5px; max-width:250px; padding:4px 8px; border-radius:999px; background:")
					.append(bg).append("; border:1px solid #e2e8f0; color:#0f172a; font-size:10px; font-weight:900; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;'>")
					.append("<span style='min-width:17px; height:17px; border-radius:999px; display:inline-flex; align-items:center; justify-content:center; background:")
					.append(color).append("; color:#ffffff; font-size:9px; font-weight:900;'>").append(no)
					.append("</span>")
					.append("<span style='overflow:hidden; text-overflow:ellipsis; white-space:nowrap;'>")
					.append(safeHtml(getAlurName(alur))).append("</span>")
					.append("<span style='font-size:9px; color:").append(getSlaStatusColor(alur))
					.append("; background:#ffffff; border:1px solid rgba(15,23,42,.06); border-radius:999px; padding:1px 5px;'>")
					.append(safeHtml(status)).append("</span>")
					.append("</span>");
			no++;
		}
		if (tampil <= 0) {
			sb.append("<span style='padding:6px 9px; border-radius:999px; background:#fff7ed; border:1px solid #fed7aa; color:#9a3412; font-size:10px; font-weight:900;'>Belum ada alur persetujuan yang sedang berjalan</span>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private boolean isAlurSudahDilewatiV12(Object alur) {
		try {
			if (hasPelakuAlur(alur)) {
				return true;
			}
			if (alur instanceof AlurPersetujuanSuratKeluarStatus) {
				AlurPersetujuanSuratKeluarStatus s = (AlurPersetujuanSuratKeluarStatus) alur;
				return Boolean.TRUE.equals(s.getDisetujui()) || Boolean.TRUE.equals(s.getDitolak())
						|| Boolean.TRUE.equals(s.getTelahDirevisi()) || Boolean.TRUE.equals(s.getSelesai());
			}
			if (alur instanceof AlurPersetujuanSuratMasukStatus) {
				AlurPersetujuanSuratMasukStatus s = (AlurPersetujuanSuratMasukStatus) alur;
				return Boolean.TRUE.equals(s.getDisetujui()) || Boolean.TRUE.equals(s.getDitolak())
						|| Boolean.TRUE.equals(s.getTelahDirevisi());
			}
		} catch (Exception e) {
			debugError("isAlurSudahDilewatiV12", e);
		}
		return false;
	}

	private String buildParentRingkasanHtmlV12(List alurs) {
		long total = alurs == null ? 0L : alurs.size();
		long dilewati = 0L;
		long menunggu = 0L;
		long overdue = 0L;
		if (alurs != null) {
			for (int i = 0; i < alurs.size(); i++) {
				Object alur = alurs.get(i);
				if (isAlurSudahDilewatiV12(alur)) {
					dilewati++;
				}
				if (isPending(alur)) {
					menunggu++;
					if (calculateSlaHours(alur) > SLA_OVERDUE_HOURS) {
						overdue++;
					}
				}
			}
		}

		return "<div style='display:flex; flex-direction:column; gap:5px;'>"
				+ ringkasanBadgeHtmlV12("Total Alur", total, "#e0f2fe", "#075985")
				+ ringkasanBadgeHtmlV12(LABEL_RINGKASAN_MENUNGGU_PERSETUJUAN, dilewati, "#dcfce7", "#166534")
				+ ringkasanBadgeHtmlV12("Menunggu", menunggu, "#fef3c7", "#92400e")
				+ ringkasanBadgeHtmlV12("Overdue", overdue, "#fee2e2", "#991b1b") + "</div>";
	}

	private String ringkasanBadgeHtmlV12(String label, long value, String bg, String color) {
		return "<div style='display:flex; justify-content:space-between; gap:8px; align-items:center; border-radius:999px; padding:4px 8px; background:"
				+ bg + "; color:" + color + "; font-size:10px; font-weight:900;'>"
				+ "<span>" + safeHtml(label) + "</span><span>" + value + "</span></div>";
	}


	private String buildPerihalDanParameterHtmlV13(String perihal, String parameterHtml) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:11px; color:#334155; line-height:1.35; font-weight:700;'>")
				.append(safeHtml(perihal == null || perihal.trim().isEmpty() ? "-" : perihal)).append("</div>");
		if (parameterHtml != null && !parameterHtml.trim().isEmpty()) {
			sb.append(parameterHtml);
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private String buildKlasifikasiSuratKeluarParameterCompactHtmlV13(SuratKeluar suratKeluar) {
		try {
			if (suratKeluar == null || suratKeluar.getId() == null) {
				return "";
			}
			List<KlasifikasiSuratKeluarParemeterValue> values = HibernateUtil.currentSession()
					.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
					.createAlias("klasifikasiSuratKeluarParemeter", "klasifikasiSuratKeluarParemeter")
					.addOrder(Order.asc("klasifikasiSuratKeluarParemeter.nama"))
					.add(Restrictions.eq("suratKeluar", suratKeluar)).list();
			if (values == null || values.isEmpty()) {
				return "";
			}

			StringBuilder sb = new StringBuilder();
			sb.append("<div style='display:flex; flex-wrap:wrap; gap:4px; margin-top:6px;'>");
			int maxTampil = 3;
			int tampil = 0;
			for (int i = 0; i < values.size(); i++) {
				KlasifikasiSuratKeluarParemeterValue value = values.get(i);
				if (value == null || value.getKlasifikasiSuratKeluarParemeter() == null) {
					continue;
				}
				if (tampil >= maxTampil) {
					break;
				}
				String namaParam = value.getKlasifikasiSuratKeluarParemeter().getNama();
				String namaValue = value.getNama();
				sb.append("<span title='").append(safeHtml(namaParam)).append(" : ").append(safeHtml(namaValue))
						.append("' style='display:inline-flex; align-items:center; gap:3px; max-width:190px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; padding:3px 8px; border-radius:999px; background:#f8fafc; border:1px solid #e2e8f0; color:#334155; font-size:9px; font-weight:800;'>")
						.append("<span style='color:#64748b;'>").append(safeHtml(namaParam)).append("</span>")
						.append("<span style='color:#0f172a;'>: ").append(safeHtml(namaValue)).append("</span></span>");
				tampil++;
			}
			int sisa = values.size() - tampil;
			if (sisa > 0) {
				sb.append("<span style='padding:3px 8px; border-radius:999px; background:#eff6ff; border:1px solid #bfdbfe; color:#1d4ed8; font-size:9px; font-weight:900;'>+")
						.append(sisa).append(" lainnya</span>");
			}
			sb.append("</div>");
			return sb.toString();
		} catch (Exception e) {
			debugError("buildKlasifikasiSuratKeluarParameterCompactHtmlV13", e);
			return "";
		}
	}


	private void renderParentGroupDetailRowV12(Row row, AlurParentGroupRow group) {
		row.setStyle("vertical-align:top;");
		addCell(row, group.jenis);
		addCell(row, group.tanggal);
		addCell(row, group.kode);
		addHtmlCellV12(row, buildPerihalDanParameterHtmlV13(group.perihal, group.parameterHtml));
		addHtmlCellV12(row, group.timelineHtml);
		addHtmlCellV12(row, group.ringkasanHtml);
	}

	private void renderParentGroupCompactRowV12(Row row, AlurParentGroupRow group) {
		row.setStyle(compactRowStyleV8() + " vertical-align:top;");
		addCompactCellV8(row, group.jenis == null || group.jenis.indexOf("Masuk") < 0 ? "Keluar" : "Masuk", true);
		addCompactCellV8(row, group.tanggal, false);
		addCompactCellV8(row, group.kode, true);
		addHtmlCellV12(row, buildPerihalDanParameterHtmlV13(group.perihal, group.parameterHtml));
		addHtmlCellV12(row, group.timelineHtml);
		addHtmlCellV12(row, group.ringkasanHtml);
	}

	private void addHtmlCellV12(Row row, String html) {
		org.zkoss.zul.Html h = new org.zkoss.zul.Html(html == null ? "" : html);
		h.setStyle("display:block; white-space:normal; vertical-align:top; line-height:1.2;");
		h.setParent(row);
	}


	private void renderDetailRow(Row row, Object status) {
		if (status instanceof AlurParentGroupRow) {
			renderParentGroupDetailRowV12(row, (AlurParentGroupRow) status);
			return;
		}
		renderParentGroupDetailRowV12(row, buildParentGroupRowV12(status));
	}

	private void addCell(Row row, String value) {
		Label label = new Label(value == null || value.trim().isEmpty() ? "-" : value);
		label.setStyle("font-size:12px; color:#334155; white-space:normal;");
		label.setParent(row);
	}

	private Panelchildren createModernPanel(String title, Component parent) {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setTitle(title);
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:16px; overflow:hidden;"
				+ "background:#ffffff; box-shadow:0 12px 24px rgba(15,23,42,.07);");
		panel.setParent(parent);

		Panelchildren pch = new Panelchildren();
		pch.setStyle("padding:14px; background:#ffffff;");
		pch.setParent(panel);

		String description = getPanelDescriptionForEndUserV17(title);
		if (description != null && description.trim().length() > 0) {
			appendHtml(pch, dashboardExplainHtmlV17(description));
		}
		return pch;
	}

	private String dashboardExplainHtmlV17(String description) {
		return "<div style='font-size:12px; color:#64748b; line-height:1.55; margin:-2px 0 12px 0; "
				+ "padding:10px 12px; border-radius:12px; background:#f8fafc; border:1px dashed #cbd5e1;'>"
				+ safeHtml(description) + "</div>";
	}

	private String getPanelDescriptionForEndUserV17(String title) {
		if (title == null) {
			return "membantu menampilkan kondisi alur surat secara ringkas agar pengguna lebih mudah memahami pekerjaan yang perlu dipantau.";
		}
		String t = title.toLowerCase(java.util.Locale.ENGLISH);
		if (t.indexOf("perbandingan") >= 0 || t.indexOf("jenis surat") >= 0) {
			return "membandingkan jumlah proses surat masuk dan surat keluar. Informasi ini membantu pengguna melihat sumber pekerjaan yang paling banyak pada periode yang dipilih.";
		}
		if (t.indexOf("") >= 0) {
			return "menunjukkan perjalanan surat dari tahap awal sampai selesai. Pengguna dapat melihat di bagian mana proses paling banyak menunggu.";
		}
		if (t.indexOf("sla") >= 0 || t.indexOf("keterlambatan") >= 0 || t.indexOf("aging") >= 0) {
			return "membantu melihat surat yang berisiko terlambat atau sudah melewati batas waktu. Gunakan informasi ini untuk menentukan prioritas tindak lanjut.";
		}
		if (t.indexOf("kontrol") >= 0 || t.indexOf("detail") >= 0) {
			return "menampilkan daftar rinci proses alur surat sesuai filter dashboard. Data rinci membantu pengguna menelusuri surat yang perlu diperiksa lebih lanjut.";
		}
		if (t.indexOf("report") >= 0 || t.indexOf("laporan") >= 0) {
			return "menyajikan laporan proses persetujuan surat dalam bentuk ringkas. Pengguna dapat melihat status dan urutan proses tanpa membuka data satu per satu.";
		}
		if (t.indexOf("performa") >= 0 || t.indexOf("pejabat") >= 0 || t.indexOf("jabatan") >= 0) {
			return "memperlihatkan beban dan kinerja tindak lanjut berdasarkan pejabat atau jabatan. Informasi ini membantu pemerataan pekerjaan dan pemantauan tanggung jawab.";
		}
		if (t.indexOf("trend") >= 0 || t.indexOf("tren") >= 0 || t.indexOf("distribusi") >= 0) {
			return "menampilkan pola perubahan data dari waktu ke waktu. Dengan melihat tren, pengguna dapat mengetahui apakah pekerjaan sedang meningkat, menurun, atau perlu perhatian.";
		}
		if (t.indexOf("sebaran") >= 0 || t.indexOf("bottleneck") >= 0) {
			return "menunjukkan titik penumpukan proses pada alur tertentu. Gunakan untuk mengetahui tahap yang paling sering menyebabkan surat tertahan.";
		}
		if (t.indexOf("aktivitas") >= 0) {
			return "menampilkan aktivitas terbaru pada alur surat. Pengguna dapat melihat perkembangan terakhir tanpa harus membuka seluruh arsip surat.";
		}
		if (t.indexOf("matrix") >= 0 || t.indexOf("matriks") >= 0 || t.indexOf("audit") >= 0 || t.indexOf("eksekusi") >= 0) {
			return "merangkum kondisi pengendalian alur surat dan kesiapan tindak lanjut. Informasi ini membantu pimpinan mengambil keputusan dengan lebih cepat.";
		}
		return "membantu pengguna memahami kondisi alur surat secara ringkas, sehingga pekerjaan yang perlu perhatian dapat segera diketahui.";
	}


	private void renderCounterListClickable(Component parent, Map<String, Integer> source, String emptyMessage,
			final String detailType, final String detailTitlePrefix) {
		if (source == null || source.isEmpty()) {
			appendEmptyState(parent, emptyMessage);
			return;
		}

		/*
		 * V10 FIX:
		 * Sebelumnya angka counter berasal dari Map hasil analyzeRows/sample,
		 * sedangkan popup memakai DetailDataProvider yang melakukan scan/query sendiri.
		 * Akibatnya angka di dashboard bisa berbeda dengan total data popup.
		 *
		 * Sekarang angka yang tampil dihitung dari provider yang sama dengan popup,
		 * sehingga nilai counter == paging total di popup.
		 */
		List rawList = sortCounter(source);
		List list = new ArrayList();
		for (int i = 0; i < rawList.size(); i++) {
			CounterItem raw = (CounterItem) rawList.get(i);
			if (raw == null) {
				continue;
			}
			DetailDataProvider provider = createCounterDetailProviderV9(detailType, raw.label);
			long providerCount = 0L;
			try {
				providerCount = provider == null ? 0L : provider.count();
			} catch (Exception e) {
				debugError("renderCounterListClickable-count-" + detailType + "-" + raw.label, e);
				providerCount = raw.count;
			}

			if (providerCount <= 0L) {
				continue;
			}

			CounterItem item = new CounterItem();
			item.label = raw.label;
			item.count = providerCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) providerCount;
			list.add(item);
		}

		if (list.isEmpty()) {
			appendEmptyState(parent, emptyMessage);
			return;
		}

		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				CounterItem a = (CounterItem) o1;
				CounterItem b = (CounterItem) o2;
				if (b.count != a.count) {
					return b.count - a.count;
				}
				String la = a.label == null ? "" : a.label;
				String lb = b.label == null ? "" : b.label;
				return la.compareToIgnoreCase(lb);
			}
		});

		int max = ((CounterItem) list.get(0)).count;

		for (int i = 0; i < list.size() && i < 8; i++) {
			CounterItem item = (CounterItem) list.get(i);
			final String label = item == null ? "" : item.label;
			final int count = item == null ? 0 : item.count;
			final DetailDataProvider detailProvider = createCounterDetailProviderV9(detailType, label);

			int pct = max <= 0 ? 0 : (int) Math.round((count * 100.0d) / max);
			if (pct < 4 && count > 0) {
				pct = 4;
			}

			Div wrapper = new Div();
			wrapper.setStyle("padding:9px 0; border-bottom:1px solid #f1f5f9;");
			wrapper.setParent(parent);

			Hbox top = new Hbox();
			top.setWidth("100%");
			top.setPack("justify");
			top.setAlign("center");
			top.setStyle("gap:10px;");
			top.setParent(wrapper);

			Label lbl = new Label((i + 1) + ". " + (label == null || label.trim().isEmpty() ? "Tidak diketahui" : label));
			lbl.setTooltiptext(label);
			lbl.setStyle("font-size:12px; font-weight:800; color:#334155; white-space:normal; line-height:1.25;");
			lbl.setParent(top);

			createDetailNumber(top, String.valueOf(count), detailTitlePrefix + ": " + label, detailProvider,
					"font-size:12px; font-weight:900; color:#1d4ed8; text-decoration:none; "
							+ "cursor:pointer; border-bottom:1px dashed #1d4ed8; min-width:38px; text-align:right;");

			appendHtml(wrapper,
					"<div style='margin-top:7px; height:8px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>"
							+ "<div style='height:8px; width:" + pct
							+ "%; background:#2563eb; border-radius:999px;'></div></div>");
		}
	}


	private DetailDataProvider createCounterDetailProviderV9(final String detailType, final String label) {
		return createRuntimeFilteredProvider(createAllProvider(), new RuntimeRowFilter() {
			public boolean accept(Object row) {
				if ("ALUR".equals(detailType)) {
					return safeCounterEqualsV9(getAlurName(row), label);
				}
				if ("PEJABAT".equals(detailType)) {
					return safeCounterEqualsV9(getPejabatName(row), label);
				}
				if ("SLA_RISK_PEJABAT".equals(detailType)) {
					return safeCounterEqualsV9(getPejabatName(row), label) && isPending(row)
							&& calculateSlaHours(row) > SLA_WARNING_HOURS;
				}
				if ("KLASIFIKASI".equals(detailType)) {
					return safeCounterEqualsV9(getKlasifikasiName(row), label);
				}
				if ("SATKER".equals(detailType)) {
					return safeCounterEqualsV9(getSatkerName(row), label);
				}
				return false;
			}
		});
	}

	private boolean safeCounterEqualsV9(String a, String b) {
		String x = a == null ? "" : a.trim();
		String y = b == null ? "" : b.trim();
		return x.equalsIgnoreCase(y);
	}


	private void renderCounterList(Component parent, Map<String, Integer> source, String emptyMessage) {
		if (source == null || source.isEmpty()) {
			appendEmptyState(parent, emptyMessage);
			return;
		}
		List list = sortCounter(source);
		int max = list.isEmpty() ? 0 : ((CounterItem) list.get(0)).count;
		for (int i = 0; i < list.size() && i < 8; i++) {
			CounterItem item = (CounterItem) list.get(i);
			int pct = max <= 0 ? 0 : (int) Math.round((item.count * 100.0d) / max);
			if (pct < 4 && item.count > 0) {
				pct = 4;
			}
			appendHtml(parent, "<div style='padding:9px 0; border-bottom:1px solid #f1f5f9;'>"
					+ "<div style='display:flex; justify-content:space-between; gap:10px;'>"
					+ "<div style='font-size:12px; font-weight:800; color:#334155;'>" + (i + 1) + ". "
					+ safeHtml(item.label) + "</div>"
					+ "<div style='font-size:12px; font-weight:900; color:#0f172a;'>" + item.count + "</div></div>"
					+ "<div style='margin-top:7px; height:8px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>"
					+ "<div style='height:8px; width:" + pct
					+ "%; background:#2563eb; border-radius:999px;'></div></div></div>");
		}
	}

	private List sortCounter(Map<String, Integer> source) {
		List list = new ArrayList();
		for (String key : source.keySet()) {
			CounterItem item = new CounterItem();
			item.label = key;
			item.count = source.get(key) == null ? 0 : source.get(key).intValue();
			list.add(item);
		}
		Collections.sort(list, new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				CounterItem a = (CounterItem) o1;
				CounterItem b = (CounterItem) o2;
				if (b.count != a.count) {
					return b.count - a.count;
				}
				return a.label.compareToIgnoreCase(b.label);
			}
		});
		return list;
	}

	private String gaugeHtml(String title, long pct, String desc, String color) {
		if (pct < 0L) {
			pct = 0L;
		}
		if (pct > 100L) {
			pct = 100L;
		}
		return "<div style='padding:8px 0; border-bottom:1px solid #e2e8f0;'>"
				+ "<div style='display:flex; justify-content:space-between; gap:10px; align-items:center;'>"
				+ "<div><div style='font-size:12px; font-weight:900; color:#0f172a;'>" + safeHtml(title)
				+ "</div><div style='font-size:11px; color:#64748b; margin-top:2px;'>" + safeHtml(desc)
				+ "</div></div>" + "<div style='font-size:13px; font-weight:900; color:#0f172a;'>" + pct
				+ "%</div></div>" + "<div style='margin-top:7px; height:9px; border-radius:999px; background:#e2e8f0; overflow:hidden;'>"
				+ "<div style='height:9px; width:" + pct + "%; border-radius:999px; background:" + color
				+ ";'></div></div></div>";
	}

	private String actionPlanCard(String no, String title, String desc, String bg, String color) {
		return "<div style='border-radius:16px; padding:14px; background:" + bg
				+ "; border:1px solid rgba(15,23,42,.08); min-height:105px;'>"
				+ "<div style='display:flex; justify-content:space-between; gap:12px; align-items:center;'>"
				+ "<div style='font-size:12px; font-weight:900; color:" + color + ";'>" + safeHtml(title)
				+ "</div>" + "<div style='width:28px; height:28px; border-radius:999px; background:#ffffff; color:"
				+ color + "; display:flex; align-items:center; justify-content:center; font-weight:900;'>"
				+ safeHtml(no) + "</div></div>" + "<div style='font-size:12px; color:" + color
				+ "; line-height:1.45; margin-top:10px;'>" + safeHtml(desc) + "</div></div>";
	}

	private void appendHtml(Component parent, String html) {
		org.zkoss.zul.Html h = new org.zkoss.zul.Html(html);
		h.setParent(parent);
	}

	private void appendEmptyState(Component parent, String message) {
		appendHtml(parent, "<div style='padding:18px; border-radius:14px; background:#f8fafc; border:1px dashed #cbd5e1;"
				+ "text-align:center; color:#64748b; font-size:12px;'>" + safeHtml(message) + "</div>");
	}

	private void addCounter(Map<String, Integer> map, String key) {
		if (key == null || key.trim().isEmpty()) {
			key = "Tidak diketahui";
		}
		Integer current = map.get(key);
		map.put(key, current == null ? 1 : current + 1);
	}

	private long percent(long value, long total) {
		if (total <= 0L || value <= 0L) {
			return 0L;
		}
		return Math.round((value * 100.0d) / total);
	}

	private long max(long[] values) {
		long max = 0L;
		if (values == null) {
			return max;
		}
		for (int i = 0; i < values.length; i++) {
			if (values[i] > max) {
				max = values[i];
			}
		}
		return max;
	}

	private Date addHours(Date date, int hours) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date == null ? new Date() : date);
		cal.add(Calendar.HOUR_OF_DAY, hours);
		return cal.getTime();
	}

	private Object getPreviousAlurStatus(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				AlurPersetujuanSuratKeluarStatus current = (AlurPersetujuanSuratKeluarStatus) status;
				List alurs = loadAlurKeluarByParent(current.getSuratKeluar());
				Object previous = null;
				for (int i = 0; i < alurs.size(); i++) {
					AlurPersetujuanSuratKeluarStatus row = (AlurPersetujuanSuratKeluarStatus) alurs.get(i);
					if (row == current || (row.getId() != null && current.getId() != null
							&& row.getId().equals(current.getId()))) {
						return previous;
					}
					previous = row;
				}
			} else if (status instanceof AlurPersetujuanSuratMasukStatus) {
				AlurPersetujuanSuratMasukStatus current = (AlurPersetujuanSuratMasukStatus) status;
				List alurs = loadAlurMasukByParent(current.getSuratMasuk());
				Object previous = null;
				for (int i = 0; i < alurs.size(); i++) {
					AlurPersetujuanSuratMasukStatus row = (AlurPersetujuanSuratMasukStatus) alurs.get(i);
					if (row == current || (row.getId() != null && current.getId() != null
							&& row.getId().equals(current.getId()))) {
						return previous;
					}
					previous = row;
				}
			}
		} catch (Exception e) {
			debugError("getPreviousAlurStatus", e);
		}
		return null;
	}

	private void clearParentSequenceCacheV7() {
		if (alurKeluarByParentIdCache == null) {
			alurKeluarByParentIdCache = new HashMap<Long, List>();
		} else {
			alurKeluarByParentIdCache.clear();
		}
		if (alurMasukByParentIdCache == null) {
			alurMasukByParentIdCache = new HashMap<Long, List>();
		} else {
			alurMasukByParentIdCache.clear();
		}
	}

	private void preloadParentSequenceCacheV7(List rows) {
		try {
			if (rows == null || rows.isEmpty()) {
				return;
			}

			Map<Long, SuratKeluar> keluarMap = new HashMap<Long, SuratKeluar>();
			Map<Long, SuratMasuk> masukMap = new HashMap<Long, SuratMasuk>();

			for (int i = 0; i < rows.size(); i++) {
				Object row = rows.get(i);
				if (row instanceof AlurPersetujuanSuratKeluarStatus) {
					SuratKeluar surat = ((AlurPersetujuanSuratKeluarStatus) row).getSuratKeluar();
					if (surat != null && surat.getId() != null && !alurKeluarByParentIdCache.containsKey(surat.getId())) {
						keluarMap.put(surat.getId(), surat);
					}
				} else if (row instanceof AlurPersetujuanSuratMasukStatus) {
					SuratMasuk surat = ((AlurPersetujuanSuratMasukStatus) row).getSuratMasuk();
					if (surat != null && surat.getId() != null && !alurMasukByParentIdCache.containsKey(surat.getId())) {
						masukMap.put(surat.getId(), surat);
					}
				}
			}

			if (!keluarMap.isEmpty()) {
				preloadAlurKeluarByParentBatchV7(new ArrayList(keluarMap.values()));
			}
			if (!masukMap.isEmpty()) {
				preloadAlurMasukByParentBatchV7(new ArrayList(masukMap.values()));
			}
		} catch (Exception e) {
			debugError("preloadParentSequenceCacheV7", e);
		}
	}

	@SuppressWarnings("unchecked")
	private void preloadAlurKeluarByParentBatchV7(List suratKeluarList) {
		if (suratKeluarList == null || suratKeluarList.isEmpty()) {
			return;
		}
		try {
			int batchSize = 100;
			for (int start = 0; start < suratKeluarList.size(); start += batchSize) {
				int end = Math.min(start + batchSize, suratKeluarList.size());
				List batch = suratKeluarList.subList(start, end);

				for (int i = 0; i < batch.size(); i++) {
					SuratKeluar surat = (SuratKeluar) batch.get(i);
					if (surat != null && surat.getId() != null && !alurKeluarByParentIdCache.containsKey(surat.getId())) {
						alurKeluarByParentIdCache.put(surat.getId(), new ArrayList());
					}
				}

				List alurs = HibernateUtil.currentSession().createCriteria(AlurPersetujuanSuratKeluarStatus.class)
						.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.in("suratKeluar", batch))
						.addOrder(Order.asc("id")).list();

				for (int i = 0; i < alurs.size(); i++) {
					AlurPersetujuanSuratKeluarStatus row = (AlurPersetujuanSuratKeluarStatus) alurs.get(i);
					SuratKeluar surat = row.getSuratKeluar();
					if (surat != null && surat.getId() != null) {
						List list = (List) alurKeluarByParentIdCache.get(surat.getId());
						if (list == null) {
							list = new ArrayList();
							alurKeluarByParentIdCache.put(surat.getId(), list);
						}
						list.add(row);
					}
				}
			}
		} catch (Exception e) {
			debugError("preloadAlurKeluarByParentBatchV7", e);
		}
	}

	@SuppressWarnings("unchecked")
	private void preloadAlurMasukByParentBatchV7(List suratMasukList) {
		if (suratMasukList == null || suratMasukList.isEmpty()) {
			return;
		}
		try {
			int batchSize = 100;
			for (int start = 0; start < suratMasukList.size(); start += batchSize) {
				int end = Math.min(start + batchSize, suratMasukList.size());
				List batch = suratMasukList.subList(start, end);

				for (int i = 0; i < batch.size(); i++) {
					SuratMasuk surat = (SuratMasuk) batch.get(i);
					if (surat != null && surat.getId() != null && !alurMasukByParentIdCache.containsKey(surat.getId())) {
						alurMasukByParentIdCache.put(surat.getId(), new ArrayList());
					}
				}

				List alurs = HibernateUtil.currentSession().createCriteria(AlurPersetujuanSuratMasukStatus.class)
						.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.in("suratMasuk", batch))
						.addOrder(Order.asc("id")).list();

				for (int i = 0; i < alurs.size(); i++) {
					AlurPersetujuanSuratMasukStatus row = (AlurPersetujuanSuratMasukStatus) alurs.get(i);
					SuratMasuk surat = row.getSuratMasuk();
					if (surat != null && surat.getId() != null) {
						List list = (List) alurMasukByParentIdCache.get(surat.getId());
						if (list == null) {
							list = new ArrayList();
							alurMasukByParentIdCache.put(surat.getId(), list);
						}
						list.add(row);
					}
				}
			}
		} catch (Exception e) {
			debugError("preloadAlurMasukByParentBatchV7", e);
		}
	}

	@SuppressWarnings("unchecked")
	private List loadAlurKeluarByParent(SuratKeluar suratKeluar) {
		if (suratKeluar == null || suratKeluar.getId() == null) {
			return new ArrayList();
		}
		List cached = (List) alurKeluarByParentIdCache.get(suratKeluar.getId());
		if (cached != null) {
			return cached;
		}
		List list = HibernateUtil.currentSession().createCriteria(AlurPersetujuanSuratKeluarStatus.class)
				.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("suratKeluar", suratKeluar))
				.addOrder(Order.asc("id")).list();
		alurKeluarByParentIdCache.put(suratKeluar.getId(), list == null ? new ArrayList() : list);
		return list == null ? new ArrayList() : list;
	}

	@SuppressWarnings("unchecked")
	private List loadAlurMasukByParent(SuratMasuk suratMasuk) {
		if (suratMasuk == null || suratMasuk.getId() == null) {
			return new ArrayList();
		}
		List cached = (List) alurMasukByParentIdCache.get(suratMasuk.getId());
		if (cached != null) {
			return cached;
		}
		List list = HibernateUtil.currentSession().createCriteria(AlurPersetujuanSuratMasukStatus.class)
				.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("suratMasuk", suratMasuk))
				.addOrder(Order.asc("id")).list();
		alurMasukByParentIdCache.put(suratMasuk.getId(), list == null ? new ArrayList() : list);
		return list == null ? new ArrayList() : list;
	}

	private Date getTanggalDirubah(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				return ((AlurPersetujuanSuratKeluarStatus) status).getTanggal_dirubah();
			}
			if (status instanceof AlurPersetujuanSuratMasukStatus) {
				return ((AlurPersetujuanSuratMasukStatus) status).getTanggal_dirubah();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:3144");
		}
		return null;
	}

	private long calculateSlaHours(Object status) {
		/*
		 * V6: SLA berjalan antar alur. Start time diambil dari waktu tindak lanjut
		 * alur sebelumnya. Jika ini alur pertama, fallback ke waktu parent surat.
		 */
		Date start = getSlaStartTimeFromPreviousAlur(status);
		Date end = getStatusTime(status);
		if (start == null) {
			return 0L;
		}
		if (end == null || isPending(status)) {
			end = new Date();
		}
		long diff = end.getTime() - start.getTime();
		return diff <= 0L ? 0L : Math.round(diff / (1000.0d * 60.0d * 60.0d));
	}

	private Date getSlaStartTimeFromPreviousAlur(Object status) {
		Object previous = getPreviousAlurStatus(status);
		if (previous != null) {
			Date d = getStatusTime(previous);
			if (d == null) {
				d = getTanggalDirubah(previous);
			}
			if (d != null) {
				return d;
			}
		}
		return getStartTime(status);
	}

	private Date getStartTime(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				SuratKeluar surat = ((AlurPersetujuanSuratKeluarStatus) status).getSuratKeluar();
				if (surat != null && surat.getWaktu() != null) {
					return surat.getWaktu();
				}
				if (surat != null && surat.getTanggal() != null) {
					return surat.getTanggal();
				}
				return ((AlurPersetujuanSuratKeluarStatus) status).getTanggal_dirubah();
			}
			if (status instanceof AlurPersetujuanSuratMasukStatus) {
				SuratMasuk surat = ((AlurPersetujuanSuratMasukStatus) status).getSuratMasuk();
				if (surat != null && surat.getWaktu() != null) {
					return surat.getWaktu();
				}
				if (surat != null && surat.getTanggal() != null) {
					return surat.getTanggal();
				}
				return ((AlurPersetujuanSuratMasukStatus) status).getTanggal_dirubah();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:3202");
		}
		return null;
	}

	private Date getStatusTime(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				AlurPersetujuanSuratKeluarStatus s = (AlurPersetujuanSuratKeluarStatus) status;
				if (Boolean.TRUE.equals(s.getDisetujui())) {
					return s.getWaktuPersetujuan();
				}
				if (Boolean.TRUE.equals(s.getDitolak())) {
					return s.getWaktuDitolak();
				}
				return s.getTanggal_dirubah();
			}
			if (status instanceof AlurPersetujuanSuratMasukStatus) {
				AlurPersetujuanSuratMasukStatus s = (AlurPersetujuanSuratMasukStatus) status;
				if (Boolean.TRUE.equals(s.getDisetujui())) {
					return s.getWaktuPersetujuan();
				}
				if (Boolean.TRUE.equals(s.getDitolak())) {
					return s.getWaktuDitolak();
				}
				return s.getTanggal_dirubah();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:3229");
		}
		return null;
	}

	private boolean isPending(Object status) {
		return isBelumDitindaklanjuti(status);
	}

	private boolean isBelumDitindaklanjuti(Object status) {
		if (status instanceof AlurPersetujuanSuratKeluarStatus) {
			AlurPersetujuanSuratKeluarStatus s = (AlurPersetujuanSuratKeluarStatus) status;
			return !Boolean.TRUE.equals(s.getDisetujui()) && !Boolean.TRUE.equals(s.getDitolak())
					&& !hasPelakuAlur(status);
		}
		if (status instanceof AlurPersetujuanSuratMasukStatus) {
			AlurPersetujuanSuratMasukStatus s = (AlurPersetujuanSuratMasukStatus) status;
			return !Boolean.TRUE.equals(s.getDisetujui()) && !Boolean.TRUE.equals(s.getDitolak())
					&& !hasPelakuAlur(status);
		}
		return false;
	}

	private boolean hasPelakuAlur(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				AlurPersetujuanSuratKeluarStatus s = (AlurPersetujuanSuratKeluarStatus) status;
				return s.getKonseptor() != null || s.getMahasiswa() != null || s.getSiswa() != null;
			}
			if (status instanceof AlurPersetujuanSuratMasukStatus) {
				AlurPersetujuanSuratMasukStatus s = (AlurPersetujuanSuratMasukStatus) status;
				return s.getKonseptor() != null || s.getMahasiswa() != null || s.getSiswa() != null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:3262");
		}
		return false;
	}

	private boolean isMasuk(Object status) {
		return status instanceof AlurPersetujuanSuratMasukStatus;
	}

	private String getStatusText(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				AlurPersetujuanSuratKeluarStatus s = (AlurPersetujuanSuratKeluarStatus) status;
				if (Boolean.TRUE.equals(s.getDitolak())) {
					return "Ditolak";
				}
				if (Boolean.TRUE.equals(s.getTelahDirevisi())) {
					return "Direvisi";
				}
				if (Boolean.TRUE.equals(s.getDisetujui())) {
					return "Disetujui";
				}
				if (Boolean.TRUE.equals(s.getSelesai())) {
					return "Selesai";
				}
			}
			if (status instanceof AlurPersetujuanSuratMasukStatus) {
				AlurPersetujuanSuratMasukStatus s = (AlurPersetujuanSuratMasukStatus) status;
				if (Boolean.TRUE.equals(s.getDitolak())) {
					return "Ditolak";
				}
				if (Boolean.TRUE.equals(s.getTelahDirevisi())) {
					return "Direvisi";
				}
				if (Boolean.TRUE.equals(s.getDisetujui())) {
					return "Disetujui";
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:3300");
		}
		if (isBelumDitindaklanjuti(status)) {
			return "Belum Ditindaklanjuti";
		}
		return "Menunggu";
	}

	private String getStatusColor(Object status) {
		String st = getStatusText(status);
		if ("Ditolak".equals(st)) {
			return "#dc2626";
		}
		if ("Direvisi".equals(st)) {
			return "#2563eb";
		}
		if ("Disetujui".equals(st) || "Selesai".equals(st)) {
			return "#16a34a";
		}
		return "#f59e0b";
	}


	private String getSlaStatusLabel(Object status) {
		long hours = calculateSlaHours(status);
		if (isPending(status)) {
			if (hours > SLA_OVERDUE_HOURS) {
				return "Overdue";
			}
			if (hours > SLA_WARNING_HOURS) {
				return "Warning";
			}
			return "On Track";
		}
		return hours <= SLA_WARNING_HOURS ? "Selesai Tepat Waktu" : "Selesai Terlambat";
	}

	private String getSlaStatusColor(Object status) {
		String label = getSlaStatusLabel(status);
		if ("Overdue".equals(label) || "Selesai Terlambat".equals(label)) {
			return "#991b1b";
		}
		if ("Warning".equals(label)) {
			return "#9a3412";
		}
		return "#166534";
	}

	private String getSlaStatusBg(Object status) {
		String label = getSlaStatusLabel(status);
		if ("Overdue".equals(label) || "Selesai Terlambat".equals(label)) {
			return "#fee2e2";
		}
		if ("Warning".equals(label)) {
			return "#ffedd5";
		}
		return "#dcfce7";
	}

	private String getSlaAction(Object status) {
		String label = getSlaStatusLabel(status);
		if ("Overdue".equals(label)) {
			return "Follow Up / Eskalasi";
		}
		if ("Warning".equals(label)) {
			return "Pantau";
		}
		if ("Selesai Terlambat".equals(label)) {
			return "Evaluasi";
		}
		return "-";
	}

	private String getAlurName(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				AlurPersetujuanSuratKeluar alur = ((AlurPersetujuanSuratKeluarStatus) status)
						.getAlurPersetujuanSuratKeluar();
				if (alur != null && alur.getNama() != null) {
					return alur.getNama();
				}
			}
			if (status instanceof AlurPersetujuanSuratMasukStatus) {
				AlurPersetujuanSuratMasuk alur = ((AlurPersetujuanSuratMasukStatus) status)
						.getAlurPersetujuanSuratMasuk();
				if (alur != null && alur.getNama() != null) {
					return alur.getNama();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:3389");
		}
		return "Tanpa Alur";
	}


	private String getKonseptorName(Object status) {
		return getPengirimAlurName(status);
	}

	private String getPengirimAlurName(Object status) {
		Object previous = getPreviousAlurStatus(status);
		if (previous != null) {
			String actor = getPelakuAlurName(previous);
			if (actor != null && !actor.trim().isEmpty() && !"Tanpa Pelaku".equals(actor)) {
				return actor;
			}
		}
		return getPengirimAwalDariParent(status);
	}

	private String getPenerimaAlurName(Object status) {
		return getPejabatName(status);
	}

	private String getPelakuAlurName(Object status) {
		try {
			Tbmuser konseptor = null;
			Mahasiswa mahasiswa = null;
			Siswa siswa = null;
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				AlurPersetujuanSuratKeluarStatus s = (AlurPersetujuanSuratKeluarStatus) status;
				konseptor = s.getKonseptor();
				mahasiswa = s.getMahasiswa();
				siswa = s.getSiswa();
			} else if (status instanceof AlurPersetujuanSuratMasukStatus) {
				AlurPersetujuanSuratMasukStatus s = (AlurPersetujuanSuratMasukStatus) status;
				konseptor = s.getKonseptor();
				mahasiswa = s.getMahasiswa();
				siswa = s.getSiswa();
			}
			if (konseptor != null) {
				return firstNotEmpty(konseptor.getUserNama(), konseptor.getUserId(), "Tanpa Pelaku");
			}
			if (mahasiswa != null) {
				return firstNotEmpty(mahasiswa.getNama(), String.valueOf(mahasiswa), "Tanpa Pelaku");
			}
			if (siswa != null) {
				return firstNotEmpty(siswa.getNama(), String.valueOf(siswa), "Tanpa Pelaku");
			}
		} catch (Exception e) {
			debugError("getPelakuAlurName", e);
		}
		return "Tanpa Pelaku";
	}

	private String getPengirimAwalDariParent(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				SuratKeluar surat = ((AlurPersetujuanSuratKeluarStatus) status).getSuratKeluar();
				if (surat != null) {
					if (surat.getMahasiswa() != null) {
						return firstNotEmpty(surat.getMahasiswa().getNama(), String.valueOf(surat.getMahasiswa()),
								"Pengajuan Awal");
					}
					if (surat.getSiswa() != null) {
						return firstNotEmpty(surat.getSiswa().getNama(), String.valueOf(surat.getSiswa()),
								"Pengajuan Awal");
					}
					if (surat.getKonseptor() != null) {
						return firstNotEmpty(surat.getKonseptor().getUserNama(), surat.getKonseptor().getUserId(),
								"Pengajuan Awal");
					}
				}
			} else if (status instanceof AlurPersetujuanSuratMasukStatus) {
				SuratMasuk surat = ((AlurPersetujuanSuratMasukStatus) status).getSuratMasuk();
				if (surat != null && surat.getKonseptor() != null) {
					return firstNotEmpty(surat.getKonseptor().getUserNama(), surat.getKonseptor().getUserId(),
							"Pengajuan Awal");
				}
			}
		} catch (Exception e) {
			debugError("getPengirimAwalDariParent", e);
		}
		return "Pengajuan Awal";
	}

	private String getPejabatName(Object status) {
		/*
		 * V5 FIX:
		 * Jabatan saat ini mengikuti field pejabat pada status alur:
		 * String namaPejabat = pejabat.getNama();
		 */
		try {
			Pejabat pejabat = null;
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				pejabat = ((AlurPersetujuanSuratKeluarStatus) status).getPejabat();
			} else if (status instanceof AlurPersetujuanSuratMasukStatus) {
				pejabat = ((AlurPersetujuanSuratMasukStatus) status).getPejabat();
			}
			if (pejabat != null) {
				return firstNotEmpty(pejabat.getNama(), String.valueOf(pejabat), "Tanpa Pejabat");
			}
		} catch (Exception e) {
			debugError("getPejabatName", e);
		}
		return "Tanpa Pejabat";
	}

	private String getKlasifikasiName(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				SuratKeluar surat = ((AlurPersetujuanSuratKeluarStatus) status).getSuratKeluar();
				if (surat != null && surat.getKlasifikasiSuratKeluar() != null
						&& surat.getKlasifikasiSuratKeluar().getNama() != null) {
					return "Keluar - " + surat.getKlasifikasiSuratKeluar().getNama();
				}
			}
			if (status instanceof AlurPersetujuanSuratMasukStatus) {
				SuratMasuk surat = ((AlurPersetujuanSuratMasukStatus) status).getSuratMasuk();
				if (surat != null && surat.getKlasifikasiSuratMasuk() != null
						&& surat.getKlasifikasiSuratMasuk().getNama() != null) {
					return "Masuk - " + surat.getKlasifikasiSuratMasuk().getNama();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:3514");
		}
		return "Tanpa Klasifikasi";
	}

	private String getSatkerName(Object status) {
		try {
			SatuanKerja sk = null;
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				SuratKeluar surat = ((AlurPersetujuanSuratKeluarStatus) status).getSuratKeluar();
				sk = surat == null ? null : surat.getSatuanKerja();
			} else if (status instanceof AlurPersetujuanSuratMasukStatus) {
				SuratMasuk surat = ((AlurPersetujuanSuratMasukStatus) status).getSuratMasuk();
				sk = surat == null ? null : surat.getSatuanKerja();
			}
			if (sk != null && sk.getNama() != null) {
				return sk.getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:3532");
		}
		return "Tanpa Satuan Kerja";
	}

	private Date getTanggal(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				SuratKeluar surat = ((AlurPersetujuanSuratKeluarStatus) status).getSuratKeluar();
				return surat == null ? null : surat.getTanggal();
			}
			if (status instanceof AlurPersetujuanSuratMasukStatus) {
				SuratMasuk surat = ((AlurPersetujuanSuratMasukStatus) status).getSuratMasuk();
				return surat == null ? null : surat.getTanggal();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:3547");
		}
		return null;
	}

	private String getKode(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				SuratKeluar surat = ((AlurPersetujuanSuratKeluarStatus) status).getSuratKeluar();
				return firstNotEmpty(surat == null ? null : surat.getKode(), "-");
			}
			if (status instanceof AlurPersetujuanSuratMasukStatus) {
				SuratMasuk surat = ((AlurPersetujuanSuratMasukStatus) status).getSuratMasuk();
				return firstNotEmpty(surat == null ? null : surat.getKode(), surat == null ? null : surat.getNoSurat(), "-");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:3562");
		}
		return "-";
	}

	private String getPerihal(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				SuratKeluar surat = ((AlurPersetujuanSuratKeluarStatus) status).getSuratKeluar();
				return firstNotEmpty(surat == null ? null : surat.getPerihal(), surat == null ? null : surat.getNama(), "-");
			}
			if (status instanceof AlurPersetujuanSuratMasukStatus) {
				SuratMasuk surat = ((AlurPersetujuanSuratMasukStatus) status).getSuratMasuk();
				return firstNotEmpty(surat == null ? null : surat.getPerihal(), surat == null ? null : surat.getNama(), "-");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:3577");
		}
		return "-";
	}

	private String getKeterangan(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				return ((AlurPersetujuanSuratKeluarStatus) status).getKeterangan();
			}
			if (status instanceof AlurPersetujuanSuratMasukStatus) {
				return ((AlurPersetujuanSuratMasukStatus) status).getKeterangan();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:3590");
		}
		return "-";
	}

	private String getCatatan(Object status) {
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				return ((AlurPersetujuanSuratKeluarStatus) status).getCatatanRevisi();
			}
			if (status instanceof AlurPersetujuanSuratMasukStatus) {
				return ((AlurPersetujuanSuratMasukStatus) status).getCatatanRevisi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:3603");
		}
		return "-";
	}


	private List sortSlaPerformance(Map<String, SlaPerformanceRow> source) {
		List list = new ArrayList();
		if (source == null) {
			return list;
		}
		for (String key : source.keySet()) {
			SlaPerformanceRow row = (SlaPerformanceRow) source.get(key);
			if (row != null) {
				list.add(row);
			}
		}
		Collections.sort(list, new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				SlaPerformanceRow a = (SlaPerformanceRow) o1;
				SlaPerformanceRow b = (SlaPerformanceRow) o2;
				long pa = a.totalClosed <= 0L ? 0L : Math.round((a.tepatWaktu * 100.0d) / a.totalClosed);
				long pb = b.totalClosed <= 0L ? 0L : Math.round((b.tepatWaktu * 100.0d) / b.totalClosed);
				if (pa == pb) {
					return (int) (b.overdue - a.overdue);
				}
				return (int) (pa - pb);
			}
		});
		return list;
	}

	private String getSlaPerformanceStatus(long pct) {
		if (pct <= 40L) {
			return "Sangat Tidak Baik";
		}
		if (pct <= 70L) {
			return "Tidak Baik";
		}
		if (pct <= 85L) {
			return "Cukup Baik";
		}
		if (pct <= 99L) {
			return "Baik";
		}
		return "Sangat Baik";
	}

	private String getSlaPerformanceAction(long pct) {
		if (pct <= 70L) {
			return "Perlu Lebih Pengawasan";
		}
		if (pct <= 99L) {
			return "Diperhatikan";
		}
		return "-";
	}

	private String getSlaPerformanceColor(long pct) {
		if (pct <= 70L) {
			return "#991b1b";
		}
		if (pct <= 85L) {
			return "#92400e";
		}
		return "#166534";
	}

	private String formatTrendMonth(Date date) {
		try {
			return date == null ? "Tanpa Tanggal" : new java.text.SimpleDateFormat("MMM yyyy", new java.util.Locale("id", "ID")).format(date);
		} catch (Exception e) {
			return "Tanpa Tanggal";
		}
	}

	private String topLabel(Map<String, Integer> map, String fallback) {
		if (map == null || map.isEmpty()) {
			return fallback;
		}
		List list = sortCounter(map);
		return list.isEmpty() ? fallback : ((CounterItem) list.get(0)).label;
	}

	private String formatDate(Date date) {
		try {
			return date == null ? "-" : Common.dateFormat.get().format(date);
		} catch (Exception e) {
			return "-";
		}
	}

	private String formatDateTime(Date date) {
		try {
			return date == null ? "-" : Common.dateFormat3.get().format(date);
		} catch (Exception e) {
			return "-";
		}
	}

	private String firstNotEmpty(String a, String fallback) {
		return a == null || a.trim().isEmpty() ? fallback : a;
	}

	private String firstNotEmpty(String a, String b, String fallback) {
		if (a != null && !a.trim().isEmpty()) {
			return a;
		}
		if (b != null && !b.trim().isEmpty()) {
			return b;
		}
		return fallback;
	}

	private String safeHtml(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private void debugError(String context, Exception e) {
		if (!debug) {
			return;
		}
		try {
			System.err.println("[DasboardAlurSurat DEBUG] " + context + " : " + (e == null ? "" : e.getMessage()));
			if (e != null) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/DasboardAlurSurat.java:3733");
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardAlurSurat.java:3735");
		}
	}

	/**
	 * Tipe implementasi bersarang {@link CounterItem} milik {@link DasboardAlurSurat}. Kelas ini memberi nama pada
	 * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardAlurSurat}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String label}, {@code int count}.
	 * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see DasboardAlurSurat
	 */
	private static class CounterItem {
		String label;
		int count;
	}

	/**
	 * Tipe implementasi bersarang {@link AlurSuratItem} milik {@link DasboardAlurSurat}. Kelas ini memberi nama
	 * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardAlurSurat}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String jenis}, {@code String kode},
	 * {@code String perihal}, {@code String alur}, {@code String pejabat}, {@code String konseptor}, {@code String
	 * status}, {@code String waktu}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 *
	 * @see DasboardAlurSurat
	 */
	private static class AlurSuratItem {
		String jenis;
		String kode;
		String perihal;
		String alur;
		String pejabat;
		String konseptor;
		String status;
		String waktu;
		String waktuMasuk;
		String statusSla;
		String slaColor;
		String slaBg;
		String action;
		long slaHours;
		String color;
	}

	/**
	 * Tipe implementasi bersarang {@link SlaPerformanceRow} milik {@link DasboardAlurSurat}. Kelas ini memberi
	 * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardAlurSurat}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String jenisSurat}, {@code String
	 * konseptor}, {@code String jabatan}, {@code long totalClosed}, {@code long tepatWaktu}, {@code long
	 * tidakTepatWaktu}, {@code long outstanding}, {@code long overdue}. Aturan bisnis bersama tetap berada pada
	 * kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see DasboardAlurSurat
	 */
	private static class SlaPerformanceRow {
		String jenisSurat;
		String konseptor;
		String jabatan;
		long totalClosed;
		long tepatWaktu;
		long tidakTepatWaktu;
		long outstanding;
		long overdue;
	}


	/**
	 * Tipe implementasi bersarang {@link AlurParentGroupRow} milik {@link DasboardAlurSurat}. Kelas ini memberi
	 * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardAlurSurat}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String jenis}, {@code String
	 * tanggal}, {@code String kode}, {@code String perihal}, {@code String parameterHtml}, {@code String
	 * timelineHtml}, {@code String ringkasanHtml}. Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 *
	 * @see DasboardAlurSurat
	 */
	private static class AlurParentGroupRow {
		String jenis;
		String tanggal;
		String kode;
		String perihal;
		String parameterHtml;
		String timelineHtml;
		String ringkasanHtml;
	}



	/**
	 * Tipe implementasi bersarang {@link DashboardRenderRequestV16} milik {@link DasboardAlurSurat}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardAlurSurat}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Component parent}, {@code Date
	 * mulai}, {@code Date sampai}, {@code SatuanKerja satuanKerja}, {@code String keyword}. Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see DasboardAlurSurat
	 */
	private static class DashboardRenderRequestV16 {
		Component parent;
		Date mulai;
		Date sampai;
		SatuanKerja satuanKerja;
		String keyword;

		DashboardRenderRequestV16(Component parent, Date mulai, Date sampai, SatuanKerja satuanKerja, String keyword) {
			this.parent = parent;
			this.mulai = mulai;
			this.sampai = sampai;
			this.satuanKerja = satuanKerja;
			this.keyword = keyword;
		}
	}


	/**
	 * Tipe implementasi bersarang {@link AlurSuratDashboardData} milik {@link DasboardAlurSurat}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardAlurSurat}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Date mulai}, {@code Date sampai},
	 * {@code SatuanKerja satker}, {@code String keyword}, {@code long totalTahap}, {@code long totalKeluar},
	 * {@code long totalMasuk}, {@code long menunggu}. Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 *
	 * @see DasboardAlurSurat
	 */
	private static class AlurSuratDashboardData {
		Date mulai;
		Date sampai;
		SatuanKerja satker;
		String keyword;

		long totalTahap;
		long totalKeluar;
		long totalMasuk;
		long menunggu;
		long pendingKeluar;
		long pendingMasuk;
		long disetujui;
		long ditolak;
		long direvisi;
		long selesai;
		long slaOverdue;
		long overdueKeluar;
		long overdueMasuk;
		long slaWarning;
		long slaAman;
		long age0_24;
		long age24_48;
		long age48_72;
		long age72Plus;
		long tanpaKode;
		long tanpaPerihal;
		long tanpaAlur;
		long tanpaPejabat;
		long slaClosed;
		long selesaiTepatWaktu;
		long selesaiTerlambat;
		long slaCompliancePct = 100L;
		double avgSlaHours;
		long avgSlaTotalHours;
		long avgSlaClosedCount;
		long avgSlaExcludedPendingCount;

		Map<String, Integer> perAlur = new TreeMap<String, Integer>();
		Map<String, Integer> perPejabat = new TreeMap<String, Integer>();
		Map<String, Integer> perKlasifikasi = new TreeMap<String, Integer>();
		Map<String, Integer> perSatker = new TreeMap<String, Integer>();
		Map<String, Integer> slaRiskPerPejabat = new TreeMap<String, Integer>();
		Map<String, SlaPerformanceRow> slaPerformanceMap = new TreeMap<String, SlaPerformanceRow>();
		Map<String, Integer> trendSlaTotal = new TreeMap<String, Integer>();
		Map<String, Integer> trendSlaOnTime = new TreeMap<String, Integer>();
		List<AlurSuratItem> recentItems = new ArrayList<AlurSuratItem>();
		List<AlurSuratItem> slaWatchlist = new ArrayList<AlurSuratItem>();
	}
}
