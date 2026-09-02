package ais.common.newui.akademik;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;

/**
 * Kontrak native untuk menu mahasiswa "Isi KRS (Pilihan)".
 *
 * <p>Menu ini memakai alur {@code KrsAction/KrsHelper}, bukan KRS paket atau
 * konversi. Muat biasa tidak menulis hasil sinkronisasi; aksi {@code update}
 * meneruskan {@code keDatabase=true}. Penghapusan juga memakai aturan layar
 * lama: hanya detail yang belum disetujui yang dapat dihapus.</p>
 *
 * <p>Semester bawaan sengaja mengikuti aturan lama. Dua konfigurasi hanya
 * dipakai bila nilainya berbeda; bila sama, layar memilih semester berjalan.
 * Nilai tersebut selalu dibatasi oleh semester lulus mahasiswa.</p>
 */
public final class NewUiKrsPilihanController {

    private static final String MODULE = "root";

    private NewUiKrsPilihanController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        JSONObject json = new JSONObject();
        try {
            String action = text(request.getParameter("action"), "meta");
            if (!aksiDikenal(action)) {
                response.setStatus(405);
                fail(json, "ACTION_NOT_ALLOWED", "Aksi tidak dikenal pada layar ini.");
                write(response, json); return;
            }
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json); return;
            }
            if (mengubah(action)) {
                if (!"POST".equalsIgnoreCase(request.getMethod())) {
                    response.setStatus(405);
                    fail(json, "METHOD_NOT_ALLOWED", "Gunakan HTTP POST untuk perubahan data.");
                    write(response, json); return;
                }
                if (!NewUiCsrfUtil.isValid(request)) {
                    response.setStatus(403);
                    fail(json, "CSRF_INVALID", "Token keamanan tidak valid. Muat ulang halaman.");
                    write(response, json); return;
                }
            }

            Tbmuser user = Common.getCurrentUser(request);
            Mahasiswa mahasiswa = user == null ? null : user.getMahasiswa();
            if (mahasiswa == null || mahasiswa.getId() == null) {
                throw new SecurityException("Layar ini hanya untuk akun mahasiswa.");
            }

            if ("meta".equals(action)) meta(json, request, mahasiswa);
            else if ("list".equals(action)) daftar(json, request, mahasiswa, false);
            else if ("update".equals(action)) daftar(json, request, mahasiswa, true);
            else NewUiKrsNonPaketController.hapus(json, request, mahasiswa);
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Permintaan KRS gagal diproses.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiKrsPilihanController"); }
            catch (Exception ignored) { }
        }
        write(response, json);
    }

    static boolean aksiDikenal(String action) {
        return "meta".equals(action) || "list".equals(action)
                || "update".equals(action) || "delete".equals(action);
    }

    static boolean mengubah(String action) {
        return "update".equals(action) || "delete".equals(action);
    }

    static int[] semesterBawaan(int konfigurasiMulai, int konfigurasiSampai,
            int semesterBerjalan, Integer semesterLulus) {
        int batas = NewUiKrsPaketController.batasSemester(semesterLulus);
        int berjalan = batasi(semesterBerjalan, batas);
        int mulai = batasi(konfigurasiMulai, batas);
        int sampai = batasi(konfigurasiSampai, batas);
        if (mulai == sampai) return new int[] { berjalan, berjalan };
        return mulai <= sampai ? new int[] { mulai, sampai }
                : new int[] { sampai, mulai };
    }

    private static int batasi(int value, int batas) {
        if (value < 1) return 1;
        return value > batas ? batas : value;
    }

    private static int[] semesterBawaan(Mahasiswa mahasiswa) {
        int berjalan = NewUiKuesionerMahasiswaController.semesterSekarang(mahasiswa);
        int konfigurasiMulai = Common.getKonfigurasi(
                "default_pemilihan_semester_mulai", String.valueOf(berjalan)).niliaInteger();
        int konfigurasiSampai = Common.getKonfigurasi(
                "default_pemilihan_semester_sampai", String.valueOf(berjalan)).niliaInteger();
        return semesterBawaan(konfigurasiMulai, konfigurasiSampai,
                berjalan, mahasiswa.getSemesterLulus());
    }

    private static void meta(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa)
            throws JSONException {
        int batas = NewUiKrsPaketController.batasSemester(mahasiswa.getSemesterLulus());
        int[] bawaan = semesterBawaan(mahasiswa);
        JSONArray pilihan = new JSONArray();
        for (int i = 1; i <= batas; i++) pilihan.put(i);
        j.put("judul", "Isi KRS (Pilihan)")
                .put("mahasiswa", new JSONObject().put("id", mahasiswa.getId())
                        .put("nim", nz(mahasiswa.getNim())).put("nama", nz(mahasiswa.getNama())))
                .put("pilihanSemester", pilihan)
                .put("semesterMulaiBawaan", bawaan[0])
                .put("semesterSampaiBawaan", bawaan[1])
                .put("csrfHeader", NewUiCsrfUtil.HEADER)
                .put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)))
                .put("alurPengambilan", "rencanastudi")
                .put("catatanSinkronisasi", "Menampilkan daftar tidak menulis data. Tombol sinkronkan "
                        + "menyimpan hasil sinkronisasi KRS ke basis data.");
    }

    private static void daftar(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa,
            boolean keDatabase) throws Exception {
        int batas = NewUiKrsPaketController.batasSemester(mahasiswa.getSemesterLulus());
        int[] bawaan = semesterBawaan(mahasiswa);
        int mulai = integer(request.getParameter("mulai"), bawaan[0]);
        int sampai = integer(request.getParameter("sampai"), bawaan[1]);
        if (!NewUiKrsNonPaketController.rentangValid(mulai, sampai, batas)) {
            throw new IllegalArgumentException("Rentang semester harus antara 1 dan " + batas
                    + ", dan semester awal tidak boleh melewati semester akhir.");
        }

        Integer semesterPendek = semesterPendek(request);
        List<String[]> periods = Common.generateSemestersForGrid(mahasiswa,
                Integer.valueOf(mulai), Integer.valueOf(sampai), semesterPendek);
        JSONArray groups = new JSONArray();
        for (String[] period : periods) {
            int semester = parseSemester(period);
            if (semester <= 0) continue;
            int tahapan = parseTahapan(period);
            String tahunAjaran = period == null || period.length == 0 ? "" : nz(period[0]);
            groups.put(NewUiKrsNonPaketController.grup(mahasiswa, tahunAjaran,
                    semester, tahapan, semesterPendek, keDatabase));
        }
        j.put("kelompok", groups).put("totalKelompok", groups.length())
                .put("mulai", mulai).put("sampai", sampai)
                .put("ditulisKeBasisData", keDatabase);
    }

    private static int parseSemester(String[] period) {
        try { return Integer.parseInt(period[1].split(",")[0]); }
        catch (Exception e) { return 0; }
    }

    private static int parseTahapan(String[] period) {
        try { return Integer.parseInt(period[3]); }
        catch (Exception e) { return 0; }
    }

    private static Integer semesterPendek(HttpServletRequest request) {
        String value = request.getParameter("semesterPendek");
        return "1".equals(value) || "true".equalsIgnoreCase(value)
                ? Perkuliahan.SEMESTER_PENDEK : null;
    }

    private static int integer(String value, int fallback) {
        try { return value == null || value.trim().length() == 0
                ? fallback : Integer.parseInt(value.trim()); }
        catch (Exception e) { return fallback; }
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }

    private static String nz(String value) { return value == null ? "" : value; }

    private static void fail(JSONObject j, String code, String message) throws JSONException {
        j.put("ok", false).put("code", code).put("message", nz(message));
    }

    private static void write(HttpServletResponse response, JSONObject j) throws Exception {
        response.getWriter().print(j.toString());
    }
}
