package ais.common;

import java.util.concurrent.ConcurrentHashMap;

import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;

/**
 * Cache multi-level untuk semua panel dasbord.
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  L1  — local variable DashData dalam renderAll()                    │
 * │        Sudah ada di semua kelas. Data dimuat 1× lalu dikirim ke     │
 * │        semua sub-panel render. Tidak butuh kelas ini.               │
 * │                                                                     │
 * │  L2  — ZK Desktop attribute (per browser-tab / per user session)   │
 * │        Dasbor yang sama tidak perlu query ulang selama TTL aktif.  │
 * │        Ideal saat user bolak-balik antar tab.                       │
 * │                                                                     │
 * │  L3  — static ConcurrentHashMap (per JVM, lintas semua sesi)       │
 * │        Dipakai hanya untuk data admin / non-personal. Saat 10 admin │
 * │        membuka dasbor yang sama serentak, hanya 1 query ke DB.     │
 * │        TTL lebih pendek agar data tidak terlalu basi.               │
 * └─────────────────────────────────────────────────────────────────────┘
 *
 * Cara pakai di setiap kelas Dasbor:
 * <pre>
 *   private MyDashData loadDataWithCache() {
 *       boolean isPersonal = ...;   // siswa/mhs/guru melihat data dirinya
 *       String  key = DashboardCacheUtil.key("NamaKelas", lingkup.name(),
 *                                             isPersonal ? userId : null);
 *
 *       // L2 — session cache
 *       Object fromL2 = DashboardCacheUtil.getL2(key);
 *       if (fromL2 instanceof MyDashData) return (MyDashData) fromL2;
 *
 *       // L3 — app-wide (hanya kalau bukan personal)
 *       if (!isPersonal) {
 *           Object fromL3 = DashboardCacheUtil.getL3(key);
 *           if (fromL3 instanceof MyDashData) {
 *               DashboardCacheUtil.putL2(key, fromL3);   // pemanasan L2
 *               return (MyDashData) fromL3;
 *           }
 *       }
 *
 *       // Cache miss — ambil dari DB
 *       MyDashData d = loadData();
 *       DashboardCacheUtil.putL2(key, d);
 *       if (!isPersonal) DashboardCacheUtil.putL3(key, d);
 *       return d;
 *   }
 * </pre>
 */
public final class DashboardCacheUtil {

    // ── Default TTL ────────────────────────────────────────────────────────
    /** TTL L2 (per-session): 5 menit cukup lama untuk bolak-balik tab. */
    public static final int TTL_L2_MS = 5 * 60 * 1000;

    /**
     * TTL L3 (app-wide): 3 menit — lebih pendek agar admin melihat data
     * yang masih segar. Turunkan ke 60_000 jika data sangat dinamis.
     */
    public static final int TTL_L3_MS = 3 * 60 * 1000;

    // Prefix agar key tidak bentrok dengan atribut Desktop lain
    private static final String PFX_L2 = "_dc2_";

    // L3 store — thread-safe, otomatis evict entry expired saat akses
    private static final ConcurrentHashMap<String, CachedEntry> L3 =
            new ConcurrentHashMap<String, CachedEntry>();

    private DashboardCacheUtil() {}

    // ════════════════════════════════════════════════════════════════════
    // L2 — ZK Desktop / session
    // ════════════════════════════════════════════════════════════════════

    /**
     * Simpan {@code data} ke cache L2 dengan TTL default
     * ({@value #TTL_L2_MS} ms).
     */
    public static void putL2(String key, Object data) {
        putL2(key, data, TTL_L2_MS);
    }

    /** Simpan {@code data} ke cache L2 dengan TTL kustom (dalam ms). */
    public static void putL2(String key, Object data, int ttlMs) {
        try {
            Desktop d = currentDesktop();
            if (d != null) d.setAttribute(PFX_L2 + key, new CachedEntry(data, ttlMs));
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DashboardCacheUtil.java:92");}
    }

    /**
     * Ambil data dari L2.
     * @return data tersimpan, atau {@code null} jika tidak ada / sudah expired.
     */
    public static Object getL2(String key) {
        try {
            Desktop d = currentDesktop();
            if (d == null) return null;
            Object raw = d.getAttribute(PFX_L2 + key);
            if (raw instanceof CachedEntry) {
                CachedEntry e = (CachedEntry) raw;
                if (!e.isExpired()) return e.data;
                d.removeAttribute(PFX_L2 + key);
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DashboardCacheUtil.java:109");}
        return null;
    }

    /**
     * Hapus entry L2 secara eksplisit — panggil saat data diperbarui
     * agar halaman berikutnya mengambil data segar.
     */
    public static void invalidateL2(String key) {
        try {
            Desktop d = currentDesktop();
            if (d != null) d.removeAttribute(PFX_L2 + key);
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DashboardCacheUtil.java:121");}
    }

    // ════════════════════════════════════════════════════════════════════
    // L3 — Application / JVM-wide
    // ════════════════════════════════════════════════════════════════════

    /**
     * Simpan {@code data} ke cache L3 dengan TTL default
     * ({@value #TTL_L3_MS} ms).
     */
    public static void putL3(String key, Object data) {
        putL3(key, data, TTL_L3_MS);
    }

    /** Simpan {@code data} ke cache L3 dengan TTL kustom (dalam ms). */
    public static void putL3(String key, Object data, int ttlMs) {
        L3.put(key, new CachedEntry(data, ttlMs));
    }

    /**
     * Ambil data dari L3.
     * @return data tersimpan, atau {@code null} jika tidak ada / sudah expired.
     */
    public static Object getL3(String key) {
        CachedEntry e = L3.get(key);
        if (e == null) return null;
        if (e.isExpired()) { L3.remove(key); return null; }
        return e.data;
    }

    /**
     * Hapus entry L3 — panggil saat data diperbarui secara massal
     * (misalnya setelah import atau sync).
     */
    public static void invalidateL3(String key) {
        L3.remove(key);
    }

    /**
     * Hapus semua entry L3 yang sudah expired.
     * Panggil dari scheduled task harian / saat Tomcat startup.
     */
    public static void evictExpiredL3() {
        for (String k : L3.keySet()) {
            CachedEntry e = L3.get(k);
            if (e != null && e.isExpired()) L3.remove(k);
        }
    }

    /** Jumlah entry aktif (belum expired) di L3. Berguna untuk monitoring. */
    public static int sizeL3() {
        int n = 0;
        for (CachedEntry e : L3.values()) { if (!e.isExpired()) n++; }
        return n;
    }

    // ════════════════════════════════════════════════════════════════════
    // Helper: bangun cache key standar
    // ════════════════════════════════════════════════════════════════════

    /**
     * Buat cache key unik berdasarkan nama class, scope/lingkup, dan
     * identitas entitas pengguna.
     *
     * <ul>
     *   <li>{@code userEntityId != null} → data personal; gunakan hanya L2.</li>
     *   <li>{@code userEntityId == null} → data admin/bersama; boleh L2 + L3.</li>
     * </ul>
     *
     * @param className     nama pendek kelas (e.g. "DasbordCatatan")
     * @param lingkup       nama lingkup/scope (e.g. "SEMUA", "SISWA")
     * @param userEntityId  ID entitas personal (siswa.id / mhs.id / dll.),
     *                      atau {@code null} untuk data admin/bersama.
     */
    public static String key(String className, String lingkup, Long userEntityId) {
        if (userEntityId != null) {
            return className + "_" + lingkup + "_u" + userEntityId;
        }
        return className + "_" + lingkup + "_shared";
    }

    /**
     * Versi key dengan parameter filter tambahan (untuk dashboard yang punya
     * filter yayasan/sekolah/tanggal seperti DashboardAktifitasHarianSiswa).
     */
    public static String keyWithFilter(String className, String lingkup,
                                       Long userEntityId, String filterFingerprint) {
        String base = key(className, lingkup, userEntityId);
        if (filterFingerprint != null && !filterFingerprint.isEmpty()) {
            return base + "_" + filterFingerprint;
        }
        return base;
    }

    // ════════════════════════════════════════════════════════════════════
    // Inner: entry cache dengan TTL
    // ════════════════════════════════════════════════════════════════════

    public static final class CachedEntry {
        public final Object data;
        private final long  expiresAt;

        public CachedEntry(Object data, int ttlMs) {
            this.data      = data;
            this.expiresAt = System.currentTimeMillis() + ttlMs;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

        /** Sisa waktu hidup dalam milidetik (negatif jika sudah expired). */
        public long remainingMs() {
            return expiresAt - System.currentTimeMillis();
        }
    }

    // ── Private helper ──────────────────────────────────────────────────
    private static Desktop currentDesktop() {
        try {
            return Executions.getCurrent().getDesktop();
        } catch (Exception e) {
            return null;
        }
    }
}
