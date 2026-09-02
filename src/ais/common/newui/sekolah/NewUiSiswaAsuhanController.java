package ais.common.newui.sekolah;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;

/**
 * Daftar siswa asuhan seorang guru: Guru BK dan Guru Wali.
 *
 * <h3>Mengapa bukan Generic CRUD</h3>
 * <p>Kedua layar ini adalah batas privasi, bukan sekadar tampilan tersaring.
 * Generic CRUD menyusun penyaringnya dari masukan klien, dan penyekat yang
 * dikirim klien dapat dihilangkan klien. Di sini penyekatnya dihitung di server
 * dari identitas pengguna yang sedang masuk dan tidak dapat disentuh
 * permintaan: tidak ada satu pun parameter yang dapat melebarkannya.</p>
 *
 * <h3>Baca saja, dan sengaja</h3>
 * <p>Tidak ada aksi mutasi sama sekali. Penyuntingan siswa adalah milik layar
 * master siswa beserta seluruh aturannya; membuka mutasi lewat jalur bersekat
 * ini berarti menduplikasi aturan itu di tempat kedua yang lebih mudah terlewat
 * ketika aturannya berubah.</p>
 *
 * <h3>Kriterianya disalin, bukan disusun ulang</h3>
 * <p>Penyekatnya sama persis dengan yang dipakai {@code SiswaAction}: id siswa
 * diambil dari {@link KelasSiswaPunyaSiswa} yang {@code kelasSiswa.guruBk} atau
 * {@code kelasSiswa.guruPembina}-nya adalah guru bersangkutan. Menyusun ulang
 * kriteria yang sepertinya benar, misalnya menyaring langsung pada kolom
 * {@code Siswa.guruBk}, menghasilkan daftar yang tampak masuk akal namun berisi
 * anak yang salah, dan tidak ada gejala yang menandainya.</p>
 *
 * <p>Label Guru Wali pada layar lama merujuk kolom {@code guruPembina}.
 * Pemasangan itu dibaca dari {@code siswa.zul} dan dari kriteria
 * {@code SiswaAction.initCriteria}, bukan ditebak dari kemiripan nama.</p>
 *
 * <h3>Gagal tertutup</h3>
 * <p>Pengguna yang tidak terhubung ke satu pun data Guru ditolak, bukan
 * diperlakukan sebagai pengguna tanpa penyaring. Guru yang tidak mengasuh siapa
 * pun menerima daftar kosong, bukan seluruh siswa: himpunan id kosong berarti
 * kosong, bukan berarti batasnya tidak berlaku.</p>
 */
public final class NewUiSiswaAsuhanController {

    private static final String MODULE = "sekolah";

    /** Guru BK: penyekat pada {@code kelasSiswa.guruBk}. */
    public static final String MODE_BK = "bk";
    /** Guru Wali: penyekat pada {@code kelasSiswa.guruPembina}. */
    public static final String MODE_WALI = "wali";

    private static final int BATAS_HALAMAN = 50;

    private NewUiSiswaAsuhanController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String mode, String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        JSONObject json = new JSONObject();
        try {
            if (!MODE_BK.equals(mode) && !MODE_WALI.equals(mode)) {
                throw new IllegalArgumentException("Mode layar tidak dikenal.");
            }
            String action = teks(request.getParameter("action"), "meta");
            // Hanya aksi baca. Aksi lain ditolak sebelum apa pun dikerjakan.
            if (!"meta".equals(action) && !"list".equals(action)) {
                response.setStatus(405);
                gagal(json, "ACTION_NOT_ALLOWED", "Layar ini hanya menyediakan pembacaan.");
                tulis(response, json);
                return;
            }
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                gagal(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                tulis(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            Guru guru = user.ambilGuru();
            if (guru == null || guru.getId() == null) {
                // Bukan galat teknis: akun ini memang bukan guru, sehingga tidak
                // ada asuhan yang dapat ditampilkan. Menampilkan seluruh siswa di
                // sini persis kebocoran yang hendak dicegah layar ini.
                throw new SecurityException(
                        "Layar ini hanya untuk akun yang terhubung dengan data guru.");
            }

            if ("meta".equals(action)) meta(json, mode, guru);
            else daftar(json, request, mode, guru);
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            gagal(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            gagal(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            gagal(json, "INTERNAL_ERROR", "Daftar siswa gagal disiapkan.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiSiswaAsuhanController"); }
            catch (Exception diabaikan) { }
        }
        tulis(response, json);
    }

    private static void meta(JSONObject j, String mode, Guru guru) throws JSONException {
        j.put("judul", MODE_BK.equals(mode)
                ? "Siswa Bimbingan (Guru BK)" : "Siswa Asuhan (Guru Wali)");
        j.put("mode", mode);
        j.put("guru", guru.getNama() == null ? "" : guru.getNama());
        j.put("hanyaBaca", true);
        JSONArray kolom = new JSONArray();
        kolom.put(new JSONObject().put("nama", "nis").put("label", "NIS"));
        kolom.put(new JSONObject().put("nama", "nama").put("label", "Nama"));
        j.put("kolom", kolom);
        j.put("batasHalaman", BATAS_HALAMAN);
    }

    @SuppressWarnings("unchecked")
    private static void daftar(JSONObject j, HttpServletRequest request, String mode, Guru guru)
            throws JSONException {
        Session session = HibernateUtil.currentSession();

        // Himpunan id siswa asuhan, dihitung dari identitas guru pada sesi.
        // Tidak ada parameter permintaan yang ikut menentukannya.
        List<Long> idAsuhan = idAsuhan(session, mode, guru);

        JSONArray baris = new JSONArray();
        if (idAsuhan.isEmpty()) {
            // Kosong berarti kosong. Melewatkan pembatasan ketika himpunannya
            // kosong akan mengubah "tidak mengasuh siapa pun" menjadi "melihat
            // semua orang".
            j.put("baris", baris);
            j.put("total", 0);
            j.put("halaman", 0);
            return;
        }

        int halaman = angka(request.getParameter("page"), 0);
        if (halaman < 0) halaman = 0;
        String cari = teks(request.getParameter("q"), "").trim();

        Criteria kriteria = session.createCriteria(Siswa.class)
                .add(Restrictions.in("id", idAsuhan));
        Criteria penghitung = session.createCriteria(Siswa.class)
                .add(Restrictions.in("id", idAsuhan));
        if (cari.length() > 0) {
            kriteria.add(Restrictions.or(
                    Restrictions.ilike("nama", cari, MatchMode.ANYWHERE),
                    Restrictions.ilike("nis", cari, MatchMode.ANYWHERE)));
            penghitung.add(Restrictions.or(
                    Restrictions.ilike("nama", cari, MatchMode.ANYWHERE),
                    Restrictions.ilike("nis", cari, MatchMode.ANYWHERE)));
        }

        Object jumlah = penghitung.setProjection(Projections.rowCount()).uniqueResult();

        kriteria.addOrder(Order.asc("nama"));
        kriteria.setFirstResult(halaman * BATAS_HALAMAN);
        kriteria.setMaxResults(BATAS_HALAMAN);
        List<Siswa> siswa = kriteria.list();
        for (int i = 0; i < siswa.size(); i++) {
            Siswa s = siswa.get(i);
            baris.put(new JSONObject()
                    .put("id", s.getId())
                    .put("nis", s.getNis() == null ? "" : s.getNis())
                    .put("nama", s.getNama() == null ? "" : s.getNama()));
        }
        j.put("baris", baris);
        j.put("total", jumlah == null ? 0 : ((Number) jumlah).intValue());
        j.put("halaman", halaman);
    }

    /**
     * Id siswa asuhan seorang guru.
     *
     * <p>Dipisah agar dapat dipanggil uji mandiri tanpa melewati lapisan HTTP,
     * dan agar hanya ada satu tempat yang menuliskan kriterianya.</p>
     */
    /**
     * Properti yang menjadi penyekat untuk sebuah mode.
     *
     * <p>Dipisah supaya dapat diuji tanpa basis data. Menukar kedua properti
     * ini adalah kekeliruan paling berbahaya di berkas ini: layarnya tetap
     * tampil, jumlah barisnya tetap masuk akal, dan yang muncul adalah anak
     * asuh guru lain. Tidak ada gejala yang menandainya, jadi yang menjaganya
     * harus uji, bukan pembacaan ulang.</p>
     *
     * @throws IllegalArgumentException bila modenya tidak dikenal; tidak ada
     *         nilai bawaan, karena bawaan berarti menebak kolom penyekat
     */
    static String propertiPenyekat(String mode) {
        if (MODE_BK.equals(mode)) return "kelasSiswa.guruBk";
        if (MODE_WALI.equals(mode)) return "kelasSiswa.guruPembina";
        throw new IllegalArgumentException("Mode layar tidak dikenal: " + mode);
    }

    @SuppressWarnings("unchecked")
    public static List<Long> idAsuhan(Session session, String mode, Guru guru) {
        String properti = propertiPenyekat(mode);
        List<Object> hasil = session.createCriteria(KelasSiswaPunyaSiswa.class)
                .setProjection(Projections.distinct(Projections.property("siswa.id")))
                .createAlias("kelasSiswa", "kelasSiswa")
                .add(Restrictions.eq(properti, guru))
                .list();
        List<Long> id = new ArrayList<Long>();
        for (int i = 0; i < hasil.size(); i++) {
            Object nilai = hasil.get(i);
            if (nilai instanceof Long) id.add((Long) nilai);
        }
        return id;
    }

    private static int angka(String nilai, int bawaan) {
        try { return nilai == null ? bawaan : Integer.parseInt(nilai.trim()); }
        catch (Exception e) { return bawaan; }
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
