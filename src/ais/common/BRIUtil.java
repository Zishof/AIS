package ais.common;

import java.io.BufferedReader;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class BRIUtil {

	public static final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		}
	};
	public static final ThreadLocal<DateFormat> dateFormat1 = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		}
	};

	private final static String PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n"
			+ "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKNwapOQ6rQJHetP\n"
			+ "HRlJBIh1OsOsUBiXb3rXXE3xpWAxAha0MH+UPRblOko+5T2JqIb+xKf9Vi3oTM3t\n"
			+ "KvffaOPtzKXZauscjq6NGzA3LgeiMy6q19pvkUUOlGYK6+Xfl+B7Xw6+hBMkQuGE\n"
			+ "nUS8nkpR5mK4ne7djIyfHFfMu4ptAgMBAAECgYA+s0PPtMq1osG9oi4xoxeAGikf\n"
			+ "JB3eMUptP+2DYW7mRibc+ueYKhB9lhcUoKhlQUhL8bUUFVZYakP8xD21thmQqnC4\n"
			+ "f63asad0ycteJMLb3r+z26LHuCyOdPg1pyLk3oQ32lVQHBCYathRMcVznxOG16VK\n"
			+ "I8BFfstJTaJu0lK/wQJBANYFGusBiZsJQ3utrQMVPpKmloO2++4q1v6ZR4puDQHx\n"
			+ "TjLjAIgrkYfwTJBLBRZxec0E7TmuVQ9uJ+wMu/+7zaUCQQDDf2xMnQqYknJoKGq+\n"
			+ "oAnyC66UqWC5xAnQS32mlnJ632JXA0pf9pb1SXAYExB1p9Dfqd3VAwQDwBsDDgP6\n"
			+ "HD8pAkEA0lscNQZC2TaGtKZk2hXkdcH1SKru/g3vWTkRHxfCAznJUaza1fx0wzdG\n"
			+ "GcES1Bdez0tbW4llI5By/skZc2eE3QJAFl6fOskBbGHde3Oce0F+wdZ6XIJhEgCP\n"
			+ "iukIcKZoZQzoiMJUoVRrA5gqnmaYDI5uRRl/y57zt6YksR3KcLUIuQJAd242M/WF\n"
			+ "6YAZat3q/wEeETeQq1wrooew+8lHl05/Nt0cCpV48RGEhJ83pzBm3mnwHf8lTBJH\n" + "x6XroMXsmbnsEw==\n"
			+ "-----END PRIVATE KEY-----";

	public static RSAPrivateKey readPKCS8PrivateKey() throws Exception {

		StringBuilder pkcs8Lines = new StringBuilder();
		BufferedReader rdr = new BufferedReader(
				new StringReader(Common.getKonfigurasi("BRI_PRIVATE_KEY", PRIVATE_KEY).getNilai()));
		String line;
		while ((line = rdr.readLine()) != null) {
			pkcs8Lines.append(line);
		}

		// Remove the "BEGIN" and "END" lines, as well as any whitespace

		String pkcs8Pem = pkcs8Lines.toString();
		pkcs8Pem = pkcs8Pem.replace("-----BEGIN PRIVATE KEY-----", "");
		pkcs8Pem = pkcs8Pem.replace("-----END PRIVATE KEY-----", "");
		pkcs8Pem = pkcs8Pem.replaceAll("\\s+", "");

		System.out.println("pkcs8Pem -> " + pkcs8Pem);
		// Base64 decode the result

		byte[] encoded = java.util.Base64.getDecoder().decode(pkcs8Pem);

		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
		return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
	}

	public static String generateSignatureToken(String clientId, String formattedTimestamp) throws Exception {
		String stringToSign = clientId + "|" + formattedTimestamp;

		System.out.println("stringToSign -> " + stringToSign);

		Signature sig = Signature.getInstance("SHA256withRSA");
		sig.initSign(readPKCS8PrivateKey());
		sig.update(stringToSign.getBytes("UTF-8"));
		byte[] signatureBytes = sig.sign();

		String signature = java.util.Base64.getEncoder().encodeToString(signatureBytes);
		return signature;
	}

}
