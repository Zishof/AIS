package ais.action.master.feeder.integrator.ekspor;

import java.io.File;
import java.io.FileOutputStream;
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
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.StatusMahasiswa;
import ais.common.newui.pekerjaan.PekerjaanRegistry;

/**
 * Penyusun berkas ekspor Nilai Transfer untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan penyusunnya DIPINDAHKAN dari {@code DownloadNilaiTransfer} — bukan disalin —
 * supaya pemetaan kolom yang ditetapkan PDDIKTI hanya ada di satu tempat.
 * Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporNilaiTransferFeeder {

    private EksporNilaiTransferFeeder() { }

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

		XSSFSheet sheet = workbook.createSheet("NILAI Transfer");
		sheet.setDefaultColumnWidth(18);

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

		Session session = HibernateUtil.currentNativeSession();

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (saring.status != null) {
			String sql = "this_.mahasiswa in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ saring.status.getId() + " and tahunakademik = '"
					+ Common.getCurrentTahunAkademik() + "' and semester%2="
					+ (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
			System.out.println("sql=>" + sql);
			criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)

				.add(criteriaStatus)

				.add(Restrictions.isNotNull("matakuliahKonversi"))

//						.add(saring.tahunAjaran.isEmpty()
//								|| saring.tahunAjaran == null
//										? Restrictions.sqlRestriction("1=1")
//										: Restrictions.eq("tahunAkademik",
//												saring.tahunAjaran))
//
//						.add(saring.jenisSemester.isEmpty()
//								|| saring.jenisSemester == null
//										? Restrictions.sqlRestriction("1=1")
//										: Restrictions.sqlRestriction("this_.semester % 2 = " + (searchJenisSemester
//												.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "0")))

				.createAlias("matakuliahKonversi", "matakuliahKonversi")

				.add(saring.kodeMatakuliah.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("matakuliahKonversi.kode", saring.kodeMatakuliah.trim(),
								MatchMode.ANYWHERE))

				.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))

//						.add(saring.semester == null || saring.semester.equals(-1) ? Restrictions.sqlRestriction("true")
//								: Restrictions.eq("semester", saring.semester))

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
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("mahasiswa.program",
										saring.program))

				.add(saring.angkatan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("mahasiswa.tahunangkatan", saring.angkatan))

				.addOrder(Order.asc("mahasiswa.nim")).addOrder(Order.asc("semester")).list();

		int size = detailperkuliahans.size();

		int rowIndex = 1;
		for (Detailperkuliahan detailperkuliahanLain : detailperkuliahans) {

			progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + detailperkuliahanLain.toString() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

			createData(session, sheet, rowIndex, detailperkuliahanLain);
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

	public static void createData(Session session, XSSFSheet sheet, int rowIndex, Detailperkuliahan detailperkuliahan) {

		Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() == null
				? (detailperkuliahan.getPerkuliahan() == null ? null
						: detailperkuliahan.getPerkuliahan().getMatakuliah())
				: detailperkuliahan.getMatakuliahKonversi();
		Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
		if (matakuliah != null) {

			XSSFRow row = sheet.createRow(rowIndex);

			XSSFCell cell = row.createCell(0);
			cell.setCellValue(mahasiswa.getNim());

			cell = row.createCell(1);
			cell.setCellValue(mahasiswa.getNama());

			cell = row.createCell(2);
			cell.setCellValue(detailperkuliahan.getKodeMatakuliahAsal());

			cell = row.createCell(3);
			cell.setCellValue(detailperkuliahan.getNamaMatakuliahAsal());

			cell = row.createCell(4);
			cell.setCellValue(detailperkuliahan.getSksAsal());

			cell = row.createCell(5);
			cell.setCellValue(detailperkuliahan.getNilaiHurufAsal());

			cell = row.createCell(6);
			cell.setCellValue(matakuliah.getKode());

			cell = row.createCell(7);
			cell.setCellValue(matakuliah.getNama());

			cell = row.createCell(8);
			cell.setCellValue(detailperkuliahan.getNilaiHuruf());

			cell = row.createCell(9);
			cell.setCellValue(detailperkuliahan.getTotalIP());

			cell = row.createCell(10);
			cell.setCellValue(detailperkuliahan.getMatakuliahKonversi() != null
					? detailperkuliahan.getMatakuliahKonversi().getJurusan() == null ? ""
							: detailperkuliahan.getMatakuliahKonversi().getJurusan().getKodeEpsbed()
					: detailperkuliahan.getPerkuliahan().getJurusan().getKodeEpsbed());

		}
	}
}
