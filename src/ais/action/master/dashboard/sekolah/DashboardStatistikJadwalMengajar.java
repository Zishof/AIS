package ais.action.master.dashboard.sekolah;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;
import org.zkoss.zul.Vbox;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Komponen dashboard khusus untuk dashboard statistik jadwal mengajar. Kelas ini memilih variasi
 * data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas
 * induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code org.zkoss.zul.Html mychart}, {@code Div
 * center}, {@code Combobox tahunAjaran}, {@code int width}, {@code int height}; inisialisasi/lifecycle ({@code
 * reinit()}, {@code init()}, {@code initChart()}); pembacaan/pencarian ({@code ambilData()}, {@code
 * ambilDataSemua()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardStatistikJadwalMengajar extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;

	private org.zkoss.zul.Html mychart;
	private Div center;

	private Combobox tahunAjaran;

	public DashboardStatistikJadwalMengajar() {
		super();

	}

	public DashboardStatistikJadwalMengajar(int width, int height) throws Exception {
		super();
		reinit(width, height);
	}

	public void reinit(int width, int height) throws Exception {
		this.width = width;
		this.height = height;

	}

	public DashboardStatistikJadwalMengajar(String title, String border, boolean closable) {
		super(title, border, closable);

	}

	public void init() {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih tahun ajaran dan semester untuk menyaring data yang ditampilkan.",
				"Statistik Jadwal Mengajar",
				"Sebaran jadwal mengajar guru per tahun ajaran dan semester, beserta grafiknya.");
		Component saringanHost = hostPortal[0];
		center = (Div) hostPortal[1];

		tahunAjaran = Common.generateTahunAjaran(null);

		final Combobox searchJenisSemester = new Combobox();
		Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(1);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(2);
		searchJenisSemester.appendChild(comboitem);

		Common.selectComboItem(searchJenisSemester, Common.isNowSemensterGanjil() ? 1 : 2);
		searchJenisSemester.setReadonly(true);

		Hbox hbox = new Hbox();
		hbox.setParent(saringanHost);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun Ajaran:")));
		hbox.appendChild(tahunAjaran);
		tahunAjaran.setCols(3);
		tahunAjaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart(center, true, tahunAjaran, null, null,
						(Integer) searchJenisSemester.getSelectedItem().getValue());
			}
		});

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Semester:")));
		hbox.appendChild(searchJenisSemester);
		searchJenisSemester.setCols(2);
		searchJenisSemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart(center, true, tahunAjaran, null, null,
						(Integer) searchJenisSemester.getSelectedItem().getValue());
			}
		});

		initChart(center, true, tahunAjaran, null, null, (Integer) searchJenisSemester.getSelectedItem().getValue());
	}

	private int width = 750;
	private int height = 100;

	/**
	 * Event listener lokal milik {@link DashboardStatistikJadwalMengajar}. Kelas ini menangani event untuk
	 * komponen induk dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DashboardStatistikJadwalMengajar} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String tahun_ajaran}, {@code Long
	 * sekolahId}, {@code Integer semester}, {@code Long matpel}; operasi lokal: {@code onEvent}(). Aturan bisnis
	 * bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DashboardStatistikJadwalMengajar
	 */
	public class MyEventListener implements EventListener {

		private String tahun_ajaran;
		private Long sekolahId;
		private Integer semester;
		private Long matpel;

		public MyEventListener(String tahun_ajaran, Integer semester, Long matpel, Long sekolahId) {
			this.tahun_ajaran = tahun_ajaran;
			this.semester = semester;
			this.matpel = matpel;
			this.sekolahId = sekolahId;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(JadwalPelajaran.class, new DataCriteriaWithColumn() {

						@Override
						public Object[] initCriteria(boolean order) {

							try {

								System.out.println("tahun_ajaran -> " + tahun_ajaran + ", semester -> " + semester
										+ ", sekolahId -> " + sekolahId + ", matpel -> " + matpel);

								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(JadwalPelajaran.class)
										.add(Restrictions.eq("tahunAjaran", tahun_ajaran))
										.add(semester.equals(-1) ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("semester", semester))
										.add(sekolahId.equals(-1L) ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("sekolah.id", sekolahId))
										.add(matpel.equals(-1L) ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("matapelajaran.id", matpel));

								return new Object[] { criteria,
										new String[] { "matapelajaran.nama", "guru.nama", "guru2.nama", "guru3.nama",
												"guru4.nama", "guru5.nama", "kelas.nama", "ruang.nama",
												"jamPelajaran.nama", "jamPelajaran2.nama", "jamPelajaran3.nama",
												"jamPelajaran4.nama", "jamPelajaran5.nama" } };

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

	@SuppressWarnings("unchecked")
	public static List<Object[]> ambilData(Yayasan yayasan, Sekolah sekolah, Siswa siswa, Guru g, String ta,
			Integer semester) {
		String sqlSiswa = "";
		Session session = HibernateUtil.currentNativeSession();
		if (siswa != null) {
			List<Long> kelasId = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.createAlias("kelasSiswa", "kelasSiswa").add(Restrictions.eq("kelasSiswa.tahunAjaran", ta))
					.add(Restrictions.eq("siswa", siswa)).setProjection(Projections.groupProperty("kelasSiswa.id"))
					.list();
			if (!kelasId.isEmpty()) {
				for (Long kId : kelasId) {
					sqlSiswa += sqlSiswa.isEmpty() ? kId : "," + kId;
				}
				sqlSiswa = " and c.kelas_id in (" + sqlSiswa + ") ";
			}

			if (sqlSiswa.trim().isEmpty()) {
				sqlSiswa = " and false ";
			}
		}

		String sqlGuru = "";
		if (g != null) {
			sqlGuru = " and (c.guru_id=" + g.getId() + " or c.guru2_id=" + g.getId() + " or c.guru3_id=" + g.getId()
					+ " or c.guru4_id=" + g.getId() + " or c.guru5_id=" + g.getId() + ") ";
		}

		String sqlSekolah = "";
		if (sekolah != null) {
			sqlSekolah = "and a.sekolah_id=" + sekolah.getId();
		}

		String sqlYayasan = "";
		if (yayasan != null) {
			sqlYayasan = "and a.yayasan_id=" + yayasan.getId();
		}

		String sql = "select\r\n" + "	b.id as sekolah_id,\r\n" + "	b.nama as sekolah,\r\n"
				+ "	a.nama_guru as guru,\r\n" + "	c.tahun_ajaran,\r\n" + "	c.semester,\r\n"
				+ "	d.nama as matapelajaran,\r\n"
				+ "	count(*) as jumlah_jadwal, d.id as matapelajaranid, a.id as guruid \r\n"
				+ "	from sekolah.jadwal_pelajaran c \r\n"
				+ "	inner join sekolah.matapelajaran d on (c.matapelajaran_id=d.id)\r\n"
				+ "	inner join sekolah.sekolah b on (c.sekolah_id=b.id)\r\n" + "	inner join sekolah.guru a on (\r\n"
				+ "	c.guru_id=a.id or c.guru2_id=a.id or c.guru3_id=a.id or c.guru4_id=a.id or c.guru5_id=a.id \r\n"
				+ "	)\r\n" + "	where c.semester=" + semester + " and a.aktif and c.tahun_ajaran='" + ta + "' "
				+ sqlSiswa + " " + sqlYayasan + " " + sqlSekolah + " " + sqlGuru + " \r\n"
				+ "	group by b.id,a.id,c.tahun_ajaran,c.semester,d.id\r\n"
				+ "	order by c.tahun_ajaran,c.semester,d.id";

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		List<Object[]> jurusans = Common.ambilSql(sql);

		return jurusans;
	}

	@SuppressWarnings("unchecked")
	public static List<JadwalPelajaran> ambilDataSemua(Siswa siswa, Guru g, String ta, Integer semester) {
		String sqlSiswa = "";
		Session session = HibernateUtil.currentNativeSession();
		if (siswa != null) {
			List<Long> kelasId = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.createAlias("kelasSiswa", "kelasSiswa").add(Restrictions.eq("kelasSiswa.tahunAjaran", ta))
					.add(Restrictions.eq("siswa", siswa)).setProjection(Projections.groupProperty("kelasSiswa.id"))
					.list();
			if (!kelasId.isEmpty()) {
				for (Long kId : kelasId) {
					sqlSiswa += sqlSiswa.isEmpty() ? kId : "," + kId;
				}
				sqlSiswa = "  this_.kelas_id in (" + sqlSiswa + ") ";
			}

			if (sqlSiswa.trim().isEmpty()) {
				sqlSiswa = " false ";
			}
		} else {
			sqlSiswa = "1=1";
		}

		String sqlGuru;
		if (g != null) {
			sqlGuru = "  (this_.guru_id=" + g.getId() + " or this_.guru2_id=" + g.getId() + " or this_.guru3_id="
					+ g.getId() + " or this_.guru4_id=" + g.getId() + " or this_.guru5_id=" + g.getId()
					+ " or this_.guru6_id=" + g.getId() + " or this_.guru7_id=" + g.getId() + " or this_.guru8_id="
					+ g.getId() + " or this_.guru9_id=" + g.getId() + " or this_.guru10_id=" + g.getId()
					+ " or this_.guru11_id=" + g.getId() + " or this_.guru12_id=" + g.getId() + ") ";
		} else {
			sqlGuru = "1=1";
		}

		System.out.println("sqlSiswa -> " + sqlSiswa);
		System.out.println("sqlGuru -> " + sqlGuru);

		List<JadwalPelajaran> jadwalPelajarans = session.createCriteria(JadwalPelajaran.class)
				.createAlias("matapelajaran", "matapelajaran").add(Restrictions.eq("semester", semester))
				.add(Restrictions.eq("tahunAjaran", ta)).add(Restrictions.sqlRestriction(sqlSiswa))
				.add(Restrictions.sqlRestriction(sqlGuru)).addOrder(Order.asc("matapelajaran.urutan")).list();

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		return jadwalPelajarans;
	}

	@SuppressWarnings({ "deprecation" })
	public void initChart(Component center, boolean tampilkanChart, Combobox tahunAjaran, Siswa siswa, Guru g,
			Integer semester) {
		Common.clear(center);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Guru");
		column.setParent(columns);

		MyColumnConfig columnAktif = new MyColumnConfig("Matapelajaran");
		columnAktif.setParent(columns);
		columnAktif.setWidth("30%");

		column = new MyColumnConfig("Jumlah");
		column.setParent(columns);
		column.setWidth(siswa == null ? "20%" : "0%");

		String ta = (String) tahunAjaran.getSelectedItem().getValue();

		Rows rows = new Rows();
		rows.setParent(grid);

		Integer total = 0;

		Yayasan yayasana = SekolahUtil.getYayasan();
		Sekolah sekolaha = SekolahUtil.getSekolah();

		List<Object[]> jurusans = DashboardStatistikJadwalMengajar.ambilData(yayasana, sekolaha, siswa, g, ta,
				semester);

		CategoryModel model = new SimpleCategoryModel();
		for (Object[] objects : jurusans) {
			Long sekolahId = ((Number) (objects[0] == null ? 0 : objects[0])).longValue();
			String sekolah = (objects[1] == null ? "" : objects[1]).toString();
			String guru = (objects[2] == null ? "" : objects[2]).toString();

			semester = ((Number) (objects[4] == null ? -1 : objects[4])).intValue();
			String matapelajaran = (objects[5] == null ? "" : objects[5]).toString();
			Integer jumlahJadwal = ((Number) (objects[6] == null ? -1 : objects[6])).intValue();
			Long matapelajaranid = ((Number) (objects[7] == null ? 0 : objects[7])).longValue();

			total += jumlahJadwal;

			String taa = ta + "/" + (semester.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			Vbox vbox = new Vbox();
			row.appendChild(vbox);
			vbox.appendChild(new MyLabelBoldAja(guru));
			vbox.appendChild(new MyLabelAgakKecil(sekolah));
			vbox.appendChild(new MyLabelAgakKecil(taa));
			row.appendChild(new Label(matapelajaran));
			A a = new A(Common.numberFormat.get().format(jumlahJadwal));
			a.addEventListener("onClick", new MyEventListener(ta, semester, matapelajaranid, sekolahId));

			row.appendChild(a);

			model.setValue(guru + " " + taa, matapelajaran, jumlahJadwal);
		}

		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total")));
		row.appendChild(new Label(""));

		A a = new A(Common.numberFormat.get().format(total));
		a.addEventListener("onClick", new MyEventListener(ta, -1, -1L, -1L));
		row.appendChild(a);

		if (tampilkanChart) {
			row = new Row();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "3");
			row.setAlign("center");

			mychart = null;
        mychart = DashboardModernHtmlUtil.createAnyChart(model, "Dasbor Statistik Jadwal Mengajar", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);

			setStyle("min-height:" + (330 + (40 * jurusans.size())) + "px");
		}

	}
}
