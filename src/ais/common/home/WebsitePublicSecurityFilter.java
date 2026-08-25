package ais.common.home;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Security and observability baseline for the tenant-facing public website. */
public class WebsitePublicSecurityFilter implements Filter {
    private final SecureRandom random = new SecureRandom();

    public void init(FilterConfig filterConfig) throws ServletException { }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String requestId = Long.toHexString(System.currentTimeMillis())
                + Integer.toHexString(System.identityHashCode(req));
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        req.setAttribute("websiteCspNonce", nonce);
        req.setAttribute("websiteRequestId", requestId);

        res.setHeader("X-Request-Id", requestId);
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");
        res.setHeader("X-Frame-Options", "SAMEORIGIN");
        res.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        String csp =
                "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'self'; "
                + "form-action 'self'; img-src 'self' data: https:; font-src 'self' data:; "
                + "style-src 'self' 'unsafe-inline'; script-src 'self' 'nonce-" + nonce + "'; "
                + "connect-src 'self'";
        if (isHttps(req)) csp += "; upgrade-insecure-requests";
        res.setHeader("Content-Security-Policy", csp);
        if (isHttps(req)) {
            res.setHeader("Strict-Transport-Security", "max-age=31536000");
        }
        if ("GET".equalsIgnoreCase(req.getMethod()) && req.getRequestURI() != null
                && req.getRequestURI().startsWith(req.getContextPath() + "/web")) {
            res.setHeader("Cache-Control", "public, max-age=60, stale-while-revalidate=300");
            res.setHeader("Vary", "Accept-Language");
        }
        chain.doFilter(request, response);
    }

    private boolean isHttps(HttpServletRequest req) {
        if (req.isSecure()) return true;
        String forwarded = req.getHeader("X-Forwarded-Proto");
        return forwarded != null && "https".equalsIgnoreCase(forwarded.trim());
    }

    public void destroy() { }
}
