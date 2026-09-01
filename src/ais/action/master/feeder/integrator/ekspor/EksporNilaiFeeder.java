package ais.action.master.feeder.integrator.ekspor;

import java.io.File;
import java.io.FileOutputStream;
import java.awt.Color;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;
import java.util.TreeMap;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormatNilai;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.StatusPertemuan;
import ais.common.newui.pekerjaan.PekerjaanRegistry;

/**
 * Penyusun berkas ekspor Nilai untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan penyusunnya DIPINDAHKAN dari {@code DownloadNilai} — bukan disalin —
 * supaya pemetaan kolom yang ditetapkan PDDIKTI hanya ada di satu tempat.
 * Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporNilaiFeeder {

    private EksporNilaiFeeder() { }

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

		XSSFSheet sheet = workbook.createSheet("NILAI");
		sheet.setDefaultColumnWidth(18);

		XSSFRow rowhead = sheet.createRow((short) 0);

		rowhead.createCell(0).setCellValue("NIM");
		rowhead.createCell(1).setCellValue("Nama Mahasiswa");
		rowhead.createCell(2).setCellValue("Kode MK");
		rowhead.createCell(3).setCellValue("Mata Kuliah");
		rowhead.createCell(4).setCellValue("Semester");
		rowhead.createCell(5).setCellValue("Kelas");
		rowhead.createCell(6).setCellValue("Nilai Huruf");
		rowhead.createCell(7).setCellValue("Nilai Indeks");
		rowhead.createCell(8).setCellValue("Nilai Angka");
		rowhead.createCell(9).setCellValue("Kode Prodi");
		rowhead.createCell(10).setCellValue("Hapus?");
		rowhead.createCell(11).setCellValue("Dosen I");
		rowhead.createCell(12).setCellValue("Dosen II");
		rowhead.createCell(13).setCellValue("Dosen III");
		rowhead.createCell(14).setCellValue("Dosen IV");
		rowhead.createCell(15).setCellValue("Dosen V");
		rowhead.createCell(16).setCellValue("Dosen VI");
		rowhead.createCell(17).setCellValue("Dosen VII");
		rowhead.createCell(18).setCellValue("Dosen VIII");
		rowhead.createCell(19).setCellValue("Dosen IX");
		rowhead.createCell(20).setCellValue("Dosen X");

		Session session = HibernateUtil.currentNativeSession();

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (saring.status != null) {
			String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ saring.status.getId() + " and tahunakademik = '"
					+ saring.tahunAjaran + "' and semester%2="
					+ (saring.jenisSemester.equals(Perkuliahan.GANJIL) ? 1 : 0)
					+ ")";
			System.out.println("sql=>" + sql);
			criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)

				.add(saring.telahDinilai ? Restrictions.gt("totalNilai", 0.1)
						: Restrictions.sqlRestriction("true"))
				.add(saring.belumDinilai ? Restrictions.lt("totalNilai", 0.1)
						: Restrictions.sqlRestriction("true"))

				.add(criteriaStatus)

				.add(saring.tahunAjaran.isEmpty()
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik",
										saring.tahunAjaran))

				.add(saring.jenisSemester.isEmpty()
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.sqlRestriction("this_.semester % 2 = " + (saring.jenisSemester.equals(Perkuliahan.GANJIL) ? "1" : "0")))

				.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN)
				.createAlias("perkuliahan", "perkuliahan", Criteria.INNER_JOIN)

				.add(saring.masaPerkuliahan == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("perkuliahan.masaPerkuliahan",
								saring.masaPerkuliahan))

				.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)

				.add(saring.kelas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("perkuliahan.kelas", saring.kelas, MatchMode.ANYWHERE))

				.add(saring.kodeMatakuliah.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.or(
										Restrictions.ilike("matakuliahKonversi.nama",
												saring.kodeMatakuliah.trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("matakuliah.nama", saring.kodeMatakuliah.trim(),
												MatchMode.ANYWHERE)),
								Restrictions.or(
										Restrictions.ilike("matakuliahKonversi.kode",
												saring.kodeMatakuliah.trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("matakuliah.kode", saring.kodeMatakuliah.trim(),
												MatchMode.ANYWHERE))))

				.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))

				.add(saring.semesterKe == null || saring.semesterKe.equals(-1) ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("semester", saring.semesterKe))

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

		TreeMap<Long, String> formats = new TreeMap<Long, String>();
		for (Detailperkuliahan detailperkuliahanLain : detailperkuliahans) {
			try {
				String[] s = StringUtils.split(detailperkuliahanLain.getDetailNilai(), ";");
				for (String ss : s) {
					String[] sss = StringUtils.split(ss, ",");
					Long idFormatNilai = Long.parseLong(sss[0].trim());
					StatusPertemuan statusPertemuan = (StatusPertemuan) ConstantValues
							.ambil(StatusPertemuan.class.getName(), idFormatNilai);
					if (statusPertemuan != null) {

						String nama = FormatNilai.ambilNama(statusPertemuan,
								detailperkuliahanLain.getPerkuliahan());

						formats.put(idFormatNilai, idFormatNilai + "-" + nama);
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadNilai.java:542");
				// TODO: handle exception
			}
		}

		int indexCol = 1;
		for (Long idStatusFormat : formats.keySet()) {
			try {
				String nama = formats.get(idStatusFormat);
				if (nama != null) {
					rowhead.createCell(20 + indexCol).setCellValue(nama);
					indexCol++;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadNilai.java:555");

			}
		}

		XSSFCellStyle notLocked = workbook.createCellStyle();
		notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

		int rowIndex = 1;
		for (Detailperkuliahan detailperkuliahanLain : detailperkuliahans) {

			progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + detailperkuliahanLain.toString() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

			createData(session, sheet, rowIndex, detailperkuliahanLain,
					(MasaPerkuliahan) saring.masaPerkuliahan, notLocked, false,
					formats);
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

	public static void createData(Session session, XSSFSheet sheet, int rowIndex, Detailperkuliahan detailperkuliahan,
			MasaPerkuliahan masaPerkuliahan, XSSFCellStyle notLocked, boolean hapus, TreeMap<Long, String> formats) {

		Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() == null
				? (detailperkuliahan.getPerkuliahan() == null ? null
						: detailperkuliahan.getPerkuliahan().getMatakuliah())
				: detailperkuliahan.getMatakuliahKonversi();
		Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
		if (matakuliah != null && detailperkuliahan.getTahunAkademik() != null) {

			XSSFRow row = sheet.createRow(rowIndex);

			String id_smt = masaPerkuliahan != null ? masaPerkuliahan.getNama()
					: detailperkuliahan.getTahunAkademik().split("/")[0] + (detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek()
									.equals(Perkuliahan.SEMESTER_PENDEK) ? "3"
											: (detailperkuliahan.getSemester() % 2 == 0 ? "2" : "1"));

			if (mahasiswa.getSemesterMulai().equals(Perkuliahan.GENAP)) {
				id_smt = masaPerkuliahan != null ? masaPerkuliahan.getNama()
						: detailperkuliahan.getTahunAkademik().split("/")[0]
								+ (detailperkuliahan.getPerkuliahan() != null
										&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
										&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek()
												.equals(Perkuliahan.SEMESTER_PENDEK) ? "3"
														: (detailperkuliahan.getSemester() % 2 == 0 ? "1" : "2"));
			}

			XSSFCell cell = row.createCell(0);
			cell.setCellStyle(notLocked);
			cell.setCellValue(mahasiswa.getNim());

			cell = row.createCell(1);
			cell.setCellValue(mahasiswa.getNama());

			cell = row.createCell(2);
			cell.setCellStyle(notLocked);
			cell.setCellValue(matakuliah.getKode());

			cell = row.createCell(3);
			cell.setCellValue(matakuliah.getNama());

			cell = row.createCell(4);
			cell.setCellStyle(notLocked);
			cell.setCellValue(id_smt);

			cell = row.createCell(5);
			cell.setCellStyle(notLocked);
			cell.setCellValue(
					detailperkuliahan.getPerkuliahan() == null ? "" : detailperkuliahan.getPerkuliahan().getKelas());

			cell = row.createCell(6);
			cell.setCellStyle(notLocked);
			cell.setCellValue(detailperkuliahan.getNilaiHuruf());

			cell = row.createCell(7);
			cell.setCellValue(detailperkuliahan.getTotalIP());

			cell = row.createCell(8);
			cell.setCellStyle(notLocked);
			cell.setCellValue(detailperkuliahan.getTotalNilai());

			cell = row.createCell(9);
			cell.setCellStyle(notLocked);
			String kodeProdi = detailperkuliahan.getMatakuliahKonversi() != null
					&& detailperkuliahan.getMatakuliahKonversi().getJurusan() != null
					? detailperkuliahan.getMatakuliahKonversi().getJurusan().getKodeEpsbed()
					: detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getJurusan() != null
							? detailperkuliahan.getPerkuliahan().getJurusan().getKodeEpsbed() : "";
			cell.setCellValue(kodeProdi == null ? "" : kodeProdi);

			cell = row.createCell(10);
			cell.setCellStyle(notLocked);
			cell.setCellValue(hapus);

			Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();

			cell = row.createCell(11);
			cell.setCellStyle(notLocked);
			cell.setCellValue(
					perkuliahan == null || perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().toString());

			cell = row.createCell(12);
			cell.setCellStyle(notLocked);
			cell.setCellValue(
					perkuliahan == null || perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().toString());

			cell = row.createCell(13);
			cell.setCellStyle(notLocked);
			cell.setCellValue(
					perkuliahan == null || perkuliahan.getDosen3() == null ? "" : perkuliahan.getDosen3().toString());

			cell = row.createCell(14);
			cell.setCellStyle(notLocked);
			cell.setCellValue(
					perkuliahan == null || perkuliahan.getDosen4() == null ? "" : perkuliahan.getDosen4().toString());

			cell = row.createCell(15);
			cell.setCellStyle(notLocked);
			cell.setCellValue(
					perkuliahan == null || perkuliahan.getDosen5() == null ? "" : perkuliahan.getDosen5().toString());

			cell = row.createCell(16);
			cell.setCellStyle(notLocked);
			cell.setCellValue(
					perkuliahan == null || perkuliahan.getDosen6() == null ? "" : perkuliahan.getDosen6().toString());

			cell = row.createCell(17);
			cell.setCellStyle(notLocked);
			cell.setCellValue(
					perkuliahan == null || perkuliahan.getDosen7() == null ? "" : perkuliahan.getDosen7().toString());

			cell = row.createCell(18);
			cell.setCellStyle(notLocked);
			cell.setCellValue(
					perkuliahan == null || perkuliahan.getDosen8() == null ? "" : perkuliahan.getDosen8().toString());

			cell = row.createCell(19);
			cell.setCellStyle(notLocked);
			cell.setCellValue(
					perkuliahan == null || perkuliahan.getDosen9() == null ? "" : perkuliahan.getDosen9().toString());

			cell = row.createCell(20);
			cell.setCellStyle(notLocked);
			cell.setCellValue(
					perkuliahan == null || perkuliahan.getDosen10() == null ? "" : perkuliahan.getDosen10().toString());

			if (formats != null) {
				String[] s = StringUtils.split(detailperkuliahan.getDetailNilai(), ";");
				int indexCol = 1;
				for (Long idStatusFormat : formats.keySet()) {
					try {
						String nama = formats.get(idStatusFormat);
						if (nama != null) {
							Double n = null;
							for (String ss : s) {
								try {
									String[] sss = StringUtils.split(ss, ",");
									Long idFormatNilai = Long.parseLong(sss[0].trim());
									if (idStatusFormat.equals(idFormatNilai)) {
										n = Double.parseDouble(sss[5]);
										break;
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadNilai.java:748");
									// TODO: handle exception
								}
							}

							cell = row.createCell(20 + indexCol);
							cell.setCellStyle(notLocked);
							cell.setCellValue(n == null ? "n/a" : Common.numberFormat.get().format(n));

							indexCol++;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadNilai.java:759");

					}
				}
			}

			if (hapus) {
				Common.refreshDelete(detailperkuliahan);
			}

		}
	}
}
