package ais.common.newui;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONException;
import org.json.JSONObject;

import ais.action.servlet.Main;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.SecurityFilter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;

/** Shared authentication adapter for public AIS portals. */
public final class PortalLoginApi {
    private PortalLoginApi() { }

    public static JSONObject handle(HttpServletRequest request, HttpServletResponse response, String namespace)
            throws JSONException {
        String scope = namespace == null || !namespace.matches("[a-z][a-z0-9.-]{0,31}") ? "portal" : namespace;
        String failuresKey = scope + ".login.failures";
        String lockedKey = scope + ".login.lockedUntil";
        if (!"POST".equalsIgnoreCase(request.getMethod())) return error("Metode permintaan tidak diizinkan.");
        if (!NewUiCsrfUtil.isValid(request)) return error("Token keamanan tidak valid. Muat ulang halaman.");
        HttpSession httpSession = request.getSession();
        long now = System.currentTimeMillis();
        Object locked = httpSession.getAttribute(lockedKey);
        if (locked instanceof Long && ((Long) locked).longValue() > now)
            return error("Terlalu banyak percobaan. Tunggu satu menit lalu coba lagi.");

        String username = trim(request.getParameter("username"), 120);
        String password = request.getParameter("password");
        if (username == null || password == null || password.length() == 0 || password.length() > 512)
            return error("Nama pengguna dan kata sandi wajib diisi.");
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            String encrypted = Common.desEncrypter.get().encrypt(password);
            Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(session.createCriteria(Mahasiswa.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.eq("nim", username)).add(Restrictions.eq("pass", encrypted)).setMaxResults(1), Mahasiswa.class);
            Siswa siswa = null;
            Tbmuser user = null;
            if (mahasiswa == null) siswa = (Siswa) ConstantValues.simpleObject(session.createCriteria(Siswa.class)
                    .add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
                    .add(Restrictions.isNotNull("sekolah")).add(Restrictions.eq("nomorInduk", username))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.eq("pass", encrypted)).setMaxResults(1), Siswa.class);
            if (mahasiswa == null && siswa == null) user = (Tbmuser) ConstantValues.simpleObject(session.createCriteria(Tbmuser.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.idEq(username)).add(Restrictions.eq("userPassword", encrypted)).setMaxResults(1), Tbmuser.class);

            String identity = null;
            String clearPassword = null;
            if (user != null) { identity = user.getUserId(); clearPassword = Common.desEncrypter.get().decrypt(user.getUserPassword()); }
            else if (mahasiswa != null) { identity = mahasiswa.getNim(); clearPassword = Common.desEncrypter.get().decrypt(mahasiswa.getPass()); }
            else if (siswa != null) { identity = siswa.getNomorIndukNasional() != null ? siswa.getNomorIndukNasional() : siswa.getNomorInduk(); clearPassword = Common.desEncrypter.get().decrypt(siswa.getPass()); }
            if (identity == null || !SecurityFilter.doAutoLogin(identity, clearPassword, false, "", request, response)) {
                failed(httpSession, failuresKey, lockedKey, now);
                return error("Nama pengguna atau kata sandi tidak valid.");
            }
            Main.checkAndSetUserSession(request, true);
            httpSession.removeAttribute(failuresKey);
            httpSession.removeAttribute(lockedKey);
            return new JSONObject().put("ok", true).put("status", "success").put("message", "Otentikasi berhasil.");
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            failed(httpSession, failuresKey, lockedKey, now);
            return error("Otentikasi belum dapat diproses.");
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static void failed(HttpSession session, String failuresKey, String lockedKey, long now) {
        Object value = session.getAttribute(failuresKey);
        int attempts = value instanceof Integer ? ((Integer) value).intValue() + 1 : 1;
        session.setAttribute(failuresKey, Integer.valueOf(attempts));
        if (attempts >= 5) {
            session.setAttribute(lockedKey, Long.valueOf(now + 60000L));
            session.setAttribute(failuresKey, Integer.valueOf(0));
        }
    }

    private static JSONObject error(String message) throws JSONException {
        return new JSONObject().put("ok", false).put("status", "error").put("message", message);
    }

    private static String trim(String value, int max) {
        if (value == null || value.trim().length() == 0) return null;
        value = value.trim();
        return value.length() > max ? value.substring(0, max) : value;
    }
}
