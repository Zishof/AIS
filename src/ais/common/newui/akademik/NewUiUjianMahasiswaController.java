package ais.common.newui.akademik;

import java.io.Serializable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.newui.NewUiRouteGuard;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;

/**
 * Jadwal ujian milik mahasiswa yang sedang masuk.
 *
 * <h3>Aturan yang menentukan isinya</h3>
 * <p>Layar lama menampilkan, untuk tiap semester dalam rentang terpilih, seluruh
 * perkuliahan mahasiswa beserta pertemuan yang <b>statusnya ditandai ujian</b>
 * ({@code pertemuan.getStatusPertemuan().getUjian()}). Aturan itu disalin apa
 * adanya; menyaring dengan cara lain — misalnya menebak dari nama pertemuan yang
 * mengandung "UTS" atau "UAS" — akan melewatkan jenis ujian yang dinamai
 * berbeda oleh sebagian institusi.</p>
 *
 * <h3>Matakuliah ekivalen</h3>
 * <p>Sebuah perkuliahan dapat diambil sebagai matakuliah ekivalen. Layar lama
 * memanggil {@code Common.getMatakuliahApakahEkivalen} dan, bila hasilnya
 * berbeda dari matakuliah aslinya, menampilkan keduanya dalam bentuk
 * {@code kode (kode asli)}. Perilaku itu dipertahankan, termasuk untuk nama dan
 * SKS-nya: menampilkan hanya salah satunya membuat mahasiswa tidak dapat
 * mencocokkan ujian dengan matakuliah pada KRS-nya.</p>
 *
 * <h3>Teks dosen dan jadwal diambil dari pembangkitnya</h3>
 * <p>{@code PerkuliahanUIHelper} menyediakan {@code generateTeksDosenPerkuliahan}
 * dan {@code generateHariJamRuanganPerkuliahanUmumText} — pembangkit teks yang
 * dipakai perendernya sendiri. Keduanya dipanggil langsung, sehingga teks yang
 * muncul di sini sama persis dengan layar lama, termasuk penanganan jadwal
 * paralel.</p>
 *
 * <h3>Milik pemilik sesi, dan baca saja</h3>
 * <p>Mahasiswa diambil dari sesi, bukan parameter. Tidak ada aksi mutasi.</p>
 */
public final class NewUiUjianMahasiswaController {

    private static final String MODULE = "root";

    /** Batas atas pilihan semester; layar lama mengisi combobox 1..20. */
    private static final int MAKS_SEMESTER = 20;

    private NewUiUjianMahasiswaController() { }

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
            gagal(json, "INTERNAL_ERROR", "Jadwal ujian gagal disiapkan.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiUjianMahasiswaController"); }
            catch (Exception diabaikan) { }
        }
        tulis(response, json);
    }

    private static void meta(JSONObject j, Mahasiswa mahasiswa) throws JSONException {
        j.put("judul", "Ujian Mahasiswa");
        j.put("hanyaBaca", true);
        j.put("mahasiswa", new JSONObject()
                .put("id", mahasiswa.getId())
                .put("nama", mahasiswa.getNama() == null ? "" : mahasiswa.getNama())
                .put("nim", mahasiswa.getNim() == null ? "" : mahasiswa.getNim()));
        JSONArray pilihan = new JSONArray();
        for (int i = 1; i <= MAKS_SEMESTER; i++) pilihan.put(i);
        j.put("pilihanSemester", pilihan);
        int sekarang = NewUiKuesionerMahasiswaController.semesterSekarang(mahasiswa);
        j.put("semesterMulaiBawaan", sekarang);
        j.put("semesterSampaiBawaan", sekarang);
        // Setara checkbox "semester pendek" pada layar lama.
        j.put("semesterPendekBawaan", false);
    }

    private static void daftar(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa)
            throws JSONException {
        int sekarang = NewUiKuesionerMahasiswaController.semesterSekarang(mahasiswa);
        int mulai = angka(request.getParameter("mulai"), sekarang);
        int sampai = angka(request.getParameter("sampai"), sekarang);
        if (mulai < 1 || mulai > MAKS_SEMESTER || sampai < 1 || sampai > MAKS_SEMESTER) {
            throw new IllegalArgumentException("Semester harus antara 1 dan " + MAKS_SEMESTER + ".");
        }
        if (mulai > sampai) {
            throw new IllegalArgumentException("Semester mulai tidak boleh melewati semester sampai.");
        }

        // Sama dengan layar lama: dicentang berarti Perkuliahan.SEMESTER_PENDEK,
        // tidak dicentang berarti null -- bukan nol, dan bukan false.
        Integer semesterPendek = benar(request.getParameter("semesterPendek"))
                ? Perkuliahan.SEMESTER_PENDEK : null;

        List<String[]> datas = Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendek);
        JSONArray periode = new JSONArray();
        for (int i = 0; datas != null && i < datas.size(); i++) {
            String[] data = datas.get(i);
            if (data == null || data.length < 4) continue;
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

            JSONArray kuliah = new JSONArray();
            List<Long> ids = mahasiswa.ambilPerkuliahanDanParalel(
                    Integer.valueOf(semester), semesterPendek);
            for (int k = 0; ids != null && k < ids.size(); k++) {
                JSONObject baris = barisPerkuliahan(mahasiswa, ids.get(k));
                if (baris != null) kuliah.put(baris);
            }
            periode.put(new JSONObject()
                    .put("tahunAjaran", data[0] == null ? "" : data[0])
                    .put("semester", semester)
                    .put("jenisSemester", NewUiKuesionerMahasiswaController.jenisSemester(semester))
                    .put("tahapan", tahapan)
                    .put("perkuliahan", kuliah));
        }
        j.put("periode", periode);
        j.put("mulai", mulai);
        j.put("sampai", sampai);
        j.put("semesterPendek", semesterPendek != null);
    }

    /** Satu baris perkuliahan beserta pertemuan ujiannya; null bila tidak dapat dipakai. */
    private static JSONObject barisPerkuliahan(Mahasiswa mahasiswa, Long id) throws JSONException {
        Perkuliahan perkuliahan;
        try {
            perkuliahan = (Perkuliahan) ConstantValues.ambil(
                    Perkuliahan.class.getName(), (Serializable) id);
        } catch (Exception e) {
            return null;
        }
        if (perkuliahan == null) return null;

        Matakuliah matakuliah = perkuliahan.getMatakuliah();
        Matakuliah asli;
        try {
            Matakuliah[] pasangan = Common.getMatakuliahApakahEkivalen(
                    matakuliah, mahasiswa == null ? null : mahasiswa.getNim(), false);
            matakuliah = pasangan[0];
            asli = pasangan[1];
        } catch (Exception e) {
            asli = matakuliah;
        }
        // Layar lama menyembunyikan baris yang matakuliahnya tidak terpecahkan.
        if (matakuliah == null) return null;

        boolean sama = asli == null || matakuliah.getId() == null
                || matakuliah.getId().equals(asli.getId());

        JSONArray ujian = new JSONArray();
        List<Pertemuan> pertemuans = perkuliahan.ambilPertemuanList();
        for (int i = 0; pertemuans != null && i < pertemuans.size(); i++) {
            Pertemuan p = pertemuans.get(i);
            if (p == null || p.getStatusPertemuan() == null) continue;
            Boolean ujianKah = p.getStatusPertemuan().getUjian();
            if (ujianKah == null || !ujianKah.booleanValue()) continue;
            ujian.put(new JSONObject()
                    .put("nama", p.getStatusPertemuan().getNama() == null
                            ? "" : p.getStatusPertemuan().getNama())
                    .put("tanggal", p.getTanggal() == null
                            ? "" : Common.dateFormat4.get().format(p.getTanggal())));
        }

        return new JSONObject()
                .put("kode", labelEkivalen(matakuliah.getKode(),
                        asli == null ? null : asli.getKode(), sama))
                .put("nama", labelEkivalen(matakuliah.getNama(),
                        asli == null ? null : asli.getNama(), sama))
                .put("sks", labelEkivalen(String.valueOf(matakuliah.getSks()),
                        asli == null ? null : String.valueOf(asli.getSks()), sama))
                .put("dosen", teksAman(ais.action.master.helper.PerkuliahanUIHelper
                        .generateTeksDosenPerkuliahan(perkuliahan)))
                .put("jadwal", teksAman(ais.action.master.helper.PerkuliahanUIHelper
                        .generateHariJamRuanganPerkuliahanUmumText(perkuliahan)))
                .put("ujian", ujian);
    }

    /**
     * Label matakuliah ekivalen.
     *
     * <p>Layar lama menampilkan {@code utama} saja ketika matakuliah yang
     * diambil sama dengan aslinya, dan {@code utama (asli)} ketika berbeda.
     * Menampilkan hanya salah satunya membuat mahasiswa tidak dapat mencocokkan
     * ujian dengan matakuliah pada KRS-nya — dan barisnya tetap tampak wajar,
     * jadi kekeliruannya tidak akan dilaporkan siapa pun.</p>
     */
    static String labelEkivalen(String utama, String asli, boolean sama) {
        if (sama) return nilai(utama);
        return nilai(utama) + " (" + nilai(asli) + ")";
    }

    private static String teksAman(String nilai) {
        return nilai == null ? "" : nilai;
    }

    private static String nilai(String v) {
        return v == null ? "" : v;
    }

    private static boolean benar(String nilai) {
        return "1".equals(nilai) || "true".equalsIgnoreCase(nilai) || "ya".equalsIgnoreCase(nilai);
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
