package ais.action.servlet.api.biometric;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/** Enkripsi AES-256-GCM untuk template biometrik di server. */
public final class BiometricCrypto {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int MAX_TEMPLATE_BYTES = 65536;

	private BiometricCrypto() { }

	public static boolean configured() {
		try { key(); return true; } catch (Exception e) { return false; }
	}

	public static String keyVersion() {
		String value = setting("AIS_BIOMETRIC_KEY_VERSION");
		return value == null ? "v1" : value;
	}

	public static byte[] decodeTemplate(String base64) throws Exception {
		if (base64 == null || base64.trim().length() == 0) throw new IllegalArgumentException("Template biometrik kosong");
		byte[] bytes = Base64.decodeBase64(base64.trim());
		if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("Template biometrik tidak valid");
		if (bytes.length > MAX_TEMPLATE_BYTES) throw new IllegalArgumentException("Template biometrik melebihi 64 KiB");
		return bytes;
	}

	public static String encrypt(byte[] clear, String aad) throws Exception {
		byte[] iv = new byte[12]; RANDOM.nextBytes(iv);
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(128, iv));
		if (aad != null) cipher.updateAAD(aad.getBytes(UTF8));
		byte[] sealed = cipher.doFinal(clear);
		return keyVersion() + ":" + Base64.encodeBase64String(iv) + ":" + Base64.encodeBase64String(sealed);
	}

	public static byte[] decrypt(String encoded, String aad) throws Exception {
		if (encoded == null) throw new IllegalArgumentException("Ciphertext kosong");
		String[] parts = encoded.split(":", 3);
		if (parts.length != 3) throw new IllegalArgumentException("Format ciphertext tidak dikenal");
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(), "AES"),
				new GCMParameterSpec(128, Base64.decodeBase64(parts[1])));
		if (aad != null) cipher.updateAAD(aad.getBytes(UTF8));
		return cipher.doFinal(Base64.decodeBase64(parts[2]));
	}

	public static String sha256(byte[] bytes) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
		StringBuilder out = new StringBuilder(digest.length * 2);
		for (int i = 0; i < digest.length; i++) out.append(String.format("%02x", Integer.valueOf(digest[i] & 0xff)));
		return out.toString();
	}

	private static byte[] key() throws Exception {
		String configured = setting("AIS_BIOMETRIC_MASTER_KEY_BASE64");
		if (configured == null) throw new IllegalStateException("AIS_BIOMETRIC_MASTER_KEY_BASE64 belum disetel");
		byte[] key = Base64.decodeBase64(configured);
		if (key == null || key.length != 32) throw new IllegalStateException("Kunci biometrik harus tepat 32 byte");
		return key;
	}

	public static String setting(String name) {
		String value = System.getenv(name);
		if (value == null || value.trim().length() == 0) value = System.getProperty(name);
		return value == null || value.trim().length() == 0 ? null : value.trim();
	}
}
