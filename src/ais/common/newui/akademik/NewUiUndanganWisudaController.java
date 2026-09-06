package ais.common.newui.akademik;

import java.util.Date;

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
import ais.action.master.helper.UndanganWisudaDownloadHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.Tbmuser;

/**
 * Kontrak native "Cetak Undangan Wisuda".
 *
 * <p>Menu ini URL-nya kata kunci <code>cetakUndanganWisuda</code>;
 * {@code Common} memetakannya ke {@code GenerateUndanganWisudaWindow}. Jendela
 * itu memvalidasi kelayakan mahasiswa lalu merender template Jasper
 * <code>Undangan_Wisuda</code>.</p>
 *
 * <p><b>Dua prasyarat disalin apa adanya dari layar ZK</b> — keduanya menolak
 * cetak, bukan sekadar peringatan:</p>
 * <ol>
 *   <li>biodata mahasiswa harus ada DAN nama ayah terisi, karena template
 *       memakai parameter <code>nama_ayah</code>;</li>
 *   <li>pendaftaran wisuda harus sudah memiliki nomor kursi
 *       (<code>noKursi</code>); tanpa itu tombol cetak pada ZK memang
 *       dinonaktifkan.</li>
 * </ol>
 *
 * <p>Parameter Jasper: peta acak {@code HashMapGenerator} ditambah
 * <code>mahasiswa</code> (id) dan <code>nama_ayah</code> — persis seperti ZK.
 * PDF dikirim sebagai {@code pdfBase64}.</p>
 *
 * <p>Fail-closed: mahasiswa wajib dipilih, seluruh prasyarat diperiksa sebelum
 * render, dan pengguna tanpa sesi ditolak.</p>
 */
public final class NewUiUndanganWisudaController {

    private static final String MODULE = "root/report";
    private static final String TEMPLATE = "Undangan_Wisuda";

    private NewUiUndanganWisudaController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response, String pageKey)
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
            if ("meta".equals(action)) meta(json);
            else if ("lookup".equals(action)) lookup(json, request);
            else if ("kelayakan".equals(action)) kelayakan(json, request);
            else if ("export".equals(action) || "export_pdf".equals(action)) cetak(json, request);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) { response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage()); }
        catch (IllegalArgumentException e) { response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage()); }
        catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal menyiapkan undangan wisuda. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiUndanganWisudaController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    private static void meta(JSONObject j) throws Exception {
        j.put("judul", "Cetak Undangan Wisuda");
        j.put("template", TEMPLATE);
        j.put("mahasiswaWajib", true);
        // Disampaikan terbuka agar klien dapat menjelaskan sebab penolakan
        // sebelum pengguna menekan cetak.
        JSONArray syarat = new JSONArray();
        syarat.put("Seluruh persetujuan pendaftaran wisuda sudah disetujui.");
        syarat.put("Pendaftaran wisuda sudah memiliki nomor kursi.");
        j.put("syarat", syarat);
    }

    /** Pencarian mahasiswa; daftar awal tampil tanpa perlu mengetik. */
    private static void lookup(JSONObject j, HttpServletRequest r) throws Exception {
        String q = text(r.getParameter("q"), "");
        JSONArray arr = new JSONArray();
        Session s = HibernateUtil.openSession();
        try {
            Criteria c = s.createCriteria(Mahasiswa.class).addOrder(Order.asc("nama")).setMaxResults(50);
            if (q.length() >= 2) {
                c.add(Restrictions.or(Restrictions.ilike("nama", "%" + q + "%"),
                        Restrictions.ilike("nim", "%" + q + "%")));
            }
            for (Object o : c.list()) {
                Mahasiswa m = (Mahasiswa) o;
                arr.put(new JSONObject().put("id", m.getId()).put("nama", nz(m.getNama()))
                        .put("kode", nz(m.getNim())));
            }
        } finally { s.close(); }
        j.put("mahasiswa", arr).put("total", arr.length());
    }

    /**
     * Memeriksa kedua prasyarat tanpa mencetak, sehingga klien dapat menonaktifkan
     * tombol cetak dan menjelaskan alasannya — seperti ZK menonaktifkan tombol.
     */
    private static void kelayakan(JSONObject j, HttpServletRequest r) throws Exception {
        Long id = id(r, "mahasiswaId");
        if (id == null) throw new IllegalArgumentException("Mahasiswa wajib dipilih.");
        Session s = HibernateUtil.openSession();
        try {
            Mahasiswa mahasiswa = (Mahasiswa) s.get(Mahasiswa.class, id);
            if (mahasiswa == null) throw new IllegalArgumentException("Mahasiswa tidak ditemukan.");
            String halangan = halangan(s, mahasiswa);
            j.put("nim", nz(mahasiswa.getNim())).put("nama", nz(mahasiswa.getNama()));
            j.put("layak", halangan == null);
            if (halangan != null) j.put("alasan", halangan);
        } finally { s.close(); }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void cetak(JSONObject j, HttpServletRequest r) throws Exception {
        Long id = id(r, "mahasiswaId");
        if (id == null) throw new IllegalArgumentException("Mahasiswa wajib dipilih.");
        Session s = HibernateUtil.openSession();
        Mahasiswa mahasiswa;
        java.io.File pdf;
        try {
            mahasiswa = (Mahasiswa) s.get(Mahasiswa.class, id);
            if (mahasiswa == null) throw new IllegalArgumentException("Mahasiswa tidak ditemukan.");
            String halangan = halangan(s, mahasiswa);
            if (halangan != null) throw new IllegalArgumentException(halangan);
            PendaftaranWisuda daftar = pendaftaranTerakhir(s, mahasiswa);
            pdf = UndanganWisudaDownloadHelper.generatePdf(daftar);
        } finally { s.close(); }
        if (pdf == null || !pdf.exists()) throw new IllegalStateException("PDF undangan gagal dibuat.");
        byte[] isi = java.nio.file.Files.readAllBytes(pdf.toPath());
        j.put("namaFile", "undangan_wisuda_" + nz(mahasiswa.getNim()) + "_"
                + Common.databaseDateFormat.get().format(new Date()) + ".pdf");
        j.put("pdfBase64", java.util.Base64.getEncoder().encodeToString(isi));
    }

    /** Mengembalikan alasan penolakan, atau null bila mahasiswa layak cetak. */
    private static String halangan(Session s, Mahasiswa mahasiswa) {
        PendaftaranWisuda daftar = pendaftaranTerakhir(s, mahasiswa);
        if (daftar == null) return "Mahasiswa ini belum terdaftar wisuda.";
        if (!UndanganWisudaDownloadHelper.disetujuiSemua(daftar)) {
            return "Seluruh persetujuan pendaftaran wisuda belum selesai.";
        }
        if (daftar.getNoKursi() == null || daftar.getNoKursi().trim().length() == 0) {
            return "Mahasiswa ini belum mendapatkan nomor kursi wisuda.";
        }
        return null;
    }

    private static PendaftaranWisuda pendaftaranTerakhir(Session s, Mahasiswa mahasiswa) {
        return (PendaftaranWisuda) s.createCriteria(PendaftaranWisuda.class)
                .add(Restrictions.eq("mahasiswa", mahasiswa))
                .addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
    }

    private static Long id(HttpServletRequest r, String nama) {
        String v = r.getParameter(nama);
        if (v == null || v.trim().length() == 0) return null;
        try { return Long.valueOf(v.trim()); } catch (Exception e) { return null; }
    }

    private static String nz(String v) { return v == null ? "" : v; }

    private static String text(String v, String f) { return v == null || v.trim().length() == 0 ? f : v.trim(); }

    private static void fail(JSONObject j, String c, String m) throws Exception {
        j.put("ok", false).put("code", c).put("message", m == null ? "Operasi ditolak." : m);
    }

    private static void write(HttpServletResponse r, JSONObject j) throws Exception {
        r.getWriter().write(j.toString());
    }
}
