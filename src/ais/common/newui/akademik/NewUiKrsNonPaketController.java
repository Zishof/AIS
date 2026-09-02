package ais.common.newui.akademik;

import java.io.Serializable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Komentar;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;

/**
 * Kontrak native daftar KRS non-paket mahasiswa.
 *
 * <p>Layar lama membedakan muat biasa ({@code load(false)}) dan muat yang
 * menulis sinkronisasi ({@code load(true)}). Kontrak ini mempertahankan batas
 * tersebut: {@code list} tidak menulis, sedangkan {@code update} menulis dan
 * karenanya wajib POST, CSRF, serta izin update.</p>
 *
 * <p>Pengambilan mata kuliah baru tidak diduplikasi di controller ini. Klien
 * Flutter mengarahkan menu ke halaman Rencana Studi yang sudah memiliki alur
 * pengambilan lengkap; API ini melayani daftar, sinkronisasi, dan penghapusan
 * yang kontraknya dapat disalin tepat dari layar ZK.</p>
 */
public final class NewUiKrsNonPaketController {

    private static final String MODULE = "root";

    private NewUiKrsNonPaketController() { }

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
            if (mahasiswa == null || mahasiswa.getId() == null)
                throw new SecurityException("Layar ini hanya untuk akun mahasiswa.");

            if ("meta".equals(action)) meta(json, request, mahasiswa);
            else if ("list".equals(action)) daftar(json, request, mahasiswa, false);
            else if ("update".equals(action)) daftar(json, request, mahasiswa, true);
            else hapus(json, request, mahasiswa);
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500); fail(json, "INTERNAL_ERROR", "Permintaan KRS gagal diproses.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiKrsNonPaketController"); }
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

    static boolean rentangValid(int mulai, int sampai, int batas) {
        return mulai >= 1 && sampai >= mulai && sampai <= batas;
    }

    static String status(boolean disetujui, boolean belum) {
        if (disetujui && belum) return "Sebagian sudah disetujui";
        if (disetujui) return "Sudah disetujui semua";
        if (belum) return "Belum disetujui semua";
        return "Belum ada mata kuliah";
    }

    static boolean bolehHapus(Detailperkuliahan detail) {
        return detail != null && (detail.getPersetujuan() == null
                || Detailperkuliahan.BELUM_DISETUJUI.equals(detail.getPersetujuan()));
    }

    private static void meta(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa)
            throws JSONException {
        int batas = NewUiKrsPaketController.batasSemester(mahasiswa.getSemesterLulus());
        int bawaan = NewUiKrsPaketController.semesterBawaan(mahasiswa.getSemesterLulus(),
                NewUiKuesionerMahasiswaController.semesterSekarang(mahasiswa));
        JSONArray pilihan = new JSONArray();
        for (int i = 1; i <= batas; i++) pilihan.put(i);
        j.put("judul", "Isi KRS (Non Paket)")
                .put("mahasiswa", new JSONObject().put("id", mahasiswa.getId())
                        .put("nim", nz(mahasiswa.getNim())).put("nama", nz(mahasiswa.getNama())))
                .put("pilihanSemester", pilihan)
                .put("semesterMulaiBawaan", bawaan)
                .put("semesterSampaiBawaan", bawaan)
                .put("csrfHeader", NewUiCsrfUtil.HEADER)
                .put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)))
                .put("ambilMatakuliahTersedia", false)
                .put("ambilMatakuliahAlasan", "Gunakan halaman Rencana Studi untuk mengambil mata kuliah; "
                        + "halaman tersebut mempertahankan seluruh pemeriksaan pembayaran dan status akademik.")
                .put("catatanSinkronisasi", "Menampilkan daftar tidak menulis data. Tombol sinkronkan "
                        + "menyimpan hasil sinkronisasi KRS ke basis data.");
    }

    private static void daftar(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa,
            boolean keDatabase) throws Exception {
        int batas = NewUiKrsPaketController.batasSemester(mahasiswa.getSemesterLulus());
        int bawaan = NewUiKrsPaketController.semesterBawaan(mahasiswa.getSemesterLulus(),
                NewUiKuesionerMahasiswaController.semesterSekarang(mahasiswa));
        int mulai = integer(request.getParameter("mulai"), bawaan);
        int sampai = integer(request.getParameter("sampai"), bawaan);
        if (!rentangValid(mulai, sampai, batas))
            throw new IllegalArgumentException("Rentang semester harus antara 1 dan " + batas
                    + ", dan semester awal tidak boleh melewati semester akhir.");
        Integer semesterPendek = semesterPendek(request);
        List<String[]> periods = Common.generateSemestersForGrid(mahasiswa,
                Integer.valueOf(mulai), Integer.valueOf(sampai), semesterPendek);
        JSONArray groups = new JSONArray();
        for (String[] period : periods) {
            int semester = parseSemester(period);
            if (semester <= 0) continue;
            int tahapan = parseTahapan(period);
            String tahunAjaran = period == null || period.length == 0 ? "" : nz(period[0]);
            groups.put(grup(mahasiswa, tahunAjaran, semester, tahapan,
                    semesterPendek, keDatabase));
        }
        j.put("kelompok", groups).put("totalKelompok", groups.length())
                .put("mulai", mulai).put("sampai", sampai)
                .put("ditulisKeBasisData", keDatabase);
    }

    static JSONObject grup(Mahasiswa mahasiswa, String tahunAjaran, int semester,
            int tahapan, Integer semesterPendek, boolean keDatabase) throws Exception {
        // KrsAction/KrsNonPaketAction meneruskan nilai tahapan hasil parsing
        // apa adanya (termasuk 0 dan -1). Mengubah 0 menjadi null dapat memilih
        // kelompok KRS yang berbeda pada instalasi yang mengaktifkan tahapan.
        Integer tahap = Integer.valueOf(tahapan);
        KrsMahasiswa krs = Common.singkronkanKrsMahasiswa(mahasiswa, Integer.valueOf(semester),
                tahap, semesterPendek, keDatabase, false);
        boolean sudahBayar = Common.checkStatusPembayaranMahasiswa(
                Integer.valueOf(semester), tahap, mahasiswa, false, false);
        List<Long> ids = Common.getDetailperkuliahans(mahasiswa, Integer.valueOf(semester),
                tahap, null, semesterPendek, false, false, false,
                Boolean.valueOf(keDatabase));
        JSONArray rows = new JSONArray();
        int totalSks = 0;
        boolean approved = false, pending = false;
        for (Long id : ids) {
            Detailperkuliahan detail = detail(id);
            if (detail == null) continue;
            JSONObject row = NewUiKrsPaketController.barisDetail(mahasiswa, id);
            if (row == null) continue;
            totalSks += row.optInt("sksAngka", 0);
            row.remove("sksAngka");
            row.put("bolehHapus", bolehHapus(detail));
            if (Detailperkuliahan.DISETUJUI.equals(detail.getPersetujuan())) approved = true;
            else pending = true;
            if (!sudahBayar) {
                row.remove("dosen"); row.remove("jadwal");
                row.remove("persetujuan");
            }
            rows.put(row);
        }
        JSONObject result = new JSONObject().put("tahunAkademik", tahunAjaran)
                .put("semester", semester).put("tahapan", tahapan)
                .put("sudahBayar", sudahBayar).put("baris", rows)
                .put("total", rows.length()).put("totalSks", totalSks)
                .put("statusPersetujuan", status(approved, pending));
        if (krs != null) {
            result.put("krsId", krs.getId()).put("kelas", nz(krs.getKelas()))
                    .put("dosenPa", krs.getDosenPa() == null ? "" : nz(krs.getDosenPa().getNama()))
                    .put("keterangan", nz(krs.getKeterangan()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static void hapus(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa)
            throws JSONException {
        Long id = longValue(request.getParameter("id"));
        if (id == null) throw new IllegalArgumentException("Baris yang dihapus harus disebutkan.");
        Session session = HibernateUtil.currentSession();
        Detailperkuliahan detail = (Detailperkuliahan) session.get(Detailperkuliahan.class, id);
        if (detail == null) throw new IllegalArgumentException("Data tidak ditemukan.");
        if (detail.getMahasiswa() == null || detail.getMahasiswa().getId() == null
                || !mahasiswa.getId().equals(detail.getMahasiswa().getId()))
            throw new SecurityException("Data ini bukan milik Anda.");
        if (!bolehHapus(detail))
            throw new IllegalArgumentException("Mata kuliah yang sudah disetujui tidak dapat dihapus.");
        List<Komentar> comments = session.createCriteria(Komentar.class)
                .add(Restrictions.eq("detailperkuliahan", detail.getId())).list();
        for (Komentar comment : comments) Common.refreshDelete(comment);
        // KRS Non Paket lama memakai refreshDelete, berbeda dari KRS Paket.
        Common.refreshDelete(detail);
        j.put("dihapus", id);
    }

    private static Detailperkuliahan detail(Long id) {
        try {
            return (Detailperkuliahan) ConstantValues.ambil(
                    Detailperkuliahan.class.getName(), (Serializable) id);
        } catch (Exception e) { return null; }
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

    private static Long longValue(String value) {
        try { return value == null || value.trim().length() == 0 ? null : Long.valueOf(value.trim()); }
        catch (Exception e) { return null; }
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
