package ais.ui.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ais.common.Common;

/**
 * Ringkasan visual draft jurnal berbasis HTML/CSS ringan.
 * Tidak memakai JFreeChart agar aman untuk ZK 5.5 dan mudah dipakai ulang di dashboard lain.
 */
public class DraftJurnalDashboardUtil {

	public static class Stat {
		private String nama;
		private int draft;
		private int posted;
		private int closing;

		public Stat(String nama, int draft, int posted, int closing) {
			this.nama = nama == null ? "" : nama;
			this.draft = draft;
			this.posted = posted;
			this.closing = closing;
		}

		public String getNama() { return nama; }
		public int getDraft() { return draft; }
		public int getPosted() { return posted; }
		public int getClosing() { return closing; }
	}

	public static String buildDraftSummary(List<Stat> stats, String periode) {
		if (stats == null) {
			stats = new ArrayList<Stat>();
		}
		int totalDraft = 0;
		int totalPosted = 0;
		int totalClosing = 0;
		for (int i = 0; i < stats.size(); i++) {
			Stat s = stats.get(i);
			if (s != null) {
				totalDraft += s.getDraft();
				totalPosted += s.getPosted();
				totalClosing += s.getClosing();
			}
		}
		int total = totalDraft + totalPosted + totalClosing;
		int siap = totalDraft + totalPosted;
		int pctClosing = percent(totalClosing, total == 0 ? 1 : total);
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-family:Arial,sans-serif;color:#0f172a;'>");
		sb.append("<div style='border-radius:18px;overflow:hidden;background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);padding:18px 20px;color:white;box-shadow:0 14px 30px rgba(15,23,42,.18);'>");
		sb.append("<div style='font-size:12px;letter-spacing:.12em;text-transform:uppercase;opacity:.86;'>Draft Jurnal Akunting</div>");
		sb.append("<div style='font-size:25px;font-weight:900;margin-top:4px;'>Ringkasan kesiapan posting jurnal</div>");
		sb.append("<div style='font-size:12px;line-height:1.45;opacity:.92;margin-top:7px;'>Menunjukkan jurnal yang masih perlu diposting, jurnal yang sudah diposting, dan jurnal yang sudah masuk closing. Data ini membantu petugas menentukan pekerjaan yang perlu diselesaikan lebih dulu.</div>");
		sb.append("<div style='font-size:12px;margin-top:8px;opacity:.86;'>Periode: ").append(escape(periode)).append("</div>");
		sb.append("</div>");

		sb.append("<div style='display:flex;flex-wrap:wrap;gap:10px;margin-top:12px;'>");
		appendCard(sb, "Draft", totalDraft, "Belum diposting");
		appendCard(sb, "Terposting", totalPosted, "Sudah menjadi jurnal");
		appendCard(sb, "Closing", totalClosing, "Sudah dikunci periode");
		appendCard(sb, "Total Aktivitas", total, "Semua status");
		sb.append("</div>");

		sb.append("<div style='margin-top:12px;background:#ffffff;border:1px solid #e2e8f0;border-radius:16px;padding:14px;box-shadow:0 10px 24px rgba(15,23,42,.05);'>");
		sb.append("<div style='font-size:14px;font-weight:800;'>Kesiapan closing</div>");
		sb.append("<div style='font-size:12px;color:#64748b;margin-top:4px;'>Semakin tinggi bagian closing, semakin banyak jurnal periode ini yang sudah selesai dikunci.</div>");
		sb.append("<div style='margin-top:10px;height:12px;border-radius:999px;background:#e2e8f0;overflow:hidden;'>");
		sb.append("<div style='height:12px;width:").append(pctClosing).append("%;background:linear-gradient(90deg,#22c55e,#06b6d4);border-radius:999px;'></div>");
		sb.append("</div><div style='font-size:11px;color:#64748b;margin-top:5px;'>").append(pctClosing).append("% aktivitas berada pada status closing.</div>");
		sb.append("</div>");

		sb.append("<div style='display:flex;flex-wrap:wrap;gap:12px;margin-top:12px;'>");
		sb.append("<div style='flex:1 1 520px;background:#ffffff;border:1px solid #e2e8f0;border-radius:16px;padding:14px;box-shadow:0 10px 24px rgba(15,23,42,.05);'>");
		sb.append("<div style='font-size:14px;font-weight:800;'>Pekerjaan terbanyak</div>");
		sb.append("<div style='font-size:12px;color:#64748b;margin-top:4px;'>Urutan ini membantu memilih modul yang perlu dicek lebih dulu.</div>");
		appendTopBars(sb, stats);
		sb.append("</div>");
		sb.append("<div style='flex:1 1 320px;background:#ffffff;border:1px solid #e2e8f0;border-radius:16px;padding:14px;box-shadow:0 10px 24px rgba(15,23,42,.05);'>");
		sb.append("<div style='font-size:14px;font-weight:800;'>Radar status</div>");
		sb.append("<div style='font-size:12px;color:#64748b;margin-top:4px;'>Perbandingan draft, terposting, dan closing dalam satu tampilan sederhana.</div>");
		appendRadar(sb, totalDraft, totalPosted, totalClosing);
		sb.append("</div></div>");
		sb.append("</div>");
		return sb.toString();
	}

	private static void appendCard(StringBuilder sb, String title, int value, String desc) {
		sb.append("<div style='flex:1 1 190px;background:#ffffff;border:1px solid #e2e8f0;border-radius:16px;padding:14px;box-shadow:0 10px 24px rgba(15,23,42,.05);'>");
		sb.append("<div style='font-size:12px;color:#64748b;font-weight:700;'>").append(escape(title)).append("</div>");
		sb.append("<div style='font-size:25px;font-weight:900;color:#0f172a;margin-top:3px;'>").append(format(value)).append("</div>");
		sb.append("<div style='font-size:11px;color:#64748b;margin-top:2px;'>").append(escape(desc)).append("</div>");
		sb.append("</div>");
	}

	private static void appendTopBars(StringBuilder sb, List<Stat> stats) {
		List<Stat> copy = new ArrayList<Stat>();
		copy.addAll(stats);
		Collections.sort(copy, new Comparator<Stat>() {
			public int compare(Stat a, Stat b) {
				int va = a == null ? 0 : a.getDraft() + a.getPosted() + a.getClosing();
				int vb = b == null ? 0 : b.getDraft() + b.getPosted() + b.getClosing();
				return vb - va;
			}
		});
		int max = 1;
		for (int i = 0; i < copy.size(); i++) {
			Stat s = copy.get(i);
			int v = s == null ? 0 : s.getDraft() + s.getPosted() + s.getClosing();
			if (v > max) max = v;
		}
		int limit = copy.size() > 10 ? 10 : copy.size();
		if (limit == 0) {
			sb.append("<div style='font-size:12px;color:#64748b;margin-top:10px;'>Belum ada data pada periode ini.</div>");
			return;
		}
		for (int i = 0; i < limit; i++) {
			Stat s = copy.get(i);
			if (s == null) continue;
			int value = s.getDraft() + s.getPosted() + s.getClosing();
			int width = percent(value, max);
			sb.append("<div style='margin-top:10px;'>");
			sb.append("<div style='display:flex;justify-content:space-between;gap:8px;font-size:12px;'><span style='font-weight:700;'>").append(escape(s.getNama())).append("</span><span>").append(format(value)).append("</span></div>");
			sb.append("<div style='margin-top:4px;height:9px;background:#e2e8f0;border-radius:999px;overflow:hidden;'><div style='height:9px;width:").append(width).append("%;background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));border-radius:999px;'></div></div>");
			sb.append("</div>");
		}
	}

	private static void appendRadar(StringBuilder sb, int draft, int posted, int closing) {
		int max = Math.max(1, Math.max(draft, Math.max(posted, closing)));
		int d = percent(draft, max);
		int p = percent(posted, max);
		int c = percent(closing, max);
		sb.append("<div style='position:relative;margin:18px auto 8px;width:220px;height:220px;border-radius:999px;background:radial-gradient(circle,#ffffff 0 34%,#e2e8f0 35% 36%,#ffffff 37% 62%,#e2e8f0 63% 64%,#ffffff 65%);border:1px solid #e2e8f0;'>");
		sb.append("<div style='position:absolute;left:50%;top:50%;width:3px;height:").append(d).append("px;background:#2563eb;transform-origin:bottom center;transform:translate(-50%,-100%) rotate(0deg);border-radius:999px;'></div>");
		sb.append("<div style='position:absolute;left:50%;top:50%;width:3px;height:").append(p).append("px;background:#0891b2;transform-origin:bottom center;transform:translate(-50%,-100%) rotate(120deg);border-radius:999px;'></div>");
		sb.append("<div style='position:absolute;left:50%;top:50%;width:3px;height:").append(c).append("px;background:#22c55e;transform-origin:bottom center;transform:translate(-50%,-100%) rotate(240deg);border-radius:999px;'></div>");
		sb.append("<div style='position:absolute;left:50%;top:50%;width:76px;height:76px;margin-left:-38px;margin-top:-38px;border-radius:999px;background:#f8fafc;border:1px solid #e2e8f0;text-align:center;font-size:11px;color:#64748b;display:flex;align-items:center;justify-content:center;'>Status<br/>Jurnal</div>");
		sb.append("</div>");
		sb.append("<div style='font-size:11px;color:#64748b;line-height:1.6;'>Draft: ").append(format(draft)).append("<br/>Terposting: ").append(format(posted)).append("<br/>Closing: ").append(format(closing)).append("</div>");
	}

	private static int percent(int value, int max) {
		if (max <= 0) return 0;
		int p = (int) Math.round((value * 100.0) / max);
		return p < 0 ? 0 : (p > 100 ? 100 : p);
	}

	private static String format(int value) {
		try { return Common.numberFormat.get().format(value); } catch (Exception e) { return String.valueOf(value); }
	}

	private static String escape(String value) {
		if (value == null) return "";
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}
}
