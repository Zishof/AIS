package ais.action.master.generic.v2.test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudZkossParityService;
import ais.database.model.Agama;

/** Uji parser resource WAR tanpa container dan tanpa database. */
@SuppressWarnings("rawtypes")
public final class GenericCrudZkossParityServiceSelfTest {
    private GenericCrudZkossParityServiceSelfTest() { }

    public static void main(String[] args) throws Exception {
        final String zul = "<window apply=\"ais.action.master.AgamaAction\">"
                + "<button forward=\"onClick=onAdd\"/>"
                + "<textbox forward=\"onOK=onSearchDefault\"/>"
                + "<button onClick=\"onImport(event)\"/></window>";
        final ServletContext servlet = (ServletContext) Proxy.newProxyInstance(
                GenericCrudZkossParityServiceSelfTest.class.getClassLoader(),
                new Class[] { ServletContext.class }, new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
                        String name = method.getName(); String path = args == null ? null : String.valueOf(args[0]);
                        if ("getResourcePaths".equals(name)) {
                            if ("/WEB-INF/z/x/y/".equals(path)) return set("/WEB-INF/z/x/y/pages/");
                            if ("/WEB-INF/z/x/y/pages/".equals(path)) return set("/WEB-INF/z/x/y/pages/master/");
                            if ("/WEB-INF/z/x/y/pages/master/".equals(path)) return set("/WEB-INF/z/x/y/pages/master/agama.zul");
                            return Collections.EMPTY_SET;
                        }
                        if ("getResourceAsStream".equals(name)) return new ByteArrayInputStream(zul.getBytes("UTF-8"));
                        return defaultValue(method.getReturnType());
                    }
                });
        final HttpSession session = (HttpSession) Proxy.newProxyInstance(
                GenericCrudZkossParityServiceSelfTest.class.getClassLoader(),
                new Class[] { HttpSession.class }, new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("getServletContext".equals(method.getName())) return servlet;
                        return defaultValue(method.getReturnType());
                    }
                });
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                GenericCrudZkossParityServiceSelfTest.class.getClassLoader(),
                new Class[] { HttpServletRequest.class }, new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("getSession".equals(method.getName())) return session;
                        if ("getContextPath".equals(method.getName())) return "/ais";
                        return defaultValue(method.getReturnType());
                    }
                });
        GenericCrudDefinition definition = new GenericCrudDefinition();
        definition.setEntityClass(Agama.class);
        GenericCrudRequestContext context = new GenericCrudRequestContext();
        field(context, "request", request); field(context, "definition", definition); field(context, "canRead", Boolean.TRUE);
        List actions = new GenericCrudZkossParityService().actions(context);
        check(actions.size() == 3, "Handler forward/direct ZUL tidak terbaca lengkap");
        Map first = (Map) actions.get(0);
        check("NEW_UI_NATIVE_PANEL".equals(first.get("implementationStatus")), "Status panel native salah");
        check(first.get("legacyRoute") == null, "Panel native tidak boleh mempunyai route tampilan lain");
        check(first.get("nativePanelKey") != null, "Panel native harus mempunyai key");
        System.out.println("PASS Generic CRUD native parity inventory self-test");
    }

    private static Set set(String value) { return new HashSet(Arrays.asList(new String[] { value })); }
    private static void field(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static Object defaultValue(Class type) {
        if (!type.isPrimitive()) return null;
        if (Boolean.TYPE.equals(type)) return Boolean.FALSE;
        if (Character.TYPE.equals(type)) return Character.valueOf('\0');
        if (Byte.TYPE.equals(type)) return Byte.valueOf((byte) 0);
        if (Short.TYPE.equals(type)) return Short.valueOf((short) 0);
        if (Integer.TYPE.equals(type)) return Integer.valueOf(0);
        if (Long.TYPE.equals(type)) return Long.valueOf(0L);
        if (Float.TYPE.equals(type)) return Float.valueOf(0F);
        if (Double.TYPE.equals(type)) return Double.valueOf(0D);
        return null;
    }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
