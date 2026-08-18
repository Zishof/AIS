package ais.common;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import org.json.JSONObject;

/**
 * <h2>Gerbang Terjemah AI (Ollama) dengan fallback OTOMATIS ke kamus internal</h2>
 *
 * <p>Perilaku: bila konfigurasi {@code ollama_aktif} ON <b>dan</b> server Ollama dapat dihubungi
 * (health-check ber-timeout cepat, hasil di-cache singkat), terjemahan memakai Ollama (kualitas natural).
 * Bila Ollama <b>mati / tak dapat dihubungi / timeout / error</b>, ATAU sedang sibuk (batas paralel penuh),
 * langsung jatuh ke {@link KamusBahasaInternal} — tanpa memblokir & tanpa melempar exception.</p>
 *
 * <p><b>Konfigurasi</b> (tabel Konfigurasi, dibuat otomatis dgn nilai default bila belum ada):</p>
 * <ul>
 *   <li>{@code ollama_aktif} = {@code true} (otomatis ON; matikan dgn {@code false})</li>
 *   <li>{@code ollama_url} = {@code http://38.47.182.162:11434}</li>
 *   <li>{@code ollama_model} = {@code ecampus-translator} (model KHUSUS terjemahan, system prompt tertanam)</li>
 *   <li>{@code ollama_timeout_konek_ms} = {@code 10000} (waktu buka koneksi)</li>
 *   <li>{@code ollama_timeout_baca_ms} = {@code 120000} (batas tunggu hasil; selesai lebih cepat bila respons diterima)</li>
 *   <li>{@code ollama_num_predict} = {@code 0} (0 = OTOMATIS skala panjang teks; isi &gt;0 utk nilai tetap, mis. 64)</li>
 *   <li>{@code ollama_num_predict_maks} = {@code 512} (batas atas saat mode otomatis)</li>
 *   <li>{@code ollama_health_timeout_ms} = {@code 1000} (timeout PENDEK cek health, deteksi mati cepat)</li>
 *   <li>{@code ollama_paralel} = {@code 3} (maks permintaan Ollama serentak; sisanya fallback internal)</li>
 *   <li>{@code ollama_health_cache_dtk} = {@code 30} (jeda cek health, agar tak cek tiap panggilan)</li>
 * </ul>
 *
 * <p>Hasil Ollama di-cache di memori (key = bahasa+teks) sehingga teks sama tidak dipanggil ulang.</p>
 */
public class AiTerjemah {

	private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<String, String>();
	private static final int CACHE_MAKS = 60000;

	private static volatile long lastCekMs = 0L;
	private static volatile boolean lastSiap = false;
	private static final Object CEK_LOCK = new Object();

	private static volatile Semaphore semaphore = null;
	private static volatile int semaphorePermits = -1;

	private AiTerjemah() {
	}

	private static String konfig(String key, String def) {
		try {
			String v = Common.getKonfigurasi(key, def).getNilai();
			return v == null ? def : v.trim();
		} catch (Exception e) {
			return def;
		}
	}

	private static int konfigInt(String key, int def) {
		try {
			return Integer.parseInt(konfig(key, String.valueOf(def)));
		} catch (Exception e) {
			return def;
		}
	}

	public static boolean aktif() {
		String v = konfig("ollama_aktif", "true");
		return "true".equalsIgnoreCase(v) || "1".equals(v) || "on".equalsIgnoreCase(v) || "ya".equalsIgnoreCase(v);
	}

	/** Apakah Ollama aktif &amp; dapat dihubungi (hasil cek di-cache {@code ollama_health_cache_dtk} detik). */
	public static boolean siap() {
		if (!aktif()) {
			return false;
		}
		long now = System.currentTimeMillis();
		long jeda = konfigInt("ollama_health_cache_dtk", 30) * 1000L;
		if (now - lastCekMs < jeda) {
			return lastSiap;
		}
		synchronized (CEK_LOCK) {
			now = System.currentTimeMillis();
			if (now - lastCekMs < jeda) {
				return lastSiap;
			}
			lastSiap = cekHealth();
			lastCekMs = System.currentTimeMillis();
			return lastSiap;
		}
	}

	private static boolean cekHealth() {
		HttpURLConnection c = null;
		try {
			String url = konfig("ollama_url", "http://38.47.182.162:11434");
			// Health-check pakai timeout PENDEK tersendiri (deteksi mati cepat), TERPISAH dari timeout generate.
			int tk = konfigInt("ollama_health_timeout_ms", 1000);
			URL u = new URL(url + "/api/version");
			c = (HttpURLConnection) u.openConnection();
			c.setConnectTimeout(tk);
			c.setReadTimeout(tk);
			c.setRequestMethod("GET");
			int code = c.getResponseCode();
			return code >= 200 && code < 300;
		} catch (Exception e) {
			return false;
		} finally {
			if (c != null) {
				try {
					c.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AiTerjemah.java:115");
				}
			}
		}
	}

	private static Semaphore semaphore() {
		int permits = konfigInt("ollama_paralel", 3);
		if (permits < 1) {
			permits = 1;
		}
		if (semaphore == null || semaphorePermits != permits) {
			synchronized (CEK_LOCK) {
				if (semaphore == null || semaphorePermits != permits) {
					semaphore = new Semaphore(permits);
					semaphorePermits = permits;
				}
			}
		}
		return semaphore;
	}

	/**
	 * Terjemahkan {@code teksIndonesia} ke {@code targetLang} ("english"/"arab"/"mandarin" atau kode ZK setara).
	 * Ollama bila siap &amp; slot tersedia; selain itu kamus internal. TIDAK PERNAH melempar exception.
	 */
	public static String terjemah(String teksIndonesia, String targetLang) {
		if (teksIndonesia == null || teksIndonesia.trim().length() == 0) {
			return teksIndonesia == null ? "" : teksIndonesia;
		}
		if (!siap()) {
			return KamusBahasaInternal.terjemah(teksIndonesia, targetLang);
		}
		String kode = normalisasiLang(targetLang);
		String cacheKey = kode + "" + teksIndonesia;
		String cached = CACHE.get(cacheKey);
		if (cached != null) {
			return cached;
		}
		Semaphore s = semaphore();
		boolean acquired = false;
		try {
			acquired = s.tryAcquire(); // segera: bila slot penuh → fallback internal (tanpa menunggu)
			if (!acquired) {
				return KamusBahasaInternal.terjemah(teksIndonesia, targetLang);
			}
			String hasil = panggilOllama(teksIndonesia, kode);
			if (hasil == null || hasil.trim().length() == 0) {
				return KamusBahasaInternal.terjemah(teksIndonesia, targetLang);
			}
			hasil = bersihkan(hasil);
			if (CACHE.size() < CACHE_MAKS) {
				CACHE.put(cacheKey, hasil);
			}
			return hasil;
		} catch (Exception e) {
			return KamusBahasaInternal.terjemah(teksIndonesia, targetLang);
		} finally {
			if (acquired) {
				s.release();
			}
		}
	}

	private static String normalisasiLang(String tl) {
		String t = tl == null ? "" : tl.toLowerCase();
		if (t.startsWith("ar") || t.contains("arab")) {
			return "arab";
		}
		if (t.startsWith("zh") || t.contains("mandarin") || t.contains("china") || t.contains("chinese")) {
			return "mandarin";
		}
		return "english";
	}

	private static String namaBahasa(String kode) {
		if ("arab".equals(kode)) {
			return "Arabic";
		}
		if ("mandarin".equals(kode)) {
			return "Simplified Chinese";
		}
		return "English";
	}

	private static String panggilOllama(String teks, String kode) {
		HttpURLConnection c = null;
		try {
			String url = konfig("ollama_url", "http://38.47.182.162:11434");
			// Model KHUSUS TERJEMAHAN (system prompt sudah tertanam di model → prompt cukup pendek).
			String model = konfig("ollama_model", "ecampus-translator");
			int tk = konfigInt("ollama_timeout_konek_ms", 10000);
			int tb = konfigInt("ollama_timeout_baca_ms", 120000);

			// Prompt PENDEK (instruksi lengkap sudah di dalam model ecampus-translator) → hemat token + prompt cache.
			String prompt = "Target language: " + namaBahasa(kode) + "\nText: " + teks;

			JSONObject body = new JSONObject();
			body.put("model", model);
			body.put("prompt", prompt);
			body.put("stream", false);
			body.put("keep_alive", "24h");
			JSONObject opt = new JSONObject();
			opt.put("temperature", 0.1);
			// num_predict: bila konfig ollama_num_predict > 0 → nilai TETAP; jika 0/absen → OTOMATIS
			// menyesuaikan panjang teks (label pendek = cap kecil → cepat; teks panjang = cukup ruang,
			// dibatasi ollama_num_predict_maks). Mencegah keluaran kepanjangan tanpa memotong teks panjang.
			int npCfg = konfigInt("ollama_num_predict", 0);
			int np;
			if (npCfg > 0) {
				np = npCfg;
			} else {
				int cap = konfigInt("ollama_num_predict_maks", 512);
				np = teks.length() + 16;
				if (np < 48) {
					np = 48;
				}
				if (np > cap) {
					np = cap;
				}
			}
			opt.put("num_predict", np);
			body.put("options", opt);

			URL u = new URL(url + "/api/generate");
			c = (HttpURLConnection) u.openConnection();
			c.setConnectTimeout(tk);
			c.setReadTimeout(tb);
			c.setRequestMethod("POST");
			c.setDoOutput(true);
			c.setUseCaches(false);
			c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			c.setRequestProperty("Accept", "application/json");
			byte[] out = body.toString().getBytes("UTF-8");
			OutputStream os = c.getOutputStream();
			os.write(out);
			os.flush();
			os.close();

			int code = c.getResponseCode();
			// Pada gagal (non-2xx): baca errorStream utk log diagnosa, lalu fallback ke kamus internal (return null).
			InputStream in = (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream();
			String isi = bacaSemua(in);
			if (code < 200 || code >= 300) {
				try {
					System.out.println("AiTerjemah: Ollama HTTP " + code + " -> " + isi);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AiTerjemah.java:261");
				}
				return null;
			}
			JSONObject resp = new JSONObject(isi);
			return resp.has("response") ? resp.getString("response") : null;
		} catch (Exception e) {
			return null;
		} finally {
			if (c != null) {
				try {
					c.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AiTerjemah.java:273");
				}
			}
		}
	}

	private static String bacaSemua(InputStream in) throws Exception {
		if (in == null) {
			return "";
		}
		BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		try {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		} finally {
			try {
				r.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AiTerjemah.java:294");
			}
		}
	}

	/**
	 * Terjemahkan HANYA via Ollama (TANPA fallback kamus internal). Mengembalikan hasil terjemahan atau
	 * {@code null} bila Ollama mati/gagal/kosong. Berbeda dari {@link #terjemah}: MENUNGGU slot (hingga 2
	 * menit) alih-alih langsung fallback — dipakai untuk mode "Terjemahkan Ulang HANYA via AI".
	 */
	public static String ollamaSaja(String teksIndonesia, String targetLang) {
		if (teksIndonesia == null || teksIndonesia.trim().length() == 0) {
			return null;
		}
		if (!siap()) {
			return null;
		}
		String kode = normalisasiLang(targetLang);
		String cacheKey = kode + "" + teksIndonesia;
		String cached = CACHE.get(cacheKey);
		if (cached != null) {
			return cached;
		}
		Semaphore s = semaphore();
		boolean acquired = false;
		try {
			acquired = s.tryAcquire(2, java.util.concurrent.TimeUnit.MINUTES);
			if (!acquired) {
				return null;
			}
			String hasil = panggilOllama(teksIndonesia.trim(), kode);
			if (hasil == null || hasil.trim().length() == 0) {
				return null;
			}
			hasil = bersihkan(hasil);
			if (CACHE.size() < CACHE_MAKS) {
				CACHE.put(cacheKey, hasil);
			}
			return hasil;
		} catch (Exception e) {
			return null;
		} finally {
			if (acquired) {
				s.release();
			}
		}
	}

	/**
	 * Menilai apakah {@code teks} merupakan label/istilah UI yang BERMAKNA atau JUNK (nama orang, email,
	 * karakter acak, data uji, kode/id) — memakai Ollama (model UMUM {@code ai_model_generate}).
	 * Mengembalikan {@code TRUE} (bermakna), {@code FALSE} (junk), atau {@code null} bila TIDAK DAPAT dinilai
	 * (Ollama mati/sibuk/timeout/gagal). Pemanggil harus memperlakukan {@code null} sebagai "JANGAN hapus".
	 */
	public static Boolean bermakna(String teks) {
		if (teks == null || teks.trim().length() == 0) {
			return Boolean.FALSE;
		}
		if (!siap()) {
			return null;
		}
		Semaphore s = semaphore();
		boolean acquired = false;
		try {
			acquired = s.tryAcquire();
			if (!acquired) {
				return null;
			}
			String hasil = panggilOllamaBermakna(teks.trim());
			if (hasil == null) {
				return null;
			}
			String low = hasil.toLowerCase();
			if (low.contains("junk") || low.contains("sampah")) {
				return Boolean.FALSE;
			}
			if (low.contains("meaningful") || low.contains("bermakna")) {
				return Boolean.TRUE;
			}
			return null;
		} catch (Exception e) {
			return null;
		} finally {
			if (acquired) {
				s.release();
			}
		}
	}

	private static String panggilOllamaBermakna(String teks) {
		HttpURLConnection c = null;
		try {
			String url = konfig("ollama_url", "http://38.47.182.162:11434");
			// Penilaian butuh MODEL UMUM (bukan ecampus-translator yg khusus terjemah).
			// Kunci BARU 'ai_model_generate' (default RINGAN 1.5b).
			String model = konfig("ai_model_generate", "qwen2.5:1.5b-instruct-q4_K_M");
			int tk = konfigInt("ollama_timeout_konek_ms", 10000);
			int tb = konfigInt("ollama_timeout_baca_ms", 120000);

			String prompt = "You are cleaning a user-interface translation table for an academic information "
					+ "system. Decide if the TEXT below is a MEANINGFUL UI label/term/message/field/academic word "
					+ "or phrase, OR JUNK to be deleted. JUNK = a person's name, an email address, a phone number, "
					+ "a pure number, a date, a code/ID/reference (e.g. 'dos-086', 'MK4309'), random characters, "
					+ "repeated letters, single letters, gibberish, or test data. "
					+ "Reply with ONLY one word: MEANINGFUL or JUNK.\n\nTEXT: " + teks;

			JSONObject body = new JSONObject();
			body.put("model", model);
			body.put("prompt", prompt);
			body.put("stream", false);
			body.put("keep_alive", "24h");
			JSONObject opt = new JSONObject();
			opt.put("temperature", 0);
			opt.put("num_predict", 5);
			body.put("options", opt);

			URL u = new URL(url + "/api/generate");
			c = (HttpURLConnection) u.openConnection();
			c.setConnectTimeout(tk);
			c.setReadTimeout(tb);
			c.setRequestMethod("POST");
			c.setDoOutput(true);
			c.setUseCaches(false);
			c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			c.setRequestProperty("Accept", "application/json");
			byte[] out = body.toString().getBytes("UTF-8");
			OutputStream os = c.getOutputStream();
			os.write(out);
			os.flush();
			os.close();

			int code = c.getResponseCode();
			InputStream in = (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream();
			String isi = bacaSemua(in);
			if (code < 200 || code >= 300) {
				return null;
			}
			JSONObject resp = new JSONObject(isi);
			return resp.has("response") ? resp.getString("response") : null;
		} catch (Exception e) {
			return null;
		} finally {
			if (c != null) {
				try {
					c.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AiTerjemah.java:438");
				}
			}
		}
	}

	/** Rapikan keluaran model: buang kutip pembungkus &amp; prefix "Translation:" bila ada. */
	private static String bersihkan(String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		String low = t.toLowerCase();
		if (low.startsWith("translation:")) {
			t = t.substring("translation:".length()).trim();
		} else if (low.startsWith("terjemahan:")) {
			t = t.substring("terjemahan:".length()).trim();
		}
		if (t.length() >= 2) {
			char a = t.charAt(0);
			char b = t.charAt(t.length() - 1);
			if ((a == '"' && b == '"') || (a == '\'' && b == '\'') || (a == '“' && b == '”')) {
				t = t.substring(1, t.length() - 1).trim();
			}
		}
		return t;
	}
}
