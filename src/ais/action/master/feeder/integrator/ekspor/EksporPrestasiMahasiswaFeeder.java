package ais.action.master.feeder.integrator.ekspor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.PrestasiMahasiswa;
import ais.common.newui.pekerjaan.PekerjaanRegistry;

/**
 * Penyusun berkas ekspor Prestasi Mahasiswa untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan penyusunnya DIPINDAHKAN dari {@code DownloadPrestasiMahasiswa} — bukan disalin —
 * supaya pemetaan kolom yang ditetapkan PDDIKTI hanya ada di satu tempat.
 * Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporPrestasiMahasiswaFeeder {

    private EksporPrestasiMahasiswaFeeder() { }

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

		XSSFSheet sheet = workbook.createSheet("Prestasi Mahasiswa");
		sheet.setDefaultColumnWidth(18);

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

		Session session = HibernateUtil.currentNativeSession();

		List<PrestasiMahasiswa> prestasiMahasiswas = session.createCriteria(PrestasiMahasiswa.class)

				.add(Restrictions.eq("status", PrestasiMahasiswa.DISETUJUI))

				.add(saring.tahunAkademik == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik",
										saring.tahunAkademik))

				.add(saring.semester == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisSemester", saring.semester))

				.createAlias("mahasiswa", "mahasiswa")

				.add(saring.kelas != null && !saring.kelas.trim().isEmpty()
						? Restrictions.ilike("mahasiswa.kelas", saring.kelas.trim(), MatchMode.EXACT)
						: Restrictions.sqlRestriction("true"))

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

				.addOrder(Order.desc("id")).list();

		int size = prestasiMahasiswas.size();

		int rowIndex = 1;
		for (PrestasiMahasiswa prestasiMahasiswa : prestasiMahasiswas) {

			progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + prestasiMahasiswa.toString() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

			XSSFRow row = sheet.createRow(rowIndex);

			row.createCell(0).setCellValue(prestasiMahasiswa.getMahasiswa().getNim());
			row.createCell(1).setCellValue(prestasiMahasiswa.getMahasiswa().getNama());
			row.createCell(2).setCellValue(prestasiMahasiswa.getCabangPrestasiMahasiswa() == null ? "9"
					: prestasiMahasiswa.getCabangPrestasiMahasiswa().getKode());
			row.createCell(3).setCellValue(prestasiMahasiswa.getKategoriPrestasiMahasiswa() == null ? "9"
					: prestasiMahasiswa.getKategoriPrestasiMahasiswa().getKode());
			row.createCell(4).setCellValue(prestasiMahasiswa.getNama());
			row.createCell(5).setCellValue(prestasiMahasiswa.getTahun());
			row.createCell(6).setCellValue(prestasiMahasiswa.getPenyelenggara());
			row.createCell(7).setCellValue(prestasiMahasiswa.getPeringkat() == null ? ""
					: prestasiMahasiswa.getPeringkat().toString());

			row.createCell(8).setCellValue(prestasiMahasiswa.getMahasiswa().getJurusan().getKodeEpsbed());

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
