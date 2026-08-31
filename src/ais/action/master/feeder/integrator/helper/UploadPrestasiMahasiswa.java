package ais.action.master.feeder.integrator.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
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
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CabangPrestasiMahasiswa;
import ais.database.model.KategoriPrestasiMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PrestasiMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import org.zkoss.zul.Timer;

/**
 * Tipe khusus untuk upload prestasi mahasiswa. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code File file};
 * inisialisasi/lifecycle ({@code init()}, {@code initSpreadsheet()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class UploadPrestasiMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private File file;

	public UploadPrestasiMahasiswa() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public UploadPrestasiMahasiswa(String title, String border, boolean closable) {
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

		final Konfigurasi konfigurasi = Common.getKonfigurasi("aktifkan_upload_prestasi_di_feeder_integrator",
				Konfigurasi.TIDAK_AKTIF);

		final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
				"Upload Prestasi" + Common.ukuranLabelFileUpload(), "/img/upload.png");
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
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "prestasi.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/UploadPrestasiMahasiswa.java:171");

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

		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Prestasi Mahasiswa");
		final Label downloadPath = new Label("");

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("Prestasi Mahasiswa");
				sheet.setDefaultColumnWidth(20);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("Nama");
				rowhead.createCell(2).setCellValue("Jenis Prestasi");
				rowhead.createCell(3).setCellValue("Tingkat Prestasi");
				rowhead.createCell(4).setCellValue("Nama Prestasi");
				rowhead.createCell(5).setCellValue("Tahun");
				rowhead.createCell(6).setCellValue("Penyelenggara");
				rowhead.createCell(7).setCellValue("Peringkat");
				rowhead.createCell(8).setCellValue("Kode Prodi");

				XSSFWorkbook workbookUpload;
				{
					workbookUpload = new XSSFWorkbook(fileUpload.getAbsolutePath());

					XSSFSheet sheetUpload = workbookUpload.getSheetAt(0);
					int size = sheetUpload.getLastRowNum() + 1;

					int rowIndex = 1;
					for (int i = 1; i < size; i++) {
						/*
						 * WAJIB openSession(), BUKAN currentNativeSession(). Common.getSheetContentAsObject()
						 * di dalam pemrosesan baris menutup native session ThreadLocal
						 * (HibernateUtil.closeSession()), sehingga session hasil currentNativeSession()
						 * sudah TERTUTUP saat getTransaction().begin() dipanggil -> "Session is closed!"
						 * di SETIAP baris.
						 */
						Session session = HibernateUtil.openSession();
						try {

							if (Common.getSheetContentAsString(sheetUpload, 0, i) == null) {
								continue;
							}

							Mahasiswa mahasiswaMentah = (Mahasiswa) Common.getSheetContentAsObject(sheetUpload, 0, i,
									Mahasiswa.class);

							// Reload ke session khusus baris ini agar entitas managed (bukan detached
							// dari session lain yang sudah tertutup) -> akses lazy field & simpan aman.
							Mahasiswa mahasiswa = mahasiswaMentah == null ? null
									: (Mahasiswa) session.get(Mahasiswa.class, mahasiswaMentah.getId());

							if (mahasiswa != null) {
								String nama = Common.getSheetContentAsString(sheetUpload, 4, i);
								CabangPrestasiMahasiswa cabangPrestasiMahasiswa = (CabangPrestasiMahasiswa) Common
										.getSheetContentAsObject(sheetUpload, 2, i, CabangPrestasiMahasiswa.class);
								KategoriPrestasiMahasiswa kategoriPrestasiMahasiswa = (KategoriPrestasiMahasiswa) Common
										.getSheetContentAsObject(sheetUpload, 3, i, KategoriPrestasiMahasiswa.class);
								Integer tahun = Common.getSheetContentAsInteger(sheetUpload, 5, i);
								Integer peringkat = Common.getSheetContentAsInteger(sheetUpload, 7, i);
								String penyelenggara = Common.getSheetContentAsString(sheetUpload, 6, i);

								if (nama != null && !nama.trim().isEmpty() && cabangPrestasiMahasiswa != null
										&& kategoriPrestasiMahasiswa != null && tahun != null
										&& kategoriPrestasiMahasiswa.getKode() != null) {
									PrestasiMahasiswa prestasiMahasiswa = (PrestasiMahasiswa) session
											.createCriteria(PrestasiMahasiswa.class)
											.add(Restrictions.eq("mahasiswa", mahasiswa))
											.add(Restrictions.eq("cabangPrestasiMahasiswa", cabangPrestasiMahasiswa))
											.add(Restrictions.eq("kategoriPrestasiMahasiswa",
													kategoriPrestasiMahasiswa))
											.add(Restrictions.ilike("nama", nama, MatchMode.EXACT))
											.add(Restrictions.eq("tahun", tahun)).setMaxResults(1).uniqueResult();
									if (prestasiMahasiswa == null) {
										prestasiMahasiswa = new PrestasiMahasiswa();
										prestasiMahasiswa.setJuara("Juara ke " + peringkat);
										prestasiMahasiswa.setStatus(PrestasiMahasiswa.DISETUJUI);
										prestasiMahasiswa.setFakultas(mahasiswa.getJurusan().getFakultas());
										prestasiMahasiswa.setJurusan(mahasiswa.getJurusan());
										prestasiMahasiswa.setTanggal(ais.ui.util.WaktuUtil.getDate());
										prestasiMahasiswa.setTanggalSelesai(ais.ui.util.WaktuUtil.getDate());
										prestasiMahasiswa.setTempat("");
										prestasiMahasiswa.setPrestasiLuarKampus(
												!kategoriPrestasiMahasiswa.getKode().equals("1"));

									}
									prestasiMahasiswa.setMahasiswa(mahasiswa);
									prestasiMahasiswa.setCabangPrestasiMahasiswa(cabangPrestasiMahasiswa);
									prestasiMahasiswa.setKategoriPrestasiMahasiswa(kategoriPrestasiMahasiswa);
									prestasiMahasiswa.setNama(nama);
									prestasiMahasiswa.setTahun(tahun);
									prestasiMahasiswa.setTahunAkademik(tahun + "/" + (tahun + 1));
									prestasiMahasiswa.setPenyelenggara(penyelenggara);
									prestasiMahasiswa.setPeringkat(peringkat);

									prestasiMahasiswa.setOleh(tbmuser.getUserNama());

									String olehId = Common.generateOlehId(tbmuser);
									prestasiMahasiswa.setOlehId(olehId);

									session.getTransaction().begin();
									try {
										Common.refreshSaveOrUpdate(session, prestasiMahasiswa);
										session.getTransaction().commit();
									} catch (Exception eSimpan) {
										/*
										 * WAJIB rollback. Tanpa ini transaksi tetap AKTIF, sehingga begin() pada
										 * baris berikutnya melempar "Transaction already active".
										 */
										try {
											session.getTransaction().rollback();
										} catch (Exception eRoll) {
											ais.common.ErrorAuditUtil.record(eRoll, "rollback-gagal-upload "
												+ "src/ais/action/master/feeder/integrator/helper/UploadPrestasiMahasiswa.java");
										}
										throw eSimpan;
									}

									XSSFRow row = sheet.createRow(rowIndex);

									row.createCell(0).setCellValue(prestasiMahasiswa.getMahasiswa().getNim());
									row.createCell(1).setCellValue(prestasiMahasiswa.getMahasiswa().getNama());
									row.createCell(2)
											.setCellValue(prestasiMahasiswa.getCabangPrestasiMahasiswa() == null ? "9"
													: prestasiMahasiswa.getCabangPrestasiMahasiswa().getKode());
									row.createCell(3)
											.setCellValue(prestasiMahasiswa.getKategoriPrestasiMahasiswa() == null ? "9"
													: prestasiMahasiswa.getKategoriPrestasiMahasiswa().getKode());
									row.createCell(4).setCellValue(prestasiMahasiswa.getNama());
									row.createCell(5).setCellValue(prestasiMahasiswa.getTahun());
									row.createCell(6).setCellValue(prestasiMahasiswa.getPenyelenggara());
									row.createCell(7).setCellValue(prestasiMahasiswa.getPeringkat() == null ? ""
											: prestasiMahasiswa.getPeringkat().toString());

									row.createCell(8).setCellValue(
											prestasiMahasiswa.getMahasiswa().getJurusan().getKodeEpsbed());

									label.setValue("Sedang memproses data " + prestasiMahasiswa.toString() + " ("
											+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

									rowIndex++;
									report.sukses(i, mahasiswa.getNim() + "/" + nama, "Prestasi berhasil diproses");
								}

							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							report.gagal(i, "baris-" + i, e, "Periksa data NIM/Prestasi pada baris ini");
						} finally {
							// Tutup session khusus baris ini + bersihkan ThreadLocal sisa helper Excel.
							HibernateUtil.closeSessionQuietly(session);
							HibernateUtil.closeSession();
						}
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
				try { downloadPath.setValue(report.simpanLaporan().getAbsolutePath()); } catch (Exception rex) { ais.common.ErrorAuditUtil.record(rex, "UploadPrestasiMahasiswa-report"); }
				label.setValue("");

				} catch (Exception e1) {
					// FIX "gagal diam-diam": sebelumnya exception di sini hanya dicatat ke log audit
					// lalu label.setValue("") tetap dipanggil tanpa syarat (=SUKSES palsu) di luar try,
					// menutupi kegagalan dan membuat popup progres menutup seolah berhasil.
					Common.tampilErrorJikaAdmin(e1);
					label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
							"pemrosesan data Prestasi Mahasiswa dari file Excel yang diunggah", null, e1,
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
		timerReport.setParent(UploadPrestasiMahasiswa.this);
		timerReport.setRepeats(true);
		timerReport.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (label.getValue().isEmpty()) {
					timerReport.detach();
					if (!downloadPath.getValue().isEmpty()) {
						Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain");
					}
					MyMessageboxConfig.show(report.getRingkasan(), "Laporan Upload Prestasi Mahasiswa", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				} else if (label.getValue().startsWith("Error:")) {
					timerReport.detach();
				}
			}
		});
		timerReport.start();

	}

}
