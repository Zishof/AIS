package ais.action.master.dashboard.sekolah;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;
import org.zkoss.zul.Vbox;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardStatistikKelasSiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;

	private org.zkoss.zul.Html mychart;
	private Div center;

	private Combobox tahunAjaran;

	public DashboardStatistikKelasSiswa() {
		super();

	}

	public DashboardStatistikKelasSiswa(int width, int height) throws Exception {
		super();
		reinit(width, height);
	}

	public void reinit(int width, int height) throws Exception {
		this.width = width;
		this.height = height;

	}

	public DashboardStatistikKelasSiswa(String title, String border, boolean closable) {
		super(title, border, closable);

	}

	public void init() {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih tahun ajaran untuk menyaring data yang ditampilkan.",
				"Statistik Kelas Siswa",
				"Sebaran jumlah siswa per kelas, beserta grafiknya.");
		Component saringanHost = hostPortal[0];
		center = (Div) hostPortal[1];

		tahunAjaran = Common.generateTahunAjaran(null);
		Hbox hbox = new Hbox();
		hbox.setParent(saringanHost);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun Ajaran:")));
		hbox.appendChild(tahunAjaran);
		tahunAjaran.setCols(3);
		tahunAjaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart(center, true, tahunAjaran);
			}
		});

		initChart(center, true, tahunAjaran);
	}

	private int width = 750;
	private int height = 100;

	public class MyEventListener implements EventListener {

		private Long kelas_id;
		private String ta;

		public MyEventListener(Long kelas_id, String ta) {
			this.kelas_id = kelas_id;
			this.ta = ta;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(KelasSiswaPunyaSiswa.class, new DataCriteriaWithColumn() {

						@Override
						public Object[] initCriteria(boolean order) {

							System.out.println("kelas_id -> " + kelas_id + ", ta -> " + ta);

							try {
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(KelasSiswaPunyaSiswa.class)
										.createAlias("siswa", "siswa").addOrder(Order.asc("siswa.nama"))
										.createAlias("kelasSiswa", "kelasSiswa").add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.add(kelas_id.equals(-1L) ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("kelasSiswa.id", kelas_id))
										.add(Restrictions.eq("kelasSiswa.tahunAjaran", ta));

								return new Object[] { criteria,
										new String[] { "siswa.namaSiswa", "siswa.namaSiswa", "siswa.nomorInduk",
												"siswa.nomorIndukNasional", "siswa.tempatLahir",
												"siswa.tanggalLahir" } };

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
	public void initChart(Component center, boolean tampilkanChart, Combobox tahunAjaran) {
		Common.clear(center);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Kelas");
		column.setParent(columns);

		column = new MyColumnConfig("Jumlah");
		column.setParent(columns);
		column.setWidth("20%");

		String ta = (String) tahunAjaran.getSelectedItem().getValue();

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

		String sql = "select\r\n" + "a.id as kelas_id,\r\n" + "a.nama as nama_kelas,\r\n"
				+ "count(b.id) as jumlah_siswa\r\n" + "from sekolah.kelas a\r\n"
				+ "inner join sekolah.kelas_punya_siswa c on (c.kelas_id=a.id)\r\n"
				+ "inner join sekolah.siswa b on (c.siswa_id = b.id)\r\n"
				+ "where c.aktif and a.aktif and a.tahunajaran='" + ta + "'" + sqlSekolah + " " + sqlYayasan + " " +

				"group by a.id order by a.nama";

		List<Object[]> jurusans = Common.ambilSql(sql);

		Rows rows = new Rows();
		rows.setParent(grid);

		Integer total = 0;

		CategoryModel model = new SimpleCategoryModel();
		for (Object[] objects : jurusans) {
			Long kelas_id = ((Number) (objects[0] == null ? 0 : objects[0])).longValue();
			String nama_kelas = (objects[1] == null ? "" : objects[1]).toString();

			Integer jumlah_siswa = ((Number) (objects[2] == null ? -1 : objects[2])).intValue();

			total += jumlah_siswa;

			Row row = new Row();
		row.setValign("top");
			row.setParent(rows);
			Vbox vbox = new Vbox();
			row.appendChild(vbox);
			vbox.appendChild(new MyLabelBoldAja(nama_kelas));
			A a = new A(Common.numberFormat.get().format(jumlah_siswa));
			a.addEventListener("onClick", new MyEventListener(kelas_id, ta));

			row.appendChild(a);

			model.setValue(nama_kelas, "", jumlah_siswa);
		}

		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total")));

		A a = new A(Common.numberFormat.get().format(total));
		a.addEventListener("onClick", new MyEventListener(-1L, ta));
		row.appendChild(a);

		if (tampilkanChart) {
			row = new Row();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "3");
			row.setAlign("center");

			mychart = null;
        mychart = DashboardModernHtmlUtil.createAnyChart(model, "Dasbor Statistik Kelas Siswa", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);

			setStyle("min-height:" + (330 + (40 * jurusans.size())) + "px");
		}

	}
}
