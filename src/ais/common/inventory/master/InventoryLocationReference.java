package ais.common.inventory.master;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Referensi lokasi yang menjaga perbedaan outlet, gudang, lokasi, dan bin. */
public final class InventoryLocationReference {

	public static final String KOPERASI_TOKO = "KOPERASI_TOKO";
	public static final String SIRS_GUDANG = "SIRS_GUDANG";
	public static final String ASSET_LOKASI = "ASSET_LOKASI";
	public static final String WAREHOUSE_BIN = "WAREHOUSE_BIN";

	private final String tenantKey;
	private final String sourceType;
	private final Long sourceId;
	private final Long businessLocationId;

	public InventoryLocationReference(String tenantKey, String sourceType,
			Long sourceId, Long businessLocationId) {
		this.tenantKey = bersihkan(tenantKey);
		this.sourceType = bersihkan(sourceType);
		this.sourceId = sourceId;
		this.businessLocationId = businessLocationId;
	}

	public List<String> validate() {
		List<String> errors = new ArrayList<String>();
		if (tenantKey.length() == 0) errors.add("tenantKey wajib diisi");
		if (!tipeDikenal(sourceType)) errors.add("sourceType lokasi tidak dikenal");
		if (sourceId == null || sourceId.longValue() <= 0L) errors.add("sourceId lokasi harus positif");
		if (businessLocationId != null && businessLocationId.longValue() <= 0L) errors.add("businessLocationId harus positif");
		return Collections.unmodifiableList(errors);
	}

	public boolean isValid() { return validate().isEmpty(); }

	public String legacyKey() {
		return tenantKey + ":" + sourceType + ":" + sourceId;
	}

	public String getTenantKey() { return tenantKey; }
	public String getSourceType() { return sourceType; }
	public Long getSourceId() { return sourceId; }
	public Long getBusinessLocationId() { return businessLocationId; }

	private static boolean tipeDikenal(String value) {
		return KOPERASI_TOKO.equals(value) || SIRS_GUDANG.equals(value)
				|| ASSET_LOKASI.equals(value) || WAREHOUSE_BIN.equals(value);
	}

	private static String bersihkan(String value) {
		return value == null ? "" : value.trim().toUpperCase();
	}
}
