package ais.common.newui;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Token CSRF per-session untuk mutasi New UI (save/delete/toggle/switch-role).
 *
 * <p>Token disimpan di session dan wajib disertakan pada setiap POST mutasi
 * (parameter <code>nui_csrf</code> atau header <code>X-NUI-CSRF</code>).
 * GET tidak boleh dipakai untuk mutasi.</p>
 *
 * <p>Kompatibel Java 1.6 (UUID tersedia sejak 1.5).</p>
 */
public final class NewUiCsrfUtil {

    public static final String SESSION_KEY = "nui_csrf";
    public static final String PARAM = "nui_csrf";
    public static final String HEADER = "X-NUI-CSRF";

    /** Namespace CSRF keluarga controller beramplop {@code ok}. */
    public static final String LEGACY_SESSION_KEY = "newUiCsrfToken";
    public static final String LEGACY_HEADER = "X-CSRF-Token";

    private NewUiCsrfUtil() {
    }

    /** Ambil token session; buat bila belum ada. Panggil saat merender form/halaman. */
    public static String getToken(HttpSession session) {
        if (session == null) {
            return "";
        }
        Object existing = session.getAttribute(SESSION_KEY);
        if (existing instanceof String && ((String) existing).length() > 0) {
            return (String) existing;
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        session.setAttribute(SESSION_KEY, token);
        return token;
    }

    /**
     * Terbitkan token untuk keluarga controller beramplop {@code ok}.
     *
     * <p>Keluarga itu memvalidasi CSRF dengan atribut sesi
     * {@code newUiCsrfToken} dan header {@code X-CSRF-Token} — namespace yang
     * terpisah dari {@link #SESSION_KEY}/{@link #HEADER} di kelas ini. Yang
     * memvalidasinya ada enam belas controller, sedangkan yang menerbitkannya
     * semula hanya lima; sisanya hanya berfungsi bila pengguna kebetulan
     * membuka salah satu dari lima layar itu lebih dulu pada sesi yang sama.
     * Ketergantungan antarlayar itu tidak pernah dinyatakan, dan tidak berlaku
     * sama sekali pada klien native yang membuka menunya langsung.</p>
     *
     * <p>Nilainya diambil dari token sesi yang sama supaya kedua namespace
     * tidak berbeda isi, lalu dipasang sekali (get-or-set) agar pemanggilan
     * berikutnya tidak menggeser token yang sedang dipakai klien.</p>
     */
    public static String getTokenOkFlat(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        HttpSession session = request.getSession(true);
        String token = getToken(session);
        Object ada = session.getAttribute(LEGACY_SESSION_KEY);
        if (ada == null || String.valueOf(ada).length() == 0) {
            session.setAttribute(LEGACY_SESSION_KEY, token);
            return token;
        }
        return String.valueOf(ada);
    }

    /** true bila token pada request cocok dengan token session. */
    public static boolean isValid(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expectedObj = session.getAttribute(SESSION_KEY);
        if (!(expectedObj instanceof String)) {
            return false;
        }
        String expected = (String) expectedObj;
        if (expected.length() == 0) {
            return false;
        }
        String provided = request.getParameter(PARAM);
        if (provided == null || provided.length() == 0) {
            provided = request.getHeader(HEADER);
        }
        return expected.equals(provided);
    }
}
