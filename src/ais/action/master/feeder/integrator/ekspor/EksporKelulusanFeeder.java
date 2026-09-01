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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.common.newui.pekerjaan.PekerjaanRegistry;

/**
 * Penyusun berkas ekspor Kelulusan untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan penyusunnya DIPINDAHKAN dari {@code DownloadKelulusan} — bukan disalin —
 * supaya pemetaan kolom yang ditetapkan PDDIKTI hanya ada di satu tempat.
 * Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporKelulusanFeeder {

    private EksporKelulusanFeeder() { }

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

		XSSFSheet sheet = workbook.createSheet("Kelulusan");
		sheet.setDefaultColumnWidth(18);

		XSSFRow rowhead = sheet.createRow((short) 0);

		rowhead.createCell(0).setCellValue("NIM");
		rowhead.createCell(1).setCellValue("Nama");
		rowhead.createCell(2).setCellValue("Jenis Keluar");
		rowhead.createCell(3).setCellValue("Tanggal Keluar");
		rowhead.createCell(4).setCellValue("Semester Keluar");
		rowhead.createCell(5).setCellValue("SK Yudisium");
		rowhead.createCell(6).setCellValue("Tanggal SK Yudisium");
		rowhead.createCell(7).setCellValue("IPK");
		rowhead.createCell(8).setCellValue("No Seri Ijasah");
		rowhead.createCell(9).setCellValue("Jenis Tugas Akhir");
		rowhead.createCell(10).setCellValue("Judul Skripsi");

		rowhead.createCell(11).setCellValue("Pembimbing I");
		rowhead.createCell(12).setCellValue("Pembimbing II");
		rowhead.createCell(13).setCellValue("Pembimbing III");
		rowhead.createCell(14).setCellValue("Penguji I");
		rowhead.createCell(15).setCellValue("Penguji II");
		rowhead.createCell(16).setCellValue("Penguji III");
		rowhead.createCell(17).setCellValue("Lokasi");
		rowhead.createCell(18).setCellValue("Nomor SK Tugas");
		rowhead.createCell(19).setCellValue("Tanggal SK Tugas");
		rowhead.createCell(20).setCellValue("Kode Prodi");

		Session session = HibernateUtil.currentNativeSession();

		List<Skripsi> skripsis = session.createCriteria(Skripsi.class)

				.add(Restrictions.eq("tahunAkademik", saring.tahunAkademik))

				.add(saring.semester == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.sqlRestriction("this_.semester % 2 = " + (saring.semester.equals(Perkuliahan.GANJIL) ? "1" : "0")))

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

		int size = skripsis.size();

		int rowIndex = 1;
		for (Skripsi skripsi : skripsis) {

			String id_smt = skripsi.getTahunAkademik().split("/")[0]
					+ (skripsi.getSemester() % 2 == 0 ? "2" : "1");

			progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + skripsi.toString() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

			XSSFRow row = sheet.createRow(rowIndex);

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(skripsi.getMahasiswa(),
					skripsi.getSemester(), null, null);

			row.createCell(0).setCellValue(skripsi.getMahasiswa().getNim());
			row.createCell(1).setCellValue(skripsi.getMahasiswa().getNama());
			row.createCell(2).setCellValue(skripsi.getMahasiswa().getStatusKeluar() == null ? ""
					: skripsi.getMahasiswa().getStatusKeluar().getFeeder());
			row.createCell(3).setCellValue(skripsi.getMahasiswa().getTanggalLulus() == null ? ""
					: Common.databaseDateFormat.get().format(skripsi.getMahasiswa().getTanggalLulus()));

			row.createCell(4).setCellValue(id_smt);

			row.createCell(5).setCellValue(skripsi.getMahasiswa().getNoAkta2());

			row.createCell(6).setCellValue(skripsi.getMahasiswa().getTanggalYudisium() == null ? ""
					: Common.databaseDateFormat.get().format(skripsi.getMahasiswa().getTanggalYudisium()));
			row.createCell(7).setCellValue(krsMahasiswa.getIpk());

			row.createCell(8).setCellValue(skripsi.getMahasiswa().getNoIjazah1());
			try {
				row.createCell(9).setCellValue(skripsi.getMahasiswa().getJurusan().getJenjang().getId()
						.equals(ConstantValues.s1.getId()) ? 2 : 3);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadKelulusan.java:331");
				// TODO: handle exception
			}
			row.createCell(10).setCellValue(skripsi.getJudul());

			row.createCell(11)
					.setCellValue(skripsi.getPembimbing() == null ? "" : skripsi.getPembimbing().getNidn());
			row.createCell(12)
					.setCellValue(skripsi.getKetuaSidang() == null ? "" : skripsi.getKetuaSidang().getNidn());
			row.createCell(13)
					.setCellValue(skripsi.getPembimbing3() == null ? "" : skripsi.getPembimbing3().getNidn());

			row.createCell(14)
					.setCellValue(skripsi.getPenguji1() == null ? "" : skripsi.getPenguji1().getNidn());
			row.createCell(15)
					.setCellValue(skripsi.getPenguji2() == null ? "" : skripsi.getPenguji2().getNidn());
			row.createCell(16)
					.setCellValue(skripsi.getPenguji3() == null ? "" : skripsi.getPenguji3().getNidn());

			row.createCell(17).setCellValue(skripsi.getLokasiUjian());
			row.createCell(18).setCellValue(skripsi.getNomorSk());

			row.createCell(19).setCellValue(
					skripsi.getTglSk() == null ? "" : Common.databaseDateFormat.get().format(skripsi.getTglSk()));

			row.createCell(20).setCellValue(skripsi.getMahasiswa().getJurusan().getKodeEpsbed());

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
