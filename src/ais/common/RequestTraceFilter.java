package ais.common;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter diagnosis request global.
 *
 * Fungsi:
 * - Membuktikan request sudah masuk ke web application Tomcat.
 * - Mengetahui apakah request berhenti sebelum servlet.
 * - Menampilkan durasi filter-chain tanpa mengubah response normal aplikasi.
 *
 * Kompatibel dengan Java 1.6/1.7 dan Servlet 2.5.
 */
public class RequestTraceFilter implements Filter {

    private static final String PREFIX = "[REQUEST-TRACE] ";

    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println(PREFIX + "INIT");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
    	
    	if(true) {
    		return;
    	}

        long start = System.currentTimeMillis();
        String traceId = String.valueOf(start) + "-" + System.identityHashCode(request);

        HttpServletRequest httpRequest = request instanceof HttpServletRequest ? (HttpServletRequest) request : null;
        HttpServletResponse httpResponse = response instanceof HttpServletResponse ? (HttpServletResponse) response : null;

        String method = httpRequest == null ? "-" : safe(httpRequest.getMethod());
        String uri = httpRequest == null ? "-" : safe(httpRequest.getRequestURI());
        String query = httpRequest == null ? "-" : safe(httpRequest.getQueryString());
        String remote = httpRequest == null ? "-" : safe(httpRequest.getRemoteAddr());
        String sessionId = getSessionId(httpRequest);

        log(traceId, start, "BEFORE chain method=" + method + " uri=" + uri + " query=" + query
                + " remote=" + remote + " session=" + sessionId);

        if (httpRequest != null && "true".equalsIgnoreCase(httpRequest.getParameter("trace_headers"))) {
            printHeaders(httpRequest, traceId, start);
        }

        try {
            chain.doFilter(request, response);
            int status = getStatusSafely(httpResponse);
            log(traceId, start, "AFTER chain OK status=" + status + " uri=" + uri);
        } catch (Throwable t) {
            log(traceId, start, "ERROR chain uri=" + uri + " error=" + safeMessage(t));
            if (t instanceof IOException) {
                throw (IOException) t;
            }
            if (t instanceof ServletException) {
                throw (ServletException) t;
            }
            throw new ServletException(t);
        } finally {
            log(traceId, start, "FINALLY uri=" + uri);
        }
    }

    public void destroy() {
        System.out.println(PREFIX + "DESTROY");
    }

    private static void printHeaders(HttpServletRequest request, String traceId, long start) {
        try {
            Enumeration names = request.getHeaderNames();
            while (names != null && names.hasMoreElements()) {
                String name = String.valueOf(names.nextElement());
                log(traceId, start, "HEADER " + name + "=" + safe(request.getHeader(name)));
            }
        } catch (Exception e) {
            log(traceId, start, "HEADER ERROR " + safeMessage(e));
        }
    }

    private static String getSessionId(HttpServletRequest request) {
        if (request == null) {
            return "-";
        }
        try {
            if (request.getSession(false) != null) {
                return request.getSession(false).getId();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/RequestTraceFilter.java:101");
        }
        return "-";
    }

    private static int getStatusSafely(HttpServletResponse response) {
        if (response == null) {
            return -1;
        }
        try {
            /* getStatus tersedia di Servlet 3.x. Pada Servlet 2.5, pemanggilan langsung
             * tidak dipakai agar tetap aman untuk container lama. */
            return -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static void log(String traceId, long start, String message) {
        long elapsed = System.currentTimeMillis() - start;
        System.out.println(PREFIX + "trace=" + traceId + " elapsed=" + elapsed + "ms " + message);
    }

    private static String safe(String value) {
        return value == null ? "-" : value;
    }

    private static String safeMessage(Throwable t) {
        if (t == null) {
            return "";
        }
        String message = t.getMessage();
        if (message == null || message.trim().length() == 0) {
            return t.getClass().getName();
        }
        return t.getClass().getName() + ": " + message;
    }
}
