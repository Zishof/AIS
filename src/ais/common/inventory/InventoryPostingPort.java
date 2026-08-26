package ais.common.inventory;

/** Port tunggal yang harus dipakai adapter PR/PO/BAST/WMS/produksi/POS. */
public interface InventoryPostingPort {
	InventoryMovementResult post(InventoryMovementCommand command);
}
