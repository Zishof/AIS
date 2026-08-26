package ais.common.inventory.accountspayable;

import java.math.BigDecimal;
import java.util.Date;

public final class CreditNote {
	private final String creditNoteId;
	private final String invoiceId;
	private final BigDecimal amount;
	private final Date issuedAt;
	private final String reason;
	private final String idempotencyKey;
	public CreditNote(String creditNoteId, String invoiceId, BigDecimal amount, Date issuedAt,
			String reason, String idempotencyKey) {
		if (creditNoteId == null || invoiceId == null || idempotencyKey == null) throw new IllegalArgumentException("identitas credit note wajib");
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("nilai credit note harus positif");
		if (issuedAt == null) throw new IllegalArgumentException("tanggal credit note wajib");
		this.creditNoteId = creditNoteId; this.invoiceId = invoiceId; this.amount = amount;
		this.issuedAt = new Date(issuedAt.getTime()); this.reason = reason; this.idempotencyKey = idempotencyKey;
	}
	public String getCreditNoteId() { return creditNoteId; }
	public String getInvoiceId() { return invoiceId; }
	public BigDecimal getAmount() { return amount; }
	public Date getIssuedAt() { return new Date(issuedAt.getTime()); }
	public String getReason() { return reason; }
	public String getIdempotencyKey() { return idempotencyKey; }
}
