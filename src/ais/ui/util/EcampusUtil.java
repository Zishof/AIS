package ais.ui.util;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.IndexedColors;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.ss.usermodel.Font;
import org.zkoss.poi.ss.usermodel.Workbook;
import org.zkoss.poi.ss.util.CellRangeAddress;
import org.zkoss.poi.util.IOUtils;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFClientAnchor;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFDrawing;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;

import ais.action.master.surat.util.SuratUtil;
import ais.common.Common;
import ais.database.model.file.LampiranLain;

public class EcampusUtil {

	@SuppressWarnings("rawtypes")
	public static void tampilkan(List<List> datas, Spreadsheet excelku) throws Exception {
		tampilkan(datas, excelku, true);
	}

	@SuppressWarnings("rawtypes")
	public static void tampilkan(List<List> datas, Spreadsheet excelku, boolean auto) throws Exception {

		String fn = Common.REAL_PATH + "/tmp/rekap_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx";

		int mulaiRow = 4;
		int lebar = 1;
		for (int rowIndex = mulaiRow; rowIndex < datas.size() + mulaiRow; rowIndex++) {
			List sub = datas.get(rowIndex - mulaiRow);
			if (sub.size() > lebar) {
				lebar = sub.size();
			}
		}

		XSSFWorkbook workbook = new XSSFWorkbook();

		XSSFFont hlink_font = workbook.createFont();
		hlink_font.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
		hlink_font.setColor(new XSSFColor(Color.BLACK));

		XSSFCellStyle hlink_style = workbook.createCellStyle();
		hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
		hlink_style.setFont(hlink_font);

		hlink_style.setBorderLeft(BorderStyle.THIN);
		hlink_style.setBorderTop(BorderStyle.THIN);
		hlink_style.setBorderRight(BorderStyle.THIN);
		hlink_style.setBorderBottom(BorderStyle.DOUBLE);

		hlink_style.setBorderColor(BorderSide.TOP, new XSSFColor(new Color(0, 0, 0)));
		hlink_style.setBorderColor(BorderSide.RIGHT, new XSSFColor(new Color(0, 0, 0)));
		hlink_style.setBorderColor(BorderSide.BOTTOM, new XSSFColor(new Color(0, 0, 0)));
		hlink_style.setBorderColor(BorderSide.LEFT, new XSSFColor(new Color(0, 0, 0)));

		XSSFFont bodyfont = workbook.createFont();
		bodyfont.setBoldweight(XSSFFont.BOLDWEIGHT_NORMAL);
		bodyfont.setColor(new XSSFColor(Color.BLACK));

		XSSFCellStyle bodystyle = workbook.createCellStyle();
		bodystyle.setFont(bodyfont);

		bodystyle.setBorderLeft(BorderStyle.THIN);
		bodystyle.setBorderTop(BorderStyle.THIN);
		bodystyle.setBorderRight(BorderStyle.THIN);
		bodystyle.setBorderBottom(BorderStyle.THIN);

		bodystyle.setBorderColor(BorderSide.TOP, new XSSFColor(new Color(0, 0, 0)));
		bodystyle.setBorderColor(BorderSide.RIGHT, new XSSFColor(new Color(0, 0, 0)));
		bodystyle.setBorderColor(BorderSide.BOTTOM, new XSSFColor(new Color(0, 0, 0)));
		bodystyle.setBorderColor(BorderSide.LEFT, new XSSFColor(new Color(0, 0, 0)));

		XSSFSheet sheet = workbook.createSheet("Data");
		sheet.setDefaultColumnWidth(20);

		for (int rowIndex = mulaiRow; rowIndex < datas.size() + mulaiRow; rowIndex++) {

			List sub = datas.get(rowIndex - mulaiRow);
			boolean blank = true;
			for (int colIndex = 0; colIndex < sub.size(); colIndex++) {

				Object value = sub.get(colIndex);

				blank &= (value == null || value.toString().trim().isEmpty());
			}

			XSSFRow row = sheet.createRow(rowIndex);

			for (int colIndex = 0; colIndex < sub.size(); colIndex++) {

				Object value = sub.get(colIndex);
				XSSFCell cell = row.createCell(colIndex);

				if (!blank) {
					if ((value != null && !value.toString().isEmpty() && rowIndex == 1)
							|| (value != null && value.toString().startsWith("**"))) {
						cell.setCellStyle(hlink_style);
					} else {
						cell.setCellStyle(bodystyle);
					}
				}

				if (value != null && value instanceof Integer) {

					cell.setCellValue((Integer) value);

				} else if (value != null && value instanceof Double) {
					cell.setCellValue((Double) value);

				} else {
					cell.setCellValue(value == null ? ""
							: ((value != null && value.toString().startsWith("**") ? value.toString().substring(2)
									: value.toString())));
				}

				if (auto) {
					try {
						sheet.autoSizeColumn(colIndex);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/EcampusUtil.java:146");
						// TODO: handle exception
					}
				}
			}

		}

		LampiranLain kopLampiranLain = SuratUtil.ambilKopLampiranLain();
		String kop = kopLampiranLain == null ? "" : kopLampiranLain.ambilFile().getAbsolutePath();
//		System.out.println("kop -> " + kop);

		if (!kop.isEmpty()) {

			InputStream inputStream1 = new FileInputStream(kop);
			byte[] inputImageBytes1 = IOUtils.toByteArray(inputStream1);

			int inputImagePictureID1 = workbook.addPicture(inputImageBytes1, Workbook.PICTURE_TYPE_JPEG);
			XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
			XSSFClientAnchor ironManAnchor = new XSSFClientAnchor();
			ironManAnchor.setCol1(0); // Sets the column (0 based) of the first cell.
			ironManAnchor.setCol2(7); // Sets the column (0 based) of the Second cell.
			ironManAnchor.setRow1(0); // Sets the row (0 based) of the first cell.
			ironManAnchor.setRow2(5); // Sets the row (0 based) of the Second cell.

			drawing.createPicture(ironManAnchor, inputImagePictureID1);
		}

		File file = new File(fn);

		try {
			FileOutputStream fileOut = new FileOutputStream(file);
			workbook.write(fileOut);
			fileOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

		excelku.setSrc("../../tmp/" + file.getName());
		excelku.setStyle("border:1px solid #8AA3C1");
		excelku.setHeight("100%");
		excelku.setWidth("100%");
		excelku.setMaxrows(datas.size() + mulaiRow + 5);
		excelku.setMaxcolumns(lebar);

	}

	public static void setCellValue(Worksheet sheet, int rowIndex, int colIndex, Object value) {
		try {

			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);

			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if ((value != null && !value.toString().isEmpty() && rowIndex == 1)
					|| (value != null && value.toString().startsWith("**"))) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());

				if ((value != null && value.toString().startsWith("**"))) {
					cell.setCellValue(value.toString().substring(2));
				} else {
					cell.setCellValue(value == null ? ""
							: (value instanceof Number ? Common.numberFormat.get().format(value) : value.toString()));
				}

			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());

				cell.setCellValue(value == null ? ""
						: (value instanceof Date ? Common.dateFormat5.get().format(value)
								: value instanceof Number ? Common.numberFormat.get().format(value) : value.toString()));
			}
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void setCellValue(Worksheet sheet, int rowIndex, int colIndex, Long value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);

			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if (rowIndex == 1) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			}

			cell.setCellValue(value == null ? "" : Common.numberFormat.get().format(value));
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void setCellValue(Worksheet sheet, int rowIndex, int colIndex, Integer value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);

			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if (rowIndex == 1) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());

			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			}

			cell.setCellValue(value == null ? "" : Common.numberFormat.get().format(value));
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void setCellValue(Worksheet sheet, int rowIndex, int colIndex, Double value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);

			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if (rowIndex == 1) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			}

			cell.setCellValue(value == null ? "" : Common.numberFormat.get().format(value));
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void setCellValue(Worksheet sheet, int rowIndex, int colIndex, String value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);

			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if ((value != null && !value.toString().isEmpty() && rowIndex == 1)
					|| (value != null && value.startsWith("**"))) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());

				if ((value != null && value.startsWith("**"))) {
					cell.setCellValue(value.substring(2));
				} else {
					cell.setCellValue(value == null ? "" : value);
				}

			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
				cell.setCellValue(value == null ? "" : value);
			}
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void setCellValueBold(Worksheet sheet, int rowIndex, int colIndex, Object value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);
			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if ((value != null && !value.toString().isEmpty() && rowIndex == 1)
					|| (value != null && value.toString().startsWith("**"))) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());

				if ((value != null && value.toString().startsWith("**"))) {
					cell.setCellValue(value.toString().substring(2));
				} else {
					cell.setCellValue(value == null ? ""
							: (value instanceof Number ? Common.numberFormat.get().format(value) : value.toString()));
				}

			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			}

			cell.setCellValue(value == null ? ""
					: (value instanceof Number ? Common.numberFormat.get().format(value) : value.toString()));
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void setCellValueBold(Worksheet sheet, int rowIndex, int colIndex, Long value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);
			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if (rowIndex == 1) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			}

			cell.setCellValue(value == null ? "" : Common.numberFormat.get().format(value));
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void setCellValueBold(Worksheet sheet, int rowIndex, int colIndex, Integer value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);
			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if (rowIndex == 1) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			}

			cell.setCellValue(value == null ? "" : Common.numberFormat.get().format(value));
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void setCellValueBold(Worksheet sheet, int rowIndex, int colIndex, Double value) {

		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);
			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if (rowIndex == 1) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			}

			cell.setCellValue(value == null ? "" : Common.numberFormat.get().format(value));
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void setCellValueBold(Worksheet sheet, int rowIndex, int colIndex, String value) {
		try {
			Cell cell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);
			CellStyle cellStyle = cell.getCellStyle();
			Font font = sheet.getWorkbook().createFont();
			font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			cellStyle.setFont(font);
			cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
			cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);

			if ((value != null && !value.toString().isEmpty() && rowIndex == 1)
					|| (value != null && value.startsWith("**"))) {
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
				cellStyle.setFillBackgroundColor(IndexedColors.GREY_40_PERCENT.getIndex());

				if ((value != null && value.startsWith("**"))) {
					cell.setCellValue(value.substring(2));
				} else {
					cell.setCellValue(value == null ? "" : value);
				}

			} else {
				cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
				cell.setCellValue(value == null ? "" : value);
			}
			cell.setCellStyle(cellStyle);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void setBold(Worksheet sheet, Rect rect, Boolean isBold) {

		try {
			Utils.setFontBold(sheet, rect, isBold);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/EcampusUtil.java:531");

		}

	}

	public static void setBorder(Worksheet sheet, Rect rect, short borderFull, BorderStyle thin, String color) {
//		setBorder(sheet, rect);

		try {
			Utils.setBorder(sheet, rect, borderFull, thin, color);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/EcampusUtil.java:542");
//			setBorder(sheet, rect);
		}
	}

	public static void mergeCells(Worksheet sheet, int tRow, int lCol, int bRow, int rCol, boolean across) {
		try {
			sheet.addMergedRegion(new CellRangeAddress(tRow, bRow, lCol, rCol));
			Cell cell = Utils.getOrCreateCell(sheet, tRow, lCol);
			CellStyle cellStyle = cell.getCellStyle();
			cellStyle.setAlignment(CellStyle.ALIGN_LEFT);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/EcampusUtil.java:553");

		}
	}
}
