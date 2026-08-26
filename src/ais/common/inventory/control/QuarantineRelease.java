package ais.common.inventory.control;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Perintah pelepasan lot dari karantina tanpa menambah kuantitas stok. */
public final class QuarantineRelease {
	public static final String APPROVED = "APPROVED";

	private final String releaseId;
	private final String status;
	private final Long tenantId;
	private final Long locationId;
	private final Long itemId;
	private final Long uomId;
	private final Long lotId;
	private final BigDecimal quantity;
	private final Date businessAt;
	private final Date expiryAt;
	private final String reason;

	public QuarantineRelease(String releaseId, String status, Long tenantId,
			Long locationId, Long itemId, Long uomId, Long lotId, BigDecimal quantity,
			Date businessAt, Date expiryAt, String reason) {
		this.releaseId = clean(releaseId);
		this.status = clean(status).toUpperCase(Locale.ENGLISH);
		this.tenantId = tenantId;
		this.locationId = locationId;
		this.itemId = itemId;
		this.uomId = uomId;
		this.lotId = lotId;
		this.quantity = quantity;
		this.businessAt = copy(businessAt);
		this.expiryAt = copy(expiryAt);
		this.reason = clean(reason);
	}

	public String getReleaseId() { return releaseId; }
	public String getStatus() { return status; }
	public Long getTenantId() { return tenantId; }
	public Long getLocationId() { return locationId; }
	public Long getItemId() { return itemId; }
	public Long getUomId() { return uomId; }
	public Long getLotId() { return lotId; }
	public BigDecimal getQuantity() { return quantity; }
	public Date getBusinessAt() { return copy(businessAt); }
	public Date getExpiryAt() { return copy(expiryAt); }
	public String getReason() { return reason; }
	public String getIdempotencyKey() { return "QUARANTINE_RELEASE:" + releaseId + ":RELEASE"; }

	public List<String> validate() {
		List<String> errors = new ArrayList<String>();
		if (releaseId.length() == 0) errors.add("releaseId wajib diisi");
		if (!APPROVED.equals(status)) errors.add("pelepasan karantina harus berstatus APPROVED");
		if (tenantId == null) errors.add("tenantId wajib diisi");
		if (locationId == null) errors.add("locationId wajib diisi");
		if (itemId == null) errors.add("itemId wajib diisi");
		if (uomId == null) errors.add("uomId wajib diisi");
		if (lotId == null) errors.add("lotId wajib diisi");
		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) errors.add("quantity wajib lebih besar dari nol");
		if (businessAt == null) errors.add("businessAt wajib diisi");
		if (expiryAt != null && businessAt != null && !expiryAt.after(businessAt)) errors.add("lot kedaluwarsa tidak boleh dilepas");
		if (reason.length() == 0) errors.add("alasan pelepasan wajib diisi");
		return errors;
	}

	private static Date copy(Date value) { return value == null ? null : new Date(value.getTime()); }
	private static String clean(String value) { return value == null ? "" : value.trim(); }
}
