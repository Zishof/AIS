package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.CategoryModel;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.PortalUiHelper;
import org.zkoss.zk.ui.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
public class DashboardStatistikJumlahMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;

	private org.zkoss.zul.Html mychart;

	private Center center;
	// private SimplePieModel simplePieModel;
	private CategoryModel categoryModel;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox angkatan = new Combobox();
	private Combobox status = new Combobox();
	private Combobox searchprogram = new Combobox();

	public DashboardStatistikJumlahMahasiswa() {
		super();
		initFakultas();
		init();
		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				initChart();
			}
		});

	}

	public DashboardStatistikJumlahMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	private void initFakultas() {

		Common.insertCombo(status = new Combobox(), new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	private void init() {
		DashboardGridExportHelper.pasang(this, "Statistik Jumlah Mahasiswa");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setStyle("border:0px;background: transparent;");
		borderlayout.setParent(this);
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		North north = new North();
		north.setParent(borderlayout);
		north.setStyle("border:0px;background: transparent;");
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setStyle("border:0px;background: transparent;");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		angkatan = Common.generateTahunAngkatan(angkatan, calendar.get(Calendar.YEAR) - 1);
		row.appendChild(angkatan);
		angkatan.setWidth("90%");
		angkatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});
		/*
		 * row = new MyFormRow(); row.setSclass("ais-form-row"); row.setParent(rows);
		 * row.appendChild(new ais.ui.util.MyLabelConfig( "Jenis Kelamin")); kelamin =
		 * new Combobox(); org.zkoss.zul.Comboitem comboitem = new
		 * org.zkoss.zul.Comboitem(); comboitem.setValue("Laki-laki");
		 * comboitem.setLabel("Laki-Laki"); kelamin.appendChild(comboitem); comboitem =
		 * new MyComboitemConfig(); comboitem.setValue("Perempuan");
		 * comboitem.setLabel("Perempuan"); kelamin.appendChild(comboitem);
		 * row.appendChild(kelamin); kelamin.setWidth("90%");
		 * kelamin.addEventListener("onChange", new EventListener() {
		 * 
		 * @Override public void onEvent(Event arg0) throws Exception { initChart(); }
		 * });
		 */

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(status);
		status.setWidth("90%");
		status.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setStyle("border:0px;");

	}

	@SuppressWarnings("unchecked")
	private void initChart() {
		Common.clear(center);
		mychart = null;



		//

		Integer angkatan = (Integer) (this.angkatan.getSelectedItem() == null ? null
				: this.angkatan.getSelectedItem().getValue());
		// String kelamin = (String) (this.kelamin.getSelectedItem() == null ?
		// null
		// : this.kelamin.getSelectedItem().getValue());
		String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());
		StatusMahasiswa statusAwalMahasiswa = (StatusMahasiswa) (status.getSelectedItem() == null
				|| status.getSelectedItem().getValue() == null ? null : status.getSelectedItem().getValue());

		if (angkatan == null) {
			return;
		}

		categoryModel = new SimpleCategoryModel();
		categoryModel.clear();

		Session session = HibernateUtil.currentSession();

		double totalLaki = 0;
		double totalPerempuan = 0;

		List<Fakultas> fakultas2 = ConstantValues
				.simpleList(session.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), Fakultas.class);
		for (Fakultas f : fakultas2) {

			String sql = "select " + "sum(case a.kelamin when 'Laki-laki' then 1 else 0 end) as lakilaki,"
					+ "sum(case a.kelamin when 'Perempuan' then 1 else 0 end) as perempuan " + "from mahasiswa a "
					+ "left join jurusan j on a.jurusan  = j.id " + "left join fakultas f on j.fakultas = f.id "
					+ "where f.id =" + f.getId() + " and a.tahunangkatan = '" + angkatan + "'"
					+ (program == null ? "" : " and a.program = '" + program + "'")
					+ (statusAwalMahasiswa == null ? "" : " and a.status = '" + statusAwalMahasiswa.getId() + "'");

			List<Object[]> jurusans = Common.ambilSql(sql);

			Object[] objects = jurusans.get(0);
			Integer lakilaki = ((Number) (objects[0] == null ? 0.0 : objects[0])).intValue();
			Integer perempuan = ((Number) (objects[1] == null ? 0.0 : objects[1])).intValue();

			categoryModel.setValue("Laki-Laki", f.getNama(), lakilaki);
			categoryModel.setValue("Perempuan", f.getNama(), perempuan);

			totalLaki += lakilaki;
			totalPerempuan += perempuan;
		}

		double total = totalLaki + totalPerempuan;

		/* Wadah portal responsif: kartu bertema + chip deskripsi sederhana.
		 * Kolom menumpuk otomatis di HP lewat CSS portal (ais-ce-portallayout). */
		Component host = PortalUiHelper.panelInParent(center,
				"Statistik Jumlah Mahasiswa",
				"Gambaran cepat banyaknya mahasiswa pada angkatan dan saringan yang dipilih, lengkap perbandingan laki-laki dan perempuan.");

		/* Ringkasan angka kunci (kartu otomatis turun baris di layar sempit). */
		List<DashboardUiKit.Stat> stats = new ArrayList<DashboardUiKit.Stat>();
		stats.add(new DashboardUiKit.Stat("Total Mahasiswa", DashboardModernHtmlUtil.number(total),
				"Semua mahasiswa pada pilihan saat ini", DashboardUiKit.PRIMARY));
		stats.add(new DashboardUiKit.Stat("Laki-laki",
				DashboardModernHtmlUtil.number(totalLaki) + " (" + persen(totalLaki, total) + "%)",
				"Banyaknya mahasiswa laki-laki", DashboardUiKit.ACCENT));
		stats.add(new DashboardUiKit.Stat("Perempuan",
				DashboardModernHtmlUtil.number(totalPerempuan) + " (" + persen(totalPerempuan, total) + "%)",
				"Banyaknya mahasiswa perempuan", DashboardUiKit.GOOD));

		StringBuilder ringkas = new StringBuilder();
		ringkas.append(DashboardUiKit.cards(stats));

		/* Donat komposisi laki-laki vs perempuan. */
		LinkedHashMap<String, Double> komposisi = new LinkedHashMap<String, Double>();
		komposisi.put("Laki-laki", totalLaki);
		komposisi.put("Perempuan", totalPerempuan);
		ringkas.append(DashboardUiKit.donut("Komposisi Laki-laki & Perempuan",
				"Besar bagian lingkaran menunjukkan porsi tiap jenis kelamin dari seluruh mahasiswa.",
				komposisi, false, "Belum ada data untuk ditampilkan."));
		DashboardUiKit.html(ringkas.toString()).setParent(host);

		/* Rincian per fakultas (batang HTML/CSS — bukan JFreeChart). */
		mychart = DashboardModernHtmlUtil.createAnyChart(categoryModel, "Rincian per Fakultas",
				"Panjang batang menunjukkan jumlah mahasiswa laki-laki dan perempuan di setiap fakultas.", "bar");
		mychart.setParent(host);
	}

	/** Persentase bulat aman (0 bila pembagi nol). */
	private static int persen(double bagian, double total) {
		if (total <= 0) {
			return 0;
		}
		int p = (int) Math.round(bagian * 100.0 / total);
		return p < 0 ? 0 : (p > 100 ? 100 : p);
	}
}
