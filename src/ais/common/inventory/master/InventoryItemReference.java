package ais.common.inventory.master;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Referensi item lintas domain yang tidak menyamakan primary key legacy.
 */
public final class InventoryItemReference {

	public static final String KOPERASI_PRODUK = "KOPERASI_PRODUK";
	public static final String ASSET_MASTER_ASSET = "ASSET_MASTER_ASSET";
	public static final String SIRS_ITEM = "SIRS_ITEM";

	private final String tenantKey;
	private final String sourceType;
	private final Long sourceId;
	private final Long sourceScopeId;
	private final Long canonicalItemId;

	public InventoryItemReference(String tenantKey, String sourceType, Long sourceId,
			Long sourceScopeId, Long canonicalItemId) {
		this.tenantKey = bersihkan(tenantKey);
		this.sourceType = bersihkan(sourceType);
		this.sourceId = sourceId;
		this.sourceScopeId = sourceScopeId;
		this.canonicalItemId = canonicalItemId;
	}

	public List<String> validate() {
		List<String> errors = new ArrayList<String>();
		if (tenantKey.length() == 0) errors.add("tenantKey wajib diisi");
		if (!tipeDikenal(sourceType)) errors.add("sourceType tidak dikenal");
		if (sourceId == null || sourceId.longValue() <= 0L) errors.add("sourceId harus positif");
		if (sourceScopeId != null && sourceScopeId.longValue() <= 0L) errors.add("sourceScopeId harus positif");
		if (canonicalItemId != null && canonicalItemId.longValue() <= 0L) errors.add("canonicalItemId harus positif");
		return Collections.unmodifiableList(errors);
	}

	public boolean isValid() {
		return validate().isEmpty();
	}

	public String legacyKey() {
		return tenantKey + ":" + sourceType + ":" + sourceId + ":" + (sourceScopeId == null ? "GLOBAL" : sourceScopeId.toString());
	}

	public String getTenantKey() { return tenantKey; }
	public String getSourceType() { return sourceType; }
	public Long getSourceId() { return sourceId; }
	public Long getSourceScopeId() { return sourceScopeId; }
	public Long getCanonicalItemId() { return canonicalItemId; }

	private static boolean tipeDikenal(String value) {
		return KOPERASI_PRODUK.equals(value) || ASSET_MASTER_ASSET.equals(value) || SIRS_ITEM.equals(value);
	}

	private static String bersihkan(String value) {
		return value == null ? "" : value.trim().toUpperCase();
	}
}
