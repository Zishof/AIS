package ais.common;

/**
 * Konfigurasi terpusat untuk Portal Vendor.
 * Class ini ringan, kompatibel Java 1.7, dan hanya membaca nilai dari tabel konfigurasi melalui Common.getKonfigurasi.
 */
public final class VendorPortalConfig {

    private VendorPortalConfig() {
    }

    public static final String LOGIN_GUNAKAN_COOKIE = "vendor_login_gunakan_cookie";
    public static final String LOGIN_COOKIE_HARI = "vendor_login_cookie_hari";

    public static String nilai(String key, String defaultValue) {
        try {
            return Common.getKonfigurasi(key, defaultValue).getNilai();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean aktif(String key, String defaultValue) {
        String v = nilai(key, defaultValue);
        return "Aktif".equalsIgnoreCase(v) || "Ya".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v);
    }

    public static boolean gunakanCookieLoginVendor() {
        return aktif(LOGIN_GUNAKAN_COOKIE, "Tidak Aktif");
    }

    public static int masaCookieHari() {
        try {
            return Integer.parseInt(nilai(LOGIN_COOKIE_HARI, "7").trim());
        } catch (Exception e) {
            return 7;
        }
    }
}
