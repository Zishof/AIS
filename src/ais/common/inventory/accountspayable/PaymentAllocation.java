package ais.common.inventory.accountspayable;

import java.math.BigDecimal;
import java.util.Date;

/** Alokasi pembayaran yang menjembatani invoice dengan proses transfer legacy. */
public final class PaymentAllocation {
	private final String allocationId;
	private final String invoiceId;
	private final Long legacyTransferId;
	private final Long legacyTransferProposalId;
	private final BigDecimal amount;
	private final Date paidAt;
	private final String idempotencyKey;

	public PaymentAllocation(String allocationId, String invoiceId, Long legacyTransferId,
			Long legacyTransferProposalId, BigDecimal amount, Date paidAt, String idempotencyKey) {
		if (allocationId == null || invoiceId == null || idempotencyKey == null) throw new IllegalArgumentException("identitas alokasi wajib");
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("nilai pembayaran harus positif");
		if (paidAt == null) throw new IllegalArgumentException("tanggal bayar wajib");
		this.allocationId = allocationId;
		this.invoiceId = invoiceId;
		this.legacyTransferId = legacyTransferId;
		this.legacyTransferProposalId = legacyTransferProposalId;
		this.amount = amount;
		this.paidAt = new Date(paidAt.getTime());
		this.idempotencyKey = idempotencyKey;
	}
	public String getAllocationId() { return allocationId; }
	public String getInvoiceId() { return invoiceId; }
	public Long getLegacyTransferId() { return legacyTransferId; }
	public Long getLegacyTransferProposalId() { return legacyTransferProposalId; }
	public BigDecimal getAmount() { return amount; }
	public Date getPaidAt() { return new Date(paidAt.getTime()); }
	public String getIdempotencyKey() { return idempotencyKey; }
}
