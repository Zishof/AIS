package ais.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Tipe khusus untuk excel exporter. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit
 * pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code exportJsonToExcel}(). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 */
public class ExcelExporter {

	/**
	 * Mengubah JSONArray menjadi file Excel (.xlsx) * @param jsonArray Data sumber
	 * 
	 * @param filePath Lokasi penyimpanan file (contoh: "C:/data/report.xlsx")
	 * @throws IOException
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static void exportJsonToExcel(JSONArray jsonArray, File filePath) throws Exception {
		if (jsonArray == null || jsonArray.length() == 0) {
			throw new IllegalArgumentException("JSONArray kosong atau null.");
		}

		// Create Workbook .xlsx
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet("Data");

		// 1. Ambil Header dari Object pertama
		JSONObject firstObject = jsonArray.getJSONObject(0);
		Iterator<String> keys = firstObject.keys();

		XSSFRow headerRow = sheet.createRow(0);
		int headerColIndex = 0;
		while (keys.hasNext()) {
			String key = keys.next();
			if (key != null && !key.endsWith("_type") && !key.endsWith("_id") && !key.endsWith("_nama")) {
				XSSFCell cell = headerRow.createCell(headerColIndex++);
				cell.setCellValue(key);
			}
		}

		// 2. Isi Data
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject obj = jsonArray.getJSONObject(i);
			XSSFRow row = sheet.createRow(i + 1);

			// Iterasi ulang keys untuk memastikan urutan kolom konsisten dengan header
			int dataColIndex = 0;
			Iterator<String> dataKeys = firstObject.keys();
			while (dataKeys.hasNext()) {
				String key = dataKeys.next();
				if (key != null && !key.endsWith("_type") && !key.endsWith("_id") && !key.endsWith("_nama")) {
					XSSFCell cell = row.createCell(dataColIndex++);

					Object value = obj.opt(key);
					if (value != null) {
						if (value instanceof Number) {
							cell.setCellValue(((Number) value).doubleValue());
						} else {
							cell.setCellValue(value.toString());
						}
					}
				}
			}
		}

		// 3. Auto-size kolom agar rapi
		for (int i = 0; i < headerColIndex; i++) {
			sheet.autoSizeColumn(i);
		}

		// 4. Tulis ke File
		try {
			FileOutputStream fileOut = new FileOutputStream(filePath);
			workbook.write(fileOut);
			fileOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}
	
	
	
}