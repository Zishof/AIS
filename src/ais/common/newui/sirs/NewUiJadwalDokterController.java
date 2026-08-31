package ais.common.newui.sirs;

import java.util.Calendar;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sirs.JadwalDokter;

/**
 * Kontrak native penjadwalan tenaga medis (SIRS).
 *
 * <p>Enam menu ZK di <code>/pages/master/sirs/jadwal_dokter/</code> —
 * penjadwalan_dokter, penjadwalan_lokasi, penjadwalan_poly, penjadwalan_umum,
 * lihat_jadwal, dan lihat_jadwal_bulanan — semuanya bekerja pada entity
 * {@link JadwalDokter} yang sama. Yang membedakan hanyalah SUDUT PANDANG:
 * jadwal dikelompokkan menurut dokter, lokasi, poli, atau ditampilkan apa
 * adanya, serta rentang waktu harian atau bulanan.</p>
 *
 * <p><b>Batas yang jujur.</b> Layar ZK aslinya adalah kalender interaktif
 * (geser-taruh, klik sel untuk menyunting). Kontrak ini SENGAJA hanya
 * menyediakan pembacaan jadwal beserta pengelompokannya; penyuntingan jadwal
 * tetap melalui layar ZK sampai editor kalender native tersedia. Menyediakan
 * mutasi lewat kontrak yang belum meniru validasi bentrok jadwal akan lebih
 * berbahaya daripada berguna.</p>
 *
 * <p><b>Rentang waktu.</b> Bila klien tidak mengirim rentang, controller memakai
 * bulan berjalan untuk mode bulanan dan tujuh hari ke depan untuk mode harian —
 * meniru kalender ZK yang selalu terbuka pada periode berjalan.</p>
 *
 * <p>Fail-closed: mode di luar daftar yang dikenal ditolak, sesi tanpa pengguna
 * ditolak, dan seluruh aksi selain baca tidak tersedia.</p>
 */
public final class NewUiJadwalDokterController {

    /**
     * Harus SAMA dengan awalan folder JSP sebelum {@code /uiux/}.
     * {@code NewUiRouteGuard.evaluate} membandingkan nilai ini dengan
     * {@code nui_native_module} hasil resolver; memakai "sirs" saja membuat
     * seluruh aksi dijawab ACTION_FORBIDDEN meski hak aksesnya ada.
     */
    private static final String MODULE = "sirs/jadwal_dokter";

    /** Pengelompokan menurut dokter (menu Penjadwalan Tenaga Medis). */
    public static final String MODE_DOKTER = "dokter";
    /** Pengelompokan menurut lokasi layanan. */
    public static final String MODE_LOKASI = "lokasi";
    /** Pengelompokan menurut poli. */
    public static final String MODE_POLI = "poli";
    /** Tanpa pengelompokan; seluruh jadwal pada rentang. */
    public static final String MODE_UMUM = "umum";
    /** Tampilan jadwal mingguan (baca saja). */
    public static final String MODE_LIHAT = "lihat";
    /** Tampilan jadwal bulanan (baca saja). */
    public static final String MODE_LIHAT_BULANAN = "lihat_bulanan";

    private static final int BATAS_BARIS = 500;

    private NewUiJadwalDokterController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String mode, String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!modeDikenal(mode)) throw new IllegalArgumentException("Mode jadwal tidak dikenal.");
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403); fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia."); write(response, json); return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            if ("meta".equals(action)) meta(json, mode);
            else if ("list".equals(action)) daftar(json, request, mode);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) { response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage()); }
        catch (IllegalArgumentException e) { response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage()); }
        catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memuat jadwal. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiJadwalDokterController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    public static boolean modeDikenal(String mode) {
        return MODE_DOKTER.equals(mode) || MODE_LOKASI.equals(mode) || MODE_POLI.equals(mode)
                || MODE_UMUM.equals(mode) || MODE_LIHAT.equals(mode) || MODE_LIHAT_BULANAN.equals(mode);
    }

    /** Mode bulanan memakai rentang satu bulan penuh; sisanya tujuh hari. */
    static boolean bulanan(String mode) { return MODE_LIHAT_BULANAN.equals(mode); }

    // ------------------------------------------------------------------ meta
    private static void meta(JSONObject j, String mode) throws Exception {
        j.put("mode", mode);
        j.put("judul", judul(mode));
        j.put("kelompokMenurut", kelompok(mode));
        j.put("bulanan", bulanan(mode));
        // Editor kalender belum tersedia secara native; klien tidak boleh
        // menampilkan tombol simpan/hapus agar tidak menjanjikan yang tak ada.
        j.put("bolehUbah", false);
        j.put("batasBaris", BATAS_BARIS);
    }

    static String judul(String mode) {
        if (MODE_DOKTER.equals(mode)) return "Penjadwalan Tenaga Medis";
        if (MODE_LOKASI.equals(mode)) return "Penjadwalan Lokasi";
        if (MODE_POLI.equals(mode)) return "Penjadwalan Poli";
        if (MODE_UMUM.equals(mode)) return "Kalender Penjadwalan";
        if (MODE_LIHAT_BULANAN.equals(mode)) return "Jadwal Bulanan";
        return "Jadwal Mingguan";
    }

    /** Properti yang dipakai klien untuk mengelompokkan baris. */
    static String kelompok(String mode) {
        if (MODE_DOKTER.equals(mode)) return "dokter";
        if (MODE_LOKASI.equals(mode)) return "lokasi";
        if (MODE_POLI.equals(mode)) return "poli";
        return "";
    }

    // ---------------------------------------------------------------- daftar
    private static void daftar(JSONObject j, HttpServletRequest r, String mode) throws Exception {
        Date mulai = tanggal(r, "mulai");
        Date sampai = tanggal(r, "sampai");
        if (mulai == null || sampai == null) {
            Calendar c = Calendar.getInstance();
            if (bulanan(mode)) {
                c.set(Calendar.DAY_OF_MONTH, 1);
                mulai = awalHari(c.getTime());
                c.add(Calendar.MONTH, 1); c.add(Calendar.DAY_OF_MONTH, -1);
                sampai = akhirHari(c.getTime());
            } else {
                mulai = awalHari(c.getTime());
                c.add(Calendar.DAY_OF_MONTH, 7);
                sampai = akhirHari(c.getTime());
            }
        }

        JSONArray rows = new JSONArray();
        Session s = HibernateUtil.openSession();
        try {
            Criteria c = s.createCriteria(JadwalDokter.class)
                    .add(Restrictions.le("jadwalDokterDimulai", sampai))
                    .add(Restrictions.ge("jadwalDokterSampai", mulai))
                    .addOrder(Order.asc("jadwalDokterDimulai"))
                    .setMaxResults(BATAS_BARIS);
            // Penyaringan opsional mengikuti filter kalender ZK.
            Long lokasiId = id(r, "lokasiId");
            if (lokasiId != null) c.add(Restrictions.eq("lokasi.id", lokasiId));
            Long dokterId = id(r, "dokterId");
            if (dokterId != null) c.add(Restrictions.eq("dokter.id", dokterId));
            Long polyId = id(r, "polyId");
            if (polyId != null) c.add(Restrictions.eq("poly.id", polyId));

            for (Object o : c.list()) {
                JadwalDokter jd = (JadwalDokter) o;
                JSONObject row = new JSONObject();
                row.put("id", jd.getId());
                row.put("dokter", jd.getDokter() == null ? "" : nz(String.valueOf(jd.getDokter())));
                row.put("lokasi", jd.getLokasi() == null ? "" : nz(String.valueOf(jd.getLokasi())));
                row.put("poli", jd.getPoly() == null ? "" : nz(String.valueOf(jd.getPoly())));
                row.put("shift", jd.getShift() == null ? "" : nz(String.valueOf(jd.getShift())));
                row.put("hari", nz(jd.getHari()));
                row.put("mulai", jd.getJadwalDokterDimulai() == null ? JSONObject.NULL
                        : jd.getJadwalDokterDimulai().getTime());
                row.put("sampai", jd.getJadwalDokterSampai() == null ? JSONObject.NULL
                        : jd.getJadwalDokterSampai().getTime());
                row.put("warna", nz(jd.getWarna()));
                row.put("keterangan", nz(jd.getKeterangan()));
                rows.put(row);
            }
        } finally { s.close(); }

        j.put("rows", rows).put("total", rows.length());
        j.put("mulai", mulai.getTime()).put("sampai", sampai.getTime());
        j.put("kelompokMenurut", kelompok(mode));
        // Batas baris disampaikan terbuka supaya klien tidak menyangka data utuh.
        j.put("terpotong", rows.length() >= BATAS_BARIS);
    }

    // ------------------------------------------------------------------- util
    private static Date awalHari(Date d) {
        Calendar c = Calendar.getInstance(); c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static Date akhirHari(Date d) {
        Calendar c = Calendar.getInstance(); c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    private static Date tanggal(HttpServletRequest r, String nama) {
        String v = r.getParameter(nama);
        if (v == null || v.trim().length() == 0) return null;
        try { return new Date(Long.parseLong(v.trim())); } catch (Exception ignored) { }
        try { return Common.databaseDateFormat.get().parse(v.trim()); } catch (Exception ignored) { }
        return null;
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

    /** Dipakai self-test agar daftar mode tidak menyimpang dari menu ZK. */
    public static String[] semuaMode() {
        return new String[] { MODE_DOKTER, MODE_LOKASI, MODE_POLI, MODE_UMUM,
                MODE_LIHAT, MODE_LIHAT_BULANAN };
    }
}
