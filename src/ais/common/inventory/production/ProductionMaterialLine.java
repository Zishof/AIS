package ais.common.inventory.production;

import java.math.BigDecimal;

/** Bahan aktual per lot untuk issue atau return produksi. */
public final class ProductionMaterialLine {
	private final String lineId;
	private final Long itemId;
	private final Long uomId;
	private final Long lotId;
	private final BigDecimal quantity;
	private final BigDecimal unitCost;

	public ProductionMaterialLine(String lineId, Long itemId, Long uomId, Long lotId,
			BigDecimal quantity, BigDecimal unitCost) {
		this.lineId = clean(lineId);
		this.itemId = itemId;
		this.uomId = uomId;
		this.lotId = lotId;
		this.quantity = quantity;
		this.unitCost = unitCost == null ? BigDecimal.ZERO : unitCost;
		if (this.lineId.length() == 0 || itemId == null || uomId == null || lotId == null
				|| quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0
				|| this.unitCost.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Bahan aktual produksi tidak lengkap");
		}
	}

	public String getLineId() { return lineId; }
	public Long getItemId() { return itemId; }
	public Long getUomId() { return uomId; }
	public Long getLotId() { return lotId; }
	public BigDecimal getQuantity() { return quantity; }
	public BigDecimal getUnitCost() { return unitCost; }
	public BigDecimal getExtendedCost() { return quantity.multiply(unitCost); }

	private static String clean(String value) { return value == null ? "" : value.trim(); }
}
