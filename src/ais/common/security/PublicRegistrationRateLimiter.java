package ais.common.security;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h3>Rate limiter in-JVM utk route publik {@code /pendaftaran} (anti-automation §13.3).</h3>
 *
 * <p>Sliding-window sederhana per kunci (IP / normalized email / normalized username --
 * pemanggil membentuk kunci {@code "jenis|nilai"}). In-JVM CUKUP utk tujuan anti-abuse dasar
 * (pola sama MemoryCacheUtil repo ini; multi-node berarti limit per-node -- diterima, dicatat
 * di 08-security.md). Fail-open bila terjadi error internal: limiter tidak boleh mematikan
 * pendaftaran yang sah.</p>
 */
public final class PublicRegistrationRateLimiter {

	/** timestamp (ms) percobaan per kunci, disimpan sebagai long[] ring kecil. */
	private static final ConcurrentHashMap<String, long[]> JEJAK = new ConcurrentHashMap<String, long[]>();
	private static final int MAKS_ENTRI = 50000;

	private PublicRegistrationRateLimiter() {
	}

	/**
	 * @param kunci      mis. {@code "ip|203.0.113.7"} / {@code "email|a@b.c"} / {@code "user|toko1"}
	 * @param maksimal   jumlah percobaan yang diizinkan dalam jendela
	 * @param jendelaMs  panjang jendela (ms)
	 * @return true bila percobaan ini DIIZINKAN (dan tercatat); false bila melebihi limit.
	 */
	public static boolean izinkan(String kunci, int maksimal, long jendelaMs) {
		try {
			if (kunci == null || kunci.trim().isEmpty() || maksimal <= 0) {
				return true;
			}
			long sekarang = System.currentTimeMillis();
			bersihkanBilaPenuh(sekarang, jendelaMs);
			synchronized (kunciIntern(kunci)) {
				long[] lama = JEJAK.get(kunci);
				int hidup = 0;
				long[] baru = new long[maksimal];
				if (lama != null) {
					for (int i = 0; i < lama.length; i++) {
						if (lama[i] > 0 && sekarang - lama[i] < jendelaMs) {
							if (hidup < baru.length) {
								baru[hidup] = lama[i];
							}
							hidup++;
						}
					}
				}
				if (hidup >= maksimal) {
					JEJAK.put(kunci, baru);
					return false;
				}
				baru[hidup] = sekarang;
				JEJAK.put(kunci, baru);
				return true;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PublicRegistrationRateLimiter.izinkan");
			return true;
		}
	}

	/** Intern kunci supaya synchronized bekerja per-kunci, bukan global. */
	private static String kunciIntern(String kunci) {
		return ("PublicRegistrationRateLimiter|" + kunci).intern();
	}

	/** Penjaga memori: bila peta terlalu besar, buang entri yang seluruh jejaknya sudah kedaluwarsa. */
	private static void bersihkanBilaPenuh(long sekarang, long jendelaMs) {
		if (JEJAK.size() < MAKS_ENTRI) {
			return;
		}
		Iterator<Map.Entry<String, long[]>> it = JEJAK.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, long[]> e = it.next();
			long[] v = e.getValue();
			boolean adaHidup = false;
			for (int i = 0; i < v.length; i++) {
				if (v[i] > 0 && sekarang - v[i] < jendelaMs) {
					adaHidup = true;
					break;
				}
			}
			if (!adaHidup) {
				it.remove();
			}
		}
	}
}
