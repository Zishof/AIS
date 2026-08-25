package ais.action.master.library.modern;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONArray;
import org.json.JSONObject;

/** In-process operational counters without recording query text, user identity, or other sensitive payloads. */
public final class LibraryTelemetry {
    private static final int MAX_ROUTES = 128;
    private static final Map<String, Metric> METRICS = new ConcurrentHashMap<String, Metric>();
    private LibraryTelemetry() { }

    public static void record(String route, int status, long durationMillis) {
        route = normalize(route);
        if (!METRICS.containsKey(route) && METRICS.size() >= MAX_ROUTES) route = "other";
        Metric created = new Metric();
        Metric metric = METRICS.putIfAbsent(route, created);
        if (metric == null) metric = created;
        metric.requests.incrementAndGet();
        if (status >= 400) metric.errors.incrementAndGet();
        metric.duration.addAndGet(Math.max(0L, durationMillis));
        updateMax(metric.maximum, Math.max(0L, durationMillis));
    }

    public static JSONObject snapshot() throws Exception {
        JSONArray routes = new JSONArray();
        long requests = 0L, errors = 0L;
        for (Map.Entry<String, Metric> entry : METRICS.entrySet()) {
            Metric metric = entry.getValue();
            long count = metric.requests.get(), failed = metric.errors.get(), duration = metric.duration.get();
            requests += count; errors += failed;
            routes.put(new JSONObject().put("route", entry.getKey()).put("requests", count).put("errors", failed)
                    .put("averageMs", count == 0L ? 0L : duration / count).put("maximumMs", metric.maximum.get()));
        }
        return new JSONObject().put("status", "UP").put("startedAt", Started.AT).put("requests", requests)
                .put("errors", errors).put("routes", routes).put("scope", "per-node");
    }

    private static String normalize(String route) {
        if (route == null || route.trim().length() == 0) return "unknown";
        route = route.trim().replaceAll("[^A-Za-z0-9_/-]", "_");
        return route.length() > 80 ? route.substring(0, 80) : route;
    }

    private static void updateMax(AtomicLong target, long value) {
        long current;
        do { current = target.get(); if (value <= current) return; } while (!target.compareAndSet(current, value));
    }

    private static final class Metric {
        private final AtomicLong requests = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();
        private final AtomicLong duration = new AtomicLong();
        private final AtomicLong maximum = new AtomicLong();
    }
    private static final class Started { private static final long AT = System.currentTimeMillis(); }
}
