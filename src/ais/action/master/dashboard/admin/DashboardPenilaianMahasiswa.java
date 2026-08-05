package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vlayout;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilai;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusPertemuan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h1>DashboardPenilaianMahasiswa &mdash; Rekap Progres Penilaian per Kelas Perkuliahan</h1>
 *
 * <p>
 * Dasbor ini menjawab kebutuhan dosen, admin akademik, dan pimpinan program studi untuk memantau
 * <b>sejauh mana proses penilaian (input nilai) sudah berjalan</b> pada tiap kelas perkuliahan, tanpa
 * harus membuka detail nilai satu per satu mahasiswa. Pengguna memilih kombinasi filter di panel atas
 * (Fakultas, Program Studi, Tahun Akademik, Dosen, Jenis Semester, Semester ke, Program, Jenis Ujian
 * UTS/UAS, Masa Perkuliahan) beserta tiga kotak centang saling melengkapi ("Hanya yg blm dinilai",
 * "Sama sekali blm dinilai", "Hanya yg telah dinilai") untuk mempersempit hasil, menekan tombol
 * <b>Proses</b>, dan sistem menyusun satu baris per kelas perkuliahan berisi kode/nama mata kuliah,
 * hari/waktu, dosen pengampu, ruangan, semester, jumlah mahasiswa yang <i>sudah</i> dan <i>belum</i>
 * dinilai berikut persentasenya, tanggal terakhir nilai diubah, serta total dan rata-rata IPS/nilai
 * kelas tersebut. Hasilnya bisa diunduh sebagai berkas Excel lewat tombol <b>Download</b>.
 * </p>
 *
 * <h2>Alur teknis singkat</h2>
 * <p>
 * {@link #init()} membangun seluruh tata letak (form filter di region {@code North} &mdash; kecuali
 * saat dasbor dipanggil dari konteks satu {@link Perkuliahan} spesifik lewat
 * {@link #DashboardPenilaianMahasiswa(Perkuliahan)}, filter disembunyikan karena datanya sudah pasti,
 * dan area hasil di region {@code Center}) beserta tombol Proses/Download pada toolbar.
 * {@link #initSpreadsheet()} adalah method inti yang dipanggil saat tombol <b>Proses</b> ditekan:
 * karena menghitung progres penilaian bisa memakan waktu untuk banyak kelas (tiap kelas memerlukan
 * pemindaian seluruh {@code Detailperkuliahan} pesertanya), proses berat ini dijalankan pada
 * {@link Thread} latar belakang terpisah agar antarmuka ZK tidak macet/hang menunggu. Selama proses
 * berjalan, {@code Common.displayLoadBar(...)} menampilkan indikator "sedang memproses" yang
 * di-<i>polling</i> lewat komponen {@link Label} tersembunyi; begitu label dikosongkan, berkas Excel
 * yang sudah selesai ditulis ke folder sementara otomatis ditampilkan sebagai tabel/grid ringan (lihat
 * {@code ais.ui.util.PratinjauXlsxHelper#gantiSpreadsheetDenganGrid}, dipanggil otomatis oleh
 * {@code LoadBarUtils.loadSpreadsheet}) alih-alih widget Excel penuh yang berat di sisi peramban.
 * </p>
 *
 * <h2>Panel ringkasan visual (HTML/CSS, bukan JFreeChart)</h2>
 * <p>
 * Selain tabel mentah, dasbor ini juga menampilkan panel ringkasan berupa kartu KPI (total kelas,
 * total mahasiswa yang sudah/belum dinilai, rata-rata persentase penilaian), grafik donat komposisi
 * kelas berdasarkan status penilaian (belum mulai/sebagian/selesai), dan grafik batang sebaran
 * persentase penilaian per kelas. Seluruh angka pada panel ini dihitung <b>bersamaan</b> dengan
 * penulisan sel Excel di dalam {@link #initSpreadsheet()} (loop yang sama, tanpa kueri basis data
 * tambahan) lalu dirender lewat {@code ais.ui.util.HtmlChartHelper} setelah loop selesai, dibungkus
 * percobaan/tangkapan tersendiri supaya kegagalan menggambar grafik tidak pernah mengganggu penulisan
 * Excel maupun tampilan grid.
 * </p>
 *
 * <h2>Ketahanan terhadap data tidak lengkap</h2>
 * <p>
 * Data kelas perkuliahan pada praktiknya bisa tidak lengkap (mata kuliah belum diisi, dosen belum
 * ditetapkan, dsb.). Setiap baris kelas diproses di dalam blok percobaan tersendiri: bila satu data
 * bermasalah, kesalahannya dicatat lewat {@code ais.common.ErrorAuditUtil#record} dan proses lanjut ke
 * kelas berikutnya alih-alih menghentikan seluruh rekap.
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
public class DashboardPenilaianMahasiswa extends MyWindow {

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
	private Combobox jenisUjian = new Combobox();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();
	private Center center = new Center();

	private File file;
	private MyCheckboxConfig BelumDinilai;
	private MyCheckboxConfig SamaSekaliBelumDinilai;
	private MyCheckboxConfig TelahDinilai;
	private AmbilDataMasaPerkuliahanBanbox searchmasaperkulaiahan;
	private Perkuliahan perkuliahan = null;

	public DashboardPenilaianMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardPenilaianMahasiswa(Perkuliahan perkuliahan) {
		super();
		try {
			this.perkuliahan = perkuliahan;
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardPenilaianMahasiswa(String title, String border, boolean closable) {
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
	 * Membangun seluruh tata letak dasbor: panel filter (region {@code North}, disembunyikan bila
	 * dasbor ini dipanggil dari konteks satu {@link Perkuliahan} spesifik lewat
	 * {@link #DashboardPenilaianMahasiswa(Perkuliahan)}) berisi kombo Fakultas/Prodi/Tahun
	 * Akademik/Dosen, kombo Jenis Semester (Ganjil/Genap/SP/Semua) yang mengisi ulang pilihan
	 * "Semester ke" secara dinamis, kombo Program, kombo Jenis Ujian (UTS/UAS/dsb.) untuk menentukan
	 * jenis pertemuan yang dijadikan acuan "sudah dinilai", kombo Masa Perkuliahan, serta tiga kotak
	 * centang saling melengkapi ("Hanya yg blm dinilai", "Sama sekali blm dinilai", "Hanya yg telah
	 * dinilai") untuk mempersempit hasil rekap. Method ini hanya menyiapkan komponen UI; proses
	 * pengambilan dan perhitungan data baru dijalankan saat tombol Proses ditekan, lihat
	 * {@link #initSpreadsheet()}.
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
		north.setVisible(perkuliahan == null);

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
				+ "Menampilkan progres penilaian tiap kelas perkuliahan, yaitu berapa banyak mahasiswa yang "
				+ "sudah dan belum dinilai beserta persentasenya, sehingga dosen dan admin akademik dapat "
				+ "segera mengetahui kelas mana yang penilaiannya masih tertunda.</div>");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
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
		comboitem = new MyComboitemConfig(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(1);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.setReadonly(true);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		semesterAbsensi.appendChild(comboitem);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester ke"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");
		searchsemester.setReadonly(true);

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);
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
						}
					} else {
						for (int i : Common.ganjil) {
							org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemester.appendChild(comboitem);
						}
					}
				} else {
					for (int i : new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 }) {
						if (i == 0)
							continue;
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				}

				MyComboitemConfig comboitem = new MyComboitemConfig();
				comboitem.setLabel("Semua");
				comboitem.setValue(null);
				searchsemester.appendChild(comboitem);
			}
		};

		semesterAbsensi.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Ujian (UTS,UAS)"));
		jenisUjian = new Combobox();
		Common.insertComboDanSemua(jenisUjian, "nama", StatusPertemuan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		row.appendChild(jenisUjian);
		jenisUjian.setWidth("90%");
		jenisUjian.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Perkuliahan"));
		row.appendChild(searchmasaperkulaiahan = new AmbilDataMasaPerkuliahanBanbox());
		searchmasaperkulaiahan.setWidth("90%");
		searchmasaperkulaiahan.setReadonly(true);

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
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});
		print.setParent(toolbar);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"REKAP_PENILAIAN.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardPenilaianMahasiswa.java:376");

				}
			}
		});
		print.setParent(toolbar);

		BelumDinilai = new MyCheckboxConfig("Hanya yg blm dinilai");
		SamaSekaliBelumDinilai = new MyCheckboxConfig("Sama sekali blm dinilai");
		TelahDinilai = new MyCheckboxConfig("Hanya yg telah dinilai");
		BelumDinilai.setParent(toolbar);
		SamaSekaliBelumDinilai.setParent(toolbar);
		TelahDinilai.setParent(toolbar);

	}

	/**
	 * Method inti dasbor ini: membaca seluruh filter yang dipilih pengguna, lalu menjalankan
	 * penyusunan rekap progres penilaian pada {@link Thread} latar belakang terpisah (agar antarmuka
	 * tidak macet untuk data kelas yang banyak), menuliskannya sebagai berkas Excel sekaligus
	 * mengumpulkan ringkasan angka (komposisi status penilaian per kelas, sebaran persentase) untuk
	 * panel grafik HTML/CSS yang dirender lewat {@link #renderChartRingkasan}. Region {@code Center}
	 * dibungkus satu {@link Vlayout} berisi dua anak: panel grafik ({@code chartPanel}) dan wadah
	 * grid/Excel ({@code gridHost}) yang terpisah, sehingga saat {@code Common.displayLoadBar(...)}
	 * membersihkan dan mengisi ulang area hasil di akhir proses, panel grafik yang sudah tampil tidak
	 * ikut terhapus. Setiap kelas perkuliahan diproses dalam blok percobaan tersendiri: data yang
	 * bermasalah dicatat lalu dilewati, tidak menggagalkan seluruh rekap. Sesi Hibernate native
	 * ditutup satu kali di blok {@code finally}.
	 */
	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		Common.clear(center);
		final String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());
		final String semester = (String) (this.semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? null
						: this.semesterAbsensi.getSelectedItem().getValue());

		final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? null
				: searchsemester.getSelectedItem().getValue());

		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");

		final StatusPertemuan statusPertemuan = (StatusPertemuan) (jenisUjian.getSelectedItem() == null ? null
				: jenisUjian.getSelectedItem().getValue());

		final MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) this.searchmasaperkulaiahan
				.getAttribute("masaPerkuliahan");

		if (tahunAkademik == null) {
			return;
		}

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
		final boolean telahDinilai = TelahDinilai.isChecked();
		final boolean samaSekaliBelumDinilai = SamaSekaliBelumDinilai.isChecked();
		final boolean belumDinilai = BelumDinilai.isChecked();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("PENILAIAN");
				sheet.setDefaultColumnWidth(25);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("KODE MATAKULIAH");
				rowhead.createCell(1).setCellValue("NAMA MATAKULIAH");
				rowhead.createCell(2).setCellValue("HARI");
				rowhead.createCell(3).setCellValue("WAKTU");
				rowhead.createCell(4).setCellValue("DOSEN");
				rowhead.createCell(5).setCellValue("RUANGAN");
				rowhead.createCell(6).setCellValue("SEMESTER");
				rowhead.createCell(7).setCellValue("SUDAH DINILAI");
				rowhead.createCell(8).setCellValue("BELUM DINILAI");
				rowhead.createCell(9).setCellValue("PROSENTASE");

				rowhead.createCell(10).setCellValue("TERAKHIR INPUT");

				rowhead.createCell(11).setCellValue("TOTAL IPS");
				rowhead.createCell(12).setCellValue("RATA-RATA IPS");
				rowhead.createCell(13).setCellValue("RATA-RATA NILAI");

				Session session = HibernateUtil.currentNativeSession();

				Criterion criterion = Restrictions.sqlRestriction("false");
				if (dosen != null) {
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen1", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen2", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));
				}

				List<Perkuliahan> perkuliahans = DashboardPenilaianMahasiswa.this.perkuliahan != null
						? new ArrayList<Perkuliahan>()
						: session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

								.add(masaPerkuliahan == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("masaPerkuliahan", masaPerkuliahan))

								.add(dosen == null ? Restrictions.sqlRestriction("true") : criterion)

								.createAlias("jurusan", "jurusan").addOrder(Order.asc("waktuMulai"))

								.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("jurusan.fakultas", fakultas))

								.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("jurusan", jurusan))

								.add(program == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("program", program))

								.add(semesterKe == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("semester", semesterKe))

								.add(tahunAkademik == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("tahunAjaran", tahunAkademik))

								.add(semester == null ? Restrictions.sqlRestriction("1=1")
										: (semester.equals(Perkuliahan.SP)
												? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
												: semester.equals(Perkuliahan.GANJIL)
														? Restrictions.in("semester", Common.ganjil)
														: Restrictions.in("semester", Common.genap)))

								.list();

				if (DashboardPenilaianMahasiswa.this.perkuliahan != null) {
					perkuliahans.add(DashboardPenilaianMahasiswa.this.perkuliahan);
				}

				int size = perkuliahans.size();

				int rowIndex = 1;
				int rowIndexAda = 1;

				// Akumulasi ringkasan utk panel grafik (dihitung BERSAMAAN dgn penulisan sel Excel di
				// bawah, tanpa kueri tambahan) -> dirender lewat renderChartRingkasan setelah loop selesai.
				// statusKelas: 0=belum dinilai, 1=sebagian dinilai, 2=selesai dinilai, 3=tanpa peserta.
				final int[] statusKelas = new int[4];
				// sebaranPersentase: 0%, 1-24%, 25-49%, 50-74%, 75-99%, 100%.
				final int[] sebaranPersentase = new int[6];
				int jumlahKelasDitampilkan = 0;
				int totalSudahDinilaiMhs = 0;
				int totalBelumDinilaiMhs = 0;
				double akumulasiPersentase = 0;

				for (Perkuliahan perkuliahan : perkuliahans) {
					try {
						List<FormatNilai> formatNilais = statusPertemuan == null ? null
								: perkuliahan.ambilFormatNilai(session);
						label.setValue("Sedang memproses data " + perkuliahan.toString() + " ("
								+ Common.numberFormat.get().format(rowIndexAda * 100.0 / size) + " %)");
						rowIndexAda++;

						Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan();
						int countSudahDinilai = 0;
						double totalIps = 0.0;
						double totalNilai = 0.0;
						Date tanggalDirubah = null;
						for (Long detailperkuliahanid : detailperkuliahans) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
									.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
							if (detailperkuliahan != null) {
								// getTotalIP()/getTotalNilai() bisa mengembalikan Double null (mis. mahasiswa
								// belum sama sekali dinilai) -> dibungkus angkaAman agar tak NPE saat unboxing.
								totalIps += angkaAman(detailperkuliahan.getTotalIP());
								totalNilai += angkaAman(detailperkuliahan.getTotalNilai());

								if (statusPertemuan != null) {
									for (FormatNilai formatNilai : formatNilais) {
										if (formatNilai.getStatusPertemuan() != null && formatNilai.getStatusPertemuan()
												.getId().equals(statusPertemuan.getId())) {
											if (tanggalDirubah == null
													|| tanggalDirubah.before(detailperkuliahan.getTanggal_dirubah())) {
												tanggalDirubah = detailperkuliahan.getTanggal_dirubah();
											}
											if (detailperkuliahan.retreiveDetailNilai(formatNilai) > 0.1) {
												countSudahDinilai++;
												break;
											}
										}
									}
								} else {
									if (detailperkuliahan.getTotalNilai() > 0.1) {
										countSudahDinilai++;
									}
									if (tanggalDirubah == null
											|| tanggalDirubah.before(detailperkuliahan.getTanggal_dirubah())) {
										tanggalDirubah = detailperkuliahan.getTanggal_dirubah();
									}
								}
							}
						}

						if (telahDinilai && countSudahDinilai == 0) {
							continue;
						}

						Integer countTotal = detailperkuliahans.size();

						Integer countBelumDinilai = countTotal - countSudahDinilai;

						if (belumDinilai && countBelumDinilai.equals(0)) {
							continue;
						}

						if (samaSekaliBelumDinilai && !countBelumDinilai.equals(countTotal)) {
							continue;
						}

						// Kelas tanpa peserta (countTotal 0) tak boleh dibagi 0 -> rata-rata diamankan ke 0.
						boolean adaPeserta = countTotal != null && countTotal > 0;
						double prosentase = adaPeserta ? (100.0 * countSudahDinilai) / countTotal : 0.0;
						double rataIpsKelas = adaPeserta ? totalIps / countTotal.doubleValue() : 0.0;
						double rataNilaiKelas = adaPeserta ? totalNilai / countTotal.doubleValue() : 0.0;

						// Baris ini dijamin akan ditulis (lolos semua filter continue di atas) -> dibungkus
						// try/finally tersendiri supaya bila penulisan sel gagal di tengah jalan, rowIndex
						// tetap maju dan baris berikutnya tidak menimpa/merusak baris yang sudah setengah
						// tertulis ini.
						try {
							XSSFRow row = sheet.createRow(rowIndex);
							XSSFCell cell = row.createCell(0);
							cell.setCellValue(
									perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());

							cell = row.createCell(1);
							cell.setCellValue(
									perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());

							cell = row.createCell(2);
							cell.setCellValue(perkuliahan.getHari());

							cell = row.createCell(3);
							cell.setCellValue((perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()) + "-"
									+ (perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()));

							Map<String, Dosen> map = perkuliahan.populateDosen();
							if (map.size() > 1) {
								String dosenPengampu = "";

								for (Dosen dosen : map.values()) {
									dosenPengampu += dosen.getNama() + ", ";
								}
								cell = row.createCell(4);
								cell.setCellValue(dosenPengampu);
							} else {
								cell = row.createCell(4);
								cell.setCellValue(perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
							}

							cell = row.createCell(5);
							cell.setCellValue(perkuliahan.getRuang() == null ? "-" : perkuliahan.getRuang().getNama());

							cell = row.createCell(6);
							cell.setCellValue(perkuliahan.getSemester()
									+ (perkuliahan.getKelas() == null || perkuliahan.getKelas().equals("") ? ""
											: " " + perkuliahan.getKelas()));

							cell = row.createCell(7);
							cell.setCellValue(countSudahDinilai);

							cell = row.createCell(8);
							cell.setCellValue(countBelumDinilai);

							cell = row.createCell(9);
							cell.setCellValue(prosentase);

							cell = row.createCell(10);
							cell.setCellValue(tanggalDirubah == null ? "" : Common.dateFormat5.get().format(tanggalDirubah));

							cell = row.createCell(11);
							cell.setCellValue(totalIps);

							cell = row.createCell(12);
							cell.setCellValue(rataIpsKelas);

							cell = row.createCell(13);
							cell.setCellValue(rataNilaiKelas);

							// -- Akumulasi ringkasan panel grafik: KPI, donat status penilaian, sebaran
							// persentase -- pakai nilai yang SAMA PERSIS dengan yang baru ditulis ke sel. --
							jumlahKelasDitampilkan++;
							totalSudahDinilaiMhs += countSudahDinilai;
							totalBelumDinilaiMhs += countBelumDinilai;
							akumulasiPersentase += prosentase;

							if (!adaPeserta) {
								statusKelas[3]++;
							} else if (countSudahDinilai == 0) {
								statusKelas[0]++;
							} else if (countBelumDinilai == 0) {
								statusKelas[2]++;
							} else {
								statusKelas[1]++;
							}

							int idxSebaran = prosentase <= 0 ? 0 : prosentase < 25 ? 1 : prosentase < 50 ? 2
									: prosentase < 75 ? 3 : prosentase < 100 ? 4 : 5;
							sebaranPersentase[idxSebaran]++;
						} finally {
							rowIndex++;
						}
					} catch (Exception e) {
						// Satu data kelas bermasalah (mis. mata kuliah/dosen/detail nilai belum lengkap)
						// tidak boleh menggagalkan seluruh proses rekap; catat & lanjut ke kelas berikutnya.
						ais.common.ErrorAuditUtil.record(e,
								"auto-guard(per-perkuliahan) src/ais/action/master/dashboard/admin/DashboardPenilaianMahasiswa.java");
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
				Desktop desktopChart = chartPanel == null ? null : chartPanel.getDesktop();
				boolean chartActivated = false;
				boolean serverPushDiaktifkanOlehChart = false;
				try {
					if (desktopChart != null && desktopChart.isAlive()) {
						if (!desktopChart.isServerPushEnabled()) {
							desktopChart.enableServerPush(true);
							serverPushDiaktifkanOlehChart = true;
						}
						Executions.activate(desktopChart);
						chartActivated = true;
					}
					renderChartRingkasan(chartPanel, jumlahKelasDitampilkan, totalSudahDinilaiMhs,
							totalBelumDinilaiMhs, akumulasiPersentase, statusKelas, sebaranPersentase);
				} catch (org.zkoss.zk.ui.DesktopUnavailableException due) {
					// User menutup halaman saat proses latar selesai. Ini kondisi normal, bukan error data.
				} catch (Exception exChart) {
					ais.common.ErrorAuditUtil.record(exChart,
							"auto-guard(chart) src/ais/action/master/dashboard/admin/DashboardPenilaianMahasiswa.java");
				} finally {
					if (chartActivated) {
						try {
							Executions.deactivate(desktopChart);
						} catch (Exception exDeactivate) {
							ais.common.ErrorAuditUtil.record(exDeactivate,
									"auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardPenilaianMahasiswa.java:chart-deactivate");
						}
					}
					if (serverPushDiaktifkanOlehChart && desktopChart != null && desktopChart.isAlive()
							&& desktopChart.isServerPushEnabled()) {
						try {
							desktopChart.enableServerPush(false);
						} catch (Exception exServerPush) {
							ais.common.ErrorAuditUtil.record(exServerPush,
									"auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardPenilaianMahasiswa.java:chart-serverpush-off");
						}
					}
				}

				perkuliahans.clear();
				label.setValue("");

				} catch (Exception exProses) {
					// Kegagalan proses (mis. query bermasalah) tidak boleh membuat indikator "sedang
					// memproses" menggantung selamanya di sisi pengguna.
					ais.common.ErrorAuditUtil.record(exProses,
							"src/ais/action/master/dashboard/admin/DashboardPenilaianMahasiswa.java:initSpreadsheet-thread");
					Common.tampilErrorJikaAdmin(exProses);
					label.setValue("");
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	/** Mengembalikan nilai {@code double} aman dari {@link Double} yang mungkin {@code null} (mis. total
	 * IP/nilai kelas yang belum pernah dihitung), agar tidak terjadi {@code NullPointerException} saat
	 * unboxing. */
	private static double angkaAman(Double nilai) {
		return nilai == null ? 0.0 : nilai.doubleValue();
	}

	/**
	 * Merender panel ringkasan visual dasbor ini sepenuhnya memakai {@code ais.ui.util.HtmlChartHelper}
	 * (HTML/CSS, bukan JFreeChart): kartu KPI (total kelas yang ditampilkan, jumlah mahasiswa sudah &amp;
	 * belum dinilai, rata-rata persentase penilaian), grafik donat komposisi kelas berdasarkan status
	 * penilaian (belum dinilai, sebagian dinilai, selesai dinilai, tanpa peserta), serta grafik batang
	 * sebaran persentase penilaian antar kelas. Seluruh parameter berasal dari akumulasi yang sudah
	 * dihitung selagi menulis sel Excel di {@link #initSpreadsheet()}, sehingga tidak ada kueri basis data
	 * tambahan yang dijalankan hanya untuk kebutuhan grafik.
	 *
	 * @param chartPanel          komponen tampilan tujuan.
	 * @param jumlahKelas         jumlah kelas perkuliahan yang berhasil ditampilkan pada rekap ini.
	 * @param totalSudahDinilai   jumlah (akumulasi) mahasiswa berstatus sudah dinilai di seluruh kelas.
	 * @param totalBelumDinilai   jumlah (akumulasi) mahasiswa berstatus belum dinilai di seluruh kelas.
	 * @param akumulasiPersentase jumlah (bukan rata-rata) persentase penilaian seluruh kelas.
	 * @param statusKelas         jumlah kelas per status: [belum dinilai, sebagian dinilai, selesai
	 *                            dinilai, tanpa peserta].
	 * @param sebaranPersentase   jumlah kelas per rentang persentase: [0%, 1-24%, 25-49%, 50-74%, 75-99%,
	 *                            100%].
	 */
	private static void renderChartRingkasan(Html chartPanel, int jumlahKelas, int totalSudahDinilai,
			int totalBelumDinilai, double akumulasiPersentase, int[] statusKelas, int[] sebaranPersentase) {

		double rataPersentase = jumlahKelas == 0 ? 0 : akumulasiPersentase / jumlahKelas;

		StringBuilder html = new StringBuilder(2048);
		html.append("<div style='padding:10px 12px 4px;display:flex;flex-direction:column;gap:14px;'>");

		html.append(ais.ui.util.HtmlChartHelper.kpiCards(
				new String[] { "Total Kelas", "Mahasiswa Sudah Dinilai", "Mahasiswa Belum Dinilai",
						"Rata-rata Persentase Dinilai" },
				new String[] { String.valueOf(jumlahKelas), String.valueOf(totalSudahDinilai),
						String.valueOf(totalBelumDinilai), Common.numberFormat.get().format(rataPersentase) + "%" },
				new String[] { "kelas perkuliahan pada rekap ini", "seluruh kelas yang ditampilkan",
						"seluruh kelas yang ditampilkan", "rata-rata antar kelas" },
				null, null, new String[] { "#2563eb", "#16a34a", "#dc2626", "#0891b2" }));

		if (jumlahKelas > 0) {
			html.append(ais.ui.util.HtmlChartHelper.donut("Komposisi Kelas Berdasarkan Status Penilaian",
					"Menunjukkan berapa banyak kelas yang belum sama sekali dinilai, sudah sebagian dinilai, "
							+ "sudah selesai dinilai semua, dan yang belum ada pesertanya, sehingga kelas yang "
							+ "perlu segera ditindaklanjuti mudah terlihat.",
					new String[] { "Belum Dinilai", "Sebagian Dinilai", "Selesai Dinilai", "Tanpa Peserta" },
					new double[] { statusKelas[0], statusKelas[1], statusKelas[2], statusKelas[3] },
					new String[] { "#dc2626", "#f59e0b", "#16a34a", "#64748b" }, jumlahKelas + " kelas"));

			html.append(ais.ui.util.HtmlChartHelper.barHorizontal("Sebaran Persentase Penilaian Antar Kelas",
					"Menunjukkan berapa banyak kelas berada pada tiap rentang persentase penilaian, dari yang "
							+ "belum tersentuh sampai yang sudah tuntas 100%, sehingga progres penilaian secara "
							+ "keseluruhan mudah dievaluasi.",
					new String[] { "0%", "1-24%", "25-49%", "50-74%", "75-99%", "100%" },
					new double[] { sebaranPersentase[0], sebaranPersentase[1], sebaranPersentase[2],
							sebaranPersentase[3], sebaranPersentase[4], sebaranPersentase[5] },
					"#2563eb"));
		}

		html.append("</div>");
		chartPanel.setContent(html.toString());
	}
}
