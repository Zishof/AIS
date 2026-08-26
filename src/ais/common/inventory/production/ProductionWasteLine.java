package ais.common.inventory.production;

import java.math.BigDecimal;

/** Waste/scrap terstruktur; stok hanya diposting bila affectsStock bernilai true. */
public final class ProductionWasteLine {
	private final String lineId;
	private final Long itemId;
	private final Long uomId;
	private final Long lotId;
	private final BigDecimal quantity;
	private final String reasonCode;
	private final boolean affectsStock;

	public ProductionWasteLine(String lineId, Long itemId, Long uomId, Long lotId,
			BigDecimal quantity, String reasonCode, boolean affectsStock) {
		this.lineId = clean(lineId);
		this.itemId = itemId;
		this.uomId = uomId;
		this.lotId = lotId;
		this.quantity = quantity;
		this.reasonCode = clean(reasonCode);
		this.affectsStock = affectsStock;
		if (this.lineId.length() == 0 || itemId == null || uomId == null || lotId == null
				|| quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0
				|| this.reasonCode.length() == 0) {
			throw new IllegalArgumentException("Waste produksi tidak lengkap");
		}
	}

	public String getLineId() { return lineId; }
	public Long getItemId() { return itemId; }
	public Long getUomId() { return uomId; }
	public Long getLotId() { return lotId; }
	public BigDecimal getQuantity() { return quantity; }
	public String getReasonCode() { return reasonCode; }
	public boolean isAffectsStock() { return affectsStock; }

	private static String clean(String value) { return value == null ? "" : value.trim(); }
}
