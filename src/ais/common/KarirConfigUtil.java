package ais.common;

import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.recruitment.CalonPegawai;

/**
 * Utilitas kecil untuk Portal KARIR.
 * Dibuat terpisah agar JSP tetap ringan, teks mudah dikonfigurasi,
 * dan kebijakan cookie login/logout dapat diaktifkan atau dimatikan dari Konfigurasi.
 * Kompatibel Java 1.6/1.7.
 */
public final class KarirConfigUtil {

    public static final String SESSION_CALON = "KARIR_LOGGED_IN";
    public static final String SESSION_USER = "KARIR_USER_LOGGED_IN";
    public static final String COOKIE_TOKEN = "KARIR_LOGIN_TOKEN";
    public static final String COOKIE_USER = "KARIR_LOGIN_USER";

    private KarirConfigUtil() {
    }

    public static String text(String key, String defaultValue) {
        try {
            return Common.getKonfigurasi(key, defaultValue == null ? "" : defaultValue).getNilai();
        } catch (Exception e) {
            return defaultValue == null ? "" : defaultValue;
        }
    }

    public static boolean active(String key, boolean defaultActive) {
        String def = defaultActive ? Konfigurasi.AKTIF : Konfigurasi.TIDAK_AKTIF;
        String value = text(key, def);
        return value != null && value.trim().equalsIgnoreCase(Konfigurasi.AKTIF);
    }

    public static boolean useLoginCookie() {
        return active("karir_login_logout_menggunakan_cookie", false);
    }

    public static int cookieMaxAgeSeconds() {
        int days = 7;
        try {
            String value = text("karir_cookie_login_max_age_hari", "7");
            days = Integer.parseInt(value.trim());
        } catch (Exception e) {
            days = 7;
        }
        if (days < 1) {
            days = 1;
        }
        if (days > 30) {
            days = 30;
        }
        return days * 24 * 60 * 60;
    }

    public static void putKarirSession(HttpServletRequest request, Tbmuser user, CalonPegawai calon) {
        if (request == null || calon == null) {
            return;
        }
        try {
            request.getSession().setAttribute(SESSION_CALON, calon);
            request.getSession().setAttribute("CalonPegawai", calon);
            if (user != null) {
                request.getSession().setAttribute(SESSION_USER, user);
                request.getSession().setAttribute("mytbmuser", user);
                request.getSession().setAttribute("usersTemp", user);
                request.getSession().setAttribute("user", user);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/KarirConfigUtil.java:82");
        }
    }

    public static void clearKarirSession(HttpServletRequest request) {
        if (request == null) {
            return;
        }
        try {
            request.getSession().removeAttribute(SESSION_CALON);
            request.getSession().removeAttribute(SESSION_USER);
            request.getSession().removeAttribute("CalonPegawai");
            request.getSession().removeAttribute("mytbmuser");
            request.getSession().removeAttribute("usersTemp");
            request.getSession().removeAttribute("user");
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/KarirConfigUtil.java:97");
        }
    }

    public static void attachLoginCookies(HttpServletRequest request, HttpServletResponse response, Tbmuser user,
            CalonPegawai calon) {
        if (!useLoginCookie() || request == null || response == null || user == null || calon == null
                || calon.getId() == null) {
            return;
        }
        try {
            String raw = calon.getId() + "|" + user.getUserId() + "|" + System.currentTimeMillis();
            String encrypted = Common.desEncrypter.get().encrypt(raw);
            addCookie(request, response, COOKIE_TOKEN, encrypted, cookieMaxAgeSeconds(), true);
            addCookie(request, response, COOKIE_USER, user.getUserId(), cookieMaxAgeSeconds(), false);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/KarirConfigUtil.java:112");
        }
    }

    public static void clearLoginCookies(HttpServletRequest request, HttpServletResponse response) {
        if (request == null || response == null) {
            return;
        }
        addCookie(request, response, COOKIE_TOKEN, "", 0, true);
        addCookie(request, response, COOKIE_USER, "", 0, false);
    }

    private static void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value,
            int maxAge, boolean httpOnly) {
        try {
            Cookie c = new Cookie(name, URLEncoder.encode(value == null ? "" : value, "UTF-8"));
            c.setPath(request.getContextPath() == null || request.getContextPath().length() == 0 ? "/"
                    : request.getContextPath());
            c.setMaxAge(maxAge);
            applyHttpOnlyCompat(c, httpOnly);
            try {
                c.setSecure(request.isSecure());
            } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/KarirConfigUtil.java:134");
            }
            response.addCookie(c);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/KarirConfigUtil.java:137");
        }
    }

    /**
     * Servlet API lama yang dipakai Java 1.6/1.7 pada beberapa project belum
     * memiliki method Cookie#setHttpOnly(boolean). Pemanggilan langsung akan
     * gagal compile. Reflection membuat file tetap kompatibel; jika runtime
     * belum mendukung HttpOnly, cookie tetap dikirim dengan aman tanpa
     * menghentikan proses login/logout Karir.
     */
    private static void applyHttpOnlyCompat(Cookie cookie, boolean httpOnly) {
        if (cookie == null || !httpOnly) {
            return;
        }
        try {
            java.lang.reflect.Method method = Cookie.class.getMethod("setHttpOnly",
                    new Class[] { Boolean.TYPE });
            method.invoke(cookie, new Object[] { Boolean.TRUE });
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/KarirConfigUtil.java:156");
        }
    }

    private static String readCookie(HttpServletRequest request, String name) {
        try {
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return "";
            }
            for (int i = 0; i < cookies.length; i++) {
                Cookie c = cookies[i];
                if (c != null && name.equals(c.getName())) {
                    return URLDecoder.decode(c.getValue() == null ? "" : c.getValue(), "UTF-8");
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/KarirConfigUtil.java:172");
        }
        return "";
    }

    public static CalonPegawai resolveLoggedCandidate(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        try {
            Object existing = request.getSession().getAttribute(SESSION_CALON);
            if (existing instanceof CalonPegawai) {
                return (CalonPegawai) existing;
            }
            existing = request.getSession().getAttribute("CalonPegawai");
            if (existing instanceof CalonPegawai) {
                request.getSession().setAttribute(SESSION_CALON, existing);
                return (CalonPegawai) existing;
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/KarirConfigUtil.java:191");
        }

        if (!useLoginCookie()) {
            return null;
        }

        String token = readCookie(request, COOKIE_TOKEN);
        if (token == null || token.trim().length() == 0) {
            return null;
        }

        Session session = null;
        try {
            String raw = Common.desEncrypter.get().decrypt(token);
            String[] parts = raw == null ? new String[0] : raw.split("\\|");
            if (parts.length < 2) {
                return null;
            }
            Long calonId = Long.valueOf(parts[0]);
            String userId = parts[1];
            session = HibernateUtil.currentNativeSession();
            CalonPegawai calon = (CalonPegawai) session.get(CalonPegawai.class, calonId);
            if (calon == null || !calon.getAktif()) {
                return null;
            }
            Tbmuser user = (Tbmuser) session.createCriteria(Tbmuser.class)
                    .add(Restrictions.eq("calonPegawai", calon))
                    .add(Restrictions.eq("userId", userId))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .setMaxResults(1).uniqueResult();
            if (user == null) {
                return null;
            }
            putKarirSession(request, user, calon);
            return calon;
        } catch (Exception e) {
            return null;
        } finally {
            closeNativeSession(session);
        }
    }

    public static void closeNativeSession(Session session) {
        try {
            HibernateUtil.closeSessionQuietly(session);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/KarirConfigUtil.java:237");
        }

        // FIX double-cleanup: closeSessionQuietly(session) di atas SUDAH melakukan
        // clear+rollback+disconnect+close secara penuh & idempoten (lihat javadoc
        // HibernateUtil). Mengulang clear/disconnect/close di sini pada session yang
        // sudah tertutup SELALU melempar SessionException ("Session is closed!" /
        // "Session was already closed") pada setiap pemanggilan -- guard isOpen()
        // dulu agar blok ini jadi no-op aman, bukan noise error rutin.
        if (session != null && session.isOpen()) {
            try {
                session.clear();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/KarirConfigUtil.java:242");
            }
            try {
                session.disconnect();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/KarirConfigUtil.java:246");
            }
            try {
                session.close();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/KarirConfigUtil.java:250");
            }
        }
        try {
            HibernateUtil.closeSession();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/KarirConfigUtil.java:254");
        }
    }
}
