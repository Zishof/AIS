package ais.common;

import ais.database.model.Konfigurasi;

/**
 * Pusat pembacaan konfigurasi e-Learning agar teks dan perilaku login/logout
 * mudah diatur dari KonfigurasiNewAction tanpa mengubah JSP/Java dashboard.
 */
public final class ElearningConfigUtil {
    public static final String KEY_LOGIN_GUNAKAN_COOKIE = "elearning_login_gunakan_cookie";
    public static final String KEY_LOGOUT_HAPUS_COOKIE = "elearning_logout_hapus_cookie";
    public static final String KEY_COOKIE_HARI = "elearning_login_cookie_hari";

    private ElearningConfigUtil() {
    }

    public static String text(String key, String defaultValue) {
        try {
            Konfigurasi konfigurasi = Common.getKonfigurasi(key, defaultValue);
            if (konfigurasi == null || konfigurasi.getNilai() == null || konfigurasi.getNilai().trim().length() == 0) {
                return defaultValue;
            }
            return konfigurasi.getNilai().trim();
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    public static boolean aktif(String key, String defaultValue) {
        return Konfigurasi.AKTIF.equalsIgnoreCase(text(key, defaultValue));
    }

    public static boolean loginMenggunakanCookie() {
        return aktif(KEY_LOGIN_GUNAKAN_COOKIE, Konfigurasi.TIDAK_AKTIF);
    }

    public static boolean logoutMenghapusCookie() {
        return aktif(KEY_LOGOUT_HAPUS_COOKIE, Konfigurasi.AKTIF);
    }

    public static int cookieHari() {
        try {
            int hari = Integer.parseInt(text(KEY_COOKIE_HARI, "0"));
            return hari < 0 ? 0 : hari;
        } catch (Throwable t) {
            return 0;
        }
    }
}
