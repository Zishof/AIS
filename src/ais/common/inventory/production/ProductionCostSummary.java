package ais.common.inventory.production;

import java.math.BigDecimal;

/** Ringkasan costing produksi yang deterministik dan bebas pembulatan implicit. */
public final class ProductionCostSummary {
	private final BigDecimal materialCost;
	private final BigDecimal laborCost;
	private final BigDecimal overheadCost;
	private final BigDecimal totalCost;
	private final BigDecimal unitCost;

	public ProductionCostSummary(BigDecimal materialCost, BigDecimal laborCost,
			BigDecimal overheadCost, BigDecimal totalCost, BigDecimal unitCost) {
		this.materialCost = materialCost;
		this.laborCost = laborCost;
		this.overheadCost = overheadCost;
		this.totalCost = totalCost;
		this.unitCost = unitCost;
	}

	public BigDecimal getMaterialCost() { return materialCost; }
	public BigDecimal getLaborCost() { return laborCost; }
	public BigDecimal getOverheadCost() { return overheadCost; }
	public BigDecimal getTotalCost() { return totalCost; }
	public BigDecimal getUnitCost() { return unitCost; }
}
