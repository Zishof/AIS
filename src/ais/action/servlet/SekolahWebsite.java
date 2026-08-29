package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.action.servlet.landing.SchoolLandingService;
import ais.common.Common;

/** Endpoint website publik sekolah aktif: /sekolah/{id}. */
public class SekolahWebsite extends HttpServlet {
    private static final long serialVersionUID = 1L;

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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private void initCommonContext(HttpServletRequest request) {
        Common.REAL_PATH = getServletContext().getRealPath("/");
        Common.REAL_PATH_REPORT_TEMP = getServletContext().getRealPath("/report");
        Common.ROOT = request.getContextPath();
    }

    private Long schoolId(String path) {
        if (path == null || path.trim().length() == 0 || "/".equals(path.trim())) return null;
        String value = path.trim();
        while (value.startsWith("/")) value = value.substring(1);
        if (value.indexOf('/') >= 0 || !value.matches("[0-9]+")) return null;
        try { return Long.valueOf(value); } catch (Exception e) { return null; }
    }

    private boolean hasInvalidSchoolPath(String path, Long id) {
        return path != null && path.trim().length() > 0 && !"/".equals(path.trim()) && id == null;
    }

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
