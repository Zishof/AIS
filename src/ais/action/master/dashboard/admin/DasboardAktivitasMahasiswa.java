package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


/*
 * DASBOARD_AKTIVITAS_MAHASISWA_V3_LOADING_PROGRESS_2026_05_30
 *
 * Dashboard ringkas untuk Kegiatan, Organisasi, Prestasi, Karya/Penghargaan,
 * dan Catatan Mahasiswa. UI mengikuti pola modern DasboardSop:
 * - hero summary
 * - filter global
 * - metric card clickable
 * - popup detail paging 10 data
 * - analitik status, tren tahun, top data, dan aktivitas terbaru
 *
 * Catatan akses:
 * Jika login sebagai mahasiswa, seluruh criteria otomatis dibatasi ke data
 * mahasiswa login:
 *     Tbmuser tbmuser = Common.getCurrentUser();
 *     Mahasiswa currentMahasiswa = tbmuser.getMahasiswa();
 *
 * Kompatibilitas:
 * - Tetap Java lama, tanpa lambda/stream/try-with-resources.
 * - Menggunakan Criteria API seperti class Action yang sudah ada.
 */

import java.lang.reflect.Method;
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
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.A;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CatatanMahasiswa;
import ais.database.model.KegiatanKemahasiswaan;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.OrganisasiIntraKampusPunyaMahasiswa;
import ais.database.model.PenghargaanMahasiswa;
import ais.database.model.PrestasiMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Menampilkan kegiatan, organisasi, prestasi, penghargaan, dan catatan mahasiswa agar perkembangan non-akademik mudah dipantau.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardAktivitasMahasiswa extends MyPortallayout {

	private static final long serialVersionUID = -8206529289831842101L;
	private static final java.util.concurrent.ConcurrentHashMap<String, Object> _CACHE
			= new java.util.concurrent.ConcurrentHashMap<String, Object>();
	private static final java.util.concurrent.ConcurrentHashMap<String, Long> _EXPIRY
			= new java.util.concurrent.ConcurrentHashMap<String, Long>();
	private static final long _TTL_MS = 5L * 60 * 1000;

	public static boolean debug = false;
	public static boolean debuh = false;

	private static final int DETAIL_PAGE_SIZE = 10;
	private static final int SAMPLE_LIMIT = 500;

	private static final int TIPE_KEGIATAN = 1;
	private static final int TIPE_ORGANISASI = 2;
	private static final int TIPE_PRESTASI = 3;
	private static final int TIPE_PENGHARGAAN = 4;
	private static final int TIPE_CATATAN = 5;

	private Tbmuser tbmuser;
	private Mahasiswa currentMahasiswa;

	private Date dashboardFilterMulai;
	private Date dashboardFilterSampai;
	private Combobox dashboardFilterFakultas;
	private Combobox dashboardFilterJurusan;
	private Object selectedFakultas;
	private Object selectedJurusan;
	private String dashboardFilterKeyword = "";
	private int dashboardLoadToken = 0;

	public DasboardAktivitasMahasiswa() throws Exception {
		super();
		setWidth("100%");
		setMaximizedMode("whole");
		tbmuser = Common.getCurrentUser();
		currentMahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		init();
	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Aktivitas Mahasiswa");
		renderDashboard(this);
	}

	private void renderDashboard(final Component parent) throws Exception {
		prepareDashboardFilterDefaults();
		dashboardLoadToken++;
		DashboardLoadState state = tampilkanLoadingDashboardAktivitasMahasiswa(parent);
		state.token = dashboardLoadToken;
		scheduleDashboardLoadingStep(state);
	}

	private void prepareDashboardFilterDefaults() {
		tbmuser = Common.getCurrentUser();
		currentMahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();

		if (dashboardFilterMulai == null) {
			Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
			cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 3);
			dashboardFilterMulai = cal.getTime();
		}
		if (dashboardFilterSampai == null) {
			dashboardFilterSampai = new Date();
		}
		if (dashboardFilterKeyword == null) {
			dashboardFilterKeyword = "";
		}
	}

	private DashboardLoadState tampilkanLoadingDashboardAktivitasMahasiswa(final Component parent) {
		DashboardLoadState state = new DashboardLoadState();
		state.parent = parent;
		state.step = 0;
		state.data = new DashboardData();
		state.data.currentMahasiswa = currentMahasiswa;
		state.data.isMahasiswaLogin = currentMahasiswa != null;

		if (parent == null) {
			return state;
		}

		Common.clear(parent);

		MyPortalchildren wrapper = new MyPortalchildren();
		wrapper.setWidth("100%");
		wrapper.setStyle("padding:6px; box-sizing:border-box;");
		wrapper.setParent(parent);

		Panel panel = new Panel();
		panel.setTitle("Dasbor Aktivitas Mahasiswa");
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

		Vbox containerDasborGrid = new Vbox();
		containerDasborGrid.setWidth("100%");
		containerDasborGrid.setStyle("background:#f6f8fb; padding:16px; box-sizing:border-box;");
		containerDasborGrid.setParent(panelchildren);
		state.loadingHost = containerDasborGrid;

		Div card = new Div();
		card.setWidth("100%");
		card.setStyle("border-radius:18px; background:#ffffff; border:1px solid #e5e7eb;"
				+ "box-shadow:0 14px 30px rgba(15,23,42,.08); padding:18px; box-sizing:border-box;");
		card.setParent(containerDasborGrid);

		Html htmlLoading = new Html("<div style='display:flex; align-items:center; gap:10px; color:#0f172a;'>"
				+ "<span style='display:inline-block; width:24px; height:24px; border-radius:999px; border:3px solid #bfdbfe; border-top-color:#2563eb; animation:spin 1s linear infinite;'></span>"
				+ "<div>"
				+ "<div style='font-size:16px; font-weight:800;'>Sedang memproses Dasbor Aktivitas Mahasiswa...</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:3px;'>Mohon tunggu, sistem mengambil data kegiatan, organisasi, prestasi, karya/penghargaan, dan catatan mahasiswa secara bertahap.</div>"
				+ "</div>"
				+ "<style>@keyframes spin{from{transform:rotate(0deg)}to{transform:rotate(360deg)}}</style>"
				+ "</div>");
		htmlLoading.setParent(card);

		Div infoBox = new Div();
		infoBox.setStyle("margin-top:14px; display:flex; justify-content:space-between; align-items:center; gap:10px; flex-wrap:wrap;");
		infoBox.setParent(card);

		Label message = new Label(ais.common.Common.getBahasaConfig("Menyiapkan filter dan hak akses pengguna..."));
		message.setStyle("font-size:13px; color:#334155; font-weight:700;");
		message.setParent(infoBox);
		state.loadingMessage = message;

		Label percent = new Label("0%");
		percent.setStyle("font-size:13px; color:#2563eb; font-weight:900;");
		percent.setParent(infoBox);
		state.loadingPercent = percent;

		Div progressTrack = new Div();
		progressTrack.setWidth("100%");
		progressTrack.setStyle("height:12px; margin-top:10px; border-radius:999px; background:#e2e8f0; overflow:hidden;");
		progressTrack.setParent(card);

		Div progressBar = new Div();
		progressBar.setStyle("height:12px; width:0%; border-radius:999px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4)); transition:width .25s ease;");
		progressBar.setParent(progressTrack);
		state.loadingProgressBar = progressBar;

		Html hint = new Html("<div style='margin-top:12px; font-size:12px; color:#64748b;'>"
				+ "Progress ini berjalan per tahap query agar pengguna mengetahui data apa yang sedang diproses. "
				+ "Jika data sangat besar, dashboard tetap menampilkan indikator sampai proses selesai."
				+ "</div>");
		hint.setParent(card);

		updateDashboardLoadingProgress(state, "Menyiapkan filter dan hak akses pengguna...", 0);
		return state;
	}

	private void scheduleDashboardLoadingStep(final DashboardLoadState state) {
		try {
			if (state == null || state.token != dashboardLoadToken || state.loadingHost == null || state.loadingHost.getParent() == null) {
				return;
			}
			Timer timer = new Timer();
			timer.setDelay(120);
			timer.setRepeats(false);
			timer.addEventListener("onTimer", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						if (event != null && event.getTarget() != null) {
							event.getTarget().detach();
						}
					} catch (Exception e) {
						printDebug(e);
					}
					processDashboardLoadingStep(state);
				}
			});
			timer.setParent(state.loadingHost);
			timer.setRunning(true);
		} catch (Exception e) {
			printDebug(e);
			try {
				renderDashboardContent(state == null ? null : state.parent, state == null ? null : state.data);
			} catch (Exception ex) {
				printDebug(ex);
			}
		}
	}

	private void processDashboardLoadingStep(final DashboardLoadState state) throws Exception {
		if (state == null || state.token != dashboardLoadToken || state.parent == null) {
			return;
		}
		try {
			DashboardData d = state.data;
			if (d == null) {
				d = new DashboardData();
				d.currentMahasiswa = currentMahasiswa;
				d.isMahasiswaLogin = currentMahasiswa != null;
				state.data = d;
			}

			if (state.step == 0) {
				updateDashboardLoadingProgress(state, "Menyiapkan filter tanggal, fakultas, jurusan, keyword, dan hak akses mahasiswa...", 5);
			} else if (state.step == 1) {
				updateDashboardLoadingProgress(state, "Mengambil ringkasan Kegiatan Mahasiswa...", 10);
				d.kegiatan = countByTipe(TIPE_KEGIATAN, null);
			} else if (state.step == 2) {
				updateDashboardLoadingProgress(state, "Mengambil ringkasan Organisasi Mahasiswa...", 18);
				d.organisasi = countByTipe(TIPE_ORGANISASI, null);
			} else if (state.step == 3) {
				updateDashboardLoadingProgress(state, "Mengambil ringkasan Prestasi Mahasiswa...", 26);
				d.prestasi = countByTipe(TIPE_PRESTASI, null);
			} else if (state.step == 4) {
				updateDashboardLoadingProgress(state, "Mengambil ringkasan Karya/Penghargaan Mahasiswa...", 34);
				d.penghargaan = countByTipe(TIPE_PENGHARGAAN, null);
			} else if (state.step == 5) {
				updateDashboardLoadingProgress(state, "Mengambil ringkasan Catatan Mahasiswa...", 42);
				d.catatan = countByTipe(TIPE_CATATAN, null);
				d.total = d.kegiatan + d.organisasi + d.prestasi + d.penghargaan + d.catatan;
			} else if (state.step == 6) {
				updateDashboardLoadingProgress(state, "Menghitung data yang sudah disetujui...", 52);
				d.kegiatanDisetujui = countByTipe(TIPE_KEGIATAN, KegiatanKemahasiswaan.DISETUJUI);
				d.prestasiDisetujui = countByTipe(TIPE_PRESTASI, PrestasiMahasiswa.DISETUJUI);
				d.penghargaanDisetujui = countByTipe(TIPE_PENGHARGAAN, PenghargaanMahasiswa.DISETUJUI);
				d.totalDisetujui = d.kegiatanDisetujui + d.prestasiDisetujui + d.penghargaanDisetujui;
			} else if (state.step == 7) {
				updateDashboardLoadingProgress(state, "Menghitung status belum diproses, sedang diproses, dan ditolak...", 60);
				d.totalBelumDiproses = countStatusSemua(PrestasiMahasiswa.BELUM_DIPROSES);
				d.totalSedangDiproses = countStatusSemua(PrestasiMahasiswa.SEDANG_DIPROSES);
				d.totalDitolak = countStatusSemua(PrestasiMahasiswa.DITOLAK);
			} else if (state.step == 8) {
				updateDashboardLoadingProgress(state, "Menganalisis sampel data Kegiatan Mahasiswa...", 68);
				analyzeSampleRows(d, TIPE_KEGIATAN, listByTipe(TIPE_KEGIATAN, 0, SAMPLE_LIMIT, null));
			} else if (state.step == 9) {
				updateDashboardLoadingProgress(state, "Menganalisis sampel data Organisasi Mahasiswa...", 75);
				analyzeSampleRows(d, TIPE_ORGANISASI, listByTipe(TIPE_ORGANISASI, 0, SAMPLE_LIMIT, null));
			} else if (state.step == 10) {
				updateDashboardLoadingProgress(state, "Menganalisis sampel data Prestasi Mahasiswa...", 82);
				analyzeSampleRows(d, TIPE_PRESTASI, listByTipe(TIPE_PRESTASI, 0, SAMPLE_LIMIT, null));
			} else if (state.step == 11) {
				updateDashboardLoadingProgress(state, "Menganalisis sampel data Karya/Penghargaan Mahasiswa...", 89);
				analyzeSampleRows(d, TIPE_PENGHARGAAN, listByTipe(TIPE_PENGHARGAAN, 0, SAMPLE_LIMIT, null));
			} else if (state.step == 12) {
				updateDashboardLoadingProgress(state, "Menganalisis sampel data Catatan Mahasiswa...", 95);
				analyzeSampleRows(d, TIPE_CATATAN, listByTipe(TIPE_CATATAN, 0, SAMPLE_LIMIT, null));
			} else {
				updateDashboardLoadingProgress(state, "Menyusun tampilan dashboard dan komponen analitik...", 100);
				finalizeDashboardData(d);
				renderDashboardContent(state.parent, d);
				return;
			}
		} catch (Exception e) {
			printDebug(e);
			updateDashboardLoadingProgress(state, "Sebagian data gagal diproses, melanjutkan tahap berikutnya...", Math.min(98, getDashboardProgressByStep(state.step)));
		}

		state.step++;
		scheduleDashboardLoadingStep(state);
	}

	private int getDashboardProgressByStep(int step) {
		int[] values = new int[] { 5, 10, 18, 26, 34, 42, 52, 60, 68, 75, 82, 89, 95, 100 };
		if (step < 0) {
			return 0;
		}
		if (step >= values.length) {
			return 100;
		}
		return values[step];
	}

	private void updateDashboardLoadingProgress(DashboardLoadState state, String message, int percent) {
		try {
			if (state == null) {
				return;
			}
			if (percent < 0) {
				percent = 0;
			}
			if (percent > 100) {
				percent = 100;
			}
			if (state.loadingMessage != null) {
				state.loadingMessage.setValue(message == null ? "Memproses data dashboard..." : message);
			}
			if (state.loadingPercent != null) {
				state.loadingPercent.setValue(percent + "%");
			}
			if (state.loadingProgressBar != null) {
				state.loadingProgressBar.setStyle("height:12px; width:" + percent
						+ "%; border-radius:999px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4)); transition:width .25s ease;");
			}
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void renderDashboardContent(final Component parent, DashboardData data) throws Exception {
		if (parent == null) {
			return;
		}
		Common.clear(parent);

		if (data == null) {
			data = loadDashboardDataCached();
		}

		MyPortalchildren wrapper = new MyPortalchildren();
		wrapper.setWidth("100%");
		wrapper.setStyle("padding:6px; box-sizing:border-box;");
		wrapper.setParent(parent);

		Panel panel = new Panel();
		panel.setTitle("Dasbor Aktivitas Mahasiswa");
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

		Div shell = new Div();
		shell.setWidth("100%");
		shell.setStyle("background:#f6f8fb; padding:14px; box-sizing:border-box; overflow:auto;");
		shell.setParent(panelchildren);

		renderHero(shell, data);
		renderGlobalFilter(shell);
		renderMetricCards(shell, data);
		renderAnalyticSections(shell, data);
	}

	private void finalizeDashboardData(DashboardData d) {
		if (d == null) {
			return;
		}
		Collections.sort(d.recentItems, new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				DashboardItem a = (DashboardItem) o1;
				DashboardItem b = (DashboardItem) o2;
				if (a.waktu == null && b.waktu == null) {
					return 0;
				}
				if (a.waktu == null) {
					return 1;
				}
				if (b.waktu == null) {
					return -1;
				}
				return b.waktu.compareTo(a.waktu);
			}
		});
		while (d.recentItems.size() > 12) {
			d.recentItems.remove(d.recentItems.size() - 1);
		}
	}

	private String buildCacheKey() {
		return (tbmuser == null ? "0" : String.valueOf(tbmuser.getId()))
				+ "|" + (currentMahasiswa == null ? "" : currentMahasiswa.getId())
				+ "|" + (dashboardFilterMulai == null ? "0" : dashboardFilterMulai.getTime())
				+ "|" + (dashboardFilterSampai == null ? "0" : dashboardFilterSampai.getTime())
				+ "|" + String.valueOf(selectedFakultas)
				+ "|" + String.valueOf(selectedJurusan)
				+ "|" + (dashboardFilterKeyword == null ? "" : dashboardFilterKeyword.trim());
	}

	@SuppressWarnings("unchecked")
	private DashboardData loadDashboardDataCached() {
		String _k = buildCacheKey();
		Long _e = _EXPIRY.get(_k);
		if (_e != null && _e > System.currentTimeMillis() && _CACHE.containsKey(_k)) {
			return (DashboardData) _CACHE.get(_k);
		}
		DashboardData data = loadDashboardData();
		_CACHE.put(_k, data);
		_EXPIRY.put(_k, System.currentTimeMillis() + _TTL_MS);
		return data;
	}

	private DashboardData loadDashboardData() {
		DashboardData d = new DashboardData();
		d.currentMahasiswa = currentMahasiswa;
		d.isMahasiswaLogin = currentMahasiswa != null;

		d.kegiatan = countByTipe(TIPE_KEGIATAN, null);
		d.organisasi = countByTipe(TIPE_ORGANISASI, null);
		d.prestasi = countByTipe(TIPE_PRESTASI, null);
		d.penghargaan = countByTipe(TIPE_PENGHARGAAN, null);
		d.catatan = countByTipe(TIPE_CATATAN, null);
		d.total = d.kegiatan + d.organisasi + d.prestasi + d.penghargaan + d.catatan;

		d.kegiatanDisetujui = countByTipe(TIPE_KEGIATAN, KegiatanKemahasiswaan.DISETUJUI);
		d.prestasiDisetujui = countByTipe(TIPE_PRESTASI, PrestasiMahasiswa.DISETUJUI);
		d.penghargaanDisetujui = countByTipe(TIPE_PENGHARGAAN, PenghargaanMahasiswa.DISETUJUI);
		d.totalDisetujui = d.kegiatanDisetujui + d.prestasiDisetujui + d.penghargaanDisetujui;

		d.totalBelumDiproses = countStatusSemua(PrestasiMahasiswa.BELUM_DIPROSES);
		d.totalSedangDiproses = countStatusSemua(PrestasiMahasiswa.SEDANG_DIPROSES);
		d.totalDitolak = countStatusSemua(PrestasiMahasiswa.DITOLAK);

		analyzeSampleRows(d, TIPE_KEGIATAN, listByTipe(TIPE_KEGIATAN, 0, SAMPLE_LIMIT, null));
		analyzeSampleRows(d, TIPE_ORGANISASI, listByTipe(TIPE_ORGANISASI, 0, SAMPLE_LIMIT, null));
		analyzeSampleRows(d, TIPE_PRESTASI, listByTipe(TIPE_PRESTASI, 0, SAMPLE_LIMIT, null));
		analyzeSampleRows(d, TIPE_PENGHARGAAN, listByTipe(TIPE_PENGHARGAAN, 0, SAMPLE_LIMIT, null));
		analyzeSampleRows(d, TIPE_CATATAN, listByTipe(TIPE_CATATAN, 0, SAMPLE_LIMIT, null));

		finalizeDashboardData(d);

		return d;
	}

	private int countStatusSemua(String status) {
		return countByTipe(TIPE_KEGIATAN, status) + countByTipe(TIPE_PRESTASI, status)
				+ countByTipe(TIPE_PENGHARGAAN, status);
	}

	private int countByTipe(int tipe, String status) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Criteria criteria = createBaseCriteria(session, tipe, false);
			applyStatusRestriction(criteria, tipe, status);
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

	@SuppressWarnings("unchecked")
	private List listByTipe(int tipe, int page, int pageSize, String status) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Criteria criteria = createBaseCriteria(session, tipe, true);
			applyStatusRestriction(criteria, tipe, status);
			criteria.setFirstResult(page * pageSize);
			criteria.setMaxResults(pageSize);
			return criteria.list();
		} catch (Exception e) {
			printDebug(e);
			return new ArrayList();
		} finally {
			closeSessionSafely(session);
		}
	}

	private Criteria createBaseCriteria(Session session, int tipe, boolean order) {
		Criteria criteria = null;
		String tanggalField = getTanggalField(tipe);

		if (tipe == TIPE_KEGIATAN) {
			criteria = session.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class)
					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
					.createAlias("kegiatanKemahasiswaan", "kegiatan", Criteria.LEFT_JOIN);
		} else if (tipe == TIPE_ORGANISASI) {
			criteria = session.createCriteria(OrganisasiIntraKampusPunyaMahasiswa.class)
					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
					.createAlias("organisasiIntraKampus", "organisasi", Criteria.LEFT_JOIN);
		} else if (tipe == TIPE_PRESTASI) {
			criteria = session.createCriteria(PrestasiMahasiswa.class).createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
					.createAlias("cabangPrestasiMahasiswa", "cabang", Criteria.LEFT_JOIN)
					.createAlias("kategoriPrestasiMahasiswa", "kategori", Criteria.LEFT_JOIN);
		} else if (tipe == TIPE_PENGHARGAAN) {
			criteria = session.createCriteria(PenghargaanMahasiswa.class)
					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
					.createAlias("kategoriPenghargaan", "kategori", Criteria.LEFT_JOIN);
		} else {
			criteria = session.createCriteria(CatatanMahasiswa.class).createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
					.createAlias("jenisCatatanMahasiswa", "jenis", Criteria.LEFT_JOIN);
		}

		criteria.add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"), Restrictions.eq("mahasiswa.aktif", true)));

		if (currentMahasiswa != null) {
			criteria.add(Restrictions.eq("mahasiswa", currentMahasiswa));
		} else {
			if (selectedJurusan != null) {
				criteria.add(Restrictions.eq("mahasiswa.jurusan", selectedJurusan));
			}
			if (selectedFakultas != null) {
				criteria.add(Restrictions.eq("jurusan.fakultas", selectedFakultas));
			}
		}

		if (dashboardFilterMulai != null) {
			criteria.add(Restrictions.ge(tanggalField, dashboardFilterMulai));
		}
		if (dashboardFilterSampai != null) {
			criteria.add(Restrictions.le(tanggalField, dashboardFilterSampai));
		}

		applyKeywordRestriction(criteria, tipe);

		if (order) {
			criteria.addOrder(Order.desc(tanggalField));
			criteria.addOrder(Order.desc("id"));
		}

		return criteria;
	}

	private void applyStatusRestriction(Criteria criteria, int tipe, String status) {
		if (status == null || status.trim().isEmpty()) {
			return;
		}
		if (tipe == TIPE_KEGIATAN) {
			criteria.add(Restrictions.eq("kegiatan.status", status));
		} else if (tipe == TIPE_PRESTASI || tipe == TIPE_PENGHARGAAN) {
			criteria.add(Restrictions.eq("status", status));
		}
	}

	private void applyKeywordRestriction(Criteria criteria, int tipe) {
		if (dashboardFilterKeyword == null || dashboardFilterKeyword.trim().isEmpty()) {
			return;
		}
		String key = dashboardFilterKeyword.trim();

		Criterion mahasiswa = Restrictions.or(Restrictions.ilike("mahasiswa.nama", key, MatchMode.ANYWHERE),
				Restrictions.ilike("mahasiswa.nim", key, MatchMode.ANYWHERE));

		if (tipe == TIPE_KEGIATAN) {
			criteria.add(Restrictions.or(mahasiswa,
					Restrictions.or(Restrictions.ilike("kegiatan.nama", key, MatchMode.ANYWHERE),
							Restrictions.ilike("keterangan", key, MatchMode.ANYWHERE))));
		} else if (tipe == TIPE_ORGANISASI) {
			criteria.add(Restrictions.or(mahasiswa,
					Restrictions.or(Restrictions.ilike("organisasi.nama", key, MatchMode.ANYWHERE),
							Restrictions.ilike("keterangan", key, MatchMode.ANYWHERE))));
		} else if (tipe == TIPE_PRESTASI) {
			criteria.add(Restrictions.or(mahasiswa,
					Restrictions.or(Restrictions.ilike("nama", key, MatchMode.ANYWHERE),
							Restrictions.or(Restrictions.ilike("penyelenggara", key, MatchMode.ANYWHERE),
									Restrictions.ilike("capaian", key, MatchMode.ANYWHERE)))));
		} else if (tipe == TIPE_PENGHARGAAN) {
			criteria.add(Restrictions.or(mahasiswa,
					Restrictions.or(Restrictions.ilike("nama", key, MatchMode.ANYWHERE),
							Restrictions.ilike("capaian", key, MatchMode.ANYWHERE))));
		} else {
			criteria.add(Restrictions.or(mahasiswa,
					Restrictions.or(Restrictions.ilike("keterangan", key, MatchMode.ANYWHERE),
							Restrictions.ilike("jenis.nama", key, MatchMode.ANYWHERE))));
		}
	}

	private String getTanggalField(int tipe) {
		if (tipe == TIPE_PRESTASI || tipe == TIPE_PENGHARGAAN) {
			return "tanggal";
		}
		if (tipe == TIPE_CATATAN) {
			return "waktu";
		}
		return "mulai";
	}

	private void analyzeSampleRows(DashboardData d, int tipe, List rows) {
		if (rows == null) {
			return;
		}
		for (Iterator it = rows.iterator(); it.hasNext();) {
			Object row = it.next();
			Date waktu = getDateValue(row, tipe);
			String tahun = waktu == null ? "Tanpa Tanggal" : getYear(waktu);
			addCounter(d.perTahun, tahun + "|" + getTipeLabel(tipe));
			addCounter(d.perKategori, getKategoriText(row, tipe));
			addCounter(d.perStatus, getStatusText(row, tipe));
			d.recentItems.add(buildDashboardItem(row, tipe));
		}
	}

	private DashboardItem buildDashboardItem(Object row, int tipe) {
		DashboardItem item = new DashboardItem();
		item.tipe = tipe;
		item.tipeLabel = getTipeLabel(tipe);
		item.judul = getJudulText(row, tipe);
		item.mahasiswa = getMahasiswaText(row);
		item.kategori = getKategoriText(row, tipe);
		item.status = getStatusText(row, tipe);
		item.waktu = getDateValue(row, tipe);
		return item;
	}

	private void renderHero(Component parent, DashboardData d) {
		Div hero = new Div();
		hero.setStyle("position:relative; overflow:hidden; border-radius:18px; padding:22px 24px; color:#ffffff;"
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);"
				+ "box-shadow:0 14px 30px rgba(15,23,42,.18); box-sizing:border-box;");
		hero.setParent(parent);

		appendHtml(hero,
				"<div style='position:absolute; right:-60px; top:-70px; width:210px; height:210px; border-radius:999px; background:rgba(255,255,255,.12);'></div>"
						+ "<div style='position:absolute; right:90px; bottom:-70px; width:160px; height:160px; border-radius:999px; background:rgba(255,255,255,.10);'></div>");

		org.zkoss.zul.Hbox content = new org.zkoss.zul.Hbox();
		content.setWidth("100%");
		content.setPack("justify");
		content.setAlign("center");
		content.setStyle("position:relative; z-index:1; gap:16px; flex-wrap:wrap;");
		content.setParent(hero);

		Vbox titleBox = new Vbox();
		titleBox.setStyle("max-width:780px;");
		titleBox.setParent(content);

		String subTitle = d.isMahasiswaLogin ? "Mode mahasiswa aktif: dashboard hanya menampilkan data milik "
				+ safeHtml(getMahasiswaText(currentMahasiswa)) + "." : "Pantau data kemahasiswaan berdasarkan kegiatan, organisasi, prestasi, karya/penghargaan, dan catatan.";

		appendHtml(titleBox,
				"<div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;'>Student Activity Control Center</div>"
						+ "<div style='font-size:28px; line-height:1.15; font-weight:800; margin-top:6px;'>Dasbor Aktivitas Mahasiswa</div>"
						+ "<div style='font-size:13px; opacity:.90; margin-top:8px;'>" + subTitle
						+ " Angka utama dapat diklik untuk membuka detail data dengan paging.</div>");

		Div totalBox = new Div();
		totalBox.setStyle("min-width:210px; border-radius:16px; background:rgba(255,255,255,.16); padding:14px 16px;"
				+ "box-shadow:inset 0 0 0 1px rgba(255,255,255,.18); text-align:right;");
		totalBox.setParent(content);
		appendHtml(totalBox,
				"<div style='font-size:12px; opacity:.86;'>Total Data Dipantau</div>" + "<div style='font-size:34px; font-weight:900;'>"
						+ formatInt(d.total) + "</div>" + "<div style='font-size:12px; opacity:.86;'>Disetujui: "
						+ formatInt(d.totalDisetujui) + "</div>");
	}

	private void renderGlobalFilter(Component parent) {
		Div box = new Div();
		box.setStyle("margin-top:12px; padding:14px; background:#ffffff; border:1px solid #e8eef6;"
				+ "border-radius:16px; box-shadow:0 10px 26px rgba(15,23,42,0.04); box-sizing:border-box;");
		box.setParent(parent);

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("border:0; background:transparent; padding:0; display:flex; flex-wrap:wrap; align-items:center; gap:8px;");
		toolbar.setParent(box);

		new MyLabelAgakKecil("Mulai:").setParent(toolbar);
		final MyDatebox mulai = new MyDatebox(dashboardFilterMulai);
		mulai.setCols(10);
		mulai.setParent(toolbar);

		new MyLabelAgakKecil("Sampai:").setParent(toolbar);
		final MyDatebox sampai = new MyDatebox(dashboardFilterSampai);
		sampai.setCols(10);
		sampai.setParent(toolbar);

		if (currentMahasiswa == null) {
			new MyLabelAgakKecil("Fakultas:").setParent(toolbar);
			dashboardFilterFakultas = new Combobox();
			dashboardFilterFakultas.setCols(18);
			dashboardFilterFakultas.setParent(toolbar);
			new MyLabelAgakKecil("Jurusan:").setParent(toolbar);
			dashboardFilterJurusan = new Combobox();
			dashboardFilterJurusan.setCols(18);
			dashboardFilterJurusan.setParent(toolbar);
			try {
				Common.initFakultasDanJurusanDanSemua(dashboardFilterJurusan, dashboardFilterFakultas);
				Common.selectComboItem(dashboardFilterFakultas, selectedFakultas);
				Common.selectComboItem(dashboardFilterJurusan, selectedJurusan);
			} catch (Exception e) {
				printDebug(e);
			}
		}

		new MyLabelAgakKecil("Keyword:").setParent(toolbar);
		final Textbox keyword = new Textbox(dashboardFilterKeyword);
		keyword.setCols(24);
		keyword.setTooltiptext("Cari nama kegiatan/organisasi/prestasi/penghargaan/catatan, nama mahasiswa, atau NIM.");
		keyword.setParent(toolbar);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Terapkan", "/img/search.png");
		cari.setTooltiptext("Terapkan filter dasbor");
		cari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				dashboardFilterMulai = mulai.getValue();
				dashboardFilterSampai = sampai.getValue();
				dashboardFilterKeyword = keyword.getValue() == null ? "" : keyword.getValue().trim();
				if (currentMahasiswa == null) {
					selectedFakultas = dashboardFilterFakultas == null || dashboardFilterFakultas.getSelectedItem() == null
							? null
							: dashboardFilterFakultas.getSelectedItem().getValue();
					selectedJurusan = dashboardFilterJurusan == null || dashboardFilterJurusan.getSelectedItem() == null ? null
							: dashboardFilterJurusan.getSelectedItem().getValue();
				}
				renderDashboard(DasboardAktivitasMahasiswa.this);
			}
		});
		cari.setParent(toolbar);

		MyToolbarbuttonConfig reset = new MyToolbarbuttonConfig("Reset", "/img/cancel.gif");
		reset.setTooltiptext("Reset filter dasbor");
		reset.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				dashboardFilterMulai = null;
				dashboardFilterSampai = null;
				selectedFakultas = null;
				selectedJurusan = null;
				dashboardFilterKeyword = "";
				renderDashboard(DasboardAktivitasMahasiswa.this);
			}
		});
		reset.setParent(toolbar);
	}

	private void initFakultasJurusanCombos(Combobox fakultas, Combobox jurusan) throws Exception {
		Common.initFakultasDanJurusanDanSemua(jurusan, fakultas);
	}

	private void renderMetricCards(Component parent, DashboardData d) {
		Div wrap = new Div();
		wrap.setStyle("display:flex; flex-wrap:wrap; gap:10px; margin-top:12px;");
		wrap.setParent(parent);

		renderCard(wrap, "Total Aktivitas", d.total, "Semua data kemahasiswaan", "#0f172a", TIPE_KEGIATAN, null, true);
		renderCard(wrap, "Kegiatan", d.kegiatan, "Kegiatan mahasiswa", "#2563eb", TIPE_KEGIATAN, null, false);
		renderCard(wrap, "Organisasi", d.organisasi, "Organisasi intra kampus", "#0891b2", TIPE_ORGANISASI, null, false);
		renderCard(wrap, "Prestasi", d.prestasi, "Prestasi mahasiswa", "#7c3aed", TIPE_PRESTASI, null, false);
		renderCard(wrap, "Karya/Penghargaan", d.penghargaan, "Karya dan penghargaan", "#b45309", TIPE_PENGHARGAAN, null, false);
		renderCard(wrap, "Catatan", d.catatan, "Catatan mahasiswa", "#475569", TIPE_CATATAN, null, false);
	}

	private void renderCard(Component parent, String title, int value, String subtitle, String color, final int tipe,
			final String status, final boolean all) {
		Div card = new Div();
		card.setStyle("flex:1 1 170px; min-width:170px; background:#ffffff; border:1px solid #e8eef6; border-radius:16px;"
				+ "padding:14px; box-sizing:border-box; box-shadow:0 10px 26px rgba(15,23,42,0.05);");
		card.setParent(parent);

		appendHtml(card,
				"<div style='height:4px; width:42px; border-radius:999px; background:" + color + "; margin-bottom:10px;'></div>"
						+ "<div style='font-size:12px; color:#64748b; font-weight:700; text-transform:uppercase;'>"
						+ safeHtml(title) + "</div>");

		A angka = new A(formatInt(value));
		angka.setStyle("display:block; font-size:30px; line-height:1; font-weight:900; color:" + color
				+ "; text-decoration:none; margin-top:6px;");
		angka.setTooltiptext("Klik untuk melihat detail " + title);
		angka.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (all) {
					showAllDetailPopup("Total Aktivitas Mahasiswa");
				} else {
					showDetailPopup(getTipeLabel(tipe), tipe, status);
				}
			}
		});
		angka.setParent(card);

		appendHtml(card, "<div style='font-size:12px; color:#64748b; margin-top:8px;'>" + safeHtml(subtitle) + "</div>");
	}

	private void renderAnalyticSections(Component parent, DashboardData data) {
		MyPortallayout portal = new MyPortallayout();
		portal.setWidth("100%");
		portal.setStyle("margin-top:12px;");
		portal.setParent(parent);

		String half = Common.isMobile() ? "100%" : "50%";
		MyPortalchildren top = createPortalChild(portal, "100%");
		MyPortalchildren left = createPortalChild(portal, half);
		MyPortalchildren right = createPortalChild(portal, half);
		MyPortalchildren bottom = createPortalChild(portal, "100%");

		renderStatusPipeline(top, data);
		renderTrendTahunan(left, data);
		renderTopKategori(right, data);
		renderAktivitasTerbaru(left, data);
		renderQuickInsight(right, data);
		renderDetailMatrix(bottom, data);
	}

	private MyPortalchildren createPortalChild(Component parent, String width) {
		MyPortalchildren pc = new MyPortalchildren();
		pc.setWidth(width);
		pc.setStyle("padding:6px; box-sizing:border-box;");
		pc.setParent(parent);
		return pc;
	}

	private Panelchildren createPanel(Component parent, String title) {
		Panel panel = new Panel();
		panel.setTitle(title);
		panel.setBorder("normal");
		panel.setCollapsible(true);
		panel.setClosable(false);
		panel.setStyle("border-radius:16px; overflow:hidden; border:1px solid #e5e7eb; background:#ffffff;"
				+ "box-shadow:0 10px 24px rgba(15,23,42,.06); margin-bottom:12px;");
		panel.setParent(parent);
		Panelchildren children = new Panelchildren();
		children.setStyle("padding:12px; background:#ffffff;");
		children.setParent(panel);
		appendPanelDescriptionEndUserV27(children, title);
		return children;
	}

	private void renderStatusPipeline(Component parent, final DashboardData data) {
		Panelchildren pc = createPanel(parent, "Pipeline Persetujuan / Verifikasi");
		Div wrap = new Div();
		wrap.setStyle("display:flex; flex-wrap:wrap; gap:10px;");
		wrap.setParent(pc);
		renderStatusCard(wrap, "Belum Diproses", data.totalBelumDiproses, PrestasiMahasiswa.BELUM_DIPROSES, "#64748b");
		renderStatusCard(wrap, "Sedang Diproses", data.totalSedangDiproses, PrestasiMahasiswa.SEDANG_DIPROSES, "#2563eb");
		renderStatusCard(wrap, "Disetujui", data.totalDisetujui, PrestasiMahasiswa.DISETUJUI, "#16a34a");
		renderStatusCard(wrap, "Ditolak", data.totalDitolak, PrestasiMahasiswa.DITOLAK, "#dc2626");
		appendHtml(pc,
				"<div style='font-size:12px; color:#64748b; margin-top:8px;'>Catatan: status dihitung dari Kegiatan, Prestasi, dan Karya/Penghargaan. Organisasi dan Catatan tidak memakai status approval yang sama.</div>");
	}

	private void renderStatusCard(Component parent, String label, int value, final String status, String color) {
		Div card = new Div();
		card.setStyle("flex:1 1 180px; min-width:180px; padding:12px; border-radius:14px; background:#f8fafc; border:1px solid #e2e8f0;");
		card.setParent(parent);
		appendHtml(card, "<div style='font-size:12px; color:#64748b; font-weight:800;'>" + safeHtml(label) + "</div>");
		A a = new A(formatInt(value));
		a.setStyle("display:block; margin-top:4px; color:" + color + "; font-size:26px; font-weight:900; text-decoration:none;");
		a.setTooltiptext("Klik untuk melihat detail status " + label);
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showStatusAllDetailPopup(status);
			}
		});
		a.setParent(card);
	}

	private void renderTrendTahunan(Component parent, DashboardData data) {
		Panelchildren pc = createPanel(parent, "Tren Per Tahun");
		Grid grid = createSimpleGrid(new String[] { "Tahun", "Kegiatan", "Organisasi", "Prestasi", "Karya/Penghargaan", "Catatan" });
		grid.setParent(pc);
		Rows rows = new Rows();
		rows.setParent(grid);

		TreeMap tahunMap = new TreeMap();
		for (Iterator it = data.perTahun.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			String tahun = key.substring(0, key.indexOf("|"));
			tahunMap.put(tahun, tahun);
		}
		if (tahunMap.isEmpty()) {
			appendEmptyRow(rows, 6, "Belum ada data pada rentang filter.");
			return;
		}
		for (Iterator it = tahunMap.keySet().iterator(); it.hasNext();) {
			String tahun = (String) it.next();
			Row row = new Row();
			row.setParent(rows);
			new Label(tahun).setParent(row);
			new Label(formatInt(getCounter(data.perTahun, tahun + "|Kegiatan"))).setParent(row);
			new Label(formatInt(getCounter(data.perTahun, tahun + "|Organisasi"))).setParent(row);
			new Label(formatInt(getCounter(data.perTahun, tahun + "|Prestasi"))).setParent(row);
			new Label(formatInt(getCounter(data.perTahun, tahun + "|Karya/Penghargaan"))).setParent(row);
			new Label(formatInt(getCounter(data.perTahun, tahun + "|Catatan"))).setParent(row);
		}
	}

	private void renderTopKategori(Component parent, DashboardData data) {
		Panelchildren pc = createPanel(parent, "Top Kategori / Jenis Aktivitas");
		Grid grid = createSimpleGrid(new String[] { "Kategori / Jenis", "Jumlah" });
		grid.setParent(pc);
		Rows rows = new Rows();
		rows.setParent(grid);

		List keys = sortedCounterKeys(data.perKategori);
		if (keys.isEmpty()) {
			appendEmptyRow(rows, 2, "Belum ada data kategori.");
			return;
		}
		int limit = Math.min(10, keys.size());
		for (int i = 0; i < limit; i++) {
			String key = (String) keys.get(i);
			Row row = new Row();
			row.setParent(rows);
			new Label(key).setParent(row);
			new Label(formatInt(getCounter(data.perKategori, key))).setParent(row);
		}
	}

	private void renderAktivitasTerbaru(Component parent, DashboardData data) {
		Panelchildren pc = createPanel(parent, "Aktivitas Terbaru");
		Grid grid = createSimpleGrid(new String[] { "Tanggal", "Jenis", "Judul", "Mahasiswa", "Status" });
		grid.setParent(pc);
		Rows rows = new Rows();
		rows.setParent(grid);
		if (data.recentItems.isEmpty()) {
			appendEmptyRow(rows, 5, "Belum ada aktivitas terbaru.");
			return;
		}
		for (Iterator it = data.recentItems.iterator(); it.hasNext();) {
			DashboardItem item = (DashboardItem) it.next();
			Row row = new Row();
			row.setParent(rows);
			new Label(formatDate(item.waktu)).setParent(row);
			new Label(item.tipeLabel).setParent(row);
			new Label(item.judul).setParent(row);
			new Label(item.mahasiswa).setParent(row);
			new Label(item.status).setParent(row);
		}
	}

	private void renderQuickInsight(Component parent, DashboardData d) {
		Panelchildren pc = createPanel(parent, "Insight Singkat");
		String coverage = d.total <= 0 ? "0" : formatPercent(d.totalDisetujui, d.total);
		appendHtml(pc,
				"<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(180px,1fr)); gap:10px;'>"
						+ insightBox("Rasio Disetujui", coverage,
								"Kegiatan, prestasi, dan penghargaan yang sudah disetujui dibanding total data.")
						+ insightBox("Dalam Proses", formatInt(d.totalBelumDiproses + d.totalSedangDiproses),
								"Butuh perhatian operator untuk proses validasi/approval.")
						+ insightBox("Catatan Pembinaan", formatInt(d.catatan),
								"Jumlah catatan mahasiswa pada rentang filter yang sedang dipantau.")
						+ "</div>");
	}

	private String insightBox(String title, String value, String desc) {
		return "<div style='border-radius:14px; padding:12px; background:#f8fafc; border:1px solid #e2e8f0;'>"
				+ "<div style='font-size:12px; color:#64748b; font-weight:800;'>" + safeHtml(title) + "</div>"
				+ "<div style='font-size:24px; color:#0f172a; font-weight:900; margin-top:4px;'>" + safeHtml(value) + "</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:6px;'>" + safeHtml(desc) + "</div>" + "</div>";
	}

	private void renderDetailMatrix(Component parent, DashboardData d) {
		Panelchildren pc = createPanel(parent, "Matriks Ringkasan Dashboard");
		Grid grid = createSimpleGrid(new String[] { "Data", "Total", "Disetujui", "Keterangan" });
		grid.setParent(pc);
		Rows rows = new Rows();
		rows.setParent(grid);
		appendMatrixRow(rows, "Kegiatan Mahasiswa", d.kegiatan, d.kegiatanDisetujui, "Diambil dari KegiatanKemahasiswaanPunyaMahasiswa");
		appendMatrixRow(rows, "Organisasi Mahasiswa", d.organisasi, -1, "Diambil dari OrganisasiIntraKampusPunyaMahasiswa");
		appendMatrixRow(rows, "Prestasi Mahasiswa", d.prestasi, d.prestasiDisetujui, "Diambil dari PrestasiMahasiswa");
		appendMatrixRow(rows, "Karya/Penghargaan", d.penghargaan, d.penghargaanDisetujui, "Diambil dari PenghargaanMahasiswa");
		appendMatrixRow(rows, "Catatan Mahasiswa", d.catatan, -1, "Diambil dari CatatanMahasiswa");
	}

	private void appendMatrixRow(Rows rows, String label, int total, int disetujui, String ket) {
		Row row = new Row();
		row.setParent(rows);
		new Label(label).setParent(row);
		new Label(formatInt(total)).setParent(row);
		new Label(disetujui < 0 ? "-" : formatInt(disetujui)).setParent(row);
		new Label(ket).setParent(row);
	}

	private void showAllDetailPopup(String title) throws Exception {
		Window window = createDetailWindow(title);
		Vbox vbox = new Vbox();
		vbox.setWidth("100%");
		vbox.setParent(window);
		appendDetailBlock(vbox, "Kegiatan Mahasiswa", TIPE_KEGIATAN, null);
		appendDetailBlock(vbox, "Organisasi Mahasiswa", TIPE_ORGANISASI, null);
		appendDetailBlock(vbox, "Prestasi Mahasiswa", TIPE_PRESTASI, null);
		appendDetailBlock(vbox, "Karya/Penghargaan", TIPE_PENGHARGAAN, null);
		appendDetailBlock(vbox, "Catatan Mahasiswa", TIPE_CATATAN, null);
		window.onModal();
	}

	private void showStatusAllDetailPopup(String status) throws Exception {
		Window window = createDetailWindow("Detail Status: " + status);
		Vbox vbox = new Vbox();
		vbox.setWidth("100%");
		vbox.setParent(window);
		appendDetailBlock(vbox, "Kegiatan Mahasiswa", TIPE_KEGIATAN, status);
		appendDetailBlock(vbox, "Prestasi Mahasiswa", TIPE_PRESTASI, status);
		appendDetailBlock(vbox, "Karya/Penghargaan", TIPE_PENGHARGAAN, status);
		window.onModal();
	}

	private void showDetailPopup(String title, final int tipe, final String status) throws Exception {
		final Window window = createDetailWindow("Detail " + title);
		final Vbox box = new Vbox();
		box.setWidth("100%");
		box.setParent(window);
		renderPagedDetail(box, tipe, status);
		window.onModal();
	}

	private Window createDetailWindow(String title) {
		final Window window = new Window();
		window.setTitle(title);
		window.setWidth(Common.isMobile() ? "96%" : "86%");
		window.setHeight(Common.isMobile() ? "88%" : "82%");
		window.setBorder("normal");
		window.setClosable(true);
		window.setSizable(true);
		window.setPosition("center");
		window.setStyle("border-radius:14px; overflow:hidden;");

		// Window harus terpasang ke halaman sebelum doModal() / onModal() dipanggil.
		// Tanpa parent, onModal() melempar SuspendNotAllowedException: Not attached.
		try {
			org.zkoss.zk.ui.Page pg = org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl() != null
					? org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage() : null;
			if (pg != null) {
				window.setParent(pg.getFirstRoot());
			}
		} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardAktivitasMahasiswa.java:1116");
		}

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("border:0; background:#f8fafc; padding:8px;");
		toolbar.setParent(window);
		MyToolbarbuttonConfig close = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		close.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		close.setParent(toolbar);
		return window;
	}

	private void appendDetailBlock(Component parent, String title, final int tipe, final String status) {
		Panelchildren pc = createPanel(parent, title + (status == null ? "" : " - " + status));
		renderPagedDetail(pc, tipe, status);
	}

	private void renderPagedDetail(final Component parent, final int tipe, final String status) {
		Common.clear(parent);
		final Paging paging = new Paging();
		paging.setPageSize(DETAIL_PAGE_SIZE);
		paging.setTotalSize(countByTipe(tipe, status));
		paging.setDetailed(true);
		paging.setParent(parent);

		final Grid grid = createSimpleGrid(getDetailHeaders(tipe));
		grid.setParent(parent);
		final Rows rows = new Rows();
		rows.setParent(grid);

		EventListener reload = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				renderDetailRows(rows, tipe, paging.getActivePage(), status);
			}
		};
		paging.addEventListener("onPaging", reload);
		try {
			reload.onEvent(null);
		} catch (Exception e) {
			printDebug(e);
		}
	}

	private void renderDetailRows(Rows rows, int tipe, int page, String status) {
		Common.clear(rows);
		List list = listByTipe(tipe, page, DETAIL_PAGE_SIZE, status);
		if (list == null || list.isEmpty()) {
			appendEmptyRow(rows, getDetailHeaders(tipe).length, "Tidak ada data detail.");
			return;
		}
		for (Iterator it = list.iterator(); it.hasNext();) {
			Object rowObj = it.next();
			Row row = new Row();
			row.setParent(rows);
			if (tipe == TIPE_KEGIATAN) {
				new Label(formatDate(getDateValue(rowObj, tipe))).setParent(row);
				new Label(getMahasiswaText(rowObj)).setParent(row);
				new Label(getJudulText(rowObj, tipe)).setParent(row);
				new Label(valueOf(getNestedValue(rowObj, "jabatanKegiatanKemahasiswaan.nama"))).setParent(row);
				new Label(getStatusText(rowObj, tipe)).setParent(row);
			} else if (tipe == TIPE_ORGANISASI) {
				new Label(formatDate(getDateValue(rowObj, tipe))).setParent(row);
				new Label(getMahasiswaText(rowObj)).setParent(row);
				new Label(getJudulText(rowObj, tipe)).setParent(row);
				new Label(valueOf(getNestedValue(rowObj, "jabatanOrganisasiIntraKampus.nama"))).setParent(row);
				new Label(valueOf(getNestedValue(rowObj, "keterangan"))).setParent(row);
			} else if (tipe == TIPE_PRESTASI) {
				new Label(formatDate(getDateValue(rowObj, tipe))).setParent(row);
				new Label(getMahasiswaText(rowObj)).setParent(row);
				new Label(getJudulText(rowObj, tipe)).setParent(row);
				new Label(getKategoriText(rowObj, tipe)).setParent(row);
				new Label(getStatusText(rowObj, tipe)).setParent(row);
			} else if (tipe == TIPE_PENGHARGAAN) {
				new Label(formatDate(getDateValue(rowObj, tipe))).setParent(row);
				new Label(getMahasiswaText(rowObj)).setParent(row);
				new Label(getJudulText(rowObj, tipe)).setParent(row);
				new Label(getKategoriText(rowObj, tipe)).setParent(row);
				new Label(getStatusText(rowObj, tipe)).setParent(row);
			} else {
				new Label(formatDate(getDateValue(rowObj, tipe))).setParent(row);
				new Label(getMahasiswaText(rowObj)).setParent(row);
				new Label(getKategoriText(rowObj, tipe)).setParent(row);
				new Label(valueOf(getNestedValue(rowObj, "dosen.nama"))).setParent(row);
				new Label(valueOf(getNestedValue(rowObj, "keterangan"))).setParent(row);
			}
		}
	}

	private String[] getDetailHeaders(int tipe) {
		if (tipe == TIPE_KEGIATAN) {
			return new String[] { "Tanggal", "Mahasiswa", "Kegiatan", "Jabatan", "Status" };
		}
		if (tipe == TIPE_ORGANISASI) {
			return new String[] { "Tanggal", "Mahasiswa", "Organisasi", "Jabatan", "Keterangan" };
		}
		if (tipe == TIPE_PRESTASI) {
			return new String[] { "Tanggal", "Mahasiswa", "Prestasi", "Kategori/Cabang", "Status" };
		}
		if (tipe == TIPE_PENGHARGAAN) {
			return new String[] { "Tanggal", "Mahasiswa", "Karya/Penghargaan", "Kategori", "Status" };
		}
		return new String[] { "Tanggal", "Mahasiswa", "Jenis Catatan", "Dosen", "Keterangan" };
	}

	private Grid createSimpleGrid(String[] headers) {
		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		Columns columns = new Columns();
		columns.setParent(grid);
		for (int i = 0; i < headers.length; i++) {
			Column column = new Column(headers[i]);
			column.setParent(columns);
		}
		return grid;
	}

	private void appendEmptyRow(Rows rows, int span, String text) {
		Row row = new Row();
		row.setSpans(String.valueOf(span));
		row.setAlign("center");
		row.setParent(rows);
		new Label(text).setParent(row);
	}

	private String getTipeLabel(int tipe) {
		if (tipe == TIPE_KEGIATAN) {
			return "Kegiatan";
		}
		if (tipe == TIPE_ORGANISASI) {
			return "Organisasi";
		}
		if (tipe == TIPE_PRESTASI) {
			return "Prestasi";
		}
		if (tipe == TIPE_PENGHARGAAN) {
			return "Karya/Penghargaan";
		}
		return "Catatan";
	}

	private String getJudulText(Object row, int tipe) {
		if (row == null) {
			return "-";
		}
		if (tipe == TIPE_KEGIATAN) {
			return valueOf(getNestedValue(row, "kegiatanKemahasiswaan.nama"));
		}
		if (tipe == TIPE_ORGANISASI) {
			return valueOf(getNestedValue(row, "organisasiIntraKampus.nama"));
		}
		if (tipe == TIPE_CATATAN) {
			String jenis = valueOf(getNestedValue(row, "jenisCatatanMahasiswa.nama"));
			return jenis.length() == 0 || "-".equals(jenis) ? "Catatan Mahasiswa" : jenis;
		}
		return valueOf(getNestedValue(row, "nama"));
	}

	private String getKategoriText(Object row, int tipe) {
		if (tipe == TIPE_KEGIATAN) {
			String skala = valueOf(getNestedValue(row, "skalaKegiatanKemahasiswaan.nama"));
			String jabatan = valueOf(getNestedValue(row, "jabatanKegiatanKemahasiswaan.nama"));
			return joinNonEmpty("Kegiatan", skala, jabatan);
		}
		if (tipe == TIPE_ORGANISASI) {
			String organisasi = valueOf(getNestedValue(row, "organisasiIntraKampus.nama"));
			String jabatan = valueOf(getNestedValue(row, "jabatanOrganisasiIntraKampus.nama"));
			return joinNonEmpty("Organisasi", organisasi, jabatan);
		}
		if (tipe == TIPE_PRESTASI) {
			String kategori = valueOf(getNestedValue(row, "kategoriPrestasiMahasiswa.nama"));
			String cabang = valueOf(getNestedValue(row, "cabangPrestasiMahasiswa.nama"));
			return joinNonEmpty("Prestasi", kategori, cabang);
		}
		if (tipe == TIPE_PENGHARGAAN) {
			String kategori = valueOf(getNestedValue(row, "kategoriPenghargaan.nama"));
			return kategori == null || kategori.trim().isEmpty() || "-".equals(kategori) ? "Karya/Penghargaan" : kategori;
		}
		String jenis = valueOf(getNestedValue(row, "jenisCatatanMahasiswa.nama"));
		return jenis == null || jenis.trim().isEmpty() || "-".equals(jenis) ? "Catatan" : jenis;
	}

	private String getStatusText(Object row, int tipe) {
		if (tipe == TIPE_ORGANISASI || tipe == TIPE_CATATAN) {
			return "-";
		}
		if (tipe == TIPE_KEGIATAN) {
			return valueOf(getNestedValue(row, "kegiatanKemahasiswaan.status"));
		}
		return valueOf(getNestedValue(row, "status"));
	}

	private Date getDateValue(Object row, int tipe) {
		Object value = null;
		if (tipe == TIPE_PRESTASI || tipe == TIPE_PENGHARGAAN) {
			value = getNestedValue(row, "tanggal");
		} else if (tipe == TIPE_CATATAN) {
			value = getNestedValue(row, "waktu");
		} else {
			value = getNestedValue(row, "mulai");
		}
		return value instanceof Date ? (Date) value : null;
	}

	private String getMahasiswaText(Object row) {
		if (row == null) {
			return "-";
		}

		// Penting: jangan overload getMahasiswaText(Mahasiswa).
		// Pada beberapa pemanggilan Java bisa jatuh ke versi Object lagi, lalu recursive.
		if (row instanceof Mahasiswa) {
			return formatMahasiswaText((Mahasiswa) row);
		}

		Object mahasiswa = getNestedValue(row, "mahasiswa");
		if (mahasiswa instanceof Mahasiswa) {
			return formatMahasiswaText((Mahasiswa) mahasiswa);
		}
		return "-";
	}

	private String formatMahasiswaText(Mahasiswa mahasiswa) {
		if (mahasiswa == null) {
			return "-";
		}
		String nim = valueOf(getNestedValue(mahasiswa, "nim"));
		String nama = valueOf(getNestedValue(mahasiswa, "nama"));
		if (nim == null || nim.trim().isEmpty() || "-".equals(nim)) {
			return nama;
		}
		if (nama == null || nama.trim().isEmpty() || "-".equals(nama)) {
			return nim;
		}
		return nim + " - " + nama;
	}

	private Object getNestedValue(Object obj, String path) {
		try {
			if (obj == null || path == null || path.trim().isEmpty()) {
				return null;
			}
			Object current = obj;
			String[] parts = path.split("\\.");
			for (int i = 0; i < parts.length; i++) {
				if (current == null) {
					return null;
				}
				String name = parts[i];
				String methodName = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
				Method method = current.getClass().getMethod(methodName, new Class[0]);
				current = method.invoke(current, new Object[0]);
			}
			return current;
		} catch (Exception e) {
			return null;
		}
	}

	private String valueOf(Object obj) {
		if (obj == null) {
			return "-";
		}
		try {
			return obj.toString();
		} catch (Exception e) {
			return "-";
		}
	}

	private String joinNonEmpty(String fallback, String a, String b) {
		String result = "";
		if (a != null && !a.trim().isEmpty() && !"-".equals(a)) {
			result = a;
		}
		if (b != null && !b.trim().isEmpty() && !"-".equals(b)) {
			result = result.trim().isEmpty() ? b : result + " / " + b;
		}
		return result.trim().isEmpty() ? fallback : result;
	}

	private String getYear(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return String.valueOf(cal.get(Calendar.YEAR));
	}

	private void addCounter(Map map, String key) {
		if (key == null || key.trim().isEmpty() || "-".equals(key)) {
			key = "Tidak diketahui";
		}
		Integer current = (Integer) map.get(key);
		map.put(key, current == null ? new Integer(1) : new Integer(current.intValue() + 1));
	}

	private int getCounter(Map map, String key) {
		Integer value = (Integer) map.get(key);
		return value == null ? 0 : value.intValue();
	}

	private List sortedCounterKeys(final Map map) {
		List keys = new ArrayList(map.keySet());
		Collections.sort(keys, new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				Integer a = (Integer) map.get(o1);
				Integer b = (Integer) map.get(o2);
				int av = a == null ? 0 : a.intValue();
				int bv = b == null ? 0 : b.intValue();
				if (av == bv) {
					return String.valueOf(o1).compareTo(String.valueOf(o2));
				}
				return bv - av;
			}
		});
		return keys;
	}

	private String formatInt(int value) {
		try {
			return java.text.NumberFormat.getInstance().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String formatPercent(int numerator, int denominator) {
		if (denominator <= 0) {
			return "0%";
		}
		double value = (numerator * 100.0d) / denominator;
		try {
			return new java.text.DecimalFormat("#,##0.##").format(value) + "%";
		} catch (Exception e) {
			return String.valueOf((int) value) + "%";
		}
	}

	private String formatDate(Date date) {
		if (date == null) {
			return "-";
		}
		try {
			return Common.dateFormat1.get().format(date);
		} catch (Exception e) {
			return String.valueOf(date);
		}
	}

	private void appendHtml(Component parent, String html) {
		Html h = new Html(html);
		h.setParent(parent);
	}

	private String safeHtml(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private void printDebug(Exception e) {
		if (debug || debuh) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DasboardAktivitasMahasiswa.java:1485");
		}
	}

	private void closeSessionSafely(Session session) {
		ais.ui.util.DashboardModernHtmlUtil.closeOpenedSession(session);
	}


	private static class DashboardLoadState {
		Component parent;
		Vbox loadingHost;
		Label loadingMessage;
		Label loadingPercent;
		Div loadingProgressBar;
		DashboardData data;
		int step;
		int token;
	}

	private static class DashboardData {
		boolean isMahasiswaLogin;
		Mahasiswa currentMahasiswa;
		int kegiatan;
		int organisasi;
		int prestasi;
		int penghargaan;
		int catatan;
		int total;
		int kegiatanDisetujui;
		int prestasiDisetujui;
		int penghargaanDisetujui;
		int totalDisetujui;
		int totalBelumDiproses;
		int totalSedangDiproses;
		int totalDitolak;
		Map perTahun = new TreeMap();
		Map perKategori = new HashMap();
		Map perStatus = new HashMap();
		List recentItems = new ArrayList();
	}

	private static class DashboardItem {
		int tipe;
		String tipeLabel;
		String judul;
		String mahasiswa;
		String kategori;
		String status;
		Date waktu;
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
