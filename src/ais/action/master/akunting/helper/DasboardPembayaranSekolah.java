package ais.action.master.akunting.helper;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.MoveEvent;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.A;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Textbox;

import ais.action.master.sekolah.TagihanAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyToolbarbuttonConfig;

import org.zkoss.zul.Html;

/**
 * Memantau tagihan, pembayaran, dan piutang siswa agar sekolah mudah melihat pembayaran yang perlu ditindaklanjuti.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardPembayaranSekolah extends MyPortallayout {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9006490521125337935L;

	private Sekolah sk = null;
	private static final int DETAIL_PAGE_SIZE = 10;
	private static final int DASHBOARD_SAMPLE_LIMIT = 1200;
	private static final int DASHBOARD_TOP_PIUTANG_LIMIT = 12;
	private String dashboardTahunAjaran = null;
	private ItemBiayaSekolah dashboardItemBiayaSekolah = null; // legacy field, filter baru memakai nama/kode teks
	private String dashboardItemBiayaKeyword = "";
	private String dashboardKeyword = "";
	private transient org.zkoss.zk.ui.Component dashboardLoadingComponent = null;
	private transient org.zkoss.zul.Html dashboardLoadingHtml = null;
	private String dashboardCurrentStage = "";
	private int dashboardCurrentProgress = 0;
	private long dashboardProcessStart = 0L;
	public static boolean debug = false;
	public static boolean debuh = false;

	public DasboardPembayaranSekolah() throws Exception {
		super();
		sk = SekolahUtil.getSekolah();
		dashboardTahunAjaran = Common.getCurrentTahunAkademik();
		// setHeight("25000px");
		setWidth("100%");
		setMaximizedMode("whole");
		init();
	}

	private int width = 550;
	private int height = 110;
	
	/**
	 * Membangun Panel Grafik Tren Pembayaran untuk posisi paling atas Dashboard.
	 */
	private Panel buildTrendPanel() {
		
		
		Panel panel = new Panel();
		panel.setTitle("Tren Riwayat Pembayaran Siswa");
		panel.setBorder("normal");
		panel.setCollapsible(true);
		panel.setStyle("margin-bottom: 20px;");

		org.zkoss.zul.Panelchildren pc = new org.zkoss.zul.Panelchildren();
		pc.setParent(panel);

		// --- BAGIAN FILTER DATA ---
		org.zkoss.zul.Groupbox gbFilter = new org.zkoss.zul.Groupbox();
		gbFilter.setMold("3d");
		gbFilter.setParent(pc);
		org.zkoss.zul.Caption cap = new org.zkoss.zul.Caption("Filter Tren Pembayaran");
		cap.setParent(gbFilter);

		Grid gridFilter = new Grid();
		gridFilter.setParent(gbFilter);
		gridFilter.setStyle("border: none;");
		Columns cols = new Columns();
		cols.setParent(gridFilter);
		
		Column col1 = new Column(); col1.setWidth("160px"); col1.setParent(cols);
		Column col2 = new Column(); col2.setParent(cols);
		Column col3 = new Column(); col3.setWidth("160px"); col3.setParent(cols);
		Column col4 = new Column(); col4.setParent(cols);

		Rows rows = new Rows();
		rows.setParent(gridFilter);
		
		// Baris 1 Filter
		MyFormRow row1 = new MyFormRow();
		row1.setParent(rows);

		row1.appendChild(new org.zkoss.zul.Label("Siswa / Calon Siswa :"));
		final org.zkoss.zul.Textbox txtSiswa = new org.zkoss.zul.Textbox();
		txtSiswa.setWidth("95%");
		row1.appendChild(txtSiswa);

		row1.appendChild(new org.zkoss.zul.Label("Jenis Biaya (Nama/Kode) :"));
		final org.zkoss.zul.Textbox txtJenis = new org.zkoss.zul.Textbox();
		txtJenis.setWidth("95%");
		row1.appendChild(txtJenis);
		
		// Baris 2 Filter
		MyFormRow row2 = new MyFormRow();
		row2.setParent(rows);

		row2.appendChild(new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig("Periode Tren :")));
		final org.zkoss.zul.Combobox cmbPeriode = new org.zkoss.zul.Combobox();
		cmbPeriode.setReadonly(true);
		cmbPeriode.setWidth("95%");
		cmbPeriode.appendItem("Harian");
		cmbPeriode.appendItem("Mingguan");
		cmbPeriode.appendItem("Bulanan");
		cmbPeriode.appendItem("Semesteran");
		cmbPeriode.setSelectedIndex(2); // Default ke Bulanan
		row2.appendChild(cmbPeriode);

		// Tombol Eksekusi
		org.zkoss.zul.Button btnFilter = new org.zkoss.zul.Button("Tampilkan Grafik");
		btnFilter.setImage("/img/svg/search.svg");
		row2.appendChild(btnFilter);

		// --- BAGIAN GRAFIK HTML/CSS ---
		final org.zkoss.zul.Div trendChart = new org.zkoss.zul.Div();
		trendChart.setParent(pc);
		trendChart.setStyle("width:100%;box-sizing:border-box;margin-top:12px;");

		btnFilter.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				refreshTrendData(trendChart, txtSiswa.getValue(), txtJenis.getValue(), cmbPeriode.getValue());
			}
		});
		try {
			refreshTrendData(trendChart, txtSiswa.getValue(), txtJenis.getValue(), cmbPeriode.getValue());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		// Memuat data secara default pertama kali saat halaman dibuka
		try {
			refreshTrendData(trendChart, "", "", "Bulanan");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPembayaranSekolah.java:198");}

		return panel;
	}

	/**
	 * Melakukan kalkulasi dan agregasi data PembayaranSiswaDetail berdasarkan parameter filter
	 */
	private void refreshTrendData(org.zkoss.zul.Div chart, String filterSiswa, String filterJenis, String periode) {
		Session session = null;
		try {
			session = ais.database.hibernate.HibernateUtil.currentNativeSession();
			
			Criteria crit = session.createCriteria(ais.database.model.sekolah.PembayaranSiswaDetail.class, "psd");
			crit.createAlias("psd.pembayaranSiswa", "ps", Criteria.INNER_JOIN);
			crit.createAlias("ps.siswa", "s", Criteria.LEFT_JOIN);
			crit.createAlias("ps.calonSiswa", "cs", Criteria.LEFT_JOIN);
			crit.createAlias("psd.tagihan", "t", Criteria.LEFT_JOIN);
			crit.createAlias("t.nominalBiaya", "nb", Criteria.LEFT_JOIN);
			crit.createAlias("nb.pengaturanBiaya", "pb", Criteria.LEFT_JOIN);
			crit.createAlias("pb.jenisBiayaSekolah", "jbs", Criteria.LEFT_JOIN);

			// Logic Filter Siswa
			if (filterSiswa != null && !filterSiswa.trim().isEmpty()) {
				crit.add(Restrictions.or(
					Restrictions.ilike("s.nama", "%" + filterSiswa.trim() + "%"),
					Restrictions.ilike("cs.nama", "%" + filterSiswa.trim() + "%")
				));
			}

			// Logic Filter Jenis Biaya (Nama atau Kode)
			if (filterJenis != null && !filterJenis.trim().isEmpty()) {
				crit.add(Restrictions.or(
					Restrictions.ilike("jbs.nama", "%" + filterJenis.trim() + "%"),
					Restrictions.ilike("jbs.kode", "%" + filterJenis.trim() + "%")
				));
			}

			// Hanya ambil yang tanggal transaksinya tidak null
			crit.add(Restrictions.isNotNull("ps.tanggal"));
			
			// Lakukan Proyeksi (Projection) agar DB hanya menarik Kolom Tanggal dan Nominal saja (Sangat Ringan)
			crit.setProjection(Projections.projectionList()
				.add(Projections.property("ps.tanggal"))
				.add(Projections.property("psd.nominal"))
			);

			List<Object[]> results = crit.list();

			// Grouping Key berdasarkan pilihan Periode (Menggunakan TreeMap agar terurut secara kronologis)
			java.util.Map<String, Double> aggMap = new java.util.TreeMap<String, Double>();
			java.text.SimpleDateFormat sdf;
			if ("Harian".equals(periode)) {
				sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
			} else if ("Mingguan".equals(periode)) {
				sdf = new java.text.SimpleDateFormat("yyyy-'W'ww"); // yyyy-W42
			} else {
				sdf = new java.text.SimpleDateFormat("yyyy-MM"); // Bulanan
			}

			java.util.Calendar cal = java.util.Calendar.getInstance();
			for (Object[] row : results) {
				java.util.Date tgl = (java.util.Date) row[0];
				Double nom = row[1] != null ? ((Number)row[1]).doubleValue() : 0.0;
				
				String key = "";
				if ("Semesteran".equals(periode)) {
					cal.setTime(tgl);
					int year = cal.get(java.util.Calendar.YEAR);
					int month = cal.get(java.util.Calendar.MONTH); // 0-11
					// Asumsi: Bulan 0 s/d 5 = Sem 1, Bulan 6 s/d 11 = Sem 2
					key = year + "-S" + (month < 6 ? "1" : "2");
				} else {
					key = sdf.format(tgl);
				}

				Double current = aggMap.get(key);
				aggMap.put(key, (current == null ? 0.0 : current) + nom);
			}

			// Masukkan hasil ke dalam HtmlCategoryModel (standar ZK 5.5)
			HtmlCategoryModel model = new HtmlCategoryModel();
			for (java.util.Map.Entry<String, Double> entry : aggMap.entrySet()) {
				model.setValue("Pembayaran", entry.getKey(), entry.getValue());
			}
			Common.clear(chart);
			chart.appendChild(new Html(buildModernChartHtml("Tren Riwayat Pembayaran Siswa", model,
					"Naik-turun jumlah pembayaran siswa dari waktu ke waktu, supaya pola bayar harian, mingguan, bulanan, atau semesteran mudah terlihat.")));

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/DasboardPembayaranSekolah.java:288");
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPembayaranSekolah.java:291");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPembayaranSekolah.java:292");}
				try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPembayaranSekolah.java:293");}
			}
		}
	}

	private void init() throws Exception {

		EventListener reloadPiutangPerSekolah = new EventListener() {

			private String c = dashboardTahunAjaran == null ? Common.getCurrentTahunAkademik() : dashboardTahunAjaran;
			private String itemBiayaKeyword = dashboardItemBiayaKeyword == null ? "" : dashboardItemBiayaKeyword;

			private void piutangSiswa() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardPembayaranSekolah.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Piutang Per-Item Biaya"));
				panel.setBorder("none");
				panel.setCollapsible(false);
				panel.setClosable(false);
				panel.setMaximizable(false);
				panel.setMinimizable(false);
				panel.addEventListener("onMove", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						MoveEvent moveEvent = (MoveEvent) arg0;
						String left = moveEvent.getLeft();
						String top = moveEvent.getTop();

					}
				});
				panel.setStyle(
						"margin-bottom:10px;border: 1px;border-style: solid;border-color: #dbdbd9;margin-left: 3px;margin-right: 3px;border-radius: 10px;");
				final Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				EventListener pengajuanBaruEventListener = new EventListener() {

					public EventListener getThis() {
						return this;
					}

					@SuppressWarnings({ "unchecked" })
					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final Combobox ambilDataSekolahBanbox = new Combobox();
						final Combobox cari = Common.generateTahunAjaran(null);
						final Textbox searchJenisPembayaran = new Textbox();
						searchJenisPembayaran.setCols(12);
						searchJenisPembayaran.setValue(itemBiayaKeyword == null ? "" : itemBiayaKeyword);
						searchJenisPembayaran.setTooltiptext("Ketik nama/kode item biaya sekolah");

						Common.selectComboItem(cari, c);
						ambilDataSekolahBanbox.setCols(7);
						ambilDataSekolahBanbox.setValue(sk == null ? "Sekolah" : sk.getNama());
						ambilDataSekolahBanbox.setAttribute("sekolah", sk);
						ambilDataSekolahBanbox.setAttribute("myValue", sk);
						ambilDataSekolahBanbox.setReadonly(true);
						ambilDataSekolahBanbox.setParent(toolbar);
						Yayasan selectedYayasan = SekolahUtil.getYayasan();
						Common.insertComboDanSemua(ambilDataSekolahBanbox, new String[] { "nama", "jenisSekolah" },
								"yayasan", Sekolah.class, "=" + Common.getBahasaConfig("sekolah") + "=",
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								Restrictions.eq("yayasan", selectedYayasan));
						Common.selectComboItem(ambilDataSekolahBanbox, sk);

						Sekolah selectedSekolah = SekolahUtil.getSekolah();
						if (selectedSekolah != null && selectedSekolah.getId() != null) {
							ambilDataSekolahBanbox.setDisabled(true);
						}
						searchJenisPembayaran.setParent(toolbar);

						ambilDataSekolahBanbox.addEventListener("onChange", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								c = cari.getValue().trim();

								sk = (Sekolah) (ambilDataSekolahBanbox.getSelectedItem() == null ? null
										: ambilDataSekolahBanbox.getSelectedItem().getValue());

								itemBiayaKeyword = searchJenisPembayaran.getValue() == null ? "" : searchJenisPembayaran.getValue().trim();

								Common.createDefaultTimer(getThis());
							}
						});

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(6);
						cari.setParent(toolbar);

						cari.addEventListener("onChange", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								c = cari.getValue().trim();

								sk = (Sekolah) (ambilDataSekolahBanbox.getSelectedItem() == null ? null
										: ambilDataSekolahBanbox.getSelectedItem().getValue());

								itemBiayaKeyword = searchJenisPembayaran.getValue() == null ? "" : searchJenisPembayaran.getValue().trim();

								Common.createDefaultTimer(getThis());
							}
						});

						searchJenisPembayaran.addEventListener("onOK", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								c = cari.getValue().trim();

								sk = (Sekolah) (ambilDataSekolahBanbox.getSelectedItem() == null ? null
										: ambilDataSekolahBanbox.getSelectedItem().getValue());

								itemBiayaKeyword = searchJenisPembayaran.getValue() == null ? "" : searchJenisPembayaran.getValue().trim();

								Common.createDefaultTimer(getThis());
							}
						});

						MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
						refresh.setTooltiptext("Refresh");
						refresh.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								Common.clear(DasboardPembayaranSekolah.this);
								DasboardPembayaranSekolah.this.init();
							}
						});
						refresh.setParent(toolbar);

						Session session = HibernateUtil.currentSession();
						List<Object[]> tagihans = session.createCriteria(Tagihan.class)
								.createAlias("itemBiayaSekolah", "legacyItem", Criteria.LEFT_JOIN)

								.setProjection(Projections.projectionList().add(Projections.groupProperty("sekolah"))
										.add(Projections.groupProperty("itemBiayaSekolah"))
										.add(Projections.sum("nominal")).add(Projections.sum("dibayar")))

								.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("sekolah", sk))
								.add(itemBiayaKeyword == null || itemBiayaKeyword.trim().length() == 0 ? Restrictions.sqlRestriction("true")
										: Restrictions.or(Restrictions.ilike("legacyItem.nama", "%" + itemBiayaKeyword.trim() + "%"),
												Restrictions.ilike("legacyItem.kode", "%" + itemBiayaKeyword.trim() + "%")))

								.add(Restrictions.eq("tahunAjaran", c)).add(Restrictions.isNotNull("sekolah"))
								.add(Restrictions.isNotNull("itemBiayaSekolah"))

								.addOrder(Order.asc("sekolah")).addOrder(Order.asc("itemBiayaSekolah"))
								.setMaxResults(500).list();

						MyFormRow rowUtama = new MyFormRow();
						rowUtama.setParent(rowUtamapalingAwal.getParent());

						Grid grid = new Grid();
						grid.setSclass("dgrid");
						grid.setParent(rowUtama);
						grid.setSclass("fgrid");
						grid.setStyle("min-height:100px;border:0px;background: transparent;");
						grid.setMold("paging");
						grid.setPageSize(10);
						grid.getPagingChild().setMold("os");

						Columns columns = new Columns();
						columns.setParent(grid);

						Column column = new MyColumnConfig("Sekolah");
						column.setWidth("18%");
						columns.appendChild(column);

						column = new MyColumnConfig("Jenis Pembayaran");
						columns.appendChild(column);
						column.setWidth("18%");

						column = new MyColumnConfig("Nominal");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Dibayar");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Piutang");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("%");
						column.setAlign("right");
						column.setWidth("8%");
						columns.appendChild(column);

						Rows rows = new Rows();
						rows.setParent(grid);

						int size = 0;
						HtmlCategoryModel categoryModel = new HtmlCategoryModel();
						HtmlCategoryModel categoryModelLunas = new HtmlCategoryModel();

						Double totalnominal = 0.0;
						Double totaldibayar = 0.0;
						Double totalpiutang = 0.0;

						for (Object[] tagihan : tagihans) {

							final Sekolah sekolah = (Sekolah) tagihan[0];
							final ItemBiayaSekolah itemBiayaSekolah = (ItemBiayaSekolah) tagihan[1];
							Number nominal = (Number) (tagihan[2] == null ? 0.0 : tagihan[2]);
							Number dibayar = (Number) (tagihan[3] == null ? 0.0 : tagihan[3]);
							Double piutang = nominal.doubleValue() - dibayar.doubleValue();

							if (nominal.doubleValue() > 0.01) {

								totalnominal += nominal.doubleValue();
								totaldibayar += dibayar.doubleValue();
								totalpiutang += piutang.doubleValue();

								size++;
								MyFormRow rowUtamaLagi = new MyFormRow();
								rowUtamaLagi.setParent(rows);
								new MyLabelKecilBold(sekolah.getNama()).setParent(rowUtamaLagi);
								new MyLabelKecil(itemBiayaSekolah.getNama()).setParent(rowUtamaLagi);

								rowUtamaLagi.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(nominal)));

								A a = new A(Common.numberFormat.get().format(dibayar));
								a.setStyle("font-size:11px;");

								a.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
												Detailperkuliahan.class, new DataCriteriaWithColumn() {

													@Override
													public Object[] initCriteria(boolean order) {

														try {

															int countCal = ((Number) HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.gt("dibayar", 0.1))
																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))
																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("sekolah", sekolah))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.add(Restrictions.isNotNull("calonSiswa"))
																	.setProjection(Projections.rowCount())
																	.uniqueResult()).intValue();
															int countSiswa = ((Number) HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.gt("dibayar", 0.1))
																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))
																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("sekolah", sekolah))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.add(Restrictions.isNotNull("siswa"))
																	.setProjection(Projections.rowCount())
																	.uniqueResult()).intValue();

															Criteria criteria = HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.gt("dibayar", 0.1))

																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))

																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("sekolah", sekolah))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.addOrder(Order.asc("id"));

															String[] tag;
															if (countCal > 0 && countSiswa > 0) {
																tag = TagihanAction.DATA;
															} else if (countSiswa > 0) {
																tag = TagihanAction.DATA_SISWA;
															} else {
																tag = TagihanAction.DATA_CALON;
															}

															return new Object[] { criteria, tag };

														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														}
														return null;
													}

												}, null, "Download Data", "/img/print.png", null, null, false, null,
												"DATA TAMBAHAN",
												new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "" })
												.getAttribute("eventListener");

										eventListener.onEvent(null);

									}
								});

								rowUtamaLagi.appendChild(a);

								a = new A(Common.numberFormat.get().format(piutang));
								a.setStyle("font-size:11px;");

								a.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
												Detailperkuliahan.class, new DataCriteriaWithColumn() {

													@Override
													public Object[] initCriteria(boolean order) {

														try {

															int countCal = ((Number) HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.le("dibayar", 0.1))
																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))
																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("sekolah", sekolah))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.add(Restrictions.isNotNull("calonSiswa"))
																	.setProjection(Projections.rowCount())
																	.uniqueResult()).intValue();
															int countSiswa = ((Number) HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.le("dibayar", 0.1))
																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))
																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("sekolah", sekolah))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.add(Restrictions.isNotNull("siswa"))
																	.setProjection(Projections.rowCount())
																	.uniqueResult()).intValue();

															Criteria criteria = HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.le("dibayar", 0.1))

																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))

																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("sekolah", sekolah))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.addOrder(Order.asc("id"));

															String[] tag;
															if (countCal > 0 && countSiswa > 0) {
																tag = TagihanAction.DATA;
															} else if (countSiswa > 0) {
																tag = TagihanAction.DATA_SISWA;
															} else {
																tag = TagihanAction.DATA_CALON;
															}

															return new Object[] { criteria, tag };

														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														}
														return null;
													}

												}, null, "Download Data", "/img/print.png", null, null, false, null,
												"DATA TAMBAHAN",
												new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "" })
												.getAttribute("eventListener");

										eventListener.onEvent(null);

									}
								});

								rowUtamaLagi.appendChild(a);

								Double lunas = (dibayar.doubleValue() * 100.0) / nominal.doubleValue();

								rowUtamaLagi.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(lunas)));

								categoryModel.setValue(sekolah.getNama(), itemBiayaSekolah.getNama(),
										piutang.doubleValue());

								categoryModelLunas.setValue(sekolah.getNama(), itemBiayaSekolah.getNama(), lunas);

							}
						}

						Foot rowUtamaLagi = new Foot();
						rowUtamaLagi.setParent(grid);

						Footer footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecilBold("TOTAL").setParent(footer);
						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecil().setParent(footer);

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totalnominal)));

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totaldibayar)));
						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totalpiutang)));
						Double lunas = totalnominal == null || totalnominal.doubleValue() == 0.0 ? Double.valueOf(0.0) : Double.valueOf((totaldibayar.doubleValue() * 100.0) / totalnominal.doubleValue());
						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(lunas)));

						MyFormRow row = new MyFormRow();
		row.setValign("top");
						row.setParent(rowUtamapalingAwal.getParent());
						row.setAlign("center");
						row.appendChild(new Html(buildModernChartHtml(String.valueOf("Piutang Siswa TA " + (c)), categoryModel,
								"membantu melihat perbandingan piutang, pembayaran, dan tingkat pelunasan siswa berdasarkan filter yang dipilih.")));
row = new MyFormRow();
						row.setParent(rowUtamapalingAwal.getParent());
						row.setAlign("center");
						row.appendChild(new Html(buildModernChartHtml(String.valueOf("Persen Lunas TA " + (c)), categoryModelLunas,
								"membantu melihat perbandingan piutang, pembayaran, dan tingkat pelunasan siswa berdasarkan filter yang dipilih.")));
tagihans = null;
					}
				};

				pengajuanBaruEventListener.onEvent(null);
			}

			private void piutangSiswaPerAngkatan() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardPembayaranSekolah.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Piutang Per-Angkatan"));
				panel.setBorder("none");
				panel.setCollapsible(false);
				panel.setClosable(false);
				panel.setMaximizable(false);
				panel.setMinimizable(false);
				panel.addEventListener("onMove", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						MoveEvent moveEvent = (MoveEvent) arg0;
						String left = moveEvent.getLeft();
						String top = moveEvent.getTop();

					}
				});
				panel.setStyle(
						"margin-bottom:10px;border: 1px;border-style: solid;border-color: #dbdbd9;margin-left: 3px;margin-right: 3px;border-radius: 10px;");
				final Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				EventListener pengajuanBaruEventListener = new EventListener() {

					public EventListener getThis() {
						return this;
					}

					@SuppressWarnings({ "unchecked" })
					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final Combobox ambilDataSekolahBanbox = new Combobox();
						final Combobox cari = Common.generateTahunAjaran(null);

						final Textbox searchJenisPembayaran = new Textbox();
						searchJenisPembayaran.setCols(12);
						searchJenisPembayaran.setValue(itemBiayaKeyword == null ? "" : itemBiayaKeyword);
						searchJenisPembayaran.setTooltiptext("Ketik nama/kode item biaya sekolah");

						Common.selectComboItem(cari, c);
						ambilDataSekolahBanbox.setCols(7);
						ambilDataSekolahBanbox.setValue(sk == null ? "Sekolah" : sk.getNama());
						ambilDataSekolahBanbox.setAttribute("sekolah", sk);
						ambilDataSekolahBanbox.setAttribute("myValue", sk);
						ambilDataSekolahBanbox.setReadonly(true);
						ambilDataSekolahBanbox.setParent(toolbar);

						Yayasan selectedYayasan = SekolahUtil.getYayasan();
						Common.insertComboDanSemua(ambilDataSekolahBanbox, new String[] { "nama", "jenisSekolah" },
								"yayasan", Sekolah.class, "=" + Common.getBahasaConfig("sekolah") + "=",
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								Restrictions.eq("yayasan", selectedYayasan));

						Common.selectComboItem(ambilDataSekolahBanbox, sk);

						Sekolah selectedSekolah = SekolahUtil.getSekolah();
						if (selectedSekolah != null && selectedSekolah.getId() != null) {
							ambilDataSekolahBanbox.setDisabled(true);
						}

						searchJenisPembayaran.setParent(toolbar);

						ambilDataSekolahBanbox.addEventListener("onChange", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								c = cari.getValue().trim();

								sk = (Sekolah) (ambilDataSekolahBanbox.getSelectedItem() == null ? null
										: ambilDataSekolahBanbox.getSelectedItem().getValue());

								itemBiayaKeyword = searchJenisPembayaran.getValue() == null ? "" : searchJenisPembayaran.getValue().trim();

								Common.createDefaultTimer(getThis());
							}
						});

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(6);
						cari.setParent(toolbar);

						cari.addEventListener("onChange", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								c = cari.getValue().trim();

								sk = (Sekolah) (ambilDataSekolahBanbox.getSelectedItem() == null ? null
										: ambilDataSekolahBanbox.getSelectedItem().getValue());
								itemBiayaKeyword = searchJenisPembayaran.getValue() == null ? "" : searchJenisPembayaran.getValue().trim();

								Common.createDefaultTimer(getThis());
							}
						});

						searchJenisPembayaran.addEventListener("onOK", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								c = cari.getValue().trim();

								sk = (Sekolah) (ambilDataSekolahBanbox.getSelectedItem() == null ? null
										: ambilDataSekolahBanbox.getSelectedItem().getValue());
								itemBiayaKeyword = searchJenisPembayaran.getValue() == null ? "" : searchJenisPembayaran.getValue().trim();

								Common.createDefaultTimer(getThis());
							}
						});

						MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
						refresh.setTooltiptext("Refresh");
						refresh.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								Common.clear(DasboardPembayaranSekolah.this);
								DasboardPembayaranSekolah.this.init();
							}
						});
						refresh.setParent(toolbar);

						Session session = HibernateUtil.currentSession();
						List<Object[]> tagihans = session.createCriteria(Tagihan.class)
								.createAlias("itemBiayaSekolah", "legacyItem", Criteria.LEFT_JOIN)

								.setProjection(
										Projections.projectionList().add(Projections.groupProperty("tahunAngkatan"))
												.add(Projections.groupProperty("itemBiayaSekolah"))
												.add(Projections.sum("nominal")).add(Projections.sum("dibayar")))

								.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("sekolah", sk))
								.add(itemBiayaKeyword == null || itemBiayaKeyword.trim().length() == 0 ? Restrictions.sqlRestriction("true")
										: Restrictions.or(Restrictions.ilike("legacyItem.nama", "%" + itemBiayaKeyword.trim() + "%"),
												Restrictions.ilike("legacyItem.kode", "%" + itemBiayaKeyword.trim() + "%")))

								.add(Restrictions.eq("tahunAjaran", c)).add(Restrictions.isNotNull("tahunAngkatan"))
								.add(Restrictions.isNotNull("itemBiayaSekolah"))

								.addOrder(Order.asc("tahunAngkatan")).addOrder(Order.asc("itemBiayaSekolah"))
								.setMaxResults(500).list();

						MyFormRow rowUtama = new MyFormRow();
						rowUtama.setParent(rowUtamapalingAwal.getParent());

						Grid grid = new Grid();
						grid.setSclass("dgrid");
						grid.setParent(rowUtama);
						grid.setSclass("fgrid");
						grid.setStyle("min-height:100px;border:0px;background: transparent;");
						grid.setMold("paging");
						grid.setPageSize(10);
						grid.getPagingChild().setMold("os");

						Columns columns = new Columns();
						columns.setParent(grid);

						Column column = new MyColumnConfig("Angkatan");
						column.setWidth("10%");
						columns.appendChild(column);

						column = new MyColumnConfig("Jenis Pembayaran");
						columns.appendChild(column);
						column.setWidth("18%");

						column = new MyColumnConfig("Nominal");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Dibayar");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Piutang");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("%");
						column.setAlign("right");
						column.setWidth("8%");
						columns.appendChild(column);

						Rows rows = new Rows();
						rows.setParent(grid);

						int size = 0;
						HtmlCategoryModel categoryModel = new HtmlCategoryModel();
						HtmlCategoryModel categoryModelLunas = new HtmlCategoryModel();

						Double totalnominal = 0.0;
						Double totaldibayar = 0.0;
						Double totalpiutang = 0.0;

						for (Object[] tagihan : tagihans) {

							final Number tahunAngkatan = (Number) tagihan[0];
							final ItemBiayaSekolah itemBiayaSekolah = (ItemBiayaSekolah) tagihan[1];
							Number nominal = (Number) (tagihan[2] == null ? 0.0 : tagihan[2]);
							Number dibayar = (Number) (tagihan[3] == null ? 0.0 : tagihan[3]);
							Double piutang = nominal.doubleValue() - dibayar.doubleValue();

							if (nominal.doubleValue() > 0.01) {

								totalnominal += nominal.doubleValue();
								totaldibayar += dibayar.doubleValue();
								totalpiutang += piutang.doubleValue();

								size++;
								MyFormRow rowUtamaLagi = new MyFormRow();
								rowUtamaLagi.setParent(rows);
								new MyLabelKecilBold(tahunAngkatan + "").setParent(rowUtamaLagi);
								new MyLabelKecil(itemBiayaSekolah.getNama()).setParent(rowUtamaLagi);

								rowUtamaLagi.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(nominal)));

								A a = new A(Common.numberFormat.get().format(dibayar));
								a.setStyle("font-size:11px;");

								a.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
												Detailperkuliahan.class, new DataCriteriaWithColumn() {

													@Override
													public Object[] initCriteria(boolean order) {

														try {

															int countCal = ((Number) HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.gt("dibayar", 0.1))
																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))
																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("tahunAngkatan",
																			tahunAngkatan.intValue()))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.add(Restrictions.isNotNull("calonSiswa"))
																	.setProjection(Projections.rowCount())
																	.uniqueResult()).intValue();
															int countSiswa = ((Number) HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.gt("dibayar", 0.1))
																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))
																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("tahunAngkatan",
																			tahunAngkatan.intValue()))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.add(Restrictions.isNotNull("siswa"))
																	.setProjection(Projections.rowCount())
																	.uniqueResult()).intValue();

															Criteria criteria = HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.gt("dibayar", 0.1))

																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))

																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("tahunAngkatan",
																			tahunAngkatan.intValue()))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.addOrder(Order.asc("id"));

															String[] tag;
															if (countCal > 0 && countSiswa > 0) {
																tag = TagihanAction.DATA;
															} else if (countSiswa > 0) {
																tag = TagihanAction.DATA_SISWA;
															} else {
																tag = TagihanAction.DATA_CALON;
															}

															return new Object[] { criteria, tag };

														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														}
														return null;
													}

												}, null, "Download Data", "/img/print.png", null, null, false, null,
												"DATA TAMBAHAN",
												new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "" })
												.getAttribute("eventListener");

										eventListener.onEvent(null);

									}
								});

								rowUtamaLagi.appendChild(a);

								a = new A(Common.numberFormat.get().format(piutang));
								a.setStyle("font-size:11px;");

								a.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
												Detailperkuliahan.class, new DataCriteriaWithColumn() {

													@Override
													public Object[] initCriteria(boolean order) {

														try {

															int countCal = ((Number) HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.le("dibayar", 0.1))
																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))
																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("tahunAngkatan",
																			tahunAngkatan.intValue()))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.add(Restrictions.isNotNull("calonSiswa"))
																	.setProjection(Projections.rowCount())
																	.uniqueResult()).intValue();
															int countSiswa = ((Number) HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.le("dibayar", 0.1))
																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))
																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("tahunAngkatan",
																			tahunAngkatan.intValue()))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.add(Restrictions.isNotNull("siswa"))
																	.setProjection(Projections.rowCount())
																	.uniqueResult()).intValue();

															Criteria criteria = HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.le("dibayar", 0.1))

																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))

																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("tahunAngkatan",
																			tahunAngkatan.intValue()))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.addOrder(Order.asc("id"));

															String[] tag;
															if (countCal > 0 && countSiswa > 0) {
																tag = TagihanAction.DATA;
															} else if (countSiswa > 0) {
																tag = TagihanAction.DATA_SISWA;
															} else {
																tag = TagihanAction.DATA_CALON;
															}

															return new Object[] { criteria, tag };

														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														}
														return null;
													}

												}, null, "Download Data", "/img/print.png", null, null, false, null,
												"DATA TAMBAHAN",
												new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "" })
												.getAttribute("eventListener");

										eventListener.onEvent(null);

									}
								});

								rowUtamaLagi.appendChild(a);

								Double lunas = (dibayar.doubleValue() * 100.0) / nominal.doubleValue();

								rowUtamaLagi.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(lunas)));

								categoryModel.setValue(tahunAngkatan + "", itemBiayaSekolah.getNama(),
										piutang.doubleValue());

								categoryModelLunas.setValue(tahunAngkatan + "", itemBiayaSekolah.getNama(), lunas);

							}
						}

						Foot rowUtamaLagi = new Foot();
						rowUtamaLagi.setParent(grid);

						Footer footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecilBold("TOTAL").setParent(footer);
						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecil().setParent(footer);

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totalnominal)));

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totaldibayar)));
						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totalpiutang)));
						Double lunas = totalnominal == null || totalnominal.doubleValue() == 0.0 ? Double.valueOf(0.0) : Double.valueOf((totaldibayar.doubleValue() * 100.0) / totalnominal.doubleValue());
						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(lunas)));

						MyFormRow row = new MyFormRow();
		row.setValign("top");
						row.setParent(rowUtamapalingAwal.getParent());
						row.setAlign("center");
						row.appendChild(new Html(buildModernChartHtml(String.valueOf("Piutang Siswa TA " + (c)), categoryModel,
								"membantu melihat perbandingan piutang, pembayaran, dan tingkat pelunasan siswa berdasarkan filter yang dipilih.")));
row = new MyFormRow();
						row.setParent(rowUtamapalingAwal.getParent());
						row.setAlign("center");
						row.appendChild(new Html(buildModernChartHtml(String.valueOf("Persen Lunas TA " + (c)), categoryModelLunas,
								"membantu melihat perbandingan piutang, pembayaran, dan tingkat pelunasan siswa berdasarkan filter yang dipilih.")));
tagihans = null;
					}
				};

				pengajuanBaruEventListener.onEvent(null);
			}

			private void piutangSiswaPerKelas() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardPembayaranSekolah.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Piutang Per-Kelas"));
				panel.setBorder("none");
				panel.setCollapsible(false);
				panel.setClosable(false);
				panel.setMaximizable(false);
				panel.setMinimizable(false);
				panel.addEventListener("onMove", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						MoveEvent moveEvent = (MoveEvent) arg0;
						String left = moveEvent.getLeft();
						String top = moveEvent.getTop();

					}
				});
				panel.setStyle(
						"margin-bottom:10px;border: 1px;border-style: solid;border-color: #dbdbd9;margin-left: 3px;margin-right: 3px;border-radius: 10px;");
				final Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				EventListener pengajuanBaruEventListener = new EventListener() {

					public EventListener getThis() {
						return this;
					}

					@SuppressWarnings({ "unchecked" })
					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final Combobox ambilDataSekolahBanbox = new Combobox();
						final Combobox cari = Common.generateTahunAjaran(null);

						final Textbox searchJenisPembayaran = new Textbox();
						searchJenisPembayaran.setCols(12);
						searchJenisPembayaran.setValue(itemBiayaKeyword == null ? "" : itemBiayaKeyword);
						searchJenisPembayaran.setTooltiptext("Ketik nama/kode item biaya sekolah");

						Common.selectComboItem(cari, c);
						ambilDataSekolahBanbox.setCols(7);
						ambilDataSekolahBanbox.setValue(sk == null ? "Sekolah" : sk.getNama());
						ambilDataSekolahBanbox.setAttribute("sekolah", sk);
						ambilDataSekolahBanbox.setAttribute("myValue", sk);
						ambilDataSekolahBanbox.setReadonly(true);
						ambilDataSekolahBanbox.setParent(toolbar);

						Yayasan selectedYayasan = SekolahUtil.getYayasan();
						Common.insertComboDanSemua(ambilDataSekolahBanbox, new String[] { "nama", "jenisSekolah" },
								"yayasan", Sekolah.class, "=" + Common.getBahasaConfig("sekolah") + "=",
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								Restrictions.eq("yayasan", selectedYayasan));

						Common.selectComboItem(ambilDataSekolahBanbox, sk);

						Sekolah selectedSekolah = SekolahUtil.getSekolah();
						if (selectedSekolah != null && selectedSekolah.getId() != null) {
							ambilDataSekolahBanbox.setDisabled(true);
						}

						searchJenisPembayaran.setParent(toolbar);

						ambilDataSekolahBanbox.addEventListener("onChange", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								c = cari.getValue().trim();

								sk = (Sekolah) (ambilDataSekolahBanbox.getSelectedItem() == null ? null
										: ambilDataSekolahBanbox.getSelectedItem().getValue());

								itemBiayaKeyword = searchJenisPembayaran.getValue() == null ? "" : searchJenisPembayaran.getValue().trim();

								Common.createDefaultTimer(getThis());
							}
						});

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(6);
						cari.setParent(toolbar);

						cari.addEventListener("onChange", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								c = cari.getValue().trim();

								sk = (Sekolah) (ambilDataSekolahBanbox.getSelectedItem() == null ? null
										: ambilDataSekolahBanbox.getSelectedItem().getValue());
								itemBiayaKeyword = searchJenisPembayaran.getValue() == null ? "" : searchJenisPembayaran.getValue().trim();

								Common.createDefaultTimer(getThis());
							}
						});

						searchJenisPembayaran.addEventListener("onOK", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								c = cari.getValue().trim();

								sk = (Sekolah) (ambilDataSekolahBanbox.getSelectedItem() == null ? null
										: ambilDataSekolahBanbox.getSelectedItem().getValue());
								itemBiayaKeyword = searchJenisPembayaran.getValue() == null ? "" : searchJenisPembayaran.getValue().trim();

								Common.createDefaultTimer(getThis());
							}
						});

						MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
						refresh.setTooltiptext("Refresh");
						refresh.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								Common.clear(DasboardPembayaranSekolah.this);
								DasboardPembayaranSekolah.this.init();
							}
						});
						refresh.setParent(toolbar);

						Session session = HibernateUtil.currentSession();
						List<Object[]> tagihans = session.createCriteria(Tagihan.class)
								.createAlias("itemBiayaSekolah", "legacyItem", Criteria.LEFT_JOIN)

								.setProjection(Projections.projectionList().add(Projections.groupProperty("kelasSiswa"))
										.add(Projections.groupProperty("itemBiayaSekolah"))
										.add(Projections.sum("nominal")).add(Projections.sum("dibayar")))

								.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("sekolah", sk))
								.add(itemBiayaKeyword == null || itemBiayaKeyword.trim().length() == 0 ? Restrictions.sqlRestriction("true")
										: Restrictions.or(Restrictions.ilike("legacyItem.nama", "%" + itemBiayaKeyword.trim() + "%"),
												Restrictions.ilike("legacyItem.kode", "%" + itemBiayaKeyword.trim() + "%")))

								.add(Restrictions.eq("tahunAjaran", c)).add(Restrictions.isNotNull("kelasSiswa"))
								.add(Restrictions.isNotNull("itemBiayaSekolah"))

								.addOrder(Order.asc("kelasSiswa")).addOrder(Order.asc("itemBiayaSekolah"))
								.setMaxResults(500).list();

						MyFormRow rowUtama = new MyFormRow();
						rowUtama.setParent(rowUtamapalingAwal.getParent());

						Grid grid = new Grid();
						grid.setSclass("dgrid");
						grid.setParent(rowUtama);
						grid.setSclass("fgrid");
						grid.setStyle("min-height:100px;border:0px;background: transparent;");
						grid.setMold("paging");
						grid.setPageSize(10);
						grid.getPagingChild().setMold("os");

						Columns columns = new Columns();
						columns.setParent(grid);

						Column column = new MyColumnConfig("Angkatan");
						column.setWidth("10%");
						columns.appendChild(column);

						column = new MyColumnConfig("Jenis Pembayaran");
						columns.appendChild(column);
						column.setWidth("18%");

						column = new MyColumnConfig("Nominal");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Dibayar");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Piutang");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("%");
						column.setAlign("right");
						column.setWidth("8%");
						columns.appendChild(column);

						Rows rows = new Rows();
						rows.setParent(grid);

						int size = 0;
						HtmlCategoryModel categoryModel = new HtmlCategoryModel();
						HtmlCategoryModel categoryModelLunas = new HtmlCategoryModel();

						Double totalnominal = 0.0;
						Double totaldibayar = 0.0;
						Double totalpiutang = 0.0;

						for (Object[] tagihan : tagihans) {

							final KelasSiswa kelasSiswa = (KelasSiswa) tagihan[0];
							final ItemBiayaSekolah itemBiayaSekolah = (ItemBiayaSekolah) tagihan[1];
							Number nominal = (Number) (tagihan[2] == null ? 0.0 : tagihan[2]);
							Number dibayar = (Number) (tagihan[3] == null ? 0.0 : tagihan[3]);
							Double piutang = nominal.doubleValue() - dibayar.doubleValue();

							if (nominal.doubleValue() > 0.01) {

								totalnominal += nominal.doubleValue();
								totaldibayar += dibayar.doubleValue();
								totalpiutang += piutang.doubleValue();

								size++;
								MyFormRow rowUtamaLagi = new MyFormRow();
								rowUtamaLagi.setParent(rows);
								new MyLabelKecilBold(kelasSiswa + "").setParent(rowUtamaLagi);
								new MyLabelKecil(itemBiayaSekolah.getNama()).setParent(rowUtamaLagi);

								rowUtamaLagi.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(nominal)));

								A a = new A(Common.numberFormat.get().format(dibayar));
								a.setStyle("font-size:11px;");

								a.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
												Detailperkuliahan.class, new DataCriteriaWithColumn() {

													@Override
													public Object[] initCriteria(boolean order) {

														try {

															int countCal = ((Number) HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.gt("dibayar", 0.1))
																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))
																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("kelasSiswa", kelasSiswa))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.add(Restrictions.isNotNull("calonSiswa"))
																	.setProjection(Projections.rowCount())
																	.uniqueResult()).intValue();
															int countSiswa = ((Number) HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.gt("dibayar", 0.1))
																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))
																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("kelasSiswa", kelasSiswa))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.add(Restrictions.isNotNull("siswa"))
																	.setProjection(Projections.rowCount())
																	.uniqueResult()).intValue();

															Criteria criteria = HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.gt("dibayar", 0.1))

																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))

																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("kelasSiswa", kelasSiswa))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.addOrder(Order.asc("id"));

															String[] tag;
															if (countCal > 0 && countSiswa > 0) {
																tag = TagihanAction.DATA;
															} else if (countSiswa > 0) {
																tag = TagihanAction.DATA_SISWA;
															} else {
																tag = TagihanAction.DATA_CALON;
															}

															return new Object[] { criteria, tag };

														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														}
														return null;
													}

												}, null, "Download Data", "/img/print.png", null, null, false, null,
												"DATA TAMBAHAN",
												new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "" })
												.getAttribute("eventListener");

										eventListener.onEvent(null);

									}
								});

								rowUtamaLagi.appendChild(a);

								a = new A(Common.numberFormat.get().format(piutang));
								a.setStyle("font-size:11px;");

								a.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
												Detailperkuliahan.class, new DataCriteriaWithColumn() {

													@Override
													public Object[] initCriteria(boolean order) {

														try {

															int countCal = ((Number) HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.lt("dibayar", 0.1))
																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))
																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("kelasSiswa", kelasSiswa))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.add(Restrictions.isNotNull("calonSiswa"))
																	.setProjection(Projections.rowCount())
																	.uniqueResult()).intValue();
															int countSiswa = ((Number) HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.lt("dibayar", 0.1))
																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))
																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("kelasSiswa", kelasSiswa))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.add(Restrictions.isNotNull("siswa"))
																	.setProjection(Projections.rowCount())
																	.uniqueResult()).intValue();

															Criteria criteria = HibernateUtil.currentSession()
																	.createCriteria(Tagihan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("bukanTagihan", false))
																	.add(Restrictions.gt("nominal", 0.1))
																	.add(Restrictions.lt("dibayar", 0.1))

																	.add(Restrictions.isNotNull("sekolah"))
																	.add(Restrictions.isNotNull("itemBiayaSekolah"))

																	.add(Restrictions.eq("tahunAjaran", c))
																	.add(Restrictions.eq("kelasSiswa", kelasSiswa))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah))
																	.addOrder(Order.asc("id"));

															String[] tag;
															if (countCal > 0 && countSiswa > 0) {
																tag = TagihanAction.DATA;
															} else if (countSiswa > 0) {
																tag = TagihanAction.DATA_SISWA;
															} else {
																tag = TagihanAction.DATA_CALON;
															}

															return new Object[] { criteria, tag };

														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														}
														return null;
													}

												}, null, "Download Data", "/img/print.png", null, null, false, null,
												"DATA TAMBAHAN",
												new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "" })
												.getAttribute("eventListener");

										eventListener.onEvent(null);

									}
								});

								rowUtamaLagi.appendChild(a);

								Double lunas = (dibayar.doubleValue() * 100.0) / nominal.doubleValue();

								rowUtamaLagi.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(lunas)));

								categoryModel.setValue(kelasSiswa.getNama() + "", itemBiayaSekolah.getNama(),
										piutang.doubleValue());

								categoryModelLunas.setValue(kelasSiswa.getNama() + "", itemBiayaSekolah.getNama(),
										lunas);

							}
						}

						Foot rowUtamaLagi = new Foot();
						rowUtamaLagi.setParent(grid);

						Footer footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecilBold("TOTAL").setParent(footer);
						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecil().setParent(footer);

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totalnominal)));

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totaldibayar)));
						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totalpiutang)));
						Double lunas = totalnominal == null || totalnominal.doubleValue() == 0.0 ? Double.valueOf(0.0) : Double.valueOf((totaldibayar.doubleValue() * 100.0) / totalnominal.doubleValue());
						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(lunas)));

						MyFormRow row = new MyFormRow();
		row.setValign("top");
						row.setParent(rowUtamapalingAwal.getParent());
						row.setAlign("center");
						row.appendChild(new Html(buildModernChartHtml(String.valueOf("Piutang Siswa TA " + (c)), categoryModel,
								"membantu melihat perbandingan piutang, pembayaran, dan tingkat pelunasan siswa berdasarkan filter yang dipilih.")));
row = new MyFormRow();
						row.setParent(rowUtamapalingAwal.getParent());
						row.setAlign("center");
						row.appendChild(new Html(buildModernChartHtml(String.valueOf("Persen Lunas TA " + (c)), categoryModelLunas,
								"membantu melihat perbandingan piutang, pembayaran, dan tingkat pelunasan siswa berdasarkan filter yang dipilih.")));
tagihans = null;
					}
				};

				pengajuanBaruEventListener.onEvent(null);
			}

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					tampilkanLoadingDashboardPembayaran(DasboardPembayaranSekolah.this,
							"Menyiapkan tampilan dashboard pembayaran sekolah...", 3);

					updateDashboardProgress("Memuat overview pembayaran dan tagihan...", 8);
					renderEnhancedDashboardPembayaranSekolah();

					updateDashboardProgress("Memuat grafik tren pembayaran siswa...", 72);
					// 1. Buat kontainer kolom khusus ZK Portal
					MyPortalchildren portalTrendContainer = new MyPortalchildren();
					portalTrendContainer.setParent(DasboardPembayaranSekolah.this); // 'this' adalah MyPortallayout
					portalTrendContainer.setWidth("100%"); // Atur lebar penuh 100%

					// 2. Masukkan panel grafik tren ke dalam MyPortalchildren tersebut
					Panel trendPanel = buildTrendPanel();
					portalTrendContainer.appendChild(trendPanel);

					updateDashboardProgress("Memuat piutang per item biaya...", 82);
					piutangSiswa();
					updateDashboardProgress("Memuat piutang per angkatan...", 90);
					piutangSiswaPerAngkatan();
					updateDashboardProgress("Memuat piutang per kelas...", 96);
					piutangSiswaPerKelas();

					finishDashboardProgress("Dashboard pembayaran sekolah selesai dimuat.");
				} finally {
					removeDashboardLoading();
					clearDashboardBusy();
				}
			}
		};

		/* Tampilkan skeleton loading dulu agar halaman tidak terkesan blank saat queries berjalan.
		 * Queries aktual dijalankan oleh timer di AJAX request berikutnya, setelah skeleton terkirim ke browser. */
		tampilkanLoadingDashboardPembayaran(DasboardPembayaranSekolah.this,
				"Menyiapkan dashboard pembayaran sekolah...", 1);
		Common.createDefaultTimer(reloadPiutangPerSekolah);
	}

	// =========================================================================================
	// DASHBOARD PEMBAYARAN SEKOLAH - ENHANCED BERBASIS TEMPLATE SOP V7
	// =========================================================================================

	private void renderEnhancedDashboardPembayaranSekolah() {
		try {
			updateDashboardProgress("Menyiapkan dashboard pembayaran sekolah...", 10);
			if (dashboardTahunAjaran == null || dashboardTahunAjaran.trim().length() == 0) {
				dashboardTahunAjaran = Common.getCurrentTahunAkademik();
			}
			if (dashboardKeyword == null) {
				dashboardKeyword = "";
			}

			MyPortalchildren wrapper = new MyPortalchildren();
			wrapper.setParent(DasboardPembayaranSekolah.this);
			wrapper.setWidth("100%");
			wrapper.setStyle("padding:6px; box-sizing:border-box;");

			Panel panel = new ais.ui.util.MyPanelConfig();
			panel.setTitle("Dasbor Pembayaran dan Tagihan Siswa / Mahasiswa");
			panel.setBorder("none");
			panel.setCollapsible(false);
			panel.setClosable(false);
			panel.setMaximizable(false);
			panel.setMinimizable(false);
			panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:18px; overflow:hidden;"
					+ "background:#ffffff; box-shadow:0 14px 28px rgba(15,23,42,.08);");
			panel.setParent(wrapper);

			Panelchildren pch = new Panelchildren();
			pch.setStyle("padding:0; background:#f6f8fb;");
			pch.setParent(panel);

			org.zkoss.zul.Div shell = new org.zkoss.zul.Div();
			shell.setWidth("100%");
			shell.setStyle("background:#f6f8fb; padding:14px; box-sizing:border-box; overflow:auto;");
			shell.setParent(pch);

			final PembayaranDashboardData data = loadPembayaranDashboardData();
			renderPembayaranHero(shell, data);
			renderPembayaranGlobalFilter(shell);
			renderDashboardProcessingInfo(shell, data);
			renderPembayaranMetricCards(shell, data);
			ais.ui.util.DashboardJurnalPembayaranUtil.renderJurnalSiswaPanel(shell, "Ringkasan Jurnal Pembayaran Siswa",
					"Menunjukkan akun kas/bank, piutang, pendapatan, denda, dan diskon yang dipakai pada pembayaran siswa. Data ini membantu petugas melihat akun yang belum lengkap sebelum transaksi diposting.");

			MyPortallayout analyticLayout = new MyPortallayout();
			analyticLayout.setWidth("100%");
			analyticLayout.setMaximizedMode("whole");
			analyticLayout.setStyle("margin-top:12px; padding:0; background:transparent;");
			analyticLayout.setParent(shell);

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

			updateDashboardProgress("Menampilkan funnel pembayaran...", 73);
			renderFunnelPembayaran(pcTop, data);
			updateDashboardProgress("Menampilkan risiko tagihan...", 75);
			renderRisikoTagihan(pcLeft, data);
			updateDashboardProgress("Menampilkan sebaran item biaya...", 77);
			renderSebaranItemBiaya(pcRight, data);
			updateDashboardProgress("Menampilkan sebaran kelas dan angkatan...", 79);
			renderSebaranKelasDanAngkatan(pcLeft, data);
			updateDashboardProgress("Menampilkan sebaran sekolah/unit...", 80);
			renderSebaranSekolah(pcRight, data);
			updateDashboardProgress("Menampilkan prioritas penagihan...", 81);
			renderPrioritasPenagihan(pcBottom, data);
			updateDashboardProgress("Menampilkan dashboard tambahan dari laporan pembayaran...", 82);
			renderDashboardTambahanDariLaporan(pcBottom, data);
			updateDashboardProgress("Menampilkan rencana aksi pembayaran...", 83);
			renderRencanaAksiPembayaran(pcBottom, data);
		} catch (Exception e) {
			printDebug(e);
			appendHtml(DasboardPembayaranSekolah.this, "<div style='margin:8px; padding:14px; border-radius:14px; background:#fff7ed; "
					+ "border:1px solid #fed7aa; color:#9a3412; font-size:12px; font-weight:700;'>"
					+ "Dashboard pembayaran tambahan belum dapat dimuat. Aktifkan debug/debuh=true untuk melihat detail error.</div>");
		} finally {
			clearDashboardBusy();
		}
	}

	private void renderPembayaranHero(Component parent, final PembayaranDashboardData d) {
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
		titleBox.setStyle("max-width:760px;");
		titleBox.setParent(content);
		appendHtml(titleBox, "<div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;'>Payment Control Center</div>"
				+ "<div style='font-size:28px; line-height:1.15; font-weight:800; margin-top:6px;'>Dasbor Pembayaran & Tagihan</div>"
				+ "<div style='font-size:13px; opacity:.90; margin-top:8px;'>Pantau tagihan terbit, realisasi pembayaran, piutang, status lunas/cicilan, dan prioritas penagihan dalam satu layar. Klik angka untuk membuka popup detail datanya.</div>");
		String sekolahText = sk == null ? "Semua Sekolah" : safeNama(sk);
		String itemText = dashboardItemBiayaKeyword == null || dashboardItemBiayaKeyword.trim().length() == 0 ? "Semua Item Biaya" : dashboardItemBiayaKeyword.trim();
		String keywordText = dashboardKeyword == null || dashboardKeyword.trim().length() == 0 ? "Tanpa keyword" : dashboardKeyword.trim();
		appendHtml(titleBox, "<div style='margin-top:12px; display:flex; gap:8px; flex-wrap:wrap;'>"
				+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>TA: " + escapeHtml(dashboardTahunAjaran == null || dashboardTahunAjaran.trim().length() == 0 ? "Semua Tahun Ajaran" : dashboardTahunAjaran) + "</span>"
				+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>" + escapeHtml(sekolahText) + "</span>"
				+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>" + escapeHtml(itemText) + "</span>"
				+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>Cari: " + escapeHtml(keywordText) + "</span>"
				+ "</div>");

		org.zkoss.zul.Hbox numberBox = new org.zkoss.zul.Hbox();
		numberBox.setStyle("gap:10px; flex-wrap:wrap;");
		numberBox.setParent(content);
		createHeroNumberPembayaran(numberBox, "Total Tagihan", d.totalTagihan, "Detail Semua Tagihan", createAllTagihanCriteriaProvider());
		createHeroNumberPembayaran(numberBox, "Collection Rate", d.collectionRate + "%", "Detail Tagihan Lunas", createLunasCriteriaProvider());
	}

	private void renderPembayaranGlobalFilter(final Component parent) throws Exception {
		final org.zkoss.zul.Div filterContainer = new org.zkoss.zul.Div();
		filterContainer.setParent(parent);
		filterContainer.setStyle("margin-top:12px; padding:14px; background:#ffffff; border:1px solid #e8eef6; "
				+ "border-radius:16px; box-shadow:0 10px 26px rgba(15,23,42,0.04); box-sizing:border-box;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(filterContainer);
		toolbar.setStyle("border:0; background:transparent; padding:0; display:flex; flex-wrap:wrap; align-items:center; gap:8px;");

		new MyLabelAgakKecil("Tahun Ajaran:").setParent(toolbar);
		final Combobox cmbTahun = Common.generateTahunAjaran(null);
		cmbTahun.setCols(7);
		Common.selectComboItem(cmbTahun, dashboardTahunAjaran);
		cmbTahun.setParent(toolbar);

		new MyLabelAgakKecil("Sekolah:").setParent(toolbar);
		final Combobox cbSekolah = new Combobox();
		cbSekolah.setCols(8);
		cbSekolah.setReadonly(true);
		Yayasan selectedYayasan = SekolahUtil.getYayasan();
		Common.insertComboDanSemua(cbSekolah, new String[] { "nama", "jenisSekolah" }, "yayasan", Sekolah.class,
				"=" + Common.getBahasaConfig("sekolah") + "=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("yayasan", selectedYayasan));
		Common.selectComboItem(cbSekolah, sk);
		Sekolah selectedSekolah = SekolahUtil.getSekolah();
		if (selectedSekolah != null && selectedSekolah.getId() != null) {
			cbSekolah.setDisabled(true);
		}
		cbSekolah.setParent(toolbar);

		new MyLabelAgakKecil("Item Biaya:").setParent(toolbar);
		final Textbox txtItemBiaya = new Textbox();
		txtItemBiaya.setCols(13);
		txtItemBiaya.setValue(dashboardItemBiayaKeyword == null ? "" : dashboardItemBiayaKeyword);
		txtItemBiaya.setTooltiptext("Ketik nama atau kode item biaya sekolah. Contoh: SPP, Daftar Ulang, Buku");
		txtItemBiaya.setParent(toolbar);

		new MyLabelAgakKecil("Cari:").setParent(toolbar);
		final org.zkoss.zul.Textbox txtKeyword = new org.zkoss.zul.Textbox();
		txtKeyword.setCols(15);
		txtKeyword.setValue(dashboardKeyword == null ? "" : dashboardKeyword);
		txtKeyword.setTooltiptext("Cari nama siswa/calon siswa, sekolah, kelas, angkatan, atau item biaya");
		txtKeyword.setParent(toolbar);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan Dasbor", "/img/svg/search.svg");
		refresh.setTooltiptext("Refresh dashboard pembayaran berdasarkan filter global");
		refresh.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; padding:6px 14px; margin-left:4px;");
		refresh.setParent(toolbar);

		EventListener refreshListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				updateDashboardBusy("Memproses filter dashboard pembayaran...");
				dashboardTahunAjaran = normalizeDashboardTahunAjaran(cmbTahun.getValue());
				Object selectedSekolahObj = cbSekolah.getSelectedItem() == null ? null : cbSekolah.getSelectedItem().getValue();
				sk = selectedSekolahObj instanceof Sekolah && ((Sekolah) selectedSekolahObj).getId() != null ? (Sekolah) selectedSekolahObj : null;
				dashboardItemBiayaSekolah = null;
				dashboardItemBiayaKeyword = txtItemBiaya.getValue() == null ? "" : txtItemBiaya.getValue().trim();
				dashboardKeyword = txtKeyword.getValue() == null ? "" : txtKeyword.getValue().trim();
				Common.clear(DasboardPembayaranSekolah.this);
				DasboardPembayaranSekolah.this.init();
			}
		};
		refresh.addEventListener("onClick", refreshListener);
		txtKeyword.addEventListener("onOK", refreshListener);
		cmbTahun.addEventListener("onChange", refreshListener);
		cbSekolah.addEventListener("onChange", refreshListener);
		txtItemBiaya.addEventListener("onOK", refreshListener);
	}

	private void renderDashboardProcessingInfo(Component parent, PembayaranDashboardData d) {
		if (d == null) {
			return;
		}
		StringBuilder html = new StringBuilder();
		html.append("<div style='margin-top:12px; padding:12px 14px; border-radius:16px; background:#eff6ff; border:1px solid #bfdbfe; box-shadow:0 8px 20px rgba(37,99,235,.08);'>");
		html.append("<div style='display:flex; justify-content:space-between; gap:12px; flex-wrap:wrap; align-items:center;'>");
		html.append("<div><div style='font-size:13px; font-weight:900; color:#1e3a8a;'>Status Pemrosesan Dashboard</div>");
		html.append("<div style='font-size:11px; color:#475569; margin-top:3px;'>");
		html.append(d.fastQuery ? "Mode cepat aktif: aggregate dan ringkasan dihitung langsung di database." : "Mode fallback aktif: sebagian data dihitung melalui Criteria lama.");
		html.append("</div></div>");
		html.append("<div style='padding:7px 10px; border-radius:999px; background:#ffffff; color:#1d4ed8; font-size:11px; font-weight:900;'>Selesai dalam " + d.dashboardLoadMillis + " ms</div>");
		html.append("</div>");
		if (d.processingSteps != null && !d.processingSteps.isEmpty()) {
			html.append("<div style='margin-top:9px; display:flex; gap:6px; flex-wrap:wrap;'>");
			for (Iterator it = d.processingSteps.iterator(); it.hasNext();) {
				String step = (String) it.next();
				html.append("<span style='font-size:10px; font-weight:800; color:#1e40af; background:#ffffff; border:1px solid #dbeafe; border-radius:999px; padding:5px 8px;'>");
				html.append(escapeHtml(step));
				html.append("</span>");
			}
			html.append("</div>");
		}
		html.append("</div>");
		appendHtml(parent, html.toString());
	}

	private void renderPembayaranMetricCards(Component parent, PembayaranDashboardData d) {
		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px;");
		wrap.setParent(parent);

		createMetricCardPembayaran(wrap, "Nominal Tagihan", formatMoney(d.totalNominal), "Total tagihan terbit", "#dbeafe", "#1e40af", "Σ", "Detail Semua Tagihan", createAllTagihanCriteriaProvider());
		createMetricCardPembayaran(wrap, "Sudah Dibayar", formatMoney(d.totalDibayar), "Realisasi pembayaran", "#dcfce7", "#166534", "✓", "Detail Tagihan Sudah Dibayar", createSudahBayarCriteriaProvider());
		createMetricCardPembayaran(wrap, "Sisa Piutang", formatMoney(d.totalPiutang), "Tagihan belum tertagih", "#fee2e2", "#991b1b", "!", "Detail Tagihan Masih Piutang", createPiutangCriteriaProvider());
		createMetricCardPembayaran(wrap, "Lunas", String.valueOf(d.lunas), "Tagihan selesai", "#ecfdf5", "#047857", "★", "Detail Tagihan Lunas", createLunasCriteriaProvider());
		createMetricCardPembayaran(wrap, "Belum Bayar", String.valueOf(d.belumBayar), "Belum ada pembayaran", "#fef3c7", "#92400e", "0", "Detail Belum Bayar", createBelumBayarCriteriaProvider());
		createMetricCardPembayaran(wrap, "Cicilan", String.valueOf(d.cicilan), "Sudah bayar sebagian", "#ede9fe", "#5b21b6", "↗", "Detail Cicilan / Parsial", createCicilanCriteriaProvider());
		createMetricCardPembayaran(wrap, "Lebih Bayar", String.valueOf(d.lebihBayar), "Pembayaran melebihi tagihan", "#cffafe", "#155e75", "+", "Detail Lebih Bayar", createLebihBayarCriteriaProvider());
		createMetricCardPembayaran(wrap, "Peserta Terpantau", String.valueOf(d.totalPeserta), "Siswa/calon siswa unik", "#f1f5f9", "#334155", "☷", "Detail Semua Tagihan", createAllTagihanCriteriaProvider());
	}

	private void renderFunnelPembayaran(Component parent, PembayaranDashboardData d) {
		Panelchildren pch = createModernPanelPembayaran(" Tagihan dan Pembayaran", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px; line-height:1.55;'>"
				+ " menunjukkan pergerakan dari tagihan terbit, mulai dibayar, pembayaran parsial, sampai lunas. Setiap nilai dapat diklik untuk membuka popup detail.</div>");
		int max = getMaxValue(new int[] { d.totalTagihan, d.sudahBayar, d.cicilan, d.lunas, d.belumBayar });
		renderFunnelRowPembayaran(pch, "Tagihan Terbit", d.totalTagihan, max, "#2563eb", "Detail Semua Tagihan", createAllTagihanCriteriaProvider());
		renderFunnelRowPembayaran(pch, "Sudah Ada Pembayaran", d.sudahBayar, max, "#16a34a", "Detail Tagihan Sudah Dibayar", createSudahBayarCriteriaProvider());
		renderFunnelRowPembayaran(pch, "Pembayaran Parsial / Cicilan", d.cicilan, max, "#7c3aed", "Detail Cicilan / Parsial", createCicilanCriteriaProvider());
		renderFunnelRowPembayaran(pch, "Lunas", d.lunas, max, "#0891b2", "Detail Tagihan Lunas", createLunasCriteriaProvider());
		renderFunnelRowPembayaran(pch, "Belum Bayar", d.belumBayar, max, "#f59e0b", "Detail Belum Bayar", createBelumBayarCriteriaProvider());
	}

	private void renderRisikoTagihan(Component parent, PembayaranDashboardData d) {
		Panelchildren pch = createModernPanelPembayaran("Analitik Risiko Piutang", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "membantu membaca kesehatan pembayaran: collection rate, outstanding ratio, zero payment, dan cicilan yang perlu ditindaklanjuti.</div>");
		renderMiniGaugePembayaran(pch, "Collection Rate", d.collectionRate, "Dibayar dibanding nominal tagihan.", "#16a34a", "Detail Tagihan Lunas", createLunasCriteriaProvider());
		renderMiniGaugePembayaran(pch, "Outstanding Ratio", d.outstandingRatio, "Sisa piutang dibanding nominal tagihan.", "#dc2626", "Detail Tagihan Masih Piutang", createPiutangCriteriaProvider());
		renderMiniGaugePembayaran(pch, "Zero Payment", percent(d.belumBayar, d.totalTagihan), "Proporsi tagihan tanpa pembayaran.", "#f59e0b", "Detail Belum Bayar", createBelumBayarCriteriaProvider());
		renderMiniGaugePembayaran(pch, "Partial Payment", percent(d.cicilan, d.totalTagihan), "Proporsi pembayaran sebagian.", "#7c3aed", "Detail Cicilan / Parsial", createCicilanCriteriaProvider());
	}

	private void renderSebaranItemBiaya(Component parent, PembayaranDashboardData d) {
		Panelchildren pch = createModernPanelPembayaran("Sebaran Piutang per Item Biaya", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:10px;'>Item biaya dengan piutang terbesar perlu menjadi prioritas rekonsiliasi dan penagihan.</div>");
		renderCounterSummary(pch, d.perItemBiaya, "Belum ada data item biaya pada filter ini.", 8, "#2563eb", "item");
	}

	private void renderSebaranKelasDanAngkatan(Component parent, PembayaranDashboardData d) {
		Panelchildren pch = createModernPanelPembayaran("Sebaran per Kelas & Angkatan", parent);
		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		wrap.setParent(pch);
		org.zkoss.zul.Div left = new org.zkoss.zul.Div();
		left.setStyle("flex:1 1 300px;");
		left.setParent(wrap);
		appendHtml(left, "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Kelas dengan piutang tertinggi</div>");
		renderCounterSummary(left, d.perKelas, "Belum ada data kelas.", 7, "#7c3aed", "kelas");
		org.zkoss.zul.Div right = new org.zkoss.zul.Div();
		right.setStyle("flex:1 1 300px;");
		right.setParent(wrap);
		appendHtml(right, "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Angkatan dengan piutang tertinggi</div>");
		renderCounterSummary(right, d.perAngkatan, "Belum ada data angkatan.", 7, "#0891b2", "angkatan");
	}

	private void renderSebaranSekolah(Component parent, PembayaranDashboardData d) {
		Panelchildren pch = createModernPanelPembayaran("Sebaran per Sekolah / Unit", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:10px;'>Ringkasan tagihan, pembayaran, dan piutang per sekolah/unit.</div>");
		renderCounterSummary(pch, d.perSekolah, "Belum ada data sekolah pada filter ini.", 8, "#16a34a", "sekolah");
	}

	private void renderPrioritasPenagihan(Component parent, PembayaranDashboardData d) {
		Panelchildren pch = createModernPanelPembayaran("Watchlist Prioritas Penagihan", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "Daftar ini mengambil sample tagihan dengan sisa piutang terbesar agar tim keuangan cepat menentukan follow-up.</div>");
		if (d.topPiutang == null || d.topPiutang.isEmpty()) {
			appendEmptyState(pch, "Belum ada tagihan piutang pada filter saat ini.");
			return;
		}
		Grid grid = new Grid();
		grid.setSclass("dgrid fgrid");
		grid.setWidth("100%");
		grid.setStyle("border:0; background:#ffffff;");
		grid.setParent(pch);
		Columns columns = new Columns();
		columns.setParent(grid);
		addColumn(columns, "Peserta", "22%", "left");
		addColumn(columns, "Sekolah/Kelas", "22%", "left");
		addColumn(columns, "Item Biaya", "20%", "left");
		addColumn(columns, "Nominal", "12%", "right");
		addColumn(columns, "Dibayar", "12%", "right");
		addColumn(columns, "Piutang", "12%", "right");
		Rows rows = new Rows();
		rows.setParent(grid);
		for (Iterator it = d.topPiutang.iterator(); it.hasNext();) {
			final Tagihan tagihan = (Tagihan) it.next();
			MyFormRow row = new MyFormRow();
			row.setParent(rows);
			addGridCell(row, getPesertaName(tagihan));
			addGridCell(row, getSekolahKelasText(tagihan));
			addGridCell(row, getObjectName(getValue(tagihan, "getItemBiayaSekolah")));
			addGridCellRight(row, formatMoney(getNumberValue(tagihan, "getNominal")));
			addGridCellRight(row, formatMoney(getNumberValue(tagihan, "getDibayar")));
			A detail = new A(formatMoney(getPiutang(tagihan)));
			detail.setStyle("font-size:11px; font-weight:900; color:#dc2626; text-decoration:none; cursor:pointer;");
			detail.setTooltiptext("Klik untuk melihat detail piutang peserta ini");
			detail.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					showDetailTagihan("Detail Piutang - " + getPesertaName(tagihan), createSingleTagihanCriteriaProvider(tagihan));
				}
			});
			row.appendChild(detail);
		}
	}

	private void renderDashboardTambahanDariLaporan(Component parent, PembayaranDashboardData d) {
		Panelchildren pch = createModernPanelPembayaran("Dashboard Tambahan dari Laporan Rincian Pembayaran", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "mengombinasikan pola query dari LaporanRincianPembayaranSiswa: pembayaran per cara bayar, tren bulan pembayaran, item pembayaran, dan pembayar terbesar. Filter tahun ajaran, sekolah, item biaya, dan keyword tetap ikut diterapkan.</div>");
		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		wrap.setParent(pch);

		org.zkoss.zul.Div cara = new org.zkoss.zul.Div();
		cara.setStyle("flex:1 1 420px;");
		cara.setParent(wrap);
		appendHtml(cara, "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Realisasi per Cara Pembayaran</div>");
		renderCounterSummary(cara, d.perCaraBayar, "Belum ada data cara pembayaran.", 8, "#0ea5e9", "caraBayar");

		org.zkoss.zul.Div bulan = new org.zkoss.zul.Div();
		bulan.setStyle("flex:1 1 420px;");
		bulan.setParent(wrap);
		appendHtml(bulan, "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Tren Pembayaran per Bulan</div>");
		renderCounterSummary(bulan, d.perBulanPembayaran, "Belum ada data tren bulanan.", 8, "#16a34a", "bulanBayar");

		org.zkoss.zul.Div wrap2 = new org.zkoss.zul.Div();
		wrap2.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px;");
		wrap2.setParent(pch);
		org.zkoss.zul.Div item = new org.zkoss.zul.Div();
		item.setStyle("flex:1 1 420px;");
		item.setParent(wrap2);
		appendHtml(item, "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Top Item Realisasi Pembayaran</div>");
		renderCounterSummary(item, d.perItemPembayaran, "Belum ada realisasi pembayaran per item.", 8, "#7c3aed", "itemBayar");

		org.zkoss.zul.Div siswa = new org.zkoss.zul.Div();
		siswa.setStyle("flex:1 1 420px;");
		siswa.setParent(wrap2);
		appendHtml(siswa, "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Top Pembayar / Setoran Terbesar</div>");
		renderCounterSummary(siswa, d.topPembayar, "Belum ada data pembayar.", 8, "#f59e0b", "pembayar");
	}

	private void renderRencanaAksiPembayaran(Component parent, PembayaranDashboardData d) {
		Panelchildren pch = createModernPanelPembayaran("Rencana Aksi Keuangan Sekolah", parent);
		String html = "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:12px;'>"
				+ buildActionCard("1", "Prioritaskan Piutang Besar", "Gunakan watchlist untuk menghubungi peserta dengan sisa tagihan terbesar terlebih dahulu.", "#fee2e2", "#991b1b")
				+ buildActionCard("2", "Rekonsiliasi Cicilan", "Periksa pembayaran parsial agar tidak tertahan terlalu lama sebelum dinyatakan lunas.", "#ede9fe", "#5b21b6")
				+ buildActionCard("3", "Validasi Lebih Bayar", "Cek tagihan lebih bayar untuk proses koreksi, pengembalian, atau kompensasi tagihan berikutnya.", "#cffafe", "#155e75")
				+ buildActionCard("4", "Pantau Item Biaya Dominan", "Fokus pada item biaya dengan piutang terbesar karena berdampak langsung pada arus kas.", "#dcfce7", "#166534")
				+ "</div>";
		appendHtml(pch, html);
	}

	private PembayaranDashboardData loadPembayaranDashboardData() {
		PembayaranDashboardData d = new PembayaranDashboardData();
		long start = System.currentTimeMillis();
		try {
			d.fastQuery = true;
			addDashboardStep(d, "mulai load dashboard mode SQL rinci");
			updateDashboardProgress("Menghitung agregat tagihan sesuai query DasboardPiutangRinciSekolah...", 14);

			/*
			 * Jangan lagi membaca aggregate native SQL menggunakan Object[] index panjang.
			 * Pada beberapa kombinasi Hibernate lama + PostgreSQL, metadata kolom aggregate
			 * dapat salah terbaca sehingga nilai SUM nominal ikut masuk ke field count/status.
			 * Akibatnya kartu Lunas/Belum Bayar/Cicilan/Lebih Bayar bisa tampil sama dengan
			 * nominal uang. Field utama sekarang dihitung per ekspresi tunggal agar aman.
			 */
			loadNativeAggregateFields(d);
			d.collectionRate = percentDouble(d.totalDibayar, d.totalNominal);
			d.outstandingRatio = percentDouble(d.totalPiutang, d.totalNominal);
			addDashboardStep(d, "agregat native SQL aman");

			updateDashboardProgress("Menghitung ringkasan per item biaya...", 24);
			loadNativeSummary(d.perItemBiaya, "COALESCE(ib.nama,'Tidak diketahui item biaya')", "COALESCE(ib.nama,'Tidak diketahui item biaya')", "item biaya");
			addDashboardStep(d, "item biaya native");

			updateDashboardProgress("Menghitung ringkasan per sekolah/unit...", 34);
			loadNativeSummary(d.perSekolah, "COALESCE(sek.nama,'Tidak diketahui sekolah')", "COALESCE(sek.nama,'Tidak diketahui sekolah')", "sekolah/unit");
			addDashboardStep(d, "sekolah/unit native");

			updateDashboardProgress("Menghitung ringkasan per kelas...", 44);
			loadNativeSummary(d.perKelas, "COALESCE(ks.nama, CAST(t.kelas_siswa_id AS TEXT), 'Tidak diketahui kelas')", "COALESCE(ks.nama, CAST(t.kelas_siswa_id AS TEXT), 'Tidak diketahui kelas')", "kelas");
			addDashboardStep(d, "kelas native");

			updateDashboardProgress("Menghitung ringkasan per angkatan...", 54);
			loadNativeSummary(d.perAngkatan, "COALESCE(CAST(t.tahunangkatan AS TEXT),'Tidak diketahui angkatan')", "COALESCE(CAST(t.tahunangkatan AS TEXT),'Tidak diketahui angkatan')", "angkatan");
			addDashboardStep(d, "angkatan native");

			updateDashboardProgress("Mengambil watchlist piutang terbesar...", 62);
			loadFastTopPiutang(d);
			addDashboardStep(d, "watchlist top piutang");

			loadNativeLaporanPembayaranSummaries(d);

		} catch (Exception e) {
			printDebug(e);
			d = new PembayaranDashboardData();
			d.fastQuery = false;
			addDashboardStep(d, "fallback criteria lama setelah SQL rinci gagal");
			try {
				loadPembayaranDashboardDataFallback(d);
			} catch (Exception ex) {
				printDebug(ex);
			}
		} finally {
			d.dashboardLoadMillis = System.currentTimeMillis() - start;
			clearDashboardBusy();
		}
		return d;
	}

	private void loadPembayaranDashboardDataFallback(PembayaranDashboardData d) throws Exception {
		Object[] aggregate = getTagihanAggregate();
		if (aggregate != null) {
			d.totalTagihan = aggregate[0] == null ? 0 : ((Number) aggregate[0]).intValue();
			d.totalNominal = aggregate[1] == null ? 0.0 : ((Number) aggregate[1]).doubleValue();
			d.totalDibayar = aggregate[2] == null ? 0.0 : ((Number) aggregate[2]).doubleValue();
		}
		d.totalPiutang = Math.max(0.0, d.totalNominal - d.totalDibayar);
		d.sudahBayar = countTagihan(createSudahBayarCriteriaProvider());
		d.lunas = countTagihan(createLunasCriteriaProvider());
		d.belumBayar = countTagihan(createBelumBayarCriteriaProvider());
		d.cicilan = countTagihan(createCicilanCriteriaProvider());
		d.lebihBayar = countTagihan(createLebihBayarCriteriaProvider());
		d.collectionRate = percentDouble(d.totalDibayar, d.totalNominal);
		d.outstandingRatio = percentDouble(d.totalPiutang, d.totalNominal);

		Criteria sampleCriteria = buildBaseTagihanCriteria(true);
		sampleCriteria.setMaxResults(DASHBOARD_SAMPLE_LIMIT);
		@SuppressWarnings("unchecked")
		List<Tagihan> sampleRows = sampleCriteria.list();
		analyzePembayaranRows(d, sampleRows);
	}

	private String dashboardNativeNettoExpr() {
		return "(COALESCE(t.nominal,0)-COALESCE(t.diskon,0))";
	}

	private String dashboardNativeSisaExpr() {
		return "(COALESCE(t.nominal,0)-COALESCE(t.diskon,0)-COALESCE(t.dibayar,0))";
	}

	private String buildDashboardNativeSqlBase() {
		String sqlBase = " FROM sekolah.tagihan t "
				+ " LEFT JOIN sekolah.siswa s ON t.siswa_id = s.id "
				+ " LEFT JOIN sekolah.calon_siswa c ON t.calon_siswa_id = c.id "
				+ " LEFT JOIN sekolah.item_biaya_sekolah ib ON t.item_biaya_id = ib.id "
				+ " LEFT JOIN sekolah.sekolah sek ON (sek.id = s.sekolah_id OR sek.id = c.sekolah_id) "
				+ " LEFT JOIN sekolah.kelas ks ON t.kelas_siswa_id = ks.id "
				+ " WHERE (COALESCE(t.nominal,0) > 0.1 OR COALESCE(t.dibayar,0) > 0.1) ";

		if (sk != null && sk.getId() != null) {
			sqlBase += " AND (s.sekolah_id = :dashSekolahId OR c.sekolah_id = :dashSekolahId) ";
		}
		if (dashboardTahunAjaran != null && dashboardTahunAjaran.trim().length() > 0) {
			sqlBase += " AND t.tahunajaran = :dashTahunAjaran ";
		}
		if (hasDashboardItemBiayaKeyword()) {
			sqlBase += " AND (ib.nama ILIKE :dashItemBiayaKeyword OR ib.kode ILIKE :dashItemBiayaKeyword) ";
		}

		String c = dashboardKeyword == null ? "" : dashboardKeyword.trim();
		if (c.length() > 0) {
			sqlBase += " AND (s.nama_siswa ILIKE :dashKeyword "
					+ " OR c.nama_siswa ILIKE :dashKeyword "
					+ " OR s.nomor_induk ILIKE :dashKeyword "
					+ " OR c.nomor_induk ILIKE :dashKeyword "
					+ " OR ib.nama ILIKE :dashKeyword "
					+ " OR sek.nama ILIKE :dashKeyword) ";
		}
		return sqlBase;
	}

	private void bindDashboardNativeParameters(SQLQuery query) {
		if (query == null) {
			return;
		}
		if (sk != null && sk.getId() != null) {
			query.setParameter("dashSekolahId", sk.getId());
		}
		if (dashboardTahunAjaran != null && dashboardTahunAjaran.trim().length() > 0) {
			query.setString("dashTahunAjaran", dashboardTahunAjaran.trim());
		}
		if (hasDashboardItemBiayaKeyword()) {
			query.setString("dashItemBiayaKeyword", "%" + dashboardItemBiayaKeyword.trim() + "%");
		}
		String c = dashboardKeyword == null ? "" : dashboardKeyword.trim();
		if (c.length() > 0) {
			query.setString("dashKeyword", "%" + c + "%");
		}
	}


	private void loadNativeAggregateFields(PembayaranDashboardData d) {
		if (d == null) {
			return;
		}

		String sisa = dashboardNativeSisaExpr();
		String netto = dashboardNativeNettoExpr();

		d.totalTagihan = getNativeCountValue("");
		d.totalNominal = getNativeSumValue(netto, "");
		d.totalDibayar = getNativeSumValue("COALESCE(t.dibayar,0)", "");

		/*
		 * Piutang ditampilkan sebagai outstanding positif saja.
		 * Lebih bayar tetap dihitung di kartu sendiri dan tidak mengurangi total piutang.
		 */
		d.totalPiutang = getNativeSumValue("(CASE WHEN " + sisa + " > 0.1 THEN " + sisa + " ELSE 0 END)", "");

		d.sudahBayar = getNativeCountValue(" AND COALESCE(t.dibayar,0) > 0.1 ");
		d.lunas = getNativeCountValue(" AND " + netto + " > 0.1 AND " + sisa + " <= 0.1 ");
		d.belumBayar = getNativeCountValue(" AND " + sisa + " > 0.1 AND COALESCE(t.dibayar,0) <= 0.1 ");
		d.cicilan = getNativeCountValue(" AND " + sisa + " > 0.1 AND COALESCE(t.dibayar,0) > 0.1 ");
		d.lebihBayar = getNativeCountValue(" AND " + sisa + " < -0.1 ");

		d.totalPeserta = getNativeDistinctCountValue("s.id") + getNativeDistinctCountValue("c.id");
	}

	private int getNativeCountValue(String extraWhere) {
		try {
			String sql = "SELECT COUNT(t.id) " + buildDashboardNativeSqlBase()
					+ (extraWhere == null ? "" : extraWhere);
			SQLQuery query = HibernateUtil.currentSession().createSQLQuery(sql);
			bindDashboardNativeParameters(query);
			return toInt(query.uniqueResult());
		} catch (Exception e) {
			printDebug(e);
		}
		return 0;
	}

	private double getNativeSumValue(String expression, String extraWhere) {
		try {
			String sql = "SELECT COALESCE(SUM(" + expression + "),0) "
					+ buildDashboardNativeSqlBase()
					+ (extraWhere == null ? "" : extraWhere);
			SQLQuery query = HibernateUtil.currentSession().createSQLQuery(sql);
			bindDashboardNativeParameters(query);
			return toDouble(query.uniqueResult());
		} catch (Exception e) {
			printDebug(e);
		}
		return 0.0;
	}

	private int getNativeDistinctCountValue(String expression) {
		try {
			String sql = "SELECT COUNT(DISTINCT " + expression + ") " + buildDashboardNativeSqlBase();
			SQLQuery query = HibernateUtil.currentSession().createSQLQuery(sql);
			bindDashboardNativeParameters(query);
			return toInt(query.uniqueResult());
		} catch (Exception e) {
			printDebug(e);
		}
		return 0;
	}


	private Object[] getNativeTagihanAggregate() {
		String sql = "SELECT COUNT(t.id), "
				+ " COALESCE(SUM(" + dashboardNativeNettoExpr() + "),0), "
				+ " COALESCE(SUM(COALESCE(t.dibayar,0)),0), "
				+ " COALESCE(SUM(" + dashboardNativeSisaExpr() + "),0), "
				+ " COALESCE(SUM(COALESCE(t.diskon,0)),0), "
				+ " COALESCE(SUM(CASE WHEN COALESCE(t.dibayar,0) > 0.1 THEN 1 ELSE 0 END),0), "
				+ " COALESCE(SUM(CASE WHEN " + dashboardNativeNettoExpr() + " > 0.1 AND " + dashboardNativeSisaExpr() + " <= 0.1 THEN 1 ELSE 0 END),0), "
				+ " COALESCE(SUM(CASE WHEN " + dashboardNativeSisaExpr() + " > 0.1 AND COALESCE(t.dibayar,0) <= 0.1 THEN 1 ELSE 0 END),0), "
				+ " COALESCE(SUM(CASE WHEN " + dashboardNativeSisaExpr() + " > 0.1 AND COALESCE(t.dibayar,0) > 0.1 THEN 1 ELSE 0 END),0), "
				+ " COALESCE(SUM(CASE WHEN " + dashboardNativeSisaExpr() + " < -0.1 THEN 1 ELSE 0 END),0), "
				+ " COUNT(DISTINCT s.id), COUNT(DISTINCT c.id) "
				+ buildDashboardNativeSqlBase();
		SQLQuery query = HibernateUtil.currentSession().createSQLQuery(sql);
		bindDashboardNativeParameters(query);
		return (Object[]) query.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	private void loadNativeSummary(Map<String, SummaryCounter> target, String labelExpr, String groupExpr, String labelFallback) {
		if (target == null) {
			return;
		}
		try {
			String sql = "SELECT " + labelExpr + " AS label_group, COUNT(t.id), "
					+ " COALESCE(SUM(" + dashboardNativeNettoExpr() + "),0), "
					+ " COALESCE(SUM(COALESCE(t.dibayar,0)),0), "
					+ " COALESCE(SUM(" + dashboardNativeSisaExpr() + "),0) "
					+ buildDashboardNativeSqlBase()
					+ " GROUP BY " + groupExpr
					+ " ORDER BY COALESCE(SUM(" + dashboardNativeSisaExpr() + "),0) DESC LIMIT 30";
			SQLQuery query = HibernateUtil.currentSession().createSQLQuery(sql);
			bindDashboardNativeParameters(query);
			List<Object[]> rows = query.list();
			if (rows == null) {
				return;
			}
			for (Iterator it = rows.iterator(); it.hasNext();) {
				Object[] row = (Object[]) it.next();
				SummaryCounter c = new SummaryCounter();
				c.key = null;
				c.label = safeString(row[0], "Tidak diketahui " + labelFallback);
				c.count = toInt(row[1]);
				c.nominal = toDouble(row[2]);
				c.dibayar = toDouble(row[3]);
				c.piutang = Math.max(0.0, toDouble(row[4]));
				target.put(c.label, c);
			}
		} catch (Exception e) {
			printDebug(e);
		}
	}


	private String buildPembayaranNativeSqlBase() {
		String sqlBase = " FROM sekolah.pembayaran_siswa_detail a "
				+ " INNER JOIN sekolah.pembayaran_siswa b ON (a.pembayaran_siswa_id = b.id) "
				+ " LEFT JOIN sekolah.siswa s ON (b.siswa_id = s.id) "
				+ " LEFT JOIN sekolah.calon_siswa c ON (b.calon_siswa_id = c.id) "
				+ " LEFT JOIN sekolah.item_biaya_sekolah ib ON (ib.id = a.item_biaya_id) "
				+ " LEFT JOIN bank_host bh ON (bh.id = b.bank_host_id) "
				+ " LEFT JOIN sekolah.akun_pembayaran_siswa aps ON (aps.id = b.akun_pembayaran_siswa_id) "
				+ " LEFT JOIN sekolah.tagihan t ON (t.pembayaran_siswa_detail_id = a.id) "
				+ " LEFT JOIN sekolah.pengaturan_biaya p ON (t.pengaturan_biaya = p.id) "
				+ " WHERE COALESCE(a.nominal,0) > 0.1 ";
		if (dashboardTahunAjaran != null && dashboardTahunAjaran.trim().length() > 0) {
			sqlBase += " AND (p.tahunajaran = :dashTahunAjaran OR t.tahunajaran = :dashTahunAjaran) ";
		}
		if (sk != null && sk.getId() != null) {
			sqlBase += " AND (s.sekolah_id = :dashSekolahId OR c.sekolah_id = :dashSekolahId) ";
		}
		if (hasDashboardItemBiayaKeyword()) {
			sqlBase += " AND (ib.nama ILIKE :dashItemBiayaKeyword OR ib.kode ILIKE :dashItemBiayaKeyword) ";
		}
		String ckey = dashboardKeyword == null ? "" : dashboardKeyword.trim();
		if (ckey.length() > 0) {
			sqlBase += " AND (s.nama_siswa ILIKE :dashKeyword OR c.nama_siswa ILIKE :dashKeyword "
					+ " OR s.nomor_induk ILIKE :dashKeyword OR c.nomor_induk ILIKE :dashKeyword "
					+ " OR ib.nama ILIKE :dashKeyword OR ib.kode ILIKE :dashKeyword "
					+ " OR aps.nama_pembayaran ILIKE :dashKeyword OR bh.nama ILIKE :dashKeyword) ";
		}
		return sqlBase;
	}

	private void bindPembayaranNativeParameters(SQLQuery query) {
		bindDashboardNativeParameters(query);
	}

	@SuppressWarnings("unchecked")
	private void loadNativePembayaranSummary(Map<String, SummaryCounter> target, String labelExpr, String groupExpr,
			String labelFallback, int limit) {
		if (target == null) {
			return;
		}
		try {
			String sql = "SELECT " + labelExpr + " AS label_group, COUNT(a.id), COALESCE(SUM(COALESCE(a.nominal,0)),0) "
					+ buildPembayaranNativeSqlBase()
					+ " GROUP BY " + groupExpr
					+ " ORDER BY COALESCE(SUM(COALESCE(a.nominal,0)),0) DESC LIMIT " + limit;
			SQLQuery query = HibernateUtil.currentSession().createSQLQuery(sql);
			bindPembayaranNativeParameters(query);
			List<Object[]> rows = query.list();
			if (rows == null) {
				return;
			}
			for (Iterator it = rows.iterator(); it.hasNext();) {
				Object[] row = (Object[]) it.next();
				SummaryCounter c = new SummaryCounter();
				c.key = null;
				c.label = safeString(row[0], "Tidak diketahui " + labelFallback);
				c.count = toInt(row[1]);
				c.nominal = toDouble(row[2]);
				c.dibayar = toDouble(row[2]);
				c.piutang = toDouble(row[2]);
				target.put(c.label, c);
			}
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void loadNativeLaporanPembayaranSummaries(PembayaranDashboardData d) {
		if (d == null) {
			return;
		}
		updateDashboardProgress("Menghitung ringkasan realisasi pembayaran per cara bayar...", 66);
		loadNativePembayaranSummary(d.perCaraBayar,
				"COALESCE(bh.nama, aps.nama_pembayaran, 'Tanpa Cara Bayar')",
				"COALESCE(bh.nama, aps.nama_pembayaran, 'Tanpa Cara Bayar')", "cara bayar", 15);
		addDashboardStep(d, "cara bayar pembayaran native");

		updateDashboardProgress("Menghitung tren pembayaran per bulan...", 68);
		loadNativePembayaranSummary(d.perBulanPembayaran,
				"COALESCE(CAST(b.tahun AS TEXT),'') || '-' || LPAD(COALESCE(CAST(b.bulan AS TEXT),'0'),2,'0')",
				"COALESCE(CAST(b.tahun AS TEXT),'') || '-' || LPAD(COALESCE(CAST(b.bulan AS TEXT),'0'),2,'0')", "bulan pembayaran", 18);
		addDashboardStep(d, "tren bulan pembayaran native");

		updateDashboardProgress("Menghitung realisasi pembayaran per item biaya...", 70);
		loadNativePembayaranSummary(d.perItemPembayaran,
				"COALESCE(ib.nama,'Tidak diketahui item biaya')",
				"COALESCE(ib.nama,'Tidak diketahui item biaya')", "item pembayaran", 15);
		addDashboardStep(d, "item pembayaran native");

		updateDashboardProgress("Menghitung pembayar terbesar...", 71);
		loadNativePembayaranSummary(d.topPembayar,
				"COALESCE(s.nama_siswa, c.nama_siswa, 'Tanpa Nama')",
				"COALESCE(s.nama_siswa, c.nama_siswa, 'Tanpa Nama')", "pembayar", 12);
		addDashboardStep(d, "top pembayar native");
	}


	private Object[] getFastTagihanAggregate() {
		String hql = "select count(t.id), sum(t.nominal), sum(t.dibayar), "
				+ "sum(case when t.dibayar > 0.1 then 1 else 0 end), "
				+ "sum(case when t.dibayar >= t.nominal then 1 else 0 end), "
				+ "sum(case when t.dibayar is null or t.dibayar <= 0.1 then 1 else 0 end), "
				+ "sum(case when t.dibayar > 0.1 and t.dibayar < t.nominal then 1 else 0 end), "
				+ "sum(case when t.dibayar > t.nominal then 1 else 0 end), "
				+ "count(distinct dashSiswa.id), count(distinct dashCalonSiswa.id) "
				+ buildDashboardHqlFromWhere("t");
		org.hibernate.Query query = HibernateUtil.currentSession().createQuery(hql);
		bindDashboardHqlParameters(query);
		return (Object[]) query.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	private void loadFastSummaryAssociation(Map<String, SummaryCounter> target, String associationProperty, String labelFallback) {
		if (target == null) {
			return;
		}
		try {
			String hql = "select grp, count(t.id), sum(t.nominal), sum(t.dibayar) from "
					+ Tagihan.class.getName() + " t left join t." + associationProperty + " grp "
					+ buildDashboardHqlJoins("t") + buildDashboardHqlWhere("t") + " group by grp";
			org.hibernate.Query query = HibernateUtil.currentSession().createQuery(hql);
			bindDashboardHqlParameters(query);
			List<Object[]> rows = query.list();
			if (rows == null) {
				return;
			}
			for (Iterator it = rows.iterator(); it.hasNext();) {
				Object[] row = (Object[]) it.next();
				Object key = row[0];
				double nominal = toDouble(row[2]);
				double dibayar = toDouble(row[3]);
				SummaryCounter c = new SummaryCounter();
				c.key = key;
				c.label = key == null ? "Tidak diketahui" : getObjectName(key);
				if (c.label == null || c.label.trim().length() == 0 || "Tidak diketahui".equals(c.label)) {
					c.label = "Tidak diketahui " + labelFallback;
				}
				c.count = toInt(row[1]);
				c.nominal = nominal;
				c.dibayar = dibayar;
				c.piutang = Math.max(0.0, nominal - dibayar);
				target.put(c.label, c);
			}
		} catch (Exception e) {
			printDebug(e);
		}
	}

	@SuppressWarnings("unchecked")
	private void loadFastSummarySimple(Map<String, SummaryCounter> target, String propertyName, String labelFallback) {
		if (target == null) {
			return;
		}
		try {
			String hql = "select t." + propertyName + ", count(t.id), sum(t.nominal), sum(t.dibayar) "
					+ buildDashboardHqlFromWhere("t") + " group by t." + propertyName;
			org.hibernate.Query query = HibernateUtil.currentSession().createQuery(hql);
			bindDashboardHqlParameters(query);
			List<Object[]> rows = query.list();
			if (rows == null) {
				return;
			}
			for (Iterator it = rows.iterator(); it.hasNext();) {
				Object[] row = (Object[]) it.next();
				Object key = row[0];
				double nominal = toDouble(row[2]);
				double dibayar = toDouble(row[3]);
				SummaryCounter c = new SummaryCounter();
				c.key = key;
				c.label = safeString(key, "Tidak diketahui " + labelFallback);
				c.count = toInt(row[1]);
				c.nominal = nominal;
				c.dibayar = dibayar;
				c.piutang = Math.max(0.0, nominal - dibayar);
				target.put(c.label, c);
			}
		} catch (Exception e) {
			printDebug(e);
		}
	}

	@SuppressWarnings("unchecked")
	private void loadFastTopPiutang(PembayaranDashboardData d) {
		try {
			String hql = "select t " + buildDashboardHqlFromWhere("t")
					+ " and (coalesce(t.nominal,0) - coalesce(t.diskon,0) - coalesce(t.dibayar,0)) > 0.1 "
					+ " order by (coalesce(t.nominal,0) - coalesce(t.diskon,0) - coalesce(t.dibayar,0)) desc, t.id desc";
			org.hibernate.Query query = HibernateUtil.currentSession().createQuery(hql);
			bindDashboardHqlParameters(query);
			query.setMaxResults(DASHBOARD_TOP_PIUTANG_LIMIT);
			d.topPiutang = query.list();
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private String buildDashboardHqlFromWhere(String alias) {
		return " from " + Tagihan.class.getName() + " " + alias + " " + buildDashboardHqlJoins(alias) + buildDashboardHqlWhere(alias);
	}

	private String buildDashboardHqlJoins(String alias) {
		return " left join " + alias + ".siswa dashSiswa "
				+ " left join " + alias + ".calonSiswa dashCalonSiswa "
				+ " left join " + alias + ".sekolah dashSekolah "
				+ " left join " + alias + ".itemBiayaSekolah dashItemBiaya ";
	}

	private String buildDashboardHqlWhere(String alias) {
		String where = " where (coalesce(" + alias + ".nominal,0) > 0.1 or coalesce(" + alias + ".dibayar,0) > 0.1) ";
		if (sk != null && sk.getId() != null) {
			where += " and " + alias + ".sekolah = :dashSekolah ";
		}
		if (dashboardTahunAjaran != null && dashboardTahunAjaran.trim().length() > 0) {
			where += " and " + alias + ".tahunAjaran = :dashTahunAjaran ";
		}
		if (hasDashboardItemBiayaKeyword()) {
			where += " and (lower(dashItemBiaya.nama) like :dashItemBiayaKeyword or lower(dashItemBiaya.kode) like :dashItemBiayaKeyword) ";
		}
		String c = dashboardKeyword == null ? "" : dashboardKeyword.trim();
		if (c.length() > 0) {
			where += " and (lower(dashSiswa.nama) like :dashKeyword "
					+ " or lower(dashCalonSiswa.nama) like :dashKeyword "
					+ " or lower(dashSekolah.nama) like :dashKeyword "
					+ " or lower(dashItemBiaya.nama) like :dashKeyword ";
			if (isInteger(c)) {
				where += " or " + alias + ".tahunAngkatan = :dashKeywordAngkatan ";
			}
			where += ") ";
		}
		return where;
	}

	private void bindDashboardHqlParameters(org.hibernate.Query query) {
		if (sk != null && sk.getId() != null) {
			query.setParameter("dashSekolah", sk);
		}
		if (dashboardTahunAjaran != null && dashboardTahunAjaran.trim().length() > 0) {
			query.setString("dashTahunAjaran", dashboardTahunAjaran.trim());
		}
		if (hasDashboardItemBiayaKeyword()) {
			query.setString("dashItemBiayaKeyword", "%" + dashboardItemBiayaKeyword.trim().toLowerCase() + "%");
		}
		String c = dashboardKeyword == null ? "" : dashboardKeyword.trim();
		if (c.length() > 0) {
			query.setString("dashKeyword", "%" + c.toLowerCase() + "%");
			if (isInteger(c)) {
				try {
					query.setInteger("dashKeywordAngkatan", Integer.parseInt(c));
				} catch (Exception e) {
					printDebug(e);
				}
			}
		}
	}

	private boolean hasDashboardItemBiayaKeyword() {
		return dashboardItemBiayaKeyword != null && dashboardItemBiayaKeyword.trim().length() > 0;
	}

	private String normalizeDashboardItemBiayaKeyword(String value) {
		if (value == null) {
			return "";
		}
		String v = value.trim();
		if ("Semua".equalsIgnoreCase(v) || "Semua Item Biaya".equalsIgnoreCase(v)) {
			return "";
		}
		return v;
	}

	private String normalizeDashboardTahunAjaran(String value) {
		if (value == null) {
			return null;
		}
		String v = value.trim();
		if (v.length() == 0 || "Semua".equalsIgnoreCase(v) || "Semua Tahun".equalsIgnoreCase(v)
				|| "Semua Tahun Ajaran".equalsIgnoreCase(v) || v.indexOf("=") >= 0) {
			return null;
		}
		return v;
	}


	private boolean isInteger(String s) {
		if (s == null || s.trim().length() == 0) {
			return false;
		}
		try {
			Integer.parseInt(s.trim());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private int toInt(Object value) {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		try {
			return value == null ? 0 : Integer.parseInt(value.toString());
		} catch (Exception e) {
			return 0;
		}
	}

	private double toDouble(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return value == null ? 0.0 : Double.parseDouble(value.toString());
		} catch (Exception e) {
			return 0.0;
		}
	}

	private void addDashboardStep(PembayaranDashboardData d, String step) {
		if (d != null && step != null) {
			d.processingSteps.add(step);
		}
	}

	private void tampilkanLoadingDashboardPembayaran(Component parent, String message, int progress) {
		removeDashboardLoading();
		clearDashboardBusy();
		dashboardProcessStart = System.currentTimeMillis();
		dashboardCurrentProgress = normalizeDashboardProgress(progress);
		dashboardCurrentStage = message == null ? "Memproses data dashboard pembayaran..." : message;
		dashboardLoadingComponent = null;
		dashboardLoadingHtml = null;

		try {
			if (parent == null) {
				return;
			}

			Common.clear(parent);

			MyPortalchildren portalLoading = new MyPortalchildren();
			portalLoading.setWidth("100%");
			portalLoading.setStyle("padding:6px; box-sizing:border-box;");
			portalLoading.setParent(parent);
			dashboardLoadingComponent = portalLoading;

			Panel panel = new ais.ui.util.MyPanelConfig();
			panel.setTitle("Memuat Dashboard Pembayaran Sekolah");
			panel.setBorder("none");
			panel.setCollapsible(false);
			panel.setClosable(false);
			panel.setMaximizable(false);
			panel.setMinimizable(false);
			panel.setStyle("margin-bottom:12px; border:1px solid #dbeafe; border-radius:18px; overflow:hidden;"
					+ "background:#ffffff; box-shadow:0 14px 28px rgba(37,99,235,.10);");
			panel.setParent(portalLoading);

			Panelchildren panelchildren = new Panelchildren();
			panelchildren.setStyle("padding:0; background:#f8fafc;");
			panelchildren.setParent(panel);

			dashboardLoadingHtml = new org.zkoss.zul.Html(buildDashboardLoadingHtml(dashboardCurrentStage,
					dashboardCurrentProgress));
			dashboardLoadingHtml.setParent(panelchildren);

			org.zkoss.zk.ui.util.Clients.showBusy(dashboardCurrentStage);
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private int normalizeDashboardProgress(int progress) {
		if (progress < 0) {
			return 0;
		}
		if (progress > 100) {
			return 100;
		}
		return progress;
	}

	private void updateDashboardProgress(String message, int progress) {
		dashboardCurrentProgress = normalizeDashboardProgress(progress);
		dashboardCurrentStage = message == null ? "Memproses data dashboard pembayaran..." : message;
		updateDashboardBusy(dashboardCurrentStage);
	}

	private void finishDashboardProgress(String message) {
		updateDashboardProgress(message == null ? "Dashboard pembayaran selesai dimuat." : message, 100);
	}

	private String buildDashboardLoadingHtml(String message, int progress) {
		String safeMessage = escapeHtml(message == null ? "Memproses data dashboard pembayaran..." : message);
		int pct = normalizeDashboardProgress(progress);
		long elapsed = dashboardProcessStart <= 0 ? 0 : (System.currentTimeMillis() - dashboardProcessStart);
		String elapsedInfo = elapsed <= 0 ? "" : " &bull; " + elapsed + " ms";

		StringBuilder html = new StringBuilder();
		html.append("<div style=\"padding:18px 20px; background:linear-gradient(135deg, rgba(var(--ais-theme-primary-rgb,29,78,216),.10) 0%, #f8fafc 55%, rgba(var(--ais-theme-primary-rgb,29,78,216),.05) 100%);\">");
		html.append("<div style=\"display:flex; align-items:center; justify-content:space-between; gap:12px; flex-wrap:wrap;\">");
		html.append("<div>");
		html.append("<div style=\"font-size:15px; color:#0f172a; font-weight:900;\"><i class=\"fa fa-spinner fa-spin\"></i> Memproses Dashboard Pembayaran Sekolah</div>");
		html.append("<div style=\"font-size:12px; color:#475569; margin-top:6px; line-height:1.45;\">");
		html.append(safeMessage);
		html.append("</div>");
		html.append("</div>");
		html.append("<div style=\"min-width:92px; text-align:center; padding:9px 12px; border-radius:999px; background:#ffffff; border:1px solid #bfdbfe; color:#1d4ed8; font-size:18px; font-weight:900;\">");
		html.append(pct).append("%");
		html.append("</div>");
		html.append("</div>");
		html.append("<div style=\"margin-top:14px; height:13px; background:#dbeafe; border-radius:999px; overflow:hidden; box-shadow:inset 0 1px 2px rgba(15,23,42,.10);\">");
		html.append("<div style=\"height:13px; width:").append(pct).append("%; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4)); border-radius:999px; transition:width .3s ease;\"></div>");
		html.append("</div>");
		html.append("<div style=\"margin-top:10px; display:flex; justify-content:space-between; gap:10px; flex-wrap:wrap; font-size:11px; color:#64748b;\">");
		html.append("<span>Mohon tunggu, sistem sedang menghitung data tagihan, pembayaran, piutang, grafik, dan rekap rinci.</span>");
		html.append("<span>Progress ").append(pct).append("%").append(elapsedInfo).append("</span>");
		html.append("</div>");
		html.append("</div>");
		return html.toString();
	}

	private void updateDashboardBusy(String message) {
		try {
			String msg = message == null ? "Memproses data dashboard pembayaran..." : message;
			dashboardCurrentStage = msg;
			if (dashboardLoadingHtml != null) {
				dashboardLoadingHtml.setContent(buildDashboardLoadingHtml(msg, dashboardCurrentProgress));
				dashboardLoadingHtml.invalidate();
			}
			org.zkoss.zk.ui.util.Clients.showBusy((dashboardCurrentProgress > 0 ? dashboardCurrentProgress + "% - " : "") + msg);
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void removeDashboardLoading() {
		try {
			if (dashboardLoadingComponent != null && dashboardLoadingComponent.getParent() != null) {
				dashboardLoadingComponent.getParent().removeChild(dashboardLoadingComponent);
			}
		} catch (Exception e) {
			printDebug(e);
		} finally {
			dashboardLoadingComponent = null;
			dashboardLoadingHtml = null;
			clearDashboardBusy();
		}
	}

	private void clearDashboardBusy() {
		try {
			org.zkoss.zk.ui.util.Clients.clearBusy();
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private Object[] getTagihanAggregate() {
		try {
			Criteria criteria = buildBaseTagihanCriteria(false);
			criteria.setProjection(Projections.projectionList().add(Projections.rowCount()).add(Projections.sum("nominal"))
					.add(Projections.sum("dibayar")));
			return (Object[]) criteria.uniqueResult();
		} catch (Exception e) {
			printDebug(e);
		}
		return null;
	}

	private int countTagihan(TagihanCriteriaProvider provider) {
		try {
			Criteria criteria = provider.buildCriteria(false);
			Number n = (Number) criteria.setProjection(Projections.rowCount()).uniqueResult();
			return n == null ? 0 : n.intValue();
		} catch (Exception e) {
			printDebug(e);
		}
		return 0;
	}

	private void analyzePembayaranRows(PembayaranDashboardData d, List<Tagihan> rows) {
		if (rows == null) {
			return;
		}
		Map<String, String> uniquePeserta = new HashMap<String, String>();
		for (Tagihan tagihan : rows) {
			if (tagihan == null) {
				continue;
			}
			double nominal = getNumberValue(tagihan, "getNominal");
			double dibayar = getNumberValue(tagihan, "getDibayar");
			double piutang = Math.max(0.0, nominal - dibayar);
			String pesertaKey = getPesertaName(tagihan);
			if (pesertaKey != null && pesertaKey.trim().length() > 0) {
				uniquePeserta.put(pesertaKey, pesertaKey);
			}
			addSummaryCounter(d.perItemBiaya, getObjectName(getValue(tagihan, "getItemBiayaSekolah")), getValue(tagihan, "getItemBiayaSekolah"), nominal, dibayar, piutang);
			addSummaryCounter(d.perSekolah, getObjectName(getValue(tagihan, "getSekolah")), getValue(tagihan, "getSekolah"), nominal, dibayar, piutang);
			addSummaryCounter(d.perKelas, getObjectName(getValue(tagihan, "getKelasSiswa")), getValue(tagihan, "getKelasSiswa"), nominal, dibayar, piutang);
			addSummaryCounter(d.perAngkatan, safeString(getValue(tagihan, "getTahunAngkatan"), "Tidak diketahui"), getValue(tagihan, "getTahunAngkatan"), nominal, dibayar, piutang);
			if (piutang > 0.1) {
				d.topPiutang.add(tagihan);
			}
		}
		d.totalPeserta = uniquePeserta.size();
		Collections.sort(d.topPiutang, new Comparator<Tagihan>() {
			@Override
			public int compare(Tagihan o1, Tagihan o2) {
				return Double.compare(getPiutang(o2), getPiutang(o1));
			}
		});
		if (d.topPiutang.size() > 12) {
			d.topPiutang = new ArrayList<Tagihan>(d.topPiutang.subList(0, 12));
		}
	}

	private Criteria buildBaseTagihanCriteria(boolean order) {
		Criteria criteria = HibernateUtil.currentSession().createCriteria(Tagihan.class);
		criteria.add(Restrictions.sqlRestriction("(coalesce({alias}.nominal,0) > 0.1 or coalesce({alias}.dibayar,0) > 0.1)"));
		if (dashboardTahunAjaran != null && dashboardTahunAjaran.trim().length() > 0) {
			/*
			 * Detail popup berbasis Criteria masih boleh mengikuti filter tahun ajaran
			 * ketika user memilihnya. Default dashboard sekarang null/semua tahun agar
			 * total awal sama dengan DasboardPiutangRinciSekolah.
			 */
			criteria.add(Restrictions.eq("tahunAjaran", dashboardTahunAjaran.trim()));
		}
		if (sk != null && sk.getId() != null) {
			criteria.add(Restrictions.eq("sekolah", sk));
		}
		if (hasDashboardItemBiayaKeyword()) {
			try {
				criteria.createAlias("itemBiayaSekolah", "dashFilterItemBiaya", Criteria.LEFT_JOIN);
				String itemKey = dashboardItemBiayaKeyword.trim();
				criteria.add(Restrictions.or(Restrictions.ilike("dashFilterItemBiaya.nama", "%" + itemKey + "%"),
						Restrictions.ilike("dashFilterItemBiaya.kode", "%" + itemKey + "%")));
			} catch (Exception e) {
				printDebug(e);
			}
		}
		applyKeywordTagihanFilter(criteria);
		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		return criteria;
	}

	private void applyKeywordTagihanFilter(Criteria criteria) {
		String c = dashboardKeyword == null ? "" : dashboardKeyword.trim();
		if (criteria == null || c.length() == 0) {
			return;
		}
		try {
			criteria.createAlias("siswa", "dashSiswa", Criteria.LEFT_JOIN);
			criteria.createAlias("calonSiswa", "dashCalonSiswa", Criteria.LEFT_JOIN);
			criteria.createAlias("sekolah", "dashSekolah", Criteria.LEFT_JOIN);
			criteria.createAlias("itemBiayaSekolah", "dashItemBiaya", Criteria.LEFT_JOIN);
			Criterion siswa = Restrictions.ilike("dashSiswa.nama", "%" + c + "%");
			Criterion calon = Restrictions.ilike("dashCalonSiswa.nama", "%" + c + "%");
			Criterion sekolah = Restrictions.ilike("dashSekolah.nama", "%" + c + "%");
			Criterion item = Restrictions.ilike("dashItemBiaya.nama", "%" + c + "%");
			Criterion angkatan = Restrictions.ilike("tahunAngkatan", "%" + c + "%");
			criteria.add(Restrictions.or(Restrictions.or(siswa, calon), Restrictions.or(Restrictions.or(sekolah, item), angkatan)));
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void createMetricCardPembayaran(Component parent, String title, String value, String desc, String bg, String color,
			String icon, final String detailTitle, final TagihanCriteriaProvider provider) {
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
		createDetailNumberPembayaran(top, value, detailTitle, provider,
				"font-size:22px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer; text-align:right;");

		appendHtml(card, "<div style='font-size:12px; color:#64748b; margin-top:10px;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:#94a3b8; margin-top:3px;'>" + escapeHtml(desc) + "</div>");
	}

	private void createHeroNumberPembayaran(Component parent, String label, int value, String detailTitle, TagihanCriteriaProvider provider) {
		createHeroNumberPembayaran(parent, label, String.valueOf(value), detailTitle, provider);
	}

	private void createHeroNumberPembayaran(Component parent, String label, String value, String detailTitle, TagihanCriteriaProvider provider) {
		org.zkoss.zul.Div card = new org.zkoss.zul.Div();
		card.setStyle("background:rgba(255,255,255,.16); border:1px solid rgba(255,255,255,.24); padding:12px 16px; border-radius:14px; min-width:130px; text-align:center; box-sizing:border-box;");
		card.setParent(parent);
		createDetailNumberPembayaran(card, value, detailTitle, provider,
				"display:block; font-size:24px; font-weight:800; color:#ffffff; text-decoration:none; cursor:pointer;");
		appendHtml(card, "<div style='font-size:11px; opacity:.85;'>" + escapeHtml(label) + "</div>");
	}

	private A createDetailNumberPembayaran(Component parent, String text, final String title, final TagihanCriteriaProvider provider, String style) {
		A a = new A(text);
		a.setTooltiptext("Klik untuk melihat detail data");
		a.setStyle(style);
		a.setParent(parent);
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showDetailTagihan(title, provider);
			}
		});
		return a;
	}

	private Panelchildren createModernPanelPembayaran(String title, Component parent) {
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
		appendPanelDescriptionEndUserV27(pch, title);
		return pch;
	}

	private void renderFunnelRowPembayaran(Component parent, String label, int value, int max, String color, String detailTitle,
			TagihanCriteriaProvider provider) {
		int pct = max <= 0 ? 0 : (int) Math.round((value * 100.0) / max);
		if (pct < 4 && value > 0) {
			pct = 4;
		}
		org.zkoss.zul.Hbox row = new org.zkoss.zul.Hbox();
		row.setWidth("100%");
		row.setAlign("center");
		row.setStyle("gap:10px; margin:10px 0;");
		row.setParent(parent);
		appendHtml(row, "<div style='width:190px; font-size:12px; color:#334155; font-weight:700;'>" + escapeHtml(label) + "</div>");
		appendHtml(row, "<div style='flex:1; background:#e5e7eb; border-radius:999px; height:12px; overflow:hidden;'>"
				+ "<div style='height:12px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div></div>");
		createDetailNumberPembayaran(row, String.valueOf(value), detailTitle, provider,
				"width:70px; text-align:right; font-size:13px; font-weight:900; color:#0f172a; text-decoration:none; cursor:pointer;");
	}

	private void renderMiniGaugePembayaran(Component parent, String title, int pct, String desc, String color,
			String detailTitle, TagihanCriteriaProvider provider) {
		if (pct < 0) pct = 0;
		if (pct > 100) pct = 100;
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
		createDetailNumberPembayaran(top, pct + "%", detailTitle, provider,
				"font-size:13px; font-weight:900; color:#0f172a; text-decoration:none; cursor:pointer;");
		appendHtml(row, "<div style='margin-top:7px; height:9px; border-radius:999px; background:#e2e8f0; overflow:hidden;'>"
				+ "<div style='height:9px; width:" + pct + "%; border-radius:999px; background:" + color + ";'></div></div>");
	}

	private void renderCounterSummary(Component parent, Map<String, SummaryCounter> source, String emptyMessage, int limit, String color, String mode) {
		if (source == null || source.isEmpty()) {
			appendEmptyState(parent, emptyMessage);
			return;
		}
		List<SummaryCounter> rows = sortSummaryCounter(source);
		double max = rows.isEmpty() ? 1.0 : Math.max(1.0, rows.get(0).piutang);
		int no = 1;
		for (Iterator it = rows.iterator(); it.hasNext();) {
			SummaryCounter rowData = (SummaryCounter) it.next();
			if (rowData == null) continue;
			if (no > limit) break;
			int pct = (int) Math.round((rowData.piutang * 100.0) / max);
			if (pct < 4 && rowData.piutang > 0) pct = 4;
			org.zkoss.zul.Div row = new org.zkoss.zul.Div();
			row.setStyle("padding:9px 0; border-bottom:1px solid #f1f5f9;");
			row.setParent(parent);
			org.zkoss.zul.Hbox top = new org.zkoss.zul.Hbox();
			top.setWidth("100%");
			top.setPack("justify");
			top.setStyle("gap:10px;");
			top.setParent(row);
			appendHtml(top, "<div style='font-size:12px; font-weight:800; color:#334155;'>" + no + ". " + escapeHtml(rowData.label) + "</div>");
			createDetailNumberPembayaran(top, formatMoney(rowData.piutang), "Detail " + rowData.label, createCounterCriteriaProvider(mode, rowData.label, rowData.key),
					"font-size:12px; font-weight:900; color:#0f172a; text-decoration:none; cursor:pointer; text-align:right;");
			appendHtml(row, "<div style='margin-top:7px; height:8px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>"
					+ "<div style='height:8px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div></div>"
					+ "<div style='font-size:10px; color:#64748b; margin-top:5px;'>Tagihan " + escapeHtml(formatMoney(rowData.nominal))
					+ " · Dibayar " + escapeHtml(formatMoney(rowData.dibayar)) + " · " + rowData.count + " data</div>");
			no++;
		}
	}

	private void showDetailTagihan(final String title, final TagihanCriteriaProvider provider) throws Exception {
		final org.zkoss.zul.Window window = new org.zkoss.zul.Window();
		window.setTitle(title);
		window.setClosable(true);
		window.setSizable(true);
		window.setBorder("normal");
		window.setWidth(Common.isMobile() ? "96%" : "92%");
		window.setHeight(Common.isMobile() ? "92%" : "82%");
		window.setStyle("border-radius:14px; overflow:hidden;");
		try {
			if (DasboardPembayaranSekolah.this.getPage() != null) {
				window.setPage(DasboardPembayaranSekolah.this.getPage());
			} else {
				window.setParent(DasboardPembayaranSekolah.this);
			}
		} catch (Exception e) {
			printDebug(e);
			window.setParent(DasboardPembayaranSekolah.this);
		}

		org.zkoss.zul.Vbox root = new org.zkoss.zul.Vbox();
		root.setWidth("100%");
		root.setHeight("100%");
		root.setStyle("background:#f8fafc; padding:12px; box-sizing:border-box;");
		root.setParent(window);

		final org.zkoss.zul.Label info = new org.zkoss.zul.Label();
		info.setStyle("font-size:12px; font-weight:700; color:#334155; padding:8px 10px; background:#ffffff; border:1px solid #e5e7eb; border-radius:10px;");
		info.setParent(root);

		final org.zkoss.zul.Paging paging = new org.zkoss.zul.Paging();
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
				updateDashboardBusy("Memuat detail popup pembayaran...");
				Common.clear(dataBox);
				int total = 0;
				List<Tagihan> data = new ArrayList<Tagihan>();
				try {
					Criteria countCriteria = provider.buildCriteria(false);
					Number number = (Number) countCriteria.setProjection(Projections.rowCount()).uniqueResult();
					total = number == null ? 0 : number.intValue();
					paging.setTotalSize(total);

					Criteria listCriteria = provider.buildCriteria(true);
					data = listCriteria.setFirstResult(DETAIL_PAGE_SIZE * paging.getActivePage()).setMaxResults(DETAIL_PAGE_SIZE).list();
				} catch (Exception e) {
					printDebug(e);
					appendEmptyState(dataBox, "Terjadi error saat mengambil data detail. Aktifkan debug/debuh=true untuk melihat stacktrace.");
					clearDashboardBusy();
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
				addColumn(columns, "Peserta", "18%", "left");
				addColumn(columns, "Sekolah", "14%", "left");
				addColumn(columns, "Kelas/Angkatan", "14%", "left");
				addColumn(columns, "Item Biaya", "18%", "left");
				addColumn(columns, "Nominal", "10%", "right");
				addColumn(columns, "Dibayar", "10%", "right");
				addColumn(columns, "Piutang", "10%", "right");
				addColumn(columns, "Status", "8%", "left");

				Rows rows = new Rows();
				rows.setParent(grid);
				if (data == null || data.isEmpty()) {
					MyFormRow row = new MyFormRow();
					row.setParent(rows);
					org.zkoss.zul.Label lbl = new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig("Tidak ada data detail untuk indikator ini."));
					lbl.setStyle("padding:14px; color:#64748b;");
					row.appendChild(lbl);
				} else {
					for (Iterator it = data.iterator(); it.hasNext();) {
						Tagihan tagihan = (Tagihan) it.next();
						MyFormRow row = new MyFormRow();
						row.setParent(rows);
						addGridCell(row, getPesertaName(tagihan));
						addGridCell(row, getObjectName(getValue(tagihan, "getSekolah")));
						addGridCell(row, getKelasAngkatanText(tagihan));
						addGridCell(row, getObjectName(getValue(tagihan, "getItemBiayaSekolah")));
						addGridCellRight(row, formatMoney(getNumberValue(tagihan, "getNominal")));
						addGridCellRight(row, formatMoney(getNumberValue(tagihan, "getDibayar")));
						addGridCellRight(row, formatMoney(getPiutang(tagihan)));
						addGridCell(row, getStatusTagihan(tagihan));
					}
				}
				clearDashboardBusy();
			}
		};
		paging.addEventListener("onPaging", reload);
		reload.onEvent(null);
		window.doModal();
	}

	private TagihanCriteriaProvider createAllTagihanCriteriaProvider() {
		return new TagihanCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				return buildBaseTagihanCriteria(order);
			}
		};
	}

	private TagihanCriteriaProvider createSudahBayarCriteriaProvider() {
		return new TagihanCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				return buildBaseTagihanCriteria(order).add(Restrictions.sqlRestriction("coalesce({alias}.dibayar,0) > 0.1"));
			}
		};
	}

	private TagihanCriteriaProvider createPiutangCriteriaProvider() {
		return new TagihanCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				return buildBaseTagihanCriteria(order).add(Restrictions.sqlRestriction("(coalesce({alias}.nominal,0)-coalesce({alias}.diskon,0)) > coalesce({alias}.dibayar,0)"));
			}
		};
	}

	private TagihanCriteriaProvider createLunasCriteriaProvider() {
		return new TagihanCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				return buildBaseTagihanCriteria(order).add(Restrictions.sqlRestriction("coalesce({alias}.dibayar,0) >= (coalesce({alias}.nominal,0)-coalesce({alias}.diskon,0))"));
			}
		};
	}

	private TagihanCriteriaProvider createBelumBayarCriteriaProvider() {
		return new TagihanCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				return buildBaseTagihanCriteria(order).add(Restrictions.sqlRestriction("coalesce({alias}.dibayar,0) <= 0.1"));
			}
		};
	}

	private TagihanCriteriaProvider createCicilanCriteriaProvider() {
		return new TagihanCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				return buildBaseTagihanCriteria(order).add(Restrictions.sqlRestriction("coalesce({alias}.dibayar,0) > 0.1"))
						.add(Restrictions.sqlRestriction("coalesce({alias}.dibayar,0) < (coalesce({alias}.nominal,0)-coalesce({alias}.diskon,0))"));
			}
		};
	}

	private TagihanCriteriaProvider createLebihBayarCriteriaProvider() {
		return new TagihanCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				return buildBaseTagihanCriteria(order).add(Restrictions.sqlRestriction("coalesce({alias}.dibayar,0) > (coalesce({alias}.nominal,0)-coalesce({alias}.diskon,0))"));
			}
		};
	}

	private TagihanCriteriaProvider createSingleTagihanCriteriaProvider(final Tagihan tagihan) {
		return new TagihanCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Criteria criteria = buildBaseTagihanCriteria(order);
				Object id = getValue(tagihan, "getId");
				if (id != null) {
					criteria.add(Restrictions.eq("id", id));
				}
				return criteria;
			}
		};
	}

	private TagihanCriteriaProvider createCounterCriteriaProvider(final String mode, final String label, final Object key) {
		return new TagihanCriteriaProvider() {
			@Override
			public Criteria buildCriteria(boolean order) {
				Criteria criteria = buildBaseTagihanCriteria(order);
				try {
					if (key != null) {
						if ("item".equals(mode)) {
							criteria.add(Restrictions.eq("itemBiayaSekolah", key));
						} else if ("sekolah".equals(mode)) {
							criteria.add(Restrictions.eq("sekolah", key));
						} else if ("kelas".equals(mode)) {
							criteria.add(Restrictions.eq("kelasSiswa", key));
						} else if ("angkatan".equals(mode)) {
							criteria.add(Restrictions.eq("tahunAngkatan", key));
						}
						return criteria;
					}
					if (label == null || label.trim().length() == 0 || "Tidak diketahui".equals(label)) {
						return criteria;
					}
					if ("item".equals(mode)) {
						criteria.createAlias("itemBiayaSekolah", "counterItemBiaya", Criteria.LEFT_JOIN);
						criteria.add(Restrictions.ilike("counterItemBiaya.nama", "%" + label + "%"));
					} else if ("sekolah".equals(mode)) {
						criteria.createAlias("sekolah", "counterSekolah", Criteria.LEFT_JOIN);
						criteria.add(Restrictions.ilike("counterSekolah.nama", "%" + label + "%"));
					} else if ("angkatan".equals(mode)) {
						criteria.add(Restrictions.ilike("tahunAngkatan", "%" + label + "%"));
					}
				} catch (Exception e) {
					printDebug(e);
				}
				return criteria;
			}
		};
	}

	private void addSummaryCounter(Map<String, SummaryCounter> map, String label, Object key, double nominal, double dibayar, double piutang) {
		if (label == null || label.trim().length() == 0) {
			label = "Tidak diketahui";
		}
		SummaryCounter c = map.get(label);
		if (c == null) {
			c = new SummaryCounter();
			c.label = label;
			c.key = key;
			map.put(label, c);
		}
		c.count++;
		c.nominal += nominal;
		c.dibayar += dibayar;
		c.piutang += piutang;
	}

	private List<SummaryCounter> sortSummaryCounter(Map<String, SummaryCounter> source) {
		List<SummaryCounter> list = new ArrayList<SummaryCounter>();
		if (source != null) {
			list.addAll(source.values());
		}
		Collections.sort(list, new Comparator<SummaryCounter>() {
			@Override
			public int compare(SummaryCounter o1, SummaryCounter o2) {
				int byPiutang = Double.compare(o2.piutang, o1.piutang);
				if (byPiutang != 0) return byPiutang;
				return o2.count - o1.count;
			}
		});
		return list;
	}

	private void addColumn(Columns columns, String label, String width, String align) {
		Column column = new MyColumnConfig(label);
		if (width != null && width.length() > 0) {
			column.setWidth(width);
		}
		if (align != null && align.length() > 0) {
			column.setAlign(align);
		}
		column.setStyle("font-weight:800; color:#0f172a; background:#f1f5f9;");
		column.setParent(columns);
	}

	private void addGridCell(Row row, String value) {
		org.zkoss.zul.Label label = new org.zkoss.zul.Label(value == null ? "" : value);
		label.setStyle("font-size:11px; color:#334155;");
		row.appendChild(label);
	}

	private void addGridCellRight(Row row, String value) {
		org.zkoss.zul.Label label = new org.zkoss.zul.Label(value == null ? "" : value);
		label.setStyle("display:block; text-align:right; font-size:11px; color:#334155; font-weight:700;");
		row.appendChild(label);
	}

	private void appendHtml(Component parent, String html) {
		org.zkoss.zul.Html h = new org.zkoss.zul.Html(html == null ? "" : html);
		h.setParent(parent);
	}

	private void appendEmptyState(Component parent, String message) {
		appendHtml(parent, "<div style='padding:14px; border-radius:12px; background:#f8fafc; border:1px dashed #cbd5e1; color:#64748b; font-size:12px;'>"
				+ escapeHtml(message) + "</div>");
	}

	private String buildActionCard(String no, String title, String desc, String bg, String color) {
		return "<div style='border-radius:16px; padding:14px; background:" + bg
				+ "; border:1px solid rgba(15,23,42,.08); min-height:105px;'>"
				+ "<div style='display:flex; justify-content:space-between; gap:12px; align-items:center;'>"
				+ "<div style='font-size:12px; font-weight:900; color:" + color + ";'>" + escapeHtml(title) + "</div>"
				+ "<div style='width:28px; height:28px; border-radius:999px; background:#ffffff; color:" + color
				+ "; display:flex; align-items:center; justify-content:center; font-weight:900;'>" + escapeHtml(no) + "</div></div>"
				+ "<div style='font-size:12px; color:" + color + "; line-height:1.45; margin-top:10px;'>"
				+ escapeHtml(desc) + "</div></div>";
	}

	private int getMaxValue(int[] values) {
		int max = 0;
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				if (values[i] > max) max = values[i];
			}
		}
		return max <= 0 ? 1 : max;
	}

	private int percent(int value, int total) {
		if (total <= 0 || value <= 0) return 0;
		return (int) Math.round((value * 100.0d) / total);
	}

	private int percentDouble(double value, double total) {
		if (total <= 0 || value <= 0) return 0;
		return (int) Math.round((value * 100.0d) / total);
	}

	private String formatMoney(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String getPesertaName(Tagihan tagihan) {
		Object siswa = getValue(tagihan, "getSiswa");
		if (siswa != null) return getObjectName(siswa);
		Object calon = getValue(tagihan, "getCalonSiswa");
		if (calon != null) return getObjectName(calon);
		return "Tidak diketahui";
	}

	private String getSekolahKelasText(Tagihan tagihan) {
		String sekolah = getObjectName(getValue(tagihan, "getSekolah"));
		String kelas = getKelasAngkatanText(tagihan);
		return sekolah + " / " + kelas;
	}

	private String getKelasAngkatanText(Tagihan tagihan) {
		String kelas = getObjectName(getValue(tagihan, "getKelasSiswa"));
		String angkatan = safeString(getValue(tagihan, "getTahunAngkatan"), "-");
		if ("Tidak diketahui".equals(kelas)) {
			kelas = "-";
		}
		return kelas + " / " + angkatan;
	}

	private String getStatusTagihan(Tagihan tagihan) {
		double nominal = getNumberValue(tagihan, "getNominal");
		double dibayar = getNumberValue(tagihan, "getDibayar");
		if (nominal <= 0.1) return "Tidak Aktif";
		if (dibayar <= 0.1) return "Belum Bayar";
		if (dibayar > nominal) return "Lebih Bayar";
		if (dibayar >= nominal) return "Lunas";
		return "Cicilan";
	}

	private double getPiutang(Tagihan tagihan) {
		double nominal = getNumberValue(tagihan, "getNominal");
		double diskon = getNumberValue(tagihan, "getDiskon");
		double dibayar = getNumberValue(tagihan, "getDibayar");
		return Math.max(0.0, nominal - diskon - dibayar);
	}

	private double getNumberValue(Object obj, String method) {
		Object value = getValue(obj, method);
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return value == null ? 0.0 : Double.parseDouble(value.toString());
		} catch (Exception e) {
			return 0.0;
		}
	}

	private String getObjectName(Object obj) {
		if (obj == null) return "Tidak diketahui";
		Object nama = getValue(obj, "getNama");
		if (nama != null && nama.toString().trim().length() > 0) return nama.toString();
		return obj.toString();
	}

	private String safeNama(Object obj) {
		return getObjectName(obj);
	}

	private String safeString(Object obj, String defaultValue) {
		if (obj == null) return defaultValue;
		String s = obj.toString();
		return s == null || s.trim().length() == 0 ? defaultValue : s;
	}

	private Object getValue(Object obj, String method) {
		if (obj == null || method == null) return null;
		try {
			return obj.getClass().getMethod(method, new Class[] {}).invoke(obj, new Object[] {});
		} catch (Exception e) {
			return null;
		}
	}

	private String escapeHtml(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}

	private void printDebug(Exception e) {
		if (debug || debuh) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/DasboardPembayaranSekolah.java:3585");
		}
	}

	private static class PembayaranDashboardData {
		int totalTagihan;
		int sudahBayar;
		int lunas;
		int belumBayar;
		int cicilan;
		int lebihBayar;
		int totalPeserta;
		int collectionRate;
		int outstandingRatio;
		double totalNominal;
		double totalDibayar;
		double totalPiutang;
		Map<String, SummaryCounter> perItemBiaya = new TreeMap<String, SummaryCounter>();
		Map<String, SummaryCounter> perSekolah = new TreeMap<String, SummaryCounter>();
		Map<String, SummaryCounter> perKelas = new TreeMap<String, SummaryCounter>();
		Map<String, SummaryCounter> perAngkatan = new TreeMap<String, SummaryCounter>();
		Map<String, SummaryCounter> perCaraBayar = new TreeMap<String, SummaryCounter>();
		Map<String, SummaryCounter> perBulanPembayaran = new TreeMap<String, SummaryCounter>();
		Map<String, SummaryCounter> perItemPembayaran = new TreeMap<String, SummaryCounter>();
		Map<String, SummaryCounter> topPembayar = new TreeMap<String, SummaryCounter>();
		List<Tagihan> topPiutang = new ArrayList<Tagihan>();
		List<String> processingSteps = new ArrayList<String>();
		long dashboardLoadMillis;
		boolean fastQuery;
	}

	private static class SummaryCounter {
		String label;
		Object key;
		int count;
		double nominal;
		double dibayar;
		double piutang;
	}

	interface TagihanCriteriaProvider {
		Criteria buildCriteria(boolean order);
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
		if (t.indexOf("") >= 0 || t.indexOf("alur") >= 0 || t.indexOf("pipeline") >= 0 || t.indexOf("progress") >= 0) {
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
