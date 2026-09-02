package ais.common.newui.menu;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Route menu yang tidak boleh ditebak atau diarahkan ke proses bisnis lain.
 *
 * <p>Registry ini berbeda dari {@link NewUiRuteEksplisitRegistry}: registry
 * tersebut memilih halaman pengganti yang sudah dibuktikan setara, sedangkan
 * kelas ini mencatat route yang sumber fungsinya memang sudah dihentikan atau
 * tidak ada di basis kode. Menampilkan alasan yang tegas lebih aman daripada
 * layar scaffold kosong, terlebih untuk transaksi keuangan.</p>
 */
public final class NewUiUnavailableRouteRegistry {

    public static final String RETIRED = "FUNCTION_RETIRED";
    public static final String SOURCE_NOT_AVAILABLE = "FUNCTION_SOURCE_NOT_AVAILABLE";

    /** Penjelasan stabil yang aman ditampilkan kepada pengguna dan klien API. */
    public static final class Entry {
        private final String code;
        private final String title;
        private final String message;
        private final String evidence;
        private final int httpStatus;

        Entry(String code, String title, String message, String evidence, int httpStatus) {
            this.code = code;
            this.title = title;
            this.message = message;
            this.evidence = evidence;
            this.httpStatus = httpStatus;
        }

        public String getCode() { return code; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getEvidence() { return evidence; }
        public int getHttpStatus() { return httpStatus; }
    }

    private static final Map<String, Entry> ROUTES;

    static {
        Map<String, Entry> routes = new LinkedHashMap<String, Entry>();

        Entry beasiswa = new Entry(RETIRED, "Fungsi pembayaran beasiswa telah dihentikan",
                "Gunakan alur pembayaran mahasiswa dan pengaturan biaya/diskon yang berlaku. "
                + "Fungsi lama tidak diaktifkan kembali karena aturan transaksinya sudah tidak digunakan.",
                "DaftarUlangMahasiswaLamaBeasiswaAction dan varian pengembalian dihapus pada SVN r7511 "
                + "(1 September 2016); ZUL yang tersisa kemudian turut dihapus.", 410);
        routes.put("/pages/master/daftarulang_mahasiswa_lama_beasiswa.zul", beasiswa);
        routes.put("/pages/master/daftarulang_mahasiswa_lama_pengembalian_beasiswa.zul", beasiswa);

        Entry sirs = new Entry(SOURCE_NOT_AVAILABLE, "Sumber fungsi posting SIRS tidak tersedia",
                "Menu ini berasal dari pemasangan SIRS lain atau data menu lama. Hubungi administrator "
                + "untuk memperbaiki atau menonaktifkan entri menu; sistem tidak akan mengarahkannya ke transaksi lain.",
                "Tidak ada ZUL, action, controller native, maupun padanan proses bisnis untuk tiga URL posting SIRS "
                + "di repository ini.", 501);
        routes.put("/pages/master/sirs/akunting/posting_transaksi_deposit.zul", sirs);
        routes.put("/pages/master/sirs/akunting/posting_transaksi_kasir.zul", sirs);
        routes.put("/pages/master/sirs/akunting/posting_transaksi_penerimaan_order.zul", sirs);

        ROUTES = Collections.unmodifiableMap(routes);
    }

    private NewUiUnavailableRouteRegistry() { }

    public static Entry find(String route) {
        if (route == null) return null;
        String normalized = route.trim();
        int query = normalized.indexOf('?');
        if (query >= 0) normalized = normalized.substring(0, query);
        int fragment = normalized.indexOf('#');
        if (fragment >= 0) normalized = normalized.substring(0, fragment);
        return ROUTES.get(normalized);
    }

    public static Map<String, Entry> all() {
        return ROUTES;
    }
}
