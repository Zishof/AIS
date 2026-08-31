package ais.common.test;

import org.json.JSONObject;
import ais.common.JurnalAksesKatalog;
import ais.common.JurnalRoleMenuSynchronizer;

/**
 * Harness uji manual (bukan JUnit — dijalankan lewat method {@link #main(String[])} langsung, mirip
 * pola {@code TenantXxxSelfTest} pada paket {@code ais.service.tenant.test}) untuk memverifikasi
 * kebenaran katalog hak akses menu modul Jurnal ({@link JurnalAksesKatalog}) beserta sinkronisasi
 * menu fisik berbasis peran ({@link JurnalRoleMenuSynchronizer}). Kelas ini bukan bagian dari alur
 * aplikasi produksi maupun rangkaian uji otomatis (mis. Maven Surefire) — ia dijalankan manual oleh
 * pengembang (mis. lewat "Run As > Java Application" di IDE atau {@code java -cp ... main} dari
 * command line) sebagai pemeriksaan cepat/sanity-check setelah mengubah definisi katalog menu Jurnal
 * atau logika sinkronisasinya, sebelum perubahan tersebut diverifikasi lebih lanjut di lingkungan
 * UAT.
 *
 * <h2>Apa yang diverifikasi</h2>
 * <p>
 * Rangkaian pemeriksaan di {@link #main(String[])} mencakup, secara berurutan: (1) jumlah menu
 * kanonik pada {@link JurnalAksesKatalog#DAFTAR} harus tepat 28 entri — memastikan katalog menu
 * tidak sengaja bertambah/berkurang tanpa disadari; (2) representasi hak akses yang tidak valid
 * (JSON {@code null}, JSON kosong {@code "{}"} tanpa versi skema, atau versi skema tidak dikenal
 * seperti {@code schemaVersion:2}) harus selalu ditolak ({@code bolehMenu} mengembalikan
 * {@code false}) — menegaskan sikap "tolak secara default" (fail-closed) terhadap data hak akses
 * yang tidak dapat dipahami; (3) model default hasil {@link JurnalAksesKatalog#modelUntukEditor}
 * (representasi awal untuk editor hak akses peran) juga harus menolak semua menu secara default,
 * dan tidak memasang menu fisik apa pun lewat
 * {@link JurnalRoleMenuSynchronizer#desiredMenuIds(String)}; (4) begitu satu menu anak (mis.
 * {@code "journals"}) diaktifkan pada model editor, sinkronisasi menu fisik harus otomatis
 * menyertakan <b>baik menu induk maupun menu anak</b> (id {@code 2000460500} dan
 * {@code 2000460502}) — memverifikasi bahwa hierarki menu induk-anak diselesaikan otomatis, bukan
 * memerlukan pengaktifan eksplisit menu induk; (5) setelah menu {@code dashboard}, hak CRUD
 * {@code read} pada {@code dashboard}, dan hak workflow {@code viewAudit} diaktifkan, ketiganya
 * harus diizinkan oleh {@code bolehMenu}/{@code bolehCrud}/{@code bolehWorkflow} secara berurutan,
 * sementara kunci yang tidak dikenal ({@code "unknown"}) tetap ditolak; dan (6) nilai non-boolean
 * pada field menu (string {@code "true"} alih-alih literal boolean {@code true}) harus ditolak,
 * membuktikan validasi tipe data yang ketat pada representasi JSON hak akses, bukan sekadar
 * pengecekan "truthy" longgar.
 * </p>
 *
 * <p>
 * Konstruktor privat tanpa isi mencegah instansiasi kelas ini — seluruh anggotanya statis dan
 * dipanggil langsung lewat nama kelas.
 * </p>
 */
public final class JurnalAksesKatalogSelfTest {
    /** Konstruktor privat kosong untuk mencegah instansiasi; kelas ini murni kumpulan method statis. */
    private JurnalAksesKatalogSelfTest() {}

    /**
     * Menjalankan seluruh rangkaian pemeriksaan sanity-check katalog akses dan sinkronisasi menu
     * Jurnal secara berurutan lewat {@link #check(boolean, String)}. Setiap pemeriksaan yang gagal
     * langsung menghentikan eksekusi dengan melempar {@link IllegalStateException} berisi pesan
     * yang menjelaskan pemeriksaan mana yang gagal — cocok dipakai sebagai sinyal pass/fail
     * sederhana saat dijalankan manual dari command line/IDE. Bila seluruh pemeriksaan lolos, pesan
     * {@code "JurnalAksesKatalogSelfTest OK"} dicetak ke konsol sebagai penanda sukses.
     *
     * @param args argumen baris perintah, tidak dipakai
     * @throws Exception diteruskan dari operasi {@link JurnalAksesKatalog}/
     *                    {@link JurnalRoleMenuSynchronizer} yang dipanggil; {@link IllegalStateException}
     *                    dilempar oleh {@link #check(boolean, String)} sendiri saat satu pemeriksaan
     *                    gagal
     */
    public static void main(String[] args) throws Exception {
        check(JurnalAksesKatalog.DAFTAR.size() == 28, "28 menu canonical");
        check(!JurnalAksesKatalog.bolehMenu(null, "dashboard"), "null deny");
        check(!JurnalAksesKatalog.bolehMenu("{}", "dashboard"), "missing version deny");
        check(!JurnalAksesKatalog.bolehMenu("{\"schemaVersion\":2,\"menu\":{},\"crud\":{},\"workflow\":{}}", "dashboard"), "unknown version deny");
        JSONObject editor = JurnalAksesKatalog.modelUntukEditor(null);
        check(!JurnalAksesKatalog.bolehMenu(editor.toString(), "dashboard"), "editor defaults deny");
        check(JurnalRoleMenuSynchronizer.desiredMenuIds(editor.toString()).isEmpty(), "default deny tidak memasang menu fisik");
        editor.getJSONObject("menu").put("journals", true);
        check(JurnalRoleMenuSynchronizer.desiredMenuIds(editor.toString()).contains(Long.valueOf(2000460500L)), "parent jurnal otomatis");
        check(JurnalRoleMenuSynchronizer.desiredMenuIds(editor.toString()).contains(Long.valueOf(2000460502L)), "menu journals otomatis");
        editor.getJSONObject("menu").put("dashboard", true);
        editor.getJSONObject("crud").getJSONObject("dashboard").put("read", true);
        editor.getJSONObject("workflow").put("viewAudit", true);
        String raw = editor.toString();
        check(JurnalAksesKatalog.bolehMenu(raw, "dashboard"), "known menu allow");
        check(JurnalAksesKatalog.bolehCrud(raw, "dashboard", "read"), "known crud allow");
        check(JurnalAksesKatalog.bolehWorkflow(raw, "viewAudit"), "known workflow allow");
        check(!JurnalAksesKatalog.bolehMenu(raw, "unknown"), "unknown key deny");
        editor.getJSONObject("menu").put("dashboard", "true");
        check(!JurnalAksesKatalog.bolehMenu(editor.toString(), "dashboard"), "non boolean deny");
        System.out.println("JurnalAksesKatalogSelfTest OK");
    }
    /**
     * Memeriksa satu asersi tunggal: melempar {@link IllegalStateException} berisi {@code message}
     * bila {@code value} bernilai {@code false}, atau tidak melakukan apa pun bila {@code true}.
     * Berfungsi sebagai pengganti sederhana {@code assertTrue} pustaka JUnit di harness manual ini.
     *
     * @param value   hasil kondisi yang diperiksa; harus {@code true} agar pemeriksaan dianggap
     *                lolos
     * @param message pesan yang disertakan pada exception bila pemeriksaan gagal, menjelaskan
     *                pemeriksaan mana yang tidak terpenuhi
     */
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
