package ais.action.master.feeder.integrator.ekspor;

import java.io.File;
import java.io.FileOutputStream;
import java.awt.Color;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import ais.action.master.helper.AmbilDataKurikulumBanbox;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.common.newui.pekerjaan.PekerjaanRegistry;

/**
 * Penyusun berkas ekspor Matakuliah untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p>Badan penyusunnya DIPINDAHKAN dari {@code DownloadMatakuliah} — bukan disalin —
 * supaya pemetaan kolom yang ditetapkan PDDIKTI hanya ada di satu tempat.
 * Panel ZK lama kini memanggil kelas ini.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporMatakuliahFeeder {

    private EksporMatakuliahFeeder() { }

    /**
     * Susun berkas ekspor ke {@code tujuan}.
     *
     * @return jumlah baris data yang ditulis
     */
    public static int tulis(File tujuan, SaringanFeeder s, PekerjaanRegistry.Progres progres)
            throws Exception {
        if (s == null) s = new SaringanFeeder();
        if (progres == null) {
            progres = new PekerjaanRegistry.Progres() {
                public void lapor(int persen, String pesan) { }
            };
        }
		XSSFWorkbook workbook = new XSSFWorkbook();

		XSSFSheet sheet = workbook.createSheet("Matakuliah");
		sheet.setDefaultColumnWidth(18);

		XSSFRow rowhead = sheet.createRow((short) 0);

		rowhead.createCell(0).setCellValue("Kode MK");
		rowhead.createCell(1).setCellValue("Nama MK");
		rowhead.createCell(2).setCellValue("Jenis MK");
		rowhead.createCell(3).setCellValue("Kelompok MK");
		rowhead.createCell(4).setCellValue("SKS Tatap Muka");
		rowhead.createCell(5).setCellValue("SKS Praktek");
		rowhead.createCell(6).setCellValue("SKS Prak Lapaangan");
		rowhead.createCell(7).setCellValue("SKS Simulasi");
		rowhead.createCell(8).setCellValue("SAP ?");
		rowhead.createCell(9).setCellValue("Silabus ?");
		rowhead.createCell(10).setCellValue("Ada Bahan Ajar ?");
		rowhead.createCell(11).setCellValue("Ada Acara Praktek ?");
		rowhead.createCell(12).setCellValue("Ada Diktat ?");
		rowhead.createCell(13).setCellValue("Tgl Mulai Efektif");
		rowhead.createCell(14).setCellValue("Tgl Akhir Efektif");
		rowhead.createCell(15).setCellValue("Semester");
		rowhead.createCell(16).setCellValue("Kode Prodi");

		Session session = HibernateUtil.currentNativeSession();

		List<KurikulumPunyaMatakuliah> matakuliahs = session.createCriteria(KurikulumPunyaMatakuliah.class)

				.add(s.kurikulum == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kurikulum", s.kurikulum))

				.createAlias("matakuliah", "matakuliah").createAlias("kurikulum", "kurikulum")
				.createAlias("kurikulum.program", "program")

				.add(Restrictions
						.or(s.nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("matakuliah.kode", s.nama.trim(),
										MatchMode.ANYWHERE),

								s.nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("matakuliah.nama",
												s.nama.trim(), MatchMode.ANYWHERE)))

				.add(s.jurusan == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kurikulum.jurusan", s.jurusan))

				.createAlias("kurikulum.jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(s.fakultas == null
								? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqValue("jurusan.fakultas", s.fakultas, false))

				.add(s.program == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("program.nama", s.program))

				.add(s.semester == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", s.semester))

				.addOrder(Order.asc("semester")).addOrder(Order.asc("matakuliah.nama")).list();

		Map<String, KurikulumPunyaMatakuliah> kodes = new HashMap<String, KurikulumPunyaMatakuliah>();
		for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : matakuliahs) {
			Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();
			if (matakuliah != null && matakuliah.getKode() != null && !matakuliah.getKode().trim().isEmpty()) {
				kodes.put(matakuliah.getKode().trim(), kurikulumPunyaMatakuliah);
			}
		}

		int size = kodes.size();

		XSSFCellStyle notLocked = workbook.createCellStyle();
		notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

		int rowIndex = 1;
		for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kodes.values()) {
			Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();

			progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size), "Sedang memproses data " + matakuliah.toString() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

			XSSFRow row = sheet.createRow(rowIndex);

			XSSFCell cell = row.createCell(0);
			cell.setCellStyle(notLocked);
			cell.setCellValue(matakuliah.getKode());

			cell = row.createCell(1);
			cell.setCellStyle(notLocked);
			cell.setCellValue(matakuliah.getNama());

			row.createCell(2).setCellValue("A");
			row.createCell(3).setCellValue("");
			row.createCell(4).setCellValue(matakuliah.getSksDiskusi());
			row.createCell(5).setCellValue(matakuliah.getSksPraktek());
			row.createCell(6).setCellValue(matakuliah.getSksPraktekLapangan());
			row.createCell(7).setCellValue(matakuliah.getSksSimulasi());
			row.createCell(8).setCellValue(matakuliah.getAdaSap() ? 1 : 0);
			row.createCell(9).setCellValue(matakuliah.getAdaSilabus() ? 1 : 0);
			row.createCell(10).setCellValue(matakuliah.getAdaBahanAjar() ? 1 : 0);
			row.createCell(11).setCellValue(matakuliah.getAdaAcaraPraktek() ? 1 : 0);
			row.createCell(12).setCellValue(matakuliah.getAdaDiktat() ? 1 : 0);
			row.createCell(13).setCellValue(matakuliah.getTanggalMulai() == null ? ""
					: Common.databaseDateFormat.get().format(matakuliah.getTanggalMulai()));
			row.createCell(14).setCellValue(matakuliah.getTanggalSampai() == null ? ""
					: Common.databaseDateFormat.get().format(matakuliah.getTanggalSampai()));

			cell = row.createCell(15);
			cell.setCellStyle(notLocked);
			cell.setCellValue(kurikulumPunyaMatakuliah.getSemester());

			cell = row.createCell(16);
			cell.setCellStyle(notLocked);
			cell.setCellValue(matakuliah.getJurusan() == null ? "" : matakuliah.getJurusan().getKodeEpsbed());
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
