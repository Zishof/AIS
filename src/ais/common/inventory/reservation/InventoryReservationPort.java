package ais.common.inventory.reservation;

/** Port reservasi stok untuk allocation, picking, dan konsumsi. */
public interface InventoryReservationPort {
	InventoryReservationResult apply(InventoryReservationCommand command);
}
