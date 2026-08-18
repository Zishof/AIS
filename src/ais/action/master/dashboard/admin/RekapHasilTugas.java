package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vlayout;

import ais.action.master.helper.AbsensiHelper;
import ais.common.Common;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.Tugas;
import ais.database.model.TugasPertemuan;
import ais.database.model.file.TugasFileContent;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h1>RekapHasilTugas &mdash; Rekapitulasi Hasil Tugas per Pertemuan</h1>
 *
 * <p>
 * Jendela modal ini dipakai oleh dosen/admin dari layar pengelolaan <b>Pertemuan</b> (lewat tombol
 * toolbar "Rekap Hasil Tugas" pada {@code PertemuanAction}) maupun dari layar Jadwal Ujian PMB (lewat
 * {@code JadwalUjianPMBAction}) untuk melihat <b>seluruh hasil tugas</b> (baik tugas pada pertemuan itu
 * sendiri maupun tugas tambahan lewat {@link TugasPertemuan}) dari satu atau beberapa
 * {@link Pertemuan} sekaligus, tanpa harus membuka penilaian tugas satu per satu. Untuk tiap pertemuan
 * yang memiliki tugas, sistem menyusun satu tabel berisi satu baris per peserta (mahasiswa reguler,
 * atau {@link BiodataCalonMahasiswa} bila pertemuan tsb adalah ujian PMB) dan satu kolom per tugas,
 * berisi tautan berkas yang dikumpulkan beserta nilainya, ditutup dengan kolom Total, "Ikut Tugas (x)",
 * dan Rata-Rata. Hasilnya dapat diunduh sebagai berkas Excel lewat tombol <b>Download Data</b>.
 * </p>
 *
 * <h2>Alur teknis singkat</h2>
 * <p>
 * {@link #init()} membangun tata letak (toolbar berisi tombol Tutup opsional &amp; Download Data pada
 * region {@code North}, area hasil pada region {@code Center}) lalu langsung memanggil
 * {@link #initSpreadsheet()}. Karena menyusun rekap bisa memakan waktu untuk banyak pertemuan/peserta,
 * penyusunan berkas Excel dijalankan pada {@link Thread} latar belakang terpisah agar antarmuka ZK
 * tidak macet; selama proses berjalan, {@code Common.displayLoadBar(...)} menampilkan indikator
 * "sedang memproses" yang di-<i>polling</i> lewat komponen {@link Label} tersembunyi, dan begitu label
 * dikosongkan, berkas Excel yang sudah selesai ditulis otomatis ditampilkan sebagai tabel/grid ringan
 * (lihat {@code ais.ui.util.PratinjauXlsxHelper#gantiSpreadsheetDenganGrid}, dipanggil otomatis di
 * dalam {@code LoadBarUtils}) alih-alih widget Excel penuh yang berat di sisi peramban.
 * </p>
 *
 * <h2>Panel ringkasan visual (HTML/CSS, bukan JFreeChart)</h2>
 * <p>
 * Selain tabel mentah, dasbor ini juga menampilkan panel ringkasan berupa kartu KPI (jumlah pertemuan
 * &amp; peserta yang direkap, rata-rata nilai tugas, persentase pengumpulan), grafik donat komposisi
 * pengumpulan tugas (sudah vs belum dikumpulkan), grafik batang sebaran rentang rata-rata nilai tugas
 * per peserta, dan grafik batang perbandingan rata-rata nilai tugas antar pertemuan (bila lebih dari
 * satu pertemuan direkap). Seluruh angka pada panel ini dihitung <b>bersamaan</b> dengan penulisan sel
 * Excel di dalam {@link #initSpreadsheet()} (loop yang sama, tanpa kueri basis data tambahan) lalu
 * dirender lewat {@code ais.ui.util.HtmlChartHelper} setelah loop selesai, dibungkus percobaan/tangkapan
 * tersendiri supaya kegagalan menggambar grafik tidak pernah mengganggu penulisan Excel maupun tampilan
 * grid.
 * </p>
 *
 * <h2>Ketahanan terhadap data tidak lengkap</h2>
 * <p>
 * Data pertemuan/peserta pada praktiknya bisa tidak lengkap (status aktif belum diisi, program studi
 * kosong, dsb.). Setiap pertemuan dan setiap peserta diproses di dalam blok percobaan tersendiri: bila
 * satu data bermasalah, kesalahannya dicatat lewat {@code ais.common.ErrorAuditUtil#record} dan proses
 * lanjut ke data berikutnya alih-alih menghentikan seluruh rekap.
 * </p>
 *
 * @author e-Campus UI Team
 */
public class RekapHasilTugas extends MyWindow {

	/**
	 *
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();

	private File file;

	private Pertemuan[] pertemuans;

	private boolean simple;

	public RekapHasilTugas(boolean simple, Pertemuan... pertemuans) {
		super();
		this.simple = simple;
		this.pertemuans = pertemuans;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun seluruh tata letak jendela rekap: toolbar pada region {@code North} berisi tombol
	 * "Tutup" (hanya bila {@code simple} bernilai false) dan tombol "Download Data" untuk mengunduh
	 * berkas Excel yang sudah disusun, serta area hasil (region {@code Center}) tempat panel grafik
	 * ringkasan dan grid pratinjau ditampilkan. Deskripsi ringkas dasbor ditampilkan di atas toolbar.
	 * Method ini langsung memanggil {@link #initSpreadsheet()} di akhir untuk menyusun data begitu
	 * jendela dibuka.
	 */
	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North south = new North();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		// North hanya boleh 1 anak -> bungkus dlm Vlayout: deskripsi ringkas dasbor di atas,
		// toolbar (Tutup/Download Data) tetap di bawah, sama seperti pola Vlayout pada Center.
		Vlayout northBox = new Vlayout();
		northBox.setWidth("100%");
		northBox.setParent(south);

		Html deskripsiDasbor = new Html();
		deskripsiDasbor.setContent("<div style='font-size:13px;color:#475569;padding:2px 4px 6px;'>"
				+ "Menampilkan rekap hasil tugas seluruh mahasiswa/peserta pada pertemuan terpilih, lengkap "
				+ "dengan ringkasan grafik pengumpulan dan capaian nilai tugas, sehingga hasil belajar mudah "
				+ "dievaluasi tanpa membuka penilaian tugas satu per satu.</div>");
		deskripsiDasbor.setParent(northBox);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(northBox);

		if (!simple) {
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					detach();
				}
			});
			cancel.setParent(toolbar);
		}
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					if (file == null || !file.exists()) {
						return;
					}
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"Hasil Ujian " + (pertemuans.length == 0 ? ""
									: URLEncoder.encode(pertemuans[0].getTopik(), "UTF-8")) + ".xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugas.java:111");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		initSpreadsheet();
	}

	/**
	 * Method inti dasbor ini: untuk tiap {@link Pertemuan} yang aktif dan memiliki tugas, menyusun satu
	 * tabel berisi satu baris per peserta beserta nilai tiap tugas, menuliskannya sebagai berkas Excel
	 * pada {@link Thread} latar belakang terpisah (agar antarmuka tidak macet untuk data yang banyak),
	 * sekaligus mengumpulkan ringkasan angka (jumlah pertemuan/peserta, rata-rata nilai, komposisi
	 * pengumpulan tugas, sebaran nilai, rata-rata per pertemuan) untuk panel grafik HTML/CSS yang
	 * dirender lewat {@link #renderChartRingkasan}. Region {@code Center} dibungkus satu {@link Vlayout}
	 * berisi dua anak: panel grafik ({@code chartPanel}) dan wadah grid/Excel ({@code gridHost}) yang
	 * terpisah, sehingga saat {@code Common.displayLoadBar(...)} membersihkan dan mengisi ulang area
	 * hasil di akhir proses, panel grafik yang sudah tampil tidak ikut terhapus. Setiap pertemuan dan
	 * setiap peserta diproses dalam blok percobaan tersendiri: data yang bermasalah dicatat lalu
	 * dilewati, tidak menggagalkan seluruh rekap.
	 */
	private void initSpreadsheet() throws Exception {

		if (pertemuans.length == 0) {
			return;
		}

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		(file = new File(filename)).createNewFile();

		// Center hanya boleh 1 anak -> bungkus dlm Vlayout: panel grafik ringkasan di atas, wadah
		// grid/Excel di bawah lewat Div terpisah (gridHost) supaya saat Common.displayLoadBar(...)
		// membersihkan & mengisi ulang area hasil, panel grafik yang sudah tampil tidak ikut terhapus.
		Common.clear(center);
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

		// FIX IllegalStateException "Components can be accessed only in event listeners":
		// renderChartRingkasan() (dipanggil dari Thread latar di bawah) memanggil
		// chartPanel.setContent(...) yang menyentuh komponen ZK -- WAJIB lewat
		// Executions.activate(desktop) dulu. Ambil referensi desktop di sini (thread ZK yang
		// masih punya konteks), bukan di dalam Thread latar.
		final org.zkoss.zk.ui.Desktop desktopChart = chartPanel.getDesktop();

		new Thread(new Runnable() {

			@SuppressWarnings("rawtypes")
			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("DATA");
				sheet.setDefaultColumnWidth(20);
				int rowIndex = 0;
				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setUnderline(XSSFFont.U_SINGLE);

				final XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);

				hlink_style.setFont(hlink_font);

				// -- Akumulasi ringkasan utk panel grafik (dihitung BERSAMAAN dgn penulisan sel Excel
				// di bawah, tanpa kueri tambahan) -> dirender lewat renderChartRingkasan setelah loop
				// selesai. --
				int totalPertemuanDiproses = 0;
				int totalPesertaDiproses = 0;
				double akumulasiRataRata = 0;
				int jumlahPesertaDenganRataRata = 0;
				int jumlahSlotDikumpulkan = 0;
				int jumlahSlotBelumKumpul = 0;
				final int[] sebaranNilai = new int[5];
				final Map<String, double[]> rataPerPertemuan = new LinkedHashMap<String, double[]>();

				for (Pertemuan pertemuan : pertemuans) {
					try {
					if (pertemuan.getAktif() != null && pertemuan.getAktif()) {
						List<Tugas> tugas2 = new ArrayList<Tugas>();
						if (!pertemuan.getJudultugas().isEmpty()) {
							tugas2.add(pertemuan);
						}
						Collection<TugasPertemuan> tugases = pertemuan.ambilTugasPertemuanTotal().values();
						for (TugasPertemuan tugasPertemuan : tugases) {
							if (!tugasPertemuan.getJudultugas().isEmpty()) {
								tugas2.add(tugasPertemuan);
							}
						}
						if (tugas2.isEmpty()) {
							continue;
						}
						totalPertemuanDiproses++;

						sheet.createRow(rowIndex).createCell(0)
								.setCellValue("Tugas pada pertemuan \"" + pertemuan.getTopik() + "\"");

						rowIndex++;
						XSSFRow rowhead = sheet.createRow(rowIndex);

						rowhead.createCell(0).setCellValue("No.");
						rowhead.createCell(1).setCellValue("NIM/No. Reg");
						rowhead.createCell(2).setCellValue("Nama");
						rowhead.createCell(3).setCellValue("Prodi");

						rowhead.createCell(4).setCellValue("Kelas");

						rowIndex++;

						int index = 5;

						Map<Long, TreeMap<Long, TugasFileContent>> nilais = new HashMap<Long, TreeMap<Long, TugasFileContent>>();
						for (Tugas tugas : tugas2) {
							TreeMap<Long, TugasFileContent> d = tugas.ambilTugasFileContentTotal();
							nilais.put(tugas.getId(), d);
							rowhead.createCell(index).setCellValue(tugas.getJudultugas());
							index++;
						}
						rowhead.createCell(index).setCellValue("Total");
						rowhead.createCell(index + 1).setCellValue("Ikut Tugas (x)");
						rowhead.createCell(index + 2).setCellValue("Rata-Rata");

						List hasilUjianMahasiswas;

						if (pertemuan.getJadwalUjianPMB() != null) {
							hasilUjianMahasiswas = AbsensiHelper.populateMahasiswaDariPertemuan(pertemuan);
						} else {
							hasilUjianMahasiswas = pertemuan.ambilMahasiswa();
						}

						int indexLocal = 1;
						for (Object valueObject : hasilUjianMahasiswas) {
							try {

							if (valueObject instanceof Mahasiswa) {
								Mahasiswa mahasiswa = (Mahasiswa) valueObject;

								label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
										+ Common.numberFormat.get().format(indexLocal * 100.0 / hasilUjianMahasiswas.size())
										+ " %)");

								XSSFRow row = sheet.createRow(rowIndex);
								row.createCell(0).setCellValue(indexLocal);
								row.createCell(1).setCellValue(mahasiswa.getNim());
								row.createCell(2).setCellValue(mahasiswa.getNama());
								row.createCell(3).setCellValue(
										mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());
								KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
								row.createCell(4).setCellValue(krsMahasiswa.getKelas());

								index = 5;
								Double totalNilai = 0.0;
								int jumlahBolehIkut = 0;
								for (Tugas tugas : tugas2) {
									Long id = mahasiswa.getId();
									if (!tugas.getMhsYgTidakIkut().contains("," + id + ",")) {
										TreeMap<Long, TugasFileContent> d = nilais.get(tugas.getId());
										TugasFileContent tugasFileContent = d.containsKey(id) ? d.get(id) : null;
										Double nilai = tugasFileContent != null ? tugasFileContent.getNilai() : null;
										String url = tugas.ambilLink(tugasFileContent);
										XSSFCell cellTambahan = row.createCell(index);

										cellTambahan
												.setCellValue(tugasFileContent == null ? ""
														: tugasFileContent.ambilRealNameSesuaiDenganNIM(mahasiswa)
																+ " ("
																+ Common.numberFormat.get().format(
																		nilai == null ? 0.0 : nilai.doubleValue())
																+ ")");

										if (url != null && !url.trim().isEmpty()) {
											cellTambahan.setCellStyle(hlink_style);
											XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
													.createHyperlink(Hyperlink.LINK_URL);
											link.setAddress(url);
											cellTambahan.setHyperlink(link);
										}

										totalNilai += nilai == null ? 0.0 : nilai.doubleValue();

										jumlahBolehIkut++;

										// -- Akumulasi ringkasan panel grafik: komposisi pengumpulan tugas
										// (donat), tanpa kueri tambahan, memakai data yang sudah diambil
										// di atas. --
										if (tugasFileContent != null) {
											jumlahSlotDikumpulkan++;
										} else {
											jumlahSlotBelumKumpul++;
										}

									} else {
										row.createCell(index).setCellValue("Tidak perlu ikut");
									}

									index++;
								}
								row.createCell(index).setCellValue(totalNilai);
								row.createCell(index + 1).setCellValue(jumlahBolehIkut);
								row.createCell(index + 2)
										.setCellValue(Common.numberFormat.get().format(totalNilai / jumlahBolehIkut));
								indexLocal++;

								// -- Akumulasi ringkasan panel grafik: KPI rata-rata, sebaran nilai, dan
								// rata-rata per pertemuan -- hanya utk peserta yg punya minimal 1 tugas
								// wajib (jumlahBolehIkut > 0), memakai nilai yg sudah dihitung di atas. --
								totalPesertaDiproses++;
								if (jumlahBolehIkut > 0) {
									double rataRataPeserta = totalNilai / jumlahBolehIkut;
									akumulasiRataRata += rataRataPeserta;
									jumlahPesertaDenganRataRata++;
									int idxSebaran = rataRataPeserta < 60 ? 0 : rataRataPeserta < 70 ? 1
											: rataRataPeserta < 80 ? 2 : rataRataPeserta < 90 ? 3 : 4;
									sebaranNilai[idxSebaran]++;

									double[] aggPertemuan = rataPerPertemuan.get(pertemuan.getTopik());
									if (aggPertemuan == null) {
										aggPertemuan = new double[2];
										rataPerPertemuan.put(pertemuan.getTopik(), aggPertemuan);
									}
									aggPertemuan[0] += rataRataPeserta;
									aggPertemuan[1] += 1;
								}

							} else if (valueObject instanceof BiodataCalonMahasiswa) {
								BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) valueObject;

								label.setValue("Sedang memproses data " + biodataCalonMahasiswa.toString() + " ("
										+ Common.numberFormat.get().format(indexLocal * 100.0 / hasilUjianMahasiswas.size())
										+ " %)");

								XSSFRow row = sheet.createRow(rowIndex);
								row.createCell(0).setCellValue(indexLocal);
								row.createCell(1).setCellValue(biodataCalonMahasiswa.getNoRegistrasi() + "/"
										+ biodataCalonMahasiswa.getNoUjian());
								row.createCell(2).setCellValue(biodataCalonMahasiswa.getNama());

								String prodiPilihan = "";
								if (biodataCalonMahasiswa.getProdi1() != null) {
									prodiPilihan += biodataCalonMahasiswa.getProdi1().getNama();
								}
								if (biodataCalonMahasiswa.getProdi2() != null) {
									prodiPilihan += prodiPilihan.isEmpty() ? biodataCalonMahasiswa.getProdi2().getNama()
											: ", " + biodataCalonMahasiswa.getProdi2().getNama();
								}
								if (biodataCalonMahasiswa.getProdi3() != null) {
									prodiPilihan += prodiPilihan.isEmpty() ? biodataCalonMahasiswa.getProdi3().getNama()
											: ", " + biodataCalonMahasiswa.getProdi3().getNama();
								}
								if (biodataCalonMahasiswa.getProdi4() != null) {
									prodiPilihan += prodiPilihan.isEmpty() ? biodataCalonMahasiswa.getProdi4().getNama()
											: ", " + biodataCalonMahasiswa.getProdi4().getNama();
								}
								if (biodataCalonMahasiswa.getProdi5() != null) {
									prodiPilihan += prodiPilihan.isEmpty() ? biodataCalonMahasiswa.getProdi5().getNama()
											: ", " + biodataCalonMahasiswa.getProdi5().getNama();
								}

								row.createCell(3).setCellValue(prodiPilihan);

								row.createCell(4).setCellValue(biodataCalonMahasiswa.getStatusAwalMahasiswa() == null
										? "" : biodataCalonMahasiswa.getStatusAwalMahasiswa().getNama());

								index = 5;
								Double totalNilai = 0.0;
								int jumlahBolehIkut = 0;
								for (Tugas tugas : tugas2) {
									Long id = biodataCalonMahasiswa.getId();
									if (!tugas.getMhsYgTidakIkut().contains("," + id + ",")) {
										TreeMap<Long, TugasFileContent> d = nilais.get(tugas.getId());
										TugasFileContent tugasFileContent = d.containsKey(id) ? d.get(id) : null;
										Double nilai = tugasFileContent != null ? tugasFileContent.getNilai() : null;
										String url = tugas.ambilLink(tugasFileContent);
										XSSFCell cellTambahan = row.createCell(index);
										cellTambahan.setCellValue(tugasFileContent == null ? ""
												: tugasFileContent.ambilRealNameSesuaiDenganNIM(biodataCalonMahasiswa)
														+ " (" + Common.numberFormat.get().format(
																nilai == null ? 0.0 : nilai.doubleValue())
														+ ")");

										if (url != null && !url.trim().isEmpty()) {
											cellTambahan.setCellStyle(hlink_style);
											XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
													.createHyperlink(Hyperlink.LINK_URL);
											link.setAddress(url);
											cellTambahan.setHyperlink(link);
										}

										totalNilai += nilai == null ? 0.0 : nilai.doubleValue();

										jumlahBolehIkut++;

										// -- Akumulasi ringkasan panel grafik: komposisi pengumpulan tugas
										// (donat), tanpa kueri tambahan, memakai data yang sudah diambil
										// di atas. --
										if (tugasFileContent != null) {
											jumlahSlotDikumpulkan++;
										} else {
											jumlahSlotBelumKumpul++;
										}

									} else {
										row.createCell(index).setCellValue("Tidak perlu ikut");
									}
								}
								row.createCell(index).setCellValue(Common.numberFormat.get().format(totalNilai));
								row.createCell(index + 1).setCellValue(jumlahBolehIkut);
								row.createCell(index + 2)
										.setCellValue(Common.numberFormat.get().format(totalNilai / jumlahBolehIkut));

								indexLocal++;

								// -- Akumulasi ringkasan panel grafik: KPI rata-rata, sebaran nilai, dan
								// rata-rata per pertemuan -- hanya utk peserta yg punya minimal 1 tugas
								// wajib (jumlahBolehIkut > 0), memakai nilai yg sudah dihitung di atas. --
								totalPesertaDiproses++;
								if (jumlahBolehIkut > 0) {
									double rataRataPeserta = totalNilai / jumlahBolehIkut;
									akumulasiRataRata += rataRataPeserta;
									jumlahPesertaDenganRataRata++;
									int idxSebaran = rataRataPeserta < 60 ? 0 : rataRataPeserta < 70 ? 1
											: rataRataPeserta < 80 ? 2 : rataRataPeserta < 90 ? 3 : 4;
									sebaranNilai[idxSebaran]++;

									double[] aggPertemuan = rataPerPertemuan.get(pertemuan.getTopik());
									if (aggPertemuan == null) {
										aggPertemuan = new double[2];
										rataPerPertemuan.put(pertemuan.getTopik(), aggPertemuan);
									}
									aggPertemuan[0] += rataRataPeserta;
									aggPertemuan[1] += 1;
								}
							}

							} catch (Exception exBaris) {
								// Satu data peserta bermasalah (mis. relasi prodi/status belum lengkap)
								// tidak boleh menggagalkan seluruh proses rekap; catat & lanjut.
								ais.common.ErrorAuditUtil.record(exBaris,
										"auto-guard(per-peserta) src/ais/action/master/dashboard/admin/RekapHasilTugas.java");
							} finally {
								rowIndex++;
							}
						}
						nilais = null;
						hasilUjianMahasiswas = null;
						rowIndex++;
					}
					} catch (Exception exPertemuan) {
						// Satu pertemuan bermasalah (mis. status aktif belum diisi, relasi tugas
						// tidak lengkap) tidak boleh menggagalkan rekap pertemuan lainnya; catat & lanjut.
						ais.common.ErrorAuditUtil.record(exPertemuan,
								"auto-guard(per-pertemuan) src/ais/action/master/dashboard/admin/RekapHasilTugas.java");
					}
				}

				Common.setStyled(sheet);
				sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					ais.common.ErrorAuditUtil.record(e,
							"src/ais/action/master/dashboard/admin/RekapHasilTugas.java:initSpreadsheet-tulisExcel");
					Common.tampilErrorJikaAdmin(e);
				}

				System.out.println("Your excel file has been generated! ");

				// -- Render panel grafik (HTML/CSS, BUKAN JFreeChart) dari ringkasan yang sudah
				// dihitung di loop di atas. Kegagalan menggambar grafik tidak boleh mengganggu
				// Excel/grid yang sudah berhasil ditulis. --
				try {
					if (desktopChart != null && desktopChart.isAlive()) {
						org.zkoss.zk.ui.Executions.activate(desktopChart);
						try {
							renderChartRingkasan(chartPanel, totalPertemuanDiproses, totalPesertaDiproses,
									akumulasiRataRata, jumlahPesertaDenganRataRata, jumlahSlotDikumpulkan,
									jumlahSlotBelumKumpul, sebaranNilai, rataPerPertemuan);
						} finally {
							org.zkoss.zk.ui.Executions.deactivate(desktopChart);
						}
					}
				} catch (Exception exChart) {
					ais.common.ErrorAuditUtil.record(exChart,
							"auto-guard(chart) src/ais/action/master/dashboard/admin/RekapHasilTugas.java");
				}

				label.setValue("");

				} catch (Exception exProses) {
					// Kegagalan proses (mis. kueri/relasi bermasalah) tidak boleh membuat indikator
					// "sedang memproses" menggantung selamanya di sisi pengguna.
					ais.common.ErrorAuditUtil.record(exProses,
							"src/ais/action/master/dashboard/admin/RekapHasilTugas.java:initSpreadsheet-thread");
					Common.tampilErrorJikaAdmin(exProses);
					label.setValue("");
				}
			}
		}).start();

	}

	/**
	 * Merender panel ringkasan visual dasbor ini sepenuhnya memakai {@code ais.ui.util.HtmlChartHelper}
	 * (HTML/CSS, bukan JFreeChart): kartu KPI (jumlah pertemuan &amp; peserta yang direkap, rata-rata
	 * nilai tugas, persentase pengumpulan), grafik donat komposisi pengumpulan tugas (sudah vs belum
	 * dikumpulkan), grafik batang sebaran rentang rata-rata nilai tugas per peserta, serta grafik batang
	 * perbandingan rata-rata nilai tugas antar pertemuan (hanya bila rekap mencakup minimal 2 pertemuan
	 * agar perbandingannya bermakna). Seluruh parameter berasal dari akumulasi yang sudah dihitung
	 * selagi menulis sel Excel di {@link #initSpreadsheet()}, sehingga tidak ada kueri basis data
	 * tambahan yang dijalankan hanya untuk kebutuhan grafik.
	 *
	 * @param chartPanel                  komponen tampilan tujuan.
	 * @param totalPertemuanDiproses      jumlah pertemuan yang punya tugas dan berhasil direkap.
	 * @param totalPesertaDiproses        jumlah peserta (mahasiswa/calon mahasiswa) yang berhasil diproses.
	 * @param akumulasiRataRata           jumlah (bukan rata-rata) rata-rata nilai tugas seluruh peserta
	 *                                    yang punya minimal satu tugas wajib.
	 * @param jumlahPesertaDenganRataRata jumlah peserta yang punya minimal satu tugas wajib (penyebut
	 *                                    utk {@code akumulasiRataRata}).
	 * @param jumlahSlotDikumpulkan       jumlah slot tugas wajib yang sudah dikumpulkan peserta.
	 * @param jumlahSlotBelumKumpul       jumlah slot tugas wajib yang belum dikumpulkan peserta.
	 * @param sebaranNilai                jumlah peserta per rentang rata-rata nilai tugas:
	 *                                    [&lt;60, 60-69, 70-79, 80-89, 90-100].
	 * @param rataPerPertemuan            peta topik pertemuan -&gt; {jumlah rata-rata, jumlah peserta}.
	 */
	private static void renderChartRingkasan(Html chartPanel, int totalPertemuanDiproses, int totalPesertaDiproses,
			double akumulasiRataRata, int jumlahPesertaDenganRataRata, int jumlahSlotDikumpulkan,
			int jumlahSlotBelumKumpul, int[] sebaranNilai, Map<String, double[]> rataPerPertemuan) {

		double rataRataKeseluruhan = jumlahPesertaDenganRataRata == 0 ? 0
				: akumulasiRataRata / jumlahPesertaDenganRataRata;
		int totalSlotTugas = jumlahSlotDikumpulkan + jumlahSlotBelumKumpul;
		double persentaseKumpul = totalSlotTugas == 0 ? 0 : (jumlahSlotDikumpulkan * 100.0 / totalSlotTugas);

		StringBuilder html = new StringBuilder(4096);
		html.append("<div style='padding:10px 12px 4px;display:flex;flex-direction:column;gap:14px;'>");

		html.append(ais.ui.util.HtmlChartHelper.kpiCards(
				new String[] { "Pertemuan Diproses", "Peserta Diproses", "Rata-rata Nilai Tugas",
						"Persentase Pengumpulan" },
				new String[] { String.valueOf(totalPertemuanDiproses), String.valueOf(totalPesertaDiproses),
						Common.numberFormat.get().format(rataRataKeseluruhan),
						Common.numberFormat.get().format(persentaseKumpul) + "%" },
				new String[] { "pertemuan dengan tugas aktif", "mahasiswa/peserta yang direkap",
						"dari peserta yang punya tugas wajib",
						jumlahSlotDikumpulkan + " dari " + totalSlotTugas + " slot tugas terkumpul" },
				null, null, new String[] { "#2563eb", "#16a34a", "#0891b2", "#7c3aed" }));

		if (totalSlotTugas > 0) {
			html.append(ais.ui.util.HtmlChartHelper.donut("Komposisi Pengumpulan Tugas",
					"Menunjukkan berapa banyak tugas yang sudah dikumpulkan dibandingkan yang belum "
							+ "dikumpulkan dari seluruh tugas wajib pada pertemuan yang direkap.",
					new String[] { "Sudah Mengumpulkan", "Belum Mengumpulkan" },
					new double[] { jumlahSlotDikumpulkan, jumlahSlotBelumKumpul },
					new String[] { "#16a34a", "#dc2626" },
					Common.numberFormat.get().format(persentaseKumpul) + "%"));
		}

		html.append(ais.ui.util.HtmlChartHelper.barHorizontal("Sebaran Rata-rata Nilai Tugas per Peserta",
				"Menunjukkan berapa banyak mahasiswa/peserta berada pada tiap rentang rata-rata nilai "
						+ "tugas, dari yang paling rendah sampai paling tinggi, sehingga sebaran capaian "
						+ "tugas terlihat sekilas.",
				new String[] { "< 60", "60 - 69", "70 - 79", "80 - 89", "90 - 100" },
				new double[] { sebaranNilai[0], sebaranNilai[1], sebaranNilai[2], sebaranNilai[3],
						sebaranNilai[4] },
				"#2563eb"));

		if (rataPerPertemuan.size() >= 2) {
			String[] labelPertemuan = rataPerPertemuan.keySet().toArray(new String[rataPerPertemuan.size()]);
			double[] nilaiPertemuan = new double[labelPertemuan.length];
			for (int i = 0; i < labelPertemuan.length; i++) {
				double[] agg = rataPerPertemuan.get(labelPertemuan[i]);
				nilaiPertemuan[i] = agg[1] == 0 ? 0 : agg[0] / agg[1];
			}
			html.append(ais.ui.util.HtmlChartHelper.barHorizontal(
					"Perbandingan Rata-rata Nilai Tugas Antar Pertemuan",
					"Membandingkan rata-rata nilai tugas peserta pada tiap pertemuan, sehingga pertemuan "
							+ "dengan capaian tugas tertinggi atau terendah mudah terlihat.",
					labelPertemuan, nilaiPertemuan, "#7c3aed"));
		}

		html.append("</div>");
		chartPanel.setContent(html.toString());
	}
}
