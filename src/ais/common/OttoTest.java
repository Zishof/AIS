package ais.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import com.google.common.hash.Hashing;

//import org.json.JSONObject;

public class OttoTest {

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

		String currentTimestamp = Instant.now().toEpochMilli() + "";
		currentTimestamp = currentTimestamp.substring(0, currentTimestamp.length() - 3);

		currentTimestamp = "1540383020";

		String apiKey = "KP33PP0EE0AAP1EE1009010PP01I91OA";

		System.out.println("currentTimestamp -> " + currentTimestamp);
		System.out.println("apiKey -> " + apiKey);

		String bodyS = "{\"customerDetails\":{\"email\":\"jihan.nabilah@ottodigital.id\",\"firstName\":\"Mohammad Fauzi\",\"lastName\":\"Murtadho\",\"phone\":\"6281382028582\"},\"transactionDetails\":{\"amount\":10000,\"currency\":\"IDR\",\"merchantName\":\"Uninus\",\"orderId\":\"ORD10a01a0\",\"vaOrderId\":\"\"}}";

		System.out.println("body -> " + bodyS);

		String valueToDigest = bodyS.trim().toLowerCase() + "&" + currentTimestamp + "&" + apiKey;

		System.out.println("signature sebelum hash -> " + valueToDigest);

		String signature = Hashing.hmacSha512(apiKey.getBytes()).newHasher()
				.putString(valueToDigest, StandardCharsets.UTF_8).hash().toString();

		System.out.println("signature setelah hash -> " + signature);

		String MID = "OP1E00030999";

		System.out.println("MID sebelum hash -> " + MID);

		String encodedMID = Base64.getEncoder().encodeToString(MID.getBytes());

		System.out.println("MID setelah hash -> " + encodedMID);

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
