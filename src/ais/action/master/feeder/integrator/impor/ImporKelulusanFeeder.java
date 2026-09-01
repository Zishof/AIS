package ais.action.master.feeder.integrator.impor;

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
import ais.action.master.feeder.integrator.ekspor.SaringanFeeder;
import ais.common.UploadReportHelper;
import ais.common.newui.pekerjaan.PekerjaanRegistry;
import ais.database.model.Tbmuser;

/**
 * Pembaca berkas unggahan Kelulusan untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan pembacanya DIPINDAHKAN dari {@code UploadKelulusan} — bukan disalin — supaya
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
public final class ImporKelulusanFeeder {

    private ImporKelulusanFeeder() { }

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
        final UploadReportHelper report = new UploadReportHelper("Upload Kelulusan");
        final HasilImpor hasil = new HasilImpor();
        hasil.berkasHasil = tujuan;
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
					// dari session lain yang sudah tertutup) -> update pasti ter-flush.
					Mahasiswa mahasiswa = mahasiswaMentah == null ? null
							: (Mahasiswa) session.get(Mahasiswa.class, mahasiswaMentah.getId());

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
						try {
							Common.refreshUpdate(session, mahasiswa);
							Common.refreshSaveOrUpdate(session, skripsi);
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
									+ "src/ais/action/master/feeder/integrator/helper/UploadKelulusan.java");
							}
							throw eSimpan;
						}

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
		}


        hasil.laporan = report.simpanLaporan();
        hasil.ringkasan = report.getRingkasan();
        progres.lapor(100, "Selesai memproses " + hasil.baris + " baris.");
        return hasil;
    }
}
