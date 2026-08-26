package ais.common.inventory.production;

/** Ringkasan operasi aplikasi produksi tanpa membocorkan detail persistence. */
public final class ProductionOperationResult {
	private final boolean successful;
	private final boolean idempotentReplay;
	private final int affectedLines;
	private final String message;

	public ProductionOperationResult(boolean successful, boolean idempotentReplay,
			int affectedLines, String message) {
		this.successful = successful;
		this.idempotentReplay = idempotentReplay;
		this.affectedLines = affectedLines;
		this.message = message == null ? "" : message.trim();
	}

	public boolean isSuccessful() { return successful; }
	public boolean isIdempotentReplay() { return idempotentReplay; }
	public int getAffectedLines() { return affectedLines; }
	public String getMessage() { return message; }
}
