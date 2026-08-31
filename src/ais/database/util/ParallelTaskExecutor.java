package ais.database.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ais.common.Common;

/**
 * Helper untuk memproses koleksi data secara paralel.
 * Kompatibel dengan Java 1.6 / 1.7.
 *
 * Catatan performa:
 * - Default report dibuat lebih agresif sampai 250 thread.
 * - Nilai tetap bisa dikendalikan dari konfigurasi report_parallel_max_threads.
 * - Jika database/server terasa berat, turunkan konfigurasi ke 50 / 100 / 150.
 */
public class ParallelTaskExecutor {

    public static final int DEFAULT_MAX_THREADS = 250;
    public static final int DEFAULT_REPORT_MAX_THREADS = 250;
    private static final int MIN_THREADS = 1;
    private static final int HARD_MAX_THREADS = 250;
    private static final long DEFAULT_AWAIT_TERMINATION_SECONDS = 300L;

    /**
     * Kontrak callback/strategi bersarang milik {@link ParallelTaskExecutor}. Tipe ini memisahkan satu variasi
     * perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link ParallelTaskExecutor} dan dapat mengakses
     * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code execute}(). Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> pekerjaan berjalan di thread lain. Buka/tutup resource miliknya sendiri, jangan
     * memakai komponen ZK atau session Hibernate request tanpa aktivasi yang eksplisit, dan laporkan kegagalan
     * melalui mekanisme kelas induk.</p>
     *
     * @see ParallelTaskExecutor
     */
    public interface Task<T> {
        void execute(T item) throws Exception;
    }

    /**
     * Thread maksimum default untuk proses report.
     * Bisa dikonfigurasi melalui Konfigurasi: report_parallel_max_threads.
     */
    public static int getDefaultReportMaxThreads() {
        return getIntKonfigurasi("report_parallel_max_threads", DEFAULT_REPORT_MAX_THREADS, MIN_THREADS,
                HARD_MAX_THREADS);
    }

    /**
     * Thread maksimum umum. Bisa dikonfigurasi melalui Konfigurasi: parallel_task_max_threads.
     */
    public static int getDefaultMaxThreads() {
        return getIntKonfigurasi("parallel_task_max_threads", DEFAULT_MAX_THREADS, MIN_THREADS, HARD_MAX_THREADS);
    }

    /**
     * Timeout tunggu thread selesai. Bisa dikonfigurasi melalui: parallel_task_await_seconds.
     */
    public static long getAwaitTerminationSeconds() {
        int value = getIntKonfigurasi("parallel_task_await_seconds", (int) DEFAULT_AWAIT_TERMINATION_SECONDS, 10,
                3600);
        return value;
    }

    public static <T> void process(List<T> items, final Task<T> task) throws Exception {
        process(items, getDefaultMaxThreads(), task);
    }

    public static <T> void processReport(List<T> items, final Task<T> task) throws Exception {
        process(items, getDefaultReportMaxThreads(), task);
    }

    public static <T> void process(List<T> items, int maxThreads, final Task<T> task) throws Exception {
        if (items == null || items.isEmpty() || task == null) {
            return;
        }

        int threads = normalizeThreadCount(items.size(), maxThreads);
        ExecutorService executor = Executors.newFixedThreadPool(threads, new NamedThreadFactory("ais-parallel-task"));
        List<Future<Void>> futures = new ArrayList<Future<Void>>(items.size());
        boolean selesaiNormal = false;

        try {
            for (final T item : items) {
                futures.add(executor.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        task.execute(item);
                        return null;
                    }
                }));
            }

            for (Future<Void> future : futures) {
                future.get();
            }
            selesaiNormal = true;
        } finally {
            shutdownExecutor(executor, selesaiNormal);
        }
    }

    private static int normalizeThreadCount(int itemSize, int requestedThreads) {
        int threads = requestedThreads;
        if (threads <= 0) {
            threads = getDefaultMaxThreads();
        }
        if (threads < MIN_THREADS) {
            threads = MIN_THREADS;
        }
        if (threads > HARD_MAX_THREADS) {
            threads = HARD_MAX_THREADS;
        }
        if (itemSize > 0 && threads > itemSize) {
            threads = itemSize;
        }
        return threads <= 0 ? MIN_THREADS : threads;
    }

    private static void shutdownExecutor(ExecutorService executor, boolean selesaiNormal) throws Exception {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(getAwaitTerminationSeconds(), TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(30L, TimeUnit.SECONDS) && !selesaiNormal) {
                    throw new RuntimeException("ParallelTaskExecutor timeout: masih ada thread yang belum selesai.");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private static int getIntKonfigurasi(String nama, int defaultValue, int min, int max) {
        int value = defaultValue;
        try {
            String nilai = Common.getKonfigurasi(nama, String.valueOf(defaultValue)).getNilai();
            if (nilai != null && nilai.trim().length() > 0) {
                value = Integer.parseInt(nilai.trim());
            }
        } catch (Exception e) {
            value = defaultValue;
        }
        if (value < min) {
            value = min;
        }
        if (value > max) {
            value = max;
        }
        return value;
    }

    /**
     * Pekerjaan latar bersarang milik {@link ParallelTaskExecutor} untuk named thread factory. Tipe ini membatasi
     * state yang dibawa ke eksekusi asinkron dan tidak boleh membawa session request secara implisit.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link ParallelTaskExecutor}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String prefix}, {@code AtomicInteger
     * counter}; operasi lokal: {@code newThread}(). Aturan bisnis bersama tetap berada pada kelas induk atau
     * service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> pekerjaan berjalan di thread lain. Buka/tutup resource miliknya sendiri, jangan
     * memakai komponen ZK atau session Hibernate request tanpa aktivasi yang eksplisit, dan laporkan kegagalan
     * melalui mekanisme kelas induk.</p>
     *
     * @see ParallelTaskExecutor
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix == null || prefix.trim().length() == 0 ? "ais-parallel-task" : prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        }
    }
}
