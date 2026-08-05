package ais.action.master.dashboard.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.MahasiswaAction;
import ais.action.master.PendaftaranCutiMahasiswaAction;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyWindow;

public class DashboardStatistikMahamahasiswaLulus extends MyWindow {

	private static final long serialVersionUID = 1L;
	private static final String[] DATA_TAMBAHAN = new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
			"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
			"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
			"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
			"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
			"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
			"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
			"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
			"", "" };

	private Div center;
	private int width = 750;
	private int height = 100;

	public DashboardStatistikMahamahasiswaLulus() {
		super();
	}

	public DashboardStatistikMahamahasiswaLulus(int width, int height) throws Exception {
		super();
		reinit(width, height);
	}

	public void reinit(int width, int height) throws Exception {
		this.width = width;
		this.height = height;
	}

	public DashboardStatistikMahamahasiswaLulus(String title, String border, boolean closable) {
		super(title, border, closable);
	}

	public void init() {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");

		/* Portal responsif (kartu tunggal, natural-height) menggantikan Borderlayout+Center. */
		center = (Div) ais.ui.util.DasborResponsifHelper.isiTunggal(this,
				"Statistik Mahasiswa Lulus",
				"Jumlah mahasiswa yang lulus per program studi dan periode, beserta grafiknya.");
		initChart(center, true);
	}

	public class MyEventListener implements EventListener {
		private Integer tahunMasuk;
		private Long jurusanId;
		private Integer mintahun = null;

		public MyEventListener(Integer tahunMasuk, Long jurusanId) {
			this.tahunMasuk = tahunMasuk;
			this.jurusanId = jurusanId;
		}

		public MyEventListener(Integer tahunMasuk, Integer mintahun, Long jurusanId) {
			this.tahunMasuk = tahunMasuk;
			this.jurusanId = jurusanId;
			this.mintahun = mintahun;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {
			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(Mahasiswa.class, new DataCriteriaWithColumn() {
						@Override
						public Object[] initCriteria(boolean order) {
							try {
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(Mahasiswa.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.addOrder(Order.desc("tahunLulus"));
								if (jurusanId != null && !jurusanId.equals(-1L)) {
									criteria.add(Restrictions.eq("jurusan.id", jurusanId));
								}
								if (mintahun != null) {
									criteria.add(Restrictions.le("tahunLulus", mintahun));
								} else if (tahunMasuk != null && !tahunMasuk.equals(-1)) {
									criteria.add(Restrictions.eq("tahunLulus", tahunMasuk));
								}
								return new Object[] { criteria, MahasiswaAction.contents };
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
							return null;
						}
					}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN", DATA_TAMBAHAN)
					.getAttribute("eventListener");

			if (eventListener != null) {
				eventListener.onEvent(null);
			}
		}
	}

	public class MyEventListenerCuti implements EventListener {
		private Long jurusanId;
		private String tahunAkademik;
		private String ganjilGenap;

		public MyEventListenerCuti(String tahunAkademik, String ganjilGenap, Long jurusanId) {
			this.tahunAkademik = tahunAkademik;
			this.ganjilGenap = ganjilGenap;
			this.jurusanId = jurusanId;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {
			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(PendaftaranCutiMahasiswa.class, new DataCriteriaWithColumn() {
						@Override
						public Object[] initCriteria(boolean order) {
							try {
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(PendaftaranCutiMahasiswa.class)
										.add(Restrictions.eq("persetujuan", true)).createAlias("mahasiswa", "mahasiswa")
										.add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"), Restrictions.eq("mahasiswa.aktif", true)))
										.addOrder(Order.desc("tahunAkademik")).addOrder(Order.desc("ganjilGenap"))
										.addOrder(Order.asc("mahasiswa.nim"))
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
								if (tahunAkademik != null) {
									criteria.add(Restrictions.eq("tahunAkademik", tahunAkademik));
								}
								if (ganjilGenap != null) {
									criteria.add(Restrictions.eq("ganjilGenap", ganjilGenap));
								}
								if (jurusanId != null && !jurusanId.equals(-1L)) {
									criteria.add(Restrictions.eq("mahasiswa.jurusan.id", jurusanId));
								}
								return new Object[] { criteria, PendaftaranCutiMahasiswaAction.contents };
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
							return null;
						}
					}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN", DATA_TAMBAHAN)
					.getAttribute("eventListener");

			if (eventListener != null) {
				eventListener.onEvent(null);
			}
		}
	}

	@SuppressWarnings({ "deprecation" })
	public void initChart(Component center, boolean tampilkanChart) {
		try {
			Common.clear(center);
			Grid grid = createGrid(center, "Jumlah Lulus");
			Rows rows = new Rows();
			rows.setParent(grid);

			Tbmuser tbmuser = Common.getCurrentUser();
			Fakultas fakultas = tbmuser == null ? null : tbmuser.ambilFakultas();
			Jurusan jurusana = tbmuser == null ? null : tbmuser.ambilJurusan();
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

			StringBuilder sql = new StringBuilder();
			sql.append("SELECT b.id AS jurusan_id, b.nama AS jurusan, a.tahunlulus, COUNT(*) AS jumlah_siswa ");
			sql.append("FROM mahasiswa a ");
			sql.append("INNER JOIN jurusan b ON (a.jurusan = b.id) ");
			sql.append("INNER JOIN fakultas c ON (b.fakultas = c.id) ");
			sql.append("WHERE b.aktif = true AND (a.aktif = true OR a.aktif IS NULL) ");
			sql.append("AND a.tahunlulus > 1900 AND a.tahunlulus < 2100 AND a.status_keluar = 1 ");

			if (jurusana != null && jurusana.getId() != null) {
				sql.append("AND a.jurusan = ").append(jurusana.getId()).append(" ");
			}
			if (fakultas != null && fakultas.getId() != null) {
				sql.append("AND b.fakultas = ").append(fakultas.getId()).append(" ");
			}
			if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				sql.append("AND c.perguruan_tinggi = ").append(perguruanTinggi.getId()).append(" ");
			}
			sql.append("GROUP BY b.id, b.nama, a.tahunlulus ");
			sql.append("ORDER BY b.id ASC, a.tahunlulus DESC");

			List<Object[]> jurusans = Common.ambilSql(sql.toString());
			renderTahunDashboard(rows, jurusans, tampilkanChart, "Grafik Mahasiswa Lulus per Tahun",
					"Batang memperlihatkan jumlah lulusan pada setiap prodi dan tahun lulus.");
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings({ "deprecation" })
	public void initChartCuti(Component center) {
		try {
			Common.clear(center);
			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setParent(center);
			grid.setWidth("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
			new MyColumnConfig("Prodi").setParent(columns);
			MyColumnConfig columnTa = new MyColumnConfig("TA");
			columnTa.setParent(columns);
			columnTa.setWidth("20%");
			MyColumnConfig columnSmt = new MyColumnConfig("Smt");
			columnSmt.setParent(columns);
			columnSmt.setWidth("20%");
			MyColumnConfig columnJumlah = new MyColumnConfig("Jumlah Cuti");
			columnJumlah.setParent(columns);
			columnJumlah.setWidth("20%");

			Tbmuser tbmuser = Common.getCurrentUser();
			Fakultas fakultas = tbmuser == null ? null : tbmuser.ambilFakultas();
			Jurusan jurusanUser = tbmuser == null ? null : tbmuser.ambilJurusan();
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

			StringBuilder sql = new StringBuilder();
			sql.append("SELECT b.id AS jurusan_id, b.nama AS jurusan, aa.tahun_akademik, aa.ganjil_genap, COUNT(*) AS jumlah_siswa ");
			sql.append("FROM pendaftaran_cuti_mahasiswa aa ");
			sql.append("INNER JOIN mahasiswa a ON (aa.mahasiswa = a.id) ");
			sql.append("INNER JOIN jurusan b ON (a.jurusan = b.id) ");
			sql.append("INNER JOIN fakultas c ON (b.fakultas = c.id) ");
			sql.append("WHERE b.aktif = true AND (a.aktif = true OR a.aktif IS NULL) AND aa.persetujuan = true ");
			if (jurusanUser != null && jurusanUser.getId() != null) {
				sql.append("AND a.jurusan = ").append(jurusanUser.getId()).append(" ");
			}
			if (fakultas != null && fakultas.getId() != null) {
				sql.append("AND b.fakultas = ").append(fakultas.getId()).append(" ");
			}
			if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				sql.append("AND c.perguruan_tinggi = ").append(perguruanTinggi.getId()).append(" ");
			}
			sql.append("GROUP BY b.id, b.nama, aa.tahun_akademik, aa.ganjil_genap ");
			sql.append("ORDER BY b.id ASC, aa.tahun_akademik DESC, aa.ganjil_genap ASC");

			List<Object[]> jurusans = Common.ambilSql(sql.toString());
			Rows rows = new Rows();
			rows.setParent(grid);
			Integer total = 0;
			List<DashboardAkademikHtmlCssHelper.BarItem> chartItems = new ArrayList<DashboardAkademikHtmlCssHelper.BarItem>();

			if (jurusans != null) {
				for (Object[] objects : jurusans) {
					Long jurusanId = objects[0] != null ? ((Number) objects[0]).longValue() : 0L;
					String jurusan = objects[1] != null ? objects[1].toString() : "";
					String tahunAkademik = objects[2] != null ? objects[2].toString() : "";
					String ganjilGenap = objects[3] != null ? objects[3].toString() : "";
					Integer jumlahSiswa = objects[4] != null ? ((Number) objects[4]).intValue() : 0;
					total += jumlahSiswa;

					Row row = new Row();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label(jurusan));
					row.appendChild(new ais.ui.util.MyLabelKecil(tahunAkademik));
					row.appendChild(new ais.ui.util.MyLabelKecil(ganjilGenap));
					A a = new A(Common.numberFormat.get().format(jumlahSiswa));
					a.addEventListener("onClick", new MyEventListenerCuti(tahunAkademik, ganjilGenap, jurusanId));
					row.appendChild(a);
					chartItems.add(DashboardAkademikHtmlCssHelper.item(jurusan, tahunAkademik + " " + ganjilGenap,
							jumlahSiswa));
				}
			}

			Row rowTotal = new Row();
			rowTotal.setValign("top");
			rowTotal.setParent(rows);
			rowTotal.appendChild(new Label(ais.common.Common.getBahasaConfig("Total")));
			rowTotal.appendChild(new Label(""));
			rowTotal.appendChild(new Label(""));
			A aTotal = new A(Common.numberFormat.get().format(total));
			aTotal.addEventListener("onClick", new MyEventListenerCuti(null, null, -1L));
			rowTotal.appendChild(aTotal);

			Row rowChart = new Row();
			rowChart.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowChart, "4");
			rowChart.appendChild(new Html(DashboardAkademikHtmlCssHelper.verticalBarChart("Grafik Mahasiswa Cuti",
					"Batang memperlihatkan jumlah mahasiswa cuti berdasarkan prodi, tahun akademik, dan semester.", chartItems)));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private Grid createGrid(Component center, String jumlahCaption) {
		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(center);
		grid.setWidth("100%");
		Columns columns = new Columns();
		columns.setParent(grid);
		new MyColumnConfig("Prodi").setParent(columns);
		MyColumnConfig columnTahun = new MyColumnConfig("Tahun");
		columnTahun.setParent(columns);
		columnTahun.setWidth("20%");
		MyColumnConfig columnJumlah = new MyColumnConfig(jumlahCaption);
		columnJumlah.setParent(columns);
		columnJumlah.setWidth("20%");
		return grid;
	}

	private void renderTahunDashboard(Rows rows, List<Object[]> jurusans, boolean tampilkanChart, String chartTitle,
			String chartDescription) {
		Integer total = 0;
		Integer totalTahunLama = 0;
		Map<Long, Integer> maxtahuns = new HashMap<Long, Integer>();
		List<DashboardAkademikHtmlCssHelper.BarItem> chartItems = new ArrayList<DashboardAkademikHtmlCssHelper.BarItem>();

		if (jurusans != null) {
			for (Object[] objects : jurusans) {
				Long jurusanId = objects[0] != null ? ((Number) objects[0]).longValue() : 0L;
				Integer tahun = objects[2] != null ? ((Number) objects[2]).intValue() : -1;
				Integer maxtahun = maxtahuns.containsKey(jurusanId) ? maxtahuns.get(jurusanId) : -1;
				if (maxtahun < tahun) {
					maxtahuns.put(jurusanId, tahun);
				}
			}
		}

		Long previousJurusanId = -1L;
		String previousJurusanName = "";
		int mintahun = 0;

		if (jurusans != null) {
			for (Object[] objects : jurusans) {
				Long jurusanId = objects[0] != null ? ((Number) objects[0]).longValue() : 0L;
				String jurusanName = objects[1] != null ? objects[1].toString() : "";
				Integer tahun = objects[2] != null ? ((Number) objects[2]).intValue() : -1;
				Integer jumlahSiswa = objects[3] != null ? ((Number) objects[3]).intValue() : 0;
				total += jumlahSiswa;

				if (!jurusanId.equals(previousJurusanId)) {
					if (totalTahunLama > 0 && !previousJurusanId.equals(-1L)) {
						cetakBarisTahunLama(rows, previousJurusanName, mintahun, totalTahunLama, previousJurusanId,
								chartItems);
						totalTahunLama = 0;
					}
					previousJurusanId = jurusanId;
					previousJurusanName = jurusanName;
				}

				Integer maxTahun = maxtahuns.get(jurusanId);
				mintahun = (maxTahun == null ? 0 : maxTahun.intValue()) - (tampilkanChart ? 50 : 5);

				if (mintahun < tahun.intValue()) {
					Row row = new Row();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label(jurusanName));
					row.appendChild(new Label(tahun.toString()));
					A a = new A(Common.numberFormat.get().format(jumlahSiswa));
					a.addEventListener("onClick", new MyEventListener(tahun, jurusanId));
					row.appendChild(a);
					chartItems.add(DashboardAkademikHtmlCssHelper.item(jurusanName, tahun.toString(), jumlahSiswa));
				} else {
					totalTahunLama += jumlahSiswa;
				}
			}

			if (totalTahunLama > 0 && !previousJurusanId.equals(-1L)) {
				cetakBarisTahunLama(rows, previousJurusanName, mintahun, totalTahunLama, previousJurusanId, chartItems);
			}
		}

		Row rowTotal = new Row();
		rowTotal.setValign("top");
		rowTotal.setParent(rows);
		rowTotal.appendChild(new Label(ais.common.Common.getBahasaConfig("Total")));
		rowTotal.appendChild(new Label(""));
		A aTotal = new A(Common.numberFormat.get().format(total));
		aTotal.addEventListener("onClick", new MyEventListener(-1, -1L));
		rowTotal.appendChild(aTotal);

		if (tampilkanChart) {
			Row rowChart = new Row();
			rowChart.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowChart, "3");
			rowChart.appendChild(new Html(DashboardAkademikHtmlCssHelper.verticalBarChart(chartTitle, chartDescription,
					chartItems)));
			setStyle("min-height:" + (Math.max(330, height + (chartItems.size() * 18))) + "px");
		}
	}

	private void cetakBarisTahunLama(Rows rows, String jurusanName, int mintahun, int totalTahunLama, Long jurusanId,
			List<DashboardAkademikHtmlCssHelper.BarItem> chartItems) {
		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(jurusanName));
		row.appendChild(new Label(mintahun + ">="));
		A a = new A(Common.numberFormat.get().format(totalTahunLama));
		a.addEventListener("onClick", new MyEventListener(null, mintahun, jurusanId));
		row.appendChild(a);
		chartItems.add(DashboardAkademikHtmlCssHelper.item(jurusanName, mintahun + ">=", totalTahunLama));
	}
}
