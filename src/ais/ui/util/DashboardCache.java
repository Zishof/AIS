package ais.ui.util;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Criteria;

/**
 * Cache berjenjang (multi-level) terpusat untuk seluruh dasbor AIS.
 *
 * <p>Tujuan: membuat dasbor terasa sangat cepat dengan menghindari perhitungan
 * agregat berulang ke database. Ada tiga tingkat cache yang saling melengkapi:</p>
 *
 * <ul>
 *   <li><b>L1 — In-process, TTL pendek ({@value #L1_TTL_MS} ms / 90 dtk).</b>
 *       Untuk hasil <i>yang sudah dirakit</i> (mis. objek data dasbor satu layar).
 *       Membuat buka-tutup tab / ganti filter bolak-balik dalam waktu dekat terasa instan.</li>
 *   <li><b>L2 — Hibernate second-level / query cache (EhCache).</b>
 *       Diaktifkan per-query lewat {@link #cacheable(Criteria)} sehingga hasil
 *       SQL agregat dipakai bersama lintas sesi/pengguna selama entitas belum berubah.</li>
 *   <li><b>L3 — In-process, TTL panjang ({@value #L3_TTL_MS} ms / 30 mnt), <i>shared</i>.</b>
 *       Untuk blok agregat berat yang sama bagi semua pengguna (mis. rekap setahun).
 *       Bertahan lebih lama sehingga pengguna pertama "membayar" biaya hitung, sisanya gratis.</li>
 * </ul>
 *
 * <p>Pemakaian khas pada sebuah dasbor:</p>
 * <pre>
 *   // L1: cek apakah seluruh data layar sudah ada (buka ulang cepat)
 *   Object hit = DashboardCache.getIfPresent("payroll.dash." + tahun);
 *
 *   // L3: blok agregat berat, dipakai bersama 30 menit
 *   GajiData g = DashboardCache.l3("payroll.gaji." + tahun, new DashboardCache.Loader&lt;GajiData&gt;() {
 *       public GajiData load() { ... query ... }
 *   });
 *
 *   // L2: tandai criteria agar di-cache Hibernate
 *   List rows = DashboardCache.cacheable(sess.createCriteria(X.class) ... ).list();
 * </pre>
 *
 * <p>Implementasi sengaja sederhana &amp; tanpa dependensi (ConcurrentHashMap) agar aman
 * di Java 8 dan ringan. Bukan pengganti EhCache; L2 tetap mengandalkan EhCache.</p>
 */
public final class DashboardCache {

    /** TTL cache L1 (hasil rakitan layar) — 90 detik. */
    public static final long L1_TTL_MS = 90L * 1000L;

    /** TTL cache L3 (agregat berat bersama) — 30 menit. */
    public static final long L3_TTL_MS = 30L * 60L * 1000L;

    private static final String L1 = "L1:";
    private static final String L3 = "L3:";

    /** Ambang jumlah entri sebelum pembersihan entri kedaluwarsa dijalankan. */
    private static final int PURGE_THRESHOLD = 512;

    /** Pemuat data lazy; hanya dipanggil saat cache miss. */
    public interface Loader<T> {
        T load() throws Exception;
    }

    private static final class Entry {
        final Object value;
        final long expiry;
        Entry(Object value, long expiry) {
            this.value = value;
            this.expiry = expiry;
        }
    }

    private static final ConcurrentHashMap<String, Entry> STORE = new ConcurrentHashMap<String, Entry>();

    private DashboardCache() {
    }

    // ── L1 ───────────────────────────────────────────────────────────────────

    /** Ambil dari L1; jika miss/kedaluwarsa, muat lewat {@code loader} lalu simpan di L1. */
    public static <T> T l1(String key, Loader<T> loader) throws Exception {
        return load(L1, key, L1_TTL_MS, loader);
    }

    /** Simpan nilai rakitan ke L1 secara manual (mis. setelah render bertahap selesai). */
    public static void putL1(String key, Object value) {
        put(L1, key, L1_TTL_MS, value);
    }

    // ── L3 ───────────────────────────────────────────────────────────────────

    /** Ambil dari L3; jika miss/kedaluwarsa, muat lewat {@code loader} lalu simpan di L3. */
    public static <T> T l3(String key, Loader<T> loader) throws Exception {
        return load(L3, key, L3_TTL_MS, loader);
    }

    /** Simpan nilai ke L3 secara manual. */
    public static void putL3(String key, Object value) {
        put(L3, key, L3_TTL_MS, value);
    }

    // ── L2 (Hibernate) ─────────────────────────────────────────────────────────

    /**
     * Menandai sebuah {@link Criteria} agar hasilnya dilayani dari Hibernate
     * second-level query cache (L2/EhCache). Aman dipanggil walau query cache
     * belum aktif — efeknya hanya diabaikan.
     *
     * @return criteria yang sama (untuk chaining).
     */
    public static Criteria cacheable(Criteria criteria) {
        if (criteria != null) {
            try {
                criteria.setCacheable(true);
            } catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/ui/util/DashboardCache.java:114");
                // query cache tidak aktif / versi berbeda — abaikan, query tetap jalan.
            }
        }
        return criteria;
    }

    // ── Lookup tanpa load-through ───────────────────────────────────────────────

    /**
     * Mengembalikan nilai ter-cache untuk {@code key} bila masih segar, dengan
     * urutan prioritas L1 lalu L3. Mengembalikan {@code null} bila tidak ada.
     * Berguna untuk jalur "buka ulang cepat" tanpa memicu perhitungan.
     */
    public static Object getIfPresent(String key) {
        Object v = peek(L1 + key);
        if (v != null) {
            return v;
        }
        return peek(L3 + key);
    }

    // ── Invalidasi ─────────────────────────────────────────────────────────────

    /** Hapus satu key dari semua tingkat. */
    public static void invalidate(String key) {
        STORE.remove(L1 + key);
        STORE.remove(L3 + key);
    }

    /**
     * Hapus semua key (di semua tingkat) yang mengandung {@code keyContains}.
     * Panggil ini dari modul saat data sumber berubah, mis. setelah posting gaji:
     * {@code DashboardCache.invalidateContaining("payroll.")}.
     */
    public static void invalidateContaining(String keyContains) {
        if (keyContains == null) {
            return;
        }
        Iterator<Map.Entry<String, Entry>> it = STORE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Entry> e = it.next();
            String k = e.getKey();
            int idx = k.indexOf(':');
            String bareKey = idx >= 0 ? k.substring(idx + 1) : k;
            if (bareKey.contains(keyContains)) {
                it.remove();
            }
        }
    }

    /** Kosongkan seluruh cache (mis. saat ganti tahun akademik aktif / maintenance). */
    public static void clearAll() {
        STORE.clear();
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static <T> T load(String level, String key, long ttl, Loader<T> loader) throws Exception {
        String full = level + key;
        Object cached = peek(full);
        if (cached != null) {
            return (T) cached;
        }
        T value = loader.load();
        put(level, key, ttl, value);
        return value;
    }

    private static Object peek(String fullKey) {
        Entry e = STORE.get(fullKey);
        if (e == null) {
            return null;
        }
        if (e.expiry <= System.currentTimeMillis()) {
            STORE.remove(fullKey);
            return null;
        }
        return e.value;
    }

    private static void put(String level, String key, long ttl, Object value) {
        if (value == null) {
            return;
        }
        if (STORE.size() > PURGE_THRESHOLD) {
            purgeExpired();
        }
        STORE.put(level + key, new Entry(value, System.currentTimeMillis() + ttl));
    }

    private static void purgeExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> it = STORE.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expiry <= now) {
                it.remove();
            }
        }
    }
}
