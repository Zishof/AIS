
package ais.common.newui.menu;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

/**
 * Memetakan identitas route lama ke JSP native di WEB-INF/new.
 *
 * <p>URL lama tidak pernah menjadi target render. Nama file/class hanya dipakai
 * sebagai kunci pencarian terhadap JSP New UI yang benar-benar tersedia.</p>
 */
public final class NewUiNativeJspResolver {

    private static final String CACHE_KEY = NewUiNativeJspResolver.class.getName() + ".paths";
    private static final String ROOT = "/WEB-INF/new/";
    private static final Pattern COMPOSER = Pattern.compile("(?:apply|use)\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE);

    public static final class Result {
        private final String target;
        private final String module;
        private final String page;

        Result(String target, String module, String page) {
            this.target = target; this.module = module; this.page = page;
        }
        public String getTarget() { return target; }
        public String getModule() { return module; }
        public String getPage() { return page; }
    }

    private NewUiNativeJspResolver() { }

    public static Result resolve(ServletContext context, String existingRoute, boolean service) {
        if (context == null) return null;
        Set<String> paths = cachedUiPaths(context);
        Result result = resolveFromPaths(existingRoute, service, paths);
        if (result == null) {
            String composer = composerRoute(context, existingRoute);
            if (composer.length() > 0) result = resolveFromPaths(composer, service, paths);
        }
        if (result == null) return null;
        try {
            URL found = context.getResource(result.getTarget());
            return found == null ? null : result;
        } catch (Exception e) { return null; }
    }

    static Result resolveFromPaths(String existingRoute, boolean service, Set<String> uiPaths) {
        String key = routeKey(existingRoute);
        if (key.length() == 0 || uiPaths == null || uiPaths.isEmpty()) return null;
        List<String> candidates = new ArrayList<String>();
        for (String path : uiPaths) {
            if (path == null || !path.startsWith(ROOT) || path.indexOf("/uiux/") < 0
                    || !path.endsWith(".jsp") || path.endsWith("/index.jsp")
                    || path.startsWith(ROOT + "_shared/")) continue;
            String file = path.substring(path.lastIndexOf('/') + 1, path.length() - 4);
            if (key.equals(compact(file)) || key.equals(compact(stripSuffix(file, "action")))) candidates.add(path);
        }
        if (candidates.isEmpty()) return null;
        Collections.sort(candidates);
        String normalizedRoute = compact(existingRoute);
        String best = null; int bestScore = Integer.MIN_VALUE; boolean tied = false;
        for (int i = 0; i < candidates.size(); i++) {
            String path = candidates.get(i); int score = score(path, existingRoute, normalizedRoute);
            if (score > bestScore) { best = path; bestScore = score; tied = false; }
            else if (score == bestScore && best != null && !sameLogicalPage(best, path)) tied = true;
        }
        if (best == null || tied) return null;
        int moduleStart = ROOT.length(); int moduleEnd = best.indexOf('/', moduleStart);
        int uiux = best.indexOf("/uiux/");
        if (moduleEnd < 0 || uiux < 0) return null;
        String module = best.substring(moduleStart, moduleEnd);
        String page = best.substring(uiux + 6, best.length() - 4);
        String target = best;
        if (service) target = ROOT + module + "/services/" + page + "_service.jsp";
        return new Result(target, module, page);
    }

    private static int score(String path, String route, String normalizedRoute) {
        int value = 10000;
        String pathCompact = compact(path);
        String[] tokens = route == null ? new String[0] : route.toLowerCase().replace('\\', '/').split("[/.$_-]+");
        for (int i = 0; i < tokens.length; i++) {
            String token = compact(tokens[i]);
            if (token.length() > 2 && pathCompact.indexOf(token) >= 0) value += 120;
        }
        if (normalizedRoute.indexOf("master") >= 0 && path.startsWith(ROOT + "root/")) value += 80;
        if (path.indexOf("/helper/") >= 0 && normalizedRoute.indexOf("helper") < 0) value -= 20;
        return value;
    }

    private static boolean sameLogicalPage(String left, String right) {
        if (left == null || right == null) return false;
        return compact(left).equals(compact(right));
    }

    private static String routeKey(String route) {
        if (route == null) return "";
        String value = route.trim().replace('\\', '/');
        int query = value.indexOf('?'); if (query >= 0) value = value.substring(0, query);
        int hash = value.indexOf('#'); if (hash >= 0) value = value.substring(0, hash);
        String[] parts = value.split("[/.$]+");
        String base = parts.length == 0 ? value : parts[parts.length - 1];
        if ("zul".equalsIgnoreCase(base) || "jsp".equalsIgnoreCase(base) || "class".equalsIgnoreCase(base)) {
            base = parts.length > 1 ? parts[parts.length - 2] : "";
        }
        if (("list".equalsIgnoreCase(base) || "index".equalsIgnoreCase(base)) && parts.length > 2) base = parts[parts.length - 3];
        base = stripSuffix(base, "action");
        return compact(base);
    }

    private static String stripSuffix(String value, String suffix) {
        if (value == null) return "";
        String compactValue = compact(value), compactSuffix = compact(suffix);
        if (compactValue.endsWith(compactSuffix) && compactValue.length() > compactSuffix.length()) {
            return compactValue.substring(0, compactValue.length() - compactSuffix.length());
        }
        return value;
    }

    private static String compact(String value) {
        if (value == null) return "";
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "");
    }

    private static String composerRoute(ServletContext context, String existingRoute) {
        if (context == null || existingRoute == null) return "";
        String route = existingRoute.trim().replace('\\', '/');
        int query = route.indexOf('?'); if (query >= 0) route = route.substring(0, query);
        if (!route.startsWith("/") || route.indexOf("..") >= 0 || !route.toLowerCase().endsWith(".zul")) return "";
        InputStream stream = null; BufferedReader reader = null;
        try {
            stream = context.getResourceAsStream("/WEB-INF/z/x/y" + route);
            if (stream == null) return "";
            reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder source = new StringBuilder(); String line; int lines = 0;
            while ((line = reader.readLine()) != null && lines++ < 160) source.append(line).append('\n');
            Matcher matcher = COMPOSER.matcher(source.toString());
            while (matcher.find()) {
                String className = matcher.group(1);
                if (className == null || className.indexOf(".action.") < 0) continue;
                int dot = className.lastIndexOf('.');
                String simple = dot < 0 ? className : className.substring(dot + 1);
                if (simple.endsWith("Action") && simple.length() > 6) simple = simple.substring(0, simple.length() - 6);
                return simple;
            }
        } catch (Exception ignored) {
            return "";
        } finally {
            try { if (reader != null) reader.close(); else if (stream != null) stream.close(); } catch (Exception ignored) { }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private static Set<String> cachedUiPaths(ServletContext context) {
        Object cached = context.getAttribute(CACHE_KEY);
        if (cached instanceof Set) return (Set<String>) cached;
        synchronized (context) {
            cached = context.getAttribute(CACHE_KEY);
            if (cached instanceof Set) return (Set<String>) cached;
            Set<String> result = new HashSet<String>();
            collect(context, ROOT, result);
            context.setAttribute(CACHE_KEY, result);
            return result;
        }
    }

    private static void collect(ServletContext context, String directory, Set<String> result) {
        Set<String> children;
        try { children = context.getResourcePaths(directory); }
        catch (Exception e) { return; }
        if (children == null) return;
        for (String child : children) {
            if (child == null || child.indexOf("/WEB-INF/new/_shared/") == 0) continue;
            if (child.endsWith("/")) collect(context, child, result);
            else if (child.indexOf("/uiux/") >= 0 && child.endsWith(".jsp")) result.add(child);
        }
    }
}
