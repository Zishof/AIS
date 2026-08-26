package ais.common.inventory.shadow;

import java.math.BigDecimal;

import ais.common.inventory.ledger.InventoryBalanceKey;
import ais.common.inventory.ledger.InventoryLedgerRepository;

/** Rekonsiliasi read-only. Layanan ini tidak pernah memperbaiki saldo otomatis. */
public final class InventoryReconciliationService {

	private final InventoryLegacyBalancePort legacyBalancePort;
	private final InventoryLedgerRepository ledgerRepository;

	public InventoryReconciliationService(InventoryLegacyBalancePort legacyBalancePort,
			InventoryLedgerRepository ledgerRepository) {
		if (legacyBalancePort == null) throw new IllegalArgumentException("legacyBalancePort wajib diisi");
		if (ledgerRepository == null) throw new IllegalArgumentException("ledgerRepository wajib diisi");
		this.legacyBalancePort = legacyBalancePort;
		this.ledgerRepository = ledgerRepository;
	}

	public InventoryReconciliationResult reconcile(InventoryBalanceKey balanceKey) {
		if (balanceKey == null) {
			return failed(null, "balanceKey wajib diisi");
		}
		try {
			BigDecimal legacy = legacyBalancePort.getBalance(balanceKey);
			BigDecimal ledger = ledgerRepository.getBalance(balanceKey);
			if (legacy == null || ledger == null) {
				return new InventoryReconciliationResult(InventoryReconciliationResult.FAILED,
						balanceKey, legacy, ledger, null, "saldo tidak lengkap");
			}
			BigDecimal difference = ledger.subtract(legacy);
			String status = difference.compareTo(BigDecimal.ZERO) == 0
					? InventoryReconciliationResult.MATCHED
					: InventoryReconciliationResult.MISMATCH;
			return new InventoryReconciliationResult(status, balanceKey, legacy, ledger,
					difference, status.equals(InventoryReconciliationResult.MATCHED)
							? "saldo sesuai" : "saldo berbeda");
		} catch (RuntimeException error) {
			return failed(balanceKey, error.getMessage() == null
					? error.getClass().getName() : error.getMessage());
		}
	}

	private InventoryReconciliationResult failed(InventoryBalanceKey key, String message) {
		return new InventoryReconciliationResult(InventoryReconciliationResult.FAILED,
				key, null, null, null, message);
	}
}
