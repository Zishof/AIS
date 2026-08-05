package ais.common;

public class Test3 {



	public static void main(String[] args) throws Exception {

//		String Authorization = "Bearer 07765ab7-a18c-42e8-a4e8-b4484b3230c9";
//		String tokenReq = Authorization.split(" ")[1].trim();

//		System.out.println("==> tokenReq " + tokenReq);

		String signatureKey = "0CcfEADwiAssIGQ6AMiWbiP9VHI0zzrBu4WUKfY1bNEF9q3FZJ";

		String payload = "pKMUSJfL5G8wKHSbhoTU7PQ3TJdX0HlV;create_va:1234567885";

//		String signature = Hashing.hmacSha512(strClientScret_bca.getBytes()).newHasher()
//				.putString(StringToSign, StandardCharsets.UTF_8).hash().toString();

//		Mac HmacSHA512 = Mac.getInstance("HmacSHA512");
//		SecretKeySpec secret_key = new SecretKeySpec(strClientScret_bca.getBytes(),
//				"HmacSHA512");
//		HmacSHA512.init(secret_key);
//
//		String signature = org.apache.commons.codec.binary.Base64
//				.encodeBase64String(HmacSHA512.doFinal(payload.getBytes()));
//		

		String signature = Common.buildHmacSignature(payload, signatureKey);

//		Mac HmacSHA512 = Mac.getInstance("HmacSHA512");
//		SecretKeySpec secret_key = new SecretKeySpec(strClientScret_bca.getBytes(), "HmacSHA512");
//		HmacSHA512.init(secret_key);
//
//		String signature = Base64.encodeBase64String(HmacSHA512.doFinal(StringToSign.getBytes()));

		System.out.println("==> signature " + signature);
//		Document doc = Jsoup.parse(
//				new URL("https://docs.google.com/viewerng/viewer?url=http://123.231.135.102/SIM/Mg-3.ppt"), 5000);
//
//		String title = doc.select("title").text();
//		System.out.println(title);
	}

}
