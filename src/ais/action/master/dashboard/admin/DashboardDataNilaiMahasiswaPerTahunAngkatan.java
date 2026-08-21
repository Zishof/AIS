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
 * <h1>DashboardDataNilaiMahasiswaPerTahunAngkatan &mdash; Rekap IP/IPK Mahasiswa per Tahun Angkatan</h1>
 *
 * <p>
 * Dasbor ini melayani kebutuhan staf akademik, dosen wali (Dosen PA), dan pimpinan program studi untuk
 * membandingkan capaian akademik mahasiswa <b>antar tahun angkatan</b> (cohort masuk) dalam satu tabel
 * rekap, tanpa harus membuka data KRS/nilai satu per satu. Pengguna memilih kombinasi filter pada panel
 * atas (Fakultas, Program Studi, Tahun Akademik, Dosen PA, jenis semester Ganjil/Genap, semester ke
 * berapa, angkatan, dan program), menekan tombol <b>Proses</b>, dan sistem menyusun satu baris per
 * mahasiswa berisi NIM, nama, fakultas, program studi, program, tahun angkatan, tahun akademik, semester
 * berjalan, IP semester ganjil, IP semester genap, IP rata-rata (rata-rata ganjil &amp; genap), serta
 * IPK kumulatif. Hasilnya dapat diunduh sebagai berkas Excel lewat tombol <b>Download</b>.
 * </p>
 *
 * <h2>Alur teknis singkat</h2>
 * <p>
 * {@link #init()} membangun seluruh tata letak (form filter di region {@code North}, area hasil di
 * region {@code Center}) beserta event-listener kombo yang saling bergantung (memilih jenis semester
 * mengisi ulang pilihan "Semester ke", yang lalu menghitung ulang tahun angkatan secara otomatis lewat
 * listener {@code semesterEventListener}). {@link #initSpreadsheet()} adalah method inti yang dipanggil
 * saat tombol <b>Proses</b> ditekan: karena penyusunan rekap menghitung ulang IP setiap mahasiswa
 * (memanggil {@code Mahasiswa#hitungIPKSemester} dan menyinkronkan KRS lewat
 * {@code Common#singkronkanKrsMahasiswa}) yang bisa memakan waktu lama untuk data yang banyak, proses
 * berat ini dijalankan pada {@link Thread} latar belakang terpisah agar antarmuka ZK tidak macet/hang
 * menunggu. Selama proses berjalan, {@code Common.displayLoadBar(...)} menampilkan indikator "sedang
 * memproses" yang di-<i>polling</i> lewat komponen {@link Label} tersembunyi; begitu label dikosongkan,
 * berkas Excel yang sudah selesai ditulis ke folder sementara otomatis ditampilkan sebagai tabel/grid
 * ringan alih-alih widget Excel penuh yang berat di sisi peramban.
 * </p>
 *
 * <h2>Panel ringkasan visual (HTML/CSS, bukan JFreeChart)</h2>
 * <p>
 * Selain tabel mentah, dasbor ini menampilkan panel ringkasan berupa kartu KPI (total mahasiswa,
 * rata-rata IPK, rata-rata IP semester, jumlah angkatan tercakup), grafik garis tren rata-rata IPK dan
 * IP semester per tahun angkatan (grafik utama dasbor ini karena data memang dikelompokkan per
 * angkatan), grafik donat komposisi jumlah mahasiswa per angkatan, dan grafik batang sebaran rentang
 * nilai IPK. Seluruh angka pada panel ini dihitung <b>bersamaan</b> dengan penulisan sel Excel di dalam
 * {@link #initSpreadsheet()} (loop yang sama, tanpa kueri basis data tambahan) lalu dirender lewat
 * {@code ais.ui.util.HtmlChartHelper} setelah loop selesai, dibungkus percobaan/tangkapan tersendiri
 * supaya kegagalan menggambar grafik tidak pernah mengganggu penulisan Excel maupun tampilan grid.
 * </p>
 *
 * <h2>Ketahanan terhadap data tidak lengkap</h2>
 * <p>
 * Data mahasiswa pada praktiknya bisa tidak lengkap (program studi/fakultas belum diisi, tahun angkatan
 * kosong, riwayat KRS belum tersinkron, dsb.). Setiap baris mahasiswa diproses di dalam blok percobaan
 * tersendiri: bila satu data bermasalah, kesalahannya dicatat lewat
 * {@code ais.common.ErrorAuditUtil#record} dan proses lanjut ke mahasiswa berikutnya alih-alih
 * menghentikan seluruh rekap (indeks baris tetap dinaikkan di blok {@code finally} agar baris berikutnya
 * tidak tertimpa). Nilai IP/IPK yang berasal dari kolom {@code Double} yang mungkin {@code null} (belum
 * pernah dihitung) diamankan lewat {@link #angkaAman(Double)} sebelum dipakai dalam operasi aritmetika
 * atau ditulis ke sel Excel, sehingga tidak terjadi {@code NullPointerException} saat auto-unboxing.
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
public class DashboardDataNilaiMahasiswaPerTahunAngkatan extends MyWindow {

	/**
	 *
	 */
	private static final long serialVersionUID = 790038368339375113L;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Intbox angkatan = new Intbox();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();
	private Center center = new Center();

	private File file;

	public DashboardDataNilaiMahasiswaPerTahunAngkatan() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardDataNilaiMahasiswaPerTahunAngkatan(String title, String border, boolean closable) {
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
	 * Membangun seluruh tata letak dasbor: panel filter (region {@code North}) berisi deskripsi singkat
	 * dasbor, kombo Fakultas/Prodi/Tahun Akademik/Dosen PA, kombo jenis semester (Ganjil/Genap) yang
	 * mengisi ulang pilihan "Semester ke" secara dinamis lalu menghitung ulang tahun angkatan yang
	 * sesuai lewat listener {@code semesterEventListener} (sehingga pengguna tidak perlu mengisi
	 * angkatan secara manual), kotak Angkatan dan Program, serta area hasil (region {@code Center}) dan
	 * tombol Proses/Download pada toolbar. Method ini hanya menyiapkan komponen UI; proses pengambilan
	 * dan perhitungan data baru dijalankan saat tombol Proses ditekan, lihat {@link #initSpreadsheet()}.
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
		/* FIX 21-08-2026: tinggi panel filter kurang 52px sehingga baris toolbar
		 * (Proses/Download) terpotong di bagian bawah. Ditambah satu tinggi baris
		 * toolbar ZK. Autoscroll tetap aktif sebagai pengaman bila isi filter
		 * bertambah di kemudian hari. */
		north.setHeight("272px");
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
				+ "Menampilkan rekap IP semester (ganjil/genap), IP rata-rata, dan IPK setiap mahasiswa yang "
				+ "dikelompokkan per tahun angkatan, lengkap dengan ringkasan tren capaian akademik antar "
				+ "angkatan sehingga performa tiap angkatan mudah dibandingkan.</div>");
		deskripsiDasbor.setParent(row);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen PA"));
		row.appendChild(searchDosen);
		searchDosen.setWidth("90%");
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

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester ke"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");

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
				final String tahunAkademik = (String) (DashboardDataNilaiMahasiswaPerTahunAngkatan.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardDataNilaiMahasiswaPerTahunAngkatan.this.tahunAkademik.getSelectedItem()
										.getValue());
				final String semester = (String) (DashboardDataNilaiMahasiswaPerTahunAngkatan.this.semesterAbsensi
						.getSelectedItem() == null ? Perkuliahan.GANJIL
								: DashboardDataNilaiMahasiswaPerTahunAngkatan.this.semesterAbsensi.getSelectedItem()
										.getValue());

				final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? -1
						: searchsemester.getSelectedItem().getValue());

				if (tahunAkademik == null) {
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
				if (semesterAbsensi.getSelectedItem() == null) {
					return;
				}
				Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
				if (genap) {
					for (int i : Common.genap) {
						if (i == 0)
							continue;
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					for (int i : Common.ganjil) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
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
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardDataNilaiMahasiswaPerTahunAngkatan.java:download");

				}
			}
		});
		print.setParent(toolbar);

	}

	/**
	 * Method inti dasbor ini: membaca seluruh filter yang dipilih pengguna, lalu menjalankan penyusunan
	 * rekap IP/IPK mahasiswa per tahun angkatan pada {@link Thread} latar belakang terpisah (agar
	 * antarmuka tidak macet untuk data yang banyak), menuliskannya sebagai berkas Excel sekaligus
	 * mengumpulkan ringkasan angka (rata-rata IPK/IP per angkatan, jumlah mahasiswa per angkatan,
	 * sebaran rentang IPK) untuk panel grafik HTML/CSS yang dirender lewat
	 * {@link #renderChartRingkasan}. Region {@code Center} dibungkus satu {@link Vlayout} berisi dua
	 * anak: panel grafik ({@code chartPanel}) dan wadah grid/Excel ({@code gridHost}) yang terpisah,
	 * sehingga saat {@code Common.displayLoadBar(...)} membersihkan dan mengisi ulang area hasil di
	 * akhir proses, panel grafik yang sudah tampil tidak ikut terhapus. Setiap data mahasiswa diproses
	 * dalam blok percobaan tersendiri: data yang bermasalah dicatat lalu dilewati, tidak menggagalkan
	 * seluruh rekap; indeks baris tetap dinaikkan di blok {@code finally} agar baris berikutnya tidak
	 * tertimpa. Sesi Hibernate native ditutup satu kali di blok {@code finally}.
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

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_" + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

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

		/* UBAH 21-08-2026: tabel rincian sebelumnya hanya sebuah Div polos tanpa penanda apa pun,
		 * sehingga setelah panel grafik yang tinggi (kartu KPI + donut) ia terdorong ke bawah lipatan
		 * dan terlihat seolah hilang. Kini diberi judul bagian dan tinggi minimum supaya jelas bahwa
		 * rincian datanya ada di bawah grafik. Isinya tetap Grid ZK biasa -- widget Excel dirender
		 * lalu diganti Grid oleh PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(). */
		final org.zkoss.zul.Html judulRincian = new org.zkoss.zul.Html(
				"<div style='margin:14px 2px 6px;font-size:14px;font-weight:700;color:#0f172a;'>"
				+ "Rincian Data</div>"
				+ "<div style='margin:0 2px 8px;font-size:12px;color:#64748b;'>"
				+ "Tabel lengkap di bawah ini memuat data yang meringkas grafik di atas.</div>");
		judulRincian.setParent(centerBox);

		final Div gridHost = new Div();
		gridHost.setStyle("width:100%;min-height:220px;");
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
					rowhead.createCell(8).setCellValue("IP SEMESTER GANJIL");
					rowhead.createCell(9).setCellValue("IP SEMESTER GENAP");
					rowhead.createCell(10).setCellValue("IP RATA-RATA");
					rowhead.createCell(11).setCellValue("IP KUMULATIF");

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

					// Akumulasi ringkasan utk panel grafik (dihitung BERSAMAAN dgn penulisan sel Excel di
					// bawah, tanpa kueri tambahan) -> dirender lewat renderChartRingkasan setelah loop
					// selesai. Kunci peta = tahun angkatan (terurut menaik lewat TreeMap); nilai =
					// {jumlah IPK, jumlah IP rata-rata, jumlah mahasiswa} pada angkatan tsb.
					final Map<Integer, double[]> rekapPerAngkatan = new TreeMap<Integer, double[]>();
					final int[] sebaranIpk = new int[5];
					double akumulasiIpk = 0;
					double akumulasiIpRata = 0;
					int jumlahValid = 0;

					for (Mahasiswa mahasiswa : mahasiswas) {

						try {

							label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
									+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

							XSSFRow row = sheet.createRow(rowIndex);
							XSSFCell cell = row.createCell(0);
							cell.setCellValue(mahasiswa.getNim());

							cell = row.createCell(1);
							cell.setCellValue(mahasiswa.getNama());

							String namaFakultas = mahasiswa.getJurusan() == null
									|| mahasiswa.getJurusan().getFakultas() == null ? ""
											: mahasiswa.getJurusan().getFakultas().getNama();
							String namaJurusan = mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama();

							cell = row.createCell(2);
							cell.setCellValue(namaFakultas);
							cell = row.createCell(3);
							cell.setCellValue(namaJurusan);

							cell = row.createCell(4);
							cell.setCellValue(mahasiswa.getProgram());

							cell = row.createCell(5);
							cell.setCellValue(mahasiswa.getTahunangkatan());

							cell = row.createCell(6);
							cell.setCellValue(tahunAkademik);

							Integer semesterGanjil = Common.getSemester(mahasiswa.getTahunangkatan(), tahunAkademik,
									Perkuliahan.GANJIL, mahasiswa.getPindahKeKampusIniMasukSemester(),
									mahasiswa.getSemesterMulai());

							Integer semesterGenap = Common.getSemester(mahasiswa.getTahunangkatan(), tahunAkademik,
									Perkuliahan.GENAP, mahasiswa.getPindahKeKampusIniMasukSemester(),
									mahasiswa.getSemesterMulai());

							if (semesterGanjil > semesterGenap) {
								semesterGenap += 2;
							}

							cell = row.createCell(7);
							cell.setCellValue(semesterGanjil + " & " + semesterGenap);

							double ipGanjil = angkaAman(mahasiswa.hitungIPKSemester(semesterGanjil, null, null));
							double ipGenap = angkaAman(mahasiswa.hitungIPKSemester(semesterGenap, null, null));

							cell = row.createCell(8);
							cell.setCellValue(ipGanjil);

							cell = row.createCell(9);
							cell.setCellValue(ipGenap);

							double ipRata = (ipGanjil + ipGenap) / 2;
							cell = row.createCell(10);
							cell.setCellValue(ipRata);

							KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
							double ipkKumulatif = angkaAman(krsMahasiswa.getIpk());

							cell = row.createCell(11);
							cell.setCellValue(ipkKumulatif);

							// -- Akumulasi ringkasan panel grafik: KPI, tren IPK/IP per angkatan, sebaran
							// IPK -- pakai nilai yang barusan ditulis ke sel Excel di atas, tanpa hitung
							// ulang / tanpa kueri tambahan. --
							akumulasiIpk += ipkKumulatif;
							akumulasiIpRata += ipRata;
							jumlahValid++;

							int idxSebaran = ipkKumulatif < 2.00 ? 0 : ipkKumulatif < 2.50 ? 1
									: ipkKumulatif < 3.00 ? 2 : ipkKumulatif < 3.50 ? 3 : 4;
							sebaranIpk[idxSebaran]++;

							Integer tahunAngkatanBaris = mahasiswa.getTahunangkatan();
							if (tahunAngkatanBaris != null) {
								double[] agg = rekapPerAngkatan.get(tahunAngkatanBaris);
								if (agg == null) {
									agg = new double[3];
									rekapPerAngkatan.put(tahunAngkatanBaris, agg);
								}
								agg[0] += ipkKumulatif;
								agg[1] += ipRata;
								agg[2] += 1;
							}

						} catch (Exception exBaris) {
							// Satu data mahasiswa bermasalah (mis. prodi/fakultas/angkatan/riwayat KRS
							// belum lengkap) tidak boleh menggagalkan seluruh proses rekap; catat & lanjut.
							ais.common.ErrorAuditUtil.record(exBaris,
									"auto-guard(per-mahasiswa) src/ais/action/master/dashboard/admin/DashboardDataNilaiMahasiswaPerTahunAngkatan.java");
						} finally {
							rowIndex++;
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
						renderChartRingkasan(chartPanel, jumlahValid, akumulasiIpk, akumulasiIpRata, sebaranIpk,
								rekapPerAngkatan);
					} catch (Exception exChart) { ais.common.ErrorAuditUtil.record(exChart,
							"auto-guard(chart) src/ais/action/master/dashboard/admin/DashboardDataNilaiMahasiswaPerTahunAngkatan.java"); }

					mahasiswas.clear();
					label.setValue("");

				} catch (Exception exProses) {
					// Kegagalan proses (mis. query bermasalah) tidak boleh membuat indikator "sedang
					// memproses" menggantung selamanya di sisi pengguna.
					ais.common.ErrorAuditUtil.record(exProses,
							"src/ais/action/master/dashboard/admin/DashboardDataNilaiMahasiswaPerTahunAngkatan.java:initSpreadsheet-thread");
					Common.tampilErrorJikaAdmin(exProses);
					label.setValue("");
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	/**
	 * Mengembalikan nilai {@code double} aman dari {@link Double} yang mungkin {@code null} (mis. IP/IPK
	 * mahasiswa yang belum pernah dihitung), agar tidak terjadi {@code NullPointerException} saat
	 * unboxing.
	 */
	private static double angkaAman(Double nilai) {
		return nilai == null ? 0.0 : nilai.doubleValue();
	}

	/**
	 * Merender panel ringkasan visual dasbor ini sepenuhnya memakai {@code ais.ui.util.HtmlChartHelper}
	 * (HTML/CSS, bukan JFreeChart): kartu KPI (total mahasiswa, rata-rata IPK, rata-rata IP semester,
	 * jumlah angkatan tercakup), grafik garis tren rata-rata IPK dan IP semester per tahun angkatan
	 * (grafik utama dasbor ini karena data memang dikelompokkan per angkatan), grafik donat komposisi
	 * jumlah mahasiswa per angkatan, serta grafik batang sebaran rentang nilai IPK. Seluruh parameter
	 * berasal dari akumulasi yang sudah dihitung selagi menulis sel Excel di {@link #initSpreadsheet()},
	 * sehingga tidak ada kueri basis data tambahan yang dijalankan hanya untuk kebutuhan grafik.
	 *
	 * @param chartPanel       komponen tampilan tujuan.
	 * @param jumlahValid      jumlah mahasiswa yang berhasil diproses.
	 * @param akumulasiIpk     jumlah (bukan rata-rata) IPK kumulatif seluruh mahasiswa yang berhasil diproses.
	 * @param akumulasiIpRata  jumlah (bukan rata-rata) IP rata-rata (ganjil &amp; genap) seluruh mahasiswa yang berhasil diproses.
	 * @param sebaranIpk       jumlah mahasiswa per rentang IPK: [&lt;2.00, 2.00-2.49, 2.50-2.99, 3.00-3.49, 3.50-4.00].
	 * @param rekapPerAngkatan peta tahun angkatan -&gt; {jumlah IPK, jumlah IP rata-rata, jumlah mahasiswa}, terurut menaik.
	 */
	private static void renderChartRingkasan(Html chartPanel, int jumlahValid, double akumulasiIpk,
			double akumulasiIpRata, int[] sebaranIpk, Map<Integer, double[]> rekapPerAngkatan) {

		double rataIpk = jumlahValid == 0 ? 0 : akumulasiIpk / jumlahValid;
		double rataIpRata = jumlahValid == 0 ? 0 : akumulasiIpRata / jumlahValid;

		StringBuilder html = new StringBuilder(4096);
		html.append("<div style='padding:10px 12px 4px;display:flex;flex-direction:column;gap:14px;'>");

		html.append(ais.ui.util.HtmlChartHelper.kpiCards(
				new String[] { "Total Mahasiswa", "Rata-rata IPK", "Rata-rata IP Semester", "Jumlah Angkatan" },
				new String[] { String.valueOf(jumlahValid), Common.numberFormat.get().format(rataIpk),
						Common.numberFormat.get().format(rataIpRata), String.valueOf(rekapPerAngkatan.size()) },
				new String[] { "mahasiswa berhasil diproses", "skala 0.00 - 4.00", "skala 0.00 - 4.00",
						"tahun angkatan tercakup pada rekap ini" },
				null, null, new String[] { "#2563eb", "#16a34a", "#0891b2", "#7c3aed" }));

		if (!rekapPerAngkatan.isEmpty()) {
			int n = rekapPerAngkatan.size();
			String[] kategoriAngkatan = new String[n];
			double[] seriIpk = new double[n];
			double[] seriIpRata = new double[n];
			double[] jumlahMhsPerAngkatan = new double[n];
			int i = 0;
			for (Map.Entry<Integer, double[]> entri : rekapPerAngkatan.entrySet()) {
				double[] agg = entri.getValue();
				double jml = agg[2];
				kategoriAngkatan[i] = String.valueOf(entri.getKey());
				seriIpk[i] = jml == 0 ? 0 : agg[0] / jml;
				seriIpRata[i] = jml == 0 ? 0 : agg[1] / jml;
				jumlahMhsPerAngkatan[i] = jml;
				i++;
			}

			html.append(ais.ui.util.HtmlChartHelper.lineMulti("Tren Rata-rata IPK per Tahun Angkatan",
					"Menunjukkan naik-turunnya rata-rata nilai IPK dan IP semester mahasiswa dari satu "
							+ "angkatan ke angkatan berikutnya, sehingga perkembangan capaian akademik antar "
							+ "angkatan terlihat jelas.",
					kategoriAngkatan, new String[] { "Rata-rata IPK", "Rata-rata IP Semester" },
					new double[][] { seriIpk, seriIpRata }, new String[] { "#16a34a", "#0891b2" }));

			html.append(ais.ui.util.HtmlChartHelper.donut("Komposisi Jumlah Mahasiswa per Tahun Angkatan",
					"Menunjukkan proporsi jumlah mahasiswa pada tiap tahun angkatan dari seluruh data yang "
							+ "direkap, sehingga angkatan dengan jumlah mahasiswa terbanyak mudah terlihat.",
					kategoriAngkatan, jumlahMhsPerAngkatan, null, jumlahValid + " mhs"));
		}

		html.append(ais.ui.util.HtmlChartHelper.barHorizontal("Sebaran Nilai IPK Mahasiswa",
				"Menunjukkan berapa banyak mahasiswa berada pada tiap rentang nilai IPK, dari yang paling "
						+ "rendah sampai paling tinggi, sehingga sebaran capaian akademik terlihat sekilas.",
				new String[] { "< 2.00", "2.00 - 2.49", "2.50 - 2.99", "3.00 - 3.49", "3.50 - 4.00" },
				new double[] { sebaranIpk[0], sebaranIpk[1], sebaranIpk[2], sebaranIpk[3], sebaranIpk[4] },
				"#2563eb"));

		html.append("</div>");
		chartPanel.setContent(html.toString());
	}
}
