package ais.action.master.feeder.integrator.ekspor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.Perkuliahan;
import ais.database.model.pkl.KelompokPkl;
import ais.common.newui.pekerjaan.PekerjaanRegistry;

/**
 * Penyusun berkas ekspor Peserta Mahasiswa PKL untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan penyusunnya DIPINDAHKAN dari {@code DownloadAktifitasMahasiwaPklPesertaMahasiswa} — bukan disalin —
 * supaya pemetaan kolom yang ditetapkan PDDIKTI hanya ada di satu tempat.
 * Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporPesertaMahasiswaPklFeeder {

    private EksporPesertaMahasiswaPklFeeder() { }

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

		XSSFSheet sheet = workbook.createSheet("Template Aktivitas");
		sheet.setDefaultColumnWidth(18);

		XSSFRow rowhead = sheet.createRow((short) 0);

		rowhead.createCell(0).setCellValue("Semester");
		rowhead.createCell(1).setCellValue("ID AKTIVITAS");
		rowhead.createCell(2).setCellValue("NIM/NIPD");
		rowhead.createCell(3).setCellValue("Nama Mahasiswa");
		rowhead.createCell(4).setCellValue("Jenis Peran");
		rowhead.createCell(5).setCellValue("Kode Prodi Aktivitas");
		rowhead.createCell(6).setCellValue("Kode Prodi Mahasiswa");

		Session session = HibernateUtil.currentNativeSession();

		List<MahasiswaDapatKelompokPkl> kelompokPkls = ConstantValues.simpleList(
				session.createCriteria(MahasiswaDapatKelompokPkl.class)

						.createAlias("kelompokPkl", "kelompokPkl").createAlias("mahasiswa", "mahasiswa")

						.add(saring.kelas != null && !saring.kelas.trim().isEmpty()
								? Restrictions.ilike("kelompokPkl.nama_kelompok", saring.kelas.trim(), MatchMode.EXACT)
								: Restrictions.sqlRestriction("true"))

						.createAlias("kelompokPkl.pkl", "pkl")

						.add(saring.semester == null || saring.semester.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("pkl.semester", saring.semester))

						.add(saring.tahunAkademik == null || saring.tahunAkademik.trim().isEmpty()
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("pkl.tahunAkademik", saring.tahunAkademik))

						.add(saring.jurusan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("mahasiswa.jurusan", saring.jurusan))

						.createAlias("mahasiswa.jurusan", "jurusan")

						.add(saring.fakultas == null
								|| saring.fakultas == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqValue("jurusan.fakultas", saring.fakultas, false))

						.addOrder(Order.asc("mahasiswa.nim")).addOrder(Order.asc("kelompokPkl.nama_kelompok")),
				MahasiswaDapatKelompokPkl.class);

		int size = kelompokPkls.size();

		int rowIndex = 1;
		for (MahasiswaDapatKelompokPkl kelompokPklDapatKelompokPkl : kelompokPkls) {
			KelompokPkl kelompokPkl = kelompokPklDapatKelompokPkl.getKelompokPkl();
			Mahasiswa mahasiswa = kelompokPklDapatKelompokPkl.getMahasiswa();
			System.out.println("kelompokPkl.getNama_kelompok() -> " + kelompokPkl.getNama_kelompok());

			progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + kelompokPkl.getNama() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

			XSSFRow row = sheet.createRow(rowIndex);

			String id_smt = saring.tahunAkademik.split("/")[0]
					+ (saring.semester.equals(Perkuliahan.SP) ? "3" : saring.semester.equals(Perkuliahan.GENAP) ? "2" : "1");

			XSSFCell cell = row.createCell(0);
			cell.setCellValue(id_smt);

			cell = row.createCell(1);
			cell.setCellValue(kelompokPkl.getId().toString());

			cell = row.createCell(2);
			cell.setCellValue(mahasiswa.getNim());

			cell = row.createCell(3);
			cell.setCellValue(mahasiswa.getNama());

			cell = row.createCell(4);
			cell.setCellValue(2);

			cell = row.createCell(5);
			cell.setCellValue(kelompokPkl.getPkl() == null || kelompokPkl.getPkl().getJurusan() == null ? ""
					: kelompokPkl.getPkl().getJurusan().getKodeEpsbed());

			cell = row.createCell(6);
			cell.setCellValue(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getKodeEpsbed());

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
