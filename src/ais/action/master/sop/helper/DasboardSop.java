package ais.action.master.sop.helper;
import ais.ui.util.DashboardGridExportHelper;

/* DASBOARD_SOP_WAITING_PANEL_FIX_V16_2026_06_04 */

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.MoveEvent;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.A;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.sop.DisposisiSopAction;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.DashboardCacheUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sop.AktorSop;
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.sop.Sop;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBoldBiru;
import ais.ui.util.MyLabelAgakKecilBoldHijau;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Komponen dashboard khusus untuk dasboard sop. Kelas ini memilih variasi data atau tampilan
 * dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyPortallayout}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code boolean debug}, {@code boolean debuh},
 * {@code int DETAIL_PAGE_SIZE}, {@code Tbmuser tbmuser}, {@code Tabs tabs}, {@code Tabpanels tabpanels}, {@code
 * Component menu}, {@code int DASHBOARD_SAMPLE_LIMIT}; inisialisasi/lifecycle ({@code init()});
 * pembacaan/pencarian ({@code getAktorRestriction()}, {@code getAktorRestriction()}, {@code
 * getCountPengajuanBaru()}, {@code getCountDataPengajuanAnda()}, {@code tampilkanLoadingDashboardSop()}, {@code
 * updateLoadingDashboardSop()}); validasi/perhitungan ({@code invalidateSopDashboardCache()}, {@code
 * bolehSetujuiMassal()}); mutasi data ({@code renderFunnelProsesSop()}, {@code attachKlikProses()}, {@code
 * jalankanSetujuiMassalMenunggu()}, {@code dialogSetujuiMassal()}, {@code setujuiTugas()});
 * penghapusan/pembatalan ({@code removeCriteriaReferences()}); pelaporan/ekspor ({@code renderHomeDasborTab()},
 * {@code renderHomeDasborContentAsync()}, {@code renderHomeDasborContent()}, {@code renderHeroDasbor()}, {@code
 * renderMetricCards()}, {@code renderDasborSopGlobalFilter()}); operasi domain lain ({@code
 * createDipantauCriteria()}, {@code analyzeDashboardRows()}, {@code clearTime()}, {@code addCounter()}, {@code
 * buildRecentItem()}, {@code isDisposisiSelesai()}); konfigurasi constructor: {@code tbmuser}. Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyPortallayout
 */
public class DasboardSop extends MyPortallayout {

	private static final long serialVersionUID = -9006490521125337935L;

	/**
	 * Aktifkan ke true jika ingin melihat stacktrace error dashboard/detail popup.
	 * Field debuh disediakan karena mengikuti permintaan user sebelumnya; debug tetap
	 * dipertahankan agar lebih mudah dibaca oleh developer lain.
	 */
	public static boolean debug = false;
	public static boolean debuh = false;
	private static final int DETAIL_PAGE_SIZE = 10;
	private Tbmuser tbmuser;
	private Tabs tabs;
	private Tabpanels tabpanels;
	private Component menu;

	private static final int DASHBOARD_SAMPLE_LIMIT = 500;

	/*
	 * FILTER GLOBAL DASBOR SOP
	 * Filter ini dipakai oleh: card overview, popup detail, dan dashboard analitis.
	 * Panel operasional existing memakai form pencarian lokal masing-masing.
	 */
	private MyPortallayout dashboardExistingLayout;
	/*
	 * Khusus panel operasional existing: buildPengajuanBaru, buildPengajuanBaruDisposisi,
	 * buildPengajuanSelesai, buildMenungguDisposisi, dan buildMenungguAktorDisposisi
	 * memakai form pencarian lokal masing-masing, sehingga tidak ikut filter global dasbor.
	 */
	private boolean ignoreGlobalFilterForLegacyPanel = false;
	private Date dashboardFilterMulai;
	private Date dashboardFilterSampai;
	private ais.database.model.rab.SatuanKerja dashboardFilterSatker;
	private String dashboardFilterKeyword = "";

	private transient org.zkoss.zul.Html dashboardLoadingHtml;
	private transient String dashboardLoadingTitle = "Memproses Dasbor SOP";
	private final Map<Criteria, Session> criteriaSessionMap = new HashMap<Criteria, Session>();


	private Criterion getAktorRestriction() {
		return AktorSop.buatCriterion(tbmuser);
	}

	private Criterion getAktorRestriction(Criteria criteria) {
		return AktorSop.buatCriterion(tbmuser, true, criteria);
	}

	public DasboardSop(Tabs tabs, Tabpanels tabpanels, Component menu) throws Exception {
		super();
		this.tabs = tabs;
		this.tabpanels = tabpanels;
		this.menu = menu;
		setWidth("100%");
		setMaximizedMode("whole");
		tbmuser = Common.getCurrentUser();
		init();
	}

	private int getCountPengajuanBaru() {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Criteria criteria = createCriteria(session, AlurSop.class).add(Restrictions.eq("start", true))
					.setProjection(Projections.rowCount()).createAlias("aktorSop", "aktorSop")
					.add(getAktorRestriction());
			if (!ignoreGlobalFilterForLegacyPanel) {
				applyGlobalAlurSopFilter(criteria);
			}
			int jml = ((Number) criteria.uniqueResult()).intValue();
			return jml;
		} catch (Exception e) {
			printDebug(e);
			return 0;
		} finally {
			closeSessionSafely(session);
		}
	}


	private int getCountDataPengajuanAnda() {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Criteria criteria = createCriteria(session, DisposisiSop.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(getUserRestriction(tbmuser, ""));
			try {
				criteria.createAlias("sop", "sopDetail", Criteria.LEFT_JOIN);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:162");
			}
			applyGlobalDisposisiSopFilter(criteria);
			criteria.setProjection(Projections.rowCount());
			Object result = criteria.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			printDebug(e);
			return 0;
		} finally {
			closeSessionSafely(session);
		}
	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Sop");
		renderHomeDasborTab();
	}

	private void renderHomeDasborTab() throws Exception {
		tbmuser = Common.getCurrentUser();
		renderHomeDasborContentAsync(this, dashboardFilterMulai, dashboardFilterSampai, dashboardFilterSatker,
				dashboardFilterKeyword);
	}

	private void renderHomeDasborContentAsync(final Component parent, Date mulai, Date sampai,
			final ais.database.model.rab.SatuanKerja satuanKerja, String keyword) throws Exception {
		if (parent == null) {
			return;
		}

		// Default filter tanggal global dikosongkan agar dashboard menampilkan semua data SOP.
		// Tanggal hanya dipakai jika user memilih nilai pada filter Mulai/Sampai.
		if (keyword == null) {
			keyword = "";
		}

		final Date finalMulai = mulai;
		final Date finalSampai = sampai;
		final String finalKeyword = keyword.trim();

		dashboardFilterMulai = finalMulai;
		dashboardFilterSampai = finalSampai;
		dashboardFilterSatker = satuanKerja;
		dashboardFilterKeyword = finalKeyword;

		tampilkanLoadingDashboardSop(parent, "Menyiapkan dasbor SOP dan filter awal...", 5);

		final SopDashboardData[] dashboardDataRef = new SopDashboardData[1];

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					updateLoadingDashboardSop("Menghitung card overview: pengajuan, antrian, disposisi, selesai, dan batas waktu...", 25);
					dashboardDataRef[0] = loadSopDashboardDataWithCache();
					updateLoadingDashboardSop("Data ringkasan berhasil diambil. Menyiapkan layout dasbor operasional...", 60);

					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event event2) throws Exception {
							try {
								updateLoadingDashboardSop("Merender dasbor operasional existing dan analitik alur kerja SOP...", 78);

								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event event3) throws Exception {
										try {
											updateLoadingDashboardSop("Menyelesaikan tampilan dasbor SOP...", 92);
											renderHomeDasborContent(parent, finalMulai, finalSampai, satuanKerja,
													finalKeyword, dashboardDataRef[0]);
										} catch (Exception e) {
											tampilkanErrorLoadingDashboardSop(parent, e);
											throw e;
										}
									}
								});
							} catch (Exception e) {
								tampilkanErrorLoadingDashboardSop(parent, e);
								throw e;
							}
						}
					});
				} catch (Exception e) {
					tampilkanErrorLoadingDashboardSop(parent, e);
					throw e;
				}
			}
		});
	}

	private void renderHomeDasborContent(final Component parent, Date mulai, Date sampai,
			ais.database.model.rab.SatuanKerja satuanKerja, String keyword, SopDashboardData preloadedDashboardData)
			throws Exception {
		if (parent == null) {
			return;
		}
		Common.clear(parent);

		// Default filter tanggal global dikosongkan agar dashboard menampilkan semua data SOP.
		// Tanggal hanya dipakai jika user memilih nilai pada filter Mulai/Sampai.
		if (keyword == null) {
			keyword = "";
		}

		dashboardFilterMulai = mulai;
		dashboardFilterSampai = sampai;
		dashboardFilterSatker = satuanKerja;
		dashboardFilterKeyword = keyword.trim();

		Panelchildren dashboardContainer = null;
		if (parent instanceof MyPortallayout) {
			MyPortalchildren wrapper = new MyPortalchildren();
			wrapper.setWidth("100%");
			wrapper.setStyle("padding:6px; box-sizing:border-box;");
			wrapper.setParent(parent);

			Panel panel = new ais.ui.util.MyPanelConfig();
			panel.setTitle("Dasbor");
			panel.setBorder("none");
			panel.setCollapsible(false);
			panel.setClosable(false);
			panel.setMaximizable(false);
			panel.setMinimizable(false);
			panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:18px; overflow:hidden;"
					+ "background:#ffffff; box-shadow:0 14px 28px rgba(15,23,42,.08);");
			panel.setParent(wrapper);

			dashboardContainer = new Panelchildren();
			dashboardContainer.setStyle("padding:0; background:#f6f8fb;");
			dashboardContainer.setParent(panel);
		} else {
			dashboardContainer = new Panelchildren();
			dashboardContainer.setStyle("padding:0; background:#f6f8fb;");
			dashboardContainer.setParent(parent);
		}

		org.zkoss.zul.Div shell = new org.zkoss.zul.Div();
		shell.setWidth("100%");
		shell.setStyle("background:#f6f8fb; padding:14px; box-sizing:border-box; overflow:auto;");
		shell.setParent(dashboardContainer);

		final SopDashboardData dashboardData = preloadedDashboardData == null ? loadSopDashboardData() : preloadedDashboardData;

		updateLoadingDashboardSop("Menampilkan hero, filter, dan card overview SOP...", 72);
		renderHeroDasbor(shell, dashboardData);
		renderDasborSopGlobalFilter(shell, dashboardFilterMulai, dashboardFilterSampai, dashboardFilterSatker, dashboardFilterKeyword);
		renderMetricCards(shell, dashboardData);
		updateLoadingDashboardSop("Menampilkan panel operasional existing SOP...", 82);
		renderExistingDashboardOperasional(shell);

		MyPortallayout portalLayout = new MyPortallayout();
		portalLayout.setWidth("100%");
		portalLayout.setStyle("margin-top:12px;");
		portalLayout.setParent(shell);

		String pcWidth = Common.isMobile() ? "100%" : "50%";

		MyPortalchildren pcTop = new MyPortalchildren();
		pcTop.setWidth("100%");
		pcTop.setStyle("padding:6px;");
		pcTop.setParent(portalLayout);

		MyPortalchildren pcLeft = new MyPortalchildren();
		pcLeft.setWidth(pcWidth);
		pcLeft.setStyle("padding:6px;");
		pcLeft.setParent(portalLayout);

		MyPortalchildren pcRight = new MyPortalchildren();
		pcRight.setWidth(pcWidth);
		pcRight.setStyle("padding:6px;");
		pcRight.setParent(portalLayout);

		MyPortalchildren pcBottom = new MyPortalchildren();
		pcBottom.setWidth("100%");
		pcBottom.setStyle("padding:6px;");
		pcBottom.setParent(portalLayout);

		updateLoadingDashboardSop("Menampilkan dasbor analitik SOP dan alur kerja...", 90);
		renderFunnelProsesSop(pcTop, dashboardData);
		renderDeadlineAnalytic(pcLeft, dashboardData);
		renderSebaranSop(pcRight, dashboardData);
		renderBebanAktor(pcLeft, dashboardData);
		renderAktivitasTerbaru(pcRight, dashboardData);
		renderQuickInsight(pcBottom, dashboardData);
		renderWorkflowDetailAnalytics(pcBottom, dashboardData);

		// Dashboard tambahan V7 diadaptasi dari template DasboardSurat V10
		// dan disesuaikan dengan data/criteria SOP.
		renderDasborSopTemplateTambahanV7(shell, dashboardData);
	}


	private void tampilkanLoadingDashboardSop(Component parent, String message, int percent) throws Exception {
		if (parent == null) {
			return;
		}
		Common.clear(parent);
		dashboardLoadingTitle = "Memproses Dasbor SOP";
		dashboardLoadingHtml = new org.zkoss.zul.Html(buildLoadingDashboardSopHtml(message, percent));

		Vbox containerDasborGrid = new Vbox();
		containerDasborGrid.setWidth("100%");
		containerDasborGrid.setStyle("padding:14px; box-sizing:border-box; background:#f6f8fb;");

		Panelchildren loadingPanelchildren = null;
		if (parent instanceof MyPortallayout) {
			MyPortalchildren wrapper = new MyPortalchildren();
			wrapper.setWidth("100%");
			wrapper.setStyle("padding:6px; box-sizing:border-box;");
			wrapper.setParent(parent);

			Panel panel = new ais.ui.util.MyPanelConfig();
			panel.setTitle("Dasbor");
			panel.setBorder("none");
			panel.setCollapsible(false);
			panel.setClosable(false);
			panel.setMaximizable(false);
			panel.setMinimizable(false);
			panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:18px; overflow:hidden;"
					+ "background:#ffffff; box-shadow:0 14px 28px rgba(15,23,42,.08);");
			panel.setParent(wrapper);

			loadingPanelchildren = new Panelchildren();
			loadingPanelchildren.setStyle("padding:0; background:#f6f8fb;");
			loadingPanelchildren.setParent(panel);
			containerDasborGrid.setParent(loadingPanelchildren);
		} else {
			containerDasborGrid.setParent(parent);
		}

		containerDasborGrid.appendChild(dashboardLoadingHtml);
	}

	private void updateLoadingDashboardSop(String message, int percent) {
		try {
			if (dashboardLoadingHtml != null) {
				dashboardLoadingHtml.setContent(buildLoadingDashboardSopHtml(message, percent));
			}
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private String buildLoadingDashboardSopHtml(String message, int percent) {
		if (percent < 0) {
			percent = 0;
		}
		if (percent > 100) {
			percent = 100;
		}
		String safeMessage = escapeHtml(message == null ? "Memproses data dasbor SOP..." : message);
		return "<div style=\"padding:22px; border-radius:18px; background:#ffffff; border:1px solid #e5e7eb; "
				+ "box-shadow:0 14px 32px rgba(15,23,42,.08); color:#0f172a;\">"
				+ "<div style=\"display:flex; align-items:center; justify-content:space-between; gap:12px; flex-wrap:wrap;\">"
				+ "<div>"
				+ "<div style=\"font-size:11px; letter-spacing:.12em; text-transform:uppercase; color:#2563eb; font-weight:900;\">"
				+ escapeHtml(dashboardLoadingTitle) + "</div>"
				+ "<div style=\"font-size:18px; font-weight:900; margin-top:6px;\">"
				+ "<i class=\"fa fa-spinner fa-spin\"></i> Mengambil Data Dasbor SOP</div>"
				+ "<div style=\"font-size:12px; color:#64748b; margin-top:8px; line-height:1.55;\">"
				+ safeMessage + "</div>"
				+ "</div>"
				+ "<div style=\"min-width:86px; text-align:right; font-size:30px; font-weight:900; color:#0f172a;\">"
				+ percent + "%</div>"
				+ "</div>"
				+ "<div style=\"height:12px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:18px;\">"
				+ "<div style=\"height:12px; width:" + percent + "%; border-radius:999px; "
				+ "background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));\"></div>"
				+ "</div>"
				+ "<div style=\"display:flex; gap:8px; flex-wrap:wrap; margin-top:14px; font-size:11px; color:#475569;\">"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#eff6ff; color:#1d4ed8; font-weight:800;\">Overview</span>"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#fef3c7; color:#92400e; font-weight:800;\">Panel Existing</span>"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#ecfdf5; color:#166534; font-weight:800;\">Analitik Alur Kerja</span>"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#f5f3ff; color:#5b21b6; font-weight:800;\">Popup Detail</span>"
				+ "</div>"
				+ "</div>";
	}

	private void tampilkanErrorLoadingDashboardSop(Component parent, Exception e) {
		try {
			printDebug(e);
			Common.clear(parent);
			Vbox box = new Vbox();
			box.setWidth("100%");
			box.setStyle("padding:14px; box-sizing:border-box; background:#f6f8fb;");

			org.zkoss.zul.Html html = new org.zkoss.zul.Html("<div style='padding:18px; border-radius:16px; "
					+ "background:#fff7ed; border:1px solid #fed7aa; color:#9a3412; font-size:12px; line-height:1.55;'>"
					+ "<b>Dasbor SOP belum dapat dimuat.</b><br/>"
					+ "Silakan aktifkan <b>debug = true</b> atau <b>debuh = true</b> untuk melihat detail error di console."
					+ "</div>");

			if (parent instanceof MyPortallayout) {
				MyPortalchildren wrapper = new MyPortalchildren();
				wrapper.setWidth("100%");
				wrapper.setStyle("padding:6px; box-sizing:border-box;");
				wrapper.setParent(parent);

				Panel panel = new ais.ui.util.MyPanelConfig();
				panel.setTitle("Dasbor");
				panel.setBorder("none");
				panel.setCollapsible(false);
				panel.setClosable(false);
				panel.setMaximizable(false);
				panel.setMinimizable(false);
				panel.setStyle("margin-bottom:12px; border:1px solid #fed7aa; border-radius:18px; overflow:hidden;"
						+ "background:#ffffff; box-shadow:0 14px 28px rgba(15,23,42,.08);");
				panel.setParent(wrapper);

				Panelchildren pch = new Panelchildren();
				pch.setStyle("padding:0; background:#f6f8fb;");
				pch.setParent(panel);
				box.setParent(pch);
			} else {
				box.setParent(parent);
			}
			box.appendChild(html);
		} catch (Exception ex) {
			printDebug(ex);
		}
	}

	private SopDashboardData loadSopDashboardDataWithCache() {
		String key = getSopDashboardCacheKey(dashboardFilterMulai, dashboardFilterSampai,
				dashboardFilterSatker, dashboardFilterKeyword);
		Object fromL2 = DashboardCacheUtil.getL2(key);
		if (fromL2 instanceof SopDashboardData) return (SopDashboardData) fromL2;
		Object fromL3 = DashboardCacheUtil.getL3(key);
		if (fromL3 instanceof SopDashboardData) {
			DashboardCacheUtil.putL2(key, fromL3);
			return (SopDashboardData) fromL3;
		}
		SopDashboardData d = loadSopDashboardData();
		DashboardCacheUtil.putL2(key, d);
		DashboardCacheUtil.putL3(key, d);
		return d;
	}

	private String getSopDashboardCacheKey(Date mulai, Date sampai,
			ais.database.model.rab.SatuanKerja satuanKerja, String keyword) {
		String fp = (mulai != null ? String.valueOf(mulai.getTime()) : "0")
				+ "_" + (sampai != null ? String.valueOf(sampai.getTime()) : "0")
				+ "_" + (satuanKerja != null ? String.valueOf(satuanKerja.getId()) : "all")
				+ "_" + (keyword != null ? keyword.trim() : "");
		return DashboardCacheUtil.keyWithFilter("DasboardSop", "ADMIN", null, fp);
	}

	private void invalidateSopDashboardCache(Date mulai, Date sampai,
			ais.database.model.rab.SatuanKerja satuanKerja, String keyword) {
		String key = getSopDashboardCacheKey(mulai, sampai, satuanKerja, keyword);
		DashboardCacheUtil.invalidateL2(key);
		DashboardCacheUtil.invalidateL3(key);
	}

	/**
	 * Kriteria "proses yang dipantau pengguna ini" -- sebagai pengaju, petugas,
	 * atau subjek pengajuan.
	 *
	 * <p>Dipisah menjadi metode sendiri supaya bentuknya PERSIS SAMA antara sampel
	 * analitik dan hitungan tepat "Proses Dipantau". Bila keduanya disusun
	 * terpisah, keduanya bisa menyimpang tanpa ketahuan.</p>
	 */
	private Criteria createDipantauCriteria(Session session) {
		Criteria criteria = createCriteria(session, DisposisiAlurSop.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
				.add(Restrictions.isNotNull("alurSop.sop"))
				.createAlias("alurSop.aktorSop", "aktorSop", Criteria.LEFT_JOIN)
				.createAlias("disposisiSop", "disposisiSop")
				.createAlias("sebelumnya", "sebelumnya", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
						Restrictions.eq("disposisiSop.aktif", true)));

		try {
			Criterion pengaju = getUserRestriction(tbmuser, "disposisiSop");
			Criterion petugas = getAktorRestriction(criteria);
			Criterion rootUser = getUserRestriction(tbmuser, "");
			criteria.add(Restrictions.or(pengaju, Restrictions.or(petugas, rootUser)));
		} catch (Exception e) {
			criteria.add(getUserRestriction(tbmuser, "disposisiSop"));
		}

		applyGlobalDisposisiFilter(criteria);
		return criteria;
	}

	private SopDashboardData loadSopDashboardData() {
		SopDashboardData d = new SopDashboardData();
		updateLoadingDashboardSop("Mengambil jumlah data pengajuan Anda...", 28);
		d.jumlahSopBaru = getCountDataPengajuanAnda();
		updateLoadingDashboardSop("Mengambil jumlah proses yang menunggu disposisi Anda...", 32);
		d.menungguSaya = getCountMenunggu();
		updateLoadingDashboardSop("Mengambil riwayat disposisi, data selesai, petugas berjalan, dan batas waktu...", 38);
		d.sudahSayaDisposisi = getCountDisposisi();
		d.selesai = getCountSelesai();
		d.menungguAktor = getCountMenungguAktor();
		d.mendekatiDeadline = getCountMendekatiDeadline();
		d.totalAntrian = d.menungguSaya + d.menungguAktor;
		d.totalAktivitas = d.jumlahSopBaru + d.menungguSaya + d.sudahSayaDisposisi + d.selesai + d.menungguAktor;

		Session session = null;
		try {
			updateLoadingDashboardSop("Mengambil sampel data alur kerja SOP untuk analitik sebaran, petugas, dan batas waktu...", 48);
			session = HibernateUtil.openSession();
			Criteria criteria = createDipantauCriteria(session);

			criteria.addOrder(Order.desc("id"));
			criteria.setMaxResults(DASHBOARD_SAMPLE_LIMIT);

			@SuppressWarnings("unchecked")
			List<DisposisiAlurSop> rows = criteria.list();
			updateLoadingDashboardSop("Menganalisis data alur kerja SOP: SOP dominan, petugas padat, kelengkapan data, dan batas waktu...", 55);
			analyzeDashboardRows(d, rows);
			// "Proses Dipantau" adalah angka utama pada hero, jadi dihitung TEPAT lewat
			// COUNT DISTINCT -- BUKAN dari sampel DASHBOARD_SAMPLE_LIMIT di atas. Dari
			// sampel, angkanya JENUH begitu data melewati batas sampel, sehingga instalasi
			// sibuk selalu melihat angka yang sama dan keliru. Sampel tetap dipakai untuk
			// analitik SEBARAN (per SOP, per petugas, batas waktu) karena di sana sampel
			// memang sah.
			try {
				Object jumlahDipantau = createDipantauCriteria(session)
						.setProjection(Projections.countDistinct("disposisiSop.id")).uniqueResult();
				if (jumlahDipantau != null) {
					d.totalDipantau = ((Number) jumlahDipantau).intValue();
				}
			} catch (Exception eHitungDipantau) {
				// Gagal menghitung tepat -> biarkan angka sampel yang dipakai.
				printDebug(eHitungDipantau);
			}
		} catch (Exception e) {
			// Biarkan kartu utama tetap tampil dari count-count yang sudah aman.
			printDebug(e);
		} finally {
			closeSessionSafely(session);
		}

		if (d.totalDipantau <= 0) {
			d.totalDipantau = d.totalAktivitas;
		}
		return d;
	}

	private void analyzeDashboardRows(SopDashboardData d, List<DisposisiAlurSop> rows) {
		if (rows == null) {
			return;
		}

		Calendar cal = Calendar.getInstance();
		clearTime(cal);
		Date hariIni = cal.getTime();
		cal.add(Calendar.DATE, 1);
		Date besok = cal.getTime();
		cal.add(Calendar.DATE, 6);
		Date tujuhHari = cal.getTime();

		Map<Long, Long> uniqueDisposisi = new HashMap<Long, Long>();

		for (DisposisiAlurSop row : rows) {
			if (row == null || row.getDisposisiSop() == null) {
				continue;
			}

			Long disposisiId = row.getDisposisiSop().getId();
			if (disposisiId != null && !uniqueDisposisi.containsKey(disposisiId)) {
				uniqueDisposisi.put(disposisiId, disposisiId);
			}

			addCounter(d.perSop, getNamaSop(row));
			addCounter(d.perAktor, getNamaAktor(row));
			addCounter(d.perBulan, getBulanTahunKey(row));
			accumulateMetadataQualitySopV7(d, row);

			if (d.aktivitasTerbaru.size() < 8) {
				d.aktivitasTerbaru.add(buildRecentItem(row));
			}

			Date deadline = row.getWaktuMaksimal();
			if (deadline != null && !isDisposisiSelesai(row)) {
				if (deadline.before(hariIni)) {
					d.deadlineLewat++;
				} else if (deadline.before(besok)) {
					d.deadlineHariIni++;
				} else if (deadline.before(tujuhHari)) {
					d.deadlineMingguIni++;
				} else {
					d.deadlineAman++;
				}
			}
		}

		d.totalDipantau = uniqueDisposisi.size();
	}

	private void clearTime(Calendar cal) {
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
	}

	private void addCounter(Map<String, Integer> map, String key) {
		if (key == null || key.trim().isEmpty()) {
			key = "Tidak diketahui";
		}
		Integer current = map.get(key);
		map.put(key, current == null ? 1 : current + 1);
	}

	private SopRecentItem buildRecentItem(DisposisiAlurSop row) {
		SopRecentItem item = new SopRecentItem();
		item.sop = getNamaSop(row);
		item.aktor = getNamaAktor(row);
		item.waktu = getWaktuText(row);
		item.status = getStatusRingkas(row);
		item.deadline = row.getWaktuMaksimal() == null ? "" : Common.dateFormat51.get().format(row.getWaktuMaksimal());
		return item;
	}

	private String getBulanTahunKey(DisposisiAlurSop row) {
		try {
			Date waktu = null;
			try { waktu = row.getWaktu(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:642"); }
			if (waktu == null) {
				try { waktu = row.getDisposisiSop().getWaktu(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:644"); }
			}
			if (waktu != null) {
				String formatted = Common.databaseDateFormat.get().format(waktu);
				if (formatted != null && formatted.length() >= 7) {
					return formatted.substring(0, 7);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:652");
		}
		return "?";
	}

	private String getNamaSop(DisposisiAlurSop row) {
		try {
			if (row != null && row.getDisposisiSop() != null && row.getDisposisiSop().getSop() != null) {
				return row.getDisposisiSop().getSop().getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:662");
		}
		return "SOP Tidak Diketahui";
	}

	private String getNamaAktor(DisposisiAlurSop row) {
		try {
			if (row != null && row.getAlurSop() != null) {
				String aktor = row.getAlurSop().getAktor();
				String tahap = row.getAlurSop().getNama();
				if (aktor != null && tahap != null) {
					return aktor + " - " + tahap;
				}
				if (aktor != null) {
					return aktor;
				}
				if (tahap != null) {
					return tahap;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:682");
		}
		return "Tahap Tidak Diketahui";
	}

	private String getWaktuText(DisposisiAlurSop row) {
		try {
			if (row != null && row.getWaktu() != null) {
				return Common.dateFormat51.get().format(row.getWaktu());
			}
			if (row != null && row.getDisposisiSop() != null && row.getDisposisiSop().getWaktu() != null) {
				return Common.dateFormat51.get().format(row.getDisposisiSop().getWaktu());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:695");
		}
		return "-";
	}

	private String getStatusRingkas(DisposisiAlurSop row) {
		try {
			if (isDisposisiSelesai(row)) {
				return "Selesai";
			}
			if (row.getDiajukanOleh() == null && row.getSiswa() == null && row.getMahasiswa() == null) {
				return "Menunggu Disposisi";
			}
			if (row.getWaktu() != null) {
				return "Sudah Diproses";
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:711");
		}
		return "Dalam Proses";
	}

	private boolean isDisposisiSelesai(DisposisiAlurSop row) {
		try {
			Object v = row.getClass().getMethod("getSelesai").invoke(row);
			return Boolean.TRUE.equals(v);
		} catch (Exception e) {
			try {
				Object v = row.getClass().getMethod("isSelesai").invoke(row);
				return Boolean.TRUE.equals(v);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:724");
			}
		}
		return false;
	}

	private void renderHeroDasbor(Component parent, final SopDashboardData d) {
		org.zkoss.zul.Div hero = new org.zkoss.zul.Div();
		hero.setStyle("position:relative; overflow:hidden; border-radius:18px; padding:22px 24px; color:#ffffff;"
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);"
				+ "box-shadow:0 14px 30px rgba(15,23,42,.18); box-sizing:border-box;");
		hero.setParent(parent);

		appendHtml(hero, "<div style='position:absolute; right:-60px; top:-70px; width:210px; height:210px; border-radius:999px; background:rgba(255,255,255,.12);'></div>"
				+ "<div style='position:absolute; right:90px; bottom:-70px; width:160px; height:160px; border-radius:999px; background:rgba(255,255,255,.10);'></div>");

		org.zkoss.zul.Hbox content = new org.zkoss.zul.Hbox();
		content.setWidth("100%");
		content.setPack("justify");
		content.setAlign("center");
		content.setStyle("position:relative; z-index:1; gap:16px; flex-wrap:wrap;");
		content.setParent(hero);

		org.zkoss.zul.Vbox titleBox = new org.zkoss.zul.Vbox();
		titleBox.setStyle("max-width:720px;");
		titleBox.setParent(content);
		appendHtml(titleBox, "<div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;'>SOP Control Center</div>"
				+ "<div style='font-size:28px; line-height:1.15; font-weight:800; margin-top:6px;'>Dasbor Proses SOP</div>"
				+ "<div style='font-size:13px; opacity:.90; margin-top:8px;'>Pantau alur pengajuan, antrian disposisi, penyelesaian, dan risiko batas waktu dalam satu layar. Klik angka untuk melihat detail datanya.</div>");
		String satkerText = dashboardFilterSatker == null ? "Semua Satker" : dashboardFilterSatker.getNama();
		String keywordText = dashboardFilterKeyword == null || dashboardFilterKeyword.trim().isEmpty() ? "Tanpa keyword" : dashboardFilterKeyword.trim();
		appendHtml(titleBox, "<div style='margin-top:12px; display:flex; gap:8px; flex-wrap:wrap;'>"
				+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>Periode: "
				+ escapeHtml(formatPeriodeDasbor(dashboardFilterMulai, dashboardFilterSampai)) + "</span>"
				+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>" + escapeHtml(satkerText) + "</span>"
				+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>Cari: " + escapeHtml(keywordText) + "</span>"
				+ "</div>");

		org.zkoss.zul.Hbox numberBox = new org.zkoss.zul.Hbox();
		numberBox.setStyle("gap:10px; flex-wrap:wrap;");
		numberBox.setParent(content);
		createHeroNumber(numberBox, "Proses Dipantau", d.totalDipantau, "Detail Proses Dipantau", createAllDashboardCriteriaProvider());
		createHeroNumber(numberBox, "Total Antrian", d.totalAntrian, "Detail Total Antrian", createTotalAntrianCriteriaProvider());
	}


	private void renderMetricCards(Component parent, SopDashboardData d) {
		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px;");
		wrap.setParent(parent);

		createMetricCard(wrap, "Menunggu Saya", d.menungguSaya, "Butuh disposisi / tindak lanjut", "#fee2e2", "#991b1b", "!", "Detail Menunggu Disposisi Saya", createMenungguSayaCriteriaProvider());
		createMetricCard(wrap, "Sudah Disposisi", d.sudahSayaDisposisi, "Riwayat proses oleh Anda", "#dcfce7", "#166534", "✓", "Detail Pengajuan yang Sudah Saya Disposisi", createSudahDisposisiCriteriaProvider());
		createMetricCard(wrap, "Jumlah Data Pengajuan Anda", d.jumlahSopBaru, "Pengajuan SOP yang Anda lakukan", "#dbeafe", "#1e40af", "+", "Detail Data Pengajuan Anda", createDataPengajuanAndaCriteriaProvider(), createDisposisiSopRowRenderer());
		createMetricCard(wrap, "Selesai", d.selesai, "Pengajuan selesai", "#fef3c7", "#92400e", "★", "Detail Pengajuan Selesai", createSelesaiCriteriaProvider());
		createMetricCard(wrap, "Menunggu Petugas", d.menungguAktor, "Proses sedang berjalan", "#ede9fe", "#5b21b6", "→", "Detail Proses Menunggu Petugas", createMenungguAktorCriteriaProvider());
		createMetricCard(wrap, "Lewat Batas Waktu", d.mendekatiDeadline, "Subset menunggu disposisi Anda yang sudah melewati batas waktu", "#fee2e2", "#991b1b", "⏱", "Detail Menunggu Disposisi Anda Berdasarkan Batas Waktu", createMendekatiDeadlineCriteriaProvider());
	}

	private void createMetricCard(Component parent, String title, int value, String desc, String bg, String color,
			String icon, String detailTitle, DetailCriteriaProvider provider) {
		createMetricCard(parent, title, value, desc, bg, color, icon, detailTitle, provider, createDisposisiAlurRowRenderer());
	}

	private void createMetricCard(Component parent, String title, int value, String desc, String bg, String color,
			String icon, final String detailTitle, final DetailCriteriaProvider provider, final DetailRowRenderer renderer) {
		org.zkoss.zul.Div card = new org.zkoss.zul.Div();
		card.setStyle("flex:1 1 150px; min-width:150px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:14px;"
				+ "box-shadow:0 10px 22px rgba(15,23,42,.06); box-sizing:border-box;");
		card.setParent(parent);

		org.zkoss.zul.Hbox top = new org.zkoss.zul.Hbox();
		top.setWidth("100%");
		top.setPack("justify");
		top.setAlign("center");
		top.setParent(card);

		appendHtml(top, "<div style='width:38px; height:38px; border-radius:12px; display:flex; align-items:center; justify-content:center; font-weight:800; background:"
				+ bg + "; color:" + color + ";'>" + escapeHtml(icon) + "</div>");
		createDetailNumber(top, String.valueOf(value), detailTitle, provider, renderer, "font-size:26px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer;");

		appendHtml(card, "<div style='font-size:12px; color:#64748b; margin-top:10px;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:#94a3b8; margin-top:3px;'>" + escapeHtml(desc) + "</div>");
	}



	private void renderDasborSopGlobalFilter(final Component parent, Date mulai, Date sampai,
			final ais.database.model.rab.SatuanKerja satuanKerja, String keyword) throws Exception {
		final org.zkoss.zul.Div filterContainer = new org.zkoss.zul.Div();
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

		new MyLabelAgakKecil("Cari:").setParent(toolbar);
		final Textbox txtKeyword = new Textbox();
		txtKeyword.setCols(14);
		txtKeyword.setValue(keyword == null ? "" : keyword);
		txtKeyword.setTooltiptext("Cari nama SOP, properti, keyword, tahap, petugas, catatan, atau pengaju");
		txtKeyword.setParent(toolbar);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan Dasbor", "/img/svg/search.svg");
		refresh.setTooltiptext("Refresh dasbor SOP berdasarkan filter global");
		refresh.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; "
				+ "padding:6px 14px; margin-left:4px;");
		refresh.setParent(toolbar);

		MyToolbarbuttonConfig sinkronisasi = new MyToolbarbuttonConfig("Sinkronisasi", "/img/svg/refresh.svg");
		sinkronisasi.setTooltiptext("Ambil ulang data pengajuan SOP terbaru langsung dari database");
		sinkronisasi.setStyle("font-weight:bold; color:#ffffff; background:#059669; border-radius:10px; "
				+ "padding:6px 14px; margin-left:4px;");
		sinkronisasi.setParent(toolbar);

		EventListener refreshListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				renderHomeDasborContentAsync(DasboardSop.this, dbMulai.getValue(), dbSampai.getValue(),
						(ais.database.model.rab.SatuanKerja) cbSatker.getAttribute("satuanKerja"),
						txtKeyword.getValue());
			}
		};
		refresh.addEventListener("onClick", refreshListener);
		sinkronisasi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Date mulaiAktif = dbMulai.getValue();
				Date sampaiAktif = dbSampai.getValue();
				ais.database.model.rab.SatuanKerja satkerAktif =
						(ais.database.model.rab.SatuanKerja) cbSatker.getAttribute("satuanKerja");
				String keywordAktif = txtKeyword.getValue();
				invalidateSopDashboardCache(mulaiAktif, sampaiAktif, satkerAktif, keywordAktif);
				renderHomeDasborContentAsync(DasboardSop.this, mulaiAktif, sampaiAktif,
						satkerAktif, keywordAktif);
			}
		});
		txtKeyword.addEventListener("onOK", refreshListener);
		dbMulai.addEventListener("onChange", refreshListener);
		dbSampai.addEventListener("onChange", refreshListener);
	}

	private void renderExistingDashboardOperasional(Component parent) throws Exception {
		appendHtml(parent, "<div style='margin-top:14px; margin-bottom:8px; padding:12px 14px; border-radius:16px; "
				+ "background:#ffffff; border:1px solid #e5e7eb; box-shadow:0 10px 24px rgba(15,23,42,.04);'>"
				+ "<div style='font-size:14px; font-weight:800; color:#0f172a;'>Daftar Operasional SOP</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:4px;'>Daftar operasional SOP ditampilkan tepat di bawah ringkasan utama. Filter tanggal, pilihan SOP, dan pencarian pada daftar ini berdiri sendiri, sehingga pengguna dapat mencari data khusus tanpa mengubah ringkasan dasbor di atasnya.</div>"
				+ "</div>");

		MyPortallayout existingLayout = new MyPortallayout();
		existingLayout.setWidth("100%");
		existingLayout.setMaximizedMode("whole");
		existingLayout.setStyle("margin-top:8px; padding:0; background:transparent;");
		existingLayout.setParent(parent);

		MyPortallayout previousLayout = dashboardExistingLayout;
		boolean previousIgnoreGlobalFilter = ignoreGlobalFilterForLegacyPanel;
		dashboardExistingLayout = existingLayout;
		ignoreGlobalFilterForLegacyPanel = true;
		try {
			renderLegacyDashboardPanels();
		} catch (Exception e) {
			printDebug(e);
			appendHtml(parent, "<div style='padding:14px; margin-top:10px; border-radius:14px; background:#fff7ed; "
					+ "border:1px solid #fed7aa; color:#9a3412; font-size:12px; font-weight:700;'>"
					+ "Daftar operasional SOP belum dapat dimuat. Aktifkan debug/debuh=true untuk melihat detail error di console.</div>");
		} finally {
			ignoreGlobalFilterForLegacyPanel = previousIgnoreGlobalFilter;
			dashboardExistingLayout = previousLayout;
		}
	}

	private void renderLegacyDashboardPanels() throws Exception {
		tbmuser = Common.getCurrentUser();
		boolean urutanSopAktif = Common.bolehKonfigurasi("konfigurasi_urutan_sop", Konfigurasi.TIDAK_AKTIF);

		if (urutanSopAktif) {
			if (getCountMenunggu() > 0)
				buildMenungguDisposisi();
			if (getCountDisposisi() > 0)
				buildPengajuanBaruDisposisi();
			if (getCountPengajuanBaru() > 0)
				buildPengajuanBaru();
		} else {
			if (getCountPengajuanBaru() > 0)
				buildPengajuanBaru();
			if (getCountDisposisi() > 0)
				buildPengajuanBaruDisposisi();
			if (getCountMenunggu() > 0)
				buildMenungguDisposisi();
		}

		if (getCountSelesai() > 0)
			buildPengajuanSelesai();
		if (getCountMenungguAktor() > 0)
			buildMenungguAktorDisposisi();

		if (getCountMendekatiDeadline() > 0)
			buildMendekatiDedlineDisposisi();
	}

	private MyPortallayout getCurrentDashboardPortalParent() {
		return dashboardExistingLayout == null ? this : dashboardExistingLayout;
	}





	private void renderDasborSopTemplateTambahanV7(final org.zkoss.zul.Div body, final SopDashboardData data) {
		try {
			MyPortallayout analyticLayout = new MyPortallayout();
			analyticLayout.setParent(body);
			analyticLayout.setWidth("100%");
			analyticLayout.setMaximizedMode("whole");
			analyticLayout.setStyle("margin-top:10px; padding:0; background:transparent;");

			String pcWidth = Common.isMobile() ? "100%" : "50%";

			MyPortalchildren pcTop = new MyPortalchildren();
			pcTop.setWidth("100%");
			pcTop.setStyle("padding:6px; box-sizing:border-box;");
			pcTop.setParent(analyticLayout);

			MyPortalchildren pcLeft = new MyPortalchildren();
			pcLeft.setWidth(pcWidth);
			pcLeft.setStyle("padding:6px; box-sizing:border-box;");
			pcLeft.setParent(analyticLayout);

			MyPortalchildren pcRight = new MyPortalchildren();
			pcRight.setWidth(pcWidth);
			pcRight.setStyle("padding:6px; box-sizing:border-box;");
			pcRight.setParent(analyticLayout);

			MyPortalchildren pcBottom = new MyPortalchildren();
			pcBottom.setWidth("100%");
			pcBottom.setStyle("padding:6px; box-sizing:border-box;");
			pcBottom.setParent(analyticLayout);

			renderFunnelWorkflowSopV7(pcTop, data);
			renderSlaRiskWorkflowSopV7(pcLeft, data);
			renderMetadataQualitySopV7(pcRight, data);
			renderBebanSopDanTahapV7(pcLeft, data);
			renderAktivitasPrioritasSopV7(pcRight, data);
			renderSopGovernanceHealthV7(pcBottom, data);
			renderQueuePressureSopV7(pcBottom, data);
			renderExecutionPlanWorkflowSopV7(pcBottom, data);

			// Dashboard lanjutan SOP/Workflow diletakkan setelah rekomendasi tindak lanjut.
			renderWorkflowAgingAndBacklogV7(pcBottom, data);
			renderWorkflowStageControlV7(pcBottom, data);
			renderWorkflowGovernanceMatrixV7(pcBottom, data);
			renderWorkflowPemeriksaanReadinessV7(pcBottom, data);
			renderPembedaAntrianDanDeadlineV13(pcBottom, data);
			renderSopRadarChartV13(pcBottom, data);
			renderSopTrendBulananV13(pcBottom, data);
		} catch (Exception e) {
			printDebug(e);
			appendHtml(body, "<div style='padding:14px; margin-top:12px; border-radius:14px; background:#fff7ed; "
					+ "border:1px solid #fed7aa; color:#9a3412; font-size:12px; font-weight:700;'>"
					+ "Dasbor analisis tambahan belum dapat dimuat. Aktifkan <b>debug = true</b> atau <b>debuh = true</b> untuk melihat detail error.</div>");
		}
	}

	private Panelchildren createModernPanelV7(String title, org.zkoss.zk.ui.Component parent) {
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
		return pch;
	}

	private void appendPanelExplanationV13(Component parent, String tujuan, String sumberData, String caraBaca,
			String tindakLanjut) {
		StringBuilder penjelasan = new StringBuilder();
		appendSimpleSentenceV17(penjelasan, tujuan);
		appendSimpleSentenceV17(penjelasan, caraBaca);
		appendSimpleSentenceV17(penjelasan, tindakLanjut);

		if (penjelasan.length() == 0) {
			appendSimpleSentenceV17(penjelasan, sumberData);
		}

		appendHtml(parent, "<div style='margin-bottom:12px; padding:12px 14px; border-radius:14px; "
				+ "background:#f8fafc; border:1px solid #e2e8f0; color:#334155; line-height:1.55; "
				+ "font-size:12px;'>" + escapeHtml(penjelasan.toString()) + "</div>");
	}

	private void appendSimpleSentenceV17(StringBuilder builder, String text) {
		if (builder == null || text == null) {
			return;
		}
		String value = text.trim();
		if (value.length() == 0) {
			return;
		}
		if (builder.length() > 0) {
			builder.append(" ");
		}
		builder.append(value);
	}

	private void appendLegacyPanelExplanationV13(Component parent, String title) {
		String tujuan = "Menampilkan daftar pekerjaan SOP sesuai jenisnya, misalnya pengajuan yang masih berjalan, sudah diproses, sudah selesai, atau menunggu tindakan Anda.";
		String sumber = "Data pengajuan SOP yang masih aktif. Daftar ini mengikuti pencarian yang tersedia di masing-masing panel, seperti tanggal, pilihan SOP, dan kata kunci.";
		String cara = "Gunakan kotak pencarian, tanggal, dan pilihan SOP pada bagian atas panel untuk mempersempit daftar. Ini membantu pengguna menemukan data tertentu tanpa harus melihat semua data.";
		String tindak = "Klik salah satu baris untuk membuka detail proses SOP. Gunakan tombol refresh jika ingin memperbarui daftar setelah ada perubahan data.";

		if (title != null && title.toLowerCase().indexOf("sedang diproses") >= 0) {
			tujuan = "Menampilkan pengajuan SOP yang Anda ajukan sendiri dan saat ini masih berjalan. Bagian ini berguna untuk memantau sampai mana pengajuan Anda diproses.";
			cara = "Data ini bukan daftar tugas yang harus Anda disposisi. Data ini adalah daftar pengajuan milik Anda yang sedang menunggu proses dari tahap atau petugas lain.";
			tindak = "Gunakan bagian ini untuk mengecek posisi terakhir pengajuan, siapa/tahap mana yang sedang memproses, dan apakah ada catatan dari petugas sebelumnya.";
		} else if (title != null && title.toLowerCase().indexOf("baru saja anda disposisi") >= 0) {
			tujuan = "Menampilkan daftar SOP yang sudah Anda tindak lanjuti atau disposisikan. Bagian ini berguna untuk melihat pekerjaan yang sudah Anda proses.";
			cara = "Data ini menunjukkan riwayat tindakan Anda, sehingga dapat digunakan untuk melihat bukti bahwa suatu proses sudah pernah Anda tangani.";
			tindak = "Gunakan bagian ini untuk melihat catatan yang pernah diberikan, keputusan terakhir, dan ke mana proses berlanjut setelah Anda disposisikan.";
		} else if (title != null && title.toLowerCase().indexOf("telah selesai") >= 0) {
			tujuan = "Menampilkan pengajuan SOP milik Anda yang sudah selesai atau sudah sampai tahap akhir.";
			cara = "Data ini bukan pekerjaan yang perlu diproses lagi. Data ini lebih berfungsi sebagai arsip penyelesaian.";
			tindak = "Gunakan untuk memeriksa hasil akhir, membuat laporan penyelesaian, atau menelusuri dokumen yang sudah selesai.";
		} else if (title != null && title.toLowerCase().indexOf("menunggu disposisi anda") >= 0) {
			tujuan = "Menampilkan semua proses SOP aktif yang saat ini menunggu tindakan Anda.";
			cara = "Berisi seluruh antrian Anda. Di dalamnya bisa ada proses yang masih aman, belum memiliki batas waktu, maupun sudah melewati batas waktu.";
			tindak = "Utamakan proses yang batas waktunya paling mendesak, lalu lanjutkan ke proses lainnya secara berurutan.";
		} else if (title != null && title.toLowerCase().indexOf("menunggu disposisi") >= 0) {
			tujuan = "Menampilkan proses SOP yang sedang berada di tahap atau petugas berikutnya.";
			cara = "Membantu melihat proses yang masih berjalan di pihak lain, sehingga Anda tetap dapat memantau walaupun bukan Anda yang sedang memproses.";
			tindak = "Gunakan untuk koordinasi dengan unit atau petugas terkait jika proses terlihat terlalu lama berhenti di satu tahap.";
		} else if (title != null && title.toLowerCase().indexOf("batas waktu") >= 0) {
			tujuan = "Menampilkan proses yang harus diprioritaskan karena sudah memiliki batas waktu dan batas waktu tersebut sudah terlewati.";
			cara = "Hanya bagian dari daftar antrian Anda, yaitu yang sudah terlambat. Jadi jumlahnya bisa lebih kecil dari semua antrian disposisi Anda.";
			tindak = "Buka dan selesaikan data di bagian ini terlebih dahulu, atau lakukan koordinasi jika perlu bantuan pihak lain.";
		}
		appendPanelExplanationV13(parent, tujuan, sumber, cara, tindak);
	}

	private void renderFunnelWorkflowSopV7(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Ringkasan Alur Proses SOP", parent);
		appendPanelExplanationV13(pch, "Menunjukkan perjalanan proses SOP dari awal pengajuan sampai selesai. Membantu pengguna melihat apakah pekerjaan masih banyak di tahap awal, menunggu tindakan, atau sudah selesai.", "Data pengajuan SOP dan tahapan proses yang masih aktif, sesuai hak akses pengguna dan filter tanggal/pencarian pada dasbor.", "Jika angka pada bagian menunggu besar, berarti masih banyak pekerjaan yang perlu dipantau atau ditindaklanjuti. Jika angka selesai besar, berarti banyak proses sudah berhasil diselesaikan.", "Klik angka untuk melihat daftar datanya. Dahulukan proses yang jumlah antriannya paling besar atau batas waktunya paling dekat.");
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px; line-height:1.55;'>"
				+ "Memperlihatkan posisi proses SOP dari awal pengajuan, menunggu disposisi, sedang berjalan, sampai selesai. "
				+ "Semua angka dapat diklik untuk melihat daftar datanya.</div>");
		int max = getMaxValue(new int[] { d.jumlahSopBaru, d.menungguSaya, d.menungguAktor, d.sudahSayaDisposisi, d.selesai });
		renderFunnelRow(pch, "Jumlah Data Pengajuan Anda", d.jumlahSopBaru, max, "#2563eb", "Detail Data Pengajuan Anda", createDataPengajuanAndaCriteriaProvider(), createDisposisiSopRowRenderer());
		renderFunnelRow(pch, "Menunggu Disposisi Anda", d.menungguSaya, max, "#f59e0b", "Detail Menunggu Disposisi Saya", createMenungguSayaCriteriaProvider(), createDisposisiAlurRowRenderer());
		renderFunnelRow(pch, "Menunggu Petugas / Tahap", d.menungguAktor, max, "#7c3aed", "Detail Proses Menunggu Petugas", createMenungguAktorCriteriaProvider(), createDisposisiAlurRowRenderer());
		renderFunnelRow(pch, "Sudah Anda Disposisi", d.sudahSayaDisposisi, max, "#16a34a", "Detail Pengajuan yang Sudah Saya Disposisi", createSudahDisposisiCriteriaProvider(), createDisposisiAlurRowRenderer());
		renderFunnelRow(pch, "Selesai", d.selesai, max, "#0891b2", "Detail Pengajuan Selesai", createSelesaiCriteriaProvider(), createDisposisiAlurRowRenderer());
	}

	private void renderSlaRiskWorkflowSopV7(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Analisis Risiko Keterlambatan SOP", parent);
		appendPanelExplanationV13(pch, "Membantu menilai apakah proses SOP berjalan lancar atau berisiko terlambat. Membandingkan jumlah proses selesai, antrian yang belum diproses, dan batas waktu yang sudah dekat atau terlewati.", "Ringkasan data SOP aktif, batas waktu proses, dan status apakah proses sudah selesai atau masih berjalan.", "Angka penyelesaian yang tinggi menandakan proses berjalan baik. Angka risiko antrian atau risiko batas waktu yang tinggi menandakan perlu perhatian lebih cepat.", "Gunakan bagian ini untuk menentukan apakah perlu mengingatkan petugas, meninjau ulang batas waktu pelayanan, atau membagi pekerjaan ke petugas lain.");
		int totalAlur = Math.max(1, d.totalAktivitas);
		int backlog = d.totalAntrian;
		int selesai = d.selesai;
		int deadlineKritis = d.deadlineLewat + d.deadlineHariIni;
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "Menilai tekanan alur kerja dari besarnya antrian, batas waktu kritis, dan sinyal penyelesaian. "
				+ "Semakin tinggi tekanan antrian, semakin perlu dilakukan pengingat atau koordinasi kepada petugas/tahap terkait.</div>");
		renderMiniGaugeV7(pch, "Tingkat Selesai", percentV7(selesai, totalAlur), "Proporsi proses selesai dibanding total aktivitas dasbor.", "#16a34a", "Detail Pengajuan Selesai", createSelesaiCriteriaProvider());
		renderMiniGaugeV7(pch, "Tekanan Antrian", percentV7(backlog, totalAlur), "Rasio item yang masih berada di antrian aktif.", "#f59e0b", "Detail Total Antrian", createTotalAntrianCriteriaProvider());
		renderMiniGaugeV7(pch, "Risiko Batas Waktu", percentV7(deadlineKritis, Math.max(1, d.totalDipantau)), "Proporsi proses yang lewat batas waktu atau jatuh tempo hari ini.", "#dc2626", "Detail Batas Waktu Kritis", createDeadlineUrgentCriteriaProvider());
		renderMiniGaugeV7(pch, "Volume Pengajuan", percentV7(d.jumlahSopBaru, Math.max(1, d.jumlahSopBaru + backlog)), "Indikasi volume pengajuan Anda dibanding beban antrian.", "#2563eb", "Detail Data Pengajuan Anda", createDataPengajuanAndaCriteriaProvider(), createDisposisiSopRowRenderer());
	}

	private void renderMetadataQualitySopV7(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Kelengkapan Informasi Proses SOP", parent);
		appendPanelExplanationV13(pch, "Menilai apakah informasi penting pada proses SOP sudah lengkap. Informasi yang lengkap akan memudahkan pencarian, pemeriksaan, dan pelaporan.", "Batas waktu, petugas/tahap, nama tahap, dan catatan pada setiap proses SOP.", "Semakin banyak data yang kosong, semakin sulit proses dipantau dan dipertanggungjawabkan.", "Lengkapi data SOP dan biasakan pengisian catatan pada setiap tahap agar bukti proses lebih jelas.");
		int issue = d.tanpaDeadline + d.tanpaAktor + d.tanpaTahap + d.tanpaCatatan;
		int peluang = Math.max(1, d.totalDipantau * 4);
		int score = Math.max(0, 100 - percentV7(issue, peluang));
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "Informasi proses yang lengkap membantu pemantauan batas waktu, pemeriksaan riwayat, pencarian data, dan analisis titik macet.</div>");
		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		wrap.setParent(pch);
		createPressureCardV7(wrap, "Skor Kelengkapan Data", score, "Estimasi kepatuhan kelengkapan data (%)", "#ecfdf5", "#166534", "Detail Proses Dipantau", createAllDashboardCriteriaProvider());
		createPressureCardV7(wrap, "Tanpa Batas Waktu", d.tanpaDeadline, "Perlu batas waktu proses", "#dbeafe", "#1e40af", "Detail Proses Tanpa Batas Waktu", createNoDeadlineCriteriaProvider());
		createPressureCardV7(wrap, "Tanpa Petugas", d.tanpaAktor, "Perlu penentuan petugas", "#fef3c7", "#92400e", "Detail Proses Tanpa Petugas", createNoAktorCriteriaProvider());
		createPressureCardV7(wrap, "Tanpa Catatan", d.tanpaCatatan, "Perlu dokumentasi catatan", "#fee2e2", "#991b1b", "Detail Proses Tanpa Catatan", createNoCatatanCriteriaProvider());
	}

	private void renderBebanSopDanTahapV7(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Beban SOP dan Tahap Proses", parent);
		appendPanelExplanationV13(pch, "Menampilkan jenis SOP dan tahap/petugas yang paling sering muncul. Ini membantu mengetahui bagian mana yang paling sibuk atau berpotensi menjadi titik macet.", "Jumlah kemunculan tiap nama SOP dan tiap tahap/petugas pada data yang sedang difilter.", "Bar paling panjang menunjukkan jenis SOP atau tahap yang paling banyak jumlahnya.", "Tinjau SOP atau tahap yang paling padat. Pertimbangkan penyederhanaan proses, penunjukan pengganti, atau pengingat otomatis.");
		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		wrap.setParent(pch);
		org.zkoss.zul.Div left = new org.zkoss.zul.Div();
		left.setStyle("flex:1 1 300px;");
		left.setParent(wrap);
		appendHtml(left, "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>SOP paling dominan</div>");
		renderMiniRowCounterV7(left, d.perSop, "Belum ada data SOP dominan.", 7, "#2563eb", "sop");
		org.zkoss.zul.Div right = new org.zkoss.zul.Div();
		right.setStyle("flex:1 1 300px;");
		right.setParent(wrap);
		appendHtml(right, "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Tahap / petugas paling padat</div>");
		renderMiniRowCounterV7(right, d.perAktor, "Belum ada data petugas/tahap dominan.", 7, "#7c3aed", "petugas");
	}

	private void renderAktivitasPrioritasSopV7(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Daftar Perhatian dan Prioritas SOP", parent);
		appendPanelExplanationV13(pch, "Memberikan daftar singkat pekerjaan yang perlu diperhatikan dan menampilkan aktivitas terakhir sebagai riwayat proses.", "Jumlah antrian, data yang sudah kritis karena batas waktu, dan aktivitas proses terbaru.", "Jika angka menunggu Anda atau batas waktu kritis tinggi, berarti daftar kerja perlu segera diperiksa.", "Klik angka yang kritis, lalu dahulukan proses yang sudah lewat batas waktu atau jatuh tempo hari ini.");
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "Watchlist membantu admin melihat pekerjaan yang paling perlu diproses tanpa membuka satu per satu menu transaksi SOP.</div>");
		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		wrap.setParent(pch);
		org.zkoss.zul.Div left = new org.zkoss.zul.Div();
		left.setStyle("flex:1 1 300px;");
		left.setParent(wrap);
		createWorkflowMiniCard(left, "Menunggu Saya", d.menungguSaya, "Tugas yang perlu segera Anda disposisi.", "Detail Menunggu Disposisi Saya", createMenungguSayaCriteriaProvider());
		createWorkflowMiniCard(left, "Batas Waktu Kritis", d.deadlineLewat + d.deadlineHariIni, "Lewat batas waktu dan jatuh tempo hari ini.", "Detail Batas Waktu Kritis", createDeadlineUrgentCriteriaProvider());
		org.zkoss.zul.Div right = new org.zkoss.zul.Div();
		right.setStyle("flex:1 1 300px;");
		right.setParent(wrap);
		renderRecentItemsV7(right, d);
	}

	private void renderSopGovernanceHealthV7(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Indeks Kondisi Alur SOP", parent);
		appendPanelExplanationV13(pch, "Merangkum kondisi proses SOP dalam satu skor sederhana. Skor ini mempertimbangkan penyelesaian, antrian, risiko batas waktu, dan kelengkapan data.", "Gabungan jumlah aktivitas, jumlah antrian, batas waktu, kelengkapan data, dan jumlah proses selesai.", "Skor tinggi berarti proses relatif terkendali. Skor rendah berarti ada antrian, keterlambatan, atau data kosong yang perlu diperbaiki.", "Gunakan skor ini sebagai bahan evaluasi rutin dan untuk menentukan bagian mana yang harus diperbaiki lebih dulu.");
		int totalAlur = Math.max(1, d.totalAktivitas);
		int completion = percentV7(d.selesai, totalAlur);
		int queue = percentV7(d.totalAntrian, totalAlur);
		int metadataRisk = percentV7(d.tanpaDeadline + d.tanpaAktor + d.tanpaTahap + d.tanpaCatatan, Math.max(1, d.totalDipantau * 4));
		int deadlineRisk = percentV7(d.deadlineLewat + d.deadlineHariIni, Math.max(1, d.totalDipantau));
		int health = 100 - ((queue * 35) / 100) - ((metadataRisk * 20) / 100) - ((deadlineRisk * 25) / 100) + ((completion * 20) / 100);
		if (health < 0) {
			health = 0;
		}
		if (health > 100) {
			health = 100;
		}

		String status = health >= 80 ? "Sehat" : (health >= 60 ? "Perlu Dipantau" : "Prioritas Perbaikan");
		String statusBg = health >= 80 ? "#dcfce7" : (health >= 60 ? "#fef3c7" : "#fee2e2");
		String statusColor = health >= 80 ? "#166534" : (health >= 60 ? "#92400e" : "#991b1b");

		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; gap:14px; flex-wrap:wrap; align-items:stretch;");
		wrap.setParent(pch);

		org.zkoss.zul.Div score = new org.zkoss.zul.Div();
		score.setStyle("flex:1 1 230px; border-radius:16px; padding:16px; color:#ffffff; background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); box-shadow:0 12px 24px rgba(37,99,235,.18);");
		score.setParent(wrap);
		appendHtml(score, "<div style='font-size:11px; letter-spacing:.08em; text-transform:uppercase; opacity:.82;'>Skor Kondisi Alur</div>"
				+ "<div style='font-size:46px; line-height:1; font-weight:900; margin-top:10px;'>" + health + "</div>"
				+ "<div style='display:inline-block; margin-top:12px; border-radius:999px; background:" + statusBg + "; color:" + statusColor + "; padding:5px 10px; font-size:11px; font-weight:800;'>" + escapeHtml(status) + "</div>");

		org.zkoss.zul.Div gauges = new org.zkoss.zul.Div();
		gauges.setStyle("flex:2 1 420px;");
		gauges.setParent(wrap);
		renderMiniGaugeV7(gauges, "Tingkat Selesai", completion, "Alur yang selesai dibanding total aktivitas.", "#16a34a", "Detail Pengajuan Selesai", createSelesaiCriteriaProvider());
		renderMiniGaugeV7(gauges, "Tekanan Antrian", queue, "Antrian aktif yang masih menunggu.", "#f59e0b", "Detail Total Antrian", createTotalAntrianCriteriaProvider());
		renderMiniGaugeV7(gauges, "Risiko Batas Waktu", deadlineRisk, "Risiko batas waktu lewat/hari ini.", "#dc2626", "Detail Batas Waktu Kritis", createDeadlineUrgentCriteriaProvider());
		renderMiniGaugeV7(gauges, "Risiko Data Kosong", metadataRisk, "Risiko adanya informasi penting yang masih kosong pada proses.", "#7c3aed", "Detail Proses Tanpa Batas Waktu", createNoDeadlineCriteriaProvider());
	}

	private void renderQueuePressureSopV7(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Peta Penumpukan Antrian SOP", parent);
		appendPanelExplanationV13(pch, "Membedakan pekerjaan yang harus Anda proses sendiri, pekerjaan yang sedang berada di petugas lain, dan pekerjaan yang sudah mendesak karena batas waktu.", "Jumlah proses yang menunggu Anda, menunggu petugas/tahap lain, batas waktu kritis, dan total proses yang dipantau.", "Jika menunggu Anda tinggi, maka tindakan utama ada di akun Anda. Jika menunggu petugas tinggi, hambatan kemungkinan ada pada petugas atau tahap lain.", "Gunakan bagian ini untuk menentukan apakah cukup diproses sendiri atau perlu koordinasi dengan unit lain.");
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "Membantu melihat titik tekanan: apakah lebih berat pada disposisi Anda, antrian tahap, atau batas waktu kritis.</div>");
		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		wrap.setParent(pch);
		createPressureCardV7(wrap, "Menunggu Saya", d.menungguSaya, "Antrian disposisi langsung", "#fef3c7", "#92400e", "Detail Menunggu Disposisi Saya", createMenungguSayaCriteriaProvider());
		createPressureCardV7(wrap, "Menunggu Petugas", d.menungguAktor, "Antrian tahap/petugas berjalan", "#ede9fe", "#5b21b6", "Detail Proses Menunggu Petugas", createMenungguAktorCriteriaProvider());
		createPressureCardV7(wrap, "Batas Waktu Kritis", d.deadlineLewat + d.deadlineHariIni, "Perlu koordinasi prioritas segera", "#fee2e2", "#991b1b", "Detail Batas Waktu Kritis", createDeadlineUrgentCriteriaProvider());
		createPressureCardV7(wrap, "Proses Dipantau", d.totalDipantau, "Sampel proses analisis", "#dbeafe", "#1e40af", "Detail Proses Dipantau", createAllDashboardCriteriaProvider());
	}

	private void renderExecutionPlanWorkflowSopV7(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Rencana Tindak Lanjut SOP", parent);
		appendPanelExplanationV13(pch, "Mengubah hasil dasbor menjadi saran tindakan yang mudah dilakukan.", "Jumlah antrian, data yang belum lengkap, masalah batas waktu, serta SOP/tahap yang paling banyak muncul.", "Setiap kartu berisi saran langkah kerja berdasarkan kondisi data saat ini.", "Gunakan sebagai daftar pemeriksaan harian atau mingguan untuk admin SOP dan pimpinan unit.");
		int backlog = d.totalAntrian;
		int metadataIssue = d.tanpaDeadline + d.tanpaAktor + d.tanpaTahap + d.tanpaCatatan;
		CounterItem topSop = getFirstCounterItem(d.perSop);
		CounterItem topAktor = getFirstCounterItem(d.perAktor);
		String prioritas1 = backlog > 0 ? "Tuntaskan " + backlog + " item yang masih menunggu disposisi/tahap alur kerja."
				: "Tidak ada pekerjaan tertunda utama pada filter saat ini.";
		String prioritas2 = metadataIssue > 0 ? "Lengkapi " + metadataIssue + " informasi proses yang masih kosong agar data lebih siap diperiksa."
				: "Kelengkapan Data utama relatif aman pada filter saat ini.";
		String prioritas3 = (d.deadlineLewat + d.deadlineHariIni) > 0
				? "Koordinasi Prioritas batas waktu kritis agar tidak menjadi titik macet lintas unit."
				: "Belum ada sinyal batas waktu kritis dominan.";
		String prioritas4 = (topAktor == null ? "Belum ada tahap dominan" : topAktor.label)
				+ " perlu dipantau sebagai tahap/petugas dengan volume tertinggi."
				+ (topSop == null ? "" : " SOP padat: " + topSop.label + ".");

		String html = "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(230px,1fr)); gap:12px;'>"
				+ buildActionPlanCardV7("1", "Kontrol Antrian", prioritas1, "#fee2e2", "#991b1b")
				+ buildActionPlanCardV7("2", "Kelengkapan Kelengkapan Data", prioritas2, "#dbeafe", "#1e40af")
				+ buildActionPlanCardV7("3", "Kontrol Batas Waktu", prioritas3, "#fef3c7", "#92400e")
				+ buildActionPlanCardV7("4", "Pemantauan Tahap", prioritas4, "#ede9fe", "#5b21b6")
				+ "</div>";
		appendHtml(pch, html);
	}


	private void renderWorkflowAgingAndBacklogV7(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Umur Antrian dan Pekerjaan Tertunda SOP", parent);
		appendPanelExplanationV13(pch, "Membantu melihat tingkat keterlambatan dari sisi waktu: sudah lewat batas waktu, jatuh tempo hari ini, akan jatuh tempo dalam tujuh hari, dan total antrian.", "Batas waktu pada setiap tahap SOP dan status proses yang masih aktif.", "Proses yang sudah lewat batas waktu harus diprioritaskan paling dulu. Setelah itu proses yang jatuh tempo hari ini, kemudian yang jatuh tempo dalam minggu ini.", "Susun urutan kerja berdasarkan tingkat kedaruratan waktu agar pelayanan SOP lebih terkendali.");
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "Dasbor ini membantu membaca umur antrian secara praktis dari sinyal batas waktu dan jumlah pekerjaan aktif. "
				+ "Gunakan bagian ini untuk memutuskan mana yang perlu ditindaklanjuti hari ini, minggu ini, atau cukup dipantau.</div>");

		org.zkoss.zul.Hbox wrap = new org.zkoss.zul.Hbox();
		wrap.setWidth("100%");
		wrap.setStyle("gap:12px; flex-wrap:wrap;");
		wrap.setParent(pch);

		createPressureCardV7(wrap, "Lewat Batas Waktu", d.deadlineLewat, "Sudah melewati batas waktu", "#fee2e2", "#991b1b", "Detail Lewat Batas Waktu", createDeadlineCriteriaProvider("lewat"));
		createPressureCardV7(wrap, "Jatuh Tempo Hari Ini", d.deadlineHariIni, "Perlu diprioritaskan hari ini", "#ffedd5", "#9a3412", "Detail Batas Waktu Hari Ini", createDeadlineCriteriaProvider("hari_ini"));
		createPressureCardV7(wrap, "7 Hari ke Depan", d.deadlineMingguIni, "Perlu pemantauan mingguan", "#fef3c7", "#92400e", "Detail Batas Waktu 7 Hari ke Depan", createDeadlineCriteriaProvider("minggu_ini"));
		createPressureCardV7(wrap, "Antrian Aktif", d.totalAntrian, "Menunggu disposisi/tahap", "#dbeafe", "#1e40af", "Detail Total Antrian", createTotalAntrianCriteriaProvider());

		int critical = d.deadlineLewat + d.deadlineHariIni;
		String note = critical > 0
				? "Ada " + critical + " proses yang masuk kategori kritis. Prioritaskan komunikasi dengan petugas/tahap terkait."
				: "Belum ada batas waktu kritis pada filter saat ini. Fokuskan pemantauan pada antrian aktif dan batas waktu minggu ini.";
		appendHtml(pch, "<div style='margin-top:12px; padding:12px 14px; border-radius:14px; background:#f8fafc; border:1px solid #e2e8f0; color:#475569; font-size:12px; line-height:1.5;'>"
				+ escapeHtml(note) + "</div>");
	}

	private void renderWorkflowStageControlV7(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Kontrol Tahap yang Paling Padat", parent);
		appendPanelExplanationV13(pch, "Menunjukkan SOP dan tahap yang paling banyak jumlahnya agar perbaikan difokuskan pada bagian yang paling berpengaruh.", "Perhitungan jumlah berdasarkan nama SOP dan nama tahap/petugas.", "SOP atau tahap di peringkat atas perlu dipantau karena paling banyak menyita pekerjaan.", "Tinjau kembali alur, pembagian petugas, dan batas waktu pada SOP atau tahap yang paling padat.");
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "Menyorot SOP dan petugas/tahap dengan volume paling besar. Ini berguna untuk menemukan titik macet operasional tanpa membuka transaksi satu per satu.</div>");

		org.zkoss.zul.Hbox wrap = new org.zkoss.zul.Hbox();
		wrap.setWidth("100%");
		wrap.setStyle("gap:14px; flex-wrap:wrap;");
		wrap.setParent(pch);

		org.zkoss.zul.Div left = new org.zkoss.zul.Div();
		left.setStyle("flex:1 1 320px; min-width:280px;");
		left.setParent(wrap);
		appendHtml(left, "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>SOP dengan volume tertinggi</div>");
		renderMiniRowCounterV7(left, d.perSop, "Belum ada SOP dominan pada filter saat ini.", 10, "#2563eb", "sop");

		org.zkoss.zul.Div right = new org.zkoss.zul.Div();
		right.setStyle("flex:1 1 320px; min-width:280px;");
		right.setParent(wrap);
		appendHtml(right, "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Petugas / tahap dengan volume tertinggi</div>");
		renderMiniRowCounterV7(right, d.perAktor, "Belum ada tahap dominan pada filter saat ini.", 10, "#7c3aed", "petugas");
	}

	private void renderWorkflowGovernanceMatrixV7(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Matriks Tata Kelola SOP", parent);
		appendPanelExplanationV13(pch, "Menampilkan empat indikator utama: penyelesaian, jumlah antrian, risiko batas waktu, dan kelengkapan data.", "Ringkasan aktivitas, antrian, batas waktu, dan kelengkapan informasi pada dasbor SOP.", "Indikator risiko yang paling tinggi menunjukkan bagian yang paling perlu diperbaiki.", "Gunakan sebagai bahan rapat evaluasi untuk menentukan prioritas perbaikan SOP.");
		int total = Math.max(1, d.totalDipantau);
		int completion = percentV7(d.selesai, Math.max(1, d.totalAktivitas));
		int queue = percentV7(d.totalAntrian, Math.max(1, d.totalAktivitas));
		int deadlineRisk = percentV7(d.deadlineLewat + d.deadlineHariIni, total);
		int metadataRisk = percentV7(d.tanpaDeadline + d.tanpaAktor + d.tanpaTahap + d.tanpaCatatan, Math.max(1, total * 4));

		appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "Matriks ini menggabungkan penyelesaian, tekanan antrian, risiko batas waktu, dan kualitas kelengkapan data sebagai bahan evaluasi tata kelola SOP.</div>");

		renderMiniGaugeV7(pch, "Penyelesaian", completion, "Proporsi proses selesai dibanding total aktivitas.", "#16a34a", "Detail Pengajuan Selesai", createSelesaiCriteriaProvider());
		renderMiniGaugeV7(pch, "Tekanan Antrian", queue, "Proporsi pekerjaan yang masih berada di antrian aktif.", "#f59e0b", "Detail Total Antrian", createTotalAntrianCriteriaProvider());
		renderMiniGaugeV7(pch, "Risiko Batas Waktu", deadlineRisk, "Proporsi proses lewat batas waktu atau jatuh tempo hari ini.", "#dc2626", "Detail Batas Waktu Kritis", createDeadlineUrgentCriteriaProvider());
		renderMiniGaugeV7(pch, "Risiko Kelengkapan Data", metadataRisk, "Perbandingan jumlah informasi penting yang kosong terhadap keseluruhan data yang seharusnya terisi.", "#7c3aed", "Detail Proses Tanpa Batas Waktu", createNoDeadlineCriteriaProvider());
	}

	private void renderWorkflowPemeriksaanReadinessV7(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Kesiapan Pemeriksaan dan Dokumentasi SOP", parent);
		appendPanelExplanationV13(pch, "Menilai apakah data SOP sudah cukup lengkap untuk pemeriksaan internal atau pemeriksaan.", "Informasi proses seperti batas waktu, petugas, tahap, catatan, dan aktivitas terbaru.", "Semakin sedikit data kosong, semakin siap data SOP untuk diperiksa dan ditelusuri kembali.", "Lengkapi data kosong dan biasakan menulis catatan agar bukti proses lebih jelas.");
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "Menunjukkan kelengkapan data SOP untuk kebutuhan pemeriksaan: batas waktu, petugas, tahap, dan catatan proses. Angka dapat diklik untuk membuka daftar detail.</div>");

		org.zkoss.zul.Hbox wrap = new org.zkoss.zul.Hbox();
		wrap.setWidth("100%");
		wrap.setStyle("gap:12px; flex-wrap:wrap;");
		wrap.setParent(pch);

		createPressureCardV7(wrap, "Tanpa Batas Waktu", d.tanpaDeadline, "Belum punya batas waktu", "#dbeafe", "#1e40af", "Detail Proses Tanpa Batas Waktu", createNoDeadlineCriteriaProvider());
		createPressureCardV7(wrap, "Tanpa Petugas", d.tanpaAktor, "Belum jelas penanggung jawab", "#fef3c7", "#92400e", "Detail Proses Tanpa Petugas", createNoAktorCriteriaProvider());
		createPressureCardV7(wrap, "Tanpa Tahap", d.tanpaTahap, "Nama tahap belum lengkap", "#ede9fe", "#5b21b6", "Detail Proses Tanpa Tahap", createNoTahapCriteriaProvider());
		createPressureCardV7(wrap, "Tanpa Catatan", d.tanpaCatatan, "Dokumentasi proses minim", "#fee2e2", "#991b1b", "Detail Proses Tanpa Catatan", createNoCatatanCriteriaProvider());

		appendHtml(pch, "<div style='margin-top:14px; font-size:12px; font-weight:900; color:#0f172a;'>Aktivitas terakhir sebagai bahan riwayat proses</div>");
		renderRecentItemsV7(pch, d);
	}

	private void renderPembedaAntrianDanDeadlineV13(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Pembeda Antrian Disposisi dan Batas Waktu", parent);
		appendPanelExplanationV13(pch,
				"Menjelaskan perbedaan antara seluruh pekerjaan yang menunggu tindakan Anda dan pekerjaan yang sudah terlambat.",
				"Data pertama berisi semua antrian aktif milik Anda. Data kedua hanya mengambil antrian milik Anda yang sudah memiliki batas waktu dan batas waktunya sudah terlewati.",
				"Menunggu disposisi adalah daftar kerja lengkap. Menunggu berdasarkan batas waktu adalah bagian dari daftar tersebut yang harus didahulukan karena sudah melewati batas waktu.",
				"Jika angka batas waktu lebih dari nol, tangani data tersebut lebih dulu. Setelah itu lanjutkan ke antrian disposisi lainnya.");

		org.zkoss.zul.Hbox wrap = new org.zkoss.zul.Hbox();
		wrap.setWidth("100%");
		wrap.setStyle("gap:12px; flex-wrap:wrap; align-items:stretch;");
		wrap.setParent(pch);

		org.zkoss.zul.Div semua = new org.zkoss.zul.Div();
		semua.setStyle("flex:1 1 320px; min-width:280px; padding:14px; border-radius:16px; background:#eff6ff; border:1px solid #bfdbfe;");
		semua.setParent(wrap);
		createDetailNumber(semua, String.valueOf(d.menungguSaya), "Detail Menunggu Disposisi Saya", createMenungguSayaCriteriaProvider(), createDisposisiAlurRowRenderer(),
				"display:block; font-size:34px; line-height:1; font-weight:900; color:#1d4ed8; text-decoration:none; cursor:pointer;");
		appendHtml(semua, "<div style='font-size:13px; font-weight:900; color:#1e3a8a; margin-top:10px;'>Proses yang sedang menunggu disposisi Anda</div>"
				+ "<div style='font-size:12px; color:#1e40af; line-height:1.55; margin-top:7px;'>Ini adalah semua proses aktif yang saat ini berada pada kewenangan atau tahap Anda. Data ini tidak selalu terlambat; sebagian masih aman, sebagian belum memiliki batas waktu, dan sebagian mungkin sudah lewat batas waktu.</div>");

		org.zkoss.zul.Div deadline = new org.zkoss.zul.Div();
		deadline.setStyle("flex:1 1 320px; min-width:280px; padding:14px; border-radius:16px; background:#fef2f2; border:1px solid #fecaca;");
		deadline.setParent(wrap);
		createDetailNumber(deadline, String.valueOf(d.mendekatiDeadline), "Detail Menunggu Disposisi Anda Berdasarkan Batas Waktu", createMendekatiDeadlineCriteriaProvider(), createDisposisiAlurRowRenderer(),
				"display:block; font-size:34px; line-height:1; font-weight:900; color:#b91c1c; text-decoration:none; cursor:pointer;");
		appendHtml(deadline, "<div style='font-size:13px; font-weight:900; color:#7f1d1d; margin-top:10px;'>Menunggu disposisi Anda berdasarkan batas waktu</div>"
				+ "<div style='font-size:12px; color:#991b1b; line-height:1.55; margin-top:7px;'>Ini adalah bagian dari antrian Anda yang sudah memiliki batas waktu dan sudah melewati batas waktu. Ini menjadi daftar prioritas yang sebaiknya ditangani lebih dulu.</div>");
	}

	private void renderSopRadarChartV13(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Radar Kinerja SOP", parent);
		appendPanelExplanationV13(pch,
				"Melihat sekaligus kondisi proses SOP dari 5 sisi berbeda dalam satu gambar.",
				"Lima ukuran kinerja dari data SOP yang sedang difilter.",
				"Area berwarna biru menunjukkan kondisi saat ini. Semakin lebar bidang warnanya, semakin baik. Sudut yang mengerucut ke tengah berarti bagian itu perlu perhatian lebih.",
				"Fokus perbaikan pada dimensi dengan nilai terkecil, terutama yang skornya di bawah 50.");
		int[] vals = {
			percentV7(d.selesai, Math.max(1, d.totalAktivitas)),
			Math.max(0, 100 - percentV7(d.deadlineLewat + d.deadlineHariIni, Math.max(1, d.totalDipantau))),
			Math.max(0, 100 - percentV7(d.totalAntrian, Math.max(1, d.totalAktivitas))),
			Math.max(0, 100 - percentV7(d.tanpaCatatan, Math.max(1, d.totalDipantau))),
			Math.max(0, 100 - percentV7(d.tanpaDeadline + d.tanpaAktor + d.tanpaTahap + d.tanpaCatatan, Math.max(1, d.totalDipantau * 4)))
		};
		String[] lbls = { "Penyelesaian", "Ketepatan Waktu", "Kontrol Antrian", "Catatan Terisi", "Data Lengkap" };
		String[] clrs = { "#2563eb", "#16a34a", "#f59e0b", "#7c3aed", "#0891b2" };
		appendHtml(pch, buildSopRadarSvg(vals, lbls, clrs));
		StringBuilder legend = new StringBuilder();
		legend.append("<div style='display:flex;flex-wrap:wrap;gap:10px;margin-top:12px;padding:12px;border-radius:12px;background:#f8fafc;border:1px solid #e2e8f0;'>");
		for (int i = 0; i < vals.length; i++) {
			String sc = vals[i] >= 70 ? "#166534" : (vals[i] >= 40 ? "#92400e" : "#991b1b");
			legend.append("<div style='display:flex;align-items:center;gap:6px;min-width:165px;'>")
			      .append("<div style='width:10px;height:10px;border-radius:2px;flex-shrink:0;background:").append(clrs[i]).append(";'></div>")
			      .append("<div style='font-size:12px;color:#334155;'>").append(escapeHtml(lbls[i])).append(": <b style='color:").append(sc).append(";'>").append(vals[i]).append("%</b></div>")
			      .append("</div>");
		}
		legend.append("</div>");
		appendHtml(pch, legend.toString());
	}

	private String buildSopRadarSvg(int[] vals, String[] lbls, String[] clrs) {
		double cx = 250, cy = 210, R = 140;
		double[] ang = {
			Math.toRadians(-90),
			Math.toRadians(-18),
			Math.toRadians(54),
			Math.toRadians(126),
			Math.toRadians(198)
		};
		StringBuilder svg = new StringBuilder();
		svg.append("<svg viewBox='0 0 500 400' xmlns='http://www.w3.org/2000/svg' style='width:100%;max-width:480px;display:block;margin:0 auto;'>");
		// Grid rings 20-100%
		String[] ringStroke = { "#f1f5f9", "#e2e8f0", "#cbd5e1", "#94a3b8", "#e2e8f0" };
		for (int g = 4; g >= 0; g--) {
			double s = (g + 1) * 0.2;
			StringBuilder pts = new StringBuilder();
			for (int i = 0; i < 5; i++) {
				if (i > 0) pts.append(",");
				pts.append(svgFmt(cx + R * s * Math.cos(ang[i]))).append(",").append(svgFmt(cy + R * s * Math.sin(ang[i])));
			}
			svg.append("<polygon points='").append(pts).append("' fill='").append(g == 4 ? "#f8fafc" : "none").append("' stroke='").append(ringStroke[g]).append("' stroke-width='1'/>");
			svg.append("<text x='").append(svgFmt(cx + 4)).append("' y='").append(svgFmt(cy - R * s - 4))
			   .append("' font-size='9' fill='#94a3b8' text-anchor='start'>").append((g + 1) * 20).append("%</text>");
		}
		// Axis lines
		for (int i = 0; i < 5; i++) {
			svg.append("<line x1='").append(svgFmt(cx)).append("' y1='").append(svgFmt(cy))
			   .append("' x2='").append(svgFmt(cx + R * Math.cos(ang[i]))).append("' y2='").append(svgFmt(cy + R * Math.sin(ang[i])))
			   .append("' stroke='#e2e8f0' stroke-width='1'/>");
		}
		// Data polygon
		StringBuilder dp = new StringBuilder();
		for (int i = 0; i < 5; i++) {
			double s = vals[i] / 100.0;
			if (i > 0) dp.append(",");
			dp.append(svgFmt(cx + R * s * Math.cos(ang[i]))).append(",").append(svgFmt(cy + R * s * Math.sin(ang[i])));
		}
		svg.append("<polygon points='").append(dp).append("' fill='rgba(37,99,235,0.15)' stroke='#2563eb' stroke-width='2.5' stroke-linejoin='round'/>");
		// Dots
		for (int i = 0; i < 5; i++) {
			double s = vals[i] / 100.0;
			double px = cx + R * s * Math.cos(ang[i]);
			double py = cy + R * s * Math.sin(ang[i]);
			svg.append("<circle cx='").append(svgFmt(px)).append("' cy='").append(svgFmt(py)).append("' r='5' fill='").append(clrs[i]).append("' stroke='#fff' stroke-width='2'/>");
		}
		// Axis labels
		String[] anchors = { "middle", "start", "start", "end", "end" };
		for (int i = 0; i < 5; i++) {
			double lx = cx + R * 1.22 * Math.cos(ang[i]);
			double ly = cy + R * 1.22 * Math.sin(ang[i]);
			svg.append("<text x='").append(svgFmt(lx)).append("' y='").append(svgFmt(ly))
			   .append("' font-size='11' fill='#334155' text-anchor='").append(anchors[i]).append("' dominant-baseline='middle' font-weight='700'>")
			   .append(escapeHtml(lbls[i])).append("</text>");
		}
		svg.append("</svg>");
		return svg.toString();
	}

	private String svgFmt(double v) {
		return String.format("%.1f", v);
	}

	private void renderSopTrendBulananV13(org.zkoss.zk.ui.Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanelV7("Tren Aktivitas SOP per Bulan", parent);
		appendPanelExplanationV13(pch,
				"Menampilkan seberapa banyak aktivitas SOP terjadi tiap bulan.",
				"Dihitung dari sampel data alur kerja SOP sesuai filter dasbor.",
				"Batang lebih tinggi berarti lebih banyak aktivitas SOP pada bulan itu. Perhatikan apakah ada kenaikan atau penurunan yang mencolok.",
				"Jika ada bulan dengan lonjakan tinggi, periksa apakah ada event atau perubahan kebijakan yang menyebabkan kenaikan beban.");
		if (d.perBulan == null || d.perBulan.isEmpty()) {
			appendEmptyState(pch, "Belum ada data tren bulanan yang dapat ditampilkan.");
			return;
		}
		List<String> sortedKeys = new ArrayList<String>();
		for (String k : d.perBulan.keySet()) {
			if (k != null && k.length() >= 7 && !k.startsWith("?")) {
				sortedKeys.add(k);
			}
		}
		Collections.sort(sortedKeys);
		if (sortedKeys.size() > 12) {
			sortedKeys = new ArrayList<String>(sortedKeys.subList(sortedKeys.size() - 12, sortedKeys.size()));
		}
		if (sortedKeys.isEmpty()) {
			appendEmptyState(pch, "Belum ada data tren bulanan yang dapat ditampilkan.");
			return;
		}
		int maxVal = 1;
		for (String k : sortedKeys) {
			Integer v = d.perBulan.get(k);
			if (v != null && v.intValue() > maxVal) maxVal = v.intValue();
		}
		appendHtml(pch, buildSopTrendBarHtml(sortedKeys, d.perBulan, maxVal));
	}

	private static final String[] NAMA_BULAN_SINGKAT = { "Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des" };

	private String buildSopTrendBarHtml(List<String> keys, Map<String, Integer> data, int maxVal) {
		int minW = Math.max(360, keys.size() * 52);
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='overflow-x:auto;'>");
		sb.append("<div style='display:flex;align-items:flex-end;gap:8px;min-width:").append(minW)
		  .append("px;height:130px;padding:0 4px;border-bottom:2px solid #e2e8f0;margin-bottom:8px;box-sizing:border-box;'>");
		for (String key : keys) {
			Integer cnt = data.get(key);
			int val = cnt == null ? 0 : cnt.intValue();
			int hPx = maxVal <= 0 ? 4 : Math.max(4, (int) Math.round((val * 110.0) / maxVal));
			sb.append("<div style='flex:1;min-width:38px;display:flex;flex-direction:column;align-items:center;justify-content:flex-end;gap:4px;'>")
			  .append("<div style='font-size:10px;color:#0f172a;font-weight:800;'>").append(val).append("</div>")
			  .append("<div style='width:80%;height:").append(hPx).append("px;min-height:4px;border-radius:5px 5px 0 0;background:linear-gradient(180deg,var(--ais-theme-accent,#06b6d4),var(--ais-theme-primary,#2563eb));'></div>")
			  .append("</div>");
		}
		sb.append("</div>");
		sb.append("<div style='display:flex;gap:8px;min-width:").append(minW).append("px;padding:0 4px;'>");
		for (String key : keys) {
			String label = key;
			try {
				int mIdx = Integer.parseInt(key.substring(5, 7)) - 1;
				String yr = key.substring(2, 4);
				if (mIdx >= 0 && mIdx < 12) {
					label = NAMA_BULAN_SINGKAT[mIdx] + " '" + yr;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:1510");
			}
			sb.append("<div style='flex:1;min-width:38px;text-align:center;font-size:9px;color:#64748b;'>").append(escapeHtml(label)).append("</div>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	private void renderMiniGaugeV7(Component parent, String title, int pct, String desc, String color,
			String detailTitle, DetailCriteriaProvider provider) {
		renderMiniGaugeV7(parent, title, pct, desc, color, detailTitle, provider, createDisposisiAlurRowRenderer());
	}



	private void renderMiniGaugeV7(Component parent, String title, int pct, String desc, String color,
			String detailTitle, DetailCriteriaProvider provider, DetailRowRenderer renderer) {
		if (pct < 0) {
			pct = 0;
		}
		if (pct > 100) {
			pct = 100;
		}
		org.zkoss.zul.Div row = new org.zkoss.zul.Div();
		row.setStyle("padding:8px 0; border-bottom:1px solid #e2e8f0;");
		row.setParent(parent);
		org.zkoss.zul.Hbox top = new org.zkoss.zul.Hbox();
		top.setWidth("100%");
		top.setPack("justify");
		top.setAlign("center");
		top.setStyle("gap:10px;");
		top.setParent(row);
		appendHtml(top, "<div><div style='font-size:12px; font-weight:900; color:#0f172a;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:#64748b; margin-top:2px;'>" + escapeHtml(desc) + "</div></div>");
		createDetailNumber(top, pct + "%", detailTitle, provider, renderer,
				"font-size:13px; font-weight:900; color:#0f172a; text-decoration:none; cursor:pointer;");
		appendHtml(row, "<div style='margin-top:7px; height:9px; border-radius:999px; background:#e2e8f0; overflow:hidden;'>"
				+ "<div style='height:9px; width:" + pct + "%; border-radius:999px; background:" + color + ";'></div></div>");
	}

	private void createPressureCardV7(Component parent, String title, int value, String desc, String bg, String color,
			String detailTitle, DetailCriteriaProvider provider) {
		org.zkoss.zul.Div card = new org.zkoss.zul.Div();
		card.setStyle("flex:1 1 170px; min-width:170px; border-radius:14px; padding:13px; background:" + bg
				+ "; border:1px solid rgba(15,23,42,.08); box-sizing:border-box;");
		card.setParent(parent);
		createDetailNumber(card, String.valueOf(value), detailTitle, provider, createDisposisiAlurRowRenderer(),
				"display:block; font-size:25px; line-height:1; font-weight:900; color:" + color + "; text-decoration:none; cursor:pointer;");
		appendHtml(card, "<div style='font-size:12px; font-weight:900; color:" + color + "; margin-top:8px;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:" + color + "; opacity:.82; margin-top:5px;'>" + escapeHtml(desc) + "</div>");
	}

	private void renderMiniRowCounterV7(Component parent, Map<String, Integer> source, String emptyMessage, int limit,
			String color, String mode) {
		if (source == null || source.isEmpty()) {
			appendEmptyState(parent, emptyMessage);
			return;
		}
		List<CounterItem> rows = sortCounter(source);
		int max = rows.isEmpty() ? 1 : Math.max(1, rows.get(0).count);
		int no = 1;
		for (CounterItem rowData : rows) {
			if (rowData == null) {
				continue;
			}
			if (no > limit) {
				break;
			}
			int pct = max <= 0 ? 0 : (int) Math.round((rowData.count * 100.0) / max);
			if (pct < 4 && rowData.count > 0) {
				pct = 4;
			}
			org.zkoss.zul.Div row = new org.zkoss.zul.Div();
			row.setStyle("padding:9px 0; border-bottom:1px solid #f1f5f9;");
			row.setParent(parent);
			org.zkoss.zul.Hbox top = new org.zkoss.zul.Hbox();
			top.setWidth("100%");
			top.setPack("justify");
			top.setStyle("gap:10px;");
			top.setParent(row);
			appendHtml(top, "<div style='font-size:12px; font-weight:800; color:#334155;'>" + no + ". " + escapeHtml(rowData.label) + "</div>");
			DetailCriteriaProvider provider = "sop".equals(mode) ? createSopCounterCriteriaProvider(rowData.label)
					: createAktorCounterCriteriaProvider(rowData.label);
			createDetailNumber(top, String.valueOf(rowData.count), "Detail " + rowData.label, provider,
					createDisposisiAlurRowRenderer(), "font-size:12px; font-weight:900; color:#0f172a; text-decoration:none; cursor:pointer;");
			appendHtml(row, "<div style='margin-top:7px; height:8px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>"
					+ "<div style='height:8px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div></div>");
			no++;
		}
	}

	private void renderRecentItemsV7(Component parent, SopDashboardData d) {
		appendHtml(parent, "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Aktivitas terbaru pada filter saat ini</div>");
		if (d.aktivitasTerbaru == null || d.aktivitasTerbaru.isEmpty()) {
			appendEmptyState(parent, "Belum ada aktivitas terbaru yang dapat ditampilkan.");
			return;
		}
		String html = "";
		for (SopRecentItem item : d.aktivitasTerbaru) {
			html += "<div style='padding:11px 12px; border:1px solid #e2e8f0; border-radius:14px; margin-bottom:8px; background:#ffffff;'>"
					+ "<div style='display:flex; justify-content:space-between; gap:8px; align-items:flex-start;'>"
					+ "<div style='font-weight:800; color:#0f172a; font-size:13px;'>" + escapeHtml(item.sop) + "</div>"
					+ "<span style='display:inline-block; padding:5px 9px; border-radius:999px; background:#eff6ff; color:#1d4ed8; font-size:11px; font-weight:800;'>" + escapeHtml(item.status) + "</span></div>"
					+ "<div style='font-size:12px; color:#475569; margin-top:4px;'>" + escapeHtml(item.aktor) + "</div>"
					+ "<div style='font-size:11px; color:#94a3b8; margin-top:6px;'>" + escapeHtml(item.waktu)
					+ (item.deadline == null || item.deadline.isEmpty() ? "" : " · Batas Waktu " + escapeHtml(item.deadline))
					+ "</div></div>";
		}
		appendHtml(parent, html);
	}

	private String buildActionPlanCardV7(String no, String title, String desc, String bg, String color) {
		return "<div style='border-radius:16px; padding:14px; background:" + bg
				+ "; border:1px solid rgba(15,23,42,.08); min-height:105px;'>"
				+ "<div style='display:flex; justify-content:space-between; gap:12px; align-items:center;'>"
				+ "<div style='font-size:12px; font-weight:900; color:" + color + ";'>" + escapeHtml(title) + "</div>"
				+ "<div style='width:28px; height:28px; border-radius:999px; background:#ffffff; color:" + color
				+ "; display:flex; align-items:center; justify-content:center; font-weight:900;'>" + escapeHtml(no) + "</div></div>"
				+ "<div style='font-size:12px; color:" + color + "; line-height:1.45; margin-top:10px;'>"
				+ escapeHtml(desc) + "</div></div>";
	}

	private int percentV7(int value, int total) {
		if (total <= 0 || value <= 0) {
			return 0;
		}
		return (int) Math.round((value * 100.0d) / total);
	}

	private String sectionIntroHtmlV7(String title, String description) {
		return "<div style='margin-top:16px; padding:14px 16px; border-radius:16px; background:#ffffff; "
				+ "border:1px solid #e8eef6; box-shadow:0 8px 22px rgba(15,23,42,0.04);'>"
				+ "<div style='font-size:16px; font-weight:900; color:#0f172a;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:5px; line-height:1.55;'>"
				+ escapeHtml(description) + "</div></div>";
	}

	private void accumulateMetadataQualitySopV7(SopDashboardData d, DisposisiAlurSop row) {
		try {
			if (row.getWaktuMaksimal() == null) {
				d.tanpaDeadline++;
			}
		} catch (Exception e) {
			d.tanpaDeadline++;
		}
		try {
			if (row.getAlurSop() == null || row.getAlurSop().getAktor() == null || row.getAlurSop().getAktor().trim().isEmpty()) {
				d.tanpaAktor++;
			}
		} catch (Exception e) {
			d.tanpaAktor++;
		}
		try {
			if (row.getAlurSop() == null || row.getAlurSop().getNama() == null || row.getAlurSop().getNama().trim().isEmpty()) {
				d.tanpaTahap++;
			}
		} catch (Exception e) {
			d.tanpaTahap++;
		}
		try {
			if (row.getKeterangan() == null || row.getKeterangan().trim().isEmpty()) {
				d.tanpaCatatan++;
			}
		} catch (Exception e) {
			d.tanpaCatatan++;
		}
	}


	private void renderFunnelProsesSop(Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanel("Overview Funnel Proses SOP", parent);
		appendPanelExplanationV13(pch, "Memberikan ringkasan posisi proses SOP secara sederhana sebelum melihat analisis yang lebih rinci.", "Jumlah pengajuan Anda, proses yang menunggu disposisi, proses berjalan, proses yang sudah Anda tindak lanjuti, dan proses yang sudah selesai.", "Gunakan ringkasan ini untuk melihat apakah proses lebih banyak tertahan di awal, di tahap disposisi, atau menjelang selesai.", "Klik angka terbesar untuk melihat data yang paling banyak dan menentukan tindakan berikutnya.");
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>Ringkasan posisi proses berdasarkan query yang sudah tersedia pada class SOP. Semua angka bisa diklik untuk melihat data detail.</div>");
		int max = getMaxValue(new int[] { d.jumlahSopBaru, d.menungguSaya, d.menungguAktor, d.sudahSayaDisposisi, d.selesai });
		renderFunnelRow(pch, "Jumlah Data Pengajuan Anda", d.jumlahSopBaru, max, "#2563eb", "Detail Data Pengajuan Anda", createDataPengajuanAndaCriteriaProvider(), createDisposisiSopRowRenderer());
		renderFunnelRow(pch, "Menunggu Disposisi Anda", d.menungguSaya, max, "#dc2626", "Detail Menunggu Disposisi Saya", createMenungguSayaCriteriaProvider(), createDisposisiAlurRowRenderer());
		renderFunnelRow(pch, "Menunggu Petugas / Tahap", d.menungguAktor, max, "#7c3aed", "Detail Proses Menunggu Petugas", createMenungguAktorCriteriaProvider(), createDisposisiAlurRowRenderer());
		renderFunnelRow(pch, "Sudah Anda Disposisi", d.sudahSayaDisposisi, max, "#16a34a", "Detail Pengajuan yang Sudah Saya Disposisi", createSudahDisposisiCriteriaProvider(), createDisposisiAlurRowRenderer());
		renderFunnelRow(pch, "Selesai", d.selesai, max, "#d97706", "Detail Pengajuan Selesai", createSelesaiCriteriaProvider(), createDisposisiAlurRowRenderer());
	}


	private void renderFunnelRow(Component parent, String label, int value, int max, String color, String detailTitle,
			DetailCriteriaProvider provider, DetailRowRenderer renderer) {
		int pct = max <= 0 ? 0 : (int) Math.round((value * 100.0) / max);
		if (pct < 4 && value > 0) {
			pct = 4;
		}
		org.zkoss.zul.Hbox row = new org.zkoss.zul.Hbox();
		row.setWidth("100%");
		row.setAlign("center");
		// Garis pemisah antar-baris + bingkai kiri (label) vs angka kanan agar mudah dibaca
		// (konsisten dgn panel "Sebaran SOP Terbanyak" yg sudah memakai border-bottom).
		row.setStyle("gap:10px; margin:0; padding:9px 2px; border-bottom:1px solid #eef2f7;");
		row.setParent(parent);
		appendHtml(row, "<div style='width:180px; font-size:12px; color:#334155; font-weight:700;'>" + escapeHtml(label) + "</div>");
		appendHtml(row, "<div style='flex:1; background:#e5e7eb; border-radius:999px; height:12px; overflow:hidden;'>"
				+ "<div style='height:12px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div></div>");
		createDetailNumber(row, String.valueOf(value), detailTitle, provider, renderer,
				"width:54px; text-align:right; font-size:13px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer;"
						+ " border-left:1px solid #e5e7eb; padding-left:10px;");
	}


	private void renderDeadlineAnalytic(Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanel("Analitik Risiko Batas Waktu", parent);
		appendPanelExplanationV13(pch, "Menampilkan proses SOP berdasarkan kedekatan atau keterlambatan batas waktunya.", "Batas waktu proses dan status apakah proses sudah selesai atau belum.", "Yang sudah lewat batas waktu dan yang jatuh tempo hari ini adalah prioritas utama. Yang jatuh tempo minggu ini perlu dipantau.", "Tindak lanjuti data batas waktu kritis terlebih dahulu agar proses tidak semakin terlambat.");
		int total = d.deadlineLewat + d.deadlineHariIni + d.deadlineMingguIni + d.deadlineAman;
		if (total == 0) {
			appendEmptyState(pch, "Belum ada data batas waktu aktif pada sampel proses yang dapat dianalisis.");
			return;
		}
		int max = getMaxValue(new int[] { d.deadlineLewat, d.deadlineHariIni, d.deadlineMingguIni, d.deadlineAman });
		renderFunnelRow(pch, "Lewat Batas Waktu", d.deadlineLewat, max, "#b91c1c", "Detail Lewat Batas Waktu", createDeadlineCriteriaProvider("lewat"), createDisposisiAlurRowRenderer());
		renderFunnelRow(pch, "Jatuh Tempo Hari Ini", d.deadlineHariIni, max, "#ea580c", "Detail Batas Waktu Hari Ini", createDeadlineCriteriaProvider("hari_ini"), createDisposisiAlurRowRenderer());
		renderFunnelRow(pch, "7 Hari ke Depan", d.deadlineMingguIni, max, "#ca8a04", "Detail Batas Waktu 7 Hari ke Depan", createDeadlineCriteriaProvider("minggu_ini"), createDisposisiAlurRowRenderer());
		renderFunnelRow(pch, "Masih Aman", d.deadlineAman, max, "#16a34a", "Detail Batas Waktu Masih Aman", createDeadlineCriteriaProvider("aman"), createDisposisiAlurRowRenderer());
		appendHtml(pch, "<div style='margin-top:12px; padding:10px; border-radius:12px; background:#f8fafc; border:1px solid #e2e8f0; color:#64748b; font-size:11px;'>Prioritaskan item <b>Lewat Batas Waktu</b> dan <b>Hari Ini</b> untuk mengurangi titik macet proses SOP.</div>");
	}


	private void renderSebaranSop(Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanel("Sebaran SOP Terbanyak", parent);
		appendPanelExplanationV13(pch, "Menunjukkan jenis SOP yang paling banyak digunakan atau paling banyak muncul dalam proses.", "Jumlah kemunculan tiap nama SOP pada data yang sedang difilter.", "SOP yang paling sering digunakan perlu dipastikan alurnya mudah, jelas, dan tidak membuat proses menumpuk.", "Tinjau SOP yang paling sering digunakan untuk kemungkinan disederhanakan atau dibuatkan pengingat otomatis.");
		renderTopCounterList(pch, d.perSop, "Belum ada sebaran SOP yang dapat ditampilkan.", "sop");
	}


	private void renderBebanAktor(Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanel("Beban Per Petugas / Tahap SOP", parent);
		appendPanelExplanationV13(pch, "Menunjukkan petugas atau tahap yang menerima beban proses paling banyak.", "Data nama petugas/tahap pada alur SOP yang sedang berjalan.", "Semakin tinggi jumlah pada satu tahap, semakin besar kemungkinan proses menumpuk di tahap tersebut.", "Pertimbangkan petugas pengganti, pembagian tugas, atau pengingat otomatis untuk tahap yang paling padat.");
		renderTopCounterList(pch, d.perAktor, "Belum ada beban petugas yang dapat ditampilkan.", "petugas");
	}


	private void renderTopCounterList(Component parent, Map<String, Integer> source, String emptyMessage, final String mode) {
		if (source == null || source.isEmpty()) {
			appendEmptyState(parent, emptyMessage);
			return;
		}

		List<CounterItem> list = sortCounter(source);
		int max = list.isEmpty() ? 0 : list.get(0).count;
		int no = 1;
		for (final CounterItem item : list) {
			if (no > 8) {
				break;
			}
			int pct = max <= 0 ? 0 : (int) Math.round((item.count * 100.0) / max);
			if (pct < 4 && item.count > 0) {
				pct = 4;
			}

			org.zkoss.zul.Div row = new org.zkoss.zul.Div();
			row.setStyle("padding:10px 0; border-bottom:1px solid #f1f5f9;");
			row.setParent(parent);

			org.zkoss.zul.Hbox top = new org.zkoss.zul.Hbox();
			top.setWidth("100%");
			top.setPack("justify");
			top.setAlign("center");
			top.setStyle("gap:12px;");
			top.setParent(row);

			appendHtml(top, "<div style='font-size:12px; font-weight:700; color:#334155;'>" + no + ". " + escapeHtml(item.label) + "</div>");
			DetailCriteriaProvider provider = "sop".equals(mode) ? createSopCounterCriteriaProvider(item.label) : createAktorCounterCriteriaProvider(item.label);
			createDetailNumber(top, String.valueOf(item.count), "Detail " + item.label, provider, createDisposisiAlurRowRenderer(),
					"font-size:12px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer;");

			appendHtml(row, "<div style='margin-top:7px; height:8px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>"
					+ "<div style='height:8px; width:" + pct + "%; background:#0ea5e9; border-radius:999px;'></div></div>");
			no++;
		}
	}


	private List<CounterItem> sortCounter(Map<String, Integer> source) {
		List<CounterItem> list = new ArrayList<CounterItem>();
		for (String key : source.keySet()) {
			CounterItem item = new CounterItem();
			item.label = key;
			item.count = source.get(key) == null ? 0 : source.get(key).intValue();
			list.add(item);
		}
		Collections.sort(list, new Comparator<CounterItem>() {
			@Override
			public int compare(CounterItem a, CounterItem b) {
				if (b.count != a.count) {
					return b.count - a.count;
				}
				return a.label.compareToIgnoreCase(b.label);
			}
		});
		return list;
	}

	private void renderAktivitasTerbaru(Component parent, SopDashboardData d) {
		Panelchildren pch = createModernPanel("Aktivitas SOP Terbaru", parent);
		appendPanelExplanationV13(pch, "Menampilkan aktivitas terbaru agar pengguna dapat melihat perubahan terakhir pada proses SOP.", "Data proses SOP terbaru berdasarkan urutan terakhir masuk atau terakhir diproses.", "Aktivitas terbaru membantu memastikan bahwa proses masih berjalan dan tidak berhenti terlalu lama.", "Gunakan untuk pengecekan cepat setelah melakukan disposisi atau setelah memperbarui dasbor.");
		if (d.aktivitasTerbaru == null || d.aktivitasTerbaru.isEmpty()) {
			appendEmptyState(pch, "Belum ada aktivitas terbaru yang dapat ditampilkan.");
			return;
		}

		String html = "";
		for (SopRecentItem item : d.aktivitasTerbaru) {
			html += "<div style='display:flex; gap:10px; padding:11px 0; border-bottom:1px solid #f1f5f9;'>"
					+ "<div style='width:10px; height:10px; margin-top:5px; border-radius:999px; background:#2563eb;'></div>"
					+ "<div style='flex:1; min-width:0;'>"
					+ "<div style='font-size:12px; font-weight:800; color:#0f172a;'>" + escapeHtml(item.sop) + "</div>"
					+ "<div style='font-size:11px; color:#64748b; margin-top:3px;'>" + escapeHtml(item.aktor) + "</div>"
					+ "<div style='font-size:11px; color:#94a3b8; margin-top:3px;'>" + escapeHtml(item.waktu) + (item.deadline == null || item.deadline.isEmpty() ? "" : " · Batas Waktu " + escapeHtml(item.deadline)) + "</div>"
					+ "</div><div style='font-size:10px; color:#1d4ed8; background:#dbeafe; border-radius:999px; padding:4px 8px; height:14px; white-space:nowrap;'>"
					+ escapeHtml(item.status) + "</div></div>";
		}
		appendHtml(pch, html);
	}

	private void renderQuickInsight(Component parent, final SopDashboardData d) {
		Panelchildren pch = createModernPanel("Rekomendasi Tindak Lanjut", parent);
		appendPanelExplanationV13(pch, "Menampilkan saran tindakan berdasarkan kondisi data agar pengguna lebih mudah menentukan prioritas kerja.", "Ringkasan jumlah antrian, batas waktu, kelengkapan data, dan tahap yang paling banyak muncul.", "Saran akan berubah sesuai filter dan kondisi data yang sedang ditampilkan.", "Gunakan saran ini sebagai panduan sebelum membuka detail proses satu per satu.");

		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		wrap.setParent(pch);

		createInsightCard(wrap, "Fokus Hari Ini", d.menungguSaya,
				d.menungguSaya > 0 ? "proses menunggu disposisi Anda." : "proses menunggu disposisi langsung dari Anda.",
				"#f8fafc", "#e2e8f0", "#0f172a", "Detail Menunggu Disposisi Saya", createMenungguSayaCriteriaProvider());
		createInsightCard(wrap, "Kontrol Batas Waktu", d.deadlineLewat + d.deadlineHariIni,
				(d.deadlineLewat + d.deadlineHariIni) > 0 ? "proses perlu dicek segera." : "risiko batas waktu kritis pada sampel aktif.",
				"#fff7ed", "#fed7aa", "#9a3412", "Detail Batas Waktu Kritis", createDeadlineUrgentCriteriaProvider());
		createInsightCard(wrap, "Kinerja Penyelesaian", d.selesai,
				"pengajuan selesai yang terdeteksi.", "#ecfdf5", "#bbf7d0", "#166534", "Detail Pengajuan Selesai", createSelesaiCriteriaProvider());

		org.zkoss.zul.Div actionBox = new org.zkoss.zul.Div();
		actionBox.setStyle("margin-top:14px; display:flex; gap:8px; flex-wrap:wrap;");
		actionBox.setParent(pch);

		MyToolbarbuttonConfig btnRefresh = new MyToolbarbuttonConfig("Refresh Dasbor", "/img/svg/refresh.svg");
		btnRefresh.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; padding:7px 14px;");
		btnRefresh.setParent(actionBox);
		btnRefresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(DasboardSop.this);
				DasboardSop.this.init();
			}
		});
	}


	private void renderWorkflowDetailAnalytics(Component parent, final SopDashboardData d) {
		Panelchildren pch = createModernPanel("Analisis Alur Kerja Lanjutan", parent);
		appendPanelExplanationV13(pch, "Menggabungkan beberapa indikator untuk melihat apakah proses SOP berjalan efektif atau masih banyak hambatan.", "Ringkasan dasbor, batas waktu, jumlah antrian, kelengkapan data, dan proses selesai.", "Jika indikator antrian atau risiko tinggi, berarti perlu tindakan perbaikan. Jika indikator penyelesaian tinggi, berarti proses berjalan baik.", "Gunakan bagian ini untuk pemantauan rutin oleh admin dan pimpinan.");
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>Dasbor tambahan untuk membaca tekanan alur kerja, titik macet, dan prioritas eksekusi. Klik angka untuk membuka detail tabel.</div>");

		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		wrap.setParent(pch);

		createWorkflowMiniCard(wrap, "Tekanan Antrian", d.totalAntrian, "Total proses dalam antrian aktif.", "Detail Total Antrian", createTotalAntrianCriteriaProvider());
		createWorkflowMiniCard(wrap, "Batas Waktu Kritis", d.deadlineLewat + d.deadlineHariIni, "Lewat batas waktu dan jatuh tempo hari ini.", "Detail Batas Waktu Kritis", createDeadlineUrgentCriteriaProvider());
		createWorkflowMiniCard(wrap, "Proses Dipantau", d.totalDipantau, "Sampel proses yang dianalisis dasbor.", "Detail Proses Dipantau", createAllDashboardCriteriaProvider());

		CounterItem topSop = getFirstCounterItem(d.perSop);
		if (topSop != null) {
			createWorkflowMiniCard(wrap, "SOP Paling Padat", topSop.count, topSop.label, "Detail " + topSop.label, createSopCounterCriteriaProvider(topSop.label));
		}

		CounterItem topAktor = getFirstCounterItem(d.perAktor);
		if (topAktor != null) {
			createWorkflowMiniCard(wrap, "Tahap Paling Padat", topAktor.count, topAktor.label, "Detail " + topAktor.label, createAktorCounterCriteriaProvider(topAktor.label));
		}
	}

	private CounterItem getFirstCounterItem(Map<String, Integer> source) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		List<CounterItem> list = sortCounter(source);
		return list.isEmpty() ? null : list.get(0);
	}

	private void createWorkflowMiniCard(Component parent, String title, int value, String desc, String detailTitle,
			DetailCriteriaProvider provider) {
		org.zkoss.zul.Div card = new org.zkoss.zul.Div();
		card.setStyle("flex:1 1 220px; min-width:220px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:14px;"
				+ "box-shadow:0 8px 18px rgba(15,23,42,.05); box-sizing:border-box;");
		card.setParent(parent);
		appendHtml(card, "<div style='font-size:12px; font-weight:800; color:#0f172a;'>" + escapeHtml(title) + "</div>");
		createDetailNumber(card, String.valueOf(value), detailTitle, provider, createDisposisiAlurRowRenderer(),
				"display:block; margin-top:8px; font-size:28px; line-height:1; font-weight:900; color:#2563eb; text-decoration:none; cursor:pointer;");
		appendHtml(card, "<div style='font-size:11px; color:#64748b; margin-top:8px;'>" + escapeHtml(desc) + "</div>");
	}

	private void createInsightCard(Component parent, String title, int value, String desc, String bg, String border,
			String color, String detailTitle, DetailCriteriaProvider provider) {
		org.zkoss.zul.Div card = new org.zkoss.zul.Div();
		card.setStyle("flex:1 1 280px; background:" + bg + "; border:1px solid " + border + "; border-radius:14px; padding:13px; box-sizing:border-box;");
		card.setParent(parent);
		appendHtml(card, "<div style='font-size:12px; font-weight:800; color:" + color + ";'>" + escapeHtml(title) + "</div>");
		org.zkoss.zul.Hbox line = new org.zkoss.zul.Hbox();
		line.setAlign("center");
		line.setStyle("gap:6px; margin-top:5px;");
		line.setParent(card);
		createDetailNumber(line, String.valueOf(value), detailTitle, provider, createDisposisiAlurRowRenderer(),
				"font-size:16px; font-weight:900; color:" + color + "; text-decoration:none; cursor:pointer;");
		appendHtml(line, "<div style='font-size:12px; color:" + color + ";'>" + escapeHtml(desc) + "</div>");
	}

	private void createHeroNumber(Component parent, String label, int value, String detailTitle, DetailCriteriaProvider provider) {
		org.zkoss.zul.Div card = new org.zkoss.zul.Div();
		card.setStyle("background:rgba(255,255,255,.16); border:1px solid rgba(255,255,255,.24); padding:12px 16px; border-radius:14px; min-width:120px; text-align:center; box-sizing:border-box;");
		card.setParent(parent);
		createDetailNumber(card, String.valueOf(value), detailTitle, provider, createDisposisiAlurRowRenderer(),
				"display:block; font-size:24px; font-weight:800; color:#ffffff; text-decoration:none; cursor:pointer;");
		appendHtml(card, "<div style='font-size:11px; opacity:.85;'>" + escapeHtml(label) + "</div>");
	}

	private A createDetailNumber(Component parent, String text, final String title, final DetailCriteriaProvider provider,
			final DetailRowRenderer renderer, String style) {
		A a = new A(text);
		a.setTooltiptext("Klik untuk melihat detail data");
		a.setStyle(style);
		a.setParent(parent);
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail(title, provider, renderer);
			}
		});
		return a;
	}

	private void viewDetail(final String title, final DetailCriteriaProvider provider, final DetailRowRenderer renderer)
			throws Exception {
		final org.zkoss.zul.Window window = new org.zkoss.zul.Window();
		window.setTitle(title);
		window.setClosable(true);
		window.setSizable(true);
		window.setBorder("normal");
		window.setWidth(Common.isMobile() ? "96%" : "92%");
		window.setHeight(Common.isMobile() ? "92%" : "82%");
		window.setStyle("border-radius:14px; overflow:hidden;");
		try {
			if (DasboardSop.this.getPage() != null) {
				window.setPage(DasboardSop.this.getPage());
			} else {
				window.setParent(menu != null ? menu : DasboardSop.this);
			}
		} catch (Exception e) {
			printDebug(e);
			window.setParent(menu != null ? menu : DasboardSop.this);
		}

		org.zkoss.zul.Vbox root = new org.zkoss.zul.Vbox();
		root.setWidth("100%");
		root.setHeight("100%");
		root.setStyle("background:#f8fafc; padding:12px; box-sizing:border-box;");
		root.setParent(window);

		final org.zkoss.zul.Label info = new org.zkoss.zul.Label();
		info.setStyle("font-size:12px; font-weight:700; color:#334155; padding:8px 10px; background:#ffffff; border:1px solid #e5e7eb; border-radius:10px;");
		info.setParent(root);

		final Paging paging = new Paging();
		paging.setPageSize(DETAIL_PAGE_SIZE);
		paging.setMold("os");
		paging.setParent(root);

		final org.zkoss.zul.Div dataBox = new org.zkoss.zul.Div();
		dataBox.setWidth("100%");
		dataBox.setHeight("100%");
		dataBox.setStyle("overflow:auto; background:#ffffff; border:1px solid #e5e7eb; border-radius:12px; padding:8px; box-sizing:border-box;");
		dataBox.setParent(root);

		final EventListener reload = new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(dataBox);

				Criteria countCriteria = null;
				int total = 0;
				try {
					countCriteria = provider.buildCriteria(false);
					Number number = (Number) countCriteria.setProjection(Projections.rowCount()).uniqueResult();
					total = number == null ? 0 : number.intValue();
					paging.setTotalSize(total);
				} catch (Exception e) {
					printDebug(e);
					appendEmptyState(dataBox,
							"Terjadi error saat menghitung data detail. Aktifkan debug/debuh=true untuk melihat stacktrace.");
					return;
				} finally {
					closeCriteriaSessionSafely(countCriteria);
				}

				Criteria listCriteria = null;
				List data = new ArrayList();
				try {
					listCriteria = provider.buildCriteria(true);
					data = listCriteria.setFirstResult(DETAIL_PAGE_SIZE * paging.getActivePage())
							.setMaxResults(DETAIL_PAGE_SIZE).list();

					int start = total == 0 ? 0 : (DETAIL_PAGE_SIZE * paging.getActivePage()) + 1;
					int end = DETAIL_PAGE_SIZE * paging.getActivePage() + (data == null ? 0 : data.size());
					info.setValue("Menampilkan " + start + " - " + end + " dari " + total
							+ " data. Page size: " + DETAIL_PAGE_SIZE + ".");

					Grid grid = new Grid();
					grid.setSclass("dtabel ftabel");
					grid.setWidth("100%");
					grid.setStyle("border:0; background:#ffffff;");
					grid.setParent(dataBox);

					org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
					columns.setParent(grid);
					String[] headers = renderer.getHeaders();
					for (int i = 0; i < headers.length; i++) {
						org.zkoss.zul.Column col = new org.zkoss.zul.Column(headers[i]);
						col.setStyle("font-weight:800; color:#0f172a; background:#f1f5f9;");
						col.setParent(columns);
					}

					Rows rows = new Rows();
					rows.setParent(grid);
					if (data == null || data.isEmpty()) {
						Row row = new Row();
						row.setParent(rows);
						org.zkoss.zul.Label lbl = new org.zkoss.zul.Label(
								ais.common.Common.getBahasaConfig("Tidak ada data detail untuk indikator ini."));
						lbl.setStyle("padding:14px; color:#64748b;");
						row.appendChild(lbl);
					} else {
						renderer.render(rows, data);
					}
				} catch (Exception e) {
					printDebug(e);
					appendEmptyState(dataBox,
							"Terjadi error saat mengambil data detail. Aktifkan debug/debuh=true untuk melihat stacktrace.");
				} finally {
					data = null;
					closeCriteriaSessionSafely(listCriteria);
				}
			}
		};
		paging.addEventListener("onPaging", reload);
		reload.onEvent(null);
		window.doModal();
	}

	private DetailRowRenderer createDisposisiAlurRowRenderer() {
		return new DetailRowRenderer() {
			@Override
			public String[] getHeaders() {
				return new String[] { "SOP", "Pengaju", "Tahap / Petugas", "Status", "Waktu", "Batas Waktu", "Catatan", "" };
			}

			@SuppressWarnings("unchecked")
			@Override
			public void render(Rows rows, List data) throws Exception {
				for (Iterator it = data.iterator(); it.hasNext();) {
					final DisposisiAlurSop item = (DisposisiAlurSop) it.next();
					Row row = new Row();
					row.setParent(rows);
					addGridCell(row, getNamaSop(item));
					String oleh = "";
					try {
						oleh = getOlehName(item.getDisposisiSop().getDiajukanOleh(), item.getDisposisiSop().getSiswa(), item.getDisposisiSop().getMahasiswa());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:2081");
					}
					addGridCell(row, oleh == null || oleh.isEmpty() ? "-" : oleh);
					addGridCell(row, getNamaAktor(item));
					addGridCell(row, getStatusRingkas(item));
					addGridCell(row, getWaktuText(item));
					addGridCell(row, item.getWaktuMaksimal() == null ? "-" : Common.dateFormat51.get().format(item.getWaktuMaksimal()));
					addGridCell(row, item.getKeterangan() == null ? "" : item.getKeterangan());

					A buka = new A("Buka");
					buka.setStyle("font-weight:800; color:#2563eb; text-decoration:none; cursor:pointer;");
					buka.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							if (item != null && item.getDisposisiSop() != null) {
								TampilanAlurSopAction.prosess(item.getDisposisiSop().getId(), tabs, tabpanels, true, menu, null);
							}
						}
					});
					row.appendChild(buka);
				}
			}
		};
	}


	private DetailRowRenderer createDisposisiSopRowRenderer() {
		return new DetailRowRenderer() {
			@Override
			public String[] getHeaders() {
				return new String[] { "SOP", "Pengaju", "Waktu Pengajuan", "Status Terakhir", "Keterangan", "" };
			}

			@SuppressWarnings("unchecked")
			@Override
			public void render(Rows rows, List data) throws Exception {
				for (Iterator it = data.iterator(); it.hasNext();) {
					final DisposisiSop item = (DisposisiSop) it.next();
					Row row = new Row();
					row.setParent(rows);

					String sopName = "SOP Tidak Diketahui";
					try {
						if (item.getSop() != null && item.getSop().getNama() != null) {
							sopName = item.getSop().getNama();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:2127");
					}
					addGridCell(row, sopName);

					String pengaju = "-";
					try {
						pengaju = getOlehName(item.getDiajukanOleh(), item.getSiswa(), item.getMahasiswa());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:2134");
					}
					addGridCell(row, pengaju == null || pengaju.isEmpty() ? "-" : pengaju);

					String waktu = "-";
					try {
						waktu = item.getWaktu() == null ? "-" : Common.dateFormat.get().format(item.getWaktu());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:2141");
					}
					addGridCell(row, waktu);

					String statusTerakhir = "-";
					Session sessionStatus = null;
					try {
						sessionStatus = HibernateUtil.openSession();
						DisposisiAlurSop terakhir = (DisposisiAlurSop) sessionStatus
								.createCriteria(DisposisiAlurSop.class)
								.add(Restrictions.eq("disposisiSop", item))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
						if (terakhir != null) {
							statusTerakhir = getStatusRingkas(terakhir) + " - " + getNamaAktor(terakhir);
						}
					} catch (Exception e) {
						printDebug(e);
					} finally {
						closeSessionSafely(sessionStatus);
					}
					addGridCell(row, statusTerakhir);
					addGridCell(row, item.getKeterangan() == null ? "" : item.getKeterangan());

					A buka = new A("Buka");
					buka.setStyle("font-weight:800; color:#2563eb; text-decoration:none; cursor:pointer;");
					buka.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							if (item != null && item.getId() != null) {
								TampilanAlurSopAction.prosess(item.getId(), tabs, tabpanels, true, menu, null);
							}
						}
					});
					row.appendChild(buka);
				}
			}
		};
	}

	private DetailRowRenderer createAlurSopRowRenderer() {
		return new DetailRowRenderer() {
			@Override
			public String[] getHeaders() {
				return new String[] { "SOP", "Tahap Awal", "Petugas", "Status", "" };
			}

			@SuppressWarnings("unchecked")
			@Override
			public void render(Rows rows, List data) throws Exception {
				for (Iterator it = data.iterator(); it.hasNext();) {
					final AlurSop item = (AlurSop) it.next();
					Row row = new Row();
					row.setParent(rows);
					String sopName = "SOP Tidak Diketahui";
					try {
						if (item.getSop() != null) {
							sopName = item.getSop().getNama();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:2200");
					}
					addGridCell(row, sopName);
					addGridCell(row, item.getNama() == null ? "-" : item.getNama());
					addGridCell(row, item.getAktor() == null ? "-" : item.getAktor());
					addGridCell(row, isAlurStart(item) ? "Tahap Awal" : "Tahap Lanjutan");
					A ajukan = new A("Ajukan");
					ajukan.setStyle("font-weight:800; color:#2563eb; text-decoration:none; cursor:pointer;");
					ajukan.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							DisposisiSop disposisi = new DisposisiSop();
							DisposisiSopAction.onAddExternal(new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									Common.clear(DasboardSop.this);
									DasboardSop.this.init();
								}
							}, disposisi, true);
						}
					});
					row.appendChild(ajukan);
				}
			}
		};
	}

	private boolean isAlurStart(AlurSop item) {
		if (item == null) {
			return false;
		}
		try {
			Object value = item.getClass().getMethod("getStart", new Class[0]).invoke(item, new Object[0]);
			return Boolean.TRUE.equals(value);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:2234");
		}
		try {
			Object value = item.getClass().getMethod("isStart", new Class[0]).invoke(item, new Object[0]);
			return Boolean.TRUE.equals(value);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:2239");
		}
		return false;
	}

	private void addGridCell(Row row, String text) {
		org.zkoss.zul.Label label = new org.zkoss.zul.Label(text == null ? "" : text);
		label.setStyle("font-size:12px; color:#334155; padding:7px 6px; white-space:normal;");
		row.appendChild(label);
	}


	private DetailCriteriaProvider createDataPengajuanAndaCriteriaProvider() {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Session session = HibernateUtil.openSession();
				Criteria criteria = createCriteria(session, DisposisiSop.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(getUserRestriction(tbmuser, ""));
				try {
					criteria.createAlias("sop", "sopDetail", Criteria.LEFT_JOIN);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:2261");
				}
				applyGlobalDisposisiSopFilter(criteria);
				if (order) {
					criteria.addOrder(Order.desc("id"));
				}
				return criteria;
			}
		};
	}

	private DetailCriteriaProvider createPengajuanBaruCriteriaProvider() {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Session session = HibernateUtil.openSession();
				Criteria criteria = createCriteria(session, AlurSop.class)
						.add(Restrictions.eq("start", true))
						.createAlias("aktorSop", "aktorSop")
						.add(getAktorRestriction());
				try {
					criteria.createAlias("sop", "sopDetail", Criteria.LEFT_JOIN);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:2283");
				}
				if (order) {
					try {
						criteria.addOrder(Order.asc("sopDetail.nama"));
					} catch (Exception e) {
						criteria.addOrder(Order.asc("id"));
					}
				}
				return criteria;
			}
		};
	}


	private Criteria createMenungguSayaSesuaiPanelCriteria(Session session, boolean order, boolean hanyaLewatDeadline) {
		Criteria criteria = createCriteria(session, DisposisiAlurSop.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("selesai"), Restrictions.eq("selesai", false)))
				.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
				.add(Restrictions.isNotNull("alurSop.sop"))
				.createAlias("alurSop.aktorSop", "aktorSop", Criteria.LEFT_JOIN)
				.createAlias("disposisiSop", "disposisiSop")
				.createAlias("sebelumnya", "sebelumnya", Criteria.LEFT_JOIN);

		criteria.add(getAktorRestriction(criteria))
				.add(Restrictions.and(Restrictions.isNull("diajukanOleh"),
						Restrictions.and(Restrictions.isNull("siswa"), Restrictions.isNull("mahasiswa"))))
				.add(Restrictions.isNull("setelahnya"))
				.add(Restrictions.isNotNull("sebelumnya"))
				.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
						Restrictions.eq("disposisiSop.aktif", true)));

		if (hanyaLewatDeadline) {
			criteria.add(Restrictions.isNotNull("waktuMaksimal"));
			criteria.add(Restrictions.lt("waktuMaksimal", new Date()));
		}

		if (order) {
			if (hanyaLewatDeadline) {
				criteria.addOrder(Order.asc("waktuMaksimal"));
			} else {
				criteria.addOrder(Order.desc("id"));
			}
		}
		return criteria;
	}

	private Criteria createSelesaiSesuaiPanelCriteria(Session session, boolean order) {
		Criterion criterion1 = Restrictions.isNull("alurSop.setelahnya");
		criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya2"));
		criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya3"));
		criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya4"));
		criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya5"));
		criterion1 = Restrictions.or(criterion1, Restrictions.eq("selesai", true));

		Criteria criteria = createCriteria(session, DisposisiAlurSop.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
				.add(Restrictions.isNotNull("alurSop.sop"))
				.add(Restrictions.eq("alurSop.start", false))
				.add(Restrictions.or(Restrictions.eq("selesai", true), Restrictions.isNull("setelahnya")))
				.add(criterion1)
				.add(Restrictions.isNotNull("waktu"))
				.createAlias("disposisiSop", "disposisiSop")
				.add(getUserRestriction(tbmuser, "disposisiSop"))
				.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
						Restrictions.eq("disposisiSop.aktif", true)));

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		return criteria;
	}

	private Criteria createMenungguAktorSesuaiPanelCriteria(Session session, boolean order) {
		Criteria criteria = createCriteria(session, DisposisiAlurSop.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
				.add(Restrictions.isNotNull("alurSop.sop"))
				.add(Restrictions.isNull("diajukanOleh"))
				.add(Restrictions.isNull("mahasiswa"))
				.add(Restrictions.isNull("siswa"))
				.createAlias("disposisiSop", "disposisiSop")
				.add(getUserRestriction(tbmuser, "disposisiSop"))
				.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
						Restrictions.eq("disposisiSop.aktif", true)));

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		return criteria;
	}

	private DetailCriteriaProvider createMenungguSayaCriteriaProvider() {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				return createMenungguSayaSesuaiPanelCriteria(HibernateUtil.openSession(), order, false);
			}
		};
	}

	private DetailCriteriaProvider createSudahDisposisiCriteriaProvider() {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Criteria criteria = baseDisposisiCriteria()
						.add(Restrictions.eq("alurSop.start", false))
						.add(Restrictions.isNotNull("waktu"))
						.add(getUserRestriction(tbmuser, ""));
				if (order) {
					criteria.addOrder(Order.desc("id"));
				}
				return criteria;
			}
		};
	}

	private DetailCriteriaProvider createSelesaiCriteriaProvider() {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				return createSelesaiSesuaiPanelCriteria(HibernateUtil.openSession(), order);
			}
		};
	}

	private DetailCriteriaProvider createMenungguAktorCriteriaProvider() {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				return createMenungguAktorSesuaiPanelCriteria(HibernateUtil.openSession(), order);
			}
		};
	}

	private DetailCriteriaProvider createMendekatiDeadlineCriteriaProvider() {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				return createMenungguSayaSesuaiPanelCriteria(HibernateUtil.openSession(), order, true);
			}
		};
	}

	private DetailCriteriaProvider createTotalAntrianCriteriaProvider() {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Criteria criteria = baseDisposisiCriteria();
				Criterion menungguPemohon = getUserRestriction(tbmuser, "disposisiSop");
				Criterion menungguPetugas;
				try {
					menungguPetugas = getAktorRestriction(criteria);
				} catch (Exception e) {
					menungguPetugas = Restrictions.sqlRestriction("true");
				}
				criteria.add(Restrictions.or(menungguPemohon, menungguPetugas));
				criteria.add(Restrictions.or(Restrictions.isNull("selesai"), Restrictions.eq("selesai", false)));
				if (order) {
					criteria.addOrder(Order.desc("id"));
				}
				return criteria;
			}
		};
	}

	private DetailCriteriaProvider createAllDashboardCriteriaProvider() {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Criteria criteria = baseDisposisiCriteria();
				try {
					Criterion pengaju = getUserRestriction(tbmuser, "disposisiSop");
					Criterion petugas = getAktorRestriction(criteria);
					Criterion rootUser = getUserRestriction(tbmuser, "");
					criteria.add(Restrictions.or(pengaju, Restrictions.or(petugas, rootUser)));
				} catch (Exception e) {
					criteria.add(getUserRestriction(tbmuser, "disposisiSop"));
				}
				if (order) {
					criteria.addOrder(Order.desc("id"));
				}
				return criteria;
			}
		};
	}

	private DetailCriteriaProvider createDeadlineCriteriaProvider(final String mode) {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Criteria criteria = createAllDashboardCriteriaProvider().buildCriteria(false);
				criteria.add(Restrictions.isNotNull("waktuMaksimal"));
				Calendar cal = Calendar.getInstance();
				clearTime(cal);
				Date today = cal.getTime();
				cal.add(Calendar.DATE, 1);
				Date tomorrow = cal.getTime();
				cal.add(Calendar.DATE, 6);
				Date sevenDays = cal.getTime();
				if ("lewat".equals(mode)) {
					criteria.add(Restrictions.lt("waktuMaksimal", today));
				} else if ("hari_ini".equals(mode)) {
					criteria.add(Restrictions.ge("waktuMaksimal", today)).add(Restrictions.lt("waktuMaksimal", tomorrow));
				} else if ("minggu_ini".equals(mode)) {
					criteria.add(Restrictions.ge("waktuMaksimal", tomorrow)).add(Restrictions.lt("waktuMaksimal", sevenDays));
				} else if ("aman".equals(mode)) {
					criteria.add(Restrictions.ge("waktuMaksimal", sevenDays));
				}
				criteria.add(Restrictions.or(Restrictions.isNull("selesai"), Restrictions.eq("selesai", false)));
				if (order) {
					criteria.addOrder(Order.asc("waktuMaksimal"));
				}
				return criteria;
			}
		};
	}

	private DetailCriteriaProvider createDeadlineUrgentCriteriaProvider() {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Criteria criteria = createAllDashboardCriteriaProvider().buildCriteria(false);
				criteria.add(Restrictions.isNotNull("waktuMaksimal"));
				Calendar cal = Calendar.getInstance();
				clearTime(cal);
				Date today = cal.getTime();
				cal.add(Calendar.DATE, 1);
				Date tomorrow = cal.getTime();
				criteria.add(Restrictions.lt("waktuMaksimal", tomorrow));
				criteria.add(Restrictions.or(Restrictions.isNull("selesai"), Restrictions.eq("selesai", false)));
				if (order) {
					criteria.addOrder(Order.asc("waktuMaksimal"));
				}
				return criteria;
			}
		};
	}


	private DetailCriteriaProvider createNoDeadlineCriteriaProvider() {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Criteria criteria = createAllDashboardCriteriaProvider().buildCriteria(false);
				criteria.add(Restrictions.isNull("waktuMaksimal"));
				if (order) {
					criteria.addOrder(Order.desc("id"));
				}
				return criteria;
			}
		};
	}

	private DetailCriteriaProvider createNoAktorCriteriaProvider() {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Criteria criteria = createAllDashboardCriteriaProvider().buildCriteria(false);
				criteria.add(Restrictions.or(Restrictions.isNull("alurSop.petugas"), Restrictions.eq("alurSop.petugas", "")));
				if (order) {
					criteria.addOrder(Order.desc("id"));
				}
				return criteria;
			}
		};
	}

	private DetailCriteriaProvider createNoTahapCriteriaProvider() {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Criteria criteria = createAllDashboardCriteriaProvider().buildCriteria(false);
				criteria.add(Restrictions.or(Restrictions.isNull("alurSop.nama"), Restrictions.eq("alurSop.nama", "")));
				if (order) {
					criteria.addOrder(Order.desc("id"));
				}
				return criteria;
			}
		};
	}

	private DetailCriteriaProvider createNoCatatanCriteriaProvider() {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Criteria criteria = createAllDashboardCriteriaProvider().buildCriteria(false);
				criteria.add(Restrictions.or(Restrictions.isNull("keterangan"), Restrictions.eq("keterangan", "")));
				if (order) {
					criteria.addOrder(Order.desc("id"));
				}
				return criteria;
			}
		};
	}


	private DetailCriteriaProvider createSopCounterCriteriaProvider(final String sopName) {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Criteria criteria = createAllDashboardCriteriaProvider().buildCriteria(false);
				try {
					criteria.createAlias("disposisiSop.sop", "sopFilter", Criteria.LEFT_JOIN);
					criteria.add(Restrictions.eq("sopFilter.nama", sopName));
				} catch (Exception e) {
					criteria.add(Restrictions.sqlRestriction("true"));
				}
				if (order) {
					criteria.addOrder(Order.desc("id"));
				}
				return criteria;
			}
		};
	}

	private DetailCriteriaProvider createAktorCounterCriteriaProvider(final String aktorTahap) {
		return new DetailCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Criteria criteria = createAllDashboardCriteriaProvider().buildCriteria(false);
				String aktor = aktorTahap;
				String tahap = aktorTahap;
				int idx = aktorTahap == null ? -1 : aktorTahap.indexOf(" - ");
				if (idx > -1) {
					aktor = aktorTahap.substring(0, idx);
					tahap = aktorTahap.substring(idx + 3);
				}
				criteria.add(Restrictions.or(Restrictions.eq("alurSop.petugas", aktor), Restrictions.eq("alurSop.nama", tahap)));
				if (order) {
					criteria.addOrder(Order.desc("id"));
				}
				return criteria;
			}
		};
	}

	private Criteria baseDisposisiCriteria() {
		Session session = HibernateUtil.openSession();
		Criteria criteria = createCriteria(session, DisposisiAlurSop.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
				.add(Restrictions.isNotNull("alurSop.sop"))
				.createAlias("alurSop.aktorSop", "aktorSop", Criteria.LEFT_JOIN)
				.createAlias("disposisiSop", "disposisiSop")
				.createAlias("sebelumnya", "sebelumnya", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)));
		applyGlobalDisposisiFilter(criteria);
		return criteria;
	}

	private void printDebug(Exception e) {
		if ((debug || debuh) && e != null) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sop/helper/DasboardSop.java:2638");
		}
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
		return pch;
	}

	private void appendHtml(Component parent, String html) {
		org.zkoss.zul.Html h = new org.zkoss.zul.Html(html);
		h.setParent(parent);
	}

	private void appendEmptyState(Component parent, String message) {
		appendHtml(parent, "<div style='padding:18px; border-radius:14px; background:#f8fafc; border:1px dashed #cbd5e1;"
				+ "text-align:center; color:#64748b; font-size:12px;'>" + escapeHtml(message) + "</div>");
	}

	private int getMaxValue(int[] values) {
		int max = 0;
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

	private String escapeHtml(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}

	@SuppressWarnings("unused")
	private int getCountMendekatiDeadline() {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Criteria criteria = createMenungguSayaSesuaiPanelCriteria(session, false, true)
					.setProjection(Projections.rowCount());
			Object result = criteria.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			printDebug(e);
			return 0;
		} finally {
			closeSessionSafely(session);
		}
	}

	private void buildDashboardPanel(String title, final DashboardDataHandler dataHandler) throws Exception {
		final String panelTitle = title;
		final boolean panelIgnoreGlobalFilter = ignoreGlobalFilterForLegacyPanel;
		MyPortalchildren portalchildren = new MyPortalchildren();
		portalchildren.setParent(getCurrentDashboardPortalParent());
		portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");
		portalchildren.setStyle("padding:6px; box-sizing:border-box;");

		Panel panel = new ais.ui.util.MyPanelConfig();
		portalchildren.appendChild(panel);
		panel.setTitle(title);
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.addEventListener("onMove", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				MoveEvent moveEvent = (MoveEvent) arg0;
				System.out.println("left -> " + moveEvent.getLeft() + ", top -> " + moveEvent.getTop());
			}
		});
		panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:16px; overflow:hidden;"
				+ "background:#ffffff; box-shadow:0 12px 24px rgba(15,23,42,.07);");

		final Panelchildren panelchildren = new Panelchildren();
		panelchildren.setStyle("background:#ffffff; padding:10px;");
		panelchildren.setParent(panel);

		EventListener pengajuanBaruEventListener = new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(panelchildren);
				appendLegacyPanelExplanationV13(panelchildren, panelTitle);

				Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
				rowUtamapalingAwal.getGrid().setSclass("dtabel");

				Toolbar toolbar = new Toolbar();
				toolbar.setStyle("border:0; background:#f8fafc; border-radius:14px; padding:8px; margin-bottom:8px;"
						+ "box-shadow:inset 0 0 0 1px #e5e7eb;");
				toolbar.setParent(rowUtamapalingAwal);

				final Combobox sop = new Combobox();
				sop.setCols(5);
				sop.setStyle("border-radius:8px;");
				final Textbox cari = new Textbox();
				cari.setStyle("border-radius:8px;");
				cari.setValue(panelIgnoreGlobalFilter ? "" : (dashboardFilterKeyword == null ? "" : dashboardFilterKeyword));
				final MyDatebox mulai = new MyDatebox(panelIgnoreGlobalFilter ? null : dashboardFilterMulai);
				mulai.setStyle("border-radius:8px;");
				final MyDatebox sampai = new MyDatebox(panelIgnoreGlobalFilter ? null : dashboardFilterSampai);
				sampai.setStyle("border-radius:8px;");

				// Handler khusus untuk tombol tambahan di toolbar (misal: "Pengajuan Baru")
				dataHandler.onToolbarCreated(toolbar);

				new MyLabelAgakKecil("SOP:").setParent(toolbar);
				sop.setParent(toolbar);

				new MyLabelAgakKecil("Cari:").setParent(toolbar);
				cari.setCols(5);
				cari.setParent(toolbar);

				new MyLabelAgakKecil("Tgl:").setParent(toolbar);
				mulai.setCols(5);
				mulai.setParent(toolbar);
				new MyLabelAgakKecil("sd").setParent(toolbar);
				sampai.setCols(5);
				sampai.setParent(toolbar);

				MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig(dataHandler.getRefreshLabel(),
						dataHandler.getRefreshIcon());
				refresh.setTooltiptext("Refresh");
				refresh.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; padding:6px 12px;");
				refresh.setParent(toolbar);

				final DataCriteria dataCriteria = new DataCriteria() {
					@Override
					public Criteria initCriteria(boolean order) {
						Criteria criteria = dataHandler.buildCriteria(cari.getValue().trim(), mulai, sampai, sop, order);
						if (!panelIgnoreGlobalFilter) {
							applyGlobalSatkerFilter(criteria);
						}
						return criteria;
					}
				};

				Criteria sopCriteria = null;
				List<Sop> sops = new ArrayList<Sop>();
				try {
					sopCriteria = (Criteria) dataCriteria.initCriteria(false);
					sops = ConstantValues.simpleList(
							sopCriteria.setProjection(Projections.groupProperty("disposisiSop.sop.id")), Sop.class,
							false);
					Collections.sort(sops);
					sops.add(null);
					Common.insertComboItems(sop, "nama", "keterangan", sops);
				} catch (Exception e) {
					printDebug(e);
					sops.add(null);
					Common.insertComboItems(sop, "nama", "keterangan", sops);
				} finally {
					closeCriteriaSessionSafely(sopCriteria);
				}

				final Paging paging = new Paging();
				Row rowUtama = new Row();
				rowUtama.setParent(rowUtamapalingAwal.getParent());
				rowUtama.appendChild(paging);

				final Row rowUtamaData = new Row();
				rowUtamaData.setParent(rowUtamapalingAwal.getParent());

				final EventListener dataSearchDefault = new EventListener() {
					private EventListener getThis() {
						return this;
					}

					@Override
					public void onEvent(Event event) throws Exception {
						Common.clear(rowUtamaData);

						Criteria countCriteria = null;
						try {
							countCriteria = (Criteria) dataCriteria.initCriteria(false);
							Common.initPaging5(countCriteria, paging);
						} finally {
							closeCriteriaSessionSafely(countCriteria);
						}

						Criteria listCriteria = null;
						List<DisposisiAlurSop> disposisiSops = new ArrayList<DisposisiAlurSop>();
						try {
							listCriteria = (Criteria) dataCriteria.initCriteria(true);
							disposisiSops = listCriteria
									.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage())))
									.setMaxResults(5).list();

							Grid grid = new Grid();
							grid.setSclass("dtabel ftabel");
							grid.setParent(rowUtamaData);
							grid.setStyle("min-height:100px; border:0; background:transparent; border-radius:14px;");
							grid.setMold("paging");
							grid.setPageSize(10);
							grid.getPagingChild().setMold("os");

							Rows rows = new Rows();
							rows.setParent(grid);

							dataHandler.renderData(rows, disposisiSops, getThis());
						} finally {
							disposisiSops = null;
							closeCriteriaSessionSafely(listCriteria);
						}
					}
				};

				Common.initPaging5(paging, dataSearchDefault);
				dataSearchDefault.onEvent(arg0);

				cari.addEventListener("onOK", dataSearchDefault);
				Common.selectComboItem(sop, null);
				sop.setReadonly(true);
				sop.addEventListener("onChange", dataSearchDefault);
				refresh.addEventListener("onClick", dataSearchDefault);
				// Tombol SETUJUI SEMUA ANTRIAN (select all) - hanya panel "menunggu disposisi Anda"
				// dan bila pengguna punya hak akses (konfigurasi hak_akses_setujui_massal_sop).
				if (dataHandler instanceof BaseSearchDataHandler
						&& ((BaseSearchDataHandler) dataHandler).supportsBulkApprove() && bolehSetujuiMassal()) {
					MyToolbarbuttonConfig btnMassal = new MyToolbarbuttonConfig("Setujui Massal", "/img/svg/check2.svg");
					btnMassal.setTooltiptext("Setujui SEMUA pengajuan yang menunggu disposisi Anda sekaligus (select all)");
					btnMassal.setStyle("font-weight:bold; color:#ffffff; background:#16a34a; border-radius:10px; padding:6px 12px; margin-left:6px;");
					btnMassal.setParent(toolbar);
					btnMassal.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event evBulk) throws Exception {
							dialogSetujuiMassal(dataSearchDefault);
						}
					});
				}
			}
		};

		pengajuanBaruEventListener.onEvent(null);
	}

	// =========================================================================================
	// IMPLEMENTASI SETIAP PANEL DENGAN HANDLER
	// =========================================================================================

	private void buildPengajuanBaru() throws Exception {
		buildDashboardPanel("Pengajuan Anda yang sedang diproses", new DashboardDataHandler() {
			@Override
			public String getRefreshLabel() {
				return "Refresh";
			}

			@Override
			public String getRefreshIcon() {
				return "/img/svg/refresh.svg";
			}

			@Override
			public void onToolbarCreated(Toolbar toolbar) {
				MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Pengajuan Baru",
						"/img/svg/form-one.svg");
				toolbarbutton.setStyle("font-weight:bold; color:#0f172a; background:#e0f2fe; border-radius:10px; padding:6px 12px;");
				toolbarbutton.setParent(toolbar);
				toolbarbutton.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						DisposisiSop disposisi = new DisposisiSop();
						DisposisiSopAction.onAddExternal(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.clear(DasboardSop.this);
								DasboardSop.this.init();
							}
						}, disposisi, true);
					}
				});
			}

			@Override
			public Criteria buildCriteria(String search, MyDatebox mulai, MyDatebox sampai, Combobox sop,
					boolean order) {
				Session session = HibernateUtil.openSession();
				Criteria criteria = createCriteria(session, DisposisiAlurSop.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(getDateRestriction(sampai, "<=")).add(getDateRestriction(mulai, ">="))
						.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
						.add(Restrictions.isNotNull("alurSop.sop")).add(Restrictions.isNull("diajukanOleh"))
						.add(Restrictions.isNull("mahasiswa")).add(Restrictions.isNull("siswa"))
						.createAlias("disposisiSop", "disposisiSop").add(getSearchRestriction(search))
						.add(getUserRestriction(tbmuser, "disposisiSop"))
						.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
								Restrictions.eq("disposisiSop.aktif", true)));

				applySopFilter(criteria, sop);
				if (order)
					criteria.addOrder(Order.desc("id"));
				return criteria;
			}

			@Override
			public void renderData(Rows rows, List<DisposisiAlurSop> data, final EventListener listener)
					throws Exception {
				for (DisposisiAlurSop disposisiAlurSop : data) {
					if (disposisiAlurSop.getAktif()) {
						Row row = new Row();
						row.setParent(rows);
						A a = new A();
						a.setParent(row);
						Vbox vbox = new Vbox();
						a.appendChild(vbox);

						renderKodePengajuanMultiSumber(vbox, disposisiAlurSop);

						vbox.appendChild(new MyLabelBoldAja(disposisiAlurSop.getDisposisiSop().getSop().getNama()
								+ " (pengajuan "
								+ Common.dateFormat.get().format(disposisiAlurSop.getDisposisiSop().getWaktu()) + ")"));

						try {
							if (disposisiAlurSop.getSebelumnya() != null
									&& disposisiAlurSop.getSebelumnya().getAlurSop() != null) {
								vbox.appendChild(
										new MyLabelAgakKecil(disposisiAlurSop.getSebelumnya().getAlurSop().getAktor()
												+ " - " + disposisiAlurSop.getSebelumnya().getAlurSop().getNama()));
								vbox.appendChild(new MyLabelKecil(
										"Catatan : " + disposisiAlurSop.getSebelumnya().getKeterangan()));
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:2981");
						}

						vbox.appendChild(new MyLabelAgakKecil(disposisiAlurSop.getAlurSop().getAktor() + " - "
								+ disposisiAlurSop.getAlurSop().getNama()));
						attachKlikProses(a, disposisiAlurSop.getDisposisiSop().getId(), listener);
					}
				}
			}
		});
	}

	private void buildPengajuanBaruDisposisi() throws Exception {
		buildDashboardPanel("Pengajuan yang baru saja Anda disposisi", new BaseSearchDataHandler() {
			@Override
			public Criteria buildCriteria(String search, MyDatebox mulai, MyDatebox sampai, Combobox sop,
					boolean order) {
				Session session = HibernateUtil.openSession();
				Criteria criteria = createCriteria(session, DisposisiAlurSop.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(getDateRestriction(sampai, "<=")).add(getDateRestriction(mulai, ">="))
						.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
						.add(Restrictions.isNotNull("alurSop.sop")).add(Restrictions.eq("alurSop.start", false))
						.add(Restrictions.isNotNull("waktu")).createAlias("disposisiSop", "disposisiSop")
						.add(getSearchRestriction(search)).add(getUserRestriction(tbmuser, "")) // Root entity user
																								// restriction
						.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
								Restrictions.eq("disposisiSop.aktif", true)));

				applySopFilter(criteria, sop);
				if (order)
					criteria.addOrder(Order.desc("id"));
				return criteria;
			}

			@Override
			public void renderData(Rows rows, List<DisposisiAlurSop> data, final EventListener listener)
					throws Exception {
				for (DisposisiAlurSop disposisiAlurSop : data) {
					if (disposisiAlurSop.getAktif()) {
						Row row = new Row();
						row.setParent(rows);
						A a = new A();
						a.setParent(row);
						Vbox vbox = new Vbox();
						a.appendChild(vbox);

						// Kode pengajuan dicari dari beberapa sumber agar PASTI tampil
						// (mis. No.0098/UM/ADM/VI/2026, No.0257/INV-VDR/YTB/VI/2026).
						renderKodePengajuanMultiSumber(vbox, disposisiAlurSop);
						vbox.appendChild(new MyLabelBoldAja(disposisiAlurSop.getDisposisiSop().getSop().getNama()
								+ " (selesai " + Common.dateFormat.get().format(disposisiAlurSop.getWaktu()) + ")"));
						vbox.appendChild(new MyLabelAgakKecil(disposisiAlurSop.getAlurSop().getAktor() + " - "
								+ disposisiAlurSop.getAlurSop().getNama()));

						renderStatusDisposisiTerakhir(vbox, disposisiAlurSop);

						vbox.appendChild(new MyLabelKecil("Catatan : " + disposisiAlurSop.getKeterangan()
								+ (disposisiAlurSop.getDisposisiSop() == null
										|| disposisiAlurSop.getDisposisiSop().getKeterangan().isEmpty() ? ""
												: " (" + disposisiAlurSop.getDisposisiSop().getKeterangan() + ")")));

						attachKlikProses(a, disposisiAlurSop.getDisposisiSop().getId(), listener);
					}
				}
			}
		});
	}

	private void buildPengajuanSelesai() throws Exception {
		buildDashboardPanel("Pengajuan Anda yang telah selesai", new BaseSearchDataHandler() {
			@Override
			public Criteria buildCriteria(String search, MyDatebox mulai, MyDatebox sampai, Combobox sop,
					boolean order) {
				Criterion criterion1 = Restrictions.isNull("alurSop.setelahnya");
				criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya2"));
				criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya3"));
				criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya4"));
				criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya5"));
				criterion1 = Restrictions.or(criterion1, Restrictions.eq("selesai", true));

				Session session = HibernateUtil.openSession();
				Criteria criteria = createCriteria(session, DisposisiAlurSop.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(getDateRestriction(sampai, "<=")).add(getDateRestriction(mulai, ">="))
						.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
						.add(Restrictions.isNotNull("alurSop.sop")).add(Restrictions.eq("alurSop.start", false))
						.add(Restrictions.or(Restrictions.eq("selesai", true), Restrictions.isNull("setelahnya")))
						.add(criterion1).add(Restrictions.isNotNull("waktu"))
						.createAlias("disposisiSop", "disposisiSop").add(getSearchRestriction(search))
						.add(getUserRestriction(tbmuser, "disposisiSop"))
						.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
								Restrictions.eq("disposisiSop.aktif", true)));

				applySopFilter(criteria, sop);
				if (order)
					criteria.addOrder(Order.desc("id"));
				return criteria;
			}

			@Override
			public void renderData(Rows rows, List<DisposisiAlurSop> data, final EventListener listener)
					throws Exception {
				// Rendering logikanya identik dengan pengajuanBaruDisposisi
				for (DisposisiAlurSop disposisiAlurSop : data) {
					if (disposisiAlurSop.getAktif()) {
						Row row = new Row();
						row.setParent(rows);
						A a = new A();
						a.setParent(row);
						Vbox vbox = new Vbox();
						a.appendChild(vbox);

						// Kode pengajuan dicari dari beberapa sumber agar PASTI tampil
						// (mis. No.0098/UM/ADM/VI/2026, No.0257/INV-VDR/YTB/VI/2026).
						renderKodePengajuanMultiSumber(vbox, disposisiAlurSop);
						vbox.appendChild(new MyLabelBoldAja(disposisiAlurSop.getDisposisiSop().getSop().getNama()
								+ " (selesai " + Common.dateFormat.get().format(disposisiAlurSop.getWaktu()) + ")"));
						vbox.appendChild(new MyLabelAgakKecil(disposisiAlurSop.getAlurSop().getAktor() + " - "
								+ disposisiAlurSop.getAlurSop().getNama()));

						renderStatusDisposisiTerakhir(vbox, disposisiAlurSop);

						vbox.appendChild(new MyLabelKecil("Catatan : " + disposisiAlurSop.getKeterangan()
								+ (disposisiAlurSop.getDisposisiSop() == null
										|| disposisiAlurSop.getDisposisiSop().getKeterangan().isEmpty() ? ""
												: " (" + disposisiAlurSop.getDisposisiSop().getKeterangan() + ")")));

						attachKlikProses(a, disposisiAlurSop.getDisposisiSop().getId(), listener);
					}
				}
			}
		});
	}

	private void buildMenungguDisposisi() throws Exception {
		buildDashboardPanel("Proses yang sedang menunggu disposisi Anda", new BaseSearchDataHandler() {
			@Override
			public boolean supportsBulkApprove() {
				return true;
			}

			@Override
			public Criteria buildCriteria(String search, MyDatebox mulai, MyDatebox sampai, Combobox sop,
					boolean order) {
				Session session = HibernateUtil.openSession();
				Criteria criteria = createCriteria(session, DisposisiAlurSop.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.isNull("selesai"), Restrictions.eq("selesai", false)))
						.add(getDateRestriction(sampai, "<=")).add(getDateRestriction(mulai, ">="))
						.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
						.add(Restrictions.isNotNull("alurSop.sop"))
						.createAlias("alurSop.aktorSop", "aktorSop", Criteria.LEFT_JOIN)
						.createAlias("disposisiSop", "disposisiSop")
						.createAlias("sebelumnya", "sebelumnya", Criteria.LEFT_JOIN);

				criteria.add(getAktorRestriction(criteria))
						.add(Restrictions.and(Restrictions.isNull("diajukanOleh"),
								Restrictions.and(Restrictions.isNull("siswa"), Restrictions.isNull("mahasiswa"))))
						.add(Restrictions.isNull("setelahnya")).add(Restrictions.isNotNull("sebelumnya"))
						.add(getSearchRestriction(search))
						.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
								Restrictions.eq("disposisiSop.aktif", true)));

				applySopFilter(criteria, sop);
				if (order)
					criteria.addOrder(Order.desc("id"));
				return criteria;
			}

			@Override
			public void renderData(Rows rows, List<DisposisiAlurSop> data, final EventListener listener)
					throws Exception {
				appendPanelModeInfo(rows, "Semua Proses yang Menunggu Disposisi Anda",
						"Menampilkan seluruh antrian aktif yang saat ini berada pada petugas/tahap Anda, baik yang memiliki batas waktu maupun yang belum memiliki batas waktu.",
						"#eff6ff", "#bfdbfe", "#1e40af");
				for (DisposisiAlurSop disposisiAlurSop : data) {
					if (disposisiAlurSop.getAktif()) {
						Row row = new Row();
						row.setParent(rows);
						A a = new A();
						a.setParent(row);
						Vbox vbox = new Vbox();
						a.appendChild(vbox);

						String oleh = getOlehName(disposisiAlurSop.getDisposisiSop().getDiajukanOleh(),
								disposisiAlurSop.getDisposisiSop().getSiswa(),
								disposisiAlurSop.getDisposisiSop().getMahasiswa());

						renderKodePengajuanMultiSumber(vbox, disposisiAlurSop);
						vbox.appendChild(new MyLabelBoldAja(
								disposisiAlurSop.getDisposisiSop().getSop().getNama() + " (pengajuan "
										+ Common.dateFormat.get().format(disposisiAlurSop.getDisposisiSop().getWaktu()) + " "
										+ oleh + ")"));

						renderStatusDisposisiTerakhir(vbox, disposisiAlurSop);

						vbox.appendChild(new MyLabelAgakKecil(disposisiAlurSop.getAlurSop().getAktor() + " - "
								+ disposisiAlurSop.getAlurSop().getNama()));

						try {
							if (disposisiAlurSop.getSebelumnya() != null) {
								vbox.appendChild(
										new MyLabelAgakKecil(disposisiAlurSop.getSebelumnya().getAlurSop().getAktor()
												+ " - " + disposisiAlurSop.getSebelumnya().getAlurSop().getNama()));
								vbox.appendChild(new MyLabelKecil(
										"Catatan : " + disposisiAlurSop.getSebelumnya().getKeterangan()));
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:3189");
						}

						if (disposisiAlurSop.getWaktuMaksimal() != null) {
							vbox.appendChild(new MyLabelAgakKecilBoldMerah(
									"Batas Waktu:" + Common.dateFormat51.get().format(disposisiAlurSop.getWaktuMaksimal())));
						}

						attachKlikProses(a, disposisiAlurSop.getDisposisiSop().getId(), listener);
					}
				}
			}
		});
	}

	private void buildMenungguAktorDisposisi() throws Exception {
		buildDashboardPanel("Menunggu disposisi", new BaseSearchDataHandler() {
			@Override
			public Criteria buildCriteria(String search, MyDatebox mulai, MyDatebox sampai, Combobox sop,
					boolean order) {
				Session session = HibernateUtil.openSession();
				Criteria criteria = createCriteria(session, DisposisiAlurSop.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(getDateRestriction(sampai, "<=")).add(getDateRestriction(mulai, ">="))
						.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
						.add(Restrictions.isNotNull("alurSop.sop")).add(Restrictions.isNull("diajukanOleh"))
						.add(Restrictions.isNull("mahasiswa")).add(Restrictions.isNull("siswa"))
						.createAlias("disposisiSop", "disposisiSop").add(getSearchRestriction(search))
						.add(getUserRestriction(tbmuser, "disposisiSop"))
						.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
								Restrictions.eq("disposisiSop.aktif", true)));

				applySopFilter(criteria, sop);
				if (order)
					criteria.addOrder(Order.desc("id"));
				return criteria;
			}

			@Override
			public void renderData(Rows rows, List<DisposisiAlurSop> data, final EventListener listener)
					throws Exception {
				Map<String, List<DisposisiAlurSop>> aktors = new HashMap<String, List<DisposisiAlurSop>>();
				for (DisposisiAlurSop disposisiAlurSop : data) {
					if (disposisiAlurSop.getAktif()) {
						try {
							List<DisposisiAlurSop> a = aktors.get(disposisiAlurSop.getAlurSop().getAktor());
							if (a == null) {
								a = new ArrayList<DisposisiAlurSop>();
								aktors.put(disposisiAlurSop.getAlurSop().getAktor(), a);
							}
							a.add(disposisiAlurSop);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:3240");
						}
					}
				}

				for (String aktor : aktors.keySet()) {
					Row row = new Row();
					row.setParent(rows);

					Vbox vbox1 = new Vbox();
					row.appendChild(vbox1);
					vbox1.appendChild(new MyLabelBold(aktor));

					for (DisposisiAlurSop disposisiAlurSop : aktors.get(aktor)) {
						String oleh = getOlehName(disposisiAlurSop.getDisposisiSop().getDiajukanOleh(),
								disposisiAlurSop.getDisposisiSop().getSiswa(),
								disposisiAlurSop.getDisposisiSop().getMahasiswa());

						A a = new A();
						a.setParent(vbox1);
						Vbox vbox = new Vbox();
						a.appendChild(vbox);

						renderKodePengajuanMultiSumber(vbox, disposisiAlurSop);
						vbox.appendChild(new MyLabelAgakKecil(
								disposisiAlurSop.getDisposisiSop().getSop().getNama() + " (pengajuan "
										+ Common.dateFormat.get().format(disposisiAlurSop.getDisposisiSop().getWaktu()) + " "
										+ oleh + ")"));

						vbox.appendChild(new MyLabelAgakKecil(disposisiAlurSop.getAlurSop().getNama()));

						if (disposisiAlurSop.getWaktuMaksimal() != null) {
							vbox.appendChild(new MyLabelAgakKecilBoldMerah(
									"Batas Waktu:" + Common.dateFormat51.get().format(disposisiAlurSop.getWaktuMaksimal())));
						}

						renderStatusDisposisiTerakhir(vbox, disposisiAlurSop);
						attachKlikProses(a, disposisiAlurSop.getDisposisiSop().getId(), listener);
					}
				}
			}
		});
	}

	// =========================================================================================
	// HELPER METHODS (Queries, Utilities, Formatting)
	// =========================================================================================

	@SuppressWarnings("unchecked")
	private void renderPropertiJson(Vbox vbox, String propertiJson) {
		String kode = ambilKodeDariProperti(propertiJson);
		if (kode != null && !kode.trim().isEmpty()) {
			vbox.appendChild(new MyLabelBold(kode));
		}
	}

	/**
	 * Ambil kode/nomor pengajuan dari properti JSON DisposisiSop/DisposisiAlurSop secara ROBUST.
	 *
	 * <p>Versi lama hanya membaca KEY PERTAMA lalu field {@code kode} di dalamnya, sehingga kode
	 * pengajuan (mis. {@code No.0098/UM/ADM/VI/2026}, {@code No.0257/INV-VDR/YTB/VI/2026}) tidak
	 * tampil bila struktur properti berbeda antar jenis SOP atau key pertama kebetulan tidak
	 * memuat kode. Versi ini memindai SEMUA key (nilai berupa objek transaksi) dan juga level
	 * atas, lalu mengembalikan field kode/nomor pertama yang tidak kosong.</p>
	 *
	 * @return kode pengajuan, atau null bila tidak ditemukan.
	 */
	private String ambilKodeDariProperti(String propertiJson) {
		try {
			if (propertiJson == null || propertiJson.trim().isEmpty()) {
				return null;
			}
			JSONObject root = new JSONObject(propertiJson);
			String[] fields = new String[] { "kode", "nomor", "nomorSurat", "noSurat", "no_surat", "kodeTransaksi" };
			// 1. Level atas (properti DisposisiAlurSop kadang langsung berisi kode).
			for (String f : fields) {
				if (!root.isNull(f)) {
					String k = String.valueOf(root.get(f));
					if (k != null && !k.trim().isEmpty()) {
						return k.trim();
					}
				}
			}
			// 2. Pindai SETIAP key; bila nilainya objek transaksi, cek field kode/nomor di dalamnya.
			Iterator<String> keys = root.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				Object val = root.opt(key);
				if (val instanceof JSONObject) {
					JSONObject obj = (JSONObject) val;
					for (String f : fields) {
						if (!obj.isNull(f)) {
							String k = String.valueOf(obj.get(f));
							if (k != null && !k.trim().isEmpty()) {
								return k.trim();
							}
						}
					}
				}
			}
		} catch (Exception e) {
			printDebug(e);
		}
		return null;
	}

	/**
	 * Tampilkan kode pengajuan untuk panel "Pengajuan yang baru saja Anda disposisi".
	 *
	 * <p>Sebelumnya kode tidak muncul di panel ini karena hanya membaca properti DisposisiSop
	 * (yang bisa kosong untuk data tertentu). Di sini kode dicari dari BEBERAPA sumber agar
	 * pasti tampil: properti DisposisiSop → properti DisposisiAlurSop langkah ini → properti
	 * langkah START (disposisiStart, yang selalu diisi kode saat pengajuan dibuat).</p>
	 */
	private void renderKodePengajuanMultiSumber(Vbox vbox, DisposisiAlurSop disposisiAlurSop) {
		try {
			if (disposisiAlurSop == null) {
				return;
			}
			String kode = null;
			if (disposisiAlurSop.getDisposisiSop() != null) {
				kode = ambilKodeDariProperti(disposisiAlurSop.getDisposisiSop().getProperti());
			}
			if (kode == null || kode.trim().isEmpty()) {
				kode = ambilKodeDariProperti(disposisiAlurSop.getProperti());
			}
			if ((kode == null || kode.trim().isEmpty()) && disposisiAlurSop.getDisposisiSop() != null
					&& disposisiAlurSop.getDisposisiSop().getDisposisiStart() != null) {
				kode = ambilKodeDariProperti(disposisiAlurSop.getDisposisiSop().getDisposisiStart().getProperti());
			}
			if (kode != null && !kode.trim().isEmpty()) {
				vbox.appendChild(new MyLabelBold(kode));
			}
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void renderStatusDisposisiTerakhir(Vbox vbox, DisposisiAlurSop disposisiAlurSop) {
		Session session = null;
		try {
			if (disposisiAlurSop == null || disposisiAlurSop.getDisposisiSop() == null) {
				return;
			}
			session = HibernateUtil.openSession();
			DisposisiAlurSop disposisiTerakhir = (DisposisiAlurSop) createCriteria(session, DisposisiAlurSop.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.isNotNull("alurSop"))
					.add(Restrictions.eq("disposisiSop", disposisiAlurSop.getDisposisiSop()))
					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

			if (disposisiTerakhir != null && !disposisiTerakhir.getId().equals(disposisiAlurSop.getId())) {
				if (disposisiTerakhir.getAlurSop() != null && disposisiTerakhir.getAlurSop().getPenolakanAdaDiSini()
						&& disposisiTerakhir.getSebelumnya() != null
						&& disposisiTerakhir.getSebelumnya().getAlurSop() != null) {
					String infototal = "Ditolak : " + disposisiTerakhir.getSebelumnya().getAlurSop().getAktor() + " - "
							+ disposisiTerakhir.getSebelumnya().getAlurSop().getNama();
					infototal += " " + getOlehName(disposisiTerakhir.getSebelumnya().getDiajukanOleh(),
							disposisiTerakhir.getSebelumnya().getSiswa(), disposisiTerakhir.getSebelumnya().getMahasiswa());
					vbox.appendChild(new MyLabelAgakKecilBoldMerah(infototal.trim()));

				} else if (disposisiTerakhir.getDiajukanOleh() == null && disposisiTerakhir.getSiswa() == null
						&& disposisiTerakhir.getMahasiswa() == null) {
					vbox.appendChild(new MyLabelAgakKecilBoldBiru("Menunggu : " + disposisiTerakhir.getAlurSop().getAktor()
							+ " - " + disposisiTerakhir.getAlurSop().getNama()));

				} else {
					String olehInfo = getOlehName(disposisiTerakhir.getDiajukanOleh(), disposisiTerakhir.getSiswa(),
							disposisiTerakhir.getMahasiswa());
					try {
						vbox.appendChild(new MyLabelAgakKecilBoldHijau(
								"Proses terakhir "
										+ (disposisiTerakhir.getSetelahnya() == null
												|| disposisiTerakhir.getSetelahnya().getAlurSop() == null ? ""
														: "\"" + disposisiTerakhir.getSetelahnya().getAlurSop()
																.getNama() + "\"")
										+ " oleh : " + olehInfo + "(" + disposisiTerakhir.getAlurSop().getAktor()
										+ ") - " + disposisiTerakhir.getAlurSop().getNama()));
					} catch (Exception e) {
						printDebug(e);
					}
				}
			}
		} catch (Exception e) {
			printDebug(e);
		} finally {
			closeSessionSafely(session);
		}
	}

	private void attachKlikProses(A a, final Long disposisiSopId, final EventListener listener) {
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				TampilanAlurSopAction.prosess(disposisiSopId, tabs, tabpanels, false, menu, listener);
			}
		});
	}

	private boolean bolehSetujuiMassal() {
		try {
			return Common.bolehUploadDataKonfigurasi("hak_akses_setujui_massal_sop", "*");
		} catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private int[] jalankanSetujuiMassalMenunggu(java.util.Set<Long> pilih, String keterangan) {
		String ket = keterangan == null ? "" : keterangan;
		// Pastikan pengguna aktif tersedia (defensif: tbmuser bisa null pada sebagian jalur render).
		if (tbmuser == null) {
			tbmuser = Common.getCurrentUser();
		}
		if (tbmuser == null || tbmuser.getUserId() == null) {
			return new int[] { 0, 0, 0 };
		}
		int lewat = 0, ok = 0, gagal = 0;
		java.util.List<Object[]> tugas = new java.util.ArrayList<Object[]>();
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			java.util.List pendingList;
			try {
				pendingList = ais.action.master.sop.helper.PengajuanAndaSopUtil
					.criteriaMenungguSaya(session, tbmuser, false).list();
			} catch (Exception e) {
				pendingList = new java.util.ArrayList();
			}
			for (Object o : pendingList) {
				DisposisiAlurSop step = (DisposisiAlurSop) o;
				if (step == null || step.getDisposisiSop() == null || step.getAlurSop() == null) {
					continue;
				}
				Long dsId = step.getDisposisiSop().getId();
				if (dsId == null) {
					continue;
				}
				if (pilih != null && !pilih.contains(dsId)) {
					continue;
				}
				AlurSop a = step.getAlurSop();
				if (Boolean.TRUE.equals(a.getCatatanWajibDiisi()) && ket.trim().isEmpty()) {
					lewat++;
					continue;
				}
				java.util.List<Long> rute = new java.util.ArrayList<Long>();
				java.util.List opsi = null;
				try {
					opsi = a.ambilAlurSetelahnya();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:3490");
				}
				boolean ruteOpsional = Boolean.TRUE.equals(a.getAlurSetelahnyaTidakWajib());
				boolean berupaPilihan = Boolean.TRUE.equals(a.getAlurSetelahnyaBerupaPilihan());
				int nOpsi = (opsi == null ? 0 : opsi.size());
				if (nOpsi > 0) {
					if (!ruteOpsional && (nOpsi > 1 || berupaPilihan)) {
						lewat++;
						continue;
					}
					for (Object oo : opsi) {
						AlurSop na = (AlurSop) oo;
						if (na != null && na.getId() != null) {
							rute.add(na.getId());
						}
					}
				}
				tugas.add(new Object[] { dsId, step.getId(), a.getId(), rute });
			}
		} catch (Exception e) {
			printDebug(e);
		} finally {
			closeSessionSafely(session);
		}
		for (Object[] t : tugas) {
			try {
				ais.action.master.sop.helper.ProsesDisposisiSopService.Hasil h = ais.action.master.sop.helper.ProsesDisposisiSopService
					.prosesLangkah(tbmuser, (Long) t[0], (Long) t[1], (Long) t[2], tbmuser.getUserId(),
						new java.util.Date(), null, ket, true, false, (java.util.List<Long>) t[3]);
				if (h != null && h.ok) {
					ok++;
				} else {
					gagal++;
				}
			} catch (Exception e) {
				gagal++;
			}
		}
		return new int[] { ok, lewat, gagal };
	}

	private void dialogSetujuiMassal(final EventListener refreshAfter) throws Exception {
		if (tbmuser == null) {
			tbmuser = Common.getCurrentUser();
		}
		// Muat antrian + hitung RUTE PERSETUJUAN per item (menuju "Setujui dan Selesai" / jalur setujui,
		// JANGAN jalur revisi/kembali-ke-pengaju). itemData: {dsId, stepId, alurId, label, rute(List<Long>)}
		final java.util.List<Object[]> itemData = new java.util.ArrayList<Object[]>();
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			java.util.List pending = new java.util.ArrayList();
			try {
				pending = ais.action.master.sop.helper.PengajuanAndaSopUtil
					.criteriaMenungguSaya(session, tbmuser, false).addOrder(Order.desc("id")).list();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:3545");
			}
			for (Object o : pending) {
				DisposisiAlurSop step = (DisposisiAlurSop) o;
				if (step == null || step.getDisposisiSop() == null || step.getAlurSop() == null) {
					continue;
				}
				Long dsId = step.getDisposisiSop().getId();
				if (dsId == null) {
					continue;
				}
				AlurSop a = step.getAlurSop();
				String kode = "";
				try { kode = step.getKode() == null ? "" : step.getKode(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:3558"); }
				String sopNama = "";
				try { sopNama = step.getDisposisiSop().getSop() == null ? "" : step.getDisposisiSop().getSop().getNama(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:3560"); }
				String tahap = "";
				try { tahap = a.getAktor() + " - " + a.getNama(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:3562"); }
				String label = (kode.isEmpty() ? "" : kode + " - ") + sopNama + (tahap.trim().isEmpty() ? "" : "  [" + tahap + "]");
				java.util.List<Long> rute = new java.util.ArrayList<Long>();
				try {
					java.util.List op = a.ambilAlurSetelahnya();
					java.util.List<Long> setuju = new java.util.ArrayList<Long>();
					java.util.List<Long> baik = new java.util.ArrayList<Long>();
					if (op != null) {
						for (Object oo : op) {
							AlurSop na = (AlurSop) oo;
							if (na == null || na.getId() == null) {
								continue;
							}
							// Rute MAJU untuk persetujuan: BUKAN revisi (kembali ke pengaju) dan BUKAN penolakan.
							boolean revisi = Boolean.TRUE.equals(na.getKembaliKePengaju());
							boolean tolak = Boolean.TRUE.equals(na.getPenolakanAdaDiSini());
							boolean maju = !revisi && !tolak;
							// setuju HANYA rute maju yang bertanda persetujuan (rute revisi kadang salah di-flag setujui).
							if (maju && Boolean.TRUE.equals(a.ambilAlurSetujui(na))) {
								setuju.add(na.getId());
							}
							if (maju) {
								baik.add(na.getId());
							}
						}
					}
					// Langkah "Setujui dan Selesai" (jikaProsesDisetujuiMakaSelesai) => finalisasi TANPA rute.
					// Selain itu: utamakan rute ber-tanda SETUJUI; kalau tak ada pakai rute MAJU (bukan revisi/penolakan).
					if (!Boolean.TRUE.equals(a.getJikaProsesDisetujuiMakaSelesai())) {
						rute = !setuju.isEmpty() ? setuju : baik;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:3593");
				}
				itemData.add(new Object[] { dsId, step.getId(), a.getId(), label, rute });
			}
		} catch (Exception e) {
			printDebug(e);
		} finally {
			closeSessionSafely(session);
		}
		if (itemData.isEmpty()) {
			ais.ui.util.MyMessageboxConfig.show(
				"Mohon maaf, saat ini tidak terdapat antrian yang menunggu disposisi Bapak/Ibu. Silakan periksa kembali pada lain waktu apabila terdapat pengajuan baru.",
				"Setujui Massal", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
			return;
		}
		final org.zkoss.zul.Window win = new org.zkoss.zul.Window();
		win.setTitle("Setujui Massal - Pilih Antrian (" + itemData.size() + ")");
		win.setWidth("600px");
		win.setBorder("normal");
		win.setClosable(true);
		win.setStyle("border-radius:14px; box-shadow:0 20px 45px rgba(15,23,42,.18);");
		win.setPage(getPage());
		Vbox box = new Vbox();
		box.setStyle("padding:16px; gap:10px;");
		box.setWidth("100%");
		box.setParent(win);
		org.zkoss.zul.Div infoBox = new org.zkoss.zul.Div();
		infoBox.setStyle("background:#eff6ff; border:1px solid #bfdbfe; border-radius:10px; padding:10px 12px;");
		infoBox.setParent(box);
		MyLabelKecil infoLbl = new MyLabelKecil("Centang antrian yang ingin Anda setujui, lalu klik Setujui Terpilih. Semua yang dicentang akan diproses menuju SETUJUI DAN SELESAI (mengikuti alur persetujuan), bukan Revisi.");
		infoLbl.setMultiline(true);
		infoLbl.setStyle("color:#1e40af; font-size:12px; line-height:1.5;");
		infoLbl.setParent(infoBox);
		final java.util.List<org.zkoss.zul.Checkbox> checks = new java.util.ArrayList<org.zkoss.zul.Checkbox>();
		final java.util.List<org.zkoss.zul.Hbox> rowsUi = new java.util.ArrayList<org.zkoss.zul.Hbox>();
		final java.util.List<String> labelsLower = new java.util.ArrayList<String>();
		final java.util.List<Object[]> states = new java.util.ArrayList<Object[]>();
		final MyLabelAgakKecil lblTerpilih = new MyLabelAgakKecil("");
		org.zkoss.zul.Hbox cariRow = new org.zkoss.zul.Hbox();
		cariRow.setWidth("100%");
		cariRow.setAlign("middle");
		cariRow.setSpacing("8px");
		cariRow.setStyle("background:#f8fafc; border:1px solid #eef2f7; border-radius:10px; padding:8px 10px; box-sizing:border-box;");
		cariRow.setParent(box);
		new MyLabelAgakKecil("Cari:").setParent(cariRow);
		final Textbox cari = new Textbox();
		cari.setWidth("300px");
		cari.setTooltiptext("Cari antrian: kode / nama SOP / tahap");
		cari.setStyle("border-radius:8px;");
		cari.setParent(cariRow);
		org.zkoss.zul.Div cariSpacer = new org.zkoss.zul.Div();
		cariSpacer.setHflex("1");
		cariSpacer.setParent(cariRow);
		final org.zkoss.zul.Checkbox pilihSemua = new org.zkoss.zul.Checkbox("Pilih Semua");
		pilihSemua.setStyle("font-weight:bold;");
		pilihSemua.setParent(cariRow);
		org.zkoss.zul.Div listDiv = new org.zkoss.zul.Div();
		listDiv.setStyle("max-height:360px; overflow:auto; border:1px solid #e5e7eb; border-radius:10px; padding:4px; width:100%; box-sizing:border-box; background:#ffffff;");
		box.appendChild(listDiv);
		Vbox listBox = new Vbox();
		listBox.setWidth("100%");
		listBox.setParent(listDiv);
		for (Object[] it : itemData) {
			String label = (String) it[3];
			org.zkoss.zul.Hbox rowH = new org.zkoss.zul.Hbox();
			rowH.setWidth("100%");
			rowH.setAlign("middle");
			rowH.setSpacing("8px");
			rowH.setStyle("padding:6px 8px; border-bottom:1px solid #f1f5f9; box-sizing:border-box;");
			rowH.setParent(listBox);
			final org.zkoss.zul.Checkbox cb = new org.zkoss.zul.Checkbox();
			cb.setParent(rowH);
			MyLabelKecil lbl = new MyLabelKecil(label);
			lbl.setMultiline(true);
			lbl.setStyle("color:#0f172a;");
			lbl.setParent(rowH);
			states.add(new Object[] { cb, it[0], it[1], it[2], it[4] });
			checks.add(cb);
			rowsUi.add(rowH);
			labelsLower.add(label == null ? "" : label.toLowerCase());
		}
		final EventListener recount = new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				int c = 0;
				for (org.zkoss.zul.Checkbox cb : checks) {
					if (cb.isChecked()) {
						c++;
					}
				}
				lblTerpilih.setValue(c + " antrian dipilih");
			}
		};
		for (org.zkoss.zul.Checkbox cb0 : checks) {
			cb0.addEventListener("onCheck", recount);
		}
		EventListener filterCari = new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				String q;
				if (e instanceof org.zkoss.zk.ui.event.InputEvent) {
					q = ((org.zkoss.zk.ui.event.InputEvent) e).getValue();
				} else {
					q = cari.getValue();
				}
				q = q == null ? "" : q.trim().toLowerCase();
				for (int i2 = 0; i2 < rowsUi.size(); i2++) {
					rowsUi.get(i2).setVisible(q.length() == 0 || labelsLower.get(i2).indexOf(q) >= 0);
				}
			}
		};
		cari.addEventListener("onChanging", filterCari);
		cari.addEventListener("onOK", filterCari);
		pilihSemua.addEventListener("onCheck", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				boolean v = pilihSemua.isChecked();
				for (int i2 = 0; i2 < checks.size(); i2++) {
					if (rowsUi.get(i2).isVisible()) {
						checks.get(i2).setChecked(v);
					}
				}
			}
		});
		pilihSemua.addEventListener("onCheck", recount);
		recount.onEvent(null);
		MyLabelAgakKecil lblCat = new MyLabelAgakKecil("Catatan (opsional, dipakai untuk semua yang disetujui):");
		lblCat.setStyle("color:#475569; font-weight:600;");
		box.appendChild(lblCat);
		final Textbox catatan = new Textbox();
		catatan.setMultiline(true);
		catatan.setRows(2);
		catatan.setWidth("100%");
		catatan.setStyle("border-radius:8px; box-sizing:border-box;");
		catatan.setParent(box);
		org.zkoss.zul.Div sep = new org.zkoss.zul.Div();
		sep.setStyle("border-top:1px solid #e5e7eb; margin-top:12px;");
		sep.setParent(box);
		org.zkoss.zul.Hbox footer = new org.zkoss.zul.Hbox();
		footer.setWidth("100%");
		footer.setAlign("middle");
		footer.setStyle("margin-top:12px;");
		footer.setParent(box);
		lblTerpilih.setStyle("color:#475569; font-weight:700;");
		lblTerpilih.setParent(footer);
		org.zkoss.zul.Div spacer = new org.zkoss.zul.Div();
		spacer.setHflex("1");
		spacer.setParent(footer);
		final MyToolbarbuttonConfig btnOk = new MyToolbarbuttonConfig("Setujui Terpilih", "/img/svg/check2.svg");
		btnOk.setWidth("160px");
		btnOk.setStyle("font-weight:bold; color:#ffffff; background:#16a34a; border-radius:10px; padding:8px 0; text-align:center; margin-right:8px;");
		btnOk.setParent(footer);
		MyToolbarbuttonConfig btnBatal = new MyToolbarbuttonConfig("Batal", "/img/svg/close.svg");
		btnBatal.setWidth("160px");
		btnBatal.setStyle("font-weight:bold; color:#334155; background:#f1f5f9; border:1px solid #e2e8f0; border-radius:10px; padding:8px 0; text-align:center;");
		btnBatal.setParent(footer);
		btnBatal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				win.detach();
			}
		});
		btnOk.addEventListener("onClick", new EventListener() {
			@Override
			@SuppressWarnings("unchecked")
			public void onEvent(Event e) throws Exception {
				java.util.List<Object[]> tugas = new java.util.ArrayList<Object[]>();
				for (Object[] st : states) {
					org.zkoss.zul.Checkbox cbi = (org.zkoss.zul.Checkbox) st[0];
					if (!cbi.isChecked()) {
						continue;
					}
					tugas.add(new Object[] { st[1], st[2], st[3], st[4] });
				}
				if (tugas.isEmpty()) {
					ais.ui.util.MyMessageboxConfig.show(
						"Mohon Bapak/Ibu terlebih dahulu memilih minimal satu antrian yang akan disetujui. Langkah yang dapat dilakukan: (1) tandai (centang) antrian pada daftar; (2) pastikan minimal satu antrian terpilih; (3) tekan tombol persetujuan kembali.",
						"Setujui Massal", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.EXCLAMATION);
					return;
				}
				btnOk.setDisabled(true);
				int[] r = setujuiTugas(tugas, catatan.getValue());
				try {
					win.detach();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:3777");
				}
				ais.ui.util.MyMessageboxConfig.showFormat(
					"Proses persetujuan massal telah selesai. Berhasil menyetujui {V1} pengajuan (Setujui dan Selesai / lanjut alur){V2}.",
					"Setujui Massal", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION,
					r[0], (r[2] > 0 ? ", namun terdapat " + r[2] + " pengajuan yang gagal diproses" : ""));
				if (refreshAfter != null) {
					try {
						refreshAfter.onEvent(e);
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/helper/DasboardSop.java:3786");
					}
				}
			}
		});
		win.doModal();
	}
	/** Proses daftar tugas {dsId, stepId, alurId, List<Long> rute} via prosesLangkah. Return {ok,lewat,gagal}. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private int[] setujuiTugas(java.util.List<Object[]> tugas, String keterangan) {
		if (tbmuser == null) {
			tbmuser = Common.getCurrentUser();
		}
		if (tbmuser == null || tbmuser.getUserId() == null || tugas == null) {
			return new int[] { 0, 0, 0 };
		}
		String ket = keterangan == null ? "" : keterangan;
		int ok = 0, gagal = 0;
		for (Object[] t : tugas) {
			try {
				ais.action.master.sop.helper.ProsesDisposisiSopService.Hasil h = ais.action.master.sop.helper.ProsesDisposisiSopService
					.prosesLangkah(tbmuser, (Long) t[0], (Long) t[1], (Long) t[2], tbmuser.getUserId(),
						new java.util.Date(), null, ket, true, false, (java.util.List<Long>) t[3]);
				if (h != null && h.ok) {
					ok++;
				} else {
					gagal++;
				}
			} catch (Exception e) {
				gagal++;
			}
		}
		return new int[] { ok, 0, gagal };
	}

	private String getOlehName(Tbmuser user, Siswa siswa, Mahasiswa mahasiswa) {
		if (user != null)
			return user.getUserNama();
		if (mahasiswa != null)
			return mahasiswa.getNama();
		if (siswa != null)
			return siswa.getNama();
		return "";
	}

	private int getCountMenunggu() {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Criteria criteria = createMenungguSayaSesuaiPanelCriteria(session, false, false)
					.setProjection(Projections.rowCount());
			Object result = criteria.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			printDebug(e);
			return 0;
		} finally {
			closeSessionSafely(session);
		}
	}

	private int getCountDisposisi() {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Criteria criteria = createCriteria(session, DisposisiAlurSop.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.isNotNull("alurSop"))
					.createAlias("alurSop", "alurSop").add(Restrictions.isNotNull("alurSop.sop"))
					.add(Restrictions.eq("alurSop.start", false)).add(Restrictions.isNotNull("waktu"))
					.createAlias("disposisiSop", "disposisiSop").add(getUserRestriction(tbmuser, ""))
					.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
							Restrictions.eq("disposisiSop.aktif", true)));
			if (!ignoreGlobalFilterForLegacyPanel) {
				applyGlobalDisposisiFilter(criteria);
			}
			return ((Number) criteria.uniqueResult()).intValue();
		} catch (Exception e) {
			printDebug(e);
			return 0;
		} finally {
			closeSessionSafely(session);
		}
	}

	private int getCountSelesai() {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Criteria criteria = createSelesaiSesuaiPanelCriteria(session, false)
					.setProjection(Projections.rowCount());
			Object result = criteria.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			printDebug(e);
			return 0;
		} finally {
			closeSessionSafely(session);
		}
	}

	private int getCountMenungguAktor() {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Criteria criteria = createMenungguAktorSesuaiPanelCriteria(session, false)
					.setProjection(Projections.rowCount());
			Object result = criteria.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			printDebug(e);
			return 0;
		} finally {
			closeSessionSafely(session);
		}
	}

	private void applyGlobalDisposisiFilter(Criteria criteria) {
		if (criteria == null) {
			return;
		}
		try {
			if (dashboardFilterMulai != null) {
				criteria.add(getDateRestriction(dashboardFilterMulai, ">="));
			}
			if (dashboardFilterSampai != null) {
				criteria.add(getDateRestriction(dashboardFilterSampai, "<="));
			}
			criteria.add(getSearchRestriction(dashboardFilterKeyword));
			applyGlobalSatkerFilter(criteria);
		} catch (Exception e) {
			printDebug(e);
		}
	}


	private void applyGlobalDisposisiSopFilter(Criteria criteria) {
		if (criteria == null) {
			return;
		}
		try {
			if (dashboardFilterMulai != null) {
				criteria.add(getDateRestrictionRoot(dashboardFilterMulai, ">="));
			}
			if (dashboardFilterSampai != null) {
				criteria.add(getDateRestrictionRoot(dashboardFilterSampai, "<="));
			}
			criteria.add(getDisposisiSopRootSearchRestriction(dashboardFilterKeyword));
			applyGlobalDisposisiSopSatkerFilter(criteria);
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private Criterion getDisposisiSopRootSearchRestriction(String c) {
		if (c == null || c.trim().isEmpty()) {
			return Restrictions.sqlRestriction("true");
		}
		String keyword = c.trim();
		return Restrictions.or(Restrictions.ilike("properti", keyword, MatchMode.ANYWHERE),
				Restrictions.or(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE),
						Restrictions.ilike("sopDetail.nama", keyword, MatchMode.ANYWHERE)));
	}

	private void applyGlobalDisposisiSopSatkerFilter(Criteria criteria) {
		if (criteria == null || dashboardFilterSatker == null) {
			return;
		}
		try {
			criteria.createAlias("diajukanOleh", "globalPengajuUserRoot", Criteria.LEFT_JOIN);
			criteria.createAlias("globalPengajuUserRoot.pegawai", "globalPengajuPegawaiRoot", Criteria.LEFT_JOIN);
			criteria.add(Restrictions.eq("globalPengajuPegawaiRoot.satuanKerja", dashboardFilterSatker));
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void applyGlobalSatkerFilter(Criteria criteria) {
		if (criteria == null || dashboardFilterSatker == null) {
			return;
		}
		try {
			criteria.createAlias("disposisiSop.diajukanOleh", "globalPengajuUser", Criteria.LEFT_JOIN);
			criteria.createAlias("globalPengajuUser.pegawai", "globalPengajuPegawai", Criteria.LEFT_JOIN);
			criteria.createAlias("diajukanOleh", "globalRootUser", Criteria.LEFT_JOIN);
			criteria.createAlias("globalRootUser.pegawai", "globalRootPegawai", Criteria.LEFT_JOIN);
			criteria.add(Restrictions.or(Restrictions.eq("globalPengajuPegawai.satuanKerja", dashboardFilterSatker),
					Restrictions.eq("globalRootPegawai.satuanKerja", dashboardFilterSatker)));
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void applyGlobalAlurSopFilter(Criteria criteria) {
		if (criteria == null) {
			return;
		}
		String c = dashboardFilterKeyword == null ? "" : dashboardFilterKeyword.trim();
		if (!c.isEmpty()) {
			try {
				criteria.createAlias("sop", "globalSopFilter", Criteria.LEFT_JOIN);
				criteria.add(Restrictions.or(Restrictions.ilike("globalSopFilter.nama", c, MatchMode.ANYWHERE),
						Restrictions.or(Restrictions.ilike("nama", c, MatchMode.ANYWHERE),
								Restrictions.ilike("petugas", c, MatchMode.ANYWHERE))));
			} catch (Exception e) {
				printDebug(e);
			}
		}
	}

	private String formatPeriodeDasbor(Date mulai, Date sampai) {
		if (mulai == null && sampai == null) {
			return "Semua Periode";
		}
		if (mulai == null) {
			return "Sampai " + formatTanggalDasbor(sampai);
		}
		if (sampai == null) {
			return "Mulai " + formatTanggalDasbor(mulai);
		}
		return formatTanggalDasbor(mulai) + " s.d. " + formatTanggalDasbor(sampai);
	}

	private String formatTanggalDasbor(Date date) {
		try {
			return date == null ? "-" : Common.dateFormat.get().format(date);
		} catch (Exception e) {
			return "-";
		}
	}

	private void closeSessionSafely(Session session) {
		ais.ui.util.DashboardModernHtmlUtil.closeOpenedSession(session);
	}

	private Criteria createCriteria(Session session, Class clazz) {
		Criteria criteria = session.createCriteria(clazz);
		if (criteria != null && session != null) {
			synchronized (criteriaSessionMap) {
				criteriaSessionMap.put(criteria, session);
			}
		}
		return criteria;
	}

	private void closeCriteriaSessionSafely(Criteria criteria) {
		if (criteria == null) {
			return;
		}
		Session mappedSession = null;
		synchronized (criteriaSessionMap) {
			mappedSession = criteriaSessionMap.remove(criteria);
		}
		if (mappedSession != null) {
			closeSessionSafely(mappedSession);
			return;
		}
		try {
			Object sessionObject = criteria.getClass().getMethod("getSession", new Class[0]).invoke(criteria,
					new Object[0]);
			if (sessionObject instanceof Session) {
				closeSessionSafely((Session) sessionObject);
			}
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void removeCriteriaReferences(Session session) {
		if (session == null) {
			return;
		}
		synchronized (criteriaSessionMap) {
			Iterator<Map.Entry<Criteria, Session>> iterator = criteriaSessionMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<Criteria, Session> entry = iterator.next();
				if (entry != null && entry.getValue() == session) {
					iterator.remove();
				}
			}
		}
	}

	// =========================================================================================
	// HELPER CRITERION & INTERFACES
	// =========================================================================================

	private Criterion getDateRestriction(MyDatebox datebox, String operator) {
		if (datebox == null || datebox.getValue() == null)
			return Restrictions.sqlRestriction("true");
		return getDateRestriction(datebox.getValue(), operator);
	}

	private Criterion getDateRestriction(Date date, String operator) {
		// Criteria root DisposisiAlurSop: tanggal difilter pada disposisiSop.waktu (tanggal pengajuan SOP
		// yang ditampilkan & TERSIMPAN). DULU memakai this_.waktu (= DisposisiAlurSop.waktu) yang untuk
		// baris dashboard (diajukanOleh/mahasiswa/siswa NULL) bernilai NULL di DB → date(NULL) membuat
		// SELURUH baris terbuang sehingga hasil filter tanggal KOSONG.
		return getDateRestrictionPada(date, operator, "disposisiSop.waktu");
	}

	/** Versi untuk criteria yang ROOT-nya DisposisiSop (filter pada kolom waktu milik root sendiri). */
	private Criterion getDateRestrictionRoot(Date date, String operator) {
		return getDateRestrictionPada(date, operator, "waktu");
	}

	/**
	 * Filter rentang tanggal INKLUSIF per-hari pada {@code property} (mengabaikan komponen jam pada
	 * timestamp): ">=" → {@code property >= awal hari tanggal}; "<=" → {@code property < awal hari
	 * BERIKUTNYA} sehingga seluruh hari "sampai" ikut tercakup. Property-based (bukan native SQL) agar
	 * alias/relasi di-resolve Hibernate dengan benar.
	 */
	private Criterion getDateRestrictionPada(Date date, String operator, String property) {
		if (date == null)
			return Restrictions.sqlRestriction("true");
		if (operator.equals(">=")) {
			return Restrictions.ge(property, awalHari(date, 0));
		}
		return Restrictions.lt(property, awalHari(date, 1));
	}

	private static Date awalHari(Date date, int tambahHari) {
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.setTime(date);
		c.add(java.util.Calendar.DAY_OF_MONTH, tambahHari);
		c.set(java.util.Calendar.HOUR_OF_DAY, 0);
		c.set(java.util.Calendar.MINUTE, 0);
		c.set(java.util.Calendar.SECOND, 0);
		c.set(java.util.Calendar.MILLISECOND, 0);
		return c.getTime();
	}

	private Criterion getSearchRestriction(String c) {
		if (c == null || c.isEmpty())
			return Restrictions.sqlRestriction("true");
		return Restrictions.or(Restrictions.ilike("disposisiSop.properti", c, MatchMode.ANYWHERE),
				Restrictions.or(Restrictions.ilike("properti", c, MatchMode.ANYWHERE),
						Restrictions.ilike("keyword", c, MatchMode.ANYWHERE)));
	}

	private Criterion getUserRestriction(Tbmuser user, String prefix) {
		if (AktorSop.bolehMelihatSemuaSop(user)) {
			return Restrictions.sqlRestriction("true");
		}
		String p = prefix == null || prefix.isEmpty() ? "" : prefix + ".";
		if (user != null && (user.getMahasiswa() != null || user.getSiswa() != null)) {
			return Restrictions.or(Restrictions.eq(p + "mahasiswa", user.getMahasiswa()),
					Restrictions.eq(p + "siswa", user.getSiswa()));
		}
		return user == null ? Restrictions.sqlRestriction("false") : Restrictions.eq(p + "diajukanOleh", user);
	}

	private void applySopFilter(Criteria criteria, Combobox sopBox) {
		if (sopBox != null && sopBox.getSelectedItem() != null) {
			Sop s = (Sop) sopBox.getSelectedItem().getValue();
			if (s != null)
				criteria.add(Restrictions.eq("disposisiSop.sop", s));
		}
	}

	private void appendPanelModeInfo(Rows rows, String title, String description, String background, String border,
			String color) {
		try {
			Row row = new Row();
			row.setParent(rows);
			org.zkoss.zul.Html html = new org.zkoss.zul.Html("<div style='padding:10px 12px; margin-bottom:8px; "
					+ "border-radius:12px; background:" + background + "; border:1px solid " + border
					+ "; color:" + color + "; line-height:1.45;'>" + "<div style='font-size:12px; font-weight:900;'>"
					+ escapeHtml(title) + "</div>" + "<div style='font-size:11px; margin-top:4px;'>"
					+ escapeHtml(description) + "</div></div>");
			row.appendChild(html);
		} catch (Exception e) {
			printDebug(e);
		}
	}

	@SuppressWarnings("unused")
	private void buildMendekatiDedlineDisposisi() throws Exception {
		buildDashboardPanel("Menunggu disposisi Anda berdasarkan batas waktu", new BaseSearchDataHandler() {
			@Override
			public Criteria buildCriteria(String search, MyDatebox mulai, MyDatebox sampai, Combobox sop,
					boolean order) {
				Session session = HibernateUtil.openSession();
				Criteria criteria = createCriteria(session, DisposisiAlurSop.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(getDateRestriction(sampai, "<=")).add(getDateRestriction(mulai, ">="))
						.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
						.add(Restrictions.isNotNull("alurSop.sop"))
						.createAlias("alurSop.aktorSop", "aktorSop", Criteria.LEFT_JOIN)
						.createAlias("disposisiSop", "disposisiSop")
						.createAlias("sebelumnya", "sebelumnya", Criteria.LEFT_JOIN);

				criteria.add(getAktorRestriction(criteria)).add(Restrictions.isNull("diajukanOleh"))
						.add(Restrictions.isNull("mahasiswa")).add(Restrictions.isNull("siswa"))
						.add(Restrictions.isNotNull("waktuMaksimal")).add(Restrictions.lt("waktuMaksimal", new Date()))
						.add(getSearchRestriction(search))
						.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
								Restrictions.eq("disposisiSop.aktif", true)));

				applySopFilter(criteria, sop);
				if (order)
					criteria.addOrder(Order.desc("waktuMaksimal"));
				return criteria;
			}

			@Override
			public void renderData(Rows rows, List<DisposisiAlurSop> data, final EventListener listener)
					throws Exception {
				appendPanelModeInfo(rows, "Menunggu Disposisi Anda Berdasarkan Lewat Batas Waktu",
						"Menampilkan bagian dari antrian disposisi Anda yang sudah memiliki batas waktu dan sudah melewati batas tersebut. Ini menjadi daftar prioritas, bukan seluruh antrian.",
						"#fef2f2", "#fecaca", "#991b1b");
				TreeMap<String, List<DisposisiAlurSop>> waktus = new TreeMap<String, List<DisposisiAlurSop>>();

				// Grouping berdasarkan waktu maksimal
				for (DisposisiAlurSop disposisiAlurSop : data) {
					if (disposisiAlurSop.getAktif() && disposisiAlurSop.getWaktuMaksimal() != null) {
						String waktu = Common.dateFormat8.get().format(disposisiAlurSop.getWaktuMaksimal());
						List<DisposisiAlurSop> a = waktus.get(waktu);
						if (a == null) {
							a = new ArrayList<DisposisiAlurSop>();
							waktus.put(waktu, a);
						}
						a.add(disposisiAlurSop);
					}
				}

				// Rendering UI Grouping
				for (String waktu : waktus.keySet()) {
					Row row = new Row();
					row.setParent(rows);

					Vbox vbox1 = new Vbox();
					row.appendChild(vbox1);
					vbox1.appendChild(new MyLabelBold(waktu));

					for (DisposisiAlurSop disposisiAlurSop : waktus.get(waktu)) {
						String oleh = getOlehName(disposisiAlurSop.getDisposisiSop().getDiajukanOleh(),
								disposisiAlurSop.getDisposisiSop().getSiswa(),
								disposisiAlurSop.getDisposisiSop().getMahasiswa());

						A a = new A();
						a.setParent(vbox1);
						Vbox vbox = new Vbox();
						a.appendChild(vbox);

						renderKodePengajuanMultiSumber(vbox, disposisiAlurSop);
						vbox.appendChild(new MyLabelAgakKecil(
								disposisiAlurSop.getDisposisiSop().getSop().getNama() + " (pengajuan "
										+ Common.dateFormat.get().format(disposisiAlurSop.getDisposisiSop().getWaktu()) + " "
										+ oleh + ")"));
						vbox.appendChild(new MyLabelAgakKecil(disposisiAlurSop.getAlurSop().getAktor() + " - "
								+ disposisiAlurSop.getAlurSop().getNama()));

						renderStatusDisposisiTerakhir(vbox, disposisiAlurSop);

						if (disposisiAlurSop.getWaktuMaksimal() != null) {
							vbox.appendChild(new MyLabelAgakKecilBoldMerah(
									"Batas Waktu:" + Common.dateFormat51.get().format(disposisiAlurSop.getWaktuMaksimal())));
						}

						attachKlikProses(a, disposisiAlurSop.getDisposisiSop().getId(), listener);
					}
				}
			}
		});
	}


	private static class SopDashboardData {
		int jumlahSopBaru;
		int menungguSaya;
		int sudahSayaDisposisi;
		int selesai;
		int menungguAktor;
		int mendekatiDeadline;
		int totalAntrian;
		int totalAktivitas;
		int totalDipantau;
		int deadlineLewat;
		int deadlineHariIni;
		int deadlineMingguIni;
		int deadlineAman;
		int tanpaDeadline;
		int tanpaAktor;
		int tanpaTahap;
		int tanpaCatatan;
		Map<String, Integer> perSop = new HashMap<String, Integer>();
		Map<String, Integer> perAktor = new HashMap<String, Integer>();
		Map<String, Integer> perBulan = new HashMap<String, Integer>();
		List<SopRecentItem> aktivitasTerbaru = new ArrayList<SopRecentItem>();
	}

	private static class SopRecentItem {
		String sop;
		String aktor;
		String waktu;
		String status;
		String deadline;
	}

	private static class CounterItem {
		String label;
		int count;
	}

	interface DetailCriteriaProvider {
		Criteria buildCriteria(boolean order);
	}

	interface DetailRowRenderer {
		String[] getHeaders();

		void render(Rows rows, List data) throws Exception;
	}

	interface DashboardDataHandler {
		void onToolbarCreated(Toolbar toolbar);

		String getRefreshLabel();

		String getRefreshIcon();

		Criteria buildCriteria(String search, MyDatebox mulai, MyDatebox sampai, Combobox sop, boolean order);

		void renderData(Rows rows, List<DisposisiAlurSop> data, EventListener listener) throws Exception;
	}

	abstract class BaseSearchDataHandler implements DashboardDataHandler {
		public boolean supportsBulkApprove() {
			return false;
		}

		@Override
		public void onToolbarCreated(Toolbar toolbar) {
		}

		@Override
		public String getRefreshLabel() {
			return "";
		}

		@Override
		public String getRefreshIcon() {
			return "/img/svg/search.svg";
		}
	}
}
