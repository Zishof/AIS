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
 * <b>PERINGATAN KEAMANAN KRITIS:</b> konstanta {@link #PRIVATE_KEY} berisi RSA private key
 * lengkap format PKCS8/PEM yang ditulis langsung (hardcoded) di kode sumber ini (baris berisi
 * blok {@code -----BEGIN PRIVATE KEY----- ... -----END PRIVATE KEY-----}). Nilai ini dipakai
 * sebagai NILAI DEFAULT/FALLBACK oleh {@link #readPKCS8PrivateKey()} — yaitu hanya dipakai bila
 * konfigurasi database {@code BRI_PRIVATE_KEY} (lewat {@code Common.getKonfigurasi}) belum
 * diisi — namun keberadaannya di kode sumber tetap merupakan kebocoran kredensial nyata: siapa
 * pun dengan akses baca ke repositori (termasuk riwayat SVN) dapat memperoleh private key ini.
 * Bila key tersebut adalah private key produksi sungguhan milik merchant BRI AIS (bukan key
 * contoh dari dokumentasi API), maka pihak lain berpotensi menandatangani permintaan API BRI
 * atas nama merchant ini. Javadoc ini TIDAK mengubah nilai tersebut sesuai instruksi; lihat
 * ringkasan laporan terkait untuk detail lokasi baris agar dapat ditindaklanjuti (mis. rotasi
 * key di sisi BRI dan penghapusan nilai literal ini dari kode sumber, cukup mengandalkan
 * konfigurasi database/secret store).
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
	 * Nilai default/fallback RSA private key format PKCS8/PEM yang dipakai
	 * {@link #readPKCS8PrivateKey()} apabila konfigurasi {@code BRI_PRIVATE_KEY} belum diisi
	 * di database. <b>Kredensial tertanam di kode sumber — lihat peringatan keamanan kritis
	 * pada javadoc kelas.</b>
	 */
	private final static String PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n"
			+ "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKNwapOQ6rQJHetP\n"
			+ "HRlJBIh1OsOsUBiXb3rXXE3xpWAxAha0MH+UPRblOko+5T2JqIb+xKf9Vi3oTM3t\n"
			+ "KvffaOPtzKXZauscjq6NGzA3LgeiMy6q19pvkUUOlGYK6+Xfl+B7Xw6+hBMkQuGE\n"
			+ "nUS8nkpR5mK4ne7djIyfHFfMu4ptAgMBAAECgYA+s0PPtMq1osG9oi4xoxeAGikf\n"
			+ "JB3eMUptP+2DYW7mRibc+ueYKhB9lhcUoKhlQUhL8bUUFVZYakP8xD21thmQqnC4\n"
			+ "f63asad0ycteJMLb3r+z26LHuCyOdPg1pyLk3oQ32lVQHBCYathRMcVznxOG16VK\n"
			+ "I8BFfstJTaJu0lK/wQJBANYFGusBiZsJQ3utrQMVPpKmloO2++4q1v6ZR4puDQHx\n"
			+ "TjLjAIgrkYfwTJBLBRZxec0E7TmuVQ9uJ+wMu/+7zaUCQQDDf2xMnQqYknJoKGq+\n"
			+ "oAnyC66UqWC5xAnQS32mlnJ632JXA0pf9pb1SXAYExB1p9Dfqd3VAwQDwBsDDgP6\n"
			+ "HD8pAkEA0lscNQZC2TaGtKZk2hXkdcH1SKru/g3vWTkRHxfCAznJUaza1fx0wzdG\n"
			+ "GcES1Bdez0tbW4llI5By/skZc2eE3QJAFl6fOskBbGHde3Oce0F+wdZ6XIJhEgCP\n"
			+ "iukIcKZoZQzoiMJUoVRrA5gqnmaYDI5uRRl/y57zt6YksR3KcLUIuQJAd242M/WF\n"
			+ "6YAZat3q/wEeETeQq1wrooew+8lHl05/Nt0cCpV48RGEhJ83pzBm3mnwHf8lTBJH\n" + "x6XroMXsmbnsEw==\n"
			+ "-----END PRIVATE KEY-----";

	/**
	 * Memuat RSA private key yang dipakai untuk menandatangani permintaan API BRI. Nilai PEM
	 * key diambil dari konfigurasi database {@code BRI_PRIVATE_KEY} (lewat
	 * {@code Common.getKonfigurasi}), dengan {@link #PRIVATE_KEY} sebagai nilai default/fallback
	 * bila konfigurasi tersebut belum diisi (lihat peringatan keamanan pada javadoc kelas).
	 * Baris header/footer PEM ({@code -----BEGIN/END PRIVATE KEY-----}) dan seluruh whitespace
	 * dibuang, sisanya di-decode Base64 menjadi bentuk {@link PKCS8EncodedKeySpec} lalu
	 * dikonversi menjadi objek {@link RSAPrivateKey} lewat {@link KeyFactory} algoritma RSA.
	 *
	 * <p>
	 * <b>Catatan:</b> method ini mencetak PEM yang sudah dibersihkan (base64 mentah, yang
	 * sebenarnya SETARA dengan private key itu sendiri) ke {@link System#out} lewat
	 * {@code System.out.println("pkcs8Pem -> " + pkcs8Pem)} — ini berpotensi membocorkan
	 * private key ke berkas log aplikasi bila level log/console ditangkap dan disimpan.
	 * </p>
	 *
	 * @return objek {@link RSAPrivateKey} siap pakai untuk operasi {@link Signature}
	 * @throws Exception diteruskan dari kegagalan decode Base64, parsing PKCS8, atau algoritma
	 *                    RSA tidak tersedia di penyedia keamanan JVM
	 */
	public static RSAPrivateKey readPKCS8PrivateKey() throws Exception {

		StringBuilder pkcs8Lines = new StringBuilder();
		BufferedReader rdr = new BufferedReader(
				new StringReader(Common.getKonfigurasi("BRI_PRIVATE_KEY", PRIVATE_KEY).getNilai()));
		String line;
		while ((line = rdr.readLine()) != null) {
			pkcs8Lines.append(line);
		}

		// Remove the "BEGIN" and "END" lines, as well as any whitespace

		String pkcs8Pem = pkcs8Lines.toString();
		pkcs8Pem = pkcs8Pem.replace("-----BEGIN PRIVATE KEY-----", "");
		pkcs8Pem = pkcs8Pem.replace("-----END PRIVATE KEY-----", "");
		pkcs8Pem = pkcs8Pem.replaceAll("\\s+", "");

		System.out.println("pkcs8Pem -> " + pkcs8Pem);
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
	 * <p>
	 * <b>Catatan:</b> method ini mencetak {@code stringToSign} (bahan mentah sebelum
	 * ditandatangani, berisi client id) ke {@link System#out} untuk keperluan debug — tidak
	 * membocorkan key itu sendiri, namun tetap sebaiknya tidak aktif di lingkungan produksi.
	 * </p>
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

		System.out.println("stringToSign -> " + stringToSign);

		Signature sig = Signature.getInstance("SHA256withRSA");
		sig.initSign(readPKCS8PrivateKey());
		sig.update(stringToSign.getBytes("UTF-8"));
		byte[] signatureBytes = sig.sign();

		String signature = java.util.Base64.getEncoder().encodeToString(signatureBytes);
		return signature;
	}

}
