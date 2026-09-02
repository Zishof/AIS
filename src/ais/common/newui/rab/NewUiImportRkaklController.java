package ais.common.newui.rab;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.common.newui.NewUiUnggahRequest;
import ais.common.newui.pekerjaan.PekerjaanRegistry;
import ais.database.model.Tbmuser;

/**
 * Import Data RKAKL: menerima unggahan, bukan path direktori server.
 *
 * <h3>Mengapa bentuknya berubah dari layar lama</h3>
 * <p>Layar ZK meminta pengguna mengetik <b>path direktori di server</b> lalu
 * membaca seluruh isinya. Dua hal membuatnya tidak dapat dipindahkan apa
 * adanya:</p>
 * <ul>
 *   <li>menerima path server dari pengguna adalah permukaan baca berkas
 *       sembarang — apa pun yang dapat dibaca proses Tomcat dapat ditunjuk;</li>
 *   <li>kemajuannya dilaporkan ke widget ZK yang dipegang utas latar, sehingga
 *       tidak ada catatan di sisi server yang dapat ditanya ulang oleh API.</li>
 * </ul>
 *
 * <p>Keduanya diganti: berkas <b>diunggah</b>, dan pekerjaannya dijalankan lewat
 * {@link PekerjaanRegistry} — catatan status yang <b>sudah ada</b> di basis kode
 * ini, dibuat untuk keluarga integrator Feeder dengan alasan yang persis sama.
 * Membuat penyimpan kedua akan berarti dua catatan status yang harus dijaga
 * sama.</p>
 *
 * <h3>Wadahnya .zip, bukan .xlsx</h3>
 * <p>Impor ini membaca berkas <b>DBF</b> berakhiran {@code .keu}
 * ({@code com.linuxense.javadbf.DBFReader}), dan membacanya <b>banyak sekaligus</b>
 * dari satu direktori. Satu berkas spreadsheet tidak dapat menggantikannya, jadi
 * yang diterima adalah satu {@code .zip} berisi berkas-berkas tersebut, yang
 * diekstrak ke direktori sementara milik pekerjaan itu sendiri.</p>
 *
 * <h3>Ekstraksi yang tidak boleh keluar kandang</h3>
 * <p>Entri arsip yang jalurnya menunjuk ke luar direktori tujuan
 * (<i>zip slip</i>) ditolak, dan <b>seluruh arsipnya</b> ikut ditolak — bukan
 * dilewati diam-diam. Arsip yang memuatnya tidak pernah benar, dan mengimpor
 * sisanya berarti memasukkan sebagian data sambil menganggapnya lengkap. Jumlah
 * entri dan total ukuran hasil ekstraksi juga dibatasi.</p>
 *
 * <h3>Kemajuan dilaporkan kasar, dan itu jujur</h3>
 * <p>{@code ImportFromRKAKLHelper.importData} melaporkan kemajuannya ke komponen
 * ZK yang di sini bernilai {@code null}, sehingga tidak ada kait untuk mengetahui
 * berkas keberapa yang sedang diproses. Yang dilaporkan karena itu hanya dua
 * titik: mulai dan selesai. Menggerakkan bilah kemajuan dengan angka yang
 * dikarang lebih buruk daripada menyatakan "sedang berjalan" — yang pertama
 * membuat orang menunggu dengan keyakinan palsu.</p>
 */
public final class NewUiImportRkaklController {

    private static final String MODULE = "rab";

    /** Jenis pekerjaan pada catatan status. */
    static final String JENIS = "import_rkakl";

    /** Batas jumlah entri dalam arsip. */
    static final int MAKS_ENTRI = 2000;

    /** Batas total ukuran hasil ekstraksi. */
    static final long MAKS_TOTAL = 256L * 1024L * 1024L;

    private NewUiImportRkaklController() { }

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
                    gagal(json, "METHOD_NOT_ALLOWED", "Gunakan HTTP POST untuk memulai impor.");
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

            if ("meta".equals(action)) meta(json, request, user);
            else if ("detail".equals(action)) status(json, request, user);
            else mulai(json, request, user);
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
            try { ais.common.ErrorAuditUtil.record(e, "NewUiImportRkaklController"); }
            catch (Exception diabaikan) { }
        }
        tulis(response, json);
    }

    static boolean aksiDikenal(String action) {
        return "meta".equals(action) || "detail".equals(action) || "create".equals(action);
    }

    /** Hanya memulai impor yang mengubah data. */
    static boolean mengubah(String action) {
        return "create".equals(action);
    }

    /** Identitas pemilik pekerjaan; {@link PekerjaanRegistry} memakai String. */
    private static String pemilik(Tbmuser user) {
        return user == null || user.getId() == null ? "" : String.valueOf(user.getId());
    }

    // ------------------------------------------------------------------ meta

    private static void meta(JSONObject j, HttpServletRequest request, Tbmuser user)
            throws JSONException {
        j.put("judul", "Import Data RKAKL");
        j.put("formatDiterima", ".zip");
        j.put("catatanFormat", "Unggah satu berkas .zip berisi berkas RKAKL berakhiran .keu. "
                + "Berkas selain itu di dalam arsip diabaikan.");
        j.put("batasEntri", MAKS_ENTRI);
        j.put("batasUkuranUnggahan", NewUiUnggahRequest.BATAS_UKURAN);
        j.put("csrfHeader", NewUiCsrfUtil.HEADER);
        j.put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));

        JSONArray pekerjaan = new JSONArray();
        List<PekerjaanRegistry.Pekerjaan> milik = PekerjaanRegistry.milik(pemilik(user));
        for (int i = 0; i < milik.size() && i < 20; i++) {
            pekerjaan.put(json(milik.get(i)));
        }
        j.put("pekerjaan", pekerjaan);
    }

    // ---------------------------------------------------------------- status

    private static void status(JSONObject j, HttpServletRequest request, Tbmuser user)
            throws JSONException {
        String id = teks(request.getParameter("jobId"), "");
        if (id.length() == 0) throw new IllegalArgumentException("jobId wajib diisi.");
        PekerjaanRegistry.Pekerjaan p = PekerjaanRegistry.lihat(id, pemilik(user));
        if (p == null) {
            // Tidak dibedakan dari "bukan milik Anda": pembedaan itu sendiri
            // sudah memberi tahu bahwa id tersebut ada.
            throw new IllegalArgumentException("Pekerjaan tidak ditemukan.");
        }
        j.put("pekerjaan", json(p));
    }

    private static JSONObject json(PekerjaanRegistry.Pekerjaan p) throws JSONException {
        return new JSONObject()
                .put("status", p.getStatus())
                .put("persen", p.getPersen())
                .put("pesan", p.getPesan())
                .put("selesai", p.getSelesai())
                .put("tuntas", p.tuntas());
    }

    // ----------------------------------------------------------------- mulai

    private static void mulai(JSONObject j, HttpServletRequest request, Tbmuser user)
            throws Exception {
        if (!(request instanceof NewUiUnggahRequest)) {
            throw new IllegalArgumentException(
                    "Impor harus dikirim sebagai unggahan berkas (multipart/form-data).");
        }
        NewUiUnggahRequest unggah = (NewUiUnggahRequest) request;
        File arsip = unggah.getBerkas();
        String nama = unggah.getNamaBerkas() == null ? "" : unggah.getNamaBerkas();
        if (arsip == null) throw new IllegalArgumentException("Berkas belum dipilih.");
        if (!nama.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Berkas impor harus berformat .zip berisi "
                    + "berkas RKAKL berakhiran .keu.");
        }

        File tujuan = new File(Common.REAL_PATH + "/tmp/rkakl-"
                + System.currentTimeMillis() + "-" + pemilik(user));
        if (!tujuan.mkdirs() && !tujuan.isDirectory()) {
            throw new IllegalStateException("Direktori sementara tidak dapat dibuat.");
        }

        int berkasKeu;
        try {
            berkasKeu = ekstrak(arsip, tujuan);
        } catch (Exception e) {
            // Arsip ditolak: jangan tinggalkan hasil ekstraksi separuh jalan.
            hapusPohon(tujuan);
            throw e;
        }
        if (berkasKeu == 0) {
            hapusPohon(tujuan);
            throw new IllegalArgumentException(
                    "Arsip tidak memuat satu pun berkas berakhiran .keu.");
        }

        final String path = tujuan.getAbsolutePath();
        final int jumlah = berkasKeu;
        String id = PekerjaanRegistry.mulai(JENIS,
                "Import Data RKAKL (" + jumlah + " berkas)", pemilik(user), null,
                new PekerjaanRegistry.Tugas() {
                    public File kerjakan(PekerjaanRegistry.Progres progres) throws Exception {
                        try {
                            progres.lapor(0, "Mengimpor " + jumlah + " berkas RKAKL.");
                            ais.action.master.helper.impor.ImportFromRKAKLHelper
                                    .importData(path, null, null, null);
                            progres.lapor(100, "Impor selesai: " + jumlah + " berkas diproses.");
                            // Impor ini tidak menghasilkan berkas untuk diunduh.
                            return null;
                        } finally {
                            // Selalu dibersihkan, termasuk saat gagal: isinya data
                            // anggaran, dan meninggalkannya berarti menumpuk
                            // salinan di disk server yang tidak pernah ditengok.
                            hapusPohon(new File(path));
                        }
                    }
                });

        j.put("jobId", id);
        j.put("berkas", jumlah);
    }

    /**
     * Ekstrak arsip ke {@code tujuan}; mengembalikan jumlah berkas {@code .keu}.
     *
     * <p>Menolak entri yang keluar dari direktori tujuan, arsip yang terlalu
     * banyak entrinya, dan isi yang melebihi batas ukuran.</p>
     */
    static int ekstrak(File arsip, File tujuan) throws Exception {
        ZipFile zip = new ZipFile(arsip);
        int keu = 0;
        long total = 0L;
        int entri = 0;
        try {
            String kanonTujuan = tujuan.getCanonicalPath() + File.separator;
            Enumeration<? extends ZipEntry> daftar = zip.entries();
            while (daftar.hasMoreElements()) {
                ZipEntry e = daftar.nextElement();
                if (++entri > MAKS_ENTRI) {
                    throw new IllegalArgumentException("Arsip memuat terlalu banyak berkas "
                            + "(maksimal " + MAKS_ENTRI + ").");
                }
                if (e.isDirectory()) continue;

                File keluaran = new File(tujuan, e.getName());
                // Penjaga zip slip: jalur hasil harus tetap di dalam tujuan.
                if (!keluaran.getCanonicalPath().startsWith(kanonTujuan)) {
                    throw new IllegalArgumentException("Arsip memuat entri dengan jalur yang "
                            + "keluar dari direktori tujuan: " + e.getName());
                }
                if (keluaran.getParentFile() != null) keluaran.getParentFile().mkdirs();

                InputStream in = zip.getInputStream(e);
                OutputStream out = new FileOutputStream(keluaran);
                try {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        total += n;
                        if (total > MAKS_TOTAL) {
                            throw new IllegalArgumentException("Isi arsip melebihi batas ukuran.");
                        }
                        out.write(buf, 0, n);
                    }
                } finally {
                    try { out.close(); } catch (Exception diabaikan) { }
                    try { in.close(); } catch (Exception diabaikan) { }
                }
                if (e.getName().toLowerCase().endsWith("keu")) keu++;
            }
        } finally {
            try { zip.close(); } catch (Exception diabaikan) { }
        }
        return keu;
    }

    /** Hapus direktori beserta isinya. */
    static void hapusPohon(File berkas) {
        if (berkas == null || !berkas.exists()) return;
        if (berkas.isDirectory()) {
            File[] anak = berkas.listFiles();
            for (int i = 0; anak != null && i < anak.length; i++) hapusPohon(anak[i]);
        }
        try { berkas.delete(); } catch (Exception diabaikan) { }
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
