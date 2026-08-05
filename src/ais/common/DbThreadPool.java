package ais.common;

/**
 * Pembatas jumlah thread paralel untuk tugas latar yang membuka koneksi database
 * (Hibernate Session -&gt; koneksi c3p0).
 *
 * <p>
 * MASALAH yang dicegah: pool koneksi c3p0 (hibernate.c3p0.max_size) berukuran terbatas
 * (mis. 80). Banyak proses memakai {@code Executors.newFixedThreadPool(50..250)} yang
 * masing-masing thread-nya membuka Session/koneksi. Bila total thread yang memegang/
 * meminta koneksi melampaui max_size, SEMUA thread (termasuk AJP/HTTP request) akan
 * menunggu pada {@code BasicResourcePool.checkoutResource} sehingga Tomcat tidak bisa
 * diakses (terlihat "hang" walau CPU/heap normal).
 * </p>
 *
 * <p>
 * {@link #safe(int)} menahan jumlah thread pada plafon AMAN yang jauh di bawah max_size,
 * sehingga selalu tersisa koneksi untuk request web normal. Plafon dapat diatur lewat
 * konfigurasi {@code max_thread_db_paralel} (default 16, maksimum keras 32). Tugas yang
 * melebihi plafon akan ANTRE (bukan gagal), jauh lebih stabil daripada menghabiskan pool.
 * </p>
 */
public final class DbThreadPool {

	/** Batas atas keras agar konfigurasi yang keliru tidak mengembalikan pool raksasa. */
	private static final int HARD_MAX = 32;

	/** Plafon default bila konfigurasi tidak ada / tidak valid. */
	private static final int DEFAULT_CEILING = 16;

	private DbThreadPool() {
	}

	/** Plafon paralelisme aman (dari konfigurasi {@code max_thread_db_paralel}). */
	public static int ceiling() {
		int max = DEFAULT_CEILING;
		try {
			ais.database.model.Konfigurasi k = Common.getKonfigurasi("max_thread_db_paralel",
					String.valueOf(DEFAULT_CEILING));
			if (k != null && k.getNilai() != null && k.getNilai().trim().length() > 0) {
				max = Integer.parseInt(k.getNilai().trim());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DbThreadPool.java:43");
			// abaikan -> pakai default
		}
		if (max < 1) {
			max = 1;
		}
		if (max > HARD_MAX) {
			max = HARD_MAX;
		}
		return max;
	}

	/**
	 * Kembalikan jumlah thread AMAN: minimal 1, maksimal {@link #ceiling()}.
	 *
	 * @param requested jumlah thread yang diminta kode (mis. 100). Akan dipotong ke plafon.
	 */
	public static int safe(int requested) {
		int max = ceiling();
		if (requested < 1) {
			return 1;
		}
		if (requested > max) {
			return max;
		}
		return requested;
	}
}
