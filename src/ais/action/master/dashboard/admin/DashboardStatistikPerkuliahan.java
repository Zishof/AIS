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
/**
 * Komponen dashboard khusus untuk dashboard statistik perkuliahan. Kelas ini memilih variasi data
 * atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code org.zkoss.zul.Html mychart}, {@code Div
 * center}, {@code SimplePieModel simplePieModel}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Combobox tahunAkademik}, {@code Combobox semesterAbsensi}, {@code Combobox
 * searchsemester}; inisialisasi/lifecycle ({@code reinit()}, {@code initFakultas()}, {@code init()}, {@code
 * initChart()}); konfigurasi constructor: {@code tampilRinci}. Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardStatistikPerkuliahan extends MyWindow {

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
	// private Label angkatan = new Label();

	public DashboardStatistikPerkuliahan() throws Exception {
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

	public DashboardStatistikPerkuliahan(int width, int height) throws Exception {
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

	public DashboardStatistikPerkuliahan(String title, String border, boolean closable) throws Exception {
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
		DashboardGridExportHelper.pasang(this, "Statistik Perkuliahan");

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih fakultas dan program studi untuk menyaring data yang ditampilkan.",
				"Statistik Perkuliahan",
				"Sebaran data perkuliahan per program studi, beserta grafiknya.");
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
		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.setParent(rows);
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");

		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");
		searchsemester.setReadonly(true);

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

			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "3");
			row.setAlign("center");

			MyButtonConfig myButtonConfig = new MyButtonConfig("Lihat Rinci", "/img/12123-eyes-icon.png");
			myButtonConfig.setParent(row);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					DashboardPerkuliahan laporan = new DashboardPerkuliahan();
					laporan.setHeight("99%");
					laporan.setWidth("99%");
					laporan.setTitle("Rekap Perkuliahan");
					laporan.setClosable(true);
					laporan.setBorder("none");

					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
					laporan.onModal();
				}
			});
		}

	}

	private int width = 800;
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

		if (tahunAkademik == null) {
			return;
		}

		simplePieModel = new SimplePieModel();
		simplePieModel.clear();

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		String sql = "select " + "sum(case g.perkuliahan when g.perkuliahan then 0 else 1 end) belum_diambil, "
				+ "sum(case b.perkuliahan when b.perkuliahan then 1 else 0 end) jumlah_pengambil_krs_0_sd_10, "
				+ "sum(case c.perkuliahan when c.perkuliahan then 1 else 0 end) jumlah_pengambil_krs_11_sd_20, "
				+ "sum(case d.perkuliahan when d.perkuliahan then 1 else 0 end) jumlah_pengambil_krs_21_sd_30, "
				+ "sum(case e.perkuliahan when e.perkuliahan then 1 else 0 end) jumlah_pengambil_krs_31_sd_40, "
				+ "sum(case f.perkuliahan when f.perkuliahan then 1 else 0 end) jumlah_pengambil_krs_lebih_besar_40 "
				+ "from perkuliahan a "
				+ "left join (select max(perkuliahan) as perkuliahan from detailperkuliahan group by perkuliahan having count(id) between 1 and 10) b on (a.id = b.perkuliahan) "
				+ "left join (select max(perkuliahan) as perkuliahan from detailperkuliahan group by perkuliahan having count(id) between 11 and 20) c on (a.id = c.perkuliahan) "
				+ "left join (select max(perkuliahan) as perkuliahan from detailperkuliahan group by perkuliahan having count(id) between 21 and 30) d on (a.id = d.perkuliahan) "
				+ "left join (select max(perkuliahan) as perkuliahan from detailperkuliahan group by perkuliahan having count(id) between 31 and 40) e on (a.id = e.perkuliahan) "
				+ "left join (select max(perkuliahan) as perkuliahan from detailperkuliahan group by perkuliahan having count(id) > 40) f on (a.id = f.perkuliahan) "
				+ "left join (select max(perkuliahan) as perkuliahan from detailperkuliahan group by perkuliahan) g on (a.id = g.perkuliahan)  "
				+ "left join jurusan x on (a.jurusan = x.id  ) left join fakultas xx on (xx.id = x.fakultas)  "
				+ " where 1=1  "
				+ (perguruanTinggi == null || perguruanTinggi.getId() == null ? ""
						: " and xx.perguruan_tinggi=" + perguruanTinggi.getId())
				+ " and x.aktif and xx.aktif  "
				+ (semesterKe == null || semesterKe.equals(-1) ? "" : " and a.semester = " + semesterKe)
				+ (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
				+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId()) + " and tahun_ajaran = '"
				+ tahunAkademik + "' and (a.merupakan_paralel is null or a.merupakan_paralel = false) and semester "
				+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))
				+ (program == null ? "" : " and a.program = '" + program + "'");

		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);

		Object[] objects = jurusans.get(0);
		Double belumAmbilKrs = ((Number) (objects[0] == null ? 0.0 : objects[0])).doubleValue();
		Double diambil0sd10 = ((Number) (objects[1] == null ? 0.0 : objects[1])).doubleValue();
		Double diambil10sd20 = ((Number) (objects[2] == null ? 0.0 : objects[2])).doubleValue();
		Double diambil20sd30 = ((Number) (objects[3] == null ? 0.0 : objects[3])).doubleValue();
		Double diambil30sd40 = ((Number) (objects[4] == null ? 0.0 : objects[4])).doubleValue();
		Double diambillebihDari40 = ((Number) (objects[5] == null ? 0.0 : objects[5])).doubleValue();

		Double total = belumAmbilKrs + diambil0sd10 + diambil10sd20 + diambil20sd30 + diambil30sd40
				+ diambillebihDari40;

		simplePieModel.setValue(
				"0 Mhs (" + Common.numberFormat.get().format(belumAmbilKrs) + "/"
						+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : belumAmbilKrs * 100 / total) + "%)",
				belumAmbilKrs);
		simplePieModel.setValue(
				"1 s.d 10 Mhs (" + Common.numberFormat.get().format(diambil0sd10) + "/"
						+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : diambil0sd10 * 100 / total) + "%)",
				diambil0sd10);
		simplePieModel.setValue(
				"11 s.d 20 Mhs (" + Common.numberFormat.get().format(diambil10sd20) + "/"
						+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : diambil10sd20 * 100 / total) + "%)",
				diambil10sd20);
		simplePieModel.setValue(
				"21 s.d 30 Mhs (" + Common.numberFormat.get().format(diambil20sd30) + "/"
						+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : diambil20sd30 * 100 / total) + "%)",
				diambil20sd30);
		simplePieModel.setValue(
				"31 s.d 40 Mhs (" + Common.numberFormat.get().format(diambil30sd40) + "/"
						+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : diambil30sd40 * 100 / total) + "%)",
				diambil30sd40);
		simplePieModel.setValue(
				"> 40 Mhs (" + Common.numberFormat.get().format(diambillebihDari40) + "/"
						+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : diambillebihDari40 * 100 / total) + "%)",
				diambillebihDari40);
        mychart = DashboardModernHtmlUtil.createAnyChart(simplePieModel, "Dasbor Statistik Perkuliahan", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);

	}
}
