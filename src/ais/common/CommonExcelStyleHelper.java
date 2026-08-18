package ais.common;

import java.awt.Color;

import org.apache.poi.ss.usermodel.IndexedColors;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.ss.usermodel.Font;
import org.zkoss.poi.ss.usermodel.Workbook;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide;
import org.zkoss.zss.model.Worksheet;

/**
 * Helper styling Excel/Spreadsheet agar Common.java tidak menampung detail teknis
 * style cell yang panjang.
 */
public final class CommonExcelStyleHelper {

	private CommonExcelStyleHelper() {
	}

	public static void setStyled(Worksheet sheet) {
		try {
			if (sheet == null || sheet.getWorkbook() == null || sheet.getRow(0) == null) {
				return;
			}
			Workbook workbook = sheet.getWorkbook();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_BOLD);

			CellStyle hlinkStyle = workbook.createCellStyle();
			hlinkStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
			hlinkStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
			hlinkStyle.setFont(font);
			hlinkStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			hlinkStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			hlinkStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			hlinkStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_NORMAL);

			CellStyle bodyStyle = workbook.createCellStyle();
			bodyStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
			bodyStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			bodyStyle.setFont(font);
			bodyStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			bodyStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			bodyStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			bodyStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			int lastCell = sheet.getRow(0).getLastCellNum();
			applyRowStyle(sheet, 0, lastCell, bodyStyle);
			applyRowStyle(sheet, 1, lastCell, bodyStyle);
			applyRowStyle(sheet, 2, lastCell, hlinkStyle);
			for (int row = 3; row <= sheet.getLastRowNum(); row++) {
				applyRowStyle(sheet, row, lastCell, bodyStyle);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonExcelStyleHelper.java:65");
		}
	}

	private static void applyRowStyle(Worksheet sheet, int rowIndex, int lastCell, CellStyle style) {
		try {
			if (sheet.getRow(rowIndex) == null) {
				return;
			}
			for (int i = 0; i < lastCell; i++) {
				try {
					if (sheet.getRow(rowIndex).getCell(i) != null) {
						sheet.getRow(rowIndex).getCell(i).setCellStyle(style);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonExcelStyleHelper.java:79");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonExcelStyleHelper.java:82");
		}
	}

	public static void setStyled(XSSFSheet sheet) {
		try {
			if (sheet == null || sheet.getWorkbook() == null || sheet.getRow(0) == null) {
				return;
			}
			XSSFWorkbook workbook = sheet.getWorkbook();
			XSSFFont hlinkFont = workbook.createFont();
			hlinkFont.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
			hlinkFont.setColor(new XSSFColor(Color.BLACK));

			XSSFCellStyle hlinkStyle = createHeaderStyle(workbook, hlinkFont, new XSSFColor(Color.LIGHT_GRAY));
			XSSFCellStyle redStyle = createHeaderStyle(workbook, hlinkFont, new XSSFColor(Color.RED));
			XSSFCellStyle greenStyle = createHeaderStyle(workbook, hlinkFont, new XSSFColor(Color.GREEN));
			XSSFCellStyle yellowStyle = createHeaderStyle(workbook, hlinkFont, new XSSFColor(Color.YELLOW));
			XSSFCellStyle bodyStyle = createBodyStyle(workbook);

			int lastCell = sheet.getRow(0).getLastCellNum();
			for (int i = 0; i < lastCell; i++) {
				try {
					if (sheet.getRow(0).getCell(i) != null) {
						sheet.getRow(0).getCell(i).setCellStyle(hlinkStyle);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonExcelStyleHelper.java:108");
				}
			}

			for (int row = 1; row <= sheet.getLastRowNum(); row++) {
				if (sheet.getRow(row) == null) {
					continue;
				}
				for (int i = 0; i < lastCell; i++) {
					try {
						XSSFCell cell = sheet.getRow(row).getCell(i);
						if (cell == null) {
							continue;
						}
						applyMarkerStyle(cell, bodyStyle, hlinkStyle, redStyle, greenStyle, yellowStyle);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonExcelStyleHelper.java:123");
					}
				}
			}

			for (int i = 0; i < lastCell; i++) {
				try {
					sheet.autoSizeColumn(i);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonExcelStyleHelper.java:131");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonExcelStyleHelper.java:134");
		}
	}

	private static XSSFCellStyle createHeaderStyle(XSSFWorkbook workbook, XSSFFont font, XSSFColor color) {
		XSSFCellStyle style = workbook.createCellStyle();
		style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		style.setFillForegroundColor(color);
		style.setFont(font);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.DOUBLE);
		setBlackBorder(style);
		return style;
	}

	private static XSSFCellStyle createBodyStyle(XSSFWorkbook workbook) {
		XSSFFont bodyFont = workbook.createFont();
		bodyFont.setBoldweight(XSSFFont.BOLDWEIGHT_NORMAL);
		bodyFont.setColor(new XSSFColor(Color.BLACK));
		XSSFCellStyle style = workbook.createCellStyle();
		style.setFont(bodyFont);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		setBlackBorder(style);
		return style;
	}

	private static void setBlackBorder(XSSFCellStyle style) {
		XSSFColor black = new XSSFColor(new Color(0, 0, 0));
		style.setBorderColor(BorderSide.TOP, black);
		style.setBorderColor(BorderSide.RIGHT, black);
		style.setBorderColor(BorderSide.BOTTOM, black);
		style.setBorderColor(BorderSide.LEFT, black);
	}

	private static void applyMarkerStyle(XSSFCell cell, XSSFCellStyle bodyStyle, XSSFCellStyle hlinkStyle,
			XSSFCellStyle redStyle, XSSFCellStyle greenStyle, XSSFCellStyle yellowStyle) {
		String value = Common.getCellContent(cell);
		if (value != null && value.startsWith("**RED")) {
			cell.setCellValue(value.substring(5));
			cell.setCellStyle(redStyle);
		} else if (value != null && value.startsWith("**GREEN")) {
			cell.setCellValue(value.substring(7));
			cell.setCellStyle(greenStyle);
		} else if (value != null && value.startsWith("**YELLOW")) {
			cell.setCellValue(value.substring(8));
			cell.setCellStyle(yellowStyle);
		} else if (value != null && value.startsWith("**")) {
			cell.setCellValue(value.substring(2));
			cell.setCellStyle(hlinkStyle);
		} else {
			cell.setCellStyle(bodyStyle);
		}
	}
}
