package ais.action.master.dashboard.sekolah;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.action.master.sekolah.SiswaAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Komponen dashboard khusus untuk dashboard statistik siswa lulus. Kelas ini memilih variasi data
 * atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code org.zkoss.zul.Html mychart}, {@code Div
 * center}, {@code int width}, {@code int height}; inisialisasi/lifecycle ({@code reinit()}, {@code init()},
 * {@code initChart()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardStatistikSiswaLulus extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;

	private org.zkoss.zul.Html mychart;
	private Div center;

	public DashboardStatistikSiswaLulus() {
		super();

	}

	public DashboardStatistikSiswaLulus(int width, int height) throws Exception {
		super();
		reinit(width, height);
	}

	public void reinit(int width, int height) throws Exception {
		this.width = width;
		this.height = height;

	}

	public DashboardStatistikSiswaLulus(String title, String border, boolean closable) {
		super(title, border, closable);

	}

	public void init() {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		/* Portal responsif (kartu tunggal, natural-height) menggantikan Borderlayout+Center. */
		center = (Div) ais.ui.util.DasborResponsifHelper.isiTunggal(this,
				"Statistik Siswa Lulus",
				"Jumlah siswa yang lulus per periode, beserta grafiknya.");
		initChart(center, true);
	}

	private int width = 750;
	private int height = 100;

	/**
	 * Event listener lokal milik {@link DashboardStatistikSiswaLulus}. Kelas ini menangani event untuk komponen
	 * induk dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DashboardStatistikSiswaLulus} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Integer tahunLulus}, {@code Long
	 * sekolahId}; operasi lokal: {@code onEvent}(). Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DashboardStatistikSiswaLulus
	 */
	public class MyEventListener implements EventListener {

		private Integer tahunLulus;
		private Long sekolahId;

		public MyEventListener(Integer tahunLulus, Long sekolahId) {
			this.tahunLulus = tahunLulus;
			this.sekolahId = sekolahId;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(Siswa.class, new DataCriteriaWithColumn() {

						@Override
						public Object[] initCriteria(boolean order) {

							try {
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa",""))
										.add(Restrictions.isNotNull("sekolah")).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.add(sekolahId.equals(-1L) ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("sekolah.id", sekolahId))
										.add(tahunLulus.equals(-1) ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunLulus", tahunLulus));

								return new Object[] { criteria, SiswaAction.contents };

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
	public void initChart(Component center, boolean tampilkanChart) {
		Common.clear(center);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Sekolah");
		column.setParent(columns);

		MyColumnConfig columnAktif = new MyColumnConfig("Tahun");
		columnAktif.setParent(columns);
		columnAktif.setWidth("20%");

		column = new MyColumnConfig("Jumlah Siswa");
		column.setParent(columns);
		column.setWidth("20%");

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

		String sql = "select\r\n" + "	b.id as sekolah_id,\r\n" + "	b.nama as sekolah,\r\n" + "	a.tahunlulus,\r\n"
				+ "	count(*) as jumlah_siswa\r\n" + "	from sekolah.siswa a\r\n"
				+ "	inner join sekolah.sekolah b on (a.sekolah_id=b.id)\r\n"
				+ "	where a.aktif and a.tahunlulus is not null \r\n" + sqlSekolah + " " + sqlYayasan + " "
				+ "	group by b.id,a.tahunlulus\r\n" + "	order by b.id,a.tahunlulus";

		List<Object[]> jurusans = Common.ambilSql(sql);

		Rows rows = new Rows();
		rows.setParent(grid);

		Integer total = 0;

		CategoryModel model = new SimpleCategoryModel();
		for (Object[] objects : jurusans) {
			Long sekolahId = ((Number) (objects[0] == null ? 0 : objects[0])).longValue();
			String sekolah = (objects[1] == null ? "" : objects[1]).toString();
			Integer tahunLulus = ((Number) (objects[2] == null ? -1 : objects[2])).intValue();
			Integer jumlahSiswa = ((Number) (objects[3] == null ? -1 : objects[3])).intValue();

			total += jumlahSiswa;

			Row row = new Row();
		row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(sekolah));
			row.appendChild(new Label(tahunLulus.toString()));
			A a = new A(Common.numberFormat.get().format(jumlahSiswa));
			a.addEventListener("onClick", new MyEventListener(tahunLulus, sekolahId));

			row.appendChild(a);

			model.setValue(sekolah, tahunLulus, jumlahSiswa);
		}

		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total")));
		row.appendChild(new Label(""));

		A a = new A(Common.numberFormat.get().format(total));
		a.addEventListener("onClick", new MyEventListener(-1, -1L));
		row.appendChild(a);

		if (tampilkanChart) {
			row = new Row();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "3");
			row.setAlign("center");

			mychart = null;
        mychart = DashboardModernHtmlUtil.createAnyChart(model, "Dasbor Statistik Siswa Lulus", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);

			setStyle("min-height:" + (330 + (40 * jurusans.size())) + "px");
		}

	}
}
