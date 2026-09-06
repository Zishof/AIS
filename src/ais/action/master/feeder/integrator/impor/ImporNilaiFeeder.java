package ais.action.master.feeder.integrator.impor;

import java.io.File;
import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import ais.common.ConstantValues;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.action.master.feeder.integrator.ekspor.SaringanFeeder;
import ais.common.UploadReportHelper;
import ais.common.newui.pekerjaan.PekerjaanRegistry;
import ais.database.model.Tbmuser;

/**
 * Pembaca berkas unggahan Nilai untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan pembacanya DIPINDAHKAN dari {@code UploadNilai} — bukan disalin — supaya
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
public final class ImporNilaiFeeder {

    private ImporNilaiFeeder() { }

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
        final UploadReportHelper report = new UploadReportHelper("Upload Nilai");
        final HasilImpor hasil = new HasilImpor();
        hasil.berkasHasil = tujuan;
		XSSFWorkbook workbook = new XSSFWorkbook();

		XSSFSheet sheet = workbook.createSheet("NILAI");
		sheet.setDefaultColumnWidth(20);

		XSSFRow rowhead = sheet.createRow((short) 0);

		rowhead.createCell(0).setCellValue("NIM");
		rowhead.createCell(1).setCellValue("Nama Mahasiswa");
		rowhead.createCell(2).setCellValue("Kode MK");
		rowhead.createCell(3).setCellValue("Mata Kuliah");
		rowhead.createCell(4).setCellValue("Semester");
		rowhead.createCell(5).setCellValue("Kelas");
		rowhead.createCell(6).setCellValue("Nilai Huruf");
		rowhead.createCell(7).setCellValue("Nilai Indeks");
		rowhead.createCell(8).setCellValue("Nilai Angka");
		rowhead.createCell(9).setCellValue("Kode Prodi");
		rowhead.createCell(10).setCellValue("Hapus?");
		rowhead.createCell(11).setCellValue("Dosen I");
		rowhead.createCell(12).setCellValue("Dosen II");
		rowhead.createCell(13).setCellValue("Dosen III");
		rowhead.createCell(14).setCellValue("Dosen IV");
		rowhead.createCell(15).setCellValue("Dosen V");
		rowhead.createCell(16).setCellValue("Dosen VI");
		rowhead.createCell(17).setCellValue("Dosen VII");
		rowhead.createCell(18).setCellValue("Dosen VIII");
		rowhead.createCell(19).setCellValue("Dosen IX");
		rowhead.createCell(20).setCellValue("Dosen X");

		XSSFWorkbook workbookUpload;
		{
			workbookUpload = new XSSFWorkbook(fileUpload.getAbsolutePath());

			XSSFSheet sheetUpload = workbookUpload.getSheetAt(0);
			int size = sheetUpload.getLastRowNum() + 1;

			Map<Long, Integer> map = new HashMap<Long, Integer>();
			for (int col = 21; col <= 100; col++) {
				try {
					String kelas = Common.getSheetContentAsString(sheetUpload, col, 0);
					if (kelas != null && !kelas.trim().isEmpty()) {
						String[] s = kelas.split("-");
						map.put(Long.parseLong(s[0]), col);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/ImporNilaiFeeder.java:395");
					// TODO: handle exception
				}
			}

			System.out.println("map -> " + map);

			XSSFCellStyle notLocked = workbook.createCellStyle();
			notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
			notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

			int rowIndex = 1;
			for (int i = 1; i < size; i++) {
				/*
				 * WAJIB openSession(), BUKAN currentNativeSession(). Common.getSheetContentAsObject()
				 * di dalam pemrosesan baris menutup native session ThreadLocal
				 * (HibernateUtil.closeSession()), sehingga session hasil currentNativeSession()
				 * sudah TERTUTUP saat dipakai lagi (createCriteria/begin) -> "Session is closed!"
				 * di SETIAP baris.
				 */
				Session session = HibernateUtil.openSession();
				try {

					if (Common.getSheetContentAsString(sheetUpload, 0, i) == null) {
						continue;
					}
					Boolean hapus = Common.getSheetContentAsBoolean(sheetUpload, 10, i);
					String smt = Common.getSheetContentAsString(sheetUpload, 4, i);

					Dosen dosen1 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 11, i, Dosen.class);
					Dosen dosen2 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 12, i, Dosen.class);
					Dosen dosen3 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 13, i, Dosen.class);
					Dosen dosen4 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 14, i, Dosen.class);
					Dosen dosen5 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 15, i, Dosen.class);
					Dosen dosen6 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 16, i, Dosen.class);
					Dosen dosen7 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 17, i, Dosen.class);
					Dosen dosen8 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 18, i, Dosen.class);
					Dosen dosen9 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 19, i, Dosen.class);
					Dosen dosen10 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 20, i, Dosen.class);

					/*
					 * Reload ke session baris ini agar entitas managed (bukan detached dari
					 * session lain milik helper Excel yang sudah tertutup) sebelum dipakai
					 * sebagai relasi yang disimpan.
					 */
					dosen1 = dosen1 == null ? null : (Dosen) session.get(Dosen.class, dosen1.getId());
					dosen2 = dosen2 == null ? null : (Dosen) session.get(Dosen.class, dosen2.getId());
					dosen3 = dosen3 == null ? null : (Dosen) session.get(Dosen.class, dosen3.getId());
					dosen4 = dosen4 == null ? null : (Dosen) session.get(Dosen.class, dosen4.getId());
					dosen5 = dosen5 == null ? null : (Dosen) session.get(Dosen.class, dosen5.getId());
					dosen6 = dosen6 == null ? null : (Dosen) session.get(Dosen.class, dosen6.getId());
					dosen7 = dosen7 == null ? null : (Dosen) session.get(Dosen.class, dosen7.getId());
					dosen8 = dosen8 == null ? null : (Dosen) session.get(Dosen.class, dosen8.getId());
					dosen9 = dosen9 == null ? null : (Dosen) session.get(Dosen.class, dosen9.getId());
					dosen10 = dosen10 == null ? null : (Dosen) session.get(Dosen.class, dosen10.getId());

					String nim = Common.getSheetContentAsString(sheetUpload, 0, i);

					Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(
							(session.createCriteria(Mahasiswa.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"),
											Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("nim", nim.trim())).setMaxResults(1)),
							Mahasiswa.class);

					if (mahasiswa != null) {

						System.out.println("mahasiswa " + mahasiswa);

						String kodeMatakuliah = Common.getSheetContentAsString(sheetUpload, 2, i);
						String kelas = Common.getSheetContentAsString(sheetUpload, 5, i);
						Integer semester = null;
						String tahunAkademik = null;

						Integer statusSemesterPendek = null;
						try {
							statusSemesterPendek = Integer.parseInt(smt.trim().substring(4)) == 3
									? Perkuliahan.SEMESTER_PENDEK
									: null;
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/ImporNilaiFeeder.java:451");
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

						System.out.println("semester " + semester + " tahunAkademik " + tahunAkademik);

						if (semester == null || tahunAkademik == null) {
							continue;
						}

						Double nilai = Common.getSheetContentAsDouble(sheetUpload, 8, i);

						String hurufdata = Common.getSheetContentAsString(sheetUpload, 6, i);

						if ((nilai == null || nilai < 0.01) && hurufdata != null
								&& !hurufdata.trim().isEmpty()) {

							// Cari aturan konversi huruf->angka lewat indeks terpusat (prioritas per Jurusan ->
							// Fakultas -> global murni), sama seperti ConstantValues.lulusDariNilaiHuruf() dkk --
							// menggantikan 3 loop tangan yang sebelumnya disalin-tempel di sini (tier terakhirnya
							// sempat tanpa syarat cakupan sama sekali, lihat r86085).
							NilaiHuruf nilaiHuruf = ConstantValues.nilaiHurufTerkait(hurufdata.trim(), mahasiswa);

							if (nilaiHuruf != null) {
								nilai = (nilaiHuruf.getMulai() + nilaiHuruf.getSampai()) / 2.0;
							}

							System.out.println("nilaiHuruf " + nilaiHuruf + ", huruf = " + hurufdata);

						}

						progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Upload data " + nim + " - " + kodeMatakuliah + " - " + nilai + " ("
								+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

						Map<Long, Double> mapNilai = new HashMap<Long, Double>();
						for (Long statusId : map.keySet()) {
							try {
								Integer col = map.get(statusId);
								Double subnilai = Common.getSheetContentAsDouble(sheetUpload, col, i);
								mapNilai.put(statusId, subnilai);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/ImporNilaiFeeder.java:541");
								// TODO: handle exception
							}

						}

						System.out.println("mahasiswa " + mahasiswa + " tahunAkademik = " + tahunAkademik
								+ ", semester = " + semester + ", nilai " + nilai + ", huruf = " + hurufdata
								+ ", kodeMatakuliah " + kodeMatakuliah + ", mapNilai " + mapNilai);

						Detailperkuliahan detailperkuliahan = createDetailperkulihana(session, mahasiswa,
								semester, nim, kodeMatakuliah, kelas, tahunAkademik, nilai, tbmuser,
								statusSemesterPendek, dosen1, dosen2, dosen3, dosen4, dosen5, dosen6, dosen7,
								dosen8, dosen9, dosen10, mapNilai);

						if (detailperkuliahan != null) {
							detailperkuliahan.setTahunAkademik(tahunAkademik);

							Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() == null
									? (detailperkuliahan.getPerkuliahan() == null ? null
											: detailperkuliahan.getPerkuliahan().getMatakuliah())
									: detailperkuliahan.getMatakuliahKonversi();
							if (matakuliah != null) {

								ais.action.master.feeder.integrator.ekspor.EksporNilaiFeeder.createData(
										session, sheet, rowIndex, detailperkuliahan, null,
										notLocked, hapus, null);

								rowIndex++;
								report.sukses(i, nim + "/" + kodeMatakuliah, "Nilai " + nilai);
							}
						}

					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					report.gagal(i, "baris-" + i, e, "Periksa data NIM/MK pada baris ini");
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

	private static Detailperkuliahan createDetailperkulihana(Session session, Mahasiswa mahasiswa, Integer semester,
			String nim, String kodeMatakuliah, String kelas, String tahunAkademik, Double nilai, Tbmuser tbmuser,
			Integer statusSemesterPendek, Dosen dosen1, Dosen dosen2, Dosen dosen3, Dosen dosen4, Dosen dosen5,
			Dosen dosen6, Dosen dosen7, Dosen dosen8, Dosen dosen9, Dosen dosen10, Map<Long, Double> mapNilai) {

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
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

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

					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
		}

		if ((detailperkuliahan == null && kelas != null && !kelas.trim().isEmpty())
				|| (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null && kelas != null
						&& !kelas.trim().isEmpty() && detailperkuliahan.getPerkuliahan().getKelas() != null
						&& !detailperkuliahan.getPerkuliahan().getKelas().trim().equalsIgnoreCase(kelas.trim()))) {
			detailperkuliahan = ImporKrsFeeder.createDetailperkulihana(session, mahasiswa, semester, nim, kodeMatakuliah,
					kelas, tahunAkademik, tbmuser, statusSemesterPendek);
		}

		if (detailperkuliahan == null && nilai > 0.01) {
			String kode = kodeMatakuliah.trim();
			Matakuliah matakuliah = (Matakuliah) session.createCriteria(Matakuliah.class)
					.add(Restrictions.ilike("kode", kode)).add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
					.setMaxResults(1).uniqueResult();

			if (matakuliah == null && !kode.trim().isEmpty() && Common.isNumber(kode)) {
				try {
					matakuliah = (Matakuliah) session.createCriteria(Matakuliah.class)
							.add(Restrictions.idEq(Long.parseLong(kode))).setMaxResults(1).uniqueResult();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/ImporNilaiFeeder.java:256");
				}
			}

			if (matakuliah == null) {
				return detailperkuliahan;
			}

			if (detailperkuliahan == null) {
				detailperkuliahan = new Detailperkuliahan(tbmuser, ais.action.master.feeder.integrator.helper.UploadNilai.class);
			}
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
		detailperkuliahan.setOleh(tbmuser.getUserNama());

		List<FormatNilai> formatNilais = Common.getFormatNilais(session, detailperkuliahan.getPerkuliahan());
		for (FormatNilai formatNilai : formatNilais) {
			Double nn = formatNilai.getStatusPertemuan() == null || formatNilai.getStatusPertemuan().getId() == null
					? null
					: mapNilai.get(formatNilai.getStatusPertemuan().getId());
			if (nn == null) {
				nn = nilai;
			}
			detailperkuliahan.populateDetailNilai(formatNilai, null, nn, true, tbmuser);
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
					+ "src/ais/action/master/feeder/integrator/helper/ImporNilaiFeeder.java");
			}
			throw eSimpan;
		}

		System.out.println("TA detailperkuliahan = " + detailperkuliahan.getTahunAkademik() + ", semester = "
				+ detailperkuliahan.getSemester() + ", detailperkuliahan.id = " + detailperkuliahan.getId());

		return detailperkuliahan;
	}
}
