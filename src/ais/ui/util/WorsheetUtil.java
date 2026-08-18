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

public class WorsheetUtil {

	public static void copySheets(XSSFWorkbook newWorkbook, XSSFSheet newSheet, Sheet sheet) {
		copySheets(newWorkbook, newSheet, sheet, true);
	}

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
