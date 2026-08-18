package ais.common;

import javax.servlet.http.HttpServletResponse;

public class ResponseContext {
    // ThreadLocal bertindak seperti Map<Thread, HttpServletResponse>
    private static final ThreadLocal<HttpServletResponse> RESPONSE_HOLDER = new ThreadLocal<HttpServletResponse>();

    public static void set(HttpServletResponse request) {
    	RESPONSE_HOLDER.set(request);
    }

    public static HttpServletResponse get() {
        return RESPONSE_HOLDER.get();
    }

    public static void remove() {
    	RESPONSE_HOLDER.remove();
    }
}