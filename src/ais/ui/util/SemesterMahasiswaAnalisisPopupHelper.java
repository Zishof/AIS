package ais.ui.util;

import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Html;

import ais.action.master.helper.SemesterMahasiswaAnalisisHelper;
import ais.action.master.helper.SemesterMahasiswaAnalisisHelper.AnalisisSemester;
import ais.action.master.helper.SemesterMahasiswaAnalisisHelper.RingkasanSemester;
import ais.common.Common;
import ais.database.model.Mahasiswa;

/**
 * Adapter UI reusable untuk angka semester mahasiswa. Algoritma dan query tetap berada di
 * {@link SemesterMahasiswaAnalisisHelper}; kelas ini hanya memasang listener dan merender
 * snapshot menjadi dialog. Dengan batas ini, halaman lain dapat memakai analisis yang sama tanpa
 * menyalin keputusan akademik ke dalam event handler ZK.
 */
public final class SemesterMahasiswaAnalisisPopupHelper {

	private SemesterMahasiswaAnalisisPopupHelper() {
	}

	/** Membuat angka semester menjadi link tanpa menjalankan query riwayat sebelum diklik. */
	public static void pasangLink(final A link, final Mahasiswa mahasiswa,
			final RingkasanSemester ringkasan) {
		if (link == null || mahasiswa == null || ringkasan == null) return;
		String semester = String.valueOf(ringkasan.getSemesterEfektif());
		/*
		 * Jangan tampilkan hanya angka: pada grid yang padat tautan tersebut mudah dianggap label
		 * biasa. Label aksi dan bentuk pill dipasang di helper ini agar seluruh baris/pemanggil
		 * analisis semester memperoleh affordance klik yang sama.
		 */
		link.setLabel("Semester " + semester + " · klik");
		link.setTooltiptext("Klik untuk membuka rincian Semester " + semester
				+ ": asal angka, rumus, bukti KRS, dan saran perbaikan");
		link.setStyle("display:inline-block;color:#075985;background:#e0f2fe;"
				+ "border:1px solid #7dd3fc;border-radius:5px;padding:2px 7px;"
				+ "text-decoration:underline;text-underline-offset:2px;cursor:pointer;"
				+ "font-weight:700;white-space:nowrap;");
		link.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				tampilkan(SemesterMahasiswaAnalisisHelper.analisisLengkap(mahasiswa, ringkasan));
			}
		});
	}

	/** Menampilkan hasil analisis sebagai modal yang dapat digulir pada desktop maupun mobile. */
	public static void tampilkan(AnalisisSemester analisis) throws Exception {
		if (analisis == null || analisis.getRingkasan() == null) return;
		MyWindow window = new MyWindow("Analisis Semester Mahasiswa", "normal", true);
		window.setWidth(Common.isMobile() ? "100%" : "820px");
		window.setHeight(Common.isMobile() ? "92%" : "720px");
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

	private static String buatHtml(AnalisisSemester analisis) {
		RingkasanSemester r = analisis.getRingkasan();
		StringBuilder html = new StringBuilder();
		html.append("<div style='font-family:Arial,sans-serif;color:#172033;font-size:13px;line-height:1.52;'>")
				.append("<div style='background:#174a7e;color:white;padding:16px 18px;'>")
				.append("<div style='font-size:19px;font-weight:bold;'>Analisis Semester Mahasiswa</div>")
				.append("<div style='font-size:12px;margin-top:3px;opacity:.95;'>Angka dijelaskan dari periode akademik, scope mahasiswa, data awal studi, dan bukti KRS.</div>")
				.append("</div><div style='padding:16px 18px;'>");

		html.append("<div style='background:#eef6ff;border:1px solid #8fc1ee;border-left:5px solid #1769aa;border-radius:6px;padding:14px 15px;margin-bottom:12px;'>")
				.append("<div style='font-size:12px;font-weight:bold;color:#315b7d;text-transform:uppercase;'>Jawaban Singkat</div>")
				.append("<div style='font-size:20px;font-weight:bold;color:#123e6b;margin-top:3px;'>Semester ")
				.append(esc(r.getSemesterEfektif())).append("</div>")
				.append("<div style='margin-top:4px;color:#334155;'><b>Periode acuan:</b> ")
				.append(esc(r.getTahunAkademik())).append(" ").append(esc(r.getJenisSemester())).append("</div>")
				.append("<div style='margin-top:8px;font-size:14px;font-weight:bold;'>")
				.append(esc(analisis.getKeputusanUtama())).append("</div>")
				.append("<div style='margin-top:7px;color:#334155;'><b>Sumber:</b> ")
				.append(esc(r.getSumberPeriode())).append(".</div></div>");

		tambahDaftarJikaAda(html, "Penyebab Selisih atau Kendala", analisis.getPenghambat(),
				"#fff1f2", "#fda4af", "#9f1239");
		tambahDaftarJikaAda(html, "Yang Sudah Konsisten", analisis.getKondisiBenar(),
				"#f0fdf4", "#86efac", "#166534");
		tambahDaftarJikaAda(html, "Hal yang Perlu Diperhatikan", analisis.getPerhatian(),
				"#fffbeb", "#fcd34d", "#854d0e");
		tambahDaftar(html, "Solusi yang Disarankan", analisis.getSaran(),
				"#eff6ff", "#93c5fd", "#1e3a8a");

		html.append("<div style='font-size:14px;font-weight:bold;color:#334155;margin:18px 0 8px;'>Bukti yang Dibaca Sistem</div>")
				.append("<div style='background:white;border:1px solid #d7e0ea;border-radius:8px;padding:12px;margin-bottom:12px;'>")
				.append("<table style='width:100%;border-collapse:collapse;'>");
		for (Map.Entry<String, String> entry : analisis.getFakta().entrySet()) {
			html.append("<tr><td style='width:39%;padding:6px 8px;border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;vertical-align:top;'>")
					.append(esc(entry.getKey())).append("</td><td style='padding:6px 8px;border:1px solid #e2e8f0;'>")
					.append(esc(entry.getValue())).append("</td></tr>");
		}
		html.append("</table></div>");

		html.append("<div style='background:#f8fafc;border:1px solid #cbd5e1;border-radius:8px;padding:12px;margin-bottom:12px;'>")
				.append("<div style='font-weight:bold;margin-bottom:6px;'>Cara Membaca Hasil</div>")
				.append("<div>Nomor semester bukan diambil dari semester KRS terakhir. Sistem memetakan tahun angkatan dan semester mulai ke periode akademik efektif, lalu menambahkan offset pindahan/alih prodi. KRS dipakai sebagai bukti pembanding. Karena itu kepala KRS kosong atau tulisan 'Belum pernah mengambil KRS' tidak boleh sendirian menaikkan angka semester.</div>")
				.append("<div style='margin-top:7px;'>Kalender Akademik mengatur jadwal kegiatan dan pembukaan fitur. Rencana Tahun Akademik menentukan pasangan Tahun Akademik dan Ganjil/Genap untuk resolver semester. Keduanya perlu konsisten, tetapi centang aktif pada Kalender Akademik tidak otomatis menggantikan Rencana Tahun Akademik.</div>")
				.append("</div>");

		html.append("<div style='font-size:11px;color:#64748b;margin-top:10px;'>")
				.append("Analisis ini read-only. Popup tidak mengubah semester, KRS, status mahasiswa, atau konfigurasi akademik. Koreksi dilakukan pada sumber data yang disebutkan, kemudian daftar dimuat ulang.")
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
