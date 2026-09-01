package ais.action.master.feeder.integrator.ekspor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.common.newui.pekerjaan.PekerjaanRegistry;

/**
 * Penyusun berkas ekspor History untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan penyusunnya DIPINDAHKAN dari {@code DownloadHistory} — bukan disalin —
 * supaya pemetaan kolom yang ditetapkan PDDIKTI hanya ada di satu tempat.
 * Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporHistoryFeeder {

    private EksporHistoryFeeder() { }

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

		XSSFSheet sheet = workbook.createSheet("Mahasiswa");
		sheet.setDefaultColumnWidth(18);

		XSSFRow rowhead = sheet.createRow((short) 0);

		rowhead.createCell(0).setCellValue("NIM");
		rowhead.createCell(1).setCellValue("Mulai Semester");
		rowhead.createCell(2).setCellValue("Jenis Pendaftaran");
		rowhead.createCell(3).setCellValue("Jalur Pendaftaran");
		rowhead.createCell(4).setCellValue("Tanggal Pendaftaran");
		rowhead.createCell(5).setCellValue("SKS Diakui");
		rowhead.createCell(6).setCellValue("Asal Perguruan Tinggi");
		rowhead.createCell(7).setCellValue("Asal Program Studi");
		rowhead.createCell(8).setCellValue("Kode Prodi");

		Session session = HibernateUtil.currentNativeSession();

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (saring.status != null) {
			String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ saring.status.getId() + " and tahunakademik = '"
					+ Common.getCurrentTahunAkademik() + "' and semester%2="
					+ (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
			System.out.println("sql=>" + sql);
			criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(criteriaStatus)

				.add(saring.kelas != null && !saring.kelas.trim().isEmpty()
						? Restrictions.ilike("kelas", saring.kelas.trim(), MatchMode.EXACT)
						: Restrictions.sqlRestriction("true"))

				.add(Restrictions.and(
						saring.nim.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("nim", saring.nim.trim(), MatchMode.ANYWHERE),

						saring.nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("nama", saring.nama.trim(),
										MatchMode.ANYWHERE)))

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

				.add(saring.angkatan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", saring.angkatan))

				.addOrder(Order.asc("nim")).list();

		int size = mahasiswas.size();

		int rowIndex = 1;
		for (Mahasiswa mahasiswa : mahasiswas) {

			progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + mahasiswa.toString() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

			Integer tahun = Integer.parseInt(StringUtils.split(saring.tahunAkademik, "/")[0]);

			Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(), saring.semester,
					mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, currentSemester, null, null,
					false);

			XSSFRow row = sheet.createRow(rowIndex);

			row.createCell(0).setCellValue(mahasiswa.getNim());
			row.createCell(1).setCellValue(mahasiswa.getTahunangkatan()
					+ (mahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL) ? "1" : "2"));
			row.createCell(2)
					.setCellValue(mahasiswa.getStatusAwalMahasiswa() == null
							|| mahasiswa.getStatusAwalMahasiswa().getFeeder() == null ? ""
									: mahasiswa.getStatusAwalMahasiswa().getFeeder().toString());
			row.createCell(3).setCellValue(
					mahasiswa.getJenisSeleksi() == null || mahasiswa.getJenisSeleksi().getKode() == null ? ""
							: mahasiswa.getJenisSeleksi().getKode().trim());
			row.createCell(4).setCellValue(mahasiswa.getTanggalMasuk() == null ? ""
					: Common.databaseDateFormat.get().format(mahasiswa.getTanggalMasuk()));
			row.createCell(5).setCellValue(krsMahasiswa.getSksk());
			row.createCell(6).setCellValue(mahasiswa.getPindahanDariKampus());
			row.createCell(7).setCellValue(mahasiswa.getNamaProdiPindah());
			row.createCell(8).setCellValue(mahasiswa.getJurusan().getKodeEpsbed());

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
