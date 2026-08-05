package ais.common;

import java.util.regex.Pattern;

import ais.database.model.Konfigurasi;

/**
 * <h3>Lapis pertahanan untuk endpoint SQL mentah dari klien</h3>
 *
 * <p>Servlet {@code Data} menyediakan dua aksi yang menerima SQL mentah dari browser:
 * {@code action=sql} (baca, lewat {@link ais.action.servlet.api.DaftarDataService#sql}) dan
 * {@code action=update_data} (tulis, lewat {@link Common#updateSql}). Keduanya HANYA dijaga oleh
 * pengecekan "harus login" — tidak ada pembatasan tabel, peran, maupun lingkup toko, dan jalur
 * {@code tanpaLogin=true} pun bisa melewati pengecekan login. Akibatnya pengguna mana pun yang
 * login berpotensi membaca/mengubah/menghapus SELURUH basis data lewat DevTools, dan isolasi
 * pedagang yang ditegakkan di sisi server (Action ZK) bisa dilewati.</p>
 *
 * <p><b>Kelas ini = lapis-1 yang murah:</b> membatasi BENTUK SQL — {@code action=sql} wajib
 * read-only (SELECT), dan {@code action=update_data} menolak DDL/perintah berbahaya
 * (DROP/ALTER/TRUNCATE/CREATE/GRANT/REVOKE/VACUUM/…) serta akses objek sistem database. Ini TIDAK
 * menutup kebocoran-baca penuh (untuk itu butuh endpoint bertipe / whitelist tabel — proyek besar),
 * tetapi menghentikan eskalasi paling parah (tulis lewat endpoint baca, perusakan skema).</p>
 *
 * <h3>Cara kerja &amp; mode (KONFIGURASI, default AMAN)</h3>
 * <p>Perilaku dikendalikan konfigurasi {@code mode_proteksi_sql_endpoint} (tabel public.konfigurasi):</p>
 * <ul>
 *   <li><b>off</b> (DEFAULT) — guard non-aktif total; perilaku persis seperti sebelum kelas ini ada
 *       (tidak ada yang diblok, tidak ada log). Aman untuk di-deploy tanpa mengubah apa pun.</li>
 *   <li><b>log</b> — TIDAK memblok apa pun, hanya mencetak ke log SQL yang SEHARUSNYA diblok. Dipakai
 *       untuk observasi/audit sebelum penegakan, agar ketahuan dulu adakah pemakaian sah yang akan
 *       terkena (false positive).</li>
 *   <li><b>enforce</b> — benar-benar menolak SQL yang melanggar (kembalikan status gagal).</li>
 * </ul>
 *
 * <h3>Pemeliharaan</h3>
 * <p><b>Fail-open by design:</b> bila membaca konfigurasi atau evaluasi melempar exception, guard
 * mengembalikan "diizinkan" — guard tidak boleh menjadi penyebab aplikasi rusak. Daftar kata kunci
 * sengaja konservatif (memakai batas-kata {@code \\b}) agar nama kolom seperti {@code created_at},
 * {@code is_deleted}, {@code total_update} TIDAK ikut tertangkap; isi string literal di-mask dulu
 * supaya token di dalam {@code '...'} tidak ikut dipindai. Bila menambah kata kunci, hati-hati
 * terhadap nama kolom umum (mis. {@code comment}, {@code owner}, {@code status} sengaja TIDAK
 * dimasukkan ke daftar tulis karena lazim sebagai nama kolom).</p>
 */
public class SqlSecurityGuard {

    public static final String MODE_OFF = "off";
    public static final String MODE_LOG = "log";
    public static final String MODE_ENFORCE = "enforce";

    /** Mengganti isi string literal SQL ('...' termasuk escape '') agar tidak ikut dipindai. */
    private static final Pattern STRING_LITERAL = Pattern.compile("'(?:[^']|'')*'");

    /** Kata kunci tulis/DDL yang dilarang pada action=sql (read-only). */
    private static final Pattern WRITE_OR_DDL_READ = Pattern.compile(
            "\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|merge|call|copy|vacuum|reindex|cluster|into)\\b");

    /** DDL/perintah berbahaya yang dilarang pada action=update_data (DML biasa tetap boleh). */
    private static final Pattern DDL_DANGEROUS_WRITE = Pattern.compile(
            "\\b(drop|alter|truncate|create|grant|revoke|vacuum|reindex|cluster|copy)\\b");

    /** Objek/skema sistem database yang tidak boleh disentuh dari endpoint klien. */
    private static final Pattern SYS_OBJECTS = Pattern.compile(
            "(information_schema|pg_catalog|pg_authid|pg_shadow|pg_roles|pg_user\\b|pg_database)");

    /** Hasil evaluasi: boleh atau tidak, beserta alasannya. */
    public static class Result {
        public final boolean allowed;
        public final String reason;

        Result(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }
    }

    private static final Result OK = new Result(true, "");
    private static final String[] EMPTY = new String[0];

    /**
     * Daftar default token sensitif yang TIDAK boleh muncul di SQL klien (baca maupun tulis).
     * Sengaja memakai nama KOLOM kredensial, bukan nama tabel: {@code tbmuser} sendiri lazim dibaca
     * secara sah (mis. {@code SELECT userid FROM tbmuser} untuk cek keunikan saat registrasi member),
     * jadi memblok seluruh tabel akan merusak fitur; sebaliknya kolom {@code userpassword}/password
     * tidak pernah perlu dibaca/ditulis dari browser. Admin bisa menambah nama tabel/kolom lain
     * (mis. tabel gaji) lewat konfigurasi {@code daftar_token_terlarang_sql} tanpa redeploy.
     */
    private static final String DEFAULT_TOKEN_TERLARANG = "password,sandi,userpassword";

    private SqlSecurityGuard() {
    }

    /** Membaca mode aktif dari konfigurasi; fail-safe ke {@link #MODE_OFF}. */
    private static String mode() {
        try {
            Konfigurasi k = Common.getKonfigurasi("mode_proteksi_sql_endpoint", MODE_OFF);
            String m = (k == null || k.getNilai() == null) ? MODE_OFF : k.getNilai().trim().toLowerCase();
            if (MODE_LOG.equals(m) || MODE_ENFORCE.equals(m)) {
                return m;
            }
            return MODE_OFF;
        } catch (Exception e) {
            return MODE_OFF;
        }
    }

    /** Daftar token (kolom/tabel sensitif) dari konfigurasi {@code daftar_token_terlarang_sql}. */
    private static String[] sensitiveTokens() {
        try {
            Konfigurasi k = Common.getKonfigurasi("daftar_token_terlarang_sql", DEFAULT_TOKEN_TERLARANG);
            String raw = (k == null || k.getNilai() == null) ? "" : k.getNilai().trim().toLowerCase();
            if (raw.isEmpty()) {
                return EMPTY;
            }
            return raw.split(",");
        } catch (Exception e) {
            return EMPTY;
        }
    }

    /**
     * Menolak SQL klien (baca/tulis) yang menyentuh token sensitif (mis. kolom kredensial
     * {@code userpassword}). Pencocokan substring case-insensitive supaya varian seperti
     * {@code user_password}/{@code password_hash} ikut tertangkap. Fail-open bila error.
     */
    private static Result checkSensitive(String maskedLower) {
        try {
            for (String t : sensitiveTokens()) {
                String tok = t.trim();
                if (!tok.isEmpty() && maskedLower.indexOf(tok) >= 0) {
                    return new Result(false,
                            "Akses kolom/tabel sensitif tidak diizinkan dari endpoint SQL klien (pola: '" + tok + "').");
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SqlSecurityGuard.java:134");
            // fail-open: guard tidak boleh menjadi penyebab kegagalan
        }
        return OK;
    }

    /**
     * Pemeriksaan untuk {@code action=sql}: HARUS read-only (SELECT/WITH/EXPLAIN), satu statement.
     *
     * @return {@link Result#allowed}=true bila boleh dilanjutkan (selalu true saat mode=off / mode=log).
     */
    public static Result checkReadSql(String sql) {
        String mode = mode();
        if (MODE_OFF.equals(mode)) {
            return OK;
        }
        Result v = evaluateRead(sql);
        if (v.allowed) {
            return OK;
        }
        log("READ", mode, v.reason, sql);
        return MODE_ENFORCE.equals(mode) ? v : OK;
    }

    /**
     * Pemeriksaan untuk {@code action=update_data}: DML (UPDATE/INSERT/DELETE) boleh — termasuk
     * banyak statement bertumpuk (fitur "Hitung Ulang" mengandalkannya) — TAPI DDL/berbahaya ditolak.
     */
    public static Result checkWriteSql(String sql) {
        String mode = mode();
        if (MODE_OFF.equals(mode)) {
            return OK;
        }
        Result v = evaluateWrite(sql);
        if (v.allowed) {
            return OK;
        }
        log("WRITE", mode, v.reason, sql);
        return MODE_ENFORCE.equals(mode) ? v : OK;
    }

    private static Result evaluateRead(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new Result(false, "SQL kosong.");
        }
        String core = mask(sql).trim().toLowerCase();
        while (core.endsWith(";")) {
            core = core.substring(0, core.length() - 1).trim();
        }
        if (core.indexOf(';') >= 0) {
            return new Result(false, "Statement bertumpuk tidak diizinkan pada action=sql.");
        }
        if (!(core.startsWith("select") || core.startsWith("with") || core.startsWith("explain")
                || core.startsWith("("))) {
            return new Result(false, "Hanya SELECT yang diizinkan pada action=sql (read-only).");
        }
        if (WRITE_OR_DDL_READ.matcher(core).find()) {
            return new Result(false, "Kata kunci tulis/DDL tidak diizinkan pada action=sql (read-only).");
        }
        if (SYS_OBJECTS.matcher(core).find()) {
            return new Result(false, "Akses objek sistem database tidak diizinkan.");
        }
        return checkSensitive(core);
    }

    private static Result evaluateWrite(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new Result(false, "SQL kosong.");
        }
        String masked = mask(sql).toLowerCase();
        if (DDL_DANGEROUS_WRITE.matcher(masked).find()) {
            return new Result(false,
                    "Perintah DDL/berbahaya (drop/alter/truncate/create/grant/revoke/...) tidak diizinkan.");
        }
        if (SYS_OBJECTS.matcher(masked).find()) {
            return new Result(false, "Akses objek sistem database tidak diizinkan.");
        }
        return checkSensitive(masked);
    }

    /** Mengganti isi string literal dengan '' agar token di dalam '...' tidak ikut dipindai. */
    private static String mask(String sql) {
        try {
            return STRING_LITERAL.matcher(sql).replaceAll("''");
        } catch (Exception e) {
            return sql;
        }
    }

    private static void log(String jenis, String mode, String reason, String sql) {
        try {
            String potong = sql == null ? "" : (sql.length() > 400 ? sql.substring(0, 400) + "..." : sql);
            System.out.println("[SqlSecurityGuard][mode=" + mode + "][" + jenis + "] DITOLAK: " + reason
                    + " | SQL: " + potong);
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/SqlSecurityGuard.java:228");
        }
    }
}
