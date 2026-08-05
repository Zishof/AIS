package ais.action.master.dashboard.admin;

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
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
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

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.format1.akademik.LaporanRekapJumlahMahasiswa;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardStatistikStatusMahasiswaPerJurusan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;

	private org.zkoss.zul.Html mychart;
	private Div center;

	private Combobox searchsemester = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox angkatan = new Combobox();
	private Combobox angkatansd = new Combobox();

	public DashboardStatistikStatusMahasiswaPerJurusan() {
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

	public DashboardStatistikStatusMahasiswaPerJurusan(int width, int height) throws Exception {
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

	public DashboardStatistikStatusMahasiswaPerJurusan(String title, String border, boolean closable) {
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
		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center.
		 * Kartu Saringan di atas, kartu Isi (center) di bawah. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih fakultas dan program studi untuk menyaring data mahasiswa yang ditampilkan.",
				"Statistik Status Mahasiswa per Program Studi",
				"Sebaran mahasiswa menurut status di tiap program studi, lengkap dengan grafiknya.");
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

		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", new EventListener() {

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
					LaporanRekapJumlahMahasiswa laporan = new LaporanRekapJumlahMahasiswa();
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

	public class MyEventListener implements EventListener {

		private Long statusId;
		private Long jurusanId;

		public MyEventListener(Long statusId, Long jurusanId) {
			this.statusId = statusId;
			this.jurusanId = jurusanId;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(HistoryStatusMahasiswa.class, new DataCriteriaWithColumn() {

						@Override
						public Object[] initCriteria(boolean order) {

							try {
								PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(HistoryStatusMahasiswa.class)
										.add(statusId.equals(-1L) ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("statusMahasiswa.id", statusId))
										.createAlias("mahasiswa", "mahasiswa")
										.createAlias("mahasiswa.jurusan", "jurusan")
										.add(jurusanId == null || jurusanId.equals(-1L)
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("jurusan.id", jurusanId))
										.createAlias("jurusan.fakultas", "fakultas")
										.add(perguruanTinggi == null || perguruanTinggi.getId() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi))
										.add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"),
												Restrictions.eq("mahasiswa.aktif", true)))
										.add(Restrictions.between("mahasiswa.tahunangkatan",
												angkatan.getSelectedItem().getValue(),
												angkatansd.getSelectedItem().getValue()))
										.add(Restrictions.eq("tahunAkademik",
												tahunAkademik.getSelectedItem().getValue()))
										.add(Restrictions.sqlRestriction("this_.semester%2=" + (searchsemester
												.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "0")));

								String[] contents = new String[] { "mahasiswa.nim", "mahasiswa.nama",
										"mahasiswa.jurusan.nama", "statusMahasiswa.nama", "tahunAkademik",
										"ganjilGenap", "semester", "tanggalStatus", "keterangan" };

								return new Object[] { criteria, contents };

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

	public static List<Object[]> ambilData(PerguruanTinggi perguruanTinggi, String tahunAkademik, String smt,
			Integer angkatan, Integer angkatansd) {
		String sql = "select sum(case a1.status_mahasiswa when " + ConstantValues.AKTIF.getId()
				+ " then 1 else 0 end) as aktif,  " + "sum(case a1.status_mahasiswa when " + ConstantValues.CUTI.getId()
				+ " then 1 else 0 end) as cuti,  " + "sum(case a1.status_mahasiswa when " + ConstantValues.LULUS.getId()
				+ " then 1 else 0 end) as lulus,  " + "sum(case a1.status_mahasiswa when "
				+ ConstantValues.DROP_OUT.getId() + " then 1 else 0 end) as drop_out,  "
				+ "sum(case a1.status_mahasiswa when " + ConstantValues.TIDAK_AKTIF.getId()
				+ " then 1 else 0 end) as tidak_aktif, b.nama as jurusan, b.id as jurusan_id "
				+ " from mahasiswa a inner join history_status_mahasiswa a1 on (a.id = a1.mahasiswa)  "
				+ " inner join jurusan b on (a.jurusan = b.id  )  left join fakultas c on (c.id = b.fakultas)  "
				+ " where (a.aktif or a.aktif is null)  "
				+ (perguruanTinggi == null || perguruanTinggi.getId() == null ? ""
						: " and c.perguruan_tinggi=" + perguruanTinggi.getId())
				+ " and b.aktif and c.aktif and a1.tahunakademik='" + tahunAkademik + "' and a1.semester%2="
				+ (smt.equals(Perkuliahan.GANJIL) ? "1" : "0") + " " + " and a.tahunangkatan between " + angkatan
				+ " and " + angkatansd + " and (a1.tahap = 0 or a1.tahap is null) group by b.id order by b.nama";

		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);

		return jurusans;
	}

	@SuppressWarnings({ "deprecation" })
	private void initChart() {
		Common.clear(center);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Prodi");
		column.setParent(columns);
		column.setWidth("30%");

		MyColumnConfig columnAktif = new MyColumnConfig("Aktif");
		columnAktif.setParent(columns);
		MyColumnConfig columnCuti = new MyColumnConfig("Cuti");
		columnCuti.setParent(columns);
		MyColumnConfig columnLulus = new MyColumnConfig("Lulus");
		columnLulus.setParent(columns);
		MyColumnConfig columnDO = new MyColumnConfig("DO");
		columnDO.setParent(columns);
		MyColumnConfig columnTA = new MyColumnConfig("Tidak Aktif");
		columnTA.setParent(columns);
		column = new MyColumnConfig("Total");
		column.setParent(columns);

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		List<Object[]> jurusans = DashboardStatistikStatusMahasiswaPerJurusan.ambilData(perguruanTinggi,
				(String) tahunAkademik.getSelectedItem().getValue(),
				(String) searchsemester.getSelectedItem().getValue(), (Integer) angkatan.getSelectedItem().getValue(),
				(Integer) angkatansd.getSelectedItem().getValue());

		Rows rows = new Rows();
		rows.setParent(grid);

		Double aktifTotal = 0.0;
		Double cutiTotal = 0.0;
		Double lulusTotal = 0.0;
		Double drop_outTotal = 0.0;
		Double tidak_aktifTotal = 0.0;
		Double semuaTotal = 0.0;

		CategoryModel model = new SimpleCategoryModel();
		for (Object[] objects : jurusans) {

			Double aktif = ((Number) (objects[0] == null ? 0.0 : objects[0])).doubleValue();
			Double cuti = ((Number) (objects[1] == null ? 0.0 : objects[1])).doubleValue();
			Double lulus = ((Number) (objects[2] == null ? 0.0 : objects[2])).doubleValue();
			Double drop_out = ((Number) (objects[3] == null ? 0.0 : objects[3])).doubleValue();

			Double tidak_aktif = ((Number) (objects[4] == null ? 0.0 : objects[4])).doubleValue();

			String jurusan = (objects[5] == null ? "" : objects[5]).toString();

			Long jurusanId = ((Number) (objects[6] == null ? -1L : objects[6])).longValue();

			Double total = aktif + cuti + lulus + drop_out + tidak_aktif;

			aktifTotal += aktif;
			cutiTotal += cuti;
			lulusTotal += lulus;
			drop_outTotal += drop_out;
			tidak_aktifTotal += tidak_aktif;
			semuaTotal += total;

			MyFormRow row = new MyFormRow();
		row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(jurusan));

			A a = new A(Common.numberFormat.get().format(aktif));
			a.addEventListener("onClick", new MyEventListener(1L, jurusanId));

			row.appendChild(a);

			a = new A(Common.numberFormat.get().format(cuti));
			a.addEventListener("onClick", new MyEventListener(2L, jurusanId));
			row.appendChild(a);

			a = new A(Common.numberFormat.get().format(lulus));
			a.addEventListener("onClick", new MyEventListener(3L, jurusanId));
			row.appendChild(a);

			a = new A(Common.numberFormat.get().format(drop_out));
			a.addEventListener("onClick", new MyEventListener(4L, jurusanId));
			row.appendChild(a);

			a = new A(Common.numberFormat.get().format(tidak_aktif));
			a.addEventListener("onClick", new MyEventListener(5L, jurusanId));
			row.appendChild(a);

			a = new A(Common.numberFormat.get().format(total));
			a.addEventListener("onClick", new MyEventListener(-1L, jurusanId));
			row.appendChild(a);

			row.appendChild(a);

			model.setValue(jurusan, "Aktif", aktif);
			model.setValue(jurusan, "Cuti", cuti);
			model.setValue(jurusan, "Lulus", lulus);
			model.setValue(jurusan, "DO", drop_out);
			model.setValue(jurusan, "Tidak Aktif", tidak_aktif);

		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total")));

		A a = new A(Common.numberFormat.get().format(aktifTotal));
		a.addEventListener("onClick", new MyEventListener(1L, -1L));
		row.appendChild(a);

		a = new A(Common.numberFormat.get().format(cutiTotal));
		a.addEventListener("onClick", new MyEventListener(2L, -1L));
		row.appendChild(a);

		a = new A(Common.numberFormat.get().format(lulusTotal));
		a.addEventListener("onClick", new MyEventListener(3L, -1L));
		row.appendChild(a);

		a = new A(Common.numberFormat.get().format(drop_outTotal));
		a.addEventListener("onClick", new MyEventListener(4L, -1L));
		row.appendChild(a);

		a = new A(Common.numberFormat.get().format(tidak_aktifTotal));
		a.addEventListener("onClick", new MyEventListener(5L, -1L));
		row.appendChild(a);

		a = new A(Common.numberFormat.get().format(semuaTotal));
		a.addEventListener("onClick", new MyEventListener(-1L, -1L));
		row.appendChild(a);

		if (aktifTotal < 0.01) {
			columnAktif.setWidth("0px");
			for (Object[] objects : jurusans) {
				String jurusan = (objects[5] == null ? "" : objects[5]).toString();
				model.removeValue(jurusan, "Aktif");
			}
		}
		if (cutiTotal < 0.01) {
			columnCuti.setWidth("0px");
			for (Object[] objects : jurusans) {
				String jurusan = (objects[5] == null ? "" : objects[5]).toString();
				model.removeValue(jurusan, "Cuti");
			}
		}
		if (lulusTotal < 0.01) {
			columnLulus.setWidth("0px");
			for (Object[] objects : jurusans) {
				String jurusan = (objects[5] == null ? "" : objects[5]).toString();
				model.removeValue(jurusan, "Lulus");
			}
		}
		if (drop_outTotal < 0.01) {
			columnDO.setWidth("0px");
			for (Object[] objects : jurusans) {
				String jurusan = (objects[5] == null ? "" : objects[5]).toString();
				model.removeValue(jurusan, "DO");
			}
		}
		if (tidak_aktifTotal < 0.01) {
			columnTA.setWidth("0px");
			for (Object[] objects : jurusans) {
				String jurusan = (objects[5] == null ? "" : objects[5]).toString();
				model.removeValue(jurusan, "Tidak Aktif");
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "7");

		mychart = null;
        mychart = DashboardModernHtmlUtil.createAnyChart(model, "Dasbor Statistik Status Mahasiswa Per Jurusan", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);

		setStyle("min-height:" + (330 + (40 * jurusans.size())) + "px");

	}
}
