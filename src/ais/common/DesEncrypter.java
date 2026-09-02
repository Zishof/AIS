package ais.common;

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

/**
 * This class is use to encrypt and decrypt URL that is to be send to subscriber
 * it's used by delivery module when resend process.
 *
 * <h2>Riwayat — migrasi ke AES-256-GCM dicoba lalu DIBATALKAN (2026-09-02)</h2>
 * <p>
 * Kelas ini dipakai lewat {@link Common#desEncrypter} di puluhan titik seluruh AIS, termasuk
 * enkripsi password akun ({@code Tbmuser.userPassword} dan kolom {@code pass}/{@code passOrtu}
 * sejenis) dengan cipher DES dan passphrase TETAP {@link Common#DES_PASS_PHRASE} yang sama di
 * semua instalasi — lihat catatan keamanan pada javadoc {@link Common#DES_PASS_PHRASE} untuk
 * detail risikonya.
 * </p>
 * <p>
 * Pada 2026-09-02 sempat dicoba mengganti {@link #encrypt(String)} ke AES-256-GCM (kunci acak
 * per-instalasi, tidak lagi tertanam di kode) dengan {@link #decrypt(String)} tetap membaca
 * ciphertext DES lama sebagai fallback. Perubahan ini di-<i>revert</i> segera setelah diketahui
 * MEMATIKAN LOGIN — penyebabnya: sebagian pemanggil (mis. jalur login
 * {@code ais.database.model.Mahasiswa} di
 * {@code ais.common.CommonSecurityLoginHelper#checkLogin}, juga beberapa titik di
 * {@code ais.action.servlet.api.ApiUtil}, {@code ais.action.servlet.api.KantinHelper},
 * {@code ais.action.servlet.api.ResetPasswordApi}, {@code ais.action.master.resources.MahasiswaResource},
 * dan resource API lain) memverifikasi password dengan meng-enkripsi input lalu membandingkan
 * CIPHERTEXT-KE-CIPHERTEXT langsung pada query database
 * ({@code Restrictions.eq("pass", encrypt(password))}), BUKAN mendekripsi nilai tersimpan lalu
 * membandingkan plaintext. Pola ini hanya berfungsi bila {@link #encrypt(String)} DETERMINISTIK
 * (plaintext sama selalu menghasilkan ciphertext sama) — DES pada kelas ini bersifat begitu
 * (parameter salt/iterationCount tetap), sedangkan AES-GCM yang benar SELALU memakai IV acak per
 * panggilan sehingga ciphertext-nya berbeda setiap kali meski plaintext-nya sama, membuat seluruh
 * perbandingan ciphertext-ke-ciphertext di atas gagal total.
 * </p>
 * <p>
 * <b>Implikasi bagi upaya perbaikan berikutnya</b>: mengganti cipher ini ke skema yang tidak
 * deterministik (termasuk AES-GCM yang benar) TIDAK BOLEH dilakukan hanya di kelas ini — seluruh
 * pemanggil yang memakai pola "enkripsi lalu bandingkan ciphertext" harus DIUBAH LEBIH DULU
 * menjadi pola "dekripsi nilai tersimpan lalu bandingkan plaintext" (seperti jalur
 * {@code ais.database.model.Tbmuser} di {@code ais.common.CommonSecurityLoginHelper#checkLogin},
 * yang TIDAK terdampak revert ini). Kelas ini untuk saat ini TETAP memakai DES kunci tetap sampai
 * pekerjaan migrasi pemanggil tersebut dilakukan secara terpisah.
 * </p>
 *
 * @author Wildan Rizaluddin
 */
public class DesEncrypter {
	Cipher ecipher;
	Cipher dcipher;

	private static Logger log = Logger.getLogger(DesEncrypter.class);

	// 8-byte Salt
	byte[] salt = { (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
			(byte) 0x56, (byte) 0x35, (byte) 0xE3, (byte) 0x03 };

	// Iteration count
	int iterationCount = 19;

	/**
	 * constructor to create instance with Secretkey type
	 * 
	 * @param key
	 */
	public DesEncrypter(SecretKey key) {
		try {
			ecipher = Cipher.getInstance("DES");
			dcipher = Cipher.getInstance("DES");
			ecipher.init(Cipher.ENCRYPT_MODE, key);
			dcipher.init(Cipher.DECRYPT_MODE, key);
		} catch (javax.crypto.NoSuchPaddingException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DesEncrypter.java:46");
		} catch (java.security.NoSuchAlgorithmException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DesEncrypter.java:47");
		} catch (java.security.InvalidKeyException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DesEncrypter.java:48");
		}
	}

	/**
	 * constructor to create instance with String type
	 * 
	 * @param passPhrase
	 */
	public DesEncrypter(String passPhrase) {
		try {
			// Create the key
			KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt,
					iterationCount);
			SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
					.generateSecret(keySpec);
			ecipher = Cipher.getInstance(key.getAlgorithm());
			dcipher = Cipher.getInstance(key.getAlgorithm());

			// Prepare the parameter to the ciphers
			AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt,
					iterationCount);

			// Create the ciphers
			ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
			dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
		} catch (java.security.InvalidAlgorithmParameterException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DesEncrypter.java:74");
		} catch (java.security.spec.InvalidKeySpecException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DesEncrypter.java:75");
		} catch (javax.crypto.NoSuchPaddingException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DesEncrypter.java:76");
		} catch (java.security.NoSuchAlgorithmException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DesEncrypter.java:77");
		} catch (java.security.InvalidKeyException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DesEncrypter.java:78");
		}
	}

	/**
	 * method to encrypt string
	 * 
	 * @param str
	 * @return base64-encoded and encrypted string on success
	 */
	public String encrypt(String str) {
		try {
			// Encode the string into bytes using utf-8
			byte[] utf8 = str.getBytes("UTF8");

			// Encrypt
			byte[] enc = ecipher.doFinal(utf8);

			// Encode bytes to base64 to get a string
			return new String(Base64.encodeBase64(enc));
		} catch (javax.crypto.BadPaddingException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DesEncrypter.java:98");
		} catch (IllegalBlockSizeException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DesEncrypter.java:99");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DesEncrypter.java:100");
		}
		return "";
	}

	/**
	 * method to decrypt string
	 * 
	 * @param str
	 * @return decrypted string on success
	 */
	public String decrypt(String str) {
		if (str == null || str.trim().length() == 0 || dcipher == null) {
			return "";
		}
		try {
			// Decode base64 to get bytes
			byte[] dec = Base64.decodeBase64(str.getBytes());

			// Decrypt
			byte[] utf8 = dcipher.doFinal(dec);

			// Decode using utf-8
			return new String(utf8, "UTF8");
		} catch (javax.crypto.BadPaddingException e) {
			// Skenario WAJAR (bukan bug): cookie "remember me"/nilai terenkripsi
			// corrupt, kadaluarsa, atau berasal dari format enkripsi lama/beda
			// key. Cukup log info -- JANGAN pakai ErrorAuditUtil.record() di
			// sini karena selalu tercatat sbg ERROR di dashboard Error Log
			// walau sudah ditangani (return "") dan tidak mengganggu user.
			log.info("DesEncrypter.decrypt: data tidak valid/kadaluarsa (BadPadding), diabaikan: " + e.getMessage());
		} catch (IllegalBlockSizeException e) {
			log.info("DesEncrypter.decrypt: data tidak valid/kadaluarsa (IllegalBlockSize), diabaikan: " + e.getMessage());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DesEncrypter.java:123");
		}
		return "";
	}

	public static void main(String[] main) {
		DesEncrypter encrypter = new DesEncrypter("coreSDP");
		log.debug(encrypter.encrypt("Hello"));
		log.debug(encrypter.decrypt(encrypter.encrypt("Hello")));
	}
}
