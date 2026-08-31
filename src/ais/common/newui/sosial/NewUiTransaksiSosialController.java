package ais.common.newui.sosial;

import java.math.BigDecimal;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sosial.TransaksiDonasi;

/**
 * Kontrak native workspace transaksi sosial: Zakat, Infaq, Shodaqoh, dan Donasi.
 *
 * <p>Keempat menu ZK (<code>/pages/master/sosial/{zakat,infaq,shodaqoh,donasi}_workspace.zul</code>)
 * hanya membungkus satu layar bersama <code>transaksi_sosial_workspace.zul</code>
 * dengan atribut <code>kategori</code> yang berbeda; seluruh logikanya berada di
 * {@code ais.action.master.sosial.SosialTransaksiAction}. Karena itu satu
 * controller melayani keempatnya, dibedakan parameter kategori.</p>
 *
 * <p><b>Mengapa bukan scaffold Generic CRUD.</b> Daftar transaksi WAJIB disaring
 * menurut kode jenis dana dan tenant. Scaffold generik akan menampilkan seluruh
 * kategori bercampur — zakat tampak pada layar donasi dan sebaliknya — sehingga
 * pemetaan naif justru menyesatkan.</p>
 *
 * <p><b>Aturan penyaringan disalin apa adanya dari layar ZK</b>
 * ({@code SosialTransaksiAction.addFundFilter} dan {@code initCriteria}):</p>
 * <ul>
 *   <li>INFAQ  → kode jenis dana mengandung "INFAQ" atau "INFAK";</li>
 *   <li>SHODAQOH → mengandung "SHODAQ" atau "SEDEKAH";</li>
 *   <li>ZAKAT / DONASI → mengandung nama kategorinya sendiri;</li>
 *   <li>selalu dibatasi <code>tenantKey</code> milik pengguna aktif;</li>
 *   <li>penyaringan opsional: nomor transaksi, nama donatur, dan status.</li>
 * </ul>
 *
 * <p>Ringkasan dasbor memakai perhitungan yang sama dengan ZK: jumlah transaksi,
 * total nominal berstatus ALLOCATED/PAID, serta cacah PENDING_PAYMENT dan
 * ALLOCATED.</p>
 *
 * <p><b>Fail-closed.</b> Kategori di luar keempat nilai yang dikenal ditolak,
 * sesi tanpa pengguna ditolak, dan controller ini SENGAJA hanya menyediakan aksi
 * baca — pembuatan serta perubahan transaksi sosial tetap melalui layar ZK yang
 * memegang SocialPrivilegeGuard dan alur pembayarannya.</p>
 */
public final class NewUiTransaksiSosialController {

    private static final String MODULE = "sosial";

    public static final String KATEGORI_ZAKAT = "ZAKAT";
    public static final String KATEGORI_INFAQ = "INFAQ";
    public static final String KATEGORI_SHODAQOH = "SHODAQOH";
    public static final String KATEGORI_DONASI = "DONASI";

    /** Batas baris per halaman; sama dengan paging layar ZK. */
    private static final int UKURAN_HALAMAN = 25;

    private NewUiTransaksiSosialController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String kategori, String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!sahKategori(kategori)) throw new IllegalArgumentException("Kategori sosial tidak valid.");
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403); fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia."); write(response, json); return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            if ("meta".equals(action)) meta(json, kategori);
            else if ("list".equals(action)) daftar(json, request, kategori);
            else if ("ringkasan".equals(action)) ringkasan(json, request, kategori);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) { response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage()); }
        catch (IllegalArgumentException e) { response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage()); }
        catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memuat transaksi sosial. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiTransaksiSosialController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    private static boolean sahKategori(String k) {
        return KATEGORI_ZAKAT.equals(k) || KATEGORI_INFAQ.equals(k)
                || KATEGORI_SHODAQOH.equals(k) || KATEGORI_DONASI.equals(k);
    }

    // ------------------------------------------------------------------ meta
    /** Judul layar dan pilihan status; sama dengan combobox pada layar ZK. */
    private static void meta(JSONObject j, String kategori) throws Exception {
        j.put("kategori", kategori);
        j.put("judul", judul(kategori));
        JSONArray status = new JSONArray().put("");
        String[] pilihan = { "DRAFT", "PENDING_PAYMENT", "ALLOCATED", "CANCELLED" };
        for (int i = 0; i < pilihan.length; i++) status.put(pilihan[i]);
        j.put("status", status);
        j.put("ukuranHalaman", UKURAN_HALAMAN);
        // Hanya baca: klien tidak boleh menampilkan tombol simpan/hapus.
        j.put("bolehUbah", false);
    }

    // ----------------------------------------------------------------- daftar
    private static void daftar(JSONObject j, HttpServletRequest r, String kategori) throws Exception {
        int halaman = Math.max(1, angka(r, "page", 1));
        JSONArray rows = new JSONArray();
        Session s = HibernateUtil.openSession();
        try {
            Criteria c = kriteria(s, r, kategori).addOrder(Order.desc("createdAt"))
                    .setFirstResult((halaman - 1) * UKURAN_HALAMAN)
                    .setMaxResults(UKURAN_HALAMAN);
            for (Object o : c.list()) {
                TransaksiDonasi t = (TransaksiDonasi) o;
                JSONObject row = new JSONObject();
                row.put("id", t.getId());
                row.put("nomor", nz(t.getTransactionNumber()));
                row.put("donatur", t.getAnonymous() != null && t.getAnonymous().booleanValue()
                        ? "(anonim)" : nz(t.getDonorNameSnapshot()));
                row.put("jenisDana", t.getFundType() == null ? "" : nz(t.getFundType().getNama()));
                row.put("status", nz(t.getStatus()));
                row.put("nominal", nilai(t.getGrossDonationAmount()));
                row.put("dibayarPada", t.getPaidAt() == null ? JSONObject.NULL : t.getPaidAt().getTime());
                row.put("dibuatPada", t.getCreatedAt() == null ? JSONObject.NULL : t.getCreatedAt().getTime());
                rows.put(row);
            }
            Number total = (Number) kriteria(s, r, kategori)
                    .setProjection(Projections.rowCount()).uniqueResult();
            j.put("total", total == null ? 0 : total.longValue());
        } finally { s.close(); }
        j.put("rows", rows).put("page", halaman).put("size", UKURAN_HALAMAN);
    }

    // --------------------------------------------------------------- ringkasan
    /** Empat angka dasbor ZK: cacah, total nominal terbayar, pending, teralokasi. */
    private static void ringkasan(JSONObject j, HttpServletRequest r, String kategori) throws Exception {
        Session s = HibernateUtil.openSession();
        try {
            Number cacah = (Number) kriteria(s, r, kategori)
                    .setProjection(Projections.rowCount()).uniqueResult();
            BigDecimal nominal = (BigDecimal) kriteria(s, r, kategori)
                    .add(Restrictions.in("status", new String[] { "ALLOCATED", "PAID" }))
                    .setProjection(Projections.sum("grossDonationAmount")).uniqueResult();
            Number pending = (Number) kriteria(s, r, kategori)
                    .add(Restrictions.eq("status", "PENDING_PAYMENT"))
                    .setProjection(Projections.rowCount()).uniqueResult();
            Number teralokasi = (Number) kriteria(s, r, kategori)
                    .add(Restrictions.eq("status", "ALLOCATED"))
                    .setProjection(Projections.rowCount()).uniqueResult();
            j.put("totalTransaksi", cacah == null ? 0 : cacah.longValue());
            j.put("totalNominal", nominal == null ? 0d : nominal.doubleValue());
            j.put("totalPending", pending == null ? 0 : pending.longValue());
            j.put("totalTeralokasi", teralokasi == null ? 0 : teralokasi.longValue());
        } finally { s.close(); }
    }

    // ---------------------------------------------------------------- kriteria
    /**
     * Menyusun kriteria yang identik dengan {@code SosialTransaksiAction}:
     * alias jenis dana, penyaringan kategori, batas tenant, lalu penyaringan
     * opsional nomor/donatur/status.
     */
    private static Criteria kriteria(Session s, HttpServletRequest r, String kategori) {
        Criteria c = s.createCriteria(TransaksiDonasi.class).createAlias("fundType", "f");
        saringKategori(c, kategori);
        String tenant = text(r.getParameter("tenantKey"), "");
        if (tenant.length() > 0) c.add(Restrictions.eq("tenantKey", tenant));
        String nomor = text(r.getParameter("nomor"), "");
        if (nomor.length() > 0) c.add(Restrictions.ilike("transactionNumber", nomor, MatchMode.ANYWHERE));
        String donatur = text(r.getParameter("donatur"), "");
        if (donatur.length() > 0) c.add(Restrictions.ilike("donorNameSnapshot", donatur, MatchMode.ANYWHERE));
        String status = text(r.getParameter("status"), "");
        if (status.length() > 0) c.add(Restrictions.eq("status", status));
        return c;
    }

    /** Penyaringan kode jenis dana; ejaan alternatif mengikuti layar ZK. */
    private static void saringKategori(Criteria c, String kategori) {
        if (KATEGORI_INFAQ.equals(kategori)) {
            c.add(Restrictions.or(Restrictions.ilike("f.kode", "%INFAQ%"),
                    Restrictions.ilike("f.kode", "%INFAK%")));
        } else if (KATEGORI_SHODAQOH.equals(kategori)) {
            c.add(Restrictions.or(Restrictions.ilike("f.kode", "%SHODAQ%"),
                    Restrictions.ilike("f.kode", "%SEDEKAH%")));
        } else {
            c.add(Restrictions.ilike("f.kode", "%" + kategori + "%"));
        }
    }

    /** Judul layar; "SHODAQOH" ditulis "Shodaqoh" seperti pada ZK. */
    static String judul(String kategori) {
        if (KATEGORI_SHODAQOH.equals(kategori)) return "Shodaqoh";
        return kategori.substring(0, 1) + kategori.substring(1).toLowerCase();
    }

    // ------------------------------------------------------------------- util
    private static double nilai(BigDecimal v) { return v == null ? 0d : v.doubleValue(); }

    private static int angka(HttpServletRequest r, String nama, int bawaan) {
        String v = r.getParameter(nama);
        if (v == null || v.trim().length() == 0) return bawaan;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return bawaan; }
    }

    private static String nz(String v) { return v == null ? "" : v; }

    private static String text(String v, String f) { return v == null || v.trim().length() == 0 ? f : v.trim(); }

    private static void fail(JSONObject j, String c, String m) throws Exception {
        j.put("ok", false).put("code", c).put("message", m == null ? "Operasi ditolak." : m);
    }

    private static void write(HttpServletResponse r, JSONObject j) throws Exception {
        r.getWriter().write(j.toString());
    }

    /** Dipakai self-test agar daftar kategori tidak menyimpang dari ZK. */
    public static String[] semuaKategori() {
        return new String[] { KATEGORI_ZAKAT, KATEGORI_INFAQ, KATEGORI_SHODAQOH, KATEGORI_DONASI };
    }

    /** Dipakai self-test: apakah kategori diterima controller. */
    public static boolean kategoriDikenal(String kategori) { return sahKategori(kategori); }

    /** Tanggal terakhir dipakai untuk penamaan berkas ekspor bila kelak ada. */
    static String cap(Date d) { return d == null ? "" : Common.databaseDateFormat.get().format(d); }
}
