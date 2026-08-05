package ais.action.report.format1.employ;
import ais.ui.util.DashboardGridExportHelper;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JabatanFungsional;

public class StatistikJumlahPegawaiBaseJabatanFungsional extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;


	private Center center;
	// private SimplePieModel simplePieModel;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox angkatan = new Combobox();
	private Combobox status = new Combobox();
	private Combobox searchprogram = new Combobox();

	

	public StatistikJumlahPegawaiBaseJabatanFungsional() {
		super();
		initPegawai();
		init();
		initChart();

	}

	public StatistikJumlahPegawaiBaseJabatanFungsional(String title,
			String border, boolean closable) {
		super(title, border, closable);
		initPegawai();
		init();
		initChart();
	}

	private void initPegawai() {

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" },
				Fakultas.class);
		Common.insertCombo(status = new Combobox(), new String[] { "nama",
				"kodeEpsbed" }, StatusMahasiswa.class);

		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				searchjurusan.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchjurusan, new String[] { "nama",
						"kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)), Restrictions
						.eq("fakultas", searchfakultas.getSelectedItem()
								.getValue()));
			}

		}

		searchfakultas.addEventListener("onChange",
				new SearchFakultasEventListener());

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			Common.clear(searchjurusan);
			Common.insertCombo(searchjurusan, new String[] { "nama",
					"kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			searchfakultas.setDisabled(true);
		} else {
			searchfakultas.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
			searchjurusan.setDisabled(true);
		} else {
			searchjurusan.setDisabled(false);
		}

	}

	private void init() {
		DashboardGridExportHelper.pasang(this, "Statistik Jumlah Pegawai Base Jabatan Fungsional");
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

		MyGrid grid = new MyGrid();grid.setWidth("100%");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		angkatan = Common.generateTahunAngkatan(angkatan,
				calendar.get(Calendar.YEAR) - 1);
		row.appendChild(angkatan);
		angkatan.setWidth("90%");
		angkatan.addEventListener("onChange", new EventListener() {

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

		// Data untuk grafik batang HTML/CSS (HtmlChartHelper), menggantikan bar 3D JFreeChart.
		java.util.List<String> namaJabatanList = new java.util.ArrayList<String>();
		java.util.List<Double> jumlahJabatanList = new java.util.ArrayList<Double>();

		Session session = HibernateUtil.currentSession();

		List<JabatanFungsional> jabatanFungsionals = session.createCriteria(
				JabatanFungsional.class).list();
		for (JabatanFungsional f : jabatanFungsionals) {

			String sql = "select "
					+ "sum(case a.jabatan_fungsional when b.id then 1 else 0 end) as jumlah "
					+ "from pegawai a "
					+ "left join employ.jabatan_fungsional b on (a.jabatan_fungsional=b.id) "
					+ "where b.id =" + f.getId();

			System.out.println(sql);
			List<Object[]> pegawais = session.createSQLQuery(sql).list();
			if (pegawais.size() == 0) {
				return;
			}
			// System.out.println(f.getNama() + jurusans.get(0));

			// Object[] objects = jurusans.get(0);
			//
			Integer jumlah = ((Number) (pegawais.get(0) == null ? 0.0
					: pegawais.get(0))).intValue();

			namaJabatanList.add(f.getNama());
			jumlahJabatanList.add((double) jumlah);
		}

		double[] nilaiJabatan = new double[jumlahJabatanList.size()];
		for (int i = 0; i < nilaiJabatan.length; i++) {
			nilaiJabatan[i] = jumlahJabatanList.get(i).doubleValue();
		}
		// Grafik batang HTML/CSS modern + penjelasan bahasa sederhana untuk pengguna awam.
		String htmlBar = ais.ui.util.HtmlChartHelper.barHorizontal("Pegawai per Jabatan Fungsional",
				"Menampilkan jumlah pegawai pada setiap jabatan fungsional.",
				namaJabatanList.toArray(new String[namaJabatanList.size()]), nilaiJabatan, "#1877f2");
		center.appendChild(new ais.ui.util.MyHtml(htmlBar));

	}
}
