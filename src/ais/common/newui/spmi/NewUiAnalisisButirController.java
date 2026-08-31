package ais.common.newui.spmi;

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

import ais.action.master.helper.PenjaminanMutuAnalisisHelper;
import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;

/**
 * Kontrak native Dasbor Penjaminan Mutu — Analisis Butir Soal.
 *
 * <p>Layar ZK-nya (<code>/pages/master/spmi/analisis_butir_penjaminan_mutu.zul</code>)
 * hanya sebuah wadah kosong yang diisi
 * {@code PenjaminanMutuAnalisisHelper.render()} melalui zscript, sehingga tidak
 * ada kontrak JSON yang bisa dipakai ulang. Controller ini menyusun data yang
 * sama secara headless.</p>
 *
 * <p><b>Kueri disalin dari {@code PenjaminanMutuAnalisisHelper.loadData()}</b>,
 * bukan dikarang: {@link PertemuanPunyaUjian} dengan alias pertemuan →
 * perkuliahan, hanya baris yang memiliki format nilai dan bukan
 * {@code Tugas.JSON}, disaring opsional menurut tahun ajaran dan ganjil/genap,
 * diurutkan id menurun, dibatasi 250 baris.</p>
 *
 * <p><b>Status dan catatan</b> tersimpan sebagai JSON pada kolom
 * <code>analisis_catatan_json</code>, bukan kolom terindeks. Karena itu
 * penyaringan status dilakukan di memori — persis seperti layar ZK — dan nilai
 * bawaannya {@code menunggu}. Pembacaannya memakai
 * {@link PenjaminanMutuAnalisisHelper#getStatus(PertemuanPunyaUjian)} agar
 * aturannya tetap satu sumber.</p>
 *
 * <p><b>Batas yang disengaja.</b> Kontrak ini hanya membaca. Menyetujui atau
 * meminta revisi memicu notifikasi ke dosen bersangkutan; alur notifikasi itu
 * belum direproduksi secara native, sehingga tombolnya tidak disediakan agar
 * tidak menjanjikan proses yang belum ada.</p>
 */
public final class NewUiAnalisisButirController {

    private static final String MODULE = "spmi";
    private static final int BATAS_BARIS = 250;

    private NewUiAnalisisButirController() { }

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
            else if ("list".equals(action)) daftar(json, request);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) { response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage()); }
        catch (IllegalArgumentException e) { response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage()); }
        catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memuat analisis butir. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiAnalisisButirController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    private static void meta(JSONObject j) throws Exception {
        j.put("judul", "Dasbor Penjaminan Mutu — Analisis Butir Soal");
        JSONArray status = new JSONArray()
                .put(PenjaminanMutuAnalisisHelper.STATUS_MENUNGGU)
                .put(PenjaminanMutuAnalisisHelper.STATUS_DISETUJUI)
                .put(PenjaminanMutuAnalisisHelper.STATUS_PERLU_REVISI);
        j.put("status", status);
        j.put("batasBaris", BATAS_BARIS);
        // Persetujuan/revisi memicu notifikasi dosen yang belum direproduksi.
        j.put("bolehMenilai", false);
    }

    private static void daftar(JSONObject j, HttpServletRequest r) throws Exception {
        String ta = text(r.getParameter("tahunAjaran"), "");
        String semester = text(r.getParameter("semester"), "");
        String saringStatus = text(r.getParameter("status"), "");

        JSONArray rows = new JSONArray();
        int menunggu = 0, disetujui = 0, revisi = 0;
        Session s = HibernateUtil.openSession();
        try {
            Criteria c = s.createCriteria(PertemuanPunyaUjian.class, "ppu")
                    .createAlias("ppu.pertemuan", "prt")
                    .createAlias("prt.perkuliahan", "pkl")
                    .add(Restrictions.isNotNull("ppu.formatNilais"))
                    .add(Restrictions.ne("ppu.formatNilais", Tugas.JSON))
                    .addOrder(Order.desc("ppu.id"))
                    .setMaxResults(BATAS_BARIS);
            if (ta.length() > 0) c.add(Restrictions.eq("pkl.tahunAjaran", ta));
            if (semester.length() > 0) c.add(Restrictions.ilike("pkl.ganjilGenap", semester));

            List<PertemuanPunyaUjian> daftar = new ArrayList<PertemuanPunyaUjian>();
            for (Object o : c.list()) daftar.add((PertemuanPunyaUjian) o);

            for (PertemuanPunyaUjian ppu : daftar) {
                String st = PenjaminanMutuAnalisisHelper.getStatus(ppu);
                if (PenjaminanMutuAnalisisHelper.STATUS_DISETUJUI.equals(st)) disetujui++;
                else if (PenjaminanMutuAnalisisHelper.STATUS_PERLU_REVISI.equals(st)) revisi++;
                else menunggu++;
                // Penyaringan status di memori: nilainya tersimpan dalam JSON,
                // bukan kolom terindeks (sama seperti layar ZK).
                if (saringStatus.length() > 0 && !saringStatus.equals(st)) continue;

                JSONObject row = new JSONObject();
                row.put("id", ppu.getId());
                row.put("ujian", nz(ppu.getNama()));
                row.put("status", st);
                row.put("catatan", catatan(ppu));
                try {
                    if (ppu.getPertemuan() != null && ppu.getPertemuan().getPerkuliahan() != null) {
                        row.put("tahunAjaran", nz(ppu.getPertemuan().getPerkuliahan().getTahunAjaran()));
                        row.put("semester", nz(ppu.getPertemuan().getPerkuliahan().getGanjilGenap()));
                        row.put("kelas", nz(ppu.getPertemuan().getPerkuliahan().getKelas()));
                        row.put("matakuliah",
                                ppu.getPertemuan().getPerkuliahan().getMatakuliah() == null ? ""
                                        : nz(ppu.getPertemuan().getPerkuliahan().getMatakuliah().getNama()));
                    }
                } catch (Exception ignored) {
                    // Relasi tidak lengkap tidak boleh menggagalkan seluruh daftar.
                }
                rows.put(row);
            }
        } finally { s.close(); }

        j.put("rows", rows).put("total", rows.length());
        j.put("jumlahMenunggu", menunggu).put("jumlahDisetujui", disetujui)
         .put("jumlahPerluRevisi", revisi);
        j.put("terpotong", menunggu + disetujui + revisi >= BATAS_BARIS);
    }

    /** Catatan penilai; tersimpan bersama status pada kolom JSON yang sama. */
    private static String catatan(PertemuanPunyaUjian ppu) {
        try {
            String json = ppu.getAnalisisCatatanJson();
            if (json == null || json.trim().length() == 0) return "";
            return new JSONObject(json).optString("catatan", "");
        } catch (Exception e) {
            return "";
        }
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
