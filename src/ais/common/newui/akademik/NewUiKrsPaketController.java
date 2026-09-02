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

/**
 * KRS Paket: rencana studi mahasiswa yang diambil sebagai satu paket matakuliah.
 *
 * <h3>Membaca tidak menulis; menyegarkan menulis</h3>
 * <p>Layar lama punya dua jalur muat yang berbeda, dan perbedaannya penting:
 * {@code onSearchDefault} memanggil {@code load(false)} sedangkan
 * {@code onSearchDefaultKeDatabase} memanggil {@code load(true)}. Nilai itu
 * diteruskan sebagai {@code keDatabase} ke
 * {@code Common.singkronkanKrsMahasiswa}, yang <b>menentukan apakah hasil
 * sinkronisasi ditulis ke basis data</b>.</p>
 *
 * <p>Pemisahan itu dipertahankan apa adanya: {@code list} menyinkronkan tanpa
 * menulis, dan {@code update} menulis. Menyatukan keduanya akan membuat setiap
 * penyegaran daftar — yang pada aplikasi native bisa terjadi jauh lebih sering
 * daripada membuka layar ZK — ikut menulis data akademik.</p>
 *
 * <h3>Rentang semester berbeda dari layar akademik lain</h3>
 * <p>Layar ini mengisi pilihan semester 1..{@code semesterLulus} bila mahasiswa
 * sudah punya semester lulus, dan 1..40 bila belum — bukan 1..20 seperti layar
 * Kuesioner dan Ujian. Bawaannya pun dibatasi: bila semester berjalan sudah
 * melewati semester lulus, yang dipilih adalah semester lulus. Menyalin aturan
 * dari layar akademik lain akan menyembunyikan semester yang seharusnya dapat
 * dilihat mahasiswa lama.</p>
 *
 * <h3>Pengambilan paket</h3>
 * <p>Tersedia lewat aksi {@code create}, dan keempat gerbangnya — syarat ujian
 * ber-KRS, dosen pembimbing akademik, pembayaran, serta status keaktifan —
 * dievaluasi ulang di dalamnya, bukan dipercayakan pada pemanggilan
 * {@code options} sebelumnya.</p>
 *
 * <p>Satu jalur masih ditutup: bila konfigurasi pembuatan jadwal otomatis
 * menyala, layar lama membuat jadwal kosong lebih dulu, dan jalur itu belum
 * dipindahkan. Permintaannya ditolak seluruhnya, dan {@code meta} menyatakannya
 * lewat {@code ambilPaketTersedia: false} beserta alasannya. Mengambil sebagian
 * paket akan tampak berhasil dan meninggalkan KRS yang kurang.</p>
 */
public final class NewUiKrsPaketController {

    private static final String MODULE = "root";

    /** Batas atas pilihan semester bila mahasiswa belum punya semester lulus. */
    static final int MAKS_SEMESTER_TANPA_LULUS = 40;

    private NewUiKrsPaketController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        JSONObject json = new JSONObject();
        try {
            String action = teks(request.getParameter("action"), "meta");
            if (!aksiDikenal(action)) {
                response.setStatus(405);
                gagal(json, "ACTION_NOT_ALLOWED", "Aksi tidak dikenal pada layar ini.");
                tulis(response, json);
                return;
            }
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                gagal(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                tulis(response, json);
                return;
            }
            if (mengubah(action)) {
                if (!"POST".equalsIgnoreCase(request.getMethod())) {
                    response.setStatus(405);
                    gagal(json, "METHOD_NOT_ALLOWED", "Gunakan HTTP POST untuk perubahan data.");
                    tulis(response, json);
                    return;
                }
                if (!NewUiCsrfUtil.isValid(request)) {
                    response.setStatus(403);
                    gagal(json, "CSRF_INVALID", "Token keamanan tidak valid. Muat ulang halaman.");
                    tulis(response, json);
                    return;
                }
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            Mahasiswa mahasiswa = user.getMahasiswa();
            if (mahasiswa == null || mahasiswa.getId() == null) {
                throw new SecurityException("Layar ini hanya untuk akun mahasiswa.");
            }

            if ("meta".equals(action)) meta(json, request, mahasiswa);
            else if ("list".equals(action)) daftar(json, request, mahasiswa, false);
            else if ("options".equals(action)) gerbang(json, request, mahasiswa);
            else if ("create".equals(action)) ambilPaket(json, request, user, mahasiswa);
            else if ("update".equals(action)) daftar(json, request, mahasiswa, true);
            else hapus(json, request, mahasiswa);
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            gagal(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            gagal(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            gagal(json, "INTERNAL_ERROR", "Permintaan gagal diproses.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiKrsPaketController"); }
            catch (Exception diabaikan) { }
        }
        tulis(response, json);
    }

    /**
     * Aksi yang dilayani.
     *
     * <p>{@code update} dipakai untuk menyegarkan KRS ke basis data — kata kerja
     * yang dikenal {@code NewUiRouteGuard} dan menuntut izin Update, yang memang
     * sesuai karena aksi itu menulis. Kata kerja sendiri akan ditolak penjaga.</p>
     */
    static boolean aksiDikenal(String action) {
        return "meta".equals(action) || "list".equals(action) || "options".equals(action)
                || "create".equals(action) || "update".equals(action) || "delete".equals(action);
    }

    /** Aksi yang mengubah data; menuntut POST dan token CSRF. */
    static boolean mengubah(String action) {
        return "create".equals(action) || "update".equals(action) || "delete".equals(action);
    }

    /**
     * Batas atas pilihan semester.
     *
     * <p>Disalin dari {@code KrsPaketAction}: sampai semester lulus bila sudah
     * ada dan lebih dari nol, selain itu 40.</p>
     */
    static int batasSemester(Integer semesterLulus) {
        return semesterLulus != null && semesterLulus.intValue() > 0
                ? semesterLulus.intValue() : MAKS_SEMESTER_TANPA_LULUS;
    }

    /**
     * Semester yang dipilih secara bawaan.
     *
     * <p>Bila semester berjalan sudah melewati semester lulus, yang dipilih
     * adalah semester lulus — bukan semester berjalan.</p>
     */
    static int semesterBawaan(Integer semesterLulus, int semesterBerjalan) {
        if (semesterLulus != null && semesterBerjalan > semesterLulus.intValue()) {
            return semesterLulus.intValue();
        }
        return semesterBerjalan;
    }

    // ------------------------------------------------------------------ meta

    private static void meta(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa)
            throws JSONException {
        j.put("judul", "Isi KRS (Paket)");
        j.put("mahasiswa", new JSONObject()
                .put("id", mahasiswa.getId())
                .put("nama", mahasiswa.getNama() == null ? "" : mahasiswa.getNama())
                .put("nim", mahasiswa.getNim() == null ? "" : mahasiswa.getNim()));

        int batas = batasSemester(mahasiswa.getSemesterLulus());
        JSONArray pilihan = new JSONArray();
        for (int i = 1; i <= batas; i++) pilihan.put(i);
        j.put("pilihanSemester", pilihan);
        j.put("semesterBawaan", semesterBawaan(mahasiswa.getSemesterLulus(),
                NewUiKuesionerMahasiswaController.semesterSekarang(mahasiswa)));

        j.put("csrfHeader", NewUiCsrfUtil.HEADER);
        j.put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));

        // Satu jalur konfigurasi masih menutup pengambilan: bila pembuatan
        // jadwal otomatis menyala, layar lama membuat jadwal kosong lebih dulu
        // dan jalur itu belum dipindahkan. Dinyatakan terbuka supaya klien
        // menampilkan arahan, bukan tombol yang gagal saat ditekan.
        boolean jadwalOtomatis = konfigurasiAktif2(
                "untuk_pengambilan_krs_paket_jika_jadwal_belum_dibuat_otomatis_"
                + "membuat_jadwal_dengan_waktu_ruang_dosen_yang_kosong");
        j.put("ambilPaketTersedia", !jadwalOtomatis);
        j.put("ambilPaketAlasan", jadwalOtomatis
                ? "Institusi ini mengaktifkan pembuatan jadwal otomatis saat pengambilan KRS "
                + "paket, dan jalur itu belum tersedia pada tampilan baru. Gunakan tampilan lama."
                : "");
        j.put("catatanSinkronisasi", "Menyegarkan (update) menuliskan hasil sinkronisasi KRS "
                + "ke basis data; menampilkan daftar tidak.");
    }

    // ------------------------------------------------------------------ list

    private static void daftar(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa,
            boolean keDatabase) throws JSONException {
        int batas = batasSemester(mahasiswa.getSemesterLulus());
        int bawaan = semesterBawaan(mahasiswa.getSemesterLulus(),
                NewUiKuesionerMahasiswaController.semesterSekarang(mahasiswa));
        int semester = angka(request.getParameter("semester"), bawaan);
        if (semester < 1 || semester > batas) {
            throw new IllegalArgumentException("Semester harus antara 1 dan " + batas + ".");
        }
        Integer tahapan = angkaOpsional(request.getParameter("tahapan"));
        Integer semesterPendek = semesterPendek(request);

        // keDatabase mengikuti aksi: list tidak menulis, update menulis.
        KrsMahasiswa krs = Common.singkronkanKrsMahasiswa(mahasiswa, Integer.valueOf(semester),
                tahapan, semesterPendek, keDatabase, false);

        List<Long> ids = Common.getDetailperkuliahans(mahasiswa, Integer.valueOf(semester),
                null, semesterPendek, false, Boolean.FALSE, Boolean.valueOf(keDatabase));

        JSONArray baris = new JSONArray();
        int totalSks = 0;
        for (int i = 0; ids != null && i < ids.size(); i++) {
            JSONObject o = barisDetail(mahasiswa, ids.get(i));
            if (o == null) continue;
            totalSks += o.optInt("sksAngka", 0);
            o.remove("sksAngka");
            baris.put(o);
        }
        j.put("baris", baris);
        j.put("total", baris.length());
        j.put("totalSks", totalSks);
        j.put("semester", semester);
        j.put("ditulisKeBasisData", keDatabase);
        // Tahun ajaran semester ini. Tanpa ini pengambilan paket tidak dapat
        // dipakai sama sekali: aksi `create` mewajibkan tahunAjaran, sedangkan
        // tidak ada aksi lain yang pernah memberitahukannya kepada klien.
        // Memakai helper yang sama dengan Ikut Perkuliahan supaya kedua layar
        // menurunkannya dari sumber yang sama.
        j.put("tahunAjaran", NewUiIkutPerkuliahanController.tahunAjaran(
                mahasiswa, semester, semesterPendek));
        if (krs != null) {
            // Ringkasan KRS memakai field yang benar-benar ada pada
            // KrsMahasiswa: sks yang diambil dan keterangannya.
            j.put("krs", new JSONObject()
                    .put("id", krs.getId())
                    .put("sksYangDiambil", krs.getSksYangDiambil() == null
                            ? 0 : krs.getSksYangDiambil().intValue())
                    .put("keterangan", krs.getKeterangan() == null ? "" : krs.getKeterangan()));
        }
    }

    static JSONObject barisDetail(Mahasiswa mahasiswa, Long id) throws JSONException {
        Detailperkuliahan d;
        try {
            d = (Detailperkuliahan) ConstantValues.ambil(
                    Detailperkuliahan.class.getName(), (Serializable) id);
        } catch (Exception e) {
            return null;
        }
        if (d == null) return null;
        Perkuliahan perkuliahan = d.getPerkuliahan();
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
        if (matakuliah == null) return null;
        boolean sama = asli == null || matakuliah.getId() == null
                || matakuliah.getId().equals(asli.getId());

        return new JSONObject()
                .put("id", d.getId())
                .put("kode", NewUiUjianMahasiswaController.labelEkivalen(
                        matakuliah.getKode(), asli == null ? null : asli.getKode(), sama))
                .put("nama", NewUiUjianMahasiswaController.labelEkivalen(
                        matakuliah.getNama(), asli == null ? null : asli.getNama(), sama))
                .put("sks", NewUiUjianMahasiswaController.labelEkivalen(
                        String.valueOf(matakuliah.getSks()),
                        asli == null ? null : String.valueOf(asli.getSks()), sama))
                .put("sksAngka", matakuliah.getSks())
                .put("dosen", aman(ais.action.master.helper.PerkuliahanUIHelper
                        .generateTeksDosenPerkuliahan(perkuliahan)))
                .put("jadwal", aman(ais.action.master.helper.PerkuliahanUIHelper
                        .generateHariJamRuanganPerkuliahanUmumText(perkuliahan)))
                .put("nilaiHuruf", d.getNilaiHuruf() == null ? "" : d.getNilaiHuruf())
                .put("persetujuan", d.getPersetujuan() == null ? "" : String.valueOf(d.getPersetujuan()))
                .put("bolehHapus", NewUiIkutPerkuliahanController.bolehHapus(d));
    }

    // ----------------------------------------------------------------- hapus

    @SuppressWarnings("unchecked")
    private static void hapus(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa)
            throws JSONException {
        Long id = angkaPanjang(request.getParameter("id"));
        if (id == null) throw new IllegalArgumentException("Baris yang dihapus harus disebutkan.");

        Session session = HibernateUtil.currentSession();
        Detailperkuliahan detail = (Detailperkuliahan) session.get(Detailperkuliahan.class, id);
        if (detail == null) throw new IllegalArgumentException("Data tidak ditemukan.");
        if (detail.getMahasiswa() == null || detail.getMahasiswa().getId() == null
                || !detail.getMahasiswa().getId().equals(mahasiswa.getId())) {
            throw new SecurityException("Data ini bukan milik Anda.");
        }
        if (!NewUiIkutPerkuliahanController.bolehHapus(detail)) {
            throw new IllegalArgumentException("Mohon maaf, mata kuliah ini tidak dapat dihapus "
                    + "karena nilainya tidak nol (telah memiliki nilai). Apabila penghapusan tetap "
                    + "diperlukan, hubungi bagian Akademik atau Admin.");
        }

        List<Komentar> komentars = session.createCriteria(Komentar.class)
                .add(Restrictions.eq("detailperkuliahan", detail.getId())).list();
        for (int i = 0; i < komentars.size(); i++) {
            Common.refreshDelete(komentars.get(i));
        }
        // Layar lama memakai session.delete di sini, bukan refreshDelete seperti
        // pada Ikut Perkuliahan. Perbedaan itu dipertahankan.
        session.delete(detail);
        j.put("dihapus", id);
    }

    // --------------------------------------------------------------- gerbang

    /**
     * Evaluasi gerbang pengambilan paket, tanpa mengambil apa pun.
     *
     * <p>Layar lama memeriksa empat hal berurutan lalu berhenti pada yang
     * pertama gagal, sehingga mahasiswa hanya melihat satu alasan meskipun
     * beberapa syarat belum terpenuhi. Kontrak ini mengevaluasi seluruhnya dan
     * mengembalikan semuanya — memberitahukannya satu per satu memaksa
     * mahasiswa bolak-balik tanpa keperluan.</p>
     *
     * <p>Aturan tiap gerbang disalin apa adanya, termasuk bawaan
     * konfigurasinya ({@code Konfigurasi.AKTIF}). Gerbang yang dimatikan
     * institusi harus tetap dianggap lolos; menyalakannya sendiri akan menolak
     * mahasiswa yang selama ini boleh mengambil KRS.</p>
     *
     * <p>Baca saja: tidak ada paket yang diambil di sini, dan sinkronisasi
     * dipanggil dengan {@code keDatabase} bernilai false.</p>
     */
    @SuppressWarnings("unchecked")
    private static void gerbang(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa)
            throws JSONException {
        int batas = batasSemester(mahasiswa.getSemesterLulus());
        int bawaan = semesterBawaan(mahasiswa.getSemesterLulus(),
                NewUiKuesionerMahasiswaController.semesterSekarang(mahasiswa));
        int semester = angka(request.getParameter("semester"), bawaan);
        if (semester < 1 || semester > batas) {
            throw new IllegalArgumentException("Semester harus antara 1 dan " + batas + ".");
        }
        Integer tahapan = angkaOpsional(request.getParameter("tahapan"));
        Integer semesterPendek = semesterPendek(request);

        KrsMahasiswa krs = Common.singkronkanKrsMahasiswa(mahasiswa, Integer.valueOf(semester),
                tahapan, semesterPendek, false, false);

        JSONArray daftar = new JSONArray();
        boolean semuaLolos = true;

        // 1. Syarat ujian yang menyaratkan KRS.
        StringBuilder peringatan = new StringBuilder();
        try {
            List<ais.database.model.SyaratUjian> syarats = ConstantValues.simpleList(
                    HibernateUtil.currentSession()
                            .createCriteria(ais.database.model.SyaratUjian.class)
                            .add(Restrictions.eq("krs", Boolean.TRUE))
                            .add(Restrictions.or(Restrictions.isNull("aktif"),
                                    Restrictions.eq("aktif", Boolean.TRUE))),
                    ais.database.model.SyaratUjian.class);
            List<String> warnings = new java.util.ArrayList<String>();
            for (int i = 0; syarats != null && i < syarats.size(); i++) {
                ais.action.master.SyaratUjianAction.checkSyaratSyaratUjian(
                        syarats.get(i), null, mahasiswa, Integer.valueOf(semester),
                        "Ambil KRS", warnings);
            }
            for (int i = 0; i < warnings.size(); i++) {
                if (peringatan.length() > 0) peringatan.append("\n\n");
                peringatan.append(warnings.get(i));
            }
        } catch (Exception e) {
            // Gagal memeriksa bukan berarti lolos.
            peringatan.append("Syarat pengambilan KRS tidak dapat diperiksa saat ini.");
        }
        boolean lolosSyarat = peringatan.length() == 0;
        semuaLolos = semuaLolos && lolosSyarat;
        daftar.put(gerbangJson("syarat_ujian", lolosSyarat,
                lolosSyarat ? "" : peringatan.toString()));

        // 2. Dosen Pembimbing Akademik.
        boolean wajibPa = konfigurasiAktif("dosen_pa_harus_ada_sebelum_isi_krs");
        Object dosenPa = krs == null ? null : krs.getDosenPa();
        boolean lolosPa = !wajibPa || dosenPa != null;
        semuaLolos = semuaLolos && lolosPa;
        daftar.put(gerbangJson("dosen_pa", lolosPa, lolosPa ? ""
                : "Mohon maaf, Anda belum memiliki Dosen Pembimbing Akademik sehingga belum dapat "
                + "mengambil KRS. Hubungi bagian Akademik atau Admin Fakultas/Prodi untuk "
                + "mendaftarkannya."));

        // 3. Pembayaran.
        boolean wajibBayar = konfigurasiAktif("mahasiswa_harus_bayar_sebelum_isi_krs");
        boolean lolosBayar = true;
        if (wajibBayar) {
            try {
                if (!Common.checkStatusPembayaranMahasiswa(Integer.valueOf(semester), tahapan,
                        mahasiswa, false, false) && semester >= 1) {
                    lolosBayar = false;
                }
                if (lolosBayar && !ais.action.master.helper.UtsDanUasCheckerHelper
                        .checkPembayaranSebelumKRSSudahMemenuhi(mahasiswa,
                                Integer.valueOf(semester), tahapan)) {
                    lolosBayar = false;
                }
            } catch (Exception e) {
                lolosBayar = false;
            }
        }
        semuaLolos = semuaLolos && lolosBayar;
        daftar.put(gerbangJson("pembayaran", lolosBayar, lolosBayar ? ""
                : "Mohon maaf, pembayaran biaya perkuliahan pada semester " + semester
                + " belum selesai. Lakukan pembayaran terlebih dahulu, lalu ambil kembali KRS ini."));

        // 4. Status keaktifan mahasiswa.
        boolean wajibAktif = konfigurasiAktif("status_mahasiswa_harus_aktif_sebelum_isi_krs");
        boolean lolosAktif = true;
        String namaStatus = "";
        if (wajibAktif) {
            try {
                ais.database.model.StatusMahasiswa status =
                        ais.action.master.helper.HistoryStatusMahasiswaUtil
                                .getHistoryStatusMahasiswa(krs)
                                .ambilStatusMahasiswa(Integer.valueOf(semester));
                namaStatus = status == null || status.getNama() == null ? "" : status.getNama();
                lolosAktif = status != null && status.getId() != null
                        && status.getId().equals(ConstantValues.AKTIF.getId());
            } catch (Exception e) {
                lolosAktif = false;
            }
        }
        semuaLolos = semuaLolos && lolosAktif;
        daftar.put(gerbangJson("status_aktif", lolosAktif, lolosAktif ? ""
                : "Mohon maaf, status kemahasiswaan Anda saat ini adalah \"" + namaStatus
                + "\" sehingga Anda belum dapat mengambil KRS."));

        j.put("gerbang", daftar);
        j.put("bolehAmbilPaket", semuaLolos);
        j.put("semester", semester);
        // Ikut diumumkan di sini juga: klien yang memeriksa gerbang lebih dulu
        // sudah memegang semua yang dibutuhkan untuk memanggil `create`.
        j.put("tahunAjaran", NewUiIkutPerkuliahanController.tahunAjaran(
                mahasiswa, semester, semesterPendek));
    }

    // ------------------------------------------------------------ ambil paket

    /**
     * Mengambil paket perkuliahan menjadi baris KRS.
     *
     * <h4>Gerbang dijalankan lebih dulu, dan menutup</h4>
     * <p>Keempat gerbang dievaluasi ulang di sini — bukan dipercayakan pada
     * pemanggilan {@code options} sebelumnya. Klien dapat memanggil aksi ini
     * langsung, dan keadaan dapat berubah di antara dua permintaan.</p>
     *
     * <h4>Satu jalur konfigurasi sengaja ditolak</h4>
     * <p>Bila {@code untuk_pengambilan_krs_paket_jika_jadwal_belum_dibuat_...}
     * menyala, layar lama <b>membuat jadwal kosong</b> untuk matakuliah paket
     * yang belum berjadwal sebelum mengambil. Pembuatan jadwal itu belum
     * dipindahkan, jadi permintaan ditolak dengan jelas alih-alih mengambil
     * sebagian paket diam-diam. Bawaan konfigurasi itu TIDAK_AKTIF.</p>
     *
     * <h4>Aturan per matakuliah, disalin</h4>
     * <ul>
     *   <li>prasyarat gagal → <b>dilewati diam-diam</b>, seperti aslinya;</li>
     *   <li>sudah punya baris untuk perkuliahan itu → dilewati;</li>
     *   <li>kapasitas kelas <b>diambil alih kuota per angkatan</b> bila ada;
     *       yang penuh dilewati dan dikumpulkan sebagai peringatan;</li>
     *   <li>baris baru berstatus {@code DISETUJUI} — berbeda dari Ikut
     *       Perkuliahan yang {@code BELUM_DISETUJUI}.</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private static void ambilPaket(JSONObject j, HttpServletRequest request, Tbmuser user,
            Mahasiswa mahasiswa) throws Exception {
        int batas = batasSemester(mahasiswa.getSemesterLulus());
        int bawaan = semesterBawaan(mahasiswa.getSemesterLulus(),
                NewUiKuesionerMahasiswaController.semesterSekarang(mahasiswa));
        int semester = angka(request.getParameter("semester"), bawaan);
        if (semester < 1 || semester > batas) {
            throw new IllegalArgumentException("Semester harus antara 1 dan " + batas + ".");
        }
        String tahunAjaran = teks(request.getParameter("tahunAjaran"), "");
        if (tahunAjaran.length() == 0) {
            throw new IllegalArgumentException("Tahun ajaran wajib diisi.");
        }
        Integer semesterPendek = semesterPendek(request);

        if (konfigurasiAktif2("untuk_pengambilan_krs_paket_jika_jadwal_belum_dibuat_otomatis_"
                + "membuat_jadwal_dengan_waktu_ruang_dosen_yang_kosong")) {
            throw new IllegalArgumentException("Institusi ini mengaktifkan pembuatan jadwal "
                    + "otomatis saat pengambilan KRS paket, dan jalur itu belum tersedia pada "
                    + "tampilan baru. Gunakan tampilan lama untuk mengambil paket.");
        }

        // Gerbang dievaluasi ulang; menutup bila ada yang belum lolos.
        JSONObject cek = new JSONObject();
        gerbang(cek, request, mahasiswa);
        if (!cek.optBoolean("bolehAmbilPaket", false)) {
            j.put("gerbang", cek.opt("gerbang"));
            throw new IllegalArgumentException(
                    "Syarat pengambilan KRS belum terpenuhi. Rincian pada daftar gerbang.");
        }

        ais.database.model.PaketPerkuliahan paket = cariPaket(
                mahasiswa, semester, tahunAjaran, semesterPendek);
        if (paket == null) {
            throw new IllegalArgumentException("Tidak ada paket perkuliahan yang sesuai dengan "
                    + "data Anda untuk semester " + semester + ".");
        }

        String kelas = mahasiswa.getKelas() == null ? "" : mahasiswa.getKelas().trim();
        JSONArray ditolak = new JSONArray();
        int berhasil = 0;

        org.hibernate.Session sesi = HibernateUtil.getSessionFactory().openSession();
        boolean transaksiLokal = false;
        try {
            if (!sesi.getTransaction().isActive()) {
                sesi.beginTransaction();
                transaksiLokal = true;
            }

            List<ais.database.model.KurikulumPunyaMatakuliah> isi = sesi
                    .createCriteria(ais.database.model.KurikulumPunyaMatakuliah.class)
                    .add(Restrictions.eq("kurikulum", paket.getKurikulum()))
                    .add(Restrictions.eq("semester", Integer.valueOf(semester))).list();

            for (int i = 0; i < isi.size(); i++) {
                Matakuliah mk = isi.get(i).getMatakuliah();
                if (mk == null) continue;

                Perkuliahan perkuliahan = cariPerkuliahan(sesi, mk, mahasiswa, kelas,
                        semester, tahunAjaran, semesterPendek);
                if (perkuliahan == null) {
                    ditolak.put(baris(mk, "Jadwal belum tersedia untuk matakuliah ini."));
                    continue;
                }

                // Prasyarat gagal dilewati diam-diam pada layar lama. Di sini
                // tetap dilewati, tetapi dilaporkan: sebuah API yang menghilangkan
                // matakuliah tanpa sepatah kata membuat mahasiswa mengira paketnya
                // memang hanya sekian.
                if (!Common.checkMatakuliahPrasyarat(mk, mahasiswa, Integer.valueOf(semester))) {
                    ditolak.put(baris(mk, "Prasyarat matakuliah belum terpenuhi."));
                    continue;
                }

                int sudahAda = ais.action.master.helper.KrsUtilHelper
                        .ambilJumlahDetailperkuliahan(sesi, perkuliahan, mahasiswa, false);
                if (sudahAda != 0) continue;

                int terisi = ais.action.master.helper.KrsUtilHelper
                        .ambilJumlahDetailperkuliahan(sesi, perkuliahan, false) + 1;
                Integer kapasitas = perkuliahan.getKapasitasKelas();
                ais.database.model.PembagianKuotaPerkuliahanBerdasarkantahunAngkatan pembagian =
                        ais.action.master.helper.KrsUtilHelper
                                .ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan(
                                        sesi, perkuliahan, mahasiswa.getTahunangkatan(), false);
                Number kuota = pembagian == null ? null : pembagian.getKuota();
                if (kuota != null) kapasitas = Integer.valueOf(kuota.intValue());
                if (kapasitas != null && terisi > kapasitas.intValue()) {
                    ditolak.put(baris(mk, "Kapasitas perkuliahan sudah penuh. Maksimal "
                            + kapasitas + "."));
                    continue;
                }

                Detailperkuliahan detail = new Detailperkuliahan(user, NewUiKrsPaketController.class);
                detail.setNilaiHuruf("");
                detail.setTotalNilai(Double.valueOf(0.0));
                detail.setMahasiswa(mahasiswa);
                detail.setPerkuliahan(perkuliahan);
                detail.setSemester(Integer.valueOf(semester));
                detail.setPersetujuan(Detailperkuliahan.DISETUJUI);
                ais.action.master.helper.KrsUtilHelper.simpanKrsJikaBelumAda(sesi, detail);
                berhasil++;
            }

            if (transaksiLokal) sesi.getTransaction().commit();
        } catch (Exception e) {
            if (transaksiLokal && sesi.getTransaction().isActive()) {
                try { sesi.getTransaction().rollback(); } catch (Exception diabaikan) { }
            }
            throw e;
        } finally {
            try { if (sesi.isOpen()) sesi.close(); } catch (Exception diabaikan) { }
        }

        j.put("paket", paket.getNama() == null ? "" : paket.getNama());
        j.put("berhasil", berhasil);
        j.put("ditolak", ditolak);
        j.put("semester", semester);
    }

    private static JSONObject baris(Matakuliah mk, String alasan) throws JSONException {
        return new JSONObject()
                .put("kode", mk.getKode() == null ? "" : mk.getKode())
                .put("nama", mk.getNama() == null ? "" : mk.getNama())
                .put("alasan", alasan);
    }

    /** Paket yang cocok; kriterianya disalin dari layar pemilih paket. */
    private static ais.database.model.PaketPerkuliahan cariPaket(Mahasiswa mahasiswa,
            int semester, String tahunAjaran, Integer semesterPendek) {
        org.hibernate.Session sesi = HibernateUtil.currentSession();
        return (ais.database.model.PaketPerkuliahan) ConstantValues.simpleObject(
                sesi.createCriteria(ais.database.model.PaketPerkuliahan.class)
                        .add(Restrictions.sqlRestriction(semester + " between minsmt and maxsmt"))
                        .add(Restrictions.sqlRestriction(
                                mahasiswa.getTahunangkatan() + " between mulai and sampai"))
                        .add(semesterPendek == null
                                ? Restrictions.isNull("statusSemesterPendek")
                                : Restrictions.eq("statusSemesterPendek", semesterPendek))
                        .add(Restrictions.eq("tahunAkademik", tahunAjaran))
                        .addOrder(org.hibernate.criterion.Order.desc("angkatanMulai"))
                        .addOrder(org.hibernate.criterion.Order.desc("angkatanSampai"))
                        .addOrder(org.hibernate.criterion.Order.desc("id"))
                        .setMaxResults(1),
                ais.database.model.PaketPerkuliahan.class);
    }

    /**
     * Perkuliahan untuk sebuah matakuliah paket.
     *
     * <p>Kriterianya disalin, termasuk penyaring
     * {@code tampilkanSaatPengambilanKrs} yang mudah terlewat: tanpa itu, kelas
     * yang sengaja disembunyikan dari pengambilan KRS ikut terambil.</p>
     *
     * <p>Layar lama menyimpan hasilnya ke peta bertakik id matakuliah di dalam
     * perulangan, sehingga bila ada beberapa kelas yang cocok, <b>yang terakhir
     * yang dipakai</b>. Perilaku itu ditiru dengan mengambil elemen terakhir.</p>
     */
    @SuppressWarnings("unchecked")
    private static Perkuliahan cariPerkuliahan(org.hibernate.Session sesi, Matakuliah mk,
            Mahasiswa mahasiswa, String kelas, int semester, String tahunAjaran,
            Integer semesterPendek) {
        if (mahasiswa.getJurusan() == null) return null;
        List<Perkuliahan> hasil = sesi.createCriteria(Perkuliahan.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"),
                        Restrictions.eq("aktif", Boolean.TRUE)))
                .add(Restrictions.or(Restrictions.isNull("tampilkanSaatPengambilanKrs"),
                        Restrictions.eq("tampilkanSaatPengambilanKrs", Boolean.TRUE)))
                .add(Restrictions.eq("matakuliah.id", mk.getId()))
                .add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
                        : Restrictions.eq("statusSemesterPendek", semesterPendek))
                .add(Restrictions.eq("jurusan.id", mahasiswa.getJurusan().getId()))
                .add(Restrictions.eq("program", mahasiswa.getProgram()))
                .add(Restrictions.ilike("kelas", kelas,
                        org.hibernate.criterion.MatchMode.EXACT))
                .add(Restrictions.eq("semester", Integer.valueOf(semester)))
                .add(Restrictions.eq("tahunAjaran", tahunAjaran))
                .add(Restrictions.or(Restrictions.eq("merupakan_paralel", Boolean.FALSE),
                        Restrictions.isNull("merupakan_paralel")))
                .list();
        if (hasil == null || hasil.isEmpty()) return null;
        return hasil.get(hasil.size() - 1);
    }

    /** Konfigurasi bernilai AKTIF dengan bawaan TIDAK_AKTIF. */
    private static boolean konfigurasiAktif2(String kunci) {
        try {
            ais.database.model.Konfigurasi k = Common.getKonfigurasi(kunci,
                    ais.database.model.Konfigurasi.TIDAK_AKTIF);
            return k != null && ais.database.model.Konfigurasi.AKTIF.equals(k.getNilai());
        } catch (Exception e) {
            return false;
        }
    }

    private static JSONObject gerbangJson(String kode, boolean lolos, String pesan)
            throws JSONException {
        return new JSONObject().put("kode", kode).put("lolos", lolos)
                .put("pesan", pesan == null ? "" : pesan);
    }

    /** Konfigurasi bernilai AKTIF; bawaannya AKTIF, sama seperti layar lama. */
    private static boolean konfigurasiAktif(String kunci) {
        try {
            ais.database.model.Konfigurasi k = Common.getKonfigurasi(kunci,
                    ais.database.model.Konfigurasi.AKTIF);
            return k != null && ais.database.model.Konfigurasi.AKTIF.equals(k.getNilai());
        } catch (Exception e) {
            // Tidak terbaca berarti diperlakukan menyala, sesuai bawaannya --
            // bukan dimatikan diam-diam.
            return true;
        }
    }

    // --------------------------------------------------------------- utilitas

    private static Integer semesterPendek(HttpServletRequest request) {
        String v = request.getParameter("semesterPendek");
        boolean ya = "1".equals(v) || "true".equalsIgnoreCase(v) || "ya".equalsIgnoreCase(v);
        return ya ? Perkuliahan.SEMESTER_PENDEK : null;
    }

    private static Long angkaPanjang(String nilai) {
        try { return nilai == null ? null : Long.valueOf(nilai.trim()); }
        catch (Exception e) { return null; }
    }

    private static Integer angkaOpsional(String nilai) {
        try { return nilai == null || nilai.trim().length() == 0 ? null : Integer.valueOf(nilai.trim()); }
        catch (Exception e) { return null; }
    }

    private static int angka(String nilai, int bawaan) {
        try { return nilai == null || nilai.trim().length() == 0 ? bawaan : Integer.parseInt(nilai.trim()); }
        catch (Exception e) { return bawaan; }
    }

    private static String aman(String nilai) {
        return nilai == null ? "" : nilai;
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
