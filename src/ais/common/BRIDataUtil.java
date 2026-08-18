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

public class BRIDataUtil {

	public static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	public static DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

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
		String expiredTimestamp = dateFormat1.format(expired.getTime()) + "+07:00";

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
		String expiredTimestamp = dateFormat1.format(expired.getTime()) + "+07:00";

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
		String expiredTimestamp = dateFormat1.format(expired.getTime()) + "+07:00";

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
		String expiredTimestamp = dateFormat1.format(expired.getTime()) + "+07:00";

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

	private static JSONObject doPost(String bodyS, VirtualAccountBank virtualAccountBankOnline) throws Exception {

		virtualAccountBankOnline.setRequest(bodyS);

		String currentTimestamp = dateFormat.format(new Date()) + "+07:00";

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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BRIDataUtil.java:247");
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BRIDataUtil.java:301");
		}

		return new JSONObject();
	}

}
