package ais.action.servlet.api.biometric;

/**
 * DTO immutable hasil pencocokan biometrik (mis. sidik jari/wajah) dari suatu penyedia layanan
 * biometrik eksternal atau lokal: apakah cocok ({@link #isMatched()}), skor kemiripan mentah
 * ({@link #getScore()}), dan nama penyedia yang melakukan pencocokan ({@link #getProvider()}).
 * Dipakai sebagai nilai balik API biometrik pada paket {@code servlet.api.biometric}.
 */
public final class BiometricMatchResult {
	private final boolean matched;
	private final double score;
	private final String provider;

	public BiometricMatchResult(boolean matched, double score, String provider) {
		this.matched = matched; this.score = score; this.provider = provider;
	}
	public boolean isMatched() { return matched; }
	public double getScore() { return score; }
	public String getProvider() { return provider; }
}
