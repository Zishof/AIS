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

public class BJBUtil {
	public static String dataToken = null;

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

	private static String encode(byte[] bytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

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
