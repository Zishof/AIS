package ais.common.newui.menu.test;

import ais.common.newui.menu.NewUiIconUtil;

/** Test harness tanpa JUnit untuk kompatibilitas icon Menu lama. */
public final class NewUiIconUtilSelfTest {

    private NewUiIconUtilSelfTest() { }

    private static void check(String expected, String actual) {
        if (!expected.equals(actual)) throw new IllegalStateException(expected + " != " + actual);
    }

    public static void main(String[] args) {
        check("fa-solid fa-bus", NewUiIconUtil.classes("fas fa-bus", false));
        check("fa-regular fa-calendar-days", NewUiIconUtil.classes("far fa-calendar-alt", false));
        check("fa-solid fa-van-shuttle", NewUiIconUtil.classes("fas fa-shuttle-van", false));
        check("fa-solid fa-right-left", NewUiIconUtil.classes("fas fa-exchange-alt", false));
        check("fa-solid fa-user-doctor", NewUiIconUtil.classes("fas fa-user-md", false));
        check("fa-solid fa-folder-tree", NewUiIconUtil.classes("fas fa-", true));
        check("fa-solid fa-file-lines", NewUiIconUtil.classes("<script>alert(1)</script>", false));
        check("fa-solid fa-graduation-cap",
                NewUiIconUtil.classes("fas fa-folder", "Sistem Informasi Akademik", true));
        check("fa-solid fa-envelopes-bulk",
                NewUiIconUtil.classes(null, "Tata Kelola Surat", true));
        check("fa-solid fa-money-bill-wave",
                NewUiIconUtil.classes("fas fa-folder", "Penggajian (Payroll)", true));
        check("fa-solid fa-user-shield",
                NewUiIconUtil.classes("fas fa-folder", "Satuan Pengawasan Internal", true));
        check("fa-solid fa-folder-tree",
                NewUiIconUtil.classes("fas fa-folder-tree", "Kategori Khusus Pelanggan", true));
        System.out.println("PASS Font Awesome 7 menu icon normalization self-test");
    }
}
