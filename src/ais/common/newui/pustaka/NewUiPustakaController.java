package ais.common.newui.pustaka;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.library.modern.LibraryMarcApi;
import ais.action.master.library.modern.LibraryOperationsApi;
import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.model.Tbmuser;

/**
 * Kontrak native modul pustaka: Katalogisasi MARCXML dan Operasional Modern.
 *
 * <p><b>Mengapa controller ini ada.</b> Kedua menu ZK-nya
 * (<code>/pages/master/library/katalogisasi_marc.zul</code> dan
 * <code>operasional_modern.zul</code>) ternyata hanya pembungkus
 * <code>&lt;iframe src="/ais/baru?p=pustaka&amp;s=..."&gt;</code>. Membiarkannya
 * berarti aplikasi desktop tetap bergantung pada halaman web eksternal.
 * Controller ini memindahkan keduanya ke jalur kontrak JSON yang sama dengan
 * layar native lain, sehingga klien Flutter cukup memanggil
 * <code>desktop-native-api</code>.</p>
 *
 * <p><b>Logika bisnis tidak ditulis ulang.</b> Modul pustaka sudah memiliki dua
 * kelas API yang matang — {@link LibraryMarcApi} dan {@link LibraryOperationsApi} —
 * lengkap dengan penjaga hak petugas, batas ukuran MARCXML, pemeriksaan CSRF
 * untuk mutasi, dan telemetri. Controller ini MENDELEGASIKAN ke keduanya;
 * menyalin ulang aturannya hanya akan melahirkan dua sumber kebenaran yang
 * lambat laun menyimpang.</p>
 *
 * <p><b>Lapisan penjaga bertambah, bukan berkurang.</b> Sebelum delegasi,
 * setiap permintaan melewati {@link NewUiRouteGuard} sehingga hak akses menu
 * dan peran ikut diperiksa — pemeriksaan yang tidak dilakukan endpoint
 * <code>/ais/baru</code>. Penjaga bawaan modul pustaka tetap berlaku sesudahnya.</p>
 *
 * <p><b>CSRF.</b> Aksi mutasi MARC (preview/import) menuntut token
 * {@link NewUiCsrfUtil}. Aksi {@code meta} menerbitkan token itu sehingga klien
 * desktop dapat melakukan bootstrap tanpa menyalin HTML, lalu mengirimkannya
 * kembali pada header {@code X-NUI-CSRF}. Token tidak pernah ikut tersimpan di
 * cache klien.</p>
 */
public final class NewUiPustakaController {

    private static final String MODULE = "library";

    /** Layar Katalogisasi MARCXML (impor/ekspor cantuman). */
    public static final String MODE_KATALOGISASI = "katalogisasi";
    /** Layar Operasional Modern (dasbor, serial, inventarisasi, denda). */
    public static final String MODE_OPERASIONAL = "operasional";

    private NewUiPustakaController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String mode, String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        JSONObject json = new JSONObject();
        try {
            if (!MODE_KATALOGISASI.equals(mode) && !MODE_OPERASIONAL.equals(mode)) {
                throw new IllegalArgumentException("Mode pustaka tidak dikenal.");
            }
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403); fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia."); write(response, json); return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");

            if ("meta".equals(action)) {
                meta(json, request, mode);
            } else {
                // Delegasi ke API modul pustaka yang sudah memegang seluruh
                // aturan bisnisnya sendiri.
                JSONObject hasil = MODE_KATALOGISASI.equals(mode)
                        ? LibraryMarcApi.handle(request)
                        : LibraryOperationsApi.handle(request);
                if (hasil == null) throw new IllegalStateException("Modul pustaka tidak mengembalikan jawaban.");
                // API pustaka memakai kunci `ok` yang sama; kegagalan di sana
                // diteruskan apa adanya beserta pesannya agar sebabnya jelas.
                if (!hasil.optBoolean("ok", false)) {
                    response.setStatus(422);
                    fail(json, "PUSTAKA_DITOLAK", hasil.optString("error", "Permintaan pustaka ditolak."));
                    write(response, json);
                    return;
                }
                // Salin muatan apa adanya supaya klien memakai bentuk yang sama
                // dengan modul pustaka, tanpa terjemahan yang bisa menyimpang.
                // Versi org.json pada proyek ini memakai keys() (Iterator),
                // bukan keySet().
                java.util.Iterator<?> kunci = hasil.keys();
                while (kunci.hasNext()) {
                    String k = String.valueOf(kunci.next());
                    if ("ok".equals(k)) continue;
                    json.put(k, hasil.get(k));
                }
            }
            json.put("ok", true);
        } catch (SecurityException e) { response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage()); }
        catch (IllegalArgumentException e) { response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage()); }
        catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memproses permintaan pustaka. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiPustakaController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    /**
     * Menjelaskan aksi yang tersedia beserta sifatnya, dan menerbitkan token
     * CSRF untuk bootstrap klien desktop.
     */
    private static void meta(JSONObject j, HttpServletRequest r, String mode) throws Exception {
        j.put("mode", mode);
        j.put("judul", MODE_KATALOGISASI.equals(mode)
                ? "Katalogisasi MARCXML" : "Operasional Perpustakaan");
        JSONArray baca = new JSONArray();
        JSONArray mutasi = new JSONArray();
        if (MODE_KATALOGISASI.equals(mode)) {
            baca.put("status").put("export");
            mutasi.put("preview").put("import");
            j.put("catatan", "Impor MARCXML dibatasi 2 MB dan hanya untuk petugas kataloger.");
        } else {
            baca.put("overview").put("serial_list").put("inventory_list")
                .put("comment_list").put("fine_list").put("health");
            mutasi.put("serial_claim").put("serial_resolve")
                  .put("inventory_create").put("inventory_scan").put("inventory_close")
                  .put("comment_add").put("comment_moderate")
                  .put("fine_payment").put("fine_waive").put("fine_reverse");
            j.put("catatan", "Aksi dasbor menuntut hak petugas perpustakaan.");
        }
        j.put("aksiBaca", baca).put("aksiMutasi", mutasi);
        // Token CSRF hanya untuk sesi berjalan; klien tidak boleh menyimpannya.
        j.put("csrf", NewUiCsrfUtil.getToken(r.getSession()));
        j.put("headerCsrf", NewUiCsrfUtil.HEADER);
    }

    private static String text(String v, String f) { return v == null || v.trim().length() == 0 ? f : v.trim(); }

    private static void fail(JSONObject j, String c, String m) throws Exception {
        j.put("ok", false).put("code", c).put("message", m == null ? "Operasi ditolak." : m);
    }

    private static void write(HttpServletResponse r, JSONObject j) throws Exception {
        r.getWriter().write(j.toString());
    }

    /** Dipakai self-test agar daftar mode tidak menyimpang dari menu ZK. */
    public static String[] semuaMode() {
        return new String[] { MODE_KATALOGISASI, MODE_OPERASIONAL };
    }
}
