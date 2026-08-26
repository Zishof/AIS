package ais.common.inventory.shadow;

/** Hasil terisolasi shadow-write setelah transaksi legacy selesai. */
public final class InventoryShadowWriteResult {

	public static final String DISABLED = "DISABLED";
	public static final String POSTED = "POSTED";
	public static final String ALREADY_POSTED = "ALREADY_POSTED";
	public static final String REJECTED = "REJECTED";
	public static final String FAILED = "FAILED";

	private final String writerCode;
	private final String status;
	private final String idempotencyKey;
	private final Long movementId;
	private final String message;

	public InventoryShadowWriteResult(String writerCode, String status,
			String idempotencyKey, Long movementId, String message) {
		this.writerCode = clean(writerCode);
		this.status = clean(status);
		this.idempotencyKey = clean(idempotencyKey);
		this.movementId = movementId;
		this.message = clean(message);
	}

	public String getWriterCode() { return writerCode; }
	public String getStatus() { return status; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public Long getMovementId() { return movementId; }
	public String getMessage() { return message; }
	public boolean isSuccessful() {
		return POSTED.equals(status) || ALREADY_POSTED.equals(status);
	}

	public InventoryShadowWriteResult withMessage(String newMessage) {
		return new InventoryShadowWriteResult(writerCode, status, idempotencyKey,
				movementId, newMessage);
	}

	private static String clean(String value) {
		return value == null ? "" : value.trim();
	}
}
