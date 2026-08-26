package ais.action.servlet.api.biometric;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Pemilih matcher. Face embedding float32 didukung bawaan; fingerprint melalui SDK vendor. */
public final class BiometricMatcherRegistry {
	public static final String FACE_FORMAT = "FACE_EMBEDDING_F32_LE_V1";
	private static volatile BiometricMatcherProvider external;
	private static volatile boolean loaded;

	private BiometricMatcherRegistry() { }

	public static boolean available(String modality, String format) {
		if ("FACE".equals(modality) && FACE_FORMAT.equals(format)) return true;
		BiometricMatcherProvider p = external();
		return p != null && p.supports(modality, format);
	}

	public static BiometricMatchResult match(String modality, String format, byte[] probe, byte[] reference)
			throws Exception {
		if ("FACE".equals(modality) && FACE_FORMAT.equals(format)) return matchFace(probe, reference);
		BiometricMatcherProvider p = external();
		if (p == null || !p.supports(modality, format))
			throw new IllegalStateException("SDK matcher untuk " + modality + "/" + format + " belum dikonfigurasi");
		return p.match(modality, format, probe, reference);
	}

	public static String providerName(String modality, String format) {
		if ("FACE".equals(modality) && FACE_FORMAT.equals(format)) return "AIS_COSINE_FACE_V1";
		BiometricMatcherProvider p = external(); return p == null ? "UNAVAILABLE" : p.name();
	}

	private static BiometricMatchResult matchFace(byte[] a, byte[] b) {
		if (a == null || b == null || a.length != b.length || a.length < 16 || a.length % 4 != 0)
			return new BiometricMatchResult(false, 0.0d, "AIS_COSINE_FACE_V1");
		ByteBuffer aa = ByteBuffer.wrap(a).order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
		double dot = 0.0d, na = 0.0d, nb = 0.0d;
		while (aa.remaining() >= 4) {
			float x = aa.getFloat(), y = bb.getFloat();
			if (Float.isNaN(x) || Float.isInfinite(x) || Float.isNaN(y) || Float.isInfinite(y))
				return new BiometricMatchResult(false, 0.0d, "AIS_COSINE_FACE_V1");
			dot += x * y; na += x * x; nb += y * y;
		}
		double score = na == 0.0d || nb == 0.0d ? 0.0d : dot / (Math.sqrt(na) * Math.sqrt(nb));
		double threshold = numberSetting("AIS_BIOMETRIC_FACE_THRESHOLD", 0.82d);
		return new BiometricMatchResult(score >= threshold, score, "AIS_COSINE_FACE_V1");
	}

	private static BiometricMatcherProvider external() {
		if (loaded) return external;
		synchronized (BiometricMatcherRegistry.class) {
			if (loaded) return external;
			try {
				String className = BiometricCrypto.setting("AIS_BIOMETRIC_MATCHER_CLASS");
				if (className != null) external = (BiometricMatcherProvider) Class.forName(className).newInstance();
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "BiometricMatcherRegistry.external");
				external = null;
			} finally { loaded = true; }
		}
		return external;
	}

	private static double numberSetting(String name, double fallback) {
		try { String v = BiometricCrypto.setting(name); return v == null ? fallback : Double.parseDouble(v); }
		catch (Exception e) { return fallback; }
	}
}
