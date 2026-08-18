package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

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
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimplePieModel;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardStatistikStatusMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;

	private org.zkoss.zul.Html mychart;
	private Div center;
	private SimplePieModel simplePieModel;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox angkatan = new Combobox();
	private Combobox angkatansd = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox tahunAkademik = new Combobox();

	public DashboardStatistikStatusMahasiswa() {
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

	public DashboardStatistikStatusMahasiswa(int width, int height) throws Exception {
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

	public DashboardStatistikStatusMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	@SuppressWarnings("deprecation")
	private void init() {
		DashboardGridExportHelper.pasang(this, "Statistik Status Mahasiswa");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center.
		 * Kartu Saringan di atas, kartu Isi (center) di bawah. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data mahasiswa yang ditampilkan.",
				"Statistik Status Mahasiswa",
				"Sebaran mahasiswa menurut status (aktif, cuti, lulus, keluar), lengkap dengan grafiknya.");
		Component saringanHost = hostPortal[0];
		center = (Div) hostPortal[1];

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setStyle("border:0px;background: transparent;");
		grid.setParent(saringanHost);
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

		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		angkatan = Common.generateTahunAngkatan(angkatan, calendar.get(Calendar.YEAR) - 10);
		angkatan.setReadonly(true);
		hbox.appendChild(angkatan);
		angkatan.setCols(2);
		angkatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		angkatansd = Common.generateTahunAngkatan(angkatansd, calendar.get(Calendar.YEAR));
		angkatansd.setReadonly(true);
		hbox.appendChild(angkatansd);
		angkatansd.setCols(2);
		angkatansd.addEventListener("onChange", new EventListener() {

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

		searchsemester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchsemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchsemester.appendChild(comboitem);
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");
		Common.selectComboItem(searchsemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");
		searchsemester.setReadonly(true);
		searchsemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		if (tampilRinci) {

			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "3");
			row.setAlign("center");

			MyButtonConfig myButtonConfig = new MyButtonConfig("Lihat Rinci", "/img/12123-eyes-icon.png");
			myButtonConfig.setParent(row);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					DashboardMahasiswa laporan = new DashboardMahasiswa();
					laporan.setHeight("95%");
					laporan.setWidth("90%");
					laporan.setTitle("Rekap Status Mahasiswa");
					laporan.setClosable(true);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
					laporan.onModal();
				}
			});
		}

	}

	private int width = 750;
	private int height = 100;

	private void initChart() {
		Common.clear(center);
		mychart = null;

		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());
		String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		simplePieModel = new SimplePieModel();
		simplePieModel.clear();

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		String sql = "select  sum(case a1.status_mahasiswa when 1 then 1 else 0 end) as aktif,  "
				+ "sum(case a1.status_mahasiswa when 2 then 1 else 0 end) as cuti,  "
				+ "sum(case a1.status_mahasiswa when 3 then 1 else 0 end) as lulus,  "
				+ "sum(case a1.status_mahasiswa when 4 then 1 else 0 end) as drop_out,  "
				+ "sum(case a1.status_mahasiswa when 5 then 1 else 0 end) as tidak_aktif from mahasiswa a "
				+ " inner join history_status_mahasiswa a1 on (a.id = a1.mahasiswa)  "
				+ "left join jurusan b on (a.jurusan = b.id  )  left join fakultas c on (c.id = b.fakultas)  "
				+ " where (a.aktif or a.aktif is null)  "
				+ (perguruanTinggi == null || perguruanTinggi.getId() == null ? ""
						: " and c.perguruan_tinggi=" + perguruanTinggi.getId())
				+ " and b.aktif and c.aktif  and a1.tahunakademik='" + tahunAkademik.getSelectedItem().getValue()
				+ "' and a1.semester%2="
				+ (searchsemester.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "0") + " "
				+ " and a.tahunangkatan between " + angkatan.getSelectedItem().getValue() + " and "
				+ angkatansd.getSelectedItem().getValue()
				+ (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
				+ (fakultas == null ? "" : " and b.fakultas = " + fakultas.getId())
				+ (program == null ? "" : " and a.program = '" + program + "'");

		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);

		Object[] objects = jurusans.get(0);
		Double aktif = ((Number) (objects[0] == null ? 0.0 : objects[0])).doubleValue();
		Double cuti = ((Number) (objects[1] == null ? 0.0 : objects[1])).doubleValue();
		Double lulus = ((Number) (objects[2] == null ? 0.0 : objects[2])).doubleValue();
		Double drop_out = ((Number) (objects[3] == null ? 0.0 : objects[3])).doubleValue();

		Double tidak_aktif = ((Number) (objects[4] == null ? 0.0 : objects[4])).doubleValue();

		Double total = aktif + cuti + lulus + drop_out + tidak_aktif;

		simplePieModel.setValue("Aktif (" + Common.numberFormat.get().format(aktif) + "/"
				+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : aktif * 100 / total) + "%)", aktif);
		simplePieModel.setValue("Cuti (" + Common.numberFormat.get().format(cuti) + "/"
				+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : cuti * 100 / total) + "%)", cuti);
		simplePieModel.setValue("Lulus (" + Common.numberFormat.get().format(lulus) + "/"
				+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : lulus * 100 / total) + "%)", lulus);
		simplePieModel.setValue("Drop Out (" + Common.numberFormat.get().format(drop_out) + "/"
				+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : drop_out * 100 / total) + "%)", drop_out);

		simplePieModel.setValue(
				"Tidak Aktif (" + Common.numberFormat.get().format(tidak_aktif) + "/"
						+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : tidak_aktif * 100 / total) + "%)",
				tidak_aktif);
        mychart = DashboardModernHtmlUtil.createAnyChart(simplePieModel, "Dasbor Statistik Status Mahasiswa", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);

	}
}
