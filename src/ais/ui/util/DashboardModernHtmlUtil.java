package ais.ui.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.zkoss.zul.Html;

import ais.action.master.dashboard.admin.KartuRingkasanKit;
import ais.common.Common;
import ais.common.CommonDashboardHtmlHelper;

/**
 * Utilitas dashboard modern berbasis HTML/CSS.
 * Dibuat agar tampilan dasbor memakai HTML/CSS ringan,
 * konsisten, dan mudah dirawat tanpa mengubah logika pengambilan data.
 */
public final class DashboardModernHtmlUtil {

    private DashboardModernHtmlUtil() {
    }

    /** Penghasil id popup unik per render (agar banyak grafik dalam satu halaman tidak bentrok). */
    private static final java.util.concurrent.atomic.AtomicInteger SEQ = new java.util.concurrent.atomic.AtomicInteger();

    /**
     * Bangun BARIS KARTU RINGKASAN modern &amp; BISA DIKLIK (gaya dashboard e-Learning, mengikuti tema
     * institusi aktif) dari pasangan {label &rarr; nilai} sebuah grafik. Menampilkan tiga kartu &mdash;
     * Total keseluruhan, Kelompok terbanyak, dan Jumlah kelompok &mdash; yang semuanya dapat diklik
     * untuk membuka popup berisi rincian SELURUH kelompok beserta persentasenya. Dipakai oleh semua
     * dasbor/statistik lewat {@link #buildPieChartHtml} dan {@link #buildCategoryChartHtml}, sehingga
     * satu peningkatan di sini otomatis berlaku ke seluruh dasbor &mdash; tanpa mengubah cara
     * pengambilan datanya. Warna kartu memakai penanda {@link KartuRingkasanKit#TEMA} agar senada
     * dengan tema yang dipilih pengguna; popup tidak meminta data ke server (sudah disiapkan saat
     * render).
     *
     * @param parts pasangan kelompok &rarr; nilai (urutan dipertahankan).
     * @param title judul ringkasan (mis. nama dasbor).
     * @param desc  satu kalimat penjelas ramah orang awam (boleh {@code null}).
     * @return potongan HTML kartu ringkasan + popup; string kosong bila data kosong.
     */
    public static String ringkasanKlikable(java.util.LinkedHashMap<String, Double> parts, String title, String desc) {
        if (parts == null || parts.isEmpty()) {
            return "";
        }
        double total = 0;
        String topLabel = "";
        double topVal = -1;
        for (Map.Entry<String, Double> e : parts.entrySet()) {
            double v = e.getValue() == null ? 0 : e.getValue().doubleValue();
            total += v;
            if (v > topVal) {
                topVal = v;
                topLabel = String.valueOf(e.getKey());
            }
        }
        String modalId = "dmh_modal_" + SEQ.incrementAndGet();
        if (isBlank(desc)) {
            desc = "Ringkasan angka penting dari grafik di bawah. Klik kartu untuk melihat rincian tiap kelompok.";
        }
        StringBuilder sb = new StringBuilder(1536);
        sb.append(KartuRingkasanKit.bukaPanel(title, desc));
        sb.append(KartuRingkasanKit.klikable(KartuRingkasanKit.kartuTotal("Total Keseluruhan", KartuRingkasanKit.TEMA,
                (int) Math.round(total), "Jumlah seluruh data pada grafik ini."), modalId));
        sb.append(KartuRingkasanKit.klikable(KartuRingkasanKit.kartuTotal("Kelompok Terbanyak", KartuRingkasanKit.TEMA,
                (int) Math.round(topVal < 0 ? 0 : topVal),
                topLabel + " — kelompok dengan jumlah paling besar."), modalId));
        sb.append(KartuRingkasanKit.klikable(KartuRingkasanKit.kartuTotal("Jumlah Kelompok",
                KartuRingkasanKit.TEMA_AKSEN, parts.size(), "Banyaknya kelompok yang dibandingkan pada grafik."),
                modalId));
        sb.append(KartuRingkasanKit.tutupGrid());
        // Popup: rincian seluruh kelompok + persentase.
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, Double> e : parts.entrySet()) {
            double v = e.getValue() == null ? 0 : e.getValue().doubleValue();
            int pct = total <= 0 ? 0 : (int) Math.round(v * 100.0 / total);
            body.append(KartuRingkasanKit.barisRincian(String.valueOf(e.getKey()), number(v) + " (" + pct + "%)"));
        }
        body.append(KartuRingkasanKit.barisRincian("Total", number(total)));
        sb.append(KartuRingkasanKit.modal(modalId, "Rincian " + title, body.toString()));
        sb.append(KartuRingkasanKit.tutupPanel());
        return sb.toString();
    }

	public static void closeOpenedSession(Session session) {
	    if (session == null) {
	        return;
	    }
	    try {
	        if (session.isOpen()) {
	            try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/DashboardModernHtmlUtil.java:92");}
	            try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/DashboardModernHtmlUtil.java:93");}
	            session.close();
	        }
	    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/DashboardModernHtmlUtil.java:98");}
	}

    public static Html createCategoryChart(Object model, String title, String description, boolean horizontal) {
        return new Html(buildCategoryChartHtml(model, title, description, horizontal));
    }

    public static Html createPieChart(Object model, String title, String description) {
        return new Html(buildPieChartHtml(model, title, description));
    }

    public static Html createAnyChart(Object model, String title, String description, String type) {
        if (type != null && type.toLowerCase().indexOf("pie") >= 0) {
            return createPieChart(model, title, description);
        }
        return createCategoryChart(model, title, description, true);
    }

    public static String description(String text) {
        if (isBlank(text)) {
            text = "Ringkasan ini membantu membaca data penting secara cepat tanpa menghitung ulang dari tabel.";
        }
        return CommonDashboardHtmlHelper.descriptionBlock(text);
    }

    public static String card(String title, String value, String note) {
		title = ais.common.Common.getBahasaConfig(title);
		note = ais.common.Common.getBahasaConfig(note);
        return CommonDashboardHtmlHelper.metricCard(title, value, note);
    }

    public static String progress(String label, double value, double max, String color) {
        int percent = percent(value, max);
        if (isBlank(color)) {
            color = "#2563eb";
        }
        return "<div style='margin:8px 0;'>"
                + "<div style='display:flex;justify-content:space-between;gap:8px;font-size:11px;color:#334155;font-weight:700;'>"
                + "<span>" + html(label) + "</span><span>" + percent + "%</span></div>"
                + "<div style='height:12px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:4px;'>"
                + "<div style='height:12px;width:" + percent + "%;background:" + color + ";border-radius:999px;'></div></div></div>";
    }

    public static String buildCategoryChartHtml(Object model, String title, String description, boolean horizontal) {
        List series = toList(callNoArg(model, "getSeries"));
        List categories = toList(callNoArg(model, "getCategories"));
        double max = 0.0;
        for (int i = 0; i < series.size(); i++) {
            Object s = series.get(i);
            for (int j = 0; j < categories.size(); j++) {
                Object c = categories.get(j);
                double v = doubleValue(callValue(model, s, c));
                if (v > max) {
                    max = v;
                }
            }
        }
        if (max <= 0.0) {
            max = 1.0;
        }
        // Warna seri pertama mengikuti tema institusi; sisanya warna pembeda tetap.
        String[] colors = new String[] { "var(--ais-theme-primary,#2563eb)", "#16a34a", "#f97316", "#9333ea",
                "#dc2626", "#0891b2", "#f59e0b" };
        // Kartu ringkasan modern (tema + bisa diklik) berdasar jumlah per kategori (seluruh seri).
        java.util.LinkedHashMap<String, Double> parts = new java.util.LinkedHashMap<String, Double>();
        for (int j = 0; j < categories.size(); j++) {
            Object c = categories.get(j);
            double tot = 0;
            for (int i = 0; i < series.size(); i++) {
                tot += doubleValue(callValue(model, series.get(i), c));
            }
            parts.put(String.valueOf(c), Double.valueOf(tot));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(ringkasanKlikable(parts, title, description));
        sb.append("<div style='font-family:Arial,sans-serif;background:#ffffff;border:1px solid #e2e8f0;border-radius:14px;padding:12px;margin:8px 0;box-shadow:0 2px 8px rgba(15,23,42,0.06);'>");
        sb.append("<div style='font-size:13px;color:#334155;font-weight:800;margin-bottom:4px;'>")
                .append(parts.isEmpty() ? html(title) : "Grafik Perbandingan").append("</div>");
        sb.append(description(description));
        if (series.isEmpty() || categories.isEmpty()) {
            sb.append("<div style='padding:12px;color:#64748b;background:#f8fafc;border:1px dashed #cbd5e1;border-radius:10px;'>Belum ada data yang bisa divisualisasikan.</div>");
        } else {
            sb.append("<div style='display:flex;gap:10px;flex-wrap:wrap;font-size:11px;color:#475569;margin-bottom:10px;'>");
            for (int i = 0; i < series.size(); i++) {
                sb.append("<span><b style='display:inline-block;width:10px;height:10px;border-radius:3px;background:")
                        .append(colors[i % colors.length]).append(";'></b> ").append(html(String.valueOf(series.get(i)))).append("</span>");
            }
            sb.append("</div>");
            for (int j = 0; j < categories.size(); j++) {
                Object c = categories.get(j);
                sb.append("<div style='margin:9px 0;padding-bottom:7px;border-bottom:1px dashed #e2e8f0;'>");
                sb.append("<div style='font-size:11px;color:#334155;font-weight:700;margin-bottom:4px;'>").append(html(String.valueOf(c))).append("</div>");
                for (int i = 0; i < series.size(); i++) {
                    Object s = series.get(i);
                    double v = doubleValue(callValue(model, s, c));
                    int w = percent(v, max);
                    sb.append("<div style='display:flex;align-items:center;gap:6px;margin:3px 0;'>")
                            .append("<span style='width:110px;max-width:110px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:10px;color:#64748b;'>")
                            .append(html(String.valueOf(s))).append("</span>")
                            .append("<div style='flex:1;height:11px;background:#e2e8f0;border-radius:999px;overflow:hidden;'>")
                            .append("<div style='height:11px;width:").append(w).append("%;background:").append(colors[i % colors.length])
                            .append(";border-radius:999px;'></div></div>")
                            .append("<span style='min-width:70px;text-align:right;font-size:10px;color:#334155;font-weight:700;'>")
                            .append(html(number(v))).append("</span></div>");
                }
                sb.append("</div>");
            }
        }
        sb.append("</div>");
        return sb.toString();
    }

    public static String buildPieChartHtml(Object model, String title, String description) {
        // Ditingkatkan: donat (conic-gradient) + legenda persen via kit reusable, seragam dengan dasbor lain.
        List keys = toList(callFirstNoArg(model, new String[] { "getKeys", "getCategories", "getItems" }));
        java.util.LinkedHashMap<String, Double> parts = new java.util.LinkedHashMap<String, Double>();
        for (int i = 0; i < keys.size(); i++) {
            Object key = keys.get(i);
            parts.put(String.valueOf(key), Double.valueOf(doubleValue(callValue(model, key))));
        }
        // Kartu ringkasan modern (tema + bisa diklik) di atas + donat di bawah (judul cukup di kartu).
        String ringkasan = ringkasanKlikable(parts, title, description);
        String donut = DashboardUiKit.donut(ringkasan.length() > 0 ? "Grafik Perbandingan" : title,
                CommonDashboardHtmlHelper.simpleDescription(description), parts, false,
                "Belum ada data yang bisa ditampilkan.");
        return ringkasan + donut;
    }

    public static String spiderRadar(String title, String description, String[] labels, double[] values) {
        // Ditingkatkan: jaring laba-laba SVG asli via kit reusable (bukan lagi baris progres datar).
        int n = (labels == null || values == null) ? 0 : Math.min(labels.length, values.length);
        String[] lbls = new String[n];
        int[] vals = new int[n];
        for (int i = 0; i < n; i++) {
            lbls[i] = labels[i];
            int v = (int) Math.round(values[i]);
            vals[i] = v < 0 ? 0 : (v > 100 ? 100 : v);
        }
        String chart = DashboardUiKit.spider(title, CommonDashboardHtmlHelper.simpleDescription(description), lbls,
                vals);
        if (n <= 0) {
            return chart;
        }
        String modalId = "dmh_spider_" + SEQ.incrementAndGet();
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < n; i++) {
            body.append(KartuRingkasanKit.barisRincian(lbls[i], vals[i] + "%"));
        }
        // css() disertakan agar popup tetap bergaya walau spider dipakai sendirian tanpa kartu ringkasan.
        return KartuRingkasanKit.css() + KartuRingkasanKit.bungkusKlikable(chart, modalId)
                + KartuRingkasanKit.modal(modalId, "Rincian " + title, body.toString());
    }

    private static Object callNoArg(Object target, String method) {
        if (target == null || method == null) {
            return null;
        }
        try {
            Method m = target.getClass().getMethod(method, new Class[] {});
            return m.invoke(target, new Object[] {});
        } catch (Exception e) {
            return null;
        }
    }

    private static Object callFirstNoArg(Object target, String[] methods) {
        if (methods == null) {
            return null;
        }
        for (int i = 0; i < methods.length; i++) {
            Object value = callNoArg(target, methods[i]);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Object callValue(Object target, Object key) {
        if (target == null) {
            return null;
        }
        try {
            Method[] methods = target.getClass().getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method m = methods[i];
                if ("getValue".equals(m.getName()) && m.getParameterTypes().length == 1) {
                    return m.invoke(target, new Object[] { key });
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/DashboardModernHtmlUtil.java:288");
        }
        return null;
    }

    private static Object callValue(Object target, Object s, Object c) {
        if (target == null) {
            return null;
        }
        try {
            Method[] methods = target.getClass().getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method m = methods[i];
                if ("getValue".equals(m.getName()) && m.getParameterTypes().length == 2) {
                    return m.invoke(target, new Object[] { s, c });
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/DashboardModernHtmlUtil.java:305");
        }
        return null;
    }

    private static List toList(Object object) {
        List list = new ArrayList();
        if (object == null) {
            return list;
        }
        if (object instanceof Collection) {
            list.addAll((Collection) object);
            return list;
        }
        if (object instanceof Object[]) {
            Object[] arr = (Object[]) object;
            for (int i = 0; i < arr.length; i++) {
                list.add(arr[i]);
            }
            return list;
        }
        if (object instanceof Map) {
            list.addAll(((Map) object).keySet());
            return list;
        }
        return list;
    }

    public static int percent(double value, double max) {
        if (max <= 0.0) {
            return 0;
        }
        int result = (int) Math.round(value * 100.0d / max);
        if (result < 0) {
            return 0;
        }
        if (result > 100) {
            return 100;
        }
        return result;
    }

    public static double safeDouble(Double value) {
        return value == null ? 0.0d : value.doubleValue();
    }

    public static double doubleValue(Object value) {
        if (value == null) {
            return 0.0d;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0.0d;
        }
    }

    public static String money(double value) {
        try {
            return "Rp " + Common.numberFormat.get().format(value);
        } catch (Exception e) {
            return "Rp " + value;
        }
    }

    public static String number(double value) {
        try {
            return Common.numberFormat.get().format(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    public static String html(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
