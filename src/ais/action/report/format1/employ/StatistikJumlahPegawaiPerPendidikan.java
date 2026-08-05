package ais.action.report.format1.employ;
import ais.ui.util.DashboardGridExportHelper;

import java.util.List;

import org.hibernate.Session;
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
import ais.database.model.StatusPegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.Pendidikan;

public class StatistikJumlahPegawaiPerPendidikan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9199098044659403642L;
	private Center center;

	private Combobox searchstatuspegawai = new Combobox();

	

	public StatistikJumlahPegawaiPerPendidikan() {
		super();
		initPegawai();
		init();
		initChart();

	}

	public StatistikJumlahPegawaiPerPendidikan(String title, String border,
			boolean closable) {
		super(title, border, closable);
		initPegawai();
		init();
		initChart();
	}

	private void initPegawai() {
		Common.insertCombo(searchstatuspegawai, new String[] { "nama", "id" },
				StatusPegawai.class);
	}

	private void init() {
		DashboardGridExportHelper.pasang(this, "Statistik Jumlah Pegawai Per Pendidikan");
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
		
		Rows rows = new Rows();
		rows.setParent(grid);
		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Pegawai"));
		row.appendChild(searchstatuspegawai);
		searchstatuspegawai.setWidth("90%");
		searchstatuspegawai.addEventListener("onChange", new EventListener() {

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

		StatusPegawai statusPegawai = (StatusPegawai) (searchstatuspegawai
				.getSelectedItem() == null ? null : searchstatuspegawai
				.getSelectedItem().getValue());

	  

		// Data untuk grafik batang HTML/CSS (HtmlChartHelper) — jumlah pegawai per pendidikan.
		java.util.List<String> pendidikanList = new java.util.ArrayList<String>();
		java.util.List<Double> jumlahPendidikanList = new java.util.ArrayList<Double>();

		Session session = HibernateUtil.currentSession();
		String sql = "select max(b.nama) as pendidikan,count(a.id) as jumlah from (select a.id as id,(select aa.pendidikan from employ.riwayat_pendidikan_pegawai aa where aa.pegawai = a.id order by aa.tahun_lulus desc limit 1) as id_pendidikan from pegawai a where 1=1 " 	
				+ (statusPegawai == null ? "" : " and a.status_pegawai = "
						+ statusPegawai.getId())
				+" ) as a left join employ.pendidikan b on (a.id_pendidikan = b.id)" +"where a.id_pendidikan is not null group by a.id_pendidikan, b.id order by b.id";
		System.out.println(sql);
		List<Object[]> pendidikan = session.createSQLQuery(sql).list();
		if (pendidikan.size() == 0) {
			return;
		}

		for (Object[] objects : pendidikan) {
			String namaPendidikan = objects[0] == null ? "" : objects[0].toString();
			double jumlah = ((Number) (objects[1] == null ? 0.0 : objects[1])).doubleValue();

			pendidikanList.add(namaPendidikan);
			jumlahPendidikanList.add(jumlah);
		}
		//List<Pendidikan> pendidikan2 = session.createCriteria(Pendidikan.class)
			//	.list();
//		
//		for (Pendidikan p : pendidikan2) {
//			String sql ="select max(b.id) as pendidikan,count(a.id) as jumlah from (select a.id as id,(select aa.pendidikan from employ.riwayat_pendidikan_pegawai aa where aa.pegawai = a.id order by aa.tahun_lulus desc limit 1) as id_pendidikan from pegawai a where 1=1 " 	
//					+" ) as a left join employ.pendidikan b on (a.id_pendidikan = b.id)" +"where a.id_pendidikan is not null group by a.id_pendidikan, b.id order by b.id";
//		
//			System.out.println(sql);
//			List<Object[]> pegawai = session.createSQLQuery(sql).list();
//			if (pegawai.size() == 0) {
//				return;
//			}
//			 
//			
//			Double total = 0.0;
//			for (Object[] objects : pegawai) {
//				Double jumlah = ((Number) (objects[1] == null ? 0.0 : objects[1]))
//						.doubleValue();
//				total += jumlah;
//				categoryModel.setValue("Jumlah", p.getNama(),jumlah);
//				
//				  
//			}

	 					
		
		 

		
	 

		double[] nilaiPendidikan = new double[jumlahPendidikanList.size()];
		for (int i = 0; i < nilaiPendidikan.length; i++) {
			nilaiPendidikan[i] = jumlahPendidikanList.get(i).doubleValue();
		}
		// Grafik batang HTML/CSS modern + penjelasan bahasa sederhana untuk pengguna awam.
		String htmlBar = ais.ui.util.HtmlChartHelper.barHorizontal("Pegawai per Jenjang Pendidikan",
				"Menampilkan jumlah pegawai berdasarkan jenjang pendidikan terakhir.",
				pendidikanList.toArray(new String[pendidikanList.size()]), nilaiPendidikan, "#8b5cf6");
		center.appendChild(new ais.ui.util.MyHtml(htmlBar));

}
}
