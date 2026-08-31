package ais.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitas kecil untuk menghasilkan digest MD5 (32 karakter heksadesimal huruf kecil) dari sebuah
 * {@link String}, memakai {@link MessageDigest} bawaan JDK dengan algoritma {@code "MD5"}.
 *
 * <h2>Peringatan keamanan — MD5 TIDAK aman untuk kata sandi</h2>
 * <p>
 * MD5 secara kriptografis <b>sudah tidak aman</b> untuk kebutuhan keamanan modern, termasuk (dan
 * terutama) untuk menyimpan/memverifikasi kata sandi:
 * </p>
 * <ul>
 * <li><b>Rentan collision</b> — sejak 2004-2005 telah ditemukan metode praktis untuk membuat dua
 * input berbeda yang menghasilkan digest MD5 identik, sehingga MD5 tidak dapat diandalkan untuk
 * jaminan integritas kriptografis (mis. tanda tangan digital, verifikasi keaslian berkas).</li>
 * <li><b>Rentan rainbow table / brute force</b> — MD5 dirancang untuk kecepatan komputasi, BUKAN
 * untuk memperlambat penyerang (tanpa salt dan tanpa faktor kerja/iterasi seperti bcrypt, scrypt,
 * Argon2, atau PBKDF2). Digest MD5 dari string umum (kata sandi pendek/lazim) dapat dipecahkan
 * hampir seketika lewat tabel pra-hitung (rainbow table) yang tersedia luas, dan brute force atas
 * MD5 dapat dijalankan pada kecepatan miliaran percobaan per detik dengan GPU modern.</li>
 * <li>Kelas ini TIDAK menerapkan salt sama sekali — dua input identik ({@code str} yang sama)
 * SELALU menghasilkan digest yang identik pula, sehingga pemakaian untuk kata sandi juga rentan
 * terhadap serangan lookup-table lintas-pengguna (dua pengguna dengan kata sandi sama akan
 * memiliki hash tersimpan yang sama, membocorkan kesamaan tersebut).</li>
 * </ul>
 * <p>
 * Catatan ini bersifat informatif dan TIDAK mengubah perilaku kode — bila kelas ini dipakai di
 * suatu tempat untuk menyimpan/memverifikasi kata sandi pengguna, hal itu adalah risiko keamanan
 * nyata yang sebaiknya ditinjau dan dimigrasikan ke fungsi hash kata sandi khusus (bcrypt, scrypt,
 * Argon2, atau minimal PBKDF2 dengan salt unik per pengguna dan faktor kerja yang memadai) secara
 * terpisah dari pekerjaan dokumentasi ini.
 * </p>
 *
 * <h2>Catatan thread-safety</h2>
 * <p>
 * Bidang {@link #digester} adalah satu instans {@link MessageDigest} statis yang dibagi ke seluruh
 * pemanggil {@link #crypt(String)} di JVM yang sama. {@link MessageDigest} pada implementasi JDK
 * standar TIDAK dijamin aman dipakai bersamaan dari banyak thread tanpa sinkronisasi eksternal —
 * pemanggilan {@link #crypt(String)} secara konkuren dari banyak thread berpotensi menghasilkan
 * hasil digest yang salah/tidak konsisten (kondisi balapan pada state internal digester) alih-alih
 * melempar exception yang mudah terdeteksi. Ini adalah catatan observasi atas kode yang ada, bukan
 * perubahan perilaku.
 * </p>
 */
public class MD5 {
	/**
	 * Instans {@link MessageDigest} algoritma MD5 yang dibagi statis untuk seluruh pemanggilan
	 * {@link #crypt(String)}. Diinisialisasi sekali di blok static; bila algoritma {@code "MD5"}
	 * tidak tersedia di provider JCE runtime (praktiknya nyaris tidak pernah terjadi karena MD5
	 * adalah algoritma bawaan wajib JDK), tetap bernilai {@code null} dan setiap pemanggilan
	 * {@link #crypt(String)} berikutnya akan melempar {@link NullPointerException} saat memanggil
	 * {@code digester.update(...)}.
	 */
	private static MessageDigest digester;

	/**
	 * Blok inisialisasi statis: membuat instans {@link #digester} sekali saat kelas dimuat.
	 * Kegagalan (seharusnya tidak pernah terjadi untuk algoritma bawaan MD5) hanya dilaporkan lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)} dan TIDAK menghentikan pemuatan kelas — lihat
	 * catatan pada {@link #digester} soal konsekuensi bila inisialisasi ini gagal.
	 */
	static {
		try {
			digester = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menghitung digest MD5 dari {@code str} dan mengembalikannya sebagai string heksadesimal huruf
	 * kecil sepanjang 32 karakter (tiap byte digest direpresentasikan 2 karakter heksa, diberi
	 * awalan {@code "0"} bila nilainya di bawah {@code 0x10} agar panjang tetap konsisten).
	 *
	 * <p>
	 * Lihat peringatan keamanan pada javadoc kelas ini sebelum memakai method ini untuk menyimpan
	 * atau memverifikasi kata sandi — MD5 tanpa salt tidak layak dipakai untuk tujuan tersebut.
	 * </p>
	 *
	 * @param str teks yang akan di-hash; TIDAK boleh {@code null} atau string kosong
	 * @return digest MD5 dari {@code str} dalam bentuk heksadesimal huruf kecil 32 karakter
	 * @throws IllegalArgumentException bila {@code str} {@code null} atau panjangnya nol
	 */
	public static String crypt(String str) {
		if (str == null || str.length() == 0) {
			throw new IllegalArgumentException(
					"String to encript cannot be null or zero length");
		}

		digester.update(str.getBytes());
		byte[] hash = digester.digest();
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
			if ((0xff & hash[i]) < 0x10) {
				hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
			} else {
				hexString.append(Integer.toHexString(0xFF & hash[i]));
			}
		}
		return hexString.toString();
	}
}