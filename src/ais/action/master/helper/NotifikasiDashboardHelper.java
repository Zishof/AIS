package ais.action.master.helper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import ais.common.NotifikasiCache;
import ais.ui.util.DashboardUiKit;

/**
 * <h1>NotifikasiDashboardHelper — Dasbor ringkas pemberitahuan (HTML/CSS modern, tanpa jfreechart)</h1>
 *
 * <p>
 * Helper ini merangkai satu halaman dasbor untuk modul Pemberitahuan/Notifikasi.
 * Tujuannya menjawab pertanyaan sederhana yang penting bagi pengelola: "berapa
 * banyak pemberitahuan yang beredar, sudah dibaca atau belum, jenisnya apa saja,
 * dan bagaimana perkembangannya beberapa hari terakhir?". Seluruh tampilan dibangun
 * dari potongan-potongan {@link DashboardUiKit} (kartu angka, donat komposisi,
 * garis tren/sparkline, dan baris progres) yang sepenuhnya berbasis HTML + CSS
 * modern — responsif di ponsel maupun desktop — sehingga <b>tidak</b> bergantung
 * pada pustaka grafik berat seperti jfreechart.
 * </p>
 *
 * <h2>Mengapa memakai kembali DashboardUiKit (prinsip reuse)</h2>
 * <p>
 * Daripada menulis HTML/CSS grafik dari nol di setiap dasbor, helper ini memanggil
 * komponen siap pakai {@link DashboardUiKit}. Keuntungannya: konsistensi tampilan di
 * seluruh aplikasi, satu titik perbaikan bila gaya/warna berubah, dan kode dasbor
 * yang ringkas serta mudah dipelihara. Komponen yang dipakai antara lain:
 * {@link DashboardUiKit#cards(List)} untuk kartu angka ringkas,
 * {@link DashboardUiKit#donut(String, String, LinkedHashMap, boolean, String)} untuk
 * komposisi jenis, {@link DashboardUiKit#sparkline(String, String, List, String, String)}
 * untuk tren harian, serta
 * {@link DashboardUiKit#progressLines(String, String, LinkedHashMap)} untuk rasio
 * dibaca/belum. Pola yang sama dapat diterapkan ke dasbor lain agar seragam.
 * </p>
 *
 * <h2>Sumber data: cache, bukan basis data</h2>
 * <p>
 * Seluruh angka dihitung dari {@link NotifikasiCache#snapshot()} — kumpulan
 * pemberitahuan terbaru yang sudah ada di memori (dihangatkan saat startup Tomcat).
 * Dengan demikian dasbor tampil hampir seketika tanpa membebani basis data, bahkan
 * saat banyak admin membukanya bersamaan. Konsekuensinya, angka mencerminkan jendela
 * "terbaru" (maksimal {@link NotifikasiCache#MAKS_SNAPSHOT} entri) dan menyegar
 * mengikuti TTL cache; ini memadai untuk pemantauan operasional sehari-hari. Bila di
 * kemudian hari dibutuhkan rentang historis penuh, cukup tambahkan satu metode
 * agregasi berbasis query di sini tanpa mengubah struktur tampilan.
 * </p>
 *
 * <h2>Bahasa deskripsi yang ramah orang awam</h2>
 * <p>
 * Setiap panel diberi keterangan satu kalimat yang sengaja dibuat sesederhana
 * mungkin dan menghindari istilah teknis, sehingga pengguna yang sama sekali tidak
 * memahami dunia teknologi informasi tetap langsung mengerti apa yang sedang dilihat.
 * Deskripsi tidak memakai frasa berpola "Panel ini ..."; ia langsung menjelaskan
 * manfaatnya dalam bahasa sehari-hari, misalnya "Naik-turun jumlah pemberitahuan tiap
 * hari selama sepekan terakhir.".
 * </p>
 *
 * <h2>Keamanan tampilan</h2>
 * <p>
 * Helper hanya menampilkan ANGKA hasil agregasi (jumlah, persentase, hitungan per
 * hari) dan label statis; ia tidak menampilkan isi pesan personal pengguna lain
 * sehingga aman dipakai pada dasbor pengelola. Bila terjadi kegagalan tak terduga
 * saat merangkai, {@link #buildHtml()} mengembalikan kartu pesan yang sopan alih-alih
 * melempar kesalahan, agar halaman induk tetap tampil utuh.
 * </p>
 *
 * <h2>Pemeliharaan</h2>
 * <p>
 * Menambah panel baru cukup dengan menambah satu blok pemanggilan komponen
 * {@link DashboardUiKit} pada {@link #buildHtml()}; mengubah ambang/skema warna cukup
 * di satu tempat. Karena perhitungan dipisah ke metode-metode kecil yang jelas
 * (komposisi jenis, tren harian, rasio dibaca), tiap bagian dapat diuji dan dirawat
 * secara mandiri. Inilah inti dari maksimalkan-reuse: logika tampilan terpusat di
 * {@link DashboardUiKit}, logika data terpusat di {@link NotifikasiCache}, dan helper
 * ini hanya "menjahit" keduanya menjadi satu dasbor yang utuh.
 * </p>
 *
 * <h2>Ide pengembangan lanjutan (pola yang sama, relevan untuk dasbor lain)</h2>
 * <p>
 * Dengan fondasi yang sama, dasbor ini dapat diperkaya secara bertahap: tambahkan
 * grafik batang horizontal ({@link DashboardUiKit#barList(String, String, java.util.Map, String, String, boolean, String)})
 * untuk menampilkan jenis peristiwa terbanyak (tugas, ujian, tagihan), grafik garis
 * ganda untuk membandingkan "dikirim" versus "dibaca" dari hari ke hari, atau diagram
 * jaring laba-laba ({@link DashboardUiKit#spider(String, String, String[], int[])})
 * untuk memetakan kanal penyampaian (aplikasi, email, WhatsApp, push). Pola yang sama —
 * "ambil angka dari cache, gambar dengan DashboardUiKit, beri satu kalimat deskripsi
 * sederhana" — dapat diterapkan ke dasbor akademik, keuangan, maupun kepegawaian
 * sehingga seluruh aplikasi memiliki bahasa visual yang konsisten, ringan, responsif,
 * dan mudah dipahami pengguna awam. Karena setiap komponen sudah responsif secara
 * bawaan, dasbor otomatis rapi baik di layar ponsel yang sempit maupun di monitor
 * lebar tanpa kode tambahan.
 * </p>
 */
public final class NotifikasiDashboardHelper {

	private NotifikasiDashboardHelper() {
	}

	private static final String HIJAU = "#16a34a";
	private static final String KUNING = "#f59e0b";
	private static final String MERAH = "#dc2626";

	/**
	 * Rangkai seluruh dasbor pemberitahuan sebagai satu string HTML siap pasang ke
	 * komponen {@code <html>} ZK. Aman terhadap kegagalan (mengembalikan pesan sopan).
	 *
	 * @return markup HTML dasbor
	 */
	public static String buildHtml() {
		try {
			List<NotifikasiCache.Item> data = NotifikasiCache.snapshot();
			if (data == null) {
				data = new ArrayList<NotifikasiCache.Item>();
			}

			int total = data.size();
			int belum = 0;
			int hariIni = 0;
			int info = 0, warning = 0, danger = 0, lain = 0;

			Calendar c0 = Calendar.getInstance();
			c0.set(Calendar.HOUR_OF_DAY, 0);
			c0.set(Calendar.MINUTE, 0);
			c0.set(Calendar.SECOND, 0);
			c0.set(Calendar.MILLISECOND, 0);
			long awalHariIni = c0.getTimeInMillis();

			for (NotifikasiCache.Item it : data) {
				if (!it.isBuka()) {
					belum++;
				}
				if (it.getWaktu() != null && it.getWaktu().getTime() >= awalHariIni) {
					hariIni++;
				}
				String s = it.getStatusNotif() == null ? "" : it.getStatusNotif().trim().toUpperCase();
				if ("INFO".equals(s)) {
					info++;
				} else if ("WARNING".equals(s)) {
					warning++;
				} else if ("DANGER".equals(s)) {
					danger++;
				} else {
					lain++;
				}
			}
			int sudah = total - belum;

			StringBuilder sb = new StringBuilder();
			sb.append("<div style='padding:4px 2px;'>");

			// Pengantar — kalimat sederhana untuk orang awam.
			sb.append(DashboardUiKit.introBanner("Ringkasan Pemberitahuan",
					"Gambaran cepat semua pemberitahuan terbaru: berapa yang sudah dibaca, jenisnya apa saja, dan bagaimana naik-turunnya beberapa hari terakhir."));

			// Kartu angka ringkas.
			List<DashboardUiKit.Stat> stats = new ArrayList<DashboardUiKit.Stat>();
			stats.add(new DashboardUiKit.Stat("Total Terbaru", String.valueOf(total),
					"Pemberitahuan terbaru yang tercatat.", DashboardUiKit.PRIMARY));
			stats.add(new DashboardUiKit.Stat("Belum Dibaca", String.valueOf(belum),
					"Masih menunggu dibuka penerima.", KUNING));
			stats.add(new DashboardUiKit.Stat("Sudah Dibaca", String.valueOf(sudah),
					"Sudah dibuka oleh penerima.", HIJAU));
			stats.add(new DashboardUiKit.Stat("Hari Ini", String.valueOf(hariIni),
					"Dikirim pada hari ini.", DashboardUiKit.ACCENT));
			sb.append(DashboardUiKit.cards(stats));

			// Grid responsif berisi grafik.
			sb.append(DashboardUiKit.openGrid(280));

			// Komposisi jenis (donat).
			LinkedHashMap<String, Double> komposisi = new LinkedHashMap<String, Double>();
			komposisi.put("Biasa (Info)", (double) info);
			komposisi.put("Peringatan", (double) warning);
			komposisi.put("Mendesak", (double) danger);
			if (lain > 0) {
				komposisi.put("Lainnya", (double) lain);
			}
			sb.append(DashboardUiKit.donut("Jenis Pemberitahuan",
					"Perbandingan pemberitahuan biasa, yang berupa peringatan, dan yang mendesak.", komposisi, false,
					"Belum ada pemberitahuan untuk ditampilkan."));

			// Tren 7 hari (sparkline).
			LinkedHashMap<String, Integer> perHari = hitungPerHari(data, 7);
			List<Integer> nilaiTren = new ArrayList<Integer>(perHari.values());
			sb.append(DashboardUiKit.sparkline("Tren 7 Hari Terakhir",
					"Naik-turun jumlah pemberitahuan tiap hari selama sepekan terakhir.", nilaiTren,
					DashboardUiKit.PRIMARY, "Data tren belum cukup untuk ditampilkan."));

			// Rasio dibaca (progress).
			LinkedHashMap<String, Integer> rasio = new LinkedHashMap<String, Integer>();
			rasio.put("Sudah dibaca", total > 0 ? (int) Math.round(sudah * 100.0 / total) : 0);
			rasio.put("Belum dibaca", total > 0 ? (int) Math.round(belum * 100.0 / total) : 0);
			sb.append(DashboardUiKit.progressLines("Sudah Dibaca vs Belum",
					"Seberapa banyak pemberitahuan yang sudah benar-benar dibuka penerima.", rasio));

			sb.append(DashboardUiKit.closeGrid());
			sb.append("</div>");
			return sb.toString();
		} catch (Throwable e) {
			return "<div style='padding:14px;border:1px solid #e2e8f0;border-radius:12px;background:#fff;color:#64748b;'>"
					+ "Ringkasan pemberitahuan sedang disiapkan. Silakan muat ulang sebentar lagi.</div>";
		}
	}

	/**
	 * Hitung jumlah pemberitahuan per hari untuk {@code jumlahHari} hari terakhir
	 * (termasuk hari ini), berurutan dari paling lama ke paling baru.
	 *
	 * @param data       snapshot pemberitahuan
	 * @param jumlahHari banyaknya hari yang ditinjau
	 * @return peta label-hari ("dd/MM") menuju jumlah pemberitahuan hari itu
	 */
	private static LinkedHashMap<String, Integer> hitungPerHari(List<NotifikasiCache.Item> data, int jumlahHari) {
		SimpleDateFormat kunci = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat label = new SimpleDateFormat("dd/MM", new Locale("id", "ID"));

		LinkedHashMap<String, Integer> hasil = new LinkedHashMap<String, Integer>();
		LinkedHashMap<String, String> kunciKeLabel = new LinkedHashMap<String, String>();

		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, -(jumlahHari - 1));
		for (int i = 0; i < jumlahHari; i++) {
			String k = kunci.format(c.getTime());
			kunciKeLabel.put(k, label.format(c.getTime()));
			hasil.put(label.format(c.getTime()), 0);
			c.add(Calendar.DATE, 1);
		}

		if (data != null) {
			for (NotifikasiCache.Item it : data) {
				Date w = it.getWaktu();
				if (w == null) {
					continue;
				}
				String k = kunci.format(w);
				String lbl = kunciKeLabel.get(k);
				if (lbl != null) {
					hasil.put(lbl, hasil.get(lbl) + 1);
				}
			}
		}
		return hasil;
	}
}
