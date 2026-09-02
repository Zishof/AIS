package ais.common.newui.laporan;

import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;

/**
 * Kontrak native dua laporan rekapitulasi yang menu-nya berupa kata kunci aksi,
 * bukan berkas ZUL: <code>rekapDataPmdk</code> dan
 * <code>rekapAngketDosenPerDosen</code>.
 *
 * <p>Kedua menu itu dipetakan {@code Common} ke jendela ZK
 * ({@code LaporanRekapitulasiPMDKWindow} dan
 * {@code LaporanAngketDosenPerDosenWindow}) yang tugasnya hanya menyusun
 * parameter lalu menyerahkan render ke Jasper. Parameter di sini disalin dari
 * kedua kelas tersebut, bukan ditebak:</p>
 *
 * <ul>
 *   <li><b>PMDK</b> — template <code>rekap_data_pmdk</code>, satu parameter
 *       <code>tahunakademik</code> di atas peta acak {@code HashMapGenerator}.</li>
 *   <li><b>Angket dosen</b> — template
 *       <code>rekap_angket_dosen_per_dosen_saja</code> dengan
 *       <code>genapGanjil</code>, <code>tahun_akademik</code>,
 *       <code>dosen</code>/<code>nama_dosen</code>,
 *       <code>fakultas</code>/<code>fakultas_nama</code>,
 *       <code>jurusan</code>/<code>jurusan_nama</code>, <code>program</code>,
 *       dan <code>aktif</code>. Id yang tidak dipilih dikirim -1 dan teks
 *       kosong dikirim "Semua", persis seperti layar ZK, sehingga kueri laporan
 *       tidak pernah melebar tanpa filter.</li>
 * </ul>
 *
 * <p>PDF dikirim sebagai {@code pdfBase64} pada amplop JSON — pola yang sama
 * dengan controller laporan native lainnya. Fail-closed: jenis laporan di luar
 * daftar ditolak, sesi tanpa pengguna ditolak, dan hanya aksi baca/ekspor yang
 * tersedia.</p>
 */
public final class NewUiLaporanRekapController {

    private static final String MODULE = "root/report";

    /** Rekapitulasi data pendaftar PMDK. */
    public static final String JENIS_PMDK = "pmdk";
    /** Rekapitulasi angket mahasiswa per dosen. */
    public static final String JENIS_ANGKET_DOSEN = "angket_dosen";

    private NewUiLaporanRekapController() { }

    private static String template(String jenis) {
        if (JENIS_PMDK.equals(jenis)) return "rekap_data_pmdk";
        if (JENIS_ANGKET_DOSEN.equals(jenis)) return "rekap_angket_dosen_per_dosen_saja";
        throw new IllegalArgumentException("Jenis laporan rekapitulasi tidak dikenal.");
    }

    private static String judul(String jenis) {
        return JENIS_PMDK.equals(jenis) ? "Rekap Data PMDK" : "Rekap Angket Dosen Per Dosen";
    }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String jenis, String pageKey) throws Exception {
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
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLaporanRekapController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    // ------------------------------------------------------------------ meta
    /** Daftar filter yang harus ditampilkan klien untuk jenis laporan ini. */
    private static void meta(JSONObject j, String jenis) throws Exception {
        j.put("jenis", jenis).put("judul", judul(jenis)).put("template", template(jenis));
        JSONArray filter = new JSONArray();
        filter.put(deskripsi("tahunAkademik", "Tahun Akademik", "teks", true));
        if (JENIS_ANGKET_DOSEN.equals(jenis)) {
            // Ketiga nilai ini yang diterima template Jasper; sebagai teks
            // bebas pengguna harus menebak ejaannya persis.
            filter.put(deskripsi("genapGanjil", "Ganjil/Genap", "pilihan", false)
                    .put("opsi", new JSONArray().put("Semua").put("Ganjil").put("Genap")));
            filter.put(deskripsi("dosenId", "Dosen", "relasi", false));
            filter.put(deskripsi("fakultasId", "Fakultas", "relasi", false));
            filter.put(deskripsi("jurusanId", "Jurusan", "relasi", false));
            filter.put(deskripsi("program", "Program", "teks", false));
            // "bendera", bukan "boolean": klien laporan hanya mengenal nama
            // yang pertama, dan tipe asing jatuh ke kolom teks bebas sehingga
            // penyaring ini praktis tidak dapat dipakai.
            filter.put(deskripsi("aktif", "Hanya yang aktif", "bendera", false));
        }
        j.put("filter", filter);
    }

    private static JSONObject deskripsi(String nama, String label, String tipe, boolean wajib) throws Exception {
        return new JSONObject().put("nama", nama).put("label", label).put("tipe", tipe).put("wajib", wajib);
    }

    // ----------------------------------------------------------------- cetak
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void cetak(JSONObject j, HttpServletRequest r, String jenis) throws Exception {
        String tpl = template(jenis);
        Map parameters = ais.common.HashMapGenerator.getRand();

        if (JENIS_PMDK.equals(jenis)) {
            String ta = text(r.getParameter("tahunAkademik"), "");
            if (ta.length() == 0) throw new IllegalArgumentException("Tahun akademik wajib dipilih.");
            parameters.put("tahunakademik", ta);
        } else {
            // Nilai bawaan meniru layar ZK: teks kosong menjadi "Semua" dan id
            // yang tidak dipilih menjadi -1 agar filter laporan tetap tegas.
            parameters.put("genapGanjil", text(r.getParameter("genapGanjil"), "Semua"));
            parameters.put("tahun_akademik", text(r.getParameter("tahunAkademik"), "Semua"));
            Long dosen = id(r, "dosenId");
            parameters.put("dosen", dosen == null ? Long.valueOf(-1L) : dosen);
            parameters.put("nama_dosen", text(r.getParameter("namaDosen"), namaEntitas(Dosen.class, dosen)));
            Long fakultas = id(r, "fakultasId");
            parameters.put("fakultas", fakultas == null ? Long.valueOf(-1L) : fakultas);
            parameters.put("fakultas_nama", text(r.getParameter("namaFakultas"), namaEntitas(Fakultas.class, fakultas)));
            Long jurusan = id(r, "jurusanId");
            parameters.put("jurusan", jurusan == null ? Long.valueOf(-1L) : jurusan);
            parameters.put("jurusan_nama", text(r.getParameter("namaJurusan"), namaEntitas(Jurusan.class, jurusan)));
            parameters.put("program", text(r.getParameter("program"), "-1"));
            parameters.put("aktif", Boolean.valueOf("true".equalsIgnoreCase(text(r.getParameter("aktif"), "false"))));
        }

        java.io.File pdf = ais.action.report.Report.generateFileReportSimple(
                ais.action.report.Report.PDF, parameters, tpl);
        if (pdf == null || !pdf.exists()) throw new IllegalStateException("PDF laporan gagal dibuat.");
        byte[] isi = java.nio.file.Files.readAllBytes(pdf.toPath());
        j.put("namaFile", jenis + "_" + Common.databaseDateFormat.get().format(new Date()) + ".pdf");
        j.put("varianNama", judul(jenis));
        j.put("pdfBase64", java.util.Base64.getEncoder().encodeToString(isi));
    }

    // ------------------------------------------------------------------- util
    // --------------------------------------------------------------- lookup
    /**
     * Isi ketiga penyaring relasi. Tanpa aksi ini dropdown Dosen, Fakultas, dan
     * Jurusan selalu kosong, sehingga layar mengumumkan penyaring yang tidak
     * pernah dapat dipakai — kontraknya lengkap tetapi layarnya tidak berguna.
     */
    private static void lookup(JSONObject j, HttpServletRequest r) throws Exception {
        String nama = text(r.getParameter("filter"), "");
        String q = text(r.getParameter("q"), "");
        Class<?> kelas;
        if ("dosenId".equals(nama)) kelas = Dosen.class;
        else if ("fakultasId".equals(nama)) kelas = Fakultas.class;
        else if ("jurusanId".equals(nama)) kelas = Jurusan.class;
        else throw new IllegalArgumentException("Filter relasi tidak dikenal.");

        JSONArray arr = new JSONArray();
        Session s = HibernateUtil.openSession();
        try {
            Criteria c = s.createCriteria(kelas).setMaxResults(50);
            if (q.length() >= 2) c.add(Restrictions.ilike("nama", "%" + q + "%"));
            c.addOrder(Order.asc("nama"));
            // Jurusan disaring oleh fakultas yang sudah dipilih; tanpa ini
            // daftarnya bercampur antar-fakultas dan pengguna memilih jurusan
            // yang tidak mungkin muncul pada laporannya.
            Long fakultas = id(r, "fakultasId");
            if (Jurusan.class.equals(kelas) && fakultas != null) {
                c.add(Restrictions.eq("fakultas.id", fakultas));
            }
            for (Object o : c.list()) {
                arr.put(new JSONObject().put("id", namaProperti(o, "getId"))
                        .put("nama", String.valueOf(namaProperti(o, "getNama"))));
            }
        } finally { s.close(); }
        j.put("pilihan", arr).put("total", arr.length());
    }

    private static Object namaProperti(Object o, String getter) {
        try { return o.getClass().getMethod(getter, new Class[0]).invoke(o, new Object[0]); }
        catch (Exception e) { return ""; }
    }

    /**
     * Nama tampil untuk header laporan, dicari dari id yang dipilih.
     *
     * <p>Template Jasper meminta {@code nama_dosen}, {@code fakultas_nama}, dan
     * {@code jurusan_nama}, sementara klien hanya memegang id hasil lookup.
     * Menuntut klien mengirim namanya membuat parameter itu selalu kosong —
     * jadi server yang mencarinya.</p>
     */
    private static String namaEntitas(Class<?> kelas, Long id) {
        if (id == null) return "";
        Session s = HibernateUtil.openSession();
        try {
            Object o = s.get(kelas, id);
            return o == null ? "" : String.valueOf(namaProperti(o, "getNama"));
        } catch (Exception e) {
            return "";
        } finally { s.close(); }
    }

    private static Long id(HttpServletRequest r, String nama) {
        String v = r.getParameter(nama);
        if (v == null || v.trim().length() == 0) return null;
        try { return Long.valueOf(v.trim()); } catch (Exception e) { return null; }
    }

    private static String text(String v, String f) { return v == null || v.trim().length() == 0 ? f : v.trim(); }

    private static void fail(JSONObject j, String c, String m) throws Exception {
        j.put("ok", false).put("code", c).put("message", m == null ? "Operasi ditolak." : m);
    }

    private static void write(HttpServletResponse r, JSONObject j) throws Exception {
        r.getWriter().write(j.toString());
    }

    /** Dipakai self-test agar pemetaan jenis-ke-template tidak menyimpang. */
    public static String templateUntuk(String jenis) { return template(jenis); }

    /** Dipakai self-test: daftar jenis yang dilayani controller ini. */
    public static String[] semuaJenis() { return new String[] { JENIS_PMDK, JENIS_ANGKET_DOSEN }; }
}
