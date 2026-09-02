package ais.common.newui.laporan;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.poi.ss.usermodel.Sheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

/** Self-test struktur XLSX dan validasi filter Rekap Penilaian. */
public final class NewUiLaporanRekapPenilaianSelfTest {
    private NewUiLaporanRekapPenilaianSelfTest() { }
    private static void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }
    public static void main(String[] args) throws Exception {
        NewUiLaporanRekapPenilaianController.Baris row =
                new NewUiLaporanRekapPenilaianController.Baris();
        row.kode = "MK01"; row.matakuliah = "Algoritma"; row.hari = "Senin";
        row.waktu = "08:00-10:00"; row.dosen = "Dosen A"; row.ruang = "R1";
        row.semester = "3 A"; row.total = 4; row.sudah = 3; row.belum = 1;
        row.persen = 75.0; row.terakhir = "02 September 2026";
        row.totalIps = 12.0; row.rataIps = 3.0; row.rataNilai = 80.0;
        List<NewUiLaporanRekapPenilaianController.Baris> rows =
                new ArrayList<NewUiLaporanRekapPenilaianController.Baris>();
        rows.add(row);
        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(
                NewUiLaporanRekapPenilaianController.buatXlsx(rows)));
        Sheet sheet = wb.getSheetAt(0);
        check("KODE MATAKULIAH".equals(sheet.getRow(0).getCell(0).getStringCellValue()),
                "header berubah");
        check("MK01".equals(sheet.getRow(1).getCell(0).getStringCellValue()), "kode hilang");
        check(sheet.getRow(1).getCell(7).getNumericCellValue() == 3.0
                        && sheet.getRow(1).getCell(8).getNumericCellValue() == 1.0
                        && sheet.getRow(1).getCell(9).getNumericCellValue() == 75.0,
                "progres nilai keliru");
        check(sheet.getRow(1).getCell(12).getNumericCellValue() == 3.0
                        && sheet.getRow(1).getCell(13).getNumericCellValue() == 80.0,
                "rata-rata keliru");
        System.out.println("NewUiLaporanRekapPenilaianSelfTest OK");
    }
}
