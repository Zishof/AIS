package ais.action.master.obe;

import ais.common.CommonDashboardHtmlHelper;

/**
 * <h3>PikobeChartHelper — pembangun visual (radar/spider, donut, batang, tren) berbasis
 * HTML &amp; CSS/SVG modern, TANPA JFreeChart</h3>
 *
 * <p>Semua method mengembalikan potongan HTML murni (SVG/CSS inline) sehingga ringan,
 * responsif, dan bisa langsung ditaruh di komponen {@code org.zkoss.zul.Html}. Dipakai
 * bersama oleh halaman Kesiapan OBE (dan bisa dipakai dasbor lain) agar gaya grafik konsisten
 * dan mudah dirawat (reuse maksimal). Tidak ada ketergantungan ke pustaka chart eksternal.</p>
 *
 * <p>Gaya Java 1.6/1.7 (tanpa lambda/stream/diamond).</p>
 */
public final class PikobeChartHelper {

	private PikobeChartHelper() {
	}

	/** Palet warna lembut & modern dipakai bergiliran. */
	public static final String[] PALET = { "#2563eb", "#16a34a", "#f59e0b", "#db2777", "#0891b2", "#7c3aed",
			"#dc2626", "#0d9488", "#ca8a04", "#4f46e5" };

	private static String esc(String v) {
		return CommonDashboardHtmlHelper.escape(v);
	}

	private static String fmt(double v) {
		if (v == Math.floor(v) && !Double.isInfinite(v)) {
			return String.valueOf((long) v);
		}
		return String.valueOf(Math.round(v * 10.0) / 10.0);
	}

	private static String fmtInt(double v) {
		return String.valueOf(Math.round(v));
	}

	private static String kosong(String pesan) {
		return "<div style='color:#94a3b8;font-size:12px;text-align:center;padding:24px 0;'>"
				+ esc(pesan) + "</div>";
	}

	// ════════════════════════════════════════════════════════════════════════
	// RADAR / SPIDER WEB
	// ════════════════════════════════════════════════════════════════════════

	/**
	 * Grafik jaring laba-laba (radar). Cocok untuk melihat seberapa merata sebuah ukuran
	 * tersebar di banyak sumbu (mis. berapa mata kuliah menopang tiap CPL).
	 */
	public static String radar(String[] sumbu, double[] nilai, double nilaiMaks) {
		int n = sumbu == null ? 0 : sumbu.length;
		if (n < 3) {
			return kosong("Data belum cukup untuk grafik jaring (minimal 3 item).");
		}
		double max = nilaiMaks <= 0 ? 1 : nilaiMaks;
		int cx = 160, cy = 160, R = 110, rings = 4;
		StringBuilder s = new StringBuilder();
		s.append("<svg viewBox='0 0 320 320' style='width:100%;max-width:360px;height:auto;display:block;margin:0 auto;'>");
		for (int g = 1; g <= rings; g++) {
			double rr = R * g / (double) rings;
			s.append("<circle cx='160' cy='160' r='").append(fmt(rr))
					.append("' fill='none' stroke='#e2e8f0' stroke-width='1'/>");
		}
		StringBuilder poly = new StringBuilder();
		StringBuilder titik = new StringBuilder();
		for (int i = 0; i < n; i++) {
			double ang = -Math.PI / 2 + i * 2 * Math.PI / n;
			double ax = cx + R * Math.cos(ang), ay = cy + R * Math.sin(ang);
			s.append("<line x1='160' y1='160' x2='").append(fmt(ax)).append("' y2='").append(fmt(ay))
					.append("' stroke='#e2e8f0' stroke-width='1'/>");
			double v = i < nilai.length ? nilai[i] : 0;
			if (v < 0) {
				v = 0;
			}
			if (v > max) {
				v = max;
			}
			double pr = R * v / max;
			double px = cx + pr * Math.cos(ang), py = cy + pr * Math.sin(ang);
			poly.append(fmt(px)).append(",").append(fmt(py)).append(" ");
			titik.append("<circle cx='").append(fmt(px)).append("' cy='").append(fmt(py))
					.append("' r='3' fill='#2563eb'/>");
			double lx = cx + (R + 16) * Math.cos(ang), ly = cy + (R + 16) * Math.sin(ang);
			String anchor = lx < cx - 6 ? "end" : (lx > cx + 6 ? "start" : "middle");
			s.append("<text x='").append(fmt(lx)).append("' y='").append(fmt(ly + 3))
					.append("' font-size='9' fill='#475569' text-anchor='").append(anchor).append("'>")
					.append(esc(potong(sumbu[i], 12))).append("</text>");
		}
		s.append("<polygon points='").append(poly.toString().trim())
				.append("' fill='rgba(37,99,235,.18)' stroke='#2563eb' stroke-width='2' stroke-linejoin='round'/>");
		s.append(titik);
		s.append("</svg>");
		return s.toString();
	}

	// ════════════════════════════════════════════════════════════════════════
	// DONUT
	// ════════════════════════════════════════════════════════════════════════

	/** Grafik donat untuk menampilkan pembagian/komposisi. */
	public static String donut(String[] label, double[] nilai) {
		double total = 0;
		for (int i = 0; i < nilai.length; i++) {
			total += nilai[i];
		}
		if (total <= 0) {
			return kosong("Belum ada data untuk ditampilkan.");
		}
		int r = 70;
		double C = 2 * Math.PI * r;
		StringBuilder s = new StringBuilder();
		s.append("<div style='display:flex;align-items:center;gap:18px;flex-wrap:wrap;justify-content:center;'>");
		s.append("<svg viewBox='0 0 200 200' style='width:160px;height:160px;flex:0 0 auto;'>");
		s.append("<circle cx='100' cy='100' r='70' fill='none' stroke='#eef2f7' stroke-width='26'/>");
		double offset = 0;
		for (int i = 0; i < nilai.length; i++) {
			double len = nilai[i] / total * C;
			String col = PALET[i % PALET.length];
			s.append("<circle cx='100' cy='100' r='70' fill='none' stroke='").append(col)
					.append("' stroke-width='26' stroke-dasharray='").append(fmt(len)).append(" ")
					.append(fmt(C - len)).append("' stroke-dashoffset='").append(fmt(-offset))
					.append("' transform='rotate(-90 100 100)'/>");
			offset += len;
		}
		s.append("<text x='100' y='95' text-anchor='middle' font-size='22' font-weight='800' fill='#0f172a'>")
				.append(fmtInt(total)).append("</text>");
		s.append("<text x='100' y='114' text-anchor='middle' font-size='10' fill='#94a3b8'>Total</text>");
		s.append("</svg>");
		s.append("<div style='font-size:12px;min-width:150px;'>");
		for (int i = 0; i < label.length; i++) {
			double v = i < nilai.length ? nilai[i] : 0;
			int pct = (int) Math.round(v * 100 / total);
			s.append("<div style='display:flex;align-items:center;gap:7px;margin:4px 0;'>")
					.append("<span style='width:11px;height:11px;border-radius:3px;background:")
					.append(PALET[i % PALET.length]).append(";display:inline-block;flex:0 0 auto;'></span>")
					.append("<span style='color:#334155;'>").append(esc(label[i])).append("</span>")
					.append("<b style='margin-left:auto;color:#0f172a;'>").append(fmtInt(v)).append(" (")
					.append(pct).append("%)</b></div>");
		}
		s.append("</div></div>");
		return s.toString();
	}

	// ════════════════════════════════════════════════════════════════════════
	// BATANG HORIZONTAL
	// ════════════════════════════════════════════════════════════════════════

	/** Grafik batang horizontal sederhana untuk membandingkan beberapa angka. */
	public static String bars(String[] label, double[] nilai, String warna) {
		if (label == null || label.length == 0) {
			return kosong("Belum ada data.");
		}
		double max = 0;
		for (int i = 0; i < nilai.length; i++) {
			if (nilai[i] > max) {
				max = nilai[i];
			}
		}
		if (max <= 0) {
			max = 1;
		}
		String col = (warna == null || warna.length() == 0) ? "#2563eb" : warna;
		StringBuilder s = new StringBuilder("<div style='display:flex;flex-direction:column;gap:9px;'>");
		for (int i = 0; i < label.length; i++) {
			double v = i < nilai.length ? nilai[i] : 0;
			int pct = (int) Math.round(v * 100 / max);
			if (pct < 2 && v > 0) {
				pct = 2;
			}
			s.append("<div><div style='display:flex;justify-content:space-between;font-size:11px;color:#475569;margin-bottom:3px;'>")
					.append("<span>").append(esc(label[i])).append("</span><b style='color:#0f172a;'>")
					.append(fmtInt(v)).append("</b></div>")
					.append("<div style='background:#eef2f7;border-radius:7px;height:13px;overflow:hidden;'>")
					.append("<div style='height:100%;width:").append(pct)
					.append("%;background:linear-gradient(90deg,").append(col).append(",")
					.append(terangkan(col)).append(");border-radius:7px;'></div></div></div>");
		}
		s.append("</div>");
		return s.toString();
	}

	// ════════════════════════════════════════════════════════════════════════
	// TREN (GARIS + AREA)
	// ════════════════════════════════════════════════════════════════════════

	/** Grafik garis (tren) untuk melihat naik/turun sebuah angka antar titik. */
	public static String trend(String[] label, double[] nilai) {
		int n = nilai == null ? 0 : nilai.length;
		if (n < 2) {
			return kosong("Data belum cukup untuk grafik tren (minimal 2 titik).");
		}
		double max = -Double.MAX_VALUE, min = Double.MAX_VALUE;
		for (int i = 0; i < n; i++) {
			if (nilai[i] > max) {
				max = nilai[i];
			}
			if (nilai[i] < min) {
				min = nilai[i];
			}
		}
		if (max <= min) {
			max = min + 1;
		}
		int W = 340, H = 140, pad = 24;
		double dx = (W - 2 * pad) / (double) (n - 1);
		StringBuilder line = new StringBuilder();
		StringBuilder area = new StringBuilder();
		StringBuilder dots = new StringBuilder();
		StringBuilder labels = new StringBuilder();
		area.append(fmt(pad)).append(",").append(fmt(H - pad)).append(" ");
		for (int i = 0; i < n; i++) {
			double x = pad + i * dx;
			double y = H - pad - (nilai[i] - min) / (max - min) * (H - 2 * pad);
			line.append(fmt(x)).append(",").append(fmt(y)).append(" ");
			area.append(fmt(x)).append(",").append(fmt(y)).append(" ");
			dots.append("<circle cx='").append(fmt(x)).append("' cy='").append(fmt(y))
					.append("' r='3' fill='#2563eb'/>");
			if (label != null && i < label.length) {
				labels.append("<text x='").append(fmt(x)).append("' y='").append(H - 6)
						.append("' font-size='8' fill='#94a3b8' text-anchor='middle'>")
						.append(esc(potong(label[i], 8))).append("</text>");
			}
		}
		area.append(fmt(W - pad)).append(",").append(fmt(H - pad));
		StringBuilder s = new StringBuilder();
		s.append("<svg viewBox='0 0 340 140' style='width:100%;max-width:420px;height:auto;'>");
		s.append("<polygon points='").append(area).append("' fill='rgba(37,99,235,.12)'/>");
		s.append("<polyline points='").append(line.toString().trim())
				.append("' fill='none' stroke='#2563eb' stroke-width='2.5' stroke-linejoin='round' stroke-linecap='round'/>");
		s.append(dots).append(labels);
		s.append("</svg>");
		return s.toString();
	}

	// ════════════════════════════════════════════════════════════════════════
	// util
	// ════════════════════════════════════════════════════════════════════════

	private static String potong(String v, int maks) {
		if (v == null) {
			return "";
		}
		v = v.trim();
		return v.length() > maks ? v.substring(0, maks - 1) + "…" : v;
	}

	/** Versi lebih terang dari sebuah warna hex untuk gradien (sederhana). */
	private static String terangkan(String hex) {
		try {
			if (hex == null || hex.length() != 7 || hex.charAt(0) != '#') {
				return hex;
			}
			int r = Integer.parseInt(hex.substring(1, 3), 16);
			int g = Integer.parseInt(hex.substring(3, 5), 16);
			int b = Integer.parseInt(hex.substring(5, 7), 16);
			r = Math.min(255, r + 55);
			g = Math.min(255, g + 55);
			b = Math.min(255, b + 55);
			return String.format("#%02x%02x%02x", Integer.valueOf(r), Integer.valueOf(g), Integer.valueOf(b));
		} catch (Exception e) {
			return hex;
		}
	}
}
