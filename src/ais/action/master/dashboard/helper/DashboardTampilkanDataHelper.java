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

/**
 * Komponen dashboard khusus untuk dashboard tampilkan data. Kelas ini memilih variasi data atau
 * tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String sql}, {@code String judul};
 * operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
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
