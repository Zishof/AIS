package ais.common.inventory.jdbc;

/** Kegagalan infrastruktur saat membaca atau mem-posting ledger inventory. */
public final class InventoryPersistenceException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InventoryPersistenceException(String message, Throwable cause) {
		super(message, cause);
	}
}
