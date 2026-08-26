package ais.common.inventory.replenishment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Rencana pemenuhan read-only; belum menjadi reservasi, transfer, ataupun PR. */
public final class OutletReplenishmentPlan {

	public static final String READY_FROM_WAREHOUSE = "READY_FROM_WAREHOUSE";
	public static final String PARTIAL_PROCUREMENT_REQUIRED = "PARTIAL_PROCUREMENT_REQUIRED";
	public static final String PROCUREMENT_REQUIRED = "PROCUREMENT_REQUIRED";
	public static final String REJECTED = "REJECTED";
	public static final String FAILED = "FAILED";

	private final String status;
	private final OutletReplenishmentRequest request;
	private final List<OutletReplenishmentPlanLine> lines;
	private final List<String> messages;

	public OutletReplenishmentPlan(String status, OutletReplenishmentRequest request,
			List<OutletReplenishmentPlanLine> lines, List<String> messages) {
		this.status = status == null ? "" : status.trim();
		this.request = request;
		this.lines = lines == null
				? Collections.<OutletReplenishmentPlanLine>emptyList()
				: Collections.unmodifiableList(new ArrayList<OutletReplenishmentPlanLine>(lines));
		this.messages = messages == null
				? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(messages));
	}

	public String getStatus() { return status; }
	public OutletReplenishmentRequest getRequest() { return request; }
	public List<OutletReplenishmentPlanLine> getLines() { return lines; }
	public List<String> getMessages() { return messages; }
	public boolean isSuccessful() {
		return READY_FROM_WAREHOUSE.equals(status)
				|| PARTIAL_PROCUREMENT_REQUIRED.equals(status)
				|| PROCUREMENT_REQUIRED.equals(status);
	}
}

