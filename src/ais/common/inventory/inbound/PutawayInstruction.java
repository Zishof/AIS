package ais.common.inventory.inbound;

import java.math.BigDecimal;
import java.util.Date;

/** Instruksi pemindahan barang yang telah lolos QC ke lokasi stok tersedia. */
public final class PutawayInstruction {
	private final String detailId;
	private final int receiptLineNumber;
	private final Long targetLocationId;
	private final BigDecimal quantity;
	private final Date completedAt;
	private final boolean completed;

	public PutawayInstruction(String detailId, int receiptLineNumber, Long targetLocationId,
			BigDecimal quantity, Date completedAt, boolean completed) {
		this.detailId = detailId == null ? "" : detailId.trim();
		this.receiptLineNumber = receiptLineNumber;
		this.targetLocationId = targetLocationId;
		this.quantity = quantity;
		this.completedAt = completedAt == null ? null : new Date(completedAt.getTime());
		this.completed = completed;
	}

	public String getDetailId() { return detailId; }
	public int getReceiptLineNumber() { return receiptLineNumber; }
	public Long getTargetLocationId() { return targetLocationId; }
	public BigDecimal getQuantity() { return quantity; }
	public Date getCompletedAt() { return completedAt == null ? null : new Date(completedAt.getTime()); }
	public boolean isCompleted() { return completed; }
}
