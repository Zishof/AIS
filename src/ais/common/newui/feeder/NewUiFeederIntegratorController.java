package ais.common.newui.feeder;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.feeder.integrator.ekspor.EksporMahasiswaFeeder;
import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.common.newui.pekerjaan.PekerjaanRegistry;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
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
 * <p><b>Cakupan saat ini.</b> Baru ekspor Mahasiswa yang badannya sudah
 * dipindahkan sehingga dapat dijalankan dari sini. Panel lain diumumkan apa
 * adanya sebagai belum tersedia; klien menampilkannya, bukan menyembunyikannya,
 * supaya operator tahu pekerjaan itu masih dijalankan dari layar lama.</p>
 */
public final class NewUiFeederIntegratorController {

    /** Harus sama dengan awalan folder JSP sebelum {@code /uiux/}. */
    private static final String MODULE = "feeder";

    /** Panel penyiapan berkas ekspor Mahasiswa; satu-satunya yang sudah native. */
    public static final String PANEL_MAHASISWA = "unduh_mahasiswa";

    /** Satu panel di dalam sebuah layar integrator. */
    private static final class Panel {
        final String kode;
        final String label;
        final String jenis;
        final boolean native_;

        Panel(String kode, String label, String jenis, boolean native_) {
            this.kode = kode;
            this.label = label;
            this.jenis = jenis;
            this.native_ = native_;
        }
    }

    private NewUiFeederIntegratorController() { }

    /** Panel milik tiap layar, urutannya sama dengan tab pada layar ZK. */
    private static List<Panel> panel(String mode) {
        List<Panel> p = new ArrayList<Panel>();
        if ("mahasiswa".equals(mode)) {
            p.add(new Panel(PANEL_MAHASISWA, "Download Mahasiswa", "unduh", true));
        } else if ("matakuliah".equals(mode)) {
            p.add(new Panel("unduh_matakuliah", "Download Matakuliah", "unduh", false));
        } else if ("ajar_dosen".equals(mode)) {
            p.add(new Panel("unduh_ajar_dosen", "Download Ajar Dosen", "unduh", false));
            p.add(new Panel("unggah_ajar_dosen", "Upload Ajar Dosen", "unggah", false));
        } else if ("kelas".equals(mode)) {
            p.add(new Panel("unduh_kelas", "Download Kelas", "unduh", false));
            p.add(new Panel("unggah_kelas", "Upload Kelas", "unggah", false));
        } else if ("krs".equals(mode)) {
            p.add(new Panel("unduh_krs", "Download KRS", "unduh", false));
            p.add(new Panel("unggah_krs", "Upload KRS", "unggah", false));
        } else if ("nilai".equals(mode)) {
            p.add(new Panel("unduh_nilai", "Download Nilai", "unduh", false));
            p.add(new Panel("unggah_nilai", "Upload Nilai", "unggah", false));
        } else if ("akm".equals(mode)) {
            p.add(new Panel("unduh_akm", "Download AKM", "unduh", false));
        } else if ("kelulusan".equals(mode)) {
            p.add(new Panel("unduh_kelulusan", "Download Kelulusan", "unduh", false));
            p.add(new Panel("unggah_kelulusan", "Upload Kelulusan", "unggah", false));
        } else if ("prestasi_mahasiswa".equals(mode)) {
            p.add(new Panel("unduh_prestasi", "Download Prestasi", "unduh", false));
            p.add(new Panel("unggah_prestasi", "Upload Prestasi", "unggah", false));
        } else if ("history".equals(mode)) {
            p.add(new Panel("unduh_history", "Download History", "unduh", false));
        } else if ("nilai_transfer".equals(mode)) {
            p.add(new Panel("unduh_nilai_transfer", "Download Nilai Transfer", "unduh", false));
            p.add(new Panel("unggah_nilai_transfer", "Upload Nilai Transfer", "unggah", false));
        } else if ("aktifitas_mahasiswa".equals(mode)) {
            p.add(new Panel("unduh_kkn", "Download KKN", "unduh", false));
            p.add(new Panel("unduh_pkl", "Download PKL", "unduh", false));
            p.add(new Panel("unduh_skripsi", "Download Skripsi", "unduh", false));
            p.add(new Panel("unduh_bimbingan", "Download Bimbingan", "unduh", false));
        } else if ("aktifitas_mahasiswa_peserta".equals(mode)) {
            p.add(new Panel("unduh_peserta_mahasiswa", "Download Peserta Mahasiswa", "unduh", false));
        } else if ("aktifitas_dosen_peserta".equals(mode)) {
            p.add(new Panel("unduh_peserta_dosen", "Download Peserta Dosen", "unduh", false));
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
            if (!p.native_) {
                o.put("alasan", "unduh".equals(p.jenis)
                        ? "Susunan kolom berkas ini masih menyatu dengan layar lama. Memindahkannya "
                                + "dilakukan satu per satu agar pemetaan kolom PDDIKTI tidak berubah."
                        : "Pembacaan berkas unggahan menuntut jalur unggah berkas yang belum ada pada "
                                + "kontrak native.");
            }
            daftar.put(o);
        }
        j.put("panel", daftar);
        j.put("catatanCakupan", "Panel yang belum tersedia native tetap dapat dijalankan dari layar lama.");
        // Alur pekerjaan panjang dinyatakan supaya klien tahu harus menanyakan
        // kemajuannya, bukan menunggu satu jawaban yang tidak akan datang.
        j.put("alurPekerjaan", new JSONArray().put("export_mulai").put("detail").put("export"));
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

        final EksporMahasiswaFeeder.Saringan s = new EksporMahasiswaFeeder.Saringan();
        s.kelas = text(r.getParameter("kelas"), "");
        s.nim = text(r.getParameter("nim"), "");
        s.nama = text(r.getParameter("nama"), "");
        Session sesi = HibernateUtil.openSession();
        try {
            s.jurusan = (Jurusan) muat(sesi, Jurusan.class, r.getParameter("jurusanId"));
            s.fakultas = (Fakultas) muat(sesi, Fakultas.class, r.getParameter("fakultasId"));
            s.program = (Program) muat(sesi, Program.class, r.getParameter("programId"));
            s.status = (StatusMahasiswa) muat(sesi, StatusMahasiswa.class, r.getParameter("statusId"));
        } finally {
            sesi.close();
        }
        String angkatan = text(r.getParameter("angkatan"), "").trim();
        if (angkatan.length() > 0) {
            try {
                s.angkatan = Integer.valueOf(Integer.parseInt(angkatan));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Angkatan harus berupa angka.");
            }
        }

        final File tujuan = File.createTempFile("feeder_mahasiswa_", ".xlsx");
        String namaBerkas = "feeder_mahasiswa_"
                + Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate()) + ".xlsx";
        String id = PekerjaanRegistry.mulai("feeder_" + kode, dipilih.label, pemilik, namaBerkas,
                new PekerjaanRegistry.Tugas() {
                    public File kerjakan(PekerjaanRegistry.Progres progres) throws Exception {
                        try {
                            EksporMahasiswaFeeder.tulis(tujuan, s, progres);
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
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\""
                + (p.getNamaBerkas().length() == 0 ? "feeder.xlsx" : p.getNamaBerkas()) + "\"");
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
