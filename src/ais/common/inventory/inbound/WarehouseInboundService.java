package ais.common.inventory.inbound;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ais.common.inventory.InventoryMovementCommand;
import ais.common.inventory.InventoryMovementResult;
import ais.common.inventory.InventoryPostingPort;

/**
 * Gerbang tunggal stok inbound menjadi tersedia.
 *
 * BAST dan QC hanya mencatat fakta penerimaan. Mutasi stok baru diposting dari
 * putaway yang selesai, dengan idempotency key stabil per detail putaway.
 */
public final class WarehouseInboundService {
	private final InventoryPostingPort postingPort;

	public WarehouseInboundService(InventoryPostingPort postingPort) {
		if (postingPort == null) throw new IllegalArgumentException("postingPort wajib diisi");
		this.postingPort = postingPort;
	}

	public WarehouseInboundResult makeAvailable(GoodsReceipt receipt,
			List<PutawayInstruction> instructions) {
		List<String> errors = new ArrayList<String>();
		if (receipt == null) {
			errors.add("receipt wajib diisi");
			return result(WarehouseInboundResult.REJECTED, 0, 0, errors);
		}
		errors.addAll(receipt.validate());
		if (instructions == null || instructions.isEmpty()) errors.add("putaway wajib diisi");
		if (!errors.isEmpty()) return result(WarehouseInboundResult.REJECTED, 0, 0, errors);

		Set<String> detailIds = new HashSet<String>();
		Map<Integer, BigDecimal> putawayByLine = new HashMap<Integer, BigDecimal>();
		for (int i = 0; i < instructions.size(); i++) {
			PutawayInstruction instruction = instructions.get(i);
			if (instruction == null) {
				errors.add("instruksi putaway tidak boleh null");
				continue;
			}
			if (instruction.getDetailId().length() == 0) errors.add("detailId putaway wajib diisi");
			else if (!detailIds.add(instruction.getDetailId())) errors.add("detailId putaway ganda: " + instruction.getDetailId());
			GoodsReceiptLine line = receipt.findLine(instruction.getReceiptLineNumber());
			if (line == null) errors.add("baris receipt tidak ditemukan: " + instruction.getReceiptLineNumber());
			if (!instruction.isCompleted()) errors.add("putaway belum selesai: " + instruction.getDetailId());
			if (instruction.getTargetLocationId() == null) errors.add("targetLocationId wajib diisi: " + instruction.getDetailId());
			if (instruction.getCompletedAt() == null) errors.add("completedAt wajib diisi: " + instruction.getDetailId());
			if (instruction.getQuantity() == null || instruction.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
				errors.add("quantity putaway harus lebih besar dari nol: " + instruction.getDetailId());
			} else if (line != null) {
				Integer lineNumber = Integer.valueOf(line.getLineNumber());
				BigDecimal current = putawayByLine.get(lineNumber);
				putawayByLine.put(lineNumber, (current == null ? BigDecimal.ZERO : current).add(instruction.getQuantity()));
			}
		}

		for (int i = 0; i < receipt.getLines().size(); i++) {
			GoodsReceiptLine line = receipt.getLines().get(i);
			BigDecimal accepted = line.getAcceptedQuantity();
			BigDecimal putaway = putawayByLine.get(Integer.valueOf(line.getLineNumber()));
			putaway = putaway == null ? BigDecimal.ZERO : putaway;
			if (accepted.compareTo(putaway) != 0) {
				errors.add("jumlah putaway baris " + line.getLineNumber()
						+ " harus sama dengan jumlah diterima QC");
			}
		}
		if (!errors.isEmpty()) return result(WarehouseInboundResult.REJECTED, 0, 0, errors);

		int posted = 0;
		int replay = 0;
		for (int i = 0; i < instructions.size(); i++) {
			PutawayInstruction instruction = instructions.get(i);
			GoodsReceiptLine line = receipt.findLine(instruction.getReceiptLineNumber());
			InventoryMovementCommand command = new InventoryMovementCommand(
					receipt.getTenantId(), instruction.getTargetLocationId(), line.getItemId(),
					line.getUomId(), line.getLotId(), instruction.getQuantity(), "PUTAWAY",
					instruction.getDetailId(), "INBOUND_AVAILABLE",
					"PUTAWAY:" + instruction.getDetailId() + ":MOVE", instruction.getCompletedAt());
			try {
				InventoryMovementResult movement = postingPort.post(command);
				if (movement == null || !movement.isSuccessful()) {
					errors.add(movement == null ? "postingPort tidak mengembalikan hasil"
						: "posting ditolak: " + movement.getMessage());
					return result(WarehouseInboundResult.FAILED, posted, replay, errors);
				}
				if (movement.isIdempotentReplay()) replay++;
				else posted++;
			} catch (RuntimeException ex) {
				errors.add("posting inbound gagal: " + pesan(ex));
				return result(WarehouseInboundResult.FAILED, posted, replay, errors);
			}
		}
		String status = posted == 0 ? WarehouseInboundResult.ALREADY_POSTED : WarehouseInboundResult.POSTED;
		return result(status, posted, replay, errors);
	}

	private static WarehouseInboundResult result(String status, int posted, int replay,
			List<String> messages) {
		return new WarehouseInboundResult(status, posted, replay, messages);
	}

	private static String pesan(Throwable throwable) {
		String message = throwable == null ? null : throwable.getMessage();
		return message == null || message.trim().length() == 0 ? "kesalahan tanpa pesan" : message.trim();
	}
}
