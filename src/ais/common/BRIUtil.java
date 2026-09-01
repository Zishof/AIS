package ais.common;

import java.io.BufferedReader;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Kelas utilitas kriptografi untuk integrasi API perbankan BRI (Bank Rakyat Indonesia) di AIS,
 * berisi fungsi pembentukan <i>signature</i> (tanda tangan digital) SHA256withRSA yang
 * disyaratkan skema keamanan API BRI (pola umum API perbankan open-banking Indonesia: setiap
 * permintaan ditandatangani dengan private key RSA milik merchant/klien agar server bank dapat
 * memverifikasinya memakai public key pasangannya).
 *
 * <p>
 * <b>Relasi dengan {@link BRIDataUtil}</b> — kedua kelas ini SALING MELENGKAPI, bukan
 * duplikat: {@code BRIUtil} (kelas ini) berfokus SEMPIT pada primitif kriptografi murni —
 * memuat private key RSA format PKCS8 dan menandatangani string dengan algoritma
 * SHA256withRSA — tanpa pengetahuan apa pun tentang endpoint, format request/response, atau
 * data transaksi BRI. Sebaliknya, {@link BRIDataUtil} (lihat javadoc kelasnya) berisi
 * logika data/model dan pemanggilan API BRI yang lebih luas (mis. pembentukan payload transaksi,
 * pemetaan data AIS ke format API BRI). Dua-duanya sama-sama menyentuh konfigurasi
 * {@code BRI_PRIVATE_KEY}/kredensial BRI — {@code BRIUtil} sebagai pembentuk signature-nya,
 * {@code BRIDataUtil} sebagai pemakai signature tersebut dalam pemanggilan API sesungguhnya.
 * </p>
 *
 * <p>
 * Kelas ini murni berisi method statis tanpa state instance, kecuali dua {@link ThreadLocal}
 * {@link #dateFormat}/{@link #dateFormat1} yang dipakai untuk memformat tanggal sesuai format
 * yang disyaratkan API BRI (ISO-8601 dengan/tanpa milidetik) — pemakaian {@link ThreadLocal}
 * di sini penting karena {@link SimpleDateFormat} TIDAK thread-safe, sehingga setiap thread
 * mendapat instance formatter sendiri-sendiri agar aman dipakai bersamaan pada aplikasi web
 * multi-thread.
 * </p>
 *
 * <p>
 * <b>Riwayat keamanan (DIPERBAIKI 2026-09-01):</b> kelas ini sebelumnya menyimpan RSA private
 * key lengkap format PKCS8/PEM sebagai konstanta hardcoded ({@code PRIVATE_KEY}) yang dipakai
 * sebagai nilai default/fallback oleh {@link #readPKCS8PrivateKey()} bila konfigurasi database
 * {@code BRI_PRIVATE_KEY} belum diisi. Nilai literal itu sudah DIHAPUS dari kode sumber — kunci
 * kini WAJIB dibaca dari konfigurasi {@code BRI_PRIVATE_KEY}, dan {@link #readPKCS8PrivateKey()}
 * melempar {@link IllegalStateException} bila konfigurasi tersebut kosong, alih-alih diam-diam
 * jatuh ke key tertanam. Dua baris {@code System.out.println} yang sebelumnya mencetak PEM
 * key (setara dengan private key itu sendiri) dan bahan tanda tangan ke log juga sudah dihapus.
 * <b>Tindak lanjut yang TETAP diperlukan di luar perubahan kode ini:</b> key yang sebelumnya
 * tertanam sudah lama berada di riwayat SVN repositori ini dan harus dianggap bocor — bila itu
 * key produksi sungguhan milik merchant BRI AIS (bukan key contoh dokumentasi API), WAJIB
 * dirotasi/dicabut di sisi BRI dan diganti dengan key baru yang HANYA disimpan di konfigurasi
 * database/secret store, tidak pernah di kode sumber.
 * </p>
 */
public class BRIUtil {

	/**
	 * Formatter tanggal ISO-8601 dengan presisi milidetik ({@code yyyy-MM-dd'T'HH:mm:ss.SSS})
	 * sesuai format timestamp yang disyaratkan sebagian endpoint API BRI. Dibungkus
	 * {@link ThreadLocal} karena {@link SimpleDateFormat} tidak thread-safe.
	 */
	public static final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		}
	};
	/**
	 * Formatter tanggal ISO-8601 tanpa milidetik ({@code yyyy-MM-dd'T'HH:mm:ss}), varian lebih
	 * ringkas dari {@link #dateFormat} untuk endpoint API BRI yang tidak menyertakan presisi
	 * milidetik pada timestamp-nya. Dibungkus {@link ThreadLocal} karena
	 * {@link SimpleDateFormat} tidak thread-safe.
	 */
	public static final ThreadLocal<DateFormat> dateFormat1 = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		}
	};

	/**
	 * Memuat RSA private key yang dipakai untuk menandatangani permintaan API BRI. Nilai PEM
	 * key WAJIB berasal dari konfigurasi database {@code BRI_PRIVATE_KEY} (lewat
	 * {@code Common.getKonfigurasi}) — tidak ada nilai default/fallback tertanam di kode
	 * (lihat riwayat keamanan pada javadoc kelas). Baris header/footer PEM
	 * ({@code -----BEGIN/END PRIVATE KEY-----}) dan seluruh whitespace dibuang, sisanya
	 * di-decode Base64 menjadi bentuk {@link PKCS8EncodedKeySpec} lalu dikonversi menjadi objek
	 * {@link RSAPrivateKey} lewat {@link KeyFactory} algoritma RSA.
	 *
	 * @return objek {@link RSAPrivateKey} siap pakai untuk operasi {@link Signature}
	 * @throws IllegalStateException bila konfigurasi {@code BRI_PRIVATE_KEY} kosong/belum diisi
	 * @throws Exception diteruskan dari kegagalan decode Base64, parsing PKCS8, atau algoritma
	 *                    RSA tidak tersedia di penyedia keamanan JVM
	 */
	public static RSAPrivateKey readPKCS8PrivateKey() throws Exception {

		String konfigurasiKey = Common.getKonfigurasi("BRI_PRIVATE_KEY", "").getNilai();
		if (konfigurasiKey == null || konfigurasiKey.trim().isEmpty()) {
			throw new IllegalStateException(
					"Private key BRI belum dikonfigurasi. Isi konfigurasi BRI_PRIVATE_KEY dengan RSA private key format PKCS8/PEM merchant BRI AIS.");
		}

		StringBuilder pkcs8Lines = new StringBuilder();
		BufferedReader rdr = new BufferedReader(new StringReader(konfigurasiKey));
		String line;
		while ((line = rdr.readLine()) != null) {
			pkcs8Lines.append(line);
		}

		// Remove the "BEGIN" and "END" lines, as well as any whitespace

		String pkcs8Pem = pkcs8Lines.toString();
		pkcs8Pem = pkcs8Pem.replace("-----BEGIN PRIVATE KEY-----", "");
		pkcs8Pem = pkcs8Pem.replace("-----END PRIVATE KEY-----", "");
		pkcs8Pem = pkcs8Pem.replaceAll("\\s+", "");

		// Base64 decode the result

		byte[] encoded = java.util.Base64.getDecoder().decode(pkcs8Pem);

		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
		return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
	}

	/**
	 * Membentuk token tanda tangan digital (signature) yang disyaratkan API BRI untuk
	 * autentikasi permintaan bertipe "signature token" (biasanya dipakai pada tahap perolehan
	 * access token OAuth API BRI). String yang ditandatangani mengikuti format baku
	 * {@code "<clientId>|<formattedTimestamp>"}, ditandatangani dengan algoritma
	 * {@code SHA256withRSA} memakai private key dari {@link #readPKCS8PrivateKey()}, lalu hasil
	 * tanda tangan biner di-encode Base64 agar siap dikirim sebagai header/parameter HTTP.
	 *
	 * @param clientId          client id/merchant id API BRI
	 * @param formattedTimestamp timestamp permintaan yang sudah diformat sesuai spesifikasi API
	 *                            BRI (lihat {@link #dateFormat}/{@link #dateFormat1})
	 * @return signature dalam bentuk string Base64, siap dipakai sebagai header permintaan API
	 *         BRI
	 * @throws Exception diteruskan dari kegagalan memuat private key ({@link
	 *                    #readPKCS8PrivateKey()}) atau kegagalan proses penandatanganan
	 *                    {@link Signature}
	 */
	public static String generateSignatureToken(String clientId, String formattedTimestamp) throws Exception {
		String stringToSign = clientId + "|" + formattedTimestamp;

		Signature sig = Signature.getInstance("SHA256withRSA");
		sig.initSign(readPKCS8PrivateKey());
		sig.update(stringToSign.getBytes("UTF-8"));
		byte[] signatureBytes = sig.sign();

		String signature = java.util.Base64.getEncoder().encodeToString(signatureBytes);
		return signature;
	}

}
