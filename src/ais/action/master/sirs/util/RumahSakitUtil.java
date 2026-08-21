package ais.action.master.sirs.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.RumahSakit;

/** Resolusi tenant fasilitas kesehatan berdasarkan domain request publik. */
public final class RumahSakitUtil {
    private static final long CACHE_MILLIS = 60000L;
    private static final Object CACHE_LOCK = new Object();
    private static volatile Map<String, RumahSakit> byDomain = Collections.emptyMap();
    private static volatile long loadedAt;

    private RumahSakitUtil() { }

    public static RumahSakit getRumahSakit(HttpServletRequest request) {
        if (request == null) return null;
        Object cached = request.getSession().getAttribute("rumahSakit_data");
        if (cached instanceof RumahSakit && ((RumahSakit) cached).getId() != null) {
            return (RumahSakit) cached;
        }
        refreshIfNeeded();
        RumahSakit result = findByDomain(request.getServerName());
        if (result != null && result.getId() != null) {
            request.getSession().setAttribute("rumahSakit_data", result);
        }
        return result;
    }

    public static void clearCache() {
        synchronized (CACHE_LOCK) {
            byDomain = Collections.emptyMap();
            loadedAt = 0L;
        }
    }

    private static RumahSakit findByDomain(String serverName) {
        String host = normalize(serverName);
        if (host.length() == 0) return null;
        RumahSakit exact = byDomain.get(host);
        if (exact != null) return exact;
        for (Map.Entry<String, RumahSakit> entry : byDomain.entrySet()) {
            String domain = entry.getKey();
            if (host.startsWith(domain + ".") || host.contains(domain)) return entry.getValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void refreshIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - loadedAt < CACHE_MILLIS) return;
        synchronized (CACHE_LOCK) {
            if (now - loadedAt < CACHE_MILLIS) return;
            Session session = null;
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                List<RumahSakit> rows = session.createCriteria(RumahSakit.class)
                        .add(Restrictions.eq("aktif", Boolean.TRUE)).list();
                Map<String, RumahSakit> replacement = new LinkedHashMap<String, RumahSakit>();
                for (RumahSakit item : rows) {
                    if (item == null || item.getId() == null) continue;
                    for (String raw : item.getDomain().split("[,;\\s]+")) {
                        String domain = normalize(raw);
                        if (domain.length() > 0) replacement.put(domain, item);
                    }
                }
                byDomain = Collections.unmodifiableMap(replacement);
            } catch (Exception e) {
                ais.common.ErrorAuditUtil.record(e, "RumahSakitUtil.refresh");
            } finally {
                loadedAt = System.currentTimeMillis();
                if (session != null && session.isOpen()) try { session.close(); } catch (Exception ignored) { }
            }
        }
    }

    public static String getRumahSakitMedia(HttpServletRequest request, String jenis, RumahSakit rumahSakit) {
        String fallback = mediaRoot(request) + (jenis != null && jenis.contains("background")
                ? "/img/main.jpg" : "/img/logo.png");
        if (rumahSakit == null || rumahSakit.getId() == null || jenis == null || jenis.trim().length() == 0) {
            return fallback;
        }
        String cacheKey = "media_rumah_sakit_" + jenis + rumahSakit.getId();
        Object cached = request == null ? null : request.getSession().getAttribute(cacheKey);
        if (cached instanceof String && ((String) cached).trim().length() > 0) return (String) cached;
        String extension = jenis.contains("background") || jenis.contains("banner") ? ".jpg" : ".png";
        String result = mediaRoot(request) + "/img/" + jenis + rumahSakit.getId() + extension;
        if (request != null) request.getSession().setAttribute(cacheKey, result);
        return result;
    }

    private static String mediaRoot(HttpServletRequest request) {
        if (request == null) return Common.ROOT == null ? "" : Common.ROOT;
        String scheme = Common.isSecure(request) ? "https" : "http";
        int port = request.getServerPort();
        return scheme + "://" + request.getServerName()
                + (port == 80 || port == 443 ? "" : ":" + port) + request.getContextPath();
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String result = value.trim().toLowerCase(Locale.ENGLISH);
        int scheme = result.indexOf("://");
        if (scheme >= 0) result = result.substring(scheme + 3);
        int slash = result.indexOf('/');
        if (slash >= 0) result = result.substring(0, slash);
        int colon = result.indexOf(':');
        if (colon >= 0) result = result.substring(0, colon);
        return result.startsWith("www.") ? result.substring(4) : result;
    }
}
