package ais.action.master.surat.helper;
import ais.ui.util.DashboardGridExportHelper;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


/* DASBOARD_SURAT_V20_UIUX_EXPLANATION_REFACTOR_2026_06_04
 * - Menambahkan penjelasan end-user pada panel legacy dan modern.
 * - Menjaga semua chart/grafik memakai HTML/CSS.
 * - Mengurangi log debug yang tidak perlu pada mode produksi.
 */






















/* DASBOARD_SURAT_V19A_FIX_LOADING_HELPER_SCOPE_2026_05_30 */
/* DASBOARD_SURAT_V19_LOADING_PROGRESS_2026_05_30 */
/* DASBOARD_SURAT_V18_SIMPLE_NODE_TIMELINE_TOP_ALIGN_2026_05_29 */
/* DASBOARD_SURAT_V17C_FIX_UNTUK_SAYA_STRICT_PEJABAT_2026_05_29 */
/* DASBOARD_SURAT_V17B_DEFAULT_UNTUK_SAYA_CHECKED_2026_05_29 */
/* DASBOARD_SURAT_V17A_FIX_UNTUK_SAYA_SCOPE_2026_05_29 */
/* DASBOARD_SURAT_V17_FILTER_UNTUK_SAYA_MELIHAT_SEMUA_2026_05_29 */
/* DASBOARD_SURAT_V16A_FIX_MISSING_OR_ACCESS_HELPER_2026_05_29 */
/* DASBOARD_SURAT_V16_OR_VISIBILITY_OR_PEJABAT_ACCESS_2026_05_29 */
/* DASBOARD_SURAT_V15_COMPACT_KLASIFIKASI_PARAMETER_2026_05_29 */
/* DASBOARD_SURAT_V14_GROUP_ALUR_BY_PARENT_TIMELINE_2026_05_29 */
/* DASBOARD_SURAT_V13_RELOAD_PENGAJUAN_IGNORE_GLOBAL_FILTER_2026_05_29 */
/* DASBOARD_SURAT_ACCESS_CONTROL_V12_FIX_DUPLICATE_KLASIFIKASI_ALIAS_2026_05_28 */
/* DASBOARD_SURAT_ACCESS_CONTROL_V11_2026_05_28 */
/* DASBOARD_SURAT_ENHANCED_UIUX_V10_REMOVE_UNUSED_SINGLE_TAB_2026_05_28 */
/* DASBOARD_SURAT_ENHANCED_UIUX_V9_GLOBAL_FILTER_2026_05_27 */
/* DASBOARD_SURAT_V8_COMPILE_FIX_CREATE_DETAIL_PROVIDER_2026_05_27 */
/* DASBOARD_SURAT_ENHANCED_UIUX_V8_CLICKABLE_DETAIL_POPUP_2026_05_27 */
/* DASBOARD_SURAT_ENHANCED_UIUX_V7_SOP_TEMPLATE_2026_05_27 */
/* DASBOARD_SURAT_ENHANCED_UIUX_V6_REORDER_EXISTING_AFTER_OVERVIEW_2026_05_27 */
/* TATA_KELOLA_SURAT_V5_DEBUG_BOOLEAN_TRUE_2026_05_27 */
/* ENHANCED_UIUX_V4_RENAMED_FILE_MARKER_2026_05_27 */
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.MoveEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.surat.AlurPersetujuanSuratKeluarStatusAction;
import ais.action.master.surat.AlurPersetujuanSuratMasukStatusAction;
import ais.action.master.surat.SuratKeluarAction;
import ais.action.master.surat.SuratMasukAction;
import ais.action.master.surat.util.SuratUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.Tbmrole;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.file.FotoGambarSuratKeluar;
import ais.database.model.file.FotoGambarSuratMasuk;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Pejabat;
import ais.database.model.surat.AlurPersetujuanSuratKeluarStatus;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;
import ais.database.model.surat.KlasifikasiSuratKeluarParemeterValue;
import ais.database.model.surat.OpsiSuratKeluarValue;
import ais.database.model.surat.OpsiSuratMasukValue;
import ais.database.model.surat.SuratKeluar;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBoldHijau;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Memantau surat masuk, surat keluar, disposisi, revisi, dan arsip agar pengelolaan persuratan lebih tertib.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardSurat extends MyPortallayout {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9006490521125337935L;
	private static final int DASHBOARD_SURAT_SAMPLE_LIMIT = 500;
	private static final String EVENT_RENDER_DASBOR_SURAT_V19 = "onRenderDasborSuratV19";
	/* SOURCE_TEMPLATE_DASBOARD_SOP_V7: renderInternalDasborOverview + renderAnalyticDasborTambahan + createModernPanel */
	
	/*
	 * DEBUG GLOBAL DASHBOARD SURAT 
	 * true  = tampilkan error dashboard ke console.
	 * false = sembunyikan error minor dashboard agar UI utama tidak terganggu.
	 */
	private static boolean debug = false;

	public static boolean isDebug() {
		return debug;
	}

	public static void setDebug(boolean debugValue) {
		debug = debugValue;
	}

	private static void debugError(String context, Exception e) {
		if (debug) {
			System.err.println("[DasboardSurat DEBUG] " + context);
			if (e != null) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/DasboardSurat.java:165");
			}
		}
	}

	private void tampilkanLoadingDashboardSuratV19(Component parent, String judul, String detail, int persen) {
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
			final Vbox containerDasborGrid = new Vbox();
			containerDasborGrid.setWidth("100%");
			containerDasborGrid.setStyle("box-sizing:border-box; padding:14px;");
			Html htmlLoading = new Html(buildLoadingDashboardSuratHtmlV19(judul, detail, persen));
			containerDasborGrid.appendChild(htmlLoading);
			parent.appendChild(containerDasborGrid);
		} catch (Exception e) {
			debugError("tampilkanLoadingDashboardSuratV19", e);
		}
	}

	private String buildLoadingDashboardSuratHtmlV19(String judul, String detail, int persen) {
		String title = judul == null ? "Memproses Dasbor Surat..." : judul;
		String desc = detail == null ? "Mohon tunggu, sistem sedang mengambil data persuratan." : detail;
		return "<div style='padding:18px; border-radius:18px; background:#ffffff; border:1px solid #e5e7eb;"
				+ "box-shadow:0 12px 28px rgba(15,23,42,.08); color:#334155; font-family:Arial, sans-serif;'>"
				+ "<div style='display:flex; align-items:center; gap:12px;'>"
				+ "<div style='width:42px; height:42px; border-radius:14px; display:flex; align-items:center; justify-content:center;"
				+ "background:linear-gradient(135deg,#0f766e,#22c55e); color:#fff; font-size:18px;'>"
				+ "<i class=\"fa fa-spinner fa-spin\"></i></div>"
				+ "<div style='flex:1; min-width:0;'>"
				+ "<div style='font-size:15px; font-weight:900; color:#0f172a;'>" + safeHtml(title) + "</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:3px; line-height:1.45;'>" + safeHtml(desc)
				+ "</div></div>"
				+ "<div style='font-size:18px; font-weight:900; color:#0f766e;'>" + persen + "%</div></div>"
				+ "<div style='margin-top:14px; height:10px; border-radius:999px; background:#e2e8f0; overflow:hidden;'>"
				+ "<div style='height:10px; width:" + persen
				+ "%; border-radius:999px; background:linear-gradient(90deg,#0f766e,#22c55e);'></div></div>"
				+ "<div style='margin-top:10px; display:flex; gap:6px; flex-wrap:wrap;'>"
				+ "<span style='font-size:10px; font-weight:800; color:#075985; background:#e0f2fe; border-radius:999px; padding:4px 8px;'>Ringkasan</span>"
				+ "<span style='font-size:10px; font-weight:800; color:#166534; background:#dcfce7; border-radius:999px; padding:4px 8px;'>Persetujuan</span>"
				+ "<span style='font-size:10px; font-weight:800; color:#7c2d12; background:#ffedd5; border-radius:999px; padding:4px 8px;'>Disposisi</span>"
				+ "<span style='font-size:10px; font-weight:800; color:#5b21b6; background:#ede9fe; border-radius:999px; padding:4px 8px;'>Analitik</span>"
				+ "</div></div>";
	}


	/**
	 * Tipe implementasi bersarang {@link DashboardSuratRenderRequestV19} milik {@link DasboardSurat}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardSurat}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code org.zkoss.zul.Div body}, {@code Date
	 * mulai}, {@code Date sampai}, {@code ais.database.model.rab.SatuanKerja satuanKerja}, {@code String keyword}.
	 * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see DasboardSurat
	 */
	private static class DashboardSuratRenderRequestV19 {
		org.zkoss.zul.Div body;
		Date mulai;
		Date sampai;
		ais.database.model.rab.SatuanKerja satuanKerja;
		String keyword;

		DashboardSuratRenderRequestV19(org.zkoss.zul.Div body, Date mulai, Date sampai,
				ais.database.model.rab.SatuanKerja satuanKerja, String keyword) {
			this.body = body;
			this.mulai = mulai;
			this.sampai = sampai;
			this.satuanKerja = satuanKerja;
			this.keyword = keyword;
		}
	}


	/**
	 * Kontrak callback/strategi bersarang milik {@link DasboardSurat}. Tipe ini memisahkan satu variasi perilaku
	 * lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DasboardSurat} dan dapat mengakses state kelas
	 * induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
	 * implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DasboardSurat
	 */
	private interface LegacyDashboardRenderer {
		void render() throws Exception;
	}

	private LegacyDashboardRenderer legacyDashboardRenderer;
	private MyPortallayout dashboardLegacyLayout;
	private String dashboardDetailBridgeIdV8;
	private Date dashboardGlobalMulaiV9;
	private Date dashboardGlobalSampaiV9;
	private ais.database.model.rab.SatuanKerja dashboardGlobalSatuanKerjaV9;
	private String dashboardGlobalKeywordV9;
	private boolean legacyDashboardRenderedInlineV9;

	private Tbmuser tbmuser;

	public DasboardSurat() throws Exception {
		super();
		// setHeight("25000px");
		setWidth("100%");
		setMaximizedMode("whole");
		tbmuser = Common.getCurrentUser();
		try {
			init();
		} catch (Exception e) {
			debugError("constructor-init", e);
			throw e;
		}
	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Surat");
		EventListener reloadPengajuan = new EventListener() {

			private Date mulai = null;
			private Date sampai = new Date();
			private Boolean blm = false;

			private void pengajuanBaru() throws Exception {

				if (mulai == null) {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
					mulai = calendar.getTime();
				}

				// V13: Abaikan filter global dashboard.
				// memiliki form pencarian sendiri, sehingga mulai/sampai/keyword/satker
				// harus berasal dari komponen pencarian di bagian ini.
				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(getCurrentDashboardPortalParent());
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle("Pengajuan Surat Anda");
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

						if (debug) { System.out.println("left -> " + left + ", top -> " + top); }
					}
				});
				panel.setStyle(legacyPanelStyleV7());
				final Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				EventListener pengajuanBaruEventListener = new EventListener() {

					public EventListener getThis() {
						return this;
					}

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);
						appendLegacyPanelIntroV20(panelchildren, "Pengajuan Surat Anda");

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setStyle(toolbarStyleV7());
						toolbar.setParent(rowUtamapalingAwal);

						final Textbox cari = new Textbox();

						MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Pengajuan Surat Baru",
								"/img/svg/form-one.svg");

						toolbarbutton.setParent(toolbar);
						toolbarbutton.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								SuratKeluarAction.onAddExternal(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.clear(DasboardSurat.this);
										DasboardSurat.this.init();
									}
								}, new SuratKeluar());

							}
						});

						final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
						ambilDataSatuanKerjaBanbox.setCols(5);
						ambilDataSatuanKerjaBanbox.setReadonly(true);
						ambilDataSatuanKerjaBanbox.setParent(toolbar);

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setValue("");
						cari.setParent(toolbar);

						new MyLabelAgakKecil("Tgl:").setParent(toolbar);

						final MyDatebox searchmulai = new MyDatebox(mulai);
						final MyDatebox searchsampai = new MyDatebox(sampai);

						searchmulai.setCols(4);
						searchsampai.setCols(4);

						searchmulai.setReadonly(true);
						searchsampai.setReadonly(true);

						searchmulai.setParent(toolbar);
						searchsampai.setParent(toolbar);

						MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
						refresh.setTooltiptext("Refresh");
						refresh.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {

								String c = cari.getValue().trim();

								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(SuratKeluar.class)
										.createAlias("klasifikasiSuratKeluar", "aksesKlasifikasiSuratKeluar", Criteria.LEFT_JOIN)

										.add(ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja") == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("satuanKerja",
														ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja")))

										.add(searchmulai.getValue() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.ge("tanggal", searchmulai.getValue()))
										.add(searchsampai.getValue() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.le("tanggal", searchsampai.getValue()))

										.add(createSuratKeluarVisibilityCriterion("", "aksesKlasifikasiSuratKeluar."))

										.add(createSuratKeluarKeywordCriterion("", c)

										);

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

							@Override
							public void onEvent(Event event) {

								tampilkanLoadingDashboardSuratV19(rowUtamaData, "Mengambil Data Pengajuan Surat", "Memuat daftar pengajuan sesuai filter panel...", 35);

								Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

								List<SuratKeluar> suratKeluars = ((Criteria) dataCriteria.initCriteria(true))
										.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage())))
										.setMaxResults(5).list();

								Common.clear(rowUtamaData);

								MyGrid grid = new MyGrid();
								// grid.setSclass("dgrid");
								grid.setParent(rowUtamaData);
								grid.setSclass("fgrid");
								grid.setStyle("min-height:100px; border:0; background:transparent; border-radius:14px; overflow:hidden;");
								grid.setMold("paging");
								grid.setPageSize(10);
								grid.getPagingChild().setMold("os");

								Rows rows = new Rows();
								rows.setParent(grid);

								for (final SuratKeluar suratKeluar : suratKeluars) {
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									Vbox vbox1 = new Vbox();
									vbox1.setParent(rowUtamaLagi);

									Vbox a;
									(a = new Vbox()).setParent(vbox1);
									Vbox vbox = new Vbox();
									a.appendChild(vbox);

									vbox.appendChild(
											new MyLabelBoldAja(suratKeluar.getKlasifikasiSuratKeluar().getNama()));
									vbox.appendChild(new MyLabelAgakKecil(suratKeluar.getKode()));
									vbox.appendChild(new MyLabelAgakKecil(suratKeluar.getPerihal()));
									appendKlasifikasiSuratKeluarParameterCompactV15(vbox, suratKeluar);
									vbox.appendChild(new MyLabelAgakKecil(
											(suratKeluar.getAgenda() == null ? "" : suratKeluar.getAgenda())
													+ " (pengajuan "
													+ Common.dateFormat61.get().format(suratKeluar.getWaktu()) + ")"));

									if (suratKeluar.getAlurDitolak() != null
											&& suratKeluar.getAlurDitolak().getTelahDirevisi()) {
										try {
											vbox.appendChild(new MyLabelAgakKecilBoldMerah("Direvisi dengan catatan : "
													+ suratKeluar.getAlurDitolak().getCatatanRevisi()));
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/DasboardSurat.java:481");
										}
										try {
											vbox.appendChild(
													new MyLabelAgakKecilBoldMerah("Sebelumnya ditolak dengan catatan : "
															+ suratKeluar.getAlurDitolak().getKeterangan()));
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/DasboardSurat.java:488");
										}
									} else if (suratKeluar.getAlurDitolak() != null
											&& suratKeluar.getAlurDitolak().getDitolak()) {
										try {
											vbox.appendChild(new MyLabelAgakKecilBoldMerah("Ditolak dengan catatan : "
													+ suratKeluar.getAlurDitolak().getKeterangan()));
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/DasboardSurat.java:496");
										}
									}

									String html = "";
									Session session = HibernateUtil.currentSession();
									List<AlurPersetujuanSuratKeluarStatus> alurPersetujuanSuratKeluarStatuss = session
											.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
											.add(Restrictions.isNotNull("kodeUnik"))
											.add(Restrictions.eq("suratKeluar", suratKeluar)).addOrder(Order.asc("id"))
											.list();
									for (AlurPersetujuanSuratKeluarStatus myAlurPersetujuanSuratKeluarStatus : alurPersetujuanSuratKeluarStatuss) {
										if (myAlurPersetujuanSuratKeluarStatus.getJenisJabatan() == null
												&& myAlurPersetujuanSuratKeluarStatus
														.getAlurPersetujuanSuratKeluar() != null
												&& myAlurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar()
														.getJenisJabatan() != null) {
											html += "<li>"
													+ myAlurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar()
													+ " : "
													+ (myAlurPersetujuanSuratKeluarStatus.getDisetujui()
															? ("<font style=\"font-size: x-small;color:blue;font-weight: bolder;\">Sudah ditindak-lanjuti "
																	+ (myAlurPersetujuanSuratKeluarStatus
																			.getPejabat() == null
																			|| myAlurPersetujuanSuratKeluarStatus
																					.getPejabat().getPegawai() == null
																							? (myAlurPersetujuanSuratKeluarStatus
																									.getPejabat()
																									.getDosen() == null
																											? ""
																											: " " + myAlurPersetujuanSuratKeluarStatus
																													.getPejabat()
																													.getDosen()
																													.getNama())
																							: " " + myAlurPersetujuanSuratKeluarStatus
																									.getPejabat()
																									.getPegawai()
																									.getNama())
																	+ (myAlurPersetujuanSuratKeluarStatus
																			.getWaktuPersetujuan() == null
																					? ""
																					: " pada waktu "
																							+ Common.dateFormat3.get().format(
																									myAlurPersetujuanSuratKeluarStatus
																											.getWaktuPersetujuan()))
																	+ "</font>")
															: myAlurPersetujuanSuratKeluarStatus.getDitolak()
																	? ("<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Ditolak "
																			+ (myAlurPersetujuanSuratKeluarStatus
																					.getPejabat() == null
																					|| myAlurPersetujuanSuratKeluarStatus
																							.getPejabat()
																							.getPegawai() == null
																									? (myAlurPersetujuanSuratKeluarStatus
																											.getPejabat()
																											.getDosen() == null
																													? ""
																													: " " + myAlurPersetujuanSuratKeluarStatus
																															.getPejabat()
																															.getDosen()
																															.getNama())
																									: " " + myAlurPersetujuanSuratKeluarStatus
																											.getPejabat()
																											.getPegawai()
																											.getNama())
																			+ (myAlurPersetujuanSuratKeluarStatus
																					.getWaktuDitolak() == null
																							? ""
																							: " pada waktu "
																									+ Common.dateFormat3.get()
																											.format(myAlurPersetujuanSuratKeluarStatus
																													.getWaktuDitolak()))
																			+ "</font>")
																	: "<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Menunggu Persetujuan"
																			+ (myAlurPersetujuanSuratKeluarStatus
																					.getPejabat() == null
																							? ""
																							: " " + myAlurPersetujuanSuratKeluarStatus
																									.getPejabat()
																									.getNama())
																			+ "</font>")
													+ "</li>";
										} else if (myAlurPersetujuanSuratKeluarStatus.getJenisJabatan() != null) {
											html += "<li>"
													+ myAlurPersetujuanSuratKeluarStatus.getJenisJabatan().getNama()
													+ " : "
													+ (myAlurPersetujuanSuratKeluarStatus.getDisetujui()
															? ("<font style=\"font-size: x-small;color:blue;font-weight: bolder;\">Sudah ditindak-lanjuti "
																	+ (myAlurPersetujuanSuratKeluarStatus
																			.getPejabat() == null
																			|| myAlurPersetujuanSuratKeluarStatus
																					.getPejabat().getPegawai() == null
																							? (myAlurPersetujuanSuratKeluarStatus
																									.getPejabat()
																									.getDosen() == null
																											? ""
																											: " " + myAlurPersetujuanSuratKeluarStatus
																													.getPejabat()
																													.getDosen()
																													.getNama())
																							: " " + myAlurPersetujuanSuratKeluarStatus
																									.getPejabat()
																									.getPegawai()
																									.getNama())
																	+ (myAlurPersetujuanSuratKeluarStatus
																			.getWaktuPersetujuan() == null
																					? ""
																					: " pada waktu "
																							+ Common.dateFormat3.get().format(
																									myAlurPersetujuanSuratKeluarStatus
																											.getWaktuPersetujuan()))
																	+ "</font>")
															: myAlurPersetujuanSuratKeluarStatus.getDitolak()
																	? ("<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Ditolak "
																			+ (myAlurPersetujuanSuratKeluarStatus
																					.getPejabat() == null
																					|| myAlurPersetujuanSuratKeluarStatus
																							.getPejabat()
																							.getPegawai() == null
																									? (myAlurPersetujuanSuratKeluarStatus
																											.getPejabat()
																											.getDosen() == null
																													? ""
																													: " " + myAlurPersetujuanSuratKeluarStatus
																															.getPejabat()
																															.getDosen()
																															.getNama())
																									: " " + myAlurPersetujuanSuratKeluarStatus
																											.getPejabat()
																											.getPegawai()
																											.getNama())
																			+ (myAlurPersetujuanSuratKeluarStatus
																					.getWaktuDitolak() == null
																							? ""
																							: " pada waktu "
																									+ Common.dateFormat3.get()
																											.format(myAlurPersetujuanSuratKeluarStatus
																													.getWaktuDitolak()))
																			+ "</font>")
																	: "<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Menunggu Persetujuan"
																			+ (myAlurPersetujuanSuratKeluarStatus
																					.getPejabat() == null
																							? ""
																							: " " + myAlurPersetujuanSuratKeluarStatus
																									.getPejabat()
																									.getNama())
																			+ "</font>")
													+ "</li>";
										}
									}

									html = SuratKeluarAction.infoDisposisiBagan(suratKeluar);

									new ais.ui.util.MyHtml(html).setParent(vbox);

									html = "";
									List<String> suratKeluarValues = session.createCriteria(OpsiSuratKeluarValue.class)
											.setProjection(Projections.groupProperty("nama"))
											.add(Restrictions.eq("suratKeluar", suratKeluar)).list();
									for (String opsiSuratKeluarValue : suratKeluarValues) {
										html += "<li>" + opsiSuratKeluarValue + "</li>";
									}

									new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">"
											+ Common.getBahasaConfig("Opsi") + ":<ul>" + html + "</ul></font>")
											.setParent(vbox);

//									final AlurPersetujuanSuratKeluarStatus disposisiTerakhir = (AlurPersetujuanSuratKeluarStatus) session
//											.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
//											.add(Restrictions.isNotNull("kodeUnik"))
//											.add(Restrictions.isNotNull("suratKeluar"))
//											.add(Restrictions.eq("suratKeluar", suratKeluar)).addOrder(Order.desc("id"))
//											.setMaxResults(1).uniqueResult();

									Hbox hbox = new Hbox();
									hbox.setParent(vbox1);

									LampiranLain lainMahasiswa = LampiranLain.ambil(
											suratKeluar.getKlasifikasiSuratKeluar().getId(),
											LampiranLain.FILE_JRXML_LAYOUT_SURAT);
									if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
										MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
										button.setTooltiptext("Cetak Data");
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												suratKeluar.cetak(tbmuser);
											}

										});
										button.setParent(hbox);
									}

									Toolbarbutton button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
									button.setTooltiptext("Ubah Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											SuratKeluarAction.onAddExternal(getThis(), suratKeluar);
										}

									});
									button.setParent(hbox);

									Session sessions = StreamingHibernateUtil.getInstance().currentSession();
									List<Object[]> fotoGambarSuratKeluars = suratKeluar == null
											|| suratKeluar.getId() == null
													? new ArrayList<Object[]>()
													: sessions.createCriteria(FotoGambarSuratKeluar.class)
															.setProjection(Projections.projectionList()
																	.add(Projections.property("id"))
																	.add(Projections.property("nama")))
															.add(Restrictions.eq("suratKeluar", suratKeluar.getId()))
															.addOrder(Order.desc("id")).list();

									for (Object[] fotoGambarSuratKeluar : fotoGambarSuratKeluars) {
										try {
											final Long id = (Long) fotoGambarSuratKeluar[0];
											String nama = (String) fotoGambarSuratKeluar[1];

											button = new ais.ui.util.MyToolbarbuttonConfig(nama,
													"/img/svg/download.svg");
											button.setTooltiptext("Download " + nama);
											button.addEventListener("onClick", new EventListener() {
												@Override
												public void onEvent(Event event) throws Exception {

													Session sessions = StreamingHibernateUtil.getInstance()
															.currentSession();

													FotoGambarSuratKeluar fotoGambarSuratKeluar = (FotoGambarSuratKeluar) sessions
															.createCriteria(FotoGambarSuratKeluar.class)
															.add(Restrictions.idEq(id)).uniqueResult();

													if (fotoGambarSuratKeluar.getGdrive() != null
															&& !fotoGambarSuratKeluar.getGdrive().isEmpty()) {
														ExecutionsCtrl.getCurrent().sendRedirect(
																fotoGambarSuratKeluar.downloadGDriveUrl(), "_blank");
													} else if (fotoGambarSuratKeluar != null) {

														Common.display(fotoGambarSuratKeluar);

													}

													sessions.disconnect();
													sessions.close();
													StreamingHibernateUtil.getInstance().closeSession();

												}

											});
											button.setParent(hbox);
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:748");
											// TODO: handle exception
										}
									}
									sessions.disconnect();
									sessions.close();
									StreamingHibernateUtil.getInstance().closeSession();

								}
								suratKeluars = null;
							}
						};

						Common.initPaging5(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						searchsampai.addEventListener("onChange", dataSearchDefault);
						searchmulai.addEventListener("onChange", dataSearchDefault);
						cari.addEventListener("onOK", dataSearchDefault);
						ambilDataSatuanKerjaBanbox.setEventListener(dataSearchDefault);
						refresh.addEventListener("onClick", dataSearchDefault);
					}
				};

				pengajuanBaruEventListener.onEvent(null);
			}

			private void menungguDisposisi() throws Exception {

				if (mulai == null) {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
					mulai = calendar.getTime();
				}

				// V13: Abaikan filter global dashboard.
				// memiliki form pencarian sendiri, sehingga mulai/sampai/keyword/satker
				// harus berasal dari komponen pencarian di bagian ini.
				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(getCurrentDashboardPortalParent());
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle("Persetujuan Surat Keluar");
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

						if (debug) { System.out.println("left -> " + left + ", top -> " + top); }
					}
				});
				panel.setStyle(legacyPanelStyleV7());
				final Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				EventListener pengajuanBaruEventListener = new EventListener() {

					public EventListener getThis() {
						return this;
					}

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);
						appendLegacyPanelIntroV20(panelchildren, "Persetujuan Surat Keluar");

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setStyle(toolbarStyleV7());
						toolbar.setParent(rowUtamapalingAwal);

						final Textbox cari = new Textbox();
						final MyDatebox searchmulai = new MyDatebox(mulai);
						final MyDatebox searchsampai = new MyDatebox(sampai);

						final MyCheckboxConfig blmDisetujui = new MyCheckboxConfig("Blm Disetujui");
						blmDisetujui.setChecked(blm);

						final MyCheckboxConfig untukSaya = new MyCheckboxConfig("Untuk Saya");
						untukSaya.setTooltiptext("Tampilkan hanya surat/alur disposisi yang ditujukan untuk Anda");
						// V17B: untuk user yang boleh melihat semua surat, default tetap difokuskan ke surat untuk dirinya sendiri.
						untukSaya.setChecked(bolehTampilkanFilterUntukSayaV17());

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setValue("");
						cari.setParent(toolbar);

						new MyLabelAgakKecil("Tgl:").setParent(toolbar);

						searchmulai.setCols(4);
						searchsampai.setCols(4);

						searchmulai.setReadonly(true);
						searchsampai.setReadonly(true);

						searchmulai.setParent(toolbar);
						searchsampai.setParent(toolbar);
						blmDisetujui.setParent(toolbar);
						if (bolehTampilkanFilterUntukSayaV17()) {
							untukSaya.setParent(toolbar);
						}

						MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
						refresh.setTooltiptext("Refresh");
						refresh.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {
								String c = cari.getValue().trim();

								List<Pejabat> pejabats = Common.getCurrentPejabat(true);
								ArrayList<JenisJabatan> jenisJabatans = new ArrayList<JenisJabatan>();

								for (Pejabat pejabat : pejabats) {
									jenisJabatans.add(pejabat.getJenisJabatan());
								}

								Session session = HibernateUtil.currentSession();

								Criteria criteria = session.createCriteria(AlurPersetujuanSuratKeluarStatus.class)

										.add(blmDisetujui == null ? Restrictions.sqlRestriction("true") : blmDisetujui.isChecked() ? pendingCriterion()
											: Restrictions.sqlRestriction("true"))

										.add(Restrictions.isNotNull("kodeUnik"))

										.createAlias("suratKeluar", "suratKeluar")
										.createAlias("suratKeluar.klasifikasiSuratKeluar", "aksesKlasifikasiSuratKeluar", Criteria.LEFT_JOIN)

										.add(searchmulai.getValue() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.ge("suratKeluar.tanggal", searchmulai.getValue()))
										.add(searchsampai.getValue() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.le("suratKeluar.tanggal", searchsampai.getValue()))
										.createAlias("pejabat", "pejabat", Criteria.LEFT_JOIN)

										// V16: visibility surat ATAU akses pejabat.
										// Jika dibuat AND, surat yang belum masuk disposisi pejabat tidak tampil.
										.add(createSuratKeluarAccessDenganUntukSayaV17(
												untukSaya.isChecked(),
												"suratKeluar.",
												"aksesKlasifikasiSuratKeluar."))

										.add(createSuratKeluarKeywordCriterion("suratKeluar.", c)

										);

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

							@Override
							public void onEvent(Event event) {

								tampilkanLoadingDashboardSuratV19(rowUtamaData, "Mengambil Data Persetujuan Surat", "Memuat alur persetujuan keluar dan timeline disposisi...", 35);

								// FIX: hitung distinct SuratKeluar (bukan baris AlurPersetujuanSuratKeluarStatus)
								// supaya paging akurat — 1 surat dgn 12 step alur sebelumnya dihitung 12 halaman
								Object _cntSK = ((Criteria) dataCriteria.initCriteria(false))
										.setProjection(Projections.countDistinct("suratKeluar.id")).uniqueResult();
								int _totalSK = _cntSK == null ? 0 : ((Number) _cntSK).intValue();
								paging.setPageSize(5);
								paging.setMold("os");
								paging.setDetailed(!Common.isMobile());
								paging.setTotalSize(_totalSK);
								paging.setVisible(_totalSK > 5);
								int _pageSK = paging == null ? 0 : paging.getActivePage();
								if (_pageSK * 5 >= _totalSK) { _pageSK = 0; if (paging != null) paging.setActivePage(0); }

								List<AlurPersetujuanSuratKeluarStatus> suratKeluars = new ArrayList<AlurPersetujuanSuratKeluarStatus>();
								if (_totalSK > 0) {
									List<Long> _idsSK = (List<Long>) ((Criteria) dataCriteria.initCriteria(false))
											.setProjection(Projections.distinct(Projections.property("suratKeluar.id")))
											.addOrder(Order.desc("suratKeluar.id"))
											.setFirstResult(5 * _pageSK).setMaxResults(5).list();
									if (_idsSK != null && !_idsSK.isEmpty()) {
										suratKeluars = (List<AlurPersetujuanSuratKeluarStatus>) ((Criteria) dataCriteria
												.initCriteria(true))
												.add(Restrictions.in("suratKeluar.id", _idsSK))
												.list();
										suratKeluars = groupAlurKeluarByParentV14(suratKeluars);
									}
								}

								Common.clear(rowUtamaData);

								MyGrid grid = new MyGrid();
								grid.setParent(rowUtamaData);
								grid.setSclass("fgrid");
								grid.setStyle("min-height:100px; border:0; background:transparent; border-radius:14px; overflow:hidden;");
								grid.setMold("paging");
								grid.setPageSize(100);
								grid.getPagingChild().setMold("os");

								Rows rows = new Rows();
								rows.setParent(grid);

								for (AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatusData : suratKeluars) {
										final AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus = pilihAlurKeluarUntukTindakLanjutV20(alurPersetujuanSuratKeluarStatusData);
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									String oleh = "";

									if (alurPersetujuanSuratKeluarStatus.getSuratKeluar().getMahasiswa() != null) {
										oleh = alurPersetujuanSuratKeluarStatus.getSuratKeluar().getMahasiswa()
												.getNama();
									} else if (alurPersetujuanSuratKeluarStatus.getSuratKeluar().getSiswa() != null) {
										oleh = alurPersetujuanSuratKeluarStatus.getSuratKeluar().getSiswa().getNama();
									} else if (alurPersetujuanSuratKeluarStatus.getSuratKeluar()
											.getKonseptor() != null) {
										oleh = alurPersetujuanSuratKeluarStatus.getSuratKeluar().getKonseptor()
												.getUserNama();
									}

									Vbox vbox1 = new Vbox();
									vbox1.setParent(rowUtamaLagi);

									Vbox a;
									(a = new Vbox()).setParent(vbox1);
									Vbox vbox = new Vbox();
									a.appendChild(vbox);
									try {
										// NPE fix: disposisi_sop adalah FK nullable pada SuratKeluar (banyak surat
										// belum terhubung ke alur SOP), jadi getDisposisiSop() sah bernilai null.
										// Guard supaya label nama+oleh tetap tampil, tanggal fallback ke waktu surat.
										ais.database.model.sop.DisposisiSop _disposisiSopSK = alurPersetujuanSuratKeluarStatus
												.getSuratKeluar().getDisposisiSop();
										java.util.Date _waktuPengajuanSK = _disposisiSopSK != null
												&& _disposisiSopSK.getWaktu() != null ? _disposisiSopSK.getWaktu()
														: alurPersetujuanSuratKeluarStatus.getSuratKeluar().getWaktu();
										vbox.appendChild(new MyLabelBoldAja(
												alurPersetujuanSuratKeluarStatus.getSuratKeluar().getNama()
														+ " (pengajuan "
														+ (_waktuPengajuanSK == null ? "-"
																: Common.dateFormat.get().format(_waktuPengajuanSK))
														+ " " + oleh + ")"));

									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:1008");
//								e.printStackTrace();
									}

									try {

										vbox.appendChild(new MyLabelBoldAja(
												alurPersetujuanSuratKeluarStatus.getSuratKeluar().getKode()));
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/DasboardSurat.java:1017");
									}
									try {
										vbox.appendChild(new MyLabelAgakKecil(
												alurPersetujuanSuratKeluarStatus.getSuratKeluar().getPerihal()));
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/DasboardSurat.java:1023");
									}

									try {
										vbox.appendChild(new MyLabelAgakKecil("Waktu Pengajuan : " + Common.dateFormat61.get()
												.format(alurPersetujuanSuratKeluarStatus.getSuratKeluar().getWaktu())));
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/DasboardSurat.java:1030");
									}

									if (alurPersetujuanSuratKeluarStatus.getTelahDirevisi()) {
										try {
											vbox.appendChild(new MyLabelAgakKecil("Telah Direvisi, catatan : "
													+ alurPersetujuanSuratKeluarStatus.getCatatanRevisi()));
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/DasboardSurat.java:1038");
										}
									}

									Session session = HibernateUtil.currentSession();
									AlurPersetujuanSuratKeluarStatus disposisiTerakhir = (AlurPersetujuanSuratKeluarStatus) session
											.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
											.add(Restrictions.isNotNull("kodeUnik"))
											.add(Restrictions.isNotNull("suratKeluar"))
											.add(Restrictions.eq("suratKeluar",
													alurPersetujuanSuratKeluarStatus.getSuratKeluar()))
											.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
									if (disposisiTerakhir != null) {

										if (disposisiTerakhir.getJenisJabatan() != null
												&& !disposisiTerakhir.getDisetujui()
												&& !disposisiTerakhir.getDitolak()) {

											vbox.appendChild(new MyLabelAgakKecilBoldMerah("Menunggu persetujuan : "
													+ disposisiTerakhir.getJenisJabatan().getNama()
													+ (disposisiTerakhir.getPejabat() != null
															&& !disposisiTerakhir.getPejabat().getNama().isEmpty()
																	? " (" + disposisiTerakhir.getPejabat().getNama()
																			+ ")"
																	: "")));
										} else if (disposisiTerakhir.getDitolak()) {
											vbox.appendChild(new MyLabelAgakKecilBoldMerah("Ditolak oleh : "
													+ (disposisiTerakhir.getJenisJabatan() == null ? ""
															: disposisiTerakhir.getJenisJabatan().getNama())
													+ (disposisiTerakhir.getPejabat() != null
															&& !disposisiTerakhir.getPejabat().getNama().isEmpty()
																	? " (" + disposisiTerakhir.getPejabat().getNama()
																			+ ")"
																	: "")));
										} else {
											vbox.appendChild(new MyLabelAgakKecilBoldHijau("Disetujui oleh : "
													+ (disposisiTerakhir.getJenisJabatan() == null ? ""
															: disposisiTerakhir.getJenisJabatan().getNama())
													+ (disposisiTerakhir.getPejabat() != null
															&& !disposisiTerakhir.getPejabat().getNama().isEmpty()
																	? " (" + disposisiTerakhir.getPejabat().getNama()
																			+ ")"
																	: "")));
										}
									}

									String html = "";
									AlurPersetujuanSuratKeluarStatus myAlurPersetujuanSuratKeluarStatus = alurPersetujuanSuratKeluarStatus;
									if (myAlurPersetujuanSuratKeluarStatus.getJenisJabatan() == null) {
										html += "" + myAlurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar()
												+ " : "
												+ (myAlurPersetujuanSuratKeluarStatus.getDisetujui()
														? ("<font style=\"font-size: x-small;color:blue;font-weight: bolder;\">Sudah ditindak-lanjuti "
																+ (myAlurPersetujuanSuratKeluarStatus
																		.getPejabat() == null
																		|| myAlurPersetujuanSuratKeluarStatus
																				.getPejabat().getPegawai() == null
																						? (myAlurPersetujuanSuratKeluarStatus
																								.getPejabat()
																								.getDosen() == null
																										? ""
																										: " " + myAlurPersetujuanSuratKeluarStatus
																												.getPejabat()
																												.getDosen()
																												.getNama())
																						: " " + myAlurPersetujuanSuratKeluarStatus
																								.getPejabat()
																								.getPegawai().getNama())
																+ (myAlurPersetujuanSuratKeluarStatus
																		.getWaktuPersetujuan() == null
																				? ""
																				: " pada waktu " + Common.dateFormat3.get()
																						.format(myAlurPersetujuanSuratKeluarStatus
																								.getWaktuPersetujuan()))
																+ "</font>")
														:

														myAlurPersetujuanSuratKeluarStatus.getDitolak()
																? ("<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Ditolak "
																		+ (myAlurPersetujuanSuratKeluarStatus
																				.getPejabat() == null
																				|| myAlurPersetujuanSuratKeluarStatus
																						.getPejabat()
																						.getPegawai() == null
																								? (myAlurPersetujuanSuratKeluarStatus
																										.getPejabat()
																										.getDosen() == null
																												? ""
																												: " " + myAlurPersetujuanSuratKeluarStatus
																														.getPejabat()
																														.getDosen()
																														.getNama())
																								: " " + myAlurPersetujuanSuratKeluarStatus
																										.getPejabat()
																										.getPegawai()
																										.getNama())
																		+ (myAlurPersetujuanSuratKeluarStatus
																				.getWaktuDitolak() == null
																						? ""
																						: " pada waktu "
																								+ Common.dateFormat3.get()
																										.format(myAlurPersetujuanSuratKeluarStatus
																												.getWaktuDitolak()))
																		+ "</font>")
																:

																"<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Menunggu Persetujuan"
																		+ (myAlurPersetujuanSuratKeluarStatus
																				.getPejabat() == null
																						? ""
																						: " " + myAlurPersetujuanSuratKeluarStatus
																								.getPejabat().getNama())
																		+ "</font>")
												+ "";
									} else {
										html += "" + myAlurPersetujuanSuratKeluarStatus.getJenisJabatan().getNama()
												+ " : "
												+ (myAlurPersetujuanSuratKeluarStatus.getDisetujui()
														? ("<font style=\"font-size: x-small;color:blue;font-weight: bolder;\">Sudah ditindak-lanjuti "
																+ (myAlurPersetujuanSuratKeluarStatus
																		.getPejabat() == null
																		|| myAlurPersetujuanSuratKeluarStatus
																				.getPejabat().getPegawai() == null
																						? (myAlurPersetujuanSuratKeluarStatus
																								.getPejabat()
																								.getDosen() == null
																										? ""
																										: " " + myAlurPersetujuanSuratKeluarStatus
																												.getPejabat()
																												.getDosen()
																												.getNama())
																						: " " + myAlurPersetujuanSuratKeluarStatus
																								.getPejabat()
																								.getPegawai().getNama())
																+ (myAlurPersetujuanSuratKeluarStatus
																		.getWaktuPersetujuan() == null
																				? ""
																				: " pada waktu " + Common.dateFormat3.get()
																						.format(myAlurPersetujuanSuratKeluarStatus
																								.getWaktuPersetujuan()))
																+ "</font>")
														:

														myAlurPersetujuanSuratKeluarStatus.getDitolak()
																? ("<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Ditolak "
																		+ (myAlurPersetujuanSuratKeluarStatus
																				.getPejabat() == null
																				|| myAlurPersetujuanSuratKeluarStatus
																						.getPejabat()
																						.getPegawai() == null
																								? (myAlurPersetujuanSuratKeluarStatus
																										.getPejabat()
																										.getDosen() == null
																												? ""
																												: " " + myAlurPersetujuanSuratKeluarStatus
																														.getPejabat()
																														.getDosen()
																														.getNama())
																								: " " + myAlurPersetujuanSuratKeluarStatus
																										.getPejabat()
																										.getPegawai()
																										.getNama())
																		+ (myAlurPersetujuanSuratKeluarStatus
																				.getWaktuDitolak() == null
																						? ""
																						: " pada waktu "
																								+ Common.dateFormat3.get()
																										.format(myAlurPersetujuanSuratKeluarStatus
																												.getWaktuDitolak()))
																		+ "</font>")
																:

																"<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Menunggu Persetujuan"
																		+ (myAlurPersetujuanSuratKeluarStatus
																				.getPejabat() == null
																						? ""
																						: " " + myAlurPersetujuanSuratKeluarStatus
																								.getPejabat().getNama())
																		+ "</font>")
												+ "";
									}

									html = SuratKeluarAction.infoDisposisiBagan(
											alurPersetujuanSuratKeluarStatus.getSuratKeluar());
									vbox.appendChild(new Html(html));

									vbox.appendChild(new MyLabelKecil(
											"Catatan : " + alurPersetujuanSuratKeluarStatus.getKeterangan()));

									html = "";
									List<String> suratKeluarValues = session.createCriteria(OpsiSuratKeluarValue.class)
											.setProjection(Projections.groupProperty("nama"))
											.add(Restrictions.eq("suratKeluar",
													alurPersetujuanSuratKeluarStatus.getSuratKeluar()))
											.list();
									for (String opsiSuratKeluarValue : suratKeluarValues) {
										html += "<li>" + opsiSuratKeluarValue + "</li>";
									}

									new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">"
											+ Common.getBahasaConfig("Opsi") + ":<ul>" + html + "</ul></font>")
											.setParent(vbox);

									Hbox hbox = new Hbox();
									hbox.setParent(vbox1);

									Toolbarbutton button = new MyToolbarbuttonConfig("Catatan Disposisi", "/img/print.png");
									button.setOrient("vertical");
									button.setTooltiptext("Lihat catatan disposisi (tabel)");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											CatatanDisposisiPopupHelper.showKeluar(alurPersetujuanSuratKeluarStatus,
													tbmuser, (org.zkoss.zk.ui.Component) event.getTarget());
										}

									});
									button.setParent(hbox);
										boolean boleh = bolehAksesAlurBerdasarkanLoginV20(alurPersetujuanSuratKeluarStatus.getPejabat(),
												alurPersetujuanSuratKeluarStatus.getJenisJabatan());

										if (isPendingAlurKeluarV20(alurPersetujuanSuratKeluarStatus)) {

										if (boleh) {
											button = new MyToolbarbuttonConfig("Tindak Lanjuti", "/img/Check-icon.png");
											button.setOrient("vertical");
											button.setTooltiptext("Ubah Data");
											button.addEventListener("onClick", new EventListener() {
												@Override
												public void onEvent(Event event) throws Exception {

													AlurPersetujuanSuratKeluarStatusAction.onAddExternal(getThis(),
															alurPersetujuanSuratKeluarStatus);
												}

											});
											button.setParent(hbox);
										}

									} else {
										if (boleh) {
											button = new MyToolbarbuttonConfig("Ubah", "/img/Check-icon.png");
											button.setOrient("vertical");
											button.setTooltiptext("Ubah Data");
											button.addEventListener("onClick", new EventListener() {
												@Override
												public void onEvent(Event event) throws Exception {

													AlurPersetujuanSuratKeluarStatusAction.onAddExternal(getThis(),
															alurPersetujuanSuratKeluarStatus);
												}

											});
											button.setParent(hbox);
										}
									}

									button = new MyToolbarbuttonConfig("Lihat", "/img/eye-icon.png");
									button.setOrient("vertical");
									button.setTooltiptext("Lihat Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											AlurPersetujuanSuratKeluarStatusAction
													.onPreview(alurPersetujuanSuratKeluarStatus);
										}
									});
									button.setParent(hbox);

									try {
										SuratKeluar suratKeluar = alurPersetujuanSuratKeluarStatus.getSuratKeluar();

										Session sessions = StreamingHibernateUtil.getInstance().currentSession();
										List<Object[]> fotoGambarSuratKeluars = suratKeluar == null
												|| suratKeluar.getId() == null
														? new ArrayList<Object[]>()
														: sessions.createCriteria(FotoGambarSuratKeluar.class)
																.setProjection(Projections.projectionList()
																		.add(Projections.property("id"))
																		.add(Projections.property("nama")))
																.add(Restrictions.eq("suratKeluar",
																		suratKeluar.getId()))
																.addOrder(Order.desc("id")).list();

										for (Object[] fotoGambarSuratKeluar : fotoGambarSuratKeluars) {
											try {
												final Long id = (Long) fotoGambarSuratKeluar[0];
												String nama = (String) fotoGambarSuratKeluar[1];

												button = new MyToolbarbuttonConfig(nama, "/img/svg/download.svg");
												button.setOrient("vertical");
												button.setTooltiptext("Download " + nama);
												button.addEventListener("onClick", new EventListener() {
													@Override
													public void onEvent(Event event) throws Exception {

														Session sessions = null;
														try {
															sessions = StreamingHibernateUtil.getInstance()
																	.currentSession();

															FotoGambarSuratKeluar fotoGambarSuratKeluar = (FotoGambarSuratKeluar) sessions
																	.createCriteria(FotoGambarSuratKeluar.class)
																	.add(Restrictions.idEq(id)).uniqueResult();

															if (fotoGambarSuratKeluar == null) {
																return;
															}
															if (fotoGambarSuratKeluar.getGdrive() != null
																	&& !fotoGambarSuratKeluar.getGdrive().isEmpty()) {
																ExecutionsCtrl.getCurrent().sendRedirect(
																		fotoGambarSuratKeluar.downloadGDriveUrl(),
																		"_blank");
															} else {
																Common.display(fotoGambarSuratKeluar);
															}
														} finally {
															Common.closeOpenedSession(sessions);
															try {
																StreamingHibernateUtil.getInstance().closeSession();
															} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:1358");
															}
														}

													}

												});
												button.setParent(hbox);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:1361");
												// TODO: handle exception
											}
										}
										sessions.disconnect();
										sessions.close();
										StreamingHibernateUtil.getInstance().closeSession();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/DasboardSurat.java:1369");
									}

								}
								suratKeluars = null;
							}
						};

						Common.initPaging5(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						searchsampai.addEventListener("onChange", dataSearchDefault);
						searchmulai.addEventListener("onChange", dataSearchDefault);
						cari.addEventListener("onOK", dataSearchDefault);

						refresh.addEventListener("onClick", dataSearchDefault);
						blmDisetujui.addEventListener("onClick", dataSearchDefault);
						untukSaya.addEventListener("onClick", dataSearchDefault);
					}
				};

				pengajuanBaruEventListener.onEvent(null);
			}

			private void menungguDisposisiMasuk() throws Exception {

				if (mulai == null) {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
					mulai = calendar.getTime();
				}

				// V13: Abaikan filter global dashboard.
				// memiliki form pencarian sendiri, sehingga mulai/sampai/keyword/satker
				// harus berasal dari komponen pencarian di bagian ini.
				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(getCurrentDashboardPortalParent());
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle("Disposisi Surat Masuk");
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

						if (debug) { System.out.println("left -> " + left + ", top -> " + top); }
					}
				});
				panel.setStyle(legacyPanelStyleV7());
				final Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				EventListener pengajuanBaruEventListener = new EventListener() {

					public EventListener getThis() {
						return this;
					}

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);
						appendLegacyPanelIntroV20(panelchildren, "Disposisi Surat Masuk");

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setStyle(toolbarStyleV7());
						toolbar.setParent(rowUtamapalingAwal);

						final MyDatebox searchmulai = new MyDatebox(mulai);
						final MyDatebox searchsampai = new MyDatebox(sampai);
						final MyCheckboxConfig blmDisetujui = new MyCheckboxConfig("Blm Disetujui");
						blmDisetujui.setChecked(blm);

						final MyCheckboxConfig untukSaya = new MyCheckboxConfig("Untuk Saya");
						untukSaya.setTooltiptext("Tampilkan hanya surat/alur disposisi yang ditujukan untuk Anda");
						// V17B: untuk user yang boleh melihat semua surat, default tetap difokuskan ke surat untuk dirinya sendiri.
						untukSaya.setChecked(bolehTampilkanFilterUntukSayaV17());

						final Textbox cari = new Textbox();

						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(5);
						cari.setValue("");
						cari.setParent(toolbar);

						new MyLabelAgakKecil("Tgl:").setParent(toolbar);

						searchmulai.setCols(4);
						searchsampai.setCols(4);

						searchmulai.setReadonly(true);
						searchsampai.setReadonly(true);

						searchmulai.setParent(toolbar);

						searchsampai.setParent(toolbar);

						blmDisetujui.setParent(toolbar);
						if (bolehTampilkanFilterUntukSayaV17()) {
							untukSaya.setParent(toolbar);
						}

						MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
						refresh.setTooltiptext("Refresh");
						refresh.setParent(toolbar);

						final DataCriteria dataCriteria = new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {
								String c = cari.getValue().trim();

								List<Pejabat> pejabats = Common.getCurrentPejabat(true);
								ArrayList<JenisJabatan> jenisJabatans = new ArrayList<JenisJabatan>();

								for (Pejabat pejabat : pejabats) {
									jenisJabatans.add(pejabat.getJenisJabatan());
								}

								Session session = HibernateUtil.currentSession();

								Criteria criteria = session.createCriteria(AlurPersetujuanSuratMasukStatus.class)

										.add(blmDisetujui == null ? Restrictions.sqlRestriction("true") : blmDisetujui.isChecked() ? pendingCriterion()
										: Restrictions.sqlRestriction("true"))

										.add(Restrictions.isNotNull("kodeUnik"))

										.createAlias("suratMasuk", "suratMasuk")
										.createAlias("suratMasuk.klasifikasiSuratMasuk", "aksesKlasifikasiSuratMasuk", Criteria.LEFT_JOIN)

										.add(searchmulai.getValue() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.ge("suratMasuk.tanggal", searchmulai.getValue()))
										.add(searchsampai.getValue() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.le("suratMasuk.tanggal", searchsampai.getValue()))

										.createAlias("pejabat", "pejabat", Criteria.LEFT_JOIN)

										// V16: visibility surat ATAU akses pejabat.
										// Jika dibuat AND, surat yang belum masuk disposisi pejabat tidak tampil.
										.add(createSuratMasukAccessDenganUntukSayaV17(
												untukSaya.isChecked(),
												"suratMasuk.",
												"aksesKlasifikasiSuratMasuk.",
												""))

										.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.or(
														Restrictions.ilike("suratMasuk.noSurat", c, MatchMode.ANYWHERE),
														Restrictions.or(
																Restrictions.ilike("suratMasuk.perihal", c,
																		MatchMode.ANYWHERE),
																Restrictions.or(
																		Restrictions.ilike("suratMasuk.ringkasan", c,
																				MatchMode.ANYWHERE),
																		Restrictions.or(
																				Restrictions.ilike(
																						"suratMasuk.keterangan", c,
																						MatchMode.ANYWHERE),
																				Restrictions.or(
																						Restrictions.ilike(
																								"suratMasuk.kode", c,
																								MatchMode.ANYWHERE),
																						Restrictions.ilike(
																								"suratMasuk.nama", c,
																								MatchMode.ANYWHERE))))))

										);

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

							@Override
							public void onEvent(Event event) {

								tampilkanLoadingDashboardSuratV19(rowUtamaData, "Mengambil Data Disposisi Surat Masuk", "Memuat disposisi masuk dan riwayat alur yang sudah dilewati...", 35);

								// FIX: hitung distinct SuratMasuk (bukan baris AlurPersetujuanSuratMasukStatus)
								Object _cntSM = ((Criteria) dataCriteria.initCriteria(false))
										.setProjection(Projections.countDistinct("suratMasuk.id")).uniqueResult();
								int _totalSM = _cntSM == null ? 0 : ((Number) _cntSM).intValue();
								paging.setPageSize(5);
								paging.setMold("os");
								paging.setDetailed(!Common.isMobile());
								paging.setTotalSize(_totalSM);
								paging.setVisible(_totalSM > 5);
								int _pageSM = paging == null ? 0 : paging.getActivePage();
								if (_pageSM * 5 >= _totalSM) { _pageSM = 0; if (paging != null) paging.setActivePage(0); }

								List<AlurPersetujuanSuratMasukStatus> suratMasuks = new ArrayList<AlurPersetujuanSuratMasukStatus>();
								if (_totalSM > 0) {
									List<Long> _idsSM = (List<Long>) ((Criteria) dataCriteria.initCriteria(false))
											.setProjection(Projections.distinct(Projections.property("suratMasuk.id")))
											.addOrder(Order.desc("suratMasuk.id"))
											.setFirstResult(5 * _pageSM).setMaxResults(5).list();
									if (_idsSM != null && !_idsSM.isEmpty()) {
										suratMasuks = (List<AlurPersetujuanSuratMasukStatus>) ((Criteria) dataCriteria
												.initCriteria(true))
												.add(Restrictions.in("suratMasuk.id", _idsSM))
												.list();
										suratMasuks = groupAlurMasukByParentV14(suratMasuks);
									}
								}

								Common.clear(rowUtamaData);

								MyGrid grid = new MyGrid();
								grid.setParent(rowUtamaData);
								grid.setSclass("fgrid");
								grid.setStyle("min-height:100px; border:0; background:transparent; border-radius:14px; overflow:hidden;");
								grid.setMold("paging");
								grid.setPageSize(10);
								grid.getPagingChild().setMold("os");

								Rows rows = new Rows();
								rows.setParent(grid);

								for (AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatusData : suratMasuks) {
										final AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus = pilihAlurMasukUntukTindakLanjutV20(alurPersetujuanSuratMasukStatusData);
									Row rowUtamaLagi = new Row();
									rowUtamaLagi.setParent(rows);

									String oleh = alurPersetujuanSuratMasukStatus.getSuratMasuk().getAsal();

									Vbox vbox1 = new Vbox();
									vbox1.setParent(rowUtamaLagi);

									Vbox a;
									(a = new Vbox()).setParent(vbox1);
									Vbox vbox = new Vbox();
									a.appendChild(vbox);
									try {
										// NPE fix: disposisi_sop adalah FK nullable pada SuratMasuk (banyak surat
										// belum terhubung ke alur SOP), jadi getDisposisiSop() sah bernilai null.
										// Guard supaya label nama+oleh tetap tampil, tanggal fallback ke waktu surat.
										ais.database.model.sop.DisposisiSop _disposisiSopSM = alurPersetujuanSuratMasukStatus
												.getSuratMasuk().getDisposisiSop();
										java.util.Date _waktuPengajuanSM = _disposisiSopSM != null
												&& _disposisiSopSM.getWaktu() != null ? _disposisiSopSM.getWaktu()
														: alurPersetujuanSuratMasukStatus.getSuratMasuk().getWaktu();
										vbox.appendChild(new MyLabelBoldAja(
												alurPersetujuanSuratMasukStatus.getSuratMasuk().getNama()
														+ " (pengajuan "
														+ (_waktuPengajuanSM == null ? "-"
																: Common.dateFormat.get().format(_waktuPengajuanSM))
														+ " " + oleh + ")"));

									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:1637");
										// TODO: handle exception
									}

									try {

										vbox.appendChild(new MyLabelBoldAja(
												alurPersetujuanSuratMasukStatus.getSuratMasuk().getKode()));
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:1645");
										// TODO: handle exception
									}

									try {
										vbox.appendChild(new MyLabelAgakKecil(
												alurPersetujuanSuratMasukStatus.getSuratMasuk().getPerihal()));
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/DasboardSurat.java:1653");
									}

									try {
										vbox.appendChild(new MyLabelAgakKecil("Waktu Pengajuan : " + Common.dateFormat61.get()
												.format(alurPersetujuanSuratMasukStatus.getSuratMasuk().getWaktu())));
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/DasboardSurat.java:1660");
									}

									if (alurPersetujuanSuratMasukStatus.getTelahDirevisi()) {
										try {
											vbox.appendChild(new MyLabelAgakKecil("Telah Direvisi, catatan : "
													+ alurPersetujuanSuratMasukStatus.getCatatanRevisi()));
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/DasboardSurat.java:1668");
										}
									}

									String html = "";
									List<AlurPersetujuanSuratMasukStatus> alurPersetujuanSuratMasukStatuses = HibernateUtil
											.currentSession().createCriteria(AlurPersetujuanSuratMasukStatus.class)
											.add(Restrictions.isNotNull("kodeUnik"))
											.add(Restrictions.eq("suratMasuk",
													alurPersetujuanSuratMasukStatus.getSuratMasuk()))
											.addOrder(Order.asc("id")).list();

									for (AlurPersetujuanSuratMasukStatus myAlurPersetujuanSuratMasukStatus : alurPersetujuanSuratMasukStatuses) {
										if (myAlurPersetujuanSuratMasukStatus.getJenisJabatan() == null) {
											html += "<li>"
													+ myAlurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk()
													+ " : "
													+ (myAlurPersetujuanSuratMasukStatus.getDisetujui()
															? ("<font style=\"font-size: x-small;color:blue;font-weight: bolder;\">Sudah ditindak-lanjuti "
																	+ (myAlurPersetujuanSuratMasukStatus
																			.getPejabat() == null
																			|| myAlurPersetujuanSuratMasukStatus
																					.getPejabat().getPegawai() == null
																							? (myAlurPersetujuanSuratMasukStatus
																									.getPejabat()
																									.getDosen() == null
																											? ""
																											: " " + myAlurPersetujuanSuratMasukStatus
																													.getPejabat()
																													.getDosen()
																													.getNama())
																							: " " + myAlurPersetujuanSuratMasukStatus
																									.getPejabat()
																									.getPegawai()
																									.getNama())
																	+ (myAlurPersetujuanSuratMasukStatus
																			.getWaktuPersetujuan() == null
																					? ""
																					: " pada waktu "
																							+ Common.dateFormat3.get().format(
																									myAlurPersetujuanSuratMasukStatus
																											.getWaktuPersetujuan()))
																	+ "</font>")
															:

															myAlurPersetujuanSuratMasukStatus.getDitolak()
																	? ("<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Ditolak "
																			+ (myAlurPersetujuanSuratMasukStatus
																					.getPejabat() == null
																					|| myAlurPersetujuanSuratMasukStatus
																							.getPejabat()
																							.getPegawai() == null
																									? (myAlurPersetujuanSuratMasukStatus
																											.getPejabat()
																											.getDosen() == null
																													? ""
																													: " " + myAlurPersetujuanSuratMasukStatus
																															.getPejabat()
																															.getDosen()
																															.getNama())
																									: " " + myAlurPersetujuanSuratMasukStatus
																											.getPejabat()
																											.getPegawai()
																											.getNama())
																			+ (myAlurPersetujuanSuratMasukStatus
																					.getTanggal_dirubah() == null
																							? ""
																							: " pada waktu "
																									+ Common.dateFormat3.get()
																											.format(myAlurPersetujuanSuratMasukStatus
																													.getTanggal_dirubah()))
																			+ "</font>")
																	:

																	"<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Menunggu Persetujuan"
																			+ (myAlurPersetujuanSuratMasukStatus
																					.getPejabat() == null
																							? ""
																							: " " + myAlurPersetujuanSuratMasukStatus
																									.getPejabat()
																									.getNama())
																			+ "</font>")
													+ "</li>";
										} else {
											html += "<li>"
													+ myAlurPersetujuanSuratMasukStatus.getJenisJabatan().getNama()
													+ " : "
													+ (myAlurPersetujuanSuratMasukStatus.getDisetujui()
															? ("<font style=\"font-size: x-small;color:blue;font-weight: bolder;\">Sudah ditindak-lanjuti "
																	+ (myAlurPersetujuanSuratMasukStatus
																			.getPejabat() == null
																			|| myAlurPersetujuanSuratMasukStatus
																					.getPejabat().getPegawai() == null
																							? (myAlurPersetujuanSuratMasukStatus
																									.getPejabat()
																									.getDosen() == null
																											? ""
																											: " " + myAlurPersetujuanSuratMasukStatus
																													.getPejabat()
																													.getDosen()
																													.getNama())
																							: " " + myAlurPersetujuanSuratMasukStatus
																									.getPejabat()
																									.getPegawai()
																									.getNama())
																	+ (myAlurPersetujuanSuratMasukStatus
																			.getWaktuPersetujuan() == null
																					? ""
																					: " pada waktu "
																							+ Common.dateFormat3.get().format(
																									myAlurPersetujuanSuratMasukStatus
																											.getWaktuPersetujuan()))
																	+ "</font>")
															:

															myAlurPersetujuanSuratMasukStatus.getDitolak()
																	? ("<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Ditolak "
																			+ (myAlurPersetujuanSuratMasukStatus
																					.getPejabat() == null
																					|| myAlurPersetujuanSuratMasukStatus
																							.getPejabat()
																							.getPegawai() == null
																									? (myAlurPersetujuanSuratMasukStatus
																											.getPejabat()
																											.getDosen() == null
																													? ""
																													: " " + myAlurPersetujuanSuratMasukStatus
																															.getPejabat()
																															.getDosen()
																															.getNama())
																									: " " + myAlurPersetujuanSuratMasukStatus
																											.getPejabat()
																											.getPegawai()
																											.getNama())
																			+ (myAlurPersetujuanSuratMasukStatus
																					.getTanggal_dirubah() == null
																							? ""
																							: " pada waktu "
																									+ Common.dateFormat3.get()
																											.format(myAlurPersetujuanSuratMasukStatus
																													.getTanggal_dirubah()))
																			+ "</font>")
																	:

																	"<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Menunggu Persetujuan"
																			+ (myAlurPersetujuanSuratMasukStatus
																					.getPejabat() == null
																							? ""
																							: " " + myAlurPersetujuanSuratMasukStatus
																									.getPejabat()
																									.getNama())
																			+ "</font>")
													+ "</li>";
										}
									}
									html = SuratMasukAction.infoDisposisiBagan(
											alurPersetujuanSuratMasukStatus.getSuratMasuk());
									vbox.appendChild(new Html(html));

									vbox.appendChild(new MyLabelKecil(
											"Catatan : " + alurPersetujuanSuratMasukStatus.getKeterangan()));

									html = "";
									Session session = HibernateUtil.currentSession();
									List<OpsiSuratMasukValue> suratKeluarValues = session
											.createCriteria(OpsiSuratMasukValue.class).add(Restrictions.eq("suratMasuk",
													alurPersetujuanSuratMasukStatus.getSuratMasuk()))
											.list();
									for (OpsiSuratMasukValue opsiSuratKeluarValue : suratKeluarValues) {
										html += "<li>" + opsiSuratKeluarValue.getNama() + "</li>";
									}

									new ais.ui.util.MyHtml(
											"<font style=\"font-size: x-small;\">Opsi:<ul>" + html + "</ul></font>")
											.setParent(vbox);

									Hbox hbox = new Hbox();
									hbox.setParent(vbox1);

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Catatan Disposisi",
											"/img/print.png");
									button.setOrient("vertical");
									button.setTooltiptext("Lihat catatan disposisi (tabel)");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											CatatanDisposisiPopupHelper.showMasuk(alurPersetujuanSuratMasukStatus,
													tbmuser, (org.zkoss.zk.ui.Component) event.getTarget());
										}

									});
									button.setParent(hbox);
										boolean boleh = bolehAksesAlurBerdasarkanLoginV20(alurPersetujuanSuratMasukStatus.getPejabat(),
												alurPersetujuanSuratMasukStatus.getJenisJabatan());

										if (isPendingAlurMasukV20(alurPersetujuanSuratMasukStatus)) {

										if (boleh) {
											button = new MyToolbarbuttonConfig("Tindak Lanjuti", "/img/Check-icon.png");
											button.setOrient("vertical");
											button.setTooltiptext("Ubah Data");
											button.addEventListener("onClick", new EventListener() {
												@Override
												public void onEvent(Event event) throws Exception {

													AlurPersetujuanSuratMasukStatusAction.onAddExternal(getThis(),
															alurPersetujuanSuratMasukStatus);
												}

											});
											button.setParent(hbox);
										}

									} else {
										if (boleh) {
											button = new MyToolbarbuttonConfig("Ubah", "/img/Check-icon.png");
											button.setOrient("vertical");
											button.setTooltiptext("Ubah Data");
											button.addEventListener("onClick", new EventListener() {
												@Override
												public void onEvent(Event event) throws Exception {

													AlurPersetujuanSuratMasukStatusAction.onAddExternal(getThis(),
															alurPersetujuanSuratMasukStatus);
												}

											});
											button.setParent(hbox);
										}
									}

									button = new MyToolbarbuttonConfig("Lihat", "/img/eye-icon.png");
									button.setOrient("vertical");
									button.setTooltiptext("Lihat Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											AlurPersetujuanSuratMasukStatusAction
													.onPreview(alurPersetujuanSuratMasukStatus);
										}
									});
									button.setParent(hbox);

									try {
										SuratMasuk suratMasuk = alurPersetujuanSuratMasukStatus.getSuratMasuk();

										Session sessions = StreamingHibernateUtil.getInstance().currentSession();
										List<Object[]> fotoGambarSuratMasuks = suratMasuk == null
												|| suratMasuk.getId() == null
														? new ArrayList<Object[]>()
														: sessions.createCriteria(FotoGambarSuratMasuk.class)
																.setProjection(Projections.projectionList()
																		.add(Projections.property("id"))
																		.add(Projections.property("nama")))
																.add(Restrictions.eq("suratMasuk", suratMasuk.getId()))
																.addOrder(Order.desc("id")).list();

										for (Object[] fotoGambarSuratMasuk : fotoGambarSuratMasuks) {
											try {
												final Long id = (Long) fotoGambarSuratMasuk[0];
												String nama = (String) fotoGambarSuratMasuk[1];

												button = new MyToolbarbuttonConfig(nama, "/img/svg/download.svg");
												button.setOrient("vertical");
												button.setTooltiptext("Download " + nama);
												button.addEventListener("onClick", new EventListener() {
													@Override
													public void onEvent(Event event) throws Exception {

														Session sessions = StreamingHibernateUtil.getInstance()
																.currentSession();

														FotoGambarSuratMasuk fotoGambarSuratMasuk = (FotoGambarSuratMasuk) sessions
																.createCriteria(FotoGambarSuratMasuk.class)
																.add(Restrictions.idEq(id)).uniqueResult();

														if (fotoGambarSuratMasuk.getGdrive() != null
																&& !fotoGambarSuratMasuk.getGdrive().isEmpty()) {
															ExecutionsCtrl.getCurrent().sendRedirect(
																	fotoGambarSuratMasuk.downloadGDriveUrl(), "_blank");
														} else if (fotoGambarSuratMasuk != null) {
															Common.display(fotoGambarSuratMasuk);
														}

														sessions.disconnect();
														sessions.close();
														StreamingHibernateUtil.getInstance().closeSession();

													}

												});
												button.setParent(hbox);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:1958");
												// TODO: handle exception
											}
										}
										sessions.disconnect();
										sessions.close();
										StreamingHibernateUtil.getInstance().closeSession();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/DasboardSurat.java:1966");
									}

								}
								suratMasuks = null;
							}
						};

						Common.initPaging5(paging, dataSearchDefault);
						dataSearchDefault.onEvent(arg0);

						refresh.addEventListener("onClick", dataSearchDefault);

						searchsampai.addEventListener("onChange", dataSearchDefault);
						searchmulai.addEventListener("onChange", dataSearchDefault);
						cari.addEventListener("onOK", dataSearchDefault);

						refresh.addEventListener("onClick", dataSearchDefault);
						blmDisetujui.addEventListener("onClick", dataSearchDefault);
						untukSaya.addEventListener("onClick", dataSearchDefault);

					}
				};

				pengajuanBaruEventListener.onEvent(null);
			}

			@Override
			public void onEvent(Event arg0) throws Exception {

				legacyDashboardRenderer = new LegacyDashboardRenderer() {
					public void render() throws Exception {
						if (tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getMahasiswa() == null) {
							menungguDisposisi();

							menungguDisposisiMasuk();
						}

						pengajuanBaru();
					}
				};

				renderDasborSuratInternal(mulai, sampai);

			}
		};

		reloadPengajuan.onEvent(null);
	}





	private void setGlobalFilterDasborSuratV9(Date mulai, Date sampai,
			ais.database.model.rab.SatuanKerja satuanKerja, String keyword) {
		dashboardGlobalMulaiV9 = mulai;
		dashboardGlobalSampaiV9 = sampai;
		dashboardGlobalSatuanKerjaV9 = satuanKerja;
		dashboardGlobalKeywordV9 = keyword == null ? "" : keyword.trim();
	}

	private Date getGlobalMulaiV9(Date fallback) {
		return dashboardGlobalMulaiV9 == null ? fallback : dashboardGlobalMulaiV9;
	}

	private Date getGlobalSampaiV9(Date fallback) {
		return dashboardGlobalSampaiV9 == null ? fallback : dashboardGlobalSampaiV9;
	}

	private ais.database.model.rab.SatuanKerja getGlobalSatuanKerjaV9() {
		return dashboardGlobalSatuanKerjaV9;
	}

	private String getGlobalKeywordV9() {
		return dashboardGlobalKeywordV9 == null ? "" : dashboardGlobalKeywordV9;
	}

	private void renderDashboardOperasionalGlobalV9(org.zkoss.zul.Div body) {
		if (legacyDashboardRenderer == null) {
			return;
		}

		appendHtml(body, sectionIntroHtml("Dashboard Operasional Global",
				"Panel operasional existing di bawah ini sudah memakai filter global dashboard: tanggal mulai, tanggal sampai, "
						+ "satuan kerja untuk data surat keluar, dan kata kunci pencarian. Karena posisinya tepat setelah overview, "
						+ "user dapat langsung melihat ringkasan lalu daftar kerja yang sesuai filter."));

		MyPortallayout legacyLayout = new MyPortallayout();
		legacyLayout.setParent(body);
		legacyLayout.setWidth("100%");
		legacyLayout.setMaximizedMode("whole");
		legacyLayout.setStyle("margin-top:10px; padding:0; background:transparent;");

		MyPortallayout previousLayout = dashboardLegacyLayout;
		dashboardLegacyLayout = legacyLayout;
		try {
			legacyDashboardRenderer.render();
			legacyDashboardRenderedInlineV9 = true;
		} catch (Exception e) {
			debugError("renderDashboardOperasionalGlobalV9", e);
			appendHtml(body, "<div style='padding:14px; margin-top:10px; border-radius:14px; background:#fff7ed; "
					+ "border:1px solid #fed7aa; color:#9a3412; font-size:12px; font-weight:700;'>"
					+ "Dashboard operasional global belum dapat dimuat. Aktifkan <b>debug = true</b> untuk melihat detail error di console.</div>");
		} finally {
			dashboardLegacyLayout = previousLayout;
		}
	}

	private String legacyPanelStyleV7() {
		return "margin-bottom:12px; border:1px solid #e5e7eb; border-radius:16px; overflow:hidden;"
				+ "background:#ffffff; box-shadow:0 12px 24px rgba(15,23,42,.07);";
	}

	private String toolbarStyleV7() {
		return "border:0; background:#f8fafc; border-radius:14px; padding:8px; margin-bottom:8px;"
				+ "box-shadow:inset 0 0 0 1px #e5e7eb;";
	}

	private MyPortallayout getCurrentDashboardPortalParent() {
		return dashboardLegacyLayout == null ? DasboardSurat.this : dashboardLegacyLayout;
	}

	private void renderDasborSuratInternal(final Date mulaiDefault, final Date sampaiDefault) throws Exception {
		MyPortalchildren portalchildren = new MyPortalchildren();
		portalchildren.setParent(DasboardSurat.this);
		portalchildren.setWidth("100%");
		portalchildren.setStyle("padding: 5px; margin-bottom: 12px;");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(portalchildren);
		panel.setTitle("Dasbor Surat");
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle("margin-bottom:14px; border:1px solid #e6edf5; border-radius:16px; "
				+ "background:#ffffff; box-shadow:0 12px 30px rgba(15,23,42,0.08); overflow:hidden;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);
		panelchildren.setStyle("padding:0; background:#f8fafc;");

		/*
		 * V10: Tabbox/Tabs/Tabpanels dihapus karena hanya berisi satu tab "Dasbor".
		 * Konten dashboard langsung ditempatkan di Panelchildren agar DOM lebih ringan
		 * dan tidak ada tab kosong/tidak terpakai.
		 */
		final org.zkoss.zul.Div body = new org.zkoss.zul.Div();
		body.setParent(panelchildren);
		body.setWidth("100%");
		body.setStyle("box-sizing:border-box; padding:14px; background:#f8fafc;");
		renderDasborSuratContent(body, mulaiDefault, sampaiDefault, null, null);
	}

	private void renderDasborSuratContent(final org.zkoss.zul.Div body, final Date mulai, final Date sampai,
			final ais.database.model.rab.SatuanKerja satuanKerja, final String keyword) throws Exception {
		if (body == null) {
			return;
		}
		tampilkanLoadingDashboardSuratV19(body, "Menyiapkan Dasbor Surat",
				"Menyiapkan filter dan antrian proses dashboard persuratan...", 5);

		if (body.getPage() == null) {
			renderDasborSuratContentInternal(body, mulai, sampai, satuanKerja, keyword);
			return;
		}

		final DashboardSuratRenderRequestV19 request = new DashboardSuratRenderRequestV19(body, mulai, sampai,
				satuanKerja, keyword);
		final EventListener listener = new EventListener() {
			public void onEvent(Event event) throws Exception {
				try {
					body.removeEventListener(EVENT_RENDER_DASBOR_SURAT_V19, this);
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:2141");
				}
				DashboardSuratRenderRequestV19 r = (DashboardSuratRenderRequestV19) event.getData();
				renderDasborSuratContentInternal(r.body, r.mulai, r.sampai, r.satuanKerja, r.keyword);
			}
		};
		body.addEventListener(EVENT_RENDER_DASBOR_SURAT_V19, listener);
		Events.echoEvent(EVENT_RENDER_DASBOR_SURAT_V19, body, request);
	}


	private void renderDasborSuratContentInternal(final org.zkoss.zul.Div body, Date mulai, Date sampai,
			final ais.database.model.rab.SatuanKerja satuanKerja, String keyword) throws Exception {
		tampilkanLoadingDashboardSuratV19(body, "Validasi Filter Dasbor Surat",
				"Menyiapkan periode, satuan kerja, keyword, dan akses pengguna...", 10);

		if (mulai == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
			mulai = calendar.getTime();
		}
		if (sampai == null) {
			sampai = new Date();
		}
		if (keyword == null) {
			keyword = "";
		}

		setGlobalFilterDasborSuratV9(mulai, sampai, satuanKerja, keyword);
		legacyDashboardRenderedInlineV9 = false;

		try {
			tampilkanLoadingDashboardSuratV19(body, "Mengambil Ringkasan Dasbor",
					"Menghitung pengajuan, persetujuan, disposisi masuk, SLA, dan indikator tata kelola...", 35);
			DashboardSuratData data = loadDashboardSuratData(mulai, sampai, satuanKerja, keyword);

			tampilkanLoadingDashboardSuratV19(body, "Menyusun Tampilan Dasbor Surat",
					"Menyusun hero, filter, overview, panel operasional, dan analitik persuratan...", 80);
			Common.clear(body);
			renderDasborSuratHero(body, mulai, sampai);
			renderDasborSuratFilter(body, mulai, sampai, satuanKerja, keyword);

			registerDashboardDetailBridgeV8(body, data);
			renderDasborSuratOverview(body, data);

			/*
			 * V9: panel operasional existing memakai filter global dan ditempatkan langsung
			 * setelah overview, sebelum renderDasborExistingDiBawahOverview(...).
			 */
			renderDashboardOperasionalGlobalV9(body);

			renderDasborExistingDiBawahOverview(body);

			renderDasborSuratAnalitik(body, data);
			renderDasborSuratTindakLanjut(body, data);
			renderDasborSuratAktivitas(body, data);
			renderDasborTataKelolaPersuratanV6(body, data);
			renderDasborSopTemplateTambahanV7(body, data);
		} catch (Exception e) {
			debugError("renderDasborSuratContent", e);
			appendHtml(body,
					"<div style='padding:16px; margin-top:12px; border-radius:14px; background:#fff1f2; color:#991b1b; "
							+ "border:1px solid #fecdd3; font-weight:600;'>Dasbor belum dapat dimuat. "
							+ "Silakan gunakan daftar surat seperti biasa atau tekan Refresh setelah koneksi database siap.</div>");
		}
	}


	private void renderDasborExistingDiBawahOverview(org.zkoss.zul.Div body) {
		if (legacyDashboardRenderedInlineV9) {
			return;
		}
		if (legacyDashboardRenderer == null) {
			return;
		}

		appendHtml(body, sectionIntroHtml("Dashboard Operasional Existing",
				"Bagian ini berisi dashboard lama yang sudah digunakan harian: pengajuan surat, persetujuan surat keluar, "
						+ "dan disposisi surat masuk. Pada V7 posisinya dipindahkan tepat di bawah overview agar user langsung "
						+ "melihat ringkasan angka terlebih dahulu, lalu daftar kerja operasional yang perlu diproses."));

		MyPortallayout legacyLayout = new MyPortallayout();
		legacyLayout.setParent(body);
		legacyLayout.setWidth("100%");
		legacyLayout.setMaximizedMode("whole");
		legacyLayout.setStyle("margin-top:10px; padding:0; background:transparent;");

		MyPortallayout previousLayout = dashboardLegacyLayout;
		dashboardLegacyLayout = legacyLayout;
		try {
			legacyDashboardRenderer.render();
		} catch (Exception e) {
			debugError("renderDasborExistingDiBawahOverview", e);
			appendHtml(body, "<div style='padding:14px; margin-top:10px; border-radius:14px; background:#fff7ed; "
					+ "border:1px solid #fed7aa; color:#9a3412; font-size:12px; font-weight:700;'>"
					+ "Dashboard operasional existing belum dapat dimuat. Aktifkan <b>debug = true</b> untuk melihat detail error di console.</div>");
		} finally {
			dashboardLegacyLayout = previousLayout;
		}
	}

	private void renderDasborSuratHero(org.zkoss.zul.Div parent, Date mulai, Date sampai) {
		String periode = formatTanggalDasbor(mulai) + " s.d. " + formatTanggalDasbor(sampai);
		String user = tbmuser == null ? "Pengguna" : safeText(tbmuser.getUserNama());
		appendHtml(parent, "<div style='position:relative; overflow:hidden; border-radius:18px; padding:22px; "
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); "
				+ "color:#ffffff; box-shadow:0 18px 38px rgba(29,78,216,0.22);'>"
				+ "<div style='position:absolute; width:240px; height:240px; right:-70px; top:-90px; border-radius:999px; "
				+ "background:rgba(255,255,255,0.13);'></div>"
				+ "<div style='position:absolute; width:160px; height:160px; right:120px; bottom:-92px; border-radius:999px; "
				+ "background:rgba(255,255,255,0.10);'></div>"
				+ "<div style='position:relative; z-index:2;'>"
				+ "<div style='font-size:12px; letter-spacing:.12em; text-transform:uppercase; opacity:.86; font-weight:700;'>Monitoring Persuratan & Alur Persetujuan</div>"
				+ "<div style='font-size:28px; line-height:1.18; font-weight:800; margin-top:7px;'>Dasbor Surat Terpadu</div>"
				+ "<div style='font-size:13px; max-width:820px; opacity:.93; margin-top:8px;'>Ringkasan pengajuan, persetujuan surat keluar, disposisi surat masuk, tren volume, dan daftar tindak lanjut prioritas dalam satu halaman internal.</div>"
				+ "<div style='margin-top:14px; display:flex; gap:8px; flex-wrap:wrap;'>"
				+ badgeHtml("Periode: " + safeText(periode), "rgba(255,255,255,.16)", "#ffffff")
				+ badgeHtml("User: " + user, "rgba(255,255,255,.16)", "#ffffff")
				+ badgeHtml(isUserInternal() ? "Akses: Internal" : "Akses: Pemohon", "rgba(255,255,255,.16)", "#ffffff")
				+ "</div></div></div>");
	}

	private void renderDasborSuratFilter(final org.zkoss.zul.Div parent, Date mulai, Date sampai,
			final ais.database.model.rab.SatuanKerja satuanKerja, String keyword) throws Exception {
		final org.zkoss.zul.Div filterContainer = new org.zkoss.zul.Div();
		filterContainer.setParent(parent);
		filterContainer.setStyle("margin-top:14px; padding:14px; background:#ffffff; border:1px solid #e8eef6; "
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
		txtKeyword.setTooltiptext("Cari perihal, kode, nama, agenda, ringkasan, atau nomor surat");
		txtKeyword.setParent(toolbar);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan Dasbor", "/img/svg/search.svg");
		refresh.setTooltiptext("Refresh dasbor berdasarkan filter");
		refresh.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; "
				+ "padding:6px 14px; margin-left:4px;");
		refresh.setParent(toolbar);
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				renderDasborSuratContent(parent, dbMulai.getValue(), dbSampai.getValue(),
						(ais.database.model.rab.SatuanKerja) cbSatker.getAttribute("satuanKerja"),
						txtKeyword.getValue());
			}
		});
		txtKeyword.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				renderDasborSuratContent(parent, dbMulai.getValue(), dbSampai.getValue(),
						(ais.database.model.rab.SatuanKerja) cbSatker.getAttribute("satuanKerja"),
						txtKeyword.getValue());
			}
		});
	}

	private DashboardSuratData loadDashboardSuratData(Date mulai, Date sampai,
			ais.database.model.rab.SatuanKerja satuanKerja, String keyword) throws Exception {
		DashboardSuratData d = new DashboardSuratData();
		d.mulai = mulai;
		d.sampai = sampai;
		d.satuanKerja = satuanKerja;
		d.keyword = keyword == null ? "" : keyword.trim();

		Session session = HibernateUtil.currentSession();

		d.totalPengajuanKeluar = countCriteria(createCriteriaSuratKeluarDashboard(session, mulai, sampai, satuanKerja,
				d.keyword, false));

		d.totalTanpaKodeKeluar = countCriteria(createCriteriaSuratKeluarDashboard(session, mulai, sampai, satuanKerja,
				d.keyword, false).add(emptyTextCriterion("kode")));
		d.totalTanpaPerihalKeluar = countCriteria(createCriteriaSuratKeluarDashboard(session, mulai, sampai, satuanKerja,
				d.keyword, false).add(emptyTextCriterion("perihal")));
		d.totalTanpaSatkerKeluar = countCriteria(createCriteriaSuratKeluarDashboard(session, mulai, sampai, satuanKerja,
				d.keyword, false).add(Restrictions.isNull("satuanKerja")));

		d.totalPersetujuanKeluar = countCriteria(createCriteriaPersetujuanKeluarDashboard(session, mulai, sampai,
				satuanKerja, d.keyword, false));
		d.totalMenungguKeluar = countCriteria(createCriteriaPersetujuanKeluarDashboard(session, mulai, sampai,
				satuanKerja, d.keyword, false).add(pendingCriterion()));
		d.totalDisetujuiKeluar = countCriteria(createCriteriaPersetujuanKeluarDashboard(session, mulai, sampai,
				satuanKerja, d.keyword, false).add(Restrictions.eq("disetujui", true)));
		d.totalDitolakKeluar = countCriteria(createCriteriaPersetujuanKeluarDashboard(session, mulai, sampai,
				satuanKerja, d.keyword, false).add(Restrictions.eq("ditolak", true)));
		d.totalRevisiKeluar = countCriteria(createCriteriaPersetujuanKeluarDashboard(session, mulai, sampai,
				satuanKerja, d.keyword, false).add(Restrictions.eq("telahDirevisi", true)));

		if (isUserInternal()) {
			d.totalPersetujuanMasuk = countCriteria(createCriteriaPersetujuanMasukDashboard(session, mulai, sampai,
					d.keyword, false));
			d.totalMenungguMasuk = countCriteria(createCriteriaPersetujuanMasukDashboard(session, mulai, sampai,
					d.keyword, false).add(pendingCriterion()));
			d.totalDisetujuiMasuk = countCriteria(createCriteriaPersetujuanMasukDashboard(session, mulai, sampai,
					d.keyword, false).add(Restrictions.eq("disetujui", true)));
			d.totalDitolakMasuk = countCriteria(createCriteriaPersetujuanMasukDashboard(session, mulai, sampai,
					d.keyword, false).add(Restrictions.eq("ditolak", true)));
			d.totalRevisiMasuk = countCriteria(createCriteriaPersetujuanMasukDashboard(session, mulai, sampai,
					d.keyword, false).add(Restrictions.eq("telahDirevisi", true)));
		}

		d.topKlasifikasiKeluar = loadTopKlasifikasiKeluar(session, mulai, sampai, satuanKerja, d.keyword);
		d.topSatkerKeluar = loadTopSatkerKeluar(session, mulai, sampai, satuanKerja, d.keyword);
		d.trendPengajuanKeluar = loadTrendPengajuanKeluar(session, mulai, sampai, satuanKerja, d.keyword);
		d.recentPengajuanKeluar = loadRecentPengajuanKeluar(session, mulai, sampai, satuanKerja, d.keyword);
		d.pendingKeluar = loadPendingKeluar(session, mulai, sampai, satuanKerja, d.keyword);
		if (isUserInternal()) {
			d.pendingMasuk = loadPendingMasuk(session, mulai, sampai, d.keyword);
		}
		return d;
	}

	private Criteria createCriteriaSuratKeluarDashboard(Session session, Date mulai, Date sampai,
			ais.database.model.rab.SatuanKerja satuanKerja, String keyword, boolean order) {
		Criteria criteria = session.createCriteria(SuratKeluar.class)
				.createAlias("klasifikasiSuratKeluar", "aksesKlasifikasiSuratKeluar", Criteria.LEFT_JOIN);
		applyDateFilter(criteria, "tanggal", mulai, sampai);
		if (satuanKerja != null) {
			criteria.add(Restrictions.eq("satuanKerja", satuanKerja));
		}
		criteria.add(createSuratKeluarAccessCriterion());
		criteria.add(createSuratKeluarKeywordCriterion("", keyword));
		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		return criteria;
	}

	private Criteria createCriteriaPersetujuanKeluarDashboard(Session session, Date mulai, Date sampai,
			ais.database.model.rab.SatuanKerja satuanKerja, String keyword, boolean order) {
		Criteria criteria = session.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
				.add(Restrictions.isNotNull("kodeUnik")).createAlias("suratKeluar", "suratKeluar")
				.createAlias("suratKeluar.klasifikasiSuratKeluar", "aksesKlasifikasiSuratKeluar", Criteria.LEFT_JOIN)
				.createAlias("pejabat", "pejabat", Criteria.LEFT_JOIN);
		applyDateFilter(criteria, "suratKeluar.tanggal", mulai, sampai);
		if (satuanKerja != null) {
			criteria.add(Restrictions.eq("suratKeluar.satuanKerja", satuanKerja));
		}
		// V16: visibility surat ATAU akses pejabat.
		// Jika dibuat AND, surat yang belum masuk disposisi pejabat tidak tampil.
		criteria.add(createSuratKeluarAccessDenganUntukSayaV17(false, "suratKeluar.", "aksesKlasifikasiSuratKeluar."));
		criteria.add(createSuratKeluarKeywordCriterion("suratKeluar.", keyword));
		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		return criteria;
	}

	private Criteria createCriteriaPersetujuanMasukDashboard(Session session, Date mulai, Date sampai, String keyword,
			boolean order) {
		Criteria criteria = session.createCriteria(AlurPersetujuanSuratMasukStatus.class)
				.add(Restrictions.isNotNull("kodeUnik")).createAlias("suratMasuk", "suratMasuk")
				.createAlias("suratMasuk.klasifikasiSuratMasuk", "aksesKlasifikasiSuratMasuk", Criteria.LEFT_JOIN)
				.createAlias("pejabat", "pejabat", Criteria.LEFT_JOIN);
		applyDateFilter(criteria, "suratMasuk.tanggal", mulai, sampai);
		// V16: visibility surat ATAU akses pejabat.
		// Jika dibuat AND, surat yang belum masuk disposisi pejabat tidak tampil.
		criteria.add(createSuratMasukAccessDenganUntukSayaV17(false, "suratMasuk.", "aksesKlasifikasiSuratMasuk.", ""));
		criteria.add(createSuratMasukKeywordCriterion("suratMasuk.", keyword));
		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		return criteria;
	}

	private void applyDateFilter(Criteria criteria, String propertyName, Date mulai, Date sampai) {
		if (mulai != null) {
			criteria.add(Restrictions.ge(propertyName, mulai));
		}
		if (sampai != null) {
			criteria.add(Restrictions.le(propertyName, sampai));
		}
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

	private org.hibernate.criterion.Criterion createSuratKeluarAccessCriterion() {
		return createSuratKeluarVisibilityCriterion("", "aksesKlasifikasiSuratKeluar.");
	}


	
	private boolean bolehTampilkanFilterUntukSayaV17() {
		try {
			Tbmrole tbmrole = tbmuser == null ? null : tbmuser.hakAkses();
			return tbmrole != null && tbmrole.getRoleId() != null && Boolean.TRUE.equals(tbmrole.getMelihatSemuaSurat());
		} catch (Exception e) {
			debugError("bolehTampilkanFilterUntukSayaV17", e);
			return false;
		}
	}

	private Criterion createSuratKeluarAccessDenganUntukSayaV17(boolean untukSaya, String suratPrefix,
			String klasifikasiPrefix) {
		if (untukSaya) {
			return createPejabatAccessCriterionUntukSayaV17C();
		}
		return createSuratKeluarOrPejabatAccessCriterion(suratPrefix, klasifikasiPrefix);
	}

	private Criterion createSuratMasukAccessDenganUntukSayaV17(boolean untukSaya, String suratPrefix,
			String klasifikasiPrefix, String statusPrefix) {
		if (untukSaya) {
			return createPejabatAccessCriterionUntukSayaV17C();
		}
		return createSuratMasukOrPejabatAccessCriterion(suratPrefix, klasifikasiPrefix, statusPrefix);
	}

	/**
	 * V17C:
	 * Khusus checkbox "Untuk Saya".
	 * Jangan pakai createPejabatAccessCriterion() karena method itu sengaja return true
	 * untuk user dengan hak melihat semua surat. Untuk filter "Untuk Saya", user harus
	 * benar-benar match dengan pejabat/jenis jabatan/username/jenis pengguna di baris alur.
	 */
	private org.hibernate.criterion.Criterion createPejabatAccessCriterionUntukSayaV17C() {
		List<Pejabat> pejabats = null;
		try {
			pejabats = Common.getCurrentPejabat(true);
		} catch (Exception e) {
			pejabats = new ArrayList<Pejabat>();
		}
		if (pejabats == null) {
			pejabats = new ArrayList<Pejabat>();
		}

		ArrayList<JenisJabatan> jenisJabatans = new ArrayList<JenisJabatan>();
		for (Pejabat pejabat : pejabats) {
			if (pejabat != null && pejabat.getJenisJabatan() != null) {
				jenisJabatans.add(pejabat.getJenisJabatan());
			}
		}

		org.hibernate.criterion.Criterion byJenisPengguna = Restrictions.sqlRestriction("false");
		org.hibernate.criterion.Criterion byUsername = Restrictions.sqlRestriction("false");
		try {
			if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null) {
				byJenisPengguna = Restrictions.ilike("pejabat.jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
						MatchMode.ANYWHERE);
			}
			String userIdText = tbmuser == null || tbmuser.getUserId() == null ? "" : String.valueOf(tbmuser.getUserId());
			if (!userIdText.trim().isEmpty()) {
				byUsername = Restrictions.ilike("pejabat.usernamePengguna", "," + userIdText + ",",
						MatchMode.ANYWHERE);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:2632");
		}

		org.hibernate.criterion.Criterion byJabatan = jenisJabatans.isEmpty() ? Restrictions.sqlRestriction("false")
				: Restrictions.in("pejabat.jenisJabatan", jenisJabatans);
		org.hibernate.criterion.Criterion byPejabat = pejabats.isEmpty() ? Restrictions.sqlRestriction("false")
				: Restrictions.in("pejabat", pejabats);

		return Restrictions.or(Restrictions.or(byJenisPengguna, byUsername), Restrictions.or(byJabatan, byPejabat));
	}


private Criterion createSuratKeluarOrPejabatAccessCriterion(String suratPrefix, String klasifikasiPrefix) {
		/*
		 * V16A FIX:
		 * Gabungan akses dibuat OR, bukan AND.
		 * - visibility surat: user boleh melihat surat berdasarkan konseptor/dosen/guru/mahasiswa/siswa/role klasifikasi.
		 * - akses pejabat: user sedang/berhak menjadi penerima disposisi.
		 *
		 * Dengan OR, surat yang belum masuk ke disposisi pejabat tetap tampil jika lolos visibility surat.
		 */
		return Restrictions.or(createSuratKeluarVisibilityCriterion(suratPrefix, klasifikasiPrefix),
				createPejabatAccessCriterion());
	}

	private Criterion createSuratMasukOrPejabatAccessCriterion(String suratPrefix, String klasifikasiPrefix,
			String statusPrefix) {
		return Restrictions.or(createSuratMasukVisibilityCriterion(suratPrefix, klasifikasiPrefix, statusPrefix),
				createPejabatAccessCriterion());
	}

	private org.hibernate.criterion.Criterion createPejabatAccessCriterion() {
		if (bolehMelihatSemuaSurat(tbmuser)) {
			return Restrictions.sqlRestriction("true");
		}
		List<Pejabat> pejabats = null;
		try {
			pejabats = Common.getCurrentPejabat(true);
		} catch (Exception e) {
			pejabats = new ArrayList<Pejabat>();
		}
		if (pejabats == null) {
			pejabats = new ArrayList<Pejabat>();
		}

		ArrayList<JenisJabatan> jenisJabatans = new ArrayList<JenisJabatan>();
		for (Pejabat pejabat : pejabats) {
			if (pejabat != null && pejabat.getJenisJabatan() != null) {
				jenisJabatans.add(pejabat.getJenisJabatan());
			}
		}

		org.hibernate.criterion.Criterion byJenisPengguna = Restrictions.sqlRestriction("false");
		org.hibernate.criterion.Criterion byUsername = Restrictions.sqlRestriction("false");
		try {
			if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null) {
				byJenisPengguna = Restrictions.ilike("pejabat.jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
						MatchMode.ANYWHERE);
			}
			String userIdText = tbmuser == null || tbmuser.getUserId() == null ? "" : String.valueOf(tbmuser.getUserId());
			if (!userIdText.trim().isEmpty()) {
				byUsername = Restrictions.ilike("pejabat.usernamePengguna", "," + userIdText + ",",
						MatchMode.ANYWHERE);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:2696");
		}

		org.hibernate.criterion.Criterion byJabatan = jenisJabatans.isEmpty() ? Restrictions.sqlRestriction("false")
				: Restrictions.in("pejabat.jenisJabatan", jenisJabatans);
		org.hibernate.criterion.Criterion byPejabat = pejabats.isEmpty() ? Restrictions.sqlRestriction("false")
				: Restrictions.in("pejabat", pejabats);
		return Restrictions.or(Restrictions.or(byJenisPengguna, byUsername), Restrictions.or(byJabatan, byPejabat));
	}

	private org.hibernate.criterion.Criterion createSuratKeluarKeywordCriterion(String prefix, String keyword) {
		String c = keyword == null ? "" : keyword.trim();
		if (c.isEmpty()) {
			return Restrictions.sqlRestriction("true");
		}
		// "Narasi surat" = ISI/parameter surat yang diisikan ke template, tersimpan sebagai
		// OpsiSuratKeluarValue (nama = label opsi, keterangan = isi/nilai) — BUKAN kolom langsung
		// SuratKeluar. Dicocokkan lewat subquery agar pencarian menjangkau isi/narasi surat, bukan
		// hanya perihal & kode. Subquery hanya ditambahkan bila ada kata kunci (tidak membebani default).
		DetachedCriteria narasiSub = DetachedCriteria.forClass(OpsiSuratKeluarValue.class)
				.createAlias("suratKeluar", "sk").setProjection(Projections.property("sk.id"))
				.add(Restrictions.or(Restrictions.ilike("nama", c, MatchMode.ANYWHERE),
						Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE)));
		return Restrictions.or(Restrictions.ilike(prefix + "perihal", c, MatchMode.ANYWHERE),
				Restrictions.or(Restrictions.ilike(prefix + "agenda", c, MatchMode.ANYWHERE),
						Restrictions.or(Restrictions.ilike(prefix + "keterangan", c, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike(prefix + "kode", c, MatchMode.ANYWHERE),
										Restrictions.or(Restrictions.ilike(prefix + "nama", c, MatchMode.ANYWHERE),
												Subqueries.propertyIn(prefix + "id", narasiSub))))));
	}

	private org.hibernate.criterion.Criterion createSuratMasukKeywordCriterion(String prefix, String keyword) {
		String c = keyword == null ? "" : keyword.trim();
		if (c.isEmpty()) {
			return Restrictions.sqlRestriction("true");
		}
		return Restrictions.or(Restrictions.ilike(prefix + "noSurat", c, MatchMode.ANYWHERE),
				Restrictions.or(Restrictions.ilike(prefix + "perihal", c, MatchMode.ANYWHERE),
						Restrictions.or(Restrictions.ilike(prefix + "ringkasan", c, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike(prefix + "keterangan", c, MatchMode.ANYWHERE),
										Restrictions.or(Restrictions.ilike(prefix + "kode", c, MatchMode.ANYWHERE),
												Restrictions.ilike(prefix + "nama", c, MatchMode.ANYWHERE))))));
	}

	
	private org.hibernate.criterion.Criterion emptyTextCriterion(String propertyName) {
		return Restrictions.or(Restrictions.isNull(propertyName), Restrictions.eq(propertyName, ""));
	}

	private org.hibernate.criterion.Criterion pendingCriterion() {
		return Restrictions.and(
				Restrictions.or(Restrictions.isNull("disetujui"), Restrictions.eq("disetujui", false)),
				Restrictions.or(Restrictions.isNull("ditolak"), Restrictions.eq("ditolak", false)));
	}

	private long countCriteria(Criteria criteria) {
		Object result = criteria.setProjection(Projections.rowCount()).uniqueResult();
		return result instanceof Number ? ((Number) result).longValue() : 0L;
	}

	@SuppressWarnings("unchecked")
	private List<DashboardMiniRow> loadTopKlasifikasiKeluar(Session session, Date mulai, Date sampai,
			ais.database.model.rab.SatuanKerja satuanKerja, String keyword) {
		Criteria criteria = createCriteriaSuratKeluarDashboard(session, mulai, sampai, satuanKerja, keyword, false);
		// V12 FIX: alias klasifikasiSuratKeluar sudah dibuat di createCriteriaSuratKeluarDashboard
		// sebagai aksesKlasifikasiSuratKeluar. Jangan createAlias ulang path yang sama.
		criteria.setProjection(Projections.projectionList()
				.add(Projections.groupProperty("aksesKlasifikasiSuratKeluar.nama")).add(Projections.rowCount()));
		List<Object[]> rows = criteria.list();
		List<DashboardMiniRow> result = new ArrayList<DashboardMiniRow>();
		for (Object[] row : rows) {
			String label = row[0] == null ? "Tanpa Klasifikasi" : String.valueOf(row[0]);
			long value = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
			result.add(new DashboardMiniRow(label, value));
		}
		sortAndLimit(result, 8);
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<DashboardMiniRow> loadTopSatkerKeluar(Session session, Date mulai, Date sampai,
			ais.database.model.rab.SatuanKerja satuanKerja, String keyword) {
		Criteria criteria = createCriteriaSuratKeluarDashboard(session, mulai, sampai, satuanKerja, keyword, false);
		criteria.createAlias("satuanKerja", "satker", Criteria.LEFT_JOIN);
		criteria.setProjection(Projections.projectionList().add(Projections.groupProperty("satker.nama"))
				.add(Projections.rowCount()));
		List<Object[]> rows = criteria.list();
		List<DashboardMiniRow> result = new ArrayList<DashboardMiniRow>();
		for (Object[] row : rows) {
			String label = row[0] == null ? "Tanpa Satuan Kerja" : String.valueOf(row[0]);
			long value = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
			result.add(new DashboardMiniRow(label, value));
		}
		sortAndLimit(result, 8);
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<DashboardMiniRow> loadTrendPengajuanKeluar(Session session, Date mulai, Date sampai,
			ais.database.model.rab.SatuanKerja satuanKerja, String keyword) {
		Criteria criteria = createCriteriaSuratKeluarDashboard(session, mulai, sampai, satuanKerja, keyword, false);
		criteria.setProjection(Projections.property("tanggal"));
		List<Date> tanggals = criteria.list();
		java.util.TreeMap<Integer, DashboardMiniRow> map = new java.util.TreeMap<Integer, DashboardMiniRow>();
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM yyyy", new java.util.Locale("id", "ID"));
		for (Date tanggal : tanggals) {
			if (tanggal == null) {
				continue;
			}
			Calendar cal = Calendar.getInstance();
			cal.setTime(tanggal);
			int key = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH);
			DashboardMiniRow row = map.get(key);
			if (row == null) {
				row = new DashboardMiniRow(sdf.format(tanggal), 0L);
				map.put(key, row);
			}
			row.value++;
		}
		List<DashboardMiniRow> result = new ArrayList<DashboardMiniRow>(map.values());
		int max = 12;
		while (result.size() > max) {
			result.remove(0);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<DashboardItem> loadRecentPengajuanKeluar(Session session, Date mulai, Date sampai,
			ais.database.model.rab.SatuanKerja satuanKerja, String keyword) {
		List<DashboardItem> result = new ArrayList<DashboardItem>();
		List<SuratKeluar> rows = createCriteriaSuratKeluarDashboard(session, mulai, sampai, satuanKerja, keyword, true)
				.setMaxResults(8).list();
		for (SuratKeluar suratKeluar : rows) {
			if (suratKeluar == null) {
				continue;
			}
			String title = firstNotEmpty(suratKeluar.getKode(), suratKeluar.getNama(), "Surat Keluar");
			String desc = safeText(suratKeluar.getPerihal());
			String klasifikasi = suratKeluar.getKlasifikasiSuratKeluar() == null ? "Tanpa klasifikasi"
					: safeText(suratKeluar.getKlasifikasiSuratKeluar().getNama());
			String meta = klasifikasi + " • " + formatTanggalDasbor(suratKeluar.getTanggal());
			result.add(new DashboardItem(title, desc, meta, "Pengajuan"));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<DashboardItem> loadPendingKeluar(Session session, Date mulai, Date sampai,
			ais.database.model.rab.SatuanKerja satuanKerja, String keyword) {
		List<DashboardItem> result = new ArrayList<DashboardItem>();
		List<AlurPersetujuanSuratKeluarStatus> rows = createCriteriaPersetujuanKeluarDashboard(session, mulai, sampai,
				satuanKerja, keyword, true).add(pendingCriterion()).setMaxResults(8).list();
		for (AlurPersetujuanSuratKeluarStatus status : rows) {
			if (status == null || status.getSuratKeluar() == null) {
				continue;
			}
			SuratKeluar surat = status.getSuratKeluar();
			String title = firstNotEmpty(surat.getKode(), surat.getNama(), "Surat Keluar");
			String desc = safeText(surat.getPerihal());
			String meta = getTargetPersetujuanKeluar(status) + " • " + formatTanggalDasbor(surat.getTanggal());
			result.add(new DashboardItem(title, desc, meta, "Menunggu keluar"));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<DashboardItem> loadPendingMasuk(Session session, Date mulai, Date sampai, String keyword) {
		List<DashboardItem> result = new ArrayList<DashboardItem>();
		List<AlurPersetujuanSuratMasukStatus> rows = createCriteriaPersetujuanMasukDashboard(session, mulai, sampai,
				keyword, true).add(pendingCriterion()).setMaxResults(8).list();
		for (AlurPersetujuanSuratMasukStatus status : rows) {
			if (status == null || status.getSuratMasuk() == null) {
				continue;
			}
			SuratMasuk surat = status.getSuratMasuk();
			String title = firstNotEmpty(surat.getKode(), surat.getNama(), surat.getKode(), "Surat Masuk");
			String desc = safeText(surat.getPerihal());
			String meta = getTargetPersetujuanMasuk(status) + " • " + formatTanggalDasbor(surat.getTanggal());
			result.add(new DashboardItem(title, desc, meta, "Menunggu masuk"));
		}
		return result;
	}


	/**
	 * Kontrak callback/strategi bersarang milik {@link DasboardSurat}. Tipe ini memisahkan satu variasi perilaku
	 * lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DasboardSurat} dan dapat mengakses state kelas
	 * induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
	 * implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code count()}, {@code list}(). Aturan
	 * bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see DasboardSurat
	 */
	private interface DashboardDetailProviderV8 {
		long count() throws Exception;

		List list(int firstResult, int maxResults) throws Exception;
	}

	/**
	 * Kontrak callback/strategi bersarang milik {@link DasboardSurat}. Tipe ini memisahkan satu variasi perilaku
	 * lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DasboardSurat} dan dapat mengakses state kelas
	 * induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
	 * implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code build}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see DasboardSurat
	 */
	private interface DashboardDetailCriteriaBuilderV8 {
		Criteria build(Session session, boolean order) throws Exception;
	}

	private void registerDashboardDetailBridgeV8(org.zkoss.zul.Div body, final DashboardSuratData data) {
		try {
			dashboardDetailBridgeIdV8 = "dsDetailBridgeV8" + System.currentTimeMillis();

			org.zkoss.zul.Div bridge = new org.zkoss.zul.Div();
			bridge.setId(dashboardDetailBridgeIdV8);
			bridge.setVisible(false);
			bridge.setParent(body);
			bridge.addEventListener("onDashboardDetail", new EventListener() {
				public void onEvent(Event event) throws Exception {
					String detailKey = event == null || event.getData() == null ? "" : String.valueOf(event.getData());
					viewDetailByKeyV8(detailKey, data);
				}
			});
		} catch (Exception e) {
			debugError("registerDashboardDetailBridgeV8", e);
		}
	}

	private String detailLinkHtmlV8(String detailKey, long value) {
		String valueText = String.valueOf(value);
		if (detailKey == null || detailKey.trim().isEmpty() || dashboardDetailBridgeIdV8 == null) {
			return valueText;
		}
		String key = escapeJsV8(detailKey);
		String bridgeId = escapeJsV8(dashboardDetailBridgeIdV8);
		return "<a href='#' title='Klik untuk melihat detail data' "
				+ "onclick=\"try{var w=zk.Widget.$('$" + bridgeId
				+ "');if(w){zAu.send(new zk.Event(w,'onDashboardDetail','" + key
				+ "',{toServer:true}));}}catch(e){} return false;\" "
				+ "style='color:inherit; text-decoration:none; border-bottom:1px dashed currentColor; cursor:pointer;'>"
				+ valueText + "</a>";
	}

	private String escapeJsV8(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"")
				.replace("\r", " ").replace("\n", " ");
	}

	private String resolveMetricDetailKeyV8(String title) {
		String t = safeText(title).toLowerCase();
		if (t.indexOf("pengajuan surat") >= 0 || t.indexOf("pengajuan surat keluar") >= 0) {
			return "DETAIL_PENGAJUAN_KELUAR";
		}
		if (t.indexOf("menunggu tindak lanjut") >= 0 || t.indexOf("backlog alur") >= 0) {
			return "DETAIL_MENUNGGU_ALL";
		}
		if (t.indexOf("menunggu keluar") >= 0 || t.indexOf("menunggu persetujuan keluar") >= 0) {
			return "DETAIL_MENUNGGU_KELUAR";
		}
		if (t.indexOf("menunggu masuk") >= 0 || t.indexOf("menunggu disposisi masuk") >= 0) {
			return "DETAIL_MENUNGGU_MASUK";
		}
		if (t.indexOf("disetujui / diproses") >= 0 || t.indexOf("disposisi masuk selesai") >= 0) {
			return "DETAIL_SELESAI_ALL";
		}
		if (t.indexOf("disetujui surat keluar") >= 0) {
			return "DETAIL_DISETUJUI_KELUAR";
		}
		if (t.indexOf("ditolak") >= 0 && t.indexOf("revisi") < 0) {
			return "DETAIL_DITOLAK_ALL";
		}
		if (t.indexOf("revisi") >= 0 || t.indexOf("risiko revisi/tolak") >= 0 || t.indexOf("perlu koreksi") >= 0) {
			return "DETAIL_REVISI_DITOLAK";
		}
		if (t.indexOf("total alur") >= 0) {
			return "DETAIL_TOTAL_ALUR";
		}
		if (t.indexOf("tanpa kode") >= 0) {
			return "DETAIL_TANPA_KODE";
		}
		if (t.indexOf("tanpa perihal") >= 0) {
			return "DETAIL_TANPA_PERIHAL";
		}
		if (t.indexOf("tanpa satker") >= 0 || t.indexOf("tanpa satuan kerja") >= 0) {
			return "DETAIL_TANPA_SATKER";
		}
		return null;
	}

	private String resolveProgressDetailKeyV8(String title, String label) {
		String t = safeText(title).toLowerCase();
		String l = safeText(label).toLowerCase();
		if (t.indexOf("metadata") >= 0) {
			return resolveMetricDetailKeyV8(label);
		}
		if (t.indexOf("surat keluar") >= 0 || t.indexOf("persetujuan") >= 0) {
			if (l.indexOf("menunggu") >= 0) {
				return "DETAIL_MENUNGGU_KELUAR";
			}
			if (l.indexOf("disetujui") >= 0 || l.indexOf("selesai") >= 0) {
				return "DETAIL_DISETUJUI_KELUAR";
			}
			if (l.indexOf("ditolak") >= 0) {
				return "DETAIL_DITOLAK_KELUAR";
			}
		}
		if (t.indexOf("surat masuk") >= 0 || t.indexOf("disposisi") >= 0) {
			if (l.indexOf("menunggu") >= 0) {
				return "DETAIL_MENUNGGU_MASUK";
			}
			if (l.indexOf("disetujui") >= 0 || l.indexOf("selesai") >= 0) {
				return "DETAIL_DISETUJUI_MASUK";
			}
			if (l.indexOf("ditolak") >= 0) {
				return "DETAIL_DITOLAK_MASUK";
			}
		}
		if (l.indexOf("menunggu") >= 0) {
			return "DETAIL_MENUNGGU_ALL";
		}
		if (l.indexOf("selesai") >= 0 || l.indexOf("disetujui") >= 0) {
			return "DETAIL_SELESAI_ALL";
		}
		if (l.indexOf("ditolak") >= 0) {
			return "DETAIL_DITOLAK_ALL";
		}
		return null;
	}

	private String resolveMiniTableDetailKeyV8(String title, String label) {
		String t = safeText(title).toLowerCase();
		if (t.indexOf("klasifikasi") >= 0) {
			return "TOP_KLASIFIKASI|" + safeText(label);
		}
		if (t.indexOf("satuan kerja") >= 0 || t.indexOf("satker") >= 0) {
			return "TOP_SATKER|" + safeText(label);
		}
		return null;
	}

	private void viewDetailByKeyV8(String detailKey, DashboardSuratData data) {
		try {
			DashboardDetailProviderV8 provider = createDetailProviderByKeyV8(detailKey, data);
			if (provider == null) {
				viewDetail("Detail Data Dashboard", "Detail belum tersedia untuk angka ini.", emptyDetailProviderV8());
				return;
			}
			viewDetail(resolveDetailTitleV8(detailKey), resolveDetailDescriptionV8(detailKey), provider);
		} catch (Exception e) {
			debugError("viewDetailByKeyV8-" + detailKey, e);
		}
	}

	private DashboardDetailProviderV8 emptyDetailProviderV8() {
		return new DashboardDetailProviderV8() {
			public long count() {
				return 0L;
			}

			public List list(int firstResult, int maxResults) {
				return new ArrayList();
			}
		};
	}

	private DashboardDetailProviderV8 createDetailProviderByKeyV8(String detailKey, final DashboardSuratData data) {
		if (detailKey == null) {
			return null;
		}

		if ("DETAIL_PENGAJUAN_KELUAR".equals(detailKey)) {
			return criteriaDetailProviderV8(new DashboardDetailCriteriaBuilderV8() {
				public Criteria build(Session session, boolean order) {
					return createCriteriaSuratKeluarDashboard(session, data.mulai, data.sampai, data.satuanKerja,
							data.keyword, order);
				}
			});
		}
		if ("DETAIL_MENUNGGU_KELUAR".equals(detailKey)) {
			return criteriaDetailProviderV8(new DashboardDetailCriteriaBuilderV8() {
				public Criteria build(Session session, boolean order) {
					return createCriteriaPersetujuanKeluarDashboard(session, data.mulai, data.sampai, data.satuanKerja,
							data.keyword, order).add(pendingCriterion());
				}
			});
		}
		if ("DETAIL_DISETUJUI_KELUAR".equals(detailKey)) {
			return criteriaDetailProviderV8(new DashboardDetailCriteriaBuilderV8() {
				public Criteria build(Session session, boolean order) {
					return createCriteriaPersetujuanKeluarDashboard(session, data.mulai, data.sampai, data.satuanKerja,
							data.keyword, order).add(Restrictions.eq("disetujui", true));
				}
			});
		}
		if ("DETAIL_DITOLAK_KELUAR".equals(detailKey)) {
			return criteriaDetailProviderV8(new DashboardDetailCriteriaBuilderV8() {
				public Criteria build(Session session, boolean order) {
					return createCriteriaPersetujuanKeluarDashboard(session, data.mulai, data.sampai, data.satuanKerja,
							data.keyword, order).add(Restrictions.eq("ditolak", true));
				}
			});
		}
		if ("DETAIL_REVISI_KELUAR".equals(detailKey)) {
			return criteriaDetailProviderV8(new DashboardDetailCriteriaBuilderV8() {
				public Criteria build(Session session, boolean order) {
					return createCriteriaPersetujuanKeluarDashboard(session, data.mulai, data.sampai, data.satuanKerja,
							data.keyword, order).add(Restrictions.eq("telahDirevisi", true));
				}
			});
		}
		if ("DETAIL_MENUNGGU_MASUK".equals(detailKey)) {
			return criteriaDetailProviderV8(new DashboardDetailCriteriaBuilderV8() {
				public Criteria build(Session session, boolean order) {
					return createCriteriaPersetujuanMasukDashboard(session, data.mulai, data.sampai, data.keyword, order)
							.add(pendingCriterion());
				}
			});
		}
		if ("DETAIL_DISETUJUI_MASUK".equals(detailKey)) {
			return criteriaDetailProviderV8(new DashboardDetailCriteriaBuilderV8() {
				public Criteria build(Session session, boolean order) {
					return createCriteriaPersetujuanMasukDashboard(session, data.mulai, data.sampai, data.keyword, order)
							.add(Restrictions.eq("disetujui", true));
				}
			});
		}
		if ("DETAIL_DITOLAK_MASUK".equals(detailKey)) {
			return criteriaDetailProviderV8(new DashboardDetailCriteriaBuilderV8() {
				public Criteria build(Session session, boolean order) {
					return createCriteriaPersetujuanMasukDashboard(session, data.mulai, data.sampai, data.keyword, order)
							.add(Restrictions.eq("ditolak", true));
				}
			});
		}
		if ("DETAIL_REVISI_MASUK".equals(detailKey)) {
			return criteriaDetailProviderV8(new DashboardDetailCriteriaBuilderV8() {
				public Criteria build(Session session, boolean order) {
					return createCriteriaPersetujuanMasukDashboard(session, data.mulai, data.sampai, data.keyword, order)
							.add(Restrictions.eq("telahDirevisi", true));
				}
			});
		}
		if ("DETAIL_MENUNGGU_ALL".equals(detailKey)) {
			return combinedDetailProviderV8(createDetailProviderByKeyV8("DETAIL_MENUNGGU_KELUAR", data),
					createDetailProviderByKeyV8("DETAIL_MENUNGGU_MASUK", data));
		}
		if ("DETAIL_SELESAI_ALL".equals(detailKey)) {
			return combinedDetailProviderV8(createDetailProviderByKeyV8("DETAIL_DISETUJUI_KELUAR", data),
					createDetailProviderByKeyV8("DETAIL_DISETUJUI_MASUK", data));
		}
		if ("DETAIL_DITOLAK_ALL".equals(detailKey)) {
			return combinedDetailProviderV8(createDetailProviderByKeyV8("DETAIL_DITOLAK_KELUAR", data),
					createDetailProviderByKeyV8("DETAIL_DITOLAK_MASUK", data));
		}
		if ("DETAIL_REVISI_DITOLAK".equals(detailKey)) {
			return combinedDetailProviderV8(createDetailProviderByKeyV8("DETAIL_REVISI_KELUAR", data),
					createDetailProviderByKeyV8("DETAIL_REVISI_MASUK", data),
					createDetailProviderByKeyV8("DETAIL_DITOLAK_KELUAR", data),
					createDetailProviderByKeyV8("DETAIL_DITOLAK_MASUK", data));
		}
		if ("DETAIL_TOTAL_ALUR".equals(detailKey)) {
			return combinedDetailProviderV8(new DashboardDetailProviderV8[] {
					createDetailProviderByKeyV8("DETAIL_MENUNGGU_KELUAR", data),
					createDetailProviderByKeyV8("DETAIL_DISETUJUI_KELUAR", data),
					createDetailProviderByKeyV8("DETAIL_DITOLAK_KELUAR", data),
					createDetailProviderByKeyV8("DETAIL_MENUNGGU_MASUK", data),
					createDetailProviderByKeyV8("DETAIL_DISETUJUI_MASUK", data),
					createDetailProviderByKeyV8("DETAIL_DITOLAK_MASUK", data) });
		}
		if ("DETAIL_TANPA_KODE".equals(detailKey)) {
			return metadataSuratKeluarProviderV8(data, "kode");
		}
		if ("DETAIL_TANPA_PERIHAL".equals(detailKey)) {
			return metadataSuratKeluarProviderV8(data, "perihal");
		}
		if ("DETAIL_TANPA_SATKER".equals(detailKey)) {
			return criteriaDetailProviderV8(new DashboardDetailCriteriaBuilderV8() {
				public Criteria build(Session session, boolean order) {
					return createCriteriaSuratKeluarDashboard(session, data.mulai, data.sampai, data.satuanKerja,
							data.keyword, order).add(Restrictions.isNull("satuanKerja"));
				}
			});
		}
		if (detailKey.startsWith("TOP_KLASIFIKASI|")) {
			final String label = detailKey.substring("TOP_KLASIFIKASI|".length());
			return criteriaDetailProviderV8(new DashboardDetailCriteriaBuilderV8() {
				public Criteria build(Session session, boolean order) {
					Criteria criteria = createCriteriaSuratKeluarDashboard(session, data.mulai, data.sampai,
							data.satuanKerja, data.keyword, order);
					if ("Tanpa klasifikasi".equalsIgnoreCase(label) || "Tanpa Klasifikasi".equals(label)) {
						criteria.add(Restrictions.isNull("klasifikasiSuratKeluar"));
					} else {
						// V12 FIX: gunakan alias existing dari createCriteriaSuratKeluarDashboard.
						criteria.add(Restrictions.eq("aksesKlasifikasiSuratKeluar.nama", label));
					}
					return criteria;
				}
			});
		}
		if (detailKey.startsWith("TOP_SATKER|")) {
			final String label = detailKey.substring("TOP_SATKER|".length());
			return criteriaDetailProviderV8(new DashboardDetailCriteriaBuilderV8() {
				public Criteria build(Session session, boolean order) {
					Criteria criteria = createCriteriaSuratKeluarDashboard(session, data.mulai, data.sampai,
							data.satuanKerja, data.keyword, order);
					if ("Tanpa Satuan Kerja".equals(label)) {
						criteria.add(Restrictions.isNull("satuanKerja"));
					} else {
						criteria.createAlias("satuanKerja", "satker", Criteria.LEFT_JOIN);
						criteria.add(Restrictions.eq("satker.nama", label));
					}
					return criteria;
				}
			});
		}
		if (detailKey.startsWith("TREND_PENGAJUAN_KELUAR|")) {
			final String label = detailKey.substring("TREND_PENGAJUAN_KELUAR|".length());
			return criteriaDetailProviderV8(new DashboardDetailCriteriaBuilderV8() {
				public Criteria build(Session session, boolean order) throws Exception {
					Criteria criteria = createCriteriaSuratKeluarDashboard(session, data.mulai, data.sampai,
							data.satuanKerja, data.keyword, order);
					Date[] range = parseTrendMonthRangeV8(label);
					if (range != null) {
						criteria.add(Restrictions.ge("tanggal", range[0]));
						criteria.add(Restrictions.lt("tanggal", range[1]));
					}
					return criteria;
				}
			});
		}
		return null;
	}

	private DashboardDetailProviderV8 metadataSuratKeluarProviderV8(final DashboardSuratData data,
			final String propertyName) {
		return criteriaDetailProviderV8(new DashboardDetailCriteriaBuilderV8() {
			public Criteria build(Session session, boolean order) {
				return createCriteriaSuratKeluarDashboard(session, data.mulai, data.sampai, data.satuanKerja,
						data.keyword, order).add(emptyTextCriterion(propertyName));
			}
		});
	}

	private DashboardDetailProviderV8 criteriaDetailProviderV8(final DashboardDetailCriteriaBuilderV8 builder) {
		return new DashboardDetailProviderV8() {
			public long count() throws Exception {
				Session session = HibernateUtil.currentSession();
				return countCriteria(builder.build(session, false));
			}

			@SuppressWarnings("unchecked")
			public List list(int firstResult, int maxResults) throws Exception {
				Session session = HibernateUtil.currentSession();
				return builder.build(session, true).setFirstResult(firstResult).setMaxResults(maxResults).list();
			}
		};
	}

	private DashboardDetailProviderV8 combinedDetailProviderV8(final DashboardDetailProviderV8 a,
			final DashboardDetailProviderV8 b) {
		return combinedDetailProviderV8(new DashboardDetailProviderV8[] { a, b });
	}

	private DashboardDetailProviderV8 combinedDetailProviderV8(final DashboardDetailProviderV8 a,
			final DashboardDetailProviderV8 b, final DashboardDetailProviderV8 c, final DashboardDetailProviderV8 d) {
		return combinedDetailProviderV8(new DashboardDetailProviderV8[] { a, b, c, d });
	}

	private DashboardDetailProviderV8 combinedDetailProviderV8(final DashboardDetailProviderV8[] providers) {
		return new DashboardDetailProviderV8() {
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

			public List list(int firstResult, int maxResults) throws Exception {
				List result = new ArrayList();
				if (providers == null || maxResults <= 0) {
					return result;
				}
				long skip = firstResult;
				int remaining = maxResults;
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
						remaining = maxResults - result.size();
					}
					skip = 0L;
				}
				return result;
			}
		};
	}

	private Date[] parseTrendMonthRangeV8(String label) {
		try {
			java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM yyyy", new java.util.Locale("id", "ID"));
			Date start = sdf.parse(label);
			Calendar cal = Calendar.getInstance();
			cal.setTime(start);
			cal.set(Calendar.DATE, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			start = cal.getTime();
			cal.add(Calendar.MONTH, 1);
			Date end = cal.getTime();
			return new Date[] { start, end };
		} catch (Exception e) {
			debugError("parseTrendMonthRangeV8-" + label, e);
			return null;
		}
	}

	private String resolveDetailTitleV8(String detailKey) {
		if (detailKey == null) {
			return "Detail Data Dashboard";
		}
		if (detailKey.startsWith("TOP_KLASIFIKASI|")) {
			return "Detail Klasifikasi: " + detailKey.substring("TOP_KLASIFIKASI|".length());
		}
		if (detailKey.startsWith("TOP_SATKER|")) {
			return "Detail Satuan Kerja: " + detailKey.substring("TOP_SATKER|".length());
		}
		if (detailKey.startsWith("TREND_PENGAJUAN_KELUAR|")) {
			return "Detail Tren Pengajuan: " + detailKey.substring("TREND_PENGAJUAN_KELUAR|".length());
		}
		if ("DETAIL_PENGAJUAN_KELUAR".equals(detailKey)) {
			return "Detail Pengajuan Surat Keluar";
		}
		if ("DETAIL_MENUNGGU_ALL".equals(detailKey)) {
			return "Detail Menunggu Tindak Lanjut";
		}
		if ("DETAIL_SELESAI_ALL".equals(detailKey)) {
			return "Detail Disetujui / Diproses";
		}
		if ("DETAIL_DITOLAK_ALL".equals(detailKey)) {
			return "Detail Ditolak";
		}
		if ("DETAIL_REVISI_DITOLAK".equals(detailKey)) {
			return "Detail Revisi / Tolak";
		}
		if ("DETAIL_TOTAL_ALUR".equals(detailKey)) {
			return "Detail Total Alur";
		}
		if ("DETAIL_TANPA_KODE".equals(detailKey)) {
			return "Detail Surat Tanpa Kode";
		}
		if ("DETAIL_TANPA_PERIHAL".equals(detailKey)) {
			return "Detail Surat Tanpa Perihal";
		}
		if ("DETAIL_TANPA_SATKER".equals(detailKey)) {
			return "Detail Surat Tanpa Satuan Kerja";
		}
		return "Detail Data Dashboard";
	}

	private String resolveDetailDescriptionV8(String detailKey) {
		return "Data ditampilkan dalam grid paging 10 baris per halaman. Klik angka pada dashboard untuk membuka detail sesuai indikator.";
	}

	private void viewDetail(final String title, final String description, final DashboardDetailProviderV8 provider) {
		try {
			final org.zkoss.zul.Window window = new org.zkoss.zul.Window();
			window.setTitle(title);
			window.setWidth(Common.isMobile() ? "96%" : "86%");
			window.setHeight(Common.isMobile() ? "88%" : "78%");
			window.setClosable(true);
			window.setSizable(true);
			window.setMaximizable(true);
			window.setBorder("normal");
			window.setStyle("border-radius:16px; overflow:hidden;");
			if (DasboardSurat.this.getPage() != null) {
				window.setPage(DasboardSurat.this.getPage());
			} else if (DasboardSurat.this.getParent() != null) {
				window.setParent(DasboardSurat.this.getParent());
			}

			org.zkoss.zul.Div wrapper = new org.zkoss.zul.Div();
			wrapper.setParent(window);
			wrapper.setWidth("100%");
			wrapper.setHeight("100%");
			wrapper.setStyle("box-sizing:border-box; padding:12px; background:#f8fafc; overflow:auto;");

			appendHtml(wrapper, "<div style='padding:12px 14px; margin-bottom:10px; border-radius:14px; "
					+ "background:#ffffff; border:1px solid #e5e7eb; color:#475569; font-size:12px; line-height:1.55;'>"
					+ safeHtml(description)
					+ "<br/><b>V14:</b> Data alur persetujuan/disposisi digrup per parent Surat Masuk/Surat Keluar. "
					+ "Satu baris mewakili satu surat dan menampilkan timeline alur yang sudah dilewati.</div>");

			final DashboardDetailProviderV8 parentGroupedProviderV14 = wrapParentGroupedStatusProviderV14(provider);

			final org.zkoss.zul.Paging paging = new org.zkoss.zul.Paging();
			paging.setPageSize(10);
			paging.setDetailed(true);
			paging.setParent(wrapper);

			final org.zkoss.zul.Grid grid = new org.zkoss.zul.Grid();
			grid.setParent(wrapper);
			grid.setSclass("dgrid fgrid");
			grid.setStyle("margin-top:8px; border:1px solid #e5e7eb; border-radius:14px; overflow:hidden; background:#ffffff;");
			grid.setMold("paging");
			grid.setPageSize(10);

			org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
			columns.setParent(grid);
			addDetailColumnV8(columns, "Tanggal", "115px");
			addDetailColumnV8(columns, "Nomor / Kode", "145px");
			addDetailColumnV8(columns, "Perihal / Nama", "250px");
			addDetailColumnV8(columns, "Rincian Alur Persetujuan", null);
			addDetailColumnV8(columns, "Ringkasan", "190px");

			final org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
			rows.setParent(grid);

			final EventListener refreshDetail = new EventListener() {
				public void onEvent(Event event) throws Exception {
					Common.clear(rows);
					long total = parentGroupedProviderV14 == null ? 0L : parentGroupedProviderV14.count();
					paging.setTotalSize(total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total);

					List dataRows = parentGroupedProviderV14 == null ? new ArrayList()
							: parentGroupedProviderV14.list(paging.getActivePage() * 10, 10);
					if (dataRows == null || dataRows.isEmpty()) {
						Row row = new Row();
						row.setParent(rows);
						org.zkoss.zul.Label empty = new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig("Tidak ada data untuk indikator ini."));
						empty.setStyle("padding:14px; color:#64748b; font-size:12px;");
						empty.setParent(row);
						return;
					}

					for (int i = 0; i < dataRows.size(); i++) {
						Row row = new Row();
						row.setParent(rows);
						renderDetailRowV8(row, dataRows.get(i));
					}
				}
			};

			paging.addEventListener("onPaging", refreshDetail);
			refreshDetail.onEvent(null);
			window.doModal();
		} catch (Exception e) {
			debugError("viewDetail-" + title, e);
		}
	}

	private void addDetailColumnV8(org.zkoss.zul.Columns columns, String label, String width) {
		org.zkoss.zul.Column column = new org.zkoss.zul.Column(label);
		if (width != null) {
			column.setWidth(width);
		}
		column.setStyle("font-size:12px; font-weight:800; color:#334155;");
		column.setParent(columns);
	}


	private DashboardDetailProviderV8 wrapParentGroupedStatusProviderV14(final DashboardDetailProviderV8 baseProvider) {
		return new DashboardDetailProviderV8() {
			private List collectGroups(int first, int max, boolean countOnly) throws Exception {
				List result = new ArrayList();
				if (baseProvider == null) {
					return result;
				}

				Map grouped = new LinkedHashMap();
				List orderedGroups = new ArrayList();
				int offset = 0;
				int batchSize = 250;
				int scanLimit = 20000;

				while (offset < scanLimit) {
					List rows = baseProvider.list(offset, batchSize);
					if (rows == null || rows.isEmpty()) {
						break;
					}

					for (int i = 0; i < rows.size(); i++) {
						Object row = rows.get(i);
						if (row instanceof AlurPersetujuanSuratKeluarStatus
								|| row instanceof AlurPersetujuanSuratMasukStatus) {
							String key = getParentGroupKeyV14(row);
							if (!grouped.containsKey(key)) {
								DashboardAlurParentGroupV14 group = buildParentGroupV14(row);
								grouped.put(key, group);
								orderedGroups.add(group);
							}
						} else {
							orderedGroups.add(row);
						}
					}

					if (rows.size() < batchSize) {
						break;
					}
					offset += batchSize;
				}

				if (countOnly) {
					result.add(Long.valueOf(orderedGroups.size()));
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
				List data = collectGroups(0, 0, true);
				return data == null || data.isEmpty() ? 0L : ((Number) data.get(0)).longValue();
			}

			public List list(int firstResult, int maxResults) throws Exception {
				return collectGroups(firstResult, maxResults, false);
			}
		};
	}

	private String getParentGroupKeyV14(Object status) {
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
			debugError("getParentGroupKeyV14", e);
		}
		return "UNKNOWN-" + String.valueOf(System.identityHashCode(status));
	}

	private DashboardAlurParentGroupV14 buildParentGroupV14(Object status) {
		DashboardAlurParentGroupV14 group = new DashboardAlurParentGroupV14();
		try {
			if (status instanceof AlurPersetujuanSuratKeluarStatus) {
				SuratKeluar surat = ((AlurPersetujuanSuratKeluarStatus) status).getSuratKeluar();
				group.jenis = "Surat Keluar";
				group.tanggal = formatTanggalDasbor(surat == null ? null : surat.getTanggal());
				group.kode = surat == null ? "-" : firstNotEmpty(surat.getKode(), surat.getNama(), "-");
				group.perihal = surat == null ? "-" : safeText(surat.getPerihal());
				group.parameterHtml = buildKlasifikasiSuratKeluarParameterCompactHtmlV15(surat);
				List alurs = loadAlurKeluarBySuratV14(surat);
				group.timelineHtml = buildAlurTimelineHtmlV14(alurs);
				group.ringkasanHtml = buildAlurRingkasanHtmlV14(alurs);
			} else if (status instanceof AlurPersetujuanSuratMasukStatus) {
				SuratMasuk surat = ((AlurPersetujuanSuratMasukStatus) status).getSuratMasuk();
				group.jenis = "Surat Masuk";
				group.tanggal = formatTanggalDasbor(surat == null ? null : surat.getTanggal());
				group.kode = surat == null ? "-" : firstNotEmpty(surat.getNama(), surat.getKode(), "-");
				group.perihal = surat == null ? "-" : safeText(surat.getPerihal());
				group.parameterHtml = "";
				List alurs = loadAlurMasukBySuratV14(surat);
				group.timelineHtml = buildAlurTimelineHtmlV14(alurs);
				group.ringkasanHtml = buildAlurRingkasanHtmlV14(alurs);
			}
		} catch (Exception e) {
			debugError("buildParentGroupV14", e);
			group.parameterHtml = "";
			group.timelineHtml = "<div style='font-size:11px;color:#991b1b;'>Gagal memuat rincian alur.</div>";
			group.ringkasanHtml = "-";
		}
		return group;
	}

	@SuppressWarnings("unchecked")
	private List loadAlurKeluarBySuratV14(SuratKeluar suratKeluar) {
		if (suratKeluar == null || suratKeluar.getId() == null) {
			return new ArrayList();
		}
		return HibernateUtil.currentSession().createCriteria(AlurPersetujuanSuratKeluarStatus.class)
				.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("suratKeluar", suratKeluar))
				.addOrder(Order.asc("id")).list();
	}

	@SuppressWarnings("unchecked")
	private List loadAlurMasukBySuratV14(SuratMasuk suratMasuk) {
		if (suratMasuk == null || suratMasuk.getId() == null) {
			return new ArrayList();
		}
		return HibernateUtil.currentSession().createCriteria(AlurPersetujuanSuratMasukStatus.class)
				.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("suratMasuk", suratMasuk))
				.addOrder(Order.asc("id")).list();
	}

	private String buildAlurTimelineHtmlV14(List alurs) {
		if (alurs == null || alurs.isEmpty()) {
			return "<div style='padding:6px 9px; border-radius:999px; background:#f8fafc; border:1px dashed #cbd5e1; color:#64748b; font-size:10px; font-weight:800; display:inline-block;'>Belum ada data alur</div>";
		}
		/*
		 * V18:
		 * Timeline legacy dan popup dibuat simpel: node kecil + panah.
		 * Ini menjaga tampilan Persetujuan Surat, Disposisi Surat Masuk, dan
		 * Pengajuan Surat Anda tidak terlalu tinggi.
		 */
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:flex; align-items:center; gap:5px; flex-wrap:wrap; line-height:1.1; padding:1px 0;'>");
		int no = 1;
		int tampil = 0;
		String prevLabel = null;
		Date prevWaktu = null;
		for (int i = 0; i < alurs.size(); i++) {
			Object alur = alurs.get(i);
			if (!isAlurSudahDilewatiV14(alur) && !isAlurMenungguTargetLoginV20(alur)) {
				continue;
			}
			if (tampil > 0) {
				sb.append("<span style='color:#94a3b8; font-size:13px; font-weight:900;'>&rarr;</span>");
			}
			tampil++;
			String color = getAlurStatusColorV14(alur);
			String status = getAlurStatusTextV14(alur);
			String waktu = getAlurWaktuV14(alur);
			String konseptor = getAlurKonseptorRingkasanV14(alur);
			Date waktuDisposisi = getAlurWaktuDateV14(alur);
			String title = safeHtml(getAlurLabelV14(alur) + " | " + getAlurActorV14(alur) + " | " + status
					+ (waktu == null || waktu.trim().isEmpty() ? "" : " | " + waktu)
					+ (konseptor == null || konseptor.trim().isEmpty() ? ""
							: " | Jabatan yang mendisposisikan : " + konseptor)
					+ (konseptor == null || konseptor.trim().isEmpty() || waktuDisposisi == null ? ""
							: " | Tanggal & Waktu : " + Common.dateFormat3.get().format(waktuDisposisi)));
			sb.append("<span title='").append(title)
					.append("' style='display:inline-flex; align-items:center; gap:5px; max-width:250px; padding:4px 8px; border-radius:999px; background:")
					.append(getAlurStatusBgV14(alur)).append("; border:1px solid #e2e8f0; color:#0f172a; font-size:10px; font-weight:900; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;'>")
					.append("<span style='min-width:17px; height:17px; border-radius:999px; display:inline-flex; align-items:center; justify-content:center; background:")
					.append(color).append("; color:#ffffff; font-size:9px; font-weight:900;'>").append(no)
					.append("</span>")
					.append("<span style='overflow:hidden; text-overflow:ellipsis; white-space:nowrap;'>")
					.append(safeHtml(getAlurLabelV14(alur))).append("</span>")
					.append("<span style='font-size:9px; color:").append(color)
					.append("; background:#ffffff; border:1px solid rgba(15,23,42,.06); border-radius:999px; padding:1px 5px;'>")
					.append(safeHtml(status)).append("</span>")
					.append("</span>");
			no++;
			prevLabel = getAlurLabelV14(alur);
			prevWaktu = getAlurWaktuDateV14(alur);
		}
		if (tampil <= 0) {
			sb.append("<span style='padding:6px 9px; border-radius:999px; background:#fff7ed; border:1px solid #fed7aa; color:#9a3412; font-size:10px; font-weight:900;'>Belum ada alur persetujuan yang perlu ditampilkan</span>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private boolean isAlurSudahDilewatiV14(Object alur) {
		try {
			if (alur instanceof AlurPersetujuanSuratKeluarStatus) {
				AlurPersetujuanSuratKeluarStatus s = (AlurPersetujuanSuratKeluarStatus) alur;
				return Boolean.TRUE.equals(s.getDisetujui()) || Boolean.TRUE.equals(s.getDitolak())
						|| Boolean.TRUE.equals(s.getTelahDirevisi()) || Boolean.TRUE.equals(s.getSelesai())
						|| s.getKonseptor() != null || s.getMahasiswa() != null || s.getSiswa() != null;
			}
			if (alur instanceof AlurPersetujuanSuratMasukStatus) {
				AlurPersetujuanSuratMasukStatus s = (AlurPersetujuanSuratMasukStatus) alur;
				return Boolean.TRUE.equals(s.getDisetujui()) || Boolean.TRUE.equals(s.getDitolak())
						|| Boolean.TRUE.equals(s.getTelahDirevisi()) || s.getKonseptor() != null
						|| s.getMahasiswa() != null || s.getSiswa() != null;
			}
		} catch (Exception e) {
			debugError("isAlurSudahDilewatiV14", e);
		}
		return false;
	}

	private boolean isAlurMenungguTargetLoginV20(Object alur) {
		try {
			if (alur instanceof AlurPersetujuanSuratKeluarStatus) {
				return bolehTindakLanjutAlurKeluarV20((AlurPersetujuanSuratKeluarStatus) alur);
			}
			if (alur instanceof AlurPersetujuanSuratMasukStatus) {
				return bolehTindakLanjutAlurMasukV20((AlurPersetujuanSuratMasukStatus) alur);
			}
		} catch (Exception e) {
			debugError("isAlurMenungguTargetLoginV20", e);
		}
		return false;
	}

	private String getAlurLabelV14(Object alur) {
		try {
			if (alur instanceof AlurPersetujuanSuratKeluarStatus) {
				AlurPersetujuanSuratKeluarStatus s = (AlurPersetujuanSuratKeluarStatus) alur;
				if (s.getJenisJabatan() != null) {
					return s.getJenisJabatan().getNama();
				}
				if (s.getAlurPersetujuanSuratKeluar() != null) {
					return String.valueOf(s.getAlurPersetujuanSuratKeluar());
				}
				if (s.getPejabat() != null) {
					return s.getPejabat().getNama();
				}
			} else if (alur instanceof AlurPersetujuanSuratMasukStatus) {
				AlurPersetujuanSuratMasukStatus s = (AlurPersetujuanSuratMasukStatus) alur;
				if (s.getJenisJabatan() != null) {
					return s.getJenisJabatan().getNama();
				}
				if (s.getAlurPersetujuanSuratMasuk() != null) {
					return String.valueOf(s.getAlurPersetujuanSuratMasuk());
				}
				if (s.getPejabat() != null) {
					return s.getPejabat().getNama();
				}
			}
		} catch (Exception e) {
			debugError("getAlurLabelV14", e);
		}
		return "Alur";
	}

	private String getAlurActorV14(Object alur) {
		try {
			if (alur instanceof AlurPersetujuanSuratKeluarStatus) {
				AlurPersetujuanSuratKeluarStatus s = (AlurPersetujuanSuratKeluarStatus) alur;
				return getPelakuAlurV14(s.getKonseptor(), s.getMahasiswa(), s.getSiswa(), s.getPejabat());
			}
			if (alur instanceof AlurPersetujuanSuratMasukStatus) {
				AlurPersetujuanSuratMasukStatus s = (AlurPersetujuanSuratMasukStatus) alur;
				return getPelakuAlurV14(s.getKonseptor(), s.getMahasiswa(), s.getSiswa(), s.getPejabat());
			}
		} catch (Exception e) {
			debugError("getAlurActorV14", e);
		}
		return "Pelaku belum terbaca";
	}

	private String getAlurKonseptorJabatanV14(Object alur) {
		try {
			Tbmuser konseptor = null;
			if (alur instanceof AlurPersetujuanSuratKeluarStatus) {
				konseptor = ((AlurPersetujuanSuratKeluarStatus) alur).getKonseptor();
			} else if (alur instanceof AlurPersetujuanSuratMasukStatus) {
				konseptor = ((AlurPersetujuanSuratMasukStatus) alur).getKonseptor();
			}
			JenisJabatan jenisJabatan = getJenisJabatanKonseptorV14(konseptor);
			if (jenisJabatan != null && jenisJabatan.getNama() != null && jenisJabatan.getNama().trim().length() > 0) {
				return jenisJabatan.getNama();
			}
		} catch (Exception e) {
			debugError("getAlurKonseptorJabatanV14", e);
		}
		return "";
	}

	private String getAlurKonseptorRingkasanV14(Object alur) {
		try {
			Tbmuser konseptor = null;
			if (alur instanceof AlurPersetujuanSuratKeluarStatus) {
				konseptor = ((AlurPersetujuanSuratKeluarStatus) alur).getKonseptor();
			} else if (alur instanceof AlurPersetujuanSuratMasukStatus) {
				konseptor = ((AlurPersetujuanSuratMasukStatus) alur).getKonseptor();
			}
			if (konseptor != null) {
				String ringkasan = String.valueOf(konseptor);
				return bersihkanKeteranganKurungKonseptorV14(ringkasan);
			}
		} catch (Exception e) {
			debugError("getAlurKonseptorRingkasanV14", e);
		}
		return "";
	}

	private String bersihkanKeteranganKurungKonseptorV14(String ringkasan) {
		if (ringkasan == null || "null".equalsIgnoreCase(ringkasan.trim())) {
			return "";
		}
		return ringkasan.trim().replaceAll("\\s*\\([^)]*\\)\\s*$", "").trim();
	}

	private JenisJabatan getJenisJabatanKonseptorV14(Tbmuser konseptor) {
		if (konseptor == null) {
			return null;
		}
		try {
			Tbmrole role = konseptor.hakAkses();
			if (role != null && role.getJenisJabatan() != null) {
				return role.getJenisJabatan();
			}
		} catch (Exception e) {
			debugError("getJenisJabatanKonseptorV14-role", e);
		}
		try {
			List<Pejabat> pejabats = ConstantValues.simpleList(
					HibernateUtil.currentSession().createCriteria(Pejabat.class)
							.add(Restrictions.or(
									Restrictions.ilike("usernamePengguna", "," + konseptor.getUserId() + ",",
											MatchMode.ANYWHERE),
									Restrictions.or(Restrictions.eq("pegawai", konseptor.getPegawai()),
											Restrictions.or(Restrictions.eq("dosen", konseptor.getDosen()),
													Restrictions.eq("guru", konseptor.getGuru())))))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setMaxResults(1),
					Pejabat.class);
			if (!pejabats.isEmpty() && pejabats.get(0).getJenisJabatan() != null) {
				return pejabats.get(0).getJenisJabatan();
			}
		} catch (Exception e) {
			debugError("getJenisJabatanKonseptorV14-pejabat", e);
		}
		return null;
	}

	private String getPelakuAlurV14(Tbmuser konseptor, Object mahasiswa, Object siswa, Pejabat pejabat) {
		try {
			if (konseptor != null) {
				return "Oleh " + firstNotEmpty(konseptor.getUserNama(), konseptor.getUserId(), "Konseptor");
			}
			if (mahasiswa != null) {
				try {
					return "Oleh " + String.valueOf(mahasiswa.getClass().getMethod("getNama").invoke(mahasiswa));
				} catch (Exception e) {
					return "Oleh " + String.valueOf(mahasiswa);
				}
			}
			if (siswa != null) {
				try {
					return "Oleh " + String.valueOf(siswa.getClass().getMethod("getNama").invoke(siswa));
				} catch (Exception e) {
					return "Oleh " + String.valueOf(siswa);
				}
			}
			if (pejabat != null) {
				return "Target " + pejabat.getNama();
			}
		} catch (Exception e) {
			debugError("getPelakuAlurV14", e);
		}
		return "Belum ada pelaku";
	}

	private String getAlurStatusTextV14(Object alur) {
		try {
			if (alur instanceof AlurPersetujuanSuratKeluarStatus) {
				AlurPersetujuanSuratKeluarStatus s = (AlurPersetujuanSuratKeluarStatus) alur;
				if (Boolean.TRUE.equals(s.getDitolak())) {
					return "Ditolak";
				}
				if (Boolean.TRUE.equals(s.getTelahDirevisi())) {
					return "Revisi";
				}
				if (Boolean.TRUE.equals(s.getDisetujui())) {
					return "Disetujui";
				}
				if (Boolean.TRUE.equals(s.getSelesai())) {
					return "Selesai";
				}
			} else if (alur instanceof AlurPersetujuanSuratMasukStatus) {
				AlurPersetujuanSuratMasukStatus s = (AlurPersetujuanSuratMasukStatus) alur;
				if (Boolean.TRUE.equals(s.getDitolak())) {
					return "Ditolak";
				}
				if (Boolean.TRUE.equals(s.getTelahDirevisi())) {
					return "Revisi";
				}
				if (Boolean.TRUE.equals(s.getDisetujui())) {
					return "Disetujui";
				}
			}
		} catch (Exception e) {
			debugError("getAlurStatusTextV14", e);
		}
		return "Menunggu Persetujuan";
	}

	private String getAlurStatusColorV14(Object alur) {
		String status = getAlurStatusTextV14(alur);
		if ("Ditolak".equals(status)) {
			return "#dc2626";
		}
		if ("Revisi".equals(status)) {
			return "#2563eb";
		}
		if ("Disetujui".equals(status) || "Selesai".equals(status)) {
			return "#16a34a";
		}
		if ("Menunggu Persetujuan".equals(status)) {
			return "#f59e0b";
		}
		return "#64748b";
	}

	private String getAlurStatusBgV14(Object alur) {
		String status = getAlurStatusTextV14(alur);
		if ("Ditolak".equals(status)) {
			return "#fee2e2";
		}
		if ("Revisi".equals(status)) {
			return "#dbeafe";
		}
		if ("Disetujui".equals(status) || "Selesai".equals(status)) {
			return "#dcfce7";
		}
		if ("Menunggu Persetujuan".equals(status)) {
			return "#fef3c7";
		}
		return "#f1f5f9";
	}

	private String getAlurWaktuV14(Object alur) {
		try {
			Date waktu = getAlurWaktuDateV14(alur);
			return waktu == null ? "" : Common.dateFormat3.get().format(waktu);
		} catch (Exception e) {
			return "";
		}
	}

	private Date getAlurWaktuDateV14(Object alur) {
		try {
			if (alur instanceof AlurPersetujuanSuratKeluarStatus) {
				AlurPersetujuanSuratKeluarStatus s = (AlurPersetujuanSuratKeluarStatus) alur;
				if (Boolean.TRUE.equals(s.getDisetujui()) && s.getWaktuPersetujuan() != null) {
					return s.getWaktuPersetujuan();
				}
				if (Boolean.TRUE.equals(s.getDitolak()) && s.getWaktuDitolak() != null) {
					return s.getWaktuDitolak();
				}
				return s.getTanggal_dirubah();
			} else if (alur instanceof AlurPersetujuanSuratMasukStatus) {
				AlurPersetujuanSuratMasukStatus s = (AlurPersetujuanSuratMasukStatus) alur;
				if (Boolean.TRUE.equals(s.getDisetujui()) && s.getWaktuPersetujuan() != null) {
					return s.getWaktuPersetujuan();
				}
				if (Boolean.TRUE.equals(s.getDitolak()) && s.getWaktuDitolak() != null) {
					return s.getWaktuDitolak();
				}
				return s.getTanggal_dirubah();
			}
		} catch (Exception e) {
			debugError("getAlurWaktuDateV14", e);
		}
		return null;
	}

	private String buildAlurRingkasanHtmlV14(List alurs) {
		long total = alurs == null ? 0L : alurs.size();
		long sudahDiproses = 0L;
		long menungguPersetujuan = 0L;
		if (alurs != null) {
			for (int i = 0; i < alurs.size(); i++) {
				Object alur = alurs.get(i);
				if (isPendingAlurObjectV20(alur)) {
					menungguPersetujuan++;
				} else if (isAlurSudahDilewatiV14(alur)) {
					sudahDiproses++;
				}
			}
		}
		return "<div style='display:flex; flex-direction:column; gap:5px;'>"
				+ ringkasanBadgeV14("Total", total, "#e0f2fe", "#075985")
				+ ringkasanBadgeV14("Sudah Diproses", sudahDiproses, "#dcfce7", "#166534")
				+ ringkasanBadgeV14("Menunggu Persetujuan", menungguPersetujuan, "#fef3c7", "#92400e") + "</div>";
	}

	private boolean isPendingAlurObjectV20(Object alur) {
		if (alur instanceof AlurPersetujuanSuratKeluarStatus) {
			return isPendingAlurKeluarV20((AlurPersetujuanSuratKeluarStatus) alur);
		}
		if (alur instanceof AlurPersetujuanSuratMasukStatus) {
			return isPendingAlurMasukV20((AlurPersetujuanSuratMasukStatus) alur);
		}
		return false;
	}

	private String ringkasanBadgeV14(String label, long value, String bg, String color) {
		return "<div style='display:flex; justify-content:space-between; gap:8px; align-items:center; border-radius:999px; padding:4px 8px; background:"
				+ bg + "; color:" + color + "; font-size:10px; font-weight:900;'>"
				+ "<span>" + safeHtml(label) + "</span><span>" + value + "</span></div>";
	}


	private String buildPerihalDanParameterHtmlV15(String perihal, String parameterHtml) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:11px; color:#334155; line-height:1.35; font-weight:700;'>")
				.append(safeHtml(perihal == null || perihal.trim().isEmpty() ? "-" : perihal)).append("</div>");
		if (parameterHtml != null && !parameterHtml.trim().isEmpty()) {
			sb.append(parameterHtml);
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private String buildKlasifikasiSuratKeluarParameterCompactHtmlV15(SuratKeluar suratKeluar) {
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
			debugError("buildKlasifikasiSuratKeluarParameterCompactHtmlV15", e);
			return "";
		}
	}

	private void appendKlasifikasiSuratKeluarParameterCompactV15(Vbox parent, SuratKeluar suratKeluar) {
		try {
			String html = buildKlasifikasiSuratKeluarParameterCompactHtmlV15(suratKeluar);
			if (html != null && !html.trim().isEmpty()) {
				Html h = new Html(html);
				h.setStyle("display:block; margin-top:2px;");
				h.setParent(parent);
			}
		} catch (Exception e) {
			debugError("appendKlasifikasiSuratKeluarParameterCompactV15", e);
		}
	}


	private void renderParentGroupDetailRowV14(Row row, DashboardAlurParentGroupV14 group) {
		row.setStyle("vertical-align:top;");
		addDetailCellV8(row, group.tanggal);
		addDetailCellV8(row, group.kode);
		addDetailHtmlCellV14(row, buildPerihalDanParameterHtmlV15(group.perihal, group.parameterHtml));
		addDetailHtmlCellV14(row, group.timelineHtml);
		addDetailHtmlCellV14(row, group.ringkasanHtml);
	}

	private void addDetailHtmlCellV14(Row row, String html) {
		Html h = new Html(html == null ? "" : html);
		h.setStyle("display:block; white-space:normal; vertical-align:top; line-height:1.2;");
		h.setParent(row);
	}

	private AlurPersetujuanSuratKeluarStatus pilihAlurKeluarUntukTindakLanjutV20(AlurPersetujuanSuratKeluarStatus fallback) {
		try {
			if (fallback == null || fallback.getSuratKeluar() == null || fallback.getSuratKeluar().getId() == null) {
				return fallback;
			}
			List alurs = loadAlurKeluarBySuratV14(fallback.getSuratKeluar());
			for (int i = 0; alurs != null && i < alurs.size(); i++) {
				Object row = alurs.get(i);
				if (row instanceof AlurPersetujuanSuratKeluarStatus) {
					AlurPersetujuanSuratKeluarStatus status = (AlurPersetujuanSuratKeluarStatus) row;
					if (bolehTindakLanjutAlurKeluarV20(status)) {
						return status;
					}
				}
			}
		} catch (Exception e) {
			debugError("pilihAlurKeluarUntukTindakLanjutV20", e);
		}
		return fallback;
	}

	private AlurPersetujuanSuratMasukStatus pilihAlurMasukUntukTindakLanjutV20(AlurPersetujuanSuratMasukStatus fallback) {
		try {
			if (fallback == null || fallback.getSuratMasuk() == null || fallback.getSuratMasuk().getId() == null) {
				return fallback;
			}
			List alurs = loadAlurMasukBySuratV14(fallback.getSuratMasuk());
			for (int i = 0; alurs != null && i < alurs.size(); i++) {
				Object row = alurs.get(i);
				if (row instanceof AlurPersetujuanSuratMasukStatus) {
					AlurPersetujuanSuratMasukStatus status = (AlurPersetujuanSuratMasukStatus) row;
					if (bolehTindakLanjutAlurMasukV20(status)) {
						return status;
					}
				}
			}
		} catch (Exception e) {
			debugError("pilihAlurMasukUntukTindakLanjutV20", e);
		}
		return fallback;
	}

	private boolean isPendingAlurKeluarV20(AlurPersetujuanSuratKeluarStatus status) {
		return status != null && !Boolean.TRUE.equals(status.getDisetujui()) && !Boolean.TRUE.equals(status.getDitolak());
	}

	private boolean isPendingAlurMasukV20(AlurPersetujuanSuratMasukStatus status) {
		return status != null && !Boolean.TRUE.equals(status.getDisetujui()) && !Boolean.TRUE.equals(status.getDitolak());
	}

	private boolean bolehTindakLanjutAlurKeluarV20(AlurPersetujuanSuratKeluarStatus status) {
		return isPendingAlurKeluarV20(status) && bolehAksesAlurBerdasarkanLoginV20(status == null ? null : status.getPejabat(),
				status == null ? null : status.getJenisJabatan());
	}

	private boolean bolehTindakLanjutAlurMasukV20(AlurPersetujuanSuratMasukStatus status) {
		return isPendingAlurMasukV20(status) && bolehAksesAlurBerdasarkanLoginV20(status == null ? null : status.getPejabat(),
				status == null ? null : status.getJenisJabatan());
	}

	private boolean bolehAksesAlurBerdasarkanLoginV20(Pejabat pejabatTarget, JenisJabatan jenisJabatanTarget) {
		try {
			if (Common.getApakahAdmin()) {
				return true;
			}
			if (pejabatTarget != null && pejabatTarget.getId() != null) {
				List<Pejabat> pejabats = Common.getCurrentPejabat(false);
				for (int i = 0; pejabats != null && i < pejabats.size(); i++) {
					Pejabat pejabat = pejabats.get(i);
					if (pejabat != null && pejabat.getId() != null && pejabat.getId().equals(pejabatTarget.getId())) {
						return true;
					}
					if (jenisJabatanTarget != null && jenisJabatanTarget.getId() != null && pejabat != null
							&& pejabat.getJenisJabatan() != null
							&& jenisJabatanTarget.getId().equals(pejabat.getJenisJabatan().getId())) {
						return true;
					}
				}
			}
			if (jenisJabatanTarget != null && jenisJabatanTarget.getId() != null) {
				List<Pejabat> pejabats = Common.getCurrentPejabat(false);
				for (int i = 0; pejabats != null && i < pejabats.size(); i++) {
					Pejabat pejabat = pejabats.get(i);
					if (pejabat != null && pejabat.getJenisJabatan() != null
							&& jenisJabatanTarget.getId().equals(pejabat.getJenisJabatan().getId())) {
						return true;
					}
				}
			}
			if (pejabatTarget != null && tbmuser != null) {
				String userId = tbmuser.getUserId() == null ? "" : tbmuser.getUserId().trim();
				String roleId = tbmuser.hakAkses() == null || tbmuser.hakAkses().getRoleId() == null ? ""
						: tbmuser.hakAkses().getRoleId().trim();
				String usernamePengguna = pejabatTarget.getUsernamePengguna();
				String jenisPengguna = pejabatTarget.getJenisPengguna();
				if (userId.length() > 0 && containsTokenV20(usernamePengguna, userId)) {
					return true;
				}
				if (roleId.length() > 0 && containsTokenV20(jenisPengguna, roleId)) {
					return true;
				}
			}
		} catch (Exception e) {
			debugError("bolehAksesAlurBerdasarkanLoginV20", e);
		}
		return false;
	}

	private boolean containsTokenV20(String csv, String token) {
		if (csv == null || token == null || token.trim().length() == 0) {
			return false;
		}
		String value = "," + csv.trim() + ",";
		value = value.replaceAll(",,", ",").replaceAll(",,", ",");
		return value.indexOf("," + token.trim() + ",") >= 0;
	}

	private List<AlurPersetujuanSuratKeluarStatus> groupAlurKeluarByParentV14(List<AlurPersetujuanSuratKeluarStatus> input) {
		LinkedHashMap<Long, AlurPersetujuanSuratKeluarStatus> map = new LinkedHashMap<Long, AlurPersetujuanSuratKeluarStatus>();
		if (input != null) {
			for (AlurPersetujuanSuratKeluarStatus row : input) {
				if (row == null || row.getSuratKeluar() == null || row.getSuratKeluar().getId() == null) {
					continue;
				}
				if (!map.containsKey(row.getSuratKeluar().getId())) {
					map.put(row.getSuratKeluar().getId(), row);
				}
			}
		}
		return new ArrayList<AlurPersetujuanSuratKeluarStatus>(map.values());
	}

	private List<AlurPersetujuanSuratMasukStatus> groupAlurMasukByParentV14(List<AlurPersetujuanSuratMasukStatus> input) {
		LinkedHashMap<Long, AlurPersetujuanSuratMasukStatus> map = new LinkedHashMap<Long, AlurPersetujuanSuratMasukStatus>();
		if (input != null) {
			for (AlurPersetujuanSuratMasukStatus row : input) {
				if (row == null || row.getSuratMasuk() == null || row.getSuratMasuk().getId() == null) {
					continue;
				}
				if (!map.containsKey(row.getSuratMasuk().getId())) {
					map.put(row.getSuratMasuk().getId(), row);
				}
			}
		}
		return new ArrayList<AlurPersetujuanSuratMasukStatus>(map.values());
	}


	private void renderDetailRowV8(Row row, Object item) {
		try {
			if (item instanceof DashboardAlurParentGroupV14) {
				renderParentGroupDetailRowV14(row, (DashboardAlurParentGroupV14) item);
			} else if (item instanceof SuratKeluar) {
				renderSuratKeluarDetailRowV8(row, (SuratKeluar) item);
			} else if (item instanceof AlurPersetujuanSuratKeluarStatus) {
				renderPersetujuanKeluarDetailRowV8(row, (AlurPersetujuanSuratKeluarStatus) item);
			} else if (item instanceof AlurPersetujuanSuratMasukStatus) {
				renderPersetujuanMasukDetailRowV8(row, (AlurPersetujuanSuratMasukStatus) item);
			} else if (item instanceof DashboardItem) {
				renderDashboardItemDetailRowV8(row, (DashboardItem) item);
			} else {
				addDetailCellV8(row, "-");
				addDetailCellV8(row, "-");
				addDetailCellV8(row, item == null ? "-" : safeText(String.valueOf(item)));
				addDetailCellV8(row, "-");
				addDetailCellV8(row, "-");
			}
		} catch (Exception e) {
			debugError("renderDetailRowV8", e);
			addDetailCellV8(row, "-");
			addDetailCellV8(row, "-");
			addDetailCellV8(row, "Data tidak dapat ditampilkan.");
			addDetailCellV8(row, "Error");
			addDetailCellV8(row, e == null ? "" : safeText(e.getMessage()));
		}
	}

	private void renderSuratKeluarDetailRowV8(Row row, SuratKeluar surat) {
		addDetailCellV8(row, formatTanggalDasbor(surat == null ? null : surat.getTanggal()));
		addDetailCellV8(row, surat == null ? "-" : firstNotEmpty(surat.getKode(), surat.getNama(), "-"));
		addDetailCellV8(row, surat == null ? "-" : safeText(surat.getPerihal()));
		String klasifikasi = "-";
		try {
			klasifikasi = surat != null && surat.getKlasifikasiSuratKeluar() != null
					? safeText(surat.getKlasifikasiSuratKeluar().getNama()) : "-";
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:4165");
		}
		addDetailCellV8(row, klasifikasi);
		String satker = "-";
		try {
			satker = surat != null && surat.getSatuanKerja() != null ? safeText(surat.getSatuanKerja().getNama()) : "-";
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:4171");
		}
		addDetailCellV8(row, satker);
	}

	private void renderPersetujuanKeluarDetailRowV8(Row row, AlurPersetujuanSuratKeluarStatus status) {
		SuratKeluar surat = status == null ? null : status.getSuratKeluar();
		addDetailCellV8(row, formatTanggalDasbor(surat == null ? null : surat.getTanggal()));
		addDetailCellV8(row, surat == null ? "-" : firstNotEmpty(surat.getKode(), surat.getNama(), "-"));
		addDetailCellV8(row, surat == null ? "-" : safeText(surat.getPerihal()));
		addDetailCellV8(row, getTargetPersetujuanKeluar(status));
		addDetailCellV8(row, statusTextKeluarV8(status));
	}

	private void renderPersetujuanMasukDetailRowV8(Row row, AlurPersetujuanSuratMasukStatus status) {
		SuratMasuk surat = status == null ? null : status.getSuratMasuk();
		addDetailCellV8(row, formatTanggalDasbor(surat == null ? null : surat.getTanggal()));
		addDetailCellV8(row, surat == null ? "-" : firstNotEmpty(surat.getNama(), surat.getKode(), "-"));
		addDetailCellV8(row, surat == null ? "-" : safeText(surat.getPerihal()));
		addDetailCellV8(row, getTargetPersetujuanMasuk(status));
		addDetailCellV8(row, statusTextMasukV8(status));
	}

	private void renderDashboardItemDetailRowV8(Row row, DashboardItem item) {
		addDetailCellV8(row, "-");
		addDetailCellV8(row, item == null ? "-" : safeText(item.title));
		addDetailCellV8(row, item == null ? "-" : safeText(item.description));
		addDetailCellV8(row, item == null ? "-" : safeText(item.status));
		addDetailCellV8(row, item == null ? "-" : safeText(item.meta));
	}

	private String statusTextKeluarV8(AlurPersetujuanSuratKeluarStatus status) {
		if (status == null) {
			return "-";
		}
		try {
			if (Boolean.TRUE.equals(status.getDitolak())) {
				return "Ditolak" + (status.getKeterangan() == null ? "" : " - " + safeText(status.getKeterangan()));
			}
			if (Boolean.TRUE.equals(status.getTelahDirevisi())) {
				return "Revisi";
			}
			if (Boolean.TRUE.equals(status.getDisetujui())) {
				return "Disetujui";
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:4216");
		}
		return "Menunggu";
	}

	private String statusTextMasukV8(AlurPersetujuanSuratMasukStatus status) {
		if (status == null) {
			return "-";
		}
		try {
			if (Boolean.TRUE.equals(status.getDitolak())) {
				return "Ditolak" + (status.getKeterangan() == null ? "" : " - " + safeText(status.getKeterangan()));
			}
			if (Boolean.TRUE.equals(status.getTelahDirevisi())) {
				return "Revisi";
			}
			if (Boolean.TRUE.equals(status.getDisetujui())) {
				return "Disetujui";
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:4235");
		}
		return "Menunggu";
	}

	private void addDetailCellV8(Row row, String value) {
		org.zkoss.zul.Label label = new org.zkoss.zul.Label(value == null || value.trim().isEmpty() ? "-" : value);
		label.setStyle("font-size:12px; color:#334155; white-space:normal; vertical-align:top; line-height:1.25;");
		label.setParent(row);
	}

	private void renderDasborSuratOverview(org.zkoss.zul.Div parent, DashboardSuratData data) {
		long totalAntrian = data.totalMenungguKeluar + data.totalMenungguMasuk;
		long totalSelesai = data.totalDisetujuiKeluar + data.totalDisetujuiMasuk;
		long totalDitolak = data.totalDitolakKeluar + data.totalDitolakMasuk;
		long totalRevisi = data.totalRevisiKeluar + data.totalRevisiMasuk;
		long totalWorkflow = data.totalPersetujuanKeluar + data.totalPersetujuanMasuk;
		String rasioSelesai = formatPercent(totalSelesai, totalWorkflow);

		appendHtml(parent, sectionIntroHtml("Overview Persuratan",
				"Card overview ini memberikan gambaran cepat volume pengajuan, jumlah antrian, tingkat penyelesaian, "
						+ "penolakan, revisi, dan total alur persetujuan/disposisi pada periode filter."));

		String html = "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px; margin-top:14px;'>"
				+ metricCardHtml("Pengajuan Surat", data.totalPengajuanKeluar, "Surat keluar sesuai hak akses", "#2563eb",
						"✉")
				+ metricCardHtml("Menunggu Tindak Lanjut", totalAntrian, "Approval/disposisi belum selesai", "#f59e0b", "&#9203;")
				+ metricCardHtml("Disetujui / Diproses", totalSelesai, "Rasio selesai " + rasioSelesai, "#16a34a", "✓")
				+ metricCardHtml("Ditolak", totalDitolak, "Butuh perhatian & evaluasi", "#dc2626", "!")
				+ metricCardHtml("Revisi", totalRevisi, "Sudah/harus revisi berkas", "#7c3aed", "↻")
				+ metricCardHtml("Total Alur", totalWorkflow, "Semua log persetujuan/disposisi", "#0891b2", "≡")
				+ "</div>";
		appendHtml(parent, html);
	}

	private void renderDasborSuratAnalitik(org.zkoss.zul.Div parent, DashboardSuratData data) {
		appendHtml(parent, sectionIntroHtml("Dashboard Analitis Persuratan",
				"Bagian ini berada di bawah dashboard operasional existing. Tujuannya untuk membaca pola proses: "
						+ "sebaran status persetujuan, klasifikasi yang paling sering dipakai, satuan kerja pengaju terbanyak, "
						+ "dan tren pengajuan dari waktu ke waktu."));

		long totalKeluar = data.totalMenungguKeluar + data.totalDisetujuiKeluar + data.totalDitolakKeluar;
		long totalMasuk = data.totalMenungguMasuk + data.totalDisetujuiMasuk + data.totalDitolakMasuk;

		String pipelineKeluar = dashboardExplainHtml("Menunjukkan komposisi status alur surat keluar: berapa yang masih menunggu, sudah disetujui, dan ditolak. Berguna untuk melihat bottleneck approval.")
				+ progressSectionHtml("Pipeline Persetujuan Surat Keluar", totalKeluar,
				new DashboardMiniRow("Menunggu", data.totalMenungguKeluar),
				new DashboardMiniRow("Disetujui", data.totalDisetujuiKeluar),
				new DashboardMiniRow("Ditolak", data.totalDitolakKeluar));
		String pipelineMasuk = dashboardExplainHtml("Menunjukkan kondisi disposisi surat masuk. Jika jumlah menunggu tinggi, berarti perlu prioritas tindak lanjut pada pejabat/unit terkait.")
				+ progressSectionHtml("Pipeline Disposisi Surat Masuk", totalMasuk,
				new DashboardMiniRow("Menunggu", data.totalMenungguMasuk),
				new DashboardMiniRow("Disetujui", data.totalDisetujuiMasuk),
				new DashboardMiniRow("Ditolak", data.totalDitolakMasuk));

		appendHtml(parent, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:14px;'>"
				+ dashboardPanelHtml(pipelineKeluar) + dashboardPanelHtml(pipelineMasuk) + "</div>");

		String topKlasifikasi = dashboardExplainHtml("Memperlihatkan klasifikasi surat keluar yang paling sering digunakan, sehingga dapat menjadi dasar standardisasi template dan evaluasi kebutuhan administrasi.")
				+ tableMiniRowsHtml("Top Klasifikasi Surat Keluar", data.topKlasifikasiKeluar, "Klasifikasi",
				"Jumlah");
		String topSatker = dashboardExplainHtml("Memperlihatkan satuan kerja dengan volume pengajuan tertinggi agar pimpinan dapat melihat beban administrasi per unit.")
				+ tableMiniRowsHtml("Top Satuan Kerja Pengaju", data.topSatkerKeluar, "Satuan Kerja", "Jumlah");
		appendHtml(parent, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
				+ dashboardPanelHtml(topKlasifikasi) + dashboardPanelHtml(topSatker) + "</div>");

		appendHtml(parent, dashboardPanelHtml(dashboardExplainHtml("Grafik tren membantu melihat periode ramai/sepi pengajuan surat, sehingga staf administrasi dapat menyiapkan kapasitas layanan.")
				+ trendHtml("Tren Pengajuan Surat Keluar", data.trendPengajuanKeluar)));
	}

	private void renderDasborSuratTindakLanjut(org.zkoss.zul.Div parent, DashboardSuratData data) {
		appendHtml(parent, sectionIntroHtml("Dashboard Tindak Lanjut Prioritas",
				"Ringkasan ini membantu user internal melihat surat yang masih perlu diproses. Daftar ini bersifat operasional "
						+ "dan dapat dipakai sebagai watchlist harian agar tidak ada surat tertahan terlalu lama."));

		String keluar = dashboardExplainHtml("Daftar surat keluar yang masih membutuhkan persetujuan. Prioritaskan item yang tanggalnya paling lama atau terkait layanan penting.")
				+ itemListHtml("Tindak Lanjut Persetujuan Surat Keluar", data.pendingKeluar,
				"Tidak ada persetujuan surat keluar yang menunggu pada filter ini.");
		String masuk = dashboardExplainHtml("Daftar disposisi surat masuk yang masih menunggu tindak lanjut. Cocok untuk monitoring surat masuk penting yang perlu keputusan cepat.")
				+ itemListHtml("Tindak Lanjut Disposisi Surat Masuk", data.pendingMasuk,
				isUserInternal() ? "Tidak ada disposisi surat masuk yang menunggu pada filter ini."
						: "Disposisi surat masuk hanya ditampilkan untuk user internal.");
		appendHtml(parent, "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
				+ dashboardPanelHtml(keluar) + dashboardPanelHtml(masuk) + "</div>");
	}

	private void renderDasborSuratAktivitas(org.zkoss.zul.Div parent, DashboardSuratData data) {
		appendHtml(parent, sectionIntroHtml("Dashboard Aktivitas Terbaru",
				"Pengajuan surat paling baru. Cek di sini apakah data sudah masuk, tanpa perlu membuka menu transaksi satu per satu."));

		appendHtml(parent,
				dashboardPanelHtml(dashboardExplainHtml("Pengajuan surat paling baru. Cocok untuk verifikasi cepat apakah surat sudah tercatat di sistem.")
						+ itemListHtml("Pengajuan Surat Terbaru", data.recentPengajuanKeluar,
						"Belum ada pengajuan surat pada periode/filter ini.")));
	}


	private void renderDasborTataKelolaPersuratanV6(org.zkoss.zul.Div parent, DashboardSuratData data) {
		try {
			appendHtml(parent, sectionIntroHtml("Dashboard Tata Kelola & Risiko Persuratan",
					"Bagian ini diletakkan paling bawah karena sifatnya analisis manajerial. Isinya membantu menilai apakah proses surat "
							+ "sudah terkendali dari sisi SLA, metadata arsip, backlog, revisi, dan penolakan."));

			long totalAlur = data.totalPersetujuanKeluar + data.totalPersetujuanMasuk;
			long totalAntrian = data.totalMenungguKeluar + data.totalMenungguMasuk;
			long totalSelesai = data.totalDisetujuiKeluar + data.totalDisetujuiMasuk;
			long totalDitolak = data.totalDitolakKeluar + data.totalDitolakMasuk;
			long totalRevisi = data.totalRevisiKeluar + data.totalRevisiMasuk;
			long totalMetadataIssue = data.totalTanpaKodeKeluar + data.totalTanpaPerihalKeluar + data.totalTanpaSatkerKeluar;

			long selesaiPct = totalAlur <= 0L ? 100L : Math.round((totalSelesai * 100.0d) / totalAlur);
			long backlogPct = totalAlur <= 0L ? 0L : Math.round((totalAntrian * 100.0d) / totalAlur);
			long metadataPct = data.totalPengajuanKeluar <= 0L ? 100L
					: Math.max(0L, 100L - Math.round((totalMetadataIssue * 100.0d) / (data.totalPengajuanKeluar * 3.0d)));
			long risikoPct = Math.min(100L, backlogPct + (totalDitolak * 8L) + (totalRevisi * 4L));
			long indeks = Math.max(0L, Math.min(100L, Math.round((selesaiPct + metadataPct + (100L - risikoPct)) / 3.0d)));

			String html = "<div style='margin-top:16px;'>"
					+ "<div style='padding:16px 18px; border-radius:18px; background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); color:#ffffff; "
					+ "box-shadow:0 16px 34px rgba(15,23,42,0.16);'>"
					+ "<div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.82; font-weight:800;'>Tata Kelola Persuratan</div>"
					+ "<div style='font-size:22px; font-weight:900; margin-top:5px;'>Dashboard Governance & Risiko Administrasi Surat</div>"
					+ "<div style='font-size:12px; margin-top:6px; opacity:.88;'>Ringkasan ini membaca kualitas tata kelola: kecepatan penyelesaian alur, kelengkapan metadata arsip, risiko backlog, serta sinyal revisi/penolakan. Gunakan sebagai bahan evaluasi SOP persuratan.</div>"
					+ "</div>"
					+ "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px; margin-top:12px;'>"
					+ metricCardHtml("Indeks Tata Kelola", indeks, "Skor komposit workflow, metadata, risiko", "#0f766e", "◎")
					+ metricCardHtml("Kepatuhan Metadata", metadataPct, "Estimasi kelengkapan kode/perihal/satker (%)", "#2563eb", "M")
					+ metricCardHtml("Backlog Alur", totalAntrian, "Menunggu approval/disposisi", "#f59e0b", "B")
					+ metricCardHtml("Risiko Revisi/Tolak", totalDitolak + totalRevisi, "Item perlu evaluasi kualitas dokumen", "#dc2626", "R")
					+ "</div>"
					+ "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
					+ dashboardPanelHtml(progressSectionHtml("Kontrol SLA & Backlog", totalAlur,
							new DashboardMiniRow("Selesai", totalSelesai),
							new DashboardMiniRow("Menunggu", totalAntrian),
							new DashboardMiniRow("Ditolak", totalDitolak)))
					+ dashboardPanelHtml(progressSectionHtml("Kualitas Metadata Arsip", data.totalPengajuanKeluar,
							new DashboardMiniRow("Tanpa Kode", data.totalTanpaKodeKeluar),
							new DashboardMiniRow("Tanpa Perihal", data.totalTanpaPerihalKeluar),
							new DashboardMiniRow("Tanpa Satker", data.totalTanpaSatkerKeluar)))
					+ "</div>"
					+ "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;'>"
					+ dashboardPanelHtml(rekomendasiTataKelolaHtml(indeks, backlogPct, metadataPct, totalRevisi, totalDitolak))
					+ dashboardPanelHtml(radarRisikoTataKelolaHtml(backlogPct, metadataPct, risikoPct))
					+ "</div>"
					+ "</div>";
			appendHtml(parent, html);
		} catch (Exception e) {
			debugError("renderDasborTataKelolaPersuratanV6", e);
		}
	}

	private String rekomendasiTataKelolaHtml(long indeks, long backlogPct, long metadataPct, long totalRevisi,
			long totalDitolak) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:16px; font-weight:800; color:#0f172a; margin-bottom:10px;'>")
				.append("Rekomendasi Aksi Tata Kelola</div>");

		sb.append("<div style='display:flex; flex-direction:column; gap:8px;'>");
		if (indeks < 70L) {
			sb.append(rekomendasiItemHtml("Prioritas Tinggi", "Lakukan review mingguan atas alur surat karena indeks tata kelola masih rendah.", "#fee2e2", "#991b1b"));
		} else {
			sb.append(rekomendasiItemHtml("Stabil", "Indeks tata kelola cukup baik, lanjutkan monitoring berkala.", "#dcfce7", "#166534"));
		}
		if (backlogPct > 30L) {
			sb.append(rekomendasiItemHtml("Backlog", "Terapkan reminder otomatis atau eskalasi untuk approval/disposisi yang tertahan.", "#fef3c7", "#92400e"));
		}
		if (metadataPct < 85L) {
			sb.append(rekomendasiItemHtml("Metadata Arsip", "Wajibkan kode, perihal, dan satuan kerja sebelum surat masuk ke alur persetujuan.", "#dbeafe", "#1d4ed8"));
		}
		if (totalRevisi + totalDitolak > 0L) {
			sb.append(rekomendasiItemHtml("Kualitas Draft", "Buat checklist pra-submit untuk mengurangi revisi dan penolakan dokumen.", "#f3e8ff", "#6b21a8"));
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String rekomendasiItemHtml(String title, String text, String bg, String color) {
		return "<div style='padding:11px 12px; border-radius:13px; background:" + bg + "; color:" + color
				+ "; border:1px solid rgba(15,23,42,0.06);'>"
				+ "<div style='font-weight:900; font-size:12px;'>" + safeHtml(title) + "</div>"
				+ "<div style='font-size:12px; margin-top:3px;'>" + safeHtml(text) + "</div></div>";
	}

	private String radarRisikoTataKelolaHtml(long backlogPct, long metadataPct, long risikoPct) {
		String risikoMetadata = String.valueOf(100L - metadataPct);
		return "<div style='font-size:16px; font-weight:800; color:#0f172a; margin-bottom:10px;'>Radar Risiko Tata Kelola</div>"
				+ dashboardExplainHtml("Radar ini membantu membaca tiga risiko utama: antrian yang menumpuk, metadata arsip yang belum lengkap, serta revisi atau penolakan. Gunakan sebagai tanda awal untuk menentukan prioritas perbaikan.")
				+ progressRowHtml("Risiko Backlog", backlogPct, 100L, "#f59e0b")
				+ progressRowHtml("Risiko Metadata", Long.parseLong(risikoMetadata), 100L, "#2563eb")
				+ progressRowHtml("Risiko Revisi/Tolak", risikoPct, 100L, "#dc2626")
				+ "<div style='font-size:11px; color:#64748b; margin-top:8px;'>Debug mode saat ini: "
				+ (debug ? "true" : "false")
				+ " | Field global: <b>private static boolean debug = false;</b></div>";
	}




	private void renderDasborSopTemplateTambahanV7(final org.zkoss.zul.Div body, final DashboardSuratData data) {
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

			renderFunnelPersuratanV7(pcTop, data);
			renderSlaRiskPersuratanV7(pcLeft, data);
			renderMetadataQualityV7(pcRight, data);
			renderBebanKlasifikasiV7(pcLeft, data);
			renderAktivitasPrioritasV7(pcRight, data);
			renderSuratGovernanceHealthV7(pcBottom, data);
			renderQueuePressureV7(pcBottom, data);
			renderExecutionPlanPersuratanV7(pcBottom, data);
			renderDasborSuratTambahanHtmlCssV20(pcBottom, data);
		} catch (Exception e) {
			debugError("renderDasborSopTemplateTambahanV7", e);
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

		String description = getPanelDescriptionForEndUserV20(title);
		if (description != null && description.trim().length() > 0) {
			appendHtml(pch, dashboardExplainHtml(description));
		}
		return pch;
	}

	private String getPanelDescriptionForEndUserV20(String title) {
		if (title == null) {
			return "Ringkasan persuratan agar pekerjaan lebih mudah dipantau dan diselesaikan tepat waktu.";
		}
		String t = title.toLowerCase(java.util.Locale.ENGLISH);
		if (t.indexOf("pengajuan surat anda") >= 0) {
			return "Semua surat yang pernah Anda ajukan. Cek di sini apakah surat sudah diproses, perlu diperbaiki, atau sudah selesai.";
		}
		if (t.indexOf("persetujuan surat keluar") >= 0) {
			return "Surat keluar yang menunggu tanda tangan atau keputusan Anda. Segera tindaklanjuti agar proses tidak tertunda.";
		}
		if (t.indexOf("disposisi surat masuk") >= 0) {
			return "Surat masuk yang perlu Anda teruskan ke pihak yang tepat. Semakin cepat didisposisi, semakin cepat surat ditangani.";
		}
		if (t.indexOf("dasbor surat") >= 0) {
			return "Semua kondisi surat masuk dan keluar dalam satu tampilan. Tidak perlu buka satu per satu — langsung tahu mana yang menumpuk.";
		}
		if (t.indexOf("sla") >= 0 || t.indexOf("risiko") >= 0) {
			return "Surat yang sudah terlalu lama menunggu dan berisiko terlambat. Segera ingatkan pejabat atau unit terkait.";
		}
		if (t.indexOf("metadata") >= 0 || t.indexOf("arsip") >= 0) {
			return "Kelengkapan data surat seperti kode, perihal, dan unit kerja. Surat yang datanya lengkap lebih mudah dicari dan diaudit nanti.";
		}
		if (t.indexOf("klasifikasi") >= 0 || t.indexOf("unit") >= 0) {
			return "Jenis surat dan unit yang paling banyak mengajukan. Membantu pimpinan melihat dari mana beban administrasi terbesar berasal.";
		}
		if (t.indexOf("watchlist") >= 0 || t.indexOf("prioritas") >= 0) {
			return "Daftar pekerjaan mendesak yang perlu segera diperhatikan hari ini. Jadikan acuan agar tidak ada surat yang terlewat.";
		}
		if (t.indexOf("kesehatan") >= 0 || t.indexOf("health") >= 0) {
			return "Skor kondisi alur persuratan secara keseluruhan. Nilai tinggi berarti sehat, nilai rendah berarti perlu perhatian segera.";
		}
		if (t.indexOf("tekanan") >= 0 || t.indexOf("antrian") >= 0) {
			return "Titik-titik penumpukan pekerjaan surat. Gunakan untuk menentukan mana yang harus diselesaikan duluan.";
		}
		if (t.indexOf("eksekusi") >= 0 || t.indexOf("rencana") >= 0) {
			return "Saran tindakan berdasarkan kondisi saat ini. Cocok dibaca di awal hari sebelum mulai kerja.";
		}
		if (t.indexOf("heatmap") >= 0 || t.indexOf("tren") >= 0) {
			return "Pola volume surat dari bulan ke bulan. Terlihat kapan musim ramai dan kapan sepi, berguna untuk merencanakan tenaga.";
		}
		if (t.indexOf("radar") >= 0 || t.indexOf("spider") >= 0) {
			return "Beberapa indikator penting persuratan dalam satu tampilan. Semakin merata nilainya, semakin seimbang pengelolaan surat di instansi.";
		}
		if (t.indexOf("kompas") >= 0 || t.indexOf("operasional") >= 0) {
			return "Status harian persuratan dalam bahasa sederhana. Langsung terlihat apakah semuanya berjalan normal atau ada yang perlu dibenahi.";
		}
		if (t.indexOf("funnel") >= 0 || t.indexOf("proses") >= 0) {
			return "Perjalanan surat dari pengajuan sampai selesai, lengkap di tiap tahapnya. Mudah melihat tahap mana yang paling banyak menumpuk.";
		}
		return "Informasi persuratan yang ringkas agar keputusan bisa diambil tanpa membaca seluruh tabel satu per satu.";
	}

	private void renderFunnelPersuratanV7(org.zkoss.zk.ui.Component parent, DashboardSuratData d) {
		Panelchildren pch = createModernPanelV7(" Proses Persuratan", parent);
		long max = maxValueV7(new long[] { d.totalPengajuanKeluar, d.totalMenungguKeluar, d.totalDisetujuiKeluar,
				d.totalMenungguMasuk, d.totalDisetujuiMasuk, d.totalDitolakKeluar + d.totalDitolakMasuk });
		String html = "<div style='font-size:12px; color:#64748b; margin-bottom:12px; line-height:1.55;'>"
				+ " memperlihatkan posisi proses surat dari pengajuan, antrian persetujuan/disposisi, sampai selesai. "
				+ "Gunakan untuk membaca bottleneck alur kerja harian.</div>"
				+ buildFunnelRowV7("Pengajuan Surat Keluar", d.totalPengajuanKeluar, max, "#2563eb")
				+ buildFunnelRowV7("Menunggu Persetujuan Keluar", d.totalMenungguKeluar, max, "#f59e0b")
				+ buildFunnelRowV7("Disetujui Surat Keluar", d.totalDisetujuiKeluar, max, "#16a34a")
				+ buildFunnelRowV7("Menunggu Disposisi Masuk", d.totalMenungguMasuk, max, "#7c3aed")
				+ buildFunnelRowV7("Disposisi Masuk Selesai", d.totalDisetujuiMasuk, max, "#0891b2")
				+ buildFunnelRowV7("Ditolak / Perlu Koreksi", d.totalDitolakKeluar + d.totalDitolakMasuk, max, "#dc2626");
		appendHtml(pch, html);
	}

	private void renderSlaRiskPersuratanV7(org.zkoss.zk.ui.Component parent, DashboardSuratData d) {
		Panelchildren pch = createModernPanelV7("Analitik Risiko SLA Persuratan", parent);
		long totalAlur = d.totalPersetujuanKeluar + d.totalPersetujuanMasuk;
		long backlog = d.totalMenungguKeluar + d.totalMenungguMasuk;
		long selesai = d.totalDisetujuiKeluar + d.totalDisetujuiMasuk;
		long risikoKoreksi = d.totalDitolakKeluar + d.totalDitolakMasuk + d.totalRevisiKeluar + d.totalRevisiMasuk;
		String html = "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "menilai tekanan SLA dari besarnya antrian terhadap total alur. Makin tinggi backlog, makin perlu eskalasi ke pejabat/unit terkait.</div>"
				+ buildMiniGaugeV7("Completion Rate", percentV7(selesai, totalAlur),
						"Proporsi alur persetujuan/disposisi yang sudah selesai.", "#16a34a")
				+ buildMiniGaugeV7("Queue Pressure", percentV7(backlog, totalAlur),
						"Rasio item yang masih menunggu proses.", "#f59e0b")
				+ buildMiniGaugeV7("Correction Risk", percentV7(risikoKoreksi, Math.max(1L, totalAlur + risikoKoreksi)),
						"Proporsi revisi/penolakan terhadap aktivitas alur.", "#dc2626")
				+ buildMiniGaugeV7("Throughput Signal", percentV7(d.totalPengajuanKeluar, Math.max(1L, d.totalPengajuanKeluar + backlog)),
						"Indikasi volume masuk dibanding beban antrian.", "#2563eb");
		appendHtml(pch, html);
	}

	private void renderMetadataQualityV7(org.zkoss.zk.ui.Component parent, DashboardSuratData d) {
		Panelchildren pch = createModernPanelV7("Kualitas Metadata Arsip Surat", parent);
		long issue = d.totalTanpaKodeKeluar + d.totalTanpaPerihalKeluar + d.totalTanpaSatkerKeluar;
		long peluang = Math.max(1L, d.totalPengajuanKeluar * 3L);
		long score = Math.max(0L, 100L - Math.round((issue * 100.0d) / peluang));
		String html = "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "Metadata yang lengkap membuat surat mudah dicari, diaudit, dan diarsipkan. memantau indikasi data kosong pada surat keluar.</div>"
				+ "<div style='display:flex; gap:12px; flex-wrap:wrap;'>"
				+ buildPressureCardV7("Skor Metadata", score, "Estimasi kepatuhan metadata", "#ecfdf5", "#166534")
				+ buildPressureCardV7("Tanpa Kode", d.totalTanpaKodeKeluar, "Perlu penomoran/validasi", "#dbeafe", "#1e40af")
				+ buildPressureCardV7("Tanpa Perihal", d.totalTanpaPerihalKeluar, "Perlu isi uraian surat", "#fef3c7", "#92400e")
				+ buildPressureCardV7("Tanpa Satker", d.totalTanpaSatkerKeluar, "Perlu mapping unit", "#fee2e2", "#991b1b")
				+ "</div>";
		appendHtml(pch, html);
	}

	private void renderBebanKlasifikasiV7(org.zkoss.zk.ui.Component parent, DashboardSuratData d) {
		Panelchildren pch = createModernPanelV7("Beban Klasifikasi & Unit Kerja", parent);
		String html = "<div style='display:flex; gap:12px; flex-wrap:wrap;'>"
				+ "<div style='flex:1 1 300px;'>"
				+ "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Klasifikasi surat dominan</div>"
				+ buildMiniRowCounterV7(d.topKlasifikasiKeluar, "Belum ada data klasifikasi surat.", 7, "#2563eb")
				+ "</div>"
				+ "<div style='flex:1 1 300px;'>"
				+ "<div style='font-size:12px; font-weight:900; color:#0f172a; margin-bottom:8px;'>Satuan kerja pengaju dominan</div>"
				+ buildMiniRowCounterV7(d.topSatkerKeluar, "Belum ada data satuan kerja pengaju.", 7, "#7c3aed")
				+ "</div>"
				+ "</div>";
		appendHtml(pch, html);
	}

	private void renderAktivitasPrioritasV7(org.zkoss.zk.ui.Component parent, DashboardSuratData d) {
		Panelchildren pch = createModernPanelV7("Watchlist Aktivitas & Prioritas", parent);
		String html = "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "Pekerjaan surat yang paling mendesak. Selesaikan dari sini tanpa harus buka menu satu per satu.</div>"
				+ "<div style='display:flex; gap:12px; flex-wrap:wrap;'>"
				+ "<div style='flex:1 1 300px;'>" + itemListHtml("Persetujuan Keluar Menunggu", d.pendingKeluar,
						"Tidak ada persetujuan keluar yang menunggu.") + "</div>"
				+ "<div style='flex:1 1 300px;'>" + itemListHtml("Disposisi Masuk Menunggu", d.pendingMasuk,
						"Tidak ada disposisi masuk yang menunggu.") + "</div>"
				+ "</div>";
		appendHtml(pch, html);
	}

	private void renderSuratGovernanceHealthV7(org.zkoss.zk.ui.Component parent, DashboardSuratData d) {
		Panelchildren pch = createModernPanelV7("Indeks Kesehatan Workflow Persuratan", parent);
		long totalAlur = d.totalPersetujuanKeluar + d.totalPersetujuanMasuk;
		long selesai = d.totalDisetujuiKeluar + d.totalDisetujuiMasuk;
		long backlog = d.totalMenungguKeluar + d.totalMenungguMasuk;
		long issue = d.totalTanpaKodeKeluar + d.totalTanpaPerihalKeluar + d.totalTanpaSatkerKeluar;
		int completion = percentV7(selesai, totalAlur);
		int queue = percentV7(backlog, Math.max(1L, totalAlur));
		int metadataRisk = percentV7(issue, Math.max(1L, d.totalPengajuanKeluar * 3L));
		int health = 100 - ((queue * 45) / 100) - ((metadataRisk * 25) / 100) + ((completion * 30) / 100);
		if (health < 0) {
			health = 0;
		}
		if (health > 100) {
			health = 100;
		}

		String status = health >= 80 ? "Sehat" : (health >= 60 ? "Perlu Dipantau" : "Prioritas Perbaikan");
		String statusBg = health >= 80 ? "#dcfce7" : (health >= 60 ? "#fef3c7" : "#fee2e2");
		String statusColor = health >= 80 ? "#166534" : (health >= 60 ? "#92400e" : "#991b1b");

		String html = "<div style='display:flex; gap:14px; flex-wrap:wrap; align-items:stretch;'>"
				+ "<div style='flex:1 1 230px; border-radius:16px; padding:16px; color:#ffffff; background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); box-shadow:0 12px 24px rgba(37,99,235,.18);'>"
				+ "<div style='font-size:11px; letter-spacing:.08em; text-transform:uppercase; opacity:.82;'>Workflow Health Score</div>"
				+ "<div style='font-size:46px; line-height:1; font-weight:900; margin-top:10px;'>" + health + "</div>"
				+ "<div style='display:inline-block; margin-top:12px; border-radius:999px; background:" + statusBg + "; color:" + statusColor + "; padding:5px 10px; font-size:11px; font-weight:800;'>" + status + "</div>"
				+ "</div>"
				+ "<div style='flex:2 1 420px;'>"
				+ buildMiniGaugeV7("Completion Rate", completion, "Alur yang selesai dibanding total alur.", "#16a34a")
				+ buildMiniGaugeV7("Queue Pressure", queue, "Antrian aktif yang masih menunggu.", "#f59e0b")
				+ buildMiniGaugeV7("Metadata Risk", metadataRisk, "Risiko arsip dari metadata kosong.", "#dc2626")
				+ "</div></div>";
		appendHtml(pch, html);
	}

	private void renderQueuePressureV7(org.zkoss.zk.ui.Component parent, DashboardSuratData d) {
		Panelchildren pch = createModernPanelV7("Peta Tekanan Antrian Persuratan", parent);
		String html = "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>"
				+ "membantu melihat titik tekanan: apakah lebih berat di persetujuan surat keluar, disposisi surat masuk, atau kualitas dokumen.</div>"
				+ "<div style='display:flex; gap:12px; flex-wrap:wrap;'>"
				+ buildPressureCardV7("Menunggu Keluar", d.totalMenungguKeluar, "Antrian approval surat keluar", "#fef3c7", "#92400e")
				+ buildPressureCardV7("Menunggu Masuk", d.totalMenungguMasuk, "Antrian disposisi surat masuk", "#ede9fe", "#5b21b6")
				+ buildPressureCardV7("Revisi", d.totalRevisiKeluar + d.totalRevisiMasuk, "Butuh koreksi dokumen", "#dbeafe", "#1e40af")
				+ buildPressureCardV7("Ditolak", d.totalDitolakKeluar + d.totalDitolakMasuk, "Perlu evaluasi sebab tolak", "#fee2e2", "#991b1b")
				+ "</div>";
		appendHtml(pch, html);
	}

	private void renderExecutionPlanPersuratanV7(org.zkoss.zk.ui.Component parent, DashboardSuratData d) {
		Panelchildren pch = createModernPanelV7("Prioritas Eksekusi Tata Kelola Persuratan", parent);
		long backlog = d.totalMenungguKeluar + d.totalMenungguMasuk;
		long issue = d.totalTanpaKodeKeluar + d.totalTanpaPerihalKeluar + d.totalTanpaSatkerKeluar;
		String prioritas1 = backlog > 0 ? "Tuntaskan " + backlog + " item yang masih menunggu approval/disposisi."
				: "Tidak ada backlog utama pada filter saat ini.";
		String prioritas2 = issue > 0 ? "Lengkapi " + issue + " metadata yang berpotensi kosong agar arsip lebih siap audit."
				: "Metadata utama relatif aman pada filter saat ini.";
		String prioritas3 = d.totalRevisiKeluar + d.totalRevisiMasuk > 0
				? "Review penyebab revisi agar kualitas draft surat meningkat sebelum masuk approval."
				: "Belum ada sinyal revisi yang dominan.";
		String prioritas4 = topLabelV7(d.topSatkerKeluar, "Belum ada unit dominan")
				+ " perlu dipantau sebagai unit dengan volume pengajuan tertinggi.";

		String html = "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(230px,1fr)); gap:12px;'>"
				+ buildActionPlanCardV7("1", "Kontrol Antrian", prioritas1, "#fee2e2", "#991b1b")
				+ buildActionPlanCardV7("2", "Kelengkapan Arsip", prioritas2, "#dbeafe", "#1e40af")
				+ buildActionPlanCardV7("3", "Kualitas Draft", prioritas3, "#fef3c7", "#92400e")
				+ buildActionPlanCardV7("4", "Monitoring Unit", prioritas4, "#ede9fe", "#5b21b6")
				+ "</div>";
		appendHtml(pch, html);
	}


	private void renderDasborSuratTambahanHtmlCssV20(org.zkoss.zk.ui.Component parent, DashboardSuratData d) {
		Panelchildren pch = createModernPanelV7("Tren, Radar, dan Kompas Operasional", parent);
		long totalAlur = d.totalPersetujuanKeluar + d.totalPersetujuanMasuk;
		long selesai = d.totalDisetujuiKeluar + d.totalDisetujuiMasuk;
		long backlog = d.totalMenungguKeluar + d.totalMenungguMasuk;
		long koreksi = d.totalDitolakKeluar + d.totalDitolakMasuk + d.totalRevisiKeluar + d.totalRevisiMasuk;
		long issue = d.totalTanpaKodeKeluar + d.totalTanpaPerihalKeluar + d.totalTanpaSatkerKeluar;

		String html = "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(310px,1fr)); gap:12px;'>"
				+ "<div style='padding:14px; border-radius:16px; background:#f8fafc; border:1px solid #e2e8f0;'>"
				+ "<div style='font-size:14px; font-weight:900; color:#0f172a;'>Tren Volume Surat Keluar</div>"
				+ "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-top:6px;'>"
				+ "Volume surat keluar per bulan. Bar lebih tinggi = bulan paling sibuk.</div>"
				+ buildHeatmapTrendV20(d.trendPengajuanKeluar)
				+ "</div>"
				+ "<div style='padding:14px; border-radius:16px; background:#f8fafc; border:1px solid #e2e8f0;'>"
				+ "<div style='font-size:14px; font-weight:900; color:#0f172a;'>Radar Kesehatan Persuratan</div>"
				+ "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-top:6px;'>"
				+ "Lima aspek persuratan sekaligus. Area biru lebar berarti sistem sehat dan penyelesaian tinggi.</div>"
				+ buildSvgRadarChartV20(percentV7(selesai, totalAlur), percentV7(backlog, Math.max(1L, totalAlur)),
						percentV7(koreksi, Math.max(1L, totalAlur + koreksi)),
						percentV7(issue, Math.max(1L, d.totalPengajuanKeluar * 3L)),
						percentV7(d.totalPengajuanKeluar, Math.max(1L, d.totalPengajuanKeluar + backlog)))
				+ "</div>"
				+ "</div>";

		html += "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(230px,1fr)); gap:12px; margin-top:12px;'>"
				+ buildOperationalCompassCardV20("Status Antrian", backlog, backlog > 0 ? "Perlu dipantau" : "Aman",
						"Surat menunggu tanda tangan atau disposisi.", "#fef3c7", "#92400e")
				+ buildOperationalCompassCardV20("Koreksi Dokumen", koreksi,
						koreksi > 0 ? "Perlu evaluasi" : "Stabil",
						"Surat dikembalikan karena perlu diperbaiki.", "#fee2e2", "#991b1b")
				+ buildOperationalCompassCardV20("Kelengkapan Arsip", issue,
						issue > 0 ? "Perlu dilengkapi" : "Lengkap",
						"Surat tanpa data lengkap, sulit ditemukan di arsip.", "#dbeafe", "#1e40af")
				+ buildOperationalCompassCardV20("Surat Baru", d.totalPengajuanKeluar,
						d.totalPengajuanKeluar > 0 ? "Ada aktivitas" : "Belum ada",
						"Total surat keluar yang masuk ke sistem.", "#dcfce7", "#166534")
				+ "</div>";

		appendHtml(pch, html);
	}

	private String buildOperationalCompassCardV20(String title, long value, String status, String desc, String bg,
			String color) {
		return "<div style='border-radius:16px; padding:14px; background:" + bg
				+ "; border:1px solid rgba(15,23,42,.08); min-height:118px;'>"
				+ "<div style='font-size:11px; font-weight:900; letter-spacing:.08em; text-transform:uppercase; color:"
				+ color + ";'>" + safeHtml(status) + "</div>"
				+ "<div style='font-size:28px; font-weight:900; color:" + color + "; margin-top:8px;'>"
				+ detailLinkHtmlV8(resolveMetricDetailKeyV8(title), value) + "</div>"
				+ "<div style='font-size:13px; font-weight:900; color:" + color + "; margin-top:4px;'>"
				+ safeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:" + color + "; opacity:.84; line-height:1.45; margin-top:6px;'>"
				+ safeHtml(desc) + "</div></div>";
	}

	private String buildHeatmapTrendV20(List<DashboardMiniRow> rows) {
		if (rows == null || rows.isEmpty()) {
			return emptyHtml("Belum ada data tren untuk filter ini.");
		}
		long max = 1L;
		for (DashboardMiniRow row : rows) {
			if (row != null && row.value > max) {
				max = row.value;
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:flex; gap:8px; align-items:flex-end; overflow:auto; padding:16px 4px 4px 4px; min-height:150px;'>");
		for (DashboardMiniRow row : rows) {
			if (row == null) {
				continue;
			}
			long height = 22L + Math.round((row.value * 95.0d) / max);
			long pct = Math.round((row.value * 100.0d) / max);
			String bg = pct >= 80L ? "#1d4ed8" : (pct >= 50L ? "#06b6d4" : (pct >= 25L ? "#93c5fd" : "#dbeafe"));
			sb.append("<div style='min-width:64px; text-align:center;'>")
					.append("<div style='height:120px; display:flex; align-items:flex-end; justify-content:center;'>")
					.append("<div title='").append(safeHtml(row.label)).append(": ").append(row.value)
					.append("' style='width:42px; height:").append(height)
					.append("px; border-radius:12px 12px 4px 4px; background:").append(bg)
					.append("; box-shadow:0 8px 16px rgba(37,99,235,.16);'></div></div>")
					.append("<div style='font-size:11px; font-weight:900; color:#0f172a;'>")
					.append(detailLinkHtmlV8("pengajuan_keluar", row.value)).append("</div>")
					.append("<div style='font-size:10px; color:#64748b; white-space:nowrap;'>")
					.append(safeHtml(row.label)).append("</div></div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String buildSpiderWebCssV20(int selesai, int antrian, int koreksi, int metadata, int volume) {
		return "<div style='margin-top:14px; display:grid; grid-template-columns:160px 1fr; gap:14px; align-items:center;'>"
				+ "<div style='width:150px; height:150px; border-radius:999px; position:relative; "
				+ "background:conic-gradient(#16a34a 0 " + selesai + "%,#e5e7eb " + selesai + "% 100%);"
				+ "box-shadow:inset 0 0 0 16px #f8fafc, 0 10px 22px rgba(15,23,42,.08);'>"
				+ "<div style='position:absolute; inset:33px; border-radius:999px; background:conic-gradient(#f59e0b 0 "
				+ antrian + "%,#e5e7eb " + antrian + "% 100%);'></div>"
				+ "<div style='position:absolute; inset:58px; border-radius:999px; background:#ffffff; display:flex; align-items:center; justify-content:center; "
				+ "font-size:18px; font-weight:900; color:#0f172a;'>" + selesai + "%</div></div>"
				+ "<div>"
				+ buildMiniGaugeV7("Selesai", selesai, "Persentase alur yang sudah tuntas.", "#16a34a")
				+ buildMiniGaugeV7("Antrian", antrian, "Pekerjaan yang masih menunggu.", "#f59e0b")
				+ buildMiniGaugeV7("Koreksi", koreksi, "Revisi dan penolakan dokumen.", "#dc2626")
				+ buildMiniGaugeV7("Risiko Metadata", metadata, "Indikasi data surat yang belum lengkap.", "#2563eb")
				+ buildMiniGaugeV7("Volume Masuk", volume, "Tekanan dari pengajuan baru.", "#7c3aed")
				+ "</div></div>";
	}

	private String buildSvgRadarChartV20(int selesai, int antrian, int koreksi, int metadata, int volume) {
		// Precomputed pentagon: cos(-90°+i*72°)*1000, sin(-90°+i*72°)*1000 (i=0..4, clockwise from top)
		final int[] COS1K = {0, 951, 588, -588, -951};
		final int[] SIN1K = {-1000, -309, 809, 809, -309};
		final int CX = 130, CY = 130, R = 95, LR = R + 18;
		final String[] LABELS = {"Selesai", "Antrian", "Koreksi", "Metadata", "Volume"};
		final int[] VALS = {selesai, antrian, koreksi, metadata, volume};
		final String[] CLRS = {"#16a34a", "#f59e0b", "#dc2626", "#2563eb", "#7c3aed"};
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='margin-top:14px;'>");
		sb.append("<svg viewBox='0 0 260 260' width='100%' style='max-width:240px; display:block; margin:0 auto;'>");
		// Grid rings at 25%, 50%, 75%, 100%
		for (int ring = 1; ring <= 4; ring++) {
			int rr = R * ring / 4;
			sb.append("<polygon points='");
			for (int i = 0; i < 5; i++) {
				long px = Math.round(CX + (rr * COS1K[i]) / 1000.0);
				long py = Math.round(CY + (rr * SIN1K[i]) / 1000.0);
				if (i > 0) sb.append(' ');
				sb.append(px).append(',').append(py);
			}
			sb.append("' fill='none' stroke='#e2e8f0' stroke-width='1'/>");
		}
		// Axis lines center → tip
		for (int i = 0; i < 5; i++) {
			long px = Math.round(CX + (R * COS1K[i]) / 1000.0);
			long py = Math.round(CY + (R * SIN1K[i]) / 1000.0);
			sb.append("<line x1='").append(CX).append("' y1='").append(CY)
			  .append("' x2='").append(px).append("' y2='").append(py)
			  .append("' stroke='#cbd5e1' stroke-width='1'/>");
		}
		// Data polygon (filled area)
		sb.append("<polygon points='");
		for (int i = 0; i < 5; i++) {
			long px = Math.round(CX + ((long) R * VALS[i] * COS1K[i]) / 100000.0);
			long py = Math.round(CY + ((long) R * VALS[i] * SIN1K[i]) / 100000.0);
			if (i > 0) sb.append(' ');
			sb.append(px).append(',').append(py);
		}
		sb.append("' fill='rgba(37,99,235,.18)' stroke='#2563eb' stroke-width='2.5'/>");
		// Data point dots
		for (int i = 0; i < 5; i++) {
			long px = Math.round(CX + ((long) R * VALS[i] * COS1K[i]) / 100000.0);
			long py = Math.round(CY + ((long) R * VALS[i] * SIN1K[i]) / 100000.0);
			sb.append("<circle cx='").append(px).append("' cy='").append(py)
			  .append("' r='4' fill='").append(CLRS[i]).append("' stroke='#fff' stroke-width='1.5'/>");
		}
		// Axis labels
		for (int i = 0; i < 5; i++) {
			long lx = Math.round(CX + (LR * COS1K[i]) / 1000.0);
			long ly = Math.round(CY + (LR * SIN1K[i]) / 1000.0);
			String anchor = COS1K[i] < -100 ? "end" : (COS1K[i] > 100 ? "start" : "middle");
			sb.append("<text x='").append(lx).append("' y='").append(ly)
			  .append("' text-anchor='").append(anchor)
			  .append("' dominant-baseline='middle' font-size='10' font-weight='700' fill='#334155'>")
			  .append(safeHtml(LABELS[i])).append("</text>");
		}
		// Center label: completion %
		sb.append("<text x='").append(CX).append("' y='").append(CY - 7)
		  .append("' text-anchor='middle' dominant-baseline='middle' font-size='15' font-weight='900' fill='#0f172a'>")
		  .append(selesai).append("%</text>")
		  .append("<text x='").append(CX).append("' y='").append(CY + 9)
		  .append("' text-anchor='middle' dominant-baseline='middle' font-size='9' fill='#64748b'>selesai</text>");
		sb.append("</svg>");
		// Legend strip
		sb.append("<div style='display:flex; flex-wrap:wrap; gap:6px 12px; justify-content:center; margin-top:8px;'>");
		for (int i = 0; i < 5; i++) {
			sb.append("<div style='display:flex; align-items:center; gap:4px;'>")
			  .append("<div style='width:9px; height:9px; border-radius:50%; background:").append(CLRS[i]).append(";'></div>")
			  .append("<span style='font-size:10px; color:#64748b;'>").append(safeHtml(LABELS[i]))
			  .append(" <b style='color:#334155;'>").append(VALS[i]).append("%</b></span>")
			  .append("</div>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	private String buildFunnelRowV7(String label, long value, long max, String color) {
		long pct = max <= 0L ? 0L : Math.round((value * 100.0d) / max);
		if (pct < 4L && value > 0L) {
			pct = 4L;
		}
		return "<div style='display:flex; align-items:center; gap:10px; margin:10px 0;'>"
				+ "<div style='width:210px; font-size:12px; color:#334155; font-weight:800;'>" + safeHtml(label) + "</div>"
				+ "<div style='flex:1; background:#e5e7eb; border-radius:999px; height:12px; overflow:hidden;'>"
				+ "<div style='height:12px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div></div>"
				+ "<div style='width:58px; text-align:right; font-size:13px; font-weight:900; color:#0f172a;'>" + detailLinkHtmlV8(resolveMetricDetailKeyV8(label), value) + "</div></div>";
	}

	private String buildMiniGaugeV7(String title, int pct, String desc, String color) {
		if (pct < 0) {
			pct = 0;
		}
		if (pct > 100) {
			pct = 100;
		}
		return "<div style='padding:8px 0; border-bottom:1px solid #e2e8f0;'>"
				+ "<div style='display:flex; justify-content:space-between; gap:10px; align-items:center;'>"
				+ "<div><div style='font-size:12px; font-weight:900; color:#0f172a;'>" + safeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:#64748b; margin-top:2px;'>" + safeHtml(desc) + "</div></div>"
				+ "<div style='font-size:13px; font-weight:900; color:#0f172a;'>" + pct + "%</div></div>"
				+ "<div style='margin-top:7px; height:9px; border-radius:999px; background:#e2e8f0; overflow:hidden;'>"
				+ "<div style='height:9px; width:" + pct + "%; border-radius:999px; background:" + color + ";'></div></div></div>";
	}

	private String buildPressureCardV7(String title, long value, String desc, String bg, String color) {
		String detailKey = resolveMetricDetailKeyV8(title);
		return "<div style='flex:1 1 170px; min-width:170px; border-radius:14px; padding:13px; background:" + bg + "; border:1px solid rgba(15,23,42,.08);'>"
				+ "<div style='font-size:25px; line-height:1; font-weight:900; color:" + color + ";'>" + detailLinkHtmlV8(detailKey, value) + "</div>"
				+ "<div style='font-size:12px; font-weight:900; color:" + color + "; margin-top:8px;'>" + safeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:" + color + "; opacity:.82; margin-top:5px;'>" + safeHtml(desc) + "</div></div>";
	}

	private String buildMiniRowCounterV7(List<DashboardMiniRow> rows, String emptyMessage, int limit, String color) {
		if (rows == null || rows.isEmpty()) {
			return "<div style='padding:14px; border-radius:14px; background:#f8fafc; border:1px dashed #cbd5e1; color:#64748b; font-size:12px;'>"
					+ safeHtml(emptyMessage) + "</div>";
		}
		long max = 1L;
		for (DashboardMiniRow row : rows) {
			if (row != null && row.value > max) {
				max = row.value;
			}
		}
		String html = "";
		int no = 1;
		for (DashboardMiniRow row : rows) {
			if (row == null) {
				continue;
			}
			if (no > limit) {
				break;
			}
			long pct = Math.round((row.value * 100.0d) / max);
			if (pct < 4L && row.value > 0L) {
				pct = 4L;
			}
			html += "<div style='padding:9px 0; border-bottom:1px solid #f1f5f9;'>"
					+ "<div style='display:flex; justify-content:space-between; gap:10px;'>"
					+ "<div style='font-size:12px; font-weight:800; color:#334155;'>" + no + ". " + safeHtml(row.label) + "</div>"
					+ "<div style='font-size:12px; font-weight:900; color:#0f172a;'>" + row.value + "</div></div>"
					+ "<div style='margin-top:7px; height:8px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>"
					+ "<div style='height:8px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div></div></div>";
			no++;
		}
		return html;
	}

	private String buildActionPlanCardV7(String no, String title, String desc, String bg, String color) {
		return "<div style='border-radius:16px; padding:14px; background:" + bg + "; border:1px solid rgba(15,23,42,.08); min-height:105px;'>"
				+ "<div style='display:flex; justify-content:space-between; gap:12px; align-items:center;'>"
				+ "<div style='font-size:12px; font-weight:900; color:" + color + ";'>" + safeHtml(title) + "</div>"
				+ "<div style='width:28px; height:28px; border-radius:999px; background:#ffffff; color:" + color + "; display:flex; align-items:center; justify-content:center; font-weight:900;'>" + safeHtml(no) + "</div></div>"
				+ "<div style='font-size:12px; color:" + color + "; line-height:1.45; margin-top:10px;'>" + safeHtml(desc) + "</div></div>";
	}

	private int percentV7(long value, long total) {
		if (total <= 0L || value <= 0L) {
			return 0;
		}
		return (int) Math.round((value * 100.0d) / total);
	}

	private long maxValueV7(long[] values) {
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

	private String topLabelV7(List<DashboardMiniRow> rows, String fallback) {
		if (rows == null || rows.isEmpty()) {
			return fallback;
		}
		DashboardMiniRow top = rows.get(0);
		return top == null || top.label == null || top.label.trim().isEmpty() ? fallback : top.label;
	}

	private String sectionIntroHtml(String title, String description) {
		return "<div style='margin-top:16px; padding:14px 16px; border-radius:16px; background:#ffffff; "
				+ "border:1px solid #e8eef6; box-shadow:0 8px 22px rgba(15,23,42,0.04);'>"
				+ "<div style='font-size:16px; font-weight:900; color:#0f172a;'>" + safeHtml(title) + "</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:5px; line-height:1.55;'>"
				+ safeHtml(description) + "</div></div>";
	}

	private String dashboardExplainHtml(String description) {
		return "<div style='font-size:12px; color:#64748b; line-height:1.55; margin:-2px 0 12px 0; "
				+ "padding:10px 12px; border-radius:12px; background:#f8fafc; border:1px dashed #cbd5e1;'>"
				+ safeHtml(description) + "</div>";
	}

	private void appendLegacyPanelIntroV20(Component parent, String title) {
		try {
			String description = getPanelDescriptionForEndUserV20(title);
			if (description != null && description.trim().length() > 0) {
				appendHtml(parent, dashboardExplainHtml(description));
			}
		} catch (Exception e) {
			debugError("appendLegacyPanelIntroV20", e);
		}
	}

	private String progressSectionHtml(String title, long total, DashboardMiniRow a, DashboardMiniRow b, DashboardMiniRow c) {
		return "<div><div style='font-size:16px; font-weight:800; color:#0f172a; margin-bottom:10px;'>" + safeHtml(title)
				+ "</div>" + progressRowHtml(resolveProgressDetailKeyV8(title, a.label), a.label, a.value, total, "#f59e0b")
				+ progressRowHtml(resolveProgressDetailKeyV8(title, b.label), b.label, b.value, total, "#16a34a")
				+ progressRowHtml(resolveProgressDetailKeyV8(title, c.label), c.label, c.value, total, "#dc2626") + "</div>";
	}

	private String progressRowHtml(String label, long value, long total, String color) {
		return progressRowHtml(null, label, value, total, color);
	}

	private String progressRowHtml(String detailKey, String label, long value, long total, String color) {
		long pct = total <= 0L ? 0L : Math.round((value * 100.0d) / total);
		return "<div style='margin-bottom:12px;'>"
				+ "<div style='display:flex; justify-content:space-between; gap:10px; font-size:12px; color:#475569; margin-bottom:5px;'>"
				+ "<span style='font-weight:700;'>" + safeHtml(label) + "</span><span>" + detailLinkHtmlV8(detailKey, value) + " (" + pct + "%)</span></div>"
				+ "<div style='height:10px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>"
				+ "<div style='height:10px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div>"
				+ "</div></div>";
	}

	private String tableMiniRowsHtml(String title, List<DashboardMiniRow> rows, String col1, String col2) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:16px; font-weight:800; color:#0f172a; margin-bottom:10px;'>")
				.append(safeHtml(title)).append("</div>");
		if (rows == null || rows.isEmpty()) {
			sb.append(emptyHtml("Belum ada data."));
			return sb.toString();
		}
		sb.append("<table style='width:100%; border-collapse:separate; border-spacing:0 7px;'>")
				.append("<thead><tr style='font-size:11px; color:#64748b; text-transform:uppercase; letter-spacing:.05em;'>")
				.append("<th style='text-align:left; padding:0 8px;'>").append(safeHtml(col1)).append("</th>")
				.append("<th style='text-align:right; padding:0 8px;'>").append(safeHtml(col2)).append("</th>")
				.append("</tr></thead><tbody>");
		for (DashboardMiniRow row : rows) {
			sb.append("<tr style='background:#f8fafc; border-radius:12px;'>")
					.append("<td style='padding:10px 8px; color:#0f172a; font-weight:650; border-radius:10px 0 0 10px;'>")
					.append(safeHtml(row.label)).append("</td>")
					.append("<td style='padding:10px 8px; text-align:right; color:#1d4ed8; font-weight:800; border-radius:0 10px 10px 0;'>")
					.append(detailLinkHtmlV8(resolveMiniTableDetailKeyV8(title, row.label), row.value)).append("</td></tr>");
		}
		sb.append("</tbody></table>");
		return sb.toString();
	}

	private String trendHtml(String title, List<DashboardMiniRow> rows) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:16px; font-weight:800; color:#0f172a; margin-bottom:10px;'>")
				.append(safeHtml(title)).append("</div>");
		if (rows == null || rows.isEmpty()) {
			sb.append(emptyHtml("Belum ada tren untuk periode/filter ini."));
			return sb.toString();
		}
		long max = 1L;
		for (DashboardMiniRow row : rows) {
			if (row.value > max) {
				max = row.value;
			}
		}
		sb.append("<div style='display:flex; align-items:flex-end; gap:8px; height:170px; padding:12px; background:#f8fafc; border-radius:14px; overflow:auto;'>");
		for (DashboardMiniRow row : rows) {
			long height = 10L + Math.round((row.value * 130.0d) / max);
			sb.append("<div style='min-width:64px; text-align:center; display:flex; flex-direction:column; justify-content:flex-end; height:145px;'>")
					.append("<div style='font-size:11px; font-weight:800; color:#1d4ed8; margin-bottom:4px;'>")
					.append(detailLinkHtmlV8("TREND_PENGAJUAN_KELUAR|" + row.label, row.value)).append("</div>")
					.append("<div style='height:").append(height)
					.append("px; border-radius:12px 12px 4px 4px; background:linear-gradient(180deg, var(--ais-theme-accent,#38bdf8), var(--ais-theme-primary,#2563eb));'></div>")
					.append("<div style='font-size:10px; color:#64748b; margin-top:6px; white-space:nowrap;'>")
					.append(safeHtml(row.label)).append("</div></div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String itemListHtml(String title, List<DashboardItem> items, String emptyText) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:16px; font-weight:800; color:#0f172a; margin-bottom:10px;'>")
				.append(safeHtml(title)).append("</div>");
		if (items == null || items.isEmpty()) {
			sb.append(emptyHtml(emptyText));
			return sb.toString();
		}
		for (DashboardItem item : items) {
			sb.append("<div style='padding:11px 12px; border:1px solid #e2e8f0; border-radius:14px; margin-bottom:8px; background:#ffffff;'>")
					.append("<div style='display:flex; justify-content:space-between; gap:8px; align-items:flex-start;'>")
					.append("<div style='font-weight:800; color:#0f172a; font-size:13px;'>").append(safeHtml(item.title))
					.append("</div>").append(badgeHtml(item.status, "#eff6ff", "#1d4ed8")).append("</div>")
					.append("<div style='font-size:12px; color:#475569; margin-top:4px;'>").append(safeHtml(item.description))
					.append("</div>").append("<div style='font-size:11px; color:#94a3b8; margin-top:6px;'>")
					.append(safeHtml(item.meta)).append("</div></div>");
		}
		return sb.toString();
	}

	private String metricCardHtml(String title, long value, String subtitle, String color, String icon) {
		String detailKey = resolveMetricDetailKeyV8(title);
		return "<div style='background:#ffffff; border:1px solid #e8eef6; border-radius:17px; padding:15px; "
				+ "box-shadow:0 10px 24px rgba(15,23,42,0.05); min-height:106px;'>"
				+ "<div style='display:flex; align-items:center; justify-content:space-between; gap:10px;'>"
				+ "<div style='font-size:12px; color:#64748b; font-weight:800; text-transform:uppercase; letter-spacing:.04em;'>"
				+ safeHtml(title) + "</div>"
				+ "<div style='width:34px; height:34px; border-radius:12px; display:flex; align-items:center; justify-content:center; "
				+ "color:#ffffff; font-weight:900; background:" + color + ";'>" + safeHtml(icon) + "</div></div>"
				+ "<div style='font-size:30px; line-height:1; font-weight:900; color:#0f172a; margin-top:12px;'>" + detailLinkHtmlV8(detailKey, value)
				+ "</div>" + "<div style='font-size:12px; color:#64748b; margin-top:6px;'>" + safeHtml(subtitle)
				+ "</div></div>";
	}

	private String dashboardPanelHtml(String content) {
		return "<div style='margin-top:12px; padding:15px; background:#ffffff; border:1px solid #e8eef6; border-radius:17px; "
				+ "box-shadow:0 10px 24px rgba(15,23,42,0.05); box-sizing:border-box;'>" + content + "</div>";
	}

	private String badgeHtml(String text, String bg, String color) {
		return "<span style='display:inline-block; padding:5px 9px; border-radius:999px; background:" + bg
				+ "; color:" + color + "; font-size:11px; font-weight:800;'>" + safeHtml(text) + "</span>";
	}

	private String emptyHtml(String text) {
		return "<div style='padding:14px; border-radius:13px; background:#f8fafc; color:#64748b; border:1px dashed #cbd5e1; font-size:12px;'>"
				+ safeHtml(text) + "</div>";
	}

	private void appendHtml(org.zkoss.zk.ui.Component parent, String html) {
		Html h = new Html(html);
		h.setParent(parent);
	}

	private void sortAndLimit(List<DashboardMiniRow> rows, final int max) {
		java.util.Collections.sort(rows, new java.util.Comparator<DashboardMiniRow>() {
			public int compare(DashboardMiniRow a, DashboardMiniRow b) {
				if (a.value == b.value) {
					return safeText(a.label).compareToIgnoreCase(safeText(b.label));
				}
				return a.value < b.value ? 1 : -1;
			}
		});
		while (rows.size() > max) {
			rows.remove(rows.size() - 1);
		}
	}

	private String getTargetPersetujuanKeluar(AlurPersetujuanSuratKeluarStatus status) {
		try {
			if (status.getJenisJabatan() != null) {
				return safeText(status.getJenisJabatan().getNama());
			}
			if (status.getAlurPersetujuanSuratKeluar() != null) {
				return safeText(String.valueOf(status.getAlurPersetujuanSuratKeluar()));
			}
			if (status.getPejabat() != null) {
				return safeText(status.getPejabat().getNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:5177");
		}
		return "Pejabat terkait";
	}

	private String getTargetPersetujuanMasuk(AlurPersetujuanSuratMasukStatus status) {
		try {
			if (status.getJenisJabatan() != null) {
				return safeText(status.getJenisJabatan().getNama());
			}
			if (status.getAlurPersetujuanSuratMasuk() != null) {
				return safeText(String.valueOf(status.getAlurPersetujuanSuratMasuk()));
			}
			if (status.getPejabat() != null) {
				return safeText(status.getPejabat().getNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:5193");
		}
		return "Pejabat terkait";
	}

	private String firstNotEmpty(String a, String b, String c) {
		return firstNotEmpty(a, b, null, c);
	}

	private String firstNotEmpty(String a, String b, String c, String fallback) {
		if (a != null && !a.trim().isEmpty()) {
			return safeText(a);
		}
		if (b != null && !b.trim().isEmpty()) {
			return safeText(b);
		}
		if (c != null && !c.trim().isEmpty()) {
			return safeText(c);
		}
		return safeText(fallback);
	}

	private String safeText(String value) {
		return value == null ? "" : value;
	}

	private String safeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private String formatTanggalDasbor(Date date) {
		if (date == null) {
			return "-";
		}
		try {
			return Common.dateFormat.get().format(date);
		} catch (Exception e) {
			return new java.text.SimpleDateFormat("dd MMM yyyy", new java.util.Locale("id", "ID")).format(date);
		}
	}

	private String formatPercent(long numerator, long denominator) {
		if (denominator <= 0L) {
			return "0%";
		}
		return Math.round((numerator * 100.0d) / denominator) + "%";
	}

	private boolean isUserInternal() {
		try {
			return tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getMahasiswa() == null;
		} catch (Exception e) {
			return false;
		}
	}


	/**
	 * Tipe implementasi bersarang {@link DashboardAlurParentGroupV14} milik {@link DasboardSurat}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardSurat}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String jenis}, {@code String
	 * tanggal}, {@code String kode}, {@code String perihal}, {@code String parameterHtml}, {@code String
	 * timelineHtml}, {@code String ringkasanHtml}. Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 *
	 * @see DasboardSurat
	 */
	private static class DashboardAlurParentGroupV14 {
		String jenis;
		String tanggal;
		String kode;
		String perihal;
		String parameterHtml;
		String timelineHtml;
		String ringkasanHtml;
	}


	/**
	 * Tipe implementasi bersarang {@link DashboardSuratData} milik {@link DasboardSurat}. Kelas ini memberi nama
	 * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardSurat}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Date mulai}, {@code Date sampai},
	 * {@code ais.database.model.rab.SatuanKerja satuanKerja}, {@code String keyword}, {@code long
	 * totalPengajuanKeluar}, {@code long totalPersetujuanKeluar}, {@code long totalMenungguKeluar}, {@code long
	 * totalDisetujuiKeluar}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 *
	 * @see DasboardSurat
	 */
	private static class DashboardSuratData {
		Date mulai;
		Date sampai;
		ais.database.model.rab.SatuanKerja satuanKerja;
		String keyword;
		long totalPengajuanKeluar;
		long totalPersetujuanKeluar;
		long totalMenungguKeluar;
		long totalDisetujuiKeluar;
		long totalDitolakKeluar;
		long totalRevisiKeluar;
		long totalPersetujuanMasuk;
		long totalMenungguMasuk;
		long totalDisetujuiMasuk;
		long totalDitolakMasuk;
		long totalRevisiMasuk;
		long totalTanpaKodeKeluar;
		long totalTanpaPerihalKeluar;
		long totalTanpaSatkerKeluar;
		List<DashboardMiniRow> topKlasifikasiKeluar = new ArrayList<DashboardMiniRow>();
		List<DashboardMiniRow> topSatkerKeluar = new ArrayList<DashboardMiniRow>();
		List<DashboardMiniRow> trendPengajuanKeluar = new ArrayList<DashboardMiniRow>();
		List<DashboardItem> recentPengajuanKeluar = new ArrayList<DashboardItem>();
		List<DashboardItem> pendingKeluar = new ArrayList<DashboardItem>();
		List<DashboardItem> pendingMasuk = new ArrayList<DashboardItem>();
	}

	/**
	 * Pembawa data/helper lokal milik {@link DasboardSurat} untuk dashboard mini row. Tipe ini mengelompokkan
	 * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardSurat}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String label}, {@code long value}.
	 * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see DasboardSurat
	 */
	private static class DashboardMiniRow {
		String label;
		long value;

		DashboardMiniRow(String label, long value) {
			this.label = label;
			this.value = value;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link DashboardItem} milik {@link DasboardSurat}. Kelas ini memberi nama pada
	 * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardSurat}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String title}, {@code String
	 * description}, {@code String meta}, {@code String status}. Aturan bisnis bersama tetap berada pada kelas
	 * induk atau service yang dipanggilnya.</p>
	 *
	 * @see DasboardSurat
	 */
	private static class DashboardItem {
		String title;
		String description;
		String meta;
		String status;

		DashboardItem(String title, String description, String meta, String status) {
			this.title = title;
			this.description = description;
			this.meta = meta;
			this.status = status;
		}
	}

	public static void tolak(Session session, AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus) {

		System.out.println("tolak alurPersetujuanSuratKeluarStatus -> " + alurPersetujuanSuratKeluarStatus);

		SuratKeluar suratKeluar = (SuratKeluar) session.createCriteria(SuratKeluar.class)
				.add(Restrictions.idEq(alurPersetujuanSuratKeluarStatus.getSuratKeluar().getId())).uniqueResult();

		session.refresh(suratKeluar);
		suratKeluar.setAlurDitolak(alurPersetujuanSuratKeluarStatus);
		Common.refreshUpdate(session, suratKeluar);
		session.flush();

	}

	public static void tolak(Session session, AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus) {

		System.out.println("tolak alurPersetujuanSuratMasukStatus -> " + alurPersetujuanSuratMasukStatus);
		SuratMasuk suratMasuk = (SuratMasuk) session.createCriteria(SuratMasuk.class)
				.add(Restrictions.idEq(alurPersetujuanSuratMasukStatus.getSuratMasuk().getId())).uniqueResult();

		session.refresh(suratMasuk);

		suratMasuk.setAlurDitolak(alurPersetujuanSuratMasukStatus);
		session.getTransaction().begin();
		Common.refreshUpdate(session, suratMasuk);

		session.flush();

	}

	// ── Static renderer helpers — reused by both Keluar and Masuk renderers ────

	private static String escHtml(String s) {
		if (s == null || s.isEmpty()) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String pejabatNamaV20(Pejabat pejabat) {
		if (pejabat == null) return "";
		try {
			if (pejabat.getPegawai() != null && pejabat.getPegawai().getNama() != null)
				return pejabat.getPegawai().getNama();
			if (pejabat.getDosen() != null && pejabat.getDosen().getNama() != null)
				return pejabat.getDosen().getNama();
			if (pejabat.getNama() != null) return pejabat.getNama();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:5361"); /* ignore */ }
		return "";
	}

	private static String buildStatusPillV20(boolean disetujui, boolean ditolak) {
		if (ditolak)
			return "<span style='padding:1px 7px; border-radius:999px; background:#fee2e2; color:#991b1b; font-size:9px; font-weight:900;'>&#x2717; Ditolak</span>";
		if (disetujui)
			return "<span style='padding:1px 7px; border-radius:999px; background:#dcfce7; color:#166534; font-size:9px; font-weight:900;'>&#x2713; Selesai</span>";
		return "<span style='padding:1px 7px; border-radius:999px; background:#fef3c7; color:#92400e; font-size:9px; font-weight:900;'>&#x25cf; Menunggu</span>";
	}

	private static String buildAlurStatusCardV20(String jabatan, boolean disetujui, boolean ditolak,
			String namaAktor, String catatan, String waktu) {
		String bg     = ditolak ? "#fff5f5" : (disetujui ? "#f0fdf4" : "#fffbeb");
		String border = ditolak ? "#fecaca" : (disetujui ? "#bbf7d0" : "#fde68a");
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='margin:3px 0; padding:6px 9px; border-radius:10px; background:").append(bg)
		  .append("; border:1px solid ").append(border).append("; line-height:1.4;'>")
		  .append("<div style='display:flex; align-items:center; justify-content:space-between; gap:6px; flex-wrap:wrap;'>")
		  .append("<span style='font-size:11px; font-weight:900; color:#0f172a;'>").append(escHtml(jabatan)).append("</span>")
		  .append(buildStatusPillV20(disetujui, ditolak))
		  .append("</div>");
		if (namaAktor != null && !namaAktor.isEmpty())
			sb.append("<div style='font-size:10px; color:#64748b; margin-top:2px;'>").append(escHtml(namaAktor)).append("</div>");
		if (catatan != null && !catatan.isEmpty())
			sb.append("<div style='font-size:10px; color:#7c3aed; font-style:italic; margin-top:1px;'>").append(escHtml(catatan)).append("</div>");
		if (waktu != null && !waktu.isEmpty())
			sb.append("<div style='font-size:9px; color:#94a3b8; margin-top:1px;'>").append(escHtml(waktu)).append("</div>");
		sb.append("</div>");
		return sb.toString();
	}

	public static String buildAlurKeluarStatusHtmlV20(AlurPersetujuanSuratKeluarStatus s) {
		if (s == null) return "<div style='font-size:10px; color:#94a3b8;'>Belum ada data</div>";
		String jabatan = s.getJenisJabatan() != null ? s.getJenisJabatan().getNama()
				: (s.getAlurPersetujuanSuratKeluar() != null ? String.valueOf(s.getAlurPersetujuanSuratKeluar()) : "");
		boolean disetujui = Boolean.TRUE.equals(s.getDisetujui());
		boolean ditolak   = Boolean.TRUE.equals(s.getDitolak());
		String namaAktor  = pejabatNamaV20(s.getPejabat());
		String catatan    = s.getKeterangan() != null ? s.getKeterangan().trim() : "";
		String waktu = "";
		try {
			if (ditolak && s.getWaktuDitolak() != null)
				waktu = Common.dateFormat3.get().format(s.getWaktuDitolak());
			else if (disetujui && s.getWaktuPersetujuan() != null)
				waktu = Common.dateFormat3.get().format(s.getWaktuPersetujuan());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:5408"); /* ignore */ }
		return buildAlurStatusCardV20(jabatan, disetujui, ditolak, namaAktor, catatan, waktu);
	}

	/**
	 * Kartu alur persetujuan SATU status surat masuk, LENGKAP dengan catatan/keterangan — dipakai
	 * popup "Catatan Disposisi" ({@code CatatanDisposisiPopupHelper}). Berbeda dgn versi List di bawah
	 * yang sengaja tanpa catatan (ringkas untuk bagan). Mirror {@link #buildAlurKeluarStatusHtmlV20}.
	 */
	public static String buildAlurMasukStatusHtmlV20(AlurPersetujuanSuratMasukStatus s) {
		if (s == null) return "<div style='font-size:10px; color:#94a3b8;'>Belum ada data</div>";
		String jabatan = s.getJenisJabatan() != null ? s.getJenisJabatan().getNama()
				: (s.getAlurPersetujuanSuratMasuk() != null ? String.valueOf(s.getAlurPersetujuanSuratMasuk()) : "");
		boolean disetujui = Boolean.TRUE.equals(s.getDisetujui());
		boolean ditolak = Boolean.TRUE.equals(s.getDitolak());
		String namaAktor = pejabatNamaV20(s.getPejabat());
		String catatan = s.getKeterangan() != null ? s.getKeterangan().trim() : "";
		String waktu = "";
		try {
			if (ditolak && s.getWaktuDitolak() != null)
				waktu = Common.dateFormat3.get().format(s.getWaktuDitolak());
			else if (disetujui && s.getWaktuPersetujuan() != null)
				waktu = Common.dateFormat3.get().format(s.getWaktuPersetujuan());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) DasboardSurat.buildAlurMasukStatusHtmlV20");
		}
		return buildAlurStatusCardV20(jabatan, disetujui, ditolak, namaAktor, catatan, waktu);
	}

	public static String buildAlurMasukStatusListHtmlV20(List<AlurPersetujuanSuratMasukStatus> items) {
		if (items == null || items.isEmpty())
			return "<div style='font-size:10px; color:#94a3b8;'>Belum ada alur</div>";
		StringBuilder sb = new StringBuilder();
		for (AlurPersetujuanSuratMasukStatus s : items) {
			if (s == null) continue;
			String jabatan = s.getJenisJabatan() != null ? s.getJenisJabatan().getNama()
					: (s.getAlurPersetujuanSuratMasuk() != null ? String.valueOf(s.getAlurPersetujuanSuratMasuk()) : "");
			boolean disetujui = Boolean.TRUE.equals(s.getDisetujui());
			boolean ditolak   = Boolean.TRUE.equals(s.getDitolak());
			String namaAktor  = pejabatNamaV20(s.getPejabat());
			String waktu = "";
			try {
				if (s.getWaktuPersetujuan() != null)
					waktu = Common.dateFormat3.get().format(s.getWaktuPersetujuan());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/DasboardSurat.java:5427"); /* ignore */ }
			sb.append(buildAlurStatusCardV20(jabatan, disetujui, ditolak, namaAktor, "", waktu));
		}
		return sb.toString();
	}

	public static String buildOpsiChipsHtmlV20(List<String> values) {
		if (values == null || values.isEmpty()) return "";
		StringBuilder sb = new StringBuilder("<div style='display:flex; flex-wrap:wrap; gap:4px; padding:2px 0;'>");
		for (String v : values) {
			if (v == null || v.trim().isEmpty()) continue;
			sb.append("<span style='padding:2px 7px; border-radius:999px; background:#eff6ff; color:#1e40af; font-size:10px; font-weight:700;'>")
			  .append(escHtml(v)).append("</span>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	public static String buildParamRowHtmlV20(String label, String value) {
		return "<div style='margin:2px 0; font-size:11px;'>"
				+ "<span style='color:#64748b;'>" + escHtml(label) + ":</span> "
				+ "<span style='color:#0f172a; font-weight:700;'>" + escHtml(value) + "</span></div>";
	}

	public static String buildIsiWrapperHtmlV20(String content, String keterangan) {
		StringBuilder sb = new StringBuilder("<div style='line-height:1.55;'>").append(content);
		if (keterangan != null && !keterangan.trim().isEmpty())
			sb.append("<div style='margin-top:4px; font-size:10px; color:#64748b; font-style:italic;'>")
			  .append(escHtml(keterangan)).append("</div>");
		sb.append("</div>");
		return sb.toString();
	}

	public static String buildAktifBadgeHtmlV20(boolean aktif) {
		if (aktif)
			return "<span style='display:inline-block; padding:2px 10px; border-radius:999px; background:#dcfce7; color:#166534; font-size:10px; font-weight:800;'>&#x2713; Aktif</span>";
		return "<span style='display:inline-block; padding:2px 10px; border-radius:999px; background:#f1f5f9; color:#64748b; font-size:10px;'>Tidak Aktif</span>";
	}

	public static String buildUnitInfoHtmlV20(String sk, String fak, String jur, String yay, String sek) {
		StringBuilder sb = new StringBuilder("<div style='font-size:11px; line-height:1.6;'>");
		boolean hasAny = false;
		if (sk != null && !sk.trim().isEmpty()) {
			sb.append("<div style='font-weight:800; color:#0f172a;'>").append(escHtml(sk)).append("</div>");
			hasAny = true;
		}
		if ((fak != null && !fak.trim().isEmpty()) || (jur != null && !jur.trim().isEmpty())) {
			sb.append("<div style='color:#2563eb; margin-left:6px;'>");
			if (fak != null && !fak.trim().isEmpty()) sb.append(escHtml(fak));
			if (jur != null && !jur.trim().isEmpty())
				sb.append(" &rsaquo; <span style='color:#7c3aed;'>").append(escHtml(jur)).append("</span>");
			sb.append("</div>");
			hasAny = true;
		}
		if ((yay != null && !yay.trim().isEmpty()) || (sek != null && !sek.trim().isEmpty())) {
			sb.append("<div style='color:#0891b2; margin-left:6px;'>");
			if (yay != null && !yay.trim().isEmpty()) sb.append(escHtml(yay));
			if (sek != null && !sek.trim().isEmpty())
				sb.append(" &rsaquo; <span style='color:#0f766e;'>").append(escHtml(sek)).append("</span>");
			sb.append("</div>");
			hasAny = true;
		}
		if (!hasAny) sb.append("<span style='font-size:10px; color:#94a3b8;'>Semua Unit</span>");
		sb.append("</div>");
		return sb.toString();
	}

	public static String buildParentAlurHtmlV20(String parentNama) {
		if (parentNama == null || parentNama.trim().isEmpty())
			return "<span style='font-size:10px; color:#94a3b8;'>—</span>";
		return "<span style='display:inline-block; padding:2px 7px; border-radius:6px; background:#f0f9ff;"
				+ " color:#0369a1; font-size:10px; font-weight:700; border:1px solid #bae6fd;'>"
				+ "&larr; " + escHtml(parentNama) + "</span>";
	}

	public static String buildKeteranganHtmlV20(String keterangan) {
		if (keterangan == null || keterangan.trim().isEmpty())
			return "<span style='font-size:10px; color:#94a3b8;'>—</span>";
		return "<div style='font-size:11px; color:#475569; font-style:italic; line-height:1.5;'>"
				+ escHtml(keterangan) + "</div>";
	}

}
