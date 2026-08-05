package ais.action.master.sapto.util;

import java.util.ArrayList;
import java.util.List;

import ais.common.Common;

/**
 * Generates self-contained HTML+SVG charts for SAPTO dashboards.
 * No external chart libraries — pure SVG/HTML only.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SaptoChartBuilder {

    private static final String[] PALETTE = {
        "#3B82F6","#10B981","#F59E0B","#EF4444","#8B5CF6",
        "#06B6D4","#F97316","#84CC16","#EC4899","#6366F1"
    };

    private static final String STYLE_WRAPPER =
        "font-family:system-ui,-apple-system,sans-serif;background:#F9FAFB;border-radius:12px;" +
        "padding:20px;margin:4px;box-shadow:0 1px 4px rgba(0,0,0,.08)";

    public static String build(String chartType, List<List> datas, int dataStartRow,
                               String[] headers, String title) {
        if (datas == null) return empty(title);
        List<List> rows = extract(datas, dataStartRow);
        if (rows.isEmpty()) return empty(title);

        if ("bar".equals(chartType))     return bar(rows, headers, title, false);
        if ("stacked".equals(chartType)) return bar(rows, headers, title, true);
        if ("line".equals(chartType))    return line(rows, headers, title);
        if ("pie".equals(chartType))     return pie(rows, headers, title);
        if ("radar".equals(chartType))   return radar(rows, headers, title);
        return empty(title);
    }

    // -----------------------------------------------------------------------
    // Bar chart (normal and stacked)
    // -----------------------------------------------------------------------

    private static String bar(List<List> rows, String[] headers, String title, boolean stacked) {
        final int W = 560, H = 300;
        final int PL = 65, PR = 20, PT = 20, PB = 90;
        int cW = W - PL - PR, cH = H - PT - PB;
        int n = Math.min(rows.size(), 20);
        int maxSeries = 0;
        for (List r : rows) maxSeries = Math.max(maxSeries, r.size() - 1);
        maxSeries = Math.min(maxSeries, PALETTE.length);
        if (maxSeries == 0) return empty(title);

        double maxV = 0;
        for (int i = 0; i < n; i++) {
            List r = rows.get(i);
            if (stacked) {
                double s = 0; for (int j = 1; j <= maxSeries && j < r.size(); j++) s += toNum(r.get(j));
                maxV = Math.max(maxV, s);
            } else {
                for (int j = 1; j <= maxSeries && j < r.size(); j++) maxV = Math.max(maxV, toNum(r.get(j)));
            }
        }
        if (maxV == 0) maxV = 1;

        StringBuilder sb = new StringBuilder();
        sb.append(svgOpen(W, H));
        gridLines(sb, PL, PT, cW, cH, 5, maxV);

        int gw = cW / n;
        for (int i = 0; i < n; i++) {
            List r = rows.get(i);
            String lbl = trim(cellStr(r, 0), 14);
            int gx = PL + i * gw;
            if (stacked) {
                double cum = 0;
                for (int s = 1; s <= maxSeries && s < r.size(); s++) {
                    double v = toNum(r.get(s));
                    int bh = (int)(cH * v / maxV);
                    int by = PT + cH - (int)(cH * (cum + v) / maxV);
                    rect(sb, gx + 3, by, gw - 6, bh, PALETTE[(s-1) % PALETTE.length]);
                    cum += v;
                }
            } else {
                int sw = Math.max((gw - 6) / maxSeries, 2);
                for (int s = 1; s <= maxSeries && s < r.size(); s++) {
                    double v = toNum(r.get(s));
                    int bh = (int)(cH * v / maxV);
                    int bx = gx + 3 + (s-1) * sw;
                    rect(sb, bx, PT + cH - bh, sw - 1, bh, PALETTE[(s-1) % PALETTE.length]);
                }
            }
            rotatedLabel(sb, gx + gw / 2, PT + cH + 8, lbl, "#374151", 9);
        }
        axes(sb, PL, PT, cW, cH);
        if (headers != null && maxSeries > 0) legend(sb, PL, H - 14, headers, 1, maxSeries);
        sb.append("</svg>");
        return wrap(sb.toString(), title);
    }

    // -----------------------------------------------------------------------
    // Line chart
    // -----------------------------------------------------------------------

    private static String line(List<List> rows, String[] headers, String title) {
        final int W = 560, H = 300;
        final int PL = 65, PR = 40, PT = 20, PB = 90;
        int cW = W - PL - PR, cH = H - PT - PB;
        int n = Math.min(rows.size(), 30);
        int maxSeries = 0;
        for (List r : rows) maxSeries = Math.max(maxSeries, r.size() - 1);
        maxSeries = Math.min(maxSeries, PALETTE.length);
        if (maxSeries == 0) return empty(title);

        double maxV = 0, minV = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            List r = rows.get(i);
            for (int j = 1; j <= maxSeries && j < r.size(); j++) {
                double v = toNum(r.get(j));
                maxV = Math.max(maxV, v);
                if (v > 0) minV = Math.min(minV, v);
            }
        }
        if (maxV == 0) maxV = 1;
        if (minV == Double.MAX_VALUE) minV = 0;

        StringBuilder sb = new StringBuilder();
        sb.append(svgOpen(W, H));
        gridLines(sb, PL, PT, cW, cH, 5, maxV);

        for (int s = 1; s <= maxSeries; s++) {
            String color = PALETTE[(s-1) % PALETTE.length];
            StringBuilder points = new StringBuilder();
            for (int i = 0; i < n; i++) {
                List r = rows.get(i);
                double v = s < r.size() ? toNum(r.get(s)) : 0;
                int x = PL + (n > 1 ? i * cW / (n - 1) : cW / 2);
                int y = PT + cH - (int)(cH * v / maxV);
                points.append(x).append(",").append(y).append(" ");
            }
            sb.append("<polyline points='").append(points)
              .append("' fill='none' stroke='").append(color)
              .append("' stroke-width='2.5' stroke-linejoin='round'/>");
            for (int i = 0; i < n; i++) {
                List r = rows.get(i);
                double v = s < r.size() ? toNum(r.get(s)) : 0;
                int x = PL + (n > 1 ? i * cW / (n - 1) : cW / 2);
                int y = PT + cH - (int)(cH * v / maxV);
                sb.append("<circle cx='").append(x).append("' cy='").append(y)
                  .append("' r='3.5' fill='").append(color).append("' stroke='#fff' stroke-width='1.5'/>");
            }
        }

        // X labels
        int step = Math.max(1, n / 8);
        for (int i = 0; i < n; i += step) {
            List r = rows.get(i);
            String lbl = trim(cellStr(r, 0), 10);
            int x = PL + (n > 1 ? i * cW / (n - 1) : cW / 2);
            rotatedLabel(sb, x, PT + cH + 8, lbl, "#374151", 9);
        }

        axes(sb, PL, PT, cW, cH);
        if (headers != null && maxSeries > 0) legend(sb, PL, H - 14, headers, 1, maxSeries);
        sb.append("</svg>");
        return wrap(sb.toString(), title);
    }

    // -----------------------------------------------------------------------
    // Pie chart
    // -----------------------------------------------------------------------

    private static String pie(List<List> rows, String[] headers, String title) {
        final int W = 480, H = 280;
        final int CX = 140, CY = 130, R = 110;
        int n = Math.min(rows.size(), 8);
        double total = 0;
        for (int i = 0; i < n; i++) {
            List r = rows.get(i);
            total += r.size() > 1 ? toNum(r.get(1)) : 0;
        }
        if (total == 0) return empty(title);

        StringBuilder sb = new StringBuilder();
        sb.append(svgOpen(W, H));

        double angle = -Math.PI / 2;
        for (int i = 0; i < n; i++) {
            List r = rows.get(i);
            double v = r.size() > 1 ? toNum(r.get(1)) : 0;
            double sweep = 2 * Math.PI * v / total;
            double endAngle = angle + sweep;
            int x1 = (int)(CX + R * Math.cos(angle));
            int y1 = (int)(CY + R * Math.sin(angle));
            int x2 = (int)(CX + R * Math.cos(endAngle));
            int y2 = (int)(CY + R * Math.sin(endAngle));
            int largeArc = sweep > Math.PI ? 1 : 0;
            String color = PALETTE[i % PALETTE.length];
            sb.append("<path d='M").append(CX).append(",").append(CY)
              .append(" L").append(x1).append(",").append(y1)
              .append(" A").append(R).append(",").append(R).append(" 0 ")
              .append(largeArc).append(",1 ")
              .append(x2).append(",").append(y2)
              .append(" Z' fill='").append(color).append("' stroke='#fff' stroke-width='2'/>");
            angle = endAngle;
        }

        // Legend on right
        int lx = CX + R + 30, ly = 30;
        for (int i = 0; i < n; i++) {
            List r = rows.get(i);
            String lbl = trim(cellStr(r, 0), 16);
            double v = r.size() > 1 ? toNum(r.get(1)) : 0;
            int pct = (int)(v * 100 / total);
            rect(sb, lx, ly, 12, 12, PALETTE[i % PALETTE.length]);
            sb.append("<text x='").append(lx + 16).append("' y='").append(ly + 10)
              .append("' font-size='10' fill='#374151'>").append(esc(lbl))
              .append(" (").append(pct).append("%)</text>");
            ly += 22;
        }

        sb.append("</svg>");
        return wrap(sb.toString(), title);
    }

    // -----------------------------------------------------------------------
    // Radar / spider-web chart
    // -----------------------------------------------------------------------

    private static String radar(List<List> rows, String[] headers, String title) {
        final int W = 500, H = 400;
        final int CX = 180, CY = 200, R = 140;
        int n = Math.min(rows.size(), 10);
        if (n < 3) return bar(rows, headers, title, false);

        double maxV = 0;
        for (int i = 0; i < n; i++) {
            List r = rows.get(i);
            for (int j = 1; j < r.size(); j++) maxV = Math.max(maxV, toNum(r.get(j)));
        }
        if (maxV == 0) maxV = 1;

        StringBuilder sb = new StringBuilder();
        sb.append(svgOpen(W, H));

        // Spoke lines and grid
        for (int lvl = 1; lvl <= 5; lvl++) {
            double rr = R * lvl / 5.0;
            StringBuilder pts = new StringBuilder();
            for (int i = 0; i < n; i++) {
                double a = 2 * Math.PI * i / n - Math.PI / 2;
                pts.append((int)(CX + rr * Math.cos(a))).append(",")
                   .append((int)(CY + rr * Math.sin(a))).append(" ");
            }
            sb.append("<polygon points='").append(pts)
              .append("' fill='none' stroke='#E5E7EB' stroke-width='1'/>");
        }
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n - Math.PI / 2;
            int ex = (int)(CX + R * Math.cos(a));
            int ey = (int)(CY + R * Math.sin(a));
            sb.append("<line x1='").append(CX).append("' y1='").append(CY)
              .append("' x2='").append(ex).append("' y2='").append(ey)
              .append("' stroke='#D1D5DB' stroke-width='1'/>");
            String lbl = trim(cellStr(rows.get(i), 0), 14);
            int tx = (int)(CX + (R + 18) * Math.cos(a));
            int ty = (int)(CY + (R + 14) * Math.sin(a));
            sb.append("<text x='").append(tx).append("' y='").append(ty)
              .append("' text-anchor='middle' font-size='9' fill='#374151'>")
              .append(esc(lbl)).append("</text>");
        }

        // Data series — use column 1 from each row
        StringBuilder pts = new StringBuilder();
        for (int i = 0; i < n; i++) {
            List r = rows.get(i);
            double v = r.size() > 1 ? toNum(r.get(1)) : 0;
            double rr = R * v / maxV;
            double a = 2 * Math.PI * i / n - Math.PI / 2;
            pts.append((int)(CX + rr * Math.cos(a))).append(",")
               .append((int)(CY + rr * Math.sin(a))).append(" ");
        }
        sb.append("<polygon points='").append(pts)
          .append("' fill='").append(PALETTE[0]).append("' fill-opacity='.25' stroke='")
          .append(PALETTE[0]).append("' stroke-width='2'/>");

        sb.append("</svg>");
        return wrap(sb.toString(), title);
    }

    // -----------------------------------------------------------------------
    // Summary cards panel (for identity / profile pages)
    // -----------------------------------------------------------------------

    public static String buildSummaryCards(String[][] cards) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='display:flex;flex-wrap:wrap;gap:12px;padding:8px'>");
        for (String[] card : cards) {
            if (card == null || card.length < 2) continue;
            sb.append("<div style='background:#fff;border-radius:10px;padding:16px 20px;")
              .append("box-shadow:0 1px 4px rgba(0,0,0,.1);min-width:180px;flex:1 1 180px'>")
              .append("<div style='font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:.05em;margin-bottom:4px'>")
              .append(esc(card[0])).append("</div>")
              .append("<div style='font-size:20px;font-weight:700;color:#111827'>")
              .append(esc(card[1])).append("</div>")
              .append(card.length > 2 && card[2] != null ?
                  "<div style='font-size:11px;color:#9CA3AF;margin-top:2px'>" + esc(card[2]) + "</div>" : "")
              .append("</div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Recommendation panel
    // -----------------------------------------------------------------------

    public static String buildRecommendation(String heading, String[] points) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background:#EFF6FF;border-left:4px solid #3B82F6;border-radius:0 8px 8px 0;")
          .append("padding:12px 16px;margin:8px 4px'>")
          .append("<div style='font-size:12px;font-weight:600;color:#1D4ED8;margin-bottom:6px'>")
          .append(esc(heading)).append("</div><ul style='margin:0;padding-left:18px'>");
        for (String p : points) {
            sb.append("<li style='font-size:11px;color:#374151;margin-bottom:3px'>").append(esc(p)).append("</li>");
        }
        sb.append("</ul></div>");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // SVG helpers
    // -----------------------------------------------------------------------

    private static String svgOpen(int w, int h) {
        return "<svg viewBox='0 0 " + w + " " + h + "' xmlns='http://www.w3.org/2000/svg' " +
               "style='width:100%;max-width:" + w + "px;display:block'>";
    }

    private static void gridLines(StringBuilder sb, int pl, int pt, int cW, int cH, int steps, double maxV) {
        for (int i = 0; i <= steps; i++) {
            int y = pt + (int)(cH * i / (double) steps);
            double val = maxV * (steps - i) / steps;
            sb.append("<line x1='").append(pl).append("' y1='").append(y)
              .append("' x2='").append(pl + cW).append("' y2='").append(y)
              .append("' stroke='#E5E7EB' stroke-width='1' stroke-dasharray='3,3'/>");
            sb.append("<text x='").append(pl - 5).append("' y='").append(y + 4)
              .append("' text-anchor='end' font-size='9' fill='#6B7280'>")
              .append(fmtNum(val)).append("</text>");
        }
    }

    private static void axes(StringBuilder sb, int pl, int pt, int cW, int cH) {
        sb.append("<line x1='").append(pl).append("' y1='").append(pt)
          .append("' x2='").append(pl).append("' y2='").append(pt + cH)
          .append("' stroke='#9CA3AF' stroke-width='1.5'/>");
        sb.append("<line x1='").append(pl).append("' y1='").append(pt + cH)
          .append("' x2='").append(pl + cW).append("' y2='").append(pt + cH)
          .append("' stroke='#9CA3AF' stroke-width='1.5'/>");
    }

    private static void rect(StringBuilder sb, int x, int y, int w, int h, String color) {
        if (h <= 0) return;
        sb.append("<rect x='").append(x).append("' y='").append(y)
          .append("' width='").append(w).append("' height='").append(h)
          .append("' fill='").append(color).append("' rx='3'/>");
    }

    private static void rotatedLabel(StringBuilder sb, int x, int y, String lbl, String color, int size) {
        sb.append("<text transform='rotate(-35,").append(x).append(",").append(y)
          .append(")' x='").append(x).append("' y='").append(y)
          .append("' text-anchor='end' font-size='").append(size).append("' fill='")
          .append(color).append("'>").append(esc(lbl)).append("</text>");
    }

    private static void legend(StringBuilder sb, int startX, int y, String[] headers, int from, int count) {
        int x = startX;
        for (int i = from; i < from + count && i < headers.length; i++) {
            String h = headers[i] == null ? ("Seri " + i) : esc(headers[i]);
            rect(sb, x, y - 8, 10, 10, PALETTE[(i - from) % PALETTE.length]);
            sb.append("<text x='").append(x + 14).append("' y='").append(y + 1)
              .append("' font-size='9' fill='#374151'>").append(h).append("</text>");
            x += Math.min(h.length() * 7 + 22, 90);
        }
    }

    private static String wrap(String content, String title) {
        return "<div style='" + STYLE_WRAPPER + "'>" +
            (title != null && !title.isEmpty() ?
                "<p style='margin:0 0 12px;font-size:13px;font-weight:600;color:#111827'>" + esc(title) + "</p>" : "") +
            content + "</div>";
    }

    private static String empty(String title) {
        return wrap("<p style='color:#9CA3AF;text-align:center;padding:32px 0;font-size:13px'>" +
                    "Belum ada data yang dapat ditampilkan dalam grafik.</p>", title);
    }

    // -----------------------------------------------------------------------
    // Data extraction helpers
    // -----------------------------------------------------------------------

    private static List<List> extract(List<List> datas, int startRow) {
        List<List> result = new ArrayList<List>();
        for (int i = startRow; i < datas.size(); i++) {
            List r = datas.get(i);
            if (r == null || r.isEmpty()) continue;
            for (Object o : r) {
                if (o != null && !o.toString().trim().isEmpty()) { result.add(r); break; }
            }
        }
        return result;
    }

    private static double toNum(Object o) {
        if (o == null) return 0;
        try { return Double.parseDouble(o.toString().replace(",", "").trim()); } catch (Exception e) { return 0; }
    }

    private static String cellStr(List r, int col) {
        if (r == null || col >= r.size() || r.get(col) == null) return "";
        return r.get(col).toString().trim();
    }

    private static String esc(Object o) {
        if (o == null) return "";
        return o.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    private static String trim(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private static String fmtNum(double v) {
        return Common.numberFormat.get().format(v); 
    }
}
