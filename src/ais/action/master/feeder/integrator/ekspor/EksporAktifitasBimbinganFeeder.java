package ais.action.master.feeder.integrator.ekspor;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

import ais.action.master.feeder.util.FeederJSONImport;
import ais.common.Common;
import ais.common.newui.pekerjaan.PekerjaanRegistry;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KrsMahasiswa;

/** Penyusun aktivitas Bimbingan Akademik/PA untuk template Feeder. */
public final class EksporAktifitasBimbinganFeeder {

	private EksporAktifitasBimbinganFeeder() {
	}

	public static int tulis(File tujuan, SaringanFeeder saring, PekerjaanRegistry.Progres progres)
			throws Exception {
		if (saring == null) saring = new SaringanFeeder();
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
		List<KrsMahasiswa> daftar = EksporBimbinganPaHelper.ambil(session, saring);
		int size = daftar.size();
		int rowIndex = 1;
		String idJenis = FeederJSONImport.JENIS_KEGIATAN.get("Bimbingan akademis");

		for (KrsMahasiswa krs : daftar) {
			progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size),
					"Sedang memproses data " + krs.getNama() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");
			XSSFRow row = sheet.createRow(rowIndex);
			XSSFCell cell = row.createCell(0);
			cell.setCellValue(EksporBimbinganPaHelper.idSemester(krs));
			cell = row.createCell(1);
			cell.setCellValue(idJenis == null ? "7" : idJenis);
			cell = row.createCell(2);
			cell.setCellValue(EksporBimbinganPaHelper.judul(krs));
			cell = row.createCell(3);
			cell.setCellValue("");
			cell = row.createCell(4);
			cell.setCellValue(krs.getNoSk());
			cell = row.createCell(5);
			cell.setCellValue(krs.getTglSk() == null ? ""
					: Common.databaseDateFormat.get().format(krs.getTglSk()));
			cell = row.createCell(6);
			cell.setCellValue("1");
			cell = row.createCell(7);
			cell.setCellValue(krs.getId().toString());
			cell = row.createCell(8);
			cell.setCellValue(EksporBimbinganPaHelper.kodeProdi(krs));
			cell = row.createCell(9);
			cell.setCellValue("1");
			cell = row.createCell(10);
			cell.setCellValue(krs.getTanggalAwalBimbingan() == null ? ""
					: Common.databaseDateFormat.get().format(krs.getTanggalAwalBimbingan()));
			row.createCell(11).setCellValue("");
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

