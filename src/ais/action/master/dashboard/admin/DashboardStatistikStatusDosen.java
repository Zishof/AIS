package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimplePieModel;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardStatistikStatusDosen extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;

	private org.zkoss.zul.Html mychart;
	private Div center;
	private SimplePieModel simplePieModel;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	public DashboardStatistikStatusDosen() {
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

	public DashboardStatistikStatusDosen(int width, int height) throws Exception {
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

	public DashboardStatistikStatusDosen(String title, String border, boolean closable) {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	private void init() {
		DashboardGridExportHelper.pasang(this, "Statistik Status Dosen");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center.
		 * Kartu Saringan di atas, kartu Isi (center) di bawah. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih fakultas dan program studi untuk menyaring data dosen yang ditampilkan.",
				"Statistik Status Dosen",
				"Sebaran dosen menurut status kepegawaian, lengkap dengan grafiknya.");
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
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row.setParent(rows);
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

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

	private void initChart() {
		Common.clear(center);
		mychart = null;

		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		simplePieModel = new SimplePieModel();
		simplePieModel.clear();

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		Session session = HibernateUtil.currentSession();

		List<IkatanKerjaDosen> ikatanKerjaDosens = ConstantValues
				.simpleList(session.createCriteria(IkatanKerjaDosen.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.desc("nama")), IkatanKerjaDosen.class);

		String sql = "select  ";

		for (IkatanKerjaDosen ikatanKerjaDosen : ikatanKerjaDosens) {
			sql += "sum(case a.ikatan_kerja_dosen when " + ikatanKerjaDosen.getId() + " then 1 else 0 end) as status"
					+ ikatanKerjaDosen.getId() + ",  ";
		}

		sql += " count(*) total from dosen a "
				+ "left join jurusan b on (a.jurusan = b.id  )  left join fakultas c on (c.id = b.fakultas)  "
				+ " where a.aktif  "
				+ (perguruanTinggi == null || perguruanTinggi.getId() == null ? ""
						: " and c.perguruan_tinggi=" + perguruanTinggi.getId())
				+ " and b.aktif and c.aktif  " + (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
				+ (fakultas == null ? "" : " and b.fakultas = " + fakultas.getId());

		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);

		Object[] objects = jurusans.get(0);
		Double total = ((Number) (objects[ikatanKerjaDosens.size()] == null ? 0.0 : objects[ikatanKerjaDosens.size()]))
				.doubleValue();
		int index = 0;
		for (IkatanKerjaDosen ikatanKerjaDosen : ikatanKerjaDosens) {
			Double status = ((Number) (objects[index] == null ? 0.0 : objects[index])).doubleValue();
			if (status > 0.1) {
				simplePieModel.setValue(ikatanKerjaDosen.getNama() + " (" + Common.numberFormat.get().format(status) + "/"
						+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : status * 100 / total) + "%)", status);
			}
			index++;
		}
        mychart = DashboardModernHtmlUtil.createAnyChart(simplePieModel, "Dasbor Statistik Status Dosen", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);

	}
}
