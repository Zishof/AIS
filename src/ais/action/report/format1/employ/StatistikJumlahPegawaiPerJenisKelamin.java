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
import org.zkoss.zul.Vbox;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.StatusPegawai;

/**
 * Penyusun/penyaji laporan untuk statistik jumlah pegawai per jenis kelamin. Kelas ini mengubah
 * data domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Vbox box},
 * {@code Combobox searchstatuspegawai}; inisialisasi/lifecycle ({@code initPegawai()}, {@code init()}, {@code
 * initChart()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class StatistikJumlahPegawaiPerJenisKelamin extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9199098044659403642L;
	private Center center;
	private Vbox box;
	private Combobox searchstatuspegawai = new Combobox();

	// 

	public StatistikJumlahPegawaiPerJenisKelamin() {
		super();
		initPegawai();
		init();
		initChart();

	}

	public StatistikJumlahPegawaiPerJenisKelamin(String title, String border,
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
		DashboardGridExportHelper.pasang(this, "Statistik Jumlah Pegawai Per Jenis Kelamin");
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
		column.setParent(columns);

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
		box = new Vbox();
		box.setWidth("600px");
		box.setAlign("center");
		box.setPack("center");
		box.setParent(center);

		StatusPegawai statusPegawai = (StatusPegawai) (searchstatuspegawai
				.getSelectedItem() == null ? null : searchstatuspegawai
				.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		String sql = "select "
				+ "sum(case a.kelamin when 'Laki-laki' then 1 else 0 end) as laki_laki, "
				+ "sum(case a.kelamin when 'Perempuan' then 1 else 0 end) as perempuan "
				+ "from pegawai a "
				+ "left join status_pegawai b on (a.status_pegawai=b.id) "
				+ " where 1=1 "
				+ (statusPegawai == null ? "" : " and a.status_pegawai = "
						+ statusPegawai.getId());

		System.out.println(sql);
		List<Object[]> kelamins = session.createSQLQuery(sql).list();
		if (kelamins.size() == 0) {
			return;
		}
		Object[] objects = kelamins.get(0);

		Double laki_laki = ((Number) (objects[0] == null ? 0.0 : objects[0]))
				.doubleValue();
		Double perempuan = ((Number) (objects[1] == null ? 0.0 : objects[1]))
				.doubleValue();

		Double total = laki_laki + perempuan;

		// Grafik komposisi pegawai per jenis kelamin — donut HTML/CSS (HtmlChartHelper),
		// menggantikan pie 3D JFreeChart. Ringan, responsif, + penjelasan untuk pengguna awam.
		String htmlDonut = ais.ui.util.HtmlChartHelper.donut("Pegawai Laki-laki vs Perempuan",
				"Menampilkan perbandingan jumlah pegawai laki-laki dan perempuan.",
				new String[] { "Laki-laki", "Perempuan" }, new double[] { laki_laki, perempuan },
				new String[] { "#1877f2", "#e4496b" }, "laki-laki");
		box.appendChild(new ais.ui.util.MyHtml(htmlDonut));

	}
}
