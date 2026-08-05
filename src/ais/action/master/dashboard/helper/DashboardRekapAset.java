package ais.action.master.dashboard.helper;

/*
 * ENHANCED VERSION - DEBUG + ANALITIK ASET/INVENTARIS
 * File download ini sengaja diberi nama berbeda agar tidak tertukar/cache.
 * Untuk dipasang menggantikan class lama, rename file ini kembali menjadi DashboardRekapAset.java
 * karena public class di bawah tetap bernama DashboardRekapAset.
 *
 * Marker perubahan:
 * - private static boolean debug = false;
 * - setDebug(boolean) dan isDebug()
 * - renderAnalitikAsetInventarisTambahan(...)
 * - chart aman dari label null/kosong
 */
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.AssetDetail;
import ais.database.model.asset.StatusAsset;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import org.zkoss.zul.Html;

public class DashboardRekapAset extends MyWindow {

	private static final long serialVersionUID = 790038368339375113L;

	/**
	 * Set true saat troubleshooting agar stacktrace dashboard tambahan tampil di log.
	 * Default false untuk mode produksi.
	 */
	private static boolean debug = false;

	private Combobox searchstatus = new Combobox();
	private Center center = new Center();

	private MyDatebox mulai;
	private MyDatebox sampai;

	public DashboardRekapAset() {
		super();
		try {
			init();
		}catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/helper/DashboardRekapAset.java:92");
		}
	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		// 1. FILTER UTAMA (Desain Grid Modern)
		Div filterContainer = new Div();
		filterContainer.setStyle(
				"padding: 15px; background: #ffffff; border-radius: 12px; border: 1px solid #e9ecef; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.02);");
		filterContainer.setParent(north);

		org.zkoss.zul.Grid filterGrid = new org.zkoss.zul.Grid();
		filterGrid.setSclass("fgrid");
		filterGrid.setStyle("border: none; background: transparent;");
		filterGrid.setParent(filterContainer);

		Rows filterRows = new Rows();
		filterRows.setParent(filterGrid);

		Row row1 = new Row();
		row1.setStyle("background: transparent; border: none;");
		row1.setParent(filterRows);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, 1);
		calendar.set(Calendar.MONTH, 0);
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 5);

		Hbox rangeBox = new Hbox();
		rangeBox.setStyle("align-items: center; gap: 8px;");
		rangeBox.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Beli Mulai:"));
		rangeBox.appendChild(mulai = new MyDatebox(calendar.getTime()));
		mulai.setReadonly(true);

		rangeBox.appendChild(new ais.ui.util.MyLabelConfig(" Sampai:"));
		rangeBox.appendChild(sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		sampai.setReadonly(true);
		row1.appendChild(rangeBox);

		Hbox statusBox = new Hbox();
		statusBox.setStyle("align-items: center; gap: 8px;");
		statusBox.appendChild(new ais.ui.util.MyLabelConfig("Status Aset:"));
		statusBox.appendChild(searchstatus);
		Common.insertComboDanSemua(searchstatus, "nama", StatusAsset.class);
		Common.selectComboItem(searchstatus, null);
		row1.appendChild(statusBox);

		Row row2 = new Row();
		row2.setStyle("background: transparent; border: none; padding-top: 10px;");
		row2.setParent(filterRows);

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("border: none; background: transparent;");
		toolbar.setParent(row2);

		MyToolbarbuttonConfig btnProses = new MyToolbarbuttonConfig("Tampilkan Dasbor Lengkap", "/img/search.gif");
		btnProses.setStyle(
				"font-weight: bold; background-color: #0d6efd; color: #ffffff; padding: 6px 16px; border-radius: 6px; margin-right: 10px;");
		btnProses.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadDashboard();
			}
		});
		btnProses.setParent(toolbar);

		MyToolbarbuttonConfig btnExcel = new MyToolbarbuttonConfig("Buka Excel Matriks (Popup)", "/img/print.png");
		btnExcel.setStyle(
				"font-weight: bold; background-color: #198754; color: #ffffff; padding: 6px 16px; border-radius: 6px;");
		btnExcel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showExcelPopup();
			}
		});
		btnExcel.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// Load dashboard secara default
		loadDashboard();
	}

	// Class struktur untuk menampung meta-data masing-masing panel
	class DashboardKategori {
		String title;
		String hqlGroupPath;

		public DashboardKategori(String t, String h) {
			this.title = t;
			this.hqlGroupPath = h;
		}
	}

	@SuppressWarnings("unchecked")
	private void loadDashboard() {
		Common.clear(center);

		MyPortallayout portalLayout = new MyPortallayout();
		portalLayout.setWidth("100%");
		portalLayout.setParent(center);

		String pcWidth = Common.isMobile() ? "100%" : "50%";

		// Kolom untuk Layout (1 Full Width, 2 Half Width)
		MyPortalchildren pcTop = new MyPortalchildren();
		pcTop.setWidth("100%");
		pcTop.setParent(portalLayout);

		MyPortalchildren pcLeft = new MyPortalchildren();
		pcLeft.setWidth(pcWidth);
		pcLeft.setStyle("padding: 5px;");
		pcLeft.setParent(portalLayout);

		MyPortalchildren pcRight = new MyPortalchildren();
		pcRight.setWidth(pcWidth);
		pcRight.setStyle("padding: 5px;");
		pcRight.setParent(portalLayout);

		// List Kategori yang akan dirender (Berdasarkan list initFakultas bawaan)
		List<DashboardKategori> listKategori = new ArrayList<DashboardKategori>();
		listKategori.add(new DashboardKategori("Jenis Aset", "masterAsset.jenisAsset"));
		listKategori.add(new DashboardKategori("Kelompok Aset", "masterAsset.kelompokAsset"));
		listKategori.add(new DashboardKategori("Penyedia Aset", "masterAsset.defaultPenyedia"));
		listKategori.add(new DashboardKategori("Pemilik Aset", "asset.pemilikAsset"));
		listKategori.add(new DashboardKategori("Lokasi Aset", "asset.lokasi"));
		listKategori.add(new DashboardKategori("Ruang Aset", "asset.ruang"));
		listKategori.add(new DashboardKategori("Pengadaan", "asset.saldoAwalMasterAssetDetail"));
		listKategori.add(new DashboardKategori("Permintaan", "asset.permintaanPengadaanMasterAssetDetail"));
		listKategori.add(new DashboardKategori("Satuan Kerja", "asset.satuanKerja"));

		Session session = HibernateUtil.currentSession();
		Date dMulai = mulai.getValue();
		Date dSampai = sampai.getValue();
		StatusAsset sts = (StatusAsset) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		// 1. RENDERING KARTU SUMMARY (TOP)
		Panel pnlSummary = new Panel();
		pnlSummary.setTitle("Overview Total Aset (" + Common.dateFormat.get().format(dMulai) + " s/d "
				+ Common.dateFormat.get().format(dSampai) + ")");
		pnlSummary.setBorder("normal");
		pnlSummary.setParent(pcTop);

		Panelchildren pchSummary = new Panelchildren();
		pchSummary.setStyle("padding: 20px; background: #fff; text-align: center;");
		pchSummary.setParent(pnlSummary);

		try {
			Criteria cTotal = session.createCriteria(AssetDetail.class, "ad")
					.add(Restrictions.between("tanggalBeli", dMulai, dSampai));
			if (sts != null)
				cTotal.add(Restrictions.eq("statusAsset", sts));

			Long totalAsetAll = (Long) cTotal.setProjection(Projections.rowCount()).uniqueResult();

			Div card = new Div();
			card.setStyle(
					"display: inline-block; padding: 20px 40px; background-color: #0d6efd; border-radius: 12px; color: #ffffff; box-shadow: 0 4px 6px rgba(13,110,253,0.3);");
			card.setParent(pchSummary);

			Label lblTitle = new Label(ais.common.Common.getBahasaConfig("TOTAL ASET TERFILTER"));
			lblTitle.setStyle("font-size: 14px; font-weight: 600; opacity: 0.9; display: block;");
			card.appendChild(lblTitle);

			Label lblVal = new Label(Common.numberFormat.get().format(totalAsetAll != null ? totalAsetAll : 0));
			lblVal.setStyle("font-size: 42px; font-weight: 700; display: block; margin-top: 5px;");
			card.appendChild(lblVal);

		} catch (Exception e) {
			debugException("Gagal render summary total aset", e);
		}

		// 2. RENDERING GRAFIK & GRID UNTUK MASING-MASING KATEGORI
		int layoutCounter = 0;

		for (DashboardKategori kategori : listKategori) {
			try {
				Criteria c = session.createCriteria(AssetDetail.class, "ad")
						.createAlias("ad.asset", "asset", Criteria.LEFT_JOIN)
						.createAlias("asset.masterAsset", "masterAsset", Criteria.LEFT_JOIN)
						.add(Restrictions.between("tanggalBeli", dMulai, dSampai));

				if (sts != null)
					c.add(Restrictions.eq("statusAsset", sts));

				c.setProjection(Projections.projectionList().add(Projections.groupProperty(kategori.hqlGroupPath))
						.add(Projections.rowCount()));

				List<Object[]> results = c.list();

				Map<String, Long> dataMap = new LinkedHashMap<String, Long>();
				for (Object[] row : results) {
					String labelName = "Tidak Diketahui / Kosong";
					Object entity = row[0];
					if (entity != null) {
						if (entity instanceof GeneralValueObject) {
							labelName = ((GeneralValueObject) entity).getNama();
						} else {
							try {
								Object nama = entity.getClass().getMethod("getNama").invoke(entity);
								labelName = nama == null ? null : String.valueOf(nama);
							} catch (Exception e) {
								labelName = entity.toString();
							}
						}
					}

					Long count = row[1] == null ? Long.valueOf(0L) : (Long) row[1];
					putDashboardData(dataMap, labelName, count);
				}

				// Tentukan posisi (Kiri / Kanan bergantian)
				MyPortalchildren targetParent = (layoutCounter % 2 == 0) ? pcLeft : pcRight;

				// Panel Chart
				Panel pnlChart = new Panel();
				pnlChart.setTitle("Grafik Berdasarkan " + kategori.title);
				pnlChart.setBorder("normal");
				pnlChart.setParent(targetParent);

				Panelchildren pchChart = new Panelchildren();
				pchChart.setStyle("text-align:center; padding:15px; background:#fff;");
				pchChart.setParent(pnlChart);

				pchChart.appendChild(new Html(buildModernChartHtmlFromMap("Grafik Berdasarkan " + kategori.title,
						dataMap,
						"Menampilkan komposisi aset berdasarkan kategori terpilih. Grafik ini membantu pengguna melihat kelompok aset paling banyak tanpa menghitung tabel satu per satu.")));

				// Panel Grid (Tabel)
				Panel pnlGrid = new Panel();
				pnlGrid.setTitle("Rincian Tabel " + kategori.title);
				pnlGrid.setBorder("normal");
				pnlGrid.setStyle("margin-bottom: 20px;");
				pnlGrid.setParent(targetParent);

				Panelchildren pchGrid = new Panelchildren();
				pchGrid.setStyle("padding: 10px; background: #fff;");
				pchGrid.setParent(pnlGrid);

				MyGrid gridData = new MyGrid();
				gridData.setMold("paging");
				gridData.setPageSize(5);
				gridData.setSclass("dgrid fgrid table-striped");
				gridData.setWidth("100%");
				gridData.setParent(pchGrid);

				org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
				cols.setParent(gridData);
				new ais.ui.util.MyColumnConfig(kategori.title).setParent(cols);
				new ais.ui.util.MyColumnConfig("Jumlah Aset").setParent(cols);

				org.zkoss.zul.Rows gridRows = new org.zkoss.zul.Rows();
				gridRows.setParent(gridData);

				for (Map.Entry<String, Long> entry : dataMap.entrySet()) {
					Row r = new Row();
					r.setParent(gridRows);
					r.appendChild(new Label(entry.getKey()));

					Label lblCount = new Label(Common.numberFormat.get().format(entry.getValue()) + " Item");
					lblCount.setStyle("font-weight: bold; color: #198754;");
					r.appendChild(lblCount);
				}

				layoutCounter++;
			} catch (Exception e) {
				debugException("Gagal render dashboard kategori " + kategori.title, e);
			}
		}

		renderAnalitikAsetInventarisTambahan(portalLayout, session, dMulai, dSampai, sts);
	}


	public static void setDebug(boolean debugMode) {
		debug = debugMode;
	}

	public static boolean isDebug() {
		return debug;
	}

	private void debugException(String context, Exception e) {
		if (debug) {

			if (e != null) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/helper/DashboardRekapAset.java:396");
			}
		}
	}

	/**
	 * Dashboard tambahan di bawah dashboard kategori existing.
	 * Tujuannya untuk audit kelengkapan inventaris dan prioritas area aset terbanyak.
	 */
	private void renderAnalitikAsetInventarisTambahan(MyPortallayout portalLayout, Session session, Date dMulai,
			Date dSampai, StatusAsset sts) {
		try {
			renderAnalitikAsetHeader(portalLayout);

			MyPortalchildren pcLeftBottom = createAnalitikPortalchildren(portalLayout, Common.isMobile() ? "100%" : "50%");
			MyPortalchildren pcRightBottom = createAnalitikPortalchildren(portalLayout, Common.isMobile() ? "100%" : "50%");

			renderKesehatanDataAset(pcLeftBottom, session, dMulai, dSampai, sts);
			renderStatusAsetRingkas(pcRightBottom, session, dMulai, dSampai, sts);
			renderGroupedAsetPanel(pcLeftBottom, session, dMulai, dSampai, sts, "Top Lokasi Aset",
					"Lokasi dengan jumlah aset terbanyak untuk prioritas stock opname dan kontrol fisik.", "asset.lokasi",
					"Lokasi");
			renderGroupedAsetPanel(pcRightBottom, session, dMulai, dSampai, sts, "Top Ruang Aset",
					"Ruang dengan konsentrasi aset tertinggi untuk prioritas inventarisasi.", "asset.ruang", "Ruang");
			renderGroupedAsetPanel(pcLeftBottom, session, dMulai, dSampai, sts, "Top Penyedia Aset",
					"Penyedia dengan kontribusi aset terbanyak sebagai bahan evaluasi vendor.", "masterAsset.defaultPenyedia",
					"Penyedia");
			renderGroupedAsetPanel(pcRightBottom, session, dMulai, dSampai, sts, "Top Master Aset",
					"Master aset paling dominan dalam periode filter.", "asset.masterAsset", "Master Aset");
			renderGroupedAsetPanel(pcLeftBottom, session, dMulai, dSampai, sts, "Top Satuan Kerja Pemakai",
					"Unit kerja dengan jumlah aset terbanyak untuk monitoring distribusi inventaris.", "asset.satuanKerja",
					"Satuan Kerja");
			renderGroupedAsetPanel(pcRightBottom, session, dMulai, dSampai, sts, "Top Jenis Aset",
					"Jenis aset paling banyak untuk membaca komposisi inventaris.", "masterAsset.jenisAsset", "Jenis Aset");
		} catch (Exception e) {
			debugException("Gagal render dashboard analitik aset dan inventaris", e);
		}
	}

	private void renderAnalitikAsetHeader(MyPortallayout portalLayout) {
		MyPortalchildren pc = createAnalitikPortalchildren(portalLayout, "100%");
		Panel panel = createAnalitikPanel(pc, "Dasbor Analitik Manajemen Aset & Inventaris",
				"Dashboard tambahan untuk audit kelengkapan data, konsentrasi aset, prioritas stock opname, dan monitoring distribusi inventaris.");
		Panelchildren body = (Panelchildren) panel.getFirstChild();

		Hbox row = new Hbox();
		row.setStyle("width:100%; gap:12px; flex-wrap:wrap; align-items:stretch;");
		row.setParent(body);

		createMiniInfoCard(row, "Fokus Analisis", "Audit, Lokasi, Vendor", "#0d6efd");
		createMiniInfoCard(row, "Penempatan", "Di bawah dashboard kategori", "#198754");
		createMiniInfoCard(row, "Mode Debug", debug ? "AKTIF" : "NONAKTIF", debug ? "#dc3545" : "#6c757d");
	}

	private void renderKesehatanDataAset(MyPortalchildren parent, Session session, Date dMulai, Date dSampai, StatusAsset sts) {
		try {
			long total = countCriteria(criteriaAssetBase(session, dMulai, dSampai, sts));
			List<String[]> rows = new ArrayList<String[]>();
			rows.add(new String[] { "Total aset terfilter", formatLong(total), "Baseline audit" });
			rows.add(new String[] { "Status aset kosong", formatLong(countCriteria(criteriaAssetBase(session, dMulai, dSampai, sts)
					.add(Restrictions.isNull("statusAsset")))), "Lengkapi agar filter status valid" });
			rows.add(new String[] { "Master aset kosong", formatLong(countCriteria(criteriaAssetBase(session, dMulai, dSampai, sts)
					.add(Restrictions.isNull("asset.masterAsset")))), "Lengkapi mapping master aset" });
			rows.add(new String[] { "Jenis aset kosong", formatLong(countCriteria(criteriaAssetBase(session, dMulai, dSampai, sts)
					.add(Restrictions.isNull("masterAsset.jenisAsset")))), "Penting untuk klasifikasi laporan" });
			rows.add(new String[] { "Kelompok aset kosong", formatLong(countCriteria(criteriaAssetBase(session, dMulai, dSampai, sts)
					.add(Restrictions.isNull("masterAsset.kelompokAsset")))), "Penting untuk pengelompokan inventaris" });
			rows.add(new String[] { "Lokasi aset kosong", formatLong(countCriteria(criteriaAssetBase(session, dMulai, dSampai, sts)
					.add(Restrictions.isNull("asset.lokasi")))), "Prioritas validasi fisik" });
			rows.add(new String[] { "Ruang aset kosong", formatLong(countCriteria(criteriaAssetBase(session, dMulai, dSampai, sts)
					.add(Restrictions.isNull("asset.ruang")))), "Prioritas stock opname detail" });
			rows.add(new String[] { "Satuan kerja kosong", formatLong(countCriteria(criteriaAssetBase(session, dMulai, dSampai, sts)
					.add(Restrictions.isNull("asset.satuanKerja")))), "Validasi penanggung jawab unit" });

			renderSimpleTablePanel(parent, "Kesehatan & Kelengkapan Data Aset",
					"Audit cepat field penting yang sering menyebabkan rekap aset kurang akurat.",
					new String[] { "Indikator", "Jumlah", "Catatan" }, rows, 8);
		} catch (Exception e) {
			debugException("Gagal render Kesehatan Data Aset", e);
		}
	}

	@SuppressWarnings("unchecked")
	private void renderStatusAsetRingkas(MyPortalchildren parent, Session session, Date dMulai, Date dSampai, StatusAsset sts) {
		try {
			Criteria c = criteriaAssetBase(session, dMulai, dSampai, sts);
			c.setProjection(Projections.projectionList().add(Projections.groupProperty("statusAsset"))
					.add(Projections.rowCount(), "jumlah"));
			c.addOrder(Order.desc("jumlah"));
			List<Object[]> results = c.list();

			List<String[]> rows = new ArrayList<String[]>();
			for (Object[] row : results) {
				rows.add(new String[] { labelFromEntity(row[0]), formatLong(toLong(row[1])), "Status inventaris" });
			}
			renderSimpleTablePanel(parent, "Komposisi Status Aset",
					"Ringkasan status aset untuk memantau kondisi inventaris berdasarkan filter aktif.",
					new String[] { "Status", "Jumlah", "Keterangan" }, rows, 8);
		} catch (Exception e) {
			debugException("Gagal render Komposisi Status Aset", e);
		}
	}

	@SuppressWarnings("unchecked")
	private void renderGroupedAsetPanel(MyPortalchildren parent, Session session, Date dMulai, Date dSampai, StatusAsset sts,
			String title, String subtitle, String groupPath, String labelHeader) {
		try {
			Criteria c = criteriaAssetBase(session, dMulai, dSampai, sts);
			c.setProjection(Projections.projectionList().add(Projections.groupProperty(groupPath))
					.add(Projections.rowCount(), "jumlah"));
			c.addOrder(Order.desc("jumlah"));
			c.setMaxResults(10);
			List<Object[]> results = c.list();

			List<String[]> rows = new ArrayList<String[]>();
			for (Object[] row : results) {
				rows.add(new String[] { labelFromEntity(row[0]), formatLong(toLong(row[1])), buildPercentLabel(toLong(row[1]),
						countCriteria(criteriaAssetBase(session, dMulai, dSampai, sts))) });
			}
			renderSimpleTablePanel(parent, title, subtitle, new String[] { labelHeader, "Jumlah", "Porsi" }, rows, 10);
		} catch (Exception e) {
			debugException("Gagal render grouped aset: " + title, e);
		}
	}

	private Criteria criteriaAssetBase(Session session, Date dMulai, Date dSampai, StatusAsset sts) {
		Criteria c = session.createCriteria(AssetDetail.class, "ad")
				.createAlias("ad.asset", "asset", Criteria.LEFT_JOIN)
				.createAlias("asset.masterAsset", "masterAsset", Criteria.LEFT_JOIN)
				.add(Restrictions.between("tanggalBeli", dMulai, dSampai));
		if (sts != null) {
			c.add(Restrictions.eq("statusAsset", sts));
		}
		return c;
	}

	private long countCriteria(Criteria criteria) {
		Object result = criteria.setProjection(Projections.rowCount()).uniqueResult();
		return toLong(result);
	}

	private MyPortalchildren createAnalitikPortalchildren(MyPortallayout portalLayout, String width) {
		MyPortalchildren pc = new MyPortalchildren();
		pc.setWidth(width);
		pc.setStyle("padding:6px; box-sizing:border-box;");
		pc.setParent(portalLayout);
		return pc;
	}

	private Panel createAnalitikPanel(MyPortalchildren parent, String title, String subtitle) {
		Panel panel = new Panel();
		panel.setTitle(title);
		panel.setBorder("normal");
		panel.setStyle("margin:8px 3px 14px 3px; border-radius:14px; box-shadow:0 8px 24px rgba(15,23,42,0.08);"
				+ "overflow:hidden; background:#ffffff;");
		panel.setParent(parent);

		Panelchildren body = new Panelchildren();
		body.setStyle("padding:14px; background:linear-gradient(180deg,#ffffff 0%,#f8fafc 100%);");
		body.setParent(panel);

		if (subtitle != null && subtitle.trim().length() > 0) {
			Label desc = new Label(subtitle);
			desc.setStyle("display:block; margin-bottom:12px; color:#64748b; font-size:12px; line-height:18px;");
			desc.setParent(body);
		}
		return panel;
	}

	private void renderSimpleTablePanel(MyPortalchildren parent, String title, String subtitle, String[] headers,
			List<String[]> rows, int pageSize) {
		Panel panel = createAnalitikPanel(parent, title, subtitle);
		Panelchildren body = (Panelchildren) panel.getFirstChild();

		MyGrid grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(pageSize <= 0 ? 5 : pageSize);
		grid.setSclass("dgrid fgrid table-striped");
		grid.setWidth("100%");
		grid.setStyle("border:0; background:transparent; min-height:120px;");
		grid.setParent(body);

		org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
		cols.setParent(grid);
		for (int i = 0; i < headers.length; i++) {
			new ais.ui.util.MyColumnConfig(headers[i]).setParent(cols);
		}

		org.zkoss.zul.Rows gridRows = new org.zkoss.zul.Rows();
		gridRows.setParent(grid);
		if (rows == null || rows.isEmpty()) {
			Row r = new Row();
			r.setParent(gridRows);
			r.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada data")));
			for (int i = 1; i < headers.length; i++) {
				r.appendChild(new Label("-"));
			}
			return;
		}

		for (String[] dataRow : rows) {
			Row r = new Row();
			r.setParent(gridRows);
			for (int i = 0; i < headers.length; i++) {
				Label label = new Label(dataRow != null && i < dataRow.length ? safeString(dataRow[i]) : "-");
				if (i > 0) {
					label.setStyle("font-weight:600; color:#334155;");
				}
				r.appendChild(label);
			}
		}
	}

	private void createMiniInfoCard(Hbox parent, String title, String value, String color) {
		Div card = new Div();
		card.setStyle("display:inline-block; min-width:210px; flex:1; padding:14px 16px; border-radius:14px;"
				+ "background:#ffffff; border:1px solid #e5e7eb; box-shadow:0 6px 18px rgba(15,23,42,0.07);");
		card.setParent(parent);

		Label lblTitle = new Label(title);
		lblTitle.setStyle("font-size:11px; color:#64748b; text-transform:uppercase; letter-spacing:.4px; font-weight:700; display:block;");
		lblTitle.setParent(card);

		Label lblValue = new Label(value);
		lblValue.setStyle("font-size:18px; color:" + color + "; font-weight:800; margin-top:4px; display:block;");
		lblValue.setParent(card);
	}

	private String buildPercentLabel(long value, long total) {
		if (total <= 0) {
			return "0%";
		}
		return Common.numberFormat.get().format((value * 100.0) / total) + "%";
	}

	private String safeString(Object value) {
		if (value == null) {
			return "-";
		}
		String text = String.valueOf(value).trim();
		return text.length() == 0 || "null".equalsIgnoreCase(text) ? "-" : text;
	}

	private String formatLong(long value) {
		return Common.numberFormat.get().format(value);
	}

	private long toLong(Object value) {
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		return 0L;
	}

	private String labelFromEntity(Object entity) {
		if (entity == null) {
			return "Tidak Diketahui / Kosong";
		}
		try {
			if (entity instanceof GeneralValueObject) {
				return normalizeDashboardLabel(((GeneralValueObject) entity).getNama());
			}
			Object nama = entity.getClass().getMethod("getNama").invoke(entity);
			return normalizeDashboardLabel(nama);
		} catch (Exception e) {
			return normalizeDashboardLabel(entity);
		}
	}


	private void putDashboardData(Map<String, Long> dataMap, String rawLabel, Long count) {
		if (dataMap == null) {
			return;
		}

		String label = normalizeDashboardLabel(rawLabel);
		Long safeCount = count == null ? Long.valueOf(0L) : count;
		Long currentCount = dataMap.get(label);
		dataMap.put(label, Long.valueOf((currentCount == null ? 0L : currentCount.longValue()) + safeCount.longValue()));
	}

	private String normalizeDashboardLabel(Object rawLabel) {
		if (rawLabel == null) {
			return "Tidak Diketahui / Kosong";
		}

		String label = String.valueOf(rawLabel).trim();
		if (label.length() == 0 || "null".equalsIgnoreCase(label)) {
			return "Tidak Diketahui / Kosong";
		}

		return label;
	}

	// =========================================================================
	// METODE POPUP WINDOW UNTUK SPREADSHEET MATRIKS (Excel Lama)
	// =========================================================================
	private void showExcelPopup() {
		final Window winExcel = new Window("Rekap Matriks Spreadsheet Aset", "normal", true);
		winExcel.setWidth("95%");
		winExcel.setHeight("90%");
		winExcel.setPosition("center");
		winExcel.setClosable(true);
		winExcel.setParent(this);

		Borderlayout bl = new Borderlayout();
		bl.setParent(winExcel);

		North n = new North();
		n.setParent(bl);

		// Lokalisasi Combobox Pilihan Pivot Matriks
		final Combobox cbPivot = new Combobox();
		cbPivot.setReadonly(true);

		Comboitem comboitem = new Comboitem("Jenis Aset");
		comboitem.setValue("x.jenis_asset");
		cbPivot.appendChild(comboitem);
		comboitem = new Comboitem("Kelompok Aset");
		comboitem.setValue("x.kelompok_asset");
		cbPivot.appendChild(comboitem);
		comboitem = new Comboitem("Penyedia Aset");
		comboitem.setValue("x.default_penyedia");
		cbPivot.appendChild(comboitem);
		comboitem = new Comboitem("Pemilik Aset");
		comboitem.setValue("m.pemilik_asset");
		cbPivot.appendChild(comboitem);
		comboitem = new Comboitem("Lokasi");
		comboitem.setValue("m.lokasi");
		cbPivot.appendChild(comboitem);
		comboitem = new Comboitem("Ruang");
		comboitem.setValue("m.ruang");
		cbPivot.appendChild(comboitem);
		comboitem = new Comboitem("Pengadaan");
		comboitem.setValue("m.saldo_awal_master_asset_detail");
		cbPivot.appendChild(comboitem);
		comboitem = new Comboitem("Permintaan");
		comboitem.setValue("m.permintaan_pengadaan_master_asset_detail");
		cbPivot.appendChild(comboitem);
		comboitem = new Comboitem("Satuan Kerja");
		comboitem.setValue("m.satuan_kerja");
		cbPivot.appendChild(comboitem);
		cbPivot.setSelectedIndex(0);

		Toolbar tbar = new Toolbar();
		tbar.setStyle("padding: 10px;");
		tbar.setParent(n);

		tbar.appendChild(new ais.ui.util.MyLabelConfig("Pilih Dasar Matriks: "));
		tbar.appendChild(cbPivot);

		final Center cExcel = new Center();
		cExcel.setParent(bl);

		MyToolbarbuttonConfig btnProsesExcel = new MyToolbarbuttonConfig("Generate Matrix", "/img/print.png");
		btnProsesExcel.setStyle(
				"margin-left: 10px; background-color: #0dcaf0; color: #000; font-weight: bold; border-radius: 4px;");
		btnProsesExcel.setParent(tbar);

		final Spreadsheet ss = new ais.ui.util.MySpreadsheet();

		MyToolbarbuttonConfig btnDownload = new MyToolbarbuttonConfig("Download Excel File", "/img/print.png");
		btnDownload.setStyle(
				"margin-left: 10px; background-color: #198754; color: #fff; font-weight: bold; border-radius: 4px;");
		btnDownload.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (ss.getBook() != null) {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					ss.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"Rekap_Matriks_Inventaris.xlsx");
				}
			}
		});
		btnDownload.setParent(tbar);

		// Event Listener Proses Matriks Excel
		btnProsesExcel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				generateMatriksSpreadsheet(cbPivot, ss, cExcel);
			}
		});

		// Trigger eksekusi pertama kali saat popup dibuka
		try {
			generateMatriksSpreadsheet(cbPivot, ss, cExcel);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/helper/DashboardRekapAset.java:787");
		}

		try {
			winExcel.doModal();
		} catch (SuspendNotAllowedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/helper/DashboardRekapAset.java:794");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/helper/DashboardRekapAset.java:797");
		}
	}

	private static Map<String, String> mapping = new HashMap<String, String>();
	static {
		mapping.put("m.pemilik_asset", "asset.pemilikAsset");
		mapping.put("x.jenis_asset", "masterAsset.jenisAsset");
		mapping.put("x.kelompok_asset", "masterAsset.kelompokAsset");
		mapping.put("x.default_penyedia", "masterAsset.defaultPenyedia");
		mapping.put("m.lokasi", "asset.lokasi");
		mapping.put("m.ruang", "asset.ruang");
		mapping.put("m.cara_pengadaan_asset", "asset.saldoAwalMasterAssetDetail");
		mapping.put("m.saldo_awal_master_asset_detail", "asset.permintaanPengadaanMasterAssetDetail");
		mapping.put("m.satuan_kerja", "asset.satuanKerja");
		
		
		
	}

	@SuppressWarnings("unchecked")
	private void generateMatriksSpreadsheet(Combobox cbPivot, final Spreadsheet spreadsheet, final Center centerPopup) {
		Common.clear(centerPopup);

		final String namaKolom = (String) (cbPivot.getSelectedItem() == null ? null
				: cbPivot.getSelectedItem().getValue());
		if (namaKolom == null)
			return;

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				String kolomGroup = mapping.get(namaKolom);

				StatusAsset sts = (StatusAsset) (searchstatus.getSelectedItem() == null
						|| searchstatus.getSelectedItem().getValue() == null ? null
								: searchstatus.getSelectedItem().getValue());

				Session session = HibernateUtil.currentSession();
				List<Object[]> jurusans = new ArrayList<Object[]>();

				List<StatusAsset> statusAssets = session.createCriteria(StatusAsset.class)
						.add(sts == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", sts.getId()))
						.addOrder(Order.asc("nama")).list();

				List<GeneralValueObject> generalValueObjects = session.createCriteria(AssetDetail.class)
						.createAlias("asset", "asset").createAlias("asset.masterAsset", "masterAsset")
						.setProjection(Projections.groupProperty(kolomGroup)).add(Restrictions.isNotNull(kolomGroup))
						.add(Restrictions.between("tanggalBeli", mulai.getValue(), sampai.getValue())).list();

				Collections.sort(generalValueObjects);

				String sql = "select x.nama as master_asset, ";

				for (GeneralValueObject generalValueObject : generalValueObjects) {
					if (generalValueObject.getNama() != null && !generalValueObject.getNama().isEmpty()) {
						for (StatusAsset statusAsset : statusAssets) {
							sql += "sum(case when " + namaKolom + " = " + generalValueObject.getId()
									+ " and aaa.status_asset=" + statusAsset.getId() + " then 1 else 0 end) as \""
									+ statusAsset.getNama() + " " + generalValueObject.getNama().trim() + "\", ";
						}
						sql += "sum(case when " + namaKolom + " = " + generalValueObject.getId()
								+ " then 1 else 0 end) as \"" + generalValueObject.getNama().trim() + "\", ";
					}
				}

				for (StatusAsset statusAsset : statusAssets) {
					sql += "sum(case when aaa.status_asset=" + statusAsset.getId() + " then 1 else 0 end) as \""
							+ statusAsset.getNama() + "\", ";
				}

				sql += " sum(case when " + namaKolom + " is not null then 1 else 0 end) as total "
						+ " from asset.asset_detail aaa   inner join asset.asset m on (aaa.asset = m.id  )    "
						+ " inner join asset.master_asset x on (m.master_asset = x.id  )     where 1=1 "
						+ (sts == null ? "" : " and aaa.status_asset=" + sts.getId() + " ")
						+ " and aaa.tanggalbeli between date('"
						+ Common.databaseDateFormat.get().format(mulai.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(sampai.getValue())
						+ "') group by x.id order by x.nama ";

				jurusans = Common.ambilSql(sql);

				spreadsheet.setParent(centerPopup);
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
				spreadsheet.setMaxcolumns(
						(generalValueObjects.size() * (statusAssets.size() + 1)) + statusAssets.size() + 2);
				spreadsheet.setMaxrows(jurusans.size() + 25);

				Worksheet sheet = spreadsheet.getSelectedSheet();
				sheet.setDefaultColumnWidth(40);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

				ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
						"REKAPITULASI INVENTARIS MATRIKS\nPer Tanggal "
								+ Common.dateFormat1.get().format(mulai.getValue()) + " s.d "
								+ Common.dateFormat1.get().format(sampai.getValue()));
				final String color = "#000000";
				int rowIndex = 2;

				Utils.setRowHeight(sheet, 1, 150);
				ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
				Cell cell = Utils.getCell(sheet, 1, 0);
				cell.getCellStyle().setWrapText(true);
				cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

				ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Aset");
				Utils.setColumnWidth(sheet, 0, 200);

				int colIndex = 1;
				for (GeneralValueObject generalValueObject : generalValueObjects) {
					int i = 0;
					for (StatusAsset statusAsset : statusAssets) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + i,
								generalValueObject.getNama());
						Utils.setColumnWidth(sheet, colIndex + i, 70);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + i, statusAsset.getNama());
						i++;
					}

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + i, generalValueObject.getNama());
					Utils.setColumnWidth(sheet, colIndex + i, 70);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + i, "Total");

					ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, colIndex, rowIndex,
							colIndex + statusAssets.size(), false);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + statusAssets.size(), "Total");

					colIndex += statusAssets.size() + 1;
				}

				Map<Long, Integer[]> totals = new HashMap<Long, Integer[]>();
				int i = 0;
				for (StatusAsset statusAsset : statusAssets) {
					Integer[] nilais = new Integer[generalValueObjects.size()];
					totals.put(statusAsset.getId(), nilais);

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + i, "Total");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + i, statusAsset.getNama());
					Utils.setColumnWidth(sheet, colIndex + i, 70);
					i++;
				}

				totals.put(-1L, new Integer[generalValueObjects.size()]);

				ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, colIndex, rowIndex, colIndex + statusAssets.size(),
						false);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + statusAssets.size(), "Total");

				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
				ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
						true);

				rowIndex = 4;
				colIndex = 0;

				String namaFakultas = "";
				Integer[] nilais = new Integer[generalValueObjects.size()];
				Integer[] nilaisTotalSemua = new Integer[statusAssets.size() + 1];

				for (Object[] objects : jurusans) {
					if (objects[0] != null) {
						if (!namaFakultas.equals(objects[0].toString())) {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0].toString());
							namaFakultas = objects[0].toString();
						} else {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "");
						}

					} else {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Tidak ada");
					}

					colIndex = 1;
					int index = 0;
					for (@SuppressWarnings("unused")
					GeneralValueObject generalValueObject : generalValueObjects) {
						for (StatusAsset statusAsset : statusAssets) {
							Integer[] nilaisTotal = totals.get(statusAsset.getId());
							if (nilaisTotal[index] == null) {
								nilaisTotal[index] = 0;
							}
							Integer nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
							nilaisTotal[index] += nilai0;
							colIndex++;
						}

						if (nilais[index] == null) {
							nilais[index] = 0;
						}
						Integer nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);

						Integer[] nilaisTotal = totals.get(-1L);
						if (nilaisTotal[index] == null) {
							nilaisTotal[index] = 0;
						}
						nilaisTotal[index] += nilai0;
						nilais[index] += nilai0;
						colIndex++;

						index++;
					}

					int y = 0;
					for (@SuppressWarnings("unused")
					StatusAsset statusAsset : statusAssets) {
						if (nilaisTotalSemua[y] == null) {
							nilaisTotalSemua[y] = 0;
						}
						Integer nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
						nilaisTotalSemua[y] += nilai0;
						colIndex++;
						y++;
					}

					if (nilaisTotalSemua[y] == null) {
						nilaisTotalSemua[y] = 0;
					}
					Integer nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
					nilaisTotalSemua[y] += nilai0;

					try {
						ais.ui.util.EcampusUtil.setBorder(sheet,
								new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapAset.java:1032");
					}

					rowIndex++;
				}

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
				colIndex = 1;
				for (i = 0; i < nilais.length; i++) {
					for (StatusAsset statusAsset : statusAssets) {
						Integer[] nilaisTotal = totals.get(statusAsset.getId());

						int jum = nilaisTotal[i];
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
						colIndex++;
					}

					int jum = nilais[i];
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
					colIndex++;
				}

				for (i = 0; i < nilaisTotalSemua.length; i++) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilaisTotalSemua[i]);
					colIndex++;
				}

				try {
					ais.ui.util.EcampusUtil.setBorder(sheet,
							new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
							BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapAset.java:1065");
				}

				Common.setStyled(sheet);
				spreadsheet.setMaxrows(rowIndex + 1);
				// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

				try {
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex),
							true);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapAset.java:1077");
				}

			}
		});
	}

	private static class HtmlCategoryModel {
		private List<HtmlCategoryRow> rows = new ArrayList<HtmlCategoryRow>();

		public void clear() {
			rows.clear();
		}

		public void setValue(String series, Object category, Object value) {
			HtmlCategoryRow row = new HtmlCategoryRow();
			row.series = series == null ? "" : series;
			row.category = category == null ? "" : String.valueOf(category);
			row.value = toDoubleDashboardValue(value);
			rows.add(row);
		}

		public List<HtmlCategoryRow> getRows() {
			return rows;
		}
	}

	private static class HtmlCategoryRow {
		String series;
		String category;
		double value;
	}

	private String buildModernChartHtml(String title, HtmlCategoryModel model, String description) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='width:100%;box-sizing:border-box;padding:14px;border:1px solid #e2e8f0;border-radius:16px;background:#ffffff;box-shadow:0 8px 18px rgba(15,23,42,.06);'>");
		sb.append("<div style='font-size:14px;font-weight:900;color:#0f172a;margin-bottom:6px;'>").append(escapeDashboardHtml(title)).append("</div>");
		if (description != null && description.trim().length() > 0) {
			sb.append("<div style='font-size:11px;color:#64748b;line-height:1.55;margin-bottom:10px;'>").append(escapeDashboardHtml(description)).append("</div>");
		}
		if (model == null || model.getRows() == null || model.getRows().isEmpty()) {
			sb.append("<div style='padding:12px;border-radius:12px;background:#f8fafc;color:#64748b;font-size:12px;'>Belum ada data yang dapat ditampilkan.</div></div>");
			return sb.toString();
		}
		double max = 0.0d;
		for (int i = 0; i < model.getRows().size(); i++) {
			HtmlCategoryRow r = (HtmlCategoryRow) model.getRows().get(i);
			if (r != null && r.value > max) {
				max = r.value;
			}
		}
		if (max <= 0.0d) {
			max = 1.0d;
		}
		sb.append("<div style='display:flex;flex-direction:column;gap:7px;'>");
		for (int i = 0; i < model.getRows().size(); i++) {
			HtmlCategoryRow r = (HtmlCategoryRow) model.getRows().get(i);
			if (r == null) {
				continue;
			}
			int width = (int) Math.round((r.value * 100.0d) / max);
			if (width < 2 && r.value > 0.0d) {
				width = 2;
			}
			sb.append("<div style='display:grid;grid-template-columns:minmax(95px,210px) 1fr minmax(70px,120px);gap:8px;align-items:center;'>");
			sb.append("<div style='font-size:11px;color:#334155;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;'>").append(escapeDashboardHtml(r.category)).append("</div>");
			sb.append("<div style='height:14px;border-radius:999px;background:#e2e8f0;overflow:hidden;'><div style='height:14px;width:").append(width)
					.append("%;border-radius:999px;background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div></div>");
			sb.append("<div style='font-size:11px;color:#0f172a;font-weight:900;text-align:right;'>").append(formatDashboardNumber(r.value)).append("</div>");
			sb.append("</div>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	private static double toDoubleDashboardValue(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return value == null ? 0.0d : Double.parseDouble(String.valueOf(value));
		} catch (Exception e) {
			return 0.0d;
		}
	}

	private static String formatDashboardNumber(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(Math.round(value));
		}
	}

	private static String escapeDashboardHtml(Object value) {
		String text = value == null ? "" : String.valueOf(value);
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}



	private String buildModernChartHtmlFromMap(String title, Map<String, Long> dataMap, String description) {
		HtmlCategoryModel model = new HtmlCategoryModel();
		if (dataMap != null) {
			for (Map.Entry<String, Long> entry : dataMap.entrySet()) {
				String label = entry == null ? "" : entry.getKey();
				Long value = entry == null ? Long.valueOf(0L) : entry.getValue();
				model.setValue("Jumlah", label, value);
			}
		}
		return buildModernChartHtml(title, model, description);
	}


}