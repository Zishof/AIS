package ais.common.inventory.master;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aturan konversi satuan per item kanonis.
 *
 * Rumus: nilai tujuan = nilai asal x pembilang / penyebut.
 */
public final class InventoryUomConversion {

	private final String tenantKey;
	private final Long canonicalItemId;
	private final String fromUomCode;
	private final String toUomCode;
	private final BigDecimal numerator;
	private final BigDecimal denominator;
	private final int scale;
	private final int roundingMode;

	public InventoryUomConversion(String tenantKey, Long canonicalItemId,
			String fromUomCode, String toUomCode, BigDecimal numerator,
			BigDecimal denominator, int scale, int roundingMode) {
		this.tenantKey = bersihkan(tenantKey);
		this.canonicalItemId = canonicalItemId;
		this.fromUomCode = bersihkan(fromUomCode);
		this.toUomCode = bersihkan(toUomCode);
		this.numerator = numerator;
		this.denominator = denominator;
		this.scale = scale;
		this.roundingMode = roundingMode;
	}

	public List<String> validate() {
		List<String> errors = new ArrayList<String>();
		if (tenantKey.length() == 0) errors.add("tenantKey wajib diisi");
		if (canonicalItemId == null || canonicalItemId.longValue() <= 0L) errors.add("canonicalItemId harus positif");
		if (fromUomCode.length() == 0) errors.add("fromUomCode wajib diisi");
		if (toUomCode.length() == 0) errors.add("toUomCode wajib diisi");
		if (fromUomCode.equals(toUomCode)) errors.add("satuan asal dan tujuan harus berbeda");
		if (numerator == null || numerator.compareTo(BigDecimal.ZERO) <= 0) errors.add("pembilang harus positif");
		if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) errors.add("penyebut harus positif");
		if (scale < 0 || scale > 12) errors.add("scale harus di antara 0 dan 12");
		if (roundingMode < BigDecimal.ROUND_UP || roundingMode > BigDecimal.ROUND_UNNECESSARY) errors.add("roundingMode tidak dikenal");
		return Collections.unmodifiableList(errors);
	}

	public boolean isValid() {
		return validate().isEmpty();
	}

	public BigDecimal convert(BigDecimal value) {
		if (value == null) throw new IllegalArgumentException("nilai konversi wajib diisi");
		if (!isValid()) throw new IllegalStateException(validate().toString());
		return value.multiply(numerator).divide(denominator, scale, roundingMode);
	}

	public InventoryUomConversion reverse() {
		return new InventoryUomConversion(tenantKey, canonicalItemId, toUomCode,
				fromUomCode, denominator, numerator, scale, roundingMode);
	}

	public String getTenantKey() { return tenantKey; }
	public Long getCanonicalItemId() { return canonicalItemId; }
	public String getFromUomCode() { return fromUomCode; }
	public String getToUomCode() { return toUomCode; }
	public BigDecimal getNumerator() { return numerator; }
	public BigDecimal getDenominator() { return denominator; }
	public int getScale() { return scale; }
	public int getRoundingMode() { return roundingMode; }

	private static String bersihkan(String value) {
		return value == null ? "" : value.trim().toUpperCase();
	}
}
