
package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

/*
 * DASHBOARD_STATISTIK_KUNJUNGAN_PENGGUNA_MOBILE_HTML_CSS_2026
 *
 * Dashboard akses mobile tanpa grafik lama/rendering gambar lama. Grafik dan tren dirender dengan HTML/CSS modern.
 */

import java.util.Calendar;
import java.util.List;

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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardStatistikKunjunganPenggunaMobile extends MyWindow {

	private static final long serialVersionUID = -28636873241676666L;

	private Div center;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();
	private MyDatebox mulai = new MyDatebox();
	private MyDatebox sampai = new MyDatebox();
	private MyCheckboxConfig mahasiswa;
	private MyTextbox searchLinkProfile;
	private MyCheckboxConfig dosen;
	private MyCheckboxConfig admin;
	private MyCheckboxConfig siswa;
	private MyCheckboxConfig guru;
	private boolean tampilRinci = false;
	private int width = 1200;
	private int height = 430;

	public DashboardStatistikKunjunganPenggunaMobile() throws Exception {
		super();
		initFakultas();
		init();
		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				initChart();
			}
		});
	}

	public DashboardStatistikKunjunganPenggunaMobile(int width, int height) throws Exception {
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

	public DashboardStatistikKunjunganPenggunaMobile(String title, String border, boolean closable) throws Exception {
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
		DashboardGridExportHelper.pasang(this, "Statistik Kunjungan Pengguna Mobile");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Atur saringan untuk menyesuaikan data kunjungan yang ditampilkan.",
				"Statistik Kunjungan Pengguna (Mobile)",
				"Banyaknya kunjungan pengguna sistem dari aplikasi mobile, lengkap dengan grafiknya.");
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
		refresh.setTooltiptext("Memuat ulang statistik akses mobile.");
		refresh.setParent(toolbar);
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		if (tampilRinci && Common.getApakahAdmin()) {
			MyButtonConfig detail = new MyButtonConfig("Lihat Rinci", "/img/12123-eyes-icon.png");
			detail.setParent(toolbar);
			detail.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					MyWindow laporan = new MyWindow();
					laporan.setHeight("99%");
					laporan.setWidth("99%");
					laporan.setTitle("Rekap Kunjungan Mobile");
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
		for (int i = 0; i < 6; i++) {
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

		searchLinkProfile = new MyTextbox();
		searchLinkProfile.setWidth("95%");
		searchLinkProfile.setTooltiptext("Isi kata kunci perangkat/aplikasi mobile, lalu tekan Enter.");
		row.appendChild(searchLinkProfile);
		searchLinkProfile.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		Hbox tanggalBox = new Hbox();
		tanggalBox.setAlign("center");
		row.appendChild(tanggalBox);
		Calendar dateMulai = ais.ui.util.WaktuUtil.getCalendar();
		dateMulai.set(Calendar.MONTH, dateMulai.get(Calendar.MONTH) - 1);
		mulai.setValue(dateMulai.getTime());
		mulai.setCols(4);
		mulai.setReadonly(true);
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
		tanggalBox.appendChild(sampai);
		sampai.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "6");
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
		StatistikKunjunganDashboardUtil.loading("Mengambil statistik kunjungan mobile...").setParent(center);

		try {
			StatistikKunjunganDashboardUtil.Filter filter = readFilter();
			List<StatistikKunjunganDashboardUtil.DailyRow> rows = StatistikKunjunganDashboardUtil.loadDailyRows(
					"log_mobile", filter, true);
			Common.clear(center);
			renderDashboard(rows, filter);
		} catch (Exception e) {
			Common.clear(center);
			center.appendChild(new Html(StatistikKunjunganDashboardUtil.errorHtml(
					"Gagal memuat statistik kunjungan mobile", e)));
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
		filter.linkProfile = searchLinkProfile == null ? null : searchLinkProfile.getValue();
		return filter;
	}

	private void renderDashboard(List<StatistikKunjunganDashboardUtil.DailyRow> rows, StatistikKunjunganDashboardUtil.Filter filter) {
		Div wrapper = new Div();
		wrapper.setStyle("width:100%;min-height:" + Math.max(420, height) + "px;box-sizing:border-box;padding:12px;background:#f6f8fb;overflow:auto;");
		wrapper.setParent(center);
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.heroHtml(
				"Dashboard Statistik Kunjungan Mobile",
				"Ringkasan ini memantau login yang berasal dari perangkat atau aplikasi mobile. Membantu melihat seberapa besar penggunaan mobile dibandingkan akses biasa.")));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.filterInfoHtml(filter, "Sumber data: log mobile aplikasi.")));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.summaryCardsHtml(rows, filter)));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.trendPanelHtml("Tren Kunjungan Mobile Harian",
				"Memperlihatkan pola akses mobile setiap hari. Jika naik tajam, berarti pengguna lebih banyak memakai perangkat mobile pada periode tersebut.",
				rows, filter)));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.compositionPanelHtml("Komposisi Pengguna Mobile",
				"Menunjukkan kelompok pengguna yang paling banyak memakai akses mobile, sehingga pengelola dapat memprioritaskan perbaikan tampilan mobile.",
				rows, filter)));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.radarPanelHtml(rows, filter)));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.tablePanelHtml(rows, filter)));
	}
}
