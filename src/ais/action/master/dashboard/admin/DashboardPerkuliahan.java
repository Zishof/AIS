package ais.action.master.dashboard.admin;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.Type;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardPerkuliahan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Center center;
	private Intbox mulai;
	private Intbox sampai;
	private Combobox searchprogram;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	protected Grid grid;

	public DashboardPerkuliahan() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardPerkuliahan(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);

		Center center = new Center();
		center.setBorder("none");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setStyle("min-height:11000px");
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Perkuliahan", "/img/home-icon.png");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Kehadiran Dosen", "/img/Document-Text-icon.png");
		tab2.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("Kehadiran Mahasiswa", "/img/Document-Text-icon.png");
		tab3.setParent(tabs);

		MyTabConfig tab4 = new MyTabConfig("Rekapitulasi pencapaian", "/img/Document-Text-icon.png");
		tab4.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("11000px");

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tabpanel2.setHeight("11000px");
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().isEmpty()) {

					DashboardKehadiranDosen kehadiranDosen = new DashboardKehadiranDosen();
					kehadiranDosen.setHeight("100%");
					kehadiranDosen.setWidth("100%");
					kehadiranDosen.setParent(tabpanel2);

				}

			}
		});

		final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
		tabpanel3.setParent(tabpanels);
		tabpanel3.setHeight("11000px");
		tab3.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel3.getChildren().isEmpty()) {

					DashboardKehadiranMahasiswa kehadiranMahasiswa = new DashboardKehadiranMahasiswa();
					kehadiranMahasiswa.setHeight("100%");
					kehadiranMahasiswa.setWidth("100%");
					kehadiranMahasiswa.setParent(tabpanel3);

				}

			}
		});

		final Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
		tabpanel4.setParent(tabpanels);
		tabpanel4.setHeight("11000px");
		tab4.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel4.getChildren().isEmpty()) {

					DashboardPencapaianPerkuliahan dashboardPencapaianPerkuliahan = new DashboardPencapaianPerkuliahan();
					dashboardPencapaianPerkuliahan.setHeight("100%");
					dashboardPencapaianPerkuliahan.setWidth("100%");
					dashboardPencapaianPerkuliahan.setParent(tabpanel4);

				}

			}
		});

		borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel1);

		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(borderlayout);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tahun Akademik"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		};

		mulai = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 3);
		mulai.setCols(4);
		hbox.appendChild(mulai);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));

		sampai = new Intbox(mulai.getValue() + 3);
		sampai.setCols(4);
		hbox.appendChild(sampai);

		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);

		searchprogram = new Combobox();
		row.appendChild(new MyLabelConfig("Program"));
		Common.initPrograms(searchprogram);
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.setReadonly(true);

		searchprogram.addEventListener("onChange", eventListener);

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		row.appendChild(new MyLabelConfig("Fakultas / Prodi"));
		hbox = new Hbox();
		hbox.setParent(row);
		hbox.appendChild(searchfakultas);
		hbox.appendChild(searchjurusan);
		searchfakultas.addEventListener("onChange", eventListener);
		searchjurusan.addEventListener("onChange", eventListener);
		searchfakultas.setCols(2);
		searchjurusan.setCols(2);

		DashboardPerkuliahan.this.center = new Center();
		ais.ui.util.ZkCompat.setFlex(DashboardPerkuliahan.this.center, true);
		DashboardPerkuliahan.this.center.setParent(borderlayout);

		row = new MyFormRow();
		row.setParent(rows);
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DashboardPerkuliahan.this.grid);
			}
		});

		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				reload();
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void reload() {
		Common.clear(center);

		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		final List<Jurusan> jurusans = HibernateUtil.currentSession().createCriteria(Jurusan.class)
				.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", jur.getId()))
				.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fak))
				.addOrder(Order.asc("fakultas"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		final Map<Long, List<Object[]>> datas = new HashMap<Long, List<Object[]>>();

		final int mul = mulai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 7
				: mulai.getValue();
		final int sam = sampai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
				: sampai.getValue();

		final List<String> tas = new ArrayList<String>();
		for (int tahun = mul; tahun <= sam; tahun++) {
			final String tahunAjaran = tahun + "/" + (tahun + 1);
			tas.add(tahunAjaran);
		}

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(center);

				grid = new Grid();
				grid.setSclass("dgrid");
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig("Fakultas");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Jurusan");
				column.setParent(columns);

				column.setWidth("15%");

				List<String> ta = new ArrayList<String>();

				for (int tahun = mul; tahun <= sam; tahun++) {
					ta.add(tahun + "/1");
					ta.add(tahun + "/2");
					ta.add(tahun + "/3");
				}

				for (String tahunAjaran : ta) {
					column.setParent(columns);
					column = new MyColumnConfig(tahunAjaran);
					column.setAlign("center");
					column.setParent(columns);
				}

				Rows rows = new Rows();
				rows.setParent(grid);

				SimpleCategoryModel categoryModelGanjil = new SimpleCategoryModel();
				categoryModelGanjil.clear();

				SimpleCategoryModel categoryModelGenap = new SimpleCategoryModel();
				categoryModelGenap.clear();

				SimpleCategoryModel categoryModelSp = new SimpleCategoryModel();
				categoryModelSp.clear();

				for (final Jurusan jurusan : jurusans) {
					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(jurusan.getFakultas().getNama()));
					row.appendChild(new MyLabelBoldAja(jurusan.getNama()));

					List<Object[]> data = datas.get(jurusan.getId());

					for (final String tahunAjaran : tas) {
						Number ganjil = 0;
						Number genap = 0;
						Number sp = 0;

						for (Object[] o : data) {
							Object tahunangkatan = o[3];
							if (tahunangkatan != null && tahunangkatan.toString().equalsIgnoreCase(tahunAjaran)) {
								ganjil = (Number) o[0];
								genap = (Number) o[1];
								sp = (Number) o[2];
								break;
							}
						}

						categoryModelGanjil.setValue(jurusan.getNama(), tahunAjaran, ganjil);

						A a = new A(Common.numberFormat.get().format(ganjil));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Common.displayWindow("/common/dashboard/perkuliahan.zul?tahunAjaran=" + tahunAjaran
										+ "&jenisSemester=1&statusSemesterPendek=null&jurusan=" + jurusan.getId()
										+ "&program=" + (program == null ? null : URLEncoder.encode(program, "UTF-8")),
										true, "95%", "95%");

							}
						});

						categoryModelGenap.setValue(jurusan.getNama(), tahunAjaran, genap);

						a = new A(Common.numberFormat.get().format(genap));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Common.displayWindow("/common/dashboard/perkuliahan.zul?tahunAjaran=" + tahunAjaran
										+ "&jenisSemester=2&statusSemesterPendek=null&jurusan=" + jurusan.getId()
										+ "&program=" + (program == null ? null : URLEncoder.encode(program, "UTF-8")),
										true, "95%", "95%");

							}
						});

						categoryModelSp.setValue(jurusan.getNama(), tahunAjaran, sp);

						a = new A(Common.numberFormat.get().format(sp));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Common.displayWindow("/common/dashboard/perkuliahan.zul?tahunAjaran=" + tahunAjaran
										+ "&jenisSemester=3&statusSemesterPendek=1&jurusan=" + jurusan.getId()
										+ "&program=" + (program == null ? null : URLEncoder.encode(program, "UTF-8")),
										true, "95%", "95%");

							}
						});
					}
				}

				MyFormRow row = new MyFormRow();
		row.setValign("top");
				row.setParent(rows);
				row.setSpans((((sam - mul) * 3) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModelGanjil, String.valueOf("Penawaran Perkuliahan Semester Ganjil"), "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
row = new MyFormRow();
				row.setParent(rows);
				row.setSpans((((sam - mul) * 3) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModelGenap, String.valueOf("Penawaran Perkuliahan Semester Genap"), "Jumlah penawaran kuliah semester genap ditampilkan agar beban layanan akademik mudah dipantau.", String.valueOf("bar")));




				row = new MyFormRow();
				row.setParent(rows);
				row.setSpans((((sam - mul) * 3) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModelSp, String.valueOf("Penawaran Perkuliahan Semester Pendek"), "Jumlah penawaran kuliah semester pendek ditampilkan agar kebutuhan kelas tambahan mudah dilihat.", String.valueOf("bar")));



			}
		});

		new Thread(new Runnable() {

			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();

				int i = 1;
				for (final Jurusan jurusan : jurusans) {
					label.setValue("Sedang memproses data di prodi " + jurusan.getNama() + " ("
							+ Common.numberFormat.get().format((i * 100.0) / jurusans.size()) + ")");
					i++;

					String sql = "sum(case when semester%2=1 and status_semesterpendek is null then 1 else 0 end) as ganjil,"
							+ "sum(case when semester%2=0 and status_semesterpendek is null then 1 else 0 end) as genap,"
							+ "sum(case when status_semesterpendek=" + Perkuliahan.SEMESTER_PENDEK
							+ " then 1 else 0 end) as sp";

					List<Object[]> data = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.isNull("perkuliahan_paralel"))
							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("program", program))

							.setProjection(Projections.projectionList()
									.add(Projections.sqlProjection(sql, new String[] { "ganjil", "genap", "sp" },
											new Type[] { org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE }))
									.add(Projections.groupProperty("tahunAjaran")))
							.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.in("tahunAjaran", tas)).list();

					datas.put(jurusan.getId(), data);
				}
				HibernateUtil.closeSession();

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	private int width = 750;
	private int height = 100;
}
