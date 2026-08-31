package ais.common.newui.akademik;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;

/**
 * Kontrak native layar "Pengajuan Sidang Skripsi/Tugas Akhir".
 *
 * <p>Menu ini URL-nya berupa kata kunci <code>daftar_sidang_atau_munaqosah</code>,
 * bukan berkas ZUL; {@code Common} memetakannya ke
 * {@code CommonUiFactoryHelper.tampilkanDaftarSkripsi()}. Layar itu bekerja
 * untuk MAHASISWA YANG SEDANG LOGIN: mengambil satu baris {@link Skripsi}
 * miliknya, atau menyiapkan baris kosong bila belum pernah mendaftar.</p>
 *
 * <p><b>Pelingkupan identitas.</b> Controller ini mengikuti aturan yang sama dan
 * tidak menerima parameter mahasiswa dari klien. Data selalu diambil dari
 * {@code Tbmuser.getMahasiswa()} pada sesi aktif, sehingga seorang mahasiswa
 * tidak dapat membaca pengajuan sidang milik orang lain dengan menebak id.
 * Pengguna tanpa relasi mahasiswa ditolak dengan pesan yang sama seperti ZK.</p>
 *
 * <p><b>Batas yang disengaja.</b> Kontrak ini hanya membaca status pengajuan.
 * Pendaftaran sidang melibatkan alur persetujuan pembimbing dan penjadwalan yang
 * belum direproduksi secara native; menyediakan tombol simpan di sini akan
 * menjanjikan proses yang belum ada. Pendaftaran tetap melalui layar ZK.</p>
 */
public final class NewUiSidangSkripsiController {

    private static final String MODULE = "root";

    private NewUiSidangSkripsiController() { }

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
            Mahasiswa mahasiswa = user.getMahasiswa();
            if (mahasiswa == null) throw new SecurityException("Anda harus login sebagai mahasiswa.");

            if ("meta".equals(action)) meta(json, mahasiswa);
            else if ("list".equals(action) || "get".equals(action)) pengajuan(json, mahasiswa);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) { response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage()); }
        catch (IllegalArgumentException e) { response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage()); }
        catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memuat pengajuan sidang. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiSidangSkripsiController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    private static void meta(JSONObject j, Mahasiswa mahasiswa) throws Exception {
        j.put("nim", nz(mahasiswa.getNim()));
        j.put("nama", nz(mahasiswa.getNama()));
        // Pendaftaran belum tersedia native; klien tidak boleh menampilkan
        // tombol daftar agar tidak menjanjikan proses yang belum ada.
        j.put("bolehDaftar", false);
    }

    /**
     * Satu baris pengajuan milik mahasiswa aktif. Bila belum ada, dikirim
     * {@code terdaftar:false} — bukan galat — karena pada layar ZK keadaan itu
     * berarti "silakan mendaftar", bukan kegagalan.
     */
    private static void pengajuan(JSONObject j, Mahasiswa mahasiswa) throws Exception {
        Session s = HibernateUtil.openSession();
        try {
            Skripsi skripsi = (Skripsi) s.createCriteria(Skripsi.class)
                    .add(Restrictions.eq("mahasiswa", mahasiswa))
                    .setMaxResults(1).uniqueResult();
            if (skripsi == null) {
                j.put("terdaftar", false);
                j.put("pesan", "Belum ada pengajuan sidang untuk mahasiswa ini.");
                return;
            }
            j.put("terdaftar", true);
            j.put("id", skripsi.getId());
            j.put("judul", nz(skripsi.getJudul()));
            j.put("jenis", nz(skripsi.getJenis()));
            j.put("semester", nz(skripsi.getSmt()));
            j.put("pembimbing", skripsi.getPembimbing() == null ? ""
                    : nz(skripsi.getPembimbing().getNama()));
            j.put("ketuaSidang", skripsi.getKetuaSidang() == null ? ""
                    : nz(skripsi.getKetuaSidang().getNama()));
            j.put("tanggalSidang", skripsi.getTanggalSidang() == null ? JSONObject.NULL
                    : skripsi.getTanggalSidang().getTime());
            j.put("waktuSidang", nz(skripsi.getWaktuSidang()));
            j.put("telahSidang", skripsi.getTelahSidang() != null
                    && skripsi.getTelahSidang().intValue() > 0);
            j.put("nilaiHuruf", nz(skripsi.getNilaiHuruf()));
            j.put("totalNilai", skripsi.getTotalNilai() == null ? JSONObject.NULL
                    : skripsi.getTotalNilai());
        } finally { s.close(); }
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
