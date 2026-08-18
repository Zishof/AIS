package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Portal KARIR modern.
 *
 * Route utama:
 * - /karir                                      -> landing page portal karir
 * - /karir?auth_action=logout                  -> logout calon pegawai
 * - /karir?hanya_tampil_jsp=true&p=karir&s=... -> service/partial JSP modul karir
 *
 * Catatan kompatibilitas:
 * - Tetap memakai servlet lama bernama Karir agar mapping web.xml lama tidak perlu diubah.
 * - Jika JSP modern belum dipasang, fallback legacy ZUL bisa diaktifkan kembali dari baris komentar.
 */
public class Karir extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public Karir() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            throw new ServletException(e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            throw new ServletException(e);
        }
    }

    private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            Common.ROOT = request.getContextPath();
            Common.REAL_PATH = getServletContext().getRealPath("/");
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Karir.java:55");
        }

        String authAction = request.getParameter("auth_action");
        if ("logout".equals(authAction)) {
            clearKarirSession(request);
            response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/karir?logout=1"));
            return;
        }

        String hanyaTampilJsp = request.getParameter("hanya_tampil_jsp");
        if ("true".equalsIgnoreCase(hanyaTampilJsp) || "1".equals(hanyaTampilJsp)) {
            String p = cleanPathPart(request.getParameter("p"));
            String s = cleanPathPart(request.getParameter("s"));
            if (p.length() == 0) {
                p = "karir";
            }
            if (s.length() == 0) {
                s = "landing_page";
            }
            request.getRequestDispatcher("/WEB-INF/baru/modul/" + p + "/" + s + ".jsp").forward(request, response);
            return;
        }

        request.getRequestDispatcher("/WEB-INF/baru/modul/karir/landing_page.jsp").forward(request, response);
        // Fallback legacy jika diperlukan:
        // request.getRequestDispatcher("/WEB-INF/z/x/y/karir.zul").forward(request, response);
    }

    private String cleanPathPart(String value) {
        if (value == null) {
            return "";
        }
        value = value.trim();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void clearKarirSession(HttpServletRequest request) {
        try {
            request.getSession().removeAttribute("KARIR_LOGGED_IN");
            request.getSession().removeAttribute("KARIR_USER_LOGGED_IN");
            request.getSession().removeAttribute("CalonPegawai");
            request.getSession().removeAttribute("mytbmuser");
            request.getSession().removeAttribute("usersTemp");
            request.getSession().removeAttribute("user");
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Karir.java:107");
        }
    }
}
