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
				.append("<div style='font-size:12px;margin-top:3px;opacity:.94;'>Penjelasan keputusan status berdasarkan bukti aktual yang dibaca sistem.</div>")
				.append("</div><div style='padding:16px 18px;'>");

		html.append("<div style='background:#eef6ff;border:1px solid #8fc1ee;border-left:5px solid #1769aa;border-radius:6px;padding:14px 15px;margin-bottom:12px;'>")
				.append("<div style='font-size:12px;font-weight:bold;color:#315b7d;text-transform:uppercase;'>Jawaban Singkat</div>")
				.append("<div style='font-size:17px;font-weight:bold;color:#123e6b;margin-top:3px;'>Status: ")
				.append(esc(analisis.getStatusNama())).append("</div>")
				.append("<div style='font-size:14px;margin-top:7px;font-weight:bold;'>")
				.append(esc(analisis.getKeputusanUtama())).append("</div>")
				.append("<div style='margin-top:7px;color:#334155;'><b>Artinya:</b> ")
				.append(esc(analisis.getArtiBagiPengguna())).append("</div></div>");

		tambahDaftarJikaAda(html, "Penghambat Utama", analisis.getPenghambatUtama(),
				"#fff1f2", "#fda4af", "#9f1239");
		tambahDaftarJikaAda(html, "Yang Sudah Benar dan Bukan Penyebab", analisis.getKondisiTerpenuhi(),
				"#f0fdf4", "#86efac", "#166534");
		tambahDaftarJikaAda(html, "Perhatian Tambahan", analisis.getPerhatianTambahan(),
				"#fffbeb", "#fcd34d", "#854d0e");
		tambahDaftar(html, "Yang Harus Dilakukan Sekarang", analisis.getSaran(),
				"#eff6ff", "#93c5fd", "#1e3a8a");

		html.append("<div style='font-size:14px;font-weight:bold;color:#334155;margin:18px 0 8px;'>Bukti dan Cara Sistem Memutuskan</div>");
		tambahDaftar(html, "Rincian Tagihan Syarat Aktif", analisis.getRincianPembayaran(),
				"#f8fafc", "#cbd5e1", "#334155");

		html.append("<div style='background:white;border:1px solid #d7e0ea;border-radius:8px;padding:12px;margin-bottom:12px;'>")
				.append("<div style='font-weight:bold;margin-bottom:8px;'>Fakta yang Dibaca Sistem</div>")
				.append("<table style='width:100%;border-collapse:collapse;'>");
		for (Map.Entry<String, String> entry : analisis.getFakta().entrySet()) {
			html.append("<tr><td style='width:38%;padding:6px 8px;border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;'>")
					.append(esc(entry.getKey())).append("</td><td style='padding:6px 8px;border:1px solid #e2e8f0;'>")
					.append(esc(entry.getValue())).append("</td></tr>");
		}
		html.append("</table></div>");

		tambahDaftar(html, "Penjelasan Pendukung", analisis.getTemuan(), "#fff7ed", "#fed7aa", "#9a3412");
		tambahDaftar(html, "Jejak Keputusan Algoritma", analisis.getJejakAturan(),
				"#f8fafc", "#cbd5e1", "#334155");

		html.append("<div style='font-size:11px;color:#64748b;margin-top:10px;'>")
				.append("Catatan: persentase pembayaran menunjukkan bukti pembayaran yang diakui terhadap tagihan, bukan selalu syarat pelunasan 100%. Analisis memakai cicilan yang sudah committed di database. Setelah pembayaran, perubahan KRS, persetujuan cuti, atau koreksi status, gunakan Refresh agar history dihitung ulang.")
				.append("</div></div></div>");
		return html.toString();
	}

	private static void tambahDaftarJikaAda(StringBuilder html, String judul, List<String> values,
			String background, String border, String color) {
		if (values == null || values.isEmpty()) return;
		tambahDaftar(html, judul, values, background, border, color);
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
