package ais.action.master.feeder.integrator.ekspor;

import java.io.File;
import java.io.FileOutputStream;
import java.awt.Color;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
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
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.common.newui.pekerjaan.PekerjaanRegistry;

/**
 * Penyusun berkas ekspor Kelas untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan penyusunnya DIPINDAHKAN dari {@code DownloadKelas} — bukan disalin —
 * supaya pemetaan kolom yang ditetapkan PDDIKTI hanya ada di satu tempat.
 * Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporKelasFeeder {

    private EksporKelasFeeder() { }

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

		XSSFSheet sheet = workbook.createSheet("KELAS");
		sheet.setDefaultColumnWidth(18);

		XSSFRow rowhead = sheet.createRow((short) 0);

		rowhead.createCell(0).setCellValue("semester");
		rowhead.createCell(1).setCellValue("Kode matakuliah");
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

				.add(saring.jurusan == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan", saring.jurusan))

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(saring.fakultas == null
								? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqValue("jurusan.fakultas", saring.fakultas, false))

				.add(saring.program == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("program", saring.program))

				.addOrder(Order.desc("id")).list();

		int size = perkuliahans.size();

		XSSFCellStyle notLocked = workbook.createCellStyle();
		notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

		int rowIndex = 1;
		for (Perkuliahan perkuliahan : perkuliahans) {

			progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + perkuliahan.toString() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

			XSSFRow row = sheet.createRow(rowIndex);

			String id_smt = saring.masaPerkuliahan != null
					? ((MasaPerkuliahan) saring.masaPerkuliahan).getNama()
					: perkuliahan.getTahunAjaran().split("/")[0]
							+ (perkuliahan.getStatusSemesterPendek() != null
									&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)
											? "3"
											: (perkuliahan.getSemester() % 2 == 0 ? "2" : "1"));

			XSSFCell cell = row.createCell(0);
			cell.setCellStyle(notLocked);
			cell.setCellValue(id_smt);

			cell = row.createCell(1);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());

			cell = row.createCell(2);
			cell.setCellValue(perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());

			cell = row.createCell(3);
			cell.setCellStyle(notLocked);
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

			cell = row.createCell(7);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan.getJurusan().getKodeEpsbed());

			cell = row.createCell(8);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan.getWaktuMulai());

			cell = row.createCell(9);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan.getWaktuSelesai());

			cell = row.createCell(10);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan.getHari());

			cell = row.createCell(11);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getKode());

			cell = row.createCell(12);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan == null || perkuliahan.getDosen1() == null ? ""
					: perkuliahan.getDosen1().toString());

			cell = row.createCell(13);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan == null || perkuliahan.getDosen2() == null ? ""
					: perkuliahan.getDosen2().toString());

			cell = row.createCell(14);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan == null || perkuliahan.getDosen3() == null ? ""
					: perkuliahan.getDosen3().toString());

			cell = row.createCell(15);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan == null || perkuliahan.getDosen4() == null ? ""
					: perkuliahan.getDosen4().toString());

			cell = row.createCell(16);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan == null || perkuliahan.getDosen5() == null ? ""
					: perkuliahan.getDosen5().toString());

			cell = row.createCell(17);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan == null || perkuliahan.getDosen6() == null ? ""
					: perkuliahan.getDosen6().toString());

			cell = row.createCell(18);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan == null || perkuliahan.getDosen7() == null ? ""
					: perkuliahan.getDosen7().toString());

			cell = row.createCell(19);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan == null || perkuliahan.getDosen8() == null ? ""
					: perkuliahan.getDosen8().toString());

			cell = row.createCell(20);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan == null || perkuliahan.getDosen9() == null ? ""
					: perkuliahan.getDosen9().toString());

			cell = row.createCell(21);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan == null || perkuliahan.getDosen10() == null ? ""
					: perkuliahan.getDosen10().toString());

			cell = row.createCell(22);
			cell.setCellStyle(notLocked);
			cell.setCellValue(perkuliahan == null ? "" : perkuliahan.getProgram());

			rowIndex++;

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
