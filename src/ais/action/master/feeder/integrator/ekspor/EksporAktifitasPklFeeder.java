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
import ais.database.model.Perkuliahan;
import ais.database.model.pkl.KelompokPkl;
import ais.common.newui.pekerjaan.PekerjaanRegistry;

/**
 * Penyusun berkas ekspor Aktifitas PKL untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan penyusunnya DIPINDAHKAN dari {@code DownloadAktifitasMahasiwaPkl} — bukan disalin —
 * supaya pemetaan kolom yang ditetapkan PDDIKTI hanya ada di satu tempat.
 * Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporAktifitasPklFeeder {

    private EksporAktifitasPklFeeder() { }

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
		rowhead.createCell(1).setCellValue("Jenis Aktivitas");
		rowhead.createCell(2).setCellValue("Judul");
		rowhead.createCell(3).setCellValue("Lokasi");
		rowhead.createCell(4).setCellValue("Nomor SK Tugas");
		rowhead.createCell(5).setCellValue("Tanggal SK Tugas");
		rowhead.createCell(6).setCellValue("Jenis Anggota");
		rowhead.createCell(7).setCellValue("ID AKTIVITAS");
		rowhead.createCell(8).setCellValue("Kode Prodi");
		rowhead.createCell(9).setCellValue("Program MBKM");
		rowhead.createCell(10).setCellValue("Tanggal Mulai");
		rowhead.createCell(11).setCellValue("Tanggal Selesai");

		Session session = HibernateUtil.currentNativeSession();

		List<KelompokPkl> kelompokPkls = ConstantValues.simpleList(session.createCriteria(KelompokPkl.class)

				.add(saring.kelas != null && !saring.kelas.trim().isEmpty()
						? Restrictions.ilike("nama_kelompok", saring.kelas.trim(), MatchMode.EXACT)
						: Restrictions.sqlRestriction("true"))

				.createAlias("pkl", "pkl")

				.add(saring.semester == null || saring.semester.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pkl.semester", saring.semester))

				.add(saring.tahunAkademik == null || saring.tahunAkademik.trim().isEmpty()
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pkl.tahunAkademik", saring.tahunAkademik))

				.add(saring.jurusan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pkl.jurusan", saring.jurusan))

				.add(saring.fakultas == null
						|| saring.fakultas == null
								? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqValue("pkl.fakultas", saring.fakultas, false))

				.addOrder(Order.asc("nama_kelompok")), KelompokPkl.class);

		int size = kelompokPkls.size();

		int rowIndex = 1;
		for (KelompokPkl kelompokPkl : kelompokPkls) {

			System.out.println("kelompokPkl.getNama_kelompok() -> " + kelompokPkl.getNama_kelompok());

			String idjenis = kelompokPkl.getPkl().getJenisAktfitasMahasiswa() == null ? "6"
					: kelompokPkl.getPkl().getJenisAktfitasMahasiswa().getFeeder().toString();

			progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + kelompokPkl.getNama() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

			XSSFRow row = sheet.createRow(rowIndex);

			String id_smt = saring.tahunAkademik.split("/")[0]
					+ (saring.semester.equals(Perkuliahan.SP) ? "3" : saring.semester.equals(Perkuliahan.GENAP) ? "2" : "1");

			XSSFCell cell = row.createCell(0);
			cell.setCellValue(id_smt);

			cell = row.createCell(1);
			cell.setCellValue(idjenis);

			cell = row.createCell(2);
			cell.setCellValue(kelompokPkl.getNama_kelompok());

			cell = row.createCell(3);
			cell.setCellValue(kelompokPkl.getAlamat());

			cell = row.createCell(4);
			cell.setCellValue(kelompokPkl.getNoSk());

			cell = row.createCell(5);
			cell.setCellValue(kelompokPkl.getTglSk() == null ? ""
					: Common.databaseDateFormat.get().format(kelompokPkl.getTglSk()));

			cell = row.createCell(6);
			cell.setCellValue("1");

			cell = row.createCell(7);
			cell.setCellValue(kelompokPkl.getId().toString());

			cell = row.createCell(8);
			cell.setCellValue(kelompokPkl.getPkl() == null || kelompokPkl.getPkl().getJurusan() == null ? ""
					: kelompokPkl.getPkl().getJurusan().getKodeEpsbed());

			cell = row.createCell(9);
			cell.setCellValue("1");

			cell = row.createCell(10);
			cell.setCellValue(kelompokPkl.getTanggal_mulai() == null ? ""
					: Common.databaseDateFormat.get().format(kelompokPkl.getTanggal_mulai()));

			cell = row.createCell(11);
			cell.setCellValue(kelompokPkl.getTanggal_selesai() == null ? ""
					: Common.databaseDateFormat.get().format(kelompokPkl.getTanggal_selesai()));

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
