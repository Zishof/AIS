package ais.action.servlet.api.biometric;

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
