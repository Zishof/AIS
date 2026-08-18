package ais.action.master.obe;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.zkoss.zul.Filedownload;

import ais.common.Common;

/**
 * <h3>PikobeExportHelper — unduh data tabel ke berkas Excel (.xlsx)</h3>
 *
 * <p>Membuat berkas Excel dari header + baris data yang sama persis dengan yang sedang
 * ditampilkan di grid, lalu mengirimkannya sebagai unduhan ke pengguna. Dipakai bersama
 * oleh tiap tab PIKOBE (reuse) sehingga semua tombol "Unduh Excel" berperilaku seragam
 * dan mudah dirawat.</p>
 *
 * <p>Memakai Apache/zkoss POI ({@code org.zkoss.poi}) — TANPA library chart. Bersifat
 * fail-safe: kegagalan ditampilkan ke admin, tidak melempar ke pemanggil. Gaya Java
 * 1.6/1.7.</p>
 */
public final class PikobeExportHelper {

	private static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private PikobeExportHelper() {
	}

	/**
	 * Bangun &amp; kirim berkas Excel.
	 *
	 * @param namaFile  nama berkas tanpa ekstensi (mis. "OBE_CPL_Informatika")
	 * @param judulSheet nama sheet
	 * @param header    judul kolom
	 * @param baris     daftar baris; tiap baris adalah array String sepanjang header
	 */
	public static void unduhExcel(String namaFile, String judulSheet, String[] header, List<String[]> baris) {
		try {
			org.zkoss.poi.ss.usermodel.Workbook wb = new org.zkoss.poi.xssf.usermodel.XSSFWorkbook();
			org.zkoss.poi.ss.usermodel.Sheet sheet = wb.createSheet(amanNamaSheet(judulSheet));

			int r = 0;
			org.zkoss.poi.ss.usermodel.Row hr = sheet.createRow(r++);
			if (header != null) {
				for (int c = 0; c < header.length; c++) {
					hr.createCell(c).setCellValue(header[c] == null ? "" : header[c]);
					sheet.setColumnWidth(c, 256 * 28);
				}
			}
			if (baris != null) {
				for (int i = 0; i < baris.size(); i++) {
					String[] data = baris.get(i);
					org.zkoss.poi.ss.usermodel.Row dr = sheet.createRow(r++);
					if (data != null) {
						for (int c = 0; c < data.length; c++) {
							dr.createCell(c).setCellValue(data[c] == null ? "" : data[c]);
						}
					}
				}
			}
			try {
				sheet.createFreezePane(0, 1); // bekukan baris judul
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/obe/PikobeExportHelper.java:63");
			}

			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			wb.write(bout);
			Filedownload.save(bout.toByteArray(), MIME_XLSX, amanNamaFile(namaFile) + ".xlsx");
		} catch (Throwable t) {
			try {
				Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/obe/PikobeExportHelper.java:72");
			}
		}
	}

	private static String amanNamaSheet(String v) {
		if (v == null || v.trim().length() == 0) {
			return "Data";
		}
		String s = v.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
		if (s.length() > 31) {
			s = s.substring(0, 31);
		}
		return s.length() == 0 ? "Data" : s;
	}

	private static String amanNamaFile(String v) {
		if (v == null || v.trim().length() == 0) {
			return "data";
		}
		return v.replaceAll("[\\\\/?*\\[\\]:\\s]+", "_");
	}
}
