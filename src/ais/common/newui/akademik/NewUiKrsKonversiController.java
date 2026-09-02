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
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;

/** Kontrak native daftar KRS konversi/kurikulum mahasiswa. */
public final class NewUiKrsKonversiController {

    private static final String MODULE = "root";

    private NewUiKrsKonversiController() { }

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

            if ("meta".equals(action)) meta(json, request, mahasiswa, pageKey);
            else if ("list".equals(action)) daftar(json, request, mahasiswa, false);
            else if ("update".equals(action)) daftar(json, request, mahasiswa, true);
            else hapus(json, request, mahasiswa);
            json = sukses(json);
        } catch (SecurityException e) {
            response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Permintaan KRS konversi gagal diproses.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiKrsKonversiController"); }
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

    static boolean bolehHapus(Detailperkuliahan detail) {
        if (detail == null || detail.getTotalNilai() == null) return false;
        return detail.getTotalNilai().doubleValue() < 0.01d;
    }

    static String labelSemester(Integer semesterDetail, Integer semesterPerkuliahan) {
        if (semesterDetail == null) return "";
        if (semesterPerkuliahan == null || semesterDetail.equals(semesterPerkuliahan)) {
            return String.valueOf(semesterDetail);
        }
        return semesterDetail + " / " + semesterPerkuliahan
                + (semesterDetail.intValue() > semesterPerkuliahan.intValue()
                        ? " (Mengulang)" : " (Menabung)");
    }

    private static void meta(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa,
            String pageKey) throws JSONException {
        int batas = NewUiKrsPaketController.batasSemester(mahasiswa.getSemesterLulus());
        int[] bawaan = semesterBawaan(mahasiswa);
        JSONArray pilihan = new JSONArray();
        for (int i = 1; i <= batas; i++) pilihan.put(i);
        j.put("title", "Isi KRS Konversi")
                .put("displayName", "KRS Konversi")
                .put("identifierProperty", "id")
                .put("pageSize", 25)
                .put("canCreate", false)
                .put("canUpdate", false)
                .put("canDelete", NewUiRouteGuard.isActionAuthorized(
                        request, MODULE, pageKey, "delete"))
                .put("rowAudit", false)
                .put("fields", fields())
                .put("mahasiswa", new JSONObject().put("id", mahasiswa.getId())
                        .put("nim", nz(mahasiswa.getNim())).put("nama", nz(mahasiswa.getNama())))
                .put("pilihanSemester", pilihan)
                .put("semesterMulaiBawaan", bawaan[0])
                .put("semesterSampaiBawaan", bawaan[1])
                .put("csrfHeader", NewUiCsrfUtil.HEADER)
                .put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)))
                .put("csrf", NewUiCsrfUtil.getToken(request.getSession(true)))
                .put("ambilKonversiTersedia", false)
                .put("ambilKonversiAlasan", "Pemilih konversi mengganti seluruh pilihan belum disetujui "
                        + "dan memerlukan pemeriksaan paket kurikulum serta prasyarat. Gunakan tampilan lama "
                        + "untuk mengambil paket baru; daftar dan penghapusan aman tersedia di sini.")
                .put("catatanSinkronisasi", "Menampilkan daftar tidak menulis data. Sinkronkan wajib POST.");
    }

    private static void daftar(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa,
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
        JSONArray groups = new JSONArray();
        JSONArray rows = new JSONArray();
        for (String[] period : periods) {
            int semester = parseSemester(period);
            if (semester <= 0 || semester == 1000) continue;
            int tahapan = parseTahapan(period);
            String tahunAkademik = period == null || period.length == 0 ? "" : nz(period[0]);
            JSONObject group = grup(mahasiswa, tahunAkademik, semester, tahapan,
                    semesterPendek, keDatabase);
            groups.put(group);
            JSONArray groupRows = group.optJSONArray("baris");
            if (groupRows != null) {
                for (int i = 0; i < groupRows.length(); i++) {
                    JSONObject row = groupRows.getJSONObject(i);
                    row.put("tahunAkademik", tahunAkademik)
                            .put("semesterKelompok", semester)
                            .put("tahapanKelompok", tahapan);
                    rows.put(row);
                }
            }
        }
        j.put("kelompok", groups).put("totalKelompok", groups.length())
                .put("rows", rows).put("total", rows.length())
                .put("page", 1).put("pageSize", Math.max(1, rows.length()))
                .put("mulai", mulai).put("sampai", sampai)
                .put("ditulisKeBasisData", keDatabase);
    }

    private static JSONObject grup(Mahasiswa mahasiswa, String tahunAkademik, int semester,
            int tahapan, Integer semesterPendek, boolean keDatabase) throws Exception {
        KrsMahasiswa krs = Common.singkronkanKrsMahasiswa(mahasiswa,
                Integer.valueOf(semester), Integer.valueOf(tahapan), semesterPendek,
                keDatabase, false);
        // Helper konversi memakai overload ini; tahapan tidak menjadi filter
        // detail, tetapi tetap dipakai pada ringkasan KrsMahasiswa di atas.
        List<Long> ids = Common.getDetailperkuliahans(mahasiswa, Integer.valueOf(semester),
                null, semesterPendek, false, false, Boolean.valueOf(keDatabase));
        JSONArray rows = new JSONArray();
        int totalSks = 0;
        boolean approved = false, pending = false;
        for (Long id : ids) {
            Detailperkuliahan detail = detail(id);
            JSONObject row = baris(mahasiswa, detail);
            if (row == null) continue;
            totalSks += row.optInt("sksAngka", 0);
            row.remove("sksAngka");
            if (Detailperkuliahan.DISETUJUI.equals(detail.getPersetujuan())) approved = true;
            else pending = true;
            rows.put(row);
        }
        JSONObject result = new JSONObject().put("tahunAkademik", tahunAkademik)
                .put("semester", semester).put("tahapan", tahapan)
                .put("baris", rows).put("total", rows.length()).put("totalSks", totalSks)
                .put("statusPersetujuan", NewUiKrsNonPaketController.status(approved, pending));
        if (krs != null) {
            result.put("krsId", krs.getId()).put("kelas", nz(krs.getKelas()))
                    .put("dosenPa", krs.getDosenPa() == null ? "" : nz(krs.getDosenPa().getNama()))
                    .put("keterangan", nz(krs.getKeterangan()));
        }
        return result;
    }

    private static JSONObject baris(Mahasiswa mahasiswa, Detailperkuliahan detail)
            throws JSONException {
        if (detail == null) return null;
        Perkuliahan perkuliahan = detail.getPerkuliahan();
        Matakuliah matakuliah = perkuliahan == null
                ? detail.getMatakuliahKonversi() : perkuliahan.getMatakuliah();
        if (matakuliah == null) return null;
        Matakuliah asli = matakuliah;
        try {
            Matakuliah[] pair = Common.getMatakuliahApakahEkivalen(
                    matakuliah, mahasiswa == null ? null : mahasiswa.getNim(), false);
            if (pair != null && pair.length > 0) matakuliah = pair[0];
            if (pair != null && pair.length > 1) asli = pair[1];
        } catch (Exception ignored) { }
        if (matakuliah == null) return null;
        boolean sama = asli == null || matakuliah.getId() == null
                || matakuliah.getId().equals(asli.getId());
        String dosen = perkuliahan == null ? "" : aman(
                ais.action.master.helper.PerkuliahanUIHelper.generateTeksDosenPerkuliahan(perkuliahan));
        String jadwal = perkuliahan == null ? "" : aman(
                ais.action.master.helper.PerkuliahanUIHelper.generateHariJamRuanganPerkuliahanUmumText(perkuliahan));
        Integer semesterPerkuliahan = perkuliahan == null ? null : perkuliahan.getSemester();
        return new JSONObject().put("id", detail.getId())
                .put("jenis", perkuliahan == null ? "konversi" : "perkuliahan")
                .put("kode", NewUiUjianMahasiswaController.labelEkivalen(
                        matakuliah.getKode(), asli == null ? null : asli.getKode(), sama))
                .put("nama", NewUiUjianMahasiswaController.labelEkivalen(
                        matakuliah.getNama(), asli == null ? null : asli.getNama(), sama))
                .put("sks", NewUiUjianMahasiswaController.labelEkivalen(
                        String.valueOf(matakuliah.getSks()),
                        asli == null ? null : String.valueOf(asli.getSks()), sama))
                .put("sksAngka", matakuliah.getSks())
                .put("dosen", dosen).put("jadwal", jadwal)
                .put("semester", labelSemester(detail.getSemester(), semesterPerkuliahan))
                .put("kelas", perkuliahan == null ? "" : nz(perkuliahan.getKelas()))
                .put("persetujuan", Detailperkuliahan.DISETUJUI.equals(detail.getPersetujuan())
                        ? "Ya" : "Belum")
                .put("nilaiHuruf", nz(detail.getNilaiHuruf()))
                .put("totalNilai", detail.getTotalNilai() == null
                        ? JSONObject.NULL : detail.getTotalNilai())
                .put("bolehHapus", bolehHapus(detail));
    }

    @SuppressWarnings("unchecked")
    private static void hapus(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa)
            throws JSONException {
        Long id = longValue(request.getParameter("id"));
        if (id == null) throw new IllegalArgumentException("Baris yang dihapus harus disebutkan.");
        Session session = HibernateUtil.currentSession();
        Detailperkuliahan detail = (Detailperkuliahan) session.get(Detailperkuliahan.class, id);
        if (detail == null) throw new IllegalArgumentException("Data tidak ditemukan.");
        if (detail.getMahasiswa() == null || detail.getMahasiswa().getId() == null
                || !mahasiswa.getId().equals(detail.getMahasiswa().getId())) {
            throw new SecurityException("Data ini bukan milik Anda.");
        }
        if (!bolehHapus(detail)) {
            throw new IllegalArgumentException("Mata kuliah tidak dapat dihapus karena nilainya tidak nol.");
        }
        List<Komentar> comments = session.createCriteria(Komentar.class)
                .add(Restrictions.eq("detailperkuliahan", detail.getId())).list();
        for (Komentar comment : comments) Common.refreshDelete(comment);
        // Helper konversi lama memakai session.delete, bukan refreshDelete.
        session.delete(detail);
        j.put("dihapus", id);
    }

    private static Detailperkuliahan detail(Long id) {
        try {
            return (Detailperkuliahan) ConstantValues.ambil(
                    Detailperkuliahan.class.getName(), (Serializable) id);
        } catch (Exception e) { return null; }
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

    private static Long longValue(String value) {
        try { return value == null || value.trim().length() == 0
                ? null : Long.valueOf(value.trim()); }
        catch (Exception e) { return null; }
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }

    private static String nz(String value) { return value == null ? "" : value; }
    private static String aman(String value) { return value == null ? "" : value; }

    static JSONObject sukses(JSONObject data) throws JSONException {
        return new JSONObject().put("success", true).put("data", data);
    }

    private static JSONArray fields() throws JSONException {
        return new JSONArray()
                .put(field("tahunAkademik", "Tahun Akademik", "java.lang.String"))
                .put(field("semesterKelompok", "Semester", "java.lang.Integer"))
                .put(field("kode", "Kode", "java.lang.String"))
                .put(field("nama", "Mata Kuliah", "java.lang.String"))
                .put(field("sks", "SKS", "java.lang.String"))
                .put(field("jenis", "Jenis", "java.lang.String"))
                .put(field("dosen", "Dosen", "java.lang.String"))
                .put(field("jadwal", "Jadwal", "java.lang.String"))
                .put(field("kelas", "Kelas", "java.lang.String"))
                .put(field("persetujuan", "Persetujuan", "java.lang.String"))
                .put(field("nilaiHuruf", "Nilai Huruf", "java.lang.String"))
                .put(field("totalNilai", "Nilai", "java.lang.Double"));
    }

    private static JSONObject field(String property, String label, String javaType)
            throws JSONException {
        return new JSONObject().put("property", property).put("label", label)
                .put("javaType", javaType).put("tableVisible", true)
                .put("readable", true).put("createable", false)
                .put("updateable", false);
    }

    private static void fail(JSONObject j, String code, String message) throws JSONException {
        j.put("success", false).put("code", code).put("message", nz(message));
    }

    private static void write(HttpServletResponse response, JSONObject j) throws Exception {
        response.getWriter().print(j.toString());
    }
}
