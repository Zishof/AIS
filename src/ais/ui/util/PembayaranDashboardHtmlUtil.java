package ais.ui.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Util HTML ringan untuk dashboard pembayaran.
 * Dibuat tanpa JFreeChart agar cepat dirender di ZKoss 5.5 dan tetap kompatibel Java 1.7.
 */
public final class PembayaranDashboardHtmlUtil {

	private static final ThreadLocal<NumberFormat> NUMBER_FORMAT = new ThreadLocal<NumberFormat>() {
		@Override
		protected NumberFormat initialValue() {
			return new DecimalFormat("#,##0.##");
		}
	};

	private PembayaranDashboardHtmlUtil() {
	}

	public static String buildSpiderWebOnly(String title, String description, double tagihan, double dibayar, double sisa,
			double totalRiwayat, double jumlahTransaksi, double rataRata) {
		String[] labels = new String[] { "Tagihan", "Dibayar", "Sisa", "Riwayat", "Frekuensi", "Rata-rata" };
		double maxUang = maxAbs(tagihan, dibayar, sisa, totalRiwayat, rataRata);
		double[] percent = new double[] { persen(tagihan, maxUang), persen(dibayar, maxUang), persen(sisa, maxUang),
				persen(totalRiwayat, maxUang), persenJumlahTransaksi(jumlahTransaksi), persen(rataRata, maxUang) };
		double[] rawValues = new double[] { tagihan, dibayar, sisa, totalRiwayat, jumlahTransaksi, rataRata };

		StringBuilder html = new StringBuilder();
		html.append("<div style='background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;padding:12px;box-sizing:border-box;font-family:Arial, sans-serif;'>");
		html.append("<div style='font-size:13px;font-weight:700;color:#0f172a;'>").append(escape(title)).append("</div>");
		if (description != null && description.trim().length() > 0) {
			html.append("<div style='font-size:10px;color:#64748b;margin-top:2px;'>").append(escape(description)).append("</div>");
		}
		html.append("<div style='display:flex;gap:12px;align-items:center;flex-wrap:wrap;margin-top:10px;'>");
		html.append(buildRadarSvg(labels, percent));
		html.append("<div style='flex:1;min-width:180px;'>");
		for (int i = 0; i < labels.length; i++) {
			appendLegendRow(html, labels[i], rawValues[i], percent[i], i == 4);
		}
		html.append("</div>");
		html.append("</div>");
		html.append("</div>");
		return html.toString();
	}

	private static String buildRadarSvg(String[] labels, double[] percent) {
		double cx = 105.0;
		double cy = 105.0;
		double radius = 72.0;
		StringBuilder html = new StringBuilder();
		html.append("<div style='width:220px;min-width:220px;height:220px;'>");
		html.append("<svg width='220' height='220' viewBox='0 0 220 220' xmlns='http://www.w3.org/2000/svg'>");
		for (int level = 1; level <= 4; level++) {
			double r = radius * level / 4.0;
			html.append("<polygon points='").append(points(labels.length, cx, cy, r, null)).append("' ")
					.append("fill='none' stroke='#e2e8f0' stroke-width='1'/>");
		}
		for (int i = 0; i < labels.length; i++) {
			double[] p = point(i, labels.length, cx, cy, radius);
			html.append("<line x1='").append(round(cx)).append("' y1='").append(round(cy)).append("' x2='")
					.append(round(p[0])).append("' y2='").append(round(p[1]))
					.append("' stroke='#e2e8f0' stroke-width='1'/>");
		}
		html.append("<polygon points='").append(points(labels.length, cx, cy, radius, percent)).append("' ")
				.append("fill='#2563eb' fill-opacity='0.20' stroke='#2563eb' stroke-width='2'/>");
		for (int i = 0; i < labels.length; i++) {
			double[] p = point(i, labels.length, cx, cy, radius + 18.0);
			html.append("<text x='").append(round(p[0])).append("' y='").append(round(p[1]))
					.append("' text-anchor='middle' dominant-baseline='middle' font-size='9' fill='#334155'>")
					.append(escape(labels[i])).append("</text>");
		}
		html.append("</svg>");
		html.append("</div>");
		return html.toString();
	}

	private static String points(int total, double cx, double cy, double radius, double[] percent) {
		StringBuilder points = new StringBuilder();
		for (int i = 0; i < total; i++) {
			double r = radius;
			if (percent != null && percent.length > i) {
				r = radius * clamp(percent[i]) / 100.0;
			}
			double[] p = point(i, total, cx, cy, r);
			if (i > 0) {
				points.append(" ");
			}
			points.append(round(p[0])).append(",").append(round(p[1]));
		}
		return points.toString();
	}

	private static double[] point(int index, int total, double cx, double cy, double radius) {
		double angle = -Math.PI / 2.0 + (2.0 * Math.PI * index / total);
		return new double[] { cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius };
	}

	private static void appendLegendRow(StringBuilder html, String label, double value, double percent, boolean transaksi) {
		html.append("<div style='margin-bottom:8px;'>");
		html.append("<div style='display:flex;justify-content:space-between;gap:8px;font-size:10px;color:#334155;'>");
		html.append("<span style='font-weight:700;'>").append(escape(label)).append("</span>");
		if (transaksi) {
			html.append("<span>").append(format(value)).append(" transaksi</span>");
		} else {
			html.append("<span>Rp ").append(format(value)).append("</span>");
		}
		html.append("</div>");
		html.append("<div style='height:8px;background:#f1f5f9;border-radius:999px;overflow:hidden;margin-top:3px;'>");
		html.append("<div style='height:8px;width:").append(round(clamp(percent)))
				.append("%;background:#2563eb;border-radius:999px;'></div>");
		html.append("</div>");
		html.append("</div>");
	}

	private static double maxAbs(double a, double b, double c, double d, double e) {
		double max = Math.abs(a);
		max = Math.max(max, Math.abs(b));
		max = Math.max(max, Math.abs(c));
		max = Math.max(max, Math.abs(d));
		max = Math.max(max, Math.abs(e));
		return max <= 0.0 ? 1.0 : max;
	}

	private static double persen(double value, double total) {
		if (total <= 0.0) {
			return 0.0;
		}
		return clamp(Math.abs(value) * 100.0 / Math.abs(total));
	}

	private static double persenJumlahTransaksi(double jumlahTransaksi) {
		if (jumlahTransaksi <= 0.0) {
			return 0.0;
		}
		return clamp(jumlahTransaksi * 10.0);
	}

	private static double clamp(double value) {
		if (value < 0.0) {
			return 0.0;
		}
		return value > 100.0 ? 100.0 : value;
	}

	private static String round(double value) {
		return NUMBER_FORMAT.get().format(value).replace(',', '.');
	}

	private static String format(double value) {
		return NUMBER_FORMAT.get().format(value);
	}

	private static String escape(String value) {
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
}
