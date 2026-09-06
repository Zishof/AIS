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

    /**
     * Menangani GET dengan mendelegasikan langsung ke {@link #processRequest}.
     *
     * @param request  request HTTP masuk yang akan diteruskan ke shell mobile JSP
     * @param response response HTTP keluar yang menerima hasil forward
     * @throws ServletException bila {@link #processRequest} membungkus exception non-servlet/
     *                          non-IO sebagai {@code ServletException}
     * @throws IOException      bila forward ke JSP gagal karena kegagalan I/O
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Menangani POST dengan perilaku identik seperti {@link #doGet}: mendelegasikan langsung
     * ke {@link #processRequest}.
     *
     * @param request  request HTTP masuk yang akan diteruskan ke shell mobile JSP
     * @param response response HTTP keluar yang menerima hasil forward
     * @throws ServletException idem {@link #doGet}
     * @throws IOException      idem {@link #doGet}
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Menyiapkan state global aplikasi lewat {@link #initCommonRequestState}, lalu meneruskan
     * (forward, bukan redirect — URL browser tidak berubah) request ke {@link #MOBILE_JSP}.
     *
     * <p>Exception dari {@link javax.servlet.RequestDispatcher#forward} diteruskan apa adanya
     * bila sudah berupa {@link ServletException} atau {@link IOException}; jenis lain
     * dibungkus sebagai {@link ServletException} agar tetap sesuai kontrak method servlet.</p>
     *
     * @param request  request HTTP masuk yang diteruskan ke {@link #MOBILE_JSP}
     * @param response response HTTP keluar yang menerima hasil forward
     * @throws ServletException bila forward gagal dengan {@code ServletException}, atau bila
     *                          exception lain dibungkus menjadi ini
     * @throws IOException      bila forward gagal karena kegagalan I/O
     */
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

    /**
     * Mengisi ulang field statis {@link Common} yang menyimpan path/URL aplikasi saat ini,
     * yang masih dibaca banyak modul lama sebagai sumber path absolut/URL dasar.
     *
     * <p><b>Perhatian konkurensi:</b> {@link Common#REAL_PATH}, {@link Common#ROOT},
     * {@link Common#CURRENT_URL}, dan {@link Common#CURRENT_URL_SIMPLE} adalah field statis
     * (bukan {@code ThreadLocal}) yang ditimpa method ini pada SETIAP request. {@code REAL_PATH}
     * dan {@code ROOT} pada praktiknya stabil (satu context/deployment path per instalasi),
     * namun {@code CURRENT_URL}/{@code CURRENT_URL_SIMPLE} bisa berbeda antar request bila
     * aplikasi diakses lewat lebih dari satu hostname/skema secara bersamaan — dua request
     * konkuren berpotensi saling menimpa nilai ini sebelum salah satunya sempat dipakai,
     * pola field statis termutasi yang sudah dikenal berulang di basis kode ini.</p>
     *
     * @param request request HTTP masuk yang menjadi sumber context path, hostname, skema, dan
     *                port untuk membangun ulang nilai path/URL global
     */
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

    /**
     * Membangun URL dasar (skema + host + port bila bukan default + opsional context path)
     * dari sebuah request, dipakai {@link #initCommonRequestState} untuk mengisi
     * {@link Common#CURRENT_URL} dan {@link Common#CURRENT_URL_SIMPLE}.
     *
     * @param request        request HTTP sumber skema ({@link Common#isSecure}), hostname,
     *                       dan port
     * @param includeContext {@code true} untuk menambahkan context path aplikasi di akhir URL
     *                       ({@link Common#CURRENT_URL}), {@code false} untuk URL dasar saja
     *                       tanpa context path ({@link Common#CURRENT_URL_SIMPLE})
     * @return URL dasar berbentuk {@code skema://host[:port][/contextPath]}; port disertakan
     *         hanya bila bukan 80 (HTTP) atau 443 (HTTPS)
     */
    private String buildBaseUrl(HttpServletRequest request, boolean includeContext) {
        String protocol = Common.isSecure(request) ? "https://" : "http://";
        int port = request.getServerPort();
        String portText = (port == 80 || port == 443) ? "" : ":" + port;
        String context = includeContext ? request.getContextPath() : "";
        return protocol + request.getServerName() + portText + context;
    }
}
