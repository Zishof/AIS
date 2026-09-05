package ais.common;

import java.text.Normalizer;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Kontrak hak akses katalog laporan eBisnis per grup pengguna.
 *
 * <p>Konfigurasi disimpan di {@code Tbmrole.ebisnisMenu.aksesLaporan} agar tidak
 * menambah kolom/tabel baru. Bentuknya:</p>
 *
 * <pre>
 * {
 *   "semua": false,
 *   "kategori": { "penjualan": true },
 *   "laporan": { "penjualan/pnj_faktur": true }
 * }
 * </pre>
 *
 * <p>Konfigurasi yang belum ada selalu dianggap {@code semua=true}. Ini penting
 * untuk kompatibilitas role lama. Begitu mode pembatasan dipilih ({@code semua=false}),
 * kategori dan laporan yang tidak tercantum ditolak (fail-closed), termasuk laporan
 * baru yang kelak ditambahkan ke katalog.</p>
 */
public final class EbisnisLaporanAkses {

	public static final String KUNCI = "aksesLaporan";
	public static final String KUNCI_SEMUA = "semua";
	public static final String KUNCI_KATEGORI = "kategori";
	public static final String KUNCI_LAPORAN = "laporan";

	private EbisnisLaporanAkses() {
	}

	/** Konfigurasi kompatibel utk role lama: seluruh katalog boleh dilihat. */
	public static JSONObject defaultKonfigurasi() {
		JSONObject obj = new JSONObject();
		try {
			obj.put(KUNCI_SEMUA, true);
			obj.put(KUNCI_KATEGORI, new JSONObject());
			obj.put(KUNCI_LAPORAN, new JSONObject());
		} catch (JSONException ex) {
			ErrorAuditUtil.record(ex, "EbisnisLaporanAkses.defaultKonfigurasi");
		}
		return obj;
	}

	/**
	 * Normalisasi konfigurasi tersimpan tanpa kehilangan pilihan kategori/laporan.
	 * Nilai rusak kembali ke default kompatibel, bukan memblokir semua role lama.
	 */
	public static JSONObject normalisasi(JSONObject tersimpan) {
		JSONObject hasil = defaultKonfigurasi();
		if (tersimpan == null) {
			return hasil;
		}
		try {
			hasil.put(KUNCI_SEMUA, tersimpan.optBoolean(KUNCI_SEMUA, true));
			JSONObject kategori = tersimpan.optJSONObject(KUNCI_KATEGORI);
			JSONObject laporan = tersimpan.optJSONObject(KUNCI_LAPORAN);
			hasil.put(KUNCI_KATEGORI,
					kategori == null ? new JSONObject() : new JSONObject(kategori.toString()));
			hasil.put(KUNCI_LAPORAN,
					laporan == null ? new JSONObject() : new JSONObject(laporan.toString()));
		} catch (Exception ex) {
			ErrorAuditUtil.record(ex, "EbisnisLaporanAkses.normalisasi: konfigurasi rusak");
			return defaultKonfigurasi();
		}
		return hasil;
	}

	/** Ambil konfigurasi dari hasil {@link EbisnisMenuKatalog#urai(String)}. */
	public static JSONObject dariMenuRole(JSONObject menuRole) {
		return normalisasi(menuRole == null ? null : menuRole.optJSONObject(KUNCI));
	}

	/** ID kategori stabil dan tidak bergantung bahasa antarmuka. */
	public static String idKategori(String namaMentah) {
		String nilai = namaMentah == null ? "" : namaMentah.trim().toLowerCase(Locale.US);
		try {
			nilai = Normalizer.normalize(nilai, Normalizer.Form.NFD)
					.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
		} catch (Exception abaikan) {
			// Nama kategori saat ini ASCII; normalisasi adalah pengaman utk penambahan nanti.
		}
		nilai = nilai.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
		return nilai.length() == 0 ? "kategori" : nilai;
	}

	/** Kunci laporan memakai kategori + id karena satu id dapat tampil di dua kategori. */
	public static String kunciLaporan(String kategoriId, String laporanId) {
		return (kategoriId == null ? "" : kategoriId.trim()) + "/"
				+ (laporanId == null ? "" : laporanId.trim());
	}

	public static boolean semua(JSONObject menuRole) {
		if (menuRole != null && menuRole.optBoolean("supervisor", false)) {
			return true;
		}
		return dariMenuRole(menuRole).optBoolean(KUNCI_SEMUA, true);
	}

	/** Cek pasangan kategori/laporan pada mode pembatasan. */
	public static boolean boleh(JSONObject menuRole, String kategoriId, String laporanId) {
		if (semua(menuRole)) {
			return true;
		}
		JSONObject cfg = dariMenuRole(menuRole);
		JSONObject kategori = cfg.optJSONObject(KUNCI_KATEGORI);
		JSONObject laporan = cfg.optJSONObject(KUNCI_LAPORAN);
		return kategori != null && kategori.optBoolean(kategoriId, false)
				&& laporan != null && laporan.optBoolean(kunciLaporan(kategoriId, laporanId), false);
	}

	/**
	 * Saring katalog sebelum dikirim ke klien. Kategori tanpa satu pun laporan yang
	 * diizinkan ikut dibuang sehingga dropdown tidak menampilkan pilihan kosong.
	 */
	public static JSONArray saringKatalog(JSONArray katalog, JSONObject menuRole) throws JSONException {
		if (katalog == null) {
			return new JSONArray();
		}
		if (semua(menuRole)) {
			return new JSONArray(katalog.toString());
		}
		JSONArray hasil = new JSONArray();
		for (int i = 0; i < katalog.length(); i++) {
			JSONObject kat = katalog.getJSONObject(i);
			String kategoriId = kat.optString("katId", idKategori(kat.optString("kat", "")));
			JSONArray items = kat.optJSONArray("items");
			JSONArray diizinkan = new JSONArray();
			for (int j = 0; items != null && j < items.length(); j++) {
				JSONObject item = items.getJSONObject(j);
				if (boleh(menuRole, kategoriId, item.optString("id", ""))) {
					diizinkan.put(new JSONObject(item.toString()));
				}
			}
			if (diizinkan.length() > 0) {
				JSONObject salinan = new JSONObject(kat.toString());
				salinan.put("katId", kategoriId);
				salinan.put("items", diizinkan);
				hasil.put(salinan);
			}
		}
		return hasil;
	}

	/**
	 * Otorisasi endpoint laporan berdasarkan id laporan. Jika id yang sama muncul di
	 * lebih dari satu kategori, akses sah bila minimal satu pasangan kategori/id diizinkan.
	 */
	public static boolean bolehMenjalankan(JSONArray katalog, JSONObject menuRole, String laporanId)
			throws JSONException {
		if (semua(menuRole)) {
			return true;
		}
		String id = laporanId == null ? "" : laporanId.trim();
		if (id.length() == 0 || katalog == null) {
			return false;
		}
		for (int i = 0; i < katalog.length(); i++) {
			JSONObject kat = katalog.getJSONObject(i);
			String kategoriId = kat.optString("katId", idKategori(kat.optString("kat", "")));
			JSONArray items = kat.optJSONArray("items");
			for (int j = 0; items != null && j < items.length(); j++) {
				JSONObject item = items.getJSONObject(j);
				if (id.equals(item.optString("id", "")) && boleh(menuRole, kategoriId, id)) {
					return true;
				}
			}
		}
		return false;
	}
}
