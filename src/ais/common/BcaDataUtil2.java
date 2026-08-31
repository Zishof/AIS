package ais.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONObject;

import com.google.common.hash.Hashing;

import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;

//import org.json.JSONObject;

/**
 * Utilitas integrasi pembuatan <b>Virtual Account (VA)</b> lewat API SNAP (Standar Nasional Open
 * API Pembayaran, standar Bank Indonesia untuk API perbankan) milik <b>Bank BRI</b> (Bank Rakyat
 * Indonesia), dipakai untuk membuatkan nomor VA tagihan bagi mahasiswa, siswa, dan calon
 * mahasiswa/siswa di AIS.
 *
 * <p>
 * <b>Catatan penamaan (didokumentasikan apa adanya, bukan diubah)</b> — nama kelas ini
 * ({@code BcaDataUtil2}, merujuk BCA/Bank Central Asia) TIDAK sesuai dengan bank yang sebenarnya
 * diintegrasikan: seluruh kode di dalamnya (endpoint {@code sandbox.partner.api.bri.co.id}, kunci
 * konfigurasi berawalan {@code BRI_*}) memanggil API SNAP milik <b>BRI</b>, bukan BCA. Kemungkinan
 * kelas ini semula direncanakan/disalin dari integrasi BCA lalu diisi ulang untuk BRI tanpa
 * mengganti nama kelasnya.
 * </p>
 *
 * <h2>Alur kerja</h2>
 * <p>
 * Empat overload {@code post} ({@link #post(Mahasiswa, int, String, VirtualAccountBank)},
 * {@link #post(BiodataCalonMahasiswa, int, String, VirtualAccountBank)},
 * {@link #post(Siswa, int, String, VirtualAccountBank)},
 * {@link #post(CalonSiswa, int, String, VirtualAccountBank)}) hanya berbeda pada tipe entitas
 * pemilik tagihan (dipakai untuk membentuk {@code virtualAccountName} dari nama+NIM/no
 * registrasi/NISN entitas tersebut); keempatnya menyusun payload JSON permintaan pembuatan VA
 * yang identik strukturnya (partnerServiceId, customerNo, virtualAccountNo, virtualAccountName,
 * totalAmount, expiredDate [besok, pukul yang sama, zona +07:00 WIB], trxId, additionalInfo) lalu
 * mendelegasikan pengiriman aktual ke {@link #doPost(String, VirtualAccountBank)}.
 * </p>
 * <p>
 * {@link #doPost(String, VirtualAccountBank)} menjalankan DUA panggilan SNAP BRI berurutan lewat
 * proses {@code curl} eksternal ({@link ProcessBuilder}, bukan pustaka HTTP Java): (1) meminta
 * access token B2B (client-credentials) ke endpoint {@code /snap/v1.0/access-token/b2b} dengan
 * tanda tangan HMAC yang dibangkitkan {@link BRIUtil#generateSignatureToken(String, String)}; (2)
 * memakai token tersebut untuk memanggil {@code /snap/v1.0/transfer-va/create-va} dengan header
 * {@code X-SIGNATURE} berupa HMAC-SHA512 (dari {@link Hashing#hmacSha512(byte[])}) atas payload
 * {@code method:path:token:sha256(body):timestamp}, sesuai spesifikasi keamanan standar SNAP.
 * Nomor VA sendiri dibentuk dari {@code srv} (kode layanan BRI) digabung nomor pelanggan acak 11
 * digit, di-<i>pad</i> kiri dengan spasi lalu dipotong ke panjang tetap (8 digit untuk
 * {@code partnerServiceId}, 19 digit untuk {@code virtualAccountNo}) — pola padding manual, bukan
 * {@code String.format}.
 * </p>
 *
 * <h2>Peringatan keamanan — kredensial API BRI tertanam sebagai nilai default</h2>
 * <p>
 * Seluruh parameter integrasi BRI dibaca lewat {@link Common#getKonfigurasi(String, String)} —
 * yang berarti nilai runtime SEHARUSNYA berasal dari database konfigurasi — namun setiap
 * pemanggilan disertai nilai default yang tertanam langsung di kode (lihat
 * {@link #doPost(String, VirtualAccountBank)} dan seluruh overload {@code post}):
 * </p>
 * <ul>
 * <li>{@code BRI_CLIENT_ID} default {@code "WAVmwxO0EXUJyW4SDiY4ydUAe3gUvQYD"} — berbentuk string
 * acak khas client id API partner, bukan placeholder.</li>
 * <li>{@code BRI_CLIENT_SECRET} default {@code "IGa7p9oeRJUhfdVR"} — berbentuk string acak khas
 * client secret, bukan placeholder.</li>
 * <li>{@code BRI_SRV_ID} default {@code "23212"}, {@code BRI_PARTNER_ID} default
 * {@code "ECAMPUS"}, {@code BRI_EXTERNAL_ID} default {@code "1262222"}, {@code BRI_CHANNEL_ID}
 * default {@code "12345"} — identitas partner/kanal tambahan yang menyertai kredensial di atas.</li>
 * </ul>
 * <p>
 * Endpoint yang dipanggil ({@code sandbox.partner.api.bri.co.id}) adalah domain SANDBOX BRI,
 * sehingga risiko kebocoran kredensial ini kemungkinan bersifat lingkungan uji, bukan produksi
 * langsung — namun client id/secret tetap merupakan kredensial otentikasi nyata yang tertanam
 * permanen di kode sumber dan sebaiknya ditinjau/dirotasi tanpa bergantung pada asumsi
 * "hanya sandbox". Sesuai cakupan pekerjaan dokumentasi ini, nilai-nilai tersebut TIDAK diubah.
 * </p>
 *
 * <h2>Peringatan keamanan tambahan — kredensial tercetak ke log/konsol dan argumen proses</h2>
 * <p>
 * {@link #doPost(String, VirtualAccountBank)} mencetak {@code clientSecret} dalam bentuk TEKS
 * POLOS ke {@code System.out} (baris {@code System.out.println("client_secret " + clientSecret)})
 * serta mencetak {@code dataToken} (access token B2B) dan payload tanda tangan HMAC ke konsol
 * juga — bila log aplikasi tidak diamankan/dibatasi aksesnya, kredensial ini dapat bocor lewat
 * berkas log. Client id, client secret (tersirat lewat header), signature, dan access token juga
 * diteruskan sebagai ARGUMEN BARIS PERINTAH ke proses {@code curl} eksternal lewat
 * {@link ProcessBuilder} — pada banyak sistem operasi, argumen proses dapat terlihat oleh proses
 * lain di mesin yang sama (mis. lewat {@code ps aux} di Linux), sehingga ini adalah jalur
 * kebocoran kredensial tambahan di luar pencatatan log. Observasi ini dicatat sebagai temuan
 * keamanan tanpa mengubah kode.
 * </p>
 */
public class BcaDataUtil2 {

	/** Format tanggal ISO-8601 dengan milidetik ({@code yyyy-MM-dd'T'HH:mm:ss.SSS}), dipakai untuk {@code X-TIMESTAMP} permintaan token. Dibuat {@link ThreadLocal} karena {@link SimpleDateFormat} tidak thread-safe. */
	public static final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		}
	};
	/** Format tanggal ISO-8601 tanpa milidetik ({@code yyyy-MM-dd'T'HH:mm:ss}), dipakai untuk {@code expiredDate} VA. Dibuat {@link ThreadLocal} karena {@link SimpleDateFormat} tidak thread-safe. */
	public static final ThreadLocal<DateFormat> dateFormat1 = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		}
	};

	/**
	 * Membuat Virtual Account BRI untuk tagihan seorang {@link Mahasiswa}. Nama VA dibentuk dari
	 * nama mahasiswa dan NIM-nya. Lihat javadoc kelas untuk alur lengkap pembuatan VA dan
	 * peringatan keamanan terkait kredensial BRI.
	 *
	 * @param mahasiswa                 pemilik tagihan; nama dan NIM-nya dipakai sebagai
	 *                                   {@code virtualAccountName}
	 * @param nominal                   nominal tagihan (rupiah, tanpa desimal — dikirim sebagai
	 *                                   {@code nominal + ".00"})
	 * @param keterangan                deskripsi tagihan (dipotong maksimum 40 karakter)
	 * @param virtualAccountBankOnline  entitas VA yang akan diisi dengan request/response mentah
	 *                                   dan tanggal kedaluwarsa hasil panggilan ini
	 * @return respons JSON mentah dari API SNAP BRI (hasil pembuatan VA)
	 * @throws Exception diteruskan dari kegagalan proses {@code curl}/parsing JSON di
	 *                    {@link #doPost(String, VirtualAccountBank)}
	 */
	public static JSONObject post(Mahasiswa mahasiswa, int nominal, String keterangan,
			VirtualAccountBank virtualAccountBankOnline) throws Exception {

		String srv = Common.getKonfigurasi("BRI_SRV_ID", "23212").getNilai();

		String partnerServiceId = "              " + srv;

		partnerServiceId = partnerServiceId.substring(partnerServiceId.length() - 8);

		String customerNo = Common.getGeneratedAngkaDigit(11);
		String virtualAccountNo = "                   " + srv + customerNo;
		virtualAccountNo = virtualAccountNo.substring(virtualAccountNo.length() - 19);
		String virtualAccountName = mahasiswa.getNama() + " " + mahasiswa.getNim();
		Calendar expired = Calendar.getInstance();
		expired.set(Calendar.DATE, expired.get(Calendar.DATE) + 1);
		String expiredTimestamp = dateFormat1.get().format(expired.getTime()) + "+07:00";

		String trxId = Common.getGeneratedBarCode();

		JSONObject totalAmount = new JSONObject();
		totalAmount.put("value", nominal + ".00");
		totalAmount.put("currency", "IDR");

		JSONObject additionalInfo = new JSONObject();
		additionalInfo.put("description", Common.maxPanjang(keterangan, 40));

		JSONObject jsonObjectReq = new JSONObject();

		jsonObjectReq.put("partnerServiceId", partnerServiceId);
		jsonObjectReq.put("customerNo", customerNo);
		jsonObjectReq.put("virtualAccountNo", virtualAccountNo);
		jsonObjectReq.put("virtualAccountName", Common.maxPanjang(virtualAccountName, 255));
		jsonObjectReq.put("totalAmount", totalAmount);
		jsonObjectReq.put("expiredDate", expiredTimestamp);
		jsonObjectReq.put("trxId", trxId);
		jsonObjectReq.put("additionalInfo", additionalInfo);

		String postData = jsonObjectReq.toString();
		virtualAccountBankOnline.setKadaluarsa(expired.getTime()); 

		return doPost(postData, virtualAccountBankOnline);
	}

	/**
	 * Seperti {@link #post(Mahasiswa, int, String, VirtualAccountBank)}, untuk tagihan seorang
	 * calon mahasiswa ({@link BiodataCalonMahasiswa}); nama VA dibentuk dari nama dan nomor
	 * registrasi calon mahasiswa tersebut.
	 *
	 * @param biodataCalonMahasiswa     pemilik tagihan
	 * @param nominal                   nominal tagihan (rupiah, tanpa desimal)
	 * @param keterangan                deskripsi tagihan (dipotong maksimum 40 karakter)
	 * @param virtualAccountBankOnline  entitas VA yang akan diisi request/response mentah dan
	 *                                   tanggal kedaluwarsa
	 * @return respons JSON mentah dari API SNAP BRI
	 * @throws Exception diteruskan dari {@link #doPost(String, VirtualAccountBank)}
	 */
	public static JSONObject post(BiodataCalonMahasiswa biodataCalonMahasiswa, int nominal, String keterangan,
			VirtualAccountBank virtualAccountBankOnline) throws Exception {

		String srv = Common.getKonfigurasi("BRI_SRV_ID", "23212").getNilai();

		String partnerServiceId = "              " + srv;

		partnerServiceId = partnerServiceId.substring(partnerServiceId.length() - 8);

		String customerNo = Common.getGeneratedAngkaDigit(11);
		String virtualAccountNo = "                   " + srv + customerNo;
		virtualAccountNo = virtualAccountNo.substring(virtualAccountNo.length() - 19);
		String virtualAccountName = biodataCalonMahasiswa.getNama() + " " + biodataCalonMahasiswa.getNoRegistrasi();
		Calendar expired = Calendar.getInstance();
		expired.set(Calendar.DATE, expired.get(Calendar.DATE) + 1);
		String expiredTimestamp = dateFormat1.get().format(expired.getTime()) + "+07:00";

		String trxId = Common.getGeneratedBarCode();

		JSONObject totalAmount = new JSONObject();
		totalAmount.put("value", nominal + ".00");
		totalAmount.put("currency", "IDR");

		JSONObject additionalInfo = new JSONObject();
		additionalInfo.put("description", Common.maxPanjang(keterangan, 40));

		JSONObject jsonObjectReq = new JSONObject();

		jsonObjectReq.put("partnerServiceId", partnerServiceId);
		jsonObjectReq.put("customerNo", customerNo);
		jsonObjectReq.put("virtualAccountNo", virtualAccountNo);
		jsonObjectReq.put("virtualAccountName", Common.maxPanjang(virtualAccountName, 255));
		jsonObjectReq.put("totalAmount", totalAmount);
		jsonObjectReq.put("expiredDate", expiredTimestamp);
		jsonObjectReq.put("trxId", trxId);
		jsonObjectReq.put("additionalInfo", additionalInfo);

		String postData = jsonObjectReq.toString();
		virtualAccountBankOnline.setKadaluarsa(expired.getTime()); 
		return doPost(postData, virtualAccountBankOnline);
	}

	/**
	 * Seperti {@link #post(Mahasiswa, int, String, VirtualAccountBank)}, untuk tagihan seorang
	 * {@link Siswa} (jenjang sekolah); nama VA dibentuk dari nama siswa dan Nomor Induk Nasional
	 * (NISN)-nya.
	 *
	 * @param siswa                     pemilik tagihan
	 * @param nominal                   nominal tagihan (rupiah, tanpa desimal)
	 * @param keterangan                deskripsi tagihan (dipotong maksimum 40 karakter)
	 * @param virtualAccountBankOnline  entitas VA yang akan diisi request/response mentah dan
	 *                                   tanggal kedaluwarsa
	 * @return respons JSON mentah dari API SNAP BRI
	 * @throws Exception diteruskan dari {@link #doPost(String, VirtualAccountBank)}
	 */
	public static JSONObject post(Siswa siswa, int nominal, String keterangan,
			VirtualAccountBank virtualAccountBankOnline) throws Exception {

		String srv = Common.getKonfigurasi("BRI_SRV_ID", "23212").getNilai();

		String partnerServiceId = "              " + srv;

		partnerServiceId = partnerServiceId.substring(partnerServiceId.length() - 8);

		String customerNo = Common.getGeneratedAngkaDigit(11);
		String virtualAccountNo = "                   " + srv + customerNo;
		virtualAccountNo = virtualAccountNo.substring(virtualAccountNo.length() - 19);
		String virtualAccountName = siswa.getNama() + " " + siswa.getNomorIndukNasional();
		Calendar expired = Calendar.getInstance();
		expired.set(Calendar.DATE, expired.get(Calendar.DATE) + 1);
		String expiredTimestamp = dateFormat1.get().format(expired.getTime()) + "+07:00";

		String trxId = Common.getGeneratedBarCode();

		JSONObject totalAmount = new JSONObject();
		totalAmount.put("value", nominal + ".00");
		totalAmount.put("currency", "IDR");

		JSONObject additionalInfo = new JSONObject();
		additionalInfo.put("description", Common.maxPanjang(keterangan, 40));

		JSONObject jsonObjectReq = new JSONObject();

		jsonObjectReq.put("partnerServiceId", partnerServiceId);
		jsonObjectReq.put("customerNo", customerNo);
		jsonObjectReq.put("virtualAccountNo", virtualAccountNo);
		jsonObjectReq.put("virtualAccountName", Common.maxPanjang(virtualAccountName, 255));
		jsonObjectReq.put("totalAmount", totalAmount);
		jsonObjectReq.put("expiredDate", expiredTimestamp);
		jsonObjectReq.put("trxId", trxId);
		jsonObjectReq.put("additionalInfo", additionalInfo);

		String postData = jsonObjectReq.toString();
		virtualAccountBankOnline.setKadaluarsa(expired.getTime()); 
		return doPost(postData, virtualAccountBankOnline);
	}

	/**
	 * Seperti {@link #post(Mahasiswa, int, String, VirtualAccountBank)}, untuk tagihan seorang
	 * calon siswa ({@link CalonSiswa}); nama VA dibentuk dari nama dan nomor registrasi calon
	 * siswa tersebut.
	 *
	 * @param calonSiswa                pemilik tagihan
	 * @param nominal                   nominal tagihan (rupiah, tanpa desimal)
	 * @param keterangan                deskripsi tagihan (dipotong maksimum 40 karakter)
	 * @param virtualAccountBankOnline  entitas VA yang akan diisi request/response mentah dan
	 *                                   tanggal kedaluwarsa
	 * @return respons JSON mentah dari API SNAP BRI
	 * @throws Exception diteruskan dari {@link #doPost(String, VirtualAccountBank)}
	 */
	public static JSONObject post(CalonSiswa calonSiswa, int nominal, String keterangan,
			VirtualAccountBank virtualAccountBankOnline) throws Exception {

		String srv = Common.getKonfigurasi("BRI_SRV_ID", "23212").getNilai();

		String partnerServiceId = "              " + srv;

		partnerServiceId = partnerServiceId.substring(partnerServiceId.length() - 8);

		String customerNo = Common.getGeneratedAngkaDigit(11);
		String virtualAccountNo = "                   " + srv + customerNo;
		virtualAccountNo = virtualAccountNo.substring(virtualAccountNo.length() - 19);
		String virtualAccountName = calonSiswa.getNama() + " " + calonSiswa.getNoRegistrasi();
		Calendar expired = Calendar.getInstance();
		expired.set(Calendar.DATE, expired.get(Calendar.DATE) + 1);
		String expiredTimestamp = dateFormat1.get().format(expired.getTime()) + "+07:00";

		String trxId = Common.getGeneratedBarCode();

		JSONObject totalAmount = new JSONObject();
		totalAmount.put("value", nominal + ".00");
		totalAmount.put("currency", "IDR");

		JSONObject additionalInfo = new JSONObject();
		additionalInfo.put("description", Common.maxPanjang(keterangan, 40));

		JSONObject jsonObjectReq = new JSONObject();

		jsonObjectReq.put("partnerServiceId", partnerServiceId);
		jsonObjectReq.put("customerNo", customerNo);
		jsonObjectReq.put("virtualAccountNo", virtualAccountNo);
		jsonObjectReq.put("virtualAccountName", Common.maxPanjang(virtualAccountName, 255));
		jsonObjectReq.put("totalAmount", totalAmount);
		jsonObjectReq.put("expiredDate", expiredTimestamp);
		jsonObjectReq.put("trxId", trxId);
		jsonObjectReq.put("additionalInfo", additionalInfo);

		String postData = jsonObjectReq.toString();
		virtualAccountBankOnline.setKadaluarsa(expired.getTime()); 
		return doPost(postData, virtualAccountBankOnline);
	}

	/**
	 * Implementasi kanonik pemanggilan API SNAP BRI: menyimpan {@code bodyS} sebagai request mentah
	 * ke {@code virtualAccountBankOnline}, meminta access token B2B (dengan tanda tangan
	 * {@link BRIUtil#generateSignatureToken(String, String)}), lalu memakai token tersebut untuk
	 * memanggil endpoint pembuatan VA dengan tanda tangan HMAC-SHA512 atas payload
	 * {@code method:path:token:sha256(body):timestamp}. Kedua panggilan HTTP dijalankan lewat
	 * proses {@code curl} eksternal ({@link ProcessBuilder}), BUKAN pustaka HTTP Java. Lihat
	 * javadoc kelas untuk peringatan keamanan terkait kredensial yang dicetak ke log dan diteruskan
	 * sebagai argumen proses di method ini.
	 *
	 * <p>
	 * Kegagalan pada permintaan token (tahap pertama) ditangkap dan hanya dicatat ke
	 * {@link ErrorAuditUtil} — method TETAP melanjutkan ke tahap kedua dengan {@code dataToken}
	 * kosong bila token gagal diperoleh (menghasilkan panggilan create-VA yang pasti ditolak
	 * server BRI karena {@code Authorization} tidak valid, alih-alih menghentikan proses lebih
	 * awal). Kegagalan pada permintaan create-VA (tahap kedua) juga ditangkap dan hanya dicatat;
	 * dalam kasus itu method mengembalikan {@link JSONObject} kosong, bukan melempar exception ke
	 * pemanggil.
	 * </p>
	 *
	 * @param bodyS                    payload JSON permintaan pembuatan VA (sudah disusun lengkap
	 *                                  oleh salah satu overload {@code post})
	 * @param virtualAccountBankOnline entitas VA yang diisi dengan request mentah (di awal) dan
	 *                                  response mentah (di akhir, bila berhasil)
	 * @return respons JSON dari API create-VA BRI, atau {@link JSONObject} kosong bila tahap
	 *         create-VA gagal
	 * @throws Exception tidak secara eksplisit dilempar keluar untuk kedua tahap curl (keduanya
	 *                    menangkap exception-nya sendiri); dideklarasikan untuk kompatibilitas
	 *                    tanda tangan dengan pemanggil
	 */
	private static JSONObject doPost(String bodyS, VirtualAccountBank virtualAccountBankOnline) throws Exception {

		virtualAccountBankOnline.setRequest(bodyS);

		String currentTimestamp = dateFormat.get().format(new Date()) + "+07:00";

		String clientId = Common.getKonfigurasi("BRI_CLIENT_ID", "WAVmwxO0EXUJyW4SDiY4ydUAe3gUvQYD").getNilai();
		String clientSecret = Common.getKonfigurasi("BRI_CLIENT_SECRET", "IGa7p9oeRJUhfdVR").getNilai();
		String dataToken = "";

		try {

			System.out.println("currentTimestamp -> " + currentTimestamp);

			String tokenSignature = BRIUtil.generateSignatureToken(clientId, currentTimestamp);

			System.out.println("tokenSignature -> " + tokenSignature);

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("grantType", "client_credentials");
			String postData = jsonObject.toString();

			System.out.println("postData -> " + postData);

			String[] command = { "curl", "--location", "--request", "POST",
					"https://sandbox.partner.api.bri.co.id/snap/v1.0/access-token/b2b", "--header",
					"Content-Type: application/json", "--header", "X-TIMESTAMP: " + currentTimestamp, "--header",
					"X-CLIENT-KEY: " + clientId, "--header", "X-SIGNATURE: " + tokenSignature, "--data-raw", postData };

			System.out.println("request -> ");
			for (String s : command) {
				System.out.print(s + " ");
			}
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
			System.out.println("\nresponse -> " + hasil);
			jsonObject = new JSONObject(hasil);
			dataToken = jsonObject.getString("accessToken");
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BcaDataUtil2.java:247");
		}

		String requestPath = "/snap/v1.0/transfer-va/create-va";
		String httpMethod = "POST";

		String sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(bodyS);

		String payload = httpMethod + ":" + requestPath + ":" + dataToken + ":" + sha256hex + ":" + currentTimestamp;

		System.out.println("client_secret " + clientSecret);
		System.out.println("dataToken " + dataToken);

		System.out.println("sebelum HAMAC SHA 512 " + payload);

		String signature = Hashing.hmacSha512(clientSecret.getBytes()).newHasher()
				.putString(payload, StandardCharsets.UTF_8).hash().toString();

		System.out.println("setelah HAMAC SHA 512 " + signature);

		String partnerId = Common.getKonfigurasi("BRI_PARTNER_ID", "ECAMPUS").getNilai();
		String externalID = Common.getKonfigurasi("BRI_EXTERNAL_ID", "1262222").getNilai();
		String channelID = Common.getKonfigurasi("BRI_CHANNEL_ID", "12345").getNilai();

		try {
			String[] command = { "curl", "--location", "--request", "POST",
					"https://sandbox.partner.api.bri.co.id/snap/v1.0/transfer-va/create-va", "--header",
					"Content-Type: application/json", "--header", "Authorization: Bearer " + dataToken, "--header",
					"X-TIMESTAMP: " + currentTimestamp, "--header", "X-SIGNATURE: " + signature, "--header",
					"X-PARTNER-ID: " + partnerId, "--header", "CHANNEL-ID: " + channelID, "--header",
					"X-EXTERNAL-ID: " + externalID, "--data-raw", bodyS };

			System.out.println("request -> ");
			for (String s : command) {
				System.out.print(s + " ");
			}

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
			System.out.println("\n\nhasil -> " + hasil);

			virtualAccountBankOnline.setResponse(hasil);

			return new JSONObject(hasil);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BcaDataUtil2.java:301");
		}

		return new JSONObject();
	}

}
