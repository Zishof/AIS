package ais.common.inventory.ledger;

import java.math.BigDecimal;
import java.util.Date;

/** Rekaman immutable hasil posting ledger beserta saldo sebelum dan sesudah. */
public final class InventoryLedgerRecord {

	private final Long movementId;
	private final InventoryBalanceKey balanceKey;
	private final String idempotencyKey;
	private final BigDecimal quantity;
	private final BigDecimal balanceBefore;
	private final BigDecimal balanceAfter;
	private final Date postedAt;

	public InventoryLedgerRecord(Long movementId, InventoryBalanceKey balanceKey,
			String idempotencyKey, BigDecimal quantity, BigDecimal balanceBefore,
			BigDecimal balanceAfter, Date postedAt) {
		if (movementId == null) throw new IllegalArgumentException("movementId wajib diisi");
		if (balanceKey == null) throw new IllegalArgumentException("balanceKey wajib diisi");
		if (kosong(idempotencyKey)) throw new IllegalArgumentException("idempotencyKey wajib diisi");
		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
			throw new IllegalArgumentException("quantity tidak boleh kosong atau nol");
		}
		if (balanceBefore == null || balanceAfter == null) {
			throw new IllegalArgumentException("saldo sebelum dan sesudah wajib diisi");
		}
		if (balanceBefore.add(quantity).compareTo(balanceAfter) != 0) {
			throw new IllegalArgumentException("saldo sesudah harus sama dengan saldo sebelum ditambah quantity");
		}
		if (postedAt == null) throw new IllegalArgumentException("postedAt wajib diisi");
		this.movementId = movementId;
		this.balanceKey = balanceKey;
		this.idempotencyKey = idempotencyKey.trim();
		this.quantity = quantity;
		this.balanceBefore = balanceBefore;
		this.balanceAfter = balanceAfter;
		this.postedAt = new Date(postedAt.getTime());
	}

	public Long getMovementId() { return movementId; }
	public InventoryBalanceKey getBalanceKey() { return balanceKey; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public BigDecimal getQuantity() { return quantity; }
	public BigDecimal getBalanceBefore() { return balanceBefore; }
	public BigDecimal getBalanceAfter() { return balanceAfter; }
	public Date getPostedAt() { return new Date(postedAt.getTime()); }

	private static boolean kosong(String value) {
		return value == null || value.trim().length() == 0;
	}
}
