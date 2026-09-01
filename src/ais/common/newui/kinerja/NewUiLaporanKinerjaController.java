package ais.common.newui.kinerja;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;

/**
 * Kontrak native laporan kinerja pegawai/dosen: keluarga BKD (Beban Kerja
 * Dosen) dan LKP (Laporan Kinerja Pegawai).
 *
 * <p>Layar ZK-nya adalah jendela laporan yang hanya menyusun parameter lalu
 * menyerahkan render ke Jasper. Controller ini menyusun parameter yang SAMA
 * secara headless, kemudian mengirim PDF sebagai {@code pdfBase64} pada amplop
 * JSON — pola yang sama dengan {@code NewUiLaporanSekolahController}. Tidak ada
 * ZUL, WebView, maupun iframe yang dilibatkan.</p>
 *
 * <p>Sumber parameter (dibaca langsung dari kelas ZK agar tidak menebak):</p>
 * <ul>
 *   <li>BKD — {@code ta}, {@code semester}, {@code dosen}, serta berkas tanda
 *       tangan {@code ttd_dosen}/{@code ttd_atasan} dari LampiranLain
 *       {@link LampiranLain#TTD_DOSEN} milik dosen dan atasan langsungnya
 *       (lihat {@code ais.action.report.bkd.LaporanRencanaBkdWindow}).</li>
 *   <li>LKP — {@code tahun}, {@code bulan}, {@code bulan_str}, {@code dosen},
 *       tiga prosentase SKP dari konfigurasi, {@code mulai}, {@code banyak},
 *       serta foto pegawai (lihat
 *       {@code ais.action.report.lkp.LaporanRealisasiLkpWindow}).</li>
 * </ul>
 *
 * <p>Fail-closed: aksi divalidasi {@link NewUiRouteGuard}, pengguna tanpa sesi
 * ditolak, dan pegawai yang tidak dipilih dikirim sebagai -1 persis seperti ZK
 * sehingga query laporan tidak pernah melebar tanpa filter.</p>
 */
public final class NewUiLaporanKinerjaController {

    private static final String MODULE = "root/report";

    /** Satu varian laporan: kode stabil, nama tampil, dan template Jasper. */
    public static final class Varian {
        public final String kode, nama, template, keluarga;
        Varian(String kode, String nama, String template, String keluarga) {
            this.kode = kode; this.nama = nama; this.template = template; this.keluarga = keluarga;
        }
    }

    public static final String JENIS_BKD_RENCANA = "bkd_rencana";
    public static final String JENIS_BKD_REALISASI = "bkd_realisasi";
    public static final String JENIS_BKD_VERIFIKASI = "bkd_verifikasi";
    public static final String JENIS_LKP = "lkp";
    public static final String JENIS_LKP_CATATAN = "lkp_catatan";
    public static final String JENIS_LKP_DETAIL = "lkp_detail";

    private NewUiLaporanKinerjaController() { }

    private static Varian varian(String jenis) {
        if (JENIS_BKD_RENCANA.equals(jenis))
            return new Varian(jenis, "Laporan Rencana Kinerja", "form_rencana_kinerja_dosen", "bkd");
        if (JENIS_BKD_REALISASI.equals(jenis))
            return new Varian(jenis, "Laporan Realisasi Kinerja", "form_realisasi_kinerja_dosen", "bkd");
        if (JENIS_BKD_VERIFIKASI.equals(jenis))
            return new Varian(jenis, "Lembar Hasil Verifikasi BKD", "summary_hasil_verifikasi_bkd", "bkd");
        if (JENIS_LKP.equals(jenis))
            return new Varian(jenis, "Realisasi Kerja Pegawai", "lkp_pegawai", "lkp");
        if (JENIS_LKP_CATATAN.equals(jenis))
            return new Varian(jenis, "Catatan Harian Kinerja", "lkp_pegawai_catatan", "lkp");
        if (JENIS_LKP_DETAIL.equals(jenis))
            return new Varian(jenis, "Rincian Kinerja Pegawai", "lkp_pegawai_detail", "lkp");
        throw new IllegalArgumentException("Jenis laporan kinerja tidak dikenal.");
    }

    public static void handle(HttpServletRequest request, HttpServletResponse response, String jenis, String pageKey)
            throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403); fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia."); write(response, json); return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            if ("meta".equals(action)) meta(json, jenis);
            else if ("lookup".equals(action)) lookup(json, request);
            else if ("export".equals(action) || "export_pdf".equals(action)) cetak(json, request, jenis);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) { response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage()); }
        catch (IllegalArgumentException e) { response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage()); }
        catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memproses laporan. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLaporanKinerjaController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    // ------------------------------------------------------------------ meta
    private static void meta(JSONObject j, String jenis) throws Exception {
        Varian v = varian(jenis);
        j.put("kode", v.kode).put("nama", v.nama).put("keluarga", v.keluarga);

        int tahunKini = Calendar.getInstance().get(Calendar.YEAR);
        JSONArray tahun = new JSONArray();
        for (int t = tahunKini + 1; t >= tahunKini - 5; t--) tahun.put(t);
        j.put("tahun", tahun);

        if ("bkd".equals(v.keluarga)) {
            JSONArray ta = new JSONArray();
            for (int t = tahunKini + 1; t >= tahunKini - 5; t--) ta.put(t + "/" + (t + 1));
            j.put("tahunAkademik", ta);
            j.put("semester", new JSONArray().put("Ganjil").put("Genap"));
        } else {
            JSONArray bulan = new JSONArray();
            for (int i = 0; i < Common.BULAN.length; i++) {
                bulan.put(new JSONObject().put("nilai", i).put("nama", Common.BULAN[i]));
            }
            j.put("bulan", bulan);
            j.put("mulaiBawaan", 0).put("banyakBawaan", 5);
        }
        j.put("pegawaiWajib", true);
    }

    // ---------------------------------------------------------------- lookup
    /** Pencarian pegawai untuk filter; daftar awal tampil tanpa mengetik. */
    private static void lookup(JSONObject j, HttpServletRequest r) throws Exception {
        String q = text(r.getParameter("q"), "");
        JSONArray arr = new JSONArray();
        Session s = HibernateUtil.openSession();
        try {
            Criteria c = s.createCriteria(Pegawai.class).addOrder(Order.asc("nama")).setMaxResults(50);
            if (q.length() >= 2) {
                c.add(Restrictions.or(Restrictions.ilike("nama", "%" + q + "%"),
                        Restrictions.ilike("mycode", "%" + q + "%")));
            }
            for (Object o : c.list()) {
                Pegawai p = (Pegawai) o;
                arr.put(new JSONObject().put("id", p.getId()).put("nama", nz(p.getNama()))
                        .put("kode", nz(p.getMycode())));
            }
        } finally { s.close(); }
        j.put("pegawai", arr).put("total", arr.length());
    }

    // ----------------------------------------------------------------- cetak
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void cetak(JSONObject j, HttpServletRequest r, String jenis) throws Exception {
        Varian v = varian(jenis);
        Long pegawaiId = id(r, "pegawaiId");
        if (pegawaiId == null) throw new IllegalArgumentException("Pegawai wajib dipilih.");

        Session s = HibernateUtil.openSession();
        Pegawai pegawai;
        try { pegawai = (Pegawai) s.get(Pegawai.class, pegawaiId); }
        finally { s.close(); }
        if (pegawai == null) throw new IllegalArgumentException("Pegawai tidak ditemukan.");

        Map parameters = new HashMap();
        if ("bkd".equals(v.keluarga)) {
            String ta = text(r.getParameter("tahunAkademik"), "");
            String semester = text(r.getParameter("semester"), "");
            if (ta.length() == 0) throw new IllegalArgumentException("Tahun akademik wajib dipilih.");
            if (semester.length() == 0) throw new IllegalArgumentException("Semester wajib dipilih.");
            parameters.put("ta", ta);
            parameters.put("semester", semester);
            parameters.put("dosen", pegawai.getId() == null ? Long.valueOf(-1L) : pegawai.getId());
            // Tanda tangan hanya disertakan bila lampirannya benar-benar gambar,
            // persis syarat pada layar ZK.
            if (pegawai.getDosen() != null) {
                String ttd = berkasTandaTangan(pegawai.getDosen().getId());
                if (ttd != null) parameters.put("ttd_dosen", ttd);
                if (pegawai.getDosen().getAtasanlangsung() != null) {
                    String atasan = berkasTandaTangan(pegawai.getDosen().getAtasanlangsung());
                    if (atasan != null) parameters.put("ttd_atasan", atasan);
                }
            }
        } else {
            Integer tahun = angka(r, "tahun");
            Integer bulan = angka(r, "bulan");
            if (tahun == null) throw new IllegalArgumentException("Tahun wajib dipilih.");
            if (bulan == null || bulan < 0 || bulan >= Common.BULAN.length)
                throw new IllegalArgumentException("Bulan wajib dipilih.");
            parameters.put("tahun", tahun);
            parameters.put("bulan", bulan);
            parameters.put("bulan_str", Common.BULAN[bulan]);
            parameters.put("dosen", pegawai.getId() == null ? Long.valueOf(-1L) : pegawai.getId());
            parameters.put("prosentasi_nilai_skp_kuantitas_double", konfigurasi("prosentasi_nilai_skp_kuantitas", 40));
            parameters.put("prosentasi_nilai_skp_kualitas_double", konfigurasi("prosentasi_nilai_skp_kualitas", 40));
            parameters.put("prosentasi_nilai_skp_waktu_double", konfigurasi("prosentasi_nilai_skp_waktu", 20));
            Integer mulai = angka(r, "mulai");
            Integer banyak = angka(r, "banyak");
            parameters.put("mulai", mulai == null ? Integer.valueOf(0) : mulai);
            parameters.put("banyak", banyak == null ? Integer.valueOf(5) : banyak);
            try { pegawai.putPhoto(parameters); } catch (Exception ignored) { }
        }

        ais.common.newui.laporan.JasperPdfUtil.tulis(j, v.template, parameters, v.kode, v.nama);
    }

    // ------------------------------------------------------------------ util
    private static String berkasTandaTangan(Long dosenId) {
        if (dosenId == null) return null;
        try {
            LampiranLain lam = LampiranLain.ambil(dosenId, LampiranLain.TTD_DOSEN);
            String nama = lam == null ? null : lam.getNama();
            if (nama == null) return null;
            String kecil = nama.toLowerCase();
            boolean gambar = kecil.endsWith(".jpg") || kecil.endsWith(".png") || kecil.endsWith(".jpeg")
                    || kecil.endsWith(".gif") || kecil.endsWith(".tif") || kecil.endsWith(".bmp");
            if (!gambar) return null;
            java.io.File f = lam.ambilFile();
            return f == null ? null : f.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private static double konfigurasi(String kunci, double bawaan) {
        try { return Double.parseDouble(Common.getKonfigurasi(kunci, String.valueOf(bawaan)).getNilai()); }
        catch (Exception e) { return bawaan; }
    }

    private static Long id(HttpServletRequest r, String nama) {
        String v = r.getParameter(nama);
        if (v == null || v.trim().length() == 0) return null;
        try { return Long.valueOf(v.trim()); } catch (Exception e) { return null; }
    }

    private static Integer angka(HttpServletRequest r, String nama) {
        String v = r.getParameter(nama);
        if (v == null || v.trim().length() == 0) return null;
        try { return Integer.valueOf(v.trim()); } catch (Exception e) { return null; }
    }

    private static String nz(String v) { return v == null ? "" : v; }

    private static String text(String v, String f) { return v == null || v.trim().length() == 0 ? f : v.trim(); }

    private static void fail(JSONObject j, String c, String m) throws Exception {
        j.put("ok", false).put("code", c).put("message", m == null ? "Operasi ditolak." : m);
    }

    private static void write(HttpServletResponse r, JSONObject j) throws Exception {
        r.getWriter().write(j.toString());
    }

    /** Dipertahankan agar daftar varian dapat diuji tanpa container. */
    public static List<String> semuaJenis() {
        List<String> hasil = new ArrayList<String>();
        hasil.add(JENIS_BKD_RENCANA); hasil.add(JENIS_BKD_REALISASI); hasil.add(JENIS_BKD_VERIFIKASI);
        hasil.add(JENIS_LKP); hasil.add(JENIS_LKP_CATATAN); hasil.add(JENIS_LKP_DETAIL);
        return hasil;
    }
}
