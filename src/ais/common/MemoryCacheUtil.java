package ais.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lapisan cache objek IN-JVM (murni memori) milik AIS. Menjadi "gudang RAM" tempat entity Hibernate
 * yang sering dibaca disimpan dalam bentuk {@link java.util.Map} bernama, sehingga halaman tidak perlu
 * memukul basis data berulang kali. Pemanggil utama adalah {@link DataUtil} (lihat
 * {@code ambilData}/{@code masukkanData}) dan {@code MemoryDbUtil}.
 *
 * <h3>Konsep</h3>
 * Registry Map bernama yang di-share satu JVM: tiap "kelas data" (mis. {@code <namaKelas>_data_<CODE>})
 * memiliki sebuah {@link Map} key(=id)→objek anak {@code GeneralValueObject}. Dengan begitu entity yang
 * sudah pernah dibaca dapat dipakai ulang tanpa query DB — cukup 1 ID + kelas objek yang tersimpan di
 * dalam JVM.
 *
 * <h3>Riwayat &amp; keputusan desain (PENTING)</h3>
 * Versi sebelumnya membangun cache di atas <b>MapDB</b> (store key-value berbasis file/mmap) dan
 * secara opsional <b>Hazelcast</b> (pub/sub antar-node untuk deploy multi-instance). Keduanya telah
 * DIHAPUS atas permintaan: MapDB sering memicu kontensi monitor {@code org.mapdb.DB} pada jalur panas
 * dan kegagalan I/O ({@code ClosedChannelException}→{@code IOError}) yang meruntuhkan request kritis;
 * Hazelcast hanya diperlukan bila deploy &gt;1 node. Kini cache MURNI memori 1 JVM memakai
 * {@link ConcurrentHashMap} (thread-safe, tanpa lock global, tanpa I/O berkas). Kelas ini tidak lagi
 * bergantung pada pustaka {@code org.mapdb.*} maupun {@code com.hazelcast.*}.
 *
 * <h3>Cara kerja</h3>
 * Tiap {@code Map} diperoleh via {@link #get(String)} yang membuat {@link ConcurrentHashMap} baru bila
 * nama belum terdaftar, atau mengembalikan yang sudah ada (idempoten). Nilai disimpan sebagai
 * REFERENSI objek langsung di heap (bukan salinan hasil serialisasi seperti MapDB dahulu), sehingga
 * akses lebih murah. Berkas konfigurasi ringan ({@link #CONFIG_BASE_PATH}) hanya dibaca untuk mengisi
 * {@link #lokasi_file_temporary_data} yang dipakai modul lain — opsi lama MapDB/Hazelcast diabaikan.
 *
 * <h3>Konsistensi data</h3>
 * Cache HANYA mirror data DB (sumber kebenaran tetap RDBMS). Setiap jalur TULIS (simpan/ubah/hapus
 * entity) WAJIB memperbarui cache lewat {@code DataUtil.masukkanData}/{@code hapus} agar tidak basi —
 * kelalaian inilah akar bug "skor 0 / data lama" yang pernah terjadi pada modul ujian. Karena nilai
 * kini berupa referensi (bukan salinan), perlakukan hasil {@code get} sebagai read-mostly.
 *
 * <h3>Keamanan thread &amp; pemeliharaan</h3>
 * {@link ConcurrentHashMap} aman untuk akses konkuren tanpa lock global. Kompatibel Java 1.6/1.7
 * (tanpa lambda/Stream). Cache memakai referensi KUAT (tidak auto-evict); karena hanya mirror RDBMS,
 * bila jejak memori jadi masalah pada dataset sangat besar, pertimbangkan menambah kebijakan eviction
 * di kemudian hari (di luar cakupan penghapusan MapDB/Hazelcast ini).
 */
public class MemoryCacheUtil {

    private static final String CONFIG_BASE_PATH = "/opt/.g/.h/";

    /** Lokasi berkas temporary (dibaca dari konfigurasi; dipakai KonfigurasiAction/InitDataHelper). */
    public static String lokasi_file_temporary_data = null;

    /** Kode unik per-JVM untuk penamaan map ({@code <kelas>_data_<CODE>}); stabil sepanjang umur JVM. */
    private static String CODE = null;

    /**
     * Registry cache IN-JVM: nama map -> Map(key->objek). Semua entri adalah {@link ConcurrentHashMap}
     * (thread-safe, tanpa lock tunggal). Menggantikan store MapDB (file/mmap) + replikasi Hazelcast
     * yang dulu dipakai — kini murni memori 1 JVM.
     */
    @SuppressWarnings("rawtypes")
    private static final ConcurrentHashMap<String, Map> MAPS = new ConcurrentHashMap<String, Map>();

    /**
     * Ringkasan isi cache untuk snapshot performa (OPTIMASI FASE 10): jumlah region dan
     * total baris yang ditahan di heap. Dipakai memantau apakah preload/cache benar-benar
     * terkendali setelah Fase 2 dan Fase 3. Menelan seluruh kegagalan.
     */
    public static String statistik() {
        try {
            int region = MAPS.size();
            long totalBaris = 0L;
            for (Map m : MAPS.values()) {
                if (m != null) {
                    totalBaris += m.size();
                }
            }
            return "region=" + region + ", total baris=" + totalBaris;
        } catch (Throwable t) {
            return "gagal dibaca: " + t.getClass().getSimpleName();
        }
    }

    /** Penanda bahwa konfigurasi ringan (lokasi_file_temporary_data) sudah dibaca sekali. */
    private static volatile boolean configLoaded = false;

    /** Constructor privat: class murni utilitas statis, tak boleh diinstansiasi. */
    private MemoryCacheUtil() {
    }

    /**
     * Mengembalikan kode unik per-instance JVM yang dipakai sebagai sufiks nama map
     * ({@code <kelas>_data_<CODE>}). Stabil sepanjang umur proses agar penulis &amp; pembaca memakai
     * map yang sama. Di-generate sekali (0..999) memakai {@link ThreadLocalRandom}.
     *
     * @return kode unik per-JVM untuk penamaan map cache
     */
    public static String getCODE() {
        if (CODE == null) {
            CODE = String.valueOf(ThreadLocalRandom.current().nextLong(0, 1000));
        }
        return CODE;
    }

    /**
     * Membaca konfigurasi ringan SEKALI (thread-safe, malas). Satu-satunya nilai yang masih relevan
     * adalah {@link #lokasi_file_temporary_data}; opsi lama MapDB/Hazelcast (memoryDb, path_map,
     * size_map, cache_map, use_distributed_cache, dll.) sudah tidak dipakai.
     */
    private static void ensureConfigLoaded() {
        if (configLoaded) {
            return;
        }
        synchronized (MemoryCacheUtil.class) {
            if (configLoaded) {
                return;
            }
            try {
                getCODE();
                Properties props = loadConfiguration(determineContextName());
                lokasi_file_temporary_data = props.getProperty("lokasi_file_temporary_data", "").trim();
            } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/MemoryCacheUtil.java:108");
                // Abaikan: biarkan lokasi_file_temporary_data pada nilai default (null/kosong).
            }
            configLoaded = true;
        }
    }

    /**
     * Mengambil (atau membuat bila belum ada) sebuah {@link Map} cache bernama {@code classData}.
     *
     * <p>Inilah pintu utama akses cache: pemanggil ({@link DataUtil}, MemoryDbUtil, dsb.) menyebut
     * nama logis map — biasanya {@code <namaKelas>_data_<CODE>} — lalu memperoleh peta key→objek yang
     * dipakai bersama seluruh JVM. Map yang dikembalikan adalah {@link ConcurrentHashMap} in-memory;
     * perubahan (put/remove) langsung terlihat oleh semua pembaca di JVM yang sama.</p>
     *
     * <p>Saat aplikasi sedang berhenti, TIDAK membuat map baru (mengembalikan yang sudah ada atau map
     * kosong) agar tidak menahan referensi baru saat shutdown.</p>
     *
     * <p>Catatan: {@link ConcurrentHashMap} tidak mengizinkan key/value {@code null} — sama seperti
     * HTreeMap MapDB sebelumnya, sehingga perilaku pemanggil lama tetap kompatibel.</p>
     *
     * @param classData nama logis map cache
     * @return map cache (dibuat bila belum ada)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Map get(String classData) {
        ensureConfigLoaded();

        if (Common.aplikasiSedangBerhenti) {
            Map existing = MAPS.get(classData);
            return existing != null ? existing : new java.util.HashMap();
        }

        Map cached = MAPS.get(classData);
        if (cached != null) {
            return cached;
        }
        Map created = new ConcurrentHashMap();
        Map prev = MAPS.putIfAbsent(classData, created);
        return prev != null ? prev : created;
    }

    /**
     * Kompatibilitas API pasca-penghapusan MapDB. Dahulu membangun ULANG store MapDB setelah kegagalan
     * I/O fatal ({@code ClosedChannelException}→{@code IOError}) pada FileChannel/mmap-nya.
     *
     * <p>Kini cache MURNI IN-JVM ({@link ConcurrentHashMap}) → tidak ada berkas/mmap/FileChannel yang
     * bisa {@code ClosedChannelException}/{@code IOError}, jadi tidak ada store yang perlu dibangun
     * ulang. Method dipertahankan agar pemanggil lama (BacaTulisUtil, GeneralValueObject, dll.) tetap
     * kompilasi; ia hanya membersihkan referensi lokal MemoryDbUtil sebagai tindakan aman.</p>
     *
     * @param namaMapOpsional nama map (untuk log; boleh null)
     */
    public static void rebuildAfterFailure(String namaMapOpsional) {
        if (Common.aplikasiSedangBerhenti) {
            return;
        }
        try {
            MemoryDbUtil.resetLocalReferences();
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/MemoryCacheUtil.java:167");
            // diabaikan.
        }
    }

    /**
     * Memeriksa apakah sebuah map cache bernama {@code clazzs} sudah pernah dibuat — TANPA membuatnya
     * (berbeda dari {@link #get(String)} yang membuat bila belum ada).
     *
     * @param clazzs nama map cache yang diperiksa
     * @return {@code true} bila sudah ada di registry in-JVM
     */
    public static boolean exists(String clazzs) {
        return MAPS.containsKey(clazzs);
    }

    /**
     * No-op pasca-penghapusan Hazelcast. Dahulu mem-broadcast perubahan map mentah (PUT/REMOVE) ke
     * node lain dalam kluster Hazelcast untuk menjaga koherensi cache lintas-node.
     *
     * <p>Deploy kini 1 JVM (cache lokal) sehingga tidak ada yang perlu di-broadcast. Method
     * dipertahankan sebagai no-op agar jalur tulis cache pemanggil tidak perlu diubah.</p>
     *
     * @param mapName nama map yang berubah
     * @param action  "PUT" atau "REMOVE"
     * @param key     kunci entri
     * @param data    nilai untuk PUT (boleh null saat REMOVE)
     */
    public static void broadcastUpdate(String mapName, String action, String key, String data) {
        // no-op (single JVM; Hazelcast dihapus).
    }

    /**
     * No-op pasca-penghapusan Hazelcast. Dahulu mem-broadcast permintaan sinkronisasi ENTITY ke node
     * lain. Deploy kini 1 JVM sehingga cache lokal sudah koheren; dipertahankan sebagai no-op.
     *
     * @param action       jenis aksi sinkronisasi
     * @param clazzsPrefix nama kelas/prefix entity
     * @param key          id entity
     */
    public static void broadcastObjectSync(String action, String clazzsPrefix, String key) {
        // no-op (single JVM; Hazelcast dihapus).
    }

    /**
     * Menentukan nama context (nama webapp) dari lokasi {@code WEB-INF/classes} di classpath, untuk
     * memilih berkas konfigurasi khusus context pada {@link #loadConfiguration(String)}. Seluruhnya
     * dibungkus try/catch sehingga kegagalan apa pun menghasilkan string kosong (fallback default).
     *
     * @return nama context webapp, atau string kosong bila gagal dideteksi
     */
    private static String determineContextName() {
        String context = "";
        try {
            String path = MemoryCacheUtil.class.getClassLoader().getResource("").getPath();
            String fullPath = URLDecoder.decode(path, "UTF-8");
            String[] pathArr = fullPath.split("/WEB-INF/classes/");
            if (pathArr.length > 0) {
                fullPath = pathArr[0];
                String[] ss = fullPath.split("/");
                if (ss.length > 0) {
                    context = ss[ss.length - 1];
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/MemoryCacheUtil.java:231");
        }
        return context;
    }

    /**
     * Memuat berkas konfigurasi menjadi {@link Properties} (memilih {@code <context>.txt} bila ada,
     * jika tidak fallback {@code xxyxyx.txt}). Kini hanya {@link #lokasi_file_temporary_data} yang
     * dibaca; berkas boleh tidak ada (dikembalikan {@link Properties} kosong, tidak pernah null).
     * Stream selalu ditutup di {@code finally}.
     *
     * @param contextName nama context untuk memilih berkas konfigurasi khusus
     * @return properti konfigurasi (kosong bila berkas tidak ada/gagal dibaca)
     */
    private static Properties loadConfiguration(String contextName) {
        Properties props = new Properties();
        File fileConf = new File(CONFIG_BASE_PATH + "xxyxyx.txt");
        File fileConfKhusus = new File(CONFIG_BASE_PATH + contextName + ".txt");

        if (fileConfKhusus.exists()) {
            fileConf = fileConfKhusus;
        }

        if (fileConf.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(fileConf);
                props.load(fis);
            } catch (IOException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/MemoryCacheUtil.java:259");
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/MemoryCacheUtil.java:264");
                    }
                }
            }
        }
        return props;
    }
}
