package ais.common.newui.laporan;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.poi.ss.usermodel.Sheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

/** Self-test workbook lima keluaran Laporan Denda tanpa basis data/container. */
public final class NewUiLaporanDendaSelfTest {
    private NewUiLaporanDendaSelfTest() { }
    private static void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }

    public static void main(String[] args) throws Exception {
        List<Object[]> harian = new ArrayList<Object[]>();
        harian.add(new Object[] { "01-09-2026", "Admin A (a)", 2, 10000.0, 7000.0 });
        harian.add(new Object[] { "01-09-2026", "Admin B (b)", 1, 5000.0, 1000.0 });
        harian.add(new Object[] { "02-09-2026", "Admin A (a)", 3, 30000.0, 20000.0 });
        List<Object[]> rekap = new ArrayList<Object[]>();
        rekap.add(new Object[] { "Admin A (a)", 5, 40000.0, 27000.0 });
        rekap.add(new Object[] { "Admin B (b)", 1, 5000.0, 1000.0 });
        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(
                NewUiLaporanDendaController.buatRekapXlsx(harian, rekap,
                        "REKAPITULASI DENDA ANGGOTA\n SEMUA PERPUSTAKAAN", "Jumlah Anggota")));
        Sheet s = wb.getSheetAt(0);
        check("01-09-2026".equals(s.getRow(3).getCell(0).getStringCellValue()),
                "tanggal awal hilang");
        check("".equals(s.getRow(4).getCell(0).getStringCellValue()),
                "tanggal berulang tidak dikosongkan");
        check(s.getRow(4).getCell(5).getNumericCellValue() == 3.0
                        && s.getRow(4).getCell(6).getNumericCellValue() == 15000.0
                        && s.getRow(4).getCell(7).getNumericCellValue() == 8000.0,
                "subtotal per tanggal keliru");
        check(s.getRow(6).getCell(5).getNumericCellValue() == 6.0
                        && s.getRow(6).getCell(6).getNumericCellValue() == 45000.0
                        && s.getRow(6).getCell(7).getNumericCellValue() == 28000.0,
                "total harian keliru");

        List<Object[]> detail = new ArrayList<Object[]>();
        detail.add(new Object[] { "Pusat", "Anggota A", "Prodi", "01 September 2026",
                "Buku", 10000.0, 4000.0, "K-1", "Admin" });
        detail.add(new Object[] { "Pusat", "Anggota B", "Prodi", "02 September 2026",
                "Buku 2", 5000.0, 1000.0, "K-2", "Admin" });
        wb = new XSSFWorkbook(new ByteArrayInputStream(
                NewUiLaporanDendaController.buatDataXlsx("DATA DENDA", new String[] {
                        "Perpustakaan", "Anggota", "Prodi", "Tanggal", "Item", "Denda",
                        "Dibayar", "Kode", "Admin" }, detail, 4, 5, 6)));
        s = wb.getSheetAt(0);
        check("Denda Total".equals(s.getRow(5).getCell(4).getStringCellValue())
                        && s.getRow(5).getCell(5).getNumericCellValue() == 15000.0
                        && s.getRow(5).getCell(6).getNumericCellValue() == 5000.0,
                "total rincian keliru");

        check(NewUiLaporanDendaController.jenis(NewUiLaporanDendaController.BELUM_LUNAS)
                .equals(NewUiLaporanDendaController.BELUM_LUNAS), "jenis sah ditolak");
        boolean ditolak = false;
        try { NewUiLaporanDendaController.jenis("lain"); }
        catch (IllegalArgumentException expected) { ditolak = true; }
        check(ditolak, "jenis asing harus ditolak");
        System.out.println("NewUiLaporanDendaSelfTest OK (5 jenis laporan)");
    }
}
