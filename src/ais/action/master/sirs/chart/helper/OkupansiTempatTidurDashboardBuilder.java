package ais.action.master.sirs.chart.helper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.zkoss.zul.Html;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.StatusTempatTidur;
import ais.database.model.sirs.TempatTidur;
import ais.ui.util.DiskusiUiHelper;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyChart;

/**
 * Pembangun dasbor <b>Okupansi Tempat Tidur (Rawat Inap)</b> — memotret kondisi tempat tidur SAAT INI:
 * berapa banyak yang terisi, kosong, atau tidak siap pakai, dan bagaimana sebarannya per kelas
 * perawatan. Ini adalah metrik operasional terpenting rawat inap: manajemen dan petugas pendaftaran
 * bisa langsung tahu apakah masih ada tempat kosong untuk pasien baru, dan kelas mana yang penuh.
 *
 * <h3>Untuk apa dasbor ini (bahasa awam)</h3>
 * Menjawab pertanyaan yang muncul setiap hari di rumah sakit: "Masih ada kamar kosong tidak? Di
 * kelas apa?" Dasbor menampilkan jumlah tempat tidur yang sedang dipakai dan yang masih tersedia,
 * lengkap dengan perkiraan tingkat keterisian (okupansi) — semua sebagai gambar sederhana yang bisa
 * dibaca sekilas.
 *
 * <h3>Grafik yang ditampilkan</h3>
 * <ol>
 *   <li><b>Kartu ringkasan (KPI)</b> — total tempat tidur, jumlah terisi, jumlah kosong, dan
 *       perkiraan tingkat keterisian (okupansi) dalam persen.</li>
 *   <li><b>Lingkaran porsi (donat) status</b> — perbandingan status seluruh tempat tidur (mis.
 *       terisi, kosong, perbaikan) apa adanya sesuai data.</li>
 *   <li><b>Batang jumlah tempat tidur per kelas</b> — kelas perawatan mana yang paling banyak
 *       tempat tidurnya.</li>
 *   <li><b>Batang tempat tidur TERISI per kelas</b> — kelas mana yang paling penuh saat ini.</li>
 * </ol>
 *
 * <h3>Cara kerja teknis</h3>
 * Seluruh {@link TempatTidur} dibaca lewat Criteria (akses berbasis properti, tanpa menebak nama
 * kolom) — jumlah tempat tidur biasanya kecil (puluhan/ratusan) sehingga aman ditarik lalu dihitung
 * di memori. Untuk tiap tempat tidur diambil nama {@link StatusTempatTidur} dan nama
 * {@link KelasPerawatan}, lalu dikelompokkan memakai {@link LinkedHashMap} (menjaga urutan agar
 * warna/legenda konsisten). "Terisi" dikenali secara fleksibel dari nama status yang mengandung kata
 * "isi" (mis. "Terisi"); "Kosong" dari nama yang mengandung "kosong"/"tersedia" — sehingga variasi
 * penamaan status tetap tertangani. Grafik digambar dengan {@link HtmlChartHelper} (HTML + CSS
 * modern, tanpa JFreeChart).
 *
 * <h3>Manajemen session (PENTING)</h3>
 * Pembacaan memakai {@link HibernateUtil#currentSession()} (session request ZK) yang ditutup otomatis
 * — TIDAK ditutup manual (menutupnya berisiko "Session is closed!"). Kelas ini tidak membuka
 * {@code openSession()}/{@code currentNativeSession()} sehingga tak ada koneksi yang perlu ditutup
 * sendiri (menghindari kebocoran koneksi).
 *
 * <h3>Ketahanan &amp; efisiensi</h3>
 * Semua akses (status/kelas bisa null) dijaga sehingga satu baris data tak lengkap tidak menggagalkan
 * dasbor. Penghitungan dilakukan dalam satu lintasan, {@link StringBuilder} berkapasitas awal dipakai
 * agar hemat memori, dan data kosong ditangani dengan pesan ramah. Kelas util statis murni, aman
 * lintas request.
 *
 * <h3>Kompatibilitas</h3>
 * Java 1.7 / ZKoss 5.5 (tanpa lambda/diamond/Stream), {@code try/catch} gaya Java 1.6, memakai helper
 * bersama {@link HtmlChartHelper}.
 *
 * @author AIS
 */
public final class OkupansiTempatTidurDashboardBuilder {

	private static final String[] PALET = new String[] { "#e4496b", "#42b72a", "#f7b928", "#1877f2",
			"#8b5cf6", "#00a5b5", "#ff7a45", "#6b7280" };

	private OkupansiTempatTidurDashboardBuilder() {
	}

	/**
	 * Menggambar dasbor okupansi tempat tidur berdasarkan kondisi terkini.
	 *
	 * @param target wadah {@link MyChart} tempat dasbor digambar (tak boleh null).
	 */
	@SuppressWarnings("unchecked")
	public static void render(MyChart target) {
		if (target == null) {
			return;
		}
		try {
			Session session = HibernateUtil.currentSession(); // request session — JANGAN ditutup manual.
			List<TempatTidur> beds = session.createCriteria(TempatTidur.class).list();
			if (beds == null) {
				beds = new ArrayList<TempatTidur>();
			}

			Map<String, Integer> perStatus = new LinkedHashMap<String, Integer>();
			Map<String, Integer> perKelasTotal = new LinkedHashMap<String, Integer>();
			Map<String, Integer> perKelasTerisi = new LinkedHashMap<String, Integer>();
			int total = 0;
			int terisi = 0;
			int kosong = 0;

			for (TempatTidur bed : beds) {
				if (bed == null) {
					continue;
				}
				total++;

				String namaStatus = "(Tak diketahui)";
				StatusTempatTidur st = bed.getStatusTempatTidur();
				if (st != null && st.getNama() != null && st.getNama().trim().length() > 0) {
					namaStatus = st.getNama().trim();
				}
				tambah(perStatus, namaStatus);

				String namaKelas = "(Tanpa Kelas)";
				KelasPerawatan kp = bed.getKelasPerawatan();
				if (kp != null && kp.getNama() != null && kp.getNama().trim().length() > 0) {
					namaKelas = kp.getNama().trim();
				}
				tambah(perKelasTotal, namaKelas);

				String low = namaStatus.toLowerCase();
				boolean isTerisi = low.indexOf("isi") >= 0; // "terisi"/"berisi"
				boolean isKosong = low.indexOf("kosong") >= 0 || low.indexOf("tersedia") >= 0;
				if (isTerisi) {
					terisi++;
					tambah(perKelasTerisi, namaKelas);
				} else if (isKosong) {
					kosong++;
				}
			}

			gambar(target, total, terisi, kosong, perStatus, perKelasTotal, perKelasTerisi);
		} catch (Exception e) {
			tampilkanGagal(target, e);
		}
	}

	// ─────────────────────────── internal ───────────────────────────

	private static void gambar(MyChart target, int total, int terisi, int kosong,
			Map<String, Integer> perStatus, Map<String, Integer> perKelasTotal,
			Map<String, Integer> perKelasTerisi) {

		StringBuilder sb = new StringBuilder(8192);
		sb.append("<div style=\"font-family:inherit;display:flex;flex-direction:column;gap:14px;\">");
		sb.append("<div style=\"padding:12px 14px;border-radius:12px;background:linear-gradient(135deg,#fff1f2,#fffafa);border:1px solid #fecdd3;\">")
				.append("<div style=\"font-size:15px;font-weight:800;color:#9f1239;\">")
				.append(DiskusiUiHelper.escapeHtml("Okupansi Tempat Tidur (Rawat Inap) — Kondisi Terkini")).append("</div>")
				.append("<div style=\"font-size:12px;color:#475569;margin-top:3px;\">")
				.append("Berapa tempat tidur yang sedang dipakai dan yang masih kosong, beserta sebarannya per kelas perawatan.")
				.append("</div></div>");

		if (total <= 0) {
			sb.append("<div style=\"padding:26px;text-align:center;color:#64748b;border:1px dashed #cbd5e1;border-radius:12px;\">")
					.append("Belum ada data tempat tidur.</div></div>");
			suntik(target, sb.toString());
			return;
		}

		String okupansi = Math.round(terisi * 100.0 / total) + "%";
		String[] kLabel = new String[] { "Total Tempat Tidur", "Terisi", "Kosong", "Perkiraan Okupansi" };
		String[] kNilai = new String[] { String.valueOf(total), String.valueOf(terisi),
				String.valueOf(kosong), okupansi };
		String[] kSub = new String[] { "seluruh bangsal", "sedang dipakai", "siap dipakai",
				"terisi / total" };
		sb.append(HtmlChartHelper.kpiCards(kLabel, kNilai, kSub, null, null, PALET));

		sb.append("<div style=\"display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:14px;\">");

		// Donat status seluruh tempat tidur.
		String[] stLabel = kunci(perStatus);
		double[] stVal = nilai(perStatus);
		sb.append("<div>").append(HtmlChartHelper.donut(
				"Status Tempat Tidur",
				"Perbandingan status seluruh tempat tidur (terisi, kosong, atau tidak siap pakai) apa adanya.",
				stLabel, stVal, PALET, String.valueOf(total))).append("</div>");

		// Batang jumlah tempat tidur per kelas.
		String[] klLabel = kunci(perKelasTotal);
		double[] klVal = nilai(perKelasTotal);
		sb.append("<div>").append(HtmlChartHelper.barHorizontal(
				"Jumlah Tempat Tidur per Kelas",
				"Kelas perawatan mana yang paling banyak tempat tidurnya. Batang terpanjang = kelas terbesar.",
				klLabel, klVal, PALET[3])).append("</div>");

		// Batang tempat tidur TERISI per kelas (selaraskan urutan dengan daftar kelas).
		double[] klTerisiVal = new double[klLabel.length];
		for (int i = 0; i < klLabel.length; i++) {
			Integer v = perKelasTerisi.get(klLabel[i]);
			klTerisiVal[i] = v == null ? 0 : v.intValue();
		}
		sb.append("<div>").append(HtmlChartHelper.barHorizontal(
				"Tempat Tidur Terisi per Kelas",
				"Kelas mana yang paling penuh saat ini. Semakin panjang batang, semakin banyak yang terisi.",
				klLabel, klTerisiVal, PALET[0])).append("</div>");

		sb.append("</div></div>");
		suntik(target, sb.toString());
	}

	private static void tambah(Map<String, Integer> map, String key) {
		Integer v = map.get(key);
		map.put(key, Integer.valueOf(v == null ? 1 : v.intValue() + 1));
	}

	private static String[] kunci(Map<String, Integer> map) {
		return map.keySet().toArray(new String[map.size()]);
	}

	private static double[] nilai(Map<String, Integer> map) {
		double[] r = new double[map.size()];
		int i = 0;
		for (Integer v : map.values()) {
			r[i++] = v == null ? 0 : v.intValue();
		}
		return r;
	}

	private static void suntik(MyChart target, String html) {
		Common.clear(target);
		Html node = new Html(html);
		node.setParent(target);
	}

	private static void tampilkanGagal(MyChart target, Exception e) {
		try {
			Common.tampilErrorJikaAdmin(e);
			Common.clear(target);
			Html node = new Html("<div style=\"padding:20px;color:#a94442;background:#f8d7da;"
					+ "border:1px solid #f5c2c7;border-radius:10px;\">Gagal memuat dasbor okupansi tempat tidur. "
					+ "Silakan coba lagi atau hubungi admin.</div>");
			node.setParent(target);
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/sirs/chart/helper/OkupansiTempatTidurDashboardBuilder.java:235");
		}
	}
}
