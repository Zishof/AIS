package ais.action.master.feeder.integrator.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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
import ais.common.ConstantValues;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.StatusKeluar;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import org.zkoss.zul.Timer;

public class UploadKelulusan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private File file;

	public UploadKelulusan() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public UploadKelulusan(String title, String border, boolean closable) {
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

		final Konfigurasi konfigurasi = Common.getKonfigurasi("aktifkan_upload_kelulusan_di_feeder_integrator",
				Konfigurasi.TIDAK_AKTIF);

		final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
				"Upload Kelulusan" + Common.ukuranLabelFileUpload(), "/img/upload.png");
		button.setParent(toolbar);
		button.setVisible(konfigurasi.getNilai().equals(Konfigurasi.AKTIF));
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
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "KELULUSAN.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/UploadKelulusan.java:174");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
	}

	@SuppressWarnings({})
	private void initSpreadsheet(final File fileUpload) throws Exception {

		final Tbmuser tbmuser = Common.getCurrentUser();

		Common.clear(center);

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Kelulusan");
		final Label downloadPath = new Label("");

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("KELULUSAN");
				sheet.setDefaultColumnWidth(20);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("Nama");
				rowhead.createCell(2).setCellValue("Jenis Keluar");
				rowhead.createCell(3).setCellValue("Tanggal Keluar");
				rowhead.createCell(4).setCellValue("Semester Keluar");
				rowhead.createCell(5).setCellValue("SK Yudisium");
				rowhead.createCell(6).setCellValue("Tanggal SK Yudisium");
				rowhead.createCell(7).setCellValue("IPK");
				rowhead.createCell(8).setCellValue("No Seri Ijasah");
				rowhead.createCell(9).setCellValue("Jenis Tugas Akhir");
				rowhead.createCell(10).setCellValue("Judul Skripsi");

				rowhead.createCell(11).setCellValue("Pembimbing I");
				rowhead.createCell(12).setCellValue("Pembimbing II");
				rowhead.createCell(13).setCellValue("Pembimbing III");
				rowhead.createCell(14).setCellValue("Penguji I");
				rowhead.createCell(15).setCellValue("Penguji II");
				rowhead.createCell(16).setCellValue("Penguji III");
				rowhead.createCell(17).setCellValue("Lokasi");
				rowhead.createCell(18).setCellValue("Nomor SK Tugas");
				rowhead.createCell(19).setCellValue("Tanggal SK Tugas");
				rowhead.createCell(20).setCellValue("Kode Prodi");

				XSSFWorkbook workbookUpload;
				{
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

							Mahasiswa mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheetUpload, 0, i,
									Mahasiswa.class);

							if (mahasiswa != null) {

								String dosen1 = Common.getSheetContentAsString(sheetUpload, 11, i);
								String dosen2 = Common.getSheetContentAsString(sheetUpload, 12, i);
								String dosen3 = Common.getSheetContentAsString(sheetUpload, 13, i);

								String penguji1 = Common.getSheetContentAsString(sheetUpload, 14, i);
								String penguji2 = Common.getSheetContentAsString(sheetUpload, 15, i);
								String penguji3 = Common.getSheetContentAsString(sheetUpload, 16, i);

								String statuskeluar = Common.getSheetContentAsString(sheetUpload, 2, i);

								String id_smt = Common.getSheetContentAsString(sheetUpload, 4, i);

								Dosen d1 = (Dosen) (dosen1 == null || dosen1.trim().isEmpty()
										|| dosen1.trim().length() < 4
												? null
												: session.createCriteria(Dosen.class)
														.add(Restrictions.eq("nidn", dosen1)).addOrder(Order.desc("id"))
														.setMaxResults(1).uniqueResult());

								Dosen d2 = (Dosen) (dosen2 == null || dosen2.trim().isEmpty()
										|| dosen2.trim().length() < 4
												? null
												: session.createCriteria(Dosen.class)
														.add(Restrictions.eq("nidn", dosen2)).addOrder(Order.desc("id"))
														.setMaxResults(1).uniqueResult());

								Dosen d3 = (Dosen) (dosen3 == null || dosen3.trim().isEmpty()
										|| dosen3.trim().length() < 4
												? null
												: session.createCriteria(Dosen.class)
														.add(Restrictions.eq("nidn", dosen3)).addOrder(Order.desc("id"))
														.setMaxResults(1).uniqueResult());

								Dosen p1 = (Dosen) (penguji1 == null || penguji1.trim().isEmpty()
										|| penguji1.trim().length() < 4
												? null
												: session.createCriteria(Dosen.class)
														.add(Restrictions.eq("nidn", penguji1))
														.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult());

								Dosen p2 = (Dosen) (penguji2 == null || penguji2.trim().isEmpty()
										|| penguji2.trim().length() < 4
												? null
												: session.createCriteria(Dosen.class)
														.add(Restrictions.eq("nidn", penguji2))
														.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult());

								Dosen p3 = (Dosen) (penguji3 == null || penguji3.trim().isEmpty()
										|| penguji3.trim().length() < 4
												? null
												: session.createCriteria(Dosen.class)
														.add(Restrictions.eq("nidn", penguji3))
														.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult());

								Skripsi skripsi = (Skripsi) session.createCriteria(Skripsi.class)
										.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();
								if (skripsi == null) {
									skripsi = new Skripsi();
								}

								try {
									int tahun = Integer.parseInt(id_smt.trim().substring(0, 4));
									skripsi.setTahunAkademik(tahun + "/" + (tahun + 1));
									Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(),
											skripsi.getTahunAkademik(),
											id_smt.trim().substring(4, 5).equals("1") ? Perkuliahan.GANJIL
													: Perkuliahan.GENAP,
											mahasiswa.getPindahKeKampusIniMasukSemester(),
											mahasiswa.getSemesterMulai());
									skripsi.setSemester(smt);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/UploadKelulusan.java:328");

								}

								skripsi.setJudul(Common.getSheetContentAsString(sheetUpload, 10, i));
								skripsi.setLokasiUjian(Common.getSheetContentAsString(sheetUpload, 17, i));
								skripsi.setNomorSk(Common.getSheetContentAsString(sheetUpload, 18, i));

								try {
									skripsi.setTglSk(Common.databaseDateFormat.get()
											.parse(Common.getSheetContentAsString(sheetUpload, 19, i)));
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/UploadKelulusan.java:339");

								}

								if (d1 != null) {
									skripsi.setPembimbing(d1);
								}
								if (d2 != null) {
									skripsi.setKetuaSidang(d2);
								}
								if (d3 != null) {
									skripsi.setPembimbing3(d3);
								}

								if (p1 != null) {
									skripsi.setPenguji1(p1);
								}
								if (p2 != null) {
									skripsi.setPenguji2(p2);
								}
								if (p3 != null) {
									skripsi.setPenguji3(p3);
								}

								// try {
								// skripsi.setAwalBimbingan(Common.databaseDateFormat.get().parse(awal.trim()));
								// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/UploadKelulusan.java:365");
								//
								// }
								//
								// try {
								// skripsi.setAkhirBimbingan(Common.databaseDateFormat.get().parse(akhir.trim()));
								// } catch (Exception e) {
								//
								// }

								StatusKeluar statusKeluar = (StatusKeluar) (statuskeluar == null
										|| statuskeluar.trim().isEmpty()
												? null
												: session.createCriteria(StatusKeluar.class)
														.add(Restrictions.eq("feeder", statuskeluar)).setMaxResults(1)
														.uniqueResult());

								if (statusKeluar != null) {
									Integer semesterLulus = Mahasiswa.hitungSmtLulus(statusKeluar, mahasiswa);
									mahasiswa.setSemesterLulus(semesterLulus);
									mahasiswa.setStatusKeluar(statusKeluar);
								}

								try {
									mahasiswa.setTanggalLulus(Common.databaseDateFormat.get()
											.parse(Common.getSheetContentAsString(sheetUpload, 3, i)));
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/UploadKelulusan.java:391");

								}

								mahasiswa.setNoAkta1(Common.getSheetContentAsString(sheetUpload, 5, i));

								try {
									mahasiswa.setTanggalYudisium(Common.databaseDateFormat.get()
											.parse(Common.getSheetContentAsString(sheetUpload, 6, i)));
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/UploadKelulusan.java:400");

								}

								mahasiswa.setNoIjazah2(Common.getSheetContentAsString(sheetUpload, 8, i));

								String olehId = Common.generateOlehId(tbmuser);
								mahasiswa.setOlehId(olehId);
								mahasiswa.setOleh(tbmuser == null ? null : tbmuser.getUserId());
								skripsi.setOlehId(olehId);
								skripsi.setOleh(tbmuser == null ? null : tbmuser.getUserId());

								skripsi.setMahasiswa(mahasiswa);

								mahasiswa.setJudulSkripsi(skripsi.getJudul());

								session.getTransaction().begin();
								Common.refreshUpdate(session, mahasiswa);
								Common.refreshSaveOrUpdate(session, skripsi);
								session.getTransaction().commit();

								XSSFRow row = sheet.createRow(rowIndex);
								KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(skripsi.getMahasiswa(),
										skripsi.getSemester(), null, null);

								row.createCell(0).setCellValue(skripsi.getMahasiswa().getNim());
								row.createCell(1).setCellValue(skripsi.getMahasiswa().getNama());
								row.createCell(2).setCellValue(skripsi.getMahasiswa().getStatusKeluar() == null ? ""
										: skripsi.getMahasiswa().getStatusKeluar().getFeeder());
								row.createCell(3).setCellValue(skripsi.getMahasiswa().getTanggalLulus() == null ? ""
										: Common.databaseDateFormat.get().format(skripsi.getMahasiswa().getTanggalLulus()));

								row.createCell(4).setCellValue(id_smt);

								row.createCell(5).setCellValue(skripsi.getMahasiswa().getNoAkta2());

								row.createCell(6)
										.setCellValue(skripsi.getMahasiswa().getTanggalYudisium() == null ? ""
												: Common.databaseDateFormat.get()
														.format(skripsi.getMahasiswa().getTanggalYudisium()));
								row.createCell(7).setCellValue(krsMahasiswa.getIpk());

								row.createCell(8).setCellValue(skripsi.getMahasiswa().getNoIjazah1());
								try {
									row.createCell(9).setCellValue(skripsi.getMahasiswa().getJurusan().getJenjang()
											.getId().equals(ConstantValues.s1.getId()) ? 2 : 3);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/UploadKelulusan.java:446");
									// TODO: handle exception
								}
								row.createCell(10).setCellValue(skripsi.getJudul());

								row.createCell(11).setCellValue(
										skripsi.getPembimbing() == null ? "" : skripsi.getPembimbing().getNidn());
								row.createCell(12).setCellValue(
										skripsi.getKetuaSidang() == null ? "" : skripsi.getKetuaSidang().getNidn());
								row.createCell(13).setCellValue(
										skripsi.getPembimbing3() == null ? "" : skripsi.getPembimbing3().getNidn());

								row.createCell(14).setCellValue(
										skripsi.getPenguji1() == null ? "" : skripsi.getPenguji1().getNidn());
								row.createCell(15).setCellValue(
										skripsi.getPenguji2() == null ? "" : skripsi.getPenguji2().getNidn());
								row.createCell(16).setCellValue(
										skripsi.getPenguji3() == null ? "" : skripsi.getPenguji3().getNidn());

								row.createCell(17).setCellValue(skripsi.getLokasiUjian());
								row.createCell(18).setCellValue(skripsi.getNomorSk());

								row.createCell(19).setCellValue(skripsi.getTglSk() == null ? ""
										: Common.databaseDateFormat.get().format(skripsi.getTglSk()));

								row.createCell(20).setCellValue(skripsi.getMahasiswa().getJurusan().getKodeEpsbed());

								rowIndex++;
								report.sukses(i, mahasiswa.getNim(), "Kelulusan berhasil");

							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							report.gagal(i, "baris-" + i, e, "Periksa data NIM/Kelulusan pada baris ini");
						}

						HibernateUtil.closeSession();
					}

					Common.setStyled(sheet);sizedata.setValue(rowIndex + 1);

					try {
						FileOutputStream fileOut = new FileOutputStream(filename);
						workbook.write(fileOut);
						fileOut.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						Common.tampilErrorJikaAdmin(e);
					}
				}

				System.out.println("Your excel file has been generated! " );

				HibernateUtil.closeSession();
				try { downloadPath.setValue(report.simpanLaporan().getAbsolutePath()); } catch (Exception rex) { ais.common.ErrorAuditUtil.record(rex, "UploadKelulusan-report"); }
				label.setValue("");

				} catch (Exception e1) {
					// FIX "gagal diam-diam": sebelumnya exception di sini hanya dicatat ke log audit
					// lalu label.setValue("") tetap dipanggil tanpa syarat (=SUKSES palsu) di luar try,
					// menutupi kegagalan dan membuat popup progres menutup seolah berhasil.
					Common.tampilErrorJikaAdmin(e1);
					label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
							"pemrosesan data Kelulusan dari file Excel yang diunggah", null, e1,
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
		timerReport.setParent(UploadKelulusan.this);
		timerReport.setRepeats(true);
		timerReport.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (label.getValue().isEmpty()) {
					timerReport.detach();
					if (!downloadPath.getValue().isEmpty()) {
						Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain");
					}
					MyMessageboxConfig.show(report.getRingkasan(), "Laporan Upload Kelulusan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				} else if (label.getValue().startsWith("Error:")) {
					timerReport.detach();
				}
			}
		});
		timerReport.start();

	}

}
