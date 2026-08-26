package ais.action.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    private static final long serialVersionUID = 1L;
    private static final String KONFIGURASI_PAKSA_SKIN = "paksa_halaman_utama_menggunakan_skin";
    private static final String HALAMAN_UTAMA_SKIN = "/WEB-INF/j/index.jsp";

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
