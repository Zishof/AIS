package ais.ui.util;

import org.zkoss.zk.ui.util.Clients;

/**
 * Loading indicator ringan untuk halaman-halaman posting jurnal.
 * Tidak memakai komponen berat dan tidak membutuhkan perubahan ZUL.
 * Kompatibel Java 1.6/1.7.
 */
public class PostingJurnalLoadingUtil {

    private static final String DOM_ID = "postingJurnalLoadingIndicator";

    public static void show(String title, String message, int percent) {
        render(title, message, percent, false);
    }

    public static void update(String title, String message, int percent) {
        render(title, message, percent, false);
    }

    public static void complete(String title, String message, int percent) {
        render(title, message, percent, true);
    }

    private static void render(String title, String message, int percent, boolean autoHide) {
        int p = percent;
        if (p < 0) {
            p = 0;
        }
        if (p > 100) {
            p = 100;
        }
        String safeTitle = escapeJs(title == null ? "Memuat Data Jurnal" : title);
        String safeMessage = escapeJs(message == null ? "Data sedang disiapkan." : message);

        StringBuilder js = new StringBuilder();
        js.append("(function(){try{");
        js.append("var id='").append(DOM_ID).append("';");
        js.append("if(window.__postingJurnalLoadingHideTimer){try{clearTimeout(window.__postingJurnalLoadingHideTimer);}catch(e){}window.__postingJurnalLoadingHideTimer=null;}");
        js.append("var box=document.getElementById(id);");
        js.append("if(!box){");
        js.append("box=document.createElement('div');box.id=id;document.body.appendChild(box);");
        js.append("box.style.position='fixed';box.style.right='18px';box.style.top='18px';box.style.zIndex='999999';");
        js.append("box.style.width='360px';box.style.maxWidth='calc(100% - 36px)';box.style.borderRadius='18px';");
        js.append("box.style.overflow='hidden';box.style.boxShadow='0 18px 42px rgba(15,23,42,.24)';");
        js.append("box.style.border='1px solid rgba(191,219,254,.95)';box.style.background='#ffffff';");
        js.append("box.style.transition='opacity .35s ease, transform .35s ease';box.style.opacity='0';box.style.transform='translateY(-8px)';");
        js.append("}");
        js.append("box.style.display='block';box.style.opacity='1';box.style.transform='translateY(0)';");
        js.append("box.innerHTML=");
        js.append("'<div style=\"padding:15px 16px;background:linear-gradient(135deg,#eff6ff 0%,#ffffff 64%);font-family:Arial,sans-serif;box-sizing:border-box;\">'");
        js.append("+'<div style=\"display:flex;align-items:flex-start;gap:10px;\">'");
        js.append("+'<div style=\"width:34px;height:34px;border-radius:999px;background:linear-gradient(135deg,#2563eb,#06b6d4);color:#fff;display:flex;align-items:center;justify-content:center;font-weight:900;box-shadow:0 8px 18px rgba(37,99,235,.24);\">&#8635;</div>'");
        js.append("+'<div style=\"flex:1;min-width:0;\">'");
        js.append("+'<div style=\"font-size:14px;font-weight:900;color:#0f172a;line-height:1.25;\">").append(safeTitle).append("</div>'");
        js.append("+'<div style=\"font-size:12px;color:#475569;line-height:1.45;margin-top:4px;\">").append(safeMessage).append("</div>'");
        js.append("+'</div>'");
        js.append("+'<div style=\"font-size:20px;font-weight:900;color:#2563eb;white-space:nowrap;\">").append(p).append("%</div>'");
        js.append("+'</div>'");
        js.append("+'<div style=\"margin-top:12px;height:12px;border-radius:999px;background:#e2e8f0;overflow:hidden;\">'");
        js.append("+'<div style=\"width:").append(p).append("%;height:12px;border-radius:999px;background:linear-gradient(90deg,#2563eb,#06b6d4,#22c55e);transition:width .35s ease;\"></div>'");
        js.append("+'</div>'");
        js.append("+'<div style=\"font-size:11px;color:#64748b;margin-top:8px;\">Tampilan akan tertutup otomatis setelah data selesai dimuat.</div>'");
        js.append("+'</div>'; ");
        if (autoHide) {
            js.append("window.__postingJurnalLoadingHideTimer=setTimeout(function(){try{var e=document.getElementById(id);if(e){e.style.opacity='0';e.style.transform='translateY(-8px)';setTimeout(function(){try{var x=document.getElementById(id);if(x){x.style.display='none';}}catch(ee){}},380);}}catch(e){}window.__postingJurnalLoadingHideTimer=null;},750);");
        }
        js.append("}catch(e){}})();");
        try {
            Clients.evalJavaScript(js.toString());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/PostingJurnalLoadingUtil.java:72");
        }
    }

    private static String escapeJs(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        v = v.replace("\\", "\\\\");
        v = v.replace("'", "\\'");
        v = v.replace("\"", "\\\"");
        v = v.replace("\r", " ");
        v = v.replace("\n", " ");
        v = v.replace("<", "&lt;");
        v = v.replace(">", "&gt;");
        return v;
    }
}
