package ais.action.master.sirs.chart.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zul.Html;

import ais.common.Common;
import ais.common.CommonSirs;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Poly;
import ais.ui.util.DiskusiUiHelper;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyChart;

/**
 * Pembangun (builder) dasbor <b>Kunjungan Pasien Rawat Jalan</b> — dipakai bersama oleh dua layar
 * ringkasan: rekap per-<i>minggu</i> dalam satu bulan ({@code RawatJalanMingguanDashboardAction})
 * dan rekap per-<i>bulan</i> dalam satu tahun ({@code RawatJalanBulananDashboardAction}). Kelas ini
 * menyatukan SELURUH logika pengambilan data dan penggambaran grafik di SATU tempat sehingga kedua
 * layar cukup memanggil satu baris ({@link #renderMingguan(MyChart, Integer, Integer)} atau
 * {@link #renderBulanan(MyChart, Integer)}), dan setiap penyempurnaan tampilan/kalkulasi cukup
 * dilakukan sekali di sini (prinsip <i>reuse</i> untuk memudahkan pemeliharaan).
 *
 * <h3>Untuk apa dasbor ini (bahasa awam)</h3>
 * Dasbor ini menjawab pertanyaan sehari-hari manajemen klinik/rumah sakit: "Berapa banyak pasien
 * rawat jalan yang datang, kapan ramainya, dan poli mana yang paling sibuk?" Semua ditampilkan
 * sebagai gambar sederhana (kartu angka, garis naik-turun, batang perbandingan, lingkaran porsi,
 * dan jaring laba-laba) yang bisa dibaca sekilas tanpa perlu mengerti komputer.
 *
 * <h3>Grafik yang ditampilkan</h3>
 * <ol>
 *   <li><b>Kartu ringkasan (KPI)</b> — total kunjungan, rata-rata per periode, poli teramai, serta
 *       jumlah poli yang menerima pasien. Memberi gambaran besar dalam sekali pandang.</li>
 *   <li><b>Garis tren</b> — naik-turunnya jumlah pasien dari waktu ke waktu (per minggu atau per
 *       bulan). Garis menanjak berarti kunjungan bertambah.</li>
 *   <li><b>Batang perbandingan poli</b> — membandingkan jumlah pasien antar poli; batang terpanjang
 *       adalah poli paling ramai.</li>
 *   <li><b>Lingkaran porsi (donat)</b> — seberapa besar bagian tiap poli dari keseluruhan
 *       kunjungan.</li>
 *   <li><b>Jaring laba-laba (radar)</b> — "bentuk" sebaran kunjungan antar poli; makin melebar ke
 *       satu arah, makin dominan poli tersebut.</li>
 * </ol>
 *
 * <h3>Cara kerja teknis</h3>
 * Data diambil dari tabel {@code sirs.pendaftaran} yang berjenis "Rawat Jalan". Untuk mode mingguan,
 * baris periode berasal dari tabel bantu {@code minggu} yang diisi terlebih dahulu lewat
 * {@link CommonSirs#getMinggu(int, int)}; untuk mode bulanan, baris periode adalah bulan 01..12.
 * Untuk tiap periode dihitung jumlah pasien per poli memakai agregasi SQL ({@code sum(case ... )}),
 * menghasilkan matriks {@code nilai[periode][poli]} yang lalu diolah menjadi seluruh grafik. Grafik
 * digambar sepenuhnya dengan <b>HTML + CSS modern</b> lewat {@link HtmlChartHelper} (tanpa JFreeChart
 * maupun applet), sehingga ringan, responsif di layar ponsel maupun desktop, dan konsisten dengan
 * dasbor lain di aplikasi. Hasil akhir disuntikkan ke dalam wadah {@link MyChart} (yang pada dasarnya
 * sebuah {@code Div}) sebagai satu simpul {@link Html}.
 *
 * <h3>Manajemen session (PENTING)</h3>
 * Seluruh pembacaan memakai {@link HibernateUtil#currentSession()} — yakni session milik request ZK
 * yang sedang berjalan. Session jenis ini <b>TIDAK</b> ditutup manual di sini karena akan ditutup
 * otomatis oleh kerangka kerja di akhir request; menutupnya manual justru berisiko "Session is
 * closed!" pada pemakai lain di request yang sama. Kelas ini sengaja tidak membuka
 * {@code openSession()}/{@code currentNativeSession()} agar tidak ada koneksi yang perlu ditutup
 * sendiri (menghindari kebocoran koneksi c3p0).
 *
 * <h3>Ketahanan &amp; efisiensi</h3>
 * Semua akses koleksi dijaga dari {@code null}/indeks di luar batas (mis. jumlah kolom SQL tidak
 * sinkron dengan jumlah poli), sehingga satu baris data yang aneh tidak menggagalkan seluruh dasbor.
 * Kalkulasi total/rata-rata dilakukan dalam SATU kali lintasan (single pass) atas matriks data untuk
 * hemat CPU, dan {@link StringBuilder} berkapasitas awal dipakai agar perakitan HTML hemat memori
 * (menghindari realokasi berulang). Kelas bersifat util statis murni (tak menyimpan state) sehingga
 * aman dipakai lintas request tanpa penguncian.
 *
 * <h3>Kompatibilitas</h3>
 * Ditulis untuk Java 1.7 dan ZKoss 5.5 (tanpa lambda, diamond operator, atau Stream API), memakai
 * blok {@code try/catch} gaya Java 1.6. Bergantung pada helper bersama {@link HtmlChartHelper} agar
 * gaya visual seragam dengan seluruh dasbor aplikasi.
 *
 * @author AIS
 */
public final class RawatJalanDashboardBuilder {

	/** Palet warna konsisten dengan {@link HtmlChartHelper} (dipakai untuk deret grafik). */
	private static final String[] PALET = new String[] { "#1877f2", "#42b72a", "#f7b928", "#e4496b",
			"#8b5cf6", "#00a5b5", "#ff7a45", "#6b7280", "#eab308", "#14b8a6" };

	/** Util statis murni — tak boleh diinstansiasi. */
	private RawatJalanDashboardBuilder() {
	}

	/**
	 * Menggambar dasbor kunjungan rawat jalan <b>per minggu</b> untuk satu bulan &amp; tahun tertentu.
	 *
	 * @param target wadah {@link MyChart} tempat dasbor digambar (tak boleh null).
	 * @param tahun  tahun yang ditinjau; bila null dipakai tahun berjalan.
	 * @param bulan  bulan 1..12 yang ditinjau; bila null dipakai bulan berjalan.
	 */
	public static void renderMingguan(MyChart target, Integer tahun, Integer bulan) {
		if (target == null) {
			return;
		}
		int th = tahun == null ? Calendar.getInstance().get(Calendar.YEAR) : tahun.intValue();
		int bl = bulan == null ? (Calendar.getInstance().get(Calendar.MONTH) + 1) : bulan.intValue();
		try {
			Data d = dataMingguan(th, bl);
			gambar(target, d.judul, d.satuanPeriode, d.periodeLabels, d.polies, d.matriks);
		} catch (Exception e) {
			tampilkanGagal(target, e);
		}
	}

	/**
	 * Menggambar dasbor kunjungan rawat jalan <b>per bulan</b> sepanjang satu tahun tertentu.
	 *
	 * @param target wadah {@link MyChart} tempat dasbor digambar (tak boleh null).
	 * @param tahun  tahun yang ditinjau; bila null dipakai tahun berjalan.
	 */
	public static void renderBulanan(MyChart target, Integer tahun) {
		if (target == null) {
			return;
		}
		int th = tahun == null ? Calendar.getInstance().get(Calendar.YEAR) : tahun.intValue();
		try {
			Data d = dataBulanan(th);
			gambar(target, d.judul, d.satuanPeriode, d.periodeLabels, d.polies, d.matriks);
		} catch (Exception e) {
			tampilkanGagal(target, e);
		}
	}

	/** Matriks kunjungan {@code nilai[periode][poli]} beserta label barisnya. */
	public static final class Data {
		public final String judul;
		public final String satuanPeriode;
		public final List<String> periodeLabels;
		public final List<Poly> polies;
		public final List<double[]> matriks;

		Data(String judul, String satuanPeriode, List<String> periodeLabels, List<Poly> polies,
				List<double[]> matriks) {
			this.judul = judul;
			this.satuanPeriode = satuanPeriode;
			this.periodeLabels = periodeLabels;
			this.polies = polies;
			this.matriks = matriks;
		}
	}

	/**
	 * Data kunjungan rawat jalan per minggu dalam satu bulan.
	 *
	 * <p>Tabel bantu {@code minggu} disiapkan lebih dulu; tanpa itu bulan yang
	 * belum pernah dibuka tidak menghasilkan satu baris pun.</p>
	 */
	public static Data dataMingguan(int tahun, int bulan) {
		CommonSirs.getMinggu(bulan, tahun);
		Session session = HibernateUtil.currentSession(); // request session — JANGAN ditutup manual.
		List<Poly> polies = ambilPoly(session);

		StringBuilder sql = new StringBuilder(512);
		sql.append("select to_char(max(m.tanggal_mulai),'DD-MM') || ' s.d ' || to_char(max(m.tanggal_sampai),'DD-MM') as periode, ");
		appendKolomPoli(sql, polies);
		sql.append("-1 from minggu m inner join pendaftaran aa on (date(aa.tanggalpendaftaran) between date(m.tanggal_mulai) and date(m.tanggal_sampai)) ")
				.append("where aa.jenis = 'Rawat Jalan' and m.bulan = ").append(bulan)
				.append(" and m.tahun = ").append(tahun)
				.append(" group by m.id order by date(max(m.tanggal_mulai))");

		return dariSql(session, sql.toString(), polies,
				"Kunjungan Pasien Rawat Jalan — per Minggu, Bulan " + bulan + " Tahun " + tahun, "minggu");
	}

	/** Data kunjungan rawat jalan per bulan sepanjang satu tahun. */
	public static Data dataBulanan(int tahun) {
		Session session = HibernateUtil.currentSession(); // request session — JANGAN ditutup manual.
		List<Poly> polies = ambilPoly(session);

		StringBuilder sql = new StringBuilder(512);
		sql.append("select to_char(aa.tanggalpendaftaran,'MM') as periode, ");
		appendKolomPoli(sql, polies);
		sql.append("-1 from sirs.pendaftaran aa where aa.jenis = 'Rawat Jalan' ")
				.append("and to_char(aa.tanggalpendaftaran,'YYYY') = '").append(tahun).append("' ")
				.append("group by to_char(aa.tanggalpendaftaran,'MM') order by periode");

		return dariSql(session, sql.toString(), polies,
				"Kunjungan Pasien Rawat Jalan — per Bulan, Tahun " + tahun, "bulan");
	}

	// ─────────────────────────── internal ───────────────────────────

	/** Ambil seluruh poli (guard null → list kosong). */
	@SuppressWarnings("unchecked")
	private static List<Poly> ambilPoly(Session session) {
		List<Poly> polies = session.createCriteria(Poly.class).list();
		return polies == null ? new ArrayList<Poly>() : polies;
	}

	/** Tambahkan kolom agregasi {@code sum(case aa.poly when <id> then 1 else 0 end)} untuk tiap poli. */
	private static void appendKolomPoli(StringBuilder sql, List<Poly> polies) {
		for (int i = 0; i < polies.size(); i++) {
			Poly poly = polies.get(i);
			if (poly == null || poly.getId() == null) {
				continue;
			}
			sql.append("sum(case aa.poly when ").append(poly.getId()).append(" then 1 else 0 end) as poli_")
					.append(poly.getId()).append(", ");
		}
	}

	/**
	 * Menjalankan SQL agregasi lalu menyusun matriks {@code nilai[periode][poli]}.
	 * Kolom pertama tiap baris = label periode (String); kolom berikutnya = jumlah pasien per poli.
	 */
	@SuppressWarnings("unchecked")
	private static Data dariSql(Session session, String sql, List<Poly> polies, String judul,
			String satuanPeriode) {
		List<Object[]> baris = session.createSQLQuery(sql).list();
		if (baris == null) {
			baris = new ArrayList<Object[]>();
		}

		int nPoli = polies.size();
		List<String> periodeLabels = new ArrayList<String>();
		List<double[]> matriks = new ArrayList<double[]>(); // tiap elemen = nilai per poli utk satu periode

		for (Object[] kolom : baris) {
			if (kolom == null || kolom.length == 0) {
				continue;
			}
			String label = kolom[0] == null ? "" : kolom[0].toString();
			double[] perPoli = new double[nPoli];
			int idxPoli = 0;
			for (int c = 1; c < kolom.length && idxPoli < nPoli; c++) {
				Object v = kolom[c];
				// Kolom penutup sentinel "-1" (bukan angka poli) diabaikan bila terbaca.
				double nilai = (v instanceof Number) ? ((Number) v).doubleValue() : 0;
				if (nilai < 0) {
					continue;
				}
				perPoli[idxPoli] = nilai;
				idxPoli++;
			}
			periodeLabels.add(label);
			matriks.add(perPoli);
		}
		return new Data(judul, satuanPeriode, periodeLabels, polies, matriks);
	}

	/** Rakit seluruh grafik HTML/CSS dari data yang sudah tersedia dan suntikkan ke wadah. */
	private static void gambar(MyChart target, String judul, String satuanPeriode,
			List<String> periodeLabels, List<Poly> polies, List<double[]> matriks) {

		int nPeriode = periodeLabels.size();
		int nPoli = polies.size();

		String[] poliNama = new String[nPoli];
		for (int j = 0; j < nPoli; j++) {
			Poly p = polies.get(j);
			poliNama[j] = (p == null || p.getNama() == null) ? ("Poli " + (j + 1)) : p.getNama();
		}

		// Single-pass: total per periode, total per poli, grand total.
		double[] totalPerPeriode = new double[nPeriode];
		double[] totalPerPoli = new double[nPoli];
		double grandTotal = 0;
		for (int i = 0; i < nPeriode; i++) {
			double[] row = matriks.get(i);
			for (int j = 0; j < nPoli; j++) {
				double v = (row != null && j < row.length) ? row[j] : 0;
				totalPerPeriode[i] += v;
				totalPerPoli[j] += v;
				grandTotal += v;
			}
		}

		int poliTeramai = -1;
		double maksPoli = -1;
		int poliAktif = 0;
		for (int j = 0; j < nPoli; j++) {
			if (totalPerPoli[j] > 0) {
				poliAktif++;
			}
			if (totalPerPoli[j] > maksPoli) {
				maksPoli = totalPerPoli[j];
				poliTeramai = j;
			}
		}
		double rataPerPeriode = nPeriode > 0 ? (grandTotal / nPeriode) : 0;
		String[] labelPeriode = periodeLabels.toArray(new String[periodeLabels.size()]);

		StringBuilder sb = new StringBuilder(8192);
		sb.append("<div style=\"font-family:inherit;display:flex;flex-direction:column;gap:14px;\">");

		// Judul + penjelasan singkat, bahasa awam.
		sb.append("<div style=\"padding:12px 14px;border-radius:12px;background:linear-gradient(135deg,#eef4ff,#f7fbff);")
				.append("border:1px solid #dbe7ff;\">")
				.append("<div style=\"font-size:15px;font-weight:800;color:#1e3a8a;\">")
				.append(DiskusiUiHelper.escapeHtml(judul)).append("</div>")
				.append("<div style=\"font-size:12px;color:#475569;margin-top:3px;\">")
				.append("Ringkasan berapa banyak pasien rawat jalan yang datang, kapan ramainya, dan poli mana yang paling sibuk.")
				.append("</div></div>");

		if (nPeriode == 0 || grandTotal <= 0) {
			sb.append("<div style=\"padding:26px;text-align:center;color:#64748b;border:1px dashed #cbd5e1;")
					.append("border-radius:12px;\">Belum ada data kunjungan rawat jalan pada periode ini.</div>");
			sb.append("</div>");
			suntik(target, sb.toString());
			return;
		}

		// 1) Kartu ringkasan (KPI).
		String namaTeramai = poliTeramai >= 0 ? poliNama[poliTeramai] : "-";
		String[] kpiLabel = new String[] { "Total Kunjungan", "Rata-rata per " + satuanPeriode,
				"Poli Teramai", "Poli Menerima Pasien" };
		String[] kpiNilai = new String[] { fmt(grandTotal), fmt(rataPerPeriode), namaTeramai,
				poliAktif + " poli" };
		String[] kpiSub = new String[] { "pasien rawat jalan", "pasien tiap " + satuanPeriode,
				poliTeramai >= 0 ? (fmt(maksPoli) + " pasien") : "", "dari " + nPoli + " poli" };
		sb.append(HtmlChartHelper.kpiCards(kpiLabel, kpiNilai, kpiSub, null, null, PALET));

		// Grid responsif untuk grafik (1 kolom di ponsel, banyak kolom di desktop).
		sb.append("<div style=\"display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:14px;\">");

		// 2) Tren jumlah pasien per periode (garis).
		double[][] trenNilai = new double[][] { totalPerPeriode };
		sb.append("<div>").append(HtmlChartHelper.lineMulti(
				"Tren Kunjungan per " + satuanPeriode,
				"Naik-turunnya jumlah pasien dari waktu ke waktu. Garis menanjak berarti kunjungan bertambah.",
				labelPeriode, new String[] { "Total Pasien" }, trenNilai,
				new String[] { PALET[0] })).append("</div>");

		// 3) Perbandingan poli (batang horizontal).
		sb.append("<div>").append(HtmlChartHelper.barHorizontal(
				"Perbandingan Antar Poli",
				"Membandingkan jumlah pasien di tiap poli. Batang terpanjang adalah poli paling ramai.",
				poliNama, totalPerPoli, PALET[1])).append("</div>");

		// 4) Porsi tiap poli (donat).
		sb.append("<div>").append(HtmlChartHelper.donut(
				"Porsi Kunjungan per Poli",
				"Seberapa besar bagian tiap poli dari seluruh kunjungan. Irisan terbesar = poli paling banyak dikunjungi.",
				poliNama, totalPerPoli, PALET, fmt(grandTotal))).append("</div>");

		// 5) Jaring laba-laba sebaran poli (radar) — hanya bila cukup poli agar terbaca.
		if (nPoli >= 3) {
			double[][] radarNilai = new double[][] { totalPerPoli };
			sb.append("<div>").append(HtmlChartHelper.radar(
					"Sebaran Kunjungan Antar Poli",
					"Bentuk sebaran kunjungan antar poli. Makin melebar ke satu arah, makin dominan poli tersebut.",
					poliNama, new String[] { "Distribusi" }, radarNilai,
					new String[] { PALET[3] }, maksPoli)).append("</div>");
		}

		sb.append("</div>"); // grid
		sb.append("</div>"); // root

		suntik(target, sb.toString());
	}

	/** Bersihkan wadah lalu tempel satu simpul HTML berisi seluruh dasbor. */
	private static void suntik(MyChart target, String html) {
		Common.clear(target);
		Html node = new Html(html);
		node.setParent(target);
	}

	/** Tampilkan pesan gagal yang ramah bila terjadi error (tanpa menjatuhkan halaman). */
	private static void tampilkanGagal(MyChart target, Exception e) {
		try {
			Common.tampilErrorJikaAdmin(e);
			Common.clear(target);
			Html node = new Html("<div style=\"padding:20px;color:#a94442;background:#f8d7da;"
					+ "border:1px solid #f5c2c7;border-radius:10px;\">Gagal memuat dasbor kunjungan rawat jalan. "
					+ "Silakan coba lagi atau hubungi admin.</div>");
			node.setParent(target);
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/sirs/chart/helper/RawatJalanDashboardBuilder.java:344");
			// jangan pernah menggagalkan render karena kegagalan menampilkan pesan gagal.
		}
	}

	/** Format angka bulat ramah (tanpa desimal untuk hitungan pasien). */
	private static String fmt(double v) {
		return String.valueOf(Math.round(v));
	}
}
