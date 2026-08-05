package ais.action.master.sirs.chart.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zul.Html;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.DiskusiUiHelper;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyChart;

/**
 * Pembangun dasbor <b>10 Diagnosa Penyakit Terbanyak</b> — merangkum penyakit apa saja yang paling
 * sering ditegakkan (diagnosa akhir utama) sepanjang satu tahun, berdasarkan kode ICD. Ini termasuk
 * indikator MUTU &amp; profil layanan: manajemen bisa mengetahui beban penyakit terbesar di
 * rumah sakit/klinik dan merencanakan obat, alat, serta tenaga sesuai pola penyakit yang nyata.
 *
 * <h3>Untuk apa dasbor ini (bahasa awam)</h3>
 * Menjawab: "Penyakit apa yang paling banyak ditangani tahun ini?" Ditampilkan sebagai daftar batang
 * dari yang terbanyak ke yang lebih sedikit, ditambah kartu angka ringkas — mudah dibaca tanpa perlu
 * paham istilah medis maupun komputer.
 *
 * <h3>Grafik yang ditampilkan</h3>
 * <ol>
 *   <li><b>Kartu ringkasan (KPI)</b> — total kasus terdiagnosis, diagnosa paling banyak beserta
 *       jumlahnya, dan berapa ragam (jenis) diagnosa yang muncul.</li>
 *   <li><b>Batang 10 diagnosa terbanyak</b> — perbandingan jumlah kasus tiap diagnosa; batang
 *       terpanjang adalah penyakit yang paling sering ditangani.</li>
 *   <li><b>Lingkaran porsi (donat)</b> — seberapa besar bagian tiap diagnosa teratas dari total
 *       kasus (sisanya digabung sebagai "Lainnya").</li>
 * </ol>
 *
 * <h3>Cara kerja teknis</h3>
 * Data diambil dari {@code sirs.diagnosa_penyakit} yang di-<i>join</i> ke {@code sirs.icd} pada kolom
 * {@code diagnosa_akhir1} (diagnosa akhir utama), dikelompokkan per nama diagnosa dan diurut menurun
 * lalu dibatasi 10 teratas — seluruh agregasi dilakukan di sisi database (hemat memori, cepat).
 * Grafik digambar dengan {@link HtmlChartHelper} (HTML + CSS modern, tanpa JFreeChart). Bila nama
 * Indonesia diagnosa kosong, dipakai kode ICD-nya agar tetap terbaca.
 *
 * <h3>Manajemen session (PENTING)</h3>
 * Pembacaan memakai {@link HibernateUtil#currentSession()} (session request ZK) yang ditutup otomatis
 * — TIDAK ditutup manual (menutupnya berisiko "Session is closed!"). Kelas ini tidak membuka
 * {@code openSession()}/{@code currentNativeSession()} sehingga tak ada koneksi yang perlu ditutup
 * sendiri (menghindari kebocoran koneksi).
 *
 * <h3>Ketahanan &amp; efisiensi</h3>
 * Hanya 10 baris teratas yang ditarik ke memori; total kasus dihitung dalam satu lintasan; akses
 * indeks/koleksi dijaga dari {@code null}; {@link StringBuilder} berkapasitas awal dipakai agar hemat
 * memori; data kosong ditangani dengan pesan ramah. Kelas util statis murni, aman lintas request.
 *
 * <h3>Kompatibilitas</h3>
 * Java 1.7 / ZKoss 5.5 (tanpa lambda/diamond/Stream), {@code try/catch} gaya Java 1.6, memakai helper
 * bersama {@link HtmlChartHelper}.
 *
 * @author AIS
 */
public final class DiagnosaTerbanyakDashboardBuilder {

	private static final String[] PALET = new String[] { "#0ea5e9", "#42b72a", "#f7b928", "#e4496b",
			"#8b5cf6", "#00a5b5", "#ff7a45", "#6b7280", "#eab308", "#14b8a6", "#94a3b8" };

	private DiagnosaTerbanyakDashboardBuilder() {
	}

	/**
	 * Menggambar dasbor 10 diagnosa terbanyak untuk satu tahun.
	 *
	 * @param target wadah {@link MyChart} tempat dasbor digambar (tak boleh null).
	 * @param tahun  tahun yang ditinjau; bila null dipakai tahun berjalan.
	 */
	@SuppressWarnings("unchecked")
	public static void render(MyChart target, Integer tahun) {
		if (target == null) {
			return;
		}
		int th = tahun == null ? Calendar.getInstance().get(Calendar.YEAR) : tahun.intValue();
		try {
			Session session = HibernateUtil.currentSession(); // request session — JANGAN ditutup manual.

			String sql = "select coalesce(nullif(trim(i.nama_indonesia),''), i.kode, '(Tanpa Nama)') as diagnosa, "
					+ "count(*) as jml from sirs.diagnosa_penyakit d "
					+ "inner join sirs.icd i on (d.diagnosa_akhir1 = i.id) "
					+ "where to_char(d.tanggal,'YYYY') = '" + th + "' "
					+ "group by 1 order by jml desc limit 10";

			List<Object[]> baris = session.createSQLQuery(sql).list();
			if (baris == null) {
				baris = new ArrayList<Object[]>();
			}

			// Total seluruh kasus terdiagnosis (untuk KPI & porsi) — hitung terpisah agar akurat.
			String sqlTotal = "select count(*) from sirs.diagnosa_penyakit d "
					+ "where d.diagnosa_akhir1 is not null and to_char(d.tanggal,'YYYY') = '" + th + "'";
			Object totObj = session.createSQLQuery(sqlTotal).uniqueResult();
			long totalSemua = (totObj instanceof Number) ? ((Number) totObj).longValue() : 0;

			gambar(target, th, baris, totalSemua);
		} catch (Exception e) {
			tampilkanGagal(target, e);
		}
	}

	// ─────────────────────────── internal ───────────────────────────

	private static void gambar(MyChart target, int tahun, List<Object[]> baris, long totalSemua) {
		int n = baris.size();
		String[] nama = new String[n];
		double[] jumlah = new double[n];
		double totalTop = 0;
		for (int i = 0; i < n; i++) {
			Object[] row = baris.get(i);
			nama[i] = (row != null && row.length > 0 && row[0] != null) ? row[0].toString() : "(Tanpa Nama)";
			double v = (row != null && row.length > 1 && row[1] instanceof Number)
					? ((Number) row[1]).doubleValue() : 0;
			jumlah[i] = v;
			totalTop += v;
		}

		StringBuilder sb = new StringBuilder(8192);
		sb.append("<div style=\"font-family:inherit;display:flex;flex-direction:column;gap:14px;\">");
		sb.append("<div style=\"padding:12px 14px;border-radius:12px;background:linear-gradient(135deg,#eff6ff,#f8fbff);border:1px solid #bfdbfe;\">")
				.append("<div style=\"font-size:15px;font-weight:800;color:#075985;\">")
				.append(DiskusiUiHelper.escapeHtml("10 Diagnosa Penyakit Terbanyak — Tahun " + tahun)).append("</div>")
				.append("<div style=\"font-size:12px;color:#475569;margin-top:3px;\">")
				.append("Penyakit yang paling sering ditangani tahun ini, dari yang terbanyak ke yang lebih sedikit.")
				.append("</div></div>");

		if (n == 0 || totalSemua <= 0) {
			sb.append("<div style=\"padding:26px;text-align:center;color:#64748b;border:1px dashed #cbd5e1;border-radius:12px;\">")
					.append("Belum ada data diagnosa pada tahun ini.</div></div>");
			suntik(target, sb.toString());
			return;
		}

		String[] kLabel = new String[] { "Total Kasus Terdiagnosis", "Diagnosa Terbanyak",
				"Ragam Diagnosa (Top 10)" };
		String[] kNilai = new String[] { String.valueOf(totalSemua), nama[0], String.valueOf(n) };
		String[] kSub = new String[] { "tahun " + tahun, fmt(jumlah[0]) + " kasus", "jenis teratas" };
		sb.append(HtmlChartHelper.kpiCards(kLabel, kNilai, kSub, null, null, PALET));

		sb.append("<div style=\"display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:14px;\">");

		sb.append("<div>").append(HtmlChartHelper.barHorizontal(
				"10 Diagnosa Terbanyak",
				"Perbandingan jumlah kasus tiap diagnosa. Batang terpanjang adalah penyakit paling sering ditangani.",
				nama, jumlah, PALET[0])).append("</div>");

		// Donat porsi top-10 + "Lainnya".
		double sisa = totalSemua - totalTop;
		if (sisa < 0) {
			sisa = 0;
		}
		String[] donLabel = new String[n + 1];
		double[] donVal = new double[n + 1];
		for (int i = 0; i < n; i++) {
			donLabel[i] = nama[i];
			donVal[i] = jumlah[i];
		}
		donLabel[n] = "Lainnya";
		donVal[n] = sisa;
		sb.append("<div>").append(HtmlChartHelper.donut(
				"Porsi Diagnosa Teratas",
				"Seberapa besar bagian tiap diagnosa teratas dari seluruh kasus; sisanya digabung sebagai \"Lainnya\".",
				donLabel, donVal, PALET, String.valueOf(totalSemua))).append("</div>");

		sb.append("</div></div>");
		suntik(target, sb.toString());
	}

	private static String fmt(double v) {
		return String.valueOf(Math.round(v));
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
					+ "border:1px solid #f5c2c7;border-radius:10px;\">Gagal memuat dasbor diagnosa terbanyak. "
					+ "Silakan coba lagi atau hubungi admin.</div>");
			node.setParent(target);
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/sirs/chart/helper/DiagnosaTerbanyakDashboardBuilder.java:192");
		}
	}
}
