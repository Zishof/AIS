package ais.common.newui.menu.test;

import ais.common.newui.menu.SapuScaffoldProbe;

/** Uji klasifikasi semantik scaffold tanpa membutuhkan Hibernate atau basis data. */
public final class SapuScaffoldProbeSelfTest {
    private static int failures;

    public static void main(String[] args) {
        String jsp = "request.setAttribute(\"nuiServiceType\", \"Dashboard\");";
        check("dashboard".equals(SapuScaffoldProbe.tipeLayananDariIsi(jsp)),
                "tipe literal tidak terbaca");
        check("tidak_diketahui".equals(SapuScaffoldProbe.tipeLayananDariIsi("tanpa atribut")),
                "sumber tanpa atribut harus eksplisit tidak diketahui");

        check(SapuScaffoldProbe.perluKontrakKhusus("dashboard"), "dashboard harus direview");
        check(SapuScaffoldProbe.perluKontrakKhusus("report"), "report harus direview");
        check(SapuScaffoldProbe.perluKontrakKhusus("integration"), "integration harus direview");
        check(SapuScaffoldProbe.perluKontrakKhusus("workflow"), "workflow harus direview");
        check(!SapuScaffoldProbe.perluKontrakKhusus("master"), "master bukan otomatis risiko semantik");
        check(!SapuScaffoldProbe.perluKontrakKhusus("list"), "list bukan otomatis risiko semantik");

        if (failures > 0) {
            System.out.println("FAIL SapuScaffoldProbeSelfTest: " + failures);
            System.exit(1);
        }
        System.out.println("PASS SapuScaffoldProbeSelfTest");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            failures++;
            System.out.println("  - " + message);
        }
    }
}
