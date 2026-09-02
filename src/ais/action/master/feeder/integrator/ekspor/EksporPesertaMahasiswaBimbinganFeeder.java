package ais.action.master.feeder.integrator.ekspor;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

import ais.common.Common;
import ais.common.newui.pekerjaan.PekerjaanRegistry;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;

/** Penyusun peserta mahasiswa Bimbingan Akademik/PA untuk template Feeder. */
public final class EksporPesertaMahasiswaBimbinganFeeder {

	private EksporPesertaMahasiswaBimbinganFeeder() {
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
		rowhead.createCell(1).setCellValue("ID AKTIVITAS");
		rowhead.createCell(2).setCellValue("NIM/NIPD");
		rowhead.createCell(3).setCellValue("Nama Mahasiswa");
		rowhead.createCell(4).setCellValue("Jenis Peran");
		rowhead.createCell(5).setCellValue("Kode Prodi Aktivitas");
		rowhead.createCell(6).setCellValue("Kode Prodi Mahasiswa");

		Session session = HibernateUtil.currentNativeSession();
		List<KrsMahasiswa> daftar = EksporBimbinganPaHelper.ambil(session, saring);
		int size = daftar.size();
		int rowIndex = 1;
		for (KrsMahasiswa krs : daftar) {
			Mahasiswa mahasiswa = krs.getMahasiswa();
			progres.lapor(size <= 0 ? 0 : (int) (rowIndex * 100.0 / size),
					"Sedang memproses data " + krs.getNama() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");
			XSSFRow row = sheet.createRow(rowIndex);
			XSSFCell cell = row.createCell(0);
			cell.setCellValue(EksporBimbinganPaHelper.idSemester(krs));
			cell = row.createCell(1);
			cell.setCellValue(krs.getId().toString());
			cell = row.createCell(2);
			cell.setCellValue(mahasiswa.getNim());
			cell = row.createCell(3);
			cell.setCellValue(mahasiswa.getNama());
			cell = row.createCell(4);
			cell.setCellValue(3);
			cell = row.createCell(5);
			cell.setCellValue(EksporBimbinganPaHelper.kodeProdi(krs));
			cell = row.createCell(6);
			cell.setCellValue(EksporBimbinganPaHelper.kodeProdi(krs));
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

