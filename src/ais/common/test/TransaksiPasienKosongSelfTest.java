package ais.common.test;

import java.lang.reflect.Method;
import java.util.Date;
import ais.action.master.sirs.util.CommonPendaftaranUtil;
import ais.database.model.sirs.Pasien;

/** Tests optional patient display data without connecting to a database. */
public final class TransaksiPasienKosongSelfTest {
    private static String call(String name, Pasien pasien) throws Exception {
        Method method = CommonPendaftaranUtil.class.getDeclaredMethod(name, Pasien.class);
        method.setAccessible(true);
        return (String) method.invoke(null, pasien);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Pasien kosong = new Pasien() {
            public Date getTanggalLahir() { return null; }
            public Integer getUmur() { return null; }
        };
        check("".equals(call("formatTanggalLahirTransaksi", null)), "No patient date");
        check("".equals(call("formatTanggalLahirTransaksi", kosong)), "Missing birth date");
        check("".equals(call("formatUmurTransaksi", null)), "No patient age");
        check("".equals(call("formatUmurTransaksi", kosong)), "Missing age must not become zero");
        Pasien bayi = new Pasien() {
            public Integer getUmur() { return Integer.valueOf(0); }
        };
        check("0 thn".equals(call("formatUmurTransaksi", bayi)), "Actual zero age preserved");
        Pasien dewasa = new Pasien() {
            public Integer getUmur() { return Integer.valueOf(25); }
        };
        check("25 thn".equals(call("formatUmurTransaksi", dewasa)), "Existing age preserved");
        System.out.println("PASS TransaksiPasienKosongSelfTest");
    }
}
