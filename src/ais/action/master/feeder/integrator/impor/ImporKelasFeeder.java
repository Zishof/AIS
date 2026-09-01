package ais.action.master.feeder.integrator.impor;

import java.io.File;
import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Date;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Ruang;
import ais.action.master.feeder.integrator.ekspor.SaringanFeeder;
import ais.common.UploadReportHelper;
import ais.common.newui.pekerjaan.PekerjaanRegistry;
import ais.database.model.Tbmuser;

/**
 * Pembaca berkas unggahan Kelas untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan pembacanya DIPINDAHKAN dari {@code UploadKelas} — bukan disalin — supaya
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
public final class ImporKelasFeeder {

    private ImporKelasFeeder() { }

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
        final UploadReportHelper report = new UploadReportHelper("Upload Kelas");
        final HasilImpor hasil = new HasilImpor();
        hasil.berkasHasil = tujuan;
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFCellStyle notLocked = workbook.createCellStyle();
		notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
		XSSFSheet sheet = workbook.createSheet("KELAS");
		sheet.setDefaultColumnWidth(20);

		XSSFRow rowhead = sheet.createRow((short) 0);

		rowhead.createCell(0).setCellValue("semester");
		rowhead.createCell(1).setCellValue("kode matakuliah");
		rowhead.createCell(2).setCellValue("Nama Matakuliah");
		rowhead.createCell(3).setCellValue("Kelas");
		rowhead.createCell(4).setCellValue("Bahasan");
		rowhead.createCell(5).setCellValue("Tanggal Mulai Efektif");
		rowhead.createCell(6).setCellValue("Tanggal Akhir Efektif");
		rowhead.createCell(7).setCellValue("Kode Prodi");

		rowhead.createCell(8).setCellValue("Mulai");
		rowhead.createCell(9).setCellValue("Sampai");
		rowhead.createCell(10).setCellValue("Hari");
		rowhead.createCell(11).setCellValue("Ruang");
		rowhead.createCell(12).setCellValue("Dosen I");
		rowhead.createCell(13).setCellValue("Dosen II");
		rowhead.createCell(14).setCellValue("Dosen III");
		rowhead.createCell(15).setCellValue("Dosen IV");
		rowhead.createCell(16).setCellValue("Dosen V");
		rowhead.createCell(17).setCellValue("Dosen VI");
		rowhead.createCell(18).setCellValue("Dosen VII");
		rowhead.createCell(19).setCellValue("Dosen VIII");
		rowhead.createCell(20).setCellValue("Dosen IX");
		rowhead.createCell(21).setCellValue("Dosen X");
		rowhead.createCell(22).setCellValue("Program");

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
				 * sudah TERTUTUP saat dipakai lagi (createCriteria/begin) -> "Session is closed!"
				 * di SETIAP baris.
				 */
				Session session = HibernateUtil.openSession();
				try {

					if (Common.getSheetContentAsString(sheetUpload, 0, i) == null) {
						continue;
					}

					String smt = Common.getSheetContentAsString(sheetUpload, 0, i);

					String kode = Common.getSheetContentAsString(sheetUpload, 1, i);

					String nama = Common.getSheetContentAsString(sheetUpload, 2, i);

					String kelas = Common.getSheetContentAsString(sheetUpload, 3, i);

					String bahasan = Common.getSheetContentAsString(sheetUpload, 4, i);

					Date mulai = Common.getSheetContentAsDateDatabase(sheetUpload, 5, i);

					Date sampai = Common.getSheetContentAsDateDatabase(sheetUpload, 6, i);

					String kodeP = Common.getSheetContentAsString(sheetUpload, 7, i);
					Jurusan jurusan = kodeP == null ? null
							: (Jurusan) ConstantValues.simpleObject(session.createCriteria(Jurusan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.or(Restrictions.eq("kode", kodeP.trim()),
											Restrictions.eq("kodeEpsbed", kodeP.trim())))
									.setMaxResults(1), Jurusan.class);

					if (jurusan == null) {
						XSSFRow row = sheet.createRow(rowIndex);
						XSSFCell cell = row.createCell(0);
						cell.setCellValue(smt);

						cell = row.createCell(1);
						cell.setCellValue(kodeP);

						cell = row.createCell(2);
						cell.setCellValue(kodeP);

						cell = row.createCell(3);
						cell.setCellValue("Kode Prodi tidak ditemukan");
						rowIndex++;
						report.gagal(i, smt + "/" + kode, "Kode Prodi tidak ditemukan: " + kodeP, "Pastikan kolom Kode Prodi sesuai data prodi di sistem");
					}

					else {

						String waktuMulai = Common.getSheetContentAsString(sheetUpload, 8, i);
						String waktuSelesai = Common.getSheetContentAsString(sheetUpload, 9, i);
						String hari = Common.getSheetContentAsString(sheetUpload, 10, i);
						Ruang ruang = (Ruang) Common.getSheetContentAsObject(sheetUpload, 11, i, Ruang.class);
						Dosen dosen1 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 12, i, Dosen.class);
						Dosen dosen2 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 13, i, Dosen.class);
						Dosen dosen3 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 14, i, Dosen.class);
						Dosen dosen4 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 15, i, Dosen.class);
						Dosen dosen5 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 16, i, Dosen.class);
						Dosen dosen6 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 17, i, Dosen.class);
						Dosen dosen7 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 18, i, Dosen.class);
						Dosen dosen8 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 19, i, Dosen.class);
						Dosen dosen9 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 20, i, Dosen.class);
						Dosen dosen10 = (Dosen) Common.getSheetContentAsObject(sheetUpload, 21, i, Dosen.class);

						/*
						 * Reload ke session baris ini agar entitas managed (bukan detached dari
						 * session lain milik helper Excel yang sudah tertutup) sebelum dipakai
						 * sebagai relasi Perkuliahan yang disimpan.
						 */
						ruang = ruang == null ? null : (Ruang) session.get(Ruang.class, ruang.getId());
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

						String program = Common.getSheetContentAsString(sheetUpload, 22, i);
						if (program == null || program.trim().isEmpty()) {
							program = "Reguler";
						}

						Matakuliah matakuliah = (Matakuliah) (kode == null || kode.trim().isEmpty() ? null
								: session.createCriteria(Matakuliah.class)
										.add(Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT))
										.add(Restrictions.eq("jurusan", jurusan)).setMaxResults(1)
										.uniqueResult());
						if (matakuliah == null && !kode.trim().isEmpty() && Common.isNumber(kode)) {
							try {
								matakuliah = (Matakuliah) session.createCriteria(Matakuliah.class)
										.add(Restrictions.idEq(Long.parseLong(kode))).setMaxResults(1)
										.uniqueResult();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/ImporKelasFeeder.java:341");
							}
						}

						if (matakuliah == null && !kode.trim().isEmpty()) {
							try {
								matakuliah = (Matakuliah) session.createCriteria(Matakuliah.class)
										.add(Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT))
										.setMaxResults(1).uniqueResult();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/ImporKelasFeeder.java:351");
							}
						}

						if (matakuliah == null && !nama.trim().isEmpty()) {
							try {
								matakuliah = (Matakuliah) session.createCriteria(Matakuliah.class)
										.add(Restrictions.ilike("nama", nama.trim(), MatchMode.ANYWHERE))
										.add(Restrictions.eq("jurusan", jurusan)).setMaxResults(1)
										.uniqueResult();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/ImporKelasFeeder.java:362");
							}
						}

						KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) (matakuliah == null
								? null
								: session.createCriteria(KurikulumPunyaMatakuliah.class)
										.createAlias("matakuliah", "matakuliah")
										.createAlias("kurikulum", "kurikulum", Criteria.LEFT_JOIN)
										.createAlias("kurikulum.program", "program", Criteria.LEFT_JOIN)
										.add(Restrictions.eq("kurikulum.jurusan", jurusan))
										.add(Restrictions.or(Restrictions.isNull("kurikulum.program"),
												Restrictions.eq("program.nama", program)))
										.add(Restrictions.eq("matakuliah.kode", kode.trim()))
										.addOrder(Order.desc("kurikulum.tahun")).addOrder(Order.desc("id"))
										.setMaxResults(1).uniqueResult());

						if (kurikulumPunyaMatakuliah == null) {
							XSSFRow row = sheet.createRow(rowIndex);
							XSSFCell cell = row.createCell(0);
							cell.setCellValue(smt);

							cell = row.createCell(1);
							cell.setCellValue(matakuliah == null ? "" : matakuliah.getKode());

							cell = row.createCell(2);
							cell.setCellValue(matakuliah == null ? "" : matakuliah.getKode());

							cell = row.createCell(3);
							cell.setCellValue("Kurikulum tidak ditemukan");
							rowIndex++;
							report.gagal(i, smt + "/" + kode, "Kurikulum tidak ditemukan untuk MK: " + kode, "Pastikan kurikulum untuk matakuliah ini sudah ada di sistem");
						}

						else if (kurikulumPunyaMatakuliah != null && matakuliah != null && smt != null
								&& !smt.trim().isEmpty() && kelas != null && !kelas.trim().isEmpty()) {

							Integer tahun = Integer.parseInt(smt.trim().substring(0, 4));

							Integer statusSemesterPendek = null;
							try {
								statusSemesterPendek = Integer.parseInt(smt.trim().substring(4)) == 3
										? Perkuliahan.SEMESTER_PENDEK
										: null;
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/ImporKelasFeeder.java:405");
								// TODO: handle exception
							}

							String tahunAkademik = tahun + "/" + (tahun + 1);

							progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Upload data " + matakuliah + " - " + smt + " - " + kelas + " ("
									+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

							Perkuliahan perkuliahan = ImporKelasFeeder.checkPerkuliahan(session, kelas,
									tahunAkademik, tbmuser, bahasan, mulai, sampai, kurikulumPunyaMatakuliah,
									program, statusSemesterPendek, waktuMulai, waktuSelesai, hari, ruang,
									dosen1, dosen2, dosen3, dosen4, dosen5, dosen6, dosen7, dosen8, dosen9,
									dosen10, true);

							XSSFRow row = sheet.createRow(rowIndex);
							XSSFCell cell = row.createCell(0);
							cell.setCellValue(smt);

							cell = row.createCell(1);
							cell.setCellValue(perkuliahan.getMatakuliah() == null ? ""
									: perkuliahan.getMatakuliah().getKode());

							cell = row.createCell(2);
							cell.setCellValue(perkuliahan.getMatakuliah() == null ? ""
									: perkuliahan.getMatakuliah().getNama());

							cell = row.createCell(3);
							cell.setCellValue(perkuliahan.getKelas() == null ? "" : perkuliahan.getKelas());

							cell = row.createCell(4);
							cell.setCellValue(perkuliahan.getDeskripsiPembelajaran() == null ? ""
									: perkuliahan.getDeskripsiPembelajaran());

							cell = row.createCell(5);
							cell.setCellValue(perkuliahan.getPerkuliahanDimulai() == null ? ""
									: Common.databaseDateFormat.get().format(perkuliahan.getPerkuliahanDimulai()));
							cell = row.createCell(6);
							cell.setCellValue(perkuliahan.getPerkuliahanSampai() == null ? ""
									: Common.databaseDateFormat.get().format(perkuliahan.getPerkuliahanSampai()));

							row.createCell(7).setCellValue(perkuliahan.getJurusan().getKodeEpsbed());

							cell = row.createCell(8);

							cell.setCellValue(perkuliahan.getWaktuMulai());

							cell = row.createCell(9);

							cell.setCellValue(perkuliahan.getWaktuSelesai());

							cell = row.createCell(10);

							cell.setCellValue(perkuliahan.getHari());

							cell = row.createCell(11);

							cell.setCellValue(
									perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getKode());

							cell = row.createCell(12);

							cell.setCellValue(perkuliahan == null || perkuliahan.getDosen1() == null ? ""
									: perkuliahan.getDosen1().toString());

							cell = row.createCell(13);

							cell.setCellValue(perkuliahan == null || perkuliahan.getDosen2() == null ? ""
									: perkuliahan.getDosen2().toString());

							cell = row.createCell(14);

							cell.setCellValue(perkuliahan == null || perkuliahan.getDosen3() == null ? ""
									: perkuliahan.getDosen3().toString());

							cell = row.createCell(15);

							cell.setCellValue(perkuliahan == null || perkuliahan.getDosen4() == null ? ""
									: perkuliahan.getDosen4().toString());

							cell = row.createCell(16);

							cell.setCellValue(perkuliahan == null || perkuliahan.getDosen5() == null ? ""
									: perkuliahan.getDosen5().toString());

							cell = row.createCell(17);

							cell.setCellValue(perkuliahan == null || perkuliahan.getDosen6() == null ? ""
									: perkuliahan.getDosen6().toString());

							cell = row.createCell(18);

							cell.setCellValue(perkuliahan == null || perkuliahan.getDosen7() == null ? ""
									: perkuliahan.getDosen7().toString());

							cell = row.createCell(19);

							cell.setCellValue(perkuliahan == null || perkuliahan.getDosen8() == null ? ""
									: perkuliahan.getDosen8().toString());

							cell = row.createCell(20);

							cell.setCellValue(perkuliahan == null || perkuliahan.getDosen9() == null ? ""
									: perkuliahan.getDosen9().toString());

							cell = row.createCell(21);

							cell.setCellValue(perkuliahan == null || perkuliahan.getDosen10() == null ? ""
									: perkuliahan.getDosen10().toString());

							cell = row.createCell(22);

							cell.setCellValue(perkuliahan == null ? "" : perkuliahan.getProgram());

							rowIndex++;
							report.sukses(i, smt + "/" + kode + "/" + kelas, "Kelas berhasil diproses");

						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					report.gagal(i, "baris-" + i, e, "Periksa data kelas pada baris ini");
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

	public static Perkuliahan checkPerkuliahan(Session session, String kelas, String tahunAkademik, Tbmuser tbmuser,
			String bahasan, Date mulai, Date selesai, KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah, String program,
			Integer statusSemesterPendek, String waktuMulai, String waktuSelesai, String hari, Ruang ruang,
			Dosen dosen1, Dosen dosen2, Dosen dosen3, Dosen dosen4, Dosen dosen5, Dosen dosen6, Dosen dosen7,
			Dosen dosen8, Dosen dosen9, Dosen dosen10, boolean rinci) {
		Jurusan jurusan = kurikulumPunyaMatakuliah.getKurikulum().getJurusan();
		Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();

		Perkuliahan perkuliahan = (Perkuliahan) session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(statusSemesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
						: Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK))
				.addOrder(Order.desc("id")).add(Restrictions.eq("semester", kurikulumPunyaMatakuliah.getSemester()))
				.add(Restrictions.ilike("kelas", kelas, MatchMode.EXACT))
				.add(Restrictions.eq("tahunAjaran", tahunAkademik)).add(Restrictions.eq("matakuliah", matakuliah))
				.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.eq("program", program))
				.add(Restrictions.isNull("perkuliahan_paralel")).setMaxResults(1).uniqueResult();

		if (rinci) {

			if (perkuliahan == null) {
				perkuliahan = new Perkuliahan();
				perkuliahan.setOleh(tbmuser.getUserNama());
			}

			perkuliahan.setMerupakan_tanpa_jadwal_perkuliahan(waktuMulai == null || waktuMulai.trim().isEmpty());
			perkuliahan.setMerupakan_tanpa_dosen(dosen1 == null);
			perkuliahan.setMerupakan_tanpa_ruangan(ruang == null);
			perkuliahan.setWaktuMulai(waktuMulai);
			perkuliahan.setWaktuSelesai(waktuSelesai);
			perkuliahan.setHari(hari);
			perkuliahan.setRuang(ruang);
			perkuliahan.setDosen1(dosen1);
			perkuliahan.setDosen2(dosen2);
			perkuliahan.setDosen3(dosen3);
			perkuliahan.setDosen4(dosen4);
			perkuliahan.setDosen5(dosen5);
			perkuliahan.setDosen6(dosen6);
			perkuliahan.setDosen7(dosen7);
			perkuliahan.setDosen8(dosen8);
			perkuliahan.setDosen9(dosen9);
			perkuliahan.setDosen10(dosen10);

			perkuliahan.setJurusan(jurusan);
			perkuliahan.setProgram(program);
			perkuliahan.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
			perkuliahan.setSemester(kurikulumPunyaMatakuliah.getSemester());
			perkuliahan.setKurikulum(kurikulumPunyaMatakuliah.getKurikulum());

			String olehId = Common.generateOlehId(tbmuser);
			perkuliahan.setOlehId(olehId);
			perkuliahan.setMatakuliah(matakuliah);
			perkuliahan.setKelas(kelas);
			perkuliahan.setTahunAjaran(tahunAkademik);
			perkuliahan.setDeskripsiPembelajaran(bahasan);
			perkuliahan.setPerkuliahanDimulai(mulai);
			perkuliahan.setPerkuliahanSampai(selesai);
			perkuliahan.setStatusSemesterPendek(statusSemesterPendek);

			session.getTransaction().begin();
			try {
				session.saveOrUpdate(perkuliahan);
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
						+ "src/ais/action/master/feeder/integrator/helper/ImporKelasFeeder.java");
				}
				throw eSimpan;
			}
		} else if (perkuliahan == null) {
			perkuliahan = new Perkuliahan();
			perkuliahan.setOleh(tbmuser.getUserNama());

			perkuliahan.setMerupakan_tanpa_jadwal_perkuliahan(waktuMulai == null || waktuMulai.trim().isEmpty());
			perkuliahan.setMerupakan_tanpa_dosen(dosen1 == null);
			perkuliahan.setMerupakan_tanpa_ruangan(ruang == null);
			perkuliahan.setWaktuMulai(waktuMulai);
			perkuliahan.setWaktuSelesai(waktuSelesai);
			perkuliahan.setHari(hari);
			perkuliahan.setRuang(ruang);
			perkuliahan.setDosen1(dosen1);
			perkuliahan.setDosen2(dosen2);
			perkuliahan.setDosen3(dosen3);
			perkuliahan.setDosen4(dosen4);
			perkuliahan.setDosen5(dosen5);
			perkuliahan.setDosen6(dosen6);
			perkuliahan.setDosen7(dosen7);
			perkuliahan.setDosen8(dosen8);
			perkuliahan.setDosen9(dosen9);
			perkuliahan.setDosen10(dosen10);

			perkuliahan.setJurusan(jurusan);
			perkuliahan.setProgram(program);
			perkuliahan.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
			perkuliahan.setSemester(kurikulumPunyaMatakuliah.getSemester());
			perkuliahan.setKurikulum(kurikulumPunyaMatakuliah.getKurikulum());

			String olehId = Common.generateOlehId(tbmuser);
			perkuliahan.setOlehId(olehId);
			perkuliahan.setMatakuliah(matakuliah);
			perkuliahan.setKelas(kelas);
			perkuliahan.setTahunAjaran(tahunAkademik);
			perkuliahan.setDeskripsiPembelajaran(bahasan);
			perkuliahan.setPerkuliahanDimulai(mulai);
			perkuliahan.setPerkuliahanSampai(selesai);
			perkuliahan.setStatusSemesterPendek(statusSemesterPendek);

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
						+ "src/ais/action/master/feeder/integrator/helper/ImporKelasFeeder.java");
				}
				throw eSimpan;
			}

		}
		return perkuliahan;
	}
}
