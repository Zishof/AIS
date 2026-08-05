package ais.action.master.feeder.integrator.helper;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Calendar;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Konfigurasi;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import org.zkoss.zul.Timer;

public class UploadKrs extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();

	private File file;

	public UploadKrs() {
		super();
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public UploadKrs(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		borderlayout.setHeight("2000px");
		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);

		final Konfigurasi konfigurasi = Common.getKonfigurasi("aktifkan_upload_krs_di_feeder_integrator",
				Konfigurasi.TIDAK_AKTIF);

		final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Upload KRS" + Common.ukuranLabelFileUpload(),
				"/img/upload.png");
		button.setVisible(konfigurasi.getNilai().equals(Konfigurasi.AKTIF));
		button.setParent(toolbar);
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

		if (Common.getApakahAdmin()) {
			final MyToolbarbuttonConfig tidakAktifkan = new MyToolbarbuttonConfig("Tidak Aktifkan Upload",
					"/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig aktifkan = new MyToolbarbuttonConfig("Aktifkan Upload",
					"/img/svg/check2-circle.svg");
			aktifkan.setVisible(konfigurasi.getNilai().equals(Konfigurasi.TIDAK_AKTIF));
			tidakAktifkan.setVisible(konfigurasi.getNilai().equals(Konfigurasi.AKTIF));

			aktifkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					konfigurasi.setNilai(Konfigurasi.AKTIF);
					Common.refreshUpdate(konfigurasi);
					MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);
					button.setVisible(konfigurasi.getNilai().equals(Konfigurasi.AKTIF));
					tidakAktifkan.setVisible(konfigurasi.getNilai().equals(Konfigurasi.AKTIF));
					aktifkan.setVisible(konfigurasi.getNilai().equals(Konfigurasi.TIDAK_AKTIF));
				}
			});
			aktifkan.setParent(toolbar);

			tidakAktifkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					konfigurasi.setNilai(Konfigurasi.TIDAK_AKTIF);
					Common.refreshUpdate(konfigurasi);
					MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);
					button.setVisible(konfigurasi.getNilai().equals(Konfigurasi.AKTIF));
					tidakAktifkan.setVisible(konfigurasi.getNilai().equals(Konfigurasi.AKTIF));
					aktifkan.setVisible(konfigurasi.getNilai().equals(Konfigurasi.TIDAK_AKTIF));
				}
			});
			tidakAktifkan.setParent(toolbar);
		}

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ambil Data", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "KRS.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/UploadKrs.java:183");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
	}

	public static Detailperkuliahan createDetailperkulihana(Session session, Mahasiswa mahasiswa, Integer semester,
			String nim, String kodeMatakuliah, String kelas, String tahunAkademik, Tbmuser tbmuser,
			Integer statusSemesterPendek) {

		Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
				.createAlias("mahasiswa", "mahasiswa")

				.add(Restrictions.eq("semester", semester))

				.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN)
				.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
				.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)

				.add(Restrictions.eq("mahasiswa.nim", nim.trim()))

				.add(Restrictions.or(
						Restrictions.ilike("matakuliahKonversi.kode", kodeMatakuliah.trim(), MatchMode.EXACT),
						Restrictions.ilike("matakuliah.kode", kodeMatakuliah.trim(), MatchMode.EXACT)))

				.addOrder(Order.desc("id"))

				.setMaxResults(1).uniqueResult();

		if (detailperkuliahan == null && !kodeMatakuliah.trim().isEmpty() && Common.isNumber(kodeMatakuliah)) {

			detailperkuliahan = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
					.createAlias("mahasiswa", "mahasiswa")

					.add(Restrictions.eq("semester", semester))

					.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN)
					.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
					.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)

					.add(Restrictions.eq("mahasiswa.nim", nim.trim()))

					.add(Restrictions.or(Restrictions.eq("matakuliahKonversi.id", Long.parseLong(kodeMatakuliah)),
							Restrictions.eq("matakuliah.id", Long.parseLong(kodeMatakuliah))))

					.addOrder(Order.desc("id"))

					.setMaxResults(1).uniqueResult();
		}

		if (detailperkuliahan == null || detailperkuliahan.getPerkuliahan() == null
				|| (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null && kelas != null
						&& !kelas.trim().isEmpty() && detailperkuliahan.getPerkuliahan().getKelas() != null
						&& !detailperkuliahan.getPerkuliahan().getKelas().trim().equalsIgnoreCase(kelas.trim()))) {
			String kode = kodeMatakuliah.trim();
			Matakuliah matakuliah = (Matakuliah) session.createCriteria(Matakuliah.class)
					.add(Restrictions.ilike("kode", kode)).add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
					.setMaxResults(1).uniqueResult();

			if (matakuliah == null && !kode.trim().isEmpty() && Common.isNumber(kode)) {
				try {
					matakuliah = (Matakuliah) session.createCriteria(Matakuliah.class)
							.add(Restrictions.idEq(Long.parseLong(kode))).setMaxResults(1).uniqueResult();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/UploadKrs.java:253");
				}
			}

			if (matakuliah == null) {
				matakuliah = (Matakuliah) session.createCriteria(Matakuliah.class)
						.add(Restrictions.ilike("kode", kodeMatakuliah.trim())).setMaxResults(1).uniqueResult();
			}

			if (matakuliah == null) {
				return detailperkuliahan;
			}

			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) (matakuliah == null ? null
					: session.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("kurikulum", "kurikulum")
							.createAlias("kurikulum.program", "program")
							.add(Restrictions.eq("kurikulum.jurusan", mahasiswa.getJurusan()))
							.add(Restrictions.eq("program.nama", mahasiswa.getProgram()))
							.add(Restrictions.eq("matakuliah", matakuliah)).addOrder(Order.desc("kurikulum.tahun"))
							.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult());

			if (kurikulumPunyaMatakuliah == null) {
				Kurikulum kurikulum = (Kurikulum) session.createCriteria(Kurikulum.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.createAlias("program", "program").add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
						.add(Restrictions.eq("program.nama", mahasiswa.getProgram())).addOrder(Order.desc("tahun"))
						.setMaxResults(1).uniqueResult();

				if (kurikulum == null) {
					kurikulum = new Kurikulum();
					kurikulum.setNamaAsli(
							"Kurikulum " + mahasiswa.getJurusan().getNama() + " program " + mahasiswa.getProgram()
									+ " tahun " + ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
					kurikulum.setNama(kurikulum.getNamaAsli());
					kurikulum.setJurusan(mahasiswa.getJurusan());
					kurikulum.setProgram(new Program(mahasiswa.getProgram()));
					kurikulum.setTahun(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
					kurikulum.setJenisSemester(Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
					kurikulum.setTahunAkademik(Common.getCurrentTahunAkademik());
					session.getTransaction().begin();
					session.saveOrUpdate(kurikulum);
					session.getTransaction().commit();
				}

				kurikulumPunyaMatakuliah = new KurikulumPunyaMatakuliah();
				kurikulumPunyaMatakuliah.setMatakuliah(matakuliah);
				kurikulumPunyaMatakuliah.setSemester(semester);
				kurikulumPunyaMatakuliah.setKurikulum(kurikulum);
				session.getTransaction().begin();
				session.saveOrUpdate(kurikulumPunyaMatakuliah);
				session.getTransaction().commit();
			}

			if (detailperkuliahan == null) {
				detailperkuliahan = new Detailperkuliahan(tbmuser, UploadKrs.class);
			}
			detailperkuliahan.setPersetujuan(Detailperkuliahan.DISETUJUI);
			detailperkuliahan.setMahasiswa(mahasiswa);
			Perkuliahan perkuliahan = UploadKelas.checkPerkuliahan(session, kelas, tahunAkademik, tbmuser, null, null,
					null, kurikulumPunyaMatakuliah, mahasiswa.getProgram(), statusSemesterPendek, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, false);
			detailperkuliahan.setPerkuliahan(perkuliahan);
			detailperkuliahan.setSemester(semester);
			if (perkuliahan != null) {
				detailperkuliahan.setMatakuliahKonversi(null);
			}

		}

		detailperkuliahan.setSemester(semester);
		detailperkuliahan.setOleh(tbmuser.getUserNama());
		String olehId = Common.generateOlehId(tbmuser);
		detailperkuliahan.setOlehId(olehId);

		session.getTransaction().begin();
		session.saveOrUpdate(detailperkuliahan);
		session.getTransaction().commit();

		System.out.println("TA detailperkuliahan = " + detailperkuliahan.getTahunAkademik() + ", semester = "
				+ detailperkuliahan.getSemester() + ", detailperkuliahan.id = " + detailperkuliahan.getId());

		return detailperkuliahan;
	}

	@SuppressWarnings({})
	private void initSpreadsheet(final File fileUpload) throws Exception {

		final Tbmuser tbmuser = Common.getCurrentUser();

		Common.clear(center);

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload KRS");
		final Label downloadPath = new Label("");

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("KRS");
				sheet.setDefaultColumnWidth(20);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("Nama Mahasiswa");
				rowhead.createCell(2).setCellValue("Semester");
				rowhead.createCell(3).setCellValue("Kode MK");
				rowhead.createCell(4).setCellValue("Mata Kuliah");
				rowhead.createCell(5).setCellValue("Kelas");
				rowhead.createCell(6).setCellValue("Kode Prodi");

				XSSFWorkbook workbookUpload;
				{
					workbookUpload = new XSSFWorkbook(fileUpload.getAbsolutePath());

					XSSFSheet sheetUpload = workbookUpload.getSheetAt(0);
					int size = sheetUpload.getLastRowNum() + 1;

					XSSFCellStyle notLocked = workbook.createCellStyle();
					notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
					notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

					int rowIndex = 1;
					for (int i = 1; i < size; i++) {
						Session session = HibernateUtil.currentNativeSession();
						try {

							if (Common.getSheetContentAsString(sheetUpload, 0, i) == null) {
								continue;
							}

							String smt = Common.getSheetContentAsString(sheetUpload, 2, i);

							String nim = Common.getSheetContentAsString(sheetUpload, 0, i);

							String kodeMatakuliah = Common.getSheetContentAsString(sheetUpload, 3, i);

							String kelas = Common.getSheetContentAsString(sheetUpload, 5, i);

							Mahasiswa mahasiswa = (Mahasiswa) (session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("nim", nim.trim())).setMaxResults(1).uniqueResult());

							if (mahasiswa != null && kodeMatakuliah != null && !kodeMatakuliah.trim().isEmpty()
									&& smt != null && kelas != null && !kelas.trim().isEmpty()) {

								Integer semester = null;
								String tahunAkademik = null;

								Integer statusSemesterPendek = null;
								try {
									statusSemesterPendek = Integer.parseInt(smt.trim().substring(4)) == 3
											? Perkuliahan.SEMESTER_PENDEK
											: null;
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/UploadKrs.java:417");
									// TODO: handle exception
								}

								if (smt.length() > 4) {
									Integer tahun = Integer.parseInt(smt.substring(0, 4));
									Integer jenis = Integer.parseInt(smt.substring(4));

									tahunAkademik = tahun + "/" + (tahun + 1);
									semester = Common.getSemester(mahasiswa.getTahunangkatan(), tahunAkademik,
											jenis.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
											mahasiswa.getPindahKeKampusIniMasukSemester(),
											mahasiswa.getSemesterMulai());
								} else {
									semester = Integer.parseInt(smt);
									if (semester > 14) {
										continue;
									}

									Integer tahunAkademikMulai = Common.getTahunAkademik(semester,
											mahasiswa.getTahunangkatan(), mahasiswa.getSemesterMulai());
									tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
								}

								if (semester == null || tahunAkademik == null) {
									continue;
								}

								label.setValue("Upload data " + nim + " - " + kodeMatakuliah + " ("
										+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

								System.out.println("mahasiswa " + mahasiswa + ", tahunAkademik = " + tahunAkademik
										+ ", semester = " + semester + ", kodeMatakuliah " + kodeMatakuliah);

								Detailperkuliahan detailperkuliahan = createDetailperkulihana(session, mahasiswa,
										semester, nim, kodeMatakuliah, kelas, tahunAkademik, tbmuser,
										statusSemesterPendek);

								if (detailperkuliahan != null) {
									detailperkuliahan.setTahunAkademik(tahunAkademik);

									Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() == null
											? (detailperkuliahan.getPerkuliahan() == null ? null
													: detailperkuliahan.getPerkuliahan().getMatakuliah())
											: detailperkuliahan.getMatakuliahKonversi();
									if (matakuliah != null) {

										if (DownloadKrs.createData(session, sheet, rowIndex, detailperkuliahan, null,
												notLocked)) {
											rowIndex++;
											report.sukses(i, nim + "/" + kodeMatakuliah, "KRS berhasil");
										}
									}
								}

							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							report.gagal(i, "baris-" + i, e, "Periksa data NIM/MK KRS pada baris ini");
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
					}
				}

				System.out.println("Your excel file has been generated! ");

				HibernateUtil.closeSession();
				try { downloadPath.setValue(report.simpanLaporan().getAbsolutePath()); } catch (Exception rex) { ais.common.ErrorAuditUtil.record(rex, "UploadKrs-report"); }
				label.setValue("");

				} catch (Exception e1) {
					// FIX "gagal diam-diam": sebelumnya exception di sini hanya dicatat ke log audit
					// lalu label.setValue("") tetap dipanggil tanpa syarat (=SUKSES palsu) di luar try,
					// menutupi kegagalan dan membuat popup progres menutup seolah berhasil.
					Common.tampilErrorJikaAdmin(e1);
					label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
							"pemrosesan data KRS dari file Excel yang diunggah", null, e1,
							new String[] {
									"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
									"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
									"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

		final Timer timerReport = new Timer(500);
		timerReport.setParent(UploadKrs.this);
		timerReport.setRepeats(true);
		timerReport.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (label.getValue().isEmpty()) {
					timerReport.detach();
					if (!downloadPath.getValue().isEmpty()) {
						Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain");
					}
					MyMessageboxConfig.show(report.getRingkasan(), "Laporan Upload KRS", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				} else if (label.getValue().startsWith("Error:")) {
					timerReport.detach();
				}
			}
		});
		timerReport.start();

	}
}
