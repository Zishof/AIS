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
 * Pembangun dasbor <b>Pendapatan (Kas Masuk) Pasien</b> — merangkum uang yang benar-benar diterima
 * kasir dari transaksi pembayaran pasien sepanjang satu tahun, memecahnya per bulan dan per cara
 * bayar (tunai vs non-tunai). Kelas ini melengkapi dasbor kunjungan/pendaftaran dengan sisi
 * <i>keuangan</i>, sehingga manajemen bisa melihat bukan hanya "berapa banyak pasien" tetapi juga
 * "berapa besar uang yang masuk dan kapan puncaknya".
 *
 * <h3>Untuk apa dasbor ini (bahasa awam)</h3>
 * Menjawab: "Sepanjang tahun ini, berapa total uang yang diterima dari pasien, bulan mana yang paling
 * besar, dan berapa banyak yang dibayar tunai dibanding lewat kartu/transfer?" Semua ditampilkan
 * sebagai gambar sederhana (kartu angka, garis naik-turun, batang per bulan, dan lingkaran porsi)
 * yang bisa dibaca sekilas tanpa perlu mengerti komputer maupun akuntansi.
 *
 * <h3>Grafik yang ditampilkan</h3>
 * <ol>
 *   <li><b>Kartu ringkasan (KPI)</b> — total pendapatan setahun, rata-rata per bulan, bulan dengan
 *       pemasukan tertinggi, dan porsi pembayaran non-tunai.</li>
 *   <li><b>Garis tren bulanan</b> — naik-turunnya pemasukan tiap bulan; mudah melihat bulan ramai
 *       maupun sepi.</li>
 *   <li><b>Batang pemasukan per bulan</b> — membandingkan besar pemasukan antar bulan.</li>
 *   <li><b>Lingkaran porsi (donat)</b> — perbandingan uang yang diterima tunai vs non-tunai.</li>
 * </ol>
 *
 * <h3>Cara kerja teknis</h3>
 * Data diambil dari {@code sirs.pembayaran} dengan agregasi SQL per bulan
 * ({@code to_char(tanggal_pembayaran,'MM')}): menjumlahkan {@code total_biaya} (nilai transaksi),
 * {@code bayar_tunai}, dan {@code bayar_non_tunai}. Seluruh angka lalu diolah menjadi grafik memakai
 * {@link HtmlChartHelper} (HTML + CSS modern, tanpa JFreeChart). Nilai rupiah pada kartu diformat
 * dengan pemisah ribuan agar mudah dibaca; grafik memakai satuan <i>juta rupiah</i> agar sumbu tetap
 * ringkas untuk angka besar.
 *
 * <h3>Manajemen session (PENTING)</h3>
 * Pembacaan memakai {@link HibernateUtil#currentSession()} (session request ZK) yang ditutup otomatis
 * oleh kerangka kerja — TIDAK ditutup manual (menutupnya berisiko "Session is closed!"). Kelas ini
 * tidak membuka {@code openSession()}/{@code currentNativeSession()} sehingga tak ada koneksi yang
 * perlu ditutup sendiri (menghindari kebocoran koneksi).
 *
 * <h3>Ketahanan &amp; efisiensi</h3>
 * Agregasi dilakukan di sisi database (bukan menarik ribuan baris ke memori), sehingga hemat memori
 * dan cepat. Semua akses indeks/koleksi dijaga dari {@code null}, total dihitung dalam satu lintasan,
 * dan {@link StringBuilder} berkapasitas awal dipakai agar perakitan HTML hemat memori. Data kosong
 * ditangani dengan pesan ramah, bukan grafik pecah. Kelas util statis murni (tanpa state), aman
 * lintas request.
 *
 * <h3>Kompatibilitas</h3>
 * Java 1.7 / ZKoss 5.5 (tanpa lambda/diamond/Stream), {@code try/catch} gaya Java 1.6, memakai helper
 * bersama {@link HtmlChartHelper} agar gaya visual seragam dengan dasbor lain.
 *
 * @author AIS
 */
public final class PendapatanDashboardBuilder {

	private static final String[] PALET = new String[] { "#16a34a", "#1877f2", "#f7b928", "#e4496b",
			"#8b5cf6", "#00a5b5" };

	private static final String[] NAMA_BULAN = new String[] { "Jan", "Feb", "Mar", "Apr", "Mei",
			"Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des" };

	private PendapatanDashboardBuilder() {
	}

	/**
	 * Menggambar dasbor pendapatan untuk satu tahun.
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
			Data d = data(th);
			gambar(target, th, d.totalBulan, d.tunaiBulan, d.nonTunaiBulan);
		} catch (Exception e) {
			tampilkanGagal(target, e);
		}
	}

	/** Pendapatan per bulan (indeks 0..11) dipilah tunai dan non-tunai. */
	public static final class Data {
		public final double[] totalBulan;
		public final double[] tunaiBulan;
		public final double[] nonTunaiBulan;

		Data(double[] totalBulan, double[] tunaiBulan, double[] nonTunaiBulan) {
			this.totalBulan = totalBulan;
			this.tunaiBulan = tunaiBulan;
			this.nonTunaiBulan = nonTunaiBulan;
		}
	}

	/**
	 * Data mentah dasbor, terpisah dari penyajiannya.
	 *
	 * <p>Dipakai bersama layar ZK dan kontrak native supaya kueri serta
	 * agregasinya hanya ada di satu tempat.</p>
	 */
	@SuppressWarnings("unchecked")
	public static Data data(int tahun) {
		Session session = HibernateUtil.currentSession(); // request session — JANGAN ditutup manual.

		String sql = "select to_char(tanggal_pembayaran,'MM') as bln, "
				+ "coalesce(sum(total_biaya),0) as total, "
				+ "coalesce(sum(bayar_tunai),0) as tunai, "
				+ "coalesce(sum(bayar_non_tunai),0) as nontunai "
				+ "from sirs.pembayaran where to_char(tanggal_pembayaran,'YYYY') = '" + tahun + "' "
				+ "group by 1 order by 1";

		List<Object[]> baris = session.createSQLQuery(sql).list();
		if (baris == null) {
			baris = new ArrayList<Object[]>();
		}

		double[] totalBulan = new double[12];
		double[] tunaiBulan = new double[12];
		double[] nonTunaiBulan = new double[12];
		for (Object[] row : baris) {
			if (row == null || row.length < 4) {
				continue;
			}
			int b = parseBulanIndex(row[0]);
			if (b < 0) {
				continue;
			}
			totalBulan[b] = angka(row[1]);
			tunaiBulan[b] = angka(row[2]);
			nonTunaiBulan[b] = angka(row[3]);
		}
		return new Data(totalBulan, tunaiBulan, nonTunaiBulan);
	}

	// ─────────────────────────── internal ───────────────────────────

	private static void gambar(MyChart target, int tahun, double[] totalBulan, double[] tunaiBulan,
			double[] nonTunaiBulan) {

		double grandTotal = 0;
		double totalTunai = 0;
		double totalNonTunai = 0;
		int bulanTertinggi = -1;
		double maks = -1;
		for (int b = 0; b < 12; b++) {
			grandTotal += totalBulan[b];
			totalTunai += tunaiBulan[b];
			totalNonTunai += nonTunaiBulan[b];
			if (totalBulan[b] > maks) {
				maks = totalBulan[b];
				bulanTertinggi = b;
			}
		}
		double rataBulan = grandTotal / 12.0;

		StringBuilder sb = new StringBuilder(8192);
		sb.append("<div style=\"font-family:inherit;display:flex;flex-direction:column;gap:14px;\">");
		sb.append("<div style=\"padding:12px 14px;border-radius:12px;background:linear-gradient(135deg,#ecfdf5,#f7fffb);border:1px solid #bbf7d0;\">")
				.append("<div style=\"font-size:15px;font-weight:800;color:#166534;\">")
				.append(DiskusiUiHelper.escapeHtml("Pendapatan (Kas Masuk) Pasien — Tahun " + tahun)).append("</div>")
				.append("<div style=\"font-size:12px;color:#475569;margin-top:3px;\">")
				.append("Total uang yang diterima dari pasien tiap bulan, bulan mana yang paling besar, dan porsi tunai vs non-tunai.")
				.append("</div></div>");

		if (grandTotal <= 0) {
			sb.append("<div style=\"padding:26px;text-align:center;color:#64748b;border:1px dashed #cbd5e1;border-radius:12px;\">")
					.append("Belum ada data pembayaran pada tahun ini.</div></div>");
			suntik(target, sb.toString());
			return;
		}

		// KPI (rupiah diformat pemisah ribuan).
		String namaBulanTertinggi = bulanTertinggi >= 0 ? NAMA_BULAN[bulanTertinggi] : "-";
		String[] kLabel = new String[] { "Total Pendapatan", "Rata-rata / Bulan", "Bulan Tertinggi",
				"Porsi Non-Tunai" };
		String[] kNilai = new String[] { rp(grandTotal), rp(rataBulan), namaBulanTertinggi,
				persen(totalNonTunai, grandTotal) };
		String[] kSub = new String[] { "tahun " + tahun, "sepanjang tahun",
				bulanTertinggi >= 0 ? rp(maks) : "", "tunai " + persen(totalTunai, grandTotal) };
		sb.append(HtmlChartHelper.kpiCards(kLabel, kNilai, kSub, null, null, PALET));

		sb.append("<div style=\"display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:14px;\">");

		// Grafik dalam satuan JUTA agar sumbu tetap ringkas untuk angka besar.
		double[] totalJuta = keJuta(totalBulan);
		sb.append("<div>").append(HtmlChartHelper.lineMulti(
				"Tren Pendapatan per Bulan (juta Rp)",
				"Naik-turunnya uang yang diterima tiap bulan. Garis menanjak berarti pemasukan bertambah.",
				NAMA_BULAN, new String[] { "Pendapatan" }, new double[][] { totalJuta },
				new String[] { PALET[0] })).append("</div>");

		sb.append("<div>").append(HtmlChartHelper.barHorizontal(
				"Pemasukan per Bulan (juta Rp)",
				"Membandingkan besar pemasukan antar bulan. Batang terpanjang adalah bulan paling besar.",
				NAMA_BULAN, totalJuta, PALET[1])).append("</div>");

		sb.append("<div>").append(HtmlChartHelper.donut(
				"Cara Bayar: Tunai vs Non-Tunai",
				"Perbandingan uang yang diterima secara tunai dan lewat kartu/transfer (non-tunai).",
				new String[] { "Tunai", "Non-Tunai" }, new double[] { totalTunai, totalNonTunai },
				new String[] { PALET[2], PALET[3] }, rp(grandTotal))).append("</div>");

		sb.append("</div></div>");
		suntik(target, sb.toString());
	}

	/** "01".."12" → 0..11; -1 bila tak valid. */
	private static int parseBulanIndex(Object o) {
		if (o == null) {
			return -1;
		}
		try {
			int m = Integer.parseInt(o.toString().trim());
			return (m >= 1 && m <= 12) ? (m - 1) : -1;
		} catch (Exception e) {
			return -1;
		}
	}

	private static double angka(Object o) {
		return (o instanceof Number) ? ((Number) o).doubleValue() : 0;
	}

	private static double[] keJuta(double[] a) {
		double[] r = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			r[i] = Math.round(a[i] / 100000.0) / 10.0; // 1 desimal juta
		}
		return r;
	}

	/** Format rupiah dengan pemisah ribuan, mis. "Rp 12.500.000". */
	private static String rp(double v) {
		long bulat = Math.round(v);
		String s = String.valueOf(Math.abs(bulat));
		StringBuilder out = new StringBuilder();
		int c = 0;
		for (int i = s.length() - 1; i >= 0; i--) {
			out.append(s.charAt(i));
			if (++c % 3 == 0 && i > 0) {
				out.append('.');
			}
		}
		String angka = out.reverse().toString();
		return (bulat < 0 ? "-Rp " : "Rp ") + angka;
	}

	private static String persen(double bagian, double total) {
		if (total <= 0) {
			return "0%";
		}
		return Math.round(bagian * 100.0 / total) + "%";
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
					+ "border:1px solid #f5c2c7;border-radius:10px;\">Gagal memuat dasbor pendapatan. "
					+ "Silakan coba lagi atau hubungi admin.</div>");
			node.setParent(target);
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/sirs/chart/helper/PendapatanDashboardBuilder.java:260");
		}
	}
}
