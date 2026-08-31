package ais.service.registration;

import ais.common.security.PasswordHashService;

/**
 * <h3>Verifikasi mandiri logika MURNI pendaftaran tenant (§21.1) -- tanpa DB/kontainer.</h3>
 *
 * <p>Jalankan: {@code java -cp <classes> ais.service.registration.VerifikasiPendaftaranTenantMandiri}
 * -- exit 0 = semua lulus; exit 1 = ada kegagalan (dicetak). SENGAJA hanya menguji method yang
 * tidak menyentuh Konfigurasi/DB (normalisasi, regex, hash) -- uji integrasi/konkurensi ber-DB
 * ada di {@code VerifikasiKonkurensiPendaftaran} (butuh server ter-deploy, bagian UAT).</p>
 */
public final class VerifikasiPendaftaranTenantMandiri {

	private static int gagal = 0;
	private static int lulus = 0;

	/**
	 * Titik masuk harness verifikasi mandiri (§21.1 dokumen master): menjalankan serangkaian
	 * kasus uji berbasis assert sederhana ({@link #cek}) langsung terhadap method-method logika
	 * MURNI di paket pendaftaran tenant mandiri -- tanpa Hibernate/DB/kontainer, sehingga dapat
	 * dijalankan cepat dari command line ({@code java -cp <classes>
	 * ais.service.registration.VerifikasiPendaftaranTenantMandiri}) sebagai jaring pengaman
	 * sebelum deploy, terpisah dari uji integrasi/konkurensi ber-DB di
	 * {@code VerifikasiKonkurensiPendaftaran} (yang butuh server ter-deploy, bagian UAT).
	 *
	 * <p>
	 * Kasus uji dikelompokkan mengikuti bagian dokumen master yang relevan, dieksekusi berurutan
	 * dan TIDAK saling bergantung (tidak ada state bersama antar kelompok selain counter
	 * {@link #lulus}/{@link #gagal}):
	 * </p>
	 * <ul>
	 * <li><b>Normalisasi email (§14.1)</b> -- trim/lowercase via
	 * {@link PendaftaranValidationService#normalisasiEmail} dan validasi format via
	 * {@link PendaftaranValidationService#emailValid} (termasuk penolakan tanpa TLD dan yang
	 * mengandung spasi).</li>
	 * <li><b>Normalisasi + validasi username (§14.2)</b> -- lowercase, normalisasi Unicode NFKC
	 * (mis. digit/huruf fullwidth diubah ke ASCII biasa) via
	 * {@link PendaftaranValidationService#normalisasiUsername}, serta aturan panjang, karakter
	 * awal, huruf besar, dan tanda hubung (username harus aman dipakai sebagai nama schema
	 * PostgreSQL) via {@link PendaftaranValidationService#usernameValid}.</li>
	 * <li><b>Telepon</b> -- pembuangan karakter non-digit dan tanda plus ganda via
	 * {@link PendaftaranValidationService#normalisasiTelepon}.</li>
	 * <li><b>Dedup jenis usaha (§14.4)</b> -- parsing daftar id dipisah koma, pembuangan duplikat
	 * dan token bukan angka, dengan urutan dipertahankan (id pertama = primary) via
	 * {@link PendaftaranValidationService#dedupIdJenisUsaha}.</li>
	 * <li><b>{@code rapikan}</b> -- trim dan pemotongan panjang string, termasuk penanganan input
	 * {@code null}.</li>
	 * <li><b>{@link PasswordHashService} (§12.3)</b> -- panjang hash/salt hex, verifikasi
	 * password benar/salah, penanganan input {@code null}, iterasi default saat parameter
	 * {@code null}, keunikan salt antar-pemanggilan, determinisme SHA-256 (termasuk satu nilai
	 * SHA-256("abc") yang sudah dikenal publik sebagai patokan), serta keacakan/panjang token.</li>
	 * <li><b>Kompatibilitas hash dengan jalur lama</b> -- memastikan parameter algoritma/iterasi
	 * ({@code PBKDF2WithHmacSHA256}, 120000 iterasi) tetap sama dengan yang dipakai
	 * {@code PendaftarPublicHelper} versi lama, karena kolom hash dipakai bersama lintas jalur.</li>
	 * </ul>
	 *
	 * <p>
	 * Di akhir, ringkasan jumlah {@link #lulus}/{@link #gagal} dicetak ke stdout; bila ada
	 * minimal satu kegagalan, proses keluar dengan {@link System#exit(int)} kode 1 (agar mudah
	 * dideteksi oleh script/CI pemanggil), selain itu method kembali secara normal (exit 0
	 * implisit).
	 * </p>
	 *
	 * @param args tidak dipakai
	 */
	public static void main(String[] args) {
		// ---- Normalisasi email (§14.1) ----
		cek("email trim+lowercase", "budi@toko.id",
				PendaftaranValidationService.normalisasiEmail("  Budi@Toko.ID  "));
		cek("email valid", true, PendaftaranValidationService.emailValid("a@b.co"));
		cek("email tanpa tld tidak valid", false, PendaftaranValidationService.emailValid("a@b"));
		cek("email spasi tidak valid", false, PendaftaranValidationService.emailValid("a b@c.co"));
		cek("email kosong tidak valid", false, PendaftaranValidationService.emailValid(""));

		// ---- Normalisasi + validasi username (§14.2) ----
		cek("username lowercase", "tokoberkah",
				PendaftaranValidationService.normalisasiUsername("TokoBerkah"));
		cek("username NFKC (fullwidth->ascii)", "toko1",
				PendaftaranValidationService.normalisasiUsername("ｔｏｋｏ１"));
		cek("username valid dasar", true, PendaftaranValidationService.usernameValid("toko_berkah1"));
		cek("username terlalu pendek", false, PendaftaranValidationService.usernameValid("ab"));
		cek("username mulai angka", false, PendaftaranValidationService.usernameValid("1toko"));
		cek("username huruf besar ditolak", false, PendaftaranValidationService.usernameValid("Toko"));
		cek("username dash ditolak (schema-safe)", false, PendaftaranValidationService.usernameValid("toko-a"));
		cek("username 31 char valid", true,
				PendaftaranValidationService.usernameValid("a012345678901234567890123456789"));
		cek("username 32 char ditolak", false,
				PendaftaranValidationService.usernameValid("a0123456789012345678901234567890"));

		// ---- Telepon ----
		cek("telepon buang non-digit", "+628123456789",
				PendaftaranValidationService.normalisasiTelepon("+62 812-3456-789"));
		cek("telepon plus ganda dibuang", "+62812",
				PendaftaranValidationService.normalisasiTelepon("+62+812"));

		// ---- Dedup jenis usaha (§14.4) ----
		java.util.Set<Long> ids = PendaftaranValidationService.dedupIdJenisUsaha("3, 1,3,abc, 2,1");
		cek("dedup ukuran", 3, ids.size());
		cek("dedup pertahankan urutan (primary=3)", Long.valueOf(3L), ids.iterator().next());
		cek("dedup kosong", 0, PendaftaranValidationService.dedupIdJenisUsaha("  ").size());

		// ---- rapikan ----
		cek("rapikan trim", "abc", PendaftaranValidationService.rapikan("  abc  ", 10));
		cek("rapikan potong", "abcde", PendaftaranValidationService.rapikan("abcdefgh", 5));
		cek("rapikan null", "", PendaftaranValidationService.rapikan(null, 5));

		// ---- PasswordHashService (§12.3) ----
		String[] hs = PasswordHashService.hash("RahasiaKu123!");
		cek("hash hex 64 char (256 bit)", 64, hs[0].length());
		cek("salt hex 64 char (32 byte)", 64, hs[1].length());
		cek("verify benar", true,
				PasswordHashService.verify("RahasiaKu123!", hs[0], hs[1], Integer.valueOf(120000)));
		cek("verify salah", false,
				PasswordHashService.verify("RahasiaKu123?", hs[0], hs[1], Integer.valueOf(120000)));
		cek("verify null aman", false, PasswordHashService.verify(null, hs[0], hs[1], null));
		cek("verify iterasi null pakai default", true,
				PasswordHashService.verify("RahasiaKu123!", hs[0], hs[1], null));
		String[] hs2 = PasswordHashService.hash("RahasiaKu123!");
		cek("salt unik per hash", false, hs[1].equals(hs2[1]));
		cek("sha256 deterministik", PasswordHashService.sha256Hex("abc"),
				PasswordHashService.sha256Hex("abc"));
		cek("sha256 abc dikenal",
				"ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
				PasswordHashService.sha256Hex("abc"));
		cek("token acak panjang", 64, PasswordHashService.tokenAcakHex(32).length());
		cek("token acak unik", false,
				PasswordHashService.tokenAcakHex(32).equals(PasswordHashService.tokenAcakHex(32)));

		// ---- Kompatibilitas hash dgn jalur lama (PBKDF2 sama parameter) ----
		// PendaftarPublicHelper memakai PBKDF2WithHmacSHA256/120000/256 -- hash service baru
		// harus terverifikasi oleh parameter yang sama (kolom dipakai bersama).
		cek("parameter versi 1 = 120000 iterasi", 120000, PasswordHashService.ITERASI);
		cek("algoritma versi 1", "PBKDF2WithHmacSHA256", PasswordHashService.ALGORITHM);

		System.out.println("---------------------------------------------");
		System.out.println("LULUS=" + lulus + " GAGAL=" + gagal);
		if (gagal > 0) {
			System.exit(1);
		}
	}

	/**
	 * Helper assert sederhana dipakai tiap baris pengujian di {@link #main(String[])}:
	 * membandingkan {@code diharapkan} dengan {@code aktual} (via {@code equals}, aman terhadap
	 * {@code null} di kedua sisi), mencetak baris {@code [LULUS]}/{@code [GAGAL]} ke stdout, dan
	 * menaikkan counter {@link #lulus} atau {@link #gagal} sesuai hasil.
	 *
	 * @param nama       label kasus uji, dicetak apa adanya pada output
	 * @param diharapkan nilai yang diharapkan
	 * @param aktual     nilai aktual hasil pemanggilan method yang sedang diuji
	 */
	private static void cek(String nama, Object diharapkan, Object aktual) {
		boolean sama = diharapkan == null ? aktual == null : diharapkan.equals(aktual);
		if (sama) {
			lulus++;
			System.out.println("[LULUS] " + nama);
		} else {
			gagal++;
			System.out.println("[GAGAL] " + nama + " -- diharapkan <" + diharapkan + "> aktual <" + aktual + ">");
		}
	}
}
