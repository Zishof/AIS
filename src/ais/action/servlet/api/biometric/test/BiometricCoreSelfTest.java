package ais.action.servlet.api.biometric.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

import ais.action.servlet.api.biometric.BiometricCrypto;
import ais.action.servlet.api.biometric.BiometricMatchResult;
import ais.action.servlet.api.biometric.BiometricMatcherRegistry;

/**
 * Self-test tanpa JUnit untuk primitive keamanan biometrik.
 *
 * Jalankan dari hasil kompilasi dengan classpath WEB-INF/lib. Tidak memakai
 * database dan tidak mencetak kunci maupun template.
 */
public final class BiometricCoreSelfTest {
	private BiometricCoreSelfTest() { }

	public static void main(String[] args) throws Exception {
		String previousKey = System.getProperty("AIS_BIOMETRIC_MASTER_KEY_BASE64");
		String previousThreshold = System.getProperty("AIS_BIOMETRIC_FACE_THRESHOLD");
		try {
			byte[] key = new byte[32];
			for (int i = 0; i < key.length; i++) key[i] = (byte) (i + 1);
			System.setProperty("AIS_BIOMETRIC_MASTER_KEY_BASE64", Base64.encodeBase64String(key));
			System.setProperty("AIS_BIOMETRIC_FACE_THRESHOLD", "0.82");

			testEncryptionRoundTrip();
			testAadTamperRejected();
			testFaceMatcher();
			System.out.println("BiometricCoreSelfTest: OK");
		} finally {
			restore("AIS_BIOMETRIC_MASTER_KEY_BASE64", previousKey);
			restore("AIS_BIOMETRIC_FACE_THRESHOLD", previousThreshold);
		}
	}

	private static void testEncryptionRoundTrip() throws Exception {
		byte[] clear = new byte[] { 1, 3, 3, 7, 9, 2, 4, 8 };
		String sealed = BiometricCrypto.encrypt(clear, "subject|FACE|format");
		assertTrue(!sealed.contains(Base64.encodeBase64String(clear)),
				"Ciphertext tidak boleh memuat template asli");
		byte[] opened = BiometricCrypto.decrypt(sealed, "subject|FACE|format");
		assertTrue(Arrays.equals(clear, opened), "Round-trip AES-GCM gagal");
	}

	private static void testAadTamperRejected() throws Exception {
		String sealed = BiometricCrypto.encrypt(new byte[] { 7, 7, 7, 7 },
				"subject-a|FINGERPRINT|ISO_19794_2");
		boolean rejected = false;
		try {
			BiometricCrypto.decrypt(sealed, "subject-b|FINGERPRINT|ISO_19794_2");
		} catch (Exception expected) {
			rejected = true;
		}
		assertTrue(rejected, "Perubahan subject/AAD harus ditolak");
	}

	private static void testFaceMatcher() throws Exception {
		byte[] reference = floats(1.0f, 0.0f, 0.5f, 0.25f);
		byte[] near = floats(0.99f, 0.01f, 0.49f, 0.26f);
		byte[] opposite = floats(-1.0f, 0.0f, -0.5f, -0.25f);
		BiometricMatchResult accepted = BiometricMatcherRegistry.match("FACE",
				BiometricMatcherRegistry.FACE_FORMAT, near, reference);
		BiometricMatchResult rejected = BiometricMatcherRegistry.match("FACE",
				BiometricMatcherRegistry.FACE_FORMAT, opposite, reference);
		assertTrue(accepted.isMatched(), "Embedding wajah serupa harus cocok");
		assertTrue(!rejected.isMatched(), "Embedding wajah berlawanan harus ditolak");
	}

	private static byte[] floats(float a, float b, float c, float d) {
		return ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
				.putFloat(a).putFloat(b).putFloat(c).putFloat(d).array();
	}

	private static void restore(String name, String value) {
		if (value == null) System.clearProperty(name); else System.setProperty(name, value);
	}

	private static void assertTrue(boolean value, String message) {
		if (!value) throw new IllegalStateException(message);
	}
}
