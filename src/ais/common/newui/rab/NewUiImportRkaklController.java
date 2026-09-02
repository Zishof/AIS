package ais.common.newui.rab;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.linuxense.javadbf.DBFReader;

import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;

/**
 * Preflight baca-saja untuk berkas RKAKL pada direktori staging tetap.
 *
 * <p>Controller tidak menerima path dari request dan sengaja tidak memanggil
 * helper lama yang menjatuhkan seluruh schema {@code rab_import}. Eksekusi baru
 * boleh dibuka setelah tersedia job store persisten, exclusive lock, audit,
 * backup, pertukaran schema atomik, dan rollback.</p>
 */
public final class NewUiImportRkaklController {

    static final String STAGING_KEY = "direktori_staging_import_rkakl";
    private static final String MODULE = "rab";

    private NewUiImportRkaklController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        try {
            String action = text(request.getParameter("action"), "meta");
            if (!aksiDikenal(action)) {
                response.setStatus(405);
                write(response, gagal("ACTION_NOT_ALLOWED",
                        "Hanya metadata dan preflight baca-saja yang tersedia."));
                return;
            }
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                write(response, gagal("ACTION_FORBIDDEN", "Hak akses tidak tersedia."));
                return;
            }
            JSONObject data = new JSONObject();
            if ("meta".equals(action)) meta(data);
            else daftar(data, request);
            write(response, sukses(data));
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            write(response, gagal("VALIDATION_FAILED", e.getMessage()));
        } catch (Exception e) {
            response.setStatus(500);
            try { ais.common.ErrorAuditUtil.record(e, "NewUiImportRkaklController"); }
            catch (Exception ignored) { }
            write(response, gagal("INTERNAL_ERROR", "Preflight RKAKL gagal dimuat."));
        }
    }

    static boolean aksiDikenal(String action) {
        return "meta".equals(action) || "list".equals(action);
    }

    private static void meta(JSONObject j) throws JSONException {
        j.put("title", "Import Data RKAKL - Preflight Aman")
                .put("displayName", "Berkas RKAKL")
                .put("identifierProperty", "id")
                .put("pageSize", 25)
                .put("canCreate", false).put("canUpdate", false)
                .put("canDelete", false).put("rowAudit", false)
                .put("fields", fields())
                .put("importExecutionEnabled", false)
                .put("stagingConfigurationKey", STAGING_KEY)
                .put("safetyNotice", "Eksekusi impor belum dibuka. Alur lama menghapus schema "
                        + "rab_import, menerima path server bebas, dan tidak mempunyai job store "
                        + "persisten, exclusive lock, audit hasil, maupun rollback atomik.")
                .put("requiredControls", new JSONArray()
                        .put("Direktori staging tetap yang dikelola administrator")
                        .put("Penyimpanan status pekerjaan yang dapat ditanya ulang")
                        .put("Exclusive lock untuk mencegah dua impor bersamaan")
                        .put("Backup dan pertukaran schema secara atomik")
                        .put("Audit per berkas serta prosedur rollback"));
    }

    private static JSONArray fields() throws JSONException {
        return new JSONArray()
                .put(field("namaBerkas", "Berkas", "java.lang.String"))
                .put(field("ukuran", "Ukuran", "java.lang.String"))
                .put(field("jumlahKolom", "Kolom", "java.lang.Integer"))
                .put(field("jumlahBaris", "Baris", "java.lang.Integer"))
                .put(field("status", "Status", "java.lang.String"))
                .put(field("keterangan", "Keterangan", "java.lang.String"));
    }

    private static JSONObject field(String property, String label, String javaType)
            throws JSONException {
        return new JSONObject().put("property", property).put("label", label)
                .put("javaType", javaType).put("tableVisible", true)
                .put("readable", true).put("createable", false)
                .put("updateable", false);
    }

    private static void daftar(JSONObject j, HttpServletRequest request) throws Exception {
        String configured = Common.getKonfigurasi(STAGING_KEY, "").getNilai();
        String path = configured == null ? "" : configured.trim();
        List<JSONObject> all = new ArrayList<JSONObject>();
        if (path.length() == 0) {
            all.add(statusRow("STAGING_BELUM_DIATUR",
                    "Konfigurasi " + STAGING_KEY + " belum diisi."));
        } else {
            File staging = new File(path);
            if (!staging.isDirectory()) {
                all.add(statusRow("STAGING_TIDAK_TERSEDIA",
                        "Direktori staging yang dikonfigurasi tidak tersedia."));
            } else {
                scan(staging, all, batasMb(), batasBerkas());
            }
        }

        String query = text(request.getParameter("q"), "").toLowerCase(Locale.ENGLISH);
        List<JSONObject> filtered = new ArrayList<JSONObject>();
        for (JSONObject row : all) {
            if (query.length() == 0 || cocok(row, query)) filtered.add(row);
        }
        int pageSize = clamp(integer(request.getParameter("pageSize"), 25), 1, 100);
        int page = Math.max(1, integer(request.getParameter("page"), 1));
        int start = Math.min(filtered.size(), (page - 1) * pageSize);
        int end = Math.min(filtered.size(), start + pageSize);
        JSONArray rows = new JSONArray();
        for (int i = start; i < end; i++) rows.put(filtered.get(i));
        j.put("rows", rows).put("total", filtered.size())
                .put("page", page).put("pageSize", pageSize)
                .put("pageCount", filtered.size() == 0 ? 0
                        : (filtered.size() + pageSize - 1) / pageSize)
                .put("executionBlocked", true);
    }

    private static void scan(File staging, List<JSONObject> rows, long maxMb, int maxFiles)
            throws Exception {
        File canonicalStaging = staging.getCanonicalFile();
        File[] files = canonicalStaging.listFiles();
        if (files == null) {
            rows.add(statusRow("STAGING_TIDAK_DAPAT_DIBACA",
                    "Direktori staging tidak dapat dibaca oleh service."));
            return;
        }
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File left, File right) {
                return left.getName().compareToIgnoreCase(right.getName());
            }
        });
        int candidates = 0;
        for (int i = 0; i < files.length && candidates < maxFiles; i++) {
            File file = files[i];
            if (!file.isFile()
                    || !file.getName().toLowerCase(Locale.ENGLISH).endsWith(".keu")) continue;
            candidates++;
            rows.add(periksa(canonicalStaging, file, maxMb, candidates));
        }
        if (candidates == 0) {
            rows.add(statusRow("TIDAK_ADA_BERKAS",
                    "Belum ada berkas .KEU pada direktori staging."));
        } else if (candidates >= maxFiles) {
            rows.add(statusRow("BATAS_DAFTAR_TERCAPAI",
                    "Daftar dibatasi " + maxFiles + " berkas; rapikan staging sebelum impor."));
        }
    }

    private static JSONObject periksa(File staging, File file, long maxMb, int sequence)
            throws JSONException {
        JSONObject row = new JSONObject().put("id", "file-" + sequence)
                .put("namaBerkas", file.getName()).put("ukuran", ukuran(file.length()))
                .put("jumlahKolom", JSONObject.NULL).put("jumlahBaris", JSONObject.NULL);
        try {
            String root = staging.getCanonicalPath() + File.separator;
            if (!file.getCanonicalPath().startsWith(root)) {
                return ditolak(row, "Berkas keluar dari direktori staging.");
            }
            String reason = alasanNama(file.getName());
            if (reason.length() > 0) return ditolak(row, reason);
            if (file.length() <= 0) return ditolak(row, "Berkas kosong.");
            if (file.length() > maxMb * 1024L * 1024L) {
                return ditolak(row, "Ukuran melebihi batas preflight " + maxMb + " MB.");
            }
            FileInputStream input = null;
            try {
                input = new FileInputStream(file);
                DBFReader reader = new DBFReader(input);
                int fields = reader.getFieldCount();
                if (fields < 1) return ditolak(row, "Header DBF tidak mempunyai kolom.");
                row.put("jumlahKolom", fields).put("jumlahBaris", reader.getRecordCount());
            } finally {
                if (input != null) try { input.close(); } catch (Exception ignored) { }
            }
            return row.put("status", "SIAP DITINJAU")
                    .put("keterangan", "Header DBF valid; eksekusi impor tetap diblokir sampai "
                            + "kontrol transaksi, job, audit, dan rollback tersedia.");
        } catch (Exception e) {
            return ditolak(row, "Header DBF tidak valid atau tidak dapat dibaca.");
        }
    }

    static String alasanNama(String name) {
        if (name == null || !name.toLowerCase(Locale.ENGLISH).endsWith(".keu")) {
            return "Ekstensi berkas harus .KEU.";
        }
        String lower = name.toLowerCase(Locale.ENGLISH);
        if (lower.contains("log") || lower.contains("t_cek")) {
            return "Berkas log/t_cek dilewati sesuai aturan RKAKL lama.";
        }
        String base = name.substring(0, name.length() - 4);
        if (!base.matches("[A-Za-z0-9_]+")) {
            return "Nama tabel turunan hanya boleh berisi huruf, angka, dan garis bawah.";
        }
        return "";
    }

    private static JSONObject ditolak(JSONObject row, String reason) throws JSONException {
        return row.put("status", "DITOLAK").put("keterangan", reason);
    }

    private static JSONObject statusRow(String status, String description) throws JSONException {
        return new JSONObject().put("id", "status-" + status)
                .put("namaBerkas", "-").put("ukuran", "-")
                .put("jumlahKolom", JSONObject.NULL).put("jumlahBaris", JSONObject.NULL)
                .put("status", status).put("keterangan", description);
    }

    private static boolean cocok(JSONObject row, String query) {
        return (row.optString("namaBerkas") + " " + row.optString("status") + " "
                + row.optString("keterangan")).toLowerCase(Locale.ENGLISH).contains(query);
    }

    private static long batasMb() {
        try {
            return clampLong(Long.parseLong(Common.getKonfigurasi(
                    "batas_ukuran_preflight_rkakl_mb", "50").getNilai()), 1L, 200L);
        } catch (Exception e) { return 50L; }
    }

    private static int batasBerkas() {
        try {
            return clamp(Integer.parseInt(Common.getKonfigurasi(
                    "batas_berkas_preflight_rkakl", "200").getNilai()), 1, 500);
        } catch (Exception e) { return 200; }
    }

    private static String ukuran(long bytes) {
        if (bytes >= 1024L * 1024L) return (bytes / (1024L * 1024L)) + " MB";
        if (bytes >= 1024L) return (bytes / 1024L) + " KB";
        return bytes + " B";
    }

    static JSONObject sukses(JSONObject data) throws JSONException {
        return new JSONObject().put("success", true).put("data", data);
    }

    private static JSONObject gagal(String code, String message) throws JSONException {
        return new JSONObject().put("success", false).put("code", code)
                .put("message", message == null ? "" : message);
    }

    private static int integer(String value, int fallback) {
        try { return value == null ? fallback : Integer.parseInt(value.trim()); }
        catch (Exception e) { return fallback; }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }

    private static void write(HttpServletResponse response, JSONObject json) throws Exception {
        response.getWriter().print(json.toString());
    }
}
