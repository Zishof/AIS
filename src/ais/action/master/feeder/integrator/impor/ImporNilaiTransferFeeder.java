package ais.action.master.feeder.integrator.impor;

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
import ais.common.Common;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Konfigurasi;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.action.master.feeder.integrator.ekspor.SaringanFeeder;
import ais.common.UploadReportHelper;
import ais.common.newui.pekerjaan.PekerjaanRegistry;
import ais.database.model.Tbmuser;

/**
 * Pembaca berkas unggahan Nilai Transfer untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan pembacanya DIPINDAHKAN dari {@code UploadNilaiTransfer} — bukan disalin — supaya
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
public final class ImporNilaiTransferFeeder {

    private ImporNilaiTransferFeeder() { }

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
        final UploadReportHelper report = new UploadReportHelper("Upload Nilai Transfer");
        final HasilImpor hasil = new HasilImpor();
        hasil.berkasHasil = tujuan;
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

						progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Upload data " + nim + " - " + kodeMatakuliah + " - " + nilai + " ("
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

								ais.action.master.feeder.integrator.ekspor.EksporNilaiTransferFeeder.createData(
										session, sheet, rowIndex, detailperkuliahan);

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

	private static Detailperkuliahan createDetailperkulihana(Session session, Mahasiswa mahasiswa, String nim,
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
				detailperkuliahan = new Detailperkuliahan(tbmuser, ais.action.master.feeder.integrator.helper.UploadNilaiTransfer.class);
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
					+ "src/ais/action/master/feeder/integrator/helper/ImporNilaiTransferFeeder.java");
			}
			throw eSimpan;
		}

		System.out.println("TA detailperkuliahan = " + detailperkuliahan.getTahunAkademik() + ", semester = "
				+ detailperkuliahan.getSemester() + ", detailperkuliahan.id = " + detailperkuliahan.getId());

		return detailperkuliahan;
	}
}
