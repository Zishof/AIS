package ais.common.newui.koperasi;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.koperasi.helper.LaporanKeuanganCoaHelper;
import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.JenisLaporan;

/**
 * Kontrak native Laporan Keuangan Koperasi (Neraca / Laba Rugi / Arus Kas).
 *
 * <p>Laporan ini disusun dari buku besar terposting memakai pemetaan akun yang
 * dikonfigurasi di modul akuntansi ({@code KelompokLaporan}). Strukturnya —
 * grup, baris, akun rinci, dan subtotal — dihasilkan
 * {@link LaporanKeuanganCoaHelper#susun} yang juga dipakai layar ZK, sehingga
 * angka pada kedua layar berasal dari perhitungan yang sama persis.</p>
 *
 * <p><b>Yang dikirim adalah angka, bukan HTML.</b> Layar ZK merangkai tabel
 * HTML lengkap dengan gaya; kontrak ini mengirim strukturnya saja agar klien
 * menggambar tabelnya secara native. Itu pula sebabnya nilai akun rinci kini
 * disimpan sebagai bilangan di helper, bukan teks rupiah yang sudah diformat.</p>
 *
 * <p><b>Batas yang jujur.</b> Layar ZK-nya juga memuat dua tampilan tambahan:
 * dasbor ikhtisar simpan-pinjam dan Catatan atas Laporan Keuangan (CALK).
 * Keduanya adalah rangkaian kartu dan narasi HTML yang belum dipisahkan dari
 * penyajiannya, sehingga BELUM tersedia di sini; kontrak ini menyajikan
 * laporan keuangan resminya saja. Klien wajib membaca {@code bagianTersedia}
 * ketimbang mengandaikan seluruh layar sudah tercakup.</p>
 *
 * <p>Ikhtisar penyeimbang (total aktiva vs pasiva, atau pendapatan vs beban)
 * dihitung di server dengan aturan pengelompokan yang sama seperti ZK agar
 * klien tidak perlu menebak grup mana yang termasuk aktiva.</p>
 */
public final class NewUiLaporanKeuanganKoperasiController {

    /** Harus sama dengan awalan folder JSP sebelum {@code /uiux/}. */
    private static final String MODULE = "koperasi";

    private NewUiLaporanKeuanganKoperasiController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response, String pageKey)
            throws Exception {
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
            if ("meta".equals(action)) meta(json, session);
            else if ("list".equals(action)) daftar(json, request, session);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal menyusun laporan keuangan. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLaporanKeuanganKoperasiController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    // ------------------------------------------------------------------ meta

    @SuppressWarnings("unchecked")
    private static void meta(JSONObject j, Session session) throws Exception {
        j.put("judul", "Laporan Keuangan Koperasi");
        JSONArray tersedia = new JSONArray();
        tersedia.put("laporan_resmi");
        j.put("bagianTersedia", tersedia);
        j.put("catatanCakupan", "Dasbor ikhtisar dan Catatan atas Laporan Keuangan (CALK) belum tersedia "
                + "secara native; keduanya masih pada layar lama.");

        // Pilihan jenis laporan diambil dari yang benar-benar punya pemetaan
        // akun; jenis tanpa pemetaan akan menghasilkan laporan kosong.
        List<JenisLaporan> jenis = session.createQuery(
                "select distinct kl.jenisLaporan from KelompokLaporan kl where kl.jenisLaporan is not null").list();
        // PostgreSQL menolak DISTINCT dengan ORDER BY kolom di luar select list,
        // jadi pengurutan dilakukan di Java — sama seperti layar ZK.
        java.util.Collections.sort(jenis, new java.util.Comparator<JenisLaporan>() {
            public int compare(JenisLaporan a, JenisLaporan b) {
                String na = (a == null || a.getNama() == null) ? "" : a.getNama();
                String nb = (b == null || b.getNama() == null) ? "" : b.getNama();
                return na.compareToIgnoreCase(nb);
            }
        });
        JSONArray pilihan = new JSONArray();
        for (JenisLaporan jl : jenis) {
            pilihan.put(new JSONObject().put("id", jl.getId())
                    .put("nama", jl.getNama() == null ? "(tanpa nama)" : jl.getNama())
                    .put("kumulatif", LaporanKeuanganCoaHelper.kumulatif(jl)));
        }
        j.put("pilihanJenis", pilihan);
        if (pilihan.length() == 0) {
            j.put("catatan", "Belum ada jenis laporan keuangan yang terkonfigurasi di modul akuntansi.");
        }

        // Periode bawaan sama dengan ZK: awal tahun berjalan s/d hari ini.
        Calendar c = ais.ui.util.WaktuUtil.getCalendar();
        j.put("sampaiBawaan", tanggal(c.getTime()));
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DAY_OF_MONTH, 1);
        j.put("mulaiBawaan", tanggal(c.getTime()));
    }

    // ------------------------------------------------------------------ isi

    private static void daftar(JSONObject j, HttpServletRequest request, Session session) throws Exception {
        Long id = idWajib(request.getParameter("jenis"), "Jenis laporan belum dipilih.");
        JenisLaporan jl = (JenisLaporan) session.get(JenisLaporan.class, id);
        if (jl == null) throw new IllegalArgumentException("Jenis laporan tidak ditemukan.");

        Date mulai = urai(request.getParameter("mulai"));
        Date sampai = urai(request.getParameter("sampai"));
        boolean kumulatif = LaporanKeuanganCoaHelper.kumulatif(jl);

        LinkedHashMap<Long, LaporanKeuanganCoaHelper.Grup> grupMap =
                LaporanKeuanganCoaHelper.susun(session, jl, mulai, sampai);

        double aset = 0, pasiva = 0, pendapatan = 0, beban = 0;
        JSONArray grup = new JSONArray();
        for (LaporanKeuanganCoaHelper.Grup g : grupMap.values()) {
            JSONArray baris = new JSONArray();
            for (LaporanKeuanganCoaHelper.Baris b : g.baris) {
                JSONArray rincian = new JSONArray();
                for (LaporanKeuanganCoaHelper.Rincian r : b.rincian) {
                    rincian.put(new JSONObject().put("nama", r.nama).put("nilai", r.nilai));
                }
                baris.put(new JSONObject().put("label", b.label).put("nilai", b.nilai).put("rincian", rincian));
            }
            double total = g.total();
            grup.put(new JSONObject().put("nama", g.nama)
                    .put("keterangan", g.keterangan == null ? "" : g.keterangan)
                    .put("baris", baris).put("total", total));

            // Pengelompokan ikhtisar mengikuti aturan yang sama dengan ZK.
            String low = g.nama == null ? "" : g.nama.toLowerCase();
            if (low.contains("aktiva") || low.contains("aset")) {
                aset += total;
            } else if (low.contains("kewajiban") || low.contains("hutang") || low.contains("modal")
                    || low.contains("ekuitas") || low.contains("pasiva")) {
                pasiva += total;
            } else if (low.contains("pendapat") || low.contains("penerimaan")) {
                pendapatan += total;
            } else if (low.contains("beban") || low.contains("biaya")) {
                beban += total;
            }
        }

        j.put("judul", jl.getNama() == null ? "Laporan Keuangan" : jl.getNama());
        j.put("kumulatif", kumulatif);
        j.put("mulai", tanggal(mulai));
        j.put("sampai", tanggal(sampai));
        j.put("grup", grup);

        JSONObject ikhtisar = new JSONObject();
        if (kumulatif && (aset != 0.0 || pasiva != 0.0)) {
            ikhtisar.put("jenis", "neraca");
            ikhtisar.put("totalAktiva", aset);
            ikhtisar.put("totalPasiva", pasiva);
            // Ambang 1 rupiah sama dengan layar ZK: selisih pembulatan sen tidak
            // dianggap sebagai neraca yang tidak seimbang.
            ikhtisar.put("seimbang", Math.abs(aset - pasiva) < 1.0);
            ikhtisar.put("selisih", aset - pasiva);
        } else if (!kumulatif && (pendapatan != 0.0 || beban != 0.0)) {
            ikhtisar.put("jenis", "hasil_usaha");
            ikhtisar.put("pendapatan", pendapatan);
            ikhtisar.put("beban", beban);
            ikhtisar.put("sisaHasilUsaha", pendapatan - beban);
        } else {
            ikhtisar.put("jenis", "tidak_ada");
        }
        j.put("ikhtisar", ikhtisar);
    }

    // ------------------------------------------------------------------ util

    private static Date urai(String nilai) {
        String v = text(nilai, "").trim();
        if (!v.matches("\\d{4}-\\d{2}-\\d{2}")) return null;
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(v);
        } catch (Exception e) {
            return null;
        }
    }

    private static String tanggal(Date d) {
        return d == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd").format(d);
    }

    private static Long idWajib(String nilai, String pesan) {
        String v = text(nilai, "").trim();
        try {
            long l = Long.parseLong(v);
            if (l <= 0) throw new NumberFormatException();
            return Long.valueOf(l);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(pesan);
        }
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
