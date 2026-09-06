package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Servlet PMB.
 *
 * Perbaikan V11:
 * - PMB JSP versi baru diberi fallback aman ke ZUL lama jika terjadi JasperException/NPE.
 * - Menghindari double forward dan "Cannot forward after response has been committed".
 * - Null-safe konfigurasi dan parameter request.
 * - Kompatibel Java 1.7 / gaya Java 1.6.
 */
public class Pmb extends HttpServlet {

    /**
     * ID versi serialisasi tetap untuk kompatibilitas antar versi kelas servlet ini.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Path ZUL versi lama halaman PMB (Penerimaan Mahasiswa Baru). Dipakai sebagai
     * tujuan bila diminta eksplisit lewat parameter {@code versilama}, bila entitas
     * domain (perguruan tinggi/sekolah/yayasan) memilih tampilan klasik, bila
     * konfigurasi memaksa nonaktifkan versi baru, DAN sebagai fallback aman ketika
     * forward ke {@link #PATH_PMB_BARU} gagal di tengah jalan.
     */
    private static final String PATH_PMB_LAMA = "/WEB-INF/z/x/y/pmb.zul";
    /**
     * Path JSP versi baru halaman PMB, dipakai bila konfigurasi/parameter memilih
     * tampilan baru. Kegagalan pada forward ke path ini (JasperException/NPE/dsb.)
     * ditangkap dan dialihkan lewat {@link #writeFallbackPage}, bukan mem-forward
     * ulang ke {@link #PATH_PMB_LAMA}, karena ZUL lama dan JSP baru tidak menjamin
     * kompatibel menerima atribut request yang sama.
     */
    private static final String PATH_PMB_BARU = "/WEB-INF/baru/pmb.jsp";

    /**
     * Konstruktor default tanpa argumen, dipanggil kontainer servlet saat instansiasi.
     */
    public Pmb() {
        super();
    }

    /**
     * Menangani permintaan GET dengan mendelegasikan ke
     * {@link #process(HttpServletRequest, HttpServletResponse)}.
     *
     * <p>Kesalahan tak tertangani di {@link #process} ditangkap di sini dan dialihkan
     * ke {@link #handleFatalError} agar pengunjung tetap melihat halaman fallback
     * yang ramah, bukan error kontainer mentah.</p>
     *
     * @param request permintaan HTTP masuk
     * @param response respons HTTP yang akan diisi hasil forward atau halaman fallback
     * @throws ServletException tidak pernah dilempar keluar method ini (ditangkap di dalam)
     * @throws IOException diteruskan dari {@link #handleFatalError} bila penulisan
     *         halaman fallback itu sendiri gagal
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            handleFatalError(request, response, e);
        }
    }

    /**
     * Menangani permintaan POST dengan perilaku yang sama persis dengan
     * {@link #doGet(HttpServletRequest, HttpServletResponse)}.
     *
     * @param request permintaan HTTP masuk
     * @param response respons HTTP yang akan diisi hasil forward atau halaman fallback
     * @throws ServletException tidak pernah dilempar keluar method ini (ditangkap di dalam)
     * @throws IOException diteruskan dari {@link #handleFatalError} bila penulisan
     *         halaman fallback itu sendiri gagal
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            handleFatalError(request, response, e);
        }
    }

    /**
     * Menyiapkan konteks URL lalu mem-forward ke halaman PMB hasil
     * {@link #resolveDispatcherPath(HttpServletRequest)}, dengan fallback otomatis
     * lewat {@link #forwardWithFallback}.
     *
     * <p>Tidak ada pemeriksaan sesi/otentikasi pengguna: halaman PMB memang publik
     * (pendaftar belum menjadi pengguna terautentikasi aplikasi), sehingga tidak
     * adanya gerbang login di sini adalah fakta arsitektur yang disengaja, bukan
     * celah keamanan.</p>
     *
     * @param request permintaan HTTP masuk
     * @param response respons HTTP yang akan menerima hasil forward
     * @throws Exception diteruskan dari {@link #forwardWithFallback}
     */
    private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        initCommonContext(request);

        String dispatcherPath = resolveDispatcherPath(request);
        forwardWithFallback(request, response, dispatcherPath);
    }

    /**
     * Menentukan preferensi tampilan PMB (klasik/baru/default) dari entitas domain
     * yang berlaku untuk permintaan ini.
     *
     * <p>Urutan pencarian: {@link PerguruanTinggi} lebih dulu (lewat
     * {@link PerguruanTinggiUtil#getPerguruanTinggi(HttpServletRequest)}); bila
     * tidak memberi preferensi eksplisit dan aplikasi berjalan dalam mode sekolah
     * ({@link Common#chekPtAtauSekolah()}), dilanjutkan ke {@link Sekolah} lalu ke
     * {@link Yayasan} sebagai fallback bertingkat. Preferensi pertama yang BUKAN nilai
     * default langsung dipakai. Seluruh pengecualian ditelan dan menghasilkan nilai
     * default ({@link PerguruanTinggi#TAMPILAN_DEFAULT}), agar kegagalan membaca
     * entitas domain tidak menggagalkan seluruh permintaan PMB.</p>
     *
     * @param request permintaan HTTP, dipakai util domain untuk menentukan entitas
     *         mana yang berlaku (mis. berdasar subdomain/parameter)
     * @return kode preferensi tampilan ({@link PerguruanTinggi#TAMPILAN_BARU},
     *         {@link PerguruanTinggi#TAMPILAN_KLASIK}, atau
     *         {@link PerguruanTinggi#TAMPILAN_DEFAULT})
     */
    private String getPiilhanTampilanDomain(HttpServletRequest request) {
        try {
            PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
            if (pt != null && !PerguruanTinggi.TAMPILAN_DEFAULT.equals(pt.getPiilhanTampilan())) {
                return pt.getPiilhanTampilan();
            }
            boolean[] ptAtauSekolah = Common.chekPtAtauSekolah();
            boolean sekolahMode = ptAtauSekolah != null && ptAtauSekolah.length > 1 && ptAtauSekolah[1];
            if (sekolahMode) {
                Sekolah sekolah = SekolahUtil.getSekolah(request);
                if (sekolah != null && !Sekolah.TAMPILAN_DEFAULT.equals(sekolah.getPiilhanTampilan())) {
                    return sekolah.getPiilhanTampilan();
                }
                Yayasan yayasan = SekolahUtil.getYayasan(request);
                if (yayasan != null && !Yayasan.TAMPILAN_DEFAULT.equals(yayasan.getPiilhanTampilan())) {
                    return yayasan.getPiilhanTampilan();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return PerguruanTinggi.TAMPILAN_DEFAULT;
    }

    /**
     * Menentukan path internal tujuan forward untuk halaman PMB, dievaluasi menurut
     * urutan prioritas berikut (prioritas pertama yang cocok langsung dipakai):
     * <ol>
     * <li>Parameter {@code versilama=true} -- selalu ke {@link #PATH_PMB_LAMA},
     * mengesampingkan seluruh preferensi lain.</li>
     * <li>Preferensi tampilan entitas domain
     * ({@link #getPiilhanTampilanDomain(HttpServletRequest)}): {@code TAMPILAN_BARU}
     * menandai atribut request {@code new_context=pmb} lalu ke shell
     * {@code /WEB-INF/new/index.jsp}; {@code TAMPILAN_KLASIK} ke
     * {@link #PATH_PMB_LAMA}.</li>
     * <li>Konfigurasi global {@code pmb_versi_baru_dinonaktifkan}: bila aktif, paksa
     * ke {@link #PATH_PMB_LAMA} tanpa memandang parameter permintaan.</li>
     * <li>Bila belum ada keputusan, tampilan baru dipakai bila parameter
     * {@code baru} hadir, atau konfigurasi {@code default_pmb_gunakan_versi_baru}
     * aktif, atau parameter {@code hanya_tampil_jsp=true}; selain itu fallback ke
     * {@link #PATH_PMB_LAMA}.</li>
     * </ol>
     * <p>Pembacaan konfigurasi yang gagal ditangkap dan dilaporkan lewat
     * {@link Common#tampilErrorJikaAdmin(Exception)} tanpa menghentikan resolusi;
     * prioritas yang gagal dibaca dianggap tidak aktif dan evaluasi lanjut ke
     * prioritas berikutnya.</p>
     *
     * @param request permintaan HTTP, sumber parameter {@code versilama},
     *         {@code baru}, dan {@code hanya_tampil_jsp}
     * @return path internal tujuan forward, tidak pernah {@code null}
     */
    private String resolveDispatcherPath(HttpServletRequest request) {
        if (isTrue(request.getParameter("versilama"))) {
            return PATH_PMB_LAMA;
        }

        // Cek pilihan tampilan dari entitas domain
        String piilhan = getPiilhanTampilanDomain(request);
        if (PerguruanTinggi.TAMPILAN_BARU.equals(piilhan)) {
            request.setAttribute("new_context", "pmb");
            return "/WEB-INF/new/index.jsp";
        } else if (PerguruanTinggi.TAMPILAN_KLASIK.equals(piilhan)) {
            return PATH_PMB_LAMA;
        }

        // Jika PMB versi baru dinonaktifkan secara global, paksa ke ZUL lama
        try {
            Konfigurasi konfDisabled = Common.getKonfigurasi("pmb_versi_baru_dinonaktifkan", Konfigurasi.TIDAK_AKTIF);
            if (konfDisabled != null && konfDisabled.getNilai() != null
                    && Konfigurasi.AKTIF.equalsIgnoreCase(konfDisabled.getNilai().trim())) {
                return PATH_PMB_LAMA;
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }

        String dispatcherPath = PATH_PMB_LAMA;
        Konfigurasi config = null;
        try {
            config = Common.getKonfigurasi("default_pmb_gunakan_versi_baru", Konfigurasi.AKTIF);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }

        boolean isVersiBaruAktif = config != null && config.getNilai() != null
                && Konfigurasi.AKTIF.equalsIgnoreCase(config.getNilai().trim());
        boolean requestVersiBaru = request.getParameter("baru") != null;
        boolean hanyaTampilJsp = isTrue(request.getParameter("hanya_tampil_jsp"));

        if (requestVersiBaru || isVersiBaruAktif || hanyaTampilJsp) {
            dispatcherPath = PATH_PMB_BARU;
        }
        return dispatcherPath;
    }

    /**
     * Melakukan forward ke {@code dispatcherPath}, dengan penanganan kegagalan
     * berlapis agar pengunjung publik PMB tidak pernah melihat stack trace mentah.
     *
     * <p>Bila respons sudah ter-commit, method langsung berhenti tanpa melakukan
     * apa pun. Kegagalan pada forward (termasuk {@link Throwable} non-{@link Exception}
     * seperti {@link Error}, agar JasperException/StackOverflowError pada JSP pun
     * tertangani) selalu dilaporkan lewat
     * {@link Common#tampilErrorJikaAdmin(Exception)}. Jika path yang gagal adalah
     * {@link #PATH_PMB_BARU}, atribut request {@code pmb_jsp_error} diisi dan alur
     * dialihkan ke {@link #forwardToLegacyOrFallback}; untuk path lain (termasuk
     * kegagalan pada {@link #PATH_PMB_LAMA} sendiri), langsung ke
     * {@link #writeFallbackPage}.</p>
     *
     * @param request permintaan HTTP yang akan di-forward
     * @param response respons HTTP tujuan forward atau halaman fallback
     * @param dispatcherPath path internal tujuan forward yang sudah ditentukan
     *         {@link #resolveDispatcherPath(HttpServletRequest)}
     * @throws ServletException diteruskan dari {@link javax.servlet.RequestDispatcher#forward}
     *         bila kegagalan tidak tertangkap oleh blok fallback
     * @throws IOException diteruskan dari forward atau dari penulisan halaman fallback
     */
    private void forwardWithFallback(HttpServletRequest request, HttpServletResponse response, String dispatcherPath)
            throws ServletException, IOException {
        if (response.isCommitted()) {
            return;
        }

		try {
			javax.servlet.RequestDispatcher dispatcher = request.getRequestDispatcher(dispatcherPath);
			if (dispatcher == null) {
				throw new ServletException("Halaman PMB tidak ditemukan: " + dispatcherPath);
			}
			dispatcher.forward(request, response);
        } catch (Throwable e) {
            Common.tampilErrorJikaAdmin(asException(e));

            if (PATH_PMB_BARU.equals(dispatcherPath)) {
                request.setAttribute("pmb_jsp_error", e);
                forwardToLegacyOrFallback(request, response, e);
                return;
            }

            writeFallbackPage(request, response, e);
        }
    }

    /**
     * Menuliskan halaman fallback setelah forward ke {@link #PATH_PMB_BARU} gagal.
     *
     * <p>Meskipun namanya menyiratkan alternatif "kembali ke ZUL lama", implementasi
     * saat ini SELALU menuju {@link #writeFallbackPage} -- forward balik ke
     * {@link #PATH_PMB_LAMA} sengaja tidak dilakukan karena atribut request yang
     * sudah diisi untuk JSP baru (atau state parsial yang sempat tertulis ke
     * response) tidak terjamin aman dipakai ulang oleh ZUL lama.</p>
     *
     * @param request permintaan HTTP asal, diteruskan ke halaman fallback
     * @param response respons HTTP; tidak diproses lebih lanjut bila sudah ter-commit
     * @param originalError kesalahan asli dari kegagalan forward JSP baru, dipakai
     *         {@link #writeFallbackPage} bila diperlukan (saat ini tidak ditampilkan
     *         ke pengguna, hanya diteruskan sebagai parameter)
     * @throws IOException diteruskan dari {@link #writeFallbackPage}
     */
    private void forwardToLegacyOrFallback(HttpServletRequest request, HttpServletResponse response, Throwable originalError)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        writeFallbackPage(request, response, originalError);
    }

    /**
     * Menangani kegagalan tak terduga yang lolos dari {@link #process} (dipanggil
     * dari blok {@code catch} di {@link #doGet}/{@link #doPost}).
     *
     * <p>Melaporkan kesalahan lewat {@link Common#tampilErrorJikaAdmin(Exception)}
     * lalu selalu menuliskan {@link #writeFallbackPage}, tanpa membedakan jenis
     * kegagalan -- pengunjung publik PMB tidak boleh terhenti tanpa penjelasan hanya
     * karena satu kesalahan tak terduga.</p>
     *
     * @param request permintaan HTTP asal
     * @param response respons HTTP yang akan menerima halaman fallback
     * @param e kesalahan asli, boleh berupa {@link Throwable} apa pun
     * @throws IOException diteruskan dari {@link #writeFallbackPage}
     */
    private void handleFatalError(HttpServletRequest request, HttpServletResponse response, Throwable e) throws IOException {
        Common.tampilErrorJikaAdmin(asException(e));
        writeFallbackPage(request, response, e);
    }

    /**
     * Menuliskan halaman HTML statis sederhana ("Halaman PMB belum bisa
     * ditampilkan") langsung ke {@link HttpServletResponse#getWriter()}, sebagai
     * jaring pengaman terakhir ketika seluruh jalur forward JSP/ZUL PMB gagal.
     *
     * <p>Bila respons sudah ter-commit, method langsung berhenti (tidak bisa menulis
     * halaman baru lagi). Selain itu, buffer respons di-reset lebih dulu lewat
     * {@link #safeResetBuffer(HttpServletResponse)} untuk membuang output parsial
     * yang mungkin sudah sempat tertulis sebelum kegagalan terjadi. Halaman berisi
     * tombol "Muat ulang" dan "Kembali ke halaman utama" (dibangun dari
     * {@link HttpServletRequest#getContextPath()}), serta kode bantuan
     * {@code PMB-FALLBACK} untuk memudahkan pelacakan dukungan. Parameter {@code e}
     * tidak ditampilkan ke pengguna (mencegah kebocoran detail internal ke
     * pengunjung publik); ia hanya diteruskan untuk konsistensi signature dengan
     * pemanggil yang sudah melaporkan kesalahan tersebut sebelumnya.</p>
     *
     * @param request permintaan HTTP, sumber context path untuk tautan navigasi
     * @param response respons HTTP yang menerima halaman HTML fallback
     * @param e kesalahan asli yang memicu fallback; tidak dirender ke output
     * @throws IOException diteruskan dari {@link HttpServletResponse#getWriter()}
     *         atau penulisan ke writer tersebut
     */
    private void writeFallbackPage(HttpServletRequest request, HttpServletResponse response, Throwable e) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        safeResetBuffer(response);
        response.setContentType("text/html;charset=UTF-8");

        String contextPath = request == null || request.getContextPath() == null ? "" : request.getContextPath();
        String homeUrl = contextPath + "/";

        PrintWriter writer = response.getWriter();
        writer.println("<!DOCTYPE html>");
        writer.println("<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        writer.println("<title>PMB</title>");
        writer.println("<style>");
        writer.println("body{margin:0;font-family:Arial,Helvetica,sans-serif;background:#f4f7fb;color:#1f2937;}");
        writer.println(".wrap{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px;}");
        writer.println(".card{max-width:620px;background:#fff;border-radius:18px;padding:28px;box-shadow:0 18px 48px rgba(15,23,42,.16);border:1px solid #e5e7eb;}");
        writer.println("h1{font-size:24px;margin:0 0 12px;color:#111827;}p{line-height:1.6;margin:0 0 12px;color:#4b5563;}");
        writer.println(".actions{margin-top:20px;display:flex;gap:10px;flex-wrap:wrap}.btn{display:inline-block;padding:11px 16px;border-radius:10px;text-decoration:none;font-weight:bold}.primary{background:#166534;color:#fff}.secondary{background:#e5e7eb;color:#111827}");
        writer.println(".small{font-size:12px;color:#6b7280;margin-top:18px}");
        writer.println("</style></head><body><div class=\"wrap\"><div class=\"card\">");
        writer.println("<h1>Halaman PMB belum bisa ditampilkan</h1>");
        writer.println("<p>Terjadi kendala saat membuka tampilan PMB. Data Anda tetap aman. Silakan muat ulang halaman atau kembali ke halaman utama.</p>");
        writer.println("<div class=\"actions\"><a class=\"btn primary\" href=\"javascript:location.reload()\">Muat ulang</a><a class=\"btn secondary\" href=\"" + homeUrl + "\">Kembali ke halaman utama</a></div>");
        writer.println("<div class=\"small\">Kode bantuan: PMB-FALLBACK</div>");
        writer.println("</div></div></body></html>");
        writer.flush();
    }

    /**
     * Me-reset buffer respons secara aman, menelan kegagalan alih-alih
     * melemparkannya keluar.
     *
     * <p>{@link HttpServletResponse#resetBuffer()} hanya dipanggil bila respons
     * belum ter-commit (memanggilnya pada respons yang sudah ter-commit akan
     * melempar {@link IllegalStateException}); kegagalan lain ditangkap dan
     * dilaporkan lewat {@link Common#tampilErrorJikaAdmin(Exception)} tanpa
     * menghentikan alur penulisan halaman fallback.</p>
     *
     * @param response respons HTTP yang buffer-nya akan direset, boleh {@code null}
     */
    private void safeResetBuffer(HttpServletResponse response) {
        try {
            if (response != null && !response.isCommitted()) {
                response.resetBuffer();
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
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
        Common.CURRENT_URL_SIMPLE = (request.isSecure() ? "https://" : "http://") + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443 ? ""
                        : ":" + request.getServerPort());
        Common.CURRENT_URL = (Common.isSecure(request) ? "https://" : "http://") + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort())
                + request.getContextPath();
    }

    /**
     * Memeriksa apakah nilai string sama dengan literal {@code "true"}.
     *
     * @param value string yang diperiksa, boleh {@code null}
     * @return {@code true} bila {@code value} tidak null dan setelah di-trim sama
     *         (case-insensitive) dengan {@code "true"}
     */
    private static boolean isTrue(String value) {
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

    /**
     * Mengonversi {@link Throwable} apa pun menjadi {@link Exception}, membungkus
     * {@link Error} atau {@link Throwable} lain yang bukan {@link Exception} agar
     * dapat diteruskan ke API pelaporan yang mensyaratkan tipe {@link Exception}
     * (mis. {@link Common#tampilErrorJikaAdmin(Exception)}).
     *
     * @param throwable objek yang dikonversi, tidak boleh {@code null}
     * @return {@code throwable} itu sendiri bila sudah berupa {@link Exception},
     *         atau {@link Exception} baru yang membungkusnya sebagai cause
     */
    private static Exception asException(Throwable throwable) {
        if (throwable instanceof Exception) {
            return (Exception) throwable;
        }
        return new Exception(throwable);
    }
}
