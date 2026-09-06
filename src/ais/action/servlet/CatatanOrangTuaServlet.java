package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;

import ais.common.Common;
import ais.common.security.SiswaCatatanOrtuTokenService;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Siswa;

/**
 * Endpoint publik tanpa login untuk halaman "Catatan Orang Tua" — memungkinkan orang tua/wali
 * melihat catatan aktivitas harian anaknya dan memberi tanggapan, tanpa perlu akun/login AIS.
 *
 * <p>URL: {@code /AktiftasHarianSiswa?token={TOKEN}}. Servlet ini memvalidasi {@code TOKEN}
 * lewat {@link SiswaCatatanOrtuTokenService#cariSiswaByToken} (format hex, cocok dengan hash
 * tersimpan, belum kedaluwarsa), menyimpan id {@link Siswa} yang ditemukan di HTTP session
 * (lihat {@link #SK_SISWA_ID}), lalu mengarahkan (redirect) ke halaman ZUL
 * {@code common/catatan_orang_tua_terhadap_aktiftas_harian_siswa.zul} yang composer-nya
 * ({@code CatatanOrangTuaAktiftasHarianAction}) membaca id tersebut dari session untuk
 * menampilkan data siswa yang bersangkutan.</p>
 *
 * <p><b>Riwayat keamanan &mdash; parameter {@code siswa} lama (id mentah) SUDAH DIHAPUS:</b>
 * versi sebelumnya menerima parameter {@code siswa={ID_SISWA}} dan hanya memeriksa bahwa baris
 * {@link Siswa} dengan id tersebut ada — TIDAK ADA token/secret per-siswa, TIDAK ADA pengecekan
 * relasi orang tua-anak sama sekali (bukan sekadar gerbang lemah, melainkan benar-benar absen).
 * Karena id siswa adalah primary key yang umumnya berurutan, siapa pun dapat mengubah nilai
 * {@code siswa} pada URL untuk membaca catatan aktivitas harian, pesan pembina/guru, dan
 * riwayat komentar siswa LAIN yang bukan anaknya, bahkan mengirim tanggapan seolah-olah sebagai
 * orang tua siswa tersebut. Perbaikan: parameter {@code siswa} DIGANTI TOTAL dengan
 * {@code token} acak 256-bit tak-tertebak per-siswa (lihat {@link SiswaCatatanOrtuTokenService}),
 * dicocokkan lewat hash-nya sebelum data siswa mana pun ditampilkan. TIDAK ADA masa transisi:
 * tautan lama berparameter {@code siswa} langsung ditolak (lihat {@link #process}) sejak
 * perbaikan ini berlaku — bukan kelonggaran yang disengaja, melainkan konsekuensi dari
 * digantinya total mekanisme identifikasi; sekolah perlu menerbitkan &amp; membagikan ulang
 * tautan bertoken ke orang tua/wali (lewat {@link SiswaCatatanOrtuTokenService#terbitkanToken}).
 * Belum ada layar admin yang memanggil {@code terbitkanToken} dan menampilkan tautan jadi untuk
 * disalin staf — itu tugas terpisah yang masih perlu dikerjakan sebelum tautan baru dapat mulai
 * dibagikan ke orang tua/wali.</p>
 */
public class CatatanOrangTuaServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Key atribut HTTP session tempat {@link #process} menyimpan id {@link Siswa} yang sedang
     * dilihat, agar dapat dibaca kembali oleh composer ZUL
     * {@code CatatanOrangTuaAktiftasHarianAction} pada request berikutnya dalam sesi browser
     * yang sama. Nilai yang tersimpan HANYA divalidasi berupa id yang ada di database (lihat
     * catatan keamanan pada Javadoc kelas) — bukan bukti kepemilikan/relasi orang tua-anak.
     */
    public static final String SK_SISWA_ID = "catatan_pub_siswa_id";

    /**
     * Menangani GET dengan mendelegasikan langsung ke {@link #process}; parameter dan alur
     * identik untuk GET maupun POST karena tautan yang dibagikan ke orang tua berbentuk URL
     * biasa (GET).
     *
     * @param req request HTTP masuk; parameter {@code token} berisi token akses siswa yang
     *            diminta
     * @param res response HTTP keluar; berisi redirect ke halaman ZUL bila valid, atau halaman
     *            error HTML sederhana bila tidak
     * @throws ServletException tidak pernah dilempar dari {@link #process} pada praktiknya,
     *                          dipertahankan karena tanda tangan {@link HttpServlet#doGet}
     * @throws IOException      bila penulisan redirect/respons gagal
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        process(req, res);
    }

    /**
     * Menangani POST dengan perilaku identik seperti {@link #doGet}: mendelegasikan langsung
     * ke {@link #process}.
     *
     * @param req request HTTP masuk; parameter sama seperti pada {@link #doGet}
     * @param res response HTTP keluar; sama seperti pada {@link #doGet}
     * @throws ServletException idem {@link #doGet}
     * @throws IOException      idem {@link #doGet}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        process(req, res);
    }

    /**
     * Memvalidasi parameter {@code token} dan, bila cocok dengan siswa yang belum kedaluwarsa,
     * menyimpan id siswa itu ke HTTP session lalu mengarahkan browser ke halaman ZUL Catatan
     * Orang Tua.
     *
     * <p>Alur: (1) tolak dengan halaman error generik (lewat {@link #sendError}) bila parameter
     * {@code token} kosong/tidak ada; (2) buka sesi Hibernate baru dan cari {@link Siswa} lewat
     * {@link SiswaCatatanOrtuTokenService#cariSiswaByToken} — method itu sendiri yang memvalidasi
     * format token, mencocokkan hash-nya, dan menegakkan kedaluwarsa opsional; (3) bila TIDAK ada
     * siswa yang cocok, balas dengan pesan generik yang SAMA baik untuk token kosong, berformat
     * salah, tidak ditemukan, maupun kedaluwarsa (mencegah oracle yang membedakan alasan
     * penolakan); (4) bila cocok, simpan id siswa ke atribut session {@link #SK_SISWA_ID}
     * (membuat HTTP session baru bila belum ada) dan redirect ke
     * {@code common/catatan_orang_tua_terhadap_aktiftas_harian_siswa.zul} relatif terhadap
     * context path; (5) exception apa pun ditangkap, dicatat lewat
     * {@link ais.common.ErrorAuditUtil}, dan dibalas sebagai halaman error generik; (6) sesi
     * Hibernate selalu dibersihkan (clear/disconnect/close) di blok {@code finally}.</p>
     *
     * @param req request HTTP masuk; parameter {@code token} berisi token akses siswa (hex)
     *            yang diterbitkan lewat {@link SiswaCatatanOrtuTokenService#terbitkanToken}
     * @param res response HTTP keluar; diisi redirect (302) ke halaman ZUL bila sukses, atau
     *            halaman HTML error sederhana (lewat {@link #sendError}) bila gagal pada
     *            langkah mana pun
     * @throws ServletException tidak pernah dilempar keluar; dipertahankan karena tanda tangan
     *                          method pemanggil
     * @throws IOException      bila penulisan redirect atau halaman error gagal
     */
    private void process(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        Session session = null;
        try {
            String tokenParam = req.getParameter("token");

            // Pesan generik yang SAMA untuk setiap kegagalan (kosong/format salah/tidak
            // ditemukan/kedaluwarsa/parameter siswa lama) -- lihat catatan keamanan pada Javadoc
            // class mengenai penggantian total mekanisme identifikasi.
            final String pesanTolak = "Tautan tidak valid atau sudah tidak berlaku. "
                + "Hubungi pihak sekolah untuk mendapatkan tautan baru.";

            if (tokenParam == null || tokenParam.trim().isEmpty()) {
                sendError(res, req.getContextPath(), pesanTolak);
                return;
            }

            session = HibernateUtil.getSessionFactory().openSession();
            Siswa siswa = SiswaCatatanOrtuTokenService.cariSiswaByToken(session, tokenParam);
            if (siswa == null) {
                sendError(res, req.getContextPath(), pesanTolak);
                return;
            }

            // Simpan ID siswa di HTTP session agar bisa dibaca Action ZK
            req.getSession(true).setAttribute(SK_SISWA_ID, siswa.getId());

            // Arahkan ke halaman ZUL
            String target = req.getContextPath()
                + "/common/catatan_orang_tua_terhadap_aktiftas_harian_siswa.zul";
            res.sendRedirect(target);

        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/CatatanOrangTuaServlet.java:79");
            sendError(res, req.getContextPath(), "Terjadi kesalahan sistem.");
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CatatanOrangTuaServlet.java:83");}
                try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CatatanOrangTuaServlet.java:84");}
                try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CatatanOrangTuaServlet.java:85");}
            }
        }
    }

    /**
     * Menulis halaman HTML error sederhana (ikon peringatan, judul, pesan) langsung ke
     * {@code response} sebagai pengganti redirect sukses — dipakai oleh {@link #process}
     * setiap kali validasi parameter {@code token} gagal atau terjadi exception.
     *
     * <p>Pesan yang ditampilkan di-escape lewat {@link #esc(String)} untuk mencegah HTML/
     * XSS injection walau nilainya saat ini selalu literal konstan yang dipilih oleh kode
     * pemanggil, bukan input mentah dari klien.</p>
     *
     * @param res     response HTTP keluar; diisi {@code Content-Type: text/html;charset=UTF-8}
     *                dan badan halaman error
     * @param ctxPath context path aplikasi; parameter ini TIDAK dipakai pada badan HTML yang
     *                dihasilkan (dipertahankan pada tanda tangan method untuk kemungkinan
     *                pemakaian mendatang, mis. tautan kembali ke beranda)
     * @param pesan   pesan yang ditampilkan ke pengguna, di-escape lewat {@link #esc(String)}
     *                sebelum disisipkan ke HTML
     * @throws IOException bila penulisan respons gagal
     */
    private void sendError(HttpServletResponse res, String ctxPath, String pesan)
            throws IOException {
        res.setContentType("text/html;charset=UTF-8");
        res.getWriter().println(
            "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<title>Tidak Ditemukan</title>"
            + "<style>body{font-family:sans-serif;text-align:center;padding:60px 20px;background:#f0f4f8;}"
            + "h2{color:#1e3a5f;} p{color:#64748b;} a{color:#2563eb;}</style></head><body>"
            + "<h2>&#9888; " + esc(pesan) + "</h2>"
            + "<p>Pastikan tautan yang Anda gunakan sudah benar, atau hubungi pihak sekolah.</p>"
            + "</body></html>");
    }

    /**
     * Meng-escape karakter HTML spesial ({@code &}, {@code <}, {@code >}) pada teks agar aman
     * disisipkan ke dalam badan halaman HTML yang dibentuk {@link #sendError}, mencegah
     * injeksi markup/skrip bila suatu saat pesan berasal dari input yang kurang terpercaya.
     *
     * @param s teks masukan, boleh {@code null}
     * @return teks yang sudah di-escape, atau string kosong bila {@code s} adalah {@code null}
     */
    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
