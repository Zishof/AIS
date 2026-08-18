package ais.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordChecker {

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