package ais.common.inventory.procurement;

import java.util.ArrayList;
import java.util.List;

import ais.common.inventory.replenishment.OutletReplenishmentLine;
import ais.common.inventory.replenishment.OutletReplenishmentPlan;
import ais.common.inventory.replenishment.OutletReplenishmentPlanLine;
import ais.common.inventory.replenishment.OutletReplenishmentRequest;

/** Mengubah hanya shortage replenishment menjadi draft PR vendor existing. */
public final class ReplenishmentShortageToProcurementService {

	private static final String IDEMPOTENCY_SUFFIX = ":SHORTAGE-PR";
	private final ProcurementRequisitionPort requisitionPort;

	public ReplenishmentShortageToProcurementService(ProcurementRequisitionPort requisitionPort) {
		if (requisitionPort == null) throw new IllegalArgumentException("requisitionPort wajib diisi");
		this.requisitionPort = requisitionPort;
	}

	public ProcurementRequisitionResult createOrFind(OutletReplenishmentPlan plan) {
		if (plan == null) return ProcurementRequisitionResult.rejected("plan wajib diisi");
		if (!plan.isSuccessful()) {
			return ProcurementRequisitionResult.rejected("plan replenishment belum berhasil: " + plan.getStatus());
		}
		OutletReplenishmentRequest request = plan.getRequest();
		if (request == null || !request.isValid()) {
			return ProcurementRequisitionResult.rejected("request replenishment tidak valid");
		}

		List<ProcurementRequisitionDraftLine> shortageLines = new ArrayList<ProcurementRequisitionDraftLine>();
		int draftLineNumber = 1;
		for (int i = 0; i < plan.getLines().size(); i++) {
			OutletReplenishmentPlanLine planLine = plan.getLines().get(i);
			if (planLine == null || !planLine.requiresProcurement()) continue;
			OutletReplenishmentLine sourceLine = planLine.getRequestLine();
			if (sourceLine == null) {
				return ProcurementRequisitionResult.rejected("baris plan shortage tidak memiliki baris sumber");
			}
			shortageLines.add(new ProcurementRequisitionDraftLine(draftLineNumber,
					sourceLine.getLineNumber(), sourceLine.getItemId(), sourceLine.getUomId(),
					planLine.getProcurementShortageQuantity()));
			draftLineNumber++;
		}
		if (shortageLines.isEmpty()) {
			return ProcurementRequisitionResult.notRequired("stok gudang mencukupi; PR tidak dibuat");
		}

		ProcurementRequisitionDraft draft = new ProcurementRequisitionDraft(request.getTenantId(),
				request.getSourceWarehouseLocationId(), request.getTargetOutletLocationId(),
				request.getRequestNumber(), request.getIdempotencyKey() + IDEMPOTENCY_SUFFIX,
				request.getRequestedAt(), shortageLines);
		List<String> errors = draft.validate();
		if (!errors.isEmpty()) {
			return ProcurementRequisitionResult.rejected(gabung(errors));
		}
		try {
			ProcurementRequisitionResult result = requisitionPort.createOrFind(draft);
			if (result == null) {
				return ProcurementRequisitionResult.failed("adapter PR mengembalikan hasil null", draft);
			}
			return result;
		} catch (RuntimeException e) {
			String message = e.getMessage() == null ? e.getClass().getName() : e.getMessage();
			return ProcurementRequisitionResult.failed("adapter PR gagal: " + message, draft);
		}
	}

	private static String gabung(List<String> messages) {
		StringBuilder value = new StringBuilder();
		for (int i = 0; i < messages.size(); i++) {
			if (i > 0) value.append("; ");
			value.append(messages.get(i));
		}
		return value.toString();
	}
}
