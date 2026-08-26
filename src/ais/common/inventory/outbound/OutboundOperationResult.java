package ais.common.inventory.outbound;

/** Ringkasan operasi lintas port untuk UI/API. */
public final class OutboundOperationResult {
	private final boolean successful;
	private final boolean idempotentReplay;
	private final boolean discrepancy;
	private final int affectedLines;
	private final String message;

	public OutboundOperationResult(boolean successful, boolean idempotentReplay,
			boolean discrepancy, int affectedLines, String message) {
		this.successful = successful;
		this.idempotentReplay = idempotentReplay;
		this.discrepancy = discrepancy;
		this.affectedLines = affectedLines;
		this.message = message == null ? "" : message.trim();
	}

	public boolean isSuccessful() { return successful; }
	public boolean isIdempotentReplay() { return idempotentReplay; }
	public boolean hasDiscrepancy() { return discrepancy; }
	public int getAffectedLines() { return affectedLines; }
	public String getMessage() { return message; }
}
