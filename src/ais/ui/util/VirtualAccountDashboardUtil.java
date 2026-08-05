package ais.ui.util;

import java.util.ArrayList;
import java.util.List;

import ais.common.Common;

/**
 * Util kecil untuk membuat ringkasan HTML/CSS Virtual Account.
 * Tidak memakai JFreeChart agar ringan untuk ZKoss 5.5 dan mudah dipakai ulang.
 */
public class VirtualAccountDashboardUtil {

    public static class BankSummary {
        public String bank;
        public int jumlah;
        public double nominal;

        public BankSummary(String bank, int jumlah, double nominal) {
            this.bank = bank;
            this.jumlah = jumlah;
            this.nominal = nominal;
        }
    }

    public static class TrendSummary {
        public String label;
        public int jumlah;

        public TrendSummary(String label, int jumlah) {
            this.label = label;
            this.jumlah = jumlah;
        }
    }

    public static class Summary {
        public int total;
        public int sudahBayar;
        public int belumBayar;
        public int kendala;
        public int belumKadaluarsa;
        public int kadaluarsa;
        public double totalNominal;
        public double totalBiayaAdmin;
        public double totalTopup;
        public List<BankSummary> bankSummaries = new ArrayList<BankSummary>();
        public List<TrendSummary> trendSummaries = new ArrayList<TrendSummary>();
        public String periode;
        public String keteranganFilter;
    }

    private VirtualAccountDashboardUtil() {
    }

    public static String renderProgress(String title, String detail, int percent) {
        int pct = normalizePercent(percent);
        String safeTitle = escape(title == null ? "Memuat data virtual account" : title);
        String safeDetail = escape(detail == null ? "Data sedang disiapkan." : detail);
        String pctText = pct + "%";
        return "<div style='font-family:Arial,sans-serif;margin:0 0 10px 0;padding:12px;border-radius:8px;"
                + "background:linear-gradient(135deg,#eff6ff,#ecfeff);border:1px solid #bfdbfe;box-shadow:0 6px 18px rgba(15,23,42,.08);'>"
                + "<div style='display:flex;justify-content:space-between;gap:10px;align-items:center;'>"
                + "<div>"
                + "<div style='font-size:14px;font-weight:800;color:#0f172a;'>" + safeTitle + "</div>"
                + "<div style='font-size:11px;color:#475569;margin-top:2px;'>" + safeDetail + "</div>"
                + "</div>"
                + "<div style='font-size:18px;font-weight:900;color:#1d4ed8;'>" + pctText + "</div>"
                + "</div>"
                + "<div style='height:11px;border-radius:999px;background:#dbeafe;margin-top:10px;overflow:hidden;'>"
                + "<div style='height:11px;width:" + pct + "%;border-radius:999px;background:linear-gradient(90deg,#2563eb,#06b6d4,#22c55e);'></div>"
                + "</div>"
                + "</div>";
    }

    public static String renderDashboard(Summary data) {
        if (data == null) {
            data = new Summary();
        }

        int total = Math.max(0, data.total);
        int paidPercent = percent(data.sudahBayar, total);
        int unpaidPercent = percent(data.belumBayar, total);
        int healthyPercent = percent(Math.max(0, total - data.kendala), total);
        int activePercent = percent(data.belumKadaluarsa, total);
        int expiredPercent = percent(data.kadaluarsa, total);

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Arial,sans-serif;background:#f8fafc;padding:12px;box-sizing:border-box;color:#0f172a;'>");
        sb.append("<div style='background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);color:#fff;border-radius:8px;padding:16px;margin-bottom:12px;box-shadow:0 12px 26px rgba(15,23,42,.18);'>");
        sb.append("<div style='font-size:20px;font-weight:900;margin-bottom:4px;'>Dasbor Virtual Account</div>");
        sb.append("<div style='font-size:12px;line-height:1.45;color:#dbeafe;'>Melihat jumlah tagihan online, pembayaran yang sudah masuk, tagihan yang masih aktif, dan transaksi yang perlu diperiksa. Ringkasan mengikuti filter tanggal, bank, unit, dan status yang dipilih.</div>");
        if (data.periode != null && data.periode.trim().length() > 0) {
            sb.append("<div style='font-size:11px;margin-top:8px;color:#bfdbfe;'>Periode: ").append(escape(data.periode)).append("</div>");
        }
        if (data.keteranganFilter != null && data.keteranganFilter.trim().length() > 0) {
            sb.append("<div style='font-size:11px;margin-top:2px;color:#bfdbfe;'>Filter: ").append(escape(data.keteranganFilter)).append("</div>");
        }
        sb.append("</div>");

        sb.append("<div style='display:flex;flex-wrap:wrap;gap:10px;margin-bottom:12px;'>");
        appendCard(sb, "Total VA", total, "Semua nomor virtual account sesuai filter.", "#e0f2fe");
        appendCard(sb, "Sudah Bayar", data.sudahBayar, "Pembayaran sudah tercatat di sistem.", "#dcfce7");
        appendCard(sb, "Belum Bayar", data.belumBayar, "Masih menunggu pembayaran dari pengguna.", "#fef9c3");
        appendCard(sb, "Kendala", data.kendala, "Perlu dicek karena ada catatan gangguan.", "#fee2e2");
        appendMoneyCard(sb, "Nominal", data.totalNominal, "Total nilai tagihan virtual account.", "#ede9fe");
        appendMoneyCard(sb, "Biaya Admin", data.totalBiayaAdmin, "Biaya administrasi yang tercatat.", "#ffedd5");
        appendMoneyCard(sb, "Topup", data.totalTopup, "Tambahan deposit/tabungan dari VA.", "#ccfbf1");
        appendCard(sb, "Kadaluarsa", data.kadaluarsa, "VA yang sudah melewati batas waktu.", "#f1f5f9");
        sb.append("</div>");

        sb.append("<div style='display:flex;flex-wrap:wrap;gap:12px;margin-bottom:12px;'>");
        sb.append("<div style='flex:1;min-width:300px;background:#fff;border:1px solid #e2e8f0;border-radius:8px;padding:14px;box-shadow:0 8px 20px rgba(15,23,42,.06);'>");
        sb.append("<div style='font-weight:900;font-size:15px;margin-bottom:4px;'>Kondisi pembayaran</div>");
        sb.append("<div style='font-size:12px;color:#64748b;margin-bottom:10px;'>Membantu melihat pembayaran yang sudah selesai dan yang masih perlu ditagih.</div>");
        appendProgress(sb, "Sudah bayar", paidPercent, data.sudahBayar, total);
        appendProgress(sb, "Belum bayar", unpaidPercent, data.belumBayar, total);
        appendProgress(sb, "Masih aktif", activePercent, data.belumKadaluarsa, total);
        appendProgress(sb, "Kadaluarsa", expiredPercent, data.kadaluarsa, total);
        sb.append("</div>");

        sb.append("<div style='flex:1;min-width:300px;background:#fff;border:1px solid #e2e8f0;border-radius:8px;padding:14px;box-shadow:0 8px 20px rgba(15,23,42,.06);'>");
        sb.append("<div style='font-weight:900;font-size:15px;margin-bottom:4px;'>Spider kesiapan VA</div>");
        sb.append("<div style='font-size:12px;color:#64748b;margin-bottom:10px;'>Semakin penuh nilainya, semakin baik kondisi data virtual account.</div>");
        sb.append("<div style='display:grid;grid-template-columns:1fr 1fr;gap:8px;'>");
        appendSpiderMetric(sb, "Pembayaran", paidPercent);
        appendSpiderMetric(sb, "Data aktif", activePercent);
        appendSpiderMetric(sb, "Tanpa kendala", healthyPercent);
        appendSpiderMetric(sb, "Belum bayar", 100 - unpaidPercent);
        sb.append("</div>");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("<div style='display:flex;flex-wrap:wrap;gap:12px;'>");
        sb.append("<div style='flex:1;min-width:320px;background:#fff;border:1px solid #e2e8f0;border-radius:8px;padding:14px;box-shadow:0 8px 20px rgba(15,23,42,.06);'>");
        sb.append("<div style='font-weight:900;font-size:15px;margin-bottom:4px;'>Ringkasan per bank</div>");
        sb.append("<div style='font-size:12px;color:#64748b;margin-bottom:10px;'>Menunjukkan kanal bank yang paling banyak dipakai.</div>");
        appendBankBars(sb, data.bankSummaries);
        sb.append("</div>");

        sb.append("<div style='flex:1;min-width:320px;background:#fff;border:1px solid #e2e8f0;border-radius:8px;padding:14px;box-shadow:0 8px 20px rgba(15,23,42,.06);'>");
        sb.append("<div style='font-weight:900;font-size:15px;margin-bottom:4px;'>Tren harian</div>");
        sb.append("<div style='font-size:12px;color:#64748b;margin-bottom:10px;'>Memudahkan melihat hari yang ramai dibuat atau dibayar.</div>");
        appendTrendBars(sb, data.trendSummaries);
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");
        return sb.toString();
    }

    private static void appendCard(StringBuilder sb, String title, int value, String desc, String bg) {
        sb.append("<div style='flex:1;min-width:150px;background:").append(bg).append(";border:1px solid rgba(148,163,184,.45);border-radius:8px;padding:12px;'>");
        sb.append("<div style='font-size:11px;color:#475569;font-weight:700;'>").append(escape(title)).append("</div>");
        sb.append("<div style='font-size:24px;font-weight:900;margin:4px 0;color:#0f172a;'>").append(formatNumber(value)).append("</div>");
        sb.append("<div style='font-size:11px;color:#64748b;line-height:1.35;'>").append(escape(desc)).append("</div>");
        sb.append("</div>");
    }

    private static void appendMoneyCard(StringBuilder sb, String title, double value, String desc, String bg) {
        sb.append("<div style='flex:1;min-width:170px;background:").append(bg).append(";border:1px solid rgba(148,163,184,.45);border-radius:8px;padding:12px;'>");
        sb.append("<div style='font-size:11px;color:#475569;font-weight:700;'>").append(escape(title)).append("</div>");
        sb.append("<div style='font-size:20px;font-weight:900;margin:4px 0;color:#0f172a;'>").append(formatMoney(value)).append("</div>");
        sb.append("<div style='font-size:11px;color:#64748b;line-height:1.35;'>").append(escape(desc)).append("</div>");
        sb.append("</div>");
    }

    private static void appendProgress(StringBuilder sb, String title, int percent, int value, int total) {
        int pct = normalizePercent(percent);
        sb.append("<div style='margin:8px 0;'>");
        sb.append("<div style='display:flex;justify-content:space-between;font-size:12px;margin-bottom:4px;'>");
        sb.append("<b>").append(escape(title)).append("</b><span>").append(formatNumber(value)).append(" / ").append(formatNumber(total)).append(" (" + pct + "%)</span>");
        sb.append("</div><div style='height:10px;border-radius:999px;background:#e2e8f0;overflow:hidden;'>");
        sb.append("<div style='height:10px;width:").append(pct).append("%;border-radius:999px;background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div>");
        sb.append("</div></div>");
    }

    private static void appendSpiderMetric(StringBuilder sb, String label, int percent) {
        int pct = normalizePercent(percent);
        sb.append("<div style='border:1px solid #e2e8f0;border-radius:8px;padding:10px;background:#f8fafc;'>");
        sb.append("<div style='font-size:11px;color:#475569;font-weight:800;margin-bottom:6px;'>").append(escape(label)).append("</div>");
        sb.append("<div style='height:8px;background:#e2e8f0;border-radius:999px;overflow:hidden;'><div style='height:8px;width:").append(pct).append("%;background:linear-gradient(90deg,#8b5cf6,#22c55e);'></div></div>");
        sb.append("<div style='font-size:16px;font-weight:900;margin-top:5px;'>").append(pct).append("%</div>");
        sb.append("</div>");
    }

    private static void appendBankBars(StringBuilder sb, List<BankSummary> banks) {
        if (banks == null || banks.isEmpty()) {
            sb.append("<div style='font-size:12px;color:#64748b;'>Belum ada data bank pada filter ini.</div>");
            return;
        }
        int max = 1;
        for (int i = 0; i < banks.size(); i++) {
            if (banks.get(i).jumlah > max) {
                max = banks.get(i).jumlah;
            }
        }
        for (int i = 0; i < banks.size(); i++) {
            BankSummary bank = banks.get(i);
            int pct = percent(bank.jumlah, max);
            sb.append("<div style='margin:8px 0;'>");
            sb.append("<div style='display:flex;justify-content:space-between;font-size:12px;margin-bottom:3px;'><b>")
                    .append(escape(empty(bank.bank) ? "Tanpa bank" : bank.bank)).append("</b><span>")
                    .append(formatNumber(bank.jumlah)).append(" VA &middot; ").append(formatMoney(bank.nominal)).append("</span></div>");
            sb.append("<div style='height:9px;background:#e2e8f0;border-radius:999px;overflow:hidden;'><div style='height:9px;width:")
                    .append(pct).append("%;background:linear-gradient(90deg,#14b8a6,#2563eb);'></div></div>");
            sb.append("</div>");
        }
    }

    private static void appendTrendBars(StringBuilder sb, List<TrendSummary> trends) {
        if (trends == null || trends.isEmpty()) {
            sb.append("<div style='font-size:12px;color:#64748b;'>Belum ada tren pada periode ini.</div>");
            return;
        }
        int max = 1;
        for (int i = 0; i < trends.size(); i++) {
            if (trends.get(i).jumlah > max) {
                max = trends.get(i).jumlah;
            }
        }
        sb.append("<div style='display:flex;align-items:flex-end;gap:7px;height:140px;border-bottom:1px solid #e2e8f0;padding-top:8px;'>");
        for (int i = 0; i < trends.size(); i++) {
            TrendSummary trend = trends.get(i);
            int pct = Math.max(3, percent(trend.jumlah, max));
            sb.append("<div style='flex:1;text-align:center;'>");
            sb.append("<div style='height:").append(pct).append("%;min-height:4px;border-radius:8px 8px 0 0;background:linear-gradient(180deg,#2563eb,#38bdf8);'></div>");
            sb.append("<div style='font-size:10px;color:#64748b;margin-top:4px;'>").append(escape(trend.label)).append("</div>");
            sb.append("<div style='font-size:11px;font-weight:800;color:#0f172a;'>").append(formatNumber(trend.jumlah)).append("</div>");
            sb.append("</div>");
        }
        sb.append("</div>");
    }

    private static int percent(int value, int total) {
        if (total <= 0) {
            return 0;
        }
        return normalizePercent((int) Math.round((value * 100.0) / total));
    }

    private static int normalizePercent(int percent) {
        if (percent < 0) {
            return 0;
        }
        if (percent > 100) {
            return 100;
        }
        return percent;
    }

    private static String formatNumber(int value) {
        try {
            return Common.numberFormat.get().format(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static String formatMoney(double value) {
        try {
            return Common.numberFormat.get().format(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static boolean empty(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
