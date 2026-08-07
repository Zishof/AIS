package ais.action.master.generic.v2;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;
import javax.servlet.http.HttpSession;

public final class GenericCrudCsrf {
    private GenericCrudCsrf() { }
    private static final String KEY = "generic_crud_v2_csrf";
    public static String token(HttpServletRequest request) {
        if (request == null) return "";
        HttpSession session = request.getSession(true);
        Object value = session.getAttribute(KEY);
        if (value instanceof String && ((String) value).length() > 0) return (String) value;
        String created = UUID.randomUUID().toString().replace("-", "");
        session.setAttribute(KEY, created);
        return created;
    }
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
