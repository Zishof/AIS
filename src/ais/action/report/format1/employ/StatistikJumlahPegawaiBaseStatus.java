package ais.action.report.format1.employ;
import ais.ui.util.DashboardGridExportHelper;


import ais.ui.util.MyFormRow;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.North;
import org.zkoss.zul.Rows;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/**
 * Penyusun/penyaji laporan untuk statistik jumlah pegawai base status. Kelas ini mengubah data
 * domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Combobox
 * searchstatuspegawai}; inisialisasi/lifecycle ({@code initPegawai()}, {@code init()}, {@code initChart()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class StatistikJumlahPegawaiBaseStatus extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9199098044659403642L;
	private Center center;

	private Combobox searchstatuspegawai = new Combobox();

	

	public StatistikJumlahPegawaiBaseStatus() {
		super();
		initPegawai();
		init();
		initChart();

	}

	public StatistikJumlahPegawaiBaseStatus(String title, String border,
			boolean closable) {
		super(title, border, closable);
		initPegawai();
		init();
		initChart();
	}

	private void initPegawai() {
		// Common.insertCombo(searchstatuspegawai, new String[] { "nama", "id"
		// },
		// StatusPegawai.class);
	}

	private void init() {
		DashboardGridExportHelper.pasang(this, "Statistik Jumlah Pegawai Base Status");
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

		// MyFormRow row = new MyFormRow();row.setValign("top");
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("Status Pegawai"));
		// row.appendChild(searchstatuspegawai);
		// searchstatuspegawai.setWidth("90%");
		// searchstatuspegawai.addEventListener("onChange", new EventListener()
		// {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initChart();
		// }
		// });

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setStyle("border:0px;");

	}

	@SuppressWarnings("unchecked")
	private void initChart() {
		Common.clear(center);

		Session session = HibernateUtil.currentSession();
		String sql = "select "
				+ "sum(case a.status_pegawai when 1 then 1 else 0 end) as aktif, "
				+ "sum(case a.status_pegawai when 2 then 1 else 0 end) as cuti, "
				+ "sum(case a.status_pegawai when 3 then 1 else 0 end) as tidak_aktif, "
				+ "sum(case a.status_pegawai when 4 then 1 else 0 end) as keluar, "
				+ "sum(case a.status_pegawai when 5 then 1 else 0 end) as meninggal, "
				+ "sum(case a.status_pegawai when 6 then 1 else 0 end) as pensiun "
				+ "from pegawai a "
				+ "left join status_pegawai b on (a.status_pegawai=b.id) ";

		System.out.println(sql);
		List<Object[]> status = session.createSQLQuery(sql).list();
		if (status.size() == 0) {
			return;
		}
		Object[] objects = status.get(0);

		// PERBAIKAN BUG: pengecekan null sebelumnya keliru memakai objects[1] untuk semua kolom
		// (objects[2..5]) sehingga rawan NullPointerException / salah ambil data. Diperbaiki agar
		// tiap kolom dicek pada indeksnya sendiri.
		double aktif = ((Number) (objects[0] == null ? 0.0 : objects[0])).doubleValue();
		double cuti = ((Number) (objects[1] == null ? 0.0 : objects[1])).doubleValue();
		double tidak_aktif = ((Number) (objects[2] == null ? 0.0 : objects[2])).doubleValue();
		double keluar = ((Number) (objects[3] == null ? 0.0 : objects[3])).doubleValue();
		double meninggal = ((Number) (objects[4] == null ? 0.0 : objects[4])).doubleValue();
		double pensiun = ((Number) (objects[5] == null ? 0.0 : objects[5])).doubleValue();

		// Grafik komposisi pegawai berdasarkan status — donut HTML/CSS (HtmlChartHelper),
		// menggantikan pie 3D JFreeChart. + penjelasan bahasa sederhana untuk pengguna awam.
		String htmlDonut = ais.ui.util.HtmlChartHelper.donut("Pegawai Berdasarkan Status",
				"Menampilkan sebaran jumlah pegawai berdasarkan statusnya, yaitu aktif, cuti, tidak aktif, keluar, meninggal, dan pensiun.",
				new String[] { "Aktif", "Cuti", "Tidak Aktif", "Keluar", "Meninggal", "Pensiun" },
				new double[] { aktif, cuti, tidak_aktif, keluar, meninggal, pensiun },
				new String[] { "#42b72a", "#f7b928", "#9aa0a6", "#e4496b", "#5f6368", "#1877f2" }, "aktif");
		center.appendChild(new ais.ui.util.MyHtml(htmlDonut));

	}
}
