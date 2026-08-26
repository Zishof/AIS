package ais.common.inventory.accountspayable;

public final class AccountsPayableOperationResult {
	private final boolean successful;
	private final boolean idempotentReplay;
	private final String message;
	public AccountsPayableOperationResult(boolean successful, boolean idempotentReplay, String message) {
		this.successful = successful; this.idempotentReplay = idempotentReplay; this.message = message;
	}
	public boolean isSuccessful() { return successful; }
	public boolean isIdempotentReplay() { return idempotentReplay; }
	public String getMessage() { return message; }
}
