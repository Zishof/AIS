package ais.action.master.koperasi.gudang;

import java.text.DecimalFormat;

/**
 * <h2>GudangChartSvg — pembuat grafik <b>SVG (HTML/CSS)</b> untuk dasbor pergudangan.</h2>
 *
 * <p>
 * Kumpulan metode statik yang menghasilkan potongan <b>SVG</b> siap-tempel (dipasang ke komponen ZK
 * {@code Html} pada dasbor versi ZKoss, atau di mana pun HTML bisa dirender). Tujuannya memenuhi
 * kebutuhan visual modern <b>tanpa JFreeChart</b>: batang (bar), lingkaran (donut), garis tren, dan
 * radar/spider — semuanya murni SVG + CSS sehingga ringan, tajam di layar apa pun (vektor), dan
 * seragam dengan versi JSP. Metode-metode ini <b>bebas status</b> (stateless) dan dapat dipakai ulang
 * lintas dasbor, mendukung pemeliharaan yang mudah. Kompatibel Java 1.7.
 * </p>
 *
 * @author AIS e-Kantin (modul pergudangan)
 */
public final class GudangChartSvg {

	/** Palet warna baku yang selaras dengan tema aplikasi. */
	public static final String[] PALET = { "#0d6efd", "#16a34a", "#f59e0b", "#dc2626", "#7c3aed",
			"#0891b2", "#db2777", "#65a30d", "#ea580c", "#334155" };

	private static final ThreadLocal<DecimalFormat> NF = new ThreadLocal<DecimalFormat>() {
		@Override
		protected DecimalFormat initialValue() {
			return new DecimalFormat("#,##0");
		}
	};

	private GudangChartSvg() {
	}

	private static String esc(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String warna(String[] colors, int i) {
		if (colors != null && i < colors.length && colors[i] != null && colors[i].length() > 0) {
			return colors[i];
		}
		return PALET[i % PALET.length];
	}

	/**
	 * Grafik batang horizontal (mis. nilai stok per lokasi).
	 *
	 * @param labels label tiap batang.
	 * @param values nilai tiap batang.
	 * @param colors warna tiap batang (boleh null → palet).
	 * @return potongan SVG.
	 */
	public static String barHorizontal(String[] labels, double[] values, String[] colors) {
		int n = labels == null ? 0 : labels.length;
		if (n == 0) {
			return "<div style='color:#94a3b8;font-size:12px;padding:10px'>Tidak ada data.</div>";
		}
		double max = 1;
		for (int i = 0; i < n; i++) {
			if (values[i] > max) {
				max = values[i];
			}
		}
		int W = 440, rowH = 26, H = n * rowH + 6, lblW = 130, barW = W - lblW - 70;
		StringBuilder s = new StringBuilder();
		s.append("<svg viewBox='0 0 ").append(W).append(" ").append(H).append("' width='100%' style='max-height:")
				.append(H + 4).append("px'>");
		for (int i = 0; i < n; i++) {
			int y = i * rowH + 4;
			double w = Math.max(2, barW * values[i] / max);
			String lab = labels[i] == null ? "" : (labels[i].length() > 18 ? labels[i].substring(0, 17) + "…" : labels[i]);
			s.append("<text x='0' y='").append(y + 15).append("' font-size='11' fill='#334155'>").append(esc(lab))
					.append("</text>");
			s.append("<rect x='").append(lblW).append("' y='").append(y).append("' width='").append((long) w)
					.append("' height='16' rx='4' fill='").append(warna(colors, i)).append("'></rect>");
			s.append("<text x='").append(lblW + (long) w + 5).append("' y='").append(y + 13)
					.append("' font-size='10.5' fill='#64748b'>").append(NF.get().format(values[i])).append("</text>");
		}
		return s.append("</svg>").toString();
	}

	/**
	 * Grafik donut + legenda (mis. komposisi nilai per jenis lokasi).
	 *
	 * @param labels label tiap irisan.
	 * @param values nilai tiap irisan.
	 * @param colors warna tiap irisan (boleh null → palet).
	 * @return potongan SVG + legenda HTML.
	 */
	public static String donut(String[] labels, double[] values, String[] colors) {
		int n = labels == null ? 0 : labels.length;
		double tot = 0;
		for (int i = 0; i < n; i++) {
			tot += values[i];
		}
		if (tot <= 0) {
			return "<div style='color:#94a3b8;font-size:12px;padding:10px'>Tidak ada data.</div>";
		}
		double cx = 90, cy = 90, r = 70, rin = 42, a = -Math.PI / 2;
		StringBuilder s = new StringBuilder("<svg viewBox='0 0 300 180' width='100%' style='max-height:190px'>");
		for (int i = 0; i < n; i++) {
			double frac = values[i] / tot, a2 = a + frac * 2 * Math.PI;
			double x1 = cx + r * Math.cos(a), y1 = cy + r * Math.sin(a), x2 = cx + r * Math.cos(a2), y2 = cy + r * Math.sin(a2);
			double xi2 = cx + rin * Math.cos(a2), yi2 = cy + rin * Math.sin(a2), xi1 = cx + rin * Math.cos(a), yi1 = cy + rin * Math.sin(a);
			int big = frac > 0.5 ? 1 : 0;
			s.append("<path d='M").append(x1).append(" ").append(y1).append(" A").append(r).append(" ").append(r)
					.append(" 0 ").append(big).append(" 1 ").append(x2).append(" ").append(y2).append(" L").append(xi2)
					.append(" ").append(yi2).append(" A").append(rin).append(" ").append(rin).append(" 0 ").append(big)
					.append(" 0 ").append(xi1).append(" ").append(yi1).append(" Z' fill='").append(warna(colors, i))
					.append("'></path>");
			a = a2;
		}
		s.append("<text x='").append(cx).append("' y='").append(cy - 2)
				.append("' text-anchor='middle' font-size='12' font-weight='700' fill='#0f172a'>").append(NF.get().format(tot))
				.append("</text><text x='").append(cx).append("' y='").append(cy + 14)
				.append("' text-anchor='middle' font-size='9' fill='#64748b'>Total</text></svg>");
		StringBuilder lg = new StringBuilder(
				"<div style='display:flex;flex-wrap:wrap;gap:4px 12px;font-size:11px;color:#475569;margin-top:6px'>");
		for (int i = 0; i < n; i++) {
			lg.append("<span><i style='display:inline-block;width:10px;height:10px;border-radius:3px;margin-right:4px;background:")
					.append(warna(colors, i)).append("'></i>").append(esc(labels[i])).append(" (")
					.append(Math.round(values[i] / tot * 100)).append("%)</span>");
		}
		return s.append(lg).append("</div>").toString();
	}

	/**
	 * Grafik garis tren masuk vs keluar per tanggal.
	 *
	 * @param tgl    label tanggal.
	 * @param masuk  nilai masuk.
	 * @param keluar nilai keluar.
	 * @return potongan SVG.
	 */
	public static String trend(String[] tgl, double[] masuk, double[] keluar) {
		int n = tgl == null ? 0 : tgl.length;
		if (n == 0) {
			return "<div style='color:#94a3b8;font-size:12px;padding:10px'>Belum ada mutasi.</div>";
		}
		int W = 440, H = 170, pad = 26;
		double max = 1;
		for (int i = 0; i < n; i++) {
			max = Math.max(max, Math.max(masuk[i], keluar[i]));
		}
		double xw = (double) (W - pad * 2) / Math.max(1, n - 1);
		StringBuilder pM = new StringBuilder(), pK = new StringBuilder();
		for (int i = 0; i < n; i++) {
			double x = pad + i * xw;
			pM.append(x).append(",").append(H - pad - (H - pad * 2) * masuk[i] / max).append(" ");
			pK.append(x).append(",").append(H - pad - (H - pad * 2) * keluar[i] / max).append(" ");
		}
		StringBuilder s = new StringBuilder("<svg viewBox='0 0 ").append(W).append(" ").append(H)
				.append("' width='100%' style='max-height:180px'>");
		s.append("<line x1='").append(pad).append("' y1='").append(H - pad).append("' x2='").append(W - pad)
				.append("' y2='").append(H - pad).append("' stroke='#e2e8f0'></line>");
		s.append("<polyline fill='none' stroke='#16a34a' stroke-width='2' points='").append(pM).append("'></polyline>");
		s.append("<polyline fill='none' stroke='#dc2626' stroke-width='2' points='").append(pK).append("'></polyline>");
		s.append("<text x='").append(pad).append("' y='14' font-size='10' fill='#16a34a'>Masuk</text>");
		s.append("<text x='").append(pad + 55).append("' y='14' font-size='10' fill='#dc2626'>Keluar</text>");
		s.append("<text x='").append(pad).append("' y='").append(H - 8).append("' font-size='9' fill='#94a3b8'>")
				.append(esc(tgl[0])).append("</text>");
		s.append("<text x='").append(W - pad).append("' y='").append(H - 8)
				.append("' font-size='9' fill='#94a3b8' text-anchor='end'>").append(esc(tgl[n - 1])).append("</text>");
		return s.append("</svg>").toString();
	}

	/**
	 * Grafik radar/spider membandingkan beberapa lokasi pada beberapa metrik (nilai ternormalisasi 0..1).
	 *
	 * @param axes   nama sumbu (metrik).
	 * @param names  nama tiap seri (lokasi).
	 * @param values nilai ternormalisasi [seri][sumbu] rentang 0..1.
	 * @param colors warna tiap seri.
	 * @return potongan SVG + legenda.
	 */
	public static String radar(String[] axes, String[] names, double[][] values, String[] colors) {
		int n = axes == null ? 0 : axes.length, m = names == null ? 0 : names.length;
		if (n == 0 || m == 0) {
			return "<div style='color:#94a3b8;font-size:12px;padding:10px'>Tidak ada data.</div>";
		}
		double cx = 110, cy = 100, r = 80;
		StringBuilder s = new StringBuilder("<svg viewBox='0 0 300 200' width='100%' style='max-height:210px'>");
		for (int g = 1; g <= 3; g++) {
			StringBuilder pp = new StringBuilder();
			for (int i = 0; i < n; i++) {
				double a = -Math.PI / 2 + i * 2 * Math.PI / n;
				pp.append(cx + r * g / 3 * Math.cos(a)).append(",").append(cy + r * g / 3 * Math.sin(a)).append(" ");
			}
			s.append("<polygon points='").append(pp).append("' fill='none' stroke='#e2e8f0'></polygon>");
		}
		for (int i = 0; i < n; i++) {
			double a = -Math.PI / 2 + i * 2 * Math.PI / n;
			s.append("<text x='").append(cx + (r + 14) * Math.cos(a)).append("' y='").append(cy + (r + 14) * Math.sin(a))
					.append("' font-size='9' fill='#64748b' text-anchor='middle'>").append(esc(axes[i])).append("</text>");
		}
		for (int k = 0; k < m; k++) {
			StringBuilder pp = new StringBuilder();
			for (int i = 0; i < n; i++) {
				double a = -Math.PI / 2 + i * 2 * Math.PI / n;
				double v = Math.max(0, Math.min(1, values[k][i]));
				pp.append(cx + r * v * Math.cos(a)).append(",").append(cy + r * v * Math.sin(a)).append(" ");
			}
			String col = warna(colors, k);
			s.append("<polygon points='").append(pp).append("' fill='").append(col).append("22' stroke='").append(col)
					.append("' stroke-width='1.5'></polygon>");
		}
		s.append("</svg>");
		StringBuilder lg = new StringBuilder(
				"<div style='display:flex;flex-wrap:wrap;gap:4px 12px;font-size:11px;color:#475569;margin-top:6px'>");
		for (int k = 0; k < m; k++) {
			lg.append("<span><i style='display:inline-block;width:10px;height:10px;border-radius:3px;margin-right:4px;background:")
					.append(warna(colors, k)).append("'></i>").append(esc(names[k])).append("</span>");
		}
		return s.append(lg).append("</div>").toString();
	}
}
