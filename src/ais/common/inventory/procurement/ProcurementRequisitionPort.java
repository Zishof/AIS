package ais.common.inventory.procurement;

/**
 * Boundary penyimpanan PR existing. Implementasi wajib mencari berdasarkan
 * idempotencyKey sebelum insert dan mengembalikan ALREADY_EXISTS saat retry.
 */
public interface ProcurementRequisitionPort {
	ProcurementRequisitionResult createOrFind(ProcurementRequisitionDraft draft);
}
