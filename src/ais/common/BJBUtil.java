package ais.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;

import com.google.common.hash.Hashing;

import ais.action.servlet.Bjb;
import ais.database.model.BankHost;

/**
 * Integrasi pembayaran/billing langsung dengan <b>Bank BJB</b> ("bjb langsung") pada AIS, meliputi
 * pembuatan token akses OAuth berbasis JWT yang ditandatangani sendiri (self-signed, HS256),
 * pembuatan tagihan (billing) Virtual Account, dan inquiry status billing — seluruhnya dijalankan
 * lewat proses eksternal {@code curl} (bukan pustaka HTTP client Java) dengan payload dialirkan
 * sebagai argumen {@code --data}.
 *
 * <h2>Alur autentikasi</h2>
 * <p>
 * Berbeda dari OAuth standar yang meminta token dari authorization server memakai client
 * credential biasa, kelas ini <b>membangun sendiri</b> sebuah JWT (header {@code {alg:"HS256",
 * typ:"JWT", kid:...}}, payload berisi {@code sub/aud/iat/exp} dengan masa berlaku ~1 jam),
 * menandatanganinya dengan HMAC-SHA256 memakai {@code client_secret} sebagai kunci (lewat
 * {@link #hmacSha256(String, String)}), lalu mengirim JWT tersebut sebagai body permintaan
 * {@code POST /oauth/client/token} ke endpoint BJB untuk memperoleh {@code dataToken} — token
 * inilah yang kemudian dipakai sebagai {@code Authorization: Bearer} pada permintaan billing/
 * inquiry berikutnya. {@link #dataToken} disimpan sebagai field statis (dicache di memori proses,
 * bukan per-request) dan otomatis diambil ulang lewat {@link #ambilTokenBJB()} bila masih
 * {@code null} atau bila permintaan billing/inquiry gagal dan diminta mengulang
 * ({@code ulang=true}).
 * </p>
 *
 * <p>
 * Setiap permintaan billing/inquiry (selain permintaan token) juga ditandatangani ulang secara
 * terpisah lewat signature HMAC-SHA256 atas string gabungan {@code path=...&method=...&token=
 * ...&timestamp=...&body=...} (header kustom {@code BJB-Timestamp}/{@code BJB-Signature}),
 * memakai pustaka Guava {@link com.google.common.hash.Hashing#hmacSha256(byte[])} — pola
 * signature-per-request yang berbeda dari signature pembuatan token JWT ({@link #hmacSha256})
 * yang memakai {@link javax.crypto.Mac} langsung, walau keduanya memakai algoritma dan kunci
 * ({@code client_secret}) yang sama.
 * </p>
 *
 * <h2>Peringatan keamanan — client secret tertanam sebagai nilai default</h2>
 * <p>
 * <b>Seluruh pembacaan konfigurasi {@code bjb_langsung_client_secret} pada kelas ini
 * (di {@link #inquiryBillingBJB}, {@link #billingBJB}, dan {@link #ambilTokenBJB()}) memberikan
 * nilai default yang merupakan client secret nyata dalam bentuk teks polos:
 * {@code "pf-f1gKNtV58qL9mbojMiILOJ2JGg6OA6YzZ9FSGP9I"}.</b> Nilai default yang sama juga ditanam
 * ulang sebagai variabel lokal {@code originalInput} pada {@link #main(String[])}. Selama
 * konfigurasi runtime {@code bjb_langsung_client_secret} tidak diisi secara eksplisit di
 * database/tabel konfigurasi, sistem akan otomatis memakai secret bawaan ini — yang berarti
 * kredensial produksi terhadap layanan billing Bank BJB berpotensi sudah ter-commit ke riwayat
 * repositori. Selain itu, {@code kid} JWT juga memiliki default tertanam
 * ({@code "7KPDFVEA")}, dan host BJB default berupa alamat IP internal
 * ({@code "http://10.44.224.31:23808"}) yang menyingkap topologi jaringan internal. Dokumentasi
 * ini TIDAK mengubah maupun menghapus nilai-nilai tersebut dari kode — pemilik integrasi WAJIB
 * menilai apakah client secret ini masih aktif di sisi Bank BJB dan, bila iya, segera
 * memindahkannya sepenuhnya ke konfigurasi rahasia runtime (tanpa default tertanam di kode) serta
 * mempertimbangkan rotasi.
 * </p>
 *
 * <p>
 * Kelas ini juga mencetak banyak informasi sensitif ke {@code System.out} untuk keperluan
 * debugging (URL, signature, token akses, payload lengkap perintah {@code curl}) — pada
 * lingkungan produksi, log ini berpotensi menyingkap token/signature yang masih berlaku kepada
 * siapa pun yang memiliki akses baca log aplikasi.
 * </p>
 */
public class BJBUtil {
	/**
	 * Token akses (Bearer) BJB yang sedang berlaku, dicache sebagai field statis di memori proses
	 * setelah berhasil diambil lewat {@link #ambilTokenBJB()}. Tidak ada mekanisme kedaluwarsa
	 * eksplisit di sisi kelas ini — token dianggap tetap valid sampai suatu permintaan gagal dan
	 * dipanggil ulang dengan {@code ulang=true}, yang memicu pengambilan token baru.
	 */
	public static String dataToken = null;

	/**
	 * Melakukan inquiry status billing (tagihan Virtual Account) BJB untuk satu Company
	 * Identification Number (CIN) dan nomor VA tertentu lewat permintaan {@code GET /billing/
	 * {cin}/{va}} yang ditandatangani dan dieksekusi lewat proses {@code curl} eksternal. Bila
	 * respons berisi objek {@code transactions}, hasilnya diteruskan ke
	 * {@link Bjb#doProcess(double, String, String, String, BankHost, Object, String, boolean)}
	 * untuk diproses sebagai pembayaran masuk.
	 *
	 * <p>
	 * Bila {@link #dataToken} belum ada, diambil dulu lewat {@link #ambilTokenBJB()}; bila
	 * pengambilan token tetap gagal, method mengembalikan {@code null} tanpa mencoba memanggil
	 * endpoint billing. Bila terjadi exception saat proses inquiry dan {@code ulang} bernilai
	 * {@code true}, token diambil ulang dan permintaan dicoba sekali lagi secara rekursif dengan
	 * {@code ulang=false} (mencegah pengulangan tak terbatas); bila {@code ulang=false} dan tetap
	 * gagal, exception hanya dicatat (stack trace + {@code ErrorAuditUtil}) tanpa dilempar ulang.
	 * </p>
	 *
	 * @param postData       body permintaan (JSON) yang disertakan pada signature dan permintaan
	 *                       {@code curl}
	 * @param cin            Company Identification Number BJB terkait tagihan
	 * @param va             nomor Virtual Account yang diperiksa
	 * @param bankHostDefault konfigurasi host bank yang diteruskan ke {@link Bjb#doProcess}
	 * @param ulang          bila {@code true}, otomatis mengambil ulang token dan mencoba sekali
	 *                       lagi saat permintaan pertama gagal
	 * @return {@link JSONObject} respons billing BJB bila berhasil; {@code null} bila token tidak
	 *         tersedia; objek {@link JSONObject} kosong/parsial bila terjadi kegagalan yang
	 *         ditangkap secara internal
	 */
	public static JSONObject inquiryBillingBJB(String postData, String cin, String va, BankHost bankHostDefault,
			boolean ulang) {

		if (dataToken == null) {
			ambilTokenBJB();
		}

		if (dataToken == null) {
			return null;
		}

		JSONObject jSONObject = null;
		try {
			String strURL = Common.getKonfigurasi("bjb_langsung_host", "http://10.44.224.31:23808").getNilai()
					+ "/billing/" + cin + "/" + va;
			String currentTimestamp = Instant.now().toEpochMilli() + "";
			currentTimestamp = currentTimestamp.substring(0, currentTimestamp.length() - 3);

			String valueToDigest = "path=/billing/" + cin + "/" + va + "&method=GET&token=" + dataToken + "&timestamp="
					+ currentTimestamp + "&body=" + postData;

			String client_secret = Common
					.getKonfigurasi("bjb_langsung_client_secret", "pf-f1gKNtV58qL9mbojMiILOJ2JGg6OA6YzZ9FSGP9I")
					.getNilai();

			String signature = Hashing.hmacSha256(client_secret.getBytes()).newHasher()
					.putString(valueToDigest, StandardCharsets.UTF_8).hash().toString();

			System.out.println("strURL -> " + strURL);
			System.out.println("valueToDigest -> " + valueToDigest);
			System.out.println("currentTimestamp -> " + currentTimestamp);
			System.out.println("signature -> " + signature);
			System.out.println("postData -> " + postData);
			System.out.println("dataToken -> " + dataToken);

			String[] command = { "curl", "--header", "Content-Type: application/json", "--header",
					"Authorization: Bearer " + dataToken, "--header", "BJB-Timestamp: " + currentTimestamp, "--header",
					"BJB-Signature: " + signature, "--request", "GET", "--data", postData, strURL };

			System.out.println("");
			System.out.println("");

			for (String c : command) {
				System.out.print(c + " ");
			}

			System.out.println("");
			System.out.println("");

			ProcessBuilder process = new ProcessBuilder(command);
			Process p;
			p = process.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String hasil = builder.toString();
			System.out.println(hasil);
			jSONObject = new JSONObject(hasil);
			JSONObject transaction = jSONObject.getJSONObject("transactions");

			String amount = transaction.getString("transaction_amount");
			String bank = "BJB";
			String tanggalP = transaction.getString("transaction_date");

			System.out.println("va -> " + va + " bankHostDefault " + bankHostDefault);

			Bjb.doProcess(Double.parseDouble(amount), tanggalP, va, bank, bankHostDefault, null, jSONObject.toString(),
					true);

			return jSONObject;
		} catch (Exception e) {
			if (ulang) {
				ambilTokenBJB();
				return inquiryBillingBJB(postData, cin, va, bankHostDefault, false);
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BJBUtil.java:101");
		}
		return jSONObject;
	}

	/**
	 * Membuat tagihan (billing) Virtual Account baru di BJB lewat permintaan
	 * {@code POST /billing} yang ditandatangani dan dieksekusi lewat {@code curl} eksternal,
	 * mengikuti pola signature dan penanganan galat/pengulangan yang sama seperti
	 * {@link #inquiryBillingBJB}.
	 *
	 * @param postData body permintaan (JSON) berisi detail tagihan yang hendak dibuat
	 * @param ulang    bila {@code true}, otomatis mengambil ulang token dan mencoba sekali lagi
	 *                 saat permintaan pertama gagal
	 * @return {@link JSONObject} respons BJB (memuat {@code va_number} nomor VA yang dibuat) bila
	 *         berhasil; {@code null} bila token tidak tersedia atau terjadi kegagalan yang tidak
	 *         diulang
	 */
	public static JSONObject billingBJB(String postData, boolean ulang) {

		if (dataToken == null) {
			ambilTokenBJB();
		}

		if (dataToken == null) {
			return null;
		}

		try {
			String strURL = Common.getKonfigurasi("bjb_langsung_host", "http://10.44.224.31:23808").getNilai()
					+ "/billing";
			String currentTimestamp = Instant.now().toEpochMilli() + "";
			currentTimestamp = currentTimestamp.substring(0, currentTimestamp.length() - 3);

			String valueToDigest = "path=/billing&method=POST&token=" + dataToken + "&timestamp=" + currentTimestamp
					+ "&body=" + postData;

			String client_secret = Common
					.getKonfigurasi("bjb_langsung_client_secret", "pf-f1gKNtV58qL9mbojMiILOJ2JGg6OA6YzZ9FSGP9I")
					.getNilai();

			String signature = Hashing.hmacSha256(client_secret.getBytes()).newHasher()
					.putString(valueToDigest, StandardCharsets.UTF_8).hash().toString();

			System.out.println("strURL -> " + strURL);
			System.out.println("valueToDigest -> " + valueToDigest);
			System.out.println("currentTimestamp -> " + currentTimestamp);
			System.out.println("signature -> " + signature);
			System.out.println("postData -> " + postData);
			System.out.println("dataToken -> " + dataToken);

			String[] command = { "curl", "--header", "Content-Type: application/json", "--header",
					"Authorization: Bearer " + dataToken, "--header", "BJB-Timestamp: " + currentTimestamp, "--header",
					"BJB-Signature: " + signature, "--request", "POST", "--data", postData, strURL };

			System.out.println("");
			System.out.println("");

			for (String c : command) {
				System.out.print(c + " ");
			}

			System.out.println("");
			System.out.println("");

			ProcessBuilder process = new ProcessBuilder(command);
			Process p;
			p = process.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String hasil = builder.toString();
			System.out.println(hasil);

			JSONObject jSONObject = new JSONObject(hasil);
			String va = jSONObject.getString("va_number");
			System.out.println("va -> " + va);

			return jSONObject;
		} catch (Exception e) {
			if (ulang) {
				ambilTokenBJB();
				return billingBJB(postData, false);
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BJBUtil.java:176");
		}
		return null;
	}

	/**
	 * Membangun dan meminta token akses OAuth baru dari BJB dengan cara menandatangani sendiri
	 * sebuah JWT (header HS256 + payload {@code sub/aud/iat/exp}, masa berlaku ~1 jam) memakai
	 * {@code client_secret} sebagai kunci HMAC-SHA256 (lewat {@link #hmacSha256(String, String)}),
	 * mengirimkannya sebagai body permintaan {@code POST /oauth/client/token} lewat {@code curl}
	 * eksternal, lalu menyimpan token yang dikembalikan (field {@code data} pada respons JSON) ke
	 * {@link #dataToken}. Lihat catatan keamanan pada Javadoc kelas terkait {@code client_secret}
	 * dan {@code kid} yang memiliki nilai default tertanam di kode.
	 *
	 * <p>
	 * Kegagalan apa pun (pembuatan signature, eksekusi {@code curl}, parsing respons) hanya
	 * dicatat (stack trace + {@code ErrorAuditUtil}) tanpa dilempar ulang — {@link #dataToken}
	 * akan tetap bernilai {@code null} (atau nilai lama sebelumnya bila pemanggilan ini adalah
	 * percobaan ulang) bila terjadi kegagalan.
	 * </p>
	 */
	private static void ambilTokenBJB() {
		try {
			// ambil token

			JSONObject header = new JSONObject();
			header.put("alg", "HS256");
			header.put("typ", "JWT");
			header.put("kid", Common.getKonfigurasi("bjb_langsung_kid", "7KPDFVEA").getNilai());

			String currentTimestamp = Instant.now().toEpochMilli() + "";
			currentTimestamp = currentTimestamp.substring(0, currentTimestamp.length() - 3);
			String after1Hour = Instant.now().plusSeconds(3599).toEpochMilli() + "";
			after1Hour = after1Hour.substring(0, after1Hour.length() - 3);
			JSONObject payload = new JSONObject();
			payload.put("sub", "va-online");
			payload.put("aud", "access-token");
			payload.put("iat", Long.parseLong(currentTimestamp));
			payload.put("exp", Long.parseLong(after1Hour));

			String h = header.toString();
			String payld = payload.toString();

			System.out.println("header -> " + h);
			System.out.println("payload -> " + payld);

			String headerEncode = encode(h.getBytes());
			String payloadEncode = encode(payld.getBytes());

			System.out.println("headerEncode " + headerEncode);
			System.out.println("payloadEncode " + payloadEncode);

			String client_secret = Common
					.getKonfigurasi("bjb_langsung_client_secret", "pf-f1gKNtV58qL9mbojMiILOJ2JGg6OA6YzZ9FSGP9I")
					.getNilai();

			String signature = hmacSha256(headerEncode + "." + payloadEncode, client_secret);

			String encoded = headerEncode + "." + payloadEncode + "." + signature;

			System.out.println("encoded " + encoded);

			String strURL = Common.getKonfigurasi("bjb_langsung_host", "http://10.44.224.31:23808").getNilai()
					+ "/oauth/client/token";

			String[] command = { "curl", "--header", "Content-Type: application/json", "--request", "POST", "--data",
					encoded, strURL };

			System.out.println("");
			System.out.println("");

			for (String c : command) {
				System.out.print(c + " ");
			}

			System.out.println("");
			System.out.println("");

			ProcessBuilder process = new ProcessBuilder(command);
			Process p;
			p = process.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String hasil = builder.toString();
			System.out.println("hasil -> " + hasil);

			JSONObject jSONObject = new JSONObject(hasil);
			dataToken = jSONObject.getString("data");

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BJBUtil.java:255");
		}
	}

	/**
	 * Menghitung signature HMAC-SHA256 atas {@code data} memakai {@code secret} sebagai kunci,
	 * lalu meng-encode hasilnya dengan Base64 URL-safe tanpa padding lewat {@link #encode(byte[])}
	 * — format signature yang lazim dipakai pada JWT ({@code header.payload.signature}).
	 *
	 * @param data   string yang akan ditandatangani (pada pemakaian di kelas ini: gabungan
	 *               header dan payload JWT yang sudah di-encode Base64URL, dipisah titik)
	 * @param secret kunci HMAC (client secret BJB)
	 * @return signature dalam format Base64 URL-safe tanpa padding, atau {@code null} bila terjadi
	 *         kegagalan (mis. algoritma {@code HmacSHA256} tidak tersedia) — kegagalan ditelan
	 *         tanpa dicatat/dilempar ulang
	 */
	private static String hmacSha256(String data, String secret) {
		try {

			// MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = secret.getBytes(StandardCharsets.UTF_8);// digest.digest(secret.getBytes(StandardCharsets.UTF_8));

			Mac sha256Hmac = Mac.getInstance("HmacSHA256");
			SecretKeySpec secretKey = new SecretKeySpec(hash, "HmacSHA256");
			sha256Hmac.init(secretKey);

			byte[] signedBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

			return encode(signedBytes);
		} catch (Exception e) {

			return null;
		}
	}

	/**
	 * Meng-encode larik byte ke string Base64 URL-safe tanpa padding ({@code '='}), sesuai
	 * kebutuhan encoding segmen JWT (RFC 7515).
	 *
	 * @param bytes data mentah yang akan di-encode
	 * @return representasi Base64 URL-safe tanpa padding dari {@code bytes}
	 */
	private static String encode(byte[] bytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * Titik masuk baris perintah untuk menguji manual pembuatan JWT dan signature HMAC-SHA256
	 * yang dipakai {@link #ambilTokenBJB()}, tanpa benar-benar mengirim permintaan ke endpoint
	 * BJB — hanya mencetak header, payload, dan signature yang dihasilkan ke konsol.
	 *
	 * <p>
	 * <b>Catatan keamanan:</b> variabel lokal {@code originalInput} pada method ini menanam ulang
	 * client secret BJB yang sama seperti dijelaskan pada Javadoc kelas
	 * ({@code "pf-f1gKNtV58qL9mbojMiILOJ2JGg6OA6YzZ9FSGP9I"}), begitu pula {@code kid} JWT
	 * ({@code "7KPDFVEA"}).
	 * </p>
	 *
	 * @param argv argumen baris perintah (tidak dipakai)
	 * @throws Exception tidak pernah benar-benar dilempar pada alur ini; dideklarasikan mengikuti
	 *                    signature program mandiri
	 */
	public static void main(String[] argv) throws Exception {

		JSONObject header = new JSONObject();
		header.put("alg", "HS256");
		header.put("typ", "JWT");
		header.put("kid", "7KPDFVEA");

		String currentTimestamp = Instant.now().toEpochMilli() + "";
		currentTimestamp = currentTimestamp.substring(0, currentTimestamp.length() - 3);
		String after1Hour = Instant.now().plusSeconds(3599).toEpochMilli() + "";
		after1Hour = after1Hour.substring(0, after1Hour.length() - 3);
		JSONObject payload = new JSONObject();
		payload.put("sub", "va-online");
		payload.put("aud", "access-token");
		payload.put("iat", Long.parseLong(currentTimestamp));
		payload.put("exp", Long.parseLong(after1Hour));

		String h = header.toString();
		String payld = payload.toString();

		System.out.println("header " + h);
		System.out.println("payload " + payld);

		String headerEncode = encode(h.getBytes());
		String payloadEncode = encode(payld.getBytes());

		System.out.println("headerEncode " + headerEncode);
		System.out.println("payloadEncode " + payloadEncode);

		String originalInput = "pf-f1gKNtV58qL9mbojMiILOJ2JGg6OA6YzZ9FSGP9I";

		String signature = hmacSha256(headerEncode + "." + payloadEncode, originalInput);

		System.out.println("signature " + signature);
	}
}
