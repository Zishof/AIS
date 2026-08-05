package ais.action.master.akunting.helper;
import ais.ui.util.DashboardGridExportHelper;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


/*
 * DASBOARD_AKUNTANSI_V2_PAGING_HTML_CSS_CHART_FIX_COLUMN_AND_STRING_2026_05_28
 *
 * Dibuat sebagai dashboard utama Akuntansi dengan pola UI/UX dari template DasboardSop.
 * Data dashboard mengambil referensi query dari:
 * - DasboardAkunting.java
 * - DasboardBukuBesar.java
 * - DasboardNeracaLajur.java
 *
 * Catatan kompatibilitas:
 * - Tetap memakai style Java lama (tanpa lambda / stream).
 * - Angka utama clickable dan membuka popup detail paging per 10 data.
 * - Filter global tanggal, satuan kerja, dan keyword dipakai oleh semua kartu/panel.
 */
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.A;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.DashboardCacheUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.JenisLaporan;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Memadukan ringkasan jurnal, buku besar, dan neraca agar kondisi akuntansi terlihat dalam satu tempat.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardAkuntansi extends MyPortallayout {

	private static final long serialVersionUID = -9006490521125337935L;

	public static boolean debug = false;

	private static final int DETAIL_PAGE_SIZE = 10;
	private static final int DASHBOARD_PAGE_SIZE = 10;
	private static final int TOP_LIMIT = 12;

	private Date dashboardFilterMulai;
	private Date dashboardFilterSampai;
	private SatuanKerja dashboardFilterSatker;
	private String dashboardFilterKeyword = "";
	private int dashboardRenderVersion = 0;
	private AkuntansiDashboardData loadingDashboardData;
	private org.zkoss.zul.Vbox loadingDashboardContainer;

	public DasboardAkuntansi() throws Exception {
		super();
		setWidth("100%");
		setMaximizedMode("whole");
		init();
	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Akuntansi");
		if (dashboardFilterMulai == null) {
			dashboardFilterMulai = getDefaultMulai();
		}
		if (dashboardFilterSampai == null) {
			dashboardFilterSampai = WaktuUtil.getDate();
		}
		renderDashboard();
	}

	private Date getDefaultMulai() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 2);
		return calendar.getTime();
	}

	private void tampilkanLoadingDashboardAkuntansi() {
		tampilkanLoadingDashboardAkuntansi("Mengambil Ringkasan Dasbor Akuntansi...", 5);
	}

	private void tampilkanLoadingDashboardAkuntansi(String pesan, int persen) {
		Common.clear(this);
		loadingDashboardContainer = null;

		MyPortalchildren wrapper = new MyPortalchildren();
		wrapper.setWidth("100%");
		wrapper.setStyle("padding:6px; box-sizing:border-box;");
		wrapper.setParent(this);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setTitle("Dashboard Akuntansi");
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:18px; overflow:hidden;"
				+ "background:#ffffff; box-shadow:0 14px 28px rgba(15,23,42,.08);");
		panel.setParent(wrapper);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setStyle("padding:0; background:#f6f8fb;");
		panelchildren.setParent(panel);

		loadingDashboardContainer = new org.zkoss.zul.Vbox();
		loadingDashboardContainer.setWidth("100%");
		loadingDashboardContainer.setStyle("background:#f6f8fb; padding:14px; box-sizing:border-box;");
		loadingDashboardContainer.setParent(panelchildren);

		updateLoadingDashboardAkuntansi(pesan, persen);
	}

	private void updateLoadingDashboardAkuntansi(String pesan, int persen) {
		if (loadingDashboardContainer == null) {
			return;
		}
		try {
			Common.clear(loadingDashboardContainer);
			Html htmlLoading = new Html(buildLoadingDashboardHtml(pesan, persen, false));
			loadingDashboardContainer.appendChild(htmlLoading);
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void tampilkanErrorLoadingDashboardAkuntansi(Exception e) {
		if (loadingDashboardContainer == null) {
			tampilkanLoadingDashboardAkuntansi("Terjadi kesalahan saat memuat dashboard akuntansi.", 100);
		}
		try {
			Common.clear(loadingDashboardContainer);
			String pesan = "Terjadi kesalahan saat memuat dashboard akuntansi.";
			if (debug && e != null && e.getMessage() != null) {
				pesan = pesan + " Detail: " + e.getMessage();
			}
			Html htmlLoading = new Html(buildLoadingDashboardHtml(pesan, 100, true));
			loadingDashboardContainer.appendChild(htmlLoading);

			MyToolbarbuttonConfig ulangi = new MyToolbarbuttonConfig("Muat Ulang Dashboard", "/img/svg/refresh.svg");
			ulangi.setStyle("font-weight:bold; color:#ffffff; background:#dc2626; border-radius:10px; padding:7px 14px; margin:0 auto;");
			ulangi.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					renderDashboard();
				}
			});
			loadingDashboardContainer.appendChild(ulangi);
		} catch (Exception ex) {
			printDebug(ex);
		}
	}

	private String buildLoadingDashboardHtml(String pesan, int persen, boolean error) {
		persen = normalizeProgressPercent(persen);
		String warnaUtama = error ? "#dc2626" : "#047857";
		String warnaMuda = error ? "#fee2e2" : "#d1fae5";
		String icon = error ? "fa fa-exclamation-triangle" : "fa fa-spinner fa-spin";
		String judul = error ? "Dashboard Akuntansi Gagal Dimuat" : "Memproses Dashboard Akuntansi";
		String text = pesan == null || pesan.trim().length() == 0 ? "Memproses data dashboard..." : pesan.trim();

		return "<div style='padding:26px 18px; text-align:center; color:#334155;'>"
				+ "<div style='max-width:760px; margin:0 auto; background:#ffffff; border:1px solid #e5e7eb; border-radius:18px; "
				+ "box-shadow:0 14px 28px rgba(15,23,42,.08); padding:24px; box-sizing:border-box;'>"
				+ "<div style='width:54px; height:54px; margin:0 auto 12px auto; border-radius:18px; background:" + warnaMuda + "; "
				+ "display:flex; align-items:center; justify-content:center; color:" + warnaUtama + "; font-size:24px;'>"
				+ "<i class='" + icon + "'></i></div>"
				+ "<div style='font-size:18px; font-weight:800; color:#0f172a;'>" + escapeHtml(judul) + "</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:6px; line-height:1.6;'>"
				+ escapeHtml(text) + "</div>"
				+ "<div style='margin-top:18px; width:100%; height:16px; background:#e5e7eb; border-radius:999px; overflow:hidden;'>"
				+ "<div style='height:16px; width:" + persen + "%; background:" + warnaUtama + "; border-radius:999px; "
				+ "transition:width .3s ease;'></div></div>"
				+ "<div style='font-size:12px; font-weight:800; color:" + warnaUtama + "; margin-top:8px;'>"
				+ persen + "% selesai</div>"
				+ "<div style='margin-top:14px; display:flex; justify-content:center; gap:8px; flex-wrap:wrap;'>"
				+ "<span style='padding:5px 9px; border-radius:999px; background:#f8fafc; border:1px solid #e5e7eb; font-size:11px;'>Ringkasan</span>"
				+ "<span style='padding:5px 9px; border-radius:999px; background:#f8fafc; border:1px solid #e5e7eb; font-size:11px;'>Saldo</span>"
				+ "<span style='padding:5px 9px; border-radius:999px; background:#f8fafc; border:1px solid #e5e7eb; font-size:11px;'>Top Akun</span>"
				+ "<span style='padding:5px 9px; border-radius:999px; background:#f8fafc; border:1px solid #e5e7eb; font-size:11px;'>Laporan</span>"
				+ "<span style='padding:5px 9px; border-radius:999px; background:#f8fafc; border:1px solid #e5e7eb; font-size:11px;'>Grafik</span>"
				+ "</div>"
				+ "</div></div>";
	}

	private int normalizeProgressPercent(int persen) {
		if (persen < 0) {
			return 0;
		}
		if (persen > 100) {
			return 100;
		}
		return persen;
	}


	private String buildAkuntansiCacheKey() {
		String fp = (dashboardFilterMulai  != null ? String.valueOf(dashboardFilterMulai.getTime())  : "0")
				+ "_" + (dashboardFilterSampai != null ? String.valueOf(dashboardFilterSampai.getTime()) : "0")
				+ "_" + (dashboardFilterSatker  != null ? String.valueOf(dashboardFilterSatker.getId())   : "all")
				+ "_" + (dashboardFilterKeyword != null ? dashboardFilterKeyword : "");
		return DashboardCacheUtil.keyWithFilter("DasboardAkuntansi", "ADMIN", null, fp);
	}

	private void renderDashboard() throws Exception {
		String cacheKey = buildAkuntansiCacheKey();
		Object fromL2 = DashboardCacheUtil.getL2(cacheKey);
		if (fromL2 instanceof AkuntansiDashboardData) {
			renderDashboardContent((AkuntansiDashboardData) fromL2);
			return;
		}
		Object fromL3 = DashboardCacheUtil.getL3(cacheKey);
		if (fromL3 instanceof AkuntansiDashboardData) {
			DashboardCacheUtil.putL2(cacheKey, fromL3);
			renderDashboardContent((AkuntansiDashboardData) fromL3);
			return;
		}
		dashboardRenderVersion++;
		final int version = dashboardRenderVersion;
		loadingDashboardData = new AkuntansiDashboardData();
		tampilkanLoadingDashboardAkuntansi("Mempersiapkan tampilan dashboard akuntansi...", 5);
		jadwalkanProsesDashboardAkuntansi(version, 1);
	}

	private void jadwalkanProsesDashboardAkuntansi(final int version, final int tahap) {
		try {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					prosesDashboardAkuntansiBertahap(version, tahap);
				}
			});
		} catch (Exception e) {
			printDebug(e);
			try {
				prosesDashboardAkuntansiBertahap(version, tahap);
			} catch (Exception ex) {
				tampilkanErrorLoadingDashboardAkuntansi(ex);
			}
		}
	}

	private void prosesDashboardAkuntansiBertahap(final int version, final int tahap) throws Exception {
		if (version != dashboardRenderVersion) {
			return;
		}
		if (loadingDashboardData == null) {
			loadingDashboardData = new AkuntansiDashboardData();
		}

		try {
			if (tahap == 1) {
				updateLoadingDashboardAkuntansi("Mengambil ringkasan transaksi terposting, bukti/jurnal, debet, kredit, dan saldo bersih...", 10);
				jadwalkanProsesDashboardAkuntansi(version, 2);
			} else if (tahap == 2) {
				loadDashboardSummaryData(loadingDashboardData);
				updateLoadingDashboardAkuntansi("Ringkasan transaksi selesai. Menghitung saldo awal dan saldo akhir periode...", 25);
				jadwalkanProsesDashboardAkuntansi(version, 3);
			} else if (tahap == 3) {
				loadDashboardSaldoData(loadingDashboardData);
				updateLoadingDashboardAkuntansi("Saldo awal dan saldo akhir selesai. Mengambil konfigurasi jenis laporan aktif...", 38);
				jadwalkanProsesDashboardAkuntansi(version, 4);
			} else if (tahap == 4) {
				loadDashboardJenisLaporanData(loadingDashboardData);
				updateLoadingDashboardAkuntansi("Jenis laporan aktif selesai. Mengambil trend mutasi bulanan...", 50);
				jadwalkanProsesDashboardAkuntansi(version, 5);
			} else if (tahap == 5) {
				loadDashboardMutasiBulananData(loadingDashboardData);
				updateLoadingDashboardAkuntansi("Trend mutasi bulanan selesai. Mengambil Top Akun Debet...", 62);
				jadwalkanProsesDashboardAkuntansi(version, 6);
			} else if (tahap == 6) {
				loadDashboardTopAkunDebetData(loadingDashboardData);
				updateLoadingDashboardAkuntansi("Top Akun Debet selesai. Mengambil Top Akun Kredit...", 72);
				jadwalkanProsesDashboardAkuntansi(version, 7);
			} else if (tahap == 7) {
				loadDashboardTopAkunKreditData(loadingDashboardData);
				updateLoadingDashboardAkuntansi("Top Akun Kredit selesai. Mengambil ringkasan laporan keuangan...", 80);
				jadwalkanProsesDashboardAkuntansi(version, 8);
			} else if (tahap == 8) {
				loadDashboardRingkasLaporanData(loadingDashboardData);
				updateLoadingDashboardAkuntansi("Ringkasan laporan keuangan selesai. Mengambil sinyal Neraca Lajur...", 88);
				jadwalkanProsesDashboardAkuntansi(version, 9);
			} else if (tahap == 9) {
				loadDashboardRingkasNeracaData(loadingDashboardData);
				updateLoadingDashboardAkuntansi("Data utama selesai. Menyusun kartu, tabel rekap paging, dan grafik HTML/CSS modern...", 96);
				jadwalkanProsesDashboardAkuntansi(version, 10);
			} else {
				AkuntansiDashboardData data = loadingDashboardData;
				loadingDashboardData = null;
				String ck = buildAkuntansiCacheKey();
				DashboardCacheUtil.putL2(ck, data);
				DashboardCacheUtil.putL3(ck, data);
				renderDashboardContent(data);
			}
		} catch (Exception e) {
			printDebug(e);
			tampilkanErrorLoadingDashboardAkuntansi(e);
		}
	}

	private void renderDashboardContent(AkuntansiDashboardData data) throws Exception {
		if (data == null) {
			data = new AkuntansiDashboardData();
		}
		Common.clear(this);

		MyPortalchildren wrapper = new MyPortalchildren();
		wrapper.setWidth("100%");
		wrapper.setStyle("padding:6px; box-sizing:border-box;");
		wrapper.setParent(this);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setTitle("Dashboard Akuntansi");
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:18px; overflow:hidden;"
				+ "background:#ffffff; box-shadow:0 14px 28px rgba(15,23,42,.08);");
		panel.setParent(wrapper);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setStyle("padding:0; background:#f6f8fb;");
		panelchildren.setParent(panel);

		org.zkoss.zul.Div shell = new org.zkoss.zul.Div();
		shell.setWidth("100%");
		shell.setStyle("background:#f6f8fb; padding:14px; box-sizing:border-box; overflow:auto;");
		shell.setParent(panelchildren);

		renderHero(shell, data);
		renderGlobalFilter(shell);
		renderMetricCards(shell, data);
		renderAnalyticLayout(shell, data);
		renderQuickInsight(shell, data);
		renderChartDashboard(shell, data);
	}


	private AkuntansiDashboardData loadDashboardData() {
		AkuntansiDashboardData data = new AkuntansiDashboardData();
		try {
			loadDashboardSummaryData(data);
			loadDashboardSaldoData(data);
			loadDashboardJenisLaporanData(data);
			loadDashboardMutasiBulananData(data);
			loadDashboardTopAkunDebetData(data);
			loadDashboardTopAkunKreditData(data);
			loadDashboardRingkasLaporanData(data);
			loadDashboardRingkasNeracaData(data);
		} catch (Exception e) {
			printDebug(e);
		}
		return data;
	}

	private void loadDashboardSummaryData(AkuntansiDashboardData data) {
		if (data == null) {
			return;
		}
		try {
			Session session = HibernateUtil.currentSession();
			Object[] summary = getSingleRow(session, buildSummarySql());
			if (summary != null) {
				data.totalBukti = toLong(summary[0]);
				data.totalBaris = toLong(summary[1]);
				data.totalDebet = toDouble(summary[2]);
				data.totalKredit = toDouble(summary[3]);
				data.saldoBersih = toDouble(summary[4]);
				data.akunTerpakai = toLong(summary[5]);
				data.jurnalPenyesuaian = toLong(summary[6]);
				data.jurnalPenutup = toLong(summary[7]);
				data.transaksiClosing = toLong(summary[8]);
			}
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void loadDashboardSaldoData(AkuntansiDashboardData data) {
		if (data == null) {
			return;
		}
		try {
			Session session = HibernateUtil.currentSession();
			Object saldoAwal = getSingleValue(session, buildSaldoSql("date(gt.tanggal_transaksi) < date(:mulai)"));
			data.saldoAwal = toDouble(saldoAwal);
			data.saldoAkhir = data.saldoAwal + data.saldoBersih;
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void loadDashboardJenisLaporanData(AkuntansiDashboardData data) {
		if (data == null) {
			return;
		}
		try {
			Session session = HibernateUtil.currentSession();
			data.laporanAktif = getJumlahJenisLaporanAktif(session);
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void loadDashboardMutasiBulananData(AkuntansiDashboardData data) {
		if (data == null) {
			return;
		}
		try {
			Session session = HibernateUtil.currentSession();
			data.mutasiBulanan = getList(session, buildMutasiBulananSql(), TOP_LIMIT);
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void loadDashboardTopAkunDebetData(AkuntansiDashboardData data) {
		if (data == null) {
			return;
		}
		try {
			Session session = HibernateUtil.currentSession();
			data.topAkunDebet = getList(session, buildTopAkunSql("sum(t.debet)", "total_debet", "t.debet <> 0"), TOP_LIMIT);
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void loadDashboardTopAkunKreditData(AkuntansiDashboardData data) {
		if (data == null) {
			return;
		}
		try {
			Session session = HibernateUtil.currentSession();
			data.topAkunKredit = getList(session, buildTopAkunSql("sum(t.kredit)", "total_kredit", "t.kredit <> 0"), TOP_LIMIT);
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void loadDashboardRingkasLaporanData(AkuntansiDashboardData data) {
		if (data == null) {
			return;
		}
		try {
			Session session = HibernateUtil.currentSession();
			data.ringkasLaporan = getList(session, buildRingkasLaporanSql(), TOP_LIMIT);
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void loadDashboardRingkasNeracaData(AkuntansiDashboardData data) {
		if (data == null) {
			return;
		}
		try {
			Session session = HibernateUtil.currentSession();
			data.ringkasNeraca = getList(session, buildRingkasNeracaSql(), TOP_LIMIT);
		} catch (Exception e) {
			printDebug(e);
		}
	}


	private int getJumlahJenisLaporanAktif(Session session) {
		try {
			Number n = (Number) session.createCriteria(JenisLaporan.class)
					.add(Restrictions.eq("tampilDiDashboard", true))
					.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
			return n == null ? 0 : n.intValue();
		} catch (Exception e) {
			printDebug(e);
			return 0;
		}
	}

	private Object[] getSingleRow(Session session, String sql) {
		try {
			Query query = session.createSQLQuery(sql);
			bindGlobalParams(query);
			Object object = query.uniqueResult();
			if (object instanceof Object[]) {
				return (Object[]) object;
			}
			if (object != null) {
				return new Object[] { object };
			}
		} catch (Exception e) {
			printDebug(e);
		}
		return null;
	}

	private Object getSingleValue(Session session, String sql) {
		try {
			Query query = session.createSQLQuery(sql);
			bindGlobalParams(query);
			return query.uniqueResult();
		} catch (Exception e) {
			printDebug(e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private List<Object[]> getList(Session session, String sql, int limit) {
		try {
			Query query = session.createSQLQuery(sql);
			bindGlobalParams(query);
			if (limit > 0) {
				query.setMaxResults(limit);
			}
			return query.list();
		} catch (Exception e) {
			printDebug(e);
			return new ArrayList<Object[]>();
		}
	}

	private void renderHero(Component parent, AkuntansiDashboardData data) {
		org.zkoss.zul.Div hero = new org.zkoss.zul.Div();
		hero.setStyle("position:relative; overflow:hidden; border-radius:18px; padding:22px 24px; color:#ffffff;"
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);"
				+ "box-shadow:0 14px 30px rgba(15,23,42,.18); box-sizing:border-box;");
		hero.setParent(parent);

		appendHtml(hero, "<div style='position:absolute; right:-70px; top:-80px; width:230px; height:230px; border-radius:999px; background:rgba(255,255,255,.12);'></div>"
				+ "<div style='position:absolute; right:105px; bottom:-70px; width:165px; height:165px; border-radius:999px; background:rgba(255,255,255,.10);'></div>");

		org.zkoss.zul.Hbox content = new org.zkoss.zul.Hbox();
		content.setWidth("100%");
		content.setPack("justify");
		content.setAlign("center");
		content.setStyle("position:relative; z-index:1; gap:16px; flex-wrap:wrap;");
		content.setParent(hero);

		org.zkoss.zul.Vbox titleBox = new org.zkoss.zul.Vbox();
		titleBox.setStyle("max-width:760px;");
		titleBox.setParent(content);

		appendHtml(titleBox, "<div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;'>Accounting Control Center</div>"
				+ "<div style='font-size:28px; line-height:1.15; font-weight:800; margin-top:6px;'>Dashboard Akuntansi</div>"
				+ "<div style='font-size:13px; opacity:.90; margin-top:8px;'>Ringkasan transaksi terposting, saldo, mutasi debet/kredit, akun aktif, laporan keuangan, dan sinyal Neraca Lajur. Klik angka untuk melihat detail data paging per 10 baris.</div>");

		String satkerText = dashboardFilterSatker == null ? "Semua Unit" : dashboardFilterSatker.getNama();
		String keywordText = dashboardFilterKeyword == null || dashboardFilterKeyword.trim().length() == 0
				? "Tanpa keyword" : dashboardFilterKeyword.trim();

		appendHtml(titleBox, "<div style='margin-top:12px; display:flex; gap:8px; flex-wrap:wrap;'>"
				+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>Periode: "
				+ escapeHtml(formatTanggal(dashboardFilterMulai)) + " s.d. " + escapeHtml(formatTanggal(dashboardFilterSampai)) + "</span>"
				+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>"
				+ escapeHtml(satkerText) + "</span>"
				+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>Cari: "
				+ escapeHtml(keywordText) + "</span>"
				+ "</div>");

		org.zkoss.zul.Hbox numberBox = new org.zkoss.zul.Hbox();
		numberBox.setStyle("gap:10px; flex-wrap:wrap;");
		numberBox.setParent(content);

		createHeroNumber(numberBox, "Saldo Awal", formatNumber(data.saldoAwal), "Detail Saldo Awal per Akun",
				createSaldoPerAkunProvider("date(gt.tanggal_transaksi) < date(:mulai)", "order by a.kode"));
		createHeroNumber(numberBox, "Saldo Akhir", formatNumber(data.saldoAkhir), "Detail Saldo Akhir per Akun",
				createSaldoPerAkunProvider("date(gt.tanggal_transaksi) <= date(:sampai)", "order by a.kode"));
	}

	private void renderGlobalFilter(final Component parent) throws Exception { 
		final org.zkoss.zul.Div filterContainer = new org.zkoss.zul.Div();
		filterContainer.setParent(parent);
		filterContainer.setStyle("margin-top:12px; padding:14px; background:#ffffff; border:1px solid #e8eef6; "
				+ "border-radius:16px; box-shadow:0 10px 26px rgba(15,23,42,0.04); box-sizing:border-box;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(filterContainer);
		toolbar.setStyle("border:0; background:transparent; padding:0; display:flex; flex-wrap:wrap; align-items:center; gap:8px;");

		new MyLabelAgakKecil("Mulai:").setParent(toolbar);
		final MyDatebox dbMulai = new MyDatebox(dashboardFilterMulai);
		dbMulai.setReadonly(true);
		dbMulai.setCols(5);
		dbMulai.setParent(toolbar);

		new MyLabelAgakKecil("Sampai:").setParent(toolbar);
		final MyDatebox dbSampai = new MyDatebox(dashboardFilterSampai);
		dbSampai.setReadonly(true);
		dbSampai.setCols(5);
		dbSampai.setParent(toolbar);

		new MyLabelAgakKecil("Unit:").setParent(toolbar);
		final AmbilDataSatuanKerjaBanbox cbSatker = new AmbilDataSatuanKerjaBanbox();
		cbSatker.setCols(8);
		cbSatker.setReadonly(true);
		cbSatker.setValue(dashboardFilterSatker == null ? "Unit" : dashboardFilterSatker.getNama());
		cbSatker.setAttribute("satuanKerja", dashboardFilterSatker);
		cbSatker.setAttribute("myValue", dashboardFilterSatker);
		cbSatker.setParent(toolbar);

		new MyLabelAgakKecil("Cari:").setParent(toolbar);
		final Textbox txtKeyword = new Textbox();
		txtKeyword.setCols(16);
		txtKeyword.setValue(dashboardFilterKeyword == null ? "" : dashboardFilterKeyword);
		txtKeyword.setTooltiptext("Cari kode bukti, kode akun, nama akun, atau keterangan transaksi");
		txtKeyword.setParent(toolbar);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan Dashboard", "/img/svg/search.svg");
		refresh.setTooltiptext("Refresh dashboard akuntansi berdasarkan filter global");
		refresh.setStyle("font-weight:bold; color:#ffffff; background:#047857; border-radius:10px; "
				+ "padding:6px 14px; margin-left:4px;");
		refresh.setParent(toolbar);

		MyToolbarbuttonConfig reset = new MyToolbarbuttonConfig("Reset", "/img/svg/refresh.svg");
		reset.setTooltiptext("Kembalikan filter ke default");
		reset.setStyle("font-weight:bold; color:#0f172a; background:#f8fafc; border-radius:10px; "
				+ "padding:6px 14px; margin-left:4px;");
		reset.setParent(toolbar);

		final EventListener refreshListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				dashboardFilterMulai = dbMulai.getValue();
				dashboardFilterSampai = dbSampai.getValue();
				dashboardFilterSatker = (SatuanKerja) cbSatker.getAttribute("satuanKerja");
				dashboardFilterKeyword = txtKeyword.getValue() == null ? "" : txtKeyword.getValue().trim();
				renderDashboard();
			}
		};

		refresh.addEventListener("onClick", refreshListener);
		txtKeyword.addEventListener("onOK", refreshListener);
		dbMulai.addEventListener("onChange", refreshListener);
		dbSampai.addEventListener("onChange", refreshListener);

		reset.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				dashboardFilterMulai = getDefaultMulai();
				dashboardFilterSampai = WaktuUtil.getDate();
				dashboardFilterSatker = null;
				dashboardFilterKeyword = "";
				renderDashboard();
			}
		});
	}

	private void renderMetricCards(Component parent, AkuntansiDashboardData data) {
		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px;");
		wrap.setParent(parent);

		createMetricCard(wrap, "Bukti / Jurnal", formatNumber(data.totalBukti), "Jumlah grup transaksi terposting",
				"#dbeafe", "#1e40af", "J", "Detail Bukti / Jurnal Terposting",
				createTransaksiDetailProvider("", "order by gt.tanggal_transaksi desc, gt.id desc, a.kode"));

		createMetricCard(wrap, "Baris Transaksi", formatNumber(data.totalBaris), "Jumlah detail transaksi",
				"#ecfeff", "#155e75", "T", "Detail Baris Transaksi",
				createTransaksiDetailProvider("", "order by gt.tanggal_transaksi desc, gt.id desc, a.kode"));

		createMetricCard(wrap, "Total Debet", formatNumber(data.totalDebet), "Mutasi debet periode terpilih",
				"#dcfce7", "#166534", "D", "Detail Mutasi Debet",
				createTransaksiDetailProvider(" and coalesce(t.debet,0) <> 0 ", "order by gt.tanggal_transaksi desc, gt.id desc, a.kode"));

		createMetricCard(wrap, "Total Kredit", formatNumber(data.totalKredit), "Mutasi kredit periode terpilih",
				"#fee2e2", "#991b1b", "K", "Detail Mutasi Kredit",
				createTransaksiDetailProvider(" and coalesce(t.kredit,0) <> 0 ", "order by gt.tanggal_transaksi desc, gt.id desc, a.kode"));

		createMetricCard(wrap, "Saldo Bersih", formatNumber(data.saldoBersih), "Debet dikurangi kredit periode",
				"#fef3c7", "#92400e", "Σ", "Detail Saldo Bersih",
				createTransaksiDetailProvider(" and coalesce(t.debet,0) - coalesce(t.kredit,0) <> 0 ", "order by gt.tanggal_transaksi desc, gt.id desc, a.kode"));

		createMetricCard(wrap, "Akun Terpakai", formatNumber(data.akunTerpakai), "Akun yang memiliki transaksi",
				"#ede9fe", "#5b21b6", "A", "Detail Akun Terpakai",
				createAkunTerpakaiProvider());

		createMetricCard(wrap, "Jurnal Penyesuaian", formatNumber(data.jurnalPenyesuaian), "Grup transaksi jenis 7",
				"#fce7f3", "#9d174d", "7", "Detail Jurnal Penyesuaian",
				createTransaksiDetailProvider(" and gt.jenis_transaksi = 7 ", "order by gt.tanggal_transaksi desc, gt.id desc, a.kode"));

		createMetricCard(wrap, "Jurnal Penutup", formatNumber(data.jurnalPenutup), "Grup transaksi jenis 9",
				"#e0e7ff", "#3730a3", "9", "Detail Jurnal Penutup",
				createTransaksiDetailProvider(" and gt.jenis_transaksi = 9 ", "order by gt.tanggal_transaksi desc, gt.id desc, a.kode"));

		createMetricCard(wrap, "Closing", formatNumber(data.transaksiClosing), "Transaksi dengan closing terisi",
				"#ccfbf1", "#115e59", "C", "Detail Transaksi Closing",
				createTransaksiDetailProvider(" and gt.closing is not null ", "order by gt.tanggal_transaksi desc, gt.id desc, a.kode"));

		createMetricCard(wrap, "Laporan Aktif", formatNumber(data.laporanAktif), "Jenis laporan tampil dashboard",
				"#ffedd5", "#9a3412", "L", "Detail Jenis Laporan Aktif",
				createJenisLaporanProvider());
	}

	private void createHeroNumber(Component parent, String label, String value, final String detailTitle,
			final SqlDetailProvider provider) {
		org.zkoss.zul.Div card = new org.zkoss.zul.Div();
		card.setStyle("min-width:142px; padding:12px 14px; border-radius:16px; background:rgba(255,255,255,.16);"
				+ "border:1px solid rgba(255,255,255,.20); text-align:center; box-sizing:border-box;");
		card.setParent(parent);

		createDetailNumber(card, value, detailTitle, provider,
				"display:block; font-size:22px; line-height:1.1; font-weight:800; color:#ffffff; text-decoration:none; cursor:pointer;");
		appendHtml(card, "<div style='font-size:11px; opacity:.85; margin-top:4px;'>" + escapeHtml(label) + "</div>");
	}

	private void createMetricCard(Component parent, String title, String value, String desc, String bg, String color,
			String icon, final String detailTitle, final SqlDetailProvider provider) {
		org.zkoss.zul.Div card = new org.zkoss.zul.Div();
		card.setStyle("flex:1 1 170px; min-width:170px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:14px;"
				+ "box-shadow:0 10px 22px rgba(15,23,42,.06); box-sizing:border-box;");
		card.setParent(parent);

		org.zkoss.zul.Hbox top = new org.zkoss.zul.Hbox();
		top.setWidth("100%");
		top.setPack("justify");
		top.setAlign("center");
		top.setParent(card);

		appendHtml(top, "<div style='width:38px; height:38px; border-radius:12px; display:flex; align-items:center; justify-content:center; font-weight:800; background:"
				+ bg + "; color:" + color + ";'>" + escapeHtml(icon) + "</div>");
		createDetailNumber(top, value, detailTitle, provider,
				"font-size:22px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer;");

		appendHtml(card, "<div style='font-size:12px; color:#64748b; margin-top:10px;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:#94a3b8; margin-top:3px;'>" + escapeHtml(desc) + "</div>");
	}

	private A createDetailNumber(Component parent, String text, final String title, final SqlDetailProvider provider,
			String style) {
		A a = new A(text == null ? "0" : text);
		a.setTooltiptext("Klik untuk melihat detail data");
		a.setStyle(style);
		a.setParent(parent);
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail(title, provider);
			}
		});
		return a;
	}

	private void viewDetail(final String title, final SqlDetailProvider provider) throws Exception {
		final Window window = new Window();
		window.setTitle(title);
		window.setClosable(true);
		window.setSizable(true);
		window.setBorder("normal");
		window.setWidth(Common.isMobile() ? "96%" : "92%");
		window.setHeight(Common.isMobile() ? "92%" : "82%");
		window.setStyle("border-radius:14px; overflow:hidden;");
		try {
			if (DasboardAkuntansi.this.getPage() != null) {
				window.setPage(DasboardAkuntansi.this.getPage());
			} else {
				window.setParent(DasboardAkuntansi.this);
			}
		} catch (Exception e) {
			printDebug(e);
			window.setParent(DasboardAkuntansi.this);
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
				int total = 0;
				List data = new ArrayList();
				try {
					Session session = HibernateUtil.currentSession();

					Query countQuery = session.createSQLQuery(provider.getCountSql());
					provider.bind(countQuery);
					Number number = (Number) countQuery.uniqueResult();
					total = number == null ? 0 : number.intValue();
					paging.setTotalSize(total);

					Query listQuery = session.createSQLQuery(provider.getListSql());
					provider.bind(listQuery);
					data = listQuery.setFirstResult(DETAIL_PAGE_SIZE * paging.getActivePage())
							.setMaxResults(DETAIL_PAGE_SIZE).list();
				} catch (Exception e) {
					printDebug(e);
					appendEmptyState(dataBox, "Terjadi error saat mengambil data detail. Aktifkan debug/debuh=true untuk melihat stacktrace.");
					return;
				}

				int start = total == 0 ? 0 : (DETAIL_PAGE_SIZE * paging.getActivePage()) + 1;
				int end = DETAIL_PAGE_SIZE * paging.getActivePage() + (data == null ? 0 : data.size());
				info.setValue("Menampilkan " + start + " - " + end + " dari " + total + " data. Page size: " + DETAIL_PAGE_SIZE + ".");

				Grid grid = new Grid();
				grid.setSclass("dgrid fgrid");
				grid.setWidth("100%");
				grid.setStyle("border:0; background:#ffffff;");
				grid.setParent(dataBox);

				Columns columns = new Columns();
				columns.setParent(grid);
				String[] headers = provider.getHeaders();
				for (int i = 0; i < headers.length; i++) {
					Column col = new Column(headers[i]);
					col.setStyle("font-weight:800; color:#0f172a; background:#f1f5f9;");
					col.setParent(columns);
				}

				Rows rows = new Rows();
				rows.setParent(grid);
				if (data == null || data.isEmpty()) {
					Row row = new Row();
					row.setParent(rows);
					org.zkoss.zul.Label lbl = new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig("Tidak ada data detail untuk indikator ini."));
					lbl.setStyle("padding:14px; color:#64748b;");
					row.appendChild(lbl);
				} else {
					provider.render(rows, data);
				}
			}
		};
		paging.addEventListener("onPaging", reload);
		reload.onEvent(null);
		window.doModal();
	}

	private void renderAnalyticLayout(Component parent, AkuntansiDashboardData data) throws Exception {
		MyPortallayout analyticLayout = new MyPortallayout();
		analyticLayout.setParent(parent);
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

		renderMutasiBulanan(pcTop);
		renderTopAkunPanel(pcLeft, "Top Akun Debet", "Akun dengan nilai debet terbesar pada periode ini.",
				"Detail Top Akun Debet", "sum(t.debet)", "total_debet", "t.debet <> 0");
		renderTopAkunPanel(pcRight, "Top Akun Kredit", "Akun dengan nilai kredit terbesar pada periode ini.",
				"Detail Top Akun Kredit", "sum(t.kredit)", "total_kredit", "t.kredit <> 0");
		renderRingkasLaporan(pcLeft);
		renderRingkasNeraca(pcRight);
		renderDetailTransaksiTerbaru(pcBottom);
	}

	private void renderMutasiBulanan(Component parent) throws Exception {
		Panel panel = createPanel(parent, "Trend Mutasi Bulanan",
				"Ringkasan debet, kredit, dan saldo bersih per bulan dari transaksi terposting. Tabel ini sudah paging per 10 baris.");
		Panelchildren body = (Panelchildren) panel.getFirstChild();
		renderPagedGrid(body, createMutasiBulananProvider());
	}

	private void renderTopAkunPanel(Component parent, String title, String desc,
			final String detailTitle, final String aggregateExpr, final String alias, final String havingCondition)
			throws Exception {
		Panel panel = createPanel(parent, title, desc + " Tabel ini sudah paging per 10 baris.");
		Panelchildren body = (Panelchildren) panel.getFirstChild();
		renderPagedGrid(body, createTopAkunProvider(detailTitle, aggregateExpr, alias, havingCondition));
	}

	private void renderRingkasLaporan(Component parent) throws Exception {
		Panel panel = createPanel(parent, "Ringkasan Laporan Keuangan",
				"Akumulasi per jenis laporan, mengikuti pola join kelompok laporan dari dashboard laporan keuangan. Tabel ini sudah paging per 10 baris.");
		Panelchildren body = (Panelchildren) panel.getFirstChild();
		renderPagedGrid(body, createRingkasLaporanProvider());
	}

	private void renderRingkasNeraca(Component parent) throws Exception {
		Panel panel = createPanel(parent, "Sinyal Neraca Lajur",
				"Ringkasan mutasi normal, penyesuaian, penutup, dan closing berdasarkan pola Neraca Lajur. Tabel ini sudah paging per 10 baris.");
		Panelchildren body = (Panelchildren) panel.getFirstChild();
		renderPagedGrid(body, createRingkasNeracaProvider());
	}

	private void renderDetailTransaksiTerbaru(Component parent) throws Exception {
		Panel panel = createPanel(parent, "Transaksi Terbaru",
				"Transaksi sesuai filter global, diurutkan dari yang terbaru. Tabel ini sudah paging per 10 baris.");
		Panelchildren body = (Panelchildren) panel.getFirstChild();
		renderPagedGrid(body,
				createTransaksiDetailProvider("", "order by gt.tanggal_transaksi desc, gt.id desc, a.kode"));
	}

	private void renderPagedGrid(final Component parent, final SqlDetailProvider provider) throws Exception {
		final org.zkoss.zul.Label info = new org.zkoss.zul.Label();
		info.setStyle("display:block; font-size:12px; font-weight:700; color:#334155; padding:8px 10px; "
				+ "background:#f8fafc; border:1px solid #e5e7eb; border-radius:10px; margin-bottom:8px;");
		info.setParent(parent);

		final Paging paging = new Paging();
		paging.setPageSize(DASHBOARD_PAGE_SIZE);
		paging.setMold("os");
		paging.setStyle("margin-bottom:8px;");
		paging.setParent(parent);

		final org.zkoss.zul.Div dataBox = new org.zkoss.zul.Div();
		dataBox.setWidth("100%");
		dataBox.setStyle("overflow:auto; background:#ffffff; border:1px solid #e5e7eb; border-radius:12px; padding:8px; box-sizing:border-box;");
		dataBox.setParent(parent);

		final EventListener reload = new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(dataBox);
				int total = 0;
				List data = new ArrayList();
				try {
					Session session = HibernateUtil.currentSession();

					Query countQuery = session.createSQLQuery(provider.getCountSql());
					provider.bind(countQuery);
					Number number = (Number) countQuery.uniqueResult();
					total = number == null ? 0 : number.intValue();
					paging.setTotalSize(total);

					Query listQuery = session.createSQLQuery(provider.getListSql());
					provider.bind(listQuery);
					data = listQuery.setFirstResult(DASHBOARD_PAGE_SIZE * paging.getActivePage())
							.setMaxResults(DASHBOARD_PAGE_SIZE).list();
				} catch (Exception e) {
					printDebug(e);
					appendEmptyState(dataBox,
							"Terjadi error saat mengambil data rekap. Aktifkan debug/debuh=true untuk melihat stacktrace.");
					return;
				}

				int start = total == 0 ? 0 : (DASHBOARD_PAGE_SIZE * paging.getActivePage()) + 1;
				int end = DASHBOARD_PAGE_SIZE * paging.getActivePage() + (data == null ? 0 : data.size());
				info.setValue("Menampilkan " + start + " - " + end + " dari " + total + " data. Page size: "
						+ DASHBOARD_PAGE_SIZE + ".");

				Grid grid = new Grid();
				grid.setSclass("dgrid fgrid");
				grid.setWidth("100%");
				grid.setStyle("border:0; background:#ffffff;");
				grid.setParent(dataBox);

				Columns columns = new Columns();
				columns.setParent(grid);
				String[] headers = provider.getHeaders();
				for (int i = 0; i < headers.length; i++) {
					appendColumn(columns, headers[i]);
				}

				Rows rows = new Rows();
				rows.setParent(grid);
				if (data == null || data.isEmpty()) {
					Row row = new Row();
					row.setParent(rows);
					org.zkoss.zul.Label lbl = new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig("Tidak ada data untuk filter ini."));
					lbl.setStyle("padding:14px; color:#64748b;");
					row.appendChild(lbl);
				} else {
					provider.render(rows, data);
				}
			}
		};
		paging.addEventListener("onPaging", reload);
		reload.onEvent(null);
	}

	private void renderQuickInsight(Component parent, AkuntansiDashboardData data) {
		org.zkoss.zul.Div box = new org.zkoss.zul.Div();
		box.setStyle("margin-top:12px; padding:14px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px;"
				+ "box-shadow:0 10px 24px rgba(15,23,42,.04);");
		box.setParent(parent);

		double selisih = data.totalDebet - data.totalKredit;
		String statusSaldo = Math.abs(selisih) < 0.00001 ? "Debet dan kredit periode ini seimbang."
				: "Ada selisih debet/kredit sebesar " + formatNumber(selisih) + ".";
		String closingText = data.transaksiClosing > 0 ? "Terdapat " + formatNumber(data.transaksiClosing)
				+ " grup transaksi yang sudah memiliki informasi closing." : "Belum ada transaksi closing pada filter ini.";

		appendHtml(box, "<div style='font-size:14px; font-weight:800; color:#0f172a;'>Quick Insight</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:6px; line-height:1.6;'>"
				+ escapeHtml(statusSaldo) + "<br/>"
				+ "Saldo akhir = saldo awal + saldo bersih periode: <b>" + escapeHtml(formatNumber(data.saldoAkhir)) + "</b>.<br/>"
				+ escapeHtml(closingText) + "<br/>"
				+ "Jumlah jenis laporan yang aktif tampil di dashboard: <b>" + escapeHtml(formatNumber(data.laporanAktif)) + "</b>."
				+ "</div>");
	}

	private Panel createPanel(Component parent, String title, String subtitle) {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setTitle(title);
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:16px; overflow:hidden;"
				+ "background:#ffffff; box-shadow:0 10px 24px rgba(15,23,42,.05);");
		panel.setParent(parent);

		Panelchildren body = new Panelchildren();
		body.setStyle("padding:12px; background:#ffffff;");
		body.setParent(panel);

		appendHtml(body, buildPanelDescriptionHtml(title, subtitle));
		return panel;
	}

	private String buildPanelDescriptionHtml(String title, String subtitle) {
		String text = subtitle == null || subtitle.trim().length() == 0 ? getDefaultPanelDescription(title) : subtitle.trim();
		return "<div style='font-size:12px; color:#475569; margin-bottom:12px; line-height:1.55; padding:10px 12px;"
				+ "border-radius:12px; background:#f8fafc; border:1px solid #e2e8f0;'>"
				+ "<b style='color:#0f172a;'></b> " + escapeHtml(text) + "</div>";
	}

	private String getDefaultPanelDescription(String title) {
		if (title == null) {
			return "membantu menampilkan ringkasan data akuntansi agar pengguna dapat memahami kondisi utama tanpa membaca seluruh tabel satu per satu.";
		}
		String t = title.toLowerCase(java.util.Locale.ENGLISH);
		if (t.indexOf("tren") >= 0 || t.indexOf("trend") >= 0 || t.indexOf("bulanan") >= 0) {
			return "memperlihatkan perubahan nilai transaksi dari waktu ke waktu, sehingga pengguna dapat mengetahui bulan yang ramai transaksi.";
		}
		if (t.indexOf("debet") >= 0) {
			return "menampilkan akun dengan nilai debet terbesar agar pengguna mudah melihat sumber penambahan nilai pada periode berjalan.";
		}
		if (t.indexOf("kredit") >= 0) {
			return "menampilkan akun dengan nilai kredit terbesar agar pengguna mudah melihat sumber pengurangan nilai pada periode berjalan.";
		}
		if (t.indexOf("saldo") >= 0) {
			return "membantu melihat posisi saldo awal, perubahan periode berjalan, dan saldo akhir dengan tampilan yang mudah dibaca.";
		}
		if (t.indexOf("neraca") >= 0) {
			return "membantu mengecek keseimbangan debet dan kredit pada kelompok transaksi penting seperti mutasi normal, penyesuaian, penutup, dan closing.";
		}
		return "membantu menampilkan ringkasan data akuntansi penting dalam bentuk sederhana agar mudah dipahami oleh pengguna.";
	}

	private void renderChartDashboard(Component parent, AkuntansiDashboardData data) throws Exception {
		appendHtml(parent, "<div style='margin-top:16px; margin-bottom:8px; padding:12px 14px; border-radius:16px; "
				+ "background:#ffffff; border:1px solid #e5e7eb; box-shadow:0 10px 24px rgba(15,23,42,.04);'>"
				+ "<div style='font-size:14px; font-weight:800; color:#0f172a;'>Grafik Akuntansi HTML/CSS</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:4px;'>Visualisasi ini dibuat dengan HTML dan CSS modern agar ringan, cepat tampil, dan tetap mudah dibaca tanpa komponen chart lama.</div>"
				+ "</div>");

		MyPortallayout chartLayout = new MyPortallayout();
		chartLayout.setWidth("100%");
		chartLayout.setMaximizedMode("whole");
		chartLayout.setStyle("margin-top:8px; padding:0; background:transparent;");
		chartLayout.setParent(parent);

		String pcWidth = Common.isMobile() ? "100%" : "50%";

		MyPortalchildren pcTop = new MyPortalchildren();
		pcTop.setWidth("100%");
		pcTop.setStyle("padding:6px; box-sizing:border-box;");
		pcTop.setParent(chartLayout);

		MyPortalchildren pcLeft = new MyPortalchildren();
		pcLeft.setWidth(pcWidth);
		pcLeft.setStyle("padding:6px; box-sizing:border-box;");
		pcLeft.setParent(chartLayout);

		MyPortalchildren pcRight = new MyPortalchildren();
		pcRight.setWidth(pcWidth);
		pcRight.setStyle("padding:6px; box-sizing:border-box;");
		pcRight.setParent(chartLayout);

		MyPortalchildren pcBottomLeft = new MyPortalchildren();
		pcBottomLeft.setWidth(pcWidth);
		pcBottomLeft.setStyle("padding:6px; box-sizing:border-box;");
		pcBottomLeft.setParent(chartLayout);

		MyPortalchildren pcBottomRight = new MyPortalchildren();
		pcBottomRight.setWidth(pcWidth);
		pcBottomRight.setStyle("padding:6px; box-sizing:border-box;");
		pcBottomRight.setParent(chartLayout);

		renderChartMutasiBulanan(pcTop, data);
		renderChartKomposisiAkun(pcLeft, "Komposisi Debet per Akun", data == null ? null : data.topAkunDebet, true);
		renderChartKomposisiAkun(pcRight, "Komposisi Kredit per Akun", data == null ? null : data.topAkunKredit, false);
		renderChartNeracaLajur(pcBottomLeft, data);
		renderChartSaldoBersih(pcBottomRight, data);
		renderRadarKesehatanAkuntansi(pcTop, data);
	}

	private void renderChartMutasiBulanan(Component parent, AkuntansiDashboardData data) {
		Panel panel = createPanel(parent, "Tren Mutasi Bulanan",
				"memperlihatkan perbandingan debet, kredit, dan saldo bersih per bulan. Pengguna dapat melihat bulan yang paling aktif dan arah pergerakan saldo.");
		Panelchildren body = (Panelchildren) panel.getFirstChild();
		try {
			if (data == null || data.mutasiBulanan == null || data.mutasiBulanan.isEmpty()) {
				appendEmptyState(body, "Belum ada data mutasi bulanan untuk ditampilkan sebagai grafik.");
				return;
			}
			appendHtml(body, buildMutasiBulananHtml(data.mutasiBulanan));
		} catch (Exception e) {
			printDebug(e);
			appendEmptyState(body, "Tren mutasi bulanan belum dapat dimuat.");
		}
	}

	private String buildMutasiBulananHtml(List<Object[]> rows) {
		double max = 1.0;
		for (int i = 0; rows != null && i < rows.size(); i++) {
			Object[] row = rows.get(i);
			max = Math.max(max, Math.abs(toDouble(row[2])));
			max = Math.max(max, Math.abs(toDouble(row[3])));
			max = Math.max(max, Math.abs(toDouble(row[4])));
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='overflow:auto; padding:6px 2px 2px 2px;'>");
		sb.append("<div style='display:flex; gap:12px; align-items:flex-end; min-height:230px; min-width:720px;'>");
		for (int i = rows.size() - 1; i >= 0; i--) {
			Object[] row = rows.get(i);
			String bulan = safeString(row[0]);
			double debet = toDouble(row[2]);
			double kredit = toDouble(row[3]);
			double saldo = toDouble(row[4]);
			int hDebet = htmlPercent(Math.abs(debet), max, 150, 8);
			int hKredit = htmlPercent(Math.abs(kredit), max, 150, 8);
			int hSaldo = htmlPercent(Math.abs(saldo), max, 150, 8);
			String saldoColor = saldo < 0.0 ? "#dc2626" : "#2563eb";
			sb.append("<div style='min-width:74px; text-align:center;'>");
			sb.append("<div style='height:170px; display:flex; align-items:flex-end; justify-content:center; gap:4px;'>");
			sb.append("<div title='Debet ").append(escapeHtml(formatNumber(debet))).append("' style='width:14px; height:")
					.append(hDebet).append("px; border-radius:8px 8px 3px 3px; background:#16a34a;'></div>");
			sb.append("<div title='Kredit ").append(escapeHtml(formatNumber(kredit))).append("' style='width:14px; height:")
					.append(hKredit).append("px; border-radius:8px 8px 3px 3px; background:#dc2626;'></div>");
			sb.append("<div title='Saldo ").append(escapeHtml(formatNumber(saldo))).append("' style='width:14px; height:")
					.append(hSaldo).append("px; border-radius:8px 8px 3px 3px; background:").append(saldoColor).append(";'></div>");
			sb.append("</div><div style='font-size:10px; color:#64748b; margin-top:5px; white-space:nowrap;'>").append(escapeHtml(bulan)).append("</div></div>");
		}
		sb.append("</div><div style='margin-top:10px; display:flex; gap:10px; flex-wrap:wrap; font-size:11px; color:#64748b;'>")
				.append("<span><b style='color:#16a34a;'>■</b> Debet</span>")
				.append("<span><b style='color:#dc2626;'>■</b> Kredit</span>")
				.append("<span><b style='color:#2563eb;'>■</b> Saldo positif</span>")
				.append("<span><b style='color:#dc2626;'>■</b> Saldo negatif</span></div></div>");
		return sb.toString();
	}

	private void renderChartKomposisiAkun(Component parent, String title, List<Object[]> rows, boolean debet) {
		Panel panel = createPanel(parent, title,
				debet ? "menampilkan akun dengan kontribusi debet terbesar. Informasi ini membantu pengguna mengetahui sumber penambahan nilai yang dominan."
						: "menampilkan akun dengan kontribusi kredit terbesar. Informasi ini membantu pengguna mengetahui sumber pengurangan nilai yang dominan.");
		Panelchildren body = (Panelchildren) panel.getFirstChild();
		try {
			if (rows == null || rows.isEmpty()) {
				appendEmptyState(body, "Belum ada data akun untuk ditampilkan sebagai grafik.");
				return;
			}
			appendHtml(body, buildKomposisiAkunHtml(rows, debet));
		} catch (Exception e) {
			printDebug(e);
			appendEmptyState(body, "Komposisi akun belum dapat dimuat.");
		}
	}

	private String buildKomposisiAkunHtml(List<Object[]> rows, boolean debet) {
		double total = 0.0;
		for (Iterator<Object[]> it = rows.iterator(); it.hasNext();) {
			Object[] row = it.next();
			total += Math.abs(debet ? toDouble(row[3]) : toDouble(row[4]));
		}
		if (total <= 0.0) {
			return "<div style='padding:12px; border-radius:12px; background:#f8fafc; color:#64748b; font-size:12px;'>Belum ada nilai komposisi yang dapat divisualisasikan.</div>";
		}
		String[] colors = new String[] { "#2563eb", "#16a34a", "#f59e0b", "#dc2626", "#7c3aed", "#0891b2", "#db2777", "#65a30d", "#ea580c", "#475569" };
		StringBuilder list = new StringBuilder();
		for (int i = 0; i < rows.size(); i++) {
			Object[] row = rows.get(i);
			double nilai = Math.abs(debet ? toDouble(row[3]) : toDouble(row[4]));
			if (nilai <= 0.0) {
				continue;
			}
			int pct = htmlPercent(nilai, total, 100, 1);
			String color = colors[i % colors.length];
			String label = safeString(row[0]) + " - " + safeString(row[1]);
			list.append("<div style='margin-bottom:8px;'>")
					.append("<div style='display:flex; justify-content:space-between; gap:8px; font-size:11px; color:#0f172a;'>")
					.append("<span style='font-weight:700;'>").append(escapeHtml(label)).append("</span>")
					.append("<span>").append(escapeHtml(formatNumber(nilai))).append("</span></div>")
					.append("<div style='height:8px; border-radius:999px; background:#e2e8f0; overflow:hidden; margin-top:3px;'>")
					.append("<div style='height:8px; width:").append(pct).append("%; background:").append(color).append("; border-radius:999px;'></div></div></div>");
		}
		return "<div style='padding:4px;'>" + list.toString() + "</div>";
	}

	private void renderChartNeracaLajur(Component parent, AkuntansiDashboardData data) {
		Panel panel = createPanel(parent, "Sinyal Neraca Lajur",
				"membandingkan debet dan kredit pada mutasi normal, penyesuaian, penutup, dan closing. Tujuannya membantu pengguna melihat area yang perlu dicek keseimbangannya.");
		Panelchildren body = (Panelchildren) panel.getFirstChild();
		try {
			if (data == null || data.ringkasNeraca == null || data.ringkasNeraca.isEmpty()) {
				appendEmptyState(body, "Belum ada data Neraca Lajur untuk ditampilkan sebagai grafik.");
				return;
			}
			appendHtml(body, buildNeracaLajurHtml(data.ringkasNeraca));
		} catch (Exception e) {
			printDebug(e);
			appendEmptyState(body, "Sinyal Neraca Lajur belum dapat dimuat.");
		}
	}

	private String buildNeracaLajurHtml(List<Object[]> rows) {
		double max = 1.0;
		for (Iterator<Object[]> it = rows.iterator(); it.hasNext();) {
			Object[] row = it.next();
			max = Math.max(max, Math.abs(toDouble(row[1])));
			max = Math.max(max, Math.abs(toDouble(row[2])));
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:flex; flex-direction:column; gap:10px;'>");
		for (Iterator<Object[]> it = rows.iterator(); it.hasNext();) {
			Object[] row = it.next();
			String label = safeString(row[0]);
			double debet = toDouble(row[1]);
			double kredit = toDouble(row[2]);
			int pDebet = htmlPercent(Math.abs(debet), max, 100, 0);
			int pKredit = htmlPercent(Math.abs(kredit), max, 100, 0);
			sb.append("<div style='padding:10px; border:1px solid #e2e8f0; border-radius:12px; background:#f8fafc;'>")
					.append("<div style='font-size:12px; font-weight:800; color:#0f172a; margin-bottom:6px;'>").append(escapeHtml(label)).append("</div>")
					.append("<div style='display:grid; grid-template-columns:70px 1fr 95px; gap:8px; align-items:center; font-size:11px; color:#64748b;'>")
					.append("<span>Debet</span><div style='height:9px; border-radius:999px; background:#e2e8f0; overflow:hidden;'><div style='height:9px; width:")
					.append(pDebet).append("%; background:#16a34a;'></div></div><span style='text-align:right;'>").append(escapeHtml(formatNumber(debet))).append("</span>")
					.append("<span>Kredit</span><div style='height:9px; border-radius:999px; background:#e2e8f0; overflow:hidden;'><div style='height:9px; width:")
					.append(pKredit).append("%; background:#dc2626;'></div></div><span style='text-align:right;'>").append(escapeHtml(formatNumber(kredit))).append("</span></div></div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private void renderChartSaldoBersih(Component parent, AkuntansiDashboardData data) {
		Panel panel = createPanel(parent, "Saldo Bersih Bulanan",
				"menunjukkan pergerakan saldo bersih dari mutasi transaksi per bulan. Nilai positif dan negatif dibuat berbeda agar mudah dikenali.");
		Panelchildren body = (Panelchildren) panel.getFirstChild();
		try {
			if (data == null || data.mutasiBulanan == null || data.mutasiBulanan.isEmpty()) {
				appendEmptyState(body, "Belum ada data saldo bersih untuk ditampilkan sebagai grafik.");
				return;
			}
			appendHtml(body, buildSaldoBersihHtml(data.mutasiBulanan));
		} catch (Exception e) {
			printDebug(e);
			appendEmptyState(body, "Saldo bersih bulanan belum dapat dimuat.");
		}
	}

	private String buildSaldoBersihHtml(List<Object[]> rows) {
		double max = 1.0;
		for (int i = 0; rows != null && i < rows.size(); i++) {
			Object[] row = rows.get(i);
			max = Math.max(max, Math.abs(toDouble(row[4])));
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='overflow:auto; padding:8px 2px;'><div style='display:flex; align-items:center; gap:10px; min-width:620px;'>");
		for (int i = rows.size() - 1; i >= 0; i--) {
			Object[] row = rows.get(i);
			double saldo = toDouble(row[4]);
			int h = htmlPercent(Math.abs(saldo), max, 115, 10);
			String color = saldo < 0.0 ? "#dc2626" : "#2563eb";
			sb.append("<div style='min-width:72px; text-align:center;'>")
					.append("<div style='height:140px; display:flex; align-items:flex-end; justify-content:center;'>")
					.append("<div title='").append(escapeHtml(formatNumber(saldo))).append("' style='width:42px; height:")
					.append(h).append("px; background:").append(color)
					.append("; border-radius:12px 12px 4px 4px; box-shadow:0 8px 18px rgba(15,23,42,.12);'></div></div>")
					.append("<div style='font-size:10px; color:#64748b; margin-top:5px;'>").append(escapeHtml(safeString(row[0]))).append("</div></div>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	private void renderRadarKesehatanAkuntansi(Component parent, AkuntansiDashboardData data) {
		Panel panel = createPanel(parent, "Radar Kesehatan Akuntansi",
				"Ringkasan ini merangkum keseimbangan debet-kredit, aktivitas jurnal, pemakaian akun, dan kesiapan laporan. Semakin seimbang nilainya, semakin baik kondisi akuntansi.");
		Panelchildren body = (Panelchildren) panel.getFirstChild();
		try {
			if (data == null) {
				appendEmptyState(body, "Belum ada data radar kesehatan akuntansi.");
				return;
			}
			double maxMutasi = Math.max(Math.abs(data.totalDebet), Math.abs(data.totalKredit));
			int seimbang = 100 - htmlPercent(Math.abs(data.totalDebet - data.totalKredit), maxMutasi <= 0.0 ? 1.0 : maxMutasi, 100, 0);
			int aktivitas = htmlPercent(data.totalBaris, Math.max(1.0, data.totalBaris + 20.0), 100, 0);
			int akun = htmlPercent(data.akunTerpakai, Math.max(1.0, data.akunTerpakai + 10.0), 100, 0);
			int laporan = htmlPercent(data.laporanAktif, Math.max(1.0, data.laporanAktif + 3.0), 100, 0);
			int jurnal = htmlPercent(data.jurnalPenyesuaian + data.jurnalPenutup + data.transaksiClosing,
					Math.max(1.0, data.totalBukti + data.jurnalPenyesuaian + data.jurnalPenutup + data.transaksiClosing), 100, 0);
			appendHtml(body, buildRadarHtml(seimbang, aktivitas, akun, laporan, jurnal));
		} catch (Exception e) {
			printDebug(e);
			appendEmptyState(body, "Radar kesehatan akuntansi belum dapat dimuat.");
		}
	}

	private String buildRadarHtml(int seimbang, int aktivitas, int akun, int laporan, int jurnal) {
		return "<div style='display:grid; grid-template-columns:180px 1fr; gap:16px; align-items:center;'>"
				+ "<div style='width:170px; height:170px; border-radius:999px; position:relative; background:conic-gradient(#16a34a 0 " + seimbang + "%,#e2e8f0 " + seimbang + "% 100%);"
				+ "box-shadow:inset 0 0 0 18px #ffffff, 0 10px 22px rgba(15,23,42,.08);'>"
				+ "<div style='position:absolute; inset:42px; border-radius:999px; background:conic-gradient(#2563eb 0 " + aktivitas + "%,#e2e8f0 " + aktivitas + "% 100%);'></div>"
				+ "<div style='position:absolute; inset:70px; border-radius:999px; background:#ffffff; display:flex; align-items:center; justify-content:center; font-size:20px; font-weight:900; color:#0f172a;'>" + seimbang + "%</div></div>"
				+ "<div>" + buildRadarRow("Keseimbangan Debet-Kredit", seimbang, "#16a34a")
				+ buildRadarRow("Aktivitas Jurnal", aktivitas, "#2563eb")
				+ buildRadarRow("Pemakaian Akun", akun, "#7c3aed")
				+ buildRadarRow("Kesiapan Laporan", laporan, "#f59e0b")
				+ buildRadarRow("Jurnal Khusus / Closing", jurnal, "#dc2626") + "</div></div>";
	}

	private String buildRadarRow(String label, int value, String color) {
		int v = value < 0 ? 0 : (value > 100 ? 100 : value);
		return "<div style='margin-bottom:9px;'><div style='display:flex; justify-content:space-between; font-size:11px; font-weight:800; color:#0f172a;'>"
				+ "<span>" + escapeHtml(label) + "</span><span>" + v + "%</span></div>"
				+ "<div style='height:9px; border-radius:999px; background:#e2e8f0; overflow:hidden; margin-top:4px;'>"
				+ "<div style='height:9px; width:" + v + "%; background:" + color + "; border-radius:999px;'></div></div></div>";
	}

	private int htmlPercent(double value, double total, int maxPercent, int minPercent) {
		if (total <= 0.0) {
			return 0;
		}
		int p = (int) Math.round((Math.abs(value) * maxPercent) / Math.abs(total));
		if (p < minPercent && value > 0.0) {
			p = minPercent;
		}
		if (p > maxPercent) {
			p = maxPercent;
		}
		if (p < 0) {
			p = 0;
		}
		return p;
	}


	/* ========================= SQL BUILDER ========================= */

	private String buildSummarySql() {
		return "select count(distinct gt.id) as total_bukti, "
				+ "count(t.id) as total_baris, "
				+ "coalesce(sum(t.debet),0) as total_debet, "
				+ "coalesce(sum(t.kredit),0) as total_kredit, "
				+ "coalesce(sum(t.debet-t.kredit),0) as saldo_bersih, "
				+ "count(distinct t.akun) as akun_terpakai, "
				+ "count(distinct case when gt.jenis_transaksi=7 then gt.id else null end) as jurnal_penyesuaian, "
				+ "count(distinct case when gt.jenis_transaksi=9 then gt.id else null end) as jurnal_penutup, "
				+ "count(distinct case when gt.closing is not null then gt.id else null end) as transaksi_closing "
				+ buildBaseFrom()
				+ buildGlobalWhere("");
	}

	private String buildSaldoSql(String dateCondition) {
		return "select coalesce(sum(t.debet-t.kredit),0) "
				+ buildBaseFrom()
				+ buildGlobalWhereWithoutDate(" and " + dateCondition);
	}

	private String buildMutasiBulananBaseSql() {
		return "select to_char(gt.tanggal_transaksi,'YYYY-MM') as bulan, "
				+ "count(distinct gt.id) as total_bukti, "
				+ "coalesce(sum(t.debet),0) as debet, "
				+ "coalesce(sum(t.kredit),0) as kredit, "
				+ "coalesce(sum(t.debet-t.kredit),0) as saldo "
				+ buildBaseFrom()
				+ buildGlobalWhere("")
				+ " group by to_char(gt.tanggal_transaksi,'YYYY-MM') ";
	}

	private String buildMutasiBulananSql() {
		return buildMutasiBulananBaseSql() + " order by to_char(gt.tanggal_transaksi,'YYYY-MM') desc";
	}

	private String buildTopAkunBaseSql(String aggregateExpr, String alias, String havingCondition) {
		return "select coalesce(a.kode,'-') as kode_akun, coalesce(a.nama,'Tanpa Akun') as nama_akun, "
				+ "count(distinct gt.id) as total_bukti, "
				+ "coalesce(sum(t.debet),0) as debet, "
				+ "coalesce(sum(t.kredit),0) as kredit, "
				+ "coalesce(sum(t.debet-t.kredit),0) as saldo, "
				+ "coalesce(" + aggregateExpr + ",0) as " + alias + " "
				+ buildBaseFrom()
				+ buildGlobalWhere(" and " + havingCondition)
				+ " group by a.id, a.kode, a.nama ";
	}

	private String buildTopAkunSql(String aggregateExpr, String alias, String havingCondition) {
		return buildTopAkunBaseSql(aggregateExpr, alias, havingCondition) + " order by " + alias + " desc, kode_akun";
	}

	private String buildRingkasLaporanBaseSql() {
		return "select coalesce(max(f.nama), max(f.keterangan), 'Laporan') as jenis_laporan, "
				+ "coalesce(max(c.keterangan), max(c.keterangan1), '-') as kelompok_laporan, "
				+ "coalesce(sum(t.debet),0) as debet, "
				+ "coalesce(sum(t.kredit),0) as kredit, "
				+ "coalesce(sum(t.debet-t.kredit),0) as saldo "
				+ " from akunting.transaksi t "
				+ " inner join akunting.grup_transaksi gt on gt.id=t.grup_transaksi "
				+ getPostingJoin("gt")
				+ " inner join akunting.kelompok_laporan_punya_akun kpa on t.akun = kpa.akun "
				+ " inner join akunting.kelompok_laporan c on c.id = kpa.kelompok_laporan "
				+ " inner join akunting.jenis_laporan f on f.id = c.jenis_laporan "
				+ " left join akunting.akun a on a.id=t.akun "
				+ " where (c.aktif is null or c.aktif) "
				+ " and gt.posting_history is not null "
				+ " and date(gt.tanggal_transaksi) between date(:mulai) and date(:sampai) "
				+ " and case when :satker = -1 then true else gt.satuan_kerja = :satker end "
				+ " and case when :keyword = '' then true else (coalesce(gt.kode,'') ilike :keywordLike "
				+ " or coalesce(a.kode,'') ilike :keywordLike or coalesce(a.nama,'') ilike :keywordLike "
				+ " or coalesce(t.keterangan,'') ilike :keywordLike) end "
				+ " group by f.id, c.id ";
	}

	private String buildRingkasLaporanSql() {
		return buildRingkasLaporanBaseSql() + " order by f.id, c.id";
	}

	private String buildRingkasNeracaSql() {
		String base = buildBaseFrom() + buildGlobalWhere("");
		return "select 'Mutasi Normal' as kategori, "
				+ "coalesce(sum(case when (gt.jenis_transaksi != 7 or gt.jenis_transaksi is null) and (gt.jenis_transaksi != 9 or gt.jenis_transaksi is null) then t.debet else 0 end),0) as debet, "
				+ "coalesce(sum(case when (gt.jenis_transaksi != 7 or gt.jenis_transaksi is null) and (gt.jenis_transaksi != 9 or gt.jenis_transaksi is null) then t.kredit else 0 end),0) as kredit, "
				+ "coalesce(sum(case when (gt.jenis_transaksi != 7 or gt.jenis_transaksi is null) and (gt.jenis_transaksi != 9 or gt.jenis_transaksi is null) then t.debet-t.kredit else 0 end),0) as saldo "
				+ base
				+ " union all "
				+ "select 'Jurnal Penyesuaian' as kategori, "
				+ "coalesce(sum(case when gt.jenis_transaksi=7 then t.debet else 0 end),0) as debet, "
				+ "coalesce(sum(case when gt.jenis_transaksi=7 then t.kredit else 0 end),0) as kredit, "
				+ "coalesce(sum(case when gt.jenis_transaksi=7 then t.debet-t.kredit else 0 end),0) as saldo "
				+ base
				+ " union all "
				+ "select 'Jurnal Penutup' as kategori, "
				+ "coalesce(sum(case when gt.jenis_transaksi=9 then t.debet else 0 end),0) as debet, "
				+ "coalesce(sum(case when gt.jenis_transaksi=9 then t.kredit else 0 end),0) as kredit, "
				+ "coalesce(sum(case when gt.jenis_transaksi=9 then t.debet-t.kredit else 0 end),0) as saldo "
				+ base
				+ " union all "
				+ "select 'Closing' as kategori, "
				+ "coalesce(sum(case when gt.closing is not null then t.debet else 0 end),0) as debet, "
				+ "coalesce(sum(case when gt.closing is not null then t.kredit else 0 end),0) as kredit, "
				+ "coalesce(sum(case when gt.closing is not null then t.debet-t.kredit else 0 end),0) as saldo "
				+ base;
	}

	private String buildBaseFrom() {
		return " from akunting.transaksi t "
				+ " inner join akunting.grup_transaksi gt on gt.id=t.grup_transaksi "
				+ getPostingJoin("gt")
				+ " left join akunting.akun a on a.id=t.akun ";
	}

	private String getPostingJoin(String grupTransaksiAlias) {
		if (!ConstantValues.otomatisTerposting) {
			return " inner join akunting.posting_history ph on (ph.id=" + grupTransaksiAlias
					+ ".posting_history and ph.posting=true) ";
		}
		return "";
	}

	private String buildGlobalWhere(String extraWhere) {
		return " where gt.posting_history is not null "
				+ " and t.akun is not null "
				+ " and date(gt.tanggal_transaksi) between date(:mulai) and date(:sampai) "
				+ " and case when :satker = -1 then true else gt.satuan_kerja = :satker end "
				+ buildKeywordWhere()
				+ (extraWhere == null ? "" : extraWhere);
	}

	private String buildGlobalWhereWithoutDate(String extraWhere) {
		return " where gt.posting_history is not null "
				+ " and t.akun is not null "
				+ " and date(:sampai) is not null "
				+ " and case when :satker = -1 then true else gt.satuan_kerja = :satker end "
				+ buildKeywordWhere()
				+ (extraWhere == null ? "" : extraWhere);
	}

	private String buildKeywordWhere() {
		return " and case when :keyword = '' then true else (coalesce(gt.kode,'') ilike :keywordLike "
				+ " or coalesce(a.kode,'') ilike :keywordLike "
				+ " or coalesce(a.nama,'') ilike :keywordLike "
				+ " or coalesce(t.keterangan,'') ilike :keywordLike) end ";
	}

	private String buildTransaksiDetailBaseSql(String extraWhere, String orderSql) {
		return "select coalesce(gt.kode,'') as kode_bukti, "
				+ "gt.tanggal_transaksi, "
				+ "coalesce(a.kode,'') as kode_akun, "
				+ "coalesce(a.nama,'') as nama_akun, "
				+ "coalesce(t.keterangan,'') as keterangan, "
				+ "coalesce(t.debet,0) as debet, "
				+ "coalesce(t.kredit,0) as kredit, "
				+ "coalesce(t.debet-t.kredit,0) as saldo, "
				+ "gt.id as id_grup, "
				+ "a.id as id_akun "
				+ buildBaseFrom()
				+ buildGlobalWhere(extraWhere)
				+ " " + (orderSql == null ? " order by gt.tanggal_transaksi desc, gt.id desc, a.kode " : orderSql);
	}

	private void bindGlobalParams(Query query) {
		Date mulai = dashboardFilterMulai == null ? getDefaultMulai() : dashboardFilterMulai;
		Date sampai = dashboardFilterSampai == null ? WaktuUtil.getDate() : dashboardFilterSampai;
		SatuanKerja satuanKerja = dashboardFilterSatker;
		Long satkerId = satuanKerja == null || satuanKerja.getId() == null ? Long.valueOf(-1L) : satuanKerja.getId();
		String keyword = dashboardFilterKeyword == null ? "" : dashboardFilterKeyword.trim();

		/*
		 * Beberapa query dashboard tidak selalu memakai semua parameter global.
		 * Contoh: saldo akhir hanya memakai :sampai, :satker, :keyword, :keywordLike
		 * dan tidak memiliki :mulai. Pada Hibernate lama, memanggil setString()
		 * untuk parameter yang tidak ada akan menimbulkan IllegalArgumentException.
		 * Karena itu binding dibuat selektif berdasarkan named parameter yang benar-benar ada.
		 */
		if (hasNamedParameter(query, "mulai")) {
			query.setString("mulai", Common.databaseDateFormat.get().format(mulai));
		}
		if (hasNamedParameter(query, "sampai")) {
			query.setString("sampai", Common.databaseDateFormat.get().format(sampai));
		}
		if (hasNamedParameter(query, "satker")) {
			query.setLong("satker", satkerId.longValue());
		}
		if (hasNamedParameter(query, "keyword")) {
			query.setString("keyword", keyword);
		}
		if (hasNamedParameter(query, "keywordLike")) {
			query.setString("keywordLike", "%" + keyword + "%");
		}
	}

	private boolean hasNamedParameter(Query query, String name) {
		if (query == null || name == null) {
			return false;
		}
		try {
			String[] names = query.getNamedParameters();
			if (names == null) {
				return false;
			}
			for (int i = 0; i < names.length; i++) {
				if (name.equals(names[i])) {
					return true;
				}
			}
		} catch (Exception e) {
			printDebug(e);
		}
		return false;
	}

	/* ========================= PROVIDER DETAIL ========================= */

	private SqlDetailProvider createTransaksiDetailProvider(final String extraWhere, final String orderSql) {
		return new BaseSqlDetailProvider() {
			@Override
			public String getBaseSql() {
				return buildTransaksiDetailBaseSql(extraWhere, "");
			}

			@Override
			public String getListSql() {
				return buildTransaksiDetailBaseSql(extraWhere, orderSql);
			}

			@Override
			public String[] getHeaders() {
				return getTransaksiHeaders();
			}

			@Override
			@SuppressWarnings("unchecked")
			public void render(Rows rows, List data) throws Exception {
				renderTransaksiRows(rows, data);
			}
		};
	}

	private SqlDetailProvider createAkunTerpakaiProvider() {
		return new BaseSqlDetailProvider() {
			@Override
			public String getBaseSql() {
				return "select coalesce(a.kode,'') as kode_akun, coalesce(a.nama,'') as nama_akun, "
						+ "count(distinct gt.id) as bukti, coalesce(sum(t.debet),0) as debet, "
						+ "coalesce(sum(t.kredit),0) as kredit, coalesce(sum(t.debet-t.kredit),0) as saldo "
						+ buildBaseFrom()
						+ buildGlobalWhere("")
						+ " group by a.id, a.kode, a.nama ";
			}

			@Override
			public String getListSql() {
				return getBaseSql() + " order by a.kode ";
			}

			@Override
			public String[] getHeaders() {
				return new String[] { "Kode", "Nama Akun", "Bukti", "Debet", "Kredit", "Saldo" };
			}

			@Override
			@SuppressWarnings("unchecked")
			public void render(Rows rows, List data) throws Exception {
				for (Iterator<Object[]> it = data.iterator(); it.hasNext();) {
					Object[] o = it.next();
					Row row = new Row();
					row.setParent(rows);
					addGridCell(row, o[0]+"");
					addGridCell(row, o[1]+"");
					addGridCell(row, formatNumber(toDouble(o[2])), "right");
					addGridCell(row, formatNumber(toDouble(o[3])), "right");
					addGridCell(row, formatNumber(toDouble(o[4])), "right");
					addGridCell(row, formatNumber(toDouble(o[5])), "right");
				}
			}

			
		};
	}
	

	private SqlDetailProvider createSaldoPerAkunProvider(final String dateCondition, final String orderSql) {
		return new BaseSqlDetailProvider() {
			@Override
			public String getBaseSql() {
				return "select coalesce(a.kode,'') as kode_akun, coalesce(a.nama,'') as nama_akun, "
						+ "coalesce(sum(t.debet),0) as debet, coalesce(sum(t.kredit),0) as kredit, "
						+ "coalesce(sum(t.debet-t.kredit),0) as saldo "
						+ buildBaseFrom()
						+ buildGlobalWhereWithoutDate(" and " + dateCondition)
						+ " group by a.id, a.kode, a.nama "
						+ " having coalesce(sum(t.debet-t.kredit),0) <> 0 ";
			}

			@Override
			public String getListSql() {
				return getBaseSql() + " " + orderSql;
			}

			@Override
			public String[] getHeaders() {
				return new String[] { "Kode", "Nama Akun", "Debet", "Kredit", "Saldo" };
			}

			@Override
			@SuppressWarnings("unchecked")
			public void render(Rows rows, List data) throws Exception {
				for (Iterator<Object[]> it = data.iterator(); it.hasNext();) {
					Object[] o = it.next();
					Row row = new Row();
					row.setParent(rows);
					addGridCell(row, o[0]+"");
					addGridCell(row, o[1]+"");
					addGridCell(row, formatNumber(toDouble(o[2])), "right");
					addGridCell(row, formatNumber(toDouble(o[3])), "right");
					addGridCell(row, formatNumber(toDouble(o[4])), "right");
				}
			}
		};
	}

	private SqlDetailProvider createJenisLaporanProvider() {
		return new BaseSqlDetailProvider() {
			@Override
			public String getBaseSql() {
				return "select id, coalesce(nama,'') as nama, coalesce(keterangan,'') as keterangan "
						+ " from akunting.jenis_laporan "
						+ " where coalesce(tampildidashboard,false) = true ";
			}

			@Override
			public String getListSql() {
				return getBaseSql() + " order by nama ";
			}

			@Override
			public void bind(Query query) {
				// Tidak membutuhkan filter global.
			}

			@Override
			public String[] getHeaders() {
				return new String[] { "ID", "Nama", "Keterangan" };
			}

			@Override
			@SuppressWarnings("unchecked")
			public void render(Rows rows, List data) throws Exception {
				for (Iterator<Object[]> it = data.iterator(); it.hasNext();) {
					Object[] o = it.next();
					Row row = new Row();
					row.setParent(rows);
					addGridCell(row, o[0]+"");
					addGridCell(row, o[1]+"");
					addGridCell(row, o[2]+"");
				}
			}
		};
	}

	private SqlDetailProvider createMutasiBulananProvider() {
		return new BaseSqlDetailProvider() {
			@Override
			public String getBaseSql() {
				return buildMutasiBulananBaseSql();
			}

			@Override
			public String getListSql() {
				return getBaseSql() + " order by bulan desc";
			}

			@Override
			public String[] getHeaders() {
				return new String[] { "Bulan", "Bukti", "Debet", "Kredit", "Saldo", "Visual" };
			}

			@Override
			@SuppressWarnings("unchecked")
			public void render(Rows rows, List data) throws Exception {
				double max = 0.0;
				for (Iterator<Object[]> it = data.iterator(); it.hasNext();) {
					Object[] row = it.next();
					double debet = toDouble(row[2]);
					double kredit = toDouble(row[3]);
					double gross = Math.abs(debet) + Math.abs(kredit);
					if (gross > max) {
						max = gross;
					}
				}

				for (Iterator<Object[]> it = data.iterator(); it.hasNext();) {
					Object[] o = it.next();
					Row row = new Row();
					row.setParent(rows);
					addGridCell(row, safeString(o[0]));
					addGridCell(row, formatNumber(toDouble(o[1])), "right");
					addGridCell(row, formatNumber(toDouble(o[2])), "right");
					addGridCell(row, formatNumber(toDouble(o[3])), "right");
					addGridCell(row, formatNumber(toDouble(o[4])), "right");

					double gross = Math.abs(toDouble(o[2])) + Math.abs(toDouble(o[3]));
					addBarCell(row, max <= 0.0 ? 0 : (int) Math.round((gross / max) * 100));
				}
			}
		};
	}

	private SqlDetailProvider createTopAkunProvider(final String detailTitle, final String aggregateExpr,
			final String alias, final String havingCondition) {
		return new BaseSqlDetailProvider() {
			@Override
			public String getBaseSql() {
				return buildTopAkunBaseSql(aggregateExpr, alias, havingCondition);
			}

			@Override
			public String getListSql() {
				return getBaseSql() + " order by " + alias + " desc, kode_akun";
			}

			@Override
			public String[] getHeaders() {
				return new String[] { "Akun", "Bukti", "Debet", "Kredit", "Saldo", "Detail" };
			}

			@Override
			@SuppressWarnings("unchecked")
			public void render(Rows rows, List data) throws Exception {
				for (Iterator<Object[]> it = data.iterator(); it.hasNext();) {
					Object[] o = it.next();
					Row row = new Row();
					row.setParent(rows);
					addGridCell(row, safeString(o[0]) + " - " + safeString(o[1]));
					addGridCell(row, formatNumber(toDouble(o[2])), "right");
					addGridCell(row, formatNumber(toDouble(o[3])), "right");
					addGridCell(row, formatNumber(toDouble(o[4])), "right");
					addGridCell(row, formatNumber(toDouble(o[5])), "right");

					final String kodeAkun = safeString(o[0]);
					A detail = new A("Buka");
					detail.setStyle("font-weight:800; color:#2563eb; text-decoration:none; cursor:pointer;");
					detail.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							viewDetail(detailTitle + " - " + kodeAkun,
									createTransaksiDetailProvider(" and a.kode = '" + sqlEscape(kodeAkun) + "' ",
											"order by gt.tanggal_transaksi desc, gt.id desc, a.kode"));
						}
					});
					row.appendChild(detail);
				}
			}
		};
	}

	private SqlDetailProvider createRingkasLaporanProvider() {
		return new BaseSqlDetailProvider() {
			@Override
			public String getBaseSql() {
				return buildRingkasLaporanBaseSql();
			}

			@Override
			public String getListSql() {
				return getBaseSql() + " order by jenis_laporan, kelompok_laporan";
			}

			@Override
			public String[] getHeaders() {
				return new String[] { "Jenis Laporan", "Kelompok", "Debet", "Kredit", "Saldo" };
			}

			@Override
			@SuppressWarnings("unchecked")
			public void render(Rows rows, List data) throws Exception {
				for (Iterator<Object[]> it = data.iterator(); it.hasNext();) {
					Object[] o = it.next();
					Row row = new Row();
					row.setParent(rows);
					addGridCell(row, safeString(o[0]));
					addGridCell(row, safeString(o[1]));
					addGridCell(row, formatNumber(toDouble(o[2])), "right");
					addGridCell(row, formatNumber(toDouble(o[3])), "right");
					addGridCell(row, formatNumber(toDouble(o[4])), "right");
				}
			}
		};
	}

	private SqlDetailProvider createRingkasNeracaProvider() {
		return new BaseSqlDetailProvider() {
			@Override
			public String getBaseSql() {
				return buildRingkasNeracaSql();
			}

			@Override
			public String getListSql() {
				return getBaseSql() + " order by kategori";
			}

			@Override
			public String[] getHeaders() {
				return new String[] { "Kategori", "Debet", "Kredit", "Saldo" };
			}

			@Override
			@SuppressWarnings("unchecked")
			public void render(Rows rows, List data) throws Exception {
				for (Iterator<Object[]> it = data.iterator(); it.hasNext();) {
					Object[] o = it.next();
					Row row = new Row();
					row.setParent(rows);
					addGridCell(row, safeString(o[0]));
					addGridCell(row, formatNumber(toDouble(o[1])), "right");
					addGridCell(row, formatNumber(toDouble(o[2])), "right");
					addGridCell(row, formatNumber(toDouble(o[3])), "right");
				}
			}
		};
	}


	private String[] getTransaksiHeaders() {
		return new String[] { "No. Bukti", "Tanggal", "Akun", "Nama Akun", "Keterangan", "Debet", "Kredit", "Saldo" };
	}

	@SuppressWarnings("unchecked")
	private void renderTransaksiRows(Rows rows, List data) throws Exception {
		for (Iterator<Object[]> it = data.iterator(); it.hasNext();) {
			Object[] o = it.next();
			Row row = new Row();
			row.setParent(rows);
			addGridCell(row, safeString(o[0]));
			addGridCell(row, formatTanggalJam(o[1]));
			addGridCell(row, safeString(o[2]));
			addGridCell(row, safeString(o[3]));
			addGridCell(row, safeString(o[4]));
			addGridCell(row, formatNumber(toDouble(o[5])), "right");
			addGridCell(row, formatNumber(toDouble(o[6])), "right");
			addGridCell(row, formatNumber(toDouble(o[7])), "right");
		}
	}

	private abstract class BaseSqlDetailProvider implements SqlDetailProvider {
		public String getCountSql() {
			return "select count(*) from (" + getBaseSql() + ") x";
		}

		public String getListSql() {
			return getBaseSql();
		}

		public void bind(Query query) {
			bindGlobalParams(query);
		}
	}

	private interface SqlDetailProvider {
		String getBaseSql();

		String getCountSql();

		String getListSql();

		String[] getHeaders();

		void bind(Query query);

		void render(Rows rows, List data) throws Exception;
	}

	/* ========================= UI HELPER ========================= */

	private void appendColumn(Columns columns, String label) {
		Column column = new Column(label);
		column.setStyle("font-weight:800; color:#0f172a; background:#f1f5f9;");
		column.setParent(columns);
	}

	private void addGridCell(Row row, String text) {
		addGridCell(row, text, "left");
	}

	private void addGridCell(Row row, String text, String align) {
		org.zkoss.zul.Label label = new org.zkoss.zul.Label(text == null ? "" : text);
		label.setStyle("font-size:12px; color:#334155; padding:7px 6px; white-space:normal; text-align:" + align + ";");
		row.appendChild(label);
	}

	private void addBarCell(Row row, int percent) {
		if (percent < 0) {
			percent = 0;
		}
		if (percent > 100) {
			percent = 100;
		}
		org.zkoss.zul.Div outer = new org.zkoss.zul.Div();
		outer.setStyle("height:10px; width:100%; background:#e5e7eb; border-radius:999px; overflow:hidden; margin-top:9px;");
		org.zkoss.zul.Div inner = new org.zkoss.zul.Div();
		inner.setStyle("height:10px; width:" + percent + "%; background:#047857; border-radius:999px;");
		inner.setParent(outer);
		row.appendChild(outer);
	}

	private void appendHtml(Component parent, String html) {
		Html h = new Html(html == null ? "" : html);
		h.setParent(parent);
	}

	private void appendEmptyState(Component parent, String text) {
		appendHtml(parent, "<div style='padding:14px; color:#64748b; background:#f8fafc; border:1px dashed #cbd5e1; border-radius:12px; font-size:12px;'>"
				+ escapeHtml(text) + "</div>");
	}

	private String formatTanggal(Date date) {
		if (date == null) {
			return "-";
		}
		try {
			return Common.dateFormat1.get().format(date);
		} catch (Exception e) {
			return date.toString();
		}
	}

	private String formatTanggalJam(Object value) {
		if (value == null) {
			return "-";
		}
		if (value instanceof Date) {
			try {
				return Common.dateFormat51.get().format((Date) value);
			} catch (Exception e) {
				return value.toString();
			}
		}
		return value.toString();
	}

	private String formatNumber(long value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String formatNumber(double value) {
		try {
			return value < 0.0 ? "(" + Common.numberFormat.get().format(Math.abs(value)) + ")"
					: Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	/*
	 * Jangan gunakan nama helper bernama "toString dengan argumen Object".
	 * Pada anonymous/inner class Java lama, pemanggilan helper lama toString dengan argumen array dapat
	 * terbaca sebagai Object.toString() yang tidak menerima argumen sehingga
	 * muncul compile error:
	 * "The method toString in the type Object is not applicable for the arguments Object".
	 */
	private String safeString(Object object) {
		if (object == null) {
			return "";
		}
		String s = object.toString();
		return "null".equalsIgnoreCase(s) ? "" : s;
	}

	private long toLong(Object object) {
		if (object == null) {
			return 0L;
		}
		if (object instanceof Number) {
			return ((Number) object).longValue();
		}
		try {
			return Long.parseLong(object.toString());
		} catch (Exception e) {
			return 0L;
		}
	}

	private double toDouble(Object object) {
		if (object == null) {
			return 0.0;
		}
		if (object instanceof Number) {
			return ((Number) object).doubleValue();
		}
		try {
			return Double.parseDouble(object.toString());
		} catch (Exception e) {
			return 0.0;
		}
	}

	private String escapeHtml(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private String sqlEscape(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("'", "''");
	}

	private void printDebug(Exception e) {
		if (debug) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/DasboardAkuntansi.java:2125");
		}
	}

	private static class AkuntansiDashboardData {
		long totalBukti;
		long totalBaris;
		long akunTerpakai;
		long jurnalPenyesuaian;
		long jurnalPenutup;
		long transaksiClosing;
		int laporanAktif;
		double saldoAwal;
		double totalDebet;
		double totalKredit;
		double saldoBersih;
		double saldoAkhir;
		List<Object[]> mutasiBulanan = new ArrayList<Object[]>();
		List<Object[]> topAkunDebet = new ArrayList<Object[]>();
		List<Object[]> topAkunKredit = new ArrayList<Object[]>();
		List<Object[]> ringkasLaporan = new ArrayList<Object[]>();
		List<Object[]> ringkasNeraca = new ArrayList<Object[]>();
	}
}
