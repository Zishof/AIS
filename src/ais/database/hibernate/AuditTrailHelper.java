package ais.database.hibernate;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.SaveOrUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import ais.common.Common;
import ais.database.model.AccessedUsers;
import ais.database.model.DetailLogLogin;
import ais.database.model.ErrorLog;
import ais.database.model.GeneralValueObject;
import ais.database.model.LogUserActifity;
import ais.database.model.MemoryInfo;
import ais.database.model.OnlineUsers;
import ais.database.model.RolePrivilage;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * Helper audit terpusat untuk Hibernate 3.6.
 *
 * Tujuan utama:
 * 1. Membedakan perubahan data bisnis dengan perubahan metadata audit.
 * 2. Menghindari audit palsu saat hanya tanggal_dirubah/olehId/oleh yang berubah.
 * 3. Menjaga keputusan UpdateEventListener agar AuditListener/Envers memakai keputusan yang sama.
 *
 * Tetap ditulis dengan style Java 1.6/1.7 agar kompatibel dengan project lama.
 */
public final class AuditTrailHelper {

    /**
     * Debug audit global. Default true sesuai kebutuhan monitoring awal.
     * Set menjadi false setelah audit stabil agar log production tidak terlalu ramai.
     */
    public static boolean debug = false;

    private static final String DEBUG_PREFIX = "[AIS-AUDIT]";

    public static final String PROP_TANGGAL_DIRUBAH = "tanggal_dirubah";
    public static final String PROP_OLEH_ID = "olehId";
    public static final String PROP_OLEH = "oleh";

    private static final Set<String> IGNORED_UPDATE_PROPERTIES = new HashSet<String>();

    private static final ThreadLocal<Map<String, Boolean>> UPDATE_DECISIONS = new ThreadLocal<Map<String, Boolean>>() {
        @Override
        protected Map<String, Boolean> initialValue() {
            return new HashMap<String, Boolean>();
        }
    };

    static {
        IGNORED_UPDATE_PROPERTIES.add(PROP_TANGGAL_DIRUBAH);
        IGNORED_UPDATE_PROPERTIES.add(PROP_OLEH_ID);
        IGNORED_UPDATE_PROPERTIES.add(PROP_OLEH);
    }

    private AuditTrailHelper() {
    }

    public static void debug(String message) {
        if (!debug) {
            return;
        }
        try {
            System.out.println(DEBUG_PREFIX + " " + new Date() + " - " + message);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/AuditTrailHelper.java:82");
            // Jangan sampai proses audit gagal hanya karena debug log.
        }
    }

    public static void debug(String message, Throwable throwable) {
        if (!debug) {
            return;
        }
        debug(message + (throwable == null ? "" : " | " + throwable.getClass().getName() + " : " + throwable.getMessage()));
    }

    public static String describeEntity(Object entity, Serializable id) {
        String className = getEntityClassName(entity);
        Serializable realId = id == null ? safeIdentifier(entity) : id;
        return (className == null ? "unknown" : className) + "#" + (realId == null ? "new/unknown" : realId);
    }

    public static String describeActivity(Integer activityType) {
        if (activityType == null) {
            return "UNKNOWN";
        }
        if (Integer.valueOf(1).equals(activityType)) {
            return "CREATE";
        }
        if (Integer.valueOf(2).equals(activityType)) {
            return "UPDATE";
        }
        if (Integer.valueOf(3).equals(activityType)) {
            return "DELETE";
        }
        if (Integer.valueOf(0).equals(activityType)) {
            return "READ";
        }
        return String.valueOf(activityType);
    }

    public static String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (maxLength < 1 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    public static boolean isIgnoredUpdateProperty(String propertyName) {
        return propertyName == null || IGNORED_UPDATE_PROPERTIES.contains(propertyName);
    }

    public static boolean isAuditable(Object entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof AccessedUsers || entity instanceof DetailLogLogin || entity instanceof LogUserActifity
                || entity instanceof MemoryInfo || entity instanceof RolePrivilage || entity instanceof OnlineUsers
                || entity instanceof ErrorLog) {
            return false;
        }
        if (entity instanceof org.hibernate.envers.DefaultRevisionEntity) {
            return false;
        }
        String className = getEntityClassName(entity);
        if (className == null) {
            return false;
        }
        if (className.startsWith("org.hibernate.envers.")) {
            return false;
        }
        if (className.equals("ais.database.model.LogUserActifity")
                || className.equals("ais.database.model.DetailLogLogin")
                || className.equals("ais.database.model.AccessedUsers")
                || className.equals("ais.database.model.MemoryInfo")
                || className.equals("ais.database.model.OnlineUsers")
                || className.equals("ais.database.model.RolePrivilage")
                || className.equals("ais.database.model.ErrorLog")) {
            return false;
        }
        return true;
    }

    public static boolean hasBusinessChange(Object[] currentState, Object[] previousState, String[] propertyNames) {
        if (currentState == null || previousState == null || propertyNames == null) {
            return true;
        }
        int max = Math.min(Math.min(currentState.length, previousState.length), propertyNames.length);
        for (int i = 0; i < max; i++) {
            if (isIgnoredUpdateProperty(propertyNames[i])) {
                continue;
            }
            if (!valuesEqual(currentState[i], previousState[i])) {
                return true;
            }
        }
        return false;
    }

    public static String buildBusinessChangeText(Object[] currentState, Object[] previousState, String[] propertyNames) {
        if (currentState == null || previousState == null || propertyNames == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int max = Math.min(Math.min(currentState.length, previousState.length), propertyNames.length);
        for (int i = 0; i < max; i++) {
            if (isIgnoredUpdateProperty(propertyNames[i])) {
                continue;
            }
            if (!valuesEqual(currentState[i], previousState[i])) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(propertyNames[i]).append(": ").append(toDisplayValue(previousState[i])).append(" -> ")
                        .append(toDisplayValue(currentState[i]));
            }
        }
        return builder.toString();
    }

    public static boolean hasBusinessChange(SaveOrUpdateEvent event) {
        if (event == null || event.getObject() == null) {
            return false;
        }
        Object entity = event.getObject();
        if (!isAuditable(entity)) {
            return false;
        }
        try {
            SessionImplementor session = event.getSession();
            EntityPersister persister = resolveEntityPersister(event, entity, session);
            Serializable id = event.getRequestedId();
            if (id == null) {
                id = persister.getIdentifier(entity, session);
            }
            if (id == null) {
                return true;
            }
            Object[] databaseSnapshot = persister.getDatabaseSnapshot(id, session);
            if (databaseSnapshot == null) {
                return true;
            }
            Object[] currentState = persister.getPropertyValues(entity, EntityMode.POJO);
            String[] propertyNames = persister.getPropertyNames();
            return hasBusinessChange(currentState, databaseSnapshot, propertyNames);
        } catch (Exception e) {
            // Fail-open: jika pembandingan gagal, audit tetap dicatat agar tidak kehilangan jejak penting.
            debug("Pembandingan perubahan gagal, audit dibuat fail-open untuk " + describeEntity(entity, null), e);
            // Kegagalan akibat thread latar DI-INTERUPSI (mis. shutdown / c3p0 statement reaping)
            // bersifat transient dan bukan bug aplikasi -> jangan dicatat sebagai error admin agar
            // Error Log tidak terpolusi. Audit tetap fail-open (return true).
            if (!disebabkanInterupsi(e) && !disebabkanKoneksiTertutup(e)) {
                Common.tampilErrorJikaAdmin(e);
            }
            return true;
        }
    }

    /**
     * Memeriksa apakah rantai penyebab {@code t} berasal dari interupsi thread
     * ({@link InterruptedException}) - lazim terjadi pada thread latar yang di-interupsi saat
     * shutdown atau saat c3p0 mereap statement. Kegagalan seperti ini transient dan tidak perlu
     * dilaporkan sebagai error admin. Penelusuran dibatasi agar aman dari rantai sebab melingkar.
     *
     * @param t throwable yang ditelusuri (boleh null)
     * @return {@code true} bila ada {@link InterruptedException} pada rantai penyebab
     */
    private static boolean disebabkanInterupsi(Throwable t) {
        int guard = 0;
        while (t != null && guard < 30) {
            if (t instanceof InterruptedException) {
                return true;
            }
            String msg = t.getMessage();
            if (msg != null && msg.indexOf("InterruptedException") >= 0) {
                return true;
            }
            t = t.getCause();
            guard++;
        }
        return false;
    }

    private static boolean disebabkanKoneksiTertutup(Throwable t) {
        int guard = 0;
        while (t != null && guard < 30) {
            if (t instanceof org.hibernate.exception.JDBCConnectionException) {
                return true;
            }
            if (t instanceof java.sql.SQLException) {
                String state = ((java.sql.SQLException) t).getSQLState();
                if ("08000".equalsIgnoreCase(state) || "08003".equalsIgnoreCase(state)
                        || "08006".equalsIgnoreCase(state)) {
                    return true;
                }
            }
            String msg = t.getMessage();
			if (msg != null) {
				String low = msg.toLowerCase();
				if (low.indexOf("connection has been closed") >= 0
						|| low.indexOf("connection is closed") >= 0
						|| low.indexOf("this connection has been closed") >= 0
						|| low.indexOf("socket closed") >= 0
						|| low.indexOf("statement has been closed") >= 0
						|| low.indexOf("session is closed") >= 0) {
					return true;
				}
			}
            t = t.getCause();
            guard++;
        }
        return false;
    }

    private static EntityPersister resolveEntityPersister(SaveOrUpdateEvent event, Object entity, SessionImplementor session) {
        if (session == null) {
            return null;
        }

        String entityName = null;
        try {
            entityName = event == null ? null : event.getEntityName();
        } catch (Exception e) {
            entityName = null;
        }

        if (entityName == null || entityName.trim().length() == 0 || isProxyEntityName(entityName)) {
            entityName = getEntityClassName(entity);
        }

        if (entityName == null || entityName.trim().length() == 0) {
            return session.getEntityPersister(null, entity);
        }

        try {
            return session.getEntityPersister(entityName, entity);
        } catch (HibernateException e) {
            /*
             * Hibernate 3.6 sering mengirim object proxy Javassist ke listener, misalnya:
             * ais.database.model.Mahasiswa_$$_javassist_674.
             * Jika entityName null, Hibernate mencoba membaca proxy class tersebut sebagai entity
             * dan menimbulkan MappingException: Unknown entity. Fallback ini memaksa memakai
             * nama class asli hasil normalisasi agar save/update model tetap berjalan.
             */
            debug("Gagal mengambil EntityPersister dengan entityName=" + entityName
                    + ", coba fallback class asli untuk " + describeEntity(entity, null), e);
            String normalizedEntityName = getEntityClassName(entity);
            if (normalizedEntityName != null && normalizedEntityName.trim().length() > 0
                    && !normalizedEntityName.equals(entityName)) {
                return session.getEntityPersister(normalizedEntityName, entity);
            }
            throw e;
        }
    }

    private static boolean isProxyEntityName(String entityName) {
        if (entityName == null) {
            return false;
        }
        return entityName.indexOf("_$$_") > 0 || entityName.indexOf("$$") > 0
                || (entityName.indexOf("ais.database.model.") == 0 && entityName.indexOf("_") > 0);
    }

    public static String buildBusinessChangeText(SaveOrUpdateEvent event) {
        if (event == null || event.getObject() == null) {
            return "";
        }
        Object entity = event.getObject();
        if (!isAuditable(entity)) {
            return "";
        }
        try {
            SessionImplementor session = event.getSession();
            EntityPersister persister = resolveEntityPersister(event, entity, session);
            Serializable id = event.getRequestedId();
            if (id == null) {
                id = persister.getIdentifier(entity, session);
            }
            if (id == null) {
                return "Data baru atau identifier belum tersedia.";
            }
            Object[] databaseSnapshot = persister.getDatabaseSnapshot(id, session);
            if (databaseSnapshot == null) {
                return "Snapshot database belum tersedia.";
            }
            Object[] currentState = persister.getPropertyValues(entity, EntityMode.POJO);
            String[] propertyNames = persister.getPropertyNames();
            return buildBusinessChangeText(currentState, databaseSnapshot, propertyNames);
        } catch (Exception e) {
            debug("Gagal membangun detail perubahan bisnis untuk " + describeEntity(entity, null), e);
            return "";
        }
    }

    public static void markUpdateDecision(Object entity, Serializable id, boolean hasBusinessChange) {
        if (entity == null) {
            return;
        }
        UPDATE_DECISIONS.get().put(buildKey(entity, id), Boolean.valueOf(hasBusinessChange));
    }

    public static Boolean peekUpdateDecision(Object entity, Serializable id) {
        if (entity == null) {
            return null;
        }
        return UPDATE_DECISIONS.get().get(buildKey(entity, id));
    }

    public static Boolean consumeUpdateDecision(Object entity, Serializable id) {
        if (entity == null) {
            return null;
        }
        return UPDATE_DECISIONS.get().remove(buildKey(entity, id));
    }

    /**
     * Membersihkan seluruh keputusan update pada thread saat ini.
     *
     * Wajib dipanggil di akhir setiap request (lihat FilterJSP). Keputusan yang
     * di-mark oleh listener tetapi tidak pernah di-consume (mis. update yang
     * dibatalkan karena tidak ada perubahan bisnis) akan menetap di ThreadLocal
     * worker thread Tomcat dan dapat salah men-suppress / meloloskan audit pada
     * request berikutnya yang kebetulan memakai thread yang sama.
     */
    public static void clearUpdateDecisions() {
        try {
            UPDATE_DECISIONS.get().clear();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/AuditTrailHelper.java:377");
            // Jangan ganggu request hanya karena pembersihan audit gagal.
        }
        try {
            UPDATE_DECISIONS.remove();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/AuditTrailHelper.java:382");
            // Abaikan.
        }
    }

    /**
     * Mengecek apakah entity pada event saveOrUpdate adalah data yang SUDAH ADA
     * di database (punya identifier dan snapshot row). Dipakai listener save-update
     * untuk membedakan jalur CREATE (insert baru) dengan jalur UPDATE.
     *
     * Fail-safe: jika pengecekan gagal, dianggap data baru agar proses simpan
     * tidak pernah terblokir oleh logika audit.
     */
    public static boolean isExistingEntity(SaveOrUpdateEvent event) {
        if (event == null || event.getObject() == null) {
            return false;
        }
        Object entity = event.getObject();
        try {
            SessionImplementor session = event.getSession();
            EntityPersister persister = resolveEntityPersister(event, entity, session);
            if (persister == null) {
                return false;
            }
            Serializable id = event.getRequestedId();
            if (id == null) {
                id = persister.getIdentifier(entity, session);
            }
            if (id == null) {
                return false;
            }
            return persister.getDatabaseSnapshot(id, session) != null;
        } catch (Exception e) {
            debug("Gagal mengecek keberadaan entity, dianggap data baru untuk "
                    + describeEntity(entity, null), e);
            return false;
        }
    }

    public static Serializable safeIdentifier(Object entity) {
        if (entity == null) {
            return null;
        }
        try {
            if (entity instanceof HibernateProxy) {
                LazyInitializer initializer = ((HibernateProxy) entity).getHibernateLazyInitializer();
                if (initializer != null && initializer.getIdentifier() instanceof Serializable) {
                    return (Serializable) initializer.getIdentifier();
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/AuditTrailHelper.java:432");
            // lanjut fallback
        }
        try {
            if (entity instanceof Tbmuser) {
                return ((Tbmuser) entity).getUserId();
            }
            if (entity instanceof Tbmrole) {
                return ((Tbmrole) entity).getRoleId();
            }
            if (entity instanceof GeneralValueObject) {
                return ((GeneralValueObject) entity).getId();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/AuditTrailHelper.java:445");
            // lanjut fallback
        }
        return null;
    }

    private static String buildKey(Object entity, Serializable id) {
        Serializable realId = id == null ? safeIdentifier(entity) : id;
        String className = getEntityClassName(entity);
        if (realId == null) {
            return className + "#" + System.identityHashCode(entity);
        }
        return className + "#" + realId;
    }

    /**
     * Mengembalikan nilai metadata audit ke nilai semula jika tidak ada perubahan bisnis.
     *
     * Hibernate 3.6 tetap dapat menjadwalkan UPDATE walaupun Interceptor tidak mengubah
     * state, karena object entity mungkin sudah lebih dulu berubah pada field audit
     * (tanggal_dirubah/olehId/oleh) melalui DataUtil.ubahDataHistory(). Method ini
     * menetralkan kembali state audit agar dirty-check tidak menghasilkan SQL UPDATE
     * yang hanya berisi kolom audit.
     */
    public static void restoreIgnoredUpdateProperties(Object entity, Object[] currentState, Object[] previousState,
            String[] propertyNames) {
        if (currentState == null || previousState == null || propertyNames == null) {
            return;
        }
        int max = Math.min(Math.min(currentState.length, previousState.length), propertyNames.length);
        Object tanggalDirubah = null;
        Object olehId = null;
        Object oleh = null;
        boolean hasTanggalDirubah = false;
        boolean hasOlehId = false;
        boolean hasOleh = false;

        for (int i = 0; i < max; i++) {
            String propertyName = propertyNames[i];
            if (!isIgnoredUpdateProperty(propertyName)) {
                continue;
            }
            currentState[i] = previousState[i];
            if (PROP_TANGGAL_DIRUBAH.equals(propertyName)) {
                tanggalDirubah = previousState[i];
                hasTanggalDirubah = true;
            } else if (PROP_OLEH_ID.equals(propertyName)) {
                olehId = previousState[i];
                hasOlehId = true;
            } else if (PROP_OLEH.equals(propertyName)) {
                oleh = previousState[i];
                hasOleh = true;
            }
        }

        restoreIgnoredUpdatePropertiesOnEntity(entity, tanggalDirubah, hasTanggalDirubah, olehId, hasOlehId, oleh,
                hasOleh);
    }

    public static void restoreIgnoredUpdatePropertiesOnEntity(Object entity, Object tanggalDirubah,
            boolean hasTanggalDirubah, Object olehId, boolean hasOlehId, Object oleh, boolean hasOleh) {
        if (!(entity instanceof GeneralValueObject)) {
            return;
        }
        GeneralValueObject gvo = (GeneralValueObject) entity;
        try {
            if (hasTanggalDirubah && (tanggalDirubah == null || tanggalDirubah instanceof Date)) {
                gvo.setTanggal_dirubah((Date) tanggalDirubah);
            }
        } catch (Exception e) {
            debug("Gagal restore tanggal_dirubah untuk " + describeEntity(entity, safeIdentifier(entity)), e);
        }
        try {
            if (hasOlehId) {
                setStringPropertyEvenWhenEmpty(entity, "olehId", olehId == null ? null : String.valueOf(olehId));
            }
        } catch (Exception e) {
            debug("Gagal restore olehId untuk " + describeEntity(entity, safeIdentifier(entity)), e);
        }
        try {
            if (hasOleh) {
                setStringPropertyEvenWhenEmpty(entity, "oleh", oleh == null ? null : String.valueOf(oleh));
            }
        } catch (Exception e) {
            debug("Gagal restore oleh untuk " + describeEntity(entity, safeIdentifier(entity)), e);
        }
    }

    private static void setStringPropertyEvenWhenEmpty(Object entity, String fieldName, String value) throws Exception {
        Class clazz = entity == null ? null : entity.getClass();
        while (clazz != null) {
            try {
                java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(entity, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
    }

    public static boolean hasOnlyIgnoredUpdateProperties(Object[] currentState, Object[] previousState,
            String[] propertyNames) {
        if (currentState == null || previousState == null || propertyNames == null) {
            return false;
        }
        boolean hasIgnoredChange = false;
        int max = Math.min(Math.min(currentState.length, previousState.length), propertyNames.length);
        for (int i = 0; i < max; i++) {
            if (!valuesEqual(currentState[i], previousState[i])) {
                if (!isIgnoredUpdateProperty(propertyNames[i])) {
                    return false;
                }
                hasIgnoredChange = true;
            }
        }
        return hasIgnoredChange;
    }

    private static boolean valuesEqual(Object left, Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left instanceof byte[] && right instanceof byte[]) {
            return Arrays.equals((byte[]) left, (byte[]) right);
        }
        if (left instanceof char[] && right instanceof char[]) {
            return Arrays.equals((char[]) left, (char[]) right);
        }
        if (left.getClass().isArray() && right.getClass().isArray()) {
            int lengthLeft = Array.getLength(left);
            int lengthRight = Array.getLength(right);
            if (lengthLeft != lengthRight) {
                return false;
            }
            for (int i = 0; i < lengthLeft; i++) {
                if (!valuesEqual(Array.get(left, i), Array.get(right, i))) {
                    return false;
                }
            }
            return true;
        }
        Object normalizedLeft = normalizeValue(left);
        Object normalizedRight = normalizeValue(right);
        if (normalizedLeft == normalizedRight) {
            return true;
        }
        if (normalizedLeft == null || normalizedRight == null) {
            return false;
        }
        return normalizedLeft.equals(normalizedRight);
    }

    private static Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return Long.valueOf(((Date) value).getTime());
        }
        if (value instanceof Calendar) {
            return Long.valueOf(((Calendar) value).getTimeInMillis());
        }
        if (value instanceof HibernateProxy) {
            try {
                LazyInitializer initializer = ((HibernateProxy) value).getHibernateLazyInitializer();
                Serializable id = initializer == null ? null : initializer.getIdentifier();
                Class persistentClass = initializer == null ? null : initializer.getPersistentClass();
                return (persistentClass == null ? value.getClass().getName() : persistentClass.getName()) + "#" + id;
            } catch (Exception e) {
                return value;
            }
        }
        if (value instanceof GeneralValueObject) {
            Serializable id = safeIdentifier(value);
            return getEntityClassName(value) + "#" + id;
        }
        if (value instanceof PersistentCollection) {
            PersistentCollection collection = (PersistentCollection) value;
            if (!collection.wasInitialized()) {
                return "PersistentCollection#uninitialized";
            }
            return value;
        }
        return value;
    }

    private static String toDisplayValue(Object value) {
        Object normalized = normalizeValue(value);
        if (normalized == null) {
            return "";
        }
        String text = String.valueOf(normalized);
        if (text.length() > 300) {
            return text.substring(0, 300) + "...";
        }
        return text;
    }

    private static String getEntityClassName(Object entity) {
        if (entity == null) {
            return "";
        }
        try {
            if (entity instanceof HibernateProxy) {
                LazyInitializer initializer = ((HibernateProxy) entity).getHibernateLazyInitializer();
                if (initializer != null && initializer.getPersistentClass() != null) {
                    return initializer.getPersistentClass().getName();
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/AuditTrailHelper.java:659");
            // fallback di bawah
        }
        String className = entity.getClass().getName();
        int proxyIndex = className.indexOf("_$$_");
        if (proxyIndex > 0) {
            return className.substring(0, proxyIndex);
        }
        proxyIndex = className.indexOf("$$");
        if (proxyIndex > 0) {
            return className.substring(0, proxyIndex);
        }
        proxyIndex = className.indexOf("_");
        if (proxyIndex > 0 && className.indexOf("ais.database.model.") == 0) {
            return className.substring(0, proxyIndex);
        }
        return className;
    }
}
