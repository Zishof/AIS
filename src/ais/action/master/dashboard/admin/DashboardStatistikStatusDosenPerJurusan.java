package ais.action.master.dashboard.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.action.master.DosenAction;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Komponen dashboard khusus untuk dashboard statistik status dosen per jurusan. Kelas ini memilih
 * variasi data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas
 * induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code org.zkoss.zul.Html mychart}, {@code Div
 * center}, {@code boolean tampilRinci}, {@code int width}, {@code int height}; inisialisasi/lifecycle ({@code
 * reinit()}, {@code initFakultas()}, {@code init()}, {@code initChart()}); pembacaan/pencarian ({@code
 * ambilData()}); konfigurasi constructor: {@code tampilRinci}. Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardStatistikStatusDosenPerJurusan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;

	private org.zkoss.zul.Html mychart;
	private Div center;

	public DashboardStatistikStatusDosenPerJurusan() {
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

	public DashboardStatistikStatusDosenPerJurusan(int width, int height) throws Exception {
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

	public DashboardStatistikStatusDosenPerJurusan(String title, String border, boolean closable) {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	private void initFakultas() {

	}

	public class MyEventListener implements EventListener {

		private Long ikatanKerjaId;
		private Long jurusanId;

		public MyEventListener(Long ikatanKerjaId, Long jurusanId) {
			this.ikatanKerjaId = ikatanKerjaId;
			this.jurusanId = jurusanId;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(Dosen.class, new DataCriteriaWithColumn() {

						@Override
						public Object[] initCriteria(boolean order) {

							try {
								PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(Dosen.class)
										.add(ikatanKerjaId.equals(-1L) ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("ikatanKerjaDosen.id", ikatanKerjaId))
										.createAlias("jurusan", "jurusan")
										.add(jurusanId == null || jurusanId.equals(-1L)
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("jurusan.id", jurusanId))
										.createAlias("jurusan.fakultas", "fakultas")
										.add(perguruanTinggi == null || perguruanTinggi.getId() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)));

								return new Object[] { criteria, DosenAction.contents };

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

	private void init() {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center.
		 * Kartu Saringan di atas, kartu Isi (center) di bawah. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih fakultas dan program studi untuk menyaring data dosen yang ditampilkan.",
				"Statistik Status Dosen per Program Studi",
				"Sebaran dosen menurut status di tiap program studi, lengkap dengan grafiknya.");
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

		if (tampilRinci) {

			MyButtonConfig myButtonConfig = new MyButtonConfig("Lihat Rinci", "/img/12123-eyes-icon.png");
			myButtonConfig.setParent(row);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					DashboardDosen laporan = new DashboardDosen();

					laporan.setHeight("99%");
					laporan.setWidth("99%");
					laporan.setTitle("Rekap Status Dosen");
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

	public static List<Object[]> ambilData(List<IkatanKerjaDosen> ikatanKerjaDosens, PerguruanTinggi perguruanTinggi,
			Fakultas fakultas, Jurusan jurusan) {
		Session session = HibernateUtil.currentNativeSession();
		String sql = "select  ";

		for (IkatanKerjaDosen ikatanKerjaDosen : ikatanKerjaDosens) {

			sql += "sum(case a.ikatan_kerja_dosen when " + ikatanKerjaDosen.getId() + " then 1 else 0 end) as status"
					+ ikatanKerjaDosen.getId() + ",  ";
		}

		sql += " count(*) total, b.nama as jurusan, b.id as jurusan_id from dosen a "
				+ " inner join jurusan b on (a.jurusan = b.id  )  left join fakultas c on (c.id = b.fakultas)  "
				+ " where a.aktif "
				+ (perguruanTinggi == null || perguruanTinggi.getId() == null ? ""
						: " and c.perguruan_tinggi=" + perguruanTinggi.getId())

				+ (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
				+ (fakultas == null ? "" : " and a.fakultas = " + fakultas.getId())

				+ " and b.aktif and c.aktif group by b.id order by b.nama";

		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
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

		Column column = new MyColumnConfig("Prodi");
		column.setParent(columns);
		column.setWidth("30%");

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Session session = HibernateUtil.currentNativeSession();

		List<IkatanKerjaDosen> ikatanKerjaDosens = ConstantValues
				.simpleList(session.createCriteria(IkatanKerjaDosen.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.desc("nama")), IkatanKerjaDosen.class);
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		List<Column> columns2 = new ArrayList<Column>();
		for (IkatanKerjaDosen ikatanKerjaDosen : ikatanKerjaDosens) {
			column = new Column(ikatanKerjaDosen.getNama());
			column.setParent(columns);
			columns2.add(column);

		}
		column = new MyColumnConfig("Total");
		column.setParent(columns);

		List<Object[]> jurusans = DashboardStatistikStatusDosenPerJurusan.ambilData(ikatanKerjaDosens, perguruanTinggi,
				null, null);

		Rows rows = new Rows();
		rows.setParent(grid);
		CategoryModel model = new SimpleCategoryModel();
		Map<Long, Double> totals = new HashMap<Long, Double>();
		Double totalSemua = 0.0;
		for (Object[] objects : jurusans) {
			Double total = ((Number) (objects[ikatanKerjaDosens.size()] == null ? 0.0
					: objects[ikatanKerjaDosens.size()])).doubleValue();
			String jurusan = (objects[ikatanKerjaDosens.size() + 1] == null ? ""
					: objects[ikatanKerjaDosens.size() + 1]).toString();
			Long jurusanId = ((Number) (objects[ikatanKerjaDosens.size() + 2] == null ? -1L
					: objects[ikatanKerjaDosens.size() + 2])).longValue();

			MyFormRow row = new MyFormRow();
		row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(jurusan));

			int index = 0;
			for (IkatanKerjaDosen ikatanKerjaDosen : ikatanKerjaDosens) {
				Double status = ((Number) (objects[index] == null ? 0.0 : objects[index])).doubleValue();

				MyEventListener myEventListener = new MyEventListener(ikatanKerjaDosen.getId(), jurusanId);

				A a = new A(Common.numberFormat.get().format(status));
				a.addEventListener("onClick", myEventListener);
				row.appendChild(a);

				model.setValue(jurusan, ikatanKerjaDosen.getNama(), status);
				index++;

				if (totals.containsKey(ikatanKerjaDosen.getId())) {
					totals.put(ikatanKerjaDosen.getId(), status + totals.get(ikatanKerjaDosen.getId()));
				} else {
					totals.put(ikatanKerjaDosen.getId(), status);
				}
			}
			A a = new A(Common.numberFormat.get().format(total));
			a.addEventListener("onClick", new MyEventListener(-1L, jurusanId));
			row.appendChild(a);

			totalSemua += total;
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total")));
		int index = 0;
		for (IkatanKerjaDosen ikatanKerjaDosen : ikatanKerjaDosens) {
			Double tot = totals.get(ikatanKerjaDosen.getId());

			MyEventListener myEventListener = new MyEventListener(ikatanKerjaDosen.getId(), -1L);

			try {
				A a = new A(Common.numberFormat.get().format(tot));
				a.addEventListener("onClick", myEventListener);
				row.appendChild(a);

				if (tot < 0.01) {
					columns2.get(index).setWidth("0px");
					for (Object[] objects : jurusans) {
						String jurusan = (objects[ikatanKerjaDosens.size() + 1] == null ? ""
								: objects[ikatanKerjaDosens.size() + 1]).toString();
						model.removeValue(jurusan, ikatanKerjaDosen.getNama());
					}

				}
			} catch (Exception e) {
				row.appendChild(new Label());
			}
			index++;
		}
		A a = new A(Common.numberFormat.get().format(totalSemua));
		a.addEventListener("onClick", new MyEventListener(-1L, -1L));
		row.appendChild(a);

		row = new MyFormRow();
		row.setParent(rows);
		row.setSpans((ikatanKerjaDosens.size() + 2) + "");
        mychart = DashboardModernHtmlUtil.createAnyChart(model, "Dasbor Statistik Status Dosen Per Jurusan", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);
		setStyle("min-height:" + (330 + (40 * jurusans.size())) + "px");

	}
}
