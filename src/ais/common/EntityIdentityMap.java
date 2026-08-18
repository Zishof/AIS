package ais.common;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;

import ais.database.model.GeneralValueObject;

/**
 * JVM-wide identity map: satu Java object per entity class+ID.
 *
 * <p><b>Problem yang diselesaikan:</b><br>
 * MapDB menyerialkan/deserialkan Tagihan beserta NominalBiaya-nya.  Saat
 * deserialisasi terbentuk objek Java BARU — berbeda dengan instance yang
 * dikelola Hibernate pada sesi aktif.  Akibatnya ketika {@code nb.setBukanTagihan(true)}
 * dipanggil, Tagihan di cache masih melihat salinan lama ({@code bukanTagihan=null}).
 *
 * <p><b>Solusi:</b><br>
 * Setiap kali entity disimpan atau dimuat, code memanggil
 * {@link #canonical(GeneralValueObject)}.  Method ini menjamin:<br>
 * • Jika belum ada canonical untuk ID tersebut → objek yang diberikan
 *   menjadi canonical dan dikembalikan.<br>
 * • Jika sudah ada canonical (objek lain, ID sama) → scalar field terbaru
 *   di-<em>merge</em> ke dalam canonical, lalu canonical itu dikembalikan.<br>
 * Semua pemegang referensi ke canonical lama otomatis melihat nilai terbaru
 * tanpa perlu iterasi atau invalidasi manual.
 *
 * <p><b>Memory safety:</b><br>
 * Canonical disimpan sebagai {@link WeakReference} sehingga GC bebas
 * mengumpulkan entitas yang sudah tidak lagi direferensikan secara kuat.
 * Saat canonical di-GC, pendaftaran berikutnya langsung menjadi canonical baru.
 */
public class EntityIdentityMap {

    // key = "fully.qualified.ClassName#id"  →  WeakRef ke canonical instance
    private static final ConcurrentHashMap<String, WeakReference<Object>> REGISTRY =
            new ConcurrentHashMap<String, WeakReference<Object>>();

    // ReferenceQueue untuk me-reap entry yang referent-nya sudah di-GC (optimasi RAM Fase 1).
    // Tanpa ini, value memang weak tapi KEY String + wrapper WeakReference tetap strong dan
    // tidak pernah dihapus → registry tumbuh permanen ±100 byte per entity yang pernah
    // dikanonikalisasi sekali lalu tidak diakses lagi.
    private static final java.lang.ref.ReferenceQueue<Object> QUEUE =
            new java.lang.ref.ReferenceQueue<Object>();

    /** WeakReference yang mengingat key registry-nya sendiri agar bisa di-reap dari QUEUE. */
    private static final class KeyedWeakReference extends WeakReference<Object> {
        private final String kunci;

        KeyedWeakReference(String kunci, Object referent, java.lang.ref.ReferenceQueue<Object> queue) {
            super(referent, queue);
            this.kunci = kunci;
        }
    }

    /**
     * Buang entry yang referent-nya sudah di-GC. Murah (poll non-blocking), dipanggil di
     * awal setiap operasi publik. remove(key, value) dua-argumen menjamin canonical BARU
     * dengan key sama tidak ikut terhapus.
     */
    private static void reapStaleEntries() {
        java.lang.ref.Reference<?> ref;
        while ((ref = QUEUE.poll()) != null) {
            if (ref instanceof KeyedWeakReference) {
                REGISTRY.remove(((KeyedWeakReference) ref).kunci, ref);
            }
        }
    }

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Kembalikan canonical instance untuk entity ini.
     *
     * <ul>
     *   <li>Belum ada canonical → entity ini langsung jadi canonical.</li>
     *   <li>Sudah ada canonical, beda objek → scalar fields di-merge dari
     *       {@code fresh} ke canonical; kembalikan canonical (bukan fresh).</li>
     *   <li>Sudah ada canonical, SAMA objek → kembalikan saja.</li>
     * </ul>
     *
     * @param fresh entity dari Hibernate atau deserialisasi MapDB
     * @return canonical instance (mungkin beda referensi dari {@code fresh})
     */
    @SuppressWarnings("unchecked")
    public static <T extends GeneralValueObject> T canonical(T fresh) {
        if (fresh == null) return null;

        // PENTING — JANGAN pernah menjadikan HibernateProxy sebagai canonical.
        // Interceptor AuditTimestampInterceptor.instantiate() mengembalikan canonical sebagai
        // IMPLEMENTASI dari proxy yang sedang di-inisialisasi. Bila canonical itu adalah proxy yang
        // SAMA (karena key kini pakai nama kelas PERSISTEN), proxy menjadi implementasi dirinya
        // sendiri → getId()/method apa pun memanggil dirinya tanpa henti = StackOverflowError.
        // Solusi: untuk proxy yang SUDAH ter-inisialisasi, kanonikalisasi IMPLEMENTASI aslinya;
        // untuk proxy yang BELUM ter-inisialisasi, JANGAN daftarkan (biarkan onLoad mendaftarkan
        // instance ASLI saat entity benar-benar dimuat) — juga menghindari inisialisasi paksa.
        if (fresh instanceof org.hibernate.proxy.HibernateProxy) {
            org.hibernate.proxy.LazyInitializer li =
                    ((org.hibernate.proxy.HibernateProxy) fresh).getHibernateLazyInitializer();
            if (li != null) {
                if (li.isUninitialized()) {
                    return fresh;
                }
                Object impl = li.getImplementation();
                if (impl instanceof GeneralValueObject && impl != fresh) {
                    return canonical((T) impl);
                }
                return fresh;
            }
        }

        Long id = fresh.getId();
        if (id == null) return fresh;

        reapStaleEntries();

        String key = keyForObject(fresh, id);

        WeakReference<Object> existingRef = REGISTRY.get(key);
        T existing = existingRef == null ? null : (T) existingRef.get();

        if (existing == null || existing == fresh) {
            // Tidak ada canonical atau sudah GC — fresh menjadi canonical baru
            REGISTRY.put(key, new KeyedWeakReference(key, fresh, QUEUE));
            return fresh;
        }

        // Canonical berbeda → merge scalar fields dari fresh ke canonical
        mergeScalars(existing, fresh);
        return existing;
    }

    /**
     * Ambil canonical instance yang sudah terdaftar.
     * Kembalikan {@code null} jika belum terdaftar atau sudah di-GC.
     */
    @SuppressWarnings("unchecked")
    public static <T extends GeneralValueObject> T get(Class<T> clazz, Long id) {
        if (id == null) return null;
        reapStaleEntries();
        WeakReference<Object> ref = REGISTRY.get(key(clazz, id));
        return ref == null ? null : (T) ref.get();
    }

    /**
     * Hapus entry dari registry (setelah entity dihapus dari DB).
     */
    public static void evict(Class<? extends GeneralValueObject> clazz, Long id) {
        reapStaleEntries();
        if (id != null) REGISTRY.remove(key(clazz, id));
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private static String key(Class<?> clazz, Long id) {
        return normalize(clazz.getName()) + '#' + id;
    }

    /**
     * Key dari sebuah OBJEK — memakai NAMA KELAS PERSISTEN (bukan kelas proxy Hibernate) agar
     * instance proxy dan non-proxy untuk (entity,id) yang sama memetakan ke SATU canonical.
     */
    private static String keyForObject(Object obj, Long id) {
        return classNameOf(obj) + '#' + id;
    }

    /** Nama kelas persisten dari objek: tangani HibernateProxy, lalu pangkas penanda proxy. */
    private static String classNameOf(Object data) {
        try {
            if (data instanceof org.hibernate.proxy.HibernateProxy) {
                org.hibernate.proxy.LazyInitializer li =
                        ((org.hibernate.proxy.HibernateProxy) data).getHibernateLazyInitializer();
                if (li != null && li.getPersistentClass() != null) {
                    return li.getPersistentClass().getName();
                }
            }
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/EntityIdentityMap.java:143");
            // abaikan → fallback ke normalisasi nama kelas biasa.
        }
        return normalize(data.getClass().getName());
    }

    /** Pangkas penanda kelas proxy (javassist {@code _$$_}, CGLIB {@code $$}) → nama kelas asli. */
    private static String normalize(String className) {
        if (className == null) {
            return "";
        }
        int i = className.indexOf("_$$_");
        if (i > 0) {
            return className.substring(0, i);
        }
        i = className.indexOf("$$");
        if (i > 0) {
            return className.substring(0, i);
        }
        return className;
    }

    /**
     * Copy semua scalar field non-null dari {@code source} ke {@code target}.
     *
     * "Scalar" = primitive, Boolean, Number, String, Date, BigDecimal, Enum.
     * Entity references dan collections TIDAK di-copy (biarkan Hibernate lazy-load).
     * Nilai {@code null} dari source TIDAK menimpa nilai existing di target
     * (hindari menghapus data valid dengan fragment entity yang hanya berisi sebagian field).
     */
    private static void mergeScalars(Object target, Object source) {
        Class<?> c = source.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                int mod = f.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) continue;
                if (!isScalar(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object val = f.get(source);
                    if (val != null) f.set(target, val);
                } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/EntityIdentityMap.java:184");
                    // refleksi gagal (mis. SecurityManager) — lewati satu field
                }
            }
            c = c.getSuperclass();
        }
    }

    private static boolean isScalar(Class<?> t) {
        return t.isPrimitive()
                || t == String.class
                || t == Boolean.class   || t == Character.class
                || t == Byte.class      || t == Short.class
                || t == Integer.class   || t == Long.class
                || t == Float.class     || t == Double.class
                || java.util.Date.class.isAssignableFrom(t)
                || t == java.math.BigDecimal.class
                || t.isEnum();
    }
}
