package ais.action.master.sirs.chart.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.zkoss.zul.Html;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.DiskusiUiHelper;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyChart;

/**
 * Pembangun dasbor <b>Ringkasan Pendaftaran Pasien</b> — memotret SELURUH jenis kunjungan
 * (Rawat Jalan, Rawat Inap, UGD, dan jenis lain bila ada) sepanjang satu tahun. Berbeda dari
 * {@link RawatJalanDashboardBuilder} yang fokus pada satu jenis (rawat jalan) dan memecahnya
 * per-poli, kelas ini memberi pandangan "helikopter" untuk manajemen: seberapa besar beban tiap
 * jalur layanan, kapan ramainya sepanjang tahun, dan tren naik-turunnya.
 *
 * <h3>Untuk apa dasbor ini (bahasa awam)</h3>
 * Menjawab: "Sepanjang tahun ini, berapa banyak pasien yang mendaftar, lewat jalur mana saja
 * (rawat jalan / rawat inap / UGD), dan bulan mana yang paling ramai?" Semua ditampilkan sebagai
 * gambar sederhana yang bisa dibaca sekilas tanpa perlu paham komputer.
 *
 * <h3>Grafik yang ditampilkan</h3>
 * <ol>
 *   <li><b>Kartu ringkasan (KPI)</b> — total pendaftaran setahun dan jumlah untuk tiap jalur
 *       layanan (rawat jalan, rawat inap, UGD), sehingga beban tiap jalur terlihat langsung.</li>
 *   <li><b>Garis tren bulanan</b> — satu garis untuk tiap jalur layanan sepanjang 12 bulan; mudah
 *       melihat jalur mana yang naik atau turun dan pada bulan berapa lonjakan terjadi.</li>
 *   <li><b>Lingkaran porsi (donat)</b> — perbandingan porsi tiap jalur layanan dari total
 *       pendaftaran setahun.</li>
 *   <li><b>Batang total per bulan</b> — total seluruh pendaftaran tiap bulan, untuk melihat
 *       pola musiman (bulan sibuk vs sepi).</li>
 * </ol>
 *
 * <h3>Cara kerja teknis</h3>
 * Data diambil dari {@code sirs.pendaftaran} dengan agregasi SQL: dikelompokkan per bulan
 * ({@code to_char(tanggalpendaftaran,'MM')}) dan per jenis kunjungan ({@code jenis}). Hasilnya
 * di-<i>pivot</i> di Java menjadi matriks {@code jumlah[jenis][bulan]} lalu diolah menjadi seluruh
 * grafik memakai {@link HtmlChartHelper} (HTML + CSS modern, tanpa JFreeChart). Daftar "jenis"
 * TIDAK di-hardcode: nilai apa pun yang muncul di data otomatis menjadi satu deret/warna, sehingga
 * penambahan jalur layanan baru di masa depan langsung tercakup tanpa mengubah kode ini.
 *
 * <h3>Manajemen session (PENTING)</h3>
 * Pembacaan memakai {@link HibernateUtil#currentSession()} (session request ZK) yang ditutup
 * otomatis oleh kerangka kerja — TIDAK ditutup manual di sini (menutupnya berisiko "Session is
 * closed!"). Kelas ini sengaja tidak membuka {@code openSession()}/{@code currentNativeSession()}
 * sehingga tak ada koneksi yang perlu ditutup sendiri (menghindari kebocoran koneksi).
 *
 * <h3>Ketahanan &amp; efisiensi</h3>
 * Semua akses koleksi/indeks dijaga dari {@code null} sehingga satu baris data aneh tidak
 * menggagalkan dasbor. Pivot dan seluruh total dihitung dalam satu-dua kali lintasan (hemat CPU),
 * {@link StringBuilder} berkapasitas awal dipakai agar perakitan HTML hemat memori, dan
 * {@link LinkedHashMap} menjaga urutan kemunculan jenis agar warna/legenda konsisten. Kelas util
 * statis murni (tanpa state) sehingga aman lintas request.
 *
 * <h3>Kompatibilitas</h3>
 * Java 1.7 / ZKoss 5.5 (tanpa lambda/diamond/Stream), {@code try/catch} gaya Java 1.6, memakai
 * helper bersama {@link HtmlChartHelper} agar gaya visual seragam dengan dasbor lain.
 *
 * @author AIS
 */
public final class PendaftaranOverviewDashboardBuilder {

	private static final String[] PALET = new String[] { "#1877f2", "#42b72a", "#f7b928", "#e4496b",
			"#8b5cf6", "#00a5b5", "#ff7a45", "#6b7280", "#eab308", "#14b8a6" };

	private static final String[] NAMA_BULAN = new String[] { "Jan", "Feb", "Mar", "Apr", "Mei",
			"Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des" };

	private PendaftaranOverviewDashboardBuilder() {
	}

	/**
	 * Menggambar dasbor ringkasan pendaftaran untuk satu tahun.
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

			String sql = "select to_char(tanggalpendaftaran,'MM') as bln, "
					+ "coalesce(nullif(trim(jenis),''),'(Lainnya)') as jns, count(*) as jml "
					+ "from sirs.pendaftaran where to_char(tanggalpendaftaran,'YYYY') = '" + th + "' "
					+ "group by 1, 2 order by 1, 2";

			List<Object[]> baris = session.createSQLQuery(sql).list();
			if (baris == null) {
				baris = new ArrayList<Object[]>();
			}

			// Pivot: jenis -> array 12 bulan. LinkedHashMap menjaga urutan kemunculan.
			Map<String, double[]> perJenis = new LinkedHashMap<String, double[]>();
			for (Object[] row : baris) {
				if (row == null || row.length < 3) {
					continue;
				}
				int bulanIdx = parseBulanIndex(row[0]); // 0..11
				if (bulanIdx < 0) {
					continue;
				}
				String jenis = row[1] == null ? "(Lainnya)" : row[1].toString();
				double jml = (row[2] instanceof Number) ? ((Number) row[2]).doubleValue() : 0;

				double[] arr = perJenis.get(jenis);
				if (arr == null) {
					arr = new double[12];
					perJenis.put(jenis, arr);
				}
				arr[bulanIdx] += jml;
			}

			gambar(target, th, perJenis);
		} catch (Exception e) {
			tampilkanGagal(target, e);
		}
	}

	// ─────────────────────────── internal ───────────────────────────

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

	private static void gambar(MyChart target, int tahun, Map<String, double[]> perJenis) {
		int nJenis = perJenis.size();
		String[] jenisNama = new String[nJenis];
		double[][] nilaiPerJenis = new double[nJenis][]; // [jenis][bulan]
		double[] totalPerJenis = new double[nJenis];
		double[] totalPerBulan = new double[12];
		double grandTotal = 0;

		int idx = 0;
		for (Map.Entry<String, double[]> e : perJenis.entrySet()) {
			jenisNama[idx] = e.getKey();
			double[] arr = e.getValue();
			nilaiPerJenis[idx] = arr;
			for (int b = 0; b < 12; b++) {
				double v = arr[b];
				totalPerJenis[idx] += v;
				totalPerBulan[b] += v;
				grandTotal += v;
			}
			idx++;
		}

		StringBuilder sb = new StringBuilder(8192);
		sb.append("<div style=\"font-family:inherit;display:flex;flex-direction:column;gap:14px;\">");
		sb.append("<div style=\"padding:12px 14px;border-radius:12px;background:linear-gradient(135deg,#eef4ff,#f7fbff);border:1px solid #dbe7ff;\">")
				.append("<div style=\"font-size:15px;font-weight:800;color:#1e3a8a;\">")
				.append(DiskusiUiHelper.escapeHtml("Ringkasan Pendaftaran Pasien — Tahun " + tahun)).append("</div>")
				.append("<div style=\"font-size:12px;color:#475569;margin-top:3px;\">")
				.append("Gambaran menyeluruh jumlah pasien yang mendaftar lewat tiap jalur layanan, dan bulan mana yang paling ramai.")
				.append("</div></div>");

		if (grandTotal <= 0) {
			sb.append("<div style=\"padding:26px;text-align:center;color:#64748b;border:1px dashed #cbd5e1;border-radius:12px;\">")
					.append("Belum ada data pendaftaran pada tahun ini.</div></div>");
			suntik(target, sb.toString());
			return;
		}

		// KPI: total + tiap jenis (maksimal 3 kartu jenis teratas agar rapi + kartu total).
		int jenisTeramai = argMax(totalPerJenis);
		List<String> kLabel = new ArrayList<String>();
		List<String> kNilai = new ArrayList<String>();
		List<String> kSub = new ArrayList<String>();
		kLabel.add("Total Pendaftaran");
		kNilai.add(fmt(grandTotal));
		kSub.add("pasien tahun " + tahun);
		for (int j = 0; j < nJenis; j++) {
			kLabel.add(jenisNama[j]);
			kNilai.add(fmt(totalPerJenis[j]));
			kSub.add(persen(totalPerJenis[j], grandTotal) + " dari total");
		}
		sb.append(HtmlChartHelper.kpiCards(toArr(kLabel), toArr(kNilai), toArr(kSub), null, null, PALET));

		sb.append("<div style=\"display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:14px;\">");

		// Tren bulanan per jalur layanan (garis multi-deret).
		sb.append("<div>").append(HtmlChartHelper.lineMulti(
				"Tren Pendaftaran per Bulan",
				"Satu garis untuk tiap jalur layanan sepanjang tahun. Garis menanjak berarti pasien bertambah pada bulan itu.",
				NAMA_BULAN, jenisNama, nilaiPerJenis, PALET)).append("</div>");

		// Porsi tiap jalur layanan (donat).
		sb.append("<div>").append(HtmlChartHelper.donut(
				"Porsi Tiap Jalur Layanan",
				"Perbandingan porsi rawat jalan, rawat inap, dan UGD dari total pendaftaran setahun.",
				jenisNama, totalPerJenis, PALET, fmt(grandTotal))).append("</div>");

		// Total pendaftaran per bulan (batang horizontal) — pola musiman.
		sb.append("<div>").append(HtmlChartHelper.barHorizontal(
				"Total Pendaftaran per Bulan",
				"Total seluruh pendaftaran tiap bulan. Batang terpanjang adalah bulan paling ramai.",
				NAMA_BULAN, totalPerBulan, PALET[0])).append("</div>");

		sb.append("</div></div>");
		suntik(target, sb.toString());

		// Hindari peringatan "variabel tak dipakai" untuk jenisTeramai bila suatu saat tak dipakai.
		if (jenisTeramai < 0) {
			return;
		}
	}

	private static int argMax(double[] a) {
		int best = -1;
		double maks = -1;
		for (int i = 0; i < a.length; i++) {
			if (a[i] > maks) {
				maks = a[i];
				best = i;
			}
		}
		return best;
	}

	private static String[] toArr(List<String> l) {
		return l.toArray(new String[l.size()]);
	}

	private static String persen(double bagian, double total) {
		if (total <= 0) {
			return "0%";
		}
		return Math.round(bagian * 100.0 / total) + "%";
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
					+ "border:1px solid #f5c2c7;border-radius:10px;\">Gagal memuat ringkasan pendaftaran. "
					+ "Silakan coba lagi atau hubungi admin.</div>");
			node.setParent(target);
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/sirs/chart/helper/PendaftaranOverviewDashboardBuilder.java:270");
		}
	}
}
