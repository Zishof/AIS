package ais.common.inventory.ledger;

/**
 * Kunci kanonis saldo persediaan. Lot boleh kosong untuk barang yang tidak
 * dikelola per lot, tetapi tenant, lokasi, dan item selalu wajib.
 */
public final class InventoryBalanceKey {

	private final Long tenantId;
	private final Long locationId;
	private final Long itemId;
	private final Long lotId;

	public InventoryBalanceKey(Long tenantId, Long locationId, Long itemId, Long lotId) {
		if (tenantId == null) throw new IllegalArgumentException("tenantId wajib diisi");
		if (locationId == null) throw new IllegalArgumentException("locationId wajib diisi");
		if (itemId == null) throw new IllegalArgumentException("itemId wajib diisi");
		this.tenantId = tenantId;
		this.locationId = locationId;
		this.itemId = itemId;
		this.lotId = lotId;
	}

	public Long getTenantId() { return tenantId; }
	public Long getLocationId() { return locationId; }
	public Long getItemId() { return itemId; }
	public Long getLotId() { return lotId; }

	public boolean equals(Object value) {
		if (this == value) return true;
		if (!(value instanceof InventoryBalanceKey)) return false;
		InventoryBalanceKey other = (InventoryBalanceKey) value;
		return tenantId.equals(other.tenantId)
				&& locationId.equals(other.locationId)
				&& itemId.equals(other.itemId)
				&& sama(lotId, other.lotId);
	}

	public int hashCode() {
		int result = tenantId.hashCode();
		result = 31 * result + locationId.hashCode();
		result = 31 * result + itemId.hashCode();
		result = 31 * result + (lotId == null ? 0 : lotId.hashCode());
		return result;
	}

	private static boolean sama(Object left, Object right) {
		return left == null ? right == null : left.equals(right);
	}
}
