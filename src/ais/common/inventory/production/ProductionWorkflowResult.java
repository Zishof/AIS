package ais.common.inventory.production;

/** Hasil persistence workflow produksi, termasuk replay idempoten. */
public final class ProductionWorkflowResult {
	public static final String APPLIED = "APPLIED";
	public static final String ALREADY_APPLIED = "ALREADY_APPLIED";
	public static final String REJECTED = "REJECTED";

	private final String status;
	private final String message;

	public ProductionWorkflowResult(String status, String message) {
		this.status = status == null ? "" : status.trim();
		this.message = message == null ? "" : message.trim();
	}

	public String getStatus() { return status; }
	public String getMessage() { return message; }
	public boolean isSuccessful() { return APPLIED.equals(status) || ALREADY_APPLIED.equals(status); }
	public boolean isIdempotentReplay() { return ALREADY_APPLIED.equals(status); }
}
