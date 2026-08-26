package ais.common.inventory.replenishment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ais.common.inventory.ledger.InventoryBalanceKey;

/**
 * Menghitung pemenuhan gudang dan kekurangan procurement tanpa menulis stok.
 */
public final class OutletReplenishmentPlanner {

	private final ReplenishmentAvailabilityPort availabilityPort;

	public OutletReplenishmentPlanner(ReplenishmentAvailabilityPort availabilityPort) {
		if (availabilityPort == null) throw new IllegalArgumentException("availabilityPort wajib diisi");
		this.availabilityPort = availabilityPort;
	}

	public OutletReplenishmentPlan plan(OutletReplenishmentRequest request) {
		if (request == null) {
			return new OutletReplenishmentPlan(OutletReplenishmentPlan.REJECTED, null,
					Collections.<OutletReplenishmentPlanLine>emptyList(),
					Collections.singletonList("request wajib diisi"));
		}
		List<String> validationErrors = request.validate();
		if (!validationErrors.isEmpty()) {
			return new OutletReplenishmentPlan(OutletReplenishmentPlan.REJECTED, request,
					Collections.<OutletReplenishmentPlanLine>emptyList(), validationErrors);
		}

		List<OutletReplenishmentPlanLine> resultLines = new ArrayList<OutletReplenishmentPlanLine>();
		boolean hasWarehouseAllocation = false;
		boolean hasProcurementShortage = false;
		try {
			for (int i = 0; i < request.getLines().size(); i++) {
				OutletReplenishmentLine line = request.getLines().get(i);
				InventoryBalanceKey balanceKey = new InventoryBalanceKey(request.getTenantId(),
						request.getSourceWarehouseLocationId(), line.getItemId(), null);
				BigDecimal available = availabilityPort.findAvailableQuantity(balanceKey);
				if (available == null || available.compareTo(BigDecimal.ZERO) < 0) available = BigDecimal.ZERO;
				BigDecimal allocation = available.min(line.getRequestedQuantity());
				BigDecimal shortage = line.getRequestedQuantity().subtract(allocation);
				if (allocation.compareTo(BigDecimal.ZERO) > 0) hasWarehouseAllocation = true;
				if (shortage.compareTo(BigDecimal.ZERO) > 0) hasProcurementShortage = true;
				resultLines.add(new OutletReplenishmentPlanLine(line, available, allocation, shortage));
			}
		} catch (RuntimeException exception) {
			return new OutletReplenishmentPlan(OutletReplenishmentPlan.FAILED, request,
					Collections.<OutletReplenishmentPlanLine>emptyList(),
					Collections.singletonList("gagal membaca stok tersedia: " + aman(exception.getMessage())));
		}

		String status;
		if (!hasProcurementShortage) status = OutletReplenishmentPlan.READY_FROM_WAREHOUSE;
		else if (!hasWarehouseAllocation) status = OutletReplenishmentPlan.PROCUREMENT_REQUIRED;
		else status = OutletReplenishmentPlan.PARTIAL_PROCUREMENT_REQUIRED;
		return new OutletReplenishmentPlan(status, request, resultLines,
				Collections.<String>emptyList());
	}

	private static String aman(String value) {
		return value == null ? "tanpa rincian" : value;
	}
}

