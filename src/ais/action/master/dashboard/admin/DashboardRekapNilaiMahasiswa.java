package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vlayout;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.common.Common;
import ais.common.CommonFileUtil;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h1>DashboardRekapNilaiMahasiswa &mdash; Rekapitulasi Nilai (IPS/IPK/SKS) Mahasiswa</h1>
 *
 * <p>
 * Dasbor ini menjawab kebutuhan paling umum dari staf akademik, dosen wali (Dosen PA), dan pimpinan
 * program studi: <b>melihat capaian nilai mahasiswa dalam satu tabel rekap</b> tanpa harus membuka
 * data KRS satu per satu. Pengguna memilih kombinasi filter di panel atas (Fakultas, Program Studi,
 * Tahun Akademik, Dosen PA, jenis semester Ganjil/Genap, semester ke berapa, angkatan, program, kelas,
 * atau nama/NIM mahasiswa tertentu), menekan tombol <b>Proses</b>, dan sistem akan menyusun satu baris
 * per mahasiswa berisi NIM, nama, program studi, angkatan, kelas, status, dosen PA, serta IPS/IPK/SKS
 * (baik nilai akhir saja, atau rincian per semester bila kotak centang <i>"Setiap semester"</i>
 * dicentang). Hasilnya bisa diunduh sebagai berkas Excel lewat tombol <b>Download</b>.
 * </p>
 *
 * <h2>Alur teknis singkat</h2>
 * <p>
 * {@link #init()} membangun seluruh tata letak (form filter di region {@code North}, area hasil di
 * region {@code Center}) beserta seluruh event-listener kombo yang saling bergantung (mis. memilih
 * jenis semester akan mengisi ulang pilihan "Semester ke", yang lalu menghitung ulang tahun angkatan
 * secara otomatis). {@link #initSpreadsheet()} adalah method inti yang dipanggil saat tombol
 * <b>Proses</b> ditekan: karena penyusunan rekap bisa memakan waktu lama untuk data mahasiswa yang
 * banyak (terutama saat opsi "Setiap semester" aktif, karena IPS/IPK dihitung ulang untuk setiap
 * semester dari 1 sampai semester yang dipilih), proses berat ini dijalankan pada {@link Thread} latar
 * belakang terpisah agar antarmuka ZK tidak macet/hang menunggu. Selama proses berjalan,
 * {@code Common.displayLoadBar(...)} menampilkan indikator "sedang memproses" yang di-<i>polling</i>
 * lewat komponen {@link Label} tersembunyi; begitu label dikosongkan, berkas Excel yang sudah selesai
 * ditulis ke folder sementara otomatis ditampilkan sebagai tabel/grid ringan (lihat
 * {@code ais.ui.util.PratinjauXlsxHelper#gantiSpreadsheetDenganGrid}) alih-alih widget Excel penuh yang
 * berat di sisi peramban.
 * </p>
 *
 * <h2>Panel ringkasan visual (HTML/CSS, bukan JFreeChart)</h2>
 * <p>
 * Selain tabel mentah, dasbor ini juga menampilkan panel ringkasan berupa kartu KPI (total mahasiswa,
 * rata-rata IPK/IPS, jumlah lulus), grafik donat komposisi status mahasiswa, grafik batang sebaran
 * rentang nilai IPK, grafik garis tren rata-rata IPS/IPK per semester (bila "Setiap semester"
 * dicentang), dan grafik jaring laba-laba (radar) perbandingan rata-rata IPK antar program studi.
 * Seluruh angka pada panel ini dihitung <b>bersamaan</b> dengan penulisan sel Excel di dalam
 * {@link #initSpreadsheet()} (loop yang sama, tanpa kueri basis data tambahan) lalu dirender lewat
 * {@code ais.ui.util.HtmlChartHelper} setelah loop selesai, dibungkus percobaan/tangkapan tersendiri
 * supaya kegagalan menggambar grafik tidak pernah mengganggu penulisan Excel maupun tampilan grid.
 * </p>
 *
 * <h2>Ketahanan terhadap data tidak lengkap</h2>
 * <p>
 * Data mahasiswa pada praktiknya bisa tidak lengkap (program studi belum diisi, tahun angkatan kosong,
 * riwayat status belum tercatat, dsb.). Setiap baris mahasiswa diproses di dalam blok percobaan
 * tersendiri: bila satu data bermasalah, kesalahannya dicatat lewat
 * {@code ais.common.ErrorAuditUtil#record} dan proses lanjut ke mahasiswa berikutnya alih-alih
 * menghentikan seluruh rekap (yang sebelumnya bisa membuat proses macet tanpa keterangan bagi
 * mahasiswa lain yang datanya sebenarnya lengkap dan valid).
 * </p>
 *
 * <h2>Sesi Hibernate</h2>
 * <p>
 * Thread latar belakang mengambil sesi lewat {@code HibernateUtil.openSession()} (sesi native,
 * BUKAN sesi ber-scope permintaan/{@code currentSession()}), sehingga wajib ditutup secara eksplisit;
 * penutupan dilakukan tepat satu kali di blok {@code finally} agar sesi selalu tertutup baik proses
 * berhasil maupun gagal, dan tidak dipanggil berulang di banyak tempat.
 * </p>
 *
 * @author e-Campus UI Team
 */
public class DashboardRekapNilaiMahasiswa extends MyWindow {

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
	private MyCheckboxConfig ambilDariDatabase = new MyCheckboxConfig("Hitung ulang");
	private MyCheckboxConfig tiapSemester = new MyCheckboxConfig("Setiap semester");
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();
	private Textbox mahasiswa = new Textbox();
	private Center center = new Center();

	private MyTextbox kelas = new MyTextbox();

	private File file;

	public DashboardRekapNilaiMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapNilaiMahasiswa(String title, String border, boolean closable) {
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
	 * Fakultas/Prodi/Tahun Akademik/Dosen PA, kombo jenis semester (Ganjil/Genap) yang mengisi ulang
	 * pilihan "Semester ke" secara dinamis, kotak teks Kelas/Mahasiswa untuk pencarian bebas, dua
	 * kotak centang ("Hitung ulang" untuk memaksa hitung ulang IPS/IPK dari data KRS terbaru alih-alih
	 * memakai nilai tersimpan, dan "Setiap semester" untuk menampilkan rincian per semester alih-alih
	 * hanya nilai akhir), serta area hasil (region {@code Center}) dan tombol Proses/Download pada
	 * toolbar. Perubahan pada kombo "Jenis Semester" atau "Semester ke" otomatis menghitung ulang
	 * tahun angkatan yang sesuai lewat listener {@code ubahAngkatan}, sehingga pengguna tidak perlu
	 * mengisi angkatan secara manual. Method ini hanya menyiapkan komponen UI; proses pengambilan dan
	 * perhitungan data baru dijalankan saat tombol Proses ditekan, lihat {@link #initSpreadsheet()}.
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
		north.setHeight("240px");
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
				+ "Menampilkan rekap nilai (IPS, IPK, dan SKS) setiap mahasiswa per semester, lengkap dengan "
				+ "ringkasan grafik performa akademik, sehingga hasil belajar mahasiswa mudah dievaluasi tanpa "
				+ "membuka data satu per satu.</div>");
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
		searchsemester.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatan);
		angkatan.setWidth("90%");
		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		final EventListener ubahAngkatan = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final String tahunAkademik = (String) (DashboardRekapNilaiMahasiswa.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardRekapNilaiMahasiswa.this.tahunAkademik.getSelectedItem().getValue());
				final String semester = (String) (DashboardRekapNilaiMahasiswa.this.semesterAbsensi
						.getSelectedItem() == null ? Perkuliahan.GANJIL
								: DashboardRekapNilaiMahasiswa.this.semesterAbsensi.getSelectedItem().getValue());

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
				searchsemester.setSelectedIndex(0);
				ubahAngkatan.onEvent(null);
			}
		};

		tahunAkademik.addEventListener("onChange", ubahAngkatan);
		searchsemester.addEventListener("onChange", ubahAngkatan);

		semesterAbsensi.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "1,1,1,1,1,1,2");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas);
		kelas.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(mahasiswa);
		mahasiswa.setWidth("90%");

		row.appendChild(ambilDariDatabase);

		row.appendChild(tiapSemester);
		tiapSemester.setChecked(true);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(row);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheet();
			}
		});
		print.setParent(toolbar);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "REKAP_PENILAIAN.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapNilaiMahasiswa.java:334");

				}
			}
		});
		print.setParent(toolbar);

	}

	/**
	 * Method inti dasbor ini: membaca seluruh filter yang dipilih pengguna, lalu menjalankan
	 * penyusunan rekap nilai mahasiswa pada {@link Thread} latar belakang terpisah (agar antarmuka
	 * tidak macet untuk data yang banyak), menuliskannya sebagai berkas Excel sekaligus mengumpulkan
	 * ringkasan angka (status, sebaran IPK, tren per semester, rata-rata per prodi) untuk panel grafik
	 * HTML/CSS yang dirender lewat {@link #renderChartRingkasan}. Region {@code Center} dibungkus satu
	 * {@link Vlayout} berisi dua anak: panel grafik ({@code chartPanel}) dan wadah grid/Excel
	 * ({@code gridHost}) yang terpisah, sehingga saat {@code Common.displayLoadBar(...)} membersihkan
	 * dan mengisi ulang area hasil di akhir proses, panel grafik yang sudah tampil tidak ikut terhapus.
	 * Setiap data mahasiswa diproses dalam blok percobaan tersendiri: data yang bermasalah dicatat lalu
	 * dilewati, tidak menggagalkan seluruh rekap. Sesi Hibernate native ditutup satu kali di blok
	 * {@code finally}.
	 */
	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		Common.clear(center);
		final String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());

		final String semester = (String) (this.semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? Perkuliahan.GANJIL
						: this.semesterAbsensi.getSelectedItem().getValue());

		final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? -1
				: searchsemester.getSelectedItem().getValue());
		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");

		final String kelas = this.kelas.getValue().trim();

		final String mahasiswa = this.mahasiswa.getValue().trim();

		final boolean hitungUlang = ambilDariDatabase.isChecked();
		final boolean tampilPerSemester = tiapSemester.isChecked();

		if (tahunAkademik == null) {
			return;
		}
		final Integer tahunAngkatan = angkatan.getValue();
		final int tahun = Integer.parseInt(StringUtils.split(tahunAkademik, "/")[0]);

		System.out.println("init spreadsheet running => tahun = " + tahun);

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		file = new File(filename);
		CommonFileUtil.ensureWritableFile(file, 50L * 1024L * 1024L);
		file.createNewFile();

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
		final Desktop desktop = Executions.getCurrent().getDesktop();

		/* OPTIMASI FASE 5: server push dulu dinyalakan di sini tetapi TIDAK PERNAH dimatikan,
		 * sehingga browser terus polling (menahan thread Tomcat) selama tab terbuka walau proses
		 * sudah selesai. Tugas juga dijalankan pada thread MENTAH tanpa batas.
		 * jalankanDenganPush() menyalakan push ber-reference-count, menjalankan tugas pada pool
		 * daemon berbatas milik AsyncTaskManager, lalu MELEPAS push di finally. */
		ais.common.AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() {

			@Override
			public void run() {
				Session session = null;
				try {

				System.out.println("tahunAkademik = " + tahunAkademik);

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("PENILAIAN");
				sheet.setDefaultColumnWidth(15);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("NAMA");
				rowhead.createCell(2).setCellValue("PRODI");
				rowhead.createCell(3).setCellValue("PROGRAM");
				rowhead.createCell(4).setCellValue("ANGKATAN");
				rowhead.createCell(5).setCellValue("KELAS");
				rowhead.createCell(6).setCellValue("SEMESTER");
				rowhead.createCell(7).setCellValue("STATUS");
				rowhead.createCell(8).setCellValue("STATUS AWAL");
				rowhead.createCell(9).setCellValue("DOSEN PA");

				if (tampilPerSemester) {
					for (int i = 1; i <= semesterKe; i++) {
						rowhead.createCell(9 + i).setCellValue("IPS SMT " + i);
						rowhead.createCell(semesterKe + 9 + i).setCellValue("IPK SMT " + i);

						rowhead.createCell((semesterKe * 2) + 9 + i).setCellValue("SKS SMT " + i);
						rowhead.createCell((semesterKe * 3) + 9 + i).setCellValue("SKSK SMT " + i);
						
						rowhead.createCell((semesterKe * 4) + 9 + i).setCellValue("SKS LULUS SMT " + i);
						rowhead.createCell((semesterKe * 5) + 9 + i).setCellValue("SKSK LULUS SMT " + i);
	
					}
				} else {
					rowhead.createCell(10).setCellValue("IPS");
					rowhead.createCell(11).setCellValue("IPK");

					rowhead.createCell(12).setCellValue("SKS");
					rowhead.createCell(13).setCellValue("SKSK");
					
					rowhead.createCell(14).setCellValue("SKS LULUS");
					rowhead.createCell(15).setCellValue("SKSK LULUS");
				}

				session = HibernateUtil.openSession();

				List<Mahasiswa> mahasiswas = ConstantValues.simpleList(session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(mahasiswa.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.ilike("nim", mahasiswa.trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("nama", mahasiswa.trim(), MatchMode.ANYWHERE)))

						.addOrder(Order.asc("nim"))

						.add(dosen != null ? Restrictions.eq("dosen", dosen.getId())
								: Restrictions.sqlRestriction("1=1"))

						.createAlias("jurusan", "jurusan")

						.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan.fakultas", fakultas))

						.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))

						.add(program == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("program", program))

						.add(tahunAngkatan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunangkatan", tahunAngkatan))

						.add(kelas != null && !kelas.trim().isEmpty()
								? Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)
								: Restrictions.sqlRestriction("true"))

						, Mahasiswa.class);

				int size = mahasiswas.size();

				int rowIndex = 1;
				int rowIndexAda = 1;

				// Akumulasi ringkasan utk panel grafik (dihitung BERSAMAAN dgn penulisan sel Excel di
				// bawah, tanpa kueri tambahan) -> dirender lewat renderChartRingkasan setelah loop selesai.
				final Map<String, int[]> jumlahPerStatus = new LinkedHashMap<String, int[]>();
				final Map<String, double[]> ipkPerJurusan = new LinkedHashMap<String, double[]>();
				final int semesterKeAman = semesterKe != null && semesterKe > 0 ? semesterKe : 0;
				final double[] totalIpsPerSmt = new double[semesterKeAman + 1];
				final double[] totalIpkPerSmt = new double[semesterKeAman + 1];
				final int[] jumlahPerSmt = new int[semesterKeAman + 1];
				final int[] sebaranIpk = new int[5];
				double akumulasiIpk = 0;
				double akumulasiIps = 0;
				int jumlahValid = 0;

				for (Mahasiswa mahasiswa : mahasiswas) {

					try {

						if (hitungUlang) {
							mahasiswa.reInitDetailperkuliahan(session);
						}

						safeSetLabel(desktop, label, "Sedang memproses data " + mahasiswa.toString() + " ("
								+ Common.numberFormat.get().format(rowIndexAda * 100.0 / size) + " %)");
						rowIndexAda++;

						Integer smt = semesterKe > 0 ? semesterKe
								: Common.getSemester(mahasiswa.getTahunangkatan(), tahunAkademik, semester,
										mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

						KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt, null, null, hitungUlang);

						XSSFRow row = sheet.createRow(rowIndex);
						XSSFCell cell = row.createCell(0);
						cell.setCellValue(mahasiswa.getNim());

						cell = row.createCell(1);
						cell.setCellValue(mahasiswa.getNama());

						String namaJurusan = mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama();
						cell = row.createCell(2);
						cell.setCellValue(namaJurusan);

						cell = row.createCell(3);
						cell.setCellValue(mahasiswa.getProgram());

						cell = row.createCell(4);
						cell.setCellValue(mahasiswa.getTahunangkatan() == null ? ""
								: mahasiswa.getTahunangkatan().toString());

						cell = row.createCell(5);
						cell.setCellValue(krsMahasiswa.getKelas());

						cell = row.createCell(6);
						cell.setCellValue(smt);

						String namaStatus = "";
						try {
							ais.database.model.HistoryStatusMahasiswa historyStatus = ais.action.master.helper.HistoryStatusMahasiswaUtil
									.currentStatus(mahasiswa, tahunAkademik, smt);
							if (historyStatus != null && historyStatus.getStatusMahasiswa() != null) {
								namaStatus = historyStatus.getStatusMahasiswa().getNama();
							}
						} catch (Exception exStatus) { ais.common.ErrorAuditUtil.record(exStatus,
								"auto-guard(status-mahasiswa) src/ais/action/master/dashboard/admin/DashboardRekapNilaiMahasiswa.java"); }
						cell = row.createCell(7);
						cell.setCellValue(namaStatus);

						cell = row.createCell(8);
						cell.setCellValue(mahasiswa.getStatusAwalMahasiswa() == null ? ""
								: mahasiswa.getStatusAwalMahasiswa().getNama());

						cell = row.createCell(9);
						cell.setCellValue(krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama());

						if (tampilPerSemester) {
							for (int i = 1; i <= semesterKe; i++) {

								krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, i, null, null, hitungUlang);
								double ipsSmt = angkaAman(krsMahasiswa.getIps());
								double ipkSmt = angkaAman(krsMahasiswa.getIpk());

								cell = row.createCell(9 + i);
								cell.setCellValue(ipsSmt);

								cell = row.createCell(semesterKe + 9 + i);
								cell.setCellValue(ipkSmt);

								cell = row.createCell((semesterKe * 2) + 9 + i);
								cell.setCellValue(krsMahasiswa.getSksYangDiambil());

								cell = row.createCell((semesterKe * 3) + 9 + i);
								cell.setCellValue(krsMahasiswa.getSksk());

								cell = row.createCell((semesterKe * 4) + 9 + i);
								cell.setCellValue(krsMahasiswa.getSksYangDiambilLulus());

								cell = row.createCell((semesterKe * 5) + 9 + i);
								cell.setCellValue(krsMahasiswa.getSkskLulus());

								if (i < totalIpsPerSmt.length) {
									totalIpsPerSmt[i] += ipsSmt;
									totalIpkPerSmt[i] += ipkSmt;
									jumlahPerSmt[i]++;
								}
							}
						} else {
							double ipsAkhir = angkaAman(krsMahasiswa.getIps());
							double ipkAkhir = angkaAman(krsMahasiswa.getIpk());

							cell = row.createCell(10);
							cell.setCellValue(ipsAkhir);

							cell = row.createCell(11);
							cell.setCellValue(ipkAkhir);

							cell = row.createCell(12);
							cell.setCellValue(krsMahasiswa.getSksYangDiambil());

							cell = row.createCell(13);
							cell.setCellValue(krsMahasiswa.getSksk());

							cell = row.createCell(14);
							cell.setCellValue(krsMahasiswa.getSksYangDiambilLulus());

							cell = row.createCell(15);
							cell.setCellValue(krsMahasiswa.getSkskLulus());
						}

						// -- Akumulasi ringkasan panel grafik: KPI, donat status, radar IPK per prodi,
						// sebaran IPK, tren IPS/IPK per semester -- pakai nilai akhir (krsMahasiswa
						// sudah menunjuk data semester terakhir yg diproses baik dari cabang di atas). --
						double ipkFinal = angkaAman(krsMahasiswa.getIpk());
						double ipsFinal = angkaAman(krsMahasiswa.getIps());
						akumulasiIpk += ipkFinal;
						akumulasiIps += ipsFinal;
						jumlahValid++;

						int idxSebaran = ipkFinal < 2.00 ? 0 : ipkFinal < 2.50 ? 1 : ipkFinal < 3.00 ? 2
								: ipkFinal < 3.50 ? 3 : 4;
						sebaranIpk[idxSebaran]++;

						if (!namaStatus.isEmpty()) {
							int[] c = jumlahPerStatus.get(namaStatus);
							if (c == null) {
								c = new int[1];
								jumlahPerStatus.put(namaStatus, c);
							}
							c[0]++;
						}

						if (!namaJurusan.isEmpty()) {
							double[] agg = ipkPerJurusan.get(namaJurusan);
							if (agg == null) {
								agg = new double[2];
								ipkPerJurusan.put(namaJurusan, agg);
							}
							agg[0] += ipkFinal;
							agg[1] += 1;
						}

					} catch (Exception exBaris) {
						// Satu data mahasiswa bermasalah (mis. prodi/angkatan/riwayat status belum
						// lengkap) tidak boleh menggagalkan seluruh proses rekap; catat & lanjut.
						ais.common.ErrorAuditUtil.record(exBaris,
								"auto-guard(per-mahasiswa) src/ais/action/master/dashboard/admin/DashboardRekapNilaiMahasiswa.java");
					} finally {
						rowIndex++;
					}
				}

				Common.setStyled(sheet);
				final int jumlahBaris = rowIndex + 1;

				try {
					CommonFileUtil.ensureWritableFile(file, 50L * 1024L * 1024L);
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					Common.tampilErrorJikaAdmin(e);
				}

				// -- Render panel grafik (HTML/CSS, BUKAN JFreeChart) dari ringkasan yang sudah
				// dihitung di loop di atas. Kegagalan menggambar grafik tidak boleh mengganggu
				// Excel/grid yang sudah berhasil ditulis. --
				final int jumlahValidFinal = jumlahValid;
				final double akumulasiIpkFinal = akumulasiIpk;
				final double akumulasiIpsFinal = akumulasiIps;
				try {
					Executions.schedule(desktop, new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							sizedata.setValue(jumlahBaris);
							renderChartRingkasan(chartPanel, jumlahValidFinal, akumulasiIpkFinal,
									akumulasiIpsFinal, jumlahPerStatus, ipkPerJurusan, sebaranIpk,
									totalIpsPerSmt, totalIpkPerSmt, jumlahPerSmt, semesterKeAman,
									tampilPerSemester);
							label.setValue("");
						}
					}, new Event("onRekapNilaiSelesai"));
				} catch (Exception exChart) { ais.common.ErrorAuditUtil.record(exChart,
						"auto-guard(chart) src/ais/action/master/dashboard/admin/DashboardRekapNilaiMahasiswa.java"); }

				mahasiswas.clear();
				} catch (Exception exProses) {
					// Kegagalan proses (mis. query bermasalah) tidak boleh membuat indikator "sedang
					// memproses" menggantung selamanya di sisi pengguna.
					ais.common.ErrorAuditUtil.record(exProses,
							"src/ais/action/master/dashboard/admin/DashboardRekapNilaiMahasiswa.java:initSpreadsheet-thread");
					Common.tampilErrorJikaAdmin(exProses);
					safeSetLabel(desktop, label, "");
				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		});

	}

	private static void safeSetLabel(Desktop desktop, final Label label, final String value) {
		if (desktop == null || !desktop.isAlive()) return;
		try {
			Executions.schedule(desktop, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					label.setValue(value);
				}
			}, new Event("onRekapNilaiProgress"));
		} catch (Exception error) {
			ais.common.ErrorAuditUtil.record(error,
					"auto-audit src/ais/action/master/dashboard/admin/DashboardRekapNilaiMahasiswa.java:schedule-ui");
		}
	}

	/** Mengembalikan nilai {@code double} aman dari {@link Double} yang mungkin {@code null} (mis. IPS/IPK
	 * mahasiswa yang belum pernah dihitung), agar tidak terjadi {@code NullPointerException} saat unboxing. */
	private static double angkaAman(Double nilai) {
		return nilai == null ? 0.0 : nilai.doubleValue();
	}

	/**
	 * Merender panel ringkasan visual dasbor ini sepenuhnya memakai {@code ais.ui.util.HtmlChartHelper}
	 * (HTML/CSS, bukan JFreeChart): kartu KPI (total mahasiswa, rata-rata IPK/IPS, jumlah lulus), grafik
	 * donat komposisi status mahasiswa, grafik batang sebaran rentang nilai IPK, grafik garis tren
	 * rata-rata IPS/IPK per semester (hanya bila opsi "Setiap semester" dicentang dan semester yang
	 * dipilih lebih dari satu), serta grafik radar (jaring laba-laba) perbandingan rata-rata IPK antar
	 * program studi (hanya bila datanya mencakup minimal 3 program studi agar bentuk jaring bermakna).
	 * Seluruh parameter berasal dari akumulasi yang sudah dihitung selagi menulis sel Excel di
	 * {@link #initSpreadsheet()}, sehingga tidak ada kueri basis data tambahan yang dijalankan hanya
	 * untuk kebutuhan grafik.
	 *
	 * @param chartPanel      komponen tampilan tujuan.
	 * @param jumlahValid     jumlah mahasiswa yang berhasil diproses.
	 * @param akumulasiIpk    jumlah (bukan rata-rata) IPK akhir seluruh mahasiswa yang berhasil diproses.
	 * @param akumulasiIps    jumlah (bukan rata-rata) IPS akhir seluruh mahasiswa yang berhasil diproses.
	 * @param jumlahPerStatus peta nama status mahasiswa -&gt; jumlah (untuk grafik donat).
	 * @param ipkPerJurusan   peta nama program studi -&gt; {jumlah IPK, jumlah mahasiswa} (untuk radar).
	 * @param sebaranIpk      jumlah mahasiswa per rentang IPK: [&lt;2.00, 2.00-2.49, 2.50-2.99, 3.00-3.49, 3.50-4.00].
	 * @param totalIpsPerSmt  jumlah IPS per indeks semester (indeks 0 tak dipakai).
	 * @param totalIpkPerSmt  jumlah IPK per indeks semester (indeks 0 tak dipakai).
	 * @param jumlahPerSmt    jumlah mahasiswa per indeks semester (indeks 0 tak dipakai).
	 * @param semesterKeAman  semester ke berapa yang dipilih (0 bila "semua semester").
	 * @param perSemester     true bila opsi "Setiap semester" dicentang.
	 */
	private static void renderChartRingkasan(Html chartPanel, int jumlahValid, double akumulasiIpk,
			double akumulasiIps, Map<String, int[]> jumlahPerStatus, Map<String, double[]> ipkPerJurusan,
			int[] sebaranIpk, double[] totalIpsPerSmt, double[] totalIpkPerSmt, int[] jumlahPerSmt,
			int semesterKeAman, boolean perSemester) {

		double rataIpk = jumlahValid == 0 ? 0 : akumulasiIpk / jumlahValid;
		double rataIps = jumlahValid == 0 ? 0 : akumulasiIps / jumlahValid;
		int jumlahLulus = 0;
		for (Map.Entry<String, int[]> entri : jumlahPerStatus.entrySet()) {
			if (entri.getKey().toLowerCase().contains("lulus")) {
				jumlahLulus += entri.getValue()[0];
			}
		}

		StringBuilder html = new StringBuilder(4096);
		html.append("<div style='padding:10px 12px 4px;display:flex;flex-direction:column;gap:14px;'>");

		html.append(ais.ui.util.HtmlChartHelper.kpiCards(
				new String[] { "Total Mahasiswa", "Rata-rata IPK", "Rata-rata IPS", "Jumlah Berstatus Lulus" },
				new String[] { String.valueOf(jumlahValid), Common.numberFormat.get().format(rataIpk),
						Common.numberFormat.get().format(rataIps), String.valueOf(jumlahLulus) },
				new String[] { "mahasiswa berhasil diproses", "skala 0.00 - 4.00", "skala 0.00 - 4.00",
						"dari seluruh data pada rekap ini" },
				null, null, new String[] { "#2563eb", "#16a34a", "#0891b2", "#7c3aed" }));

		if (!jumlahPerStatus.isEmpty()) {
			String[] labelStatus = jumlahPerStatus.keySet().toArray(new String[jumlahPerStatus.size()]);
			double[] nilaiStatus = new double[labelStatus.length];
			for (int i = 0; i < labelStatus.length; i++) {
				nilaiStatus[i] = jumlahPerStatus.get(labelStatus[i])[0];
			}
			html.append(ais.ui.util.HtmlChartHelper.donut("Komposisi Status Mahasiswa",
					"Menunjukkan berapa banyak mahasiswa pada masing-masing status (aktif, cuti, lulus, dan "
							+ "lainnya) dari seluruh data yang direkap, sehingga proporsinya mudah dibandingkan.",
					labelStatus, nilaiStatus, null, jumlahValid + " mhs"));
		}

		html.append(ais.ui.util.HtmlChartHelper.barHorizontal("Sebaran Nilai IPK Mahasiswa",
				"Menunjukkan berapa banyak mahasiswa berada pada tiap rentang nilai IPK, dari yang paling "
						+ "rendah sampai paling tinggi, sehingga sebaran capaian akademik terlihat sekilas.",
				new String[] { "< 2.00", "2.00 - 2.49", "2.50 - 2.99", "3.00 - 3.49", "3.50 - 4.00" },
				new double[] { sebaranIpk[0], sebaranIpk[1], sebaranIpk[2], sebaranIpk[3], sebaranIpk[4] },
				"#2563eb"));

		if (perSemester && semesterKeAman > 1) {
			String[] kategoriSmt = new String[semesterKeAman];
			double[] seriIps = new double[semesterKeAman];
			double[] seriIpk = new double[semesterKeAman];
			for (int i = 1; i <= semesterKeAman; i++) {
				kategoriSmt[i - 1] = "Smt " + i;
				seriIps[i - 1] = jumlahPerSmt[i] == 0 ? 0 : totalIpsPerSmt[i] / jumlahPerSmt[i];
				seriIpk[i - 1] = jumlahPerSmt[i] == 0 ? 0 : totalIpkPerSmt[i] / jumlahPerSmt[i];
			}
			html.append(ais.ui.util.HtmlChartHelper.lineMulti("Tren Rata-rata IPS dan IPK per Semester",
					"Menunjukkan naik-turunnya rata-rata nilai mahasiswa dari semester ke semester, sehingga "
							+ "perkembangan prestasi akademik terlihat jelas.",
					kategoriSmt, new String[] { "Rata-rata IPS", "Rata-rata IPK" },
					new double[][] { seriIps, seriIpk }, new String[] { "#0891b2", "#16a34a" }));
		}

		if (ipkPerJurusan.size() >= 3) {
			String[] axesJurusan = ipkPerJurusan.keySet().toArray(new String[ipkPerJurusan.size()]);
			double[] nilaiIpkJurusan = new double[axesJurusan.length];
			for (int i = 0; i < axesJurusan.length; i++) {
				double[] agg = ipkPerJurusan.get(axesJurusan[i]);
				nilaiIpkJurusan[i] = agg[1] == 0 ? 0 : agg[0] / agg[1];
			}
			html.append(ais.ui.util.HtmlChartHelper.radar("Perbandingan Rata-rata IPK Antar Program Studi",
					"Membandingkan rata-rata nilai IPK antar program studi sekaligus dalam satu tampilan, "
							+ "sehingga program studi dengan capaian tertinggi atau terendah mudah terlihat.",
					axesJurusan, new String[] { "Rata-rata IPK" }, new double[][] { nilaiIpkJurusan },
					new String[] { "#7c3aed" }, 4.0));
		}

		html.append("</div>");
		chartPanel.setContent(html.toString());
	}
}
