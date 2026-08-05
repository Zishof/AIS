package ais.action.master.antarjemput;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.DashboardCacheUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.antarjemput.DetailPenjemputanAntarJemput;
import ais.database.model.antarjemput.JadwalAntarJemput;
import ais.database.model.antarjemput.KartuPenjemputAntarJemput;
import ais.database.model.antarjemput.KendaraanAntarJemput;
import ais.database.model.antarjemput.LogNotifikasiAntarJemput;
import ais.database.model.antarjemput.PesertaJadwalAntarJemput;
import ais.database.model.antarjemput.RuteAntarJemput;
import ais.database.model.antarjemput.TransaksiPenjemputanAntarJemput;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyPanelConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class DasboardAntarJemput extends MyPortallayout {

	private static final long serialVersionUID = 1L;
	private static final int SAMPLE_LIMIT = 300;
	private Date filterMulai;
	private Date filterSampai;
	private String filterKeyword = "";
	private transient org.zkoss.zul.Html loadingHtml;

	public DasboardAntarJemput() throws Exception {
		super();
		setWidth("100%");
		setMaximizedMode("whole");
		init();
	}

	private void init() throws Exception {
		renderAsync();
	}

	private void renderAsync() throws Exception {
		tampilkanLoading("Menyiapkan Dasboard Antar Jemput...", 10);
		final AntarJemputDashboardData[] ref = new AntarJemputDashboardData[1];
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				updateLoading("Menghitung armada, rute, jadwal, peserta, scan, dan notifikasi...", 35);
				ref[0] = loadDashboardDataWithCache();
				updateLoading("Merender visual operasional antar jemput...", 80);
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						renderDashboard(ref[0]);
					}
				});
			}
		});
	}

	private AntarJemputDashboardData loadDashboardDataWithCache() {
		String fp = (filterMulai  != null ? String.valueOf(filterMulai.getTime())  : "0")
				+ "_" + (filterSampai != null ? String.valueOf(filterSampai.getTime()) : "0")
				+ "_" + (filterKeyword != null ? filterKeyword : "");
		String key = DashboardCacheUtil.keyWithFilter("DasboardAntarJemput", "ADMIN", null, fp);
		Object fromL2 = DashboardCacheUtil.getL2(key);
		if (fromL2 instanceof AntarJemputDashboardData) return (AntarJemputDashboardData) fromL2;
		Object fromL3 = DashboardCacheUtil.getL3(key);
		if (fromL3 instanceof AntarJemputDashboardData) {
			DashboardCacheUtil.putL2(key, fromL3);
			return (AntarJemputDashboardData) fromL3;
		}
		AntarJemputDashboardData d = loadDashboardData();
		DashboardCacheUtil.putL2(key, d);
		DashboardCacheUtil.putL3(key, d);
		return d;
	}

	private AntarJemputDashboardData loadDashboardData() {
		AntarJemputDashboardData d = new AntarJemputDashboardData();
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			d.totalKendaraan = count(session, KendaraanAntarJemput.class, null, null);
			d.kendaraanAktif = countActive(session, KendaraanAntarJemput.class);
			d.totalRute = count(session, RuteAntarJemput.class, null, null);
			d.ruteAktif = countActive(session, RuteAntarJemput.class);
			d.totalJadwal = count(session, JadwalAntarJemput.class, "tanggal", new String[] { "kode", "nama", "hari", "status" });
			d.jadwalAktif = countActive(session, JadwalAntarJemput.class);
			d.totalPeserta = count(session, PesertaJadwalAntarJemput.class, null,
					new String[] { "kode", "nama", "titikJemput", "titikTurun", "statusLangganan" });
			d.pesertaAktif = countActive(session, PesertaJadwalAntarJemput.class);
			d.totalKartu = count(session, KartuPenjemputAntarJemput.class, null,
					new String[] { "kode", "nama", "namaPenjemput", "nomorKartu", "nomorHp" });
			d.kartuAktif = countActive(session, KartuPenjemputAntarJemput.class);
			d.totalScan = count(session, TransaksiPenjemputanAntarJemput.class, "waktuScan",
					new String[] { "kode", "nama", "tipeScan", "nomorScan", "pintuGerbang", "status" });
			d.totalPanggilan = count(session, DetailPenjemputanAntarJemput.class, "waktuDipanggil",
					new String[] { "kode", "nama", "teksPanggilan", "statusPanggilan" });
			d.totalNotifikasi = count(session, LogNotifikasiAntarJemput.class, "waktuKirim",
					new String[] { "kode", "nama", "kanal", "perangkatTujuan", "status" });
			loadStatusSummary(session, d);
			loadTopJadwal(session, d);
			loadRecentScan(session, d);
			fillDerivedData(d);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSessionSafely(session);
		}
		return d;
	}

	private int count(Session session, Class clazz, String dateField, String[] keywordFields) {
		Criteria criteria = session.createCriteria(clazz);
		applyFilter(criteria, dateField, keywordFields);
		criteria.setProjection(Projections.rowCount());
		Object result = criteria.uniqueResult();
		return result == null ? 0 : ((Number) result).intValue();
	}

	private int countActive(Session session, Class clazz) {
		Criteria criteria = session.createCriteria(clazz);
		criteria.add(Restrictions.eq("aktif", Boolean.TRUE));
		criteria.setProjection(Projections.rowCount());
		Object result = criteria.uniqueResult();
		return result == null ? 0 : ((Number) result).intValue();
	}

	private void applyFilter(Criteria criteria, String dateField, String[] keywordFields) {
		if (dateField != null) {
			if (filterMulai != null) {
				criteria.add(Restrictions.ge(dateField, filterMulai));
			}
			if (filterSampai != null) {
				criteria.add(Restrictions.le(dateField, filterSampai));
			}
		}
		String keyword = filterKeyword == null ? "" : filterKeyword.trim();
		if (keyword.length() > 0 && keywordFields != null && keywordFields.length > 0) {
			Disjunction disjunction = Restrictions.disjunction();
			for (int i = 0; i < keywordFields.length; i++) {
				disjunction.add(Restrictions.ilike(keywordFields[i], "%" + keyword + "%"));
			}
			criteria.add(disjunction);
		}
	}

	private void loadStatusSummary(Session session, AntarJemputDashboardData d) {
		loadGroupedText(session, d.statusScan, TransaksiPenjemputanAntarJemput.class, "status", "waktuScan",
				new String[] { "kode", "nama", "tipeScan", "nomorScan", "pintuGerbang", "status" });
		loadGroupedText(session, d.statusPanggilan, DetailPenjemputanAntarJemput.class, "statusPanggilan", "waktuDipanggil",
				new String[] { "kode", "nama", "teksPanggilan", "statusPanggilan" });
		loadGroupedText(session, d.statusNotifikasi, LogNotifikasiAntarJemput.class, "status", "waktuKirim",
				new String[] { "kode", "nama", "kanal", "perangkatTujuan", "status" });
	}

	private void loadGroupedText(Session session, Map target, Class clazz, String property, String dateField,
			String[] keywordFields) {
		Criteria criteria = session.createCriteria(clazz);
		applyFilter(criteria, dateField, keywordFields);
		criteria.setMaxResults(SAMPLE_LIMIT);
		List list = criteria.list();
		for (int i = 0; i < list.size(); i++) {
			Object value = readProperty(list.get(i), property);
			String key = value == null ? "Belum Diisi" : String.valueOf(value);
			increment(target, key);
		}
	}

	private void loadTopJadwal(Session session, AntarJemputDashboardData d) {
		Criteria criteria = session.createCriteria(JadwalAntarJemput.class);
		applyFilter(criteria, "tanggal", new String[] { "kode", "nama", "hari", "status" });
		criteria.addOrder(Order.desc("tanggal_dirubah"));
		criteria.setMaxResults(SAMPLE_LIMIT);
		List list = criteria.list();
		for (int i = 0; i < list.size(); i++) {
			JadwalAntarJemput jadwal = (JadwalAntarJemput) list.get(i);
			String rute = jadwal.getRuteAntarJemput() == null ? "Tanpa Rute" : jadwal.getRuteAntarJemput().getNama();
			String kendaraan = jadwal.getKendaraanAntarJemput() == null ? "Tanpa Kendaraan"
					: jadwal.getKendaraanAntarJemput().getNama();
			increment(d.perRute, rute);
			increment(d.perKendaraan, kendaraan);
			increment(d.perHari, jadwal.getHari() == null ? "Belum Diisi" : jadwal.getHari());
		}
	}

	private void loadRecentScan(Session session, AntarJemputDashboardData d) {
		Criteria criteria = session.createCriteria(TransaksiPenjemputanAntarJemput.class);
		applyFilter(criteria, "waktuScan", new String[] { "kode", "nama", "tipeScan", "nomorScan", "pintuGerbang", "status" });
		criteria.addOrder(Order.desc("waktuScan"));
		criteria.setMaxResults(8);
		List list = criteria.list();
		SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm");
		for (int i = 0; i < list.size(); i++) {
			TransaksiPenjemputanAntarJemput t = (TransaksiPenjemputanAntarJemput) list.get(i);
			String waktu = t.getWaktuScan() == null ? "-" : fmt.format(t.getWaktuScan());
			d.recentScan.add(waktu + " - " + safe(t.getNomorScan()) + " - " + safe(t.getStatus()));
		}
	}

	private void fillDerivedData(AntarJemputDashboardData d) {
		d.utilisasiArmada = percent(d.kendaraanAktif, d.totalKendaraan);
		d.cakupanRute = percent(d.ruteAktif, d.totalRute);
		d.cakupanPeserta = percent(d.pesertaAktif, d.totalPeserta);
		d.validitasKartu = percent(d.kartuAktif, d.totalKartu);
		d.intensitasScan = d.totalPeserta == 0 ? 0 : Math.min(100, (d.totalScan * 100) / d.totalPeserta);
		d.notifikasiRate = d.totalPanggilan == 0 ? 0 : Math.min(100, (d.totalNotifikasi * 100) / d.totalPanggilan);
	}

	private int percent(int part, int total) {
		if (total <= 0) {
			return 0;
		}
		return Math.min(100, (part * 100) / total);
	}

	private Object readProperty(Object object, String property) {
		try {
			String method = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
			return object.getClass().getMethod(method, new Class[0]).invoke(object, new Object[0]);
		} catch (Exception e) {
			return null;
		}
	}

	private void renderDashboard(AntarJemputDashboardData data) {
		Common.clear(this);
		MyPortalchildren wrapper = createPortalChildren(this, "100%");
		wrapper.setStyle("width:100%;max-width:100%;box-sizing:border-box;padding:8px;");

		Panel panel = new MyPanelConfig();
		panel.setTitle("Dasboard Antar Jemput");
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle("width:100%;max-width:100%;box-sizing:border-box;margin:0 0 14px 0;border:0;background:#ffffff;");
		panel.setParent(wrapper);

		Panelchildren body = new Panelchildren();
		body.setStyle("width:100%;max-width:100%;box-sizing:border-box;padding:10px;background:#f8fafc;");
		body.setParent(panel);

		org.zkoss.zul.Div shell = new org.zkoss.zul.Div();
		shell.setWidth("100%");
		shell.setStyle("width:100%;max-width:100%;box-sizing:border-box;overflow:hidden;");
		shell.setParent(body);

		renderHero(shell, data);
		renderFilter(shell);
		renderMetricCards(shell, data);

		MyPortallayout portal = new MyPortallayout();
		portal.setWidth("100%");
		portal.setStyle("width:100%;max-width:100%;box-sizing:border-box;border:0;background:transparent;");
		portal.setParent(shell);
		String colWidth = Common.isMobile() ? "100%" : "50%";
		MyPortalchildren top = createPortalChildren(portal, "100%");
		MyPortalchildren left = createPortalChildren(portal, colWidth);
		MyPortalchildren right = createPortalChildren(portal, colWidth);
		MyPortalchildren bottom = createPortalChildren(portal, "100%");

		renderFunnel(top, data);
		renderTopList(left, "Sebaran Rute", data.perRute, "Rute paling aktif berdasarkan sampel jadwal terakhir.");
		renderTopList(right, "Sebaran Kendaraan", data.perKendaraan,
				"Armada yang paling sering terhubung ke jadwal antar jemput.");
		renderTopList(left, "Status Scan", data.statusScan, "Status transaksi scan dari kartu atau nomor jemput.");
		renderTopList(right, "Status Panggilan", data.statusPanggilan,
				"Monitoring panggilan keluar kelas sampai serah terima.");
		renderRecentActivity(left, data);
		renderRadar(bottom, data);
		renderInsight(bottom, data);
	}

	private MyPortalchildren createPortalChildren(Component parent, String width) {
		MyPortalchildren pc = new MyPortalchildren();
		pc.setWidth(width);
		pc.setStyle("width:100%;max-width:100%;box-sizing:border-box;padding:6px;");
		pc.setParent(parent);
		return pc;
	}

	private void renderHero(Component parent, AntarJemputDashboardData d) {
		org.zkoss.zul.Div hero = new org.zkoss.zul.Div();
		hero.setStyle("width:100%;box-sizing:border-box;padding:18px 20px;margin-bottom:12px;border-radius:14px;background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);color:#ffffff;");
		hero.setParent(parent);
		appendHtml(hero,
				"<div>Transport Operation Center</div>"
						+ "<div>Dasboard Antar Jemput</div>"
						+ "<div>Pantau rute, jadwal, armada, peserta, kartu penjemput, scan gerbang, panggilan kelas, dan notifikasi dalam satu layar operasional.</div>"
						+ "<div>"
						+ heroNumber("Armada Aktif", d.kendaraanAktif) + heroNumber("Jadwal", d.totalJadwal)
						+ heroNumber("Peserta Aktif", d.pesertaAktif) + heroNumber("Scan", d.totalScan) + "</div>");
	}

	private String heroNumber(String label, int value) {
		return "<span>"
				+ "<b>" + value + "</b>"
				+ "<small>" + escapeHtml(label) + "</small></span>";
	}

	private void renderFilter(final Component parent) {
		final org.zkoss.zul.Div box = new org.zkoss.zul.Div();
		box.setParent(parent);
		box.setStyle("width:100%;max-width:100%;box-sizing:border-box;margin:8px 0 12px 0;padding:10px;border-radius:10px;background:#ffffff;border:1px solid #dbe4ef;");
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(box);
		toolbar.setStyle("border:0;background:transparent;padding:4px;box-sizing:border-box;");
		new MyLabelAgakKecil("Mulai:").setParent(toolbar);
		final MyDatebox mulai = new MyDatebox(filterMulai);
		mulai.setReadonly(true);
		mulai.setCols(5);
		mulai.setParent(toolbar);
		new MyLabelAgakKecil("Sampai:").setParent(toolbar);
		final MyDatebox sampai = new MyDatebox(filterSampai);
		sampai.setReadonly(true);
		sampai.setCols(5);
		sampai.setParent(toolbar);
		new MyLabelAgakKecil("Cari:").setParent(toolbar);
		final Textbox keyword = new Textbox(filterKeyword == null ? "" : filterKeyword);
		keyword.setCols(18);
		keyword.setTooltiptext("Cari kode, nama, status, nomor kartu, pintu gerbang, rute, atau kendaraan");
		keyword.setParent(toolbar);
		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Terapkan Filter", "/img/svg/search.svg");
		refresh.setStyle("font-weight:bold;font-size:12px;margin-left:6px;");
		refresh.setParent(toolbar);
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				filterMulai = mulai.getValue();
				filterSampai = sampai.getValue();
				filterKeyword = keyword.getValue();
				Common.clear(DasboardAntarJemput.this);
				init();
			}
		});
	}

	private void renderMetricCards(Component parent, AntarJemputDashboardData d) {
		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("width:100%;max-width:100%;box-sizing:border-box;margin:8px 0;overflow:hidden;");
		wrap.setParent(parent);
		metricCard(wrap, "Kendaraan", d.totalKendaraan, d.kendaraanAktif + " aktif", "#dbeafe", "#1e40af", "K");
		metricCard(wrap, "Rute", d.totalRute, d.ruteAktif + " aktif", "#ecfdf5", "#166534", "R");
		metricCard(wrap, "Jadwal", d.totalJadwal, d.jadwalAktif + " aktif", "#fef3c7", "#92400e", "J");
		metricCard(wrap, "Peserta", d.totalPeserta, d.pesertaAktif + " aktif", "#dcfce7", "#166534", "P");
		metricCard(wrap, "Kartu", d.totalKartu, d.kartuAktif + " aktif", "#ede9fe", "#5b21b6", "C");
		metricCard(wrap, "Notifikasi", d.totalNotifikasi, "terkirim/antri", "#fee2e2", "#991b1b", "N");
	}

	private void metricCard(Component parent, String title, int value, String desc, String bg, String color, String icon) {
		org.zkoss.zul.Div card = new org.zkoss.zul.Div();
		card.setStyle("display:inline-block;vertical-align:top;width:180px;min-height:88px;margin:6px;padding:12px;border-radius:12px;background:#ffffff;border:1px solid #e2e8f0;box-shadow:0 6px 16px rgba(15,23,42,0.08);box-sizing:border-box;");
		card.setParent(parent);
		appendHtml(card,
				"<div>"
						+ "<div>" + escapeHtml(icon) + "</div>"
						+ "<div>" + value + "</div></div>"
						+ "<div>" + escapeHtml(title) + "</div>"
						+ "<div>" + escapeHtml(desc) + "</div>");
	}

	private void renderFunnel(Component parent, AntarJemputDashboardData d) {
		StringBuilder html = new StringBuilder();
		html.append("<div>");
		html.append(funnelStep("Armada", d.kendaraanAktif, "#2563eb"));
		html.append(funnelStep("Rute", d.ruteAktif, "#059669"));
		html.append(funnelStep("Jadwal", d.totalJadwal, "#ca8a04"));
		html.append(funnelStep("Peserta", d.pesertaAktif, "#16a34a"));
		html.append(funnelStep("Scan", d.totalScan, "#7c3aed"));
		html.append(funnelStep("Notifikasi", d.totalNotifikasi, "#dc2626"));
		html.append("</div>");
		renderPanel(parent, "Alur Operasional", "Ringkasan funnel dari kesiapan armada sampai notifikasi.", html.toString());
	}

	private String funnelStep(String label, int value, String color) {
		return "<div>"
				+ "<div></div>"
				+ "<div>" + value + "</div>"
				+ "<div>" + escapeHtml(label) + "</div></div>";
	}

	private void renderTopList(Component parent, String title, Map map, String desc) {
		StringBuilder html = new StringBuilder();
		List keys = topKeys(map, 7);
		if (keys.isEmpty()) {
			html.append(emptyState("Belum ada data yang dapat ditampilkan."));
		} else {
			int max = maxValue(map, keys);
			for (int i = 0; i < keys.size(); i++) {
				String key = (String) keys.get(i);
				int value = ((Integer) map.get(key)).intValue();
				int width = max <= 0 ? 0 : Math.max(8, (value * 100) / max);
				html.append("<div>")
						.append("<div>")
						.append("<b>").append(escapeHtml(key)).append("</b><span>").append(value).append("</span></div>")
						.append("<div>")
						.append("<div></div></div></div>");
			}
		}
		renderPanel(parent, title, desc, html.toString());
	}

	private void renderRecentActivity(Component parent, AntarJemputDashboardData d) {
		StringBuilder html = new StringBuilder();
		if (d.recentScan.isEmpty()) {
			html.append(emptyState("Belum ada transaksi scan terbaru."));
		} else {
			html.append("<div>");
			for (int i = 0; i < d.recentScan.size(); i++) {
				html.append("<div>")
						.append(escapeHtml((String) d.recentScan.get(i))).append("</div>");
			}
			html.append("</div>");
		}
		renderPanel(parent, "Aktivitas Scan Terbaru", "Transaksi gerbang paling baru dari filter aktif.", html.toString());
	}

	private void renderRadar(Component parent, AntarJemputDashboardData d) {
		String[] labels = new String[] { "Armada", "Rute", "Peserta", "Kartu", "Scan", "Notifikasi" };
		int[] values = new int[] { d.utilisasiArmada, d.cakupanRute, d.cakupanPeserta, d.validitasKartu,
				d.intensitasScan, d.notifikasiRate };
		StringBuilder html = new StringBuilder();
		html.append("<div>");
		for (int i = 0; i < labels.length; i++) {
			html.append("<div>")
					.append("<div>").append(labels[i]).append("</div>")
					.append("<div>").append(values[i]).append("%</div>")
					.append("<div>")
					.append("<div></div></div></div>");
		}
		html.append("</div>");
		renderPanel(parent, "Spider Web Kesiapan Layanan",
				"Indikator kesiapan layanan dalam bentuk skor ringkas yang mudah dibandingkan antar periode.", html.toString());
	}

	private void renderInsight(Component parent, AntarJemputDashboardData d) {
		StringBuilder html = new StringBuilder();
		html.append("<div>");
		html.append(insight("Prioritas Operasional",
				d.kendaraanAktif == 0 ? "Aktifkan data kendaraan dan sopir terlebih dahulu."
						: "Pastikan jadwal hari ini sudah memiliki kendaraan dan rute."));
		html.append(insight("Keamanan Penjemputan",
				d.kartuAktif < d.pesertaAktif ? "Sebagian peserta belum memiliki kartu penjemput aktif."
						: "Validitas kartu penjemput sudah selaras dengan peserta aktif."));
		html.append(insight("Komunikasi",
				d.totalNotifikasi < d.totalPanggilan ? "Jumlah notifikasi lebih rendah dari panggilan, cek kanal/perangkat."
						: "Notifikasi sudah mengikuti volume panggilan."));
		html.append("</div>");
		renderPanel(parent, "Insight Manajemen", "Rekomendasi cepat berbasis data operasional antar jemput.", html.toString());
	}

	private String insight(String title, String text) {
		return "<div>"
				+ "<div>" + escapeHtml(title) + "</div>"
				+ "<div>" + escapeHtml(text)
				+ "</div></div>";
	}

	private void renderPanel(Component parent, String title, String desc, String innerHtml) {
		Panel panel = new MyPanelConfig();
		panel.setTitle(title);
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle("width:100%;max-width:100%;box-sizing:border-box;margin:0 0 14px 0;border:0;background:#ffffff;");
		panel.setParent(parent);
		Panelchildren body = new Panelchildren();
		body.setStyle("width:100%;max-width:100%;box-sizing:border-box;padding:10px;background:#f8fafc;");
		body.setParent(panel);
		appendHtml(body,
				"<div>" + escapeHtml(desc)
						+ "</div>" + innerHtml);
	}


	private void tampilkanLoading(String message, int percent) {
		Common.clear(this);
		MyPortalchildren pc = createPortalChildren(this, "100%");
		Panel panel = new Panel();
		panel.setBorder("none");
		panel.setWidth("100%");
		panel.setStyle("width:100%;max-width:100%;box-sizing:border-box;border:0;background:transparent;");
		panel.setParent(pc);
		Panelchildren body = new Panelchildren();
		body.setStyle("width:100%;max-width:100%;box-sizing:border-box;padding:0;background:transparent;");
		body.setParent(panel);
		loadingHtml = new org.zkoss.zul.Html(loadingHtml(message, percent));
		loadingHtml.setParent(body);
	}

	private void updateLoading(String message, int percent) {
		if (loadingHtml != null) {
			loadingHtml.setContent(loadingHtml(message, percent));
		}
	}

	private String loadingHtml(String message, int percent) {
		return "<div>"
				+ "<div>Memuat Dasboard Antar Jemput</div>"
				+ "<div>" + escapeHtml(message) + "</div>"
				+ "<div>"
				+ "<div></div></div></div>";
	}

	private void appendHtml(Component parent, String html) {
		org.zkoss.zul.Html h = new org.zkoss.zul.Html(html);
		h.setParent(parent);
	}

	private void increment(Map map, String key) {
		if (key == null || key.trim().length() == 0) {
			key = "Belum Diisi";
		}
		Integer current = (Integer) map.get(key);
		map.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
	}

	private List topKeys(Map map, int max) {
		List keys = new ArrayList(map.keySet());
		for (int i = 0; i < keys.size(); i++) {
			for (int j = i + 1; j < keys.size(); j++) {
				String a = (String) keys.get(i);
				String b = (String) keys.get(j);
				int av = ((Integer) map.get(a)).intValue();
				int bv = ((Integer) map.get(b)).intValue();
				if (bv > av) {
					keys.set(i, b);
					keys.set(j, a);
				}
			}
		}
		if (keys.size() > max) {
			return new ArrayList(keys.subList(0, max));
		}
		return keys;
	}

	private int maxValue(Map map, List keys) {
		int max = 0;
		for (int i = 0; i < keys.size(); i++) {
			int value = ((Integer) map.get(keys.get(i))).intValue();
			if (value > max) {
				max = value;
			}
		}
		return max;
	}

	private String emptyState(String text) {
		return "<div>"
				+ escapeHtml(text) + "</div>";
	}

	private String safe(String value) {
		return value == null || value.trim().length() == 0 ? "-" : value.trim();
	}

	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private void closeSessionSafely(Session session) {
		if (session != null && session.isOpen()) {
			try {
				session.close();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	private static class AntarJemputDashboardData {
		int totalKendaraan;
		int kendaraanAktif;
		int totalRute;
		int ruteAktif;
		int totalJadwal;
		int jadwalAktif;
		int totalPeserta;
		int pesertaAktif;
		int totalKartu;
		int kartuAktif;
		int totalScan;
		int totalPanggilan;
		int totalNotifikasi;
		int utilisasiArmada;
		int cakupanRute;
		int cakupanPeserta;
		int validitasKartu;
		int intensitasScan;
		int notifikasiRate;
		Map perRute = new HashMap();
		Map perKendaraan = new HashMap();
		Map perHari = new HashMap();
		Map statusScan = new HashMap();
		Map statusPanggilan = new HashMap();
		Map statusNotifikasi = new HashMap();
		List recentScan = new ArrayList();
	}
}
