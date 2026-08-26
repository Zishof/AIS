package ais.common.inventory.shadow;

import java.math.BigDecimal;

import ais.common.inventory.ledger.InventoryBalanceKey;

/** Pembaca saldo legacy. Implementasi tidak boleh melakukan mutasi. */
public interface InventoryLegacyBalancePort {
	BigDecimal getBalance(InventoryBalanceKey balanceKey);
}
