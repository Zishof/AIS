package ais.action.master.library.modern.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.action.master.library.modern.LibraryPortalSecurityFilter;

/** Fast header contract check for the /pustaka security filter. */
public final class LibraryPortalSecurityFilterSelfTest {
    private LibraryPortalSecurityFilterSelfTest() { }

    public static void main(String[] args) throws Exception {
        final Map<String, Object> requestValues = new HashMap<String, Object>();
        requestValues.put("getParameter:s", "_catalog_api");
        requestValues.put("getRequestURI", "/pustaka");
        requestValues.put("getRemoteAddr", "127.0.0.1");
        requestValues.put("isSecure", Boolean.TRUE);
        final Map<String, Object> responseValues = new HashMap<String, Object>();
        responseValues.put("getStatus", Integer.valueOf(200));
        HttpServletRequest request = proxy(HttpServletRequest.class, requestValues);
        HttpServletResponse response = proxy(HttpServletResponse.class, responseValues);
        new LibraryPortalSecurityFilter().doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) { }
        });
        check(header(responseValues, "X-Content-Type-Options").equals("nosniff"), "nosniff tidak dipasang");
        check(header(responseValues, "Content-Security-Policy").contains("object-src 'none'"), "CSP tidak dipasang");
        check(header(responseValues, "Cache-Control").equals("no-store"), "API masih dapat di-cache");
        check(header(responseValues, "Strict-Transport-Security").contains("max-age"), "HSTS tidak dipasang pada HTTPS");
        check(header(responseValues, "X-Request-Id").length() > 10, "Request ID tidak tersedia");
        check(header(responseValues, "Server-Timing").startsWith("app;dur="), "Server timing tidak tersedia");
        System.out.println("LibraryPortalSecurityFilterSelfTest OK headers api-cache telemetry");
    }

    private static String header(Map<String, Object> values, String name) {
        Object value = values.get("header:" + name);
        return value == null ? "" : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, final Map<String, Object> values) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("setHeader".equals(method.getName())) { values.put("header:" + String.valueOf(args[0]), args[1]); return null; }
                if ("setAttribute".equals(method.getName())) { values.put("attribute:" + String.valueOf(args[0]), args[1]); return null; }
                String key = method.getName();
                if (args != null && args.length == 1) key += ":" + String.valueOf(args[0]);
                if (values.containsKey(key)) return values.get(key);
                Class<?> result = method.getReturnType();
                if (result == Boolean.TYPE) return Boolean.FALSE;
                if (result == Integer.TYPE) return Integer.valueOf(0);
                if (result == Long.TYPE) return Long.valueOf(0L);
                return null;
            }
        });
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
