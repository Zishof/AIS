package ais.action.master.jurnal.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.PortalLoginApi;

/** Fast contract checks for the journal portal login boundary. */
public final class JurnalPortalLoginSelfTest {
    private JurnalPortalLoginSelfTest() { }

    public static void main(String[] args) throws Exception {
        HttpServletResponse response = proxy(HttpServletResponse.class, new HashMap<String, Object>());
        Map<String, Object> getValues = new HashMap<String, Object>();
        getValues.put("getMethod", "GET");
        JSONObject getResult = PortalLoginApi.handle(proxy(HttpServletRequest.class, getValues), response, "jurnal");
        check(!getResult.optBoolean("ok") && getResult.optString("message").indexOf("Metode") >= 0,
                "GET login harus ditolak");

        final Map<String, Object> sessionValues = new HashMap<String, Object>();
        sessionValues.put(NewUiCsrfUtil.SESSION_KEY, "token-benar");
        HttpSession session = proxy(HttpSession.class, sessionValues);
        Map<String, Object> postValues = new HashMap<String, Object>();
        postValues.put("getMethod", "POST");
        postValues.put("getSession:false", session);
        postValues.put("getParameter:" + NewUiCsrfUtil.PARAM, "token-salah");
        JSONObject csrfResult = PortalLoginApi.handle(proxy(HttpServletRequest.class, postValues), response, "jurnal");
        check(!csrfResult.optBoolean("ok") && csrfResult.optString("message").indexOf("Token") >= 0,
                "CSRF login harus fail-closed");
        check(!sessionValues.containsKey("jurnal.login.failures"), "CSRF invalid tidak boleh menghitung kegagalan password");
        System.out.println("JurnalPortalLoginSelfTest OK");
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, final Map<String, Object> values) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                String key = method.getName();
                if (args != null && args.length == 1) key += ":" + String.valueOf(args[0]);
                if ("getAttribute".equals(method.getName())) return values.get(String.valueOf(args[0]));
                if ("setAttribute".equals(method.getName())) { values.put(String.valueOf(args[0]), args[1]); return null; }
                if ("removeAttribute".equals(method.getName())) { values.remove(String.valueOf(args[0])); return null; }
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
