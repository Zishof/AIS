package ais.common.inventory.replenishment;

import java.math.BigDecimal;

import ais.common.inventory.ledger.InventoryBalanceKey;

/** Pembaca stok tersedia; implementasi tidak boleh melakukan mutasi. */
public interface ReplenishmentAvailabilityPort {
	BigDecimal findAvailableQuantity(InventoryBalanceKey balanceKey);
}

