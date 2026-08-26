package ais.common.inventory.outbound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Snapshot transfer outbound. Status berasal dari persistence adapter. */
public final class OutboundOrder {
	public static final String APPROVED = "APPROVED";
	public static final String RESERVED = "RESERVED";
	public static final String PICKED = "PICKED";
	public static final String PACKED = "PACKED";
	public static final String DISPATCHED = "DISPATCHED";
	public static final String IN_TRANSIT = "IN_TRANSIT";
	public static final String PARTIALLY_RECEIVED = "PARTIALLY_RECEIVED";
	public static final String RECEIVED = "RECEIVED";

	private final String orderId;
	private final Long tenantId;
	private final Long sourceLocationId;
	private final Long destinationLocationId;
	private final String status;
	private final Date reservationExpiresAt;
	private final List<OutboundLotAllocation> allocations;

	public OutboundOrder(String orderId, Long tenantId, Long sourceLocationId,
			Long destinationLocationId, String status, Date reservationExpiresAt,
			List<OutboundLotAllocation> allocations) {
		this.orderId = clean(orderId);
		this.tenantId = tenantId;
		this.sourceLocationId = sourceLocationId;
		this.destinationLocationId = destinationLocationId;
		this.status = clean(status);
		this.reservationExpiresAt = copy(reservationExpiresAt);
		this.allocations = allocations == null
				? Collections.<OutboundLotAllocation>emptyList()
				: Collections.unmodifiableList(new ArrayList<OutboundLotAllocation>(allocations));
		if (this.orderId.length() == 0 || tenantId == null || sourceLocationId == null
				|| destinationLocationId == null || sourceLocationId.equals(destinationLocationId)
				|| this.status.length() == 0 || this.allocations.isEmpty()) {
			throw new IllegalArgumentException("Order outbound tidak lengkap");
		}
	}

	public String getOrderId() { return orderId; }
	public Long getTenantId() { return tenantId; }
	public Long getSourceLocationId() { return sourceLocationId; }
	public Long getDestinationLocationId() { return destinationLocationId; }
	public String getStatus() { return status; }
	public Date getReservationExpiresAt() { return copy(reservationExpiresAt); }
	public List<OutboundLotAllocation> getAllocations() { return allocations; }

	private static String clean(String value) { return value == null ? "" : value.trim(); }
	private static Date copy(Date value) { return value == null ? null : new Date(value.getTime()); }
}
