package ais.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.json.JSONObject;

import com.google.common.hash.Hashing;

import ais.action.servlet.Bjb;
import ais.database.model.BankHost;

public class BJBSUtil {
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BJBSUtil.java:97");
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
