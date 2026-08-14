package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusPertemuan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h1>DashboardGradePenilaianMahasiswa &mdash; Rekap Kelengkapan Penilaian (Grade) per Kelas</h1>
 *
 * <p>
 * Dasbor ini dipakai oleh staf akademik, koordinator program studi, dan pimpinan fakultas untuk
 * memantau <b>sejauh mana dosen sudah menuntaskan penilaian (grade) di tiap kelas perkuliahan</b>
 * pada satu tahun akademik/semester tertentu, tanpa harus membuka detail nilai mahasiswa satu per
 * satu. Pengguna memilih kombinasi filter di panel atas (Fakultas, Prodi, Tahun Akademik, Dosen,
 * jenis semester Ganjil/Genap, semester ke berapa, Program, Jenis Ujian/UTS-UAS, dan Masa
 * Perkuliahan), menekan tombol <b>Proses</b>, dan sistem menyusun satu baris per kelas perkuliahan
 * berisi identitas kelas (kode &amp; nama matakuliah, hari, waktu, dosen, ruangan, SKS, prodi,
 * program, jumlah mahasiswa, tahun akademik, masa perkuliahan, semester, kelas) beserta jumlah
 * peserta pada tiap huruf nilai (A, B, C, dst. &mdash; diambil dinamis dari data {@code NilaiHuruf})
 * dan kolom terakhir "KOSONG/BELUM" untuk peserta yang belum dinilai sama sekali. Tiga kotak
 * centang pada toolbar ("Hanya yg belum dinilai", "Sama sekali belum dinilai", "Hanya yg telah
 * dinilai") membantu mempersempit hasil hanya ke kelas-kelas yang masih perlu ditindaklanjuti.
 * Hasilnya bisa diunduh sebagai berkas Excel lewat tombol <b>Download</b>.
 * </p>
 *
 * <h2>Alur teknis singkat</h2>
 * <p>
 * {@link #init()} membangun tata letak (form filter di region {@code North}, area hasil di region
 * {@code Center}). {@link #initSpreadsheet()} adalah method inti yang dipanggil saat tombol
 * <b>Proses</b> ditekan: karena menghitung jumlah peserta per huruf nilai untuk setiap kelas bisa
 * memakan waktu lama (satu kueri hitung tersendiri per huruf nilai per kelas), proses berat ini
 * dijalankan pada {@link Thread} latar belakang terpisah agar antarmuka ZK tidak macet/hang.
 * Selama proses berjalan, {@code Common.displayLoadBar(...)} menampilkan indikator "sedang
 * memproses" yang di-<i>polling</i> lewat komponen {@link Label} tersembunyi; begitu label
 * dikosongkan, berkas Excel yang sudah selesai ditulis ke folder sementara otomatis ditampilkan
 * sebagai tabel/grid ringan (lihat {@code ais.ui.util.PratinjauXlsxHelper#gantiSpreadsheetDenganGrid},
 * dipanggil otomatis dari dalam {@code Common.displayLoadBar}) alih-alih widget Excel penuh yang
 * berat di sisi peramban.
 * </p>
 *
 * <h2>Panel ringkasan visual (HTML/CSS, bukan JFreeChart)</h2>
 * <p>
 * Selain tabel mentah, dasbor ini juga menampilkan panel ringkasan berupa kartu KPI (jumlah kelas
 * diproses, total peserta, jumlah sudah dinilai, jumlah belum dinilai), grafik donat proporsi
 * peserta sudah vs belum dinilai, grafik batang sebaran jumlah peserta per huruf nilai di seluruh
 * kelas, dan grafik jaring laba-laba (radar) perbandingan persentase kelengkapan penilaian antar
 * program studi. Seluruh angka pada panel ini dihitung <b>bersamaan</b> dengan penulisan sel Excel
 * di dalam {@link #initSpreadsheet()} (loop yang sama, tanpa kueri basis data tambahan) lalu
 * dirender lewat {@code ais.ui.util.HtmlChartHelper} setelah loop selesai.
 * </p>
 *
 * <h2>Ketahanan terhadap data tidak lengkap</h2>
 * <p>
 * Data satu kelas perkuliahan pada praktiknya bisa tidak lengkap (matakuliah/dosen/ruangan belum
 * terhubung, dsb.). Setiap baris kelas diproses di dalam blok percobaan tersendiri: bila satu data
 * bermasalah, kesalahannya dicatat lewat {@code ais.common.ErrorAuditUtil#record} dan proses lanjut
 * ke kelas berikutnya alih-alih menghentikan seluruh rekap.
 * </p>
 *
 * <h2>Sesi Hibernate</h2>
 * <p>
 * Thread latar belakang mengambil sesi lewat {@code HibernateUtil.currentNativeSession()} (sesi
 * native, BUKAN sesi ber-scope permintaan/{@code currentSession()}), sehingga wajib ditutup secara
 * eksplisit; penutupan dilakukan tepat satu kali di blok {@code finally} agar sesi selalu tertutup
 * baik proses berhasil maupun gagal, dan tidak dipanggil berulang di banyak tempat.
 * </p>
 *
 * @author e-Campus UI Team
 */
public class DashboardGradePenilaianMahasiswa extends MyWindow {

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

	public DashboardGradePenilaianMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardGradePenilaianMahasiswa(String title, String border, boolean closable) {
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
	 * Fakultas/Prodi/Tahun Akademik/Dosen, kombo jenis semester (Ganjil/Genap) yang mengisi ulang
	 * pilihan "Semester ke" secara dinamis, kombo Program, Jenis Ujian (UTS/UAS), Masa Perkuliahan,
	 * serta tiga kotak centang untuk mempersempit hasil berdasarkan status kelengkapan penilaian,
	 * area hasil (region {@code Center}), dan tombol Proses/Download pada toolbar. Method ini hanya
	 * menyiapkan komponen UI; proses pengambilan dan perhitungan data baru dijalankan saat tombol
	 * Proses ditekan, lihat {@link #initSpreadsheet()}.
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
				+ "Menampilkan rekap kelengkapan penilaian (grade) tiap kelas perkuliahan, lengkap dengan "
				+ "jumlah peserta per huruf nilai dan yang belum dinilai, sehingga kelas mana saja yang masih "
				+ "perlu ditindaklanjuti dosennya mudah diketahui.</div>");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
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
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardGradePenilaianMahasiswa.java:334");

				}
			}
		});
		print.setParent(toolbar);

		BelumDinilai = new MyCheckboxConfig("Hanya yg belum dinilai");
		SamaSekaliBelumDinilai = new MyCheckboxConfig("Sama sekali belum dinilai");
		TelahDinilai = new MyCheckboxConfig("Hanya yg telah dinilai");
		BelumDinilai.setParent(toolbar);
		SamaSekaliBelumDinilai.setParent(toolbar);
		TelahDinilai.setParent(toolbar);

	}

	/**
	 * Method inti dasbor ini: membaca seluruh filter yang dipilih pengguna, lalu menjalankan
	 * penyusunan rekap kelengkapan penilaian pada {@link Thread} latar belakang terpisah (agar
	 * antarmuka tidak macet untuk data kelas yang banyak), menuliskannya sebagai berkas Excel
	 * sekaligus mengumpulkan ringkasan angka (total peserta, sudah/belum dinilai, sebaran huruf
	 * nilai, kelengkapan per prodi) untuk panel grafik HTML/CSS yang dirender lewat
	 * {@link #renderChartRingkasan}. Region {@code Center} dibungkus satu {@link Vlayout} berisi
	 * dua anak: panel grafik ({@code chartPanel}) dan wadah grid/Excel ({@code gridHost}) yang
	 * terpisah, sehingga saat {@code Common.displayLoadBar(...)} membersihkan dan mengisi ulang
	 * area hasil di akhir proses, panel grafik yang sudah tampil tidak ikut terhapus. Setiap baris
	 * kelas perkuliahan diproses dalam blok percobaan tersendiri: data yang bermasalah dicatat lalu
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

				rowhead.createCell(0).setCellValue("NO.");
				rowhead.createCell(1).setCellValue("KODE MATAKULIAH");
				rowhead.createCell(2).setCellValue("NAMA MATAKULIAH");
				rowhead.createCell(3).setCellValue("HARI");
				rowhead.createCell(4).setCellValue("WAKTU");
				rowhead.createCell(5).setCellValue("DOSEN");
				rowhead.createCell(6).setCellValue("RUANGAN");
				rowhead.createCell(7).setCellValue("SKS");
				rowhead.createCell(8).setCellValue("PRODI");
				rowhead.createCell(9).setCellValue("PROGRAM");
				rowhead.createCell(10).setCellValue("JUMLAH MHS");
				rowhead.createCell(11).setCellValue("TAHUN AKADEMIK");
				rowhead.createCell(12).setCellValue("MASA PERKULIAHAN");
				rowhead.createCell(13).setCellValue("SEMESTER");
				rowhead.createCell(14).setCellValue("KELAS");

				Session session = HibernateUtil.currentNativeSession();

				List<String> nilais = session.createCriteria(NilaiHuruf.class)
						.setProjection(Projections.property("nilaiHuruf")).addOrder(Order.desc("nilaiDiIPK")).list();

				List<String> n = new ArrayList<String>();
				for (String s : nilais) {
					if (!n.contains(s.trim().toUpperCase())) {
						n.add(s.trim().toUpperCase());
					}
				}

				int index = 15;
				for (String s : n) {
					rowhead.createCell(index).setCellValue(s);
					index++;
				}

				rowhead.createCell(index).setCellValue("KOSONG/BELUM");

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

				List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(masaPerkuliahan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("masaPerkuliahan", masaPerkuliahan))

						.add(dosen == null ? Restrictions.sqlRestriction("true") : criterion)

						.createAlias("jurusan", "jurusan").addOrder(Order.asc("waktuMulai"))

						.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan.fakultas", fakultas))

						.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))

						.add(program == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("program", program))

						.add(semesterKe == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", semesterKe))

						.add(tahunAkademik == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", tahunAkademik))

						.add(Restrictions.eq("ganjilGenap", semester))

						.list();

				int size = perkuliahans.size();

				int rowIndex = 1;
				int rowIndexAda = 1;

				// Akumulasi ringkasan utk panel grafik (dihitung BERSAMAAN dgn penulisan sel Excel
				// di bawah, tanpa kueri tambahan) -> dirender lewat renderChartRingkasan setelah
				// loop selesai.
				final Map<String, int[]> sebaranNilaiHuruf = new LinkedHashMap<String, int[]>();
				final Map<String, double[]> kelengkapanPerJurusan = new LinkedHashMap<String, double[]>();
				int kelasDiproses = 0;
				long totalPeserta = 0;
				long totalSudahDinilai = 0;
				long totalBelumDinilai = 0;

				for (Perkuliahan perkuliahan : perkuliahans) {

					label.setValue("Sedang memproses data " + perkuliahan.toString() + " ("
							+ Common.numberFormat.get().format(rowIndexAda * 100.0 / size) + " %)");
					rowIndexAda++;

					Integer countSudahDinilai = ((Number) session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.isNull("ikutiPerkuliahan"))

							.add(statusPertemuan == null ? Restrictions.gt("totalNilai", 1.0)
									: Restrictions.and(
											Restrictions.or(
													Restrictions.ilike("detailNilai", statusPertemuan.getId() + ",",
															MatchMode.START),
													Restrictions.ilike("detailNilai",
															";" + statusPertemuan.getId() + ",", MatchMode.ANYWHERE)),

											Restrictions.not(Restrictions.or(
													Restrictions.ilike("detailNilai", statusPertemuan.getId() + ",0",
															MatchMode.START),
													Restrictions.ilike("detailNilai",
															";" + statusPertemuan.getId() + ",0", MatchMode.ANYWHERE))))

							)

							.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
							.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
							.uniqueResult()).intValue();
					if (telahDinilai && countSudahDinilai.equals(0)) {
						continue;
					}

					Integer countTotal = ((Number) session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.isNull("ikutiPerkuliahan"))
							.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
							.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
							.uniqueResult()).intValue();

					Integer countBelumDinilai = countTotal - countSudahDinilai;

					if (belumDinilai && countBelumDinilai.equals(0)) {
						continue;
					}

					if (samaSekaliBelumDinilai && !countBelumDinilai.equals(countTotal)) {
						continue;
					}

					if (telahDinilai && countSudahDinilai.equals(0)) {
						continue;
					}

					try {

					int totalPesertaAwal = countTotal;

					// -- Akumulasi ringkasan panel grafik: KPI (kelas diproses, total peserta,
					// sudah/belum dinilai) dan kelengkapan penilaian per prodi (utk radar). --
					kelasDiproses++;
					totalPeserta += totalPesertaAwal;
					totalSudahDinilai += countSudahDinilai;
					totalBelumDinilai += countBelumDinilai;

					String namaJurusanKursus = perkuliahan.getJurusan() == null ? ""
							: perkuliahan.getJurusan().getNama();
					if (!namaJurusanKursus.isEmpty()) {
						double[] agg = kelengkapanPerJurusan.get(namaJurusanKursus);
						if (agg == null) {
							agg = new double[2];
							kelengkapanPerJurusan.put(namaJurusanKursus, agg);
						}
						agg[0] += countSudahDinilai;
						agg[1] += totalPesertaAwal;
					}

					XSSFRow row = sheet.createRow(rowIndex);
					XSSFCell cell = row.createCell(0);
					cell.setCellValue((rowIndex));

					cell = row.createCell(1);
					cell.setCellValue(perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());

					cell = row.createCell(2);
					cell.setCellValue(perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());

					cell = row.createCell(3);
					cell.setCellValue(perkuliahan.getHari());

					cell = row.createCell(4);
					cell.setCellValue((perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()) + "-"
							+ (perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()));

					Map<String, Dosen> map = perkuliahan.populateDosen();
					if (map.size() > 1) {
						String dosenPengampu = "";

						for (Dosen dosen : map.values()) {
							dosenPengampu += dosen.getNama() + ", ";
						}
						cell = row.createCell(5);
						cell.setCellValue(dosenPengampu);
					} else {
						cell = row.createCell(5);
						cell.setCellValue(perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
					}

					cell = row.createCell(6);
					cell.setCellValue(perkuliahan.getRuang() == null ? "-" : perkuliahan.getRuang().getNama());

					cell = row.createCell(7);
					cell.setCellValue(perkuliahan.getMatakuliah() == null ? 0
							: angkaAman(perkuliahan.getMatakuliah().getSks()));

					cell = row.createCell(8);
					cell.setCellValue(perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());

					cell = row.createCell(9);
					cell.setCellValue(perkuliahan.getProgram() == null ? "" : perkuliahan.getProgram());

					cell = row.createCell(10);
					cell.setCellValue(countTotal);

					cell = row.createCell(11);
					cell.setCellValue(perkuliahan.getTahunAjaran());

					cell = row.createCell(12);
					cell.setCellValue(
							perkuliahan.getMasaPerkuliahan() == null ? "" : perkuliahan.getMasaPerkuliahan().getNama());

					cell = row.createCell(13);
					cell.setCellValue(angkaAman(perkuliahan.getSemester()));

					cell = row.createCell(14);
					cell.setCellValue(perkuliahan.getKelas());

					index = 15;
					for (String s : n) {
						countTotal = ((Number) session.createCriteria(Detailperkuliahan.class)
								.add(Restrictions.ilike("nilaiHuruf", s, MatchMode.EXACT))
								.add(Restrictions.gt("totalNilai", 0.1)).add(Restrictions.isNull("ikutiPerkuliahan"))
								.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
								.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
								.uniqueResult()).intValue();
						cell = row.createCell(index);
						cell.setCellValue(countTotal);
						index++;

						int[] accHuruf = sebaranNilaiHuruf.get(s);
						if (accHuruf == null) {
							accHuruf = new int[1];
							sebaranNilaiHuruf.put(s, accHuruf);
						}
						accHuruf[0] += countTotal;
					}

					countTotal = ((Number) session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.or(Restrictions.isNull("totalNilai"), Restrictions.lt("totalNilai", 0.1)))
							.add(Restrictions.isNull("ikutiPerkuliahan"))
							.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
							.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
							.uniqueResult()).intValue();
					cell = row.createCell(index);
					cell.setCellValue(countTotal);

					int[] accKosong = sebaranNilaiHuruf.get("KOSONG/BELUM");
					if (accKosong == null) {
						accKosong = new int[1];
						sebaranNilaiHuruf.put("KOSONG/BELUM", accKosong);
					}
					accKosong[0] += countTotal;

					} catch (Exception exBaris) {
						// Satu data kelas bermasalah (mis. matakuliah/dosen/ruangan belum terhubung)
						// tidak boleh menggagalkan seluruh proses rekap; catat & lanjut.
						ais.common.ErrorAuditUtil.record(exBaris,
								"auto-guard(per-kursus) src/ais/action/master/dashboard/admin/DashboardGradePenilaianMahasiswa.java");
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
					renderChartRingkasan(chartPanel, kelasDiproses, totalPeserta, totalSudahDinilai,
							totalBelumDinilai, sebaranNilaiHuruf, kelengkapanPerJurusan);
				} catch (Exception exChart) { ais.common.ErrorAuditUtil.record(exChart,
						"auto-guard(chart) src/ais/action/master/dashboard/admin/DashboardGradePenilaianMahasiswa.java"); }

				perkuliahans.clear();
				label.setValue("");

				} catch (Exception exProses) {
					// Kegagalan proses (mis. query bermasalah) tidak boleh membuat indikator "sedang
					// memproses" menggantung selamanya di sisi pengguna.
					ais.common.ErrorAuditUtil.record(exProses,
							"src/ais/action/master/dashboard/admin/DashboardGradePenilaianMahasiswa.java:initSpreadsheet-thread");
					Common.tampilErrorJikaAdmin(exProses);
					label.setValue("");
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	/**
	 * Mengembalikan nilai {@code int} aman dari {@link Integer} yang mungkin {@code null} (mis.
	 * SKS atau semester yang belum terisi lengkap pada data matakuliah/kelas), agar tidak terjadi
	 * {@code NullPointerException} saat unboxing menuju {@code XSSFCell#setCellValue}.
	 */
	private static int angkaAman(Integer nilai) {
		return nilai == null ? 0 : nilai.intValue();
	}

	/**
	 * Merender panel ringkasan visual dasbor ini sepenuhnya memakai {@code ais.ui.util.HtmlChartHelper}
	 * (HTML/CSS, bukan JFreeChart): kartu KPI (jumlah kelas diproses, total peserta, jumlah sudah
	 * dinilai, jumlah belum dinilai), grafik donat proporsi peserta sudah vs belum dinilai, grafik
	 * batang sebaran jumlah peserta per huruf nilai (termasuk "KOSONG/BELUM") di seluruh kelas yang
	 * direkap, serta grafik radar (jaring laba-laba) perbandingan persentase kelengkapan penilaian
	 * antar program studi (hanya bila datanya mencakup minimal 3 program studi agar bentuk jaring
	 * bermakna). Seluruh parameter berasal dari akumulasi yang sudah dihitung selagi menulis sel
	 * Excel di {@link #initSpreadsheet()}, sehingga tidak ada kueri basis data tambahan yang
	 * dijalankan hanya untuk kebutuhan grafik.
	 *
	 * @param chartPanel            komponen tampilan tujuan.
	 * @param kelasDiproses         jumlah kelas perkuliahan yang berhasil masuk rekap (lolos filter).
	 * @param totalPeserta          jumlah total peserta (mahasiswa) pada seluruh kelas yang direkap.
	 * @param totalSudahDinilai     jumlah peserta yang sudah dinilai pada seluruh kelas yang direkap.
	 * @param totalBelumDinilai     jumlah peserta yang belum dinilai pada seluruh kelas yang direkap.
	 * @param sebaranNilaiHuruf     peta huruf nilai (termasuk "KOSONG/BELUM") -&gt; jumlah peserta
	 *                              (untuk grafik batang sebaran).
	 * @param kelengkapanPerJurusan peta nama program studi -&gt; {jumlah sudah dinilai, jumlah total
	 *                              peserta} (untuk radar persentase kelengkapan penilaian).
	 */
	private static void renderChartRingkasan(Html chartPanel, int kelasDiproses, long totalPeserta,
			long totalSudahDinilai, long totalBelumDinilai, Map<String, int[]> sebaranNilaiHuruf,
			Map<String, double[]> kelengkapanPerJurusan) {

		double persenSudahDinilai = totalPeserta == 0 ? 0 : (totalSudahDinilai * 100.0 / totalPeserta);

		StringBuilder html = new StringBuilder(4096);
		html.append("<div style='padding:10px 12px 4px;display:flex;flex-direction:column;gap:14px;'>");

		html.append(ais.ui.util.HtmlChartHelper.kpiCards(
				new String[] { "Kelas Diproses", "Total Peserta", "Sudah Dinilai", "Belum Dinilai" },
				new String[] { String.valueOf(kelasDiproses), String.valueOf(totalPeserta),
						String.valueOf(totalSudahDinilai), String.valueOf(totalBelumDinilai) },
				new String[] { "kelas perkuliahan pada rekap ini", "mahasiswa peserta kelas",
						Common.numberFormat.get().format(persenSudahDinilai) + "% dari total peserta",
						"masih perlu dinilai dosen" },
				null, null, new String[] { "#2563eb", "#0891b2", "#16a34a", "#dc2626" }));

		if (totalPeserta > 0) {
			html.append(ais.ui.util.HtmlChartHelper.donut("Status Penilaian Peserta",
					"Menunjukkan berapa persen peserta di seluruh kelas yang direkap sudah dinilai dosennya "
							+ "dan berapa persen yang masih menunggu penilaian.",
					new String[] { "Sudah Dinilai", "Belum Dinilai" },
					new double[] { totalSudahDinilai, totalBelumDinilai }, new String[] { "#16a34a", "#dc2626" },
					Common.numberFormat.get().format(persenSudahDinilai) + "%"));
		}

		if (!sebaranNilaiHuruf.isEmpty()) {
			String[] labelHuruf = sebaranNilaiHuruf.keySet().toArray(new String[sebaranNilaiHuruf.size()]);
			double[] nilaiHuruf = new double[labelHuruf.length];
			for (int i = 0; i < labelHuruf.length; i++) {
				nilaiHuruf[i] = sebaranNilaiHuruf.get(labelHuruf[i])[0];
			}
			html.append(ais.ui.util.HtmlChartHelper.barHorizontal("Sebaran Nilai Huruf Seluruh Peserta",
					"Menunjukkan berapa banyak peserta yang mendapat tiap huruf nilai (A, B, C, dan seterusnya) "
							+ "serta yang belum dinilai sama sekali, dijumlahkan dari seluruh kelas pada rekap ini.",
					labelHuruf, nilaiHuruf, "#2563eb"));
		}

		if (kelengkapanPerJurusan.size() >= 3) {
			String[] axesJurusan = kelengkapanPerJurusan.keySet().toArray(new String[kelengkapanPerJurusan.size()]);
			double[] persenJurusan = new double[axesJurusan.length];
			for (int i = 0; i < axesJurusan.length; i++) {
				double[] agg = kelengkapanPerJurusan.get(axesJurusan[i]);
				persenJurusan[i] = agg[1] == 0 ? 0 : (agg[0] * 100.0 / agg[1]);
			}
			html.append(ais.ui.util.HtmlChartHelper.radar("Kelengkapan Penilaian Antar Program Studi",
					"Membandingkan persentase peserta yang sudah dinilai antar program studi sekaligus dalam "
							+ "satu tampilan, sehingga program studi yang penilaiannya masih tertinggal mudah "
							+ "terlihat.",
					axesJurusan, new String[] { "% Sudah Dinilai" }, new double[][] { persenJurusan },
					new String[] { "#7c3aed" }, 100.0));
		}

		html.append("</div>");
		chartPanel.setContent(html.toString());
	}
}
