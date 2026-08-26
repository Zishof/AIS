package ais.common.inventory.accountspayable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Aggregate tagihan vendor; bukan alias SaldoAwalMasterAsset. */
public final class VendorInvoice {
	private final String invoiceId;
	private final long tenantId;
	private final long vendorId;
	private final String vendorInvoiceNumber;
	private final String currencyCode;
	private final Date invoiceDate;
	private final Date dueDate;
	private final BigDecimal grossAmount;
	private final List<VendorInvoiceLine> lines;
	private String status;
	private BigDecimal paidAmount = BigDecimal.ZERO;
	private BigDecimal creditAmount = BigDecimal.ZERO;

	public VendorInvoice(String invoiceId, long tenantId, long vendorId, String vendorInvoiceNumber,
			String currencyCode, Date invoiceDate, Date dueDate, BigDecimal grossAmount,
			List<VendorInvoiceLine> lines) {
		if (invoiceId == null || invoiceId.trim().length() == 0) throw new IllegalArgumentException("invoiceId wajib");
		if (tenantId <= 0 || vendorId <= 0) throw new IllegalArgumentException("tenant/vendor wajib");
		if (vendorInvoiceNumber == null || vendorInvoiceNumber.trim().length() == 0) throw new IllegalArgumentException("nomor invoice wajib");
		if (currencyCode == null || currencyCode.trim().length() == 0) throw new IllegalArgumentException("mata uang wajib");
		if (invoiceDate == null || dueDate == null || dueDate.before(invoiceDate)) throw new IllegalArgumentException("tanggal invoice tidak valid");
		if (grossAmount == null || grossAmount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("grossAmount harus positif");
		if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("rincian invoice wajib");
		BigDecimal lineTotal = BigDecimal.ZERO;
		for (VendorInvoiceLine line : lines) lineTotal = lineTotal.add(line.getLineAmount());
		if (lineTotal.compareTo(grossAmount) != 0) throw new IllegalArgumentException("total rincian tidak sama dengan total invoice");
		this.invoiceId = invoiceId;
		this.tenantId = tenantId;
		this.vendorId = vendorId;
		this.vendorInvoiceNumber = vendorInvoiceNumber.trim();
		this.currencyCode = currencyCode.trim();
		this.invoiceDate = new Date(invoiceDate.getTime());
		this.dueDate = new Date(dueDate.getTime());
		this.grossAmount = grossAmount;
		this.lines = Collections.unmodifiableList(new ArrayList<VendorInvoiceLine>(lines));
		this.status = AccountsPayableStatus.DRAFT;
	}

	public String getInvoiceId() { return invoiceId; }
	public long getTenantId() { return tenantId; }
	public long getVendorId() { return vendorId; }
	public String getVendorInvoiceNumber() { return vendorInvoiceNumber; }
	public String getCurrencyCode() { return currencyCode; }
	public Date getInvoiceDate() { return new Date(invoiceDate.getTime()); }
	public Date getDueDate() { return new Date(dueDate.getTime()); }
	public BigDecimal getGrossAmount() { return grossAmount; }
	public List<VendorInvoiceLine> getLines() { return lines; }
	public String getStatus() { return status; }
	public BigDecimal getPaidAmount() { return paidAmount; }
	public BigDecimal getCreditAmount() { return creditAmount; }
	public BigDecimal getOpenAmount() { return grossAmount.subtract(paidAmount).subtract(creditAmount); }
	void setStatus(String status) { this.status = status; }
	void addPayment(BigDecimal amount) { paidAmount = paidAmount.add(amount); }
	void addCredit(BigDecimal amount) { creditAmount = creditAmount.add(amount); }
}
