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
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.Perkuliahan;
import ais.database.model.kkn.KelompokKkn;
import ais.common.newui.pekerjaan.PekerjaanRegistry;

/**
 * Penyusun berkas ekspor Peserta Dosen KKN untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan penyusunnya DIPINDAHKAN dari {@code DownloadAktifitasMahasiwaKknPesertaDosen} — bukan disalin —
 * supaya pemetaan kolom yang ditetapkan PDDIKTI hanya ada di satu tempat.
 * Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporPesertaDosenKknFeeder {

    private EksporPesertaDosenKknFeeder() { }

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
		rowhead.createCell(2).setCellValue("NIDN/ID Dosen NEO");
		rowhead.createCell(3).setCellValue("Nama Dosen");
		rowhead.createCell(4).setCellValue("Jenis Peran");
		rowhead.createCell(5).setCellValue("Urutan Bimbing/Uji");
		rowhead.createCell(6).setCellValue("Kategori Kegiatan");
		rowhead.createCell(7).setCellValue("Kode Prodi");

		Session session = HibernateUtil.currentNativeSession();

		List<KelompokKkn> kelompokKkns = ConstantValues.simpleList(session.createCriteria(KelompokKkn.class)

				.add(saring.kelas != null && !saring.kelas.trim().isEmpty()
						? Restrictions.ilike("nama_kelompok", saring.kelas.trim(), MatchMode.EXACT)
						: Restrictions.sqlRestriction("true"))

				.createAlias("kkn", "kkn")

				.add(saring.semester == null || saring.semester.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kkn.semester", saring.semester))

				.add(saring.tahunAkademik == null || saring.tahunAkademik.trim().isEmpty()
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kkn.tahunAkademik", saring.tahunAkademik))

				.add(saring.jurusan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kkn.jurusan", saring.jurusan))

				.add(saring.fakultas == null
						|| saring.fakultas == null
								? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqValue("kkn.fakultas", saring.fakultas, false))

				.addOrder(Order.asc("nama_kelompok")), KelompokKkn.class);

		int size = kelompokKkns.size();

		int rowIndex = 1;
		for (KelompokKkn kelompokKkn : kelompokKkns) {
			int pembimbing = 1;
			for (Dosen dosen : kelompokKkn.populateDosenBuNama()) {
				System.out.println("kelompokKkn.getNama_kelompok() -> " + kelompokKkn.getNama_kelompok());

				progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + kelompokKkn.getNama() + " ("
						+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

				XSSFRow row = sheet.createRow(rowIndex);

				String id_smt = saring.tahunAkademik.split("/")[0] + (saring.semester.equals(Perkuliahan.SP) ? "3"
						: saring.semester.equals(Perkuliahan.GENAP) ? "2" : "1");

				XSSFCell cell = row.createCell(0);
				cell.setCellValue(id_smt);

				cell = row.createCell(1);
				cell.setCellValue(kelompokKkn.getId().toString());

				cell = row.createCell(2);
				cell.setCellValue(dosen.getNidn());

				cell = row.createCell(3);
				cell.setCellValue(dosen.getNama());

				cell = row.createCell(4);
				cell.setCellValue(1);

				cell = row.createCell(5);
				cell.setCellValue(pembimbing);

				cell = row.createCell(6);
				cell.setCellValue("110300");

				cell = row.createCell(7);
				cell.setCellValue(kelompokKkn.getKkn().getJurusan() == null ? ""
						: kelompokKkn.getKkn().getJurusan().getKodeEpsbed());

				rowIndex++;
				pembimbing++;
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
