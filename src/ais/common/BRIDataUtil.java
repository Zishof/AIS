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
 * Kelas utilitas untuk membentuk dan mengirim permintaan pembuatan Virtual Account (VA) BRI
 * lewat API SNAP BRI ({@code https://sandbox.partner.api.bri.co.id/snap/v1.0/transfer-va/create-va}),
 * dipakai di berbagai modul AIS yang perlu menerbitkan VA pembayaran atas nama entitas yang
 * berbeda-beda: mahasiswa ({@link Mahasiswa}), calon mahasiswa ({@link BiodataCalonMahasiswa}),
 * siswa sekolah ({@link Siswa}), dan calon siswa ({@link CalonSiswa}).
 *
 * <p>
 * <b>Relasi dengan {@link BRIUtil}</b> — kelas ini berfokus pada LOGIKA DATA dan pemanggilan
 * API BRI yang sesungguhnya: menyusun payload permintaan pembuatan VA (nomor VA, nama pemilik
 * VA, nominal, tanggal kadaluarsa, dsb.) dari entitas domain AIS, memperoleh access token OAuth
 * B2B, membentuk header {@code X-SIGNATURE} untuk permintaan create-VA, memanggil API lewat
 * proses {@code curl} eksternal, dan menyimpan request/response mentah ke record
 * {@link VirtualAccountBank}. Kelas {@link BRIUtil} yang terpisah HANYA menyediakan primitif
 * kriptografi murni (memuat private key RSA dan menandatangani string SHA256withRSA) yang
 * dipakai kelas ini ({@link #doPost}) untuk memperoleh access token; {@code BRIDataUtil} tidak
 * menduplikasi logika kriptografi tersebut, melainkan memanggilnya lewat
 * {@link BRIUtil#generateSignatureToken(String, String)}.
 * </p>
 *
 * <p>
 * Empat overload publik {@link #post(Mahasiswa, int, String, VirtualAccountBank)}, {@link
 * #post(BiodataCalonMahasiswa, int, String, VirtualAccountBank)}, {@link #post(Siswa, int,
 * String, VirtualAccountBank)}, dan {@link #post(CalonSiswa, int, String, VirtualAccountBank)}
 * memiliki struktur logika yang IDENTIK — perbedaannya hanya pada tipe entitas pemilik VA dan
 * bagaimana {@code virtualAccountName} disusun dari field entitas tersebut (nama+NIM untuk
 * mahasiswa, nama+nomor registrasi untuk calon mahasiswa/calon siswa, nama+NISN untuk siswa).
 * Keempatnya menyusun payload JSON permintaan create-VA (partnerServiceId dari konfigurasi
 * {@code BRI_SRV_ID}, nomor customer acak 11 digit, nomor VA gabungan srv+customer sepanjang 19
 * digit, nominal dengan format {@code "<nilai>.00"}, tanggal kadaluarsa besok dari sekarang, dan
 * id transaksi dari {@link Common#getGeneratedBarCode()}), lalu bermuara pada satu implementasi
 * privat kanonik {@link #doPost(String, VirtualAccountBank)} yang benar-benar melakukan
 * pemanggilan API BRI.
 * </p>
 *
 * <p>
 * <b>Riwayat keamanan (DIPERBAIKI 2026-09-01):</b> method privat
 * {@link #doPost(String, VirtualAccountBank)} sebelumnya membaca {@code BRI_CLIENT_ID}/
 * {@code BRI_CLIENT_SECRET} (dipakai langsung sebagai kunci HMAC-SHA512 penandatangan permintaan
 * create-VA — bersifat rahasia) lewat {@code Common.getKonfigurasi(key, default)} dengan NILAI
 * DEFAULT rahasia tertulis langsung di kode sumber. Default tersebut sudah DIHAPUS — kedua nilai
 * kini WAJIB diisi lewat konfigurasi, dan {@link #doPost} melempar {@link IllegalStateException}
 * bila salah satunya kosong. Nilai {@code BRI_SRV_ID}, {@code BRI_PARTNER_ID},
 * {@code BRI_EXTERNAL_ID}, dan {@code BRI_CHANNEL_ID} TETAP memiliki default tertulis di kode
 * karena bersifat pengenal (identifier), bukan rahasia kriptografis. Seluruh baris
 * {@code System.out.println}/{@code print} yang sebelumnya mencetak {@code clientSecret},
 * {@code tokenSignature}, {@code dataToken} (access token OAuth), payload HMAC, {@code signature},
 * respons API, dan bahkan perintah {@code curl} LENGKAP (termasuk header {@code Authorization}/
 * {@code X-SIGNATURE}/{@code X-CLIENT-KEY}) ke {@link System#out} juga sudah DIHAPUS —
 * sebelumnya berisiko membocorkan seluruh rahasia dan token akses ke berkas log aplikasi pada
 * SETIAP pemanggilan create-VA. <b>Tindak lanjut yang TETAP diperlukan di luar perubahan kode
 * ini:</b> {@code client_id}/{@code client_secret} yang sebelumnya tertanam sudah lama berada di
 * riwayat SVN dan harus dianggap bocor — bila itu kredensial sungguhan (bukan kredensial sandbox
 * BRI, mengingat endpoint yang dipanggil kelas ini adalah {@code sandbox.partner.api.bri.co.id}),
 * WAJIB dirotasi/dicabut di sisi BRI.
 * </p>
 */
public class BRIDataUtil {

	/**
	 * Formatter tanggal ISO-8601 dengan presisi milidetik ({@code yyyy-MM-dd'T'HH:mm:ss.SSS}),
	 * dipakai untuk timestamp header {@code X-TIMESTAMP} pada permintaan token OAuth API BRI.
	 * Dibungkus {@link ThreadLocal} karena {@link SimpleDateFormat} tidak thread-safe.
	 */
	public static final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		}
	};
	/**
	 * Formatter tanggal ISO-8601 tanpa milidetik ({@code yyyy-MM-dd'T'HH:mm:ss}), dipakai untuk
	 * menyusun {@code expiredDate} pada payload pembuatan VA. Dibungkus {@link ThreadLocal}
	 * karena {@link SimpleDateFormat} tidak thread-safe.
	 */
	public static final ThreadLocal<DateFormat> dateFormat1 = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		}
	};

	/**
	 * Membentuk dan mengirim permintaan pembuatan Virtual Account BRI atas nama seorang
	 * mahasiswa. Nama pemilik VA disusun dari {@code nama + " " + nim} mahasiswa. Lihat javadoc
	 * kelas untuk uraian lengkap struktur payload dan alur kerja yang identik pada ketiga
	 * overload {@code post} lainnya.
	 *
	 * @param mahasiswa                 mahasiswa pemilik VA yang akan dibuat
	 * @param nominal                   nominal tagihan VA dalam rupiah (bilangan bulat, akan
	 *                                   diformat sebagai {@code "<nominal>.00"})
	 * @param keterangan                deskripsi/keterangan tagihan, dipotong maksimal 40
	 *                                   karakter sebelum dikirim ke API BRI
	 * @param virtualAccountBankOnline  record {@link VirtualAccountBank} yang akan diperbarui
	 *                                   di tempat: kolom {@code request}, {@code response}, dan
	 *                                   {@code kadaluarsa} ditulis oleh proses ini
	 * @return objek {@link JSONObject} respons API BRI (hasil parsing JSON mentah dari
	 *         {@code curl}), atau {@link JSONObject} kosong bila pemanggilan API gagal
	 * @throws Exception diteruskan dari kegagalan penyusunan payload atau pemanggilan
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
	 * Seperti {@link #post(Mahasiswa, int, String, VirtualAccountBank)}, namun untuk calon
	 * mahasiswa; nama pemilik VA disusun dari {@code nama + " " + noRegistrasi}
	 * {@link BiodataCalonMahasiswa}.
	 *
	 * @param biodataCalonMahasiswa    calon mahasiswa pemilik VA yang akan dibuat
	 * @param nominal                  nominal tagihan VA dalam rupiah
	 * @param keterangan               deskripsi tagihan (dipotong maksimal 40 karakter)
	 * @param virtualAccountBankOnline record {@link VirtualAccountBank} yang diperbarui di
	 *                                  tempat
	 * @return respons API BRI dalam bentuk {@link JSONObject}, atau kosong bila gagal
	 * @throws Exception diteruskan dari kegagalan penyusunan payload atau pemanggilan API
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
	 * Seperti {@link #post(Mahasiswa, int, String, VirtualAccountBank)}, namun untuk siswa
	 * sekolah; nama pemilik VA disusun dari {@code nama + " " + nomorIndukNasional}
	 * {@link Siswa}.
	 *
	 * @param siswa                    siswa pemilik VA yang akan dibuat
	 * @param nominal                  nominal tagihan VA dalam rupiah
	 * @param keterangan               deskripsi tagihan (dipotong maksimal 40 karakter)
	 * @param virtualAccountBankOnline record {@link VirtualAccountBank} yang diperbarui di
	 *                                  tempat
	 * @return respons API BRI dalam bentuk {@link JSONObject}, atau kosong bila gagal
	 * @throws Exception diteruskan dari kegagalan penyusunan payload atau pemanggilan API
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
	 * Seperti {@link #post(Mahasiswa, int, String, VirtualAccountBank)}, namun untuk calon
	 * siswa; nama pemilik VA disusun dari {@code nama + " " + noRegistrasi} {@link CalonSiswa}.
	 *
	 * @param calonSiswa               calon siswa pemilik VA yang akan dibuat
	 * @param nominal                  nominal tagihan VA dalam rupiah
	 * @param keterangan               deskripsi tagihan (dipotong maksimal 40 karakter)
	 * @param virtualAccountBankOnline record {@link VirtualAccountBank} yang diperbarui di
	 *                                  tempat
	 * @return respons API BRI dalam bentuk {@link JSONObject}, atau kosong bila gagal
	 * @throws Exception diteruskan dari kegagalan penyusunan payload atau pemanggilan API
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
	 * Implementasi kanonik pemanggilan API SNAP BRI untuk pembuatan Virtual Account, dipakai
	 * bersama oleh keempat overload publik {@code post}. Alur kerja: (1) menyimpan payload
	 * mentah {@code bodyS} ke {@code virtualAccountBankOnline.setRequest}; (2) memperoleh
	 * access token OAuth B2B dari endpoint {@code /snap/v1.0/access-token/b2b} — permintaan
	 * ditandatangani dengan {@link BRIUtil#generateSignatureToken(String, String)} memakai
	 * {@code clientId} dan dikirim lewat proses {@code curl} eksternal, responsnya di-parse
	 * untuk diambil field {@code accessToken}; (3) menyusun signature HMAC-SHA512 untuk
	 * permintaan create-VA sesuai spesifikasi SNAP (payload {@code
	 * "<method>:<path>:<accessToken>:<sha256hex(body)>:<timestamp>"}, ditandatangani memakai
	 * {@code clientSecret} sebagai kunci HMAC); (4) memanggil endpoint
	 * {@code /snap/v1.0/transfer-va/create-va} lewat {@code curl} dengan header
	 * {@code Authorization}, {@code X-SIGNATURE}, {@code X-PARTNER-ID}, {@code CHANNEL-ID}, dan
	 * {@code X-EXTERNAL-ID}; (5) menyimpan respons mentah ke
	 * {@code virtualAccountBankOnline.setResponse} dan mengembalikannya sebagai
	 * {@link JSONObject}.
	 *
	 * <p>
	 * Kegagalan pada tahap perolehan access token (langkah 2) ditangkap dan hanya dicatat ke
	 * audit error tanpa menghentikan alur — {@code dataToken} akan tetap berupa string kosong
	 * dan permintaan create-VA (langkah 4) akan tetap dicoba dengan token kosong tersebut
	 * (kemungkinan besar akan ditolak API BRI). Kegagalan pada tahap create-VA (langkah 4)
	 * ditangkap serupa dan method mengembalikan {@link JSONObject} kosong. Lihat peringatan
	 * keamanan pada javadoc kelas terkait kredensial default dan pencetakan secret/token ke
	 * {@link System#out} di dalam method ini.
	 * </p>
	 *
	 * @param bodyS                     payload JSON permintaan create-VA yang sudah disusun
	 *                                  oleh salah satu overload {@code post}
	 * @param virtualAccountBankOnline  record {@link VirtualAccountBank} yang diperbarui di
	 *                                  tempat (request/response mentah)
	 * @return respons API BRI dalam bentuk {@link JSONObject}; {@link JSONObject} kosong bila
	 *         pemanggilan create-VA gagal (bukan {@code null})
	 * @throws Exception dilempar dari kegagalan di luar blok try-catch internal (mis. kegagalan
	 *                    menghitung {@code sha256hex}/HMAC sebelum pemanggilan create-VA)
	 */
	private static JSONObject doPost(String bodyS, VirtualAccountBank virtualAccountBankOnline) throws Exception {

		virtualAccountBankOnline.setRequest(bodyS);

		String currentTimestamp = dateFormat.get().format(new Date()) + "+07:00";

		String clientId = Common.getKonfigurasi("BRI_CLIENT_ID", "").getNilai();
		String clientSecret = Common.getKonfigurasi("BRI_CLIENT_SECRET", "").getNilai();
		if (clientId == null || clientId.trim().isEmpty() || clientSecret == null || clientSecret.trim().isEmpty()) {
			throw new IllegalStateException(
					"Kredensial BRI belum dikonfigurasi. Isi konfigurasi BRI_CLIENT_ID dan BRI_CLIENT_SECRET.");
		}
		String dataToken = "";

		try {

			String tokenSignature = BRIUtil.generateSignatureToken(clientId, currentTimestamp);

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("grantType", "client_credentials");
			String postData = jsonObject.toString();

			String[] command = { "curl", "--location", "--request", "POST",
					"https://sandbox.partner.api.bri.co.id/snap/v1.0/access-token/b2b", "--header",
					"Content-Type: application/json", "--header", "X-TIMESTAMP: " + currentTimestamp, "--header",
					"X-CLIENT-KEY: " + clientId, "--header", "X-SIGNATURE: " + tokenSignature, "--data-raw", postData };

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
			jsonObject = new JSONObject(hasil);
			dataToken = jsonObject.getString("accessToken");
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BRIDataUtil.java:247");
		}

		String requestPath = "/snap/v1.0/transfer-va/create-va";
		String httpMethod = "POST";

		String sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(bodyS);

		String payload = httpMethod + ":" + requestPath + ":" + dataToken + ":" + sha256hex + ":" + currentTimestamp;

		String signature = Hashing.hmacSha512(clientSecret.getBytes()).newHasher()
				.putString(payload, StandardCharsets.UTF_8).hash().toString();

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

			virtualAccountBankOnline.setResponse(hasil);

			return new JSONObject(hasil);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BRIDataUtil.java:301");
		}

		return new JSONObject();
	}

}
