package ais.common;

import java.util.Map;

import ais.database.model.Konfigurasi;
import ais.database.model.sekolah.Tagihan;

/**
 * Lapisan cache lokal (per-JVM, statis) di atas {@link MemoryCacheUtil} yang menyimpan referensi
 * ke sejumlah struktur data yang sering diakses berulang kali di seluruh aplikasi AIS —
 * konfigurasi sistem, kamus terjemahan multi-bahasa, data kelas/objek yang sudah pernah dimuat,
 * peta tagihan siswa, serta status "sudah dihitung" per tagihan. Tujuan kelas ini adalah
 * mengurangi biaya pemanggilan {@link MemoryCacheUtil#get(String)} yang berulang untuk kunci yang
 * sama, dengan menyimpan hasil pemanggilan pertama pada field statis lokal kelas ini
 * (lazy-loaded, diisi hanya sekali per siklus hidup nilai cache).
 *
 * <p>
 * <b>Pola dua lapis cache</b> — setiap method {@code getXxx()} pada kelas ini mengikuti pola yang
 * identik: bila field statis lokal masih {@code null}, ambil nilainya dari
 * {@link MemoryCacheUtil} (yang kemungkinan merupakan cache tersendiri dengan mekanisme
 * invalidasi/refresh sendiri) dan simpan pada field lokal; panggilan berikutnya langsung
 * mengembalikan nilai yang sudah tersimpan di field lokal tanpa memanggil
 * {@link MemoryCacheUtil} lagi. Konsekuensinya, nilai pada kelas ini dapat menjadi <b>basi
 * (stale)</b> relatif terhadap {@link MemoryCacheUtil} setelah pengisian pertama, sampai
 * {@link #resetLocalReferences()} dipanggil untuk mengosongkan seluruh field lokal dan memaksa
 * pengisian ulang dari {@link MemoryCacheUtil} pada akses berikutnya. Pemanggilan
 * {@link #resetLocalReferences()} biasanya dipicu dari titik-titik di aplikasi yang mengubah data
 * sumber (mis. perubahan konfigurasi sistem atau kamus bahasa) agar perubahan tersebut segera
 * terlihat oleh pemanggil kelas ini.
 * </p>
 *
 * <p>
 * Seluruh method bersifat statis; method {@link #resetLocalReferences()} disinkronkan
 * ({@code synchronized}) untuk mencegah kondisi balapan (race condition) saat beberapa thread
 * mereset field lokal secara bersamaan, sementara method {@code getXxx()} lain TIDAK
 * disinkronkan — dalam skenario akses bersamaan yang jarang, hal ini secara teoretis dapat
 * menyebabkan pemanggilan {@link MemoryCacheUtil#get(String)} lebih dari sekali sebelum field
 * lokal terisi, namun tidak menyebabkan kerusakan data karena nilai yang diambil dari
 * {@link MemoryCacheUtil} untuk kunci yang sama seharusnya konsisten.
 * </p>
 */
public class MemoryDbUtil {

	/** Cache lokal peta konfigurasi sistem (kunci konfigurasi → objek {@link Konfigurasi}). */
	private static Map<String, Konfigurasi> mapkonfigurasi = null;

	/**
	 * Mengosongkan seluruh field cache lokal statis pada kelas ini (konfigurasi, kamus bahasa,
	 * data kelas, peta tagihan, dan penanda tanpa-spasi), memaksa setiap method {@code getXxx()}/
	 * {@code apakahTanpaSpasi()} berikutnya untuk mengambil ulang nilainya dari
	 * {@link MemoryCacheUtil} (atau, untuk {@link #apakahTanpaSpasi()}, dari konfigurasi
	 * {@code matakuliah_tanpa_spasi}). Dipanggil setiap kali data sumber yang mendasari cache ini
	 * berubah dan perubahan tersebut perlu segera terlihat, tanpa harus menunggu restart aplikasi.
	 */
	public static synchronized void resetLocalReferences() {
		mapkonfigurasi = null;
		udahDataClass = null;
		bahasaIndonesias = null;
		bahasaEnglishs = null;
		bahasaArabs = null;
		bahasaMandarins = null;
		dataKey = null;
		allTagihan = null;
		allTagihanSudah = null;
		tanpaSpasi = null;
	}

	/**
	 * Mengembalikan peta konfigurasi sistem (kunci nama konfigurasi → {@link Konfigurasi}),
	 * diambil sekali dari {@link MemoryCacheUtil} lalu di-cache secara lokal.
	 *
	 * @return peta konfigurasi sistem, atau {@code null} bila belum pernah dimuat ke
	 *         {@link MemoryCacheUtil}
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Konfigurasi> getKonfigurasi() {
		if (mapkonfigurasi == null) {
			mapkonfigurasi = (Map<String, Konfigurasi>) MemoryCacheUtil.get("mapkonfigurasi");
		}
		return mapkonfigurasi;
	}

	/** Cache lokal peta data kelas yang sudah pernah dimuat (kunci → nama kelas/status). */
	private static Map<String, String> udahDataClass = null;
	/** Cache lokal kamus terjemahan Bahasa Indonesia (kunci istilah → terjemahan). */
	private static Map<String, String> bahasaIndonesias = null;
	/** Cache lokal kamus terjemahan Bahasa Inggris (kunci istilah → terjemahan). */
	private static Map<String, String> bahasaEnglishs = null;
	/** Cache lokal kamus terjemahan Bahasa Arab (kunci istilah → terjemahan). */
	private static Map<String, String> bahasaArabs = null;
	/** Cache lokal kamus terjemahan Bahasa Mandarin (kunci istilah → terjemahan). */
	private static Map<String, String> bahasaMandarins = null;
	/** Cache lokal peta data sementara serba-guna (kunci → nilai string). */
	private static Map<String, String> dataKey = null;

	/** Cache lokal peta seluruh tagihan (kunci → {@link Tagihan}). */
	private static Map<String, Tagihan> allTagihan = null;
	/** Cache lokal peta status "sudah dihitung/diproses" per id tagihan. */
	private static Map<Long, Boolean> allTagihanSudah = null;

	/**
	 * Mengembalikan peta data kelas yang sudah pernah dimuat sebelumnya, diambil sekali dari
	 * {@link MemoryCacheUtil} lalu di-cache secara lokal.
	 *
	 * @return peta data kelas, atau {@code null} bila belum pernah dimuat ke {@link MemoryCacheUtil}
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getDataClass() {
		if (udahDataClass == null) {
			udahDataClass = (Map<String, String>) MemoryCacheUtil.get("udahDataClass");
		}
		return udahDataClass;
	}

	/**
	 * Mengembalikan peta seluruh tagihan (kunci → {@link Tagihan}), diambil sekali dari
	 * {@link MemoryCacheUtil} lalu di-cache secara lokal.
	 *
	 * @return peta seluruh tagihan, atau {@code null} bila belum pernah dimuat ke
	 *         {@link MemoryCacheUtil}
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Tagihan> getAllTagihan() {
		if (allTagihan == null) {
			allTagihan = (Map<String, Tagihan>) MemoryCacheUtil.get("allTagihan");
		}
		return allTagihan;
	}

	/**
	 * Mengembalikan peta status "sudah diproses/dihitung" per id tagihan, diambil sekali dari
	 * {@link MemoryCacheUtil} lalu di-cache secara lokal.
	 *
	 * @return peta status per id tagihan, atau {@code null} bila belum pernah dimuat ke
	 *         {@link MemoryCacheUtil}
	 */
	@SuppressWarnings("unchecked")
	public static Map<Long, Boolean> getAllTagihanSudah() {
		if (allTagihanSudah == null) {
			allTagihanSudah = (Map<Long, Boolean>) MemoryCacheUtil.get("allTagihanSudah");
		}
		return allTagihanSudah;
	}

	/**
	 * Mengembalikan kamus terjemahan Bahasa Indonesia, diambil sekali dari
	 * {@link MemoryCacheUtil} lalu di-cache secara lokal.
	 *
	 * @return kamus terjemahan Bahasa Indonesia, atau {@code null} bila belum pernah dimuat
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getBahasaIndonesias() {
		if (bahasaIndonesias == null) {
			bahasaIndonesias = (Map<String, String>) MemoryCacheUtil.get("bahasaIndonesias");
		}
		return bahasaIndonesias;
	}

	/**
	 * Mengembalikan kamus terjemahan Bahasa Inggris, diambil sekali dari {@link MemoryCacheUtil}
	 * lalu di-cache secara lokal.
	 *
	 * @return kamus terjemahan Bahasa Inggris, atau {@code null} bila belum pernah dimuat
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getBahasaEnglishs() {
		if (bahasaEnglishs == null) {
			bahasaEnglishs = (Map<String, String>) MemoryCacheUtil.get("bahasaEnglishs");
		}
		return bahasaEnglishs;
	}

	/**
	 * Mengembalikan kamus terjemahan Bahasa Arab, diambil sekali dari {@link MemoryCacheUtil}
	 * lalu di-cache secara lokal.
	 *
	 * @return kamus terjemahan Bahasa Arab, atau {@code null} bila belum pernah dimuat
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getBahasaArabs() {
		if (bahasaArabs == null) {
			bahasaArabs = (Map<String, String>) MemoryCacheUtil.get("bahasaArabs");
		}
		return bahasaArabs;
	}

	/**
	 * Mengembalikan kamus terjemahan Bahasa Mandarin, diambil sekali dari {@link MemoryCacheUtil}
	 * lalu di-cache secara lokal.
	 *
	 * @return kamus terjemahan Bahasa Mandarin, atau {@code null} bila belum pernah dimuat
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getBahasaMandarins() {
		if (bahasaMandarins == null) {
			bahasaMandarins = (Map<String, String>) MemoryCacheUtil.get("bahasaMandarins");
		}
		return bahasaMandarins;
	}

	/**
	 * Mengembalikan peta data sementara serba-guna (disimpan di {@link MemoryCacheUtil} dengan
	 * kunci {@code "data_temp"}), diambil sekali lalu di-cache secara lokal.
	 *
	 * @return peta data sementara, atau {@code null} bila belum pernah dimuat
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getDataKey() {
		if (dataKey == null) {
			dataKey = (Map<String, String>) MemoryCacheUtil.get("data_temp");
		}
		return dataKey;
	}

	/** Cache lokal hasil evaluasi konfigurasi {@code matakuliah_tanpa_spasi}. */
	private static Boolean tanpaSpasi = null;

	/**
	 * Menentukan apakah kode mata kuliah pada institusi ini disusun tanpa spasi, berdasarkan
	 * konfigurasi {@code matakuliah_tanpa_spasi}. Hasil evaluasi pertama di-cache secara lokal
	 * (field {@link #tanpaSpasi}) sehingga pemanggilan berikutnya tidak perlu mengevaluasi ulang
	 * konfigurasi lewat {@link Common#bolehKonfigurasi(String)}.
	 *
	 * @return {@code true} bila konfigurasi {@code matakuliah_tanpa_spasi} aktif (kode mata
	 *         kuliah tanpa spasi); {@code false} bila tidak aktif
	 */
	public static boolean apakahTanpaSpasi() {
		if (tanpaSpasi == null) {
			tanpaSpasi = Common.bolehKonfigurasi("matakuliah_tanpa_spasi");
		}
		return tanpaSpasi;
	}
}