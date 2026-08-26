package ais.common.inventory.ledger;

import java.math.BigDecimal;

import ais.common.inventory.InventoryPostingPort;

/**
 * Repository ledger yang wajib mem-posting mutasi dan memperbarui saldo dalam
 * satu transaksi atomik. Implementasi database wajib menegakkan unique key
 * tenant + idempotency key, bukan melakukan check-then-insert tanpa constraint.
 */
public interface InventoryLedgerRepository extends InventoryPostingPort {
	InventoryLedgerRecord findByIdempotencyKey(Long tenantId, String idempotencyKey);
	BigDecimal getBalance(InventoryBalanceKey balanceKey);
}
