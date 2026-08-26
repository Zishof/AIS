package ais.common.inventory.procurement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Draft kebutuhan pembelian eksternal. Adapter konkret wajib menyimpannya pada
 * PermintaanPengadaanMasterAsset beserta detail dan bridge item, bukan membuat
 * keluarga dokumen PR baru.
 */
public final class ProcurementRequisitionDraft {

	private final Long tenantId;
	private final Long sourceWarehouseLocationId;
	private final Long targetOutletLocationId;
	private final String sourceReplenishmentRequestNumber;
	private final String idempotencyKey;
	private final Date requestedAt;
	private final List<ProcurementRequisitionDraftLine> lines;

	public ProcurementRequisitionDraft(Long tenantId, Long sourceWarehouseLocationId,
			Long targetOutletLocationId, String sourceReplenishmentRequestNumber,
			String idempotencyKey, Date requestedAt,
			List<ProcurementRequisitionDraftLine> lines) {
		this.tenantId = tenantId;
		this.sourceWarehouseLocationId = sourceWarehouseLocationId;
		this.targetOutletLocationId = targetOutletLocationId;
		this.sourceReplenishmentRequestNumber = bersihkan(sourceReplenishmentRequestNumber);
		this.idempotencyKey = bersihkan(idempotencyKey);
		this.requestedAt = salin(requestedAt);
		this.lines = lines == null
				? Collections.<ProcurementRequisitionDraftLine>emptyList()
				: Collections.unmodifiableList(new ArrayList<ProcurementRequisitionDraftLine>(lines));
	}

	public List<String> validate() {
		List<String> errors = new ArrayList<String>();
		if (tenantId == null || tenantId.longValue() <= 0L) errors.add("tenantId harus positif");
		if (sourceWarehouseLocationId == null || sourceWarehouseLocationId.longValue() <= 0L) {
			errors.add("sourceWarehouseLocationId harus positif");
		}
		if (targetOutletLocationId == null || targetOutletLocationId.longValue() <= 0L) {
			errors.add("targetOutletLocationId harus positif");
		}
		if (sourceReplenishmentRequestNumber.length() == 0) {
			errors.add("sourceReplenishmentRequestNumber wajib diisi");
		}
		if (idempotencyKey.length() == 0) errors.add("idempotencyKey wajib diisi");
		if (requestedAt == null) errors.add("requestedAt wajib diisi");
		if (lines.isEmpty()) errors.add("minimal satu shortage wajib diisi");
		Set<Integer> lineNumbers = new HashSet<Integer>();
		Set<String> itemUomKeys = new HashSet<String>();
		for (int i = 0; i < lines.size(); i++) {
			ProcurementRequisitionDraftLine line = lines.get(i);
			if (line == null) {
				errors.add("baris draft PR tidak boleh null");
				continue;
			}
			List<String> lineErrors = line.validate();
			for (int j = 0; j < lineErrors.size(); j++) {
				errors.add("baris " + line.getLineNumber() + ": " + lineErrors.get(j));
			}
			Integer lineNumber = Integer.valueOf(line.getLineNumber());
			if (!lineNumbers.add(lineNumber)) errors.add("lineNumber duplikat: " + lineNumber);
			if (line.getItemId() != null && line.getUomId() != null) {
				String itemUomKey = line.getItemId() + ":" + line.getUomId();
				if (!itemUomKeys.add(itemUomKey)) errors.add("item dan UOM duplikat: " + itemUomKey);
			}
		}
		return Collections.unmodifiableList(errors);
	}

	public boolean isValid() { return validate().isEmpty(); }
	public Long getTenantId() { return tenantId; }
	public Long getSourceWarehouseLocationId() { return sourceWarehouseLocationId; }
	public Long getTargetOutletLocationId() { return targetOutletLocationId; }
	public String getSourceReplenishmentRequestNumber() { return sourceReplenishmentRequestNumber; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public Date getRequestedAt() { return salin(requestedAt); }
	public List<ProcurementRequisitionDraftLine> getLines() { return lines; }

	private static String bersihkan(String value) {
		return value == null ? "" : value.trim();
	}

	private static Date salin(Date value) {
		return value == null ? null : new Date(value.getTime());
	}
}
