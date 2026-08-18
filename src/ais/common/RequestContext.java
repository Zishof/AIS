package ais.common;

import javax.servlet.http.HttpServletRequest;

public class RequestContext {
    // ThreadLocal bertindak seperti Map<Thread, HttpServletRequest>
    private static final ThreadLocal<HttpServletRequest> REQUEST_HOLDER = new ThreadLocal<HttpServletRequest>();

    public static void set(HttpServletRequest request) {
        REQUEST_HOLDER.set(request);
    }

    public static HttpServletRequest get() {
        return REQUEST_HOLDER.get();
    }

    public static void remove() {
        REQUEST_HOLDER.remove();
    }
}