package ais.common.inventory.procurement;

/**
 * Memetakan identitas kanonik replenishment ke referensi model pengadaan lama.
 * Implementasi sengaja berbasis ID agar adapter tidak menebak relasi item,
 * pengguna, atau toko dari nama/kode yang dapat berubah.
 */
public interface ProcurementRequisitionLegacyReferenceResolver {

	String resolveRequesterUserId(ProcurementRequisitionDraft draft);

	Long resolveTargetTokoId(ProcurementRequisitionDraft draft);

	Long resolveMasterAssetId(ProcurementRequisitionDraft draft,
			ProcurementRequisitionDraftLine line);
}
