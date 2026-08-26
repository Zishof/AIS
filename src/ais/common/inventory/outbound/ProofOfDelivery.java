package ais.common.inventory.outbound;

import java.util.Date;

/** Bukti serah-terima pengiriman; tidak memposting stok. */
public final class ProofOfDelivery {
	private final String proofId;
	private final String shipmentId;
	private final String receivedBy;
	private final Date deliveredAt;
	private final String note;

	public ProofOfDelivery(String proofId, String shipmentId, String receivedBy,
			Date deliveredAt, String note) {
		this.proofId = clean(proofId);
		this.shipmentId = clean(shipmentId);
		this.receivedBy = clean(receivedBy);
		this.deliveredAt = copy(deliveredAt);
		this.note = clean(note);
		if (this.proofId.length() == 0 || this.shipmentId.length() == 0
				|| this.receivedBy.length() == 0 || deliveredAt == null) {
			throw new IllegalArgumentException("Proof of delivery tidak lengkap");
		}
	}

	public String getProofId() { return proofId; }
	public String getShipmentId() { return shipmentId; }
	public String getReceivedBy() { return receivedBy; }
	public Date getDeliveredAt() { return copy(deliveredAt); }
	public String getNote() { return note; }

	private static String clean(String value) { return value == null ? "" : value.trim(); }
	private static Date copy(Date value) { return value == null ? null : new Date(value.getTime()); }
}
