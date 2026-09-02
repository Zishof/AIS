package ais.common.newui.dashboard;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/**
 * Rekap aktivitas pustakawan dalam sebuah rentang tanggal.
 *
 * <h3>Kuerinya disalin apa adanya</h3>
 * <p>SQL di bawah disalin baris demi baris dari
 * {@code DashboardAktiftasPustakawan.initSpreadsheet()}, bukan ditulis ulang.
 * Kuerinya menggabungkan enam sumber kegiatan pustakawan lewat rangkaian
 * {@code left join}; menyusun ulang kueri semacam itu dengan maksud yang sama
 * hampir pasti menghasilkan angka berbeda di kasus pinggir, dan tidak ada yang
 * menyadarinya sampai seseorang membandingkan dengan layar lama.</p>
 *
 * <h3>Batas atas tanggal bersifat eksklusif</h3>
 * <p>Layar lama menambah satu hari pada tanggal sampai, lalu memakai
 * {@code between mulai and sampai+1}. Perilaku itu dipertahankan persis.
 * Memakai tanggal sampai apa adanya akan diam-diam menghilangkan kegiatan pada
 * hari terakhir rentang.</p>
 *
 * <h3>Tentang penyisipan tanggal ke dalam SQL</h3>
 * <p>Kedua tanggal disisipkan ke teks SQL, sama seperti layar lama. Yang
 * disisipkan bukan masukan pengguna, melainkan hasil
 * {@code Common.databaseDateFormat} atas objek {@code Date} yang sudah diurai
 * lebih dulu; masukan yang bukan tanggal ditolak sebagai
 * {@code VALIDATION_FAILED} sebelum menyentuh kueri, sehingga tidak ada teks
 * dari pengguna yang pernah sampai ke SQL.</p>
 *
 * <h3>Baca saja</h3>
 * <p>Hanya {@code meta} dan {@code list}; layar ini memang hanya melaporkan.</p>
 */
public final class NewUiAktifitasPustakawanController {

    private static final String MODULE = "dashboard";

    private NewUiAktifitasPustakawanController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        JSONObject json = new JSONObject();
        try {
            String action = teks(request.getParameter("action"), "meta");
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

            if ("meta".equals(action)) meta(json);
            else daftar(json, request);
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            gagal(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            gagal(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            gagal(json, "INTERNAL_ERROR", "Rekap aktivitas gagal disiapkan.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiAktifitasPustakawanController"); }
            catch (Exception diabaikan) { }
        }
        tulis(response, json);
    }

    /** Nama kolom hasil, berurutan sama dengan select pada SQL. */
    static final String[] KOLOM = {
        "perpustakaan", "oleh", "usernama", "input_baru", "copy_item",
        "input_anggota", "peminjaman", "kembali",
    };

    private static void meta(JSONObject j) throws JSONException {
        j.put("judul", "Laporan Aktifitas Pustakawan");
        j.put("hanyaBaca", true);
        JSONArray kolom = new JSONArray();
        for (int i = 0; i < KOLOM.length; i++) kolom.put(KOLOM[i]);
        j.put("kolom", kolom);
        String hariIni = Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate());
        j.put("mulaiBawaan", hariIni);
        j.put("sampaiBawaan", hariIni);
    }

    @SuppressWarnings("unchecked")
    private static void daftar(JSONObject j, HttpServletRequest request) throws JSONException {
        String dateMulai = Common.databaseDateFormat.get().format(
                tanggal(request.getParameter("mulai"), "Tanggal Mulai"));

        // Sama seperti layar lama: batas atas ditambah satu hari sehingga
        // between mencakup seluruh hari terakhir rentang.
        Calendar tglSampai = ais.ui.util.WaktuUtil.getCalendar();
        tglSampai.setTime(tanggal(request.getParameter("sampai"), "Tanggal Sampai"));
        tglSampai.set(Calendar.DATE, tglSampai.get(Calendar.DATE) + 1);
        String dateSelesai = Common.databaseDateFormat.get().format(tglSampai.getTime());

        Session session = HibernateUtil.currentSession();
        String sql = "select cc.nama as perpustakaan,aa.oleh, bb.usernama, (case when a.qty is null then 0 else a.qty end) + (case when b.qty is null then 0 else b.qty end) as input_baru, "
                + " (case when c.qty is null then 0 else c.qty end) as copy_item, "
                + " (case when d.qty is null then 0 else d.qty end) as input_anggota, "
                + " (case when e.qty is null then 0 else e.qty end) as peminjaman, "
                + " (case when f.qty is null then 0 else f.qty end) as kembali " +

				" from ( " + " 	select oleh,perpustakaan " + " 	from( "
                + " 		select dibuat_oleh as oleh,perpustakaan from library.saldo_awal " + " 		union all  "
                + " 		select dibuat_oleh as oleh,perpustakaan from library.batch_item_punya_barcode "
                + " 		union all  " + " 		select dibuat_oleh as oleh,perpustakaan from library.anggota "
                + " 		union all  "
                + " 		select dibuat_oleh as oleh,perpustakaan from library.peminjaman_pengadaan_item "
                + " 		union all  "
                + " 		select dibuat_oleh as oleh,perpustakaan from library.kembali_pengadaan_item " + " 	) a "
                + " 	where oleh is not null and oleh != 'external_update' " + " 	and perpustakaan is not null "
                + " 	group by oleh,perpustakaan " + " ) as aa " + " inner join tbmuser bb on (aa.oleh = bb.userid) "
                + " inner join library.perpustakaan cc on (aa.perpustakaan = cc.id) " + " left join ( "
                + " 	select a.dibuat_oleh as oleh, count(*) as qty, perpustakaan from library.saldo_awal a  "
                + " 	inner join library.saldo_awal_detail b on (a.id = b.saldo_awal) "
                + " 	where a.tanggal_dirubah between date('" + dateMulai + "') and date('" + dateSelesai + "') "
                + " 	group by a.dibuat_oleh,perpustakaan "
                + " ) a on (a.oleh = aa.oleh and a.perpustakaan = aa.perpustakaan) " + " left join ( "
                + " 	select a.dibuat_oleh as oleh, count(*) as qty, perpustakaan from library.terima_pengadaan_item a  "
                + " 	inner join library.terima_pengadaan_item_detail b on (a.id = b.terima_pengadaan_item) "
                + " 	where a.tanggal_dirubah between date('" + dateMulai + "') and date('" + dateSelesai + "') "
                + " 	group by a.dibuat_oleh, perpustakaan "
                + " ) b on (b.oleh = aa.oleh and b.perpustakaan = aa.perpustakaan) " + " left join ( "
                + " 	select a.dibuat_oleh as oleh, count(*) as qty, b.perpustakaan from library.batch_item_punya_barcode a  "
                + " 	inner join library.item_punya_barcode b on (a.id = b.batch_item_punya_barcode) "
                + " 	where a.tanggal_dirubah between date('" + dateMulai + "') and date('" + dateSelesai + "') "
                + " 	group by a.dibuat_oleh, b.perpustakaan "
                + " ) c on (c.oleh = aa.oleh and c.perpustakaan = aa.perpustakaan) " + " left join ( "
                + " 	select a.dibuat_oleh as oleh, count(*) as qty, perpustakaan from library.anggota a  "
                + " 	where a.tanggal_dirubah between date('" + dateMulai + "') and date('" + dateSelesai + "') "
                + " 	group by a.dibuat_oleh, perpustakaan "
                + " ) d on (d.oleh = aa.oleh and d.perpustakaan = aa.perpustakaan) " + " left join ( "
                + " 	select a.dibuat_oleh as oleh, count(*) as qty, perpustakaan from library.peminjaman_pengadaan_item a  "
                + " 	inner join library.peminjaman_pengadaan_item_detail b on (a.id = b.peminjaman_pengadaan_item) "
                + " 	where a.tanggal_dirubah between date('" + dateMulai + "') and date('" + dateSelesai + "') "
                + " 	group by a.dibuat_oleh, perpustakaan "
                + " ) e on (e.oleh = aa.oleh and e.perpustakaan = aa.perpustakaan) " + " left join ( "
                + " 	select a.dibuat_oleh as oleh, count(*) as qty, perpustakaan from library.kembali_pengadaan_item a  "
                + " 	inner join library.kembali_pengadaan_item_detail b on (a.id = b.kembali_pengadaan_item) "
                + " 	where a.tanggal_dirubah between date('" + dateMulai + "') and date('" + dateSelesai + "') "
                + " 	group by a.dibuat_oleh, perpustakaan "
                + " ) f on (f.oleh = aa.oleh and f.perpustakaan = aa.perpustakaan)";

        List<Object[]> data = session.createSQLQuery(sql).list();
        JSONArray baris = new JSONArray();
        for (int i = 0; i < data.size(); i++) {
            Object[] d = data.get(i);
            JSONObject o = new JSONObject();
            for (int k = 0; k < KOLOM.length; k++) {
                Object nilai = k < d.length ? d[k] : null;
                o.put(KOLOM[k], nilai == null ? "" : nilai);
            }
            baris.put(o);
        }
        j.put("baris", baris);
        j.put("total", baris.length());
        j.put("mulai", dateMulai);
        j.put("sampai", dateSelesai);
    }

    /**
     * Urai tanggal, atau tolak.
     *
     * <p>Tidak ada nilai bawaan diam-diam: rentang yang salah ketik akan
     * menghasilkan rekap yang tampak wajar untuk periode yang bukan diminta.</p>
     */
    private static Date tanggal(String nilai, String label) {
        if (nilai == null || nilai.trim().length() == 0) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        try {
            return Common.databaseDateFormat.get().parse(nilai.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(label + " bukan tanggal yang sah.");
        }
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
