package ais.common.newui.laporan;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ais.database.model.Jurusan;

/** Self-test pemetaan hasil agregasi tabel Akreditasi 1 B. */
public final class NewUiLaporanAkreditasi1BSelfTest {
    private NewUiLaporanAkreditasi1BSelfTest() { }
    private static void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }
    public static void main(String[] args) {
        List<Object[]> sumber = new ArrayList<Object[]>();
        sumber.add(new Object[] { Jurusan.TERAKREDITASI_A, BigInteger.valueOf(2),
                BigInteger.ONE, BigInteger.valueOf(4), BigInteger.ZERO, BigInteger.ZERO,
                BigInteger.ONE, BigInteger.ZERO, BigInteger.ZERO, BigInteger.valueOf(3),
                BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO });
        List<Map<String, Object>> rows =
                NewUiLaporanAkreditasi1BController.susunBaris(sumber);
        check(rows.size() == Jurusan.SEMUA_STATUS.size(), "status nol ikut hilang");
        Map<String, Object> akreditasiA = null;
        for (Map<String, Object> row : rows)
            if (Jurusan.TERAKREDITASI_A.equals(row.get("status"))) akreditasiA = row;
        check(akreditasiA != null, "status A hilang");
        check(((Number) akreditasiA.get("S3")).doubleValue() == 2.0
                        && ((Number) akreditasiA.get("S1")).doubleValue() == 4.0
                        && ((Number) akreditasiA.get("D4")).doubleValue() == 3.0,
                "kolom jenjang bergeser");
        Map<String, Object> kosong = rows.get(0);
        if (Jurusan.TERAKREDITASI_A.equals(kosong.get("status"))) kosong = rows.get(1);
        check(((Number) kosong.get("S3")).doubleValue() == 0.0
                        && ((Number) kosong.get("D1")).doubleValue() == 0.0,
                "baris tanpa data tidak nol");
        System.out.println("NewUiLaporanAkreditasi1BSelfTest OK ("
                + rows.size() + " status)");
    }
}
