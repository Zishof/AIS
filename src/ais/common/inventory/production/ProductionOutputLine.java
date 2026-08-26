package ais.common.inventory.production;

import java.math.BigDecimal;

/** Hasil jadi/WIP per lot yang diterima dari satu order produksi. */
public final class ProductionOutputLine {
	private final String lineId;
	private final Long itemId;
	private final Long uomId;
	private final Long lotId;
	private final BigDecimal acceptedQuantity;

	public ProductionOutputLine(String lineId, Long itemId, Long uomId, Long lotId,
			BigDecimal acceptedQuantity) {
		this.lineId = clean(lineId);
		this.itemId = itemId;
		this.uomId = uomId;
		this.lotId = lotId;
		this.acceptedQuantity = acceptedQuantity;
		if (this.lineId.length() == 0 || itemId == null || uomId == null || lotId == null
				|| acceptedQuantity == null || acceptedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Hasil produksi tidak lengkap");
		}
	}

	public String getLineId() { return lineId; }
	public Long getItemId() { return itemId; }
	public Long getUomId() { return uomId; }
	public Long getLotId() { return lotId; }
	public BigDecimal getAcceptedQuantity() { return acceptedQuantity; }

	private static String clean(String value) { return value == null ? "" : value.trim(); }
}
