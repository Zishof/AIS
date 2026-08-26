package ais.common.inventory.outbound;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import ais.common.inventory.InventoryMovementCommand;
import ais.common.inventory.InventoryMovementResult;
import ais.common.inventory.InventoryPostingPort;
import ais.common.inventory.ledger.InventoryBalanceKey;
import ais.common.inventory.reservation.InventoryReservationCommand;
import ais.common.inventory.reservation.InventoryReservationPort;
import ais.common.inventory.reservation.InventoryReservationResult;

/** Orkestrasi distribusi. Adapter wajib membungkus satu operasi dalam transaksi DB. */
public final class OutboundDistributionService {
	private final InventoryReservationPort reservationPort;
	private final InventoryPostingPort postingPort;
	private final OutboundWorkflowPort workflowPort;

	public OutboundDistributionService(InventoryReservationPort reservationPort,
			InventoryPostingPort postingPort, OutboundWorkflowPort workflowPort) {
		if (reservationPort == null || postingPort == null || workflowPort == null) {
			throw new IllegalArgumentException("Port outbound wajib diisi");
		}
		this.reservationPort = reservationPort;
		this.postingPort = postingPort;
		this.workflowPort = workflowPort;
	}

	public OutboundOperationResult reserve(OutboundOrder order, Date occurredAt) {
		requireStatus(order, OutboundOrder.APPROVED);
		boolean replay = true;
		List<OutboundLotAllocation> items = order.getAllocations();
		for (int i = 0; i < items.size(); i++) {
			OutboundLotAllocation item = items.get(i);
			String base = "OUTBOUND:" + order.getOrderId() + ":" + item.getAllocationId();
			InventoryReservationResult result = reservationPort.apply(new InventoryReservationCommand(
					InventoryReservationCommand.RESERVE, base, base + ":RESERVE",
					item.getSourceBalanceKey(), item.getQuantity(), order.getReservationExpiresAt()));
			if (!result.isSuccessful()) return failed(result.getMessage());
			if (!result.isIdempotentReplay()) replay = false;
		}
		OutboundWorkflowResult workflow = workflow(order.getOrderId(), "RESERVE", occurredAt, "");
		if (!workflow.isSuccessful()) return failed(workflow.getMessage());
		return success(replay && workflow.isIdempotentReplay(), false, items.size(), "Stok outbound telah direservasi");
	}

	public OutboundOperationResult markPicked(OutboundOrder order, Date occurredAt) {
		requireStatus(order, OutboundOrder.RESERVED);
		return workflowOnly(order.getOrderId(), "PICK", occurredAt, "Picking selesai");
	}

	public OutboundOperationResult markPacked(OutboundOrder order, Date occurredAt) {
		requireStatus(order, OutboundOrder.PICKED);
		return workflowOnly(order.getOrderId(), "PACK", occurredAt, "Packing selesai");
	}

	public OutboundOperationResult dispatch(Shipment shipment, Date occurredAt) {
		OutboundOrder order = shipment.getOrder();
		requireStatus(order, OutboundOrder.PACKED);
		boolean replay = true;
		List<OutboundLotAllocation> items = shipment.getAllocations();
		for (int i = 0; i < items.size(); i++) {
			OutboundLotAllocation item = items.get(i);
			String reserveBase = "OUTBOUND:" + order.getOrderId() + ":" + item.getAllocationId();
			InventoryReservationResult consumed = reservationPort.apply(new InventoryReservationCommand(
					InventoryReservationCommand.CONSUME, reserveBase, reserveBase + ":CONSUME",
					item.getSourceBalanceKey(), item.getQuantity(), null));
			if (!consumed.isSuccessful()) return failed(consumed.getMessage());
			String issueKey = "OUTBOUND:" + shipment.getShipmentId() + ":" + item.getAllocationId() + ":ISSUE";
			InventoryBalanceKey balance = item.getSourceBalanceKey();
			InventoryMovementResult movement = postingPort.post(new InventoryMovementCommand(
					order.getTenantId(), order.getSourceLocationId(), balance.getItemId(), item.getUomId(),
					balance.getLotId(), item.getQuantity().negate(), "SHIPMENT", shipment.getShipmentId(),
					"OUTBOUND_ISSUE", issueKey, occurredAt));
			if (!movement.isSuccessful()) return failed(movement.getMessage());
			if (!consumed.isIdempotentReplay() || !movement.isIdempotentReplay()) replay = false;
		}
		OutboundWorkflowResult workflow = workflow(shipment.getShipmentId(), "DISPATCH", occurredAt,
				"DO " + shipment.getDeliveryOrderId());
		if (!workflow.isSuccessful()) return failed(workflow.getMessage());
		return success(replay && workflow.isIdempotentReplay(), false, items.size(), "Shipment telah diserahkan ke pengangkut");
	}

	public OutboundOperationResult recordProofOfDelivery(ProofOfDelivery proof) {
		OutboundWorkflowResult result = workflowPort.apply(new OutboundWorkflowCommand("SHIPMENT",
				proof.getShipmentId(), "PROOF_OF_DELIVERY", "POD:" + proof.getProofId(),
				proof.getDeliveredAt(), proof.getReceivedBy() + " " + proof.getNote()));
		return result.isSuccessful()
				? success(result.isIdempotentReplay(), false, 0, "Proof of delivery tersimpan")
				: failed(result.getMessage());
	}

	public OutboundOperationResult receiveAtOutlet(OutletReceipt receipt) {
		Shipment shipment = receipt.getShipment();
		OutboundOrder order = shipment.getOrder();
		if (!OutboundOrder.DISPATCHED.equals(order.getStatus()) && !OutboundOrder.IN_TRANSIT.equals(order.getStatus())) {
			throw new IllegalStateException("Penerimaan outlet hanya untuk shipment dispatched/in transit");
		}
		boolean replay = true;
		int posted = 0;
		List<OutletReceiptLine> lines = receipt.getLines();
		for (int i = 0; i < lines.size(); i++) {
			OutletReceiptLine line = lines.get(i);
			if (line.getAcceptedQuantity().compareTo(BigDecimal.ZERO) > 0) {
				OutboundLotAllocation allocation = line.getAllocation();
				InventoryBalanceKey source = allocation.getSourceBalanceKey();
				String key = "OUTLET_RECEIPT:" + receipt.getReceiptId() + ":" + line.getReceiptLineId() + ":RECEIVE";
				InventoryMovementResult result = postingPort.post(new InventoryMovementCommand(
						order.getTenantId(), order.getDestinationLocationId(), source.getItemId(), allocation.getUomId(),
						source.getLotId(), line.getAcceptedQuantity(), "OUTLET_RECEIPT", receipt.getReceiptId(),
						"OUTLET_RECEIPT_IN", key, receipt.getReceivedAt()));
				if (!result.isSuccessful()) return failed(result.getMessage());
				if (!result.isIdempotentReplay()) replay = false;
				posted++;
			}
		}
		String action = receipt.hasDiscrepancy() ? "RECEIVE_PARTIAL_OR_EXCEPTION" : "RECEIVE_COMPLETE";
		OutboundWorkflowResult workflow = workflow(receipt.getReceiptId(), action, receipt.getReceivedAt(),
				receipt.hasDiscrepancy() ? "Buat discrepancy/claim" : "Diterima lengkap");
		if (!workflow.isSuccessful()) return failed(workflow.getMessage());
		return success(replay && workflow.isIdempotentReplay(), receipt.hasDiscrepancy(), posted,
				receipt.hasDiscrepancy() ? "Penerimaan tercatat dengan selisih" : "Penerimaan outlet lengkap");
	}

	private OutboundOperationResult workflowOnly(String id, String action, Date at, String message) {
		OutboundWorkflowResult result = workflow(id, action, at, message);
		return result.isSuccessful() ? success(result.isIdempotentReplay(), false, 0, message) : failed(result.getMessage());
	}

	private OutboundWorkflowResult workflow(String id, String action, Date at, String note) {
		return workflowPort.apply(new OutboundWorkflowCommand("OUTBOUND", id, action,
				"OUTBOUND:" + id + ":" + action, at, note));
	}

	private static void requireStatus(OutboundOrder order, String expected) {
		if (order == null || !expected.equals(order.getStatus())) {
			throw new IllegalStateException("Status outbound harus " + expected);
		}
	}

	private static OutboundOperationResult success(boolean replay, boolean discrepancy, int count, String message) {
		return new OutboundOperationResult(true, replay, discrepancy, count, message);
	}

	private static OutboundOperationResult failed(String message) {
		return new OutboundOperationResult(false, false, false, 0, message);
	}
}
