package ais.common.inventory.production;

import ais.common.inventory.InventoryMovementCommand;
import ais.common.inventory.InventoryMovementResult;
import ais.common.inventory.InventoryPostingPort;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Orkestrator produksi: workflow dan posting stok tetap terpisah serta idempoten. */
public final class ProductionService {
	private static final String SOURCE_TYPE = "PRODUCTION_ORDER";
	private final InventoryPostingPort postingPort;
	private final ProductionWorkflowPort workflowPort;

	public ProductionService(InventoryPostingPort postingPort, ProductionWorkflowPort workflowPort) {
		if (postingPort == null || workflowPort == null) {
			throw new IllegalArgumentException("Port produksi wajib diisi");
		}
		this.postingPort = postingPort;
		this.workflowPort = workflowPort;
	}

	public ProductionOperationResult release(ProductionWorkOrder order, Date businessAt) {
		return transition(order, ProductionWorkOrder.DRAFT, "RELEASE", "RELEASE", businessAt);
	}

	public ProductionOperationResult start(ProductionWorkOrder order, Date businessAt) {
		return transition(order, ProductionWorkOrder.RELEASED, "START", "START", businessAt);
	}

	public ProductionOperationResult complete(ProductionWorkOrder order,
			BigDecimal acceptedOutputQuantity, Date businessAt) {
		if (acceptedOutputQuantity == null || acceptedOutputQuantity.compareTo(BigDecimal.ZERO) <= 0) {
			return rejected("Order tidak dapat diselesaikan tanpa hasil diterima");
		}
		return transition(order, ProductionWorkOrder.IN_PROGRESS, "COMPLETE",
				"COMPLETE", businessAt);
	}

	public ProductionOperationResult issueMaterials(ProductionWorkOrder order,
			List<ProductionMaterialLine> lines, Date businessAt) {
		if (order == null || (!ProductionWorkOrder.RELEASED.equals(order.getStatus())
				&& !ProductionWorkOrder.IN_PROGRESS.equals(order.getStatus()))) {
			return rejected("Issue bahan hanya untuk order RELEASED/IN_PROGRESS");
		}
		return postMaterials(order, lines, businessAt, true);
	}

	public ProductionOperationResult returnMaterials(ProductionWorkOrder order,
			List<ProductionMaterialLine> lines, Date businessAt) {
		if (order == null || !ProductionWorkOrder.IN_PROGRESS.equals(order.getStatus())) {
			return rejected("Return bahan hanya untuk order IN_PROGRESS");
		}
		return postMaterials(order, lines, businessAt, false);
	}

	public ProductionOperationResult receiveOutputs(ProductionWorkOrder order,
			List<ProductionOutputLine> lines, Date businessAt) {
		if (order == null || !ProductionWorkOrder.IN_PROGRESS.equals(order.getStatus())) {
			return rejected("Penerimaan hasil hanya untuk order IN_PROGRESS");
		}
		if (lines == null || lines.isEmpty()) return rejected("Hasil produksi wajib diisi");
		Set<String> ids = new HashSet<String>();
		boolean replay = true;
		for (int i = 0; i < lines.size(); i++) {
			ProductionOutputLine line = lines.get(i);
			if (line == null || !ids.add(line.getLineId())) return rejected("Baris hasil tidak valid/duplikat");
			if (!order.getBillOfMaterial().getOutputItemId().equals(line.getItemId())
					|| !order.getBillOfMaterial().getOutputUomId().equals(line.getUomId())) {
				return rejected("Item/UOM hasil tidak sesuai BOM");
			}
			InventoryMovementResult movement = postingPort.post(command(order, line.getItemId(),
					line.getUomId(), line.getLotId(), line.getAcceptedQuantity(),
					"PRODUCTION_OUTPUT_RECEIPT", key(order, "OUTPUT", line.getLineId(), "RECEIVE"), businessAt));
			if (movement == null || !movement.isSuccessful()) return rejected(message(movement, "Posting hasil ditolak"));
			replay = replay && movement.isIdempotentReplay();
		}
		ProductionWorkflowResult workflow = workflowPort.apply(workflow(order, "OUTPUT_RECEIPT",
				"OUTPUT", key(order, "WORKFLOW", "OUTPUT", "RECEIVE"), businessAt));
		if (workflow == null || !workflow.isSuccessful()) return rejected(message(workflow, "Workflow hasil ditolak"));
		return success(replay && workflow.isIdempotentReplay(), lines.size(), "Hasil produksi diterima");
	}

	public ProductionOperationResult recordWaste(ProductionWorkOrder order,
			List<ProductionWasteLine> lines, Date businessAt) {
		if (order == null || !ProductionWorkOrder.IN_PROGRESS.equals(order.getStatus())) {
			return rejected("Waste hanya untuk order IN_PROGRESS");
		}
		if (lines == null || lines.isEmpty()) return rejected("Waste wajib diisi");
		Set<String> ids = new HashSet<String>();
		boolean replay = true;
		int posted = 0;
		for (int i = 0; i < lines.size(); i++) {
			ProductionWasteLine line = lines.get(i);
			if (line == null || !ids.add(line.getLineId())) return rejected("Baris waste tidak valid/duplikat");
			if (line.isAffectsStock()) {
				InventoryMovementResult movement = postingPort.post(command(order, line.getItemId(),
						line.getUomId(), line.getLotId(), line.getQuantity().negate(),
						"PRODUCTION_WASTE", key(order, "WASTE", line.getLineId(), "POST"), businessAt));
				if (movement == null || !movement.isSuccessful()) return rejected(message(movement, "Posting waste ditolak"));
				replay = replay && movement.isIdempotentReplay();
				posted++;
			}
		}
		ProductionWorkflowResult workflow = workflowPort.apply(workflow(order, "WASTE",
				"WASTE", key(order, "WORKFLOW", "WASTE", "RECORD"), businessAt));
		if (workflow == null || !workflow.isSuccessful()) return rejected(message(workflow, "Workflow waste ditolak"));
		return success(replay && workflow.isIdempotentReplay(), posted, "Waste produksi dicatat");
	}

	public ProductionCostSummary calculateCost(List<ProductionMaterialLine> materials,
			BigDecimal laborCost, BigDecimal overheadCost, BigDecimal acceptedOutputQuantity) {
		BigDecimal labor = nonNegative(laborCost, "Biaya tenaga kerja");
		BigDecimal overhead = nonNegative(overheadCost, "Biaya overhead");
		if (acceptedOutputQuantity == null || acceptedOutputQuantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Hasil diterima wajib lebih dari nol");
		}
		BigDecimal material = BigDecimal.ZERO;
		if (materials != null) {
			for (int i = 0; i < materials.size(); i++) {
				if (materials.get(i) == null) throw new IllegalArgumentException("Bahan costing tidak valid");
				material = material.add(materials.get(i).getExtendedCost());
			}
		}
		BigDecimal total = material.add(labor).add(overhead);
		BigDecimal unit = total.divide(acceptedOutputQuantity, 6, BigDecimal.ROUND_HALF_UP);
		return new ProductionCostSummary(material, labor, overhead, total, unit);
	}

	private ProductionOperationResult postMaterials(ProductionWorkOrder order,
			List<ProductionMaterialLine> lines, Date businessAt, boolean issue) {
		if (lines == null || lines.isEmpty()) return rejected("Bahan produksi wajib diisi");
		Set<String> ids = new HashSet<String>();
		boolean replay = true;
		String operation = issue ? "ISSUE" : "RETURN";
		String event = issue ? "PRODUCTION_MATERIAL_ISSUE" : "PRODUCTION_MATERIAL_RETURN";
		for (int i = 0; i < lines.size(); i++) {
			ProductionMaterialLine line = lines.get(i);
			if (line == null || !ids.add(line.getLineId())) return rejected("Baris bahan tidak valid/duplikat");
			BigDecimal quantity = issue ? line.getQuantity().negate() : line.getQuantity();
			InventoryMovementResult movement = postingPort.post(command(order, line.getItemId(),
					line.getUomId(), line.getLotId(), quantity, event,
					key(order, "MATERIAL", line.getLineId(), operation), businessAt));
			if (movement == null || !movement.isSuccessful()) return rejected(message(movement, "Posting bahan ditolak"));
			replay = replay && movement.isIdempotentReplay();
		}
		ProductionWorkflowResult workflow = workflowPort.apply(workflow(order,
				"MATERIAL_" + operation, "MATERIAL", key(order, "WORKFLOW", "MATERIAL", operation), businessAt));
		if (workflow == null || !workflow.isSuccessful()) return rejected(message(workflow, "Workflow bahan ditolak"));
		return success(replay && workflow.isIdempotentReplay(), lines.size(), "Bahan produksi " + operation.toLowerCase());
	}

	private ProductionOperationResult transition(ProductionWorkOrder order, String requiredStatus,
			String action, String reference, Date businessAt) {
		if (order == null || !requiredStatus.equals(order.getStatus())) {
			return rejected("Status order tidak sesuai untuk " + action);
		}
		ProductionWorkflowResult result = workflowPort.apply(workflow(order, action, reference,
				key(order, "WORKFLOW", reference, action), businessAt));
		if (result == null || !result.isSuccessful()) return rejected(message(result, "Workflow ditolak"));
		return success(result.isIdempotentReplay(), 0, "Workflow " + action + " berhasil");
	}

	private InventoryMovementCommand command(ProductionWorkOrder order, Long itemId, Long uomId,
			Long lotId, BigDecimal quantity, String event, String idempotencyKey, Date businessAt) {
		return new InventoryMovementCommand(order.getTenantId(), order.getLocationId(), itemId,
				uomId, lotId, quantity, SOURCE_TYPE, order.getOrderId(), event, idempotencyKey, businessAt);
	}

	private ProductionWorkflowCommand workflow(ProductionWorkOrder order, String action,
			String reference, String idempotencyKey, Date businessAt) {
		return new ProductionWorkflowCommand(order.getTenantId(), order.getLocationId(),
				order.getOrderId(), action, reference, idempotencyKey, businessAt);
	}

	private static String key(ProductionWorkOrder order, String group, String reference, String action) {
		return "PRODUCTION:" + order.getOrderId() + ":" + group + ":" + reference + ":" + action;
	}

	private static BigDecimal nonNegative(BigDecimal value, String label) {
		BigDecimal checked = value == null ? BigDecimal.ZERO : value;
		if (checked.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException(label + " tidak boleh negatif");
		return checked;
	}

	private static String message(InventoryMovementResult result, String fallback) {
		return result == null || result.getMessage() == null || result.getMessage().length() == 0
				? fallback : result.getMessage();
	}

	private static String message(ProductionWorkflowResult result, String fallback) {
		return result == null || result.getMessage() == null || result.getMessage().length() == 0
				? fallback : result.getMessage();
	}

	private static ProductionOperationResult success(boolean replay, int affected, String message) {
		return new ProductionOperationResult(true, replay, affected, message);
	}

	private static ProductionOperationResult rejected(String message) {
		return new ProductionOperationResult(false, false, 0, message);
	}
}
