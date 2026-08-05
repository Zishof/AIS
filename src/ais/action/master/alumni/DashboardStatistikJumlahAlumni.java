package ais.action.master.alumni;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.CategoryModel;
import org.zkoss.zul.Center;
import ais.ui.util.MyChart;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.action.master.MahasiswaAction;
import ais.action.master.dashboard.admin.DashboardLulusan;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyWindow;

public class DashboardStatistikJumlahAlumni extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;

	private MyChart mychart;

	private Center center;
	// private SimplePieModel simplePieModel;
	private CategoryModel categoryModel;

	private Combobox angkatan = new Combobox();
	private Combobox angkatansd = new Combobox();

	private Combobox kelulusan = new Combobox();
	private Combobox kelulusansd = new Combobox();

	public DashboardStatistikJumlahAlumni() {
		super();
		initFakultas();
		init();
		initChart();

	}

	private boolean tampilRinci = false;

	public DashboardStatistikJumlahAlumni(int width, int height) throws Exception {
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

	public DashboardStatistikJumlahAlumni(String title, String border, boolean closable) {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	private void initFakultas() {

	}

	@SuppressWarnings("deprecation")
	private void init() {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setStyle("border:0px;background: transparent;");
		borderlayout.setParent(this);
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		North north = new North();
		north.setStyle("border:0px;background: transparent;");
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setStyle("border:0px;background: transparent;");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		row.appendChild(new MyLabelConfig("Angkatan"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		angkatan = Common.generateTahunAngkatan(angkatan, calendar.get(Calendar.YEAR) - 20);
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

		row.appendChild(new MyLabelConfig("Kelulusan"));
		hbox = new Hbox();
		row.appendChild(hbox);

		kelulusan = Common.generateTahunAngkatan(kelulusan, calendar.get(Calendar.YEAR) - 10);
		kelulusan.setReadonly(true);
		hbox.appendChild(kelulusan);
		kelulusan.setCols(2);
		kelulusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		kelulusansd = Common.generateTahunAngkatan(kelulusansd, calendar.get(Calendar.YEAR));
		kelulusansd.setReadonly(true);
		hbox.appendChild(kelulusansd);
		kelulusansd.setCols(2);
		kelulusansd.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		if (tampilRinci) {

			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "4");

			MyButtonConfig myButtonConfig = new MyButtonConfig("Lihat Rinci", "/img/12123-eyes-icon.png");
			myButtonConfig.setParent(row);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					DashboardLulusan laporan = new DashboardLulusan();
					laporan.setHeight("99%");
					laporan.setWidth("99%");
					laporan.setTitle("Alumni");
					laporan.setClosable(true);
					laporan.setBorder("none");

					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
					laporan.onModal();
				}
			});
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setStyle("border:0px;");

	}

	private int width = 750;
	private int height = 100;

	public class MyEventListener implements EventListener {

		private String kelamin;
		private Long jurusanId;

		public MyEventListener(String kelamin, Long jurusanId) {
			this.kelamin = kelamin;
			this.jurusanId = jurusanId;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(Mahasiswa.class, new DataCriteriaWithColumn() {

						@Override
						public Object[] initCriteria(boolean order) {

							try {

								PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(Mahasiswa.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.add(kelamin == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("kelamin", kelamin))

										.add(Restrictions.eq("statusKeluar.id", 1L)).createAlias("jurusan", "jurusan")
										.add(jurusanId == null || jurusanId.equals(-1L)
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("jurusan.id", jurusanId))
										.createAlias("jurusan.fakultas", "fakultas")
										.add(perguruanTinggi == null || perguruanTinggi.getId() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))

										.add(Restrictions.between("tahunangkatan",
												angkatan.getSelectedItem().getValue(),
												angkatansd.getSelectedItem().getValue()))

										.add(Restrictions.between("tahunLulus", kelulusan.getSelectedItem().getValue(),
												kelulusansd.getSelectedItem().getValue()));

								return new Object[] { criteria, MahasiswaAction.contents };

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
							return null;
						}

					}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
							new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "" })
					.getAttribute("eventListener");

			eventListener.onEvent(null);

		}
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void initChart() {
		Common.clear(center);
		mychart = null;

		categoryModel = new SimpleCategoryModel();
		categoryModel.clear();

		Session session = HibernateUtil.currentSession();

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Prodi");
		column.setParent(columns);
		column.setWidth("30%");

		MyColumnConfig columnAktif = new MyColumnConfig("Laki-laki");
		columnAktif.setParent(columns);
		MyColumnConfig columnCuti = new MyColumnConfig("Perempuan");
		columnCuti.setParent(columns);
		column = new MyColumnConfig("Total");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Double aktifTotal = 0.0;
		Double cutiTotal = 0.0;
		Double semuaTotal = 0.0;
		// Data untuk grafik HTML/CSS (stacked bar) — dikumpulkan saat menelusuri tiap prodi.
		java.util.List<String> prodiNamaList = new java.util.ArrayList<String>();
		java.util.List<int[]> prodiNilaiList = new java.util.ArrayList<int[]>();
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		List<Jurusan> fakultas2 = ConstantValues.simpleList(
				session.createCriteria(Jurusan.class).createAlias("fakultas", "fakultas")
						.add(perguruanTinggi == null || perguruanTinggi.getId() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				Jurusan.class);
		for (Jurusan f : fakultas2) {

			String sql = "select sum(case a.kelamin when 'Laki-laki' then 1 else 0 end) as lakilaki,"
					+ "sum(case a.kelamin when 'Perempuan' then 1 else 0 end) as perempuan from mahasiswa a "
					+ "where a.jurusan =" + f.getId()

					+ " and a.tahunangkatan between " + angkatan.getSelectedItem().getValue() + " and "
					+ angkatansd.getSelectedItem().getValue()

					+ " and a.tahunlulus between " + kelulusan.getSelectedItem().getValue() + " and "
					+ kelulusansd.getSelectedItem().getValue()

					+ " and a.status_keluar = 1";

			System.out.println(sql);
			List<Object[]> jurusans = Common.ambilSql(sql);

			Object[] objects = jurusans.get(0);
			//
			Integer lakilaki = ((Number) (objects[0] == null ? 0.0 : objects[0])).intValue();
			Integer perempuan = ((Number) (objects[1] == null ? 0.0 : objects[1])).intValue();

			if (lakilaki > 0.1)
				categoryModel.setValue(f.getNama(), "Laki-Laki", lakilaki);
			if (perempuan > 0.1)
				categoryModel.setValue(f.getNama(), "Perempuan", perempuan);

			prodiNamaList.add(f.getNama());
			prodiNilaiList.add(new int[] { lakilaki, perempuan });

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(f.getNama()));

			A a = new A(Common.numberFormat.get().format(lakilaki));
			a.addEventListener("onClick", new MyEventListener("Laki-laki", f.getId()));
			row.appendChild(a);

			a = new A(Common.numberFormat.get().format(perempuan));
			a.addEventListener("onClick", new MyEventListener("Perempuan", f.getId()));
			row.appendChild(a);

			a = new A(Common.numberFormat.get().format(lakilaki + perempuan));
			a.addEventListener("onClick", new MyEventListener(null, f.getId()));
			row.appendChild(a);

			aktifTotal += lakilaki;
			cutiTotal += perempuan;
			semuaTotal += (lakilaki + perempuan);
		}

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total")));

		A a = new A(Common.numberFormat.get().format(aktifTotal));
		a.addEventListener("onClick", new MyEventListener("Laki-laki", null));
		row.appendChild(a);

		a = new A(Common.numberFormat.get().format(cutiTotal));
		a.addEventListener("onClick", new MyEventListener("Perempuan", null));
		row.appendChild(a);

		a = new A(Common.numberFormat.get().format(semuaTotal));
		a.addEventListener("onClick", new MyEventListener(null, null));
		row.appendChild(a);

		// RINGKASAN (overview): donut komposisi keseluruhan Laki-laki vs Perempuan, memakai total
		// yang sudah dihitung. Pola "overview + detail": gambaran cepat dulu, lalu rincian per prodi
		// pada stacked bar di bawahnya. Grafik HTML/CSS (HtmlChartHelper), bukan JFreeChart.
		MyFormRow rowDonut = new MyFormRow();
		rowDonut.setValign("top");
		rowDonut.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowDonut, "4");
		String htmlDonutGender = ais.ui.util.HtmlChartHelper.donut(
				"Komposisi Alumni: Laki-laki vs Perempuan",
				"Menampilkan perbandingan jumlah alumni laki-laki dan perempuan secara keseluruhan.",
				new String[] { "Laki-laki", "Perempuan" }, new double[] { aktifTotal, cutiTotal },
				new String[] { "#1877f2", "#e4496b" }, "laki-laki");
		rowDonut.appendChild(new ais.ui.util.MyHtml(htmlDonutGender));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "4");
		// Grafik komposisi alumni per prodi memakai STACKED BAR HTML/CSS modern
		// (ais.ui.util.HtmlChartHelper) — menggantikan bar 3D JFreeChart. Ringan, responsif,
		// dilengkapi penjelasan bahasa sederhana untuk pengguna awam.
		String[] kategoriProdi = prodiNamaList.toArray(new String[prodiNamaList.size()]);
		double[][] nilaiProdi = new double[prodiNilaiList.size()][2];
		for (int i = 0; i < prodiNilaiList.size(); i++) {
			int[] v = prodiNilaiList.get(i);
			nilaiProdi[i][0] = v[0];
			nilaiProdi[i][1] = v[1];
		}
		String htmlGrafikAlumni = ais.ui.util.HtmlChartHelper.stackedBar(
				"Jumlah Alumni per Program Studi",
				"Menampilkan jumlah alumni pada setiap program studi beserta rincian jumlah laki-laki dan perempuan. Semakin panjang batang, semakin banyak jumlah alumninya.",
				kategoriProdi, new String[] { "Laki-laki", "Perempuan" }, nilaiProdi,
				new String[] { "#1877f2", "#e4496b" });
		row.appendChild(new ais.ui.util.MyHtml(htmlGrafikAlumni));

		setStyle("min-height:" + (330 + (40 * fakultas2.size())) + "px");

	}
}
