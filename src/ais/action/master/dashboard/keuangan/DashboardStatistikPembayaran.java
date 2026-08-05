package ais.action.master.dashboard.keuangan;

import java.util.Calendar;
import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimplePieModel;

import ais.action.report.helper.keuangan.LaporanRekapHostToHostWindow;
import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Fakultas;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardStatistikPembayaran extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;

	private org.zkoss.zul.Html mychart;
	private Div center;
	private SimplePieModel simplePieModel;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox angkatanMhsMulai = new Combobox();private Combobox angkatanMhs = new Combobox();
	private Combobox jenisPembayaran = new Combobox();

	public DashboardStatistikPembayaran() throws Exception {
		super();
		initFakultas();
		init();
		initChart();

	}

	private boolean tampilRinci = false;

	public DashboardStatistikPembayaran(int width, int height) throws Exception {
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

	public DashboardStatistikPembayaran(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data pembayaran yang ditampilkan.",
				"Statistik Pembayaran",
				"Sebaran pembayaran mahasiswa per periode dan jenis, beserta grafiknya.");
		Component saringanHost = hostPortal[0];
		center = (Div) hostPortal[1];

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(saringanHost);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row.setParent(rows);
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row.setParent(rows);
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);
		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		jenisPembayaran = Common.createComboJenisPembayaranDanSemua(jenisPembayaran);
		row.setParent(rows);
		Common.selectComboItem(jenisPembayaran, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
		row.appendChild(jenisPembayaran);
		jenisPembayaran.setWidth("90%");
		jenisPembayaran.setReadonly(true);
		jenisPembayaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jenisPembayaran.getSelectedItem() == null || jenisPembayaran.getSelectedItem().getValue() == null)
					return;
				if (jenisPembayaran.getValue().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)) {
					searchfakultas.setDisabled(true);
					searchfakultas.setSelectedItem(null);
					searchjurusan.setDisabled(true);
					searchjurusan.setSelectedItem(null);
					semesterAbsensi.setDisabled(true);
					semesterAbsensi.setSelectedItem(null);
					searchsemester.setDisabled(true);
					searchsemester.setSelectedItem(null);
				} else if (jenisPembayaran.getValue().equals(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
					searchfakultas.setDisabled(false);
					searchjurusan.setDisabled(false);
					semesterAbsensi.setDisabled(true);
					semesterAbsensi.setSelectedItem(null);
					searchsemester.setDisabled(true);
					searchsemester.setSelectedItem(null);
				} else {
					searchfakultas.setDisabled(false);
					searchjurusan.setDisabled(false);
					semesterAbsensi.setDisabled(false);
					searchsemester.setDisabled(false);
				}

				initChart();

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(1);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.setReadonly(true);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row.setParent(rows);
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");

		row.setParent(rows);
		row.appendChild(angkatanMhs);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Angkatan");
		comboitem.setValue(null);
		angkatanMhs.appendChild(comboitem);
		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 10; i <= ais.ui.util.WaktuUtil
				.getCalendar().get(Calendar.YEAR) + 10; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			angkatanMhs.appendChild(comboitem);
		}
		angkatanMhs.setSelectedIndex(0);
		angkatanMhs.setWidth("90%");
		angkatanMhs.setReadonly(true);
		angkatanMhs.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});
		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);

				if (semesterAbsensi.getSelectedItem() == null) {
					return;
				}
				Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel("Semester");
				comboitem.setValue(null);
				searchsemester.appendChild(comboitem);
				if (genap) {
					for (int i : Common.genap) {
						if (i == 0)
							continue;
						comboitem = new MyComboitemConfig();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					for (int i : Common.ganjil) {
						comboitem = new MyComboitemConfig();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				}

				searchsemester.setSelectedIndex(0);
				searchsemester.setReadonly(true);
			}
		};
		semesterAbsensi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(arg0);
				initChart();
			}
		});

		searchsemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		eventListener.onEvent(null);

		if (tampilRinci) {

			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "4");
			
			Hbox hbox = new Hbox();
			hbox.setParent(row);

			// Laporan modern (Cetak + Ekspor Excel + progress + grafik HTML/CSS) via mesin reuse.
			ais.action.master.helper.DashboardReportKit.pasangTombol(hbox, this, buatSumberLaporanPembayaran());

			MyButtonConfig myButtonConfig = new MyButtonConfig("Lihat Rinci", "/img/12123-eyes-icon.png");
			myButtonConfig.setParent(hbox);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					MyWindow laporan = new MyWindow();
					laporan.setHeight("99%");
					laporan.setWidth("99%");
					laporan.setTitle("Rincian Pembayaran");
					laporan.setClosable(true);
					laporan.setBorder("none");

					Borderlayout borderlayout = new Borderlayout();
					laporan.appendChild(borderlayout);

					Center center = new Center();
					ais.ui.util.ZkCompat.setFlex(center, true);
					center.setParent(borderlayout);

					center.appendChild(new Iframe("/pages/master/kegiatan.zul"));
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
					laporan.onModal();
				}
			});

			myButtonConfig = new MyButtonConfig("Lihat Rekap", "/img/12123-eyes-icon.png");
			myButtonConfig.setParent(hbox);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					LaporanRekapHostToHostWindow laporan = new LaporanRekapHostToHostWindow();
					laporan.setHeight("99%");
					laporan.setWidth("99%");
					laporan.setTitle("Rekap Pembayaran");
					laporan.setClosable(true);
					laporan.setBorder("none");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
					laporan.onModal();
				}
			});
		}

	}

	private int width = 750;
	private int height = 450;

	private void initChart() {
		Common.clear(center);
		mychart = null;

		final String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());
		final String semester = (String) (semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? null
						: semesterAbsensi.getSelectedItem().getValue());

		final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null
				|| searchsemester.getSelectedItem().getValue() == null ? null
						: searchsemester.getSelectedItem().getValue());
		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Integer angkatan = (Integer) (angkatanMhs.getSelectedItem() == null ? null
				: angkatanMhs.getSelectedItem().getValue());
		JenisKegiatan jenisPembayaran = (JenisKegiatan) (this.jenisPembayaran.getSelectedItem() == null ? null
				: this.jenisPembayaran.getSelectedItem().getValue());

		if (tahunAkademik == null || jenisPembayaran == null) {
			return;
		}

		simplePieModel = new SimplePieModel();
		simplePieModel.clear();

			String sql = bangunSqlRingkasan(tahunAkademik, semester, fakultas, jurusan, semesterKe, program, angkatan, jenisPembayaran);

		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);

		if (jurusans == null || jurusans.isEmpty()) {
			return;
		}
		Object[] objects = jurusans.get(0);
		Double belumMembayar = ((Number) (objects[0] == null ? 0.0 : objects[0])).doubleValue();
		Double sudahLunas = ((Number) (objects[1] == null ? 0.0 : objects[1])).doubleValue();
		Double belumLunas = ((Number) (objects[2] == null ? 0.0 : objects[2])).doubleValue();

		Double total = belumMembayar + sudahLunas + belumLunas;

		simplePieModel.setValue(
				"Belum Membayar (" + Common.numberFormat.get().format(belumMembayar) + "/"
						+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : belumMembayar * 100 / total) + "%)",
				belumMembayar);
		simplePieModel.setValue("Sudah Lunas (" + Common.numberFormat.get().format(sudahLunas) + "/"
				+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : sudahLunas * 100 / total) + "%)", sudahLunas);
		simplePieModel.setValue("Belum Lunas (" + Common.numberFormat.get().format(belumLunas) + "/"
				+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : belumLunas * 100 / total) + "%)", belumLunas);
        mychart = DashboardModernHtmlUtil.createAnyChart(simplePieModel, "Dasbor Statistik Pembayaran", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);

	}

	/**
	 * Menyusun query SQL ringkasan pembayaran (belum membayar / sudah lunas / belum lunas) sesuai
	 * jenis pembayaran dan filter aktif. Dipisah dari {@code initChart()} agar dapat DIPAKAI ULANG
	 * oleh laporan cetak/Excel tanpa menyalin logika. Perbandingan semester memakai
	 * {@code Perkuliahan.GENAP.equals(semester)} (aman bila {@code semester} null).
	 */
	private String bangunSqlRingkasan(final String tahunAkademik, final String semester, Fakultas fakultas, Jurusan jurusan, Integer semesterKe, String program, Integer angkatan, JenisKegiatan jenisPembayaran) {
		String sql;
		if (jenisPembayaran.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
			sql = "select\nsum(case when b.id is null then 1 else 0 end) as belum_membayar,\n"
					+ "sum(case when b.amount_terhutang=0 then 1 else 0 end) as sudah_lunas,\n"
					+ "sum(case when b.amount_terhutang>0.1 then 1 else 0 end) as belum_lunas\nfrom biodata_calon_mahasiswa a\n"
					+ "left join kegiatan b on (b.mahasiswa=a.id and b.jenis_kegiatan = " + jenisPembayaran.getId()
					+ " and b.tahun_akademik='" + tahunAkademik + "'  and "
					+ ((Perkuliahan.GENAP.equals(semester) ? " b.semster % 2 = 0 " : " b.semster % 2 = 1 "))
					+ (semesterKe == null || semesterKe.equals(-1) ? "" : " and b.semster = " + semesterKe) + ")\n"
					+ " inner join jurusan c on (a.prodi_lulus=c.id)\n where 1=1 "
					+ (program == null ? "" : " and a.program = '" + program + "' ")
					+ (angkatan == null ? "" : " and a.tahun = " + angkatan + " ")
					+ (jurusan == null ? "" : " and a.prodi_lulus = " + jurusan.getId())
					+ (fakultas == null ? "" : " and c.fakultas = " + fakultas.getId());
		} else if (jenisPembayaran.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)) {
			sql = "select\nsum(case when b.id is null then 1 else 0 end) as belum_membayar,\n"
					+ "sum(case when b.amount_terhutang=0 then 1 else 0 end) as sudah_lunas,\n"
					+ "sum(case when b.amount_terhutang>0.1 then 1 else 0 end) as belum_lunas\nfrom biodata_calon_mahasiswa a\n"
					+ "left join kegiatan b on (b.mahasiswa=a.id and b.jenis_kegiatan = " + jenisPembayaran.getId()
					+ " and b.tahun_akademik='" + tahunAkademik + "'  and "
					+ ((Perkuliahan.GENAP.equals(semester) ? " b.semster % 2 = 0 " : " b.semster % 2 = 1 "))
					+ (semesterKe == null || semesterKe.equals(-1) ? "" : " and b.semster = " + semesterKe) + ")\n"
					+ " left join jurusan c1 on (a.prodi_1=c1.id)\n   left join jurusan c2 on (a.prodi_2=c2.id)\n "
					+ " left join jurusan c3 on (a.prodi3=c3.id)\n   left join jurusan c4 on (a.prodi4=c4.id)\n "
					+ " left join jurusan c5 on (a.prodi5=c5.id)\n   where 1=1 "
					+ (program == null ? "" : " and a.program = '" + program + "' ")
					+ (angkatan == null ? "" : " and a.tahun = " + angkatan + " ")
					+ (jurusan == null ? ""
							: (" and (a.prodi_1 = " + jurusan.getId() + " or a.prodi_2 = " + jurusan.getId()
									+ " or a.prodi3 = " + jurusan.getId() + " or a.prodi4 = " + jurusan.getId()
									+ "  or a.prodi5 = " + jurusan.getId() + ")"))
					+ (fakultas == null ? ""
							: (" and (c1.fakultas = " + fakultas.getId() + " or c2.fakultas = " + fakultas.getId()
									+ " or c3.fakultas = " + fakultas.getId() + " or c4.fakultas = " + fakultas.getId()
									+ " or c5.fakultas = " + fakultas.getId() + " )"));
		} else {
			sql = "select\nsum(case when b.id is null then 1 else 0 end) as belum_membayar,\n"
					+ "sum(case when b.amount_terhutang=0 then 1 else 0 end) as sudah_lunas,\n"
					+ "sum(case when b.amount_terhutang>0.1 then 1 else 0 end) as belum_lunas\nfrom mahasiswa a\n"
					+ "left join kegiatan b on (b.mahasiswa=a.id and b.jenis_kegiatan = " + jenisPembayaran.getId()
					+ " and b.tahun_akademik='" + tahunAkademik + "'  and "
					+ ((Perkuliahan.GENAP.equals(semester) ? " b.semster % 2 = 0 " : " b.semster % 2 = 1 "))
					+ (semesterKe == null || semesterKe.equals(-1) ? "" : " and b.semster = " + semesterKe) + ")\n"
					+ " left join jurusan c on (a.jurusan=c.id)\n where 1=1 "
					+ (program == null ? "" : " and a.program = '" + program + "' ")
					+ (angkatan == null ? "" : " and a.tahunangkatan = " + angkatan + " ")
					+ (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
					+ (fakultas == null ? "" : " and c.fakultas = " + fakultas.getId());
		}

		return sql;
	}

	/**
	 * Menghitung ringkasan pembayaran {@code [belumMembayar, sudahLunas, belumLunas]} sesuai filter
	 * aktif — dipakai laporan cetak/Excel. Membaca kombobox filter yang sama dengan {@code initChart()},
	 * membangun SQL lewat {@link #bangunSqlRingkasan}, lalu menjalankannya via
	 * {@code Common.ambilSql} (memakai {@code currentSession()} yang ditutup otomatis kerangka kerja —
	 * tidak ditutup manual). Null/empty hasil dijaga agar tidak melempar.
	 */
	private double[] ringkasanPembayaran() {
		final String thn = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());
		final String smt = (String) (semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? null
						: semesterAbsensi.getSelectedItem().getValue());
		final Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		final Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());
		final Integer smtKe = (Integer) (searchsemester.getSelectedItem() == null
				|| searchsemester.getSelectedItem().getValue() == null ? null
						: searchsemester.getSelectedItem().getValue());
		final String prog = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());
		final Integer angk = (Integer) (angkatanMhs.getSelectedItem() == null ? null
				: angkatanMhs.getSelectedItem().getValue());
		final JenisKegiatan jenis = (JenisKegiatan) (this.jenisPembayaran.getSelectedItem() == null ? null
				: this.jenisPembayaran.getSelectedItem().getValue());

		if (thn == null || jenis == null) {
			return new double[] { 0, 0, 0 };
		}
		try {
			List<Object[]> data = Common.ambilSql(bangunSqlRingkasan(thn, smt, fak, jur, smtKe, prog, angk, jenis));
			if (data == null || data.isEmpty() || data.get(0) == null) {
				return new double[] { 0, 0, 0 };
			}
			Object[] o = data.get(0);
			double belum = ((Number) (o[0] == null ? 0.0 : o[0])).doubleValue();
			double lunas = ((Number) (o[1] == null ? 0.0 : o[1])).doubleValue();
			double belumLunas = ((Number) (o[2] == null ? 0.0 : o[2])).doubleValue();
			return new double[] { belum, lunas, belumLunas };
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DashboardStatistikPembayaran.java:545");
			}
			return new double[] { 0, 0, 0 };
		}
	}

	private static String bagianDariTotal(long part, long total) {
		if (total <= 0) {
			return "0% dari total";
		}
		return String.format(java.util.Locale.US, "%.1f%% dari total", part * 100.0 / total);
	}

	/**
	 * Deskripsi laporan "Statistik Pembayaran Mahasiswa" untuk mesin
	 * {@link ais.action.master.helper.DashboardReportKit}: kartu KPI ringkas dan diagram donut
	 * perbandingan Sudah Lunas / Belum Lunas / Belum Membayar. Data mengikuti filter yang aktif saat
	 * tombol diklik. Grafik dibuat HTML/CSS (tanpa JFreeChart), responsif untuk layar kecil.
	 */
	private ais.action.master.helper.DashboardReportKit.SumberLaporan buatSumberLaporanPembayaran() {
		return new ais.action.master.helper.DashboardReportKit.SumberLaporan() {
			@Override
			public String judul() {
				return "Statistik Pembayaran Mahasiswa";
			}

			@Override
			public String subjudul() {
				return jenisPembayaran.getSelectedItem() == null ? ""
						: String.valueOf(jenisPembayaran.getSelectedItem().getLabel());
			}

			@Override
			public String deskripsi() {
				return "Melihat berapa banyak mahasiswa yang sudah lunas, belum lunas, dan belum membayar sama sekali.";
			}

			@Override
			public java.util.List<ais.action.master.helper.DashboardReportKit.Bagian> bagian() {
				java.util.List<ais.action.master.helper.DashboardReportKit.Bagian> b =
						new java.util.ArrayList<ais.action.master.helper.DashboardReportKit.Bagian>();

				b.add(ais.action.master.helper.DashboardReportKit.kpi("Ringkasan Pembayaran",
						"Jumlah mahasiswa berdasarkan status pembayarannya.",
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							@Override
							public java.util.List<Object[]> ambil() {
								double[] r = ringkasanPembayaran();
								long belum = Math.round(r[0]);
								long lunas = Math.round(r[1]);
								long belumLunas = Math.round(r[2]);
								long total = belum + lunas + belumLunas;
								java.util.List<Object[]> out = new java.util.ArrayList<Object[]>();
								out.add(new Object[] { "Total Mahasiswa", ais.action.master.helper.DashboardReportKit.fmt(total), "sesuai filter" });
								out.add(new Object[] { "Sudah Lunas", ais.action.master.helper.DashboardReportKit.fmt(lunas), bagianDariTotal(lunas, total) });
								out.add(new Object[] { "Belum Lunas", ais.action.master.helper.DashboardReportKit.fmt(belumLunas), bagianDariTotal(belumLunas, total) });
								out.add(new Object[] { "Belum Membayar", ais.action.master.helper.DashboardReportKit.fmt(belum), bagianDariTotal(belum, total) });
								return out;
							}
						}));

				b.add(ais.action.master.helper.DashboardReportKit.donut("Perbandingan Status Pembayaran",
						"Bagian terbesar dan terkecil dari status pembayaran mudah terlihat sekilas.",
						new String[] { "Status", "Jumlah" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							@Override
							public java.util.List<Object[]> ambil() {
								double[] r = ringkasanPembayaran();
								java.util.List<Object[]> out = new java.util.ArrayList<Object[]>();
								out.add(new Object[] { "Sudah Lunas", Long.valueOf(Math.round(r[1])) });
								out.add(new Object[] { "Belum Lunas", Long.valueOf(Math.round(r[2])) });
								out.add(new Object[] { "Belum Membayar", Long.valueOf(Math.round(r[0])) });
								return out;
							}
						}));
				return b;
			}
		};
	}
}
