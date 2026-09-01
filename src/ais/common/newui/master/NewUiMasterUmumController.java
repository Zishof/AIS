package ais.common.newui.master;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.konfigurasi.SkemaKonfigurasi;
import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;

/**
 * Kontrak native lima layar master yang tidak termasuk keluarga mana pun.
 *
 * <p>Kelimanya berbeda watak, dan batas yang diambil pun berbeda — disebutkan
 * terang-terangan di sini karena dua di antaranya <b>sengaja tidak</b>
 * memperoleh kemampuan menjalankan pekerjaannya:</p>
 *
 * <ul>
 *   <li><b>Status Kehadiran</b> — baca saja, terkunci identitas mahasiswa
 *       pemilik sesi.</li>
 *   <li><b>Pengaturan Konfigurasi SKP</b> dan <b>Konfigurasi Sekolah</b> —
 *       baca dan simpan, memakai skema yang sama dengan layar ZK.</li>
 *   <li><b>Import Data Dari EPSBED</b> dan <b>Ekspor ke Feeder</b> — hanya
 *       menjelaskan diri; lihat alasannya di bawah.</li>
 * </ul>
 *
 * <h3>Mengapa impor dan ekspor tidak dijalankan dari sini</h3>
 * <p>Impor EPSBED menerima <b>letak folder di server</b> yang diketik pengguna,
 * lalu membaca seluruh isinya. Pada layar ZK jalur itu berada di balik layar
 * admin; membukanya sebagai kontrak berarti klien menentukan folder mana yang
 * dibaca server — pembacaan berkas sembarang, bukan sekadar impor data. Ekspor
 * Feeder menyambung ke server PDDIKTI memakai alamat beserta kata sandi yang
 * tersimpan di konfigurasi.</p>
 *
 * <p>Keduanya juga pekerjaan panjang yang melaporkan kemajuannya lewat widget
 * ZK yang dipegang utas latar — tidak ada catatan kemajuan di sisi server yang
 * dapat ditanya ulang oleh sebuah API. Menjalankannya lewat kontrak menuntut
 * dua hal yang belum ada: folder yang dibatasi pada wilayah yang ditetapkan
 * administrator, dan penyimpan status pekerjaan yang dapat ditanya. Sampai
 * keduanya tersedia, kontrak ini hanya menerangkan mengapa layarnya belum
 * tersedia native — jauh lebih berguna daripada halaman kosong tanpa
 * keterangan.</p>
 *
 * <p>Fail-closed: mode tak dikenal ditolak, sesi tanpa pengguna ditolak,
 * penyimpanan wajib POST beserta token CSRF.</p>
 */
public final class NewUiMasterUmumController {

    /** Harus sama dengan awalan folder JSP sebelum {@code /uiux/}. */
    private static final String MODULE = "root";

    /** Status Kehadiran mahasiswa. */
    public static final String MODE_ABSENSI = "absensi";
    /** Pengaturan Konfigurasi SKP. */
    public static final String MODE_KONFIGURASI_SKP = "konfigurasi_skp";
    /** Konfigurasi Sekolah. */
    public static final String MODE_KONFIGURASI_SEKOLAH = "konfigurasi_sekolah";
    /** Import Data Dari EPSBED — hanya menjelaskan diri. */
    public static final String MODE_IMPOR_EPSBED = "impor_epsbed";
    /** Ekspor ke Feeder — hanya menjelaskan diri. */
    public static final String MODE_EKSPOR_FEEDER = "ekspor_feeder";

    private NewUiMasterUmumController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String mode, String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!modeDikenal(mode)) throw new IllegalArgumentException("Mode master tidak dikenal.");
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");

            if (MODE_ABSENSI.equals(mode)) absensi(json, request, user, action);
            else if (MODE_KONFIGURASI_SKP.equals(mode)) {
                konfigurasi(json, request, action, SkemaKonfigurasi.SKP, "Pengaturan Konfigurasi SKP", false);
            } else if (MODE_KONFIGURASI_SEKOLAH.equals(mode)) {
                konfigurasi(json, request, action, SkemaKonfigurasi.SEKOLAH, "Konfigurasi Sekolah", true);
            } else {
                belumTersedia(json, mode, action);
            }
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memproses permintaan. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiMasterUmumController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    public static boolean modeDikenal(String mode) {
        return MODE_ABSENSI.equals(mode) || MODE_KONFIGURASI_SKP.equals(mode)
                || MODE_KONFIGURASI_SEKOLAH.equals(mode) || MODE_IMPOR_EPSBED.equals(mode)
                || MODE_EKSPOR_FEEDER.equals(mode);
    }

    // ------------------------------------------------------------- kehadiran

    /**
     * Status Kehadiran.
     *
     * <p>Mahasiswanya SELALU diambil dari sesi, tidak pernah dari parameter;
     * menerima nomor mahasiswa dari klien akan membuat siapa pun dapat membaca
     * kehadiran orang lain hanya dengan menggantinya.</p>
     */
    private static void absensi(JSONObject j, HttpServletRequest request, Tbmuser user, String action)
            throws Exception {
        Mahasiswa mahasiswa = user.getMahasiswa();
        if (mahasiswa == null) throw new SecurityException("Halaman ini khusus untuk mahasiswa.");
        int sekarang = mahasiswa.currentSemester() == null ? 1 : mahasiswa.currentSemester().intValue();

        if ("meta".equals(action)) {
            j.put("judul", "Status Kehadiran");
            j.put("mahasiswa", teks(mahasiswa.getNama()));
            j.put("nim", teks(mahasiswa.getNim()));
            JSONArray semester = new JSONArray();
            for (int i = 1; i <= 20; i++) {
                semester.put(i);
            }
            j.put("pilihanSemester", semester);
            j.put("semesterBawaan", sekarang);
            return;
        }
        if (!"list".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");

        int semester = angka(request.getParameter("semester"), 1, 20, sekarang, "Semester tidak sah.");
        boolean semesterPendek = "true".equalsIgnoreCase(text(request.getParameter("semesterPendek"), "false"));
        Integer pendek = semesterPendek
                ? Integer.valueOf(ais.database.model.Perkuliahan.SEMESTER_PENDEK) : null;

        JSONArray periode = new JSONArray();
        List<String[]> datas = Common.generateSemestersForGrid(mahasiswa, Integer.valueOf(semester),
                Integer.valueOf(semester), pendek);
        for (String[] data : datas) {
            int smt = angkaAman(data.length > 1 ? data[1].split(",")[0] : null);
            if (smt <= 0) continue;
            int tahapan = angkaAman(data.length > 3 ? data[3] : null);
            periode.put(satuPeriode(mahasiswa, data.length > 0 ? data[0] : "", smt, tahapan, pendek));
        }
        j.put("periode", periode);
        j.put("semester", semester);
        j.put("semesterPendek", semesterPendek);
    }

    /** Ringkasan KRS satu periode beserta rekap kehadiran tiap mata kuliah. */
    @SuppressWarnings("unchecked")
    private static JSONObject satuPeriode(Mahasiswa mahasiswa, String tahunAjaran, int semester,
            int tahapan, Integer semesterPendek) throws Exception {
        // keDatabase = false: pembacaan TIDAK boleh menulis ulang KRS. Layar ZK
        // memakai nilai yang sama saat sekadar menampilkan.
        ais.database.model.KrsMahasiswa krs = Common.singkronkanKrsMahasiswa(mahasiswa,
                Integer.valueOf(semester), Integer.valueOf(tahapan), semesterPendek, false);

        JSONObject o = new JSONObject();
        o.put("tahunAjaran", teks(tahunAjaran));
        o.put("semester", semester);
        o.put("tahapan", tahapan);
        o.put("tahunAkademik", krs == null ? "" : teks(krs.getTahunAkademik()));
        o.put("dosenPa", krs == null || krs.getDosenPa() == null
                ? "Belum memiliki dosen pembimbing akademik" : teks(krs.getDosenPa().getNama()));
        o.put("ips", krs == null ? 0 : krs.getIps());
        o.put("ipk", krs == null ? 0 : krs.getIpk());
        o.put("sksSemester", krs == null ? 0 : krs.getSksYangDiambil());
        o.put("sksKumulatif", krs == null ? 0 : krs.getSksk());

        JSONArray rows = new JSONArray();
        int totalSks = 0, hadir = 0, sakit = 0, izin = 0, alpa = 0;
        List<Long> perkuliahanIds = mahasiswa.ambilPerkuliahanDanParalel(Integer.valueOf(semester), semesterPendek);
        if (perkuliahanIds != null) {
            for (Long id : perkuliahanIds) {
                ais.database.model.Perkuliahan perkuliahan = (ais.database.model.Perkuliahan)
                        ais.common.ConstantValues.ambil(ais.database.model.Perkuliahan.class.getName(), id);
                if (perkuliahan == null) continue;
                ais.database.model.Matakuliah[] mk = Common.getMatakuliahApakahEkivalen(
                        perkuliahan.getMatakuliah(), mahasiswa.getNim(), false);
                ais.database.model.Matakuliah matakuliah = mk == null ? null : mk[0];
                if (matakuliah == null) continue;
                ais.database.model.Matakuliah asli = mk[1];

                java.util.List<ais.database.model.Pertemuan> pertemuans =
                        new java.util.ArrayList<ais.database.model.Pertemuan>();
                java.util.TreeMap<String, Long> peta = perkuliahan.ambilPertemuan();
                if (peta != null) {
                    for (Long pid : peta.values()) {
                        ais.database.model.Pertemuan p = (ais.database.model.Pertemuan)
                                ais.database.model.GeneralValueObject.ambilData(
                                        ais.database.model.Pertemuan.class, String.valueOf(pid));
                        if (p != null) pertemuans.add(p);
                    }
                }
                // Statistik kehadiran dihitung sumber yang sama dengan layar ZK;
                // indeks 4 berisi peta status (M hadir, S sakit, I izin, A alpa).
                Object[] jml = perkuliahan.ambilJumlahPertemuanStatistik(pertemuans, mahasiswa, null, true, true);
                java.util.Map<String, Integer> status = (java.util.Map<String, Integer>)
                        (jml == null || jml.length < 5 ? null : jml[4]);

                int m = ambil(status, "M"), s = ambil(status, "S"), i = ambil(status, "I"), a = ambil(status, "A");
                hadir += m; sakit += s; izin += i; alpa += a;
                totalSks += matakuliah.getSks();

                boolean ekivalen = asli != null && matakuliah.getId() != null
                        && !matakuliah.getId().equals(asli.getId());
                rows.put(new JSONObject()
                        .put("kode", ekivalen ? matakuliah.getKode() + " (" + asli.getKode() + ")"
                                : teks(matakuliah.getKode()))
                        .put("nama", ekivalen ? matakuliah.getNama() + " (" + asli.getNama() + ")"
                                : teks(matakuliah.getNama()))
                        .put("sks", matakuliah.getSks())
                        .put("hadir", m).put("sakit", s).put("izin", i).put("alpa", a)
                        .put("totalPertemuan", pertemuans.size()));
            }
        }
        o.put("rows", rows);
        o.put("totalSks", totalSks);
        o.put("totalHadir", hadir);
        o.put("totalSakit", sakit);
        o.put("totalIzin", izin);
        o.put("totalAlpa", alpa);
        return o;
    }

    private static int ambil(java.util.Map<String, Integer> status, String kunci) {
        if (status == null) return 0;
        Integer v = status.get(kunci);
        return v == null ? 0 : v.intValue();
    }

    // ----------------------------------------------------------- konfigurasi

    /**
     * Layar konfigurasi berbasis pasangan kunci-nilai.
     *
     * <p>Skema dan nilai bawaannya dibaca dari {@link SkemaKonfigurasi} — sumber
     * yang sama dengan layar ZK. Itu bukan kerapian belaka: membaca konfigurasi
     * akan MEMBUAT barisnya dengan bawaan yang disebut pemanggil bila belum ada,
     * sehingga bawaan yang berbeda antara dua layar akan menetapkan nilai yang
     * berbeda secara permanen.</p>
     */
    private static void konfigurasi(JSONObject j, HttpServletRequest request, String action,
            List<SkemaKonfigurasi.Butir> skema, String judul, boolean adaLampiran) throws Exception {
        if ("meta".equals(action)) {
            j.put("judul", judul);
            j.put("csrfHeader", NewUiCsrfUtil.HEADER);
            j.put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));
            JSONArray butir = new JSONArray();
            for (SkemaKonfigurasi.Butir b : skema) {
                butir.put(new JSONObject().put("kunci", b.kunci).put("label", b.label)
                        .put("tipe", b.tipe).put("kelompok", b.kelompok).put("baris", b.baris)
                        .put("bawaan", b.bawaan()).put("nilai", b.nilai()));
            }
            j.put("butir", butir);
            j.put("nilaiSaklarAktif", ais.database.model.Konfigurasi.AKTIF);
            j.put("nilaiSaklarTidakAktif", ais.database.model.Konfigurasi.TIDAK_AKTIF);
            if (adaLampiran) {
                // Berkas lampiran bukan pasangan kunci-nilai sehingga tidak dapat
                // disunting di sini. Daftarnya tetap disebutkan supaya pengguna
                // tidak mengira seluruh pengaturan layar lama sudah tampil.
                JSONArray lampiran = new JSONArray();
                for (String nama : SkemaKonfigurasi.LAMPIRAN_SEKOLAH) {
                    lampiran.put(nama);
                }
                j.put("lampiran", lampiran);
                j.put("catatanLampiran", "Berkas berikut berupa unggahan (logo, banner, tanda tangan, "
                        + "stempel, alur PDF) dan masih diatur di layar lama.");
            }
            return;
        }
        if ("save".equals(action)) {
            wajibMutasi(request);
            String kunci = text(request.getParameter("kunci"), "").trim();
            SkemaKonfigurasi.Butir butir = SkemaKonfigurasi.cari(skema, kunci);
            // Hanya kunci yang dideklarasikan skema layar INI yang boleh ditulis;
            // tanpa penjagaan ini kontrak menjadi jalan untuk mengubah konfigurasi
            // apa pun di seluruh aplikasi.
            if (butir == null) throw new IllegalArgumentException("Kunci konfigurasi tidak dikenal pada layar ini.");
            String nilai = text(request.getParameter("nilai"), "");
            if (SkemaKonfigurasi.SAKLAR.equals(butir.tipe)) {
                String aktif = ais.database.model.Konfigurasi.AKTIF;
                String tidak = ais.database.model.Konfigurasi.TIDAK_AKTIF;
                if (!aktif.equals(nilai) && !tidak.equals(nilai)) {
                    throw new IllegalArgumentException("Nilai saklar harus " + aktif + " atau " + tidak + ".");
                }
            }
            SkemaKonfigurasi.simpan(butir, nilai);
            j.put("kunci", butir.kunci);
            j.put("nilai", butir.nilai());
            j.put("pesan", "Pengaturan tersimpan.");
            return;
        }
        throw new IllegalArgumentException("Aksi tidak dikenal.");
    }

    // -------------------------------------------------------- belum tersedia

    /**
     * Layar yang sengaja belum dilayani native, beserta alasannya.
     *
     * <p>Menjawab dengan keterangan lebih berguna daripada halaman kosong:
     * operator tahu pekerjaan itu tetap harus dijalankan dari layar lama, dan
     * pengembang berikutnya tahu apa yang harus ada lebih dulu.</p>
     */
    private static void belumTersedia(JSONObject j, String mode, String action) throws Exception {
        if (!"meta".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");
        boolean impor = MODE_IMPOR_EPSBED.equals(mode);
        j.put("judul", impor ? "Import Data Dari EPSBED" : "Ekspor ke Feeder");
        j.put("tersediaNative", false);
        j.put("alasan", impor
                ? "Impor menerima letak folder di server yang diketik pengguna lalu membaca seluruh "
                        + "isinya. Membukanya sebagai kontrak berarti klien menentukan folder mana yang "
                        + "dibaca server, bukan sekadar mengimpor data."
                : "Ekspor menyambung ke server Feeder memakai alamat beserta kata sandi yang tersimpan "
                        + "di konfigurasi, dan berjalan lama sambil melaporkan kemajuannya.");
        JSONArray syarat = new JSONArray();
        if (impor) {
            syarat.put("Folder sumber dibatasi pada wilayah yang ditetapkan administrator, bukan diketik bebas.");
        } else {
            syarat.put("Kredensial Feeder tidak pernah melewati klien.");
        }
        syarat.put("Penyimpan status pekerjaan yang dapat ditanya ulang, menggantikan pelaporan kemajuan "
                + "lewat widget layar.");
        j.put("syaratSebelumNative", syarat);
        j.put("tindakan", "Jalankan dari layar lama sampai kedua syarat di atas tersedia.");
    }

    // ------------------------------------------------------------------ util

    private static void wajibMutasi(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new SecurityException("Mutasi hanya dilayani lewat POST.");
        }
        if (!NewUiCsrfUtil.isValid(request)) {
            throw new SecurityException("Token CSRF tidak sah. Muat ulang halaman.");
        }
    }

    private static int angka(String nilai, int min, int max, int bawaan, String pesan) {
        String v = text(nilai, "").trim();
        if (v.length() == 0) return bawaan;
        try {
            int n = Integer.parseInt(v);
            if (n < min || n > max) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(pesan);
        }
    }

    /** Angka dari data grid semester; 0 bila tidak terbaca. */
    private static int angkaAman(String nilai) {
        try {
            return Integer.parseInt(text(nilai, "0").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String teks(String s) {
        return s == null ? "" : s;
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value;
    }

    private static void fail(JSONObject json, String code, String message) {
        try {
            json.put("ok", false);
            json.put("code", code);
            json.put("message", message == null ? "" : message);
        } catch (Exception ignored) { }
    }

    private static void write(HttpServletResponse response, JSONObject json) throws Exception {
        response.getWriter().write(json.toString());
    }
}
