package ais.action.master.dashboard.admin;

import java.util.ArrayList;
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
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.DosenAction;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyWindow;

/**
 * Kartu dasbor admin yang menampilkan statistik jumlah dosen aktif per program studi (jurusan),
 * dibatasi otomatis sesuai cakupan user yang login (jurusan/fakultas/perguruan tinggi bila
 * berlaku), lengkap dengan grafik batang vertikal. Setiap angka jumlah dosen dapat diklik untuk
 * mengunduh daftar dosen yang mendasarinya lewat {@link Common#cetakDataCustomButton}.
 */
public class DashboardStatistikDosenJurusan extends MyWindow {

	private static final long serialVersionUID = -28636873241676666L;

	private Div center;
	private int width = 750;
	private int height = 100;

	/** Membuat instans kartu tanpa ukuran kustom; panggil {@link #init()} untuk menyusun isinya. */
	public DashboardStatistikDosenJurusan() {
		super();
	}

	/**
	 * Membuat instans kartu dengan ukuran awal kustom.
	 *
	 * @param width  lebar area chart yang diinginkan (piksel)
	 * @param height tinggi dasar area chart (piksel), disesuaikan lagi berdasarkan jumlah baris
	 */
	public DashboardStatistikDosenJurusan(int width, int height) throws Exception {
		super();
		reinit(width, height);
	}

	/** Memperbarui ukuran yang dipakai kartu ini; tidak menyusun ulang isi yang sudah dirender. */
	public void reinit(int width, int height) throws Exception {
		this.width = width;
		this.height = height;
	}

	/**
	 * Membuat instans kartu dengan judul, gaya border, dan status dapat-ditutup kustom (warisan
	 * konstruktor {@link MyWindow}).
	 */
	public DashboardStatistikDosenJurusan(String title, String border, boolean closable) {
		super(title, border, closable);
	}

	/** Menyusun kartu portal responsif (judul + deskripsi) dan langsung mengisi tabel+grafik lewat {@link #initChart(Component, boolean)}. */
	public void init() {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");

		/* Portal responsif (kartu tunggal, natural-height) menggantikan Borderlayout+Center. */
		center = (Div) ais.ui.util.DasborResponsifHelper.isiTunggal(this,
				"Statistik Dosen per Program Studi",
				"Jumlah dosen di tiap program studi, beserta grafiknya.");
		initChart(center, true);
	}

	/** Listener klik pada satu angka jumlah dosen: membuka unduhan daftar dosen jurusan terkait (atau seluruh dosen bila {@code jurusanId} adalah penanda total, {@code -1}). */
	public class MyEventListener implements EventListener {

		private Long jurusanId;

		/** @param jurusanId id jurusan yang datanya diunduh saat diklik, atau {@code -1} untuk baris total (seluruh jurusan) */
		public MyEventListener(Long jurusanId) {
			this.jurusanId = jurusanId;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {
			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(Dosen.class, new DataCriteriaWithColumn() {

						@Override
						public Object[] initCriteria(boolean order) {
							try {
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(Dosen.class).addOrder(Order.asc("nama"))
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

								if (jurusanId != null && !jurusanId.equals(-1L)) {
									criteria.add(Restrictions.eq("jurusan.id", jurusanId));
								}

								return new Object[] { criteria, DosenAction.contents };

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

	/**
	 * Menjalankan agregasi SQL jumlah dosen aktif per jurusan (dibatasi otomatis ke
	 * jurusan/fakultas/perguruan tinggi user yang login bila berlaku), menampilkannya sebagai
	 * tabel dua kolom (nama jurusan, jumlah dosen -- tiap angka dapat diklik) dengan baris total
	 * di akhir, dan opsional menambahkan grafik batang vertikal di bawah tabel.
	 *
	 * @param center         komponen wadah yang akan diisi ulang (dikosongkan lebih dulu)
	 * @param tampilkanChart {@code true} untuk menampilkan grafik batang di bawah tabel
	 */
	@SuppressWarnings({ "deprecation" })
	public void initChart(Component center, boolean tampilkanChart) {
		Common.clear(center);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(center);
		grid.setWidth("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		new MyColumnConfig("Prodi").setParent(columns);

		MyColumnConfig columnJumlah = new MyColumnConfig("Jumlah Dosen");
		columnJumlah.setParent(columns);
		columnJumlah.setWidth("30%");

		Tbmuser tbmuser = Common.getCurrentUser();
		Fakultas fakultasUser = tbmuser == null ? null : tbmuser.ambilFakultas();
		Jurusan jurusanUser = tbmuser == null ? null : tbmuser.ambilJurusan();
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT b.id AS jurusan_id, b.nama AS jurusan, COUNT(*) AS jumlah_dosen ");
		sql.append("FROM dosen a ");
		sql.append("INNER JOIN jurusan b ON (a.jurusan = b.id) ");
		sql.append("INNER JOIN fakultas c ON (b.fakultas = c.id) ");
		sql.append("WHERE (a.aktif = true OR a.aktif IS NULL) AND b.aktif = true ");

		if (jurusanUser != null && jurusanUser.getId() != null) {
			sql.append("AND a.jurusan = ").append(jurusanUser.getId()).append(" ");
		}
		if (fakultasUser != null && fakultasUser.getId() != null) {
			sql.append("AND b.fakultas = ").append(fakultasUser.getId()).append(" ");
		}
		if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
			sql.append("AND c.perguruan_tinggi = ").append(perguruanTinggi.getId()).append(" ");
		}

		sql.append("GROUP BY b.id, b.nama ");
		sql.append("ORDER BY b.nama ASC");

		List<Object[]> jurusans = Common.ambilSql(sql.toString());

		Rows rows = new Rows();
		rows.setParent(grid);

		Integer total = 0;
		List<DashboardAkademikHtmlCssHelper.BarItem> chartItems = new ArrayList<DashboardAkademikHtmlCssHelper.BarItem>();

		if (jurusans != null) {
			for (Object[] objects : jurusans) {
				Long jurusanId = objects[0] != null ? ((Number) objects[0]).longValue() : 0L;
				String jurusan = objects[1] != null ? objects[1].toString() : "";
				Integer jumlahDosen = objects[2] != null ? ((Number) objects[2]).intValue() : 0;

				total += jumlahDosen;

				Row row = new Row();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new Label(jurusan));

				A a = new A(Common.numberFormat.get().format(jumlahDosen));
				a.addEventListener("onClick", new MyEventListener(jurusanId));
				row.appendChild(a);

				chartItems.add(DashboardAkademikHtmlCssHelper.item(jurusan, "Dosen", jumlahDosen));
			}
		}

		Row rowTotal = new Row();
		rowTotal.setValign("top");
		rowTotal.setParent(rows);
		rowTotal.appendChild(new Label(ais.common.Common.getBahasaConfig("Total")));

		A aTotal = new A(Common.numberFormat.get().format(total));
		aTotal.addEventListener("onClick", new MyEventListener(-1L));
		rowTotal.appendChild(aTotal);

		if (tampilkanChart) {
			Row rowChart = new Row();
			rowChart.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowChart, "2");
			rowChart.appendChild(new Html(DashboardAkademikHtmlCssHelper.verticalBarChart("Grafik Dosen per Prodi",
					"Batang memperlihatkan jumlah dosen aktif pada setiap program studi.", chartItems)));
			setStyle("min-height:" + (Math.max(330, height + (chartItems.size() * 18))) + "px");
		}
	}
}
