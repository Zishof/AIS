package ais.action.master.feeder.integrator.impor;

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
import ais.action.master.feeder.integrator.ekspor.SaringanFeeder;
import ais.common.UploadReportHelper;
import ais.common.newui.pekerjaan.PekerjaanRegistry;
import ais.database.model.Tbmuser;

/**
 * Pembaca berkas unggahan Ajar Dosen untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan pembacanya DIPINDAHKAN dari {@code UploadAjarDosen} — bukan disalin — supaya
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
public final class ImporAjarDosenFeeder {

    private ImporAjarDosenFeeder() { }

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
        final UploadReportHelper report = new UploadReportHelper("Upload Ajar Dosen");
        final HasilImpor hasil = new HasilImpor();
        hasil.berkasHasil = tujuan;
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
									.add(Restrictions.eq("jurusan", saring.jurusan)).setMaxResults(1).uniqueResult());

					Dosen dosen = (Dosen) (nidn == null || nidn.trim().isEmpty() ? null
							: session.createCriteria(Dosen.class)
									.add(Restrictions.ilike("nidn", nidn.trim(), MatchMode.EXACT))
									.setMaxResults(1).uniqueResult());

					KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) (matakuliah == null
							? null
							: session.createCriteria(KurikulumPunyaMatakuliah.class)
									.createAlias("kurikulum", "kurikulum")
									.createAlias("kurikulum.program", "program")
									.add(Restrictions.eq("kurikulum.jurusan", saring.jurusan))
									.add(Restrictions.eq("program.nama", saring.namaProgram))
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

						progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Upload data " + matakuliah + " - " + smt + " - " + dosen + " ("
								+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

						Perkuliahan perkuliahan = ImporAjarDosenFeeder.checkPerkuliahan(session, dosen, kelas,
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
						cell.setCellValue(saring.jurusan.getKodeEpsbed());

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

			Common.setStyled(sheet);hasil.baris = rowIndex + 1;

			try {
				FileOutputStream fileOut = new FileOutputStream(filename);
				workbook.write(fileOut);
				fileOut.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}

		} catch (Exception galat) {
			// Layar lama menuliskan kegagalan ini ke label progres lalu return.
			// Di sini ia dilempar supaya pemanggil melaporkannya sebagai gagal,
			// bukan sebagai selesai tanpa data.
			throw galat;
		}

        hasil.laporan = report.simpanLaporan();
        hasil.ringkasan = report.getRingkasan();
        progres.lapor(100, "Selesai memproses " + hasil.baris + " baris.");
        return hasil;
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
						+ "src/ais/action/master/feeder/integrator/helper/ImporAjarDosenFeeder.java");
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
					+ "src/ais/action/master/feeder/integrator/helper/ImporAjarDosenFeeder.java");
			}
			throw new RuntimeException(eSimpan);
		}

		return perkuliahan;
	}
}
