package ais.action.servlet.api;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONException;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;

/**
 * Utility ringan untuk kebutuhan umum API mobile/web service.
 *
 * Catatan kompatibilitas:
 * - Tidak memakai lambda / try-with-resources agar tetap aman untuk Java 1.7.
 * - Session yang dikirim ke closeOpenedSession(...) diasumsikan berasal dari openSession()
 *   atau currentNativeSession(). Jangan gunakan method ini untuk HibernateUtil.currentSession().
 */
public final class ApiHelperSupport {

    public static final String STATUS_OK = "00";
    public static final String STATUS_EMPTY = "99";
    public static final String STATUS_ERROR = "500";

    private static final int DEFAULT_MAX_BODY_CHARS = 2 * 1024 * 1024;
    private static final int DEFAULT_MAX_LOG_CHARS = 60000;

    private ApiHelperSupport() {
    }

    public static boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static String lowerAction(String action) {
        return action == null ? "" : action.trim().toLowerCase(java.util.Locale.ENGLISH);
    }

    public static String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static String truncate(String value) {
        return truncate(value, DEFAULT_MAX_LOG_CHARS);
    }

    public static String truncate(String value, int maxChars) {
        if (value == null) {
            return null;
        }
        if (maxChars <= 0 || value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars);
    }

    public static JSONObject defaultResponse() {
        return status(STATUS_EMPTY, "Tidak ada informasi yang bisa ditampilkan");
    }

    public static JSONObject errorResponse(String description) {
        return status(STATUS_ERROR, description == null ? "Terjadi kesalahan internal server" : description);
    }

    public static JSONObject status(String status, String description) {
        JSONObject object = new JSONObject();
        try {
            object.put("status", status);
            object.put("description", description == null ? "" : description);
        } catch (JSONException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiHelperSupport.java:78");
        }
        return object;
    }


    public static void put(JSONObject object, String key, Object value) {
        if (object == null || key == null) {
            return;
        }
        try {
            object.put(key, value);
        } catch (JSONException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiHelperSupport.java:90");
        }
    }

    public static void putStatus(JSONObject object, String status, String description) {
        put(object, "status", status);
        put(object, "description", description == null ? "" : description);
    }

    public static void putSuccess(JSONObject object, String description) {
        putStatus(object, STATUS_OK, description);
    }

    public static void putEmpty(JSONObject object, String description) {
        putStatus(object, STATUS_EMPTY, description);
    }

    public static void putError(JSONObject object, String description) {
        putStatus(object, STATUS_ERROR, description);
    }

    public static String optString(JSONObject object, String key) {
        if (object == null || key == null) {
            return "";
        }
        try {
            if (object.isNull(key)) {
                return "";
            }
            return object.getString(key);
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isNullOrEmptyJsonValue(JSONObject object, String key) {
        return object == null || key == null || object.isNull(key) || !hasText(optString(object, key));
    }


    public static interface SessionCallback {
        Object doInSession(Session session) throws Exception;
    }

    public static interface VoidSessionCallback {
        void doInSession(Session session) throws Exception;
    }

    public static Object withOpenedSession(Session session, SessionCallback callback) throws Exception {
        try {
            return callback == null ? null : callback.doInSession(session);
        } finally {
            closeOpenedSession(session);
        }
    }

    public static void withOpenedSession(Session session, VoidSessionCallback callback) throws Exception {
        try {
            if (callback != null) {
                callback.doInSession(session);
            }
        } finally {
            closeOpenedSession(session);
        }
    }
    public static void closeCurrentNativeSession() {
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            try {
            } finally {
            	HibernateUtil.closeSessionQuietly(session);
            }
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiHelperSupport.java:163");
        }
        closeOpenedSession(session);
    }

    public static void closeOpenedSession(Session session) {
        closeOpenedSession(session, true);
    }

    public static void closeOpenedSession(Session session, boolean closeHibernateUtilSession) {
        if (session != null) {
            try {
                if (session.isOpen()) {
                    try {
                        session.clear();
                    } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiHelperSupport.java:178");
                    }
                    try {
                        session.disconnect();
                    } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiHelperSupport.java:182");
                    }
                    try {
                        session.close();
                    } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiHelperSupport.java:186");
                    }
                }
            } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiHelperSupport.java:189");
            }
        }
        if (closeHibernateUtilSession) {
        }
    }

    public static void rollbackQuietly(Transaction transaction) {
        if (transaction != null) {
            try {
                if (!transaction.wasCommitted() && !transaction.wasRolledBack()) {
                    transaction.rollback();
                }
            } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiHelperSupport.java:202");
            }
        }
    }

    public static void commit(Transaction transaction) {
        if (transaction != null) {
            transaction.commit();
        }
    }

    public static String readBody(HttpServletRequest request) throws IOException {
        return readBody(request, DEFAULT_MAX_BODY_CHARS);
    }

    public static String readBody(HttpServletRequest request, int maxChars) throws IOException {
        if (request == null) {
            return "";
        }
        BufferedReader reader = null;
        StringBuilder buffer = new StringBuilder(1024);
        try {
            reader = request.getReader();
            char[] data = new char[4096];
            int read;
            int total = 0;
            while ((read = reader.read(data)) != -1) {
                total += read;
                if (maxChars > 0 && total > maxChars) {
                    throw new IOException("Ukuran body request melebihi batas maksimum " + maxChars + " karakter");
                }
                buffer.append(data, 0, read);
            }
            return buffer.toString();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiHelperSupport.java:240");
                }
            }
        }
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String ip = firstHeader(request, "Cf-Connecting-Ip");
        if (!hasText(ip)) {
            ip = firstHeader(request, "CF-Connecting-IP");
        }
        if (!hasText(ip)) {
            ip = firstHeader(request, "X-Forwarded-For");
        }
        if (hasText(ip) && ip.indexOf(',') >= 0) {
            ip = ip.substring(0, ip.indexOf(',')).trim();
        }
        if (!hasText(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private static String firstHeader(HttpServletRequest request, String name) {
        try {
            String value = request.getHeader(name);
            return value == null ? "" : value.trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean hasGetter(Class<?> clazz, String getterName) {
        if (clazz == null || getterName == null) {
            return false;
        }
        try {
            clazz.getMethod(getterName, new Class[0]);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
