package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimplePieModel;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardStatistikPenilaian extends MyWindow {

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

	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();

	public DashboardStatistikPenilaian() throws Exception {
		super();
		initFakultas();
		init();
		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				initChart();
			}
		});

	}

	private boolean tampilRinci = false;

	public DashboardStatistikPenilaian(int width, int height) throws Exception {
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

	public DashboardStatistikPenilaian(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	private void initFakultas() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Statistik Penilaian");

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih fakultas dan program studi untuk menyaring data yang ditampilkan.",
				"Statistik Penilaian",
				"Sebaran nilai/penilaian per program studi, beserta grafiknya.");
		Component saringanHost = hostPortal[0];
		center = (Div) hostPortal[1];

		Grid grid = new Grid();
		grid.setSclass("dgrid");
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

		row.setParent(rows);
		row.appendChild(searchDosen);
		searchDosen.setWidth("90%");
		searchDosen.setWidth("90%");
		searchDosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
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
						comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					for (int i : Common.ganjil) {
						comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				}
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

			MyButtonConfig myButtonConfig = new MyButtonConfig("Lihat Rinci", "/img/12123-eyes-icon.png");
			myButtonConfig.setParent(row);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					DashboardNilaiMahasiswa laporan = new DashboardNilaiMahasiswa();
					laporan.setHeight("99%");
					laporan.setWidth("99%");
					laporan.setTitle("Penilaian");
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

		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());
		String semester = (String) (this.semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? Perkuliahan.GANJIL
						: this.semesterAbsensi.getSelectedItem().getValue());

		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null
				|| searchsemester.getSelectedItem().getValue() == null ? -1
						: searchsemester.getSelectedItem().getValue());

		String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");

		if (tahunAkademik == null) {
			return;
		}

		simplePieModel = new SimplePieModel();
		simplePieModel.clear();

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		String sql = "select sum(case when aa.total_nilai < 0.01 then 1 else 0 end) as belum_dinilai,  "
				+ "sum(case when aa.total_nilai >= 0.1 then 1 else 0 end) as sudah_dinilai  "
				+ "from detailperkuliahan aa inner join perkuliahan a on (aa.perkuliahan=a.id) "
				+ "left join jurusan b on (a.jurusan = b.id  )  " + "left join fakultas c on (c.id = b.fakultas)  "
				+ " where 1=1  "
				+ (perguruanTinggi == null || perguruanTinggi.getId() == null ? ""
						: " and c.perguruan_tinggi=" + perguruanTinggi.getId())
				+ " and b.aktif and c.aktif  "
				+ (semesterKe == null || semesterKe.equals(-1) ? "" : " and a.semester = " + semesterKe)
				+ (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
				+ (fakultas == null ? "" : " and b.fakultas = " + fakultas.getId()) + " and a.tahun_ajaran = '"
				+ tahunAkademik + "' and (a.merupakan_paralel is null or a.merupakan_paralel = false) and a.semester "
				+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))
				+ (program == null ? "" : " and a.program = '" + program + "' ")

				+ (dosen == null ? ""
						: " and (" + "a.dosen1 = " + dosen.getId() + " or  " + "a.dosen2 = " + dosen.getId() + " or  "
								+ "a.dosen3 = " + dosen.getId() + " or  " + "a.dosen4 = " + dosen.getId() + " or  "
								+ "a.dosen5 = " + dosen.getId() + " or  " + "a.dosen6 = " + dosen.getId() + " or  "
								+ "a.dosen7 = " + dosen.getId() + " or  " + "a.dosen8 = " + dosen.getId() + " or  "
								+ "a.dosen9 = " + dosen.getId() + " or  " + "a.dosen10 = " + dosen.getId() + ")");

		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);

		Object[] objects = jurusans.get(0);

		Double belumDinilai = ((Number) (objects[0] == null ? 0.0 : objects[0])).doubleValue();
		Double sudahDinilai = ((Number) (objects[1] == null ? 0.0 : objects[1])).doubleValue();

		Double total = belumDinilai + sudahDinilai;

		simplePieModel.setValue(
				"Belum Dinilai (" + Common.numberFormat.get().format(belumDinilai) + "/"
						+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : belumDinilai * 100 / total) + "%)",
				belumDinilai);

		simplePieModel.setValue(
				"Sudah Dinilai (" + Common.numberFormat.get().format(sudahDinilai) + "/"
						+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : sudahDinilai * 100 / total) + "%)",
				sudahDinilai);
        mychart = DashboardModernHtmlUtil.createAnyChart(simplePieModel, "Dasbor Statistik Penilaian", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);

	}
}
