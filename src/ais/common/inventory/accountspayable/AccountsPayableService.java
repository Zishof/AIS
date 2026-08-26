package ais.common.inventory.accountspayable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Layanan domain Fase 10: invoice, 3-way match, pembayaran, credit note, dan posting. */
public final class AccountsPayableService {
	private final AccountsPayablePort repository;
	private final JournalPostingPort journal;
	private final PeriodLockPort periodLock;

	public AccountsPayableService(AccountsPayablePort repository, JournalPostingPort journal,
			PeriodLockPort periodLock) {
		if (repository == null || journal == null || periodLock == null) throw new IllegalArgumentException("port wajib");
		this.repository = repository; this.journal = journal; this.periodLock = periodLock;
	}

	public AccountsPayableOperationResult registerInvoice(VendorInvoice invoice) {
		if (invoice == null) return failed("Invoice wajib");
		if (repository.vendorInvoiceExists(invoice.getTenantId(), invoice.getVendorId(), invoice.getVendorInvoiceNumber())) {
			return failed("Nomor invoice vendor sudah terdaftar pada tenant dan vendor yang sama");
		}
		repository.saveInvoice(invoice);
		return ok("Invoice tersimpan");
	}

	public ThreeWayMatchResult match(VendorInvoice invoice,
			Map<String, BigDecimal> orderedQuantities, Map<String, BigDecimal> receivedQuantities,
			BigDecimal quantityTolerance) {
		List<String> messages = new ArrayList<String>();
		BigDecimal tolerance = quantityTolerance == null ? BigDecimal.ZERO : quantityTolerance.abs();
		if (invoice == null) {
			messages.add("Invoice wajib");
			return new ThreeWayMatchResult(ThreeWayMatchResult.EXCEPTION, messages);
		}
		for (VendorInvoiceLine line : invoice.getLines()) {
			if (line.getPoDetailId() == null) messages.add(line.getLineId() + ": referensi PO tidak tersedia");
			if (line.getReceiptDetailId() == null) messages.add(line.getLineId() + ": referensi BAST/penerimaan tidak tersedia");
			BigDecimal ordered = orderedQuantities == null ? null : orderedQuantities.get(line.getLineId());
			BigDecimal received = receivedQuantities == null ? null : receivedQuantities.get(line.getLineId());
			if (ordered == null) messages.add(line.getLineId() + ": kuantitas PO tidak tersedia");
			else if (line.getQuantity().subtract(ordered).abs().compareTo(tolerance) > 0) messages.add(line.getLineId() + ": kuantitas invoice berbeda dari PO");
			if (received == null) messages.add(line.getLineId() + ": kuantitas penerimaan tidak tersedia");
			else if (line.getQuantity().subtract(received).abs().compareTo(tolerance) > 0) messages.add(line.getLineId() + ": kuantitas invoice berbeda dari penerimaan");
		}
		ThreeWayMatchResult result = new ThreeWayMatchResult(messages.isEmpty()
				? ThreeWayMatchResult.MATCHED : ThreeWayMatchResult.EXCEPTION, messages);
		invoice.setStatus(result.isMatched() ? AccountsPayableStatus.MATCHED : AccountsPayableStatus.MATCH_EXCEPTION);
		repository.saveMatch(invoice.getInvoiceId(), result);
		repository.updateInvoice(invoice);
		return result;
	}

	public AccountsPayableOperationResult approve(VendorInvoice invoice) {
		if (invoice == null || !AccountsPayableStatus.MATCHED.equals(invoice.getStatus())) return failed("Hanya invoice matched yang dapat disetujui");
		invoice.setStatus(AccountsPayableStatus.APPROVED);
		repository.updateInvoice(invoice);
		return ok("Invoice disetujui");
	}

	public AccountsPayableOperationResult allocatePayment(VendorInvoice invoice, PaymentAllocation allocation) {
		if (invoice == null || allocation == null || !invoice.getInvoiceId().equals(allocation.getInvoiceId())) return failed("Alokasi pembayaran tidak sesuai invoice");
		if (repository.idempotencyKeyExists(allocation.getIdempotencyKey())) return replay("Pembayaran sudah dialokasikan");
		if (!AccountsPayableStatus.APPROVED.equals(invoice.getStatus()) && !AccountsPayableStatus.PARTIALLY_PAID.equals(invoice.getStatus())) return failed("Invoice belum disetujui");
		if (allocation.getAmount().compareTo(invoice.getOpenAmount()) > 0) return failed("Pembayaran melebihi saldo terbuka");
		invoice.addPayment(allocation.getAmount());
		invoice.setStatus(invoice.getOpenAmount().compareTo(BigDecimal.ZERO) == 0 ? AccountsPayableStatus.PAID : AccountsPayableStatus.PARTIALLY_PAID);
		repository.savePayment(allocation); repository.updateInvoice(invoice);
		return ok("Pembayaran dialokasikan");
	}

	public AccountsPayableOperationResult applyCreditNote(VendorInvoice invoice, CreditNote creditNote) {
		if (invoice == null || creditNote == null || !invoice.getInvoiceId().equals(creditNote.getInvoiceId())) return failed("Credit note tidak sesuai invoice");
		if (repository.idempotencyKeyExists(creditNote.getIdempotencyKey())) return replay("Credit note sudah diterapkan");
		if (!AccountsPayableStatus.APPROVED.equals(invoice.getStatus())
				&& !AccountsPayableStatus.PARTIALLY_PAID.equals(invoice.getStatus())) return failed("Invoice belum disetujui");
		if (creditNote.getAmount().compareTo(invoice.getOpenAmount()) > 0) return failed("Credit note melebihi saldo terbuka");
		invoice.addCredit(creditNote.getAmount());
		if (invoice.getOpenAmount().compareTo(BigDecimal.ZERO) == 0) invoice.setStatus(AccountsPayableStatus.PAID);
		repository.saveCreditNote(creditNote); repository.updateInvoice(invoice);
		return ok("Credit note diterapkan");
	}

	public AccountsPayableOperationResult postInvoice(VendorInvoice invoice, Date postingDate) {
		if (invoice == null || postingDate == null) return failed("Invoice dan tanggal posting wajib");
		if (!AccountsPayableStatus.APPROVED.equals(invoice.getStatus())
				&& !AccountsPayableStatus.PARTIALLY_PAID.equals(invoice.getStatus())
				&& !AccountsPayableStatus.PAID.equals(invoice.getStatus())) return failed("Invoice belum disetujui");
		return post(invoice.getTenantId(), "AP_INVOICE", invoice.getInvoiceId(), "APPROVED",
				invoice.getGrossAmount(), postingDate, "AP-INVOICE-" + invoice.getInvoiceId());
	}

	public AccountsPayableOperationResult postPayment(VendorInvoice invoice, PaymentAllocation allocation) {
		if (invoice == null || allocation == null) return failed("Invoice dan pembayaran wajib");
		return post(invoice.getTenantId(), "AP_PAYMENT", allocation.getAllocationId(), "PAID",
				allocation.getAmount(), allocation.getPaidAt(), "AP-PAYMENT-" + allocation.getIdempotencyKey());
	}

	public AccountsPayableOperationResult reverse(long tenantId, String sourceType, String sourceId,
			BigDecimal amount, Date postingDate, String reversalId) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return failed("Nilai reversal harus positif");
		return post(tenantId, sourceType, sourceId, "REVERSAL", amount.negate(), postingDate,
				"AP-REVERSAL-" + reversalId);
	}

	private AccountsPayableOperationResult post(long tenantId, String sourceType, String sourceId,
			String eventType, BigDecimal amount, Date postingDate, String idempotencyKey) {
		if (periodLock.isLocked(tenantId, postingDate)) return failed("Periode akuntansi terkunci");
		if (journal.alreadyPosted(sourceType, sourceId, eventType)) return replay("Jurnal sumber sudah diposting");
		journal.post(tenantId, sourceType, sourceId, eventType, amount, postingDate, idempotencyKey);
		return ok("Jurnal diposting");
	}

	private AccountsPayableOperationResult ok(String message) { return new AccountsPayableOperationResult(true, false, message); }
	private AccountsPayableOperationResult replay(String message) { return new AccountsPayableOperationResult(true, true, message); }
	private AccountsPayableOperationResult failed(String message) { return new AccountsPayableOperationResult(false, false, message); }
}
