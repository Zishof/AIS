package ais.action.master.library.modern;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

/** Bounded per-node fixed-window limiter. A shared gateway remains required for cluster-wide enforcement. */
public final class LibraryRateLimiter {
    private static final int MAX_KEYS = 10000;
    private static final Map<String, Window> WINDOWS = new ConcurrentHashMap<String, Window>();
    private LibraryRateLimiter() { }

    public static boolean allow(HttpServletRequest request, String namespace, int limit, long windowMillis) {
        String remote = request == null ? "unknown" : clean(request.getRemoteAddr(), 80);
        return allow(namespace, remote, limit, windowMillis, System.currentTimeMillis());
    }

    public static boolean allow(String namespace, String remote, int limit, long windowMillis) {
        return allow(namespace, remote, limit, windowMillis, System.currentTimeMillis());
    }

    static boolean allow(String namespace, String remote, int limit, long windowMillis, long now) {
        if (namespace == null || !namespace.matches("[a-z0-9-]{1,40}") || limit < 1 || windowMillis < 1000L) return false;
        String key = namespace + ':' + clean(remote, 80);
        Window existing = WINDOWS.get(key);
        if (existing != null) return increment(existing, limit, windowMillis, now);
        if (WINDOWS.size() >= MAX_KEYS) cleanup(now, windowMillis);
        if (WINDOWS.size() >= MAX_KEYS) return false;
        Window created = new Window(now);
        Window window = WINDOWS.putIfAbsent(key, created);
        if (window == null) window = created;
        return increment(window, limit, windowMillis, now);
    }

    private static boolean increment(Window window, int limit, long windowMillis, long now) {
        synchronized (window) {
            if (now - window.startedAt >= windowMillis || now < window.startedAt) {
                window.startedAt = now;
                window.count = 1;
                return true;
            }
            window.count++;
            return window.count <= limit;
        }
    }

    private static void cleanup(long now, long windowMillis) {
        for (Map.Entry<String, Window> entry : WINDOWS.entrySet()) {
            if (now - entry.getValue().startedAt >= windowMillis * 2L) WINDOWS.remove(entry.getKey(), entry.getValue());
        }
    }

    private static String clean(String value, int max) {
        if (value == null) return "unknown";
        value = value.trim().replaceAll("[^A-Za-z0-9:._-]", "_");
        if (value.length() == 0) return "unknown";
        return value.length() > max ? value.substring(0, max) : value;
    }

    /**
     * Tipe implementasi bersarang {@link Window} milik {@link LibraryRateLimiter}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link LibraryRateLimiter}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code long startedAt}, {@code int count}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see LibraryRateLimiter
     */
    private static final class Window {
        private long startedAt;
        private int count;
        private Window(long startedAt) { this.startedAt = startedAt; }
    }
}
