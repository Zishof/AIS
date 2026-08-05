package ais.action.master.helper.penilaian;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class UploadNilaiMahasiswaFormatEpsbed extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();

	private boolean edit = false;
	private boolean delete = false;
	private boolean create = false;

	private File file;

	public UploadNilaiMahasiswaFormatEpsbed() {
		super();
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Upload Nilai Mahasiswa Format EPSBED",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Upload Nilai Mahasiswa Format EPSBED.",
							"Periksa hak akses (privilege) Create/Update/Delete akun anda pada modul Nilai.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	public UploadNilaiMahasiswaFormatEpsbed(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Upload Nilai Mahasiswa Format EPSBED",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Upload Nilai Mahasiswa Format EPSBED.",
							"Periksa hak akses (privilege) Create/Update/Delete akun anda pada modul Nilai.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	private void init() throws Exception {

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		create = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);

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

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Upload Nilai" + Common.ukuranLabelFileUpload(),
				"/img/upload.png");
		button.setParent(toolbar);
		button.setVisible(create && edit && delete);
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub

				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {
					InputStream inputStream = media.getStreamData();
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();
					initSpreadsheet(file);

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ambil Data", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "NILAI_PDPT.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/penilaian/UploadNilaiMahasiswaFormatEpsbed.java:151");
					PesanFormalHelper.tampilkanGagalException(
							"Mengambil (download) kembali berkas Excel yang telah di-upload",
							e,
							new String[] {
									"Pastikan berkas Excel (xlsx) sudah pernah berhasil di-upload sebelumnya melalui tombol upload.",
									"Ulangi proses upload berkas terlebih dahulu, kemudian coba tombol Ambil Data kembali.",
									"Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
	}

	private Detailperkuliahan createDetailperkulihana(Session session, Mahasiswa mahasiswa, Integer semester,
			String nim, String kodeMatakuliah, Double nilai, Tbmuser tbmuser) {

		Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
				.createAlias("mahasiswa", "mahasiswa")

				.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN)
				.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
				.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)

				.add(Restrictions.or(Restrictions.eq("semester", semester), Restrictions.eq("semester", 0)))
				.add(Restrictions.eq("mahasiswa.nim", nim.trim()))

				.add(Restrictions.or(
						Restrictions.ilike("matakuliahKonversi.kode", kodeMatakuliah.trim(), MatchMode.EXACT),
						Restrictions.ilike("matakuliah.kode", kodeMatakuliah.trim(), MatchMode.EXACT)))

				.setMaxResults(1).uniqueResult();

		if (detailperkuliahan == null) {
			detailperkuliahan = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
					.createAlias("mahasiswa", "mahasiswa")

					.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN)
					.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
					.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)

					.add(Restrictions.eq("mahasiswa.nim", nim.trim()))

					.add(Restrictions.or(
							Restrictions.ilike("matakuliahKonversi.kode", kodeMatakuliah.trim(), MatchMode.EXACT),
							Restrictions.ilike("matakuliah.kode", kodeMatakuliah.trim(), MatchMode.EXACT)))

					.setMaxResults(1).uniqueResult();
		}

		if (detailperkuliahan == null && nilai > 0.01) {
			Matakuliah matakuliah = (Matakuliah) session.createCriteria(Matakuliah.class)
					.add(Restrictions.ilike("kode", kodeMatakuliah.trim()))
					.add(Restrictions.eq("jurusan", mahasiswa.getJurusan())).setMaxResults(1).uniqueResult();
			if (matakuliah == null) {
				matakuliah = (Matakuliah) session.createCriteria(Matakuliah.class)
						.add(Restrictions.ilike("kode", kodeMatakuliah.trim())).setMaxResults(1).uniqueResult();
			}

			if (matakuliah == null) {
				return detailperkuliahan;
			}

			detailperkuliahan = new Detailperkuliahan(tbmuser, UploadNilaiMahasiswaFormatEpsbed.class);
			detailperkuliahan.setMahasiswa(mahasiswa);
			detailperkuliahan.setMatakuliahKonversi(matakuliah);
			detailperkuliahan.setSemester(semester);

		}

		// System.out.println("detailperkuliahan = " + detailperkuliahan + ",
		// nilai = " + nilai);

		if (detailperkuliahan == null) {
			return null;
		}
		Matakuliah matakuliah = detailperkuliahan == null ? null
				: detailperkuliahan.getPerkuliahan() != null ? detailperkuliahan.getPerkuliahan().getMatakuliah()
						: detailperkuliahan.getMatakuliahKonversi();
		NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(nilai, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
				mahasiswa.getJurusan().getFakultas(), detailperkuliahan.getTahunAkademik(),
				detailperkuliahan.getPerkuliahan() == null ? null : detailperkuliahan.getPerkuliahan().getGanjilGenap(),
				matakuliah == null ? "" : matakuliah.getKode(),
				matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
		detailperkuliahan.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
		detailperkuliahan.setTotalNilai(nilai);
		detailperkuliahan.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
		detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
		detailperkuliahan.setSemester(semester);

		Double totalSementara = nilai;
		nilaiHuruf = Common.getNilaiHuruf(totalSementara, detailperkuliahan.getMahasiswa().getTahunangkatan(),
				detailperkuliahan.getMahasiswa().getJurusan(),
				detailperkuliahan.getMahasiswa().getJurusan().getFakultas(), detailperkuliahan.getTahunAkademik(),
				detailperkuliahan.getPerkuliahan() == null ? null : detailperkuliahan.getPerkuliahan().getGanjilGenap(),
				matakuliah == null ? "" : matakuliah.getKode(),
				matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

		detailperkuliahan.setTotalNilaiSementara(totalSementara);
		detailperkuliahan.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
		detailperkuliahan.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

		session.getTransaction().begin();
		session.saveOrUpdate(detailperkuliahan);
		session.getTransaction().commit();

		System.out.println("TA detailperkuliahan = " + detailperkuliahan.getTahunAkademik() + ", semester = "
				+ detailperkuliahan.getSemester() + ", detailperkuliahan.id = " + detailperkuliahan.getId());

		return detailperkuliahan;
	}

	@SuppressWarnings({})
	private void initSpreadsheet(final File fileUpload) throws Exception {

		Common.clear(center);

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);
		final Tbmuser tbmuser = Common.getCurrentUser();
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Nilai Mahasiswa Format EPSBED");
		final Label downloadPath = new Label("");

		final Timer timerReport = new Timer(300);
		timerReport.setParent(UploadNilaiMahasiswaFormatEpsbed.this);
		timerReport.setRepeats(true);
		timerReport.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (label.getValue().isEmpty()) {
					timerReport.stop();
					timerReport.detach();
					if (!downloadPath.getValue().isEmpty()) {
						try {
							Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain");
						} catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) UploadNilaiMahasiswaFormatEpsbed download laporan"); }
					}
					MyMessageboxConfig.show(report.getRingkasan(), "Laporan Upload Nilai EPSBED", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}
			}
		});
		timerReport.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
				lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

				lockedNumericStyle.setLocked(true);

				XSSFCellStyle notLocked = workbook.createCellStyle();
				notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				notLocked.setLocked(false);

				XSSFSheet sheet = workbook.createSheet("NILAI");
				sheet.protectSheet("passwordrahasia");
				sheet.setDefaultColumnWidth(20);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("SMTR");
				rowhead.createCell(1).setCellValue("KODE PRODI");
				rowhead.createCell(2).setCellValue("KELAS");
				rowhead.createCell(3).setCellValue("KODE MATAKULIAH");
				rowhead.createCell(4).setCellValue("NAMA MATAKULIAH");
				rowhead.createCell(5).setCellValue("NIM");
				rowhead.createCell(6).setCellValue("NAMA");
				rowhead.createCell(7).setCellValue("NILAI");
				rowhead.createCell(8).setCellValue("ANGKA");
				rowhead.createCell(9).setCellValue("BOBOT");
				rowhead.createCell(10).setCellValue("SEMESTER");

				XSSFWorkbook workbookUpload;
				try {
					workbookUpload = new XSSFWorkbook(fileUpload.getAbsolutePath());

					XSSFSheet sheetUpload = workbookUpload.getSheetAt(0);
					int size = sheetUpload.getLastRowNum() + 1;

					int rowIndex = 1;
					for (int i = 1; i < size; i++) {
						Session session = HibernateUtil.currentNativeSession();
						try {

							if (Common.getSheetContentAsString(sheetUpload, 0, i) == null) {
								continue;
							}

							String smt = Common.getSheetContentAsString(sheetUpload, 0, i);

							String nim = Common.getSheetContentAsString(sheetUpload, 5, i);

							Integer mysmt = Common.getSheetContentAsInteger(sheetUpload, 10, i);

							Mahasiswa mahasiswa = (Mahasiswa) (session.createCriteria(Mahasiswa.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", nim.trim()))
									.setMaxResults(1).uniqueResult());

							if (mahasiswa == null) {
								report.gagal(i, "NIM: " + nim, "Mahasiswa tidak ditemukan", "Pastikan NIM mahasiswa terdaftar di sistem.");
							}

							if (mahasiswa != null) {

								String kodeMatakuliah = Common.getSheetContentAsString(sheetUpload, 3, i);

								Integer tahun = Integer.parseInt(smt.substring(0, 4));
								Integer jenis = Integer.parseInt(smt.substring(4));

								String tahunAkademik = tahun + "/" + (tahun + 1);
								Integer semester = mysmt != null ? mysmt
										: Common.getSemester(mahasiswa.getTahunangkatan(), tahunAkademik,
												jenis.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
												mahasiswa.getPindahKeKampusIniMasukSemester(),
												mahasiswa.getSemesterMulai());

								Double nilai = Common.getSheetContentAsDouble(sheetUpload, 8, i);

								String huruf = Common.getSheetContentAsString(sheetUpload, 7, i);

								if (nilai == null && huruf != null) {

									NilaiHuruf nilaiHuruf = (NilaiHuruf) session.createCriteria(NilaiHuruf.class)
											.add(Restrictions.ilike("nilaiHuruf", huruf)).setMaxResults(1)
											.uniqueResult();
									if (nilaiHuruf != null) {
										nilai = (nilaiHuruf.getMulai() + nilaiHuruf.getSampai()) / 2.0;
									}

								}

								label.setValue("Upload data " + nim + " - " + kodeMatakuliah + " - " + nilai + " ("
										+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

								System.out.println("mahasiswa " + mahasiswa + " tahun => " + tahun + ", jenis => "
										+ jenis + ", tahunAkademik = " + tahunAkademik + ", semester = " + semester
										+ ", nilai " + nilai + ", huruf = " + huruf + ", kodeMatakuliah "
										+ kodeMatakuliah);

								Detailperkuliahan detailperkuliahan = createDetailperkulihana(session, mahasiswa,
										semester, nim, kodeMatakuliah, nilai, tbmuser);

								if (detailperkuliahan == null) {
									report.gagal(i, "NIM: " + nim + " | MK: " + kodeMatakuliah, "Kode mata kuliah tidak ditemukan atau nilai tidak valid", "Periksa kode mata kuliah dan pastikan sudah terdaftar.");
								}

								if (detailperkuliahan != null) {
									report.sukses(i, "NIM: " + nim + " | MK: " + kodeMatakuliah + " | Nilai: " + nilai, null);
									detailperkuliahan.setTahunAkademik(tahunAkademik);

									Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() == null
											? (detailperkuliahan.getPerkuliahan() == null ? null
													: detailperkuliahan.getPerkuliahan().getMatakuliah())
											: detailperkuliahan.getMatakuliahKonversi();
									if (matakuliah != null) {

										XSSFRow row = sheet.createRow(rowIndex);

										String id_smt = "";
										try {
											id_smt = detailperkuliahan.getTahunAkademik().split("/")[0]
													+ (detailperkuliahan.getSemester() % 2 == 0 ? "2" : "1");
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}

										XSSFCell cell = row.createCell(0);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(id_smt);

										cell = row.createCell(1);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(matakuliah.getJurusan().getKodeEpsbed());

										cell = row.createCell(2);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(detailperkuliahan.getPerkuliahan() == null ? ""
												: detailperkuliahan.getPerkuliahan().getKelas());

										cell = row.createCell(3);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(matakuliah.getKode());

										cell = row.createCell(4);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(matakuliah.getNama());

										cell = row.createCell(5);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(detailperkuliahan.getMahasiswa().getNim());

										cell = row.createCell(6);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(detailperkuliahan.getMahasiswa().getNama());

										cell = row.createCell(7);
										cell.setCellStyle(notLocked);
										cell.setCellValue(detailperkuliahan.getNilaiHuruf());

										cell = row.createCell(8);
										cell.setCellStyle(notLocked);
										cell.setCellValue(detailperkuliahan.getTotalNilai());

										cell = row.createCell(9);
										cell.setCellStyle(notLocked);
										cell.setCellValue(detailperkuliahan.getTotalIP());

										cell = row.createCell(10);
										cell.setCellStyle(notLocked);
										cell.setCellValue(detailperkuliahan.getSemester());

										rowIndex++;
									}
								}

							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

						HibernateUtil.closeSession();
					}

					Common.setStyled(sheet);
					sizedata.setValue(rowIndex + 1);

					try {
						FileOutputStream fileOut = new FileOutputStream(filename);
						workbook.write(fileOut);
						fileOut.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						Common.tampilErrorJikaAdmin(e);
						PesanFormalHelper.tampilkanGagalException(
								"Menyimpan berkas Excel hasil proses Upload Nilai Mahasiswa Format EPSBED",
								e,
								new String[] {
										"Pastikan berkas dengan nama yang sama tidak sedang dibuka oleh aplikasi lain (misalnya Microsoft Excel).",
										"Periksa ketersediaan ruang penyimpanan (disk space) pada server.",
										"Ulangi proses upload data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
					}

				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/penilaian/UploadNilaiMahasiswaFormatEpsbed.java:470");
					PesanFormalHelper.tampilkanGagalException(
							"Memproses berkas Excel Upload Nilai Mahasiswa Format EPSBED",
							e1,
							new String[] {
									"Pastikan berkas Excel yang di-upload sesuai format template yang telah ditentukan (kolom dan urutan data).",
									"Periksa isi data pada setiap baris, pastikan tidak ada sel yang kosong atau bertipe data yang salah.",
									"Ulangi proses upload data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
					// FIX "gagal diam-diam": sebelumnya setelah dialog error ini ditampilkan,
					// eksekusi tetap lanjut ke label.setValue("") di bawah (di luar catch ini)
					// tanpa syarat, membuat progress bar tetap melaporkan SUKSES palsu meski
					// proses baru saja gagal total. Set label ke status Error dan hentikan di sini.
					label.setValue("Error: " + PesanFormalHelper.pesanGagalException(
							"Memproses berkas Excel Upload Nilai Mahasiswa Format EPSBED", null, e1,
							new String[] {
									"Pastikan berkas Excel yang di-upload sesuai format template yang telah ditentukan (kolom dan urutan data).",
									"Periksa isi data pada setiap baris, pastikan tidak ada sel yang kosong atau bertipe data yang salah.",
									"Ulangi proses upload data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
					return;
				}

				System.out.println("Your excel file has been generated! ");

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) UploadNilaiMahasiswaFormatEpsbed laporan"); }

				HibernateUtil.closeSession();
				label.setValue("");

			} catch (Exception eOuter) {
					// FIX "gagal diam-diam"/hang selamanya: try terluar ini sebelumnya TIDAK
					// punya catch (hanya finally), jadi exception di luar blok try dalam akan
					// lolos tak tertangani keluar dari run() thread ini; label.setValue("")
					// tidak pernah tereksekusi dan popup progres tidak akan pernah tertutup.
					Common.tampilErrorJikaAdmin(eOuter);
					label.setValue("Error: " + PesanFormalHelper.pesanGagalException(
							"pemrosesan Upload Nilai Mahasiswa Format EPSBED", null, eOuter,
							new String[] {
									"Pastikan berkas Excel yang di-upload sesuai format template yang telah ditentukan.",
									"Periksa koneksi ke database server dan ketersediaan ruang penyimpanan (disk space) pada server.",
									"Ulangi proses upload data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
}
