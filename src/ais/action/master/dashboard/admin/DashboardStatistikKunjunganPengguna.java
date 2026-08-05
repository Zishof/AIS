
package ais.action.master.dashboard.admin;

/*
 * DASHBOARD_STATISTIK_KUNJUNGAN_PENGGUNA_HTML_CSS_2026
 *
 * Refactor dari dashboard kunjungan lama:
 * - Menghapus grafik lama, rendering gambar lama, dan rendering gambar grafik.
 * - Mengganti grafik menjadi HTML/CSS modern mengikuti pola dashboard SOP:
 *   hero, filter, card ringkasan, panel tren, komposisi, radar, dan tabel ringkas.
 * - Query menggunakan parameter Hibernate agar lebih aman.
 * - openSession() selalu ditutup di finally.
 * - currentSession() tidak ditutup manual.
 * - Tetap kompatibel Java 1.6/1.7 dan ZKoss 5.5: tanpa lambda, stream, try-with-resources.
 */

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardStatistikKunjunganPengguna extends MyWindow {

	private static final long serialVersionUID = -28636873241676666L;

	private Div center;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();
	private MyDatebox mulai = new MyDatebox();
	private MyDatebox sampai = new MyDatebox();
	private MyCheckboxConfig mahasiswa;
	private MyCheckboxConfig dosen;
	private MyCheckboxConfig admin;
	private MyCheckboxConfig siswa;
	private MyCheckboxConfig guru;
	private boolean tampilRinci = false;
	private int width = 1200;
	private int height = 430;

	public DashboardStatistikKunjunganPengguna() throws Exception {
		super();
		initFakultas();
		init();
		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				initChart();
			}
		});
	}

	public DashboardStatistikKunjunganPengguna(int width, int height) throws Exception {
		super();
		tampilRinci = true;
		reinit(width, height);
	}

	public void reinit(int width, int height) throws Exception {
		this.width = width;
		this.height = height;
		initFakultas();
		init();
		initChart();
	}

	public DashboardStatistikKunjunganPengguna(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	private void initFakultas() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);
	}

	private void init() {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		// Parent (Tabbox) dibuat tinggi 20000px agar konten dashboard yang panjang (saringan +
		// grafik + tabel) muat penuh; tiap tabpanel mengikuti parent ini dengan lantai
		// min-height:15000px (lihat setStyle tabpanel di bawah).
		tabbox.setHeight("20000px");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Per Pengguna");
		tab1.setParent(tabs);

		MyTabConfig tab51 = new MyTabConfig("Per Jenis Pengguna");
		tab51.setParent(tabs);

		MyTabConfig tab52 = new MyTabConfig("Per Satuan Kerja");
		tab52.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("Akses Mobile");
		tab3.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setStyle("min-height:15000px;height:100%;overflow:auto;background:#f6f8fb;");

		final Tabpanel tabpanel51 = new ais.ui.util.MyTabpanel();
		tabpanel51.setParent(tabpanels);
		tabpanel51.setStyle("min-height:15000px;height:100%;overflow:auto;background:#f6f8fb;");
		tab51.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel51.getChildren().size() == 0) {
					DashboardStatistikKunjunganJenisPengguna dashboard = new DashboardStatistikKunjunganJenisPengguna();
					dashboard.setHeight("100%");
					dashboard.setWidth("100%");
					dashboard.setParent(tabpanel51);
				}
			}
		});

		final Tabpanel tabpanel52 = new ais.ui.util.MyTabpanel();
		tabpanel52.setParent(tabpanels);
		tabpanel52.setStyle("min-height:15000px;height:100%;overflow:auto;background:#f6f8fb;");
		tab52.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel52.getChildren().size() == 0) {
					DashboardStatistikKunjunganSatuanKerja dashboard = new DashboardStatistikKunjunganSatuanKerja();
					dashboard.setHeight("100%");
					dashboard.setWidth("100%");
					dashboard.setParent(tabpanel52);
				}
			}
		});

		final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
		tabpanel3.setParent(tabpanels);
		tabpanel3.setStyle("min-height:15000px;height:100%;overflow:auto;background:#f6f8fb;");
		tab3.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel3.getChildren().size() == 0) {
					DashboardStatistikKunjunganPenggunaMobile dashboard = new DashboardStatistikKunjunganPenggunaMobile();
					dashboard.setHeight("100%");
					dashboard.setWidth("100%");
					dashboard.setParent(tabpanel3);
				}
			}
		});

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(tabpanel1,
				"Saringan Data",
				"Atur saringan untuk menyesuaikan data kunjungan yang ditampilkan.",
				"Statistik Kunjungan Pengguna",
				"Banyaknya kunjungan pengguna sistem, lengkap dengan grafiknya.");
		Component saringanHost = hostPortal[0];
		center = (Div) hostPortal[1];

		Vbox filterBox = new Vbox();
		filterBox.setWidth("100%");
		filterBox.setStyle("box-sizing:border-box;padding:10px 12px;background:#ffffff;border-bottom:1px solid #e5e7eb;");
		filterBox.setParent(saringanHost);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(filterBox);
		toolbar.setStyle("border:0;background:transparent;padding:0;");

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
		refresh.setTooltiptext("Memuat ulang dashboard sesuai filter terbaru.");
		refresh.setParent(toolbar);
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		ais.action.master.helper.LaporanKunjunganPenggunaHelper.pasangTombolLaporan(
				toolbar, this, mulai, sampai, searchfakultas, searchjurusan, searchyayasan, searchsekolah);

		if (tampilRinci && Common.getApakahAdmin()) {
			MyButtonConfig detail = new MyButtonConfig("Lihat Rinci", "/img/12123-eyes-icon.png");
			detail.setParent(toolbar);
			detail.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					MyWindow laporan = new MyWindow();
					laporan.setHeight("99%");
					laporan.setWidth("99%");
					laporan.setTitle("Rekap Kunjungan Pengguna");
					laporan.setClosable(true);
					laporan.setBorder("none");
					Borderlayout bl = new Borderlayout();
					laporan.appendChild(bl);
					Center c = new Center();
					ais.ui.util.ZkCompat.setFlex(c, true);
					c.setParent(bl);
					c.appendChild(new Iframe("/pages/master/log_login.zul"));
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
					laporan.onModal();
				}
			});
		}

		Grid grid = new Grid();
		grid.setWidth("100%");
		grid.setStyle("border:0;background:transparent;");
		grid.setParent(filterBox);

		Columns columns = new Columns();
		columns.setParent(grid);
		for (int i = 0; i < 5; i++) {
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
		}

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		setupCombo(row, searchfakultas, "Fakultas");
		setupCombo(row, searchjurusan, "Prodi/Jurusan");
		setupCombo(row, searchyayasan, "Yayasan");
		setupCombo(row, searchsekolah, "Sekolah");

		Hbox tanggalBox = new Hbox();
		tanggalBox.setAlign("center");
		row.appendChild(tanggalBox);
		Calendar dateMulai = ais.ui.util.WaktuUtil.getCalendar();
		dateMulai.set(Calendar.MONTH, dateMulai.get(Calendar.MONTH) - 1);
		mulai.setValue(dateMulai.getTime());
		mulai.setCols(4);
		mulai.setReadonly(true);
		mulai.setTooltiptext("Tanggal awal kunjungan yang ingin dianalisis.");
		tanggalBox.appendChild(mulai);
		mulai.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});
		tanggalBox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		sampai.setValue(ais.ui.util.WaktuUtil.getDate());
		sampai.setCols(4);
		sampai.setReadonly(true);
		sampai.setTooltiptext("Tanggal akhir kunjungan yang ingin dianalisis.");
		tanggalBox.appendChild(sampai);
		sampai.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "5");
		row.setAlign("center");
		row.setParent(rows);

		Hbox hbox = new Hbox();
		hbox.setAlign("center");
		hbox.setStyle("gap:8px;flex-wrap:wrap;");
		row.appendChild(hbox);
		hbox.appendChild(mahasiswa = new MyCheckboxConfig("Mahasiswa"));
		hbox.appendChild(dosen = new MyCheckboxConfig("Dosen"));
		hbox.appendChild(siswa = new MyCheckboxConfig("Siswa"));
		hbox.appendChild(guru = new MyCheckboxConfig("Guru"));
		hbox.appendChild(admin = new MyCheckboxConfig("Admin"));

		mahasiswa.setChecked(true);
		dosen.setChecked(true);
		admin.setChecked(true);
		siswa.setChecked(true);
		guru.setChecked(true);

		addReload(mahasiswa);
		addReload(dosen);
		addReload(siswa);
		addReload(guru);
		addReload(admin);

	}

	private void setupCombo(Row row, Combobox combo, String tooltip) {
		row.appendChild(combo);
		combo.setWidth("95%");
		combo.setReadonly(true);
		combo.setTooltiptext(tooltip);
		combo.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});
	}

	private void addReload(MyCheckboxConfig checkbox) {
		checkbox.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});
	}

	private void initChart() {
		if (center == null) {
			return;
		}
		Common.clear(center);
		Div loading = StatistikKunjunganDashboardUtil.loading("Mengambil statistik kunjungan pengguna...");
		loading.setParent(center);

		try {
			StatistikKunjunganDashboardUtil.Filter filter = readFilter();
			List<StatistikKunjunganDashboardUtil.DailyRow> rows = StatistikKunjunganDashboardUtil.loadDailyRows(
					"log_login", filter, false);
			Common.clear(center);
			renderDashboard(rows, filter);
		} catch (Exception e) {
			Common.clear(center);
			center.appendChild(new Html(StatistikKunjunganDashboardUtil.errorHtml(
					"Gagal memuat statistik kunjungan pengguna", e)));
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private StatistikKunjunganDashboardUtil.Filter readFilter() {
		StatistikKunjunganDashboardUtil.Filter filter = new StatistikKunjunganDashboardUtil.Filter();
		filter.fakultas = StatistikKunjunganDashboardUtil.selectedFakultas(searchfakultas);
		filter.jurusan = StatistikKunjunganDashboardUtil.selectedJurusan(searchjurusan);
		filter.yayasan = StatistikKunjunganDashboardUtil.selectedYayasan(searchyayasan);
		filter.sekolah = StatistikKunjunganDashboardUtil.selectedSekolah(searchsekolah);
		filter.mulai = mulai == null ? null : mulai.getValue();
		filter.sampai = sampai == null ? null : sampai.getValue();
		filter.showMahasiswa = mahasiswa == null || mahasiswa.isChecked();
		filter.showDosen = dosen == null || dosen.isChecked();
		filter.showSiswa = siswa == null || siswa.isChecked();
		filter.showGuru = guru == null || guru.isChecked();
		filter.showAdmin = admin == null || admin.isChecked();
		return filter;
	}

	private void renderDashboard(List<StatistikKunjunganDashboardUtil.DailyRow> rows, StatistikKunjunganDashboardUtil.Filter filter) {
		Div wrapper = new Div();
		wrapper.setStyle("width:100%;min-height:" + Math.max(420, height) + "px;box-sizing:border-box;padding:12px;background:#f6f8fb;overflow:auto;");
		wrapper.setParent(center);

		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.heroHtml(
				"Dashboard Statistik Kunjungan Pengguna",
				"Ringkasan ini membantu memantau jumlah pengguna yang berhasil login dari waktu ke waktu. Gunakan filter untuk melihat aktivitas berdasarkan unit akademik, sekolah, dan jenis pengguna.")));

		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.filterInfoHtml(filter, "Sumber data: log login aplikasi web.")));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.summaryCardsHtml(rows, filter)));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.trendPanelHtml("Tren Kunjungan Harian",
				"Menunjukkan pola naik-turun kunjungan per hari. Jika grafik meningkat tajam, biasanya sedang ada kegiatan akademik atau administrasi yang ramai diakses.",
				rows, filter)));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.compositionPanelHtml("Komposisi Kunjungan per Jenis Pengguna",
				"Memperlihatkan kelompok pengguna yang paling aktif mengakses sistem. Informasi ini membantu menentukan layanan mana yang perlu diprioritaskan.",
				rows, filter)));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.radarPanelHtml(rows, filter)));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.tablePanelHtml(rows, filter)));
	}

	public static TreeMap<Integer, Object[]> ambilDataKunjungan(Fakultas fakultas, Jurusan jurusan, Yayasan yayasan,
			Sekolah sekolah, String dateMulai, String dateSelesai, boolean mhs, boolean dsn, boolean ssw,
			boolean gr, boolean adm) {
		StatistikKunjunganDashboardUtil.Filter filter = new StatistikKunjunganDashboardUtil.Filter();
		filter.fakultas = fakultas;
		filter.jurusan = jurusan;
		filter.yayasan = yayasan;
		filter.sekolah = sekolah;
		filter.mulai = StatistikKunjunganDashboardUtil.parseDate(dateMulai);
		filter.sampai = StatistikKunjunganDashboardUtil.parseDate(dateSelesai);
		filter.showMahasiswa = mhs;
		filter.showDosen = dsn;
		filter.showSiswa = ssw;
		filter.showGuru = gr;
		filter.showAdmin = adm;
		List<StatistikKunjunganDashboardUtil.DailyRow> list = StatistikKunjunganDashboardUtil.loadDailyRows("log_login", filter, false);
		TreeMap<Integer, Object[]> data = new TreeMap<Integer, Object[]>();
		for (StatistikKunjunganDashboardUtil.DailyRow row : list) {
			if (row == null || row.tanggal == null) {
				continue;
			}
			try {
				Integer key = Integer.valueOf(Integer.parseInt(Common.dateFormat83.get().format(row.tanggal)));
				data.put(key, new Object[] { Double.valueOf(row.mahasiswa), Double.valueOf(row.dosen),
						Double.valueOf(row.siswa), Double.valueOf(row.guru), Double.valueOf(row.admin) });
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		return data;
	}
}

class StatistikKunjunganDashboardUtil {

	private static final NumberFormat INT = new DecimalFormat("#,##0");
	private static final int TOP_LIMIT = 12;

	static class Filter {
		Fakultas fakultas;
		Jurusan jurusan;
		Yayasan yayasan;
		Sekolah sekolah;
		Date mulai;
		Date sampai;
		boolean showMahasiswa = true;
		boolean showDosen = true;
		boolean showSiswa = true;
		boolean showGuru = true;
		boolean showAdmin = true;
		String linkProfile;
	}

	static class DailyRow {
		Date tanggal;
		long mahasiswa;
		long dosen;
		long siswa;
		long guru;
		long admin;

		long total(Filter filter) {
			long total = 0L;
			if (filter == null || filter.showMahasiswa) {
				total += mahasiswa;
			}
			if (filter == null || filter.showDosen) {
				total += dosen;
			}
			if (filter == null || filter.showSiswa) {
				total += siswa;
			}
			if (filter == null || filter.showGuru) {
				total += guru;
			}
			if (filter == null || filter.showAdmin) {
				total += admin;
			}
			return total;
		}
	}

	static class LabelRow {
		Date tanggal;
		String label;
		long jumlah;
	}

	static Div loading(String text) {
		Div div = new Div();
		div.setStyle("padding:18px;text-align:center;color:#64748b;background:#f8fafc;");
		div.appendChild(new Html("<i class='fa fa-spinner fa-spin'></i> " + safeHtml(text)));
		return div;
	}

	static String heroHtml(String title, String desc) {
		return "<div style='border-radius:18px;padding:18px;margin-bottom:12px;color:white;"
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);box-shadow:0 16px 34px rgba(15,23,42,.16);'>"
				+ "<div style='font-size:22px;font-weight:900;letter-spacing:.2px;'>" + safeHtml(title) + "</div>"
				+ "<div style='font-size:12px;line-height:1.6;opacity:.92;margin-top:6px;max-width:980px;'>" + safeHtml(desc) + "</div>"
				+ "</div>";
	}

	static String filterInfoHtml(Filter filter, String desc) {
		String periode = formatDate(filter == null ? null : filter.mulai) + " s.d " + formatDate(filter == null ? null : filter.sampai);
		return "<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:10px;margin-bottom:12px;'>"
				+ miniInfo("Periode", periode, "Rentang tanggal yang sedang dianalisis.")
				+ miniInfo("Unit Akademik", safeName(filter == null ? null : filter.fakultas) + " / " + safeName(filter == null ? null : filter.jurusan),
						"Filter fakultas dan program studi/jurusan.")
				+ miniInfo("Unit Sekolah", safeName(filter == null ? null : filter.yayasan) + " / " + safeName(filter == null ? null : filter.sekolah),
						"Filter yayasan dan sekolah.")
				+ miniInfo("Keterangan", desc, "Data hanya menghitung login yang berhasil.")
				+ "</div>";
	}

	private static String miniInfo(String title, String value, String desc) {
		return "<div style='padding:12px;border-radius:14px;background:#fff;border:1px solid #e5e7eb;box-shadow:0 8px 18px rgba(15,23,42,.05);'>"
				+ "<div style='font-size:11px;color:#64748b;font-weight:800;text-transform:uppercase;letter-spacing:.06em;'>" + safeHtml(title) + "</div>"
				+ "<div style='font-size:13px;color:#0f172a;font-weight:900;margin-top:5px;line-height:1.4;'>" + safeHtml(value) + "</div>"
				+ "<div style='font-size:11px;color:#64748b;margin-top:4px;line-height:1.45;'>" + safeHtml(desc) + "</div></div>";
	}

	static String summaryCardsHtml(List<DailyRow> rows, Filter filter) {
		long mahasiswa = sum(rows, "mahasiswa");
		long dosen = sum(rows, "dosen");
		long siswa = sum(rows, "siswa");
		long guru = sum(rows, "guru");
		long admin = sum(rows, "admin");
		long total = 0L;
		for (int i = 0; rows != null && i < rows.size(); i++) {
			total += rows.get(i).total(filter);
		}
		long hariAktif = 0L;
		for (int i = 0; rows != null && i < rows.size(); i++) {
			if (rows.get(i).total(filter) > 0L) {
				hariAktif++;
			}
		}
		return "<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:10px;margin-bottom:12px;'>"
				+ card("Total Kunjungan", total, "Jumlah login berhasil sesuai filter.")
				+ card("Hari Aktif", hariAktif, "Jumlah hari yang memiliki aktivitas login.")
				+ card("Mahasiswa", filter == null || filter.showMahasiswa ? mahasiswa : 0L, "Aktivitas login mahasiswa.")
				+ card("Dosen", filter == null || filter.showDosen ? dosen : 0L, "Aktivitas login dosen.")
				+ card("Siswa", filter == null || filter.showSiswa ? siswa : 0L, "Aktivitas login siswa.")
				+ card("Guru", filter == null || filter.showGuru ? guru : 0L, "Aktivitas login guru.")
				+ card("Admin/User", filter == null || filter.showAdmin ? admin : 0L, "Aktivitas login admin dan user lain.")
				+ "</div>";
	}

	private static String card(String title, long value, String desc) {
		title = ais.common.Common.getBahasaConfig(title);
		desc = ais.common.Common.getBahasaConfig(desc);
		return "<div style='padding:14px;border-radius:16px;background:#fff;border:1px solid #e5e7eb;box-shadow:0 10px 22px rgba(15,23,42,.06);'>"
				+ "<div style='font-size:11px;color:#64748b;font-weight:900;text-transform:uppercase;letter-spacing:.06em;'>" + safeHtml(title) + "</div>"
				+ "<div style='font-size:28px;font-weight:900;color:#0f172a;margin-top:7px;'>" + INT.format(value) + "</div>"
				+ "<div style='font-size:11px;color:#64748b;line-height:1.45;margin-top:4px;'>" + safeHtml(desc) + "</div></div>";
	}

	static String trendPanelHtml(String title, String desc, List<DailyRow> rows, Filter filter) {
		long max = 1L;
		for (int i = 0; rows != null && i < rows.size(); i++) {
			long t = rows.get(i).total(filter);
			if (t > max) {
				max = t;
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append(panelStart(title, desc));
		if (rows == null || rows.isEmpty()) {
			sb.append(empty("Belum ada data kunjungan pada periode ini."));
		} else {
			sb.append("<div style='display:flex;gap:7px;align-items:flex-end;overflow:auto;padding:16px 4px 4px 4px;min-height:190px;'>");
			for (int i = 0; i < rows.size(); i++) {
				DailyRow row = rows.get(i);
				long total = row.total(filter);
				int h = 18 + (int) Math.round((total * 130.0D) / max);
				sb.append("<div style='min-width:54px;text-align:center;'>")
						.append("<div style='height:150px;display:flex;align-items:flex-end;justify-content:center;'>")
						.append("<div title='").append(formatDate(row.tanggal)).append(": ").append(INT.format(total))
						.append("' style='width:34px;height:").append(h)
						.append("px;border-radius:12px 12px 5px 5px;background:linear-gradient(180deg,#2563eb,#06b6d4);box-shadow:0 9px 18px rgba(37,99,235,.20);'></div></div>")
						.append("<div style='font-size:11px;color:#0f172a;font-weight:900;'>").append(INT.format(total)).append("</div>")
						.append("<div style='font-size:10px;color:#64748b;white-space:nowrap;'>").append(formatDay(row.tanggal)).append("</div>")
						.append("</div>");
			}
			sb.append("</div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	static String compositionPanelHtml(String title, String desc, List<DailyRow> rows, Filter filter) {
		long mahasiswa = filter == null || filter.showMahasiswa ? sum(rows, "mahasiswa") : 0L;
		long dosen = filter == null || filter.showDosen ? sum(rows, "dosen") : 0L;
		long siswa = filter == null || filter.showSiswa ? sum(rows, "siswa") : 0L;
		long guru = filter == null || filter.showGuru ? sum(rows, "guru") : 0L;
		long admin = filter == null || filter.showAdmin ? sum(rows, "admin") : 0L;
		long total = Math.max(1L, mahasiswa + dosen + siswa + guru + admin);
		StringBuilder sb = new StringBuilder();
		sb.append(panelStart(title, desc));
		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:10px;'>");
		sb.append(progress("Mahasiswa", mahasiswa, total, "var(--ais-theme-primary,#2563eb)"));
		sb.append(progress("Dosen", dosen, total, "#7c3aed"));
		sb.append(progress("Siswa", siswa, total, "#16a34a"));
		sb.append(progress("Guru", guru, total, "#f97316"));
		sb.append(progress("Admin/User", admin, total, "#0f172a"));
		sb.append("</div></div>");
		return sb.toString();
	}

	static String radarPanelHtml(List<DailyRow> rows, Filter filter) {
		long total = 0L;
		long maxDay = 0L;
		for (int i = 0; rows != null && i < rows.size(); i++) {
			long v = rows.get(i).total(filter);
			total += v;
			if (v > maxDay) {
				maxDay = v;
			}
		}
		long hari = rows == null ? 0L : rows.size();
		int intensitas = percent(maxDay, Math.max(1L, total));
		int kontinuitas = percent(countActiveDays(rows, filter), Math.max(1L, hari));
		int akademik = percent(sum(rows, "mahasiswa") + sum(rows, "dosen"), Math.max(1L, total));
		int sekolah = percent(sum(rows, "siswa") + sum(rows, "guru"), Math.max(1L, total));
		int admin = percent(sum(rows, "admin"), Math.max(1L, total));

		String html = panelStart("Radar Kesehatan Aktivitas Sistem",
				"Merangkum kondisi aktivitas login dalam satu tampilan. Nilai yang seimbang menunjukkan sistem dipakai oleh berbagai kelompok pengguna secara sehat.")
				+ "<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px;align-items:center;'>"
				+ "<div style='text-align:center;padding:14px;border-radius:18px;background:#f8fafc;border:1px solid #e2e8f0;'>"
				+ "<div style='margin:auto;width:180px;height:180px;border-radius:999px;position:relative;background:conic-gradient(var(--ais-theme-primary,#2563eb) 0 "
				+ intensitas + "%,#e2e8f0 " + intensitas + "% 100%);box-shadow:inset 0 0 0 18px #fff,0 12px 24px rgba(15,23,42,.08);'>"
				+ "<div style='position:absolute;inset:42px;border-radius:999px;background:conic-gradient(#16a34a 0 "
				+ kontinuitas + "%,#e2e8f0 " + kontinuitas + "% 100%);'></div>"
				+ "<div style='position:absolute;inset:72px;border-radius:999px;background:white;display:flex;align-items:center;justify-content:center;font-size:22px;font-weight:900;color:#0f172a;'>"
				+ kontinuitas + "%</div></div>"
				+ "<div style='font-size:11px;color:#64748b;margin-top:10px;'>Lingkar luar: puncak aktivitas, lingkar dalam: kontinuitas hari aktif.</div></div>"
				+ "<div>"
				+ gauge("Intensitas Puncak", intensitas, "var(--ais-theme-primary,#2563eb)")
				+ gauge("Kontinuitas", kontinuitas, "#16a34a")
				+ gauge("Akademik PT", akademik, "#7c3aed")
				+ gauge("Sekolah", sekolah, "#f97316")
				+ gauge("Admin/User", admin, "#0f172a")
				+ "</div></div></div>";
		return html;
	}

	static String tablePanelHtml(List<DailyRow> rows, Filter filter) {
		StringBuilder sb = new StringBuilder();
		sb.append(panelStart("Tabel Ringkas Kunjungan Harian",
				"Tabel ini menampilkan angka kunjungan per hari sebagai pelengkap grafik. Gunakan tabel ini ketika membutuhkan angka pasti untuk pemeriksaan cepat."));
		sb.append("<div style='overflow:auto;max-height:420px;'><table style='width:100%;border-collapse:collapse;font-size:12px;'>")
				.append("<thead><tr style='background:#f1f5f9;color:#0f172a;'>")
				.append(th("Tanggal")).append(th("Mahasiswa")).append(th("Dosen")).append(th("Siswa"))
				.append(th("Guru")).append(th("Admin/User")).append(th("Total")).append("</tr></thead><tbody>");
		if (rows == null || rows.isEmpty()) {
			sb.append("<tr><td colspan='7' style='padding:12px;color:#64748b;text-align:center;'>Belum ada data.</td></tr>");
		} else {
			for (int i = rows.size() - 1; i >= 0; i--) {
				DailyRow row = rows.get(i);
				sb.append("<tr>")
						.append(td(formatDate(row.tanggal)))
						.append(td(INT.format(row.mahasiswa)))
						.append(td(INT.format(row.dosen)))
						.append(td(INT.format(row.siswa)))
						.append(td(INT.format(row.guru)))
						.append(td(INT.format(row.admin)))
						.append(td("<b>" + INT.format(row.total(filter)) + "</b>"))
						.append("</tr>");
			}
		}
		sb.append("</tbody></table></div></div>");
		return sb.toString();
	}

	static String labelTrendPanelHtml(String title, String desc, List<LabelRow> rows) {
		TreeMap<String, Long> totalPerLabel = new TreeMap<String, Long>();
		for (int i = 0; rows != null && i < rows.size(); i++) {
			LabelRow row = rows.get(i);
			if (row == null || row.label == null) {
				continue;
			}
			Long current = totalPerLabel.get(row.label);
			totalPerLabel.put(row.label, Long.valueOf((current == null ? 0L : current.longValue()) + row.jumlah));
		}
		List<Map.Entry<String, Long>> entries = new ArrayList<Map.Entry<String, Long>>(totalPerLabel.entrySet());
		java.util.Collections.sort(entries, new java.util.Comparator<Map.Entry<String, Long>>() {
			@Override
			public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		long max = 1L;
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).getValue().longValue() > max) {
				max = entries.get(i).getValue().longValue();
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append(panelStart(title, desc));
		if (entries.isEmpty()) {
			sb.append(empty("Belum ada data pada periode ini."));
		} else {
			sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:10px;'>");
			int limit = Math.min(TOP_LIMIT, entries.size());
			for (int i = 0; i < limit; i++) {
				Map.Entry<String, Long> e = entries.get(i);
				sb.append(progress(e.getKey(), e.getValue().longValue(), max, pickColor(i)));
			}
			sb.append("</div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	static String labelTablePanelHtml(String title, String desc, List<LabelRow> rows) {
		StringBuilder sb = new StringBuilder();
		sb.append(panelStart(title, desc));
		sb.append("<div style='overflow:auto;max-height:420px;'><table style='width:100%;border-collapse:collapse;font-size:12px;'>")
				.append("<thead><tr style='background:#f1f5f9;color:#0f172a;'>")
				.append(th("Tanggal")).append(th("Kategori")).append(th("Jumlah")).append("</tr></thead><tbody>");
		if (rows == null || rows.isEmpty()) {
			sb.append("<tr><td colspan='3' style='padding:12px;color:#64748b;text-align:center;'>Belum ada data.</td></tr>");
		} else {
			int start = Math.max(0, rows.size() - 200);
			for (int i = rows.size() - 1; i >= start; i--) {
				LabelRow row = rows.get(i);
				sb.append("<tr>").append(td(formatDate(row.tanggal))).append(td(safeHtml(row.label)))
						.append(td(INT.format(row.jumlah))).append("</tr>");
			}
		}
		sb.append("</tbody></table></div></div>");
		return sb.toString();
	}

	static String labelRadarPanelHtml(String title, String desc, List<LabelRow> rows) {
		TreeMap<String, Long> totals = new TreeMap<String, Long>();
		long all = 0L;
		for (int i = 0; rows != null && i < rows.size(); i++) {
			LabelRow row = rows.get(i);
			if (row == null || row.label == null) {
				continue;
			}
			Long current = totals.get(row.label);
			totals.put(row.label, Long.valueOf((current == null ? 0L : current.longValue()) + row.jumlah));
			all += row.jumlah;
		}
		List<Map.Entry<String, Long>> entries = new ArrayList<Map.Entry<String, Long>>(totals.entrySet());
		java.util.Collections.sort(entries, new java.util.Comparator<Map.Entry<String, Long>>() {
			@Override
			public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		StringBuilder sb = new StringBuilder();
		sb.append(panelStart(title, desc));
		if (entries.isEmpty()) {
			sb.append(empty("Belum ada data untuk membuat radar."));
		} else {
			sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px;align-items:center;'>");
			long top = entries.get(0).getValue().longValue();
			int topPct = percent(top, Math.max(1L, all));
			sb.append("<div style='text-align:center;padding:14px;border-radius:18px;background:#f8fafc;border:1px solid #e2e8f0;'>")
					.append("<div style='margin:auto;width:170px;height:170px;border-radius:999px;background:conic-gradient(var(--ais-theme-primary,#2563eb) 0 ")
					.append(topPct).append("%,#e2e8f0 ").append(topPct)
					.append("% 100%);box-shadow:inset 0 0 0 20px #fff,0 12px 24px rgba(15,23,42,.08);display:flex;align-items:center;justify-content:center;'>")
					.append("<span style='font-size:22px;font-weight:900;color:#0f172a;'>").append(topPct).append("%</span></div>")
					.append("<div style='font-size:11px;color:#64748b;margin-top:10px;'>Persentase kategori terbesar terhadap total kunjungan.</div></div>");
			sb.append("<div>");
			int limit = Math.min(8, entries.size());
			for (int i = 0; i < limit; i++) {
				Map.Entry<String, Long> e = entries.get(i);
				sb.append(gauge(e.getKey(), percent(e.getValue().longValue(), Math.max(1L, all)), pickColor(i)));
			}
			sb.append("</div></div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private static String panelStart(String title, String desc) {
		return "<div style='border-radius:18px;background:#fff;border:1px solid #e5e7eb;box-shadow:0 12px 24px rgba(15,23,42,.07);padding:14px;margin-bottom:12px;'>"
				+ "<div style='font-size:15px;font-weight:900;color:#0f172a;'>" + safeHtml(title) + "</div>"
				+ "<div style='font-size:12px;color:#64748b;line-height:1.55;margin-top:5px;margin-bottom:10px;'>" + safeHtml(desc) + "</div>";
	}

	private static String progress(String label, long value, long total, String color) {
		int pct = percent(value, total);
		return "<div style='padding:10px;border-radius:14px;background:#f8fafc;border:1px solid #e2e8f0;'>"
				+ "<div style='display:flex;justify-content:space-between;gap:10px;font-size:12px;font-weight:900;color:#0f172a;'>"
				+ "<span>" + safeHtml(label) + "</span><span>" + INT.format(value) + " (" + pct + "%)</span></div>"
				+ "<div style='height:12px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:8px;'>"
				+ "<div style='height:12px;width:" + pct + "%;background:" + color + ";border-radius:999px;'></div></div></div>";
	}

	private static String gauge(String label, int value, String color) {
		if (value < 0) {
			value = 0;
		}
		if (value > 100) {
			value = 100;
		}
		return "<div style='margin-bottom:8px;'><div style='display:flex;justify-content:space-between;font-size:11px;font-weight:900;color:#0f172a;'>"
				+ "<span>" + safeHtml(label) + "</span><span>" + value + "%</span></div>"
				+ "<div style='height:10px;border-radius:999px;background:#e2e8f0;overflow:hidden;margin-top:4px;'>"
				+ "<div style='height:10px;width:" + value + "%;background:" + color + ";border-radius:999px;'></div></div></div>";
	}

	private static String empty(String text) {
		return "<div style='padding:14px;border-radius:14px;background:#f8fafc;border:1px dashed #cbd5e1;color:#64748b;font-size:12px;text-align:center;'>"
				+ safeHtml(text) + "</div>";
	}

	private static String th(String text) {
		return "<th style='padding:9px;border-bottom:1px solid #e2e8f0;text-align:left;white-space:nowrap;'>" + safeHtml(text) + "</th>";
	}

	private static String td(String text) {
		return "<td style='padding:8px;border-bottom:1px solid #e2e8f0;color:#334155;'>" + text + "</td>";
	}

	private static long sum(List<DailyRow> rows, String field) {
		long total = 0L;
		for (int i = 0; rows != null && i < rows.size(); i++) {
			DailyRow row = rows.get(i);
			if ("mahasiswa".equals(field)) {
				total += row.mahasiswa;
			} else if ("dosen".equals(field)) {
				total += row.dosen;
			} else if ("siswa".equals(field)) {
				total += row.siswa;
			} else if ("guru".equals(field)) {
				total += row.guru;
			} else if ("admin".equals(field)) {
				total += row.admin;
			}
		}
		return total;
	}

	private static long countActiveDays(List<DailyRow> rows, Filter filter) {
		long total = 0L;
		for (int i = 0; rows != null && i < rows.size(); i++) {
			if (rows.get(i).total(filter) > 0L) {
				total++;
			}
		}
		return total;
	}

	static List<DailyRow> loadDailyRows(String tableName, Filter filter, boolean mobile) {
		Session session = null;
		List<DailyRow> result = new ArrayList<DailyRow>();
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			StringBuilder sql = new StringBuilder();
			sql.append("select date(a.\"login\") as tanggal, ")
					.append("sum(case when a.mahasiswa is not null then 1 else 0 end) as mahasiswa, ")
					.append("sum(case when a.dosen is not null then 1 else 0 end) as dosen, ")
					.append("sum(case when a.siswa is not null then 1 else 0 end) as siswa, ");
			if (mobile) {
				sql.append("sum(case when d.guru is not null then 1 else 0 end) as guru, ")
						.append("sum(case when d.usernama is not null and a.dosen is null and d.guru is null then 1 else 0 end) as tbmuser ");
			} else {
				sql.append("sum(case when a.guru is not null then 1 else 0 end) as guru, ")
						.append("sum(case when d.usernama is not null and a.dosen is null and a.guru is null then 1 else 0 end) as tbmuser ");
			}
			sql.append("from ").append(tableName).append(" a left join tbmuser d on (a.tbmuser = d.userid) ")
					.append("where a.\"login\" >= cast(:mulai as date) and a.\"login\" < (cast(:sampai as date) + interval '1 day') ")
					.append("and a.success_status = true ");
			appendCommonFilterSql(sql, filter, true);
			if (mobile && filter != null && hasText(filter.linkProfile)) {
				sql.append(" and lower(coalesce(a.link_profile,'')) like :linkProfile ");
			}
			sql.append(" group by date(a.\"login\") order by date(a.\"login\") ");
			SQLQuery query = session.createSQLQuery(sql.toString());
			bindCommon(query, filter);
			if (mobile && filter != null && hasText(filter.linkProfile)) {
				query.setParameter("linkProfile", "%" + filter.linkProfile.trim().toLowerCase() + "%");
			}
			query.setFetchSize(300);
			List list = query.list();
			for (int i = 0; list != null && i < list.size(); i++) {
				Object[] o = toArray(list.get(i));
				DailyRow row = new DailyRow();
				row.tanggal = toDate(o, 0);
				row.mahasiswa = toLong(o, 1);
				row.dosen = toLong(o, 2);
				row.siswa = toLong(o, 3);
				row.guru = toLong(o, 4);
				row.admin = toLong(o, 5);
				result.add(row);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(session);
		}
		return result;
	}

	static List<LabelRow> loadLabelRows(String tableName, String mode, Filter filter, boolean mobile) {
		Session session = null;
		List<LabelRow> result = new ArrayList<LabelRow>();
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			String labelExpr;
			String join = "";
			if ("jenis".equals(mode)) {
				join = " left join tbmrole r on (d.userrole = r.roleid) ";
				labelExpr = "(case when a.mahasiswa is not null then 'Mahasiswa' when a.siswa is not null then 'Siswa' "
						+ "when a.dosen is not null then 'Dosen' when " + (mobile ? "d.guru" : "a.guru")
						+ " is not null then 'Guru' else coalesce(r.rolename,'Admin/User') end)";
			} else {
				join = " left join rab.satuan_kerja r on (a.satuan_kerja = r.id) ";
				labelExpr = "coalesce(r.nama,'Tanpa Satuan Kerja')";
			}
			StringBuilder sql = new StringBuilder();
			sql.append("select date(a.\"login\") as tanggal, ").append(labelExpr).append(" as label, count(a.id) as jumlah ")
					.append("from ").append(tableName).append(" a left join tbmuser d on (a.tbmuser = d.userid) ")
					.append(join)
					.append("where a.\"login\" >= cast(:mulai as date) and a.\"login\" < (cast(:sampai as date) + interval '1 day') ")
					.append("and a.success_status = true ");
			appendCommonFilterSql(sql, filter, false);
			if (filter != null && !filter.showMahasiswa) {
				sql.append(" and a.mahasiswa is null ");
			}
			if (filter != null && !filter.showDosen) {
				sql.append(" and a.dosen is null ");
			}
			if (filter != null && !filter.showSiswa) {
				sql.append(" and a.siswa is null ");
			}
			if (filter != null && !filter.showGuru) {
				if (mobile) {
					sql.append(" and d.guru is null ");
				} else {
					sql.append(" and a.guru is null ");
				}
			}
			if (mobile && filter != null && hasText(filter.linkProfile)) {
				sql.append(" and lower(coalesce(a.link_profile,'')) like :linkProfile ");
			}
			sql.append(" group by date(a.\"login\"), ").append(labelExpr).append(" order by date(a.\"login\"), ").append(labelExpr);
			SQLQuery query = session.createSQLQuery(sql.toString());
			bindCommon(query, filter);
			if (mobile && filter != null && hasText(filter.linkProfile)) {
				query.setParameter("linkProfile", "%" + filter.linkProfile.trim().toLowerCase() + "%");
			}
			query.setFetchSize(500);
			List list = query.list();
			for (int i = 0; list != null && i < list.size(); i++) {
				Object[] o = toArray(list.get(i));
				LabelRow row = new LabelRow();
				row.tanggal = toDate(o, 0);
				row.label = o.length > 1 && o[1] != null ? String.valueOf(o[1]) : "Tidak Teridentifikasi";
				row.jumlah = toLong(o, 2);
				result.add(row);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(session);
		}
		return result;
	}

	private static void appendCommonFilterSql(StringBuilder sql, Filter filter, boolean includeNothing) {
		if (filter == null) {
			return;
		}
		if (filter.jurusan != null && filter.jurusan.getId() != null) {
			sql.append(" and a.jurusan = :jurusan ");
		}
		if (filter.fakultas != null && filter.fakultas.getId() != null) {
			sql.append(" and a.fakultas = :fakultas ");
		}
		if (filter.yayasan != null && filter.yayasan.getId() != null) {
			sql.append(" and (a.yayasan is null or a.yayasan = :yayasan) ");
		}
		if (filter.sekolah != null && filter.sekolah.getId() != null) {
			sql.append(" and a.sekolah = :sekolah ");
		}
	}

	private static void bindCommon(SQLQuery query, Filter filter) {
		if (filter == null) {
			query.setParameter("mulai", Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate()));
			query.setParameter("sampai", Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate()));
			return;
		}
		Date mulai = filter.mulai == null ? ais.ui.util.WaktuUtil.getDate() : filter.mulai;
		Date sampai = filter.sampai == null ? ais.ui.util.WaktuUtil.getDate() : filter.sampai;
		query.setParameter("mulai", Common.databaseDateFormat.get().format(mulai));
		query.setParameter("sampai", Common.databaseDateFormat.get().format(sampai));
		if (filter.jurusan != null && filter.jurusan.getId() != null) {
			query.setParameter("jurusan", filter.jurusan.getId());
		}
		if (filter.fakultas != null && filter.fakultas.getId() != null) {
			query.setParameter("fakultas", filter.fakultas.getId());
		}
		if (filter.yayasan != null && filter.yayasan.getId() != null) {
			query.setParameter("yayasan", filter.yayasan.getId());
		}
		if (filter.sekolah != null && filter.sekolah.getId() != null) {
			query.setParameter("sekolah", filter.sekolah.getId());
		}
	}

	private static Object[] toArray(Object row) {
		if (row instanceof Object[]) {
			return (Object[]) row;
		}
		return new Object[] { row };
	}

	private static Date toDate(Object[] row, int index) {
		if (row == null || row.length <= index || row[index] == null) {
			return null;
		}
		if (row[index] instanceof Date) {
			return (Date) row[index];
		}
		try {
			return Common.databaseDateFormat.get().parse(String.valueOf(row[index]));
		} catch (Exception e) {
			return null;
		}
	}

	private static long toLong(Object[] row, int index) {
		if (row == null || row.length <= index || row[index] == null) {
			return 0L;
		}
		if (row[index] instanceof Number) {
			return ((Number) row[index]).longValue();
		}
		try {
			return Long.parseLong(String.valueOf(row[index]));
		} catch (Exception e) {
			return 0L;
		}
	}

	private static int percent(long value, long total) {
		if (total <= 0L) {
			return 0;
		}
		int p = (int) Math.round((value * 100.0D) / total);
		if (p < 0) {
			return 0;
		}
		if (p > 100) {
			return 100;
		}
		return p;
	}

	static String errorHtml(String title, Exception e) {
		return "<div style='margin:12px;padding:14px;border-radius:14px;background:#fee2e2;border:1px solid #fecaca;color:#991b1b;'>"
				+ "<b>" + safeHtml(title) + "</b><br/>" + safeHtml(e == null ? "" : e.getMessage()) + "</div>";
	}

	private static String safeName(Object object) {
		return object == null ? "Semua" : String.valueOf(object);
	}

	private static String safeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private static String formatDate(Date date) {
		if (date == null) {
			return "-";
		}
		try {
			return Common.dateFormat.get().format(date);
		} catch (Exception e) {
			return String.valueOf(date);
		}
	}

	private static String formatDay(Date date) {
		if (date == null) {
			return "-";
		}
		try {
			return Common.dateFormat83.get().format(date);
		} catch (Exception e) {
			return formatDate(date);
		}
	}

	static Date parseDate(String value) {
		if (value == null || value.trim().length() == 0) {
			return null;
		}
		try {
			return Common.databaseDateFormat.get().parse(value.trim());
		} catch (Exception e) {
			return null;
		}
	}

	private static String pickColor(int index) {
		String[] colors = new String[] { "var(--ais-theme-primary,#2563eb)", "#16a34a", "#f97316", "#7c3aed", "#0891b2", "#dc2626",
				"#0f172a", "#65a30d", "#be185d", "#9333ea", "#0d9488", "#ca8a04" };
		if (index < 0) {
			index = 0;
		}
		return colors[index % colors.length];
	}

	static Fakultas selectedFakultas(Combobox combo) {
		Object value = selectedValue(combo);
		return value instanceof Fakultas ? (Fakultas) value : null;
	}

	static Jurusan selectedJurusan(Combobox combo) {
		Object value = selectedValue(combo);
		return value instanceof Jurusan ? (Jurusan) value : null;
	}

	static Yayasan selectedYayasan(Combobox combo) {
		Object value = selectedValue(combo);
		return value instanceof Yayasan ? (Yayasan) value : null;
	}

	static Sekolah selectedSekolah(Combobox combo) {
		Object value = selectedValue(combo);
		return value instanceof Sekolah ? (Sekolah) value : null;
	}

	private static Object selectedValue(Combobox combo) {
		try {
			return combo == null || combo.getSelectedItem() == null ? null : combo.getSelectedItem().getValue();
		} catch (Exception e) {
			return null;
		}
	}

	static boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}

	static void closeSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				session.clear();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardStatistikKunjunganPengguna.java:1181");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardStatistikKunjunganPengguna.java:1187");
		}
	}
}
