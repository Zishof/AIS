package ais.action.master.dashboard.admin;

import java.util.List;

import ais.common.Common;

/**
 * Helper ringan untuk membuat grafik dashboard akademik berbasis HTML/CSS.
 * Tidak memakai komponen grafik berat sehingga lebih ringan dan mudah dirender di ZKoss lama.
 * Kompatibel Java 1.7 / source 1.6.
 */
public final class DashboardAkademikHtmlCssHelper {

	private DashboardAkademikHtmlCssHelper() {
	}

	public static final class BarItem {
		private String label;
		private String subLabel;
		private int value;

		public BarItem(String label, String subLabel, int value) {
			this.label = label;
			this.subLabel = subLabel;
			this.value = value;
		}

		public String getLabel() {
			return label;
		}

		public String getSubLabel() {
			return subLabel;
		}

		public int getValue() {
			return value;
		}
	}

	public static BarItem item(String label, String subLabel, int value) {
		return new BarItem(label, subLabel, value);
	}

	public static String css() {
		return "<style type=\"text/css\">"
				+ ".ais-akad-card{font-family:Arial,Helvetica,sans-serif;background:#fff;border:1px solid #e2e8f0;border-radius:14px;box-shadow:0 8px 22px rgba(15,23,42,.06);margin:8px 0 12px 0;overflow:hidden;}"
				+ ".ais-akad-head{padding:13px 15px;background:linear-gradient(135deg, rgba(var(--ais-theme-primary-rgb,29,78,216),.10) 0%, #f8fafc 55%, rgba(var(--ais-theme-primary-rgb,29,78,216),.05) 100%);border-bottom:1px solid #e2e8f0;}"
				+ ".ais-akad-title{font-weight:800;font-size:14px;color:#0f172a;line-height:1.35;}"
				+ ".ais-akad-desc{font-size:11px;color:#475569;line-height:1.45;margin-top:4px;}"
				+ ".ais-akad-body{padding:12px;}"
				+ ".ais-akad-empty{padding:16px;color:#64748b;font-size:11px;text-align:center;border:1px dashed #cbd5e1;border-radius:12px;background:#f8fafc;}"
				+ ".ais-akad-stats{display:flex;flex-wrap:wrap;gap:10px;}"
				+ ".ais-akad-stat{flex:1 1 120px;min-width:110px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:10px;}"
				+ ".ais-akad-stat .l{font-size:10px;color:#64748b;text-transform:uppercase;letter-spacing:.04em;font-weight:700;}"
				+ ".ais-akad-stat .v{font-size:19px;color:#0f172a;font-weight:800;margin-top:5px;}"
				+ ".ais-akad-stat .h{font-size:10.5px;color:#64748b;margin-top:4px;line-height:1.35;}"
				+ ".ais-akad-list{display:block;border:1px solid #e2e8f0;border-radius:12px;background:#fff;overflow:hidden;}"
				+ ".ais-akad-rowitem{padding:10px;border-bottom:1px solid #f1f5f9;}"
				+ ".ais-akad-rowitem:last-child{border-bottom:none;}"
				+ ".ais-akad-rowtitle{font-size:11px;font-weight:800;color:#0f172a;line-height:1.35;margin-bottom:6px;}"
				+ ".ais-akad-rowmeta{font-size:9.5px;color:#64748b;margin-top:2px;line-height:1.35;}"
				+ ".ais-akad-line{display:flex;align-items:center;gap:6px;margin:5px 0;}"
				+ ".ais-akad-lname{width:58px;min-width:58px;font-size:9.5px;color:#64748b;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}"
				+ ".ais-akad-track{height:8px;flex:1;background:#f1f5f9;border-radius:999px;overflow:hidden;}"
				+ ".ais-akad-fill{display:block;height:8px;border-radius:999px;}"
				+ ".ais-akad-lval{width:42px;min-width:42px;text-align:right;font-size:10px;font-weight:800;color:#0f172a;}"
				+ ".ais-akad-fill0{background:linear-gradient(90deg,#bfdbfe,#2563eb);}"
				+ ".ais-akad-fill1{background:linear-gradient(90deg,#bbf7d0,#059669);}"
				+ ".ais-akad-fill2{background:linear-gradient(90deg,#fed7aa,#f97316);}"
				+ ".ais-akad-fill3{background:linear-gradient(90deg,#ddd6fe,#7c3aed);}"
				+ ".ais-akad-fill4{background:linear-gradient(90deg,#fecdd3,#e11d48);}"
				+ ".ais-akad-legend{display:flex;flex-wrap:wrap;gap:9px;margin-top:10px;font-size:10.5px;color:#475569;}"
				+ ".ais-akad-dot{display:inline-block;width:9px;height:9px;border-radius:50%;margin-right:4px;vertical-align:-1px;}"
				+ ".ais-akad-stat-click{cursor:pointer;transition:box-shadow .15s ease,transform .15s ease,border-color .15s ease;}"
				+ ".ais-akad-stat-click:hover{box-shadow:0 8px 18px rgba(15,23,42,.12);transform:translateY(-2px);border-color:rgba(var(--ais-theme-primary-rgb,29,78,216),.5);}"
				+ ".ais-akad-click-hint{font-size:9.5px;color:rgba(var(--ais-theme-primary-rgb,29,78,216),1);font-weight:800;margin-top:5px;}"
				+ ".ais-akad-modal{display:none;position:fixed;left:0;top:0;right:0;bottom:0;background:rgba(15,23,42,.55);z-index:99999;align-items:center;justify-content:center;padding:16px;box-sizing:border-box;}"
				+ ".ais-akad-modal-box{background:#fff;border-radius:14px;max-width:520px;width:100%;max-height:82vh;overflow:auto;box-shadow:0 24px 64px rgba(0,0,0,.32);}"
				+ ".ais-akad-modal-head{padding:13px 16px;font-weight:800;font-size:14px;color:#0f172a;border-bottom:1px solid #e2e8f0;display:flex;justify-content:space-between;align-items:center;gap:10px;background:linear-gradient(135deg,rgba(var(--ais-theme-primary-rgb,29,78,216),.10),#f8fafc);}"
				+ ".ais-akad-modal-x{cursor:pointer;font-size:22px;line-height:1;color:#64748b;padding:0 4px;}"
				+ ".ais-akad-modal-x:hover{color:#0f172a;}"
				+ ".ais-akad-modal-body{padding:15px 16px;font-size:12px;color:#334155;line-height:1.6;}"
				+ ".ais-akad-modal-body p{margin:0 0 9px 0;}"
				+ ".ais-akad-formula{background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:11px 13px;margin:9px 0;font-size:12.5px;color:#0f172a;line-height:1.7;}"
				+ ".ais-akad-mtbl{width:100%;border-collapse:collapse;font-size:12px;margin:6px 0;}"
				+ ".ais-akad-mtbl th,.ais-akad-mtbl td{border:1px solid #e2e8f0;padding:6px 9px;text-align:left;}"
				+ ".ais-akad-mtbl thead th{background:#f1f5f9;color:#0f172a;font-weight:800;position:sticky;top:0;}"
				+ ".ais-akad-mtbl td:last-child,.ais-akad-mtbl th:last-child{text-align:right;width:96px;font-weight:700;white-space:nowrap;}"
				+ ".ais-akad-mtbl tfoot td{background:#f8fafc;font-weight:800;color:#0f172a;}"
				+ ".ais-akad-mtbl tbody tr:hover td{background:rgba(var(--ais-theme-primary-rgb,29,78,216),.06);}"
				+ "@media screen and (max-width:360px){"
				+ ".ais-akad-body{padding:10px;}"
				+ ".ais-akad-rowitem{padding:9px;}"
				+ ".ais-akad-lname{width:54px;min-width:54px;}"
				+ ".ais-akad-lval{width:40px;min-width:40px;}"
				+ ".ais-akad-stat{min-width:100px;}"
				+ "}"
				+ "</style>";
	}

	public static String panel(String title, String description, String body) {
		StringBuffer sb = new StringBuffer();
		sb.append(css());
		sb.append("<div class=\"ais-akad-card\">");
		sb.append("<div class=\"ais-akad-head\"><div class=\"ais-akad-title\">").append(esc(title)).append("</div>");
		if (description != null && description.trim().length() > 0) {
			sb.append("<div class=\"ais-akad-desc\">").append(esc(description)).append("</div>");
		}
		sb.append("</div><div class=\"ais-akad-body\">");
		sb.append(body == null ? "" : body);
		sb.append("</div></div>");
		return sb.toString();
	}

	public static String stat(String label, String value, String hint) {
		StringBuffer sb = new StringBuffer();
		sb.append("<div class=\"ais-akad-stat\"><div class=\"l\">").append(esc(label)).append("</div>");
		sb.append("<div class=\"v\">").append(esc(value)).append("</div>");
		if (hint != null && hint.trim().length() > 0) {
			sb.append("<div class=\"h\">").append(esc(hint)).append("</div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * Sama seperti {@link #stat(String, String, String)} tetapi kartu bisa
	 * di-klik untuk membuka popup penjelasan (modal dengan id {@code modalId}).
	 * Murni HTML/CSS/JS (tanpa round-trip server) agar ringan di dalam Html ZK.
	 */
	public static String statClickable(String label, String value, String hint, String modalId) {
		StringBuffer sb = new StringBuffer();
		sb.append("<div class=\"ais-akad-stat ais-akad-stat-click\" onclick=\"var m=document.getElementById('")
				.append(esc(modalId)).append("');if(m){m.style.display='flex';}\">");
		sb.append("<div class=\"l\">").append(esc(label)).append("</div>");
		sb.append("<div class=\"v\">").append(esc(value)).append("</div>");
		if (hint != null && hint.trim().length() > 0) {
			sb.append("<div class=\"h\">").append(esc(hint)).append("</div>");
		}
		sb.append("<div class=\"ais-akad-click-hint\">&#128712; Klik untuk lihat cara hitung</div>");
		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * Popup/overlay penjelasan. {@code bodyHtml} sudah berupa HTML (tidak di-esc)
	 * agar pemanggil bisa menyusun penjelasan terformat. Klik latar / tombol ×
	 * menutup popup.
	 */
	public static String modal(String modalId, String title, String bodyHtml) {
		StringBuffer sb = new StringBuffer();
		sb.append("<div id=\"").append(esc(modalId))
				.append("\" class=\"ais-akad-modal\" onclick=\"if(event.target===this){this.style.display='none';}\">");
		sb.append("<div class=\"ais-akad-modal-box\">");
		sb.append("<div class=\"ais-akad-modal-head\"><span>").append(esc(title)).append("</span>");
		sb.append("<span class=\"ais-akad-modal-x\" onclick=\"var m=document.getElementById('").append(esc(modalId))
				.append("');if(m){m.style.display='none';}\">&times;</span></div>");
		sb.append("<div class=\"ais-akad-modal-body\">").append(bodyHtml == null ? "" : bodyHtml).append("</div>");
		sb.append("</div></div>");
		return sb.toString();
	}

	public static String verticalBarChart(String title, String description, List<BarItem> items) {
		if (items == null || items.isEmpty()) {
			return panel(title, description, "<div class=\"ais-akad-empty\">Belum ada data yang dapat ditampilkan.</div>");
		}

		int max = 0;
		for (int i = 0; i < items.size(); i++) {
			BarItem item = items.get(i);
			if (item != null && item.getValue() > max) {
				max = item.getValue();
			}
		}
		if (max <= 0) {
			max = 1;
		}

		StringBuffer body = new StringBuffer();
		body.append("<div class=\"ais-akad-list\">");
		for (int i = 0; i < items.size(); i++) {
			BarItem item = items.get(i);
			if (item == null) {
				continue;
			}
			int value = item.getValue();
			int persen = Math.max(3, (int) Math.round((value * 100.0d) / max));
			body.append("<div class=\"ais-akad-rowitem\">");
			body.append("<div class=\"ais-akad-rowtitle\" title=\"").append(escAttr(item.getLabel())).append("\">")
					.append(esc(shortText(item.getLabel(), 48))).append("</div>");
			body.append("<div class=\"ais-akad-line\"><span class=\"ais-akad-lname\">Jumlah</span>");
			body.append("<span class=\"ais-akad-track\"><span class=\"ais-akad-fill ais-akad-fill0\" style=\"width:")
					.append(persen).append("%\"></span></span>");
			body.append("<span class=\"ais-akad-lval\">").append(fmt(value)).append("</span></div>");
			if (item.getSubLabel() != null && item.getSubLabel().trim().length() > 0) {
				body.append("<div class=\"ais-akad-rowmeta\">").append(esc(item.getSubLabel())).append("</div>");
			}
			body.append("</div>");
		}
		body.append("</div>");
		return panel(title, description, body.toString());
	}

	public static String groupedBarChart(String title, String description, List<String> labels, String[] seriesNames,
			List<int[]> values) {
		if (labels == null || labels.isEmpty() || seriesNames == null || values == null || values.isEmpty()) {
			return panel(title, description, "<div class=\"ais-akad-empty\">Belum ada data yang dapat ditampilkan.</div>");
		}

		int max = 0;
		for (int i = 0; i < values.size(); i++) {
			int[] row = values.get(i);
			if (row == null) {
				continue;
			}
			for (int j = 0; j < row.length; j++) {
				if (row[j] > max) {
					max = row[j];
				}
			}
		}
		if (max <= 0) {
			max = 1;
		}

		StringBuffer body = new StringBuffer();
		body.append("<div class=\"ais-akad-list\">");
		for (int i = 0; i < labels.size(); i++) {
			String label = labels.get(i);
			int[] row = i < values.size() ? values.get(i) : null;
			body.append("<div class=\"ais-akad-rowitem\">");
			body.append("<div class=\"ais-akad-rowtitle\" title=\"").append(escAttr(label)).append("\">")
					.append(esc(shortText(label, 56))).append("</div>");
			for (int j = 0; j < seriesNames.length; j++) {
				int value = row != null && j < row.length ? row[j] : 0;
				int persen = Math.max(3, (int) Math.round((value * 100.0d) / max));
				body.append("<div class=\"ais-akad-line\"><span class=\"ais-akad-lname\" title=\"")
						.append(escAttr(seriesNames[j])).append("\">").append(esc(seriesNames[j])).append("</span>");
				body.append("<span class=\"ais-akad-track\"><span class=\"ais-akad-fill ais-akad-fill")
						.append(j % 5).append("\" style=\"width:").append(persen).append("%\"></span></span>");
				body.append("<span class=\"ais-akad-lval\">").append(fmt(value)).append("</span></div>");
			}
			body.append("</div>");
		}
		body.append("</div><div class=\"ais-akad-legend\">");
		for (int j = 0; j < seriesNames.length; j++) {
			body.append("<span><i class=\"ais-akad-dot ais-akad-s").append(j % 5).append("\"></i>")
					.append(esc(seriesNames[j])).append("</span>");
		}
		body.append("</div>");
		return panel(title, description, body.toString());
	}

	/** Palet warna konsisten (dipakai chart spider/trend/funnel). */
	private static String[] palette() {
		// Warna pertama mengikuti tema institusi aktif; sisanya warna pembeda tetap.
		return new String[] { "var(--ais-theme-primary,#2563eb)", "#16a34a", "#f59e0b", "#db2777", "#7c3aed",
				"#0891b2", "#dc2626" };
	}

	private static String legendHtml(List<String> seriesNames, String[] pal) {
		if (seriesNames == null || seriesNames.isEmpty()) {
			return "";
		}
		StringBuffer b = new StringBuffer("<div class=\"ais-akad-legend\">");
		for (int s = 0; s < seriesNames.size(); s++) {
			b.append("<span><i class=\"ais-akad-dot\" style=\"background:").append(pal[s % pal.length])
					.append("\"></i>").append(esc(seriesNames.get(s))).append("</span>");
		}
		return b.append("</div>").toString();
	}

	/**
	 * Diagram SPIDER/RADAR (SVG inline, responsif): membandingkan beberapa "sumbu" (mis. status:
	 * Aktif/Cuti/Lulus/DO/Rasio) untuk satu atau beberapa seri (mis. prodi). Modern, tanpa jfreechart.
	 */
	public static String spiderChart(String title, String description, String[] axes, List<String> seriesNames,
			List<int[]> seriesValues) {
		if (axes == null || axes.length < 3 || seriesValues == null || seriesValues.isEmpty()) {
			return panel(title, description, "<div class=\"ais-akad-empty\">Belum ada data yang dapat ditampilkan.</div>");
		}
		int n = axes.length;
		int max = 1;
		for (int s = 0; s < seriesValues.size(); s++) {
			int[] v = seriesValues.get(s);
			if (v != null) {
				for (int x = 0; x < v.length; x++) {
					if (v[x] > max) {
						max = v[x];
					}
				}
			}
		}
		String[] pal = palette();
		int size = 320, cx = size / 2, cy = size / 2, r = (size / 2) - 48;
		StringBuffer svg = new StringBuffer();
		svg.append("<div style=\"text-align:center\"><svg viewBox=\"0 0 ").append(size).append(" ").append(size)
				.append("\" style=\"width:100%;height:auto;max-width:360px\" preserveAspectRatio=\"xMidYMid meet\">");
		for (int g = 1; g <= 4; g++) {
			StringBuffer pts = new StringBuffer();
			double rr = r * g / 4.0;
			for (int i = 0; i < n; i++) {
				double a = -Math.PI / 2 + 2 * Math.PI * i / n;
				pts.append((int) (cx + rr * Math.cos(a))).append(",").append((int) (cy + rr * Math.sin(a))).append(" ");
			}
			svg.append("<polygon points=\"").append(pts).append("\" fill=\"none\" stroke=\"#e2e8f0\"/>");
		}
		for (int i = 0; i < n; i++) {
			double a = -Math.PI / 2 + 2 * Math.PI * i / n;
			svg.append("<line x1=\"").append(cx).append("\" y1=\"").append(cy).append("\" x2=\"")
					.append((int) (cx + r * Math.cos(a))).append("\" y2=\"").append((int) (cy + r * Math.sin(a)))
					.append("\" stroke=\"#e2e8f0\"/>");
			int tx = (int) (cx + (r + 16) * Math.cos(a)), ty = (int) (cy + (r + 16) * Math.sin(a));
			String anchor = Math.abs(Math.cos(a)) < 0.3 ? "middle" : (Math.cos(a) > 0 ? "start" : "end");
			svg.append("<text x=\"").append(tx).append("\" y=\"").append(ty + 3).append("\" font-size=\"10\" fill=\"#475569\" text-anchor=\"")
					.append(anchor).append("\">").append(esc(shortText(axes[i], 12))).append("</text>");
		}
		for (int s = 0; s < seriesValues.size(); s++) {
			int[] v = seriesValues.get(s);
			if (v == null) {
				continue;
			}
			StringBuffer pts = new StringBuffer();
			for (int i = 0; i < n; i++) {
				int val = i < v.length ? v[i] : 0;
				double rr = r * val / (double) max;
				double a = -Math.PI / 2 + 2 * Math.PI * i / n;
				pts.append((int) (cx + rr * Math.cos(a))).append(",").append((int) (cy + rr * Math.sin(a))).append(" ");
			}
			String c = pal[s % pal.length];
			svg.append("<polygon points=\"").append(pts).append("\" fill=\"").append(c)
					.append("\" fill-opacity=\"0.2\" stroke=\"").append(c).append("\" stroke-width=\"2\"/>");
		}
		svg.append("</svg></div>").append(legendHtml(seriesNames, pal));
		return panel(title, description, svg.toString());
	}

	/**
	 * Grafik TREN GARIS (SVG inline, responsif): perkembangan angka dari waktu ke waktu (mis. jumlah
	 * mahasiswa aktif &amp; lulusan per angkatan/tahun). Modern, tanpa jfreechart.
	 */
	public static String trendLineChart(String title, String description, List<String> xLabels,
			List<String> seriesNames, List<int[]> seriesValues) {
		if (xLabels == null || xLabels.isEmpty() || seriesValues == null || seriesValues.isEmpty()) {
			return panel(title, description, "<div class=\"ais-akad-empty\">Belum ada data yang dapat ditampilkan.</div>");
		}
		int nx = xLabels.size();
		int max = 1;
		for (int s = 0; s < seriesValues.size(); s++) {
			int[] v = seriesValues.get(s);
			if (v != null) {
				for (int x = 0; x < v.length; x++) {
					if (v[x] > max) {
						max = v[x];
					}
				}
			}
		}
		String[] pal = palette();
		int w = 640, h = 260, padL = 42, padB = 28, padT = 12, padR = 12;
		int plotW = w - padL - padR, plotH = h - padT - padB;
		StringBuffer svg = new StringBuffer();
		svg.append("<div style=\"text-align:center\"><svg viewBox=\"0 0 ").append(w).append(" ").append(h)
				.append("\" style=\"width:100%;height:auto;max-width:680px\" preserveAspectRatio=\"xMidYMid meet\">");
		for (int g = 0; g <= 4; g++) {
			int y = padT + plotH - (plotH * g / 4);
			svg.append("<line x1=\"").append(padL).append("\" y1=\"").append(y).append("\" x2=\"").append(w - padR)
					.append("\" y2=\"").append(y).append("\" stroke=\"#eef2f7\"/>");
			svg.append("<text x=\"").append(padL - 6).append("\" y=\"").append(y + 3)
					.append("\" font-size=\"9\" fill=\"#94a3b8\" text-anchor=\"end\">").append(max * g / 4).append("</text>");
		}
		for (int i = 0; i < nx; i++) {
			int x = padL + (nx == 1 ? plotW / 2 : plotW * i / (nx - 1));
			svg.append("<text x=\"").append(x).append("\" y=\"").append(h - 8)
					.append("\" font-size=\"9\" fill=\"#64748b\" text-anchor=\"middle\">")
					.append(esc(shortText(xLabels.get(i), 8))).append("</text>");
		}
		for (int s = 0; s < seriesValues.size(); s++) {
			int[] v = seriesValues.get(s);
			if (v == null) {
				continue;
			}
			String c = pal[s % pal.length];
			StringBuffer pts = new StringBuffer();
			for (int i = 0; i < nx; i++) {
				int val = i < v.length ? v[i] : 0;
				int x = padL + (nx == 1 ? plotW / 2 : plotW * i / (nx - 1));
				int y = padT + plotH - (int) (plotH * (val / (double) max));
				pts.append(x).append(",").append(y).append(" ");
			}
			svg.append("<polyline points=\"").append(pts).append("\" fill=\"none\" stroke=\"").append(c)
					.append("\" stroke-width=\"2.5\"/>");
			for (int i = 0; i < nx; i++) {
				int val = i < v.length ? v[i] : 0;
				int x = padL + (nx == 1 ? plotW / 2 : plotW * i / (nx - 1));
				int y = padT + plotH - (int) (plotH * (val / (double) max));
				svg.append("<circle cx=\"").append(x).append("\" cy=\"").append(y).append("\" r=\"2.6\" fill=\"")
						.append(c).append("\"/>");
			}
		}
		svg.append("</svg></div>").append(legendHtml(seriesNames, pal));
		return panel(title, description, svg.toString());
	}

	/**
	 * Diagram FUNNEL/corong (HTML/CSS): tahapan menyusut, mis. angkatan masuk -&gt; aktif -&gt; lulus.
	 * Lebar batang sebanding nilai. Modern, responsif, tanpa jfreechart.
	 */
	public static String funnelChart(String title, String description, String[] stages, int[] values) {
		if (stages == null || values == null || stages.length == 0) {
			return panel(title, description, "<div class=\"ais-akad-empty\">Belum ada data yang dapat ditampilkan.</div>");
		}
		int max = 1;
		for (int i = 0; i < values.length; i++) {
			if (values[i] > max) {
				max = values[i];
			}
		}
		String[] pal = palette();
		StringBuffer body = new StringBuffer("<div style=\"display:flex;flex-direction:column;gap:6px;padding:4px 0\">");
		for (int i = 0; i < stages.length; i++) {
			int v = i < values.length ? values[i] : 0;
			int pct = Math.max(10, (int) Math.round(v * 100.0 / max));
			String c = pal[i % pal.length];
			body.append("<div style=\"display:flex;align-items:center;gap:10px\">")
					.append("<div style=\"flex:0 0 42%;text-align:right;font-size:12px;color:#334155\">")
					.append(esc(stages[i])).append("</div>")
					.append("<div style=\"flex:1;\"><div style=\"width:").append(pct)
					.append("%;background:").append(c)
					.append(";color:#fff;font-size:12px;font-weight:700;padding:5px 10px;border-radius:6px;white-space:nowrap\">")
					.append(fmt(v)).append("</div></div></div>");
		}
		return panel(title, description, body.append("</div>").toString());
	}

	/**
	 * Grafik DONUT (lingkaran berlubang) murni HTML/CSS memakai {@code conic-gradient} &mdash;
	 * tanpa pustaka chart eksternal. Cocok untuk menampilkan KOMPOSISI/proporsi dari beberapa
	 * kategori (mis. komposisi kehadiran: Hadir/Izin/Sakit/Alpa/Belum). Bagian tengah donut
	 * menampilkan angka TOTAL, dan di sampingnya ada legenda berisi label, jumlah, dan persentase
	 * tiap kategori. Tata letak fleksibel (membungkus) sehingga rapi di layar mobile maupun desktop.
	 *
	 * @param title       judul kartu.
	 * @param description  penjelasan singkat (boleh {@code null}).
	 * @param labels       nama tiap kategori, berurutan.
	 * @param values       nilai tiap kategori, indeks selaras dengan {@code labels}.
	 * @param colors       warna tiap kategori (opsional); bila {@code null} memakai palet bawaan.
	 * @return potongan HTML kartu berisi donut + legenda.
	 */
	public static String donutChart(String title, String description, List<String> labels, int[] values,
			String[] colors) {
		if (labels == null || values == null || labels.isEmpty()) {
			return panel(title, description,
					"<div class=\"ais-akad-empty\">Belum ada data yang dapat ditampilkan.</div>");
		}
		int total = 0;
		for (int i = 0; i < values.length; i++) {
			if (values[i] > 0) {
				total += values[i];
			}
		}
		String[] pal = (colors != null && colors.length > 0) ? colors : palette();
		StringBuffer gr = new StringBuffer();
		StringBuffer legend = new StringBuffer(
				"<div style=\"display:flex;flex-direction:column;gap:6px;min-width:170px\">");
		double acc = 0;
		for (int i = 0; i < labels.size(); i++) {
			int v = (i < values.length && values[i] > 0) ? values[i] : 0;
			double pct = total > 0 ? v * 100.0 / total : 0;
			String c = pal[i % pal.length];
			if (gr.length() > 0) {
				gr.append(",");
			}
			gr.append(c).append(" ").append(satuDesimal(acc)).append("% ").append(satuDesimal(acc + pct)).append("%");
			acc += pct;
			legend.append("<div style=\"display:flex;align-items:center;gap:8px;font-size:12px;color:#334155\">")
					.append("<span style=\"width:11px;height:11px;border-radius:3px;flex:0 0 auto;background:").append(c)
					.append("\"></span>").append("<span style=\"flex:1\">").append(esc(labels.get(i))).append("</span>")
					.append("<b>").append(fmt(v)).append("</b>")
					.append("<span style=\"color:#94a3b8\">(").append((int) Math.round(pct)).append("%)</span></div>");
		}
		legend.append("</div>");
		String ring = total > 0 ? ("conic-gradient(" + gr.toString() + ")") : "conic-gradient(#e5e7eb 0% 100%)";
		StringBuffer body = new StringBuffer(
				"<div style=\"display:flex;flex-wrap:wrap;gap:18px;align-items:center;justify-content:center;padding:6px 0\">");
		body.append("<div style=\"position:relative;width:150px;height:150px;border-radius:50%;flex:0 0 auto;background:")
				.append(ring).append("\">")
				.append("<div style=\"position:absolute;top:26px;left:26px;right:26px;bottom:26px;background:#fff;border-radius:50%;display:flex;flex-direction:column;align-items:center;justify-content:center\">")
				.append("<div style=\"font-size:22px;font-weight:800;color:#0f172a\">").append(fmt(total))
				.append("</div>").append("<div style=\"font-size:10px;color:#94a3b8\">Total</div></div></div>");
		body.append(legend);
		body.append("</div>");
		return panel(title, description, body.toString());
	}

	private static String satuDesimal(double v) {
		return String.valueOf(Math.round(v * 10.0) / 10.0);
	}

	public static String fmt(int value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	public static String esc(Object value) {
		if (value == null) {
			return "";
		}
		String s = String.valueOf(value);
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	public static String escAttr(Object value) {
		return esc(value);
	}

	public static String shortText(String text, int max) {
		if (text == null) {
			return "";
		}
		if (text.length() <= max) {
			return text;
		}
		return text.substring(0, max) + "...";
	}
}
