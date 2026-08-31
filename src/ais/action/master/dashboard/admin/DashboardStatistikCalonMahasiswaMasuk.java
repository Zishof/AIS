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

import ais.action.master.pmb.CetakRegistrasiAction;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyWindow;

/**
 * Widget dasbor "Statistik Calon Mahasiswa Masuk": tabel + grafik batang HTML/CSS jumlah
 * {@link BiodataCalonMahasiswa} (pendaftar) per program studi dan tahun masuk, dibatasi otomatis
 * ke fakultas/jurusan/perguruan tinggi milik {@link Tbmuser} yang login. Tahun-tahun lama (di luar
 * jendela tampilan — 50 tahun terakhir untuk mode chart, 5 tahun untuk mode ringkas) digabung jadi
 * satu baris "&gt;= tahun" per prodi lewat {@link #cetakBarisTahunLama} agar tabel tidak terlalu
 * panjang.
 *
 * <p>
 * Setiap angka jumlah pada tabel adalah tautan ({@link A}) yang bila diklik memicu
 * {@link MyEventListener}: membuka unduhan Excel data {@link BiodataCalonMahasiswa} yang cocok
 * (prodi + tahun spesifik, atau "&gt;= tahun" untuk baris gabungan) lewat
 * {@link Common#cetakDataCustomButton}. Data agregat diambil lewat satu query SQL native; id
 * fakultas/jurusan/PT yang disisipkan ke SQL berasal dari objek terpilih user (bukan input bebas
 * pengguna).
 * </p>
 */
public class DashboardStatistikCalonMahasiswaMasuk extends MyWindow {

	private static final long serialVersionUID = -28636873241676666L;

	private Div center;
	private int width = 750;
	private int height = 100;

	/** Konstruktor default kosong; pemanggil bertanggung jawab memanggil {@link #init()} sendiri. */
	public DashboardStatistikCalonMahasiswaMasuk() {
		super();
	}

	/** Konstruktor mode widget berukuran tetap; hanya menyimpan ukuran (tidak otomatis memanggil {@link #init()}). */
	public DashboardStatistikCalonMahasiswaMasuk(int width, int height) throws Exception {
		super();
		reinit(width, height);
	}

	/** Menyimpan ukuran widget baru. */
	public void reinit(int width, int height) throws Exception {
		this.width = width;
		this.height = height;
	}

	/** Konstruktor dengan judul/border/closable eksplisit. */
	public DashboardStatistikCalonMahasiswaMasuk(String title, String border, boolean closable) {
		super(title, border, closable);
	}

	/** Membangun kartu dasbor tunggal (via {@link ais.ui.util.DasborResponsifHelper#isiTunggal}) dan memuat tabel+grafik lewat {@link #initChart(Component, boolean)} dengan grafik ditampilkan. */
	public void init() {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");

		/* Portal responsif (kartu tunggal, natural-height) menggantikan Borderlayout+Center. */
		center = (Div) ais.ui.util.DasborResponsifHelper.isiTunggal(this,
				"Statistik Calon Mahasiswa Masuk",
				"Jumlah calon mahasiswa (pendaftar) per program studi, beserta grafiknya.");
		initChart(center, true);
	}

	/**
	 * Listener klik untuk tautan angka pada tabel: memicu unduhan data
	 * {@link BiodataCalonMahasiswa} yang cocok (prodi tertentu atau semua, tahun spesifik atau
	 * {@code <= mintahun} untuk baris gabungan "tahun lama") lewat
	 * {@link Common#cetakDataCustomButton}.
	 */
	public class MyEventListener implements EventListener {

		private Integer tahunMasuk;
		private Long jurusanId;
		private Integer mintahun = null;

		/** Listener untuk baris tahun spesifik (bukan gabungan). */
		public MyEventListener(Integer tahunMasuk, Long jurusanId) {
			this.tahunMasuk = tahunMasuk;
			this.jurusanId = jurusanId;
		}

		/** Listener untuk baris gabungan "tahun lama" (filter {@code tahun <= mintahun}). */
		public MyEventListener(Integer tahunMasuk, Integer mintahun, Long jurusanId) {
			this.tahunMasuk = tahunMasuk;
			this.jurusanId = jurusanId;
			this.mintahun = mintahun;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {
			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(BiodataCalonMahasiswa.class, new DataCriteriaWithColumn() {

						@Override
						public Object[] initCriteria(boolean order) {
							try {
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(BiodataCalonMahasiswa.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.addOrder(Order.desc("tahun"));

								if (jurusanId != null && !jurusanId.equals(-1L)) {
									criteria.add(Restrictions.eq("prodiLulus.id", jurusanId));
								}

								if (mintahun != null) {
									criteria.add(Restrictions.le("tahun", mintahun));
								} else if (tahunMasuk != null && !tahunMasuk.equals(-1)) {
									criteria.add(Restrictions.eq("tahun", tahunMasuk));
								}

								return new Object[] { criteria, CetakRegistrasiAction.contents };

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
							return null;
						}

					}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
							new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "" })
					.getAttribute("eventListener");

			if (eventListener != null) {
				eventListener.onEvent(null);
			}
		}
	}

	@SuppressWarnings({ "deprecation" })
	/**
	 * Menjalankan query agregat jumlah calon mahasiswa per prodi/tahun (dibatasi fakultas/jurusan/PT
	 * user), lalu merender tabel: baris per (prodi, tahun) dalam jendela tampilan (50 tahun terakhir
	 * bila {@code tampilkanChart}, 5 tahun bila tidak), digabung jadi satu baris "&gt;= tahun" untuk
	 * sisanya per prodi lewat {@link #cetakBarisTahunLama}, ditutup baris total. Bila
	 * {@code tampilkanChart} true, menambahkan grafik batang HTML/CSS di bawah tabel dan menyesuaikan
	 * tinggi minimum komponen berdasarkan jumlah item grafik.
	 *
	 * @param center         komponen host tempat tabel/grafik dirender (dibersihkan dulu)
	 * @param tampilkanChart sertakan grafik batang dan gunakan jendela tahun yang lebih lebar bila {@code true}
	 */
	public void initChart(Component center, boolean tampilkanChart) {
		Common.clear(center);

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

		MyColumnConfig columnJumlah = new MyColumnConfig("Jumlah Cal Mhs");
		columnJumlah.setParent(columns);
		columnJumlah.setWidth("20%");

		Tbmuser tbmuser = Common.getCurrentUser();
		Fakultas fakultas = (tbmuser == null) ? null : tbmuser.ambilFakultas();
		Jurusan jurusana = (tbmuser == null) ? null : tbmuser.ambilJurusan();
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT b.id AS jurusan_id, b.nama AS jurusan, a.tahun, COUNT(*) AS jumlah_siswa ");
		sql.append("FROM biodata_calon_mahasiswa a ");
		sql.append("INNER JOIN jurusan b ON (a.prodi_lulus = b.id) ");
		sql.append("INNER JOIN fakultas c ON (b.fakultas = c.id) ");
		sql.append("WHERE b.aktif = true AND (a.aktif = true OR a.aktif IS NULL) ");
		sql.append("AND a.tahun > 1900 AND a.tahun < 2100 ");

		if (jurusana != null && jurusana.getId() != null) {
			sql.append("AND a.prodi_lulus = ").append(jurusana.getId()).append(" ");
		}
		if (fakultas != null && fakultas.getId() != null) {
			sql.append("AND b.fakultas = ").append(fakultas.getId()).append(" ");
		}
		if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
			sql.append("AND c.perguruan_tinggi = ").append(perguruanTinggi.getId()).append(" ");
		}

		sql.append("GROUP BY b.id, b.nama, a.tahun ");
		sql.append("ORDER BY b.id ASC, a.tahun DESC");

		List<Object[]> jurusans = Common.ambilSql(sql.toString());

		Rows rows = new Rows();
		rows.setParent(grid);

		Integer total = 0;
		Integer totalTahunLama = 0;

		Map<Long, Integer> maxtahuns = new HashMap<Long, Integer>();
		List<DashboardAkademikHtmlCssHelper.BarItem> chartItems = new ArrayList<DashboardAkademikHtmlCssHelper.BarItem>();

		if (jurusans != null) {
			for (Object[] objects : jurusans) {
				Long jurusanId = objects[0] != null ? ((Number) objects[0]).longValue() : 0L;
				Integer tahunMasuk = objects[2] != null ? ((Number) objects[2]).intValue() : -1;

				Integer maxtahun = maxtahuns.containsKey(jurusanId) ? maxtahuns.get(jurusanId) : -1;
				if (maxtahun < tahunMasuk) {
					maxtahuns.put(jurusanId, tahunMasuk);
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
				Integer tahunMasuk = objects[2] != null ? ((Number) objects[2]).intValue() : -1;
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

				if (mintahun < tahunMasuk.intValue()) {
					Row row = new Row();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label(jurusanName));
					row.appendChild(new Label(tahunMasuk.toString()));

					A a = new A(Common.numberFormat.get().format(jumlahSiswa));
					a.addEventListener("onClick", new MyEventListener(tahunMasuk, jurusanId));
					row.appendChild(a);

					chartItems.add(DashboardAkademikHtmlCssHelper.item(jurusanName, tahunMasuk.toString(), jumlahSiswa));
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
			rowChart.appendChild(new Html(DashboardAkademikHtmlCssHelper.verticalBarChart("Grafik Calon Mahasiswa per Prodi/Tahun",
					"Batang memperlihatkan jumlah calon mahasiswa pada setiap prodi dan tahun pendaftaran.", chartItems)));
			setStyle("min-height:" + (Math.max(330, height + (chartItems.size() * 18))) + "px");
		}
	}

	/** Menambahkan satu baris tabel gabungan "&gt;= {@code mintahun}" untuk prodi {@code jurusanName}, beserta item grafik batang yang sesuai. */
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
