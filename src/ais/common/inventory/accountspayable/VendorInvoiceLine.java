package ais.common.inventory.accountspayable;

import java.math.BigDecimal;

/** Rincian tagihan; referensi PO/BAST boleh kosong untuk invoice non-PO. */
public final class VendorInvoiceLine {
	private final String lineId;
	private final Long poDetailId;
	private final Long receiptDetailId;
	private final BigDecimal quantity;
	private final BigDecimal unitPrice;
	private final BigDecimal taxAmount;

	public VendorInvoiceLine(String lineId, Long poDetailId, Long receiptDetailId,
			BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxAmount) {
		if (lineId == null || lineId.trim().length() == 0) throw new IllegalArgumentException("lineId wajib");
		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("quantity harus positif");
		if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("unitPrice tidak valid");
		if (taxAmount == null || taxAmount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("taxAmount tidak valid");
		this.lineId = lineId;
		this.poDetailId = poDetailId;
		this.receiptDetailId = receiptDetailId;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.taxAmount = taxAmount;
	}

	public String getLineId() { return lineId; }
	public Long getPoDetailId() { return poDetailId; }
	public Long getReceiptDetailId() { return receiptDetailId; }
	public BigDecimal getQuantity() { return quantity; }
	public BigDecimal getUnitPrice() { return unitPrice; }
	public BigDecimal getTaxAmount() { return taxAmount; }
	public BigDecimal getLineAmount() { return quantity.multiply(unitPrice).add(taxAmount); }
}
