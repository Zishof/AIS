package ais.action.master.feeder.integrator.ekspor;

import java.io.File;
import java.io.FileOutputStream;
import java.awt.Color;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.common.newui.pekerjaan.PekerjaanRegistry;

/**
 * Penyusun berkas ekspor Ajar Dosen untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan penyusunnya DIPINDAHKAN dari {@code DownloadAjarDosen} — bukan disalin —
 * supaya pemetaan kolom yang ditetapkan PDDIKTI hanya ada di satu tempat.
 * Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporAjarDosenFeeder {

    private EksporAjarDosenFeeder() { }

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
		final Integer semesterDariAngkatan = saring.angkatan == null || saring.tahunAkademik == null || saring.semester == null ? null
        : Common.getSemester(saring.angkatan, saring.tahunAkademik, saring.semester, Integer.valueOf(1), Perkuliahan.GANJIL);
		XSSFWorkbook workbook = new XSSFWorkbook();

		XSSFSheet sheet = workbook.createSheet("Ajar Dosen");
		sheet.setDefaultColumnWidth(18);

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
		rowhead.createCell(9).setCellValue("Program");
		rowhead.createCell(10).setCellValue("Nama matakuliah");

		Session session = HibernateUtil.currentNativeSession();

		List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(saring.masaPerkuliahan == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("masaPerkuliahan",
								saring.masaPerkuliahan))

				.add(saring.tahunAkademik == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAjaran",
										saring.tahunAkademik))

				.add(saring.semester == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("ganjilGenap", saring.semester))

				.add(!saring.kelas.trim().isEmpty() ? Restrictions.ilike("kelas", saring.kelas.trim(), MatchMode.ANYWHERE)
						: Restrictions.sqlRestriction("true"))

				.add(semesterDariAngkatan == null || semesterDariAngkatan.intValue() <= 0
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("semester", semesterDariAngkatan))

				.add(saring.jurusan == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan", saring.jurusan))

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(saring.fakultas == null
								? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqValue("jurusan.fakultas", saring.fakultas, false))

				.add(saring.program == null
						|| saring.program == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("program", saring.program))

				.addOrder(Order.desc("id")).list();

		XSSFCellStyle notLocked = workbook.createCellStyle();
		notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

		int size = perkuliahans.size();

		int rowIndex = 1;
		for (Perkuliahan perkuliahan : perkuliahans) {

			Map<String, Dosen> dosens = perkuliahan.populateDosen();

			progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + perkuliahan.toString() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

			if (dosens.isEmpty()) {

				XSSFRow row = sheet.createRow(rowIndex);

				String id_smt = saring.masaPerkuliahan != null
						? ((MasaPerkuliahan) saring.masaPerkuliahan).getNama()
						: perkuliahan.getTahunAjaran().split("/")[0]
								+ (perkuliahan.getStatusSemesterPendek() != null && perkuliahan
										.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK) ? "3"
												: (perkuliahan.getSemester() % 2 == 0 ? "2" : "1"));

				XSSFCell cell = row.createCell(0);
				cell.setCellStyle(notLocked);
				cell.setCellValue(id_smt);

				cell = row.createCell(1);
				cell.setCellValue("");

				cell = row.createCell(2);
				cell.setCellValue("");

				cell = row.createCell(3);
				cell.setCellStyle(notLocked);
				cell.setCellValue(
						perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());

				cell = row.createCell(4);
				cell.setCellStyle(notLocked);
				cell.setCellValue(perkuliahan.getKelas());

				cell = row.createCell(5);
				cell.setCellValue(perkuliahan.getJumlahMaksimalPertemuan());

				int jumlah = ((Number) session.createCriteria(Pertemuan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
						.uniqueResult()).intValue();

				cell = row.createCell(6);
				cell.setCellValue(jumlah);

				cell = row.createCell(7);
				cell.setCellValue(perkuliahan.getJurusan().getKodeEpsbed());

				cell = row.createCell(8);
				cell.setCellValue(
						perkuliahan.getMatakuliah() == null ? null : perkuliahan.getMatakuliah().getSks());

				cell = row.createCell(9);
				cell.setCellStyle(notLocked);
				cell.setCellValue(perkuliahan.getProgram());

				cell = row.createCell(10);
				cell.setCellValue(
						perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());

				rowIndex++;

			} else {

				for (Dosen dosen : dosens.values()) {
					XSSFRow row = sheet.createRow(rowIndex);

					String id_smt = saring.masaPerkuliahan != null
							? ((MasaPerkuliahan) saring.masaPerkuliahan)
									.getNama()
							: perkuliahan.getTahunAjaran().split("/")[0]
									+ (perkuliahan.getStatusSemesterPendek() != null && perkuliahan
											.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK) ? "3"
													: (perkuliahan.getSemester() % 2 == 0 ? "2" : "1"));

					XSSFCell cell = row.createCell(0);
					cell.setCellStyle(notLocked);
					cell.setCellValue(id_smt);

					cell = row.createCell(1);
					cell.setCellStyle(notLocked);
					cell.setCellValue(dosen.getNidn());

					cell = row.createCell(2);
					cell.setCellValue(dosen.getNama());

					cell = row.createCell(3);
					cell.setCellStyle(notLocked);
					cell.setCellValue(
							perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());

					cell = row.createCell(4);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan.getKelas());

					cell = row.createCell(5);
					cell.setCellValue(perkuliahan.getJumlahMaksimalPertemuan());

					int jumlah = ((Number) session.createCriteria(Pertemuan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("perkuliahan", perkuliahan))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();

					cell = row.createCell(6);
					cell.setCellValue(jumlah);

					cell = row.createCell(7);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan.getJurusan().getKodeEpsbed());

					cell = row.createCell(8);
					cell.setCellValue(
							perkuliahan.getMatakuliah() == null ? null : perkuliahan.getMatakuliah().getSks());

					cell = row.createCell(9);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan.getProgram());

					cell = row.createCell(10);
					cell.setCellValue(
							perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());

					rowIndex++;
				}
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
}
