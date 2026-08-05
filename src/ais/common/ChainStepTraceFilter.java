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
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Filter diagnosis bertahap untuk mencari titik request berhenti di filter-chain.
 *
 * Cara pakai:
 * - Daftarkan class ini beberapa kali dengan nama filter berbeda.
 * - Beri init-param "label" berbeda, misalnya 01-BEFORE-ERROR-AUDIT.
 * - Urutkan filter-mapping di web.xml di sela-sela filter utama.
 *
 * Kompatibel Java 1.6/1.7 dan Servlet 2.5.
 */
public class ChainStepTraceFilter implements Filter {

    private static final String PREFIX = "[CHAIN-TRACE] ";
    private String label = "-";

    public void init(FilterConfig filterConfig) throws ServletException {
        if (filterConfig != null && filterConfig.getInitParameter("label") != null) {
            label = filterConfig.getInitParameter("label");
        }
        System.out.println(PREFIX + label + " INIT");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        long start = System.currentTimeMillis();
        String traceId = String.valueOf(start) + "-" + System.identityHashCode(request);
        HttpServletRequest httpRequest = request instanceof HttpServletRequest ? (HttpServletRequest) request : null;
        HttpServletResponse httpResponse = response instanceof HttpServletResponse ? (HttpServletResponse) response : null;

        String method = httpRequest == null ? "-" : safe(httpRequest.getMethod());
        String uri = httpRequest == null ? "-" : safe(httpRequest.getRequestURI());
        String query = httpRequest == null ? "-" : safe(httpRequest.getQueryString());
        String remote = httpRequest == null ? "-" : safe(httpRequest.getRemoteAddr());
        String session = sessionId(httpRequest);

        StatusCaptureResponse wrappedResponse = httpResponse == null ? null : new StatusCaptureResponse(httpResponse);

        System.out.println(PREFIX + label + " trace=" + traceId
                + " elapsed=0ms BEFORE chain method=" + method
                + " uri=" + uri
                + " query=" + query
                + " remote=" + remote
                + " session=" + session);

        if (httpRequest != null && "true".equalsIgnoreCase(httpRequest.getParameter("trace_headers"))) {
            printHeaders(httpRequest, label, traceId, start);
        }

        try {
            if (wrappedResponse != null) {
                chain.doFilter(request, wrappedResponse);
            } else {
                chain.doFilter(request, response);
            }
            System.out.println(PREFIX + label + " trace=" + traceId
                    + " elapsed=" + elapsed(start)
                    + "ms AFTER chain OK status=" + (wrappedResponse == null ? "-" : String.valueOf(wrappedResponse.getCapturedStatus()))
                    + " uri=" + uri);
        } catch (Throwable t) {
            System.out.println(PREFIX + label + " trace=" + traceId
                    + " elapsed=" + elapsed(start)
                    + "ms ERROR chain uri=" + uri
                    + " error=" + safeMessage(t));
            if (t instanceof IOException) {
                throw (IOException) t;
            }
            if (t instanceof ServletException) {
                throw (ServletException) t;
            }
            throw new ServletException(t);
        } finally {
            System.out.println(PREFIX + label + " trace=" + traceId
                    + " elapsed=" + elapsed(start)
                    + "ms FINALLY uri=" + uri
                    + " committed=" + (httpResponse == null ? "-" : String.valueOf(httpResponse.isCommitted())));
        }
    }

    public void destroy() {
        System.out.println(PREFIX + label + " DESTROY");
    }

    private static long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    private static String sessionId(HttpServletRequest request) {
        try {
            if (request != null && request.getSession(false) != null) {
                return request.getSession(false).getId();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ChainStepTraceFilter.java:108");
        }
        return "-";
    }

    private static void printHeaders(HttpServletRequest request, String label, String traceId, long start) {
        try {
            Enumeration names = request.getHeaderNames();
            while (names != null && names.hasMoreElements()) {
                String name = String.valueOf(names.nextElement());
                System.out.println(PREFIX + label + " trace=" + traceId
                        + " elapsed=" + elapsed(start)
                        + "ms HEADER " + name + "=" + request.getHeader(name));
            }
        } catch (Exception e) {
            System.out.println(PREFIX + label + " trace=" + traceId
                    + " elapsed=" + elapsed(start)
                    + "ms HEADER ERROR " + safeMessage(e));
        }
    }

    private static String safe(String s) {
        return s == null ? "-" : s;
    }

    private static String safeMessage(Throwable t) {
        if (t == null) {
            return "";
        }
        String msg = t.getMessage();
        if (msg == null || msg.trim().length() == 0) {
            return t.getClass().getName();
        }
        return t.getClass().getName() + ": " + msg;
    }

    private static class StatusCaptureResponse extends HttpServletResponseWrapper {
        private int status = -1;

        StatusCaptureResponse(HttpServletResponse response) {
            super(response);
        }

        public void setStatus(int sc) {
            status = sc;
            super.setStatus(sc);
        }

        public void setStatus(int sc, String sm) {
            status = sc;
            super.setStatus(sc, sm);
        }

        public void sendError(int sc) throws IOException {
            status = sc;
            super.sendError(sc);
        }

        public void sendError(int sc, String msg) throws IOException {
            status = sc;
            super.sendError(sc, msg);
        }

        public void sendRedirect(String location) throws IOException {
            status = 302;
            super.sendRedirect(location);
        }

        int getCapturedStatus() {
            return status;
        }
    }
}
