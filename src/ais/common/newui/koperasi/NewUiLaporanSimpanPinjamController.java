package ais.common.newui.koperasi;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.koperasi.helper.SimpanPinjamReportService;
import ais.action.master.koperasi.helper.SimpanPinjamUiUtil;
import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/** Kontrak native lengkap untuk Laporan Simpan Pinjam Koperasi. */
public final class NewUiLaporanSimpanPinjamController {

    private static final String MODULE = "koperasi";
    private static final int UKURAN_BAWAAN = 50;
    private static final int UKURAN_MAKSIMAL = 200;

    private NewUiLaporanSimpanPinjamController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            Session session = HibernateUtil.currentSession();
            if ("meta".equals(action)) {
                meta(json);
            } else if ("list".equals(action)) {
                daftar(json, request, session);
            } else if ("detail".equals(action)) {
                surat(json, session);
            } else if ("export".equals(action)) {
                ekspor(response, request, session);
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
            fail(json, "INTERNAL_ERROR", "Gagal menyusun laporan simpan pinjam. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLaporanSimpanPinjamController"); }
            catch (Exception ignored) { }
        }
        write(response, json);
    }

    private static void meta(JSONObject j) throws Exception {
        JSONArray bagian = new JSONArray();
        for (SimpanPinjamReportService.Bagian b : SimpanPinjamReportService.katalog()) {
            bagian.put(new JSONObject().put("kunci", b.kunci).put("judul", b.judul)
                    .put("deskripsi", b.deskripsi));
        }
        j.put("judul", "Laporan Simpan Pinjam")
                .put("bagian", bagian)
                .put("bagianBawaan", SimpanPinjamReportService.BUKU_SIMPAN_PINJAM)
                .put("ukuranHalamanBawaan", UKURAN_BAWAAN)
                .put("ukuranHalamanMaksimal", UKURAN_MAKSIMAL)
                .put("bolehEkspor", true)
                .put("bolehSuratTeguran", true)
                .put("catatan", "Delapan buku utama, perhitungan bunga simpanan, ekspor Excel, dan surat teguran memakai sumber data yang sama dengan layar lama.");
    }

    private static void daftar(JSONObject j, HttpServletRequest request, Session session) throws Exception {
        String kunci = bagianWajib(request);
        int page = angka(request.getParameter("page"), 1, Integer.MAX_VALUE, 1);
        int pageSize = angka(request.getParameter("pageSize"), 1, UKURAN_MAKSIMAL, UKURAN_BAWAAN);
        SimpanPinjamReportService.Bagian bagian = SimpanPinjamReportService.bangun(
                session, kunci, ais.ui.util.WaktuUtil.getDate());
        int total = bagian.baris.size();
        int pageCount = total == 0 ? 1 : (int) Math.ceil(total / (double) pageSize);
        if (page > pageCount) page = pageCount;
        int awal = Math.min(total, (page - 1) * pageSize);
        int akhir = Math.min(total, awal + pageSize);

        JSONArray baris = new JSONArray();
        for (int i = awal; i < akhir; i++) baris.put(baris(bagian.baris.get(i)));
        j.put("bagian", bagian.kunci).put("judul", bagian.judul)
                .put("deskripsi", bagian.deskripsi).put("header", array(bagian.header))
                .put("jenisKolom", array(bagian.jenisKolom)).put("rows", baris)
                .put("grafik", peta(bagian.grafik)).put("ringkasan", peta(bagian.ringkasan))
                .put("catatan", bagian.catatan == null ? "" : bagian.catatan)
                .put("page", page).put("pageSize", pageSize).put("pageCount", pageCount)
                .put("total", total).put("dari", total == 0 ? 0 : awal + 1).put("sampai", akhir);
    }

    private static void surat(JSONObject j, Session session) throws Exception {
        SimpanPinjamReportService.Surat surat = SimpanPinjamReportService.suratTeguran(
                session, ais.ui.util.WaktuUtil.getDate());
        j.put("judul", "Surat Teguran")
                .put("jumlahSurat", surat.jumlah)
                .put("html", surat.html)
                .put("pesan", surat.jumlah == 0
                        ? "Tidak ada anggota yang menunggak."
                        : "Surat teguran siap ditinjau dan dicetak.");
    }

    private static void ekspor(HttpServletResponse response, HttpServletRequest request,
            Session session) throws Exception {
        String kunci = bagianWajib(request);
        SimpanPinjamReportService.Bagian bagian = SimpanPinjamReportService.bangun(
                session, kunci, ais.ui.util.WaktuUtil.getDate());
        File file = SimpanPinjamUiUtil.buatExcel(bagian.namaBerkas, bagian.sheet,
                bagian.header, bagian.jenisKolom, bagian.baris);
        String nama = bagian.namaBerkas + "_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".xlsx";
        response.reset();
        response.setContentType(SimpanPinjamUiUtil.XLSX_MIME);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(nama, "UTF-8").replace("+", "%20"));
        response.setContentLength((int) Math.min(Integer.MAX_VALUE, file.length()));
        FileInputStream in = null;
        ServletOutputStream out = null;
        try {
            in = new FileInputStream(file);
            out = response.getOutputStream();
            byte[] buffer = new byte[16384];
            int baca;
            while ((baca = in.read(buffer)) >= 0) out.write(buffer, 0, baca);
            out.flush();
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) { }
            if (out != null) try { out.close(); } catch (Exception ignored) { }
            if (file != null && file.exists() && !file.delete()) file.deleteOnExit();
        }
    }

    private static String bagianWajib(HttpServletRequest request) {
        String kunci = text(request.getParameter("bagian"), "");
        if (!SimpanPinjamReportService.dikenal(kunci)) {
            throw new IllegalArgumentException("Bagian laporan tidak dikenal.");
        }
        return kunci;
    }

    private static JSONArray baris(Object[] nilai) throws Exception {
        JSONArray hasil = new JSONArray();
        if (nilai == null) return hasil;
        for (Object item : nilai) {
            if (item == null) hasil.put(JSONObject.NULL);
            else if (item instanceof Date) hasil.put(new SimpleDateFormat("yyyy-MM-dd").format((Date) item));
            else hasil.put(item);
        }
        return hasil;
    }

    private static JSONArray array(String[] nilai) throws Exception {
        JSONArray hasil = new JSONArray();
        for (String item : nilai) hasil.put(item);
        return hasil;
    }

    private static JSONArray array(int[] nilai) throws Exception {
        JSONArray hasil = new JSONArray();
        for (int item : nilai) hasil.put(item);
        return hasil;
    }

    private static JSONObject peta(Map<?, ?> nilai) throws Exception {
        JSONObject hasil = new JSONObject();
        for (Map.Entry<?, ?> e : nilai.entrySet()) {
            if (e.getKey() != null) hasil.put(String.valueOf(e.getKey()), e.getValue());
        }
        return hasil;
    }

    private static int angka(String nilai, int min, int max, int fallback) {
        String v = text(nilai, "");
        if (v.length() == 0) return fallback;
        try {
            int hasil = Integer.parseInt(v);
            if (hasil < min || hasil > max) throw new NumberFormatException();
            return hasil;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nomor atau ukuran halaman tidak sah.");
        }
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }

    private static void fail(JSONObject j, String code, String message) throws Exception {
        j.put("ok", false).put("code", code)
                .put("message", message == null ? "Operasi ditolak." : message);
    }

    private static void write(HttpServletResponse response, JSONObject json) throws Exception {
        response.getWriter().write(json.toString());
    }
}
