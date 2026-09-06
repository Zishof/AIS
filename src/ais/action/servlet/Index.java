package ais.action.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;
import ais.action.servlet.landing.PesantrenLandingService;
import ais.database.model.Konfigurasi;

/**
 * Servlet halaman index/home.
 *
 * Urutan tampilan publik (kompatibel dengan routing lama):
 * 1. Bila paksa_halaman_utama_menggunakan_skin aktif, wajib memakai skin.
 * 2. default_login_ke_epesantren -> pesantren.jsp.
 * 3. default_login_ke_ebisnis -> ebisnis.jsp.
 * 4. default_login_ke_erp -> erp.jsp.
 * 5. default_home_versi_baru -> home.jsp.
 * 6. default_home_login_versi_baru -> login2.jsp.
 * 7. Skin hasil upload (/WEB-INF/j/index.jsp).
 * 8. Bila skin tidak tersedia -> home.jsp.
 *
 * Enhancement aman:
 * - Null-safe untuk Konfigurasi.
 * - File check aman jika path null.
 * - Forward/redirect tidak dilakukan jika response sudah committed.
 * - Shell `/WEB-INF/new/index.jsp` tidak dipakai untuk halaman publik karena
 *   membutuhkan session `mytbmuser`; shell tersebut tetap digunakan oleh `/new`
 *   dan alur aplikasi setelah login.
 * - Kompatibel source/target Java 8 proyek.
 */
public class Index extends HttpServlet {

    /**
     * ID versi serialisasi tetap untuk kompatibilitas antar versi kelas servlet ini.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Kunci konfigurasi yang memaksa halaman utama publik selalu memakai skin hasil
     * upload ({@link #HALAMAN_UTAMA_SKIN}), mengesampingkan seluruh aturan routing
     * konfigurasi lain di bawahnya bila bernilai aktif.
     */
    private static final String KONFIGURASI_PAKSA_SKIN = "paksa_halaman_utama_menggunakan_skin";
    /**
     * Path JSP skin hasil upload admin yang dipakai sebagai halaman utama publik,
     * relatif terhadap root webapp. Path ini juga dipakai sebagai fallback terakhir
     * bila tidak satu pun konfigurasi routing lain aktif.
     */
    private static final String HALAMAN_UTAMA_SKIN = "/WEB-INF/j/index.jsp";
    /**
     * Sumber acak kriptografis untuk menghasilkan nonce Content-Security-Policy pada
     * halaman landing ePesantren; dibuat sekali sebagai konstanta kelas agar tidak
     * membuat instance {@link SecureRandom} baru pada tiap permintaan.
     */
    private static final SecureRandom CSP_RANDOM = new SecureRandom();

    /**
     * Konstruktor default tanpa argumen, dipanggil kontainer servlet saat instansiasi.
     */
    public Index() {
        super();
    }

    /**
     * Menangani permintaan GET ke halaman utama publik dengan mendelegasikan ke
     * {@link #process(HttpServletRequest, HttpServletResponse)}.
     *
     * <p>Pengecualian yang terjadi selama pemrosesan ditangkap dan dilaporkan lewat
     * {@link Common#tampilErrorJikaAdmin(Exception)} tanpa dilempar ulang; halaman ini
     * publik dan tidak boleh menampilkan stack trace ke pengunjung biasa.</p>
     *
     * @param request permintaan HTTP masuk
     * @param response respons HTTP yang akan diisi hasil forward halaman utama
     * @throws ServletException tidak pernah dilempar keluar method ini (ditangkap di dalam)
     * @throws IOException tidak pernah dilempar keluar method ini (ditangkap di dalam)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e); 
        }
    }

    /**
     * Menangani permintaan POST ke halaman utama publik dengan perilaku yang sama
     * persis dengan {@link #doGet(HttpServletRequest, HttpServletResponse)}.
     *
     * @param request permintaan HTTP masuk
     * @param response respons HTTP yang akan diisi hasil forward halaman utama
     * @throws ServletException tidak pernah dilempar keluar method ini (ditangkap di dalam)
     * @throws IOException tidak pernah dilempar keluar method ini (ditangkap di dalam)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    /**
     * Menentukan dan mem-forward halaman utama publik sesuai delapan prioritas yang
     * dijelaskan pada Javadoc kelas ({@link Index}), dievaluasi berurutan dari atas
     * ke bawah -- prioritas pertama yang aktif langsung dipakai dan method berhenti.
     *
     * <p>Tidak ada pemeriksaan sesi/otentikasi pengguna di method ini: halaman utama
     * memang dapat diakses publik tanpa login, sesuai perannya sebagai pintu masuk
     * sebelum autentikasi (landing/skin/home/login). Ini adalah fakta arsitektur yang
     * disengaja, bukan celah keamanan -- konten yang dirender di tiap tujuan forward
     * (home.jsp, login2.jsp, skin upload, dsb.) sendiri bertanggung jawab untuk tidak
     * membocorkan data privat sebelum login.</p>
     *
     * @param request permintaan HTTP masuk
     * @param response respons HTTP yang akan menerima hasil forward
     * @throws Exception diteruskan dari {@link javax.servlet.RequestDispatcher#forward}
     *         atau dari pembacaan konfigurasi; ditangkap oleh pemanggil
     *         ({@link #doGet}/{@link #doPost})
     */
    @SuppressWarnings({ "deprecation" })
    private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        initCommonContext(request);

        Konfigurasi config = Common.getKonfigurasi(KONFIGURASI_PAKSA_SKIN, Konfigurasi.TIDAK_AKTIF);
        if (isAktif(config)) {
            String fileSkin = request.getRealPath(HALAMAN_UTAMA_SKIN);
            if (fileExists(fileSkin)) {
                request.setAttribute("homeUiEntry", "skin:forced");
                forward(request, response, HALAMAN_UTAMA_SKIN);
            } else if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "Skin halaman utama belum tersedia. Unggah skin ZIP yang memiliki file index.jsp atau nonaktifkan konfigurasi paksa skin.");
            }
            return;
        }

        config = Common.getKonfigurasi("default_login_ke_epesantren", Konfigurasi.TIDAK_AKTIF);
        if (isAktif(config)) {
            configurePesantrenResponse(request, response);
            PesantrenLandingService.prepare(request);
            request.setAttribute("homeUiEntry", "configuration:epesantren");
            forward(request, response, "/WEB-INF/baru/pesantren.jsp");
            return;
        }

        config = Common.getKonfigurasi("default_login_ke_ebisnis", Konfigurasi.TIDAK_AKTIF);
        if (isAktif(config)) {
            forward(request, response, "/WEB-INF/baru/ebisnis.jsp");
            return;
        }

        config = Common.getKonfigurasi("default_login_ke_erp", Konfigurasi.TIDAK_AKTIF);
        if (isAktif(config)) {
            forward(request, response, "/WEB-INF/baru/erp.jsp");
            return;
        }

        config = Common.getKonfigurasi("default_home_versi_baru", Konfigurasi.TIDAK_AKTIF);
        if (isAktif(config)) {
            request.setAttribute("homeUiEntry", "configuration:home");
            forward(request, response, "/WEB-INF/baru/home.jsp");
            return;
        }

        config = Common.getKonfigurasi("default_home_login_versi_baru", Konfigurasi.TIDAK_AKTIF);
        if (isAktif(config)) {
            forward(request, response, "/WEB-INF/baru/login2.jsp");
            return;
        }

        String fileDiMedia = request.getRealPath(HALAMAN_UTAMA_SKIN);
        if (fileExists(fileDiMedia)) {
            request.setAttribute("homeUiEntry", "skin");
            forward(request, response, HALAMAN_UTAMA_SKIN);
            return;
        }

        request.setAttribute("homeUiEntry", "fallback:home");
        forward(request, response, "/WEB-INF/baru/home.jsp");
    }

    /**
     * Menginisialisasi variabel statis global {@link Common} (REAL_PATH, ROOT,
     * CURRENT_URL, dst.) dari request saat ini, agar kode lain yang membaca variabel
     * statis tersebut (laporan, util, JSP lama) mendapat nilai yang konsisten dengan
     * permintaan yang sedang diproses.
     *
     * @param request permintaan HTTP yang menjadi sumber path dan URL kontekstual
     */
    private void initCommonContext(HttpServletRequest request) {
        Common.REAL_PATH = getServletContext().getRealPath("/");
        Common.REAL_PATH_REPORT_TEMP = getServletContext().getRealPath("/report");
        Common.ROOT = request.getContextPath();
        if (Common.sanitizedRequestHostForCurrentUrl(request) != null) {
            Common.CURRENT_URL_SIMPLE = (request.isSecure() ? "https://" : "http://") + request.getServerName()
                    + (request.getServerPort() == 80 || request.getServerPort() == 443 ? ""
                            : ":" + request.getServerPort());
            Common.CURRENT_URL = (Common.isSecure(request) ? "https://" : "http://") + request.getServerName()
                    + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort())
                    + request.getContextPath();
        }
    }

    /**
     * Melakukan forward internal ke {@code path} bila respons belum ter-commit.
     *
     * <p>Pemeriksaan {@link HttpServletResponse#isCommitted()} mencegah
     * "IllegalStateException: Cannot forward after response has been committed"
     * bila pemanggil sebelumnya sudah mengirim header/body (mis. lewat
     * {@code sendError}).</p>
     *
     * @param request permintaan HTTP yang akan di-forward
     * @param response respons HTTP tujuan forward
     * @param path path internal (relatif WEB-INF atau context) tujuan forward
     * @throws ServletException diteruskan dari {@link javax.servlet.RequestDispatcher#forward}
     * @throws IOException diteruskan dari {@link javax.servlet.RequestDispatcher#forward}
     */
    private static void forward(HttpServletRequest request, HttpServletResponse response, String path)
            throws ServletException, IOException {
        if (!response.isCommitted()) {
            request.getRequestDispatcher(path).forward(request, response);
        }
    }

    /**
     * Memeriksa apakah nilai konfigurasi sama dengan {@link Konfigurasi#AKTIF}.
     *
     * <p>Null-safe: konfigurasi yang belum diisi di basis data (baris tidak ada, atau
     * nilainya null) selalu dianggap TIDAK aktif, bukan dilempar sebagai kesalahan.</p>
     *
     * @param config baris konfigurasi yang diperiksa, boleh {@code null}
     * @return {@code true} bila {@code config} tidak null, nilainya tidak null, dan
     *         setelah di-trim sama (case-insensitive) dengan {@link Konfigurasi#AKTIF}
     */
    private static boolean isAktif(Konfigurasi config) {
        return config != null && config.getNilai() != null && Konfigurasi.AKTIF.equalsIgnoreCase(config.getNilai().trim());
    }

    /**
     * Memeriksa keberadaan berkas secara aman di sistem berkas lokal.
     *
     * <p>Null-safe terhadap path kosong/null (mis. hasil {@code getRealPath} pada
     * webapp yang di-deploy dari archive tanpa filesystem nyata), dan menelan seluruh
     * pengecualian {@link java.nio.file.Path} sebagai "berkas tidak ada" alih-alih
     * melempar keluar.</p>
     *
     * @param filePath path absolut berkas yang diperiksa, boleh {@code null}/kosong
     * @return {@code true} bila path tidak kosong dan berkas benar-benar ada
     */
    private static boolean fileExists(String filePath) {
        if (filePath == null || filePath.trim().length() == 0) {
            return false;
        }
        try {
            Path filePathObj = Paths.get(filePath);
            return Files.exists(filePathObj);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Memasang header keamanan dan Content-Security-Policy untuk halaman landing
     * ePesantren.
     *
     * <p>Nonce CSP acak (18 byte {@link SecureRandom}, Base64 URL-safe tanpa padding)
     * dibangkitkan per-permintaan dan disimpan di {@code request} sebagai atribut
     * {@code pesantrenCspNonce} agar JSP dapat memakainya pada atribut {@code nonce}
     * elemen {@code <style>}/{@code <script>} inline, konsisten dengan CSP yang
     * dipasang di sini ({@code style-src 'self' 'nonce-...'; script-src 'self'
     * 'nonce-...'}). ID permintaan (kombinasi waktu dan identity hash request) dipasang
     * sebagai header {@code X-Request-Id} untuk korelasi log.</p>
     *
     * <p>Header tambahan: {@code X-Content-Type-Options: nosniff},
     * {@code Referrer-Policy: strict-origin-when-cross-origin},
     * {@code Permissions-Policy} yang menonaktifkan kamera/mikrofon/geolokasi/pembayaran,
     * {@code X-Frame-Options: SAMEORIGIN}, dan {@code Cross-Origin-Opener-Policy:
     * same-origin}. Bila koneksi HTTPS ({@link #isHttps(HttpServletRequest)}), CSP
     * ditambah {@code upgrade-insecure-requests} dan header
     * {@code Strict-Transport-Security} dipasang. Cache-Control dibedakan: permintaan
     * GET boleh di-cache publik singkat (60 detik, dengan stale-while-revalidate 300
     * detik) beserta header {@code Vary: Accept-Language}, sedangkan permintaan non-GET
     * selalu {@code no-store}.</p>
     *
     * @param request permintaan HTTP yang menerima atribut nonce dan dipakai untuk
     *        mendeteksi metode HTTP serta protokol
     * @param response respons HTTP yang menerima seluruh header keamanan/CSP
     */
    private static void configurePesantrenResponse(HttpServletRequest request, HttpServletResponse response) {
        byte[] bytes = new byte[18];
        CSP_RANDOM.nextBytes(bytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String requestId = Long.toHexString(System.currentTimeMillis())
                + Integer.toHexString(System.identityHashCode(request));
        request.setAttribute("pesantrenCspNonce", nonce);
        response.setHeader("X-Request-Id", requestId);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        String csp = "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'self'; "
                + "form-action 'self'; img-src 'self' data: https:; font-src 'self' data:; "
                + "style-src 'self' 'nonce-" + nonce + "'; script-src 'self' 'nonce-" + nonce
                + "'; connect-src 'self'";
        if (isHttps(request)) {
            csp += "; upgrade-insecure-requests";
            response.setHeader("Strict-Transport-Security", "max-age=31536000");
        }
        response.setHeader("Content-Security-Policy", csp);
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            response.setHeader("Cache-Control", "public, max-age=60, stale-while-revalidate=300");
            response.setHeader("Vary", "Accept-Language");
        } else {
            response.setHeader("Cache-Control", "no-store");
        }
    }

    /**
     * Mendeteksi apakah permintaan berjalan di atas HTTPS, termasuk saat aplikasi ini
     * berada di belakang reverse proxy/load balancer yang men-terminate TLS.
     *
     * <p>Memeriksa {@link HttpServletRequest#isSecure()} lebih dulu; bila bernilai
     * {@code false} (umum terjadi ketika TLS di-terminate oleh proxy di depan
     * container), method jatuh ke header {@code X-Forwarded-Proto} yang lazim dipasang
     * proxy tersebut.</p>
     *
     * @param request permintaan HTTP yang diperiksa
     * @return {@code true} bila koneksi aman secara langsung, atau header
     *         {@code X-Forwarded-Proto} bernilai {@code https} (case-insensitive)
     */
    private static boolean isHttps(HttpServletRequest request) {
        if (request.isSecure()) return true;
        String forwarded = request.getHeader("X-Forwarded-Proto");
        return forwarded != null && "https".equalsIgnoreCase(forwarded.trim());
    }
}
