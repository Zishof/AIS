package ais.common.newui.laporan;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.poi.ss.usermodel.Sheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

/** Self-test struktur workbook Pendapatan Kursus tanpa basis data/container. */
public final class NewUiLaporanKursusSelfTest {

    private NewUiLaporanKursusSelfTest() { }

    private static void check(boolean benar, String pesan) {
        if (!benar) throw new IllegalStateException(pesan);
    }

    public static void main(String[] args) throws Exception {
        List<Object[]> rows = new ArrayList<Object[]>();
        rows.add(new Object[] { "01-09-2026", "Aisyah", 2, 100000.0 });
        rows.add(new Object[] { "01-09-2026", "Fatimah", 1, 50000.0 });
        rows.add(new Object[] { "02-09-2026", "Maryam", 3, 300000.0 });
        byte[] bytes = NewUiLaporanKursusController.buatXlsx(rows);
        check(bytes.length > 1000, "workbook tidak terbentuk");

        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes));
        Sheet sheet = wb.getSheetAt(0);
        check("REKAPITULASI PEMBALIAN".equals(
                sheet.getRow(1).getCell(0).getStringCellValue()), "judul berubah");
        check("Tanggal".equals(sheet.getRow(2).getCell(0).getStringCellValue()),
                "header tanggal berubah");
        check("01-09-2026".equals(sheet.getRow(3).getCell(0).getStringCellValue()),
                "tanggal pertama tidak ditampilkan");
        check("".equals(sheet.getRow(4).getCell(0).getStringCellValue()),
                "tanggal berulang harus dikosongkan seperti layar ZK");
        check(sheet.getRow(4).getCell(4).getNumericCellValue() == 3.0
                        && sheet.getRow(4).getCell(5).getNumericCellValue() == 150000.0,
                "subtotal tanggal pertama keliru");
        check(sheet.getRow(5).getCell(4).getNumericCellValue() == 3.0
                        && sheet.getRow(5).getCell(5).getNumericCellValue() == 300000.0,
                "subtotal tanggal kedua keliru");
        check("TOTAL".equals(sheet.getRow(6).getCell(0).getStringCellValue())
                        && sheet.getRow(6).getCell(4).getNumericCellValue() == 6.0
                        && sheet.getRow(6).getCell(5).getNumericCellValue() == 450000.0,
                "total keseluruhan keliru");

        check(NewUiLaporanKursusController.tanggal("2026-09-02", "Tanggal") != null,
                "tanggal valid ditolak");
        boolean ditolak = false;
        try { NewUiLaporanKursusController.tanggal("2026-02-31", "Tanggal"); }
        catch (IllegalArgumentException expected) { ditolak = true; }
        check(ditolak, "tanggal tidak valid harus ditolak");
        System.out.println("NewUiLaporanKursusSelfTest OK");
    }
}
