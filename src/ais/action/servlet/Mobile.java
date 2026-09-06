package ais.action.servlet;

import ais.common.Common;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet shell mobile AIS — versi JSP (tidak memerlukan kompilasi ulang untuk perubahan tampilan).
 *
 * <p>Servlet ini meneruskan semua request ke halaman mobile berbasis JSP+Bootstrap yang berada di
 * {@code /WEB-INF/baru/modul/mobile/index.jsp}. Pendekatan ini menggantikan {@code mobile.zul}
 * (ZKoss) sehingga perubahan tampilan UI/UX cukup menyunting file JSP tanpa harus mengompilasi
 * ulang kode Java.</p>
 *
 * <p>Sebelum meneruskan request, servlet mengisi nilai global path aplikasi
 * ({@link Common#REAL_PATH}, {@link Common#ROOT}, dsb.) yang masih dipakai banyak modul lama.
 * Tidak ada Hibernate session yang dibuka di sini, sehingga tidak ada yang perlu ditutup di
 * blok {@code finally}.</p>
 *
 * <p>Kompatibel Java 1.7. Tidak ada lambda, stream, atau try-with-resources.</p>
 */
public class Mobile extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Path JSP shell mobile — di bawah WEB-INF agar tidak bisa diakses langsung. */
    private static final String MOBILE_JSP = "/WEB-INF/baru/modul/mobile/index.jsp";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            initCommonRequestState(request);
            request.getRequestDispatcher(MOBILE_JSP).forward(request, response);
        } catch (ServletException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void initCommonRequestState(HttpServletRequest request) {
        ServletContext context = getServletContext();
        Common.REAL_PATH = context.getRealPath("/");
        Common.REAL_PATH_REPORT_TEMP = context.getRealPath("/report");
        Common.ROOT = request.getContextPath();
        if (Common.sanitizedRequestHostForCurrentUrl(request) != null) {
            Common.CURRENT_URL_SIMPLE = buildBaseUrl(request, false);
            Common.CURRENT_URL = buildBaseUrl(request, true);
        }
    }

    private String buildBaseUrl(HttpServletRequest request, boolean includeContext) {
        String protocol = Common.isSecure(request) ? "https://" : "http://";
        int port = request.getServerPort();
        String portText = (port == 80 || port == 443) ? "" : ":" + port;
        String context = includeContext ? request.getContextPath() : "";
        return protocol + request.getServerName() + portText + context;
    }
}
