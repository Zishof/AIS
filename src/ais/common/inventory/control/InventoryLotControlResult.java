package ais.common.inventory.control;

/** Hasil idempoten perubahan status lot. */
public final class InventoryLotControlResult {
	public static final String RELEASED = "RELEASED";
	public static final String ALREADY_RELEASED = "ALREADY_RELEASED";
	public static final String REJECTED = "REJECTED";

	private final String status;
	private final String message;

	public InventoryLotControlResult(String status, String message) {
		this.status = status == null ? "" : status.trim();
		this.message = message == null ? "" : message.trim();
	}

	public String getStatus() { return status; }
	public String getMessage() { return message; }
	public boolean isSuccessful() { return RELEASED.equals(status) || ALREADY_RELEASED.equals(status); }
	public boolean isIdempotentReplay() { return ALREADY_RELEASED.equals(status); }
}

