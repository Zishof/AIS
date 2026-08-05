package ais.action.master.dashboard.admin;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;

/**
 * Shared UI helpers for OBE and Tren Aktivitas dashboards.
 * All methods are static — no instance state.
 */
public final class DashboardUiUtil {

    private DashboardUiUtil() {}

    // ── 1. Global CSS ──────────────────────────────────────────────────────────

    /**
     * Injects a one-time {@code <style>} block into {@code parent}.
     * Safe to call multiple times per page — browser deduplicates identical style blocks.
     */
    public static void injectGlobalCss(Component parent) {
        appendHtml(parent,
            "<style>"
            /* === design tokens === */
            + ":root{"
            +   "--dash-radius:16px;"
            +   "--dash-shadow:0 2px 14px rgba(0,0,0,.07);"
            +   "--dash-border:#e5e7eb;"
            +   "--dash-bg:#f8fafc;"
            +   "--dash-primary:#2563eb;"
            +   "--dash-primary-lt:#eff6ff;"
            + "}"
            /* === table hover & layout === */
            + ".dash-tbl{width:100%;border-collapse:collapse;font-size:11px;}"
            + ".dash-tbl tbody tr{transition:background .12s;}"
            + ".dash-tbl tbody tr:hover td{background:rgba(37,99,235,.045)!important;}"
            + ".dash-tbl td,.dash-tbl th{padding:7px 10px;}"
            /* === panel card === */
            + ".dash-panel{background:#fff;border-radius:var(--dash-radius);"
            +   "padding:18px 20px;box-shadow:var(--dash-shadow);margin-bottom:12px;overflow:hidden;}"
            /* === buttons === */
            + ".dash-btn{display:inline-flex;align-items:center;justify-content:center;"
            +   "gap:4px;padding:6px 14px;border-radius:8px;border:1px solid var(--dash-border);"
            +   "background:#fff;color:#475569;font-size:11px;font-weight:600;cursor:pointer;"
            +   "transition:all .15s ease;line-height:1.2;white-space:nowrap;}"
            + ".dash-btn:hover:not([disabled]){background:var(--dash-primary-lt);"
            +   "color:var(--dash-primary);border-color:#bfdbfe;}"
            + ".dash-btn:active:not([disabled]){transform:translateY(1px);}"
            + ".dash-btn[disabled]{background:#f8fafc;color:#cbd5e1;cursor:not-allowed;"
            +   "border-color:#f1f5f9;}"
            /* === page indicator chip === */
            + ".dash-pg{display:inline-flex;align-items:center;justify-content:center;"
            +   "min-width:28px;height:28px;padding:0 8px;border-radius:8px;"
            +   "background:var(--dash-primary);color:#fff;font-size:11px;font-weight:700;}"
            /* === badge === */
            + ".dash-badge{display:inline-flex;align-items:center;border-radius:999px;"
            +   "padding:2px 9px;font-size:10px;font-weight:700;line-height:1.4;}"
            /* === animations === */
            + "@keyframes dash-spin{to{transform:rotate(360deg)}}"
            + "@keyframes dash-pulse{0%,100%{opacity:1}50%{opacity:.4}}"
            + ".dash-spin{display:inline-block;animation:dash-spin 1.1s linear infinite;}"
            + ".dash-pulse{animation:dash-pulse 1.6s ease-in-out infinite;}"
            /* === responsive helpers === */
            + "@media(max-width:640px){"
            +   ".dash-xs-hide{display:none!important;}"
            +   ".dash-xs-col{flex-direction:column!important;}"
            +   ".dash-xs-full{width:100%!important;min-width:0!important;}"
            + "}"
            + "</style>"
        );
    }

    // ── 2. Shared pagination bar ───────────────────────────────────────────────

    /**
     * Renders a styled Prev / indicator / Next pagination bar into {@code container}.
     * Does nothing when only one page exists.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void renderPageNav(Div container, int curPage, int pageSize, int total,
            EventListener prevEv, EventListener nextEv) {
        int totalPages = Math.max(1, (total + pageSize - 1) / pageSize);
        if (totalPages <= 1) return;
        int from = curPage * pageSize + 1;
        int to   = Math.min((curPage + 1) * pageSize, total);

        Div nav = new Div();
        nav.setStyle(
            "display:flex; align-items:center; justify-content:space-between; "
            + "flex-wrap:wrap; gap:8px; padding:10px 2px 2px; "
            + "border-top:1px solid #f1f5f9; margin-top:10px;");
        nav.setParent(container);

        // Left: range info (hidden on very small screens)
        appendHtml(nav,
            "<span class='dash-xs-hide' style='font-size:11px; color:#64748b; flex:1; min-width:0;'>"
            + "Menampilkan <b>" + from + "–" + to + "</b> dari <b>" + total
            + "</b> mata kuliah</span>");

        // Right: [Prev] [page/total] [Next]
        Div btnGroup = new Div();
        btnGroup.setStyle("display:flex; align-items:center; gap:6px;");
        btnGroup.setParent(nav);

        if (curPage > 0) {
            org.zkoss.zul.Button btnPrev = new org.zkoss.zul.Button("← Sebelumnya");
            btnPrev.setSclass("dash-btn");
            btnPrev.addEventListener("onClick", prevEv);
            btnGroup.appendChild(btnPrev);
        } else {
            appendHtml(btnGroup,
                "<button disabled class='dash-btn'>← Sebelumnya</button>");
        }

        appendHtml(btnGroup,
            "<span class='dash-pg'>" + (curPage + 1) + "</span>"
            + "<span style='font-size:11px; color:#94a3b8;'>/ " + totalPages + "</span>");

        if (curPage < totalPages - 1) {
            org.zkoss.zul.Button btnNext = new org.zkoss.zul.Button("Berikutnya →");
            btnNext.setSclass("dash-btn");
            btnNext.addEventListener("onClick", nextEv);
            btnGroup.appendChild(btnNext);
        } else {
            appendHtml(btnGroup,
                "<button disabled class='dash-btn'>Berikutnya →</button>");
        }
    }

    // ── 3. Common HTML snippets ────────────────────────────────────────────────

    /** Empty-state block with magnifier icon, title, and sub-text. */
    public static String emptyHtml(String title) {
        return "<div style='padding:32px 16px; text-align:center;'>"
             + "<div style='font-size:30px; margin-bottom:8px; opacity:.6;'>&#128269;</div>"
             + "<div style='font-size:13px; font-weight:700; color:#475569;'>"
             + esc(title) + "</div>"
             + "<div style='font-size:11px; color:#94a3b8; margin-top:4px;'>"
             + "Coba ubah atau kosongkan filter pencarian.</div>"
             + "</div>";
    }

    /** Animated loading block — requires {@link #injectGlobalCss} to be called first. */
    public static String loadingHtml(String message) {
        return "<div style='padding:54px 16px; text-align:center;'>"
             + "<div class='dash-spin' style='font-size:34px; margin-bottom:14px;'>&#9696;</div>"
             + "<div style='font-size:15px; font-weight:700; color:#334155;'>"
             + esc(message) + "</div>"
             + "<div style='font-size:11px; color:#94a3b8; margin-top:6px;'>"
             + "Sedang menghitung data, mohon tunggu…</div>"
             + "</div>";
    }

    /** Small info bar showing current page range above a paged table. */
    public static String pageInfoBar(int curPage, int pageSize, int total) {
        int totalPages = Math.max(1, (total + pageSize - 1) / pageSize);
        int from = curPage * pageSize + 1;
        int to   = Math.min((curPage + 1) * pageSize, total);
        return "<div style='display:flex; justify-content:space-between; align-items:center; "
             +   "padding:5px 10px; background:#f8fafc; border-radius:8px; "
             +   "margin-bottom:8px; font-size:11px; border:1px solid #f1f5f9;'>"
             + "<span style='color:#64748b;'>Hal. <b>" + (curPage + 1)
             + "</b> / <b>" + totalPages + "</b></span>"
             + "<span style='color:#94a3b8;'>" + from + "–" + to
             + " dari " + total + " MK</span>"
             + "</div>";
    }

    /** Colored rounded badge span. */
    public static String badge(String text, String bg, String color) {
        return "<span class='dash-badge' style='background:" + bg + ";color:" + color + ";'>"
             + esc(text) + "</span>";
    }

    /** Green (ok) or red (not ok) status badge. */
    public static String statusBadge(boolean ok, String okText, String nokText) {
        return badge(ok ? okText : nokText,
                     ok ? "#dcfce7" : "#fee2e2",
                     ok ? "#166534" : "#991b1b");
    }

    // ── 4. Low-level helpers ──────────────────────────────────────────────────

    /** HTML-escapes a string; returns empty string for null. */
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static void appendHtml(Component parent, String html) {
        new Html(html).setParent(parent);
    }
}
