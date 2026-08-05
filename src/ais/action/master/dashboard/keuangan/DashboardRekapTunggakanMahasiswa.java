package ais.action.master.dashboard.keuangan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
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

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusAwalMahasiswa;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h1>DashboardRekapTunggakanMahasiswa &mdash; Rekapitulasi Tunggakan Pembayaran Mahasiswa</h1>
 *
 * <p>
 * Dasbor ini dipakai oleh staf keuangan/kasir dan pimpinan untuk <b>menagih dan memantau tunggakan
 * (kekurangan pembayaran) mahasiswa</b> per kegiatan pembayaran (mis. UKT, her-registrasi, wisuda, dsb.),
 * tanpa harus membuka data pembayaran satu per satu. Pengguna memilih kombinasi filter di panel atas
 * (Fakultas, Prodi, Program, Status Awal, Angkatan, Semester, Tahun Akademik, Jenis Pembayaran, nama/NIM
 * mahasiswa, atau Kelas), menekan tombol <b>Proses</b>, dan sistem menyusun satu baris per kegiatan
 * pembayaran yang masih memiliki nilai tagihan/dibayar berisi identitas mahasiswa, kontak, jenis
 * pembayaran, semester, tahun akademik, status lunas, nominal tagihan/dibayar, serta persentase lunas.
 * Hasilnya bisa diunduh sebagai berkas Excel lewat tombol <b>Download</b>.
 * </p>
 *
 * <h2>Alur teknis singkat</h2>
 * <p>
 * {@link #init()} membangun tata letak (form filter di region {@code North}, area hasil di region
 * {@code Center}) beserta tombol Proses/Download pada toolbar. {@link #initSpreadsheet()} adalah method
 * inti yang dipanggil saat tombol <b>Proses</b> ditekan: karena data {@code Kegiatan} pembayaran bisa
 * sangat banyak, penyusunan rekap dijalankan pada {@link Thread} latar belakang terpisah agar antarmuka
 * ZK tidak macet/hang menunggu. Selama proses berjalan, {@code Common.displayLoadBar(...)} menampilkan
 * indikator "sedang memproses" yang di-<i>polling</i> lewat komponen {@link Label} tersembunyi; begitu
 * label dikosongkan, berkas Excel yang sudah selesai ditulis ke folder sementara otomatis ditampilkan
 * sebagai tabel/grid ringan (lihat {@code ais.ui.util.PratinjauXlsxHelper}) yang dijalankan secara
 * internal oleh {@code LoadBarUtils.loadSpreadsheet}, alih-alih widget Excel penuh yang berat di sisi
 * peramban.
 * </p>
 *
 * <h2>Panel ringkasan visual (HTML/CSS, bukan JFreeChart)</h2>
 * <p>
 * Selain tabel mentah, dasbor ini juga menampilkan panel ringkasan berupa kartu KPI (total tagihan,
 * total dibayar, total tunggakan, jumlah kegiatan yang belum lunas), grafik donat komposisi
 * lunas/belum lunas, grafik batang sebaran tunggakan per jenis pembayaran, dan grafik garis tren
 * tagihan/dibayar/tunggakan per semester (bila data mencakup lebih dari satu semester). Seluruh angka
 * pada panel ini dihitung <b>bersamaan</b> dengan penulisan sel Excel di dalam {@link #initSpreadsheet()}
 * (loop yang sama, tanpa kueri basis data tambahan) lalu dirender lewat {@code ais.ui.util.HtmlChartHelper}
 * setelah loop selesai, dibungkus percobaan/tangkapan tersendiri supaya kegagalan menggambar grafik tidak
 * pernah mengganggu penulisan Excel maupun tampilan grid.
 * </p>
 *
 * <h2>Ketahanan terhadap data tidak lengkap</h2>
 * <p>
 * Data kegiatan pembayaran pada praktiknya bisa tidak lengkap (biodata mahasiswa belum lengkap, jenis
 * kegiatan sudah dihapus, dsb.). Setiap kegiatan diproses di dalam blok percobaan tersendiri: bila satu
 * data bermasalah, kesalahannya dicatat lewat {@code ais.common.ErrorAuditUtil#record} dan proses lanjut
 * ke kegiatan berikutnya alih-alih menghentikan seluruh rekap.
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
public class DashboardRekapTunggakanMahasiswa extends MyWindow {

	/**
	 *
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox angkatanMhsMulai = new Combobox();private Combobox angkatanMhs = new Combobox();
	private Combobox searchStatusAwalMahasiswa;
	private Textbox mahasiswa;
	private Combobox semester = new Combobox();
	private Center center = new Center();

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	private Combobox jenisPembayaran;

	private File file;

	private Combobox tahunAkademik = new Combobox();

	private Textbox kelas;

	public DashboardRekapTunggakanMahasiswa() {
		super();
		try {
			initFakultas();
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapTunggakanMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			initFakultas();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun seluruh tata letak dasbor: panel filter (region {@code North}) berisi kombo
	 * Fakultas/Prodi/Program/Status Awal/Angkatan/Semester/Tahun Akademik/Jenis Pembayaran, kotak teks
	 * Mahasiswa/Kelas untuk pencarian bebas, deskripsi ringkas dasbor, serta area hasil (region
	 * {@code Center}) dan tombol Proses/Download pada toolbar. Method ini hanya menyiapkan komponen UI;
	 * proses pengambilan dan perhitungan data baru dijalankan saat tombol Proses ditekan, lihat
	 * {@link #initSpreadsheet()}.
	 */
	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		jenisPembayaran = new Combobox();
		Common.insertComboDanSemua(jenisPembayaran, "namaKegiatan", JenisKegiatan.class,
				Restrictions.eq("aktif", true));
		jenisPembayaran.setReadonly(true);

		for (int i = 1; i < 32; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}
		semester.setReadonly(true);
		Comboitem comboitem = new Comboitem();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		semester.appendChild(comboitem);
		semester.setSelectedItem(comboitem);

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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow descRow = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(descRow, "10");
		descRow.setParent(rows);
		Html deskripsiDasbor = new Html();
		deskripsiDasbor.setContent("<div style='font-size:13px;color:#475569;padding:2px 4px 8px;'>"
				+ "Menampilkan daftar tunggakan (kekurangan pembayaran) tiap mahasiswa per jenis pembayaran, "
				+ "semester, dan tahun akademik, lengkap dengan ringkasan grafik agar tunggakan yang masih "
				+ "harus ditagih mudah dipantau tanpa membuka data satu per satu.</div>");
		deskripsiDasbor.setParent(descRow);

		MyFormRow row = new MyFormRow();
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

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		row.appendChild(searchStatusAwalMahasiswa = new Combobox());
		Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(searchStatusAwalMahasiswa, null);
		searchStatusAwalMahasiswa.setCols(4);
		searchStatusAwalMahasiswa.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatanMhs);
		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 10; i <= ais.ui.util.WaktuUtil
				.getCalendar().get(Calendar.YEAR) + 10; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			angkatanMhs.appendChild(comboitem);
		}
		angkatanMhs.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semester);
		row.setParent(rows);
		semester.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisPembayaran);
		row.setParent(rows);
		jenisPembayaran.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(mahasiswa = new Textbox());
		mahasiswa.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new Textbox());
		kelas.setWidth("90%");

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "10");
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
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Data Mahasiswa.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DashboardRekapTunggakanMahasiswa.java:254");

				}
			}
		});
		print.setParent(toolbar);

	}

	/**
	 * Method inti dasbor ini: membaca seluruh filter yang dipilih pengguna, lalu menjalankan penyusunan
	 * rekap tunggakan pembayaran mahasiswa pada {@link Thread} latar belakang terpisah (agar antarmuka
	 * tidak macet untuk data yang banyak), menuliskannya sebagai berkas Excel sekaligus mengumpulkan
	 * ringkasan angka (total tagihan/dibayar/tunggakan, status lunas, sebaran per jenis pembayaran, tren
	 * per semester) untuk panel grafik HTML/CSS yang dirender lewat {@link #renderChartRingkasan}. Region
	 * {@code Center} dibungkus satu {@link Vlayout} berisi dua anak: panel grafik ({@code chartPanel})
	 * dan wadah grid/Excel ({@code gridHost}) yang terpisah, sehingga saat
	 * {@code Common.displayLoadBar(...)} membersihkan dan mengisi ulang area hasil di akhir proses, panel
	 * grafik yang sudah tampil tidak ikut terhapus. Setiap kegiatan pembayaran diproses dalam blok
	 * percobaan tersendiri: data yang bermasalah dicatat lalu dilewati, tidak menggagalkan seluruh rekap.
	 * Sesi Hibernate native ditutup satu kali di blok {@code finally}.
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

		final Integer semesterKe = (Integer) (semester.getSelectedItem() == null ? null
				: semester.getSelectedItem().getValue());
		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Integer angkatan = (Integer) (angkatanMhs.getSelectedItem() == null ? null
				: angkatanMhs.getSelectedItem().getValue());

		final JenisKegiatan jenisKegiatan = (JenisKegiatan) (this.jenisPembayaran.getSelectedItem() == null ? null
				: this.jenisPembayaran.getSelectedItem().getValue());

		if (tahunAkademik == null) {
			return;
		}

		final StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa
				.getSelectedItem() == null ? null : searchStatusAwalMahasiswa.getSelectedItem().getValue());

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		(file = new File(filename)).createNewFile();

		final String nama = mahasiswa.getValue().trim();
		final String kls = kelas.getValue().trim();

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

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("BAYAR");
				sheet.setDefaultColumnWidth(10);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM/NO REG");
				rowhead.createCell(1).setCellValue("NAMA");
				rowhead.createCell(2).setCellValue("TELP/HP");
				rowhead.createCell(3).setCellValue("EMAIL");
				rowhead.createCell(4).setCellValue("ALAMAT");
				rowhead.createCell(5).setCellValue("JENIS PEMBAYARAN");
				rowhead.createCell(6).setCellValue("SEMESTER");
				rowhead.createCell(7).setCellValue("TA");
				rowhead.createCell(8).setCellValue("LUNAS");
				rowhead.createCell(9).setCellValue("TAGIHAN");
				rowhead.createCell(10).setCellValue("DIBAYAR");
				rowhead.createCell(11).setCellValue("PROSENTASE");

				rowhead.createCell(12).setCellValue("TAGIHAN RINCI");
				rowhead.createCell(13).setCellValue("DIBAYAR RINCI");

				Session session = null;
				List<Kegiatan> kegiatans = null;
				try {
					session = HibernateUtil.currentNativeSession();

					kegiatans = session.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true))
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))

						.add(semesterKe == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("semster", semesterKe))

						.add(jenisKegiatan == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenisKegiatan", jenisKegiatan))

						.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAkademik", tahunAkademik))

						.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
						.createAlias("calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)

						.add(kls.isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("mahasiswa.kelas", kls, MatchMode.ANYWHERE))

						.add(nama.isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.or(
												Restrictions.ilike("calonMahasiswa.noRegistrasi", nama,
														MatchMode.ANYWHERE),
												Restrictions.ilike("calonMahasiswa.nama", nama, MatchMode.ANYWHERE)),
										Restrictions.or(Restrictions.ilike("mahasiswa.nim", nama, MatchMode.ANYWHERE),
												Restrictions.ilike("mahasiswa.nama", nama, MatchMode.ANYWHERE))))

						.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("mahasiswa.statusAwalMahasiswa", statusAwalMahasiswa),
										Restrictions.eq("calonMahasiswa.statusAwalMahasiswa", statusAwalMahasiswa)))

						.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
						.createAlias("calonMahasiswa.prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)

						.addOrder(Order.asc("mahasiswa.nim")).addOrder(Order.asc("calonMahasiswa.noRegistrasi"))

						.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.eq("jurusan.fakultas", fakultas),
										Restrictions.eq("prodiLulus.fakultas", fakultas)))

						.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.eq("mahasiswa.jurusan", jurusan),
										Restrictions.eq("calonMahasiswa.prodiLulus", jurusan)))

						.add(program == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.eq("mahasiswa.program", program),
										Restrictions.eq("calonMahasiswa.program", program)))

						.add(angkatan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.eq("mahasiswa.tahunangkatan", angkatan),
										Restrictions.eq("calonMahasiswa.tahun", angkatan)))

						.list();

				int size = kegiatans.size();

				int rowIndex = 1;
				int rowIndexData = 1;
				int rowIndexMhs = 1;

				Double totalTagihan = 0.0;
				Double totalDibayar = 0.0;
				Double totalTunggakan = 0.0;
				Double totalpersen = 0.0;

				// Akumulasi ringkasan utk panel grafik (dihitung BERSAMAAN dgn penulisan sel Excel di
				// bawah, tanpa kueri tambahan) -> dirender lewat renderChartRingkasan setelah loop selesai.
				final Map<String, double[]> tunggakanPerJenis = new LinkedHashMap<String, double[]>();
				final Map<Integer, double[]> rekapPerSemester = new TreeMap<Integer, double[]>();
				int jumlahLunas = 0;
				int jumlahBelumLunas = 0;

				for (Kegiatan kegiatan : kegiatans) {

					try {

						label.setValue("Sedang memproses data " + kegiatan.toString() + " ("
								+ Common.numberFormat.get().format(rowIndexMhs * 100.0 / size) + " %)");
						rowIndexMhs++;

						Double tag = kegiatan.getTagihan();
						Double byr = kegiatan.getDibayar();

						if (Math.abs(tag + byr) > 0.1) {

							try {

								Mahasiswa mahasiswa = kegiatan.getMahasiswa();
								BiodataCalonMahasiswa biodataCalonMahasiswa = kegiatan.getCalonMahasiswa();

								XSSFRow row = sheet.createRow(rowIndexData);
								XSSFCell cell = row.createCell(0);
								cell.setCellValue(mahasiswa != null ? mahasiswa.getNim()
										: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNoRegistrasi() : "");

								cell = row.createCell(1);
								cell.setCellValue(mahasiswa != null ? mahasiswa.getNama()
										: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNama() : "");

								if (mahasiswa != null) {
									// ambilBiodata() bisa null (mis. mahasiswa baru yg biodatanya belum
									// tersimpan) -> guard di sini agar tak NPE sebelum sempat masuk try di bawah.
									BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
									if (biodataMahasiswa == null) {
										cell = row.createCell(2);
										cell.setCellValue(mahasiswa.getTelp());
									} else {
										Object[] hp = new Object[] { biodataMahasiswa.getHp(),
												biodataMahasiswa.getTeleponRumah() };
										try {
											String hps = (hp[0] == null
													|| hp[0].toString().trim().equals("08100000000000000000")
													|| hp[0].toString().trim().equals("0000000000") ? "" : hp[0])
													+ (hp[1] == null || hp[1].toString().trim().isEmpty()
															|| hp[1].toString().trim().equals("00000000000000000000")
															|| hp[1].toString().trim().equals("000000000")
																	? ""
																	: (hp[0] == null || hp[0].toString().trim().isEmpty()
																			|| hp[0].toString().trim()
																					.equals("08100000000000000000")
																			|| hp[0].toString().trim().equals("0000000000") ? ""
																					: " / ")
																			+ hp[1]);

											cell = row.createCell(2);
											cell.setCellValue(hps);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											cell = row.createCell(2);
											cell.setCellValue(mahasiswa.getTelp());
										}
									}

									cell = row.createCell(3);
									cell.setCellValue(mahasiswa.getEmail());

									cell = row.createCell(4);
									cell.setCellValue(mahasiswa.getAlamat());
								} else if (biodataCalonMahasiswa != null) {
									cell = row.createCell(2);
									cell.setCellValue(biodataCalonMahasiswa.getTeleponRumah());

									cell = row.createCell(3);
									cell.setCellValue(biodataCalonMahasiswa.getEmail());

									cell = row.createCell(4);
									cell.setCellValue(biodataCalonMahasiswa.getAlamat());
								}

								// getJenisKegiatan() bisa null bila data jenis kegiatan sudah dihapus/tidak
								// konsisten -> guard agar tak NPE saat mengambil nama kegiatan.
								JenisKegiatan jenisKegiatanBaris = kegiatan.getJenisKegiatan();
								String namaJenis = jenisKegiatanBaris == null ? "" : jenisKegiatanBaris.getNamaKegiatan();
								cell = row.createCell(5);
								cell.setCellValue(namaJenis);

								cell = row.createCell(6);
								cell.setCellValue(kegiatan.getSemster());

								cell = row.createCell(7);
								cell.setCellValue(kegiatan.getTahunAkademik());

								cell = row.createCell(8);
								cell.setCellValue((kegiatan.getApakahLunas() ? "Ya" : "Tidak"));

								cell = row.createCell(9);
								cell.setCellValue(Common.numberFormat.get().format(tag));

								cell = row.createCell(10);
								cell.setCellValue(Common.numberFormat.get().format(byr));

								cell = row.createCell(11);
								cell.setCellValue(Common.numberFormat.get().format(kegiatan.getPersentaseLunas()) + "%");

								cell = row.createCell(12);
								cell.setCellValue(kegiatan.getTagihans());

								cell = row.createCell(13);
								cell.setCellValue(kegiatan.getBulans());

								// -- Akumulasi ringkasan panel grafik: sebaran tunggakan per jenis pembayaran,
								// dari baris detail yg sama yg baru saja ditulis ke Excel di atas. --
								String kunciJenis = namaJenis.isEmpty() ? "(Tanpa Jenis)" : namaJenis;
								double[] aggJenis = tunggakanPerJenis.get(kunciJenis);
								if (aggJenis == null) {
									aggJenis = new double[1];
									tunggakanPerJenis.put(kunciJenis, aggJenis);
								}
								aggJenis[0] += kegiatan.getAmountTerhutang();

							} finally {
								// Selalu maju walau baris ini gagal di tengah jalan, agar baris berikutnya
								// yg berhasil tidak menimpa baris yg gagal tsb.
								rowIndexData++;
							}
						}

						Double amount = kegiatan.getAmount();
						Double amountTerhutang = kegiatan.getAmountTerhutang();

						totalTagihan += amount + amountTerhutang;
						totalDibayar += amount;
						totalTunggakan += amountTerhutang;
						totalpersen += amountTerhutang;

						// -- Akumulasi ringkasan panel grafik: komposisi lunas/belum lunas & tren
						// tagihan/dibayar/tunggakan per semester, dihitung utk SELURUH kegiatan (sejajar
						// dgn total yg sudah dihitung di atas, bukan hanya yg tampil sbg baris detail). --
						if (Boolean.TRUE.equals(kegiatan.getApakahLunas())) {
							jumlahLunas++;
						} else {
							jumlahBelumLunas++;
						}

						Integer smt = kegiatan.getSemster();
						if (smt != null) {
							double[] aggSmt = rekapPerSemester.get(smt);
							if (aggSmt == null) {
								aggSmt = new double[3]; // {tagihan, dibayar, tunggakan}
								rekapPerSemester.put(smt, aggSmt);
							}
							aggSmt[0] += amount + amountTerhutang;
							aggSmt[1] += amount;
							aggSmt[2] += amountTerhutang;
						}

					} catch (Exception exBaris) {
						// Satu data kegiatan bermasalah (mis. biodata/jenis kegiatan tidak lengkap) tidak
						// boleh menggagalkan seluruh proses rekap; catat & lanjut ke kegiatan berikutnya.
						ais.common.ErrorAuditUtil.record(exBaris,
								"auto-guard(per-kegiatan) src/ais/action/master/dashboard/keuangan/DashboardRekapTunggakanMahasiswa.java");
					} finally {
						rowIndex++;
					}
				}

				XSSFRow row = sheet.createRow(rowIndex);

				XSSFCell cell = row.createCell(4);
				cell.setCellValue("Total Semua");

				cell = row.createCell(5);
				cell.setCellValue(Common.numberFormat.get().format(totalTagihan));

				cell = row.createCell(6);
				cell.setCellValue(Common.numberFormat.get().format(totalDibayar));

				cell = row.createCell(7);
				cell.setCellValue(Common.numberFormat.get().format(totalTunggakan));

				cell = row.createCell(8);
				cell.setCellValue(Common.numberFormat.get().format(persenAman(totalDibayar, totalTagihan)) + "%");

				Common.setStyled(sheet);
				sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				// -- Render panel grafik (HTML/CSS, BUKAN JFreeChart) dari ringkasan yang sudah
				// dihitung di loop di atas. Kegagalan menggambar grafik tidak boleh mengganggu
				// Excel/grid yang sudah berhasil ditulis. --
				try {
					renderChartRingkasan(chartPanel, totalTagihan, totalDibayar, totalTunggakan, jumlahLunas,
							jumlahBelumLunas, tunggakanPerJenis, rekapPerSemester);
				} catch (Exception exChart) {
					ais.common.ErrorAuditUtil.record(exChart,
							"auto-guard(chart) src/ais/action/master/dashboard/keuangan/DashboardRekapTunggakanMahasiswa.java");
				}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				} finally {
					if (session != null) {
						try {
							HibernateUtil.closeSession();
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
					if (kegiatans != null) {
						try {
							kegiatans.clear();
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
					try {
						label.setValue("");
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		}).start();

	}

	/**
	 * Menghitung persentase {@code bagian} terhadap {@code total} secara aman: bila {@code total}
	 * mendekati nol (mis. tidak ada tagihan sama sekali pada hasil rekap), dianggap 100% lunas &mdash;
	 * selaras dengan konvensi yang sama dipakai {@code Kegiatan#getPersentase()} &mdash; sehingga tidak
	 * pernah menghasilkan pembagian oleh nol / nilai "NaN%" pada baris Total Semua.
	 *
	 * @param bagian nilai pembilang (mis. total dibayar).
	 * @param total  nilai penyebut (mis. total tagihan).
	 * @return persentase 0-100 yang aman dari pembagian oleh nol.
	 */
	private static double persenAman(double bagian, double total) {
		return total < 0.01 ? 100.0 : (bagian * 100.0 / total);
	}

	/**
	 * Merender panel ringkasan visual dasbor ini sepenuhnya memakai {@code ais.ui.util.HtmlChartHelper}
	 * (HTML/CSS, bukan JFreeChart): kartu KPI (total tagihan, total dibayar, total tunggakan, jumlah
	 * kegiatan yang belum lunas), grafik donat komposisi lunas/belum lunas, grafik batang sebaran
	 * tunggakan per jenis pembayaran, serta grafik garis tren tagihan/dibayar/tunggakan per semester
	 * (hanya bila data mencakup lebih dari satu semester agar tren bermakna). Seluruh parameter berasal
	 * dari akumulasi yang sudah dihitung selagi menulis sel Excel di {@link #initSpreadsheet()}, sehingga
	 * tidak ada kueri basis data tambahan yang dijalankan hanya untuk kebutuhan grafik.
	 *
	 * @param chartPanel        komponen tampilan tujuan.
	 * @param totalTagihan      jumlah seluruh nilai tagihan kegiatan yang berhasil diproses.
	 * @param totalDibayar      jumlah seluruh nilai yang sudah dibayar dari kegiatan yang berhasil diproses.
	 * @param totalTunggakan    jumlah seluruh kekurangan pembayaran (tunggakan) yang masih harus ditagih.
	 * @param jumlahLunas       jumlah kegiatan yang berstatus lunas.
	 * @param jumlahBelumLunas  jumlah kegiatan yang belum lunas (masih memiliki tunggakan).
	 * @param tunggakanPerJenis peta nama jenis pembayaran -&gt; {jumlah tunggakan} (untuk grafik batang).
	 * @param rekapPerSemester  peta semester ke -&gt; {tagihan, dibayar, tunggakan} terurut menaik (untuk
	 *                          grafik tren).
	 */
	private static void renderChartRingkasan(Html chartPanel, double totalTagihan, double totalDibayar,
			double totalTunggakan, int jumlahLunas, int jumlahBelumLunas, Map<String, double[]> tunggakanPerJenis,
			Map<Integer, double[]> rekapPerSemester) {

		int totalKegiatan = jumlahLunas + jumlahBelumLunas;

		StringBuilder html = new StringBuilder(4096);
		html.append("<div style='padding:10px 12px 4px;display:flex;flex-direction:column;gap:14px;'>");

		html.append(ais.ui.util.HtmlChartHelper.kpiCards(
				new String[] { "Total Tagihan", "Total Dibayar", "Total Tunggakan", "Kegiatan Belum Lunas" },
				new String[] { Common.numberFormat.get().format(totalTagihan),
						Common.numberFormat.get().format(totalDibayar),
						Common.numberFormat.get().format(totalTunggakan), String.valueOf(jumlahBelumLunas) },
				new String[] { "nilai tagihan seluruh kegiatan pembayaran yang direkap",
						"sudah diterima dari mahasiswa", "kekurangan yang masih harus ditagih",
						"dari " + totalKegiatan + " kegiatan yang direkap" },
				null, null, new String[] { "#2563eb", "#16a34a", "#dc2626", "#f59e0b" }));

		if (totalKegiatan > 0) {
			html.append(ais.ui.util.HtmlChartHelper.donut("Komposisi Status Lunas",
					"Menunjukkan berapa banyak kegiatan pembayaran yang sudah lunas dibandingkan yang masih "
							+ "memiliki tunggakan, dari seluruh data yang direkap.",
					new String[] { "Lunas", "Belum Lunas" }, new double[] { jumlahLunas, jumlahBelumLunas },
					new String[] { "#16a34a", "#dc2626" }, totalKegiatan + " kegiatan"));
		}

		if (!tunggakanPerJenis.isEmpty()) {
			String[] labelJenis = tunggakanPerJenis.keySet().toArray(new String[tunggakanPerJenis.size()]);
			double[] nilaiJenis = new double[labelJenis.length];
			for (int i = 0; i < labelJenis.length; i++) {
				nilaiJenis[i] = tunggakanPerJenis.get(labelJenis[i])[0];
			}
			html.append(ais.ui.util.HtmlChartHelper.barHorizontal("Sebaran Tunggakan per Jenis Pembayaran",
					"Menunjukkan jenis pembayaran mana yang memiliki total tunggakan paling besar, sehingga "
							+ "penagihan dapat difokuskan pada jenis pembayaran yang paling banyak menunggak.",
					labelJenis, nilaiJenis, "#dc2626"));
		}

		if (rekapPerSemester.size() > 1) {
			int n = rekapPerSemester.size();
			String[] kategoriSmt = new String[n];
			double[] seriTagihan = new double[n];
			double[] seriDibayar = new double[n];
			double[] seriTunggakan = new double[n];
			int i = 0;
			for (Map.Entry<Integer, double[]> entri : rekapPerSemester.entrySet()) {
				kategoriSmt[i] = "Smt " + entri.getKey();
				double[] agg = entri.getValue();
				seriTagihan[i] = agg[0];
				seriDibayar[i] = agg[1];
				seriTunggakan[i] = agg[2];
				i++;
			}
			html.append(ais.ui.util.HtmlChartHelper.lineMulti("Tren Tagihan, Dibayar, dan Tunggakan per Semester",
					"Menunjukkan naik-turunnya nilai tagihan, yang sudah dibayar, dan yang masih menunggak pada "
							+ "tiap semester, sehingga perkembangan penagihan terlihat jelas dari semester ke "
							+ "semester.",
					kategoriSmt, new String[] { "Tagihan", "Dibayar", "Tunggakan" },
					new double[][] { seriTagihan, seriDibayar, seriTunggakan },
					new String[] { "#2563eb", "#16a34a", "#dc2626" }));
		}

		html.append("</div>");
		chartPanel.setContent(html.toString());
	}
}
