package ais.common.newui.akademik;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;

/**
 * Kuesioner (angket dosen) milik mahasiswa yang sedang masuk.
 *
 * <h3>Yang dilayani kontrak ini</h3>
 * <p>Layar lama menyusun satu {@code AngketDosenWindow} per semester dalam
 * rentang yang dipilih, masing-masing atas daftar perkuliahan mahasiswa pada
 * semester itu. Kontrak ini mengembalikan bahan yang sama — tahun ajaran,
 * semester, jenis semester, tahapan, dan id perkuliahannya — sementara
 * pengisian angketnya sendiri tetap milik layar angket yang sudah punya halaman
 * native tersendiri, dan rutenya ikut diumumkan.</p>
 *
 * <h3>Sumber datanya dipakai ulang, bukan disusun ulang</h3>
 * <p>Dua pemanggilan yang menentukan isi layar —
 * {@code Common.generateSemestersForGrid} dan
 * {@code Mahasiswa.ambilPerkuliahanDanParalel} — adalah metode data biasa, bukan
 * kode yang merender komponen. Keduanya dipanggil apa adanya, termasuk
 * penguraian {@code data[1]} dan {@code data[3]} beserta jatuh-ke-nol-nya, dan
 * penentuan Ganjil/Genap dari keganjilan nomor semester. Menyusun ulang
 * perhitungan semester akademik akan menghasilkan daftar yang tampak wajar
 * dengan tahun ajaran yang meleset satu periode.</p>
 *
 * <p>{@code semesterPendek} dikirim {@code null}: pada layar lama field itu
 * memang tidak pernah diisi. Mengisinya dengan nilai lain akan mengubah daftar
 * semester yang dihasilkan.</p>
 *
 * <h3>Milik pemilik sesi, dan baca saja</h3>
 * <p>Mahasiswa diambil dari sesi, bukan dari parameter — sama seperti layar
 * lama, yang menolak siapa pun yang bukan mahasiswa. Menerimanya dari
 * permintaan akan mengubah layar pribadi menjadi jalan membaca perkuliahan
 * mahasiswa lain. Tidak ada aksi mutasi di sini; pengisian angket dilakukan
 * layar angket dengan aturannya sendiri.</p>
 */
public final class NewUiKuesionerMahasiswaController {

    private static final String MODULE = "root";

    /** Halaman native tempat angket benar-benar diisi. */
    private static final String ROUTE_ANGKET = "helper/generic/angket_dosen";

    /** Batas atas pilihan semester; layar lama mengisi combobox 1..20. */
    private static final int MAKS_SEMESTER = 20;

    private NewUiKuesionerMahasiswaController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        JSONObject json = new JSONObject();
        try {
            String action = teks(request.getParameter("action"), "meta");
            if (!"meta".equals(action) && !"list".equals(action)) {
                response.setStatus(405);
                gagal(json, "ACTION_NOT_ALLOWED", "Layar ini hanya menyediakan pembacaan.");
                tulis(response, json);
                return;
            }
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                gagal(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                tulis(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            Mahasiswa mahasiswa = user.getMahasiswa();
            if (mahasiswa == null || mahasiswa.getId() == null) {
                // Sama dengan layar lama, yang menolak dengan pesan
                // "Anda harus login sebagai mahasiswa".
                throw new SecurityException("Layar ini hanya untuk akun mahasiswa.");
            }

            if ("meta".equals(action)) meta(json, mahasiswa);
            else daftar(json, request, mahasiswa);
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            gagal(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            gagal(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            gagal(json, "INTERNAL_ERROR", "Daftar kuesioner gagal disiapkan.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiKuesionerMahasiswaController"); }
            catch (Exception diabaikan) { }
        }
        tulis(response, json);
    }

    private static void meta(JSONObject j, Mahasiswa mahasiswa) throws JSONException {
        j.put("judul", "Kuesioner Mahasiswa");
        j.put("hanyaBaca", true);
        j.put("mahasiswa", new JSONObject()
                .put("id", mahasiswa.getId())
                .put("nama", mahasiswa.getNama() == null ? "" : mahasiswa.getNama())
                .put("nim", mahasiswa.getNim() == null ? "" : mahasiswa.getNim()));
        JSONArray pilihan = new JSONArray();
        for (int i = 1; i <= MAKS_SEMESTER; i++) pilihan.put(i);
        j.put("pilihanSemester", pilihan);
        int sekarang = semesterSekarang(mahasiswa);
        j.put("semesterMulaiBawaan", sekarang);
        j.put("semesterSampaiBawaan", sekarang);
        j.put("routeAngket", ROUTE_ANGKET);
        j.put("catatanCakupan", "Pengisian angket dilakukan pada layar angket dosen; "
                + "layar ini menampilkan semester dan perkuliahan yang menjadi cakupannya.");
    }

    private static void daftar(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa)
            throws JSONException {
        int sekarang = semesterSekarang(mahasiswa);
        int mulai = angka(request.getParameter("mulai"), sekarang);
        int sampai = angka(request.getParameter("sampai"), sekarang);
        if (mulai < 1 || mulai > MAKS_SEMESTER || sampai < 1 || sampai > MAKS_SEMESTER) {
            throw new IllegalArgumentException("Semester harus antara 1 dan " + MAKS_SEMESTER + ".");
        }
        if (mulai > sampai) {
            throw new IllegalArgumentException("Semester mulai tidak boleh melewati semester sampai.");
        }

        // semesterPendek sengaja null: layar lama tidak pernah mengisinya.
        List<String[]> datas = Common.generateSemestersForGrid(mahasiswa, mulai, sampai, null);
        JSONArray baris = new JSONArray();
        for (int i = 0; datas != null && i < datas.size(); i++) {
            String[] data = datas.get(i);
            if (data == null || data.length < 4) continue;

            // Penguraian ini disalin apa adanya dari layar lama, termasuk
            // jatuh-ke-nol ketika isinya bukan angka.
            int semester;
            try {
                semester = Integer.parseInt(data[1].split(",")[0]);
            } catch (Exception e) {
                semester = 0;
            }
            if (semester <= 0) continue;
            int tahapan;
            try {
                tahapan = Integer.parseInt(data[3]);
            } catch (Exception e) {
                tahapan = 0;
            }

            List<Long> perkuliahan = mahasiswa.ambilPerkuliahanDanParalel(
                    Integer.valueOf(semester), null);
            JSONArray idPerkuliahan = new JSONArray();
            for (int k = 0; perkuliahan != null && k < perkuliahan.size(); k++) {
                idPerkuliahan.put(perkuliahan.get(k));
            }
            baris.put(new JSONObject()
                    .put("tahunAjaran", data[0] == null ? "" : data[0])
                    .put("semester", semester)
                    // Layar lama menentukan jenis semester dari keganjilan
                    // nomornya, bukan dari kolom tersendiri.
                    .put("jenisSemester", semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)
                    .put("tahapan", tahapan)
                    .put("jumlahPerkuliahan", idPerkuliahan.length())
                    .put("perkuliahan", idPerkuliahan)
                    .put("routeAngket", ROUTE_ANGKET));
        }
        j.put("baris", baris);
        j.put("total", baris.length());
        j.put("mulai", mulai);
        j.put("sampai", sampai);
    }

    /** Semester berjalan mahasiswa; nol bila tidak dapat dihitung. */
    static int semesterSekarang(Mahasiswa mahasiswa) {
        try {
            Integer s = mahasiswa.currentSemester();
            return s == null ? 0 : s.intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Jenis semester dari nomor semester; dipisah agar dapat diuji. */
    static String jenisSemester(int semester) {
        return semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
    }

    private static int angka(String nilai, int bawaan) {
        try { return nilai == null || nilai.trim().length() == 0 ? bawaan : Integer.parseInt(nilai.trim()); }
        catch (Exception e) { return bawaan; }
    }

    private static String teks(String nilai, String bawaan) {
        return nilai == null || nilai.trim().length() == 0 ? bawaan : nilai.trim();
    }

    private static void gagal(JSONObject j, String kode, String pesan) throws JSONException {
        j.put("ok", false);
        j.put("code", kode);
        j.put("message", pesan == null ? "" : pesan);
    }

    private static void tulis(HttpServletResponse response, JSONObject j) throws java.io.IOException {
        response.getWriter().print(j.toString());
    }
}
