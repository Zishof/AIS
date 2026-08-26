package ais.common.inventory.outbound;

import java.math.BigDecimal;

/** Hasil penerimaan outlet per alokasi, termasuk selisih rusak/ditolak. */
public final class OutletReceiptLine {
	private final String receiptLineId;
	private final OutboundLotAllocation allocation;
	private final BigDecimal acceptedQuantity;
	private final BigDecimal damagedQuantity;
	private final BigDecimal rejectedQuantity;

	public OutletReceiptLine(String receiptLineId, OutboundLotAllocation allocation,
			BigDecimal acceptedQuantity, BigDecimal damagedQuantity, BigDecimal rejectedQuantity) {
		this.receiptLineId = clean(receiptLineId);
		this.allocation = allocation;
		this.acceptedQuantity = zero(acceptedQuantity);
		this.damagedQuantity = zero(damagedQuantity);
		this.rejectedQuantity = zero(rejectedQuantity);
		if (this.receiptLineId.length() == 0 || allocation == null
				|| this.acceptedQuantity.signum() < 0 || this.damagedQuantity.signum() < 0
				|| this.rejectedQuantity.signum() < 0
				|| accountedQuantity().compareTo(allocation.getQuantity()) > 0) {
			throw new IllegalArgumentException("Rincian penerimaan outlet tidak valid");
		}
	}

	public String getReceiptLineId() { return receiptLineId; }
	public OutboundLotAllocation getAllocation() { return allocation; }
	public BigDecimal getAcceptedQuantity() { return acceptedQuantity; }
	public BigDecimal getDamagedQuantity() { return damagedQuantity; }
	public BigDecimal getRejectedQuantity() { return rejectedQuantity; }
	public BigDecimal accountedQuantity() { return acceptedQuantity.add(damagedQuantity).add(rejectedQuantity); }
	public boolean hasDiscrepancy() { return accountedQuantity().compareTo(allocation.getQuantity()) != 0 || damagedQuantity.signum() > 0 || rejectedQuantity.signum() > 0; }

	private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
	private static String clean(String value) { return value == null ? "" : value.trim(); }
}
