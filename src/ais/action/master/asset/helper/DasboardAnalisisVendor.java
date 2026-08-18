package ais.action.master.asset.helper;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


/*
 * REGENERATE_V2_CLICKABLE_INSIGHT_TAGIHAN_PR_PO_BAST_2026_05_30
 * File sengaja dibuat dengan nama berbeda agar tidak tertukar/cache dengan file sebelumnya.
 * Saat dipasang ke project, rename kembali menjadi DasboardAnalisisVendor.java
 * karena public class di dalam file ini tetap: public class DasboardAnalisisVendor.
 *
 * Tambahan utama versi ini:
 * - Insight Produk & Rekomendasi Vendor: semua angka/card clickable ke popup detail paging 10.
 * - Dasbor Terima Tagihan Vendor: SaldoAwalMasterAsset + SaldoAwalMasterAssetDetail.
 * - Dasbor Purchase Request: PermintaanPengadaanMasterAsset + PermintaanPengadaanMasterAssetDetail.
 * - Dasbor Purchase Order: PemesananPengadaanMasterAsset + PemesananPengadaanMasterAssetDetail.
 * - Dasbor Terima Barang/BAST: PenerimaanPengadaanMasterAsset + PenerimaanPengadaanMasterAssetDetail.
 */

/*
 * DASBOARD_ANALISIS_VENDOR_V2_CLICKABLE_TAGIHAN_PR_PO_BAST_2026_05_30
 *
 * Dashboard baru untuk analisis Vendor/Penyedia dan Produk.
 * - Analisa Vendor: transaksi, quantity ordered/received, nominal, pembayaran, skor kualitas, skor ketepatan waktu, skor layanan, evaluasi.
 * - Analisa Produk: vendor, produk, tanggal awal/akhir transaksi, qty, nominal, barang dipesan, barang datang.
 *
 * Catatan desain:
 * - Semua child langsung dari MyPortallayout memakai MyPortalchildren agar aman pada ZK MyPortallayout.
 * - Popup detail ditempel ke Page, bukan ke MyPortallayout.
 * - Pembacaan skor BAST memakai reflection agar tetap compile walaupun nama field penilaian BAST berbeda antar versi.
 * - Jika field penilaian BAST belum tersedia, dashboard tetap aman dan memakai fallback konservatif.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.A;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Timer;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PembayaranDpMasterAsset;
import ais.database.model.asset.PembayaranPengadaanMasterAsset;
import ais.database.model.asset.PembayaranTerminMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.PenyediaAssetPunyaDokumen;
import ais.database.model.asset.PerjanjianKerjasamaMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.asset.ReturPengadaanMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import ais.database.model.inventory.PengadaanProduk;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Membandingkan vendor, produk, tagihan, PR, PO, dan penerimaan barang agar kinerja penyedia mudah dievaluasi.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardAnalisisVendor extends MyPortallayout {

	private static final long serialVersionUID = 8852964002115737935L;

	public static boolean debug = false;
	private static final int PAGE_SIZE = 10;
	private static final int SAMPLE_LIMIT = 2000;

	private MyDatebox dbMulai;
	private MyDatebox dbSampai;
	private Combobox cbVendor;
	private Textbox txtKeyword;
	private MyPortalchildren mainColumn;
	private Panel filterPanel;
	private Component progressPanel;
	private MyPortalchildren progressPortal;
	private Div progressFill;
	private Label progressPercent;
	private Label progressTitle;
	private Label progressDetail;

	private VendorDashboardData currentData;

	private static final String[] QUALITY_METHODS = new String[] {
			"getNilaiKinerjaKualitas", "getNilaiKualitas", "getKinerjaKualitas", "getPenilaianKualitas",
			"getSkorKualitas", "getScoreKualitas", "getNilaiMutu", "getNilaiSpesifikasi" };
	private static final String[] TIME_METHODS = new String[] {
			"getNilaiKinerjaKetepatanWaktuPengiriman", "getNilaiKinerjaKetepatanWaktu",
			"getNilaiKetepatanWaktuPengiriman", "getNilaiKetepatanWaktu", "getKinerjaKetepatanWaktu",
			"getSkorKetepatanWaktu", "getScoreKetepatanWaktu" };
	private static final String[] SERVICE_METHODS = new String[] {
			"getNilaiKinerjaLayanan", "getNilaiLayanan", "getKinerjaLayanan", "getPenilaianLayanan",
			"getSkorLayanan", "getScoreLayanan", "getNilaiPelayanan", "getNilaiResponseLayanan" };

	public DasboardAnalisisVendor() throws Exception {
		super();
		setWidth("100%");
		setMaximizedMode("whole");
		init();
	}

	public static void setDebug(boolean debugValue) {
		debug = debugValue;
	}

	public static boolean isDebug() {
		return debug;
	}

	private static void debugError(String context, Exception e) {
		if (!debug) {
			return;
		}
		try {
			System.err.println("[DasboardAnalisisVendor DEBUG] " + context + " : " + (e == null ? "" : e.getMessage()));
			if (e != null) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:173");
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:175");
		}
	}

	private void init() throws Exception {
		Common.clear(this);
		initDefaultFilter();
		renderFilterPanel();
		requestReloadDashboard();
	}

	private void initDefaultFilter() {
		Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
		cal.set(Calendar.MONTH, 0);
		cal.set(Calendar.DATE, 1);
		Date mulai = cal.getTime();
		Date sampai = ais.ui.util.WaktuUtil.getDate();
		dbMulai = new MyDatebox(mulai);
		dbSampai = new MyDatebox(sampai);
		dbMulai.setReadonly(true);
		dbSampai.setReadonly(true);
		cbVendor = new Combobox();
		txtKeyword = new Textbox();
	}

	private MyPortalchildren getMainColumn() {
		try {
			if (mainColumn == null || mainColumn.getParent() == null) {
				mainColumn = new MyPortalchildren();
				mainColumn.setWidth("100%");
				mainColumn.setStyle("width:100%; max-width:100%; padding:6px; box-sizing:border-box; overflow:visible;");
				mainColumn.setParent(this);
			}
		} catch (Exception e) {
			debugError("getMainColumn", e);
		}
		return mainColumn;
	}

	private void renderFilterPanel() {
		MyPortalchildren pc = getMainColumn();

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setTitle("Filter Analisis Vendor & Produk");
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle(modernPanelStyle());
		panel.setParent(pc);
		filterPanel = panel;
		panel.setAttribute("dashboardFixedPanel", Boolean.TRUE);

		Panelchildren pch = new Panelchildren();
		pch.setStyle("padding:12px; background:#ffffff;");
		pch.setParent(panel);

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("border:0; background:#f8fafc; border-radius:14px; padding:10px; display:flex; flex-wrap:wrap; gap:8px; align-items:center;");
		toolbar.setParent(pch);

		new MyLabelAgakKecil("Mulai:").setParent(toolbar);
		dbMulai.setCols(8);
		dbMulai.setParent(toolbar);

		new MyLabelAgakKecil("Sampai:").setParent(toolbar);
		dbSampai.setCols(8);
		dbSampai.setParent(toolbar);

		new MyLabelAgakKecil("Vendor:").setParent(toolbar);
		cbVendor.setCols(20);
		cbVendor.setReadonly(true);
		cbVendor.setParent(toolbar);
		try {
			Common.insertComboDanSemua(cbVendor, "nama", PenyediaAsset.class);
			Common.selectComboItem(cbVendor, null);
		} catch (Exception e) {
			debugError("renderFilterPanel.vendor-combo", e);
		}

		new MyLabelAgakKecil("Cari:").setParent(toolbar);
		txtKeyword.setCols(18);
		txtKeyword.setParent(toolbar);

		MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig("Tampilkan Analisis", "/img/svg/search.svg");
		btn.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; padding:7px 14px;");
		btn.setParent(toolbar);
		btn.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				requestReloadDashboard();
			}
		});

		EventListener reload = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				requestReloadDashboard();
			}
		};
		dbMulai.addEventListener("onChange", reload);
		dbSampai.addEventListener("onChange", reload);
		cbVendor.addEventListener("onChange", reload);
		txtKeyword.addEventListener("onOK", reload);
	}

	private void requestReloadDashboard() {
		clearDashboardResultChildren();
		showProgress(1, "Menyiapkan tampilan", "Filter sudah diterima, data analisis akan dimuat sebentar lagi.");
		try {
			final Timer timer = new Timer();
			timer.setDelay(200);
			timer.setRepeats(false);
			timer.addEventListener("onTimer", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						timer.detach();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:294");
					}
					reloadDashboard();
				}
			});
			timer.setParent(this);
		} catch (Exception e) {
			debugError("requestReloadDashboard.timer", e);
			reloadDashboard();
		}
	}

	private void reloadDashboard() {
		try {
			clearDashboardResultChildren();
			showProgress(3, "Membaca filter", "Menyiapkan periode, vendor, dan kata kunci pencarian.");
			updateProgress(12, "Mengambil data pengadaan", "Membaca data PR, PO, tagihan, BAST, retur, produk, dan dokumen vendor.");
			currentData = loadDashboardData();
			updateProgress(58, "Menghitung indikator", "Menggabungkan nominal, kuantitas, skor vendor, kepatuhan dokumen, dan risiko.");
			renderOverview(currentData);
			updateProgress(68, "Menyusun grafik", "Membuat visual ringkas, tren, dan spider web berbasis HTML/CSS.");
			renderExecutiveVisualAnalytics(currentData);
			updateProgress(76, "Menampilkan tabel", "Menyiapkan tabel analisa vendor dan produk dengan paging.");
			renderVendorAnalysisGrid(currentData.vendorRows);
			renderProductAnalysisGrid(currentData.productRows);
			updateProgress(84, "Menampilkan panel risiko", "Menyiapkan kepatuhan dokumen, rekomendasi vendor, dan insight produk.");
			renderVendorRiskAndCompliance(currentData);
			renderProductAndSupplierInsight(currentData);
			updateProgress(93, "Menampilkan panel transaksi", "Menyiapkan panel tagihan, PR, PO, dan BAST.");
			renderInvoiceVendorDashboard(currentData);
			renderPurchaseRequestDashboard(currentData);
			renderPurchaseOrderDashboard(currentData);
			renderBastDashboard(currentData);
			updateProgress(100, "Selesai", "Dashboard berhasil dimuat.");
		} catch (Exception e) {
			debugError("reloadDashboard", e);
			Common.tampilErrorJikaAdmin(e);
			showProgress(100, "Data belum bisa ditampilkan", "Terjadi kendala saat memuat dashboard. Silakan ulangi atau hubungi admin.");
		} finally {
			hideProgress();
		}
	}

	private void clearDashboardResultChildren() {
		try {
			MyPortalchildren column = getMainColumn();
			if (column != null) {
				List<Component> children = new ArrayList<Component>(column.getChildren());
				for (int i = children.size() - 1; i >= 0; i--) {
					Component child = children.get(i);
					if (child == filterPanel) {
						continue;
					}
					child.detach();
				}
				progressPanel = null;
				progressPortal = null;
				return;
			}
			List<Component> children = new ArrayList<Component>(getChildren());
			for (int i = children.size() - 1; i >= 1; i--) {
				children.get(i).detach();
			}
		} catch (Exception e) {
			debugError("clearDashboardResultChildren", e);
		}
	}

	@SuppressWarnings("unchecked")
	private VendorDashboardData loadDashboardData() {
		VendorDashboardData d = new VendorDashboardData();
		Date mulai = getMulai();
		Date sampai = getSampai();
		PenyediaAsset selectedVendor = getSelectedVendor();
		String keyword = getKeyword();
		DashboardSession dashboardSession = openDashboardSession();
		Session session = dashboardSession.session;

		try {

		try {
			Criteria cPo = session.createCriteria(PemesananPengadaanMasterAsset.class)
					.createAlias("penyedia", "penyedia", Criteria.LEFT_JOIN)
					.add(mulai == null ? Restrictions.sqlRestriction("true") : Restrictions.ge("tanggalPembuatan", mulai))
					.add(sampai == null ? Restrictions.sqlRestriction("true") : Restrictions.le("tanggalPembuatan", sampai));
			if (selectedVendor != null) {
				cPo.add(Restrictions.eq("penyedia", selectedVendor));
			}
			if (!keyword.isEmpty()) {
				cPo.add(Restrictions.or(Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE), Restrictions.or(
						Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE),
						Restrictions.ilike("penyedia.nama", keyword, MatchMode.ANYWHERE))));
			}
			cPo.addOrder(Order.desc("tanggalPembuatan"));
			cPo.setMaxResults(SAMPLE_LIMIT);
			List<PemesananPengadaanMasterAsset> pos = cPo.list();
			for (PemesananPengadaanMasterAsset po : pos) {
				processPo(d, po);
			}
		} catch (Exception e) {
			debugError("loadDashboardData.po", e);
		}

		try {
			Criteria cTerima = session.createCriteria(PenerimaanPengadaanMasterAsset.class)
					.createAlias("penyedia", "penyedia", Criteria.LEFT_JOIN)
					.add(mulai == null ? Restrictions.sqlRestriction("true") : Restrictions.ge("tanggalPembuatan", mulai))
					.add(sampai == null ? Restrictions.sqlRestriction("true") : Restrictions.le("tanggalPembuatan", sampai));
			if (selectedVendor != null) {
				cTerima.add(Restrictions.eq("penyedia", selectedVendor));
			}
			if (!keyword.isEmpty()) {
				cTerima.add(Restrictions.or(Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE), Restrictions.or(
						Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE),
						Restrictions.ilike("penyedia.nama", keyword, MatchMode.ANYWHERE))));
			}
			cTerima.addOrder(Order.desc("tanggalPembuatan"));
			cTerima.setMaxResults(SAMPLE_LIMIT);
			List<PenerimaanPengadaanMasterAsset> penerimaans = cTerima.list();
			for (PenerimaanPengadaanMasterAsset p : penerimaans) {
				processPenerimaan(d, p);
			}
		} catch (Exception e) {
			debugError("loadDashboardData.penerimaan", e);
		}

		try {
			Criteria cDetail = session.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
					.createAlias("masterAsset", "masterAsset", Criteria.LEFT_JOIN)
					.add(mulai == null ? Restrictions.sqlRestriction("true") : Restrictions.ge("tanggalPembuatan", mulai))
					.add(sampai == null ? Restrictions.sqlRestriction("true") : Restrictions.le("tanggalPembuatan", sampai));
			if (!keyword.isEmpty()) {
				cDetail.add(Restrictions.or(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE),
						Restrictions.ilike("masterAsset.nama", keyword, MatchMode.ANYWHERE)));
			}
			cDetail.addOrder(Order.desc("tanggalPembuatan"));
			cDetail.setMaxResults(SAMPLE_LIMIT);
			List<PermintaanPengadaanMasterAssetDetail> details = cDetail.list();
			for (PermintaanPengadaanMasterAssetDetail det : details) {
				processPrDetail(d, det, selectedVendor);
			}
		} catch (Exception e) {
			debugError("loadDashboardData.pr-detail", e);
		}

		try {
			Criteria cProduk = session.createCriteria(PengadaanProduk.class)
					.createAlias("supplier", "supplier", Criteria.LEFT_JOIN)
					.createAlias("produk", "produk", Criteria.LEFT_JOIN)
					.add(mulai == null ? Restrictions.sqlRestriction("true") : Restrictions.ge("waktuPengadaan", mulai))
					.add(sampai == null ? Restrictions.sqlRestriction("true") : Restrictions.le("waktuPengadaan", sampai));
			if (selectedVendor != null) {
				cProduk.add(Restrictions.eq("supplier", selectedVendor));
			}
			if (!keyword.isEmpty()) {
				cProduk.add(Restrictions.or(Restrictions.ilike("nomorFaktur", keyword, MatchMode.ANYWHERE), Restrictions.or(
						Restrictions.ilike("namaSupplier", keyword, MatchMode.ANYWHERE),
						Restrictions.ilike("produk.nama", keyword, MatchMode.ANYWHERE))));
			}
			cProduk.addOrder(Order.desc("waktuPengadaan"));
			cProduk.setMaxResults(SAMPLE_LIMIT);
			List<PengadaanProduk> produks = cProduk.list();
			for (PengadaanProduk pp : produks) {
				processPengadaanProduk(d, pp);
			}
		} catch (Exception e) {
			debugError("loadDashboardData.pengadaan-produk", e);
		}

		try {
			Criteria cRetur = session.createCriteria(ReturPengadaanMasterAsset.class)
					.createAlias("penyedia", "penyedia", Criteria.LEFT_JOIN)
					.add(mulai == null ? Restrictions.sqlRestriction("true") : Restrictions.ge("tanggalPembuatan", mulai))
					.add(sampai == null ? Restrictions.sqlRestriction("true") : Restrictions.le("tanggalPembuatan", sampai));
			if (selectedVendor != null) {
				cRetur.add(Restrictions.eq("penyedia", selectedVendor));
			}
			if (!keyword.isEmpty()) {
				cRetur.add(Restrictions.or(Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE), Restrictions.or(
						Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE),
						Restrictions.ilike("penyedia.nama", keyword, MatchMode.ANYWHERE))));
			}
			cRetur.setMaxResults(SAMPLE_LIMIT);
			List<ReturPengadaanMasterAsset> returs = cRetur.list();
			for (ReturPengadaanMasterAsset retur : returs) {
				processRetur(d, retur);
			}
		} catch (Exception e) {
			debugError("loadDashboardData.retur", e);
		}

		try {
			Criteria cSaldo = session.createCriteria(SaldoAwalMasterAsset.class)
					.createAlias("penyedia", "penyedia", Criteria.LEFT_JOIN)
					.add(mulai == null ? Restrictions.sqlRestriction("true") : Restrictions.ge("tanggalPembuatan", mulai))
					.add(sampai == null ? Restrictions.sqlRestriction("true") : Restrictions.le("tanggalPembuatan", sampai));
			if (selectedVendor != null) {
				cSaldo.add(Restrictions.eq("penyedia", selectedVendor));
			}
			if (!keyword.isEmpty()) {
				cSaldo.add(Restrictions.or(Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE), Restrictions.or(
						Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE),
						Restrictions.ilike("penyedia.nama", keyword, MatchMode.ANYWHERE))));
			}
			cSaldo.addOrder(Order.desc("tanggalPembuatan"));
			cSaldo.setMaxResults(SAMPLE_LIMIT);
			List<SaldoAwalMasterAsset> saldos = cSaldo.list();
			for (SaldoAwalMasterAsset saldo : saldos) {
				processSaldoAwal(d, saldo);
			}
		} catch (Exception e) {
			debugError("loadDashboardData.saldo-awal", e);
		}

		try {
			Criteria cSaldoDetail = session.createCriteria(SaldoAwalMasterAssetDetail.class)
					.createAlias("saldoAwal", "saldoAwal", Criteria.LEFT_JOIN)
					.createAlias("saldoAwal.penyedia", "penyedia", Criteria.LEFT_JOIN)
					.createAlias("masterAsset", "masterAsset", Criteria.LEFT_JOIN)
					.add(mulai == null ? Restrictions.sqlRestriction("true") : Restrictions.ge("saldoAwal.tanggalPembuatan", mulai))
					.add(sampai == null ? Restrictions.sqlRestriction("true") : Restrictions.le("saldoAwal.tanggalPembuatan", sampai));
			if (selectedVendor != null) {
				cSaldoDetail.add(Restrictions.eq("saldoAwal.penyedia", selectedVendor));
			}
			if (!keyword.isEmpty()) {
				cSaldoDetail.add(Restrictions.or(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE), Restrictions.or(
						Restrictions.ilike("saldoAwal.kode", keyword, MatchMode.ANYWHERE),
						Restrictions.ilike("masterAsset.nama", keyword, MatchMode.ANYWHERE))));
			}
			cSaldoDetail.addOrder(Order.desc("id"));
			cSaldoDetail.setMaxResults(SAMPLE_LIMIT);
			List<SaldoAwalMasterAssetDetail> saldoDetails = cSaldoDetail.list();
			for (SaldoAwalMasterAssetDetail detail : saldoDetails) {
				processSaldoAwalDetail(d, detail);
			}
		} catch (Exception e) {
			debugError("loadDashboardData.saldo-awal-detail", e);
		}

		try {
			Criteria cPr = session.createCriteria(PermintaanPengadaanMasterAsset.class)
					.createAlias("pemesananPengadaanMasterAsset", "po", Criteria.LEFT_JOIN)
					.createAlias("po.penyedia", "penyedia", Criteria.LEFT_JOIN)
					.add(mulai == null ? Restrictions.sqlRestriction("true") : Restrictions.ge("tanggalPembuatan", mulai))
					.add(sampai == null ? Restrictions.sqlRestriction("true") : Restrictions.le("tanggalPembuatan", sampai));
			if (selectedVendor != null) {
				cPr.add(Restrictions.eq("po.penyedia", selectedVendor));
			}
			if (!keyword.isEmpty()) {
				cPr.add(Restrictions.or(Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE),
						Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE)));
			}
			cPr.addOrder(Order.desc("tanggalPembuatan"));
			cPr.setMaxResults(SAMPLE_LIMIT);
			List<PermintaanPengadaanMasterAsset> prs = cPr.list();
			for (PermintaanPengadaanMasterAsset pr : prs) {
				processPrMaster(d, pr);
			}
		} catch (Exception e) {
			debugError("loadDashboardData.pr-master", e);
		}

		try {
			Criteria cPoDetail = session.createCriteria(PemesananPengadaanMasterAssetDetail.class)
					.createAlias("pemesananPengadaanMasterAsset", "po", Criteria.LEFT_JOIN)
					.createAlias("po.penyedia", "penyedia", Criteria.LEFT_JOIN)
					.createAlias("masterAsset", "masterAsset", Criteria.LEFT_JOIN)
					.add(mulai == null ? Restrictions.sqlRestriction("true") : Restrictions.ge("po.tanggalPembuatan", mulai))
					.add(sampai == null ? Restrictions.sqlRestriction("true") : Restrictions.le("po.tanggalPembuatan", sampai));
			if (selectedVendor != null) {
				cPoDetail.add(Restrictions.eq("po.penyedia", selectedVendor));
			}
			if (!keyword.isEmpty()) {
				cPoDetail.add(Restrictions.or(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE), Restrictions.or(
						Restrictions.ilike("po.kode", keyword, MatchMode.ANYWHERE),
						Restrictions.ilike("masterAsset.nama", keyword, MatchMode.ANYWHERE))));
			}
			cPoDetail.addOrder(Order.desc("id"));
			cPoDetail.setMaxResults(SAMPLE_LIMIT);
			List<PemesananPengadaanMasterAssetDetail> poDetails = cPoDetail.list();
			for (PemesananPengadaanMasterAssetDetail detail : poDetails) {
				processPoDetail(d, detail);
			}
		} catch (Exception e) {
			debugError("loadDashboardData.po-detail", e);
		}

		try {
			Criteria cBastDetail = session.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
					.createAlias("penerimaanPengadaanMasterAsset", "bast", Criteria.LEFT_JOIN)
					.createAlias("bast.penyedia", "penyedia", Criteria.LEFT_JOIN)
					.createAlias("masterAsset", "masterAsset", Criteria.LEFT_JOIN)
					.add(mulai == null ? Restrictions.sqlRestriction("true") : Restrictions.ge("bast.tanggalPembuatan", mulai))
					.add(sampai == null ? Restrictions.sqlRestriction("true") : Restrictions.le("bast.tanggalPembuatan", sampai));
			if (selectedVendor != null) {
				cBastDetail.add(Restrictions.eq("bast.penyedia", selectedVendor));
			}
			if (!keyword.isEmpty()) {
				cBastDetail.add(Restrictions.or(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE), Restrictions.or(
						Restrictions.ilike("bast.kode", keyword, MatchMode.ANYWHERE),
						Restrictions.ilike("masterAsset.nama", keyword, MatchMode.ANYWHERE))));
			}
			cBastDetail.addOrder(Order.desc("id"));
			cBastDetail.setMaxResults(SAMPLE_LIMIT);
			List<PenerimaanPengadaanMasterAssetDetail> bastDetails = cBastDetail.list();
			for (PenerimaanPengadaanMasterAssetDetail detail : bastDetails) {
				processBastDetail(d, detail);
			}
		} catch (Exception e) {
			debugError("loadDashboardData.bast-detail", e);
		}

		try {
			loadDocumentCompliance(d, session, selectedVendor);
		} catch (Exception e) {
			debugError("loadDashboardData.document-compliance", e);
		}

		finalizeData(d);
		return d;
		} finally {
			closeDashboardSession(dashboardSession);
		}
	}

	@SuppressWarnings("unchecked")
	private void loadDocumentCompliance(VendorDashboardData d, Session session, PenyediaAsset selectedVendor) {
		Criteria c = session.createCriteria(PenyediaAssetPunyaDokumen.class)
				.createAlias("penyediaAsset", "penyediaAsset", Criteria.LEFT_JOIN);
		if (selectedVendor != null) {
			c.add(Restrictions.eq("penyediaAsset", selectedVendor));
		}
		List<PenyediaAssetPunyaDokumen> docs = c.list();
		for (PenyediaAssetPunyaDokumen doc : docs) {
			PenyediaAsset vendor = null;
			try {
				vendor = doc.getPenyediaAsset();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:631");
			}
			VendorAnalysisRow row = getVendorRow(d, vendor, null);
			row.totalDokumen++;
			String status = safe(doc.getStatus());
			if (PenyediaAssetPunyaDokumen.VERIFIKASI.equals(status)) {
				row.dokumenTerverifikasi++;
			} else if (PenyediaAssetPunyaDokumen.REVISI.equals(status)) {
				row.dokumenRevisi++;
			} else {
				row.dokumenBelum++;
			}
		}
	}

	private void processPo(VendorDashboardData d, PemesananPengadaanMasterAsset po) {
		if (po == null) {
			return;
		}
		PenyediaAsset vendor = null;
		try {
			vendor = po.getPenyedia();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:653");
		}
		VendorAnalysisRow row = getVendorRow(d, vendor, po.getKode());
		row.poCount++;
		row.nominalTransaksi += safeDouble(po.getNilai());
		row.totalBiayaDibayar += safeDouble(po.getDibayar());
		updateDateRange(row, po.getTanggalPembuatan());
		if (Boolean.TRUE.equals(po.getPembelianLangsung())) {
			row.pembelianLangsungCount++;
		}
		d.poRows.add(po);
	}

	private void processPenerimaan(VendorDashboardData d, PenerimaanPengadaanMasterAsset penerimaan) {
		if (penerimaan == null) {
			return;
		}
		PenyediaAsset vendor = null;
		try {
			vendor = penerimaan.getPenyedia();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:673");
		}
		VendorAnalysisRow row = getVendorRow(d, vendor, penerimaan.getKode());
		row.bastCount++;
		updateDateRange(row, penerimaan.getTanggalPembuatan());
		d.penerimaanRows.add(penerimaan);

		Double kualitas = readScoreByMethods(penerimaan, QUALITY_METHODS);
		if (kualitas != null) {
			row.qualityScores.add(kualitas);
		}
		Double layanan = readScoreByMethods(penerimaan, SERVICE_METHODS);
		if (layanan != null) {
			row.serviceScores.add(layanan);
		}
		Double ketepatan = readScoreByMethods(penerimaan, TIME_METHODS);
		if (ketepatan != null) {
			row.timeScores.add(ketepatan);
		} else {
			row.timeScores.add(calculateDeliveryScore(penerimaan));
		}
	}

	private void processPrDetail(VendorDashboardData d, PermintaanPengadaanMasterAssetDetail detail, PenyediaAsset selectedVendor) {
		if (detail == null) {
			return;
		}
		PenyediaAsset vendor = getVendorFromPrDetail(detail);
		if (selectedVendor != null && (vendor == null || selectedVendor.getId() == null || !selectedVendor.getId().equals(vendor.getId()))) {
			return;
		}

		String productName = getMasterAssetName(detail.getMasterAsset());
		ProductAnalysisRow pr = getProductRow(d, vendor, productName);
		pr.jumlahTransaksi++;
		pr.qtyDipesan += safeDouble(detail.getJumlah());
		pr.qtyDatang += safeDouble(detail.getJumlahDatang());
		pr.qtyTransaksi += safeDouble(detail.getJumlah());
		pr.nominalTransaksi += safeDouble(detail.getHargaTotal());
		updateDateRange(pr, detail.getTanggalPembuatan());
		d.prDetailRows.add(detail);

		VendorAnalysisRow vr = getVendorRow(d, vendor, productName);
		vr.qtyDipesan += safeDouble(detail.getJumlah());
		vr.qtyDatang += safeDouble(detail.getJumlahDatang());
		vr.detailCount++;
		vr.productNames.put(productName, productName);
		updateDateRange(vr, detail.getTanggalPembuatan());
	}

	private void processPengadaanProduk(VendorDashboardData d, PengadaanProduk pp) {
		if (pp == null) {
			return;
		}
		PenyediaAsset vendor = null;
		try {
			vendor = pp.getSupplier();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:730");
		}
		String productName = getEntityName(pp.getProduk(), safe(pp.getNamaSupplier()));
		if (productName.length() == 0) {
			productName = "Produk Koperasi / Inventory";
		}
		ProductAnalysisRow pr = getProductRow(d, vendor, productName);
		pr.jumlahTransaksi++;
		pr.qtyTransaksi += safeDouble(pp.getQty());
		pr.qtyDipesan += safeDouble(pp.getQty());
		pr.qtyDatang += safeDouble(pp.getQty());
		pr.nominalTransaksi += safeDouble(pp.getTotalHarga());
		updateDateRange(pr, pp.getWaktuPengadaan());

		VendorAnalysisRow vr = getVendorRow(d, vendor, pp.getNamaSupplier());
		vr.produkKoperasiCount++;
		vr.nominalTransaksi += safeDouble(pp.getTotalHarga());
		vr.qtyDipesan += safeDouble(pp.getQty());
		vr.qtyDatang += safeDouble(pp.getQty());
		vr.productNames.put(productName, productName);
		updateDateRange(vr, pp.getWaktuPengadaan());
		d.pengadaanProdukRows.add(pp);
	}

	private void processRetur(VendorDashboardData d, ReturPengadaanMasterAsset retur) {
		if (retur == null) {
			return;
		}
		PenyediaAsset vendor = null;
		try {
			vendor = retur.getPenyedia();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:761");
		}
		VendorAnalysisRow row = getVendorRow(d, vendor, retur.getKode());
		row.returCount++;
		updateDateRange(row, retur.getTanggalPembuatan());
		d.returRows.add(retur);
	}

	private void processSaldoAwal(VendorDashboardData d, SaldoAwalMasterAsset saldo) {
		if (saldo == null) {
			return;
		}
		d.saldoRows.add(saldo);
		PenyediaAsset vendor = null;
		try {
			vendor = saldo.getPenyedia();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:777");
		}
		VendorAnalysisRow row = getVendorRow(d, vendor, saldo.getKode());
		row.tagihanCount++;
		row.nilaiTagihan += safeDouble(saldo.getNilai());
		row.nilaiTagihanDibayar += safeDouble(saldo.getDibayar());
		updateDateRange(row, saldo.getTanggalPembuatan());
	}

	private void processSaldoAwalDetail(VendorDashboardData d, SaldoAwalMasterAssetDetail detail) {
		if (detail == null) {
			return;
		}
		d.saldoDetailRows.add(detail);
		SaldoAwalMasterAsset saldo = null;
		try {
			saldo = detail.getSaldoAwal();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:794");
		}
		PenyediaAsset vendor = null;
		try {
			vendor = saldo == null ? null : saldo.getPenyedia();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:799");
		}
		String productName = getMasterAssetName(detail.getMasterAsset());
		ProductAnalysisRow pr = getProductRow(d, vendor, productName);
		pr.tagihanCount++;
		pr.qtyTagihan += safeDouble(detail.getJumlah());
		pr.nominalTagihan += safeDouble(detail.getHargaTotal());
		updateDateRange(pr, saldo == null ? null : saldo.getTanggalPembuatan());
		VendorAnalysisRow vr = getVendorRow(d, vendor, productName);
		vr.tagihanDetailCount++;
		vr.productNames.put(productName, productName);
	}

	private void processPrMaster(VendorDashboardData d, PermintaanPengadaanMasterAsset pr) {
		if (pr == null) {
			return;
		}
		d.prRows.add(pr);
		PenyediaAsset vendor = null;
		try {
			PemesananPengadaanMasterAsset po = pr.getPemesananPengadaanMasterAsset();
			vendor = po == null ? null : po.getPenyedia();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:821");
		}
		VendorAnalysisRow row = getVendorRow(d, vendor, pr.getKode());
		row.prCount++;
		row.nilaiPr += safeDouble(pr.getNilai());
		updateDateRange(row, pr.getTanggalPembuatan());
	}

	private void processPoDetail(VendorDashboardData d, PemesananPengadaanMasterAssetDetail detail) {
		if (detail == null) {
			return;
		}
		d.poDetailRows.add(detail);
		PemesananPengadaanMasterAsset po = null;
		try {
			po = detail.getPemesananPengadaanMasterAsset();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:837");
		}
		PenyediaAsset vendor = null;
		try {
			vendor = po == null ? null : po.getPenyedia();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:842");
		}
		String productName = getMasterAssetName(detail.getMasterAsset());
		ProductAnalysisRow pr = getProductRow(d, vendor, productName);
		pr.poCount++;
		pr.qtyPo += safeDouble(detail.getJumlah());
		pr.nominalPo += safeDouble(detail.getHargaTotal());
		updateDateRange(pr, po == null ? null : po.getTanggalPembuatan());
		VendorAnalysisRow vr = getVendorRow(d, vendor, productName);
		vr.poDetailCount++;
		vr.qtyPo += safeDouble(detail.getJumlah());
		vr.nilaiPoDetail += safeDouble(detail.getHargaTotal());
		vr.productNames.put(productName, productName);
	}

	private void processBastDetail(VendorDashboardData d, PenerimaanPengadaanMasterAssetDetail detail) {
		if (detail == null) {
			return;
		}
		d.bastDetailRows.add(detail);
		PenerimaanPengadaanMasterAsset bast = null;
		try {
			bast = detail.getPenerimaanPengadaanMasterAsset();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:865");
		}
		PenyediaAsset vendor = null;
		try {
			vendor = bast == null ? null : bast.getPenyedia();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:870");
		}
		String productName = getMasterAssetName(detail.getMasterAsset());
		ProductAnalysisRow pr = getProductRow(d, vendor, productName);
		pr.bastCount++;
		pr.qtyBast += safeDouble(detail.getDiterima());
		pr.nominalBast += safeDouble(detail.getHargaTotal());
		updateDateRange(pr, bast == null ? null : bast.getTanggalPembuatan());
		VendorAnalysisRow vr = getVendorRow(d, vendor, productName);
		vr.bastDetailCount++;
		vr.qtyBast += safeDouble(detail.getDiterima());
		vr.nilaiBastDetail += safeDouble(detail.getHargaTotal());
		vr.productNames.put(productName, productName);
	}

	private void finalizeData(VendorDashboardData d) {
		d.vendorRows.clear();
		d.vendorRows.addAll(d.vendorMap.values());
		for (VendorAnalysisRow row : d.vendorRows) {
			row.quantityScore = calculateQuantityScore(row.qtyDipesan, row.qtyDatang);
			row.qualityScore = row.qualityScores.isEmpty() ? calculateFallbackQualityScore(row) : average(row.qualityScores);
			row.timeScore = row.timeScores.isEmpty() ? 100.0 : average(row.timeScores);
			row.serviceScore = row.serviceScores.isEmpty() ? 100.0 : average(row.serviceScores);
			row.finalScore = average(row.quantityScore, row.qualityScore, row.timeScore, row.serviceScore);
			row.evaluation = buildEvaluation(row.finalScore);
			d.totalNominal += row.nominalTransaksi;
			d.totalDibayar += row.totalBiayaDibayar;
			d.totalPo += row.poCount;
			d.totalBast += row.bastCount;
			d.totalRetur += row.returCount;
			if (row.finalScore < 75.0) {
				d.vendorNeedAttention++;
			}
			if (row.totalDokumen > 0 && row.dokumenTerverifikasi < row.totalDokumen) {
				d.vendorDokumenBelumLengkap++;
			}
		}
		Collections.sort(d.vendorRows, new Comparator<VendorAnalysisRow>() {
			@Override
			public int compare(VendorAnalysisRow a, VendorAnalysisRow b) {
				if (b.nominalTransaksi > a.nominalTransaksi) return 1;
				if (b.nominalTransaksi < a.nominalTransaksi) return -1;
				return a.namaVendor.compareToIgnoreCase(b.namaVendor);
			}
		});

		d.productRows.clear();
		d.productRows.addAll(d.productMap.values());
		Collections.sort(d.productRows, new Comparator<ProductAnalysisRow>() {
			@Override
			public int compare(ProductAnalysisRow a, ProductAnalysisRow b) {
				if (b.nominalTransaksi > a.nominalTransaksi) return 1;
				if (b.nominalTransaksi < a.nominalTransaksi) return -1;
				return a.namaProduk.compareToIgnoreCase(b.namaProduk);
			}
		});
		d.totalProduk = d.productRows.size();
		for (ProductAnalysisRow p : d.productRows) {
			d.totalQtyDipesan += Math.max(p.qtyDipesan, p.qtyPo);
			d.totalQtyDatang += Math.max(p.qtyDatang, p.qtyBast);
		}
	}

	private void renderOverview(final VendorDashboardData d) {
		MyPortalchildren pc = getMainColumn();
		Panelchildren pch = createModernPanel("Analisa Vendor & Produk", pc);
		appendDownloadExcelButton(pch, "Download Excel Ringkasan", "ringkasan_dashboard_vendor", buildOverviewSummaryRows(d), arrayRenderer(), overviewSummaryHeaders());
		appendDownloadExcelLengkapButton(pch, "Download Excel Lengkap", "dashboard_analisis_vendor_lengkap", d);

		appendHtml(pch, "<div style='position:relative; overflow:hidden; border-radius:18px; padding:22px 24px; color:#ffffff;"
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); box-shadow:0 14px 30px rgba(15,23,42,.18);'>"
				+ "<div style='position:absolute; right:-60px; top:-70px; width:210px; height:210px; border-radius:999px; background:rgba(255,255,255,.12);'></div>"
				+ "<div style='position:relative; z-index:1;'>"
				+ "<div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;'>Vendor Performance Control Center</div>"
				+ "<div style='font-size:28px; line-height:1.15; font-weight:800; margin-top:6px;'>Analisa Vendor, BAST, PO & Produk</div>"
				+ "<div style='font-size:13px; opacity:.90; margin-top:8px; max-width:850px;'>Memantau nominal transaksi, kuantitas dipesan vs diterima, kualitas BAST, ketepatan waktu pengiriman, layanan, retur, dokumen vendor, dan produk yang dibeli.</div>"
				+ "</div></div>");

		Div cards = new Div();
		cards.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px; max-width:100%; overflow:visible; box-sizing:border-box;");
		cards.setParent(pch);

		appendMetricCard(cards, "Vendor Bertransaksi", d.vendorRows.size(), "Jumlah vendor pada filter", "#dbeafe", "#1e40af", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Vendor Bertransaksi", d.vendorRows, vendorDetailRenderer(), vendorHeaders(), vendorWidths());
			}
		});
		appendMetricCard(cards, "Nominal Transaksi", formatCurrencyShort(d.totalNominal), "Total nilai PO + pengadaan produk", "#dcfce7", "#166534", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Analisa Nominal per Vendor", d.vendorRows, vendorDetailRenderer(), vendorHeaders(), vendorWidths());
			}
		});
		appendMetricCard(cards, "Produk Dianalisis", d.totalProduk, "Barang/jasa dari PR dan inventory", "#ede9fe", "#5b21b6", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Analisa Produk", d.productRows, productDetailRenderer(), productHeaders(), productWidths());
			}
		});
		appendMetricCard(cards, "BAST/Penerimaan", d.totalBast, "Transaksi penerimaan barang/jasa", "#cffafe", "#155e75", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail BAST/Penerimaan", d.penerimaanRows, penerimaanRenderer(), penerimaanHeaders(), penerimaanWidths());
			}
		});
		appendMetricCard(cards, "Vendor Perlu Perhatian", d.vendorNeedAttention, "Skor evaluasi di bawah 75", "#fee2e2", "#991b1b", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Vendor Perlu Perhatian", filterVendorScore(d.vendorRows, 75.0), vendorDetailRenderer(), vendorHeaders(), vendorWidths());
			}
		});
		appendMetricCard(cards, "Dokumen Belum Lengkap", d.vendorDokumenBelumLengkap, "Dokumen vendor belum seluruhnya terverifikasi", "#fef3c7", "#92400e", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Vendor dengan Dokumen Belum Lengkap", filterVendorDocumentIssue(d.vendorRows), vendorDetailRenderer(), vendorHeaders(), vendorWidths());
			}
		});
	}

	private void renderExecutiveVisualAnalytics(final VendorDashboardData d) {
		MyPortalchildren pc = getMainColumn();
		Panelchildren pch = createModernPanel("Grafik Ringkas, Tren, dan Spider Web", pc);
		appendDownloadExcelButton(pch, "Download Excel Spider Web", "spider_web_kinerja_vendor", buildRadarExportRows(d), arrayRenderer(), radarExportHeaders());
		appendDownloadExcelButton(pch, "Download Excel Tren", "tren_pengadaan_vendor", buildTrendExportRows(d), arrayRenderer(), trendExportHeaders());
		appendDownloadExcelButton(pch, "Download Excel Top Vendor", "top_vendor_nominal", buildTopVendorExportRows(d), arrayRenderer(), topVendorExportHeaders());
		String html = "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(280px,1fr)); gap:12px;'>"
				+ buildVendorRadarHtml(d)
				+ buildVendorTrendHtml(d)
				+ buildTopVendorBarHtml(d)
				+ "</div>";
		appendHtml(pch, html);
	}

	private String buildVendorRadarHtml(VendorDashboardData d) {
		double kualitas = averageVendorScore(d, "quality");
		double waktu = averageVendorScore(d, "time");
		double layanan = averageVendorScore(d, "service");
		double kuantitas = averageVendorScore(d, "quantity");
		String points = radarPoint(110, 110, kualitas, -90) + " " + radarPoint(110, 110, waktu, 0) + " "
				+ radarPoint(110, 110, layanan, 90) + " " + radarPoint(110, 110, kuantitas, 180);
		return "<div style='border:1px solid #e2e8f0; border-radius:16px; padding:14px; background:#ffffff;'>"
				+ "<div style='font-size:13px; font-weight:900; color:#0f172a;'>Spider Web Kinerja Vendor</div>"
				+ "<div style='font-size:11px; color:#64748b; margin-top:4px;'>Semakin penuh bentuknya, semakin baik keseimbangan kualitas, waktu, layanan, dan kuantitas.</div>"
				+ "<svg width='100%' height='240' viewBox='0 0 220 220' style='margin-top:8px;'>"
				+ "<polygon points='110,20 200,110 110,200 20,110' fill='#f8fafc' stroke='#cbd5e1' stroke-width='1'/>"
				+ "<polygon points='110,50 170,110 110,170 50,110' fill='none' stroke='#e2e8f0' stroke-width='1'/>"
				+ "<line x1='110' y1='20' x2='110' y2='200' stroke='#e2e8f0'/><line x1='20' y1='110' x2='200' y2='110' stroke='#e2e8f0'/>"
				+ "<polygon points='" + points + "' fill='rgba(37,99,235,.28)' stroke='#2563eb' stroke-width='3'/>"
				+ "<text x='110' y='14' text-anchor='middle' font-size='10' fill='#334155'>Kualitas " + formatPercent(kualitas) + "</text>"
				+ "<text x='206' y='114' text-anchor='end' font-size='10' fill='#334155'>Waktu " + formatPercent(waktu) + "</text>"
				+ "<text x='110' y='214' text-anchor='middle' font-size='10' fill='#334155'>Layanan " + formatPercent(layanan) + "</text>"
				+ "<text x='14' y='114' text-anchor='start' font-size='10' fill='#334155'>Qty " + formatPercent(kuantitas) + "</text>"
				+ "</svg></div>";
	}

	private String buildVendorTrendHtml(VendorDashboardData d) {
		Map<String, TrendBucket> trends = buildMonthlyTrendBuckets(d);
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='border:1px solid #e2e8f0; border-radius:16px; padding:14px; background:#ffffff;'>");
		sb.append("<div style='font-size:13px; font-weight:900; color:#0f172a;'>Tren Aktivitas Pengadaan</div>");
		sb.append("<div style='font-size:11px; color:#64748b; margin-top:4px;'>Memperlihatkan bulan dengan transaksi vendor paling aktif.</div>");
		int max = 1;
		for (TrendBucket b : trends.values()) {
			if (b.count > max) max = b.count;
		}
		if (trends.isEmpty()) {
			sb.append("<div style='margin-top:12px; color:#94a3b8; font-size:12px;'>Belum ada tren pada filter ini.</div>");
		} else {
			sb.append("<div style='margin-top:12px;'>");
			for (String key : trends.keySet()) {
				TrendBucket b = trends.get(key);
				int pct = Math.max(4, (int) Math.round(b.count * 100.0 / max));
				sb.append("<div style='display:flex; align-items:center; gap:8px; margin:8px 0;'>")
						.append("<div style='width:58px; font-size:11px; font-weight:800; color:#334155;'>").append(escapeHtml(key)).append("</div>")
						.append("<div style='flex:1; height:12px; border-radius:999px; background:#e2e8f0; overflow:hidden;'><div style='height:12px; width:").append(pct).append("%; background:#0891b2; border-radius:999px;'></div></div>")
						.append("<div style='width:42px; text-align:right; font-size:11px; font-weight:900; color:#0f172a;'>").append(b.count).append("</div></div>");
			}
			sb.append("</div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String buildTopVendorBarHtml(VendorDashboardData d) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='border:1px solid #e2e8f0; border-radius:16px; padding:14px; background:#ffffff;'>");
		sb.append("<div style='font-size:13px; font-weight:900; color:#0f172a;'>Top Vendor Berdasarkan Nominal</div>");
		sb.append("<div style='font-size:11px; color:#64748b; margin-top:4px;'>Membantu melihat vendor dengan nilai transaksi terbesar.</div>");
		if (d == null || d.vendorRows == null || d.vendorRows.isEmpty()) {
			sb.append("<div style='margin-top:12px; color:#94a3b8; font-size:12px;'>Belum ada vendor pada filter ini.</div>");
		} else {
			double max = Math.max(1.0, d.vendorRows.get(0).nominalTransaksi);
			int limit = Math.min(6, d.vendorRows.size());
			for (int i = 0; i < limit; i++) {
				VendorAnalysisRow v = d.vendorRows.get(i);
				int pct = Math.max(4, (int) Math.round(v.nominalTransaksi * 100.0 / max));
				sb.append("<div style='margin-top:10px;'>")
						.append("<div style='display:flex; justify-content:space-between; gap:8px; font-size:11px; color:#334155;'>")
						.append("<b>").append(escapeHtml(v.namaVendor)).append("</b><span>").append(formatCurrencyShort(v.nominalTransaksi)).append("</span></div>")
						.append("<div style='height:10px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:5px;'>")
						.append("<div style='height:10px; width:").append(pct).append("%; background:#2563eb; border-radius:999px;'></div></div></div>");
			}
		}
		sb.append("</div>");
		return sb.toString();
	}

	private void renderVendorAnalysisGrid(final List<VendorAnalysisRow> rowsData) {
		MyPortalchildren pc = getMainColumn();
		Panelchildren pch = createModernPanel("Analisa Vendor", pc);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:10px;'>Kolom mengikuti kebutuhan evaluasi vendor: periode transaksi, nominal, kuantitas PO vs BAST, biaya dibayar, kualitas, waktu pengiriman, layanan, dan hasil evaluasi.</div>");
		appendDownloadExcelButton(pch, "Download Excel Analisa Vendor", "analisa_vendor", rowsData, vendorDetailRenderer(), vendorHeaders());
		renderPagedGrid(pch, rowsData, vendorDetailRenderer(), vendorHeaders(), vendorWidths(), PAGE_SIZE);
	}

	private void renderProductAnalysisGrid(final List<ProductAnalysisRow> rowsData) {
		MyPortalchildren pc = getMainColumn();
		Panelchildren pch = createModernPanel("Analisa Produk", pc);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:10px;'>Analisis produk menggabungkan detail PR/PO asset dan pengadaan produk inventory/koperasi jika tersedia.</div>");
		appendDownloadExcelButton(pch, "Download Excel Analisa Produk", "analisa_produk", rowsData, productDetailRenderer(), productHeaders());
		renderPagedGrid(pch, rowsData, productDetailRenderer(), productHeaders(), productWidths(), PAGE_SIZE);
	}

	private void renderVendorRiskAndCompliance(final VendorDashboardData d) {
		MyPortalchildren pcLeft = getMainColumn();
		Panelchildren pchLeft = createModernPanel("Kinerja & Risiko Vendor", pcLeft);
		appendDownloadExcelButton(pchLeft, "Download Excel Ringkasan Risiko", "ringkasan_risiko_vendor", buildRiskExportRows(d), arrayRenderer(), riskExportHeaders());
		appendDownloadExcelButton(pchLeft, "Download Excel Detail Risiko", "detail_risiko_vendor", d.vendorRows, vendorDetailRenderer(), vendorHeaders());

		int buruk = 0;
		int cukup = 0;
		int baik = 0;
		for (VendorAnalysisRow row : d.vendorRows) {
			if (row.finalScore < 50.0) buruk++;
			else if (row.finalScore < 75.0) cukup++;
			else baik++;
		}
		appendHtml(pchLeft, buildProgressRow("Baik / Memuaskan", baik, Math.max(1, d.vendorRows.size()), "#16a34a")
				+ buildProgressRow("Sedang / Cukup", cukup, Math.max(1, d.vendorRows.size()), "#d97706")
				+ buildProgressRow("Kurang / Buruk", buruk, Math.max(1, d.vendorRows.size()), "#dc2626")
				+ "<div style='margin-top:12px; padding:10px; border-radius:12px; background:#f8fafc; color:#64748b; border:1px solid #e2e8f0; font-size:11px;'>"
				+ "Evaluasi mengikuti kategori: &lt;50 kurang/buruk, 50-74 sedang/cukup, dan ≥75 baik/memuaskan.</div>");

		MyPortalchildren pcRight = getMainColumn();
		Panelchildren pchRight = createModernPanel("Kepatuhan Dokumen Vendor", pcRight);
		appendDownloadExcelButton(pchRight, "Download Excel Kepatuhan Dokumen", "kepatuhan_dokumen_vendor", buildComplianceExportRows(d), arrayRenderer(), complianceExportHeaders());
		renderDocumentSummary(pchRight, d.vendorRows);
	}

	private void renderProductAndSupplierInsight(final VendorDashboardData d) {
		MyPortalchildren pc = getMainColumn();
		Panelchildren pch = createModernPanel("Insight Produk & Rekomendasi Vendor", pc);
		appendDownloadExcelButton(pch, "Download Excel Ringkasan Insight", "ringkasan_insight_produk_vendor", buildInsightExportRows(d), arrayRenderer(), insightExportHeaders());
		appendDownloadExcelButton(pch, "Download Excel Detail Produk", "detail_insight_produk", d.productRows, productDetailRenderer(), productHeaders());
		Div wrap = new Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap; max-width:100%; overflow:visible; box-sizing:border-box;");
		wrap.setParent(pch);

		appendInsightCard(wrap, "Kuantitas Dipesan", formatNumber(d.totalQtyDipesan), "Total quantity dari detail PR/PO dan pengadaan produk.", "#dbeafe", "#1e40af", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Produk - Kuantitas Dipesan", d.productRows, productDetailRenderer(), productHeaders(), productWidths());
			}
		});
		appendInsightCard(wrap, "Kuantitas Datang", formatNumber(d.totalQtyDatang), "Total quantity diterima/datang berdasarkan BAST/detail.", "#dcfce7", "#166534", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail BAST - Kuantitas Datang", d.bastDetailRows, bastDetailRenderer(), bastDetailHeaders(), bastDetailWidths());
			}
		});
		appendInsightCard(wrap, "Retur", String.valueOf(d.totalRetur), "Sinyal risiko kualitas barang/jasa.", "#fee2e2", "#991b1b", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Retur Vendor", d.returRows, returRenderer(), returHeaders(), returWidths());
			}
		});
		appendInsightCard(wrap, "Biaya Dibayar", formatCurrencyShort(d.totalDibayar), "Akumulasi pembayaran yang terbaca pada PO.", "#fef3c7", "#92400e", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail PO - Biaya Dibayar", d.poRows, poRenderer(), poHeaders(), poWidths());
			}
		});

		appendHtml(pch, "<div style='margin-top:14px; display:grid; grid-template-columns:repeat(auto-fit,minmax(240px,1fr)); gap:12px;'>"
				+ buildActionPlanCard("1", "Vendor Prioritas", getTopVendorText(d), "#dbeafe", "#1e40af")
				+ buildActionPlanCard("2", "Kontrol Kualitas", d.totalRetur > 0 ? "Ada " + d.totalRetur + " transaksi retur. Review vendor dan spesifikasi barang." : "Belum ada retur pada periode filter.", "#fee2e2", "#991b1b")
				+ buildActionPlanCard("3", "Dokumen Vendor", d.vendorDokumenBelumLengkap > 0 ? d.vendorDokumenBelumLengkap + " vendor perlu melengkapi/verifikasi dokumen." : "Dokumen vendor relatif aman pada data yang terbaca.", "#fef3c7", "#92400e")
				+ buildActionPlanCard("4", "Produk Strategis", getTopProductText(d), "#ede9fe", "#5b21b6")
				+ "</div>");
	}

	private void renderInvoiceVendorDashboard(final VendorDashboardData d) {
		MyPortalchildren pc = getMainColumn();
		Panelchildren pch = createModernPanel("Dasbor Terima Tagihan Vendor", pc);
		appendDownloadExcelButton(pch, "Download Excel Ringkasan Tagihan", "ringkasan_tagihan_vendor", buildInvoiceSummaryRows(d), arrayRenderer(), simpleMetricHeaders());
		appendDownloadExcelButton(pch, "Download Excel Tagihan Vendor", "tagihan_vendor", d.saldoRows, saldoRenderer(), saldoHeaders());
		appendDownloadExcelButton(pch, "Download Excel Detail Tagihan", "detail_tagihan_vendor", d.saldoDetailRows, saldoDetailRenderer(), saldoDetailHeaders());
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:10px;'>Analisis tagihan vendor dari master <b>SaldoAwalMasterAsset</b> dan detail <b>SaldoAwalMasterAssetDetail</b>. Semua angka dapat diklik untuk membuka detail data.</div>");

		Div wrap = new Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap; max-width:100%; overflow:visible; box-sizing:border-box;");
		wrap.setParent(pch);
		final double totalTagihan = sumSaldoNilai(d.saldoRows);
		final double totalDibayar = sumSaldoDibayar(d.saldoRows);
		final List<SaldoAwalMasterAsset> belumLunas = filterSaldoBelumLunas(d.saldoRows);
		appendMetricCard(wrap, "Tagihan Vendor", d.saldoRows.size(), "Jumlah master tagihan", "#dbeafe", "#1e40af", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Master Tagihan Vendor", d.saldoRows, saldoRenderer(), saldoHeaders(), saldoWidths());
			}
		});
		appendMetricCard(wrap, "Detail Tagihan", d.saldoDetailRows.size(), "Jumlah item/barang tagihan", "#ede9fe", "#5b21b6", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Item Tagihan Vendor", d.saldoDetailRows, saldoDetailRenderer(), saldoDetailHeaders(), saldoDetailWidths());
			}
		});
		appendMetricCard(wrap, "Nominal Tagihan", formatCurrencyShort(totalTagihan), "Total nilai tagihan", "#dcfce7", "#166534", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Nominal Tagihan Vendor", d.saldoRows, saldoRenderer(), saldoHeaders(), saldoWidths());
			}
		});
		appendMetricCard(wrap, "Sudah Dibayar", formatCurrencyShort(totalDibayar), "Total yang sudah dibayar", "#cffafe", "#155e75", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Pembayaran Tagihan Vendor", d.saldoRows, saldoRenderer(), saldoHeaders(), saldoWidths());
			}
		});
		appendMetricCard(wrap, "Belum Lunas", belumLunas.size(), "Tagihan perlu follow up", "#fee2e2", "#991b1b", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Tagihan Vendor Belum Lunas", belumLunas, saldoRenderer(), saldoHeaders(), saldoWidths());
			}
		});
	}

	private void renderPurchaseRequestDashboard(final VendorDashboardData d) {
		String pcWidth = Common.isMobile() ? "100%" : "50%";
		MyPortalchildren pc = new MyPortalchildren();
		pc.setWidth(pcWidth);
		pc.setStyle("padding:6px; box-sizing:border-box;");
		pc.setParent(this);
		Panelchildren pch = createModernPanel("Dasbor Purchase Request (PR)", pc);
		appendDownloadExcelButton(pch, "Download Excel Ringkasan PR", "ringkasan_purchase_request", buildPrSummaryRows(d), arrayRenderer(), simpleMetricHeaders());
		appendDownloadExcelButton(pch, "Download Excel PR", "purchase_request", d.prRows, prRenderer(), prHeaders());
		appendDownloadExcelButton(pch, "Download Excel Detail PR", "detail_purchase_request", d.prDetailRows, prDetailRenderer(), prDetailHeaders());
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:10px;'>Analisis PR dari <b>PermintaanPengadaanMasterAsset</b> dan <b>PermintaanPengadaanMasterAssetDetail</b>.</div>");
		Div wrap = new Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap; max-width:100%; overflow:visible; box-sizing:border-box;");
		wrap.setParent(pch);
		final List<PermintaanPengadaanMasterAsset> approved = filterPrApproved(d.prRows);
		final List<PermintaanPengadaanMasterAsset> rejected = filterPrRejected(d.prRows);
		final List<PermintaanPengadaanMasterAssetDetail> unrealized = filterPrDetailUnrealized(d.prDetailRows);
		appendMetricCard(wrap, "Total PR", d.prRows.size(), "Master PR", "#dbeafe", "#1e40af", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Purchase Request", d.prRows, prRenderer(), prHeaders(), prWidths());
			}
		});
		appendMetricCard(wrap, "Detail PR", d.prDetailRows.size(), "Item PR", "#ede9fe", "#5b21b6", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Item Purchase Request", d.prDetailRows, prDetailRenderer(), prDetailHeaders(), prDetailWidths());
			}
		});
		appendMetricCard(wrap, "PR Disetujui", approved.size(), "Sudah approval", "#dcfce7", "#166534", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("PR Disetujui", approved, prRenderer(), prHeaders(), prWidths());
			}
		});
		appendMetricCard(wrap, "PR Ditolak", rejected.size(), "Ditolak/reject", "#fee2e2", "#991b1b", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("PR Ditolak", rejected, prRenderer(), prHeaders(), prWidths());
			}
		});
		appendMetricCard(wrap, "Detail Belum Realisasi", unrealized.size(), "Belum lewat PO/Uang Muka", "#fef3c7", "#92400e", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail PR Belum Realisasi", unrealized, prDetailRenderer(), prDetailHeaders(), prDetailWidths());
			}
		});
	}

	private void renderPurchaseOrderDashboard(final VendorDashboardData d) {
		String pcWidth = Common.isMobile() ? "100%" : "50%";
		MyPortalchildren pc = new MyPortalchildren();
		pc.setWidth(pcWidth);
		pc.setStyle("padding:6px; box-sizing:border-box;");
		pc.setParent(this);
		Panelchildren pch = createModernPanel("Dasbor Purchase Order (PO)", pc);
		appendDownloadExcelButton(pch, "Download Excel Ringkasan PO", "ringkasan_purchase_order", buildPoSummaryRows(d), arrayRenderer(), simpleMetricHeaders());
		appendDownloadExcelButton(pch, "Download Excel PO", "purchase_order", d.poRows, poRenderer(), poHeaders());
		appendDownloadExcelButton(pch, "Download Excel Detail PO", "detail_purchase_order", d.poDetailRows, poDetailRenderer(), poDetailHeaders());
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:10px;'>Analisis PO dari <b>PemesananPengadaanMasterAsset</b> dan <b>PemesananPengadaanMasterAssetDetail</b>.</div>");
		Div wrap = new Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap; max-width:100%; overflow:visible; box-sizing:border-box;");
		wrap.setParent(pch);
		final List<PemesananPengadaanMasterAsset> direct = filterPoPembelianLangsung(d.poRows);
		final List<PemesananPengadaanMasterAsset> approved = filterPoApproved(d.poRows);
		final double nilaiDetail = sumPoDetailNilai(d.poDetailRows);
		appendMetricCard(wrap, "Total PO", d.poRows.size(), "Master PO", "#dbeafe", "#1e40af", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Purchase Order", d.poRows, poRenderer(), poHeaders(), poWidths());
			}
		});
		appendMetricCard(wrap, "Detail PO", d.poDetailRows.size(), "Item PO", "#ede9fe", "#5b21b6", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Item Purchase Order", d.poDetailRows, poDetailRenderer(), poDetailHeaders(), poDetailWidths());
			}
		});
		appendMetricCard(wrap, "PO Disetujui", approved.size(), "Sudah approval", "#dcfce7", "#166534", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("PO Disetujui", approved, poRenderer(), poHeaders(), poWidths());
			}
		});
		appendMetricCard(wrap, "Pembelian Langsung", direct.size(), "PO direct purchase", "#cffafe", "#155e75", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Pembelian Langsung", direct, poRenderer(), poHeaders(), poWidths());
			}
		});
		appendMetricCard(wrap, "Nilai Detail PO", formatCurrencyShort(nilaiDetail), "Akumulasi detail PO", "#fef3c7", "#92400e", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Nilai PO", d.poDetailRows, poDetailRenderer(), poDetailHeaders(), poDetailWidths());
			}
		});
	}

	private void renderBastDashboard(final VendorDashboardData d) {
		MyPortalchildren pc = getMainColumn();
		Panelchildren pch = createModernPanel("Dasbor Terima Barang / BAST", pc);
		appendDownloadExcelButton(pch, "Download Excel Ringkasan BAST", "ringkasan_bast", buildBastSummaryRows(d), arrayRenderer(), simpleMetricHeaders());
		appendDownloadExcelButton(pch, "Download Excel BAST", "bast", d.penerimaanRows, penerimaanRenderer(), penerimaanHeaders());
		appendDownloadExcelButton(pch, "Download Excel Detail BAST", "detail_bast", d.bastDetailRows, bastDetailRenderer(), bastDetailHeaders());
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:10px;'>Analisis BAST dari <b>PenerimaanPengadaanMasterAsset</b> dan <b>PenerimaanPengadaanMasterAssetDetail</b>.</div>");
		Div wrap = new Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap; max-width:100%; overflow:visible; box-sizing:border-box;");
		wrap.setParent(pch);
		final double qtyBast = sumBastDetailQty(d.bastDetailRows);
		final double nilaiBast = sumBastDetailNilai(d.bastDetailRows);
		final List<PenerimaanPengadaanMasterAsset> withoutInvoice = filterBastWithoutInvoice(d.penerimaanRows);
		appendMetricCard(wrap, "Total BAST", d.penerimaanRows.size(), "Master penerimaan", "#dbeafe", "#1e40af", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail BAST / Penerimaan", d.penerimaanRows, penerimaanRenderer(), penerimaanHeaders(), penerimaanWidths());
			}
		});
		appendMetricCard(wrap, "Detail BAST", d.bastDetailRows.size(), "Item diterima", "#ede9fe", "#5b21b6", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Item BAST", d.bastDetailRows, bastDetailRenderer(), bastDetailHeaders(), bastDetailWidths());
			}
		});
		appendMetricCard(wrap, "Qty Diterima", formatNumber(qtyBast), "Total barang/jasa diterima", "#dcfce7", "#166534", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Qty Diterima BAST", d.bastDetailRows, bastDetailRenderer(), bastDetailHeaders(), bastDetailWidths());
			}
		});
		appendMetricCard(wrap, "Nilai BAST", formatCurrencyShort(nilaiBast), "Akumulasi nilai item BAST", "#cffafe", "#155e75", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("Detail Nilai BAST", d.bastDetailRows, bastDetailRenderer(), bastDetailHeaders(), bastDetailWidths());
			}
		});
		appendMetricCard(wrap, "BAST Belum Tagihan", withoutInvoice.size(), "Belum menjadi SaldoAwal/tagihan", "#fee2e2", "#991b1b", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail("BAST Belum Terima Tagihan", withoutInvoice, penerimaanRenderer(), penerimaanHeaders(), penerimaanWidths());
			}
		});
	}

	private void renderDocumentSummary(Component parent, List<VendorAnalysisRow> rows) {
		if (rows == null || rows.isEmpty()) {
			appendEmptyState(parent, "Belum ada data dokumen vendor.");
			return;
		}
		String html = "";
		int shown = 0;
		for (int i = 0; i < rows.size(); i++) {
			VendorAnalysisRow row = rows.get(i);
			if (row.totalDokumen <= 0) {
				continue;
			}
			shown++;
			if (shown > 8) {
				break;
			}
			int pct = row.totalDokumen <= 0 ? 0 : (int) Math.round(row.dokumenTerverifikasi * 100.0 / row.totalDokumen);
			html += "<div style='padding:10px 0; border-bottom:1px solid #f1f5f9;'>"
					+ "<div style='display:flex; justify-content:space-between; gap:10px;'>"
					+ "<div style='font-size:12px; font-weight:800; color:#334155;'>" + escapeHtml(row.namaVendor) + "</div>"
					+ "<div style='font-size:12px; font-weight:900; color:#0f172a;'>" + pct + "%</div></div>"
					+ "<div style='margin-top:7px; height:8px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>"
					+ "<div style='height:8px; width:" + pct + "%; background:#2563eb; border-radius:999px;'></div></div>"
					+ "<div style='font-size:11px; color:#64748b; margin-top:4px;'>Terverifikasi " + row.dokumenTerverifikasi + " dari " + row.totalDokumen + " dokumen</div>"
					+ "</div>";
		}
		if (shown == 0) {
			appendEmptyState(parent, "Belum ada dokumen vendor pada filter ini.");
		} else {
			appendHtml(parent, html);
		}
	}

	private InMemoryRowRenderer vendorDetailRenderer() {
		return new InMemoryRowRenderer() {
			@Override
			public void render(Row row, Object obj) throws Exception {
				VendorAnalysisRow v = (VendorAnalysisRow) obj;
				row.appendChild(new Label(v.namaVendor));
				row.appendChild(new Label(formatDate(v.tanggalMulai)));
				row.appendChild(new Label(formatDate(v.tanggalAkhir)));
				row.appendChild(new Label(formatCurrency(v.nominalTransaksi)));
				row.appendChild(new Label(v.getProdukRingkas()));
				row.appendChild(new Label(formatNumber(v.qtyDipesan)));
				row.appendChild(new Label(formatNumber(v.qtyDatang)));
				row.appendChild(new Label(formatCurrency(v.totalBiayaDibayar)));
				row.appendChild(new Label(formatPercent(v.qualityScore)));
				row.appendChild(new Label(formatPercent(v.timeScore)));
				row.appendChild(new Label(formatPercent(v.serviceScore)));
				Label eval = new Label(v.evaluation);
				eval.setStyle("font-weight:bold; " + (v.finalScore < 50 ? "color:#dc2626;" : (v.finalScore < 75 ? "color:#d97706;" : "color:#16a34a;")));
				row.appendChild(eval);
			}
		};
	}

	private String[] vendorHeaders() {
		return new String[] { "Nama Vendor", "Tanggal Mulai Transaksi", "Tanggal Akhir Transaksi", "Jumlah Nominal Transaksi",
				"Nama Barang Yang Dipesan Dalam PO", "Qty Dipesan PO", "Qty Datang / Diterima BAST", "Total Biaya Yang Dibayar",
				"Total Nilai Kinerja Kualitas (%)", "Total Nilai Kinerja Ketepatan Waktu Pengiriman (%)",
				"Total Nilai Kinerja Layanan (%)", "Hasil Evaluasi" };
	}

	private String[] vendorWidths() {
		return new String[] { "220px", "130px", "130px", "150px", "260px", "110px", "140px", "150px", "160px", "210px", "150px", "260px" };
	}

	private InMemoryRowRenderer productDetailRenderer() {
		return new InMemoryRowRenderer() {
			@Override
			public void render(Row row, Object obj) throws Exception {
				ProductAnalysisRow p = (ProductAnalysisRow) obj;
				row.appendChild(new Label(p.namaVendor));
				row.appendChild(new Label(p.namaProduk));
				row.appendChild(new Label(formatDate(p.tanggalMulai)));
				row.appendChild(new Label(formatDate(p.tanggalAkhir)));
				row.appendChild(new Label(formatNumber(p.qtyTransaksi)));
				row.appendChild(new Label(formatCurrency(p.nominalTransaksi)));
				row.appendChild(new Label(formatNumber(p.qtyDipesan)));
				row.appendChild(new Label(formatNumber(p.qtyDatang)));
			}
		};
	}

	private String[] productHeaders() {
		return new String[] { "Nama Vendor", "Nama Produk", "Tanggal Mulai Transaksi", "Tanggal Akhir Transaksi",
				"Jumlah Quantity Transaksi", "Jumlah Nominal Transaksi", "Jumlah Barang Yang Dipesan", "Jumlah Barang Yang Datang" };
	}

	private String[] productWidths() {
		return new String[] { "230px", "260px", "140px", "140px", "150px", "160px", "160px", "160px" };
	}

	private InMemoryRowRenderer penerimaanRenderer() {
		return new InMemoryRowRenderer() {
			@Override
			public void render(Row row, Object obj) throws Exception {
				PenerimaanPengadaanMasterAsset p = (PenerimaanPengadaanMasterAsset) obj;
				row.appendChild(new Label(safe(p.getKode())));
				row.appendChild(new Label(getVendorName(p.getPenyedia(), "Tanpa Vendor")));
				row.appendChild(new Label(formatDate(p.getTanggalPembuatan())));
				row.appendChild(new Label(p.getPemesananPengadaanMasterAsset() == null ? "" : safe(p.getPemesananPengadaanMasterAsset().getKode())));
				row.appendChild(new Label(formatCurrency(safeDouble(p.getNilai()))));
				row.appendChild(new Label(formatPercent(nvl(readScoreByMethods(p, QUALITY_METHODS), 100.0))));
				row.appendChild(new Label(formatPercent(nvl(readScoreByMethods(p, TIME_METHODS), calculateDeliveryScore(p)))));
				row.appendChild(new Label(formatPercent(nvl(readScoreByMethods(p, SERVICE_METHODS), 100.0))));
			}
		};
	}

	private String[] penerimaanHeaders() {
		return new String[] { "Kode BAST/Penerimaan", "Vendor", "Tanggal", "Kode PO", "Nilai", "Kualitas", "Ketepatan Waktu", "Layanan" };
	}

	private String[] penerimaanWidths() {
		return new String[] { "160px", "240px", "140px", "160px", "150px", "120px", "150px", "120px" };
	}

	private InMemoryRowRenderer saldoRenderer() {
		return new InMemoryRowRenderer() {
			@Override
			public void render(Row row, Object obj) throws Exception {
				SaldoAwalMasterAsset s = (SaldoAwalMasterAsset) obj;
				row.appendChild(new Label(safe(s.getKode())));
				row.appendChild(new Label(getVendorName(s.getPenyedia(), "Tanpa Vendor")));
				row.appendChild(new Label(formatDate(s.getTanggalPembuatan())));
				row.appendChild(new Label(formatDate(s.getTanggalPersetujuan())));
				row.appendChild(new Label(formatCurrency(safeDouble(s.getNilai()))));
				row.appendChild(new Label(formatCurrency(safeDouble(s.getDibayar()))));
				row.appendChild(new Label(Boolean.TRUE.equals(s.getLunas()) ? "Lunas" : "Belum Lunas"));
				row.appendChild(new Label(safe(s.getKodeTagihan())));
				row.appendChild(new Label(formatDate(s.getTanggalTagihan())));
				row.appendChild(new Label(getEntityName(s.getSatuanKerja(), "")));
			}
		};
	}

	private String[] saldoHeaders() {
		return new String[] { "Kode Tagihan", "Vendor", "Tanggal", "Tanggal Persetujuan", "Nilai", "Dibayar", "Status", "Kode Invoice", "Tanggal Invoice", "Satuan Kerja" };
	}

	private String[] saldoWidths() {
		return new String[] { "160px", "240px", "130px", "150px", "150px", "150px", "110px", "160px", "130px", "220px" };
	}

	private InMemoryRowRenderer saldoDetailRenderer() {
		return new InMemoryRowRenderer() {
			@Override
			public void render(Row row, Object obj) throws Exception {
				SaldoAwalMasterAssetDetail d = (SaldoAwalMasterAssetDetail) obj;
				SaldoAwalMasterAsset s = d.getSaldoAwal();
				row.appendChild(new Label(s == null ? "" : safe(s.getKode())));
				row.appendChild(new Label(s == null ? "Tanpa Vendor" : getVendorName(s.getPenyedia(), "Tanpa Vendor")));
				row.appendChild(new Label(getMasterAssetName(d.getMasterAsset())));
				row.appendChild(new Label(formatNumber(safeDouble(d.getJumlah()))));
				row.appendChild(new Label(formatCurrency(safeDouble(d.getHarga()))));
				row.appendChild(new Label(formatCurrency(safeDouble(d.getHargaTotal()))));
				row.appendChild(new Label(s == null ? "" : safe(s.getKodeTagihan())));
				row.appendChild(new Label(s == null ? "" : formatDate(s.getTanggalTagihan())));
				row.appendChild(new Label(getEntityName(d.getSatuanKerja(), "")));
			}
		};
	}

	private String[] saldoDetailHeaders() {
		return new String[] { "Kode Tagihan", "Vendor", "Produk/Barang", "Qty", "Harga", "Total", "Kode Invoice", "Tanggal Invoice", "Satuan Kerja" };
	}

	private String[] saldoDetailWidths() {
		return new String[] { "160px", "240px", "260px", "100px", "140px", "150px", "160px", "140px", "220px" };
	}

	private InMemoryRowRenderer prRenderer() {
		return new InMemoryRowRenderer() {
			@Override
			public void render(Row row, Object obj) throws Exception {
				PermintaanPengadaanMasterAsset pr = (PermintaanPengadaanMasterAsset) obj;
				row.appendChild(new Label(safe(pr.getKode())));
				row.appendChild(new Label(formatDate(pr.getTanggalPembuatan())));
				row.appendChild(new Label(formatDate(pr.getTanggalPersetujuan())));
				row.appendChild(new Label(formatDate(pr.getTanggalDitolak())));
				row.appendChild(new Label(getEntityName(pr.getSatuanKerja(), "")));
				row.appendChild(new Label(formatCurrency(safeDouble(pr.getNilai()))));
				row.appendChild(new Label(pr.getDisetujuiOleh() != null ? "Disetujui" : (pr.getDitolakOleh() != null ? "Ditolak" : "Proses")));
				row.appendChild(new Label(safe(pr.getKeterangan())));
			}
		};
	}

	private String[] prHeaders() {
		return new String[] { "Kode PR", "Tanggal PR", "Tanggal Setuju", "Tanggal Ditolak", "Satuan Kerja", "Nilai", "Status", "Keterangan" };
	}

	private String[] prWidths() {
		return new String[] { "160px", "130px", "130px", "130px", "220px", "150px", "110px", "280px" };
	}

	private InMemoryRowRenderer prDetailRenderer() {
		return new InMemoryRowRenderer() {
			@Override
			public void render(Row row, Object obj) throws Exception {
				PermintaanPengadaanMasterAssetDetail d = (PermintaanPengadaanMasterAssetDetail) obj;
				PermintaanPengadaanMasterAsset pr = d.getPermintaanPengadaanMasterAsset();
				row.appendChild(new Label(pr == null ? "" : safe(pr.getKode())));
				row.appendChild(new Label(formatDate(d.getTanggalPembuatan())));
				row.appendChild(new Label(getMasterAssetName(d.getMasterAsset())));
				row.appendChild(new Label(formatNumber(safeDouble(d.getJumlah()))));
				row.appendChild(new Label(formatNumber(safeDouble(d.getJumlahDatang()))));
				row.appendChild(new Label(formatCurrency(safeDouble(d.getHargaBeli()))));
				row.appendChild(new Label(formatCurrency(safeDouble(d.getHargaTotal()))));
				row.appendChild(new Label(d.getUangMuka() != null ? "Uang Muka" : (d.getPemesananPengadaanMasterAssetDetail() != null ? "PO" : "Belum Realisasi")));
				row.appendChild(new Label(getEntityName(d.getSatuanKerja(), "")));
			}
		};
	}

	private String[] prDetailHeaders() {
		return new String[] { "Kode PR", "Tanggal", "Produk/Barang", "Qty PR", "Qty Datang", "Harga", "Total", "Realisasi", "Satuan Kerja" };
	}

	private String[] prDetailWidths() {
		return new String[] { "160px", "130px", "260px", "100px", "110px", "140px", "150px", "130px", "220px" };
	}

	private InMemoryRowRenderer poRenderer() {
		return new InMemoryRowRenderer() {
			@Override
			public void render(Row row, Object obj) throws Exception {
				PemesananPengadaanMasterAsset po = (PemesananPengadaanMasterAsset) obj;
				row.appendChild(new Label(safe(po.getKode())));
				row.appendChild(new Label(getVendorName(po.getPenyedia(), "Tanpa Vendor")));
				row.appendChild(new Label(formatDate(po.getTanggalPembuatan())));
				row.appendChild(new Label(formatDate(po.getTanggalPersetujuan())));
				row.appendChild(new Label(formatDate(po.getPengirimanPalingLambat())));
				row.appendChild(new Label(formatCurrency(safeDouble(po.getNilai()))));
				row.appendChild(new Label(formatCurrency(safeDouble(po.getDibayar()))));
				row.appendChild(new Label(Boolean.TRUE.equals(po.getPembelianLangsung()) ? "Pembelian Langsung" : "PO Reguler"));
				row.appendChild(new Label(getEntityName(po.getSatuanKerja(), "")));
			}
		};
	}

	private String[] poHeaders() {
		return new String[] { "Kode PO", "Vendor", "Tanggal PO", "Tanggal Setuju", "Deadline Kirim", "Nilai", "Dibayar", "Jenis", "Satuan Kerja" };
	}

	private String[] poWidths() {
		return new String[] { "160px", "240px", "130px", "130px", "130px", "150px", "150px", "160px", "220px" };
	}

	private InMemoryRowRenderer poDetailRenderer() {
		return new InMemoryRowRenderer() {
			@Override
			public void render(Row row, Object obj) throws Exception {
				PemesananPengadaanMasterAssetDetail d = (PemesananPengadaanMasterAssetDetail) obj;
				PemesananPengadaanMasterAsset po = d.getPemesananPengadaanMasterAsset();
				row.appendChild(new Label(po == null ? "" : safe(po.getKode())));
				row.appendChild(new Label(po == null ? "Tanpa Vendor" : getVendorName(po.getPenyedia(), "Tanpa Vendor")));
				row.appendChild(new Label(po == null ? "" : formatDate(po.getTanggalPembuatan())));
				row.appendChild(new Label(getMasterAssetName(d.getMasterAsset())));
				row.appendChild(new Label(formatNumber(safeDouble(d.getJumlah()))));
				row.appendChild(new Label(formatCurrency(safeDouble(d.getHargaBeli()))));
				row.appendChild(new Label(formatCurrency(safeDouble(d.getHargaTotal()))));
				row.appendChild(new Label(d.getPenerimaanPengadaanMasterAssetDetail() != null ? "Sudah BAST" : "Belum BAST"));
			}
		};
	}

	private String[] poDetailHeaders() {
		return new String[] { "Kode PO", "Vendor", "Tanggal PO", "Produk/Barang", "Qty PO", "Harga", "Total", "Status BAST" };
	}

	private String[] poDetailWidths() {
		return new String[] { "160px", "240px", "130px", "260px", "100px", "140px", "150px", "130px" };
	}

	private InMemoryRowRenderer bastDetailRenderer() {
		return new InMemoryRowRenderer() {
			@Override
			public void render(Row row, Object obj) throws Exception {
				PenerimaanPengadaanMasterAssetDetail d = (PenerimaanPengadaanMasterAssetDetail) obj;
				PenerimaanPengadaanMasterAsset bast = d.getPenerimaanPengadaanMasterAsset();
				row.appendChild(new Label(bast == null ? "" : safe(bast.getKode())));
				row.appendChild(new Label(bast == null ? "Tanpa Vendor" : getVendorName(bast.getPenyedia(), "Tanpa Vendor")));
				row.appendChild(new Label(bast == null ? "" : formatDate(bast.getTanggalPembuatan())));
				row.appendChild(new Label(bast == null || bast.getPemesananPengadaanMasterAsset() == null ? "" : safe(bast.getPemesananPengadaanMasterAsset().getKode())));
				row.appendChild(new Label(getMasterAssetName(d.getMasterAsset())));
				row.appendChild(new Label(formatNumber(safeDouble(d.getJumlah()))));
				row.appendChild(new Label(formatNumber(safeDouble(d.getDiterima()))));
				row.appendChild(new Label(formatCurrency(safeDouble(d.getHargaBeli()))));
				row.appendChild(new Label(formatCurrency(safeDouble(d.getHargaTotal()))));
				row.appendChild(new Label(safe(d.getKondisi())));
			}
		};
	}

	private String[] bastDetailHeaders() {
		return new String[] { "Kode BAST", "Vendor", "Tanggal", "Kode PO", "Produk/Barang", "Qty PO", "Qty Diterima", "Harga", "Total", "Kondisi" };
	}

	private String[] bastDetailWidths() {
		return new String[] { "160px", "240px", "130px", "160px", "260px", "100px", "120px", "140px", "150px", "160px" };
	}

	private InMemoryRowRenderer returRenderer() {
		return new InMemoryRowRenderer() {
			@Override
			public void render(Row row, Object obj) throws Exception {
				ReturPengadaanMasterAsset r = (ReturPengadaanMasterAsset) obj;
				row.appendChild(new Label(safe(r.getKode())));
				row.appendChild(new Label(getVendorName(r.getPenyedia(), "Tanpa Vendor")));
				row.appendChild(new Label(formatDate(r.getTanggalPembuatan())));
				row.appendChild(new Label(formatDate(r.getTanggalPersetujuan())));
				row.appendChild(new Label(r.getPenerimaanPengadaanMasterAsset() == null ? "" : safe(r.getPenerimaanPengadaanMasterAsset().getKode())));
				row.appendChild(new Label(safe(r.getKeterangan())));
			}
		};
	}

	private String[] returHeaders() {
		return new String[] { "Kode Retur", "Vendor", "Tanggal", "Tanggal Setuju", "Kode BAST", "Keterangan" };
	}

	private String[] returWidths() {
		return new String[] { "160px", "240px", "130px", "130px", "160px", "320px" };
	}


	private InMemoryRowRenderer pengadaanProdukRenderer() {
		return new InMemoryRowRenderer() {
			@Override
			public void render(Row row, Object obj) throws Exception {
				PengadaanProduk p = (PengadaanProduk) obj;
				row.appendChild(new Label(safe(p.getNomorFaktur())));
				row.appendChild(new Label(getVendorName(p.getSupplier(), safe(p.getNamaSupplier()))));
				row.appendChild(new Label(getEntityName(p.getProduk(), safe(p.getNamaSupplier()))));
				row.appendChild(new Label(formatDate(p.getWaktuPengadaan())));
				row.appendChild(new Label(formatNumber(safeDouble(p.getQty()))));
				row.appendChild(new Label(formatCurrency(safeDouble(p.getTotalHarga()))));
			}
		};
	}

	private String[] pengadaanProdukHeaders() {
		return new String[] { "Nomor Faktur", "Vendor/Supplier", "Produk", "Tanggal", "Qty", "Total Harga" };
	}

	private void appendDownloadExcelLengkapButton(final Component parent, String label, final String fileName,
			final VendorDashboardData data) {
		try {
			Div wrap = new Div();
			wrap.setStyle("margin:0 0 10px 0; display:flex; justify-content:flex-start; gap:8px; flex-wrap:wrap; max-width:100%; overflow:visible; box-sizing:border-box;");
			wrap.setParent(parent);
			MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig(label == null ? "Download Excel Lengkap" : label, "/img/excel.png");
			btn.setStyle("border:1px solid #86efac; background:#16a34a; color:#ffffff; border-radius:10px; padding:7px 14px; font-weight:bold; box-shadow:0 8px 18px rgba(22,163,74,.18); white-space:normal; max-width:100%;");
			btn.setParent(wrap);
			btn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					downloadExcelLengkap(fileName, data);
				}
			});
		} catch (Exception e) {
			debugError("appendDownloadExcelLengkapButton", e);
		}
	}

	private void appendDownloadExcelButton(final Component parent, String label, final String fileName,
			final List<?> data, final InMemoryRowRenderer renderer, final String[] headers) {
		try {
			Div wrap = new Div();
			wrap.setStyle("margin:0 0 10px 0; display:flex; justify-content:flex-start; gap:8px; flex-wrap:wrap; max-width:100%; overflow:visible; box-sizing:border-box;");
			wrap.setParent(parent);
			MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig(label == null ? "Download Excel" : label, "/img/excel.png");
			btn.setStyle("border:1px solid #bbf7d0; background:#f0fdf4; color:#166534; border-radius:10px; padding:6px 12px; font-weight:bold; white-space:normal; max-width:100%;");
			btn.setParent(wrap);
			btn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					downloadExcel(fileName, data, renderer, headers);
				}
			});
		} catch (Exception e) {
			debugError("appendDownloadExcelButton", e);
		}
	}

	private void appendDownloadExcelButton(final Component parent, String label, final String fileName,
			final List<?> data, final ExcelRowBuilder builder, final String[] headers) {
		try {
			Div wrap = new Div();
			wrap.setStyle("margin:0 0 10px 0; display:flex; justify-content:flex-start; gap:8px; flex-wrap:wrap; max-width:100%; overflow:visible; box-sizing:border-box;");
			wrap.setParent(parent);
			MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig(label == null ? "Download Excel" : label, "/img/excel.png");
			btn.setStyle("border:1px solid #bbf7d0; background:#f0fdf4; color:#166534; border-radius:10px; padding:6px 12px; font-weight:bold; white-space:normal; max-width:100%;");
			btn.setParent(wrap);
			btn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					downloadExcel(fileName, data, builder, headers);
				}
			});
		} catch (Exception e) {
			debugError("appendDownloadExcelButton.builder", e);
		}
	}

	private void downloadExcel(String fileName, List<?> data, ExcelRowBuilder builder, String[] headers) {
		XSSFWorkbook workbook = null;
		FileOutputStream fos = null;
		File file = null;
		try {
			workbook = new XSSFWorkbook();
			writeSheet(workbook, "Data", data, builder, headers);
			file = File.createTempFile(sanitizeFileName(fileName == null ? "dashboard_vendor" : fileName) + "_", ".xlsx");
			fos = new FileOutputStream(file);
			workbook.write(fos);
			fos.flush();
			Filedownload.save(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		} catch (Exception e) {
			debugError("downloadExcel.builder", e);
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeQuietly(fos);
		}
	}

	private void downloadExcelLengkap(String fileName, VendorDashboardData data) {
		XSSFWorkbook workbook = null;
		FileOutputStream fos = null;
		File file = null;
		try {
			VendorDashboardData d = data == null ? new VendorDashboardData() : data;
			workbook = new XSSFWorkbook();
			writeSheet(workbook, "Ringkasan", buildOverviewSummaryRows(d), arrayBuilder(), overviewSummaryHeaders());
			writeSheet(workbook, "Spider Web", buildRadarExportRows(d), arrayBuilder(), radarExportHeaders());
			writeSheet(workbook, "Tren Bulanan", buildTrendExportRows(d), arrayBuilder(), trendExportHeaders());
			writeSheet(workbook, "Top Vendor", buildTopVendorExportRows(d), arrayBuilder(), topVendorExportHeaders());
			writeSheet(workbook, "Analisa Vendor", d.vendorRows, vendorDetailRenderer(), vendorHeaders());
			writeSheet(workbook, "Analisa Produk", d.productRows, productDetailRenderer(), productHeaders());
			writeSheet(workbook, "Risiko Vendor", buildRiskExportRows(d), arrayBuilder(), riskExportHeaders());
			writeSheet(workbook, "Kepatuhan Dokumen", buildComplianceExportRows(d), arrayBuilder(), complianceExportHeaders());
			writeSheet(workbook, "Insight", buildInsightExportRows(d), arrayBuilder(), insightExportHeaders());
			writeSheet(workbook, "Tagihan", d.saldoRows, saldoRenderer(), saldoHeaders());
			writeSheet(workbook, "Detail Tagihan", d.saldoDetailRows, saldoDetailRenderer(), saldoDetailHeaders());
			writeSheet(workbook, "PR", d.prRows, prRenderer(), prHeaders());
			writeSheet(workbook, "Detail PR", d.prDetailRows, prDetailRenderer(), prDetailHeaders());
			writeSheet(workbook, "PO", d.poRows, poRenderer(), poHeaders());
			writeSheet(workbook, "Detail PO", d.poDetailRows, poDetailRenderer(), poDetailHeaders());
			writeSheet(workbook, "BAST", d.penerimaanRows, penerimaanRenderer(), penerimaanHeaders());
			writeSheet(workbook, "Detail BAST", d.bastDetailRows, bastDetailRenderer(), bastDetailHeaders());
			writeSheet(workbook, "Retur", d.returRows, returRenderer(), returHeaders());
			writeSheet(workbook, "Pengadaan Produk", d.pengadaanProdukRows, pengadaanProdukRenderer(), pengadaanProdukHeaders());
			file = File.createTempFile(sanitizeFileName(fileName == null ? "dashboard_analisis_vendor_lengkap" : fileName) + "_", ".xlsx");
			fos = new FileOutputStream(file);
			workbook.write(fos);
			fos.flush();
			Filedownload.save(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		} catch (Exception e) {
			debugError("downloadExcelLengkap", e);
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeQuietly(fos);
		}
	}

	private void writeSheet(XSSFWorkbook workbook, String sheetName, List<?> data, InMemoryRowRenderer renderer, String[] headers) throws Exception {
		writeSheet(workbook, sheetName, data, new RendererExcelRowBuilder(renderer), headers);
	}

	private void writeSheet(XSSFWorkbook workbook, String sheetName, List<?> data, ExcelRowBuilder builder, String[] headers) throws Exception {
		XSSFSheet sheet = workbook.createSheet(sanitizeSheetName(sheetName));
		XSSFRow headerRow = sheet.createRow(0);
		if (headers != null) {
			for (int i = 0; i < headers.length; i++) {
				headerRow.createCell(i).setCellValue(headers[i] == null ? "" : headers[i]);
			}
		}
		int rowIndex = 1;
		if (data != null && builder != null) {
			for (Object obj : data) {
				Object[] values = builder.build(obj);
				XSSFRow excelRow = sheet.createRow(rowIndex++);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						setCellValue(excelRow, i, values[i]);
					}
				}
			}
		}
		int columnCount = headers == null ? 0 : headers.length;
		for (int i = 0; i < columnCount && i < 30; i++) {
			try {
				sheet.autoSizeColumn(i);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:1845");
			}
		}
	}

	private void setCellValue(XSSFRow row, int columnIndex, Object value) {
		if (row == null) {
			return;
		}
		try {
			if (value == null) {
				row.createCell(columnIndex).setCellValue("");
			} else if (value instanceof Number) {
				row.createCell(columnIndex).setCellValue(((Number) value).doubleValue());
			} else if (value instanceof Date) {
				row.createCell(columnIndex).setCellValue(formatDate((Date) value));
			} else if (value instanceof Boolean) {
				row.createCell(columnIndex).setCellValue(((Boolean) value).booleanValue() ? "Ya" : "Tidak");
			} else {
				row.createCell(columnIndex).setCellValue(String.valueOf(value));
			}
		} catch (Exception e) {
			try {
				row.createCell(columnIndex).setCellValue(value == null ? "" : String.valueOf(value));
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:1869");
			}
		}
	}

	private void downloadExcel(String fileName, List<?> data, InMemoryRowRenderer renderer, String[] headers) {
		XSSFWorkbook workbook = null;
		FileOutputStream fos = null;
		File file = null;
		try {
			workbook = new XSSFWorkbook();
			writeSheet(workbook, "Data", data, renderer, headers);
			file = File.createTempFile(sanitizeFileName(fileName == null ? "dashboard_vendor" : fileName) + "_", ".xlsx");
			fos = new FileOutputStream(file);
			workbook.write(fos);
			fos.flush();
			Filedownload.save(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		} catch (Exception e) {
			debugError("downloadExcel", e);
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeQuietly(fos);
		}
	}

	private String extractText(Component component) {
		if (component == null) {
			return "";
		}
		try {
			if (component instanceof Label) {
				return ((Label) component).getValue();
			}
			if (component instanceof A) {
				return ((A) component).getLabel();
			}
			if (component instanceof Html) {
				return ((Html) component).getContent();
			}
			StringBuilder sb = new StringBuilder();
			List children = component.getChildren();
			for (int i = 0; i < children.size(); i++) {
				String val = extractText((Component) children.get(i));
				if (val != null && val.length() > 0) {
					if (sb.length() > 0) sb.append(" ");
					sb.append(val);
				}
			}
			return sb.toString();
		} catch (Exception e) {
			return "";
		}
	}

	private String sanitizeFileName(String name) {
		String value = name == null ? "dashboard_vendor" : name;
		value = value.replaceAll("[^a-zA-Z0-9_-]", "_");
		if (value.length() == 0) {
			value = "dashboard_vendor";
		}
		return value;
	}

	private InMemoryRowRenderer arrayRenderer() {
		return new InMemoryRowRenderer() {
			@Override
			public void render(Row row, Object obj) throws Exception {
				Object[] values = obj instanceof Object[] ? (Object[]) obj : new Object[] { obj };
				for (int i = 0; i < values.length; i++) {
					Object value = values[i];
					row.appendChild(new Label(value == null ? "" : String.valueOf(value)));
				}
			}
		};
	}

	private ExcelRowBuilder arrayBuilder() {
		return new ExcelRowBuilder() {
			@Override
			public Object[] build(Object obj) throws Exception {
				return obj instanceof Object[] ? (Object[]) obj : new Object[] { obj };
			}
		};
	}

	private String[] overviewSummaryHeaders() {
		return new String[] { "Indikator", "Nilai", "Penjelasan" };
	}

	private String[] simpleMetricHeaders() {
		return new String[] { "Indikator", "Nilai", "Penjelasan" };
	}

	private String[] radarExportHeaders() {
		return new String[] { "Indikator Spider Web", "Nilai (%)", "Penjelasan" };
	}

	private String[] trendExportHeaders() {
		return new String[] { "Bulan", "Jumlah Aktivitas", "Nominal Transaksi" };
	}

	private String[] topVendorExportHeaders() {
		return new String[] { "Peringkat", "Vendor", "Nominal Transaksi", "Skor Akhir (%)", "Evaluasi" };
	}

	private String[] riskExportHeaders() {
		return new String[] { "Kategori Risiko", "Jumlah Vendor", "Persentase", "Arahan Tindak Lanjut" };
	}

	private String[] complianceExportHeaders() {
		return new String[] { "Vendor", "Total Dokumen", "Terverifikasi", "Revisi", "Belum", "Persentase Terverifikasi" };
	}

	private String[] insightExportHeaders() {
		return new String[] { "Insight", "Nilai", "Rekomendasi" };
	}

	private List<Object[]> buildOverviewSummaryRows(VendorDashboardData d) {
		List<Object[]> rows = new ArrayList<Object[]>();
		VendorDashboardData data = d == null ? new VendorDashboardData() : d;
		rows.add(new Object[] { "Periode Mulai", formatDate(getMulai()), "Tanggal awal filter dashboard." });
		rows.add(new Object[] { "Periode Sampai", formatDate(getSampai()), "Tanggal akhir filter dashboard." });
		rows.add(new Object[] { "Vendor Bertransaksi", data.vendorRows.size(), "Jumlah vendor yang memiliki aktivitas pada filter saat ini." });
		rows.add(new Object[] { "Nominal Transaksi", data.totalNominal, "Total nilai PO dan pengadaan produk yang terbaca." });
		rows.add(new Object[] { "Produk Dianalisis", data.totalProduk, "Jumlah produk atau barang yang muncul pada analisis." });
		rows.add(new Object[] { "BAST/Penerimaan", data.totalBast, "Jumlah transaksi penerimaan barang atau jasa." });
		rows.add(new Object[] { "Vendor Perlu Perhatian", data.vendorNeedAttention, "Vendor dengan skor evaluasi di bawah 75%." });
		rows.add(new Object[] { "Dokumen Belum Lengkap", data.vendorDokumenBelumLengkap, "Vendor yang dokumennya belum seluruhnya terverifikasi." });
		rows.add(new Object[] { "Total PO", data.poRows.size(), "Jumlah purchase order pada filter saat ini." });
		rows.add(new Object[] { "Total PR", data.prRows.size(), "Jumlah purchase request pada filter saat ini." });
		rows.add(new Object[] { "Total Retur", data.totalRetur, "Jumlah retur yang perlu diperhatikan." });
		return rows;
	}

	private List<Object[]> buildRadarExportRows(VendorDashboardData d) {
		List<Object[]> rows = new ArrayList<Object[]>();
		rows.add(new Object[] { "Kualitas", averageVendorScore(d, "quality"), "Rata-rata nilai kualitas dari BAST atau fallback evaluasi." });
		rows.add(new Object[] { "Ketepatan Waktu", averageVendorScore(d, "time"), "Rata-rata ketepatan waktu pengiriman vendor." });
		rows.add(new Object[] { "Layanan", averageVendorScore(d, "service"), "Rata-rata nilai layanan vendor." });
		rows.add(new Object[] { "Kuantitas", averageVendorScore(d, "quantity"), "Perbandingan quantity dipesan dan quantity diterima." });
		return rows;
	}

	private List<Object[]> buildTrendExportRows(VendorDashboardData d) {
		List<Object[]> rows = new ArrayList<Object[]>();
		Map<String, TrendBucket> trends = buildMonthlyTrendBuckets(d);
		for (String key : trends.keySet()) {
			TrendBucket b = trends.get(key);
			rows.add(new Object[] { key, b == null ? 0 : b.count, b == null ? 0.0 : b.nominal });
		}
		return rows;
	}

	private List<Object[]> buildTopVendorExportRows(VendorDashboardData d) {
		List<Object[]> rows = new ArrayList<Object[]>();
		if (d == null || d.vendorRows == null) {
			return rows;
		}
		int limit = Math.min(20, d.vendorRows.size());
		for (int i = 0; i < limit; i++) {
			VendorAnalysisRow v = d.vendorRows.get(i);
			rows.add(new Object[] { Integer.valueOf(i + 1), v.namaVendor, Double.valueOf(v.nominalTransaksi), Double.valueOf(v.finalScore), v.evaluation });
		}
		return rows;
	}

	private List<Object[]> buildRiskExportRows(VendorDashboardData d) {
		List<Object[]> rows = new ArrayList<Object[]>();
		int total = d == null || d.vendorRows == null ? 0 : d.vendorRows.size();
		int buruk = 0;
		int cukup = 0;
		int baik = 0;
		if (d != null && d.vendorRows != null) {
			for (VendorAnalysisRow row : d.vendorRows) {
				if (row.finalScore < 50.0) buruk++;
				else if (row.finalScore < 75.0) cukup++;
				else baik++;
			}
		}
		rows.add(new Object[] { "Baik / Memuaskan", Integer.valueOf(baik), percentText(baik, total), "Pertahankan vendor dan gunakan sebagai kandidat prioritas." });
		rows.add(new Object[] { "Sedang / Cukup", Integer.valueOf(cukup), percentText(cukup, total), "Pantau kualitas, ketepatan waktu, dan dokumen pendukung." });
		rows.add(new Object[] { "Kurang / Buruk", Integer.valueOf(buruk), percentText(buruk, total), "Perlu evaluasi sebelum transaksi berikutnya." });
		return rows;
	}

	private List<Object[]> buildComplianceExportRows(VendorDashboardData d) {
		List<Object[]> rows = new ArrayList<Object[]>();
		if (d == null || d.vendorRows == null) {
			return rows;
		}
		for (VendorAnalysisRow row : d.vendorRows) {
			if (row.totalDokumen <= 0) {
				continue;
			}
			rows.add(new Object[] { row.namaVendor, Integer.valueOf(row.totalDokumen), Integer.valueOf(row.dokumenTerverifikasi), Integer.valueOf(row.dokumenRevisi), Integer.valueOf(row.dokumenBelum), percentText(row.dokumenTerverifikasi, row.totalDokumen) });
		}
		return rows;
	}

	private List<Object[]> buildInsightExportRows(VendorDashboardData d) {
		List<Object[]> rows = new ArrayList<Object[]>();
		VendorDashboardData data = d == null ? new VendorDashboardData() : d;
		rows.add(new Object[] { "Kuantitas Dipesan", Double.valueOf(data.totalQtyDipesan), "Bandingkan dengan kuantitas datang untuk melihat potensi selisih." });
		rows.add(new Object[] { "Kuantitas Datang", Double.valueOf(data.totalQtyDatang), "Gunakan untuk memastikan barang/jasa benar-benar diterima." });
		rows.add(new Object[] { "Retur", Integer.valueOf(data.totalRetur), data.totalRetur > 0 ? "Review vendor dan spesifikasi barang." : "Belum ada retur pada filter ini." });
		rows.add(new Object[] { "Biaya Dibayar", Double.valueOf(data.totalDibayar), "Akumulasi pembayaran yang terbaca pada PO." });
		rows.add(new Object[] { "Vendor Prioritas", getTopVendorText(data), "Vendor dengan kontribusi transaksi terbesar." });
		rows.add(new Object[] { "Produk Strategis", getTopProductText(data), "Produk yang paling menonjol pada filter saat ini." });
		return rows;
	}

	private List<Object[]> buildInvoiceSummaryRows(VendorDashboardData d) {
		VendorDashboardData data = d == null ? new VendorDashboardData() : d;
		List<Object[]> rows = new ArrayList<Object[]>();
		rows.add(new Object[] { "Tagihan Vendor", Integer.valueOf(data.saldoRows.size()), "Jumlah master tagihan vendor." });
		rows.add(new Object[] { "Detail Tagihan", Integer.valueOf(data.saldoDetailRows.size()), "Jumlah item/barang tagihan vendor." });
		rows.add(new Object[] { "Nominal Tagihan", Double.valueOf(sumSaldoNilai(data.saldoRows)), "Total nilai tagihan." });
		rows.add(new Object[] { "Sudah Dibayar", Double.valueOf(sumSaldoDibayar(data.saldoRows)), "Total yang sudah dibayar." });
		rows.add(new Object[] { "Belum Lunas", Integer.valueOf(filterSaldoBelumLunas(data.saldoRows).size()), "Tagihan yang masih perlu follow up." });
		return rows;
	}

	private List<Object[]> buildPrSummaryRows(VendorDashboardData d) {
		VendorDashboardData data = d == null ? new VendorDashboardData() : d;
		List<Object[]> rows = new ArrayList<Object[]>();
		rows.add(new Object[] { "Total PR", Integer.valueOf(data.prRows.size()), "Jumlah master purchase request." });
		rows.add(new Object[] { "Detail PR", Integer.valueOf(data.prDetailRows.size()), "Jumlah item pada purchase request." });
		rows.add(new Object[] { "PR Disetujui", Integer.valueOf(filterPrApproved(data.prRows).size()), "PR yang sudah mendapat persetujuan." });
		rows.add(new Object[] { "PR Ditolak", Integer.valueOf(filterPrRejected(data.prRows).size()), "PR yang ditolak." });
		rows.add(new Object[] { "Detail Belum Realisasi", Integer.valueOf(filterPrDetailUnrealized(data.prDetailRows).size()), "Item PR yang belum lewat PO atau uang muka." });
		return rows;
	}

	private List<Object[]> buildPoSummaryRows(VendorDashboardData d) {
		VendorDashboardData data = d == null ? new VendorDashboardData() : d;
		List<Object[]> rows = new ArrayList<Object[]>();
		rows.add(new Object[] { "Total PO", Integer.valueOf(data.poRows.size()), "Jumlah master purchase order." });
		rows.add(new Object[] { "Detail PO", Integer.valueOf(data.poDetailRows.size()), "Jumlah item pada purchase order." });
		rows.add(new Object[] { "PO Disetujui", Integer.valueOf(filterPoApproved(data.poRows).size()), "PO yang sudah mendapat persetujuan." });
		rows.add(new Object[] { "Pembelian Langsung", Integer.valueOf(filterPoPembelianLangsung(data.poRows).size()), "PO yang termasuk pembelian langsung." });
		rows.add(new Object[] { "Nilai Detail PO", Double.valueOf(sumPoDetailNilai(data.poDetailRows)), "Akumulasi nominal detail PO." });
		return rows;
	}

	private List<Object[]> buildBastSummaryRows(VendorDashboardData d) {
		VendorDashboardData data = d == null ? new VendorDashboardData() : d;
		List<Object[]> rows = new ArrayList<Object[]>();
		rows.add(new Object[] { "Total BAST", Integer.valueOf(data.penerimaanRows.size()), "Jumlah master penerimaan." });
		rows.add(new Object[] { "Detail BAST", Integer.valueOf(data.bastDetailRows.size()), "Jumlah item yang diterima." });
		rows.add(new Object[] { "Qty Diterima", Double.valueOf(sumBastDetailQty(data.bastDetailRows)), "Total barang/jasa diterima." });
		rows.add(new Object[] { "Nilai BAST", Double.valueOf(sumBastDetailNilai(data.bastDetailRows)), "Akumulasi nilai item BAST." });
		rows.add(new Object[] { "BAST Belum Tagihan", Integer.valueOf(filterBastWithoutInvoice(data.penerimaanRows).size()), "BAST yang belum menjadi tagihan." });
		return rows;
	}

	private String percentText(int value, int total) {
		if (total <= 0) {
			return "0%";
		}
		return formatPercent(value * 100.0 / total);
	}

	private void closeQuietly(FileOutputStream fos) {
		try {
			if (fos != null) {
				fos.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:2136");
		}
	}

	private String sanitizeSheetName(String name) {
		String value = name == null ? "Data" : name;
		value = value.replace(':', '_').replace('\\', '_').replace('/', '_').replace('?', '_').replace('*', '_').replace('[', '_').replace(']', '_');
		if (value.length() == 0) {
			value = "Data";
		}
		if (value.length() > 31) {
			value = value.substring(0, 31);
		}
		return value;
	}

	private void renderPagedGrid(final Component parent, final List<?> data, final InMemoryRowRenderer renderer,
			final String[] headers, final String[] widths, final int pageSize) {
		if (data == null || data.isEmpty()) {
			appendEmptyState(parent, "Belum ada data pada filter ini.");
			return;
		}

		final Div wrapper = new Div();
		wrapper.setStyle("width:100%; max-width:100%; overflow-x:auto; overflow-y:hidden; border-radius:14px; border:1px solid #e5e7eb; box-sizing:border-box;");
		wrapper.setParent(parent);

		final Paging paging = new Paging();
		paging.setPageSize(pageSize);
		paging.setTotalSize(data.size());
		paging.setDetailed(true);
		paging.setParent(parent);

		final EventListener renderListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(wrapper);
				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setSclass("fgrid dgrid");
				grid.setStyle("border:0; min-width:980px; table-layout:auto;");
				grid.setParent(wrapper);

				Columns cols = new Columns();
				cols.setParent(grid);
				for (int i = 0; i < headers.length; i++) {
					MyColumnConfig col = new MyColumnConfig(headers[i]);
					if (widths != null && i < widths.length && widths[i] != null) {
						col.setWidth(widths[i]);
					}
					col.setParent(cols);
				}

				Rows rows = new Rows();
				rows.setParent(grid);
				int start = paging.getActivePage() * pageSize;
				int end = Math.min(start + pageSize, data.size());
				for (int i = start; i < end; i++) {
					Row row = new Row();
					row.setValign("top");
					row.setParent(rows);
					renderer.render(row, data.get(i));
				}
			}
		};
		paging.addEventListener("onPaging", renderListener);
		try {
			renderListener.onEvent(null);
		} catch (Exception e) {
			debugError("renderPagedGrid", e);
		}
	}

	private void viewDetail(String title, final List<?> data, final InMemoryRowRenderer renderer, final String[] headers, final String[] widths) {
		try {
			final Window win = new Window(title, "normal", true);
			win.setWidth("95%");
			win.setHeight("85%");
			win.setPosition("center");
			win.setClosable(true);
			win.setSizable(true);
			attachPopupWindow(win);

			Div body = new Div();
			body.setStyle("padding:12px; background:#f8fafc; height:100%; overflow:auto; box-sizing:border-box;");
			body.setParent(win);
			appendDownloadExcelButton(body, "Download Excel", title, data == null ? new ArrayList<Object>() : data, renderer, headers);
			renderPagedGrid(body, data == null ? new ArrayList<Object>() : data, renderer, headers, widths, PAGE_SIZE);
			win.doModal();
		} catch (Exception e) {
			debugError("viewDetail", e);
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void attachPopupWindow(Window win) {
		try {
			if (DasboardAnalisisVendor.this.getPage() != null) {
				win.setPage(DasboardAnalisisVendor.this.getPage());
				return;
			}
		} catch (Exception e) {
			debugError("attachPopupWindow.page", e);
		}
		try {
			Component parent = DasboardAnalisisVendor.this.getParent();
			if (parent != null) {
				win.setParent(parent);
			}
		} catch (Exception e) {
			debugError("attachPopupWindow.parent", e);
		}
	}

	private void showProgress(int percent, String title, String detail) {
		try {
			if (progressPanel == null || progressPanel.getParent() == null) {
				Panelchildren pch = createModernPanel("Memuat Dashboard Vendor", getMainColumn());
				progressPanel = pch.getParent();
				Div box = new Div();
				box.setStyle("position:relative; overflow:hidden; border-radius:18px; padding:18px; color:#ffffff; background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); box-shadow:0 16px 34px rgba(15,23,42,.18);");
				box.setParent(pch);
				progressTitle = new Label();
				progressTitle.setStyle("display:block; font-size:16px; font-weight:900;");
				progressTitle.setParent(box);
				progressDetail = new Label();
				progressDetail.setStyle("display:block; font-size:12px; opacity:.90; margin-top:5px;");
				progressDetail.setParent(box);
				Div rail = new Div();
				rail.setStyle("height:12px; background:rgba(255,255,255,.25); border-radius:999px; overflow:hidden; margin-top:14px;");
				rail.setParent(box);
				progressFill = new Div();
				progressFill.setStyle("height:12px; width:1%; background:#ffffff; border-radius:999px; transition:width .25s ease;");
				progressFill.setParent(rail);
				progressPercent = new Label();
				progressPercent.setStyle("display:block; text-align:right; font-size:12px; font-weight:900; margin-top:7px;");
				progressPercent.setParent(box);
			}
			updateProgress(percent, title, detail);
		} catch (Exception e) {
			debugError("showProgress", e);
		}
	}

	private void updateProgress(int percent, String title, String detail) {
		int pct = percent < 0 ? 0 : (percent > 100 ? 100 : percent);
		try {
			if (progressTitle != null) progressTitle.setValue(title == null ? "Memuat data" : title);
			if (progressDetail != null) progressDetail.setValue(detail == null ? "Mohon tunggu sebentar." : detail);
			if (progressPercent != null) progressPercent.setValue(pct + "%");
			if (progressFill != null) progressFill.setStyle("height:12px; width:" + Math.max(1, pct) + "%; background:#ffffff; border-radius:999px; transition:width .25s ease;");
			try {
				Clients.showBusy((title == null ? "Memuat data" : title) + " - " + pct + "%");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:2289");
			}
		} catch (Exception e) {
			debugError("updateProgress", e);
		}
	}

	private void hideProgress() {
		try {
			Clients.clearBusy();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:2299");
		}
		try {
			if (progressPanel != null) {
				progressPanel.detach();
			}
		} catch (Exception e) {
			debugError("hideProgress", e);
		} finally {
			progressPanel = null;
			progressPortal = null;
			progressFill = null;
			progressPercent = null;
			progressTitle = null;
			progressDetail = null;
		}
	}

	private void appendMetricCard(Component parent, String title, Object value, String desc, String bg, String color, EventListener click) {
		A card = new A();
		card.setStyle("flex:1 1 170px; min-width:170px; max-width:100%; box-sizing:border-box; text-decoration:none; cursor:pointer; display:block; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:14px; box-shadow:0 10px 22px rgba(15,23,42,.06);");
		card.setParent(parent);
		if (click != null) {
			card.addEventListener("onClick", click);
		}
		Div icon = new Div();
		icon.setStyle("width:38px; height:38px; border-radius:12px; display:flex; align-items:center; justify-content:center; font-weight:800; background:" + bg + "; color:" + color + "; float:left; margin-right:10px;");
		icon.setParent(card);
		icon.appendChild(new Label("●"));
		Label val = new Label(String.valueOf(value));
		val.setStyle("font-size:24px; line-height:38px; font-weight:900; color:#0f172a;");
		val.setParent(card);
		Div clear = new Div();
		clear.setStyle("clear:both;");
		clear.setParent(card);
		Label t = new Label(title);
		t.setStyle("display:block; font-size:12px; color:#64748b; margin-top:10px; font-weight:800;");
		t.setParent(card);
		Label d = new Label(desc);
		d.setStyle("display:block; font-size:11px; color:#94a3b8; margin-top:3px;");
		d.setParent(card);
	}

	private void appendInsightCard(Component parent, String title, String value, String desc, String bg, String color, EventListener click) {
		A card = new A();
		card.setStyle("flex:1 1 210px; min-width:210px; max-width:100%; box-sizing:border-box; border-radius:14px; padding:14px; background:" + bg + "; border:1px solid rgba(15,23,42,.08); text-decoration:none; cursor:pointer; display:block;");
		card.setParent(parent);
		if (click != null) {
			card.addEventListener("onClick", click);
		}
		Label val = new Label(value);
		val.setStyle("display:block; font-size:26px; font-weight:900; color:" + color + ";");
		val.setParent(card);
		Label t = new Label(title);
		t.setStyle("display:block; font-size:12px; font-weight:900; color:" + color + "; margin-top:8px;");
		t.setParent(card);
		Label de = new Label(desc);
		de.setStyle("display:block; font-size:11px; color:" + color + "; opacity:.82; margin-top:5px;");
		de.setParent(card);
	}

	private Panelchildren createModernPanel(String title, Component parent) {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setTitle(title);
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle(modernPanelStyle());
		panel.setParent(parent);
		Panelchildren pch = new Panelchildren();
		pch.setStyle("padding:14px; background:#ffffff; width:100%; max-width:100%; overflow:hidden; box-sizing:border-box;");
		pch.setParent(panel);
		appendPanelDescriptionEndUserV27(pch, title);
		return pch;
	}

	private String modernPanelStyle() {
		return "width:100%; max-width:100%; margin-bottom:12px; border:1px solid #e5e7eb; border-radius:16px; overflow:hidden; background:#ffffff; box-shadow:0 12px 24px rgba(15,23,42,.07); box-sizing:border-box;";
	}

	private void appendHtml(Component parent, String html) {
		Html h = new Html(html);
		h.setParent(parent);
	}

	private void appendEmptyState(Component parent, String message) {
		appendHtml(parent, "<div style='padding:18px; border-radius:14px; background:#f8fafc; border:1px dashed #cbd5e1; text-align:center; color:#64748b; font-size:12px;'>" + escapeHtml(message) + "</div>");
	}

	private String buildProgressRow(String label, int value, int total, String color) {
		int pct = total <= 0 ? 0 : (int) Math.round(value * 100.0 / total);
		if (pct < 4 && value > 0) pct = 4;
		return "<div style='display:flex; align-items:center; gap:10px; margin:10px 0;'>"
				+ "<div style='width:150px; font-size:12px; color:#334155; font-weight:700;'>" + escapeHtml(label) + "</div>"
				+ "<div style='flex:1; background:#e5e7eb; border-radius:999px; height:12px; overflow:hidden;'>"
				+ "<div style='height:12px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div></div>"
				+ "<div style='width:46px; text-align:right; font-size:13px; font-weight:800; color:#0f172a;'>" + value + "</div></div>";
	}

	private String buildActionPlanCard(String no, String title, String desc, String bg, String color) {
		return "<div style='border-radius:16px; padding:14px; background:" + bg + "; border:1px solid rgba(15,23,42,.08); min-height:105px;'>"
				+ "<div style='display:flex; justify-content:space-between; gap:12px; align-items:center;'>"
				+ "<div style='font-size:12px; font-weight:900; color:" + color + ";'>" + escapeHtml(title) + "</div>"
				+ "<div style='width:28px; height:28px; border-radius:999px; background:#ffffff; color:" + color + "; display:flex; align-items:center; justify-content:center; font-weight:900;'>" + escapeHtml(no) + "</div></div>"
				+ "<div style='font-size:12px; color:" + color + "; line-height:1.45; margin-top:10px;'>" + escapeHtml(desc) + "</div></div>";
	}

	private VendorAnalysisRow getVendorRow(VendorDashboardData d, PenyediaAsset vendor, String fallback) {
		String key = vendor != null && vendor.getId() != null ? "V_" + vendor.getId() : "NO_VENDOR_" + safe(fallback);
		VendorAnalysisRow row = d.vendorMap.get(key);
		if (row == null) {
			row = new VendorAnalysisRow();
			row.vendor = vendor;
			row.vendorId = vendor != null ? vendor.getId() : null;
			row.namaVendor = getVendorName(vendor, fallback == null || fallback.length() == 0 ? "Tanpa Vendor / Cash Advance" : fallback);
			d.vendorMap.put(key, row);
		}
		return row;
	}

	private ProductAnalysisRow getProductRow(VendorDashboardData d, PenyediaAsset vendor, String productName) {
		String vendorKey = vendor != null && vendor.getId() != null ? String.valueOf(vendor.getId()) : "NO_VENDOR";
		String key = vendorKey + "_" + safe(productName).toLowerCase();
		ProductAnalysisRow row = d.productMap.get(key);
		if (row == null) {
			row = new ProductAnalysisRow();
			row.vendor = vendor;
			row.namaVendor = getVendorName(vendor, "Tanpa Vendor / Cash Advance");
			row.namaProduk = productName == null || productName.trim().length() == 0 ? "Produk Tidak Diketahui" : productName;
			d.productMap.put(key, row);
		}
		return row;
	}

	private PenyediaAsset getVendorFromPrDetail(PermintaanPengadaanMasterAssetDetail detail) {
		try {
			Object poDetail = detail.getPemesananPengadaanMasterAssetDetail();
			Object po = invokeAny(poDetail, new String[] { "getPemesananPengadaanMasterAsset", "getPemesanan" });
			Object vendor = invokeAny(po, new String[] { "getPenyedia", "getSupplier" });
			if (vendor instanceof PenyediaAsset) {
				return (PenyediaAsset) vendor;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:2443");
		}
		return null;
	}

	private Object invokeAny(Object target, String[] methods) {
		if (target == null || methods == null) {
			return null;
		}
		for (int i = 0; i < methods.length; i++) {
			try {
				Method m = target.getClass().getMethod(methods[i], new Class[0]);
				return m.invoke(target, new Object[0]);
			} catch (NoSuchMethodException eTidakAda) {
				// BUKAN error: invokeAny SENGAJA mencoba beberapa kemungkinan nama getter
				// berurutan (entity lama/baru bisa beda nama field) -- kandidat yang tidak
				// ada di kelas ini adalah hal wajar, lanjut coba nama berikutnya tanpa
				// mencatat ke log error (sebelumnya tiap percobaan gagal ikut tercatat,
				// membanjiri log error dgn kejadian yg memang diharapkan/normal).
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:2456");
			}
		}
		return null;
	}

	private Double readScoreByMethods(Object target, String[] methods) {
		Object value = invokeAny(target, methods);
		if (value == null) {
			return null;
		}
		try {
			if (value instanceof Number) {
				return clampPercent(((Number) value).doubleValue());
			}
			return clampPercent(Double.parseDouble(String.valueOf(value)));
		} catch (Exception e) {
			return null;
		}
	}

	private Double calculateDeliveryScore(PenerimaanPengadaanMasterAsset penerimaan) {
		try {
			Date received = penerimaan.getTanggalPembuatan();
			PemesananPengadaanMasterAsset po = penerimaan.getPemesananPengadaanMasterAsset();
			if (po != null) {
				Date due = po.getPengirimanPalingLambat();
				if (due != null && received != null) {
					return received.after(due) ? 60.0 : 100.0;
				}
				Date order = po.getTanggalPembuatan();
				if (order != null && received != null) {
					long days = Math.max(0L, (received.getTime() - order.getTime()) / (24L * 60L * 60L * 1000L));
					if (days <= 7L) return 100.0;
					if (days <= 14L) return 90.0;
					if (days <= 30L) return 75.0;
					return 60.0;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:2495");
		}
		return 100.0;
	}

	private double calculateQuantityScore(double ordered, double received) {
		if (ordered <= 0.0) {
			return received > 0.0 ? 100.0 : 0.0;
		}
		double pct = (received / ordered) * 100.0;
		return clampPercent(pct);
	}

	private double calculateFallbackQualityScore(VendorAnalysisRow row) {
		double score = 100.0 - (row.returCount * 10.0);
		return clampPercent(score);
	}

	private double average(List<Double> values) {
		if (values == null || values.isEmpty()) {
			return 0.0;
		}
		double total = 0.0;
		for (Double d : values) {
			total += d == null ? 0.0 : d.doubleValue();
		}
		return clampPercent(total / values.size());
	}

	private double average(double a, double b, double c, double d) {
		return clampPercent((a + b + c + d) / 4.0);
	}

	private double clampPercent(double v) {
		if (v < 0.0) return 0.0;
		if (v > 100.0) return 100.0;
		return v;
	}

	private String buildEvaluation(double score) {
		if (score < 50.0) {
			return "Kurang / Buruk (" + formatPercent(score) + ") - Supplier perlu diberi peringatan sampai sanksi pemutusan kerjasama.";
		}
		if (score < 75.0) {
			return "Sedang / Cukup (" + formatPercent(score) + ") - Supplier disarankan melakukan peningkatan kinerja atau tindakan perbaikan.";
		}
		return "Baik / Memuaskan (" + formatPercent(score) + ") - Supplier dipertahankan dan perlu perbaikan berkelanjutan.";
	}

	private void updateDateRange(DateRangeRow row, Date date) {
		if (row == null || date == null) {
			return;
		}
		if (row.tanggalMulai == null || date.before(row.tanggalMulai)) {
			row.tanggalMulai = date;
		}
		if (row.tanggalAkhir == null || date.after(row.tanggalAkhir)) {
			row.tanggalAkhir = date;
		}
	}

	private DashboardSession openDashboardSession() {
		DashboardSession holder = new DashboardSession();
		try {
			Session current = HibernateUtil.currentSession();
			if (current != null && current.isOpen()) {
				holder.session = current;
				holder.mustClose = false;
				return holder;
			}
		} catch (Exception e) {
			debugError("openDashboardSession.currentSession", e);
		}
		try {
			holder.session = HibernateUtil.getSessionFactory().openSession();
			holder.mustClose = true;
		} catch (Exception e) {
			debugError("openDashboardSession.openSession", e);
			throw new RuntimeException(e);
		}
		return holder;
	}

	private void closeDashboardSession(DashboardSession holder) {
		if (holder == null || !holder.mustClose || holder.session == null) {
			return;
		}
		try {
			if (holder.session.isOpen()) {
				try {
					holder.session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:2586");
				}
				try {
					holder.session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:2590");
				}
				holder.session.close();
			}
		} catch (Exception e) {
			debugError("closeDashboardSession", e);
		}
	}

	private Date awalHari(Date date) {
		if (date == null) {
			return null;
		}
		Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	private Date akhirHari(Date date) {
		if (date == null) {
			return null;
		}
		Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
		return cal.getTime();
	}

	private String radarPoint(double cx, double cy, double score, double degree) {
		double r = 90.0 * clampPercent(score) / 100.0;
		double rad = Math.toRadians(degree);
		double x = cx + Math.cos(rad) * r;
		double y = cy + Math.sin(rad) * r;
		return Math.round(x) + "," + Math.round(y);
	}

	private double averageVendorScore(VendorDashboardData d, String key) {
		if (d == null || d.vendorRows == null || d.vendorRows.isEmpty()) {
			return 0.0;
		}
		double total = 0.0;
		for (VendorAnalysisRow row : d.vendorRows) {
			if ("quality".equals(key)) total += row.qualityScore;
			else if ("time".equals(key)) total += row.timeScore;
			else if ("service".equals(key)) total += row.serviceScore;
			else total += row.quantityScore;
		}
		return clampPercent(total / d.vendorRows.size());
	}

	private Map<String, TrendBucket> buildMonthlyTrendBuckets(VendorDashboardData d) {
		Map<String, TrendBucket> map = new LinkedHashMap<String, TrendBucket>();
		if (d == null || d.vendorRows == null) {
			return map;
		}
		for (VendorAnalysisRow row : d.vendorRows) {
			Date date = row.tanggalAkhir == null ? row.tanggalMulai : row.tanggalAkhir;
			if (date == null) {
				continue;
			}
			Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
			cal.setTime(date);
			String key = (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.YEAR);
			TrendBucket bucket = map.get(key);
			if (bucket == null) {
				bucket = new TrendBucket();
				map.put(key, bucket);
			}
			bucket.count++;
			bucket.nominal += row.nominalTransaksi;
		}
		return map;
	}

	private Date getMulai() {
		try {
			return dbMulai == null ? null : awalHari(dbMulai.getValue());
		} catch (Exception e) {
			return null;
		}
	}

	private Date getSampai() {
		try {
			return dbSampai == null ? null : akhirHari(dbSampai.getValue());
		} catch (Exception e) {
			return null;
		}
	}

	private PenyediaAsset getSelectedVendor() {
		try {
			if (cbVendor == null || cbVendor.getSelectedItem() == null || cbVendor.getSelectedItem().getValue() == null) {
				return null;
			}
			Object v = cbVendor.getSelectedItem().getValue();
			return v instanceof PenyediaAsset ? (PenyediaAsset) v : null;
		} catch (Exception e) {
			return null;
		}
	}

	private String getKeyword() {
		try {
			return txtKeyword == null || txtKeyword.getValue() == null ? "" : txtKeyword.getValue().trim();
		} catch (Exception e) {
			return "";
		}
	}

	private String getVendorName(PenyediaAsset vendor, String fallback) {
		try {
			if (vendor != null && vendor.getNama() != null && vendor.getNama().trim().length() > 0) {
				return vendor.getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:2712");
		}
		return fallback == null || fallback.trim().length() == 0 ? "Tanpa Vendor" : fallback;
	}

	private String getMasterAssetName(MasterAsset masterAsset) {
		try {
			if (masterAsset != null && masterAsset.getNama() != null && masterAsset.getNama().trim().length() > 0) {
				return masterAsset.getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:2722");
		}
		return "Produk / Jasa Tidak Diketahui";
	}

	private String getEntityName(Object entity, String fallback) {
		try {
			Object name = invokeAny(entity, new String[] { "getNama", "getName", "getKode" });
			if (name != null && String.valueOf(name).trim().length() > 0) {
				return String.valueOf(name);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardAnalisisVendor.java:2733");
		}
		return fallback == null ? "" : fallback;
	}

	private List<VendorAnalysisRow> filterVendorScore(List<VendorAnalysisRow> rows, double maxScoreExclusive) {
		List<VendorAnalysisRow> out = new ArrayList<VendorAnalysisRow>();
		if (rows == null) return out;
		for (VendorAnalysisRow row : rows) {
			if (row.finalScore < maxScoreExclusive) {
				out.add(row);
			}
		}
		return out;
	}

	private List<VendorAnalysisRow> filterVendorDocumentIssue(List<VendorAnalysisRow> rows) {
		List<VendorAnalysisRow> out = new ArrayList<VendorAnalysisRow>();
		if (rows == null) return out;
		for (VendorAnalysisRow row : rows) {
			if (row.totalDokumen > 0 && row.dokumenTerverifikasi < row.totalDokumen) {
				out.add(row);
			}
		}
		return out;
	}

	private double sumSaldoNilai(List<SaldoAwalMasterAsset> rows) {
		double total = 0.0;
		if (rows == null) return total;
		for (SaldoAwalMasterAsset s : rows) {
			total += safeDouble(s == null ? null : s.getNilai());
		}
		return total;
	}

	private double sumSaldoDibayar(List<SaldoAwalMasterAsset> rows) {
		double total = 0.0;
		if (rows == null) return total;
		for (SaldoAwalMasterAsset s : rows) {
			total += safeDouble(s == null ? null : s.getDibayar());
		}
		return total;
	}

	private double sumPoDetailNilai(List<PemesananPengadaanMasterAssetDetail> rows) {
		double total = 0.0;
		if (rows == null) return total;
		for (PemesananPengadaanMasterAssetDetail d : rows) {
			total += safeDouble(d == null ? null : d.getHargaTotal());
		}
		return total;
	}

	private double sumBastDetailQty(List<PenerimaanPengadaanMasterAssetDetail> rows) {
		double total = 0.0;
		if (rows == null) return total;
		for (PenerimaanPengadaanMasterAssetDetail d : rows) {
			total += safeDouble(d == null ? null : d.getDiterima());
		}
		return total;
	}

	private double sumBastDetailNilai(List<PenerimaanPengadaanMasterAssetDetail> rows) {
		double total = 0.0;
		if (rows == null) return total;
		for (PenerimaanPengadaanMasterAssetDetail d : rows) {
			total += safeDouble(d == null ? null : d.getHargaTotal());
		}
		return total;
	}

	private List<SaldoAwalMasterAsset> filterSaldoBelumLunas(List<SaldoAwalMasterAsset> rows) {
		List<SaldoAwalMasterAsset> out = new ArrayList<SaldoAwalMasterAsset>();
		if (rows == null) return out;
		for (SaldoAwalMasterAsset s : rows) {
			if (s != null && !Boolean.TRUE.equals(s.getLunas())) {
				out.add(s);
			}
		}
		return out;
	}

	private List<PermintaanPengadaanMasterAsset> filterPrApproved(List<PermintaanPengadaanMasterAsset> rows) {
		List<PermintaanPengadaanMasterAsset> out = new ArrayList<PermintaanPengadaanMasterAsset>();
		if (rows == null) return out;
		for (PermintaanPengadaanMasterAsset pr : rows) {
			if (pr != null && pr.getDisetujuiOleh() != null) {
				out.add(pr);
			}
		}
		return out;
	}

	private List<PermintaanPengadaanMasterAsset> filterPrRejected(List<PermintaanPengadaanMasterAsset> rows) {
		List<PermintaanPengadaanMasterAsset> out = new ArrayList<PermintaanPengadaanMasterAsset>();
		if (rows == null) return out;
		for (PermintaanPengadaanMasterAsset pr : rows) {
			if (pr != null && pr.getDitolakOleh() != null) {
				out.add(pr);
			}
		}
		return out;
	}

	private List<PermintaanPengadaanMasterAssetDetail> filterPrDetailUnrealized(List<PermintaanPengadaanMasterAssetDetail> rows) {
		List<PermintaanPengadaanMasterAssetDetail> out = new ArrayList<PermintaanPengadaanMasterAssetDetail>();
		if (rows == null) return out;
		for (PermintaanPengadaanMasterAssetDetail d : rows) {
			if (d != null && d.getUangMuka() == null && d.getPemesananPengadaanMasterAssetDetail() == null) {
				out.add(d);
			}
		}
		return out;
	}

	private List<PemesananPengadaanMasterAsset> filterPoPembelianLangsung(List<PemesananPengadaanMasterAsset> rows) {
		List<PemesananPengadaanMasterAsset> out = new ArrayList<PemesananPengadaanMasterAsset>();
		if (rows == null) return out;
		for (PemesananPengadaanMasterAsset po : rows) {
			if (po != null && Boolean.TRUE.equals(po.getPembelianLangsung())) {
				out.add(po);
			}
		}
		return out;
	}

	private List<PemesananPengadaanMasterAsset> filterPoApproved(List<PemesananPengadaanMasterAsset> rows) {
		List<PemesananPengadaanMasterAsset> out = new ArrayList<PemesananPengadaanMasterAsset>();
		if (rows == null) return out;
		for (PemesananPengadaanMasterAsset po : rows) {
			if (po != null && po.getDisetujuiOleh() != null) {
				out.add(po);
			}
		}
		return out;
	}

	private List<PenerimaanPengadaanMasterAsset> filterBastWithoutInvoice(List<PenerimaanPengadaanMasterAsset> rows) {
		List<PenerimaanPengadaanMasterAsset> out = new ArrayList<PenerimaanPengadaanMasterAsset>();
		if (rows == null) return out;
		for (PenerimaanPengadaanMasterAsset p : rows) {
			if (p != null && p.getSaldoAwalMasterAsset() == null) {
				out.add(p);
			}
		}
		return out;
	}

	private String getTopVendorText(VendorDashboardData d) {
		if (d == null || d.vendorRows == null || d.vendorRows.isEmpty()) {
			return "Belum ada vendor dominan pada filter ini.";
		}
		VendorAnalysisRow top = d.vendorRows.get(0);
		return top.namaVendor + " menjadi vendor terbesar dengan nominal " + formatCurrency(top.nominalTransaksi) + ".";
	}

	private String getTopProductText(VendorDashboardData d) {
		if (d == null || d.productRows == null || d.productRows.isEmpty()) {
			return "Belum ada produk dominan pada filter ini.";
		}
		ProductAnalysisRow top = d.productRows.get(0);
		return top.namaProduk + " dari " + top.namaVendor + " memiliki nominal terbesar " + formatCurrency(top.nominalTransaksi) + ".";
	}

	private String safe(String s) {
		return s == null ? "" : s.trim();
	}

	private double safeDouble(Double d) {
		return d == null ? 0.0 : d.doubleValue();
	}

	private double nvl(Double v, double fallback) {
		return v == null ? fallback : v.doubleValue();
	}

	private String formatDate(Date d) {
		try {
			return d == null ? "-" : Common.dateFormat.get().format(d);
		} catch (Exception e) {
			return "-";
		}
	}

	private String formatCurrency(double d) {
		try {
			return Common.numberFormat.get().format(d);
		} catch (Exception e) {
			return String.valueOf(d);
		}
	}

	private String formatCurrencyShort(double d) {
		if (Math.abs(d) >= 1000000000.0) {
			return formatNumber(d / 1000000000.0) + " M";
		}
		if (Math.abs(d) >= 1000000.0) {
			return formatNumber(d / 1000000.0) + " Jt";
		}
		return formatCurrency(d);
	}

	private String formatNumber(double d) {
		try {
			return Common.numberFormat.get().format(d);
		} catch (Exception e) {
			return String.valueOf(d);
		}
	}

	private String formatPercent(double d) {
		try {
			return Common.numberFormat.get().format(d) + "%";
		} catch (Exception e) {
			return String.valueOf(d) + "%";
		}
	}

	private String escapeHtml(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}

	private interface ExcelRowBuilder {
		Object[] build(Object obj) throws Exception;
	}

	private class RendererExcelRowBuilder implements ExcelRowBuilder {
		private InMemoryRowRenderer renderer;

		RendererExcelRowBuilder(InMemoryRowRenderer renderer) {
			this.renderer = renderer;
		}

		@Override
		public Object[] build(Object obj) throws Exception {
			if (renderer == null) {
				return new Object[0];
			}
			Row zkRow = new Row();
			renderer.render(zkRow, obj);
			List children = zkRow.getChildren();
			Object[] values = new Object[children.size()];
			for (int i = 0; i < children.size(); i++) {
				Object child = children.get(i);
				values[i] = child instanceof Component ? extractText((Component) child) : String.valueOf(child);
			}
			return values;
		}
	}

	private interface InMemoryRowRenderer {
		void render(Row row, Object obj) throws Exception;
	}

	private static class DashboardSession {
		Session session;
		boolean mustClose;
	}

	private static class TrendBucket {
		int count;
		double nominal;
	}

	private class DateRangeRow {
		Date tanggalMulai;
		Date tanggalAkhir;
	}

	private class VendorAnalysisRow extends DateRangeRow {
		Long vendorId;
		PenyediaAsset vendor;
		String namaVendor;
		double nominalTransaksi;
		double totalBiayaDibayar;
		double nilaiTagihan;
		double nilaiTagihanDibayar;
		double nilaiPr;
		double nilaiPoDetail;
		double nilaiBastDetail;
		double qtyDipesan;
		double qtyDatang;
		double qtyPo;
		double qtyBast;
		double quantityScore;
		double qualityScore;
		double timeScore;
		double serviceScore;
		double finalScore;
		String evaluation;
		int prCount;
		int poCount;
		int poDetailCount;
		int bastCount;
		int bastDetailCount;
		int tagihanCount;
		int tagihanDetailCount;
		int detailCount;
		int returCount;
		int produkKoperasiCount;
		int pembelianLangsungCount;
		int totalDokumen;
		int dokumenTerverifikasi;
		int dokumenBelum;
		int dokumenRevisi;
		Map<String, String> productNames = new LinkedHashMap<String, String>();
		List<Double> qualityScores = new ArrayList<Double>();
		List<Double> timeScores = new ArrayList<Double>();
		List<Double> serviceScores = new ArrayList<Double>();

		String getProdukRingkas() {
			if (productNames == null || productNames.isEmpty()) {
				return "-";
			}
			StringBuilder sb = new StringBuilder();
			int i = 0;
			for (String s : productNames.keySet()) {
				if (i > 0) sb.append(", ");
				if (i >= 4) {
					sb.append("+" + (productNames.size() - i) + " lainnya");
					break;
				}
				sb.append(s);
				i++;
			}
			return sb.toString();
		}
	}

	private class ProductAnalysisRow extends DateRangeRow {
		PenyediaAsset vendor;
		String namaVendor;
		String namaProduk;
		double qtyTransaksi;
		double nominalTransaksi;
		double qtyDipesan;
		double qtyDatang;
		double qtyPo;
		double qtyBast;
		double qtyTagihan;
		double nominalPo;
		double nominalBast;
		double nominalTagihan;
		int poCount;
		int bastCount;
		int tagihanCount;
		int jumlahTransaksi;
	}

	private class VendorDashboardData {
		Map<String, VendorAnalysisRow> vendorMap = new LinkedHashMap<String, VendorAnalysisRow>();
		Map<String, ProductAnalysisRow> productMap = new LinkedHashMap<String, ProductAnalysisRow>();
		List<VendorAnalysisRow> vendorRows = new ArrayList<VendorAnalysisRow>();
		List<ProductAnalysisRow> productRows = new ArrayList<ProductAnalysisRow>();
		List<PermintaanPengadaanMasterAsset> prRows = new ArrayList<PermintaanPengadaanMasterAsset>();
		List<PemesananPengadaanMasterAsset> poRows = new ArrayList<PemesananPengadaanMasterAsset>();
		List<PenerimaanPengadaanMasterAsset> penerimaanRows = new ArrayList<PenerimaanPengadaanMasterAsset>();
		List<SaldoAwalMasterAsset> saldoRows = new ArrayList<SaldoAwalMasterAsset>();
		List<SaldoAwalMasterAssetDetail> saldoDetailRows = new ArrayList<SaldoAwalMasterAssetDetail>();
		List<PemesananPengadaanMasterAssetDetail> poDetailRows = new ArrayList<PemesananPengadaanMasterAssetDetail>();
		List<PenerimaanPengadaanMasterAssetDetail> bastDetailRows = new ArrayList<PenerimaanPengadaanMasterAssetDetail>();
		List<PermintaanPengadaanMasterAssetDetail> prDetailRows = new ArrayList<PermintaanPengadaanMasterAssetDetail>();
		List<PengadaanProduk> pengadaanProdukRows = new ArrayList<PengadaanProduk>();
		List<ReturPengadaanMasterAsset> returRows = new ArrayList<ReturPengadaanMasterAsset>();
		double totalNominal;
		double totalDibayar;
		double totalQtyDipesan;
		double totalQtyDatang;
		int totalPo;
		int totalBast;
		int totalProduk;
		int totalRetur;
		int vendorNeedAttention;
		int vendorDokumenBelumLengkap;
	}

	private void appendPanelDescriptionEndUserV27(Panelchildren parent, String title) {
		if (parent == null) {
			return;
		}
		String desc = dashboardPanelDescriptionEndUserV27(title);
		if (desc == null || desc.trim().length() == 0) {
			return;
		}
		org.zkoss.zul.Html html = new org.zkoss.zul.Html("<div style=\"margin:0 0 12px 0; padding:10px 12px; "
				+ "border-radius:12px; background:#f8fafc; border:1px solid #e2e8f0; color:#475569; "
				+ "font-size:11.5px; line-height:1.55;\">"
				+ "<b style=\"color:#0f172a;\"></b> " + safeDashboardHtmlV27(desc) + "</div>");
		html.setParent(parent);
	}

	private String dashboardPanelDescriptionEndUserV27(String title) {
		if (title == null) {
			return "membantu menampilkan ringkasan data agar pengguna dapat memahami kondisi utama tanpa membaca seluruh tabel satu per satu.";
		}
		String t = title.toLowerCase(java.util.Locale.ENGLISH);
		if (t.indexOf("tren") >= 0 || t.indexOf("trend") >= 0 || t.indexOf("bulanan") >= 0 || t.indexOf("harian") >= 0) {
			return "memperlihatkan perubahan data dari waktu ke waktu, sehingga pengguna dapat melihat kapan aktivitas naik, turun, atau perlu perhatian.";
		}
		if (t.indexOf("alur") >= 0 || t.indexOf("pipeline") >= 0 || t.indexOf("progress") >= 0) {
			return "menunjukkan tahapan proses dari awal sampai akhir. Gunakan untuk mengetahui tahap mana yang paling banyak menunggu tindak lanjut.";
		}
		if (t.indexOf("risiko") >= 0 || t.indexOf("prioritas") >= 0 || t.indexOf("watchlist") >= 0 || t.indexOf("terlambat") >= 0) {
			return "menyoroti data yang perlu segera diperiksa. Daftar ini membantu pengguna menentukan pekerjaan mana yang paling penting diselesaikan lebih dulu.";
		}
		if (t.indexOf("komposisi") >= 0 || t.indexOf("distribusi") >= 0 || t.indexOf("sebaran") >= 0 || t.indexOf("kategori") >= 0) {
			return "membagi data berdasarkan kelompok atau kategori. Tujuannya agar pengguna dapat mengetahui bagian mana yang paling besar atau paling dominan.";
		}
		if (t.indexOf("top") >= 0 || t.indexOf("ranking") >= 0 || t.indexOf("peringkat") >= 0 || t.indexOf("terbesar") >= 0) {
			return "menampilkan urutan data terbesar atau paling sering muncul. Gunakan untuk melihat fokus utama yang membutuhkan perhatian atau evaluasi.";
		}
		if (t.indexOf("radar") >= 0 || t.indexOf("spider") >= 0 || t.indexOf("kesehatan") >= 0 || t.indexOf("health") >= 0) {
			return "merangkum beberapa indikator penting dalam satu tampilan sederhana. Semakin seimbang nilainya, semakin baik kondisi yang dipantau.";
		}
		if (t.indexOf("saldo") >= 0 || t.indexOf("kas") >= 0 || t.indexOf("keuangan") >= 0 || t.indexOf("nominal") >= 0 || t.indexOf("pembayaran") >= 0 || t.indexOf("piutang") >= 0) {
			return "membantu memantau nilai uang, pembayaran, atau kewajiban yang belum selesai agar keputusan keuangan dapat dilakukan lebih cepat.";
		}
		if (t.indexOf("aset") >= 0 || t.indexOf("inventaris") >= 0 || t.indexOf("barang") >= 0 || t.indexOf("pengadaan") >= 0) {
			return "membantu memantau kondisi aset, barang, atau proses pengadaan agar barang yang dibutuhkan dapat dilacak dengan lebih jelas.";
		}
		if (t.indexOf("pegawai") >= 0 || t.indexOf("kinerja") >= 0 || t.indexOf("lkp") >= 0) {
			return "membantu melihat aktivitas dan kinerja pegawai dalam bentuk ringkas sehingga pimpinan mudah memantau capaian kerja.";
		}
		if (t.indexOf("mahasiswa") >= 0 || t.indexOf("siswa") >= 0 || t.indexOf("akademik") >= 0) {
			return "membantu memantau data akademik dan aktivitas peserta didik agar perkembangan dan potensi masalah dapat terlihat lebih cepat.";
		}
		return "membantu menampilkan ringkasan data penting dalam bentuk yang mudah dibaca, sehingga pengguna dapat memahami kondisi utama dengan cepat.";
	}

	private String safeDashboardHtmlV27(String value) {
		if (value == null) {
			return "";
		}
		String s = value;
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&#39;");
		return s;
	}


}
