package ais.common.inventory.master;

/** Batas aplikasi untuk memetakan identitas legacy ke identitas kanonis. */
public interface InventoryIdentityResolver {

	Long resolveCanonicalItemId(InventoryItemReference reference);

	Long resolveBusinessLocationId(InventoryLocationReference reference);
}
