package ais.common.inventory.shadow;

/** Tujuan audit hasil shadow-write; tidak boleh menjadi bagian transaksi legacy. */
public interface InventoryShadowAuditPort {
	void record(InventoryShadowWriteResult result);
}
