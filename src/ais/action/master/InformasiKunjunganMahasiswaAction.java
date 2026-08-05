package ais.action.master;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.LogLogin;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

/**
 * Riwayat kunjungan (login) mahasiswa: profil, ringkasan aktivitas, pola waktu akses, dan rincian log.
 * Semua visual memakai {@link DashboardUiKit} (HTML+CSS murni) agar seragam dan responsif di HP/desktop.
 */
public class InformasiKunjunganMahasiswaAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 4155860737880329036L;
	private static final int PAGE_SIZE = 15;
	private static final int MAX_LOG_LOGIN = 500;

	private static final String[] HARI = { "Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu" };

	private MyWindow win;
	private List<LogLogin> logLogins = new ArrayList<LogLogin>();
	private Mahasiswa mahasiswa;
	private MyGrid fotoGrid;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser == null || tbmuser.getMahasiswa() == null) {
			alert("Anda harus login sebagai mahasiswa");
			return;
		}
		win = (MyWindow) comp;
		mahasiswa = tbmuser.getMahasiswa();

		ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("kunjungan_mahasiswa",
				"Memuat Riwayat Kunjungan",
				"Mengambil profil, riwayat login, tren kunjungan, dan rincian aktivitas mahasiswa.", 8);
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("kunjungan_mahasiswa",
							"Memuat Riwayat Kunjungan", "Menyusun ringkasan kunjungan dan tabel riwayat login.", 55);
					init();
					ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("kunjungan_mahasiswa",
							"Riwayat Kunjungan Siap", "Seluruh data kunjungan berhasil ditampilkan.", 100);
				} finally {
					ais.ui.util.KeuanganDashboardEnhanceUtil.hideFloatingProgress("kunjungan_mahasiswa");
				}
			}
		});
	}

	private void init() {
		Common.clear(win);

		// Portal 3-panel: Profil (40%) | Dasbor (60%) | Daftar (100%)
		ais.ui.util.MyPortallayout portal = ais.ui.util.PortalUiHelper.portal(win);
		ais.ui.util.MyPortalchildren kiriKolom = ais.ui.util.PortalUiHelper.kolom(portal, "40%");
		ais.ui.util.MyPortalchildren kananKolom = ais.ui.util.PortalUiHelper.kolom(portal, "60%");
		ais.ui.util.MyPortalchildren penuhKolom = ais.ui.util.PortalUiHelper.kolom(portal, "100%");

		loadLogLogin();
		renderProfilMahasiswa(kiriKolom);
		renderDashboardKunjungan(kananKolom);
		createListFoto(penuhKolom);
	}

	private void renderProfilMahasiswa(ais.ui.util.MyPortalchildren kolom) {
		Component host = ais.ui.util.PortalUiHelper.panel(kolom, "Profil Mahasiswa",
				"Data diri singkat untuk memastikan riwayat yang sedang dilihat memang milik Anda.");
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(host);

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setWidth("70%");
		columns.appendChild(column);

		Rows rows = new Rows();
		rows.setParent(grid);
		appendInfoRow(rows, "NIM", safe(mahasiswa == null ? null : mahasiswa.getNim()));
		appendInfoRow(rows, "Prodi",
				mahasiswa != null && mahasiswa.getJurusan() != null ? safe(mahasiswa.getJurusan().getNama()) : "-");
		appendInfoRow(rows, "Nama", safe(mahasiswa == null ? null : mahasiswa.getNama()));
		appendInfoRow(rows, "Fakultas",
				mahasiswa != null && mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getFakultas() != null
						? safe(mahasiswa.getJurusan().getFakultas().getNama())
						: "-");
		appendInfoRow(rows, "Angkatan", mahasiswa == null ? "-"
				: safe(String.valueOf(mahasiswa.getTahunangkatan())) + " (" + safe(mahasiswa.getSemesterMulai()) + ")");
	}

	private void appendInfoRow(Rows rows, String label, String value) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(label));
		row.appendChild(new ais.ui.util.MyLabelConfig(value == null || value.trim().length() == 0 ? "-" : value));
	}

	@SuppressWarnings("unchecked")
	private void loadLogLogin() {
		try {
			Session session = HibernateUtil.currentSession();
			logLogins = session.createCriteria(LogLogin.class).add(Restrictions.eq("mahasiswa", mahasiswa))
					.setMaxResults(MAX_LOG_LOGIN).addOrder(Order.desc("id")).list();
			if (logLogins == null) {
				logLogins = new ArrayList<LogLogin>();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			logLogins = new ArrayList<LogLogin>();
		}
	}

	private void renderDashboardKunjungan(ais.ui.util.MyPortalchildren kolom) {
		KunjunganSummary s = buildSummary(logLogins);

		Component host = ais.ui.util.PortalUiHelper.panel(kolom, "Dasbor Kunjungan",
				"Lihat seberapa sering dan kapan akun Anda dipakai masuk ke sistem.");

		// 1. Kartu ringkasan
		List<DashboardUiKit.Stat> cards = new ArrayList<DashboardUiKit.Stat>();
		cards.add(new DashboardUiKit.Stat("Total Kunjungan", String.valueOf(s.totalLogin), "Jumlah masuk tercatat",
				DashboardUiKit.PRIMARY));
		cards.add(new DashboardUiKit.Stat("Belum Logout", String.valueOf(s.belumLogout), "Sesi belum tercatat keluar",
				DashboardUiKit.WARN));
		cards.add(new DashboardUiKit.Stat("Jaringan Berbeda", String.valueOf(s.ipUnik), "Perkiraan lokasi/perangkat",
				DashboardUiKit.GOOD));
		cards.add(new DashboardUiKit.Stat("Terakhir Masuk", s.loginTerakhir, "Waktu akses paling baru",
				DashboardUiKit.INK));

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-family:Arial,sans-serif;'>");
		sb.append(DashboardUiKit.cards(cards));

		// 2. Baris visual responsif
		sb.append(DashboardUiKit.openGrid(260));
		sb.append(DashboardUiKit.sparkline("Tren Kunjungan Harian", "Naik-turun jumlah masuk tiap hari.",
				s.trenValues(), DashboardUiKit.PRIMARY, "Belum ada kunjungan untuk digambar."));
		sb.append(DashboardUiKit.spider("Kebiasaan Akun", "Gambaran cepat sehat-tidaknya kebiasaan masuk Anda.",
				new String[] { "Sesi selesai", "Keamanan jaringan", "Rutinitas", "Riwayat" },
				new int[] { s.persenSesiSelesai(), s.persenKeamanan(), s.persenRutinitas(), s.persenRiwayat() }));
		sb.append(DashboardUiKit.barList("Hari Paling Aktif", "Hari apa Anda paling sering masuk.", s.perHari(),
				DashboardUiKit.ACCENT, "kali", false, "Belum ada data hari."));
		sb.append(DashboardUiKit.barList("Waktu Favorit Masuk", "Bagian hari yang paling sering Anda pakai masuk.",
				s.perWaktu(), DashboardUiKit.WARN, "kali", false, "Belum ada data jam."));
		sb.append(DashboardUiKit.donut("Sebaran Jaringan/IP", "Dari berapa banyak jaringan akun Anda dipakai.",
				s.sebaranIp(), false, "Belum ada catatan jaringan."));
		sb.append(DashboardUiKit.closeGrid());
		sb.append("</div>");

		new Html(sb.toString()).setParent(host);
	}

	private KunjunganSummary buildSummary(List<LogLogin> logs) {
		KunjunganSummary summary = new KunjunganSummary();
		if (logs == null) {
			return summary;
		}
		summary.totalLogin = logs.size();
		Calendar cal = Calendar.getInstance();
		for (LogLogin log : logs) {
			if (log == null) {
				continue;
			}
			if (log.getLogout() == null) {
				summary.belumLogout++;
			}
			if (log.getIp() != null && log.getIp().trim().length() > 0) {
				String ip = log.getIp().trim();
				Integer c = summary.ipCount.get(ip);
				summary.ipCount.put(ip, Integer.valueOf((c == null ? 0 : c.intValue()) + 1));
			}
			Date login = log.getLogin();
			if (login != null) {
				String key = Common.dateFormat83.get().format(login);
				Integer old = summary.trenHarian.get(key);
				summary.trenHarian.put(key, Integer.valueOf((old == null ? 0 : old.intValue()) + 1));
				if (summary.loginTerakhirDate == null || login.after(summary.loginTerakhirDate)) {
					summary.loginTerakhirDate = login;
				}
				cal.setTime(login);
				summary.weekday[cal.get(Calendar.DAY_OF_WEEK) - 1]++;
				summary.jam[cal.get(Calendar.HOUR_OF_DAY)]++;
			}
		}
		summary.ipUnik = summary.ipCount.size();
		summary.loginTerakhir = summary.loginTerakhirDate == null ? "-"
				: Common.dateFormat3.get().format(summary.loginTerakhirDate);
		return summary;
	}

	private void createListFoto(ais.ui.util.MyPortalchildren kolom) {
		Component host = ais.ui.util.PortalUiHelper.panel(kolom, "Daftar Kunjungan",
				"Catatan waktu masuk, waktu keluar, dan jaringan yang dipakai setiap kali Anda login.");

		fotoGrid = new MyGrid();
		fotoGrid.setMold("paging");
		fotoGrid.setPageSize(PAGE_SIZE);
		fotoGrid.setParent(host);
		fotoGrid.setWidth("100%");

		Columns columns = new Columns();
		columns.setParent(fotoGrid);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setAlign("center");
		column.setWidth("40px");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu Login");
		column.setAlign("center");
		column.setWidth("25%");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu Logout");
		column.setAlign("center");
		column.setWidth("25%");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dari IP");
		column.setAlign("center");
		column.setWidth("20%");

		ListModel strset = new SimpleListModel(logLogins == null ? new ArrayList<LogLogin>() : logLogins);
		fotoGrid.setRowRenderer(new LogLoginRenderer());
		fotoGrid.setModelCheckMobile(strset);
		fotoGrid.renderAll();
		fotoGrid.setOddRowSclass("non-odd");
	}

	private class TampilDetailPembayaran implements EventListener {
		private LogLogin logLogin;
		private MyDetail detail;
		private MyGrid detailPembayaranGrid;

		private class DetailLogLoginRenderer extends ais.ui.util.MyRowRenderer {
			@Override
			public void render(final Row row, Object data) throws Exception {
				row.setValign("top");
				final DetailLogLogin detailLog = (DetailLogLogin) data;
				new Label(detailLog == null || detailLog.getKeterangan() == null ? "-" : detailLog.getKeterangan())
						.setParent(row);
				new Label(detailLog == null || detailLog.getWaktu() == null ? ""
						: Common.dateFormat3.get().format(detailLog.getWaktu())).setParent(row);
			}
		}

		private void createList() {
			Common.clear(detail);
			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height:200px;padding:6px;background:#f8fafc;border-radius:8px;");
			groupbox.setParent(detail);
			groupbox.appendChild(new MyCaptionStyled("Daftar Rincian Kunjungan"));

			detailPembayaranGrid = new MyGrid();
			detailPembayaranGrid.setMold("paging");
			detailPembayaranGrid.setPageSize(PAGE_SIZE);
			detailPembayaranGrid.setParent(groupbox);
			detailPembayaranGrid.setWidth("100%");
			detailPembayaranGrid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(detailPembayaranGrid);
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Menu yang diakses");
			column.setWidth("75%");
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Waktu");
			column.setAlign("center");
			column.setWidth("25%");
			loadDetailLogLogin();
		}

		@SuppressWarnings("unchecked")
		private void loadDetailLogLogin() {
			List<DetailLogLogin> details = new ArrayList<DetailLogLogin>();
			try {
				Session session = HibernateUtil.currentSession();
				details = session.createCriteria(DetailLogLogin.class).add(Restrictions.eq("logLogin", logLogin))
						.addOrder(Order.desc("id")).list();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			ListModel strset = new SimpleListModel(details == null ? new ArrayList<DetailLogLogin>() : details);
			detailPembayaranGrid.setRowRenderer(new DetailLogLoginRenderer());
			detailPembayaranGrid.setModelCheckMobile(strset);
			detailPembayaranGrid.renderAll();
			detailPembayaranGrid.setOddRowSclass("non-odd");
		}

		public TampilDetailPembayaran(MyDetail detail, LogLogin logLogin) {
			this.logLogin = logLogin;
			this.detail = detail;
		}

		@Override
		public void onEvent(Event event) throws Exception {
			Common.clear(detail);
			if (detail.isOpen()) {
				createList();
			}
		}
	}

	private class LogLoginRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final LogLogin logLogin = (LogLogin) data;
			final MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.addEventListener("onOpen", new TampilDetailPembayaran(detail, logLogin));
			new Label(logLogin == null || logLogin.getLogin() == null ? ""
					: Common.dateFormat3.get().format(logLogin.getLogin())).setParent(row);
			new Label(logLogin == null || logLogin.getLogout() == null ? "Belum/tidak logout"
					: Common.dateFormat3.get().format(logLogin.getLogout())).setParent(row);
			String ip = logLogin == null ? null : logLogin.getIp();
			if (ip != null && ip.trim().length() > 0) {
				A myIp = new A(ip);
				myIp.setTarget("_blank");
				myIp.setHref("http://whatismyipaddress.com/ip/" + ip.trim());
				myIp.setParent(row);
			} else {
				new Label("-").setParent(row);
			}
		}
	}

	private String safe(String value) {
		return value == null || value.trim().length() == 0 ? "-" : value;
	}

	/** Ringkasan kunjungan + perhitungan turunan untuk visual dasbor. */
	private static class KunjunganSummary {
		private int totalLogin;
		private int belumLogout;
		private int ipUnik;
		private Date loginTerakhirDate;
		private String loginTerakhir = "-";
		private final TreeMap<String, Integer> trenHarian = new TreeMap<String, Integer>();
		private final Map<String, Integer> ipCount = new LinkedHashMap<String, Integer>();
		private final int[] weekday = new int[7];
		private final int[] jam = new int[24];

		/** Nilai tren harian (kronologis, maksimal 14 titik terakhir) untuk sparkline. */
		private List<Integer> trenValues() {
			List<Integer> all = new ArrayList<Integer>(trenHarian.values());
			int from = Math.max(0, all.size() - 14);
			return all.subList(from, all.size());
		}

		private LinkedHashMap<String, Double> perHari() {
			LinkedHashMap<String, Double> map = new LinkedHashMap<String, Double>();
			for (int i = 1; i < 7; i++) {
				map.put(HARI[i], (double) weekday[i]);
			}
			map.put(HARI[0], (double) weekday[0]);
			return map;
		}

		private LinkedHashMap<String, Double> perWaktu() {
			LinkedHashMap<String, Double> map = new LinkedHashMap<String, Double>();
			map.put("Dini hari (00–06)", (double) sumJam(0, 6));
			map.put("Pagi (06–12)", (double) sumJam(6, 12));
			map.put("Siang (12–18)", (double) sumJam(12, 18));
			map.put("Malam (18–24)", (double) sumJam(18, 24));
			return map;
		}

		private int sumJam(int fromInclusive, int toExclusive) {
			int total = 0;
			for (int h = fromInclusive; h < toExclusive && h < 24; h++) {
				total += jam[h];
			}
			return total;
		}

		private LinkedHashMap<String, Double> sebaranIp() {
			LinkedHashMap<String, Double> map = new LinkedHashMap<String, Double>();
			int shown = 0;
			double lainnya = 0;
			for (Map.Entry<String, Integer> e : ipCount.entrySet()) {
				if (shown < 5) {
					map.put(e.getKey(), (double) e.getValue());
					shown++;
				} else {
					lainnya += e.getValue();
				}
			}
			if (lainnya > 0) {
				map.put("Lainnya", lainnya);
			}
			return map;
		}

		private int persenSesiSelesai() {
			return DashboardUiKit.pct(totalLogin - belumLogout, Math.max(1, totalLogin));
		}

		private int persenKeamanan() {
			return ipUnik <= 1 ? 100 : Math.max(20, 100 - ((ipUnik - 1) * 15));
		}

		private int persenRutinitas() {
			return DashboardUiKit.pct(Math.min(10, trenHarian.size()), 10);
		}

		private int persenRiwayat() {
			return DashboardUiKit.pct(totalLogin, MAX_LOG_LOGIN);
		}
	}
}
