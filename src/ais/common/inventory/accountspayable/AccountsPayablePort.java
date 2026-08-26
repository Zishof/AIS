package ais.common.inventory.accountspayable;

public interface AccountsPayablePort {
	boolean vendorInvoiceExists(long tenantId, long vendorId, String vendorInvoiceNumber);
	boolean idempotencyKeyExists(String idempotencyKey);
	void saveInvoice(VendorInvoice invoice);
	void saveMatch(String invoiceId, ThreeWayMatchResult result);
	void savePayment(PaymentAllocation allocation);
	void saveCreditNote(CreditNote creditNote);
	void updateInvoice(VendorInvoice invoice);
}
