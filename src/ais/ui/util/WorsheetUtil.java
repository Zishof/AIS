package ais.ui.util;

import java.util.HashMap;
import java.util.Map;

import org.zkoss.poi.ss.usermodel.Row;
import org.zkoss.poi.ss.usermodel.Sheet;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Utilitas untuk menyalin isi satu {@code Sheet} (worksheet) Excel ke {@code Sheet} lain
 * memakai API POI/ZK-POI ({@code org.zkoss.poi}), dipakai saat AIS perlu menggabungkan beberapa
 * worksheet sumber ke dalam satu workbook tujuan (mis. menggabungkan rekap dari beberapa
 * sheet/berkas menjadi satu laporan Excel gabungan). Catatan: nama kelas ({@code WorsheetUtil})
 * kemungkinan salah ketik dari "Worksheet", namun dipertahankan apa adanya sesuai kode sumber
 * — tidak diubah sebagai bagian dari pekerjaan dokumentasi ini.
 *
 * <p>
 * Penyalinan bersifat aditif: baris-baris dari {@code sheet} sumber disisipkan mulai dari baris
 * pertama yang kosong SETELAH baris terakhir yang sudah ada di {@code newSheet} (dengan satu
 * baris pemisah kosong di antaranya), sehingga memanggil {@link #copySheets(XSSFWorkbook,
 * XSSFSheet, Sheet)} berulang kali dengan {@code newSheet} yang sama akan menumpuk isi beberapa
 * sheet sumber secara berurutan ke bawah pada satu sheet tujuan, bukan menimpanya.
 * </p>
 */
public class WorsheetUtil {

	/**
	 * Menyalin seluruh baris dan lebar kolom dari {@code sheet} sumber ke {@code newSheet}
	 * tujuan, DENGAN menyalin style sel (format tampilan) — lihat {@link #copySheets(
	 * XSSFWorkbook, XSSFSheet, Sheet, boolean)} untuk detail penempatan baris. Ini adalah
	 * titik masuk publik satu-satunya untuk operasi penggabungan sheet di kelas ini.
	 *
	 * @param newWorkbook workbook tujuan (dipakai untuk membuat {@link XSSFCellStyle} baru saat
	 *                    menyalin style)
	 * @param newSheet    sheet tujuan tempat baris-baris sumber disisipkan
	 * @param sheet       sheet sumber yang akan disalin isinya
	 */
	public static void copySheets(XSSFWorkbook newWorkbook, XSSFSheet newSheet, Sheet sheet) {
		copySheets(newWorkbook, newSheet, sheet, true);
	}

	/**
	 * Implementasi penyalinan sheet: menghitung baris awal penyisipan di {@code newSheet}
	 * (satu baris setelah baris terakhir yang sudah terisi, sebagai pemisah visual), lalu
	 * menyalin setiap baris sumber (termasuk baris kosong dalam rentang) satu per satu lewat
	 * {@link #copyRow}, dan terakhir menyamakan lebar setiap kolom tujuan dengan lebar kolom
	 * sumber yang bersesuaian.
	 *
	 * @param newWorkbook workbook tujuan, diteruskan ke {@link #copyRow} untuk pembuatan style
	 * @param newSheet    sheet tujuan
	 * @param sheet       sheet sumber
	 * @param copyStyle   bila {@code true}, style sel ikut disalin (memakai cache {@code
	 *                    styleMap} agar style yang sama tidak dibuat berulang); bila
	 *                    {@code false}, hanya nilai sel yang disalin tanpa style
	 */
	private static void copySheets(XSSFWorkbook newWorkbook, XSSFSheet newSheet, Sheet sheet, boolean copyStyle) {
		int newRownumber = newSheet.getLastRowNum() + 1 + 1;
		int maxColumnNum = 0;
		Map<Integer, XSSFCellStyle> styleMap = (copyStyle) ? new HashMap<Integer, XSSFCellStyle>() : null;

		for (int i = sheet.getFirstRowNum(); i <= (sheet.getLastRowNum() + 1); i++) {
			Row srcRow = sheet.getRow(i);
			XSSFRow destRow = newSheet.createRow(i + newRownumber);
			if (srcRow != null) {
				copyRow(newWorkbook, sheet, newSheet, srcRow, destRow, styleMap);
				if (srcRow.getLastCellNum() > maxColumnNum) {
					maxColumnNum = srcRow.getLastCellNum();
				}
			}
		}
		for (int i = 0; i <= maxColumnNum; i++) {
			newSheet.setColumnWidth(i, sheet.getColumnWidth(i));
		}
	}

	/**
	 * Menyalin satu baris sumber ({@code srcRow}) ke satu baris tujuan ({@code destRow}):
	 * menyamakan tinggi baris, lalu menyalin setiap sel dalam rentang kolom baris sumber lewat
	 * {@link #copyCell}. Sel tujuan dibuat terlebih dahulu bila belum ada pada indeks kolom
	 * yang bersangkutan.
	 *
	 * @param newWorkbook workbook tujuan, diteruskan ke {@link #copyCell} untuk pembuatan style
	 * @param sheet       sheet sumber (tidak dipakai langsung, diteruskan untuk konsistensi
	 *                    signature/pemanggilan di masa depan)
	 * @param destSheet   sheet tujuan (tidak dipakai langsung dalam badan method)
	 * @param srcRow      baris sumber yang akan disalin
	 * @param destRow     baris tujuan tempat sel-sel disalin
	 * @param styleMap    cache style per hash-code style sel sumber, diteruskan ke
	 *                    {@link #copyCell}; boleh {@code null} bila style tidak perlu disalin
	 */
	public static void copyRow(XSSFWorkbook newWorkbook, Sheet sheet, XSSFSheet destSheet, Row srcRow, XSSFRow destRow,
			Map<Integer, XSSFCellStyle> styleMap) {
		destRow.setHeight(srcRow.getHeight());
		for (int j = srcRow.getFirstCellNum(); j <= srcRow.getLastCellNum(); j++) {
			XSSFCell oldCell = (XSSFCell) srcRow.getCell(j);
			XSSFCell newCell = destRow.getCell(j);
			if (oldCell != null) {
				if (newCell == null) {
					newCell = destRow.createCell(j);
				}
				copyCell(newWorkbook, oldCell, newCell, styleMap);
			}
		}
	}

	/**
	 * Menyalin nilai (dan opsional style) satu sel sumber ({@code oldCell}) ke satu sel tujuan
	 * ({@code newCell}). Style disalin lewat cache {@code styleMap} berbasis hash-code style
	 * sel sumber: bila style dengan hash-code yang sama sudah pernah dibuat di workbook tujuan,
	 * style yang sudah ada dipakai ulang (menghindari pembengkakan jumlah style unik pada
	 * workbook tujuan); bila belum ada, style baru dibuat lewat {@code newWorkbook.createCellStyle()}
	 * dan di-clone dari style sel sumber. Nilai sel disalin sesuai tipe selnya (string, numerik,
	 * kosong, boolean, error, atau formula); tipe yang tidak dikenali diabaikan (tidak menyalin
	 * apa pun).
	 *
	 * @param newWorkbook workbook tujuan, dipakai untuk membuat {@link XSSFCellStyle} baru bila
	 *                    diperlukan
	 * @param oldCell     sel sumber yang akan disalin
	 * @param newCell     sel tujuan yang akan diisi
	 * @param styleMap    cache style per hash-code style sel sumber; bila {@code null}, style
	 *                    sel sumber TIDAK disalin (hanya nilai sel yang disalin)
	 */
	public static void copyCell(XSSFWorkbook newWorkbook, XSSFCell oldCell, XSSFCell newCell,
			Map<Integer, XSSFCellStyle> styleMap) {
		if (styleMap != null) {
			int stHashCode = oldCell.getCellStyle().hashCode();
			XSSFCellStyle newCellStyle = styleMap.get(stHashCode);
			if (newCellStyle == null) {
				newCellStyle = newWorkbook.createCellStyle();
				newCellStyle.cloneStyleFrom(oldCell.getCellStyle());
				styleMap.put(stHashCode, newCellStyle);
			}
			newCell.setCellStyle(newCellStyle);
		}
		switch (oldCell.getCellType()) {
		case XSSFCell.CELL_TYPE_STRING:
			newCell.setCellValue(oldCell.getRichStringCellValue());
			break;
		case XSSFCell.CELL_TYPE_NUMERIC:
			newCell.setCellValue(oldCell.getNumericCellValue());
			break;
		case XSSFCell.CELL_TYPE_BLANK:
			newCell.setCellType(XSSFCell.CELL_TYPE_BLANK);
			break;
		case XSSFCell.CELL_TYPE_BOOLEAN:
			newCell.setCellValue(oldCell.getBooleanCellValue());
			break;
		case XSSFCell.CELL_TYPE_ERROR:
			newCell.setCellErrorValue(oldCell.getErrorCellValue());
			break;
		case XSSFCell.CELL_TYPE_FORMULA:
			newCell.setCellFormula(oldCell.getCellFormula());
			break;
		default:
			break;
		}
	}
}
