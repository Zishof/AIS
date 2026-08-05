package ais.action.master.sister;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;

import ais.common.DataSisterApi;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DataSister;
import ais.ui.util.BaseDasbordPortal;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyHtml;

/**
 * <h2>Pembangun Tampilan Ringkasan (Dasbor) Data SISTER — dapat dipakai ulang</h2>
 *
 * <p>
 * Kelas bantu (util) berisi <b>satu-satunya</b> tempat logika penggambaran ringkasan visual data SISTER,
 * agar dipakai bersama oleh dua layar: (1) dasbor mandiri {@link DasbordSinkronisasiSister} yang dibuka dari
 * tombol "Sister" pada bilah atas, dan (2) panel ringkasan yang disisipkan di halaman kelola Data SISTER
 * ({@code DataSisterAction}). Dengan memusatkan logika di sini, pemeliharaan di kemudian hari cukup dilakukan
 * di satu berkas.
 * </p>
 *
 * <h3>Isi ringkasan yang digambar</h3>
 * <ol>
 *   <li><b>Kartu angka (KPI)</b>: total baris tersimpan, jumlah jenis tabel referensi, data aktif, data
 *       non-aktif.</li>
 *   <li><b>Grafik batang</b>: 12 jenis referensi dengan data terbanyak.</li>
 *   <li><b>Grafik donat</b>: perbandingan data aktif vs non-aktif.</li>
 *   <li><b>Grafik radar / jaring laba-laba</b>: komposisi data pada lima kelompok besar.</li>
 *   <li><b>Grafik garis (tren)</b>: aktivitas penyimpanan/pembaruan data 30 hari terakhir.</li>
 * </ol>
 *
 * <p>
 * Semua grafik digambar sebagai <b>HTML/CSS modern</b> lewat {@link HtmlChartHelper} (bukan JFreeChart)
 * sehingga otomatis responsif di ponsel maupun komputer, dan diberi <b>penjelasan sederhana</b> agar mudah
 * dipahami pengguna non-teknis. Perhitungan memakai <b>query agregat</b> ({@code GROUP BY}/{@code COUNT})
 * pada {@code currentSession()} milik request ZK — <b>tidak menutup sesi</b> (dikelola kerangka kerja) dan
 * ringan meski tabel berisi puluhan ribu baris. Kompatibel Java 1.7 (tanpa lambda/stream/diamond).
 * </p>
 *
 * @author e-Campus
 */
public final class DasborSisterUiHelper {

	private DasborSisterUiHelper() {
	}

	/**
	 * Membangun seluruh ringkasan (panel + kartu KPI + 5 grafik) ke dalam {@code host}. Bila belum ada data,
	 * menampilkan ajakan untuk melakukan sinkronisasi. Aman: bagian radar &amp; tren dibungkus try/catch.
	 */
	public static void bangunRingkasan(Component host) {
		if (host == null) {
			return;
		}
		Map<String, Long> perTabel = DataSisterApi.ringkasanPerTabel();
		long total = 0;
		for (Long v : perTabel.values()) {
			total += (v == null ? 0 : v.longValue());
		}
		int jenis = perTabel.size();
		long aktif = hitungAktif(true);
		long nonAktif = hitungAktif(false);

		Component panel = BaseDasbordPortal.panelTunggal(host, "Ringkasan Data SISTER",
				"Gambaran singkat berapa banyak data dari SISTER yang sudah tersimpan di sistem.");

		if (total == 0) {
			new MyHtml("<div style='padding:18px;text-align:center;color:#65676b;font-size:14px;'>"
					+ "Belum ada data SISTER yang tersimpan. Silakan klik tombol "
					+ "<b>Sinkronkan Data Referensi</b> untuk menariknya dari server SISTER.</div>").setParent(panel);
			return;
		}

		// (1) Kartu angka (KPI)
		String kpi = HtmlChartHelper.kpiCards(
				new String[] { "Total Data", "Jenis Referensi", "Data Aktif", "Data Non-aktif" },
				new String[] { fmt(total), fmt(jenis), fmt(aktif), fmt(nonAktif) },
				new String[] { "baris tersimpan dari SISTER", "macam daftar referensi", "sedang dipakai",
						"disembunyikan" },
				(String[]) null, (boolean[]) null, new String[] { "#1877f2", "#00a884", "#f7b928", "#8a8d91" });
		new MyHtml(kpi).setParent(panel);

		// (2) Grafik batang: 12 jenis referensi dengan data terbanyak
		List<Map.Entry<String, Long>> entries = new ArrayList<Map.Entry<String, Long>>(perTabel.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, Long>>() {
			@Override
			public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
				long va = a.getValue() == null ? 0 : a.getValue().longValue();
				long vb = b.getValue() == null ? 0 : b.getValue().longValue();
				return (vb < va) ? -1 : (vb > va ? 1 : 0);
			}
		});
		int topN = Math.min(12, entries.size());
		String[] labels = new String[topN];
		double[] values = new double[topN];
		for (int i = 0; i < topN; i++) {
			labels[i] = pendekTabel(entries.get(i).getKey());
			values[i] = entries.get(i).getValue() == null ? 0 : entries.get(i).getValue().doubleValue();
		}
		String bar = HtmlChartHelper.barHorizontal("Data Terbanyak per Jenis Referensi",
				"Jenis data SISTER yang paling banyak tersimpan di sistem (12 teratas).", labels, values, "#1877f2");
		new MyHtml(bar).setParent(panel);

		// (3) Grafik donat: aktif vs non-aktif
		String donut = HtmlChartHelper.donut("Data Aktif vs Non-aktif",
				"Perbandingan data yang sedang dipakai dengan yang disembunyikan.",
				new String[] { "Aktif", "Non-aktif" }, new double[] { aktif, nonAktif },
				new String[] { "#00a884", "#8a8d91" }, "data");
		new MyHtml(donut).setParent(panel);

		// (4) Grafik radar/spider: komposisi per kelompok
		try {
			bangunRadar(panel, perTabel);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sister/DasborSisterUiHelper.java:126");
		}

		// (5) Grafik tren: aktivitas pembaruan 30 hari
		try {
			bangunTren(panel);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sister/DasborSisterUiHelper.java:133");
		}
	}

	/** Menghitung jumlah baris {@link DataSister} berdasarkan status aktif (currentSession, tidak ditutup). */
	public static long hitungAktif(boolean aktif) {
		try {
			Criteria c = HibernateUtil.currentSession().createCriteria(DataSister.class);
			if (aktif) {
				c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			} else {
				c.add(Restrictions.eq("aktif", false));
			}
			Object o = c.setProjection(Projections.rowCount()).uniqueResult();
			return o == null ? 0 : ((Number) o).longValue();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sister/DasborSisterUiHelper.java:149");
			return 0;
		}
	}

	/** Grafik radar/jaring laba-laba: komposisi data SISTER pada lima kelompok besar. */
	private static void bangunRadar(Component host, Map<String, Long> perTabel) {
		String[] kategori = { "SDM & Mahasiswa", "Wilayah & Institusi", "Kepegawaian", "Kegiatan & Tridharma",
				"Referensi Akademik" };
		double[] nilai = new double[kategori.length];
		for (Map.Entry<String, Long> e : perTabel.entrySet()) {
			String kat = kategoriTabel(e.getKey());
			long v = e.getValue() == null ? 0 : e.getValue().longValue();
			for (int i = 0; i < kategori.length; i++) {
				if (kategori[i].equals(kat)) {
					nilai[i] += v;
					break;
				}
			}
		}
		double max = 1;
		for (int i = 0; i < nilai.length; i++) {
			if (nilai[i] > max) {
				max = nilai[i];
			}
		}
		String radar = HtmlChartHelper.radar("Komposisi Data per Kelompok",
				"Sebaran banyaknya data pada tiap kelompok besar; makin jauh dari pusat berarti makin banyak.",
				kategori, new String[] { "Jumlah data" }, new double[][] { nilai }, new String[] { "#1877f2" }, max);
		new MyHtml(radar).setParent(host);
	}

	/** Grafik garis (tren) aktivitas penyimpanan/pembaruan data 30 hari terakhir. */
	@SuppressWarnings("unchecked")
	private static void bangunTren(Component host) {
		List<Object[]> rows = HibernateUtil.currentSession()
				.createSQLQuery("select to_char(tanggal_dirubah,'YYYY-MM-DD') as d, count(*) as c "
						+ "from public.data_sister where tanggal_dirubah >= now() - interval '30 day' "
						+ "group by 1 order by 1")
				.list();
		if (rows == null || rows.isEmpty()) {
			return;
		}
		int n = rows.size();
		String[] kategori = new String[n];
		double[] nilai = new double[n];
		for (int i = 0; i < n; i++) {
			Object[] r = rows.get(i);
			kategori[i] = r[0] == null ? "" : r[0].toString();
			nilai[i] = r[1] == null ? 0 : ((Number) r[1]).doubleValue();
		}
		String tren = HtmlChartHelper.lineMulti("Aktivitas Pembaruan Data (30 Hari Terakhir)",
				"Berapa banyak data SISTER yang tersimpan atau diperbarui pada tiap hari.", kategori,
				new String[] { "Data diperbarui" }, new double[][] { nilai }, new String[] { "#00a884" });
		new MyHtml(tren).setParent(host);
	}

	/** Memetakan nama endpoint/tabel ke salah satu dari lima kelompok besar untuk grafik radar. */
	private static String kategoriTabel(String nama) {
		String s = pendekTabel(nama);
		if (s == null) {
			return "Referensi Akademik";
		}
		if (s.contains("sdm") || s.contains("mahasiswa")) {
			return "SDM & Mahasiswa";
		}
		if (s.contains("wilayah") || s.equals("negara") || s.contains("perguruan") || s.contains("unit_kerja")
				|| s.contains("profil_pt") || s.contains("dudi") || s.contains("bidang_usaha")) {
			return "Wilayah & Institusi";
		}
		if (s.contains("jabatan") || s.contains("golongan") || s.contains("pangkat") || s.contains("kepegawaian")
				|| s.contains("ikatan_kerja") || s.contains("gaji") || s.contains("tunjangan")
				|| s.contains("pekerjaan") || s.contains("status_kepegawaian")) {
			return "Kepegawaian";
		}
		if (s.contains("kegiatan") || s.contains("publikasi") || s.contains("penghargaan") || s.contains("skim")
				|| s.contains("kepanitiaan") || s.contains("diklat") || s.contains("beasiswa")
				|| s.contains("kesejahteraan") || s.contains("tes") || s.contains("penelitian")
				|| s.contains("pengabdian") || s.contains("pengajaran")) {
			return "Kegiatan & Tridharma";
		}
		return "Referensi Akademik";
	}

	/** Memperpendek nama endpoint agar enak dibaca pada grafik (buang prefix "referensi/" &amp; query). */
	public static String pendekTabel(String nama) {
		if (nama == null) {
			return "";
		}
		String s = nama;
		int tanya = s.indexOf('?');
		if (tanya >= 0) {
			s = s.substring(0, tanya);
		}
		int garis = s.lastIndexOf('/');
		if (garis >= 0 && garis < s.length() - 1) {
			s = s.substring(garis + 1);
		}
		return s;
	}

	/** Memformat angka dengan pemisah ribuan. */
	public static String fmt(long n) {
		return new DecimalFormat("#,##0").format(n);
	}
}
