package ais.common.inventory.replenishment;

import java.math.BigDecimal;

/** Hasil alokasi satu baris, termasuk kekurangan yang menjadi calon procurement. */
public final class OutletReplenishmentPlanLine {

	private final OutletReplenishmentLine requestLine;
	private final BigDecimal availableQuantity;
	private final BigDecimal warehouseAllocationQuantity;
	private final BigDecimal procurementShortageQuantity;

	public OutletReplenishmentPlanLine(OutletReplenishmentLine requestLine,
			BigDecimal availableQuantity, BigDecimal warehouseAllocationQuantity,
			BigDecimal procurementShortageQuantity) {
		this.requestLine = requestLine;
		this.availableQuantity = nilai(availableQuantity);
		this.warehouseAllocationQuantity = nilai(warehouseAllocationQuantity);
		this.procurementShortageQuantity = nilai(procurementShortageQuantity);
	}

	public OutletReplenishmentLine getRequestLine() { return requestLine; }
	public BigDecimal getAvailableQuantity() { return availableQuantity; }
	public BigDecimal getWarehouseAllocationQuantity() { return warehouseAllocationQuantity; }
	public BigDecimal getProcurementShortageQuantity() { return procurementShortageQuantity; }
	public boolean requiresProcurement() { return procurementShortageQuantity.compareTo(BigDecimal.ZERO) > 0; }

	private static BigDecimal nilai(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}

