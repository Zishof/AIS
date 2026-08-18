package ais.action.master.helper.profile;

import ais.common.Common;
import ais.common.DashboardCacheUtil;

/**
 * <h3>ProfileCacheUtil — cache panel data profil per-pengguna</h3>
 *
 * <p><b>Untuk apa:</b> menyimpan hasil render HTML panel-panel data pada halaman profil
 * (ringkasan, biodata, keluarga/kepegawaian, masa studi, dll.) agar tidak dihitung ulang
 * setiap kali profil dibuka. Pembentukan HTML tersebut memanggil banyak getter relasi
 * entitas (kelas, agama, pekerjaan ortu, dll.) yang masing-masing memicu query lazy-load
 * ke database — caching string HTML-nya menghilangkan query berulang itu.</p>
 *
 * <p><b>Common vs per-pengguna.</b> Data yang sama untuk semua pengguna (ringkasan kampus,
 * jumlah mahasiswa/dosen/siswa) sudah dihangatkan saat startup oleh
 * {@link ais.common.RingkasanKampusCache}. Kelas ini menangani sisanya: data yang
 * BERBEDA-BEDA per mahasiswa/siswa/dosen/guru. Karena tiap pengguna punya data sendiri,
 * cache di-key per <i>entityId</i> dan baru terisi saat pengguna membuka profilnya (yang
 * terjadi tepat setelah login melalui dasbor home). Sesudah itu, render berikutnya instan
 * selama TTL aktif.</p>
 *
 * <p><b>Keamanan multi-tenant &amp; antar-pengguna:</b> key memuat {@code panel + role +
 * userId + host} sehingga tidak mungkin bocor antar pengguna atau antar domain. Data yang
 * disimpan hanyalah {@code String} HTML (tanpa referensi entitas) sehingga aman dari
 * {@code LazyInitializationException} saat dibaca kembali di luar sesi aslinya.</p>
 *
 * <p><b>Kesegaran:</b> TTL {@value #TTL_PER_USER_MS} ms (20 menit) — perubahan biodata yang
 * jarang akan tersusul sendiri; bila perlu segera, panggil
 * {@link #invalidateUser(String, String, Long)} dari titik penyimpanan profil.</p>
 *
 * <p><b>Pemeliharaan:</b> kompatibel Java 1.6/1.7 dan ZK 5.5 (tanpa lambda/stream/diamond).</p>
 */
public final class ProfileCacheUtil {

	/** TTL data panel profil per-pengguna (L2 sesi &amp; L3 lintas-sesi): 20 menit. */
	public static final int TTL_PER_USER_MS = 20 * 60 * 1000;

	private ProfileCacheUtil() {
	}

	/** Antarmuka pembuat HTML (Java 1.6-safe; pengganti lambda). */
	public interface Pembuat {
		String buat() throws Exception;
	}

	/**
	 * Kembalikan HTML panel profil milik satu pengguna dari cache; bila belum ada (cache
	 * miss), jalankan {@code pembuat} (yang melakukan query), simpan hasilnya, lalu
	 * kembalikan. Bila {@code userId} null (entitas belum tersimpan) caching dilewati dan
	 * pembuat langsung dijalankan agar perilaku tetap benar.
	 *
	 * @param panel   nama pendek panel, mis. "SiswaRingkasan"
	 * @param role    peran pengguna, mis. "SISWA" / "GURU" / "MAHASISWA" / "DOSEN"
	 * @param userId  id entitas pengguna (siswa.id / guru.id / mahasiswa.id / dosen.id)
	 * @param pembuat penghasil HTML bila cache kosong
	 */
	public static String htmlPerUser(String panel, String role, Long userId, Pembuat pembuat) {
		if (userId == null) {
			return jalankanAman(pembuat);
		}
		String key = bangunKey(panel, role, userId);

		// L2 — cache per-sesi (browser tab) tercepat.
		Object l2 = DashboardCacheUtil.getL2(key);
		if (l2 instanceof String) {
			return (String) l2;
		}
		// L3 — cache lintas-sesi per-pengguna (key memuat userId → terisolasi).
		Object l3 = DashboardCacheUtil.getL3(key);
		if (l3 instanceof String) {
			DashboardCacheUtil.putL2(key, l3, TTL_PER_USER_MS);
			return (String) l3;
		}

		// Cache miss → hitung sekali (memicu query), lalu simpan.
		String html = jalankanAman(pembuat);
		DashboardCacheUtil.putL2(key, html, TTL_PER_USER_MS);
		DashboardCacheUtil.putL3(key, html, TTL_PER_USER_MS);
		return html;
	}

	/** Hapus cache satu panel profil milik satu pengguna (mis. saat biodata diubah). */
	public static void invalidateUser(String panel, String role, Long userId) {
		if (userId == null) {
			return;
		}
		try {
			String key = bangunKey(panel, role, userId);
			DashboardCacheUtil.invalidateL2(key);
			DashboardCacheUtil.invalidateL3(key);
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileCacheUtil.java:92");
			// invalidasi best-effort
		}
	}

	private static String bangunKey(String panel, String role, Long userId) {
		try {
			return DashboardCacheUtil.keyWithFilter("Profile_" + panel, role == null ? "-" : role, userId, host());
		} catch (Throwable t) {
			return "Profile_" + panel + "_" + role + "_u" + userId;
		}
	}

	private static String jalankanAman(Pembuat pembuat) {
		try {
			String s = pembuat == null ? null : pembuat.buat();
			return s == null ? "" : s;
		} catch (Throwable t) {
			// Panel kosong lebih baik daripada menggagalkan seluruh render profil.
			try {
				Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
			} catch (Throwable ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileCacheUtil.java:113");
			}
			return "";
		}
	}

	private static String host() {
		try {
			return Common.getRequestHost();
		} catch (Throwable t) {
			return "";
		}
	}
}
