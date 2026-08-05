package ais.ui.util;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import ais.common.Common;

public class HeapSizeDemo {

	public static Map<String, Long[]> data =  Collections.synchronizedMap(new TreeMap<String, Long[]>());

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