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
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Wisuda;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardStatistikPengajuanWisudaPerJurusan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;

	private org.zkoss.zul.Html mychart;
	private Div center;

	private Combobox angkatan = new Combobox();
	private Combobox angkatansd = new Combobox();

	public DashboardStatistikPengajuanWisudaPerJurusan() {
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

	private Combobox searchjenis;

	public DashboardStatistikPengajuanWisudaPerJurusan(int width, int height) throws Exception {
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

	public DashboardStatistikPengajuanWisudaPerJurusan(String title, String border, boolean closable) {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	private void initFakultas() {

	}

	public class MyEventListener implements EventListener {

		private Boolean persetujuanWisuda;
		private Long jurusanId;

		public MyEventListener(Boolean persetujuanWisuda, Long jurusanId) {
			this.persetujuanWisuda = persetujuanWisuda;
			this.jurusanId = jurusanId;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(PendaftaranWisuda.class, new DataCriteriaWithColumn() {

						@Override
						public Object[] initCriteria(boolean order) {

							try {
								Wisuda wisuda = (Wisuda) (searchjenis.getSelectedItem() == null ? null
										: searchjenis.getSelectedItem().getValue());
								PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(PendaftaranWisuda.class)
										.add(persetujuanWisuda == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("persetujuanWisuda", persetujuanWisuda))
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
										.add(wisuda == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("wisuda", wisuda));

								String[] contents = new String[] { "mahasiswa.nim", "mahasiswa.nama",
										"mahasiswa.jurusan.nama", "persetujuanWisuda", "wisuda.nama",
										"skripsi.tahunAkademik", "skripsi.semester", "skripsi.formatNilaiSkripsi.nama",
										"skripsi.judul", "skripsi.judulen", "skripsi.abstrack", "skripsi.keyword",
										"skripsi.pembimbing.nama", "skripsi.ketuaSidang.nama",
										"skripsi.pembimbing3.nama", "skripsi.penguji1.nama", "skripsi.penguji2.nama",
										"skripsi.penguji3.nama", "skripsi.penguji4.nama", "skripsi.totalNilai",
										"skripsi.nilaiHuruf", "skripsi.totalIP", "skripsi.awalBimbingan",
										"skripsi.akhirBimbingan", "skripsi.jadwalSidangTugasAkhir.nama",
										"skripsi.gelombangPendaftaranSidangTugasAkhir.nama" };

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

	@SuppressWarnings("deprecation")
	private void init() {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center.
		 * Kartu Saringan di atas, kartu Isi (center) di bawah. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih tahun akademik, semester, rentang angkatan, dan pembimbing untuk menyaring data yang ditampilkan.",
				"Statistik Pengajuan Wisuda per Program Studi",
				"Jumlah pengajuan wisuda yang sudah dan belum disetujui di tiap program studi, beserta grafiknya.");
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

		row.appendChild(searchjenis = new Combobox());
		Common.insertComboDanSemua(searchjenis, new String[] { "nama" }, "keterangan", Wisuda.class, "=Wisuda=");
		searchjenis.setWidth("90%");
		searchjenis.setReadonly(true);
		searchjenis.addEventListener("onChange", new EventListener() {

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
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.setParent(rows);

			MyButtonConfig myButtonConfig = new MyButtonConfig("Lihat Rinci", "/img/12123-eyes-icon.png");
			myButtonConfig.setParent(row);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					DashboardWisudaMahasiswa laporan = new DashboardWisudaMahasiswa();
					laporan.setHeight("99%");
					laporan.setWidth("99%");
					laporan.setTitle("Rekap Wisuda");
					laporan.setClosable(true);
					laporan.setBorder("none");

					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
					laporan.onModal();
				}
			});
		}

	}

	private int width = 750;
	private int height = 100;

	@SuppressWarnings({ "deprecation" })
	private void initChart() {
		Common.clear(center);

		Grid grid = new Grid();grid.setSclass("dgrid");
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Prodi");
		column.setParent(columns);
		column.setWidth("30%");

		MyColumnConfig columnAktif = new MyColumnConfig("Telah Disetujui");
		columnAktif.setParent(columns);
		MyColumnConfig columnCuti = new MyColumnConfig("Belum Disetujui");
		columnCuti.setParent(columns);
		column = new MyColumnConfig("Total");
		column.setParent(columns);

		Wisuda wisuda = (Wisuda) (searchjenis.getSelectedItem() == null ? null
				: searchjenis.getSelectedItem().getValue());

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		String sql = "select sum(case when a1.persetujuan_wisuda then 1 else 0 end) as telah_disetujui,  "
				+ " sum(case when a1.persetujuan_wisuda then 0 else 1 end) as belum_disetujui,   b.nama as jurusan, b.id as jurusan_id "
				+ " from mahasiswa a inner join pendaftaran_wisuda a1 on (a.id = a1.mahasiswa)  "
				+ " inner join jurusan b on (a.jurusan = b.id  )  left join fakultas c on (c.id = b.fakultas)  "
				+ " where (a.aktif or a.aktif is null)  "
				+ (perguruanTinggi == null || perguruanTinggi.getId() == null ? ""
						: " and c.perguruan_tinggi=" + perguruanTinggi.getId())
				+ (wisuda == null ? "" : " and a1.wisuda=" + wisuda.getId()) + " and a.tahunangkatan between "
				+ angkatan.getSelectedItem().getValue() + " and " + angkatansd.getSelectedItem().getValue()
				+ " group by b.id order by b.nama";

		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);

		Rows rows = new Rows();
		rows.setParent(grid);

		Double aktifTotal = 0.0;
		Double cutiTotal = 0.0;
		Double semuaTotal = 0.0;

		CategoryModel model = new SimpleCategoryModel();
		for (Object[] objects : jurusans) {

			Double Pengajuan = ((Number) (objects[0] == null ? 0.0 : objects[0])).doubleValue();
			Double Disetujui = ((Number) (objects[1] == null ? 0.0 : objects[1])).doubleValue();

			String jurusan = (objects[2] == null ? "" : objects[2]).toString();
			Long jurusanId = ((Number) (objects[3] == null ? -1L : objects[3])).longValue();

			Double total = Pengajuan + Disetujui;

			aktifTotal += Pengajuan;
			cutiTotal += Disetujui;
			semuaTotal += total;

			MyFormRow row = new MyFormRow();
		row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(jurusan));

			A a = new A(Common.numberFormat.get().format(Pengajuan));
			a.addEventListener("onClick", new MyEventListener(true, jurusanId));
			row.appendChild(a);

			a = new A(Common.numberFormat.get().format(Disetujui));
			a.addEventListener("onClick", new MyEventListener(false, jurusanId));
			row.appendChild(a);

			a = new A(Common.numberFormat.get().format(total));
			a.addEventListener("onClick", new MyEventListener(null, jurusanId));
			row.appendChild(a);

			model.setValue(jurusan, "Telah Disetujui", Pengajuan);
			model.setValue(jurusan, "Belum Disetujui", Disetujui);

		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total")));

		A a = new A(Common.numberFormat.get().format(aktifTotal));
		a.addEventListener("onClick", new MyEventListener(true, null));
		row.appendChild(a);

		a = new A(Common.numberFormat.get().format(cutiTotal));
		a.addEventListener("onClick", new MyEventListener(false, null));
		row.appendChild(a);

		a = new A(Common.numberFormat.get().format(semuaTotal));
		a.addEventListener("onClick", new MyEventListener(null, null));
		row.appendChild(a);

		if (aktifTotal < 0.01) {
			columnAktif.setWidth("0px");
			for (Object[] objects : jurusans) {
				String jurusan = (objects[2] == null ? "" : objects[2]).toString();
				model.removeValue(jurusan, "Telah Disetujui");
			}
		}
		if (cutiTotal < 0.01) {
			columnCuti.setWidth("0px");
			for (Object[] objects : jurusans) {
				String jurusan = (objects[2] == null ? "" : objects[2]).toString();
				model.removeValue(jurusan, "Belum Disetujui");
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "4");

		mychart = null;
        mychart = DashboardModernHtmlUtil.createAnyChart(model, "Dasbor Statistik Pengajuan Wisuda Per Jurusan", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);

		setStyle("min-height:" + (330 + (40 * jurusans.size())) + "px");

	}
}
