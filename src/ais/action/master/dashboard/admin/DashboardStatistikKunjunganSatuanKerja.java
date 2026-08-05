
package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

/*
 * DASHBOARD_STATISTIK_KUNJUNGAN_SATUAN_KERJA_HTML_CSS_2026
 *
 * Dashboard per satuan kerja tanpa grafik lama. Grafik/trend dibuat memakai HTML dan CSS modern.
 */

import java.util.Calendar;
import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
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
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardStatistikKunjunganSatuanKerja extends MyWindow {

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
	private MyCheckboxConfig siswa;
	private MyCheckboxConfig guru;
	private int width = 1200;
	private int height = 500;

	public DashboardStatistikKunjunganSatuanKerja() throws Exception {
		super();
		initFakultas();
		init();
		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				initChart();
			}
		});
	}

	public DashboardStatistikKunjunganSatuanKerja(int width, int height) throws Exception {
		super();
		reinit(width, height);
	}

	public void reinit(int width, int height) throws Exception {
		this.width = width;
		this.height = height;
		initFakultas();
		init();
		initChart();
	}

	public DashboardStatistikKunjunganSatuanKerja(String title, String border, boolean closable) throws Exception {
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
		DashboardGridExportHelper.pasang(this, "Statistik Kunjungan per Satuan Kerja");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Atur saringan untuk menyesuaikan data kunjungan yang ditampilkan.",
				"Statistik Kunjungan per Satuan Kerja",
				"Banyaknya kunjungan pengguna sistem di tiap satuan kerja, lengkap dengan grafiknya.");
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
		refresh.setParent(toolbar);
		refresh.setTooltiptext("Memuat ulang statistik satuan kerja.");
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

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

		mahasiswa.setChecked(true);
		dosen.setChecked(true);
		siswa.setChecked(true);
		guru.setChecked(true);
		addReload(mahasiswa);
		addReload(dosen);
		addReload(siswa);
		addReload(guru);

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
		StatistikKunjunganDashboardUtil.loading("Mengambil statistik per satuan kerja...").setParent(center);
		try {
			StatistikKunjunganDashboardUtil.Filter filter = readFilter();
			List<StatistikKunjunganDashboardUtil.LabelRow> rows = StatistikKunjunganDashboardUtil.loadLabelRows(
					"log_login", "satuanKerja", filter, false);
			Common.clear(center);
			renderDashboard(rows, filter);
		} catch (Exception e) {
			Common.clear(center);
			center.appendChild(new Html(StatistikKunjunganDashboardUtil.errorHtml(
					"Gagal memuat statistik satuan kerja", e)));
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
		filter.showAdmin = true;
		return filter;
	}

	private void renderDashboard(List<StatistikKunjunganDashboardUtil.LabelRow> rows, StatistikKunjunganDashboardUtil.Filter filter) {
		Div wrapper = new Div();
		wrapper.setStyle("width:100%;min-height:" + Math.max(480, height) + "px;box-sizing:border-box;padding:12px;background:#f6f8fb;overflow:auto;");
		wrapper.setParent(center);
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.heroHtml(
				"Dashboard Kunjungan per Satuan Kerja",
				"Ringkasan ini memperlihatkan unit kerja yang paling banyak menghasilkan aktivitas login. Informasi ini membantu pimpinan melihat unit mana yang paling aktif memakai aplikasi.")));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.filterInfoHtml(filter, "Sumber data: log login, dikelompokkan berdasarkan satuan kerja.")));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.labelTrendPanelHtml(
				"Ranking Satuan Kerja",
				"Merangkum total kunjungan per satuan kerja. Satuan kerja dengan angka tinggi biasanya paling banyak memakai layanan sistem pada periode ini.",
				rows)));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.labelRadarPanelHtml(
				"Radar Distribusi Satuan Kerja",
				"Panel radar ini membantu melihat apakah penggunaan sistem merata antar unit atau hanya terkonsentrasi pada beberapa unit tertentu.",
				rows)));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.labelTablePanelHtml(
				"Tabel Kunjungan per Tanggal dan Satuan Kerja",
				"Tabel ini menampilkan rincian kunjungan harian per satuan kerja agar angka pada grafik bisa diperiksa dengan mudah.",
				rows)));
	}
}
