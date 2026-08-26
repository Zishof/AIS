package ais.common.inventory.outbound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Penerimaan outlet terpisah dari BAST pengadaan vendor. */
public final class OutletReceipt {
	private final String receiptId;
	private final Shipment shipment;
	private final Date receivedAt;
	private final List<OutletReceiptLine> lines;

	public OutletReceipt(String receiptId, Shipment shipment, Date receivedAt,
			List<OutletReceiptLine> lines) {
		this.receiptId = clean(receiptId);
		this.shipment = shipment;
		this.receivedAt = copy(receivedAt);
		this.lines = lines == null ? Collections.<OutletReceiptLine>emptyList()
				: Collections.unmodifiableList(new ArrayList<OutletReceiptLine>(lines));
		if (this.receiptId.length() == 0 || shipment == null || receivedAt == null || this.lines.isEmpty()) {
			throw new IllegalArgumentException("Penerimaan outlet tidak lengkap");
		}
	}

	public String getReceiptId() { return receiptId; }
	public Shipment getShipment() { return shipment; }
	public Date getReceivedAt() { return copy(receivedAt); }
	public List<OutletReceiptLine> getLines() { return lines; }
	public boolean hasDiscrepancy() {
		for (int i = 0; i < lines.size(); i++) if (lines.get(i).hasDiscrepancy()) return true;
		return false;
	}

	private static String clean(String value) { return value == null ? "" : value.trim(); }
	private static Date copy(Date value) { return value == null ? null : new Date(value.getTime()); }
}
