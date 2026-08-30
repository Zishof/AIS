package ais.action.master.obe;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.Row;
import org.zkoss.poi.ss.usermodel.Sheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Filedownload;

import ais.common.Common;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyMessageboxConfig;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/** Ekspor, impor, dan cetak matriks relasi OBE yang dipakai bersama oleh seluruh tab matriks CPL. */
public final class ObeRelationMatrixTransferHelper {

	private static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	public interface MatrixAdapter {
		String getTitle();
		String getRowLabel();
		String getColumnLabel();
		int getRowCount();
		int getColumnCount();
		String getRowCode(int row);
		String getRowName(int row);
		String getColumnCode(int column);
		String getColumnName(int column);
		boolean isSelected(int row, int column);
		void applyRow(int row, boolean[] selected) throws Exception;
		void refresh() throws Exception;
	}

	private ObeRelationMatrixTransferHelper() {
	}

	public static void pasang(Component parent, final MatrixAdapter adapter) {
		MyButtonConfig download = new MyButtonConfig("Download Excel");
		download.setParent(parent);
		download.setStyle("margin-left:6px");
		download.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) throws Exception {
				downloadExcel(adapter);
			}
		});

		MyButtonConfig upload = new MyButtonConfig("Upload Excel");
		upload.setParent(parent);
		upload.setStyle("margin-left:6px");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			public void onEvent(Event event) throws Exception {
				Media media = ((UploadEvent) event).getMedia();
				if (media == null) {
					return;
				}
				InputStream in = null;
				try {
					in = media.getStreamData();
					importExcel(in, adapter);
				} finally {
					if (in != null) {
						try { in.close(); } catch (Exception ignored) { }
					}
				}
			}
		});

		MyButtonConfig pdf = new MyButtonConfig("Cetak PDF");
		pdf.setParent(parent);
		pdf.setStyle("margin-left:6px");
		pdf.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) throws Exception {
				cetakPdf(adapter);
			}
		});
	}

	private static void pastikanAdaData(MatrixAdapter adapter) throws Exception {
		if (adapter.getRowCount() == 0 || adapter.getColumnCount() == 0) {
			throw new IllegalStateException("Data matriks belum tersedia. Pilih fakultas/prodi lalu klik Refresh.");
		}
	}

	private static void downloadExcel(MatrixAdapter adapter) {
		try {
			pastikanAdaData(adapter);
			XSSFWorkbook wb = new XSSFWorkbook();
			Sheet sheet = wb.createSheet("Matriks OBE");
			Row title = sheet.createRow(0);
			title.createCell(0).setCellValue(adapter.getTitle());
			Row note = sheet.createRow(1);
			note.createCell(0).setCellValue("Isi 1/YA/X untuk terhubung dan 0/TIDAK/kosong untuk tidak terhubung. Jangan mengubah kode.");
			Row header = sheet.createRow(2);
			header.createCell(0).setCellValue("Kode " + adapter.getRowLabel());
			header.createCell(1).setCellValue(adapter.getRowLabel());
			for (int c = 0; c < adapter.getColumnCount(); c++) {
				header.createCell(c + 2).setCellValue(adapter.getColumnCode(c));
			}
			Row names = sheet.createRow(3);
			names.createCell(1).setCellValue(adapter.getColumnLabel());
			for (int c = 0; c < adapter.getColumnCount(); c++) {
				names.createCell(c + 2).setCellValue(adapter.getColumnName(c));
			}
			for (int r = 0; r < adapter.getRowCount(); r++) {
				Row row = sheet.createRow(r + 4);
				row.createCell(0).setCellValue(adapter.getRowCode(r));
				row.createCell(1).setCellValue(adapter.getRowName(r));
				for (int c = 0; c < adapter.getColumnCount(); c++) {
					row.createCell(c + 2).setCellValue(adapter.isSelected(r, c) ? 1 : 0);
				}
			}
			sheet.createFreezePane(2, 4);
			sheet.setColumnWidth(0, 256 * 18);
			sheet.setColumnWidth(1, 256 * 42);
			for (int c = 0; c < adapter.getColumnCount(); c++) {
				sheet.setColumnWidth(c + 2, 256 * 16);
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			wb.write(out);
			Filedownload.save(out.toByteArray(), MIME_XLSX, fileName(adapter.getTitle()) + ".xlsx");
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void importExcel(InputStream in, MatrixAdapter adapter) {
		try {
			pastikanAdaData(adapter);
			XSSFWorkbook wb = new XSSFWorkbook(in);
			Sheet sheet = wb.getSheetAt(0);
			Row header = sheet.getRow(2);
			if (header == null) {
				throw new IllegalArgumentException("Format Excel tidak dikenali. Gunakan file hasil Download Excel dari halaman ini.");
			}
			Map<String, Integer> columns = new HashMap<String, Integer>();
			for (int c = 2; c < header.getLastCellNum(); c++) {
				columns.put(text(header.getCell(c)).trim().toUpperCase(), Integer.valueOf(c));
			}
			Map<String, Integer> rows = new HashMap<String, Integer>();
			for (int r = 0; r < adapter.getRowCount(); r++) {
				rows.put(adapter.getRowCode(r).trim().toUpperCase(), Integer.valueOf(r));
			}
			int updated = 0;
			for (int n = 4; n <= sheet.getLastRowNum(); n++) {
				Row excelRow = sheet.getRow(n);
				if (excelRow == null) continue;
				Integer rowIndex = rows.get(text(excelRow.getCell(0)).trim().toUpperCase());
				if (rowIndex == null) continue;
				boolean[] selected = new boolean[adapter.getColumnCount()];
				for (int c = 0; c < adapter.getColumnCount(); c++) {
					Integer excelColumn = columns.get(adapter.getColumnCode(c).trim().toUpperCase());
					selected[c] = excelColumn != null && truthy(text(excelRow.getCell(excelColumn.intValue())));
				}
				adapter.applyRow(rowIndex.intValue(), selected);
				updated++;
			}
			adapter.refresh();
			MyMessageboxConfig.show("Upload selesai. " + updated + " baris " + adapter.getRowLabel() + " diperbarui.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void cetakPdf(MatrixAdapter adapter) {
		try {
			pastikanAdaData(adapter);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Document document = new Document((adapter.getColumnCount() > 12 ? PageSize.A3 : PageSize.A4).rotate(), 18, 18, 22, 22);
			PdfWriter.getInstance(document, out);
			document.open();
			document.add(new Paragraph(adapter.getTitle(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
			document.add(new Paragraph(" "));
			PdfPTable table = new PdfPTable(adapter.getColumnCount() + 2);
			table.setWidthPercentage(100f);
			Font font = FontFactory.getFont(FontFactory.HELVETICA, adapter.getColumnCount() > 15 ? 5 : 7);
			addCell(table, "Kode", font);
			addCell(table, adapter.getRowLabel(), font);
			for (int c = 0; c < adapter.getColumnCount(); c++) addCell(table, adapter.getColumnCode(c), font);
			for (int r = 0; r < adapter.getRowCount(); r++) {
				addCell(table, adapter.getRowCode(r), font);
				addCell(table, adapter.getRowName(r), font);
				for (int c = 0; c < adapter.getColumnCount(); c++) addCell(table, adapter.isSelected(r, c) ? "X" : "", font);
			}
			document.add(table);
			document.close();
			Filedownload.save(out.toByteArray(), "application/pdf", fileName(adapter.getTitle()) + ".pdf");
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void addCell(PdfPTable table, String value, Font font) {
		table.addCell(new PdfPCell(new Phrase(value == null ? "" : value, font)));
	}

	private static boolean truthy(String value) {
		String v = value == null ? "" : value.trim().toUpperCase();
		return "1".equals(v) || "1.0".equals(v) || "YA".equals(v) || "Y".equals(v) || "TRUE".equals(v)
				|| "X".equals(v) || "CENTANG".equals(v) || "CHECK".equals(v);
	}

	private static String text(Cell cell) {
		if (cell == null) return "";
		try {
			if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
				double value = cell.getNumericCellValue();
				return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
			}
			if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN) return cell.getBooleanCellValue() ? "TRUE" : "FALSE";
			return cell.getStringCellValue();
		} catch (Exception e) {
			return String.valueOf(cell);
		}
	}

	public static boolean containsId(String csv, long id) {
		if (csv == null || csv.trim().length() == 0) return false;
		String[] values = csv.split(",");
		for (int i = 0; i < values.length; i++) if (String.valueOf(id).equals(values[i].trim())) return true;
		return false;
	}

	public static String buildIds(MatrixAdapter adapter, int row, boolean[] selected) {
		StringBuilder result = new StringBuilder();
		for (int c = 0; c < selected.length; c++) {
			if (selected[c]) result.append(',').append(adapter.getColumnCode(c));
		}
		if (result.length() > 0) result.append(',');
		return result.toString();
	}

	private static String fileName(String value) {
		return (value == null ? "matriks_obe" : value).replaceAll("[^A-Za-z0-9._-]+", "_");
	}
}
