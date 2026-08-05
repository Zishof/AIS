package ais.action.master.akunting.helper;
import ais.ui.util.DashboardGridExportHelper;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.MoveEvent;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.A;
import org.zkoss.zul.Column;
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

import ais.action.master.akunting.DanaTalanganAction;
import ais.action.master.akunting.KasBesarAction;
import ais.action.master.akunting.KasKecilAction;
import ais.action.master.akunting.PenggantianKasKecilAction;
import ais.action.master.akunting.PertangungjawabanAction;
import ais.action.master.akunting.UangMukaAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.DashboardCacheUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.KasBesar;
import ais.database.model.akunting.KasKecil;
import ais.database.model.akunting.PenggantianKasKecil;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.UangMuka;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Memantau pengajuan kas dan realisasi keuangan agar proses persetujuan dan pembayaran mudah dikendalikan.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardKeuangan extends MyPortallayout {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9006490521125337935L;
	private static final int DETAIL_PAGE_SIZE = 10;
	private static final int DASHBOARD_SAMPLE_LIMIT = 500;
	private static final String STATUS_ALL = "ALL";
	private static final String STATUS_PENDING = "PENDING";
	private static final String STATUS_APPROVED = "APPROVED";
	public static boolean debug = false;

	private Date dashboardFilterMulai;
	private Date dashboardFilterSampai;
	private SatuanKerja dashboardFilterSatker;
	private String dashboardFilterKeyword = "";
	private Panelchildren dashboardKeuanganContainer;

	public DasboardKeuangan() throws Exception {
		super();
		// setHeight("25000px");
		setWidth("100%");
		setMaximizedMode("whole");
		init();
	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Keuangan");
		EventListener reloadPengajuan = new EventListener() {

			private void pengajuanBaru() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardKeuangan.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Pengajuan Kasbon"));
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

					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
						final Textbox cari = new Textbox();
						ambilDataSatuanKerjaBanbox.setCols(7);
						ambilDataSatuanKerjaBanbox.setReadonly(true);
						ambilDataSatuanKerjaBanbox.setParent(toolbar);

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setParent(toolbar);

						MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
						refresh.setTooltiptext("Refresh");
						refresh.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {
								String c = cari.getValue().trim();
								SatuanKerja sk = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(UangMuka.class)
										.add(Restrictions.isNull("disetujuiOleh"))
										.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("satuanKerja", sk))

										.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(Restrictions.ilike("nama", c, MatchMode.ANYWHERE),
														Restrictions.or(
																Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE),

																Restrictions.ilike("kode", c, MatchMode.ANYWHERE))));

								if (order) {
									criteria.addOrder(Order.desc("id"));
								}

								return criteria;
							}
						};

						final Paging paging = new Paging();
						Row rowUtama = new Row();
						rowUtama.setParent(rowUtamapalingAwal.getParent());
						rowUtama.appendChild(paging);

						final Row rowUtamaData = new Row();
						rowUtamaData.setParent(rowUtamapalingAwal.getParent());

						EventListener dataSearchDefault = new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {

								Common.clear(rowUtamaData);

								Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

								List<UangMuka> uangMukas = ((Criteria) dataCriteria.initCriteria(true))
										.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage())))
										.setMaxResults(5).list();

								Grid grid = new Grid();
								grid.setSclass("dgrid");
								grid.setParent(rowUtamaData);
								grid.setSclass("fgrid");
								grid.setStyle("min-height:100px;border:0px;background: transparent;");
								grid.setMold("paging");
								grid.setPageSize(10);
								grid.getPagingChild().setMold("os");

								Columns columns = new Columns();
								columns.setParent(grid);

								Column column = new MyColumnConfig("Kode Kasbon");
								column.setWidth("30%");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Pengajuan Kasbon");
								columns.appendChild(column);

								column = new MyColumnConfig("Unit Pemohon");
								columns.appendChild(column);

								column = new MyColumnConfig("Jumlah");
								column.setAlign("right");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("10%");
								columns.appendChild(column);

								Rows rows = new Rows();
								rows.setParent(grid);

								for (final UangMuka uangMuka : uangMukas) {
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									Vbox a;
									(a = RevisiHelper.createNewRevisi(UangMuka.class, uangMuka,
											uangMuka.getKode() == null ? "" : uangMuka.getKode().trim().toString()))
											.setParent(rowUtamaLagi);
									new MyLabelAgakKecil(uangMuka.getNama()).setParent(a);

									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(uangMuka.getTanggalPembuatan())));
									rowUtamaLagi.appendChild(new MyLabelAgakKecil(uangMuka.getSatuanKerja() == null ? ""
											: uangMuka.getSatuanKerja().getNama()));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(Common.numberFormat.get().format(uangMuka.getNilai())));

									Hbox hbox = new Hbox();
									hbox.setParent(rowUtamaLagi);

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
											"/img/svg/printer.svg");
									button.setTooltiptext("Cetak Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											UangMukaAction.cetak(uangMuka);
										}
									});
									button.setParent(hbox);

									if (uangMuka.getDisposisiSop() != null) {
										button = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
										button.setTooltiptext("Lihat Alur SOP");
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												TampilanAlurSopAction.prosess(uangMuka.getDisposisiSop().getId(), null,
														null, true, event.getTarget());
											}
										});
										button.setParent(hbox);
									}

								}
								uangMukas = null;
							}
						};

						Common.initPaging5(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						cari.addEventListener("onOK", dataSearchDefault);
						ambilDataSatuanKerjaBanbox.setEventListener(dataSearchDefault);
						refresh.addEventListener("onClick", dataSearchDefault);
					}
				};

				jadwalkanLoadPanel(panelchildren, pengajuanBaruEventListener, "Mengambil data Pengajuan Kasbon...");
			}

			private void persetujuanBaru() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardKeuangan.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Persetujuan Kasbon"));
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

				EventListener persetujuanBaruEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
						final Textbox cari = new Textbox();
						ambilDataSatuanKerjaBanbox.setCols(7);
						ambilDataSatuanKerjaBanbox.setReadonly(true);
						ambilDataSatuanKerjaBanbox.setParent(toolbar);

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {
								String c = cari.getValue().trim();
								SatuanKerja sk = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(UangMuka.class)
										.add(Restrictions.isNotNull("disetujuiOleh"))
										.add(Restrictions.isNotNull("tanggalPersetujuan"))

										.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("satuanKerja", sk))

										.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(
														Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE),

														Restrictions.ilike("kode", c, MatchMode.ANYWHERE)));

								if (order) {
									criteria.addOrder(Order.desc("id"));
								}

								return criteria;
							}
						};

						final Paging paging = new Paging();
						Row rowUtama = new Row();
						rowUtama.setParent(rowUtamapalingAwal.getParent());
						rowUtama.appendChild(paging);

						final Row rowUtamaData = new Row();
						rowUtamaData.setParent(rowUtamapalingAwal.getParent());

						EventListener dataSearchDefault = new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {

								Common.clear(rowUtamaData);

								Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

								List<UangMuka> uangMukas = ((Criteria) dataCriteria.initCriteria(true))
										.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage())))
										.setMaxResults(5).list();

								Grid grid = new Grid();
								grid.setSclass("dgrid");
								grid.setParent(rowUtamaData);
								grid.setSclass("fgrid");
								grid.setStyle("min-height:100px;border:0px;background: transparent;");
								grid.setMold("paging");
								grid.setPageSize(10);
								grid.getPagingChild().setMold("os");

								Columns columns = new Columns();
								columns.setParent(grid);

								Column column = new MyColumnConfig("Kode");
								column.setWidth("30%");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Pengajuan Kasbon");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Persetujuan Kasbon");
								columns.appendChild(column);

								column = new MyColumnConfig("Disetujui oleh");
								columns.appendChild(column);

								column = new MyColumnConfig("Jumlah");
								column.setAlign("right");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("10%");
								columns.appendChild(column);

								Rows rows = new Rows();
								rows.setParent(grid);

								for (final UangMuka uangMuka : uangMukas) {
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									Vbox a;
									(a = RevisiHelper.createNewRevisi(UangMuka.class, uangMuka,
											uangMuka.getKode() == null ? "" : uangMuka.getKode().trim().toString()))
											.setParent(rowUtamaLagi);
									new MyLabelAgakKecil(uangMuka.getNama()).setParent(a);

									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(uangMuka.getTanggalPembuatan())));
									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(uangMuka.getTanggalPersetujuan())));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(uangMuka.getDisetujuiOleh().getUserNama()));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(Common.numberFormat.get().format(uangMuka.getNilai())));

									Hbox hbox = new Hbox();
									hbox.setParent(rowUtamaLagi);

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
											"/img/svg/printer.svg");
									button.setTooltiptext("Cetak Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											UangMukaAction.cetak(uangMuka);
										}
									});
									button.setParent(hbox);

									if (uangMuka.getDisposisiSop() != null) {
										button = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
										button.setTooltiptext("Lihat Alur SOP");
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												TampilanAlurSopAction.prosess(uangMuka.getDisposisiSop().getId(), null,
														null, true, event.getTarget());
											}
										});
										button.setParent(hbox);
									}

								}
								uangMukas = null;
							}
						};

						Common.initPaging5(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						cari.addEventListener("onOK", dataSearchDefault);
						ambilDataSatuanKerjaBanbox.setEventListener(dataSearchDefault);

					}
				};

				jadwalkanLoadPanel(panelchildren, persetujuanBaruEventListener, "Mengambil data Persetujuan Kasbon...");
			}

			private void pengajuanBaruDanaTalangan() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardKeuangan.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Pengajuan Talangan / Kas Besar"));
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

					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
						final Textbox cari = new Textbox();
						ambilDataSatuanKerjaBanbox.setCols(7);
						ambilDataSatuanKerjaBanbox.setReadonly(true);
						ambilDataSatuanKerjaBanbox.setParent(toolbar);

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {
								String c = cari.getValue().trim();
								SatuanKerja sk = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(DanaTalangan.class)
										.add(Restrictions.isNull("disetujuiOleh"))
										.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("satuanKerja", sk))

										.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(Restrictions.ilike("nama", c, MatchMode.ANYWHERE),
														Restrictions.or(
																Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE),

																Restrictions.ilike("kode", c, MatchMode.ANYWHERE))));

								if (order) {
									criteria.addOrder(Order.desc("id"));
								}

								return criteria;
							}
						};

						final Paging paging = new Paging();
						Row rowUtama = new Row();
						rowUtama.setParent(rowUtamapalingAwal.getParent());
						rowUtama.appendChild(paging);

						final Row rowUtamaData = new Row();
						rowUtamaData.setParent(rowUtamapalingAwal.getParent());

						EventListener dataSearchDefault = new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {

								Common.clear(rowUtamaData);

								Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

								List<DanaTalangan> danaTalangans = ((Criteria) dataCriteria.initCriteria(true))
										.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage())))
										.setMaxResults(5).list();

								Grid grid = new Grid();
								grid.setSclass("dgrid");
								grid.setParent(rowUtamaData);
								grid.setSclass("fgrid");
								grid.setStyle("min-height:100px;border:0px;background: transparent;");
								grid.setMold("paging");
								grid.setPageSize(10);
								grid.getPagingChild().setMold("os");

								Columns columns = new Columns();
								columns.setParent(grid);

								Column column = new MyColumnConfig("Kode Kasbon");
								column.setWidth("30%");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Pengajuan Kasbon");
								columns.appendChild(column);

								column = new MyColumnConfig("Unit Pemohon");
								columns.appendChild(column);

								column = new MyColumnConfig("Jumlah");
								column.setAlign("right");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("10%");
								columns.appendChild(column);

								Rows rows = new Rows();
								rows.setParent(grid);

								for (final DanaTalangan danaTalangan : danaTalangans) {
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									Vbox a;
									(a = RevisiHelper.createNewRevisi(DanaTalangan.class, danaTalangan,
											danaTalangan.getKode() == null ? ""
													: danaTalangan.getKode().trim().toString()))
											.setParent(rowUtamaLagi);
									new MyLabelAgakKecil(danaTalangan.getNama()).setParent(a);

									RevisiHelper
											.createNewRevisi(UangMuka.class, danaTalangan.getUangMuka(),
													danaTalangan.getUangMuka().getKode() == null ? ""
															: danaTalangan.getUangMuka().getKode().trim().toString())
											.setParent(a);
									new MyLabelAgakKecil(danaTalangan.getUangMuka().getNama()).setParent(a);

									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(danaTalangan.getTanggalPembuatan())));
									rowUtamaLagi
											.appendChild(new MyLabelAgakKecil(danaTalangan.getSatuanKerja() == null ? ""
													: danaTalangan.getSatuanKerja().getNama()));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(Common.numberFormat.get().format(danaTalangan.getNilai())));

									Hbox hbox = new Hbox();
									hbox.setParent(rowUtamaLagi);

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
											"/img/svg/printer.svg");
									button.setTooltiptext("Cetak Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											DanaTalanganAction.cetak(danaTalangan);
										}
									});
									button.setParent(hbox);

									if (danaTalangan.getDisposisiSop() != null) {
										button = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
										button.setTooltiptext("Lihat Alur SOP");
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												TampilanAlurSopAction.prosess(danaTalangan.getDisposisiSop().getId(),
														null, null, true, event.getTarget());
											}
										});
										button.setParent(hbox);
									}

								}
								danaTalangans = null;
							}
						};

						Common.initPaging5(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						cari.addEventListener("onOK", dataSearchDefault);
						ambilDataSatuanKerjaBanbox.setEventListener(dataSearchDefault);
					}
				};

				jadwalkanLoadPanel(panelchildren, pengajuanBaruEventListener, "Mengambil data Pengajuan Talangan / Kas Besar...");
			}

			private void persetujuanBaruDanaTalangan() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardKeuangan.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Persetujuan Talangan / Kas Besar"));
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

				EventListener persetujuanBaruEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
						final Textbox cari = new Textbox();
						ambilDataSatuanKerjaBanbox.setCols(7);
						ambilDataSatuanKerjaBanbox.setReadonly(true);
						ambilDataSatuanKerjaBanbox.setParent(toolbar);

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {
								String c = cari.getValue().trim();
								SatuanKerja sk = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(DanaTalangan.class)
										.add(Restrictions.isNotNull("disetujuiOleh"))
										.add(Restrictions.isNotNull("tanggalPersetujuan"))

										.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("satuanKerja", sk))

										.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(
														Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE),

														Restrictions.ilike("kode", c, MatchMode.ANYWHERE)));

								if (order) {
									criteria.addOrder(Order.desc("id"));
								}

								return criteria;
							}
						};

						final Paging paging = new Paging();
						Row rowUtama = new Row();
						rowUtama.setParent(rowUtamapalingAwal.getParent());
						rowUtama.appendChild(paging);

						final Row rowUtamaData = new Row();
						rowUtamaData.setParent(rowUtamapalingAwal.getParent());

						EventListener dataSearchDefault = new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {

								Common.clear(rowUtamaData);

								Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

								List<DanaTalangan> danaTalangans = ((Criteria) dataCriteria.initCriteria(true))
										.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage())))
										.setMaxResults(5).list();

								Grid grid = new Grid();
								grid.setSclass("dgrid");
								grid.setParent(rowUtamaData);
								grid.setSclass("fgrid");
								grid.setStyle("min-height:100px;border:0px;background: transparent;");
								grid.setMold("paging");
								grid.setPageSize(10);
								grid.getPagingChild().setMold("os");

								Columns columns = new Columns();
								columns.setParent(grid);

								Column column = new MyColumnConfig("Kode");
								column.setWidth("30%");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Pengajuan Kasbon");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Persetujuan Kasbon");
								columns.appendChild(column);

								column = new MyColumnConfig("Disetujui oleh");
								columns.appendChild(column);

								column = new MyColumnConfig("Jumlah");
								column.setAlign("right");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("10%");
								columns.appendChild(column);

								Rows rows = new Rows();
								rows.setParent(grid);

								for (final DanaTalangan danaTalangan : danaTalangans) {
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									Vbox a;
									(a = RevisiHelper.createNewRevisi(DanaTalangan.class, danaTalangan,
											danaTalangan.getKode() == null ? ""
													: danaTalangan.getKode().trim().toString()))
											.setParent(rowUtamaLagi);
									new MyLabelAgakKecil(danaTalangan.getNama()).setParent(a);

									RevisiHelper
											.createNewRevisi(UangMuka.class, danaTalangan.getUangMuka(),
													danaTalangan.getUangMuka().getKode() == null ? ""
															: danaTalangan.getUangMuka().getKode().trim().toString())
											.setParent(a);
									new MyLabelAgakKecil(danaTalangan.getUangMuka().getNama()).setParent(a);

									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(danaTalangan.getTanggalPembuatan())));
									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(danaTalangan.getTanggalPersetujuan())));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(danaTalangan.getDisetujuiOleh().getUserNama()));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(Common.numberFormat.get().format(danaTalangan.getNilai())));

									Hbox hbox = new Hbox();
									hbox.setParent(rowUtamaLagi);

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
											"/img/svg/printer.svg");
									button.setTooltiptext("Cetak Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											DanaTalanganAction.cetak(danaTalangan);
										}
									});
									button.setParent(hbox);

									if (danaTalangan.getDisposisiSop() != null) {
										button = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
										button.setTooltiptext("Lihat Alur SOP");
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												TampilanAlurSopAction.prosess(danaTalangan.getDisposisiSop().getId(),
														null, null, true, event.getTarget());
											}
										});
										button.setParent(hbox);
									}

								}
								danaTalangans = null;
							}
						};

						Common.initPaging5(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						cari.addEventListener("onOK", dataSearchDefault);
						ambilDataSatuanKerjaBanbox.setEventListener(dataSearchDefault);
					}
				};

				jadwalkanLoadPanel(panelchildren, persetujuanBaruEventListener, "Mengambil data Persetujuan Talangan / Kas Besar...");
			}

			private void pengajuanBaruPertangungjawaban() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardKeuangan.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Pengajuan Laporan Pertanggungjawaban"));
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

					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
						final Textbox cari = new Textbox();
						ambilDataSatuanKerjaBanbox.setCols(7);
						ambilDataSatuanKerjaBanbox.setReadonly(true);
						ambilDataSatuanKerjaBanbox.setParent(toolbar);

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {
								String c = cari.getValue().trim();
								SatuanKerja sk = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(Pertangungjawaban.class)
										.add(Restrictions.isNull("disetujuiOleh"))
										.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("satuanKerja", sk))

										.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(Restrictions.ilike("nama", c, MatchMode.ANYWHERE),
														Restrictions.or(
																Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE),

																Restrictions.ilike("kode", c, MatchMode.ANYWHERE))));

								if (order) {
									criteria.addOrder(Order.desc("id"));
								}

								return criteria;
							}
						};

						final Paging paging = new Paging();
						Row rowUtama = new Row();
						rowUtama.setParent(rowUtamapalingAwal.getParent());
						rowUtama.appendChild(paging);

						final Row rowUtamaData = new Row();
						rowUtamaData.setParent(rowUtamapalingAwal.getParent());

						EventListener dataSearchDefault = new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {

								Common.clear(rowUtamaData);

								Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

								List<Pertangungjawaban> pertangungjawabans = ((Criteria) dataCriteria
										.initCriteria(true))
										.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage())))
										.setMaxResults(5).list();

								Grid grid = new Grid();
								grid.setSclass("dgrid");
								grid.setParent(rowUtamaData);
								grid.setSclass("fgrid");
								grid.setStyle("min-height:100px;border:0px;background: transparent;");
								grid.setMold("paging");
								grid.setPageSize(10);
								grid.getPagingChild().setMold("os");

								Columns columns = new Columns();
								columns.setParent(grid);

								Column column = new MyColumnConfig("Kode LPJ");
								column.setWidth("30%");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Pengajuan LPJ");
								columns.appendChild(column);

								column = new MyColumnConfig("Unit Pemohon");
								columns.appendChild(column);

								column = new MyColumnConfig("Jumlah");
								column.setAlign("right");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("10%");
								columns.appendChild(column);

								Rows rows = new Rows();
								rows.setParent(grid);

								for (final Pertangungjawaban pertangungjawaban : pertangungjawabans) {
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									Vbox a;
									(a = RevisiHelper.createNewRevisi(Pertangungjawaban.class, pertangungjawaban,
											pertangungjawaban.getKode() == null ? ""
													: pertangungjawaban.getKode().trim().toString()))
											.setParent(rowUtamaLagi);
									new MyLabelAgakKecil(pertangungjawaban.getNama()).setParent(a);

									RevisiHelper.createNewRevisi(UangMuka.class, pertangungjawaban.getUangMuka(),
											pertangungjawaban.getUangMuka().getKode() == null ? ""
													: pertangungjawaban.getUangMuka().getKode().trim().toString())
											.setParent(a);
									new MyLabelAgakKecil(pertangungjawaban.getUangMuka().getNama()).setParent(a);

									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(pertangungjawaban.getTanggalPembuatan())));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(pertangungjawaban.getSatuanKerja() == null ? ""
													: pertangungjawaban.getSatuanKerja().getNama()));
									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.numberFormat.get().format(pertangungjawaban.getNilai())));

									Hbox hbox = new Hbox();
									hbox.setParent(rowUtamaLagi);

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
											"/img/svg/printer.svg");
									button.setTooltiptext("Cetak Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											PertangungjawabanAction.cetak(pertangungjawaban);
										}
									});
									button.setParent(hbox);

									if (pertangungjawaban.getDisposisiSop() != null) {
										button = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
										button.setTooltiptext("Lihat Alur SOP");
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												TampilanAlurSopAction.prosess(
														pertangungjawaban.getDisposisiSop().getId(), null, null, true,
														event.getTarget());
											}
										});
										button.setParent(hbox);
									}

								}
								pertangungjawabans = null;
							}
						};

						Common.initPaging5(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						cari.addEventListener("onOK", dataSearchDefault);
						ambilDataSatuanKerjaBanbox.setEventListener(dataSearchDefault);
					}
				};

				jadwalkanLoadPanel(panelchildren, pengajuanBaruEventListener, "Mengambil data Pengajuan Laporan Pertanggungjawaban...");
			}

			private void persetujuanBaruPertangungjawaban() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardKeuangan.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Persetujuan Laporan Pertanggungjawaban"));
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

				EventListener persetujuanBaruEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
						final Textbox cari = new Textbox();
						ambilDataSatuanKerjaBanbox.setCols(7);
						ambilDataSatuanKerjaBanbox.setReadonly(true);
						ambilDataSatuanKerjaBanbox.setParent(toolbar);

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {
								String c = cari.getValue().trim();
								SatuanKerja sk = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(Pertangungjawaban.class)
										.add(Restrictions.isNotNull("disetujuiOleh"))
										.add(Restrictions.isNotNull("tanggalPersetujuan"))

										.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("satuanKerja", sk))

										.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(
														Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE),

														Restrictions.ilike("kode", c, MatchMode.ANYWHERE)));

								if (order) {
									criteria.addOrder(Order.desc("id"));
								}

								return criteria;
							}
						};

						final Paging paging = new Paging();
						Row rowUtama = new Row();
						rowUtama.setParent(rowUtamapalingAwal.getParent());
						rowUtama.appendChild(paging);

						final Row rowUtamaData = new Row();
						rowUtamaData.setParent(rowUtamapalingAwal.getParent());

						EventListener dataSearchDefault = new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {

								Common.clear(rowUtamaData);

								Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

								List<Pertangungjawaban> pertangungjawabans = ((Criteria) dataCriteria
										.initCriteria(true))
										.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage())))
										.setMaxResults(5).list();

								Grid grid = new Grid();
								grid.setSclass("dgrid");
								grid.setParent(rowUtamaData);
								grid.setSclass("fgrid");
								grid.setStyle("min-height:100px;border:0px;background: transparent;");
								grid.setMold("paging");
								grid.setPageSize(10);
								grid.getPagingChild().setMold("os");

								Columns columns = new Columns();
								columns.setParent(grid);

								Column column = new MyColumnConfig("Kode");
								column.setWidth("30%");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Pengajuan Lpj");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Persetujuan Lpj");
								columns.appendChild(column);

								column = new MyColumnConfig("Disetujui oleh");
								columns.appendChild(column);

								column = new MyColumnConfig("Jumlah");
								column.setAlign("right");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("10%");
								columns.appendChild(column);

								Rows rows = new Rows();
								rows.setParent(grid);

								for (final Pertangungjawaban pertangungjawaban : pertangungjawabans) {
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									Vbox a;
									(a = RevisiHelper.createNewRevisi(Pertangungjawaban.class, pertangungjawaban,
											pertangungjawaban.getKode() == null ? ""
													: pertangungjawaban.getKode().trim().toString()))
											.setParent(rowUtamaLagi);
									new MyLabelAgakKecil(pertangungjawaban.getNama()).setParent(a);

									RevisiHelper.createNewRevisi(UangMuka.class, pertangungjawaban.getUangMuka(),
											pertangungjawaban.getUangMuka().getKode() == null ? ""
													: pertangungjawaban.getUangMuka().getKode().trim().toString())
											.setParent(a);
									new MyLabelAgakKecil(pertangungjawaban.getUangMuka().getNama()).setParent(a);

									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(pertangungjawaban.getTanggalPembuatan())));
									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(pertangungjawaban.getTanggalPersetujuan())));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(pertangungjawaban.getDisetujuiOleh().getUserNama()));
									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.numberFormat.get().format(pertangungjawaban.getNilai())));

									Hbox hbox = new Hbox();
									hbox.setParent(rowUtamaLagi);

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
											"/img/svg/printer.svg");
									button.setTooltiptext("Cetak Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											PertangungjawabanAction.cetak(pertangungjawaban);
										}
									});
									button.setParent(hbox);

									if (pertangungjawaban.getDisposisiSop() != null) {
										button = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
										button.setTooltiptext("Lihat Alur SOP");
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												TampilanAlurSopAction.prosess(
														pertangungjawaban.getDisposisiSop().getId(), null, null, true,
														event.getTarget());
											}
										});
										button.setParent(hbox);
									}

								}
								pertangungjawabans = null;
							}
						};

						Common.initPaging5(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						cari.addEventListener("onOK", dataSearchDefault);
						ambilDataSatuanKerjaBanbox.setEventListener(dataSearchDefault);
					}
				};

				jadwalkanLoadPanel(panelchildren, persetujuanBaruEventListener, "Mengambil data Persetujuan Laporan Pertanggungjawaban...");
			}

			private void pengajuanBaruKasKecil() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardKeuangan.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Pengeluaran Kas Kecil"));
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

					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
						final Textbox cari = new Textbox();
						ambilDataSatuanKerjaBanbox.setCols(7);
						ambilDataSatuanKerjaBanbox.setReadonly(true);
						ambilDataSatuanKerjaBanbox.setParent(toolbar);

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {
								String c = cari.getValue().trim();
								SatuanKerja sk = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(KasKecil.class)
										.add(Restrictions.isNull("disetujuiOleh"))
										.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("satuanKerja", sk))

										.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(Restrictions.ilike("nama", c, MatchMode.ANYWHERE),
														Restrictions.or(
																Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE),

																Restrictions.ilike("kode", c, MatchMode.ANYWHERE))));

								if (order) {
									criteria.addOrder(Order.desc("id"));
								}

								return criteria;
							}
						};

						final Paging paging = new Paging();
						Row rowUtama = new Row();
						rowUtama.setParent(rowUtamapalingAwal.getParent());
						rowUtama.appendChild(paging);

						final Row rowUtamaData = new Row();
						rowUtamaData.setParent(rowUtamapalingAwal.getParent());

						EventListener dataSearchDefault = new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {

								Common.clear(rowUtamaData);

								Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

								List<KasKecil> kasKecils = ((Criteria) dataCriteria.initCriteria(true))
										.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage())))
										.setMaxResults(5).list();

								Grid grid = new Grid();
								grid.setSclass("dgrid");
								grid.setParent(rowUtamaData);
								grid.setSclass("fgrid");
								grid.setStyle("min-height:100px;border:0px;background: transparent;");
								grid.setMold("paging");
								grid.setPageSize(10);
								grid.getPagingChild().setMold("os");

								Columns columns = new Columns();
								columns.setParent(grid);

								Column column = new MyColumnConfig("Kode Kas Kecil");
								column.setWidth("30%");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Pengajuan Kas Kecil");
								columns.appendChild(column);

								column = new MyColumnConfig("Unit Pemohon");
								columns.appendChild(column);

								column = new MyColumnConfig("Jumlah");
								column.setAlign("right");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("10%");
								columns.appendChild(column);

								Rows rows = new Rows();
								rows.setParent(grid);

								for (final KasKecil kasKecil : kasKecils) {
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									Vbox a;
									(a = RevisiHelper.createNewRevisi(KasKecil.class, kasKecil,
											kasKecil.getKode() == null ? "" : kasKecil.getKode().trim().toString()))
											.setParent(rowUtamaLagi);
									new MyLabelAgakKecil(kasKecil.getNama()).setParent(a);

									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(kasKecil.getTanggalPembuatan())));
									rowUtamaLagi.appendChild(new MyLabelAgakKecil(kasKecil.getSatuanKerja() == null ? ""
											: kasKecil.getSatuanKerja().getNama()));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(Common.numberFormat.get().format(kasKecil.getNilai())));

									Hbox hbox = new Hbox();
									hbox.setParent(rowUtamaLagi);

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
											"/img/svg/printer.svg");
									button.setTooltiptext("Cetak Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											KasKecilAction.cetak(kasKecil);
										}
									});
									button.setParent(hbox);

									if (kasKecil.getDisposisiSop() != null) {
										button = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
										button.setTooltiptext("Lihat Alur SOP");
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												TampilanAlurSopAction.prosess(kasKecil.getDisposisiSop().getId(), null,
														null, true, event.getTarget());
											}
										});
										button.setParent(hbox);
									}

								}
								kasKecils = null;
							}
						};

						Common.initPaging5(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						cari.addEventListener("onOK", dataSearchDefault);
						ambilDataSatuanKerjaBanbox.setEventListener(dataSearchDefault);
					}
				};

				jadwalkanLoadPanel(panelchildren, pengajuanBaruEventListener, "Mengambil data Pengeluaran Kas Kecil...");
			}

			private void persetujuanBaruKasKecil() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardKeuangan.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Persetujuan Kas Kecil"));
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

				EventListener persetujuanBaruEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
						final Textbox cari = new Textbox();
						ambilDataSatuanKerjaBanbox.setCols(7);
						ambilDataSatuanKerjaBanbox.setReadonly(true);
						ambilDataSatuanKerjaBanbox.setParent(toolbar);

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {
								String c = cari.getValue().trim();
								SatuanKerja sk = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(KasKecil.class)
										.add(Restrictions.isNotNull("disetujuiOleh"))
										.add(Restrictions.isNotNull("tanggalPersetujuan"))

										.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("satuanKerja", sk))

										.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(
														Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE),

														Restrictions.ilike("kode", c, MatchMode.ANYWHERE)));

								if (order) {
									criteria.addOrder(Order.desc("id"));
								}

								return criteria;
							}
						};

						final Paging paging = new Paging();
						Row rowUtama = new Row();
						rowUtama.setParent(rowUtamapalingAwal.getParent());
						rowUtama.appendChild(paging);

						final Row rowUtamaData = new Row();
						rowUtamaData.setParent(rowUtamapalingAwal.getParent());

						EventListener dataSearchDefault = new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {

								Common.clear(rowUtamaData);

								Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

								List<KasKecil> kasKecils = ((Criteria) dataCriteria.initCriteria(true))
										.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage())))
										.setMaxResults(5).list();

								Grid grid = new Grid();
								grid.setSclass("dgrid");
								grid.setParent(rowUtamaData);
								grid.setSclass("fgrid");
								grid.setStyle("min-height:100px;border:0px;background: transparent;");
								grid.setMold("paging");
								grid.setPageSize(10);
								grid.getPagingChild().setMold("os");

								Columns columns = new Columns();
								columns.setParent(grid);

								Column column = new MyColumnConfig("Kode");
								column.setWidth("30%");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Pengajuan Kas Kecil");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Persetujuan Kas Kecil");
								columns.appendChild(column);

								column = new MyColumnConfig("Disetujui oleh");
								columns.appendChild(column);

								column = new MyColumnConfig("Jumlah");
								column.setAlign("right");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("10%");
								columns.appendChild(column);

								Rows rows = new Rows();
								rows.setParent(grid);

								for (final KasKecil kasKecil : kasKecils) {
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									Vbox a;
									(a = RevisiHelper.createNewRevisi(KasKecil.class, kasKecil,
											kasKecil.getKode() == null ? "" : kasKecil.getKode().trim().toString()))
											.setParent(rowUtamaLagi);
									new MyLabelAgakKecil(kasKecil.getNama()).setParent(a);

									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(kasKecil.getTanggalPembuatan())));
									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(kasKecil.getTanggalPersetujuan())));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(kasKecil.getDisetujuiOleh().getUserNama()));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(Common.numberFormat.get().format(kasKecil.getNilai())));

									Hbox hbox = new Hbox();
									hbox.setParent(rowUtamaLagi);

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
											"/img/svg/printer.svg");
									button.setTooltiptext("Cetak Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											KasKecilAction.cetak(kasKecil);
										}
									});
									button.setParent(hbox);

									if (kasKecil.getDisposisiSop() != null) {
										button = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
										button.setTooltiptext("Lihat Alur SOP");
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												TampilanAlurSopAction.prosess(kasKecil.getDisposisiSop().getId(), null,
														null, true, event.getTarget());
											}
										});
										button.setParent(hbox);
									}

								}
								kasKecils = null;
							}
						};

						Common.initPaging5(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						cari.addEventListener("onOK", dataSearchDefault);
						ambilDataSatuanKerjaBanbox.setEventListener(dataSearchDefault);
					}
				};

				jadwalkanLoadPanel(panelchildren, persetujuanBaruEventListener, "Mengambil data Persetujuan Kas Kecil...");
			}

			private void persetujuanBaruKasBesar() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardKeuangan.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Persetujuan Kas Besar"));
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

				EventListener persetujuanBaruEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
						final Textbox cari = new Textbox();
						ambilDataSatuanKerjaBanbox.setCols(7);
						ambilDataSatuanKerjaBanbox.setReadonly(true);
						ambilDataSatuanKerjaBanbox.setParent(toolbar);

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {
								String c = cari.getValue().trim();
								SatuanKerja sk = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(KasBesar.class)
										.add(Restrictions.isNotNull("disetujuiOleh"))
										.add(Restrictions.isNotNull("tanggalPersetujuan"))

										.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("satuanKerja", sk))

										.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(
														Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE),

														Restrictions.ilike("kode", c, MatchMode.ANYWHERE)));

								if (order) {
									criteria.addOrder(Order.desc("id"));
								}

								return criteria;
							}
						};

						final Paging paging = new Paging();
						Row rowUtama = new Row();
						rowUtama.setParent(rowUtamapalingAwal.getParent());
						rowUtama.appendChild(paging);

						final Row rowUtamaData = new Row();
						rowUtamaData.setParent(rowUtamapalingAwal.getParent());

						EventListener dataSearchDefault = new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {

								Common.clear(rowUtamaData);

								Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

								List<KasBesar> kasBesars = ((Criteria) dataCriteria.initCriteria(true))
										.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage())))
										.setMaxResults(5).list();

								Grid grid = new Grid();
								grid.setSclass("dgrid");
								grid.setParent(rowUtamaData);
								grid.setSclass("fgrid");
								grid.setStyle("min-height:100px;border:0px;background: transparent;");
								grid.setMold("paging");
								grid.setPageSize(10);
								grid.getPagingChild().setMold("os");

								Columns columns = new Columns();
								columns.setParent(grid);

								Column column = new MyColumnConfig("Kode");
								column.setWidth("30%");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Pengajuan Kas Besar");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Persetujuan Kas Besar");
								columns.appendChild(column);

								column = new MyColumnConfig("Disetujui oleh");
								columns.appendChild(column);

								column = new MyColumnConfig("Jumlah");
								column.setAlign("right");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("10%");
								columns.appendChild(column);

								Rows rows = new Rows();
								rows.setParent(grid);

								for (final KasBesar kasBesar : kasBesars) {
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									Vbox a;
									(a = RevisiHelper.createNewRevisi(KasBesar.class, kasBesar,
											kasBesar.getKode() == null ? "" : kasBesar.getKode().trim().toString()))
											.setParent(rowUtamaLagi);
									new MyLabelAgakKecil(kasBesar.getNama()).setParent(a);

									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(kasBesar.getTanggalPembuatan())));
									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(kasBesar.getTanggalPersetujuan())));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(kasBesar.getDisetujuiOleh().getUserNama()));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(Common.numberFormat.get().format(kasBesar.getNilai())));

									Hbox hbox = new Hbox();
									hbox.setParent(rowUtamaLagi);

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
											"/img/svg/printer.svg");
									button.setTooltiptext("Cetak Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											KasBesarAction.cetak(kasBesar);
										}
									});
									button.setParent(hbox);

									if (kasBesar.getDisposisiSop() != null) {
										button = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
										button.setTooltiptext("Lihat Alur SOP");
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												TampilanAlurSopAction.prosess(kasBesar.getDisposisiSop().getId(), null,
														null, true, event.getTarget());
											}
										});
										button.setParent(hbox);
									}

								}
								kasBesars = null;
							}
						};

						Common.initPaging5(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						cari.addEventListener("onOK", dataSearchDefault);
						ambilDataSatuanKerjaBanbox.setEventListener(dataSearchDefault);
					}
				};

				jadwalkanLoadPanel(panelchildren, persetujuanBaruEventListener, "Mengambil data Persetujuan Kas Besar...");
			}

			private void pengajuanBaruPenggantianKasKecil() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardKeuangan.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Pengeluaran Penggantian Kas Kecil"));
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

					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
						final Textbox cari = new Textbox();
						ambilDataSatuanKerjaBanbox.setCols(7);
						ambilDataSatuanKerjaBanbox.setReadonly(true);
						ambilDataSatuanKerjaBanbox.setParent(toolbar);

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {
								String c = cari.getValue().trim();
								SatuanKerja sk = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(PenggantianKasKecil.class)
										.add(Restrictions.isNotNull("kasKecil"))
										.add(Restrictions.isNull("disetujuiOleh"))
										.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("satuanKerja", sk))

										.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(Restrictions.ilike("nama", c, MatchMode.ANYWHERE),
														Restrictions.or(
																Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE),

																Restrictions.ilike("kode", c, MatchMode.ANYWHERE))));

								if (order) {
									criteria.addOrder(Order.desc("id"));
								}

								return criteria;
							}
						};

						final Paging paging = new Paging();
						Row rowUtama = new Row();
						rowUtama.setParent(rowUtamapalingAwal.getParent());
						rowUtama.appendChild(paging);

						final Row rowUtamaData = new Row();
						rowUtamaData.setParent(rowUtamapalingAwal.getParent());

						EventListener dataSearchDefault = new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {

								Common.clear(rowUtamaData);

								Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

								List<PenggantianKasKecil> penggantianKasKecils = ((Criteria) dataCriteria
										.initCriteria(true))
										.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage())))
										.setMaxResults(5).list();

								Grid grid = new Grid();
								grid.setSclass("dgrid");
								grid.setParent(rowUtamaData);
								grid.setSclass("fgrid");
								grid.setStyle("min-height:100px;border:0px;background: transparent;");
								grid.setMold("paging");
								grid.setPageSize(10);
								grid.getPagingChild().setMold("os");

								Columns columns = new Columns();
								columns.setParent(grid);

								Column column = new MyColumnConfig("Kode Penggantian");
								column.setWidth("30%");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Pengajuan Penggantian");
								columns.appendChild(column);

								column = new MyColumnConfig("Unit Pemohon");
								columns.appendChild(column);

								column = new MyColumnConfig("Jumlah");
								column.setAlign("right");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("10%");
								columns.appendChild(column);

								Rows rows = new Rows();
								rows.setParent(grid);

								for (final PenggantianKasKecil penggantianKasKecil : penggantianKasKecils) {
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									Vbox a;
									(a = RevisiHelper.createNewRevisi(PenggantianKasKecil.class, penggantianKasKecil,
											penggantianKasKecil.getKode() == null ? ""
													: penggantianKasKecil.getKode().trim().toString()))
											.setParent(rowUtamaLagi);
									new MyLabelAgakKecil(penggantianKasKecil.getNama()).setParent(a);

									KasKecil kasKecil = penggantianKasKecil.getKasKecil();
									RevisiHelper.createNewRevisi(KasKecil.class, kasKecil,
											kasKecil.getKode() == null ? "" : kasKecil.getKode().trim().toString())
											.setParent(a);
									new MyLabelAgakKecil(kasKecil.getNama()).setParent(a);

									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(penggantianKasKecil.getTanggalPembuatan())));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(penggantianKasKecil.getSatuanKerja() == null ? ""
													: penggantianKasKecil.getSatuanKerja().getNama()));
									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.numberFormat.get().format(penggantianKasKecil.getNilai())));

									Hbox hbox = new Hbox();
									hbox.setParent(rowUtamaLagi);

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
											"/img/svg/printer.svg");
									button.setTooltiptext("Cetak Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											PenggantianKasKecilAction.cetak(penggantianKasKecil);
										}
									});
									button.setParent(hbox);

									if (penggantianKasKecil.getDisposisiSop() != null) {
										button = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
										button.setTooltiptext("Lihat Alur SOP");
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												TampilanAlurSopAction.prosess(
														penggantianKasKecil.getDisposisiSop().getId(), null, null, true,
														event.getTarget());
											}
										});
										button.setParent(hbox);
									}

								}
								penggantianKasKecils = null;
							}
						};

						Common.initPaging5(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						cari.addEventListener("onOK", dataSearchDefault);
						ambilDataSatuanKerjaBanbox.setEventListener(dataSearchDefault);
					}
				};

				jadwalkanLoadPanel(panelchildren, pengajuanBaruEventListener, "Mengambil data Pengeluaran Penggantian Kas Kecil...");
			}

			private void persetujuanBaruPenggantianKasKecil() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardKeuangan.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Persetujuan Penggantian Kas Kecil"));
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

				EventListener persetujuanBaruEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
						final Textbox cari = new Textbox();
						ambilDataSatuanKerjaBanbox.setCols(7);
						ambilDataSatuanKerjaBanbox.setReadonly(true);
						ambilDataSatuanKerjaBanbox.setParent(toolbar);

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {
								String c = cari.getValue().trim();
								SatuanKerja sk = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(PenggantianKasKecil.class)
										.add(Restrictions.isNotNull("kasKecil"))
										.add(Restrictions.isNotNull("disetujuiOleh"))
										.add(Restrictions.isNotNull("tanggalPersetujuan"))

										.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("satuanKerja", sk))

										.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(
														Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE),

														Restrictions.ilike("kode", c, MatchMode.ANYWHERE)));

								if (order) {
									criteria.addOrder(Order.desc("id"));
								}

								return criteria;
							}
						};

						final Paging paging = new Paging();
						Row rowUtama = new Row();
						rowUtama.setParent(rowUtamapalingAwal.getParent());
						rowUtama.appendChild(paging);

						final Row rowUtamaData = new Row();
						rowUtamaData.setParent(rowUtamapalingAwal.getParent());

						EventListener dataSearchDefault = new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {

								Common.clear(rowUtamaData);

								Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

								List<PenggantianKasKecil> penggantianKasKecils = ((Criteria) dataCriteria
										.initCriteria(true))
										.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage())))
										.setMaxResults(5).list();

								Grid grid = new Grid();
								grid.setSclass("dgrid");
								grid.setParent(rowUtamaData);
								grid.setSclass("fgrid");
								grid.setStyle("min-height:100px;border:0px;background: transparent;");
								grid.setMold("paging");
								grid.setPageSize(10);
								grid.getPagingChild().setMold("os");

								Columns columns = new Columns();
								columns.setParent(grid);

								Column column = new MyColumnConfig("Kode");
								column.setWidth("30%");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Pengajuan Penggantian");
								columns.appendChild(column);

								column = new MyColumnConfig("Wkt Persetujuan Penggantian");
								columns.appendChild(column);

								column = new MyColumnConfig("Disetujui oleh");
								columns.appendChild(column);

								column = new MyColumnConfig("Jumlah");
								column.setAlign("right");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("10%");
								columns.appendChild(column);

								Rows rows = new Rows();
								rows.setParent(grid);

								for (final PenggantianKasKecil penggantianKasKecil : penggantianKasKecils) {
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									Vbox a;
									(a = RevisiHelper.createNewRevisi(PenggantianKasKecil.class, penggantianKasKecil,
											penggantianKasKecil.getKode() == null ? ""
													: penggantianKasKecil.getKode().trim().toString()))
											.setParent(rowUtamaLagi);
									new MyLabelAgakKecil(penggantianKasKecil.getNama()).setParent(a);

									KasKecil kasKecil = penggantianKasKecil.getKasKecil();
									RevisiHelper.createNewRevisi(KasKecil.class, kasKecil,
											kasKecil.getKode() == null ? "" : kasKecil.getKode().trim().toString())
											.setParent(a);
									new MyLabelAgakKecil(kasKecil.getNama()).setParent(a);

									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(penggantianKasKecil.getTanggalPembuatan())));
									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.dateFormat.get().format(penggantianKasKecil.getTanggalPersetujuan())));
									rowUtamaLagi.appendChild(
											new MyLabelAgakKecil(penggantianKasKecil.getDisetujuiOleh().getUserNama()));
									rowUtamaLagi.appendChild(new MyLabelAgakKecil(
											Common.numberFormat.get().format(penggantianKasKecil.getNilai())));

									Hbox hbox = new Hbox();
									hbox.setParent(rowUtamaLagi);

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
											"/img/svg/printer.svg");
									button.setTooltiptext("Cetak Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											PenggantianKasKecilAction.cetak(penggantianKasKecil);
										}
									});
									button.setParent(hbox);

									if (penggantianKasKecil.getDisposisiSop() != null) {
										button = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
										button.setTooltiptext("Lihat Alur SOP");
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												TampilanAlurSopAction.prosess(
														penggantianKasKecil.getDisposisiSop().getId(), null, null, true,
														event.getTarget());
											}
										});
										button.setParent(hbox);
									}

								}
								penggantianKasKecils = null;
							}
						};

						Common.initPaging5(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						cari.addEventListener("onOK", dataSearchDefault);
						ambilDataSatuanKerjaBanbox.setEventListener(dataSearchDefault);
					}
				};

				jadwalkanLoadPanel(panelchildren, persetujuanBaruEventListener, "Mengambil data Persetujuan Penggantian Kas Kecil...");
			}

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					DasboardKeuangan.this.removeEventListener("onLoadDashboardKeuanganAwal", this);
				} catch (Exception e) {
					printDebug(e);
				}
				Common.clear(DasboardKeuangan.this);

				DasboardKeuangan.this.renderDashboardKeuanganUtama();

				pengajuanBaru();
				persetujuanBaru();

				pengajuanBaruDanaTalangan();
				persetujuanBaruDanaTalangan();

				pengajuanBaruPertangungjawaban();
				persetujuanBaruPertangungjawaban();

				pengajuanBaruKasKecil();
				persetujuanBaruKasKecil();

				pengajuanBaruPenggantianKasKecil();
				persetujuanBaruPenggantianKasKecil();

				persetujuanBaruKasBesar();
			}
		};

		tampilkanLoadingDashboardKeuangan(this, "Menyiapkan seluruh komponen dasbor keuangan...", 1);
		this.addEventListener("onLoadDashboardKeuanganAwal", reloadPengajuan);
		Events.echoEvent("onLoadDashboardKeuanganAwal", this, null);
	}


	private void tampilkanLoadingDashboardKeuangan(Component parent, String pesan, int persenAwal) {
		if (parent == null) {
			return;
		}
		Common.clear(parent);

		Component targetParent = parent;
		if (parent instanceof MyPortallayout) {
			MyPortalchildren wrapper = new MyPortalchildren();
			wrapper.setWidth("100%");
			wrapper.setStyle("padding:6px; box-sizing:border-box;");
			wrapper.setParent(parent);

			Panel panel = new ais.ui.util.MyPanelConfig();
			panel.setTitle("Memuat Dasbor Keuangan");
			panel.setBorder("none");
			panel.setCollapsible(false);
			panel.setClosable(false);
			panel.setMaximizable(false);
			panel.setMinimizable(false);
			panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:18px; overflow:hidden;"
					+ "background:#ffffff; box-shadow:0 14px 28px rgba(15,23,42,.08);");
			panel.setParent(wrapper);

			Panelchildren pch = new Panelchildren();
			pch.setStyle("padding:0; background:#f8fafc;");
			pch.setParent(panel);
			targetParent = pch;
		}

		Vbox containerDasborGrid = new Vbox();
		containerDasborGrid.setWidth("100%");
		containerDasborGrid.setStyle("padding:18px; box-sizing:border-box; background:#f8fafc;");
		containerDasborGrid.setParent(targetParent);

		int persen = persenAwal;
		if (persen < 1) {
			persen = 1;
		}
		if (persen > 95) {
			persen = 95;
		}
		String uid = "dk_load_" + System.currentTimeMillis() + "_" + Math.abs(System.identityHashCode(containerDasborGrid));
		Html htmlLoading = new Html("<div style=\"padding:18px; text-align:center; color:#475569; background:#ffffff; "
				+ "border:1px solid #e5e7eb; border-radius:16px; box-shadow:0 12px 26px rgba(15,23,42,.07);\">"
				+ "<div style=\"font-size:14px; font-weight:800; color:#0f172a; margin-bottom:7px;\">"
				+ "<i class=\"fa fa-spinner fa-spin\"></i> " + escapeHtml(pesan) + "</div>"
				+ "<div style=\"font-size:11px; color:#64748b; line-height:1.55; margin-bottom:12px;\">"
				+ "Sistem sedang menyiapkan query, menghitung jumlah data, menjumlahkan nominal, dan menyusun tampilan. "
				+ "Mohon tidak menekan menu lain sampai proses selesai.</div>"
				+ "<div style=\"max-width:620px; margin:0 auto; height:14px; border-radius:999px; background:#e2e8f0; overflow:hidden; border:1px solid #dbe3ef;\">"
				+ "<div id=\"" + uid + "_bar\" style=\"height:14px; width:" + persen + "%; border-radius:999px; "
				+ "background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4)); transition:width .35s ease;\"></div></div>"
				+ "<div style=\"max-width:620px; margin:8px auto 0; display:flex; justify-content:space-between; font-size:11px; color:#64748b;\">"
				+ "<span>Progress pengambilan data</span><b id=\"" + uid + "_pct\" style=\"color:#0f172a;\">" + persen
				+ "%</b></div>"
				+ "<div style=\"max-width:620px; margin:10px auto 0; display:flex; gap:6px; flex-wrap:wrap; justify-content:center;\">"
				+ "<span style=\"padding:5px 8px; border-radius:999px; background:#eff6ff; color:#1d4ed8; font-size:10px; font-weight:800;\">1. Filter</span>"
				+ "<span style=\"padding:5px 8px; border-radius:999px; background:#ecfeff; color:#0e7490; font-size:10px; font-weight:800;\">2. Ringkasan</span>"
				+ "<span style=\"padding:5px 8px; border-radius:999px; background:#f0fdf4; color:#15803d; font-size:10px; font-weight:800;\">3. Nominal</span>"
				+ "<span style=\"padding:5px 8px; border-radius:999px; background:#fff7ed; color:#c2410c; font-size:10px; font-weight:800;\">4. Detail</span>"
				+ "</div></div>"
				+ "<script type=\"text/javascript\">(function(){var p=" + persen + ";var max=95;var bar=document.getElementById('" + uid
				+ "_bar');var pct=document.getElementById('" + uid + "_pct');if(!bar||!pct){return;}var timer=setInterval(function(){"
				+ "if(!document.getElementById('" + uid + "_bar')){clearInterval(timer);return;}"
				+ "p+=(p<45?7:(p<75?4:(p<90?2:1)));if(p>=max){p=max;clearInterval(timer);}"
				+ "bar.style.width=p+'%';pct.innerHTML=p+'%';},450);})();</script>");
		htmlLoading.setParent(containerDasborGrid);
	}

	private void jadwalkanRenderDashboardKeuangan(final Date mulai, final Date sampai, final SatuanKerja satuanKerja,
			final String keyword, String pesan) throws Exception {
		if (dashboardKeuanganContainer == null) {
			return;
		}
		tampilkanLoadingDashboardKeuangan(dashboardKeuanganContainer, pesan, 3);
		final EventListener[] listener = new EventListener[1];
		listener[0] = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					dashboardKeuanganContainer.removeEventListener("onLoadDashboardKeuanganData", listener[0]);
				} catch (Exception e) {
					printDebug(e);
				}
				renderDashboardKeuanganContent(dashboardKeuanganContainer, mulai, sampai, satuanKerja, keyword);
			}
		};
		dashboardKeuanganContainer.addEventListener("onLoadDashboardKeuanganData", listener[0]);
		Events.echoEvent("onLoadDashboardKeuanganData", dashboardKeuanganContainer, null);
	}

	private void jadwalkanLoadPanel(final Panelchildren panelchildren, final EventListener loader, String pesan)
			throws Exception {
		if (panelchildren == null || loader == null) {
			return;
		}
		tampilkanLoadingDashboardKeuangan(panelchildren, pesan, 2);
		final EventListener[] listener = new EventListener[1];
		listener[0] = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					panelchildren.removeEventListener("onLoadPanelKeuangan", listener[0]);
				} catch (Exception e) {
					printDebug(e);
				}
				loader.onEvent(event);
			}
		};
		panelchildren.addEventListener("onLoadPanelKeuangan", listener[0]);
		Events.echoEvent("onLoadPanelKeuangan", panelchildren, null);
	}

	private void jadwalkanLoadDetail(final Component target, final EventListener loader, String pesan) throws Exception {
		if (target == null || loader == null) {
			return;
		}
		tampilkanLoadingDashboardKeuangan(target, pesan, 5);
		final EventListener[] listener = new EventListener[1];
		listener[0] = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					target.removeEventListener("onLoadDetailKeuangan", listener[0]);
				} catch (Exception e) {
					printDebug(e);
				}
				loader.onEvent(event);
			}
		};
		target.addEventListener("onLoadDetailKeuangan", listener[0]);
		Events.echoEvent("onLoadDetailKeuangan", target, null);
	}

	private void renderDashboardKeuanganUtama() throws Exception {
		if (dashboardFilterMulai == null) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, -1);
			dashboardFilterMulai = cal.getTime();
		}
		if (dashboardFilterSampai == null) {
			dashboardFilterSampai = new Date();
		}
		if (dashboardFilterKeyword == null) {
			dashboardFilterKeyword = "";
		}

		MyPortalchildren wrapper = new MyPortalchildren();
		wrapper.setParent(this);
		wrapper.setWidth("100%");
		wrapper.setStyle("padding:6px; box-sizing:border-box;");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setTitle("Dasbor Keuangan");
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:18px; overflow:hidden;"
				+ "background:#ffffff; box-shadow:0 14px 28px rgba(15,23,42,.08);");
		panel.setParent(wrapper);

		dashboardKeuanganContainer = new Panelchildren();
		dashboardKeuanganContainer.setStyle("padding:0; background:#f6f8fb;");
		dashboardKeuanganContainer.setParent(panel);

		jadwalkanRenderDashboardKeuangan(dashboardFilterMulai, dashboardFilterSampai,
				dashboardFilterSatker, dashboardFilterKeyword, "Mengambil ringkasan dasbor keuangan, total transaksi, nominal, satker, dan watchlist...");
	}

	private void renderDashboardKeuanganContent(final Panelchildren parent, Date mulai, Date sampai,
			SatuanKerja satuanKerja, String keyword) throws Exception {
		if (parent == null) {
			return;
		}
		Common.clear(parent);

		if (mulai == null) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, -1);
			mulai = cal.getTime();
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

		Div shell = new Div();
		shell.setWidth("100%");
		shell.setStyle("background:#f6f8fb; padding:14px; box-sizing:border-box; overflow:auto;");
		shell.setParent(parent);

		final FinanceDashboardData data = loadFinanceDashboardDataWithCache();
		renderFinanceHero(shell, data);
		renderFinanceGlobalFilter(shell, dashboardFilterMulai, dashboardFilterSampai, dashboardFilterSatker,
				dashboardFilterKeyword);
		renderFinanceMetricCards(shell, data);
		renderFinanceAnalyticPanels(shell, data);
	}

	private FinanceDashboardData loadFinanceDashboardDataWithCache() {
		String fp = (dashboardFilterMulai  != null ? String.valueOf(dashboardFilterMulai.getTime())  : "0")
				+ "_" + (dashboardFilterSampai != null ? String.valueOf(dashboardFilterSampai.getTime()) : "0")
				+ "_" + (dashboardFilterSatker  != null ? String.valueOf(dashboardFilterSatker.getId())   : "all")
				+ "_" + (dashboardFilterKeyword != null ? dashboardFilterKeyword : "");
		String key = DashboardCacheUtil.keyWithFilter("DasboardKeuangan", "ADMIN", null, fp);
		Object fromL2 = DashboardCacheUtil.getL2(key);
		if (fromL2 instanceof FinanceDashboardData) return (FinanceDashboardData) fromL2;
		Object fromL3 = DashboardCacheUtil.getL3(key);
		if (fromL3 instanceof FinanceDashboardData) {
			DashboardCacheUtil.putL2(key, fromL3);
			return (FinanceDashboardData) fromL3;
		}
		FinanceDashboardData d = loadFinanceDashboardData();
		DashboardCacheUtil.putL2(key, d);
		DashboardCacheUtil.putL3(key, d);
		return d;
	}

	private FinanceDashboardData loadFinanceDashboardData() {
		FinanceDashboardData d = new FinanceDashboardData();
		addMetric(d, "Pengajuan Kasbon", "Kasbon menunggu persetujuan", UangMuka.class, STATUS_PENDING, "#2563eb");
		addMetric(d, "Kasbon Disetujui", "Kasbon sudah disetujui", UangMuka.class, STATUS_APPROVED, "#16a34a");
		addMetric(d, "Pengajuan Talangan / Kas Besar", "Talangan menunggu persetujuan", DanaTalangan.class,
				STATUS_PENDING, "#7c3aed");
		addMetric(d, "Talangan / Kas Besar Disetujui", "Talangan sudah disetujui", DanaTalangan.class,
				STATUS_APPROVED, "#059669");
		addMetric(d, "Pengajuan Pertanggungjawaban", "LPJ menunggu persetujuan", Pertangungjawaban.class,
				STATUS_PENDING, "#ea580c");
		addMetric(d, "Pertanggungjawaban Disetujui", "LPJ sudah disetujui", Pertangungjawaban.class,
				STATUS_APPROVED, "#0891b2");
		addMetric(d, "Pengajuan Kas Kecil", "Kas kecil menunggu persetujuan", KasKecil.class, STATUS_PENDING,
				"#dc2626");
		addMetric(d, "Kas Kecil Disetujui", "Kas kecil sudah disetujui", KasKecil.class, STATUS_APPROVED,
				"#15803d");
		addMetric(d, "Pengajuan Penggantian Kas Kecil", "Reimburse kas kecil menunggu", PenggantianKasKecil.class,
				STATUS_PENDING, "#9333ea");
		addMetric(d, "Penggantian Kas Kecil Disetujui", "Reimburse kas kecil disetujui",
				PenggantianKasKecil.class, STATUS_APPROVED, "#0f766e");
		addMetric(d, "Kas Besar Disetujui", "Kas besar yang sudah disetujui", KasBesar.class, STATUS_APPROVED,
				"#1d4ed8");

		buildFinanceSamples(d);
		return d;
	}

	private void addMetric(FinanceDashboardData d, String title, String desc, Class entityClass, String status,
			String color) {
		FinanceMetric m = new FinanceMetric();
		m.title = title;
		m.description = desc;
		m.entityClass = entityClass;
		m.status = status;
		m.color = color;
		m.count = countFinance(entityClass, status, null);
		m.value = sumFinance(entityClass, status, null);
		d.metrics.add(m);

		if (STATUS_PENDING.equals(status)) {
			d.totalPendingCount += m.count;
			d.totalPendingValue += m.value;
		} else if (STATUS_APPROVED.equals(status)) {
			d.totalApprovedCount += m.count;
			d.totalApprovedValue += m.value;
		}
		d.totalCount += m.count;
		d.totalValue += m.value;
	}

	private int countFinance(Class entityClass, String status, String satkerName) {
		try {
			Criteria criteria = createBaseFinanceCriteria(entityClass, status, false, satkerName);
			Number n = (Number) criteria.setProjection(Projections.rowCount()).uniqueResult();
			return n == null ? 0 : n.intValue();
		} catch (Exception e) {
			printDebug(e);
			return 0;
		}
	}

	private double sumFinance(Class entityClass, String status, String satkerName) {
		try {
			Criteria criteria = createBaseFinanceCriteria(entityClass, status, false, satkerName);
			Number n = (Number) criteria.setProjection(Projections.sum("nilai")).uniqueResult();
			return n == null ? 0 : n.doubleValue();
		} catch (Exception e) {
			printDebug(e);
			return 0;
		}
	}

	private Criteria createBaseFinanceCriteria(Class entityClass, String status, boolean order, String satkerName) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(entityClass);

		if (STATUS_PENDING.equals(status)) {
			criteria.add(Restrictions.isNull("disetujuiOleh"));
		} else if (STATUS_APPROVED.equals(status)) {
			criteria.add(Restrictions.isNotNull("disetujuiOleh"));
			criteria.add(Restrictions.isNotNull("tanggalPersetujuan"));
		}

		if (dashboardFilterSatker != null && dashboardFilterSatker.getId() != null) {
			criteria.add(Restrictions.eq("satuanKerja", dashboardFilterSatker));
		}

		if (satkerName != null && satkerName.trim().length() > 0) {
			try {
				criteria.createAlias("satuanKerja", "detailSatker", Criteria.LEFT_JOIN);
				criteria.add(Restrictions.eq("detailSatker.nama", satkerName));
			} catch (Exception e) {
				printDebug(e);
			}
		}

		if (dashboardFilterMulai != null) {
			criteria.add(Restrictions.ge("tanggalPembuatan", dashboardFilterMulai));
		}
		if (dashboardFilterSampai != null) {
			criteria.add(Restrictions.le("tanggalPembuatan", dashboardFilterSampai));
		}

		String c = dashboardFilterKeyword == null ? "" : dashboardFilterKeyword.trim();
		if (c.length() > 0) {
			criteria.add(Restrictions.or(Restrictions.ilike("nama", c, MatchMode.ANYWHERE),
					Restrictions.or(Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE),
							Restrictions.ilike("kode", c, MatchMode.ANYWHERE))));
		}

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		return criteria;
	}

	private void buildFinanceSamples(FinanceDashboardData d) {
		Class[] allClasses = getFinanceClasses(STATUS_ALL);
		for (int i = 0; i < allClasses.length; i++) {
			List rows = listFinance(allClasses[i], STATUS_ALL, null, 0, DASHBOARD_SAMPLE_LIMIT, true);
			if (rows == null) {
				continue;
			}
			for (Iterator it = rows.iterator(); it.hasNext();) {
				Object item = it.next();
				String satker = getSatkerName(item);
				if (satker == null || satker.trim().length() == 0) {
					satker = "Tanpa Satker";
				}
				FinanceSatkerSummary s = (FinanceSatkerSummary) d.perSatker.get(satker);
				if (s == null) {
					s = new FinanceSatkerSummary();
					s.satker = satker;
					d.perSatker.put(satker, s);
				}
				s.count++;
				s.value += getNilaiDouble(item);
				if (isPending(item)) {
					s.pendingCount++;
					s.pendingValue += getNilaiDouble(item);
					if (d.recentPending.size() < 60) {
						d.recentPending.add(item);
					}
				}
			}
		}

		Collections.sort(d.recentPending, new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				Date d1 = getTanggalPembuatan(o1);
				Date d2 = getTanggalPembuatan(o2);
				long t1 = d1 == null ? 0 : d1.getTime();
				long t2 = d2 == null ? 0 : d2.getTime();
				return t1 == t2 ? 0 : (t1 < t2 ? 1 : -1);
			}
		});
		while (d.recentPending.size() > 10) {
			d.recentPending.remove(d.recentPending.size() - 1);
		}
	}

	private List listFinance(Class entityClass, String status, String satkerName, int first, int max, boolean order) {
		try {
			Criteria criteria = createBaseFinanceCriteria(entityClass, status, order, satkerName);
			if (first > 0) {
				criteria.setFirstResult(first);
			}
			if (max > 0) {
				criteria.setMaxResults(max);
			}
			return criteria.list();
		} catch (Exception e) {
			printDebug(e);
			return new ArrayList();
		}
	}

	private Class[] getFinanceClasses(String status) {
		if (STATUS_PENDING.equals(status)) {
			return new Class[] { UangMuka.class, DanaTalangan.class, Pertangungjawaban.class, KasKecil.class,
					PenggantianKasKecil.class };
		}
		return new Class[] { UangMuka.class, DanaTalangan.class, Pertangungjawaban.class, KasKecil.class,
				PenggantianKasKecil.class, KasBesar.class };
	}

	private void renderFinanceHero(Component parent, FinanceDashboardData d) {
		Div hero = new Div();
		hero.setStyle("position:relative; overflow:hidden; border-radius:18px; padding:22px 24px; color:#ffffff;"
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);"
				+ "box-shadow:0 14px 30px rgba(15,23,42,.18); box-sizing:border-box;");
		hero.setParent(parent);
		appendHtml(hero, "<div style='position:absolute; right:-60px; top:-70px; width:210px; height:210px; border-radius:999px; background:rgba(255,255,255,.12);'></div>"
				+ "<div style='position:absolute; right:90px; bottom:-70px; width:160px; height:160px; border-radius:999px; background:rgba(255,255,255,.10);'></div>");

		Hbox content = new Hbox();
		content.setWidth("100%");
		content.setPack("justify");
		content.setAlign("center");
		content.setStyle("position:relative; z-index:1; gap:16px; flex-wrap:wrap;");
		content.setParent(hero);

		Vbox titleBox = new Vbox();
		titleBox.setStyle("max-width:720px;");
		titleBox.setParent(content);
		appendHtml(titleBox,
				"<div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;'>Finance Control Center</div>"
						+ "<div style='font-size:28px; line-height:1.15; font-weight:800; margin-top:6px;'>Dasbor Keuangan</div>"
						+ "<div style='font-size:13px; opacity:.90; margin-top:8px;'>Ringkasan pengajuan, persetujuan, nilai transaksi, kasbon, dana talangan, kas kecil, kas besar, dan pertanggungjawaban. Klik angka atau nominal untuk melihat data rinci.</div>");
		String satkerText = dashboardFilterSatker == null ? "Semua Satker" : dashboardFilterSatker.getNama();
		String keywordText = dashboardFilterKeyword == null || dashboardFilterKeyword.trim().length() == 0 ? "Tanpa keyword"
				: dashboardFilterKeyword.trim();
		appendHtml(titleBox,
				"<div style='margin-top:12px; display:flex; gap:8px; flex-wrap:wrap;'>"
						+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>Periode: "
						+ escapeHtml(formatTanggalDasbor(dashboardFilterMulai)) + " s.d. "
						+ escapeHtml(formatTanggalDasbor(dashboardFilterSampai)) + "</span>"
						+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>"
						+ escapeHtml(satkerText) + "</span>"
						+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>Cari: "
						+ escapeHtml(keywordText) + "</span></div>");

		Hbox numberBox = new Hbox();
		numberBox.setStyle("gap:10px; flex-wrap:wrap;");
		numberBox.setParent(content);
		createHeroNumber(numberBox, "Total Pengajuan", d.totalPendingCount, formatCurrency(d.totalPendingValue),
				"Detail Semua Pengajuan Keuangan", createCombinedFinanceProvider(STATUS_PENDING, null));
		createHeroNumber(numberBox, "Total Disetujui", d.totalApprovedCount, formatCurrency(d.totalApprovedValue),
				"Detail Semua Persetujuan Keuangan", createCombinedFinanceProvider(STATUS_APPROVED, null));
		createHeroNumber(numberBox, "Total Dipantau", d.totalCount, formatCurrency(d.totalValue),
				"Detail Semua Data Keuangan", createCombinedFinanceProvider(STATUS_ALL, null));
	}

	private void createHeroNumber(Component parent, String label, int count, String amount, final String detailTitle,
			final FinanceDetailProvider provider) {
		Vbox box = new Vbox();
		box.setStyle("min-width:132px; padding:12px; border-radius:16px; background:rgba(255,255,255,.14);"
				+ "border:1px solid rgba(255,255,255,.22); box-sizing:border-box;");
		box.setParent(parent);
		createFinanceDetailLink(box, String.valueOf(count), detailTitle, provider,
				"font-size:28px; font-weight:900; color:#ffffff; text-decoration:none; cursor:pointer;");
		createFinanceDetailLink(box, amount, detailTitle, provider,
				"font-size:12px; font-weight:800; color:#e0f2fe; text-decoration:none; cursor:pointer;");
		appendHtml(box, "<div style='font-size:11px; opacity:.86; margin-top:4px;'>" + escapeHtml(label) + "</div>");
	}

	private void renderFinanceGlobalFilter(final Component parent, Date mulai, Date sampai, SatuanKerja satuanKerja,
			String keyword) throws Exception {
		Div filterContainer = new Div();
		filterContainer.setParent(parent);
		filterContainer.setStyle("margin-top:12px; padding:14px; background:#ffffff; border:1px solid #e8eef6; "
				+ "border-radius:16px; box-shadow:0 10px 26px rgba(15,23,42,0.04); box-sizing:border-box;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(filterContainer);
		toolbar.setStyle(
				"border:0; background:transparent; padding:0; display:flex; flex-wrap:wrap; align-items:center; gap:8px;");

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
		txtKeyword.setTooltiptext("Cari kode, nama, atau keterangan transaksi keuangan");
		txtKeyword.setParent(toolbar);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan Dasbor", "/img/svg/search.svg");
		refresh.setTooltiptext("Refresh dasbor keuangan berdasarkan filter");
		refresh.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; "
				+ "padding:6px 14px; margin-left:4px;");
		refresh.setParent(toolbar);

		EventListener refreshListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				jadwalkanRenderDashboardKeuangan(dbMulai.getValue(), dbSampai.getValue(),
						(SatuanKerja) cbSatker.getAttribute("satuanKerja"), txtKeyword.getValue(),
						"Memperbarui dasbor keuangan sesuai filter: tanggal, satker, dan keyword...");
			}
		};
		refresh.addEventListener("onClick", refreshListener);
		txtKeyword.addEventListener("onOK", refreshListener);
		dbMulai.addEventListener("onChange", refreshListener);
		dbSampai.addEventListener("onChange", refreshListener);
	}

	private void renderFinanceMetricCards(Component parent, FinanceDashboardData d) {
		Div wrap = new Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px;");
		wrap.setParent(parent);
		for (Iterator it = d.metrics.iterator(); it.hasNext();) {
			FinanceMetric m = (FinanceMetric) it.next();
			createFinanceMetricCard(wrap, m);
		}
	}

	private void createFinanceMetricCard(Component parent, final FinanceMetric m) {
		Div card = new Div();
		card.setStyle("flex:1 1 210px; min-width:210px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:14px;"
				+ "box-shadow:0 10px 22px rgba(15,23,42,.06); box-sizing:border-box;");
		card.setParent(parent);

		Hbox top = new Hbox();
		top.setWidth("100%");
		top.setPack("justify");
		top.setAlign("center");
		top.setParent(card);
		appendHtml(top, "<div style='width:38px; height:38px; border-radius:12px; display:flex; align-items:center; justify-content:center; font-weight:900; background:"
				+ m.color + "; color:#ffffff;'>Rp</div>");
		Vbox numbers = new Vbox();
		numbers.setAlign("end");
		numbers.setParent(top);
		FinanceDetailProvider provider = createSingleFinanceProvider(m.entityClass, m.status, null);
		createFinanceDetailLink(numbers, String.valueOf(m.count), "Detail " + m.title, provider,
				"font-size:24px; font-weight:900; color:#0f172a; text-decoration:none; cursor:pointer;");
		createFinanceDetailLink(numbers, formatCurrency(m.value), "Detail " + m.title, provider,
				"font-size:12px; font-weight:800; color:#2563eb; text-decoration:none; cursor:pointer;");

		appendHtml(card, "<div style='font-size:12px; color:#64748b; margin-top:10px; font-weight:800;'>"
				+ escapeHtml(m.title) + "</div>" + "<div style='font-size:11px; color:#94a3b8; margin-top:3px;'>"
				+ escapeHtml(m.description) + "</div>");
	}

	private void renderFinanceAnalyticPanels(Component parent, FinanceDashboardData d) {
		MyPortallayout portalLayout = new MyPortallayout();
		portalLayout.setWidth("100%");
		portalLayout.setMaximizedMode("whole");
		portalLayout.setStyle("margin-top:12px; padding:0; background:transparent;");
		portalLayout.setParent(parent);

		String pcWidth = Common.isMobile() ? "100%" : "50%";
		MyPortalchildren pcTop = new MyPortalchildren();
		pcTop.setWidth("100%");
		pcTop.setStyle("padding:6px; box-sizing:border-box;");
		pcTop.setParent(portalLayout);
		MyPortalchildren pcLeft = new MyPortalchildren();
		pcLeft.setWidth(pcWidth);
		pcLeft.setStyle("padding:6px; box-sizing:border-box;");
		pcLeft.setParent(portalLayout);
		MyPortalchildren pcRight = new MyPortalchildren();
		pcRight.setWidth(pcWidth);
		pcRight.setStyle("padding:6px; box-sizing:border-box;");
		pcRight.setParent(portalLayout);
		MyPortalchildren pcBottom = new MyPortalchildren();
		pcBottom.setWidth("100%");
		pcBottom.setStyle("padding:6px; box-sizing:border-box;");
		pcBottom.setParent(portalLayout);

		renderFinanceFunnel(pcTop, d);
		renderFinanceByTransactionType(pcLeft, d);
		renderFinanceBySatker(pcRight, d);
		renderFinanceRecentPending(pcBottom, d);
		renderFinanceGovernance(pcBottom, d);
	}

	private void renderFinanceFunnel(Component parent, FinanceDashboardData d) {
		Panelchildren pch = createModernPanel(" Status Keuangan", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; margin-bottom:12px;'> membaca tekanan transaksi dari pengajuan yang masih menunggu dibanding transaksi yang sudah disetujui. Angka dan nominal dapat diklik.</div>");
		int max = Math.max(1, Math.max(d.totalPendingCount, d.totalApprovedCount));
		renderFunnelRow(pch, "Menunggu Persetujuan", d.totalPendingCount, d.totalPendingValue, max, "#f59e0b",
				"Detail Semua Pengajuan Keuangan", createCombinedFinanceProvider(STATUS_PENDING, null));
		renderFunnelRow(pch, "Sudah Disetujui", d.totalApprovedCount, d.totalApprovedValue, max, "#16a34a",
				"Detail Semua Persetujuan Keuangan", createCombinedFinanceProvider(STATUS_APPROVED, null));
		renderFunnelRow(pch, "Total Dipantau", d.totalCount, d.totalValue, Math.max(1, d.totalCount), "#2563eb",
				"Detail Semua Data Keuangan", createCombinedFinanceProvider(STATUS_ALL, null));
	}

	private void renderFunnelRow(Component parent, String label, int count, double value, int max, String color,
			String detailTitle, FinanceDetailProvider provider) {
		Div row = new Div();
		row.setStyle("margin-bottom:12px;");
		row.setParent(parent);
		Hbox top = new Hbox();
		top.setWidth("100%");
		top.setPack("justify");
		top.setAlign("center");
		top.setParent(row);
		appendHtml(top, "<div style='font-size:12px; font-weight:900; color:#0f172a;'>" + escapeHtml(label) + "</div>");
		Hbox links = new Hbox();
		links.setStyle("gap:8px;");
		links.setParent(top);
		createFinanceDetailLink(links, String.valueOf(count), detailTitle, provider,
				"font-size:13px; font-weight:900; color:#0f172a; text-decoration:none; cursor:pointer;");
		createFinanceDetailLink(links, formatCurrency(value), detailTitle, provider,
				"font-size:12px; font-weight:800; color:#2563eb; text-decoration:none; cursor:pointer;");
		int pct = max <= 0 ? 0 : (int) Math.round((count * 100.0) / max);
		appendHtml(row, "<div style='margin-top:7px; height:10px; border-radius:999px; background:#e2e8f0; overflow:hidden;'>"
				+ "<div style='height:10px; width:" + pct + "%; border-radius:999px; background:" + color
				+ ";'></div></div>");
	}

	private void renderFinanceByTransactionType(Component parent, FinanceDashboardData d) {
		Panelchildren pch = createModernPanel("Rekap Jenis Transaksi", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>Urutan transaksi berdasarkan nilai terbesar pada filter saat ini.</div>");
		ArrayList sorted = new ArrayList(d.metrics);
		Collections.sort(sorted, new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				FinanceMetric a = (FinanceMetric) o1;
				FinanceMetric b = (FinanceMetric) o2;
				return a.value == b.value ? 0 : (a.value < b.value ? 1 : -1);
			}
		});
		int max = 0;
		for (Iterator it = sorted.iterator(); it.hasNext();) {
			FinanceMetric m = (FinanceMetric) it.next();
			if (m.count > max) {
				max = m.count;
			}
		}
		int no = 1;
		for (Iterator it = sorted.iterator(); it.hasNext() && no <= 10;) {
			FinanceMetric m = (FinanceMetric) it.next();
			renderRankMetricRow(pch, no, m.title, m.count, m.value, Math.max(1, max), m.color, "Detail " + m.title,
					createSingleFinanceProvider(m.entityClass, m.status, null));
			no++;
		}
	}

	private void renderRankMetricRow(Component parent, int no, String label, int count, double value, int max,
			String color, String detailTitle, FinanceDetailProvider provider) {
		Div row = new Div();
		row.setStyle("padding:10px; border:1px solid #e5e7eb; border-radius:12px; margin-bottom:8px; background:#ffffff;");
		row.setParent(parent);
		Hbox top = new Hbox();
		top.setWidth("100%");
		top.setPack("justify");
		top.setAlign("center");
		top.setParent(row);
		appendHtml(top, "<div style='font-size:12px; font-weight:800; color:#334155;'>" + no + ". "
				+ escapeHtml(label) + "</div>");
		Hbox links = new Hbox();
		links.setStyle("gap:8px;");
		links.setParent(top);
		createFinanceDetailLink(links, String.valueOf(count), detailTitle, provider,
				"font-size:12px; font-weight:900; color:#0f172a; text-decoration:none; cursor:pointer;");
		createFinanceDetailLink(links, formatCurrency(value), detailTitle, provider,
				"font-size:12px; font-weight:800; color:#2563eb; text-decoration:none; cursor:pointer;");
		int pct = (int) Math.round((count * 100.0) / Math.max(1, max));
		appendHtml(row, "<div style='margin-top:7px; height:8px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>"
				+ "<div style='height:8px; width:" + pct + "%; background:" + color + ";'></div></div>");
	}

	private void renderFinanceBySatker(Component parent, FinanceDashboardData d) {
		Panelchildren pch = createModernPanel("Rekap Unit / Satker", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>Ringkasan beban transaksi per satuan kerja. Klik angka atau nominal untuk membuka transaksi satker tersebut.</div>");
		ArrayList list = new ArrayList(d.perSatker.values());
		Collections.sort(list, new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				FinanceSatkerSummary a = (FinanceSatkerSummary) o1;
				FinanceSatkerSummary b = (FinanceSatkerSummary) o2;
				return a.value == b.value ? 0 : (a.value < b.value ? 1 : -1);
			}
		});
		if (list.isEmpty()) {
			appendEmptyState(pch, "Belum ada data satker pada filter saat ini.");
			return;
		}
		int no = 1;
		for (Iterator it = list.iterator(); it.hasNext() && no <= 10;) {
			FinanceSatkerSummary s = (FinanceSatkerSummary) it.next();
			renderSatkerRow(pch, no, s);
			no++;
		}
	}

	private void renderSatkerRow(Component parent, int no, FinanceSatkerSummary s) {
		Div row = new Div();
		row.setStyle("padding:10px; border:1px solid #e5e7eb; border-radius:12px; margin-bottom:8px; background:#ffffff;");
		row.setParent(parent);
		Hbox top = new Hbox();
		top.setWidth("100%");
		top.setPack("justify");
		top.setAlign("center");
		top.setParent(row);
		appendHtml(top, "<div style='font-size:12px; font-weight:800; color:#334155;'>" + no + ". "
				+ escapeHtml(s.satker) + "</div>");
		Hbox links = new Hbox();
		links.setStyle("gap:8px;");
		links.setParent(top);
		FinanceDetailProvider provider = createCombinedFinanceProvider(STATUS_ALL, s.satker);
		createFinanceDetailLink(links, String.valueOf(s.count), "Detail Transaksi " + s.satker, provider,
				"font-size:12px; font-weight:900; color:#0f172a; text-decoration:none; cursor:pointer;");
		createFinanceDetailLink(links, formatCurrency(s.value), "Detail Transaksi " + s.satker, provider,
				"font-size:12px; font-weight:800; color:#2563eb; text-decoration:none; cursor:pointer;");
		appendHtml(row, "<div style='font-size:11px; color:#64748b; margin-top:5px;'>Menunggu: " + s.pendingCount
				+ " data / " + escapeHtml(formatCurrency(s.pendingValue)) + "</div>");
	}

	private void renderFinanceRecentPending(Component parent, FinanceDashboardData d) {
		Panelchildren pch = createModernPanel("Watchlist Pengajuan Menunggu Persetujuan", parent);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>Daftar ringkas transaksi terbaru yang masih menunggu persetujuan.</div>");
		if (d.recentPending.isEmpty()) {
			appendEmptyState(pch, "Tidak ada pengajuan yang menunggu persetujuan pada filter saat ini.");
			return;
		}
		Grid grid = new Grid();
		grid.setSclass("dgrid fgrid");
		grid.setWidth("100%");
		grid.setStyle("border:0; background:#ffffff;");
		grid.setParent(pch);
		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(grid);
		String[] headers = new String[] { "Jenis", "Kode / Nama", "Tanggal", "Satker", "Nilai", "Aksi" };
		for (int i = 0; i < headers.length; i++) {
			Column col = new MyColumnConfig(headers[i]);
			col.setStyle("font-weight:800; color:#0f172a; background:#f1f5f9;");
			col.setParent(columns);
		}
		Rows rows = new Rows();
		rows.setParent(grid);
		for (Iterator it = d.recentPending.iterator(); it.hasNext();) {
			final Object item = it.next();
			Row row = new Row();
			row.setParent(rows);
			addGridCell(row, getEntityLabel(item));
			addGridCell(row, getKodeNama(item));
			addGridCell(row, formatTanggal(getTanggalPembuatan(item)));
			addGridCell(row, getSatkerName(item));
			A nilai = createFinanceDetailLink(row, formatCurrency(getNilaiDouble(item)),
					"Detail " + getEntityLabel(item), createSingleFinanceProvider(getEntityClass(item), STATUS_PENDING, null),
					"font-weight:900; color:#2563eb; text-decoration:none; cursor:pointer;");
			nilai.setStyle(nilai.getStyle() + " text-align:right;");
			addActionButtons(row, item);
		}
	}

	private void renderFinanceGovernance(Component parent, FinanceDashboardData d) {
		Panelchildren pch = createModernPanel("Indikator Kontrol Keuangan", parent);
		int total = Math.max(1, d.totalCount);
		int approvalRate = (int) Math.round((d.totalApprovedCount * 100.0) / total);
		int pendingRate = (int) Math.round((d.totalPendingCount * 100.0) / total);
		appendHtml(pch,
				"<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>memberi sinyal cepat untuk prioritas tindak lanjut dan kontrol persetujuan.</div>");
		Div wrap = new Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		wrap.setParent(pch);
		createControlCard(wrap, "Approval Rate", approvalRate + "%", "Porsi transaksi yang sudah disetujui",
				"Detail Semua Persetujuan Keuangan", createCombinedFinanceProvider(STATUS_APPROVED, null), "#ecfdf5",
				"#166534");
		createControlCard(wrap, "Pending Rate", pendingRate + "%", "Porsi transaksi yang masih perlu diproses",
				"Detail Semua Pengajuan Keuangan", createCombinedFinanceProvider(STATUS_PENDING, null), "#fff7ed",
				"#9a3412");
		createControlCard(wrap, "Nilai Pending", formatCurrency(d.totalPendingValue), "Nominal yang masih menunggu persetujuan",
				"Detail Semua Pengajuan Keuangan", createCombinedFinanceProvider(STATUS_PENDING, null), "#eff6ff",
				"#1d4ed8");
		createControlCard(wrap, "Nilai Disetujui", formatCurrency(d.totalApprovedValue), "Nominal transaksi yang sudah disetujui",
				"Detail Semua Persetujuan Keuangan", createCombinedFinanceProvider(STATUS_APPROVED, null), "#f0fdfa",
				"#0f766e");
	}

	private void createControlCard(Component parent, String title, String value, String desc, String detailTitle,
			FinanceDetailProvider provider, String bg, String color) {
		Div card = new Div();
		card.setStyle("flex:1 1 210px; min-width:210px; border-radius:16px; padding:14px; background:" + bg
				+ "; border:1px solid #e5e7eb; box-sizing:border-box;");
		card.setParent(parent);
		appendHtml(card, "<div style='font-size:12px; font-weight:900; color:" + color + ";'>" + escapeHtml(title)
				+ "</div>");
		createFinanceDetailLink(card, value, detailTitle, provider,
				"display:block; margin-top:8px; font-size:24px; font-weight:900; color:" + color
						+ "; text-decoration:none; cursor:pointer;");
		appendHtml(card, "<div style='font-size:11px; color:" + color + "; opacity:.82; margin-top:8px;'>"
				+ escapeHtml(desc) + "</div>");
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

	private A createFinanceDetailLink(Component parent, String text, final String title, final FinanceDetailProvider provider,
			String style) {
		A a = new A(text == null ? "0" : text);
		a.setTooltiptext("Klik untuk melihat detail data");
		a.setStyle(style);
		a.setParent(parent);
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewFinanceDetail(title, provider);
			}
		});
		return a;
	}

	private void viewFinanceDetail(final String title, final FinanceDetailProvider provider) throws Exception {
		final org.zkoss.zul.Window window = new org.zkoss.zul.Window();
		window.setTitle(title);
		window.setClosable(true);
		window.setSizable(true);
		window.setBorder("normal");
		window.setWidth(Common.isMobile() ? "96%" : "92%");
		window.setHeight(Common.isMobile() ? "92%" : "82%");
		window.setStyle("border-radius:14px; overflow:hidden;");
		try {
			if (DasboardKeuangan.this.getPage() != null) {
				window.setPage(DasboardKeuangan.this.getPage());
			} else {
				window.setParent(DasboardKeuangan.this);
			}
		} catch (Exception e) {
			printDebug(e);
			window.setParent(DasboardKeuangan.this);
		}

		Vbox root = new Vbox();
		root.setWidth("100%");
		root.setHeight("100%");
		root.setStyle("background:#f8fafc; padding:12px; box-sizing:border-box;");
		root.setParent(window);

		final Label info = new Label();
		info.setStyle(
				"font-size:12px; font-weight:700; color:#334155; padding:8px 10px; background:#ffffff; border:1px solid #e5e7eb; border-radius:10px;");
		info.setParent(root);

		final Paging paging = new Paging();
		paging.setPageSize(DETAIL_PAGE_SIZE);
		paging.setMold("os");
		paging.setParent(root);

		final Div dataBox = new Div();
		dataBox.setWidth("100%");
		dataBox.setHeight("100%");
		dataBox.setStyle(
				"overflow:auto; background:#ffffff; border:1px solid #e5e7eb; border-radius:12px; padding:8px; box-sizing:border-box;");
		dataBox.setParent(root);

		final EventListener reload = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(dataBox);
				int total = 0;
				List data = new ArrayList();
				try {
					total = provider.count();
					paging.setTotalSize(total);
					data = provider.list(DETAIL_PAGE_SIZE * paging.getActivePage(), DETAIL_PAGE_SIZE);
				} catch (Exception e) {
					printDebug(e);
					appendEmptyState(dataBox,
							"Terjadi error saat mengambil data detail. Aktifkan debug=true untuk melihat stacktrace.");
					return;
				}

				int start = total == 0 ? 0 : (DETAIL_PAGE_SIZE * paging.getActivePage()) + 1;
				int end = DETAIL_PAGE_SIZE * paging.getActivePage() + (data == null ? 0 : data.size());
				info.setValue("Menampilkan " + start + " - " + end + " dari " + total + " data. Page size: "
						+ DETAIL_PAGE_SIZE + ".");
				renderFinanceDetailGrid(dataBox, data);
			}
		};
		paging.addEventListener("onPaging", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				jadwalkanLoadDetail(dataBox, reload, "Mengambil detail data " + title + " halaman " + (paging.getActivePage() + 1) + "...");
			}
		});
		jadwalkanLoadDetail(dataBox, reload, "Mengambil detail data " + title + "...");
		window.doModal();
	}

	private void renderFinanceDetailGrid(Component parent, List data) throws Exception {
		Grid grid = new Grid();
		grid.setSclass("dgrid fgrid");
		grid.setWidth("100%");
		grid.setStyle("border:0; background:#ffffff;");
		grid.setParent(parent);

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(grid);
		String[] headers = new String[] { "Jenis", "Kode / Nama", "Tanggal Pengajuan", "Tanggal Persetujuan", "Satker",
				"Status", "Disetujui Oleh", "Nilai", "Keterangan", "Aksi" };
		for (int i = 0; i < headers.length; i++) {
			Column col = new MyColumnConfig(headers[i]);
			col.setStyle("font-weight:800; color:#0f172a; background:#f1f5f9;");
			col.setParent(columns);
		}

		Rows rows = new Rows();
		rows.setParent(grid);
		if (data == null || data.isEmpty()) {
			Row row = new Row();
			row.setParent(rows);
			addGridCell(row, "Tidak ada data detail untuk indikator ini.");
			return;
		}
		for (Iterator it = data.iterator(); it.hasNext();) {
			Object item = it.next();
			Row row = new Row();
			row.setParent(rows);
			addGridCell(row, getEntityLabel(item));
			addGridCell(row, getKodeNama(item));
			addGridCell(row, formatTanggal(getTanggalPembuatan(item)));
			addGridCell(row, formatTanggal(getTanggalPersetujuan(item)));
			addGridCell(row, getSatkerName(item));
			addGridCell(row, isPending(item) ? "Menunggu" : "Disetujui");
			addGridCell(row, getDisetujuiOlehName(item));
			addGridCellRight(row, formatCurrency(getNilaiDouble(item)));
			addGridCell(row, getKeterangan(item));
			addActionButtons(row, item);
		}
	}

	private FinanceDetailProvider createSingleFinanceProvider(final Class entityClass, final String status,
			final String satkerName) {
		return new FinanceDetailProvider() {
			@Override
			public int count() throws Exception {
				return countFinance(entityClass, status, satkerName);
			}

			@Override
			public List list(int first, int max) throws Exception {
				return listFinance(entityClass, status, satkerName, first, max, true);
			}
		};
	}

	private FinanceDetailProvider createCombinedFinanceProvider(final String status, final String satkerName) {
		return new FinanceDetailProvider() {
			@Override
			public int count() throws Exception {
				int total = 0;
				Class[] classes = getFinanceClasses(status);
				for (int i = 0; i < classes.length; i++) {
					total += countFinance(classes[i], status, satkerName);
				}
				return total;
			}

			@Override
			public List list(int first, int max) throws Exception {
				ArrayList all = new ArrayList();
				Class[] classes = getFinanceClasses(status);
				int fetch = first + max;
				if (fetch <= 0) {
					fetch = DETAIL_PAGE_SIZE;
				}
				for (int i = 0; i < classes.length; i++) {
					all.addAll(listFinance(classes[i], status, satkerName, 0, fetch, true));
				}
				Collections.sort(all, new Comparator() {
					@Override
					public int compare(Object o1, Object o2) {
						Date d1 = getTanggalPembuatan(o1);
						Date d2 = getTanggalPembuatan(o2);
						long t1 = d1 == null ? 0 : d1.getTime();
						long t2 = d2 == null ? 0 : d2.getTime();
						return t1 == t2 ? 0 : (t1 < t2 ? 1 : -1);
					}
				});
				int start = Math.min(first, all.size());
				int end = Math.min(first + max, all.size());
				return new ArrayList(all.subList(start, end));
			}
		};
	}

	private void addActionButtons(Row row, final Object item) {
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("", "/img/svg/printer.svg");
		print.setTooltiptext("Cetak Data");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				cetakFinanceItem(item);
			}
		});
		print.setParent(hbox);

		final Object disposisiSop = callGetter(item, "getDisposisiSop");
		if (disposisiSop != null) {
			MyToolbarbuttonConfig eye = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
			eye.setTooltiptext("Lihat Alur SOP");
			eye.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Object id = callGetter(disposisiSop, "getId");
					if (id instanceof Long) {
						TampilanAlurSopAction.prosess((Long) id, null, null, true, event.getTarget());
					}
				}
			});
			eye.setParent(hbox);
		}
	}

	private void cetakFinanceItem(Object item) throws Exception {
		if (item instanceof UangMuka) {
			UangMukaAction.cetak((UangMuka) item);
		} else if (item instanceof DanaTalangan) {
			DanaTalanganAction.cetak((DanaTalangan) item);
		} else if (item instanceof Pertangungjawaban) {
			PertangungjawabanAction.cetak((Pertangungjawaban) item);
		} else if (item instanceof KasKecil) {
			KasKecilAction.cetak((KasKecil) item);
		} else if (item instanceof KasBesar) {
			KasBesarAction.cetak((KasBesar) item);
		} else if (item instanceof PenggantianKasKecil) {
			PenggantianKasKecilAction.cetak((PenggantianKasKecil) item);
		}
	}

	private void addGridCell(Row row, String text) {
		Label label = new Label(text == null ? "" : text);
		label.setStyle("font-size:12px; color:#334155;");
		label.setParent(row);
	}

	private void addGridCellRight(Row row, String text) {
		Label label = new Label(text == null ? "" : text);
		label.setStyle("font-size:12px; color:#0f172a; font-weight:800; text-align:right;");
		label.setParent(row);
	}

	private Class getEntityClass(Object item) {
		if (item instanceof UangMuka) {
			return UangMuka.class;
		}
		if (item instanceof DanaTalangan) {
			return DanaTalangan.class;
		}
		if (item instanceof Pertangungjawaban) {
			return Pertangungjawaban.class;
		}
		if (item instanceof KasKecil) {
			return KasKecil.class;
		}
		if (item instanceof KasBesar) {
			return KasBesar.class;
		}
		if (item instanceof PenggantianKasKecil) {
			return PenggantianKasKecil.class;
		}
		return item == null ? UangMuka.class : item.getClass();
	}

	private String getEntityLabel(Object item) {
		if (item instanceof UangMuka) {
			return "Kasbon";
		}
		if (item instanceof DanaTalangan) {
			return "Talangan / Kas Besar";
		}
		if (item instanceof Pertangungjawaban) {
			return "Pertanggungjawaban";
		}
		if (item instanceof KasKecil) {
			return "Kas Kecil";
		}
		if (item instanceof KasBesar) {
			return "Kas Besar";
		}
		if (item instanceof PenggantianKasKecil) {
			return "Penggantian Kas Kecil";
		}
		return "Keuangan";
	}

	private boolean isPending(Object item) {
		return callGetter(item, "getDisetujuiOleh") == null;
	}

	private String getKodeNama(Object item) {
		String kode = getStringGetter(item, "getKode");
		String nama = getStringGetter(item, "getNama");
		if (kode.length() > 0 && nama.length() > 0) {
			return kode + " - " + nama;
		}
		return kode.length() > 0 ? kode : nama;
	}

	private String getSatkerName(Object item) {
		Object satker = callGetter(item, "getSatuanKerja");
		String nama = getStringGetter(satker, "getNama");
		return nama == null || nama.length() == 0 ? "-" : nama;
	}

	private String getDisetujuiOlehName(Object item) {
		Object user = callGetter(item, "getDisetujuiOleh");
		String nama = getStringGetter(user, "getUserNama");
		return nama == null || nama.length() == 0 ? "-" : nama;
	}

	private String getKeterangan(Object item) {
		String ket = getStringGetter(item, "getKeterangan");
		return ket == null || ket.length() == 0 ? "-" : ket;
	}

	private Date getTanggalPembuatan(Object item) {
		Object date = callGetter(item, "getTanggalPembuatan");
		return date instanceof Date ? (Date) date : null;
	}

	private Date getTanggalPersetujuan(Object item) {
		Object date = callGetter(item, "getTanggalPersetujuan");
		return date instanceof Date ? (Date) date : null;
	}

	private double getNilaiDouble(Object item) {
		Object n = callGetter(item, "getNilai");
		return n instanceof Number ? ((Number) n).doubleValue() : 0;
	}

	private Object callGetter(Object target, String methodName) {
		if (target == null || methodName == null) {
			return null;
		}
		try {
			return target.getClass().getMethod(methodName, new Class[0]).invoke(target, new Object[0]);
		} catch (Exception e) {
			return null;
		}
	}

	private String getStringGetter(Object target, String methodName) {
		Object value = callGetter(target, methodName);
		return value == null ? "" : String.valueOf(value);
	}

	private String formatCurrency(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String formatTanggal(Date date) {
		try {
			return date == null ? "-" : Common.dateFormat.get().format(date);
		} catch (Exception e) {
			return "-";
		}
	}

	private String formatTanggalDasbor(Date date) {
		return formatTanggal(date);
	}

	private void appendHtml(Component parent, String html) {
		org.zkoss.zul.Html h = new org.zkoss.zul.Html(html);
		h.setParent(parent);
	}

	private void appendEmptyState(Component parent, String message) {
		appendHtml(parent,
				"<div style='padding:18px; border-radius:14px; background:#f8fafc; border:1px dashed #cbd5e1; text-align:center; color:#64748b; font-size:12px;'>"
						+ escapeHtml(message) + "</div>");
	}

	private String escapeHtml(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private void printDebug(Exception e) {
		if ((debug) && e != null) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/DasboardKeuangan.java:3612");
		}
	}

	private static class FinanceDashboardData {
		List metrics = new ArrayList();
		Map perSatker = new TreeMap();
		List recentPending = new ArrayList();
		int totalPendingCount;
		int totalApprovedCount;
		int totalCount;
		double totalPendingValue;
		double totalApprovedValue;
		double totalValue;
	}

	private static class FinanceMetric {
		String title;
		String description;
		Class entityClass;
		String status;
		String color;
		int count;
		double value;
	}

	private static class FinanceSatkerSummary {
		String satker;
		int count;
		int pendingCount;
		double value;
		double pendingValue;
	}

	interface FinanceDetailProvider {
		int count() throws Exception;

		List list(int first, int max) throws Exception;
	}

}
