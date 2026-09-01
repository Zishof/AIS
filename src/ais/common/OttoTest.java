package ais.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import com.google.common.hash.Hashing;

//import org.json.JSONObject;

/**
 * Potongan kode uji-coba mandiri (bukan bagian dari alur transaksi aplikasi AIS) yang
 * mendemonstrasikan cara membangun permintaan token pembayaran ke gateway <b>OttoPay</b>
 * (endpoint lingkungan pengembangan {@code https://dev-secure.ottopay.id}), meliputi penyusunan
 * payload JSON transaksi, pembuatan tanda tangan HMAC-SHA512, encoding Basic Authorization
 * berbasis Base64 dari Merchant ID, dan eksekusi permintaan HTTP POST lewat proses eksternal
 * {@code curl} (bukan lewat pustaka HTTP client Java).
 *
 * <p>
 * Nama kelas ("OttoTest") dan gaya penulisannya — konstanta {@code apiKey}/{@code MID} tertanam
 * langsung, banyak blok komentar berisi contoh payload JSON alternatif yang dinonaktifkan,
 * timestamp yang dipaksa ({@code currentTimestamp = "1540383020"}) alih-alih memakai waktu
 * berjalan — menunjukkan kelas ini adalah skrip eksplorasi/pengujian integrasi yang dipakai saat
 * mengembangkan dukungan pembayaran lewat OttoPay, dijalankan manual sebagai program {@code main}
 * ketimbang dipanggil oleh kode aplikasi produksi.
 * </p>
 *
 * <h2>Alur pembuatan tanda tangan (signature)</h2>
 * <p>
 * Signature dihitung sebagai HMAC-SHA512 (kunci = {@code apiKey}) atas string gabungan
 * {@code body_json.trim().toLowerCase() + "&" + timestamp + "&" + apiKey}, memakai pustaka Guava
 * ({@link com.google.common.hash.Hashing#hmacSha512(byte[])}). Header {@code Authorization}
 * memakai skema Basic dengan Merchant ID (bukan pasangan username:password) yang di-encode
 * Base64. Permintaan dikirim lewat {@link ProcessBuilder} yang menjalankan {@code curl} sebagai
 * proses child dan membaca stdout-nya baris demi baris — pola yang sama seperti dipakai mesin
 * produksi push notification ({@code ais.delivery.email.sender.MailSender#kirimNotif}) untuk
 * menghindari batas panjang argumen command-line pada payload besar, walau di sini bodinya
 * relatif pendek.
 * </p>
 *
 * <h2>Riwayat keamanan (DIPERBAIKI 2026-09-01) — kredensial payment gateway sebelumnya tertanam di
 * kode sumber</h2>
 * <p>
 * Kelas ini sebelumnya menanam langsung dua rahasia integrasi OttoPay sebagai literal string di
 * kode sumber: {@code apiKey} (dipakai sebagai kunci HMAC penanda tangan permintaan) dan
 * {@code MID}/Merchant ID (dipakai untuk header Authorization Basic). Karena kelas ini
 * terverifikasi tidak dipanggil oleh kode aplikasi lain (hanya dapat dijalankan manual lewat
 * {@code main}), kedua nilai itu kini diambil dari system property (
 * {@code -Dottotest.apikey=...}, {@code -Dottotest.mid=...}) alih-alih tertanam di kode, dengan
 * {@link #main} langsung berhenti dan menampilkan petunjuk pemakaian bila salah satu belum diisi.
 * Baris {@code System.out.println} yang sebelumnya mencetak nilai {@code apiKey}, signature, MID,
 * dan MID hasil encode Base64 juga sudah dihapus, karena berpotensi membocorkan bahan autentikasi
 * lewat log konsol.
 * </p>
 *
 * <p>
 * <b>TINDAK LANJUT DI LUAR PERUBAHAN KODE INI</b>: nilai {@code apiKey}/{@code MID} yang
 * sebelumnya tertanam ({@code "KP33PP0EE0AAP1EE1009010PP01I91OA"} dan {@code "OP1E00030999"})
 * sudah lama berada di riwayat SVN dan WAJIB dianggap bocor — perlu dirotasi di sisi OttoPay bila
 * masih aktif, terlepas dari endpoint yang dituju ({@code dev-secure.ottopay.id}) tampak sebagai
 * lingkungan sandbox/pengembangan.
 * </p>
 */
public class OttoTest {

	/**
	 * Menjalankan satu siklus percobaan permintaan token pembayaran OttoPay: membaca
	 * {@code apiKey}/{@code MID} dari system property (berhenti dengan pesan bila belum diisi),
	 * menyusun payload JSON transaksi contoh, menghitung timestamp dan signature HMAC-SHA512,
	 * meng-encode Merchant ID ke Basic Authorization, lalu mengeksekusi permintaan lewat proses
	 * {@code curl} eksternal dan mencetak hasil respons ke konsol untuk keperluan debugging manual.
	 *
	 * @param t argumen baris perintah (tidak dipakai)
	 * @throws Exception diteruskan apa adanya dari kegagalan proses {@code curl} atau pembacaan
	 *                    stream keluarannya
	 */
	public static void main(String[] t) throws Exception {

//		JSONObject body = new JSONObject();
//		JSONObject customerDetails = new JSONObject();
//		customerDetails.put("email", "jihan.nabilah@ottodigital.id");
//		customerDetails.put("firstName", "Mohammad Fauzi");
//		customerDetails.put("lastName", "Murtadho");
//		customerDetails.put("phone", "6281382028582");
//
//		body.put("customerDetails", customerDetails);
//
//		JSONObject transactionDetails = new JSONObject();
//		transactionDetails.put("amount", 10000);
//		transactionDetails.put("currency", "IDR");
//		transactionDetails.put("merchantName", "Merchant");
//		transactionDetails.put("orderId", "ORDORD1010103455");
//		transactionDetails.put("vaOrderId", "");
//		transactionDetails.put("promoCode", "");
//		transactionDetails.put("vabca", "");
//		transactionDetails.put("valain", "");
//		transactionDetails.put("vamandiri", "");

//		body.put("transactionDetails", transactionDetails);

		String apiKey = System.getProperty("ottotest.apikey", "");
		String MID = System.getProperty("ottotest.mid", "");
		if (apiKey.trim().isEmpty() || MID.trim().isEmpty()) {
			System.out.println("Jalankan dengan -Dottotest.apikey=... -Dottotest.mid=...");
			return;
		}

		String currentTimestamp = Instant.now().toEpochMilli() + "";
		currentTimestamp = currentTimestamp.substring(0, currentTimestamp.length() - 3);

		currentTimestamp = "1540383020";

		String bodyS = "{\"customerDetails\":{\"email\":\"jihan.nabilah@ottodigital.id\",\"firstName\":\"Mohammad Fauzi\",\"lastName\":\"Murtadho\",\"phone\":\"6281382028582\"},\"transactionDetails\":{\"amount\":10000,\"currency\":\"IDR\",\"merchantName\":\"Uninus\",\"orderId\":\"ORD10a01a0\",\"vaOrderId\":\"\"}}";

		String valueToDigest = bodyS.trim().toLowerCase() + "&" + currentTimestamp + "&" + apiKey;

		String signature = Hashing.hmacSha512(apiKey.getBytes()).newHasher()
				.putString(valueToDigest, StandardCharsets.UTF_8).hash().toString();

		String encodedMID = Base64.getEncoder().encodeToString(MID.getBytes());

		String url = "https://dev-secure.ottopay.id/payment-services/v2.1.0/api/token";

		String[] command = { "curl", "--location", "--request", "POST", url, "--header", "Signature: " + signature,
				"--header", "Timestamp: " + currentTimestamp, "--header", "Authorization: Basic " + encodedMID,
				"--header", "Content-Type: application/json", "--data-raw", bodyS };
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

	}

}
