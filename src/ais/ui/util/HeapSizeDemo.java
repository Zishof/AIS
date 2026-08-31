package ais.ui.util;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import ais.common.Common;

/**
 * Utilitas pemantauan penggunaan heap JVM yang dipanggil saat menyusun baris status admin (mis.
 * di {@code MainAction}/{@code MainAction2}) untuk menampilkan ringkasan memori bebas/terpakai
 * kepada administrator. Setiap pemanggilan {@link #check()} juga mencatat snapshot penggunaan
 * memori ke {@link #data}, membentuk riwayat sederhana yang dapat dipakai untuk melihat tren
 * pemakaian heap dari waktu ke waktu selama JVM berjalan (tidak persisten, hilang saat restart).
 *
 * <p>
 * <b>Catatan penting (lihat komentar rinci di {@link #check()})</b>: method ini murni method
 * TAMPILAN (read-only terhadap heap) dan TIDAK BOLEH memicu {@code Runtime.gc()} — versi
 * sebelumnya pernah memanggil {@code gc()} manual saat persentase pemakaian tinggi, yang
 * menyebabkan jeda Full GC stop-the-world setiap kali halaman status admin dirender. Pembersihan
 * memori manual tetap tersedia lewat jalur terpisah (tombol admin "Bersihkan Memori Tak
 * Terpakai" di layar Daftar Pengguna Online, dan guard memori terjadwal di
 * {@code UserOnlineCounter.runGc}).
 * </p>
 */
public class HeapSizeDemo {

	/**
	 * Riwayat snapshot penggunaan memori: kunci berupa stempel waktu (format
	 * {@code Common#datetimeFormat3s}), nilai berupa pasangan {@code {freeMemory, memoryInUse}}
	 * dalam MB. Tumbuh selama JVM berjalan (tidak pernah dibersihkan otomatis) dan dibungkus
	 * {@link Collections#synchronizedMap(Map)} agar aman diakses dari banyak thread (mis.
	 * beberapa admin membuka halaman status bersamaan).
	 */
	public static Map<String, Long[]> data =  Collections.synchronizedMap(new TreeMap<String, Long[]>());

	/**
	 * Menghitung dan mengembalikan ringkasan penggunaan heap JVM saat ini dalam bentuk teks
	 * ({@code "<bebas>MB/<terpakai>MB/<persen>%"}), sekaligus mencatat snapshot nilai bebas dan
	 * terpakai (dalam MB) ke {@link #data} dengan kunci stempel waktu saat ini. Persentase
	 * dihitung terhadap {@code Runtime#totalMemory()} (heap yang sudah di-commit JVM), BUKAN
	 * {@code Runtime#maxMemory()} (batas {@code -Xmx}) — lihat catatan kelas untuk implikasi
	 * penting dari pilihan ini terhadap keputusan GC manual.
	 *
	 * @return ringkasan teks penggunaan heap saat ini
	 */
	public static String check() {
		// java.io.PrintStream out = System.out;

		// Get an instance of the Runtime class
		Runtime runtime = Runtime.getRuntime();

		// To convert from Bytes to MegaBytes:
		// 1 MB = 1024 KB and 1 KB = 1024 Bytes.
		// Therefore, 1 MB = 1024 * 1024 Bytes.
		long MegaBytes = 1024 * 1024;

		// Memory which is currently available for use by heap
		long totalMemory = runtime.totalMemory() / MegaBytes;
		// out.println("Heap size available for use -> " + totalMemory + " MB");

		// Maximum memory which can be used if required.
		// The heap cannot grow beyond this size
		// long maxMemory = runtime.maxMemory() / MegaBytes;
		// out.println("Maximum memory Heap can use -> " + maxMemory + " MB");

		// Free memory still available
		long freeMemory = runtime.freeMemory() / MegaBytes;
		// out.println("Free memory in heap -> " + freeMemory + " MB");

		// Memory currently used by heap
		long memoryInUse = totalMemory - freeMemory;
		// out.println("Memory already used by heap -> " + memoryInUse + " MB");

		data.put(Common.datetimeFormat3s.get().format(WaktuUtil.getDate()), new Long[] { freeMemory, memoryInUse });

		double persen = memoryInUse * 100.0 / totalMemory;
		// CATATAN: dulu di sini ada runtime.gc() saat persen>85. Itu SALAH — check() adalah method
		// TAMPILAN (dipanggil saat menyusun baris status admin di MainAction/MainAction2), sehingga
		// memicu Full GC stop-the-world sebagai efek samping render halaman. Lebih parah: 'persen'
		// dihitung terhadap totalMemory() (heap yang SUDAH di-commit), BUKAN maxMemory(); jadi >85%
		// tercapai pada operasi normal (mis. committed 64G dipakai 56G) padahal masih jauh di bawah
		// -Xmx → System.gc() beruntun = akumulasi pause GC ratusan detik (terlihat di snapshot
		// performa, khususnya node ParallelGC). GC dibiarkan dikelola JVM. Pembersihan manual tetap
		// ada: tombol admin "Bersihkan Memori Tak Terpakai" (DaftarPenggunaOnline) & guard memori
		// terjadwal di UserOnlineCounter.runGc (config persen_auto_run_gc_baru).

		return freeMemory + "MB/" + memoryInUse + "MB/" + Common.numberFormat.get().format(persen) + "%";
	}
}