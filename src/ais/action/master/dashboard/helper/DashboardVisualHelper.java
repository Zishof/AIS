package ais.action.master.dashboard.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.ui.util.DashboardUiKit;

/**
 * Pembungkus visual bersama (reuse) untuk dasbor yang sebelumnya hanya menampilkan tabel/Excel.
 *
 * <h3>Untuk apa</h3>
 * <p>
 * Banyak dasbor menampilkan angka mentah dalam bentuk tabel/spreadsheet sehingga sulit dibaca cepat.
 * Helper ini mengubah data yang sudah dihitung menjadi ringkasan visual ringan (kartu angka, donat
 * komposisi, dan batang peringkat) memakai {@link DashboardUiKit} - seluruhnya HTML/CSS, tanpa
 * JFreeChart. Hasilnya dipasang DI ATAS tabel yang sudah ada, jadi tabel/Excel tetap utuh.
 * </p>
 *
 * <h3>Cara kerja</h3>
 * <p>
 * Method statik di sini menerima data yang sudah dimuat dasbor (mis. {@code List<Object[]>} hasil
 * SQL, atau pasangan label-nilai) lalu mengembalikan potongan HTML siap dimasukkan ke sebuah
 * {@code org.zkoss.zul.Html}. Karena hanya menghasilkan teks HTML, aman dipanggil dari thread
 * event ZK maupun langsung saat menyusun tampilan.
 * </p>
 *
 * <p>Kompatibel Java 1.6/1.7 (tanpa lambda, stream, atau diamond operator).</p>
 */
public class DashboardVisualHelper {

	private DashboardVisualHelper() {
	}

	/**
	 * Ringkasan kehadiran (untuk rekap absensi dosen/guru/siswa). Membaca jumlah Hadir, Alpa,
	 * Sakit, Izin dari tiap baris lalu menyusun kartu persentase kehadiran, donat komposisi, dan
	 * batang peringkat kehadiran terbanyak.
	 *
	 * @param rows     data hasil SQL (tiap elemen satu baris dengan kolom-kolom angka).
	 * @param idxNama  indeks kolom nama (untuk peringkat); -1 bila tidak ada.
	 * @param idxHadir indeks kolom Hadir.
	 * @param idxAlpa  indeks kolom Alpa/Tidak hadir.
	 * @param idxSakit indeks kolom Sakit.
	 * @param idxIzin  indeks kolom Izin.
	 */
	public static String kehadiran(List<Object[]> rows, int idxNama, int idxHadir, int idxAlpa, int idxSakit,
			int idxIzin) {
		long hadir = 0;
		long alpa = 0;
		long sakit = 0;
		long izin = 0;
		LinkedHashMap<String, Double> perOrang = new LinkedHashMap<String, Double>();
		if (rows != null) {
			for (int r = 0; r < rows.size(); r++) {
				Object[] baris = rows.get(r);
				if (baris == null) {
					continue;
				}
				long h = angka(baris, idxHadir);
				hadir += h;
				alpa += angka(baris, idxAlpa);
				sakit += angka(baris, idxSakit);
				izin += angka(baris, idxIzin);
				String nama = teks(baris, idxNama);
				if (nama.length() > 0) {
					Double old = perOrang.get(nama);
					perOrang.put(nama, Double.valueOf((old == null ? 0 : old.doubleValue()) + h));
				}
			}
		}
		long totalPertemuan = hadir + alpa + sakit + izin;
		int persenHadir = totalPertemuan <= 0 ? 0 : (int) Math.round(hadir * 100.0 / totalPertemuan);

		List<DashboardUiKit.Stat> stats = new ArrayList<DashboardUiKit.Stat>();
		stats.add(new DashboardUiKit.Stat("Tingkat Kehadiran", persenHadir + "%",
				"Bagian hadir dari seluruh pertemuan", DashboardUiKit.GOOD));
		stats.add(new DashboardUiKit.Stat("Hadir", String.valueOf(hadir), "Jumlah kehadiran tercatat",
				DashboardUiKit.PRIMARY));
		stats.add(new DashboardUiKit.Stat("Tidak Hadir", String.valueOf(alpa), "Tanpa keterangan (alpa)",
				DashboardUiKit.BAD));
		stats.add(new DashboardUiKit.Stat("Sakit / Izin", String.valueOf(sakit + izin),
				"Sakit " + sakit + ", izin " + izin, DashboardUiKit.WARN));

		LinkedHashMap<String, Double> komposisi = new LinkedHashMap<String, Double>();
		komposisi.put("Hadir", Double.valueOf(hadir));
		komposisi.put("Tidak Hadir", Double.valueOf(alpa));
		komposisi.put("Sakit", Double.valueOf(sakit));
		komposisi.put("Izin", Double.valueOf(izin));

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='padding:4px 2px 12px;'>");
		sb.append(DashboardUiKit.cards(stats));
		sb.append(DashboardUiKit.openGrid(320));
		sb.append(DashboardUiKit.donut("Komposisi Kehadiran",
				"Perbandingan hadir, tidak hadir, sakit, dan izin.", komposisi, false, "Belum ada data"));
		sb.append(DashboardUiKit.barList("Kehadiran Terbanyak", "Nama dengan jumlah hadir paling banyak.",
				topN(perOrang, 8), DashboardUiKit.PRIMARY, "kali", false, "Belum ada data"));
		sb.append(DashboardUiKit.closeGrid());
		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * Ringkasan komposisi umum: kartu total + donat perbandingan + batang peringkat. Cocok untuk
	 * data "jumlah per kelompok" seperti stok per jenis barang.
	 *
	 * @param judulKonteks kata untuk judul, mis. "Stok".
	 * @param satuan       satuan angka, mis. "item".
	 * @param data         pasangan nama kelompok -> jumlah.
	 */
	public static String komposisi(String judulKonteks, String satuan, LinkedHashMap<String, Double> data) {
		double total = 0;
		int kelompokIsi = 0;
		String terbanyak = "-";
		double maks = -1;
		if (data != null) {
			for (Map.Entry<String, Double> e : data.entrySet()) {
				double v = e.getValue() == null ? 0 : e.getValue().doubleValue();
				total += v;
				if (v > 0) {
					kelompokIsi++;
				}
				if (v > maks) {
					maks = v;
					terbanyak = e.getKey();
				}
			}
		}

		List<DashboardUiKit.Stat> stats = new ArrayList<DashboardUiKit.Stat>();
		stats.add(new DashboardUiKit.Stat("Total " + judulKonteks, DashboardUiKit.money(total),
				"Jumlah keseluruhan", DashboardUiKit.PRIMARY));
		stats.add(new DashboardUiKit.Stat("Jumlah Kelompok", String.valueOf(kelompokIsi),
				"Kelompok yang memiliki isi", DashboardUiKit.ACCENT));
		stats.add(new DashboardUiKit.Stat("Terbanyak", terbanyak,
				maks < 0 ? "" : (DashboardUiKit.money(maks) + " " + satuan), DashboardUiKit.GOOD));

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='padding:4px 2px 12px;'>");
		sb.append(DashboardUiKit.cards(stats));
		sb.append(DashboardUiKit.openGrid(320));
		sb.append(DashboardUiKit.donut("Komposisi " + judulKonteks, "Perbandingan jumlah antar kelompok.",
				data, false, "Belum ada data"));
		sb.append(DashboardUiKit.barList("Peringkat " + judulKonteks, "Kelompok dengan jumlah terbanyak di atas.",
				topN(data, 10), DashboardUiKit.PRIMARY, satuan, false, "Belum ada data"));
		sb.append(DashboardUiKit.closeGrid());
		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * Tren dua seri (mis. Tagihan vs Dibayar) per periode + komposisi total. Dipakai dasbor
	 * keuangan per-bulan agar tampilan tren konsisten.
	 */
	public static String trenDuaSeri(String judul, String desc, List<String> labels, List<Double> seri1,
			String namaSeri1, List<Double> seri2, String namaSeri2, String judulKomposisi) {
		double total1 = 0;
		double total2 = 0;
		if (seri1 != null) {
			for (int i = 0; i < seri1.size(); i++) {
				total1 += seri1.get(i) == null ? 0 : seri1.get(i).doubleValue();
			}
		}
		if (seri2 != null) {
			for (int i = 0; i < seri2.size(); i++) {
				total2 += seri2.get(i) == null ? 0 : seri2.get(i).doubleValue();
			}
		}
		LinkedHashMap<String, Double> komposisi = new LinkedHashMap<String, Double>();
		komposisi.put(namaSeri1, Double.valueOf(total1));
		komposisi.put(namaSeri2, Double.valueOf(total2));

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='padding:4px 2px 12px;'>");
		sb.append(DashboardUiKit.openGrid(320));
		sb.append(DashboardUiKit.dualLineChart(judul, desc, labels, seri1, namaSeri1, DashboardUiKit.PRIMARY, seri2,
				namaSeri2, DashboardUiKit.GOOD));
		sb.append(DashboardUiKit.donut(judulKomposisi, "Perbandingan total " + namaSeri1 + " dan " + namaSeri2 + ".",
				komposisi, true, "Belum ada data"));
		sb.append(DashboardUiKit.closeGrid());
		sb.append("</div>");
		return sb.toString();
	}

	// ============================================================ Util internal

	private static LinkedHashMap<String, Double> topN(LinkedHashMap<String, Double> data, int n) {
		LinkedHashMap<String, Double> hasil = new LinkedHashMap<String, Double>();
		if (data == null || data.isEmpty()) {
			return hasil;
		}
		List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(data.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> a, Map.Entry<String, Double> b) {
				double va = a.getValue() == null ? 0 : a.getValue().doubleValue();
				double vb = b.getValue() == null ? 0 : b.getValue().doubleValue();
				return va < vb ? 1 : (va > vb ? -1 : 0);
			}
		});
		int batas = Math.min(n, list.size());
		for (int i = 0; i < batas; i++) {
			hasil.put(list.get(i).getKey(), list.get(i).getValue());
		}
		return hasil;
	}

	private static long angka(Object[] baris, int idx) {
		if (idx < 0 || baris == null || baris.length <= idx || baris[idx] == null) {
			return 0;
		}
		try {
			return (long) Double.parseDouble(baris[idx].toString().trim());
		} catch (Exception e) {
			return 0;
		}
	}

	private static String teks(Object[] baris, int idx) {
		if (idx < 0 || baris == null || baris.length <= idx || baris[idx] == null) {
			return "";
		}
		String s = baris[idx].toString().trim();
		return s.length() > 60 ? s.substring(0, 60) : s;
	}
}
