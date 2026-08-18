package ais.action.master.generic.v2.adapter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Window;

import ais.action.master.generic.GenericCrudAction;
import ais.common.HeadlessActionContext;
import ais.common.HeadlessBusinessRuleException;
import ais.database.model.GeneralValueObject;
import ais.ui.util.MyWindow;

/** Menjalankan form/init dan onSave Action existing tanpa merender halaman ZUL. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class GenericCrudExistingActionInvoker {
    private GenericCrudExistingActionInvoker() { }

    public static boolean supports(Class actionClass) {
        if (actionClass == null) return false;
        if (GenericCrudAction.class.isAssignableFrom(actionClass)) return hasDefaultConstructor(actionClass);
        return hasDefaultConstructor(actionClass)
                && !hostFields(actionClass).isEmpty()
                && booleanEventSave(actionClass) != null
                && hasEntityInit(actionClass);
    }

    public static boolean supports(Class actionClass, Class entityClass) {
        if (actionClass == null || entityClass == null || !hasDefaultConstructor(actionClass)) return false;
        if (GenericCrudAction.class.isAssignableFrom(actionClass)) return true;
        return !hostFields(actionClass).isEmpty()
                && booleanEventSave(actionClass) != null
                && compatibleInit(actionClass, entityClass) != null;
    }

    public static boolean supportsCreate(Class actionClass, Class entityClass) {
        return supports(actionClass, entityClass) && hasDefaultConstructor(entityClass);
    }

    public static void execute(Class actionClass, GeneralValueObject target) throws Exception {
        if (target == null || !supports(actionClass, target.getClass())) {
            throw new HeadlessBusinessRuleException(
                    "Action existing belum mempunyai kontrak init/onSave yang aman untuk New UI.");
        }
        Object action = newInstance(actionClass);
        if (action instanceof GenericCrudAction) {
            ((GenericCrudAction) action).executeHeadlessSave(target);
            return;
        }

        MyWindow window = new MyWindow(true);
        String captured = null;
        boolean accepted = false;
        try {
            List fields = hostFields(actionClass);
            for (int i = 0; i < fields.size(); i++) {
                Field field = (Field) fields.get(i);
                field.setAccessible(true);
                field.set(action, window);
            }
            HeadlessActionContext.enter();
            Method init = compatibleInit(actionClass, target.getClass());
            if (init == null) {
                throw new HeadlessBusinessRuleException(
                        "Action existing tidak mempunyai init(entity) yang cocok dengan model ini.");
            }
            init.setAccessible(true);
            init.invoke(action, new Object[] { target });
            Object result = booleanEventSave(actionClass).invoke(action, new Object[] { null });
            accepted = Boolean.TRUE.equals(result);
        } catch (InvocationTargetException wrapped) {
            Throwable cause = wrapped.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            throw new RuntimeException(cause);
        } finally {
            if (HeadlessActionContext.isActive()) captured = HeadlessActionContext.exit();
            try { window.detach(); } catch (Exception ignored) { }
        }
        if (!accepted) {
            throw new HeadlessBusinessRuleException(captured == null || captured.length() == 0
                    ? "Validasi business rule existing menolak penyimpanan data." : captured);
        }
    }

    private static Object newInstance(Class actionClass) throws Exception {
        Constructor constructor = actionClass.getDeclaredConstructor(new Class[0]);
        if (!constructor.isAccessible()) constructor.setAccessible(true);
        return constructor.newInstance(new Object[0]);
    }

    private static boolean hasDefaultConstructor(Class type) {
        try { type.getDeclaredConstructor(new Class[0]); return true; }
        catch (Exception missing) { return false; }
    }

    private static Method booleanEventSave(Class type) {
        try {
            Method method = type.getMethod("onSave", new Class[] { Event.class });
            return method.getReturnType() == Boolean.TYPE || method.getReturnType() == Boolean.class
                    ? method : null;
        } catch (Exception missing) { return null; }
    }

    private static boolean hasEntityInit(Class type) {
        Class current = type;
        while (current != null && current != Object.class) {
            Method[] methods = current.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                Class[] parameters = methods[i].getParameterTypes();
                if ("init".equals(methods[i].getName()) && parameters.length == 1
                        && GeneralValueObject.class.isAssignableFrom(parameters[0])) return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static Method compatibleInit(Class type, Class entityClass) {
        Method fallback = null;
        Class current = type;
        while (current != null && current != Object.class) {
            Method[] methods = current.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                Class[] parameters = methods[i].getParameterTypes();
                if (!"init".equals(methods[i].getName()) || parameters.length != 1
                        || !GeneralValueObject.class.isAssignableFrom(parameters[0])
                        || !parameters[0].isAssignableFrom(entityClass)) continue;
                if (parameters[0].equals(entityClass)) return methods[i];
                fallback = methods[i];
            }
            current = current.getSuperclass();
        }
        return fallback;
    }

    /**
     * Action lama tidak konsisten mendeklarasikan container form: sebagian
     * memakai Window/MyWindow, sebagian sengaja memakai interface Component.
     * Hanya field container yang bernama window/dialog yang diinjeksi; field UI
     * lain (button, grid, textbox) tidak pernah ditebak atau dioverride.
     */
    private static List hostFields(Class type) {
        List result = new ArrayList();
        Class current = type;
        while (current != null && current != Object.class) {
            Field[] fields = current.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Class fieldType = fields[i].getType();
                String name = fields[i].getName().toLowerCase();
                boolean containerName = name.indexOf("window") >= 0 || name.indexOf("dialog") >= 0;
                if (!Modifier.isStatic(fields[i].getModifiers()) && containerName
                        && Component.class.isAssignableFrom(fieldType)
                        && fieldType.isAssignableFrom(MyWindow.class)) result.add(fields[i]);
            }
            current = current.getSuperclass();
        }
        return result;
    }
}
