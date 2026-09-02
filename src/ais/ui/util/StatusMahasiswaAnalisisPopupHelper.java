package ais.ui.util;

import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Html;

import ais.action.master.helper.HistoryStatusMahasiswaUtil.AnalisisStatusMahasiswa;
import ais.common.Common;

/**
 * Renderer reusable untuk link dan popup analisis status mahasiswa. Kelas ini hanya mengurus UI;
 * seluruh keputusan bisnis berasal dari snapshot {@link AnalisisStatusMahasiswa} yang dibangun
 * {@code HistoryStatusMahasiswaUtil}. Pemisahan ini mencegah halaman pembayaran, profil, atau
 * halaman lain membuat tafsir status sendiri yang dapat berbeda dari mesin status kanonik.
 */
public final class StatusMahasiswaAnalisisPopupHelper {

	private StatusMahasiswaAnalisisPopupHelper() {
	}

	/** Memasang perilaku link pada teks ringkasan di dalam tanda kurung. */
	public static void pasangLink(final A link, final AnalisisStatusMahasiswa analisis) {
		if (link == null || analisis == null) return;
		link.setLabel("(" + analisis.getRingkasan() + ")");
		link.setTooltiptext("Klik untuk melihat analisis rinci status " + analisis.getStatusNama());
		link.setStyle("color:#0b63ce;text-decoration:underline;cursor:pointer;font-weight:600;"
				+ "white-space:normal;line-height:1.4;");
		link.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				tampilkan(analisis);
			}
		});
	}

	/** Menampilkan snapshot analisis sebagai dialog modal yang dapat digulir. */
	public static void tampilkan(AnalisisStatusMahasiswa analisis) throws Exception {
		if (analisis == null) return;
		MyWindow window = new MyWindow("Analisis Status Mahasiswa", "normal", true);
		window.setWidth(Common.isMobile() ? "100%" : "760px");
		window.setHeight(Common.isMobile() ? "92%" : "680px");
		window.setSizable(true);
		window.setContentStyle("overflow:auto;background:#f8fafc;padding:0;");
		window.appendChild(new Html(buatHtml(analisis)));
		if (window.getPage() == null && Executions.getCurrent() != null
				&& Executions.getCurrent().getDesktop() != null
				&& Executions.getCurrent().getDesktop().getFirstPage() != null) {
			window.setPage(Executions.getCurrent().getDesktop().getFirstPage());
		}
		window.doModal();
	}

	private static String buatHtml(AnalisisStatusMahasiswa analisis) {
		StringBuilder html = new StringBuilder();
		html.append("<div style='font-family:Arial,sans-serif;color:#172033;font-size:13px;line-height:1.5;'>");
		html.append("<div style='background:#174a7e;color:white;padding:16px 18px;'>")
				.append("<div style='font-size:19px;font-weight:bold;'>Analisis Status Mahasiswa</div>")
				.append("<div style='font-size:12px;margin-top:3px;opacity:.94;'>Analisis membaca history status, semester, KRS/SKS, cuti, status keluar, paksa aktif, bypass, tagihan syarat aktif, dan cicilan committed.</div>")
				.append("</div><div style='padding:16px 18px;'>");

		html.append("<div style='background:#eef6ff;border:1px solid #b9d8f5;border-radius:8px;padding:13px;margin-bottom:12px;'>")
				.append("<div style='font-size:15px;font-weight:bold;color:#123e6b;'>Status: ")
				.append(esc(analisis.getStatusNama())).append("</div>")
				.append("<div style='margin-top:5px;'><b>Kesimpulan cerdas:</b> ")
				.append(esc(analisis.getRingkasan())).append(".</div></div>");

		html.append("<div style='background:white;border:1px solid #d7e0ea;border-radius:8px;padding:12px;margin-bottom:12px;'>")
				.append("<div style='font-weight:bold;margin-bottom:8px;'>Fakta yang Dibaca Sistem</div>")
				.append("<table style='width:100%;border-collapse:collapse;'>");
		for (Map.Entry<String, String> entry : analisis.getFakta().entrySet()) {
			html.append("<tr><td style='width:38%;padding:6px 8px;border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;'>")
					.append(esc(entry.getKey())).append("</td><td style='padding:6px 8px;border:1px solid #e2e8f0;'>")
					.append(esc(entry.getValue())).append("</td></tr>");
		}
		html.append("</table></div>");

		tambahDaftar(html, "Temuan Utama", analisis.getTemuan(), "#fff7ed", "#fed7aa", "#9a3412");
		tambahDaftar(html, "Rincian Tagihan Syarat Aktif", analisis.getRincianPembayaran(),
				"#f0fdf4", "#bbf7d0", "#166534");
		tambahDaftar(html, "Jejak Keputusan Algoritma", analisis.getJejakAturan(),
				"#f8fafc", "#cbd5e1", "#334155");
		tambahDaftar(html, "Tindakan yang Disarankan", analisis.getSaran(),
				"#fefce8", "#fde68a", "#854d0e");

		html.append("<div style='font-size:11px;color:#64748b;margin-top:10px;'>")
				.append("Catatan: analisis menjelaskan bukti yang tersedia saat halaman dihitung. Setelah pembayaran, perubahan KRS, persetujuan cuti, atau koreksi status, gunakan Refresh agar history dihitung ulang.")
				.append("</div></div></div>");
		return html.toString();
	}

	private static void tambahDaftar(StringBuilder html, String judul, List<String> values,
			String background, String border, String color) {
		html.append("<div style='background:").append(background).append(";border:1px solid ")
				.append(border).append(";border-radius:8px;padding:12px;margin-bottom:12px;color:")
				.append(color).append(";'><div style='font-weight:bold;margin-bottom:7px;'>")
				.append(esc(judul)).append("</div>");
		if (values == null || values.isEmpty()) {
			html.append("<div>Tidak ada data tambahan pada bagian ini.</div>");
		} else {
			html.append("<ol style='margin:0;padding-left:21px;'>");
			for (String value : values) html.append("<li style='margin:4px 0;'>").append(esc(value)).append("</li>");
			html.append("</ol>");
		}
		html.append("</div>");
	}

	private static String esc(Object value) {
		String result = value == null ? "-" : value.toString();
		return result.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;");
	}
}
