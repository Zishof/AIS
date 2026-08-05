package ais.action.master.dashboard.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.MyWindow;

public class DashboardTampilkanDataHelper extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 849022873969503254L;

	private String sql;

	private String judul;

	public DashboardTampilkanDataHelper(String sql, String judul) {
		super();
		this.sql = sql;
		this.judul = judul;
		display();
	}

	public DashboardTampilkanDataHelper(String sql, String judul, String title, String border, boolean closable) {
		super(title, border, closable);
		this.sql = sql;
		this.judul = judul;
		display();
	}

	public DashboardTampilkanDataHelper(String sql, String judul, int width, int height) throws Exception {
		super();
		this.sql = sql;
		this.judul = judul;
		display();
	}

	private void display() {

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
		final Intbox ukuran = new Intbox(6);

		new Thread(new Runnable() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void run() {
				System.out.println(sql);

				Session session = HibernateUtil.currentSession();

				List<Map<String, Object>> itemBiayas = session.createSQLQuery(sql)
						.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();
				if (itemBiayas.isEmpty()) {
					return;
				}

				List<List> datas = new ArrayList<List>();
				ArrayList arrayList = new ArrayList();
				arrayList.add(judul);
				datas.add(arrayList);

				arrayList = new ArrayList();
				Map<String, Object> dataAtas = itemBiayas.get(0);
				Set<String> dataKey = dataAtas.keySet();
				arrayList.add("No.");
				for (String judul : dataKey) {
					arrayList.add(judul);
				}
				datas.add(arrayList);

				ukuran.setValue(dataKey.size() + 2);

				int index = 1;
				for (Map<String, Object> map : itemBiayas) {
					arrayList = new ArrayList();
					arrayList.add(index);
					for (String judul : dataKey) {
						arrayList.add(map.get(judul));
					}
					datas.add(arrayList);
					index++;
				}
				label.setAttribute("datas", datas);
				label.setValue("");
			}
		}).start();

		Common.clear(this);
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		SaptoUtil.displayWorksheet(label, "data_umum", center, ukuran.getValue());
	}

}
