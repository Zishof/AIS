package ais.common.inventory.inbound;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Satu baris penerimaan fisik yang dapat ditelusuri kembali ke PO dan BAST. */
public final class GoodsReceiptLine {

	public static final String PENDING = "PENDING";
	public static final String ACCEPTED = "ACCEPTED";
	public static final String PARTIAL = "PARTIAL";
	public static final String REJECTED = "REJECTED";
	public static final String QUARANTINED = "QUARANTINED";

	private final int lineNumber;
	private final Long purchaseOrderDetailId;
	private final Long bastDetailId;
	private final Long vendorId;
	private final Long itemId;
	private final Long uomId;
	private final BigDecimal expectedQuantity;
	private final BigDecimal receivedQuantity;
	private final BigDecimal acceptedQuantity;
	private final BigDecimal rejectedQuantity;
	private final BigDecimal quarantinedQuantity;
	private final Long lotId;
	private final String lotCode;
	private final Date expiryDate;
	private final Long receivingLocationId;
	private final String qualityStatus;

	public GoodsReceiptLine(int lineNumber, Long purchaseOrderDetailId, Long bastDetailId,
			Long vendorId, Long itemId, Long uomId, BigDecimal expectedQuantity,
			BigDecimal receivedQuantity, BigDecimal acceptedQuantity,
			BigDecimal rejectedQuantity, BigDecimal quarantinedQuantity, Long lotId,
			String lotCode, Date expiryDate, Long receivingLocationId, String qualityStatus) {
		this.lineNumber = lineNumber;
		this.purchaseOrderDetailId = purchaseOrderDetailId;
		this.bastDetailId = bastDetailId;
		this.vendorId = vendorId;
		this.itemId = itemId;
		this.uomId = uomId;
		this.expectedQuantity = nolJikaNull(expectedQuantity);
		this.receivedQuantity = nolJikaNull(receivedQuantity);
		this.acceptedQuantity = nolJikaNull(acceptedQuantity);
		this.rejectedQuantity = nolJikaNull(rejectedQuantity);
		this.quarantinedQuantity = nolJikaNull(quarantinedQuantity);
		this.lotId = lotId;
		this.lotCode = bersihkan(lotCode);
		this.expiryDate = salin(expiryDate);
		this.receivingLocationId = receivingLocationId;
		this.qualityStatus = bersihkan(qualityStatus).toUpperCase();
	}

	public List<String> validate() {
		List<String> errors = new ArrayList<String>();
		if (lineNumber <= 0) errors.add("lineNumber harus lebih besar dari nol");
		if (purchaseOrderDetailId == null && bastDetailId == null) {
			errors.add("purchaseOrderDetailId atau bastDetailId wajib diisi");
		}
		if (vendorId == null) errors.add("vendorId wajib diisi");
		if (itemId == null) errors.add("itemId wajib diisi");
		if (uomId == null) errors.add("uomId wajib diisi");
		if (receivingLocationId == null) errors.add("receivingLocationId wajib diisi");
		if (negatif(expectedQuantity) || negatif(receivedQuantity) || negatif(acceptedQuantity)
				|| negatif(rejectedQuantity) || negatif(quarantinedQuantity)) {
			errors.add("seluruh quantity tidak boleh negatif");
		}
		BigDecimal distributed = acceptedQuantity.add(rejectedQuantity).add(quarantinedQuantity);
		if (receivedQuantity.compareTo(distributed) != 0) {
			errors.add("receivedQuantity harus sama dengan accepted + rejected + quarantined");
		}
		if (acceptedQuantity.compareTo(BigDecimal.ZERO) > 0 && lotId == null && lotCode.length() == 0) {
			errors.add("barang diterima wajib mempunyai lotId atau lotCode");
		}
		if (!PENDING.equals(qualityStatus) && !ACCEPTED.equals(qualityStatus)
				&& !PARTIAL.equals(qualityStatus) && !REJECTED.equals(qualityStatus)
				&& !QUARANTINED.equals(qualityStatus)) {
			errors.add("qualityStatus tidak dikenal");
		}
		if (acceptedQuantity.compareTo(BigDecimal.ZERO) > 0
				&& !ACCEPTED.equals(qualityStatus) && !PARTIAL.equals(qualityStatus)) {
			errors.add("acceptedQuantity hanya boleh pada status ACCEPTED atau PARTIAL");
		}
		return Collections.unmodifiableList(errors);
	}

	public BigDecimal getQuantityVariance() { return receivedQuantity.subtract(expectedQuantity); }
	public int getLineNumber() { return lineNumber; }
	public Long getPurchaseOrderDetailId() { return purchaseOrderDetailId; }
	public Long getBastDetailId() { return bastDetailId; }
	public Long getVendorId() { return vendorId; }
	public Long getItemId() { return itemId; }
	public Long getUomId() { return uomId; }
	public BigDecimal getExpectedQuantity() { return expectedQuantity; }
	public BigDecimal getReceivedQuantity() { return receivedQuantity; }
	public BigDecimal getAcceptedQuantity() { return acceptedQuantity; }
	public BigDecimal getRejectedQuantity() { return rejectedQuantity; }
	public BigDecimal getQuarantinedQuantity() { return quarantinedQuantity; }
	public Long getLotId() { return lotId; }
	public String getLotCode() { return lotCode; }
	public Date getExpiryDate() { return salin(expiryDate); }
	public Long getReceivingLocationId() { return receivingLocationId; }
	public String getQualityStatus() { return qualityStatus; }

	private static BigDecimal nolJikaNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private static boolean negatif(BigDecimal value) {
		return value.compareTo(BigDecimal.ZERO) < 0;
	}

	private static String bersihkan(String value) {
		return value == null ? "" : value.trim();
	}

	private static Date salin(Date value) {
		return value == null ? null : new Date(value.getTime());
	}
}
