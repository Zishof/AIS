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

    private static final long serialVersionUID = 1L;

    private static final String PATH_PMB_LAMA = "/WEB-INF/z/x/y/pmb.zul";
    private static final String PATH_PMB_BARU = "/WEB-INF/baru/pmb.jsp";

    public Pmb() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            handleFatalError(request, response, e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            handleFatalError(request, response, e);
        }
    }

    private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        initCommonContext(request);

        String dispatcherPath = resolveDispatcherPath(request);
        forwardWithFallback(request, response, dispatcherPath);
    }

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

    private String resolveDispatcherPath(HttpServletRequest request) {
        if (isTrue(request.getParameter("versilama"))) {
            return PATH_PMB_LAMA;
        }

        // Cek pilihan tampilan dari entitas domain
        String piilhan = getPiilhanTampilanDomain(request);
        if (PerguruanTinggi.TAMPILAN_BARU.equals(piilhan)) {
            request.setAttribute("baru2_context", "pmb");
            return "/WEB-INF/baru2/index.jsp";
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

    private void forwardWithFallback(HttpServletRequest request, HttpServletResponse response, String dispatcherPath)
            throws ServletException, IOException {
        if (response.isCommitted()) {
            return;
        }

        try {
            request.getRequestDispatcher(dispatcherPath).forward(request, response);
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

    private void forwardToLegacyOrFallback(HttpServletRequest request, HttpServletResponse response, Throwable originalError)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }

        try {
            safeResetBuffer(response);
            request.getRequestDispatcher(PATH_PMB_LAMA).forward(request, response);
        } catch (Throwable legacyError) {
            Common.tampilErrorJikaAdmin(asException(legacyError));
            writeFallbackPage(request, response, originalError);
        }
    }

    private void handleFatalError(HttpServletRequest request, HttpServletResponse response, Throwable e) throws IOException {
        Common.tampilErrorJikaAdmin(asException(e));
        writeFallbackPage(request, response, e);
    }

    private void writeFallbackPage(HttpServletRequest request, HttpServletResponse response, Throwable e) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        safeResetBuffer(response);
        response.setContentType("text/html;charset=UTF-8");

        String contextPath = request == null || request.getContextPath() == null ? "" : request.getContextPath();
        String legacyUrl = contextPath + "/pmb?versilama=true";
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
        writer.println("<p>Terjadi kendala saat membuka tampilan PMB versi baru. Data Anda tetap aman. Silakan buka tampilan PMB versi lama atau kembali ke halaman utama.</p>");
        writer.println("<div class=\"actions\"><a class=\"btn primary\" href=\"" + legacyUrl + "\">Buka PMB versi lama</a><a class=\"btn secondary\" href=\"" + homeUrl + "\">Kembali ke halaman utama</a></div>");
        writer.println("<div class=\"small\">Kode bantuan: PMB-FALLBACK</div>");
        writer.println("</div></div></body></html>");
        writer.flush();
    }

    private void safeResetBuffer(HttpServletResponse response) {
        try {
            if (response != null && !response.isCommitted()) {
                response.resetBuffer();
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

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

    private static boolean isTrue(String value) {
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

    private static Exception asException(Throwable throwable) {
        if (throwable instanceof Exception) {
            return (Exception) throwable;
        }
        return new Exception(throwable);
    }
}
