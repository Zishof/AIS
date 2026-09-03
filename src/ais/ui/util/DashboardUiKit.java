package ais.ui.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

/**
 * Kotak peralatan tampilan dasbor modern berbasis HTML + CSS murni (tanpa JFreeChart).
 *
 * Tujuan: satu sumber komponen visual (kartu ringkasan, batang, donat, jaring laba-laba/spider,
 * garis tren) supaya tampilan antar-dasbor seragam, responsif di HP maupun desktop, dan mudah
 * dipelihara. Semua warna memakai variabel tema (--ais-theme-*) dengan warna cadangan agar ikut
 * berubah saat tema diganti.
 *
 * Catatan reuse: dipakai oleh InformasiPembayaranMahasiswaAction, InformasiKunjunganMahasiswaAction,
 * dan dapat dipakai dasbor lain. Semua method statik & bebas-state agar aman dipanggil dari mana saja.
 */
public final class DashboardUiKit {

	private DashboardUiKit() {
	}

	/** Warna tema dengan cadangan, dipakai konsisten oleh semua komponen. */
	public static final String PRIMARY = "var(--ais-theme-primary,#2563eb)";
	public static final String ACCENT = "var(--ais-theme-accent,#06b6d4)";
	public static final String GOOD = "#16a34a";
	public static final String WARN = "#f97316";
	public static final String BAD = "#dc2626";
	public static final String INK = "#0f172a";
	public static final String MUTED = "#64748b";
	public static final String LINE = "#e2e8f0";

	/** Palet warna untuk donat & seri ganda agar segmen mudah dibedakan. */
	// Warna pertama mengikuti tema institusi aktif (var --ais-theme-primary); sisanya pembeda tetap.
	// Aman karena PALETTE hanya dipakai pada konteks CSS (conic-gradient & background), bukan atribut SVG.
	private static final String[] PALETTE = { "var(--ais-theme-primary,#2563eb)", "#16a34a", "#f97316", "#9333ea",
			"#06b6d4", "#e11d48", "#ca8a04", "#0891b2", "#7c3aed", "#15803d" };

	private static final ThreadLocal<DecimalFormat> MONEY = new ThreadLocal<DecimalFormat>() {
		@Override
		protected DecimalFormat initialValue() {
			DecimalFormatSymbols s = new DecimalFormatSymbols();
			s.setGroupingSeparator('.');
			s.setDecimalSeparator(',');
			return new DecimalFormat("#,##0", s);
		}
	};

	// ============================================================
	// HELPER DASAR
	// ============================================================

	/** Amankan teks agar tidak merusak HTML. */
	public static String esc(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	/** Format angka rupiah gaya Indonesia (4.300.000). */
	public static String money(double value) {
		return MONEY.get().format(value);
	}

	/** Persentase aman 0..100 (bulat). */
	public static int pct(double value, double max) {
		if (max <= 0) {
			return 0;
		}
		long p = Math.round(value * 100.0 / max);
		if (p < 0) {
			return 0;
		}
		return p > 100 ? 100 : (int) p;
	}

	private static String color(int index) {
		return PALETTE[Math.abs(index) % PALETTE.length];
	}

	/** Bungkus string HTML menjadi komponen ZK siap-tempel. */
	public static Html html(String innerHtml) {
		return new Html(innerHtml == null ? "" : innerHtml);
	}

	// ============================================================
	// PANEL + DESKRIPSI SEDERHANA
	// ============================================================

	/**
	 * Buat panel modern dengan judul dan kalimat penjelas yang sangat sederhana untuk awam.
	 * Body panel adalah anak pertama (Panelchildren) — ambil lewat {@link #body(Panel)}.
	 */
	public static Panel panel(Component parent, String title, String simpleDesc) {
		Panel panel = new Panel();
		panel.setTitle(title);
		panel.setBorder("normal");
		panel.setCollapsible(true);
		panel.setStyle("margin-bottom:12px;border:1px solid " + LINE + ";border-radius:14px;overflow:hidden;"
				+ "box-shadow:0 6px 18px rgba(15,23,42,.06);");
		panel.setParent(parent);

		Panelchildren body = new Panelchildren();
		body.setStyle("padding:10px;background:#ffffff;");
		body.setParent(panel);
		if (simpleDesc != null && simpleDesc.trim().length() > 0) {
			body.appendChild(html(descChip(simpleDesc)));
		}
		return panel;
	}

	/** Ambil area isi panel yang dibuat lewat {@link #panel}. */
	public static Panelchildren body(Panel panel) {
		return (Panelchildren) panel.getChildren().get(0);
	}

	/** Chip penjelas singkat (kalimat sederhana untuk end user awam). */
	public static String descChip(String simpleDesc) {
		return "<div style='font-size:11.5px;color:" + MUTED + ";line-height:1.5;margin:0 0 10px 0;background:#f8fafc;"
				+ "border:1px solid " + LINE + ";border-left:3px solid " + PRIMARY + ";border-radius:8px;padding:8px 10px;'>"
				+ esc(simpleDesc) + "</div>";
	}

	// ============================================================
	// SPANDUK PENGANTAR (HEADER DASBOR / HALAMAN)
	// ============================================================

	/**
	 * Spanduk pengantar bergaya gradient untuk bagian paling atas sebuah halaman/dasbor.
	 * Berisi judul tebal + satu-dua kalimat penjelas yang sangat sederhana untuk end user awam.
	 * Dipakai ulang oleh GenericCrudAction dan semua Action dasbor agar konsisten.
	 */
	public static String introBanner(String title, String desc) {
		return "<div style=\"margin:0 0 12px 0;padding:14px 16px;border-radius:16px;"
				+ "background:linear-gradient(135deg, rgba(0,0,0,.30), rgba(0,0,0,0) 55%),"
				+ "linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);"
				+ "color:#ffffff;box-shadow:0 12px 24px rgba(15,23,42,.16);\">"
				+ "<div style=\"font-size:17px;font-weight:900;line-height:1.25;\">" + esc(title) + "</div>"
				+ "<div style=\"font-size:12px;line-height:1.65;margin-top:6px;opacity:.93;\">" + esc(desc) + "</div>"
				+ "</div>";
	}

	/** Tempel spanduk pengantar di paling atas komponen induk (tidak menggagalkan halaman bila error). */
	public static void attachIntro(Component parent, String title, String desc) {
		if (parent == null || title == null || desc == null || title.trim().isEmpty()) {
			return;
		}
		try {
			Html h = html(introBanner(title, desc));
			if (parent.getChildren() != null && parent.getChildren().size() > 0) {
				parent.insertBefore(h, (Component) parent.getChildren().get(0));
			} else {
				parent.appendChild(h);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/DashboardUiKit.java:171");
			/* spanduk pengantar bersifat kosmetik — abaikan bila gagal */
		}
	}

	/**
	 * Header modul bergaya modern: kotak ikon gradien + label kecil "MODUL" + judul tebal + satu
	 * kalimat penjelas sederhana di bawahnya. Meniru gaya header template e-learning (mis.
	 * {@code tugas_kelompok.jsp}) sehingga tampilan yang dibangun lewat komponen ZK konsisten dengan
	 * halaman JSP modern. Murni HTML/CSS inline dengan ikon berupa <b>SVG inline</b> (TANPA
	 * dependensi Font Awesome), sehingga aman dirender via {@link #html(String)} pada komponen ZK
	 * mana pun tanpa khawatir pustaka ikon belum dimuat. Warna gradien otomatis mengikuti tema aktif
	 * ({@code --ais-theme-primary} / {@code --ais-theme-accent}) dengan warna cadangan bila variabel
	 * tema tidak tersedia. Dipakai ulang oleh helper/Action mana pun agar penyeragaman header modul
	 * cukup dikelola di satu tempat.
	 *
	 * @param judul     judul modul, mis. "Linimasa Tugas Kelompok"
	 * @param deskripsi satu-dua kalimat sangat sederhana untuk end user awam (boleh kosong/{@code null})
	 * @return potongan HTML header modul siap ditempel via {@link #html(String)}
	 */
	public static String headerModul(String judul, String deskripsi) {
		return headerModul("grup", judul, deskripsi);
	}

	/**
	 * Varian {@link #headerModul(String, String)} dengan pemilihan ikon agar sesuai konteks modul.
	 * Ikon dipilih lewat kata kunci sederhana sehingga pemanggil tidak perlu menulis SVG sendiri.
	 *
	 * @param iconKey kata kunci ikon: {@code "grup"} (default), {@code "tugas"}, {@code "ujian"},
	 *                {@code "materi"}, {@code "absensi"}, atau {@code "dokumen"}
	 * @param judul     judul modul
	 * @param deskripsi satu-dua kalimat sederhana (boleh kosong/{@code null})
	 * @return potongan HTML header modul siap ditempel via {@link #html(String)}
	 */
	public static String headerModul(String iconKey, String judul, String deskripsi) {
		String desc = deskripsi == null ? "" : deskripsi.trim();
		return "<div style=\"display:flex;align-items:center;gap:14px;margin:4px 0 14px 0;flex-wrap:wrap;\">"
				+ "<div style=\"width:52px;height:52px;border-radius:16px;flex:0 0 auto;display:flex;align-items:center;"
				+ "justify-content:center;color:#fff;box-shadow:0 10px 24px rgba(15,23,42,.20);"
				+ "background:linear-gradient(135deg, rgba(255,255,255,.18), rgba(255,255,255,0) 60%),"
				+ "linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8), var(--ais-theme-accent,#06b6d4));\">"
				+ svgIkon(iconKey) + "</div>"
				+ "<div style=\"min-width:0;\">"
				+ "<div style=\"font-size:11px;letter-spacing:.12em;text-transform:uppercase;color:" + MUTED
				+ ";font-weight:800;\">Modul</div>"
				+ "<div style=\"font-size:19px;font-weight:900;color:#0f172a;letter-spacing:-.02em;line-height:1.15;\">"
				+ esc(judul) + "</div>"
				+ (desc.isEmpty() ? ""
						: "<div style=\"font-size:12.5px;color:" + MUTED + ";margin-top:3px;line-height:1.5;\">"
								+ esc(desc) + "</div>")
				+ "</div></div>";
	}

	/** Mengembalikan markup {@code <svg>} ikon garis (stroke) berdasarkan kata kunci modul. */
	private static String svgIkon(String key) {
		String k = key == null ? "" : key.trim().toLowerCase();
		String inner;
		if ("tugas".equals(k)) {
			inner = "<path d=\"M9 11l3 3L22 4\"/><path d=\"M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11\"/>";
		} else if ("ujian".equals(k)) {
			inner = "<path d=\"M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z\"/><path d=\"M14 2v6h6\"/>"
					+ "<path d=\"M16 13H8\"/><path d=\"M16 17H8\"/><path d=\"M10 9H8\"/>";
		} else if ("materi".equals(k)) {
			inner = "<path d=\"M4 19.5A2.5 2.5 0 0 1 6.5 17H20\"/>"
					+ "<path d=\"M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z\"/>";
		} else if ("absensi".equals(k)) {
			inner = "<rect x=\"3\" y=\"4\" width=\"18\" height=\"18\" rx=\"2\" ry=\"2\"/><line x1=\"16\" y1=\"2\" x2=\"16\" y2=\"6\"/>"
					+ "<line x1=\"8\" y1=\"2\" x2=\"8\" y2=\"6\"/><line x1=\"3\" y1=\"10\" x2=\"21\" y2=\"10\"/><path d=\"M9 16l2 2 4-4\"/>";
		} else if ("dokumen".equals(k)) {
			inner = "<path d=\"M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z\"/>";
		} else {
			inner = "<path d=\"M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2\"/><circle cx=\"9\" cy=\"7\" r=\"4\"/>"
					+ "<path d=\"M23 21v-2a4 4 0 0 0-3-3.87\"/><path d=\"M16 3.13a4 4 0 0 1 0 7.75\"/>";
		}
		return "<svg width=\"26\" height=\"26\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" "
				+ "stroke-linecap=\"round\" stroke-linejoin=\"round\">" + inner + "</svg>";
	}

	// ============================================================
	// KARTU RINGKASAN (RESPONSIF)
	// ============================================================

	/** Satu kartu angka ringkasan. */
	public static final class Stat {
		final String label;
		final String value;
		final String hint;
		final String accent;

		public Stat(String label, String value, String hint, String accent) {
			this.label = label;
			this.value = value;
			this.hint = hint;
			this.accent = accent == null ? PRIMARY : accent;
		}
	}

	/** Deretan kartu ringkasan yang otomatis turun baris di layar sempit (HP). */
	public static String cards(List<Stat> stats) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:flex;flex-wrap:wrap;gap:10px;margin-bottom:12px;'>");
		if (stats != null) {
			for (Stat s : stats) {
				sb.append("<div class='ais-dashboard-clickable' tabindex='0' data-ais-title='")
						.append(esc(s.label)).append("' data-ais-value='").append(esc(s.value))
						.append("' data-ais-hint='").append(esc(s.hint == null ? "" : s.hint))
						.append("' onclick='if(window.aisDashboardCardDetail){window.aisDashboardCardDetail(this,event);}' " )
						.append("onkeydown='if((event.keyCode==13||event.keyCode==32)&amp;&amp;window.aisDashboardCardDetail){window.aisDashboardCardDetail(this,event);}' ")
						.append("style='cursor:pointer;flex:1 1 150px;min-width:140px;background:#ffffff;border:1px solid ").append(LINE)
						.append(";border-top:3px solid ").append(s.accent)
						.append(";border-radius:12px;padding:11px 12px;box-shadow:0 2px 8px rgba(15,23,42,.05);transition:transform .15s ease,box-shadow .15s ease;'>")
						.append("<div style='font-size:10px;color:").append(MUTED)
						.append(";text-transform:uppercase;letter-spacing:.04em;font-weight:800;'>").append(esc(s.label))
						.append("</div>").append("<div style='font-size:19px;font-weight:800;color:").append(s.accent)
						.append(";margin-top:4px;word-break:break-word;'>").append(esc(s.value)).append("</div>");
				if (s.hint != null && s.hint.trim().length() > 0) {
					sb.append("<div style='font-size:11px;color:").append(MUTED).append(";margin-top:3px;'>")
							.append(esc(s.hint)).append("</div>");
				}
				sb.append("</div>");
			}
		}
		sb.append("</div>");
		return sb.toString();
	}

	// ============================================================
	// PEMBUNGKUS GRID RESPONSIF
	// ============================================================

	/** Buka grid responsif: kolom otomatis menyesuaikan lebar layar (minimal minPx per kolom). */
	public static String openGrid(int minPx) {
		return "<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(" + minPx
				+ "px,1fr));gap:12px;'>";
	}

	public static String closeGrid() {
		return "</div>";
	}

	// ============================================================
	// BATANG HORIZONTAL (TREN / PERINGKAT)
	// ============================================================

	/** Daftar batang horizontal. valueFormatter: "money" untuk rupiah, selain itu apa adanya + unit. */
	public static String barList(String title, String desc, Map<String, ? extends Number> data, String barColor,
			String unit, boolean asMoney, String emptyMsg) {
		StringBuilder sb = new StringBuilder();
		sb.append(cardOpen(title, desc));
		double max = 0;
		if (data != null) {
			for (Number v : data.values()) {
				double d = v == null ? 0 : v.doubleValue();
				if (d > max) {
					max = d;
				}
			}
		}
		if (data == null || data.isEmpty() || max <= 0) {
			sb.append(emptyBox(emptyMsg));
		} else {
			for (Map.Entry<String, ? extends Number> e : data.entrySet()) {
				double v = e.getValue() == null ? 0 : e.getValue().doubleValue();
				int w = pct(v, max);
				String shown = asMoney ? money(v) : (money(v) + (unit == null ? "" : " " + unit));
				sb.append("<div style='margin:7px 0;'>")
						.append("<div style='display:flex;justify-content:space-between;gap:8px;font-size:11px;color:")
						.append(INK).append(";'><span style='overflow:hidden;text-overflow:ellipsis;white-space:nowrap;'>")
						.append(esc(e.getKey())).append("</span><b>").append(esc(shown)).append("</b></div>")
						.append("<div style='height:10px;background:#eef2f7;border-radius:999px;overflow:hidden;margin-top:3px;'>")
						.append("<div style='height:10px;width:").append(w).append("%;background:").append(barColor)
						.append(";border-radius:999px;transition:width .4s ease;'></div></div></div>");
			}
		}
		sb.append(cardClose());
		return sb.toString();
	}

	// ============================================================
	// SERI GANDA (mis. Tagihan vs Terbayar)
	// ============================================================

	public static String groupedBar(String title, String desc, Map<String, Double> seriesA, Map<String, Double> seriesB,
			String labelA, String labelB, String colorA, String colorB, boolean asMoney) {
		StringBuilder sb = new StringBuilder();
		sb.append(cardOpen(title, desc));
		sb.append(legend(new String[] { labelA, labelB }, new String[] { colorA, colorB }));
		double max = 0;
		if (seriesA != null) {
			for (Double v : seriesA.values()) {
				if (v != null && v > max) {
					max = v;
				}
			}
		}
		if (seriesB != null) {
			for (Double v : seriesB.values()) {
				if (v != null && v > max) {
					max = v;
				}
			}
		}
		if ((seriesA == null || seriesA.isEmpty()) && (seriesB == null || seriesB.isEmpty())) {
			sb.append(emptyBox("Belum ada data untuk dibandingkan."));
		} else if (max <= 0) {
			sb.append(emptyBox("Belum ada nilai yang bisa ditampilkan."));
		} else {
			List<String> keys = new ArrayList<String>();
			if (seriesA != null) {
				keys.addAll(seriesA.keySet());
			}
			if (seriesB != null) {
				for (String k : seriesB.keySet()) {
					if (!keys.contains(k)) {
						keys.add(k);
					}
				}
			}
			for (String k : keys) {
				double a = seriesA != null && seriesA.get(k) != null ? seriesA.get(k) : 0.0;
				double b = seriesB != null && seriesB.get(k) != null ? seriesB.get(k) : 0.0;
				sb.append("<div style='margin:9px 0;'>")
						.append("<div style='font-size:11px;color:").append(INK).append(";font-weight:700;margin-bottom:3px;'>")
						.append(esc(k)).append("</div>");
				sb.append(miniBar(a, max, colorA, asMoney));
				sb.append(miniBar(b, max, colorB, asMoney));
				sb.append("</div>");
			}
		}
		sb.append(cardClose());
		return sb.toString();
	}

	private static String miniBar(double v, double max, String color, boolean asMoney) {
		int w = pct(v, max);
		return "<div style='display:flex;align-items:center;gap:6px;margin:2px 0;'>"
				+ "<div style='flex:1;height:9px;background:#eef2f7;border-radius:999px;overflow:hidden;'>"
				+ "<div style='height:9px;width:" + w + "%;background:" + color + ";border-radius:999px;'></div></div>"
				+ "<span style='font-size:10px;color:" + MUTED + ";min-width:70px;text-align:right;'>"
				+ esc(money(v)) + "</span></div>";
	}

	// ============================================================
	// DONAT KOMPOSISI (conic-gradient CSS, tanpa JFreeChart)
	// ============================================================

	public static String donut(String title, String desc, LinkedHashMap<String, Double> parts, boolean asMoney,
			String emptyMsg) {
		StringBuilder sb = new StringBuilder();
		sb.append(cardOpen(title, desc));
		double total = 0;
		if (parts != null) {
			for (Double v : parts.values()) {
				total += v == null ? 0 : v.doubleValue();
			}
		}
		if (parts == null || parts.isEmpty() || total <= 0) {
			sb.append(emptyBox(emptyMsg));
			sb.append(cardClose());
			return sb.toString();
		}
		StringBuilder gradient = new StringBuilder();
		StringBuilder legend = new StringBuilder();
		double acc = 0;
		int i = 0;
		for (Map.Entry<String, Double> e : parts.entrySet()) {
			double v = e.getValue() == null ? 0 : e.getValue();
			double start = acc * 100.0 / total;
			acc += v;
			double end = acc * 100.0 / total;
			String c = color(i);
			if (gradient.length() > 0) {
				gradient.append(",");
			}
			gradient.append(c).append(" ").append(fmt1(start)).append("% ").append(fmt1(end)).append("%");
			int share = pct(v, total);
			legend.append("<div style='display:flex;align-items:center;gap:6px;font-size:11px;color:").append(INK)
					.append(";margin:3px 0;'><span style='width:10px;height:10px;border-radius:3px;background:").append(c)
					.append(";display:inline-block;'></span><span style='flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;'>")
					.append(esc(e.getKey())).append("</span><b>").append(share).append("%</b></div>");
			i++;
		}
		sb.append("<div style='display:flex;flex-wrap:wrap;gap:14px;align-items:center;'>");
		sb.append("<div style='width:140px;height:140px;border-radius:999px;flex:0 0 auto;margin:0 auto;background:conic-gradient(")
				.append(gradient).append(");display:flex;align-items:center;justify-content:center;'>")
				.append("<div style='width:84px;height:84px;border-radius:999px;background:#fff;display:flex;align-items:center;justify-content:center;text-align:center;box-shadow:inset 0 0 0 1px ")
				.append(LINE).append(";'><div><div style='font-size:9px;color:").append(MUTED)
				.append(";text-transform:uppercase;font-weight:800;'>Total</div><div style='font-size:12px;font-weight:800;color:")
				.append(INK).append(";'>").append(asMoney ? esc(money(total)) : esc(money(total))).append("</div></div></div></div>");
		sb.append("<div style='flex:1;min-width:180px;'>").append(legend).append("</div>");
		sb.append("</div>");
		sb.append(cardClose());
		return sb.toString();
	}

	// ============================================================
	// JARING LABA-LABA / SPIDER (SVG murni)
	// ============================================================

	/**
	 * Diagram jaring laba-laba (radar) memakai SVG. labels & valuesPercent harus sama panjang (>=3).
	 * Nilai 0..100. Responsif: lebar mengikuti kontainer, tinggi otomatis.
	 */
	public static String spider(String title, String desc, String[] labels, int[] valuesPercent) {
		StringBuilder sb = new StringBuilder();
		sb.append(cardOpen(title, desc));
		int n = labels == null ? 0 : labels.length;
		if (n < 3 || valuesPercent == null || valuesPercent.length != n) {
			sb.append(emptyBox("Data belum cukup untuk menggambar jaring."));
			sb.append(cardClose());
			return sb.toString();
		}
		double cx = 150, cy = 140, r = 96;
		StringBuilder rings = new StringBuilder();
		// 4 cincin bantu
		for (int ring = 1; ring <= 4; ring++) {
			double rr = r * ring / 4.0;
			rings.append("<polygon points='").append(polygonPoints(cx, cy, rr, n, null))
					.append("' fill='none' stroke='").append(LINE).append("' stroke-width='1'/>");
		}
		StringBuilder axes = new StringBuilder();
		StringBuilder labelsSvg = new StringBuilder();
		for (int i = 0; i < n; i++) {
			double ang = angle(i, n);
			double x = cx + r * Math.cos(ang);
			double y = cy + r * Math.sin(ang);
			axes.append("<line x1='").append(fmt1(cx)).append("' y1='").append(fmt1(cy)).append("' x2='").append(fmt1(x))
					.append("' y2='").append(fmt1(y)).append("' stroke='").append(LINE).append("' stroke-width='1'/>");
			double lx = cx + (r + 16) * Math.cos(ang);
			double ly = cy + (r + 16) * Math.sin(ang);
			String anchor = lx < cx - 5 ? "end" : (lx > cx + 5 ? "start" : "middle");
			labelsSvg.append("<text x='").append(fmt1(lx)).append("' y='").append(fmt1(ly + 3))
					.append("' font-size='9' fill='").append(MUTED).append("' text-anchor='").append(anchor).append("'>")
					.append(esc(shorten(labels[i], 16))).append("</text>");
		}
		String dataPts = polygonPoints(cx, cy, r, n, valuesPercent);
		sb.append("<div style='text-align:center;'>");
		sb.append("<svg viewBox='0 0 300 280' style='width:100%;max-width:340px;height:auto;'>");
		sb.append(rings).append(axes);
		sb.append("<polygon points='").append(dataPts).append("' fill='").append(PRIMARY)
				.append("' fill-opacity='0.22' stroke='").append(PRIMARY).append("' stroke-width='2'/>");
		// titik simpul
		for (int i = 0; i < n; i++) {
			double ang = angle(i, n);
			double rr = r * clampPct(valuesPercent[i]) / 100.0;
			double x = cx + rr * Math.cos(ang);
			double y = cy + rr * Math.sin(ang);
			sb.append("<circle cx='").append(fmt1(x)).append("' cy='").append(fmt1(y)).append("' r='2.5' fill='")
					.append(PRIMARY).append("'/>");
		}
		sb.append(labelsSvg);
		sb.append("</svg></div>");
		sb.append(cardClose());
		return sb.toString();
	}

	private static String polygonPoints(double cx, double cy, double r, int n, int[] valuesPercent) {
		StringBuilder pts = new StringBuilder();
		for (int i = 0; i < n; i++) {
			double ang = angle(i, n);
			double rr = valuesPercent == null ? r : (r * clampPct(valuesPercent[i]) / 100.0);
			double x = cx + rr * Math.cos(ang);
			double y = cy + rr * Math.sin(ang);
			if (i > 0) {
				pts.append(" ");
			}
			pts.append(fmt1(x)).append(",").append(fmt1(y));
		}
		return pts.toString();
	}

	private static double angle(int i, int n) {
		return -Math.PI / 2 + (2 * Math.PI * i / n);
	}

	private static int clampPct(int v) {
		return v < 0 ? 0 : (v > 100 ? 100 : v);
	}

	/**
	 * Candlestick nilai transaksi per periode. Bukan grafik saham: badan lilin
	 * menunjukkan nilai transaksi pertama/terakhir, sedangkan sumbu menunjukkan
	 * transaksi terendah/tertinggi pada periode yang sama.
	 */
	public static String candlestick(String title, String desc, String[] labels,
			double[] open, double[] high, double[] low, double[] close) {
		StringBuilder sb = new StringBuilder();
		sb.append(cardOpen(title, desc));
		int n = labels == null ? 0 : labels.length;
		if (n == 0 || open == null || high == null || low == null || close == null
				|| open.length != n || high.length != n || low.length != n || close.length != n) {
			sb.append(emptyBox("Belum ada data transaksi untuk candlestick."));
			sb.append(cardClose());
			return sb.toString();
		}
		double max = 0, min = Double.MAX_VALUE;
		for (int i = 0; i < n; i++) {
			max = Math.max(max, high[i]);
			min = Math.min(min, low[i]);
		}
		if (min == Double.MAX_VALUE) min = 0;
		if (max <= min) max = min + 1;
		double w = 640, h = 210, pad = 24, step = (w - pad * 2) / n;
		sb.append("<svg viewBox='0 0 640 210' style='width:100%;height:auto;max-height:230px;'>");
		for (int i = 0; i < n; i++) {
			double x = pad + step * (i + .5);
			double yh = h - 28 - (high[i] - min) * (h - 48) / (max - min);
			double yl = h - 28 - (low[i] - min) * (h - 48) / (max - min);
			double yo = h - 28 - (open[i] - min) * (h - 48) / (max - min);
			double yc = h - 28 - (close[i] - min) * (h - 48) / (max - min);
			String warna = close[i] >= open[i] ? GOOD : BAD;
			double top = Math.min(yo, yc), tinggi = Math.max(3, Math.abs(yo - yc));
			sb.append("<line x1='").append(fmt1(x)).append("' y1='").append(fmt1(yh))
					.append("' x2='").append(fmt1(x)).append("' y2='").append(fmt1(yl))
					.append("' stroke='").append(warna).append("' stroke-width='2'/>");
			sb.append("<rect x='").append(fmt1(x - Math.min(9, step * .25))).append("' y='")
					.append(fmt1(top)).append("' width='").append(fmt1(Math.min(18, step * .5)))
					.append("' height='").append(fmt1(tinggi)).append("' rx='2' fill='").append(warna).append("'/>");
			sb.append("<text x='").append(fmt1(x)).append("' y='201' font-size='8' fill='").append(MUTED)
					.append("' text-anchor='middle'>").append(esc(shorten(labels[i], 8))).append("</text>");
		}
		sb.append("</svg>");
		sb.append(cardClose());
		return sb.toString();
	}

	// ============================================================
	// GARIS TREN (SVG sparkline)
	// ============================================================

	public static String sparkline(String title, String desc, List<? extends Number> values, String color,
			String emptyMsg) {
		StringBuilder sb = new StringBuilder();
		sb.append(cardOpen(title, desc));
		if (values == null || values.size() < 2) {
			sb.append(emptyBox(emptyMsg));
			sb.append(cardClose());
			return sb.toString();
		}
		double max = 0, min = Double.MAX_VALUE;
		for (Number v : values) {
			double d = v == null ? 0 : v.doubleValue();
			max = Math.max(max, d);
			min = Math.min(min, d);
		}
		if (max <= min) {
			max = min + 1;
		}
		double w = 300, h = 70, pad = 6;
		StringBuilder line = new StringBuilder();
		StringBuilder area = new StringBuilder();
		int n = values.size();
		for (int i = 0; i < n; i++) {
			double d = values.get(i) == null ? 0 : values.get(i).doubleValue();
			double x = pad + (w - 2 * pad) * i / (n - 1);
			double y = h - pad - (h - 2 * pad) * (d - min) / (max - min);
			if (i == 0) {
				area.append("M").append(fmt1(x)).append(",").append(fmt1(h - pad)).append(" L").append(fmt1(x)).append(",")
						.append(fmt1(y));
			} else {
				area.append(" L").append(fmt1(x)).append(",").append(fmt1(y));
				line.append(" ");
			}
			line.append(fmt1(x)).append(",").append(fmt1(y));
		}
		double lastX = pad + (w - 2 * pad);
		area.append(" L").append(fmt1(lastX)).append(",").append(fmt1(h - pad)).append(" Z");
		sb.append("<svg viewBox='0 0 300 70' preserveAspectRatio='none' style='width:100%;height:70px;'>");
		sb.append("<path d='").append(area).append("' fill='").append(color).append("' fill-opacity='0.12'/>");
		sb.append("<polyline points='").append(line).append("' fill='none' stroke='").append(color)
				.append("' stroke-width='2' stroke-linejoin='round' stroke-linecap='round'/>");
		sb.append("</svg>");
		sb.append(cardClose());
		return sb.toString();
	}

	// ============================================================
	// GARIS PROGRES / RADAR DATAR (label + persen)
	// ============================================================

	public static String progressLines(String title, String desc, LinkedHashMap<String, Integer> labelToPercent) {
		StringBuilder sb = new StringBuilder();
		sb.append(cardOpen(title, desc));
		if (labelToPercent == null || labelToPercent.isEmpty()) {
			sb.append(emptyBox("Belum ada indikator."));
		} else {
			int i = 0;
			for (Map.Entry<String, Integer> e : labelToPercent.entrySet()) {
				int v = clampPct(e.getValue() == null ? 0 : e.getValue());
				sb.append(progressLine(e.getKey(), v, color(i)));
				i++;
			}
		}
		sb.append(cardClose());
		return sb.toString();
	}

	private static String progressLine(String label, int percent, String color) {
		return "<div style='margin:8px 0;'>"
				+ "<div style='display:flex;justify-content:space-between;font-size:11px;color:" + INK
				+ ";font-weight:700;'><span>" + esc(label) + "</span><span>" + clampPct(percent) + "%</span></div>"
				+ "<div style='height:10px;background:#eef2f7;border-radius:999px;overflow:hidden;margin-top:3px;'>"
				+ "<div style='height:10px;width:" + clampPct(percent) + "%;background:" + color
				+ ";border-radius:999px;'></div></div></div>";
	}

	// ============================================================
	// INSIGHT (pasangan label-nilai, responsif)
	// ============================================================

	public static String insight(String title, String desc, LinkedHashMap<String, String> pairs) {
		StringBuilder sb = new StringBuilder();
		sb.append(cardOpen(title, desc));
		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:8px;'>");
		if (pairs != null) {
			for (Map.Entry<String, String> e : pairs.entrySet()) {
				sb.append("<div style='background:#f8fafc;border:1px solid ").append(LINE)
						.append(";border-radius:10px;padding:8px 10px;'>")
						.append("<div style='font-size:10px;color:").append(MUTED)
						.append(";text-transform:uppercase;font-weight:800;'>").append(esc(e.getKey())).append("</div>")
						.append("<div style='font-size:13px;color:").append(INK).append(";font-weight:700;margin-top:2px;'>")
						.append(esc(e.getValue())).append("</div></div>");
			}
		}
		sb.append("</div>");
		sb.append(cardClose());
		return sb.toString();
	}

	/**
	 * Panel analisis operasional berbasis aturan yang dapat dipakai ulang oleh seluruh dasbor
	 * pemantauan sistem. Pemanggil wajib mengirim bukti terukur pada {@code findings}; bagian
	 * {@code possibleCauses} sengaja diberi label "kemungkinan" agar hipotesis tidak ditampilkan
	 * sebagai fakta. Seluruh teks di-escape sebelum dirender.
	 */
	public static String smartAnalysis(String status, String summary, List<String> findings,
			List<String> possibleCauses, List<String> actions) {
		String normalized = status == null ? "INFORMASI" : status.trim().toUpperCase();
		String color = GOOD;
		String background = "#f0fdf4";
		if (normalized.indexOf("KRITIS") >= 0 || normalized.indexOf("TINGGI") >= 0) {
			color = BAD;
			background = "#fef2f2";
		} else if (normalized.indexOf("WASPADA") >= 0 || normalized.indexOf("PERHATIAN") >= 0) {
			color = WARN;
			background = "#fff7ed";
		} else if (normalized.indexOf("INFO") >= 0 || normalized.indexOf("BELUM") >= 0) {
			color = PRIMARY;
			background = "#eff6ff";
		}

		StringBuilder sb = new StringBuilder(5000);
		sb.append("<div style='background:#fff;border:1px solid ").append(LINE)
				.append(";border-left:5px solid ").append(color)
				.append(";border-radius:8px;padding:14px;margin:0 0 12px 0;box-shadow:0 6px 16px rgba(15,23,42,.06);'>")
				.append("<div style='display:flex;align-items:flex-start;justify-content:space-between;gap:10px;flex-wrap:wrap;'>")
				.append("<div><div style='font-size:15px;font-weight:900;color:").append(INK)
				.append(";'>Analisis Pintar</div><div style='font-size:11px;color:").append(MUTED)
				.append(";margin-top:3px;'>Kesimpulan otomatis dari data yang sedang ditampilkan.</div></div>")
				.append("<span style='background:").append(background).append(";color:").append(color)
				.append(";border:1px solid ").append(color)
				.append(";border-radius:999px;padding:5px 9px;font-size:10px;font-weight:900;'>")
				.append(esc(normalized)).append("</span></div>");

		if (summary != null && summary.trim().length() > 0) {
			sb.append("<div style='margin-top:10px;padding:9px 10px;background:").append(background)
					.append(";border-radius:6px;color:").append(INK)
					.append(";font-size:12px;font-weight:700;line-height:1.55;'>")
					.append(esc(summary)).append("</div>");
		}

		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:10px;margin-top:10px;'>");
		appendAnalysisList(sb, "Bukti dan Temuan", findings, "Belum ada temuan yang cukup untuk dinilai.");
		appendAnalysisList(sb, "Kemungkinan Penyebab", possibleCauses,
				"Tidak ada indikasi penyebab khusus dari data saat ini.");
		appendAnalysisList(sb, "Tindakan yang Disarankan", actions,
				"Lanjutkan pemantauan dan muat ulang data secara berkala.");
		sb.append("</div><div style='font-size:10px;color:").append(MUTED)
				.append(";margin-top:10px;line-height:1.45;'>Analisis bersifat diagnostik. Konfirmasi akar masalah melalui rincian data, log aplikasi, dan kondisi server pada waktu kejadian.</div></div>");
		return sb.toString();
	}

	private static void appendAnalysisList(StringBuilder sb, String title, List<String> values, String emptyText) {
		sb.append("<div style='background:#f8fafc;border:1px solid ").append(LINE)
				.append(";border-radius:7px;padding:10px;'>")
				.append("<div style='font-size:11px;font-weight:900;color:").append(INK).append(";'>")
				.append(esc(title)).append("</div><ol style='margin:7px 0 0 17px;padding:0;color:#334155;font-size:11px;line-height:1.55;'>");
		if (values == null || values.isEmpty()) {
			sb.append("<li>").append(esc(emptyText)).append("</li>");
		} else {
			for (String value : values) {
				if (value != null && value.trim().length() > 0) {
					sb.append("<li style='margin:3px 0;'>").append(esc(value)).append("</li>");
				}
			}
		}
		sb.append("</ol></div>");
	}

	// ============================================================
	// TABEL TREN BER-PAGING (komponen ZK, batang HTML/CSS)
	// ============================================================

	/**
	 * Tabel tren ber-paging (default 10 baris/halaman) dengan batang HTML/CSS — tanpa JFreeChart.
	 * {@code data} harus sudah terurut sesuai keinginan (mis. terbaru di atas). Cocok untuk tren
	 * harian/bulanan/semester yang datanya banyak sehingga perlu dibagi per halaman.
	 */
	public static Grid trendGrid(String labelCol, String valueCol, LinkedHashMap<String, Double> data, String barColor,
			boolean asMoney, int pageSize) {
		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setStyle("border:0;background:transparent;");
		grid.setMold("paging");
		grid.setPageSize(pageSize <= 0 ? 10 : pageSize);
		try {
			grid.getPagingChild().setMold("os");
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/ui/util/DashboardUiKit.java:667");
		}

		Columns cols = new Columns();
		cols.setParent(grid);
		Column c1 = new Column();
		c1.setLabel(labelCol);
		c1.setWidth("34%");
		c1.setParent(cols);
		Column c2 = new Column();
		c2.setLabel("");
		c2.setParent(cols);
		Column c3 = new Column();
		c3.setLabel(valueCol);
		c3.setAlign("right");
		c3.setWidth("26%");
		c3.setParent(cols);

		double max = 0;
		if (data != null) {
			for (Double v : data.values()) {
				double d = v == null ? 0 : v.doubleValue();
				if (d > max) {
					max = d;
				}
			}
		}

		Rows rows = new Rows();
		rows.setParent(grid);
		if (data == null || data.isEmpty()) {
			Row r = new Row();
			r.setParent(rows);
			Label l = new Label(ais.common.Common.getBahasaConfig("Belum ada data."));
			l.setStyle("font-size:11px;color:" + MUTED + ";");
			l.setParent(r);
			new Label("").setParent(r);
			new Label("").setParent(r);
		} else {
			for (Map.Entry<String, Double> e : data.entrySet()) {
				double v = e.getValue() == null ? 0 : e.getValue().doubleValue();
				int w = pct(v, max);
				Row r = new Row();
				r.setValign("middle");
				r.setParent(rows);
				Label lbl = new Label(e.getKey());
				lbl.setStyle("font-size:11px;color:" + INK + ";");
				lbl.setParent(r);
				Html bar = new Html("<div style='height:11px;background:#eef2f7;border-radius:999px;overflow:hidden;'>"
						+ "<div style='height:11px;width:" + w + "%;background:" + barColor
						+ ";border-radius:999px;'></div></div>");
				bar.setParent(r);
				Label val = new Label((asMoney ? "" : "") + money(v));
				val.setStyle("font-size:11px;font-weight:700;color:" + INK + ";");
				val.setParent(r);
			}
		}
		return grid;
	}

	// ============================================================
	// BAGAN ALUR / STEPPER (disposisi & alur persetujuan surat)
	// ============================================================

	/** Jenis status simpul bagan alur. */
	public static final int FLOW_DONE = 1;   // sudah ditindak-lanjuti / disetujui
	public static final int FLOW_REJECT = 2; // ditolak
	public static final int FLOW_WAIT = 3;   // belum / menunggu

	/** Satu simpul (node) pada bagan alur disposisi. */
	public static final class FlowStep {
		final String title;
		final int kind;
		final String statusText;
		final String pejabat;
		final String waktu;
		final String catatan;
		final String linkUrl;
		final String linkLabel;

		public FlowStep(String title, int kind, String statusText, String pejabat, String waktu, String catatan,
				String linkUrl, String linkLabel) {
			this.title = title;
			this.kind = kind;
			this.statusText = statusText;
			this.pejabat = pejabat;
			this.waktu = waktu;
			this.catatan = catatan;
			this.linkUrl = linkUrl;
			this.linkLabel = linkLabel;
		}
	}

	private static String flowColor(int kind) {
		if (kind == FLOW_DONE) {
			return GOOD;
		}
		if (kind == FLOW_REJECT) {
			return BAD;
		}
		return WARN;
	}

	private static String flowBg(int kind) {
		if (kind == FLOW_DONE) {
			return "#f0fdf4";
		}
		if (kind == FLOW_REJECT) {
			return "#fef2f2";
		}
		return "#fff7ed";
	}

	private static String flowLine(String icon, String text) {
		return "<div style='margin-top:4px;font-size:11px;color:" + MUTED
				+ ";display:flex;gap:5px;align-items:flex-start;'>" + "<span style='flex:0 0 auto;'>" + icon
				+ "</span><span style='flex:1;line-height:1.4;word-break:break-word;'>" + esc(text) + "</span></div>";
	}

	/**
	 * Bagan alur lengkap (dibungkus kartu) untuk panel seperti "Informasi Disposisi".
	 * Simpul horizontal bernomor + panah penghubung, warna mengikuti status, dan
	 * otomatis turun baris di layar HP (flex-wrap). HTML/CSS murni, tanpa JFreeChart.
	 */
	public static String flowBagan(String title, String desc, List<FlowStep> steps, String emptyMsg) {
		StringBuilder sb = new StringBuilder();
		sb.append(cardOpen(title, desc));
		if (steps == null || steps.isEmpty()) {
			sb.append(emptyBox(emptyMsg));
			sb.append(cardClose());
			return sb.toString();
		}
		sb.append("<div style='display:flex;flex-wrap:wrap;align-items:stretch;gap:8px;'>");
		int no = 1;
		for (int i = 0; i < steps.size(); i++) {
			FlowStep s = steps.get(i);
			if (i > 0) {
				sb.append("<div style='display:flex;align-items:center;color:#94a3b8;font-size:16px;font-weight:900;'>&rarr;</div>");
			}
			String c = flowColor(s.kind);
			sb.append("<div style='flex:1 1 200px;min-width:165px;max-width:300px;background:").append(flowBg(s.kind))
					.append(";border:1px solid ").append(LINE).append(";border-left:4px solid ").append(c)
					.append(";border-radius:12px;padding:9px 11px;box-shadow:0 2px 8px rgba(15,23,42,.05);box-sizing:border-box;'>");
			sb.append("<div style='display:flex;align-items:center;gap:7px;'>")
					.append("<span style='flex:0 0 auto;width:20px;height:20px;border-radius:999px;background:").append(c)
					.append(";color:#fff;font-size:10px;font-weight:900;display:inline-flex;align-items:center;justify-content:center;'>")
					.append(no).append("</span>")
					.append("<span style='flex:1;font-size:12px;font-weight:800;color:").append(INK)
					.append(";overflow:hidden;text-overflow:ellipsis;white-space:nowrap;'>").append(esc(s.title))
					.append("</span></div>");
			sb.append("<div style='display:inline-block;margin-top:6px;font-size:10px;font-weight:800;color:").append(c)
					.append(";background:#fff;border:1px solid ").append(LINE).append(";border-radius:999px;padding:2px 8px;'>")
					.append(esc(s.statusText)).append("</div>");
			if (s.pejabat != null && s.pejabat.trim().length() > 0) {
				sb.append(flowLine("&#128100;", s.pejabat));
			}
			if (s.waktu != null && s.waktu.trim().length() > 0) {
				sb.append(flowLine("&#128340;", s.waktu));
			}
			if (s.catatan != null && s.catatan.trim().length() > 0) {
				sb.append("<div style='margin-top:5px;font-size:11px;color:").append(INK)
						.append(";background:#fff;border:1px dashed ").append(LINE)
						.append(";border-radius:8px;padding:5px 7px;line-height:1.45;word-break:break-word;'>&#128172; ")
						.append(esc(s.catatan)).append("</div>");
			}
			if (s.linkUrl != null && s.linkUrl.trim().length() > 0) {
				sb.append("<div style='margin-top:5px;font-size:11px;'>&#128206; <a href='").append(esc(s.linkUrl))
						.append("' target='_blank' style='color:").append(PRIMARY)
						.append(";text-decoration:none;font-weight:700;'>")
						.append(esc(s.linkLabel == null || s.linkLabel.trim().isEmpty() ? "Lampiran" : s.linkLabel))
						.append("</a></div>");
			}
			sb.append("</div>");
			no++;
		}
		sb.append("</div>");
		sb.append(cardClose());
		return sb.toString();
	}

	/**
	 * Versi ringkas satu baris (untuk sel grid / daftar): pil bernomor + status,
	 * panah antar pil, detail muncul saat kursor diarahkan (tooltip). Reusable.
	 */
	public static String flowBaganInline(List<FlowStep> steps, String emptyMsg) {
		if (steps == null || steps.isEmpty()) {
			return "<span style='font-size:10px;color:" + MUTED
					+ ";background:#f8fafc;border:1px dashed #cbd5e1;border-radius:999px;padding:3px 9px;'>"
					+ esc(emptyMsg == null || emptyMsg.trim().isEmpty() ? "Belum ada disposisi" : emptyMsg) + "</span>";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:flex;align-items:center;gap:5px;flex-wrap:wrap;line-height:1.1;'>");
		int no = 1;
		for (int i = 0; i < steps.size(); i++) {
			FlowStep s = steps.get(i);
			if (i > 0) {
				sb.append("<span style='color:#94a3b8;font-size:12px;font-weight:900;'>&rarr;</span>");
			}
			String c = flowColor(s.kind);
			String tip = esc(s.title + " — " + s.statusText
					+ (s.pejabat == null || s.pejabat.trim().isEmpty() ? "" : " (" + s.pejabat + ")")
					+ (s.waktu == null || s.waktu.trim().isEmpty() ? "" : " " + s.waktu));
			sb.append("<span title='").append(tip)
					.append("' style='display:inline-flex;align-items:center;gap:5px;max-width:220px;padding:3px 8px;border-radius:999px;background:")
					.append(flowBg(s.kind)).append(";border:1px solid ").append(LINE)
					.append(";font-size:10px;font-weight:800;color:").append(INK).append(";white-space:nowrap;overflow:hidden;'>")
					.append("<span style='flex:0 0 auto;width:16px;height:16px;border-radius:999px;background:").append(c)
					.append(";color:#fff;font-size:9px;font-weight:900;display:inline-flex;align-items:center;justify-content:center;'>")
					.append(no).append("</span>")
					.append("<span style='overflow:hidden;text-overflow:ellipsis;white-space:nowrap;'>").append(esc(s.title))
					.append("</span></span>");
			no++;
		}
		sb.append("</div>");
		return sb.toString();
	}

	// ============================================================
	// PEMBUNGKUS KARTU INTERNAL
	// ============================================================

	/**
	 * Peta panas (heatmap) 2 dimensi baris &times; kolom; intensitas warna mengikuti nilai (makin
	 * pekat = makin besar). Cocok untuk pola "hari &times; jam" (kapan paling ramai). HTML/CSS murni,
	 * responsif (menggulir mendatar di layar sempit). Sel kosong tampil abu muda; nilai muncul di tooltip.
	 *
	 * @param rowLabels label baris (mis. nama hari)
	 * @param colLabels label kolom (mis. jam)
	 * @param values    matriks {@code [baris][kolom]}
	 * @param baseColor warna dasar sel (boleh konstanta tema; hex di dalamnya yang dipakai)
	 */
	public static String heatmap(String title, String desc, String[] rowLabels, String[] colLabels,
			double[][] values, String baseColor, String emptyMsg) {
		StringBuilder sb = new StringBuilder();
		sb.append(cardOpen(title, desc));
		double max = 0;
		boolean ada = false;
		if (values != null) {
			for (int r = 0; r < values.length; r++) {
				if (values[r] == null) {
					continue;
				}
				for (int c = 0; c < values[r].length; c++) {
					if (values[r][c] > max) {
						max = values[r][c];
					}
					if (values[r][c] > 0) {
						ada = true;
					}
				}
			}
		}
		if (!ada || rowLabels == null || colLabels == null) {
			sb.append(emptyBox(emptyMsg));
			sb.append(cardClose());
			return sb.toString();
		}
		String hex = heatBase(baseColor);
		sb.append("<div style='overflow-x:auto;'>");
		sb.append("<table style='border-collapse:separate;border-spacing:2px;font-size:10px;'>");
		sb.append("<tr><td></td>");
		for (int c = 0; c < colLabels.length; c++) {
			sb.append("<td style='text-align:center;color:").append(MUTED).append(";font-weight:700;'>")
					.append(esc(colLabels[c])).append("</td>");
		}
		sb.append("</tr>");
		for (int r = 0; r < rowLabels.length; r++) {
			sb.append("<tr><td style='color:").append(MUTED).append(";font-weight:700;padding-right:6px;white-space:nowrap;'>")
					.append(esc(rowLabels[r])).append("</td>");
			double[] rowv = (r < values.length) ? values[r] : null;
			for (int c = 0; c < colLabels.length; c++) {
				double v = (rowv != null && c < rowv.length) ? rowv[c] : 0;
				double ratio = max <= 0 ? 0 : v / max;
				String bg = v <= 0 ? "#f1f5f9" : rgba(hex, 0.12 + 0.88 * ratio);
				sb.append("<td title='").append(esc(rowLabels[r])).append(" ").append(esc(colLabels[c])).append(": ")
						.append(money(v)).append("' style='width:20px;height:18px;border-radius:4px;background:")
						.append(bg).append(";'></td>");
			}
			sb.append("</tr>");
		}
		sb.append("</table></div>");
		sb.append("<div style='display:flex;align-items:center;gap:6px;margin-top:8px;font-size:10px;color:" + MUTED + ";'>")
				.append("sedikit <span style='flex:0 0 90px;height:10px;border-radius:6px;background:linear-gradient(90deg,")
				.append(rgba(hex, 0.12)).append(",").append(rgba(hex, 1.0)).append(");'></span> banyak</div>");
		sb.append(cardClose());
		return sb.toString();
	}

	private static String heatBase(String c) {
		if (c == null) {
			return "#2563eb";
		}
		int h = c.indexOf('#');
		if (h >= 0 && c.length() >= h + 7) {
			return c.substring(h, h + 7);
		}
		return "#2563eb";
	}

	private static String rgba(String hex, double alpha) {
		try {
			int r = Integer.parseInt(hex.substring(1, 3), 16);
			int g = Integer.parseInt(hex.substring(3, 5), 16);
			int b = Integer.parseInt(hex.substring(5, 7), 16);
			double a = alpha < 0 ? 0 : (alpha > 1 ? 1 : alpha);
			return "rgba(" + r + "," + g + "," + b + "," + String.format(java.util.Locale.US, "%.2f", a) + ")";
		} catch (Exception e) {
			return hex;
		}
	}

	private static String cardOpen(String title, String desc) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class='ais-dashboard-clickable' tabindex='0' data-ais-title='")
				.append(esc(title == null ? "Detail Analitik" : title)).append("' data-ais-hint='")
				.append(esc(desc == null ? "" : desc))
				.append("' onclick='if(window.aisDashboardCardDetail){window.aisDashboardCardDetail(this,event);}' ")
				.append("onkeydown='if((event.keyCode==13||event.keyCode==32)&amp;&amp;window.aisDashboardCardDetail){window.aisDashboardCardDetail(this,event);}' ")
				.append("style='cursor:pointer;background:#fff;border:1px solid ").append(LINE)
				.append(";border-radius:12px;padding:12px;height:100%;box-sizing:border-box;'>");
		if (title != null && title.trim().length() > 0) {
			sb.append("<div style='font-weight:800;color:").append(INK).append(";font-size:13px;'>").append(esc(title))
					.append("</div>");
		}
		if (desc != null && desc.trim().length() > 0) {
			sb.append("<div style='font-size:11px;color:").append(MUTED).append(";margin:2px 0 8px 0;line-height:1.45;'>")
					.append(esc(desc)).append("</div>");
		}
		return sb.toString();
	}

	private static String cardClose() {
		return "</div>";
	}

	private static String emptyBox(String msg) {
		return "<div style='font-size:12px;color:" + MUTED + ";background:#f8fafc;border:1px dashed #cbd5e1;"
				+ "border-radius:8px;padding:12px;text-align:center;'>"
				+ esc(msg == null || msg.trim().isEmpty() ? "Belum ada data." : msg) + "</div>";
	}

	private static String legend(String[] labels, String[] colors) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:flex;flex-wrap:wrap;gap:12px;margin-bottom:6px;'>");
		for (int i = 0; i < labels.length; i++) {
			sb.append("<span style='display:inline-flex;align-items:center;gap:5px;font-size:11px;color:").append(MUTED)
					.append(";'><span style='width:10px;height:10px;border-radius:3px;background:").append(colors[i])
					.append(";'></span>").append(esc(labels[i])).append("</span>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	/** Potong teks panjang & beri elipsis (dipakai juga oleh dasbor lain). */
	public static String shorten(String s, int max) {
		if (s == null) {
			return "";
		}
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}

	private static String fmt1(double v) {
		return String.valueOf(Math.round(v * 10.0) / 10.0);
	}

	// ============================================================
	// GRAFIK GARIS GANDA (2 seri) — tanpa JFreeChart
	// ============================================================

	/**
	 * Grafik garis SVG dua seri (misal: Memori Bebas vs Terpakai).
	 * @param title  judul kartu
	 * @param desc   deskripsi singkat
	 * @param labels label sumbu-X (boleh null)
	 * @param s1     data seri 1
	 * @param label1 nama seri 1
	 * @param color1 warna seri 1 (hex/css)
	 * @param s2     data seri 2 (boleh null)
	 * @param label2 nama seri 2
	 * @param color2 warna seri 2 (hex/css)
	 */
	public static String dualLineChart(String title, String desc,
			List<String> labels,
			List<? extends Number> s1, String label1, String color1,
			List<? extends Number> s2, String label2, String color2) {
		int n = (s1 == null ? 0 : s1.size());
		if (n < 2) {
			StringBuilder empty = new StringBuilder();
			empty.append(cardOpen(title, desc));
			empty.append(emptyBox("Belum ada data"));
			empty.append(cardClose());
			return empty.toString();
		}

		double maxV = 0, minV = Double.MAX_VALUE;
		for (int i = 0; i < n; i++) {
			if (s1.get(i) != null) {
				maxV = Math.max(maxV, s1.get(i).doubleValue());
				minV = Math.min(minV, s1.get(i).doubleValue());
			}
			if (s2 != null && s2.size() > i && s2.get(i) != null) {
				maxV = Math.max(maxV, s2.get(i).doubleValue());
				minV = Math.min(minV, s2.get(i).doubleValue());
			}
		}
		if (maxV <= minV) {
			maxV = minV + 1;
		}

		double W = 500, H = 150, padL = 46, padR = 8, padT = 14, padB = 28;
		double chartW = W - padL - padR;
		double chartH = H - padT - padB;

		StringBuilder pts1 = new StringBuilder();
		StringBuilder pts2 = new StringBuilder();
		for (int i = 0; i < n; i++) {
			double x = padL + chartW * i / (n - 1 < 1 ? 1 : n - 1);
			double v1 = s1.get(i) == null ? 0 : s1.get(i).doubleValue();
			double y1 = padT + chartH - chartH * (v1 - minV) / (maxV - minV);
			if (i > 0) {
				pts1.append(" ");
			}
			pts1.append(fmt1(x)).append(",").append(fmt1(y1));

			if (s2 != null && s2.size() > i) {
				double v2 = s2.get(i) == null ? 0 : s2.get(i).doubleValue();
				double y2 = padT + chartH - chartH * (v2 - minV) / (maxV - minV);
				if (i > 0) {
					pts2.append(" ");
				}
				pts2.append(fmt1(x)).append(",").append(fmt1(y2));
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append(cardOpen(title, desc));
		sb.append(legend(new String[] { label1, label2 }, new String[] { color1, color2 }));
		sb.append("<svg viewBox='0 0 500 150' preserveAspectRatio='none' style='width:100%;height:150px;display:block;'>");

		double gridY1 = padT + chartH * 0.25;
		double gridY2 = padT + chartH * 0.5;
		double gridY3 = padT + chartH * 0.75;
		for (double gy : new double[] { padT, gridY1, gridY2, gridY3, padT + chartH }) {
			sb.append("<line x1='").append(fmt1(padL)).append("' y1='").append(fmt1(gy))
				.append("' x2='").append(fmt1(W - padR)).append("' y2='").append(fmt1(gy))
				.append("' stroke='#e2e8f0' stroke-width='0.8'/>");
		}

		sb.append("<line x1='").append(fmt1(padL)).append("' y1='").append(fmt1(padT))
			.append("' x2='").append(fmt1(padL)).append("' y2='").append(fmt1(padT + chartH))
			.append("' stroke='#e2e8f0' stroke-width='1'/>");

		sb.append("<text x='").append(fmt1(padL - 3)).append("' y='").append(fmt1(padT + 4))
			.append("' font-size='8' fill='#94a3b8' text-anchor='end'>").append(fmt1(maxV)).append("</text>");
		sb.append("<text x='").append(fmt1(padL - 3)).append("' y='").append(fmt1(padT + chartH))
			.append("' font-size='8' fill='#94a3b8' text-anchor='end'>").append(fmt1(minV)).append("</text>");

		if (pts2.length() > 0) {
			sb.append("<polyline points='").append(pts2)
				.append("' fill='none' stroke='").append(color2)
				.append("' stroke-width='1.5' stroke-linejoin='round' stroke-linecap='round' stroke-dasharray='5,3'/>");
		}
		sb.append("<polyline points='").append(pts1)
			.append("' fill='none' stroke='").append(color1)
			.append("' stroke-width='2' stroke-linejoin='round' stroke-linecap='round'/>");

		if (labels != null && labels.size() >= 2) {
			String first = labels.get(0);
			String last = labels.get(labels.size() - 1);
			sb.append("<text x='").append(fmt1(padL)).append("' y='").append(fmt1(H - 2))
				.append("' font-size='8' fill='#94a3b8' text-anchor='start'>").append(esc(first)).append("</text>");
			sb.append("<text x='").append(fmt1(W - padR)).append("' y='").append(fmt1(H - 2))
				.append("' font-size='8' fill='#94a3b8' text-anchor='end'>").append(esc(last)).append("</text>");
		}

		sb.append("</svg>");
		sb.append(cardClose());
		return sb.toString();
	}
}
