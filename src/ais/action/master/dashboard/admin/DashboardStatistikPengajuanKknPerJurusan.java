package ais.action.master.dashboard.admin;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.CategoryModel;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Kkn;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.kkn.KelompokKkn;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Komponen dashboard khusus untuk dashboard statistik pengajuan kkn per jurusan. Kelas ini memilih
 * variasi data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas
 * induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code org.zkoss.zul.Html mychart}, {@code Div
 * center}, {@code Combobox searchsemester}, {@code Combobox tahunAkademik}, {@code Combobox angkatan}, {@code
 * Combobox angkatansd}, {@code AmbilDataDosenBanbox dosen}, {@code String contents}; inisialisasi/lifecycle
 * ({@code reinit()}, {@code initFakultas()}, {@code init()}, {@code initChart()}); konfigurasi constructor:
 * {@code tampilRinci}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardStatistikPengajuanKknPerJurusan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;

	private org.zkoss.zul.Html mychart;
	private Div center;

	private Combobox searchsemester = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox angkatan = new Combobox();
	private Combobox angkatansd = new Combobox();
	private AmbilDataDosenBanbox dosen = new AmbilDataDosenBanbox();

	public static String[] contents = new String[] { "mahasiswa.nim", "mahasiswa.nama", "mahasiswa.jurusan.nama",
			"kelompokKkn.nama_kelompok", "kelompokKkn.alamat", "kelompokKkn.tanggal_mulai",
			"kelompokKkn.tanggal_selesai", "hasil", "keterangan", "kelompokKkn.dosen_pembimbing1.nama",
			"kelompokKkn.dosen_pembimbing2.nama", "kelompokKkn.dosen_pembimbing3.nama",
			"kelompokKkn.dosen_pembimbing4.nama", "kelompokKkn.dosen_pembimbing5.nama", "totalNilai", "nilaiHuruf",
			"totalIP", "diterima" };

	public DashboardStatistikPengajuanKknPerJurusan() {
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

	private Combobox searchjenis;

	public DashboardStatistikPengajuanKknPerJurusan(int width, int height) throws Exception {
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

	public DashboardStatistikPengajuanKknPerJurusan(String title, String border, boolean closable) {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	private void initFakultas() {

	}

	public class MyEventListener implements EventListener {

		private Boolean diterima;
		private Long jurusanId;

		public MyEventListener(Boolean diterima, Long jurusanId) {
			this.diterima = diterima;
			this.jurusanId = jurusanId;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(MahasiswaDapatKelompokKkn.class, new DataCriteriaWithColumn() {

						@Override
						public Object[] initCriteria(boolean order) {

							try {
								PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(MahasiswaDapatKelompokKkn.class)
										.add(diterima == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("diterima", diterima))
										.createAlias("kelompokKkn", "kelompokKkn").createAlias("kelompokKkn.kkn", "kkn")
										.createAlias("mahasiswa", "mahasiswa")
										.createAlias("mahasiswa.jurusan", "jurusan")
										.add(jurusanId == null || jurusanId.equals(-1L)
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("jurusan.id", jurusanId))
										.createAlias("jurusan.fakultas", "fakultas")
										.add(perguruanTinggi == null || perguruanTinggi.getId() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi))
										.add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"),
												Restrictions.eq("mahasiswa.aktif", true)))
										.add(Restrictions.between("mahasiswa.tahunangkatan",
												angkatan.getSelectedItem().getValue(),
												angkatansd.getSelectedItem().getValue()))
										.add(Restrictions.eq("kkn.tahunAkademik",
												tahunAkademik.getSelectedItem().getValue()))
										.add(Restrictions.eq("kkn.semester",
												searchsemester.getSelectedItem().getValue()));

								return new Object[] { criteria, contents };

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

	private void init() {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center.
		 * Kartu Saringan di atas, kartu Isi (center) di bawah. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih tahun akademik, semester, rentang angkatan, dan pembimbing untuk menyaring data yang ditampilkan.",
				"Statistik Pengajuan KKN per Program Studi",
				"Jumlah pengajuan KKN yang sudah dan belum disetujui di tiap program studi, beserta grafiknya.");
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

		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		searchsemester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchsemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchsemester.appendChild(comboitem);
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");
		Common.selectComboItem(searchsemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");
		searchsemester.setReadonly(true);
		searchsemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		angkatan = Common.generateTahunAngkatan(angkatan, calendar.get(Calendar.YEAR) - 10);
		angkatan.setReadonly(true);
		hbox.appendChild(angkatan);
		angkatan.setCols(2);
		angkatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		angkatansd = Common.generateTahunAngkatan(angkatansd, calendar.get(Calendar.YEAR));
		angkatansd.setReadonly(true);
		hbox.appendChild(angkatansd);
		angkatansd.setCols(2);
		angkatansd.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(searchjenis = new Combobox());
		searchjenis.setWidth("90%");
		searchjenis.setReadonly(true);
		searchjenis.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row.appendChild(dosen);
		dosen.setWidth("90%");
		dosen.setReadonly(true);
		dosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		EventListener taEventListener = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				List<Kkn> kkns = HibernateUtil.currentSession().createCriteria(Kkn.class)
						.add(Restrictions.eq("tahunAkademik", tahunAkademik.getSelectedItem().getValue()))
						.add(Restrictions.eq("semester", searchsemester.getSelectedItem().getValue())).list();

				Common.insertComboDanSemua(searchjenis, new String[] { "nama" }, "nama", KelompokKkn.class,
						"=Nama Kelompok=",
						kkns.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("kkn", kkns));
			}
		};

		tahunAkademik.addEventListener("onChange", taEventListener);
		searchsemester.addEventListener("onChange", taEventListener);
		try {
			taEventListener.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardStatistikPengajuanKknPerJurusan.java:317");
		}

		if (tampilRinci) {

			hbox = new Hbox();
			row.appendChild(hbox);

			MyButtonConfig myButtonConfig = new MyButtonConfig("Rinci", "/img/12123-eyes-icon.png");
			myButtonConfig.setParent(hbox);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					DashboardKknMahasiswa laporan = new DashboardKknMahasiswa();
					laporan.setHeight("99%");
					laporan.setWidth("99%");
					laporan.setTitle("Rekap KKN");
					laporan.setClosable(true);
					laporan.setBorder("none");

					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
					laporan.onModal();
				}
			});

			myButtonConfig = new MyButtonConfig("Data", "/img/12123-eyes-icon.png");
			myButtonConfig.setParent(hbox);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					MyWindow laporan = new MyWindow();
					laporan.setHeight("99%");
					laporan.setWidth("99%");
					laporan.setTitle("Data Disetujui");
					laporan.setClosable(true);
					laporan.setBorder("none");

					Borderlayout borderlayout = new Borderlayout();
					laporan.appendChild(borderlayout);

					Center center = new Center();
					ais.ui.util.ZkCompat.setFlex(center, true);
					center.setParent(borderlayout);

					center.appendChild(new Iframe("/pages/master/kkn/kelompok_kkn.zul"));
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
					laporan.onModal();
				}
			});
		}

	}

	private int width = 750;
	private int height = 100;

	@SuppressWarnings({ "deprecation" })
	private void initChart() {
		Common.clear(center);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Prodi");
		column.setParent(columns);
		column.setWidth("30%");

		MyColumnConfig columnAktif = new MyColumnConfig("Telah Disetujui");
		columnAktif.setParent(columns);
		MyColumnConfig columnCuti = new MyColumnConfig("Belum Disetujui");
		columnCuti.setParent(columns);
		column = new MyColumnConfig("Total");
		column.setParent(columns);

		KelompokKkn kelompokKkn = (KelompokKkn) (searchjenis.getSelectedItem() == null ? null
				: searchjenis.getSelectedItem().getValue());

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Dosen dosen = (Dosen) this.dosen.getAttribute("dosen");

		String sql = "select sum(case when a1.diterima then 1 else 0 end) as telah_sidang,  "
				+ " sum(case when a1.diterima then 0 else 1 end) as belum_sidang,   b.nama as jurusan, b.id as jurusan_id "
				+ " from mahasiswa a inner join mahasiswa_dapat_kelompok_kelompok_kkn a1 on (a.id = a1.mahasiswa)  "
				+ " inner join jurusan b on (a.jurusan = b.id  )  left join fakultas c on (c.id = b.fakultas) "
				+ " inner join kelompok_kkn d on (a1.kelompok_kkn = d.id  ) " + " inner join kkn e on (d.kkn = e.id  ) "
				+ " where (a.aktif or a.aktif is null)  "

				+ (dosen == null ? ""
						: " and (d.dosen_pembimbing1=" + dosen.getId() + " or d.dosen_pembimbing2=" + dosen.getId()
								+ " or d.dosen_pembimbing3=" + dosen.getId() + " or d.dosen_pembimbing4="
								+ dosen.getId() + " or d.dosen_pembimbing5=" + dosen.getId() + ")")

				+ (perguruanTinggi == null || perguruanTinggi.getId() == null ? ""
						: " and c.perguruan_tinggi=" + perguruanTinggi.getId())
				+ (kelompokKkn == null ? "" : " and a1.kelompok_kkn=" + kelompokKkn.getId())
				+ " and b.aktif and c.aktif and e.tahunakademik='" + tahunAkademik.getSelectedItem().getValue()
				+ "' and e.semester='" + searchsemester.getSelectedItem().getValue() + "' "
				+ " and a.tahunangkatan between " + angkatan.getSelectedItem().getValue() + " and "
				+ angkatansd.getSelectedItem().getValue() + " group by b.id order by b.nama";

		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);

		Rows rows = new Rows();
		rows.setParent(grid);

		Double aktifTotal = 0.0;
		Double cutiTotal = 0.0;
		Double semuaTotal = 0.0;

		CategoryModel model = new SimpleCategoryModel();
		for (Object[] objects : jurusans) {

			Double Pengajuan = ((Number) (objects[0] == null ? 0.0 : objects[0])).doubleValue();
			Double Disetujui = ((Number) (objects[1] == null ? 0.0 : objects[1])).doubleValue();

			String jurusan = (objects[2] == null ? "" : objects[2]).toString();
			Long jurusanId = ((Number) (objects[3] == null ? -1L : objects[3])).longValue();
			Double total = Pengajuan + Disetujui;

			aktifTotal += Pengajuan;
			cutiTotal += Disetujui;
			semuaTotal += total;

			MyFormRow row = new MyFormRow();
		row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(jurusan));

			A a = new A(Common.numberFormat.get().format(Pengajuan));
			a.addEventListener("onClick", new MyEventListener(true, jurusanId));
			row.appendChild(a);

			a = new A(Common.numberFormat.get().format(Disetujui));
			a.addEventListener("onClick", new MyEventListener(false, jurusanId));
			row.appendChild(a);

			a = new A(Common.numberFormat.get().format(total));
			a.addEventListener("onClick", new MyEventListener(null, jurusanId));
			row.appendChild(a);

			model.setValue(jurusan, "Telah Disetujui", Pengajuan);
			model.setValue(jurusan, "Belum Disetujui", Disetujui);

		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total")));

		A a = new A(Common.numberFormat.get().format(aktifTotal));
		a.addEventListener("onClick", new MyEventListener(true, null));
		row.appendChild(a);

		a = new A(Common.numberFormat.get().format(cutiTotal));
		a.addEventListener("onClick", new MyEventListener(false, null));
		row.appendChild(a);

		a = new A(Common.numberFormat.get().format(semuaTotal));
		a.addEventListener("onClick", new MyEventListener(null, null));
		row.appendChild(a);

		if (aktifTotal < 0.01) {
			columnAktif.setWidth("0px");
			for (Object[] objects : jurusans) {
				String jurusan = (objects[2] == null ? "" : objects[2]).toString();
				model.removeValue(jurusan, "Telah Disetujui");
			}
		}
		if (cutiTotal < 0.01) {
			columnCuti.setWidth("0px");
			for (Object[] objects : jurusans) {
				String jurusan = (objects[2] == null ? "" : objects[2]).toString();
				model.removeValue(jurusan, "Belum Disetujui");
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "4");

		mychart = null;
        mychart = DashboardModernHtmlUtil.createAnyChart(model, "Dasbor Statistik Pengajuan Kkn Per Jurusan", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);

		setStyle("min-height:" + (330 + (40 * jurusans.size())) + "px");

	}
}
