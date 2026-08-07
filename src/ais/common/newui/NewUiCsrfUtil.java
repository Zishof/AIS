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
