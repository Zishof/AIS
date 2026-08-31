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

/**
 * Dashboard operasional modul Antar Jemput: menampilkan ringkasan armada (kendaraan), rute,
 * jadwal, peserta, kartu penjemput, transaksi scan gerbang, panggilan kelas, dan notifikasi dalam
 * satu layar visual (kartu metrik, funnel operasional, daftar top-N per kategori, radar
 * kesiapan layanan, aktivitas scan terbaru, dan insight rekomendasi otomatis). Seluruh konten
 * dirender sebagai HTML/CSS murni (bukan library chart) di dalam portal layout responsif.
 *
 * <p>
 * Data dimuat asinkron dalam dua tahap (indikator loading ditampilkan lebih dulu lewat
 * {@link #tampilkanLoading}, lalu diperbarui progresnya) agar UI tidak terkunci selama agregasi
 * berjalan, dan dicache berlapis (L2 lalu L3) lewat {@link DashboardCacheUtil} berkunci filter
 * aktif (rentang tanggal + kata kunci) — perhitungan ulang penuh hanya terjadi saat cache kosong
 * di kedua lapis. Filter (tanggal mulai/sampai, kata kunci pencarian teks bebas di berbagai kolom)
 * dapat diterapkan ulang lewat panel filter, yang membangun ulang seluruh dashboard.
 * </p>
 */
public class DasboardAntarJemput extends MyPortallayout {

	private static final long serialVersionUID = 1L;
	/** Batas jumlah baris sampel yang diambil untuk agregasi status/sebaran (bukan hitungan total, yang selalu dihitung penuh lewat {@code rowCount}). */
	private static final int SAMPLE_LIMIT = 300;
	private Date filterMulai;
	private Date filterSampai;
	private String filterKeyword = "";
	private transient org.zkoss.zul.Html loadingHtml;

	/** Membuat dashboard dengan lebar penuh dan mode maksimal, langsung memulai pemuatan data asinkron. */
	public DasboardAntarJemput() throws Exception {
		super();
		setWidth("100%");
		setMaximizedMode("whole");
		init();
	}

	private void init() throws Exception {
		renderAsync();
	}

	/** Menampilkan indikator loading bertahap, memuat data agregat (dengan cache) di timer terpisah, lalu merender seluruh dashboard setelah data siap. */
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

	/** Mengambil data dashboard dari cache L2 lalu L3 (kunci dibangun dari filter aktif) sebelum menghitung ulang penuh lewat {@link #loadDashboardData()}; hasil hitungan baru turut disimpan ke kedua lapis cache. */
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

	/** Menghitung seluruh angka dashboard dari database (total dan jumlah aktif per entitas, ringkasan status, top jadwal, aktivitas scan terbaru, dan metrik turunan), dibungkus sesi Hibernate sendiri yang selalu ditutup di akhir. */
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

	/** Menghitung total baris {@code clazz} sesuai filter tanggal/kata kunci aktif ({@link #applyFilter}). */
	private int count(Session session, Class clazz, String dateField, String[] keywordFields) {
		Criteria criteria = session.createCriteria(clazz);
		applyFilter(criteria, dateField, keywordFields);
		criteria.setProjection(Projections.rowCount());
		Object result = criteria.uniqueResult();
		return result == null ? 0 : ((Number) result).intValue();
	}

	/** Menghitung total baris {@code clazz} berstatus {@code aktif=true}, tidak dipengaruhi filter tanggal/kata kunci. */
	private int countActive(Session session, Class clazz) {
		Criteria criteria = session.createCriteria(clazz);
		criteria.add(Restrictions.eq("aktif", Boolean.TRUE));
		criteria.setProjection(Projections.rowCount());
		Object result = criteria.uniqueResult();
		return result == null ? 0 : ((Number) result).intValue();
	}

	/** Menerapkan filter rentang tanggal (bila {@code dateField} diberikan) dan pencarian kata kunci (OR ilike di seluruh {@code keywordFields}) ke {@code criteria}, sesuai filter aktif pada dashboard. */
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

	/** Mengagregasi sebaran status scan, panggilan, dan notifikasi (masing-masing dari sampel {@link #SAMPLE_LIMIT} baris terbaru sesuai filter) ke peta hitung {@code d.statusScan}/{@code statusPanggilan}/{@code statusNotifikasi}. */
	private void loadStatusSummary(Session session, AntarJemputDashboardData d) {
		loadGroupedText(session, d.statusScan, TransaksiPenjemputanAntarJemput.class, "status", "waktuScan",
				new String[] { "kode", "nama", "tipeScan", "nomorScan", "pintuGerbang", "status" });
		loadGroupedText(session, d.statusPanggilan, DetailPenjemputanAntarJemput.class, "statusPanggilan", "waktuDipanggil",
				new String[] { "kode", "nama", "teksPanggilan", "statusPanggilan" });
		loadGroupedText(session, d.statusNotifikasi, LogNotifikasiAntarJemput.class, "status", "waktuKirim",
				new String[] { "kode", "nama", "kanal", "perangkatTujuan", "status" });
	}

	/** Mengambil sampel {@link #SAMPLE_LIMIT} baris {@code clazz} sesuai filter, lalu menghitung frekuensi nilai {@code property} (via refleksi getter, {@link #readProperty}) ke {@code target}. */
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

	/** Mengambil sampel {@link #SAMPLE_LIMIT} jadwal terbaru (diurutkan perubahan terakhir) sesuai filter, mengagregasi sebaran per rute, kendaraan, dan hari ke {@code d.perRute}/{@code perKendaraan}/{@code perHari}. */
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

	/** Mengambil 8 transaksi scan gerbang terbaru sesuai filter, memformatnya sebagai baris teks ringkas ke {@code d.recentScan}. */
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

	/** Menghitung metrik persentase turunan (utilisasi armada, cakupan rute/peserta, validitas kartu, intensitas scan, rasio notifikasi) dari angka dasar yang sudah diagregasi. */
	private void fillDerivedData(AntarJemputDashboardData d) {
		d.utilisasiArmada = percent(d.kendaraanAktif, d.totalKendaraan);
		d.cakupanRute = percent(d.ruteAktif, d.totalRute);
		d.cakupanPeserta = percent(d.pesertaAktif, d.totalPeserta);
		d.validitasKartu = percent(d.kartuAktif, d.totalKartu);
		d.intensitasScan = d.totalPeserta == 0 ? 0 : Math.min(100, (d.totalScan * 100) / d.totalPeserta);
		d.notifikasiRate = d.totalPanggilan == 0 ? 0 : Math.min(100, (d.totalNotifikasi * 100) / d.totalPanggilan);
	}

	/** Menghitung persentase {@code part} terhadap {@code total}, dibulatkan ke bawah dan dibatasi maksimum 100; mengembalikan 0 bila {@code total <= 0}. */
	private int percent(int part, int total) {
		if (total <= 0) {
			return 0;
		}
		return Math.min(100, (part * 100) / total);
	}

	/** Membaca nilai getter JavaBean {@code property} dari {@code object} lewat refleksi (mis. {@code "status"} memanggil {@code getStatus()}); mengembalikan {@code null} bila getter tidak ditemukan/gagal dipanggil. */
	private Object readProperty(Object object, String property) {
		try {
			String method = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
			return object.getClass().getMethod(method, new Class[0]).invoke(object, new Object[0]);
		} catch (Exception e) {
			return null;
		}
	}

	/** Membangun seluruh tampilan dashboard dari {@code data} yang sudah diagregasi: hero banner, panel filter, kartu metrik, funnel operasional, daftar top-N (rute/kendaraan/status), aktivitas terbaru, radar kesiapan, dan insight — disusun dalam portal layout dua kolom (satu kolom pada mobile). */
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

	/** Membuat satu kolom {@link MyPortalchildren} berlebar {@code width}, ditempel ke {@code parent}. */
	private MyPortalchildren createPortalChildren(Component parent, String width) {
		MyPortalchildren pc = new MyPortalchildren();
		pc.setWidth(width);
		pc.setStyle("width:100%;max-width:100%;box-sizing:border-box;padding:6px;");
		pc.setParent(parent);
		return pc;
	}

	/** Merender banner hero berjudul dengan ringkasan angka kunci (armada aktif, jadwal, peserta aktif, scan). */
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

	/** Menyusun markup satu angka ringkas pada hero banner. */
	private String heroNumber(String label, int value) {
		return "<span>"
				+ "<b>" + value + "</b>"
				+ "<small>" + escapeHtml(label) + "</small></span>";
	}

	/** Merender panel filter (tanggal mulai/sampai readonly, kata kunci pencarian teks bebas) dengan tombol "Terapkan Filter" yang membangun ulang seluruh dashboard dari nilai filter baru. */
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

	/** Merender enam kartu metrik ringkas (kendaraan, rute, jadwal, peserta, kartu, notifikasi) beserta jumlah aktifnya. */
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

	/** Menyusun satu kartu metrik (ikon huruf, nilai besar, judul, deskripsi) dengan warna latar/teks yang diberikan. */
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

	/** Merender panel "Alur Operasional" berbentuk funnel enam tahap (Armada → Rute → Jadwal → Peserta → Scan → Notifikasi). */
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

	/** Menyusun markup satu tahap funnel operasional. */
	private String funnelStep(String label, int value, String color) {
		return "<div>"
				+ "<div></div>"
				+ "<div>" + value + "</div>"
				+ "<div>" + escapeHtml(label) + "</div></div>";
	}

	/** Merender panel daftar top-7 kunci dengan hitungan tertinggi dari {@code map} (mis. sebaran rute/kendaraan/status) sebagai bar proporsional; menampilkan pesan kosong bila peta kosong. */
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

	/** Merender panel "Aktivitas Scan Terbaru" berisi daftar transaksi scan gerbang paling baru sesuai filter aktif. */
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

	/** Merender panel "Spider Web Kesiapan Layanan": enam indikator persentase (utilisasi armada, cakupan rute/peserta, validitas kartu, intensitas scan, rasio notifikasi) sebagai bar ringkas. */
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

	/** Merender panel "Insight Manajemen": tiga rekomendasi teks otomatis (prioritas operasional, keamanan penjemputan, komunikasi) berdasarkan perbandingan sederhana antar metrik. */
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

	/** Menyusun markup satu baris rekomendasi insight. */
	private String insight(String title, String text) {
		return "<div>"
				+ "<div>" + escapeHtml(title) + "</div>"
				+ "<div>" + escapeHtml(text)
				+ "</div></div>";
	}

	/** Membangun satu panel kartu standar (judul, deskripsi kecil, konten HTML bebas) yang dipakai berulang oleh seluruh bagian dashboard. */
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


	/** Menampilkan indikator loading awal dengan pesan dan persentase progres. */
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

	/** Memperbarui pesan dan persentase progres pada indikator loading yang sudah ditampilkan. */
	private void updateLoading(String message, int percent) {
		if (loadingHtml != null) {
			loadingHtml.setContent(loadingHtml(message, percent));
		}
	}

	/** Menyusun markup indikator loading (judul, pesan, bar progres). */
	private String loadingHtml(String message, int percent) {
		return "<div>"
				+ "<div>Memuat Dasboard Antar Jemput</div>"
				+ "<div>" + escapeHtml(message) + "</div>"
				+ "<div>"
				+ "<div></div></div></div>";
	}

	/** Membungkus {@code html} sebagai komponen {@link org.zkoss.zul.Html} dan menempelkannya ke {@code parent}. */
	private void appendHtml(Component parent, String html) {
		org.zkoss.zul.Html h = new org.zkoss.zul.Html(html);
		h.setParent(parent);
	}

	/** Menambah hitungan {@code key} pada {@code map} sebesar satu, memakai kunci {@code "Belum Diisi"} bila {@code key} kosong/{@code null}. */
	private void increment(Map map, String key) {
		if (key == null || key.trim().length() == 0) {
			key = "Belum Diisi";
		}
		Integer current = (Integer) map.get(key);
		map.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
	}

	/** Mengembalikan hingga {@code max} kunci {@code map} dengan nilai hitung tertinggi, diurutkan menurun lewat bubble sort sederhana. */
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

	/** Mengembalikan nilai hitung tertinggi di antara {@code keys} pada {@code map}, dipakai sebagai skala 100% untuk bar proporsional. */
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

	/** Menyusun markup pesan status kosong yang seragam untuk bagian tanpa data. */
	private String emptyState(String text) {
		return "<div>"
				+ escapeHtml(text) + "</div>";
	}

	/** Mengembalikan {@code value} yang sudah di-trim, atau {@code "-"} bila {@code null}/kosong. */
	private String safe(String value) {
		return value == null || value.trim().length() == 0 ? "-" : value.trim();
	}

	/** Melakukan escape karakter HTML dasar ({@code & < > "}) pada {@code value}; mengembalikan string kosong bila {@code null}. */
	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/** Menutup {@code session} dengan aman bila masih terbuka, mencatat kegagalan penutupan tanpa melempar exception. */
	private void closeSessionSafely(Session session) {
		if (session != null && session.isOpen()) {
			try {
				session.close();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	/** Struktur data internal hasil agregasi {@link #loadDashboardData()}: seluruh angka total/aktif per entitas, metrik persentase turunan, peta sebaran per kategori, dan daftar aktivitas terbaru yang dipakai untuk merender dashboard. */
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
