package ais.ui.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.zkoss.zul.CategoryModel;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.PieModel;

/**
 * Pengganti CE untuk org.zkoss.zul.Chart + JFreeChartEngine (PE):
 * chart digambar murni dengan HTML/CSS/SVG, tanpa JFreeChart, dengan
 * API setara sehingga kode lama cukup mengganti import/nama class.
 *
 * Didukung: type "pie" (donut conic-gradient + legenda), "bar"/"column"
 * (batang vertikal atau horizontal mengikuti setOrient), "line"
 * (SVG polyline multi-seri). Data tetap memakai model bawaan ZK CE:
 * SimpleCategoryModel / SimplePieModel - tidak perlu mengubah cara
 * pengisian data sama sekali.
 *
 * API kompat yang diterima namun tidak relevan untuk HTML
 * (setEngine, setThreeD, setFgAlpha, dll.) disediakan sebagai no-op.
 * Kompatibel Java 1.7 dan ZK 5 CE maupun ZK 9/10 CE.
 */
public class MyChart extends Div {

	private static final long serialVersionUID = 1L;

	private static final String[] WARNA_SERI = { "var(--ais-theme-primary,#2563eb)",
			"var(--ais-theme-accent,#06b6d4)", "#22c55e", "#f97316", "#8b5cf6", "#ec4899", "#eab308", "#14b8a6",
			"#ef4444", "#64748b", "#a3e635", "#f472b6" };
	private static final String[] WARNA_PIE = { "#2563eb", "#06b6d4", "#22c55e", "#f97316", "#8b5cf6", "#ec4899",
			"#eab308", "#14b8a6", "#ef4444", "#64748b", "#a3e635", "#f472b6", "#0ea5e9", "#84cc16", "#d946ef" };

	private String type = "pie";
	private String title = "";
	private String orient = "vertical";
	private String paneColor = "#ffffff";
	private boolean showLegend = true;
	private Object model;
	private Html kanvas;

	public MyChart() {
		setSclass("ais-ce-chart");
		setStyle("width:100%;box-sizing:border-box;");
		kanvas = new Html("");
		kanvas.setParent(this);
	}

	/* ====================== API kompat Chart ====================== */

	public void setType(String type) {
		this.type = type == null ? "pie" : type.trim().toLowerCase();
		render();
	}

	public String getType() {
		return type;
	}

	public void setModel(Object model) {
		this.model = model;
		render();
	}

	public Object getModel() {
		return model;
	}

	public void setTitle(String title) {
		this.title = title == null ? "" : title;
		render();
	}

	public String getTitle() {
		return title;
	}

	public void setOrient(String orient) {
		this.orient = orient == null ? "vertical" : orient.trim().toLowerCase();
		render();
	}

	public String getOrient() {
		return orient;
	}

	public void setShowLegend(boolean showLegend) {
		this.showLegend = showLegend;
		render();
	}

	public void setPaneColor(String paneColor) {
		this.paneColor = paneColor == null || paneColor.trim().isEmpty() ? "#ffffff" : paneColor.trim();
		render();
	}

	/* No-op kompat: pengaturan khusus JFreeChart yang tidak relevan. */
	public void setEngine(Object engine) {
	}

	public void setThreeD(boolean threeD) {
	}

	public void setFgAlpha(int alpha) {
	}

	public void setBgAlpha(int alpha) {
	}

	public void setXAxis(String label) {
	}

	public void setYAxis(String label) {
	}

	public void setPeriod(String period) {
	}

	public void setDateFormat(String format) {
	}

	public void setBgColor(String bgColor) {
	}

	public void setShowTooltiptext(boolean showTooltiptext) {
	}

	public void setAlign(String align) {
	}

	/* ========================= Rendering ========================= */

	private void render() {
		if (kanvas == null) {
			return;
		}
		// Tampilan modern + seragam: pie & bar dirender lewat renderer bersama (warna mengikuti tema
		// institusi + kartu ringkasan yang bisa diklik untuk rincian). Bila gagal, jatuh ke render
		// bawaan di bawah yang tetap dipertahankan sebagai cadangan; tipe "line" memakai render bawaan.
		try {
			if (model instanceof PieModel) {
				kanvas.setContent(DashboardModernHtmlUtil.buildPieChartHtml(model, title, ""));
				return;
			}
			if (model instanceof CategoryModel && !"line".equals(type)) {
				kanvas.setContent(DashboardModernHtmlUtil.buildCategoryChartHtml(model, title, "", true));
				return;
			}
		} catch (Throwable abaikanModern) { ais.common.ErrorAuditUtil.record(abaikanModern, "auto-audit(empty-catch) src/ais/ui/util/MyChart.java:152");
		}
		try {
			StringBuffer sb = new StringBuffer(8192);
			sb.append("<div style=\"width:100%;box-sizing:border-box;padding:12px;border-radius:14px;background:")
					.append(esc(paneColor)).append(";border:1px solid #e2e8f0;\">");
			if (title != null && title.trim().length() > 0) {
				sb.append("<div style=\"font-weight:800;font-size:13px;color:#0f172a;margin-bottom:10px;\">")
						.append(esc(title)).append("</div>");
			}
			if (model instanceof PieModel) {
				gambarPie(sb, (PieModel) model);
			} else if (model instanceof CategoryModel) {
				if ("line".equals(type)) {
					gambarLine(sb, (CategoryModel) model);
				} else {
					gambarBar(sb, (CategoryModel) model);
				}
			} else {
				sb.append("<div style=\"padding:14px;color:#64748b;font-size:12px;\">Belum ada data untuk ditampilkan.</div>");
			}
			sb.append("</div>");
			kanvas.setContent(sb.toString());
		} catch (Exception e) {
			try {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/util/MyChart.java:178");
			}
		}
	}

	private void gambarPie(StringBuffer sb, PieModel pie) {
		List kategori = keList(pie.getCategories());
		double total = 0;
		for (int i = 0; i < kategori.size(); i++) {
			total += angka(pie.getValue((Comparable) kategori.get(i)));
		}
		if (total <= 0) {
			sb.append("<div style=\"padding:14px;color:#64748b;font-size:12px;\">Belum ada data.</div>");
			return;
		}
		StringBuffer gradien = new StringBuffer();
		double derajat = 0;
		for (int i = 0; i < kategori.size(); i++) {
			double nilai = angka(pie.getValue((Comparable) kategori.get(i)));
			double akhir = derajat + (nilai / total) * 360.0;
			if (gradien.length() > 0) {
				gradien.append(",");
			}
			gradien.append(WARNA_PIE[i % WARNA_PIE.length]).append(" ").append(Math.round(derajat)).append("deg ")
					.append(Math.round(akhir)).append("deg");
			derajat = akhir;
		}
		sb.append("<div style=\"display:flex;flex-wrap:wrap;gap:16px;align-items:center;\">");
		sb.append("<div style=\"width:170px;height:170px;border-radius:50%;flex:0 0 auto;position:relative;")
				.append("background:conic-gradient(").append(gradien).append(");\">")
				.append("<div style=\"position:absolute;left:42px;top:42px;width:86px;height:86px;border-radius:50%;background:")
				.append(esc(paneColor)).append(";display:flex;align-items:center;justify-content:center;")
				.append("font-weight:800;font-size:13px;color:#0f172a;\">").append(fmt(total)).append("</div></div>");
		if (showLegend) {
			sb.append("<div style=\"flex:1 1 220px;min-width:200px;\">");
			for (int i = 0; i < kategori.size(); i++) {
				double nilai = angka(pie.getValue((Comparable) kategori.get(i)));
				int persen = (int) Math.round(nilai * 100.0 / total);
				sb.append("<div style=\"display:flex;align-items:center;gap:8px;font-size:11.5px;color:#334155;margin:5px 0;\">")
						.append("<span style=\"width:11px;height:11px;border-radius:3px;flex:0 0 auto;background:")
						.append(WARNA_PIE[i % WARNA_PIE.length]).append(";\"></span>")
						.append("<span style=\"flex:1 1 auto;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;\">")
						.append(esc(kategori.get(i))).append("</span><b>").append(fmt(nilai)).append("</b>")
						.append("<span style=\"color:#94a3b8;\">").append(persen).append("%</span></div>");
			}
			sb.append("</div>");
		}
		sb.append("</div>");
	}

	private void gambarBar(StringBuffer sb, CategoryModel data) {
		List seris = keList(data.getSeries());
		List kategori = keList(data.getCategories());
		double max = 1;
		for (int s = 0; s < seris.size(); s++) {
			for (int k = 0; k < kategori.size(); k++) {
				max = Math.max(max, angka(data.getValue((Comparable) seris.get(s), (Comparable) kategori.get(k))));
			}
		}
		boolean mendatar = "horizontal".equals(orient);
		if (mendatar) {
			for (int k = 0; k < kategori.size(); k++) {
				sb.append("<div style=\"margin:7px 0;\"><div style=\"font-size:11px;color:#475569;margin-bottom:3px;\">")
						.append(esc(kategori.get(k))).append("</div>");
				for (int s = 0; s < seris.size(); s++) {
					double nilai = angka(data.getValue((Comparable) seris.get(s), (Comparable) kategori.get(k)));
					int lebar = (int) Math.round(nilai * 100.0 / max);
					sb.append("<div style=\"display:flex;align-items:center;gap:6px;margin:2px 0;\">")
							.append("<span style=\"flex:1 1 auto;height:13px;border-radius:999px;background:#eef2f7;overflow:hidden;\">")
							.append("<span style=\"display:block;height:13px;border-radius:999px;width:").append(lebar < 2 && nilai > 0 ? 2 : lebar)
							.append("%;background:").append(WARNA_SERI[s % WARNA_SERI.length]).append(";\"></span></span>")
							.append("<b style=\"font-size:11px;color:#334155;min-width:40px;text-align:right;\">").append(fmt(nilai))
							.append("</b></div>");
				}
				sb.append("</div>");
			}
		} else {
			sb.append("<div style=\"display:flex;align-items:flex-end;gap:10px;min-height:190px;overflow-x:auto;")
					.append("padding:8px 4px 4px;background:repeating-linear-gradient(to top,transparent 0,transparent 37px,#f1f5f9 38px);\">");
			for (int k = 0; k < kategori.size(); k++) {
				sb.append("<div style=\"min-width:44px;text-align:center;flex:0 0 auto;\">")
						.append("<div style=\"height:150px;display:flex;align-items:flex-end;justify-content:center;gap:3px;\">");
				for (int s = 0; s < seris.size(); s++) {
					double nilai = angka(data.getValue((Comparable) seris.get(s), (Comparable) kategori.get(k)));
					int tinggi = Math.max(nilai > 0 ? 4 : 1, (int) Math.round(nilai * 150.0 / max));
					sb.append("<span title=\"").append(esc(seris.get(s))).append(": ").append(fmt(nilai))
							.append("\" style=\"display:inline-block;width:14px;height:").append(tinggi)
							.append("px;border-radius:6px 6px 2px 2px;background:").append(WARNA_SERI[s % WARNA_SERI.length])
							.append(";\"></span>");
				}
				sb.append("</div><div style=\"font-size:10px;color:#64748b;margin-top:5px;max-width:78px;overflow:hidden;")
						.append("text-overflow:ellipsis;white-space:nowrap;\" title=\"").append(esc(kategori.get(k)))
						.append("\">").append(esc(kategori.get(k))).append("</div></div>");
			}
			sb.append("</div>");
		}
		gambarLegenda(sb, seris);
	}

	private void gambarLine(StringBuffer sb, CategoryModel data) {
		List seris = keList(data.getSeries());
		List kategori = keList(data.getCategories());
		int lebar = Math.max(320, kategori.size() * 56);
		int tinggi = 190;
		double max = 1;
		for (int s = 0; s < seris.size(); s++) {
			for (int k = 0; k < kategori.size(); k++) {
				max = Math.max(max, angka(data.getValue((Comparable) seris.get(s), (Comparable) kategori.get(k))));
			}
		}
		sb.append("<div style=\"overflow-x:auto;\"><svg width=\"").append(lebar).append("\" height=\"").append(tinggi + 26)
				.append("\" viewBox=\"0 0 ").append(lebar).append(" ").append(tinggi + 26).append("\">");
		for (int g = 0; g <= 4; g++) {
			int y = 8 + (tinggi - 16) * g / 4;
			sb.append("<line x1=\"0\" y1=\"").append(y).append("\" x2=\"").append(lebar).append("\" y2=\"").append(y)
					.append("\" stroke=\"#e2e8f0\"/>");
		}
		for (int s = 0; s < seris.size(); s++) {
			StringBuffer titik = new StringBuffer();
			for (int k = 0; k < kategori.size(); k++) {
				double nilai = angka(data.getValue((Comparable) seris.get(s), (Comparable) kategori.get(k)));
				int x = kategori.size() <= 1 ? lebar / 2 : 20 + (lebar - 40) * k / (kategori.size() - 1);
				int y = 8 + (int) Math.round((tinggi - 16) * (1.0 - nilai / max));
				if (titik.length() > 0) {
					titik.append(' ');
				}
				titik.append(x).append(',').append(y);
			}
			String warnaPolos = WARNA_PIE[s % WARNA_PIE.length];
			sb.append("<polyline points=\"").append(titik).append("\" fill=\"none\" stroke=\"").append(warnaPolos)
					.append("\" stroke-width=\"2.5\"/>");
		}
		for (int k = 0; k < kategori.size(); k++) {
			int x = kategori.size() <= 1 ? lebar / 2 : 20 + (lebar - 40) * k / (kategori.size() - 1);
			sb.append("<text x=\"").append(x).append("\" y=\"").append(tinggi + 18)
					.append("\" text-anchor=\"middle\" font-size=\"9\" fill=\"#64748b\">").append(esc(kategori.get(k)))
					.append("</text>");
		}
		sb.append("</svg></div>");
		gambarLegenda(sb, seris);
	}

	private void gambarLegenda(StringBuffer sb, List seris) {
		if (!showLegend || seris == null || seris.size() <= 1) {
			return;
		}
		sb.append("<div style=\"display:flex;flex-wrap:wrap;gap:10px;margin-top:8px;font-size:11px;color:#475569;\">");
		for (int s = 0; s < seris.size(); s++) {
			sb.append("<span><i style=\"display:inline-block;width:10px;height:10px;border-radius:3px;margin-right:4px;")
					.append("vertical-align:middle;background:").append(WARNA_SERI[s % WARNA_SERI.length])
					.append(";\"></i>").append(esc(seris.get(s))).append("</span>");
		}
		sb.append("</div>");
	}

	/* ================== Ekspor gambar (untuk laporan) ==================
	 * Chart lama menyediakan getContent() berupa gambar untuk disisipkan
	 * ke report. Versi CE menggambar ulang model dengan java.awt murni
	 * (tanpa JFreeChart) lalu mengembalikan PNG. */

	public org.zkoss.image.Image getContent() {
		try {
			int lebar = pxAtau(getWidth(), 950);
			int tinggi = pxAtau(getHeight(), 400);
			java.awt.image.BufferedImage gambar = new java.awt.image.BufferedImage(lebar, tinggi,
					java.awt.image.BufferedImage.TYPE_INT_RGB);
			java.awt.Graphics2D g = gambar.createGraphics();
			g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
					java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(java.awt.Color.WHITE);
			g.fillRect(0, 0, lebar, tinggi);

			int atas = 16;
			if (title != null && title.trim().length() > 0) {
				g.setColor(new java.awt.Color(15, 23, 42));
				g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
				g.drawString(title, 18, 26);
				atas = 44;
			}

			if (model instanceof PieModel) {
				lukisPieAwt(g, (PieModel) model, lebar, tinggi, atas);
			} else if (model instanceof CategoryModel) {
				lukisBarAwt(g, (CategoryModel) model, lebar, tinggi, atas, "line".equals(type));
			}
			g.dispose();

			java.io.ByteArrayOutputStream keluaran = new java.io.ByteArrayOutputStream();
			javax.imageio.ImageIO.write(gambar, "png", keluaran);
			return new org.zkoss.image.AImage("chart.png", keluaran.toByteArray());
		} catch (Exception e) {
			try {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/util/MyChart.java:371");
			}
			return null;
		}
	}

	private static int pxAtau(String nilai, int bawaan) {
		try {
			String angka = nilai == null ? "" : nilai.replaceAll("[^0-9]", "");
			int hasil = angka.isEmpty() ? bawaan : Integer.parseInt(angka);
			return hasil < 120 ? bawaan : hasil;
		} catch (Exception e) {
			return bawaan;
		}
	}

	private static java.awt.Color warnaAwt(int index) {
		String hex = WARNA_PIE[index % WARNA_PIE.length];
		try {
			return java.awt.Color.decode(hex);
		} catch (Exception e) {
			return new java.awt.Color(37, 99, 235);
		}
	}

	private void lukisPieAwt(java.awt.Graphics2D g, PieModel pie, int lebar, int tinggi, int atas) {
		List kategori = keList(pie.getCategories());
		double total = 0;
		for (int i = 0; i < kategori.size(); i++) {
			total += angka(pie.getValue((Comparable) kategori.get(i)));
		}
		if (total <= 0) {
			return;
		}
		int diameter = Math.min(lebar / 2, tinggi - atas - 20);
		int x = 24, y = atas + 6;
		double mulai = 90;
		for (int i = 0; i < kategori.size(); i++) {
			double porsi = angka(pie.getValue((Comparable) kategori.get(i))) / total * 360.0;
			g.setColor(warnaAwt(i));
			g.fillArc(x, y, diameter, diameter, (int) Math.round(mulai - porsi), (int) Math.round(porsi) + 1);
			mulai -= porsi;
		}
		g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
		int ly = y + 8;
		int lx = x + diameter + 28;
		for (int i = 0; i < kategori.size() && ly < tinggi - 8; i++) {
			double nilai = angka(pie.getValue((Comparable) kategori.get(i)));
			g.setColor(warnaAwt(i));
			g.fillRect(lx, ly - 9, 11, 11);
			g.setColor(new java.awt.Color(51, 65, 85));
			g.drawString(kategori.get(i) + " : " + fmt(nilai) + " ("
					+ (int) Math.round(nilai * 100.0 / total) + "%)", lx + 17, ly + 1);
			ly += 19;
		}
	}

	private void lukisBarAwt(java.awt.Graphics2D g, CategoryModel data, int lebar, int tinggi, int atas,
			boolean garis) {
		List seris = keList(data.getSeries());
		List kategori = keList(data.getCategories());
		if (kategori.isEmpty()) {
			return;
		}
		double max = 1;
		for (int s = 0; s < seris.size(); s++) {
			for (int k = 0; k < kategori.size(); k++) {
				max = Math.max(max, angka(data.getValue((Comparable) seris.get(s), (Comparable) kategori.get(k))));
			}
		}
		int kiri = 24, bawah = tinggi - 34;
		int areaTinggi = bawah - atas - 6;
		int areaLebar = lebar - kiri - 20;
		int slot = areaLebar / kategori.size();

		g.setColor(new java.awt.Color(226, 232, 240));
		for (int i = 0; i <= 4; i++) {
			int gy = bawah - areaTinggi * i / 4;
			g.drawLine(kiri, gy, lebar - 16, gy);
		}

		g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
		if (garis) {
			for (int s = 0; s < seris.size(); s++) {
				g.setColor(warnaAwt(s));
				g.setStroke(new java.awt.BasicStroke(2.4f));
				int xSebelum = 0, ySebelum = 0;
				for (int k = 0; k < kategori.size(); k++) {
					double nilai = angka(data.getValue((Comparable) seris.get(s), (Comparable) kategori.get(k)));
					int x = kiri + slot * k + slot / 2;
					int y = bawah - (int) Math.round(areaTinggi * nilai / max);
					if (k > 0) {
						g.drawLine(xSebelum, ySebelum, x, y);
					}
					xSebelum = x;
					ySebelum = y;
				}
			}
		} else {
			int lebarBatang = Math.max(6, Math.min(26, (slot - 8) / Math.max(1, seris.size())));
			for (int k = 0; k < kategori.size(); k++) {
				int xAwal = kiri + slot * k + (slot - lebarBatang * seris.size()) / 2;
				for (int s = 0; s < seris.size(); s++) {
					double nilai = angka(data.getValue((Comparable) seris.get(s), (Comparable) kategori.get(k)));
					int tinggiBatang = (int) Math.round(areaTinggi * nilai / max);
					g.setColor(warnaAwt(s));
					g.fillRoundRect(xAwal + lebarBatang * s, bawah - tinggiBatang, lebarBatang - 2,
							Math.max(2, tinggiBatang), 4, 4);
				}
			}
		}

		g.setColor(new java.awt.Color(100, 116, 139));
		for (int k = 0; k < kategori.size(); k++) {
			String label = String.valueOf(kategori.get(k));
			if (label.length() > 12) {
				label = label.substring(0, 12) + "..";
			}
			g.drawString(label, kiri + slot * k + 4, bawah + 14);
		}
		int lx = kiri;
		for (int s = 0; s < seris.size(); s++) {
			g.setColor(warnaAwt(s));
			g.fillRect(lx, tinggi - 14, 10, 10);
			g.setColor(new java.awt.Color(51, 65, 85));
			String namaSeri = String.valueOf(seris.get(s));
			g.drawString(namaSeri, lx + 14, tinggi - 5);
			lx += 24 + g.getFontMetrics().stringWidth(namaSeri);
		}
	}

	/* ========================= Util kecil ========================= */

	private static List keList(Collection koleksi) {
		return koleksi == null ? new ArrayList() : new ArrayList(koleksi);
	}

	private static double angka(Number nilai) {
		return nilai == null ? 0.0 : nilai.doubleValue();
	}

	private static String fmt(double nilai) {
		if (nilai == Math.floor(nilai)) {
			return String.valueOf((long) nilai);
		}
		return String.valueOf(Math.round(nilai * 100.0) / 100.0);
	}

	private static String esc(Object value) {
		String s = value == null ? "" : String.valueOf(value);
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		return s;
	}
}
