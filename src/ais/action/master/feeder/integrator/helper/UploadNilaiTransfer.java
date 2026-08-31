package ais.action.master.feeder.integrator.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
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
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Konfigurasi;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import org.zkoss.zul.Timer;

/**
 * Tipe khusus untuk upload nilai transfer. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code File file};
 * inisialisasi/lifecycle ({@code init()}, {@code initSpreadsheet()}); operasi domain lain ({@code
 * createDetailperkulihana()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class UploadNilaiTransfer extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();

	private File file;

	public UploadNilaiTransfer() {
		super();
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public UploadNilaiTransfer(String title, String border, boolean closable) {
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

		final Konfigurasi konfigurasi = Common.getKonfigurasi("aktifkan_upload_nilai_transfer_di_feeder_integrator",
				Konfigurasi.TIDAK_AKTIF);

		final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
				"Upload Nilai Transfer" + Common.ukuranLabelFileUpload(), "/img/upload.png");
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
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "NILAI TRANSFER.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/UploadNilaiTransfer.java:177");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
	}

	private Detailperkuliahan createDetailperkulihana(Session session, Mahasiswa mahasiswa, String nim,
			String kodeMatakuliah, Double nilai, String kodeMatakuliahAsal, String namaMatakuliahAsal, Integer sksAsal,
			String nilaiHurufAsal, Tbmuser tbmuser) {

		Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
				.createAlias("mahasiswa", "mahasiswa")

				.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.INNER_JOIN)

				.add(Restrictions.eq("mahasiswa.nim", nim.trim())).addOrder(Order.desc("id"))
				.add(Restrictions.ilike("matakuliahKonversi.kode", kodeMatakuliah.trim(), MatchMode.EXACT))

				.setMaxResults(1).uniqueResult();

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

			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) (matakuliah == null ? null
					: session.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("kurikulum", "kurikulum")
							.createAlias("kurikulum.program", "program")
							.add(Restrictions.eq("kurikulum.jurusan", mahasiswa.getJurusan()))
							.add(Restrictions.eq("program.nama", mahasiswa.getProgram()))
							.add(Restrictions.eq("matakuliah", matakuliah)).addOrder(Order.desc("kurikulum.tahun"))
							.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult());

			if (detailperkuliahan == null) {
				detailperkuliahan = new Detailperkuliahan(tbmuser, UploadNilaiTransfer.class);
			}
			detailperkuliahan.setMahasiswa(mahasiswa);
			detailperkuliahan.setMatakuliahKonversi(matakuliah);
			detailperkuliahan
					.setSemester(kurikulumPunyaMatakuliah == null ? 0 : kurikulumPunyaMatakuliah.getSemester());

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
		detailperkuliahan.setKodeMatakuliahAsal(kodeMatakuliahAsal);
		detailperkuliahan.setNamaMatakuliahAsal(namaMatakuliahAsal);
		detailperkuliahan.setNilaiHurufAsal(nilaiHurufAsal);
		detailperkuliahan.setSksAsal(sksAsal);

		detailperkuliahan.setOleh(tbmuser.getUserNama());

		String olehId = Common.generateOlehId(tbmuser);
		detailperkuliahan.setOlehId(olehId);

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
		try {
			session.saveOrUpdate(detailperkuliahan);
			session.getTransaction().commit();
		} catch (RuntimeException eSimpan) {
			/*
			 * WAJIB rollback. Tanpa ini transaksi tetap AKTIF, sehingga begin() pada
			 * baris berikutnya melempar "Transaction already active".
			 */
			try {
				session.getTransaction().rollback();
			} catch (Exception eRoll) {
				ais.common.ErrorAuditUtil.record(eRoll, "rollback-gagal-upload "
					+ "src/ais/action/master/feeder/integrator/helper/UploadNilaiTransfer.java");
			}
			throw eSimpan;
		}

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

		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Nilai Transfer");
		final Label downloadPath = new Label("");

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("NILAI");
				sheet.setDefaultColumnWidth(20);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("Nama Mahasiswa");
				rowhead.createCell(2).setCellValue("Kode MK Asal");
				rowhead.createCell(3).setCellValue("Nama Mata Kuliah Asal");
				rowhead.createCell(4).setCellValue("SKS Asal");
				rowhead.createCell(5).setCellValue("Nilai Huruf Asal");
				rowhead.createCell(6).setCellValue("Kode Matakuliah Diakui");
				rowhead.createCell(7).setCellValue("Nama Matakuliah Diakui");
				rowhead.createCell(8).setCellValue("Nilai Huruf Diakui");
				rowhead.createCell(9).setCellValue("Nilai Angka Diakui");
				rowhead.createCell(10).setCellValue("Kode Prodi");

				XSSFWorkbook workbookUpload;
				{
					workbookUpload = new XSSFWorkbook(fileUpload.getAbsolutePath());

					XSSFSheet sheetUpload = workbookUpload.getSheetAt(0);
					int size = sheetUpload.getLastRowNum() + 1;

					int rowIndex = 1;
					for (int i = 1; i < size; i++) {
						/*
						 * WAJIB openSession(), BUKAN currentNativeSession(). Pola currentNativeSession()
						 * yang ditutup manual di akhir tiap iterasi rentan terhadap "Session is closed!"
						 * bila helper Excel lain (mis. Common.getSheetContentAsObject) ikut menutup
						 * native session ThreadLocal di tengah pemrosesan baris ini.
						 */
						Session session = HibernateUtil.openSession();
						try {

							if (Common.getSheetContentAsString(sheetUpload, 0, i) == null) {
								continue;
							}

							String nim = Common.getSheetContentAsString(sheetUpload, 0, i);

							Mahasiswa mahasiswa = (Mahasiswa) (session.createCriteria(Mahasiswa.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", nim.trim()))
									.setMaxResults(1).uniqueResult());

							if (mahasiswa != null) {

								String kodeMatakuliah = Common.getSheetContentAsString(sheetUpload, 6, i);

								String kodeMatakuliahAsal = Common.getSheetContentAsString(sheetUpload, 2, i);
								String namaMatakuliahAsal = Common.getSheetContentAsString(sheetUpload, 3, i);
								Integer sksAsal = Common.getSheetContentAsInteger(sheetUpload, 4, i);
								String nilaiHurufAsal = Common.getSheetContentAsString(sheetUpload, 5, i);

								Double nilai = 0.0;
								String huruf = Common.getSheetContentAsString(sheetUpload, 8, i);

								if ((nilai == null || nilai < 0.01) && huruf != null && !huruf.trim().isEmpty()) {

									NilaiHuruf nilaiHuruf = (NilaiHuruf) session.createCriteria(NilaiHuruf.class)
											.add(Restrictions.ilike("nilaiHuruf", huruf)).setMaxResults(1)
											.uniqueResult();
									if (nilaiHuruf != null) {
										nilai = (nilaiHuruf.getMulai() + nilaiHuruf.getSampai()) / 2.0;
									}

								}

								label.setValue("Upload data " + nim + " - " + kodeMatakuliah + " - " + nilai + " ("
										+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

								Detailperkuliahan detailperkuliahan = createDetailperkulihana(session, mahasiswa, nim,
										kodeMatakuliah, nilai, kodeMatakuliahAsal, namaMatakuliahAsal, sksAsal,
										nilaiHurufAsal, tbmuser);

								if (detailperkuliahan != null) {

									Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() == null
											? (detailperkuliahan.getPerkuliahan() == null ? null
													: detailperkuliahan.getPerkuliahan().getMatakuliah())
											: detailperkuliahan.getMatakuliahKonversi();
									if (matakuliah != null) {

										DownloadNilaiTransfer.createData(session, sheet, rowIndex, detailperkuliahan);

										rowIndex++;
									report.sukses(i, nim + "/" + kodeMatakuliah, "Nilai Transfer berhasil");
									}
								}

							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							report.gagal(i, "baris-" + i, e, "Periksa data NIM/MK Transfer pada baris ini");
						} finally {
							// Tutup session khusus baris ini + bersihkan ThreadLocal sisa helper Excel.
							HibernateUtil.closeSessionQuietly(session);
							HibernateUtil.closeSession();
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
					}
				}

				System.out.println("Your excel file has been generated! ");

				HibernateUtil.closeSession();
				try { downloadPath.setValue(report.simpanLaporan().getAbsolutePath()); } catch (Exception rex) { ais.common.ErrorAuditUtil.record(rex, "UploadNilaiTransfer-report"); }
				label.setValue("");

				} catch (Exception e1) {
					// FIX "gagal diam-diam": sebelumnya exception di sini hanya dicatat ke log audit
					// lalu label.setValue("") tetap dipanggil tanpa syarat (=SUKSES palsu) di luar try,
					// menutupi kegagalan dan membuat popup progres menutup seolah berhasil.
					Common.tampilErrorJikaAdmin(e1);
					label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
							"pemrosesan data Nilai Transfer dari file Excel yang diunggah", null, e1,
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
		timerReport.setParent(UploadNilaiTransfer.this);
		timerReport.setRepeats(true);
		timerReport.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (label.getValue().isEmpty()) {
					timerReport.detach();
					if (!downloadPath.getValue().isEmpty()) {
						Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain");
					}
					MyMessageboxConfig.show(report.getRingkasan(), "Laporan Upload Nilai Transfer", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				} else if (label.getValue().startsWith("Error:")) {
					timerReport.detach();
				}
			}
		});
		timerReport.start();

	}
}
