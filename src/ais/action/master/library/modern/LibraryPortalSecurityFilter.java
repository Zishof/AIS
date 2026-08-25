package ais.action.master.library.modern;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/** Compatible security headers and request telemetry for the legacy JSP-based library portal. */
public final class LibraryPortalSecurityFilter implements Filter {
    public void init(FilterConfig filterConfig) throws ServletException { }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        StatusResponse statusResponse = new StatusResponse(res);
        String requestId = UUID.randomUUID().toString();
        long started = System.currentTimeMillis();
        req.setAttribute("libraryRequestId", requestId);
        res.setHeader("X-Request-Id", requestId);
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        res.setHeader("X-Frame-Options", "SAMEORIGIN");
        res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=(), usb=()");
        res.setHeader("Content-Security-Policy", "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'self'; form-action 'self'; img-src 'self' data: https:; media-src 'self' https:; frame-src 'self' https:; font-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; connect-src 'self' https:");
        if (isHttps(req)) res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        if (isApi(req)) {
            res.setHeader("Cache-Control", "no-store");
            res.setHeader("X-Robots-Tag", "noindex, nofollow");
        }
        try {
            chain.doFilter(request, statusResponse);
        } finally {
            long duration = System.currentTimeMillis() - started;
            res.setHeader("Server-Timing", "app;dur=" + duration);
            LibraryTelemetry.record(route(req), statusResponse.getRecordedStatus(), duration);
        }
    }

    private boolean isApi(HttpServletRequest request) {
        String route = request.getParameter("s");
        return route != null && (route.endsWith("_api") || route.endsWith("_service") || "_oai".equals(route));
    }

    private String route(HttpServletRequest request) {
        String route = request.getParameter("s");
        return route == null || route.trim().length() == 0 ? "portal" : route;
    }

    private boolean isHttps(HttpServletRequest request) {
        if (request.isSecure()) return true;
        String forwarded = request.getHeader("X-Forwarded-Proto");
        return forwarded != null && "https".equalsIgnoreCase(forwarded.trim());
    }

    /** Tracks the response code without requiring the Servlet 3.0 getStatus() API. */
    private static final class StatusResponse extends HttpServletResponseWrapper {
        private int status = HttpServletResponse.SC_OK;

        private StatusResponse(HttpServletResponse response) { super(response); }
        public void setStatus(int value) { status = value; super.setStatus(value); }
        @SuppressWarnings("deprecation")
        public void setStatus(int value, String message) { status = value; super.setStatus(value, message); }
        public void sendError(int value) throws IOException { status = value; super.sendError(value); }
        public void sendError(int value, String message) throws IOException { status = value; super.sendError(value, message); }
        public void sendRedirect(String location) throws IOException {
            status = HttpServletResponse.SC_MOVED_TEMPORARILY;
            super.sendRedirect(location);
        }
        private int getRecordedStatus() { return status; }
    }

    public void destroy() { }
}
