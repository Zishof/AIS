package ais.common.inventory.control;

/** Adapter persistensi untuk perubahan status lot; tidak mengubah saldo kuantitas. */
public interface InventoryLotControlPort {
	InventoryLotControlResult releaseQuarantine(QuarantineRelease release);
}
