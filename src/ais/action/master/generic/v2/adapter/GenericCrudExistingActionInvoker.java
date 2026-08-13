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
        try {
            if (actionClass == null) return false;
            if (GenericCrudAction.class.isAssignableFrom(actionClass)) return hasDefaultConstructor(actionClass);
            return hasDefaultConstructor(actionClass)
                    && !hostFields(actionClass).isEmpty()
                    && lifecycleEventSave(actionClass) != null
                    && hasEntityInit(actionClass);
        } catch (Throwable unavailableDependency) {
            return false;
        }
    }

    public static boolean supports(Class actionClass, Class entityClass) {
        try {
            if (actionClass == null || entityClass == null || !hasDefaultConstructor(actionClass)) return false;
            if (GenericCrudAction.class.isAssignableFrom(actionClass)) return true;
            return !hostFields(actionClass).isEmpty()
                    && lifecycleEventSave(actionClass) != null
                    && compatibleInit(actionClass, entityClass) != null;
        } catch (Throwable unavailableDependency) {
            return false;
        }
    }

    public static boolean supportsCreate(Class actionClass, Class entityClass) {
        return supports(actionClass, entityClass) && hasDefaultConstructor(entityClass);
    }

    /** Ringkasan kontrak untuk audit CLI; tidak memuat atau mengeksekusi halaman ZUL. */
    public static String supportDiagnostics(Class actionClass, Class entityClass) {
        try {
            if (actionClass == null) return "action=missing";
            return "ctor=" + hasDefaultConstructor(actionClass)
                    + ",generic=" + GenericCrudAction.class.isAssignableFrom(actionClass)
                    + ",host=" + hostFields(actionClass).size()
                    + ",eventOnSave=" + saveContract(actionClass)
                    + ",entityInit=" + (compatibleInit(actionClass, entityClass) != null);
        } catch (Throwable failed) {
            return "reflectionError=" + failed.getClass().getName();
        }
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
            init.invoke(action, initArguments(actionClass, init, target));
            Method save = lifecycleEventSave(actionClass);
            Object result = save.invoke(action, new Object[] { null });
            accepted = save.getReturnType() == Void.TYPE || Boolean.TRUE.equals(result);
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
        catch (Throwable missing) { return false; }
    }

    private static Method lifecycleEventSave(Class type) {
        try {
            Method method = type.getMethod("onSave", new Class[] { Event.class });
            return method.getReturnType() == Boolean.TYPE || method.getReturnType() == Boolean.class
                    || method.getReturnType() == Void.TYPE
                    ? method : null;
        } catch (Throwable missing) { return null; }
    }

    private static String saveContract(Class type) {
        Method method = lifecycleEventSave(type);
        return method == null ? "none" : method.getReturnType().getName();
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
        if (fallback != null) return fallback;
        Boolean configured = configuredBooleanInit(type);
        if (configured == null) return null;
        current = type;
        while (current != null && current != Object.class) {
            Method[] methods = current.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                Class[] parameters = methods[i].getParameterTypes();
                if (!"init".equals(methods[i].getName()) || parameters.length != 2
                        || !parameters[0].isAssignableFrom(entityClass)) continue;
                if (parameters[1] == Boolean.TYPE || parameters[1] == Boolean.class) return methods[i];
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Object[] initArguments(Class actionClass, Method init, GeneralValueObject target) {
        if (init.getParameterTypes().length == 1) return new Object[] { target };
        return new Object[] { target, configuredBooleanInit(actionClass) };
    }

    /** Nilai flag mengikuti arti parameter pada Action existing, bukan ditebak dari ID entity. */
    private static Boolean configuredBooleanInit(Class actionClass) {
        if (actionClass == null) return null;
        String name = actionClass.getName();
        if ("ais.action.master.KurikulumAction".equals(name)) return Boolean.FALSE; // copy=false
        if ("ais.action.master.SkripsiAction".equals(name)) return Boolean.TRUE; // tampilkanSimpan=true
        return null;
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
