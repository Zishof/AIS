package ais.ui.util;

/**
 * <h1>HtmlChartHelper — Pembuat grafik ringan berbasis HTML &amp; CSS (tanpa JFreeChart)</h1>
 *
 * <p>
 * Kelas utilitas ini menjadi <b>satu sumber terpusat</b> (single source of truth) untuk membentuk
 * grafik sederhana yang dirender <b>sepenuhnya memakai HTML + CSS modern</b> — bukan pustaka grafik
 * berbasis gambar server seperti JFreeChart. Hasilnya berupa potongan HTML yang siap dipasang ke
 * komponen {@link MyHtml} pada halaman ZK 5.x. Pendekatan ini dipilih karena beberapa alasan praktik
 * terbaik di lapangan:
 * </p>
 * <ul>
 *   <li><b>Ringan &amp; cepat.</b> Tidak ada gambar yang digenerasi di sisi server; browser yang
 *       menggambar lewat CSS ({@code conic-gradient}, {@code flex}, lebar persen). Beban server turun
 *       dan halaman terasa lebih responsif.</li>
 *   <li><b>Responsif otomatis.</b> Karena murni HTML/CSS, grafik mengikuti lebar wadah dan menyesuaikan
 *       diri di layar ponsel maupun desktop lewat <i>media query</i> terpusat di
 *       {@code css_utama.css} (blok {@code ais-chart-*}).</li>
 *   <li><b>Mudah dirawat &amp; seragam.</b> Semua warna, jarak, dan bentuk ada di CSS; mengubah
 *       tampilan cukup di satu tempat. Pemanggil hanya menyuplai data + judul + deskripsi.</li>
 *   <li><b>Bisa dipakai-ulang.</b> Cocok mengganti pie/bar JFreeChart pada banyak dasbor (partisipasi
 *       diskusi, komposisi kehadiran, perbandingan nilai, tren, dll.).</li>
 * </ul>
 *
 * <h2>Filosofi "jelas untuk orang awam"</h2>
 * <p>
 * Setiap grafik selalu dilengkapi <b>judul singkat</b> dan <b>deskripsi 1–2 kalimat berbahasa
 * sederhana</b> yang menjelaskan <i>apa makna angka tersebut bagi pengguna</i>, bukan istilah teknis.
 * Tujuannya agar pengguna yang sama sekali tidak paham dunia IT pun langsung mengerti maksud grafik.
 * Deskripsi di-<i>escape</i> agar aman dari markup yang tidak diinginkan.
 * </p>
 *
 * <h2>Jenis grafik yang tersedia</h2>
 * <ul>
 *   <li>{@link #donut(String, String, String[], double[], String[], String)} — lingkaran "donat"
 *       (pengganti pie) untuk menampilkan <i>komposisi</i>/proporsi beberapa kategori. Persentase
 *       kategori pertama ditonjolkan di tengah donat.</li>
 *   <li>{@link #barHorizontal(String, String, String[], double[], String)} — batang mendatar untuk
 *       <i>membandingkan</i> beberapa nilai atau menampilkan <i>tren</i> sederhana antar kategori.</li>
 * </ul>
 *
 * <h2>Anatomi kelas (kontrak sclass)</h2>
 * <p>
 * Markup memakai rangkaian <i>class</i>: {@code ais-chart-card} (kartu pembungkus) &rarr;
 * {@code ais-chart-title} + {@code ais-chart-desc} (judul + penjelasan) &rarr; isi grafik
 * ({@code ais-chart-donut} / {@code ais-chart-bars}) dan {@code ais-chart-legend} (keterangan warna).
 * Seluruh gaya didefinisikan terpusat di {@code css_utama.css}.
 * </p>
 *
 * <h2>Contoh pemakaian</h2>
 * <pre>{@code
 * String html = HtmlChartHelper.donut(
 *     "Partisipasi Diskusi",
 *     "Berapa banyak peserta yang sudah ikut berdiskusi dan berapa yang belum.",
 *     new String[]{"Sudah", "Belum"},
 *     new double[]{12, 8},
 *     new String[]{"#1877f2", "#e4e6eb"},
 *     "ikut diskusi");
 * new MyHtml(html).setParent(wadah);
 * }</pre>
 *
 * <h2>Keamanan &amp; ketahanan</h2>
 * <p>
 * Judul, deskripsi, dan label di-<i>escape</i> lewat {@link DiskusiUiHelper#escapeHtml(String)}.
 * Pembagian terhadap total nol ditangani dengan aman (grafik menampilkan 0% alih-alih error).
 * Warna yang kurang dari jumlah kategori akan diputar-ulang dari palet bawaan.
 * </p>
 *
 * @author e-Campus UI Team
 */
public final class HtmlChartHelper {

	/** Palet warna bawaan (dipakai bila pemanggil tidak menyuplai warna yang cukup). */
	private static final String[] PALET = new String[] { "#1877f2", "#42b72a", "#f7b928", "#e4496b",
			"#8b5cf6", "#06b6d4", "#e4e6eb" };

	private HtmlChartHelper() {
	}

	/**
	 * Membentuk grafik "donat" (donut) berbasis CSS {@code conic-gradient} untuk menampilkan
	 * komposisi/proporsi beberapa kategori. Persentase kategori PERTAMA ditonjolkan di tengah.
	 *
	 * @param judul         judul singkat grafik (boleh null).
	 * @param deskripsi     penjelasan 1–2 kalimat bahasa sederhana (boleh null).
	 * @param labels        nama tiap kategori.
	 * @param values        nilai tiap kategori (sejajar dengan {@code labels}).
	 * @param colors        warna tiap kategori; bila kurang/null dipakai palet bawaan.
	 * @param captionTengah teks kecil di bawah persentase tengah (mis. "ikut diskusi"); boleh null.
	 * @return potongan HTML siap dipasang ke {@link MyHtml}.
	 */
	public static String donut(String judul, String deskripsi, String[] labels, double[] values,
			String[] colors, String captionTengah) {
		int n = labels == null ? 0 : labels.length;
		double total = 0;
		for (int i = 0; i < n; i++) {
			double v = values[i] < 0 ? 0 : values[i];
			total += v;
		}

		StringBuilder gradient = new StringBuilder();
		StringBuilder legend = new StringBuilder();
		double akumulasi = 0;
		for (int i = 0; i < n; i++) {
			double v = values[i] < 0 ? 0 : values[i];
			double mulai = total <= 0 ? (i * 100.0 / Math.max(n, 1)) : (akumulasi * 100.0 / total);
			akumulasi += v;
			double selesai = total <= 0 ? ((i + 1) * 100.0 / Math.max(n, 1)) : (akumulasi * 100.0 / total);
			String warna = warna(colors, i);
			if (gradient.length() > 0) {
				gradient.append(", ");
			}
			gradient.append(warna).append(" ").append(fmt(mulai)).append("% ").append(fmt(selesai)).append("%");

			double persen = total <= 0 ? 0 : (v * 100.0 / total);
			legend.append("<div class=\"ais-chart-legend-item\"><span class=\"dot\" style=\"background:")
					.append(warna).append("\"></span>").append(DiskusiUiHelper.escapeHtml(labels[i]))
					.append(" &mdash; ").append(fmtInt(v)).append(" (").append(fmt(persen)).append("%)</div>");
		}

		double persenUtama = total <= 0 || n == 0 ? 0 : ((values[0] < 0 ? 0 : values[0]) * 100.0 / total);

		StringBuilder sb = new StringBuilder(512);
		sb.append("<div class=\"ais-chart-card\">");
		kepala(sb, judul, deskripsi);
		sb.append("<div class=\"ais-chart-donut-wrap\">");
		sb.append("<div class=\"ais-chart-donut\" style=\"background:conic-gradient(").append(gradient)
				.append(")\"><div class=\"ais-chart-donut-hole\"><div class=\"ais-chart-donut-center\">")
				.append(fmt(persenUtama)).append("%");
		if (captionTengah != null && captionTengah.trim().length() > 0) {
			sb.append("<small>").append(DiskusiUiHelper.escapeHtml(captionTengah)).append("</small>");
		}
		sb.append("</div></div></div>");
		sb.append("<div class=\"ais-chart-legend\">").append(legend).append("</div>");
		sb.append("</div></div>");
		return sb.toString();
	}

	/**
	 * Membentuk grafik batang mendatar untuk membandingkan beberapa nilai atau menampilkan tren
	 * sederhana. Panjang batang proporsional terhadap nilai terbesar.
	 *
	 * @param judul     judul singkat (boleh null).
	 * @param deskripsi penjelasan bahasa sederhana (boleh null).
	 * @param labels    nama tiap batang.
	 * @param values    nilai tiap batang.
	 * @param warnaBar  warna batang (boleh null → palet bawaan).
	 * @return potongan HTML siap dipasang ke {@link MyHtml}.
	 */
	public static String barHorizontal(String judul, String deskripsi, String[] labels, double[] values,
			String warnaBar) {
		int n = labels == null ? 0 : labels.length;
		double maks = 0;
		for (int i = 0; i < n; i++) {
			if (values[i] > maks) {
				maks = values[i];
			}
		}
		String warna = warnaBar == null || warnaBar.trim().length() == 0 ? PALET[0] : warnaBar;

		StringBuilder sb = new StringBuilder(512);
		sb.append("<div class=\"ais-chart-card\">");
		kepala(sb, judul, deskripsi);
		sb.append("<div class=\"ais-chart-bars\">");
		for (int i = 0; i < n; i++) {
			double v = values[i] < 0 ? 0 : values[i];
			double persen = maks <= 0 ? 0 : (v * 100.0 / maks);
			sb.append("<div class=\"ais-chart-bar-row\"><div class=\"ais-chart-bar-label\">")
					.append(DiskusiUiHelper.escapeHtml(labels[i])).append("</div>")
					.append("<div class=\"ais-chart-bar-track\"><div class=\"ais-chart-bar-fill\" style=\"width:")
					.append(fmt(persen)).append("%;background:").append(warna).append("\"></div></div>")
					.append("<div class=\"ais-chart-bar-val\">").append(fmtInt(v)).append("</div></div>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	/**
	 * Grafik batang BERTUMPUK (stacked) mendatar. Tiap kategori menjadi satu batang yang
	 * panjangnya sebanding dengan TOTAL kategori (relatif terhadap kategori terbesar) dan terbagi
	 * menjadi segmen-segmen per seri. Cocok untuk menampilkan <i>komposisi</i> sekaligus
	 * <i>perbandingan total</i> antar kategori — mis. jumlah alumni per program studi dengan rincian
	 * laki-laki/perempuan: batang lebih panjang berarti alumninya lebih banyak, dan warna di dalamnya
	 * memperlihatkan proporsinya.
	 *
	 * @param judul        judul singkat (boleh null).
	 * @param deskripsi    penjelasan bahasa sederhana (boleh null).
	 * @param categories   nama tiap kategori (mis. nama prodi).
	 * @param seriesLabels nama tiap seri (mis. {@code {"Laki-laki","Perempuan"}}).
	 * @param values       nilai dengan indeks {@code [kategori][seri]}.
	 * @param seriesColors warna tiap seri (boleh null → palet bawaan).
	 * @return potongan HTML siap dipasang ke {@link MyHtml}.
	 */
	public static String stackedBar(String judul, String deskripsi, String[] categories,
			String[] seriesLabels, double[][] values, String[] seriesColors) {
		int nCat = categories == null ? 0 : categories.length;
		int nSer = seriesLabels == null ? 0 : seriesLabels.length;
		double maksTotal = 0;
		double[] totals = new double[nCat];
		for (int c = 0; c < nCat; c++) {
			double t = 0;
			for (int s = 0; s < nSer; s++) {
				double v = values[c][s] < 0 ? 0 : values[c][s];
				t += v;
			}
			totals[c] = t;
			if (t > maksTotal) {
				maksTotal = t;
			}
		}

		StringBuilder sb = new StringBuilder(1024);
		sb.append("<div class=\"ais-chart-card\">");
		kepala(sb, judul, deskripsi);
		sb.append("<div class=\"ais-chart-legend ais-chart-legend-row\">");
		for (int s = 0; s < nSer; s++) {
			sb.append("<div class=\"ais-chart-legend-item\"><span class=\"dot\" style=\"background:")
					.append(warna(seriesColors, s)).append("\"></span>")
					.append(DiskusiUiHelper.escapeHtml(seriesLabels[s])).append("</div>");
		}
		sb.append("</div><div class=\"ais-chart-stack\">");
		for (int c = 0; c < nCat; c++) {
			double total = totals[c];
			double lebarBar = maksTotal <= 0 ? 0 : (total * 100.0 / maksTotal);
			sb.append("<div class=\"ais-chart-stack-row\"><div class=\"ais-chart-stack-label\">")
					.append(DiskusiUiHelper.escapeHtml(categories[c])).append("</div>")
					.append("<div class=\"ais-chart-stack-track\"><div class=\"ais-chart-stack-bar\" style=\"width:")
					.append(fmt(lebarBar)).append("%\">");
			for (int s = 0; s < nSer; s++) {
				double v = values[c][s] < 0 ? 0 : values[c][s];
				double segPersen = total <= 0 ? 0 : (v * 100.0 / total);
				if (segPersen <= 0) {
					continue;
				}
				sb.append("<div class=\"ais-chart-stack-seg\" title=\"")
						.append(DiskusiUiHelper.escapeHtml(seriesLabels[s])).append(": ").append(fmtInt(v))
						.append("\" style=\"width:").append(fmt(segPersen)).append("%;background:")
						.append(warna(seriesColors, s)).append("\"></div>");
			}
			sb.append("</div></div><div class=\"ais-chart-stack-val\">").append(fmtInt(total))
					.append("</div></div>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	/**
	 * Grafik <b>radar / jaring laba-laba (spider web)</b> berbasis SVG murni (HTML/CSS, tanpa
	 * JFreeChart). Cocok membandingkan beberapa <i>dimensi</i> sekaligus dalam satu tampilan —
	 * mis. profil kompetensi lulusan, hasil tracer study per aspek (relevansi kerja, masa tunggu,
	 * gaji, kepuasan, dll.), atau perbandingan capaian antar program studi pada banyak indikator.
	 * Tiap sumbu mewakili satu dimensi; tiap seri digambar sebagai poligon — makin "menggembung" ke
	 * suatu sumbu berarti nilainya makin tinggi di dimensi itu.
	 *
	 * @param judul        judul singkat (boleh null).
	 * @param deskripsi    penjelasan bahasa sederhana (boleh null).
	 * @param axes         nama tiap sumbu/dimensi (minimal 3 agar berbentuk jaring).
	 * @param seriesLabels nama tiap seri yang dibandingkan.
	 * @param values       nilai dengan indeks {@code [seri][sumbu]}.
	 * @param colors       warna tiap seri (boleh null → palet bawaan).
	 * @param maxValue     nilai skala maksimum; bila &lt;= 0 dihitung otomatis dari nilai terbesar.
	 * @return potongan HTML (berisi SVG) siap dipasang ke {@link MyHtml}.
	 */
	public static String radar(String judul, String deskripsi, String[] axes, String[] seriesLabels,
			double[][] values, String[] colors, double maxValue) {
		int nAxis = axes == null ? 0 : axes.length;
		int nSeries = seriesLabels == null ? 0 : seriesLabels.length;
		double maks = maxValue;
		if (maks <= 0) {
			for (int s = 0; s < nSeries; s++) {
				for (int a = 0; a < nAxis; a++) {
					if (values[s][a] > maks) {
						maks = values[s][a];
					}
				}
			}
		}
		if (maks <= 0) {
			maks = 1;
		}

		final int cx = 170, cy = 160, R = 110, rings = 4;
		StringBuilder svg = new StringBuilder(2048);
		svg.append("<svg viewBox=\"0 0 340 320\" class=\"ais-chart-radar-svg\" xmlns=\"http://www.w3.org/2000/svg\">");
		for (int ring = 1; ring <= rings; ring++) {
			svg.append("<polygon points=\"").append(polyPoints(cx, cy, R * ring / (double) rings, nAxis))
					.append("\" fill=\"none\" stroke=\"#e4e6eb\" stroke-width=\"1\"/>");
		}
		for (int a = 0; a < nAxis; a++) {
			double ang = -Math.PI / 2 + a * 2 * Math.PI / nAxis;
			double x = cx + R * Math.cos(ang), y = cy + R * Math.sin(ang);
			svg.append("<line x1=\"").append(cx).append("\" y1=\"").append(cy).append("\" x2=\"").append(fmt(x))
					.append("\" y2=\"").append(fmt(y)).append("\" stroke=\"#e4e6eb\" stroke-width=\"1\"/>");
			double lx = cx + (R + 14) * Math.cos(ang), ly = cy + (R + 14) * Math.sin(ang);
			String anchor = Math.abs(Math.cos(ang)) < 0.3 ? "middle" : (Math.cos(ang) > 0 ? "start" : "end");
			svg.append("<text x=\"").append(fmt(lx)).append("\" y=\"").append(fmt(ly + 3))
					.append("\" font-size=\"9\" fill=\"#65676b\" text-anchor=\"").append(anchor).append("\">")
					.append(DiskusiUiHelper.escapeHtml(axes[a])).append("</text>");
		}
		for (int s = 0; s < nSeries; s++) {
			String w = warna(colors, s);
			StringBuilder pts = new StringBuilder();
			for (int a = 0; a < nAxis; a++) {
				double v = values[s][a] < 0 ? 0 : values[s][a];
				double rr = R * Math.min(v / maks, 1.0);
				double ang = -Math.PI / 2 + a * 2 * Math.PI / nAxis;
				if (pts.length() > 0) {
					pts.append(" ");
				}
				pts.append(fmt(cx + rr * Math.cos(ang))).append(",").append(fmt(cy + rr * Math.sin(ang)));
			}
			svg.append("<polygon points=\"").append(pts).append("\" fill=\"").append(w)
					.append("\" fill-opacity=\"0.18\" stroke=\"").append(w).append("\" stroke-width=\"2\"/>");
		}
		svg.append("</svg>");

		StringBuilder legend = new StringBuilder();
		for (int s = 0; s < nSeries; s++) {
			legend.append("<div class=\"ais-chart-legend-item\"><span class=\"dot\" style=\"background:")
					.append(warna(colors, s)).append("\"></span>").append(DiskusiUiHelper.escapeHtml(seriesLabels[s]))
					.append("</div>");
		}

		StringBuilder sb = new StringBuilder(2560);
		sb.append("<div class=\"ais-chart-card\">");
		kepala(sb, judul, deskripsi);
		sb.append("<div class=\"ais-chart-radar-wrap\">").append(svg).append("<div class=\"ais-chart-legend\">")
				.append(legend).append("</div></div></div>");
		return sb.toString();
	}

	/**
	 * Membentuk grafik <b>GARIS multi-seri (tren)</b> berbasis SVG untuk memperlihatkan
	 * perkembangan beberapa angka sepanjang waktu/periode (mis. tagihan vs pembayaran per bulan).
	 * Cocok dipakai pada semua dashboard "per bulan / per periode".
	 *
	 * <p>Murni SVG + teks (tanpa JFreeChart), ringan dan tajam di layar resolusi tinggi, serta
	 * responsif (dibungkus area yang dapat di-scroll mendatar bila kategori sangat banyak).</p>
	 *
	 * @param judul        judul singkat (boleh {@code null}).
	 * @param deskripsi    penjelasan bahasa sederhana untuk pengguna awam (boleh {@code null}).
	 * @param categories   label sumbu-X (mis. nama bulan), urut kiri→kanan.
	 * @param seriesLabels nama tiap garis/seri (mis. "Tagihan", "Dibayar").
	 * @param values       nilai dengan tata letak {@code values[seri][kategori]} — satu array
	 *                     nilai per garis, sejajar dengan {@code categories}.
	 * @param colors       warna tiap garis (boleh {@code null} → palet bawaan).
	 * @return potongan HTML siap dipasang ke {@link MyHtml}.
	 */
	public static String lineMulti(String judul, String deskripsi, String[] categories,
			String[] seriesLabels, double[][] values, String[] colors) {
		int nCat = categories == null ? 0 : categories.length;
		int nSer = seriesLabels == null ? 0 : seriesLabels.length;
		double maks = 0;
		for (int s = 0; s < nSer; s++) {
			for (int c = 0; c < nCat; c++) {
				double v = (values != null && s < values.length && values[s] != null && c < values[s].length)
						? values[s][c] : 0;
				if (v > maks) {
					maks = v;
				}
			}
		}
		if (maks <= 0) {
			maks = 1;
		}
		double x0 = 52, x1 = 624, y0 = 18, y1 = 232;
		double xStep = nCat > 1 ? (x1 - x0) / (nCat - 1) : 0;
		StringBuilder sb = new StringBuilder(1024);
		sb.append("<div class=\"ais-chart-card\">");
		kepala(sb, judul, deskripsi);
		sb.append("<div style=\"overflow-x:auto;\"><svg viewBox=\"0 0 660 270\" width=\"100%\" "
				+ "style=\"max-width:100%;height:auto;font-family:Arial,sans-serif;\">");
		for (int g = 0; g <= 4; g++) {
			double gy = y1 - (y1 - y0) * g / 4.0;
			double gv = maks * g / 4.0;
			sb.append("<line x1=\"").append(fmt(x0)).append("\" y1=\"").append(fmt(gy)).append("\" x2=\"")
					.append(fmt(x1)).append("\" y2=\"").append(fmt(gy)).append("\" stroke=\"#e2e8f0\" stroke-width=\"1\"/>");
			sb.append("<text x=\"").append(fmt(x0 - 6)).append("\" y=\"").append(fmt(gy + 3))
					.append("\" text-anchor=\"end\" font-size=\"9\" fill=\"#94a3b8\">").append(fmtInt(gv)).append("</text>");
		}
		for (int c = 0; c < nCat; c++) {
			double x = x0 + c * xStep;
			sb.append("<text x=\"").append(fmt(x)).append("\" y=\"").append(fmt(y1 + 14))
					.append("\" text-anchor=\"middle\" font-size=\"9\" fill=\"#64748b\">")
					.append(DiskusiUiHelper.escapeHtml(categories[c])).append("</text>");
		}
		for (int s = 0; s < nSer; s++) {
			String w = warna(colors, s);
			StringBuilder pts = new StringBuilder();
			for (int c = 0; c < nCat; c++) {
				double v = (values != null && s < values.length && values[s] != null && c < values[s].length)
						? values[s][c] : 0;
				double x = x0 + c * xStep;
				double y = y1 - (y1 - y0) * (v / maks);
				if (pts.length() > 0) {
					pts.append(" ");
				}
				pts.append(fmt(x)).append(",").append(fmt(y));
			}
			// garis dulu, lalu titik di atasnya
			sb.append("<polyline points=\"").append(pts).append("\" fill=\"none\" stroke=\"").append(w)
					.append("\" stroke-width=\"2.5\" stroke-linejoin=\"round\" stroke-linecap=\"round\"/>");
			for (int c = 0; c < nCat; c++) {
				double v = (values != null && s < values.length && values[s] != null && c < values[s].length)
						? values[s][c] : 0;
				double x = x0 + c * xStep;
				double y = y1 - (y1 - y0) * (v / maks);
				sb.append("<circle cx=\"").append(fmt(x)).append("\" cy=\"").append(fmt(y))
						.append("\" r=\"2.6\" fill=\"").append(w).append("\"/>");
			}
		}
		sb.append("</svg></div>");
		sb.append("<div class=\"ais-chart-legend\">");
		for (int s = 0; s < nSer; s++) {
			sb.append("<div class=\"ais-chart-legend-item\"><span class=\"dot\" style=\"background:")
					.append(warna(colors, s)).append("\"></span>").append(DiskusiUiHelper.escapeHtml(seriesLabels[s]))
					.append("</div>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	/**
	 * Grafik batang TEGAK berkelompok (grouped vertical bar chart) — cocok membandingkan dua atau
	 * lebih seri data per kategori secara berdampingan, mis. jumlah pendaftar bulan ini vs bulan yang
	 * sama tahun lalu. Tiap kelompok (satu label X) memuat satu batang per seri; tinggi batang
	 * proporsional terhadap nilai terbesar di seluruh data.
	 *
	 * @param judul        judul singkat (boleh null).
	 * @param deskripsi    penjelasan bahasa sederhana (boleh null).
	 * @param categories   label tiap kelompok di sumbu-X (mis. nama bulan).
	 * @param seriesLabels nama tiap seri yang dibandingkan.
	 * @param values       nilai dengan indeks {@code [kelompok][seri]}.
	 * @param seriesColors warna tiap seri (boleh null → palet bawaan).
	 * @return potongan HTML siap dipasang ke {@link MyHtml}.
	 */
	public static String barVerticalGrouped(String judul, String deskripsi, String[] categories,
			String[] seriesLabels, double[][] values, String[] seriesColors) {
		int nCat = categories == null ? 0 : categories.length;
		int nSer = seriesLabels == null ? 0 : seriesLabels.length;
		double maks = 0;
		for (int c = 0; c < nCat; c++) {
			for (int s = 0; s < nSer; s++) {
				double v = (values != null && c < values.length && values[c] != null && s < values[c].length)
						? values[c][s] : 0;
				if (v > maks) {
					maks = v;
				}
			}
		}
		if (maks <= 0) {
			maks = 1;
		}
		final int chartBarH = 160; // tinggi area batang dalam px
		final int barW = 22;       // lebar tiap batang px

		StringBuilder sb = new StringBuilder(2048);
		sb.append("<div class=\"ais-chart-card\">");
		kepala(sb, judul, deskripsi);
		sb.append("<div style=\"overflow-x:auto;\">");
		// min-width agar ada ruang: nCat*(nSer*barW + nSer*3 + 16)+32
		int minW = nCat * (nSer * (barW + 3) + 20) + 32;
		sb.append("<div style=\"display:flex;align-items:flex-end;gap:8px;padding:8px 4px 0;min-width:")
				.append(minW).append("px;\">");
		for (int c = 0; c < nCat; c++) {
			sb.append("<div style=\"display:flex;flex-direction:column;align-items:center;flex:1;\">");
			// area batang
			sb.append("<div style=\"display:flex;align-items:flex-end;gap:3px;height:").append(chartBarH)
					.append("px;width:100%;justify-content:center;\">");
			for (int s = 0; s < nSer; s++) {
				double v = (values != null && c < values.length && values[c] != null && s < values[c].length)
						? (values[c][s] < 0 ? 0 : values[c][s]) : 0;
				int bh = (int) Math.max(v > 0 ? 2 : 0, Math.round(v / maks * chartBarH));
				String w = warna(seriesColors, s);
				sb.append("<div style=\"display:flex;flex-direction:column;align-items:center;justify-content:flex-end;\">")
						.append("<span style=\"font-size:9px;color:#374151;margin-bottom:1px;\">").append(fmtInt(v)).append("</span>")
						.append("<div style=\"width:").append(barW).append("px;height:").append(bh)
						.append("px;background:").append(w).append(";border-radius:3px 3px 0 0;\"></div>")
						.append("</div>");
			}
			sb.append("</div>");
			// label X
			sb.append("<div style=\"font-size:10px;color:#6b7280;margin-top:3px;text-align:center;white-space:nowrap;\">")
					.append(DiskusiUiHelper.escapeHtml(categories[c])).append("</div>");
			sb.append("</div>");
		}
		sb.append("</div></div>");
		// legend
		sb.append("<div class=\"ais-chart-legend\">");
		for (int s = 0; s < nSer; s++) {
			sb.append("<div class=\"ais-chart-legend-item\"><span class=\"dot\" style=\"background:")
					.append(warna(seriesColors, s)).append("\"></span>")
					.append(DiskusiUiHelper.escapeHtml(seriesLabels[s])).append("</div>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	/**
	 * Baris <b>kartu KPI</b> (Key Performance Indicator) — deretan kartu ringkasan berisi
	 * angka-angka paling penting, dengan label, nilai besar, keterangan tambahan, dan indikator
	 * naik/turun (opsional). Tata letak otomatis menyesuaikan layar (responsif): banyak kolom di
	 * desktop, satu kolom di ponsel. Cocok sebagai baris pertama dasbor untuk memberi gambaran
	 * cepat sebelum pengguna melihat grafik detail.
	 *
	 * @param labels      nama tiap KPI (mis. "Total Peminat").
	 * @param nilaiText   nilai yang ditampilkan besar (mis. "1.234").
	 * @param subtitles   keterangan tambahan di bawah nilai (mis. "vs 980 th. lalu"); boleh null.
	 * @param deltas      teks delta (mis. "+26%"); boleh null.
	 * @param positifs    {@code true} = delta positif (hijau ↑); {@code false} = negatif (merah ↓);
	 *                    boleh null.
	 * @param colors      warna aksen tiap kartu (boleh null → palet bawaan).
	 * @return potongan HTML siap dipasang ke {@link MyHtml}.
	 */
	public static String kpiCards(String[] labels, String[] nilaiText, String[] subtitles,
			String[] deltas, boolean[] positifs, String[] colors) {
		int n = labels == null ? 0 : labels.length;
		StringBuilder sb = new StringBuilder(1024);
		sb.append("<div style=\"display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:10px;\">");
		for (int i = 0; i < n; i++) {
			String w = warna(colors, i);
			String delta = (deltas != null && i < deltas.length) ? deltas[i] : null;
			boolean pos = (positifs == null || i >= positifs.length) || positifs[i];
			String sub = (subtitles != null && i < subtitles.length) ? subtitles[i] : null;
			String nilai = (nilaiText != null && i < nilaiText.length) ? nilaiText[i] : "0";
			sb.append("<div style=\"background:var(--ais-card-bg,#fff);border-radius:12px;padding:14px 16px;")
					.append("box-shadow:0 1px 6px rgba(0,0,0,.09);border-left:4px solid ").append(w)
					.append(";min-width:0;\">");
			sb.append("<div style=\"font-size:11px;color:#6b7280;margin-bottom:3px;font-weight:600;text-transform:uppercase;letter-spacing:.4px;\">")
					.append(DiskusiUiHelper.escapeHtml(labels[i])).append("</div>");
			sb.append("<div style=\"font-size:28px;font-weight:700;color:").append(w)
					.append(";line-height:1.1;\">").append(DiskusiUiHelper.escapeHtml(nilai)).append("</div>");
			if (sub != null && sub.length() > 0) {
				sb.append("<div style=\"font-size:10px;color:#9ca3af;margin-top:2px;\">")
						.append(DiskusiUiHelper.escapeHtml(sub)).append("</div>");
			}
			if (delta != null && delta.length() > 0) {
				String dc = pos ? "#16a34a" : "#dc2626";
				String arrow = pos ? "&#9650;" : "&#9660;";
				sb.append("<div style=\"font-size:11px;color:").append(dc)
						.append(";margin-top:4px;font-weight:700;\">").append(arrow).append(" ")
						.append(DiskusiUiHelper.escapeHtml(delta)).append("</div>");
			}
			sb.append("</div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	/** Titik-titik poligon beraturan ber-{@code n} sudut, untuk cincin/grid radar. */
	private static String polyPoints(int cx, int cy, double r, int n) {
		StringBuilder p = new StringBuilder();
		for (int i = 0; i < n; i++) {
			double ang = -Math.PI / 2 + i * 2 * Math.PI / n;
			if (p.length() > 0) {
				p.append(" ");
			}
			p.append(fmt(cx + r * Math.cos(ang))).append(",").append(fmt(cy + r * Math.sin(ang)));
		}
		return p.toString();
	}

	// ── util internal ──────────────────────────────────────────────
	private static void kepala(StringBuilder sb, String judul, String deskripsi) {
		if (judul != null && judul.trim().length() > 0) {
			sb.append("<div class=\"ais-chart-title\">").append(DiskusiUiHelper.escapeHtml(judul)).append("</div>");
		}
		if (deskripsi != null && deskripsi.trim().length() > 0) {
			sb.append("<div class=\"ais-chart-desc\">").append(DiskusiUiHelper.escapeHtml(deskripsi)).append("</div>");
		}
	}

	private static String warna(String[] colors, int i) {
		if (colors != null && i < colors.length && colors[i] != null && colors[i].trim().length() > 0) {
			return colors[i];
		}
		return PALET[i % PALET.length];
	}

	private static String fmt(double d) {
		java.text.DecimalFormat df = new java.text.DecimalFormat("0.#");
		return df.format(d);
	}

	private static String fmtInt(double d) {
		if (Math.abs(d - Math.round(d)) < 0.001) {
			return String.valueOf(Math.round(d));
		}
		return fmt(d);
	}
}
