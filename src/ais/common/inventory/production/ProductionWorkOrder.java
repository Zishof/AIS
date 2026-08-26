package ais.common.inventory.production;

import java.math.BigDecimal;

/** Snapshot order produksi; adapter persistence menguasai perubahan status aktual. */
public final class ProductionWorkOrder {
	public static final String DRAFT = "DRAFT";
	public static final String RELEASED = "RELEASED";
	public static final String IN_PROGRESS = "IN_PROGRESS";
	public static final String COMPLETED = "COMPLETED";
	public static final String CANCELLED = "CANCELLED";

	private final String orderId;
	private final Long tenantId;
	private final Long locationId;
	private final BillOfMaterial billOfMaterial;
	private final BigDecimal plannedQuantity;
	private final String status;

	public ProductionWorkOrder(String orderId, Long tenantId, Long locationId,
			BillOfMaterial billOfMaterial, BigDecimal plannedQuantity, String status) {
		this.orderId = clean(orderId);
		this.tenantId = tenantId;
		this.locationId = locationId;
		this.billOfMaterial = billOfMaterial;
		this.plannedQuantity = plannedQuantity;
		this.status = clean(status);
		if (this.orderId.length() == 0 || tenantId == null || locationId == null
				|| billOfMaterial == null || !BillOfMaterial.ACTIVE.equals(billOfMaterial.getStatus())
				|| plannedQuantity == null || plannedQuantity.compareTo(BigDecimal.ZERO) <= 0
				|| !validStatus(this.status)) {
			throw new IllegalArgumentException("Order produksi tidak lengkap");
		}
	}

	public String getOrderId() { return orderId; }
	public Long getTenantId() { return tenantId; }
	public Long getLocationId() { return locationId; }
	public BillOfMaterial getBillOfMaterial() { return billOfMaterial; }
	public BigDecimal getPlannedQuantity() { return plannedQuantity; }
	public String getStatus() { return status; }

	private static boolean validStatus(String value) {
		return DRAFT.equals(value) || RELEASED.equals(value) || IN_PROGRESS.equals(value)
				|| COMPLETED.equals(value) || CANCELLED.equals(value);
	}
	private static String clean(String value) { return value == null ? "" : value.trim(); }
}
