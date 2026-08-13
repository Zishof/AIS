package ais.common.newui.menu.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ais.common.newui.menu.NewUiModuleFunctionService;

/** Audit deterministik parity fungsi toolbar index.zul tanpa database/JUnit. */
public final class NewUiModuleFunctionServiceSelfTest {

    private static final String[] TOOLBAR_KEYS = new String[] {
        "emedic", "elearning", "prestasi", "pustaka", "workflow", "repository",
        "antar_jemput", "spmi", "toko", "koperasi", "akademik", "administrasi",
        "pengadaan", "pembayaran", "keuangan", "akuntansi", "kepegawaian", "gaji",
        "kinerja", "presensi", "kalender_akademik", "info_kegiatan", "feeder", "sister"
    };

    private NewUiModuleFunctionServiceSelfTest() { }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    public static void main(String[] args) {
        check(NewUiModuleFunctionService.moduleKeys().size() == TOOLBAR_KEYS.length,
                "jumlah kontrak modul harus sama dengan toolbarbutton index.zul");
        int total = 0;
        for (int i = 0; i < TOOLBAR_KEYS.length; i++) {
            String moduleKey = TOOLBAR_KEYS[i];
            check(NewUiModuleFunctionService.supports(moduleKey),
                    "kontrak fungsi belum tersedia: " + moduleKey);
            List<String> keys = NewUiModuleFunctionService.definitionKeys(moduleKey);
            check(keys.size() >= 4, "fungsi terlalu sedikit untuk " + moduleKey);
            Set<String> unique = new HashSet<String>(keys);
            check(unique.size() == keys.size(), "key fungsi duplikat pada " + moduleKey);
            total += keys.size();
        }
        check(total >= 110, "audit fungsi toolbar tidak lengkap: " + total);
        check(NewUiModuleFunctionService.definitionSourceClasses("emedic").contains(
                "ais.action.master.dashboard.sirs.DashboardSirsKomprehensif"),
                "dashboard utama eMedic existing belum dipetakan");
        List<String> prestasiSources = NewUiModuleFunctionService.definitionSourceClasses("prestasi");
        check(prestasiSources.contains("ais.action.master.dashboard.admin.DashboardKegiatanKemahasiswaanAdmin"),
                "dashboard prestasi mahasiswa admin belum dipetakan");
        check(prestasiSources.contains("ais.action.master.dashboard.admin.DashboardKegiatanKedosenanAdmin"),
                "dashboard prestasi dosen admin belum dipetakan");
        check(prestasiSources.contains("ais.action.master.dashboard.sekolah.DashboardKegiatanKesiswaanAdmin"),
                "dashboard prestasi siswa admin belum dipetakan");
        System.out.println("NewUiModuleFunctionServiceSelfTest OK modules="
                + TOOLBAR_KEYS.length + " functions=" + total);
    }
}
