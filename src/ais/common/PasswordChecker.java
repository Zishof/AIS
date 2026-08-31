package ais.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pemeriksa kekuatan (strength) password sederhana berbasis satu ekspresi reguler. Kelas ini
 * berdiri sendiri (tidak dipanggil kelas lain dalam berkas yang ditinjau) dan menyediakan satu
 * aturan validasi tetap lewat {@link #isValidPassword(String)}, ditambah metode {@link
 * #main(String[])} yang berfungsi sebagai contoh pemakaian/uji manual di konsol untuk beberapa
 * kandidat password.
 *
 * <h2>Aturan kekuatan password yang diperiksa</h2>
 * <p>
 * Sebuah password dinyatakan valid oleh {@link #isValidPassword(String)} hanya bila SELURUH syarat
 * berikut terpenuhi sekaligus:
 * </p>
 * <ul>
 * <li><b>Panjang minimal 8 karakter</b> — dicek dua kali: pemeriksaan awal eksplisit
 * ({@code password.length() < 8}) sebagai penolakan cepat untuk {@code null}/terlalu pendek, dan
 * kuantifier {@code {8,}} di akhir regex sebagai penegasan panjang minimal yang sama pada seluruh
 * string yang diperiksa regex.</li>
 * <li><b>Minimal satu digit</b> — lookahead {@code (?=.*[0-9])} mensyaratkan setidaknya satu
 * karakter {@code 0-9} di mana pun dalam string.</li>
 * <li><b>Minimal satu huruf</b> — lookahead {@code (?=.*[a-zA-Z])} mensyaratkan setidaknya satu
 * huruf, besar ATAU kecil (regex ini <i>tidak</i> membedakan/mewajibkan kombinasi huruf besar DAN
 * kecil secara terpisah — cukup satu huruf apa pun sudah memenuhi syarat ini).</li>
 * <li><b>Minimal satu karakter spesial</b> — lookahead
 * {@code (?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?])} mensyaratkan setidaknya satu karakter dari
 * himpunan tanda baca/simbol berikut: {@code ! @ # $ % ^ & * ( ) _ + - = [ ] { } ; ' : " \ | , . < > / ?}.
 * Karakter di luar himpunan ini (mis. spasi, {@code ~}, {@code `}, atau karakter Unicode non-ASCII)
 * TIDAK dihitung sebagai karakter spesial oleh regex ini.</li>
 * </ul>
 * <p>
 * Tidak ada batas panjang maksimum yang diperiksa (kuantifier {@code {8,}} tidak berbatas atas),
 * dan tidak ada pemeriksaan tambahan seperti larangan spasi, larangan pola berurutan/berulang, atau
 * pengecekan terhadap daftar password umum/bocor — validasi murni berbasis komposisi karakter di
 * atas.
 * </p>
 */
public class PasswordChecker {

	/**
	 * Memeriksa apakah {@code password} memenuhi seluruh aturan kekuatan password yang dijelaskan
	 * pada javadoc kelas ({@link PasswordChecker}): panjang minimal 8 karakter, mengandung minimal
	 * satu digit, satu huruf, dan satu karakter spesial dari himpunan tanda baca yang didefinisikan
	 * di {@code regex}.
	 *
	 * @param password kandidat password yang akan diperiksa; boleh {@code null}
	 * @return {@code true} bila {@code password} tidak {@code null}, panjangnya minimal 8 karakter,
	 *         dan mengandung minimal satu digit, satu huruf, serta satu karakter spesial;
	 *         {@code false} sebaliknya
	 */
	public static boolean isValidPassword(String password) {
		if (password == null || password.length() < 8) {
			return false; // Password harus minimal 8 karakter
		}

		// Regex untuk memeriksa minimal 1 huruf, 1 angka, dan 1 karakter spesial
		String regex = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(password);

		return matcher.matches();
	}

	/**
	 * Contoh pemakaian/uji manual {@link #isValidPassword(String)} di konsol terhadap tujuh kandidat
	 * password yang masing-masing dirancang untuk melanggar (atau memenuhi) satu aturan tertentu —
	 * lihat komentar inline di setiap baris untuk hasil yang diharapkan.
	 *
	 * @param args argumen baris perintah; tidak dipakai
	 */
	public static void main(String[] args) {
		String password1 = "short";
		String password2 = "password123";
		String password3 = "P@$$wOrd";
		String password4 = "P@$$wOrd123";

		System.out.println(password1 + ": " + isValidPassword(password1)); // false (terlalu pendek)
		System.out.println(password2 + ": " + isValidPassword(password2)); // false (tidak ada karakter spesial)
		System.out.println(password3 + ": " + isValidPassword(password3)); // true (memenuhi semua kriteria)
		System.out.println(password4 + ": " + isValidPassword(password4)); // true (memenuhi semua kriteria)

		// Tambahan: Contoh lain untuk menunjukkan fleksibilitas regex
		String password5 = "12345678!"; // false (tidak ada huruf)
		String password6 = "abcdefgh!"; // false (tidak ada angka)
		String password7 = "abcdefgh1"; // false (tidak ada karakter spesial)
		System.out.println(password5 + ": " + isValidPassword(password5));
		System.out.println(password6 + ": " + isValidPassword(password6));
		System.out.println(password7 + ": " + isValidPassword(password7));

	}
}