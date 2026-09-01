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
import ais.action.master.feeder.util.FeederJSONImport;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Perkuliahan;
import ais.common.newui.pekerjaan.PekerjaanRegistry;

/**
 * Penyusun berkas ekspor Peserta Dosen Bimbingan untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan penyusunnya DIPINDAHKAN dari {@code DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhirPesertaDosen} — bukan disalin —
 * supaya pemetaan kolom yang ditetapkan PDDIKTI hanya ada di satu tempat.
 * Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporPesertaDosenBimbinganFeeder {

    private EksporPesertaDosenBimbinganFeeder() { }

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

		List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhirs = ConstantValues
				.simpleList(session.createCriteria(MahasiswaRequestTugasAkhir.class)

						.add(saring.kelas != null && !saring.kelas.trim().isEmpty()
								? Restrictions.ilike("judul", saring.kelas.trim(), MatchMode.EXACT)
								: Restrictions.sqlRestriction("true"))

						.createAlias("mahasiswa", "mahasiswa")

						.add(saring.semester == null || saring.semester.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.sqlRestriction(
										"semester % 2 = " + (saring.semester.equals(Perkuliahan.GENAP) ? "0" : "1")))

						.add(saring.tahunAkademik == null || saring.tahunAkademik.trim().isEmpty()
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", saring.tahunAkademik))

						.add(saring.jurusan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("mahasiswa.jurusan", saring.jurusan))

						.createAlias("mahasiswa.jurusan", "jurusan")

						.add(saring.fakultas == null
								|| saring.fakultas == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqValue("jurusan.fakultas", saring.fakultas, false))

						.addOrder(Order.asc("judul")), MahasiswaRequestTugasAkhir.class);

		int size = mahasiswaRequestTugasAkhirs.size();

		int rowIndex = 1;
		for (MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir : mahasiswaRequestTugasAkhirs) {

			String idjenis = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() == null
					|| mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi()
							.getJenisKegiatanMahasiswa() == null ? null
									: mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi()
											.getJenisKegiatanMahasiswa().getKode();

			if (idjenis == null || idjenis.trim().isEmpty()) {
				idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Laporan akhir studi");
				try {
					if (mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getJenjang().getId()
							.equals(ConstantValues.d3.getId())) {
						idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Tugas akhir");
					} else if (mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getJenjang().getId()
							.equals(ConstantValues.s2.getId())) {
						idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Tesis");
					} else if (mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getJenjang().getId()
							.equals(ConstantValues.s3.getId())) {
						idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Disertasi");
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhirPesertaDosen.java:306");
				}
			}
			progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + mahasiswaRequestTugasAkhir.getNama() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

			XSSFRow row = sheet.createRow(rowIndex);

			String id_smt = saring.tahunAkademik.split("/")[0]
					+ (saring.semester.equals(Perkuliahan.SP) ? "3" : saring.semester.equals(Perkuliahan.GENAP) ? "2" : "1");

			List<CommonVO> dataDosen = mahasiswaRequestTugasAkhir.dataDosen(true);
			int pembimbing = 0;
			int penguji = 0;
			for (CommonVO commonVO : dataDosen) {
				Dosen dosen = (Dosen) commonVO.getValueObject();

				String key = commonVO.getName();
				String nama1 = commonVO.getName1();
				if (key.toLowerCase().trim().contains("penguji")) {

					if (nama1 != null && !nama1.isEmpty()) {

					} else {
						nama1 = "110500";
					}

					penguji++;
				} else {
					if (nama1 != null && !nama1.isEmpty()) {

					} else {
						nama1 = "110400";
					}
					pembimbing++;
				}

				progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + mahasiswaRequestTugasAkhir.getNama() + " ("
						+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

				XSSFCell cell = row.createCell(0);
				cell.setCellValue(id_smt);

				cell = row.createCell(1);
				cell.setCellValue(mahasiswaRequestTugasAkhir.getId().toString());

				cell = row.createCell(2);
				cell.setCellValue(dosen.getNidn());

				cell = row.createCell(3);
				cell.setCellValue(dosen.getNama());

				cell = row.createCell(4);
				cell.setCellValue(2);

				cell = row.createCell(5);
				if (key.toLowerCase().trim().contains("penguji")) {
					cell.setCellValue(penguji);
				} else {
					cell.setCellValue(pembimbing);
				}
				cell = row.createCell(6);
				cell.setCellValue(nama1);

				cell = row.createCell(7);
				cell.setCellValue(mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan() == null ? ""
						: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKodeEpsbed());
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
}
