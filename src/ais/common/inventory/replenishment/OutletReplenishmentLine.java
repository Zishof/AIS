package ais.common.inventory.replenishment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Satu kebutuhan item pada permintaan stok internal outlet. */
public final class OutletReplenishmentLine {

	private final int lineNumber;
	private final Long itemId;
	private final Long uomId;
	private final BigDecimal requestedQuantity;

	public OutletReplenishmentLine(int lineNumber, Long itemId, Long uomId,
			BigDecimal requestedQuantity) {
		this.lineNumber = lineNumber;
		this.itemId = itemId;
		this.uomId = uomId;
		this.requestedQuantity = requestedQuantity;
	}

	public List<String> validate() {
		List<String> errors = new ArrayList<String>();
		if (lineNumber <= 0) errors.add("lineNumber harus positif");
		if (itemId == null || itemId.longValue() <= 0L) errors.add("itemId harus positif");
		if (uomId == null || uomId.longValue() <= 0L) errors.add("uomId harus positif");
		if (requestedQuantity == null || requestedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
			errors.add("requestedQuantity harus lebih besar dari nol");
		}
		return Collections.unmodifiableList(errors);
	}

	public boolean isValid() { return validate().isEmpty(); }
	public int getLineNumber() { return lineNumber; }
	public Long getItemId() { return itemId; }
	public Long getUomId() { return uomId; }
	public BigDecimal getRequestedQuantity() { return requestedQuantity; }
}

