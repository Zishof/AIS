package ais.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * <h2>Seed default terjemahan 3 bahasa (Indonesia / English / Arab)</h2>
 *
 * <p><b>Tujuan.</b> Menyediakan <b>nilai default</b> terjemahan yang ditaruh di <b>file konfigurasi</b>
 * ({@code WEB-INF/DEFAULT_BAHASA_INDONESIA.conf}, {@code DEFAULT_ENGLISH.conf}, {@code DEFAULT_ARABIC.conf}).
 * Ketika sebuah pesan/label muncul dan kuncinya BELUM ada di tabel {@code LabelBahasa}, baris baru yang
 * di-INSERT langsung memuat KETIGA bahasa dari seed ini (bukan sekadar menyalin teks Indonesia). Setelah
 * masuk DB, <b>DB menjadi acuan</b> (bila diperbarui, perubahan disimpan ke DB); file seed hanya berlaku
 * sebagai NILAI DEFAULT saat pertama kali sebuah kunci di-insert.</p>
 *
 * <p><b>Format file</b> (UTF-8, seperti {@code .properties}): {@code <kunci>=<terjemahan>} per baris. Kunci
 * memakai format kunci bahasa ternormalisasi (huruf kecil, spasi → {@code _}) &mdash; sama dengan kolom
 * {@code nama} pada {@code LabelBahasa}. Baris diawali {@code #} adalah komentar.</p>
 *
 * <p><b>Lokasi.</b> {@code Common.REAL_PATH + "WEB-INF/" + <nama file>}. Dimuat malas (lazy) sekali,
 * cache di memori; panggil {@link #reload()} untuk memuat ulang tanpa restart. Aman bila file tidak ada
 * (mengembalikan peta kosong) &amp; tahan-exception (tak pernah melempar).</p>
 */
public class DefaultBahasaSeed {

	private static volatile Map<String, String> indonesia;
	private static volatile Map<String, String> english;
	private static volatile Map<String, String> arab;
	private static volatile boolean loaded = false;
	private static final Object LOCK = new Object();

	private DefaultBahasaSeed() {
	}

	private static void loadIfNeeded() {
		if (loaded) {
			return;
		}
		synchronized (LOCK) {
			if (loaded) {
				return;
			}
			indonesia = loadFile("DEFAULT_BAHASA_INDONESIA.conf");
			english = loadFile("DEFAULT_ENGLISH.conf");
			arab = loadFile("DEFAULT_ARABIC.conf");
			loaded = true;
		}
	}

	private static Map<String, String> loadFile(String namaFile) {
		Map<String, String> map = new HashMap<String, String>();
		InputStreamReader reader = null;
		try {
			String base = Common.REAL_PATH == null ? "" : Common.REAL_PATH;
			File file = new File(base + "WEB-INF" + File.separator + namaFile);
			if (!file.exists() || !file.isFile()) {
				return map;
			}
			reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
			Properties properties = new Properties();
			properties.load(reader);
			java.util.Iterator<Object> it = properties.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				if (key == null) {
					continue;
				}
				String nilai = properties.getProperty(key);
				if (nilai != null && nilai.trim().length() > 0) {
					map.put(key.trim().toLowerCase(), nilai);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DefaultBahasaSeed.java:77");
			// abaikan: seed bersifat opsional; bila gagal dibaca, cukup pakai fallback teks Indonesia.
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DefaultBahasaSeed.java:84");
			}
		}
		return map;
	}

	/** Default Bahasa Indonesia untuk kunci; {@code null} bila tak ada. */
	public static String getIndonesia(String key) {
		return ambil(indonesia(), key);
	}

	/** Default Bahasa Inggris untuk kunci; {@code null} bila tak ada. */
	public static String getEnglish(String key) {
		return ambil(english(), key);
	}

	/** Default Bahasa Arab untuk kunci; {@code null} bila tak ada. */
	public static String getArab(String key) {
		return ambil(arab(), key);
	}

	private static Map<String, String> indonesia() {
		loadIfNeeded();
		return indonesia;
	}

	private static Map<String, String> english() {
		loadIfNeeded();
		return english;
	}

	private static Map<String, String> arab() {
		loadIfNeeded();
		return arab;
	}

	private static String ambil(Map<String, String> map, String key) {
		if (map == null || key == null) {
			return null;
		}
		return map.get(key.trim().toLowerCase());
	}

	/** Muat ulang ketiga file seed dari disk (mis. setelah admin mengedit file). */
	public static void reload() {
		synchronized (LOCK) {
			loaded = false;
		}
		loadIfNeeded();
	}

	/**
	 * <h3>Simpan/perbarui terjemahan sebuah kunci ke KETIGA file seed .conf</h3>
	 * Dipakai layar kelola Label Bahasa agar terjemahan yang disunting admin juga tercatat sebagai NILAI
	 * DEFAULT di {@code DEFAULT_BAHASA_INDONESIA.conf}/{@code DEFAULT_ENGLISH.conf}/{@code DEFAULT_ARABIC.conf}.
	 * Komentar &amp; urutan baris file dipertahankan; baris kunci yang sudah ada di-update, jika belum ada
	 * di-append. Aman terhadap exception (seed bersifat opsional).
	 */
	public static void simpan(String key, String indonesia, String english, String arab) {
		if (key == null || key.trim().length() == 0) {
			return;
		}
		String k = key.trim().toLowerCase();
		tulisSatu("DEFAULT_BAHASA_INDONESIA.conf", k, indonesia);
		tulisSatu("DEFAULT_ENGLISH.conf", k, english);
		tulisSatu("DEFAULT_ARABIC.conf", k, arab);
		reload();
	}

	private static void tulisSatu(String namaFile, String key, String nilai) {
		if (nilai == null) {
			nilai = "";
		}
		java.io.BufferedReader reader = null;
		java.io.BufferedWriter writer = null;
		try {
			String base = Common.REAL_PATH == null ? "" : Common.REAL_PATH;
			File file = new File(base + "WEB-INF" + File.separator + namaFile);
			java.util.List<String> lines = new java.util.ArrayList<String>();
			boolean found = false;
			if (file.exists()) {
				reader = new java.io.BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
				String ln;
				while ((ln = reader.readLine()) != null) {
					String trimmed = ln.trim();
					int eq = ln.indexOf('=');
					if (!trimmed.startsWith("#") && trimmed.length() > 0 && eq > 0
							&& ln.substring(0, eq).trim().equalsIgnoreCase(key)) {
						lines.add(key + "=" + nilai);
						found = true;
					} else {
						lines.add(ln);
					}
				}
				reader.close();
				reader = null;
			}
			if (!found) {
				lines.add(key + "=" + nilai);
			}
			writer = new java.io.BufferedWriter(
					new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), "UTF-8"));
			for (int i = 0; i < lines.size(); i++) {
				writer.write(lines.get(i));
				writer.newLine();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DefaultBahasaSeed.java:190");
			// abaikan: penyimpanan seed .conf bersifat opsional.
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DefaultBahasaSeed.java:197");
			}
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DefaultBahasaSeed.java:203");
			}
		}
	}
}
