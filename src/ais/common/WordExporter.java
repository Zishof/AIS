package ais.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Iterator;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

public class WordExporter {

	/**
	 * Mengubah JSONArray menjadi file Word (.docx) dengan format tabel. * @param
	 * jsonArray Data sumber
	 * 
	 * @param filePath Lokasi penyimpanan file (e.g., "D:/report.docx")
	 * @throws IOException
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static void exportJsonToWordTable(JSONArray jsonArray, File filePath) throws Exception {
		if (jsonArray == null || jsonArray.length() == 0) {
			throw new IllegalArgumentException("JSONArray tidak boleh kosong.");
		}

		XWPFDocument document = new XWPFDocument();

		// 1. Setting Ukuran Kertas ke A4 (satuan twips: 1 inch = 1440 twips)
		// A4 = 8.27 x 11.69 inches -> 11906 x 16838 twips
		CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
		CTPageSz pageSize = sectPr.addNewPgSz();
		pageSize.setW(BigInteger.valueOf(11906));
		pageSize.setH(BigInteger.valueOf(16838));

		// 2. Ambil key dari object pertama sebagai Header
		JSONObject firstObj = jsonArray.getJSONObject(0);
		Iterator<String> keys = firstObj.keys();
		int numCols = firstObj.length();

		// 3. Buat Tabel
		XWPFTable table = document.createTable(jsonArray.length() + 1, numCols);
//		table.setWidth("100%"); // Menyesuaikan lebar halaman

		// 4. Styling Header
		XWPFTableRow headerRow = table.getRow(0);
		int colIdx = 0;
		Iterator<String> headerKeys = firstObj.keys();

		while (headerKeys.hasNext()) {
			String key = headerKeys.next();
			XWPFTableCell cell = headerRow.getCell(colIdx);

			// Warna Background Silver
			cell.setColor("C0C0C0");

			XWPFRun run = cell.addParagraph().createRun();
			run.setBold(true);
			run.setText(key.toUpperCase());

			// Hapus paragraf default yang kosong di dalam cell
			if (cell.getParagraphs().size() > 1) {
				cell.removeParagraph(0);
			}
			colIdx++;
		}

		// 5. Isi Data Body
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject dataObj = jsonArray.getJSONObject(i);
			XWPFTableRow dataRow = table.getRow(i + 1);

			int cellIdx = 0;
			Iterator<String> bodyKeys = firstObj.keys(); // Gunakan urutan key yang sama dengan header
			while (bodyKeys.hasNext()) {
				String key = bodyKeys.next();
				String value = dataObj.optString(key, "");
				dataRow.getCell(cellIdx).setText(value);
				cellIdx++;
			}
		}

		// 6. Write File
		FileOutputStream out = new FileOutputStream(filePath);
		document.write(out);
		out.close();
		document.close();

		System.out.println("File berhasil dibuat di: " + filePath);
	}
}