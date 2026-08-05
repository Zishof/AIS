package ais.common;

import org.json.JSONObject;

/**
 * Shim kompatibilitas untuk pustaka org.json versi lama yang ter-deploy di
 * runtime. Build org.json lama (sebelum kira-kira 2010) BELUM memiliki method
 * <code>optLong(String, long)</code> sehingga pemanggilannya melempar
 * <code>java.lang.NoSuchMethodError</code> saat runtime, walaupun kompilasi
 * lokal sukses (classpath kompilasi memakai org.json yang lebih baru).
 *
 * <p>Method <code>optString</code>, <code>optInt</code>, dan <code>optDouble</code>
 * dua-argumen sudah ada sejak versi awal sehingga aman; hanya
 * <code>optLong(String, long)</code> yang perlu dishim.</p>
 *
 * <p>Helper ini membaca nilai memakai <code>opt(String)</code> (tersedia sejak
 * versi paling awal) lalu mengonversinya ke long secara defensif, sehingga aman
 * untuk semua versi org.json. Gaya Java 1.6/1.7: tanpa lambda, diamond, atau
 * Stream API.</p>
 */
public final class JsonCompatUtil {

	private JsonCompatUtil() {
	}

	/**
	 * Pengganti aman untuk <code>JSONObject.optLong(String, long)</code>.
	 *
	 * @param obj          objek JSON sumber (boleh null)
	 * @param key          nama properti
	 * @param defaultValue nilai default bila properti tidak ada / tidak valid
	 * @return nilai long properti, atau defaultValue bila gagal
	 */
	public static long optLong(JSONObject obj, String key, long defaultValue) {
		if (obj == null || key == null) {
			return defaultValue;
		}
		Object value;
		try {
			value = obj.opt(key);
		} catch (Exception e) {
			return defaultValue;
		}
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		String text = String.valueOf(value).trim();
		if (text.length() == 0) {
			return defaultValue;
		}
		try {
			return Long.parseLong(text);
		} catch (NumberFormatException e1) {
			try {
				return (long) Double.parseDouble(text);
			} catch (NumberFormatException e2) {
				return defaultValue;
			}
		}
	}
}
