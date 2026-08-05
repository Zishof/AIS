package ais.action.servlet.api;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sisdes.Penduduk;

/**
 * Manager token API yang bisa direuse oleh servlet/helper lain.
 *
 * Tanggung jawab class ini:
 * - menyimpan cache token secara thread-safe (IN-JVM, {@link ConcurrentHashMap});
 * - load token awal dari MemoryCache/DB.
 *
 * <p>Replikasi token antar-node via Hazelcast telah DIHAPUS (deploy 1 JVM). Token disimpan lokal;
 * bila kelak perlu multi-node, gunakan mekanisme lain — Hazelcast tidak lagi menjadi dependensi.</p>
 */
public final class ApiTokenManager {

    private static final Object TOKEN_INIT_LOCK = new Object();

    public static final Map<String, Object> tokens = new ConcurrentHashMap<String, Object>();

    private static volatile boolean tokenDbLoaded = false;

    private ApiTokenManager() {
    }

    @SuppressWarnings("unchecked")
    public static void initTokens() {
        if (!tokenDbLoaded || tokens.isEmpty()) {
            synchronized (TOKEN_INIT_LOCK) {
                if (!tokenDbLoaded || tokens.isEmpty()) {
                    Object cacheObject = ais.common.MemoryCacheUtil.get("tokens");
                    if (cacheObject instanceof Map) {
                        try {
                            tokens.putAll((Map<String, Object>) cacheObject);
                        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiTokenManager.java:50");
                        }
                    }

                    loadTokensFromDb(Mahasiswa.class);
                    loadTokensFromDb(Siswa.class);
                    loadTokensFromDb(Penduduk.class);
                    loadTbmuserTokens();
                    tokenDbLoaded = true;
                }
            }
        }
    }

    public static void putToken(String token, Object userObject) {
        if (!ApiHelperSupport.hasText(token) || userObject == null) {
            return;
        }
        tokens.put(token, userObject);
    }

    public static void removeToken(String token) {
        if (!ApiHelperSupport.hasText(token)) {
            return;
        }
        tokens.remove(token);
    }

    public static Object getTokenValue(String token) {
        if (!ApiHelperSupport.hasText(token)) {
            return null;
        }
        return tokens.get(token);
    }

    public static boolean containsToken(String token) {
        return ApiHelperSupport.hasText(token) && tokens.containsKey(token);
    }

    public static void clearLocalTokens() {
        tokens.clear();
        tokenDbLoaded = false;
    }

    @SuppressWarnings("unchecked")
    private static void loadTbmuserTokens() {
        // Sebelumnya membaca dari cache (ambilBerdasarClass) — tidak lagi andal karena
        // Tbmuser TIDAK di-preload penuh saat bootstrap. Baca langsung dari DB hanya untuk
        // user yang memiliki token (subset kecil) agar token login API lengkap.
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List<Tbmuser> users = session.createCriteria(Tbmuser.class)
                    .add(Restrictions.isNotNull("token")).add(Restrictions.ne("token", ""))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE))).list();
            if (users == null || users.isEmpty()) {
                return;
            }
            for (Tbmuser user : users) {
                if (user != null && ApiHelperSupport.hasText(user.getToken())) {
                    tokens.put(user.getToken(), user);
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiTokenManager.java:113");
        } finally {
            ApiHelperSupport.closeOpenedSession(session);
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadTokensFromDb(Class<?> clazz) {
        if (clazz == null) {
            return;
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Criteria criteria = session.createCriteria(clazz);
            if (clazz.equals(Siswa.class)) {
                criteria.add(Restrictions.isNotNull("namaSiswa"));
                criteria.add(Restrictions.ne("namaSiswa", ""));
                criteria.add(Restrictions.isNotNull("sekolah"));
            } else if (clazz.equals(Penduduk.class)) {
                criteria.add(Restrictions.isNotNull("nama"));
                criteria.add(Restrictions.ne("nama", ""));
            }

            criteria.add(Restrictions.isNotNull("token"));
            criteria.add(Restrictions.ne("token", ""));
            if (ApiHelperSupport.hasGetter(clazz, "getAktif")) {
                criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
            }
            criteria.setProjection(Projections.property("id"));

            List<Long> ids = criteria.list();
            if (ids == null || ids.isEmpty()) {
                return;
            }

            for (Long id : ids) {
                loadSingleToken(clazz, id, session);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiTokenManager.java:152");
        } finally {
            ApiHelperSupport.closeOpenedSession(session);
        }
    }

    private static void loadSingleToken(Class<?> clazz, Long id, Session session) {
        if (clazz == null || id == null || session == null) {
            return;
        }
        try {
            Object obj = ConstantValues.ambil(clazz.getName(), id, true, session);
            if (obj == null) {
                return;
            }
            Method method = clazz.getMethod("getToken", new Class[0]);
            Object tokenObject = method.invoke(obj, new Object[0]);
            String token = tokenObject == null ? null : String.valueOf(tokenObject);
            if (ApiHelperSupport.hasText(token)) {
                tokens.put(token, obj);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiTokenManager.java:173");
        }
    }
}
