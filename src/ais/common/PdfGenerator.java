package ais.common;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Tipe khusus untuk pdf generator. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit
 * pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code convertJsonToPdf}(). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 */
public class PdfGenerator {

	/**
	 * Mengubah JSONArray menjadi file PDF dalam bentuk tabel. * @param jsonArray
	 * Data sumber
	 * 
	 * @param filePath Lokasi penyimpanan file (misal: "D:/report.pdf")
	 */
	@SuppressWarnings("unchecked")
	public static void convertJsonToPdf(JSONArray jsonArray, File filePath) {
		Document document = new Document(PageSize.A4);
		try {
			PdfWriter.getInstance(document, new FileOutputStream(filePath));
			document.open();

			if (jsonArray.length() > 0) {
				// Ambil key dari objek pertama untuk dijadikan Header
				JSONObject firstObject = jsonArray.getJSONObject(0);
				int numColumns = firstObject.length();

				PdfPTable table = new PdfPTable(numColumns);
				table.setWidthPercentage(100); // Lebar tabel 100% dari A4

				// 1. Membuat Header
				Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
				BaseColor silver = new BaseColor(192, 192, 192);

				Iterator<String> keys = firstObject.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					if (key != null && !key.endsWith("_type") && !key.endsWith("_id") && !key.endsWith("_nama")) {
						PdfPCell headerCell = new PdfPCell(new Phrase(key, headerFont));
						headerCell.setBackgroundColor(silver);
						headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
						headerCell.setPadding(5);
						table.addCell(headerCell);
					}
				}

				// 2. Mengisi Data Body
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject obj = jsonArray.getJSONObject(i);
					Iterator<String> dataKeys = obj.keys();
					while (dataKeys.hasNext()) {
						String key = dataKeys.next();
						if (key != null && !key.endsWith("_type") && !key.endsWith("_id") && !key.endsWith("_nama")) {
							String value = obj.get(key).toString();
							PdfPCell cell = new PdfPCell(new Phrase(value));
							cell.setPadding(5);
							table.addCell(cell);
						}
					}
				}

				document.add(table);
			} else {
				document.add(new Paragraph("Data kosong."));
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PdfGenerator.java:81");
		} finally {
			document.close();
		}
	}
}
