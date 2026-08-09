package ais.common;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.ErrorLog;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;

/**
 * Util global untuk mencatat error Java/ZK/Servlet ke database ErrorLog.
 *
 * Versi ini sengaja tidak lagi membuat file fisik error_*.txt. Semua catatan
 * error dipusatkan ke tabel error_log agar mudah diaudit dari menu Error Log.
 * Kompatibel Java 1.6/1.7.
 */
public class ErrorAuditUtil {

    private static final Logger log = Logger.getLogger(ErrorAuditUtil.class);

    public static final String KEY_ACTIVE = "global_error_log_aktif";
    public static final String KEY_DATABASE = "global_error_log_simpan_database";
    public static final String KEY_SERVER_LOG = "global_error_log_server_log";
    public static final String KEY_REQUEST_DETAIL = "global_error_log_request_detail";
    public static final String KEY_MAX_CONTENT_LENGTH = "global_error_log_max_content_length";
    public static final String KEY_IGNORE_CLIENT_ABORT = "global_error_log_abaikan_client_abort";
    public static final String KEY_UI_DETAIL = "global_error_tampilkan_stacktrace_ui";

    public static final int DEFAULT_MAX_CONTENT_LENGTH = 250000;

    private static final ThreadLocal<Boolean> RECORDING = new ThreadLocal<Boolean>();
    private static final ThreadLocal<ErrorAuditResult> LAST_RESULT = new ThreadLocal<ErrorAuditResult>();

    // DEDUP in-memory: satu LOKASI error (kelas+baris tempat error muncul, atau 'info' bila fast-throw)
    // hanya disimpan 1x selama masa hidup JVM. Dicek lewat Set ini TANPA query DB (performa). Di-reset
    // via resetDedup() saat "Hapus Semua" di ErrorLogAction. Berlaku juga untuk error JSP (lewat
    // ErrorAuditFilter). ConcurrentHashMap + newSetFromMap = aman thread & kompatibel Java 1.6.
    private static final java.util.Set<String> SUDAH_DICATAT = java.util.Collections
            .newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());
    // Jaring pengaman agar Set tak tumbuh tanpa batas (lokasi kode berhingga; ini hanya antisipasi).
    private static final int MAX_DEDUP = 50000;

    // Pola parse teks error tersimpan (untuk SEED fingerprint dari DB saat startup). HARUS konsisten
    // dengan hitungFingerprintLokasi: frame teratas "at kelas.method(File.java:baris)" -> kelas.method:baris.
    private static final java.util.regex.Pattern POLA_FRAME = java.util.regex.Pattern
            .compile("\\bat\\s+([\\w$.]+)\\([^)]*?:(\\d+)\\)");
    private static final java.util.regex.Pattern POLA_INFO = java.util.regex.Pattern
            .compile("(?m)^Info:\\s*(.+)$");

    public static class ErrorAuditResult {
        private final String content;
        private final Long errorLogId;

        public ErrorAuditResult(String content, Long errorLogId) {
            this.content = content;
            this.errorLogId = errorLogId;
        }

        public String getContent() {
            return content;
        }

        public Long getErrorLogId() {
            return errorLogId;
        }
    }

    public static ErrorAuditResult record(Throwable throwable, String info) {
        return record(throwable, info, null);
    }

    public static ErrorAuditResult record(Throwable throwable, String info, HttpServletRequest request) {
        return record(throwable, info, request, true);
    }

    /**
     * Overload kompatibilitas untuk pemanggilan lama.
     *
     * Parameter rethrow tidak dipakai oleh util ini. Proses lempar ulang exception tetap
     * dikendalikan oleh pemanggil, misalnya ErrorAuditFilter. Parameter ini sengaja
     * dipertahankan agar class lama yang sudah memanggil record(..., false) tetap compile.
     */
    public static ErrorAuditResult record(Throwable throwable, String info, HttpServletRequest request, boolean rethrow) {
        if (isClientAbortIgnored(throwable)) {
            ErrorAuditResult result = new ErrorAuditResult("", null);
            LAST_RESULT.set(result);
            return result;
        }

        // DEDUP 1x-per-lokasi: bila kelas+baris (atau info fast-throw) ini SUDAH pernah dicatat sejak
        // start/last-reset, JANGAN catat lagi. Cek in-memory (tanpa DB) supaya performa tak turun.
        // add() -> true bila baru, false bila sudah ada. Reset via resetDedup() (menu Hapus Semua).
        boolean lokasiBaru;
        try {
            String fingerprintLokasi = hitungFingerprintLokasi(throwable, info);
            if (SUDAH_DICATAT.size() >= MAX_DEDUP) {
                SUDAH_DICATAT.clear();
            }
            lokasiBaru = SUDAH_DICATAT.add(fingerprintLokasi);
        } catch (Throwable ignoredDedup) {
            lokasiBaru = true; // bila dedup gagal, jangan sampai menghalangi pencatatan
        }
        if (!lokasiBaru) {
            // Database tetap dedup 1x-per-lokasi, tetapi setiap kejadian harus tetap masuk server log.
            // Ini penting untuk melihat frekuensi dan konteks request terbaru di catalina.out.
            String duplicateContent = buildErrorText(throwable, info, request);
            if (isServerLogActive()) {
                writeToServerLog(throwable, duplicateContent);
            }
            ErrorAuditResult result = new ErrorAuditResult(duplicateContent, null);
            LAST_RESULT.set(result);
            return result;
        }

        String content = buildErrorText(throwable, info, request);
        Long errorLogId = null;

        if (Boolean.TRUE.equals(RECORDING.get())) {
            writeToServerLog(throwable, content);
            return new ErrorAuditResult(content, null);
        }

        try {
            RECORDING.set(Boolean.TRUE);

            if (isServerLogActive()) {
                writeToServerLog(throwable, content);
            }

            if (isActive() && isDatabaseActive()) {
                errorLogId = saveToDatabase(content, request);
            }

            ErrorAuditResult result = new ErrorAuditResult(content, errorLogId);
            LAST_RESULT.set(result);
            return result;
        } catch (Throwable t) {
            try {
                log.error("Gagal mencatat error ke ErrorLog", t);
                System.err.println(content);
            } catch (Throwable ignored) {
            }
            return new ErrorAuditResult(content, errorLogId);
        } finally {
            RECORDING.set(null);
        }
    }

    public static ErrorAuditResult getLastResult() {
        return LAST_RESULT.get();
    }

    /**
     * Mencatat kegagalan request/include yang harus terlihat pada halaman error.
     * Jika server-log dimatikan lewat konfigurasi, System.err tetap digunakan agar
     * kegagalan request tidak hilang dari catalina.out.
     */
    public static ErrorAuditResult recordVisibleFailure(Throwable throwable, String info,
            HttpServletRequest request, String traceId) {
        String decorated = "trace=" + safeTrace(traceId) + (info == null ? "" : " | " + info);
        ErrorAuditResult result = record(throwable, decorated, request, false);
        String content = result == null ? null : result.getContent();
        if (content == null || content.length() == 0) {
            content = buildErrorText(throwable, decorated, request);
            result = new ErrorAuditResult(content, result == null ? null : result.getErrorLogId());
        }
        if (!isServerLogActive()) {
            try {
                System.err.println("[AIS-REQUEST-ERROR] " + decorated);
                System.err.println(content);
            } catch (Throwable ignored) {
            }
        }
        return result;
    }

    public static boolean isUiDetailActive() {
        return isKonfigurasiActive(KEY_UI_DETAIL, Konfigurasi.TIDAK_AKTIF);
    }

    private static String safeTrace(String value) {
        return value == null || value.trim().length() == 0 ? "NO-TRACE" : value.trim();
    }

    /**
     * Sidik jari LOKASI error (dasar dedup 1x-per-lokasi). Sengaja HANYA lokasi kode (tanpa tipe
     * exception) agar bisa direkonstruksi SAMA PERSIS dari teks tersimpan (lihat
     * {@link #hitungFingerprintDariKonten}) untuk keperluan seed saat startup.
     * - Ada stack trace: frame TERATAS -> "namaKelasLengkap.method:baris" (lokasi asli error muncul).
     * - Fast-throw / stack kosong (mis. NPE implisit berulang): frame tak ada -> pakai 'info' pemanggil
     *   (biasanya memuat "file.java:baris") sebagai pembeda lokasi.
     */
    private static String hitungFingerprintLokasi(Throwable throwable, String info) {
        StackTraceElement[] frames = null;
        try {
            if (throwable != null) {
                frames = throwable.getStackTrace();
            }
        } catch (Throwable ignored) {
        }
        if (frames != null && frames.length > 0) {
            StackTraceElement top = frames[0];
            return top.getClassName() + "." + top.getMethodName() + ":" + top.getLineNumber();
        }
        return "INFO:" + (info == null ? "" : info.trim());
    }

    /**
     * Rekonstruksi fingerprint lokasi dari TEKS error tersimpan (kolom keterangan). Dipakai saat SEED
     * dari DB; hasilnya HARUS sama dengan {@link #hitungFingerprintLokasi} agar dedup cocok lintas restart.
     * - Ada baris "at kelas.method(File.java:baris)" pertama -> "kelas.method:baris".
     * - Tidak ada frame (fast-throw) -> pakai baris "Info: X" -> "INFO:X".
     */
    private static String hitungFingerprintDariKonten(String konten) {
        if (konten == null) {
            return null;
        }
        java.util.regex.Matcher mFrame = POLA_FRAME.matcher(konten);
        if (mFrame.find()) {
            return mFrame.group(1) + ":" + mFrame.group(2);
        }
        java.util.regex.Matcher mInfo = POLA_INFO.matcher(konten);
        if (mInfo.find()) {
            return "INFO:" + mInfo.group(1).trim();
        }
        return null;
    }

    /**
     * Kosongkan penanda dedup in-memory sehingga setiap lokasi error boleh dicatat lagi 1x.
     * Dipanggil dari {@code ErrorLogAction} saat pengguna klik "Hapus Semua" / reset log error.
     */
    public static void resetDedup() {
        try {
            SUDAH_DICATAT.clear();
        } catch (Throwable ignored) {
        }
    }

    /**
     * SEED sekali saat startup: muat lokasi error yang SUDAH ada di tabel error_log ke penanda dedup
     * in-memory, agar error yang sama TIDAK tercatat ulang tiap kali aplikasi restart. Hanya SATU query
     * (bukan cek per-error). Membaca potongan awal 'keterangan' saja (hemat memori) & dibatasi jumlah
     * baris (jaring pengaman bila tabel besar). Aman gagal (di-try/catch); openSession() ditutup di finally.
     */
    public static void seedDedupFromDatabase() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            java.util.List<?> rows = session
                    .createSQLQuery("select left(keterangan, 3000) from error_log order by id desc limit 20000")
                    .list();
            int dimuat = 0;
            for (int i = 0; i < rows.size(); i++) {
                Object k = rows.get(i);
                if (k == null) {
                    continue;
                }
                String fp = hitungFingerprintDariKonten(k.toString());
                if (fp != null && SUDAH_DICATAT.add(fp)) {
                    dimuat++;
                }
            }
            try {
                log.info("ErrorAudit dedup seed: " + dimuat + " lokasi dimuat dari error_log (" + rows.size()
                        + " baris dibaca).");
            } catch (Throwable ignoredLog) {
            }
        } catch (Throwable t) {
            try {
                log.warn("Seed dedup ErrorAudit dari DB dilewati: " + t.getMessage());
            } catch (Throwable ignored) {
            }
        } finally {
            if (session != null) {
                try {
                    if (session.isOpen()) {
                        session.clear();
                    }
                } catch (Throwable ignored) {
                }
                try {
                    if (session.isOpen()) {
                        session.close();
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public static String buildErrorText(Throwable throwable, String info, HttpServletRequest request) {
        StringBuilder builder = new StringBuilder(8192);
        builder.append("Waktu: ").append(new Date()).append("\n");
        builder.append("Root Aplikasi: ").append(Common.ROOT == null ? "" : Common.ROOT).append("\n");
        builder.append("Thread: ").append(Thread.currentThread() == null ? "" : Thread.currentThread().getName())
                .append("\n");

        Tbmuser user = resolveUser(request);
        if (user != null) {
            try {
                builder.append("User ID: ").append(user.getUserId()).append("\n");
            } catch (Throwable ignored) {
            }
            try {
                builder.append("User: ").append(user.toString()).append("\n");
            } catch (Throwable ignored) {
            }
        }

        if (info != null && info.trim().length() > 0) {
            builder.append("Info: ").append(info.trim()).append("\n");
        }

        if (request != null && isRequestDetailActive()) {
            appendRequestInfo(builder, request);
        }

        builder.append("\n");
        builder.append(getStackTraceAsString(throwable));

        String value = builder.toString();
        int max = getMaxContentLength();
        if (max > 0 && value.length() > max) {
            value = value.substring(0, max) + "\n\n... STACK TRACE DIPOTONG. Panjang asli: " + value.length();
        }
        return value;
    }

    private static void appendRequestInfo(StringBuilder builder, HttpServletRequest request) {
        try {
            builder.append("Request Method: ").append(request.getMethod()).append("\n");
        } catch (Throwable ignored) {
        }
        try {
            builder.append("Request URI: ").append(request.getRequestURI()).append("\n");
        } catch (Throwable ignored) {
        }
        try {
            builder.append("Request URL: ").append(request.getRequestURL()).append("\n");
        } catch (Throwable ignored) {
        }
        try {
            builder.append("Query String: ").append(request.getQueryString()).append("\n");
        } catch (Throwable ignored) {
        }
        try {
            builder.append("Remote Addr: ").append(request.getRemoteAddr()).append("\n");
        } catch (Throwable ignored) {
        }
        try {
            builder.append("User-Agent: ").append(request.getHeader("User-Agent")).append("\n");
        } catch (Throwable ignored) {
        }
    }

    public static String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            pw.flush();
            String hasil = sw.toString();
            // Deteksi "fast-throw" JVM (HotSpot): exception IMPLISIT (NullPointerException,
            // ArrayIndexOutOfBounds, ArithmeticException, ClassCast, dll.) yang terjadi BERULANG di
            // lokasi bytecode yang SAMA akan dilempar sebagai instance PRA-ALOKASI TANPA stack trace
            // demi performa. Akibatnya log hanya berisi nama kelas exception (mis.
            // "java.lang.NullPointerException") tanpa satu pun baris "at ...". Beri catatan agar
            // operator tahu ini BUKAN log yang rusak, sekaligus cara memunculkan stack trace aslinya.
            StackTraceElement[] frames = throwable.getStackTrace();
            if (frames == null || frames.length == 0) {
                hasil = hasil
                        + "\n[CATATAN DIAGNOSA] Stack trace KOSONG = optimasi JVM 'fast-throw': exception implisit ("
                        + throwable.getClass().getName()
                        + ") berulang di lokasi yang sama sehingga JVM menghilangkan stack trace-nya. Untuk"
                        + " memunculkan stack trace ASLI (lokasi sumber), tambahkan flag JVM"
                        + " -XX:-OmitStackTraceInFastThrow (setenv.sh / JAVA_OPTS) lalu restart & picu ulang.";
            }
            return hasil;
        } catch (Throwable t) {
            return String.valueOf(throwable);
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static Long saveToDatabase(String content, HttpServletRequest request) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            ErrorLog errorLog = new ErrorLog();
            errorLog.setKeterangan(content == null ? "" : content);

            Tbmuser user = resolveUser(request);
            if (user != null) {
                try {
                    errorLog.setOlehId(user.getUserId());
                } catch (Throwable ignored) {
                }
                try {
                    errorLog.setOleh(user.toString());
                } catch (Throwable ignored) {
                }
            }

            session.save(errorLog);
            transaction.commit();
            return errorLog.getId();
        } catch (Throwable t) {
            try {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
            } catch (Throwable ignored) {
            }
            try {
                log.error("Gagal menyimpan catatan error ke tabel error_log", t);
            } catch (Throwable ignored) {
            }
            return null;
        } finally {
            if (session != null) {
                try {
                    session.clear();
                } catch (Throwable ignored) {
                }
                try {
                    session.disconnect();
                } catch (Throwable ignored) {
                }
                try {
                    session.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static Tbmuser resolveUser(HttpServletRequest request) {
        try {
            if (request != null) {
                Tbmuser user = Common.getCurrentUser(request);
                if (user != null) {
                    return user;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            return Common.getCurrentUser();
        } catch (Throwable ignored) {
        }
        return null;
    }


    public static boolean isClientAbortIgnored(Throwable throwable) {
        if (!isClientAbort(throwable)) {
            return false;
        }
        return isKonfigurasiActive(KEY_IGNORE_CLIENT_ABORT, Konfigurasi.AKTIF);
    }

    public static boolean isClientAbort(Throwable throwable) {
        Throwable t = throwable;
        int guard = 0;
        while (t != null && guard < 25) {
            String className = t.getClass() == null ? "" : t.getClass().getName();
            String message = t.getMessage();

            if ("org.apache.catalina.connector.ClientAbortException".equals(className)) {
                return true;
            }
            if ("org.eclipse.jetty.io.EofException".equals(className)) {
                return true;
            }

            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.indexOf("broken pipe") >= 0
                        || lower.indexOf("connection reset by peer") >= 0
                        || lower.indexOf("connection reset") >= 0
                        || lower.indexOf("clientabortexception") >= 0
                        || lower.indexOf("software caused connection abort") >= 0
                        || lower.indexOf("forcibly closed by the remote host") >= 0
                        || lower.indexOf("connection closed") >= 0
                        || lower.indexOf("stream closed") >= 0
                        || lower.indexOf("session already invalidated") >= 0
                        || lower.indexOf("cannot create a session after the response has been committed") >= 0) {
                    return true;
                }
            }

            t = t.getCause();
            guard++;
        }
        return false;
    }

    public static boolean isActive() {
        return isKonfigurasiActive(KEY_ACTIVE, Konfigurasi.AKTIF);
    }

    public static boolean isDatabaseActive() {
        return isKonfigurasiActive(KEY_DATABASE, Konfigurasi.AKTIF);
    }

    private static boolean isServerLogActive() {
        return isKonfigurasiActive(KEY_SERVER_LOG, Konfigurasi.AKTIF);
    }

    private static boolean isRequestDetailActive() {
        return isKonfigurasiActive(KEY_REQUEST_DETAIL, Konfigurasi.AKTIF);
    }

    private static boolean isKonfigurasiActive(String key, String defaultValue) {
        String value = getKonfigurasi(key, defaultValue);
        return value == null || value.trim().length() == 0 || Konfigurasi.AKTIF.equalsIgnoreCase(value.trim())
                || "true".equalsIgnoreCase(value.trim()) || "ya".equalsIgnoreCase(value.trim());
    }

    private static int getMaxContentLength() {
        String value = getKonfigurasi(KEY_MAX_CONTENT_LENGTH, String.valueOf(DEFAULT_MAX_CONTENT_LENGTH));
        try {
            return Integer.parseInt(value == null ? String.valueOf(DEFAULT_MAX_CONTENT_LENGTH) : value.trim());
        } catch (Throwable t) {
            return DEFAULT_MAX_CONTENT_LENGTH;
        }
    }

    private static String getKonfigurasi(String key, String defaultValue) {
        try {
            Konfigurasi konfigurasi = Common.getKonfigurasi(key, defaultValue);
            if (konfigurasi != null && konfigurasi.getNilai() != null) {
                return konfigurasi.getNilai();
            }
        } catch (Throwable ignored) {
        }
        return defaultValue;
    }

    private static void writeToServerLog(Throwable throwable, String content) {
        try {
            if (throwable != null) {
                log.error(content, throwable);
            } else {
                log.error(content);
            }
        } catch (Throwable t) {
            try {
                System.err.println(content);
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
