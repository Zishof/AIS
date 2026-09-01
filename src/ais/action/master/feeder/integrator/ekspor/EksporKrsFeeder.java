package ais.action.master.feeder.integrator.ekspor;

import java.io.File;
import java.io.FileOutputStream;
import java.awt.Color;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.common.newui.pekerjaan.PekerjaanRegistry;

/**
 * Penyusun berkas ekspor KRS untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan penyusunnya DIPINDAHKAN dari {@code DownloadKrs} — bukan disalin —
 * supaya pemetaan kolom yang ditetapkan PDDIKTI hanya ada di satu tempat.
 * Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporKrsFeeder {

    private EksporKrsFeeder() { }

    /**
     * Susun berkas ekspor ke {@code tujuan}.
     *
     * @return jumlah baris data yang ditulis
     */
    public static int tulis(File tujuan, SaringanFeeder saring, PekerjaanRegistry.Progres progres)
            throws Exception {
        if (saring == null) saring = new SaringanFeeder();
        // Layar lama membaca kotak isian dengan getValue().trim() untuk kelas dan
        // apa adanya untuk sisanya. Disamakan di sini supaya pemanggil mana pun —
        // panel ZK maupun controller native — menghasilkan saringan yang sama.
        saring.rapikan();
        if (progres == null) {
            progres = new PekerjaanRegistry.Progres() {
                public void lapor(int persen, String pesan) { }
            };
        }
		XSSFWorkbook workbook = new XSSFWorkbook();

		XSSFSheet sheet = workbook.createSheet("KRS");
		sheet.setDefaultColumnWidth(18);

		XSSFRow rowhead = sheet.createRow((short) 0);

		rowhead.createCell(0).setCellValue("NIM");
		rowhead.createCell(1).setCellValue("Nama Mahasiswa");
		rowhead.createCell(2).setCellValue("Semester");
		rowhead.createCell(3).setCellValue("Kode MK");
		rowhead.createCell(4).setCellValue("Mata Kuliah");
		rowhead.createCell(5).setCellValue("Kelas");
		rowhead.createCell(6).setCellValue("Kode Prodi");

		Session session = HibernateUtil.currentNativeSession();

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (saring.status != null) {
			String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ saring.status.getId() + " and tahunakademik = '"
					+ saring.tahunAjaran + "' and semester%2="
					+ (saring.jenisSemester.equals(Perkuliahan.GANJIL) ? 1 : 0)
					+ ")";
			System.out.println("sql=>" + sql);
			criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)

				.add(criteriaStatus)

				.add(saring.tahunAjaran.isEmpty()
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik",
										saring.tahunAjaran))

				.add(saring.jenisSemester.isEmpty()
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.sqlRestriction("this_.semester % 2 = " + (saring.jenisSemester.equals(Perkuliahan.GANJIL) ? "1" : "0")))

				.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN)
				.createAlias("perkuliahan", "perkuliahan", Criteria.INNER_JOIN)

				.add(saring.masaPerkuliahan == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("perkuliahan.masaPerkuliahan",
								saring.masaPerkuliahan))

				.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)

				.add(saring.kelas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("perkuliahan.kelas", saring.kelas, MatchMode.ANYWHERE))

				.add(saring.kodeMatakuliah.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.or(
										Restrictions.ilike("matakuliahKonversi.nama",
												saring.kodeMatakuliah.trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("matakuliah.nama", saring.kodeMatakuliah.trim(),
												MatchMode.ANYWHERE)),
								Restrictions.or(
										Restrictions.ilike("matakuliahKonversi.kode",
												saring.kodeMatakuliah.trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("matakuliah.kode", saring.kodeMatakuliah.trim(),
												MatchMode.ANYWHERE))))

				.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))

				.add(saring.semesterKe == null || saring.semesterKe.equals(-1) ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("semester", saring.semesterKe))

				.createAlias("mahasiswa", "mahasiswa")

				.add(Restrictions.and(
						saring.nim.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("mahasiswa.nim", saring.nim.trim(),
										MatchMode.ANYWHERE),

						saring.nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("mahasiswa.nama", saring.nama.trim(),
										MatchMode.ANYWHERE)))

				.add(saring.jurusan == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("mahasiswa.jurusan", saring.jurusan))

				.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(saring.fakultas == null
								? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqValue("jurusan.fakultas", saring.fakultas, false))

				.add(saring.program == null
						|| saring.program == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("mahasiswa.program",
										saring.program))

				.add(saring.angkatan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("mahasiswa.tahunangkatan", saring.angkatan))

				.addOrder(Order.asc("mahasiswa.nim")).addOrder(Order.asc("semester")).list();

		int size = detailperkuliahans.size();

		XSSFCellStyle notLocked = workbook.createCellStyle();
		notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

		int rowIndex = 1;
		for (Detailperkuliahan detailperkuliahanLain : detailperkuliahans) {

			progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + detailperkuliahanLain.toString() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

			if (createData(session, sheet, rowIndex, detailperkuliahanLain,
					(MasaPerkuliahan) saring.masaPerkuliahan, notLocked)) {
				rowIndex++;
			}

		}


        Common.setStyled(sheet);
        FileOutputStream keluaran = new FileOutputStream(tujuan);
        try {
            workbook.write(keluaran);
        } finally {
            keluaran.close();
        }
        progres.lapor(100, "Selesai menyusun " + (rowIndex - 1) + " baris.");
        return rowIndex - 1;
    }

	public static boolean createData(Session session, XSSFSheet sheet, int rowIndex,
			Detailperkuliahan detailperkuliahan, MasaPerkuliahan masaPerkuliahan, XSSFCellStyle notLocked) {

		Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() == null
				? (detailperkuliahan.getPerkuliahan() == null ? null
						: detailperkuliahan.getPerkuliahan().getMatakuliah())
				: detailperkuliahan.getMatakuliahKonversi();
		Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
		if (matakuliah != null && detailperkuliahan.getTahunAkademik() != null) {

			XSSFRow row = sheet.createRow(rowIndex);

			String id_smt = masaPerkuliahan != null ? masaPerkuliahan.getNama()
					: detailperkuliahan.getTahunAkademik().split("/")[0] + (detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek()
									.equals(Perkuliahan.SEMESTER_PENDEK) ? "3"
											: (detailperkuliahan.getSemester() % 2 == 0 ? "2" : "1"));

			if (mahasiswa.getSemesterMulai().equals(Perkuliahan.GENAP)) {
				id_smt = masaPerkuliahan != null ? masaPerkuliahan.getNama()
						: detailperkuliahan.getTahunAkademik().split("/")[0]
								+ (detailperkuliahan.getPerkuliahan() != null
										&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
										&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek()
												.equals(Perkuliahan.SEMESTER_PENDEK) ? "3"
														: (detailperkuliahan.getSemester() % 2 == 0 ? "1" : "2"));
			}

			XSSFCell cell = row.createCell(0);
			cell.setCellStyle(notLocked);
			cell.setCellValue(mahasiswa.getNim());

			cell = row.createCell(1);
			cell.setCellValue(mahasiswa.getNama());

			cell = row.createCell(2);
			cell.setCellStyle(notLocked);
			cell.setCellValue(id_smt);

			cell = row.createCell(3);
			cell.setCellStyle(notLocked);
			cell.setCellValue(matakuliah.getKode());

			cell = row.createCell(4);
			cell.setCellValue(matakuliah.getNama());

			cell = row.createCell(5);
			cell.setCellStyle(notLocked);
			cell.setCellValue(
					detailperkuliahan.getPerkuliahan() == null ? "" : detailperkuliahan.getPerkuliahan().getKelas());

			cell = row.createCell(6);
			cell.setCellStyle(notLocked);
			cell.setCellValue(detailperkuliahan.getMatakuliahKonversi() != null
					? detailperkuliahan.getMatakuliahKonversi().getJurusan().getKodeEpsbed()
					: detailperkuliahan.getPerkuliahan().getJurusan().getKodeEpsbed());

			return true;

		} else {
			return false;
		}
	}
}
