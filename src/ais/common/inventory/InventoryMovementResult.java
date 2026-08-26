package ais.common.inventory;

/** Hasil posting mutasi yang dapat membedakan insert baru dan retry idempoten. */
public final class InventoryMovementResult {

	public static final String POSTED = "POSTED";
	public static final String ALREADY_POSTED = "ALREADY_POSTED";
	public static final String REJECTED = "REJECTED";

	private final String status;
	private final Long movementId;
	private final String message;

	public InventoryMovementResult(String status, Long movementId, String message) {
		this.status = status == null ? "" : status.trim();
		this.movementId = movementId;
		this.message = message == null ? "" : message.trim();
	}

	public String getStatus() { return status; }
	public Long getMovementId() { return movementId; }
	public String getMessage() { return message; }
	public boolean isSuccessful() { return POSTED.equals(status) || ALREADY_POSTED.equals(status); }
	public boolean isIdempotentReplay() { return ALREADY_POSTED.equals(status); }
}
