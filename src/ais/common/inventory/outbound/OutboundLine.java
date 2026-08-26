package ais.common.inventory.outbound;

import java.math.BigDecimal;

/** Baris permintaan transfer dari gudang ke outlet. */
public final class OutboundLine {
	private final String lineId;
	private final Long itemId;
	private final Long uomId;
	private final BigDecimal requestedQuantity;

	public OutboundLine(String lineId, Long itemId, Long uomId, BigDecimal requestedQuantity) {
		this.lineId = clean(lineId);
		this.itemId = itemId;
		this.uomId = uomId;
		this.requestedQuantity = requestedQuantity;
		if (this.lineId.length() == 0 || itemId == null || uomId == null
				|| requestedQuantity == null || requestedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Baris outbound tidak lengkap");
		}
	}

	public String getLineId() { return lineId; }
	public Long getItemId() { return itemId; }
	public Long getUomId() { return uomId; }
	public BigDecimal getRequestedQuantity() { return requestedQuantity; }

	private static String clean(String value) { return value == null ? "" : value.trim(); }
}
