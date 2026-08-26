package ais.common.inventory.outbound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Satu pengiriman fisik; satu order dapat dipecah menjadi beberapa shipment. */
public final class Shipment {
	private final String shipmentId;
	private final String deliveryOrderId;
	private final OutboundOrder order;
	private final List<OutboundLotAllocation> allocations;

	public Shipment(String shipmentId, String deliveryOrderId, OutboundOrder order,
			List<OutboundLotAllocation> allocations) {
		this.shipmentId = clean(shipmentId);
		this.deliveryOrderId = clean(deliveryOrderId);
		this.order = order;
		this.allocations = allocations == null
				? Collections.<OutboundLotAllocation>emptyList()
				: Collections.unmodifiableList(new ArrayList<OutboundLotAllocation>(allocations));
		if (this.shipmentId.length() == 0 || this.deliveryOrderId.length() == 0
				|| order == null || this.allocations.isEmpty()) {
			throw new IllegalArgumentException("Shipment tidak lengkap");
		}
	}

	public String getShipmentId() { return shipmentId; }
	public String getDeliveryOrderId() { return deliveryOrderId; }
	public OutboundOrder getOrder() { return order; }
	public List<OutboundLotAllocation> getAllocations() { return allocations; }

	private static String clean(String value) { return value == null ? "" : value.trim(); }
}
