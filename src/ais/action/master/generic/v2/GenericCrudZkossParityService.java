package ais.action.master.generic.v2;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import ais.common.newui.NewUiRouteRegistry;

/**
 * Menginventarisasi handler tampilan terdahulu agar tidak ada fungsi yang
 * terlewat saat dibuat sebagai panel native. Tidak pernah mengirim route UI.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class GenericCrudZkossParityService {
    private static final String ZUL_ROOT = "/WEB-INF/z/x/y/";
    private static final Pattern APPLY = Pattern.compile("\\bapply\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern FORWARD = Pattern.compile(
            "\\bforward\\s*=\\s*\"on\\w+\\s*=\\s*([A-Za-z_]\\w*)\"");
    private static final Pattern DIRECT_EVENT = Pattern.compile(
            "\\bon[A-Z]\\w*\\s*=\\s*\"\\s*([A-Za-z_]\\w*)\\s*\\(");
    private static final Object LOCK = new Object();
    private static volatile ServletContext cachedContext;
    private static volatile Map cachedByAction = Collections.EMPTY_MAP;

    public List actions(GenericCrudRequestContext context) {
        if (context == null || context.getRequest() == null || context.getDefinition() == null
                || context.getDefinition().getEntityClass() == null || !context.isCanRead()) {
            return Collections.EMPTY_LIST;
        }
        HttpSession session = context.getRequest().getSession(false);
        if (session == null) return Collections.EMPTY_LIST;
        ServletContext servletContext = session.getServletContext();
        Map byAction = index(servletContext);
        String actionName = sourceActionName(context);
        List entries = (List) byAction.get(actionName);
        if (entries == null || entries.isEmpty()) return Collections.EMPTY_LIST;
        List result = new ArrayList();
        Set keys = new HashSet();
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = (Entry) entries.get(i);
            if (!NewUiRouteRegistry.isSafeLegacyUrl(entry.route)) continue;
            if (entry.handlers.isEmpty()) {
                add(result, keys, context, entry, "module_complete", "Panel modul lengkap", "New UI module");
            } else {
                Iterator handlers = entry.handlers.iterator();
                while (handlers.hasNext()) {
                    String handler = String.valueOf(handlers.next());
                    add(result, keys, context, entry, handler, humanize(handler), handler);
                }
            }
        }
        return result;
    }

    private String sourceActionName(GenericCrudRequestContext context) {
        Object value = context.getRequest().getAttribute("nuiServiceSourceClass");
        if (value == null) value = context.getRequest().getAttribute("nuiSourceClass");
        String name = value == null ? "" : String.valueOf(value).trim();
        if (name.length() > 0 && !"null".equalsIgnoreCase(name)) {
            int dot = name.lastIndexOf('.');
            return dot < 0 ? name : name.substring(dot + 1);
        }
        return context.getDefinition().getEntityClass().getSimpleName() + "Action";
    }

    private void add(List result, Set keys, GenericCrudRequestContext context, Entry entry,
            String keySuffix, String label, String sourceHandler) {
        String key = "native_" + keySuffix.replaceAll("[^A-Za-z0-9_]", "_").toLowerCase();
        if (!keys.add(key)) return;
        Map value = new LinkedHashMap();
        value.put("actionKey", key);
        value.put("label", label);
        value.put("group", "Panel New UI");
        value.put("requiredPrivilege", "READ");
        value.put("selectionMode", "NONE");
        value.put("implementationStatus", "NEW_UI_NATIVE_PANEL");
        value.put("nativePanelKey", key);
        value.put("sourceAction", entry.actionClass);
        value.put("sourceHandler", sourceHandler);
        value.put("enabled", Boolean.TRUE);
        result.add(value);
    }

    private Map index(ServletContext servletContext) {
        if (servletContext == null) return Collections.EMPTY_MAP;
        if (cachedContext == servletContext) return cachedByAction;
        synchronized (LOCK) {
            if (cachedContext == servletContext) return cachedByAction;
            Map result = new HashMap();
            scan(servletContext, ZUL_ROOT, result, new HashSet());
            cachedByAction = result;
            cachedContext = servletContext;
            return result;
        }
    }

    private void scan(ServletContext context, String path, Map result, Set visited) {
        if (!visited.add(path)) return;
        Set resources = context.getResourcePaths(path);
        if (resources == null) return;
        Iterator iterator = resources.iterator();
        while (iterator.hasNext()) {
            String resource = String.valueOf(iterator.next());
            if (resource.endsWith("/")) scan(context, resource, result, visited);
            else if (resource.toLowerCase().endsWith(".zul")) parse(context, resource, result);
        }
    }

    private void parse(ServletContext context, String resource, Map result) {
        InputStream input = null;
        try {
            input = context.getResourceAsStream(resource);
            if (input == null) return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            StringBuilder source = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) source.append(line).append('\n');
            Matcher apply = APPLY.matcher(source);
            if (!apply.find()) return;
            LinkedHashSet handlers = new LinkedHashSet();
            Matcher forward = FORWARD.matcher(source);
            while (forward.find()) handlers.add(forward.group(1));
            Matcher direct = DIRECT_EVENT.matcher(source);
            while (direct.find()) handlers.add(direct.group(1));
            String route = routeFromResourcePath(resource);
            if (route == null) return;
            String[] classes = apply.group(1).split(",");
            for (int i = 0; i < classes.length; i++) {
                String actionClass = classes[i].trim();
                if (actionClass.length() == 0) continue;
                String simpleName = actionClass.substring(actionClass.lastIndexOf('.') + 1);
                List entries = (List) result.get(simpleName);
                if (entries == null) { entries = new ArrayList(); result.put(simpleName, entries); }
                entries.add(new Entry(actionClass, resource, route, handlers));
            }
        } catch (Exception error) {
            ais.common.ErrorAuditUtil.record(error, "GenericCrudZkossParityService.parse " + resource);
        } finally {
            try { if (input != null) input.close(); } catch (Exception ignored) { }
        }
    }

    static String routeFromResourcePath(String resource) {
        if (resource == null || !resource.startsWith(ZUL_ROOT) || !resource.endsWith(".zul")) return null;
        String route = "/" + resource.substring(ZUL_ROOT.length());
        return NewUiRouteRegistry.isSafeLegacyUrl(route) ? route : null;
    }

    private static String humanize(String handler) {
        String value = handler == null ? "" : handler.replaceFirst("^on", "");
        value = value.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ').trim();
        return value.length() == 0 ? "Buka fungsi" : value;
    }

    private static final class Entry {
        final String actionClass; final String resourcePath; final String route; final Set handlers;
        Entry(String actionClass, String resourcePath, String route, Set handlers) {
            this.actionClass = actionClass; this.resourcePath = resourcePath; this.route = route;
            this.handlers = new LinkedHashSet(handlers);
        }
    }
}
