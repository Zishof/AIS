package ais.action.master.dashboard.admin;

/* DASBOARD_UTAMA_KEPEGAWAIAN_V2_LOADING_PROGRESS_2026_05_30
 * Dibuat berdasarkan pola UI dashboard modern pada DasboardSop_TEMPLATE_TAMBAHAN_V7_V5.java
 * dan pengambilan data utama dari DasboardKepegawaian.java.
 */

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.A;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Html;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Toolbar;

import ais.action.master.PegawaiAction;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.TipeMasaKerja;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyPanelConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class DasboardUtamaKepegawaian extends MyPortallayout {

	private static final long serialVersionUID = -2908183257156207891L;
	private static final java.util.concurrent.ConcurrentHashMap<String, Object> _CACHE
			= new java.util.concurrent.ConcurrentHashMap<String, Object>();
	private static final java.util.concurrent.ConcurrentHashMap<String, Long> _EXPIRY
			= new java.util.concurrent.ConcurrentHashMap<String, Long>();
	private static final long _TTL_MS = 5L * 60 * 1000;
	public static boolean debug = false;
	public static boolean debuh = false;

	private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(new Locale("in", "ID"));

	private Tbmuser tbmuser;
	private AmbilDataSatuanKerjaBanbox filterSatuanKerja;
	private Combobox filterTipeMasaKerja;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private SatuanKerja dashboardFilterSatker;
	private TipeMasaKerja dashboardFilterTipeMasaKerja;

	/*
	 * Panel utama dashboard. Field ini sengaja dibuat agar proses loading dan
	 * progress bisa membersihkan/mengisi ulang area dashboard yang sama, mengikuti
	 * pola loading dashboard lain di aplikasi.
	 */
	private Component tabDashboardPanel;
	private Html dashboardLoadingHtml;

	public DasboardUtamaKepegawaian() throws Exception {
		super();
		setWidth("100%");
		setHeight("100%");
		setMaximizedMode("whole");
		setStyle("background:#f6f8fb; padding:0; margin:0;");
		tbmuser = Common.getCurrentUser();
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		tabDashboardPanel = this;
		renderDashboard();
	}

	private void renderDashboard() throws Exception {
		tampilkanLoadingDashboardKepegawaian();
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					renderDashboardContent();
				} catch (Exception e) {
					printDebug(e);
					tampilkanErrorLoadingDashboardKepegawaian(e);
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});
	}

	private void tampilkanLoadingDashboardKepegawaian() {
		if (tabDashboardPanel == null) {
			return;
		}

		Common.clear(tabDashboardPanel);
		dashboardLoadingHtml = null;

		MyPortalchildren wrapper = new MyPortalchildren();
		wrapper.setWidth("100%");
		wrapper.setStyle("padding:6px; box-sizing:border-box;");
		wrapper.setParent(tabDashboardPanel);

		Panel panel = new MyPanelConfig();
		panel.setTitle("Dasbor Utama Kepegawaian");
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
		containerDasborGrid.setStyle("background:#f6f8fb; padding:18px; box-sizing:border-box;");
		containerDasborGrid.setParent(panelchildren);

		dashboardLoadingHtml = new Html(buildLoadingDashboardHtml(3,
				"Menyiapkan Dasbor Utama Kepegawaian...",
				"Sistem sedang menyiapkan area dashboard, filter, dan komponen analitik. Mohon tidak menutup halaman sampai proses selesai."));
		containerDasborGrid.appendChild(dashboardLoadingHtml);
	}

	private void updateLoadingDashboardKepegawaian(int persen, String judul, String keterangan) {
		// FIX Error C (NPE getAttachedUiEngine via Html.setContent): dipanggil dari callback
		// CommonTimerHelper (Timer ZK) yang mungkin akhirnya fire SETELAH pengguna pindah
		// halaman/tab ditutup -- komponen sudah lepas dari Page. Skip diam-diam, ini kondisi
		// normal, bukan error.
		if (dashboardLoadingHtml == null || dashboardLoadingHtml.getPage() == null) {
			return;
		}
		if (persen < 0) {
			persen = 0;
		}
		if (persen > 100) {
			persen = 100;
		}
		dashboardLoadingHtml.setContent(buildLoadingDashboardHtml(persen, judul, keterangan));
	}

	private void tampilkanErrorLoadingDashboardKepegawaian(Exception e) {
		// FIX Error C (NPE getAttachedUiEngine via Html.setContent): sama seperti
		// updateLoadingDashboardKepegawaian di atas -- callback Timer ZK bisa fire setelah
		// komponen sudah lepas dari halaman (user sudah pindah/tutup tab). Skip diam-diam.
		if (dashboardLoadingHtml == null || dashboardLoadingHtml.getPage() == null) {
			return;
		}
		String pesan = e == null || e.getMessage() == null ? "Terjadi kesalahan saat memuat dashboard." : e.getMessage();
		dashboardLoadingHtml.setContent("<div style='padding:18px; border-radius:16px; background:#fff7ed; border:1px solid #fed7aa; color:#9a3412;'>"
				+ "<div style='font-size:15px; font-weight:900; margin-bottom:6px;'>Dashboard belum dapat dimuat</div>"
				+ "<div style='font-size:12px; line-height:1.55;'>" + escapeHtml(pesan) + "</div>"
				+ "</div>");
	}

	private String buildLoadingDashboardHtml(int persen, String judul, String keterangan) {
		if (persen < 0) {
			persen = 0;
		}
		if (persen > 100) {
			persen = 100;
		}
		String safeJudul = escapeHtml(judul == null ? "Memproses dashboard..." : judul);
		String safeKeterangan = escapeHtml(keterangan == null ? "" : keterangan);
		return "<div style='padding:22px; text-align:center; color:#475569;'>"
				+ "<div style='max-width:640px; margin:0 auto; background:#ffffff; border:1px solid #e5e7eb; border-radius:18px; padding:24px; box-shadow:0 14px 30px rgba(15,23,42,.08); box-sizing:border-box;'>"
				+ "<div style='width:54px; height:54px; border-radius:18px; display:flex; align-items:center; justify-content:center; margin:0 auto 12px auto; background:#dbeafe; color:#1d4ed8; font-size:24px;'>"
				+ "<i class=\"fa fa-spinner fa-spin\"></i></div>"
				+ "<div style='font-size:17px; font-weight:900; color:#0f172a;'>" + safeJudul + "</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:8px; line-height:1.55;'>" + safeKeterangan + "</div>"
				+ "<div style='margin-top:18px; display:flex; align-items:center; gap:12px;'>"
				+ "<div style='height:12px; flex:1; background:#e2e8f0; border-radius:999px; overflow:hidden;'>"
				+ "<div style='height:12px; width:" + persen + "%; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4)); border-radius:999px;'></div>"
				+ "</div>"
				+ "<div style='min-width:52px; text-align:right; font-size:13px; font-weight:900; color:#0f172a;'>" + persen + "%</div>"
				+ "</div>"
				+ "<div style='margin-top:10px; font-size:11px; color:#94a3b8;'>Progress ini menunjukkan tahapan pengambilan data dashboard kepegawaian.</div>"
				+ "</div></div>";
	}

	private void renderDashboardContent() throws Exception {
		if (tabDashboardPanel == null) {
			tabDashboardPanel = this;
		}
		updateLoadingDashboardKepegawaian(8, "Membaca filter dashboard...",
				"Sistem sedang membaca filter satuan kerja dan jenis kerja yang dipilih pengguna.");

		DashboardPegawaiData data = loadDashboardDataCached();

		updateLoadingDashboardKepegawaian(90, "Menyusun tampilan dashboard...",
				"Data sudah selesai dihitung. Sistem sedang menyusun kartu ringkasan, insight, dan panel analitik.");

		Common.clear(tabDashboardPanel);

		MyPortalchildren wrapper = new MyPortalchildren();
		wrapper.setWidth("100%");
		wrapper.setStyle("padding:6px; box-sizing:border-box;");
		wrapper.setParent(tabDashboardPanel);

		Panel panel = new MyPanelConfig();
		panel.setTitle("Dasbor Utama Kepegawaian");
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
		renderInsightCards(shell, data);
		renderAnalyticLayout(shell, data);
	}


	private String buildCacheKey() {
		return (tbmuser == null ? "0" : String.valueOf(tbmuser.getId()))
				+ "|" + (dashboardFilterSatker == null ? "" : dashboardFilterSatker.getId())
				+ "|" + (dashboardFilterTipeMasaKerja == null ? "" : dashboardFilterTipeMasaKerja.getId());
	}

	@SuppressWarnings("unchecked")
	private DashboardPegawaiData loadDashboardDataCached() {
		String _k = buildCacheKey();
		Long _e = _EXPIRY.get(_k);
		if (_e != null && _e > System.currentTimeMillis() && _CACHE.containsKey(_k)) {
			return (DashboardPegawaiData) _CACHE.get(_k);
		}
		DashboardPegawaiData data = loadDashboardData();
		_CACHE.put(_k, data);
		_EXPIRY.put(_k, System.currentTimeMillis() + _TTL_MS);
		return data;
	}

	private DashboardPegawaiData loadDashboardData() {
		DashboardPegawaiData data = new DashboardPegawaiData();
		try {
			updateLoadingDashboardKepegawaian(12, "Mengambil Ringkasan Pegawai Aktif...",
					"Menghitung total pegawai aktif sesuai filter satuan kerja dan jenis kerja.");
			data.totalPegawaiAktif = countPegawai(null);

			updateLoadingDashboardKepegawaian(22, "Mengambil Data Pegawai Tanpa Satuan Kerja...",
					"Menghitung pegawai aktif yang belum memiliki satuan kerja agar mudah ditindaklanjuti.");
			data.tanpaSatuanKerja = countPegawai(new CriteriaCustomizer() {
				@Override
				public void customize(Criteria criteria) throws Exception {
					criteria.add(Restrictions.isNull("satuanKerja"));
				}
			});
			data.denganSatuanKerja = Math.max(0, data.totalPegawaiAktif - data.tanpaSatuanKerja);

			updateLoadingDashboardKepegawaian(32, "Mengambil Data Pegawai Tanpa Jenis Kerja...",
					"Menghitung pegawai aktif yang belum memiliki jenis kerja / tipe masa kerja.");
			data.tanpaTipeMasaKerja = countPegawai(new CriteriaCustomizer() {
				@Override
				public void customize(Criteria criteria) throws Exception {
					criteria.add(Restrictions.isNull("tipeMasaKerja"));
				}
			});
			data.denganTipeMasaKerja = Math.max(0, data.totalPegawaiAktif - data.tanpaTipeMasaKerja);

			updateLoadingDashboardKepegawaian(44, "Mengambil Sebaran Pegawai per Satuan Kerja...",
					"Sistem sedang menghitung jumlah pegawai pada setiap satuan kerja dan turunannya.");
			data.perSatuanKerja = loadSatuanKerjaCounters();

			updateLoadingDashboardKepegawaian(70, "Mengambil Sebaran Pegawai per Jenis Kerja...",
					"Sistem sedang menghitung jumlah pegawai berdasarkan jenis kerja / tipe masa kerja.");
			data.perTipeMasaKerja = loadTipeMasaKerjaCounters();

			updateLoadingDashboardKepegawaian(84, "Menghitung Insight dan Prioritas Tindak Lanjut...",
					"Sistem sedang menentukan unit terbesar, jenis kerja dominan, dan kualitas data master pegawai.");
			data.unitTerisi = countFilledCounters(data.perSatuanKerja);
			data.jenisKerjaTerisi = countFilledCounters(data.perTipeMasaKerja);
			data.topSatuanKerja = getTopCounter(data.perSatuanKerja);
			data.topTipeMasaKerja = getTopCounter(data.perTipeMasaKerja);
		} catch (Exception e) {
			printDebug(e);
			Common.tampilErrorJikaAdmin(e);
		}
		return data;
	}

	private int countFilledCounters(List counters) {
		int count = 0;
		if (counters == null) {
			return count;
		}
		for (Iterator it = counters.iterator(); it.hasNext();) {
			CounterItem item = (CounterItem) it.next();
			if (item != null && item.value > 0 && !"Tidak Ditentukan".equals(item.label)) {
				count++;
			}
		}
		return count;
	}

	private CounterItem getTopCounter(List counters) {
		if (counters == null || counters.size() == 0) {
			return null;
		}
		CounterItem top = null;
		for (Iterator it = counters.iterator(); it.hasNext();) {
			CounterItem item = (CounterItem) it.next();
			if (item == null || item.value <= 0) {
				continue;
			}
			if (top == null || item.value > top.value) {
				top = item;
			}
		}
		return top;
	}

	private List loadSatuanKerjaCounters() throws Exception {
		List result = new ArrayList();
		List satuanKerjas = getSatuanKerjaScope();
		int totalSatkerProgress = satuanKerjas == null || satuanKerjas.size() == 0 ? 1 : satuanKerjas.size();
		int satkerIndexProgress = 0;
		for (Iterator it = satuanKerjas.iterator(); it.hasNext();) {
			satkerIndexProgress++;
			final SatuanKerja satuanKerja = (SatuanKerja) it.next();
			int progress = 44 + ((satkerIndexProgress * 22) / totalSatkerProgress);
			updateLoadingDashboardKepegawaian(progress, "Mengambil Sebaran Pegawai per Satuan Kerja...",
					"Memproses satuan kerja: " + (satuanKerja == null ? "Tidak Ditentukan" : satuanKerja.getNama()));
			int count = countPegawai(new CriteriaCustomizer() {
				@Override
				public void customize(Criteria criteria) throws Exception {
					if (satuanKerja == null) {
						criteria.add(Restrictions.isNull("satuanKerja"));
					} else {
						criteria.add(Restrictions.eq("satuanKerja", satuanKerja));
					}
				}
			});
			if (count > 0 || satuanKerja == null) {
				CounterItem item = new CounterItem();
				item.label = satuanKerja == null ? "Tidak Ditentukan" : satuanKerja.getNama();
				item.value = count;
				item.criteria = createPegawaiCriteria(new CriteriaCustomizer() {
					@Override
					public void customize(Criteria criteria) throws Exception {
						if (satuanKerja == null) {
							criteria.add(Restrictions.isNull("satuanKerja"));
						} else {
							criteria.add(Restrictions.eq("satuanKerja", satuanKerja));
						}
					}
				});
				result.add(item);
			}
		}
		return result;
	}

	private List loadTipeMasaKerjaCounters() throws Exception {
		List result = new ArrayList();
		List tipeList = getTipeMasaKerjaScope();
		int totalTipeProgress = tipeList == null || tipeList.size() == 0 ? 1 : tipeList.size();
		int tipeIndexProgress = 0;
		for (Iterator it = tipeList.iterator(); it.hasNext();) {
			tipeIndexProgress++;
			final TipeMasaKerja tipe = (TipeMasaKerja) it.next();
			int progress = 70 + ((tipeIndexProgress * 10) / totalTipeProgress);
			updateLoadingDashboardKepegawaian(progress, "Mengambil Sebaran Pegawai per Jenis Kerja...",
					"Memproses jenis kerja: " + (tipe == null ? "Tidak Ditentukan" : getNamaTipeMasaKerja(tipe)));
			int count = countPegawai(new CriteriaCustomizer() {
				@Override
				public void customize(Criteria criteria) throws Exception {
					if (tipe == null) {
						criteria.add(Restrictions.isNull("tipeMasaKerja"));
					} else {
						criteria.add(Restrictions.eq("tipeMasaKerja", tipe));
					}
				}
			});
			if (count > 0 || tipe == null) {
				CounterItem item = new CounterItem();
				item.label = tipe == null ? "Tidak Ditentukan" : getNamaTipeMasaKerja(tipe);
				item.value = count;
				item.criteria = createPegawaiCriteria(new CriteriaCustomizer() {
					@Override
					public void customize(Criteria criteria) throws Exception {
						if (tipe == null) {
							criteria.add(Restrictions.isNull("tipeMasaKerja"));
						} else {
							criteria.add(Restrictions.eq("tipeMasaKerja", tipe));
						}
					}
				});
				result.add(item);
			}
		}
		return result;
	}

	private String getNamaTipeMasaKerja(TipeMasaKerja tipe) {
		try {
			if (tipe != null && tipe.getNama() != null) {
				return tipe.getNama();
			}
		} catch (Exception e) {
			printDebug(e);
		}
		return "Tidak Ditentukan";
	}

	@SuppressWarnings("unchecked")
	private List getSatuanKerjaScope() throws Exception {
		List satuanKerjas;
		if (dashboardFilterSatker != null) {
			Set temp = new HashSet();
			temp.add(dashboardFilterSatker);
			satuanKerjaTreeModel.getChildsSet(dashboardFilterSatker, temp);
			satuanKerjas = new ArrayList(temp);
			Collections.sort(satuanKerjas);
		} else {
			satuanKerjas = new ArrayList(ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas());
			Collections.sort(satuanKerjas);
		}
		satuanKerjas.add(null);
		return satuanKerjas;
	}

	@SuppressWarnings("unchecked")
	private List getTipeMasaKerjaScope() throws Exception {
		List list = new ArrayList();
		if (dashboardFilterTipeMasaKerja != null) {
			list.add(dashboardFilterTipeMasaKerja);
			return list;
		}
		list = HibernateUtil.currentSession().createCriteria(TipeMasaKerja.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama")).list();
		list.add(null);
		return list;
	}

	private int countPegawai(CriteriaCustomizer customizer) throws Exception {
		Criteria criteria = createBasePegawaiCriteria();
		if (customizer != null) {
			customizer.customize(criteria);
		}
		criteria.setProjection(Projections.rowCount());
		Number number = (Number) criteria.uniqueResult();
		return number == null ? 0 : number.intValue();
	}

	private Criteria createBasePegawaiCriteria() throws Exception {
		Criteria criteria = HibernateUtil.currentSession().createCriteria(Pegawai.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		String satkerRestriction = buildSatuanKerjaSqlRestriction();
		if (satkerRestriction != null && satkerRestriction.trim().length() > 0) {
			criteria.add(Restrictions.sqlRestriction(satkerRestriction));
		}
		if (dashboardFilterTipeMasaKerja != null) {
			criteria.add(Restrictions.eq("tipeMasaKerja", dashboardFilterTipeMasaKerja));
		}
		return criteria;
	}

	private String buildSatuanKerjaSqlRestriction() throws Exception {
		if (dashboardFilterSatker == null) {
			return null;
		}
		Set temp = new HashSet();
		temp.add(dashboardFilterSatker);
		satuanKerjaTreeModel.getChildsSet(dashboardFilterSatker, temp);
		String inSatker = "";
		for (Iterator it = temp.iterator(); it.hasNext();) {
			SatuanKerja satuanKerja = (SatuanKerja) it.next();
			if (satuanKerja != null && satuanKerja.getId() != null) {
				inSatker += inSatker.length() == 0 ? satuanKerja.getId().toString() : "," + satuanKerja.getId();
			}
		}
		return inSatker.length() == 0 ? "true" : "(this_.satuan_kerja in (" + inSatker + ") or this_.satuan_kerja is null)";
	}

	private DataCriteriaWithColumn createPegawaiCriteria(final CriteriaCustomizer customizer) {
		return new DataCriteriaWithColumn() {
			@Override
			public Object[] initCriteria(boolean order) {
				try {
					Criteria criteria = createBasePegawaiCriteria();
					if (customizer != null) {
						customizer.customize(criteria);
					}
					return new Object[] { criteria, PegawaiAction.columns };
				} catch (Exception e) {
					printDebug(e);
					Common.tampilErrorJikaAdmin(e);
				}
				return null;
			}
		};
	}

	private void renderHero(Component parent, DashboardPegawaiData data) {
		org.zkoss.zul.Div hero = new org.zkoss.zul.Div();
		hero.setStyle("position:relative; overflow:hidden; border-radius:18px; padding:22px 24px; color:#ffffff;"
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);"
				+ "box-shadow:0 14px 30px rgba(15,23,42,.18); box-sizing:border-box;");
		hero.setParent(parent);

		appendHtml(hero, "<div style='position:absolute; right:-70px; top:-80px; width:230px; height:230px; border-radius:999px; background:rgba(255,255,255,.12);'></div>"
				+ "<div style='position:absolute; right:105px; bottom:-85px; width:170px; height:170px; border-radius:999px; background:rgba(255,255,255,.10);'></div>");

		org.zkoss.zul.Hbox content = new org.zkoss.zul.Hbox();
		content.setWidth("100%");
		content.setPack("justify");
		content.setAlign("center");
		content.setStyle("position:relative; z-index:1; gap:16px; flex-wrap:wrap;");
		content.setParent(hero);

		org.zkoss.zul.Vbox titleBox = new org.zkoss.zul.Vbox();
		titleBox.setStyle("max-width:760px;");
		titleBox.setParent(content);
		appendHtml(titleBox, "<div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;'>HR Control Center</div>"
				+ "<div style='font-size:28px; line-height:1.15; font-weight:800; margin-top:6px;'>Dasbor Utama Kepegawaian</div>"
				+ "<div style='font-size:13px; opacity:.90; margin-top:8px;'>Pantau total pegawai aktif, kelengkapan satuan kerja, jenis kerja, dan sebaran pegawai per unit dalam satu layar. Klik angka untuk membuka detail data pegawai.</div>");
		appendHtml(titleBox, "<div style='margin-top:12px; display:flex; gap:8px; flex-wrap:wrap;'>"
				+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>Satker: "
				+ escapeHtml(dashboardFilterSatker == null ? "Semua Satker" : dashboardFilterSatker.getNama()) + "</span>"
				+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>Jenis Kerja: "
				+ escapeHtml(dashboardFilterTipeMasaKerja == null ? "Semua" : getNamaTipeMasaKerja(dashboardFilterTipeMasaKerja)) + "</span>"
				+ "</div>");

		org.zkoss.zul.Hbox numberBox = new org.zkoss.zul.Hbox();
		numberBox.setStyle("gap:10px; flex-wrap:wrap;");
		numberBox.setParent(content);
		createHeroNumber(numberBox, "Pegawai Aktif", data.totalPegawaiAktif, "Detail Seluruh Pegawai Aktif", createPegawaiCriteria(null));
		createHeroNumber(numberBox, "Unit Terisi", data.unitTerisi, "Detail Pegawai yang Memiliki Satuan Kerja", createPegawaiCriteria(new CriteriaCustomizer() {
			@Override
			public void customize(Criteria criteria) throws Exception {
				criteria.add(Restrictions.isNotNull("satuanKerja"));
			}
		}));
	}

	private void renderGlobalFilter(final Component parent) throws Exception {
		final org.zkoss.zul.Div filterContainer = new org.zkoss.zul.Div();
		filterContainer.setParent(parent);
		filterContainer.setStyle("margin-top:12px; padding:14px; background:#ffffff; border:1px solid #e8eef6; "
				+ "border-radius:16px; box-shadow:0 10px 26px rgba(15,23,42,0.04); box-sizing:border-box;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(filterContainer);
		toolbar.setStyle("border:0; background:transparent; padding:0; display:flex; flex-wrap:wrap; align-items:center; gap:8px;");

		final EventListener refreshListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				dashboardFilterSatker = filterSatuanKerja == null ? null : (SatuanKerja) filterSatuanKerja.getAttribute("satuanKerja");
				if (filterTipeMasaKerja != null && filterTipeMasaKerja.getSelectedItem() != null) {
					dashboardFilterTipeMasaKerja = (TipeMasaKerja) filterTipeMasaKerja.getSelectedItem().getValue();
				} else {
					dashboardFilterTipeMasaKerja = null;
				}
				renderDashboard();
			}
		};

		new MyLabelAgakKecil("Satuan Kerja:").setParent(toolbar);
		filterSatuanKerja = new AmbilDataSatuanKerjaBanbox();
		filterSatuanKerja.setCols(16);
		filterSatuanKerja.setReadonly(true);
		filterSatuanKerja.setEventListener(refreshListener);
		if (dashboardFilterSatker != null) {
			filterSatuanKerja.setValue(dashboardFilterSatker.getNama());
			filterSatuanKerja.setAttribute("satuanKerja", dashboardFilterSatker);
		}
		filterSatuanKerja.setParent(toolbar);

		new MyLabelAgakKecil("Jenis Kerja:").setParent(toolbar);
		filterTipeMasaKerja = new Combobox();
		filterTipeMasaKerja.setReadonly(true);
		filterTipeMasaKerja.setParent(toolbar);
		Common.insertComboDanSemua(filterTipeMasaKerja, "nama", "keterangan", TipeMasaKerja.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		selectTipeMasaKerja(filterTipeMasaKerja, dashboardFilterTipeMasaKerja);
		filterTipeMasaKerja.addEventListener("onChange", refreshListener);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan Dasbor", "/img/svg/search.svg");
		refresh.setTooltiptext("Refresh dasbor kepegawaian berdasarkan filter global");
		refresh.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; padding:6px 14px; margin-left:4px;");
		refresh.setParent(toolbar);
		refresh.addEventListener("onClick", refreshListener);
	}

	private void selectTipeMasaKerja(Combobox combobox, TipeMasaKerja tipeMasaKerja) {
		if (combobox == null || tipeMasaKerja == null) {
			return;
		}
		for (Iterator it = combobox.getItems().iterator(); it.hasNext();) {
			Comboitem item = (Comboitem) it.next();
			Object value = item.getValue();
			if (value != null && value.equals(tipeMasaKerja)) {
				combobox.setSelectedItem(item);
				return;
			}
		}
	}

	private void renderMetricCards(Component parent, DashboardPegawaiData data) {
		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px;");
		wrap.setParent(parent);
		createMetricCard(wrap, "Pegawai Aktif", data.totalPegawaiAktif, "Total pegawai aktif sesuai filter", "#dbeafe", "#1e40af", "P", "Detail Seluruh Pegawai Aktif", createPegawaiCriteria(null));
		createMetricCard(wrap, "Dengan Satker", data.denganSatuanKerja, "Pegawai sudah memiliki satuan kerja", "#dcfce7", "#166534", "OK", "Detail Pegawai dengan Satuan Kerja", createPegawaiCriteria(new CriteriaCustomizer() {
			@Override
			public void customize(Criteria criteria) throws Exception {
				criteria.add(Restrictions.isNotNull("satuanKerja"));
			}
		}));
		createMetricCard(wrap, "Tanpa Satker", data.tanpaSatuanKerja, "Perlu dilengkapi unit kerja", "#fee2e2", "#991b1b", "!", "Detail Pegawai Tanpa Satuan Kerja", createPegawaiCriteria(new CriteriaCustomizer() {
			@Override
			public void customize(Criteria criteria) throws Exception {
				criteria.add(Restrictions.isNull("satuanKerja"));
			}
		}));
		createMetricCard(wrap, "Dengan Jenis Kerja", data.denganTipeMasaKerja, "Pegawai sudah memiliki jenis kerja", "#ede9fe", "#5b21b6", "T", "Detail Pegawai dengan Jenis Kerja", createPegawaiCriteria(new CriteriaCustomizer() {
			@Override
			public void customize(Criteria criteria) throws Exception {
				criteria.add(Restrictions.isNotNull("tipeMasaKerja"));
			}
		}));
		createMetricCard(wrap, "Tanpa Jenis Kerja", data.tanpaTipeMasaKerja, "Perlu dilengkapi jenis kerja", "#fef3c7", "#92400e", "?", "Detail Pegawai Tanpa Jenis Kerja", createPegawaiCriteria(new CriteriaCustomizer() {
			@Override
			public void customize(Criteria criteria) throws Exception {
				criteria.add(Restrictions.isNull("tipeMasaKerja"));
			}
		}));
		createMetricCard(wrap, "Unit Terisi", data.unitTerisi, "Jumlah unit yang memiliki pegawai", "#cffafe", "#155e75", "U", "Detail Pegawai yang Memiliki Satuan Kerja", createPegawaiCriteria(new CriteriaCustomizer() {
			@Override
			public void customize(Criteria criteria) throws Exception {
				criteria.add(Restrictions.isNotNull("satuanKerja"));
			}
		}));
	}

	private void renderInsightCards(Component parent, DashboardPegawaiData data) {
		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(240px,1fr)); gap:12px; margin-top:12px;");
		wrap.setParent(parent);

		int satkerCompleteness = percent(data.denganSatuanKerja, Math.max(1, data.totalPegawaiAktif));
		int tipeCompleteness = percent(data.denganTipeMasaKerja, Math.max(1, data.totalPegawaiAktif));
		String topUnit = data.topSatuanKerja == null ? "Belum ada data dominan" : data.topSatuanKerja.label + " (" + formatNumber(data.topSatuanKerja.value) + ")";
		String topTipe = data.topTipeMasaKerja == null ? "Belum ada data dominan" : data.topTipeMasaKerja.label + " (" + formatNumber(data.topTipeMasaKerja.value) + ")";

		appendHtml(wrap, buildInsightCard("Kelengkapan Satker", satkerCompleteness + "%", "Pegawai aktif yang sudah memiliki satuan kerja.", "#ecfdf5", "#166534"));
		appendHtml(wrap, buildInsightCard("Kelengkapan Jenis Kerja", tipeCompleteness + "%", "Pegawai aktif yang sudah memiliki jenis kerja / tipe masa kerja.", "#eff6ff", "#1e40af"));
		appendHtml(wrap, buildInsightCard("Unit Terbesar", escapeHtml(topUnit), "Satuan kerja dengan jumlah pegawai terbanyak pada filter saat ini.", "#f5f3ff", "#5b21b6"));
		appendHtml(wrap, buildInsightCard("Jenis Kerja Dominan", escapeHtml(topTipe), "Jenis kerja dengan jumlah pegawai terbanyak pada filter saat ini.", "#fff7ed", "#9a3412"));
	}

	private String buildInsightCard(String title, String value, String desc, String bg, String color) {
		return "<div style='border:1px solid #e5e7eb; border-radius:16px; padding:14px; background:#ffffff; box-shadow:0 10px 22px rgba(15,23,42,.05);'>"
				+ "<div style='display:inline-block; padding:5px 9px; border-radius:999px; background:" + bg + "; color:" + color + "; font-size:11px; font-weight:800;'>" + escapeHtml(title) + "</div>"
				+ "<div style='margin-top:12px; font-size:24px; line-height:1.2; font-weight:900; color:#0f172a;'>" + value + "</div>"
				+ "<div style='margin-top:6px; font-size:12px; color:#64748b; line-height:1.45;'>" + escapeHtml(desc) + "</div>"
				+ "</div>";
	}

	private void renderAnalyticLayout(Component parent, DashboardPegawaiData data) {
		MyPortallayout analyticLayout = new MyPortallayout();
		analyticLayout.setParent(parent);
		analyticLayout.setWidth("100%");
		analyticLayout.setMaximizedMode("whole");
		analyticLayout.setStyle("margin-top:10px; padding:0; background:transparent;");

		String pcWidth = Common.isMobile() ? "100%" : "50%";

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

		renderSebaranUnitKerja(pcLeft, data);
		renderSebaranJenisKerja(pcRight, data);
		renderKualitasDataPegawai(pcLeft, data);
		renderPrioritasTindakLanjut(pcRight, data);
		renderRencanaEksekusi(pcBottom, data);
	}

	private void renderSebaranUnitKerja(Component parent, DashboardPegawaiData data) {
		Panelchildren pch = createModernPanel("Sebaran Pegawai per Satuan Kerja", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px; line-height:1.55;'>Mengikuti pola tab Data pada dashboard kepegawaian existing: pegawai dihitung berdasarkan satuan kerja, termasuk turunan satker jika filter dipilih.</div>");
		renderCounterRows(pch, data.perSatuanKerja, "Belum ada data satuan kerja pada filter ini.", "#2563eb");
	}

	private void renderSebaranJenisKerja(Component parent, DashboardPegawaiData data) {
		Panelchildren pch = createModernPanel("Sebaran Pegawai per Jenis Kerja", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px; line-height:1.55;'>Jenis kerja menggunakan field <b>tipeMasaKerja</b> seperti filter yang sudah dipakai di tab Data.</div>");
		renderCounterRows(pch, data.perTipeMasaKerja, "Belum ada data jenis kerja pada filter ini.", "#7c3aed");
	}

	private void renderKualitasDataPegawai(Component parent, DashboardPegawaiData data) {
		Panelchildren pch = createModernPanel("Kualitas Data Master Pegawai", parent);
		int satkerCompleteness = percent(data.denganSatuanKerja, Math.max(1, data.totalPegawaiAktif));
		int tipeCompleteness = percent(data.denganTipeMasaKerja, Math.max(1, data.totalPegawaiAktif));
		renderGaugeRow(pch, "Kelengkapan Satuan Kerja", satkerCompleteness, "Pegawai aktif yang sudah memiliki satuan kerja.", "#16a34a");
		renderGaugeRow(pch, "Kelengkapan Jenis Kerja", tipeCompleteness, "Pegawai aktif yang sudah memiliki jenis kerja / tipe masa kerja.", "#2563eb");
		renderGaugeRow(pch, "Data Perlu Dilengkapi", percent(data.tanpaSatuanKerja + data.tanpaTipeMasaKerja, Math.max(1, data.totalPegawaiAktif * 2)), "Gabungan sinyal data kosong pada satker dan jenis kerja.", "#dc2626");
	}

	private void renderPrioritasTindakLanjut(Component parent, DashboardPegawaiData data) {
		Panelchildren pch = createModernPanel("Watchlist Prioritas Kepegawaian", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>Gunakan bagian ini untuk melihat area yang paling perlu ditindaklanjuti oleh admin kepegawaian.</div>");
		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		wrap.setParent(pch);
		createPressureCard(wrap, "Tanpa Satker", data.tanpaSatuanKerja, "Lengkapi satuan kerja pegawai", "#fee2e2", "#991b1b", "Detail Pegawai Tanpa Satuan Kerja", createPegawaiCriteria(new CriteriaCustomizer() {
			@Override
			public void customize(Criteria criteria) throws Exception {
				criteria.add(Restrictions.isNull("satuanKerja"));
			}
		}));
		createPressureCard(wrap, "Tanpa Jenis Kerja", data.tanpaTipeMasaKerja, "Lengkapi jenis kerja pegawai", "#fef3c7", "#92400e", "Detail Pegawai Tanpa Jenis Kerja", createPegawaiCriteria(new CriteriaCustomizer() {
			@Override
			public void customize(Criteria criteria) throws Exception {
				criteria.add(Restrictions.isNull("tipeMasaKerja"));
			}
		}));
		createPressureCard(wrap, "Pegawai Aktif", data.totalPegawaiAktif, "Basis data aktif sesuai filter", "#dbeafe", "#1e40af", "Detail Seluruh Pegawai Aktif", createPegawaiCriteria(null));
	}

	private void renderRencanaEksekusi(Component parent, DashboardPegawaiData data) {
		Panelchildren pch = createModernPanel("Prioritas Eksekusi Data Kepegawaian", parent);
		String prioritas1 = data.tanpaSatuanKerja > 0 ? "Lengkapi satuan kerja untuk " + formatNumber(data.tanpaSatuanKerja) + " pegawai agar laporan unit kerja lebih akurat."
				: "Kelengkapan satuan kerja relatif aman pada filter saat ini.";
		String prioritas2 = data.tanpaTipeMasaKerja > 0 ? "Lengkapi jenis kerja / tipe masa kerja untuk " + formatNumber(data.tanpaTipeMasaKerja) + " pegawai."
				: "Kelengkapan jenis kerja relatif aman pada filter saat ini.";
		String prioritas3 = data.topSatuanKerja == null ? "Belum ada unit kerja dominan yang perlu dipantau."
				: "Pantau beban pegawai pada unit " + data.topSatuanKerja.label + " karena memiliki jumlah pegawai terbesar pada filter saat ini.";
		String prioritas4 = data.topTipeMasaKerja == null ? "Belum ada jenis kerja dominan yang perlu dipantau."
				: "Jenis kerja dominan: " + data.topTipeMasaKerja.label + ". Gunakan sebagai dasar validasi komposisi SDM.";

		String html = "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(230px,1fr)); gap:12px;'>"
				+ buildActionPlanCard("1", "Kelengkapan Satker", prioritas1, "#fee2e2", "#991b1b")
				+ buildActionPlanCard("2", "Kelengkapan Jenis Kerja", prioritas2, "#fef3c7", "#92400e")
				+ buildActionPlanCard("3", "Monitoring Unit", prioritas3, "#dbeafe", "#1e40af")
				+ buildActionPlanCard("4", "Komposisi SDM", prioritas4, "#ede9fe", "#5b21b6")
				+ "</div>";
		appendHtml(pch, html);
	}

	private Panelchildren createModernPanel(String title, Component parent) {
		Panel panel = new MyPanelConfig();
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

	private void createMetricCard(Component parent, String title, int value, String desc, String bg, String color, String icon,
			String detailTitle, DataCriteriaWithColumn criteria) {
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
		createDetailNumber(top, formatNumber(value), detailTitle, criteria, "font-size:26px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer;");

		appendHtml(card, "<div style='font-size:12px; color:#64748b; margin-top:10px;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:#94a3b8; margin-top:3px;'>" + escapeHtml(desc) + "</div>");
	}

	private void createHeroNumber(Component parent, String title, int value, String detailTitle, DataCriteriaWithColumn criteria) {
		org.zkoss.zul.Div box = new org.zkoss.zul.Div();
		box.setStyle("min-width:132px; border-radius:16px; padding:12px 14px; background:rgba(255,255,255,.14); border:1px solid rgba(255,255,255,.24); backdrop-filter:blur(4px);");
		box.setParent(parent);
		createDetailNumber(box, formatNumber(value), detailTitle, criteria, "font-size:28px; line-height:1; font-weight:900; color:#ffffff; text-decoration:none; cursor:pointer;");
		appendHtml(box, "<div style='font-size:11px; margin-top:8px; opacity:.86;'>" + escapeHtml(title) + "</div>");
	}

	private void createPressureCard(Component parent, String title, int value, String desc, String bg, String color,
			String detailTitle, DataCriteriaWithColumn criteria) {
		org.zkoss.zul.Div card = new org.zkoss.zul.Div();
		card.setStyle("flex:1 1 160px; min-width:160px; border-radius:16px; padding:14px; background:" + bg + "; border:1px solid rgba(148,163,184,.35);");
		card.setParent(parent);
		createDetailNumber(card, formatNumber(value), detailTitle, criteria, "font-size:26px; line-height:1; font-weight:900; color:" + color + "; text-decoration:none; cursor:pointer;");
		appendHtml(card, "<div style='font-size:12px; font-weight:800; color:" + color + "; margin-top:8px;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:#64748b; margin-top:4px; line-height:1.35;'>" + escapeHtml(desc) + "</div>");
	}

	private void renderCounterRows(Component parent, List counters, String emptyText, String color) {
		if (counters == null || counters.size() == 0) {
			appendHtml(parent, "<div style='padding:14px; border-radius:14px; background:#f8fafc; color:#64748b; font-size:12px;'>" + escapeHtml(emptyText) + "</div>");
			return;
		}
		int max = 1;
		for (Iterator it = counters.iterator(); it.hasNext();) {
			CounterItem item = (CounterItem) it.next();
			if (item != null && item.value > max) {
				max = item.value;
			}
		}
		int i = 0;
		for (Iterator it = counters.iterator(); it.hasNext();) {
			CounterItem item = (CounterItem) it.next();
			if (item == null) {
				continue;
			}
			renderCounterRow(parent, item, max, color);
			i++;
			if (i >= 12) {
				break;
			}
		}
	}

	private void renderCounterRow(Component parent, CounterItem item, int max, String color) {
		org.zkoss.zul.Div row = new org.zkoss.zul.Div();
		row.setStyle("padding:8px 0; border-bottom:1px solid #e2e8f0;");
		row.setParent(parent);

		org.zkoss.zul.Hbox top = new org.zkoss.zul.Hbox();
		top.setWidth("100%");
		top.setPack("justify");
		top.setAlign("center");
		top.setParent(row);
		appendHtml(top, "<div style='font-size:12px; font-weight:800; color:#0f172a; max-width:75%; white-space:normal;'>" + escapeHtml(item.label) + "</div>");
		createDetailNumber(top, formatNumber(item.value), "Detail Pegawai - " + item.label, item.criteria, "font-size:12px; font-weight:900; color:#0f172a; text-decoration:none; cursor:pointer;");

		int width = max <= 0 ? 0 : ((item.value * 100) / max);
		appendHtml(row, "<div style='height:8px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:7px;'>"
				+ "<div style='height:8px; width:" + width + "%; background:" + color + "; border-radius:999px;'></div>"
				+ "</div>");
	}

	private void renderGaugeRow(Component parent, String title, int pct, String desc, String color) {
		if (pct < 0) {
			pct = 0;
		}
		if (pct > 100) {
			pct = 100;
		}
		org.zkoss.zul.Div row = new org.zkoss.zul.Div();
		row.setStyle("padding:9px 0; border-bottom:1px solid #e2e8f0;");
		row.setParent(parent);
		appendHtml(row, "<div style='display:flex; justify-content:space-between; gap:10px; align-items:center;'>"
				+ "<div><div style='font-size:12px; font-weight:900; color:#0f172a;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:#64748b; margin-top:3px;'>" + escapeHtml(desc) + "</div></div>"
				+ "<div style='font-size:18px; font-weight:900; color:" + color + ";'>" + pct + "%</div></div>"
				+ "<div style='height:9px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:8px;'>"
				+ "<div style='height:9px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div></div>");
	}

	private void createDetailNumber(Component parent, String text, final String title, final DataCriteriaWithColumn criteria, String style) {
		A a = new A(text == null ? "0" : text);
		a.setStyle(style);
		a.setTooltiptext("Klik untuk melihat " + title);
		a.setParent(parent);
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showPegawaiDetail(title, criteria);
			}
		});
	}

	private void showPegawaiDetail(String title, DataCriteriaWithColumn criteria) throws Exception {
		if (criteria == null) {
			return;
		}
		EventListener eventListener = (EventListener) Common
				.cetakDataCustomButton(Pegawai.class, criteria, null, "Download Data", "/img/print.png", null, null, false, null,
						title == null ? "DATA PEGAWAI" : title, createEmptyColumns())
				.getAttribute("eventListener");
		if (eventListener != null) {
			eventListener.onEvent(null);
		}
	}

	private String[] createEmptyColumns() {
		String[] columns = new String[160];
		for (int i = 0; i < columns.length; i++) {
			columns[i] = "";
		}
		return columns;
	}

	private String buildActionPlanCard(String no, String title, String desc, String bg, String color) {
		return "<div style='border:1px solid #e5e7eb; border-radius:16px; padding:14px; background:#ffffff; box-shadow:0 10px 22px rgba(15,23,42,.05);'>"
				+ "<div style='width:30px; height:30px; border-radius:10px; display:flex; align-items:center; justify-content:center; background:" + bg + "; color:" + color + "; font-weight:900;'>" + escapeHtml(no) + "</div>"
				+ "<div style='font-size:13px; font-weight:900; color:#0f172a; margin-top:10px;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:12px; color:#64748b; line-height:1.5; margin-top:6px;'>" + escapeHtml(desc) + "</div>"
				+ "</div>";
	}

	private int percent(int value, int total) {
		if (total <= 0) {
			return 0;
		}
		return (int) Math.round((value * 100.0d) / total);
	}

	private String formatNumber(int value) {
		return NUMBER_FORMAT.format(value);
	}

	private void appendHtml(Component parent, String html) {
		Html h = new Html(html == null ? "" : html);
		h.setParent(parent);
	}

	private String escapeHtml(String text) {
		if (text == null) {
			return "";
		}
		String s = text;
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&#39;");
		return s;
	}

	private void printDebug(Exception e) {
		if ((debug || debuh) && e != null) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DasboardUtamaKepegawaian.java:974");
		}
	}

	private interface CriteriaCustomizer {
		void customize(Criteria criteria) throws Exception;
	}

	private static class DashboardPegawaiData {
		int totalPegawaiAktif;
		int denganSatuanKerja;
		int tanpaSatuanKerja;
		int denganTipeMasaKerja;
		int tanpaTipeMasaKerja;
		int unitTerisi;
		int jenisKerjaTerisi;
		List perSatuanKerja = new ArrayList();
		List perTipeMasaKerja = new ArrayList();
		CounterItem topSatuanKerja;
		CounterItem topTipeMasaKerja;
	}

	private static class CounterItem {
		String label;
		int value;
		DataCriteriaWithColumn criteria;
	}
}
