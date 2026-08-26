package ais.common.inventory.production;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Versi BOM immutable yang dapat dipakai ulang oleh order produksi. */
public final class BillOfMaterial {
	public static final String DRAFT = "DRAFT";
	public static final String ACTIVE = "ACTIVE";
	public static final String RETIRED = "RETIRED";

	private final String bomId;
	private final int version;
	private final Long outputItemId;
	private final Long outputUomId;
	private final BigDecimal baseQuantity;
	private final String status;
	private final List<BillOfMaterialLine> lines;

	public BillOfMaterial(String bomId, int version, Long outputItemId, Long outputUomId,
			BigDecimal baseQuantity, String status, List<BillOfMaterialLine> lines) {
		this.bomId = clean(bomId);
		this.version = version;
		this.outputItemId = outputItemId;
		this.outputUomId = outputUomId;
		this.baseQuantity = baseQuantity;
		this.status = clean(status);
		this.lines = lines == null ? Collections.<BillOfMaterialLine>emptyList()
				: Collections.unmodifiableList(new ArrayList<BillOfMaterialLine>(lines));
		if (this.bomId.length() == 0 || version <= 0 || outputItemId == null || outputUomId == null
				|| baseQuantity == null || baseQuantity.compareTo(BigDecimal.ZERO) <= 0
				|| (!DRAFT.equals(this.status) && !ACTIVE.equals(this.status) && !RETIRED.equals(this.status))
				|| this.lines.isEmpty()) {
			throw new IllegalArgumentException("BOM tidak lengkap");
		}
	}

	public String getBomId() { return bomId; }
	public int getVersion() { return version; }
	public Long getOutputItemId() { return outputItemId; }
	public Long getOutputUomId() { return outputUomId; }
	public BigDecimal getBaseQuantity() { return baseQuantity; }
	public String getStatus() { return status; }
	public List<BillOfMaterialLine> getLines() { return lines; }

	private static String clean(String value) { return value == null ? "" : value.trim(); }
}
