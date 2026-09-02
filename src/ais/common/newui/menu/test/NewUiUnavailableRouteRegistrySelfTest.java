package ais.common.newui.menu.test;

import java.util.Map;

import ais.common.newui.menu.NewUiUnavailableRouteRegistry;
import ais.common.newui.menu.NewUiUnavailableRouteRegistry.Entry;

/** Uji luring agar route keuangan tanpa sumber tidak dipetakan secara diam-diam. */
public final class NewUiUnavailableRouteRegistrySelfTest {

    private static int failures;

    public static void main(String[] args) {
        Map<String, Entry> all = NewUiUnavailableRouteRegistry.all();
        check(all.size() == 5, "jumlah route harus tepat lima, ditemukan " + all.size());

        int retired = 0;
        int missing = 0;
        for (Map.Entry<String, Entry> item : all.entrySet()) {
            Entry value = item.getValue();
            check(item.getKey().startsWith("/pages/master/"), item.getKey() + ": route tidak kanonik");
            check(value.getTitle() != null && value.getTitle().length() > 0, item.getKey() + ": judul kosong");
            check(value.getMessage() != null && value.getMessage().length() > 0, item.getKey() + ": pesan kosong");
            check(value.getEvidence() != null && value.getEvidence().length() > 0, item.getKey() + ": bukti kosong");
            check(value.getHttpStatus() >= 400, item.getKey() + ": status HTTP harus gagal-tertutup");
            if (NewUiUnavailableRouteRegistry.RETIRED.equals(value.getCode())) retired++;
            if (NewUiUnavailableRouteRegistry.SOURCE_NOT_AVAILABLE.equals(value.getCode())) missing++;
        }
        check(retired == 2, "route retired harus dua, ditemukan " + retired);
        check(missing == 3, "route tanpa sumber harus tiga, ditemukan " + missing);
        check(NewUiUnavailableRouteRegistry.find(
                "/pages/master/daftarulang_mahasiswa_lama_beasiswa.zul?x=1#y") != null,
                "query/fragment harus dinormalisasi");
        check(NewUiUnavailableRouteRegistry.find("/pages/master/mahasiswa.zul") == null,
                "route aktif tidak boleh ikut ditahan");

        if (failures > 0) {
            System.out.println("FAIL NewUiUnavailableRouteRegistrySelfTest: " + failures);
            System.exit(1);
        }
        System.out.println("PASS NewUiUnavailableRouteRegistrySelfTest (2 retired, 3 tanpa sumber)");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            failures++;
            System.out.println("  - " + message);
        }
    }
}
