package ais.action.master.feeder.integrator.impor;

import java.io.File;
import java.awt.Color;
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
import ais.action.master.feeder.integrator.ekspor.SaringanFeeder;
import ais.common.UploadReportHelper;
import ais.common.newui.pekerjaan.PekerjaanRegistry;
import ais.database.model.Tbmuser;

/**
 * Pembaca berkas unggahan KRS untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan pembacanya DIPINDAHKAN dari {@code UploadKrs} — bukan disalin — supaya
 * aturan pencocokan baris ke entitas dan penyimpanannya hanya ada di satu
 * tempat. Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah salurannya: berkas datang sebagai
 * parameter alih-alih dari widget unggah, kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label, dan kegagalan
 * dilempar alih-alih dituliskan ke label sebagai teks berawalan "Error:".</p>
 *
 * <p><b>Kelas ini menulis ke basis data.</b> Hak aksesnya diperiksa pemanggil;
 * lihat penjaga aksi {@code import_*} yang menuntut izin buat sekaligus ubah.</p>
 */
public final class ImporKrsFeeder {

    private ImporKrsFeeder() { }

    /**
     * Baca {@code fileUpload} lalu simpan isinya.
     *
     * @param fileUpload berkas .xlsx yang diunggah operator
     * @param tujuan     berkas .xlsx ringkasan hasil yang ditulis ulang di sini
     * @param saring     saringan layar; hanya sebagian panel memakainya
     * @param tbmuser    pengguna yang bertanggung jawab atas perubahan
     * @param progres    kanal pelaporan kemajuan; boleh null
     */
    public static HasilImpor proses(File fileUpload, File tujuan, SaringanFeeder saring,
            Tbmuser tbmuser, PekerjaanRegistry.Progres progres) throws Exception {
        if (fileUpload == null || !fileUpload.exists()) {
            throw new IllegalArgumentException("Berkas unggahan tidak ada.");
        }
        if (saring == null) saring = new SaringanFeeder();
        saring.rapikan();
        if (progres == null) {
            progres = new PekerjaanRegistry.Progres() {
                public void lapor(int persen, String pesan) { }
            };
        }
        final String filename = tujuan.getAbsolutePath();
        final UploadReportHelper report = new UploadReportHelper("Upload KRS");
        final HasilImpor hasil = new HasilImpor();
        hasil.berkasHasil = tujuan;
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
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/ImporKrsFeeder.java:417");
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

						progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Upload data " + nim + " - " + kodeMatakuliah + " ("
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

								if (ais.action.master.feeder.integrator.ekspor.EksporKrsFeeder.createData(
										session, sheet, rowIndex, detailperkuliahan, null,
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
				} finally {
					// Tutup session khusus baris ini + bersihkan ThreadLocal sisa helper Excel.
					HibernateUtil.closeSessionQuietly(session);
					HibernateUtil.closeSession();
				}
			}

			Common.setStyled(sheet);
			hasil.baris = rowIndex + 1;

			try {
				FileOutputStream fileOut = new FileOutputStream(filename);
				workbook.write(fileOut);
				fileOut.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}


        hasil.laporan = report.simpanLaporan();
        hasil.ringkasan = report.getRingkasan();
        progres.lapor(100, "Selesai memproses " + hasil.baris + " baris.");
        return hasil;
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/ImporKrsFeeder.java:253");
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
					try {
						session.saveOrUpdate(kurikulum);
						session.getTransaction().commit();
					} catch (RuntimeException eSimpan) {
						try {
							session.getTransaction().rollback();
						} catch (Exception eRoll) {
							ais.common.ErrorAuditUtil.record(eRoll, "rollback-gagal-upload "
								+ "src/ais/action/master/feeder/integrator/helper/ImporKrsFeeder.java");
						}
						throw eSimpan;
					}
				}

				kurikulumPunyaMatakuliah = new KurikulumPunyaMatakuliah();
				kurikulumPunyaMatakuliah.setMatakuliah(matakuliah);
				kurikulumPunyaMatakuliah.setSemester(semester);
				kurikulumPunyaMatakuliah.setKurikulum(kurikulum);
				session.getTransaction().begin();
				try {
					session.saveOrUpdate(kurikulumPunyaMatakuliah);
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
							+ "src/ais/action/master/feeder/integrator/helper/ImporKrsFeeder.java");
					}
					throw eSimpan;
				}
			}

			if (detailperkuliahan == null) {
				detailperkuliahan = new Detailperkuliahan(tbmuser, ais.action.master.feeder.integrator.helper.UploadKrs.class);
			}
			detailperkuliahan.setPersetujuan(Detailperkuliahan.DISETUJUI);
			detailperkuliahan.setMahasiswa(mahasiswa);
			Perkuliahan perkuliahan = ImporKelasFeeder.checkPerkuliahan(session, kelas, tahunAkademik, tbmuser, null, null,
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
					+ "src/ais/action/master/feeder/integrator/helper/ImporKrsFeeder.java");
			}
			throw eSimpan;
		}

		System.out.println("TA detailperkuliahan = " + detailperkuliahan.getTahunAkademik() + ", semester = "
				+ detailperkuliahan.getSemester() + ", detailperkuliahan.id = " + detailperkuliahan.getId());

		return detailperkuliahan;
	}
}
