package ais.action.master.dashboard.employ;
import ais.ui.util.DashboardGridExportHelper;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


import java.util.List;
import java.util.Date;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.MoveEvent;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.dashboard.lkp.DasboardRekapKegiatanPegawai;
import ais.action.master.lkp.helper.RealisasiKerjaPegawaiDetailAction;
import ais.action.report.lkp.LaporanRealisasiLkpTerpaduWindow;
import ais.common.Common;
import ais.common.DashboardCacheUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.lkp.RealisasiKerjaPegawai;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Memantau catatan harian dan realisasi kerja pegawai agar aktivitas kerja dapat dievaluasi dengan jelas.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardKinerja extends MyPortallayout {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9006490521125337935L;
	private Tbmuser tbmuser;

	public DasboardKinerja() throws Exception {
		super();
		// setHeight("25000px");
		setWidth("100%");
		setMaximizedMode("whole");
		tbmuser = Common.getCurrentUser();
		init();
	}

	private void init() throws Exception {
		// Tombol Cetak PDF/Ekspor Excel TIDAK lagi dipasang di sini (paling atas, di luar tab).
		// Sesuai permintaan user, dipindah ke dalam tab "Dasbor" (di actionBar) — lihat
		// renderDashboardKinerja().
		EventListener reloadPengajuan = new EventListener() {

			private String c = "";

			private void pengajuanBaru() throws Exception {

				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardKinerja.this);
//				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				portalchildren.setWidth("100%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle("Catatan Harian");
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
					private boolean pilih = false;

					public EventListener getThis() {
						return this;
					}

					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(panelchildren);
						tabbox.setWidth("100%");
						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						tabs.appendChild(new MyTabConfig("Dasbor"));

						MyTabConfig catatanHarian;
						tabs.appendChild(catatanHarian = new MyTabConfig("Catatan Harian"));

						MyTabConfig laporanLkpTerpadu;
						tabs.appendChild(laporanLkpTerpadu = new MyTabConfig("Laporan Terpadu"));

						MyTabConfig tahun2;
						tabs.appendChild(tahun2 = new MyTabConfig("Statistik"));

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						final Tabpanel tabpanelDasbor = new ais.ui.util.MyTabpanel();
						tabpanelDasbor.setStyle("overflow-y:auto; padding:10px; background:#f8f9fa;");
						tabpanels.appendChild(tabpanelDasbor);

						Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanels.appendChild(tabpanelUtama);

						final Tabpanel tabpanelLkpTerpadu = new ais.ui.util.MyTabpanel();
						tabpanelLkpTerpadu.setStyle("overflow:auto; padding:0; background:#f6f8fb;");
						tabpanels.appendChild(tabpanelLkpTerpadu);
						laporanLkpTerpadu.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (tabpanelLkpTerpadu.getChildren().isEmpty()) {
									LaporanRealisasiLkpTerpaduWindow laporanRealisasiLkpWindow = new LaporanRealisasiLkpTerpaduWindow(
											"", "none", false);
									laporanRealisasiLkpWindow.setWidth("100%");
									laporanRealisasiLkpWindow.setHeight("100%");
									laporanRealisasiLkpWindow.setParent(tabpanelLkpTerpadu);
								}
							}
						});

						final Tabpanel tabpanelTahun2 = new ais.ui.util.MyTabpanel();
						tabpanels.appendChild(tabpanelTahun2);
						tahun2.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (tabpanelTahun2.getChildren().isEmpty()) {
									DasboardRekapKegiatanPegawai dasboardRekapKegiatanPegawai = new DasboardRekapKegiatanPegawai(
											"", "none", false);
									dasboardRekapKegiatanPegawai.setWidth("100%");
									dasboardRekapKegiatanPegawai.setParent(tabpanelTahun2);
								}
							}
						});

						// BLANK saat pertama diakses: renderDashboardKinerja membangun MyPortallayout/
						// Center yang butuh dimensi kontainer. Bila dijalankan SEKARANG (tabbox belum
						// ter-attach & terukur di client) portal kolaps -> tab Dasbor tampak kosong,
						// baru muncul setelah pindah tab & kembali (ZK re-layout). FIX: tunda render ke
						// timer TANPA overlay -> berjalan setelah pohon komponen ter-attach & terukur,
						// sehingga isi Dasbor LANGSUNG muncul di akses pertama.
						Common.createDefaultTimerNoBusy(new EventListener() {
							@Override
							public void onEvent(Event evDasbor) throws Exception {
								renderDashboardKinerja(tabpanelDasbor);
							}
						});

						Row rowUtamapalingAwal = Common.tampilanScroll1(tabpanelUtama);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final Textbox cari = new Textbox(c);

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setParent(toolbar);

						MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Tambah Catatan Harian",
								"/img/svg/form-one.svg");

						toolbarbutton.setParent(toolbar);
						toolbarbutton.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								RealisasiKerjaPegawaiDetailAction.onAddExternal(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										c = cari.getValue().trim();
										pilih = true;
										Common.createDefaultTimer(getThis());

									}
								});

							}
						});

						MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
						refresh.setTooltiptext("Refresh");

						refresh.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {

								String c = cari.getValue().trim();

								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(RealisasiKerjaPegawai.class)
										.createAlias("targetKerjaPegawai", "targetKerjaPegawai")
										.add(tbmuser.getPegawai() == null || (tbmuser.hakAkses() != null
												&& tbmuser.hakAkses().getMelihatDataPegawaiLain())
														? Restrictions.sqlRestriction("true")
														: Restrictions.or(
																Restrictions.eq("targetKerjaPegawai.pegawai",
																		tbmuser.getPegawai()),
																Restrictions.eq("pegawai", tbmuser.getPegawai())));

								if (!c.trim().isEmpty()) {
									criteria.createAlias("targetKerjaPegawai.kegiatanTugasJabatan",
											"kegiatanTugasJabatan")
											.add(Restrictions.or(
													Restrictions.ilike("kegiatanTugasJabatan.nama", c.trim(),
															MatchMode.ANYWHERE),

													Restrictions.or(
															Restrictions.ilike("keterangan", c.trim(),
																	MatchMode.ANYWHERE),
															Restrictions.ilike("catatan", c.trim(),
																	MatchMode.ANYWHERE))));
								}

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

								Common.initPaging((Criteria) dataCriteria.initCriteria(false), paging);

								List<RealisasiKerjaPegawai> realisasiKerjaPegawais = ((Criteria) dataCriteria
										.initCriteria(true))
										.setFirstResult(10 * ((paging == null ? 0 : paging.getActivePage())))
										.setMaxResults(10).list();

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

								MyColumnConfig column = new MyColumnConfig();
								column.setParent(columns);
								column.setWidth("0px");

								column = new MyColumnConfig();
								column.setParent(columns);
								column.setLabel("Foto");
								column.setWidth("70px");

								column = new MyColumnConfig();
								column.setParent(columns);
								column.setLabel("Pegawai");
								column.setWidth("20%");

								column = new MyColumnConfig();
								column.setParent(columns);
								column.setLabel("Kegiatan");

								column = new MyColumnConfig();
								column.setParent(columns);
								column.setLabel("Kuantitas");
								column.setWidth("0px");

								column = new MyColumnConfig();
								column.setParent(columns);
								column.setLabel("Waktu");
								column.setWidth("0px");

								column = new MyColumnConfig();
								column.setParent(columns);
								column.setLabel("Biaya");
								column.setWidth("0px");
								column.setAlign("right");

								column = new MyColumnConfig();
								column.setParent(columns);
								column.setLabel("Verifikasi Asesor");
								column.setWidth("0px");

								column = new MyColumnConfig();
								column.setParent(columns);
								column.setLabel("Catatan Asesor");
								column.setWidth("0px");

								column = new MyColumnConfig();
								column.setParent(columns);
								column.setLabel("");
								column.setWidth("10%");

								Rows rows = new Rows();
								rows.setParent(grid);

								for (final RealisasiKerjaPegawai realisasiKerjaPegawai : realisasiKerjaPegawais) {
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									EventListener ubahEventListener = null;
									EventListener ubahListener = new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											RealisasiKerjaPegawaiDetailAction.onEditExternal(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													pilih = true;
													Common.createDefaultTimer(getThis());
												}
											}, realisasiKerjaPegawai);
										}
									};

									boolean edit = true;
									boolean delete = true;
									boolean merupakanAsesor = false;

									RealisasiKerjaPegawaiDetailAction.displayRow(rowUtamaLagi, realisasiKerjaPegawai,
											edit, delete, merupakanAsesor, ubahEventListener, ubahListener,
											new EventListener() {
												@Override
												public void onEvent(Event event) throws Exception {
													MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
															"Pertanyaan",
															MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
															MyMessageboxConfig.QUESTION, new EventListener() {

																@Override
																public void onEvent(Event event) throws Exception {
																	int i = Integer
																			.parseInt(event.getData().toString());
																	if (i == MyMessageboxConfig.OK) {
																		try {

																			Common.refreshDelete(realisasiKerjaPegawai);
																			pilih = true;
																			Common.createDefaultTimer(getThis());

																		} catch (Exception e) {
																			Common.tampilErrorJikaAdmin(e);
																			MyMessageboxConfig.show(
																					"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																							+ e.getMessage());
																		}

																	}

																}
															});

												}

											});
								}
								realisasiKerjaPegawais = null;
							}
						};

						Common.initPaging(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						refresh.addEventListener("onClick", dataSearchDefault);

						cari.addEventListener("onOK", dataSearchDefault);

						if (pilih) {
							catatanHarian.setSelected(true);
							pilih = false;
						}
					}
				};

				pengajuanBaruEventListener.onEvent(null);
			}

			@Override
			public void onEvent(Event arg0) throws Exception {

				pengajuanBaru();

			}
		};

		reloadPengajuan.onEvent(null);
	}

	private void renderDashboardKinerja(final Tabpanel tabpanelDasbor) throws Exception {
		if (tabpanelDasbor == null) {
			return;
		}

		Common.clear(tabpanelDasbor);

		// Center->scroll (bukan Div "overflow-y:auto" manual, sering tidak memunculkan
		// scrollbar di ZK ini) — lihat Common.tampilanScrollTabbox.
		// PENTING: Center (Borderlayout) hanya boleh punya SATU child langsung, jadi
		// header/actionBar/portalLayout ditaruh di dalam satu Div pembungkus, BUKAN
		// langsung ke Center-nya (dulu 3 komponen di-setParent(wrapper) langsung,
		// menyebabkan "Only one child is allowed: <Center null>" saat child kedua
		// ditambahkan).
		org.zkoss.zul.Center wrapper = Common.tampilanScrollTabbox(tabpanelDasbor);
		org.zkoss.zul.Div wrapperContent = new org.zkoss.zul.Div();
		wrapperContent.setParent(wrapper);
		wrapperContent.setStyle("width:100%; padding:0px 0px 18px 0px;");

		org.zkoss.zul.Html header = new org.zkoss.zul.Html(
				"<div style=\"padding:18px; margin-bottom:12px; border-radius:14px; background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); color:#ffffff; box-shadow:0 4px 12px rgba(0,0,0,0.12);\">"
						+ "<div style=\"font-size:22px;font-weight:700;\">Dasbor Kinerja Pegawai</div>"
						+ "<div style=\"font-size:12px;opacity:.92;margin-top:5px;\">Ringkasan catatan harian, capaian kerja, aktivitas pegawai, dan pintasan laporan LKP dalam satu tampilan.</div>"
						+ "</div>");
		header.setParent(wrapperContent);

		Toolbar actionBar = new Toolbar();
		actionBar.setStyle(
				"padding:8px 10px; margin-bottom:12px; border-radius:10px; background:#ffffff; border:1px solid #e9ecef;");
		actionBar.setParent(wrapperContent);

		MyToolbarbuttonConfig btnTambahCatatan = new MyToolbarbuttonConfig("Tambah Catatan Harian",
				"/img/svg/form-one.svg");
		btnTambahCatatan.setStyle("font-weight:bold; margin-right:8px; color:#0d6efd;");
		btnTambahCatatan.setParent(actionBar);
		btnTambahCatatan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				RealisasiKerjaPegawaiDetailAction.onAddExternal(new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						renderDashboardKinerja(tabpanelDasbor);
					}
				});
			}
		});

		MyToolbarbuttonConfig btnRefreshDasbor = new MyToolbarbuttonConfig("Refresh Dasbor", "/img/svg/refresh.svg");
		btnRefreshDasbor.setStyle("font-weight:bold; color:#198754;");
		btnRefreshDasbor.setParent(actionBar);
		btnRefreshDasbor.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				renderDashboardKinerja(tabpanelDasbor);
			}
		});

		// Tombol Cetak PDF + Ekspor Excel DI DALAM tab Dasbor (permintaan user), tergabung di
		// actionBar bersama Tambah Catatan Harian / Refresh Dasbor — tidak lagi mengambang di atas.
		// Mengekspor seluruh tabel yang ada di dalam konten Dasbor (wrapperContent).
		try {
			ais.ui.util.DashboardGridExportHelper.pasangTombol(actionBar, wrapperContent, "Kinerja");
		} catch (Exception igEkspor) {
			ais.common.ErrorAuditUtil.record(igEkspor, "auto-audit(empty-catch) DasboardKinerja.pasangTombolEkspor");
		}

		DashboardKinerjaData data = loadDashboardKinerjaDataWithCache();

		MyPortallayout portalLayout = new MyPortallayout();
		portalLayout.setWidth("100%");
		portalLayout.setParent(wrapperContent);

		MyPortalchildren pcTop = new MyPortalchildren();
		pcTop.setWidth("100%");
		pcTop.setParent(portalLayout);

		MyPortalchildren pcRadar = new MyPortalchildren();
		pcRadar.setWidth("100%");
		pcRadar.setStyle("padding:5px;");
		pcRadar.setParent(portalLayout);

		String pcWidth = Common.isMobile() ? "100%" : "50%";
		MyPortalchildren pcLeft = new MyPortalchildren();
		pcLeft.setWidth(pcWidth);
		pcLeft.setStyle("padding:5px;");
		pcLeft.setParent(portalLayout);

		MyPortalchildren pcRight = new MyPortalchildren();
		pcRight.setWidth(pcWidth);
		pcRight.setStyle("padding:5px;");
		pcRight.setParent(portalLayout);

		MyPortalchildren pcBottom = new MyPortalchildren();
		pcBottom.setWidth("100%");
		pcBottom.setStyle("padding:5px; margin-top:10px;");
		pcBottom.setParent(portalLayout);

		renderDashboardOverviewKinerja(data, pcTop);
		renderDashboardRadarKinerja(data, pcRadar);
		renderDashboardTopPegawai(data, pcLeft);
		renderDashboardTopKegiatan(data, pcRight);
		renderDashboardAktivitasTerbaru(data, pcBottom);
	}

	private DashboardKinerjaData loadDashboardKinerjaDataWithCache() {
		Long userId = (tbmuser != null) ? tbmuser.getId() : null;
		String key = DashboardCacheUtil.key("DasboardKinerja", "PERSONAL", userId);
		Object fromL2 = DashboardCacheUtil.getL2(key);
		if (fromL2 instanceof DashboardKinerjaData) return (DashboardKinerjaData) fromL2;
		DashboardKinerjaData d = loadDashboardKinerjaData();
		DashboardCacheUtil.putL2(key, d);
		return d;
	}

	private DashboardKinerjaData loadDashboardKinerjaData() {
		DashboardKinerjaData data = new DashboardKinerjaData();
		Session session = HibernateUtil.currentSession();
		try {
			Criteria countCriteria = createDashboardRealisasiCriteria(session, "", false);
			countCriteria.setProjection(Projections.rowCount());
			Object totalObj = countCriteria.uniqueResult();
			if (totalObj instanceof Number) {
				data.totalCatatan = ((Number) totalObj).longValue();
			}

			@SuppressWarnings("unchecked")
			List<RealisasiKerjaPegawai> latest = createDashboardRealisasiCriteria(session, "", true).setMaxResults(300)
					.list();
			data.latest = latest;

			java.util.Set<String> pegawaiUnik = new java.util.LinkedHashSet<String>();
			Date hariIni = ais.ui.util.WaktuUtil.getDate();

			for (RealisasiKerjaPegawai realisasi : latest) {
				String namaPegawai = safePegawaiName(realisasi);
				String namaKegiatan = safeKegiatanName(realisasi);
				Date tanggal = safeTanggalRealisasi(realisasi);

				if (namaPegawai != null && !namaPegawai.trim().isEmpty() && !"-".equals(namaPegawai)) {
					pegawaiUnik.add(namaPegawai);
					incrementMap(data.perPegawai, namaPegawai);
				}

				if (namaKegiatan != null && !namaKegiatan.trim().isEmpty() && !"-".equals(namaKegiatan)) {
					incrementMap(data.perKegiatan, namaKegiatan);
				}

				if (tanggal != null) {
					if (isSameDay(tanggal, hariIni)) {
						data.catatanHariIni++;
					}
					if (isSameMonth(tanggal, hariIni)) {
						data.catatanBulanIni++;
					}
				}

				if (isTerverifikasiAsesor(realisasi)) {
					data.terverifikasiAsesor++;
				} else {
					data.belumVerifikasiAsesor++;
				}

				String catatan = safeString(invokeGetter(realisasi, "getCatatan"));
				String keterangan = safeString(invokeGetter(realisasi, "getKeterangan"));
				if ((catatan == null || catatan.trim().isEmpty())
						&& (keterangan == null || keterangan.trim().isEmpty())) {
					data.catatanKosong++;
				}
			}
			data.totalPegawaiDalamSampel = pegawaiUnik.size();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return data;
	}

	private Criteria createDashboardRealisasiCriteria(Session session, String keyword, boolean order) {
		Criteria criteria = session.createCriteria(RealisasiKerjaPegawai.class)
				.createAlias("targetKerjaPegawai", "targetKerjaPegawai")
				.add(tbmuser == null || tbmuser.getPegawai() == null
						|| (tbmuser.hakAkses() != null && tbmuser.hakAkses().getMelihatDataPegawaiLain())
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("targetKerjaPegawai.pegawai", tbmuser.getPegawai()),
										Restrictions.eq("pegawai", tbmuser.getPegawai())));

		String c = keyword == null ? "" : keyword.trim();
		if (!c.isEmpty()) {
			criteria.createAlias("targetKerjaPegawai.kegiatanTugasJabatan", "kegiatanTugasJabatan")
					.add(Restrictions.or(Restrictions.ilike("kegiatanTugasJabatan.nama", c, MatchMode.ANYWHERE),
							Restrictions.or(Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE),
									Restrictions.ilike("catatan", c, MatchMode.ANYWHERE))));
		}

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		return criteria;
	}

	private void renderDashboardOverviewKinerja(DashboardKinerjaData data, org.zkoss.zk.ui.Component parent) {
		Panelchildren pch = createDashboardPanel("Overview Kinerja LKP", parent,
				"padding:15px; background:#ffffff; border-radius:12px;");
		org.zkoss.zul.Hbox cardsBox = new org.zkoss.zul.Hbox();
		cardsBox.setWidth("100%");
		cardsBox.setStyle("display:flex; flex-wrap:wrap; gap:10px; justify-content:center;");
		cardsBox.setParent(pch);

		createDashboardCard("TOTAL CATATAN", String.valueOf(data.totalCatatan), "Seluruh catatan sesuai hak akses",
				"#f8f9fa", "#212529", cardsBox);
		createDashboardCard("DATA TERBARU", String.valueOf(data.latest.size()), "Sampel 300 catatan terakhir",
				"#cff4fc", "#055160", cardsBox);
		createDashboardCard("PEGAWAI AKTIF", String.valueOf(data.totalPegawaiDalamSampel),
				"Pegawai pada sampel terbaru", "#d1e7dd", "#0f5132", cardsBox);
		createDashboardCard("HARI INI", String.valueOf(data.catatanHariIni), "Catatan bertanggal hari ini", "#e2e3e5",
				"#383d41", cardsBox);
		createDashboardCard("BULAN INI", String.valueOf(data.catatanBulanIni), "Catatan pada bulan berjalan", "#cfe2ff",
				"#084298", cardsBox);
		createDashboardCard("TERVERIFIKASI", String.valueOf(data.terverifikasiAsesor), "Catatan diverifikasi asesor",
				"#e0cffc", "#3b0918", cardsBox);
		createDashboardCard("BELUM VERIFIKASI", String.valueOf(data.belumVerifikasiAsesor),
				"Perlu tindak lanjut asesor", "#fff3cd", "#664d03", cardsBox);
		createDashboardCard("CATATAN KOSONG", String.valueOf(data.catatanKosong), "Keterangan/catatan belum lengkap",
				"#f8d7da", "#842029", cardsBox);
	}

	private void renderDashboardTopPegawai(DashboardKinerjaData data, org.zkoss.zk.ui.Component parent) {
		Panelchildren pch = createDashboardPanel("Pegawai dengan Aktivitas Catatan Terbanyak", parent, null);
		ais.ui.util.MyGrid grid = createDashboardGrid(pch, 10, "100%");
		addDashboardColumns(grid, new String[] { "Peringkat", "Pegawai", "Jumlah Catatan", "Insight" });
		Rows rows = new Rows();
		rows.setParent(grid);

		List<Map.Entry<String, Integer>> entries = sortMapByValueDesc(data.perPegawai);
		int no = 1;
		for (Map.Entry<String, Integer> entry : entries) {
			Row row = new Row();
			row.setParent(rows);
			row.appendChild(new org.zkoss.zul.Label("#" + no));
			row.appendChild(new org.zkoss.zul.Label(entry.getKey()));
			appendDashboardBadge(row, String.valueOf(entry.getValue()),
					entry.getValue().intValue() >= 10 ? "#198754" : "#0d6efd");
			row.appendChild(new org.zkoss.zul.Label(entry.getValue().intValue() >= 10 ? "Sangat aktif" : "Aktif"));
			no++;
			if (no > 10) {
				break;
			}
		}
		appendEmptyRowIfNeeded(rows, entries.isEmpty(), 4, "Belum ada data pegawai yang dapat diringkas.");
	}

	private void renderDashboardTopKegiatan(DashboardKinerjaData data, org.zkoss.zk.ui.Component parent) {
		Panelchildren pch = createDashboardPanel("Kegiatan / Target Kerja yang Paling Sering Dicatat", parent, null);
		ais.ui.util.MyGrid grid = createDashboardGrid(pch, 10, "100%");
		addDashboardColumns(grid, new String[] { "Peringkat", "Kegiatan", "Jumlah Catatan", "Rekomendasi" });
		Rows rows = new Rows();
		rows.setParent(grid);

		List<Map.Entry<String, Integer>> entries = sortMapByValueDesc(data.perKegiatan);
		int no = 1;
		for (Map.Entry<String, Integer> entry : entries) {
			Row row = new Row();
			row.setParent(rows);
			row.appendChild(new org.zkoss.zul.Label("#" + no));
			row.appendChild(new org.zkoss.zul.Label(shortText(entry.getKey(), 90)));
			appendDashboardBadge(row, String.valueOf(entry.getValue()),
					entry.getValue().intValue() >= 10 ? "#198754" : "#0d6efd");
			row.appendChild(new org.zkoss.zul.Label(
					entry.getValue().intValue() >= 10 ? "Pertahankan konsistensi output" : "Monitor progres"));
			no++;
			if (no > 10) {
				break;
			}
		}
		appendEmptyRowIfNeeded(rows, entries.isEmpty(), 4, "Belum ada data kegiatan yang dapat diringkas.");
	}

	private void renderDashboardAktivitasTerbaru(DashboardKinerjaData data, org.zkoss.zk.ui.Component parent) {
		Panelchildren pch = createDashboardPanel("Aktivitas Catatan Harian Terbaru", parent, null);
		ais.ui.util.MyGrid grid = createDashboardGrid(pch, 15, "100%");
		addDashboardColumns(grid, new String[] { "Tanggal", "Pegawai", "Kegiatan / Catatan", "Keterangan", "Status" });
		Rows rows = new Rows();
		rows.setParent(grid);

		int no = 0;
		for (RealisasiKerjaPegawai realisasi : data.latest) {
			Row row = new Row();
			row.setParent(rows);
			Date tanggal = realisasi.getTanggalWaktu();
			Date tanggalSampai = realisasi.getTanggalWaktuSampai();
			row.appendChild(new org.zkoss.zul.Label((tanggal == null ? "-" : formatTanggalDashboard(tanggal)) + " sd "
					+ (tanggalSampai == null ? "-" : formatTanggalDashboard(tanggalSampai))));
			row.appendChild(new org.zkoss.zul.Label(safePegawaiName(realisasi)));
			row.appendChild(new org.zkoss.zul.Label(shortText(safeKegiatanName(realisasi), 100)));
			String keterangan = safeString(invokeGetter(realisasi, "getKeterangan"));
			if (keterangan == null || keterangan.trim().isEmpty()) {
				keterangan = safeString(invokeGetter(realisasi, "getCatatan"));
			}
			row.appendChild(new org.zkoss.zul.Label(shortText(keterangan, 120)));
			appendDashboardBadge(row, isTerverifikasiAsesor(realisasi) ? "Terverifikasi" : "Belum Verifikasi",
					isTerverifikasiAsesor(realisasi) ? "#198754" : "#fd7e14");
			no++;
			if (no >= 15) {
				break;
			}
		}
		appendEmptyRowIfNeeded(rows, data.latest == null || data.latest.isEmpty(), 5,
				"Belum ada aktivitas catatan harian.");
	}

	private Panelchildren createDashboardPanel(String title, org.zkoss.zk.ui.Component parent, String style) {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setTitle(title);
		panel.setBorder("normal");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle(
				"margin-bottom:12px; border-radius:12px; border:1px solid #e9ecef; box-shadow:0 2px 6px rgba(0,0,0,0.04);");
		panel.setParent(parent);
		Panelchildren pch = new Panelchildren();
		pch.setStyle(style == null ? "padding:10px; background:#ffffff;" : style);
		pch.setParent(panel);
		appendPanelDescriptionEndUserV27(pch, title);
		return pch;
	}

	private ais.ui.util.MyGrid createDashboardGrid(org.zkoss.zk.ui.Component parent, int pageSize, String width) {
		ais.ui.util.MyGrid grid = new ais.ui.util.MyGrid();
		grid.setMold("paging");
		grid.setPageSize(pageSize);
		grid.setSclass("dgrid fgrid table-striped");
		grid.setWidth(width == null ? "100%" : width);
		grid.setStyle("border:0px; background:transparent;");
		grid.setParent(parent);
		try {
			grid.getPagingChild().setMold("os");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/employ/DasboardKinerja.java:769");
		}
		return grid;
	}

	private void addDashboardColumns(Grid grid, String[] titles) {
		Columns columns = new Columns();
		columns.setParent(grid);
		for (String title : titles) {
			new MyColumnConfig(title).setParent(columns);
		}
	}

	private void createDashboardCard(String title, String value, String sub, String bg, String text,
			org.zkoss.zul.Hbox parent) {
		org.zkoss.zul.Div card = new org.zkoss.zul.Div();
		card.setStyle("padding:15px; background-color:" + bg
				+ "; border-radius:12px; flex:1 1 12%; min-width:140px; text-align:center; box-shadow:0 2px 5px rgba(0,0,0,0.06);");
		card.setParent(parent);
		org.zkoss.zul.Label titleLabel = new org.zkoss.zul.Label(title);
		titleLabel.setStyle("font-size:11px; font-weight:600; color:" + text + "; opacity:0.82;");
		card.appendChild(titleLabel);
		card.appendChild(new org.zkoss.zul.Html("<br/>"));
		org.zkoss.zul.Label valueLabel = new org.zkoss.zul.Label(value);
		valueLabel.setStyle("font-size:26px; font-weight:700; color:" + text + ";");
		card.appendChild(valueLabel);
		card.appendChild(new org.zkoss.zul.Html("<br/>"));
		org.zkoss.zul.Label subLabel = new org.zkoss.zul.Label(sub);
		subLabel.setStyle("font-size:10px; color:" + text + "; opacity:0.72;");
		card.appendChild(subLabel);
	}

	private void appendDashboardBadge(Row row, String text, String bg) {
		org.zkoss.zul.Label label = new org.zkoss.zul.Label(text == null ? "-" : text);
		label.setStyle("display:inline-block; color:#ffffff; background:" + bg
				+ "; border-radius:999px; padding:3px 9px; font-size:11px; font-weight:700;");
		row.appendChild(label);
	}

	private void appendEmptyRowIfNeeded(Rows rows, boolean empty, int colspan, String message) {
		if (!empty) {
			return;
		}
		Row row = new Row();
		row.setParent(rows);
		org.zkoss.zul.Label label = new org.zkoss.zul.Label(message);
		label.setStyle("color:#6c757d; font-style:italic; padding:10px;");
		label.setParent(row);
		try {
			label.setAttribute("colspan", String.valueOf(colspan));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/employ/DasboardKinerja.java:819");
		}
	}

	private List<Map.Entry<String, Integer>> sortMapByValueDesc(Map<String, Integer> map) {
		List<Map.Entry<String, Integer>> entries = new java.util.ArrayList<Map.Entry<String, Integer>>(map.entrySet());
		java.util.Collections.sort(entries, new java.util.Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				int compare = o2.getValue().compareTo(o1.getValue());
				return compare != 0 ? compare : o1.getKey().compareToIgnoreCase(o2.getKey());
			}
		});
		return entries;
	}

	private void incrementMap(Map<String, Integer> map, String key) {
		Integer value = map.get(key);
		map.put(key, value == null ? 1 : value + 1);
	}

	private String safePegawaiName(RealisasiKerjaPegawai realisasi) {
		Object pegawai = invokeGetter(realisasi, "getPegawai");
		String nama = safeString(invokeGetter(pegawai, "getNama"));
		return nama == null || nama.trim().isEmpty() ? "-" : nama;
	}

	private String safeKegiatanName(RealisasiKerjaPegawai realisasi) {
		Object target = invokeGetter(realisasi, "getTargetKerjaPegawai");
		Object kegiatan = invokeGetter(target, "getKegiatanTugasJabatan");
		String nama = safeString(invokeGetter(kegiatan, "getNama"));
		if (nama != null && !nama.trim().isEmpty()) {
			return nama;
		}
		nama = safeString(invokeGetter(target, "getNama"));
		if (nama != null && !nama.trim().isEmpty()) {
			return nama;
		}
		nama = safeString(invokeGetter(realisasi, "getKeterangan"));
		if (nama != null && !nama.trim().isEmpty()) {
			return nama;
		}
		nama = safeString(invokeGetter(realisasi, "getCatatan"));
		return nama == null || nama.trim().isEmpty() ? "-" : nama;
	}

	private Date safeTanggalRealisasi(RealisasiKerjaPegawai realisasi) {
		String[] methods = new String[] { "getTanggal", "getTanggalRealisasi", "getTglRealisasi", "getTgl", "getWaktu",
				"getCreateDate", "getCreatedDate", "getCreatedAt", "getTanggalInput", "getInputDate" };
		for (String method : methods) {
			Object value = invokeGetter(realisasi, method);
			if (value instanceof Date) {
				return (Date) value;
			}
		}
		return null;
	}

	private boolean isTerverifikasiAsesor(RealisasiKerjaPegawai realisasi) {
		String[] methods = new String[] { "getVerifikasiAsesor", "isVerifikasiAsesor", "getDiverifikasiAsesor",
				"isDiverifikasiAsesor", "getTerverifikasi", "isTerverifikasi", "getVerified", "isVerified" };
		for (String method : methods) {
			Object value = invokeGetter(realisasi, method);
			if (value instanceof Boolean) {
				return ((Boolean) value).booleanValue();
			}
			if (value instanceof Number) {
				return ((Number) value).intValue() != 0;
			}
			if (value != null) {
				String text = String.valueOf(value).trim().toLowerCase();
				if ("true".equals(text) || "ya".equals(text) || "y".equals(text) || "1".equals(text)
						|| "approved".equals(text) || "verified".equals(text)) {
					return true;
				}
			}
		}
		return false;
	}

	private Object invokeGetter(Object target, String methodName) {
		if (target == null || methodName == null) {
			return null;
		}
		try {
			java.lang.reflect.Method method = target.getClass().getMethod(methodName, new Class[] {});
			return method.invoke(target, new Object[] {});
		} catch (Exception e) {
			return null;
		}
	}

	private String safeString(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private String shortText(String value, int maxLength) {
		String text = value == null || value.trim().isEmpty() ? "-" : value.trim();
		if (text.length() <= maxLength) {
			return text;
		}
		return text.substring(0, Math.max(0, maxLength - 3)) + "...";
	}

	private String formatTanggalDashboard(Date date) {
		try {
			return Common.dateFormat4.get().format(date);
		} catch (Exception e) {
			return Common.dateFormat.get().format(date);
		}
	}

	private boolean isSameDay(Date left, Date right) {
		if (left == null || right == null) {
			return false;
		}
		java.util.Calendar c1 = java.util.Calendar.getInstance();
		java.util.Calendar c2 = java.util.Calendar.getInstance();
		c1.setTime(left);
		c2.setTime(right);
		return c1.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR)
				&& c1.get(java.util.Calendar.DAY_OF_YEAR) == c2.get(java.util.Calendar.DAY_OF_YEAR);
	}

	private boolean isSameMonth(Date left, Date right) {
		if (left == null || right == null) {
			return false;
		}
		java.util.Calendar c1 = java.util.Calendar.getInstance();
		java.util.Calendar c2 = java.util.Calendar.getInstance();
		c1.setTime(left);
		c2.setTime(right);
		return c1.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR)
				&& c1.get(java.util.Calendar.MONTH) == c2.get(java.util.Calendar.MONTH);
	}

	/**
	 * Radar/spider "kesehatan kinerja LKP" dari data yang SUDAH dimuat (tanpa query
	 * tambahan). Lima indikator dinormalkan 0–100 agar mudah dibaca sekilas:
	 * Kelengkapan catatan, Verifikasi asesor, Aktivitas bulan ini, Aktivitas hari
	 * ini, dan Keterlibatan pegawai. HTML/CSS murni via {@link ais.ui.util.DashboardUiKit#spider} (tanpa JFreeChart).
	 */
	private void renderDashboardRadarKinerja(DashboardKinerjaData data, org.zkoss.zk.ui.Component parent) {
		if (data == null || parent == null) {
			return;
		}
		Panelchildren pch = createDashboardPanel("Radar Kesehatan Kinerja LKP", parent, null);
		long total = data.totalCatatan > 0 ? data.totalCatatan
				: (data.latest == null ? 0L : (long) data.latest.size());
		int verifTotal = data.terverifikasiAsesor + data.belumVerifikasiAsesor;
		String[] label = new String[] { "Kelengkapan", "Verifikasi", "Bulan Ini", "Hari Ini", "Pegawai Aktif" };
		int[] nilai = new int[] {
				pctInt(total - data.catatanKosong, total),
				pctInt(data.terverifikasiAsesor, verifTotal),
				pctInt(data.catatanBulanIni, total),
				pctInt(data.catatanHariIni, Math.max(1, data.catatanBulanIni)),
				Math.min(100, data.totalPegawaiDalamSampel * 10)
		};
		new org.zkoss.zul.Html(ais.ui.util.DashboardUiKit.spider(
				"Radar Kesehatan Kinerja",
				"Semakin lebar dan seimbang jaringnya, semakin sehat aktivitas pencatatan kinerja pegawai.",
				label, nilai)).setParent(pch);
	}

	/** Persentase bulat 0–100 yang aman terhadap pembagi nol. */
	private int pctInt(long part, long whole) {
		if (whole <= 0) {
			return 0;
		}
		int p = (int) Math.round(part * 100.0 / whole);
		return p < 0 ? 0 : (p > 100 ? 100 : p);
	}

	private static class DashboardKinerjaData {
		long totalCatatan = 0L;
		int totalPegawaiDalamSampel = 0;
		int catatanHariIni = 0;
		int catatanBulanIni = 0;
		int terverifikasiAsesor = 0;
		int belumVerifikasiAsesor = 0;
		int catatanKosong = 0;
		List<RealisasiKerjaPegawai> latest = new java.util.ArrayList<RealisasiKerjaPegawai>();
		Map<String, Integer> perPegawai = new java.util.LinkedHashMap<String, Integer>();
		Map<String, Integer> perKegiatan = new java.util.LinkedHashMap<String, Integer>();
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
