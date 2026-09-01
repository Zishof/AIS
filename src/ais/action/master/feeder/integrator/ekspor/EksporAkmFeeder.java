package ais.action.master.feeder.integrator.ekspor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.common.newui.pekerjaan.PekerjaanRegistry;

/**
 * Penyusun berkas ekspor AKM untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan penyusunnya DIPINDAHKAN dari {@code DownloadAkm} — bukan disalin —
 * supaya pemetaan kolom yang ditetapkan PDDIKTI hanya ada di satu tempat.
 * Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporAkmFeeder {

    private EksporAkmFeeder() { }

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

		XSSFSheet sheet = workbook.createSheet("AKM");
		sheet.setDefaultColumnWidth(18);

		XSSFRow rowhead = sheet.createRow((short) 0);

		rowhead.createCell(0).setCellValue("NIM");
		rowhead.createCell(1).setCellValue("Nama Mahasiswa");
		rowhead.createCell(2).setCellValue("Semester");
		rowhead.createCell(3).setCellValue("SKS");
		rowhead.createCell(4).setCellValue("IP Semester");
		rowhead.createCell(5).setCellValue("SKS Kumulatif");
		rowhead.createCell(6).setCellValue("IP Kumulatif");
		rowhead.createCell(7).setCellValue("Status");
		rowhead.createCell(8).setCellValue("Kode Prodi");
		rowhead.createCell(9).setCellValue("Biaya Kuliah");

		Session session = HibernateUtil.currentNativeSession();

		List<Mahasiswa> mahasiswas = ConstantValues
				.simpleList(session.createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.setMaxResults(saring.sampai == null ? 100 : saring.sampai)
						.setFirstResult(saring.mulai == null ? 0 : saring.mulai)

						.add(saring.kelas != null && !saring.kelas.trim().isEmpty() ? Restrictions.ilike("kelas", saring.kelas.trim(),
								MatchMode.EXACT) : Restrictions.sqlRestriction("true"))

						.add(Restrictions.and(
								saring.nim.trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("nim", saring.nim.trim(),
												MatchMode.ANYWHERE),

								saring.nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("nama", saring.nama.trim(),
												MatchMode.ANYWHERE)))

						.add(saring.jurusan == null
								|| saring.jurusan == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("jurusan", saring.jurusan))

						.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

						.add(saring.fakultas == null
								|| saring.fakultas == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqValue("jurusan.fakultas", saring.fakultas, false))

						.add(saring.program == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("program",
												saring.program))

						.add(saring.angkatan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunangkatan", saring.angkatan))

						.addOrder(Order.asc("nim")), Mahasiswa.class);

		int size = mahasiswas.size();

		int rowIndex = 1;
		for (Mahasiswa mahasiswa : mahasiswas) {

			try {
				Integer tahun = Integer.parseInt(StringUtils.split(saring.tahunAkademik, "/")[0]);

				Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(),
						saring.semester.equals(Perkuliahan.SP) ? Perkuliahan.GENAP : saring.semester,
						mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
				HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, saring.tahunAkademik,
						currentSemester);

				if (saring.status == null || (saring.status != null
						&& historyStatusMahasiswa != null && historyStatusMahasiswa.getStatusMahasiswa() != null
						&& saring.status.getId()
								.equals(historyStatusMahasiswa.getStatusMahasiswa().getId()))) {

					progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + mahasiswa.toString() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

					XSSFRow row = sheet.createRow(rowIndex);

					String id_smt = saring.tahunAkademik.split("/")[0] + (saring.semester.equals(Perkuliahan.SP) ? "3"
							: saring.semester.equals(Perkuliahan.GENAP) ? "2" : "1");

					XSSFCell cell = row.createCell(0);
					cell.setCellValue(mahasiswa.getNim());

					cell = row.createCell(1);
					cell.setCellValue(mahasiswa.getNama());

					cell = row.createCell(2);
					cell.setCellValue(id_smt);

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, currentSemester, null,
							null, saring.hitungUlang);

					cell = row.createCell(3);
					cell.setCellValue(krsMahasiswa.getSksYangDiambil());

					cell = row.createCell(4);
					cell.setCellValue(krsMahasiswa.getIps());

					cell = row.createCell(5);
					cell.setCellValue(krsMahasiswa.getSksk());

					cell = row.createCell(6);
					cell.setCellValue(krsMahasiswa.getIpk());

					cell = row.createCell(7);

					if (historyStatusMahasiswa != null
							&& historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed() != null
							&& (historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
									.equalsIgnoreCase("A")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("C")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("D")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("L")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("P")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("N")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("G")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("X")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("K"))

					) {
						cell.setCellValue(historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed());
					} else {
						cell.setCellValue("X");
					}

					row.createCell(8).setCellValue(mahasiswa.getJurusan().getKodeEpsbed());

					try {
						@SuppressWarnings("rawtypes")
						Collection detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(
								mahasiswa, currentSemester, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, false);

						int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session,
								mahasiswa, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, currentSemester,
								detailBiayas, false, false);

						if (countPengaturanBulanan > 0) {

							detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa,
									currentSemester, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA,
									countPengaturanBulanan > 0 ? "-1" : null, true, false);

						}

						if (!detailBiayas.isEmpty()) {
							Kegiatan kegiatan = mahasiswa.ambilKegiatans(currentSemester,
									ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
							Collection<DetailKegiatan> detailKegiatans = kegiatan == null
									|| kegiatan.getId() == null ? null : kegiatan.ambilDetailKegiatan(false);
							Double biaya = 0.0;
							for (Object o : detailBiayas) {
								if (o instanceof PengaturanPembayaranBulanan) {
									PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
									Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans,
											mahasiswa, currentSemester, pengaturanPembayaranBulanan);
									biaya += jumlah;
								} else if (o instanceof DetailBiaya) {
									DetailBiaya detailBiaya = (DetailBiaya) o;

									Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, false);
									biaya += jumlah;
								}
							}
							row.createCell(9).setCellValue(biaya);
						} else {
							row.createCell(9).setCellValue(0.0);
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/DownloadAkm.java:559");
						row.createCell(9).setCellValue(0.0);
					}

					rowIndex++;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/DownloadAkm.java:566");
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
