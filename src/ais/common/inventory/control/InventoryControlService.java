package ais.common.inventory.control;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import ais.common.inventory.InventoryMovementCommand;
import ais.common.inventory.InventoryMovementResult;
import ais.common.inventory.InventoryPostingPort;

/** Layanan aplikasi untuk cycle count, karantina, dan alokasi FEFO. */
public final class InventoryControlService {
	private final InventoryPostingPort postingPort;
	private final InventoryLotControlPort lotControlPort;

	public InventoryControlService(InventoryPostingPort postingPort,
			InventoryLotControlPort lotControlPort) {
		if (postingPort == null) throw new IllegalArgumentException("postingPort wajib diisi");
		if (lotControlPort == null) throw new IllegalArgumentException("lotControlPort wajib diisi");
		this.postingPort = postingPort;
		this.lotControlPort = lotControlPort;
	}

	public OperationResult postCycleCount(CycleCount count) {
		List<String> messages = new ArrayList<String>();
		if (count == null) {
			messages.add("cycle count wajib diisi");
			return new OperationResult(OperationResult.REJECTED, 0, 0, messages);
		}
		messages.addAll(count.validate());
		if (!CycleCount.APPROVED.equals(count.getStatus()) && !CycleCount.POSTED.equals(count.getStatus())) {
			messages.add("cycle count harus disetujui sebelum posting");
		}
		if (!messages.isEmpty()) return new OperationResult(OperationResult.REJECTED, 0, 0, messages);

		int posted = 0;
		int replay = 0;
		for (int i = 0; i < count.getLines().size(); i++) {
			CycleCountLine line = count.getLines().get(i);
			BigDecimal variance = line.getVariance();
			if (variance.compareTo(BigDecimal.ZERO) == 0) continue;
			InventoryMovementCommand command = new InventoryMovementCommand(
					line.getTenantId(), line.getLocationId(), line.getItemId(), line.getUomId(),
					line.getLotId(), variance, "CYCLE_COUNT", count.getCountId(),
					"STOCK_ADJUSTMENT", "CYCLE_COUNT:" + line.getLineId() + ":ADJUSTMENT",
					count.getBusinessAt());
			try {
				InventoryMovementResult result = postingPort.post(command);
				if (result == null || !result.isSuccessful()) {
					messages.add(result == null ? "postingPort tidak mengembalikan hasil"
							: "koreksi stok ditolak: " + result.getMessage());
					return new OperationResult(OperationResult.FAILED, posted, replay, messages);
				}
				if (result.isIdempotentReplay()) replay++; else posted++;
			} catch (RuntimeException ex) {
				messages.add("posting koreksi stok gagal: " + pesan(ex));
				return new OperationResult(OperationResult.FAILED, posted, replay, messages);
			}
		}
		return new OperationResult(posted == 0 ? OperationResult.ALREADY_PROCESSED
				: OperationResult.POSTED, posted, replay, messages);
	}

	public OperationResult releaseQuarantine(QuarantineRelease release) {
		List<String> messages = new ArrayList<String>();
		if (release == null) messages.add("pelepasan karantina wajib diisi");
		else messages.addAll(release.validate());
		if (!messages.isEmpty()) return new OperationResult(OperationResult.REJECTED, 0, 0, messages);
		try {
			InventoryLotControlResult result = lotControlPort.releaseQuarantine(release);
			if (result == null || !result.isSuccessful()) {
				messages.add(result == null ? "lotControlPort tidak mengembalikan hasil"
						: "pelepasan karantina ditolak: " + result.getMessage());
				return new OperationResult(OperationResult.FAILED, 0, 0, messages);
			}
			return new OperationResult(result.isIdempotentReplay()
					? OperationResult.ALREADY_PROCESSED : OperationResult.POSTED,
					result.isIdempotentReplay() ? 0 : 1, result.isIdempotentReplay() ? 1 : 0, messages);
		} catch (RuntimeException ex) {
			messages.add("pelepasan karantina gagal: " + pesan(ex));
			return new OperationResult(OperationResult.FAILED, 0, 0, messages);
		}
	}

	public FefoPlan planFefo(BigDecimal requestedQuantity, Date businessAt,
			List<FefoLotCandidate> candidates) {
		List<String> errors = new ArrayList<String>();
		if (requestedQuantity == null || requestedQuantity.compareTo(BigDecimal.ZERO) <= 0) errors.add("jumlah permintaan wajib lebih besar dari nol");
		if (businessAt == null) errors.add("businessAt wajib diisi");
		if (candidates == null) errors.add("kandidat lot wajib diisi");
		if (!errors.isEmpty()) return new FefoPlan(Collections.<Allocation>emptyList(), requestedQuantity, errors);

		List<FefoLotCandidate> eligible = new ArrayList<FefoLotCandidate>();
		for (int i = 0; i < candidates.size(); i++) {
			FefoLotCandidate value = candidates.get(i);
			if (value == null || value.getLotId() == null || value.isQuarantined()) continue;
			if (value.getAvailableQuantity() == null || value.getAvailableQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;
			if (value.getExpiryAt() != null && !value.getExpiryAt().after(businessAt)) continue;
			eligible.add(value);
		}
		Collections.sort(eligible, new Comparator<FefoLotCandidate>() {
			public int compare(FefoLotCandidate left, FefoLotCandidate right) {
				Date leftDate = left.getExpiryAt();
				Date rightDate = right.getExpiryAt();
				if (leftDate == null && rightDate != null) return 1;
				if (leftDate != null && rightDate == null) return -1;
				if (leftDate != null) {
					int compared = leftDate.compareTo(rightDate);
					if (compared != 0) return compared;
				}
				return left.getLotId().compareTo(right.getLotId());
			}
		});
		BigDecimal remaining = requestedQuantity;
		List<Allocation> allocations = new ArrayList<Allocation>();
		for (int i = 0; i < eligible.size() && remaining.compareTo(BigDecimal.ZERO) > 0; i++) {
			FefoLotCandidate lot = eligible.get(i);
			BigDecimal allocated = lot.getAvailableQuantity().min(remaining);
			allocations.add(new Allocation(lot.getLotId(), allocated));
			remaining = remaining.subtract(allocated);
		}
		return new FefoPlan(allocations, remaining, errors);
	}

	private static String pesan(Throwable value) {
		String message = value == null ? null : value.getMessage();
		return message == null || message.trim().length() == 0 ? "kesalahan tanpa pesan" : message.trim();
	}

	public static final class OperationResult {
		public static final String POSTED = "POSTED";
		public static final String ALREADY_PROCESSED = "ALREADY_PROCESSED";
		public static final String REJECTED = "REJECTED";
		public static final String FAILED = "FAILED";
		private final String status;
		private final int processedCount;
		private final int replayCount;
		private final List<String> messages;
		public OperationResult(String status, int processedCount, int replayCount, List<String> messages) {
			this.status = status;
			this.processedCount = processedCount;
			this.replayCount = replayCount;
			this.messages = Collections.unmodifiableList(new ArrayList<String>(messages));
		}
		public String getStatus() { return status; }
		public int getProcessedCount() { return processedCount; }
		public int getReplayCount() { return replayCount; }
		public List<String> getMessages() { return messages; }
		public boolean isSuccessful() { return POSTED.equals(status) || ALREADY_PROCESSED.equals(status); }
	}

	public static final class Allocation {
		private final Long lotId;
		private final BigDecimal quantity;
		public Allocation(Long lotId, BigDecimal quantity) { this.lotId = lotId; this.quantity = quantity; }
		public Long getLotId() { return lotId; }
		public BigDecimal getQuantity() { return quantity; }
	}

	public static final class FefoPlan {
		private final List<Allocation> allocations;
		private final BigDecimal shortage;
		private final List<String> errors;
		public FefoPlan(List<Allocation> allocations, BigDecimal shortage, List<String> errors) {
			this.allocations = Collections.unmodifiableList(new ArrayList<Allocation>(allocations));
			this.shortage = shortage == null ? BigDecimal.ZERO : shortage;
			this.errors = Collections.unmodifiableList(new ArrayList<String>(errors));
		}
		public List<Allocation> getAllocations() { return allocations; }
		public BigDecimal getShortage() { return shortage; }
		public List<String> getErrors() { return errors; }
		public boolean isComplete() { return errors.isEmpty() && shortage.compareTo(BigDecimal.ZERO) == 0; }
	}
}
