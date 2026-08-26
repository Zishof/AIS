package ais.common.inventory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Perintah kanonis untuk satu mutasi persediaan.
 *
 * Kontrak ini sengaja tidak mengetahui Hibernate maupun tabel fisik. Implementasi
 * writer wajib memakai idempotencyKey sebagai kunci unik agar retry tidak
 * menggandakan stok.
 */
public final class InventoryMovementCommand {

	private final Long tenantId;
	private final Long locationId;
	private final Long itemId;
	private final Long uomId;
	private final Long lotId;
	private final BigDecimal quantity;
	private final String sourceType;
	private final String sourceId;
	private final String eventType;
	private final String idempotencyKey;
	private final Date businessAt;

	public InventoryMovementCommand(Long tenantId, Long locationId, Long itemId,
			Long uomId, Long lotId, BigDecimal quantity, String sourceType,
			String sourceId, String eventType, String idempotencyKey, Date businessAt) {
		this.tenantId = tenantId;
		this.locationId = locationId;
		this.itemId = itemId;
		this.uomId = uomId;
		this.lotId = lotId;
		this.quantity = quantity;
		this.sourceType = bersihkan(sourceType);
		this.sourceId = bersihkan(sourceId);
		this.eventType = bersihkan(eventType);
		this.idempotencyKey = bersihkan(idempotencyKey);
		this.businessAt = salin(businessAt);
	}

	public List<String> validate() {
		List<String> errors = new ArrayList<String>();
		if (tenantId == null) errors.add("tenantId wajib diisi");
		if (locationId == null) errors.add("locationId wajib diisi");
		if (itemId == null) errors.add("itemId wajib diisi");
		if (uomId == null) errors.add("uomId wajib diisi");
		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) errors.add("quantity tidak boleh kosong atau nol");
		if (sourceType.length() == 0) errors.add("sourceType wajib diisi");
		if (sourceId.length() == 0) errors.add("sourceId wajib diisi");
		if (eventType.length() == 0) errors.add("eventType wajib diisi");
		if (idempotencyKey.length() == 0) errors.add("idempotencyKey wajib diisi");
		if (businessAt == null) errors.add("businessAt wajib diisi");
		return Collections.unmodifiableList(errors);
	}

	public boolean isValid() {
		return validate().isEmpty();
	}

	public Long getTenantId() { return tenantId; }
	public Long getLocationId() { return locationId; }
	public Long getItemId() { return itemId; }
	public Long getUomId() { return uomId; }
	public Long getLotId() { return lotId; }
	public BigDecimal getQuantity() { return quantity; }
	public String getSourceType() { return sourceType; }
	public String getSourceId() { return sourceId; }
	public String getEventType() { return eventType; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public Date getBusinessAt() { return salin(businessAt); }

	private static String bersihkan(String value) {
		return value == null ? "" : value.trim();
	}

	private static Date salin(Date value) {
		return value == null ? null : new Date(value.getTime());
	}
}
