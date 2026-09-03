package ais.ui.util;

import java.text.DecimalFormat;
import java.util.List;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Html;

import ais.action.master.helper.KrsDetailHelper;
import ais.action.master.helper.KrsMahasiswaAnalisisHelper;
import ais.action.master.helper.KrsMahasiswaAnalisisHelper.AnalisisKrs;
import ais.action.master.helper.KrsMahasiswaAnalisisHelper.ItemKrs;
import ais.common.Common;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;

/** Memasang affordance klik dan popup analisis pada seluruh ringkasan HTML KRS. */
public final class KrsMahasiswaAnalisisPopupHelper {

	private static final String ATTR_INSTALLED = "ais.krsAnalisis.installed";
	private static final String ATTR_MAHASISWA = "ais.krsAnalisis.mahasiswa";
	private static final String ATTR_KRS = "ais.krsAnalisis.krs";
	private static final String ATTR_REMEDIAL = "ais.krsAnalisis.remedial";

	private KrsMahasiswaAnalisisPopupHelper() {
	}

	/**
	 * Mengisi ringkasan dan memasang satu listener yang aman dipakai ulang setelah timer/refresh.
	 * Konteks disimpan pada atribut komponen, sehingga listener tidak menangkap entity lama.
	 */
	public static void pasang(final Html target, final Mahasiswa mahasiswa,
			final KrsMahasiswa krsMahasiswa, final boolean remedial) {
		if (target == null || mahasiswa == null || krsMahasiswa == null) return;
		String ringkasan = KrsDetailHelper.rubahKeteranganPengambilanKRS(krsMahasiswa, remedial);
		target.setContent(buatRingkasanKlik(ringkasan));
		target.setTooltiptext("Klik untuk membuka analisis rinci KRS, grafik persetujuan, penilaian, dan SKS");
		target.setAttribute(ATTR_MAHASISWA, mahasiswa);
		target.setAttribute(ATTR_KRS, krsMahasiswa);
		target.setAttribute(ATTR_REMEDIAL, Boolean.valueOf(remedial));
		if (!Boolean.TRUE.equals(target.getAttribute(ATTR_INSTALLED))) {
			target.setAttribute(ATTR_INSTALLED, Boolean.TRUE);
			String style = target.getStyle();
			target.setStyle((style == null || style.trim().isEmpty() ? "" : style + ";")
					+ "display:block;cursor:pointer;");
			target.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Mahasiswa currentMahasiswa = (Mahasiswa) target.getAttribute(ATTR_MAHASISWA);
					KrsMahasiswa currentKrs = (KrsMahasiswa) target.getAttribute(ATTR_KRS);
					boolean currentRemedial = Boolean.TRUE.equals(target.getAttribute(ATTR_REMEDIAL));
					tampilkan(KrsMahasiswaAnalisisHelper.analisis(
							currentMahasiswa, currentKrs, currentRemedial));
				}
			});
		}
	}

	private static String buatRingkasanKlik(String ringkasan) {
		String isi = ringkasan == null || ringkasan.trim().isEmpty()
				? "<font>Belum ada ringkasan KRS</font>" : ringkasan;
		return "<div style='display:inline-flex;align-items:flex-start;gap:6px;color:#075985;'>"
				+ "<span aria-hidden='true' style='font-size:14px;line-height:1.25;'>&#128202;</span>"
				+ "<span style='text-decoration:underline;text-decoration-style:dotted;"
				+ "text-underline-offset:3px;'>" + isi + "</span></div>";
	}

	public static void tampilkan(AnalisisKrs analisis) throws Exception {
		if (analisis == null) return;
		MyWindow window = new MyWindow("Analisis Pintar KRS Mahasiswa", "normal", true);
		window.setWidth(Common.isMobile() ? "100%" : "980px");
		window.setHeight(Common.isMobile() ? "94%" : "780px");
		window.setSizable(true);
		window.setContentStyle("overflow:auto;background:#f1f5f9;padding:0;");
		window.appendChild(new Html(buatHtml(analisis)));
		if (window.getPage() == null && Executions.getCurrent() != null
				&& Executions.getCurrent().getDesktop() != null
				&& Executions.getCurrent().getDesktop().getFirstPage() != null) {
			window.setPage(Executions.getCurrent().getDesktop().getFirstPage());
		}
		window.doModal();
	}

	private static String buatHtml(AnalisisKrs a) {
		KrsMahasiswa krs = a.getKrsMahasiswa();
		Mahasiswa mahasiswa = a.getMahasiswa();
		StringBuilder h = new StringBuilder();
		h.append("<div style='font-family:Arial,sans-serif;color:#172033;font-size:13px;line-height:1.48;'>")
				.append("<div style='background:linear-gradient(120deg,#0f4c81,#0e7490);color:white;padding:17px 20px;'>")
				.append("<div style='font-size:21px;font-weight:bold;'>Analisis Pintar KRS</div>")
				.append("<div style='margin-top:3px;'>").append(esc(mahasiswa == null ? "-" : mahasiswa.getNim()))
				.append(" &mdash; ").append(esc(mahasiswa == null ? "-" : mahasiswa.getNama())).append("</div>")
				.append("<div style='font-size:12px;margin-top:4px;opacity:.95;'>")
				.append("Semester ").append(esc(krs == null ? "-" : krs.getSemester()))
				.append(krs != null && krs.getTahapan() != null ? " &bull; Tahap " + esc(krs.getTahapan()) : "")
				.append(krs != null && krs.getSemesterPendek() != null ? " &bull; Semester Pendek" : "")
				.append(a.isRemedial() ? " &bull; Remedial" : "")
				.append(krs == null ? "" : " &bull; " + esc(krs.getTahunAkademik())).append("</div></div>")
				.append("<div style='padding:16px 18px;'>");

		h.append("<div style='background:#e0f2fe;border:1px solid #7dd3fc;border-left:5px solid #0284c7;"
				+ "border-radius:8px;padding:13px 15px;margin-bottom:13px;'>")
				.append("<div style='font-size:11px;font-weight:bold;color:#075985;text-transform:uppercase;'>Kesimpulan utama</div>")
				.append("<div style='font-size:17px;font-weight:bold;margin-top:4px;'>")
				.append(esc(a.getKesimpulan())).append("</div></div>");
		String warnaPrioritas = warnaPrioritas(a.getPrioritas());
		h.append("<div style='display:flex;align-items:flex-start;gap:10px;background:white;border:1px solid #cbd5e1;"
				+ "border-radius:8px;padding:11px 13px;margin-bottom:13px;'>")
				.append("<span style='display:inline-block;background:").append(warnaPrioritas)
				.append(";color:white;border-radius:999px;padding:3px 9px;font-size:11px;font-weight:bold;white-space:nowrap;'>")
				.append(esc(a.getPrioritas())).append("</span><div><b>Keputusan berikut yang disarankan</b><br>")
				.append(esc(a.getArahKeputusan())).append("</div></div>");

		h.append("<div style='display:flex;flex-wrap:wrap;gap:9px;margin-bottom:14px;'>");
		kartu(h, "Mata kuliah", String.valueOf(a.getTotalMatakuliah()), "cakupan KRS", "#1d4ed8");
		kartu(h, "Disetujui", String.valueOf(a.getDisetujui()), persen(a.getDisetujui(), a.getTotalMatakuliah()) + "%", "#15803d");
		kartu(h, "Sudah dinilai", String.valueOf(a.getDinilai()), persen(a.getDinilai(), a.getDisetujui()) + "% dari disetujui", "#7e22ce");
		kartu(h, "SKS terdeteksi", String.valueOf(a.getTotalSks()), "dari rincian mata kuliah", "#b45309");
		kartu(h, "Skor kesiapan", a.getSkorKesiapan() + "%", "50% persetujuan + 50% penilaian", warnaPrioritas);
		h.append("</div>");

		h.append("<div style='display:flex;flex-wrap:wrap;gap:12px;margin-bottom:14px;'>");
		grafikPersetujuan(h, a);
		grafikPenilaian(h, a);
		grafikSks(h, a);
		h.append("</div>");

		h.append("<div style='display:flex;flex-wrap:wrap;gap:12px;margin-bottom:14px;'>")
				.append("<div style='flex:1;min-width:280px;background:white;border:1px solid #cbd5e1;border-radius:8px;padding:12px;'>")
				.append("<div style='font-weight:bold;margin-bottom:8px;'>Indikator akademik tersimpan</div>")
				.append("<table style='width:100%;border-collapse:collapse;'>");
		barisFakta(h, "IPS", angka(krs == null ? null : krs.getIps()));
		barisFakta(h, "IPK", angka(krs == null ? null : krs.getIpk()));
		barisFakta(h, "SKS semester (rekap)", esc(krs == null ? "-" : krs.getSksYangDiambil()));
		barisFakta(h, "SKS hasil rincian", esc(Integer.valueOf(a.getTotalSks())));
		barisFakta(h, "Konsistensi SKS", a.isSksKonsisten()
				? "<b style='color:#166534;'>Sesuai</b>"
				: "<b style='color:#b45309;'>Selisih " + Math.abs(a.getSelisihSks()) + " SKS</b>");
		barisFakta(h, "SKS kumulatif (rekap)", esc(krs == null ? "-" : krs.getSksk()));
		barisFakta(h, "Catatan KRS", esc(krs == null || krs.getCatatan().isEmpty() ? "-" : krs.getCatatan()));
		h.append("</table></div>");
		daftar(h, "Temuan analitis", a.getTemuan(), "#fff7ed", "#fdba74", "#9a3412");
		h.append("</div>");
		daftar(h, "Rekomendasi tindak lanjut", a.getRekomendasi(), "#f0fdf4", "#86efac", "#166534");

		h.append("<div style='font-size:14px;font-weight:bold;margin:16px 0 7px;'>Rincian mata kuliah</div>")
				.append("<div style='overflow:auto;background:white;border:1px solid #cbd5e1;border-radius:8px;'>")
				.append("<table style='width:100%;border-collapse:collapse;min-width:650px;'>")
				.append("<tr style='background:#e2e8f0;'><th style='padding:8px;text-align:left;'>Kode</th>")
				.append("<th style='padding:8px;text-align:left;'>Mata kuliah</th><th style='padding:8px;'>SKS</th>")
				.append("<th style='padding:8px;text-align:left;'>Persetujuan</th><th style='padding:8px;text-align:left;'>Penilaian</th></tr>");
		if (a.getItems().isEmpty()) {
			h.append("<tr><td colspan='5' style='padding:12px;text-align:center;color:#64748b;'>Belum ada rincian mata kuliah.</td></tr>");
		} else {
			for (ItemKrs item : a.getItems()) {
				h.append("<tr><td style='padding:7px;border-top:1px solid #e2e8f0;'>").append(esc(item.getKode()))
						.append("</td><td style='padding:7px;border-top:1px solid #e2e8f0;'>").append(esc(item.getNama()))
						.append("</td><td style='padding:7px;text-align:center;border-top:1px solid #e2e8f0;'>").append(item.getSks())
						.append("</td><td style='padding:7px;border-top:1px solid #e2e8f0;color:")
						.append(item.isDisetujui() ? "#166534" : "#9a3412").append(";font-weight:bold;'>")
						.append(esc(item.getStatusPersetujuan())).append("</td><td style='padding:7px;border-top:1px solid #e2e8f0;'>")
						.append(item.isDinilai() ? "Nilai " + angka(Double.valueOf(item.getNilai())) : "Belum dinilai")
						.append("</td></tr>");
			}
		}
		h.append("</table></div>")
				.append("<div style='margin-top:12px;padding:10px 12px;background:#f8fafc;border:1px solid #cbd5e1;border-radius:7px;color:#475569;'>")
				.append("<b>Cara membaca:</b> persetujuan dihitung dari status setiap detail KRS. Penilaian dianggap terisi hanya untuk mata kuliah yang disetujui dan total nilai minimal 0,1. Grafik SKS dihitung ulang dari SKS mata kuliah pada detail; angka rekap di atas tetap ditampilkan terpisah agar sumber data dapat dibandingkan.")
				.append("</div><div style='font-size:11px;color:#64748b;margin-top:9px;'>Analisis ini read-only dan deterministik; tidak mengubah KRS, persetujuan, nilai, maupun rekap akademik.</div>")
				.append("</div></div>");
		return h.toString();
	}

	private static void kartu(StringBuilder h, String label, String nilai, String sub, String color) {
		h.append("<div style='flex:1;min-width:145px;background:white;border:1px solid #cbd5e1;border-top:4px solid ")
				.append(color).append(";border-radius:8px;padding:10px 12px;'>")
				.append("<div style='font-size:11px;color:#64748b;'>").append(esc(label)).append("</div>")
				.append("<div style='font-size:23px;font-weight:bold;color:").append(color).append(";'>")
				.append(esc(nilai)).append("</div><div style='font-size:11px;color:#64748b;'>")
				.append(esc(sub)).append("</div></div>");
	}

	private static void grafikPersetujuan(StringBuilder h, AnalisisKrs a) {
		grafik(h, "Grafik persetujuan", a.getTotalMatakuliah(),
				new int[] { a.getDisetujui(), a.getMenungguPersetujuan(), a.getStatusLain() },
				new String[] { "Disetujui", "Menunggu", "Status lain" },
				new String[] { "#16a34a", "#f59e0b", "#ef4444" });
	}

	private static void grafikPenilaian(StringBuilder h, AnalisisKrs a) {
		grafik(h, "Grafik penilaian", a.getDisetujui(),
				new int[] { a.getDinilai(), a.getBelumDinilai() },
				new String[] { "Sudah dinilai", "Belum dinilai" },
				new String[] { "#7c3aed", "#cbd5e1" });
	}

	private static void grafikSks(StringBuilder h, AnalisisKrs a) {
		grafik(h, "Grafik distribusi SKS", a.getTotalSks(),
				new int[] { a.getSksDisetujui(), a.getSksMenunggu(), a.getSksStatusLain() },
				new String[] { "SKS disetujui", "SKS menunggu", "SKS lain" },
				new String[] { "#0284c7", "#f97316", "#dc2626" });
	}

	private static void grafik(StringBuilder h, String judul, int total, int[] values,
			String[] labels, String[] colors) {
		h.append("<div style='flex:1;min-width:250px;background:white;border:1px solid #cbd5e1;border-radius:8px;padding:12px;'>")
				.append("<div style='font-weight:bold;margin-bottom:9px;'>").append(esc(judul)).append("</div>")
				.append("<div style='display:flex;height:18px;border-radius:9px;overflow:hidden;background:#e2e8f0;'>");
		for (int i = 0; i < values.length; i++) {
			int width = persen(values[i], total);
			if (width > 0) h.append("<div title='").append(esc(labels[i])).append(" ").append(values[i])
					.append("' style='width:").append(width).append("%;background:").append(colors[i]).append(";'></div>");
		}
		h.append("</div><div style='margin-top:8px;'>");
		for (int i = 0; i < values.length; i++) {
			h.append("<div style='display:inline-block;margin:2px 10px 2px 0;font-size:11px;'>")
					.append("<span style='display:inline-block;width:9px;height:9px;border-radius:2px;background:")
					.append(colors[i]).append(";margin-right:4px;'></span>").append(esc(labels[i]))
					.append(": <b>").append(values[i]).append("</b></div>");
		}
		h.append("</div></div>");
	}

	private static void daftar(StringBuilder h, String judul, List<String> values,
			String background, String border, String color) {
		h.append("<div style='flex:1;min-width:280px;background:").append(background)
				.append(";border:1px solid ").append(border).append(";border-radius:8px;padding:12px;color:")
				.append(color).append(";'><div style='font-weight:bold;margin-bottom:6px;'>")
				.append(esc(judul)).append("</div><ol style='margin:0;padding-left:20px;'>");
		if (values == null || values.isEmpty()) h.append("<li>Tidak ada catatan tambahan.</li>");
		else for (String value : values) h.append("<li style='margin:4px 0;'>").append(esc(value)).append("</li>");
		h.append("</ol></div>");
	}

	private static void barisFakta(StringBuilder h, String label, String value) {
		h.append("<tr><td style='padding:5px 7px;border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;'>")
				.append(esc(label)).append("</td><td style='padding:5px 7px;border:1px solid #e2e8f0;'>")
				.append(value).append("</td></tr>");
	}

	private static int persen(int bagian, int total) {
		if (bagian <= 0 || total <= 0) return 0;
		return Math.min(100, Math.max(1, (int) Math.round((bagian * 100.0) / total)));
	}

	private static String warnaPrioritas(String prioritas) {
		if ("TINGGI".equals(prioritas)) return "#b91c1c";
		if ("SEDANG".equals(prioritas) || "PERLU VERIFIKASI".equals(prioritas)) return "#b45309";
		return "#15803d";
	}

	private static String angka(Double value) {
		return value == null ? "-" : new DecimalFormat("0.00").format(value.doubleValue());
	}

	private static String esc(Object value) {
		String result = value == null ? "-" : value.toString();
		return result.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;");
	}
}
