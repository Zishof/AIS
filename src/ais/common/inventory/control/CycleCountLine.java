package ais.common.inventory.control;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Satu hasil hitung fisik yang akan dibandingkan dengan saldo ledger. */
public final class CycleCountLine {
	private final String lineId;
	private final Long tenantId;
	private final Long locationId;
	private final Long itemId;
	private final Long uomId;
	private final Long lotId;
	private final BigDecimal expectedQuantity;
	private final BigDecimal countedQuantity;
	private final String reason;

	public CycleCountLine(String lineId, Long tenantId, Long locationId, Long itemId,
			Long uomId, Long lotId, BigDecimal expectedQuantity,
			BigDecimal countedQuantity, String reason) {
		this.lineId = clean(lineId);
		this.tenantId = tenantId;
		this.locationId = locationId;
		this.itemId = itemId;
		this.uomId = uomId;
		this.lotId = lotId;
		this.expectedQuantity = expectedQuantity;
		this.countedQuantity = countedQuantity;
		this.reason = clean(reason);
	}

	public String getLineId() { return lineId; }
	public Long getTenantId() { return tenantId; }
	public Long getLocationId() { return locationId; }
	public Long getItemId() { return itemId; }
	public Long getUomId() { return uomId; }
	public Long getLotId() { return lotId; }
	public BigDecimal getExpectedQuantity() { return expectedQuantity; }
	public BigDecimal getCountedQuantity() { return countedQuantity; }
	public String getReason() { return reason; }
	public BigDecimal getVariance() {
		if (expectedQuantity == null || countedQuantity == null) return BigDecimal.ZERO;
		return countedQuantity.subtract(expectedQuantity);
	}

	public List<String> validate() {
		List<String> errors = new ArrayList<String>();
		if (lineId.length() == 0) errors.add("lineId wajib diisi");
		if (tenantId == null) errors.add("tenantId wajib diisi: " + lineId);
		if (locationId == null) errors.add("locationId wajib diisi: " + lineId);
		if (itemId == null) errors.add("itemId wajib diisi: " + lineId);
		if (uomId == null) errors.add("uomId wajib diisi: " + lineId);
		if (expectedQuantity == null || expectedQuantity.compareTo(BigDecimal.ZERO) < 0) {
			errors.add("expectedQuantity tidak boleh negatif: " + lineId);
		}
		if (countedQuantity == null || countedQuantity.compareTo(BigDecimal.ZERO) < 0) {
			errors.add("countedQuantity tidak boleh negatif: " + lineId);
		}
		if (getVariance().compareTo(BigDecimal.ZERO) != 0 && reason.length() == 0) {
			errors.add("alasan selisih wajib diisi: " + lineId);
		}
		return errors;
	}

	private static String clean(String value) { return value == null ? "" : value.trim(); }
}
