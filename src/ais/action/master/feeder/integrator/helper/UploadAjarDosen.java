package ais.action.master.feeder.integrator.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import org.zkoss.zul.Timer;

/**
 * Tipe khusus untuk upload ajar dosen. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Combobox
 * searchfakultas}, {@code Combobox searchjurusan}, {@code Combobox searchprogram}, {@code File file};
 * inisialisasi/lifecycle ({@code init()}, {@code initSpreadsheet()}); validasi/perhitungan ({@code
 * checkPerkuliahan()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class UploadAjarDosen extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchprogram = new Combobox();
	private File file;

	public UploadAjarDosen() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public UploadAjarDosen(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.initPrograms(searchprogram);

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
		// FIX toolbar tidak tampil (mis. tombol "Ambil Data"): pada ZK5 region North
		// memakai tinggi bawaan (+-100px); dengan flex=true isinya diregangkan ke tinggi
		// tersebut sehingga Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong.
		// Disamakan dengan layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs,
		// DownloadNilai): flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman
		// bila baris filter bertambah di kemudian hari.
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Jurusan") + " *"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program *"));
		row.appendChild(searchprogram);
		searchprogram.setReadonly(true);
		searchprogram.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		final Konfigurasi konfigurasi = Common.getKonfigurasi("aktifkan_upload_ajar_dosen_di_feeder_integrator",
				Konfigurasi.TIDAK_AKTIF);

		final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Upload Kelas" + Common.ukuranLabelFileUpload(),
				"/img/upload.png");
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
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "KELAS.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/UploadAjarDosen.java:221");

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
		final Jurusan jurusan = searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) searchjurusan.getSelectedItem().getValue();
		final String program = searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: (String) searchprogram.getSelectedItem().getValue();

		if (jurusan == null || program == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " dan program harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		final Tbmuser tbmuser = Common.getCurrentUser();

		Common.clear(center);

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Ajar Dosen");
		final Label downloadPath = new Label("");

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("Ajar Dosen");
				sheet.setDefaultColumnWidth(20);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("semester");
				rowhead.createCell(1).setCellValue("NIDN");
				rowhead.createCell(2).setCellValue("Nama Dosen");
				rowhead.createCell(3).setCellValue("kode matakuliah");
				rowhead.createCell(4).setCellValue("Nama Kelas");
				rowhead.createCell(5).setCellValue("Tatap Muka");
				rowhead.createCell(6).setCellValue("Tatap Realisasi");
				rowhead.createCell(7).setCellValue("Kode Prodi");
				rowhead.createCell(8).setCellValue("SKS Ajar");

				XSSFWorkbook workbookUpload;
				try {
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

							String smt = Common.getSheetContentAsString(sheetUpload, 0, i);

							String nidn = Common.getSheetContentAsString(sheetUpload, 1, i);

							String mk = Common.getSheetContentAsString(sheetUpload, 3, i);

							String kelas = Common.getSheetContentAsString(sheetUpload, 4, i);

							Date mulai = Common.getSheetContentAsDateDatabase(sheetUpload, 5, i);

							Date sampai = Common.getSheetContentAsDateDatabase(sheetUpload, 6, i);

							Matakuliah matakuliah = (Matakuliah) (mk == null || mk.trim().isEmpty() ? null
									: session.createCriteria(Matakuliah.class)
											.add(Restrictions.ilike("kode", mk.trim(), MatchMode.EXACT))
											.add(Restrictions.eq("jurusan", jurusan)).setMaxResults(1).uniqueResult());

							Dosen dosen = (Dosen) (nidn == null || nidn.trim().isEmpty() ? null
									: session.createCriteria(Dosen.class)
											.add(Restrictions.ilike("nidn", nidn.trim(), MatchMode.EXACT))
											.setMaxResults(1).uniqueResult());

							KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) (matakuliah == null
									? null
									: session.createCriteria(KurikulumPunyaMatakuliah.class)
											.createAlias("kurikulum", "kurikulum")
											.createAlias("kurikulum.program", "program")
											.add(Restrictions.eq("kurikulum.jurusan", jurusan))
											.add(Restrictions.eq("program.nama", program))
											.add(Restrictions.eq("matakuliah", matakuliah))
											.addOrder(Order.desc("kurikulum.tahun")).addOrder(Order.desc("id"))
											.setMaxResults(1).uniqueResult());

							System.out.println("dosen => " + dosen);
							System.out.println("matakuliah => " + matakuliah);
							System.out.println("smt => " + smt);
							System.out.println("kurikulumPunyaMatakuliah => " + kurikulumPunyaMatakuliah);

							if (kurikulumPunyaMatakuliah != null && matakuliah != null && smt != null
									&& !smt.trim().isEmpty() && dosen != null) {

								Integer tahun = Integer.parseInt(smt.substring(0, 4));

								String tahunAkademik = tahun + "/" + (tahun + 1);

								label.setValue("Upload data " + matakuliah + " - " + smt + " - " + dosen + " ("
										+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

								Perkuliahan perkuliahan = UploadAjarDosen.checkPerkuliahan(session, dosen, kelas,
										tahunAkademik, tbmuser, mulai, sampai, kurikulumPunyaMatakuliah);

								XSSFRow row = sheet.createRow(rowIndex);
								XSSFCell cell = row.createCell(0);
								cell.setCellValue(smt);

								cell = row.createCell(1);
								cell.setCellValue(dosen.getNidn());

								cell = row.createCell(2);
								cell.setCellValue(dosen.getNama());

								cell = row.createCell(3);
								cell.setCellValue(perkuliahan.getMatakuliah() == null ? ""
										: perkuliahan.getMatakuliah().getKode());

								cell = row.createCell(4);
								cell.setCellValue(perkuliahan.getKelas());

								cell = row.createCell(5);
								cell.setCellValue(perkuliahan.getJumlahMaksimalPertemuan());

								int jumlah = ((Number) session.createCriteria(Pertemuan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("perkuliahan", perkuliahan))
										.setProjection(Projections.rowCount()).uniqueResult()).intValue();

								cell = row.createCell(6);
								cell.setCellValue(jumlah);

								cell = row.createCell(7);
								cell.setCellValue(jurusan.getKodeEpsbed());

								cell = row.createCell(8);
								cell.setCellValue(perkuliahan.getMatakuliah() == null ? null
										: perkuliahan.getMatakuliah().getSks());

								rowIndex++;
								report.sukses(i, nidn + "/" + mk + "/" + kelas, "Ajar Dosen berhasil diproses");

							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							report.gagal(i, "baris-" + i, e, "Periksa data NIDN/MK pada baris ini");
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

				} catch (Exception e1) {
					// FIX "gagal diam-diam": sebelumnya exception di sini (mis. gagal parse file
					// upload/matakuliah/dosen) hanya dicatat ke log admin, lalu label.setValue("")
					// tetap dipanggil di bawah (=SUKSES palsu) menutupi kegagalan. Sekarang error
					// ditampilkan ke label progres dan sukses palsu di bawah dilewati (return).
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/feeder/integrator/helper/UploadAjarDosen.java:409");
					Common.tampilErrorJikaAdmin(e1);
					label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
							"pengiriman data Ajar Dosen ke Neo Feeder", null, e1,
							new String[] {
									"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
									"Pastikan format file Excel yang diupload dan data Matakuliah/Dosen terkait sudah benar.",
									"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
					return;
				}

				System.out.println("Your excel file has been generated! " );

				HibernateUtil.closeSession();
				try { downloadPath.setValue(report.simpanLaporan().getAbsolutePath()); } catch (Exception rex) { ais.common.ErrorAuditUtil.record(rex, "UploadAjarDosen-report"); }
				label.setValue("");

							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

		final Timer timerReport = new Timer(500);
		timerReport.setParent(UploadAjarDosen.this);
		timerReport.setRepeats(true);
		timerReport.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (label.getValue().isEmpty()) {
					timerReport.detach();
					if (!downloadPath.getValue().isEmpty()) {
						Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain");
					}
					MyMessageboxConfig.show(report.getRingkasan(), "Laporan Upload Ajar Dosen", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				} else if (label.getValue().startsWith("Error:")) {
					timerReport.detach();
				}
			}
		});
		timerReport.start();

	}

	public static Perkuliahan checkPerkuliahan(Session session, Dosen dosen, String kelas, String tahunAkademik,
			Tbmuser tbmuser, Date mulai, Date selesai, KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah) {
		Jurusan jurusan = kurikulumPunyaMatakuliah.getKurikulum().getJurusan();
		Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();
		String program = kurikulumPunyaMatakuliah.getKurikulum().getProgram()==null ? "Reguler" : kurikulumPunyaMatakuliah.getKurikulum().getProgram().getNama();

		Perkuliahan perkuliahan = (Perkuliahan) session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("id"))
				.add(Restrictions.ilike("kelas", kelas, MatchMode.EXACT))
				.add(Restrictions.eq("semester", kurikulumPunyaMatakuliah.getSemester()))
				.add(Restrictions.eq("tahunAjaran", tahunAkademik)).add(Restrictions.eq("matakuliah", matakuliah))
				.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.eq("program", program))
				.add(Restrictions.isNull("perkuliahan_paralel")).setMaxResults(1).uniqueResult();

		if (perkuliahan == null) {
			perkuliahan = new Perkuliahan();
			perkuliahan.setOleh(tbmuser.getUserNama());
			perkuliahan.setJurusan(jurusan);
			perkuliahan.setProgram(program);
			perkuliahan.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
			perkuliahan.setSemester(kurikulumPunyaMatakuliah.getSemester());
			perkuliahan.setKurikulum(kurikulumPunyaMatakuliah.getKurikulum());
			perkuliahan.setMerupakan_tanpa_jadwal_perkuliahan(true);
			perkuliahan.setMerupakan_tanpa_dosen(true);
			perkuliahan.setMerupakan_tanpa_ruangan(true);
			perkuliahan.setKelas(kelas.trim());

			String olehId = Common.generateOlehId(tbmuser);
			perkuliahan.setOlehId(olehId);
			perkuliahan.setMatakuliah(matakuliah);
			perkuliahan.setTahunAjaran(tahunAkademik);
			perkuliahan.setPerkuliahanDimulai(mulai);
			perkuliahan.setPerkuliahanSampai(selesai);
			session.getTransaction().begin();
			try {
				session.save(perkuliahan);
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
						+ "src/ais/action/master/feeder/integrator/helper/UploadAjarDosen.java");
				}
				throw eSimpan;
			}
		}

		perkuliahan.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
		Collection<Dosen> dosens = perkuliahan.populateDosen().values();
		boolean ada = dosens.contains(dosen);
		System.out.println("ada => " + ada + ", dosen => " + dosen + ", dosens yg ada => " + dosens);
		if (ada) {
			return perkuliahan;
		}
		perkuliahan.setJumlahDosen(dosens.size() + 1);

		if (perkuliahan.getDosen1() == null) {
			perkuliahan.setDosen1(dosen);
		} else if (perkuliahan.getDosen2() == null) {
			perkuliahan.setDosen2(dosen);
		} else if (perkuliahan.getDosen3() == null) {
			perkuliahan.setDosen3(dosen);
		} else if (perkuliahan.getDosen4() == null) {
			perkuliahan.setDosen4(dosen);
		} else if (perkuliahan.getDosen5() == null) {
			perkuliahan.setDosen5(dosen);
		} else if (perkuliahan.getDosen6() == null) {
			perkuliahan.setDosen6(dosen);
		} else if (perkuliahan.getDosen7() == null) {
			perkuliahan.setDosen7(dosen);
		} else if (perkuliahan.getDosen8() == null) {
			perkuliahan.setDosen8(dosen);
		} else if (perkuliahan.getDosen9() == null) {
			perkuliahan.setDosen9(dosen);
		} else if (perkuliahan.getDosen10() == null) {
			perkuliahan.setDosen10(dosen);
		}

		session.getTransaction().begin();
		try {
			Common.refreshSaveOrUpdate(session, perkuliahan);
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
					+ "src/ais/action/master/feeder/integrator/helper/UploadAjarDosen.java");
			}
			throw new RuntimeException(eSimpan);
		}

		return perkuliahan;
	}
}
