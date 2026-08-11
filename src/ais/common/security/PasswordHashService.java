package ais.common.security;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * <h3>Hash password satu-arah VERSIONED utk permukaan publik pendaftaran tenant.</h3>
 *
 * <p>Parameter versi 1 = PBKDF2WithHmacSHA256, 120.000 iterasi, key 256-bit, salt 32 byte --
 * SAMA dgn {@code PendaftarPublicHelper} existing supaya hash yang ditulis service baru tetap
 * bisa diverifikasi jalur login lama (kolom {@code pendaftar.password_hash/password_salt}
 * dipakai bersama). Bedanya dari helper lama: perbandingan hash CONSTANT-TIME
 * ({@link MessageDigest#isEqual}) dan parameter tercatat (algorithm/iterations/version)
 * di {@code pendaftar_tenant_profile} supaya upgrade-on-login dimungkinkan.</p>
 *
 * <p>DILARANG: menyimpan plaintext, menulis password ke log/audit, memakai DES reversible
 * ({@code Common.desEncrypter}) utk registrasi publik.</p>
 */
public final class PasswordHashService {

	public static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	public static final int VERSION = 1;
	public static final int ITERASI = 120000;
	private static final int PANJANG_KEY_BIT = 256;
	private static final int PANJANG_SALT_BYTE = 32;

	private PasswordHashService() {
	}

	/** @return [hashHex, saltHex] dgn parameter versi terbaru. */
	public static String[] hash(String password) {
		byte[] salt = new byte[PANJANG_SALT_BYTE];
		new SecureRandom().nextBytes(salt);
		return new String[] { pbkdf2(password, salt, ITERASI), byteToHex(salt) };
	}

	/** Verifikasi constant-time terhadap hash+salt tersimpan (iterasi mengikuti parameter tersimpan). */
	public static boolean verify(String password, String hashHexTersimpan, String saltHexTersimpan,
			Integer iterasiTersimpan) {
		if (password == null || hashHexTersimpan == null || saltHexTersimpan == null) {
			return false;
		}
		int iterasi = iterasiTersimpan == null || iterasiTersimpan.intValue() <= 0 ? ITERASI
				: iterasiTersimpan.intValue();
		String hashHex = pbkdf2(password, hexToByte(saltHexTersimpan), iterasi);
		return MessageDigest.isEqual(hashHex.toLowerCase().getBytes(), hashHexTersimpan.toLowerCase().getBytes());
	}

	private static String pbkdf2(String password, byte[] salt, int iterasi) {
		try {
			PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterasi, PANJANG_KEY_BIT);
			SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
			return byteToHex(skf.generateSecret(spec).getEncoded());
		} catch (Exception e) {
			throw new RuntimeException("Gagal menghitung hash password", e);
		}
	}

	/** SHA-256 hex utk token/OTP (verifikasi email, reservation token) -- token mentah tidak disimpan. */
	public static String sha256Hex(String data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return byteToHex(md.digest(data == null ? new byte[0] : data.getBytes("UTF-8")));
		} catch (Exception e) {
			throw new RuntimeException("Gagal menghitung SHA-256", e);
		}
	}

	/** Token acak kriptografis, hex (panjangByte*2 karakter). */
	public static String tokenAcakHex(int panjangByte) {
		byte[] b = new byte[panjangByte];
		new SecureRandom().nextBytes(b);
		return byteToHex(b);
	}

	private static String byteToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (int i = 0; i < bytes.length; i++) {
			sb.append(String.format("%02x", Integer.valueOf(bytes[i] & 0xff)));
		}
		return sb.toString();
	}

	private static byte[] hexToByte(String hex) {
		int len = hex.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i + 1 < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
		}
		return data;
	}
}
