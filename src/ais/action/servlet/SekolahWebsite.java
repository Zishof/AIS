package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.action.servlet.landing.SchoolLandingService;
import ais.common.Common;

/**
 * Servlet landing page publik satu sekolah aktif, memetakan {@code GET /sekolah/{id}}
 * (anonim, tanpa login).
 *
 * <p>Isi profil sekolah (nama, domain, dsb.) diambil lewat {@link SchoolLandingService};
 * servlet ini hanya mem-parsing {@code id} dari path, menyiapkan konteks global
 * {@link Common} (path fisik dan context path -- pola yang sama dipakai banyak servlet lawas
 * di paket ini), dan menghitung URL kanonis (skema/host/port) untuk tag {@code <link
 * rel="canonical">} di JSP.</p>
 */
public class SekolahWebsite extends HttpServlet {
    /** ID versi serialisasi servlet ini (kontrak {@link java.io.Serializable} bawaan {@code HttpServlet}). */
    private static final long serialVersionUID = 1L;

    /**
     * Melayani {@code GET /sekolah/{id}}: memvalidasi {@code id} sekolah, memuat profilnya
     * lewat {@link SchoolLandingService}, lalu mem-forward ke {@code sekolah.jsp}.
     *
     * <p>Path yang tidak berupa {@code /{angka positif}} tunggal, atau {@code id} yang tidak
     * ditemukan/tidak aktif oleh {@link SchoolLandingService#prepare}, dibalas 404.</p>
     *
     * @param request permintaan HTTP; {@code getPathInfo()} berisi {@code /{id}}
     * @param response tanggapan HTTP; diisi forward ke {@code sekolah.jsp} atau 404
     * @throws ServletException bila forward gagal
     * @throws IOException bila penulisan tanggapan gagal
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        initCommonContext(request);
        String pathInfo = request.getPathInfo();
        Long id = schoolId(pathInfo);
        if (hasInvalidSchoolPath(pathInfo, id)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!SchoolLandingService.prepare(request, id)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        SchoolLandingService.SchoolProfile profile =
                (SchoolLandingService.SchoolProfile) request.getAttribute("schoolProfile");
        request.setAttribute("schoolCanonical", canonical(request, profile));
        request.getRequestDispatcher("/WEB-INF/baru/sekolah.jsp").forward(request, response);
    }

    /**
     * Menolak seluruh permintaan {@code POST}; landing page sekolah ini hanya melayani
     * navigasi {@code GET}.
     *
     * @param request permintaan HTTP masuk (tidak dipakai selain oleh kontrak servlet)
     * @param response tanggapan HTTP; selalu diisi status 405
     * @throws IOException bila penulisan status gagal
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Menyetel variabel global {@link Common} (path fisik aplikasi, path direktori laporan
     * sementara, dan context path) dari konteks servlet dan permintaan saat ini.
     *
     * @param request permintaan HTTP yang menjadi sumber {@code contextPath}
     */
    private void initCommonContext(HttpServletRequest request) {
        Common.REAL_PATH = getServletContext().getRealPath("/");
        Common.REAL_PATH_REPORT_TEMP = getServletContext().getRealPath("/report");
        Common.ROOT = request.getContextPath();
    }

    /**
     * Mem-parsing {@code path} (dari {@code getPathInfo()}) menjadi ID sekolah.
     *
     * <p>Path {@code null}, kosong, atau {@code "/"} dianggap "halaman indeks" dan
     * menghasilkan {@code null} (bukan galat) -- lihat {@link #hasInvalidSchoolPath} untuk
     * pembeda antara "tidak ada ID" dan "ID tidak valid".</p>
     *
     * @param path nilai {@code getPathInfo()}, boleh {@code null}
     * @return ID sekolah bila path berupa satu segmen numerik, {@code null} bila path kosong
     *         atau berupa segmen non-numerik/bersegmen ganda
     */
    private Long schoolId(String path) {
        if (path == null || path.trim().length() == 0 || "/".equals(path.trim())) return null;
        String value = path.trim();
        while (value.startsWith("/")) value = value.substring(1);
        if (value.indexOf('/') >= 0 || !value.matches("[0-9]+")) return null;
        try { return Long.valueOf(value); } catch (Exception e) { return null; }
    }

    /**
     * Membedakan "tidak ada ID" (path kosong/{@code "/"}, halaman indeks yang sah) dari
     * "ID tidak valid" (ada path tetapi {@link #schoolId} gagal mem-parsing-nya menjadi angka).
     *
     * @param path nilai {@code getPathInfo()} apa adanya, boleh {@code null}
     * @param id hasil {@link #schoolId} untuk {@code path} yang sama
     * @return {@code true} bila path terisi tetapi {@code id} tetap {@code null} (path rusak)
     */
    private boolean hasInvalidSchoolPath(String path, Long id) {
        return path != null && path.trim().length() > 0 && !"/".equals(path.trim()) && id == null;
    }

    /**
     * Menghitung URL kanonis absolut untuk halaman sekolah ini, memakai domain kustom sekolah
     * bila terpasang dan valid, atau jatuh ke host permintaan saat ini.
     *
     * <p>Skema dipilih {@code https} bila {@code request.isSecure()} atau header
     * {@code X-Forwarded-Proto: https} hadir. Port hanya disertakan bila bukan port skema
     * baku (80/443) dan bila host yang dipakai sama dengan host permintaan (untuk domain
     * kustom, port permintaan tidak relevan sehingga diasumsikan port baku sesuai skema).
     * Domain yang tidak lolos validasi format host/IPv6 diabaikan, jatuh ke path relatif
     * {@code contextPath + "/sekolah/{id}"} saja.</p>
     *
     * @param request permintaan HTTP saat ini, sumber host/port/skema fallback
     * @param profile profil sekolah (boleh {@code null}), sumber domain kustom bila ada
     * @return URL kanonis absolut, atau path relatif bila domain kustom maupun host permintaan
     *         tidak valid untuk dijadikan URL absolut
     */
    private String canonical(HttpServletRequest request, SchoolLandingService.SchoolProfile profile) {
        Long id = profile == null ? null : profile.getId();
        String requestHost = request.getServerName() == null ? "" : request.getServerName().trim();
        String host = requestHost;
        if (profile != null && profile.getDomain() != null) {
            java.util.List<String> domains = Common.pisahDomain(profile.getDomain());
            if (!domains.isEmpty() && domains.get(0).matches("[A-Za-z0-9.-]+|[0-9a-fA-F:]+")) {
                host = domains.get(0);
            }
        }
        if (!host.matches("[A-Za-z0-9.-]+|[0-9a-fA-F:]+")) return request.getContextPath() + "/sekolah/" + id;
        boolean secure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        String scheme = secure ? "https" : "http";
        String authority = host.indexOf(':') >= 0 ? "[" + host + "]" : host;
        int port = host.equalsIgnoreCase(requestHost) ? request.getServerPort() : (secure ? 443 : 80);
        String portText = port == 80 || port == 443 ? "" : ":" + port;
        return scheme + "://" + authority + portText + request.getContextPath() + "/sekolah/" + id;
    }
}
