package ais.common;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Kelas demo/contoh pembelajaran (bukan bagian dari alur bisnis AIS) yang mengilustrasikan
 * perilaku {@link java.util.WeakHashMap}: entri pada map ini dapat dibuang secara otomatis
 * oleh garbage collector begitu <i>key</i>-nya tidak lagi memiliki referensi kuat (strong
 * reference) dari tempat lain di aplikasi.
 *
 * <p>
 * Skenario yang didemonstrasikan: satu entri dengan key berupa {@code String} yang sengaja
 * dibuat lewat {@code new String("Maine")} (bukan literal string dari <i>string pool</i>, agar
 * tidak ada referensi kuat lain yang menahannya) dimasukkan ke {@link WeakHashMap}. Sebuah
 * thread latar belakang kemudian memicu {@link System#gc()} secara berulang sambil memeriksa
 * apakah key tersebut masih ada di map. Karena tidak ada variabel lain yang memegang referensi
 * kuat ke objek {@code String} tersebut setelah baris {@code map.put(...)} selesai, pada suatu
 * titik garbage collector akan mengumpulkan objek key itu dan {@link WeakHashMap} otomatis
 * membuang entrinya — sehingga {@code map.containsKey("Maine")} pada akhirnya bernilai
 * {@code false} dan loop berhenti.
 * </p>
 *
 * <p>
 * <b>Catatan:</b> kelas ini murni untuk keperluan eksplorasi/pembelajaran perilaku JVM
 * (memory management, weak reference) dan tidak dipanggil dari bagian lain aplikasi AIS. Tidak
 * ada jaminan waktu pasti kapan garbage collector benar-benar berjalan meskipun
 * {@link System#gc()} dipanggil eksplisit — JVM hanya diberi "saran", bukan perintah mutlak.
 * </p>
 */
public class WeakHashMap_Demo {

	/**
	 * Map percontohan yang diisi dengan satu entri untuk didemonstrasikan perilaku
	 * penghapusan otomatisnya oleh garbage collector. Tipe mentah ({@code rawtypes}) sengaja
	 * dipakai sesuai kode aslinya.
	 */
	@SuppressWarnings("rawtypes")
	private static Map map;

	/**
	 * Titik masuk demo. Membuat {@link WeakHashMap} berisi satu entri key {@code "Maine"} yang
	 * sengaja dialokasikan sebagai objek {@code String} baru (bukan literal), lalu menjalankan
	 * thread terpisah yang memicu {@link System#gc()} berulang setiap 1500 milidetik sambil
	 * mengecek keberadaan key tersebut. Thread utama menunggu ({@link Thread#join()}) hingga
	 * thread demo selesai, yaitu setelah key otomatis terhapus dari map oleh garbage
	 * collector.
	 *
	 * @param args argumen baris perintah, tidak dipakai
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String args[]) {
		map = new WeakHashMap();
		map.put(new String("Maine"), "Augusta");

		Runnable runner = new Runnable() {
			public void run() {
				while (map.containsKey("Maine")) {
					try {
						Thread.sleep(1500);
					} catch (InterruptedException ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/WeakHashMap_Demo.java:21");
					}
					System.out.println("Thread waiting");
					System.gc();
				}
				
			}
		};
		Thread t = new Thread(runner);
		t.start();
		System.out.println("Main waiting");
		try {
			t.join();
		} catch (InterruptedException ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/WeakHashMap_Demo.java:34");
		}
	}
}