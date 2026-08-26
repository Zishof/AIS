package ais.common.inventory.procurement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Satu baris shortage yang akan dipetakan ke detail PR existing. */
public final class ProcurementRequisitionDraftLine {

	private final int lineNumber;
	private final int sourceReplenishmentLineNumber;
	private final Long itemId;
	private final Long uomId;
	private final BigDecimal requestedQuantity;

	public ProcurementRequisitionDraftLine(int lineNumber,
			int sourceReplenishmentLineNumber, Long itemId, Long uomId,
			BigDecimal requestedQuantity) {
		this.lineNumber = lineNumber;
		this.sourceReplenishmentLineNumber = sourceReplenishmentLineNumber;
		this.itemId = itemId;
		this.uomId = uomId;
		this.requestedQuantity = requestedQuantity;
	}

	public List<String> validate() {
		List<String> errors = new ArrayList<String>();
		if (lineNumber <= 0) errors.add("lineNumber harus positif");
		if (sourceReplenishmentLineNumber <= 0) {
			errors.add("sourceReplenishmentLineNumber harus positif");
		}
		if (itemId == null || itemId.longValue() <= 0L) errors.add("itemId harus positif");
		if (uomId == null || uomId.longValue() <= 0L) errors.add("uomId harus positif");
		if (requestedQuantity == null || requestedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
			errors.add("requestedQuantity harus lebih besar dari nol");
		}
		return Collections.unmodifiableList(errors);
	}

	public int getLineNumber() { return lineNumber; }
	public int getSourceReplenishmentLineNumber() { return sourceReplenishmentLineNumber; }
	public Long getItemId() { return itemId; }
	public Long getUomId() { return uomId; }
	public BigDecimal getRequestedQuantity() { return requestedQuantity; }
}
