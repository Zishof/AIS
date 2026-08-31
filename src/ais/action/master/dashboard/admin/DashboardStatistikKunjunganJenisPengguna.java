
package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

/*
 * DASHBOARD_STATISTIK_KUNJUNGAN_JENIS_PENGGUNA_HTML_CSS_2026
 *
 * Dashboard per jenis pengguna tanpa grafik lama. Grafik/trend dibuat memakai HTML dan CSS modern.
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

/**
 * Panel dasbor admin "Statistik Kunjungan per Jenis Pengguna": menampilkan ringkasan jumlah
 * kunjungan (login) sistem dikelompokkan berdasarkan jenis pengguna (mahasiswa, dosen, siswa,
 * guru — masing-masing dapat dimatikan lewat checkbox), difilter fakultas/prodi, yayasan/sekolah,
 * dan rentang tanggal (default sebulan terakhir). Berbeda dari dasbor grafik versi lama, panel
 * ini dirender murni sebagai HTML/CSS modern (bukan komponen grafik ZK) lewat
 * {@link StatistikKunjunganDashboardUtil} — menghasilkan panel ranking, radar distribusi, dan
 * tabel detail per tanggal/jenis pengguna, dibangun dari sumber data {@code log_login}
 * dikelompokkan kolom {@code jenis}. Perubahan filter apa pun langsung memicu pemuatan ulang data
 * ({@link #initChart}). Mendukung tiga mode konstruksi: mandiri (dasbor penuh dengan timer refresh
 * otomatis), ukuran kustom (dipakai saat disematkan di layar lain), dan dengan judul/border/
 * closable kustom.
 */
public class DashboardStatistikKunjunganJenisPengguna extends MyWindow {

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

	/** Membuat dasbor mandiri dengan ukuran default (1200x500), memasang timer refresh otomatis (non-busy) yang memuat ulang statistik secara berkala. */
	public DashboardStatistikKunjunganJenisPengguna() throws Exception {
		super();
		initFakultas();
		init();
		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				initChart();
			}
		});
	}

	/** Membuat dasbor dengan ukuran kustom {@code width}x{@code height} (dipakai saat disematkan di layar lain), tanpa timer refresh otomatis. */
	public DashboardStatistikKunjunganJenisPengguna(int width, int height) throws Exception {
		super();
		reinit(width, height);
	}

	/** Membangun ulang tata letak dan data dasbor dengan ukuran {@code width}x{@code height} yang baru. */
	public void reinit(int width, int height) throws Exception {
		this.width = width;
		this.height = height;
		initFakultas();
		init();
		initChart();
	}

	/** Membuat dasbor dengan judul/border/closable kustom, langsung memuat data awal tanpa timer refresh otomatis. */
	public DashboardStatistikKunjunganJenisPengguna(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	/** Mengisi dropdown filter fakultas/jurusan dan yayasan/sekolah beserta opsi "Semua". */
	private void initFakultas() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);
	}

	/** Membangun tata letak panel: portal responsif (saringan + area konten), grid filter (fakultas/jurusan/yayasan/sekolah/rentang tanggal), dan checkbox jenis pengguna — seluruhnya memicu {@link #initChart} saat berubah. */
	private void init() {
		DashboardGridExportHelper.pasang(this, "Statistik Kunjungan Jenis Pengguna");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Atur saringan untuk menyesuaikan data kunjungan yang ditampilkan.",
				"Statistik Kunjungan per Jenis Pengguna",
				"Banyaknya kunjungan menurut jenis pengguna (mahasiswa, dosen, pegawai, dll), lengkap dengan grafiknya.");
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
		refresh.setTooltiptext("Memuat ulang statistik jenis pengguna.");
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

	/** Menambahkan {@code combo} ke {@code row} dengan tooltip dan pemicu refresh otomatis saat pilihan berubah. */
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

	/** Memasang handler yang memicu {@link #initChart} setiap kali {@code checkbox} diklik. */
	private void addReload(MyCheckboxConfig checkbox) {
		checkbox.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});
	}

	/** Memuat ulang statistik kunjungan berdasarkan filter saat ini dan merender ulang panel dasbor; menampilkan pesan galat pada panel bila pemuatan gagal. */
	private void initChart() {
		if (center == null) {
			return;
		}
		Common.clear(center);
		StatistikKunjunganDashboardUtil.loading("Mengambil statistik per jenis pengguna...").setParent(center);
		try {
			StatistikKunjunganDashboardUtil.Filter filter = readFilter();
			List<StatistikKunjunganDashboardUtil.LabelRow> rows = StatistikKunjunganDashboardUtil.loadLabelRows(
					"log_login", "jenis", filter, false);
			Common.clear(center);
			renderDashboard(rows, filter);
		} catch (Exception e) {
			Common.clear(center);
			center.appendChild(new Html(StatistikKunjunganDashboardUtil.errorHtml(
					"Gagal memuat statistik jenis pengguna", e)));
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Membaca nilai filter saat ini (fakultas/jurusan/yayasan/sekolah, rentang tanggal, jenis pengguna yang ditampilkan) menjadi objek {@link StatistikKunjunganDashboardUtil.Filter}. */
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

	/** Merender panel dasbor HTML lengkap (hero, info filter, ranking, radar distribusi, tabel detail) dari baris data {@code rows} ke area konten. */
	private void renderDashboard(List<StatistikKunjunganDashboardUtil.LabelRow> rows, StatistikKunjunganDashboardUtil.Filter filter) {
		Div wrapper = new Div();
		wrapper.setStyle("width:100%;min-height:" + Math.max(480, height) + "px;box-sizing:border-box;padding:12px;background:#f6f8fb;overflow:auto;");
		wrapper.setParent(center);
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.heroHtml(
				"Dashboard Kunjungan per Jenis Pengguna",
				"Ringkasan ini memperlihatkan kelompok pengguna yang paling sering mengakses sistem. Informasi ini berguna untuk melihat layanan mana yang paling banyak digunakan.")));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.filterInfoHtml(filter, "Sumber data: log login, dikelompokkan berdasarkan jenis pengguna.")));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.labelTrendPanelHtml(
				"Ranking Jenis Pengguna",
				"Merangkum total kunjungan untuk setiap jenis pengguna. Jenis pengguna dengan nilai tertinggi adalah kelompok yang paling aktif memakai sistem.",
				rows)));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.labelRadarPanelHtml(
				"Radar Distribusi Jenis Pengguna",
				"Panel radar ini membantu membaca dominasi kelompok pengguna. Jika satu kelompok terlalu dominan, pengelola dapat mengevaluasi sosialisasi atau akses untuk kelompok lain.",
				rows)));
		wrapper.appendChild(new Html(StatistikKunjunganDashboardUtil.labelTablePanelHtml(
				"Tabel Kunjungan per Tanggal dan Jenis Pengguna",
				"Tabel ini menampilkan detail kunjungan per tanggal dan jenis pengguna sebagai data pendukung ketika ingin memeriksa angka secara rinci.",
				rows)));
	}
}
