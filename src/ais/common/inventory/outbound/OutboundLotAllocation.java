package ais.common.inventory.outbound;

import java.math.BigDecimal;

import ais.common.inventory.ledger.InventoryBalanceKey;

/** Alokasi lot sumber; unit terkecil reservasi, picking, dan issue. */
public final class OutboundLotAllocation {
	private final String allocationId;
	private final String lineId;
	private final InventoryBalanceKey sourceBalanceKey;
	private final Long uomId;
	private final BigDecimal quantity;

	public OutboundLotAllocation(String allocationId, String lineId,
			InventoryBalanceKey sourceBalanceKey, Long uomId, BigDecimal quantity) {
		this.allocationId = clean(allocationId);
		this.lineId = clean(lineId);
		this.sourceBalanceKey = sourceBalanceKey;
		this.uomId = uomId;
		this.quantity = quantity;
		if (this.allocationId.length() == 0 || this.lineId.length() == 0
				|| sourceBalanceKey == null || uomId == null || quantity == null
				|| quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Alokasi lot outbound tidak lengkap");
		}
	}

	public String getAllocationId() { return allocationId; }
	public String getLineId() { return lineId; }
	public InventoryBalanceKey getSourceBalanceKey() { return sourceBalanceKey; }
	public Long getUomId() { return uomId; }
	public BigDecimal getQuantity() { return quantity; }

	private static String clean(String value) { return value == null ? "" : value.trim(); }
}
