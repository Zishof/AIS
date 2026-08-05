package ais.action.master.dashboard.sekolah;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.CategoryModel;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.action.master.sekolah.SiswaAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardStatistikSiswaMasuk extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;

	private org.zkoss.zul.Html mychart;
	private Div center;

	public DashboardStatistikSiswaMasuk() {
		super();

	}

	public DashboardStatistikSiswaMasuk(int width, int height) throws Exception {
		super();
		reinit(width, height);
	}

	public void reinit(int width, int height) throws Exception {
		this.width = width;
		this.height = height;

	}

	public DashboardStatistikSiswaMasuk(String title, String border, boolean closable) {
		super(title, border, closable);

	}

	public void init() {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		/* Portal responsif (kartu tunggal, natural-height) menggantikan Borderlayout+Center. */
		center = (Div) ais.ui.util.DasborResponsifHelper.isiTunggal(this,
				"Statistik Siswa Masuk",
				"Jumlah siswa baru per kelas dan tahun ajaran, beserta grafiknya.");
		initChart(center, true);
	}

	private int width = 750;
	private int height = 100;

	public class MyEventListener implements EventListener {

		private Integer tahunMasuk;
		private Long sekolahId;

		public MyEventListener(Integer tahunMasuk, Long sekolahId) {
			this.tahunMasuk = tahunMasuk;
			this.sekolahId = sekolahId;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(Siswa.class, new DataCriteriaWithColumn() {

						@Override
						public Object[] initCriteria(boolean order) {

							try {
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa",""))
										.add(Restrictions.isNotNull("sekolah")).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.add(sekolahId.equals(-1L) ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("sekolah.id", sekolahId))
										.add(tahunMasuk.equals(-1) ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunMasuk", tahunMasuk));

								return new Object[] { criteria, SiswaAction.contents };

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

	@SuppressWarnings({ "deprecation" })
	public void initChart(Component center, boolean tampilkanChart) {
		Common.clear(center);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Sekolah");
		column.setParent(columns);

		MyColumnConfig columnAktif = new MyColumnConfig("Tahun");
		columnAktif.setParent(columns);
		columnAktif.setWidth("20%");

		column = new MyColumnConfig("Jumlah Siswa");
		column.setParent(columns);
		column.setWidth("20%");

		Yayasan yayasana = SekolahUtil.getYayasan();
		Sekolah sekolaha = SekolahUtil.getSekolah();

		String sqlSekolah = "";
		if (sekolaha != null && sekolaha.getId() != null) {
			sqlSekolah = "and a.sekolah_id=" + sekolaha.getId();
		}

		String sqlYayasan = "";
		if (yayasana != null && yayasana.getId() != null) {
			sqlYayasan = "and a.yayasan_id=" + yayasana.getId();
		}

		String sql = "\r\n" + "	select\r\n" + "	b.id as sekolah_id,\r\n" + "	b.nama as sekolah,\r\n"
				+ "	a.tahun_masuk,\r\n" + "	count(*) as jumlah_siswa\r\n" + "	from sekolah.siswa a\r\n"
				+ "	inner join sekolah.sekolah b on (a.sekolah_id=b.id)\r\n" + "	where a.aktif\r\n " + sqlSekolah
				+ " " + sqlYayasan + "	group by b.id,a.tahun_masuk\r\n" + "	order by b.id,a.tahun_masuk";

//		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);

		Rows rows = new Rows();
		rows.setParent(grid);

		Integer total = 0;

		CategoryModel model = new SimpleCategoryModel();
		for (Object[] objects : jurusans) {
			Long sekolahId = ((Number) (objects[0] == null ? 0 : objects[0])).longValue();
			String sekolah = (objects[1] == null ? "" : objects[1]).toString();
			Integer tahunMasuk = ((Number) (objects[2] == null ? -1 : objects[2])).intValue();
			Integer jumlahSiswa = ((Number) (objects[3] == null ? -1 : objects[3])).intValue();

			total += jumlahSiswa;

			Row row = new Row();
		row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(sekolah));
			row.appendChild(new Label(tahunMasuk.toString()));
			A a = new A(Common.numberFormat.get().format(jumlahSiswa));
			a.addEventListener("onClick", new MyEventListener(tahunMasuk, sekolahId));

			row.appendChild(a);

			model.setValue(sekolah, tahunMasuk, jumlahSiswa);
		}

		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total")));
		row.appendChild(new Label(""));

		A a = new A(Common.numberFormat.get().format(total));
		a.addEventListener("onClick", new MyEventListener(-1, -1L));
		row.appendChild(a);

		if (tampilkanChart) {
			row = new Row();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "3");
			row.setAlign("center");

			mychart = null;
        mychart = DashboardModernHtmlUtil.createAnyChart(model, "Dasbor Statistik Siswa Masuk", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);

			setStyle("min-height:" + (330 + (40 * jurusans.size())) + "px");
		}

	}
}
