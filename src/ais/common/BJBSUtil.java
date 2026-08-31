package ais.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.json.JSONObject;

import com.google.common.hash.Hashing;

import ais.action.servlet.Bjb;
import ais.database.model.BankHost;

/**
 * Kelas utilitas statis untuk integrasi dengan layanan <b>payment gateway/host-to-host BJB
 * Syariah</b> (nama kelas {@code BJBSUtil} = "BJB Syariah Util") — Bank Jabar Banten Syariah —
 * meliputi otentikasi (login token), pembuatan kode Virtual Account (VA) sementara, pembuatan
 * produk billing, dan pengecekan status tagihan (inquiry billing) untuk pembayaran biaya
 * akademik/lainnya di AIS.
 *
 * <p>
 * Seluruh panggilan HTTP ke layanan BJB Syariah dilakukan dengan cara yang tidak lazim: alih-alih
 * memakai pustaka HTTP client Java (mis. {@code HttpURLConnection}/Apache HttpClient), kelas ini
 * membangun perintah <b>{@code curl} eksternal</b> lewat {@link ProcessBuilder}, menjalankannya
 * sebagai proses OS terpisah, dan membaca stdout proses tersebut sebagai respons JSON. Pola yang
 * sama diulang pada keempat method publik/privat di kelas ini: {@link #ambilTokenBJB()} (login),
 * {@link #buatProdukBJB(String, String)} (buat produk), {@link #billingBJB(String, boolean)}
 * (buat VA sementara), dan {@link #inquiryBillingBJB(String, String, String, BankHost, boolean)}
 * (cek status tagihan, ditandatangani HMAC-SHA256).
 * </p>
 *
 * <h2>Manajemen token</h2>
 * <p>
 * Token otentikasi disimpan pada field statis {@link #dataToken}, dibagikan lintas seluruh
 * pemanggilan pada satu JVM (bukan per-request/per-user). Setiap method publik memeriksa apakah
 * {@link #dataToken} masih {@code null} dan bila perlu memanggil {@link #ambilTokenBJB()} untuk
 * login ulang. Token TIDAK memiliki mekanisme kedaluwarsa proaktif di sisi kelas ini — token lama
 * terus dipakai sampai suatu panggilan API gagal (masuk blok {@code catch}), lalu, bila parameter
 * {@code ulang} bernilai {@code true}, method mengambil token baru dan mencoba ulang PERSIS SATU
 * KALI secara rekursif dengan {@code ulang=false} (mencegah rekursi tak terbatas bila kegagalan
 * berulang).
 * </p>
 *
 * <h2 style="color:red">PERINGATAN KEAMANAN — kredensial tertanam di kode</h2>
 * <p>
 * Beberapa pemanggilan {@code Common.getKonfigurasi(key, defaultValue)} pada kelas ini menyertakan
 * kredensial produksi sebagai NILAI DEFAULT yang tertulis langsung sebagai literal string di kode
 * sumber (bukan hanya nama konfigurasi) — nilai-nilai tersebut dipakai APABILA baris konfigurasi
 * dengan key bersangkutan belum ada di database, namun tetap berarti kredensial nyata sudah
 * ter-commit ke source control:
 * </p>
 * <ul>
 * <li>{@link #ambilTokenBJB()}: {@code Common.getKonfigurasi("bjbs_username", "unb004")} dan
 * {@code Common.getKonfigurasi("bjbs_password", "004unb")} — username dan password default akun
 * BJBS tertanam sebagai literal.</li>
 * <li>{@link #inquiryBillingBJB(String, String, String, BankHost, boolean)}: {@code
 * Common.getKonfigurasi("bjb_langsung_client_secret", "pf-f1gKNtV58qL9mbojMiILOJ2JGg6OA6YzZ9FSGP9I")}
 * — client secret dipakai sebagai kunci HMAC-SHA256 untuk menandatangani setiap request inquiry.</li>
 * <li>Host default layanan juga tertanam sebagai literal (bukan rahasia, namun tetap
 * mengekspos topologi jaringan internal): {@code "bjbs_host"} default
 * {@code "http://183.91.79.70:3002"} (dipakai {@link #ambilTokenBJB()},
 * {@link #buatProdukBJB(String, String)}, {@link #billingBJB(String, boolean)}) dan
 * {@code "bjb_langsung_host"} default {@code "http://10.44.224.31:23808"} (IP privat/internal,
 * dipakai {@link #inquiryBillingBJB(String, String, String, BankHost, boolean)}).</li>
 * </ul>
 * <p>
 * Sesuai instruksi tugas dokumentasi ini, kredensial dan endpoint tersebut TIDAK diubah/dihapus di
 * sini — laporan detail lokasinya disertakan pada ringkasan akhir pekerjaan dokumentasi ini agar
 * dapat ditindaklanjuti secara terpisah (idealnya: pindahkan ke konfigurasi rahasia yang tidak
 * ikut ter-commit, dan pertimbangkan rotasi kredensial karena sudah pernah terekspos di riwayat
 * source control).
 * </p>
 *
 * <p>
 * Catatan tambahan: seluruh method di kelas ini mencetak {@code postData}, token, tanda tangan,
 * dan seluruh argumen {@code curl} (termasuk header {@code Authorization: Bearer <token>}) ke
 * {@link System#out} sebagai log debug — pada lingkungan yang log-nya tidak dikendalikan ketat,
 * ini juga berpotensi membocorkan token aktif ke berkas log.
 * </p>
 */
public class BJBSUtil {
	/**
	 * Token bearer hasil login ke layanan BJB Syariah (diisi oleh {@link #ambilTokenBJB()}),
	 * dibagikan statis lintas seluruh pemanggilan pada JVM ini. {@code null} berarti belum pernah
	 * login atau login terakhir gagal.
	 */
	public static String dataToken = null;

	/**
	 * Melakukan inquiry (pengecekan) status tagihan Virtual Account BJB lewat endpoint
	 * {@code bjb_langsung_host}, dengan request ditandatangani HMAC-SHA256 (header
	 * {@code BJB-Signature}) memakai client secret dari konfigurasi {@code bjb_langsung_client_secret}
	 * (lihat peringatan keamanan pada Javadoc kelas mengenai nilai default kredensial ini). Bila
	 * inquiry berhasil dan responsnya memuat data transaksi, method ini juga memicu pemrosesan
	 * pembayaran lewat {@link Bjb#doProcess(double, String, String, String, BankHost, Object,
	 * String, boolean)}.
	 *
	 * @param postData       payload JSON yang disisipkan ke tanda tangan permintaan dan dikirim
	 *                       sebagai data pada permintaan {@code curl} (parameter {@code --data})
	 * @param cin            identifier pelanggan (customer identification number) BJB
	 * @param va             nomor Virtual Account yang diperiksa
	 * @param bankHostDefault konfigurasi host bank yang diteruskan ke {@link Bjb#doProcess} untuk
	 *                        pemrosesan pembayaran lanjutan
	 * @param ulang          bila {@code true} dan permintaan gagal, token diambil ulang lewat
	 *                       {@link #ambilTokenBJB()} dan method mencoba ulang tepat satu kali
	 *                       (dipanggil rekursif dengan {@code ulang=false})
	 * @return objek JSON respons layanan BJB, atau {@code null} bila token tidak tersedia atau
	 *         seluruh percobaan (termasuk percobaan ulang) gagal
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BJBSUtil.java:97");
		}
		return jSONObject;
	}

	/**
	 * Membuat kode Virtual Account (VA) sementara baru lewat endpoint {@code bjbs_host}
	 * {@code /api/va-temporary}, mengirim {@code postData} sebagai payload permintaan POST.
	 *
	 * @param postData payload JSON permintaan pembuatan VA (mis. jumlah tagihan, identitas
	 *                 pelanggan) sesuai kontrak API BJB Syariah
	 * @param ulang    bila {@code true} dan permintaan gagal, token diambil ulang lewat
	 *                 {@link #ambilTokenBJB()} dan method mencoba ulang tepat satu kali
	 * @return objek JSON respons layanan (berisi data VA yang dibuat), atau {@code null} bila
	 *         token tidak tersedia atau permintaan gagal setelah percobaan ulang (bila ada)
	 */
	public static JSONObject billingBJB(String postData, boolean ulang) {

		if (dataToken == null) {
			ambilTokenBJB();
		}

		if (dataToken == null) {
			return null;
		}

		try {
			String strURL = Common.getKonfigurasi("bjbs_host", "http://183.91.79.70:3002").getNilai()
					+ "/api/va-temporary";

			System.out.println("strURL -> " + strURL);
			System.out.println("postData -> " + postData);
			System.out.println("dataToken -> " + dataToken);

			String[] command = { "curl", "--header", "Content-Type: application/json", "--header",
					"Authorization: Bearer " + dataToken, "--header", "--request", "POST", "--data", postData, strURL };

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
			JSONObject data = jSONObject.getJSONObject("data");
			System.out.println("data -> " + data);

			return jSONObject;
		} catch (Exception e) {
			if (ulang) {
				ambilTokenBJB();
				return billingBJB(postData, false);
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BJBSUtil.java:156");
		}
		return null;
	}

	/**
	 * Mendaftarkan sebuah "produk" billing baru ke layanan BJB Syariah lewat endpoint
	 * {@code bjbs_host} {@code /api/product}, dengan masa berlaku tetap 24 jam
	 * ({@code 24 * 60 * 60000} milidetik disisipkan sebagai field {@code expired} pada payload —
	 * perhatikan bahwa perhitungan ini tidak dikalikan 1000 untuk mengonversi detik ke milidetik
	 * secara konsisten, sehingga nilai literalnya kemungkinan bukan representasi 24 jam yang benar
	 * dalam milidetik; logika ini TIDAK diubah sebagai bagian dari dokumentasi ini).
	 *
	 * @param product_name nama produk yang didaftarkan
	 * @param user_create  identitas pengguna/sistem pembuat produk, dikirim sebagai field
	 *                     {@code user_create} pada payload
	 * @return id produk ({@code product_id}) yang dikembalikan layanan BJB, {@code 0} bila token
	 *         tidak tersedia (return awal sebelum percobaan), atau {@code null} bila token tidak
	 *         tersedia pada pemeriksaan kedua di dalam method; {@code 0} juga menjadi nilai
	 *         terakhir yang tersimpan bila terjadi galat sebelum respons berhasil diparsing
	 */
	public static Integer buatProdukBJB(String product_name, String user_create) {
		Integer product_id = 0;
		try {
			// ambil token

			if (dataToken == null) {
				ambilTokenBJB();
			}

			if (dataToken == null) {
				return null;
			}

			JSONObject payload = new JSONObject();
			payload.put("product_name", product_name);
			payload.put("user_create", user_create);
			payload.put("expired", 24 * 60 * 60000);

			String strURL = Common.getKonfigurasi("bjbs_host", "http://183.91.79.70:3002").getNilai() + "/api/product";

			String[] command = { "curl", "--header", "Content-Type: application/json", "--header",
					"Authorization: Bearer " + dataToken, "--request", "POST", "--data", payload.toString(), strURL };

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
			product_id = jSONObject.getJSONObject("data").getInt("product_id");

			System.out.println("product_id -> " + product_id);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BJBSUtil.java:213");
		}

		return product_id;
	}

	/**
	 * Melakukan login (sign-in) ke endpoint {@code bjbs_host} {@code /api/auth/sign-in} memakai
	 * kredensial dari konfigurasi {@code bjbs_username}/{@code bjbs_password}, dan menyimpan token
	 * hasil login ke field statis {@link #dataToken} untuk dipakai method-method lain di kelas ini.
	 *
	 * <p>
	 * <b>PERINGATAN KEAMANAN:</b> pemanggilan {@code Common.getKonfigurasi} pada method ini
	 * menyertakan username ({@code "unb004"}) dan password ({@code "004unb"}) BJB Syariah sebagai
	 * nilai default literal tertanam di kode sumber — lihat penjelasan lengkap pada Javadoc kelas
	 * {@link BJBSUtil}.
	 * </p>
	 *
	 * <p>
	 * Kegagalan (mis. galat jaringan, kredensial ditolak, respons tidak mengandung field
	 * {@code data.token}) ditangkap dan dicatat ke {@link ais.common.ErrorAuditUtil} tanpa
	 * dilempar ulang; dalam kondisi ini {@link #dataToken} tetap bernilai {@code null} (atau nilai
	 * lama sebelumnya bila pemanggilan ini adalah percobaan ulang) dan pemanggil bertanggung jawab
	 * memeriksa hal tersebut.
	 * </p>
	 */
	private static void ambilTokenBJB() {
		try {
			// ambil token

			JSONObject payload = new JSONObject();
			payload.put("username", Common.getKonfigurasi("bjbs_username", "unb004").getNilai());
			payload.put("password", Common.getKonfigurasi("bjbs_password", "004unb").getNilai());

			String strURL = Common.getKonfigurasi("bjbs_host", "http://183.91.79.70:3002").getNilai()
					+ "/api/auth/sign-in";

			String[] command = { "curl", "--header", "Content-Type: application/json", "--request", "POST", "--data",
					payload.toString(), strURL };

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
			dataToken = jSONObject.getJSONObject("data").getString("token");

			System.out.println("dataToken -> " + dataToken);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BJBSUtil.java:262");
		}
	}

}
