package ais.action.master.generic.v2;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;
import javax.servlet.http.HttpSession;

/**
 * Utilitas proteksi CSRF (Cross-Site Request Forgery) khusus framework generic-CRUD-v2. Token
 * acak ({@link UUID}) disimpan per sesi HTTP di bawah kunci atribut {@code generic_crud_v2_csrf}
 * dan wajib disertakan pada setiap permintaan mutasi (create/update/delete/import) baik sebagai
 * parameter {@code nui_csrf} maupun header {@code X-NUI-CSRF}. Kelas ini murni statis (tidak ada
 * instance) dan tidak menyimpan state di luar sesi HTTP.
 */
public final class GenericCrudCsrf {
    private GenericCrudCsrf() { }
    private static final String KEY = "generic_crud_v2_csrf";
    /**
     * Mengambil token CSRF yang sudah ada pada sesi {@code request}, atau membuat dan menyimpan
     * token baru (UUID acak tanpa tanda hubung) bila sesi belum memilikinya. Selalu membuat sesi
     * baru bila belum ada ({@code getSession(true)}).
     *
     * @param request permintaan HTTP saat ini; bila {@code null} mengembalikan string kosong
     * @return token CSRF untuk sesi ini, tidak pernah {@code null} (kecuali {@code request} null)
     */
    public static String token(HttpServletRequest request) {
        if (request == null) return "";
        HttpSession session = request.getSession(true);
        Object value = session.getAttribute(KEY);
        if (value instanceof String && ((String) value).length() > 0) return (String) value;
        String created = UUID.randomUUID().toString().replace("-", "");
        session.setAttribute(KEY, created);
        return created;
    }
    /**
     * Menjaga bahwa permintaan mutasi (create/update/delete/import) pada CRUD generik memenuhi dua
     * syarat: method HTTP harus {@code POST}, dan token CSRF yang disertakan pemanggil (parameter
     * {@code nui_csrf} atau header {@code X-NUI-CSRF}) harus cocok persis dengan token yang
     * tersimpan di sesi. Dipanggil di awal setiap operasi mutasi CRUD generik sebelum data apa pun
     * diubah.
     *
     * @param request permintaan HTTP yang akan diperiksa
     * @throws GenericCrudException status 405 bila bukan POST, atau 403 bila token CSRF tidak ada/tidak cocok
     */
    public static void requireMutation(HttpServletRequest request) throws GenericCrudException {
        if (request == null || !"POST".equalsIgnoreCase(request.getMethod())) {
            throw new GenericCrudException(405, "POST_REQUIRED", "Mutasi hanya diizinkan melalui POST.");
        }
        HttpSession session = request.getSession(false);
        Object expected = session == null ? null : session.getAttribute(KEY);
        String supplied = request.getParameter("nui_csrf");
        if (supplied == null || supplied.length() == 0) supplied = request.getHeader("X-NUI-CSRF");
        if (!(expected instanceof String) || !expected.equals(supplied)) {
            throw new GenericCrudException(403, "CSRF_INVALID", "Token CSRF tidak valid.");
        }
    }
}
