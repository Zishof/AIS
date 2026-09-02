package ais.common.newui.akademik;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.action.master.TampilanELearningAction;
import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.PertemuanPunyaGrupPertemuan;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;

/**
 * Kontrak native hub "Konsultasi Mahasiswa".
 *
 * <p>Layar ZK lama bukan CRUD konsultasi tunggal. Ia menggabungkan enam
 * konteks e-learning: KRS/akademik, KKN, PKL, bimbingan tugas akhir, sidang
 * sebagai penguji, dan konsultasi umum. Kontrak ini mempertahankan pemisahan
 * itu dan mengembalikan referensi kelompok; agenda serta detail kelompok
 * dibuka oleh halaman Aktifitas Perkuliahan Flutter yang sudah menangani
 * kontrak e-learning lengkap.</p>
 */
public final class NewUiKonsultasiMahasiswaController {

    private static final String MODULE = "root";
    static final String AKADEMIK = "akademik";
    static final String KKN = "kkn";
    static final String PKL = "pkl";
    static final String BIMBINGAN = "bimbingan";
    static final String PENGUJI = "penguji";
    static final String LAIN = "lain";

    private NewUiKonsultasiMahasiswaController() { }

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
                    fail(json, "METHOD_NOT_ALLOWED", "Gunakan HTTP POST untuk menyinkronkan data.");
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
            else daftar(json, request, mahasiswa, "update".equals(action));
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Data konsultasi gagal dimuat.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiKonsultasiMahasiswaController"); }
            catch (Exception ignored) { }
        }
        write(response, json);
    }

    static boolean aksiDikenal(String action) {
        return "meta".equals(action) || "list".equals(action) || "update".equals(action);
    }

    static boolean mengubah(String action) { return "update".equals(action); }

    static boolean jenisDikenal(String jenis) {
        return AKADEMIK.equals(jenis) || KKN.equals(jenis) || PKL.equals(jenis)
                || BIMBINGAN.equals(jenis) || PENGUJI.equals(jenis) || LAIN.equals(jenis);
    }

    private static void meta(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa)
            throws JSONException {
        JSONArray tabs = new JSONArray();
        tabs.put(tab(AKADEMIK, "Konsultasi Akademik"));
        tabs.put(tab(KKN, "KKN"));
        tabs.put(tab(PKL, "PKL"));
        tabs.put(tab(BIMBINGAN, "Bimbingan Tugas Akhir"));
        tabs.put(tab(PENGUJI, "Sidang / Penguji"));
        tabs.put(tab(LAIN, "Konsultasi Umum"));
        int[] bawaan = semesterBawaan(mahasiswa);
        j.put("judul", "Konsultasi Mahasiswa")
                .put("mahasiswa", new JSONObject().put("id", mahasiswa.getId())
                        .put("nim", nz(mahasiswa.getNim())).put("nama", nz(mahasiswa.getNama())))
                .put("kelompok", tabs)
                .put("jenisBawaan", AKADEMIK)
                .put("semesterMulaiBawaan", bawaan[0])
                .put("semesterSampaiBawaan", bawaan[1])
                .put("targetPage", "aktifitasperkuliahan")
                .put("csrfHeader", NewUiCsrfUtil.HEADER)
                .put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));
    }

    private static JSONObject tab(String id, String label) throws JSONException {
        return new JSONObject().put("id", id).put("label", label);
    }

    private static void daftar(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa,
            boolean keDatabase) throws Exception {
        String jenis = text(request.getParameter("jenis"), AKADEMIK).toLowerCase();
        if (!jenisDikenal(jenis)) throw new IllegalArgumentException("Jenis konsultasi tidak dikenal.");
        if (keDatabase && !AKADEMIK.equals(jenis)) {
            throw new IllegalArgumentException("Sinkronisasi hanya berlaku untuk konsultasi akademik.");
        }
        JSONArray rows = AKADEMIK.equals(jenis)
                ? akademik(request, mahasiswa, keDatabase)
                : kelompokKhusus(mahasiswa, jenis);
        j.put("jenis", jenis).put("baris", rows).put("total", rows.length())
                .put("ditulisKeBasisData", keDatabase)
                .put("targetPage", "aktifitasperkuliahan");
    }

    private static JSONArray akademik(HttpServletRequest request, Mahasiswa mahasiswa,
            boolean keDatabase) throws Exception {
        int batas = NewUiKrsPaketController.batasSemester(mahasiswa.getSemesterLulus());
        int[] bawaan = semesterBawaan(mahasiswa);
        int mulai = integer(request.getParameter("mulai"), bawaan[0]);
        int sampai = integer(request.getParameter("sampai"), bawaan[1]);
        if (!NewUiKrsNonPaketController.rentangValid(mulai, sampai, batas)) {
            throw new IllegalArgumentException("Rentang semester harus antara 1 dan " + batas + ".");
        }
        Integer semesterPendek = semesterPendek(request);
        List<String[]> periods = Common.generateSemestersForGrid(mahasiswa,
                Integer.valueOf(mulai), Integer.valueOf(sampai), semesterPendek);
        JSONArray rows = new JSONArray();
        for (String[] period : periods) {
            int semester = parseSemester(period);
            if (semester <= 0) continue;
            int tahapan = parseTahapan(period);
            KrsMahasiswa krs = Common.singkronkanKrsMahasiswa(mahasiswa,
                    Integer.valueOf(semester), Integer.valueOf(tahapan), semesterPendek, keDatabase);
            if (krs == null) continue;
            rows.put(new JSONObject().put("id", krs.getId())
                    .put("tahunAkademik", nz(krs.getTahunAkademik()))
                    .put("semester", krs.getSemester())
                    .put("tahapan", krs.getTahapan() == null ? 0 : krs.getTahapan())
                    .put("kelas", nz(krs.getKelas()))
                    .put("keterangan", nz(krs.getKeterangan())));
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private static JSONArray kelompokKhusus(Mahasiswa mahasiswa, String jenis)
            throws JSONException {
        int mode = mode(jenis);
        Object[] result = mahasiswa.ambilPerkuliahanDanParalel(null, null, null, "", "",
                false, null, true, false, false, mode, 0, 100, true);
        List<Object> data = result == null || result.length == 0 || !(result[0] instanceof List)
                ? java.util.Collections.<Object>emptyList() : (List<Object>) result[0];
        JSONArray rows = new JSONArray();
        for (Object item : data) {
            JSONObject row = referensi(item, jenis);
            if (row != null) rows.put(row);
        }
        return rows;
    }

    static int mode(String jenis) {
        if (KKN.equals(jenis)) return TampilanELearningAction.KKN;
        if (PKL.equals(jenis)) return TampilanELearningAction.PKL;
        if (BIMBINGAN.equals(jenis)) return TampilanELearningAction.BIMBINGAN;
        if (PENGUJI.equals(jenis)) return TampilanELearningAction.SKRIPSI;
        if (LAIN.equals(jenis)) return TampilanELearningAction.KONSULTASI;
        throw new IllegalArgumentException("Jenis konsultasi tidak memiliki mode kelompok.");
    }

    private static JSONObject referensi(Object item, String jenis) throws JSONException {
        if (item instanceof KelompokKkn) {
            KelompokKkn v = (KelompokKkn) item;
            return ref(v.getId(), v.getNama_kelompok(), jenis);
        }
        if (item instanceof KelompokPkl) {
            KelompokPkl v = (KelompokPkl) item;
            return ref(v.getId(), v.getNama_kelompok(), jenis);
        }
        if (item instanceof MahasiswaRequestTugasAkhir) {
            MahasiswaRequestTugasAkhir v = (MahasiswaRequestTugasAkhir) item;
            return ref(v.getId(), v.getJudul(), jenis);
        }
        if (item instanceof Skripsi) {
            Skripsi v = (Skripsi) item;
            return ref(v.getId(), v.getJudul(), jenis);
        }
        if (item instanceof PertemuanPunyaGrupPertemuan) {
            PertemuanPunyaGrupPertemuan v = (PertemuanPunyaGrupPertemuan) item;
            String nama = v.getGrupPertemuan() == null ? "" : v.getGrupPertemuan().getNama();
            return ref(v.getId(), nama, jenis);
        }
        return null;
    }

    private static JSONObject ref(Object id, String label, String jenis) throws JSONException {
        return new JSONObject().put("id", id).put("label", nz(label)).put("jenis", jenis);
    }

    private static int[] semesterBawaan(Mahasiswa mahasiswa) {
        int berjalan = NewUiKuesionerMahasiswaController.semesterSekarang(mahasiswa);
        int mulai = Common.getKonfigurasi("default_pemilihan_semester_mulai",
                String.valueOf(berjalan)).niliaInteger();
        int sampai = Common.getKonfigurasi("default_pemilihan_semester_sampai",
                String.valueOf(berjalan)).niliaInteger();
        return NewUiKrsPilihanController.semesterBawaan(mulai, sampai,
                berjalan, mahasiswa.getSemesterLulus());
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
