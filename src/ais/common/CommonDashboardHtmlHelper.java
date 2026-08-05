package ais.common;

/**
 * Helper kecil untuk membuat potongan HTML/CSS dashboard tanpa JFreeChart.
 * Kompatibel Java 1.6/1.7 dan bisa dipakai ulang oleh action/dashboard lain.
 */
public class CommonDashboardHtmlHelper {

    private CommonDashboardHtmlHelper() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        String result = value;
        result = result.replace("&", "&amp;");
        result = result.replace("<", "&lt;");
        result = result.replace(">", "&gt;");
        result = result.replace("\"", "&quot;");
        result = result.replace("'", "&#39;");
        return result;
    }

    public static String simpleDescription(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() == 0) {
            return "Ringkasan ini membantu melihat data penting tanpa membuka semua baris.";
        }
        text = replaceIgnoreCase(text, "Panel ini ", "");
        text = replaceIgnoreCase(text, "Dashboard ini ", "");
        text = replaceIgnoreCase(text, "Dasbor ini ", "");
        text = replaceIgnoreCase(text, "Grafik ini ", "");
        text = replaceIgnoreCase(text, "Chart ini ", "");
        text = replaceIgnoreCase(text, "menampilkan", "memperlihatkan");
        return text.trim();
    }

    public static String page(String title, String description, String content) {
        StringBuilder sb = new StringBuilder(2400 + length(content));
        sb.append("<div style='font-family:Arial,Helvetica,sans-serif;color:#0f172a;background:#f8fafc;");
        sb.append("padding:14px;box-sizing:border-box;width:100%;'>");
        sb.append(hero(title, description));
        if (content != null) {
            sb.append(content);
        }
        sb.append("</div>");
        return sb.toString();
    }

    public static String hero(String title, String description) {
        StringBuilder sb = new StringBuilder(1200);
        // Banner mengikuti tema institusi: latar = warna utama tema, teks = warna kontras tema
        // (didefinisikan tiap file tema) agar tetap terbaca pada tema terang maupun gelap.
        sb.append("<div style='background:var(--ais-theme-primary,#0f172a);border-radius:10px;padding:18px;");
        sb.append("color:var(--ais-theme-contrast,#fff);box-shadow:0 12px 28px rgba(15,23,42,.18);margin-bottom:12px;'>");
        sb.append("<div style='font-size:22px;font-weight:900;line-height:1.25;margin-bottom:6px;'>");
        sb.append(escape(title));
        sb.append("</div>");
        sb.append("<div style='font-size:12px;line-height:1.55;color:var(--ais-theme-contrast,#dbeafe);opacity:.9;max-width:980px;'>");
        sb.append(escape(simpleDescription(description)));
        sb.append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    public static String descriptionBlock(String text) {
        return "<div style='font-size:12px;color:#64748b;line-height:1.5;background:#f8fafc;"
                + "border:1px solid #e2e8f0;border-radius:8px;padding:9px;margin:0 0 10px 0;'>"
                + escape(simpleDescription(text)) + "</div>";
    }

    public static String cards(String content) {
        return "<div style='display:flex;flex-wrap:wrap;gap:10px;margin:12px 0;'>" + (content == null ? "" : content)
                + "</div>";
    }

    public static String cards(String[] contents) {
        StringBuilder sb = new StringBuilder(1200);
        if (contents != null) {
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null) {
                    sb.append(contents[i]);
                }
            }
        }
        return cards(sb.toString());
    }

    public static String metricCard(String title, String value, String description) {
        StringBuilder sb = new StringBuilder(900);
        sb.append("<div style='flex:1 1 180px;min-width:180px;background:#fff;border:1px solid #e5e7eb;");
        sb.append("border-radius:8px;padding:14px;box-shadow:0 8px 20px rgba(15,23,42,.06);box-sizing:border-box;'>");
        sb.append("<div style='font-size:11px;color:#64748b;font-weight:800;text-transform:uppercase;'>");
        sb.append(escape(title));
        sb.append("</div>");
        sb.append("<div style='font-size:24px;color:#0f172a;font-weight:900;margin-top:6px;line-height:1.15;'>");
        sb.append(escape(value));
        sb.append("</div>");
        sb.append("<div style='font-size:11px;color:#64748b;line-height:1.45;margin-top:7px;'>");
        sb.append(escape(simpleDescription(description)));
        sb.append("</div></div>");
        return sb.toString();
    }

    public static String panel(String title, String description, String content) {
        StringBuilder sb = new StringBuilder(1200 + length(content));
        sb.append("<div style='background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:15px;");
        sb.append("box-shadow:0 8px 20px rgba(15,23,42,.06);box-sizing:border-box;margin-bottom:12px;'>");
        sb.append("<div style='font-size:15px;font-weight:900;color:#0f172a;line-height:1.3;'>");
        sb.append(escape(title));
        sb.append("</div>");
        sb.append("<div style='font-size:12px;color:#64748b;line-height:1.5;margin-top:4px;'>");
        sb.append(escape(simpleDescription(description)));
        sb.append("</div>");
        sb.append("<div style='margin-top:12px;'>");
        if (content != null) {
            sb.append(content);
        }
        sb.append("</div></div>");
        return sb.toString();
    }

    /**
     * Ikon "sedang memuat" yang berputar — SVG murni + animasi SMIL (tanpa JFreeChart/FontAwesome).
     * Dipakai ulang oleh semua indikator loading dasbor agar pengguna tahu data sedang diambil.
     */
    public static String spinnerSvg(int sizePx, String color) {
        int s = sizePx <= 0 ? 18 : sizePx;
        String c = (color == null || color.trim().length() == 0) ? "#ffffff" : color;
        return "<svg width='" + s + "' height='" + s + "' viewBox='0 0 50 50'"
                + " style='display:inline-block;vertical-align:middle;'>"
                + "<circle cx='25' cy='25' r='20' fill='none' stroke='rgba(148,163,184,.35)' stroke-width='6'/>"
                + "<path fill='none' stroke='" + c + "' stroke-width='6' stroke-linecap='round'"
                + " d='M25 5 a20 20 0 0 1 17.32 10'>"
                + "<animateTransform attributeName='transform' type='rotate' from='0 25 25' to='360 25 25'"
                + " dur='0.8s' repeatCount='indefinite'/></path></svg>";
    }

    public static String progressBar(int percent, String title, String detail) {
        int value = clamp(percent, 0, 100);
        StringBuilder sb = new StringBuilder(1400);
        sb.append("<div style='margin:10px 0;padding:12px 14px;border-radius:8px;background:#0f172a;color:#fff;");
        sb.append("box-shadow:0 10px 24px rgba(15,23,42,.20);font-family:Arial,Helvetica,sans-serif;'>");
        sb.append("<div style='display:flex;align-items:center;justify-content:space-between;gap:10px;margin-bottom:8px;'>");
        sb.append("<div style='font-size:13px;font-weight:900;display:flex;align-items:center;gap:8px;'>")
          .append(spinnerSvg(16, "#38bdf8")).append("<span>").append(escape(nvl(title, "Memuat data"))).append("</span></div>");
        sb.append("<div style='font-size:12px;font-weight:900;color:#bfdbfe;'>").append(value).append("%</div>");
        sb.append("</div>");
        sb.append("<div style='height:10px;border-radius:999px;background:rgba(255,255,255,.18);overflow:hidden;'>");
        sb.append("<div style='height:10px;width:").append(value);
        sb.append("%;border-radius:999px;background:#38bdf8;'></div></div>");
        sb.append("<div style='margin-top:8px;font-size:12px;color:#dbeafe;line-height:1.45;'>");
        sb.append(escape(simpleDescription(detail)));
        sb.append("</div></div>");
        return sb.toString();
    }

    public static String barRow(String label, int value, int max) {
        return barRow(label, (long) value, (long) max);
    }

    public static String barRow(String label, long value, long max) {
        int width = percent(value, max);
        StringBuilder sb = new StringBuilder(900);
        sb.append("<div style='display:flex;align-items:center;gap:8px;margin:7px 0;'>");
        sb.append("<div style='width:130px;font-size:11px;color:#475569;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;'>");
        sb.append(escape(label)).append("</div>");
        sb.append("<div style='flex:1;height:12px;background:#e2e8f0;border-radius:999px;overflow:hidden;'>");
        sb.append("<div style='height:12px;width:").append(width);
        sb.append("%;border-radius:999px;background:var(--ais-theme-primary,#2563eb);'></div></div>");
        sb.append("<div style='width:58px;text-align:right;font-size:11px;color:#0f172a;font-weight:800;'>");
        sb.append(value).append("</div></div>");
        return sb.toString();
    }

    public static String emptyState(String text) {
        return "<div style='padding:14px;color:#64748b;background:#f8fafc;border:1px dashed #cbd5e1;"
                + "border-radius:8px;font-size:12px;line-height:1.5;'>" + escape(simpleDescription(text)) + "</div>";
    }

    public static String errorState(String title, Exception error) {
        String message = error == null ? "" : error.getClass().getName() + ": " + error.getMessage();
        return "<div style='font-family:Arial,Helvetica,sans-serif;margin:14px;padding:16px;border-radius:8px;"
                + "background:#fff1f2;border:1px solid #fecdd3;color:#9f1239;'>"
                + "<div style='font-weight:900;font-size:16px;margin-bottom:6px;'>" + escape(title)
                + " belum dapat ditampilkan.</div>"
                + "<div style='font-size:12px;line-height:1.5;'>Data rinci tetap dapat dibuka melalui tab Data.</div>"
                + "<pre style='white-space:pre-wrap;margin-top:10px;font-size:11px;'>" + escape(message)
                + "</pre></div>";
    }

    public static int percent(double value, double max) {
        if (max <= 0.0) {
            return value > 0.0 ? 100 : 0;
        }
        int p = (int) Math.round((value * 100.0) / max);
        if (p < 1 && value > 0.0) {
            p = 1;
        }
        return clamp(p, 0, 100);
    }

    private static String replaceIgnoreCase(String text, String search, String replacement) {
        if (text == null || search == null || search.length() == 0) {
            return text;
        }
        String lower = text.toLowerCase();
        String lowerSearch = search.toLowerCase();
        int index = lower.indexOf(lowerSearch);
        while (index >= 0) {
            text = text.substring(0, index) + replacement + text.substring(index + search.length());
            lower = text.toLowerCase();
            index = lower.indexOf(lowerSearch, index + replacement.length());
        }
        return text;
    }

    private static String nvl(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static int length(String value) {
        return value == null ? 0 : value.length();
    }
}
