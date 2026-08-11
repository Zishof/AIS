package ais.service.registration;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import ais.common.Common;

/**
 * <h3>Normalisasi + validasi field pendaftaran tenant (§14 dokumen master).</h3>
 *
 * <p>Murni statis tanpa akses DB (kecuali baca Konfigurasi ter-cache) supaya gampang diuji.
 * Aturan yang butuh DB (ketersediaan username, jenis usaha aktif) hidup di
 * {@link UsernameReservationService}/{@link PendaftaranTenantService}.</p>
 */
public final class PendaftaranValidationService {

	/** Regex username tenant (§14.2): huruf kecil awal, lalu huruf/angka/underscore, total 3-31. */
	public static final Pattern POLA_USERNAME = Pattern.compile("^[a-z][a-z0-9_]{2,30}$");
	private static final Pattern POLA_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	/** Reserved names bawaan (§3.4) -- WAJIB tetap terlarang apa pun isi konfigurasi. */
	private static final String[] RESERVED_BAWAAN = { "admin", "administrator", "root", "public",
			"information_schema", "pg_catalog", "postgres", "demo", "api", "app", "login", "logout",
			"pendaftaran", "www", "mail", "smtp", "ftp", "support", "billing", "static", "assets",
			"ebisnis", "ecampus", "eschool", "santri", "new_audit", "koperasi" };

	private PendaftaranValidationService() {
	}

	public static String normalisasiEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase();
	}

	public static boolean emailValid(String emailNormalized) {
		return emailNormalized != null && emailNormalized.length() <= 255
				&& POLA_EMAIL.matcher(emailNormalized).matches();
	}

	/** Unicode NFKC → lowercase → trim; TIDAK memaksa jadi valid (validasi terpisah). */
	public static String normalisasiUsername(String username) {
		if (username == null) {
			return "";
		}
		String n = Normalizer.normalize(username, Normalizer.Form.NFKC).trim().toLowerCase();
		return n;
	}

	public static boolean usernameValid(String usernameNormalized) {
		return usernameNormalized != null && POLA_USERNAME.matcher(usernameNormalized).matches();
	}

	/** Gabungan reserved bawaan + konfigurasi `pendaftaran_reserved_usernames` (CSV, tanpa compile ulang). */
	public static boolean usernameReserved(String usernameNormalized) {
		if (usernameNormalized == null || usernameNormalized.isEmpty()) {
			return true;
		}
		for (int i = 0; i < RESERVED_BAWAAN.length; i++) {
			if (RESERVED_BAWAAN[i].equals(usernameNormalized)) {
				return true;
			}
		}
		try {
			String csv = Common.getKonfigurasi("pendaftaran_reserved_usernames", "").getNilai();
			if (csv != null && !csv.trim().isEmpty()) {
				String[] tokens = csv.toLowerCase().split("[,;\\s]+");
				for (int i = 0; i < tokens.length; i++) {
					if (usernameNormalized.equals(tokens[i].trim())) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PendaftaranValidationService.usernameReserved");
		}
		return false;
	}

	/** Normalisasi telepon: simpan digit + plus di depan (display asli tetap milik pemanggil). */
	public static String normalisasiTelepon(String telp) {
		if (telp == null) {
			return "";
		}
		String bersih = telp.replaceAll("[^0-9+]", "");
		if (bersih.startsWith("+")) {
			bersih = "+" + bersih.substring(1).replaceAll("\\+", "");
		}
		return bersih;
	}

	/** Panjang minimal password configurable (`pendaftaran_password_min`, default 10 -- §5.1 langkah 7). */
	public static int minimalPanjangPassword() {
		try {
			String v = Common.getKonfigurasi("pendaftaran_password_min", "10").getNilai();
			int n = Integer.parseInt(v.trim());
			return n < 6 ? 6 : n;
		} catch (Exception e) {
			return 10;
		}
	}

	/** null = valid; selain itu pesan kesalahan utk field password. */
	public static String cekPassword(String password, String konfirmasi) {
		int min = minimalPanjangPassword();
		if (password == null || password.length() < min) {
			return "Password minimal " + min + " karakter.";
		}
		if (password.length() > 200) {
			return "Password terlalu panjang.";
		}
		if (!password.equals(konfirmasi)) {
			return "Konfirmasi password tidak sama.";
		}
		return null;
	}

	/** Versi dokumen terpublikasi saat ini (§14.5) -- submit ditolak bila versi klien berbeda. */
	public static String versiTermsAktif() {
		try {
			return Common.getKonfigurasi("pendaftaran_terms_version", "2026-01").getNilai();
		} catch (Exception e) {
			return "2026-01";
		}
	}

	public static String versiPrivacyAktif() {
		try {
			return Common.getKonfigurasi("pendaftaran_privacy_version", "2026-01").getNilai();
		} catch (Exception e) {
			return "2026-01";
		}
	}

	/** Dedup id jenis usaha dgn mempertahankan urutan pilihan (id pertama = kandidat primary). */
	public static Set<Long> dedupIdJenisUsaha(String csvIds) {
		LinkedHashSet<Long> hasil = new LinkedHashSet<Long>();
		if (csvIds == null || csvIds.trim().isEmpty()) {
			return hasil;
		}
		String[] tokens = csvIds.split("[,;\\s]+");
		for (int i = 0; i < tokens.length; i++) {
			try {
				String t = tokens[i].trim();
				if (!t.isEmpty()) {
					hasil.add(Long.valueOf(t));
				}
			} catch (NumberFormatException e) {
				// id bukan angka -> abaikan token (validasi keberadaan/aktif dilakukan terhadap DB)
			}
		}
		return hasil;
	}

	/** Trim aman + potong panjang (jaga kolom varchar; tidak merusak karakter sah). */
	public static String rapikan(String s, int maksimal) {
		if (s == null) {
			return "";
		}
		String t = s.trim();
		return t.length() > maksimal ? t.substring(0, maksimal) : t;
	}
}
