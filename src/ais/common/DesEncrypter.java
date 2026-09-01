package ais.common;

import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

/**
 * Utilitas enkripsi/dekripsi <b>reversibel</b> (bukan hash satu-arah) yang dipakai di seluruh AIS
 * untuk data yang memang harus bisa dikembalikan ke bentuk aslinya — terutama password akun
 * ({@link ais.database.model.Tbmuser#getUserPassword()} dan kolom {@code pass}/{@code passOrtu}
 * serupa pada entitas lain) yang ditampilkan ulang/di-download admin apa adanya (mis.
 * {@link ais.common.helper.DownloadPasswordPegawaiHelper}, jendela reset password), serta URL/
 * cookie yang perlu dibaca ulang isinya oleh server (link "resend", cookie "remember me", token
 * akses berkas). Dipakai lewat instance bersama {@link Common#desEncrypter} (satu per thread).
 *
 * <h2>Riwayat keamanan (DIPERBAIKI 2026-09-02) — dari DES kunci tetap ke AES-256-GCM kunci
 * per-instalasi</h2>
 * <p>
 * Sebelum perbaikan ini, {@link #encrypt(String)}/{@link #decrypt(String)} SELALU memakai cipher
 * <b>DES</b> (kunci efektif 56-bit, sudah lama dianggap usang secara kriptografi) dengan
 * passphrase {@code "AIS_UIN"} yang TETAP dan SAMA untuk seluruh instalasi AIS mana pun (lihat
 * {@link Common#DES_PASS_PHRASE}, ditulis langsung di kode sumber). Konsekuensinya: siapa pun yang
 * membaca kode sumber ini (termasuk lewat riwayat kontrol versi) otomatis punya kunci untuk
 * mendekripsi SELURUH data yang pernah dienkripsi lewat kelas ini di SEMUA instalasi AIS yang
 * berbagi codebase ini — termasuk password akun {@code Tbmuser} yang disimpan dengan
 * {@code is_encripted=true}.
 * </p>
 * <p>
 * Perbaikan ini MENGGANTI algoritma enkripsi untuk data baru menjadi <b>AES-256-GCM</b> (cipher
 * modern dengan autentikasi bawaan — manipulasi ciphertext terdeteksi lewat kegagalan tag, bukan
 * hanya diam-diam menghasilkan plaintext salah seperti mode tanpa autentikasi) dengan kunci 256-bit
 * yang di-<b>generate acak per instalasi</b> (bukan tertanam di kode) memakai
 * {@link java.security.SecureRandom}, lalu disimpan sekali lewat
 * {@link Common#getKonfigurasi(String, String)} pada kunci konfigurasi
 * {@value #KONFIGURASI_KEY_AES} — mengikuti pola auto-seed standar {@code getKonfigurasi} di AIS
 * (baris dibuat otomatis di database pada pembacaan pertama bila belum ada, nilai selanjutnya
 * selalu dibaca dari database, BUKAN dari kode). Kunci ini SENGAJA TIDAK didaftarkan sebagai baris
 * yang dapat diedit administrator di {@code KonfigurasiNewAction} — berbeda dari kredensial
 * integrasi biasa, kunci ini adalah kunci pemulihan data: mengubahnya lewat UI akan membuat SELURUH
 * data yang sudah dienkripsi dengan kunci lama menjadi PERMANEN tidak terbaca.
 * </p>
 * <p>
 * <b>Format ciphertext versi baru</b> — {@code "AESGCMv1:" + base64(iv 12-byte) + ":" +
 * base64(ciphertext+tag GCM)} — SELALU mengandung karakter {@code ":"} yang tidak pernah muncul
 * pada keluaran Base64 standar (alfabet {@code A-Za-z0-9+/=}), sehingga {@link #decrypt(String)}
 * dapat membedakan data format baru dari data lama secara otomatis TANPA kolom penanda tambahan
 * di database.
 * </p>
 * <p>
 * <b>Kompatibilitas mundur (WAJIB, bukan opsional)</b> — {@link #encrypt(String)} SELALU
 * menghasilkan format AES-GCM baru, namun {@link #decrypt(String)} tetap dapat membaca ciphertext
 * DES lama (dienkripsi dengan {@link Common#DES_PASS_PHRASE}) lewat jalur DES bawaan konstruktor
 * ({@link #DesEncrypter(String)}/{@link #DesEncrypter(SecretKey)}) sebagai FALLBACK bila string
 * tidak cocok format baru. Ini penting karena kelas ini dipakai di puluhan titik di seluruh AIS
 * (password akun, cookie "remember me", link resend email, token akses berkas) — mengganti
 * algoritma tanpa jalur baca-mundur akan membuat SELURUH data lama (password tersimpan, cookie
 * aktif, link yang sudah terkirim) mendadak tidak terbaca. Data lama TIDAK otomatis
 * dienkripsi-ulang oleh perbaikan ini; migrasi ke format baru terjadi bertahap secara alami setiap
 * kali suatu nilai ditulis ulang (mis. password diganti/direset, cookie baru diterbitkan).
 * </p>
 * <p>
 * <b>TINDAK LANJUT DI LUAR PERUBAHAN KODE INI</b>: passphrase DES lama ({@code "AIS_UIN"}) sudah
 * lama berada di riwayat SVN dan WAJIB dianggap bocor untuk SELURUH data yang masih berformat DES
 * lama (belum ditulis ulang sejak perbaikan ini). Disarankan menjalankan pekerjaan migrasi data
 * terpisah (di luar cakupan perubahan ini) yang membaca-lalu-menulis-ulang seluruh baris
 * {@code Tbmuser.userPassword}/kolom sejenis agar berpindah ke format AES-GCM baru tanpa menunggu
 * pengguna mengganti password sendiri.
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

	/** Penanda format ciphertext AES-256-GCM (versi 1) pada segmen pertama sebelum ":" — lihat riwayat keamanan pada javadoc kelas. */
	private static final String PENANDA_AESGCM_V1 = "AESGCMv1";
	/** Panjang IV GCM dalam byte, mengikuti rekomendasi NIST SP 800-38D (96 bit). */
	private static final int PANJANG_IV_GCM_BYTE = 12;
	/** Panjang tag autentikasi GCM dalam bit. */
	private static final int PANJANG_TAG_GCM_BIT = 128;
	/** Kunci konfigurasi tempat kunci AES 256-bit (base64) disimpan per instalasi — lihat riwayat keamanan pada javadoc kelas. */
	private static final String KONFIGURASI_KEY_AES = "des_encrypter_aes_key";

	/** Cache statis kunci AES 256-bit (raw bytes) sekali per JVM, diisi lazy oleh {@link #ambilKunciAes()}. */
	private static volatile byte[] aesKeyCache;

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
	 * Mengenkripsi {@code str} memakai AES-256-GCM dengan kunci per-instalasi (lihat riwayat
	 * keamanan pada javadoc kelas) — SELALU menghasilkan format baru {@code "AESGCMv1:" +
	 * base64(iv) + ":" + base64(ciphertext+tag)}, tidak lagi memakai DES ({@link #ecipher},
	 * sisa konstruktor lama, tidak dipakai method ini).
	 *
	 * @param str teks asli yang akan dienkripsi
	 * @return ciphertext berformat {@code "AESGCMv1:iv:data"} pada sukses, atau string kosong bila
	 *         terjadi kegagalan (dicatat lewat {@link ErrorAuditUtil#record})
	 */
	public String encrypt(String str) {
		try {
			byte[] iv = new byte[PANJANG_IV_GCM_BYTE];
			new SecureRandom().nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(ambilKunciAes(), "AES"),
					new GCMParameterSpec(PANJANG_TAG_GCM_BIT, iv));
			byte[] sealed = cipher.doFinal(str.getBytes("UTF-8"));
			return PENANDA_AESGCM_V1 + ":" + new String(Base64.encodeBase64(iv)) + ":"
					+ new String(Base64.encodeBase64(sealed));
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "DesEncrypter.encrypt (AES-256-GCM) gagal");
		}
		return "";
	}

	/**
	 * Mendekripsi {@code str}: mendeteksi otomatis apakah ciphertext berformat AES-256-GCM baru
	 * ({@code "AESGCMv1:..."}, lihat {@link #encrypt(String)}) atau format DES lama (base64 polos
	 * tanpa {@code ":"}, dienkripsi via konstruktor {@link #DesEncrypter(String)}/
	 * {@link #DesEncrypter(SecretKey)}), lalu mendekripsi lewat jalur yang sesuai. Lihat riwayat
	 * keamanan pada javadoc kelas mengenai alasan dua jalur ini dipertahankan bersamaan.
	 *
	 * @param str ciphertext hasil {@link #encrypt(String)} (format baru atau lama)
	 * @return teks asli pada sukses; string kosong bila {@code str} kosong/null, kedaluwarsa,
	 *         berasal dari kunci/format berbeda, atau gagal didekripsi
	 */
	public String decrypt(String str) {
		if (str == null || str.trim().length() == 0) {
			return "";
		}
		String[] bagian = str.split(":", 3);
		if (bagian.length == 3 && PENANDA_AESGCM_V1.equals(bagian[0])) {
			return decryptAesGcm(bagian[1], bagian[2]);
		}
		return decryptLegacyDes(str);
	}

	/** Jalur dekripsi AES-256-GCM (format baru) — lihat {@link #decrypt(String)}. */
	private String decryptAesGcm(String ivBase64, String dataBase64) {
		try {
			byte[] iv = Base64.decodeBase64(ivBase64);
			byte[] sealed = Base64.decodeBase64(dataBase64);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(ambilKunciAes(), "AES"),
					new GCMParameterSpec(PANJANG_TAG_GCM_BIT, iv));
			return new String(cipher.doFinal(sealed), "UTF-8");
		} catch (javax.crypto.BadPaddingException e) {
			// Skenario WAJAR (bukan bug): tag autentikasi GCM gagal -- data corrupt, kunci
			// instalasi berbeda, atau ciphertext bukan hasil encrypt() versi ini. Cukup log
			// info, JANGAN ErrorAuditUtil.record() (selalu tercatat sbg ERROR di dashboard).
			log.info("DesEncrypter.decrypt: tag AES-GCM tidak valid, diabaikan: " + e.getMessage());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "DesEncrypter.decrypt (AES-256-GCM) gagal");
		}
		return "";
	}

	/**
	 * Jalur dekripsi DES lama (format sebelum perbaikan 2026-09-02) — lihat
	 * {@link #decrypt(String)} dan riwayat keamanan pada javadoc kelas.
	 *
	 * @return decrypted string on success
	 */
	private String decryptLegacyDes(String str) {
		if (dcipher == null) {
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

	/**
	 * Mengambil kunci AES 256-bit per-instalasi, membuatnya sekali secara acak
	 * ({@link SecureRandom}) dan menyimpannya lewat {@link Common#getKonfigurasi(String, String)}
	 * bila belum ada baris konfigurasi {@value #KONFIGURASI_KEY_AES} (pola auto-seed standar AIS),
	 * lalu meng-cache hasilnya secara statis (sekali per JVM, {@code volatile} + double-checked
	 * locking) agar pemanggilan berikutnya tidak perlu round-trip database. Lihat riwayat keamanan
	 * pada javadoc kelas mengenai alasan kunci ini sengaja TIDAK didaftarkan sebagai baris yang
	 * dapat diedit administrator.
	 *
	 * @return kunci AES 256-bit (32 byte) mentah, konsisten selama masa hidup JVM
	 * @throws IllegalStateException bila nilai konfigurasi yang tersimpan bukan 32 byte setelah
	 *                                di-decode Base64 (mis. diedit manual secara keliru)
	 */
	private static byte[] ambilKunciAes() throws Exception {
		byte[] cached = aesKeyCache;
		if (cached != null) {
			return cached;
		}
		synchronized (DesEncrypter.class) {
			if (aesKeyCache != null) {
				return aesKeyCache;
			}
			byte[] kunciBaruJikaBelumAda = new byte[32];
			new SecureRandom().nextBytes(kunciBaruJikaBelumAda);
			String defaultBase64 = new String(Base64.encodeBase64(kunciBaruJikaBelumAda));
			String nilaiTersimpan = Common.getKonfigurasi(KONFIGURASI_KEY_AES, defaultBase64).getNilai();
			byte[] kunci = Base64.decodeBase64(nilaiTersimpan);
			if (kunci == null || kunci.length != 32) {
				throw new IllegalStateException("Kunci AES DesEncrypter (konfigurasi '" + KONFIGURASI_KEY_AES
						+ "') tidak valid, harus tepat 32 byte setelah decode Base64.");
			}
			aesKeyCache = kunci;
			return kunci;
		}
	}

	public static void main(String[] main) {
		DesEncrypter encrypter = new DesEncrypter("coreSDP");
		log.debug(encrypter.encrypt("Hello"));
		log.debug(encrypter.decrypt(encrypter.encrypt("Hello")));
	}
}
