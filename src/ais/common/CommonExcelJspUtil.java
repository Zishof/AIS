package ais.common;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;

/**
 * Utilitas bersama AIS untuk common excel jsp util. Kelas ini mengonsolidasikan operasi lintas
 * layar/service yang benar-benar satu domain agar pemanggil tidak membuat helper dengan fungsi
 * paralel.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pelaporan/ekspor ({@code exportDataToExcel()}); operasi domain
 * lain ({@code importDataFromExcel()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 */
public class CommonExcelJspUtil {

    /**
     * Ekspor List Data ke Response sebagai File Excel (.xlsx)
     */
    @SuppressWarnings("deprecation")
	public static void exportDataToExcel(HttpServletResponse response, List<?> dataList, String[] columns, String[] headers, String fileName, Class<?> clazz) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".xlsx\"");

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Data");
            sheet.setDefaultColumnWidth(20);

            // Styling Header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Buat Header Row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.getCell(0).setCellStyle(headerStyle);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i + 1);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Isi Data
            if (dataList != null && !dataList.isEmpty()) {
                ClassMetadata metadata = HibernateUtil.getClassMetadata(clazz);
                int rowIndex = 1;
                for (Object obj : dataList) {
                    Row row = sheet.createRow(rowIndex++);
                    
                    // ID
                    Object id = metadata.getIdentifier(obj, org.hibernate.EntityMode.POJO);
                    row.createCell(0).setCellValue(id != null ? id.toString() : "");

                    // Kolom dinamis
                    for (int i = 0; i < columns.length; i++) {
                        try {
                            Object val = metadata.getPropertyValue(obj, columns[i], org.hibernate.EntityMode.POJO);
                            row.createCell(i + 1).setCellValue(val != null ? val.toString() : "");
                        } catch (Exception e) {
                            row.createCell(i + 1).setCellValue("");
                        }
                    }
                }
            }

            OutputStream out = response.getOutputStream();
            workbook.write(out);
            workbook.close();
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonExcelJspUtil.java:87");
        }
    }

    /**
     * Impor Data dari InputStream Excel (.xlsx)
     * @throws Exception  
     */
    @SuppressWarnings("deprecation")
	public static JSONObject importDataFromExcel(InputStream is, String[] columns, Class<?> clazz) throws Exception { 
        JSONObject result = new JSONObject();
        Session session = HibernateUtil.currentSession();
        int successCount = 0;
        
        try {
            @SuppressWarnings("resource")
			Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            ClassMetadata metadata = HibernateUtil.getClassMetadata(clazz);

            session.getTransaction().begin();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Asumsi Kolom 0 adalah ID
                Cell idCell = row.getCell(0);
                Long id = null;
                
                // PERBAIKAN: Menggunakan konstanta int bawaan Cell (kompatibel dengan POI lama/ZK POI)
                if (idCell != null && idCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    id = (long) idCell.getNumericCellValue();
                } else if (idCell != null && idCell.getCellType() == Cell.CELL_TYPE_STRING && !idCell.getStringCellValue().isEmpty()) {
                    id = Long.parseLong(idCell.getStringCellValue());
                }

                Object entity = null;
                if (id != null) {
                    entity = session.get(clazz, id);
                }
                
                if (entity == null) {
                    entity = clazz.newInstance();
                }

                // Set Kolom dinamis
                for (int j = 0; j < columns.length; j++) {
                    Cell cell = row.getCell(j + 1);
                    if (cell != null) {
                        // PERBAIKAN: Menggunakan konstanta int bawaan Cell
                        String val = "";
                        if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                            val = cell.getStringCellValue();
                        } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                            val = String.valueOf((long) cell.getNumericCellValue());
                        }

                        try {
                            // Untuk mapping relasi objek kompleks, diperlukan logic tambahan.
                            // Ini adalah simplifikasi untuk tipe data dasar (String/Integer).
                            metadata.setPropertyValue(entity, columns[j], val, org.hibernate.EntityMode.POJO);
                        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonExcelJspUtil.java:149");
                            // Abaikan jika tipe tidak cocok
                        }
                    }
                }

                session.saveOrUpdate(entity);
                successCount++;
                
                // Batch processing untuk memory efficiency
                if (i % 50 == 0) {
                    session.flush();
                    session.clear();
                }
            }

            session.getTransaction().commit();
            result.put("status", "00");
            result.put("message", "Berhasil mengimpor " + successCount + " data.");

        } catch (Exception e) {
            if(session.getTransaction().isActive()) session.getTransaction().rollback();
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonExcelJspUtil.java:171");
            result.put("status", "99");
            result.put("message", "Gagal impor: " + e.getMessage());
        } finally {
            HibernateUtil.closeSession();
        }
        return result;
    }
}