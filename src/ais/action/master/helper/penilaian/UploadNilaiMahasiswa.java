package ais.action.master.helper.penilaian;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
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

/**
 * Tipe khusus untuk upload nilai mahasiswa. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code boolean edit},
 * {@code boolean delete}, {@code boolean create}, {@code File file}; inisialisasi/lifecycle ({@code init()},
 * {@code initSpreadsheet()}); operasi domain lain ({@code createDetailperkulihana()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class UploadNilaiMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();

	private boolean edit = false;
	private boolean delete = false;
	private boolean create = false;

	private File file;

	public UploadNilaiMahasiswa() {
		super();
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Upload Nilai Mahasiswa",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Upload Nilai Mahasiswa.",
							"Periksa hak akses (privilege) Create/Update/Delete akun anda pada modul Nilai.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	public UploadNilaiMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Upload Nilai Mahasiswa",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Upload Nilai Mahasiswa.",
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
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "MSMHS.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/penilaian/UploadNilaiMahasiswa.java:151");
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

	private Detailperkuliahan createDetailperkulihana(Session session, Long id, Mahasiswa mahasiswa, Integer semester,
			String nim, Long idMatakuliah, Double nilai, Long idMatakuliahPindah, Integer semesterPindah,
			Tbmuser tbmuser) {
		Detailperkuliahan detailperkuliahan = id == null || id.equals(-1L) ? null
				: (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class).add(Restrictions.idEq(id))
						.setMaxResults(1).uniqueResult();

		if (detailperkuliahan == null) {
			detailperkuliahan = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
					.createAlias("mahasiswa", "mahasiswa").add(Restrictions.eq("mahasiswa.nim", nim.trim()))
					.add(Restrictions.eq("matakuliahKonversi.id", idMatakuliah)).setMaxResults(1).uniqueResult();
		}

		if (detailperkuliahan == null) {
			detailperkuliahan = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
					.createAlias("mahasiswa", "mahasiswa").createAlias("perkuliahan", "perkuliahan")
					.createAlias("perkuliahan.matakuliah", "matakuliah").add(Restrictions.eq("semester", semester))
					.add(Restrictions.eq("mahasiswa.nim", nim.trim()))
					.add(Restrictions.eq("matakuliah.id", idMatakuliah)).setMaxResults(1).uniqueResult();
		}

		if (detailperkuliahan != null && nilai != null && nilai.intValue() == -1) {
			session.getTransaction().begin();
			session.createSQLQuery("delete from detailperkuliahan where id=" + detailperkuliahan.getId())
					.executeUpdate();
			session.getTransaction().commit();
			return null;
		}

		if (detailperkuliahan == null && nilai > 0.01) {
			detailperkuliahan = new Detailperkuliahan(tbmuser, UploadNilaiMahasiswa.class);
			detailperkuliahan.setMahasiswa(mahasiswa);
			detailperkuliahan.setMatakuliahKonversi(new Matakuliah(idMatakuliah));
			detailperkuliahan.setSemester(semester);

		}

		if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {
			return detailperkuliahan;
		}

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

		if (idMatakuliahPindah != null) {
			detailperkuliahan.setMatakuliahKonversi(new Matakuliah(idMatakuliahPindah));
		}

		if (semesterPindah != null) {
			detailperkuliahan.setSemester(semesterPindah);
		}

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
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Nilai Mahasiswa");
		final Label downloadPath = new Label("");

		final Timer timerReport = new Timer(300);
		timerReport.setParent(UploadNilaiMahasiswa.this);
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
						} catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) UploadNilaiMahasiswa download laporan"); }
					}
					MyMessageboxConfig.show(report.getRingkasan(), "Laporan Upload Nilai", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}
			}
		});
		timerReport.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				Session sessionThread = null;
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

				rowhead.createCell(0).setCellValue("ID DATA");
				rowhead.createCell(1).setCellValue("NIM MAHASISWA");
				rowhead.createCell(2).setCellValue("NAMA MAHASISWA");
				rowhead.createCell(3).setCellValue("ID MATAKULIAH");
				rowhead.createCell(4).setCellValue("MATAKULIAH");
				rowhead.createCell(5).setCellValue("SEMESTER");
				rowhead.createCell(6).setCellValue("NILAI");
				rowhead.createCell(7).setCellValue("KETERANGAN");
				rowhead.createCell(8).setCellValue("");
				rowhead.createCell(9).setCellValue("PINDAH KE ID MATAKULIAH");
				rowhead.createCell(10).setCellValue("PINDAH KE SEMESTER");

				XSSFWorkbook workbookUpload;
				try {
					workbookUpload = new XSSFWorkbook(fileUpload.getAbsolutePath());

					XSSFSheet sheetUpload = workbookUpload.getSheetAt(0);
					int size = sheetUpload.getLastRowNum() + 1;
					/*
					 * WAJIB openSession(), BUKAN currentNativeSession(). Common.getSheetContentAsString()/
					 * getSheetContentAsLong() di dalam loop menutup native session ThreadLocal, sehingga
					 * session hasil currentNativeSession() sudah TERTUTUP saat dipakai -> "Session is closed!".
					 */
					Session session = HibernateUtil.openSession();
					sessionThread = session;
					int rowIndex = 1;
					for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {

						if (Common.getSheetContentAsString(sheetUpload, 1, i) == null) {
							break;
						}

						Long id = Common.getSheetContentAsLong(sheetUpload, 0, i);
						String nim = Common.getSheetContentAsString(sheetUpload, 1, i);

						Mahasiswa mahasiswa = (Mahasiswa) (session.createCriteria(Mahasiswa.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", nim.trim()))
								.setMaxResults(1).uniqueResult());
						if (mahasiswa == null) {
							report.gagal(i, "NIM: " + nim, "Mahasiswa tidak ditemukan", "Pastikan NIM mahasiswa terdaftar di sistem.");
							continue;
						}

						String nama = Common.getSheetContentAsString(sheetUpload, 2, i);
						Long idMatakuliah = Common.getSheetContentAsLong(sheetUpload, 3, i);
						String namaMatakuliah = Common.getSheetContentAsString(sheetUpload, 4, i);
						Integer semester = 0;
						String smt = Common.getSheetContentAsString(sheetUpload, 5, i);
						if (smt != null && smt.trim().length() == 5) {
							try {
								Integer tahun = Integer.parseInt(smt.substring(0, 4));
								Integer jenis = Integer.parseInt(smt.substring(4));

								String tahunAkademik = tahun + "/" + (tahun + 1);
								semester = Common.getSemester(mahasiswa.getTahunangkatan(), tahunAkademik,
										jenis.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
										mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
							} catch (Exception e) {
								semester = Common.getSheetContentAsInteger(sheetUpload, 5, i);
							}
						} else {
							semester = Common.getSheetContentAsInteger(sheetUpload, 5, i);
						}
						String n = Common.getSheetContentAsString(sheetUpload, 6, i);

						Double nilai = 0.0;
						if (!Common.isNumber(n)) {
							NilaiHuruf nilaiHuruf = (NilaiHuruf) session.createCriteria(NilaiHuruf.class)
									.add(Restrictions.ilike("nilaiHuruf", n.trim())).setMaxResults(1).uniqueResult();
							if (nilaiHuruf != null) {
								nilai = (nilaiHuruf.getMulai() + nilaiHuruf.getSampai()) / 2.0;
							}
						} else {
							nilai = Double.parseDouble(n.trim());
						}

						String statusKonversi = Common.getSheetContentAsString(sheetUpload, 7, i);

						if (idMatakuliah == null) {
							Number m = (Number) session.createCriteria(Matakuliah.class)
									.add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
									.add(Restrictions.or(
											Restrictions.ilike("nama", namaMatakuliah.trim(), MatchMode.EXACT),
											Restrictions.ilike("kode", namaMatakuliah.trim(), MatchMode.EXACT)))
									.setProjection(Projections.property("id")).setMaxResults(1).uniqueResult();
							idMatakuliah = m == null ? null : m.longValue();
						}
						if (idMatakuliah == null) {
							report.gagal(i, "NIM: " + nim + " | MK: " + namaMatakuliah, "Kode mata kuliah tidak ditemukan", "Periksa kode mata kuliah dan pastikan sudah terdaftar.");
						}

						Long idMatakuliahPindah = Common.getSheetContentAsLong(sheetUpload, 9, i);
						Integer semesterPindah = Common.getSheetContentAsInteger(sheetUpload, 10, i);
						if (nilai != null && idMatakuliah != null) {

							label.setValue("Upload data " + nim + " - " + nama + " - " + namaMatakuliah + " ("
									+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

							Detailperkuliahan detailperkuliahan = createDetailperkulihana(session, id, mahasiswa,
									semester, nim, idMatakuliah, nilai, idMatakuliahPindah, semesterPindah, tbmuser);

							if (detailperkuliahan != null) {
								report.sukses(i, "NIM: " + nim + " | MK: " + namaMatakuliah + " | Nilai: " + nilai, null);

								XSSFRow row = sheet.createRow(rowIndex);

								XSSFCell cell = row.createCell(0);
								cell.setCellStyle(lockedNumericStyle);
								cell.setCellValue(detailperkuliahan == null || detailperkuliahan.getId() == null ? -1L : detailperkuliahan.getId());

								cell = row.createCell(1);
								cell.setCellStyle(lockedNumericStyle);
								cell.setCellValue(nim);

								cell = row.createCell(2);
								cell.setCellStyle(lockedNumericStyle);
								cell.setCellValue(nama);

								cell = row.createCell(3);
								cell.setCellStyle(lockedNumericStyle);
								cell.setCellValue(idMatakuliah);

								cell = row.createCell(4);
								cell.setCellStyle(lockedNumericStyle);
								cell.setCellValue(namaMatakuliah);

								cell = row.createCell(5);
								cell.setCellStyle(lockedNumericStyle);
								cell.setCellValue(semester);

								cell = row.createCell(6);
								if (detailperkuliahan == null || detailperkuliahan.getPerkuliahan() == null) {
									cell.setCellStyle(notLocked);
								} else {
									cell.setCellStyle(lockedNumericStyle);
								}
								cell.setCellValue(detailperkuliahan == null ? 0.0 : detailperkuliahan.getTotalNilai());

								cell = row.createCell(7);
								cell.setCellStyle(lockedNumericStyle);
								cell.setCellValue(statusKonversi);

								row.createCell(8).setCellValue("");

								cell = row.createCell(9);
								if (detailperkuliahan == null
										|| (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null)) {
									cell.setCellStyle(lockedNumericStyle);
								} else {
									cell.setCellStyle(notLocked);
								}
								cell.setCellValue(idMatakuliahPindah == null ? "" : idMatakuliahPindah.toString());

								cell = row.createCell(10);
								if (detailperkuliahan == null
										|| (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null)) {
									cell.setCellStyle(lockedNumericStyle);
								} else {
									cell.setCellStyle(notLocked);
								}
								cell.setCellValue(semesterPindah == null ? "" : semesterPindah.toString());

								rowIndex++;
							}
						}
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
								"Menyimpan berkas Excel hasil proses Upload Nilai Mahasiswa",
								e,
								new String[] {
										"Pastikan berkas dengan nama yang sama tidak sedang dibuka oleh aplikasi lain (misalnya Microsoft Excel).",
										"Periksa ketersediaan ruang penyimpanan (disk space) pada server.",
										"Ulangi proses upload data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
					}

				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/penilaian/UploadNilaiMahasiswa.java:458");
					PesanFormalHelper.tampilkanGagalException(
							"Memproses berkas Excel Upload Nilai Mahasiswa",
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
							"Memproses berkas Excel Upload Nilai Mahasiswa", null, e1,
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
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) UploadNilaiMahasiswa laporan"); }

				HibernateUtil.closeSession();
				label.setValue("");

			} catch (Exception eOuter) {
					// FIX "gagal diam-diam"/hang selamanya: try terluar ini sebelumnya TIDAK
					// punya catch (hanya finally), jadi exception di luar blok try dalam akan
					// lolos tak tertangani keluar dari run() thread ini; label.setValue("")
					// tidak pernah tereksekusi dan popup progres tidak akan pernah tertutup.
					Common.tampilErrorJikaAdmin(eOuter);
					label.setValue("Error: " + PesanFormalHelper.pesanGagalException(
							"pemrosesan Upload Nilai Mahasiswa", null, eOuter,
							new String[] {
									"Pastikan berkas Excel yang di-upload sesuai format template yang telah ditentukan.",
									"Periksa koneksi ke database server dan ketersediaan ruang penyimpanan (disk space) pada server.",
									"Ulangi proses upload data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
				} finally {
					// Tutup session khusus thread ini + bersihkan ThreadLocal sisa helper Excel.
					ais.database.hibernate.HibernateUtil.closeSessionQuietly(sessionThread);
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
}
