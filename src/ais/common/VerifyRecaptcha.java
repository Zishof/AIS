package ais.common;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;

/**
 * Klien verifikasi Google reCAPTCHA v2 sisi server: mengirimkan token respons captcha yang
 * dihasilkan browser pengguna ({@code g-recaptcha-response}, biasa disingkat
 * {@code gRecaptchaResponse}) ke endpoint resmi Google {@link #url} beserta secret key aplikasi,
 * lalu menafsirkan field {@code success} pada respons JSON sebagai hasil verifikasi. Dipakai pada
 * alur yang perlu memastikan permintaan berasal dari manusia (bukan bot), misalnya form login/
 * registrasi/lupa password publik di AIS.
 *
 * <p>
 * Implementasi memakai {@link HttpsURLConnection} bawaan JDK secara langsung (bukan pustaka HTTP
 * client pihak ketiga) dengan metode {@code POST}, header {@code User-Agent} tetap ({@link
 * #USER_AGENT}) dan {@code Accept-Language} tetap, serta body berupa
 * {@code application/x-www-form-urlencoded} manual (string {@code "secret=...&response=..."}
 * ditulis langsung ke {@link DataOutputStream}, tanpa encoding tambahan pada nilai
 * {@code gRecaptchaResponse} — dalam praktiknya token reCAPTCHA hanya berisi karakter alfanumerik
 * dan simbol aman-URL sehingga hal ini biasanya tidak menimbulkan masalah, namun tetap merupakan
 * penyusunan query string manual yang rawan salah bila polanya disalin untuk kasus lain). Respons
 * dibaca baris demi baris dari {@link BufferedReader}, digabung menjadi satu string, lalu diparsing
 * sebagai {@link JSONObject}.
 * </p>
 *
 * <p>
 * <b>PERINGATAN KEAMANAN — secret key reCAPTCHA tertanam:</b> nilai secret key yang dikirim sebagai
 * parameter {@code secret} diambil dari konstanta statis {@code ConstantValues.recapchaKey}
 * (didefinisikan di {@code ais.common.ConstantValues}, BUKAN berkas ini), yang nilainya ditulis
 * langsung sebagai literal string di kode sumber ({@code ConstantValues.java} baris 350) alih-alih
 * dibaca dari konfigurasi eksternal/environment variable. Berkas ini (dan berkas
 * {@code ConstantValues.java} tempat literalnya berada) berada di luar cakupan perubahan kode pada
 * pekerjaan dokumentasi ini — literal tersebut TIDAK diubah/dihapus di sini. Setiap kredensial yang
 * sudah pernah ter-commit ke riwayat sumber sebaiknya dianggap berpotensi terekspos dan
 * dipertimbangkan untuk dirotasi lewat panel admin Google reCAPTCHA, terlepas dari apakah berkas ini
 * sendiri diubah.
 * </p>
 *
 * <p>
 * Setiap kegagalan (jaringan, parsing JSON, dsb.) ditangkap dan menghasilkan {@code false} (captcha
 * dianggap TIDAK valid) alih-alih melempar balik pengecualian ke pemanggil, sekaligus dilaporkan
 * lewat {@link Common#tampilErrorJikaAdmin(Exception)} agar admin yang sedang login dapat melihat
 * detail galat di UI (pengguna non-admin tidak melihat apa pun selain hasil verifikasi gagal).
 * </p>
 */
public class VerifyRecaptcha {

	/** URL endpoint resmi Google untuk verifikasi token reCAPTCHA (site verify API). */
	public static final String url = "https://www.google.com/recaptcha/api/siteverify";

	/** Nilai header {@code User-Agent} tetap yang disertakan pada permintaan ke Google. */
	private final static String USER_AGENT = "Mozilla/5.0";

	/**
	 * Memverifikasi token respons reCAPTCHA yang dikirim client ke Google reCAPTCHA site-verify API,
	 * lalu mengembalikan nilai field {@code success} dari respons JSON. Lihat javadoc kelas untuk
	 * detail lengkap protokol permintaan yang dipakai dan peringatan keamanan terkait secret key.
	 *
	 * @param gRecaptchaResponse token {@code g-recaptcha-response} yang dihasilkan widget reCAPTCHA
	 *                           di sisi client; bila {@code null} atau kosong, method langsung
	 *                           mengembalikan {@code false} tanpa memanggil Google sama sekali
	 * @return {@code true} bila Google mengonfirmasi token valid ({@code success=true} pada respons
	 *         JSON); {@code false} bila token kosong/{@code null}, ditolak Google, atau terjadi
	 *         kegagalan apa pun selama proses verifikasi (jaringan, parsing, dsb.)
	 * @throws IOException hanya dapat berasal dari operasi koneksi/stream di luar blok
	 *                      {@code try}/{@code catch} internal method ini; dalam praktiknya seluruh
	 *                      badan method dibungkus {@code try}/{@code catch(Exception)} sehingga
	 *                      pengecualian ini jarang benar-benar menembus ke pemanggil
	 */
	public static boolean verify(String gRecaptchaResponse) throws IOException {
		if (gRecaptchaResponse == null || "".equals(gRecaptchaResponse)) {
			return false;
		}

		try {
			URL obj = new URL(url);
			HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

			// add reuqest header
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", USER_AGENT);
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

			String postParams = "secret=" + ConstantValues.recapchaKey + "&response=" + gRecaptchaResponse;

			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(postParams);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			System.out.println("\nSending 'POST' request to URL : " + url);
			System.out.println("Post parameters : " + postParams);
			System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
			System.out.println(response.toString());

			// parse JSON response and return 'success' value
			JSONObject jsonObject = new JSONObject(response.toString());

			return jsonObject.getBoolean("success");
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			return false;
		}
	}
}