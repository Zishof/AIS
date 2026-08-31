package ais.action.master.dashboard.keuangan;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.hibernate.SQLQuery;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldMerah;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Dasboard Piutang Rinci Khusus Ekosistem Sekolah (Siswa & Calon Siswa).
 *
 * Enhance 2026-05-28:
 * - Overview modern seperti pola DasboardSop_TEMPLATE_TAMBAHAN_V7_V5.
 * - Semua angka utama dibuat clickable dan membuka popup detail.
 * - Analitik tagihan, pembayaran, piutang, potongan, status siswa/calon siswa,
 *   top item biaya, top penunggak, funnel risiko, dan watchlist prioritas.
 * - Tetap kompatibel dengan Java 1.6/1.7 dan ZK 5.5, tanpa lambda/stream.
 */
/**
 * Menampilkan rincian piutang siswa agar tunggakan sekolah dapat dipantau berdasarkan siswa, item biaya, dan status pembayaran.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardPiutangRinciSekolah extends MyWindow {

	private static final long serialVersionUID = 1L;

	private static final String TIPE_SEMUA = "SEMUA";
	private static final String TIPE_TAGIHAN = "TAGIHAN";
	private static final String TIPE_DIBAYAR = "DIBAYAR";
	private static final String TIPE_PIUTANG = "PIUTANG";
	private static final String TIPE_POTONGAN = "POTONGAN";
	private static final String TIPE_LUNAS = "LUNAS";
	private static final String TIPE_BELUM_BAYAR = "BELUM_BAYAR";
	private static final String TIPE_SEBAGIAN = "SEBAGIAN";
	private static final String TIPE_LEBIH_BAYAR = "LEBIH_BAYAR";
	private static final String TIPE_SISWA = "SISWA";
	private static final String TIPE_CALON = "CALON";

	private static final int DETAIL_PAGE_SIZE = 10;
	private static final int DETAIL_LIMIT = 500;
	private static final int DATA_LIMIT_TAMPIL = 10000;
	private static final int GRID_PAGE_SIZE_INTERNAL = 10;

	private Vbox mainContainer;
	private MyTextbox searchNama;
	private MyTextbox searchItemBiaya;
	private Combobox comboTampilkan;
	private Checkbox chartTampil;
	private Paging paging;
	private Grid grid;
	private Html loadingDashboardHtml;

	private int jumlahDataDalamSatuHalaman = DATA_LIMIT_TAMPIL;

	public DasboardPiutangRinciSekolah() {
		super("Dasbor Piutang Siswa & Calon Siswa", "none", false);
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** PEMBERSIH SESSION TERPUSAT - MENCEGAH MEMORY LEAK (OOM) */
	private void cleanupSession(Session session) {
		ais.ui.util.DashboardModernHtmlUtil.closeOpenedSession(session);
	}

	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");

		mainContainer = new Vbox();
		mainContainer.setWidth("100%");
		mainContainer.setHeight("100%");
		mainContainer.setStyle("background:#f6f8fb; overflow:auto;");
		mainContainer.setParent(this);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(mainContainer);
		toolbar.setStyle("padding:10px; background:#ffffff; border-bottom:1px solid #e5e7eb; "
				+ "display:flex; flex-wrap:wrap; align-items:center; gap:8px;");

		toolbar.appendChild(new MyLabelAgakKecil("Nama / NIS / No.Reg:"));
		searchNama = new MyTextbox();
		searchNama.setWidth("170px");
		searchNama.setParent(toolbar);

		toolbar.appendChild(new MyLabelAgakKecil("Item Biaya:"));
		searchItemBiaya = new MyTextbox();
		searchItemBiaya.setWidth("170px");
		searchItemBiaya.setParent(toolbar);

		chartTampil = new Checkbox("Tampilkan Grafik");
		// Default dibuat tidak tercentang agar ketika user meng-klik "Tampilkan Grafik",
		// statusnya menjadi checked=true dan grafik benar-benar dirender.
		chartTampil.setChecked(false);
		chartTampil.setTooltiptext("Centang untuk menampilkan grafik ringkas tepat di atas tabel rincian.");
		chartTampil.setParent(toolbar);

		Label infoLimit = new Label("Maksimal 10.000 data; grid otomatis paging 50 baris/halaman.");
		infoLimit.setStyle("font-size:11px; color:#64748b; font-weight:bold;");
		infoLimit.setParent(toolbar);

		EventListener cariListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				jumlahDataDalamSatuHalaman = DATA_LIMIT_TAMPIL;
				reload(true);
			}
		};

		MyToolbarbuttonConfig btnTampil = new MyToolbarbuttonConfig("Tampilkan", "/img/search.png");
		btnTampil.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; padding:6px 14px;");
		btnTampil.addEventListener("onClick", cariListener);
		btnTampil.setParent(toolbar);

		searchNama.addEventListener("onOK", cariListener);
		searchItemBiaya.addEventListener("onOK", cariListener);
		chartTampil.addEventListener("onCheck", cariListener);

		MyToolbarbuttonConfig btnUnduh = new MyToolbarbuttonConfig("Unduh Dasbor Excel", "/img/excel.png");
		btnUnduh.setStyle("font-weight:bold; border-radius:10px; padding:6px 12px;");
		btnUnduh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				unduhExcelDanDasbor();
			}
		});
		btnUnduh.setParent(toolbar);

		paging = null; // paging luar dihilangkan; paging cukup di dalam Grid agar UI lebih ringan.

		reload(true);
	}

	@SuppressWarnings("unchecked")
	private void reload(boolean hitungUlangPaging) {
		while (mainContainer.getChildren().size() > 1) {
			mainContainer.removeChild(mainContainer.getLastChild());
		}
		tampilkanLoadingDashboardPiutang("Menyiapkan dashboard piutang sekolah...", 5);

		String pencarian = getSearchNamaValue();
		String itemTujuan = getSearchItemValue();
		int activePage = 0;
		int mulai = 0;
		jumlahDataDalamSatuHalaman = DATA_LIMIT_TAMPIL;

		Session session = null;
		List<Object[]> objects = null;
		DashboardData dashboardData = new DashboardData();

		try {
			updateLoadingDashboardPiutang("Membuka koneksi dan menyiapkan filter piutang...", 12);
			session = HibernateUtil.getSessionFactory().openSession();
			String sqlBase = buildSqlBase(pencarian, itemTujuan);

			updateLoadingDashboardPiutang("Menghitung overview, bucket risiko, top item, dan top penunggak...", 25);
			dashboardData = loadDashboardData(session, pencarian, itemTujuan, sqlBase);

			updateLoadingDashboardPiutang("Mengambil maksimal 10.000 rincian piutang; grid akan paging 50 baris/halaman...", 42);

			String sqlData = detailSelectSql()
					+ sqlBase
					+ " ORDER BY ib.nama, status, nama "
					+ " LIMIT " + DATA_LIMIT_TAMPIL;

			updateLoadingDashboardPiutang("Mengambil rincian data halaman aktif...", 58);
			SQLQuery qData = session.createSQLQuery(sqlData);
			applyBaseParams(qData, pencarian, itemTujuan);
			objects = qData.list();

			/*
			 * Perbaikan tampilan:
			 * Grid data diletakkan tepat setelah paging dan vflex grid dihapus.
			 * Pada ZK lama, grid dengan vflex di bawah banyak dashboard bisa tampak kosong/terpotong
			 * walaupun paging sudah menunjukkan total data.
			 */
			updateLoadingDashboardPiutang("Menyiapkan grid dan ringkasan data...", 70);
			renderRincianIntro(mainContainer, dashboardData, activePage);
			ais.ui.util.DashboardJurnalPembayaranUtil.renderJurnalSiswaPanel(mainContainer, "Ringkasan Jurnal Pembayaran Siswa",
					"Menunjukkan akun kas/bank, piutang, pendapatan, denda, dan diskon yang terkait dengan rincian piutang siswa. Petugas bisa lebih mudah melihat akun yang perlu dilengkapi.");

			if (objects == null || objects.isEmpty()) {
				Div empty = new Div();
				empty.setParent(mainContainer);
				empty.setStyle("padding:14px; margin:10px 14px; border-radius:14px; background:#fff7ed; "
						+ "border:1px solid #fed7aa; color:#9a3412; font-size:12px; font-weight:700;");
				empty.appendChild(new Label("Tidak ada rincian piutang siswa/calon siswa yang ditemukan berdasarkan filter saat ini."));
				renderDashboardOverview(mainContainer, dashboardData, pencarian, itemTujuan);
				renderDashboardAnalytics(mainContainer, dashboardData, pencarian, itemTujuan);
				renderDashboardTambahanPiutang(mainContainer, dashboardData, pencarian, itemTujuan);
				return;
			}

			// Grafik harus diletakkan sebelum tabel agar langsung terlihat setelah checkbox "Tampilkan Grafik" dicentang.
			updateLoadingDashboardPiutang("Merender ringkasan CSS ringan dan tabel rincian...", 82);
			renderChartFromRows(mainContainer, objects);
			renderDetailGrid(mainContainer, objects);
			updateLoadingDashboardPiutang("Merender overview dan dashboard analitik piutang...", 92);
			renderDashboardOverview(mainContainer, dashboardData, pencarian, itemTujuan);
			renderDashboardAnalytics(mainContainer, dashboardData, pencarian, itemTujuan);
			renderDashboardTambahanPiutang(mainContainer, dashboardData, pencarian, itemTujuan);
			updateLoadingDashboardPiutang("Selesai memproses dashboard piutang sekolah.", 100);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/keuangan/DasboardPiutangRinciSekolah.java:265");
			Common.tampilErrorJikaAdmin(e);
		} finally {
			sembunyikanLoadingDashboardPiutang();
			try { org.zkoss.zk.ui.util.Clients.clearBusy(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRinciSekolah.java:269");}
			cleanupSession(session);
		}
	}


	private void tampilkanLoadingDashboardPiutang(String message, int progress) {
		try {
			sembunyikanLoadingDashboardPiutang();
			if (mainContainer == null) return;
			loadingDashboardHtml = new Html(buildLoadingHtml(message, progress));
			loadingDashboardHtml.setParent(mainContainer);
			try { org.zkoss.zk.ui.util.Clients.showBusy(progress + "% - " + message); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRinciSekolah.java:281");}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRinciSekolah.java:282");}
	}

	private void updateLoadingDashboardPiutang(String message, int progress) {
		try {
			if (loadingDashboardHtml != null) {
				loadingDashboardHtml.setContent(buildLoadingHtml(message, progress));
				loadingDashboardHtml.invalidate();
			}
			try { org.zkoss.zk.ui.util.Clients.showBusy(progress + "% - " + message); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRinciSekolah.java:291");}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRinciSekolah.java:292");}
	}


	private void sembunyikanLoadingDashboardPiutang() {
		try {
			if (loadingDashboardHtml != null && loadingDashboardHtml.getParent() != null) {
				loadingDashboardHtml.detach();
			}
			loadingDashboardHtml = null;
			try { org.zkoss.zk.ui.util.Clients.clearBusy(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRinciSekolah.java:302");}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRinciSekolah.java:303");}
	}

	private String buildLoadingHtml(String message, int progress) {
		if (progress < 0) progress = 0;
		if (progress > 100) progress = 100;
		return "<div style='margin:10px 14px; padding:14px; text-align:left; color:#334155; background:#ffffff; border:1px solid #e5e7eb; border-radius:14px; box-shadow:0 10px 24px rgba(15,23,42,.05);'>"
				+ "<div style='font-weight:800; font-size:13px; margin-bottom:8px;'><i class=\"fa fa-spinner fa-spin\"></i> " + escapeHtml(message) + "</div>"
				+ "<div style='height:10px; background:#e5e7eb; border-radius:999px; overflow:hidden;'><div style='height:10px; width:" + progress + "%; background:#2563eb;'></div></div>"
				+ "<div style='font-size:11px; color:#64748b; margin-top:6px;'>Progress " + progress + "%</div></div>";
	}

	private String getSearchNamaValue() {
		return searchNama != null && searchNama.getValue() != null ? searchNama.getValue().trim() : "";
	}

	private String getSearchItemValue() {
		return searchItemBiaya != null && searchItemBiaya.getValue() != null ? searchItemBiaya.getValue().trim() : "";
	}

	private String buildSqlBase(String pencarian, String itemTujuan) {
		String sqlBase = " FROM sekolah.tagihan t "
				+ " LEFT JOIN sekolah.siswa s ON t.siswa_id = s.id "
				+ " LEFT JOIN sekolah.calon_siswa c ON t.calon_siswa_id = c.id "
				+ " INNER JOIN sekolah.item_biaya_sekolah ib ON t.item_biaya_id = ib.id "
				+ " WHERE (COALESCE(t.nominal,0) > 0.1 OR COALESCE(t.dibayar,0) > 0.1) ";

		if (pencarian != null && pencarian.length() > 0) {
			sqlBase += " AND (s.nama_siswa ILIKE :cari OR c.nama_siswa ILIKE :cari "
					+ " OR s.nomor_induk ILIKE :cari OR c.nomor_induk ILIKE :cari) ";
		}
		if (itemTujuan != null && itemTujuan.length() > 0) {
			sqlBase += " AND ib.nama ILIKE :itemTujuan ";
		}
		return sqlBase;
	}

	private void applyBaseParams(SQLQuery query, String pencarian, String itemTujuan) {
		if (pencarian != null && pencarian.length() > 0) {
			query.setString("cari", "%" + pencarian + "%");
		}
		if (itemTujuan != null && itemTujuan.length() > 0) {
			query.setString("itemTujuan", "%" + itemTujuan + "%");
		}
	}

	private void applyExtraParams(SQLQuery query, Map<String, Object> params) {
		if (params == null) {
			return;
		}
		for (Map.Entry<String, Object> e : params.entrySet()) {
			Object v = e.getValue();
			if (v instanceof Number) {
				query.setParameter(e.getKey(), v);
			} else {
				query.setString(e.getKey(), v == null ? "" : String.valueOf(v));
			}
		}
	}

	private String nettoExpr() {
		return "(COALESCE(t.nominal,0)-COALESCE(t.diskon,0))";
	}

	private String sisaExpr() {
		return "(COALESCE(t.nominal,0)-COALESCE(t.diskon,0)-COALESCE(t.dibayar,0))";
	}

	private String periodeExpr() {
		return "COALESCE(CAST(t.tahunbulan AS TEXT),'')";
	}

	private String kodeExpr() {
		return "COALESCE((CASE WHEN s.id IS NOT NULL THEN s.nomor_induk ELSE c.nomor_induk END),'')";
	}

	private String statusExpr() {
		return "(CASE WHEN s.id IS NOT NULL THEN 'Siswa Aktif' ELSE 'Calon Siswa' END)";
	}

	private String detailSelectSql() {
		return "SELECT "
				+ " (CASE WHEN s.id IS NOT NULL THEN s.nomor_induk ELSE c.nomor_induk END) AS kode_transaksi, "
				+ " (CASE WHEN s.id IS NOT NULL THEN s.nama_siswa ELSE c.nama_siswa END) AS nama, "
				+ " (CASE WHEN s.id IS NOT NULL THEN 'Siswa Aktif' ELSE 'Calon Siswa' END) AS status, "
				+ " ib.nama AS nama_item, "
				+ periodeExpr() + " AS tahunbulan, "
				+ " COALESCE(t.nominal,0) AS nominal, "
				+ " COALESCE(t.dibayar,0) AS dibayar, "
				+ " COALESCE(t.diskon,0) AS potongan, "
				+ nettoExpr() + " AS tagihan_netto, "
				+ sisaExpr() + " AS sisa_piutang ";
	}

	private String filterByTipe(String tipe) {
		if (TIPE_TAGIHAN.equals(tipe)) {
			return " AND " + nettoExpr() + " > 0.1 ";
		}
		if (TIPE_DIBAYAR.equals(tipe)) {
			return " AND COALESCE(t.dibayar,0) > 0.1 ";
		}
		if (TIPE_PIUTANG.equals(tipe)) {
			return " AND " + sisaExpr() + " > 0.1 ";
		}
		if (TIPE_POTONGAN.equals(tipe)) {
			return " AND COALESCE(t.diskon,0) > 0.1 ";
		}
		if (TIPE_LUNAS.equals(tipe)) {
			return " AND " + nettoExpr() + " > 0.1 AND " + sisaExpr() + " <= 0.1 ";
		}
		if (TIPE_BELUM_BAYAR.equals(tipe)) {
			return " AND " + sisaExpr() + " > 0.1 AND COALESCE(t.dibayar,0) <= 0.1 ";
		}
		if (TIPE_SEBAGIAN.equals(tipe)) {
			return " AND " + sisaExpr() + " > 0.1 AND COALESCE(t.dibayar,0) > 0.1 ";
		}
		if (TIPE_LEBIH_BAYAR.equals(tipe)) {
			return " AND " + sisaExpr() + " < -0.1 ";
		}
		if (TIPE_SISWA.equals(tipe)) {
			return " AND s.id IS NOT NULL ";
		}
		if (TIPE_CALON.equals(tipe)) {
			return " AND s.id IS NULL AND c.id IS NOT NULL ";
		}
		return "";
	}

	private DashboardData loadDashboardData(Session session, String pencarian, String itemTujuan, String sqlBase) {
		DashboardData d = new DashboardData();
		try {
			String sqlAgg = "SELECT COUNT(t.id), "
					+ " COALESCE(SUM(" + nettoExpr() + "),0), "
					+ " COALESCE(SUM(COALESCE(t.dibayar,0)),0), "
					+ " COALESCE(SUM(" + sisaExpr() + "),0), "
					+ " COALESCE(SUM(COALESCE(t.diskon,0)),0), "
					+ " COALESCE(SUM(CASE WHEN " + sisaExpr() + " > 0.1 THEN 1 ELSE 0 END),0), "
					+ " COALESCE(SUM(CASE WHEN " + nettoExpr() + " > 0.1 AND " + sisaExpr() + " <= 0.1 THEN 1 ELSE 0 END),0), "
					+ " COALESCE(SUM(CASE WHEN " + sisaExpr() + " > 0.1 AND COALESCE(t.dibayar,0) <= 0.1 THEN 1 ELSE 0 END),0), "
					+ " COALESCE(SUM(CASE WHEN " + sisaExpr() + " > 0.1 AND COALESCE(t.dibayar,0) > 0.1 THEN 1 ELSE 0 END),0), "
					+ " COALESCE(SUM(CASE WHEN " + sisaExpr() + " < -0.1 THEN 1 ELSE 0 END),0), "
					+ " COALESCE(SUM(CASE WHEN s.id IS NOT NULL THEN 1 ELSE 0 END),0), "
					+ " COALESCE(SUM(CASE WHEN s.id IS NULL AND c.id IS NOT NULL THEN 1 ELSE 0 END),0) "
					+ sqlBase;
			SQLQuery qAgg = session.createSQLQuery(sqlAgg);
			applyBaseParams(qAgg, pencarian, itemTujuan);
			Object[] r = (Object[]) qAgg.uniqueResult();
			if (r != null) {
				d.totalBaris = toInt(r[0]);
				d.totalTagihan = toDouble(r[1]);
				d.totalDibayar = toDouble(r[2]);
				d.totalPiutang = toDouble(r[3]);
				d.totalPotongan = toDouble(r[4]);
				d.jumlahPiutang = toInt(r[5]);
				d.jumlahLunas = toInt(r[6]);
				d.jumlahBelumBayar = toInt(r[7]);
				d.jumlahSebagian = toInt(r[8]);
				d.jumlahLebihBayar = toInt(r[9]);
				d.jumlahSiswa = toInt(r[10]);
				d.jumlahCalon = toInt(r[11]);
			}

			loadTopItem(session, d, pencarian, itemTujuan, sqlBase);
			loadTopSiswa(session, d, pencarian, itemTujuan, sqlBase);
			loadPeriode(session, d, pencarian, itemTujuan, sqlBase);
			loadStatusBreakdown(session, d, pencarian, itemTujuan, sqlBase);
			loadBucketPiutang(session, d, pencarian, itemTujuan, sqlBase);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/keuangan/DasboardPiutangRinciSekolah.java:471");
		}
		return d;
	}

	@SuppressWarnings("unchecked")
	private void loadTopItem(Session session, DashboardData d, String pencarian, String itemTujuan, String sqlBase) {
		String sql = "SELECT ib.nama, COUNT(t.id), COALESCE(SUM(" + nettoExpr() + "),0), "
				+ "COALESCE(SUM(COALESCE(t.dibayar,0)),0), COALESCE(SUM(" + sisaExpr() + "),0) "
				+ sqlBase
				+ " GROUP BY ib.nama ORDER BY COALESCE(SUM(" + sisaExpr() + "),0) DESC LIMIT 10";
		SQLQuery q = session.createSQLQuery(sql);
		applyBaseParams(q, pencarian, itemTujuan);
		List<Object[]> rows = q.list();
		for (Object[] r : rows) {
			DashboardGroup g = new DashboardGroup();
			g.key = r[0] == null ? "Tidak diketahui" : String.valueOf(r[0]);
			g.count = toInt(r[1]);
			g.tagihan = toDouble(r[2]);
			g.dibayar = toDouble(r[3]);
			g.piutang = toDouble(r[4]);
			d.topItems.add(g);
		}
	}

	@SuppressWarnings("unchecked")
	private void loadTopSiswa(Session session, DashboardData d, String pencarian, String itemTujuan, String sqlBase) {
		String sql = "SELECT " + kodeExpr() + ", "
				+ "COALESCE((CASE WHEN s.id IS NOT NULL THEN s.nama_siswa ELSE c.nama_siswa END),'Tanpa Nama'), "
				+ statusExpr() + ", COUNT(t.id), COALESCE(SUM(" + nettoExpr() + "),0), "
				+ "COALESCE(SUM(COALESCE(t.dibayar,0)),0), COALESCE(SUM(" + sisaExpr() + "),0) "
				+ sqlBase + filterByTipe(TIPE_PIUTANG)
				+ " GROUP BY " + kodeExpr() + ", COALESCE((CASE WHEN s.id IS NOT NULL THEN s.nama_siswa ELSE c.nama_siswa END),'Tanpa Nama'), " + statusExpr()
				+ " ORDER BY COALESCE(SUM(" + sisaExpr() + "),0) DESC LIMIT 10";
		SQLQuery q = session.createSQLQuery(sql);
		applyBaseParams(q, pencarian, itemTujuan);
		List<Object[]> rows = q.list();
		for (Object[] r : rows) {
			DashboardPerson g = new DashboardPerson();
			g.kode = r[0] == null ? "" : String.valueOf(r[0]);
			g.nama = r[1] == null ? "Tanpa Nama" : String.valueOf(r[1]);
			g.status = r[2] == null ? "" : String.valueOf(r[2]);
			g.count = toInt(r[3]);
			g.tagihan = toDouble(r[4]);
			g.dibayar = toDouble(r[5]);
			g.piutang = toDouble(r[6]);
			d.topPersons.add(g);
		}
	}

	@SuppressWarnings("unchecked")
	private void loadPeriode(Session session, DashboardData d, String pencarian, String itemTujuan, String sqlBase) {
		String sql = "SELECT " + periodeExpr() + ", COUNT(t.id), COALESCE(SUM(" + nettoExpr() + "),0), "
				+ "COALESCE(SUM(COALESCE(t.dibayar,0)),0), COALESCE(SUM(" + sisaExpr() + "),0) "
				+ sqlBase
				+ " GROUP BY " + periodeExpr()
				+ " ORDER BY " + periodeExpr() + " DESC LIMIT 12";
		SQLQuery q = session.createSQLQuery(sql);
		applyBaseParams(q, pencarian, itemTujuan);
		List<Object[]> rows = q.list();
		for (Object[] r : rows) {
			DashboardGroup g = new DashboardGroup();
			g.key = r[0] == null || String.valueOf(r[0]).length() == 0 ? "Tanpa Periode" : String.valueOf(r[0]);
			g.count = toInt(r[1]);
			g.tagihan = toDouble(r[2]);
			g.dibayar = toDouble(r[3]);
			g.piutang = toDouble(r[4]);
			d.periode.add(g);
		}
		Collections.reverse(d.periode);
	}


	@SuppressWarnings("unchecked")
	private void loadStatusBreakdown(Session session, DashboardData d, String pencarian, String itemTujuan, String sqlBase) {
		String sql = "SELECT " + statusExpr() + ", COUNT(t.id), COALESCE(SUM(" + nettoExpr() + "),0), "
				+ "COALESCE(SUM(COALESCE(t.dibayar,0)),0), COALESCE(SUM(" + sisaExpr() + "),0) "
				+ sqlBase
				+ " GROUP BY " + statusExpr()
				+ " ORDER BY " + statusExpr();
		SQLQuery q = session.createSQLQuery(sql);
		applyBaseParams(q, pencarian, itemTujuan);
		List<Object[]> rows = q.list();
		for (Object[] r : rows) {
			DashboardGroup g = new DashboardGroup();
			g.key = r[0] == null ? "Tidak diketahui" : String.valueOf(r[0]);
			g.count = toInt(r[1]);
			g.tagihan = toDouble(r[2]);
			g.dibayar = toDouble(r[3]);
			g.piutang = toDouble(r[4]);
			if ("Siswa Aktif".equals(g.key)) {
				g.tipe = TIPE_SISWA;
			} else if ("Calon Siswa".equals(g.key)) {
				g.tipe = TIPE_CALON;
			} else {
				g.tipe = TIPE_SEMUA;
			}
			d.statusGroups.add(g);
		}
	}

	@SuppressWarnings("unchecked")
	private void loadBucketPiutang(Session session, DashboardData d, String pencarian, String itemTujuan, String sqlBase) {
		/*
		 * Jangan SELECT label bucket berbentuk String langsung dari CASE.
		 * Pada beberapa kombinasi PostgreSQL/JDBC/Hibernate lama, ResultSetMetadata bisa
		 * salah membaca kolom CASE pertama sebagai integer sehingga muncul error:
		 * "Bad value for type int : Lunas / Tidak Ada Piutang".
		 * Solusinya: SQL hanya mengembalikan rank numeric, label dibuat di Java.
		 */
		String rankExpr = "(CASE "
				+ " WHEN " + sisaExpr() + " <= 0.1 THEN 1 "
				+ " WHEN " + sisaExpr() + " <= 500000 THEN 2 "
				+ " WHEN " + sisaExpr() + " <= 1000000 THEN 3 "
				+ " WHEN " + sisaExpr() + " <= 5000000 THEN 4 "
				+ " ELSE 5 END)";
		String sql = "SELECT " + rankExpr + " AS bucket_rank, COUNT(t.id), COALESCE(SUM(" + nettoExpr() + "),0), "
				+ "COALESCE(SUM(COALESCE(t.dibayar,0)),0), COALESCE(SUM(" + sisaExpr() + "),0) "
				+ sqlBase
				+ " GROUP BY " + rankExpr
				+ " ORDER BY " + rankExpr;
		SQLQuery q = session.createSQLQuery(sql);
		applyBaseParams(q, pencarian, itemTujuan);
		List<Object[]> rows = q.list();
		for (Object[] r : rows) {
			int rank = toInt(r[0]);
			DashboardGroup g = new DashboardGroup();
			g.key = bucketLabel(rank);
			g.count = toInt(r[1]);
			g.tagihan = toDouble(r[2]);
			g.dibayar = toDouble(r[3]);
			g.piutang = toDouble(r[4]);
			g.extraSql = bucketWhereSql(g.key);
			d.bucketGroups.add(g);
		}
	}

	private String bucketLabel(int rank) {
		if (rank == 1) return "Lunas / Tidak Ada Piutang";
		if (rank == 2) return "Piutang 0 - 500 Ribu";
		if (rank == 3) return "Piutang 500 Ribu - 1 Juta";
		if (rank == 4) return "Piutang 1 - 5 Juta";
		if (rank == 5) return "Piutang Di Atas 5 Juta";
		return "Tidak diketahui";
	}

	private String bucketWhereSql(String key) {
		if ("Lunas / Tidak Ada Piutang".equals(key)) {
			return " AND " + sisaExpr() + " <= 0.1 ";
		}
		if ("Piutang 0 - 500 Ribu".equals(key)) {
			return " AND " + sisaExpr() + " > 0.1 AND " + sisaExpr() + " <= 500000 ";
		}
		if ("Piutang 500 Ribu - 1 Juta".equals(key)) {
			return " AND " + sisaExpr() + " > 500000 AND " + sisaExpr() + " <= 1000000 ";
		}
		if ("Piutang 1 - 5 Juta".equals(key)) {
			return " AND " + sisaExpr() + " > 1000000 AND " + sisaExpr() + " <= 5000000 ";
		}
		if ("Piutang Di Atas 5 Juta".equals(key)) {
			return " AND " + sisaExpr() + " > 5000000 ";
		}
		return "";
	}

	private int toInt(Object o) {
		if (o == null) return 0;
		if (o instanceof Number) return ((Number) o).intValue();
		try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
	}

	private double toDouble(Object o) {
		if (o == null) return 0.0;
		if (o instanceof Number) return ((Number) o).doubleValue();
		try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0.0; }
	}

	private int percent(double value, double total) {
		if (total <= 0) return 0;
		return (int) Math.round((value * 100.0) / total);
	}

	private String uang(double value) {
		try { return Common.numberFormat.get().format(value); } catch (Exception e) { return String.valueOf(value); }
	}

	private String escapeHtml(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private void appendHtml(Component parent, String html) {
		Html h = new Html(html);
		h.setParent(parent);
	}

	private void renderDashboardOverview(Component parent, final DashboardData d, String pencarian, String itemTujuan) {
		Div shell = new Div();
		shell.setWidth("100%");
		shell.setStyle("padding:14px 14px 0 14px; box-sizing:border-box;");
		shell.setParent(parent);

		Div hero = new Div();
		hero.setStyle("position:relative; overflow:hidden; border-radius:18px; padding:22px 24px; color:#ffffff;"
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);"
				+ "box-shadow:0 14px 30px rgba(15,23,42,.18); box-sizing:border-box;");
		hero.setParent(shell);
		appendHtml(hero, "<div style='position:absolute; right:-60px; top:-70px; width:210px; height:210px; border-radius:999px; background:rgba(255,255,255,.12);'></div>"
				+ "<div style='position:absolute; right:100px; bottom:-75px; width:170px; height:170px; border-radius:999px; background:rgba(255,255,255,.10);'></div>"
				+ "<div style='position:relative; z-index:1;'>"
				+ "<div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;'>School Receivable Control Center</div>"
				+ "<div style='font-size:28px; line-height:1.15; font-weight:800; margin-top:6px;'>Dasbor Piutang Rinci Siswa & Calon Siswa</div>"
				+ "<div style='font-size:13px; opacity:.90; margin-top:8px;'>Pantau tagihan, pembayaran, potongan, sisa piutang, status pelunasan, dan prioritas penagihan dalam satu layar. Klik angka untuk membuka popup data rinci.</div>"
				+ "<div style='margin-top:12px; display:flex; gap:8px; flex-wrap:wrap;'>"
				+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>Nama/NIS: " + escapeHtml(pencarian == null || pencarian.length() == 0 ? "Semua" : pencarian) + "</span>"
				+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>Item Biaya: " + escapeHtml(itemTujuan == null || itemTujuan.length() == 0 ? "Semua" : itemTujuan) + "</span>"
				+ "</div></div>");

		Div cards = new Div();
		cards.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px;");
		cards.setParent(shell);
		createMetricCard(cards, "Total Baris", String.valueOf(d.totalBaris), "Data tagihan terpantau", "#dbeafe", "#1e40af", "#", "Detail Semua Tagihan", TIPE_SEMUA, null, null);
		createMetricCard(cards, "Tagihan Netto", uang(d.totalTagihan), "Nominal - potongan", "#ecfdf5", "#166534", "Rp", "Detail Tagihan Netto", TIPE_TAGIHAN, null, null);
		createMetricCard(cards, "Telah Dibayar", uang(d.totalDibayar), "Nominal pembayaran masuk", "#dcfce7", "#166534", "✓", "Detail Pembayaran Masuk", TIPE_DIBAYAR, null, null);
		createMetricCard(cards, "Sisa Piutang", uang(d.totalPiutang), "Tagihan netto - dibayar", "#fee2e2", "#991b1b", "!", "Detail Sisa Piutang", TIPE_PIUTANG, null, null);
		createMetricCard(cards, "Potongan", uang(d.totalPotongan), "Diskon/pengurangan", "#fef3c7", "#92400e", "%", "Detail Potongan", TIPE_POTONGAN, null, null);
		createMetricCard(cards, "Lunas", String.valueOf(d.jumlahLunas), "Tagihan sudah selesai", "#cffafe", "#155e75", "★", "Detail Tagihan Lunas", TIPE_LUNAS, null, null);
	}

	private void renderDashboardAnalytics(Component parent, DashboardData d, String pencarian, String itemTujuan) {
		Div wrap = new Div();
		wrap.setWidth("100%");
		wrap.setStyle("padding:6px 14px 0 14px; box-sizing:border-box;");
		wrap.setParent(parent);

		Div intro = new Div();
		intro.setStyle("margin-top:8px; margin-bottom:8px; padding:12px 14px; border-radius:16px; "
				+ "background:#ffffff; border:1px solid #e5e7eb; box-shadow:0 10px 24px rgba(15,23,42,.04);");
		intro.setParent(wrap);
		appendHtml(intro, "<div style='font-size:14px; font-weight:800; color:#0f172a;'>Dashboard Rinci Piutang & Tagihan</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:4px;'>Bagian ini mengikuti pola template dashboard SOP: funnel risiko, kualitas pembayaran, sebaran item biaya, periode tagihan, dan watchlist prioritas. Semua angka utama dapat diklik.</div>");

		Div gridWrap = new Div();
		gridWrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		gridWrap.setParent(wrap);

		Div left = createPanel(gridWrap, " Risiko Penagihan", "flex:1 1 420px;");
		int max = Math.max(Math.max(d.jumlahBelumBayar, d.jumlahSebagian), Math.max(d.jumlahLunas, Math.max(d.jumlahLebihBayar, 1)));
		renderFunnelRow(left, "Belum Bayar", d.jumlahBelumBayar, max, "#dc2626", "Detail Belum Bayar", TIPE_BELUM_BAYAR, null, null);
		renderFunnelRow(left, "Bayar Sebagian", d.jumlahSebagian, max, "#f59e0b", "Detail Bayar Sebagian", TIPE_SEBAGIAN, null, null);
		renderFunnelRow(left, "Lunas", d.jumlahLunas, max, "#16a34a", "Detail Lunas", TIPE_LUNAS, null, null);
		renderFunnelRow(left, "Lebih Bayar", d.jumlahLebihBayar, max, "#7c3aed", "Detail Lebih Bayar", TIPE_LEBIH_BAYAR, null, null);

		Div right = createPanel(gridWrap, "Kesehatan Pembayaran", "flex:1 1 420px;");
		renderMiniGauge(right, "Collection Rate", percent(d.totalDibayar, d.totalTagihan), "Dibayar dibanding tagihan netto.", "#16a34a", "Detail Pembayaran Masuk", TIPE_DIBAYAR, null, null);
		renderMiniGauge(right, "Receivable Pressure", percent(d.totalPiutang, d.totalTagihan), "Sisa piutang dibanding tagihan netto.", "#dc2626", "Detail Sisa Piutang", TIPE_PIUTANG, null, null);
		renderMiniGauge(right, "Potongan Rate", percent(d.totalPotongan, d.totalTagihan + d.totalPotongan), "Proporsi potongan terhadap tagihan kotor.", "#f59e0b", "Detail Potongan", TIPE_POTONGAN, null, null);
		renderMiniGauge(right, "Data Lunas", percent(d.jumlahLunas, d.totalBaris), "Jumlah baris tagihan lunas dibanding total baris.", "#0891b2", "Detail Lunas", TIPE_LUNAS, null, null);

		Div bottom = new Div();
		bottom.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px;");
		bottom.setParent(wrap);

		Div itemPanel = createPanel(bottom, "Top Item Biaya Berdasarkan Piutang", "flex:1 1 520px;");
		renderTopItemTable(itemPanel, d.topItems);

		Div siswaPanel = createPanel(bottom, "Watchlist Prioritas Penagihan", "flex:1 1 520px;");
		renderTopPersonTable(siswaPanel, d.topPersons);

		Div periodePanel = createPanel(wrap, "Tren Periode Tagihan", "margin-top:12px;");
		renderPeriodeTrend(periodePanel, d.periode);
	}


	private void renderRincianIntro(Component parent, DashboardData d, int activePage) {
		Div wrap = new Div();
		wrap.setWidth("100%");
		wrap.setStyle("padding:10px 14px 4px 14px; box-sizing:border-box;");
		wrap.setParent(parent);
		Div info = new Div();
		info.setStyle("display:flex; align-items:center; justify-content:space-between; gap:12px; flex-wrap:wrap; "
				+ "background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:12px 14px; "
				+ "box-shadow:0 10px 24px rgba(15,23,42,.04); box-sizing:border-box;");
		info.setParent(wrap);
		appendHtml(info, "<div><div style='font-size:14px; font-weight:900; color:#0f172a;'>Rincian Data Halaman Ini</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:3px;'>Paging luar dihilangkan. Data maksimal 10.000 baris ditampilkan di Grid dengan paging internal 50 baris/halaman.</div></div>");
		Div mini = new Div();
		mini.setStyle("display:flex; gap:8px; flex-wrap:wrap;");
		mini.setParent(info);
		createMiniClickableChip(mini, "Total", String.valueOf(d.totalBaris), "Detail Semua Tagihan", TIPE_SEMUA, null, null, "#1d4ed8");
		createMiniClickableChip(mini, "Piutang", uang(d.totalPiutang), "Detail Sisa Piutang", TIPE_PIUTANG, null, null, "#dc2626");
		createMiniClickableChip(mini, "Belum Bayar", String.valueOf(d.jumlahBelumBayar), "Detail Belum Bayar", TIPE_BELUM_BAYAR, null, null, "#b91c1c");
		createMiniClickableChip(mini, "Sebagian", String.valueOf(d.jumlahSebagian), "Detail Bayar Sebagian", TIPE_SEBAGIAN, null, null, "#d97706");
	}

	private void createMiniClickableChip(Component parent, String label, String value, String detailTitle, String tipe,
			String extraSql, Map<String, Object> params, String color) {
		Div chip = new Div();
		chip.setStyle("border-radius:999px; background:#f8fafc; border:1px solid #e2e8f0; padding:6px 10px; display:flex; gap:6px; align-items:center;");
		chip.setParent(parent);
		appendHtml(chip, "<span style='font-size:11px; color:#64748b; font-weight:700;'>" + escapeHtml(label) + "</span>");
		A a = createDetailLink(value, detailTitle, tipe, extraSql, params,
				"font-size:12px; font-weight:900; color:" + color + "; text-decoration:none; cursor:pointer;");
		a.setParent(chip);
	}

	private Div createPanel(Component parent, String title, String extraStyle) {
		Div panel = new Div();
		panel.setStyle((extraStyle == null ? "" : extraStyle) + " background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; "
				+ "box-shadow:0 12px 24px rgba(15,23,42,.06); padding:14px; box-sizing:border-box; margin-bottom:0;");
		panel.setParent(parent);
		appendHtml(panel, "<div style='font-size:13px; font-weight:900; color:#0f172a; margin-bottom:10px;'>" + escapeHtml(title) + "</div>");
		return panel;
	}

	private void createMetricCard(Component parent, String title, String value, String desc, String bg, String color,
			String icon, String detailTitle, String tipe, String extraSql, Map<String, Object> params) {
		Div card = new Div();
		card.setStyle("flex:1 1 170px; min-width:170px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:14px;"
				+ "box-shadow:0 10px 22px rgba(15,23,42,.06); box-sizing:border-box;");
		card.setParent(parent);
		appendHtml(card, "<div style='width:38px; height:38px; border-radius:12px; display:flex; align-items:center; justify-content:center; font-weight:800; background:"
				+ bg + "; color:" + color + ";'>" + escapeHtml(icon) + "</div>");
		A a = createDetailLink(value, detailTitle, tipe, extraSql, params, "display:block; margin-top:8px; font-size:22px; font-weight:900; color:#0f172a; text-decoration:none; cursor:pointer;");
		a.setParent(card);
		appendHtml(card, "<div style='font-size:12px; color:#64748b; margin-top:6px;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:#94a3b8; margin-top:3px;'>" + escapeHtml(desc) + "</div>");
	}

	private void renderFunnelRow(Component parent, String title, int value, int max, String color, String detailTitle,
			String tipe, String extraSql, Map<String, Object> params) {
		Div row = new Div();
		row.setStyle("margin-bottom:10px;");
		row.setParent(parent);
		int pct = max <= 0 ? 0 : Math.max(4, (value * 100) / max);
		appendHtml(row, "<div style='display:flex; align-items:center; gap:10px;'>"
				+ "<div style='width:145px; font-size:12px; color:#334155; font-weight:700;'>" + escapeHtml(title) + "</div>"
				+ "<div style='flex:1; height:12px; border-radius:999px; background:#e5e7eb; overflow:hidden;'>"
				+ "<div style='height:12px; width:" + pct + "%; border-radius:999px; background:" + color + ";'></div>"
				+ "</div></div>");
		A a = createDetailLink(String.valueOf(value), detailTitle, tipe, extraSql, params,
				"float:right; margin-top:-18px; font-size:12px; font-weight:900; color:" + color + "; text-decoration:none; cursor:pointer;");
		a.setParent(row);
		appendHtml(row, "<div style='clear:both;'></div>");
	}

	private void renderMiniGauge(Component parent, String title, int percent, String desc, String color, String detailTitle,
			String tipe, String extraSql, Map<String, Object> params) {
		Div row = new Div();
		row.setStyle("padding:10px 0; border-bottom:1px solid #eef2f7;");
		row.setParent(parent);
		appendHtml(row, "<div style='display:flex; justify-content:space-between; align-items:center; gap:10px;'>"
				+ "<div><div style='font-size:12px; font-weight:800; color:#0f172a;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:#64748b; margin-top:2px;'>" + escapeHtml(desc) + "</div></div></div>"
				+ "<div style='height:10px; margin-top:8px; border-radius:999px; background:#e5e7eb; overflow:hidden;'>"
				+ "<div style='height:10px; width:" + Math.max(0, Math.min(100, percent)) + "%; border-radius:999px; background:" + color + ";'></div></div>");
		A a = createDetailLink(percent + "%", detailTitle, tipe, extraSql, params,
				"float:right; margin-top:-42px; font-size:20px; font-weight:900; color:" + color + "; text-decoration:none; cursor:pointer;");
		a.setParent(row);
		appendHtml(row, "<div style='clear:both;'></div>");
	}

	private void renderTopItemTable(Component parent, List<DashboardGroup> rowsData) {
		if (rowsData == null || rowsData.isEmpty()) {
			appendHtml(parent, "<div style='font-size:12px; color:#64748b;'>Belum ada data item biaya.</div>");
			return;
		}
		Grid g = new Grid();
		g.setSclass("dgrid");
		g.setWidth("100%");
		g.setParent(parent);
		Columns cols = new Columns(); cols.setParent(g);
		new MyColumnConfig("Item Biaya").setParent(cols);
		MyColumnConfig c1 = new MyColumnConfig("Baris"); c1.setAlign("right"); c1.setWidth("70px"); c1.setParent(cols);
		MyColumnConfig c2 = new MyColumnConfig("Tagihan"); c2.setAlign("right"); c2.setParent(cols);
		MyColumnConfig c3 = new MyColumnConfig("Dibayar"); c3.setAlign("right"); c3.setParent(cols);
		MyColumnConfig c4 = new MyColumnConfig("Piutang"); c4.setAlign("right"); c4.setParent(cols);
		Rows rows = new Rows(); rows.setParent(g);
		for (DashboardGroup dg : rowsData) {
			Map<String, Object> p = new HashMap<String, Object>();
			p.put("itemExact", dg.key);
			String extra = " AND ib.nama = :itemExact ";
			Row r = new Row(); r.setParent(rows);
			r.appendChild(new Label(dg.key));
			r.appendChild(createDetailLink(String.valueOf(dg.count), "Detail Item Biaya: " + dg.key, TIPE_SEMUA, extra, p, linkStyleSmall()));
			r.appendChild(createDetailLink(uang(dg.tagihan), "Detail Tagihan Item: " + dg.key, TIPE_TAGIHAN, extra, p, linkStyleSmall()));
			r.appendChild(createDetailLink(uang(dg.dibayar), "Detail Pembayaran Item: " + dg.key, TIPE_DIBAYAR, extra, p, linkStyleSmall()));
			r.appendChild(createDetailLink(uang(dg.piutang), "Detail Piutang Item: " + dg.key, TIPE_PIUTANG, extra, p, linkStyleSmall()));
		}
	}

	private void renderTopPersonTable(Component parent, List<DashboardPerson> rowsData) {
		if (rowsData == null || rowsData.isEmpty()) {
			appendHtml(parent, "<div style='font-size:12px; color:#64748b;'>Tidak ada piutang siswa/calon siswa yang perlu ditagih.</div>");
			return;
		}
		Grid g = new Grid();
		g.setSclass("dgrid");
		g.setWidth("100%");
		g.setParent(parent);
		Columns cols = new Columns(); cols.setParent(g);
		new MyColumnConfig("Nama").setParent(cols);
		new MyColumnConfig("Status").setParent(cols);
		MyColumnConfig c1 = new MyColumnConfig("Baris"); c1.setAlign("right"); c1.setWidth("70px"); c1.setParent(cols);
		MyColumnConfig c2 = new MyColumnConfig("Piutang"); c2.setAlign("right"); c2.setParent(cols);
		Rows rows = new Rows(); rows.setParent(g);
		for (DashboardPerson dp : rowsData) {
			Map<String, Object> p = new HashMap<String, Object>();
			p.put("kodeExact", dp.kode);
			String extra = " AND " + kodeExpr() + " = :kodeExact ";
			Row r = new Row(); r.setParent(rows);
			r.appendChild(new Label(dp.nama + (dp.kode != null && dp.kode.length() > 0 ? " (" + dp.kode + ")" : "")));
			r.appendChild(new Label(dp.status));
			r.appendChild(createDetailLink(String.valueOf(dp.count), "Detail Tagihan: " + dp.nama, TIPE_SEMUA, extra, p, linkStyleSmall()));
			r.appendChild(createDetailLink(uang(dp.piutang), "Detail Piutang: " + dp.nama, TIPE_PIUTANG, extra, p, linkStyleSmall()));
		}
	}

	private void renderPeriodeTrend(Component parent, List<DashboardGroup> rowsData) {
		if (rowsData == null || rowsData.isEmpty()) {
			appendHtml(parent, "<div style='font-size:12px; color:#64748b;'>Belum ada data periode tagihan.</div>");
			return;
		}
		Div chartBox = new Div();
		chartBox.setParent(parent);
		chartBox.setWidth("100%");
		chartBox.setStyle("padding:8px 0; box-sizing:border-box;");
		double max = 0.0;
		for (DashboardGroup dg : rowsData) {
			if (dg == null) continue;
			if (dg.tagihan > max) max = dg.tagihan;
			if (dg.dibayar > max) max = dg.dibayar;
			if (dg.piutang > max) max = dg.piutang;
		}
		if (max <= 0.0) max = 1.0;
		int limit = Math.min(rowsData.size(), 12);
		for (int i = 0; i < limit; i++) {
			DashboardGroup dg = rowsData.get(i);
			if (dg == null) continue;
			renderCssTripleBar(chartBox, dg.key, dg.tagihan, dg.dibayar, dg.piutang, max);
		}
	}


	private void renderDashboardTambahanPiutang(Component parent, DashboardData d, String pencarian, String itemTujuan) {
		Div wrap = new Div();
		wrap.setWidth("100%");
		wrap.setStyle("padding:6px 14px 16px 14px; box-sizing:border-box;");
		wrap.setParent(parent);

		Div intro = new Div();
		intro.setStyle("margin-top:8px; margin-bottom:8px; padding:12px 14px; border-radius:16px; "
				+ "background:#ffffff; border:1px solid #e5e7eb; box-shadow:0 10px 24px rgba(15,23,42,.04);");
		intro.setParent(wrap);
		appendHtml(intro, "<div style='font-size:14px; font-weight:800; color:#0f172a;'>Dashboard Tambahan Kartu Piutang Siswa</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:4px;'>Tambahan analitik ini memecah piutang berdasarkan status siswa/calon siswa, bucket nilai tunggakan, dan prioritas aksi penagihan. Angka di tabel tetap dapat diklik untuk membuka popup detail.</div>");

		Div row1 = new Div();
		row1.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		row1.setParent(wrap);
		Div statusPanel = createPanel(row1, "Komposisi Siswa vs Calon Siswa", "flex:1 1 480px;");
		renderStatusBreakdownTable(statusPanel, d.statusGroups);
		Div bucketPanel = createPanel(row1, "Bucket Nilai Piutang", "flex:1 1 480px;");
		renderBucketPiutangTable(bucketPanel, d.bucketGroups);

		Div row2 = new Div();
		row2.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px;");
		row2.setParent(wrap);
		Div actionPanel = createPanel(row2, "Kartu Prioritas Aksi Penagihan", "flex:1 1 480px;");
		renderActionPriorityCards(actionPanel, d);
		Div agingPanel = createPanel(row2, "Quick Insight Piutang", "flex:1 1 480px;");
		renderQuickInsightPiutang(agingPanel, d);
	}

	private void renderStatusBreakdownTable(Component parent, List<DashboardGroup> rowsData) {
		if (rowsData == null || rowsData.isEmpty()) {
			appendHtml(parent, "<div style='font-size:12px; color:#64748b;'>Belum ada data status siswa/calon siswa.</div>");
			return;
		}
		Grid g = new Grid();
		g.setSclass("dgrid");
		g.setWidth("100%");
		g.setParent(parent);
		Columns cols = new Columns(); cols.setParent(g);
		new MyColumnConfig("Status").setParent(cols);
		MyColumnConfig c1 = new MyColumnConfig("Baris"); c1.setAlign("right"); c1.setParent(cols);
		MyColumnConfig c2 = new MyColumnConfig("Tagihan"); c2.setAlign("right"); c2.setParent(cols);
		MyColumnConfig c3 = new MyColumnConfig("Dibayar"); c3.setAlign("right"); c3.setParent(cols);
		MyColumnConfig c4 = new MyColumnConfig("Piutang"); c4.setAlign("right"); c4.setParent(cols);
		Rows rows = new Rows(); rows.setParent(g);
		for (DashboardGroup dg : rowsData) {
			Row r = new Row(); r.setParent(rows);
			r.appendChild(new Label(dg.key));
			r.appendChild(createDetailLink(String.valueOf(dg.count), "Detail " + dg.key, dg.tipe, null, null, linkStyleSmall()));
			r.appendChild(createDetailLink(uang(dg.tagihan), "Detail Tagihan " + dg.key, dg.tipe, null, null, linkStyleSmall()));
			r.appendChild(createDetailLink(uang(dg.dibayar), "Detail Pembayaran " + dg.key, dg.tipe == null ? TIPE_DIBAYAR : dg.tipe + "__DIBAYAR", statusAndPaidSql(dg.tipe, TIPE_DIBAYAR), null, linkStyleSmall()));
			r.appendChild(createDetailLink(uang(dg.piutang), "Detail Piutang " + dg.key, dg.tipe == null ? TIPE_PIUTANG : dg.tipe + "__PIUTANG", statusAndPaidSql(dg.tipe, TIPE_PIUTANG), null, linkStyleSmall()));
		}
	}

	private String statusAndPaidSql(String statusTipe, String paidTipe) {
		String s = "";
		if (TIPE_SISWA.equals(statusTipe)) {
			s += " AND s.id IS NOT NULL ";
		} else if (TIPE_CALON.equals(statusTipe)) {
			s += " AND s.id IS NULL AND c.id IS NOT NULL ";
		}
		if (TIPE_DIBAYAR.equals(paidTipe)) {
			s += " AND COALESCE(t.dibayar,0) > 0.1 ";
		} else if (TIPE_PIUTANG.equals(paidTipe)) {
			s += " AND " + sisaExpr() + " > 0.1 ";
		}
		return s;
	}

	private void renderBucketPiutangTable(Component parent, List<DashboardGroup> rowsData) {
		if (rowsData == null || rowsData.isEmpty()) {
			appendHtml(parent, "<div style='font-size:12px; color:#64748b;'>Belum ada data bucket piutang.</div>");
			return;
		}
		Grid g = new Grid();
		g.setSclass("dgrid");
		g.setWidth("100%");
		g.setParent(parent);
		Columns cols = new Columns(); cols.setParent(g);
		new MyColumnConfig("Bucket").setParent(cols);
		MyColumnConfig c1 = new MyColumnConfig("Baris"); c1.setAlign("right"); c1.setParent(cols);
		MyColumnConfig c2 = new MyColumnConfig("Piutang"); c2.setAlign("right"); c2.setParent(cols);
		MyColumnConfig c3 = new MyColumnConfig("Collection %"); c3.setAlign("right"); c3.setParent(cols);
		Rows rows = new Rows(); rows.setParent(g);
		for (DashboardGroup dg : rowsData) {
			Row r = new Row(); r.setParent(rows);
			r.appendChild(new Label(dg.key));
			r.appendChild(createDetailLink(String.valueOf(dg.count), "Detail Bucket: " + dg.key, TIPE_SEMUA, dg.extraSql, null, linkStyleSmall()));
			r.appendChild(createDetailLink(uang(dg.piutang), "Detail Piutang Bucket: " + dg.key, TIPE_SEMUA, dg.extraSql, null, linkStyleSmall()));
			int cr = percent(dg.dibayar, dg.tagihan);
			r.appendChild(createDetailLink(cr + "%", "Detail Collection Bucket: " + dg.key, TIPE_SEMUA, dg.extraSql, null, linkStyleSmall()));
		}
	}

	private void renderActionPriorityCards(Component parent, DashboardData d) {
		Div box = new Div();
		box.setStyle("display:flex; gap:10px; flex-wrap:wrap;");
		box.setParent(parent);
		createMetricCard(box, "Segera Ditagih", String.valueOf(d.jumlahBelumBayar), "Belum ada pembayaran", "#fee2e2", "#991b1b", "!", "Detail Prioritas: Belum Bayar", TIPE_BELUM_BAYAR, null, null);
		createMetricCard(box, "Follow-up Cicilan", String.valueOf(d.jumlahSebagian), "Sudah bayar sebagian", "#fef3c7", "#92400e", "↗", "Detail Prioritas: Bayar Sebagian", TIPE_SEBAGIAN, null, null);
		createMetricCard(box, "Validasi Lebih Bayar", String.valueOf(d.jumlahLebihBayar), "Sisa piutang negatif", "#ede9fe", "#5b21b6", "✓", "Detail Lebih Bayar", TIPE_LEBIH_BAYAR, null, null);
		createMetricCard(box, "Tagihan Lunas", String.valueOf(d.jumlahLunas), "Sudah selesai", "#dcfce7", "#166534", "★", "Detail Tagihan Lunas", TIPE_LUNAS, null, null);
	}

	private void renderQuickInsightPiutang(Component parent, DashboardData d) {
		int collectionRate = percent(d.totalDibayar, d.totalTagihan);
		int pressure = percent(d.totalPiutang, d.totalTagihan);
		String status;
		if (pressure >= 60) {
			status = "Tekanan piutang tinggi. Prioritaskan siswa/calon siswa pada watchlist dan bucket terbesar.";
		} else if (pressure >= 30) {
			status = "Tekanan piutang sedang. Lakukan follow-up rutin untuk pembayaran sebagian dan belum bayar.";
		} else {
			status = "Tekanan piutang relatif terkendali. Fokus pada validasi lebih bayar dan tagihan sisa kecil.";
		}
		appendHtml(parent, "<div style='font-size:12px; color:#64748b; line-height:1.6;'>"
				+ "<b>Collection Rate:</b> " + collectionRate + "%<br/>"
				+ "<b>Receivable Pressure:</b> " + pressure + "%<br/>"
				+ "<b>Insight:</b> " + escapeHtml(status) + "</div>");
		Div chips = new Div();
		chips.setStyle("display:flex; gap:8px; flex-wrap:wrap; margin-top:12px;");
		chips.setParent(parent);
		createMiniClickableChip(chips, "Siswa Aktif", String.valueOf(d.jumlahSiswa), "Detail Siswa Aktif", TIPE_SISWA, null, null, "#2563eb");
		createMiniClickableChip(chips, "Calon Siswa", String.valueOf(d.jumlahCalon), "Detail Calon Siswa", TIPE_CALON, null, null, "#7c3aed");
		createMiniClickableChip(chips, "Potongan", uang(d.totalPotongan), "Detail Potongan", TIPE_POTONGAN, null, null, "#d97706");
	}

	@SuppressWarnings("unchecked")
	private void renderChartFromRows(Component parent, List<Object[]> objects) {
		if (chartTampil == null || !chartTampil.isChecked()) {
			return;
		}
		Div wrap = new Div();
		wrap.setWidth("100%");
		wrap.setStyle("padding:0 14px 14px 14px; box-sizing:border-box;");
		wrap.setParent(parent);
		Div boxChart = new Div();
		boxChart.setWidth("100%");
		boxChart.setStyle("background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; "
				+ "box-shadow:0 12px 24px rgba(15,23,42,.06); padding:14px; box-sizing:border-box;");
		boxChart.setParent(wrap);
		appendHtml(boxChart, "<div style='font-size:13px; font-weight:900; color:#0f172a; margin-bottom:4px;'>"
				+ "Ringkasan Halaman Ini Per Item Biaya</div>"
				+ "<div style='font-size:11px; color:#64748b; margin-bottom:10px;'>"
				+ "Grafik dibuat dengan HTML/CSS agar jauh lebih ringan dibanding komponen chart, terutama pada data besar.</div>");
		if (objects == null || objects.isEmpty()) {
			appendHtml(boxChart, "<div style='font-size:12px; color:#b45309; padding:10px; border-radius:10px; "
					+ "background:#fffbeb; border:1px solid #fde68a;'>Tidak ada data pada halaman ini untuk dibuat grafik.</div>");
			return;
		}
		Map<String, double[]> chartDataMap = new HashMap<String, double[]>();
		for (Object[] o : objects) {
			String nmItem = o != null && o.length > 3 && o[3] != null ? o[3].toString() : "Lainnya";
			double tagihanBersih = o != null && o.length > 8 ? toDouble(o[8]) : 0.0;
			double dibayar = o != null && o.length > 6 ? toDouble(o[6]) : 0.0;
			double sisa = o != null && o.length > 9 ? toDouble(o[9]) : 0.0;
			double[] aggregates = chartDataMap.get(nmItem);
			if (aggregates == null) aggregates = new double[] { 0.0, 0.0, 0.0 };
			aggregates[0] += tagihanBersih;
			aggregates[1] += dibayar;
			aggregates[2] += sisa;
			chartDataMap.put(nmItem, aggregates);
		}
		if (chartDataMap.isEmpty()) {
			appendHtml(boxChart, "<div style='font-size:12px; color:#b45309; padding:10px; border-radius:10px; "
					+ "background:#fffbeb; border:1px solid #fde68a;'>Data grafik kosong.</div>");
			return;
		}
		List<Map.Entry<String, double[]>> list = new ArrayList<Map.Entry<String, double[]>>(chartDataMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, double[]>>() {
			public int compare(Map.Entry<String, double[]> a, Map.Entry<String, double[]> b) {
				double av = a == null || a.getValue() == null ? 0.0 : a.getValue()[2];
				double bv = b == null || b.getValue() == null ? 0.0 : b.getValue()[2];
				return bv > av ? 1 : (bv < av ? -1 : 0);
			}
		});
		double max = 0.0;
		for (int i = 0; i < list.size(); i++) {
			double[] v = list.get(i).getValue();
			if (v[0] > max) max = v[0];
			if (v[1] > max) max = v[1];
			if (v[2] > max) max = v[2];
		}
		if (max <= 0.0) max = 1.0;
		int limit = Math.min(list.size(), 12);
		for (int i = 0; i < limit; i++) {
			Map.Entry<String, double[]> e = list.get(i);
			double[] v = e.getValue();
			renderCssTripleBar(boxChart, e.getKey(), v[0], v[1], v[2], max);
		}
	}


	private void renderCssTripleBar(Component parent, String label, double tagihan, double dibayar, double piutang, double max) {
		if (max <= 0.0) max = 1.0;
		int pTagihan = cssPercent(tagihan, max);
		int pDibayar = cssPercent(dibayar, max);
		int pPiutang = cssPercent(piutang, max);
		appendHtml(parent, "<div style='margin:9px 0 12px 0; padding:10px; border:1px solid #eef2f7; border-radius:12px; background:#fbfdff;'>"
				+ "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:7px;'>" + escapeHtml(label) + "</div>"
				+ cssBarRow("Tagihan", uang(tagihan), pTagihan, "#2563eb")
				+ cssBarRow("Dibayar", uang(dibayar), pDibayar, "#16a34a")
				+ cssBarRow("Piutang", uang(piutang), pPiutang, "#dc2626")
				+ "</div>");
	}

	private String cssBarRow(String title, String value, int percent, String color) {
		if (percent < 1) percent = 1;
		if (percent > 100) percent = 100;
		return "<div style='display:flex; align-items:center; gap:8px; margin:4px 0;'>"
				+ "<div style='width:72px; font-size:11px; color:#64748b; font-weight:700;'>" + escapeHtml(title) + "</div>"
				+ "<div style='flex:1; height:9px; border-radius:999px; background:#e5e7eb; overflow:hidden;'>"
				+ "<div style='width:" + percent + "%; height:9px; border-radius:999px; background:" + color + ";'></div></div>"
				+ "<div style='width:118px; text-align:right; font-size:11px; color:#334155; font-weight:800;'>" + escapeHtml(value) + "</div>"
				+ "</div>";
	}

	private int cssPercent(double value, double max) {
		if (max <= 0.0 || value <= 0.0) return 1;
		int p = (int) Math.round((value * 100.0) / max);
		return p < 1 ? 1 : (p > 100 ? 100 : p);
	}

	private Label createGridLabel(String value) {
		Label l = new Label(value == null ? "" : value);
		l.setStyle("display:block; font-size:11px; padding:4px 6px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;");
		l.setTooltiptext(value == null ? "" : value);
		return l;
	}

	private void renderDetailGrid(Component parent, List<Object[]> objects) {
		Div wrap = new Div();
		wrap.setWidth("100%");
		wrap.setStyle("padding:0 14px 14px 14px; box-sizing:border-box; overflow-x:auto; overflow-y:hidden;");
		wrap.setParent(parent);

		grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("1120px");
		grid.setParent(wrap);
		grid.setMold("paging");
		grid.setPageSize(GRID_PAGE_SIZE_INTERNAL);
		try { grid.getPagingChild().setMold("os"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRinciSekolah.java:1159");}
		grid.setStyle("background:#ffffff; border:1px solid #e5e7eb; border-radius:14px; overflow:visible; min-height:160px;");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig colKode = new MyColumnConfig("NIS/No.Reg"); colKode.setWidth("120px"); colKode.setParent(columns);
		MyColumnConfig colNama = new MyColumnConfig("Nama Siswa"); colNama.setWidth("220px"); colNama.setParent(columns);
		MyColumnConfig colStatus = new MyColumnConfig("Status"); colStatus.setWidth("120px"); colStatus.setParent(columns);
		MyColumnConfig colPeriode = new MyColumnConfig("Periode"); colPeriode.setWidth("90px"); colPeriode.setParent(columns);
		MyColumnConfig colT = new MyColumnConfig("Tagihan"); colT.setAlign("right"); colT.setWidth("135px"); colT.setParent(columns);
		MyColumnConfig colD = new MyColumnConfig("Dibayar"); colD.setAlign("right"); colD.setWidth("135px"); colD.setParent(columns);
		MyColumnConfig colS = new MyColumnConfig("Sisa Piutang"); colS.setAlign("right"); colS.setWidth("145px"); colS.setParent(columns);
		MyColumnConfig colP = new MyColumnConfig("% Lunas"); colP.setAlign("right"); colP.setWidth("110px"); colP.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		String grupItemLama = "";
		Double tTagihanGrup = 0.0, tDibayarGrup = 0.0, tSisaGrup = 0.0;
		Double gTagihan = 0.0, gDibayar = 0.0, gSisa = 0.0;

		for (Object[] o : objects) {
			String nis = o[0] != null ? o[0].toString() : "";
			String nm = o[1] != null ? o[1].toString() : "";
			String status = o[2] != null ? o[2].toString() : "";
			String itemAktual = o[3] != null ? o[3].toString() : "";
			String periode = o[4] != null ? o[4].toString() : "-";
			double dibayar = toDouble(o[6]);
			double tagihanBersih = toDouble(o[8]);
			double sisa = toDouble(o[9]);

			if (!grupItemLama.equalsIgnoreCase(itemAktual)) {
				if (!grupItemLama.equals("")) {
					appendSubtotalRow(rows, grupItemLama, tTagihanGrup, tDibayarGrup, tSisaGrup);
					gTagihan += tTagihanGrup; gDibayar += tDibayarGrup; gSisa += tSisaGrup;
					tTagihanGrup = 0.0; tDibayarGrup = 0.0; tSisaGrup = 0.0;
				}
				Row rowHead = new Row(); rowHead.setParent(rows); rowHead.setStyle("background-color:#e0ebf5;");
				rowHead.appendChild(new Label());
				rowHead.appendChild(new MyLabelBoldMerah(itemAktual));
				rowHead.appendChild(new Label()); rowHead.appendChild(new Label());
				rowHead.appendChild(new Label()); rowHead.appendChild(new Label());
				rowHead.appendChild(new Label()); rowHead.appendChild(new Label());
				grupItemLama = itemAktual;
			}

			tTagihanGrup += tagihanBersih;
			tDibayarGrup += dibayar;
			tSisaGrup += sisa;

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("kodeExact", nis);
			params.put("itemExact", itemAktual);
			params.put("periodeExact", periode);
			String extra = " AND " + kodeExpr() + " = :kodeExact AND ib.nama = :itemExact AND " + periodeExpr() + " = :periodeExact ";

			Row rowData = new Row();
			rowData.setParent(rows);
			rowData.setStyle("min-height:30px;");
			rowData.appendChild(createGridLabel(nis));
			rowData.appendChild(createGridLabel(nm));
			rowData.appendChild(createGridLabel(status));
			rowData.appendChild(createGridLabel(periode));
			rowData.appendChild(createDetailLink(uang(tagihanBersih), "Detail Tagihan: " + nm, TIPE_TAGIHAN, extra, params, linkStyleSmall()));
			rowData.appendChild(createDetailLink(uang(dibayar), "Detail Pembayaran: " + nm, TIPE_DIBAYAR, extra, params, linkStyleSmall()));
			rowData.appendChild(createDetailLink(uang(sisa), "Detail Piutang: " + nm, TIPE_PIUTANG, extra, params, linkStyleSmall()));
			Double plunas = tagihanBersih > 0 ? (dibayar * 100.0) / tagihanBersih : 0.0;
			rowData.appendChild(createDetailLink(Common.numberFormat.get().format(plunas) + "%", "Detail Persentase Lunas: " + nm, TIPE_SEMUA, extra, params, linkStyleSmall()));
		}

		if (!grupItemLama.equals("")) {
			appendSubtotalRow(rows, grupItemLama, tTagihanGrup, tDibayarGrup, tSisaGrup);
			gTagihan += tTagihanGrup; gDibayar += tDibayarGrup; gSisa += tSisaGrup;
		}

		Foot foot = new Foot();
		foot.setParent(grid);
		Footer ft1 = new Footer(); ft1.setParent(foot); ft1.appendChild(new MyLabelBold("TOTAL HALAMAN"));
		new Footer().setParent(foot); new Footer().setParent(foot); new Footer().setParent(foot);
		Footer ftTag = new Footer(); ftTag.setParent(foot); ftTag.setAlign("right");
		ftTag.appendChild(createDetailLink(uang(gTagihan), "Detail Total Tagihan Halaman", TIPE_TAGIHAN, null, null, linkStyleSmall()));
		Footer ftDib = new Footer(); ftDib.setParent(foot); ftDib.setAlign("right");
		ftDib.appendChild(createDetailLink(uang(gDibayar), "Detail Total Dibayar Halaman", TIPE_DIBAYAR, null, null, linkStyleSmall()));
		Footer ftSis = new Footer(); ftSis.setParent(foot); ftSis.setAlign("right");
		ftSis.appendChild(createDetailLink(uang(gSisa), "Detail Total Piutang Halaman", TIPE_PIUTANG, null, null, linkStyleSmall()));
		Footer ftPer = new Footer(); ftPer.setParent(foot); ftPer.setAlign("right");
		Double gp = gTagihan > 0 ? (gDibayar * 100.0) / gTagihan : 0.0;
		ftPer.appendChild(createDetailLink(Common.numberFormat.get().format(gp) + "%", "Detail Semua Tagihan", TIPE_SEMUA, null, null, linkStyleSmall()));
	}

	private void appendSubtotalRow(Rows rows, String label, double tagihan, double dibayar, double sisa) {
		Row rowSub = new Row(); rowSub.setParent(rows); rowSub.setStyle("background-color:#f2f2f2;");
		rowSub.appendChild(new MyLabelBoldMerah("Total Sub"));
		rowSub.appendChild(new MyLabelBoldMerah(label));
		rowSub.appendChild(new Label()); rowSub.appendChild(new Label());
		Map<String, Object> p = new HashMap<String, Object>();
		p.put("itemExact", label);
		String extra = " AND ib.nama = :itemExact ";
		rowSub.appendChild(createDetailLink(uang(tagihan), "Detail Subtotal Tagihan: " + label, TIPE_TAGIHAN, extra, p, linkStyleSmall()));
		rowSub.appendChild(createDetailLink(uang(dibayar), "Detail Subtotal Dibayar: " + label, TIPE_DIBAYAR, extra, p, linkStyleSmall()));
		rowSub.appendChild(createDetailLink(uang(sisa), "Detail Subtotal Piutang: " + label, TIPE_PIUTANG, extra, p, linkStyleSmall()));
		Double ps = tagihan > 0 ? (dibayar * 100.0) / tagihan : 0;
		rowSub.appendChild(createDetailLink(Common.numberFormat.get().format(ps) + "%", "Detail Subtotal: " + label, TIPE_SEMUA, extra, p, linkStyleSmall()));
	}

	private String linkStyleSmall() {
		return "font-size:11px; font-weight:800; color:#2563eb; text-decoration:none; cursor:pointer;";
	}

	private A createDetailLink(String text, final String title, final String tipe, final String extraSql, final Map<String, Object> params, String style) {
		A a = new A(text == null ? "0" : text);
		a.setStyle(style == null ? linkStyleSmall() : style);
		a.setTooltiptext("Klik untuk melihat detail data");
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showDetailPopup(title, tipe, extraSql, params);
			}
		});
		return a;
	}

	@SuppressWarnings("unchecked")
	private void showDetailPopup(String title, String tipe, String extraSql, Map<String, Object> params) {
		Window win = new Window();
		win.setTitle(title == null ? "Detail Data" : title);
		win.setWidth(Common.isMobile() ? "96%" : "980px");
		win.setHeight(Common.isMobile() ? "88%" : "620px");
		win.setClosable(true);
		win.setSizable(true);
		win.setMaximizable(true);
		win.setStyle("border-radius:14px; overflow:hidden;");

		Vbox box = new Vbox();
		box.setWidth("100%");
		box.setHeight("100%");
		box.setStyle("background:#f8fafc; overflow:auto; padding:12px; box-sizing:border-box;");
		box.setParent(win);

		Session session = null;
		try {
			String pencarian = getSearchNamaValue();
			String itemTujuan = getSearchItemValue();
			String sqlBase = buildSqlBase(pencarian, itemTujuan) + filterByTipe(tipe) + (extraSql == null ? "" : extraSql);
			session = HibernateUtil.getSessionFactory().openSession();

			String sqlSummary = "SELECT COUNT(t.id), COALESCE(SUM(" + nettoExpr() + "),0), "
					+ "COALESCE(SUM(COALESCE(t.dibayar,0)),0), COALESCE(SUM(" + sisaExpr() + "),0), "
					+ "COALESCE(SUM(COALESCE(t.diskon,0)),0) " + sqlBase;
			SQLQuery qSummary = session.createSQLQuery(sqlSummary);
			applyBaseParams(qSummary, pencarian, itemTujuan);
			applyExtraParams(qSummary, params);
			Object[] sum = (Object[]) qSummary.uniqueResult();
			int total = sum == null ? 0 : toInt(sum[0]);
			double tagihan = sum == null ? 0 : toDouble(sum[1]);
			double dibayar = sum == null ? 0 : toDouble(sum[2]);
			double piutang = sum == null ? 0 : toDouble(sum[3]);
			double potongan = sum == null ? 0 : toDouble(sum[4]);

			Div summary = new Div();
			summary.setStyle("display:flex; gap:10px; flex-wrap:wrap; margin-bottom:10px;");
			summary.setParent(box);
			createPopupMetric(summary, "Baris", String.valueOf(total));
			createPopupMetric(summary, "Tagihan Netto", uang(tagihan));
			createPopupMetric(summary, "Dibayar", uang(dibayar));
			createPopupMetric(summary, "Sisa Piutang", uang(piutang));
			createPopupMetric(summary, "Potongan", uang(potongan));

			appendHtml(box, "<div style='font-size:11px; color:#64748b; margin-bottom:8px;'>Ditampilkan maksimal " + DETAIL_LIMIT + " baris pertama. Gunakan filter utama untuk mempersempit data bila diperlukan.</div>");

			String sqlData = detailSelectSql() + sqlBase + " ORDER BY ib.nama, status, nama LIMIT " + DETAIL_LIMIT;
			SQLQuery qData = session.createSQLQuery(sqlData);
			applyBaseParams(qData, pencarian, itemTujuan);
			applyExtraParams(qData, params);
			List<Object[]> list = qData.list();

			Grid g = new Grid();
			g.setSclass("dgrid");
			g.setWidth("100%");
			g.setMold("paging");
			g.setPageSize(DETAIL_PAGE_SIZE);
			try { g.getPagingChild().setMold("os"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRinciSekolah.java:1340");}
			g.setParent(box);

			Columns cols = new Columns(); cols.setParent(g);
			new MyColumnConfig("NIS/No.Reg").setParent(cols);
			new MyColumnConfig("Nama").setParent(cols);
			new MyColumnConfig("Status").setParent(cols);
			new MyColumnConfig("Item Biaya").setParent(cols);
			new MyColumnConfig("Periode").setParent(cols);
			MyColumnConfig ct = new MyColumnConfig("Tagihan"); ct.setAlign("right"); ct.setParent(cols);
			MyColumnConfig cd = new MyColumnConfig("Dibayar"); cd.setAlign("right"); cd.setParent(cols);
			MyColumnConfig cp = new MyColumnConfig("Potongan"); cp.setAlign("right"); cp.setParent(cols);
			MyColumnConfig cs = new MyColumnConfig("Sisa"); cs.setAlign("right"); cs.setParent(cols);

			Rows rows = new Rows(); rows.setParent(g);
			if (list != null) {
				for (Object[] o : list) {
					Row r = new Row(); r.setParent(rows);
					r.appendChild(new Label(o[0] == null ? "" : String.valueOf(o[0])));
					r.appendChild(new Label(o[1] == null ? "" : String.valueOf(o[1])));
					r.appendChild(new Label(o[2] == null ? "" : String.valueOf(o[2])));
					r.appendChild(new Label(o[3] == null ? "" : String.valueOf(o[3])));
					r.appendChild(new Label(o[4] == null ? "" : String.valueOf(o[4])));
					r.appendChild(new Label(uang(toDouble(o[8]))));
					r.appendChild(new Label(uang(toDouble(o[6]))));
					r.appendChild(new Label(uang(toDouble(o[7]))));
					r.appendChild(new Label(uang(toDouble(o[9]))));
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/keuangan/DasboardPiutangRinciSekolah.java:1370");
			appendHtml(box, "<div style='padding:12px; border-radius:12px; background:#fff7ed; border:1px solid #fed7aa; color:#9a3412;'>Detail belum dapat dimuat. " + escapeHtml(e.getMessage()) + "</div>");
		} finally {
			cleanupSession(session);
		}

		win.setParent(this);
		try {
			win.doModal();
		} catch (Exception e) {
			win.doOverlapped();
		}
	}

	private void createPopupMetric(Component parent, String title, String value) {
		Div d = new Div();
		d.setStyle("flex:1 1 130px; min-width:130px; background:#ffffff; border:1px solid #e5e7eb; border-radius:12px; padding:10px; box-sizing:border-box;");
		d.setParent(parent);
		appendHtml(d, "<div style='font-size:11px; color:#64748b;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:16px; font-weight:900; color:#0f172a; margin-top:3px;'>" + escapeHtml(value) + "</div>");
	}

	// =========================================================================
	// HELPER UNTUK EXPORT DASHBOARD KE EXCEL (Diambil dari semua data tanpa limit)
	// =========================================================================
	@SuppressWarnings({ "unchecked", "resource" })
	private void unduhExcelDanDasbor() {
		Session session = null;
		try {
			String pencarian = getSearchNamaValue();
			String itemTujuan = getSearchItemValue();
			String sqlBase = buildSqlBase(pencarian, itemTujuan);
			String sqlData = detailSelectSql() + sqlBase + " ORDER BY ib.nama, status, nama ";

			session = HibernateUtil.getSessionFactory().openSession();
			SQLQuery qData = session.createSQLQuery(sqlData);
			applyBaseParams(qData, pencarian, itemTujuan);
			List<Object[]> objects = qData.list();
			if (objects == null || objects.isEmpty()) return;

			String filename = Executions.getCurrent().getDesktop().getWebApp().getRealPath("/tmp/dasbor_piutang_sekolah_" + System.currentTimeMillis() + ".xlsx");
			File file = new File(filename); file.createNewFile();
			XSSFWorkbook workbook = new XSSFWorkbook();

			XSSFSheet sheet = workbook.createSheet("DATA RINCIAN PIUTANG");
			sheet.setDefaultColumnWidth(20);
			XSSFRow rowhead = sheet.createRow(0);
			rowhead.createCell(0).setCellValue("NIS/NO.REG"); rowhead.createCell(1).setCellValue("NAMA SISWA");
			rowhead.createCell(2).setCellValue("STATUS"); rowhead.createCell(3).setCellValue("ITEM BIAYA");
			rowhead.createCell(4).setCellValue("PERIODE"); rowhead.createCell(5).setCellValue("TAGIHAN NETTO (Rp)");
			rowhead.createCell(6).setCellValue("DIBAYAR (Rp)"); rowhead.createCell(7).setCellValue("POTONGAN (Rp)");
			rowhead.createCell(8).setCellValue("SISA PIUTANG (Rp)");

			int r = 1; double tTagihan = 0, tDibayar = 0, tPiutang = 0, tPotongan = 0;
			Map<String, Double> mapTunggakanTop5 = new HashMap<String, Double>();

			for (Object[] o : objects) {
				String nis = o[0] != null ? o[0].toString() : "";
				String nm = o[1] != null ? o[1].toString() : "";
				String status = o[2] != null ? o[2].toString() : "";
				String itemAktual = o[3] != null ? o[3].toString() : "";
				String periode = o[4] != null ? o[4].toString() : "-";
				double dibayar = toDouble(o[6]);
				double potongan = toDouble(o[7]);
				double tagihanBersih = toDouble(o[8]);
				double sisa = toDouble(o[9]);

				XSSFRow row = sheet.createRow(r++);
				row.createCell(0).setCellValue(nis); row.createCell(1).setCellValue(nm);
				row.createCell(2).setCellValue(status); row.createCell(3).setCellValue(itemAktual);
				row.createCell(4).setCellValue(periode); row.createCell(5).setCellValue(tagihanBersih);
				row.createCell(6).setCellValue(dibayar); row.createCell(7).setCellValue(potongan); row.createCell(8).setCellValue(sisa);

				tTagihan += tagihanBersih; tDibayar += dibayar; tPotongan += potongan; tPiutang += sisa;
				String keyTop5 = nm + " (" + nis + ")";
				Double curTgk = mapTunggakanTop5.get(keyTop5);
				mapTunggakanTop5.put(keyTop5, (curTgk == null ? 0 : curTgk.doubleValue()) + sisa);
			}

			XSSFRow rowfoot = sheet.createRow(r);
			rowfoot.createCell(4).setCellValue("TOTAL KESELURUHAN");
			rowfoot.createCell(5).setCellValue(tTagihan);
			rowfoot.createCell(6).setCellValue(tDibayar);
			rowfoot.createCell(7).setCellValue(tPotongan);
			rowfoot.createCell(8).setCellValue(tPiutang);

			XSSFSheet dashSheet = workbook.createSheet("DASHBOARD ANALITIK");
			dashSheet.setDefaultColumnWidth(30);
			XSSFCellStyle headerStyle = workbook.createCellStyle();
			XSSFFont fontBold = workbook.createFont(); fontBold.setBold(true);
			headerStyle.setFont(fontBold);
			headerStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(200, 220, 240)));

			XSSFRow summaryHead = dashSheet.createRow(0);
			summaryHead.createCell(0).setCellValue("RINGKASAN");
			summaryHead.createCell(1).setCellValue("NILAI");
			dashSheet.createRow(1).createCell(0).setCellValue("Total Tagihan Netto"); dashSheet.getRow(1).createCell(1).setCellValue(tTagihan);
			dashSheet.createRow(2).createCell(0).setCellValue("Total Dibayar"); dashSheet.getRow(2).createCell(1).setCellValue(tDibayar);
			dashSheet.createRow(3).createCell(0).setCellValue("Total Potongan"); dashSheet.getRow(3).createCell(1).setCellValue(tPotongan);
			dashSheet.createRow(4).createCell(0).setCellValue("Total Sisa Piutang"); dashSheet.getRow(4).createCell(1).setCellValue(tPiutang);

			XSSFRow dHead = dashSheet.createRow(6);
			XSSFCell c0 = dHead.createCell(0); c0.setCellValue("REKAPITULASI TOP 5 PIUTANG SISWA TERTINGGI"); c0.setCellStyle(headerStyle);
			XSSFCell c1 = dHead.createCell(1); c1.setCellValue("TOTAL PIUTANG (Rp)"); c1.setCellStyle(headerStyle);
			XSSFCell c2 = dHead.createCell(2); c2.setCellValue("TREN VISUAL"); c2.setCellStyle(headerStyle);

			List<Map.Entry<String, Double>> listTunggakan = new ArrayList<Map.Entry<String, Double>>(mapTunggakanTop5.entrySet());
			Collections.sort(listTunggakan, new Comparator<Map.Entry<String, Double>>() {
				public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
					return Double.compare(o2.getValue(), o1.getValue());
				}
			});

			double maxTunggakan = listTunggakan.isEmpty() ? 0 : listTunggakan.get(0).getValue();
			int topCount = 7;
			for (Map.Entry<String, Double> entry : listTunggakan) {
				if (topCount > 11) break;
				if (entry.getValue() <= 0) continue;
				XSSFRow dRow = dashSheet.createRow(topCount);
				dRow.createCell(0).setCellValue(entry.getKey());
				dRow.createCell(1).setCellValue(entry.getValue());
				double persenGrafik = maxTunggakan > 0 ? (entry.getValue() * 100.0) / maxTunggakan : 0;
				int barLength = (int) (persenGrafik / 5);
				StringBuilder bar = new StringBuilder();
				for (int i = 0; i < barLength; i++) { bar.append("█"); }
				dRow.createCell(2).setCellValue(bar.toString());
				topCount++;
			}

			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut); fileOut.close();
			Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Rekap_Piutang_Sekolah.xlsx");
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			cleanupSession(session);
		}
	}

	/**
	 * Pembawa data/helper lokal milik {@link DasboardPiutangRinciSekolah} untuk dashboard data. Tipe ini
	 * mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
	 * jelas.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * DasboardPiutangRinciSekolah}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int totalBaris}, {@code int
	 * jumlahPiutang}, {@code int jumlahLunas}, {@code int jumlahBelumBayar}, {@code int jumlahSebagian}, {@code
	 * int jumlahLebihBayar}, {@code int jumlahSiswa}, {@code int jumlahCalon}. Aturan bisnis bersama tetap berada
	 * pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see DasboardPiutangRinciSekolah
	 */
	private static class DashboardData {
		int totalBaris;
		int jumlahPiutang;
		int jumlahLunas;
		int jumlahBelumBayar;
		int jumlahSebagian;
		int jumlahLebihBayar;
		int jumlahSiswa;
		int jumlahCalon;
		double totalTagihan;
		double totalDibayar;
		double totalPiutang;
		double totalPotongan;
		List<DashboardGroup> topItems = new ArrayList<DashboardGroup>();
		List<DashboardPerson> topPersons = new ArrayList<DashboardPerson>();
		List<DashboardGroup> periode = new ArrayList<DashboardGroup>();
		List<DashboardGroup> statusGroups = new ArrayList<DashboardGroup>();
		List<DashboardGroup> bucketGroups = new ArrayList<DashboardGroup>();
	}

	/**
	 * Tipe implementasi bersarang {@link DashboardGroup} milik {@link DasboardPiutangRinciSekolah}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * DasboardPiutangRinciSekolah}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String key}, {@code String tipe},
	 * {@code String extraSql}, {@code int count}, {@code double tagihan}, {@code double dibayar}, {@code double
	 * piutang}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see DasboardPiutangRinciSekolah
	 */
	private static class DashboardGroup {
		String key;
		String tipe;
		String extraSql;
		int count;
		double tagihan;
		double dibayar;
		double piutang;
	}

	/**
	 * Tipe implementasi bersarang {@link DashboardPerson} milik {@link DasboardPiutangRinciSekolah}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * DasboardPiutangRinciSekolah}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String kode}, {@code String nama},
	 * {@code String status}, {@code int count}, {@code double tagihan}, {@code double dibayar}, {@code double
	 * piutang}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see DasboardPiutangRinciSekolah
	 */
	private static class DashboardPerson {
		String kode;
		String nama;
		String status;
		int count;
		double tagihan;
		double dibayar;
		double piutang;
	}
}
