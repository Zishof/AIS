package ais.action.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.home.HomePortalService;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Servlet halaman index/home.
 *
 * Urutan tampilan publik:
 * 1. Pilihan domain "baru" -> Home V3 (atau home lama bila feature flag dimatikan).
 * 2. Pilihan domain "klasik" -> index/login klasik.
 * 3. Landing page khusus eBisnis/ERP sesuai konfigurasi.
 * 4. default_home_versi_baru -> Home V3 dengan fallback aman.
 * 5. Login baru, index/login klasik, lalu redirect /login.
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

    private static final long serialVersionUID = 1L;

    public Index() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e); 
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    @SuppressWarnings({ "deprecation" })
    private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        initCommonContext(request);

        // Cek pilihan tampilan dari entitas domain (prioritas tertinggi)
        String pilihan = getPilihanTampilanDomain(request);
        if (PerguruanTinggi.TAMPILAN_BARU.equals(pilihan)) {
            forwardModernPublicHome(request, response, "domain");
            return;
        } else if (PerguruanTinggi.TAMPILAN_KLASIK.equals(pilihan)) {
            String filePath = request.getRealPath("/WEB-INF/j/index.jsp");
            if (fileExists(filePath)) {
                forward(request, response, "/WEB-INF/j/index.jsp");
            } else {
                forward(request, response, "/WEB-INF/j/login.jsp");
            }
            return;
        }

        Konfigurasi config = Common.getKonfigurasi("default_login_ke_ebisnis", Konfigurasi.TIDAK_AKTIF);
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
            forwardModernPublicHome(request, response, "configuration");
            return;
        }

        config = Common.getKonfigurasi("default_home_login_versi_baru", Konfigurasi.TIDAK_AKTIF);
        if (isAktif(config)) {
            forward(request, response, "/WEB-INF/baru/login2.jsp");
            return;
        }

        String fileDiMedia = request.getRealPath("/WEB-INF/j/index.jsp");
        if (fileExists(fileDiMedia)) {
            forward(request, response, "/WEB-INF/j/index.jsp");
            return;
        }

        fileDiMedia = request.getRealPath("/WEB-INF/j/login.jsp");
        if (fileExists(fileDiMedia)) {
            forward(request, response, "/WEB-INF/j/login.jsp");
            return;
        }

        if (!response.isCommitted()) {
            response.sendRedirect(request.getContextPath() + "/login");
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

    private static void forward(HttpServletRequest request, HttpServletResponse response, String path)
            throws ServletException, IOException {
        if (!response.isCommitted()) {
            request.getRequestDispatcher(path).forward(request, response);
        }
    }

    private static boolean isAktif(Konfigurasi config) {
        return config != null && config.getNilai() != null && Konfigurasi.AKTIF.equalsIgnoreCase(config.getNilai().trim());
    }

    private void forwardModernPublicHome(HttpServletRequest request, HttpServletResponse response, String source)
            throws ServletException, IOException {
        Konfigurasi homeV3 = Common.getKonfigurasi("home_ui_v3", Konfigurasi.AKTIF);
        if (!isAktif(homeV3)) {
            request.setAttribute("homeUiEntry", source + ":legacy");
            forward(request, response, "/WEB-INF/baru/home.jsp");
            return;
        }

        try {
            String requestedLanguage = request.getParameter("lang");
            if (requestedLanguage != null && requestedLanguage.trim().length() > 0) {
                Common.initBahasaParameter(requestedLanguage.trim());
            }
            request.setAttribute("homeUiEntry", source + ":v3");
            request.setAttribute("homePortal", new HomePortalService().build(request));
            forward(request, response, "/WEB-INF/baru/home-v3.jsp");
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "Index public home V3 fallback (" + source + ")");
            if (!response.isCommitted()) {
                request.setAttribute("homeUiEntry", source + ":fallback");
                forward(request, response, "/WEB-INF/baru/home.jsp");
            }
        }
    }

    private String getPilihanTampilanDomain(HttpServletRequest request) {
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
            // ignore, fall through
        }
        return PerguruanTinggi.TAMPILAN_DEFAULT;
    }

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
}
