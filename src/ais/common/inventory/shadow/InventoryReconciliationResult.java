package ais.common.inventory.shadow;

import java.math.BigDecimal;

import ais.common.inventory.ledger.InventoryBalanceKey;

/** Hasil pembandingan read-only antara saldo legacy dan saldo ledger baru. */
public final class InventoryReconciliationResult {

	public static final String MATCHED = "MATCHED";
	public static final String MISMATCH = "MISMATCH";
	public static final String FAILED = "FAILED";

	private final String status;
	private final InventoryBalanceKey balanceKey;
	private final BigDecimal legacyBalance;
	private final BigDecimal ledgerBalance;
	private final BigDecimal difference;
	private final String message;

	public InventoryReconciliationResult(String status, InventoryBalanceKey balanceKey,
			BigDecimal legacyBalance, BigDecimal ledgerBalance,
			BigDecimal difference, String message) {
		this.status = status == null ? "" : status.trim();
		this.balanceKey = balanceKey;
		this.legacyBalance = legacyBalance;
		this.ledgerBalance = ledgerBalance;
		this.difference = difference;
		this.message = message == null ? "" : message.trim();
	}

	public String getStatus() { return status; }
	public InventoryBalanceKey getBalanceKey() { return balanceKey; }
	public BigDecimal getLegacyBalance() { return legacyBalance; }
	public BigDecimal getLedgerBalance() { return ledgerBalance; }
	public BigDecimal getDifference() { return difference; }
	public String getMessage() { return message; }
	public boolean isMatched() { return MATCHED.equals(status); }
}
