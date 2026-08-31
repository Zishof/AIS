package ais.action.master.sirs.chart.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zul.Html;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.DiskusiUiHelper;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyChart;

/**
 * Pembangun dasbor <b>Kewaspadaan Kadaluarsa Obat/Barang Medis (Farmasi)</b> — memberi peringatan dini
 * atas item yang sudah kadaluarsa maupun yang akan segera kadaluarsa, supaya petugas farmasi/gudang
 * bisa menarik, menukar, atau memakai lebih dulu barang yang mendekati batas waktu (prinsip
 * <i>First-Expired-First-Out</i>) sebelum menjadi kerugian atau membahayakan pasien.
 *
 * <h3>Untuk apa dasbor ini (bahasa awam)</h3>
 * Menjawab: "Barang apa saja yang sudah kadaluarsa atau tinggal sebentar lagi kadaluarsa?" Item
 * dikelompokkan menjadi empat kondisi — <b>sudah kadaluarsa</b>, <b>kurang dari 30 hari</b>,
 * <b>30–90 hari</b>, dan <b>lebih dari 90 hari</b> — lalu ditampilkan sebagai kartu angka, batang,
 * dan lingkaran porsi, ditambah daftar barang yang paling dekat tanggal kadaluarsanya. Semua mudah
 * dibaca tanpa perlu paham istilah komputer.
 *
 * <h3>Grafik yang ditampilkan</h3>
 * <ol>
 *   <li><b>Kartu ringkasan (KPI)</b> — jumlah catatan kadaluarsa, berapa yang sudah lewat, berapa yang
 *       kurang dari 30 hari, dan berapa yang kurang dari 90 hari.</li>
 *   <li><b>Batang per kondisi</b> — perbandingan jumlah catatan di tiap kelompok waktu kadaluarsa.</li>
 *   <li><b>Lingkaran porsi (donat)</b> — seberapa besar bagian tiap kondisi dari seluruh catatan.</li>
 *   <li><b>Batang barang paling mendesak</b> — daftar item yang paling dekat kadaluarsa beserta sisa
 *       hari; batang lebih pendek berarti lebih mendesak untuk ditindaklanjuti.</li>
 * </ol>
 *
 * <h3>Cara kerja teknis</h3>
 * Data diambil dari {@code sirs.kadaluarsa} (kolom {@code tanggal_kadaluarsa}, dan {@code item} yang
 * di-<i>join</i> ke {@code sirs.item_medis} untuk nama barang). Pengelompokan kondisi waktu dilakukan
 * langsung di database dengan ekspresi {@code CASE} dan {@code interval} PostgreSQL — sehingga yang
 * ditarik ke memori hanya angka ringkas per kelompok plus 10 item paling mendesak (hemat memori,
 * cepat). Grafik digambar dengan {@link HtmlChartHelper} (HTML + CSS modern, tanpa JFreeChart). Karena
 * ini kondisi terkini, tidak ada pilihan tahun/bulan (bersifat <i>snapshot</i> relatif terhadap
 * tanggal hari ini di server).
 *
 * <h3>Manajemen session (PENTING)</h3>
 * Pembacaan memakai {@link HibernateUtil#currentSession()} (session request ZK) yang ditutup otomatis
 * — TIDAK ditutup manual (menutupnya berisiko "Session is closed!"). Kelas ini tidak membuka
 * {@code openSession()}/{@code currentNativeSession()} sehingga tidak ada koneksi yang perlu ditutup
 * sendiri (menghindari kebocoran koneksi/pool).
 *
 * <h3>Ketahanan &amp; efisiensi</h3>
 * Agregasi seluruhnya di sisi database; hanya empat kelompok + maksimal sepuluh item mendesak yang
 * masuk memori; akses indeks/koleksi dijaga dari {@code null}; {@link StringBuilder} berkapasitas awal;
 * data kosong ditangani dengan pesan ramah. Kelas util statis murni, aman dipakai lintas request.
 *
 * <h3>Kompatibilitas</h3>
 * Java 1.7 / ZKoss 5.5 (tanpa lambda/diamond/Stream), {@code try/catch} gaya Java 1.6, memakai helper
 * bersama {@link HtmlChartHelper}.
 *
 * @author AIS
 */
public final class KadaluarsaFarmasiDashboardBuilder {

	/** Label empat kondisi waktu kadaluarsa, sesuai indeks bucket 0..3 dari query. */
	private static final String[] LABEL_BUCKET = new String[] { "Sudah Kadaluarsa", "Kurang 30 Hari",
			"30 - 90 Hari", "Lebih 90 Hari" };

	/** Warna per kondisi: merah (lewat), oranye (mendesak), kuning (waspada), hijau (aman). */
	private static final String[] WARNA_BUCKET = new String[] { "#e4496b", "#ff7a45", "#f7b928", "#42b72a" };

	private static final String[] PALET_ITEM = new String[] { "#e4496b" };

	private KadaluarsaFarmasiDashboardBuilder() {
	}

	/**
	 * Menggambar dasbor kewaspadaan kadaluarsa (kondisi terkini).
	 *
	 * @param target wadah {@link MyChart} tempat dasbor digambar (tak boleh null).
	 */
	@SuppressWarnings("unchecked")
	public static void render(MyChart target) {
		if (target == null) {
			return;
		}
		try {
			Data d = data();
			gambar(target, d.bucket, d.item);
		} catch (Exception e) {
			tampilkanGagal(target, e);
		}
	}

	/** Rekap kadaluarsa: jumlah per rentang waktu, dan sepuluh item terdekat. */
	public static final class Data {
		/** Tiap baris: {bucket 0..3, jumlah}. */
		public final List<Object[]> bucket;
		/** Tiap baris: {nama item, sisa hari}. */
		public final List<Object[]> item;

		Data(List<Object[]> bucket, List<Object[]> item) {
			this.bucket = bucket;
			this.item = item;
		}
	}

	/**
	 * Data mentah dasbor, terpisah dari penyajiannya.
	 *
	 * <p>Dipakai bersama layar ZK dan kontrak native supaya kueri serta
	 * agregasinya hanya ada di satu tempat.</p>
	 */
	@SuppressWarnings("unchecked")
	public static Data data() {
		Session session = HibernateUtil.currentSession(); // request session — JANGAN ditutup manual.

		// Hitung jumlah catatan kadaluarsa per kondisi waktu, langsung di database.
		String sqlBucket = "select case "
				+ "when a.tanggal_kadaluarsa < now() then 0 "
				+ "when a.tanggal_kadaluarsa <= now() + interval '30 day' then 1 "
				+ "when a.tanggal_kadaluarsa <= now() + interval '90 day' then 2 "
				+ "else 3 end as bucket, count(*) as jml "
				+ "from sirs.kadaluarsa a where a.tanggal_kadaluarsa is not null group by 1 order by 1";
		List<Object[]> barisBucket = session.createSQLQuery(sqlBucket).list();
		if (barisBucket == null) {
			barisBucket = new ArrayList<Object[]>();
		}

		// Sepuluh item paling dekat kadaluarsa (belum lewat) beserta sisa hari.
		String sqlItem = "select coalesce(nullif(trim(c.nama),''), '(Tanpa Nama)') as nama, "
				+ "(CAST(a.tanggal_kadaluarsa AS date) - CAST(now() AS date)) as sisa_hari "
				+ "from sirs.kadaluarsa a left join sirs.item_medis c on (a.item = c.id) "
				+ "where a.tanggal_kadaluarsa >= now() order by a.tanggal_kadaluarsa asc limit 10";
		List<Object[]> barisItem = session.createSQLQuery(sqlItem).list();
		if (barisItem == null) {
			barisItem = new ArrayList<Object[]>();
		}
		return new Data(barisBucket, barisItem);
	}

	// ─────────────────────────── internal ───────────────────────────

	private static void gambar(MyChart target, List<Object[]> barisBucket, List<Object[]> barisItem) {
		double[] bucket = new double[4]; // 0..3 sesuai LABEL_BUCKET
		for (int i = 0; i < barisBucket.size(); i++) {
			Object[] row = barisBucket.get(i);
			if (row == null || row.length < 2) {
				continue;
			}
			int idx = (row[0] instanceof Number) ? ((Number) row[0]).intValue() : -1;
			double v = (row[1] instanceof Number) ? ((Number) row[1]).doubleValue() : 0;
			if (idx >= 0 && idx < 4) {
				bucket[idx] = v;
			}
		}
		double total = bucket[0] + bucket[1] + bucket[2] + bucket[3];

		StringBuilder sb = new StringBuilder(8192);
		sb.append("<div style=\"font-family:inherit;display:flex;flex-direction:column;gap:14px;\">");
		sb.append("<div style=\"padding:12px 14px;border-radius:12px;background:linear-gradient(135deg,#fff1f2,#fff7ed);border:1px solid #fecaca;\">")
				.append("<div style=\"font-size:15px;font-weight:800;color:#9f1239;\">")
				.append("Kewaspadaan Kadaluarsa Obat / Barang Medis</div>")
				.append("<div style=\"font-size:12px;color:#475569;margin-top:3px;\">")
				.append("Peringatan dini barang yang sudah atau akan segera kadaluarsa, agar bisa segera ditindaklanjuti.")
				.append("</div></div>");

		if (total <= 0) {
			sb.append("<div style=\"padding:26px;text-align:center;color:#64748b;border:1px dashed #cbd5e1;border-radius:12px;\">")
					.append("Belum ada catatan tanggal kadaluarsa.</div></div>");
			suntik(target, sb.toString());
			return;
		}

		String[] kLabel = new String[] { "Total Catatan Kadaluarsa", "Sudah Kadaluarsa",
				"Kurang 30 Hari", "Kurang 90 Hari" };
		String[] kNilai = new String[] { fmt(total), fmt(bucket[0]), fmt(bucket[1]),
				fmt(bucket[1] + bucket[2]) };
		String[] kSub = new String[] { "batch tercatat", "perlu ditarik", "segera pakai/tukar",
				"pantau ketat" };
		sb.append(HtmlChartHelper.kpiCards(kLabel, kNilai, kSub, null, null,
				new String[] { "#0ea5e9", "#e4496b", "#ff7a45", "#f7b928" }));

		sb.append("<div style=\"display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:14px;\">");

		sb.append("<div>").append(HtmlChartHelper.barHorizontal(
				"Jumlah per Kondisi Waktu",
				"Perbandingan banyaknya catatan di tiap kelompok waktu kadaluarsa (merah = sudah lewat).",
				LABEL_BUCKET, bucket, WARNA_BUCKET[0])).append("</div>");

		sb.append("<div>").append(HtmlChartHelper.donut(
				"Porsi Kondisi Kadaluarsa",
				"Seberapa besar bagian tiap kondisi dari seluruh catatan tanggal kadaluarsa.",
				LABEL_BUCKET, bucket, WARNA_BUCKET, fmt(total))).append("</div>");

		sb.append("</div>");

		// Daftar item paling mendesak (sisa hari terkecil).
		int m = barisItem.size();
		if (m > 0) {
			String[] namaItem = new String[m];
			double[] sisaHari = new double[m];
			for (int i = 0; i < m; i++) {
				Object[] row = barisItem.get(i);
				namaItem[i] = (row != null && row.length > 0 && row[0] != null) ? row[0].toString()
						: "(Tanpa Nama)";
				double v = (row != null && row.length > 1 && row[1] instanceof Number)
						? ((Number) row[1]).doubleValue() : 0;
				sisaHari[i] = v;
			}
			sb.append("<div>").append(HtmlChartHelper.barHorizontal(
					"Barang Paling Mendesak (Sisa Hari)",
					"Item yang paling dekat kadaluarsa beserta sisa harinya; batang lebih pendek berarti lebih mendesak.",
					namaItem, sisaHari, PALET_ITEM[0])).append("</div>");
		}

		sb.append("</div>");
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
					+ "border:1px solid #f5c2c7;border-radius:10px;\">Gagal memuat dasbor kadaluarsa farmasi. "
					+ "Silakan coba lagi atau hubungi admin.</div>");
			node.setParent(target);
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/sirs/chart/helper/KadaluarsaFarmasiDashboardBuilder.java:216");
		}
	}
}
