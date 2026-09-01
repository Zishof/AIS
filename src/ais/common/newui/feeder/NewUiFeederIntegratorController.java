package ais.common.newui.feeder;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.feeder.integrator.ekspor.EksporAjarDosenFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporAkmFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporAktifitasBimbinganFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporAktifitasKknFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporAktifitasPklFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporAktifitasSkripsiFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporHistoryFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporKelasFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporKelulusanFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporKrsFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporMahasiswaFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporMatakuliahFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporNilaiFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporNilaiTransferFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporPesertaDosenBimbinganFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporPesertaDosenKknFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporPesertaDosenPklFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporPesertaDosenSkripsiFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporPesertaMahasiswaBimbinganFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporPesertaMahasiswaKknFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporPesertaMahasiswaPklFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporPesertaMahasiswaSkripsiFeeder;
import ais.action.master.feeder.integrator.ekspor.EksporPrestasiMahasiswaFeeder;
import ais.action.master.feeder.integrator.ekspor.SaringanFeeder;
import ais.action.master.feeder.integrator.impor.HasilImpor;
import ais.action.master.feeder.integrator.impor.ImporAjarDosenFeeder;
import ais.action.master.feeder.integrator.impor.ImporKelasFeeder;
import ais.action.master.feeder.integrator.impor.ImporKelulusanFeeder;
import ais.action.master.feeder.integrator.impor.ImporKrsFeeder;
import ais.action.master.feeder.integrator.impor.ImporNilaiFeeder;
import ais.action.master.feeder.integrator.impor.ImporNilaiTransferFeeder;
import ais.action.master.feeder.integrator.impor.ImporPrestasiMahasiswaFeeder;
import ais.common.Common;
import ais.common.newui.NewUiUnggahRequest;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.common.newui.pekerjaan.PekerjaanRegistry;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Kurikulum;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;

/**
 * Kontrak native empat belas layar integrator Feeder.
 *
 * <h3>Koreksi atas anggapan sebelumnya</h3>
 * <p>Layar-layar ini pernah ditangguhkan dengan alasan "menyambung ke server
 * PDDIKTI memakai kredensial". <b>Anggapan itu keliru.</b> Tidak satu pun dari
 * keempat belasnya menyentuh server Feeder: panel <i>Download</i> menyusun
 * berkas .xlsx dari data lokal untuk diunggah operator sendiri ke Feeder, dan
 * panel <i>Upload</i> membaca berkas .xlsx kembali ke basis data lokal.
 * Kredensial Feeder hanya dipakai layar Ekspor ke Feeder yang berbeda.</p>
 *
 * <h3>Keberatan yang memang benar, dan cara mengatasinya</h3>
 * <p>Keberatan kedua tetap berlaku: penyusunan berkas berjalan lama di utas
 * latar dan melaporkan kemajuannya dengan memperbarui widget ZK. Sebuah API
 * tidak punya widget untuk dipegang. Itulah sebabnya
 * {@link PekerjaanRegistry} dibuat: kemajuan dicatat di sisi server sehingga
 * dapat ditanya ulang. Alurnya menjadi mulai &rarr; tanya kemajuan &rarr;
 * unduh berkas.</p>
 *
 * <h3>Susunan kolom tidak disalin</h3>
 * <p>Berkas ekspor tunduk pada susunan kolom yang ditetapkan PDDIKTI. Menulis
 * ulang pemetaannya untuk jalur native akan menciptakan dua daftar kolom yang
 * harus dijaga sama terhadap aturan pihak luar, dan yang menanggung akibat
 * kesalahannya adalah lembaga. Karena itu badan ekspor dipindahkan ke kelas
 * tanpa ZK yang dipakai bersama panel lama — bukan disalin.</p>
 *
 * <p><b>Cakupan saat ini.</b> Kedua puluh sembilan panel — dua puluh dua
 * <i>Download</i> dan tujuh <i>Upload</i> — sudah dipindahkan badannya dan dapat
 * dijalankan dari sini.</p>
 *
 * <p><b>Unduh dan unggah tidak diperlakukan sama.</b> Panel unduh menghasilkan
 * berkas; panel unggah <b>menulis ke basis data</b>. Karena itu aksinya
 * dipisahkan: {@code export_mulai} digolongkan aksi baca (setia pada layar ZK,
 * tempat penyiapan berkas terbuka bagi siapa pun yang boleh membaca menunya),
 * sedangkan {@code import_mulai} menuntut izin buat sekaligus ubah. Yang kedua
 * lebih ketat daripada layar lama, dan itu disengaja.</p>
 *
 * <p><b>Hitungan panel pernah salah dua kali.</b> Dua layar "peserta"
 * masing-masing punya empat tab (KKN, PKL, Skripsi, Bimbingan), bukan satu.
 * Daftar panel sebelumnya menyebut satu per layar dan dengan begitu menyembunyikan
 * enam tab yang ada pada layar lama. Kini seluruhnya disebut.</p>
 */
public final class NewUiFeederIntegratorController {

    /** Harus sama dengan awalan folder JSP sebelum {@code /uiux/}. */
    private static final String MODULE = "feeder";

    /** Panel penyiapan berkas ekspor Mahasiswa; panel pertama yang dipindahkan. */
    public static final String PANEL_MAHASISWA = "unduh_mahasiswa";

    /** Satu panel di dalam sebuah layar integrator. */
    /**
     * Kode saringan yang boleh muncul pada panel mana pun.
     *
     * <p>Daftarnya bukan karangan: tiap panel menyatakan saringannya sesuai
     * field {@code SaringanFeeder} yang benar-benar dibaca kelas ekspornya.
     * Menyatakan lebih banyak berarti layar menampilkan kotak yang tidak
     * berpengaruh apa pun — bentuk kebohongan yang tidak menimbulkan galat.</p>
     */
    static final String S_KELAS = "kelas";
    static final String S_NIM = "nim";
    static final String S_NAMA = "nama";
    static final String S_JURUSAN = "jurusan";
    static final String S_FAKULTAS = "fakultas";
    static final String S_PROGRAM = "program";
    static final String S_ANGKATAN = "angkatan";
    static final String S_STATUS = "status";
    static final String S_MASA = "masaPerkuliahan";
    static final String S_KURIKULUM = "kurikulum";
    static final String S_TAHUN_AKADEMIK = "tahunAkademik";
    static final String S_SEMESTER = "semester";
    static final String S_SEMESTER_KE = "semesterKe";
    static final String S_TAHUN_AJARAN = "tahunAjaran";
    static final String S_JENIS_SEMESTER = "jenisSemester";
    static final String S_KODE_MK = "kodeMatakuliah";
    static final String S_MULAI = "mulai";
    static final String S_SAMPAI = "sampai";
    static final String S_TELAH_DINILAI = "telahDinilai";
    static final String S_BELUM_DINILAI = "belumDinilai";
    static final String S_HITUNG_ULANG = "hitungUlang";
    /** Nama program sebagai teks; hanya panel unggah Ajar Dosen memakainya. */
    static final String S_NAMA_PROGRAM = "namaProgram";

    /** Saringan yang dipakai seluruh panel aktifitas dan peserta. */
    private static final String[] S_AKTIFITAS = {
            S_FAKULTAS, S_JURUSAN, S_PROGRAM, S_TAHUN_AKADEMIK, S_SEMESTER, S_KELAS };

    private static final class Panel {
        final String kode;
        final String label;
        final String jenis;
        final boolean native_;
        /** Kode saringan yang diterima panel ini; kosong untuk panel non-native. */
        final String[] saringan;

        Panel(String kode, String label, String jenis, boolean native_, String[] saringan) {
            this.kode = kode;
            this.label = label;
            this.jenis = jenis;
            this.native_ = native_;
            this.saringan = saringan == null ? new String[0] : saringan;
        }
    }

    /** Panel unduh yang badan ekspornya sudah pindah, jadi dapat dijalankan di sini. */
    private static Panel unduh(String kode, String label, String[] saringan) {
        return new Panel(kode, label, "unduh", true, saringan);
    }

    /**
     * Panel unggah yang badan pembacanya sudah pindah.
     *
     * <p>Saringannya hampir selalu kosong: berkas unggahan dibaca seluruhnya,
     * dan panel yang menyaring hanyalah Ajar Dosen.</p>
     */
    private static Panel unggah(String kode, String label, String[] saringan) {
        return new Panel(kode, label, "unggah", true, saringan);
    }

    private static Panel unggah(String kode, String label) {
        return unggah(kode, label, null);
    }

    private NewUiFeederIntegratorController() { }

    /** Panel milik tiap layar, urutannya sama dengan tab pada layar ZK. */
    private static List<Panel> panel(String mode) {
        List<Panel> p = new ArrayList<Panel>();
        if ("mahasiswa".equals(mode)) {
            p.add(unduh(PANEL_MAHASISWA, "Download Mahasiswa", new String[] {
                    S_FAKULTAS, S_JURUSAN, S_PROGRAM, S_ANGKATAN, S_STATUS,
                    S_KELAS, S_NIM, S_NAMA }));
        } else if ("matakuliah".equals(mode)) {
            p.add(unduh("unduh_matakuliah", "Download Matakuliah", new String[] {
                    S_FAKULTAS, S_JURUSAN, S_PROGRAM, S_KURIKULUM, S_SEMESTER, S_NAMA }));
        } else if ("ajar_dosen".equals(mode)) {
            p.add(unduh("unduh_ajar_dosen", "Download Ajar Dosen", new String[] {
                    S_FAKULTAS, S_JURUSAN, S_PROGRAM, S_TAHUN_AKADEMIK, S_SEMESTER,
                    S_MASA, S_ANGKATAN, S_KELAS }));
            p.add(unggah("unggah_ajar_dosen", "Upload Ajar Dosen",
                    new String[] { S_JURUSAN, S_NAMA_PROGRAM }));
        } else if ("kelas".equals(mode)) {
            p.add(unduh("unduh_kelas", "Download Kelas", new String[] {
                    S_FAKULTAS, S_JURUSAN, S_PROGRAM, S_TAHUN_AKADEMIK, S_SEMESTER,
                    S_MASA, S_KELAS }));
            p.add(unggah("unggah_kelas", "Upload Kelas"));
        } else if ("krs".equals(mode)) {
            p.add(unduh("unduh_krs", "Download KRS", new String[] {
                    S_FAKULTAS, S_JURUSAN, S_PROGRAM, S_ANGKATAN, S_STATUS, S_MASA,
                    S_SEMESTER_KE, S_TAHUN_AJARAN, S_JENIS_SEMESTER, S_KODE_MK,
                    S_KELAS, S_NIM, S_NAMA }));
            p.add(unggah("unggah_krs", "Upload KRS"));
        } else if ("nilai".equals(mode)) {
            p.add(unduh("unduh_nilai", "Download Nilai", new String[] {
                    S_FAKULTAS, S_JURUSAN, S_PROGRAM, S_ANGKATAN, S_STATUS, S_MASA,
                    S_SEMESTER_KE, S_TAHUN_AJARAN, S_JENIS_SEMESTER, S_KODE_MK,
                    S_KELAS, S_NIM, S_NAMA, S_TELAH_DINILAI, S_BELUM_DINILAI }));
            p.add(unggah("unggah_nilai", "Upload Nilai"));
        } else if ("akm".equals(mode)) {
            p.add(unduh("unduh_akm", "Download AKM", new String[] {
                    S_FAKULTAS, S_JURUSAN, S_PROGRAM, S_ANGKATAN, S_STATUS,
                    S_TAHUN_AKADEMIK, S_SEMESTER, S_KELAS, S_NIM, S_NAMA,
                    S_MULAI, S_SAMPAI, S_HITUNG_ULANG }));
        } else if ("kelulusan".equals(mode)) {
            p.add(unduh("unduh_kelulusan", "Download Kelulusan", new String[] {
                    S_FAKULTAS, S_JURUSAN, S_PROGRAM, S_TAHUN_AKADEMIK, S_SEMESTER, S_KELAS }));
            p.add(unggah("unggah_kelulusan", "Upload Kelulusan"));
        } else if ("prestasi_mahasiswa".equals(mode)) {
            p.add(unduh("unduh_prestasi", "Download Prestasi", new String[] {
                    S_FAKULTAS, S_JURUSAN, S_PROGRAM, S_TAHUN_AKADEMIK, S_SEMESTER, S_KELAS }));
            p.add(unggah("unggah_prestasi", "Upload Prestasi"));
        } else if ("history".equals(mode)) {
            p.add(unduh("unduh_history", "Download History", new String[] {
                    S_FAKULTAS, S_JURUSAN, S_PROGRAM, S_ANGKATAN, S_STATUS,
                    S_TAHUN_AKADEMIK, S_SEMESTER, S_KELAS, S_NIM, S_NAMA }));
        } else if ("nilai_transfer".equals(mode)) {
            p.add(unduh("unduh_nilai_transfer", "Download Nilai Transfer", new String[] {
                    S_FAKULTAS, S_JURUSAN, S_PROGRAM, S_ANGKATAN, S_STATUS,
                    S_KODE_MK, S_NIM, S_NAMA }));
            p.add(unggah("unggah_nilai_transfer", "Upload Nilai Transfer"));
        } else if ("aktifitas_mahasiswa".equals(mode)) {
            p.add(unduh("unduh_kkn", "Download KKN", S_AKTIFITAS));
            p.add(unduh("unduh_pkl", "Download PKL", S_AKTIFITAS));
            p.add(unduh("unduh_skripsi", "Download Skripsi", S_AKTIFITAS));
            p.add(unduh("unduh_bimbingan", "Download Bimbingan", S_AKTIFITAS));
        } else if ("aktifitas_mahasiswa_peserta".equals(mode)) {
            // Layar ini punya empat tab, bukan satu. Daftar sebelumnya hanya
            // menyebut satu panel dan karenanya menyembunyikan tiga tab yang ada
            // pada layar lama.
            p.add(unduh("unduh_peserta_mahasiswa_kkn", "Download Peserta Mahasiswa KKN", S_AKTIFITAS));
            p.add(unduh("unduh_peserta_mahasiswa_pkl", "Download Peserta Mahasiswa PKL", S_AKTIFITAS));
            p.add(unduh("unduh_peserta_mahasiswa_skripsi", "Download Peserta Mahasiswa Skripsi", S_AKTIFITAS));
            p.add(unduh("unduh_peserta_mahasiswa_bimbingan", "Download Peserta Mahasiswa Bimbingan", S_AKTIFITAS));
        } else if ("aktifitas_dosen_peserta".equals(mode)) {
            p.add(unduh("unduh_peserta_dosen_kkn", "Download Peserta Dosen KKN", S_AKTIFITAS));
            p.add(unduh("unduh_peserta_dosen_pkl", "Download Peserta Dosen PKL", S_AKTIFITAS));
            p.add(unduh("unduh_peserta_dosen_skripsi", "Download Peserta Dosen Skripsi", S_AKTIFITAS));
            p.add(unduh("unduh_peserta_dosen_bimbingan", "Download Peserta Dosen Bimbingan", S_AKTIFITAS));
        } else {
            throw new IllegalArgumentException("Mode integrator tidak dikenal.");
        }
        return p;
    }

    static String judul(String mode) {
        if ("mahasiswa".equals(mode)) return "Integrator Mahasiswa";
        if ("matakuliah".equals(mode)) return "Integrator Matakuliah";
        if ("ajar_dosen".equals(mode)) return "Integrator Ajar Dosen";
        if ("kelas".equals(mode)) return "Integrator Kelas";
        if ("krs".equals(mode)) return "Integrator KRS";
        if ("nilai".equals(mode)) return "Integrator Nilai";
        if ("akm".equals(mode)) return "Integrator AKM";
        if ("kelulusan".equals(mode)) return "Integrator Kelulusan";
        if ("prestasi_mahasiswa".equals(mode)) return "Integrator Prestasi Mahasiswa";
        if ("history".equals(mode)) return "Integrator History";
        if ("nilai_transfer".equals(mode)) return "Integrator Nilai Transfer";
        if ("aktifitas_mahasiswa".equals(mode)) return "Integrator Aktifitas Mahasiswa";
        if ("aktifitas_mahasiswa_peserta".equals(mode)) return "Integrator Peserta Mahasiswa";
        return "Integrator Peserta Dosen";
    }

    public static boolean modeDikenal(String mode) {
        try {
            panel(mode);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String mode, String pageKey) throws Exception {
        String action = text(request.getParameter("action"), "meta");
        // Unduhan mengirim berkas biner, bukan amplop JSON, sehingga jenis isinya
        // ditetapkan belakangan di dalam kirimBerkas.
        boolean unduhan = "export".equals(action);
        if (!unduhan) {
            response.setContentType("application/json; charset=UTF-8");
        }
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!modeDikenal(mode)) throw new IllegalArgumentException("Mode integrator tidak dikenal.");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            String pemilik = teks(user.getUserId());
            if (pemilik.length() == 0) throw new SecurityException("Sesi tanpa identitas pengguna.");

            if ("meta".equals(action)) meta(json, request, mode);
            else if ("lookup".equals(action)) lookup(json, request);
            // "export_mulai", bukan "create": penjaga menggolongkan awalan
            // export_ sebagai aksi baca, dan itulah yang setia pada layar ZK —
            // di sana panel penyiapan berkas terbuka bagi siapa pun yang boleh
            // MEMBACA menunya. Menuntut hak buat akan lebih ketat daripada
            // layar lama dan memblokir operator yang sah. POST beserta token
            // CSRF tetap diwajibkan karena aksi ini tetap mengerjakan sesuatu.
            else if ("export_mulai".equals(action)) mulai(json, request, mode, pemilik);
            // "import_mulai" SENGAJA bukan aksi baca. Panel unggah menulis ke
            // basis data; kedua penjaga memetakan awalan import_ ke izin buat
            // sekaligus ubah. Itu lebih ketat daripada layar ZK — di sana siapa
            // pun yang boleh membuka menunya boleh mengunggah — dan pengetatan
            // itu disengaja: yang dipertaruhkan bukan berkas keluaran melainkan
            // isi basis data.
            else if ("import_mulai".equals(action)) imporMulai(json, request, mode, pemilik, user);
            else if ("detail".equals(action)) kemajuan(json, request, pemilik);
            else if (unduhan) {
                kirimBerkas(request, response, pemilik);
                return;
            } else {
                throw new IllegalArgumentException("Aksi tidak dikenal.");
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
            fail(json, "INTERNAL_ERROR", "Gagal memproses permintaan integrator. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiFeederIntegratorController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    // ------------------------------------------------------------------ meta

    private static void meta(JSONObject j, HttpServletRequest request, String mode) throws Exception {
        j.put("judul", judul(mode));
        j.put("mode", mode);
        j.put("csrfHeader", NewUiCsrfUtil.HEADER);
        j.put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));
        JSONArray daftar = new JSONArray();
        for (Panel p : panel(mode)) {
            JSONObject o = new JSONObject().put("kode", p.kode).put("label", p.label)
                    .put("jenis", p.jenis).put("tersediaNative", p.native_);
            JSONArray saring = new JSONArray();
            for (String k : p.saringan) {
                saring.put(k);
            }
            o.put("saringan", saring);
            if ("unggah".equals(p.jenis)) {
                // Panel unggah menulis ke basis data. Klien perlu tahu itu untuk
                // meminta berkas dan menegaskan maksud pengguna, bukan
                // memperlakukannya sama dengan penyiapan berkas unduhan.
                o.put("menulisData", true);
                o.put("aksi", "import_mulai");
                o.put("bagianBerkas", "berkas");
                o.put("batasUkuran", NewUiUnggahRequest.BATAS_UKURAN);
            } else {
                o.put("menulisData", false);
                o.put("aksi", "export_mulai");
            }
            if (!p.native_) {
                o.put("alasan", "Panel ini belum tersedia native; jalankan dari layar lama.");
            }
            daftar.put(o);
        }
        j.put("panel", daftar);
        j.put("catatanCakupan", "Seluruh panel unduh dan unggah sudah dapat dijalankan dari sini.");
        // Alur pekerjaan panjang dinyatakan supaya klien tahu harus menanyakan
        // kemajuannya, bukan menunggu satu jawaban yang tidak akan datang.
        j.put("alurPekerjaan", new JSONArray().put("export_mulai").put("detail").put("export"));
        j.put("alurUnggah", new JSONArray().put("import_mulai").put("detail").put("export"));
    }

    /** Data acuan untuk saringan ekspor mahasiswa. */
    @SuppressWarnings("unchecked")
    private static void lookup(JSONObject j, HttpServletRequest r) throws Exception {
        String jenis = text(r.getParameter("jenis"), "");
        Session s = HibernateUtil.openSession();
        try {
            JSONArray arr = new JSONArray();
            if ("jurusan".equals(jenis)) {
                for (Object o : aktif(s.createCriteria(Jurusan.class)).addOrder(Order.asc("nama"))
                        .setMaxResults(300).list()) {
                    Jurusan x = (Jurusan) o;
                    arr.put(new JSONObject().put("id", x.getId()).put("nama", teks(x.getNama())));
                }
            } else if ("fakultas".equals(jenis)) {
                for (Object o : aktif(s.createCriteria(Fakultas.class)).addOrder(Order.asc("nama"))
                        .setMaxResults(300).list()) {
                    Fakultas x = (Fakultas) o;
                    arr.put(new JSONObject().put("id", x.getId()).put("nama", teks(x.getNama())));
                }
            } else if ("program".equals(jenis)) {
                for (Object o : aktif(s.createCriteria(Program.class)).addOrder(Order.asc("nama"))
                        .setMaxResults(300).list()) {
                    Program x = (Program) o;
                    arr.put(new JSONObject().put("id", x.getId()).put("nama", teks(x.getNama())));
                }
            } else if ("status".equals(jenis)) {
                for (Object o : aktif(s.createCriteria(StatusMahasiswa.class)).addOrder(Order.asc("nama"))
                        .setMaxResults(300).list()) {
                    StatusMahasiswa x = (StatusMahasiswa) o;
                    arr.put(new JSONObject().put("id", x.getId()).put("nama", teks(x.getNama())));
                }
            } else if ("masaPerkuliahan".equals(jenis)) {
                for (Object o : s.createCriteria(MasaPerkuliahan.class)
                        .addOrder(Order.desc("id")).setMaxResults(300).list()) {
                    MasaPerkuliahan x = (MasaPerkuliahan) o;
                    arr.put(new JSONObject().put("id", x.getId()).put("nama", teks(x.getNama())));
                }
            } else if ("kurikulum".equals(jenis)) {
                for (Object o : s.createCriteria(Kurikulum.class)
                        .addOrder(Order.desc("id")).setMaxResults(300).list()) {
                    Kurikulum x = (Kurikulum) o;
                    arr.put(new JSONObject().put("id", x.getId()).put("nama", teks(x.getNama())));
                }
            } else if ("tahunAkademik".equals(jenis) || "tahunAjaran".equals(jenis)) {
                // Sumbernya sama dengan combobox layar lama, supaya pilihan yang
                // tersedia di kedua jalur tidak pernah berbeda.
                for (String v : ais.common.CommonCurrentSessionHelper.tahunAngkatans) {
                    arr.put(new JSONObject().put("id", v).put("nama", v));
                }
                j.put("bawaan", teks(Common.getCurrentTahunAkademik()));
            } else if ("semester".equals(jenis) || "jenisSemester".equals(jenis)) {
                arr.put(new JSONObject().put("id", Perkuliahan.GANJIL).put("nama", Perkuliahan.GANJIL));
                arr.put(new JSONObject().put("id", Perkuliahan.GENAP).put("nama", Perkuliahan.GENAP));
                // Layar lama menyediakan "Semua" bernilai null; di sini nilai
                // kosong berarti hal yang sama.
                arr.put(new JSONObject().put("id", "").put("nama", "Semua"));
                j.put("bawaan", Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
            } else if ("semesterKe".equals(jenis)) {
                arr.put(new JSONObject().put("id", "").put("nama", "Semua"));
                for (int i = 1; i < 30; i++) {
                    arr.put(new JSONObject().put("id", String.valueOf(i)).put("nama", String.valueOf(i)));
                }
            } else {
                throw new IllegalArgumentException("Jenis acuan tidak dikenal.");
            }
            j.put("jenis", jenis);
            j.put("pilihan", arr);
        } finally {
            s.close();
        }
    }

    private static Criteria aktif(Criteria c) {
        return c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
    }

    // -------------------------------------------------------------- pekerjaan

    private static void mulai(JSONObject j, HttpServletRequest r, String mode, String pemilik)
            throws Exception {
        wajibMutasi(r);
        String kode = text(r.getParameter("panel"), "");
        Panel dipilih = null;
        for (Panel p : panel(mode)) {
            if (p.kode.equals(kode)) dipilih = p;
        }
        if (dipilih == null) throw new IllegalArgumentException("Panel tidak dikenal pada layar ini.");
        if (!dipilih.native_) {
            throw new IllegalArgumentException("Panel ini belum tersedia native; jalankan dari layar lama.");
        }

        final SaringanFeeder s = saringan(r, dipilih);

        final File tujuan = File.createTempFile("feeder_" + kode + "_", ".xlsx");
        String namaBerkas = "feeder_" + kode + "_"
                + Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate()) + ".xlsx";
        final String kodePanel = kode;
        String id = PekerjaanRegistry.mulai("feeder_" + kode, dipilih.label, pemilik, namaBerkas,
                new PekerjaanRegistry.Tugas() {
                    public File kerjakan(PekerjaanRegistry.Progres progres) throws Exception {
                        try {
                            susun(kodePanel, tujuan, s, progres);
                        } finally {
                            // Utas latar memakai session native tersendiri; wajib
                            // ditutup tepat sekali seperti pada panel ZK.
                            try { HibernateUtil.closeSession(); } catch (Exception ignored) { }
                        }
                        return tujuan;
                    }
                });
        j.put("pekerjaan", id);
        j.put("panel", kode);
        j.put("pesan", "Penyiapan berkas dimulai. Tanyakan kemajuannya secara berkala.");
    }

    /**
     * Mulai pembacaan berkas unggahan sebagai pekerjaan latar.
     *
     * <p>Berkasnya datang pada permintaan yang sama, sudah diurai oleh
     * {@link ais.common.newui.NewUiUnggahRequest} di gerbang native. Tidak ada
     * langkah "unggah dulu, mulai kemudian": langkah terpisah berarti berkas
     * berisi data mahasiswa menganggur di disk server tanpa pemilik dan tanpa
     * batas waktu, dan sebuah id yang dapat dicoba orang lain. Satu permintaan
     * menghapus seluruh persoalan itu.</p>
     */
    private static void imporMulai(JSONObject j, HttpServletRequest r, String mode,
            String pemilik, final Tbmuser pengguna) throws Exception {
        wajibMutasi(r);
        String kode = text(r.getParameter("panel"), "");
        Panel dipilih = null;
        for (Panel p : panel(mode)) {
            if (p.kode.equals(kode)) dipilih = p;
        }
        if (dipilih == null) throw new IllegalArgumentException("Panel tidak dikenal pada layar ini.");
        if (!"unggah".equals(dipilih.jenis)) {
            throw new IllegalArgumentException("Panel ini bukan panel unggah.");
        }
        if (!dipilih.native_) {
            throw new IllegalArgumentException("Panel ini belum tersedia native; jalankan dari layar lama.");
        }

        final File unggahan = berkasUnggahan(r);
        final SaringanFeeder s = saringan(r, dipilih);
        final File tujuan = File.createTempFile("feeder_" + kode + "_hasil_", ".xlsx");
        final String kodePanel = kode;
        String namaBerkas = "laporan_" + kode + "_"
                + Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate()) + ".txt";

        String id = PekerjaanRegistry.mulai("feeder_" + kode, dipilih.label, pemilik, namaBerkas,
                new PekerjaanRegistry.Tugas() {
                    public File kerjakan(PekerjaanRegistry.Progres progres) throws Exception {
                        try {
                            HasilImpor hasil = imporSusun(kodePanel, unggahan, tujuan, s, pengguna, progres);
                            // Ringkasan per baris dijadikan pesan akhir supaya
                            // hasil impor terbaca tanpa harus membuka laporannya.
                            // "Berhasil" saja tidak cukup: sebagian baris dapat
                            // gagal sementara sisanya tersimpan.
                            progres.lapor(100, hasil.ringkasan == null || hasil.ringkasan.length() == 0
                                    ? "Selesai memproses " + hasil.baris + " baris."
                                    : hasil.ringkasan);
                            return hasil.laporan;
                        } finally {
                            // Berkas unggahan memuat data pribadi; ia tidak punya
                            // alasan untuk tetap ada setelah dibaca.
                            try { if (unggahan != null) unggahan.delete(); } catch (Exception ignored) { }
                            try { HibernateUtil.closeSession(); } catch (Exception ignored) { }
                        }
                    }
                });
        j.put("pekerjaan", id);
        j.put("panel", kode);
        j.put("pesan", "Pembacaan berkas dimulai. Tanyakan kemajuannya secara berkala.");
    }

    /**
     * Ambil berkas unggahan dari permintaan yang sudah diurai gerbang native.
     *
     * <p>Ketiadaannya dijawab sebagai galat permintaan, bukan sebagai impor
     * kosong yang "berhasil".</p>
     */
    private static File berkasUnggahan(HttpServletRequest r) {
        HttpServletRequest cari = r;
        // Permintaan sudah diteruskan beberapa lapis sejak diurai, jadi
        // pembungkusnya dicari menembus lapisan-lapisan itu.
        while (cari instanceof javax.servlet.http.HttpServletRequestWrapper) {
            if (cari instanceof NewUiUnggahRequest) {
                File f = ((NewUiUnggahRequest) cari).getBerkas();
                if (f != null && f.exists()) return f;
                break;
            }
            javax.servlet.ServletRequest dalam =
                    ((javax.servlet.http.HttpServletRequestWrapper) cari).getRequest();
            if (!(dalam instanceof HttpServletRequest)) break;
            cari = (HttpServletRequest) dalam;
        }
        throw new IllegalArgumentException(
                "Sertakan berkas .xlsx pada permintaan ini (bagian bernama \"berkas\").");
    }

    /**
     * Kirim pembacaan berkas ke kelas impor milik panel.
     *
     * <p>Ditulis satu per satu dengan alasan yang sama seperti pada penyusun
     * ekspor, dan lebih penting lagi di sini: salah pemetaan berarti berkas
     * dibaca dengan aturan panel lain lalu <b>disimpan</b> ke basis data.</p>
     */
    private static HasilImpor imporSusun(String kode, File unggahan, File tujuan,
            SaringanFeeder s, Tbmuser pengguna, PekerjaanRegistry.Progres progres) throws Exception {
        if ("unggah_ajar_dosen".equals(kode)) {
            return ImporAjarDosenFeeder.proses(unggahan, tujuan, s, pengguna, progres);
        }
        if ("unggah_kelas".equals(kode)) {
            return ImporKelasFeeder.proses(unggahan, tujuan, s, pengguna, progres);
        }
        if ("unggah_krs".equals(kode)) {
            return ImporKrsFeeder.proses(unggahan, tujuan, s, pengguna, progres);
        }
        if ("unggah_nilai".equals(kode)) {
            return ImporNilaiFeeder.proses(unggahan, tujuan, s, pengguna, progres);
        }
        if ("unggah_nilai_transfer".equals(kode)) {
            return ImporNilaiTransferFeeder.proses(unggahan, tujuan, s, pengguna, progres);
        }
        if ("unggah_kelulusan".equals(kode)) {
            return ImporKelulusanFeeder.proses(unggahan, tujuan, s, pengguna, progres);
        }
        if ("unggah_prestasi".equals(kode)) {
            return ImporPrestasiMahasiswaFeeder.proses(unggahan, tujuan, s, pengguna, progres);
        }
        throw new IllegalArgumentException("Panel belum punya pembaca berkas: " + kode);
    }

    /**
     * Susun saringan dari parameter permintaan, hanya untuk field yang panel ini
     * memang menyatakannya.
     *
     * <p>Pembatasan itu disengaja: kalau semua field diisi tanpa memandang panel,
     * klien yang salah kirim akan tampak "bekerja" sampai seseorang menyadari
     * berkasnya tersaring menurut sesuatu yang tidak pernah muncul di layar.</p>
     */
    private static SaringanFeeder saringan(HttpServletRequest r, Panel panel) throws Exception {
        Set<String> boleh = new HashSet<String>(Arrays.asList(panel.saringan));
        SaringanFeeder s = new SaringanFeeder();
        if (boleh.contains(S_KELAS)) s.kelas = text(r.getParameter(S_KELAS), "");
        if (boleh.contains(S_NIM)) s.nim = text(r.getParameter(S_NIM), "");
        if (boleh.contains(S_NAMA)) s.nama = text(r.getParameter(S_NAMA), "");
        if (boleh.contains(S_TAHUN_AKADEMIK)) s.tahunAkademik = text(r.getParameter(S_TAHUN_AKADEMIK), "");
        if (boleh.contains(S_SEMESTER)) s.semester = text(r.getParameter(S_SEMESTER), "");
        if (boleh.contains(S_TAHUN_AJARAN)) s.tahunAjaran = text(r.getParameter(S_TAHUN_AJARAN), "");
        if (boleh.contains(S_JENIS_SEMESTER)) s.jenisSemester = text(r.getParameter(S_JENIS_SEMESTER), "");
        if (boleh.contains(S_KODE_MK)) s.kodeMatakuliah = text(r.getParameter(S_KODE_MK), "");
        if (boleh.contains(S_NAMA_PROGRAM)) s.namaProgram = text(r.getParameter(S_NAMA_PROGRAM), "");
        if (boleh.contains(S_ANGKATAN)) s.angkatan = angka(r.getParameter(S_ANGKATAN), "Angkatan");
        if (boleh.contains(S_SEMESTER_KE)) s.semesterKe = angka(r.getParameter(S_SEMESTER_KE), "Semester");
        if (boleh.contains(S_MULAI)) s.mulai = angka(r.getParameter(S_MULAI), "Mulai");
        if (boleh.contains(S_SAMPAI)) s.sampai = angka(r.getParameter(S_SAMPAI), "Sampai");
        if (boleh.contains(S_TELAH_DINILAI)) s.telahDinilai = benar(r.getParameter(S_TELAH_DINILAI));
        if (boleh.contains(S_BELUM_DINILAI)) s.belumDinilai = benar(r.getParameter(S_BELUM_DINILAI));
        if (boleh.contains(S_HITUNG_ULANG)) s.hitungUlang = benar(r.getParameter(S_HITUNG_ULANG));

        Session sesi = HibernateUtil.openSession();
        try {
            if (boleh.contains(S_JURUSAN)) {
                s.jurusan = (Jurusan) muat(sesi, Jurusan.class, r.getParameter("jurusanId"));
            }
            if (boleh.contains(S_FAKULTAS)) {
                s.fakultas = (Fakultas) muat(sesi, Fakultas.class, r.getParameter("fakultasId"));
            }
            if (boleh.contains(S_PROGRAM)) {
                s.program = (Program) muat(sesi, Program.class, r.getParameter("programId"));
            }
            if (boleh.contains(S_STATUS)) {
                s.status = (StatusMahasiswa) muat(sesi, StatusMahasiswa.class, r.getParameter("statusId"));
            }
            if (boleh.contains(S_MASA)) {
                s.masaPerkuliahan = (MasaPerkuliahan) muat(sesi, MasaPerkuliahan.class,
                        r.getParameter("masaPerkuliahanId"));
            }
            if (boleh.contains(S_KURIKULUM)) {
                s.kurikulum = (Kurikulum) muat(sesi, Kurikulum.class, r.getParameter("kurikulumId"));
            }
        } finally {
            sesi.close();
        }
        s.rapikan();
        return s;
    }

    /**
     * Kirim penyusunan berkas ke kelas ekspor milik panel.
     *
     * <p>Pemetaan ini ditulis satu per satu dan bukan lewat refleksi nama kelas:
     * kesalahan pemetaan di sini menghasilkan berkas dengan susunan kolom milik
     * panel lain — diterima aplikasi, ditolak PDDIKTI. Sebutan eksplisit membuat
     * kekeliruannya terlihat saat dibaca, dan panel yang belum terdaftar gagal
     * dengan jelas alih-alih diam-diam memakai penyusun yang salah.</p>
     */
    private static void susun(String kode, File tujuan, SaringanFeeder s,
            PekerjaanRegistry.Progres progres) throws Exception {
        if (PANEL_MAHASISWA.equals(kode)) EksporMahasiswaFeeder.tulis(tujuan, s, progres);
        else if ("unduh_matakuliah".equals(kode)) EksporMatakuliahFeeder.tulis(tujuan, s, progres);
        else if ("unduh_ajar_dosen".equals(kode)) EksporAjarDosenFeeder.tulis(tujuan, s, progres);
        else if ("unduh_kelas".equals(kode)) EksporKelasFeeder.tulis(tujuan, s, progres);
        else if ("unduh_krs".equals(kode)) EksporKrsFeeder.tulis(tujuan, s, progres);
        else if ("unduh_nilai".equals(kode)) EksporNilaiFeeder.tulis(tujuan, s, progres);
        else if ("unduh_akm".equals(kode)) EksporAkmFeeder.tulis(tujuan, s, progres);
        else if ("unduh_kelulusan".equals(kode)) EksporKelulusanFeeder.tulis(tujuan, s, progres);
        else if ("unduh_prestasi".equals(kode)) EksporPrestasiMahasiswaFeeder.tulis(tujuan, s, progres);
        else if ("unduh_history".equals(kode)) EksporHistoryFeeder.tulis(tujuan, s, progres);
        else if ("unduh_nilai_transfer".equals(kode)) EksporNilaiTransferFeeder.tulis(tujuan, s, progres);
        else if ("unduh_kkn".equals(kode)) EksporAktifitasKknFeeder.tulis(tujuan, s, progres);
        else if ("unduh_pkl".equals(kode)) EksporAktifitasPklFeeder.tulis(tujuan, s, progres);
        else if ("unduh_skripsi".equals(kode)) EksporAktifitasSkripsiFeeder.tulis(tujuan, s, progres);
        else if ("unduh_bimbingan".equals(kode)) EksporAktifitasBimbinganFeeder.tulis(tujuan, s, progres);
        else if ("unduh_peserta_mahasiswa_kkn".equals(kode))
            EksporPesertaMahasiswaKknFeeder.tulis(tujuan, s, progres);
        else if ("unduh_peserta_mahasiswa_pkl".equals(kode))
            EksporPesertaMahasiswaPklFeeder.tulis(tujuan, s, progres);
        else if ("unduh_peserta_mahasiswa_skripsi".equals(kode))
            EksporPesertaMahasiswaSkripsiFeeder.tulis(tujuan, s, progres);
        else if ("unduh_peserta_mahasiswa_bimbingan".equals(kode))
            EksporPesertaMahasiswaBimbinganFeeder.tulis(tujuan, s, progres);
        else if ("unduh_peserta_dosen_kkn".equals(kode))
            EksporPesertaDosenKknFeeder.tulis(tujuan, s, progres);
        else if ("unduh_peserta_dosen_pkl".equals(kode))
            EksporPesertaDosenPklFeeder.tulis(tujuan, s, progres);
        else if ("unduh_peserta_dosen_skripsi".equals(kode))
            EksporPesertaDosenSkripsiFeeder.tulis(tujuan, s, progres);
        else if ("unduh_peserta_dosen_bimbingan".equals(kode))
            EksporPesertaDosenBimbinganFeeder.tulis(tujuan, s, progres);
        else throw new IllegalArgumentException("Panel belum punya penyusun berkas: " + kode);
    }

    private static void kemajuan(JSONObject j, HttpServletRequest r, String pemilik) throws Exception {
        String id = text(r.getParameter("pekerjaan"), "");
        PekerjaanRegistry.Pekerjaan p = PekerjaanRegistry.lihat(id, pemilik);
        if (p == null) throw new IllegalArgumentException("Pekerjaan tidak ditemukan.");
        j.put("pekerjaan", p.id);
        j.put("judul", p.judul);
        j.put("status", p.getStatus());
        j.put("persen", p.getPersen());
        j.put("pesan", p.getPesan());
        j.put("tuntas", p.tuntas());
        j.put("siapDiunduh", PekerjaanRegistry.SELESAI.equals(p.getStatus()) && p.getBerkas() != null);
        j.put("namaBerkas", p.getNamaBerkas());
    }

    /**
     * Kirim berkas hasil sebagai unduhan biner.
     *
     * <p>Sengaja bukan base64 di dalam JSON: berkas ekspor bisa berukuran puluhan
     * megabita, dan base64 menambah sepertiga ukurannya sekaligus menahan
     * seluruh isi di memori pada kedua sisi.</p>
     */
    private static void kirimBerkas(HttpServletRequest r, HttpServletResponse response, String pemilik)
            throws Exception {
        String id = text(r.getParameter("pekerjaan"), "");
        PekerjaanRegistry.Pekerjaan p = PekerjaanRegistry.lihat(id, pemilik);
        if (p == null || !PekerjaanRegistry.SELESAI.equals(p.getStatus()) || p.getBerkas() == null
                || !p.getBerkas().exists()) {
            response.setContentType("application/json; charset=UTF-8");
            response.setStatus(422);
            JSONObject j = new JSONObject();
            fail(j, "VALIDATION_FAILED", "Berkas belum siap atau pekerjaan tidak ditemukan.");
            write(response, j);
            return;
        }
        File f = p.getBerkas();
        String nama = p.getNamaBerkas().length() == 0 ? "feeder.xlsx" : p.getNamaBerkas();
        // Pekerjaan unduh menghasilkan .xlsx, pekerjaan unggah menghasilkan
        // laporan teks. Menyebut keduanya spreadsheet membuat berkas laporan
        // dibuka aplikasi yang salah oleh klien yang mempercayai jenis isi.
        response.setContentType(nama.toLowerCase().endsWith(".txt")
                ? "text/plain; charset=UTF-8"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + nama + "\"");
        response.setContentLength((int) f.length());
        FileInputStream in = new FileInputStream(f);
        try {
            OutputStream out = response.getOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
        } finally {
            in.close();
        }
    }

    // ------------------------------------------------------------------ util

    private static Object muat(Session s, Class<?> kelas, String id) {
        String v = text(id, "").trim();
        if (v.length() == 0) return null;
        try {
            long l = Long.parseLong(v);
            if (l <= 0) return null;
            return s.get(kelas, Long.valueOf(l));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nilai saringan tidak sah.");
        }
    }

    /** Angka pilihan; kosong berarti tanpa saringan, bukan nol. */
    private static Integer angka(String nilai, String label) {
        String v = text(nilai, "").trim();
        if (v.length() == 0) return null;
        try {
            return Integer.valueOf(Integer.parseInt(v));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " harus berupa angka.");
        }
    }

    /**
     * Saklar; hanya "true"/"1" yang berarti menyala.
     *
     * <p>Nilai lain diperlakukan sebagai padam, mengikuti kotak centang layar
     * lama yang bawaannya tidak tercentang.</p>
     */
    private static boolean benar(String nilai) {
        String v = text(nilai, "").trim();
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    private static void wajibMutasi(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new SecurityException("Mutasi hanya dilayani lewat POST.");
        }
        if (!NewUiCsrfUtil.isValid(request)) {
            throw new SecurityException("Token CSRF tidak sah. Muat ulang halaman.");
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
