package ais.action.master.feeder.integrator.impor;

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
import ais.common.Common;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CabangPrestasiMahasiswa;
import ais.database.model.KategoriPrestasiMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PrestasiMahasiswa;
import ais.action.master.feeder.integrator.ekspor.SaringanFeeder;
import ais.common.UploadReportHelper;
import ais.common.newui.pekerjaan.PekerjaanRegistry;
import ais.database.model.Tbmuser;

/**
 * Pembaca berkas unggahan Prestasi Mahasiswa untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan pembacanya DIPINDAHKAN dari {@code UploadPrestasiMahasiswa} — bukan disalin — supaya
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
public final class ImporPrestasiMahasiswaFeeder {

    private ImporPrestasiMahasiswaFeeder() { }

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
        final UploadReportHelper report = new UploadReportHelper("Upload Prestasi Mahasiswa");
        final HasilImpor hasil = new HasilImpor();
        hasil.berkasHasil = tujuan;
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

							progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + prestasiMahasiswa.toString() + " ("
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
