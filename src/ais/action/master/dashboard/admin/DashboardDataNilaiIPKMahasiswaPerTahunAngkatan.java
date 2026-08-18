package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vlayout;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h1>DashboardDataNilaiIPKMahasiswaPerTahunAngkatan &mdash; Data Nilai &amp; IPK Mahasiswa per Tahun
 * Angkatan</h1>
 *
 * <p>
 * Dasbor ini dipakai oleh staf akademik, dosen wali (Dosen PA), dan pimpinan program studi untuk
 * menyusun rekap <b>rincian nilai per semester</b> (IP Semester, IP Kumulatif, SKS semester, SKS
 * kumulatif) setiap mahasiswa, mulai dari semester pertama sampai semester yang dipilih, sekaligus
 * mengelompokkan capaian tersebut berdasarkan <b>tahun angkatan</b> (kohor masuk) mahasiswa. Pengguna
 * memilih kombinasi filter di panel atas (Fakultas, Program Studi, Tahun Akademik "sampai dengan",
 * Dosen PA, jenis semester Ganjil/Genap, rentang semester, angkatan, dan program), menekan tombol
 * <b>Proses</b>, dan sistem menyusun satu baris Excel per kombinasi mahasiswa-semester berisi identitas
 * mahasiswa serta IPS/IPK/SKS pada semester tersebut. Hasilnya bisa diunduh sebagai berkas Excel lewat
 * tombol <b>Download</b>.
 * </p>
 *
 * <h2>Alur teknis singkat</h2>
 * <p>
 * {@link #init()} membangun tata letak (form filter di region {@code North}, area hasil di region
 * {@code Center}) beserta event-listener kombo yang saling bergantung (memilih jenis semester mengisi
 * ulang pilihan "Semester" &amp; "Semester s.d", yang lalu menghitung ulang tahun angkatan secara
 * otomatis). {@link #initSpreadsheet()} adalah method inti yang dipanggil saat tombol <b>Proses</b>
 * ditekan: karena penyusunan rekap bisa memakan waktu lama (IPS/IPK dihitung ulang untuk tiap semester
 * dari 1 sampai semester yang dipilih, untuk setiap mahasiswa), proses berat ini dijalankan pada
 * {@link Thread} latar belakang terpisah agar antarmuka ZK tidak macet/hang menunggu. Selama proses
 * berjalan, {@code Common.displayLoadBar(...)} menampilkan indikator "sedang memproses" yang
 * di-<i>polling</i> lewat komponen {@link Label} tersembunyi; begitu label dikosongkan, berkas Excel
 * yang sudah selesai ditulis ke folder sementara otomatis ditampilkan sebagai tabel/grid ringan (lihat
 * {@code ais.ui.util.PratinjauXlsxHelper#gantiSpreadsheetDenganGrid}) alih-alih widget Excel penuh yang
 * berat di sisi peramban.
 * </p>
 *
 * <h2>Panel ringkasan visual (HTML/CSS, bukan JFreeChart)</h2>
 * <p>
 * Selain tabel mentah, dasbor ini juga menampilkan panel ringkasan berupa kartu KPI (total mahasiswa,
 * rata-rata IPK terakhir, jumlah angkatan, jumlah baris data), grafik garis tren rata-rata IPK per
 * tahun angkatan (inti dari nama dasbor ini), dan grafik batang sebaran rentang nilai IPK. Seluruh
 * angka pada panel ini dihitung <b>bersamaan</b> dengan penulisan sel Excel di dalam
 * {@link #initSpreadsheet()} (loop yang sama, tanpa kueri basis data tambahan) lalu dirender lewat
 * {@code ais.ui.util.HtmlChartHelper} setelah loop selesai, dibungkus percobaan/tangkapan tersendiri
 * supaya kegagalan menggambar grafik tidak pernah mengganggu penulisan Excel maupun tampilan grid.
 * </p>
 *
 * <h2>Ketahanan terhadap data tidak lengkap</h2>
 * <p>
 * Data mahasiswa pada praktiknya bisa tidak lengkap (program studi belum diisi, tahun angkatan kosong,
 * dsb.). Setiap mahasiswa &mdash; dan setiap baris semester di dalamnya &mdash; diproses di dalam blok
 * percobaan tersendiri: bila satu data bermasalah, kesalahannya dicatat lewat
 * {@code ais.common.ErrorAuditUtil#record} dan proses lanjut ke data berikutnya alih-alih menghentikan
 * seluruh rekap.
 * </p>
 *
 * <h2>Sesi Hibernate</h2>
 * <p>
 * Thread latar belakang mengambil sesi lewat {@code HibernateUtil.currentNativeSession()} (sesi native,
 * BUKAN sesi ber-scope permintaan/{@code currentSession()}), sehingga wajib ditutup secara eksplisit;
 * penutupan dilakukan tepat satu kali di blok {@code finally} agar sesi selalu tertutup baik proses
 * berhasil maupun gagal, dan tidak dipanggil berulang di banyak tempat.
 * </p>
 *
 * @author e-Campus UI Team
 */
public class DashboardDataNilaiIPKMahasiswaPerTahunAngkatan extends MyWindow {

	/**
	 *
	 */
	private static final long serialVersionUID = 790038368339375113L;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemesterMulai = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Intbox angkatan = new Intbox();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();
	private Center center = new Center();

	private File file;

	public DashboardDataNilaiIPKMahasiswaPerTahunAngkatan() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardDataNilaiIPKMahasiswaPerTahunAngkatan(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	/**
	 * Membangun seluruh tata letak dasbor: panel filter (region {@code North}) berisi kombo
	 * Fakultas/Prodi/Tahun Akademik "sampai dengan"/Dosen PA, kombo jenis semester (Ganjil/Genap) yang
	 * mengisi ulang pilihan "Semester"/"Semester s.d" secara dinamis, kotak angkatan &amp; program,
	 * serta area hasil (region {@code Center}) dan tombol Proses/Download pada toolbar. Perubahan pada
	 * kombo "Tahun Akademik" atau "Semester s.d" otomatis menghitung ulang tahun angkatan yang sesuai
	 * lewat listener {@code semesterEventListener}, sehingga pengguna tidak perlu mengisi angkatan
	 * secara manual. Method ini hanya menyiapkan komponen UI; proses pengambilan dan perhitungan data
	 * baru dijalankan saat tombol Proses ditekan, lihat {@link #initSpreadsheet()}.
	 */
	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		initFakultas();

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("200px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);
		Html deskripsiDasbor = new Html();
		deskripsiDasbor.setContent("<div style='font-size:13px;color:#475569;padding:2px 4px 8px;'>"
				+ "Menampilkan data IP Semester, IP Kumulatif, dan SKS setiap mahasiswa per semester, "
				+ "dikelompokkan berdasarkan tahun angkatan, sehingga perkembangan capaian akademik "
				+ "tiap angkatan mahasiswa mudah dibandingkan.</div>");
		deskripsiDasbor.setParent(row);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen PA"));
		row.appendChild(searchDosen);
		searchDosen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(1);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.setReadonly(true);

		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		semesterAbsensi.appendChild(comboitem);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(searchsemesterMulai);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(searchsemester);
		searchsemesterMulai.setCols(2);
		searchsemester.setCols(2);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatan);
		angkatan.setWidth("90%");

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		final EventListener semesterEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final String tahunAkademik = (String) (DashboardDataNilaiIPKMahasiswaPerTahunAngkatan.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardDataNilaiIPKMahasiswaPerTahunAngkatan.this.tahunAkademik.getSelectedItem()
										.getValue());
				final String semester = (String) (DashboardDataNilaiIPKMahasiswaPerTahunAngkatan.this.semesterAbsensi
						.getSelectedItem() == null ? Perkuliahan.GANJIL
								: DashboardDataNilaiIPKMahasiswaPerTahunAngkatan.this.semesterAbsensi.getSelectedItem()
										.getValue());

				final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? -1
						: searchsemester.getSelectedItem().getValue());

				if (tahunAkademik == null || semester == null) {
					return;
				}
				final Integer tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
				final Boolean semuasemester = searchsemester.getSelectedItem() == null;
				if (!semuasemester) {
					angkatan.setValue(tahunAngkatan);
				}
			}
		};

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);

				Common.clear(searchsemesterMulai);
				searchsemesterMulai.setSelectedItem(null);

				if (semesterAbsensi.getSelectedItem() == null) {
					return;
				}
				if (semesterAbsensi.getSelectedItem() != null && semesterAbsensi.getSelectedItem().getValue() != null) {
					Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
					if (genap) {
						for (int i : Common.genap) {
							if (i == 0)
								continue;
							org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemester.appendChild(comboitem);

							comboitem = new org.zkoss.zul.Comboitem();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemesterMulai.appendChild(comboitem);
						}
					} else {
						for (int i : Common.ganjil) {
							org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemester.appendChild(comboitem);

							comboitem = new org.zkoss.zul.Comboitem();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemesterMulai.appendChild(comboitem);
						}
					}
				} else {
					for (int i : new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 }) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);

						comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemesterMulai.appendChild(comboitem);
					}
				}

				semesterEventListener.onEvent(null);
			}
		};
		searchsemester.addEventListener("onChange", semesterEventListener);
		tahunAkademik.addEventListener("onChange", semesterEventListener);

		semesterAbsensi.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(row);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "REKAP_PENILAIAN.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardDataNilaiIPKMahasiswaPerTahunAngkatan.java:386");

				}
			}
		});
		print.setParent(toolbar);

	}

	/**
	 * Method inti dasbor ini: membaca seluruh filter yang dipilih pengguna, lalu menjalankan
	 * penyusunan rincian nilai per semester (IPS/IPK/SKS) pada {@link Thread} latar belakang terpisah
	 * (agar antarmuka tidak macet untuk data yang banyak), menuliskannya sebagai berkas Excel sekaligus
	 * mengumpulkan ringkasan angka (rata-rata IPK terakhir per tahun angkatan, sebaran rentang IPK)
	 * untuk panel grafik HTML/CSS yang dirender lewat {@link #renderChartRingkasan}. Region
	 * {@code Center} dibungkus satu {@link Vlayout} berisi dua anak: panel grafik ({@code chartPanel})
	 * dan wadah grid/Excel ({@code gridHost}) yang terpisah, sehingga saat
	 * {@code Common.displayLoadBar(...)} membersihkan dan mengisi ulang area hasil di akhir proses,
	 * panel grafik yang sudah tampil tidak ikut terhapus. Setiap mahasiswa &mdash; dan setiap baris
	 * semester di dalamnya &mdash; diproses dalam blok percobaan tersendiri: data yang bermasalah
	 * dicatat lalu dilewati, tidak menggagalkan seluruh rekap. Sesi Hibernate native ditutup satu kali
	 * di blok {@code finally}.
	 */
	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		Common.clear(center);
		final String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());

		final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());
		final Integer semesterMulai = (Integer) (searchsemesterMulai.getSelectedItem() == null ? -1
				: searchsemesterMulai.getSelectedItem().getValue());
		final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? -1
				: searchsemester.getSelectedItem().getValue());
		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");

		if (tahunAkademik == null) {
			return;
		}
		final Integer tahunAngkatan = angkatan.getValue();
		final int tahun = Integer.parseInt(StringUtils.split(tahunAkademik, "/")[0]);

		System.out.println("init spreadsheet running => tahun = " + tahun);

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();

		// Center hanya boleh 1 anak -> bungkus dlm Vlayout: panel grafik ringkasan di atas, wadah
		// grid/Excel di bawah lewat Div terpisah (gridHost) supaya saat Common.displayLoadBar(...)
		// membersihkan & mengisi ulang area hasil, panel grafik yang sudah tampil tidak ikut terhapus.
		final Vlayout centerBox = new Vlayout();
		centerBox.setWidth("100%");
		centerBox.setHeight("100%");
		centerBox.setStyle("overflow:auto;");
		centerBox.setParent(center);

		final Html chartPanel = new Html();
		chartPanel.setParent(centerBox);

		final Div gridHost = new Div();
		gridHost.setStyle("width:100%;");
		gridHost.setParent(centerBox);

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, gridHost, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				System.out.println("tahunAkademik = " + tahunAkademik);

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("PENILAIAN");
				sheet.setDefaultColumnWidth(20);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("NAMA");
				rowhead.createCell(2).setCellValue("Fakultas".toUpperCase());
				rowhead.createCell(3).setCellValue("Jurusan".toUpperCase());
				rowhead.createCell(4).setCellValue("PROGRAM");
				rowhead.createCell(5).setCellValue("TAHUN ANGKATAN");
				rowhead.createCell(6).setCellValue("TAHUN AKADEMIK");
				rowhead.createCell(7).setCellValue("SEMESTER");
				rowhead.createCell(8).setCellValue("IP SEMESTER");
				rowhead.createCell(9).setCellValue("IP KUMULATIF");
				rowhead.createCell(10).setCellValue("SKS SEMESTER");
				rowhead.createCell(11).setCellValue("SKS KUMULATIF");

				Session session = HibernateUtil.currentNativeSession();

				List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(dosen != null ? Restrictions.eq("dosen", dosen.getId())
								: Restrictions.sqlRestriction("1=1"))

						.createAlias("jurusan", "jurusan").addOrder(Order.asc("jurusan"))
						.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))

						.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan.fakultas", fakultas))

						.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))

						.add(program == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("program", program))

						.add(tahunAngkatan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunangkatan", tahunAngkatan))

						.list();

				int size = mahasiswas.size();

				int rowIndex = 1;
				int index = 1;

				// Akumulasi ringkasan utk panel grafik (dihitung BERSAMAAN dgn penulisan sel Excel di
				// bawah, tanpa kueri tambahan) -> dirender lewat renderChartRingkasan setelah loop selesai.
				final Map<Integer, double[]> ipkPerAngkatan = new TreeMap<Integer, double[]>();
				final int[] sebaranIpk = new int[5];
				double akumulasiIpkTerakhir = 0;
				int jumlahMahasiswaValid = 0;

				for (Mahasiswa mahasiswa : mahasiswas) {
					try {
						label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
								+ Common.numberFormat.get().format(index * 100.0 / size) + " %)");
						index++;

						List<String[]> datas = Common.generateSemestersForGrid(mahasiswa, 0,
								semesterKe == -1 ? mahasiswa.currentSemester() : semesterKe,
								Perkuliahan.SEMESTER_PENDEK);

						// IPK terakhir (semester tervalid paling akhir dlm rentang tahun akademik yg
						// dipilih) mahasiswa ini -> dipakai utk agregasi per tahun angkatan di bawah.
						Double ipkTerakhirMhs = null;

						for (String[] data : datas) {

							try {
								Integer smt;
								try {
									smt = Integer.parseInt(data[1].split(",")[0]);
								} catch (Exception e) {
									smt = 0;
								}
								final Integer semester = smt;

								if (semesterMulai != null && semesterMulai > semester) {
									continue;
								}

								final String tahunAjaran = data[0];

								int tahun = Integer.parseInt(tahunAjaran.split("/")[0]);
								int tahunDipilih = Integer.parseInt(tahunAkademik.split("/")[0]);

								if (tahun <= tahunDipilih) {

									// Baris di-"reservasi" (index dimajukan) di blok finally supaya bila ada
									// data yang gagal ditulis di tengah jalan (mis. jurusan mahasiswa belum
									// terisi), baris berikutnya yang berhasil tidak menimpa baris ini.
									try {
										XSSFRow row = sheet.createRow(rowIndex);
										XSSFCell cell = row.createCell(0);
										cell.setCellValue(mahasiswa.getNim());

										cell = row.createCell(1);
										cell.setCellValue(mahasiswa.getNama());

										Jurusan jurusanMhs = mahasiswa.getJurusan();
										String namaFakultas = jurusanMhs == null || jurusanMhs.getFakultas() == null
												? ""
												: jurusanMhs.getFakultas().getNama();
										String namaJurusan = jurusanMhs == null ? "" : jurusanMhs.getNama();

										cell = row.createCell(2);
										cell.setCellValue(namaFakultas);
										cell = row.createCell(3);
										cell.setCellValue(namaJurusan);

										cell = row.createCell(4);
										cell.setCellValue(mahasiswa.getProgram());

										cell = row.createCell(5);
										cell.setCellValue(mahasiswa.getTahunangkatan());

										cell = row.createCell(6);
										cell.setCellValue(tahunAjaran);

										cell = row.createCell(7);
										cell.setCellValue(semester);

										KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester,
												null, null);

										double ipsAman = angkaAman(krsMahasiswa.getIps());
										double ipkAman = angkaAman(krsMahasiswa.getIpk());

										cell = row.createCell(8);
										cell.setCellValue(ipsAman);

										cell = row.createCell(9);
										cell.setCellValue(ipkAman);

										cell = row.createCell(10);
										cell.setCellValue(krsMahasiswa.getSksYangDiambil());

										cell = row.createCell(11);
										cell.setCellValue(krsMahasiswa.getSksk());

										ipkTerakhirMhs = ipkAman;
									} finally {
										rowIndex++;
									}
								}
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

						// -- Akumulasi ringkasan panel grafik: rata-rata IPK terakhir per tahun angkatan
						// & sebaran rentang IPK -- pakai IPK terakhir yg berhasil ditulis utk mahasiswa
						// ini (bila ada). --
						if (ipkTerakhirMhs != null) {
							double ipkAman = angkaAman(ipkTerakhirMhs);
							akumulasiIpkTerakhir += ipkAman;
							jumlahMahasiswaValid++;

							int idxSebaran = ipkAman < 2.00 ? 0 : ipkAman < 2.50 ? 1 : ipkAman < 3.00 ? 2
									: ipkAman < 3.50 ? 3 : 4;
							sebaranIpk[idxSebaran]++;

							Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
							if (tahunAngkatanMhs != null) {
								double[] agg = ipkPerAngkatan.get(tahunAngkatanMhs);
								if (agg == null) {
									agg = new double[2];
									ipkPerAngkatan.put(tahunAngkatanMhs, agg);
								}
								agg[0] += ipkAman;
								agg[1] += 1;
							}
						}

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

				Common.setStyled(sheet);
				sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					Common.tampilErrorJikaAdmin(e);
				}

				// -- Render panel grafik (HTML/CSS, BUKAN JFreeChart) dari ringkasan yang sudah
				// dihitung di loop di atas. Kegagalan menggambar grafik tidak boleh mengganggu
				// Excel/grid yang sudah berhasil ditulis. --
				try {
					renderChartRingkasan(chartPanel, jumlahMahasiswaValid, akumulasiIpkTerakhir, ipkPerAngkatan,
							sebaranIpk, rowIndex - 1);
				} catch (Exception exChart) { ais.common.ErrorAuditUtil.record(exChart,
						"auto-guard(chart) src/ais/action/master/dashboard/admin/DashboardDataNilaiIPKMahasiswaPerTahunAngkatan.java"); }

				mahasiswas.clear();
				label.setValue("");

				} catch (Exception exProses) {
					// Kegagalan proses (mis. query bermasalah) tidak boleh membuat indikator "sedang
					// memproses" menggantung selamanya di sisi pengguna.
					ais.common.ErrorAuditUtil.record(exProses,
							"src/ais/action/master/dashboard/admin/DashboardDataNilaiIPKMahasiswaPerTahunAngkatan.java:initSpreadsheet-thread");
					Common.tampilErrorJikaAdmin(exProses);
					label.setValue("");
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	/** Mengembalikan nilai {@code double} aman dari {@link Double} yang mungkin {@code null} (mis. IPS/IPK
	 * mahasiswa yang belum pernah dihitung), agar tidak terjadi {@code NullPointerException} saat unboxing. */
	private static double angkaAman(Double nilai) {
		return nilai == null ? 0.0 : nilai.doubleValue();
	}

	/**
	 * Merender panel ringkasan visual dasbor ini sepenuhnya memakai {@code ais.ui.util.HtmlChartHelper}
	 * (HTML/CSS, bukan JFreeChart): kartu KPI (total mahasiswa, rata-rata IPK terakhir, jumlah tahun
	 * angkatan, jumlah baris data semester), grafik garis tren rata-rata IPK terakhir per tahun angkatan
	 * (inti dari nama dasbor ini, hanya digambar bila datanya mencakup minimal 2 tahun angkatan agar
	 * bentuk trennya bermakna), serta grafik batang sebaran rentang nilai IPK seluruh mahasiswa yang
	 * diproses. Seluruh parameter berasal dari akumulasi yang sudah dihitung selagi menulis sel Excel di
	 * {@link #initSpreadsheet()}, sehingga tidak ada kueri basis data tambahan yang dijalankan hanya
	 * untuk kebutuhan grafik.
	 *
	 * @param chartPanel            komponen tampilan tujuan.
	 * @param jumlahMahasiswaValid  jumlah mahasiswa yang berhasil diproses (memiliki minimal satu baris
	 *                              IPK yang berhasil ditulis).
	 * @param akumulasiIpkTerakhir  jumlah (bukan rata-rata) IPK terakhir seluruh mahasiswa yang berhasil
	 *                              diproses.
	 * @param ipkPerAngkatan        peta tahun angkatan -&gt; {jumlah IPK, jumlah mahasiswa}, terurut naik
	 *                              berdasarkan tahun angkatan (dipakai untuk grafik tren).
	 * @param sebaranIpk            jumlah mahasiswa per rentang IPK: [&lt;2.00, 2.00-2.49, 2.50-2.99,
	 *                              3.00-3.49, 3.50-4.00].
	 * @param jumlahBarisData       jumlah baris rincian per semester yang berhasil ditulis ke Excel
	 *                              (satu mahasiswa bisa menyumbang lebih dari satu baris).
	 */
	private static void renderChartRingkasan(Html chartPanel, int jumlahMahasiswaValid,
			double akumulasiIpkTerakhir, Map<Integer, double[]> ipkPerAngkatan, int[] sebaranIpk,
			int jumlahBarisData) {

		double rataIpk = jumlahMahasiswaValid == 0 ? 0 : akumulasiIpkTerakhir / jumlahMahasiswaValid;

		StringBuilder html = new StringBuilder(4096);
		html.append("<div style='padding:10px 12px 4px;display:flex;flex-direction:column;gap:14px;'>");

		html.append(ais.ui.util.HtmlChartHelper.kpiCards(
				new String[] { "Total Mahasiswa", "Rata-rata IPK Terakhir", "Jumlah Tahun Angkatan",
						"Jumlah Baris Data Semester" },
				new String[] { String.valueOf(jumlahMahasiswaValid), Common.numberFormat.get().format(rataIpk),
						String.valueOf(ipkPerAngkatan.size()), String.valueOf(jumlahBarisData) },
				new String[] { "mahasiswa berhasil diproses", "skala 0.00 - 4.00",
						"kohor angkatan pada rekap ini", "baris rincian per semester pada rekap ini" },
				null, null, new String[] { "#2563eb", "#16a34a", "#7c3aed", "#0891b2" }));

		if (ipkPerAngkatan.size() >= 2) {
			String[] kategoriAngkatan = new String[ipkPerAngkatan.size()];
			double[] rataIpkAngkatan = new double[ipkPerAngkatan.size()];
			int i = 0;
			for (Map.Entry<Integer, double[]> entri : ipkPerAngkatan.entrySet()) {
				double[] agg = entri.getValue();
				kategoriAngkatan[i] = String.valueOf(entri.getKey());
				rataIpkAngkatan[i] = agg[1] == 0 ? 0 : agg[0] / agg[1];
				i++;
			}
			html.append(ais.ui.util.HtmlChartHelper.lineMulti("Tren Rata-rata IPK per Tahun Angkatan",
					"Menunjukkan naik-turunnya rata-rata IPK terakhir mahasiswa dari satu angkatan masuk ke "
							+ "angkatan berikutnya, sehingga terlihat apakah capaian akademik antar angkatan "
							+ "cenderung membaik atau menurun.",
					kategoriAngkatan, new String[] { "Rata-rata IPK" }, new double[][] { rataIpkAngkatan },
					new String[] { "#16a34a" }));
		}

		html.append(ais.ui.util.HtmlChartHelper.barHorizontal("Sebaran Nilai IPK Mahasiswa",
				"Menunjukkan berapa banyak baris data mahasiswa berada pada tiap rentang nilai IPK, dari "
						+ "yang paling rendah sampai paling tinggi, sehingga sebaran capaian akademik "
						+ "terlihat sekilas.",
				new String[] { "< 2.00", "2.00 - 2.49", "2.50 - 2.99", "3.00 - 3.49", "3.50 - 4.00" },
				new double[] { sebaranIpk[0], sebaranIpk[1], sebaranIpk[2], sebaranIpk[3], sebaranIpk[4] },
				"#2563eb"));

		html.append("</div>");
		chartPanel.setContent(html.toString());
	}
}
