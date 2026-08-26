package ais.common.inventory.production;

import java.math.BigDecimal;

/** Satu komponen resep/BOM pada kuantitas dasar hasil produksi. */
public final class BillOfMaterialLine {
	private final String lineId;
	private final Long componentItemId;
	private final Long uomId;
	private final BigDecimal quantity;
	private final BigDecimal expectedLossPercent;

	public BillOfMaterialLine(String lineId, Long componentItemId, Long uomId,
			BigDecimal quantity, BigDecimal expectedLossPercent) {
		this.lineId = clean(lineId);
		this.componentItemId = componentItemId;
		this.uomId = uomId;
		this.quantity = quantity;
		this.expectedLossPercent = expectedLossPercent == null
				? BigDecimal.ZERO : expectedLossPercent;
		if (this.lineId.length() == 0 || componentItemId == null || uomId == null
				|| quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0
				|| this.expectedLossPercent.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Komponen BOM tidak lengkap");
		}
	}

	public String getLineId() { return lineId; }
	public Long getComponentItemId() { return componentItemId; }
	public Long getUomId() { return uomId; }
	public BigDecimal getQuantity() { return quantity; }
	public BigDecimal getExpectedLossPercent() { return expectedLossPercent; }

	public BigDecimal requiredFor(BigDecimal outputQuantity, BigDecimal baseQuantity) {
		if (outputQuantity == null || outputQuantity.compareTo(BigDecimal.ZERO) <= 0
				|| baseQuantity == null || baseQuantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Kuantitas produksi wajib lebih dari nol");
		}
		BigDecimal lossFactor = BigDecimal.ONE.add(expectedLossPercent.movePointLeft(2));
		return quantity.multiply(outputQuantity).multiply(lossFactor)
				.divide(baseQuantity, 6, BigDecimal.ROUND_HALF_UP);
	}

	private static String clean(String value) { return value == null ? "" : value.trim(); }
}
