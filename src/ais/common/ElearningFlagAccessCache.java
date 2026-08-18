package ais.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <h1>ElearningFlagAccessCache — cache PERSISTEN (ke HDD) untuk ringkasan e-Learning</h1>
 *
 * <h2>Latar belakang &amp; tujuan</h2>
 * <p>Halaman e-Learning ("Aktivitas Pembelajaran Digital") menampilkan angka ringkasan per
 * pertemuan (akses, kehadiran dosen/mahasiswa, jumlah materi/tugas/audio/video). Angka-angka itu
 * dihitung dari ribuan pertemuan; menghitungnya saat halaman dibuka terasa lambat. Untuk
 * mempercepatnya sudah ada {@link ElearningRingkasanCache} — sebuah cache di MEMORI yang dihangatkan
 * (di-warm) saat aplikasi start. Masalahnya, cache memori itu HILANG setiap kali Tomcat di-restart,
 * dan proses warm-up dari database memerlukan waktu; akibatnya pengguna PERTAMA yang membuka
 * e-Learning sesaat setelah restart (sebelum warm-up selesai) masih harus menunggu.</p>
 *
 * <p>Kelas ini menutup celah tersebut dengan menyimpan SALINAN cache ringkasan ke BERKAS di hard
 * disk (mirip {@link FlagAccessCache} untuk flag). Polanya sederhana namun ampuh:</p>
 * <ol>
 *   <li><b>Saat berjalan normal</b>: isi {@link ElearningRingkasanCache} ditulis ke berkas secara
 *       berkala (default tiap 15 menit) di latar. Berkas ini berisi angka terakhir yang sudah
 *       dihitung.</li>
 *   <li><b>Saat aplikasi start (setelah restart)</b>: {@link #muatStartup()} MEMBACA berkas tersebut
 *       dan langsung mengisi cache memori — SEBELUM proses warm-up dari database berjalan. Dengan
 *       begitu, begitu Tomcat siap, angka ringkasan sudah tersedia di memori dan halaman e-Learning
 *       dapat tampil seketika tanpa menghitung ulang ke database.</li>
 *   <li><b>Penyegaran</b>: warm-up dari database tetap berjalan di latar untuk memperbarui angka
 *       sesuai data terbaru; jadi data dari disk dipakai untuk "tampil cepat", lalu disusul angka
 *       paling mutakhir tanpa pengguna merasakan jeda.</li>
 * </ol>
 *
 * <h2>Mengapa pola "tampilkan dulu dari disk, segarkan kemudian" (stale-while-revalidate)?</h2>
 * <p>Ini praktik umum di sistem berkinerja tinggi: pengguna selalu mendapat tampilan instan dari
 * data yang "cukup baru" (hasil hitung terakhir sebelum restart), sementara sistem menyegarkan data
 * di belakang layar. Trade-off-nya: tepat setelah restart, angka yang tampil bisa terpaut beberapa
 * menit dari kondisi terkini (selama warm-up belum selesai) — sebuah kompromi yang sangat layak demi
 * tampilan yang spontan. Karena cache ini hanya menyimpan ANGKA agregat (bukan data pribadi atau
 * dokumen), risiko keamanannya minimal.</p>
 *
 * <h2>Lokasi berkas</h2>
 * <p>Berkas disimpan di {@code {catalina.base}/cache/{Common.ROOT}/elearning_ringkasan.dat}
 * (jatuh balik ke {@code catalina.home} lalu {@code java.io.tmpdir} bila perlu, dan nama tenant
 * di-sanitasi). Penulisan dilakukan ke berkas sementara lalu di-rename (atomik) agar berkas tidak
 * pernah dalam keadaan setengah tertulis bila proses terhenti mendadak.</p>
 *
 * <h2>Format penyimpanan</h2>
 * <p>Memakai serialisasi objek Java standar atas {@code Map<Long, ElearningRingkasanCache.Agg>}
 * (Agg adalah {@link java.io.Serializable}). Dipilih karena struktur datanya berupa Map bersarang
 * berisi bilangan — ringkas, cepat, dan tidak memerlukan pustaka tambahan. Bila format kelas Agg
 * berubah di masa depan, {@code serialVersionUID} dan blok try-catch memastikan kegagalan baca
 * berkas lama TIDAK menghentikan aplikasi (cache cukup dibangun ulang dari database).</p>
 *
 * <h2>Ketahanan (robustness)</h2>
 * <p>Seluruh operasi I/O dibungkus penanganan kesalahan "gagal-diam": bila berkas tidak ada, rusak,
 * atau tidak dapat ditulis, aplikasi tetap berjalan normal — hanya kehilangan keuntungan "tampil
 * instan" untuk akses pertama. Penjadwal penyimpanan adalah thread daemon sehingga tidak menahan
 * proses shutdown. Tidak memakai lambda / try-with-resources agar tetap kompatibel Java 1.7, sesuai
 * gaya paket {@code ais.common}.</p>
 *
 * <h2>Cara pakai (di {@code AppStartupListener})</h2>
 * <pre>
 *   ElearningFlagAccessCache.muatStartup();              // 1) isi cache dari disk -&gt; siap instan
 *   ElearningFlagAccessCache.mulaiPenyimpananBerkala();  // 2) tulis ke disk tiap 15 menit
 *   ElearningRingkasanCache.warmupStartup();             // 3) segarkan dari DB di latar
 * </pre>
 *
 * @see ElearningRingkasanCache
 * @see FlagAccessCache
 */
public final class ElearningFlagAccessCache {

    private ElearningFlagAccessCache() {
    }

    private static final String NAMA_BERKAS = "elearning_ringkasan.dat";
    private static final long INTERVAL_SIMPAN_MENIT = 15;

    /** Scheduler penyimpanan berkala (disimpan agar bisa di-shutdown saat webapp berhenti). */
    private static volatile ScheduledExecutorService penyimpan;

    private static ThreadFactory daemonFactory(final String nama) {
        return new ThreadFactory() {
            private final AtomicInteger n = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, nama + "-" + n.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
    }

    /** Lokasi berkas cache: {catalina.base}/cache/{Common.ROOT}/elearning_ringkasan.dat */
    private static File berkasCache() {
        String base = System.getProperty("catalina.base");
        if (base == null || base.trim().isEmpty()) {
            base = System.getProperty("catalina.home");
        }
        if (base == null || base.trim().isEmpty()) {
            base = System.getProperty("java.io.tmpdir");
        }
        String root = (Common.ROOT == null ? "default" : Common.ROOT).replaceAll("[\\\\/:*?\"<>|]", "_");
        if (root.trim().isEmpty()) {
            root = "default";
        }
        File dir = new File(new File(base, "cache"), root);
        try {
            dir.mkdirs();
        } catch (Throwable ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/ElearningFlagAccessCache.java:120");
        }
        return new File(dir, NAMA_BERKAS);
    }

    /**
     * Tulis salinan cache ringkasan e-Learning saat ini ke berkas (tulis sementara lalu rename =
     * atomik). Aman dipanggil kapan saja; bila cache kosong, tidak menulis apa pun.
     */
    @SuppressWarnings("unchecked")
    public static synchronized void simpanKeFile() {
        ObjectOutputStream oos = null;
        try {
            Map<Long, ElearningRingkasanCache.Agg> data = ElearningRingkasanCache.salinIsiCache();
            if (data == null || data.isEmpty()) {
                return;
            }
            File f = berkasCache();
            File tmp = new File(f.getAbsolutePath() + ".tmp");
            oos = new ObjectOutputStream(new FileOutputStream(tmp));
            oos.writeObject(data);
            oos.flush();
            oos.close();
            oos = null;
            if (f.exists()) {
                f.delete();
            }
            if (!tmp.renameTo(f)) {
                tmp.delete();
            }
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningFlagAccessCache.java:150");
            // gagal-diam: cache disk hanya pendukung.
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (Throwable ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/ElearningFlagAccessCache.java:156");
                }
            }
        }
    }

    /**
     * Baca berkas cache (bila ada) dan muat ke {@link ElearningRingkasanCache}. Mengembalikan jumlah
     * pertemuan yang berhasil dimuat (0 bila berkas tidak ada/rusak).
     */
    @SuppressWarnings("unchecked")
    public static int muatDariFile() {
        ObjectInputStream ois = null;
        try {
            File f = berkasCache();
            if (!f.exists() || f.length() <= 0) {
                return 0;
            }
            ois = new ObjectInputStream(new FileInputStream(f));
            Object obj = ois.readObject();
            ois.close();
            ois = null;
            if (obj instanceof Map) {
                Map<Long, ElearningRingkasanCache.Agg> data = (Map<Long, ElearningRingkasanCache.Agg>) obj;
                ElearningRingkasanCache.muatKeCacheDariPersisten(data);
                return data.size();
            }
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningFlagAccessCache.java:183");
            // gagal-diam: bila berkas lama tak kompatibel, cache cukup dibangun ulang dari DB.
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (Throwable ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/ElearningFlagAccessCache.java:189");
                }
            }
        }
        return 0;
    }

    /**
     * Muat cache dari disk saat aplikasi start (sinkron, cepat — tanpa query DB). Dipanggil PALING
     * AWAL agar halaman e-Learning sudah punya angka begitu Tomcat siap, sebelum warm-up DB selesai.
     */
    public static void muatStartup() {
        try {
            int n = muatDariFile();
            System.out.println("ElearningFlagAccessCache: muat " + n + " pertemuan dari disk (tampil cepat).");
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningFlagAccessCache.java:204");
        }
    }

    /** Jadwalkan penyimpanan cache ke disk tiap 15 menit di latar (daemon). */
    public static synchronized void mulaiPenyimpananBerkala() {
        try {
            if (penyimpan != null) {
                return;
            }
            ScheduledExecutorService sch = Executors.newScheduledThreadPool(1,
                    daemonFactory("elearning-ringkasan-saver"));
            sch.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        simpanKeFile();
                    } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningFlagAccessCache.java:221");
                    }
                }
            }, INTERVAL_SIMPAN_MENIT, INTERVAL_SIMPAN_MENIT, TimeUnit.MINUTES);
            penyimpan = sch;
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningFlagAccessCache.java:226");
        }
    }

    /**
     * Hentikan scheduler penyimpanan berkala. Dipanggil dari
     * {@code AppStartupListener.contextDestroyed} saat webapp berhenti/di-reload agar thread
     * {@code elearning-ringkasan-saver-*} benar-benar berhenti (cegah warning "failed to stop it" /
     * kebocoran classloader Tomcat). Sebelum berhenti, sekali lagi simpan ke berkas.
     */
    public static synchronized void hentikanPenyimpananBerkala() {
        ScheduledExecutorService sch = penyimpan;
        penyimpan = null;
        if (sch != null) {
            try {
                simpanKeFile();
            } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningFlagAccessCache.java:242");
            }
            try {
                sch.shutdownNow();
            } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningFlagAccessCache.java:246");
            }
        }
    }
}
