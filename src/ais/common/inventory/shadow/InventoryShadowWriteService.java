package ais.common.inventory.shadow;

import java.util.List;

import ais.common.inventory.InventoryMovementCommand;
import ais.common.inventory.InventoryMovementResult;
import ais.common.inventory.InventoryPostingPort;

/**
 * Menjalankan posting bayangan sesudah commit legacy. Semua kegagalan diubah
 * menjadi hasil FAILED/REJECTED agar transaksi lama yang sudah berhasil tidak
 * ikut dianggap gagal.
 */
public final class InventoryShadowWriteService {

	private final InventoryShadowWriteSettings settings;
	private final InventoryPostingPort postingPort;
	private final InventoryShadowAuditPort auditPort;

	public InventoryShadowWriteService(InventoryShadowWriteSettings settings,
			InventoryPostingPort postingPort, InventoryShadowAuditPort auditPort) {
		if (settings == null) throw new IllegalArgumentException("settings wajib diisi");
		if (postingPort == null) throw new IllegalArgumentException("postingPort wajib diisi");
		this.settings = settings;
		this.postingPort = postingPort;
		this.auditPort = auditPort;
	}

	public InventoryShadowWriteResult executeAfterLegacyCommit(
			InventoryMovementCommand command) {
		String key = command == null ? "" : command.getIdempotencyKey();
		if (!settings.isEnabled()) {
			return audit(new InventoryShadowWriteResult(settings.getWriterCode(),
					InventoryShadowWriteResult.DISABLED, key, null,
					"shadow-write nonaktif"));
		}
		if (command == null) {
			return audit(new InventoryShadowWriteResult(settings.getWriterCode(),
					InventoryShadowWriteResult.REJECTED, "", null,
					"perintah mutasi kosong"));
		}
		List<String> errors = command.validate();
		if (!errors.isEmpty()) {
			return audit(new InventoryShadowWriteResult(settings.getWriterCode(),
					InventoryShadowWriteResult.REJECTED, key, null, join(errors)));
		}
		try {
			InventoryMovementResult posted = postingPort.post(command);
			if (posted == null) {
				return audit(new InventoryShadowWriteResult(settings.getWriterCode(),
						InventoryShadowWriteResult.FAILED, key, null,
						"posting port mengembalikan hasil kosong"));
			}
			String status = mapStatus(posted.getStatus());
			return audit(new InventoryShadowWriteResult(settings.getWriterCode(), status,
					key, posted.getMovementId(), posted.getMessage()));
		} catch (RuntimeException error) {
			return audit(new InventoryShadowWriteResult(settings.getWriterCode(),
					InventoryShadowWriteResult.FAILED, key, null,
					message(error)));
		}
	}

	private InventoryShadowWriteResult audit(InventoryShadowWriteResult result) {
		if (auditPort == null) return result;
		try {
			auditPort.record(result);
			return result;
		} catch (RuntimeException error) {
			return result.withMessage(result.getMessage() +
					"; audit gagal: " + message(error));
		}
	}

	private static String mapStatus(String status) {
		if (InventoryMovementResult.POSTED.equals(status)) return InventoryShadowWriteResult.POSTED;
		if (InventoryMovementResult.ALREADY_POSTED.equals(status)) return InventoryShadowWriteResult.ALREADY_POSTED;
		if (InventoryMovementResult.REJECTED.equals(status)) return InventoryShadowWriteResult.REJECTED;
		return InventoryShadowWriteResult.FAILED;
	}

	private static String join(List<String> values) {
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < values.size(); i++) {
			if (i > 0) text.append("; ");
			text.append(values.get(i));
		}
		return text.toString();
	}

	private static String message(RuntimeException error) {
		String value = error.getMessage();
		return value == null || value.trim().length() == 0
				? error.getClass().getName() : value.trim();
	}
}
