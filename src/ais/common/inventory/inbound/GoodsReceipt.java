package ais.common.inventory.inbound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Dokumen penerimaan gudang; tidak dengan sendirinya menambah stok tersedia. */
public final class GoodsReceipt {

	public static final String DRAFT = "DRAFT";
	public static final String IN_QC = "IN_QC";
	public static final String ACCEPTED = "ACCEPTED";
	public static final String PARTIALLY_ACCEPTED = "PARTIALLY_ACCEPTED";
	public static final String POSTED = "POSTED";
	public static final String REJECTED = "REJECTED";
	public static final String REVERSED = "REVERSED";

	private final Long tenantId;
	private final String receiptNumber;
	private final Long purchaseOrderId;
	private final Long bastId;
	private final Long vendorId;
	private final Date receivedAt;
	private final String status;
	private final List<GoodsReceiptLine> lines;

	public GoodsReceipt(Long tenantId, String receiptNumber, Long purchaseOrderId,
			Long bastId, Long vendorId, Date receivedAt, String status,
			List<GoodsReceiptLine> lines) {
		this.tenantId = tenantId;
		this.receiptNumber = bersihkan(receiptNumber);
		this.purchaseOrderId = purchaseOrderId;
		this.bastId = bastId;
		this.vendorId = vendorId;
		this.receivedAt = salin(receivedAt);
		this.status = bersihkan(status).toUpperCase();
		this.lines = lines == null
				? Collections.<GoodsReceiptLine>emptyList()
				: Collections.unmodifiableList(new ArrayList<GoodsReceiptLine>(lines));
	}

	public List<String> validate() {
		List<String> errors = new ArrayList<String>();
		if (tenantId == null) errors.add("tenantId wajib diisi");
		if (receiptNumber.length() == 0) errors.add("receiptNumber wajib diisi");
		if (purchaseOrderId == null && bastId == null) errors.add("purchaseOrderId atau bastId wajib diisi");
		if (vendorId == null) errors.add("vendorId wajib diisi");
		if (receivedAt == null) errors.add("receivedAt wajib diisi");
		if (lines.isEmpty()) errors.add("receipt wajib mempunyai minimal satu baris");
		if (!DRAFT.equals(status) && !IN_QC.equals(status) && !ACCEPTED.equals(status)
				&& !PARTIALLY_ACCEPTED.equals(status)
				&& !POSTED.equals(status) && !REJECTED.equals(status) && !REVERSED.equals(status)) {
			errors.add("status receipt tidak dikenal");
		}
		Set<Integer> numbers = new HashSet<Integer>();
		for (int i = 0; i < lines.size(); i++) {
			GoodsReceiptLine line = lines.get(i);
			if (line == null) {
				errors.add("baris receipt tidak boleh null");
				continue;
			}
			if (!numbers.add(Integer.valueOf(line.getLineNumber()))) {
				errors.add("lineNumber receipt tidak boleh ganda: " + line.getLineNumber());
			}
			if (vendorId != null && !vendorId.equals(line.getVendorId())) {
				errors.add("vendor baris harus sama dengan vendor receipt pada baris " + line.getLineNumber());
			}
			List<String> lineErrors = line.validate();
			for (int j = 0; j < lineErrors.size(); j++) {
				errors.add("baris " + line.getLineNumber() + ": " + lineErrors.get(j));
			}
		}
		return Collections.unmodifiableList(errors);
	}

	public GoodsReceiptLine findLine(int lineNumber) {
		for (int i = 0; i < lines.size(); i++) {
			GoodsReceiptLine line = lines.get(i);
			if (line != null && line.getLineNumber() == lineNumber) return line;
		}
		return null;
	}

	public Long getTenantId() { return tenantId; }
	public String getReceiptNumber() { return receiptNumber; }
	public Long getPurchaseOrderId() { return purchaseOrderId; }
	public Long getBastId() { return bastId; }
	public Long getVendorId() { return vendorId; }
	public Date getReceivedAt() { return salin(receivedAt); }
	public String getStatus() { return status; }
	public List<GoodsReceiptLine> getLines() { return lines; }

	private static String bersihkan(String value) { return value == null ? "" : value.trim(); }
	private static Date salin(Date value) { return value == null ? null : new Date(value.getTime()); }
}
