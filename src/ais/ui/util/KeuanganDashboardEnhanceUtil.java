package ais.ui.util;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Html;

/**
 * Util ringan untuk dashboard keuangan/pembayaran.
 * Kompatibel Java 1.6/1.7 dan ZK 5.5.
 */
public final class KeuanganDashboardEnhanceUtil {

    private KeuanganDashboardEnhanceUtil() {
    }

    public static void closeOpenedSession(Session session) {
        if (session == null) {
            return;
        }
        try {
            session.clear();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/KeuanganDashboardEnhanceUtil.java:23");
        }
        try {
            session.disconnect();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/KeuanganDashboardEnhanceUtil.java:27");
        }
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/KeuanganDashboardEnhanceUtil.java:33");
        }
    }

    public static void appendProgressHtml(Component parent, String title, String description, int percent) {
        if (parent == null) {
            return;
        }
        new Html(buildProgressHtml(title, description, percent)).setParent(parent);
    }

    public static String buildProgressHtml(String title, String description, int percent) {
        int p = safePercent(percent);
        String t = html(title == null ? "Memuat Data" : title);
        String d = html(description == null ? "Data sedang disiapkan." : description);
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Arial,sans-serif;margin:10px 0;padding:16px;border-radius:18px;")
          .append("background:linear-gradient(135deg,#eff6ff 0%,#ffffff 58%,#ecfeff 100%);border:1px solid #dbeafe;")
          .append("box-shadow:0 12px 28px rgba(37,99,235,.10);box-sizing:border-box;'>")
          .append("<div style='display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;'>")
          .append("<div style='min-width:210px;'><div style='font-size:15px;font-weight:900;color:#0f172a;display:flex;align-items:center;gap:8px;'>")
          .append(ais.common.CommonDashboardHtmlHelper.spinnerSvg(16, "#2563eb")).append("<span>")
          .append(t).append("</span></div><div style='font-size:12px;color:#475569;margin-top:5px;line-height:1.45;'>")
          .append(d).append("</div></div>")
          .append("<div style='font-size:25px;font-weight:900;color:#2563eb;'>").append(p).append("%</div>")
          .append("</div><div style='height:13px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:13px;'>")
          .append("<div style='height:13px;width:").append(p).append("%;border-radius:999px;background:linear-gradient(90deg,#2563eb,#06b6d4,#22c55e);transition:width .35s ease;'></div>")
          .append("</div><div style='font-size:11px;color:#64748b;margin-top:8px;'>Tampilan akan diperbarui otomatis setelah proses selesai.</div>")
          .append("</div>");
        return sb.toString();
    }

    public static void showFloatingProgress(String key, String title, String description, int percent) {
        try {
            String id = "ecampus_keu_progress_" + sanitizeKey(key);
            String html = buildFloatingProgressHtml(id, title, description, percent);
            StringBuilder js = new StringBuilder();
            js.append("(function(){try{")
              .append("var id='").append(js(id)).append("';")
              .append("var el=document.getElementById(id);")
              .append("if(!el){el=document.createElement('div');el.id=id;document.body.appendChild(el);}")
              .append("el.innerHTML='").append(js(html)).append("';")
              .append("el.style.display='block';")
              .append("}catch(e){}})();");
            Clients.evalJavaScript(js.toString());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/KeuanganDashboardEnhanceUtil.java:78");
        }
    }

    public static void updateFloatingProgress(String key, int percent, String statusLabel) {
        try {
            String id = "ecampus_keu_progress_" + sanitizeKey(key);
            int p = safePercent(percent);
            String lbl = statusLabel == null ? "" : statusLabel;
            StringBuilder js = new StringBuilder();
            js.append("(function(){try{")
              .append("var el=document.getElementById('").append(js(id)).append("');")
              .append("if(el){")
              .append("var bar=el.querySelector('[data-keu-bar]');if(bar){bar.style.width='").append(p).append("%';}")
              .append("var pct=el.querySelector('[data-keu-pct]');if(pct){pct.textContent='").append(p).append("%';}")
              .append("var lbl=el.querySelector('[data-keu-lbl]');if(lbl){lbl.textContent='").append(js(lbl)).append("';}")
              .append("}")
              .append("}catch(e){}})();");
            Clients.evalJavaScript(js.toString());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/KeuanganDashboardEnhanceUtil.java:97");
        }
    }

    public static void hideFloatingProgress(String key) {
        try {
            final String id = "ecampus_keu_progress_" + sanitizeKey(key);
            Clients.evalJavaScript("setTimeout(function(){try{var el=document.getElementById('" + js(id)
                    + "');if(el){el.style.opacity='0';setTimeout(function(){try{if(el&&el.parentNode){el.parentNode.removeChild(el);}}catch(e){}},320);}}catch(e){}},250);");
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/KeuanganDashboardEnhanceUtil.java:106");
        }
    }

    public static String buildDescriptionBox(String title, String description) {
        return "<div style='font-family:Arial,sans-serif;padding:11px 13px;margin-bottom:10px;border-radius:14px;"
                + "background:#f8fafc;border:1px solid #e2e8f0;color:#334155;line-height:1.45;'>"
                + "<div style='font-size:13px;font-weight:900;color:#0f172a;margin-bottom:4px;'>" + html(title) + "</div>"
                + "<div style='font-size:12px;'>" + html(description) + "</div></div>";
    }

    private static String buildFloatingProgressHtml(String id, String title, String description, int percent) {
        int p = safePercent(percent);
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"position:fixed;right:22px;bottom:22px;z-index:99999;width:360px;max-width:calc(100vw - 44px);")
          .append("font-family:Arial,sans-serif;background:#ffffff;border:1px solid #bfdbfe;border-radius:20px;overflow:hidden;")
          .append("box-shadow:0 22px 48px rgba(15,23,42,.24);transition:opacity .28s ease;\">")
          .append("<div style=\"padding:15px 17px;background:linear-gradient(135deg,#1d4ed8,#0891b2);color:#fff;\">")
          .append("<div style=\"font-size:14px;font-weight:900;display:flex;align-items:center;gap:8px;\">"
                  + ais.common.CommonDashboardHtmlHelper.spinnerSvg(16, "#ffffff")
                  + "<span>" + html(title == null ? "Memuat Data" : title) + "</span></div>")
          .append("<div style=\"font-size:11px;opacity:.92;margin-top:5px;line-height:1.45;\">" + html(description == null ? "Data sedang diproses." : description) + "</div>")
          .append("</div><div style=\"padding:14px 17px;\">")
          .append("<div style=\"display:flex;align-items:center;justify-content:space-between;margin-bottom:7px;color:#334155;font-size:12px;font-weight:800;\"><span>Progress</span><span data-keu-pct>")
          .append(p).append("%</span></div>")
          .append("<div style=\"height:13px;border-radius:999px;background:#e2e8f0;overflow:hidden;\"><div data-keu-bar style=\"height:13px;width:")
          .append(p).append("%;border-radius:999px;background:linear-gradient(90deg,#2563eb,#06b6d4,#22c55e);transition:width .35s ease;\"></div></div>")
          .append("<div data-keu-lbl style=\"font-size:11px;color:#64748b;margin-top:9px;\">Mohon tunggu, halaman sedang menyiapkan data terbaru.</div>")
          .append("</div></div>");
        return sb.toString();
    }

    private static int safePercent(int percent) {
        if (percent < 0) {
            return 0;
        }
        if (percent > 100) {
            return 100;
        }
        return percent;
    }

    private static String sanitizeKey(String key) {
        if (key == null || key.trim().length() == 0) {
            return "default";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? "default" : sb.toString();
    }

    public static String html(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String js(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "").replace("\n", "");
    }
}
